package com.regnosys.rosetta.generator.java;

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.java.object.ChoiceObjectGenerator;
import com.regnosys.rosetta.generator.java.object.ModelObjectGenerator;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static com.regnosys.rosetta.testutil.GenerationErrors.assertNoGenerationErrors;

/**
 * PR #304 anchor — facet exceptionSupertypeFqn.
 *
 * <p>The primary {@code extends} supertype of a generated POJO interface is FQN-inlined when its
 * SIMPLE name collides with an implicitly-imported {@code java.lang} type. The only corpus carrier
 * is the fpml type {@code fpml.consolidated.msg.Exception}: golden renders every reference to it
 * FQN-inlined (it can NEVER be imported — {@code java.lang.Exception} owns the simple name), so a
 * subtype renders {@code public interface X extends fpml.consolidated.msg.Exception}. The fork emitted
 * the BARE simple name {@code extends Exception}, which for a SAME-PACKAGE subtype still resolved
 * (same-package resolution wins over {@code java.lang}) but byte-diverged from golden.
 *
 * <p>Fix: {@link ModelObjectGenerator#collidesWithJavaLang} gates the primary-extends emission to the
 * FQN form. Green-safe by construction — across all 5 cells 0 goldens extend a bare java.lang-colliding
 * supertype simple name (verified); golden ALWAYS FQN-inlines such a supertype, and a NON-colliding
 * supertype keeps its bare simple name.
 *
 * <p>Carriers (4, all SAME-PACKAGE {@code fpml.consolidated.msg.*Exception}, the only ones whose SOLE
 * divergence is the primary-extends line): {@code EventStatusException}, {@code MessageRejected},
 * {@code ServiceNotificationException}, {@code VerificationStatusException}. (The cross-package
 * {@code *Exception} subtypes also FQN their primary extends now — moving TOWARD golden — but stay
 * divergent on their {@code import Exception} + bare nested {@code Exception.*Builder} refs.)
 *
 * <p>Anchors are WHOLE-FILE byte comparisons against the frozen 9.83.0 goldens (newline-normalized),
 * generated through the REAL {@link D11CorpusRegressionTest#loadCellCorpusCached}. REVERT-VERIFIED RED
 * for the 4 flip locks + the positive-content lock (5/6); the green-safety lock passes either way.
 */
class ModelObjectExceptionSupertypeFqnTest {

    private static final Path FPML_CELL_ROOT = Path.of("../test-corpus/rune-fpml/rune-fpml-2.0.0");
    private static final Path FPML_GOLDEN_DIR =
            FPML_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static Map<String, String> fpmlPojoOutput;

    static boolean fpmlCellAvailable() {
        return Files.isDirectory(FPML_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(FPML_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (fpmlCellAvailable()) {
            fpmlPojoOutput = generateCellPojos(
                    new D11CorpusRegressionTest.CellSpec("rune-fpml", "2.0.0", FPML_CELL_ROOT));
        }
    }

    /** POJO + choice generation only — the supertype-FQN seat lives in {@link ModelObjectGenerator}. */
    private static Map<String, String> generateCellPojos(D11CorpusRegressionTest.CellSpec cell)
            throws IOException {
        var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(), D11CorpusRegressionTest.emissionFilter(cell));
        var typeUtil = new JavaTypeUtil();
        var typeTranslator = new JavaTypeTranslator(typeUtil);
        var pojoGen = new ModelObjectGenerator(gm, typeTranslator, typeUtil);
        var choiceGen = new ChoiceObjectGenerator(gm, typeTranslator, typeUtil, pojoGen);
        Map<String, String> output = new LinkedHashMap<>();
        for (RModel model : corpus.workspace().files()) {
            if (gm.shouldGenerate(model)) {
                String version = gm.version(model);
                assertNoGenerationErrors(pojoGen.generateClasses(model, version, output));
                assertNoGenerationErrors(choiceGen.generateClasses(model, version, output));
            }
        }
        return output;
    }

    // ==== Flip locks (revert-RED): the same-package *Exception subtype FQN-inlines its supertype ====

    @Test
    @EnabledIf("fpmlCellAvailable")
    void eventStatusException_supertypeFqn_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("fpml/consolidated/msg/EventStatusException.java");
    }

    @Test
    @EnabledIf("fpmlCellAvailable")
    void messageRejected_supertypeFqn_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("fpml/consolidated/msg/MessageRejected.java");
    }

    @Test
    @EnabledIf("fpmlCellAvailable")
    void serviceNotificationException_supertypeFqn_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("fpml/consolidated/msg/ServiceNotificationException.java");
    }

    @Test
    @EnabledIf("fpmlCellAvailable")
    void verificationStatusException_supertypeFqn_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("fpml/consolidated/msg/VerificationStatusException.java");
    }

    // ==== Positive-content lock (revert-RED): the FQN fired, the bare collision is gone ====

    /**
     * {@code EventStatusException} must render the FQN supertype and carry NO bare {@code extends
     * Exception {}. Reverting the {@code collidesWithJavaLang} gate re-emits the bare
     * {@code extends Exception {} → RED.
     */
    @Test
    @EnabledIf("fpmlCellAvailable")
    void eventStatusException_supertypeFqn_positiveContent() {
        assertNotNull(fpmlPojoOutput, "rune-fpml POJO generation did not run — corpus unavailable?");
        String gen = fpmlPojoOutput.get("fpml/consolidated/msg/EventStatusException.java");
        assertNotNull(gen, "EventStatusException not generated");
        assertTrue(gen.contains("public interface EventStatusException extends fpml.consolidated.msg.Exception {"),
                "the java.lang-colliding supertype must be FQN-inlined (PR #304 exceptionSupertypeFqn)");
        assertFalse(gen.contains("public interface EventStatusException extends Exception {"),
                "the bare `extends Exception` (java.lang.Exception collision) must be gone");
    }

    // ==== Green-safety lock (passes either way): a NON-colliding supertype stays bare ====

    /**
     * A supertype whose SIMPLE name does NOT collide with {@code java.lang} keeps its bare simple
     * name. {@code YieldCurveValuation extends PricingStructureValuation}
     * ({@code java.lang.PricingStructureValuation} does not exist) must NOT be FQN-inlined — the fix
     * only touches java.lang-colliding supertypes. Passes on clean source too (the bare form was
     * never changed).
     */
    @Test
    @EnabledIf("fpmlCellAvailable")
    void yieldCurveValuation_nonCollidingSupertype_staysBare() {
        assertNotNull(fpmlPojoOutput, "rune-fpml POJO generation did not run — corpus unavailable?");
        String gen = fpmlPojoOutput.get("fpml/consolidated/mktenv/YieldCurveValuation.java");
        assertNotNull(gen, "YieldCurveValuation not generated");
        assertTrue(gen.contains("public interface YieldCurveValuation extends PricingStructureValuation {"),
                "a non-java.lang-colliding supertype must keep its bare simple name (green-safety)");
        assertFalse(gen.contains("extends fpml.consolidated.riskdef.PricingStructureValuation"),
                "the fix must NOT FQN-inline a non-colliding supertype (green-safety — no over-fire)");
    }

    private static void assertByteMatchesGolden(String path) throws IOException {
        assertNotNull(fpmlPojoOutput, "rune-fpml POJO generation did not run — corpus unavailable?");
        String generated = fpmlPojoOutput.get(path);
        assertNotNull(generated, "POJO not generated: " + path);
        Path goldenPath = FPML_GOLDEN_DIR.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated rune-fpml POJO must byte-match the golden (newline-normalized) for "
                + path + " (PR #304 exceptionSupertypeFqn).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
