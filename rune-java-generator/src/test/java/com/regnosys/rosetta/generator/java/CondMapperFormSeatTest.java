package com.regnosys.rosetta.generator.java;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.function.LabelProviderGenerator;
import com.regnosys.rosetta.generator.java.function.LabelProviderGeneratorUtil;
import com.regnosys.rosetta.generator.java.object.ChoiceObjectGenerator;
import com.regnosys.rosetta.generator.java.object.ModelObjectGenerator;
import com.regnosys.rosetta.generator.java.reports.ReportGenerator;
import com.regnosys.rosetta.generator.java.reports.RuleGenerator;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.symbols.RLinkingResult;
import com.regnosys.rosetta.symbols.RWorkspace;
import com.regnosys.rosetta.utils.DeepFeatureCallUtil;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.lang.ref.Reference.reachabilityFence;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static com.regnosys.rosetta.testutil.GenerationErrors.assertNoGenerationErrors;

/**
 * v3.1 LADDER RETIREMENT — flip seat 7: the conditional Mapper-form family
 * (the LAW-65 charter {@code target/seat7-charter.md}, written before any
 * emitter edit). Upstream compiles EVERY conditional's branches at the seat's
 * expected type and always builds a statement-form conditional
 * ({@code ExpressionGenerator.xtend:710-718} — never an inline ternary;
 * {@code JavaIfThenElseBuilder.toLambdaBody()} always routes a lambda-body
 * conditional to the block if-return form). The fork's seats decline: the
 * rule/then-body-root statement hoist stays BARE-typed with bare arms
 * ({@code mapperFormSlot}, ControlFlowHandler — the four-parent gate), and a
 * lambda-interior conditional falls to the inline
 * {@code cond.getOrDefault(false) ? a : b} ternary (the CollectionHandler
 * block gates decline a branch carrying a then-pipeline).
 *
 * <p><b>Green-safety (SCAN-PROVEN over the full frozen corpus, all 5 cells —
 * the charter records the runs):</b> {@code getOrDefault(false) ?} appears in
 * ZERO goldens (the standing ControlFlowHandler class-javadoc law), and the
 * bare consumer shape {@code output = ifThenElseResultN;} appears in ZERO
 * goldens (vs 755 golden files carrying
 * {@code output = ifThenElseResultN.get();}) — every file either rung touches
 * is waivered by construction. The bare DECL alone is NOT a marker (goldens
 * legitimately hoist bare at evaluate-arg/ctor seats) — the gate keys on the
 * CONSUMER SEAT, which is what the b-pins here freeze.
 *
 * <p><b>Test geometry (the seat-1..6 pattern):</b> same-workspace controls
 * (RED before the seat) + same-workspace inert pins (GREEN before AND after)
 * + drr 7.0.0 corpus locks (RED before the seat; whole-file byte locks on
 * single-tag carriers, classifier-proven).
 */
class CondMapperFormSeatTest {

    private static final Path REPO_ROOT =
            Path.of(System.getProperty("user.dir")).resolve("..").normalize();

    private static final List<Path> BUILTINS_SEARCH_ROOTS = List.of(
            REPO_ROOT.resolve("test-corpus/rune-dsl-builtins/rune-runtime/src/main/resources/model"),
            REPO_ROOT.resolve("rune-dsl/rune-runtime/src/main/resources/model")
    );

    static boolean builtinsAvailable() {
        return BUILTINS_SEARCH_ROOTS.stream().anyMatch(Files::isDirectory);
    }

    // =========================================================================
    // Control model (same-workspace — the corpus shapes at unit grain)
    // =========================================================================

