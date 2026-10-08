package com.regnosys.rosetta.generator.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.GenerationException;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.reports.ReportGenerator;
import com.regnosys.rosetta.generator.java.reports.RuleGenerator;
import com.regnosys.rosetta.generator.java.spi.IRGeneration;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.symbols.RWorkspace;

/**
 * PR #614, C2d retirement family 7 {@code collapse-get-suffix} — every consumer that had to know
 * whether a compiled value had COLLAPSED to a bare item (its render ending {@code .get()}) or had
 * taken the multi collapse (ending {@code .getMulti()}) moves off the rendered suffix and onto a
 * typed fact.
 *
 * <p><b>The family, verbatim from the triage</b>
 * (the development audit "2026-08-29-evidence-ledger-triage" §3, family {@code collapse-get-suffix}):
 * TWENTY-THREE ledger rows / 23 occurrences over EIGHT files — 22 RETIRE-AFTER-CENSUS plus one
 * JUSTIFIED-KEPT-CANDIDATE.
 *
 * <p><b>The retirement channels.</b>
 * <ul>
 *   <li><b>The shared arbiter</b> {@code HandlerHelper.bareOnlyElementCollapse(root, compiled)} —
 *       the family's MASTER BIJECTION, measured with zero counterexamples on all three walks: the
 *       render ends {@code .get()} &hArr; the expression root is
 *       {@code RListOpExpr(op == ONLY_ELEMENT)} AND {@code getExpressionType() == null}. Read by
 *       the deep-then decl re-wrap and the shared {@code isBareGetCollapse} block/ladder helper
 *       ({@code CollectionHandler}), the filter-predicate-arm comparand hoist
 *       ({@code ComparisonHandler}), the ITE nav-collapse arm ({@code ControlFlowHandler}), four
 *       conversion seats and the C5 meta-deref hoist ({@code ConversionHandler}), the list-literal
 *       element deref ({@code LiteralHandler}) and two arm/rung wraps
 *       ({@code FunctionExpressionRenderer}) — one read point, one mutation lane.</li>
 *   <li><b>The {@code selfUnwrapping} KIND</b>
 *       ({@code JavaExpression.selfUnwrappingBareCollapse}) — the marker the #243 navGetWrap
 *       discriminator called OVERLOADED now carries which kind it is, stamped by the ONE producer
 *       that renders a transparent {@code .get()} tail ({@code CollectionHandler}'s
 *       {@code ListOp.ONLY_ELEMENT} arm). The other ten {@code selfUnwrapping} call sites — three
 *       ctor typed-builder blocks, five bare hoist sentinels, two {@code getOrDefault(<ternary>)}
 *       joins — keep the plain factory and decline as before.</li>
 *   <li><b>The evaluate-arg ARM REPORT</b>
 *       ({@code JavaExpression.evaluateArgMultiExtracted}) — every arm of the evaluate-arg unwrap
 *       pipeline that APPENDS the {@code .getMulti()} terminal stamps it, so the two elementwise
 *       wrapper-deref seats read the producer's own control flow instead of the suffix (LAW 69).
 *       Deliberately DISTINCT from #613's {@code MultiExtracted}, which names the ADD unwrap's
 *       extraction and is read by the ADD rungs.</li>
 *   <li><b>The ADD default arm</b> reads #613's {@code JavaExpression.MultiExtracted} — the witness
 *       {@code unwrapForAddAssignment}'s own fall-through already stamps one line earlier.</li>
 *   <li><b>The block ofNull witness</b> ({@code CollectionHandler.tryDeepThenHoist}) reads the block
 *       producer's recorded {@code ExpressionCompiler.LambdaJoinFacts} through
 *       {@code FunctionExpressionRenderer.blockProducerNode} — CLOSING the divergence PR #613 banked
 *       (its code-quality review, NIT-15), where this seat still scanned the render while its twin
 *       {@code renderThenExtractSet} already read the record.</li>
 * </ul>
 *
 * <p><b>FOUR ROWS ARE ADJUDICATED JUSTIFIED-KEPT</b> — the ledger's own disposition for all four.
 * THREE of them arrived as RETIRE-AFTER-CENSUS and were MOVED to JUSTIFIED-KEPT by the census's own
 * refutation of their named channels; each keeps its text read, disclosed at the point it sits, for
 * a STRUCTURAL reason, not for want of trying:
 * <ul>
 *   <li>{@code ReferenceHandler.unwrapForEvaluateArg}'s {@code source.endsWith(".get()")} re-wrap:
 *       the structural-unwrap branch at the top of that method returns first, so every arrival there
 *       provably carries NO unwrap marker — and the family's producer kind carries one by contract.
 *       Its other named channel is refuted too (the type is null at all 32 fires; the seat's
 *       arrivals are 118,365 / 121,717 / 79,942 — default-route, IR-route, optimised).</li>
 *   <li>{@code FunctionExpressionRenderer}'s two alias-consumer arms
 *       ({@code consumerStr.endsWith(".get()")}): the row-named {@code unwrapToBuilder} channel is
 *       EMPTY at all 1,176 / 1,176 / 1,100 arrivals (default-route, IR-route, optimised) on both
 *       polarities, and each arm's own sibling conjunct
 *       {@code consumer.unwrapToBuilder().isEmpty()} would exclude the producer kind by
 *       construction. No AST shape was logged there either.</li>
 * </ul>
 * The FOURTH arrived as the family's one JUSTIFIED-KEPT-CANDIDATE and stays one, with its
 * measurement: {@code CollectionHandler.isBooleanNavFilterPredicate}'s idempotence belt fired at
 * ZERO of 10,097 arrivals on each of the three walks — 30,291 in all — with no compiled type and no unwrap witness at ANY of them — there
 * is no channel to move it to, and it guards a double-{@code .get()} that is not valid Java
 * (LAW 74).
 *
 * <p><b>The census</b> ({@code target/seat614-instruments/c2c-f7-verdicts.md}, local — env-gated
 * probes at 22 injection points over three walks: the default-route D11 275/0F with 898,739 probe
 * lines, the IR-route D11 275/0F with 694,522, and the optimised suite 12/0F with 587,624; applied,
 * never committed, reverted before any commit). Per-seat figures are transcribed into each seat's
 * own comment rather than restated here.
 *
 * <p><b>The fixtures (Part A)</b> put one shape at each seat a REDUCED model reaches AND can move:
 * a2 the {@code selfUnwrapping} kind at the comparison-operand seat, a4 the deep-then ITE
 * nav-collapse arm, a5 the list-literal meta-collapse element deref, a6 the bare distinct-collapse
 * to-enum receiver, a7 the ADD {@code default} arm's guard.
 *
 * <p><b>The lane map</b> ({@code target/seat614-instruments/lanes-f7.py}, one lane per channel's ONE
 * read point): A the shared arbiter {@code HandlerHelper.bareOnlyElementCollapse}; B the
 * {@code selfUnwrapping} KIND read at {@code isSelfUnwrappingGetOperand}; C the evaluate-arg ARM
 * REPORT ({@code JavaExpression.evaluateArgMultiExtracted} stops stamping); D the ADD arm's
 * {@code MultiExtracted} witness; E the deep-then decl's recorded-{@code LambdaJoinFacts} read;
 * F {@code ConversionHandler.isBareCollapseConversionArg}, the sibling AST predicate S09 now
 * consults alone; G the S04 max/min comparator-key seat's typed
 * {@code typeNull AND FIELD_WITH_META} gate — added at the spec-compliance review's SF-2, because
 * S04 was the one swapped seat with no lane and its only witness (corpus_c1_05
 * {@code CommodityBasisLeg1}) moved under none of A-F; and H the S19/S20 distinct-collapse ITE-arm
 * wrap's arm-builder TYPE test — added at the code-quality review's SF-3, which removed that seat's
 * arbiter call (it cross-paired the then-body node with the enclosing {@code RThenExpr}'s builder)
 * and left the type test as the seat's surviving discriminating fact, so lane A no longer reaches
 * it. Lane sensitivity is NOT asserted here — the MEASURED failing set per lane is transcribed into
 * the seat's CHANGELOG entry from the lane logs (LAW 82).
 *
 * <p><b>The S19/S20 witness, after SF-3.</b> No Part-A fixture reaches that seat, so its witness is
 * its own charter carrier: corpus_c1_13, drr {@code NotionalQuantityLeg1Rule}, whose golden carries
 * {@code ifThenElseResult = MapperS.of(distinct(thenArg6).get());} — the wrap present EXACTLY once.
 * The census found the seat's wrap firing at ZERO of its 11 reached arrivals (every one is already
 * {@code MapperS.of}-prefixed and TYPED, so the type test declines), which is why the lock's job is
 * to catch a DOUBLE wrap: under lane H the type test is inverted, those typed arms stop declining
 * and the arm is wrapped a second time. control0 pins the golden's single-wrap form.
 *
 * <p><b>Fixtures were written and DROPPED</b> because they failed under NO lane, which is
 * information-free (LAW 72) — each is disclosed at the point it would have sat: a1 for the S01
 * deep-then decl re-wrap and a3 for the existence-operand consumer of the #243 discriminator (both
 * rendered their target form for ANOTHER seat's reason, measured), and FIVE shapes tried for S03's
 * recorded-{@code LambdaJoinFacts} read — see the next paragraph, which is a finding, not an excuse.
 *
 * <p><b>THE S03 READ HAS NO WITNESS THAT CAN FAIL, AND THE REASON IS STRUCTURAL AND MEASURED.</b>
 * Under lane E (that read forced null) every one of this suite's tests passes — including the row's
 * own charter carriers, the two {@code Beneficiary…IdentifierTypeIndicatorRule} locks — and so does
 * the whole lane-E selection (390 run / 0F, optimised 12/0F — the citable lane2 run at the
 * code-quality-fixes head, {@code logs/lane2.status}; the first run at the seat commit measured
 * the same zero at 389 run, before this suite gained its S19 lock). Five reduced shapes were then built
 * specifically to arm it, and the measurement explains why none can:
 * <ul>
 *   <li>For the witness to FIRE at all, the block's recorded typed-empty element must EQUAL the
 *       meta-blind inferred element — the block must join BARE.</li>
 *   <li>For lane E to MOVE anything, {@code NavigationHandler.levelMetaWrapper} must then recover a
 *       wrapper at that same level (its rule 4 walker; rule 5 declines, because a bare typed empty
 *       is not an {@code RJavaWithMetaValue}).</li>
 *   <li>Those two are COUPLED at every shape a reduced model can reach. The block's typed empty is
 *       spelled at the level's COMPILED element, so any arm the walker can type as meta — a meta nav
 *       (walker case (a)), a disguised 2-name chain (case (f)), a bare or with-args RULE reference
 *       (case (c)) — ALSO makes that compiled element the wrapper, and the block then spells
 *       {@code return MapperS.<FieldWithMetaString>ofNull();} over a
 *       {@code MapperC<FieldWithMetaString>} decl, so the witness never fires. All three were built
 *       and rendered exactly that (a meta-nav arm, a disguised {@code ut -> ccy} arm, and a bare
 *       rule-ref arm, the last also under a {@code then only-element} tail).</li>
 *   <li>The ONE shape that DECOUPLES them is the corpus's own, and it was built too: an elseless
 *       conditional whose then-arm is a WITH-ARGS FUNCTION call with a DECLARED BARE output over a
 *       meta leaf (the {@code ExtractPartyFromRelatedPartyByRole} shape of the two
 *       {@code Beneficiary…} carriers — {@code standards-iosco-cde-version1-party-rule.rosetta:67}
 *       IN CELL drr 6.34.1, the cell this suite locks; the same rule starts at {@code :54} in
 *       drr 7.0.0-7.3.0 and over a different from-type there, so the citation is cell-pinned). It renders {@code final MapperC<String> thenArg1} and
 *       {@code return MapperS.<String>ofNull();} — the witness FIRES, exactly as on corpus — and it
 *       STILL does not move under lane E, measured. The walker has no case for a function-call arm,
 *       deliberately: that is {@code levelMetaWrapper}'s own rule 2 ("an invocation level declines —
 *       the render is the truth: an unwrapped {@code MapperS.of(rule.evaluate(…))} IS bare even where
 *       the rosetta output is meta") applied one level down, and {@code recoverMetaFromExpr}'s
 *       case (c) resolves an {@code RRule} symbol only.</li>
 * </ul>
 * So the decoupling this witness needs in order to be observable is precisely the decoupling the
 * walker is designed never to make. That is corroborated corpus-wide by the PR #613 census (c6b, the
 * S1 {@code gate=ofNull} lines): the walker answered NULL at EVERY corpus {@code ofNull} arrival —
 * 222 of 222 on each D11 walk and 108 of 108 on the optimised walk.
 * The gate is therefore redundant at this corpus — it suppresses a recovery that declines anyway —
 * which is also why the census's 40-arrival difference class (where the record says
 * typed-empty-Boolean and the retired text scan under-matched) is byte-neutral. Lane E's mutation is
 * MEASURED-INERT, not unmeasured; the corpus-wide byte statement is the o1 oracle's, not this
 * suite's.
 *
 * <p><b>The corpus carriers (Part B)</b> — cell A is drr 6.34.1, the cell the sibling seat suites
 * lock, and cell B is cdm 6.20.6 for the one carrier drr does not have. Whole-file byte locks
 * (corpus_c1), golden-token controls (control0 / control0b — golden is the oracle for the form each
 * seat decides) and the LAW 77 IR-route parity control (control2, skipping without the IR provider
 * as its siblings do; cell A only — cell B's route parity is witnessed corpus-wide by the D11
 * matrix's ROUTE ROW DIFF NONE, and a second in-suite IR-route cell generation would duplicate that
 * gate at real cost).
 */
