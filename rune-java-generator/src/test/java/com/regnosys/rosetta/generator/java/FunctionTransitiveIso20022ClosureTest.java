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
 * PR #155 (facet {@code drr_transitive_iso20022_closure}) — the D11 DRR cell
 * loader surfaces the transitive {@code org.iso20022:rosetta-source} {@code .rosetta}
 * closure into the workspace (resolution-only), mirroring the upstream DRR Maven build
 * which unpacks that dependency's sources into
 * {@code target/parent-dependency/iso20022/rosetta} and compiles them as Xtext sources
 * alongside the cell's own models (drr rosetta-source/pom.xml, maven-dependency-plugin
 * {@code unpack} + xtext-maven-plugin source roots).
 *
 * <p>Without the closure, every {@code iso20022.auth030.*} reference in the drr
 * projection functions is UNRESOLVED: a function output declared as an iso20022 type
 * alias ({@code MICIdentifier} → {@code String}, {@code ISODate} → {@code Date},
 * {@code TrueFalseIndicator} → {@code Boolean}) erases to {@code Object}, and an
 * iso20022 Document-model-typed output ({@code OrganisationIdentification15Choice__1},
 * …) additionally loses its builder/validator scaffolding ({@code ModelObjectValidator}
 * inject + {@code build()} + {@code objectValidator.validate(...)}) and its imports.
 *
 * <p>This test is REVERT-VERIFIED RED against the loader itself: it generates through
 * {@link D11CorpusRegressionTest#loadCellCorpusCached} (the REAL D11 loader, not a
 * mirrored copy), so reverting the transitive-ISO20022 walk in
 * {@code D11CorpusRegressionTest.loadCellCorpus} turns every anchor RED with the
 * {@code Object}-erased shape.
 *
 * <p>Anchors are WHOLE-FILE byte comparisons against the frozen drr/6.34.1 goldens
 * (newline-normalized) — fragment assertions are insufficient per the PR #153 lesson
 * (a fragment test passed while the file missed an import and never byte-matched):
 * <ul>
 *   <li>{@code asic .../GetPltfmIdr} — output {@code MICIdentifier} (unqualified
 *       cross-namespace alias) → {@code String};</li>
 *   <li>{@code hkma dtcc .../GetFctvDt} — output
 *       {@code iso20022.auth030.hkma.dtcc.ISODate} (namespace-QUALIFIED alias) →
 *       {@code Date};</li>
 *   <li>{@code esma .../GetPstTradRskRdctnFlg} — alias → {@code Boolean};</li>
 *   <li>{@code asic .../GetSubmitgAgt} — iso20022 Document model type
 *       ({@code OrganisationIdentification15Choice__1}): full builder/validator
 *       scaffolding + imports recovered.</li>
 * </ul>
 */
class FunctionTransitiveIso20022ClosureTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM_DEP_ROSETTA_DIR =
            Path.of("../test-corpus/cdm/cdm-5.38.0/rosetta-source/src/main/rosetta");
    private static final Path ISO_DEP_ROSETTA_DIR =
            Path.of("../test-corpus/iso20022/iso20022-1.38.0/rosetta-source/src/main/rosetta");

    private static Map<String, String> functionOutput;

    static boolean drrCellAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR)
                && Files.isDirectory(CDM_DEP_ROSETTA_DIR)
                && Files.isDirectory(ISO_DEP_ROSETTA_DIR);
    }

    @BeforeAll
    static void generateDrrFunctions() throws IOException {
        if (!drrCellAvailable()) return;
        var cell = new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT);
        // The REAL D11 loader (cached; shares the workspace with a same-JVM D11 run) —
        // builtins + transitive-CDM 5.38.0 closure + transitive-ISO20022 1.38.0 closure
        // + the drr cell's own version-stamped models.
        var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(),
                D11CorpusRegressionTest.emissionFilter(cell));
        var typeUtil = new JavaTypeUtil();
        var gen = new FunctionGenerator(gm, new JavaTypeTranslator(typeUtil), typeUtil);
        functionOutput = new LinkedHashMap<>();
        List<GenerationException> genErrors = gen.generateWithErrors(functionOutput);
        assertTrue(genErrors.isEmpty(),
                "drr FUNCTION generation reported errors: " + genErrors);
    }

    /** Unqualified cross-namespace alias output: {@code MICIdentifier} → {@code String}. */
    @Test
    @EnabledIf("drrCellAvailable")
    void getPltfmIdr_micIdentifierAliasOutput_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/projection/iso20022/asic/rewrite/trade/functions/GetPltfmIdr.java");
    }

    /** Namespace-QUALIFIED alias output: {@code iso20022.auth030.hkma.dtcc.ISODate} → {@code Date}. */
    @Test
    @EnabledIf("drrCellAvailable")
    void getFctvDt_qualifiedIsoDateAliasOutput_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/projection/iso20022/hkma/rewrite/trade/dtcc/functions/GetFctvDt.java");
    }

    /** Boolean-alias output ({@code TrueFalseIndicator}-family) → {@code Boolean}. */
    @Test
    @EnabledIf("drrCellAvailable")
    void getPstTradRskRdctnFlg_booleanAliasOutput_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/projection/iso20022/esma/emir/refit/trade/functions/GetPstTradRskRdctnFlg.java");
    }

    /**
     * iso20022 Document-model-typed output — the full builder/validator scaffolding
     * ({@code ModelObjectValidator} inject, {@code doEvaluate} returning the Builder,
     * {@code build()} + {@code objectValidator.validate(...)}) and the
     * {@code iso20022.auth030.asic.*} imports are recovered, not just the type name.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void getSubmitgAgt_documentModelTypedOutput_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/projection/iso20022/asic/rewrite/trade/functions/GetSubmitgAgt.java");
    }

    private static void assertByteMatchesGolden(String path) throws IOException {
        assertNotNull(functionOutput, "Function generation did not run — corpus unavailable?");
        String generated = functionOutput.get(path);
        assertNotNull(generated, "Function not generated: " + path);
        Path goldenPath = DRR_GOLDEN_DIR.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated output must byte-match the drr/6.34.1 golden (newline-normalized) "
                + "for " + path + " — the Object-erased shape reappears if the D11 loader's "
                + "transitive-ISO20022 closure is reverted.");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
