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
import java.util.function.Predicate;

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
import com.regnosys.rosetta.symbols.RLinkingResult;
import com.regnosys.rosetta.symbols.RWorkspace;

/**
 * SEAT 30, law 2 -- facet {@code blockLambdaSingleItemChainStamp}: <b>a block lambda whose arm
 * join RENDERED a META element publishes that element as the chain's type under
 * {@code mapSingleToItem} as well as under {@code mapSingleToList} -- the stamp becomes
 * WRAPPER-keyed with the wrap kind read from the render's own chosen method.</b>
 *
 * <p><b>The seat.</b> {@code CollectionHandler}'s extract assembly (:912-920) stamps
 * {@code LambdaCompiled.chainItemType()} onto the emitted chain for exactly ONE method
 * ({@code mapSingleToList} -> {@code MapperC<meta>}, facet {@code extractLadderChainTypeStamp},
 * PR #387). Every other method publishes {@code null}, so the wrapper the block demonstrably
 * produced is invisible to every compiled-type-gated consumer downstream.
 *
 * <p><b>The population is MEASURED, not inferred (LAW 75).</b> {@code [PROBE30-MSTI]}, one
 * instrumented D11 round over all 25 cells, BOTH routes, printed at the single publish point:
 * <pre>
 *   132 rows  chainWrapper=true stamped=false   (route-identical, 132 OFF / 132 ON)
 *    88       mapM=mapSingleToItem  position=MAPPER_EXPECTING     &lt;-- THIS LAW
 *    44       mapM=mapItemToList    position=MAPPER_C_EXPECTING   &lt;-- BANKED (see below)
 *     0       mapM=mapItem                                        &lt;-- MEASURED EMPTY
 * </pre>
 * The {@code mapItemToList} half is BANKED and deliberately NOT written: its 44 rows are
 * entirely {@code GetUnderlierProductIdentifier*} / {@code GetBasketConstituentsProductIdentifier}
 * / {@code GetAllUnderlierProductIdentifier} -- green, plus one F11 BAND file that is another
 * seat-30 law's carrier. Writing it here would put two laws in one commit with no measurement
 * for the second. {@code mapItem} is a MEASURED EMPTY population and is adjudicated as such
 * (the seat-29 {@code m-law5ii} precedent), never declared away.
 *
 * <p><b>THE LAW HAS TWO HALVES, and the second one was found by MEASUREMENT, not by design.</b>
 * The first cut shipped the stamp alone on the reasoning that facet
 * {@code ruleCondBaseEvalArgDeref} (PR #388, inside
 * {@code CollectionHandler.compileEffectiveElseConditionalBlock}) was an existing consumer waiting
 * only for a type. The first measured run said otherwise: the stamp landed and the 6.37/6.38
 * carrier still emitted the un-deref'd arm (the fork's block closed {@code });} where golden
 * closes {@code }).<String>map("Type coercion", ...)}). Reading the seat against that print gives
 * the reason -- <b>EVERY bare-evidence channel at this seat is blind to a plain feature-call then
 * arm</b>:
 * <ul>
 *   <li>#388's COMPILED branch reads {@code thenCompiled.getExpressionType()}, and
 *       {@code NavigationHandler}'s own contract is that "non-meta steps keep a null type" -- the
 *       carrier's {@code reportableInformation -> customBasket -> customBasketCode} is exactly
 *       such a step, so the branch has nothing to read;</li>
 *   <li>#388's re-root branch, which asks the RIGHT question
 *       ({@code detectMetaKind == NONE} on the resolved leaf), is scoped to
 *       {@code RSymbolReference} and never sees a feature call;</li>
 *   <li>#339 needs AST bare-sibling evidence, and {@code isProvablyBareJoinArm} excludes navs by
 *       construction (the #295 UnderlyingIdOther trap);</li>
 *   <li>#354 needs {@code recoverExprMetaWrapper} to resolve, and the walker returns {@code null}
 *       for a CONDITIONAL terminal (its own documented {@code QuantityUnitOfMeasureLeg1/2}
 *       decline) -- which is also why only a RENDER-truth stamp can type this else arm at all.</li>
 * </ul>
 * <b>Half two is therefore a one-shape ADMIT</b>: #388's re-root branch gains a second supplier of
 * the same resolved-leaf evidence -- an arm that IS an {@code RFeatureCall} already carries its
 * resolved feature and takes the identical {@code detectMetaKind == NONE} test with no synthesis.
 * The test, both equality gates and the coercion call are untouched (LAW 69: one evidence rule,
 * two arm shapes). {@code a1} is witness-unique for the admit, {@code a2} for the stamp.
 *
 * <p><b>Carriers.</b> This law alone heals <b>2</b>:
 * <pre>
 *   drr 6.37.0 POJO  drr/standards/iosco/cde/version1/basket/reports/CustomBasketCodeRule.java
 *   drr 6.38.0 POJO  drr/standards/iosco/cde/version1/basket/reports/CustomBasketCodeRule.java
 * </pre>
 * and it is one CONJUNCTIVE half of a third (the seat-29 law-9/9b precedent):
 * {@code drr 5.61.0 asic CustomBasketCodeIdentifierRule.java} is the SAME shape hoisted as a
 * STATEMENT, so its consumer is {@code FunctionExpressionRenderer.appendIteHoistChainCore},
 * which has no #388 twin. Seat 30's law 9 was drafted to add it and was <b>BANKED to S31 after
 * three measured rounds</b> ({@code target/seat30-instruments/law9-bank.md}): its statement-seat
 * coercion still crashes generation on that carrier ("Cannot create a new identifier in a closed
 * scope"), so no law-9 head exists in this seat and that file does not heal here. Its row is the
 * standing LAW-81 tripwire that fires when the S31 law lands.
 *
 * <p><b>The charter's UATPI x8 are REFUTED as law-2 carriers</b>, and the refutation is the
 * reason {@code corpus_control5} exists. The eight files' three hunks each are
 * {@code ReferenceWithMetaObservable -> Observable} NAV-RECEIVER derefs at a receiver that is
 * ALREADY wrapper-typed ({@code [PROBE30-NAVRECV] preWrapper=true nullTyped=false
 * cfRecover=ReferenceWithMetaObservable}), and neither the UATPI golden nor the UATPI fork
 * contains a {@code FieldWithMetaString}-terminal block anywhere in the file -- so no stamp can
 * produce those bytes. The 4 {@code missed=true} UATPI rows per Leg per cell are a render seat
 * that publishes no byte in the emitted file; {@code corpus_control5} pins them at ZERO movement
 * instead of claiming them.
 *
 * <p><b>GREEN EXPOSURE -- measured, named, and pinned.</b> {@code missed=true} fires on 19 green
 * {@code where=} values besides the carriers, including the byte-IDENTICAL drr 5.61.0 esma/fca
 * {@code CustomBasketCodeRule} and the iosco cde v1 {@code CustomBasketCodeRule} in
 * 6.34.1 / 6.35.0 / 6.36.0. In every one of those the block's chain is consumed by the
 * WHOLE-OUTPUT terminal:
 * <pre>
 *   final FieldWithMetaString fieldWithMetaString = thenArg2
 *       .mapSingleToItem(item -&gt; { ... return MapperS.&lt;FieldWithMetaString&gt;ofNull(); }).get();
 *   if (fieldWithMetaString == null) { output = null; } else { output = fieldWithMetaString.getValue(); }
 * </pre>
 * -- golden PRESERVES the wrapper there and emits no chain deref at all, because the whole-output
 * seat unwraps in the statement ladder instead. The stamp decides nothing; it only makes the
 * wrapper VISIBLE to a seat that was already asking for it, and the only seats that ask are the
 * ones whose join is bare. <b>The 6.36.0 -&gt; 6.37.0 pair is the cleanest possible witness</b>:
 * the SAME basename, the SAME inner block, golden deref'd in 6.37 (where the rosetta gained a
 * bare leading arm, making the join bare) and NOT deref'd in 6.36. {@code b1} is that pair as a
 * fixture; {@code corpus_control0} is it as a golden oracle; {@code corpus_control3} scans the
 * whole 6.36.0 cell for it.
 *
 * <p><b>ASSERTION DISCIPLINE -- no {@code MEASURE:} placeholder survives in any assertion.</b> The
 * first draft left a guessed "removed token" in each {@code a} test, which could have passed
 * vacuously in both states. Both are replaced by ONE POSITIONAL token,
 * {@code }).<String>map("Type coercion", ...)} -- the deref attached to the block lambda's own
 * closing brace, which is exactly golden's byte and is structurally impossible in the pre-law
 * render (whose tail is a bare {@code });}). Every {@code b} premise is likewise a token the
 * fixture's shape guarantees (the wrapper typed empty, the mapper method), not a remembered
 * rendering.
 *
 * <p><b>MEASURED at the first run of this suite (13 tests, 10 failures), against the stamp-only
 * cut:</b> {@code corpus_control0} PASSED -- so the whole green argument above is measured, not
 * asserted: golden 6.37 carries the deref, golden 6.36 does not, and the fork emits 6.36
 * byte-identically. {@code corpus_c1} FAILED with the fork's un-deref'd tail, which is the print
 * that produced half two. {@code a1/a2/b1/b3} failed at FIXTURE GENERATION (the rune body parsed
 * as one {@code RThenExpr} and inferred {@code RMissingType}) -- the {@code then} chains inside
 * the conditional arms were not parenthesised, which the real carrier source
 * ({@code standards-iosco-cde-version1-basket-rule.rosetta}) does: {@code else (X then extract
 * (if ...))}. The fixtures now follow the carrier's own bracketing. The four union controls failed
 * ONLY on the unpinned-domain sentinel -- the sentinel now PRINTS them (see
 * {@code assertUnionEqual}).
 *
 * <p><b>MEASURED at the SECOND run (13 tests, 7 failures), with both halves applied.</b>
 * {@code corpus_c1} and {@code corpus_c2} GREEN -- <b>the 6.37 and 6.38 carriers heal WHOLE</b>,
 * so the law's own claim is discharged. {@code a2}, {@code b1}, {@code control0} GREEN. The four
 * controls printed their measured values, now pinned above, and they settle the two questions
 * this suite exists to answer:
 * <ul>
 *   <li><b>NO over-fire, measured.</b> Across drr 6.37 / 6.36 / 5.61 / 7.0 -- 5,994 scanned files
 *       -- there is <b>not one residue row where {@code fork > golden} on T1</b>. Every row is a
 *       file the fork under-derefs, i.e. pre-existing band residue. drr 6.37 and 6.36 came back
 *       with an <b>EMPTY</b> residue at domain 1618 each: the carrier cell is clean beyond the
 *       heal and the GREEN TWIN cell is byte-inert. An over-stamp into {@code mapItemToList} would
 *       have added derefs on the {@code GetUnderlierProductIdentifier*} family in drr 7.0.0; it
 *       did not.</li>
 *   <li><b>{@code b2}/{@code b3} were a MIS-SCOPED PIN, not a leak.</b> Their emitted code carries
 *       ONE deref, {@code .<String>map("Type coercion", w -> w.getValue()).getMulti()} -- the BARE
 *       MapperC form emitted by the pre-existing WHOLE-OUTPUT seat, present in both states,
 *       because those fixtures' rule output is {@code List<String>} over a
 *       {@code MapperC<FieldWithMetaString>} chain. The file-wide negative could never have passed.
 *       The locks are now witness-unique: the stamp-path chain-tail token must be ABSENT and the
 *       deref count must be EXACTLY ONE, so a stamp reaching either method (which would add a
 *       second, null-guarded one at the block tail) still fails them.</li>
 * </ul>
 * {@code a1} stayed red while {@code corpus_c1} healed, so the fixture -- not the law -- differed
 * from the carrier: its else chain was NAV-rooted with ONE {@code then} step where the carrier is
 * FUNCTION-rooted with TWO, which routes the else arm through a different hoist. a1 is now a
 * faithful reduction ({@code Seat30BookForBasket then extract holder then extract (...)}). The
 * {@code RThenExpr}-unwrap hypothesis for the evidence supplier is NOT written: a1's then arm is a
 * plain nav, not a then-chain, so that widening has no measured carrier and stays banked.
 *
 * <p><b>RED at the pre-law head -- MEASURED at {@code 6c8e1544}, BOTH routes</b>
 * ({@code f30-red-default.log}, {@code f30-red-on.log}): <b>13 run / 5F / 0E / 1 skip</b>
 * (default) and <b>13 / 5F / 0E / 0 skip</b> ({@code -Pir-on}) -- the SAME five both routes:
 * <b>a1, corpus_c1, corpus_c2, corpus_control1, corpus_control5</b> ({@code control5} at
 * {@code 13 file(s)}). <b>a2 is GREEN at RED</b> -- see the failing-first note below. <b>GREEN at
 * the CURRENT head is 13/0F on both routes</b> -- measured by chain run 2 at {@code 71a92e826}.
 * At the run-1 head {@code e223ce19} it was <b>13/1F</b>: {@code corpus_control5} at
 * {@code 11 file(s)}, this suite's then-standing failure (a stale drr 7.0.0 whole-cell pin the
 * seat's own laws fired -- LAW 81 -- re-pinned at {@code 80bbe57b}, and that re-pin is what took
 * the suite to 0F). Every lane set below WAS therefore scored NET of
 * {@code corpus_control5}-at-11: a lane that printed exactly that moved nothing. <b>That netting
 * rule is HISTORICAL</b> -- it applies to the run-1 logs these sets were transcribed from. At the
 * run-2 head there is no standing failure left to net, and each lane's raw set equals its net set.
 *
 * <p><b>⚠ FAILING-FIRST NOTE ON a2 (LAW 66), MEASURED.</b> {@code a2} passes at the RED base AND
 * under {@code m-law2s}, the whole-law revert. The only lane that moves it is
 * {@code m-law2s-wrapkind}. So a2 does NOT prove the law failing-first; it is a post-law
 * mechanism pin that discriminates the STAMP'S KIND ({@code MAPPER_S} vs {@code MAPPER_C}).
 * The law's failing-first evidence at fixture grain is {@code a1} alone; at corpus grain it is
 * {@code corpus_c1} + {@code corpus_c2} (both byte-whole carrier locks) + {@code corpus_control1}.
 *
 * <p><b>MUTATIONS -- MEASURED (LAW 66/76/82). Sets transcribed from the archived logs
 * {@code f30-mut-&lt;lane&gt;.log} of BOTH chain runs -- run 1 at {@code e223ce19} and run 2 at the
 * re-pin head {@code 71a92e826}. The RAW counts quoted below are run 2's, with run 1's in
 * parentheses where they differ (by exactly the suite's then-standing failure, retired at
 * {@code 80bbe57b}); every NET set is identical across the two runs. Corrections NAMED in
 * place.</b>
 * <b>⚠ the lane loop runs the DEFAULT profile only</b>, so {@code corpus_control2} (the LAW-77
 * route-parity control) is the one skip in each {@code 13 run / 1 skipped} lane and cannot fail
 * in a lane by construction.
 * <ul>
 *   <li><b>m-law2s</b> ({@code f30-mut-m-law2s.log}) = {@code law2s-apply.py --revert}, both
 *       halves. <b>MEASURED at the run-2 head {@code 71a92e826}: 13/4F/0E/1S = a1, corpus_c1,
 *       corpus_c2, corpus_control1.</b> (Run 1 at {@code e223ce19} measured 13/<b>5</b>F/0E/1S,
 *       the extra member being the then-standing {@code corpus_control5} at its unchanged 11,
 *       which the {@code 80bbe57b} re-pin retired; the NET set is the same either way.)
 *       <b>Drafted a1, a2, corpus_c1,
 *       corpus_c2, corpus_control1, corpus_control4; measured without a2 and without
 *       corpus_control4 -- the difference explained:</b> (i) a2 is green under the whole-law
 *       revert (the failing-first note above -- this lane is where that was caught); (ii)
 *       {@code corpus_control4} (drr 5.61.0) does NOT move, i.e. <b>the law's whole-cell reach
 *       is drr 6.37/6.36/7.0-scoped and 5.61.0 is genuinely outside it</b> -- the draft
 *       over-claimed the blast radius and the measurement narrowed it. The lane reproduces the
 *       RED set exactly (net of the standing control5), so law 2 alone accounts for this suite's
 *       RED movement.</li>
 *   <li><b>m-law2s-admit</b> ({@code f30-mut-m-law2s-admit.log}) = {@code --mut-admit} (the
 *       {@code RFeatureCall} evidence supplier severed, stamp kept).
 *       <b>MEASURED at the run-2 head {@code 71a92e826}: 13/4F/0E/1S = a1, corpus_c1,
 *       corpus_c2, corpus_control1.</b> (Run 1 measured 13/<b>5</b>F/0E/1S, the extra member
 *       being the then-standing {@code corpus_control5} at 11.) <b>Drafted a1, corpus_c1, corpus_c2 ONLY;
 *       measured that set PLUS corpus_control1 -- the difference explained:</b> severing the
 *       evidence supplier is corpus-visible at whole-cell scale, not just at the two carriers;
 *       the draft under-named the control.</li>
 *   <li><b>⚠ THE FINDING THE TWO LANES ABOVE PRODUCE TOGETHER (LAW 82).</b>
 *       <b>{@code m-law2s} and {@code m-law2s-admit} measured the IDENTICAL failing set</b> --
 *       a1, corpus_c1, corpus_c2, corpus_control1 (plus, at the run-1 head only, the then-standing
 *       control5), same tests, same
 *       residue counts, and identical again at the run-2 head where both read a clean 4F. The lane was built to prove "the two halves are separable rather than
 *       one restated twice", and <b>it does not: there is no set difference between reverting
 *       BOTH halves and severing only the evidence supplier</b>. That is not evidence the halves
 *       are one thing either -- it is an absence of discrimination in the instrument. Stated
 *       plainly so no reader takes the separability claim as measured. <b>What WOULD separate
 *       them, in order of cost:</b> (1) run both lanes under {@code -Pir-on} so
 *       {@code corpus_control2} -- the LAW-77 route-parity control, which the default lane loop
 *       always skips -- can participate; the stamp lives in {@code CollectionHandler} (shared)
 *       while its consumers include {@code FunctionExpressionRenderer} (route-substituted), so
 *       the two halves have genuinely different route surfaces and control2 is the control most
 *       likely to split them; (2) add a FINER fixture whose arm reaches the stamp through the
 *       pre-existing AST channel only -- the shape the draft assumed {@code a2} was, and which
 *       a2 measurably is not -- so one half moves it and the other does not. Until one of those
 *       runs, the separability of the two halves is an argument, not a measurement.</li>
 *   <li><b>m-law2s-kind</b> ({@code f30-mut-m-law2s-kind.log}) = {@code --mut-kind} (the
 *       {@code "mapSingleToItem".equals(mapM)} conjunct severed, so ANY method stamps).
 *       <b>MEASURED at the run-2 head {@code 71a92e826}: 13/0F/0E/1S -- an unambiguous,
 *       contamination-free EMPTY set.</b> (Run 1 measured 13/<b>1</b>F/0E/1S, the one member
 *       being the then-standing {@code corpus_control5} at {@code 11 file(s)}, IDENTICAL to
 *       GREEN -> the same net EMPTY, inferred then and now measured directly.)
 *       <b>Drafted a 6F set (b2, b3, corpus_control1,
 *       corpus_control3, corpus_control4, corpus_control5); measured ZERO movement -- the widest
 *       claim-vs-measurement gap in the seat, and the difference explained:</b> the draft
 *       reasoned from a probe census ("the 44 measured {@code mapItemToList} rows live on
 *       {@code GetUnderlierProductIdentifier*} in drr 7.x and 5.61.0") to the conclusion that
 *       stamping those rows would move bytes. <b>It does not.</b> Those rows exist, but stamping
 *       them changes no consumer's output at 25 cells and no fixture's render here -- a census
 *       counts SITES, and a site is not a carrier until a consumer reads it. <b>EMPTY is the
 *       ADJUDICATED value per the seat-29 {@code m-law5ii} precedent</b>, and what it adjudicates
 *       is this: the {@code mapSingleToItem} conjunct is a strictly-narrowing guard with ZERO
 *       measured carriers. It ships on the shape argument (only that method's block lambda has
 *       the tail the stamp describes), honestly scored as un-witnessed. <b>The standing lesson
 *       for the next seat: never promote a probe SITE count into a mutation's claimed failing
 *       set without a consumer named.</b></li>
 *   <li><b>m-law2s-wrapkind</b> ({@code f30-mut-m-law2s-wrapkind.log}) = {@code --mut-wrapkind}
 *       ({@code MAPPER_C} instead of {@code MAPPER_S}). <b>MEASURED at the run-2 head
 *       {@code 71a92e826}: 13/5F/0E/1S = a1, a2, corpus_c1, corpus_c2, corpus_control0.</b>
 *       (Run 1 measured 13/<b>6</b>F/0E/1S, the extra member being the then-standing
 *       {@code corpus_control5} at 11.)
 *       <b>Drafted a1, a2, corpus_c1, corpus_c2, corpus_control1; measured corpus_control0
 *       instead of corpus_control1 -- the difference explained, and it is a better result than
 *       the claim:</b> {@code corpus_control0} is the GOLDEN-ORACLE twin pin (6.37 carries the
 *       deref, 6.36 does not, and the fork must emit 6.36 byte-identically). A wrong stamp KIND
 *       breaks the byte-inert green twin -- i.e. it is caught as an over-fire into a file that
 *       must not move at all -- which is a sharper witness than a residue-count shift in
 *       {@code control1}. This lane is also the ONLY one that moves {@code a2}, which is what
 *       makes a2 the stamp-kind pin rather than a law pin.</li>
 * </ul>
 * <b>Lane tally for this suite: 0 MATCH · 3 MISMATCH-corrected · 1 MEASURED-EMPTY against a 6F
 * claim.</b>
 */
