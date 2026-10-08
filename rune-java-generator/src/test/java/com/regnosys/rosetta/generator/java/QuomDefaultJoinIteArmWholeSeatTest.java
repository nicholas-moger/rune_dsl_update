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
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.reports.ReportGenerator;
import com.regnosys.rosetta.generator.java.reports.RuleGenerator;
import com.regnosys.rosetta.generator.java.spi.IRGeneration;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.symbols.RLinkingResult;
import com.regnosys.rosetta.symbols.RWorkspace;

/**
 * SEAT 33, law B.24 -- <b>ONE commit, TWO rungs that share ONE method-level naming group, and the
 * band's LAST four files.</b>
 *
 * <ul>
 *   <li><b>B.2</b> facet {@code defaultJoinHeteroMetaDerefBoth} (census F10 (b)): a
 *       HETEROGENEOUS-meta {@code default} -- the LEFT's compiled item and the RIGHT's bound
 *       collapse item are DIFFERENT wrapper classes over the SAME value type -- joins at the BARE
 *       value and BOTH sides deref.</li>
 *   <li><b>B.4</b> facet {@code iteArmMetaCollapseDerefSinkChannel} (census F13; the seat-32 G.3
 *       draft, re-sited): the #368 arm's wrapper-local hoist + guarded deref is opened to the
 *       STATEMENT-SINK channel, not only the lambda channel.</li>
 * </ul>
 *
 * <p><b>Why one commit</b> (verdicts33-B section 5, the naming law). Golden's method-level group is
 * {@code fieldWithMetaNonNegativeQuantitySchedule} <b>0</b> (line 68, B.2's right-hand local),
 * <b>1</b> (line 78, B.4's then-arm local), <b>2</b> (line 82, B.4's else-arm local) -- ONE group
 * of THREE, numbered in {@code StatementHoistSession} REGISTRATION order ("the bare base name for a
 * singleton group, {@code base0..n-1} in registration order otherwise", {@code :298-301}). B.2
 * alone registers a SINGLETON and mints the BARE name; B.4 alone registers two and mints
 * {@code <base>} + {@code <base>0}. Neither is golden. (The dossier's "B.2 before B.3 may mint the
 * bare name if the lambda claims have not been made" is REFUTED: law B.3's two locals live in the
 * SEPARATE {@code lambdaGroups} registry, {@code :365-367}, and cannot touch the method group.)
 *
 * <p><b>The carrier</b>: {@code drr/standards/iosco/cde/version1/quantity/functions/
 * QuantityUnitOfMeasure.java} x drr 7.0/7.1/7.2/7.3 -- <b>4 whole-file heals, and the band's last
 * four</b>. With laws B.1 (the inner chain's hoist) and B.3 (the bare rule references) already
 * landed, this commit closes the file:
 * <pre>
 * golden:68 final FieldWithMetaNonNegativeQuantitySchedule fieldWithMetaNonNegativeQuantitySchedule0 = MapperS.of(thenArg6.get()).get();
 * golden:69 final MapperS&lt;NonNegativeQuantitySchedule&gt; thenArg7 = MapperS.of(thenArg3.&lt;NonNegativeQuantitySchedule&gt;map("Type coercion", … ).getOrDefault((fieldWithMetaNonNegativeQuantitySchedule0 == null ? null : ….getValue())));
 * fork:     final MapperS&lt;FieldWithMetaNonNegativeQuantitySchedule&gt; thenArg4 = thenArg3.getOrDefault(&lt;chain&gt;);          &lt;-- B.2
 * golden:78 final FieldWithMetaNonNegativeQuantitySchedule fieldWithMetaNonNegativeQuantitySchedule1 = &lt;arm chain&gt;.first().get();
 * golden:80 ifThenElseResult = fieldWithMetaNonNegativeQuantitySchedule1 == null ? null : ….getValue();
 * fork:     ifThenElseResult = &lt;arm chain&gt;.first().get();                                                              &lt;-- B.4 then-arm
 * golden:82/84 the else-arm twin                                                                                       &lt;-- B.4 else-arm
 * </pre>
 *
 * <p><b>Neither rung is a new emitter.</b> B.2's seat, {@code SetOperationHandler
 * .tryThenBoundMetaArgDeref} (facet {@code getOrDefaultArgMetaDeref}, PR #363), already emits
 * golden's ENTIRE right half AND the outer {@code MapperS.of(} re-wrap via
 * {@code JavaExpression.selfUnwrapping}; it declined at a blanket "the LEFT's compiled item is not
 * itself a wrapper" early return. B.4's seat, {@code ControlFlowHandler:2644-2692} (facet
 * {@code iteArmMetaCollapseDeref}, PR #368 F-A3), already emits golden's exact two-statement arm
 * form with exactly the right {@code armWrapper.getValueType() == declClazz} discriminator; it was
 * gated LAMBDA-CHANNEL-ONLY and said so in its own javadoc. Both changes are one conjunct and one
 * placement fork, copied from siblings in the same files (LAW 69).
 *
 * <p><b>The measured radii, BOTH routes</b> (probe round 2 at the seat-33 base head
 * {@code fa49da010}):
 * <ul>
 *   <li>{@code [P33-DEFJOIN]} (1,212 rows): the would-fire set {@code sameWrap=false vtEq=true} is
 *       <b>12 rows / TWO {@code where=} / ZERO green</b> -- {@code fn:QuantityUnitOfMeasure} 8
 *       ({@code rightChan=walker}) + {@code fn:Price} 4 ({@code rightChan=stamp}, which is law
 *       C.2's rung at a DIFFERENT seat). The protected homogeneous set {@code sameWrap=true} is 65
 *       rows / 5 groups: {@code rule:QuantitySchedule} 20 (GREEN, named by no dossier),
 *       {@code fn:MessageID} 9 (the nine GREEN cells), {@code fn:GetBasketConstituents} 4 (golden
 *       {@code :173}), and QUOM's OWN levels 1-3 (24) and 5 (8).</li>
 *   <li>{@code [P33-ITEARM]} (2,607 rows / 59 distinct {@code where=}): the sink-channel would-fire
 *       set {@code lambdaCh=false armWrapper=true vtEqDecl=true} is <b>36 rows / TWO {@code where=}
 *       / ZERO green</b> -- {@code fn:QuantityUnitOfMeasure} 16 + {@code fn:Price} 20.</li>
 * </ul>
 *
 * <p><b>THE ORDERING IS LOAD-BEARING, not cosmetic.</b> Golden {@code Price.java} carries ZERO
 * {@code final ...WithMetaPriceSchedule} locals (its arms render in-mapper into a
 * {@code MapperC<PriceSchedule>} slot), so opening the sink channel BEFORE Price's Mapper-form-slot
 * law would emit five hoisted locals per cell that golden does not have -- moving a band file AWAY
 * from golden. After seat-33 law C.1 lands, Price's 20 rows read {@code mapperFormSlot=true} and
 * the EXISTING {@code !mapperFormSlot} conjunct excludes them for free, with no new code. This law
 * therefore lands after C.1/C.2 (charter ordering constraint 1), and {@code e2} is the fixture that
 * locks that exclusion so it cannot silently stop holding.
 *
 * <p><b>CLAIMED sets -- measured by the chain, not by this file.</b>
 * <ul>
 *   <li><b>RED at the law's parent head</b> (default route AND {@code -Pir-on}): {@code a1},
 *       {@code a2}, {@code a3}, {@code a4} (ON only), {@code corpus_c1}, {@code corpus_c2},
 *       {@code corpus_control1}. GREEN in both states: {@code e1}, {@code e2},
 *       {@code corpus_control0}, {@code corpus_control2}.</li>
 *   <li><b>GREEN at the law's head</b>: all, both routes.</li>
 *   <li><b>{@code m-lawB2-samewrap}</b> (drop the "wrapper CLASSES must DIFFER" conjunct):
 *       CLAIMED {@code e1}, {@code corpus_c1}, {@code corpus_c2}, {@code corpus_control1} FAIL
 *       (the homogeneous green population takes the deref). NOT empty: 65 measured rows.</li>
 *   <li><b>{@code m-lawB2-leftderef}</b> (keep the admission, sever the LEFT deref): CLAIMED
 *       {@code a1}, {@code corpus_c1}, {@code corpus_c2}, {@code corpus_control1} FAIL;
 *       {@code a2}/{@code a3} may survive (the arm locals and their numbering do not depend on
 *       the left). Isolates the two halves of one rung.</li>
 *   <li><b>{@code m-lawB4-sinkchannel}</b> (revert the gate to lambda-channel-only): CLAIMED
 *       {@code a2}, {@code a3}, {@code corpus_c1}, {@code corpus_c2}, {@code corpus_control1}
 *       FAIL; {@code a1} survives. Note {@code a3} fails for TWO reasons at once -- the arm locals
 *       vanish AND B.2's local re-mints the bare name as a singleton -- which is the cross-rung
 *       coupling the one-commit rule exists for.</li>
 *   <li><b>{@code m-lawB4-vteq}</b> (sever the arm's value-type equality): <b>DECLARED EMPTY</b>
 *       (LAW 82). All 158 {@code armWrapper=true} rows also read {@code vtEqDecl=true}, and the
 *       29 PriorUti/PriorUTI files never reach this anchor at all
 *       ({@code --where PriorUti --where PriorUTI} = 0 rows). The conjunct is kept as
 *       defence-in-depth and the lane is kept so its status is recorded as MEASURED-EMPTY rather
 *       than assumed load-bearing. No fixture in this suite exercises a wrapper-TYPED
 *       {@code ifThenElseResult}, so the lane is empty at fixture grain too -- stated, not
 *       hidden.</li>
 * </ul>
 *
 * <p><b>LAW 77 -- INHERITS, no IR twin.</b> {@code IRExpressionCompiler.visitDefault:3439-3442} is
 * {@code tryEmitFromIR(...).orElseGet(() -> super.visitDefault(...))} and
 * {@code IRControlFlowHandler} overrides exactly ONE method
 * ({@code ifThenElseResultBaseName}); {@code [P33-DEFJOIN]}'s four carrier groups and
 * {@code [P33-ITEARM]}'s 16 carrier rows were identical on both routes. {@code a4} and
 * {@code corpus_control2} re-prove it on the real seam.
 *
 * <p><b>LAW 74.</b> B.2 repairs {@code javac33} C7 line <b>485</b> ({@code incompatible types:
 * NonNegativeQuantitySchedule cannot be converted to FieldWithMetaNonNegativeQuantitySchedule} at
 * the {@code thenArg6} decl); B.4 repairs lines <b>479</b> and <b>482</b>
 * ({@code FieldWithMetaNonNegativeQuantitySchedule cannot be converted to
 * NonNegativeQuantitySchedule} at {@code .first().get();}, one per arm). With B.1's 470 and B.3's
 * 472/474/487x2, <b>C7 goes 8 -&gt; 0</b>.
 *
 * <p><b>LAW 82 - MEASURED by the receipts chain, run 1 at ce1a06292 (final33.status; every
 * figure below is transcribed from the chain's own logs, never from this file's earlier
 * CLAIMED paragraphs, which it supersedes).</b> GREEN 11/0F/2skip default (f33-green-default.log) /
 * 11/0F/0skip {@code -Pir-on} (f33-green-on.log); RED at the pre-seat base {@code fa49da010}: default
 * 6F = a1, a2, a3, corpus_c1, corpus_c2, corpus_control1; {@code -Pir-on} 6F = a1, a2, a3, corpus_c1, corpus_c2, corpus_control1 (f33-red-{default,on}.log).
 * Mutation lanes ({@code mut33.py}, the default profile; {@code lanes33.py --summary}):
 * <ul>
 *   <li><b>{@code m-lawB24-samewrap}</b> ({@code SOH_PAIRS_MUT_SAMEWRAP}): MEASURED 11/1F/2skip = e1 - RE-SCORED: e1 alone - c1, c2, control1 HELD: QUOM's join is heterogeneous by construction (the sever cannot move it) and the green over-fire this sever causes (measured by PriceLadderWhole's samewrap lane at control0/control1) is invisible to THIS suite's meta-hoist scan tokens; the claim over-stated this suite's reach.</li>
 *   <li><b>{@code m-lawB24-leftderef}</b> ({@code SOH_PAIRS_MUT_LEFTDEREF}): MEASURED 11/4F/2skip = a1, corpus_c1, corpus_c2, corpus_control1 - MATCH (a1, c1, c2, control1).</li>
 *   <li><b>{@code m-lawB24-sinkchannel}</b> ({@code CFH_PAIRS_MUT_SINKCHANNEL}): MEASURED 11/6F/2skip = a1, a2, a3, corpus_c1, corpus_c2, corpus_control1 - RE-SCORED (superset): a1 fell as well as the claimed a2, a3, c1, c2, control1 - the fixture's join consumer sits on the sink channel too, so the gate revert takes its deref with it.</li>
 *   <li><b>{@code m-lawB24-vteq}</b> ({@code CFH_PAIRS_MUT_VTEQ}): MEASURED 11/0F/2skip = (none) - EMPTY-as-declared at both grains (all 158 armWrapper=true rows read vtEqDecl=true).</li>
 *   <li><b>{@code m-lawB24-thenright}</b> ({@code SOH2_PAIRS_MUT_THENRIGHT}): MEASURED 11/3F/2skip = corpus_c1, corpus_c2, corpus_control1 - MATCH (c1, c2, control1; a1..a3 held).</li>
 *   <li><b>{@code m-lawB24-itetype}</b> ({@code CFH2_PAIRS_MUT_ITETYPE}): MEASURED 11/4F/2skip = a3, corpus_c1, corpus_c2, corpus_control1 - MATCH (a3, c1, c2, control1).</li>
 *   <li><b>{@code m-lawB24-extractbase}</b> ({@code RH2_PAIRS_MUT_EXTRACTBASE}): MEASURED 11/3F/2skip = corpus_c1, corpus_c2, corpus_control1 - MATCH (c1, c2, control1; the fixtures held).</li>
 * </ul>
 */
