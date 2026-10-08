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
 * PR #616, C2d retirement family 9 {@code then-binding-overlay} — the {@code #350}
 * {@code thenArgRefFor} / {@code boundImplicitReceiverType} overlay reads, the hoist witnesses and
 * the drained-chain TAIL witnesses, dispositioned WHOLE: <b>4 rows RETIRE onto an AST channel and
 * 39 rows / 40 occurrences are adjudicated JUSTIFIED-KEPT with their measurements</b>, 44 of 44
 * occurrences, no PENDING residue.
 *
 * <p><b>THE GOVERNANCE INVERSION — the census turned the triage table inside out.</b> Every one of
 * family 9's TWENTY {@code RETIRE-AFTER-CENSUS} rows moves to JUSTIFIED-KEPT, and all four
 * retirements come from the {@code JUSTIFIED-KEPT-CANDIDATE} side: 24 of 43 rows (55.8%) change
 * verdict. The twenty RAC rows were verdicted on PROPOSED channels — a chain-top window, a
 * cardinality computer, a workspace inference, a recorded restructure, a recorded hoist kind, a
 * producer-set session flag, an AST spine walk, an AST base/step test — and the c9 census finds
 * every one of those eight either unpopulated, unreachable, non-existent or measurably wrong, seat
 * by seat, with the figure at each. A triage VERDICT and a triage JUSTIFICATION are both
 * hypotheses (the #614/#615 law); this family is the strongest demonstration of it the programme
 * has produced. <b>A family can close mostly KEPT, and that is a result</b> — the measurement that
 * keeps a row is worth as much as the channel that retires one.
 *
 * <p><b>The four retirements</b>, all of them a rendered-string TAIL read deleted because the AST
 * conjunct beside it already decides ({@code target/seat616-instruments/c2c-f9-verdicts.md}, local
 * — round c9, three walks: the default-route D11 275/0F with 506,934 probe lines, the IR-route D11
 * 275/0F with 439,980, the optimised suite 12/0F with 483,255; 1,430,169 lines parsed with 0
 * ignored / 0 malformed / 0 spliced / 0 probeExc / 0 parse-reject on every walk):
 * <ul>
 *   <li><b>S22 — {@code renderLadderLevel}'s rung arm</b>, the family's ONE live retirement.
 *       {@code isThenChainTerminalCtor(rung.thenBranch())} TRUE <em>implies</em> the render ends
 *       {@code .build()} at <b>15/15</b> with ZERO counterexamples over 17,361 arrivals (5,787 per
 *       walk); the cross-tab's {@code (t,f,·)} cell is EMPTY on every walk. The converse fails 249
 *       times and all 249 carry {@code ctorArm=true} — the {@code instanceof RConstructorExpr}
 *       disjunct one line up already fires there — so the deletion changes the arm's value at NO
 *       arrival. The 15 that decide are all {@code rule:DTCC_UnderlyingAssetName}, and 100% of them
 *       carry a {@code __COERCION_PARAM_} placeholder.</li>
 *   <li><b>S21 — the ladder TERMINAL</b> and <b>S19 — {@code isCtorArm}</b>, the clause-for-clause
 *       mirrors (LAW 69, {@code CollectionHandler.renderLadderLevel}'s terminal {@code elseWrap}
 *       fold and {@code CollectionHandler.isCtorArm} — named rather than line-cited, per the
 *       ledger's own convention that line numbers drift and are not recorded; every earlier
 *       citation here had already gone stale): both sides FALSE at 1,146/1,146 and 486/486, so the
 *       two agree everywhere and the arm fires under neither form. All three move in ONE commit or
 *       none.</li>
 *   <li><b>S20 — {@code compileLambda}'s {@code #396} CR-terminal predicate arm</b>, the family's
 *       strongest single measurement: {@code isFilterPredicateCrTerminalChain(crPredChain)} and
 *       {@code bodyExpr.renderToString().endsWith(".asMapper()")} agree at <b>122,731/122,731
 *       (100.00%)</b> — T/T 36, T/F 0, F/T 0, F/F 122,695, zero off-diagonal on any walk, both
 *       polarities massively witnessed. It is a bijection with a MECHANISM: the {@code .asMapper()}
 *       tail is appended by the CR-terminal chain's own re-root, so the predicate that admits the
 *       shape and the tail that records it are ONE event.</li>
 * </ul>
 *
 * <p><b>No dead-branch deletions, and nothing stays PENDING.</b> Nine rows are MEASURED-INERT (their
 * own conjunct, or the arm they sit in, never fires at this corpus) and a corpus zero on a live
 * predicate is not a licence to delete — the #615 S21 adjudication applies verbatim, which is why
 * S19 and S21 RETIRE onto the predicate with their mirror rather than being removed.
 *
 * <p><b>THE THREE MASTER NEGATIVES</b>, each of which killed a proposed retirement and is recorded
 * in the rows it saved:
 * <ol>
 *   <li><b>The {@code #350} binding and the {@code #356} chain-top window are in NO containment
 *       relation.</b> Measured against the binding at every arrival of the five
 *       {@code thenarg-binding-hoist-witness} seats, the window is a strict SUBSET at one (S01,
 *       3/126), a strict SUPERSET at another (S02, 789 F/T), answers NEVER at a third (S13, 0/180)
 *       and disagrees in BOTH directions at a fourth (S14, 525 T/F + 342 F/T, with no arrival where
 *       both answer false). No substitution, conjunction or disjunction reproduces the binding at
 *       all five.</li>
 *   <li><b>The one-producer {@code .then(} retirement is unbuildable as specified.</b> The single
 *       producer has {@code sessionReachable=false} at 117/117 firings, so the session flag the
 *       triage proposed cannot be set where the producer runs; and the four consumers are inert
 *       anyway — ZERO {@code .then(} residues in 61,985 combined arrivals.</li>
 *   <li><b>The HOISTREG "recorded kind" agreement is a SELF-agreement.</b> The probe sat inside
 *       {@code JavaStatementScope.registerStatementHoist(String)}, which receives only a String, and
 *       recomputed the same four string tests the pull site runs. The 2,886/2,886 measures string
 *       stability across the drain and says NOTHING about a typed channel, so the HOIST group's
 *       proposed channel is UNMEASURED and cannot carry a retirement (the #614 law: the instrument
 *       can be the defect). A second round is required before any future HOIST seat, with the probe
 *       at the {@code registerStatementHoist} CALL SITES where the decl's {@code JavaType} exists.
 *       </li>
 * </ol>
 *
 * <p><b>The lane map</b> (one lane per channel READ point, never a whole method — the #614
 * code-quality MF-2; a lane that exists but was never RUN is not a receipt, LAW 82). See
 * {@code target/seat616-instruments/lanes-f9.py} (local):
 * <b>A</b> S22's {@code isThenChainTerminalCtor(rung.thenBranch())} → negated — the 15 firing rung
 * arms (5 per walk, {@code rule:DTCC_UnderlyingAssetName}) lose their {@code MapperS.of(…)} wrap and
 * the {@code ¬c1 ∧ c3NotWrapped} population gains one; <b>B</b> S21's terminal read → negated —
 * <b>the corpus DOES move here even though the arm is inert today</b>: {@code c3NotWrapped=true ∧
 * bareEnum=false} terminal arms flip from unwrapped to wrapped, <b>50 on each D11 walk and 37 on the
 * optimised walk (137 combined)</b> — {@code c3NotWrapped} true 180 / 180 / 167 less
 * {@code bareEnum} true 130 / 130 / 130, per walk, so the earlier "50 per walk / about 150 combined"
 * mixed an OFF-walk figure with a per-walk claim — and lane B therefore has a CORPUS receipt and
 * needs no fixture; <b>C</b> S19's {@code isThenChainTerminalCtor(arm)} → negated —
 * {@code isCtorArm} returns {@code c4NotWrapped} = true at 474 of 486 instead of false at all 486,
 * so lane C's receipt is also the corpus; <b>D</b> S20's
 * {@code isFilterPredicateCrTerminalChain(crPredChain)} → negated — the 12 per walk lose
 * {@code appendGet} and every FILTER_PREDICATE arrival with a non-CR-terminal {@code RThenExpr} body
 * gains one, so BOTH cell-B and cell-C byte locks must redden;
 * <b>E</b> the family-8 cross-family watch ({@code !…startsWith("MapperS.of(")}) — the CHARTER
 * defines it as a watch, and a watch that mutates nothing cannot fail, so {@code lanes-f9.py}
 * implements it as a REAL negation of that conjunct at the ONE seat where it is reached (S22's 15
 * arrivals; the two mirrors are unreachable at 1,146/1,146 and 486/486 and are excluded on purpose).
 * <b>The lane must therefore go RED</b>; what "stays green" is the family-8 population under the
 * BASELINE and under lanes A–D. A GREEN lane E means the neighbour is no longer reached — the swap
 * moved a family-8 population — and the seat re-charters. Its premise is {@code c1 ⟹ c2} at 15/15
 * plus {@code c1} FALSE at 486/486 and 1,146/1,146;
 * <b>F</b> family 8's {@code .asMapper()} consumer in {@code FunctionExpressionRenderer} (ONE ledger
 * row, TWO occurrences) — same construction, so <b>RED</b> is the pass: S20 deletes a CONSUMER and
 * touches none of the marker's NINETEEN producers, and a GREEN would mean a producer moved;
 * <b>G</b> {@code JavaStatementScope.thenArgRefFor}
 * → always {@code null}, <b>REQUIRED</b> — the family's positive control and the ONLY receipt that
 * the 39 KEPT rows read a LIVE channel rather than a vacuous one. Expect the whole D11 set to move;
 * record the failing class count, do not chase it. Every lane run must also select
 * {@code MapperWrapPrefixSeatTest} (family 8) and {@code CollapseGetSuffixSeatTest} (family 7).
 *
 * <p><b>THE CITABLE RUN — lane2 at the final code head {@code a4a3c630d}</b>
 * ({@code target/seat616-instruments/logs/lane2.status}; baseline first, then mutate → run the
 * 41-class generator selection + the 4-class optimised module → restore per lane, with
 * {@code git-status} EMPTY at both ends and the anchor check clean before and after every lane):
 * baseline <b>838 run / 0F / 0E / 31 skip</b> + optimised 12/0F;
 * <b>A 46F</b> over 20 classes + optimised 12/1F (this suite 24/5F — {@code a1}, {@code control0},
 * {@code control3}, {@code corpus_c1_01} and {@code corpus_c1_06} — with
 * {@code MapperWrapPrefixSeatTest} 21/3F, {@code CollapseGetSuffixSeatTest} 22/4F and seventeen
 * further seat suites);
 * <b>B 16F</b> over 11 classes + optimised 12/1F (this suite 24/3F = {@code control0},
 * {@code control3} and <b>{@code corpus_c1_06}</b>; {@code DefaultRightNestedThenHoistSeatTest} 2F,
 * {@code InLambdaThenChainCtlAdmitSeatTest} 2F, {@code RuleThenArmLadderNestedTreeAdmitSeatTest} 2F,
 * and 1F each in {@code AliasCondLadderChoiceJoinSeatTest},
 * {@code LadderBlockCtorNestedExtractSeatTest}, {@code DeepBareFunctionThenChainAdmitSeatTest},
 * {@code NavTailExtractLadderNestedTreeAdmitSeatTest}, {@code ThenArgDeclKindFromCompiledSeatTest},
 * {@code WrapperLadderKeepsCollapseSeatTest} and {@code SwitchBodyThenChainAdmitSeatTest};
 * {@code MapperWrapPrefixSeatTest} 21/0F — family 8 does not move under this lane);
 * <b>C 5F</b> over 3 classes + optimised 12/0F ({@code DeepThenChainComposeTest} 2F = the csa margin
 * and csa valuation {@code UniqueTransactionIdentifierRule} locks; this suite 24/2F =
 * {@code control0} and <b>{@code corpus_c1_07}</b>; {@code MapperWrapPrefixSeatTest} 21/1F = its
 * cell-B {@code BlockTradeElectionIndicatorRule} lock);
 * <b>D 4F</b> over 1 class + optimised 12/0F — this suite ALONE, and it is BOTH S20 cells as the row
 * above charts: {@code b1}, {@code control0}, {@code corpus_c1_03} (drr 7.0.0) and
 * {@code corpus_c1_04} (cdm 5.38.0);
 * <b>E 4F</b> over 2 classes + optimised 12/0F — <b>RED, which is the pass</b>, and its family-8
 * witnesses are named: {@code MapperWrapPrefixSeatTest.corpus_c1_03} (family 8's own drr 6.34.1
 * {@code DTCC_UnderlyingAssetNameRule} whole-file byte lock) and this suite's
 * {@link #control1_familyEightsNeighbouringRowsAreUntouched} at the rung conjunct (count 1 → 0),
 * with {@code a1} and {@code corpus_c1_01} beside them;
 * <b>F 1F</b> over 1 class + optimised 12/0F — <b>RED, which is the pass</b>, and the one failing
 * witness is the single family-8 {@code .asMapper()} CONSUMER assertion inside
 * {@link #control1_familyEightsNeighbouringRowsAreUntouched} (two occurrences → zero). Nothing else
 * in the 41-class selection moves, which is precisely the cross-family clearance the lane exists to
 * print: S20 deletes a CONSUMER of the marker and none of its NINETEEN producers moves with it;
 * <b>G 179F</b> over 28 classes + optimised 12/1F — the REQUIRED family positive control (this
 * suite 24/10F, {@code MapperWrapPrefixSeatTest} 21/11F, {@code CollapseGetSuffixSeatTest} 22/8F,
 * {@code DeepThenChainComposeTest} 16/15F the largest single class); <b>record the class count, do
 * not chase it</b>. Every lane moved, in the direction its row above predicts, and every lane
 * recorded ZERO errors — the failures are assertions, not breakage. The optimised leg fails only
 * under A, B and G, all in {@code OptimisedNavigationEmissionTest.conversionLawsHoldPerCell}: under
 * A and B at the drr 5.61.0 conversion-event pin (6,218 → 6,526 and 6,218 → 6,260) and under G at a
 * different assertion entirely ({@code assertAliasSeamWitness}, cdm5 T3 PIPE).
 *
 * <p><b>What lane2 turned from projection into receipt, and what it discloses.</b>
 * {@link #corpus_c1_06_dtccLoadTypeRuleByteIdenticalLaneBCarrier} and
 * {@link #corpus_c1_07_utiCsaMarginRuleByteIdenticalLaneCCarrier} exist because lanes B and C had
 * reddened only in NEIGHBOURING suites — lane B through the whole-cell controls and through this
 * suite's {@code control3}, whose lane-B offender list is EXACTLY the two cell-A goldens
 * {@code DTCC_LoadTypeRule} and the esma {@code LoadTypeRule}; lane C through
 * {@code DeepThenChainComposeTest}'s two {@code UniqueTransactionIdentifierRule} locks in the same
 * drr 6.34.1 cell. That is what the #616 code-quality review's SF-8 called blindness. Both locks
 * post-date the SUPERSEDED first run (tag {@code lane}, at the seat commit, baseline 836), so under
 * it their redness was a well-founded PROJECTION and said so (LAW 82); <b>lane2 prints it</b> —
 * {@code corpus_c1_06} is in lane B's measured failing set and {@code corpus_c1_07} in lane C's, the
 * baseline moved 836 → 838 for exactly those two tests, and the projections are now RECEIPTS.
 * <b>Two things are disclosed rather than smoothed over.</b> First, neither lock is single-lane:
 * {@code corpus_c1_06} also reddens under A (the S22 negation moves that same cell-A golden) and
 * under G, and {@code corpus_c1_07} also reddens under G. Each is still GREEN under every OTHER
 * swap's lane, so each still discriminates its own channel — but "green under every other lane"
 * would have been false and is not written. Second, {@code control0} is in lanes A–D's failing sets
 * because those lanes prefix {@code !} to a read it asserts present — an applied-the-lane signal,
 * never a corpus one; it is absent from E, F and G, which mutate other files or other conjuncts.
 *
 * <p><b>The corpus carriers, from the census and re-verified against the goldens.</b> The whole-file
 * byte locks are chosen from the census's own carriers, never the triage's. S22's deciding form
 * lives in exactly FIVE golden files — {@code DTCC_UnderlyingAssetNameRule.java} in drr 6.34.1,
 * 6.35.0, 6.36.0, 6.37.0 and 6.38.0, three {@code return MapperS.of(UnderlyingAssetNameReport
 * .builder()} wraps each, of which ONE per file is the drained then-chain terminal ctor and TWO are
 * bare {@code RConstructorExpr} rungs. That arithmetic reproduces the census exactly: 5 deciding
 * arrivals per walk (the 15 combined) and 10 per walk of the 249 {@code ctorArm} class, which is the
 * figure the census log-grepped for this rule. S20's 36 come from two functions in two different
 * corpora — {@code BuildReportableJurisdictionInformation} (drr 7.x, 8 per walk) and
 * {@code CheckEligibilityByDetails} (cdm 5.x, 4 per walk) — <b>so the S20 lock is TWO CELLS and is
 * not single-cell blind</b> (the LAW-75 lesson).
 *
 * <p><b>The cells, and the one documented deviation from the charter.</b> Cell A is drr 6.34.1 (the
 * S22 carrier and the 249-class carrier), cell B drr 7.0.0 (S20's larger carrier) and cell C
 * cdm 5.38.0 (S20's cdm carrier). The charter asks for a fourth "cell-B" lock in a cell none of the
 * four carriers touches; that lock is instead {@link #CARRIER_OUT_OF_REACH_C}, a cdm 5.38.0 file
 * outside every one of the four sites' reach — cdm 5.38.0 carries NO S19/S21/S22 carrier at all
 * (the {@code .build()} tail's deciding form is drr 6.x-only) and this file carries neither
 * {@code .asMapper().get()} nor a ctor-arm ladder wrap. The deviation is a cost decision (a fourth
 * whole-cell generation in {@code @BeforeAll}) and is recorded here rather than left implicit; it
 * costs the cell-level claim and keeps the file-level one, which is what the control actually
 * asserts.
 *
 * <p><b>The SECOND documented deviation: the S22 lock shares a golden with family 8's suite.</b>
 * The charter's cross-family protection 5 asks that the S22 byte lock reuse a <i>different</i>
 * golden in the same cell "where possible", so that one file's change cannot mask two families at
 * once — and {@link #CARRIER_LADDER_DRAINED_CTOR} is the same drr 6.34.1 path that
 * {@code MapperWrapPrefixSeatTest.CARRIER_LADDER_CTOR} locks. It is not possible here: S22's
 * deciding form lives in exactly five goldens (the same rule in drr 6.34.1, 6.35.0, 6.36.0, 6.37.0
 * and 6.38.0), so cell A carries NO second golden with that form, and moving the lock to another
 * drr cell would cost a whole further generation in {@code @BeforeAll} — the same cost decision
 * recorded above for cell B. The residual masking risk is stated rather than hidden: a change to
 * that ONE file reddens both suites together, so a family-8 regression and a family-9 regression
 * are not independently distinguishable AT THAT FILE. Two things bound it — the other three
 * deletions are locked at three different files in two other cells, and
 * {@link #CARRIER_LADDER_BARE_CTOR} (the 249-class lock, same cell, a different rule and not in
 * family 8's lock set) is the file-level control that stays green under every CHANNEL lane —
 * measured green under A–F at lane2, and red under G with the rest of the corpus.
 *
 * <p><b>Fixtures.</b> Reduced fixtures are named {@code a*} (the {@code .build()} tail mirrors),
 * {@code b*} (the {@code .asMapper()} tail), {@code c1} (the two ZERO-ARRIVAL rows S11/S12) and
 * {@code d1} (S29's decline). Lanes B and C have CORPUS receipts by measurement, so {@code a3} and
 * {@code a4} are decline locks rather than lane receipts and say so at their own javadoc — a
 * fixture that cannot fail under its lane is not silently counted as that lane's witness (the #615
 * MF-1 lesson: a channel's own suite sitting green under its own lane is the signature of a witness
 * that is not on the channel).
 */
class ThenBindingOverlaySeatTest {

    private static final Path REPO_ROOT =
            Path.of(System.getProperty("user.dir")).resolve("..").normalize();

    private static final List<Path> BUILTINS_SEARCH_ROOTS = List.of(
            REPO_ROOT.resolve("test-corpus/rune-dsl-builtins/rune-runtime/src/main/resources/model"),
            REPO_ROOT.resolve("rune-dsl/rune-runtime/src/main/resources/model"),
            REPO_ROOT.resolve("rune-runtime/src/main/resources/model"));

    /**
     * The reduced-fixture gate — the house-wide builtins predicate, deliberately NOT routed through
     * {@link Drr7Corpus} (the #615 code-quality review's NIT-10 recorded the same choice): under
     * {@code -Dcorpus.required=true} the reduced fixtures still SKIP silently if the rune-dsl
     * builtins are absent, where the corpus locks fail loudly. It matters here because {@code b1}
     * and {@code c1} are the only unit-level witnesses of their seats, so the chain's per-class run
     * line — not a green exit code — is where their having actually RUN is proven.
     */
    static boolean builtinsAvailable() {
        return BUILTINS_SEARCH_ROOTS.stream().anyMatch(Files::isDirectory);
    }

    /** Cell A — drr 6.34.1: S22's deciding carrier and the 249-class {@code ctorArm} carrier. */
    private static final Path CELL_A_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path GOLDEN_A = CELL_A_ROOT.resolve("rosetta-source/src/generated/java");

    static boolean cellAAvailable() {
        return Files.isDirectory(GOLDEN_A)
                && Files.isDirectory(CELL_A_ROOT.resolve("rosetta-source/src/main/rosetta"));
    }

    /**
     * Cell B — drr 7.0.0: S20's larger carrier ({@code BuildReportableJurisdictionInformation}, 8 of
     * the 12 arrivals per walk). Gated through {@link Drr7Corpus} so a local gating run
     * ({@code -Dcorpus.required=true}) can never pass by silently skipping the lock.
     */
    private static final Path CELL_B_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path GOLDEN_B = CELL_B_ROOT.resolve("rosetta-source/src/generated/java");

    static boolean drr7Available() {
        return Drr7Corpus.gate(Files.isDirectory(GOLDEN_B)
                && Files.isDirectory(CELL_B_ROOT.resolve("rosetta-source/src/main/rosetta")),
                ThenBindingOverlaySeatTest.class);
    }

    /**
     * Cell C — cdm 5.38.0: S20's cdm carrier ({@code CheckEligibilityByDetails}, 4 of the 12 per
     * walk) and the out-of-reach lock. Two cells for ONE seat is the point — the LAW-75 lesson that
     * a single-cell lock is blind both ways.
     */
    private static final Path CELL_C_ROOT = Path.of("../test-corpus/cdm/cdm-5.38.0");
    private static final Path GOLDEN_C = CELL_C_ROOT.resolve("rosetta-source/src/generated/java");

    static boolean cellCAvailable() {
        return Files.isDirectory(GOLDEN_C)
                && Files.isDirectory(CELL_C_ROOT.resolve("rosetta-source/src/main/rosetta"));
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

    static boolean cellCAndIrProviderAvailable() {
        return cellCAvailable() && irProviderOnClasspath();
    }

    /**
     * <b>S22's ONLY deciding carrier.</b> All 15 of the census's {@code c1TerminalCtor=true}
     * arrivals are {@code rule:DTCC_UnderlyingAssetName} (5 per walk), and the golden proves the
     * arithmetic: this file carries THREE {@code return MapperS.of(UnderlyingAssetNameReport
     * .builder()} wraps, of which the third — the one preceded by the {@code _thenArg0/_thenArg1}
     * drained hoists — is the then-chain terminal ctor the retained AST predicate admits, while the
     * first two are bare constructor arms the {@code instanceof RConstructorExpr} disjunct already
     * covers. Both polarities of the swap in ONE file.
     */
    private static final String CARRIER_LADDER_DRAINED_CTOR =
            "drr/regulation/csa/rewrite/dtcc/reports/DTCC_UnderlyingAssetNameRule.java";
    /**
     * <b>The 249 class, locked.</b> {@code rule:PackageTransactionSpread} is the largest of the six
     * carriers of the census's 249 converse failures (27 per walk of the 83). Every one of those
     * arrivals carries {@code ctorArm=true}, so the disjunct above the swapped one already sets
     * {@code thenWrap} and this lock is expected GREEN under every one of the four SWAP lanes and
     * the two cross-family watches — that is precisely its purpose: it is the corpus counterpart of
     * {@code a2}, the witness that deleting the tail read does not WIDEN the arm. It is a statement
     * of what golden says, not a sensitivity witness, and it is labelled so rather than being
     * counted as one. <b>Measured at lane2: green under A, B, C, D, E and F, red under G alone</b>
     * — G is the family positive control and moves the whole D11 set by construction, so "GREEN
     * under EVERY lane" was too strong and is corrected here rather than left standing.
     */
    private static final String CARRIER_LADDER_BARE_CTOR =
            "drr/regulation/csa/rewrite/trade/reports/PackageTransactionSpreadRule.java";
    /** <b>S20, cell B</b> — 8 of the 12 arrivals per walk. */
    private static final String CARRIER_CR_CHAIN_B =
            "drr/ingest/fpml/recordkeeping/reportableinfo/functions/"
            + "BuildReportableJurisdictionInformation.java";
    /** <b>S20, cell C</b> — the charter's named cdm 5.38.0 carrier, 4 of the 12 per walk. */
    private static final String CARRIER_CR_CHAIN_C =
            "cdm/product/collateral/functions/CheckEligibilityByDetails.java";
    /**
     * The OUT-OF-REACH lock: a then-chain-bearing cdm 5.38.0 function that carries neither
     * {@code .asMapper().get()} nor a ctor-arm ladder wrap, in a cell with no S19/S21/S22 carrier at
     * all. It proves the four deletions are inert outside their measured reach.
     */
    private static final String CARRIER_OUT_OF_REACH_C =
            "cdm/observable/asset/functions/FilterPrice.java";
    /**
     * <b>LANE B's own corpus receipt, in cell A</b> (the #616 code-quality review's SF-8: lanes B
     * and C are declared "the receipt is the CORPUS, not a fixture", and until this lock the seat's
     * own suite carried no S21-movement witness at all). Chosen from a lane run rather than from
     * the census's arrival list, because arrivals are not movements: under lane B this file is one
     * of the TWO cell-A goldens {@code control3} names in its own offender list — the negated
     * terminal predicate turns {@code elseWrap} on, the {@code mapperC && elseWrap} decline fires,
     * and the ladder falls back to the inline runtime {@code .then(} form. <b>The citable lane2 run
     * confirms both halves</b>: {@code control3}'s lane-B offender list is exactly
     * {@code DTCC_LoadTypeRule} and the esma {@code LoadTypeRule}, and the lock built on the first
     * of them is itself in lane B's failing set. The review's suggested
     * {@code Counterparty2IdentifierFormat} lock is REFUTED as a lane-B receipt and deliberately not
     * taken: {@code LadderArmEnumExpectedOwnerSeatTest}'s drr 7.0.0 hkma lock on that rule is in the
     * same lane selection and stayed GREEN under lane B in that run too (it moves only under G).
     */
    private static final String CARRIER_LADDER_TERMINAL_LANE_B =
            "drr/regulation/csa/rewrite/dtcc/reports/DTCC_LoadTypeRule.java";
    /**
     * <b>LANE C's own corpus receipt, in cell A</b> — the same SF-8 gap on the S19 side, and here
     * the review's named carrier class is CONFIRMED by the lane run: {@code rule:
     * UniqueTransactionIdentifier} is S19's largest census carrier (40 of the 162 arrivals per
     * walk) and both {@code DeepThenChainComposeTest} locks on it — csa margin and csa valuation,
     * the same drr 6.34.1 cell — are in lane C's failing set. This lock brings one of the two into
     * the seat's OWN suite so a lane-C green is diagnosable here rather than only in a neighbour's.
     * <b>At lane2 that is the whole of lane C</b>: those two sibling locks, this suite's
     * {@code control0} and {@code corpus_c1_07}, and family 8's cell-B
     * {@code BlockTradeElectionIndicatorRule} — five failures over three classes and nothing else.
     */
    private static final String CARRIER_CTOR_ARM_LANE_C =
            "drr/regulation/csa/rewrite/margin/reports/UniqueTransactionIdentifierRule.java";

    private static final List<String> CARRIERS_A =
            List.of(CARRIER_LADDER_DRAINED_CTOR, CARRIER_LADDER_BARE_CTOR,
                    CARRIER_LADDER_TERMINAL_LANE_B, CARRIER_CTOR_ARM_LANE_C);
    private static final List<String> CARRIERS_C =
            List.of(CARRIER_CR_CHAIN_C, CARRIER_OUT_OF_REACH_C);

    /** The swapped generator source — {@code control0} reads it directly. */
    private static final Path COLLECTION_HANDLER = REPO_ROOT.resolve(
            "rune-java-generator/src/main/java/com/regnosys/rosetta/generator/java/expression/"
            + "handlers/CollectionHandler.java");
    private static final Path FUNCTION_EXPRESSION_RENDERER = REPO_ROOT.resolve(
            "rune-java-generator/src/main/java/com/regnosys/rosetta/generator/java/function/"
            + "FunctionExpressionRenderer.java");

    // =========================================================================
    // Part A — the reduced fixtures
    // =========================================================================

    private static final String MODEL = """
            namespace census.seat616f9
            version "1.0.0"

            enum MarkSourceEnum:
                NAME
                OTHER

            type Mark:
                name string (0..1)
                kind MarkSourceEnum (0..1)

            type MarkReport:
                assetName string (0..*)

            type Leg:
                mark string (0..1)
                nums int (0..*)
                marks Mark (0..*)

            type Root:
                legs Leg (0..*)
                tags string (0..*)

            reporting rule A1LadderCtorArms from Root: <"a1 and a2 - seat S22: a ladder whose LAST rung is a drained then-chain terminal ctor, beside two BARE constructor rungs">
                filter legs exists
                then extract
                    if legs -> mark exists
                    then MarkReport { assetName: legs -> mark }
                    else if legs -> nums exists
                    then MarkReport { assetName: legs -> marks -> name }
                    else if legs -> marks exists
                    then (legs -> marks
                        then filter kind = MarkSourceEnum -> NAME
                        then MarkReport {
                                assetName: name
                            })

            reporting rule A3LadderEnumTerminal from Root: <"a3 - seat S21: the ladder TERMINAL takes the bare-enum wrap and the retired tail read never decides">
                filter legs exists
                then extract
                    if legs -> mark exists
                    then MarkSourceEnum -> NAME
                    else MarkSourceEnum -> OTHER

            reporting rule A4MultiBlockCondArms from Root: <"a4 - seat S19: the MULTI conditional-block arms consult isCtorArm and both decline, exactly as the census measured at 486 of 486">
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

            func IsNamedMark: <"b2's non-ComparisonResult terminal - a boolean FUNCTION call">
                inputs:
                    marks Mark (0..*)
                output:
                    named boolean (1..1)
                set named:
                    marks -> kind any = MarkSourceEnum -> NAME

            func B1FilterPredicateCrTerminalChain: <"b1 - seat S20: a filter predicate whose body is a CR-terminal then-chain re-roots exists(thenArgN).asMapper() and the arm coerces .get()">
                inputs:
                    inp Root (1..1)
                output:
                    result Leg (0..*)
                add result:
                    inp -> legs
                        filter [
                            item -> marks
                                then filter kind = MarkSourceEnum -> NAME
                                then exists
                        ]

            func B2FilterPredicateNonCrChain: <"b2 - seat S20's decline: a filter predicate whose body IS an RThenExpr but whose terminal is a boolean function call, not a ComparisonResult">
                inputs:
                    inp Root (1..1)
                output:
                    result Leg (0..*)
                add result:
                    inp -> legs
                        filter [
                            item -> marks
                                then filter kind = MarkSourceEnum -> NAME
                                then IsNamedMark
                        ]

            func C1LolParamTypedSum: <"c1 - seats S11 and S12: the list-of-lists closure param is stamped MapperC<E> from the bound receiver, so `l sum` recovers the TYPED sum">
                inputs:
                    inp Root (1..1)
                output:
                    result int (0..*)
                add result:
                    inp -> legs
                        then extract [ item -> nums ]
                        then extract l [ l sum ]

            reporting rule C1bLolFilterParamStamp from Root: <"c1b - seat S12: the RULE-path list-of-lists FILTER admission stamps its closure param from the same wrapper-keyed binding the admission read">
                extract legs
                then extract [ item -> nums ]
                then filter l [ l sum > 0 ]
                then flatten

            reporting rule D1ImplicitOperandWorkspaceTyped from Root: <"d1 - seat S29: an implicit-item arithmetic operand whose workspace inference is POPULATED - the gate short-circuits before the binding fallback">
                extract tags
                then extract item + "-X"

            """;

    /**
     * a1 — <b>S22, the firing polarity</b>. A faithful miniature of the census's only deciding
     * carrier: three ladder rungs, of which the LAST is a then-chain whose terminal body is a
     * constructor (the drained form, {@code _thenArg} hoists then a bare built ctor) and the first
     * two are bare constructor arms. All three take the {@code MapperS.of(…)} wrap, and after the
     * swap the LAST one takes it on {@code isThenChainTerminalCtor} alone. Under lane A the gate
     * closes and the drained arm loses its wrap, so the count falls from three to two.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_drainedThenChainCtorRungKeepsItsWrapOnTheAstConjunctAlone() throws IOException {
        String code = codeOnly(rule("A1LadderCtorArmsRule.java"));
        assertEquals(3, count(code, "MapperS.of(MarkReport.builder()"),
                "S22/a1: all three ladder rungs must wrap their built constructor MapperS.of(...) —"
                + " two through the `instanceof RConstructorExpr` disjunct and the LAST through"
                + " isThenChainTerminalCtor, which is now the whole drain test");
        assertContains(code, "final MapperC<Mark> thenArg0");
    }

    /**
     * a2 — <b>the 249 class, reduced</b>: the two BARE constructor rungs of the same fixture. They
     * are covered by the {@code instanceof RConstructorExpr} disjunct ABOVE the swapped one, which
     * is why the census's 249 converse failures (every one {@code ctorArm=true}) are not widened by
     * the deletion. The discriminator against a1 is the DRAIN: only the then-chain rung hoists.
     *
     * <p><b>What this test actually asserts is the DRAIN COUNT alone</b>, which is why it is named
     * for that and no longer for the coverage half. The "covered by the disjunct above" half is
     * asserted by {@code a1}'s {@code assertEquals(3, …)}: three wraps survive while only one rung
     * drains, so the other two took the wrap through a disjunct the swap did not touch. Split
     * across two tests deliberately — a1 moves under lane A and this one must not.
     *
     * <p><b>Lane-INVARIANT, and therefore not any lane's receipt</b> — the same disclaimer a3 and a4
     * carry, and it is measured rather than argued: a2 is absent from the failing set of every one
     * of the seven lanes. Negating S22's predicate (lane A) changes which disjunct admits the
     * drained rung, never whether it drains, so the hoist count this pins is untouched. Its job is
     * to keep a1's denominator honest, not to witness sensitivity.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_exactlyOneOfTheThreeCtorRungsDrainsItsThenChain() throws IOException {
        String code = codeOnly(rule("A1LadderCtorArmsRule.java"));
        assertEquals(1, count(code, "final MapperC<Mark> thenArg0"),
                "S22/a2: exactly ONE of the three rungs drains a then-chain — the other two are bare"
                + " constructor arms, the 249 class the disjunct above already admits");
    }

    /**
     * a3 — <b>S21's decline lock.</b> The ladder TERMINAL takes the bare-enum wrap through the
     * disjunct above the swapped one, and the retired tail read never decides there: the census
     * measured {@code isThenChainTerminalCtor(lvl.terminal.get())} and the tail BOTH false at
     * 1,146/1,146.
     *
     * <p><b>This fixture is NOT lane B's receipt and must not be quoted as one.</b> Lane B negates
     * the terminal's AST predicate, and for a BARE ENUM terminal the disjunct above already sets
     * {@code elseWrap}, so this fixture cannot move. Lane B's receipt is the CORPUS, and it is
     * measured: {@code c3NotWrapped=true ∧ bareEnum=false} terminal arms flip from unwrapped to
     * {@code MapperS.of(…)}-wrapped — <b>50 on each D11 walk and 37 on the optimised walk, 137
     * combined</b> ({@code c3NotWrapped} true 180 / 180 / 167 less {@code bareEnum} true
     * 130 / 130 / 130). The seat's own corpus lock for it is
     * {@link #corpus_c1_06_dtccLoadTypeRuleByteIdenticalLaneBCarrier}.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a3_bareEnumLadderTerminalWrapsThroughTheDisjunctAbove() throws IOException {
        String code = codeOnly(rule("A3LadderEnumTerminalRule.java"));
        assertContains(code, "return MapperS.of(MarkSourceEnum.OTHER);");
        assertContains(code, "return MapperS.of(MarkSourceEnum.NAME);");
    }

    /**
     * a4 — <b>S19's decline lock.</b> The MULTI conditional-block seat — the {@code if (multi)}
     * block in {@code CollectionHandler.compileEffectiveElseConditionalBlock}, named rather than
     * line-cited because the range once written here has already drifted twice — is the ONLY caller
     * of {@code isCtorArm}, and it consults it for both arms. The census measured the method
     * returning false at 486/486 — its three conjuncts all false — so this fixture pins the arm the
     * DECLINE produces.
     *
     * <p><b>Not lane C's receipt either</b>, for the same reason a3 is not lane B's: lane C's
     * measured receipt is the corpus (inverting the read makes {@code isCtorArm} return
     * {@code c4NotWrapped}, true at 474 of 486 arrivals, so every ctor-arm consumer changes arm).
     * The seat's own corpus lock for it is
     * {@link #corpus_c1_07_utiCsaMarginRuleByteIdenticalLaneCCarrier}.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a4_multiConditionalBlockArmsDeclineIsCtorArm() throws IOException {
        String code = codeNoComments(rule("A4MultiBlockCondArmsRule.java"));
        assertContains(code, "MapperC.of(Collections.singletonList(");
        assertAbsent(code, "MapperC.of(Collections.singletonList(MapperS.of(");
    }

    /**
     * b1 — <b>S20, the firing polarity.</b> A faithful miniature of {@code CheckEligibilityByDetails}
     * (a filter whose predicate body is {@code <nav> then filter <pred> then exists}): the chain
     * restructures, re-roots {@code exists(thenArgN).asMapper()}, and the {@code #396} arm coerces
     * it {@code .get()}. After the swap that arm is decided by
     * {@code isFilterPredicateCrTerminalChain} alone — the tail the deleted read tested is emitted
     * by the very re-root the predicate admits, which is why the two agreed at 122,731/122,731.
     * Under lane D this fixture loses its {@code .get()}.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b1_crTerminalChainFilterPredicateStillCoercesGet() throws IOException {
        String code = codeOnly(fn("B1FilterPredicateCrTerminalChain.java"));
        assertContains(code, ".asMapper().get()");
    }

    /**
     * b2 — <b>S20's decline polarity.</b> The same filter-predicate shape with the SAME then-chain,
     * but a terminal that is a boolean FUNCTION call rather than a ComparisonResult, so
     * {@code isFilterPredicateCrTerminalChain} declines at its terminal-class clause. The
     * {@code #396} arm must not fire, and no {@code .asMapper()} re-root is emitted — the negative
     * side of the census's 122,695 F/F arrivals, at the clause that produces most of them.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b2_nonCrTerminalChainFilterPredicateDeclinesTheArm() throws IOException {
        String code = codeOnly(fn("B2FilterPredicateNonCrChain.java"));
        assertAbsent(code, ".asMapper().get()");
    }

    /**
     * c1 — <b>the S11/S12 attempt, and it FIRES.</b> These are the family's only two rows with no
     * measurement of their own: {@code lambdaParamBoundListType}'s extract and filter legs have ZERO
     * arrivals on all three walks, while the instrument fired everywhere else in the same file (S05
     * 144,238, S06 73,372, S09 150), so the zero is the CORPUS, not the probe. A disposition on zero
     * measurement is the one thing the #609–#615 method forbids, so the charter requires an attempt
     * at a reduced fixture that enters the method.
     *
     * <p>The shape is the {@code #430}/{@code #432} list-of-lists lambda: a {@code then extract}
     * producing a list-of-lists, then a NAMED closure param over it. The assertion is the
     * discriminating one, not a shape check — {@code lambdaParamBoundListType} stamps the raw param
     * read with the {@code MapperC<E>} the runtime binds, and it is that stamp which lets the
     * in-lambda typed-sum recovery pick {@code sumInteger()}; a null stamp (the {@code #430} untyped
     * form) falls to the non-existent generic {@code sum()}. So {@code .sumInteger()} in the render
     * is a witness that the seat was ENTERED and answered.
     *
     * <p><b>The attempt SUCCEEDED, and it needed TWO fixtures because the two legs have opposite
     * scoping laws</b> — which is itself the explanation of the corpus zero. The extract leg (S11)
     * reads {@code boundImplicitReceiverType(RExtractExpr, scope)}, which delegates to
     * {@code boundImplicitReceiverType0} and DECLINES on the rule path by design (hazard H6, the
     * cp4b catch at {@code CollectionHandler:1518-1524}); the filter leg (S12) reads the 3-arg
     * overload, whose LoL admission fires ONLY when {@code findEnclosingRule(expr) != null} (the
     * seat-29 law-3 rung A at {@code CollectionHandler:1501-1508}). So S11 is reachable only from a
     * FUNCTION and S12 only from a RULE, and no single fixture can carry both. This one is the
     * function; {@link #c1b_listOfListsFilterClosureParamCarriesItsBoundElementType} is the rule.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void c1_listOfListsClosureParamCarriesItsBoundElementType() throws IOException {
        String code = codeOnly(fn("C1LolParamTypedSum.java"));
        assertContains(code, ".sumInteger()");
    }

    /**
     * c1b — <b>the S12 half of the same attempt</b>, on the RULE path, where the wrapper-keyed
     * filter admission is the ONE exception to the function-path decline. Same discriminating
     * assertion: the closure param's stamp is what lets {@code l sum} pick the typed
     * {@code sumInteger()}, so its presence witnesses that the filter leg was entered and answered.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void c1b_listOfListsFilterClosureParamCarriesItsBoundElementType() throws IOException {
        String code = codeOnly(rule("C1bLolFilterParamStampRule.java"));
        assertContains(code, ".sumInteger()");
    }

    /**
     * d1 — <b>S29's decline lock, and the positive polarity is NOT drawable.</b>
     * {@code HandlerHelper.implicitThenBindingItemType} consults the workspace FIRST and returns
     * null whenever the inference is present; the census measured 135 entries, 135 workspace
     * declines and ZERO reads of the binding fallback, with {@code ws = missing=false /
     * hasMeta=false / type=RStringType} at 270/270 over three string-concatenation carriers
     * ({@code rule:TechnicalRecordId}, {@code fn:MessageID},
     * {@code rule:TechnicalRecordIdentification}).
     *
     * <p><b>LAW 72, recorded rather than fudged:</b> the positive polarity would need an arrival
     * whose {@code getInferredType(operand).isMissing()} is TRUE — the {@code #351} sentinel — which
     * is exactly the front-end gap the row's javadoc names at {@code HandlerHelper:1056-1058}.
     * Producing it is a PARSER change, not a fixture, so this witness is decline-only and says so.
     * Its discriminating lane is G (the family-wide {@code thenArgRefFor} → null control), under
     * which the whole D11 set moves.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void d1_implicitOperandWithAPopulatedWorkspaceTypeTakesTheWorkspaceAnswer() throws IOException {
        String code = codeNoComments(rule("D1ImplicitOperandWorkspaceTypedRule.java"));
        assertContains(code,
                "MapperMaths.<String, String, String>add(item, MapperS.of(\"-X\"))");
    }

    // =========================================================================
    // Part B — the whole-file corpus byte locks
    // =========================================================================

    /** S22's deciding carrier, and the 249 class in the same file. */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_01_dtccUnderlyingAssetNameRuleByteIdentical() throws IOException {
        lockA(CARRIER_LADDER_DRAINED_CTOR);
    }

    /**
     * The 249 class's largest carrier — GREEN under all six channel lanes by construction, and
     * measured so at lane2 (it reddens only under G, the family positive control).
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_02_packageTransactionSpreadRuleByteIdentical() throws IOException {
        lockA(CARRIER_LADDER_BARE_CTOR);
    }

    /** S20, cell B — 8 of the 12 arrivals per walk. Must redden under lane D. */
    @Test
    @EnabledIf("drr7Available")
    void corpus_c1_03_buildReportableJurisdictionInformationByteIdenticalCellB() throws IOException {
        lockB(CARRIER_CR_CHAIN_B);
    }

    /** S20, cell C — the charter's cdm 5.38.0 carrier, 4 of the 12. Must redden under lane D. */
    @Test
    @EnabledIf("cellCAvailable")
    void corpus_c1_04_checkEligibilityByDetailsByteIdenticalCellC() throws IOException {
        lockC(CARRIER_CR_CHAIN_C);
    }

    /** The out-of-reach lock — a then-chain function no swap site reaches. */
    @Test
    @EnabledIf("cellCAvailable")
    void corpus_c1_05_filterPriceByteIdenticalCellC() throws IOException {
        lockC(CARRIER_OUT_OF_REACH_C);
    }

    /**
     * <b>S21, and LANE B's receipt inside this suite — PRINTED, no longer projected.</b> See
     * {@link #CARRIER_LADDER_TERMINAL_LANE_B}. The citable lane2 run at {@code a4a3c630d} has this
     * test in lane B's failing set (16F over 11 classes): with the terminal predicate negated the
     * file emits the runtime {@code .then(} form golden does not carry. It also reddens under lane A
     * — the S22 negation moves the same cell-A golden — and under the family-wide positive control
     * G, and it is GREEN under C, D, E and F, so it discriminates S21 against the three other swaps,
     * which is what the lock is for. The earlier "green under every other lane" is REFUTED by the
     * run and is not restated.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_06_dtccLoadTypeRuleByteIdenticalLaneBCarrier() throws IOException {
        lockA(CARRIER_LADDER_TERMINAL_LANE_B);
    }

    /**
     * <b>S19, and LANE C's receipt inside this suite — PRINTED, no longer projected.</b> See
     * {@link #CARRIER_CTOR_ARM_LANE_C}. The citable lane2 run has this test in lane C's failing set
     * (5F over 3 classes), beside the two sibling {@code DeepThenChainComposeTest} locks on the same
     * rule that first exposed the gap. It is GREEN under A, B, D, E and F and reddens again only
     * under the family-wide positive control G.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_07_utiCsaMarginRuleByteIdenticalLaneCCarrier() throws IOException {
        lockA(CARRIER_CTOR_ARM_LANE_C);
    }

    // =========================================================================
    // Part C — the controls
    // =========================================================================

    /**
     * control0 — <b>the swap's exact shape, asserted against the generator source.</b> The four
     * retained AST reads are PRESENT at their seats and the four deleted text conjuncts are ABSENT
     * from the file. Its job is to fail loudly if a later edit re-introduces a tail read or moves
     * one of the four channels off its seat.
     *
     * <p><b>The three quoted needles read {@link #codeNoComments}, not {@link #codeOnly}, and the
     * reason is a MEASURED trap</b> (the #615 code-quality review documented it at
     * {@code MapperWrapPrefixSeatTest}'s b2 fixture and the copy here inherited it): {@code codeOnly}
     * deletes each string literal <em>including both quote characters</em>, so its output holds no
     * {@code "} at all, while each of these three needles carries two. Under that view the
     * {@code contains} can never be true and the assertion can never fail. Measured on the two
     * blobs, needles {@code thenStr}/{@code elseStr}/{@code armReturn}: at the PARENT
     * {@code e81fc62e2}, where all three tail reads are still present, {@code codeOnly} counts
     * {@code [0, 0, 0]} — the pre-swap tree PASSES the vacuous form — while {@code codeNoComments}
     * counts {@code [1, 1, 1]} and fails it; at this head both views count {@code [0, 0, 0]}. The
     * literal-preserving view is therefore the discriminating one, and the comments that quote the
     * retired conjuncts are stripped by it just the same. Verified there is no scanner hazard for
     * either view in {@code CollectionHandler.java}: zero {@code '"'} char literals, zero text
     * blocks. <b>The fourth needle keeps {@code codeOnly}</b> — it carries no quote, and it measures
     * 1 on the parent against 0 here, so it is discriminating under either view.
     *
     * <p><b>Not a lane receipt, and NOT lane-invariant either</b> — the earlier note here claimed it
     * was green under every lane "by construction", and the runs refute that: lanes A, B, C and
     * D each prefix {@code !} to a retained read, which breaks the corresponding positive
     * {@code contains} above, and control0 is RED in all four (it is absent from lanes E, F and G,
     * which mutate other files or other conjuncts). The citable lane2 run reproduces exactly that
     * membership — A, B, C, D red, each at the one assertion its own lane negates; E, F, G green.
     * That redness says the lane applied, not that the corpus moved, so control0 must still never be
     * quoted as a sensitivity witness for a lane; the corpus receipts are the byte locks and
     * control3.
     */
    @Test
    void control0_theFourTailReadsAreGoneAndTheirChannelsRemain() throws IOException {
        String src = Files.readString(COLLECTION_HANDLER);
        assertTrue(src.contains("|| (isThenChainTerminalCtor(rung.thenBranch())"),
                "S22: the rung arm must still consult isThenChainTerminalCtor");
        assertTrue(src.contains("|| (isThenChainTerminalCtor(lvl.terminal.get())"),
                "S21: the terminal must still consult isThenChainTerminalCtor");
        assertTrue(src.contains("|| (isThenChainTerminalCtor(arm)"),
                "S19: isCtorArm must still consult isThenChainTerminalCtor");
        assertTrue(src.contains("&& isFilterPredicateCrTerminalChain(crPredChain)) {"),
                "S20: the #396 arm must now END at isFilterPredicateCrTerminalChain — the tail"
                + " conjunct that followed it is deleted, so the predicate closes the condition");
        String literalPreserving = codeNoComments(src);
        for (String deleted : List.of("thenStr.endsWith(\".build()\")",
                "elseStr.endsWith(\".build()\")",
                "armReturn.endsWith(\".build()\")")) {
            assertTrue(!literalPreserving.contains(deleted),
                    "the retired tail read must be gone from the code (comments quoting it are"
                    + " fine and are stripped by this view too; codeOnly would delete the needle's"
                    + " own quotes and could never fail): " + deleted);
        }
        assertTrue(!codeOnly(src).contains("bodyExpr.renderToString().endsWith("),
                "S20's tail read must be gone from the code");
    }

    /**
     * control1 — <b>the cross-family presence check.</b> Family 8 (closed at PR #615) owns the
     * immediate right-hand neighbour of three of the four deleted conjuncts, and its
     * {@code .asMapper()} consumer sits in another file entirely. Nothing here may move: the swaps
     * delete a conjunct, they do not migrate a producer or restate a neighbour. Lanes E and F are
     * the RUNTIME receipts for the same property; this is the static one.
     */
    @Test
    void control1_familyEightsNeighbouringRowsAreUntouched() throws IOException {
        String src = Files.readString(COLLECTION_HANDLER);
        assertEquals(1, count(src, "&& !thenStr.startsWith(\"MapperS.of(\"))"),
                "family 8's rung conjunct must survive the S22 deletion, exactly once");
        assertEquals(1, count(src, "&& !elseStr.startsWith(\"MapperS.of(\"))"),
                "family 8's terminal conjunct must survive the S21 deletion, exactly once");
        assertEquals(1, count(src, "&& !armReturn.startsWith(\"MapperS.of(\"));"),
                "family 8's isCtorArm conjunct must survive the S19 deletion, exactly once");
        String fer = Files.readString(FUNCTION_EXPRESSION_RENDERER);
        assertEquals(2, count(fer, "!terminal.endsWith(\".asMapper()\")"),
                "family 8's .asMapper() CONSUMER (one ledger row, two occurrences) must be"
                + " untouched: S20 deletes a consumer and touches none of the marker's nineteen"
                + " producers");
    }

    /**
     * control2 — <b>LAW 77 route parity, cell A.</b> All 43 seats are shared handler code that both
     * IR modules reach by subclassing; neither {@code rune-ir-java} nor
     * {@code rune-ir-java-optimised} contains a single {@code thenArgRefFor} or {@code bindThenArg}
     * call. The census corroborates the precondition at every swapped seat: S19, S21, S22 and S20
     * all have IDENTICAL default-route and IR-route arrivals (162/162, 382/382, 5,787/5,787 and
     * 41,316/41,316), so byte-inertness is route-symmetric by measurement and not by argument.
     * Skips (recorded) without the IR provider on the classpath. The cell-A carrier set is FOUR
     * files since the code-quality review's SF-8 locks joined it, so the two lane-B/lane-C receipts
     * carry a route-parity claim as well as a golden one.
     */
    @Test
    @EnabledIf("cellAAndIrProviderAvailable")
    void control2_irRouteMatchesLegacyForEveryCellACarrier() throws IOException {
        assertNotNull(drrAOutput, "drr 6.34.1 legacy-route generation did not run");
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", CELL_A_ROOT), new ArrayList<>());
        for (String carrier : CARRIERS_A) {
            assertEquals(drrAOutput.get(carrier), irOut.get(carrier), "route divergence: " + carrier);
        }
    }

    /**
     * control2b — <b>LAW 77 route parity, cell C.</b> The cell-A twin covers the {@code .build()}
     * tail seats only; S20's carriers live in cells B and C, so without this the family's largest
     * single measurement would have its route parity resting on the chain's whole-matrix ROUTE ROW
     * DIFF alone — a gate outside this suite and outside every lane.
     */
    @Test
    @EnabledIf("cellCAndIrProviderAvailable")
    void control2b_irRouteMatchesLegacyForEveryCellCCarrier() throws IOException {
        assertNotNull(cdmCOutput, "cdm 5.38.0 legacy-route generation did not run");
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("cdm", "5.38.0", CELL_C_ROOT), new ArrayList<>());
        for (String carrier : CARRIERS_C) {
            assertEquals(cdmCOutput.get(carrier), irOut.get(carrier), "route divergence: " + carrier);
        }
    }

    /**
     * control3 — <b>the RESID/MARK keep, given a standing witness.</b> Four of the 39 kept rows
     * (S23, S24, S37, S40) read the runtime {@code .then(} residue and are MEASURED-INERT: the
     * residue is FALSE at every arrival of every walk — 162/162, 18,990/18,990, 3,452/3,452 and
     * 39,381/39,381, 61,985 combined arrivals with ZERO residues. Five carriers produce the runtime
     * form anywhere in the corpus and none of them reaches a consumer. This control turns that
     * census figure into a witness the tree carries: NO generated file in the locked cells may
     * contain the runtime {@code .then(} form — the same property {@code containsUnhoistedThen}
     * measures, asserted over the whole emitted set rather than at one seat. (Verified against the
     * goldens too: zero of cell A's own 6,675 goldens carries it. The 34,686 first written here is
     * the manifest's D11 golden total across ALL FIVE cells — 3,950 + 5,903 + 6,675 + 8,597 +
     * 9,561 — not one cell's slice.)
     */
    @Test
    @EnabledIf("cellAAvailable")
    void control3_noEmittedFileCarriesTheRuntimeThenResidue() {
        assertNotNull(drrAOutput, "drr 6.34.1 generation did not run");
        List<String> offenders = drrAOutput.entrySet().stream()
                .filter(e -> e.getValue() != null && e.getValue().contains(".then("))
                .map(Map.Entry::getKey)
                .limit(10)
                .toList();
        assertTrue(offenders.isEmpty(),
                "S23/S24/S37/S40 are kept as MEASURED-INERT on the census's 61,985 zero-residue"
                + " arrivals; an emitted runtime .then( form would refute that: " + offenders);
    }

    /**
     * control4 — <b>the retirement's own premise, positively controlled.</b> The swap rests on the
     * measured implication {@code isThenChainTerminalCtor ⟹ render ends ".build()"} (15/15, zero
     * counterexamples). At the {@code a1} fixture that implication must be VISIBLE: the drained arm
     * the AST predicate admits still renders a built constructor whose tail is {@code .build()}, so
     * the deleted conjunct would have admitted exactly the same arm. Without this, a1 would pass for
     * a seat that had started wrapping something else.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void control4_theAdmittedArmStillCarriesTheTailTheDeletedConjunctTested() throws IOException {
        String code = codeOnly(rule("A1LadderCtorArmsRule.java"));
        assertContains(code, ".build());");
        assertAbsent(code, ".then(");
    }

    /**
     * control5 — <b>golden is the oracle for the two forms the seats decide.</b> Every token is
     * form-specific and falsifiable; a bare {@code contains("MapperS.of(")} would be satisfied by
     * dozens of unrelated occurrences and would witness nothing. If one of these stops holding, the
     * carrier stopped being this family's carrier and the claims above are restated, not patched.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void control5_goldensCarryTheDecidedForms() throws IOException {
        String g = Files.readString(GOLDEN_A.resolve(CARRIER_LADDER_DRAINED_CTOR));
        assertEquals(3, count(g, "return MapperS.of(UnderlyingAssetNameReport.builder()"),
                "S22: the golden must carry THREE wrapped built constructors — ONE the drained"
                + " then-chain terminal ctor the retained predicate admits (the census's 5 per walk"
                + " over five drr 6.x cells = the 15) and TWO bare constructor arms the disjunct"
                + " above covers (the census's 10 per walk of the 249)");
        assertEquals(1, count(g, "final MapperC<ProductIdentifier> _thenArg0"),
                "S22: and exactly ONE of the three must DRAIN — the drained hoist is what makes it"
                + " the then-chain terminal ctor rather than a bare constructor arm");
        assertTrue(!g.contains(".then("),
                "S22: the carrier must not carry the runtime .then( form — the undrained shape the"
                + " deleted tail read existed to exclude has zero carriers in the whole corpus");
    }

    /**
     * control5b — golden is the oracle for S20's decided form, in BOTH carrier cells. The
     * {@code .asMapper()} tail is emitted by the CR-terminal chain's own re-root and the
     * {@code #396} arm appends the {@code .get()}; the two halves in one token are what makes this
     * seat's bijection visible in golden.
     */
    @Test
    @EnabledIf("cellCAvailable")
    void control5b_cellCGoldenCarriesTheCrTerminalChainCoercion() throws IOException {
        String g = Files.readString(GOLDEN_C.resolve(CARRIER_CR_CHAIN_C));
        assertEquals(2, count(g, ".asMapper().get();"),
                "S20: the cdm carrier must coerce BOTH of its CR-terminal chain predicates —"
                + " the census's 4 arrivals per walk are this file's 2 sites x the 2 generations"
                + " of cdm 5.38.0 inside one D11 walk (its own cell and drr's transitive CDM);"
                + " a PER-WALK count cannot be decomposed by walks, which is what the earlier"
                + " '2 sites x 2 walks' wording did");
        assertTrue(!Files.readString(GOLDEN_C.resolve(CARRIER_OUT_OF_REACH_C))
                        .contains(".asMapper().get()"),
                "the out-of-reach lock must carry NEITHER decided form — that is what makes it the"
                + " inertness witness");
    }

    // =========================================================================
    // Fixture harness (the sibling seat suites' renderer, verbatim)
    // =========================================================================

    private record Render(Map<String, String> output, List<String> errors) {}

    private static Render rendered;

    private static Render render() throws IOException {
        if (rendered != null) {
            return rendered;
        }
        RModel main = AstBuilder.buildFromString(MODEL, "seat616f9.rosetta");
        main.setVersion("0.0.0.test");
        List<RModel> models = new ArrayList<>();
        models.add(main);
        models.addAll(loadBuiltinsOnly());
        RWorkspace workspace = RWorkspace.build(models).workspace();
        GeneratorModel gm = new GeneratorModel(workspace,
                m -> "census.seat616f9".equals(m.namespace()));
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

    private static int count(String haystack, String needle) {
        int n = 0;
        int i = haystack.indexOf(needle);
        while (i >= 0) {
            n++;
            i = haystack.indexOf(needle, i + needle.length());
        }
        return n;
    }

    private static void assertContains(String code, String needle) {
        assertTrue(code.contains(needle), "expected <" + needle + "> in:\n" + code);
    }

    private static void assertAbsent(String code, String needle) {
        assertTrue(!code.contains(needle), "did NOT expect <" + needle + "> in:\n" + code);
    }

    /**
     * Strip line and block comments ONLY, keeping string literals — the view for the fixtures whose
     * decided form CONTAINS a rendered literal. {@link #codeOnly} blanks exactly those tokens.
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
     * Strip line and block comments plus string literals so a javadoc, a label or a quoted Java
     * fragment never counts as code — the view for the fixtures whose decided form carries no
     * rendered literal.
     *
     * <p><b>THE TRAP, and it has now bitten twice</b> (the #615 code-quality review recorded it at
     * {@code MapperWrapPrefixSeatTest}; this suite's {@code control0} reproduced it from the copy
     * and the #616 code-quality review caught it as MF-1). The literal is deleted <em>including
     * both quote characters</em> — {@code i = j + 1} with nothing appended — so the returned text
     * contains no {@code "} at all. <b>Any needle carrying a quote is therefore unmatchable here and
     * every {@code assertAbsent}-shaped assertion over one is VACUOUS.</b> A negative assertion
     * whose needle quotes a Java string must use {@link #codeNoComments}, which strips the comments
     * just the same and keeps the literals. Positive assertions over quoted needles are safe by
     * inspection — they would fail immediately — which is why only the absence checks are affected.
     *
     * <p>This scanner, {@code codeNoComments}, {@code lock}, {@code generateCell},
     * {@code generateCellOnIrRoute}, {@code count}, {@code assertContains}, {@code assertAbsent},
     * {@code normalize}, {@code loadBuiltinsOnly} and {@code irProviderOnClasspath} are copied
     * verbatim across the seat suites, so this warning is copied with them deliberately. Extracting
     * a shared package-private harness is BANKED as its own PR rather than taken here (it would
     * rewrite five-plus suites and invalidate this seat's cited lane run); see
     * {@code target/seat616-instruments/swap-notes.md} (local) for the entry.
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
            throw new AssertionError("[ThenBindingOverlaySeatTest] builtins parse failures: "
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
    private static Map<String, String> cdmCOutput;
    private static List<String> cdmCGenErrors;

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
        if (cellCAvailable()) {
            List<String> errs = new ArrayList<>();
            cdmCOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("cdm", "5.38.0", CELL_C_ROOT), errs);
            cdmCGenErrors = errs;
        }
    }

    private static void lockA(String carrier) throws IOException {
        lock(carrier, drrAOutput, drrAGenErrors, GOLDEN_A, "drr 6.34.1");
    }

    private static void lockB(String carrier) throws IOException {
        lock(carrier, drrBOutput, drrBGenErrors, GOLDEN_B, "drr 7.0.0");
    }

    private static void lockC(String carrier) throws IOException {
        lock(carrier, cdmCOutput, cdmCGenErrors, GOLDEN_C, "cdm 5.38.0");
    }

    private static void lock(String carrier, Map<String, String> output, List<String> genErrors,
            Path goldenRoot, String cell) throws IOException {
        assertNotNull(output, cell + " generation did not run — corpus unavailable?");
        List<String> own = genErrors.stream().filter(e -> e.contains(carrier)).toList();
        assertTrue(own.isEmpty(),
                "the generator reported errors for the locked file " + carrier + ": " + own);
        String generated = output.get(carrier);
        assertNotNull(generated, "not generated in " + cell + ": " + carrier);
        Path goldenPath = goldenRoot.resolve(carrier);
        assertTrue(Files.isRegularFile(goldenPath), "golden missing: " + goldenPath);
        assertEquals(normalize(Files.readString(goldenPath)), normalize(generated),
                "generated " + cell + " output must byte-match golden (newline-normalized) for "
                + carrier + " — PR #616 family 9: the drained-chain TAIL reads move onto the AST"
                + " predicates beside them with no byte change.");
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
