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
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import javax.lang.model.SourceVersion;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.GenerationException;
import com.regnosys.rosetta.generator.java.enums.EnumGenerator;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.function.LabelProviderGenerator;
import com.regnosys.rosetta.generator.java.function.LabelProviderGeneratorUtil;
import com.regnosys.rosetta.generator.java.object.ChoiceObjectGenerator;
import com.regnosys.rosetta.generator.java.object.ModelObjectGenerator;
import com.regnosys.rosetta.generator.java.object.datarule.DataRuleGenerator;
import com.regnosys.rosetta.generator.java.reports.ReportGenerator;
import com.regnosys.rosetta.generator.java.reports.RuleGenerator;
import com.regnosys.rosetta.generator.java.spi.IRGeneration;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.symbols.RLinkingResult;
import com.regnosys.rosetta.symbols.RWorkspace;
import com.regnosys.rosetta.utils.DeepFeatureCallUtil;

/**
 * SEAT 24, law F4 — facet {@code bareFnCondHoistEveryContext}: <b>a bare fn/rule-call condition hoists
 * {@code final Boolean <name> = <call>;} and guards {@code (<name> == null ? false : <name>)} in EVERY context</b> —
 * the #179/#217 law at its two last un-wired seats. Upstream's {@code convertNullSafe} hoists a raw-Boolean
 * condition (a function's {@code evaluate} returns the raw {@code Boolean}; {@code .getOrDefault(false)} on it is a
 * Mapper method on a raw value — non-compiling, LAW 74): golden drr 5.61.0 carries 1,152 {@code _boolean}/{@code
 * booleanN} hoists over 754 files and ZERO inline {@code if (<fn>.evaluate(…).getOrDefault(false))} conditions — in
 * EVERY golden cell (the seat-24 golden census, T4inline = 0).
 *
 * <p><b>The two seats — both REFUTED census producers (LAW 72).</b> The close census charted (a) {@code
 * ControlFlowHandler.buildIfThenElseHoistBlock}'s sink-path wrap witness — the seat-24 LAW-75 probe over all 275
 * rows on BOTH routes showed every bare-fn TOP condition there FIRING (126/126) — and (b) the deep-then ladder's
 * {@code lambdaRoute()} rung gate, which {@code OptionPremiumAmount} never reaches. The probe found: (a) the 20
 * M-T1-1 rows are the deep-then ladder's TOP rung ({@code ControlFlowHandler.hoistAsDeepThenMapperLocalOrNull},
 * {@code i == 0}, the lambda route's {@code _thenArgN} base mode, witness ABSENT — the in-lambda rule-path compile
 * is the bare {@code Boolean}) — 22 ladders, 11 rule names, {@code bareRungs=1} in every one; golden asic
 * {@code NotionalAmountLeg1Rule}: {@code final Boolean _boolean = isActionTypeTERM.evaluate(thenArg0.get());} ABOVE
 * {@code final MapperS<TradableProduct> _thenArg0;} and {@code if ((_boolean == null ? false : _boolean)) {}; (b) the 4
 * M-T2-9 rows are {@code FunctionExpressionRenderer.appendIteHoistChainCore}'s {@code nestedElseBoolTail} (#399 R1),
 * gated "SESSION-active FUNCTION path only" ({@code hoistSession != null && findEnclosingRule(cond) == null}) while
 * the rule path HAS the session (probe F4C: rule rows {@code session=true sink=true witness=true}); golden esma/fca
 * {@code OptionPremiumAmount/CurrencyRule}: {@code } else { final Boolean _boolean = isCapFloor.evaluate(…); if
 * ((_boolean == null ? false : _boolean)) { … } else { … } }} where the fork flattened {@code } else if (MapperS.of(
 * isCapFloor.evaluate(…)).getOrDefault(false)) {}.
 *
 * <p><b>The seat.</b> (a) the TOP rung on the lambda route hoists {@code final Boolean _boolean = <inner>;} BEFORE
 * the {@code final MapperS<X> <sentinel>;} declaration and guards the top header — the #217 top-condition placement;
 * a witnessed wrap takes its inner exactly as the rungs do, an un-witnessed compile IS the value (render truth: the
 * bare Boolean); the singleton law is kept verbatim (a second bare-fn rung → no restructure anywhere); the i ≥ 1 rungs
 * keep the #393 deepening; the STATEMENT route (sink) has NO carrier — every real compile in the probe is
 * {@code lambdaRoute=true} — and keeps its bytes (stated). (b) the nested-else tail admits the RULE path: the gate
 * is "a session is reachable" — {@code hoistSession} or the scope's sink session, the SAME {@code boolSession} the
 * top-condition hoist reads — without the {@code findEnclosingRule(cond) == null} exclusion; the recursion's
 * top-condition hoist then fires on the rule path's witnessed compile.
 *
 * <p><b>RED at the pre-seat blob</b> (this suite kept — MEASURED by the receipts chain at the final head, LAW 78/82):
 * a1, a2, a3, {@code corpus_c1}, {@code corpus_c2}, {@code corpus_c3}, {@code corpus_control1} — 7F (under
 * {@code -Pir-on} also a4 — 8F); b1, b2, b3, {@code control0}, {@code control3} GREEN in both states
 * ({@code control2} compares routes — GREEN in both). <b>LAW 66/76 mutations</b> (each applied → run → reverted at
 * the FINAL head; the failing SETS are the chain's MEASUREMENT, not the charter's prediction): (i) the top rung
 * excluded from the bare-rung census (no top hoist) → a1, a2, c1, control1 + law F5's
 * {@code BigIntegerLiteralRulePathSeatTest.corpus_c4} (the asic {@code NotionalAmountLeg1Rule} lock carries a
 * bare-fn top rung as well as its BigInteger hoist) — 5F; (ii) the singleton law dropped at the top rung (a second
 * bare-fn rung no longer suppresses the restructure) → b2 — 1F; (iii) the rule-path admission reverted → a3, c2, c3,
 * control1 (the esma/fca {@code OptionPremiumAmountRule}/{@code OptionPremiumCurrencyRule} tails lose the hoist —
 * fork {@code [0, 0, 1]} against golden {@code [1, 1, 0]}) — 4F; (iv) the un-witnessed top-rung compile refused (the
 * witness re-imposed) → a1, c1, control1 + law F5's {@code corpus_c4} — 4F; a2 does NOT move: its top rung is a
 * RULE call whose compile is witnessed ({@code MapperS.of(…)}-wrapped) and unwraps like any rung — only the
 * un-witnessed fn-call top rung reads the fallback.
 */