    /**
     * The corpus shapes at unit grain. {@code KindLadder} plays hkma
     * UnderlyingIdentificationTypeRule (the a1 statement-seat conditional over
     * rule-call arms), {@code SrcKind} plays Counterparty2IdentifierSourceRule
     * (the a2 lambda-interior conditional whose then-branch is a parenthesized
     * then-pipeline), {@code SumOrQty} plays the TotalNotionalQuantity inner
     * block (the a3 sum-in-branch rider), {@code SimpleKind} plays the asic
     * twin (the b4 recovery-already-works block route — simple branch values).
     */
    private static final String CONTROL_MODEL = """
            namespace census.seat7
            version "1.0.0"

            type Pay:
                note string (0..1)
                mnote string (0..1)
                mid string (0..1)
                    [metadata scheme]
                qty number (0..1)
                vals number (0..*)
                kinds string (0..*)
                sub Sub (0..1)
                side Side (0..1)

            type Sub:
                val string (0..1)
                codes string (0..*)

            type Pid:
                ptype PickEnum (0..1)

            type Person2:
                pids Pid (0..*)

            type Party2:
                person Person2 (0..*)
                pid PickEnum (0..*)

            type Side:
                cpty Party2 (0..1)
                    [metadata reference]

            type Wrap:
                items string (0..*)

            enum PickEnum:
                A
                B
                O

            reporting rule PickKind from Pay: <"a1 callee - enum-output rule">
                extract if note exists then PickEnum -> A

            reporting rule OtherId from Pay: <"a1 inner callee - string-output rule">
                extract note

            reporting rule MetaId from Pay: <"a1m inner callee - META-outputted (scheme) rule">
                extract mid

            reporting rule KindLadder from Pay: <"b7 - the PLAIN statement-seat shape (already Mapper-formed)">
                filter note exists
                then if PickKind exists
                    then PickKind
                    else if OtherId exists
                    then PickEnum -> O

            reporting rule KindLadder2 from Pay: <"a1m - the hkma shape: inner exists over the META-outputted rule">
                filter note exists
                then if PickKind exists
                    then PickKind
                    else if MetaId exists
                    then PickEnum -> O

            reporting rule SrcKind from Pay: <"b8 - the PLAIN lambda-pipeline shape (already block-formed)">
                extract sub
                then extract
                    if item -> codes exists
                    then (item -> codes
                        then filter item = "N"
                        then first)
                    else if item -> val = "LEI"
                    then "LEID"

            reporting rule SrcKind2 from Pay: <"a2b - the faithful Counterparty2 replica (meta-ref receiver + first-in-cond + to-string tail)">
                extract side -> cpty
                then extract
                    if person exists
                            and [PickEnum -> A, PickEnum -> B] any = person -> pids -> ptype first
                    then (person -> pids -> ptype
                        then filter ([PickEnum -> A, PickEnum -> B]) any = item
                        then first
                        then extract item to-string)
                    else if pid any = PickEnum -> O
                    then "LEID"

            func MapStyle: <"a2c - the MapIntent shape: alias-call conds + bare enum-value arms">
                inputs:
                    pay Pay (1..1)
                output:
                    result PickEnum (0..1)
                alias hot: pay -> kinds extract item = "HOT"
                set result:
                    pay
                        extract
                            if note = "N1" or mnote = "N2"
                            then A
                            else if hot first and vals exists
                            then B
                            else if note = "O1"
                            then O

            reporting rule SimpleKind from Pay: <"b4 - the asic twin, simple branch values">
                extract sub
                then extract
                    if item -> val exists
                    then PickEnum -> B
                    else if item -> codes exists
                    then PickEnum -> O

            reporting rule Leg1Ok from Pay: <"a4 outer callee - boolean rule">
                extract mnote exists

            reporting rule FlagRule from Pay: <"a4 inner value callee - boolean rule">
                extract qty exists

            func WrapPay:
                inputs:
                    pay Pay (1..1)
                output:
                    out Pay (0..1)
                set out: pay

            func IsHot:
                inputs:
                    pay Pay (1..1)
                output:
                    result boolean (0..1)
                set result: pay -> note = "HOT"

            func IsCold:
                inputs:
                    pay Pay (1..1)
                output:
                    result boolean (0..1)
                set result: pay -> note = "COLD"

            reporting rule NestedThen2 from Pay: <"a4 - the CryptoAsset shape: nested-THEN elseless inners, bare-call + or-chain inner conds">
                filter note exists
                then if Leg1Ok = True
                    then (if IsHot(WrapPay)
                        then FlagRule)
                    else if Leg1Ok = False
                    then (if IsHot(WrapPay)
                                or IsCold(WrapPay)
                        then FlagRule)

            reporting rule NestedThen from Pay: <"b9 - real-else inner nested-THEN (no corpus carrier) stays declined">
                filter note exists
                then if note exists
                    then (if mnote exists then "X" else "Y")
                    else "Z"

            reporting rule MultiJoin from Pay: <"a5 - the MULTI-joined root">
                filter note exists
                then if note exists
                    then kinds
                    else kinds

            func SumOrQty: <"a3 - sum inside a conditional lambda branch">
                inputs:
                    pay Pay (1..1)
                output:
                    result number (0..1)
                set result:
                    pay extract
                        if item -> vals exists
                        then item -> vals sum
                        else item -> qty

            func TakeStr:
                inputs:
                    s string (0..1)
                output:
                    result string (0..1)
                set result: s

            func EvalArgSeat: <"b5 - conditional at an evaluate-arg seat keeps the BARE item hoist">
                inputs:
                    pay Pay (1..1)
                output:
                    result string (0..1)
                set result: TakeStr(if pay -> note exists then pay -> note else pay -> mnote)

            func FnSibling: <"b6 - the FUNCTION-context twin of the a1 shape">
                inputs:
                    pay Pay (1..1)
                output:
                    result PickEnum (0..1)
                set result:
                    if PickKind(pay) exists
                    then PickKind(pay)
                    else PickEnum -> O

            func ExtractRecv: <"b1 - the 432 extract-receiver Mapper slot, already works">
                inputs:
                    pay Pay (1..1)
                output:
                    result int (0..1)
                set result: (if pay -> note exists then 1 else 2) extract item + 1

            func BoolChain: <"b2 - the A2 ComparisonResult hoist, already works">
                inputs:
                    pay Pay (1..1)
                output:
                    result boolean (0..1)
                set result: (if pay -> note exists then True else False) and pay -> mnote exists

            func CtorListSeat: <"b3 - the 327 CondListCoerce seat, already works">
                inputs:
                    pay Pay (1..1)
                output:
                    result Wrap (0..1)
                set result: Wrap {
                    items: if pay -> note exists then pay -> note
                }
            """;

