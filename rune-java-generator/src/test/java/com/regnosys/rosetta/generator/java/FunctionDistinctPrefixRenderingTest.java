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
 * Facet {@code distinct_prefix_rendering} — upstream renders every DISTINCT
 * list operation as the PREFIX runtime function {@code distinct(<chain>)}, not
 * a postfix member call.
 *
 * <ol>
 *   <li><b>Arm 1 — prefix runtime-fn rendering.</b> Upstream
 *       {@code ExpressionGenerator.caseDistinctOperation} (xtend:737-743)
 *       routes DISTINCT through {@code applyRuntimeMethod('distinct', ...)},
 *       which emits the prefix call carried by the
 *       {@code ExpressionOperatorsNullSafe.*} static wildcard import — the
 *       runtime fn is {@code ExpressionOperatorsNullSafe.distinct(Mapper)};
 *       the {@code Mapper} classes have NO {@code distinct} member, so the
 *       fork's postfix {@code <chain>.distinct()} fall-through (the
 *       documented CollectionHandler deferral) rendered non-compiling Java.
 *       Corpus law: prefix {@code distinct(} appears in 150 golden files and
 *       150/150 carry the {@code ExpressionOperatorsNullSafe.*} wildcard;
 *       postfix {@code .distinct()} appears in ZERO generated goldens
 *       (the only test-corpus hits are hand-written ingest/processor
 *       helpers using Java Stream).</li>
 *   <li><b>Arm 2 — alias-signature DISTINCT walk-through.</b> The #167
 *       alias-signature type walk declined every DISTINCT (then "no corpus
 *       witness needs them"), falling to the function-output fallback that
 *       rendered the non-compiling {@code MapperS<? extends boolean>} where
 *       golden has {@code MapperS<Date>} (drr {@code IsActionTypePositionMODI}
 *       — a Boolean-output function whose alias is
 *       {@code ... -> openDateTime -> date distinct only-element}). DISTINCT
 *       now walks through to its operand's item type (multi preserved; the
 *       consuming ONLY_ELEMENT stamps single), with the operand's built-in
 *       {@code date} RECORD-FEATURE leaf — no {@code RAttribute} exists, the
 *       same gap the body-side PR #147 {@code date_record_feature_nav} arm
 *       fills — typed as the {@code Date} witness ONLY inside the DISTINCT
 *       walk: an unscoped record-feature arm could re-type aliases in
 *       currently-green files whose signatures come from the byte-frozen
 *       fallback.</li>
 * </ol>
 *
 * <p>Green-safety (dual argument): every firing shape rendered non-compiling
 * Java pre-fix (no {@code distinct} member on Mapper; {@code ? extends} over
 * a primitive type name), so no byte-matching file can carry one; and the
 * corpus law is total — ZERO golden postfix occurrences, so the prefix arm is
 * the upstream law verbatim with no decline ladder.
 *
 * <p>Anchors are WHOLE-FILE byte comparisons against the frozen goldens
 * (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}:
 * <ul>
 *   <li>cdm/5.38.0 + cdm/6.20.6 {@code DifferentOrdinalsCondition} — arm 1
 *       alone (the comparison-operand {@code distinct(item.<...>mapC(...))}
 *       inside a mapSingleToItem lambda; the wf168 law.md §5 Lead #1
 *       prediction);</li>
 *   <li>drr/6.34.1 {@code IsActionTypePositionMODI} — arms 1+2 in one file
 *       (prefix render in the alias body + the {@code MapperS<Date>}
 *       signature pair from the DISTINCT walk-through over to-date).</li>
 * </ul>
 */
class FunctionDistinctPrefixRenderingTest {

    private static final Path CDM5_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-5.38.0");
    private static final Path CDM5_GOLDEN_DIR =
            CDM5_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static Map<String, String> cdm5FunctionOutput;
    private static Map<String, String> cdm6FunctionOutput;
    private static Map<String, String> drrFunctionOutput;

    static boolean cdm5CellAvailable() {
        return Files.isDirectory(CDM5_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM5_GOLDEN_DIR);
    }

    static boolean cdm6CellAvailable() {
        return Files.isDirectory(CDM6_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM6_GOLDEN_DIR);
    }

    static boolean drrCellAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR);
    }

    @BeforeAll
    static void generateFunctions() throws IOException {
        if (cdm5CellAvailable()) {
            cdm5FunctionOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("cdm", "5.38.0", CDM5_CELL_ROOT));
        }
        if (cdm6CellAvailable()) {
            cdm6FunctionOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CDM6_CELL_ROOT));
        }
        if (drrCellAvailable()) {
            drrFunctionOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT));
        }
    }

    private static Map<String, String> generateCell(D11CorpusRegressionTest.CellSpec cell)
            throws IOException {
        var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(),
                D11CorpusRegressionTest.emissionFilter(cell));
        var typeUtil = new JavaTypeUtil();
        var gen = new FunctionGenerator(gm, new JavaTypeTranslator(typeUtil), typeUtil);
        Map<String, String> output = new LinkedHashMap<>();
        List<GenerationException> genErrors = gen.generateWithErrors(output);
        assertTrue(genErrors.isEmpty(),
                cell + " FUNCTION generation reported errors: " + genErrors);
        return output;
    }

    /** Arm 1: comparison-operand DISTINCT renders prefix {@code distinct(<chain>)} (cdm5). */
    @Test
    @EnabledIf("cdm5CellAvailable")
    void differentOrdinalsCdm5_distinctPrefix_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5FunctionOutput, CDM5_GOLDEN_DIR,
                "cdm/base/staticdata/asset/common/functions/DifferentOrdinalsCondition.java");
    }

    /** Arm 1: the identical shape in the cdm6 cell (free version-pair pin). */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void differentOrdinalsCdm6_distinctPrefix_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/base/staticdata/asset/common/functions/DifferentOrdinalsCondition.java");
    }

    /** Arms 1+2: alias-body prefix render + the MapperS&lt;Date&gt; signature walk-through. */
    @Test
    @EnabledIf("drrCellAvailable")
    void isActionTypePositionMODI_distinctAliasSignature_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/regulation/common/functions/IsActionTypePositionMODI.java");
    }

    private static void assertByteMatchesGolden(Map<String, String> functionOutput,
            Path goldenDir, String path) throws IOException {
        assertNotNull(functionOutput, "Function generation did not run — corpus unavailable?");
        String generated = functionOutput.get(path);
        assertNotNull(generated, "Function not generated: " + path);
        Path goldenPath = goldenDir.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated output must byte-match the golden (newline-normalized) for "
                + path + " — DISTINCT renders the non-compiling postfix .distinct() "
                + "member call instead of the prefix ExpressionOperatorsNullSafe "
                + "runtime fn, or the alias-signature walk declines DISTINCT to the "
                + "function-output fallback, if the distinct_prefix_rendering "
                + "recovery is reverted.");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