class BareFnCondHoistEveryContextSeatTest {

    private static final Path REPO_ROOT =
            Path.of(System.getProperty("user.dir")).resolve("..").normalize();

    private static final List<Path> BUILTINS_SEARCH_ROOTS = List.of(
            REPO_ROOT.resolve("test-corpus/rune-dsl-builtins/rune-runtime/src/main/resources/model"),
            REPO_ROOT.resolve("rune-dsl/rune-runtime/src/main/resources/model"),
            REPO_ROOT.resolve("rune-runtime/src/main/resources/model"));

    static boolean builtinsAvailable() {
        return BUILTINS_SEARCH_ROOTS.stream().anyMatch(Files::isDirectory);
    }

    static boolean irProviderOnClasspath() {
        try {
            Class.forName("com.regnosys.rosetta.generator.java.ir.IRGenerationProviderImpl");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    static boolean builtinsAndIrProviderAvailable() {
        return builtinsAvailable() && irProviderOnClasspath();
    }

    static boolean drr561AndIrProviderAvailable() {
        return drr561Available() && irProviderOnClasspath();
    }

    /**
     * One {@code from} type with a function filter, boolean functions (the {@code IsActionTypeTERM} / {@code IsCapFloor}
     * twins), a boolean RULE, navigation functions (the {@code BeforeTradeForEvent} / {@code TradableProductForEvent}
     * twins), value rules (the {@code CDEOptionPremiumAmount} / {@code NotionalCurrency1} twins), and one reporting
     * rule per shape.
     */
    private static final String MODEL = """
            namespace census.seat24f4
            version "1.0.0"

            type Prod:
                val number (0..1)

            type Trade:
                prod Prod (0..1)

            type Instr:
                trade Trade (0..1)
                before Trade (0..1)
                isTerm boolean (0..1)
                isCap boolean (0..1)
                prem number (0..1)
                alt number (0..1)
                allowed boolean (0..1)

            func IsAllowable: <"IsAllowableAction twin - the function filter">
                inputs:
                    i Instr (0..1)
                output:
                    result boolean (1..1)
                set result:
                    i -> allowed = True

            func IsTerm: <"IsActionTypeTERM twin - the bare fn-call TOP condition of the lambda ladder">
                inputs:
                    i Instr (0..1)
                output:
                    result boolean (1..1)
                set result:
                    i -> isTerm = True

            func IsCap: <"IsCapFloor twin - the bare fn-call nested-else rung of the rule then-chain ladder">
                inputs:
                    i Instr (0..1)
                output:
                    result boolean (1..1)
                set result:
                    i -> isCap = True

            func BeforeFor: <"BeforeTradeForEvent twin">
                inputs:
                    i Instr (0..1)
                output:
                    t Trade (0..1)
                set t:
                    i -> before

            func TradeFor: <"TradableProductForEvent twin">
                inputs:
                    i Instr (0..1)
                output:
                    t Trade (0..1)
                set t:
                    i -> trade

            func ValOf: <"PayoutLeg1 twin - the inner then step">
                inputs:
                    p Prod (0..1)
                output:
                    v number (0..1)
                set v:
                    p -> val

            reporting rule IsTermRule from Instr: <"a boolean RULE as the top condition (the predicate admits RRule)">
                extract isTerm

            reporting rule PremRule from Instr: <"CDEOptionPremiumAmount twin">
                extract prem

            reporting rule AltRule from Instr: <"NotionalCurrency1 twin">
                extract alt

            reporting rule A1TopRung from Instr: <"a1 - THE asic NotionalAmountLeg1 SHAPE: a bracket extract whose base is a bare-fn conditional then-chain (the lambda-route deep-then ladder's TOP rung)">
                filter IsAllowable
                then extract instr [
                    if IsTerm
                    then BeforeFor -> prod
                    else TradeFor -> prod
                        then extract ValOf
                ]
                as "a1"

            reporting rule A2RuleTopRung from Instr: <"a2 - the same ladder with a boolean RULE as the top condition">
                filter IsAllowable
                then extract instr [
                    if IsTermRule
                    then BeforeFor -> prod
                    else TradeFor -> prod
                        then extract ValOf
                ]
                as "a2"

            reporting rule A3RuleTail from Instr: <"a3 - THE esma OptionPremiumCurrency SHAPE: the rule then-chain ladder's nested-else bare-fn rung">
                filter IsAllowable
                then if PremRule exists
                    then PremRule
                    else if IsCap(item)
                    then AltRule
                as "a3"

            reporting rule B1ExistsTop from Instr: <"b1 - a NON-bare top condition keeps the inline form">
                filter IsAllowable
                then extract instr [
                    if before exists
                    then BeforeFor -> prod
                    else TradeFor -> prod
                        then extract ValOf
                ]
                as "b1"

            reporting rule B2TwoBareRungs from Instr: <"b2 - TWO bare-fn rungs in one lambda ladder keep the flat form (the singleton law)">
                filter IsAllowable
                then extract instr [
                    if IsTerm
                    then BeforeFor -> prod
                    else if IsCap
                    then TradeFor -> prod
                    else TradeFor -> prod
                        then extract ValOf
                ]
                as "b2"

            reporting rule B3FlatTail from Instr: <"b3 - a nested-else rung whose condition is NOT a bare fn call keeps the flat else-if">
                filter IsAllowable
                then if PremRule exists
                    then PremRule
                    else if alt exists
                    then AltRule
                as "b3"
            """;

    // =========================================================================
    // Part A — the seat (RED at the pre-seat blob)
    // =========================================================================

    /** a1 — the lambda-route TOP rung: the bare-fn condition hoists ABOVE the then-arg declaration and guards the header. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_lambdaTopRungHoistsAboveTheDecl() throws IOException {
        String out = rule("A1TopRungRule.java");
        assertContains(out, "final Boolean _boolean = isTerm.evaluate(thenArg.get());\n\t\t\t\t\tfinal MapperS<Prod> _thenArg;\n\t\t\t\t\tif ((_boolean == null ? false : _boolean)) {");
        assertContains(out, "_thenArg = MapperS.of(beforeFor.evaluate(thenArg.get())).<Prod>map(\"getProd\", trade -> trade.getProd());");
        assertNotContains(out, "if (isTerm.evaluate(thenArg.get()).getOrDefault(false)) {");
        assertEquals(List.of(1, 1, 0), triple(countT4(out)), "(T4dec, T4if, T4inline) in a1");
    }

    /** a2 — a boolean RULE as the top condition hoists the same way (the predicate admits RRule). */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_lambdaTopRungRuleConditionHoists() throws IOException {
        String out = rule("A2RuleTopRungRule.java");
        assertContains(out, "final Boolean _boolean = isTermRuleRule.evaluate(thenArg.get());");
        assertContains(out, "if ((_boolean == null ? false : _boolean)) {");
        assertEquals(List.of(1, 1, 0), triple(countT4(out)), "(T4dec, T4if, T4inline) in a2");
    }

    /** a3 — the rule then-chain ladder's nested-else bare-fn rung deepens the else and hoists (the #399 tail on the rule path). */
    @Test
    @EnabledIf("builtinsAvailable")
    void a3_ruleThenChainNestedElseTailHoists() throws IOException {
        String out = rule("A3RuleTailRule.java");
        assertContains(out, "} else {\n\t\t\t\tfinal Boolean _boolean = isCap.evaluate(thenArg.get());\n\t\t\t\tif ((_boolean == null ? false : _boolean)) {\n\t\t\t\t\tifThenElseResult = MapperS.of(altRuleRule.evaluate(thenArg.get()));\n\t\t\t\t} else {\n\t\t\t\t\tifThenElseResult = MapperS.<BigDecimal>ofNull();\n\t\t\t\t}\n\t\t\t}");
        assertNotContains(out, "} else if (MapperS.of(isCap.evaluate(thenArg.get())).getOrDefault(false)) {");
        assertEquals(List.of(1, 1, 0), triple(countT4(out)), "(T4dec, T4if, T4inline) in a3");
    }

    /** a4 — LAW 77: the a1 and a3 shapes through the REAL {@code IRGeneration.functionGenerator} seam ({@code -Pir-on}). */
    @Test
    @EnabledIf("builtinsAndIrProviderAvailable")
    void a4_bothSeatsOnIrRoute() throws IOException {
        String a1 = lookup(fixtureOnIrRoute(), "reports/A1TopRungRule.java");
        assertContains(a1, "final Boolean _boolean = isTerm.evaluate(thenArg.get());\n\t\t\t\t\tfinal MapperS<Prod> _thenArg;");
        String a3 = lookup(fixtureOnIrRoute(), "reports/A3RuleTailRule.java");
        assertContains(a3, "} else {\n\t\t\t\tfinal Boolean _boolean = isCap.evaluate(thenArg.get());");
    }

    // =========================================================================
    // Part B — placement pins (GREEN in BOTH states)
    // =========================================================================

    /** b1 — a NON-bare top condition keeps the inline `exists(…).getOrDefault(false)` header. */
    @Test
    @EnabledIf("builtinsAvailable")
    void b1_nonBareTopConditionUnchanged() throws IOException {
        String out = rule("B1ExistsTopRule.java");
        assertContains(out, "final MapperS<Prod> _thenArg;\n\t\t\t\t\tif (exists(");
        assertEquals(List.of(0, 0, 0), triple(countT4(out)), "(T4dec, T4if, T4inline) in b1");
    }

    /** b2 — two bare-fn rungs in one lambda ladder keep the flat form (the singleton law: no restructure anywhere). */
    @Test
    @EnabledIf("builtinsAvailable")
    void b2_twoBareRungsKeepTheFlatForm() throws IOException {
        String out = rule("B2TwoBareRungsRule.java");
        assertContains(out, "if (isTerm.evaluate(thenArg.get()).getOrDefault(false)) {");
        assertContains(out, "} else if (isCap.evaluate(thenArg.get()).getOrDefault(false)) {");
        assertNotContains(out, "final Boolean ");
    }

    /** b3 — a nested-else rung whose condition is NOT a bare fn call keeps the flat else-if. */
    @Test
    @EnabledIf("builtinsAvailable")
    void b3_nonBareNestedElseRungStaysFlat() throws IOException {
        String out = rule("B3FlatTailRule.java");
        assertContains(out, "} else if (exists(");
        assertNotContains(out, "final Boolean ");
    }

    // =========================================================================
    // Part C — the corpus (drr 5.61.0: the carriers + the whole-cell control; drr 7.0.0: LAW 79)
    // =========================================================================

    private static final Path DRR561_CELL_ROOT = Path.of("../test-corpus/drr/drr-5.61.0");
    private static final Path DRR561_GOLDEN = DRR561_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path DRR7_CELL_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path DRR7_GOLDEN = DRR7_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    static boolean drr561Available() {
        return Files.isDirectory(DRR561_GOLDEN);
    }

    static boolean drr7Available() {
        return Drr7Corpus.gate(Files.isDirectory(DRR7_GOLDEN), BareFnCondHoistEveryContextSeatTest.class);
    }

    private static final String ASIC_NOTIONAL_CCY_LEG1 = "drr/regulation/asic/rewrite/trade/reports/NotionalCurrencyLeg1Rule.java";
    private static final String ESMA_OPTION_PREMIUM_CCY = "drr/regulation/esma/emir/refit/trade/reports/OptionPremiumCurrencyRule.java";
    private static final String ESMA_OPTION_PREMIUM_AMT = "drr/regulation/esma/emir/refit/trade/reports/OptionPremiumAmountRule.java";

    /** c1 — drr 5.61.0 asic NotionalCurrencyLeg1Rule whole-file lock (the lambda top-rung carrier, F4-only). */
    @Test
    @EnabledIf("drr561Available")
    void corpus_c1_asicNotionalCurrencyLeg1ByteIdentical() throws IOException {
        lock(ASIC_NOTIONAL_CCY_LEG1);
    }

    /** c2 — drr 5.61.0 esma OptionPremiumCurrencyRule whole-file lock (the rule-path tail carrier). */
    @Test
    @EnabledIf("drr561Available")
    void corpus_c2_esmaOptionPremiumCurrencyByteIdentical() throws IOException {
        lock(ESMA_OPTION_PREMIUM_CCY);
    }

    /** c3 — drr 5.61.0 esma OptionPremiumAmountRule whole-file lock (the tail carrier with the BigInteger arm the fork already hoists). */
    @Test
    @EnabledIf("drr561Available")
    void corpus_c3_esmaOptionPremiumAmountByteIdentical() throws IOException {
        lock(ESMA_OPTION_PREMIUM_AMT);
    }

    /**
     * control0 — golden is the oracle (the frozen drr 5.61.0 tree, every kind): T1 the hoist declarations
     * {@code final Boolean _boolean|booleanN = …} (1,152 sites), T2 the null-safe guards {@code if ((… == null ? false : …))}
     * (1,152 — decl and guard pair everywhere), T3 the fork's inline bare-fn {@code if (<fn>.evaluate(…).getOrDefault(false))}
     * / flattened {@code } else if (MapperS.of(<fn>.evaluate(…)).getOrDefault(false))} forms (ZERO) — over 754 files.
     */
    @Test
    @EnabledIf("drr561Available")
    void corpus_control0_goldenDrr561IsTheOracle() throws IOException {
        Map<String, int[]> g = scan(readGoldenTree(DRR561_GOLDEN));
        assertEquals(754, g.size(), "golden token-bearing files (the whole-cell control's domain)");
        assertEquals(List.of(1152, 1152, 0), List.of(sites(g, 0), sites(g, 1), sites(g, 2)), "golden (T1, T2, T3) sites");
        assertEquals(List.of(1, 1, 0), triple(g.get(ASIC_NOTIONAL_CCY_LEG1)), "golden c1 carrier");
        assertEquals(List.of(1, 1, 0), triple(g.get(ESMA_OPTION_PREMIUM_CCY)), "golden c2 carrier");
    }

    /**
     * control1 — the FORK's WHOLE generated drr 5.61.0 cell (every kind, LAW 72): over the UNION of the files either
     * tree carries a token in (a missing side counts as all-zero, LAW 79), the per-file (T1, T2, T3) triples agree
     * FILE BY FILE except the named pre-existing residue — an over-fire (a hoist where golden has none) and an
     * under-fire (a carrier still inline) both fail here; the domain equals golden's 754.
     */
    @Test
    @EnabledIf("drr561Available")
    void corpus_control1_forkDrr561WholeCellHoistsEqualGoldenFileByFile() throws IOException {
        assertNotNull(drr561Output, "drr 5.61.0 generation did not run");
        assertEquals(List.of(), drr561GenErrors, "drr 5.61.0 reported a generation error — the scan is incomplete");
        assertUnionEqual(scan(drr561Output), scan(readGoldenTree(DRR561_GOLDEN)), drr561Output.keySet(),
                "fork", "golden", KNOWN_RESIDUE_DRR561, 754);
    }

    /**
     * The named pre-existing residue of OTHER families in this cell (LAW 73: the set, not the count) — filled from
     * the GREEN read of this suite (files whose hoist counts differ for reasons outside this law: nested ladders the
     * fork still flattens to a ternary, F12/F8), and re-pinned in the commit of whichever seat heals them.
     */
    private static final List<String> KNOWN_RESIDUE_DRR561 = List.of(
            // the four cftc Notional* rows LEFT this list at seat 32: law C.3 (ruleThenArmLadderNestedTreeAdmit) healed
            // NotionalAmountLeg1/2 + NotionalCurrencyLeg2 WHOLE in drr 5.61.0 and IMPROVED NotionalCurrencyLeg1 to a tuple this scan reads as golden's; transcribed from this control's own print (C3-trip1*.log).
            // LAW 81 — `drr/regulation/common/functions/Package_Contract_Price_Monetary.java fork=[0, 0, 0]
            // golden=[5, 5, 0]` HEALED at seat-27 law B (facet addCondDistributionTopLevel: the whole-output
            // ADD's TOP-level bare-fn condition now hoists), so this entry is re-pinned away.
            // LAW 81 — CallAmountRule.java and PutAmountRule.java (both fork=[2, 2, 0] golden=[1, 1, 0])
            // LEFT this list at seat 31: law 3 rung (b) (facet inLambdaBoolHoistShadowEscape - the
            // in-lambda `Boolean` hoist rides the per-lambda session sub-group, so it escapes the
            // method-level `_boolean` sibling as `__boolean`) healed both files WHOLE in drr 5.61.0. The
            // fork's second `_boolean` hoist is now `__boolean`, so the T1/T2 counts equal golden's.
            // RE-MEASURED at the seat-31 chain head f2a4d5c0 (5 rows, transcribed VERBATIM from
            // control1's own failing print); the union domain 754 is UNMOVED (golden's counts are non-zero).
            );  // LAW 81 (seat 33, law F.B): the NotionalLeg2Rule row LEFT - the file is WHOLE (FB-trip1.log print)

    /**
     * control2 — LAW 77 route parity: the whole drr 5.61.0 cell generated through the REAL {@code IRGeneration} seams
     * ({@code -Pir-on}) carries the SAME per-file triples as the legacy-route render, file by file over the UNION.
     */
    @Test
    @EnabledIf("drr561AndIrProviderAvailable")
    void corpus_control2_irRouteDrr561HoistsEqualLegacyRouteFileByFile() throws IOException {
        assertNotNull(drr561Output, "drr 5.61.0 legacy-route generation did not run");
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "5.61.0", DRR561_CELL_ROOT), new ArrayList<>());
        assertUnionEqual(scan(irOut), scan(drr561Output), irOut.keySet(), "ir", "legacy", List.of(),
                scan(drr561Output).size());
    }