    // =========================================================================
    // Part A — same-workspace controls (RED pre-seat; the golden-law forms)
    // =========================================================================

    /**
     * a1m — the hkma statement-seat shape: the rule then-body-root conditional
     * whose inner rung's exists-condition compiles through the #315 meta-wrap
     * (a {@code JavaConditionalExpression}), which the FER Mapper-form producer
     * refuses — the chain falls to the BARE hoist. Golden law (hkma
     * UnderlyingIdentificationTypeRule): the Mapper-typed hoist, Mapper-form
     * arms, typed ofNull, and the {@code .get()} consumer — around the SAME
     * nested-else skeleton gen already emits.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1m_ruleStatementSeat_metaExistsCond_mapperHoist() throws IOException {
        String out = generateRule(fixture(), "KindLadder2");
        assertContains(out, "final MapperS<PickEnum> ifThenElseResult;");
        assertContains(out, "ifThenElseResult = MapperS.of(pickKindRule.evaluate(thenArg.get()));");
        assertContains(out, "final String string = metaIdRule.evaluate(thenArg.get());");
        assertContains(out, "if ((string == null ? exists(MapperS.<FieldWithMetaString>ofNull())"
                + ".getOrDefault(false) : exists(MapperS.of(FieldWithMetaString.builder()"
                + ".setValue(string).build())).getOrDefault(false))) {");
        assertContains(out, "ifThenElseResult = MapperS.of(PickEnum.O);");
        assertContains(out, "ifThenElseResult = MapperS.<PickEnum>ofNull();");
        assertContains(out, "output = ifThenElseResult.get();");
        assertNotContains(out, "final PickEnum ifThenElseResult;");
        assertNotContains(out, "output = ifThenElseResult;");
    }

    /**
     * a2b — the Counterparty2 lambda shape: a ladder whose rung condition
     * renders MULTI-LINE (the {@code .first()} chain-link continuation) and
     * whose then-arm is a parenthesized then-PIPELINE. The multi-line
     * condition declines the ladder block route (the "no golden carrier shape"
     * claim golden Counterparty2 contradicts) and the whole lambda falls to
     * the non-compiling inline ternary with runtime {@code .then(} chains.
     * Golden law: the block form — the multi-line condition INSIDE
     * {@code if (…)}, the pipeline hoisted as thenArgN locals in the branch.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2b_lambdaLadder_multiLineCond_pipelineArm_blockForm() throws IOException {
        String out = generateRule(fixture(), "SrcKind2");
        assertContains(out, ".mapSingleToItem(item -> {");
        assertContains(out, ".first(), CardinalityOperator.Any)).getOrDefault(false)) {");
        assertContains(out, "final MapperC<PickEnum> thenArg0 = item.<Party2>map(\"Type coercion\"");
        assertContains(out, "final MapperC<PickEnum> thenArg1 = thenArg0");
        assertContains(out, "final MapperS<PickEnum> thenArg2 = thenArg1");
        assertContains(out, "return thenArg2");
        assertContains(out, ".mapSingleToItem(_item -> _item.map(\"to-string\", PickEnum::toDisplayString));");
        assertContains(out, "return MapperS.of(\"LEID\");");
        assertContains(out, "return MapperS.<String>ofNull();");
        assertNotContains(out, ".then(_item");
        assertNotContains(out, "MapperC.of(MapperS.of(\"LEID\"))");
        assertNotContains(out, "getOrDefault(false) ? ");
    }

    /**
     * a2c — the MapIntent lambda shape: alias-call rung conditions rendering
     * multi-line ({@code hot(pay)} + the {@code .first()} continuation) inside
     * a bare-enum-arm ladder. Same decline, same ternary fall-through (with
     * the {@code MapperC.of()} terminal that cannot compile against the
     * mapSingleToItem signature). Golden law (MapIntent): the block form with
     * {@code MapperS.of(<EnumValue>)} returns and the typed ofNull terminal.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2c_lambdaLadder_aliasCallCond_enumArms_blockForm() throws IOException {
        String out = generateFunction(fixture(), "MapStyle");
        assertContains(out, ".mapSingleToItem(item -> {");
        assertContains(out, "if (ComparisonResult.ofNullSafe(hot(pay)");
        assertContains(out, ".first()).andNullSafe(exists(item.<BigDecimal>mapC(\"getVals\"");
        assertContains(out, "return MapperS.of(PickEnum.A);");
        assertContains(out, "return MapperS.of(PickEnum.B);");
        assertContains(out, "return MapperS.of(PickEnum.O);");
        assertContains(out, "return MapperS.<PickEnum>ofNull();");
        assertNotContains(out, " ? PickEnum.A : ");
        assertNotContains(out, ": MapperC.of()");
    }

    /**
     * a4 — the CryptoAsset statement-seat shape: nested-THEN rungs (elseless
     * inners) decline {@code isHoistableThenConditional} at the then-body seat
     * (the 2-arg overload's {@code allowNestedThen=false}) and fall to the
     * BARE hoist. Golden law (csa CryptoAssetUnderlyingIndicatorLeg1Rule): the
     * Mapper hoist; the bare-FUNC inner condition hoists {@code _boolean} with
     * the null-safe guard; the or-chain inner condition stays inline; inner
     * elseless arms take the typed ofNull; the {@code .get()} consumer.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a4_statementSeat_nestedThen_boolHoist_mapperHoist() throws IOException {
        String out = generateRule(fixture(), "NestedThen2");
        assertContains(out, "final MapperS<Boolean> ifThenElseResult;");
        assertContains(out, "final Boolean _boolean = isHot.evaluate(wrapPay.evaluate(thenArg.get()));");
        assertContains(out, "if ((_boolean == null ? false : _boolean)) {");
        assertContains(out, "ifThenElseResult = MapperS.of(flagRuleRule.evaluate(thenArg.get()));");
        assertContains(out, "ifThenElseResult = MapperS.<Boolean>ofNull();");
        assertContains(out, "if (ComparisonResult.ofNullSafe(MapperS.of(isHot.evaluate(wrapPay"
                + ".evaluate(thenArg.get())))).orNullSafe(");
        assertContains(out, "output = ifThenElseResult.get();");
        assertNotContains(out, "final Boolean ifThenElseResult;");
        assertNotContains(out, "output = ifThenElseResult;");
    }

    // =========================================================================
    // Part B — same-workspace inert pins (GREEN before AND after the seat)
    // =========================================================================

    /** b1 — the #432 extract-receiver Mapper slot: already Mapper-formed, untouched. */
    @Test
    @EnabledIf("builtinsAvailable")
    void b1_extractReceiverMapperSlot_untouched() throws IOException {
        String out = generateFunction(fixture(), "ExtractRecv");
        assertContains(out, "final MapperS<Integer> ifThenElseResult;");
        assertContains(out, "ifThenElseResult = MapperS.of(1);");
        assertContains(out, "ifThenElseResult = MapperS.of(2);");
    }

