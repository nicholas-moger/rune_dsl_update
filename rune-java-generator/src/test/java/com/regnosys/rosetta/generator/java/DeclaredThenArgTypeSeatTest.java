package com.regnosys.rosetta.generator.java;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
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
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static com.regnosys.rosetta.testutil.GenerationErrors.assertNoGenerationErrors;

/**
 * v3.1 LADDER RETIREMENT — flip seat 14: <b>the then-level declaration must agree with the
 * type its own value actually has</b> (the charter: {@code target/seat14-charter.md}).
 *
 * <p><b>The upstream law.</b> {@code ExpressionGenerator.xtend:1126-1128} compiles a
 * then-argument against {@code MAPPER_C/S.wrapExtends(expr.argument)} — the
 * <b>META-KEEPING</b> overload ({@code JavaTypeUtil:183-185}, versus the META-DROPPING
 * {@code wrapExtendsWithoutMeta} at {@code :179-181}) — and then types the hoisted local
 * from that code. The fork's decl seat instead derives its element from the meta-BLIND
 * {@code getInferredType(...)} snapshot and relies on a stack of narrowly-gated recovery
 * arms to put the wrapper back. Two shapes have no arm.
 *
 * <p><b>The producer, confirmed by RUNTIME PROBE (LAW 72).</b> The seat-14 menu trace
 * attributed this lever to {@code FunctionExpressionRenderer:4872-4873}
 * ({@code renderBareInvokableThenSet}); instrumenting it printed 158 decl sites — 156 of which
 * reach the point where the compiled argument exists (2 take the {@code baseIteHoist} branch) —
 * and <b>not one was a carrier</b>, while the "compiled RHS type" the trace proposed reading was
 * {@code null} at all 156. The carriers are emitted by
 * {@code FunctionExpressionRenderer.renderThenExtractSetImpl}'s decl seat — the same
 * pattern as the seat-11 {@code NavigationHandler:1955} attribution that the seat-13 probe
 * refuted.
 *
 * <p><b>Rung 1 — {@code thenExtractFnCalleeMetaElement} (the META half).</b> A level whose
 * value is an {@code RExtractExpr} whose lambda body is an {@code RSymbolReference} bound to
 * an {@code RFunction} with a meta-annotated OUTPUT: the callee's {@code evaluate()} returns
 * the WRAPPER, so the decl must be the wrapper, but the snapshot reports the bare value type
 * and no arm at this seat consults the callee. <b>The LAW-69 finding:</b> the SIBLING seat
 * already carries exactly this arm — {@code CollectionHandler:3252-3272}, facet
 * {@code thenArgFnExtractCalleeMetaElement} (PR #392) — gated {@code k > 0} for its own
 * carrier set; ours are {@code k == 0} and the AST proof is level-independent. Carriers: the
 * eight fpml {@code Map*AccountPartyReference} functions
 * ({@code fpmlSwap -> swapStream extract MapPayerReceiverToAccountPartyReference(…) then first},
 * the callee's output {@code partyReference Party (0..1) [metadata reference]}).
 *
 * <p><b>Rung 2 — {@code defaultJoinMetaRecovery} (the RULE-CALL half).</b> The emitter for
 * this shape ALREADY exists and is byte-correct: facet {@code ruleRefWrapThenArg} (PR #360)
 * at {@code FunctionExpressionRenderer:6905-6953} hoists the bare {@code evaluate()} result
 * and lifts it into the wrapper. The probe shows every one of its gate conditions passing at
 * the carrier <b>except</b> {@code recoverInnerRuleMetaWrapper(...) == null}. That walker
 * ({@code NavigationHandler:3819+}) dispatches on nav terminals, extract-wrapped then-chains,
 * disguised leaves, deep-path leaves and then-chains — and an {@code RDefaultExpr} (a BINARY
 * node) matches none of them, so a rule whose body is a {@code then default} join reads as
 * non-meta. Upstream ({@code ExpressionGenerator.xtend:452-455}) JOINS the operands'
 * meta-annotated types ({@code joinMetaAnnotatedTypes}) — {@code default} is META-KEEPING when
 * the operands agree. Carriers: {@code QuantitySchedule}'s six-arm {@code then default} over
 * {@code ResolvablePriceQuantity -> quantitySchedule} ({@code [metadata address]}), consumed by
 * {@code NotionalAmountSchedule} (iosco v1/v3) and {@code NotionalQuantitySchedule} (v1).
 *
 * <p><b>LAW 67 — Rung 2 changes a SHARED walker, so every call form was swept.</b>
 * {@code recoverMetaFromExpr} is reached through two public entry points,
 * {@code recoverInnerRuleMetaWrapper} and {@code recoverExprMetaWrapper}, with <b>33 live call
 * sites</b> across six files ({@code FunctionExpressionRenderer} 11, {@code CollectionHandler}
 * 7, {@code ReferenceHandler} 6, {@code ConversionHandler} 4, {@code ControlFlowHandler} 2,
 * {@code NavigationHandler} 2). The walker's return only ever widens {@code null → wrapper}, and
 * every consumer applies its own value-type-equality gate before acting on it, so a consumer can
 * only move when the recovered wrapper matches the bare type it already computed. Measured over
 * the whole 25-cell population, exactly one consumer moved without healing:
 * {@code NotionalQuantityScheduleRule} — see {@code corpus_control3}.
 *
 * <p><b>Green-safety.</b> Rung 1 is safe by TYPE NECESSITY (the #392 argument): a bare-element
 * decl over a wrapper-returning {@code mapItem} body is a generics mismatch that never compiled.
 * Note that this is a property of the PREDICATE, not of the decl token — {@code final
 * MapperC<Party> thenArg} is an ordinary declaration carried by <b>66 goldens across 20 cells,
 * six of them in RING cells</b>; see {@code corpus_control1}. Reach MEASURED by runtime probe
 * over all 1,991 decl sites of the drr 7.0.0 cell: 65 have an {@code RSymbolReference} extract
 * body, split <b>8 carriers ({@code RFunction} + {@code REFERENCE_WITH_META}) + 51
 * {@code RFunction} with {@code MetaKind.NONE} + 6 {@code RRule} references</b>. The two
 * exclusions are done by DIFFERENT gates: {@code detectMetaKind != NONE} excludes the 51
 * ({@code b1}) and {@code instanceof RFunction} excludes the 6 ({@code b2}) — so the meta-kind
 * gate, not the RFunction check, is what keeps this arm off ordinary function-call extract
 * levels. Rung 2 declines whenever the arms do not recover the SAME wrapper — including the
 * in-corpus {@code FixingDate}, whose identically-shaped six-arm {@code then default} navigates
 * to the non-meta {@code settlementTerms} ({@code b4}, and the corpus stays-identical lock).
 */