class BlockLambdaSingleItemChainStampSeatTest {

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

    /** The carrier file, identical relative path in 6.37.0 and 6.38.0. */
    private static final String CBC =
            "drr/standards/iosco/cde/version1/basket/reports/CustomBasketCodeRule.java";
    /** The GREEN twin: the SAME basename one cell earlier, whose join is NOT bare. */
    private static final String CBC_GREEN_TWIN = CBC;
    /** The measured green {@code missed=true} whole-output carriers in drr 5.61.0. */
    private static final String CBC_ESMA =
            "drr/regulation/esma/emir/refit/trade/reports/CustomBasketCodeRule.java";
    private static final String CBC_FCA =
            "drr/regulation/fca/ukemir/refit/trade/reports/CustomBasketCodeRule.java";

    /** Cell A = drr 6.37.0 -- the carrier cell. */
    private static final Path CELL_A_ROOT = Path.of("../test-corpus/drr/drr-6.37.0");
    private static final Path GOLDEN_A = CELL_A_ROOT.resolve("rosetta-source/src/generated/java");
    /**
     * Cell B = drr 6.36.0 -- the GREEN TWIN cell and this law's sharpest control. It carries the
     * SAME {@code CustomBasketCodeRule} basename with the SAME inner block and a
     * {@code missed=true} row, and golden does NOT deref it (the whole-output seat unwraps
     * instead). If the stamp over-fires anywhere, this is where it shows first.
     */
    private static final Path CELL_B_ROOT = Path.of("../test-corpus/drr/drr-6.36.0");
    private static final Path GOLDEN_B = CELL_B_ROOT.resolve("rosetta-source/src/generated/java");
    /**
     * Cell C = drr 5.61.0 -- the other measured green {@code missed=true} domain (esma + fca
     * {@code CustomBasketCodeRule}, both byte-identical today) and the home of the CONJUNCTIVE
     * law-2 + law-9 carrier {@code CustomBasketCodeIdentifierRule}. <b>There is no LAW 9 suite:</b>
     * seat 30's law 9 was BANKED to S31 ({@code target/seat30-instruments/law9-bank.md}), so that
     * carrier is locked NOWHERE yet — {@code KNOWN_RESIDUE_DRR561} below carries its row as the
     * standing LAW-81 tripwire that fires when the S31 law lands.
     */
    private static final Path CELL_C_ROOT = Path.of("../test-corpus/drr/drr-5.61.0");
    private static final Path GOLDEN_C = CELL_C_ROOT.resolve("rosetta-source/src/generated/java");
    /**
     * Cell D = drr 7.0.0 -- the UATPI zero-movement domain. Law 2 DOES fire on 4
     * {@code missed=true} rows per UATPI Leg per drr 7.x cell whose kept-render site is not
     * identified by the probe; the claim is that they publish no byte, and this is the control
     * that adjudicates it rather than assuming it. It is also where 20 of the 44 banked
     * {@code mapItemToList} rows live.
     */
    private static final Path CELL_D_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path GOLDEN_D = CELL_D_ROOT.resolve("rosetta-source/src/generated/java");
    /** Cell E = drr 6.38.0 -- the SECOND carrier cell (identical carrier text to 6.37.0). */
    private static final Path CELL_E_ROOT = Path.of("../test-corpus/drr/drr-6.38.0");
    private static final Path GOLDEN_E = CELL_E_ROOT.resolve("rosetta-source/src/generated/java");