    /**
     * control3 — LAW 79: the predicate reaches every cell; the fork's whole drr 7.0.0 cell is compared with its golden
     * (862 / 862 / 0 over 633 files) the same way — the named residue is that cell's own pre-existing set.
     */
    @Test
    @EnabledIf("drr7Available")
    void corpus_control3_forkDrr7WholeCellHoistsEqualGoldenFileByFile() throws IOException {
        List<String> errs = new ArrayList<>();
        Map<String, String> out = generateCell(
                new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", DRR7_CELL_ROOT), errs);
        assertEquals(List.of(), errs, "drr 7.0.0 reported a generation error — the scan is incomplete");
        Map<String, int[]> g = scan(readGoldenTree(DRR7_GOLDEN));
        assertEquals(List.of(862, 862, 0), List.of(sites(g, 0), sites(g, 1), sites(g, 2)), "golden drr 7.0.0 (T1, T2, T3)");
        assertUnionEqual(scan(out), g, out.keySet(), "fork", "golden", KNOWN_RESIDUE_DRR7, g.size());
    }

    /**
     * The named pre-existing residue of other families in drr 7.0.0 — EMPTY since seat 26
     * law D ({@code stmtSeatItemCondNullInit}): the {@code EconomicTermsForProduct} entry
     * named the default-operand conditional whose {@code final Boolean _boolean} hoist this
     * suite's token counts; law D landed the whole null-init item form, this control FIRED
     * on its landing (LAW 81 — the pin promised "re-pinned in the commit of whichever seat
     * heals them") and the entry left in that commit.
     */
    private static final List<String> KNOWN_RESIDUE_DRR7 = List.of();

    private static List<Integer> triple(int[] c) {
        assertNotNull(c, "the file carries no token");
        return List.of(c[0], c[1], c[2]);
    }

    private static void assertUnionEqual(Map<String, int[]> a, Map<String, int[]> b,
            java.util.Set<String> emittedA, String aName, String bName, List<String> knownResidue,
            int expectedDomain) {
        List<String> mismatched = new ArrayList<>();
        List<String> notEmitted = new ArrayList<>();
        java.util.Set<String> universe = new java.util.TreeSet<>(a.keySet());
        universe.addAll(b.keySet());
        int[] zero = new int[3];
        for (String key : universe) {
            if (!emittedA.contains(key)) {
                notEmitted.add(key);
                continue;
            }
            int[] ac = a.getOrDefault(key, zero);
            int[] bc = b.getOrDefault(key, zero);
            if (!java.util.Arrays.equals(ac, bc)) {
                mismatched.add(key + " " + aName + "=" + java.util.Arrays.toString(ac)
                        + " " + bName + "=" + java.util.Arrays.toString(bc));
            }
        }
        assertEquals(knownResidue, mismatched,
                "(T1, T2, T3) differ beyond the named residue in " + mismatched.size() + " file(s)");
        assertEquals(List.of(), notEmitted,
                "token-bearing files " + bName + " carries that " + aName + " does not emit at all");
        assertEquals(expectedDomain, universe.size(),
                "the union domain must equal the oracle's token-bearing files (" + expectedDomain + ")");
    }

    // =========================================================================
    // The scan — (T1, T2, T3) per file over CODE only (comments stripped; a line walk)
    // =========================================================================

    /** True for the fork's inline bare-fn condition: `if (<ident>.evaluate(…).getOrDefault(false)) {` or the flattened `} else if (MapperS.of(<ident>.evaluate(…)).getOrDefault(false)) {` (no navigation between the wrap and the call). */
    static boolean inlineBareFnCondition(String s) {
        if (!s.endsWith(".getOrDefault(false)) {")) {
            return false;
        }
        String head = s.startsWith("} else if (") ? s.substring("} else if (".length())
                : s.startsWith("if (") ? s.substring("if (".length()) : null;
        if (head == null) {
            return false;
        }
        boolean wrapped = head.startsWith("MapperS.of(");
        String call = wrapped ? head.substring("MapperS.of(".length()) : head;
        int ev = call.indexOf(".evaluate(");
        if (ev <= 0) {
            return false;
        }
        String receiver = call.substring(0, ev);
        if (!SourceVersion.isIdentifier(receiver)) {
            return false;
        }
        int close = matchingClose(call, ev + ".evaluate".length());
        if (close < 0) {
            return false;
        }
        String tail = call.substring(close + 1);
        // no navigation between the call and the guard: the tail IS the guard (wrapped: the wrap's own `)` first)
        return tail.equals(wrapped ? ").getOrDefault(false)) {" : ".getOrDefault(false)) {");
    }

    private static int matchingClose(String s, int open) {
        int depth = 0;
        for (int i = open; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"') {
                i++;
                while (i < s.length() && s.charAt(i) != '"') {
                    if (s.charAt(i) == '\\') {
                        i++;
                    }
                    i++;
                }
                continue;
            }
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    static int[] countT4(String java) {
        int[] c = new int[3];
        for (String raw : codeOnly(java).split("\n")) {
            String s = raw.strip();
            if (s.startsWith("final Boolean _boolean = ") || isNumberedBoolDecl(s)) {
                c[0]++;
            }
            if (s.startsWith("if ((_boolean == null ? false : _boolean))")
                    || s.startsWith("} else if ((_boolean == null ? false : _boolean))")
                    || isNumberedBoolGuard(s)) {
                c[1]++;
            }
            if (inlineBareFnCondition(s)) {
                c[2]++;
            }
        }
        return c;
    }

    private static boolean isNumberedBoolDecl(String s) {
        if (!s.startsWith("final Boolean boolean")) {
            return false;
        }
        int sp = s.indexOf(' ', "final Boolean boolean".length());
        if (sp < 0) {
            return false;
        }
        String digits = s.substring("final Boolean boolean".length(), sp);
        return !digits.isEmpty() && digits.chars().allMatch(Character::isDigit) && s.startsWith(" = ", sp);
    }

    private static boolean isNumberedBoolGuard(String s) {
        String body = s.startsWith("} else if ((boolean") ? s.substring("} else if ((".length())
                : s.startsWith("if ((boolean") ? s.substring("if ((".length()) : null;
        if (body == null) {
            return false;
        }
        int sp = body.indexOf(' ');
        if (sp < 0) {
            return false;
        }
        String name = body.substring(0, sp);
        String digits = name.substring("boolean".length());
        return !digits.isEmpty() && digits.chars().allMatch(Character::isDigit)
                && body.startsWith(" == null ? false : " + name + "))", sp);
    }

    private static Map<String, int[]> scan(Map<String, String> tree) {
        Map<String, int[]> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : tree.entrySet()) {
            if (!e.getValue().contains("Boolean") && !e.getValue().contains(".evaluate(")) {
                continue;
            }
            int[] c = countT4(e.getValue());
            if (c[0] + c[1] + c[2] > 0) {
                out.put(e.getKey(), c);
            }
        }
        return out;
    }

    private static int sites(Map<String, int[]> per, int idx) {
        int n = 0;
        for (int[] c : per.values()) {
            n += c[idx];
        }
        return n;
    }

    /** Strip line and block comments so a javadoc never counts as code. */
    private static String codeOnly(String java) {
        StringBuilder sb = new StringBuilder(java.length());
        int i = 0;
        int n = java.length();
        while (i < n) {
            char ch = java.charAt(i);
            if (ch == '"') {
                int j = i + 1;
                while (j < n && java.charAt(j) != '"') {
                    if (java.charAt(j) == '\\') {
                        j++;
                    }
                    j++;
                }
                sb.append(java, i, Math.min(j + 1, n));
                i = j + 1;
            } else if (ch == '/' && i + 1 < n && java.charAt(i + 1) == '/') {
                while (i < n && java.charAt(i) != '\n') {
                    i++;
                }
            } else if (ch == '/' && i + 1 < n && java.charAt(i + 1) == '*') {
                int end = java.indexOf("*/", i + 2);
                i = end < 0 ? n : end + 2;
            } else {
                sb.append(ch);
                i++;
            }
        }
        return sb.toString();
    }

    private static Map<String, String> readGoldenTree(Path root) throws IOException {
        Map<String, String> out = new LinkedHashMap<>();
        try (var stream = Files.walk(root)) {
            for (Path p : stream.filter(q -> q.toString().endsWith(".java")).sorted().toList()) {
                out.put(root.relativize(p).toString().replace('\\', '/'), Files.readString(p));
            }
        }
        return out;
    }

    // =========================================================================
    // Cell generation (drr 5.61.0 / 7.0.0, the legacy route; the IR route for control2)
    // =========================================================================

    private static Map<String, String> drr561Output;
    private static List<String> drr561GenErrors;

    @BeforeAll
    static void generateCells() throws IOException {
        if (drr561Available()) {
            List<String> errs = new ArrayList<>();
            drr561Output = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "5.61.0", DRR561_CELL_ROOT), errs);
            drr561GenErrors = errs;
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
        var enumGen = new EnumGenerator(gm);
        var funcGen = new FunctionGenerator(gm, typeTranslator, typeUtil);
        var ruleGen = new RuleGenerator(gm, typeTranslator, funcGen);
        var reportGen = new ReportGenerator(gm, typeTranslator, funcGen);
        var dataRuleGen = new DataRuleGenerator(gm, typeTranslator, typeUtil);
        var labelProviderGen = new LabelProviderGenerator(
                gm, typeTranslator, new DeepFeatureCallUtil(gm::getType),
                new LabelProviderGeneratorUtil());
        Map<String, String> output = new LinkedHashMap<>();
        for (RModel model : corpus.workspace().files()) {
            if (gm.shouldGenerate(model)) {
                String version = gm.version(model);
                collect(errors, pojoGen.generateClasses(model, version, output));
                collect(errors, choiceGen.generateClasses(model, version, output));
                collect(errors, enumGen.generateClasses(model, version, output));
                collect(errors, ruleGen.generateClasses(model, version, output));
                collect(errors, reportGen.generateClasses(model, version, output));
                collect(errors, dataRuleGen.generateClasses(model, version, output));
                collect(errors, labelProviderGen.generateClasses(model, version, output));
            }
        }
        collect(errors, funcGen.generateWithErrors(output));
        return output;
    }