class DeclaredThenArgTypeSeatTest {

    private static final Path REPO_ROOT =
            Path.of(System.getProperty("user.dir")).resolve("..").normalize();

    private static final List<Path> BUILTINS_SEARCH_ROOTS = List.of(
            REPO_ROOT.resolve("test-corpus/rune-dsl-builtins/rune-runtime/src/main/resources/model"),
            REPO_ROOT.resolve("rune-dsl/rune-runtime/src/main/resources/model"),
            // THIS repo's own builtins, LAST so the two roots above keep their precedence
            // (loadBuiltinsOnly is putIfAbsent keyed on file name, so first root wins) but a
            // checkout without the external corpus RUNS these tests instead of silently
            // skipping them. A test that skips is not a lock - Copilot's suppressed finding on
            // PR #586 and the independent review's NIT 7, same class as #579 R1-1.
            REPO_ROOT.resolve("rune-runtime/src/main/resources/model")
    );

    static boolean builtinsAvailable() {
        return BUILTINS_SEARCH_ROOTS.stream().anyMatch(Files::isDirectory);
    }

    /**
     * The DECOY namespace — {@code Sched} under a SECOND namespace, registered FIRST, so a
     * value-type comparison that comes down to the SIMPLE name would wrongly join (b5).
     */
    private static final String MODEL_OTHER = """
            namespace census.seat14.other
            version "1.0.0"

            type Sched: <"the DECOY - the same simple name as the dep type, another namespace">
                amt string (0..1)
            """;

    /**
     * The dependency namespace — the corpus condition: LOADED but NOT GENERATED (the emission
     * filter accepts {@code census.seat14} only), exactly as every drr cell navigates vendored
     * cdm types. This is what gives the Rung-1 import consequence (a2) real content: the decl's
     * element type is imported from ANOTHER package, so swapping bare → wrapper must drop the
     * bare import.
     */
    private static final String MODEL_DEP = """
            namespace census.seat14.dep
            version "1.0.0"

            type P: <"the Rung-1 element - the bare value type">
                pn string (0..1)

            type Sched: <"the Rung-2 element - shares its simple name with the decoy (b5)">
                amt string (0..1)

            type Leg:
                legTag string (0..1)

            type Root:
                legs Leg (0..*)

            type Holder: <"a3/b4 - the meta leaf and its non-meta twin">
                sch Sched (0..1)
                    [metadata reference]
                bare Sched (0..1)

            type OtherHolder: <"b3 - a DIFFERENT value type behind the same meta kind">
                p P (0..1)
                    [metadata reference]

            type DecoyHolder: <"b5 - the same SIMPLE name, another namespace">
                sch census.seat14.other.Sched (0..1)
                    [metadata reference]

            type Inner:
                a Holder (0..1)
                b Holder (0..1)
                o OtherHolder (0..1)
                d DecoyHolder (0..1)
            """;