    static boolean cellAAvailable() {
        return Files.isDirectory(GOLDEN_A);
    }

    static boolean cellBAvailable() {
        return Files.isDirectory(GOLDEN_B);
    }

    static boolean cellCAvailable() {
        return Files.isDirectory(GOLDEN_C);
    }

    static boolean cellDAvailable() {
        return Files.isDirectory(GOLDEN_D);
    }

    static boolean cellEAvailable() {
        return Files.isDirectory(GOLDEN_E);
    }

    static boolean cellAAndIrProviderAvailable() {
        return cellAAvailable() && irProviderOnClasspath();
    }

    /**
     * a1 = THE 6.37 CustomBasketCode SHAPE, and the vector that needs BOTH halves. A rule body
     *      {@code extract (if <nav> then <nav> else (<chain> then extract (<elseless 2-rung ladder
     *      over a meta leaf>)))}. The inner extract's block lambda renders
     *      {@code return MapperS.<FieldWithMetaString>ofNull();} as its terminal, so it publishes
     *      a META arm join; the OUTER effective-else block joins a BARE nav arm with it and must
     *      deref the else arm in place. Its then arm is a MULTI-HOP {@code RFeatureCall}
     *      ({@code holder -> plainCode}) precisely because that is the shape every bare-evidence
     *      channel at this seat was blind to -- so a1 is RED with the stamp alone.
     * a2 = the STAMP-ONLY vector: a {@code to-string} then arm IS AST bare evidence, so the
     *      pre-existing #339 arm consumes the stamp with no admit involved. a1 and a2 fail for
     *      DIFFERENT reasons and neither substitutes for the other.
     * b1 = THE GREEN TWIN, as a fixture: the SAME inner block with NO bare sibling arm, so the
     *      join IS the wrapper and every consumer declines on its own terms. The stamp fires here
     *      (the wrapper typed empty is the proof) and the assertion is byte-STABILITY: no
     *      {@code "Type coercion"} may appear.
     *      <b>The first draft asserted a whole-output local shape that had never been measured;
     *      the premise is now the typed empty, which the elseless ladder guarantees structurally.</b>
     * b2 = the {@code mapItemToList} decline: the same wrapper-terminal block over a MULTI
     *      receiver with a MULTI body. The 44 measured rows of that kind must NOT be stamped by
     *      this law (they are BANKED), so this rule's bytes must not move.
     * b3 = the {@code mapItem} decline: a wrapper-terminal block over a MULTI receiver with a
     *      SINGLE body. The probe measured ZERO {@code missed=true} rows of this kind corpus-wide;
     *      b3 pins the SHAPE so a widened key is caught even where the corpus has no witness.
     *
     * <p><b>What this fixture set can and cannot prove (LAW: a control scans the domain it
     * claims).</b> The 19 green {@code where=} values a widened key admits are a CORPUS-scale
     * phenomenon spread over four cells; b1/b2/b3 pin the three SHAPES, and the exposure itself
     * is pinned by {@code corpus_control1/3/4/5}, whose domains contain those families by
     * construction. {@code m-law2s-kind} is expected to be caught by the CORPUS controls as well
     * as by b2/b3 -- stated here rather than discovered when the mutation measures a smaller set
     * than claimed.
     *
     * <p>Lexer-safe identifiers: no {@code tag}, {@code single} or {@code label}.
     */
    private static final String MODEL = """
            namespace census.seat30f13stamp
            version "1.0.0"

            type Seat30Ident:
                ident string (0..1)
                    [metadata scheme]

            type Seat30Holder:
                plainCode string (0..1)
                primaryIdents Seat30Ident (0..*)
                otherIdents Seat30Ident (0..*)

            type Seat30Book:
                holder Seat30Holder (0..1)

            type Seat30Basket:
                marker number (0..1)
                book Seat30Book (0..1)
                holder Seat30Holder (0..1)
                holders Seat30Holder (0..*)

            func Seat30BookForBasket: <"the ProductForEvent twin - a FUNCTION-CALL chain root, so the else chain hoists through the same two-level deep-then route the carrier uses">
                inputs:
                    basket Seat30Basket (1..1)
                output:
                    picked Seat30Book (0..1)
                set picked:
                    basket -> book

            reporting rule A1EffElseBlockDerefsWrapperArm from Seat30Basket: <"a1 - THE CustomBasketCodeRule 6.37 SHAPE, reduced faithfully: a MULTI-HOP NAV then arm (an RFeatureCall to a non-meta leaf - the shape every bare-evidence channel at this seat was blind to) joined with a function-rooted TWO-STEP else chain whose inner block-lambda ladder renders a FieldWithMetaString typed empty. Needs the STAMP and the ADMIT together.">
                extract
                    if book -> holder -> plainCode exists
                    then book -> holder -> plainCode
                    else (Seat30BookForBasket
                        then extract holder
                        then extract
                            (if primaryIdents exists
                            then primaryIdents -> ident first
                            else if otherIdents exists
                            then otherIdents -> ident first))
                as "a1"

            reporting rule A2EffElseBlockToStringBareArm from Seat30Basket: <"a2 - the STAMP-ONLY vector: the SAME else chain, but a to-string then arm IS AST bare evidence, so the pre-existing #339 arm consumes the stamp with no admit involved. Witness-unique for the stamp half.">
                extract
                    if marker exists
                    then marker to-string
                    else (Seat30BookForBasket
                        then extract holder
                        then extract
                            (if primaryIdents exists
                            then primaryIdents -> ident first
                            else if otherIdents exists
                            then otherIdents -> ident first))
                as "a2"

            reporting rule B1WholeOutputKeepsWrapper from Seat30Basket: <"b1 - THE GREEN TWIN: the SAME inner block with no bare sibling arm at all, so the join IS the wrapper. The stamp fires here and nothing may deref.">
                holder
                    then extract
                        (if primaryIdents exists
                        then primaryIdents -> ident first
                        else if otherIdents exists
                        then otherIdents -> ident first)
                as "b1"

            reporting rule B2MapItemToListStaysUnstamped from Seat30Basket: <"b2 - the mapItemToList decline: a MULTI receiver with a MULTI body. The 44 measured rows of this kind are BANKED, not landed, so these bytes must not move.">
                holders
                    then extract
                        (if primaryIdents exists
                        then primaryIdents -> ident
                        else if otherIdents exists
                        then otherIdents -> ident)
                as "b2"

            reporting rule B3MapItemStaysUnstamped from Seat30Basket: <"b3 - the mapItem decline: a MULTI receiver with a SINGLE body. The probe measured ZERO missed=true rows of this kind corpus-wide; this pins the SHAPE where the corpus has no witness.">
                holders
                    then extract
                        (if primaryIdents exists
                        then primaryIdents -> ident first
                        else if otherIdents exists
                        then otherIdents -> ident first)
                as "b3"
            """;