    /**
     * The cell through the REAL {@code IRGeneration} seams — the D11 ON ring's wiring (the flag set for the render
     * and restored after; the provider asserted present; the function seam asserted to hand back the IR-route
     * generator, so this can never silently be an OFF-route render).
     */
    private static Map<String, String> generateCellOnIrRoute(D11CorpusRegressionTest.CellSpec cell,
            List<String> errors) throws IOException {
        String previous = System.getProperty(IRGeneration.PROPERTY);
        System.setProperty(IRGeneration.PROPERTY, "true");
        try {
            assertNotNull(IRGeneration.providerOrNull(),
                    "the IR provider must be resolvable under -Pir-on, else this is not an ON-route render");
            var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
            var gm = new GeneratorModel(corpus.workspace(),
                    D11CorpusRegressionTest.emissionFilter(cell));
            var typeUtil = new JavaTypeUtil();
            var typeTranslator = new JavaTypeTranslator(typeUtil);
            var pojoGen = IRGeneration.modelObjectGenerator(gm, typeTranslator, typeUtil);
            var choiceGen = IRGeneration.choiceObjectGenerator(gm, typeTranslator, typeUtil, pojoGen);
            var enumGen = IRGeneration.enumGenerator(gm);
            FunctionGenerator funcGen = IRGeneration.functionGenerator(gm, typeTranslator, typeUtil);
            assertTrue(!funcGen.getClass().equals(FunctionGenerator.class),
                    "the seam must hand back the IR-route FunctionGenerator, got " + funcGen.getClass());
            var ruleGen = new RuleGenerator(gm, typeTranslator, funcGen);
            var reportGen = new ReportGenerator(gm, typeTranslator, funcGen);
            var dataRuleGen = new DataRuleGenerator(gm, typeTranslator, typeUtil);
            var labelProviderGen = new LabelProviderGenerator(
                    gm, typeTranslator, new DeepFeatureCallUtil(gm::getType),
                    new LabelProviderGeneratorUtil());
            Map<String, String> output = new LinkedHashMap<>();
            for (RModel model : corpus.workspace().files()) {
                if (gm.shouldGenerate(model)) {
                    String version = gm.version(model);
                    collect(errors, IRGeneration.generateClasses(pojoGen, model, version, output));
                    collect(errors, IRGeneration.generateClasses(choiceGen, model, version, output));
                    collect(errors, IRGeneration.generateClasses(enumGen, model, version, output));
                    collect(errors, ruleGen.generateClasses(model, version, output));
                    collect(errors, reportGen.generateClasses(model, version, output));
                    collect(errors, dataRuleGen.generateClasses(model, version, output));
                    collect(errors, labelProviderGen.generateClasses(model, version, output));
                }
            }
            collect(errors, funcGen.generateWithErrors(output));
            return output;
        } finally {
            if (previous == null) {
                System.clearProperty(IRGeneration.PROPERTY);
            } else {
                System.setProperty(IRGeneration.PROPERTY, previous);
            }
        }
    }