    /** b2 — the A2 ComparisonResult hoist: already CR-formed, untouched. */
    @Test
    @EnabledIf("builtinsAvailable")
    void b2_comparisonResultHoist_untouched() throws IOException {
        String out = generateFunction(fixture(), "BoolChain");
        assertContains(out, "final ComparisonResult ifThenElseResult;");
        assertContains(out, "ifThenElseResult = ComparisonResult.ofNullSafe(MapperS.of(true));");
        assertContains(out, "result = ifThenElseResult.andNullSafe(");
    }

    /** b3 — the #327 CondListCoerce List hoist: already List-formed, untouched. */
    @Test
    @EnabledIf("builtinsAvailable")
    void b3_condListCoerceSeat_untouched() throws IOException {
        String out = generateFunction(fixture(), "CtorListSeat");
        assertContains(out, "final List<String> ifThenElseResult;");
        assertContains(out, "ifThenElseResult = Collections.<String>emptyList();");
        assertContains(out, ".setItems(ifThenElseResult)");
    }

    /**
     * b4 — the asic twin (recovery-already-works): a simple-valued ladder in a
     * lambda ALREADY takes the block route with Mapper-form returns.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b4_simpleValuedLambdaLadder_blockAlreadyWorks() throws IOException {
        String out = generateRule(fixture(), "SimpleKind");
        assertContains(out, ".mapSingleToItem(item -> {");
        assertContains(out, "return MapperS.of(PickEnum.B);");
        assertContains(out, "return MapperS.of(PickEnum.O);");
        assertContains(out, "return MapperS.<PickEnum>ofNull();");
        assertNotContains(out, "getOrDefault(false) ? ");
    }

    /**
     * b5 — THE adversarial placement pin: a conditional at an evaluate-ARG
     * seat keeps the BARE item hoist and the bare consumer (goldens carry
     * 1,000s of bare hoists at exactly this seat — the charter's scan; the
     * Rung-M gate keys on the CONSUMER seat and must never reach this one).
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b5_evaluateArgSeat_bareItemHoist_stays() throws IOException {
        String out = generateFunction(fixture(), "EvalArgSeat");
        assertContains(out, "final String ifThenElseResult;");
        assertContains(out, "result = takeStr.evaluate(ifThenElseResult);");
        assertNotContains(out, "final MapperS<String> ifThenElseResult;");
    }

    /** b6 — the FUNCTION-context sibling: the distributed conditional-SET assignment form stays. */
    @Test
    @EnabledIf("builtinsAvailable")
    void b6_functionContextDistributedAssign_stays() throws IOException {
        String out = generateFunction(fixture(), "FnSibling");
        assertContains(out, "result = pickKindRule.evaluate(pay);");
        assertContains(out, "result = PickEnum.O;");
        assertNotContains(out, "ifThenElseResult");
    }

