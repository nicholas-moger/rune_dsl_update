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
 * PR #222 — the composed clean-tail bundle (three disjoint, green-safe GENERATOR
 * mechanisms; 8 FUNCTION flips, all cdm). Each anchor byte-matches the 9.83.0 golden
 * and reverts to RED if its mechanism is removed.
 *
 * <ol>
 *   <li><b>inlineThen VALUE-then ADD seat</b> — a whole-output ADD
 *       ({@code add <out>: <chain> then <value-body>}) hoists the upstream
 *       {@code final Mapper*<X> thenArg = <chain>; <out>.addAll(<body>.getMulti());}
 *       via {@code FunctionExpressionRenderer.renderThenExtractSet} + an {@code isAdd}
 *       branch, gated to a PLAIN (non-conditional/switch) base
 *       ({@code hasPlainThenBase}) so the cascade-safe top-level seat fires but a
 *       conditional-base then declines. (GetQuantityScheduleStepValues.)</li>
 *   <li><b>conditionValidatorBlockLambda</b> — a flat conditional validate condition
 *       ({@code if <cond> then <then>}, ComparisonResult then, no effective else)
 *       renders the block-lambda body
 *       {@code { if (<cond>.getOrDefault(false)) { return <then>; } return ComparisonResult.ofEmpty(); }}
 *       at the splice depth threaded from {@code FunctionGenerator}, instead of the
 *       non-compiling inline ternary. (Create_ContractFormationInstruction,
 *       NewFloatingPayout.)</li>
 *   <li><b>checkedMapEnumMetaWrap</b> — a bare-enum {@code to-enum} value SET into a
 *       {@code FieldWithMeta<Enum>} output constructs the wrapper with a null-guard
 *       ({@code final <Enum> e = …; if (e==null){…builder().build()…} else {…setValue(e)…}})
 *       instead of {@code toBuilder(<bareEnum>)}; gated to an {@code RConversionExpr}
 *       value (NOT a {@code with-meta} construction). (MapDayDistributionEnumWithScheme,
 *       MapGoverningLaw.)</li>
 * </ol>
 *
 * <p>All three are green-safe by construction: the fork's pre-fix form does not compile
 * (no runtime {@code Mapper.then(Function)}; a {@code ComparisonResult}/{@code MapperC}
 * mixed ternary; a bare enum passed to a {@code FieldWithMetaEnum} builder), so every
 * carrier was already a waivered mismatch.
 */
class FunctionInlineThenValueTailTest {

    private static final Path CDM5_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-5.38.0");
    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM5_GOLDEN_DIR = CDM5_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM6_GOLDEN_DIR = CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static Map<String, String> cdm5FunctionOutput;
    private static Map<String, String> cdm6FunctionOutput;

    static boolean cellsAvailable() {
        return Files.isDirectory(CDM5_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM5_GOLDEN_DIR)
                && Files.isDirectory(CDM6_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM6_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (cellsAvailable()) {
            cdm5FunctionOutput = generateCell(new D11CorpusRegressionTest.CellSpec("cdm", "5.38.0", CDM5_CELL_ROOT));
            cdm6FunctionOutput = generateCell(new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CDM6_CELL_ROOT));
        }
    }

    private static Map<String, String> generateCell(D11CorpusRegressionTest.CellSpec cell) throws IOException {
        var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(), D11CorpusRegressionTest.emissionFilter(cell));
        var typeUtil = new JavaTypeUtil();
        var gen = new FunctionGenerator(gm, new JavaTypeTranslator(typeUtil), typeUtil);
        Map<String, String> output = new LinkedHashMap<>();
        List<GenerationException> genErrors = gen.generateWithErrors(output);
        assertTrue(genErrors.isEmpty(), cell + " FUNCTION generation reported errors: " + genErrors);
        return output;
    }

    // ---- (1) inlineThen VALUE-then ADD seat ----

    @Test
    @EnabledIf("cellsAvailable")
    void cdm5_getQuantityScheduleStepValues_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5FunctionOutput, CDM5_GOLDEN_DIR,
                "cdm/product/asset/calculation/functions/GetQuantityScheduleStepValues.java");
    }

    @Test
    @EnabledIf("cellsAvailable")
    void cdm6_getQuantityScheduleStepValues_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/product/asset/calculation/functions/GetQuantityScheduleStepValues.java");
    }

    // ---- (2) conditionValidatorBlockLambda ----

    @Test
    @EnabledIf("cellsAvailable")
    void cdm5_createContractFormationInstruction_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5FunctionOutput, CDM5_GOLDEN_DIR,
                "cdm/event/common/functions/Create_ContractFormationInstruction.java");
    }

    @Test
    @EnabledIf("cellsAvailable")
    void cdm6_createContractFormationInstruction_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/event/common/functions/Create_ContractFormationInstruction.java");
    }

    @Test
    @EnabledIf("cellsAvailable")
    void cdm5_newFloatingPayout_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5FunctionOutput, CDM5_GOLDEN_DIR,
                "cdm/event/common/functions/NewFloatingPayout.java");
    }

    @Test
    @EnabledIf("cellsAvailable")
    void cdm6_newFloatingPayout_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/event/common/functions/NewFloatingPayout.java");
    }

    // ---- (3) checkedMapEnumMetaWrap ----

    @Test
    @EnabledIf("cellsAvailable")
    void cdm6_mapDayDistributionEnumWithScheme_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/datetime/functions/MapDayDistributionEnumWithScheme.java");
    }

    @Test
    @EnabledIf("cellsAvailable")
    void cdm6_mapGoverningLaw_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/legal/functions/MapGoverningLaw.java");
    }

    private static void assertByteMatchesGolden(Map<String, String> cellOutput, Path goldenDir, String path)
            throws IOException {
        assertNotNull(cellOutput, "Function generation did not run — corpus unavailable?");
        String generated = cellOutput.get(path);
        assertNotNull(generated, "Function not generated: " + path);
        Path goldenPath = goldenDir.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated output must byte-match the golden (newline-normalized) for " + path
                + " — PR #222 composed clean-tail bundle (inlineThen ADD seat / "
                + "conditionValidatorBlockLambda / checkedMapEnumMetaWrap). Reverting the "
                + "mechanism reverts this anchor to the fork's non-compiling pre-fix form (RED).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
