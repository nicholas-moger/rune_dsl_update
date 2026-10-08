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
 * PR #306 anchor — facet javaLangAttrFqn (the attribute-type seat of the #304/#305 supertype law).
 *
 * <p>#304/#305 FQN-inlined a SUPERTYPE whose SIMPLE name collides with an implicitly-imported
 * {@code java.lang} type ({@code fpml.consolidated.msg.Exception}). #306 completes the family at the
 * ATTRIBUTE-type seat: a generated POJO ATTRIBUTE whose model type's simple name collides with a
 * {@code java.lang} type is FQN-inlined by golden at every VALUE render site (getter return,
 * {@code processRosetta(... X.class ...)}, builder add/set params, impl field, for-loop element,
 * {@code X.builder()}); the BUILDER type ({@code X.XBuilder}) stays BARE. The fork rendered the value
 * type bare (resolving the model type via same-package resolution, so it COMPILED but byte-diverged
 * from golden's defensive FQN). Carriers (2): iso {@code DataResponse.getError()}
 * ({@code iso20022.dtcc.rds.harmonized.Error}, collides with {@code java.lang.Error}) + fpml
 * {@code Formula.getMath()} ({@code fpml.consolidated.shared.Math}, collides with {@code java.lang.Math}).
 *
 * <p>Fix: {@link ModelObjectGenerator#itemTypeName} returns the canonical FQN for a MODEL attribute
 * type whose simple name collides with java.lang (gate: {@code collidesWithJavaLang(simple)} AND the
 * canonical NOT in {@code java.lang} — so a java.lang type ITSELF, e.g. {@code Integer}/{@code String},
 * stays BARE). The builder-type sites ({@code dotQualifiedBuilderType}, the builder-process
 * {@code .class}) use {@code itemSimpleName} (bare); the interface-process value {@code .class} in
 * {@link com.regnosys.rosetta.generator.java.object.ModelObjectBoilerplate} mirrors the same gate.
 *
 * <p>Green-safe by construction: a java.lang TYPE itself ({@code Integer}/{@code String}/…) is bare
 * (the canonical-{@code java.lang} exclusion — see {@link #equityAccumulator_javaLangType_staysBare},
 * the load-bearing OVER-FIRE GUARD); a NON-colliding model attribute stays bare; the fix only changes
 * output when the attribute's MODEL type's simple name collides with java.lang — the only 2 such POJOs
 * in the corpus are the carriers. Green→red is structurally impossible.
 *
 * <p>Anchors are WHOLE-FILE byte comparisons against the frozen 9.83.0 goldens (newline-normalized),
 * generated through the REAL {@link D11CorpusRegressionTest#loadCellCorpusCached}. REVERT-VERIFIED RED
 * for the 4 flip + positive-content locks; the 2 green-safety locks pass either way.
 */
class ModelObjectJavaLangAttrFqnTest {

    private static final Path ISO_CELL_ROOT = Path.of("../test-corpus/iso20022/iso20022-1.38.0");
    private static final Path ISO_GOLDEN_DIR =
            ISO_CELL_ROOT.resolve("rosetta-source/target/classes/generated/java"); // D25: ISO Xtext writes here
    private static final Path FPML_CELL_ROOT = Path.of("../test-corpus/rune-fpml/rune-fpml-2.0.0");
    private static final Path FPML_GOLDEN_DIR =
            FPML_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static Map<String, String> isoPojoOutput;
    private static Map<String, String> fpmlPojoOutput;

    static boolean isoCellAvailable() {
        return Files.isDirectory(ISO_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(ISO_GOLDEN_DIR);
    }

    static boolean fpmlCellAvailable() {
        return Files.isDirectory(FPML_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(FPML_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (isoCellAvailable()) {
            isoPojoOutput = generateCellPojos(
                    new D11CorpusRegressionTest.CellSpec("iso20022", "1.38.0", ISO_CELL_ROOT));
        }
        if (fpmlCellAvailable()) {
            fpmlPojoOutput = generateCellPojos(
                    new D11CorpusRegressionTest.CellSpec("rune-fpml", "2.0.0", FPML_CELL_ROOT));
        }
    }

    /** POJO + choice generation only — the attribute-type FQN seat lives in {@link ModelObjectGenerator}. */
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

    // ==== Flip locks (revert-RED): the colliding model attribute type FQN-inlines at value sites ====

    @Test
    @EnabledIf("isoCellAvailable")
    void dataResponse_attrFqn_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(isoPojoOutput, ISO_GOLDEN_DIR,
                "iso20022/dtcc/rds/harmonized/DataResponse.java");
    }

    @Test
    @EnabledIf("fpmlCellAvailable")
    void formula_attrFqn_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(fpmlPojoOutput, FPML_GOLDEN_DIR,
                "fpml/consolidated/shared/Formula.java");
    }

    // ==== Positive-content locks (revert-RED): value type FQN'd, builder type bare ====

    /**
     * {@code DataResponse.getError()} ({@code Error} collides with {@code java.lang.Error}): the VALUE
     * type must be FQN-inlined ({@code iso20022.dtcc.rds.harmonized.Error}) at the getter, the
     * interface-process {@code .class}, and the {@code Error.builder()} call; the BUILDER type
     * ({@code Error.ErrorBuilder}) must stay BARE; a NON-colliding sibling model ({@code Core}) stays
     * bare. Reverting the fix re-renders the value type bare → RED.
     */
    @Test
    @EnabledIf("isoCellAvailable")
    void dataResponse_positiveContent() {
        assertNotNull(isoPojoOutput, "iso20022 POJO generation did not run — corpus unavailable?");
        String gen = isoPojoOutput.get("iso20022/dtcc/rds/harmonized/DataResponse.java");
        assertNotNull(gen, "DataResponse not generated");
        // VALUE type FQN-inlined at every value site.
        assertTrue(gen.contains("List<? extends iso20022.dtcc.rds.harmonized.Error> getError();"),
                "the colliding value type must be FQN-inlined at the getter (PR #306 javaLangAttrFqn)");
        assertTrue(gen.contains("iso20022.dtcc.rds.harmonized.Error.class, getError()"),
                "the colliding value .class must be FQN-inlined in the interface process method");
        assertTrue(gen.contains(
                "Error.ErrorBuilder newError = iso20022.dtcc.rds.harmonized.Error.builder();"),
                "the X.builder() call must be FQN-inlined (and the X.XBuilder type stays bare)");
        // BUILDER type stays BARE.
        assertFalse(gen.contains("iso20022.dtcc.rds.harmonized.Error.ErrorBuilder"),
                "the nested builder type X.XBuilder must stay BARE (only value sites FQN-inline)");
        // The bare value-type getter must be gone.
        assertFalse(gen.contains("List<? extends Error> getError();"),
                "the bare value-type getter must be gone (FQN-inlined)");
        // Non-colliding sibling model stays bare.
        assertTrue(gen.contains("Core getCore();"),
                "a NON-colliding model attribute stays bare (green-safety: only the colliding type FQNs)");
    }

    /**
     * {@code Formula.getMath()} ({@code Math} collides with {@code java.lang.Math}): the VALUE type must
     * be FQN-inlined ({@code fpml.consolidated.shared.Math}) at the getter, the process {@code .class},
     * and the {@code Math.builder()} call; a {@code java.lang} TYPE itself ({@code String}) stays BARE.
     * Reverting the fix re-renders the value type bare → RED.
     */
    @Test
    @EnabledIf("fpmlCellAvailable")
    void formula_positiveContent() {
        assertNotNull(fpmlPojoOutput, "rune-fpml POJO generation did not run — corpus unavailable?");
        String gen = fpmlPojoOutput.get("fpml/consolidated/shared/Formula.java");
        assertNotNull(gen, "Formula not generated");
        assertTrue(gen.contains("fpml.consolidated.shared.Math getMath();"),
                "the colliding value type must be FQN-inlined at the getter (PR #306 javaLangAttrFqn)");
        assertTrue(gen.contains("fpml.consolidated.shared.Math.class, getMath()"),
                "the colliding value .class must be FQN-inlined in the interface process method");
        assertTrue(gen.contains("result = math = fpml.consolidated.shared.Math.builder();"),
                "the X.builder() call must be FQN-inlined");
        // A java.lang TYPE itself stays bare (the canonical-java.lang exclusion).
        assertTrue(gen.contains("String getFormulaDescription();"),
                "a java.lang type itself (String) stays BARE — only a colliding MODEL type FQNs");
        assertFalse(gen.contains("java.lang.String"),
                "a java.lang type must never be FQN-inlined (green-safety)");
    }

    // ==== Green-safety locks (pass either way) ====

    /**
     * THE LOAD-BEARING OVER-FIRE GUARD. A {@code java.lang} TYPE used as an attribute
     * ({@code Integer getMaxNoOfTradingDays()}, {@code Integer.class} in process) must stay BARE — the
     * gate FQNs only a MODEL type colliding with java.lang, NOT a java.lang type itself (its canonical
     * IS {@code java.lang.Integer}). A NON-colliding model attribute ({@code Gearing}) also stays bare.
     * {@code EquityAccumulator} is GREEN (byte-matches golden); a package-blind gate that FQN'd every
     * {@code collidesWithJavaLang} simple name would emit {@code java.lang.Integer} → regress this +
     * ~1,200 other green fpml/iso POJOs. Passes either way (clean source renders Integer bare too).
     */
    @Test
    @EnabledIf("fpmlCellAvailable")
    void equityAccumulator_javaLangType_staysBare() {
        assertNotNull(fpmlPojoOutput, "rune-fpml POJO generation did not run — corpus unavailable?");
        String gen = fpmlPojoOutput.get("fpml/consolidated/accumulator/EquityAccumulator.java");
        assertNotNull(gen, "EquityAccumulator not generated");
        assertTrue(gen.contains("Integer getMaxNoOfTradingDays();"),
                "a java.lang type (Integer) stays BARE at the getter (over-fire guard)");
        assertTrue(gen.contains("Integer.class, getMaxNoOfTradingDays()"),
                "a java.lang type (Integer) stays BARE at the process .class");
        assertFalse(gen.contains("java.lang.Integer"),
                "a java.lang type must NEVER be FQN-inlined (the load-bearing over-fire guard)");
        assertTrue(gen.contains("Gearing getGearing();"),
                "a NON-colliding model attribute (Gearing) stays bare");
    }

    /** Green-safety: {@code EquityAccumulator} (GREEN) must stay byte-identical — no regression. */
    @Test
    @EnabledIf("fpmlCellAvailable")
    void equityAccumulator_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(fpmlPojoOutput, FPML_GOLDEN_DIR,
                "fpml/consolidated/accumulator/EquityAccumulator.java");
    }

    private static void assertByteMatchesGolden(Map<String, String> output, Path goldenDir, String path)
            throws IOException {
        assertNotNull(output, "POJO generation did not run — corpus unavailable?");
        String generated = output.get(path);
        assertNotNull(generated, "POJO not generated: " + path);
        Path goldenPath = goldenDir.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated POJO must byte-match the 9.83.0 golden: " + path);
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