class QuomDefaultJoinIteArmWholeSeatTest {

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

    /** Cell A = drr 7.0.0 -- the carrier and all three named green witnesses live here. */
    private static final Path CELL_A_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path GOLDEN_A = CELL_A_ROOT.resolve("rosetta-source/src/generated/java");

    /**
     * Cell B = drr 7.1.0. The carrier's golden and fork renders are BYTE-IDENTICAL across
     * 7.0/7.1/7.2/7.3 (measured over the probe round's band dumps for all four cells), so two
     * cells prove the heal is not a one-cell accident; 7.2.0 and 7.3.0 ride the whole-matrix
     * checkpoint rather than doubling this suite's generation cost.
     */
    private static final Path CELL_B_ROOT = Path.of("../test-corpus/drr/drr-7.1.0");
    private static final Path GOLDEN_B = CELL_B_ROOT.resolve("rosetta-source/src/generated/java");

    static boolean cellAAvailable() {
        return Files.isDirectory(GOLDEN_A);
    }

    static boolean cellBAvailable() {
        return Files.isDirectory(GOLDEN_B);
    }

    static boolean cellAAndIrProviderAvailable() {
        return cellAAvailable() && irProviderOnClasspath();
    }

    private static final String QUOM =
            "drr/standards/iosco/cde/version1/quantity/functions/QuantityUnitOfMeasure.java";
    /** The nine-cell homogeneous-meta green witness ({@code FieldWithMetaString} both sides). */
    private static final String MESSAGE_ID =
            "drr/regulation/common/trade/link/functions/MessageID.java";
    /** The 20-row homogeneous-meta green witness NO dossier named ({@code ReferenceWithMetaNNQS}). */
    private static final String QUANTITY_SCHEDULE_RULE =
            "drr/base/trade/quantity/reports/QuantityScheduleRule.java";
    /** Golden {@code :173} -- the third homogeneous-meta green witness ({@code FieldWithMetaString}). */
    private static final String GET_BASKET_CONSTITUENTS =
            "drr/base/trade/basket/functions/GetBasketConstituents.java";

