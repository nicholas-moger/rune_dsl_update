package com.regnosys.rosetta.generator.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.GenerationException;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.function.LabelProviderGenerator;
import com.regnosys.rosetta.generator.java.function.LabelProviderGeneratorUtil;
import com.regnosys.rosetta.generator.java.object.ChoiceObjectGenerator;
import com.regnosys.rosetta.generator.java.object.ModelObjectGenerator;
import com.regnosys.rosetta.generator.java.object.datarule.DataRuleGenerator;
import com.regnosys.rosetta.generator.java.reports.ReportGenerator;
import com.regnosys.rosetta.generator.java.reports.RuleGenerator;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.symbols.RLinkingResult;
import com.regnosys.rosetta.symbols.RWorkspace;
import com.regnosys.rosetta.utils.DeepFeatureCallUtil;

/**
 * SEAT 20 — facet {@code thenChainFnValueWrapOnce}: <b>a FUNCTION-CALL value consumed at a
 * then-chain seat — the deep-then level decl, the single-then decl, the filter receiver — carries
 * EXACTLY ONE Mapper layer: the seat adds the missing {@code MapperS.of(…)} /
 * {@code MapperC.<T>of(…)} when the render left the value bare, and adds NOTHING when the render
 * already wrapped it.</b> Upstream has no per-seat ladder: {@code evaluateCall} yields the bare
 * item/list and {@code TypeCoercionService.addCoercions} to the seat's Mapper expectation wraps it
 * once ({@code ExpressionGenerator.xtend} caseSymbolReference → callableWithArgsCall → evaluateCall;
 * {@code TypeCoercionService.xtend:585-596}).
 *
 * <p><b>The defects (two rungs, one law — LAW 69).</b>
 * <ul>
 *   <li><b>Rung E — twice.</b> {@code CollectionHandler.tryDeepThenHoist}'s #333
 *   {@code deepThenExplicitMultiFnWrap} arm wrapped an explicit multi-output fn-call level value
 *   {@code MapperC.<T>of(…)} from the AST alone; the render
 *   ({@code ReferenceHandler.tryMultiValueWrap}) already hands back the {@code MapperC.<T>of(…)}
 *   WRAP FACTORY when the callee's multi output resolves, so the fork wrote
 *   {@code MapperC.<ReportingRegime>of(MapperC.<ReportingRegime>of(extractRegimeInformation.evaluate(…)))}
 *   — 22 rows (drr 6.36–6.38 {@code NatureOfCounterparty2Rule}/{@code NatureOfTheCounterparty2Rule}
 *   + drr 7.0–7.3 {@code GetUnderlyingAssetName}, {@code DTCC_UnderlyingAssetReportRule},
 *   {@code UnderlyingIdOtherDTCCRule}, {@code UnderlyingIdOtherSourceDTCCRule}). The form compiles
 *   ({@code MapperC implements MapperBuilder}) — a parity heal. The arm now CONSULTS the render's
 *   wrap-factory truth, the very guard the #339 arm one statement later has always carried.</li>
 *   <li><b>Rung G — zero.</b> A bare no-arg FUNCTION reference renders BARE by the #308
 *   bare-invocation contract ({@code ReferenceHandler.renderImplicitFunctionInvocation}); two
 *   consumer seats added nothing: (G1) the FILTER receiver ({@code CollectionHandler.handle(RFilterExpr)})
 *   — {@code getTradeForQuantity.evaluate(input)\n\t.filterSingleNullSafe(…)}, javac
 *   cannot-find-symbol on a {@code Trade} (16 rows: drr 7.0–7.3 iosco cde v1
 *   {@code CallAmountRule}/{@code CallCurrencyRule}/{@code PutAmountRule}/{@code PutCurrencyRule});
 *   (G2) the SINGLE-then decl ({@code FunctionExpressionRenderer.renderBareInvokableThenSet}) —
 *   {@code final MapperS<Measure> thenArg = notional.evaluate(input);}, javac incompatible-types
 *   (4 rows: drr 7.0–7.3 {@code NotionalCurrencyRule}). The deep-then #339 arm has wrapped this
 *   shape since #339; the two sibling seats now consult the SAME predicate
 *   ({@code CollectionHandler.bareInvokableValueNeedsWrap}) and the SAME two forms
 *   ({@code wrapBareInvokableValue}).</li>
 * </ul>
 *
 * <p><b>LAW 75 — measured before the seat, over all 275 matrix rows (the seat-20 charter):</b> the
 * #333 arm fired on 105 sites, factory=true on EXACTLY the 22 E carriers (83 raw sites stay
 * wrapped); the filter-receiver seat had EXACTLY 16 bare-FUNCTION sites (all SINGLE, all
 * rule-top-level, ZERO bare-RULE, ZERO in-lambda); the single-then seat 4 bare-FUNCTION + 48
 * pre-wrapped bare-RULE sites; the #339 population 215 (all SINGLE) + 22 pre-wrapped bare-RULE
 * levels. The static golden census over all 179,209 goldens: ZERO single-argument
 * {@code MapperC.<T>of(MapperC.<T>of(…))}, ZERO bare-invokable values at a Mapper decl; the
 * list-literal varargs form {@code MapperC.<T>of(MapperC.<T>of(a), MapperS.of(b))} (1–5 files per
 * cell) is a DIFFERENT shape and stays.
 *
 * <p><b>RED at the pre-seat blob</b> (the seat's src/main reverted, this suite kept): exactly
 * {@code a1, a2, a3, a4, a5, a6, a7, corpus_c1, corpus_c2, corpus_c3, corpus_c4, corpus_c5,
 * corpus_control1} (21 run / 13 F); every {@code b*} and {@code control0}/{@code control2} GREEN in
 * both states. <b>LAW 66 mutations</b> (measured; the seat-20 charter §6b holds the logs): (i) the
 * E guard deleted → {@code a1, a2, a7} + the E corpus locks ({@code c1, c4, c5, control1}) RED, 21/7F;
 * (ii) the G1 arm deleted → {@code a3, a5, c2, c5, control1} RED, 21/5F; (iii) the G2 arm deleted →
 * {@code a4, a6, c3, c5, control1} RED, 21/5F; (iv) the shared predicate's factory clause deleted →
 * {@code b2, b3} RED (a bare-RULE value wrapped TWICE) + {@code control1} RED through the exact
 * double-{@code MapperS} scan (the 48 pre-wrapped bare-RULE single-then sites, e.g. drr 7.0.0
 * {@code SpreadNotationRule}), 21/3F; (v) the #339 arm's consult deleted → {@code c5} + {@code control1}
 * RED (the bare-decl scan: 15 drr 7.0.0 files of the #339 population — {@code DTCC_UnderlyingAssetReportRule},
 * {@code UnderlyingIdOther*DTCCRule}, {@code OtherPaymentRule} ×5 regimes, {@code BrokerIdRule},
 * {@code PriorUTIRule}, {@code UnderlyingAssetPriceSourceLeg1/2Rule}, …), 21/2F — {@code b6} did NOT
 * move: its rule-path CHAINED fixture is rendered by
 * {@code FunctionExpressionRenderer.renderThenExtractSetImpl}'s #276 arm, not by the #339 arm;
 * (vi) that #276 intermediate-decl wrap deleted → {@code b6} + {@code control1} RED (the bare-decl
 * scan: 12 drr 7.0.0 files of the #276 population — {@code UnderlyingIdOtherRule},
 * {@code ContractTypeRule}, {@code SeriesRule}, {@code FloatingRateIndicatorRule}, …), 21/2F. A pin
 * is a pin only once a mutation has moved it (LAW 75); the
 * #339 arm's pin is the CORPUS (v), {@code b6} pins the #276 arm (vi). <b>LAW 69, banked:</b> the
 * #276 arm keeps its OWN predicate (rule-scoped {@code bareFnThenArg} + a string-prefix
 * {@code !value.startsWith("MapperS.of(")} factory guard) beside the shared
 * {@code bareInvokableValueNeedsWrap} — the consolidation is a seat of its own, not this one.
 * <b>The RAW class of the #333 arm</b> (83 probed factory=false sites, e.g. PriceCurrency's
 * then-chain-sentinel argument) has no clean fixture — a META-annotated multi output is itself a
 * wrap factory ({@code a7}) — and is pinned by the CORPUS: {@code DTCC_UnderlyingAssetReportRule}
 * carries 32 raw + 4 factory sites in ONE byte-locked file ({@code c5}), and {@code control1} scans
 * the whole drr 7.0.0 cell for any double wrap.
 *
 * <p><b>Disclosed evidence caps.</b> The MULTI half of rungs G1/G2 ({@code MapperC.<T>of(…)} over a
 * multi-output bare function) has ZERO corpus carriers at either seat (the probe) — {@code a5}/{@code a6}
 * pin it by fixture; the LAW-69 completeness is the reason it is carried, not a corpus row.
 * {@code corpus_c5} locks the 11 carrier files of the two GENERATED cells (drr 6.36.0 + drr 7.0.0);
 * the other 31 rows are the same files in the sibling cells (6.37/6.38, 7.1–7.3), byte-locked by
 * the matrix digest at the ring, not re-generated here.
 */
