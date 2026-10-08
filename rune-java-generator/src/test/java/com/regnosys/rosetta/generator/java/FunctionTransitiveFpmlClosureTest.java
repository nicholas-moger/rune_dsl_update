package com.regnosys.rosetta.generator.java;

import com.regnosys.rosetta.generator.GenerationException;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PR #184 (facet {@code cdm6_transitive_fpml_load}) — the D11 CDM cell loader surfaces
 * the transitive {@code com.regnosys.rune-fpml} {@code .rosetta} closure into the cdm6
 * workspace (resolution-only), mirroring the upstream cdm-6.20.6 Maven build which
 * unpacks that dependency's sources into {@code target/parent-dependency/fpml/rosetta}
 * and compiles them as Xtext sources alongside the cell's own models (cdm-6.20.6
 * rosetta-source/pom.xml, maven-dependency-plugin {@code copy-rune-fpml-rosetta} unpack;
 * fpml.* is never fed to the generator because the rosetta-maven-plugin
 * {@code <sourceRoots>} is {@code classes/cdm/rosetta} only — so it is consumed
 * resolution-only, never emitted).
 *
 * <p>Without the closure, the {@code cdm.ingest.fpml.*} confirmation functions — which
 * take {@code fpml.consolidated.*} types as INPUT parameters — cannot resolve those
 * types, so the fork erases each such parameter to {@code Object} in the
 * {@code evaluate}/{@code doEvaluate}/{@code assignOutput} signatures and drops the
 * {@code import fpml.consolidated.*}. The generator itself is already correct; only the
 * harness under-loaded the dependency.
 *
 * <p>This test is the revert-verified lock for that loader change, mirroring
 * {@link FunctionTransitiveIso20022ClosureTest}: it generates through
 * {@link D11CorpusRegressionTest#loadCellCorpusCached} (the REAL D11 loader, not a
 * mirrored copy), so reverting the transitive-fpml walk in
 * {@code D11CorpusRegressionTest.loadCellCorpus} turns every anchor RED with the
 * {@code Object}-erased parameter shape.
 *
 * <p>Anchors are WHOLE-FILE byte comparisons against the frozen cdm/6.20.6 goldens
 * (newline-normalized) — fragment assertions are insufficient per the PR #153 lesson:
 * <ul>
 *   <li>{@code .../pricequantity/.../CreateKey} — {@code Leg fpmlLeg}
 *       ({@code fpml.consolidated.shared.Leg}), an empty-body passthrough: the cleanest
 *       signature-only flip;</li>
 *   <li>{@code .../common/.../MapProductIdentifier} — fpml input + a non-trivial body;</li>
 *   <li>{@code .../common/.../MapFxAmericanExerciseTerms} — fpml input;</li>
 *   <li>{@code .../common/.../GetFpmlEquityExercise} — fpml input.</li>
 * </ul>
 *
 * <p><b>PR #185 — fpml 1.5.3 rebaseline.</b> cdm6 pins rune-fpml <b>1.5.3</b>; PR #184
 * substituted 2.0.0 (the only version then in the corpus). But 2.0.0 had FLATTENED AWAY
 * the FpML model-group wrapper types the 1.5.3-built goldens navigate
 * ({@code fpml.consolidated.shared.BuyerSellerModel} / {@code ProductModel} et al. —
 * absent from the entire 2.0.0 tree), so ~430 ingest~fpml carriers stayed waivered as a
 * VERSION mismatch, not a generator gap. PR #185 loads the CORRECT 1.5.3 as cdm6's
 * transitive dependency, flipping a further 102 ingest~fpml FUNCTION goldens
 * BYTE-IDENTICALLY (zero regressions). Two added anchors lock the 1.5.3 requirement
 * specifically (RED if the dep reverts to 2.0.0):
 * <ul>
 *   <li>{@code .../bondoption/.../MapBondOptionCounterpartyList} — navigates the
 *       1.5.3-only {@code BuyerSellerModel} model group;</li>
 *   <li>{@code .../bondoption/.../MapBondOptionNonTransferableProduct} — navigates the
 *       1.5.3-only {@code ProductModel} model group.</li>
 * </ul>
 */