    /**
     * The fixture, reduced from {@code func QuantityUnitOfMeasure} (drr 7.x
     * {@code standards-iosco-cde-version1-quantity-func.rosetta:343-359}) with its cardinalities
     * and metadata kinds preserved: the {@code default} LEFT is a SINGLE
     * {@code [metadata reference]} attribute ({@code ReferenceWithMeta...}, CDM's
     * {@code ResolvablePriceQuantity.quantitySchedule}) and the RIGHT is a MULTI
     * {@code [metadata location]} attribute ({@code FieldWithMeta...}, CDM's
     * {@code PriceQuantity.quantity NonNegativeQuantitySchedule (0..*) [metadata location]}) --
     * two DIFFERENT wrapper classes over ONE value type, which is the whole discriminator. The
     * ite sits at a METHOD-STATEMENT seat (the RIGHT of the second {@code then default}), which is
     * the whole point of B.4: it has no lambda channel at all.
     *
     * <p>{@code A1} carries BOTH rungs in ONE function so {@code a3} can read the shared naming
     * group; {@code e1} and {@code e2} are the two near-miss shapes.
     *
     * <p>Lexer-safe identifiers: no {@code tag}, {@code single}, {@code label}, {@code value},
     * {@code key}.
     */
    private static final String MODEL = """
            namespace census.seat33f10bf13
            version "1.0.0"

            type Seat33Sched:
                uom string (0..1)

            type Seat33Resolvable:
                quantitySchedule Seat33Sched (0..1)
                    [metadata reference]

            type Seat33PriceQty:
                quantity Seat33Sched (0..*)
                    [metadata location]
                refQuantity Seat33Sched (0..*)
                    [metadata reference]

            type Seat33Lot:
                priceQuantity Seat33PriceQty (0..*)

            type Seat33Payout:
                commodityPayout Seat33Resolvable (0..1)
                tradeLot Seat33Lot (0..1)
                optionPayout Seat33Sched (0..1)

            func A1HeteroJoinAndIteArmCollapse: <"a1/a2/a3 - THE QuantityUnitOfMeasure SHAPE: a ReferenceWithMeta LEFT defaulted by a FieldWithMeta collapse (B.2), then defaulted again by a method-statement ite whose two arms are FieldWithMeta collapses assigned to the bare-typed ifThenElseResult (B.4). The three method-level wrapper locals are ONE numbering group.">
                inputs:
                    payout Seat33Payout (0..1)
                output:
                    picked Seat33Sched (0..1)
                set picked:
                    payout -> commodityPayout -> quantitySchedule
                        then default (payout -> tradeLot -> priceQuantity -> quantity
                                then filter uom exists
                                then only-element
                                )
                        then default if payout -> optionPayout is absent
                                then payout -> tradeLot -> priceQuantity -> quantity first
                                else payout -> tradeLot -> priceQuantity -> quantity last

            func E1HomogeneousMetaDefaultKeepsTheWrapper: <"e1 - B.2's DECLINE LOCK: the SAME shape with the SAME wrapper class on both sides (ReferenceWithMeta over ReferenceWithMeta). Upstream's joinMetaAnnotatedTypes keeps the annotation, golden keeps the wrapper, and the seat must return null exactly where it returns null today - the drr MessageID x9 / rule:QuantitySchedule / GetBasketConstituents:173 law.">
                inputs:
                    payout Seat33Payout (0..1)
                output:
                    picked Seat33Sched (0..1)
                set picked:
                    payout -> commodityPayout -> quantitySchedule
                        then default (payout -> tradeLot -> priceQuantity -> refQuantity
                                then filter uom exists
                                then only-element
                                )

            func E2MapperFormSlotArmsKeepTheirBytes: <"e2 - B.4's DECLINE LOCK, written from fn:Price's ladder shape: the SAME wrapper-item arms over the SAME bare decl, but at a MAPPER-FORM slot (the ite is a default LEFT operand, the #383 disjunct). The existing !mapperFormSlot conjunct must exclude it - this is the fixture that locks the ordering constraint that keeps Price whole.">
                inputs:
                    payout Seat33Payout (0..1)
                    fallback Seat33Sched (0..1)
                output:
                    picked Seat33Sched (0..1)
                set picked:
                    (if payout -> optionPayout is absent
                        then payout -> tradeLot -> priceQuantity -> quantity first
                        else payout -> tradeLot -> priceQuantity -> quantity last
                        ) default fallback
            """;