    /** b7 — the PLAIN rule statement-seat ladder: already Mapper-formed via the FER producer. */
    @Test
    @EnabledIf("builtinsAvailable")
    void b7_plainRuleStatementLadder_alreadyMapperFormed() throws IOException {
        String out = generateRule(fixture(), "KindLadder");
        assertContains(out, "final MapperS<PickEnum> ifThenElseResult;");
        assertContains(out, "ifThenElseResult = MapperS.of(PickEnum.O);");
        assertContains(out, "ifThenElseResult = MapperS.<PickEnum>ofNull();");
        assertContains(out, "output = ifThenElseResult.get();");
    }

    /** b8 — the PLAIN lambda pipeline ladder: already block-formed with in-branch thenArg hoists. */
    @Test
    @EnabledIf("builtinsAvailable")
    void b8_plainLambdaPipelineLadder_alreadyBlockFormed() throws IOException {
        String out = generateRule(fixture(), "SrcKind");
        assertContains(out, ".mapSingleToItem(item -> {");
        assertContains(out, "final MapperC<String> thenArg0 = item.<String>mapC(\"getCodes\"");
        assertContains(out, "final MapperC<String> thenArg1 = thenArg0");
        assertContains(out, "return thenArg1");
        assertContains(out, ".first();");
        assertContains(out, "return MapperS.of(\"LEID\");");
        assertContains(out, "return MapperS.<String>ofNull();");
    }