    private static void collect(List<String> sink, List<GenerationException> errors) {
        if (errors != null) {
            errors.forEach(e -> sink.add(e.getTargetPath() + " — " + e));
        }
    }

    private static void lock(String path) throws IOException {
        assertNotNull(drr561Output, "drr 5.61.0 generation did not run — corpus unavailable?");
        List<String> lockedErrors = drr561GenErrors.stream().filter(e -> e.contains(path)).toList();
        assertTrue(lockedErrors.isEmpty(),
                "the generator reported errors for the locked file " + path + ": " + lockedErrors);
        String generated = drr561Output.get(path);
        assertNotNull(generated, "not generated in drr 5.61.0: " + path);
        Path goldenPath = DRR561_GOLDEN.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "golden missing: " + goldenPath);
        assertEquals(normalize(Files.readString(goldenPath)), normalize(generated),
                "generated drr 5.61.0 output must byte-match golden (newline-normalized) for "
                + path + " — seat 24 F4: the bare fn/rule-call condition hoists in every context.");
    }

    // =========================================================================
    // Fixture harness
    // =========================================================================

    private static RLinkingResult linking;
    private static RModel mainModel;
    private static Map<String, String> fixtureOut;

    private static void link() throws IOException {
        if (linking == null) {
            RModel main = AstBuilder.buildFromString(MODEL, "seat24f4.rosetta");
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
            fixtureOut = render(m -> "census.seat24f4".equals(m.namespace()));
        }
        return fixtureOut;
    }

    private static Map<String, String> fixtureOutIr;

    /** The fixture's rules through the REAL {@code IRGeneration.functionGenerator} seam (the ON route). */
    private static Map<String, String> fixtureOnIrRoute() throws IOException {
        if (fixtureOutIr == null) {
            link();
            String previous = System.getProperty(IRGeneration.PROPERTY);
            System.setProperty(IRGeneration.PROPERTY, "true");
            try {
                assertNotNull(IRGeneration.providerOrNull(),
                        "the IR provider must be resolvable under -Pir-on, else this is not an ON-route render");
                GeneratorModel gm = new GeneratorModel(linking.workspace(),
                        m -> "census.seat24f4".equals(m.namespace()));
                JavaTypeUtil typeUtil = new JavaTypeUtil();
                JavaTypeTranslator tt = new JavaTypeTranslator(typeUtil);
                FunctionGenerator fg = IRGeneration.functionGenerator(gm, tt, typeUtil);
                assertTrue(!fg.getClass().equals(FunctionGenerator.class),
                        "the seam must hand back the IR-route FunctionGenerator, got " + fg.getClass());
                RuleGenerator ruleGen = new RuleGenerator(gm, tt, fg);
                Map<String, String> out = new LinkedHashMap<>();
                List<String> errors = new ArrayList<>();
                ruleGen.generateClasses(mainModel, "1.0", out)
                        .forEach(e -> errors.add(e.getTargetPath() + " — " + e));
                fg.generateWithErrors(out)
                        .forEach(e -> errors.add(e.getTargetPath() + " — " + e));
                if (!errors.isEmpty()) {
                    throw new AssertionError("fixture generation errors on the IR route: " + errors);
                }
                fixtureOutIr = out;
            } finally {
                if (previous == null) {
                    System.clearProperty(IRGeneration.PROPERTY);
                } else {
                    System.setProperty(IRGeneration.PROPERTY, previous);
                }
            }
        }
        return fixtureOutIr;
    }

    private static String rule(String fileName) throws IOException {
        return lookup(fixture(), "reports/" + fileName);
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
            throw new AssertionError("[BareFnCondHoistEveryContextSeatTest] builtins parse"
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
        assertFalse(out.contains(token),
                "forbidden token present: " + token + "\n--- in output:\n" + out);
    }
}