class ThenChainFnValueWrapOnceSeatTest {

    private static final Path REPO_ROOT =
            Path.of(System.getProperty("user.dir")).resolve("..").normalize();

    private static final List<Path> BUILTINS_SEARCH_ROOTS = List.of(
            REPO_ROOT.resolve("test-corpus/rune-dsl-builtins/rune-runtime/src/main/resources/model"),
            REPO_ROOT.resolve("rune-dsl/rune-runtime/src/main/resources/model"),
            // THIS repo's own builtins, LAST so the roots above keep their precedence — a checkout
            // without the external corpus RUNS these tests instead of skipping them (#586: a test
            // that skips is not a lock).
            REPO_ROOT.resolve("rune-runtime/src/main/resources/model"));

    static boolean builtinsAvailable() {
        return BUILTINS_SEARCH_ROOTS.stream().anyMatch(Files::isDirectory);
    }

    /**
     * One {@code from} type, the functions the seats consume, and one reporting rule per shape the
     * seat must tell apart (the carriers' kind is POJO reporting rules + one function).
     */
    private static final String MODEL = """
            namespace census.seat20
            version "1.0.0"

            type Probe:
                id string (0..1)
                flag boolean (0..1)
                items Item (0..*)

            type Item:
                name string (0..1)
                kind string (0..1)
                code string (0..1)
                    [metadata scheme]

            func GetItems: <"E - an EXPLICIT-args MULTI-output function: tryMultiValueWrap hands back the MapperC wrap factory">
                inputs:
                    p Probe (1..1)
                    k string (0..1)
                output:
                    out Item (0..*)
                add out:
                    p -> items

            func GetMetaTags: <"b1 - a MULTI META-annotated output: tryMultiValueWrap declines, the #333 arm wraps the RAW call exactly once">
                inputs:
                    p Probe (1..1)
                output:
                    out string (0..*)
                        [metadata scheme]
                add out:
                    p -> items -> code

            func GetProbe: <"G1/G2 - a bare SINGLE-output function (implicit input)">
                inputs:
                    p Probe (1..1)
                output:
                    out Probe (0..1)
                set out:
                    p

            func GetAllItems: <"a5/a6 - a bare MULTI-output function (the multi half, no corpus carrier)">
                inputs:
                    p Probe (1..1)
                output:
                    out Item (0..*)
                add out:
                    p -> items

            func Name: <"the single-then target over a Probe">
                inputs:
                    p Probe (1..1)
                output:
                    out string (0..1)
                set out:
                    p -> id

            func CountItems: <"a6 - the single-then target over a LIST">
                inputs:
                    items Item (0..*)
                output:
                    out int (1..1)
                set out:
                    items count

            func IsFlagged:
                inputs:
                    p Probe (1..1)
                output:
                    out boolean (1..1)
                set out:
                    p -> flag = True

            func HasName:
                inputs:
                    i Item (1..1)
                output:
                    out boolean (1..1)
                set out:
                    i -> name exists

            func A2EInFunctionLambda: <"a2 - rung E on the LAMBDA channel: the explicit multi call as a then-chain base inside an extract lambda of a FUNCTION (GetUnderlyingAssetName's _thenArg0)">
                inputs:
                    p Probe (1..1)
                output:
                    out string (0..*)
                add out:
                    p -> items
                        then extract
                            if kind = "a"
                            then name
                            else (GetItems(p, kind)
                                then filter HasName
                                then extract name
                                then only-element)

            reporting rule ProbeRule from Probe: <"b2/b3 - a bare RULE renders PRE-WRAPPED (renderImplicitRuleInvocation)">
                item

            reporting rule A1ERuleElseArm from Probe: <"a1 - THE E CARRIER SHAPE: an explicit multi fn call as the k==0 base of a then-chain in an ITE else-arm (NatureOfCounterparty2)">
                filter IsFlagged
                then if flag = True
                    then "x"
                    else (GetItems(item, "k")
                        then extract name
                        then distinct only-element)

            reporting rule A3G1RuleRootFilter from Probe: <"a3 - THE G1 CARRIER SHAPE: a bare function as the FILTER receiver at rule top level (CallAmount)">
                GetProbe
                    filter IsFlagged
                    then extract Name
                    then extract item

            reporting rule A4G2SingleThen from Probe: <"a4 - THE G2 CARRIER SHAPE: a bare function as the SINGLE-then base (NotionalCurrency)">
                GetProbe then Name

            reporting rule A5G1MultiHalf from Probe: <"a5 - the G1 MULTI half: a bare multi-output function as the filter receiver (no corpus carrier; pinned by fixture)">
                GetAllItems
                    filter HasName
                    then extract name
                    then distinct

            reporting rule A6G2MultiHalf from Probe: <"a6 - the G2 MULTI half: a bare multi-output function as the single-then base (no corpus carrier; pinned by fixture)">
                GetAllItems then CountItems

            reporting rule A7MetaWrapperMultiCallWrapsOnce from Probe: <"a7 - rung E on a META-wrapper element: the concrete-wrapper witness is itself a wrap factory; the #333 arm must wrap it exactly once">
                filter IsFlagged
                then if flag = True
                    then "x"
                    else (GetMetaTags(item)
                        then extract item
                        then distinct only-element)

            reporting rule B2BareRuleFilterReceiver from Probe: <"b2 - a bare RULE as the filter receiver is pre-wrapped: no second layer">
                ProbeRule
                    filter IsFlagged
                    then extract Name
                    then extract item

            reporting rule B3BareRuleSingleThen from Probe: <"b3 - a bare RULE as the single-then base is pre-wrapped: no second layer">
                ProbeRule then Name

            reporting rule B4InputRootedFilter from Probe: <"b4 - the input-rooted filter is untouched">
                filter IsFlagged
                then extract Name
                then extract item

            reporting rule B5ExplicitCallFilterReceiver from Probe: <"b5 - an explicit-args call as the filter receiver is already a wrap factory">
                GetItems(item, "k")
                    filter HasName
                    then extract name
                    then distinct

            reporting rule B6DeepThenBareFnLevel from Probe: <"b6 - the #339 arm (a bare function at a deep-then level k>0) is unchanged — the shared predicate's positive control">
                filter IsFlagged
                then GetProbe
                then extract Name
                then extract item
            """;