    private static final String MODEL_MAIN = """
            namespace census.seat14
            version "1.0.0"

            import census.seat14.dep.*

            func MakeRef: <"the Rung-1 callee - a META-annotated output (the corpus callee's shape)">
                inputs:
                    l Leg (0..1)
                output:
                    pr P (0..1)
                        [metadata reference]
                set pr: empty

            func MakeBare: <"b1 - the GREEN control callee - a BARE output">
                inputs:
                    l Leg (0..1)
                output:
                    pb P (0..1)
                set pb: empty

            func A1Carrier: <"a1/a2 - the Rung-1 carrier, the CORPUS shape: nav -> extract <fnCall> then first">
                inputs:
                    rt Root (0..1)
                output:
                    out P (0..1)
                        [metadata reference]
                set out:
                    rt -> legs
                        extract MakeRef(item)
                        then first

            func B1BareCallee: <"b1 - GREEN control: the same shape over a BARE callee output">
                inputs:
                    rt Root (0..1)
                output:
                    out P (0..1)
                set out:
                    rt -> legs
                        extract MakeBare(item)
                        then first

            reporting rule B2RuleBody from Root: <"b2 - GREEN control: the extract body is a RULE reference, not an RFunction">
                extract legs
                    extract B2Inner
                    then first

            reporting rule B2Inner from Leg: <"b2 - the inner rule the control's extract body references">
                extract legTag

            reporting rule A3Inner from Inner: <"a3 - the Rung-2 inner rule: a `then default` join over the SAME meta leaf">
                extract inn [
                    inn -> a -> sch
                        then default inn -> b -> sch
                ]

            reporting rule A3Carrier from Inner: <"a3 - the Rung-2 consumer: the #360 lift must fire">
                A3Inner
                    then extract amt

            reporting rule A4Inner from Inner: <"a4 - a PRESERVING step between the head and the default arm">
                extract inn [
                    inn -> a -> sch
                        then filter amt exists
                        then default inn -> b -> sch
                ]

            reporting rule A4Carrier from Inner: <"a4 - the consumer: the preserving step must be SKIPPED, not read as a bare arm">
                A4Inner
                    then extract amt

            reporting rule B3Inner from Inner: <"b3 - the arms recover DIFFERENT value types behind the same meta kind">
                extract inn [
                    inn -> a -> sch
                        then default inn -> o -> p
                ]

            reporting rule B3Carrier from Inner: <"b3 - GREEN control: a disagreeing join is BARE">
                B3Inner
                    then extract amt

            reporting rule B4Inner from Inner: <"b4 - the DECLINE control: the identical shape over a NON-META leaf (the in-corpus FixingDate shape)">
                extract inn [
                    inn -> a -> bare
                        then default inn -> b -> bare
                ]

            reporting rule B4Carrier from Inner: <"b4 - GREEN control: a non-meta join stays bare">
                B4Inner
                    then extract amt

            reporting rule B5Inner from Inner: <"b5 - THE adversarial pin: the same meta KIND over the same SIMPLE name in ANOTHER namespace">
                extract inn [
                    inn -> a -> sch
                        then default inn -> d -> sch
                ]

            reporting rule B5Carrier from Inner: <"b5 - the join must be value-type EXACT (canonical, not simple)">
                B5Inner
                    then extract amt
            """;

    // The Rung-1 tokens.
    private static final String R1_CARRIER_DECL = "final MapperC<P> thenArg";
    private static final String R1_HEALED_DECL = "final MapperC<ReferenceWithMetaP> thenArg";
    private static final String R1_BARE_IMPORT = "import census.seat14.dep.P;";
    private static final String R1_WRAPPER_IMPORT =
            "import census.seat14.dep.metafields.ReferenceWithMetaP;";

    // The Rung-2 tokens — the #360 lift block.
    private static final String R2_CARRIER_DECL = "final MapperS<Sched> thenArg";
    private static final String R2_HEALED_DECL = "final MapperS<ReferenceWithMetaSched> thenArg";
    private static final String R2_LIFT_PREDECL = "final Sched sched = a3InnerRule.evaluate(input);";

    // =========================================================================
    // Part A — RED pre-seat
    // =========================================================================

