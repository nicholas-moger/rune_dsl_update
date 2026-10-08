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
 * PR #226 — switchChoiceLambda. A CHOICE/TYPE-keyed {@code switch} sitting INSIDE a map-lambda body
 * ({@code <multi> extract [ item -> item switch fpml.X then Map…(item), default empty ]}) renders the
 * upstream {@code mapItem} BLOCK lambda — the flat {@code instanceof}/{@code return} ladder — instead
 * of the dormant M7b-1 {@code ControlFlowHandler.handle(RSwitchExpr)} stub's broken
 * {@code .mapSingleToItem(item -> Objects.equals(fpml.X, item) ? … : null)} chained ternary. Emitted
 * shape:
 * <pre>
 * .mapItem(item -&gt; {
 *     final CommodityLeg switchArgument = item.get();
 *     if (switchArgument == null) {
 *         return MapperS.&lt;Payout&gt;ofNull();
 *     }
 *     if (switchArgument instanceof FloatingLeg) {
 *         final FloatingLeg floatingLeg = (FloatingLeg) switchArgument;
 *         return MapperS.of(mapFloatingLegToCommodityPayout.evaluate(…, floatingLeg, …));
 *     }
 *     return MapperS.&lt;Payout&gt;ofNull();
 * }).getMulti()
 * </pre>
 * The mapper method is {@code mapItem} (not {@code mapSingleToItem}): the receiver is the multi alias
 * the legacy cardinality computer hard-codes SINGLE ({@code RShortcut}), so the in-lambda render
 * resolves the alias wrapper kind ({@code NavigationHandler.aliasReceiverProvesMulti} →
 * {@code inferShortcutIsMulti}). The case subject ({@code item}, narrowed to the case type) re-roots
 * onto the cast case var via the SAME {@code JavaStatementScope.bindSwitchSubject} binding PR #221
 * uses (the switch is the lambda body, so its subject reaches the case bodies). The choice-type gate
 * + per-case NAME-guard TYPE resolution are the shared {@code ChoiceSwitchSupport} (one source of
 * truth with PR #221's function-body seat).
 *
 * <p><b>Green-safe by construction:</b> the dormant stub's
 * {@code Objects.equals(<Type>, item) ? … : null} form is a COMPILE ERROR (a type literal compared to
 * a Mapper by {@code Objects.equals}), so every in-lambda choice-switch carrier was already a waivered
 * mismatch — the block-lambda can only turn a waivered red into golden-identical, never green&rarr;red.
 *
 * <p>Whole-file byte anchors through the REAL D11 loader over the 3 CLEAN carriers (all cdm6;
 * single-case {@code default empty} switches whose case bodies reference the subject ONLY as a direct
 * evaluate-arg). REVERT-VERIFIED RED (reverting the {@code compileChoiceSwitchBlockLambda} branch
 * reverts these anchors to the bare {@code mapSingleToItem(item -> Objects.equals(…) ? … : null)}
 * non-compiling ternary).
 */
class FunctionSwitchLambdaBlockTest {

    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR = CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static Map<String, String> cdm6FunctionOutput;

    static boolean cellsAvailable() {
        return Files.isDirectory(CDM6_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM6_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (cellsAvailable()) {
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

    @Test
    @EnabledIf("cellsAvailable")
    void cdm6_mapCommoditySwapLegListToPayoutList_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/product/commodityswap/functions/MapCommoditySwapLegListToPayoutList.java");
    }

    @Test
    @EnabledIf("cellsAvailable")
    void cdm6_mapCommoditySwapPriceQuantityList_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/product/commodityswap/functions/MapCommoditySwapPriceQuantityList.java");
    }

    @Test
    @EnabledIf("cellsAvailable")
    void cdm6_mapCommoditySwaptionPriceQuantityList_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/product/commodityswaption/functions/MapCommoditySwaptionPriceQuantityList.java");
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
                + " — a choice/type-keyed switch inside a map lambda must render the upstream "
                + "`.mapItem(item -> { final <Choice> switchArgument = item.get(); if (switchArgument "
                + "== null) { return MapperS.<R>ofNull(); } if (switchArgument instanceof X) { final X x "
                + "= (X) switchArgument; return MapperS.of(<case fn>); } return MapperS.<R>ofNull(); })` "
                + "block lambda (NOT the dormant mapSingleToItem Objects.equals(<type>, item) ? … : null "
                + "ternary), with the subject re-rooted onto the cast case var. Reverting the "
                + "compileChoiceSwitchBlockLambda branch reverts this anchor to RED.");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