    /**
     * The deref this law's consumers emit. It is the {@code WrappedItemCoercer} MapperS arm's
     * fixed null-guarded form, so the token is structural, not a quoted accident.
     */
    private static final String DEREF =
            ">map(\"Type coercion\", fieldWithMetaString -> fieldWithMetaString == null"
            + " ? null : fieldWithMetaString.getValue())";

    /**
     * The POSITIONAL form of {@link #DEREF} -- the deref attached to the block lambda's own
     * closing brace, which is exactly what the carrier's golden carries
     * ({@code }).<String>map("Type coercion", ...)}) and exactly what the pre-law render cannot
     * carry (its tail is a bare {@code });}). Asserting the positional token makes one assertion
     * do the work of a positive and a witness-unique negative, with no vacuity risk in either
     * state -- so no {@code MEASURE:} placeholder is left in an assertion.
     */
    private static final String BLOCK_TAIL_DEREF = "}).<String" + DEREF;

    /** The wrapper typed empty -- the token that proves the block PUBLISHED a chain item at all. */
    private static final String WRAPPER_EMPTY_S = "MapperS.<FieldWithMetaString>ofNull()";
    private static final String WRAPPER_EMPTY_C = "MapperC.<FieldWithMetaString>ofNull()";

    // =========================================================================
    // Part A -- the positive fixtures
    // =========================================================================