    /** a1 — Rung 1: the decl takes the callee's meta WRAPPER, not the meta-blind bare value. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_metaAnnotatedCalleeOutput_typesTheThenArgAtTheWrapper() throws IOException {
        String out = filtered("A1Carrier.java");
        assertContains(out, R1_HEALED_DECL);
        assertNotContains(out, R1_CARRIER_DECL);
    }

    /**
     * a2 — Rung 1's IMPORT consequence (LAW 70 — the decl seat is the only registrant of the
     * element's import, so re-typing it must drop the bare import and add the wrapper's).
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_metaAnnotatedCalleeOutput_dropsTheBareImportAndAddsTheWrapper() throws IOException {
        String out = filtered("A1Carrier.java");
        assertContains(out, R1_WRAPPER_IMPORT);
        assertNotContains(out, R1_BARE_IMPORT);
    }

    /**
     * a3 — Rung 2: a rule whose body is a {@code then default} join over the SAME meta leaf is
     * meta, so the #360 lift fires at the consuming then-level: the bare {@code evaluate()}
     * result is hoisted to its own local and lifted into the wrapper.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a3_defaultJoinOverTheSameMetaLeaf_firesTheRuleWrapLift() throws IOException {
        String out = filtered("A3CarrierRule.java");
        assertContains(out, R2_HEALED_DECL);
        assertContains(out, R2_LIFT_PREDECL);
        assertNotContains(out, R2_CARRIER_DECL);
    }

    /**
     * a4 — the ORDERING regression, found by deep self-review of this seat's own code.
     *
     * <p>The walker collects a then-chain's bodies OUTERMOST-first, so in
     * {@code <nav> then filter … then default <meta>} the {@code default} ARM is visited BEFORE
     * the element-preserving {@code filter}. An earlier draft of Rung 2 decided a body that
     * recovered nothing as soon as a join was pending, which mis-declined that shape: the
     * {@code filter} is element-preserving and must be SKIPPED whether or not an arm has been
     * seen. The fix was to DELETE that early return and let the pre-existing skip/stop logic
     * run — it is already correct for both states.
     *
     * <p>No corpus carrier has this shape (the drr {@code QuantitySchedule} arms are contiguous),
     * so this test is the only thing standing between the class and a silent regression.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a4_preservingStepBetweenHeadAndDefaultArm_stillRecovers() throws IOException {
        String out = filtered("A4CarrierRule.java");
        assertContains(out, "final MapperS<ReferenceWithMetaSched> thenArg");
        assertNotContains(out, "final MapperS<Sched> thenArg");
    }

    // =========================================================================
    // Part B — GREEN controls (must be unchanged by the seat)
    // =========================================================================

    /** b1 — a BARE callee output keeps the bare decl (the Rung-1 predicate must discriminate). */
    @Test
    @EnabledIf("builtinsAvailable")
    void b1_bareCalleeOutput_keepsTheBareDecl() throws IOException {
        String out = filtered("B1BareCallee.java");
        assertContains(out, "final MapperC<P> thenArg");
        assertNotContains(out, "ReferenceWithMetaP> thenArg");
    }

    /**
     * b2 — an extract body that is a RULE reference (not an {@code RFunction}) is excluded.
     * Four such sites exist in the drr 7.0.0 cell; the {@code instanceof RFunction} gate is the
     * only thing keeping them out.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b2_ruleReferenceExtractBody_keepsTheBareDecl() throws IOException {
        String out = filtered("B2RuleBodyRule.java");
        // POSITIVE half (NIT 7): the file must have rendered a body at all.
        assertContains(out, "thenArg");
        assertNotContains(out, "ReferenceWithMeta");
    }

    /**
     * b3 — the arms recover wrappers over DIFFERENT value types: {@code joinMetaAnnotatedTypes}
     * joins to the bare common type, so the recovery must DECLINE.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b3_defaultJoinOverDifferentValueTypes_declines() throws IOException {
        String out = filtered("B3CarrierRule.java");
        // POSITIVE half (NIT 7): the fixture must actually EMIT the decl, else the negative
        // assertion below would pass on an empty render.
        assertContains(out, R2_CARRIER_DECL);
        assertNotContains(out, "ReferenceWithMetaSched> thenArg");
    }

    /**
     * b4 — THE DECLINE CONTROL, and the in-corpus one: the identical six-arm shape over a
     * NON-META leaf ({@code FixingDate} navigates to {@code settlementTerms}). Every arm
     * recovers null ⇒ the join recovers null ⇒ nothing moves.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b4_defaultJoinOverANonMetaLeaf_declines() throws IOException {
        String out = filtered("B4CarrierRule.java");
        // POSITIVE half (NIT 7). b4 is immune to mutations of the JOIN's agreement test (its
        // arms recover null, so the arm short-circuits before any comparison) — which is exactly
        // why it must at least prove the decl is rendered and bare.
        assertContains(out, R2_CARRIER_DECL);
        assertNotContains(out, "ReferenceWithMeta");
    }

    /**
     * b5 — THE ADVERSARIAL PLACEMENT PIN. The two arms carry the SAME meta kind over types
     * with the SAME SIMPLE NAME in DIFFERENT namespaces (the decoy is registered FIRST). A
     * simple-name comparison would mint a wrapper the render disagrees with; the join must be
     * CANONICAL-exact and therefore decline.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b5_defaultJoinOverSameSimpleNameDifferentNamespace_declines() throws IOException {
        String out = filtered("B5CarrierRule.java");
        // POSITIVE half (NIT 7).
        assertContains(out, R2_CARRIER_DECL);
        assertNotContains(out, "ReferenceWithMetaSched> thenArg");
    }

    // =========================================================================
    // Part C — the corpus locks (drr 7.0.0)
    // =========================================================================

    private static final Path DRR7_CELL_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path DRR7_GOLDEN_DIR =
            DRR7_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    static boolean drr7Available() {
        return Drr7Corpus.gate(builtinsAvailable()
                && Files.isDirectory(DRR7_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR7_GOLDEN_DIR), DeclaredThenArgTypeSeatTest.class);
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

    /**
     * Rung 1 — the 8 fpml WHOLE-FILE heals. Every diff line is the {@code thenArg} decl's
     * element plus the now-unused bare import; goldens sha-identical across drr
     * 7.0.0/7.1.0/7.2.0/7.3.0, so each counts ×4.
     */
    private static final List<String> RUNG1_WHOLE_FILE_LOCKS = List.of(
            "cdm/ingest/fpml/confirmation/product/fxsingleleg/functions/MapFxSingleLegAccountPartyReference.java",
            "cdm/ingest/fpml/confirmation/product/fxswap/functions/MapFxSwapAccountPartyReference.java",
            "cdm/ingest/fpml/confirmation/product/returnswap/functions/MapReturnSwapAccountPartyReference.java",
            "cdm/ingest/fpml/confirmation/product/swap/functions/MapSwapAccountPartyReference.java",
            "cdm/ingest/fpml/confirmation/product/varianceswap/functions/MapVarianceSwapAccountPartyReference.java",
            "cdm/ingest/fpml/confirmation/product/varianceswaptransactionsupplement/functions/MapVarianceSwapTransactionSupplementAccountPartyReference.java",
            "cdm/ingest/fpml/confirmation/product/volatilityswap/functions/MapVolatilitySwapAccountPartyReference.java",
            "cdm/ingest/fpml/confirmation/product/volatilityswaptransactionsupplement/functions/MapVolatilitySwapTransactionSupplementAccountPartyReference.java");