    // =========================================================================
    // Part A -- the two rungs and their shared naming group (RED at the parent head)
    // =========================================================================

    /**
     * a1 -- RUNG B.2. The heterogeneous-meta join derefs BOTH sides: the LEFT takes the
     * {@code coerceNavigationReceiver} "Type coercion" hop and the RIGHT materialises into a
     * wrapper local passed as the null-guarded deref, with the whole result re-wrapped
     * {@code MapperS.of(} by {@code selfUnwrapping}. The then-arg slot then declares the BARE
     * value type, because the deref'd left is exactly what FER's {@code blockArmDerefsToBareLeaf}
     * test (#391) reads.
     *
     * <p>Witness-uniqueness: {@code .getOrDefault((} (a doubled paren) is the guarded-argument form
     * the flip CREATES and the pre-law render cannot contain; {@code MapperS<FieldWithMetaSeat33Sched>
     * thenArg} is the wrapper-typed slot the flip REMOVES.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_heterogeneousMetaDefaultDerefsBothSidesAndJoinsBare() throws IOException {
        String code = collapse(codeOnly(func("A1HeteroJoinAndIteArmCollapse.java")));
        assertContains(code, ".getOrDefault((fieldWithMetaSeat33Sched0 == null ? null"
                + " : fieldWithMetaSeat33Sched0.getValue()))");
        assertContains(code, "final FieldWithMetaSeat33Sched fieldWithMetaSeat33Sched0 = ");
        assertContains(code, "MapperS<Seat33Sched> thenArg");
        assertNotContains(code, "MapperS<FieldWithMetaSeat33Sched> thenArg");
    }

    /**
     * a2 -- RUNG B.4, <b>BOTH ARM PIPELINES</b>. The seat-32 B.1 lesson is explicit here: this
     * golden shape is produced at TWO pipelines (the then-arm window at
     * {@code ControlFlowHandler:4159-4213} and its else-arm twin at {@code :4358-4402}) and the
     * carrier uses BOTH, so a suite that asserted one arm would score a half-heal as a heal. One
     * edit inside {@code renderArm} covers both -- that is READ from the two windows, and this
     * test is what proves it.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_bothIteArmsHoistTheWrapperLocalAndAssignTheGuardedDeref() throws IOException {
        String code = collapse(codeOnly(func("A1HeteroJoinAndIteArmCollapse.java")));
        assertContains(code, "final FieldWithMetaSeat33Sched fieldWithMetaSeat33Sched1 = ");
        assertContains(code, "ifThenElseResult = fieldWithMetaSeat33Sched1 == null ? null"
                + " : fieldWithMetaSeat33Sched1.getValue();");
        assertContains(code, "final FieldWithMetaSeat33Sched fieldWithMetaSeat33Sched2 = ");
        assertContains(code, "ifThenElseResult = fieldWithMetaSeat33Sched2 == null ? null"
                + " : fieldWithMetaSeat33Sched2.getValue();");
    }

    /**
     * a3 -- THE SHARED NAMING GROUP, the reason the two rungs are ONE commit. The three
     * method-level locals are one {@code StatementHoistSession} group in registration order:
     * B.2's right-hand local first ({@code 0}), then the then-arm ({@code 1}), then the else-arm
     * ({@code 2}). The negative is the discriminating half: a group of size 1 or 2 would mint the
     * BARE base name for its first member, which golden does not have at the method level (golden's
     * two BARE occurrences live in lambda sub-groups, a separate registry).
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a3_theThreeMethodLevelLocalsAreOneNumberedGroup() throws IOException {
        String code = collapse(codeOnly(func("A1HeteroJoinAndIteArmCollapse.java")));
        assertContains(code, "final FieldWithMetaSeat33Sched fieldWithMetaSeat33Sched0 = ");
        assertContains(code, "final FieldWithMetaSeat33Sched fieldWithMetaSeat33Sched1 = ");
        assertContains(code, "final FieldWithMetaSeat33Sched fieldWithMetaSeat33Sched2 = ");
        assertNotContains(code, "final FieldWithMetaSeat33Sched fieldWithMetaSeat33Sched =");
        assertNotContains(code, "final FieldWithMetaSeat33Sched fieldWithMetaSeat33Sched3 =");
    }

    /**
     * a4 -- LAW 77: the a1/a2 shapes through the REAL {@code IRGeneration.functionGenerator} seam.
     * {@code visitDefault} falls through to {@code super} and {@code IRControlFlowHandler}
     * overrides only {@code ifThenElseResultBaseName}, so both rungs are INHERITED; the probe
     * measured both anchors' carrier rows identical on the two routes and this re-proves it on the
     * seam rather than arguing it.
     */
    @Test
    @EnabledIf("builtinsAndIrProviderAvailable")
    void a4_bothRungsRenderIdenticallyOnTheIrRoute() throws IOException {
        String legacy = collapse(codeOnly(func("A1HeteroJoinAndIteArmCollapse.java")));
        String ir = collapse(codeOnly(
                lookup(fixtureOnIrRoute(), "functions/A1HeteroJoinAndIteArmCollapse.java")));
        assertEquals(legacy, ir, "route divergence on the fixture carrier");
    }

