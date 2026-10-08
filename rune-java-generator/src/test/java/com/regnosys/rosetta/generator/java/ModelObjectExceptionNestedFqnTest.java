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
 * PR #305 anchor — facet exceptionNestedFqn (the #304 compounding follow-on).
 *
 * <p>#304 FQN-inlined the PRIMARY {@code extends} supertype whose SIMPLE name collides with an
 * implicitly-imported {@code java.lang} type (the only carrier is {@code fpml.consolidated.msg.Exception}).
 * #305 completes the family for the CROSS-PACKAGE {@code *Exception} subtypes: golden ALSO FQN-inlines the
 * NESTED supertype refs ({@code fpml.consolidated.msg.Exception.ExceptionBuilder}/{@code .ExceptionImpl}/
 * {@code .ExceptionBuilderImpl}) AND DROPS the (collision-blocked, now-unused) {@code import
 * fpml.consolidated.msg.Exception}. The fork emitted the bare nested ref ({@code Exception.ExceptionBuilder})
 * + kept the import — for a cross-package subtype this byte-diverged from golden (it compiled via the
 * import, but golden never imports a java.lang-colliding type — it FQN-inlines every reference).
 *
 * <p>Fix: {@link ModelObjectGenerator#superNestedQualifier} FQN-inlines the nested-ref qualifier when
 * {@link ModelObjectGenerator#superFqnInlined} ({@code collidesWithJavaLang} AND the supertype lives in a
 * DIFFERENT package); the same gate suppresses the supertype import at both the interface-declarations
 * import loop and the explicit super-import call.
 *
 * <p>Green-safe by construction (the universal corpus law, verified): a SAME-package colliding supertype
 * keeps the BARE nested ref (golden: the 4 {@code fpml.consolidated.msg.*Exception} subtypes — the #304
 * carriers — resolve {@code Exception.ExceptionBuilder} same-package; 0 of them FQN), so the cross-package
 * gate excludes them; a NON-colliding supertype always keeps its bare nested ref + import (e.g. cross-package
 * {@code YieldCurveValuation extends PricingStructureValuation}). The fix only changes already-divergent
 * cross-package {@code Exception} subtypes → green→red is structurally impossible.
 *
 * <p>Carriers (15, all CROSS-package {@code *Exception} subtypes of {@code fpml.consolidated.msg.Exception}):
 * ClearingEligibilityException, AllocationException, ClearingException, CollateralAllocationRejected,
 * ConfirmationException, ConsentException, ExecutionAdviceException, ExecutionException, MaturityException,
 * TradeChangeAdviceException, TradeReferenceInformationUpdateException, CreditEventException,
 * LoanNotificationException, NonpublicExecutionReportException, ValuationReportException.
 *
 * <p>Anchors are WHOLE-FILE byte comparisons against the frozen 9.83.0 goldens (newline-normalized),
 * generated through the REAL {@link D11CorpusRegressionTest#loadCellCorpusCached}. REVERT-VERIFIED RED
 * for the 5 flip locks + the positive-content lock (6/8); the 2 green-safety locks pass either way.
 */
class ModelObjectExceptionNestedFqnTest {

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

    /** POJO + choice generation only — the nested-ref FQN seat lives in {@link ModelObjectGenerator}. */
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

    // ==== Flip locks (revert-RED): cross-package *Exception subtype FQN-inlines nested refs + drops import ====

    @Test
    @EnabledIf("fpmlCellAvailable")
    void clearingException_nestedFqn_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("fpml/consolidated/confirmation/processes/ClearingException.java");
    }

    @Test
    @EnabledIf("fpmlCellAvailable")
    void clearingEligibilityException_nestedFqn_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("fpml/consolidated/clearing/processes/ClearingEligibilityException.java");
    }

    @Test
    @EnabledIf("fpmlCellAvailable")
    void creditEventException_nestedFqn_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("fpml/consolidated/credit/event/notification/CreditEventException.java");
    }

    @Test
    @EnabledIf("fpmlCellAvailable")
    void loanNotificationException_nestedFqn_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("fpml/consolidated/loan/LoanNotificationException.java");
    }

    @Test
    @EnabledIf("fpmlCellAvailable")
    void valuationReportException_nestedFqn_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("fpml/consolidated/valuation/fpmlreporting/ValuationReportException.java");
    }

    // ==== Positive-content lock (revert-RED): nested refs FQN'd, bare gone, import dropped ====

    /**
     * {@code ClearingException} must render the FQN nested supertype refs at all three sites and carry
     * NO bare {@code Exception.Exception*} ref and NO {@code import fpml.consolidated.msg.Exception}.
     * Reverting the fix re-emits the bare nested refs + the import → RED.
     */
    @Test
    @EnabledIf("fpmlCellAvailable")
    void clearingException_nestedFqn_positiveContent() {
        assertNotNull(fpmlPojoOutput, "rune-fpml POJO generation did not run — corpus unavailable?");
        String gen = fpmlPojoOutput.get("fpml/consolidated/confirmation/processes/ClearingException.java");
        assertNotNull(gen, "ClearingException not generated");
        // The three nested refs must be FQN-inlined.
        assertTrue(gen.contains("extends ClearingException, fpml.consolidated.msg.Exception.ExceptionBuilder"),
                "nested builder ref must be FQN-inlined (PR #305 exceptionNestedFqn)");
        assertTrue(gen.contains("extends fpml.consolidated.msg.Exception.ExceptionImpl"),
                "nested impl ref must be FQN-inlined");
        assertTrue(gen.contains("extends fpml.consolidated.msg.Exception.ExceptionBuilderImpl"),
                "nested builderImpl ref must be FQN-inlined");
        // The bare nested forms (java.lang.Exception collision) must be gone.
        assertFalse(gen.contains("extends ClearingException, Exception.ExceptionBuilder"),
                "the bare nested builder ref must be gone (cross-package collision FQN-inlined)");
        assertFalse(gen.contains("extends Exception.ExceptionImpl"),
                "the bare nested impl ref must be gone");
        // The collision-blocked supertype import must be suppressed.
        assertFalse(gen.contains("import fpml.consolidated.msg.Exception;"),
                "the collision-blocked supertype import must be dropped (golden never imports it)");
    }

    // ==== Green-safety locks (pass either way) ====

    /**
     * SAME-package green-safety: a SAME-package colliding {@code *Exception} subtype (the #304 carriers)
     * keeps the BARE nested ref — the cross-package gate excludes it. {@code EventStatusException}
     * (package {@code fpml.consolidated.msg}, same as {@code Exception}) must keep
     * {@code Exception.ExceptionBuilder} bare and must NOT FQN-inline it. Passes on clean source too
     * (clean never touched the nested refs). This is the load-bearing lock against a package-blind
     * over-fire that would regress the 4 #304 same-package carriers green→red.
     */
    @Test
    @EnabledIf("fpmlCellAvailable")
    void eventStatusException_samePackage_nestedStaysBare() {
        assertNotNull(fpmlPojoOutput, "rune-fpml POJO generation did not run — corpus unavailable?");
        String gen = fpmlPojoOutput.get("fpml/consolidated/msg/EventStatusException.java");
        assertNotNull(gen, "EventStatusException not generated");
        assertTrue(gen.contains("extends EventStatusException, Exception.ExceptionBuilder"),
                "a SAME-package colliding supertype keeps the bare nested ref (green-safety)");
        assertFalse(gen.contains("fpml.consolidated.msg.Exception.ExceptionBuilder"),
                "the cross-package gate must NOT FQN-inline a SAME-package nested ref");
    }

    /**
     * NON-colliding green-safety: a NON-colliding supertype keeps its bare nested ref AND its import,
     * regardless of package. {@code YieldCurveValuation extends PricingStructureValuation} (cross-package,
     * {@code java.lang.PricingStructureValuation} does not exist) must keep
     * {@code PricingStructureValuation.PricingStructureValuationBuilder} bare and keep
     * {@code import fpml.consolidated.riskdef.PricingStructureValuation}. Passes either way — guards
     * against the cross-package gate firing on a non-java.lang-colliding supertype.
     */
    @Test
    @EnabledIf("fpmlCellAvailable")
    void yieldCurveValuation_nonColliding_nestedStaysBare() {
        assertNotNull(fpmlPojoOutput, "rune-fpml POJO generation did not run — corpus unavailable?");
        String gen = fpmlPojoOutput.get("fpml/consolidated/mktenv/YieldCurveValuation.java");
        assertNotNull(gen, "YieldCurveValuation not generated");
        assertTrue(gen.contains(
                "extends YieldCurveValuation, PricingStructureValuation.PricingStructureValuationBuilder"),
                "a non-colliding supertype keeps its bare nested ref (green-safety)");
        assertTrue(gen.contains("import fpml.consolidated.riskdef.PricingStructureValuation;"),
                "a non-colliding supertype keeps its import (only collision-blocked imports drop)");
        assertFalse(gen.contains(
                "fpml.consolidated.riskdef.PricingStructureValuation.PricingStructureValuationBuilder"),
                "the fix must NOT FQN-inline a non-colliding nested ref (green-safety — no over-fire)");
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
                + path + " (PR #305 exceptionNestedFqn).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
