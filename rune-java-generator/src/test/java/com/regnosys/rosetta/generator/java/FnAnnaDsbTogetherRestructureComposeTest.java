package com.regnosys.rosetta.generator.java;

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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PR #399 — the AnnaDsb-FRE together-restructure (THE ENDGAME FLIP:
 * Create_AnnaDsbUpiRequestFromReportableEventAndUnderlying [drr FUNCTION, dl 465 — the
 * LAST waiverable file] byte-identical; divergent 1 = the PERMANENT only; the drr
 * FUNCTION cell EMPTIES — every FUNCTION cell now zero; functions TRUE 100%).
 *
 * <p><b>condChainRungHoist</b> (ControlFlowHandler + FunctionExpressionRenderer): a
 * nested-rung CONDITION that is a hoistable ctl-free then-chain compiles through the
 * arm renderer's FULL-sink relay (the #375-A4 pattern at the CONDITION seat) — the
 * {@code final MapperC<ReferenceWithMetaProductIdentifier> thenArg7 = …;} decl relays
 * onto the ARM-DEREF sink (the #364 pre-{@code if} window drains it at the frame
 * depth) and the re-rooted multi-line consumer condition re-anchors (the commodity
 * swap/option {@code if (thenArg7\n\t.first()\n\t.mapSingleToItem(item ->
 * exists(item.<ProductIdentifier>map("Type coercion", …)…).asMapper())
 * .getOrDefault(false))} — the ProductIdentifier witness rides the re-root).
 *
 * <p><b>fnArgChainArmHoist</b> (FunctionExpressionRenderer + CollectionHandler): a
 * fn-call/generic pathed-ladder arm whose subtree carries a hoistable ctl-free
 * then-chain compiles under the #397 ctor-arm full sink (armScope ITSELF — the
 * method-wide-numbering law), so the compute_IndexTermValue evaluate-arg chains hoist
 * thenArg18..21 with in-branch relocation.
 *
 * <p><b>relayedBareFnConsumerWrap</b> (FunctionExpressionRenderer): a RESTRUCTURED
 * chain whose consumer compiled to the exact {@code MapperS.of(<inner>)} wrap keeps
 * the wrap + {@code .get()} at the assignment ({@code ifThenElseResult15 =
 * MapperS.of(translatePeriodEnum.evaluate(thenArg22…)).get();}).
 *
 * <p><b>pathedLadderArmSeatRegistration</b> (ControlFlowHandler): appendConditionalChain
 * registers its owning rung on the #354 blockArmSeatConditionals channel around BOTH
 * arm renders (FUNCTION path) — arm-interior extract nested-then/ladder conditionals
 * block-convert through the EXISTING isCleanLadderContext transparency (the
 * ifThenElseResult14/15 else-arm ternary→block conversions).
 *
 * <p><b>nestedBlockCondChainHoist + nestedBlockArmChainHoist</b> (CollectionHandler):
 * compileNestedConditionalBlock pushes the #379 crThenCondWindow around each nested
 * condition compile (the node itself is the window rung) and lifts the block's own
 * value-hoist suppression for chain-carrying arms — the pendings drain in-block before
 * the {@code if}/{@code return} and multi-line hoisted conditions re-anchor (the
 * isCapFloor nest: {@code _boolean} + {@code _thenArg0..2} + block returns).
 *
 * <p><b>fnBareInvokableArgMetaDeref + fnLadderArmEvalArgDerefPull</b> (ReferenceHandler
 * + CollectionHandler): the FUNCTION direct then-body call routes the LAMBDA channel
 * (the #339-relayed consumer's implicit META arg derefs in-lambda — the DIGITAL
 * settlementCurrency arm's {@code _fieldWithMetaString1}) and BOTH meta-deref classes
 * pull in-rung under a thenArg-BOUND then-body (the consumer-side #391 signal — the
 * first settlementCurrency arm's {@code _fieldWithMetaString0} in-branch relocation).
 *
 * <p><b>inlineCollapseMetaElementStamp</b> (CollectionHandler): a FIRST/LAST collapse
 * over a META-wrapper element stamps {@code MapperS<wrapper>} so the follow-on nav's
 * receiver coercion emits the standard guarded deref ({@code .first()
 * .<NonNegativeQuantitySchedule>map("Type coercion", …getValue()).<UnitType>map(…)}).
 *
 * <p><b>methodGroupBodyTransparency</b> (JavaStatementScope): the unified-naming replay
 * maps NON-lambda deeper Body descendants to their parent's counterpart — decls minted
 * on arm/chain child scopes (the #354/#375/#397 relocation seats) join the ONE
 * method-wide group in text order (fieldWithMetaString0..14 across the FX/commodity
 * arms) while lambda boundaries keep per-lambda groups + ancestor escapes
 * ({@code _referenceWithMetaProductIdentifier}).
 *
 * <p>1 whole-file byte lock through the REAL D11 FUNCTION route + 13 occurrence-counted
 * witness tokens PRE-counted against f-probe-398post, ALL PRE 0 / GOLD ≥ 1
 * (witness399.py; the #352 str.count law).
 */
class FnAnnaDsbTogetherRestructureComposeTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static final String ANNA_DSB_FRE =
            "drr/enrichment/upi/functions/"
                    + "Create_AnnaDsbUpiRequestFromReportableEventAndUnderlying.java";

    private static Map<String, String> drrFnOutput;

    static boolean cellsAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (cellsAvailable()) {
            drrFnOutput = generateFunctions(
                    new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT));
        }
    }

    private static Map<String, String> generateFunctions(D11CorpusRegressionTest.CellSpec cell)
            throws IOException {
        var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(),
                D11CorpusRegressionTest.emissionFilter(cell));
        var typeUtil = new JavaTypeUtil();
        var funcGen = new FunctionGenerator(gm, new JavaTypeTranslator(typeUtil), typeUtil);
        Map<String, String> output = new LinkedHashMap<>();
        var errors = funcGen.generateWithErrors(output);
        if (!errors.isEmpty()) {
            System.err.println("[" + cell.corpus() + "-" + cell.version()
                    + " FUNCTION] generation errors tolerated (D11 parity): " + errors.size());
        }
        return output;
    }

    // ==== the byte lock (THE ENDGAME FLIP through the REAL D11 route) ====

    /** the whole-function together-restructure carrier — the LAST waiverable file. */
    @Test
    @EnabledIf("cellsAvailable")
    void annaDsbFre_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFnOutput, DRR_GOLDEN_DIR, ANNA_DSB_FRE);
    }

    // ==== occurrence-counted witnesses (PRE 0 / GOLD >= 1 — witness399.py) ====

    /** condChainRungHoist + fnArgChainArmHoist: the statement-seat chain hoists. */
    @Test
    @EnabledIf("cellsAvailable")
    void statementSeatChainHoists_witness() {
        String gen = gen(drrFnOutput, ANNA_DSB_FRE);
        assertEquals(1, count(gen,
                "final MapperC<ReferenceWithMetaProductIdentifier> thenArg7 = "
                        + "economicTerms(reportableEvent)"),
                "the commodity condition-chain hoist decl (PRE 0 / GOLD 1)");
        assertEquals(2, count(gen,
                ".mapSingleToItem(item -> exists(item.<ProductIdentifier>map(\"Type coercion\", "
                        + "_referenceWithMetaProductIdentifier"),
                "the re-rooted exists consumer with the ProductIdentifier witness at both "
                        + "commodity arms (PRE 0 / GOLD 2)");
        assertEquals(1, count(gen,
                "final MapperC<InterestRatePayout> thenArg18 = economicTerms(reportableEvent)"),
                "the compute_IndexTermValue evaluate-arg hoist (PRE 0 / GOLD 1)");
        assertEquals(1, count(gen, "compute_IndexTermValue.evaluate(MapperS.of(thenArg18.get())"),
                "the bare-item collapse re-root at the evaluate seat (PRE 0 / GOLD 1)");
    }

    /** nestedBlock cond/arm chain hoists + pathedLadderArmSeatRegistration. */
    @Test
    @EnabledIf("cellsAvailable")
    void nestedBlockHoists_witness() {
        String gen = gen(drrFnOutput, ANNA_DSB_FRE);
        assertEquals(1, count(gen, "if (exists(_thenArg0"),
                "the isCapFloor nest's hoisted CR condition (PRE 0 / GOLD 1)");
        assertEquals(1, count(gen, "return MapperS.of(compute_IndexTermValue.evaluate(_thenArg1"),
                "the in-block evaluate-arg hoists' consumer (PRE 0 / GOLD 1)");
        assertEquals(2, count(gen, "CardinalityOperator.Any))).getOrDefault(false)) {"),
                "the ifThenElseResult14/15 else-arm ternary→block if-headers "
                        + "(PRE 0 / GOLD 2)");
    }

    /** relayedBareFnConsumerWrap + fnBareInvokableArgMetaDeref + the collapse stamp. */
    @Test
    @EnabledIf("cellsAvailable")
    void consumerWrapAndDerefs_witness() {
        String gen = gen(drrFnOutput, ANNA_DSB_FRE);
        assertEquals(1, count(gen,
                "ifThenElseResult15 = MapperS.of(translatePeriodEnum.evaluate(thenArg22"),
                "the MapperS.of wrap-keep + .get() at the relayed arm (PRE 0 / GOLD 1)");
        assertEquals(1, count(gen,
                "final FieldWithMetaString _fieldWithMetaString0 = item.<FieldWithMetaString>"
                        + "map(\"getSettlementCurrency\""),
                "the settlementCurrency in-branch relocation (PRE 0 / GOLD 1)");
        assertEquals(1, count(gen, "final FieldWithMetaString _fieldWithMetaString1 = thenArg.get();"),
                "the DIGITAL arm's implicit-arg meta-deref (PRE 0 / GOLD 1)");
        assertEquals(1, count(gen,
                ".first().<NonNegativeQuantitySchedule>map(\"Type coercion\", "
                        + "fieldWithMetaNonNegativeQuantitySchedule"),
                "the first()-collapse coercion witness (PRE 0 / GOLD 1)");
    }

    /** methodGroupBodyTransparency: the method-wide naming group + the ripple tail. */
    @Test
    @EnabledIf("cellsAvailable")
    void methodGroupNaming_witness() {
        String gen = gen(drrFnOutput, ANNA_DSB_FRE);
        assertEquals(1, count(gen, "final FieldWithMetaString fieldWithMetaString4 = thenArg5"),
                "the FX arm's hoist joins the METHOD group (PRE 0 / GOLD 1 — the pre state "
                        + "escaped `_fieldWithMetaString0` per-arm)");
        assertEquals(1, count(gen, "fieldWithMetaString14 == null"),
                "the isdaTaxonomy coercion-param ripple tail (PRE 0 / GOLD 1)");
    }

    private static int count(String s, String token) {
        int n = 0;
        for (int i = s.indexOf(token); i >= 0; i = s.indexOf(token, i + token.length())) {
            n++;
        }
        return n;
    }

    private static String gen(Map<String, String> output, String path) {
        assertNotNull(output, "generation did not run — corpus unavailable?");
        String gen = output.get(path);
        assertNotNull(gen, "missing generated file: " + path);
        return gen;
    }

    private static void assertByteMatchesGolden(Map<String, String> output, Path goldenDir,
            String path) throws IOException {
        String generated = gen(output, path);
        Path goldenPath = goldenDir.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(golden.replace("\r\n", "\n"), generated.replace("\r\n", "\n"),
                "byte-identity REGRESSED: " + path);
    }
}