    /**
     * Rung 2 — the 2 iosco WHOLE-FILE heals (the #360 lift + its downstream coercions).
     *
     * <p><b>Not here, and why:</b> {@code NotionalQuantityScheduleRule} (iosco v1) carries the
     * same two {@code thenArg} mismatches but is NOT a carrier of this seat — it does not
     * consume {@code QuantitySchedule} at a then-level base. Its body nests the rule call
     * inside a FUNCTION-CALL ARGUMENT
     * ({@code EnrichDatedValueWithEndDate(QuantitySchedule then filter … then extract datedValue,
     * CustomSchedule) extract …}), so the decl is emitted by a different producer and the #360
     * gate ({@code base instanceof RSymbolReference} bound to an {@code RRule}) never sees it.
     * Banked as a seat-15 menu item in {@code target/seat14-charter.md} §7.
     */
    private static final List<String> RUNG2_WHOLE_FILE_LOCKS = List.of(
            "drr/standards/iosco/cde/version1/quantity/reports/NotionalAmountScheduleRule.java",
            "drr/standards/iosco/cde/version3/quantity/reports/NotionalAmountScheduleRule.java");

    /**
     * The in-corpus DECLINE witness: {@code FixingDate} has the SAME six-arm {@code then
     * default} body as {@code QuantitySchedule} but navigates to the non-meta
     * {@code settlementTerms}. Rung 2 must not touch it. (Its {@code thenArg2} mismatch was
     * the OPPOSITE direction — gen WRAPPER / golden BARE, the Rm1-B lever — until seat-28
     * law 5 healed the file WHOLE; the lock now asserts the healed bare decl and the
     * wrapper form's absence.)
     */
    private static final String DECLINE_WITNESS =
            "drr/regulation/common/trade/datetime/reports/FixingDateRule.java";

    @Test @EnabledIf("drr7Available") void corpus_r1_01_mapFxSingleLeg() throws IOException { lock(RUNG1_WHOLE_FILE_LOCKS.get(0)); }
    @Test @EnabledIf("drr7Available") void corpus_r1_02_mapFxSwap() throws IOException { lock(RUNG1_WHOLE_FILE_LOCKS.get(1)); }
    @Test @EnabledIf("drr7Available") void corpus_r1_03_mapReturnSwap() throws IOException { lock(RUNG1_WHOLE_FILE_LOCKS.get(2)); }
    @Test @EnabledIf("drr7Available") void corpus_r1_04_mapSwap_theProbedCarrier() throws IOException { lock(RUNG1_WHOLE_FILE_LOCKS.get(3)); }
    @Test @EnabledIf("drr7Available") void corpus_r1_05_mapVarianceSwap() throws IOException { lock(RUNG1_WHOLE_FILE_LOCKS.get(4)); }
    @Test @EnabledIf("drr7Available") void corpus_r1_06_mapVarianceSwapTransactionSupplement() throws IOException { lock(RUNG1_WHOLE_FILE_LOCKS.get(5)); }
    @Test @EnabledIf("drr7Available") void corpus_r1_07_mapVolatilitySwap() throws IOException { lock(RUNG1_WHOLE_FILE_LOCKS.get(6)); }
    @Test @EnabledIf("drr7Available") void corpus_r1_08_mapVolatilitySwapTransactionSupplement() throws IOException { lock(RUNG1_WHOLE_FILE_LOCKS.get(7)); }

    @Test @EnabledIf("drr7Available") void corpus_r2_01_notionalAmountSchedule_v1() throws IOException { lock(RUNG2_WHOLE_FILE_LOCKS.get(0)); }
    @Test @EnabledIf("drr7Available") void corpus_r2_02_notionalAmountSchedule_v3() throws IOException { lock(RUNG2_WHOLE_FILE_LOCKS.get(1)); }