class FunctionTransitiveFpmlClosureTest {

    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path FPML_DEP_ROSETTA_DIR =
            Path.of("../test-corpus/rune-fpml/rune-fpml-1.5.3/rosetta-source/src/main/rosetta");

    private static Map<String, String> functionOutput;

    static boolean cdm6CellAvailable() {
        return Files.isDirectory(CDM6_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM6_GOLDEN_DIR)
                && Files.isDirectory(FPML_DEP_ROSETTA_DIR);
    }

    @BeforeAll
    static void generateCdm6Functions() throws IOException {
        if (!cdm6CellAvailable()) return;
        var cell = new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CDM6_CELL_ROOT);
        // The REAL D11 loader (cached; shares the workspace with a same-JVM D11 run) —
        // builtins + the transitive rune-fpml 1.5.3 closure (PR #185: the version cdm6
        // actually pins, replacing the #184 2.0.0 substitution) + the cdm6 cell's own
        // version-stamped models. emissionFilter keeps fpml.* resolution-only.
        var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(),
                D11CorpusRegressionTest.emissionFilter(cell));
        var typeUtil = new JavaTypeUtil();
        var gen = new FunctionGenerator(gm, new JavaTypeTranslator(typeUtil), typeUtil);
        functionOutput = new LinkedHashMap<>();
        List<GenerationException> genErrors = gen.generateWithErrors(functionOutput);
        assertTrue(genErrors.isEmpty(),
                "cdm6 FUNCTION generation reported errors: " + genErrors);
    }

    /** {@code fpml.consolidated.shared.Leg} input, empty-body passthrough — the cleanest signature-only flip. */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void createKey_fpmlLegInput_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "cdm/ingest/fpml/confirmation/pricequantity/functions/CreateKey.java");
    }

    /** fpml input parameter with a non-trivial body. */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapProductIdentifier_fpmlInput_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "cdm/ingest/fpml/confirmation/common/functions/MapProductIdentifier.java");
    }

    /** fpml input parameter (exercise-terms mapping). */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapFxAmericanExerciseTerms_fpmlInput_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "cdm/ingest/fpml/confirmation/common/functions/MapFxAmericanExerciseTerms.java");
    }

    /** fpml input parameter (equity-exercise getter). */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void getFpmlEquityExercise_fpmlInput_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "cdm/ingest/fpml/confirmation/common/functions/GetFpmlEquityExercise.java");
    }

    /**
     * PR #185 version-specific lock: navigates {@code fpmlBondOption -> buyerSellerModel},
     * whose type {@code fpml.consolidated.shared.BuyerSellerModel} is a model-group
     * wrapper that EXISTS in rune-fpml 1.5.3 but was FLATTENED AWAY in 2.0.0. Reverting
     * the {@link D11CorpusRegressionTest#CDM_TO_FPML_VERSION} map to 2.0.0 (or removing
     * the 1.5.3 dep) leaves this navigation unresolved — the {@code <BuyerSellerModel>}
     * map witness + its import drop and the anchor goes RED. (One of the 102 PR #185 flips.)
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapBondOptionCounterpartyList_modelGroupNav_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "cdm/ingest/fpml/confirmation/product/bondoption/functions/MapBondOptionCounterpartyList.java");
    }

    /**
     * PR #185 version-specific lock: navigates {@code fpmlBondOption -> productModel}
     * ({@code fpml.consolidated.shared.ProductModel}), another 1.5.3-only model-group
     * wrapper absent from 2.0.0. RED if the transitive dep reverts to 2.0.0.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapBondOptionNonTransferableProduct_modelGroupNav_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "cdm/ingest/fpml/confirmation/product/bondoption/functions/MapBondOptionNonTransferableProduct.java");
    }

    private static void assertByteMatchesGolden(String path) throws IOException {
        assertNotNull(functionOutput, "Function generation did not run — corpus unavailable?");
        String generated = functionOutput.get(path);
        assertNotNull(generated, "Function not generated: " + path);
        Path goldenPath = CDM6_GOLDEN_DIR.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated output must byte-match the cdm/6.20.6 golden (newline-normalized) "
                + "for " + path + " — the Object-erased fpml-parameter shape reappears if the "
                + "D11 loader's transitive-fpml closure is reverted.");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