    // =========================================================================
    // Part A — the seat (RED at the pre-seat blob)
    // =========================================================================

    /** Rung E, golden's decl line for the carrier shape: ONE {@code MapperC.<Item>of(} layer. */
    private static final String E_ONCE =
            "final MapperC<Item> thenArg1 = MapperC.<Item>of(getItems.evaluate(thenArg0.get(), \"k\"));";
    /** The pre-seat form (rung E): the double wrap. Compiles (MapperC implements MapperBuilder) — a parity heal. */
    private static final String E_TWICE = "MapperC.<Item>of(MapperC.<Item>of(";

    /** a1 — the E carrier shape: an explicit multi fn call at the k==0 base inside an ITE else-arm wraps ONCE. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_explicitMultiCallBaseInAnElseArmWrapsOnce() throws IOException {
        String out = rule("A1ERuleElseArmRule.java");
        assertContains(out, E_ONCE);
        assertNotContains(out, E_TWICE);
    }

    /** a2 — rung E on the LAMBDA channel: the same shape inside an extract lambda of a FUNCTION ({@code _thenArg0}, GetUnderlyingAssetName). */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_explicitMultiCallBaseInsideAFunctionLambdaWrapsOnce() throws IOException {
        String out = function("A2EInFunctionLambda.java");
        assertEquals(1, countOccurrences(out, "MapperC.<Item>of(getItems.evaluate("),
                "exactly ONE MapperC.<Item>of( layer over the explicit multi call:\n" + out);
        assertNotContains(out, E_TWICE);
    }

    /** Rung G1, golden's decl for the carrier shape (the then-chain's k==0 base = the filter over the bare function). */
    private static final String G1_ONCE =
            "final MapperS<Probe> thenArg0 = MapperS.of(getProbe.evaluate(input))\n"
            + "\t\t\t\t.filterSingleNullSafe(item -> isFlagged.evaluate(item.get()));";
    /** The pre-seat form (rung G1): a Mapper member on a bare {@code Probe} — javac cannot-find-symbol (LAW 74). */
    private static final String G1_BARE = "= getProbe.evaluate(input)\n";

    /** a3 — the G1 carrier shape: a bare function as the FILTER receiver at rule top level. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a3_bareFunctionFilterReceiverAtRuleTopLevelIsWrapped() throws IOException {
        String out = rule("A3G1RuleRootFilterRule.java");
        assertContains(out, G1_ONCE);
        assertNotContains(out, G1_BARE);
    }

    /** Rung G2, golden's decl for the carrier shape. */
    private static final String G2_ONCE = "final MapperS<Probe> thenArg = MapperS.of(getProbe.evaluate(input));";
    /** The pre-seat form (rung G2): javac incompatible-types (LAW 74). */
    private static final String G2_BARE = "final MapperS<Probe> thenArg = getProbe.evaluate(input);";

    /** a4 — the G2 carrier shape: a bare function as the SINGLE-then base. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a4_bareFunctionSingleThenBaseIsWrapped() throws IOException {
        String out = rule("A4G2SingleThenRule.java");
        assertContains(out, G2_ONCE);
        assertNotContains(out, G2_BARE);
        assertContains(out, "output = MapperS.of(name.evaluate(thenArg.get())).get();");
    }

    /** a5 — the G1 MULTI half (no corpus carrier — pinned by fixture): {@code MapperC.<Item>of(…)} + {@code filterItemNullSafe}. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a5_bareMultiFunctionFilterReceiverTakesTheMapperCLayer() throws IOException {
        String out = rule("A5G1MultiHalfRule.java");
        assertContains(out, "final MapperC<Item> thenArg0 = MapperC.<Item>of(getAllItems.evaluate(input))\n"
                + "\t\t\t\t.filterItemNullSafe(item -> hasName.evaluate(item.get()));");
        assertNotContains(out, "= getAllItems.evaluate(input)\n");
    }

    /** a6 — the G2 MULTI half (no corpus carrier — pinned by fixture): {@code final MapperC<Item> thenArg = MapperC.<Item>of(…);}. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a6_bareMultiFunctionSingleThenBaseTakesTheMapperCLayer() throws IOException {
        String out = rule("A6G2MultiHalfRule.java");
        assertContains(out, "final MapperC<Item> thenArg = MapperC.<Item>of(getAllItems.evaluate(input));");
        assertNotContains(out, "final MapperC<Item> thenArg = getAllItems.evaluate(input);");
    }

    /**
     * a7 — rung E on a META-wrapper element: a META-annotated multi output ({@code [metadata scheme]})
     * takes the concrete-wrapper witness ({@code FieldWithMetaString}, #433) and so is ITSELF a wrap
     * factory — pre-seat the #333 arm wrapped it a second time
     * ({@code MapperC.<FieldWithMetaString>of(MapperC.<FieldWithMetaString>of(getMetaTags.evaluate(…)))}).
     * (Written as the RAW class's pin; the pre-seat RED run showed it is the factory class — the RAW
     * class is pinned by the corpus, see the class javadoc.)
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a7_metaWrapperElementMultiCallWrapsOnce() throws IOException {
        String out = rule("A7MetaWrapperMultiCallWrapsOnceRule.java");
        assertEquals(1, countOccurrences(out, ">of(getMetaTags.evaluate(thenArg0.get()))"),
                "the #333 arm must wrap the META-wrapper multi call exactly once:\n" + out);
        assertNotContains(out, "of(MapperC.<");
        assertNotContains(out, "of(MapperS.of(getMetaTags");
    }

    // =========================================================================
    // Part B — placement pins (GREEN in both states)
    // =========================================================================

    /** b2 — a bare RULE filter receiver is pre-wrapped ({@code renderImplicitRuleInvocation}): the shared predicate declines, no second layer. */
    @Test
    @EnabledIf("builtinsAvailable")
    void b2_aBareRuleFilterReceiverIsNotWrappedAgain() throws IOException {
        String out = rule("B2BareRuleFilterReceiverRule.java");
        assertContains(out, "final MapperS<Probe> thenArg0 = MapperS.of(probeRuleRule.evaluate(input))\n"
                + "\t\t\t\t.filterSingleNullSafe(item -> isFlagged.evaluate(item.get()));");
        assertNotContains(out, "MapperS.of(MapperS.of(");
    }