    /**
     * Control 1 (LAW 66 + LAW 72 — a control's domain is the MECHANISM's reach, and it must be
     * able to FAIL).
     *
     * <p><b>The proposition this used to assert was false.</b> An earlier draft claimed "the
     * pre-fix Rung-1 form appears in NO golden" while only scanning the eight carriers' own
     * goldens in one cell. The token is in fact carried by <b>66 goldens across 20 cells</b>,
     * <b>six of them in RING cells</b> ({@code cdm 5.38.0} ×1, {@code cdm 6.20.6} ×1,
     * {@code drr 6.34.1} ×4) — e.g. {@code cdm-6.20.6/…/party/functions/ReplaceParty.java:57}
     * and {@code drr-6.34.1/…/Beneficiary1Rule.java:57}. `final MapperC<Party> thenArg` is a
     * perfectly ordinary decl; it is only wrong at a level whose extract body calls a
     * META-annotated function.
     *
     * <p>Safety therefore comes from the <b>predicate</b> (the callee's output
     * {@code MetaKind != NONE}), never from the token's absence — so this control now asserts
     * the property that actually matters: <b>of the drr 7.0.0 goldens that carry the token and
     * are NOT carriers, the set that fails to byte-generate must be EXACTLY the two that
     * already failed before this seat.</b> There are eight such files, disjoint from the eight
     * carriers; six byte-generate today and two are pre-existing direction-B (Rm1-B) mismatches
     * whose generated bytes are identical PRE and POST. A new entry = Rung 1 over-fired.
     */
    /**
     * The two non-carrier token holders that were ALREADY byte-divergent before this seat, each
     * verified byte-identical in the generated output PRE vs POST (so Rung 1 does not touch
     * them). Named rather than silently tolerated: if a THIRD file ever joins them, the control
     * below fails, which is exactly the over-fire signal it exists to give.
     */
    // the direction-B (gen WRAPPER / golden BARE) Enrich holder LEFT this list at seat 33: law E.234 rung E.2
    // (deepThenBareElementStampSuppress - the Rm1-B lever this comment always named) healed the file WHOLE;
    // transcribed from the control print (E234-trip1.log). The list is EMPTY: a NEW entry = an over-fire.
    private static final List<String> CONTROL1_PRE_EXISTING_MISMATCHES = List.of();
            // (ExtractPartyByNameContains left this list when seat-28 law 6 — the
            //  filter-predicate META deref — healed it WHOLE; re-pinned exactly as this
            //  control's own failure text instructs. One Rm1-B holder remains.)

    @Test
    @EnabledIf("drr7Available")
    void corpus_control1_nonCarrierGoldensCarryingTheTokenStillByteGenerate() throws IOException {
        assertNotNull(drr7Output, "drr 7.0.0 generation did not run — corpus unavailable?");
        List<String> scanned = new ArrayList<>();
        List<String> diverged = new ArrayList<>();
        try (var walk = Files.walk(DRR7_GOLDEN_DIR)) {
            for (Path golden : walk.filter(p -> p.toString().endsWith(".java")).toList()) {
                String goldenText = Files.readString(golden);
                if (!goldenText.contains("final MapperC<Party> thenArg")) {
                    continue;
                }
                String rel = DRR7_GOLDEN_DIR.relativize(golden).toString().replace('\\', '/');
                if (RUNG1_WHOLE_FILE_LOCKS.contains(rel)) {
                    continue; // a carrier — its golden is the POST form, locked above
                }
                scanned.add(rel);
                String generated = drr7Output.get(rel);
                if (generated == null || !normalize(goldenText).equals(normalize(generated))) {
                    diverged.add(rel);
                }
            }
        }
        assertTrue(scanned.size() >= 8,
                "the control scanned only " + scanned.size() + " non-carrier goldens carrying the"
                + " token — expected at least 8 in drr 7.0.0. A control that scans nothing cannot"
                + " fail; re-derive its domain.");
        assertEquals(CONTROL1_PRE_EXISTING_MISMATCHES, diverged,
                "the set of non-carrier token holders that do NOT byte-generate has changed."
                + " Every file here carries `final MapperC<Party> thenArg` in its golden but is"
                + " NOT a carrier of this seat, so Rung 1's predicate must leave it alone. A NEW"
                + " entry means the predicate OVER-FIRED; a MISSING entry means one of the two"
                + " known pre-existing mismatches healed (a good thing — re-pin this list).");
    }

    /**
     * Control 2 — the in-corpus DECLINE witness stays byte-for-byte as it is today. Rung 2's
     * join must not fire on {@code FixingDate}'s non-meta arms.
     */
    @Test
    @EnabledIf("drr7Available")
    void corpus_control2_declineWitnessBytesDoNotMove() throws IOException {
        assertNotNull(drr7Output, "drr 7.0.0 generation did not run — corpus unavailable?");
        String generated = drr7Output.get(DECLINE_WITNESS);
        assertNotNull(generated, "not generated: " + DECLINE_WITNESS);
        // Re-pinned at seat 28: law 5 (the `default` meta join read from the RENDERED left)
        // healed this witness file WHOLE — the Rm1-B wrapper decl it used to carry is gone and
        // the file is byte-identical to golden. The decline lock survives in its strongest
        // form: the arms are non-meta, the decl is BARE, and the wrapper form must not return.
        assertTrue(generated.contains("final MapperS<Date> thenArg2"),
                "the healed decline witness lost its bare thenArg2 decl — a lever moved this"
                + " file: " + DECLINE_WITNESS);
        assertTrue(!generated.contains("final MapperS<FieldWithMetaDate> thenArg2"),
                "the Rm1-B wrapper decl RETURNED — Rung 2 fired where its arms are non-meta,"
                + " or the seat-28 law-5 heal regressed: " + DECLINE_WITNESS);
    }