class CollapseGetSuffixSeatTest {

    private static final Path REPO_ROOT =
            Path.of(System.getProperty("user.dir")).resolve("..").normalize();

    private static final List<Path> BUILTINS_SEARCH_ROOTS = List.of(
            REPO_ROOT.resolve("test-corpus/rune-dsl-builtins/rune-runtime/src/main/resources/model"),
            REPO_ROOT.resolve("rune-dsl/rune-runtime/src/main/resources/model"),
            REPO_ROOT.resolve("rune-runtime/src/main/resources/model"));

    static boolean builtinsAvailable() {
        return BUILTINS_SEARCH_ROOTS.stream().anyMatch(Files::isDirectory);
    }

    /** The carrier cell — drr 6.34.1 (the cell the sibling seat suites lock the same files in). */
    private static final Path CELL_A_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path GOLDEN_A = CELL_A_ROOT.resolve("rosetta-source/src/generated/java");

    static boolean cellAAvailable() {
        return Files.isDirectory(GOLDEN_A)
                && Files.isDirectory(CELL_A_ROOT.resolve("rosetta-source/src/main/rosetta"));
    }

    /** The second carrier cell — cdm 6.20.6, home of the S21 alias-rung carrier drr lacks. */
    private static final Path CELL_B_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path GOLDEN_B = CELL_B_ROOT.resolve("rosetta-source/src/generated/java");