    /**
     * b9 — the Rung-M seat-uniformity control: a nested-THEN whose inner
     * carries a REAL else has no corpus carrier, but it sits at the SAME
     * rule then-body-root CONSUMER seat, so Rung M Mapper-forms it by the
     * law (the charter scan: the replaced bare consumer shape has ZERO
     * golden carriers; upstream compiles every branch at the seat's Mapper
     * expected type). Fixture-truth: originally frozen as a bare-form
     * decline pin from the PRE probe; converted to the law form when the
     * post-wire run showed the seat-uniform gate reaches it — the seat-5
     * Rung-W posture (the seat law fires at the SEAT, not per-carrier),
     * with the movement receipt confirming ZERO corpus regressions.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b9_realElseInnerNestedThen_mapperFormsWithTheSeat() throws IOException {
        String out = generateRule(fixture(), "NestedThen");
        assertContains(out, "final MapperS<String> ifThenElseResult;");
        assertContains(out, "ifThenElseResult = MapperS.of(\"X\");");
        assertContains(out, "ifThenElseResult = MapperS.of(\"Y\");");
        assertContains(out, "ifThenElseResult = MapperS.of(\"Z\");");
        assertContains(out, "output = ifThenElseResult.get();");
        assertNotContains(out, "output = ifThenElseResult;");
    }

    /** p1 — the sum rider: the block route already renders the typed sum (the Σ receipt-rider's control). */
    @Test
    @EnabledIf("builtinsAvailable")
    void p1_sumInsideConditionalLambdaBranch_typedSum() throws IOException {
        String out = generateFunction(fixture(), "SumOrQty");
        assertContains(out, ".sumBigDecimal();");
        assertNotContains(out, ".sum()");
    }

    /** p2 — the MULTI-joined statement root: already MapperC-formed via the FER producer. */
    @Test
    @EnabledIf("builtinsAvailable")
    void p2_multiJoinedStatementRoot_alreadyMapperCFormed() throws IOException {
        String out = generateRule(fixture(), "MultiJoin");
        assertContains(out, "final MapperC<String> ifThenElseResult;");
        assertContains(out, "output = ifThenElseResult.getMulti();");
    }

    // =========================================================================
    // Part C — drr 7.0.0 corpus locks (RED pre-seat; WHOLE-FILE, classifier-proven)
    // =========================================================================

    private static final Path DRR7_CELL_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path DRR7_GOLDEN_DIR =
            DRR7_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    static boolean drr7Available() {
        return Drr7Corpus.gate(Files.isDirectory(DRR7_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR7_GOLDEN_DIR), CondMapperFormSeatTest.class);
    }

    private static Map<String, String> drr7Output;
    private static List<String> drr7GenErrors;

    @BeforeAll
    static void generateDrr7() throws IOException {
        if (drr7Available()) {
            drr7Output = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", DRR7_CELL_ROOT));
        }
    }