    /**
     * Control 3 — THE FORMERLY-EXPOSED HALF. <b>RE-PINNED at PR #586 (seat 15), which closed the
     * decl half of this gap</b>, exactly as the previous revision of this javadoc instructed.
     *
     * <p>{@code NotionalQuantityScheduleRule} (iosco v1) consumes the SAME
     * {@code QuantitySchedule} rule, so Rung 2's corrected recovery reaches it. At #585 its
     * {@code thenArg} DECLS were still emitted by a different producer — the rule call sits
     * inside a FUNCTION-CALL ARGUMENT
     * ({@code EnrichDatedValueWithEndDate(QuantitySchedule then filter … then extract datedValue,
     * CustomSchedule)}), which the #360 hoist seat never sees — so they stayed typed at the bare
     * value. Seat 15 ported that arm ({@code CollectionHandler}, facet
     * {@code argPosRuleRefWrapThenArg}) and the decls now carry the wrapper; the file went
     * <b>12 → 2 diff-lines</b>.
     *
     * <p><b>#585's DISCLOSURE WAS CORRECT, AND SEAT 15 REPAIRS IT.</b> An earlier revision of this
     * javadoc tried to retract #585's statement that the file "COMPILED before this seat and does
     * not after", on a type-level derivation. <b>The independent review refuted that retraction and
     * a real compiler settled it</b> (PR #586; three probes against the shipped {@code rune-runtime},
     * recorded in the PR body). The retraction is withdrawn; #585's own self-disclosure stands.
     *
     * <p>(1) <b>THE MECHANISM, COMPILER-CONFIRMED.</b> The regression is the {@code "Type coercion"}
     * hop, not the terminal. {@code MapperS.map} ({@code MapperS.java:102}) is
     * {@code <F> MapperS<F> map(String, Function<T,F>)} and the emitted witness supplies {@code F}
     * EXPLICITLY as {@code NonNegativeQuantitySchedule}. At #585 the receiver was still declared
     * {@code MapperS<NonNegativeQuantitySchedule>}, so {@code T} was the bare value and the lambda
     * body {@code …getValue()} returned {@code BigDecimal} (inherited from {@code Schedule}) —
     * <b>{@code error: incompatible types: BigDecimal cannot be converted to …}</b>. Seat 15 types
     * that receiver at {@code MapperS<ReferenceWithMetaNonNegativeQuantitySchedule>}, whose
     * {@code getValue()} returns {@code NonNegativeQuantitySchedule} = the explicit witness, and the
     * same shape <b>compiles</b>. So: pre-#585 compiled (no hop), post-#585 did not, post-#586 does.
     *
     * <p>(2) <b>THE RETRACTION'S DERIVATION WAS WRONG ON THE JLS, AND ON A COMPILER.</b> It claimed
     * {@code List<NotionalPeriod.NotionalPeriodBuilder> output = toBuilder(<single>)} cannot infer
     * {@code R} "because {@code List} is not a {@code RosettaModelObjectBuilder}". Java does not
     * require {@code R} to BE the target, only {@code R <: target}: both bounds are INTERFACES, so
     * JLS 18.4 resolves {@code R = glb(RosettaModelObjectBuilder, List<…Builder>)} and JLS 5.1.10
     * leaves {@code glb} undefined only when two bounds are CLASSES. Compiled against the real
     * {@code RosettaModelObject}/{@code RosettaModelObjectBuilder}, that assignment <b>compiles</b>.
     * The terminal is a byte divergence, not a compile error.
     *
     * <p>The terminal divergence ({@code …mapItem(…).get()} where golden has {@code .getMulti()})
     * DID predate #585 — that half of the earlier note was measured and was kept banked as
     * <b>seat-16 item #0</b> (its producer refuted four candidate seats by runtime probe,
     * {@code target/seat15-charter.md} §1.2). <b>It LANDED at seat 29</b>: law 4
     * ({@code setTerminalReparentedRuleMulti}) located the producer — the set-terminal read of a
     * reparented rule's multi — and healed the file WHOLE (measured in the seat-29 chain; this
     * control's residue assert fired as a heal tripwire, the LAW-81 flow). The control is now a
     * whole-file lock: the healed decls stay pinned as mechanism witnesses, and the file must be
     * byte-identical to golden.
     */
    @Test
    @EnabledIf("drr7Available")
    void corpus_control3_argumentPositionDeclsAreHealedWholeFileLocked() throws IOException {
        assertNotNull(drr7Output, "drr 7.0.0 generation did not run — corpus unavailable?");
        String generated = drr7Output.get(
                "drr/standards/iosco/cde/version1/quantity/reports/NotionalQuantityScheduleRule.java");
        assertNotNull(generated, "not generated: NotionalQuantityScheduleRule");
        assertTrue(generated.contains("\"Type coercion\", referenceWithMetaNonNegativeQuantitySchedule"),
                "Rung 2's recovery no longer reaches NotionalQuantityScheduleRule — the shared"
                + " walker changed, or this consumer moved. Re-pin deliberately.");
        assertTrue(generated.contains(
                        "final NonNegativeQuantitySchedule nonNegativeQuantitySchedule = "
                        + "quantityScheduleRule.evaluate(input);"),
                "seat 15's argument-position rule-wrap arm no longer fires — the hoisted"
                + " bare-value local is gone. Re-pin deliberately.");
        assertTrue(generated.contains(
                        "final MapperS<ReferenceWithMetaNonNegativeQuantitySchedule> thenArg0"),
                "seat 15's argument-position rule-wrap arm no longer types the base level at the"
                + " wrapper. Re-pin deliberately.");
        // The state is identical in drr 7.1.0/7.2.0/7.3.0. Rather than generate three more cells
        // (a 4x cost for one assertion), pin the CROSS-CELL GOLDEN IDENTITY that makes pinning
        // drr 7.0.0 pin all four: if a sibling cell's golden ever diverges, this cell stops
        // standing proxy for it and the pin must be widened.
        Path g70 = DRR7_GOLDEN_DIR.resolve(
                "drr/standards/iosco/cde/version1/quantity/reports/NotionalQuantityScheduleRule.java");
        String golden70 = Files.readString(g70);
        assertEquals(normalize(golden70), normalize(generated),
                "NotionalQuantityScheduleRule is a WHOLE-FILE lock since seat 29 (law 4,"
                + " setTerminalReparentedRuleMulti, landed seat-16 item #0) — any divergence from"
                + " golden is a regression.");
        for (String sibling : List.of("7.1.0", "7.2.0", "7.3.0")) {
            Path gs = Path.of("../test-corpus/drr/drr-" + sibling
                    + "/rosetta-source/src/generated/java/drr/standards/iosco/cde/version1"
                    + "/quantity/reports/NotionalQuantityScheduleRule.java");
            if (!Files.isRegularFile(gs)) {
                continue; // that cell is not in this checkout
            }
            assertEquals(normalize(golden70), normalize(Files.readString(gs)),
                    "drr " + sibling + "'s golden for NotionalQuantityScheduleRule no longer"
                    + " matches drr 7.0.0's, so pinning 7.0.0 no longer pins all four cells —"
                    + " widen this control.");
        }
    }