    /** b3 — a bare RULE single-then base is pre-wrapped: no second layer. */
    @Test
    @EnabledIf("builtinsAvailable")
    void b3_aBareRuleSingleThenBaseIsNotWrappedAgain() throws IOException {
        String out = rule("B3BareRuleSingleThenRule.java");
        assertContains(out, "final MapperS<Probe> thenArg = MapperS.of(probeRuleRule.evaluate(input));");
        assertNotContains(out, "MapperS.of(MapperS.of(");
    }

    /** b4 — the {@code input}-rooted filter (the 113-rule flip family) keeps its bytes. */
    @Test
    @EnabledIf("builtinsAvailable")
    void b4_anInputRootedFilterIsUntouched() throws IOException {
        String out = rule("B4InputRootedFilterRule.java");
        assertContains(out, "final MapperS<Probe> thenArg0 = MapperS.of(input)\n"
                + "\t\t\t\t.filterSingleNullSafe(item -> isFlagged.evaluate(item.get()));");
        assertNotContains(out, "MapperS.of(MapperS.of(");
    }

    /** b5 — an explicit-args call as the filter receiver is already the MapperC wrap factory: no second layer. */
    @Test
    @EnabledIf("builtinsAvailable")
    void b5_anExplicitCallFilterReceiverIsNotWrappedAgain() throws IOException {
        String out = rule("B5ExplicitCallFilterReceiverRule.java");
        assertEquals(1, countOccurrences(out, "MapperC.<Item>of(getItems.evaluate(input, \"k\"))"),
                "exactly ONE MapperC.<Item>of( layer over the explicit call:\n" + out);
        assertContains(out, ".filterItemNullSafe(item -> hasName.evaluate(item.get()));");
        assertNotContains(out, E_TWICE);
    }

    /**
     * b6 — a bare function at a deep-then level k>0 on the RULE path renders exactly as before. The
     * mutations said which arm this pins: NOT CollectionHandler's #339 arm (mutation (v) — that consult
     * deleted — leaves b6 GREEN; the #339 arm is pinned by the corpus, {@code c5} + {@code control1}),
     * but {@code FunctionExpressionRenderer.renderThenExtractSetImpl}'s #276 intermediate-decl wrap
     * ({@code bareFnThenArg}) — the rule-path CHAINED form routes there; mutation (vi) (that wrap
     * deleted) moves this pin. Mutation (iv) (the factory clause) does not move it either — a bare
     * function's raw value is no factory; that clause is decided by {@code b2}/{@code b3} and by
     * {@code control1}'s exact double-{@code MapperS} scan.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b6_theDeepThenBareFunctionLevelIsUnchanged() throws IOException {
        String out = rule("B6DeepThenBareFnLevelRule.java");
        assertContains(out, "final MapperS<Probe> thenArg1 = MapperS.of(getProbe.evaluate(thenArg0.get()));");
        assertNotContains(out, "MapperS.of(MapperS.of(");
    }

    /**
     * b7 — the import set: the wrap adds no duplicate import, and the Mapper it needs IS imported —
     * MapperS exactly once where the single wrap fires (a3/a4), MapperC exactly once where the multi
     * wrap fires (a5/a6), never more than once anywhere.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b7_theImportSetCarriesEachMapperExactlyOnce() throws IOException {
        for (String f : List.of("A3G1RuleRootFilterRule.java", "A4G2SingleThenRule.java",
                "A5G1MultiHalfRule.java", "A6G2MultiHalfRule.java", "A1ERuleElseArmRule.java")) {
            String out = rule(f);
            int s = countOccurrences(out, "import com.rosetta.model.lib.mapper.MapperS;");
            int c = countOccurrences(out, "import com.rosetta.model.lib.mapper.MapperC;");
            assertTrue(s <= 1 && c <= 1, "a Mapper imported more than once in " + f + ":\n" + out);
            if (f.startsWith("A3") || f.startsWith("A4")) {
                assertEquals(1, s, "MapperS must be imported once in " + f + ":\n" + out);
            }
            if (f.startsWith("A5") || f.startsWith("A6")) {
                assertEquals(1, c, "MapperC must be imported once in " + f + ":\n" + out);
            }
        }
    }

    // =========================================================================
    // Part C — the corpus (drr 6.36.0 + drr 7.0.0: whole-file locks + whole-cell controls)
    // =========================================================================

    private static final Path DRR636_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.36.0");
    private static final Path DRR636_GOLDEN_DIR =
            DRR636_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path DRR7_CELL_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path DRR7_GOLDEN_DIR =
            DRR7_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    static boolean drr636Available() {
        return Files.isDirectory(DRR636_CELL_ROOT) && Files.isDirectory(DRR636_GOLDEN_DIR);
    }

    static boolean drr7Available() {
        return Drr7Corpus.gate(Files.isDirectory(DRR7_CELL_ROOT) && Files.isDirectory(DRR7_GOLDEN_DIR), ThenChainFnValueWrapOnceSeatTest.class);
    }

    /** The E carriers in drr 6.36.0 (the same two files recur in 6.37.0/6.38.0). */
    private static final List<String> DRR636_CARRIERS = List.of(
            "drr/regulation/esma/emir/refit/trade/reports/NatureOfCounterparty2Rule.java",
            "drr/regulation/fca/ukemir/refit/trade/reports/NatureOfTheCounterparty2Rule.java");

