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
 * PR #615, C2d retirement family 8 {@code mapper-wrap-prefix} — every consumer that decided a
 * lowering by asking whether the Java it had just rendered STARTS with {@code MapperS.of(} /
 * {@code MapperC.} moves off the rendered prefix, or is disposed with its measurement.
 *
 * <p><b>The family, verbatim from the triage</b>
 * (the development audit "2026-08-29-evidence-ledger-triage", family {@code mapper-wrap-prefix}):
 * THIRTY-FOUR ledger rows / 40 occurrences over SEVEN files ({@code ArithmeticHandler},
 * {@code CollectionHandler}, {@code ControlFlowHandler}, {@code ConversionHandler},
 * {@code HandlerHelper}, {@code ReferenceHandler}, {@code FunctionExpressionRenderer} — the
 * spec-compliance review's SF-6 caught "eight" here) — 23 RETIRE-AFTER-CENSUS, 10
 * JUSTIFIED-KEPT-CANDIDATE, 1 DEAD. The family closes WHOLE: 14 occurrences retire, 26 are
 * adjudicated JUSTIFIED-KEPT with their figures, and SEVENTEEN verdicts MOVED against the triage
 * table (16 RETIRE-AFTER-CENSUS → JUSTIFIED-KEPT, 1 JUSTIFIED-KEPT-CANDIDATE → RETIRE).
 *
 * <p><b>THE MASTER FINDING — and the reason only six occurrences move onto a channel.</b> At this
 * head the rendered {@code Mapper…} prefix and the structural wrap marker
 * ({@code JavaExpression.unwrapToBuilder()}) are <b>NOT in bijection</b>, and the failure is
 * two-sided with two nameable mechanisms:
 * <ul>
 *   <li><b>The UNDER-fire</b> — the text says "already wrapped", the marker says EMPTY. Produced by
 *       the ~140 raw-string wrap sites that emit {@code MapperS.of(} / {@code MapperC.<W>of(}
 *       through {@code JavaExpression.from(code, null, refs, …)} and therefore carry neither marker
 *       nor type. Measured at nearly every seat (9,032 of one seat's 9,425 disagreements; 23,405 of
 *       another's 23,435).</li>
 *   <li><b>The INVERSION</b> — the marker is PRESENT and the render is not a wrap. Produced by the
 *       identity kinds {@code JavaExpression.selfUnwrapping} (a ctor block arm renders its builder
 *       chain verbatim, tail {@code .build()}) and {@code JavaExpression$SelfUnwrappingBareCollapse}
 *       (#614, renders {@code <prev>.get()}).</li>
 * </ul>
 * A swap of any {@code startsWith("Mapper…")} read to {@code unwrapToBuilder().isPresent()} would
 * have to exclude the identity kinds AND cover the raw producers simultaneously. Neither holds, and
 * that — not the #433 type question — is why the majority of the family stays. The seats that DO
 * retire each do so because their own enclosing code already excludes one of the two classes.
 *
 * <p><b>The retirements (six occurrences onto a channel).</b>
 * <ul>
 *   <li><b>{@code CollectionHandler.wrapSingleArmMapperCOf}'s #589 singleton-list lift</b> — the
 *       three text conjuncts of the wrap IDENTITY deleted; the gate that was already structural
 *       ({@code armExpr.unwrapToBuilder().isPresent()}) decides alone. 1,197/1,197 exact, with 484
 *       prefix-but-not-whole-wrap NEGATIVE controls all answering false. The family's one
 *       JUSTIFIED-KEPT-CANDIDATE → RETIRE movement — and its witnesses are the CELL-B locks
 *       {@code corpus_c1_10} / {@code corpus_c1_11} plus the reshaped {@code b2}, never the two
 *       cell-A goldens the suite first named (see THE S11 WITNESS below).</li>
 *   <li><b>{@code CollectionHandler.mapMethod}'s TYPED answer</b> — a new private {@code MapMethod}
 *       kind with {@code member()} (the render half) and {@code rendersSingleForm()} (the #386
 *       T386C receiver-wrap seat). A TYPE change, not a channel change: the seat still consults the
 *       chooser, so the recorded {@code gm.isMulti} misreport at the carrier is preserved.</li>
 *   <li><b>The enum-switch dispatch's {@code MapperS.of(} leg</b> — onto
 *       {@code caseExpr.unwrapToBuilder().isPresent()}, proven exact AS A SET (81/81); its
 *       {@code MapperS.<} sibling is KEPT (zero-fire, and the channel would ADD 57 fires).</li>
 *   <li><b>The #432 ternary mixed-arity arm lift ({@code ControlFlowHandler}, both arms)</b> — the
 *       two arm BUILDERS hoisted out of the {@code try}/lambda so the seats read the marker.</li>
 *   <li><b>The #260 thenArg collapse re-wrap's idempotence belt</b> — onto the in-method flag
 *       {@code wroteMapperS} (the #614 {@code collapseDerefSpliced} precedent), byte-inert at
 *       1,965/1,965 INSIDE the seat's own gate.</li>
 * </ul>
 *
 * <p><b>The deletions (eight occurrences, four removals).</b> THREE of the four transitional
 * {@code MapperS.of} string strips are dead at this corpus and are removed with their counters —
 * {@code ReferenceHandler.unwrapForEvaluateArg} (0 of 320,024), {@code unwrapSwitchArmValue} with
 * the whole {@code stripMapperOfWrap} parser (0 of 2,738; its prefix pair matched 358 times and the
 * balanced-close test rejected all 358), {@code unwrapForAddAssignment} (0 of 8,211) — plus the DEAD
 * {@code !value.startsWith("MapperC.")} conjunct at the bare-multi-fn base arm (0 of 68,087). The
 * FOURTH strip, {@code unwrapForAssignment}, is KEPT: it has 12 live arrivals, all from
 * {@code SetOperationHandler}'s {@code default} join.
 *
 * <p><b>Cross-family protections, measured rather than argued.</b> Two deleted branches sit
 * upstream of a stamp another family retired onto — {@code EvaluateArgMultiExtracted} (family 7,
 * PR #614) and {@code MultiExtracted} (family 6, PR #613). Both deletions remove a branch that
 * RETURNS, so a taken branch would have skipped the stamp; with ZERO arrivals taking either, no
 * arrival changes which stamp it reaches. No swap in this seat migrates a producer or stamps a
 * type, so the family-7 arbiter's null-type leg is untouched at all eleven seats that read it.
 *
 * <p><b>The census</b> ({@code target/seat615-instruments/c2c-f8-verdicts.md} and
 * {@code c2c-f8-seatmap.md}, local — round c8, env-gated probes at 45 tags over three walks: the
 * default-route D11 275/0F with 911,012 probe lines, the IR-route D11 275/0F with 905,989, and the
 * optimised suite 12/0F with 773,438; 2,590,439 lines parsed with 0 ignored / 0 malformed; applied,
 * never committed, reverted before any commit). A FOURTH walk (h1) over the hold-out battery
 * ({@code HoldOutGenerationTest}/{@code HoldOutByteCompareTest}/{@code HoldOutCompileGateTest}, 201
 * tests / 0F) was needed for the one seat whose only carrier is a hold-out golden, and it moved that
 * seat's verdict. Per-seat figures are transcribed into each seat's own comment rather than restated
 * here.
 *
 * <p><b>The lane map</b> (one lane per channel, at the ONE point every read goes through; a lane
 * that exists but was never RUN is not a receipt, LAW 82): <b>A</b> the #589 lift's
 * {@code armExpr.unwrapToBuilder().isPresent()} → {@code isEmpty()}; <b>B</b> the enum-switch leg's
 * new {@code caseExpr.unwrapToBuilder().isPresent()} → {@code isEmpty()}; <b>C</b> the T386C seat's
 * {@code rendersSingleForm()} → negated; <b>D</b> the two hoisted ternary arm reads →
 * {@code isEmpty()} — <b>the corpus moves NOTHING under lane D</b> (all 54 arrivals decline), so its
 * receipt is this suite's b1a/b1b fixtures and a corpus-only run of lane D is not a receipt;
 * <b>E</b> the #260 belt's {@code !wroteMapperS} → {@code wroteMapperS}; <b>F</b>
 * {@code HandlerHelper.bareOnlyElementCollapse}'s null-type leg — <b>MEASURED-INERT-EXPECTED, and
 * the expectation it was written with is REFUTED</b>: lane F was charted as an alarm ("must fail
 * LOUDLY, or the family-7 arbiter is damaged") and the lane1 run measured it GREEN (0F over the
 * whole selection AND the optimised suite). That is the correct answer, not a damaged arbiter.
 * Dropping the null-type leg cannot move a byte because the discriminating polarity — an
 * {@code ONLY_ELEMENT}-rooted arrival carrying a NON-null expression type — has ZERO arrivals at
 * the arbiter's nine call sites; that is the #614 master bijection's own corollary (the render
 * ends {@code .get()} IFF the root is {@code RListOpExpr(ONLY_ELEMENT)} AND the type is null), and
 * this seat's c8 census corroborates it at the S05 call site over the shapes that probe can see:
 * of 28,973 arrivals, every one matching any of the four {@code ONLY_ELEMENT}-rooted AST shape
 * predicates answers {@code arbiter=true} AND {@code typeNull=true} (2,052/2,052) and NONE
 * answers {@code arbiter=false} — the (shape, non-null type) cell is empty. That is ONE call site
 * and four named shapes, not a proof over all nine; the whole-lane statement is the lane1 run
 * itself. The protection lane F was
 * meant to give — that no family-8 change stamps a type at the eleven family-7 seats — is
 * therefore carried by the o1 oracle and the chain's byte gates (matrix, both rings, both D11
 * routes) plus the review's diff-level verification that no swap here populates
 * {@code getExpressionType()}. The lane is kept and RUN so the record says GREEN was expected;
 * <b>G</b> the
 * three surviving strip sites' structural reads → {@code isEmpty()}, run PER SITE, the positive
 * control that the structural branch is what carries the traffic the deletions relied on; <b>H</b>
 * the ONE surviving string strip's {@code !bare.contains("MapperS.")} → inverted, which DECIDES a
 * verdict rather than merely witnessing one (if it does not move, that branch was deletable after
 * all). Every lane run must also select {@code CollapseGetSuffixSeatTest} (family 7) and
 * {@code MetaWrapperRecoverySeatTest} (family 6). Lane sensitivity is NOT asserted here — the
 * MEASURED failing set per lane is transcribed into the seat's CHANGELOG entry from the lane logs.
 *
 * <p><b>THE CITABLE RUN — lane2 at the final code head {@code 4056e93f2}</b>
 * ({@code target/seat615-instruments/logs/lane2.status}; baseline first, then mutate → run the
 * 30-class generator selection + the optimised suite → restore per lane, clean tree both ends):
 * baseline <b>664 run / 0F / 30 skip</b> + optimised 12/0F; <b>A 11F</b> (this suite 21/3F —
 * corpus_c1_10, corpus_c1_11, b2 — + CollapseGetSuffixSeatTest 1F, WrapperLadderKeepsCollapseSeatTest 3F,
 * AliasCondLadderChoiceJoinSeatTest 2F, BareEnumComparandSeatTest 1F, CollapseCarryPrevMetaElementSeatTest 1F);
 * <b>B 3F</b> (EnumSwitchBlockWidenSeatTest 2F + this suite 1F); <b>C 1F</b> (this suite);
 * <b>D 3F</b> (this suite 2F = b1a/b1b + UpstreamFunctionGeneratorPortTest 1F =
 * canReturnDifferingCardinalitiesInIfThenElseBranches — the corpus-empty lane's fixture receipts,
 * exactly as charted); <b>E 14F</b> over six classes (this suite 3F, RuleBlockComposeTest 4F,
 * CollapseCarryPrevMetaElementSeatTest 4F, CondArmMultiMetaElementDerefSeatTest 1F,
 * AliasCondLadderChoiceJoinSeatTest 1F, AliasHeadThenChainCardinalitySeatTest 1F); <b>F 0F
 * (MEASURED-INERT-EXPECTED, the adjudication above)</b>; <b>G1 172F/16E</b> over 26 classes +
 * optimised 12/1F; <b>G2 171F/34E</b> over 20 classes + optimised 12/1F; <b>G3 75F/7E</b> over
 * 19 classes + optimised 12/1F; <b>G4 9F</b> (PathedChoiceSwitchSetSeatTest 7F,
 * AddDistributionSeatTest 1F, EnumSwitchBlockWidenSeatTest 1F); <b>H gen 664/1F —
 * FunctionExpressionRendererTest.unwrapForAssignment_legacy_string_scan_strips_mapperS_of_wrapper,
 * the #417 conditional-ref-drop assertion, the receipt that DECIDES S27b's JUSTIFIED-KEPT</b>.
 * Lane H's optimised leg was truncated by a machine restart (EXIT=0, no Tests-run line — not a
 * measurement) and RE-TAKEN at the same head on a clean tree: 12/0F ({@code logs/lane2h-opt.log};
 * the truncation and the re-take are recorded in lane2.status).
 *
 * <p><b>Fixtures written and DROPPED, per LAW 72.</b> No reduced fixture is drawn for the
 * enum-switch dispatch seat: its firing shape is the BLOCK lambda form
 * ({@code if (switchArgument == <Enum>.<CONST>) { … return …; }}), which the corpus reaches only
 * where an arm carries its own deref hoists, and a reduced model that renders the ternary form
 * instead would assert nothing about this seat. Its witness is therefore its own charter carrier,
 * the cell-B lock {@code FirstExerciseDateRule} (drr 7.0.0, iosco v1), whose BERMUDA arm splices
 * {@code return MapperC.<Date>of(…)} verbatim while its two identity-unwrap arms take the wrap —
 * the two polarities of the seat in ONE file. The same holds for the two ladder/effective-else
 * JUSTIFIED-KEPT rungs, whose witnesses are the DTCC locks.
 *
 * <p><b>The corpus carriers</b> — cell A is drr 6.34.1 (the cell the sibling seat suites lock) and
 * cell B is drr 7.0.0, for TWO seats that have no cell-A carrier at all: the enum-switch block
 * form, which does not exist in the 6.x cells (there the same rule renders an {@code exists}
 * ladder), and — the spec-compliance review's MF-1 — <b>the S11 singleton-list lift itself</b>.
 * Whole-file byte locks (corpus_c1), golden-token controls (corpus_control0 / corpus_control0b /
 * corpus_control0c — golden is the oracle for the form each seat decides, pinned to the EXACT form
 * and never to a bare {@code contains("MapperS.of(")} that any of eight occurrences would satisfy)
 * and TWO LAW-77 IR-route parity controls, one per cell — corpus_control2 (cell A, the eight
 * shared carriers) and corpus_control2b (cell B, the enum-switch carrier and the two S11 locks,
 * added at the code-quality review's SF-6 because cell A has no S11 site and the swap's route
 * parity would otherwise have rested only on the chain's whole-matrix ROUTE ROW DIFF). Both skip
 * without the IR provider as their siblings do.
 *
 * <p><b>THE S11 WITNESS, and how the suite came to be without one</b> (the spec-compliance
 * review's MF-1, recorded because the failure mode is general). S11 is the family's ONE
 * JUSTIFIED-KEPT-CANDIDATE → RETIRE movement — the swap that actually changes a decision — and it
 * was witnessed by two cell-A byte locks and two {@code corpus_control0} assertions naming a form
 * that S11 does emit. It does not emit it THERE:
 * {@code MapperC.of(Collections.singletonList(…))} is written by three different methods, and at
 * {@code GetReportTrackingNumber} and {@code JurisdictionOfCounterparty1Rule} the writer is
 * {@code ControlFlowHandler.wrapDeepThenIteArm} (S20). The two are told apart only by the statement
 * around them — S20's {@code ifThenElseResult = …;} against S11's block-lambda {@code return …;} —
 * so a {@code contains} on the expression alone cannot distinguish them, and both locks stayed
 * GREEN through lane A's inversion of S11's gate. <b>The lane run is what caught it</b>: lane A
 * moved {@code AliasCondLadderChoiceJoinSeatTest} ×2, {@code CollapseGetSuffixSeatTest} and
 * {@code WrapperLadderKeepsCollapseSeatTest} and left THIS suite 17/0F — a channel's own suite
 * sitting green under its own lane is the signature of a witness that is not on the channel.
 * <b>The census says where the carriers are</b>: all 48 gate-true S11 arrivals are drr 7.x —
 * {@code rule:IndexFactor} 24, {@code rule:BlockTradeElectionIndicator} 12,
 * {@code fn:GetBasketConstituents} 12 — and cell A contributes ZERO, so no cell-A lock could ever
 * have carried this seat. The repair is three witnesses that DO move, each measured by re-running
 * lane A against this class alone: the two cell-B locks {@code corpus_c1_10} /
 * {@code corpus_c1_11} and the RESHAPED {@code b2} fixture, which now mirrors
 * {@code BlockTradeElectionIndicator}'s block-lambda shape instead of an ITE hoist. Measured
 * (lane A applied, {@code -Dtest=MapperWrapPrefixSeatTest -Dcorpus.required=true}): <b>20 run / 3
 * FAILURES / 1 skipped</b> — exactly {@code corpus_c1_10}, {@code corpus_c1_11} and {@code b2},
 * each losing the lift to {@code MapperC.of(MapperS.of(…))}; unmutated the same selection was 20 /
 * 0F / 1 skipped. <b>Those two figures were measured at {@code dd334ce83}, BEFORE the
 * code-quality review's SF-6 added {@code corpus_control2b}</b>: the class is now 21 tests and the
 * unmutated selection measures 21 / 0F / 2 skipped (both IR controls skipping without
 * {@code -Pir-on}). The lane-A figures are left as they were measured rather than re-derived — a
 * projection is not a receipt (LAW 82) — and the three tests it moves are unchanged by SF-6, which
 * added a control the lane does not reach. The citable full-selection run at {@code 4056e93f2}
 * (lane2, above) then re-measured lane A at <b>11F with this suite 21/3F</b> — the same three
 * witnesses moving inside the full selection. {@code corpus_control0c} is a GOLDEN-token control and
 * does not move under a
 * mutation by construction — the byte locks are the sensitivity witnesses, it is the statement of
 * what golden says.
 */
class MapperWrapPrefixSeatTest {

    private static final Path REPO_ROOT =
            Path.of(System.getProperty("user.dir")).resolve("..").normalize();

    private static final List<Path> BUILTINS_SEARCH_ROOTS = List.of(
            REPO_ROOT.resolve("test-corpus/rune-dsl-builtins/rune-runtime/src/main/resources/model"),
            REPO_ROOT.resolve("rune-dsl/rune-runtime/src/main/resources/model"),
            REPO_ROOT.resolve("rune-runtime/src/main/resources/model"));

    /**
     * The reduced-fixture gate. <b>It does NOT route through {@link Drr7Corpus}</b> (the
     * code-quality review's NIT-10), so under {@code -Dcorpus.required=true} the five reduced
     * fixtures still SKIP SILENTLY if the rune-dsl builtins are absent, where the corpus locks
     * would fail loudly. That is the house-wide pattern for builtins-gated fixtures and is left
     * as-is; it matters here because b1a/b1b are lane D's ONLY receipt (the corpus declines at
     * 54/54), so the chain's per-class run line — not a green exit code — is where their having
     * actually RUN is proven.
     */
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

    /**
     * The second carrier cell — drr 7.0.0, the ONLY cell whose {@code FirstExerciseDateRule} renders
     * the enum-switch BLOCK lambda the dispatch seat lives in (the 6.x cells render an
     * {@code exists} ladder for the same rule). Gated through {@link Drr7Corpus} so a local gating
     * run can never pass by silently skipping the lock.
     */
    private static final Path CELL_B_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path GOLDEN_B = CELL_B_ROOT.resolve("rosetta-source/src/generated/java");

    static boolean drr7Available() {
        return Drr7Corpus.gate(Files.isDirectory(GOLDEN_B)
                && Files.isDirectory(CELL_B_ROOT.resolve("rosetta-source/src/main/rosetta")),
                MapperWrapPrefixSeatTest.class);
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

    /**
     * The cell-B twin of {@link #cellAAndIrProviderAvailable} (the code-quality review's SF-6).
     * Routed through {@link #drr7Available} — and therefore through {@link Drr7Corpus} — so a
     * local gating run cannot pass by silently skipping the cell-B route-parity control; the
     * IR-provider half still skips on its own, exactly as the cell-A control does.
     */
    static boolean cellBAndIrProviderAvailable() {
        return drr7Available() && irProviderOnClasspath();
    }

    /** S02 / S03 / S10 — the #373 F-alpha MULTI extract-receiver re-presentation wrap. */
    private static final String CARRIER_EXTRACT_RECV_MULTI =
            "drr/standards/iosco/cde/version1/price/reports/PriceCurrencyRule.java";
    /** S14 — the #386 T386C SINGLE extract-receiver wrap, chosen by the (now typed) map chooser. */
    private static final String CARRIER_EXTRACT_RECV_SINGLE =
            "drr/standards/iosco/cde/version1/party/reports/Direction2Leg1Rule.java";
    /** S06 / S08 — the ladder rung + terminal drained-then-chain constructor wrap. */
    private static final String CARRIER_LADDER_CTOR =
            "drr/regulation/csa/rewrite/dtcc/reports/DTCC_UnderlyingAssetNameRule.java";
    /** S07 / S09a / S09b — the effective-else / elseless block ctor-arm wraps. */
    private static final String CARRIER_BLOCK_CTOR_ARMS =
            "drr/regulation/csa/rewrite/dtcc/reports/DTCC_UnderlyingAssetReportRule.java";
    /** S13 — the {@code MapperC.}-headed arm short-circuit at the item-to-list seat. */
    private static final String CARRIER_MAPPER_C_ARM =
            "drr/regulation/common/trade/quantity/reports/NotionalQuantityLeg1Rule.java";
    /**
     * <b>S20</b>, the SIBLING lift — NOT S11 (the spec-compliance review's MF-1). This golden's
     * {@code MapperC.of(Collections.singletonList("RTNNotProvided"))} is emitted by
     * {@code ControlFlowHandler.wrapDeepThenIteArm} (the #364 {@code mapperCIteLift} / #588
     * {@code nullTypedIteLiteralArmLift} arm, which names this very file in its own javadoc), a
     * DIFFERENT family-8 seat that produces byte-identical text at an ITE-hoist assignment rather
     * than at a block-lambda {@code return}. The suite carried it as an S11 carrier until the
     * review; lane A (which inverts S11's gate and nothing else) left this lock GREEN, which is
     * the measurement that settles it. Kept — it locks S20's form, and S20 is the family's
     * VERDICT-MOVED RETIRE-AFTER-CENSUS → JUSTIFIED-KEPT row, whose emitted form deserves a byte
     * lock exactly as much.
     */
    private static final String CARRIER_ITE_ARM_SINGLETON_LIFT =
            "drr/regulation/common/functions/GetReportTrackingNumber.java";
    /**
     * <b>S20</b> again, in a different namespace — the second carrier of the SIBLING lift. The
     * #588 runtime probe printed this rule's arm verbatim
     * ({@code multi=true type=null isMapperS=false unwrap=true node=RStringLiteral
     * render=MapperS.of("NON-CA")}), so the producer here is named by measurement, not by
     * attribution.
     */
    private static final String CARRIER_ITE_ARM_SINGLETON_LIFT_2 =
            "drr/regulation/csa/rewrite/trade/reports/JurisdictionOfCounterparty1Rule.java";
    /** S32 / S33 — the #260 collapse re-wrap and the #276 bare-function then arm. */
    private static final String CARRIER_THEN_ARG_COLLAPSE =
            "drr/regulation/cftc/rewrite/trade/reports/PostPricedSwapIndicatorRule.java";

    private static final List<String> CARRIERS = List.of(
            CARRIER_EXTRACT_RECV_MULTI, CARRIER_EXTRACT_RECV_SINGLE, CARRIER_LADDER_CTOR,
            CARRIER_BLOCK_CTOR_ARMS, CARRIER_MAPPER_C_ARM, CARRIER_ITE_ARM_SINGLETON_LIFT,
            CARRIER_ITE_ARM_SINGLETON_LIFT_2, CARRIER_THEN_ARG_COLLAPSE);

    /** S15 / S16 — the enum-switch block lambda's splice-vs-wrap dispatch (cell B only). */
    private static final String CARRIER_ENUM_SWITCH_B =
            "drr/standards/iosco/cde/version1/price/reports/FirstExerciseDateRule.java";

    /**
     * <b>S11 — the REAL carrier</b>, and cell B is the only place it lives (the spec-compliance
     * review's MF-1). The c8 census puts every one of S11's 48 gate-true arrivals at three
     * carriers, all of them drr 7.x: {@code rule:IndexFactor} 24 (2 sites × 4 cells),
     * {@code rule:BlockTradeElectionIndicator} 12 and {@code fn:GetBasketConstituents} 12; the
     * 6.x cells — cell A included — contribute ZERO, which is why the suite had no effective S11
     * witness until this lock and why lane A left it 17/0F. This file is the smallest of the
     * three (71 lines) and carries BOTH polarities: the MULTI block-lambda terminal arm takes the
     * lift ({@code return MapperC.of(Collections.singletonList(false));} — the boolean literal's
     * {@code MapperS.of(false)} wrap spliced) while the SINGLE {@code mapItem} arm one level in
     * keeps its wrap ({@code return MapperS.of(false);}) from the very same literal.
     */
    private static final String CARRIER_SINGLE_WRAP_LIFT_B =
            "drr/regulation/cftc/rewrite/trade/reports/BlockTradeElectionIndicatorRule.java";
    /**
     * <b>S11</b>, the second real carrier — the two-site one ({@code rule:IndexFactor}, 8 gate-true
     * arrivals per walk over the four drr 7.x cells = 2 per cell). Its inner is not a literal but
     * a function call, so the two locks together witness the lift over both inner shapes the
     * census found.
     */
    private static final String CARRIER_SINGLE_WRAP_LIFT_B2 =
            "drr/regulation/common/trade/index/reports/IndexFactorRule.java";

    // =========================================================================
    // Part A — the reduced fixtures
    // =========================================================================

    private static final String MODEL = """
            namespace census.seat615f8
            version "1.0.0"

            type Leg:
                mark string (0..1)

            type Root:
                legs Leg (0..*)

            func B1TernaryElseArmLift: <"b1a - seat S18: a MULTI then-arm beside a SINGLE else-arm lifts the else arm">
                output:
                    result int (0..*)
                set result:
                    42
                        extract
                            if False
                            then [1, 2]
                            else 0

            func B1TernaryThenArmLift: <"b1b - seat S19: a MULTI else-arm beside a SINGLE then-arm lifts the then arm">
                output:
                    result int (0..*)
                set result:
                    42
                        extract
                            if False
                            then 0
                            else [1, 2]

            func B1TernarySameArity: <"b1c - the decline control: arms of EQUAL cardinality take no lift at either seat">
                output:
                    result int (0..*)
                set result:
                    42
                        extract
                            if False
                            then [1, 2]
                            else [3, 4]

            reporting rule B2SingleWrapArmLift from Root: <"b2 - seat S11: a whole single wrap at the MULTI block-lambda arm seat is presented as a singleton list">
                extract
                    if legs -> mark exists
                    then (legs
                        then filter mark exists
                        then extract
                            if mark exists
                            then mark
                            else "INNER")
                    else "OUTER"
                then only-element

            reporting rule B3ThenArgCollapseWrap from Root: <"b3 - seat S32: a then-body that collapses to a bare item re-wraps its declaration exactly once">
                extract legs
                then only-element
                then if item -> mark exists then item -> mark else "NONE"

            """;

    /**
     * b1a — S18, the ELSE arm. The upstream
     * {@code FunctionGeneratorTest.canReturnDifferingCardinalitiesInIfThenElseBranches} shape
     * verbatim: the SINGLE arm is a scalar literal, which compiles through {@code LiteralHandler}
     * — ALREADY migrated to {@code JavaExpression.wrappedInMapperSOf} — so the marker IS present and
     * the swapped read fires exactly where the rendered prefix fired. This is the POSITIVE polarity
     * the corpus cannot supply: all 54 census arrivals at both seats decline
     * ({@code unwrap=no AND text=false}), so lane D's receipt is here and nowhere else.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b1a_ternaryElseArmLiftsOnTheWrapMarker() throws IOException {
        String code = codeOnly(fn("B1TernaryElseArmLift.java"));
        assertContains(code, " : MapperC.of(MapperS.of(");
    }

    /**
     * b1b — S19, the THEN arm: the same lift at the mirrored gate, distinguished from b1a by its
     * position in the emitted ternary ({@code ? <then> : <else>}).
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b1b_ternaryThenArmLiftsOnTheWrapMarker() throws IOException {
        String code = codeOnly(fn("B1TernaryThenArmLift.java"));
        assertContains(code, "? MapperC.of(MapperS.of(");
    }

    /**
     * b1c — the decline control for both seats: with EQUAL arm cardinalities the AST gate never
     * opens, so no marker read happens and neither arm is lifted. Without this, b1a/b1b would pass
     * for a seat that lifted unconditionally.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b1c_equalArityTernaryTakesNoArmLift() throws IOException {
        String code = codeOnly(fn("B1TernarySameArity.java"));
        assertAbsent(code, "MapperC.of(MapperS.of(");
    }

    /**
     * b2 — S11, <b>RESHAPED at the spec-compliance review's MF-1</b>. The first version of this
     * fixture ({@code func B2SingleWrapArmLift}, a MULTI chain then a conditional with a literal
     * else-arm) rendered the right TEXT from the WRONG SEAT: its
     * {@code MapperC.of(Collections.singletonList("NONE"))} came out of
     * {@code ControlFlowHandler.wrapDeepThenIteArm} (S20), and lane A — which inverts S11's gate
     * and nothing else — left the whole suite 17/0F, which is how the mis-attribution was caught.
     *
     * <p>The shape is now a faithful miniature of the REAL S11 carrier, cell B's
     * {@code BlockTradeElectionIndicator} ({@code drr/regulation/cftc/rewrite/trade-rule}): a
     * conditional whose then-arm is a filtered MULTI chain carrying its own nested conditional —
     * which is what forces the BLOCK lambda instead of a ternary — and whose else-arm is a bare
     * string literal. The literal compiles to a whole balanced {@code MapperS.of("OUTER")} wrap
     * WITH the factory marker, and the MapperC block seat presents it as a singleton list at a
     * {@code return}. Under lane A the gate closes and the arm takes
     * {@code MapperC.of(MapperS.of("OUTER"))} instead — the form the third assertion forbids.
     *
     * <p>The second assertion is the DECLINE polarity, and the carrier has it too: the nested
     * conditional's own literal else-arm sits at a SINGLE {@code mapItem} seat and KEEPS its wrap.
     * The two literals are deliberately distinct ({@code "OUTER"} / {@code "INNER"}) so neither
     * assertion can be satisfied by the other seat's emission — the carrier uses {@code false} at
     * both and could not tell them apart.
     *
     * <p>This is the ONE fixture in the family whose decided form CONTAINS a rendered string
     * literal, so it reads {@link #codeNoComments} and not {@link #codeOnly}: the shared
     * {@code codeOnly} view (inherited verbatim from the #614 suite) blanks every string literal by
     * design, which turns the very token this seat emits into {@code singletonList()}. The model's
     * own doc strings are kept free of Java-shaped text so the literal-preserving view cannot be
     * satisfied by a definition string instead of by code.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b2_wholeSingleWrapArmLiftsToASingletonList() throws IOException {
        String code = codeNoComments(rule("B2SingleWrapArmLiftRule.java"));
        assertContains(code, "return MapperC.of(Collections.singletonList(\"OUTER\"));");
        assertContains(code, "return MapperS.of(\"INNER\");");
        assertAbsent(code, "MapperC.of(MapperS.of(");
    }

    /**
     * b3 — S32: a faithful miniature of the charter carrier {@code PostPricedSwapIndicator} (a MULTI
     * extract, a collapse, then a conditional). The collapsing then-body's declaration re-wraps the
     * bare item, and the belt that keeps it from wrapping twice is now the in-method flag. Under
     * lane E the belt inverts and the wrap disappears.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b3_collapsingThenArgDeclReWrapsExactlyOnce() throws IOException {
        String code = codeOnly(rule("B3ThenArgCollapseWrapRule.java"));
        assertContains(code, "MapperS.of(thenArg0.get())");
        assertAbsent(code, "MapperS.of(MapperS.of(thenArg0.get())");
    }

    // =========================================================================
    // Part B — the corpus carriers (cell A drr 6.34.1; cell B drr 7.0.0)
    // =========================================================================

    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_01_priceCurrencyRuleByteIdentical() throws IOException {
        lock(CARRIER_EXTRACT_RECV_MULTI);
    }

    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_02_direction2Leg1RuleByteIdentical() throws IOException {
        lock(CARRIER_EXTRACT_RECV_SINGLE);
    }

    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_03_dtccUnderlyingAssetNameRuleByteIdentical() throws IOException {
        lock(CARRIER_LADDER_CTOR);
    }

    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_04_dtccUnderlyingAssetReportRuleByteIdentical() throws IOException {
        lock(CARRIER_BLOCK_CTOR_ARMS);
    }

    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_05_notionalQuantityLeg1RuleByteIdentical() throws IOException {
        lock(CARRIER_MAPPER_C_ARM);
    }

    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_06_getReportTrackingNumberByteIdentical() throws IOException {
        lock(CARRIER_ITE_ARM_SINGLETON_LIFT);
    }

    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_07_jurisdictionOfCounterparty1RuleByteIdentical() throws IOException {
        lock(CARRIER_ITE_ARM_SINGLETON_LIFT_2);
    }

    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_08_postPricedSwapIndicatorRuleByteIdentical() throws IOException {
        lock(CARRIER_THEN_ARG_COLLAPSE);
    }

    @Test
    @EnabledIf("drr7Available")
    void corpus_c1_09_firstExerciseDateRuleByteIdenticalCellB() throws IOException {
        lockB(CARRIER_ENUM_SWITCH_B);
    }

    /**
     * corpus_c1_10 — <b>the S11 witness</b>, added at the spec-compliance review's MF-1 because the
     * suite had none: the retired identity's own carriers are all drr 7.x, and both of this file's
     * polarities live inside one 71-line rule. Under lane A ({@code isPresent()} →
     * {@code isEmpty()}) the terminal arm loses the lift and this lock goes RED.
     */
    @Test
    @EnabledIf("drr7Available")
    void corpus_c1_10_blockTradeElectionIndicatorRuleByteIdenticalCellB() throws IOException {
        lockB(CARRIER_SINGLE_WRAP_LIFT_B);
    }

    /**
     * corpus_c1_11 — the second S11 witness, the two-site carrier, whose lifted inners are function
     * calls rather than a literal. Two carriers rather than one because the census found the lift
     * at both inner shapes and a single-shape lock would under-witness it.
     */
    @Test
    @EnabledIf("drr7Available")
    void corpus_c1_11_indexFactorRuleByteIdenticalCellB() throws IOException {
        lockB(CARRIER_SINGLE_WRAP_LIFT_B2);
    }

    /**
     * corpus_control0 — golden is the oracle: each cell-A carrier still carries the EXACT form the
     * seat it witnesses decides. Every token below is form-specific and falsifiable; a bare
     * {@code contains("MapperS.of(")} would be satisfied by any of dozens of occurrences and would
     * witness nothing. If one of these ever stops holding, the carrier stopped being this family's
     * carrier and every claim above is restated, not patched.
     *
     * <p><b>TWO ASSERTIONS WERE ATTRIBUTED TO THE WRONG SEAT and are corrected here</b> (the
     * spec-compliance review's MF-1). The {@code GetReportTrackingNumber} and
     * {@code JurisdictionOfCounterparty1Rule} singleton lists were labelled S11; they are emitted
     * by S20, {@code ControlFlowHandler.wrapDeepThenIteArm}, whose own javadoc names both files.
     * The two seats emit byte-identical text from different methods and are told apart only by
     * their surrounding statement — {@code ifThenElseResult = …;} at S20's hoisted local versus
     * {@code return …;} inside S11's block lambda — so the assertions now pin the WHOLE statement,
     * not the expression. Lane A, which inverts S11's gate and nothing else, left both of these
     * GREEN, and that is the measurement the correction rests on. S11's own witnesses are cell-B
     * and live in {@link #corpus_control0c_cellBGoldenCarriesTheSingleWrapLift}.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control0_goldensCarryTheDecidedForms() throws IOException {
        assertTrue(golden(CARRIER_EXTRACT_RECV_MULTI)
                        .contains("MapperC.<PriceSchedule>of(contract_Price_Monetary"
                                + ".evaluate(thenArg0.get()))"),
                "S02/S03/S10: the golden's MULTI extract receiver must be re-presented"
                + " MapperC.<Elem>of(...) before its map-method chain");
        assertTrue(golden(CARRIER_EXTRACT_RECV_SINGLE)
                        .contains("MapperS.of(tradeForEvent.evaluate(item.get()))"),
                "S14: the golden's SINGLE explicit-args receiver must be re-presented"
                + " MapperS.of(...) — the wrap kind the map-method chooser selected");
        assertTrue(golden(CARRIER_EXTRACT_RECV_SINGLE)
                        .contains(".mapSingleToItem(trade -> trade."),
                "S14: and the SAME chooser must have selected the mapSingle* chain member — the"
                + " two halves of the #274 law, now read from one typed answer");
        assertTrue(golden(CARRIER_LADDER_CTOR)
                        .contains("return MapperS.of(UnderlyingAssetNameReport.builder()"),
                "S06/S08: the golden's ladder arms must wrap the DRAINED then-chain's built"
                + " constructor MapperS.of(...)");
        assertTrue(golden(CARRIER_BLOCK_CTOR_ARMS)
                        .contains("return MapperS.of(UnderlyingAssetReport.builder()"),
                "S07/S09: the golden's block ctor arms must carry the MapperS.of wrap");
        assertTrue(golden(CARRIER_MAPPER_C_ARM)
                        .contains("return MapperC.<PriceQuantity>of(MapperS.of("),
                "S13: the golden's rung must carry the witnessed MapperC wrap over a MapperS arm");
        assertTrue(!golden(CARRIER_MAPPER_C_ARM).contains("MapperC.of(MapperC."),
                "S13: and NOT the MapperC.of(MapperC...) double wrap the short-circuit exists to"
                + " prevent — its absence is what the seat buys");
        assertTrue(golden(CARRIER_ITE_ARM_SINGLETON_LIFT)
                        .contains("ifThenElseResult = MapperC.of(Collections.singletonList("
                                + "\"RTNNotProvided\"));"),
                "S20: the golden's ITE-hoist arm must present the null-typed literal wrap as a"
                + " SINGLETON LIST at the MapperC-typed local — and the `ifThenElseResult =` head"
                + " is the token that distinguishes this seat from S11's block-lambda `return`,"
                + " which emits byte-identical text from a different method");
        assertTrue(!golden(CARRIER_ITE_ARM_SINGLETON_LIFT).contains("MapperC.of(MapperS.of("),
                "S20: and NOT MapperC.of(MapperS.of(...)) — the form the lift replaces");
        assertTrue(golden(CARRIER_ITE_ARM_SINGLETON_LIFT_2)
                        .contains("ifThenElseResult = MapperC.of(Collections.singletonList("
                                + "\"NON-CA\"));"),
                "S20: the second carrier must carry the same singleton-list presentation at the"
                + " same ITE-hoist head — the arm the #588 runtime probe printed verbatim");
        assertTrue(golden(CARRIER_THEN_ARG_COLLAPSE)
                        .contains("final MapperS<ReportingRegime> thenArg3 = MapperS.of(thenArg2.get());"),
                "S32: the golden's collapsing then-body decl must re-wrap the bare item EXACTLY"
                + " once — the belt is what keeps it from wrapping twice");
        assertTrue(!golden(CARRIER_THEN_ARG_COLLAPSE).contains("MapperS.of(MapperS.of(thenArg2.get())"),
                "S32: and never twice — the double wrap is what lane E produces");
    }

    /**
     * corpus_control0b — the cell-B golden carries BOTH polarities of the enum-switch dispatch in
     * one file: the collapsing-MULTI arm splices its {@code MapperC.<Date>of(…)} body verbatim,
     * while an identity-unwrap arm (a {@code getOrDefault} deref that collapses to a bare item)
     * DECLINES the splice and takes the {@code MapperS.of(...)} wrap. The identity guard sits
     * OUTSIDE the disjunction the swap touches, which is why swapping one leg is byte-inert.
     */
    @Test
    @EnabledIf("drr7Available")
    void corpus_control0b_cellBGoldenCarriesBothDispatchPolarities() throws IOException {
        String g = Files.readString(GOLDEN_B.resolve(CARRIER_ENUM_SWITCH_B));
        assertTrue(g.contains("return MapperC.<Date>of(adjustableDatesResolution.evaluate("),
                "S15/S16: the golden's BERMUDA arm must SPLICE its collapsing MULTI body verbatim");
        assertTrue(g.contains("return MapperS.of(MapperS.of(adjustableDateResolution.evaluate("),
                "S15/S16: and an identity-unwrap arm must DECLINE the splice and take the wrap");
        assertTrue(g.contains("if (switchArgument == OptionExerciseStyleEnum.BERMUDA) {"),
                "S15/S16: the file must still render the enum-switch BLOCK lambda — the only form"
                + " in which this seat is reached");
    }

    /**
     * corpus_control0c — <b>S11's golden-token control, cell B</b> (the spec-compliance review's
     * MF-1). The retired identity has no cell-A carrier at all, so this is the only place golden
     * can be asked what the seat decides. Both polarities are pinned in the first file: the
     * MULTI block-lambda terminal arm takes the lift, while the SINGLE {@code mapItem} arm one
     * level in keeps the wrap over the SAME {@code false} literal — the second assertion is what
     * stops the first from passing for a seat that lifted unconditionally. The third and fourth
     * pin the function-call inner at the two-site carrier, and the fifth is the absence control:
     * {@code MapperC.of(MapperS.of(} is the form the lift replaces and the form lane A restores.
     */
    @Test
    @EnabledIf("drr7Available")
    void corpus_control0c_cellBGoldenCarriesTheSingleWrapLift() throws IOException {
        String g = Files.readString(GOLDEN_B.resolve(CARRIER_SINGLE_WRAP_LIFT_B));
        assertTrue(g.contains("return MapperC.of(Collections.singletonList(false));"),
                "S11: the golden's MULTI block-lambda terminal arm must present the whole single"
                + " wrap MapperS.of(false) as a SINGLETON LIST — the inner spliced, the wrap gone,"
                + " and `return` (not `ifThenElseResult =`) is what makes this S11's seat");
        assertTrue(g.contains("return MapperS.of(false);"),
                "S11: and the SAME literal at the SINGLE mapItem seat one level in must KEEP its"
                + " wrap — the decline polarity, without which the lift assertion would pass for a"
                + " seat that lifted everything");
        String g2 = Files.readString(GOLDEN_B.resolve(CARRIER_SINGLE_WRAP_LIFT_B2));
        assertTrue(g2.contains("return MapperC.of(Collections.singletonList("
                        + "formatToBaseOne18Rate.evaluate(BigDecimal.valueOf(1))));"),
                "S11: the two-site carrier must take the same lift over a FUNCTION-CALL inner —"
                + " the second of the two inner shapes the census found");
        assertTrue(g2.contains("return MapperC.of(Collections.singletonList(formatToBaseOneRate"
                        + ".evaluate("),
                "S11: and at its second site, so the 2-per-cell census count is witnessed as two");
        assertTrue(!g.contains("MapperC.of(MapperS.of(") && !g2.contains("MapperC.of(MapperS.of("),
                "S11: neither carrier may carry MapperC.of(MapperS.of(...)) — the pre-lift form,"
                + " and exactly what lane A puts back");
    }

    /**
     * corpus_control2 — LAW 77 route parity: every seat in this family lives in the SHARED
     * generator, which the IR route runs too, so each cell-A carrier must render identically under
     * {@code -Pir-on}. The census corroborates the law's precondition: every seat with a
     * recommended swap has IDENTICAL default-route and IR-route arrivals. Skips (recorded) without
     * the IR provider on the classpath.
     *
     * <p><b>THE SPLIT, and why there are two of these</b> (the code-quality review's SF-6). This
     * control covers CELL A only, and cell A carries no S11 site at all — all 48 gate-true S11
     * arrivals are drr 7.x. So the family's one JUSTIFIED-KEPT-CANDIDATE → RETIRE swap had its
     * route parity resting entirely on the receipts chain's ROUTE ROW DIFF over the whole matrix,
     * with nothing in this suite to fail if the two routes ever disagreed at that seat.
     * {@link #corpus_control2b_irRouteMatchesLegacyForEveryCellBCarrier} is the cell-B twin that
     * closes it; keep the two separate because the two cells have different gates (cell B's runs
     * through {@link Drr7Corpus}) and different carrier lists.
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

    /**
     * corpus_control2b — the CELL-B route-parity control (the code-quality review's SF-6), over
     * the three drr 7.0.0 carriers: the enum-switch dispatch file and the TWO S11 locks. Cell A
     * has no S11 site, so without this control S11's LAW-77 parity rested only on the chain's
     * whole-matrix ROUTE ROW DIFF — a gate that lives outside this suite and outside any lane, so
     * a route-only regression at the family's one decision-changing swap would have had no witness
     * here. The cell-B carriers are the only place the seat can be asked the question at all.
     *
     * <p>Same skip discipline as its cell-A twin: the IR-provider half skips without
     * {@code -Pir-on}, while the corpus half goes through {@link Drr7Corpus} so a local gating run
     * with {@code -Dcorpus.required=true} fails rather than skipping.
     */
    @Test
    @EnabledIf("cellBAndIrProviderAvailable")
    void corpus_control2b_irRouteMatchesLegacyForEveryCellBCarrier() throws IOException {
        assertNotNull(drrBOutput, "drr 7.0.0 legacy-route generation did not run");
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", CELL_B_ROOT), new ArrayList<>());
        for (String carrier : List.of(CARRIER_ENUM_SWITCH_B, CARRIER_SINGLE_WRAP_LIFT_B,
                CARRIER_SINGLE_WRAP_LIFT_B2)) {
            assertEquals(drrBOutput.get(carrier), irOut.get(carrier), "route divergence: " + carrier);
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
                + carrier + " — PR #615 family 8: the wrap-prefix reads move onto the factory's"
                + " marker, a typed chooser and an in-method flag with no byte change.");
    }

    /**
     * The cell-B (drr 7.0.0) twin of {@link #lock}. It served the enum-switch dispatch carrier
     * alone when it was written; since the spec-compliance review's MF-1 it serves THREE locks —
     * that carrier plus the two S11 carriers ({@code BlockTradeElectionIndicatorRule},
     * {@code IndexFactorRule}), which are the family's only effective S11 witnesses because cell A
     * has no S11 site.
     */
    private static void lockB(String carrier) throws IOException {
        assertNotNull(drrBOutput, "drr 7.0.0 generation did not run — corpus unavailable?");
        List<String> own = drrBGenErrors.stream().filter(e -> e.contains(carrier)).toList();
        assertTrue(own.isEmpty(),
                "the generator reported errors for the locked file " + carrier + ": " + own);
        String generated = drrBOutput.get(carrier);
        assertNotNull(generated, "not generated in drr 7.0.0: " + carrier);
        Path goldenPath = GOLDEN_B.resolve(carrier);
        assertTrue(Files.isRegularFile(goldenPath), "golden missing: " + goldenPath);
        assertEquals(normalize(Files.readString(goldenPath)), normalize(generated),
                "generated drr 7.0.0 output must byte-match golden (newline-normalized) for "
                + carrier + " — PR #615 family 8: the enum-switch splice leg moves onto the wrap"
                + " factory's marker with no byte change.");
    }

    // =========================================================================
    // Fixture harness (the sibling seat suites' renderer)
    // =========================================================================

    private record Render(Map<String, String> output, List<String> errors) {}

    private static Render rendered;

    private static Render render() throws IOException {
        if (rendered != null) {
            return rendered;
        }
        RModel main = AstBuilder.buildFromString(MODEL, "seat615f8.rosetta");
        main.setVersion("0.0.0.test");
        List<RModel> models = new ArrayList<>();
        models.add(main);
        models.addAll(loadBuiltinsOnly());
        RWorkspace workspace = RWorkspace.build(models).workspace();
        GeneratorModel gm = new GeneratorModel(workspace,
                m -> "census.seat615f8".equals(m.namespace()));
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
     * Strip line and block comments ONLY, keeping string literals — the view for the one seat whose
     * decided form CONTAINS a rendered literal ({@code Collections.singletonList("OUTER")}, the
     * b2 fixture's literal after its reshape at the spec-compliance review's MF-1).
     * {@link #codeOnly} would blank exactly that token; a raw read would let a model doc string
     * satisfy the assertion. Comments are stripped by the same walk {@code codeOnly} uses, with the
     * literal branch COPYING the literal through instead of dropping it.
     */
    private static String codeNoComments(String java) {
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
                int end = Math.min(j + 1, n);
                sb.append(java, i, end);
                i = end;
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
            throw new AssertionError("[MapperWrapPrefixSeatTest] builtins parse failures: "
                    + String.join("; ", failures));
        }
        return models;
    }

    // =========================================================================
    // Corpus harness (the sibling seat suites' cell generator, verbatim)
    // =========================================================================

    private static Map<String, String> drrAOutput;
    private static List<String> drrAGenErrors;
    private static Map<String, String> drrBOutput;
    private static List<String> drrBGenErrors;

    @BeforeAll
    static void generateCells() throws IOException {
        if (cellAAvailable()) {
            List<String> errs = new ArrayList<>();
            drrAOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", CELL_A_ROOT), errs);
            drrAGenErrors = errs;
        }
        if (Files.isDirectory(GOLDEN_B)
                && Files.isDirectory(CELL_B_ROOT.resolve("rosetta-source/src/main/rosetta"))) {
            List<String> errs = new ArrayList<>();
            drrBOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", CELL_B_ROOT), errs);
            drrBGenErrors = errs;
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