    // =========================================================================
    // Part B -- the two decline locks
    // =========================================================================

    /**
     * e1 -- B.2's DECLINE LOCK and the {@code m-lawB2-samewrap} lane's witness. Both operands
     * carry the SAME wrapper class, so upstream's {@code joinMetaAnnotatedTypes} keeps the
     * annotation, golden keeps the wrapper, and the seat must return {@code null} on exactly the
     * line it returns {@code null} on today. This is the drr {@code MessageID} x9 /
     * {@code rule:QuantitySchedule} 20-row / {@code GetBasketConstituents:173} law at fixture
     * scale.
     *
     * <p>PREMISE first (the seat-30 b4 pattern): a file-wide negative passes vacuously if the
     * fixture never reached the shape, so assert the {@code default} rendered and that the fixture
     * is meta at all before asserting the absence of the deref.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void e1_homogeneousMetaDefaultKeepsTheWrapperOnBothSides() throws IOException {
        String code = collapse(codeOnly(func("E1HomogeneousMetaDefaultKeepsTheWrapper.java")));
        assertContains(code, ".getOrDefault(");
        assertContains(code, "ReferenceWithMetaSeat33Sched");
        assertNotContains(code, ".getOrDefault((");
        assertNotContains(code, "final ReferenceWithMetaSeat33Sched");
    }

    /**
     * e2 -- B.4's DECLINE LOCK, written from {@code fn:Price}'s ladder shape. The arms are the
     * same wrapper-item collapses over the same bare decl class, but the slot is MAPPER-FORM (the
     * ite is a {@code default} LEFT operand -- the #383 {@code defaultOperandMapperIteSlot}
     * disjunct), so the arms re-present as {@code MapperS.of(<item>)} and the EXISTING
     * {@code !mapperFormSlot} conjunct must exclude them.
     *
     * <p>This is the fixture that locks the charter's ordering constraint: golden {@code Price.java}
     * carries ZERO {@code final ...WithMetaPriceSchedule} locals, and after seat-33 law C.1 gives
     * Price the Mapper-form slot, its 20 {@code [P33-ITEARM]} rows decline here for free. If a
     * later change made a Mapper-form slot take the sink hoist, this test is what catches it
     * BEFORE the Price cells move.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void e2_aMapperFormSlotArmTakesNoWrapperHoist() throws IOException {
        String code = collapse(codeOnly(func("E2MapperFormSlotArmsKeepTheirBytes.java")));
        assertContains(code, "ifThenElseResult");
        assertContains(code, ".getOrDefault(");
        assertNotContains(code, "final FieldWithMetaSeat33Sched");
    }

    // =========================================================================
    // Part C -- the corpus carrier: 4 whole-file heals, two cells locked here
    // =========================================================================

    /**
     * control0 -- golden is the oracle and it must DISCRIMINATE (prove the instrument can fail).
     * Golden's carrier carries the five method-level wrapper locals numbered {@code 0/1/2} plus the
     * two BARE lambda-scoped ones, the guarded {@code getOrDefault} argument and the bare-typed
     * then-arg slot; the three named GREEN witnesses exist and are inside the fork's emitted set.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control0_goldenDiscriminatesTheCarrierFromTheHomogeneousGreens() throws IOException {
        String g = collapse(codeOnly(Files.readString(GOLDEN_A.resolve(QUOM))));
        assertContains(g, "final FieldWithMetaNonNegativeQuantitySchedule"
                + " fieldWithMetaNonNegativeQuantitySchedule0 = MapperS.of(thenArg6.get()).get();");
        assertContains(g, ".getOrDefault((fieldWithMetaNonNegativeQuantitySchedule0 == null ? null"
                + " : fieldWithMetaNonNegativeQuantitySchedule0.getValue()))");
        assertContains(g, "final MapperS<NonNegativeQuantitySchedule> thenArg7 = MapperS.of(thenArg3.");
        assertContains(g, "ifThenElseResult = fieldWithMetaNonNegativeQuantitySchedule1 == null"
                + " ? null : fieldWithMetaNonNegativeQuantitySchedule1.getValue();");
        assertContains(g, "ifThenElseResult = fieldWithMetaNonNegativeQuantitySchedule2 == null"
                + " ? null : fieldWithMetaNonNegativeQuantitySchedule2.getValue();");
        assertEquals(5, count(g, "final FieldWithMeta"),
                "golden's carrier carries FIVE wrapper locals: B.2's 0, B.4's 1 and 2, and B.3's"
                + " two BARE lambda-scoped ones");

        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");
        for (String green : List.of(MESSAGE_ID, QUANTITY_SCHEDULE_RULE, GET_BASKET_CONSTITUENTS)) {
            assertTrue(Files.isRegularFile(GOLDEN_A.resolve(green)),
                    "the homogeneous-meta green oracle must exist (else this control is vacuous): "
                    + green);
            assertNotNull(drrAOutput.get(green), "the fork must emit the green witness " + green);
            String gw = collapse(codeOnly(Files.readString(GOLDEN_A.resolve(green))));
            assertFalse(gw.contains(".getOrDefault(("),
                    "a homogeneous-meta green must NOT carry the guarded-argument form: " + green);
        }
    }

    /** c1 -- the carrier goes BYTE-WHOLE on drr 7.0.0 (one of the band's last four files). */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_quantityUnitOfMeasureByteIdenticalOnDrr700() throws IOException {
        assertWhole(drrAOutput, drrAGenErrors, GOLDEN_A, "drr 7.0.0");
    }