    /** The E + G1 + G2 carriers in drr 7.0.0 (the same nine files recur in 7.1.0/7.2.0/7.3.0). */
    private static final List<String> DRR7_CARRIERS = List.of(
            // rung E
            "drr/regulation/common/trade/underlier/functions/GetUnderlyingAssetName.java",
            "drr/regulation/common/dtcc/trade/reports/DTCC_UnderlyingAssetReportRule.java",
            "drr/regulation/mas/rewrite/trade/reports/UnderlyingIdOtherDTCCRule.java",
            "drr/regulation/mas/rewrite/trade/reports/UnderlyingIdOtherSourceDTCCRule.java",
            // rung G1
            "drr/standards/iosco/cde/version1/quantity/reports/CallAmountRule.java",
            "drr/standards/iosco/cde/version1/quantity/reports/CallCurrencyRule.java",
            "drr/standards/iosco/cde/version1/quantity/reports/PutAmountRule.java",
            "drr/standards/iosco/cde/version1/quantity/reports/PutCurrencyRule.java",
            // rung G2
            "drr/standards/iosco/cde/version1/quantity/reports/NotionalCurrencyRule.java");

    /** c1 — rung E, a WHOLE-FILE heal in drr 6.36.0 (both of its diff-lines were this seat's). */
    @Test
    @EnabledIf("drr636Available")
    void corpus_c1_natureOfCounterparty2Rule636() throws IOException {
        lock(drr636Output, drr636GenErrors, DRR636_GOLDEN_DIR, DRR636_CARRIERS.get(0), "drr 6.36.0");
    }

    /** c2 — rung G1, a WHOLE-FILE heal in drr 7.0.0: 16 non-compiling files repaired across the four 7.x cells. */
    @Test
    @EnabledIf("drr7Available")
    void corpus_c2_callAmountRule700() throws IOException {
        lock(drr7Output, drr7GenErrors, DRR7_GOLDEN_DIR, DRR7_CARRIERS.get(4), "drr 7.0.0");
    }

    /** c3 — rung G2, a WHOLE-FILE heal in drr 7.0.0: 4 non-compiling files repaired across the four 7.x cells. */
    @Test
    @EnabledIf("drr7Available")
    void corpus_c3_notionalCurrencyRule700() throws IOException {
        lock(drr7Output, drr7GenErrors, DRR7_GOLDEN_DIR, DRR7_CARRIERS.get(8), "drr 7.0.0");
    }

    /** c4 — rung E on the LAMBDA channel of a FUNCTION ({@code _thenArg0}), a WHOLE-FILE heal in drr 7.0.0. */
    @Test
    @EnabledIf("drr7Available")
    void corpus_c4_getUnderlyingAssetName700() throws IOException {
        lock(drr7Output, drr7GenErrors, DRR7_GOLDEN_DIR, DRR7_CARRIERS.get(0), "drr 7.0.0");
    }

    /** c5 — EVERY carrier file of the two generated cells byte-locked (11 of the 42 rows; the other 31 are these files in the sibling cells, held by the matrix digest). */
    @Test
    @EnabledIf("drr7Available")
    void corpus_c5_everyCarrierOfTheGeneratedCellsIsAWholeFileHeal() throws IOException {
        List<String> failures = new ArrayList<>();
        for (String p : DRR7_CARRIERS) {
            try {
                lock(drr7Output, drr7GenErrors, DRR7_GOLDEN_DIR, p, "drr 7.0.0");
            } catch (AssertionError e) {
                failures.add("drr 7.0.0 " + p);
            }
        }
        if (drr636Available()) {
            for (String p : DRR636_CARRIERS) {
                try {
                    lock(drr636Output, drr636GenErrors, DRR636_GOLDEN_DIR, p, "drr 6.36.0");
                } catch (AssertionError e) {
                    failures.add("drr 6.36.0 " + p);
                }
            }
        }
        assertEquals(List.of(), failures, "carrier files that are NOT byte-identical to golden");
    }

    /**
     * control 0 — <b>GOLDEN is the oracle.</b> Over the WHOLE frozen drr 7.0.0 golden tree (and
     * drr 6.36.0 when present): ZERO single-argument {@code MapperC.<T>of(MapperC.<T>of(…))} and
     * ZERO bare-invokable values at a {@code final Mapper*<…> x = <ident>.evaluate(} decl — and the
     * scan's own positive controls: the list-literal VARARGS form
     * {@code MapperC.<T>of(MapperC.<T>of(a), …)} is PRESENT (ForwardExchangeRateRule — a different
     * shape the seat must not touch), the {@code MapperC.<X>of(} population is in the hundreds, and
     * the nine carrier files carry the once-wrapped forms.
     */
    @Test
    @EnabledIf("drr7Available")
    void corpus_control0_goldenIsTheOracle() throws IOException {
        Map<String, String> golden = readGoldenTree(DRR7_GOLDEN_DIR);
        WrapScan scan = scan(golden);
        assertEquals(List.of(), scan.doubleWrapFiles, "golden carries NO single-argument double MapperC wrap");
        assertEquals(List.of(), scan.doubleMapperSFiles, "golden carries NO exact single-argument double MapperS wrap");
        assertEquals(List.of(), scan.bareDeclFiles, "golden carries NO bare-invokable value at a Mapper decl");
        assertTrue(hasExactDoubleMapperS(
                "final MapperS<X> thenArg = MapperS.of(MapperS.of(spreadNotationEnumRule.evaluate(input)));"),
                "the double-MapperS scan must see the mutation-(iv) shape (its positive control)");
        assertFalse(hasExactDoubleMapperS(
                "MapperS.of(MapperS.of(economicTermsForProduct.evaluate(product)).<Payout>mapC(\"getPayout\", e -> e.getPayout()).get())"),
                "the double-MapperS scan must NOT see golden's chain-head shape (its negative control)");
        assertTrue(scan.varargFiles >= 1,
                "the scan must see the legitimate varargs form (ForwardExchangeRateRule); saw " + scan.varargFiles);
        assertTrue(scan.mapperCOfSites >= 500,
                "the scan must see the MapperC.<X>of( population (golden drr 7.0.0 has 1,392 files); saw "
                + scan.mapperCOfSites);
        for (String p : DRR7_CARRIERS) {
            assertTrue(golden.containsKey(p), "golden missing the carrier " + p);
        }
        if (drr636Available()) {
            WrapScan s636 = scan(readGoldenTree(DRR636_GOLDEN_DIR));
            assertEquals(List.of(), s636.doubleWrapFiles, "golden 6.36.0 carries NO double wrap");
            assertEquals(List.of(), s636.doubleMapperSFiles, "golden 6.36.0 carries NO exact double MapperS wrap");
            assertEquals(List.of(), s636.bareDeclFiles, "golden 6.36.0 carries NO bare decl");
        }
    }