    static boolean cellBAvailable() {
        return Files.isDirectory(GOLDEN_B)
                && Files.isDirectory(CELL_B_ROOT.resolve("rosetta-source/src/main/rosetta"));
    }

    static boolean irProviderOnClasspath() {
        try {
            Class.forName("com.regnosys.rosetta.generator.java.ir.IRGenerationProviderImpl");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    static boolean cellAAndIrProviderAvailable() {
        return cellAAvailable() && irProviderOnClasspath();
    }

    /** S06 — the #394 filter-predicate-arm comparand collapse hoist (the row's own carrier). */
    private static final String CARRIER_COMPARAND_HOIST =
            "drr/regulation/common/trade/party/reports/PTRRIDRule.java";
    /** S07 — the #388 ITE nav-collapse arm re-wrap (the row's own carrier). */
    private static final String CARRIER_ITE_NAV_COLLAPSE =
            "drr/regulation/csa/rewrite/trade/reports/"
            + "CountryAndProvinceOrTerritoryOfIndividualRule.java";
    /** S11 — the C5 collapsed-meta deref hoist (the row's own carrier). */
    private static final String CARRIER_C5_DEREF =
            "drr/regulation/common/trade/execution/reports/BookingLocationRule.java";
    /** S14 — the list-literal element meta-collapse deref (the row's own carrier). */
    private static final String CARRIER_LIST_LITERAL =
            "drr/regulation/common/trade/link/functions/SortIdentifiers.java";
    /** S10 + S12 — the to-string collapse deref hoist and the list-literal collapse re-wrap. */
    private static final String CARRIER_TOSTRING_COLLAPSE =
            "drr/regulation/common/trade/underlier/reports/NameOfTheUnderlyingIndexRule.java";
    /** S04 — the max/min comparator-key collapsed FIELD_WITH_META deref (the row's own carrier). */
    private static final String CARRIER_MAXMIN_KEY =
            "drr/regulation/csa/rewrite/trade/functions/CommodityBasisLeg1.java";
    /** S08 + S10 — the deeper-receiver collapse class the census found (123 of the 149 fires). */
    private static final String CARRIER_DEEP_COLLAPSE =
            "drr/regulation/csa/rewrite/trade/functions/InterestRateLeg1ReturnSwap.java";
    /** S01 + S09 — the #371 nav-collapse decl re-wrap and the to-enum conversion re-wrap. */
    private static final String CARRIER_LOAD_TYPE =
            "drr/regulation/fca/ukemir/refit/trade/reports/LoadTypeRule.java";
    /** S18 — the ADD default arm's {@code .getMulti().isEmpty()} guard (the row's own carrier). */
    private static final String CARRIER_ADD_DEFAULT =
            "drr/standards/iosco/cde/base/price/functions/Contract_Price_Monetary.java";
    /** S16 — the implicit evaluate-arg elementwise wrapper deref (the row's own carrier). */
    private static final String CARRIER_EVAL_ARG_MULTI =
            "drr/regulation/common/functions/GetUnderlierProductIdentifier.java";
    /** S19/S20 — the distinct-collapse ITE-arm wrap (the two rows' own carrier). */
    private static final String CARRIER_DISTINCT_ITE_ARM =
            "drr/regulation/common/trade/quantity/reports/NotionalQuantityLeg1Rule.java";
    /** S03 — the deep-then decl's block ofNull witness (the row's own carrier). */
    private static final String CARRIER_BLOCK_OF_NULL =
            "drr/standards/iosco/cde/version1/party/reports/"
            + "Beneficiary1IdentifierTypeIndicatorRule.java";
    /** S03 — the second half of the row's carrier pair. */
    private static final String CARRIER_BLOCK_OF_NULL_2 =
            "drr/standards/iosco/cde/version1/party/reports/"
            + "Beneficiary2IdentifierTypeIndicatorRule.java";

    private static final List<String> CARRIERS = List.of(
            CARRIER_COMPARAND_HOIST, CARRIER_C5_DEREF, CARRIER_LIST_LITERAL,
            CARRIER_TOSTRING_COLLAPSE, CARRIER_MAXMIN_KEY, CARRIER_DEEP_COLLAPSE,
            CARRIER_LOAD_TYPE, CARRIER_ADD_DEFAULT, CARRIER_EVAL_ARG_MULTI,
            CARRIER_ITE_NAV_COLLAPSE, CARRIER_BLOCK_OF_NULL, CARRIER_BLOCK_OF_NULL_2,
            CARRIER_DISTINCT_ITE_ARM);

    /**
     * S21 — the alias return-ladder rung's {@code MapperC.of(Collections.singletonList(<x>.get()))}
     * wrap (cell B; the row's own carrier, and the file that closes the census's disclosed gap —
     * the probe did not log the rung's list OP).
     */
    private static final String CARRIER_SINGLE_RUNG_B =
            "cdm/event/common/functions/Create_QuantityChange.java";

    private static final List<String> CARRIERS_B = List.of(CARRIER_SINGLE_RUNG_B);

    // =========================================================================
    // Part A — the fixtures, one per seat a reduced model reaches
    // =========================================================================

    private static final String MODEL = """
            namespace census.seat614f7
            version "1.0.0"

            enum Colour:
                RED
                BLUE

            type Leg:
                mark string (0..1)
                colour Colour (0..1)
                id string (0..1)
                    [metadata scheme]
                ids string (0..*)
                    [metadata scheme]
                plainId string (0..1)
                plainIds string (0..*)

            type Root:
                leg Leg (0..1)
                legs Leg (0..*)

            func A2CollapseComparand: <"a2 - S13: the selfUnwrapping KIND - a bare item-collapse operand re-wraps MapperS.of(...) at the comparison seat">
                inputs:
                    rt Root (0..1)
                output:
                    r boolean (1..1)
                set r:
                    rt -> legs -> plainId only-element = "x"

            reporting rule A4DeepThenIteNavCollapse from Root: <"a4 - S07: a DEEP-THEN ite hoist whose then-arm is a NAV only-element collapse - the Mapper-typed thenArg slot re-wraps MapperS.of(...)">
                extract leg
                then extract if item -> mark exists
                    then item -> plainIds only-element
                    else item -> plainId

            func A5ListLiteralMetaCollapse: <"a5 - S14: a list-literal element that collapses to a META wrapper item and derefs through the shared hoist">
                inputs:
                    rt Root (0..1)
                output:
                    r string (0..*)
                set r:
                    [ rt -> leg -> ids only-element, rt -> leg -> plainId ]

            func A6DistinctCollapseToEnum: <"a6 - S09: a to-enum whose argument is the bare distinct/only-element collapse - the conversion receiver re-wraps">
                inputs:
                    rt Root (0..1)
                output:
                    r Colour (0..1)
                set r:
                    rt -> legs -> mark
                        then distinct only-element to-enum Colour

            func A7AddDefaultArm: <"a7 - S18: the ADD `default` arm whose LEFT guard must not double the .getMulti() the unwrap just appended">
                inputs:
                    rt Root (0..1)
                output:
                    r string (0..*)
                add r:
                    if rt -> leg -> mark exists
                    then rt -> legs -> plainIds default rt -> leg -> plainIds
                    else rt -> leg -> plainIds

            """;

    /*
     * There is deliberately NO a1 for S01 (the deep-then decl's collapse re-wrap). The reduced
     * `rt -> legs then only-element then extract item -> mark` DOES render the golden
     * `MapperS.of(thenArg0.get())` form, but MEASUREMENT showed it renders it for another reason:
     * under lane A (the shared arbiter answering false everywhere) the fixture still passed, so the
     * wrap it asserts comes from the deep-then CONSUMER arm, not from this seat — a fixture that
     * passes for another reason is information-free (LAW 72), so it was dropped rather than kept.
     * S01's witness is its own charter carrier, corpus_c1_07's LoadTypeRule (the #371 nav-collapse
     * decl re-wrap trio), which DOES fail under lane A.
     */

    /** a2 — S13: the collapsed comparison operand takes the {@code MapperS.of(...)} re-wrap. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_collapseComparandReWraps() throws IOException {
        String code = codeOnly(fn("A2CollapseComparand.java"));
        assertContains(code, "areEqual(MapperS.of(");
        assertContains(code, ".get())");
    }

    /*
     * There is deliberately NO a3 for the SECOND consumer of the #243 discriminator (the existence
     * operand seat). The reduced `rt -> legs -> plainId only-element exists` renders the wrapped
     * operand, but under lane B (the discriminator answering false everywhere) it still passed —
     * the wrap there is another seat's, so the fixture carries no information about this channel
     * (LAW 72) and was dropped. The discriminator's witnesses are a2 at the comparison-operand seat
     * and corpus_c1_08's Contract_Price_Monetary, both of which DO fail under lane B.
     */

    /** a4 — S07: the nav-collapse ITE arm assigns the re-wrapped Mapper form. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a4_iteNavCollapseArmReWraps() throws IOException {
        String code = codeOnly(rule("A4DeepThenIteNavCollapseRule.java"));
        assertContains(code, "= MapperS.of(");
        assertContains(code, ".get());");
    }

    /** a5 — S14: the collapsed meta element hoists and re-presents its value guarded. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a5_listLiteralMetaCollapseDerefs() throws IOException {
        String code = fn("A5ListLiteralMetaCollapse.java");
        assertContains(code, "final FieldWithMetaString fieldWithMetaString");
        assertContains(code, "MapperS.<String>ofNull()");
    }

    /** a6 — S09: the bare distinct-collapse conversion receiver re-wraps before the checkedMap. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a6_distinctCollapseToEnumReWraps() throws IOException {
        String code = codeOnly(fn("A6DistinctCollapseToEnum.java"));
        assertContains(code, "MapperS.of(distinct(");
        assertContains(code, ".get()).checkedMap(");
    }

    /** a7 — S18: the ADD default arm's guard appends {@code .isEmpty()}, never a second suffix. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a7_addDefaultArmGuardDoesNotDoubleTheSuffix() throws IOException {
        String code = codeOnly(fn("A7AddDefaultArm.java"));
        assertContains(code, ".getMulti().isEmpty()) {");
        assertAbsent(code, ".getMulti().getMulti()");
    }

    // =========================================================================
    // Part B — the corpus carriers (cell A drr 6.34.1; cell B cdm 6.20.6)
    // =========================================================================

    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_01_ptrrIdRuleByteIdentical() throws IOException {
        lock(CARRIER_COMPARAND_HOIST);
    }

    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_02_bookingLocationRuleByteIdentical() throws IOException {
        lock(CARRIER_C5_DEREF);
    }

    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_03_sortIdentifiersByteIdentical() throws IOException {
        lock(CARRIER_LIST_LITERAL);
    }

    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_04_nameOfTheUnderlyingIndexRuleByteIdentical() throws IOException {
        lock(CARRIER_TOSTRING_COLLAPSE);
    }

    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_05_commodityBasisLeg1ByteIdentical() throws IOException {
        lock(CARRIER_MAXMIN_KEY);
    }

    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_06_interestRateLeg1ReturnSwapByteIdentical() throws IOException {
        lock(CARRIER_DEEP_COLLAPSE);
    }

    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_07_loadTypeRuleByteIdentical() throws IOException {
        lock(CARRIER_LOAD_TYPE);
    }

    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_08_contractPriceMonetaryByteIdentical() throws IOException {
        lock(CARRIER_ADD_DEFAULT);
    }

    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_09_getUnderlierProductIdentifierByteIdentical() throws IOException {
        lock(CARRIER_EVAL_ARG_MULTI);
    }

    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_10_countryAndProvinceOrTerritoryOfIndividualByteIdentical() throws IOException {
        lock(CARRIER_ITE_NAV_COLLAPSE);
    }

    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_11_beneficiary1IdentifierTypeIndicatorByteIdentical() throws IOException {
        lock(CARRIER_BLOCK_OF_NULL);
    }

    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_12_beneficiary2IdentifierTypeIndicatorByteIdentical() throws IOException {
        lock(CARRIER_BLOCK_OF_NULL_2);
    }

    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_13_notionalQuantityLeg1ByteIdentical() throws IOException {
        lock(CARRIER_DISTINCT_ITE_ARM);
    }

    @Test
    @EnabledIf("cellBAvailable")
    void corpus_c1_14_createQuantityChangeByteIdentical() throws IOException {
        lockB(CARRIER_SINGLE_RUNG_B);
    }

    /**
     * control0 — golden is the oracle: each cell-A carrier still carries the form this family's seat
     * decides. If one of these ever stops holding, the carrier stopped being this family's carrier
     * and every claim above is restated, not patched.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control0_goldensCarryTheDecidedForms() throws IOException {
        assertTrue(golden(CARRIER_COMPARAND_HOIST)
                        .contains("final ReferenceWithMetaParty referenceWithMetaParty1 = distinct("),
                "S06: the golden's filter-predicate arm must hoist the collapsed REFERENCE_WITH_META"
                + " comparand");
        assertTrue(golden(CARRIER_ITE_NAV_COLLAPSE)
                        .contains("thenArg = MapperS.of(MapperS.of(naturalPersonBuyerOrSeller"),
                "S07: the golden's nav-collapse ITE arm must re-wrap the collapse MapperS.of(...)");
        assertTrue(golden(CARRIER_C5_DEREF).contains("final FieldWithMetaString fieldWithMetaString"),
                "S11: the golden's C5 seat must hoist the collapsed FieldWithMetaString");
        assertTrue(golden(CARRIER_LIST_LITERAL).contains("final FieldWithMetaString fieldWithMetaString0"),
                "S14: the golden's list-literal elements must hoist their collapsed wrappers");
        assertTrue(golden(CARRIER_MAXMIN_KEY).contains("final FieldWithMetaString fieldWithMetaString"),
                "S04: the golden's max/min comparator key must hoist its collapsed wrapper");
        assertTrue(golden(CARRIER_ADD_DEFAULT).contains(".getMulti().isEmpty()) {"),
                "S18: the golden's ADD default arm must guard on a SINGLE .getMulti()");
        assertTrue(golden(CARRIER_EVAL_ARG_MULTI).contains("\"Type coercion\"")
                        && golden(CARRIER_EVAL_ARG_MULTI).contains(".getMulti())"),
                "S15/S16: the golden's evaluate-arg must deref elementwise before .getMulti()");
        assertTrue(golden(CARRIER_LOAD_TYPE)
                        .contains("final MapperS<ForwardPayout> thenArg3 = MapperS.of("),
                "S01: the golden's nav-collapse then-level decl must carry the MapperS.of re-wrap");
        assertTrue(golden(CARRIER_LOAD_TYPE)
                        .contains("MapperS.of(distinct(thenArg2).get()).checkedMap(\"to-enum\""),
                "S09: the golden's to-enum receiver must re-wrap the bare distinct-collapse before"
                + " the checkedMap");
        assertTrue(golden(CARRIER_DISTINCT_ITE_ARM)
                        .contains("ifThenElseResult = MapperS.of(distinct(thenArg6).get());"),
                "S19/S20: the golden's distinct-collapse ITE arm must carry the MapperS.of wrap"
                + " EXACTLY once — the seat's guard is what keeps it from wrapping a second time");
        assertTrue(golden(CARRIER_BLOCK_OF_NULL).contains("final MapperC<Party> thenArg")
                        && golden(CARRIER_BLOCK_OF_NULL).contains("ofNull()"),
                "S03: the golden's deep-then level must declare the BARE element its own block's"
                + " typed-empty terminal returns");
    }

    /** control0b — the cell-B carrier still carries the form its seat decides (S21). */
    @Test
    @EnabledIf("cellBAvailable")
    void corpus_control0b_cellBGoldenCarriesTheDecidedForm() throws IOException {
        assertTrue(Files.readString(GOLDEN_B.resolve(CARRIER_SINGLE_RUNG_B))
                        .contains("MapperC.of(Collections.singletonList("),
                "S21: the golden's single-collapsed alias rung must take the"
                + " MapperC.of(Collections.singletonList(...)) item-to-list coercion");
    }

    /**
     * control2 — LAW 77 route parity: every seat in this family lives in the SHARED generator, which
     * the IR route runs too (and the IR route's own nested only-element emitter renders the same
     * collapse), so each carrier must render identically under {@code -Pir-on}. Skips (recorded)
     * without the IR provider on the classpath.
     */
    @Test
    @EnabledIf("cellAAndIrProviderAvailable")
    void corpus_control2_irRouteMatchesLegacyForEveryCarrier() throws IOException {
        assertNotNull(drrAOutput, "drr 6.34.1 legacy-route generation did not run");
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", CELL_A_ROOT), new ArrayList<>());
        for (String carrier : CARRIERS) {
            assertEquals(drrAOutput.get(carrier), irOut.get(carrier), "route divergence: " + carrier);
        }
    }

    private static String golden(String carrier) throws IOException {
        return Files.readString(GOLDEN_A.resolve(carrier));
    }

    private static void lock(String carrier) throws IOException {
        assertNotNull(drrAOutput, "drr 6.34.1 generation did not run — corpus unavailable?");
        List<String> own = drrAGenErrors.stream().filter(e -> e.contains(carrier)).toList();
        assertTrue(own.isEmpty(),
                "the generator reported errors for the locked file " + carrier + ": " + own);
        String generated = drrAOutput.get(carrier);
        assertNotNull(generated, "not generated in drr 6.34.1: " + carrier);
        Path goldenPath = GOLDEN_A.resolve(carrier);
        assertTrue(Files.isRegularFile(goldenPath), "golden missing: " + goldenPath);
        assertEquals(normalize(Files.readString(goldenPath)), normalize(generated),
                "generated drr 6.34.1 output must byte-match golden (newline-normalized) for "
                + carrier + " — PR #614 family 7: the collapse-suffix reads move onto typed facts"
                + " with no byte change.");
    }

    /** The cell-B (cdm 6.20.6) twin of {@link #lock} — the S21 carrier lock. */
    private static void lockB(String carrier) throws IOException {
        assertNotNull(cdmBOutput, "cdm 6.20.6 generation did not run — corpus unavailable?");
        List<String> own = cdmBGenErrors.stream().filter(e -> e.contains(carrier)).toList();
        assertTrue(own.isEmpty(),
                "the generator reported errors for the locked file " + carrier + ": " + own);
        String generated = cdmBOutput.get(carrier);
        assertNotNull(generated, "not generated in cdm 6.20.6: " + carrier);
        Path goldenPath = GOLDEN_B.resolve(carrier);
        assertTrue(Files.isRegularFile(goldenPath), "golden missing: " + goldenPath);
        assertEquals(normalize(Files.readString(goldenPath)), normalize(generated),
                "generated cdm 6.20.6 output must byte-match golden (newline-normalized) for "
                + carrier + " — PR #614 family 7: the collapse-suffix reads move onto typed facts"
                + " with no byte change.");
    }

    // =========================================================================
    // Fixture harness (the MetaWrapperRecoverySeatTest renderer)
    // =========================================================================

    private record Render(Map<String, String> output, List<String> errors) {}

    private static Render rendered;

    private static Render render() throws IOException {
        if (rendered != null) {
            return rendered;
        }
        RModel main = AstBuilder.buildFromString(MODEL, "seat614f7.rosetta");
        main.setVersion("0.0.0.test");
        List<RModel> models = new ArrayList<>();
        models.add(main);
        models.addAll(loadBuiltinsOnly());
        RWorkspace workspace = RWorkspace.build(models).workspace();
        GeneratorModel gm = new GeneratorModel(workspace,
                m -> "census.seat614f7".equals(m.namespace()));
        JavaTypeUtil typeUtil = new JavaTypeUtil();
        JavaTypeTranslator tt = new JavaTypeTranslator(typeUtil);
        FunctionGenerator fg = new FunctionGenerator(gm, tt, typeUtil);
        RuleGenerator ruleGen = new RuleGenerator(gm, tt, fg);
        Map<String, String> out = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        ruleGen.generateClasses(main, "1.0", out)
                .forEach(e -> errors.add(e.getTargetPath() + " - " + e));
        fg.generateWithErrors(out)
                .forEach(e -> errors.add(e.getTargetPath() + " - " + e));
        rendered = new Render(out, errors);
        return rendered;
    }

    /** Functions land under {@code .../functions/}. */
    private static String fn(String fileName) throws IOException {
        return generated("functions/" + fileName, fileName);
    }

    /** Reporting rules land under {@code .../reports/}. */
    private static String rule(String fileName) throws IOException {
        return generated("reports/" + fileName, fileName);
    }

    private static String generated(String suffix, String fileName) throws IOException {
        Render r = render();
        List<String> own = r.errors().stream().filter(e -> e.contains(fileName)).toList();
        assertTrue(own.isEmpty(),
                "the generator reported errors for " + fileName + " (a broken fixture must fail"
                + " loudly, not skip): " + own);
        String out = r.output().entrySet().stream()
                .filter(e -> e.getKey().endsWith("/" + suffix))
                .map(Map.Entry::getValue)
                .findFirst().orElse(null);
        assertNotNull(out, "not generated: " + fileName + " (have: " + r.output().keySet() + ")");
        return out;
    }

    private static void assertContains(String code, String needle) {
        assertTrue(code.contains(needle), "expected <" + needle + "> in:\n" + code);
    }

    private static void assertAbsent(String code, String needle) {
        assertTrue(!code.contains(needle), "did NOT expect <" + needle + "> in:\n" + code);
    }

    /**
     * Strip line and block comments plus string literals so a javadoc, a label or a
     * {@code "getId"} literal never counts as code.
     */
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
                i = j + 1;
            } else if (ch == '/' && i + 1 < n && java.charAt(i + 1) == '/') {
                while (i < n && java.charAt(i) != '\n') {
                    i++;
                }
            } else if (ch == '/' && i + 1 < n && java.charAt(i + 1) == '*') {
                int e = java.indexOf("*/", i + 2);
                i = e < 0 ? n : e + 2;
            } else {
                sb.append(ch);
                i++;
            }
        }
        return sb.toString();
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n");
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
                        failures.add(p + " - " + e);
                    }
                });
        if (!failures.isEmpty()) {
            throw new AssertionError("[CollapseGetSuffixSeatTest] builtins parse failures: "
                    + String.join("; ", failures));
        }
        return models;
    }

    // =========================================================================
    // Corpus harness (the sibling seat suites' cell generator, verbatim)
    // =========================================================================

    private static Map<String, String> drrAOutput;
    private static List<String> drrAGenErrors;
    private static Map<String, String> cdmBOutput;
    private static List<String> cdmBGenErrors;

    @BeforeAll
    static void generateCells() throws IOException {
        if (cellAAvailable()) {
            List<String> errs = new ArrayList<>();
            drrAOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", CELL_A_ROOT), errs);
            drrAGenErrors = errs;
        }
        if (cellBAvailable()) {
            List<String> errs = new ArrayList<>();
            cdmBOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CELL_B_ROOT), errs);
            cdmBGenErrors = errs;
        }
    }

    private static Map<String, String> generateCell(D11CorpusRegressionTest.CellSpec cell,
            List<String> errors) throws IOException {
        var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(),
                D11CorpusRegressionTest.emissionFilter(cell));
        var typeUtil = new JavaTypeUtil();
        var typeTranslator = new JavaTypeTranslator(typeUtil);
        var funcGen = new FunctionGenerator(gm, typeTranslator, typeUtil);
        var ruleGen = new RuleGenerator(gm, typeTranslator, funcGen);
        var reportGen = new ReportGenerator(gm, typeTranslator, funcGen);
        Map<String, String> output = new LinkedHashMap<>();
        for (RModel model : corpus.workspace().files()) {
            if (gm.shouldGenerate(model)) {
                String version = gm.version(model);
                collect(errors, ruleGen.generateClasses(model, version, output));
                collect(errors, reportGen.generateClasses(model, version, output));
            }
        }
        collect(errors, funcGen.generateWithErrors(output));
        return output;
    }

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
            FunctionGenerator funcGen = IRGeneration.functionGenerator(gm, typeTranslator, typeUtil);
            assertTrue(!funcGen.getClass().equals(FunctionGenerator.class),
                    "the seam must hand back the IR-route FunctionGenerator, got " + funcGen.getClass());
            var ruleGen = new RuleGenerator(gm, typeTranslator, funcGen);
            var reportGen = new ReportGenerator(gm, typeTranslator, funcGen);
            Map<String, String> output = new LinkedHashMap<>();
            for (RModel model : corpus.workspace().files()) {
                if (gm.shouldGenerate(model)) {
                    String version = gm.version(model);
                    collect(errors, ruleGen.generateClasses(model, version, output));
                    collect(errors, reportGen.generateClasses(model, version, output));
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
            errors.forEach(e -> sink.add(e.getTargetPath() + " - " + e));
        }
    }
}