    /** c2 -- the carrier goes BYTE-WHOLE on drr 7.1.0. 7.2.0 and 7.3.0 ride the whole matrix. */
    @Test
    @EnabledIf("cellBAvailable")
    void corpus_c2_quantityUnitOfMeasureByteIdenticalOnDrr710() throws IOException {
        assertWhole(drrBOutput, drrBGenErrors, GOLDEN_B, "drr 7.1.0");
    }

    private static void assertWhole(Map<String, String> out, List<String> genErrors, Path goldenRoot,
            String cell) throws IOException {
        assertNotNull(out, cell + " generation did not run - corpus unavailable?");
        List<String> lockedErrors = genErrors.stream().filter(e -> e.contains(QUOM)).toList();
        assertTrue(lockedErrors.isEmpty(),
                "the generator reported errors for the locked file " + QUOM + ": " + lockedErrors);
        String generated = out.get(QUOM);
        assertNotNull(generated, "not generated in " + cell + ": " + QUOM);
        Path goldenPath = goldenRoot.resolve(QUOM);
        assertTrue(Files.isRegularFile(goldenPath), "golden missing: " + goldenPath);
        assertEquals(normalize(Files.readString(goldenPath)), normalize(generated),
                "generated " + cell + " output must byte-match golden (newline-normalized) for "
                + QUOM + " - seat 33 law B.24: a heterogeneous-meta default joins bare and derefs"
                + " both sides, and a method-statement ite arm hoists its wrapper local on the"
                + " statement-sink channel.");
    }