    /**
     * control 1 — <b>LAW 72: the control's domain is the MECHANISM's reach.</b> Over the WHOLE
     * generated drr 7.0.0 cell — every kind — and the whole generated drr 6.36.0 cell: ZERO
     * single-argument double MapperC wraps, ZERO exact double MapperS wraps (mutation (iv)'s shape at
     * the 48 pre-wrapped bare-RULE seats — the scan that makes this control ABLE to fail under (iv))
     * and ZERO bare-invokable values at a Mapper decl (golden's law, measured in {@code control0});
     * and the carriers must be REACHED in the once-wrapped form, else "none double / none bare" is
     * vacuous.
     */
    @Test
    @EnabledIf("drr7Available")
    void corpus_control1_noDoubleWrapAndNoBareDeclAcrossTheWholeCells() {
        assertNotNull(drr7Output, "drr 7.0.0 generation did not run — corpus unavailable?");
        WrapScan fork = scan(drr7Output);
        assertEquals(List.of(), fork.doubleWrapFiles,
                "a then-chain decl was emitted with TWO MapperC layers over a function call");
        assertEquals(List.of(), fork.doubleMapperSFiles,
                "a pre-wrapped bare-RULE value was wrapped AGAIN (MapperS.of(MapperS.of(<rule>.evaluate(…))))");
        assertEquals(List.of(), fork.bareDeclFiles,
                "a Mapper decl was emitted with a BARE function value — this does not compile");
        Set<String> reached = new LinkedHashSet<>();
        for (String p : DRR7_CARRIERS) {
            String gen = drr7Output.get(p);
            if (gen != null && (gen.contains("MapperC.<AssetIdentifier>of(underlierProductIdentifier.evaluate(")
                    || gen.contains("MapperC.<ReportingRegime>of(extractRegimeInformation.evaluate(")
                    || gen.contains("= MapperS.of(getTradeForQuantity.evaluate(input))")
                    || gen.contains("= MapperS.of(notional.evaluate(input));"))) {
                reached.add(p);
            }
        }
        assertEquals(new LinkedHashSet<>(DRR7_CARRIERS), reached,
                "every drr 7.0.0 carrier must be REACHED in the once-wrapped form (LAW 72)");
        if (drr636Available()) {
            assertNotNull(drr636Output, "drr 6.36.0 generation did not run — corpus unavailable?");
            WrapScan f636 = scan(drr636Output);
            assertEquals(List.of(), f636.doubleWrapFiles, "drr 6.36.0: a double MapperC wrap was emitted");
            assertEquals(List.of(), f636.doubleMapperSFiles, "drr 6.36.0: a double MapperS wrap was emitted");
            assertEquals(List.of(), f636.bareDeclFiles, "drr 6.36.0: a bare Mapper decl was emitted");
        }
    }

    /** control 2 — the neighbouring seat heals in drr 7.0.0 stay healed (seat 16's csa JurisdictionOfCounterparty1Rule, #588). */
    @Test
    @EnabledIf("drr7Available")
    void corpus_control2_theNeighbouringSeatHealsStayHealed() throws IOException {
        lock(drr7Output, drr7GenErrors, DRR7_GOLDEN_DIR,
                "drr/regulation/csa/rewrite/trade/reports/JurisdictionOfCounterparty1Rule.java", "drr 7.0.0");
    }

    // =========================================================================
    // The scan (a character walk over a code-only view — no regex over structured content)
    // =========================================================================

    private static final class WrapScan {
        final List<String> doubleWrapFiles = new ArrayList<>();
        /**
         * The EXACT single-argument double-MapperS shape {@code MapperS.of(MapperS.of(<ident>.evaluate(…)))}
         * — what mutation (iv) emits at a pre-wrapped bare-RULE seat. Golden's 142 (7.0.0) / 150 (6.36.0)
         * files carrying the loose token {@code MapperS.of(MapperS.of(} are a DIFFERENT shape
         * ({@code MapperS.of(MapperS.of(x.evaluate(…)).<T>mapC(…).get())}: the inner call is a chain
         * head, not the whole argument) and stay — the scan's negative domain.
         */
        final List<String> doubleMapperSFiles = new ArrayList<>();
        final List<String> bareDeclFiles = new ArrayList<>();
        int varargFiles;
        int mapperCOfSites;
    }

    private static WrapScan scan(Map<String, String> tree) {
        WrapScan r = new WrapScan();
        for (Map.Entry<String, String> e : tree.entrySet()) {
            String code = codeOnly(e.getValue());
            boolean vararg = false;
            boolean dbl = false;
            int i = code.indexOf("MapperC.<");
            while (i >= 0) {
                int gt = code.indexOf(">of(", i);
                if (gt < 0) {
                    break;
                }
                String t = code.substring(i + "MapperC.<".length(), gt);
                int open = gt + ">of".length();
                if (validTypeName(t) && code.startsWith(">of(", gt)) {
                    r.mapperCOfSites++;
                    int close = matchParen(code, open);
                    if (close > open) {
                        String inner = code.substring(open + 1, close).trim();
                        String innerHead = "MapperC.<" + t + ">of(";
                        if (inner.startsWith(innerHead)) {
                            List<String> args = topLevelArgs(inner);
                            if (args.size() > 1) {
                                vararg = true;
                            } else if (args.size() == 1) {
                                int innerClose = matchParen(inner, innerHead.length() - 1);
                                if (innerClose == inner.length() - 1) {
                                    String innerArg = inner.substring(innerHead.length(), innerClose).trim();
                                    List<String> innerArgs = topLevelArgs(innerArg.isEmpty() ? "" : innerArg);
                                    if (innerArgs.size() == 1 && isIdentEvaluateCall(innerArgs.get(0))) {
                                        dbl = true;
                                    }
                                }
                            }
                        }
                    }
                }
                i = code.indexOf("MapperC.<", i + 1);
            }
            if (vararg) {
                r.varargFiles++;
            }
            if (dbl) {
                r.doubleWrapFiles.add(e.getKey());
            }
            if (hasExactDoubleMapperS(code)) {
                r.doubleMapperSFiles.add(e.getKey());
            }
            if (hasBareInvokableMapperDecl(code)) {
                r.bareDeclFiles.add(e.getKey());
            }
        }
        return r;
    }

