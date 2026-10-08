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
 * SEAT 22, law F3 — facet {@code intLiteralNumberSeat}: <b>an int literal compiles against its CONSUMER's
 * expected type — upstream threads the context's expected type UNCHANGED through every conditional arm
 * ({@code ExpressionGenerator.caseConditionalExpression}: {@code expr.ifthen.javaCode(context)} /
 * {@code expr.elsethen.javaCode(context)}), so at a {@code number}-typed conditional seat the literal
 * coerces to {@code MapperS.of(BigDecimal.valueOf(0))} ({@code TypeCoercionService}, the int → BigDecimal
 * item conversion) — the hoisted {@code final MapperS<BigDecimal> ifThenElseResult; … = MapperS.of(
 * BigDecimal.valueOf(0));} form (golden asic {@code OptionPremiumAmountRule}) and the block-lambda
 * {@code return MapperS.of(BigDecimal.valueOf(0));} form (golden esma {@code SpreadOfLeg2BasisRule}).</b>
 *
 * <p><b>The defect.</b> The fork compiles every conditional ARM with a {@code null} expected type BY DESIGN
 * (the ladder's item type is read from the workspace snapshot AFTER the arms — {@code ControlFlowHandler}
 * and {@code CollectionHandler}'s block-lambda tree), and {@code LiteralHandler.isBigDecimalContext} reads
 * ONLY {@code ctx.expectedType()} — so a {@code then 0} under a {@code number} ladder rendered the bare
 * {@code MapperS.of(0)} beside a {@code MapperS<BigDecimal>} local / a {@code MapperS.<BigDecimal>ofNull()}
 * return: 7 whole-file drr 5.61.0 carriers ({@code OptionPremiumAmountRule} asic/mas,
 * {@code SpreadOfLeg1BasisRule} esma/fca, {@code SpreadOfLeg2BasisRule} esma/fca,
 * {@code SpreadOfLeg2PercentageRule} fca), every one NON-COMPILING ({@code MapperS<Integer>} into a
 * {@code MapperS<BigDecimal>} local / mixed lambda returns — LAW 74, 7 repairs).
 *
 * <p><b>The seat (LAW 69 — the arm agrees with the decl it is assigned into; ONE predicate, both routes).</b>
 * {@code HandlerHelper.intLiteralConditionalArmExpectedType}: an int literal that IS a conditional arm
 * (walked up through then/else arms to the OUTERMOST conditional — upstream threads the context unchanged
 * through every arm, so the ladder ROOT is the literal's effective consumer), within-long (the beyond-long
 * population is the F5 BigInteger-hoist family's, untouched), whose ladder root's workspace-inferred type
 * translates to {@code BigDecimal} — the SAME inference the hoisted local and the typed-empty
 * {@code Mapper*.<T>ofNull()} terminal are declared from. Consulted by {@code LiteralHandler.isBigDecimalContext}'s
 * null-expected rung (the legacy literal seat — the seat-22 stack probe pinned the carriers' arm compiles to
 * {@code CollectionHandler.compileLadderArmWithDeepThenDrain} (the block-lambda ladder) and
 * {@code FunctionExpressionRenderer.appendIteHoistChainCore} (the {@code ifThenElseResult} hoist), BOTH
 * funnelling into the literal handler with a null expected) and by
 * {@code IRExpressionCompiler.visitIntLiteral} (the IR claim ROOT threads the returned type as the context's
 * expected; {@code tryEmitFromIR} itself never reads it — only the leaf {@code IRJavaLeafEmitter.emitInt}
 * does — LAW 77: the same predicate at every IR claim seat).
 *
 * <p><b>LAW 75 — measured before the seat over all 275 matrix rows (the seat-22 runtime probe):</b> of 3,771
 * {@code RIntLiteral} compiles at {@code expected=null}, the conditional-arm subset whose OUTERMOST enclosing
 * conditional infers {@code number} is exactly the 7 carriers' literals ({@code value=0}; the in-band
 * {@code 9999999999999999999999999} literals belong to the F5 BigInteger-hoist family and stay untouched via
 * the within-long gate); every GREEN conditional-arm literal reads {@code int}/MISSING — the rung moves
 * nothing green. The whole-cell control below is the receipt, not the probe.
 *
 * <p><b>The seat also fires in ONE file outside its charter cell, and that is LOCKED, not narrated</b>
 * (LAW 79 — a control scans the domain the MECHANISM reaches): drr 7.0–7.3 {@code AdjustFrequencyPeriod}
 * hoists its conditional into a BARE {@code BigDecimal} local (the fork's pre-existing hoisted-local
 * divergence, close-census family F8), so the arm coerces there too — golden, whose local is
 * {@code Integer}, writes {@code = 1;}. The pre-seat fork text matched golden's by coincidence and did
 * NOT compile in the fork's own block (LAW 74, {@code Seat22Pre.preF3_bareLocalArm}); the file's diff
 * grows 6 → 8 lines in each of the four cells and closes when F8 lands. {@code corpus_c4} locks both
 * halves and {@code corpus_control3} scans the WHOLE drr 7.0.0 cell on a wider token
 * ({@code BigDecimal.valueOf(<int>)}, which sees the bare-local arm that {@code control1}'s (T1, T2)
 * tokens cannot), pinning golden's domain (194 files / 258 sites) and permitting exactly that one
 * per-file difference.
 *
 * <p><b>RED at the pre-seat blob</b> ({@code rune-java-generator/src/main} + {@code rune-ir-java/src/main}
 * at {@code e2807ac1} — this suite kept): the a-tests, {@code corpus_c1}/{@code c2}/{@code c3}/
 * {@code c4}/{@code control1}/{@code control3} (and under {@code -Pir-on} also {@code a5}/
 * {@code control2}); every {@code b*} + {@code control0} GREEN in both states. <b>LAW 66/76 mutations</b> (each applied → run → reverted at the
 * FINAL head, LAW 78; receipts in the PR body §7): (i) the predicate deleted (returns null) → every a + c +
 * {@code control1}; (ii) the {@code BigDecimal} gate dropped (every resolving inferred type admits) →
 * {@code b1} + {@code control1} (the cell's {@code int} seats move); (iii) the walk stops at the NEAREST
 * conditional instead of the outermost (isolating: the outermost walk is load-bearing) → {@code a2}/
 * {@code c2} + {@code control1} move, {@code a1}/{@code c1} stay; (iv) the IR-route threading deleted
 * ({@code visitIntLiteral} reverted; isolating: the IR claim root) → {@code -Pir-on} {@code a5} +
 * {@code control2} move, the default suite GREEN; (v) the #398 alias-return text wrap's new decline
 * reverted (the two halves compound again) → {@code b4}; (vi) the #398 IN-LAMBDA text wrap's new decline
 * reverted → {@code b5} (both are the double wrap; declared fixture pins — the whole-cell control's
 * domain is the POJO/rule cell, not FUNCTION).
 */
class IntLiteralNumberSeatTest {

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
     * One {@code from} type, the rules the seats consume, and one reporting rule per shape the seat must tell
     * apart: the asic {@code OptionPremiumAmount} shape (a then-level conditional over a filter — the hoisted
     * {@code ifThenElseResult} ladder), the esma {@code SpreadOfLeg2Basis} shape (an {@code extract} whose
     * body is a nested-then conditional with an else-if rung — the block lambda), a then-nested int arm, a
     * beyond-int literal, and the int-typed / explicit-expected / number-literal negatives.
     */
    private static final String MODEL = """
            namespace census.seat22f3
            version "1.0.0"

            enum NotationEnum:
                BASIS
                PERCENT

            type Sched:
                value number (0..1)

            type Probe:
                amount number (0..1)
                contractType string (0..1)
                sched Sched (0..1)
                name string (0..1)
                n int (0..1)

            reporting rule Allow from Probe: <"the filter">
                extract contractType exists

            reporting rule ContractType from Probe: <"a string rule">
                extract contractType

            reporting rule PremiumAmount from Probe: <"a number rule (CDEOptionPremiumAmount)">
                extract amount

            reporting rule IntRule from Probe: <"an int rule">
                extract n

            reporting rule SpreadLeg2 from Probe: <"a Sched rule (CDESpreadLeg2)">
                extract sched

            reporting rule NameOfFloat from Probe: <"a string rule (GetNameOfTheFloatingRateOfLeg2)">
                extract name

            func GetNotation: <"GetPriceNotation twin">
                inputs:
                    s Sched (1..1)
                output:
                    result NotationEnum (1..1)
                set result:
                    NotationEnum -> BASIS

            func Multiply: <"MultiplyPrice twin">
                inputs:
                    v number (1..1)
                    n NotationEnum (1..1)
                output:
                    result number (1..1)
                set result:
                    v * 100

            func Fmt: <"FormatToMax5Number twin">
                inputs:
                    n number (0..1)
                output:
                    result number (0..1)
                set result:
                    n

            reporting rule A1OptionPremiumShape from Probe: <"a1 - THE asic OptionPremiumAmount SHAPE: a then-level conditional over a filter; the int literal arm at a number ladder">
                filter Allow
                then if ContractType = "OPTN" and PremiumAmount is absent
                    then 0
                    else PremiumAmount
                    as "a1"

            reporting rule A2SpreadShape from Probe: <"a2 - THE esma SpreadOfLeg2Basis SHAPE: an extract whose body is a nested-then conditional with an else-if rung ending in 0 (the block lambda return)">
                filter Allow
                then extract
                    if SpreadLeg2 exists
                    then (if GetNotation(SpreadLeg2) = NotationEnum -> BASIS
                        then Multiply(SpreadLeg2 -> value, GetNotation(SpreadLeg2)))
                    else if NameOfFloat exists
                    then 0
                then Fmt
                    as "a2"

            reporting rule A3ThenNestedArm from Probe: <"a3 - a then-NESTED conditional's int arm under a number ladder">
                filter Allow
                then if ContractType = "X"
                    then (if PremiumAmount exists then PremiumAmount else 0)
                    else 1.5
                    as "a3"

            reporting rule A4BeyondIntLiteral from Probe: <"a4 - a beyond-int (long) literal at the same seat keeps the lowercase-l law">
                filter Allow
                then if ContractType = "OPTN" and PremiumAmount is absent
                    then 99999999999
                    else PremiumAmount
                    as "a4"

            reporting rule B1IntLadderStaysBare from Probe: <"b1 - an int-typed ladder keeps the bare literal">
                filter Allow
                then if ContractType = "X"
                    then 0
                    else IntRule
                    as "b1"

            reporting rule B2ExplicitExpected from Probe: <"b2 - the direct rule-body conditional already compiles at the rule's expected BigDecimal (byte-lock)">
                if ContractType = "X"
                    then 0
                    else PremiumAmount
                    as "b2"

            func AliasElseOne: <"b4 - THE Notional/NotionalLeg SHAPE: a function ALIAS whose ladder else-terminal is an int literal at a MapperS<BigDecimal> alias signature - the #398 TEXT wrap and this seat's literal coercion must not compound">
                inputs:
                    p Probe (1..1)
                output:
                    result number (0..1)
                alias multiplier:
                    if p -> amount exists
                    then p -> amount
                    else 1
                set result:
                    multiplier

            func AliasThenExtractElseOne: <"b5 - THE Notional interestRateMultiplier SHAPE: an alias whose body is a then-extract lambda whose conditional else-terminal is an int literal - the CollectionHandler half of the #398 text wrap">
                inputs:
                    p Probe (1..1)
                output:
                    result number (0..1)
                alias multiplier:
                    p -> sched
                        then extract
                            if value exists
                            then value
                            else 1
                set result:
                    multiplier

            reporting rule B3NumberLiteral from Probe: <"b3 - a number literal arm is unchanged">
                filter Allow
                then if ContractType = "X"
                    then 0.0
                    else PremiumAmount
                    as "b3"
            """;

    // =========================================================================
    // Part A — the seat (RED at the pre-seat blob)
    // =========================================================================

    /** a1 — the asic shape: the int arm of a number ladder renders BigDecimal.valueOf(0) beside the MapperS<BigDecimal> local. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_optionPremiumShapeIntArmAtNumberLadder() throws IOException {
        String out = rule("A1OptionPremiumShapeRule.java");
        assertContains(out, "final MapperS<BigDecimal> ifThenElseResult;");
        assertContains(out, "ifThenElseResult = MapperS.of(BigDecimal.valueOf(0));");
        assertNotContains(out, "MapperS.of(0)");
    }

    /** a2 — the esma shape: the block lambda's else-if rung returns MapperS.of(BigDecimal.valueOf(0)) beside the typed empty. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_spreadShapeBlockLambdaReturn() throws IOException {
        String out = rule("A2SpreadShapeRule.java");
        assertContains(out, "return MapperS.of(BigDecimal.valueOf(0));");
        assertContains(out, "return MapperS.<BigDecimal>ofNull();");
        assertNotContains(out, "MapperS.of(0)");
    }

    /** a3 — a then-NESTED conditional's int arm under a number ladder takes the ladder's BigDecimal. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a3_thenNestedIntArm() throws IOException {
        String out = rule("A3ThenNestedArmRule.java");
        assertContains(out, "MapperS.of(BigDecimal.valueOf(0))");
        assertNotContains(out, "MapperS.of(0)");
    }

    /** a4 — a beyond-int literal at the same seat: BigDecimal.valueOf(99999999999l) (the lowercase-l law). */
    @Test
    @EnabledIf("builtinsAvailable")
    void a4_beyondIntLiteralKeepsLowercaseL() throws IOException {
        String out = rule("A4BeyondIntLiteralRule.java");
        assertContains(out, "MapperS.of(BigDecimal.valueOf(99999999999l))");
        assertNotContains(out, "MapperS.of(99999999999L)");
    }

    /**
     * a5 — LAW 77: the a1 shape rendered through the REAL {@code IRGeneration.functionGenerator} seam
     * ({@code -Pir-on}): the IR claim root ({@code IRExpressionCompiler.visitIntLiteral}) threads the same
     * predicate, so the IR leaf renders the same {@code BigDecimal.valueOf} form.
     */
    @Test
    @EnabledIf("builtinsAndIrProviderAvailable")
    void a5_optionPremiumShapeOnIrRoute() throws IOException {
        String out = lookup(fixtureOnIrRoute(), "reports/A1OptionPremiumShapeRule.java");
        assertContains(out, "ifThenElseResult = MapperS.of(BigDecimal.valueOf(0));");
        assertNotContains(out, "MapperS.of(0)");
    }

    // =========================================================================
    // Part B — placement pins (GREEN in BOTH states)
    // =========================================================================

    /** b1 — an int-typed ladder keeps the bare literal beside its MapperS<Integer> local. */
    @Test
    @EnabledIf("builtinsAvailable")
    void b1_intLadderStaysBare() throws IOException {
        String out = rule("B1IntLadderStaysBareRule.java");
        assertContains(out, "final MapperS<Integer> ifThenElseResult;");
        assertContains(out, "ifThenElseResult = MapperS.of(0);");
        assertNotContains(out, "BigDecimal.valueOf(0)");
    }

    /**
     * b2 — the direct rule-body conditional (an explicit expected type from the rule's output) already
     * renders the seat-coerced literal — here the unwrapped direct-assign form — byte-lock.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b2_explicitExpectedUnchanged() throws IOException {
        String out = rule("B2ExplicitExpectedRule.java");
        assertContains(out, "output = BigDecimal.valueOf(0);");
        assertNotContains(out, "MapperS.of(0)");
    }

    /** b3 — a number literal arm renders new BigDecimal("0.0") — unchanged. */
    @Test
    @EnabledIf("builtinsAvailable")
    void b3_numberLiteralUnchanged() throws IOException {
        String out = rule("B3NumberLiteralRule.java");
        assertContains(out, "MapperS.of(new BigDecimal(\"0.0\"))");
        assertNotContains(out, "BigDecimal.valueOf(");
    }

    /**
     * b4 — LAW 69, the two halves that must agree: the function-ALIAS else-terminal (the drr
     * {@code Notional}/{@code NotionalLeg} {@code optionMultiplier} shape) is ALSO reached by the #398
     * TEXT wrap ({@code FunctionExpressionRenderer.wrapIntLiteralRungAsBigDecimalValueOf}, which rewrites
     * the rendered string on an AST-kind + signature-string guard). Since this seat's literal rung
     * coerces at the literal itself, that arm consults the SAME predicate and declines — exactly ONE
     * wrap, never {@code MapperS.of(BigDecimal.valueOf(BigDecimal.valueOf(1)))}. The whole-cell control
     * cannot see this (the alias lives in a FUNCTION, whose cells the control does not scan), so this is
     * a declared fixture pin.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b4_functionAliasElseTerminalWrapsExactlyOnce() throws IOException {
        String out = function("AliasElseOne.java");
        assertContains(out, "return MapperS.of(BigDecimal.valueOf(1));");
        assertNotContains(out, "BigDecimal.valueOf(BigDecimal.valueOf(");
    }

    /**
     * b5 — the SIBLING half of b4's law: the in-lambda arm seat
     * ({@code CollectionHandler.coerceIntLiteralMapperSOrNull}, the same #398 facet at the block-lambda
     * conditional) also consults the seat's predicate and declines — the drr {@code Notional}/
     * {@code NotionalLeg} {@code interestRateMultiplier}/{@code performanceMultiplier} shape. Both text
     * arms of that facet must decline, or the FUNCTION cells regress (they did on the seat's FIRST ring:
     * 10 files entered).
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b5_aliasThenExtractElseTerminalWrapsExactlyOnce() throws IOException {
        String out = function("AliasThenExtractElseOne.java");
        assertContains(out, "return MapperS.of(BigDecimal.valueOf(1));");
        assertNotContains(out, "BigDecimal.valueOf(BigDecimal.valueOf(");
    }

    // =========================================================================
    // Part C — the corpus (drr 5.61.0: the carriers + the whole-cell control)
    // =========================================================================

    private static final Path DRR561_CELL_ROOT = Path.of("../test-corpus/drr/drr-5.61.0");
    private static final Path DRR561_GOLDEN = DRR561_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    static boolean drr561Available() {
        return Files.isDirectory(DRR561_GOLDEN);
    }

    private static final List<String> DRR561_CARRIERS = List.of(
            "drr/regulation/asic/rewrite/trade/reports/OptionPremiumAmountRule.java",
            "drr/regulation/mas/rewrite/trade/reports/OptionPremiumAmountRule.java",
            "drr/regulation/esma/emir/refit/trade/reports/SpreadOfLeg1BasisRule.java",
            "drr/regulation/fca/ukemir/refit/trade/reports/SpreadOfLeg1BasisRule.java",
            "drr/regulation/esma/emir/refit/trade/reports/SpreadOfLeg2BasisRule.java",
            "drr/regulation/fca/ukemir/refit/trade/reports/SpreadOfLeg2BasisRule.java",
            "drr/regulation/fca/ukemir/refit/trade/reports/SpreadOfLeg2PercentageRule.java");

    /** c1 — asic OptionPremiumAmountRule whole-file lock (the ladder shape). */
    @Test
    @EnabledIf("drr561Available")
    void corpus_c1_asicOptionPremiumAmountRuleByteIdentical() throws IOException {
        lock(DRR561_CARRIERS.get(0));
    }

    /** c2 — esma SpreadOfLeg2BasisRule whole-file lock (the block-lambda shape). */
    @Test
    @EnabledIf("drr561Available")
    void corpus_c2_esmaSpreadOfLeg2BasisRuleByteIdentical() throws IOException {
        lock(DRR561_CARRIERS.get(4));
    }

    /** c3 — ALL seven carriers byte-identical. */
    @Test
    @EnabledIf("drr561Available")
    void corpus_c3_allSevenCarriersByteIdentical() throws IOException {
        for (String path : DRR561_CARRIERS) {
            lock(path);
        }
    }

    /**
     * control0 — golden is the oracle (the frozen drr 5.61.0 tree, every kind): the two token populations at
     * their exact census counts — T1 {@code MapperS.of(BigDecimal.valueOf(} 246 sites and T2 a BARE
     * int-literal {@code MapperS.of(N)} 150 sites over 294 token-bearing files; every carrier carries T1 and
     * no carrier carries T2.
     */
    @Test
    @EnabledIf("drr561Available")
    void corpus_control0_goldenDrr561IsTheOracle() throws IOException {
        LiteralScan g = scan(readGoldenTree(DRR561_GOLDEN));
        assertEquals(294, g.perFile.size(), "golden token-bearing files (the whole-cell control's domain)");
        assertEquals(246, g.sites(0), "golden T1 MapperS.of(BigDecimal.valueOf( sites");
        assertEquals(150, g.sites(1), "golden T2 bare int-literal MapperS.of(N) sites");
        for (String carrier : DRR561_CARRIERS) {
            int[] c = g.perFile.get(carrier);
            assertNotNull(c, "golden carrier carries a token: " + carrier);
            assertTrue(c[0] >= 1, "golden carrier carries T1: " + carrier);
            assertEquals(0, c[1], "golden carrier carries no bare int MapperS.of: " + carrier);
        }
    }

    /**
     * The named PRE-EXISTING residue of OTHER families in this cell — the only per-file (T1, T2) differences
     * between the fork's whole drr 5.61.0 cell and golden AFTER the seat, pinned EXACTLY (LAW 73: the set,
     * not the count). EMPTY since seat 24 (law F5, facet {@code bigIntegerLiteralRulePath}): the eight
     * {@code NotionalAmount*} files this list named — the fork rendered one more {@code MapperS.of(BigDecimal.valueOf(}
     * than golden at the {@code 9999…} seat, the F5 BigInteger-hoist family's — healed with that law and were
     * re-pinned here in its seat. Any entry that LEAVES this list is re-pinned in the same commit that heals it.
     * A carrier may NEVER appear here.
     */
    private static final List<String> KNOWN_RESIDUE = List.of();

    /**
     * control1 — the FORK's WHOLE generated drr 5.61.0 cell (every kind, LAW 72): over the UNION of the files
     * either tree carries a token in (a missing side counts as all-zero, LAW 79), the per-file (T1, T2) pairs
     * agree FILE BY FILE except the named pre-existing residue above — an over-fire (a green bare int
     * {@code MapperS.of(N)} turned {@code BigDecimal.valueOf}) and an under-fire (a carrier still bare) both
     * fail here; the domain equals golden's 294; every carrier is reached (carries T1, no T2).
     */
    @Test
    @EnabledIf("drr561Available")
    void corpus_control1_forkDrr561WholeCellLiteralCountsEqualGoldenFileByFile() throws IOException {
        assertNotNull(drr561Output, "drr 5.61.0 generation did not run");
        LiteralScan f = scan(drr561Output);
        LiteralScan g = scan(readGoldenTree(DRR561_GOLDEN));
        List<String> mismatched = new ArrayList<>();
        java.util.Set<String> universe = new java.util.TreeSet<>(f.perFile.keySet());
        universe.addAll(g.perFile.keySet());
        int[] zero = new int[2];
        for (String key : universe) {
            int[] fc = f.perFile.getOrDefault(key, zero);
            int[] gc = g.perFile.getOrDefault(key, zero);
            if (!java.util.Arrays.equals(gc, fc)) {
                mismatched.add(key + " fork=" + java.util.Arrays.toString(fc)
                        + " golden=" + java.util.Arrays.toString(gc));
            }
        }
        assertEquals(KNOWN_RESIDUE, mismatched,
                "(T1, T2) counts differ from golden beyond the named residue — " + mismatched.size() + " file(s)");
        assertEquals(294, universe.size(), "the union domain must equal golden's 294 token-bearing files");
        for (String carrier : DRR561_CARRIERS) {
            int[] c = f.perFile.get(carrier);
            assertNotNull(c, "carrier REACHED (carries T1): " + carrier);
            assertEquals(0, c[1], "carrier carries no bare int MapperS.of: " + carrier);
        }
    }

    /**
     * control2 — LAW 77 route parity: the whole drr 5.61.0 cell generated through the REAL
     * {@code IRGeneration} seams ({@code -Pir-on}) carries the SAME per-file (T1, T2) counts as the
     * legacy-route render, file by file over the UNION — the only control that can move under an
     * IR-route mutation (the seat-21 lesson: control1 is constructed on LEGACY generators and can never
     * see one).
     */
    @Test
    @EnabledIf("drr561AndIrProviderAvailable")
    void corpus_control2_irRouteDrr561LiteralCountsEqualLegacyRouteFileByFile() throws IOException {
        assertNotNull(drr561Output, "drr 5.61.0 legacy-route generation did not run");
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "5.61.0", DRR561_CELL_ROOT), new ArrayList<>());
        LiteralScan on = scan(irOut);
        LiteralScan off = scan(drr561Output);
        List<String> mismatched = new ArrayList<>();
        java.util.Set<String> universe = new java.util.TreeSet<>(on.perFile.keySet());
        universe.addAll(off.perFile.keySet());
        int[] zero = new int[2];
        for (String key : universe) {
            int[] a = on.perFile.getOrDefault(key, zero);
            int[] b = off.perFile.getOrDefault(key, zero);
            if (!java.util.Arrays.equals(a, b)) {
                mismatched.add(key + " ir=" + java.util.Arrays.toString(a)
                        + " legacy=" + java.util.Arrays.toString(b));
            }
        }
        assertEquals(List.of(), mismatched,
                "(T1, T2) counts differ between the routes in " + mismatched.size() + " file(s)");
    }

    // =========================================================================
    // Part C2 — drr 7.0.0: where the seat reached BEYOND its charter cell (LAW 79)
    //
    // The charter sized this law in drr 5.61.0, and control1 above scans exactly that cell. The
    // predicate, however, is corpus-wide: the close-out census over the POST-seat band found it firing
    // in ONE file outside that cell — drr 7.0-7.3 AdjustFrequencyPeriod — where the arm sits in a BARE
    // BigDecimal local rather than a MapperS, a shape control1's (T1, T2) tokens cannot see at all
    // (measured: the drr 7.x band carries 0 (T1, T2) differences before AND after the seat). The
    // control below therefore scans a WIDER token — every BigDecimal.valueOf(<int literal>) site — over
    // the whole drr 7.0.0 cell, and c4 locks the one disclosed divergence by name.
    // =========================================================================

    private static final Path DRR7_CELL_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path DRR7_GOLDEN = DRR7_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    static boolean drr7Available() {
        return Drr7Corpus.gate(Files.isDirectory(DRR7_GOLDEN), IntLiteralNumberSeatTest.class);
    }

    private static final String ADJUST_FREQUENCY_PERIOD =
            "drr/regulation/common/functions/AdjustFrequencyPeriod.java";

    /**
     * c4 — THE DISCLOSED DIVERGENCE, LOCKED (the seat's ONE adverse sub-file movement, 4 pairs).
     *
     * <p>{@code AdjustFrequencyPeriod} hoists a conditional into a BARE local. Golden declares it
     * {@code Integer} (its consumer is {@code setPeriodMultiplier(Integer)}) and writes the arm
     * {@code ifThenElseResult1 = 1;}. The fork declares the SAME local {@code BigDecimal}, from the
     * ladder's {@code number} inference — a PRE-EXISTING divergence of the hoisted-local family (close-
     * census F8, seat S27) that this seat does not touch and whose decl and else lines were already in
     * the band. Inside the fork's own {@code BigDecimal} block the pre-seat arm text {@code = 1;} did not
     * compile while coincidentally matching golden's text; since this seat the arm coerces, which
     * compiles and differs — the file's diff grows 6 → 8 lines in each of the four drr 7.x cells.
     *
     * <p>LAW 74 settles which of the two is right for the fork's own block, and the javac probe carries
     * the case ({@code Seat22Pre.preF3_bareLocalArm}: {@code error: incompatible types: int cannot be
     * converted to BigDecimal}; {@code Seat22Post.postF3_bareLocalArm}: exit 0). This test LOCKS both
     * halves so the disclosure cannot rot: when F8 lands and re-declares the local at its consumer type,
     * THIS TEST MUST FAIL — and the fix is for the predicate to read that same consumer type, not to
     * drop the coercion.
     */
    @Test
    @EnabledIf("drr7Available")
    void corpus_c4_adjustFrequencyPeriodArmFollowsTheConsumerTypedLocal() throws IOException {
        assertNotNull(drr7Output, "drr 7.0.0 generation did not run");
        String fork = drr7Output.get(ADJUST_FREQUENCY_PERIOD);
        assertNotNull(fork, "not generated in drr 7.0.0: " + ADJUST_FREQUENCY_PERIOD);
        String golden = Files.readString(DRR7_GOLDEN.resolve(ADJUST_FREQUENCY_PERIOD));
        assertTrue(golden.contains("final Integer ifThenElseResult1;"),
                "golden declares the hoisted local Integer (its consumer's type)");
        assertTrue(golden.contains("ifThenElseResult1 = 1;"),
                "golden's arm is the bare int literal, at an Integer local");
        // RE-PINNED at seat 32, law B.2 (ctorCondSingleCoerceLiteralArm): the hoisted local is now
        // declared at its CONSUMER type (Integer, the ctor-pair attribute) and the literal arm
        // follows it - bare `= 1;`, exactly golden (B2-trip1.log). The disclosed adverse movement
        // this test locked from seat 22 is CLOSED; the coercion followed the consumer type, as this
        // javadoc required, rather than being dropped.
        assertTrue(fork.contains("final Integer ifThenElseResult1;"),
                "seat 32 law B.2: the fork declares the hoisted local at its consumer type (Integer)");
        assertTrue(fork.contains("ifThenElseResult1 = 1;"),
                "seat 32 law B.2: the arm is the bare int literal at the Integer local - golden's form");
        assertTrue(!fork.contains("ifThenElseResult1 = BigDecimal.valueOf(1);"),
                "the BigDecimal coercion belonged to the fork's OLD walk-typed local, which is gone");
    }

    /**
     * The whole drr 7.0.0 cell's ONLY per-file difference in {@code BigDecimal.valueOf(<int>)} sites,
     * pinned EXACTLY (LAW 73: the set, not a count) — the disclosed one, and nothing else. An over-fire
     * anywhere else in this cell fails here.
     */
    // the AdjustFrequencyPeriod row (`fork=3 golden=2`) LEFT this list at seat 32: law B.2
    // (ctorCondSingleCoerceLiteralArm) healed the file WHOLE - the third BigDecimal.valueOf(<int>)
    // site was the coerced literal at the fork's walk-typed local; transcribed from control3's own
    // print (B2-trip1.log: expected the row, actual EMPTY).
    private static final List<String> KNOWN_DRR7_RESIDUE = List.of();

    /**
     * control3 (LAW 79 — the control scans the domain the MECHANISM reaches, not the domain the charter
     * happened to size): the fork's whole generated drr 7.0.0 cell vs golden, per file, over the UNION of
     * the files either side carries a {@code BigDecimal.valueOf(<int literal>)} site — the token that can
     * see BOTH the {@code MapperS}-wrapped arm and the bare-local arm. Golden's domain is pinned (194
     * carrier files / 258 sites, censused by
     * {@code target/seat22-instruments/f3-drr7-golden-census.py}); the only permitted per-file difference
     * is the disclosed {@code AdjustFrequencyPeriod} one. Files golden carries that the fork does not emit
     * at all (this cell's refusals) are named, not silently skipped.
     */
    @Test
    @EnabledIf("drr7Available")
    void corpus_control3_forkDrr7WholeCellIntCoercionSitesEqualGoldenExceptTheDisclosedOne()
            throws IOException {
        assertNotNull(drr7Output, "drr 7.0.0 generation did not run");
        Map<String, Integer> f = scanValueOfSites(drr7Output);
        Map<String, Integer> g = scanValueOfSites(readGoldenTree(DRR7_GOLDEN));
        assertEquals(194, g.size(), "golden drr 7.0.0 files carrying a BigDecimal.valueOf(<int>) site");
        assertEquals(258, g.values().stream().mapToInt(Integer::intValue).sum(),
                "golden drr 7.0.0 BigDecimal.valueOf(<int>) sites");
        List<String> notEmitted = new ArrayList<>();
        List<String> mismatched = new ArrayList<>();
        java.util.Set<String> universe = new java.util.TreeSet<>(f.keySet());
        universe.addAll(g.keySet());
        for (String key : universe) {
            if (!drr7Output.containsKey(key)) {
                notEmitted.add(key);          // a refusal in this cell — named below, never silent
                continue;
            }
            int fc = f.getOrDefault(key, 0);
            int gc = g.getOrDefault(key, 0);
            if (fc != gc) {
                mismatched.add(key + " fork=" + fc + " golden=" + gc);
            }
        }
        assertEquals(KNOWN_DRR7_RESIDUE, mismatched,
                "BigDecimal.valueOf(<int>) sites differ from golden beyond the disclosed file — "
                + mismatched.size() + " file(s)");
        assertEquals(NOT_EMITTED_DRR7, notEmitted,
                "golden carriers this cell does not emit (the cell's refusals) must be exactly the "
                + "named set");
    }

    /** The golden {@code BigDecimal.valueOf(<int>)} carriers this cell's refusals leave unemitted. */
    private static final List<String> NOT_EMITTED_DRR7 = List.of();

    private static Map<String, Integer> scanValueOfSites(Map<String, String> tree) {
        Map<String, Integer> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : tree.entrySet()) {
            int n = valueOfIntSites(codeOnly(e.getValue()));
            if (n > 0) {
                out.put(e.getKey(), n);
            }
        }
        return out;
    }

    /** {@code BigDecimal.valueOf(} + optional '-' + digits + ')' — an int-literal coercion site. */
    private static int valueOfIntSites(String code) {
        final String head = "BigDecimal.valueOf(";
        int sites = 0, i = code.indexOf(head);
        while (i >= 0) {
            int k = i + head.length();
            if (k < code.length() && code.charAt(k) == '-') k++;
            int d0 = k;
            while (k < code.length() && Character.isDigit(code.charAt(k))) k++;
            if (k > d0 && k < code.length() && code.charAt(k) == ')') sites++;
            i = code.indexOf(head, i + head.length());
        }
        return sites;
    }

    // =========================================================================
    // The scan — (T1, T2) per file over CODE only (comments stripped)
    // =========================================================================

    private static final String T1 = "MapperS.of(BigDecimal.valueOf(";
    private static final String MAPPER_S_OF = "MapperS.of(";

    private static final class LiteralScan {
        final Map<String, int[]> perFile = new LinkedHashMap<>();

        int sites(int idx) {
            int n = 0;
            for (int[] c : perFile.values()) n += c[idx];
            return n;
        }
    }

    private static LiteralScan scan(Map<String, String> tree) {
        LiteralScan r = new LiteralScan();
        for (Map.Entry<String, String> e : tree.entrySet()) {
            String code = codeOnly(e.getValue());
            int t1 = countOccurrences(code, T1);
            int t2 = bareIntSites(code);
            if (t1 > 0 || t2 > 0) {
                r.perFile.put(e.getKey(), new int[] { t1, t2 });
            }
        }
        return r;
    }

    /** A BARE int-literal MapperS.of: {@code MapperS.of(} + optional '-' + digits + optional L/l + ')'. */
    private static int bareIntSites(String code) {
        int sites = 0, i = code.indexOf(MAPPER_S_OF);
        while (i >= 0) {
            int k = i + MAPPER_S_OF.length();
            if (k < code.length() && code.charAt(k) == '-') k++;
            int d0 = k;
            while (k < code.length() && Character.isDigit(code.charAt(k))) k++;
            if (k > d0) {
                if (k < code.length() && (code.charAt(k) == 'L' || code.charAt(k) == 'l')) k++;
                if (k < code.length() && code.charAt(k) == ')') sites++;
            }
            i = code.indexOf(MAPPER_S_OF, i + MAPPER_S_OF.length());
        }
        return sites;
    }

    private static Map<String, String> readGoldenTree(Path dir) throws IOException {
        Map<String, String> out = new LinkedHashMap<>();
        try (var stream = Files.walk(dir)) {
            for (Path p : (Iterable<Path>) stream.filter(q -> q.toString().endsWith(".java"))::iterator) {
                out.put(dir.relativize(p).toString().replace('\\', '/'), Files.readString(p));
            }
        }
        return out;
    }

    private static String codeOnly(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        int i = 0;
        while (i < s.length()) {
            if (s.startsWith("/*", i)) {
                int end = s.indexOf("*/", i + 2);
                i = end < 0 ? s.length() : end + 2;
            } else if (s.startsWith("//", i)) {
                int end = s.indexOf('\n', i);
                i = end < 0 ? s.length() : end;
            } else if (s.charAt(i) == '"') {
                int j = i + 1;
                while (j < s.length() && s.charAt(j) != '"') {
                    if (s.charAt(j) == '\\') j++;
                    j++;
                }
                sb.append(s, i, Math.min(j + 1, s.length()));
                i = j + 1;
            } else {
                sb.append(s.charAt(i));
                i++;
            }
        }
        return sb.toString();
    }

    private static int countOccurrences(String haystack, String needle) {
        int n = 0, i = haystack.indexOf(needle);
        while (i >= 0) {
            n++;
            i = haystack.indexOf(needle, i + needle.length());
        }
        return n;
    }

    // =========================================================================
    // Corpus generation (the same harness every seat suite uses)
    // =========================================================================

    private static Map<String, String> drr561Output;
    private static List<String> drr561GenErrors = new ArrayList<>();
    private static Map<String, String> drr7Output;

    @BeforeAll
    static void generateCells() throws IOException {
        if (drr561Available()) {
            List<String> errs = new ArrayList<>();
            drr561Output = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "5.61.0", DRR561_CELL_ROOT), errs);
            drr561GenErrors = errs;
        }
        if (drr7Available()) {
            drr7Output = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", DRR7_CELL_ROOT),
                    new ArrayList<>());
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
     * The cell through the REAL {@code IRGeneration} seams — the D11 ON ring's wiring (the flag set for
     * the render and restored after; the provider asserted present; the function seam asserted to hand
     * back the IR-route generator, so this can never silently be an OFF-route render).
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
                + path + " — seat 22 F3: the int literal at a number seat.");
    }

    // =========================================================================
    // Fixture harness
    // =========================================================================

    private static RLinkingResult linking;
    private static RModel mainModel;
    private static Map<String, String> fixtureOut;

    private static void link() throws IOException {
        if (linking == null) {
            RModel main = AstBuilder.buildFromString(MODEL, "seat22f3.rosetta");
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
            fixtureOut = render(m -> "census.seat22f3".equals(m.namespace()));
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
                        m -> "census.seat22f3".equals(m.namespace()));
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
            throw new AssertionError("[IntLiteralNumberSeatTest] builtins parse"
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