    private static Map<String, String> generateCell(D11CorpusRegressionTest.CellSpec cell)
            throws IOException {
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
        var labelProviderGen = new LabelProviderGenerator(
                gm, typeTranslator, new DeepFeatureCallUtil(gm::getType),
                new LabelProviderGeneratorUtil());
        Map<String, String> output = new LinkedHashMap<>();
        // Copilot #577 R1: capture (not discard) the generator error lists — the
        // drr 7.0.0 cell legitimately reported the 3 standing TYPE_SWITCH_TERNARY_STUB
        // FUNCTION refusals until seat 23 healed them (it reports none today). The
        // harness contract is SPLIT since the v3.1 close-out's discard sweep (#606):
        // the RULE and FUNCTION generators' lists are captured into drr7GenErrors and
        // held to "no error may name a LOCKED file" (asserted in
        // assertByteMatchesGolden via the collected list — a locked carrier that
        // silently failed to generate must fail LOUDLY with the generator's own
        // message, never with a bare "not generated" assertNotNull), while the
        // pojo/choice/report/labelProvider generators' lists — every one a
        // List<GenerationException>, discarded before the sweep — are now strict
        // ZERO through assertNoGenerationErrors. Deliberate: those four kinds carry
        // no tolerated refusal on this cell, so any error there is a defect.
        drr7GenErrors = new ArrayList<>();
        for (RModel model : corpus.workspace().files()) {
            if (gm.shouldGenerate(model)) {
                String version = gm.version(model);
                assertNoGenerationErrors(pojoGen.generateClasses(model, version, output));
                assertNoGenerationErrors(choiceGen.generateClasses(model, version, output));
                ruleGen.generateClasses(model, version, output)
                        .forEach(e -> drr7GenErrors.add(e.getTargetPath() + " — " + e));
                assertNoGenerationErrors(reportGen.generateClasses(model, version, output));
                assertNoGenerationErrors(labelProviderGen.generateClasses(model, version, output));
            }
        }
        // MapIntent is a FUNCTION carrier — the seat-6 harness generated rules
        // only; this seat's locks span both kinds.
        funcGen.generateWithErrors(output)
                .forEach(e -> drr7GenErrors.add(e.getTargetPath() + " — " + e));
        return output;
    }