    /** {@code MapperS.of(MapperS.of(<ident>.evaluate(…)))} with the inner call the WHOLE outer argument. */
    private static boolean hasExactDoubleMapperS(String code) {
        final String head = "MapperS.of(";
        int i = code.indexOf(head);
        while (i >= 0) {
            int open = i + head.length() - 1;
            int close = matchParen(code, open);
            if (close > open) {
                String inner = code.substring(open + 1, close).trim();
                if (inner.startsWith(head) && topLevelArgs(inner).size() == 1) {
                    int innerClose = matchParen(inner, head.length() - 1);
                    if (innerClose == inner.length() - 1) {
                        String innerArg = inner.substring(head.length(), innerClose).trim();
                        List<String> innerArgs = topLevelArgs(innerArg.isEmpty() ? "" : innerArg);
                        if (innerArgs.size() == 1 && isIdentEvaluateCall(innerArgs.get(0))) {
                            return true;
                        }
                    }
                }
            }
            i = code.indexOf(head, i + 1);
        }
        return false;
    }

    /** {@code final Mapper[SC]<…> <name> = <ident>.evaluate(} — a bare invokable value at a Mapper decl. */
    private static boolean hasBareInvokableMapperDecl(String code) {
        int i = code.indexOf("final Mapper");
        while (i >= 0) {
            int eq = code.indexOf(" = ", i);
            int nl = code.indexOf('\n', i);
            if (eq > 0 && (nl < 0 || eq < nl)) {
                String head = code.substring(i, eq);
                if ((head.startsWith("final MapperS<") || head.startsWith("final MapperC<"))
                        && head.indexOf('>') > 0) {
                    String rhs = code.substring(eq + 3, nl < 0 ? code.length() : nl);
                    if (isIdentEvaluateCallHead(rhs)) {
                        return true;
                    }
                }
            }
            i = code.indexOf("final Mapper", i + 1);
        }
        return false;
    }

    private static boolean isIdentEvaluateCall(String s) {
        if (!isIdentEvaluateCallHead(s)) {
            return false;
        }
        int open = s.indexOf('(');
        return matchParen(s, open) == s.length() - 1;
    }

