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
 * SEAT 24, law F5 — facet {@code bigIntegerLiteralRulePath}: <b>a beyond-long int literal at a {@code number} seat
 * hoists {@code final BigInteger bigIntegerN = new BigInteger("…");} at the nearest statement seat — the method's
 * statement-hoist sink or the enclosing extract lambda's drain channel — and is consumed null-guarded: the VALUE
 * ternary {@code bigInteger == null ? MapperS.<BigDecimal>ofNull() : MapperS.of(new BigDecimal(bigInteger))} (bare as a
 * whole arm, parenthesised as an operand) or the ITEM ternary {@code (bigInteger == null ? null : new BigDecimal(
 * bigInteger))} at an evaluate-argument or {@code default} seat — on the RULE path as on the function path</b>. Golden
 * drr 5.61.0: 24 hoist declarations over 23 files (18 value + 6 item consumptions) and ZERO bare {@code new
 * BigInteger("…")} outside a declaration — in EVERY golden cell (the seat-24 golden census, T5raw = 0).
 *
 * <p><b>The defect — and the producer the probe REFUTED (LAW 72).</b> The close census charted {@code LiteralHandler
 * .hoistBigIntegerLiteralOrNull} declining for want of a sink. The seat-24 LAW-75 probe over all 275 rows on BOTH
 * routes showed the hoist is never ATTEMPTED: {@code bdCtx=false} at every beyond-long literal (497 lines) — seat 22's
 * {@code HandlerHelper.intLiteralConditionalArmExpectedType} EXCLUDES {@code bitLength() > 63} by design (its comment
 * names this family), so the literal renders the raw-int {@code MapperS.of(new BigInteger("…"))} and the #398 text arm
 * then mis-wraps it {@code MapperS.of(BigDecimal.valueOf(new BigInteger(…)))}. Three seats, ONE producer: (i) the VALUE
 * arm inside an extract lambda ({@code sink=false}) — jfsa {@code NotionalLeg1}, asic {@code NotionalAmountLeg1/2},
 * esma/fca/mas {@code NotionalAmountOf*}; (ii) the evaluate-ARG of a then-RECEIVER call — iosco {@code CDECall/PutAmount}
 * ({@code route=STATEMENT_SINK}, the 2-statement then form compiles its base argument on a sink-less scope); (iii) the
 * {@code default} RIGHT ({@code leftNumber=true sink=false}, the {@code bitLength() <= 63} gate) — asic {@code
 * TotalNotionalQuantityLeg1}, mas {@code TotalNotionalQuantityOfTheContractOrLeg1} and {@code OfLeg2} (two defaults
 * → {@code bigInteger0/1}). The IR route's leaf emitter already DEFERS a beyond-long literal to the legacy handler
 * (LAW 77: one seat for both routes).
 *
 * <p><b>The seat.</b> {@code intLiteralConditionalArmExpectedType} drops its beyond-long exclusion (both routes consult
 * it); {@code LiteralHandler.registerBigIntegerLiteralHoistOrNull} is THE producer (the sink's session or, inside an
 * extract lambda, the any-depth session + a marker-classed {@code HandlerHelper.BigIntegerLiteralHoist} on the pending
 * lambda channel, admitted by the elseless / effective-else / ladder-arm drains — the nested tree drains everything);
 * the VALUE ternary is bare for a whole conditional arm and parenthesised for an operand ({@code HandlerHelper.
 * bigIntegerValueTernary}); the {@code default} seat consults the producer for the ITEM ternary ({@code
 * bigIntegerItemTernary}); the 2-statement then form marks the method session as its base argument's sink and lifts
 * the registered declarations above the {@code thenArg} line, so the existing evaluate-arg {@code bigInt→BigDecimal}
 * arm fires.
 *
 * <p><b>RED at the pre-seat blob</b> (this suite kept — MEASURED by the receipts chain at the final head, LAW 78/82):
 * a1, a2, a3, a4, {@code corpus_c1}–{@code c5}, {@code corpus_control1} — 10F (under {@code -Pir-on} also a5 — 11F);
 * b1, b2, {@code control0}, {@code control3} GREEN in both states ({@code control2} compares routes). <b>LAW 66/76
 * mutations</b> (each applied → run → reverted at the FINAL head; the failing SETS are the chain's MEASUREMENT, not
 * the charter's prediction): (i) the predicate's beyond-long exclusion restored → a1, a2, c1, c4, control1 — 5F;
 * (ii) the lambda channel deleted (a sink-less literal keeps the inline form) → a1, a2, a3, c1, c3, c4, c5, control1
 * — 8F; (iii) the {@code default} seat's item form reverted → a3, c3, c5, control1 — 4F; (iv) the base-argument sink
 * dropped → a4, c2, control1 — 3F; (v) the whole-arm ternary parenthesised → a1, a2, c1, c4 + law F4's
 * {@code BareFnCondHoistEveryContextSeatTest.corpus_c3} (the esma {@code OptionPremiumAmountRule} lock carries a
 * bare VALUE-seat arm too) — 5F: the TEXT moves at every bare value-arm carrier while the hoist COUNT does not, so
 * {@code control1} (a site-count scan) is blind to it — seat 23's arm-COUNT-control lesson (LAW 82) in this suite.
 */
class BigIntegerLiteralRulePathSeatTest {

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

    private static final String BIG = "9999999999999999999999999";

    /**
     * One {@code from} type with a function filter, boolean functions, number functions (the {@code CDEFXNotional} /
     * {@code ExtractCDECallAmount} twins), a formatting function (the {@code FormatToShortFraction5DecimalNumber} twin),
     * and one reporting rule per seat.
     */
    private static final String MODEL = """
            namespace census.seat24f5
            version "1.0.0"

            type Instr:
                prem number (0..1)
                alt number (0..1)
                isCap boolean (0..1)
                isOpt boolean (0..1)
                isTerm boolean (0..1)
                allowed boolean (0..1)

            func IsAllowable: <"IsAllowableAction twin - the function filter">
                inputs:
                    i Instr (0..1)
                output:
                    result boolean (1..1)
                set result:
                    i -> allowed = True

            func IsCap: <"IsFXForward / IsCommoditySwap twin">
                inputs:
                    i Instr (0..1)
                output:
                    result boolean (1..1)
                set result:
                    i -> isCap = True

            func IsTerm: <"IsActionTypeTERM twin - the bracket extract's top condition (law F4's seat, already landed)">
                inputs:
                    i Instr (0..1)
                output:
                    result boolean (1..1)
                set result:
                    i -> isTerm = True

            func IsOpt: <"IsEquity twin">
                inputs:
                    i Instr (0..1)
                output:
                    result boolean (1..1)
                set result:
                    i -> isOpt = True

            func ValOf: <"CDEFXNotional twin - a number function">
                inputs:
                    i Instr (0..1)
                output:
                    v number (0..1)
                set v:
                    i -> prem

            func ExtractAmt: <"ExtractCDECallAmount twin - the then-receiver call whose second argument is the literal">
                inputs:
                    i Instr (0..1)
                    cap number (0..1)
                output:
                    r number (0..1)
                set r:
                    i -> prem

            func FmtNum: <"FormatToShortFraction5DecimalNumber twin">
                inputs:
                    n number (0..1)
                output:
                    s string (1..1)
                set s:
                    "x"

            func FnHoist: <"b2 - the FUNCTION path's existing sink hoist (the operand form, parenthesised)">
                inputs:
                    i Instr (0..1)
                output:
                    r boolean (1..1)
                set r:
                    i -> prem <= 9999999999999999999999999

            reporting rule A1ValueArm from Instr: <"a1 - THE jfsa NotionalLeg1 SHAPE: the literal is the then-arm of a conditional nested in an extract-lambda ladder">
                filter IsAllowable
                then extract
                    if IsCap(item)
                    then (if ValOf(item) = 0 then 9999999999999999999999999 else ValOf(item))
                    else alt
                as "a1"

            reporting rule A2ElselessArm from Instr: <"a2 - THE asic NotionalAmountLeg1 SHAPE: the literal is the then-arm of an effective-else conditional in a nested then-extract lambda">
                filter IsAllowable
                then extract instr [
                    if IsTerm
                    then ValOf
                    else ValOf
                        then extract (if item = 0 and IsCap(instr) then 9999999999999999999999999 else item)
                ]
                as "a2"

            reporting rule A3DefaultSeat from Instr: <"a3 - THE mas TotalNotionalQuantityOfLeg2 SHAPE: two `default` literals in one extract lambda, numbered bigInteger0/1">
                filter IsAllowable
                then extract
                    if IsCap(item)
                    then ValOf(item) default 9999999999999999999999999
                    else if IsOpt(item)
                    then prem default 9999999999999999999999999
                as "a3"

            reporting rule A4ArgSeat from Instr: <"a4 - THE iosco CDECallAmount SHAPE: the literal is the second argument of the then-receiver call">
                ExtractAmt(item, 9999999999999999999999999)
                    then FmtNum
                as "a4"

            reporting rule C1NegativeArm from Instr: <"c1 - a NEGATIVE beyond-long literal at the a1 seat (PR #609): AstBuilder folds the minus into the RIntLiteral; the #300 BigInteger arm declines it as the retired digit scan did">
                filter IsAllowable
                then extract
                    if IsCap(item)
                    then (if ValOf(item) = 0 then -9999999999999999999999999 else ValOf(item))
                    else alt
                as "c1"

            reporting rule B1WithinLong from Instr: <"b1 - a WITHIN-long literal at the same seat keeps seat 22's BigDecimal.valueOf(N)">
                filter IsAllowable
                then extract
                    if IsCap(item)
                    then (if ValOf(item) = 0 then 5 else ValOf(item))
                    else alt
                as "b1"
            """;

    private static final String DECL = "final BigInteger bigInteger = new BigInteger(\"" + BIG + "\");";
    private static final String VALUE_BARE =
            "return bigInteger == null ? MapperS.<BigDecimal>ofNull() : MapperS.of(new BigDecimal(bigInteger));";

    // =========================================================================
    // Part A — the seat (RED at the pre-seat blob)
    // =========================================================================

    /** a1 — the in-lambda VALUE arm: the hoist lands inside the owning branch, the return is the bare value ternary. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_inLambdaValueArmHoistsAndReturnsTheBareTernary() throws IOException {
        String out = rule("A1ValueArmRule.java");
        assertContains(out, DECL + "\n\t\t\t\t\t\t\t" + VALUE_BARE);
        assertNotContains(out, "new BigInteger(\"" + BIG + "\"))");
        assertNotContains(out, "return (bigInteger == null");
        assertEquals(List.of(1, 1, 0, 0), quad(countT5(out)), "(T5dec, T5guardS, T5guardI, T5raw) in a1");
    }

    /**
     * c1 (PR #609, v3.1 C2d family 1) — a NEGATIVE beyond-long literal at the a1 seat. The retired
     * {@code coerceBigIntegerLiteralIteArm} text scan declined a {@code MapperS.of(new BigInteger("-…"))}
     * arm (its digit test admitted {@code 0-9} only); the typed predicate declines it on the node's
     * sign ({@code signum() <= 0}) — the SAME verdict, one for one. MEASURED (the probe run of this
     * fixture, PR #609): a negative beyond-long arm never reaches the #300 arm as a bare wrap at
     * all — the literal seat's own predicate is non-null here and its hoist (seat 24, law F5) takes
     * it, exactly as it takes the positive twin in a1 — so the sign conjunct's reachable population
     * is EMPTY and this witness pins what a negative literal DOES render (the hoisted decl with the
     * signed digits + the bare value ternary) and that no second coercion is layered on it. A
     * mutant dropping the sign conjunct is therefore not observable by any fixture (disclosed); the
     * conjunct mirrors the retired scan by construction.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void c1_negativeBeyondLongArmTakesTheLiteralSeatHoistAndIsNotCoercedTwice() throws IOException {
        String out = rule("C1NegativeArmRule.java");
        assertContains(out, "final BigInteger bigInteger = new BigInteger(\"-" + BIG + "\");\n\t\t\t\t\t\t\t" + VALUE_BARE);
        assertNotContains(out, "new BigDecimal(new BigInteger(");
        assertNotContains(out, "MapperS.of(new BigInteger(");
        assertEquals(List.of(1, 1, 0, 0), quad(countT5(out)), "(T5dec, T5guardS, T5guardI, T5raw) in c1");
    }

    /** a2 — the effective-else arm in a NESTED then-extract lambda: the hoist lands inside the if-branch. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_nestedLambdaEffectiveElseArmHoists() throws IOException {
        String out = rule("A2ElselessArmRule.java");
        assertContains(out, DECL);
        assertContains(out, VALUE_BARE);
        assertNotContains(out, "new BigInteger(\"" + BIG + "\"))");
        assertEquals(List.of(1, 1, 0, 0), quad(countT5(out)), "(T5dec, T5guardS, T5guardI, T5raw) in a2");
    }

    /** a3 — the `default` seat: two literals in one lambda hoist `bigInteger0`/`bigInteger1` and take the ITEM ternary. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a3_defaultSeatHoistsNumberedAndTakesTheItemTernary() throws IOException {
        String out = rule("A3DefaultSeatRule.java");
        assertContains(out, "final BigInteger bigInteger0 = new BigInteger(\"" + BIG + "\");");
        assertContains(out, "final BigInteger bigInteger1 = new BigInteger(\"" + BIG + "\");");
        assertContains(out, ".getOrDefault((bigInteger0 == null ? null : new BigDecimal(bigInteger0))));");
        assertContains(out, ".getOrDefault((bigInteger1 == null ? null : new BigDecimal(bigInteger1))));");
        assertNotContains(out, ".getOrDefault(new BigInteger(");
        assertEquals(List.of(2, 0, 2, 0), quad(countT5(out)), "(T5dec, T5guardS, T5guardI, T5raw) in a3");
    }

    /** a4 — the then-receiver evaluate-argument: the hoist lands ABOVE the then-arg declaration, the argument is the ITEM ternary. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a4_thenReceiverArgumentHoistsAboveTheDecl() throws IOException {
        String out = rule("A4ArgSeatRule.java");
        assertContains(out, DECL + "\n\t\t\tfinal MapperS<BigDecimal> thenArg = MapperS.of(extractAmt.evaluate(input, (bigInteger == null ? null : new BigDecimal(bigInteger))));");
        assertNotContains(out, "evaluate(input, new BigInteger(");
        assertEquals(List.of(1, 0, 1, 0), quad(countT5(out)), "(T5dec, T5guardS, T5guardI, T5raw) in a4");
    }

    /** a5 — LAW 77: the a1 and a4 shapes through the REAL {@code IRGeneration.functionGenerator} seam ({@code -Pir-on}). */
    @Test
    @EnabledIf("builtinsAndIrProviderAvailable")
    void a5_valueAndArgSeatsOnIrRoute() throws IOException {
        String a1 = lookup(fixtureOnIrRoute(), "reports/A1ValueArmRule.java");
        assertContains(a1, DECL + "\n\t\t\t\t\t\t\t" + VALUE_BARE);
        String a4 = lookup(fixtureOnIrRoute(), "reports/A4ArgSeatRule.java");
        assertContains(a4, "(bigInteger == null ? null : new BigDecimal(bigInteger))");
    }

    // =========================================================================
    // Part B — placement pins (GREEN in BOTH states)
    // =========================================================================

    /** b1 — a within-long literal at the same seat keeps seat 22's `BigDecimal.valueOf(5)` — no hoist. */
    @Test
    @EnabledIf("builtinsAvailable")
    void b1_withinLongLiteralKeepsSeat22Form() throws IOException {
        String out = rule("B1WithinLongRule.java");
        assertContains(out, "return MapperS.of(BigDecimal.valueOf(5));");
        assertNotContains(out, "BigInteger");
    }

    /** b2 — the FUNCTION path's sink hoist keeps its operand form: the declaration + the PARENTHESISED value ternary. */
    @Test
    @EnabledIf("builtinsAvailable")
    void b2_functionPathOperandHoistUnchanged() throws IOException {
        String out = lookup(fixture(), "functions/FnHoist.java");
        assertContains(out, DECL);
        assertContains(out, "(bigInteger == null ? MapperS.<BigDecimal>ofNull() : MapperS.of(new BigDecimal(bigInteger)))");
        assertEquals(List.of(1, 1, 0, 0), quad(countT5(out)), "(T5dec, T5guardS, T5guardI, T5raw) in b2");
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
        return Drr7Corpus.gate(Files.isDirectory(DRR7_GOLDEN), BigIntegerLiteralRulePathSeatTest.class);
    }

    private static final String JFSA_NOTIONAL_LEG1 = "drr/regulation/jfsa/rewrite/trade/reports/NotionalLeg1Rule.java";
    private static final String CDE_CALL_AMOUNT = "drr/standards/iosco/cde/reports/CDECallAmountRule.java";
    private static final String ASIC_TNQ_LEG1 = "drr/regulation/asic/rewrite/trade/reports/TotalNotionalQuantityLeg1Rule.java";
    private static final String ASIC_NOTIONAL_AMT_LEG1 = "drr/regulation/asic/rewrite/trade/reports/NotionalAmountLeg1Rule.java";
    private static final String MAS_TNQ_LEG2 = "drr/regulation/mas/rewrite/trade/reports/TotalNotionalQuantityOfLeg2Rule.java";

    /** c1 — jfsa NotionalLeg1Rule (the in-lambda value arm). */
    @Test
    @EnabledIf("drr561Available")
    void corpus_c1_jfsaNotionalLeg1ByteIdentical() throws IOException {
        lock(JFSA_NOTIONAL_LEG1);
    }

    /** c2 — iosco CDECallAmountRule (the then-receiver argument). */
    @Test
    @EnabledIf("drr561Available")
    void corpus_c2_cdeCallAmountByteIdentical() throws IOException {
        lock(CDE_CALL_AMOUNT);
    }

    /** c3 — asic TotalNotionalQuantityLeg1Rule (the `default` seat, singleton name). */
    @Test
    @EnabledIf("drr561Available")
    void corpus_c3_asicTotalNotionalQuantityLeg1ByteIdentical() throws IOException {
        lock(ASIC_TNQ_LEG1);
    }

    /** c4 — asic NotionalAmountLeg1Rule (the effective-else arm in a nested lambda; F4's top-rung hoist already landed). */
    @Test
    @EnabledIf("drr561Available")
    void corpus_c4_asicNotionalAmountLeg1ByteIdentical() throws IOException {
        lock(ASIC_NOTIONAL_AMT_LEG1);
    }

    /** c5 — mas TotalNotionalQuantityOfLeg2Rule (two `default` seats in one lambda: `bigInteger0`/`bigInteger1`). */
    @Test
    @EnabledIf("drr561Available")
    void corpus_c5_masTotalNotionalQuantityOfLeg2ByteIdentical() throws IOException {
        lock(MAS_TNQ_LEG2);
    }

    /**
     * control0 — golden is the oracle (the frozen drr 5.61.0 tree, every kind): T1 the hoist declarations (24), T2 the
     * VALUE ternaries (18), T3 the ITEM ternaries (6), T4 a bare {@code new BigInteger("…")} outside a declaration
     * (ZERO) — over 23 token-bearing files; the carriers' quads pinned.
     */
    @Test
    @EnabledIf("drr561Available")
    void corpus_control0_goldenDrr561IsTheOracle() throws IOException {
        Map<String, int[]> g = scan(readGoldenTree(DRR561_GOLDEN));
        assertEquals(23, g.size(), "golden token-bearing files (the whole-cell control's domain)");
        assertEquals(List.of(24, 18, 6, 0), List.of(sites(g, 0), sites(g, 1), sites(g, 2), sites(g, 3)), "golden (T1, T2, T3, T4)");
        assertEquals(List.of(1, 1, 0, 0), quad(g.get(JFSA_NOTIONAL_LEG1)), "golden c1");
        assertEquals(List.of(1, 0, 1, 0), quad(g.get(CDE_CALL_AMOUNT)), "golden c2");
        assertEquals(List.of(1, 0, 1, 0), quad(g.get(ASIC_TNQ_LEG1)), "golden c3");
        assertEquals(List.of(1, 1, 0, 0), quad(g.get(ASIC_NOTIONAL_AMT_LEG1)), "golden c4");
        assertEquals(List.of(2, 0, 2, 0), quad(g.get(MAS_TNQ_LEG2)), "golden c5");
    }

    /**
     * control1 — the FORK's WHOLE generated drr 5.61.0 cell (every kind, LAW 72): over the UNION of the files either
     * tree carries a token in (a missing side counts as all-zero, LAW 79), the per-file quads agree FILE BY FILE — an
     * over-fire (a hoist where golden has none, a raw literal hoisted at an int seat) and an under-fire (a raw literal
     * left bare) both fail here; the domain equals golden's 23.
     */
    @Test
    @EnabledIf("drr561Available")
    void corpus_control1_forkDrr561WholeCellBigIntegerSitesEqualGoldenFileByFile() throws IOException {
        assertNotNull(drr561Output, "drr 5.61.0 generation did not run");
        assertEquals(List.of(), drr561GenErrors, "drr 5.61.0 reported a generation error — the scan is incomplete");
        assertUnionEqual(scan(drr561Output), scan(readGoldenTree(DRR561_GOLDEN)), drr561Output.keySet(),
                "fork", "golden", KNOWN_RESIDUE_DRR561, 23);
    }

    /** The named pre-existing residue of other families in this cell (LAW 73: the set, not the count) — filled from the GREEN read. */
    private static final List<String> KNOWN_RESIDUE_DRR561 = List.of();

    /**
     * control2 — LAW 77 route parity: the whole drr 5.61.0 cell generated through the REAL {@code IRGeneration} seams
     * ({@code -Pir-on}) carries the SAME per-file quads as the legacy-route render, file by file over the UNION.
     */
    @Test
    @EnabledIf("drr561AndIrProviderAvailable")
    void corpus_control2_irRouteDrr561BigIntegerSitesEqualLegacyRouteFileByFile() throws IOException {
        assertNotNull(drr561Output, "drr 5.61.0 legacy-route generation did not run");
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "5.61.0", DRR561_CELL_ROOT), new ArrayList<>());
        assertUnionEqual(scan(irOut), scan(drr561Output), irOut.keySet(), "ir", "legacy", List.of(),
                scan(drr561Output).size());
    }

    /**
     * control3 — LAW 79: the predicate reaches every cell; the fork's whole drr 7.0.0 cell is compared with its golden
     * (43 / 24 / 19 / 0 over 42 files) the same way — the named residue is that cell's own pre-existing set.
     */
    @Test
    @EnabledIf("drr7Available")
    void corpus_control3_forkDrr7WholeCellBigIntegerSitesEqualGoldenFileByFile() throws IOException {
        List<String> errs = new ArrayList<>();
        Map<String, String> out = generateCell(
                new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", DRR7_CELL_ROOT), errs);
        assertEquals(List.of(), errs, "drr 7.0.0 reported a generation error — the scan is incomplete");
        Map<String, int[]> g = scan(readGoldenTree(DRR7_GOLDEN));
        assertEquals(List.of(43, 24, 19, 0), List.of(sites(g, 0), sites(g, 1), sites(g, 2), sites(g, 3)), "golden drr 7.0.0 (T1, T2, T3, T4)");
        assertUnionEqual(scan(out), g, out.keySet(), "fork", "golden", KNOWN_RESIDUE_DRR7, g.size());
    }

    /** The named pre-existing residue of other families in drr 7.0.0 (filled from the GREEN read). */
    private static final List<String> KNOWN_RESIDUE_DRR7 = List.of();

    private static List<Integer> quad(int[] c) {
        assertNotNull(c, "the file carries no token");
        return List.of(c[0], c[1], c[2], c[3]);
    }

    private static void assertUnionEqual(Map<String, int[]> a, Map<String, int[]> b,
            java.util.Set<String> emittedA, String aName, String bName, List<String> knownResidue,
            int expectedDomain) {
        List<String> mismatched = new ArrayList<>();
        List<String> notEmitted = new ArrayList<>();
        java.util.Set<String> universe = new java.util.TreeSet<>(a.keySet());
        universe.addAll(b.keySet());
        int[] zero = new int[4];
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
                "(T1, T2, T3, T4) differ beyond the named residue in " + mismatched.size() + " file(s)");
        assertEquals(List.of(), notEmitted,
                "token-bearing files " + bName + " carries that " + aName + " does not emit at all");
        assertEquals(expectedDomain, universe.size(),
                "the union domain must equal the oracle's token-bearing files (" + expectedDomain + ")");
    }

    // =========================================================================
    // The scan — (T1, T2, T3, T4) per file over CODE only (comments stripped; a line walk)
    // =========================================================================

    static int[] countT5(String java) {
        int[] c = new int[4];
        for (String raw : codeOnly(java).split("\n")) {
            String s = raw.strip();
            boolean decl = s.startsWith("final BigInteger bigInteger") && s.contains(" = new BigInteger(\"");
            if (decl) {
                c[0]++;
            }
            if (s.contains("== null ? MapperS.<BigDecimal>ofNull() : MapperS.of(new BigDecimal(")) {
                c[1]++;
            }
            if (s.contains(" == null ? null : new BigDecimal(bigInteger")) {
                c[2]++;
            }
            if (!decl && s.contains("new BigInteger(\"")) {
                c[3]++;
            }
        }
        return c;
    }

    private static Map<String, int[]> scan(Map<String, String> tree) {
        Map<String, int[]> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : tree.entrySet()) {
            if (!e.getValue().contains("BigInteger")) {
                continue;
            }
            int[] c = countT5(e.getValue());
            if (c[0] + c[1] + c[2] + c[3] > 0) {
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
                + path + " — seat 24 F5: the beyond-long literal hoists on the rule path.");
    }

    // =========================================================================
    // Fixture harness
    // =========================================================================

    private static RLinkingResult linking;
    private static RModel mainModel;
    private static Map<String, String> fixtureOut;

    private static void link() throws IOException {
        if (linking == null) {
            RModel main = AstBuilder.buildFromString(MODEL, "seat24f5.rosetta");
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
            fixtureOut = render(m -> "census.seat24f5".equals(m.namespace()));
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
                        m -> "census.seat24f5".equals(m.namespace()));
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
            throw new AssertionError("[BigIntegerLiteralRulePathSeatTest] builtins parse"
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