    private static void lock(String path) throws IOException {
        assertNotNull(drr7Output, "drr 7.0.0 generation did not run — corpus unavailable?");
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
                + path + " (seat 14 — the then-level decl must agree with its own value's type).");
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
        var dataRuleGen = new DataRuleGenerator(gm, typeTranslator, typeUtil);
        var labelProviderGen = new LabelProviderGenerator(
                gm, typeTranslator, new DeepFeatureCallUtil(gm::getType),
                new LabelProviderGeneratorUtil());
        Map<String, String> output = new LinkedHashMap<>();
        drr7GenErrors = new ArrayList<>();
        for (RModel model : corpus.workspace().files()) {
            if (gm.shouldGenerate(model)) {
                String version = gm.version(model);
                assertNoGenerationErrors(pojoGen.generateClasses(model, version, output));
                assertNoGenerationErrors(choiceGen.generateClasses(model, version, output));
                ruleGen.generateClasses(model, version, output)
                        .forEach(e -> drr7GenErrors.add(e.getTargetPath() + " — " + e));
                assertNoGenerationErrors(reportGen.generateClasses(model, version, output));
                dataRuleGen.generateClasses(model, version, output)
                        .forEach(e -> drr7GenErrors.add(e.getTargetPath() + " — " + e));
                assertNoGenerationErrors(labelProviderGen.generateClasses(model, version, output));
            }
        }
        funcGen.generateWithErrors(output)
                .forEach(e -> drr7GenErrors.add(e.getTargetPath() + " — " + e));
        return output;
    }

    // =========================================================================
    // Harness — one linked workspace (decoy FIRST, then the dependency, then the
    // generated namespace), rendered under the corpus emission condition.
    // =========================================================================

    private static RLinkingResult linking;
    private static RModel mainModel;
    private static Map<String, String> filteredOut;

    private static void link() throws IOException {
        if (linking == null) {
            RModel other = AstBuilder.buildFromString(MODEL_OTHER, "seat14-other.rosetta");
            RModel dep = AstBuilder.buildFromString(MODEL_DEP, "seat14-dep.rosetta");
            RModel main = AstBuilder.buildFromString(MODEL_MAIN, "seat14.rosetta");
            other.setVersion("0.0.0.test");
            dep.setVersion("0.0.0.test");
            main.setVersion("0.0.0.test");
            List<RModel> models = new ArrayList<>();
            models.add(other); // the decoy registered FIRST (b5)
            models.add(dep);
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

    private static String filtered(String fileName) throws IOException {
        if (filteredOut == null) {
            filteredOut = render(m -> "census.seat14".equals(m.namespace()));
        }
        return lookup(filteredOut, fileName);
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
            throw new AssertionError("[DeclaredThenArgTypeSeatTest] builtins parse failures: "
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