    /** {@code <ident>.evaluate(…} at the head of {@code s}. */
    private static boolean isIdentEvaluateCallHead(String s) {
        int dot = s.indexOf(".evaluate(");
        if (dot <= 0) {
            return false;
        }
        String ident = s.substring(0, dot);
        if (!Character.isLowerCase(ident.charAt(0)) && ident.charAt(0) != '_') {
            return false;
        }
        for (int i = 0; i < ident.length(); i++) {
            if (!Character.isJavaIdentifierPart(ident.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean validTypeName(String t) {
        if (t.isEmpty() || !Character.isJavaIdentifierStart(t.charAt(0))) {
            return false;
        }
        for (int i = 0; i < t.length(); i++) {
            if (!Character.isJavaIdentifierPart(t.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /** Split {@code s} on depth-0 commas (a character walk). */
    private static List<String> topLevelArgs(String s) {
        List<String> out = new ArrayList<>();
        if (s.isEmpty()) {
            return out;
        }
        int depth = 0;
        int start = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(' || c == '<' || c == '[') {
                depth++;
            } else if (c == ')' || c == '>' || c == ']') {
                depth--;
            } else if (c == ',' && depth == 0) {
                out.add(s.substring(start, i).trim());
                start = i + 1;
            }
        }
        out.add(s.substring(start).trim());
        return out;
    }

    private static Map<String, String> readGoldenTree(Path dir) throws IOException {
        Map<String, String> tree = new LinkedHashMap<>();
        try (var stream = Files.walk(dir)) {
            for (Path p : stream.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java")).toList()) {
                tree.put(dir.relativize(p).toString().replace('\\', '/'), Files.readString(p));
            }
        }
        assertTrue(tree.size() > 1000, "the frozen golden tree looks truncated: " + dir + " " + tree.size());
        return tree;
    }

    /**
     * A CODE-ONLY view of generated Java: string literals, char literals and both comment forms
     * blanked to spaces, newlines preserved. A character walk, not a pattern match, and it performs
     * no structural analysis of the language content — only a literal-token scan of the fork's own
     * just-emitted output.
     */
    private static String codeOnly(String s) {
        char[] out = s.toCharArray();
        for (int i = 0; i < out.length; i++) {
            char c = out[i];
            if (c == '"' || c == '\'') {
                char quote = c;
                int j = i + 1;
                while (j < out.length && out[j] != quote) {
                    j += out[j] == '\\' ? 2 : 1;
                }
                for (int k = i; k <= Math.min(j, out.length - 1); k++) {
                    if (out[k] != '\n') {
                        out[k] = ' ';
                    }
                }
                i = j;
            } else if (c == '/' && i + 1 < out.length && out[i + 1] == '/') {
                int j = i;
                while (j < out.length && out[j] != '\n') {
                    out[j++] = ' ';
                }
                i = j;
            } else if (c == '/' && i + 1 < out.length && out[i + 1] == '*') {
                int j = i;
                while (j + 1 < out.length && !(out[j] == '*' && out[j + 1] == '/')) {
                    if (out[j] != '\n') {
                        out[j] = ' ';
                    }
                    j++;
                }
                if (j < out.length) {
                    out[j] = ' ';
                }
                if (j + 1 < out.length) {
                    out[j + 1] = ' ';
                }
                i = j + 1;
            }
        }
        return new String(out);
    }

    /** Index of the {@code ')'} matching the {@code '('} at {@code open}, or -1. */
    private static int matchParen(String s, int open) {
        int depth = 0;
        for (int i = open; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')' && --depth == 0) {
                return i;
            }
        }
        return -1;
    }

    private static int countOccurrences(String haystack, String needle) {
        int n = 0;
        int i = haystack.indexOf(needle);
        while (i >= 0) {
            n++;
            i = haystack.indexOf(needle, i + needle.length());
        }
        return n;
    }

    // =========================================================================
    // Corpus harness
    // =========================================================================

    private static Map<String, String> drr7Output;
    private static List<String> drr7GenErrors = new ArrayList<>();
    private static Map<String, String> drr636Output;
    private static List<String> drr636GenErrors = new ArrayList<>();

    @BeforeAll
    static void generateCells() throws IOException {
        if (drr7Available()) {
            List<String> errs = new ArrayList<>();
            drr7Output = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", DRR7_CELL_ROOT), errs);
            drr7GenErrors = errs;
        }
        if (drr636Available()) {
            List<String> errs = new ArrayList<>();
            drr636Output = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "6.36.0", DRR636_CELL_ROOT), errs);
            drr636GenErrors = errs;
        }
    }

    private static Map<String, String> generateCell(D11CorpusRegressionTest.CellSpec cell,
            List<String> errors) throws IOException {
        var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(),
                D11CorpusRegressionTest.emissionFilter(cell));
        var typeUtil = new JavaTypeUtil();
        var typeTranslator = new JavaTypeTranslator(typeUtil);
        var pojoGen = new ModelObjectGenerator(gm, typeTranslator, typeUtil);
        var choiceGen = new ChoiceObjectGenerator(gm, typeTranslator, typeUtil, pojoGen);
        var funcGen = new FunctionGenerator(gm, typeTranslator, typeUtil);
        var ruleGen = new RuleGenerator(gm, typeTranslator, funcGen);
        var reportGen = new ReportGenerator(gm, typeTranslator, funcGen);
        var dataRuleGen = new DataRuleGenerator(gm, typeTranslator, typeUtil);
        var labelProviderGen = new LabelProviderGenerator(
                gm, typeTranslator, new DeepFeatureCallUtil(gm::getType),
                new LabelProviderGeneratorUtil());
        Map<String, String> output = new LinkedHashMap<>();
        // #588/#589 (Copilot): every generator leg's per-model errors are aggregated, not
        // discarded — dropping them lets a byte-lock pass while unrelated work in the same cell
        // failed to generate, which is green for the wrong reason.
        for (RModel model : corpus.workspace().files()) {
            if (gm.shouldGenerate(model)) {
                String version = gm.version(model);
                collect(errors, pojoGen.generateClasses(model, version, output));
                collect(errors, choiceGen.generateClasses(model, version, output));
                collect(errors, ruleGen.generateClasses(model, version, output));
                collect(errors, reportGen.generateClasses(model, version, output));
                collect(errors, dataRuleGen.generateClasses(model, version, output));
                collect(errors, labelProviderGen.generateClasses(model, version, output));
            }
        }
        collect(errors, funcGen.generateWithErrors(output));
        return output;
    }

    /** Aggregate one generator leg's reported errors; surfaced per locked file by {@code lock}. */
    private static void collect(List<String> sink, List<GenerationException> errors) {
        if (errors != null) {
            errors.forEach(e -> sink.add(e.getTargetPath() + " — " + e));
        }
    }

    private static void lock(Map<String, String> output, List<String> genErrors, Path goldenDir,
            String path, String cell) throws IOException {
        assertNotNull(output, cell + " generation did not run — corpus unavailable?");
        List<String> lockedErrors = genErrors.stream().filter(e -> e.contains(path)).toList();
        assertTrue(lockedErrors.isEmpty(),
                "the generator reported errors for the locked file " + path + ": " + lockedErrors);
        String generated = output.get(path);
        assertNotNull(generated, "not generated in " + cell + ": " + path);
        Path goldenPath = goldenDir.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "golden missing: " + goldenPath);
        assertEquals(normalize(Files.readString(goldenPath)), normalize(generated),
                "generated " + cell + " output must byte-match golden (newline-normalized) for "
                + path + " — seat 20: a function-call value at a then-chain seat carries exactly"
                + " ONE Mapper layer.");
    }

    // =========================================================================
    // Fixture harness
    // =========================================================================

    private static RLinkingResult linking;
    private static RModel mainModel;
    private static Map<String, String> fixtureOut;

    private static void link() throws IOException {
        if (linking == null) {
            RModel main = AstBuilder.buildFromString(MODEL, "seat20.rosetta");
            main.setVersion("0.0.0.test");
            List<RModel> models = new ArrayList<>();
            models.add(main);
            models.addAll(loadBuiltinsOnly());
            linking = RWorkspace.build(models);
            mainModel = main;
        }
    }

    private static Map<String, String> render(Predicate<RModel> filter) throws IOException {
        link();
        GeneratorModel gm = new GeneratorModel(linking.workspace(), filter);
        JavaTypeUtil typeUtil = new JavaTypeUtil();
        JavaTypeTranslator tt = new JavaTypeTranslator(typeUtil);
        FunctionGenerator fg = new FunctionGenerator(gm, tt, typeUtil);
        RuleGenerator ruleGen = new RuleGenerator(gm, tt, fg);
        Map<String, String> out = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        ruleGen.generateClasses(mainModel, "1.0", out)
                .forEach(e -> errors.add(e.getTargetPath() + " — " + e));
        fg.generateWithErrors(out)
                .forEach(e -> errors.add(e.getTargetPath() + " — " + e));
        if (!errors.isEmpty()) {
            throw new AssertionError("fixture generation errors (a broken fixture"
                    + " must fail loudly, not skip): " + errors);
        }
        return out;
    }

    private static Map<String, String> fixture() throws IOException {
        if (fixtureOut == null) {
            fixtureOut = render(m -> "census.seat20".equals(m.namespace()));
        }
        return fixtureOut;
    }

    private static String rule(String fileName) throws IOException {
        return lookup(fixture(), "reports/" + fileName);
    }

    private static String function(String fileName) throws IOException {
        return lookup(fixture(), "functions/" + fileName);
    }

    private static String lookup(Map<String, String> output, String suffix) {
        return output.entrySet().stream()
                .filter(e -> e.getKey().endsWith("/" + suffix))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "'" + suffix + "' was not generated; keys=" + output.keySet()));
    }

    private static List<RModel> loadBuiltinsOnly() throws IOException {
        Map<String, Path> resolved = new LinkedHashMap<>();
        for (Path root : BUILTINS_SEARCH_ROOTS) {
            if (!Files.isDirectory(root)) {
                continue;
            }
            try (var stream = Files.walk(root)) {
                stream.filter(p -> p.toString().endsWith(".rosetta"))
                      .forEach(p -> resolved.putIfAbsent(p.getFileName().toString(), p));
            }
        }
        List<String> failures = new ArrayList<>();
        List<RModel> models = new ArrayList<>();
        resolved.values().stream()
                .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                .forEach(p -> {
                    try {
                        models.add(AstBuilder.buildFromFile(p));
                    } catch (Exception e) {
                        failures.add(p + " — " + e);
                    }
                });
        if (!failures.isEmpty()) {
            throw new AssertionError("[ThenChainFnValueWrapOnceSeatTest] builtins parse"
                    + " failures: " + String.join("; ", failures));
        }
        return models;
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }

    private static void assertContains(String out, String needle) {
        assertTrue(out.contains(needle),
                "expected needle missing:\n" + needle + "\n--- in output:\n" + out);
    }

    private static void assertNotContains(String out, String token) {
        assertTrue(!out.contains(token),
                "forbidden token present: " + token + "\n--- in output:\n" + out);
    }
}