    /**
     * control1 -- LAW 79, the whole-cell UNION scan on drr 7.0.0. Both rungs are reachable from
     * seats the two probes' censuses never enumerated (B.2's re-sited gate was probed at
     * {@code SetOperationHandler:447}, not at {@code :1538}; B.4's channel is reachable from every
     * sink-seat ite arm in the cell), so the scan is the instrument that can see an over-fire at an
     * unnamed site.
     *
     * <p><b>The columns</b> (U1, U2, U3, U4):
     * <ul>
     *   <li><b>U1</b> {@code final FieldWithMeta} -- the hoisted {@code FieldWithMeta} locals both
     *       rungs ADD. The carrier moves; nothing else may.</li>
     *   <li><b>U2</b> {@code final ReferenceWithMeta} -- the SIBLING wrapper family, which neither
     *       rung may create. This is the column an over-fire into the homogeneous
     *       {@code ReferenceWithMeta} population (rule:QuantitySchedule's 20 rows) shows up in.</li>
     *   <li><b>U3</b> {@code .getOrDefault((} -- the guarded-argument form B.2 adds. The doubled
     *       paren is what separates it from the plain {@code getOrDefault(} the whole cell uses.</li>
     *   <li><b>U4</b> {@code .getValue()} -- the deref census, the broadest column: every guarded
     *       deref either rung emits lands here, so an over-deref anywhere in the cell moves it.</li>
     * </ul>
     *
     * <p><b>{@code KNOWN_RESIDUE_DRR7} is EMPTY, by construction and not by hope.</b> This is the
     * charter's LAST law: at its head every other drr 7.0.0 band file is already whole
     * (GetBasketConstituents at row 11, Price at rows 15-16, StrikePrice at row 5,
     * TotalNotionalQuantity at row 12, Enrich at row 6), and a byte-identical file cannot differ on
     * any token. A non-empty print here therefore means an EARLIER law under-delivered -- report
     * that, never add a row.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control1_forkDrr7WholeCellMetaHoistShapesEqualGoldenFileByFile() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");
        assertEquals(List.of(), drrAGenErrors,
                "drr 7.0.0 reported a generation error - the scan is incomplete");
        assertTrue(drrAOutput.containsKey(QUOM) && drrAOutput.containsKey(MESSAGE_ID)
                        && drrAOutput.containsKey(QUANTITY_SCHEDULE_RULE)
                        && drrAOutput.containsKey(GET_BASKET_CONSTITUENTS),
                "the carrier and all THREE homogeneous-meta green witnesses must be INSIDE this"
                + " scan's domain, else control1 proves nothing about them (LAW: a control scans"
                + " the domain it claims)");
        assertUnionEqual(scan(drrAOutput), scan(readGoldenTree(GOLDEN_A)), drrAOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_DRR7, DOMAIN_DRR7);
    }

    /**
     * control2 -- LAW 77 route parity, per FILE. B.2 sits in {@code SetOperationHandler} and B.4 in
     * {@code ControlFlowHandler}, both reached from the IR-routed compiler by fall-through; this
     * pins the carrier and the three green witnesses byte-identical on the two routes.
     */
    @Test
    @EnabledIf("cellAAndIrProviderAvailable")
    void corpus_control2_irRouteMatchesLegacyForTheCarrierAndTheWitnesses() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 legacy-route generation did not run");
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", CELL_A_ROOT), new ArrayList<>());
        for (String path : List.of(QUOM, MESSAGE_ID, QUANTITY_SCHEDULE_RULE,
                GET_BASKET_CONSTITUENTS)) {
            assertEquals(drrAOutput.get(path), irOut.get(path), "route divergence: " + path);
        }
    }

    /** MEASURED EMPTY at the law's head -- see {@link #corpus_control1_forkDrr7WholeCellMetaHoistShapesEqualGoldenFileByFile}. */
    private static final List<String> KNOWN_RESIDUE_DRR7 = List.of();

    /**
     * The union domain, pinned (LAW 73) -- a negative value means an UNPINNED call site and
     * {@link #assertUnionEqual} fails loudly rather than passing vacuously.
     *
     * <p><b>DERIVED, to be re-pinned from the print.</b> 891 is the number of drr 7.0.0 GOLDEN
     * files under a {@code /functions/} or {@code /reports/} path carrying at least one of the four
     * tokens, measured by a read-only walk over
     * {@code test-corpus/drr/drr-7.0.0/rosetta-source/src/generated/java} at the seat-33 base head
     * (the walk mirrors this suite's own {@code codeOnly} + {@code collapse} + literal count). The
     * live union is that set UNIONED with the fork's token-bearing files and INTERSECTED with what
     * the fork emits, so transcribe the measured value from this assert's own
     * {@code MEASURED domain=} print.
     */
    private static final int DOMAIN_DRR7 = 891;

    private static Map<String, int[]> scan(Map<String, String> tree) {
        Map<String, int[]> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : tree.entrySet()) {
            String flat = collapse(codeOnly(e.getValue()));
            int u1 = count(flat, "final FieldWithMeta");
            int u2 = count(flat, "final ReferenceWithMeta");
            int u3 = count(flat, ".getOrDefault((");
            int u4 = count(flat, ".getValue()");
            if (u1 + u2 + u3 + u4 > 0) {
                out.put(e.getKey(), new int[] {u1, u2, u3, u4});
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

    // =========================================================================
    // The union assert (LAW 73: pin the SET, not the count) - the seat-29/30 shape
    // =========================================================================

    private static void assertUnionEqual(Map<String, int[]> a, Map<String, int[]> b,
            java.util.Set<String> emittedA, String aName, String bName, List<String> knownResidue,
            int expectedDomain) {

        List<String> mismatched = new ArrayList<>();
        java.util.Set<String> universe = new java.util.TreeSet<>(a.keySet());
        universe.addAll(b.keySet());
        universe.retainAll(emittedA);
        int[] zero = new int[4];
        for (String key : universe) {
            int[] ac = a.getOrDefault(key, zero);
            int[] bc = b.getOrDefault(key, zero);
            if (!java.util.Arrays.equals(ac, bc)) {
                mismatched.add(key + " " + aName + "=" + java.util.Arrays.toString(ac)
                        + " " + bName + "=" + java.util.Arrays.toString(bc));
            }
        }
        // The domain-pin flow (LAW 73): the sentinel failure PRINTS the measured values so
        // the pin is transcribed from this assert's own output, never invented.
        assertTrue(expectedDomain >= 0,
                "the union domain is MEASURED and pinned (LAW 73) - transcribe from this"
                        + " print: MEASURED domain=" + universe.size()
                        + " residue=" + mismatched);
        assertEquals(knownResidue, mismatched,
                "(U1, U2, U3, U4) differ beyond the named residue in " + mismatched.size()
                        + " file(s)");
        assertEquals(expectedDomain, universe.size(),
                "the union domain must equal the emitted token-bearing files (" + expectedDomain + ")");
    }

    // =========================================================================
    // Harness (the seat-30 DefaultRightNestedThenHoistSeatTest shape verbatim)
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
                    new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", CELL_A_ROOT), errs);
            drrAGenErrors = errs;
        }
        if (cellBAvailable()) {
            List<String> errs = new ArrayList<>();
            drrBOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "7.1.0", CELL_B_ROOT), errs);
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

    private static String normalize(String s) {
        return s.replace("\r\n", "\n");
    }

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

    private static void assertContains(String out, String needle) {
        assertTrue(out.contains(needle),
                "expected needle missing:\n" + needle + "\n--- in output:\n" + out);
    }

    private static void assertNotContains(String out, String token) {
        assertFalse(out.contains(token),
                "forbidden token present: " + token + "\n--- in output:\n" + out);
    }

    private static RLinkingResult linking;
    private static RModel mainModel;
    private static Map<String, String> fixtureOut;
    private static Map<String, String> fixtureIrOut;

    private static void link() throws IOException {
        if (linking == null) {
            RModel main = AstBuilder.buildFromString(MODEL, "seat33f10bf13.rosetta");
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

    private static Map<String, String> renderOnIrRoute(Predicate<RModel> filter) throws IOException {
        String previous = System.getProperty(IRGeneration.PROPERTY);
        System.setProperty(IRGeneration.PROPERTY, "true");
        try {
            assertNotNull(IRGeneration.providerOrNull(),
                    "the IR provider must be resolvable under -Pir-on, else this is not an ON-route render");
            link();
            GeneratorModel gm = new GeneratorModel(linking.workspace(), filter);
            JavaTypeUtil typeUtil = new JavaTypeUtil();
            JavaTypeTranslator tt = new JavaTypeTranslator(typeUtil);
            FunctionGenerator fg = IRGeneration.functionGenerator(gm, tt, typeUtil);
            assertTrue(!fg.getClass().equals(FunctionGenerator.class),
                    "the seam must hand back the IR-route FunctionGenerator, got " + fg.getClass());
            RuleGenerator ruleGen = new RuleGenerator(gm, tt, fg);
            Map<String, String> out = new LinkedHashMap<>();
            List<String> errors = new ArrayList<>();
            ruleGen.generateClasses(mainModel, "1.0", out)
                    .forEach(e -> errors.add(e.getTargetPath() + " - " + e));
            fg.generateWithErrors(out)
                    .forEach(e -> errors.add(e.getTargetPath() + " - " + e));
            if (!errors.isEmpty()) {
                throw new AssertionError("fixture generation errors on the IR route: " + errors);
            }
            return out;
        } finally {
            if (previous == null) {
                System.clearProperty(IRGeneration.PROPERTY);
            } else {
                System.setProperty(IRGeneration.PROPERTY, previous);
            }
        }
    }

    private static Map<String, String> fixture() throws IOException {
        if (fixtureOut == null) {
            fixtureOut = render(m -> "census.seat33f10bf13".equals(m.namespace()));
        }
        return fixtureOut;
    }

    private static Map<String, String> fixtureOnIrRoute() throws IOException {
        if (fixtureIrOut == null) {
            fixtureIrOut = renderOnIrRoute(m -> "census.seat33f10bf13".equals(m.namespace()));
        }
        return fixtureIrOut;
    }

    private static String func(String fileName) throws IOException {
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
                        failures.add(p + " - " + e);
                    }
                });
        if (!failures.isEmpty()) {
            throw new AssertionError("[QuomDefaultJoinIteArmWholeSeatTest] builtins parse"
                    + " failures: " + String.join("; ", failures));
        }
        return models;
    }
}