    /**
     * a1 -- THE CARRIER SHAPE, and the one that needs BOTH halves. Its then arm is a multi-hop
     * NAV ({@code holder -> plainCode}), an {@code RFeatureCall} to a non-meta leaf: the shape
     * every bare-evidence channel at this seat was blind to before the admit (the compiled branch
     * because a non-meta nav step keeps a null type by NavigationHandler's own contract, the
     * re-root branch because it is {@code RSymbolReference}-scoped, and the #339 sibling because
     * {@code isProvablyBareJoinArm} excludes navs). So a1 is RED with the stamp alone and GREEN
     * only with the stamp AND the admit -- which is what makes it witness-unique for the admit.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_navThenArmDerefsTheStampedWrapperArm() throws IOException {
        String code = collapse(rule("A1EffElseBlockDerefsWrapperArmRule.java"));
        assertTrue(code.contains(WRAPPER_EMPTY_S),
                "PREMISE: the inner block must render the wrapper typed empty, else there is no"
                + " chain item to stamp and a1 proves nothing about this law:\n" + code);
        assertTrue(code.contains(BLOCK_TAIL_DEREF),
                "the nav-joined else arm must deref the stamped wrapper chain AT THE BLOCK TAIL"
                + " (the pre-law render closes `});` there):\n" + code);
    }

    /**
     * a2 -- THE STAMP-ONLY vector. A {@code to-string} then arm IS AST bare evidence
     * ({@code isProvablyBareJoinArm} admits to-string explicitly), so the pre-existing #339 arm
     * consumes the stamp with no admit involved.
     *
     * <p><b>MEASURED CORRECTION (LAW 82) -- the draft's claim about this test is REFUTED.</b> It
     * said "{@code m-law2s} moves both, {@code m-law2s-admit} moves only a1". <b>Neither lane
     * moves a2:</b> {@code m-law2s} measured a1, corpus_c1, corpus_c2, corpus_control1
     * ({@code f30-mut-m-law2s.log}) and {@code m-law2s-admit} measured the IDENTICAL set
     * ({@code f30-mut-m-law2s-admit.log}) -- a2 green in both, and green at the RED base too.
     * The ONLY lane that moves a2 is {@code m-law2s-wrapkind}
     * ({@code f30-mut-m-law2s-wrapkind.log}, 13/6F). So a2's measured role is the <b>stamp-KIND
     * discriminator</b> ({@code MAPPER_S} vs {@code MAPPER_C}), not a second failing-first vector
     * for the law, and it is NOT the test that separates the law's two halves -- nothing in this
     * suite currently is. See the class javadoc's finding on {@code m-law2s} vs
     * {@code m-law2s-admit} for what would.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_toStringBareEvidenceAlsoDerefsTheStampedArm() throws IOException {
        String code = collapse(rule("A2EffElseBlockToStringBareArmRule.java"));
        assertTrue(code.contains(WRAPPER_EMPTY_S),
                "PREMISE: the inner block must render the wrapper typed empty:\n" + code);
        assertTrue(code.contains(BLOCK_TAIL_DEREF),
                "the to-string-joined else arm must deref the stamped wrapper chain at the block"
                + " tail:\n" + code);
    }

    // =========================================================================
    // Part B -- the decline pins
    // =========================================================================

    /**
     * b1 -- THE GREEN TWIN. The stamp fires here (the wrapper typed empty below is the proof) and
     * NOTHING may move: with no bare sibling arm the join IS the wrapper, so every consumer
     * declines on its own terms and golden unwraps at the whole-output seat instead.
     *
     * <p>The premise is the wrapper typed empty rather than the whole-output local shape -- the
     * typed empty is structurally guaranteed by the elseless ladder, whereas the local's exact
     * rendering was an UNMEASURED claim in the first draft and is not one the law depends on.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b1_wholeOutputConsumerKeepsTheWrapperUnDerefd() throws IOException {
        String code = collapse(rule("B1WholeOutputKeepsWrapperRule.java"));
        assertTrue(code.contains(WRAPPER_EMPTY_S),
                "PREMISE: the stamp's precondition (a wrapper typed empty) must be present, else"
                + " b1 pins nothing:\n" + code);
        assertTrue(!code.contains("\"Type coercion\""),
                "a wrapper-JOIN block must NOT gain a chain deref:\n" + code);
    }

    /**
     * b2 -- the {@code mapItemToList} decline. This law is {@code mapSingleToItem}-only; the 44
     * measured rows of that kind are BANKED.
     *
     * <p><b>MEASURED CORRECTION (LAW 82).</b> The draft said "{@code m-law2s-kind} is the lane
     * that proves the conjunct load-bearing". <b>That lane measured net EMPTY</b> -- 13/1F, the
     * standing {@code corpus_control5} at its unchanged residue of 11, b2 green
     * ({@code f30-mut-m-law2s-kind.log}). Severing the {@code "mapSingleToItem".equals(mapM)}
     * conjunct so ANY method stamps moves NO byte, here or at 25 cells. The 44 probe rows are
     * SITES, not carriers: nothing downstream reads the stamp on them. So the conjunct is
     * un-witnessed, not load-bearing, and b2 is a shape lock that would catch a future consumer
     * learning to read it. The claim is corrected, the test is not weakened.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b2_mapItemToListBlockStaysUnstamped() throws IOException {
        String code = collapse(rule("B2MapItemToListStaysUnstampedRule.java"));
        assertTrue(code.contains(".mapItemToList("),
                "PREMISE: the fixture must reach the mapItemToList form, else b2 proves nothing"
                + " (LAW: a control scans the domain it claims):\n" + code);
        assertTrue(code.contains(WRAPPER_EMPTY_C),
                "PREMISE: the MULTI-body block must publish a wrapper chain item, else the"
                + " decline is vacuous:\n" + code);
        assertTrue(!code.contains(BLOCK_TAIL_DEREF),
                "a mapItemToList block must not gain the STAMP-PATH chain-tail deref:\n" + code);
        assertEquals(1, count(code, ">map(\"Type coercion\","),
                "exactly ONE deref may appear - the pre-existing WHOLE-OUTPUT one"
                + " (`.<String>map(\"Type coercion\", w -> w.getValue()).getMulti()`, the BARE"
                + " MapperC form). MEASURED at the second run: this rule already carried it in"
                + " BOTH states, which is why the first draft's file-wide negative was a"
                + " MIS-SCOPED PIN, not a leak. A stamp reaching this method would ADD a second,"
                + " null-guarded one at the block tail:\n" + code);
    }

    /**
     * b3 -- the {@code mapItem} decline, pinned even though the probe measured ZERO rows of the
     * kind corpus-wide. A measured-empty population is adjudicated, not assumed away.
     *
     * <p><b>MEASURED (LAW 82).</b> b3 was named in {@code m-law2s-kind}'s drafted 6F set; that
     * lane measured net EMPTY and b3 stayed green. Consistent with the probe's own zero-row
     * census for this kind: the population is empty on BOTH sides, so no lane at this corpus can
     * give b3 a witness. It remains a shape lock over a measured-empty population, which is the
     * honest score.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b3_mapItemBlockStaysUnstamped() throws IOException {
        String code = collapse(rule("B3MapItemStaysUnstampedRule.java"));
        assertTrue(code.contains(".mapItem("),
                "PREMISE: the fixture must reach the mapItem form:\n" + code);
        assertTrue(code.contains(WRAPPER_EMPTY_S),
                "PREMISE: the SINGLE-body block must publish a wrapper chain item, else the"
                + " decline is vacuous:\n" + code);
        assertTrue(!code.contains(BLOCK_TAIL_DEREF),
                "a mapItem block must not gain the STAMP-PATH chain-tail deref:\n" + code);
        assertEquals(1, count(code, ">map(\"Type coercion\","),
                "exactly ONE deref may appear - the pre-existing WHOLE-OUTPUT one. MEASURED at"
                + " the second run: present in BOTH states, because a mapItem chain is"
                + " MapperC-kinded and the whole-output seat derefs it with the bare element map"
                + " before .getMulti():\n" + code);
    }

    // =========================================================================
    // Part C -- the whole-file locks
    // =========================================================================

    /** corpus_c1 -- the 6.37.0 carrier, byte-for-byte against golden. */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_drr637CustomBasketCodeRuleMatchesGolden() throws IOException {
        lockCell(drrAOutput, drrAGenErrors, GOLDEN_A, CBC, "drr 6.37.0");
    }

    /**
     * corpus_c2 -- the 6.38.0 carrier, the second cell of the same flip. WIRED to a real cell
     * load (the first draft left this asserting nothing, which is exactly the class of defect the
     * seat's standing lesson names).
     */
    @Test
    @EnabledIf("cellEAvailable")
    void corpus_c2_drr638CustomBasketCodeRuleMatchesGolden() throws IOException {
        lockCell(drrEOutput, drrEGenErrors, GOLDEN_E, CBC, "drr 6.38.0");
    }

    // =========================================================================
    // Part D -- the corpus controls (LAW 79 UNION scans)
    // =========================================================================

    /**
     * control0 -- golden is the oracle, and it DISCRIMINATES rather than merely agreeing: the
     * 6.37.0 golden carries the chain deref, the 6.36.0 golden of the SAME basename does not,
     * and the fork emits the 6.36.0 file byte-identically today.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control0_goldenDerefsIn637AndNotIn636() throws IOException {
        String g637 = collapse(Files.readString(GOLDEN_A.resolve(CBC)));
        assertTrue(g637.contains(DEREF),
                "golden 6.37.0 must carry the chain deref at the carrier");
        if (!cellBAvailable()) {
            return;
        }
        String g636 = collapse(Files.readString(GOLDEN_B.resolve(CBC_GREEN_TWIN)));
        assertTrue(!g636.contains(DEREF),
                "golden 6.36.0 must NOT carry the chain deref -- the discriminator this law's"
                + " green argument rests on");
        assertTrue(g636.contains("final FieldWithMetaString fieldWithMetaString"),
                "golden 6.36.0 must carry the whole-output wrapper local instead");
        String fork636 = drrBOutput == null ? null : drrBOutput.get(CBC_GREEN_TWIN);
        assertNotNull(fork636, "the fork must emit the green twin");
        assertEquals(normalize(Files.readString(GOLDEN_B.resolve(CBC_GREEN_TWIN))),
                normalize(fork636),
                "the green twin must stay byte-identical (the no-move witness)");
    }

    /** control1 -- LAW 79, the whole-cell UNION scan on the carrier cell drr 6.37.0. */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control1_forkDrr637WholeCellEqualsGoldenFileByFile() throws IOException {
        assertNotNull(drrAOutput, "drr 6.37.0 generation did not run");
        assertEquals(List.of(), drrAGenErrors,
                "drr 6.37.0 reported a generation error - the scan is incomplete");
        assertUnionEqual(scan(drrAOutput), scan(readGoldenTree(GOLDEN_A)), drrAOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_DRR637, DOMAIN_DRR637);
    }

