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
 * SEAT 33 law E.1 -- facet {@code collapseCarryPrevMetaElement}: <b>the collapse level carries the
 * previous level's META element.</b>
 *
 * <p><b>GOLDEN &larr; FORK</b> (one line per carrier, the whole residue of three band files):
 * <pre>
 * golden  final MapperS&lt;FieldWithMetaPriceSchedule&gt; thenArg8 = MapperS.of(thenArg7.get());
 * fork    final MapperS&lt;PriceSchedule&gt;              thenArg8 = MapperS.of(thenArg7.get());
 * </pre>
 * The statement AFTER it is byte-identical in both and already derefs the wrapper
 * ({@code item.<PriceSchedule>map("Type coercion", fieldWithMetaPriceSchedule -> ...)}), so golden
 * is its own existence proof that a {@code MapperS<FieldWithMetaPriceSchedule>} receiver renders
 * exactly one coercion and that it is that line.
 *
 * <p><b>The mechanism, MEASURED (probe {@code [P33-CARRY]}, 9,381 rows, BOTH routes, the tag logs
 * byte-identical between them).</b> The carrier chain is
 * {@code filter IsInterestRatePayout then extract ContractForEvent then extract SingleTradeLot
 * then extract priceQuantity then filter observable -> rateOption is absent then extract price
 * then flatten then filter IsFixedInterestRate then only-element then extract value}
 * ({@code regulation-techsprint-g20-mas-rule.rosetta}:150-163). Its k-trace:
 * <ul>
 *   <li>k=6 {@code then flatten} -- the #341 FLATTEN-over-LoL disjunct fires, so the level keeps
 *       the wrapper AND sets the propagation flag {@code prevRefFilterRecovered};</li>
 *   <li>k=7 {@code then filter IsFixedInterestRate} -- the #144 refs re-key has ALREADY put the
 *       wrapper on {@code itemType}, so the #297 block's own inner gate
 *       ({@code !prevItem.equals(itemType)}) reads FALSE, declines, and CLEARS the flag
 *       ({@code anchorNow=true gate=false});</li>
 *   <li>k=8 {@code then only-element} -- the collapse's compiled value is type-less (the
 *       {@code .get()} top), the flag is gone, and all SIX existing anchor disjuncts decline
 *       ({@code anchorNow=false gate=true wouldFlip=true}). The decl re-derives the meta-blind
 *       bare element.</li>
 * </ul>
 * There is no k=9 row -- {@code then extract value} is the output assignment, a different seat --
 * so nothing downstream reads the flag, and {@code FunctionExpressionRenderer}'s then-arg decl
 * fold (the #297 anchor-disjunct chain) is the SOLE read of it.
 *
 * <p><b>THE FIX -- a SEVENTH anchor disjunct, and a LAW-69 consult, not a new mechanism.</b>
 * {@code CollectionHandler}'s deep-then decl seat (facet {@code inLambdaNestedThenRestructure}, PR #352 i1b)
 * already ships this arm at the DEEP-THEN decl seat, with the same two gates
 * ({@code isBareItemOnlyElementLocal} + {@code isMapperC(prevRef)}) and the same inner test; its
 * helper is documented there as mirroring {@code FunctionExpressionRenderer.isBareItemOnlyElement},
 * so the two halves already share the predicate by name. The disjunct is appended LAST, so the
 * five anchors above keep their short-circuit order.
 *
 * <p><b>Green-safety, MEASURED over the whole matrix on BOTH routes.</b>
 * {@code [P33-CARRY] wouldFlip=true} (= {@code !anchorNow && onlyElem && prevIsC && gate}, the
 * exact predicate this disjunct adds) is <b>3 rows of 9,381, three {@code where=}, ZERO green</b>:
 * {@code rule:FixedFloatRateLeg1}, {@code rule:FixedFloatRateLeg2}, {@code rule:InterestRatePrice},
 * one row each. The 180 rows over 16 GREEN files where a bare only-element over a MapperC is
 * ALREADY anchored (the #297/#320/#338/#341 family -- Initial/VariationMarginCollateralPortfolioCode,
 * CollateralPortfolioCode*, UniqueProductIdentifier, Existing_Upi/Existing_OtcIsin/ExtractUpi,
 * DTCC_Leg1/2CommodityInstrumentID, PriceCurrency, PriceOfZeroCouponSwaps, UPI) are decided by
 * those anchors BEFORE this disjunct is reached, which is exactly why the site is here and not at
 * the earlier FER:6714 that the seat-33 dossier first sketched: a block there fires on all 180.
 *
 * <p><b>CARRIERS (CLAIMED -- measured by the chain):</b> {@code FixedFloatRateLeg1Rule.java},
 * {@code FixedFloatRateLeg2Rule.java}, {@code InterestRatePriceRule.java} x drr 5.61.0 POJO,
 * WHOLE (sig B038 = the seat-32 B074, census family F23; 2 diff lines / 1 hunk each). Band
 * 30 -&gt; 27 on BOTH routes.
 *
 * <p><b>CLAIMED RED at the seat-33 base {@code fa49da010}</b> (both routes): {@code a1},
 * {@code corpus_c1}, {@code corpus_c2}, {@code corpus_c3}, {@code corpus_control1} (+
 * {@code corpus_control2} under {@code -Pir-on}). <b>CLAIMED GREEN at the law head:</b> the whole
 * suite. {@code a2}, {@code e1}, {@code e2} and {@code corpus_control0} are GREEN at BOTH states
 * by design -- {@code control0} is the golden-side oracle (prove the instrument can fail),
 * {@code a2} is the fixture-grade twin of the risk lock below, and {@code e1}/{@code e2} are the
 * decline locks the two sever lanes are measured against.
 *
 * <p><b>THE ONE NAMED RISK, and the test that decides it.</b> Retyping {@code thenArg8} retypes
 * {@code prevRef}, which IS the receiver at the trio's last then ({@code extract value}) -- an
 * {@code RExtractExpr}, the exact seat of seat-32 law A.2's item binder, whose conjunct
 * ({@code lawRecvItem instanceof RJavaWithMetaValue}) therefore fires at the trio for the first
 * time. EXPECTED GREEN: golden's own trio file carries the {@code "Type coercion"} line directly
 * under the {@code MapperS<FieldWithMetaPriceSchedule>} decl, and A.2's narrative is that a
 * wrapper item is precisely what makes {@code ExpressionCompiler.coerceNavigationReceiver} emit
 * that deref (the route the 1,801 measured {@code wouldBind=true} green rows already take). The
 * seat-31 revert does NOT say otherwise: {@code [P31-RECV]} read the trio as
 * {@code recvItemWrapper=false}, i.e. rung 1 typed the item BARE and thereby CLOSED the
 * {@code getExpressionType() == null} recovery -- the opposite direction. The deciding instrument
 * is NOT in this suite: it is {@code ReceiverRenderTypingSeatTest.corpus_cGUARD} (one
 * {@code "Type coercion"} per trio file, equal to golden's, and the line verbatim). <b>A RED there
 * is a MEASUREMENT: bank E.1, never weaken that suite.</b> {@code a2} below is its fixture-grade
 * twin so the same question is asked at the reduced shape too.
 *
 * <p><b>MUTATIONS (LAW 66/76) -- CLAIMED, to be MEASURED by the chain (LAW 82; the corpus-grade
 * readings are already measured by {@code [P33-CARRY]}).</b>
 * <ul>
 *   <li><b>m-lawE1-whole</b> (the disjunct severed -- {@code FER_PAIRS_MUT_M_LAWE1_WHOLE}):
 *       CLAIMED {@code a1}, {@code corpus_c1}, {@code corpus_c2}, {@code corpus_c3},
 *       {@code corpus_control1} (+ {@code corpus_control2} on {@code -Pir-on}) -- the RED itself.
 *       The whole-cell control MOVES under this lane, as LAW 76 requires.</li>
 *   <li><b>m-lawE1-onlyelem</b> ({@code isBareItemOnlyElement} widened to
 *       {@code isElementPreservingTailOp}, which the enclosing {@code if} already requires, so the
 *       disjunct degenerates to "any element-preserving op over a MapperC"): the CORPUS set is
 *       MEASURED <b>EMPTY</b> -- {@code CARRY preserving=true onlyElem=false prevIsC=true
 *       anchorNow=false gate=true} = <b>0 of 9,381</b>, so {@code corpus_control1} does NOT move
 *       and the lane is declared empty at corpus grade. Its witness is FIXTURE-level: {@code e1},
 *       whose collapse is {@code then first} over the same wrapper list, gains the wrapper element
 *       and FAILS.</li>
 *   <li><b>m-lawE1-mapperc</b> (the receiver-KIND guard dropped): MEASURED <b>EMPTY at BOTH
 *       grades</b> -- {@code CARRY onlyElem=true prevIsC=false} = <b>0 of 9,381</b>: every
 *       bare-item only-element level in the matrix has a MapperC receiver, so there is no corpus
 *       shape to move AND no faithful fixture to reduce from one. The conjunct ships as
 *       defence-in-depth with that zero written into its comment.</li>
 *   <li><b>m-lawE1-meta</b> is <b>NOT a lane of this law</b>: the
 *       {@code prevItem instanceof RJavaWithMetaValue} test lives in the #297 block's SHARED inner
 *       gate, which all six existing disjuncts use. Severing it was measured anyway and moves
 *       nothing beyond the carriers (365 rows read {@code prevMeta=false} and every one of them
 *       has {@code prevItem == itemNow}, so {@code !prevItem.equals(itemType)} still declines).
 *       {@code e2} is this suite's decline lock for the gate AT THIS DISJUNCT'S REACH: a bare
 *       only-element over a NON-meta MapperC now reaches the gate for the first time and must be
 *       left alone.</li>
 * </ul>
 *
 * <p><b>LAW 77.</b> INHERITS, and the inheritance is measured twice over.
 * {@code IRFunctionExpressionRenderer extends FunctionExpressionRenderer} and overrides only the
 * NAME methods ({@code ifThenElseResultBaseName}, {@code thenArgBaseName},
 * {@code booleanHoistBaseName}) plus {@code renderSetAssignmentStatement}; the decl-ELEMENT channel
 * is not re-implemented anywhere in {@code rune-ir-java}. The probe's four seat-33 tag logs are
 * byte-identical between the routes, and the three carrier files are byte-identical between the
 * OFF and ON band dumps at the base head. {@code corpus_control2} compares the IR route against
 * GOLDEN for all three carriers and route-to-route as well, so a shared-wrong render still reports
 * as a route fact.
 *
 * <p><b>LAW 74.</b> The fork's current text is NON-COMPILING at all three carriers:
 * {@code target/seat32-instruments/javac32/javac32-report.md} section 7.4 row <b>C18</b>,
 * <b>STANDING 3</b> -- {@code 689 inference variable T has incompatible bounds -- equality
 * constraints: PriceSchedule, lower bounds: FieldWithMetaPriceSchedule} plus {@code 691} x2
 * cascade. {@code MapperS<PriceSchedule> = MapperS.of(<MapperC<FieldWithMetaPriceSchedule>>.get())}
 * is a generics mismatch, which is also why no GREEN file can carry the pre-fix form. PRE 3 -&gt;
 * POST 0.
 *
 * <p><b>LAW-81 tripwires this heal WILL fire in ANOTHER suite (not edited here; re-pinned in this
 * law's commit from that suite's own print):</b>
 * {@code FilterPredicateMetaDerefSeatTest.corpus_c1b_masTrioResidueIsExactlyTheMetaDroppedDeclElement}
 * pins the trio at EXACTLY one fork-only and one golden-only line ({@code MapperS<PriceSchedule>} /
 * {@code MapperS<FieldWithMetaPriceSchedule>}); this law makes both ZERO, and that test's own
 * javadoc instructs the upgrade to a byte lock when the decl-element law lands.
 * {@code ReceiverRenderTypingSeatTest.corpus_cGUARD} / {@code corpus_control3} and
 * {@code FilterPredicateMetaDerefSeatTest.corpus_control3} / {@code KNOWN_RESIDUE_561} are expected
 * UNMOVED -- a RED at any of them is the REFUTER, not a re-pin.
 *
 * <p><b>LAW 82 - MEASURED by the receipts chain, run 1 at ce1a06292 (final33.status; every
 * figure below is transcribed from the chain's own logs, never from this file's earlier
 * CLAIMED paragraphs, which it supersedes).</b> GREEN 10/0F/1skip default (f33-green-default.log) /
 * 10/0F/0skip {@code -Pir-on} (f33-green-on.log); RED at the pre-seat base {@code fa49da010}: default
 * 5F = a1, corpus_c1, corpus_c2, corpus_c3, corpus_control1; {@code -Pir-on} 6F = a1, corpus_c1, corpus_c2, corpus_c3, corpus_control1, corpus_control2 (f33-red-{default,on}.log).
 * Mutation lanes ({@code mut33.py}, the default profile; {@code lanes33.py --summary}):
 * <ul>
 *   <li><b>{@code m-lawE1-whole}</b> ({@code FER_PAIRS_MUT_M_LAWE1_WHOLE}): MEASURED 10/5F/1skip = a1, corpus_c1, corpus_c2, corpus_c3, corpus_control1 - MATCH (a1, c1, c2, c3, control1 - the RED itself).</li>
 *   <li><b>{@code m-lawE1-onlyelem}</b> ({@code FER_PAIRS_MUT_M_LAWE1_ONLYELEM}): MEASURED 10/1F/1skip = e1 - MATCH - e1 alone; the corpus half EMPTY as declared (control1 held).</li>
 *   <li><b>{@code m-lawE1-mapperc}</b> ({@code FER_PAIRS_MUT_M_LAWE1_MAPPERC}): MEASURED 10/0F/1skip = (none) - EMPTY-as-declared at both grades; defence-in-depth, its zero in the source comment.</li>
 * </ul>
 */
class CollapseCarryPrevMetaElementSeatTest {

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

    // The three carriers (drr 5.61.0 POJO, the mas techsprint reports).
    private static final String FFR1 =
            "drr/regulation/techsprint/g20/mas/reports/FixedFloatRateLeg1Rule.java";
    private static final String FFR2 =
            "drr/regulation/techsprint/g20/mas/reports/FixedFloatRateLeg2Rule.java";
    private static final String IRP =
            "drr/regulation/techsprint/g20/mas/reports/InterestRatePriceRule.java";

    private static final List<String> MAS_TRIO = List.of(FFR1, FFR2, IRP);

    /** Cell A = drr 5.61.0 -- the only cell that carries this sig. */
    private static final Path CELL_A_ROOT = Path.of("../test-corpus/drr/drr-5.61.0");
    private static final Path GOLDEN_A = CELL_A_ROOT.resolve("rosetta-source/src/generated/java");

    /** The golden decl this law restores, transcribed from golden (line 73 of each trio file). */
    private static final String GOLDEN_DECL =
            "final MapperS<FieldWithMetaPriceSchedule> thenArg8 = MapperS.of(thenArg7.get());";

    /** The fork's band text -- the meta-dropped element (the whole residue of the three files). */
    private static final String FORK_DECL =
            "final MapperS<PriceSchedule> thenArg8 = MapperS.of(thenArg7.get());";

    /**
     * The statement that FOLLOWS the decl, byte-identical in fork and golden at the base head.
     * Transcribed from golden (line 75). This law must not move it -- it is the A.2-binder risk's
     * corpus-grade observable, and {@code ReceiverRenderTypingSeatTest.corpus_cGUARD} is its lock.
     */
    private static final String MAS_TRIO_COERCION_LINE =
            ".mapSingleToItem(item -> item.<PriceSchedule>map(\"Type coercion\","
            + " fieldWithMetaPriceSchedule -> fieldWithMetaPriceSchedule == null ? null :"
            + " fieldWithMetaPriceSchedule.getValue()).<BigDecimal>map(\"getValue\","
            + " priceSchedule -> priceSchedule.getValue())).get();";

    static boolean cellAAvailable() {
        return Files.isDirectory(GOLDEN_A);
    }

    static boolean cellAAndIrProviderAvailable() {
        return cellAAvailable() && irProviderOnClasspath();
    }

    // =========================================================================
    // Fixtures -- reduced from the carrier's REAL source
    // =========================================================================

    /**
     * a1 = {@code regulation-techsprint-g20-mas-rule.rosetta}:150-163
     * ({@code FixedFloatRateLeg1}) reduced to its mechanism, with EVERY step of the k-trace that
     * the probe measured preserved:
     * <ul>
     *   <li>a chain-head bare-fn {@code filter} (the {@code IsInterestRatePayout} seat);</li>
     *   <li>a MULTI {@code extract} and a nav-predicate {@code filter} over it (the
     *       {@code priceQuantity} / {@code observable -> rateOption is absent} seats);</li>
     *   <li>an {@code extract} of a {@code (0..*)} <b>{@code [metadata location]}</b> attribute
     *       over that MULTI receiver -- the {@code mapItemToList} that makes a
     *       {@code MapperListOfLists<FieldWithMetaSched>} (the CDM
     *       {@code PriceQuantity -> price PriceSchedule (0..*) [metadata location]} shape,
     *       verbatim);</li>
     *   <li>{@code then flatten} -- the #341 FLATTEN-over-LoL anchor that SETS the propagation
     *       flag;</li>
     *   <li>{@code then filter IsFixed}, whose callee takes the <b>BARE</b> value type
     *       {@code (1..1)} exactly as {@code IsFixedInterestRate(price PriceSchedule (1..1))} does
     *       -- this is what makes the filter block-hoist the wrapper, which is what puts the
     *       wrapper in the compiled refs, which is what makes the #144 re-key fire and the #297
     *       inner gate DECLINE and CLEAR the flag;</li>
     *   <li>{@code then only-element} -- the carrier level;</li>
     *   <li>{@code then extract value} over a type whose bare value type declares
     *       {@code value number (0..1)} -- the CDM {@code MeasureBase -> value} name, so the
     *       fixture reproduces the {@code FieldWithMetaX.getValue()} / {@code X.getValue()} name
     *       collision that is the named risk, not a sanitised twin of it.</li>
     * </ul>
     *
     * <p>e1 = the SAME chain with {@code then first} in place of {@code then only-element}: the
     * decline lock for the ONLY_ELEMENT conjunct and the witness of the
     * {@code m-lawE1-onlyelem} lane. e2 = the SAME chain over a list with NO metadata annotation:
     * the decline lock for the shared inner gate at this disjunct's new reach.
     */
    private static final String MODEL = """
            namespace census.seat33e1
            version "1.0.0"

            type Sched:
                value number (0..1)
                ccy string (0..1)

            type Lot:
                prices Sched (0..*)
                    [metadata location]
                plains Sched (0..*)
                rate string (0..1)

            type Instr:
                lots Lot (0..*)
                reportable boolean (0..1)

            func IsReportable: <"IsInterestRatePayout twin - the chain-head filter">
                inputs:
                    i Instr (0..1)
                output:
                    result boolean (1..1)
                set result:
                    i -> reportable = True

            func IsFixed: <"IsFixedInterestRate twin - the predicate callee takes the BARE value type (1..1), which is what makes the filter hoist and deref the wrapper">
                inputs:
                    p Sched (1..1)
                output:
                    result boolean (1..1)
                set result:
                    p -> ccy exists

            reporting rule A1CollapseCarry from Instr: <"a1 - THE mas FixedFloatRateLeg1 SHAPE: flatten (sets the flag) then a bare-fn filter over the wrapper list (the #144 re-key clears it) then a BARE only-element (all six anchors decline)">
                filter IsReportable
                then extract lots
                then filter rate is absent
                then extract prices
                then flatten
                then filter IsFixed
                then only-element
                then extract value
                as "a1"

            reporting rule E1FirstCollapse from Instr: <"e1 - THE DECLINE LOCK for the ONLY_ELEMENT conjunct: the same chain collapsing with `first`; the m-lawE1-onlyelem lane widens the conjunct and this fixture is its witness">
                filter IsReportable
                then extract lots
                then filter rate is absent
                then extract prices
                then flatten
                then filter IsFixed
                then first
                then extract value
                as "e1"

            reporting rule E2NonMetaCollapse from Instr: <"e2 - THE DECLINE LOCK for the shared inner gate at this disjunct's new reach: the same chain over a list with NO metadata annotation, so prevItem is not a wrapper and the gate must decline">
                filter IsReportable
                then extract lots
                then filter rate is absent
                then extract plains
                then flatten
                then filter IsFixed
                then only-element
                then extract value
                as "e2"
            """;

    // =========================================================================
    // Part A -- the law's fixture
    // =========================================================================

    /**
     * a1 -- THE FLIP. The bare {@code only-element} over the filtered wrapper list declares the
     * receiver's META element, and the collapse's initialiser is untouched (this law retypes a
     * decl; it must not restructure the statement).
     *
     * <p><b>PIN AT RED.</b> The RED must fail on the FIRST assert -- the law's own token. If it
     * fails instead because the fixture never reaches the seat (no {@code thenArg} decl at all, or
     * the wrapper missing from the FILTER level, which {@code e1}'s second assert also checks),
     * RESHAPE THE FIXTURE, do not weaken the assert: the likeliest reshape is to restore the two
     * chain steps this reduction drops (a rule-call and a function-call {@code extract} between
     * the head filter and the MULTI extract). The asserts are deliberately numbering-INDEPENDENT
     * ({@code "MapperS<FieldWithMetaSched> thenArg"}, not {@code thenArg6}) because thenArg
     * numbering is per-scope literal; the expected line at this reduction is
     * {@code final MapperS<FieldWithMetaSched> thenArg6 = MapperS.of(thenArg5.get());}.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_bareOnlyElementOverAFilteredWrapperListKeepsTheMetaElement() throws IOException {
        String out = fixtureRule("A1CollapseCarryRule");
        assertContains(out, "MapperS<FieldWithMetaSched> thenArg");
        assertTrue(!codeOnly(out).contains("MapperS<Sched> thenArg"),
                "a1 must not keep the meta-blind bare collapse element:\n" + out);
        assertContains(out, " = MapperS.of(thenArg");
        assertContains(out, "MapperC<FieldWithMetaSched> thenArg");
    }

    /**
     * a2 -- the fixture-grade twin of the named risk (GREEN at BOTH heads by design). Retyping the
     * collapse decl retypes the receiver of the last then's {@code extract value}, which is seat-32
     * law A.2's binder seat. Golden's own trio file proves the outcome at corpus grade; this asks
     * the same question of the reduced shape: EXACTLY ONE coercion, and it is the wrapper deref
     * that feeds the {@code value} nav -- not zero (the binder ate it, the seat-31 symptom) and not
     * two (recovery AND coercion both fired).
     *
     * <p>The coercion parameter is asserted by PREFIX so a scope-minted numeric suffix cannot make
     * this brittle; the count assert is what pins the shape.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_theRetypedCollapseKeepsExactlyOneTypeCoercion() throws IOException {
        String out = fixtureRule("A1CollapseCarryRule");
        assertEquals(1, count(out, "\"Type coercion\""),
                "the collapse must render EXACTLY ONE wrapper deref:\n" + out);
        assertContains(out, ">map(\"Type coercion\", fieldWithMetaSched");
        assertContains(out, ">map(\"getValue\", ");
    }

    // =========================================================================
    // Part B -- the decline locks (the sever lanes' fixture witnesses)
    // =========================================================================

    /**
     * e1 -- THE DECLINE LOCK for the ONLY_ELEMENT conjunct, and the {@code m-lawE1-onlyelem}
     * lane's witness. {@code then first} is element-preserving and its receiver is the same
     * {@code MapperC<FieldWithMetaSched>}, so the widened predicate would fire here; the shipped
     * one must not. The second assert keeps the lock non-vacuous: the FILTER level still carries
     * the wrapper, so this really is the wrapper-list shape and not a fixture that declined for an
     * unrelated reason.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void e1_firstCollapseOverTheSameWrapperListKeepsTodaysBytes() throws IOException {
        String out = fixtureRule("E1FirstCollapseRule");
        assertTrue(!out.contains("MapperS<FieldWithMetaSched>"),
                "e1 must NOT gain the wrapper element: the disjunct is ONLY_ELEMENT-scoped:\n" + out);
        assertContains(out, "MapperC<FieldWithMetaSched> thenArg");
    }

    /**
     * e2 -- THE DECLINE LOCK for the #297 block's shared inner gate at this disjunct's NEW reach.
     * A bare {@code only-element} over a non-meta {@code MapperC<Sched>} now passes the seventh
     * disjunct for the first time and must be stopped by the gate
     * ({@code prevItem instanceof RJavaWithMetaValue}). No wrapper may appear anywhere in the
     * file, and the collapse must keep its bare element and render no coercion.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void e2_bareOnlyElementOverANonMetaListKeepsTheBareElement() throws IOException {
        String out = fixtureRule("E2NonMetaCollapseRule");
        assertTrue(!out.contains("WithMeta"),
                "e2's list carries no metadata annotation: no wrapper may be declared:\n" + out);
        assertContains(out, "MapperS<Sched> thenArg");
        assertEquals(0, count(out, "\"Type coercion\""),
                "a non-meta collapse must render no wrapper deref:\n" + out);
    }

    // =========================================================================
    // control0 -- golden is the oracle (prove the instrument can fail)
    // =========================================================================

    /**
     * control0 -- every token this suite asserts at corpus grade is read back off GOLDEN first, so
     * a broken scan cannot pass quietly. Each of the three goldens carries the wrapper decl
     * verbatim, carries the fork's meta-dropped decl nowhere, and carries EXACTLY ONE
     * {@code "Type coercion"} -- the line that sits directly under the decl this law restores.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control0_goldenIsTheOracle() throws IOException {
        for (String p : MAS_TRIO) {
            String g = normalize(Files.readString(GOLDEN_A.resolve(p)));
            assertTrue(g.contains(GOLDEN_DECL),
                    "golden must carry the wrapper collapse decl verbatim in " + p);
            assertTrue(!g.contains(FORK_DECL),
                    "golden must NOT carry the meta-dropped decl in " + p);
            assertEquals(1, count(g, "\"Type coercion\""),
                    "golden must carry EXACTLY ONE coercion in " + p);
            assertTrue(g.contains(MAS_TRIO_COERCION_LINE),
                    "golden's one coercion must be the trio line in " + p);
        }
    }

    // =========================================================================
    // The carriers -- whole-file byte compares
    // =========================================================================

    /** corpus_c1 -- {@code FixedFloatRateLeg1Rule} whole (B038-only, 2 diff lines / 1 hunk). */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_drr561FixedFloatRateLeg1RuleMatchesGolden() throws IOException {
        assertCarrierWhole(FFR1);
    }

    /** corpus_c2 -- {@code FixedFloatRateLeg2Rule} whole; the same sig in a sibling rule. */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c2_drr561FixedFloatRateLeg2RuleMatchesGolden() throws IOException {
        assertCarrierWhole(FFR2);
    }

    /**
     * corpus_c3 -- {@code InterestRatePriceRule} whole. Its chain differs from the other two at
     * two steps ({@code rateOption exists} / {@code IsInterestRateSpread}), so it is an
     * independent render of the same mechanism rather than a copy.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c3_drr561InterestRatePriceRuleMatchesGolden() throws IOException {
        assertCarrierWhole(IRP);
    }

    /**
     * The carrier lock: the decl asserts run AHEAD of the byte compare so a decl regression
     * reports as a decl regression rather than as a whole-file diff, and the coercion line is
     * checked so the A.2-binder risk shows up HERE as well as in
     * {@code ReceiverRenderTypingSeatTest.corpus_cGUARD}.
     */
    private static void assertCarrierWhole(String path) throws IOException {
        assertNotNull(drrAOutput, "drr 5.61.0 generation did not run");
        List<String> own = drrAGenErrors.stream().filter(e -> e.contains(path)).toList();
        assertTrue(own.isEmpty(), "generation errors for " + path + ": " + own);
        String gen = drrAOutput.get(path);
        assertNotNull(gen, "not generated: " + path);
        gen = normalize(gen);
        assertTrue(gen.contains(GOLDEN_DECL),
                "law E.1: the collapse must declare the previous level's meta element in " + path);
        assertTrue(!gen.contains(FORK_DECL),
                "law E.1: the meta-dropped decl must be gone in " + path
                + " (it does not compile - javac32 section 7.4 row C18)");
        assertEquals(1, count(gen, "\"Type coercion\""),
                "the retype must neither lose nor buy back a coercion in " + path);
        assertTrue(gen.contains(MAS_TRIO_COERCION_LINE),
                "the one coercion must stay the trio line verbatim in " + path);
        assertEquals(normalize(Files.readString(GOLDEN_A.resolve(path))), gen,
                path + " must byte-match golden at drr 5.61.0 - law E.1 closes its only sig"
                + " (B038)");
    }

    // =========================================================================
    // The whole-cell UNION control (LAW 79) + the IR route (LAW 77)
    // =========================================================================

    /**
     * control1 -- LAW 79, the whole-cell UNION scan on drr 5.61.0. Domain = every emitted file
     * whose GOLDEN <b>or</b> FORK text declares a {@code final Mapper}-typed local; per file the
     * (wrapper-element decls, total Mapper decls) PAIR must equal golden's beyond the NAMED
     * residue. T1 is the law's own observable -- it moves +1 at each carrier and must move nowhere
     * else; T2 is the over-fire net, deliberately global, so a decl added in one place cannot
     * cancel a decl lost in another.
     *
     * <p>This control fails when the law OVER-fires (a green file's decl is retyped: it is a
     * UNION, so a file entering from either side is compared) and when it UNDER-fires (a carrier's
     * row stays in the residue).
     *
     * <p>The GOLDEN-side domain is pinned exactly and is derivable WITHOUT the generator: a
     * read-only walk of {@code test-corpus/drr/drr-5.61.0/rosetta-source/src/generated/java}
     * (5,249 goldens) finds 1,258 carrying a {@code final Mapper} local
     * ({@code target/seat33-instruments/drafts33/E1/derive-domain.py}). The UNION domain also
     * depends on which of those the fork emits, so it carries the print-first SENTINEL until the
     * chain prints it.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control1_forkDrr561WholeCellDeclElementShapeEqualsGolden() throws IOException {
        assertNotNull(drrAOutput, "drr 5.61.0 generation did not run");
        assertEquals(List.of(), drrAGenErrors,
                "drr 5.61.0 reported a generation error - the scan is incomplete");
        Map<String, int[]> golden = scan(readGoldenTree(GOLDEN_A));
        assertEquals(GOLDEN_DOMAIN_DRR561, golden.size(),
                "the GOLDEN tree this control scans must be the frozen one (read-only walk:"
                + " 1258 of 5249 goldens declare a `final Mapper` local)");
        assertUnionEqual(scan(drrAOutput), golden, drrAOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_DRR561, UNION_DOMAIN_DRR561);
    }

    /**
     * control2 -- LAW 77, BOTH-ROUTES-vs-GOLDEN. The seat is
     * {@code FunctionExpressionRenderer.renderThenExtractSetImpl}, which the IR route's renderer
     * INHERITS (it overrides only name methods and the set-assignment statement), so the ON route
     * must reach the same bytes. All three carriers are compared against GOLDEN, and additionally
     * route-to-route so a shared-wrong render still reports as a route fact.
     */
    @Test
    @EnabledIf("cellAAndIrProviderAvailable")
    void corpus_control2_irRouteMatchesGoldenForTheTrio() throws IOException {
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "5.61.0", CELL_A_ROOT),
                new ArrayList<>());
        for (String p : MAS_TRIO) {
            assertEquals(normalize(Files.readString(GOLDEN_A.resolve(p))),
                    normalize(irOut.get(p)), "IR route vs GOLDEN: " + p);
            assertEquals(drrAOutput == null ? null : normalize(drrAOutput.get(p)),
                    normalize(irOut.get(p)), "route divergence on " + p);
        }
    }

    // =========================================================================
    // Pins
    // =========================================================================

    /**
     * DERIVED by a read-only walk of the FROZEN golden tree at the seat-33 base -- the count of
     * drr 5.61.0 goldens declaring a {@code final Mapper} local (1,258 of 5,249). Not a generator
     * measurement: it locks the corpus side of the control, so a corpus drift or a silently-empty
     * golden read fails loudly instead of making the union vacuous.
     */
    private static final int GOLDEN_DOMAIN_DRR561 = 1258;

    ///PIN: SENTINEL (-1) -- the print-first domain-pin discipline. The UNION domain is
    ///PIN: (golden-scan UNION fork-scan) INTERSECT emitted, so it cannot be derived without
    ///PIN: running the generator. The sentinel assert FAILS and PRINTS "MEASURED domain=N
    ///PIN: residue=[...]"; the lead transcribes N here, in the same commit, from that print.
    private static final int UNION_DOMAIN_DRR561 = 1159;

    /**
     * DERIVED at the seat-33 base from the OFF-route band dump: every non-band file in the cell is
     * byte-identical to golden and therefore cannot contribute a row, so the residue is exactly
     * the band files whose PAIR differs. Of the cell's SIX band files only TWO shapes differ on
     * this pair -- the three carriers (each {@code fork=[3, 9] golden=[4, 9]}, the one dropped
     * wrapper decl) and {@code NotionalLeg2Rule}, which is law F.B's charter. This list is the
     * POST-law expectation: the three carrier rows LEAVE it and the F.B row stays.
     * {@code NotionalCurrencyLeg1Rule} and {@code NotionalCurrencyOfLeg1Rule} are band files whose
     * pair already EQUALS golden's, so they never enter.
     *
     * <p>Sorted as {@code assertUnionEqual} prints them (a {@code TreeSet} universe). TRANSCRIBE
     * from the control's own print at the first chain run if it disagrees -- a disagreement is a
     * MEASUREMENT, not a licence to weaken the list.
     */
    // LAW 81 re-pin (v3.1 flip seat 33, law F.B navTailExtractLadderNestedTreeAdmit): the jfsa NotionalLeg2Rule
    // row LEFT this list - the file is WHOLE in drr 5.61.0 at the F.B head; transcribed from this control's own
    // failing print (FB-trip1.log: "differ beyond the named residue" expected [NotionalLeg2Rule row] but was []).
    private static final List<String> KNOWN_RESIDUE_DRR561 = List.of();

    // =========================================================================
    // The scan + the union assert (LAW 73: pin the SET, not the count)
    // =========================================================================

    /**
     * (T1, T2) per file: the number of {@code final Mapper*<...WithMeta...>} local declarations
     * (the law's own observable -- a carrier moves +1) and the file's TOTAL {@code final Mapper}
     * declaration count (the over-fire net). Domain gate: any {@code final Mapper} decl at all, so
     * a file that LOSES a wrapper decl stays in the universe and reports rather than silently
     * leaving the scan.
     */
    private static Map<String, int[]> scan(Map<String, String> tree) {
        Map<String, int[]> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : tree.entrySet()) {
            String code = normalize(e.getValue());
            int t1 = 0;
            for (String kind : List.of("MapperS<", "MapperC<", "MapperListOfLists<")) {
                t1 += count(code, "final " + kind + "FieldWithMeta")
                        + count(code, "final " + kind + "ReferenceWithMeta");
            }
            int t2 = count(code, "final Mapper");
            if (t1 + t2 > 0) {
                out.put(e.getKey(), new int[] {t1, t2});
            }
        }
        return out;
    }

    private static void assertUnionEqual(Map<String, int[]> a, Map<String, int[]> b,
            java.util.Set<String> emittedA, String aName, String bName, List<String> knownResidue,
            int expectedDomain) {
        List<String> mismatched = new ArrayList<>();
        java.util.Set<String> universe = new java.util.TreeSet<>(a.keySet());
        universe.addAll(b.keySet());
        universe.retainAll(emittedA);
        int[] zero = new int[2];
        for (String key : universe) {
            int[] ac = a.getOrDefault(key, zero);
            int[] bc = b.getOrDefault(key, zero);
            if (!java.util.Arrays.equals(ac, bc)) {
                mismatched.add(key + " " + aName + "=" + java.util.Arrays.toString(ac)
                        + " " + bName + "=" + java.util.Arrays.toString(bc));
            }
        }
        // The domain-pin flow (LAW 73): the sentinel failure PRINTS the measured values so the pin
        // is transcribed from this assert's own output, never invented.
        assertTrue(expectedDomain >= 0,
                "the union domain is MEASURED and pinned (LAW 73) - transcribe from this"
                        + " print: MEASURED domain=" + universe.size()
                        + " residue=" + mismatched);
        assertEquals(knownResidue, mismatched,
                "the decl-element pair differs beyond the named residue in " + mismatched.size()
                        + " file(s)");
        assertEquals(expectedDomain, universe.size(),
                "the union domain must equal the emitted token-bearing files (" + expectedDomain
                        + ")");
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

    private static void assertContains(String out, String token) {
        assertTrue(out.contains(token), "expected token missing:\n  " + token + "\nin:\n" + out);
    }

    /** String literals and comments stripped, so a token inside a doc string cannot lie. */
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

    // =========================================================================
    // Fixture harness (the RuleThenArmLadderNestedTreeAdmitSeatTest rule renderer, verbatim)
    // =========================================================================

    private record Render(Map<String, String> output, List<String> errors) {}

    private static Render rendered;

    private static Render render() throws IOException {
        if (rendered != null) {
            return rendered;
        }
        RModel main = AstBuilder.buildFromString(MODEL, "seat33e1.rosetta");
        main.setVersion("0.0.0.test");
        List<RModel> models = new ArrayList<>();
        models.add(main);
        models.addAll(loadBuiltinsOnly());
        RWorkspace workspace = RWorkspace.build(models).workspace();
        GeneratorModel gm = new GeneratorModel(workspace,
                m -> "census.seat33e1".equals(m.namespace()));
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

    private static String fixtureRule(String ruleName) throws IOException {
        Render r = render();
        String path = ruleName + ".java";
        List<String> own = r.errors().stream().filter(e -> e.contains(path)).toList();
        assertTrue(own.isEmpty(), "the generator reported errors for " + path + ": " + own);
        String out = r.output().entrySet().stream()
                .filter(e -> e.getKey().endsWith(path))
                .map(Map.Entry::getValue)
                .findFirst().orElse(null);
        assertNotNull(out, "not generated: " + path + " (have: " + r.output().keySet() + ")");
        return out;
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
            throw new AssertionError("[CollapseCarryPrevMetaElementSeatTest] builtins parse"
                    + " failures: " + String.join("; ", failures));
        }
        return models;
    }

    // =========================================================================
    // Corpus harness (the ReceiverRenderTypingSeatTest cell generator, verbatim)
    // =========================================================================

    private static Map<String, String> drrAOutput;
    private static List<String> drrAGenErrors;

    @BeforeAll
    static void generateCells() throws IOException {
        if (cellAAvailable()) {
            List<String> errs = new ArrayList<>();
            drrAOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "5.61.0", CELL_A_ROOT), errs);
            drrAGenErrors = errs;
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
                    "the IR provider must be resolvable under -Pir-on");
            var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
            var gm = new GeneratorModel(corpus.workspace(),
                    D11CorpusRegressionTest.emissionFilter(cell));
            var typeUtil = new JavaTypeUtil();
            var typeTranslator = new JavaTypeTranslator(typeUtil);
            FunctionGenerator funcGen = IRGeneration.functionGenerator(gm, typeTranslator, typeUtil);
            assertTrue(!funcGen.getClass().equals(FunctionGenerator.class),
                    "the seam must hand back the IR-route FunctionGenerator");
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

}
