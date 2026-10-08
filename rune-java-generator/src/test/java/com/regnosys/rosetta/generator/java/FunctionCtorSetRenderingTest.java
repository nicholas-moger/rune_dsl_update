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
 * Facet {@code ctor_set_rendering} — a RENDERER-ONLY rewrite of
 * {@code ConstructionHandler.handle(RConstructorExpr)} replacing the M7b-1
 * placeholder (one-line {@code Type.builder().setX(<raw Mapper value>).build()} +
 * spurious trailing {@code .get()}) with the upstream 9.83.0 multi-line typed
 * builder block whose setter arguments are coerced to the ATTRIBUTE's item type:
 *
 * <pre>
 *   lhs = toBuilder(Type.builder()
 *       .setX(&lt;coerced value&gt;)
 *       .build());
 * </pre>
 *
 * <p>Upstream reference (in-tree 9.83.0 {@code ExpressionGenerator.caseConstructorExpression}
 * + {@code evaluateConstructorValue}): each pair value is generated against the expected
 * type {@code isMulti ? List<Item> : Item}, the setter is {@code SET} or {@code SET_VALUE}
 * (meta attribute + meta-free value), and the whole block is TYPED as the POJO class —
 * so no Mapper unwrap is appended.
 *
 * <p>The fork's per-pair coercion ladder (attribute-driven, mirroring what the
 * expected-type coercion produces in the goldens):
 * <ul>
 *   <li>single attr + structurally-unwrappable value (wrap-factory MapperS/MapperC) →
 *       the RAW inner (bare param, bare {@code fn.evaluate(...)} call, literal —
 *       with {@code (long)} cast when the attr's Java type is {@code Long});</li>
 *   <li>single attr + Mapper chain → {@code <chain>.get()};</li>
 *   <li>multi attr + structurally-unwrappable MULTI value → {@code new ArrayList(<raw>)}
 *       (+ {@code java.util.ArrayList} import) — unless the value's item type is
 *       provably non-pojo, which splices the raw value bare (facet
 *       {@code ingest_setter_value_form} arm C2r);</li>
 *   <li>multi attr + Mapper chain → {@code <chain>.getMulti()};</li>
 *   <li>meta attribute → {@code set<Name>Value} setter naming — unless the value
 *       expression provably carries attribute-meta itself, which keeps the PLAIN
 *       setter (facet {@code ingest_setter_value_form} arm C1);</li>
 *   <li>anything else → the WHOLE constructor declines to the byte-identical legacy
 *       placeholder (zero regression; the file stays waivered).</li>
 * </ul>
 *
 * <p>This test is REVERT-VERIFIED RED: reverting the {@code ConstructionHandler}
 * rewrite reverts every anchor to the one-line {@code .build().get()} shape. Anchors
 * are WHOLE-FILE byte comparisons against the frozen goldens (newline-normalized),
 * generated through the REAL {@link D11CorpusRegressionTest#loadCellCorpusCached}:
 * <ul>
 *   <li>cdm/6.20.6 {@code MapDateToAdjustableDate} — META attribute {@code SET_VALUE}
 *       naming ({@code .setAdjustedDateValue(fpmlDateList)}) + raw-ident arg + the
 *       MapperS import drop;</li>
 *   <li>drr/6.34.1 {@code Create_RegimeReportableCollateral} — Mapper-chain args with
 *       {@code .get()} + multi attr {@code new ArrayList(regimeReportingSide)} +
 *       {@code java.util.ArrayList} import;</li>
 *   <li>drr/6.34.1 asic {@code Create_TradeReportHeader} — bare {@code fn.evaluate(...)}
 *       call args (structural unwrap) + {@code .setNbRcrds((long) 1)} numeric cast;</li>
 *   <li>drr/6.34.1 hkma dtcc {@code Create_TradeReportHeader} — the plain
 *       {@code .setNbRcrds(1)} variant (non-Long attr).</li>
 * </ul>
 */
class FunctionCtorSetRenderingTest {

    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM5_DEP_ROSETTA_DIR =
            Path.of("../test-corpus/cdm/cdm-5.38.0/rosetta-source/src/main/rosetta");
    private static final Path ISO_DEP_ROSETTA_DIR =
            Path.of("../test-corpus/iso20022/iso20022-1.38.0/rosetta-source/src/main/rosetta");

    private static Map<String, String> cdm6FunctionOutput;
    private static Map<String, String> drrFunctionOutput;

    static boolean cdm6CellAvailable() {
        return Files.isDirectory(CDM6_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM6_GOLDEN_DIR);
    }

    static boolean drrCellAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR)
                && Files.isDirectory(CDM5_DEP_ROSETTA_DIR)
                && Files.isDirectory(ISO_DEP_ROSETTA_DIR);
    }

    @BeforeAll
    static void generateFunctions() throws IOException {
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

    /**
     * META attribute {@code SET_VALUE} naming: {@code adjustedDate} carries
     * {@code [metadata]}, the value is the meta-free raw param — golden
     * {@code .setAdjustedDateValue(fpmlDateList)} (one setter, raw ident).
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapDateToAdjustableDate_metaSetValueNaming_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/datetime/functions/MapDateToAdjustableDate.java");
    }

    /**
     * Mapper-chain args coerced with {@code .get()} + multi attribute coerced as
     * {@code new ArrayList(regimeReportingSide)} with the {@code java.util.ArrayList}
     * import flowing through the refs channel.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void createRegimeReportableCollateral_chainGetAndArrayList_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/enrichment/common/margin/functions/Create_RegimeReportableCollateral.java");
    }

    /**
     * Bare {@code fn.evaluate(drrReport)} call args (structural unwrap of the
     * call wrap) + the {@code (long) 1} numeric cast into a Long-typed attribute.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void asicCreateTradeReportHeader_evalCallArgsAndLongCast_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/projection/iso20022/asic/rewrite/trade/functions/Create_TradeReportHeader.java");
    }

    /** The plain {@code .setNbRcrds(1)} literal variant (non-Long attribute type). */
    @Test
    @EnabledIf("drrCellAvailable")
    void hkmaDtccCreateTradeReportHeader_plainIntLiteral_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/projection/iso20022/hkma/rewrite/trade/dtcc/functions/Create_TradeReportHeader.java");
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
                + path + " — the one-line placeholder ctor shape reappears if the "
                + "ConstructionHandler typed-builder-block rewrite is reverted.");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