    /**
     * control2 -- LAW 77 route parity. The stamp itself is in {@code CollectionHandler} (shared
     * by both routes) but its consumers include {@code FunctionExpressionRenderer}, the class the
     * IR route substitutes, so route parity is load-bearing rather than a formality. The probe
     * measured the {@code missed=true} population route-identical (132/132); this re-proves it
     * per FILE on the real IR seams.
     */
    @Test
    @EnabledIf("cellAAndIrProviderAvailable")
    void corpus_control2_irRouteMatchesLegacyForTheCarrier() throws IOException {
        assertNotNull(drrAOutput, "drr 6.37.0 legacy-route generation did not run");
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "6.37.0", CELL_A_ROOT),
                new ArrayList<>());
        assertEquals(drrAOutput.get(CBC), irOut.get(CBC), "route divergence: " + CBC);
    }

    /**
     * control3 -- LAW 79 on the GREEN TWIN cell drr 6.36.0. The law must be byte-INERT here even
     * though its stamp fires on the twin file: the domain check below proves the twin is inside
     * the scan rather than merely absent.
     */
    @Test
    @EnabledIf("cellBAvailable")
    void corpus_control3_forkDrr636WholeCellEqualsGoldenFileByFile() throws IOException {
        assertNotNull(drrBOutput, "drr 6.36.0 generation did not run");
        assertEquals(List.of(), drrBGenErrors,
                "drr 6.36.0 reported a generation error - the scan is incomplete");
        assertTrue(drrBOutput.containsKey(CBC_GREEN_TWIN),
                "the green twin must be INSIDE this scan's domain, else control3 proves nothing"
                + " about it (LAW: a control scans the domain it claims)");
        assertUnionEqual(scan(drrBOutput), scan(readGoldenTree(GOLDEN_B)), drrBOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_DRR636, DOMAIN_DRR636);
    }

    /**
     * control4 -- LAW 79 on drr 5.61.0: the other measured green {@code missed=true} domain
     * (esma + fca {@code CustomBasketCodeRule}, both byte-identical today) AND the home of the
     * CONJUNCTIVE law-2 + law-9 carrier — law 9 being the one seat 30 BANKED to S31
     * ({@code target/seat30-instruments/law9-bank.md}), so its half never landed. This law alone
     * must leave the whole cell where it is.
     */
    @Test
    @EnabledIf("cellCAvailable")
    void corpus_control4_forkDrr561WholeCellEqualsGoldenFileByFile() throws IOException {
        assertNotNull(drrCOutput, "drr 5.61.0 generation did not run");
        assertEquals(List.of(), drrCGenErrors,
                "drr 5.61.0 reported a generation error - the scan is incomplete");
        assertTrue(drrCOutput.containsKey(CBC_ESMA) && drrCOutput.containsKey(CBC_FCA),
                "the two measured green missed=true files must be INSIDE this scan's domain");
        assertUnionEqual(scan(drrCOutput), scan(readGoldenTree(GOLDEN_C)), drrCOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_DRR561, DOMAIN_DRR561);
    }

    /**
     * control5 -- LAW 79 on drr 7.0.0: the UATPI ZERO-MOVEMENT adjudication. Law 2's stamp fires
     * on 4 {@code missed=true} rows per UATPI Leg in this cell and the claim is that they publish
     * no byte in the emitted file. This control is what turns that claim into a measurement --
     * and it is also where 20 of the 44 banked {@code mapItemToList} rows live, so
     * {@code m-law2s-kind} must fail here.
     */
    @Test
    @EnabledIf("cellDAvailable")
    void corpus_control5_forkDrr700WholeCellEqualsGoldenFileByFile() throws IOException {
        assertNotNull(drrDOutput, "drr 7.0.0 generation did not run");
        assertEquals(List.of(), drrDGenErrors,
                "drr 7.0.0 reported a generation error - the scan is incomplete");
        assertTrue(drrDOutput.keySet().stream().anyMatch(
                        k -> k.endsWith("/UnderlyingAssetTradingPlatformIdentifierLeg1Rule.java")),
                "the UATPI family must be INSIDE this scan's domain, else control5 proves nothing"
                + " about the zero-movement claim");
        assertUnionEqual(scan(drrDOutput), scan(readGoldenTree(GOLDEN_D)), drrDOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_DRR700, DOMAIN_DRR700);
    }

    // =========================================================================
    // The measured residue + domains (LAW 73: pin the SET, not the count)
    // =========================================================================

    /**
     * MEASURED at the seat-30 law-2 SECOND run (stamp + admit applied) and transcribed VERBATIM
     * from each control's own print -- never computed, never taken from the band lists (which are
     * supersets: 459 files held vs 128 true at the seat-29 head).
     *
     * <p><b>THE OVER-FIRE ADJUDICATION, and it is the headline of this run.</b> Across all four
     * cells there is <b>not one row where {@code fork > golden} on T1</b> -- every residue row is
     * a file where the fork emits FEWER derefs than golden, i.e. pre-existing band residue. An
     * over-stamp into {@code mapItemToList}/{@code mapItem} would have shown up as EXTRA derefs
     * on the {@code GetUnderlierProductIdentifier*} / {@code GetBasketConstituents*} families in
     * drr 7.0.0; it did not. <b>The law is measured non-entering over 5,994 scanned files in four
     * cells.</b>
     */
    private static final List<String> KNOWN_RESIDUE_DRR637 = List.of();
    private static final List<String> KNOWN_RESIDUE_DRR636 = List.of();

    /**
     * drr 5.61.0. Every row is a pre-existing band file. {@code CustomBasketCodeIdentifierRule}
     * ({@code fork=[3, 2, 1] golden=[4, 2, 1]}) and both {@code PlatformIdentifierRule}s are the
     * CONJUNCTIVE carriers this law cannot heal alone -- they need the FER deref rung that seat
     * 30 drafted as law 9 and <b>BANKED to S31</b> ({@code target/seat30-instruments/law9-bank.md}:
     * three measured rounds, the statement-seat coercion still crashing generation on the asic
     * carrier). <b>No seat-30 commit will ever own this re-pin</b> -- these three rows are the
     * standing LAW-81 tripwire that fires when the S31 law lands, and the re-pin is owed to THAT
     * commit, transcribed from its own measured print.
     */
    // LAW 81 re-pin (v3.1 flip seat 31, law 1): the asic CustomBasketCodeIdentifier +
    // PlatformIdentifier rows LEFT this list - both files healed WHOLE (byte-identical
    // to golden, locked by IteElseArmRecoveredMetaDerefSeatTest corpus_c1/c2). The
    // heal-tripwire fired exactly as prescribed and the list is re-pinned from the
    // control's own measured print (delta = the two rows REMOVED, nothing else).
    // the NotionalAmountLeg1/2 + NotionalCurrencyLeg2 rows LEFT this list at seat 32: law C.3 (ruleThenArmLadderNestedTreeAdmit) healed
    // NotionalAmountLeg1/2 + NotionalCurrencyLeg2 WHOLE in drr 5.61.0; transcribed from this control's own print (C3-trip1*.log).
    // LAW 81 re-pin (v3.1 flip seat 33, law F.A): the cftc NotionalCurrencyLeg1Rule + jfsa
    // NotionalCurrencyOfLeg1Rule rows LEFT this list - both files healed WHOLE by facet
    // blockArmWrapperHopDeref (byte-identical to golden, locked by BlockArmWrapperHopDerefSeatTest
    // corpus_c1/c2); the list is EMPTY, transcribed from this control's own print (FA-trip1.log).
    private static final List<String> KNOWN_RESIDUE_DRR561 = List.of();
            // LAW 81 (seat 33, law F.B): the NotionalLeg2Rule row fork=[2, 3, 1] golden=[2, 3, 2] LEFT - the file is WHOLE (FB-trip1.log print)
    // LAW 81 re-pin (v3.1 flip seat 31, law 1b): the mas PlatformIdentifierRule row LEFT
    // this list - the file healed WHOLE (byte-identical to golden, locked by
    // MapperFormRuleRootArmSeatTest corpus_c1). Re-pinned from the control's own measured
    // print (delta = the one row REMOVED, nothing else).

    /**
     * drr 7.0.0 -- the UATPI ZERO-MOVEMENT adjudication and the banked-halves over-fire domain.
     * {@code UnderlyingAssetTradingPlatformIdentifierLeg1/Leg2Rule} read
     * {@code fork=[3, 3, 3] golden=[9, 3, 3]}: the fork is SHORT six derefs, exactly the six
     * nav-receiver hops the draft predicted, and its {@code mapSingleToItem} count (T2 = 3)
     * MATCHES golden -- so the four {@code missed=true} rows per Leg that this law's stamp does
     * fire on publish no byte, which is the claim {@code corpus_control5} was written to
     * adjudicate. Likewise {@code GetBasketConstituents} / {@code UnderlierProductIdentifier} sit
     * below golden, never above.
     *
     * <p><b>RE-MEASURED at the seat-30 chain head {@code e223ce19}</b> — 11 entries, transcribed
     * VERBATIM from {@code corpus_control5}'s own failing print (LAW 81); law 2 measured this set
     * at 12 in its own commit, and two later laws of the SAME seat healed one of its members.
     *
     * <p><b>RE-MEASURED at the seat-31 chain head {@code f2a4d5c0}</b> — 9 entries (the print:
     * "in 9 file(s)"), transcribed VERBATIM from the same control's own failing print (LAW 81):
     * two rows LEFT this seat, both noted inline below — GetUnderlierLEIForCredit via law 2 rung 2
     * (in the law-2 commit) and UnderlierBasketIdentifier via law 4a (at the chain).
     */
    private static final List<String> KNOWN_RESIDUE_DRR700 = List.of(
            // GetBasket.java (was fork=[0, 0, 0] golden=[1, 0, 0]) left this list at seat 30, in a
            // LATER commit of this same seat: laws 6 + 7 TOGETHER
            // (condArmMultiMetaElementDeref hunk 1 + multiEmptyElseArmToBuilder hunk 2) healed it
            // WHOLE in all four drr 7.x cells, so the missing element-wise "Type coercion" hop (T1)
            // now renders. Golden's T1 = 1 keeps the file inside the union domain, so
            // DOMAIN_DRR700 is UNMOVED at 1360. This is the within-seat LAW-81 hand-off: the pin
            // was correct when law 2 measured it and the control went RED, as designed, when the
            // later law healed the row.
            // the GetBasketConstituents row (fork=[1, 2, 4] golden=[1, 2, 6]) LEFT this list at seat 33: law A.3
            // (ladderBlockCtorNestedExtract) block-converted both ctor-field ladders, so the two typed block
            // terminals now render (T3 = ofNull() 4 -> 6 = golden; T2 = .mapSingleToItem( UNMOVED at 2 - the BODY changed, not the method); the file stays BANDED on its A.5/A.1/A.2 residue
            // (LadderBlockCtorNestedExtractSeatTest pins it by name); transcribed from the control print (A3-trip1.log).
            // UnderlierBasketIdentifier.java (was fork=[0, 0, 0] golden=[2, 0, 0]) left this list at
            // seat 31: law 4a (choiceOptionNavLadderDeepHop - the FER SET-seat option ladder walks the NESTED choice option tree
            // through ChoiceSwitchSupport.findChoiceOptionPath and derefs the META option hop into the bare output)
            // healed it WHOLE in all four drr 7.x cells, so golden's two element-wise "Type coercion" hops (T1) now render.
            // Golden's T1 = 2 keeps the file inside the union domain: DOMAIN_DRR700 UNMOVED at 1360.
            // the UnderlierProductIdentifier row (fork=[10, 1, 14] golden=[20, 1, 14]) LEFT this list at seat 32:
            // law A.2 (wrapperItemReceiverBind) healed it WHOLE in all four drr 7.x cells; transcribed from
            // this control's own print (A2-trip1.log). DOMAIN_DRR700 UNMOVED (golden T1=20 keeps it in the union).
            // the IndicatorOfTheUnderlyingIndexRule row (fork=[3, 1, 0] golden=[4, 1, 1]) LEFT this list at seat 32: law D.2
            // (extractBodyMultiDefaultTernary, on law D.1's left deref) healed it WHOLE in all four drr 7.x cells;
            // transcribed from this control's own print (D2-trip1.log).
            // GetUnderlierLEIForCredit.java (was fork=[0, 0, 1] golden=[1, 0, 1]) left this list at
            // seat 31: law 2 rung 2 (lambdaItemReceiverType - the ADD-terminal deref) healed it WHOLE
            // in all four drr 7.x cells (locked whole by ReceiverRenderTypingSeatTest corpus_c1 at 7.0.0).
            // Re-pinned from the control's own measured print (delta = the one row REMOVED, nothing else).
            // (The law-2 commit's own note called this "the drr 5.61.0 row" - there is no 5.61.0 sibling:
            // the file exists only under drr 7.0-7.3; corrected at the review of #603, MF-4.)
            // the UnderlyingIndexIndicatorRule row (fork=[4, 3, 0] golden=[5, 3, 0]) LEFT this list at seat 32: law D.1 (defaultJoinDerefAtCollapsedLeft)
            // healed it WHOLE in all four drr 7.x cells; transcribed from this control's own print (D1-trip1.log).
            // the Price row (fork=[17, 0, 3] golden=[20, 0, 3]) LEFT this list: law C.2 (heteroMetaDefaultJoinDeref + iteArmMultiDefaultTernary)
            // took the rung-1 default join, the last residue after C.1 - Price is WHOLE in all four drr 7.x cells;
            // transcribed from this control's own print (C2-trip1.log), a pure row removal.
            // LAW 81 re-pin (seat 33, law C.1): fork=[10, 0, 0] -> fork=[17, 0, 3] - the statement ladder moved this scan's fork side toward golden; the file stays BANDED on C.2's default join; from C1-trip1.log.
            // the QuantityUnitOfMeasure row (fork=[3, 1, 0] golden=[2, 1, 0]) LEFT this list: law B.24 (defaultJoinHeteroMetaDerefBoth +
            // iteArmMetaCollapseDerefSinkChannel + three in-seat rungs) healed the file WHOLE in all four drr 7.x
            // cells - the band's last four files; transcribed from this control's own print (B24-trip1.log),
            // a pure row removal (was == expected minus it).
            // LAW 81 re-pin (seat 33, law C.2): fork=[1, 1, 0] -> fork=[3, 1, 0] - R3a's join deref moved this scan's fork side (LAW 80 IMPROVED-not-whole, planned; QUOM closes at B.24); from C2-trip1.log.
            // the TotalNotionalQuantity row (fork=[5, 2, 0] golden=[12, 2, 1]) LEFT this list: law D.3 (fnDeepCondBaseConfinedArmChainAdmit,
            // seven rungs) healed the file WHOLE in all four drr 7.x cells; transcribed from this control's own
            // print (D3-trip1.log), a pure row removal (was == expected minus it).
            );
            // the UnderlyingAssetTradingPlatformIdentifierLeg1/Leg2 rows LEFT this list at seat 32: law A.1
            // (choiceOptionProjectionTypeId) healed both WHOLE in all four drr 7.x cells; transcribed from this
            // control's own print (A1-trip1.log).

    /**
     * The union domains, MEASURED at the second run (LAW 73). A NEGATIVE value means an unpinned
     * call site and makes {@code assertUnionEqual} print the measured values instead of merely
     * failing.
     */
    private static final int DOMAIN_DRR637 = 1618;
    private static final int DOMAIN_DRR636 = 1618;
    private static final int DOMAIN_DRR561 = 1408;
    private static final int DOMAIN_DRR700 = 1360;

    /**
     * (T1, T2, T3) per file:
     * <ul>
     *   <li><b>T1</b> -- the law's ADDED shape: {@code >map("Type coercion",} hops, the exact
     *       token both consumers emit.</li>
     *   <li><b>T2</b> -- the seat's own footprint: {@code .mapSingleToItem(} occurrences, so a
     *       stamp that changes METHOD SELECTION (not just typing) is caught too.</li>
     *   <li><b>T3</b> -- the OVER-FIRE NET: total {@code ofNull()} occurrences. Deliberately
     *       global: the typed-empty terminal is what publishes {@code chainItemType}, so a
     *       mis-stamped chain that re-types an empty anywhere in the cell fails here, including
     *       at seats this law does not name.</li>
     * </ul>
     *
     * <p><b>ORDERING MATTERS and the first draft had it wrong.</b> {@code codeOnly} strips string
     * literals AND scans {@code //} comments to the next newline -- so running it AFTER
     * {@code collapse} (which removes every newline) would swallow the rest of the file from the
     * generated header's first {@code //} comment onward, and running it at all would delete the
     * {@code "Type coercion"} literal T1 counts. T1 therefore reads the RAW collapsed text (the
     * literal is the token) while T2/T3 read comment-stripped-THEN-collapsed text.
     */
    private static Map<String, int[]> scan(Map<String, String> tree) {
        Map<String, int[]> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : tree.entrySet()) {
            String rawFlat = collapse(e.getValue());
            String codeFlat = collapse(codeOnly(e.getValue()));
            int t1 = count(rawFlat, ">map(\"Type coercion\",");
            int t2 = count(codeFlat, ".mapSingleToItem(");
            int t3 = count(codeFlat, "ofNull()");
            if (t1 + t2 + t3 > 0) {
                out.put(e.getKey(), new int[] {t1, t2, t3});
            }
        }
        return out;
    }

    private static int count(String haystack, String needle) {
        int n = 0;
        int from = 0;
        while ((from = haystack.indexOf(needle, from)) >= 0) {
            n++;
            from += needle.length();
        }
        return n;
    }

    /**
     * The union assert (LAW 73: pin the SET, not the count).
     *
     * <p><b>The unpinned sentinel PRINTS rather than merely failing.</b> A control whose domain is
     * still {@code -1} is an EXPECTED failure -- but a bare "you have not pinned this" message
     * costs a whole corpus run and yields nothing, so the sentinel now emits the measured domain
     * size and the complete mismatch list, formatted as the Java source lines to paste back. One
     * run therefore produces every value the four controls need.
     */
    private static void assertUnionEqual(Map<String, int[]> a, Map<String, int[]> b,
            java.util.Set<String> emittedA, String aName, String bName, List<String> knownResidue,
            int expectedDomain) {
        List<String> mismatched = new ArrayList<>();
        java.util.Set<String> universe = new java.util.TreeSet<>(a.keySet());
        universe.addAll(b.keySet());
        universe.retainAll(emittedA);
        int[] zero = new int[3];
        for (String key : universe) {
            int[] ac = a.getOrDefault(key, zero);
            int[] bc = b.getOrDefault(key, zero);
            if (!java.util.Arrays.equals(ac, bc)) {
                mismatched.add(key + " " + aName + "=" + java.util.Arrays.toString(ac)
                        + " " + bName + "=" + java.util.Arrays.toString(bc));
            }
        }
        if (expectedDomain < 0) {
            StringBuilder print = new StringBuilder();
            print.append("UNPINNED CONTROL (LAW 73) - EXPECTED FAILURE, measured values below.\n");
            print.append("Paste these into the suite and re-run:\n\n");
            print.append("    private static final int DOMAIN_<CELL> = ")
                 .append(universe.size()).append(";\n\n");
            print.append("    private static final List<String> KNOWN_RESIDUE_<CELL> = List.of(");
            if (mismatched.isEmpty()) {
                print.append(");   // EMPTY - the law is byte-inert over this cell\n");
            } else {
                print.append('\n');
                for (int i = 0; i < mismatched.size(); i++) {
                    print.append("            \"").append(mismatched.get(i)).append('"')
                         .append(i + 1 < mismatched.size() ? ",\n" : ");\n");
                }
            }
            print.append("\nmismatched files: ").append(mismatched.size())
                 .append("   union domain: ").append(universe.size());
            assertTrue(false, print.toString());
        }
        assertEquals(knownResidue, mismatched,
                "(T1, T2, T3) differ beyond the named residue in " + mismatched.size() + " file(s)");
        assertEquals(expectedDomain, universe.size(),
                "the union domain must equal the emitted token-bearing files (" + expectedDomain + ")");
    }

    // =========================================================================
    // Harness (the seat-29 SetTerminalRuleMultiSeatTest shape verbatim)
    // =========================================================================

    private static Map<String, String> drrAOutput;
    private static List<String> drrAGenErrors;
    private static Map<String, String> drrBOutput;
    private static List<String> drrBGenErrors;
    private static Map<String, String> drrCOutput;
    private static List<String> drrCGenErrors;
    private static Map<String, String> drrDOutput;
    private static List<String> drrDGenErrors;
    private static Map<String, String> drrEOutput;
    private static List<String> drrEGenErrors;

    @BeforeAll
    static void generateCells() throws IOException {
        if (cellAAvailable()) {
            List<String> errs = new ArrayList<>();
            drrAOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "6.37.0", CELL_A_ROOT), errs);
            drrAGenErrors = errs;
        }
        if (cellBAvailable()) {
            List<String> errs = new ArrayList<>();
            drrBOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "6.36.0", CELL_B_ROOT), errs);
            drrBGenErrors = errs;
        }
        if (cellCAvailable()) {
            List<String> errs = new ArrayList<>();
            drrCOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "5.61.0", CELL_C_ROOT), errs);
            drrCGenErrors = errs;
        }
        if (cellDAvailable()) {
            List<String> errs = new ArrayList<>();
            drrDOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", CELL_D_ROOT), errs);
            drrDGenErrors = errs;
        }
        if (cellEAvailable()) {
            List<String> errs = new ArrayList<>();
            drrEOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "6.38.0", CELL_E_ROOT), errs);
            drrEGenErrors = errs;
        }
    }

    private static void lockCell(Map<String, String> out, List<String> genErrors, Path golden,
            String path, String cellName) throws IOException {
        assertNotNull(out, cellName + " generation did not run - corpus unavailable?");
        List<String> lockedErrors = genErrors.stream().filter(e -> e.contains(path)).toList();
        assertTrue(lockedErrors.isEmpty(),
                "the generator reported errors for the locked file " + path + ": " + lockedErrors);
        String generated = out.get(path);
        assertNotNull(generated, "not generated in " + cellName + ": " + path);
        Path goldenPath = golden.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "golden missing: " + goldenPath);
        assertEquals(normalize(Files.readString(goldenPath)), normalize(generated),
                "generated " + cellName + " output must byte-match golden (newline-normalized)"
                + " for " + path + " - seat 30 law 2: a mapSingleToItem block lambda whose arm"
                + " join rendered a META element publishes it as the chain's type.");
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

    /** The cell through the REAL {@code IRGeneration} seams (the D11 ON ring's wiring). */
    private static Map<String, String> generateCellOnIrRoute(D11CorpusRegressionTest.CellSpec cell,
            List<String> errors) throws IOException {
        String previous = System.getProperty(IRGeneration.PROPERTY);
        System.setProperty(IRGeneration.PROPERTY, "true");
        try {
            assertNotNull(IRGeneration.providerOrNull(),
                    "the IR provider must be resolvable under -Pir-on, else this is not an"
                    + " ON-route render");
            var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
            var gm = new GeneratorModel(corpus.workspace(),
                    D11CorpusRegressionTest.emissionFilter(cell));
            var typeUtil = new JavaTypeUtil();
            var typeTranslator = new JavaTypeTranslator(typeUtil);
            FunctionGenerator funcGen = IRGeneration.functionGenerator(gm, typeTranslator, typeUtil);
            assertTrue(!funcGen.getClass().equals(FunctionGenerator.class),
                    "the seam must hand back the IR-route FunctionGenerator, got "
                    + funcGen.getClass());
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

    private static Map<String, String> readGoldenTree(Path root) throws IOException {
        Map<String, String> out = new LinkedHashMap<>();
        try (var stream = Files.walk(root)) {
            stream.filter(p -> p.toString().endsWith(".java")).sorted().forEach(p -> {
                try {
                    out.put(root.relativize(p).toString().replace('\\', '/'), Files.readString(p));
                } catch (IOException e) {
                    throw new AssertionError("golden read failed: " + p, e);
                }
            });
        }
        return out;
    }

    private static void collect(List<String> sink, List<GenerationException> errors) {
        if (errors != null) {
            errors.forEach(e -> sink.add(e.getTargetPath() + " - " + e));
        }
    }

    /** Collapse every run of whitespace to one space, so a token split across lines still reads. */
    private static String collapse(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        boolean inWs = false;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n') {
                inWs = true;
                continue;
            }
            if (inWs && sb.length() > 0) {
                sb.append(' ');
            }
            inWs = false;
            sb.append(ch);
        }
        return sb.toString();
    }

    /** Strip comments plus string literals so a javadoc or label never counts as code. */
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

    private static RLinkingResult linking;
    private static RModel mainModel;
    private static Map<String, String> fixtureOut;

    private static void link() throws IOException {
        if (linking == null) {
            RModel main = AstBuilder.buildFromString(MODEL, "seat30f13stamp.rosetta");
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
                .forEach(e -> errors.add(e.getTargetPath() + " - " + e));
        fg.generateWithErrors(out)
                .forEach(e -> errors.add(e.getTargetPath() + " - " + e));
        if (!errors.isEmpty()) {
            throw new AssertionError("fixture generation errors (a broken fixture"
                    + " must fail loudly, not skip): " + errors);
        }
        return out;
    }

    private static Map<String, String> fixture() throws IOException {
        if (fixtureOut == null) {
            fixtureOut = render(m -> "census.seat30f13stamp".equals(m.namespace()));
        }
        return fixtureOut;
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
                        failures.add(p + " - " + e);
                    }
                });
        if (!failures.isEmpty()) {
            throw new AssertionError("[BlockLambdaSingleItemChainStampSeatTest] builtins parse"
                    + " failures: " + String.join("; ", failures));
        }
        return models;
    }
}