    /** Rung J+M — the hkma statement-seat carrier (the #576-named 10-line residue). */
    @Test
    @EnabledIf("drr7Available")
    void corpus_underlyingIdentificationType_hkma_wholeFile() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/hkma/rewrite/trade/reports/UnderlyingIdentificationTypeRule.java");
    }

    /** Rung Λ — the multi-line-cond + pipeline-arm lambda carrier. */
    @Test
    @EnabledIf("drr7Available")
    void corpus_counterparty2IdentifierSource_common_wholeFile() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/common/valuation/party/reports/Counterparty2IdentifierSourceRule.java");
    }

    /** Rung Λ — the alias-cond + bare-enum-arm lambda carrier (FUNCTION kind). */
    @Test
    @EnabledIf("drr7Available")
    void corpus_mapIntent_wholeFile() throws IOException {
        assertByteMatchesGolden(
                "drr/ingest/fpml/recordkeeping/message/functions/MapIntent.java");
    }

    /** Rungs N+M2 — the nested-THEN + inner-boolHoist statement carrier. */
    @Test
    @EnabledIf("drr7Available")
    void corpus_cryptoAssetUnderlyingIndicatorLeg1_csa_wholeFile() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/csa/rewrite/trade/reports/CryptoAssetUnderlyingIndicatorLeg1Rule.java");
    }

    private static void assertByteMatchesGolden(String path) throws IOException {
        assertNotNull(drr7Output, "drr 7.0.0 generation did not run — corpus unavailable?");
        // Copilot #577 R1: a generator error naming the locked file fails LOUDLY
        // with the generator's own message (the cell's 3 standing FUNCTION
        // refusals name OTHER files and pass through). Copilot R2 (suppressed
        // block, TRUE): match on the FULL slash path, never the basename —
        // Counterparty2IdentifierSourceRule.java exists at THREE regulation
        // paths, and a basename substring would cross-attribute a twin's error
        // to this lock. contains(path) covers both the recorded targetPath
        // prefix AND a path embedded in the exception message (getTargetPath()
        // is null on wrapper-preserved errors); a null-path error naming only
        // the class falls through to the byte-compare below, which still
        // catches any real generation gap for the locked file.
        List<String> lockedErrors = drr7GenErrors.stream()
                .filter(e -> e.contains(path)).toList();
        assertTrue(lockedErrors.isEmpty(),
                "generator reported errors for the locked file " + path + ": " + lockedErrors);
        String generated = drr7Output.get(path);
        assertNotNull(generated, "not generated: " + path);
        Path goldenPath = DRR7_GOLDEN_DIR.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated drr 7.0.0 output must byte-match the golden (newline-normalized) for "
                + path + " (seat 7 — the conditional Mapper-form family).");
    }

    // =========================================================================
    // Harness (the RuleGeneratorTest pattern, self-contained)
    // =========================================================================

    private static final class FixtureResult {
        final RModel model;
        final RLinkingResult linkingResult;
        final GeneratorModel generatorModel;
        final FunctionGenerator functionGenerator;

        FixtureResult(RModel model, RLinkingResult linkingResult,
                      GeneratorModel generatorModel, FunctionGenerator functionGenerator) {
            this.model = model;
            this.linkingResult = linkingResult;
            this.generatorModel = generatorModel;
            this.functionGenerator = functionGenerator;
        }
    }

    private static FixtureResult fixtureInstance;

    private static FixtureResult fixture() throws IOException {
        if (fixtureInstance == null) {
            fixtureInstance = loadFixture(CONTROL_MODEL);
        }
        return fixtureInstance;
    }

    private static FixtureResult loadFixture(String source) throws IOException {
        RModel model = AstBuilder.buildFromString(source, "seat7.rosetta");
        model.setVersion("0.0.0.test");
        List<RModel> models = new ArrayList<>();
        models.add(model);
        models.addAll(loadBuiltinsOnly());
        RLinkingResult linkingResult = RWorkspace.build(models);
        GeneratorModel gm = new GeneratorModel(linkingResult.workspace());
        JavaTypeUtil typeUtil = new JavaTypeUtil();
        FunctionGenerator fg = new FunctionGenerator(gm, new JavaTypeTranslator(typeUtil), typeUtil);
        return new FixtureResult(model, linkingResult, gm, fg);
    }

    /** All fixture outputs (rules via {@link RuleGenerator#generateClasses}, functions via
     *  {@link FunctionGenerator#generateWithErrors}) keyed by emitted path — built once. */
    private static Map<String, String> fixtureOutput;

    private static Map<String, String> fixtureOutput(FixtureResult fx) {
        if (fixtureOutput == null) {
            Map<String, String> output = new LinkedHashMap<>();
            JavaTypeUtil typeUtil = new JavaTypeUtil();
            RuleGenerator ruleGen = new RuleGenerator(fx.generatorModel,
                    new JavaTypeTranslator(typeUtil), fx.functionGenerator);
            List<String> errors = new ArrayList<>();
            ruleGen.generateClasses(fx.model, "1.0", output)
                    .forEach(e -> errors.add(e.getTargetPath() + " — " + e));
            fx.functionGenerator.generateWithErrors(output)
                    .forEach(e -> errors.add(e.getTargetPath() + " — " + e));
            reachabilityFence(fx.linkingResult);
            if (!errors.isEmpty()) {
                throw new AssertionError("fixture generation errors (a broken fixture"
                        + " must fail loudly, not skip): " + errors);
            }
            fixtureOutput = output;
        }
        return fixtureOutput;
    }

    private static String generateRule(FixtureResult fx, String ruleName) throws IOException {
        return lookup(fixtureOutput(fx), ruleName + "Rule.java");
    }

    private static String generateFunction(FixtureResult fx, String functionName) throws IOException {
        return lookup(fixtureOutput(fx), functionName + ".java");
    }

    private static String lookup(Map<String, String> output, String suffix) {
        return output.entrySet().stream()
                .filter(e -> e.getKey().endsWith("/" + suffix) || e.getKey().endsWith(suffix))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "'" + suffix + "' was not generated; keys=" + output.keySet()));
    }

    private static List<RModel> loadBuiltinsOnly() throws IOException {
        Map<String, Path> resolved = new LinkedHashMap<>();
        for (Path root : BUILTINS_SEARCH_ROOTS) {
            if (!Files.isDirectory(root)) continue;
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
                    try { models.add(AstBuilder.buildFromFile(p)); }
                    catch (Exception e) { failures.add(p + " — " + e); }
                });
        if (!failures.isEmpty()) {
            throw new AssertionError("[CondMapperFormSeatTest] builtins parse failures: "
                    + String.join("; ", failures));
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
