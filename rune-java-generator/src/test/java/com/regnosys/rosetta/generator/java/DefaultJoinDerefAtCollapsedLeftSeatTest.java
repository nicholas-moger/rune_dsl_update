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
 * SEAT 32, law D1 -- facet {@code defaultJoinDerefAtCollapsedLeft} (census family F10): at the
 * {@code default} seat the question "does the RIGHT operand render a meta wrapper" is answered
 * by the shared AST meta WALKER ({@code NavigationHandler.recoverExprMetaWrapper}) rather than
 * by the refs superset, with the COMPILED stamp kept AHEAD of it. A meta LEFT over a
 * genuinely meta-free RIGHT then derefs IN PLACE, as upstream's
 * {@code joinMetaAnnotatedTypes} requires.
 *
 * <p><b>GOLDEN &larr; FORK</b> (drr 7.0.0 {@code UnderlyingIndexIndicatorRule}, hunk 1):
 * <pre>
 * GOLDEN: .first().&lt;String&gt;map("Type coercion", fieldWithMetaString -&gt; fieldWithMetaString == null ? null : fieldWithMetaString.getValue()).getOrDefault(ifThenElseResult));
 * FORK:   .first().getOrDefault(ifThenElseResult));
 * </pre>
 *
 * <p><b>ONE RUNG.</b> The dossier proposed two; the LAW-75 round
 * ({@code verdicts32-D.md} SS D.1, 1,216 {@code [P32-DEF]} rows, BOTH routes) <b>REFUTED</b>
 * rung (a): the carrier measures {@code leftType=MapperS<FieldWithMetaString>
 * leftItemIsMeta=true} with the counterfactual {@code leftTermMetaCF} agreeing, so
 * {@code leftRendersMetaWrapper} already PASSES and no collapsed-left recovery exists to
 * write. The SOLE blocker was {@code rightRendersNoMetaWrapper}'s refs scan:
 * {@code rightRefsHasMeta=true} (the conditional right's arm text names a
 * {@code FieldWithMetaFloatingRateIndexEnum} its own arms deref elementwise) against
 * {@code rightAstMetaCF=null} (the walker sees no genuine meta right). The probe's
 * {@code rightAstMetaCF} field IS this law's expression, so the measurement is a direct
 * counterfactual of the change and not an analogue of it.
 *
 * <p><b>CARRIERS.</b> {@code UnderlyingIndexIndicatorRule} x drr 7.0/7.1/7.2/7.3 POJO --
 * <b>4 files WHOLE</b>. Its sig set is exactly {B009, B018, B039} and no import sig, and all
 * three heal from this one rung: B018 is the deref itself; B039 (the
 * {@code final MapperS<String> thenArg2} decl) follows through
 * {@code FunctionExpressionRenderer}'s {@code blockArmDerefsToBareLeaf}, whose
 * {@code .getValue()).getOrDefault(} clause is literally B018's post-fix text; B009 (the
 * downstream {@code mapSingleToItem} over-deref) follows because the ARG seat reads the
 * piped element's own stamp -- measured {@code [P32-ARG] callee=GetIndexIndicatorFromFloatingRate
 * argKind=RImplicitVariable compiledType=MapperS<...FieldWithMetaString> actualIsMeta=true
 * lambdaRecovery=false}, i.e. channel 1, so {@code NavigationHandler.implicitItemArgMeta}
 * (channel 4) never runs and re-stamping the element bare is both necessary and sufficient.
 * <b>The dossier's claimed contribution to {@code NotionalCurrencyLeg1Rule} (drr 5.61.0) is
 * REFUTED and NOT claimed here</b> -- {@code [P32-DEF]} has ZERO rows for that rule on both
 * routes; it contains no {@code default}.
 *
 * <p><b>GREEN BLAST RADIUS, measured over all 1,216 rows, route-identical.</b> With the
 * compiled-stamp check kept ahead of the walker: the would-fire set is <b>8 rows / 2
 * {@code where=} values</b> -- {@code rule:UnderlyingIndexIndicator} 4 and
 * {@code rule:IndicatorOfTheUnderlyingIndex} 4, both this seat's own carriers -- and the
 * regression quadrant (fires today, would DECLINE) is <b>ZERO rows</b>. Distinct non-carrier
 * {@code where=} values that move: ZERO. The green meta-RIGHT population keeps declining, now
 * on the right channel: {@code fn:MessageID} 9 (the 9 GREEN cells the seat-28 javadoc
 * protects) and {@code fn:GetBasketConstituents} 4 recover {@code FieldWithMetaString};
 * {@code rule:QuantitySchedule} 20 and {@code fn:QuantityUnitOfMeasure} 40 recover their own
 * wrappers; {@code rule:CollateralPortfolioIndicator} 9 sit on the #296 branch
 * ({@code metaJoin296=true}) and never reach this gate; {@code rule:FixingDate} 4 fired
 * before and fire after.
 *
 * <p><b>THE e1 LOCK IS AN IN-CORPUS WITNESS, not a fixture invention.</b>
 * {@code rightItemIsMeta=true} with {@code rightAstMetaCF=null} is measured at exactly ONE
 * place corpus-wide -- {@code fn:Price} (4 rows,
 * {@code rightType=MapperC<FieldWithMetaPriceSchedule>}, {@code rightKind=RFilterExpr}: a
 * {@code filter} right has no walker arm). A walker-ONLY predicate would coerce Price's LEFT
 * and move a six-sig band file this law does not own. e1 reduces that shape from its real
 * source and locks the decline.
 *
 * <p><b>LAW 74 -- MEASURED, not analytic.</b> {@code javac32/pre-javac.txt}, section C16
 * ({@code Seat32Pre.java:870}, the carrier's lines 84-110 verbatim):
 * <pre>
 * Seat32Pre.java:870: error: incompatible types: String cannot be converted to FieldWithMetaString
 *                 .first().getOrDefault(ifThenElseResult)));
 * </pre>
 * 1 error. {@code Mapper.getOrDefault(T)} takes the receiver's own item type, and the fork
 * hands a {@code String} to a {@code MapperS<FieldWithMetaString>}. The heal is a
 * NON_COMPILING &rarr; compiling flip; POST is owed at the seat head.
 *
 * <p><b>LAW 77 -- INHERITS, no IR twin.</b> {@code IRExpressionCompiler:3440-3442}
 * {@code visitDefault} is {@code tryEmitFromIR(...).orElseGet(() -> super.visitDefault(...))}
 * and the oracle-serve leg at {@code :13918-13921} routes to the same {@code super}; both land
 * on this handler. The one near-twin, {@code elementTerminalMeta}'s {@code RDefaultExpr} arm
 * at {@code :12789-12796}, answers a different question (the element's terminal meta face) and
 * this law neither reads nor changes it. The probe confirms: {@code [P32-DEF]} is 1,216 rows
 * with identical verdict fields on both routes. {@code corpus_control2} measures it anyway.
 *
 * <p><b>RED / GREEN -- CLAIMED, measured by the chain.</b> RED at the seat's base (both
 * routes): {@code a1}, {@code corpus_c1}, {@code corpus_c2}, {@code corpus_control1} (+
 * {@code corpus_control2} under {@code -Pir-on}); {@code e1} and {@code corpus_control0} GREEN
 * at RED. GREEN at the law's head: 7/0F/1skip default, 7/0F/0skip {@code -Pir-on}.
 *
 * <p><b>MUTATION LANES (LAW 66/76) -- MEASURED (LAW 82) by the seat-32 chain, run 1 at
 * {@code d99ded920} ({@code f32-mut-m-lawD1-*.log}):</b>
 * <ul>
 *   <li><b>m-lawD1-walker</b> (the rung reverted to the refs scan): MEASURED <b>7/4F/1S</b> =
 *       {@code a1}, {@code corpus_c1}, {@code corpus_c2}, {@code corpus_control1} -- the claim
 *       exactly; {@code e1}, {@code corpus_control0} GREEN. The law's own conjunct.</li>
 *   <li><b>m-lawD1-stamp</b> (the compiled-stamp early return dropped, the walker deciding
 *       alone): MEASURED <b>7/2F/1S</b> = {@code e1}, {@code corpus_control1} -- the claim
 *       exactly: Price's LEFT gains the hop and its row moves; {@code a1}/{@code corpus_c1}
 *       GREEN. The ORDER is load-bearing, MEASURED.</li>
 *   <li><b>m-lawD1-thread</b> ({@code null} passed for {@code rightExpr} at the call site):
 *       MEASURED <b>7/1F/1S = {@code corpus_control1}</b> only; {@code e1} GREEN.
 *       <b>Re-scored:</b> the claim "{@code e1} must FAIL" was wrong by the lane's own
 *       construction -- the compiled-stamp early return is KEPT FIRST in this lane, and Price's
 *       right carries the stamp ({@code rightItemIsMeta=true}), so {@code e1} is answered before
 *       the null-threaded walker is asked. The lane collapses into a left-only gate ONLY for the
 *       stamp-less population, which is exactly what {@code corpus_control1} measured moving
 *       (the homogeneous-meta rows). The threading is load-bearing at the corpus, witnessed by
 *       the whole-cell control alone.</li>
 * </ul>
 * RED at the chain's base {@code ddcdd151b}: {@code a1}, {@code corpus_c1}, {@code corpus_c2},
 * {@code corpus_control1} (+ {@code corpus_control2} on {@code -Pir-on}); GREEN at the head
 * 7/0F/1skip default, 7/0F/0skip {@code -Pir-on}.
 *
 * <p><b>LAW 80 accounting, CLAIMED.</b> 4 WHOLE ({@code UnderlyingIndexIndicatorRule} x drr
 * 7.0-7.3) + 4 IMPROVED ({@code IndicatorOfTheUnderlyingIndexRule} x drr 7.0-7.3: golden derefs
 * that left too, so this rung lands one of its hunks; the file goes whole only with seat 32's
 * law D2, which owns the multi-default ternary). Band 88 &rarr; 84 on BOTH routes. Zero
 * entered.
 */
class DefaultJoinDerefAtCollapsedLeftSeatTest {

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

    /** The carrier -- 4 cells, sig set {B009, B018, B039}, no import sig, so it goes WHOLE. */
    private static final String CARRIER =
            "drr/regulation/jfsa/rewrite/trade/reports/UnderlyingIndexIndicatorRule.java";

    /** The homogeneous-meta green: both operands meta, so golden keeps the wrapper. */
    private static final String MESSAGE_ID =
            "drr/regulation/common/trade/link/functions/MessageID.java";

    /** Cell A = drr 7.0.0 -- the carrier + the whole-cell UNION control. */
    private static final Path CELL_A_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path GOLDEN_A = CELL_A_ROOT.resolve("rosetta-source/src/generated/java");
    /** Cell B = drr 7.2.0 -- the second cell of the heal (the delta is byte-identical to A's). */
    private static final Path CELL_B_ROOT = Path.of("../test-corpus/drr/drr-7.2.0");
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

    // =========================================================================
    // Fixtures -- reduced from the carriers' REAL .rosetta sources
    // =========================================================================

    /**
     * a1 is reduced from {@code test-corpus/drr/drr-7.0.0/rosetta-source/src/main/rosetta/
     * regulation-jfsa-rewrite-trade-rule.rosetta:300-316}, the {@code UnderlyingIndexIndicator}
     * reporting rule: a {@code default} whose LEFT is a meta-annotated {@code (0..*)} string
     * navigated then collapsed by {@code first}, and whose RIGHT is a two-armed conditional
     * with no final else, each arm ending {@code ... first to-string} over a meta-annotated
     * ENUM -- the arm text that puts an INCIDENTAL {@code FieldWithMeta<enum>} in the right's
     * refs while the walker joins bare. (Real: {@code UnderlierProductIdentifier(item, Name)
     * -> identifier first default if IsFRA then ... -> floatingRateIndex first to-string else
     * if Qualify_BaseProduct_IRSwap(...) then ... first to-string}. The reduction drops the
     * rule-call roots and the enclosing filter/extract stages; the operand SHAPES,
     * cardinalities and metadata annotations are preserved.)
     *
     * <p>e1 is reduced from {@code standards-iosco-cde-version1-price-func.rosetta:99-102},
     * the {@code Price} function's own {@code default}: a {@code [metadata reference]} multi
     * LEFT over a {@code [metadata location]} multi RIGHT that is FILTERED. That filter is the
     * whole point -- it blinds the AST walker ({@code rightAstMetaCF=null}) while the compiled
     * stamp still says meta ({@code rightItemIsMeta=true}), the only such shape in the corpus.
     */
    private static final String MODEL = """
            namespace census.seat32d1
            version "1.0.0"

            enum RateIndexEnumD1:
                IDX_A
                IDX_B

            enum PriceKindEnumD1:
                ASSET_PRICE
                CASH_PRICE

            type IdentifierD1:
                identifier string (0..*)
                    [metadata scheme]

            type RateLeafD1:
                rateIndex RateIndexEnumD1 (0..1)
                    [metadata scheme]

            type EconomicD1:
                leaf RateLeafD1 (0..*)
                altLeaf RateLeafD1 (0..*)

            type ScheduleD1:
                amount number (0..1)
                priceKind PriceKindEnumD1 (0..1)

            type QuantityD1:
                priceSchedule ScheduleD1 (0..*)
                    [metadata reference]
                price ScheduleD1 (0..*)
                    [metadata location]

            type RootD1:
                ids IdentifierD1 (0..1)
                economic EconomicD1 (0..1)
                isFra boolean (0..1)
                isSwap boolean (0..1)
                quantity QuantityD1 (0..1)

            reporting rule A1CollapsedMetaLeft from RootD1: <"a1 - a meta LEFT collapsed by first, over a conditional RIGHT whose arms carry an incidental interior wrapper">
                extract
                    ids -> identifier first
                        default if isFra
                            then economic -> leaf -> rateIndex first to-string
                            else if isSwap
                            then economic -> altLeaf -> rateIndex first to-string

            reporting rule E1FilteredMetaRight from RootD1: <"e1 - the fn:Price witness: a genuinely meta RIGHT the AST walker cannot see through a filter; the compiled stamp must decline it">
                extract
                    quantity -> priceSchedule
                        default (quantity -> price
                            filter item -> priceKind = PriceKindEnumD1 -> ASSET_PRICE)
            """;

    /**
     * a1 -- the meta LEFT derefs IN PLACE over the conditional right. Two golden tokens and one
     * fork token, all read from the real golden
     * ({@code .../drr-7.0.0/.../UnderlyingIndexIndicatorRule.java:101}): the typed
     * {@code "Type coercion"} hop, the {@code .getValue()).getOrDefault(} adjacency (the same
     * token {@code FunctionExpressionRenderer}'s {@code blockArmDerefsToBareLeaf} reads, LAW
     * 69), and the absence of the un-deref'd {@code .first().getOrDefault(} form.
     *
     * <p>PIN AT RED: this fixture must reproduce the RED at the base head. If it does not
     * (most likely because the reduced left stamps differently from the rule-call-rooted real
     * one), RESHAPE THE FIXTURE -- add the rule-call root back -- do not weaken the assert.
     * The lambda parameter name is deliberately NOT asserted: it is scope-minted and the
     * carrier's own number depends on its file, so the asserts key on structure.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_metaLeftCollapsedByFirstDerefsOverAConditionalRight() throws IOException {
        String out = fixtureRule("A1CollapsedMetaLeftRule");
        assertContains(out, ">map(\"Type coercion\", ");
        assertContains(out, ".getValue()).getOrDefault(");
        assertTrue(!codeOnly(out).contains(".first().getOrDefault("),
                "the collapsed meta left must not reach getOrDefault still wrapped:\n" + out);
    }

    /**
     * e1 -- SUPERSEDED AT SEAT 33 by law C.2 ({@code heteroMetaDefaultJoinDeref} +
     * {@code iteArmMultiDefaultTernary}; C2-trip1.log). This was the seat-28 DECLINE LOCK for a
     * FILTERED meta RIGHT: the walker could not see through the filter, so the join kept the
     * wrapper and the pin locked "bytes unchanged". The lock's own javadoc named the shape's
     * ONLY in-corpus carrier -- {@code fn:Price} -- and cited golden {@code Price.java:123}
     * carrying the LEFT deref {@code .<X>map("Type coercion", referenceWithMetaX -> ...)}: i.e.
     * golden DEREFS this shape, and the seat-28 pin locked the fork's decline, not golden. Law
     * C.2 reads the RIGHT on the compiled stamp with the {@code recoverExprMetaWrapper} walker
     * as the fall-back, derefs BOTH operands and renders the list-form ternary -- Price is WHOLE
     * (PriceLadderWholeSeatTest.corpus_c1/c2, whole-file byte compares). The lock now holds
     * the golden-backed shape: the left deref IS present, the join keeps the seat-28 lock's
     * {@code .getValue()).getOrDefault(} form (the review of seat 33 restored this guard, which
     * the C.2 re-pin had dropped), and the in-filter guarded {@code fieldWithMeta...} coercion
     * still survives - pinned by its {@code map("Type coercion", fieldWithMeta} head rather than
     * the bare word, which two coercion params share.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void e1_filteredMetaRightDerefsTheLeftAtTheJoin() throws IOException {
        String out = fixtureRule("E1FilteredMetaRightRule");
        assertTrue(out.contains(">map(\"Type coercion\", referenceWithMeta"),
                "the ReferenceWithMeta left derefs at the default seat - golden Price.java:123's"
                        + " own form (law C.2): " + out);
        assertContains(out, ".getValue()).getOrDefault(");
        assertTrue(out.contains("map(\"Type coercion\", fieldWithMeta"),
                "the in-filter fieldWithMeta coercion must survive: " + out);
    }

    /** corpus_c1 -- the WHOLE heal, drr 7.0.0. All three of its sigs fall to this one rung. */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_drr700UnderlyingIndexIndicatorRuleMatchesGolden() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");
        List<String> own = drrAGenErrors.stream().filter(e -> e.contains(CARRIER)).toList();
        assertTrue(own.isEmpty(), "generation errors for " + CARRIER + ": " + own);
        String gen = drrAOutput.get(CARRIER);
        assertNotNull(gen, "not generated: " + CARRIER);
        assertEquals(normalize(Files.readString(GOLDEN_A.resolve(CARRIER))), normalize(gen),
                "UnderlyingIndexIndicatorRule must byte-match golden - seat 32 law D1: the"
                        + " meta left derefs in place over a walker-verified meta-free right");
    }

    /** corpus_c2 -- the same WHOLE heal in a second cell; the delta is byte-identical to A's. */
    @Test
    @EnabledIf("cellBAvailable")
    void corpus_c2_drr720UnderlyingIndexIndicatorRuleMatchesGolden() throws IOException {
        assertNotNull(drrBOutput, "drr 7.2.0 generation did not run");
        String gen = drrBOutput.get(CARRIER);
        assertNotNull(gen, "not generated: " + CARRIER);
        assertEquals(normalize(Files.readString(GOLDEN_B.resolve(CARRIER))), normalize(gen),
                "UnderlyingIndexIndicatorRule must byte-match golden in drr 7.2.0 too"
                        + " - seat 32 law D1");
    }

    /**
     * corpus_control0 -- GOLDEN IS THE ORACLE, in both directions, quoted from real bytes: the
     * carrier's golden carries the deref'd-left adjacency exactly once, and the
     * homogeneous-meta {@code MessageID} carries none. GREEN in both states; it fails only if
     * the oracle itself was misread.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control0_goldenDerefsTheCarrierAndKeepsTheHomogeneousWrapper() throws IOException {
        String carrier = Files.readString(GOLDEN_A.resolve(CARRIER));
        assertEquals(1, count(carrier, ".getValue()).getOrDefault("),
                "golden UnderlyingIndexIndicatorRule derefs this left in place");
        assertTrue(carrier.contains(".first().<String>map(\"Type coercion\", "),
                "golden's hop is typed at the JOINED bare value type");
        String message = Files.readString(GOLDEN_A.resolve(MESSAGE_ID));
        assertTrue(message.contains(".getOrDefault("),
                "golden MessageID is a default carrier");
        assertTrue(!message.contains(".getValue()).getOrDefault("),
                "golden MessageID is HOMOGENEOUS meta and must keep its wrapper");
    }

    /**
     * corpus_control1 -- LAW 79, the whole-cell UNION scan on drr 7.0.0. Per file the
     * (deref'd-left default, meta-left default that KEEPS the wrapper, hoisted wrapper local)
     * triple must equal golden's, file for file over the union, beyond the NAMED residue. The
     * scan, {@code codeOnly()} and the union assert are the SAME definitions
     * {@code DefaultMetaJoinSeatTest} uses at the same seat (LAW 69: one definition of the
     * token, two consumers) -- if the two ever disagree the seat has two truths.
     *
     * <p>This control fails on OVER-fire (a green file gains T1 / loses T2) and on UNDER-fire
     * (the carrier keeps its T3 hoist), which is the whole point of pinning the union rather
     * than the intersection.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control1_forkDrr7WholeCellMetaJoinSitesEqualGoldenFileByFile() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");
        assertEquals(List.of(), drrAGenErrors,
                "drr 7.0.0 reported a generation error - the scan is incomplete");
        assertUnionEqual(scan(drrAOutput), scan(readGoldenTree(GOLDEN_A)), drrAOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_7, GOLDEN_DOMAIN);
    }

    /**
     * The NAMED residue at THIS LAW'S HEAD (LAW 73: pin the SET, not the count) -- CLAIMED,
     * to be transcribed VERBATIM from this control's own failing print at the chain (LAW 81).
     *
     * <p>Derived from {@code DefaultMetaJoinSeatTest.KNOWN_RESIDUE_7}, whose 7 entries were
     * measured at the seat-31 chain head {@code f2a4d5c0} against the identical scan on the
     * identical cell, with TWO movements this law causes:
     * <ul>
     *   <li><b>{@code UnderlyingIndexIndicatorRule.java} (was
     *       {@code fork=[0, 0, 1] golden=[1, 0, 0]}) LEAVES this list</b> -- it is healed
     *       WHOLE. Golden's T1 = 1 keeps the file inside the union domain, so
     *       {@code GOLDEN_DOMAIN} does NOT move.</li>
     *   <li><b>{@code IndicatorOfTheUnderlyingIndexRule.java} MOVES</b> -- it is in this law's
     *       8-row would-fire set, and golden derefs its left too, so the rung lands there; the
     *       file goes WHOLE only with law D2 (the multi-default ternary), which lands AFTER
     *       this commit in the charter's order. The value below is the ANALYTIC estimate
     *       ({@code fork=[0, 1, 1]} &rarr; the T2 becomes a T1 as the left derefs, and the
     *       consumer's wrapper hoist follows the re-stamped bare element);
     *       <b>{@code fork=[1, 0, 1]} is the live alternative if the consumer hoist survives.
     *       TRANSCRIBE FROM THE PRINT.</b></li>
     * </ul>
     *
     * <p><b>DISCLOSURE for the seat's FINAL head:</b> when law D2 lands,
     * {@code IndicatorOfTheUnderlyingIndexRule} heals whole, its fork triple becomes
     * {@code [0, 0, 0]} = golden's, the row leaves this list AND the file leaves the union
     * domain -- so {@code GOLDEN_DOMAIN} moves 577 &rarr; 576 at that commit, not at this one.
     * Re-pin both from the print.
     */
    private static final List<String> KNOWN_RESIDUE_7 = List.of(
            // the GetBasketConstituents row (fork=[0, 1, 2] golden=[0, 1, 4]) LEFT this list: law A.1
            // (ctorSetterMetaDerefFunctionHost + the rung-3 ctorSetterHoistTextOrder numbering) took its tuples to
            // golden's in all four drr 7.x cells; the file stays BANDED on law A.2's lambda-name line, which this
            // tuple set cannot see; transcribed from this control's own print (A1-trip1.log).
            // the UnderlierProductIdentifier row (fork=[0, 10, 0] golden=[0, 14, 0]) LEFT this list at seat 32: law A.2 (wrapperItemReceiverBind)
            // healed it WHOLE in all four drr 7.x cells after this suite's pins were measured; transcribed from
            // the checkpoint-2 full-gensuite print (ckpt2-gensuite.log).
            // MOVED BY THIS LAW - analytic; transcribe from the print (was fork=[0, 1, 1]).
            // the IndicatorOfTheUnderlyingIndexRule row (fork=[1, 0, 0] golden=[0, 0, 0]) LEFT this list at seat 32: law D.2
            // (extractBodyMultiDefaultTernary, on D.1) healed it WHOLE after this suite's pins were measured;
            // transcribed from the checkpoint-2 full-gensuite print (ckpt2-gensuite.log).
            // UnderlyingIndexIndicatorRule.java (was fork=[0, 0, 1] golden=[1, 0, 0]) LEFT this
            // list at seat 32 law D1: healed WHOLE in all four drr 7.x cells. Golden's T1 = 1
            // keeps the file inside the union domain, so GOLDEN_DOMAIN is UNMOVED at 577.
            // the Price row (fork=[0, 3, 1] golden=[0, 2, 1]) LEFT this list: law C.2 (heteroMetaDefaultJoinDeref + iteArmMultiDefaultTernary)
            // took the rung-1 default join, the last residue after C.1 - Price is WHOLE in all four drr 7.x cells;
            // transcribed from this control's own print (C2-trip1.log), a pure row removal.
            // LAW 81 re-pin (seat 33, law C.1): fork=[0, 3, 0] -> fork=[0, 3, 1] - the statement ladder moved this scan's fork side toward golden; the file stays BANDED on C.2's default join; from C1-trip1.log.
            // the QuantityUnitOfMeasure row (fork=[1, 4, 4] golden=[1, 3, 5]) LEFT this list: law B.24 (defaultJoinHeteroMetaDerefBoth +
            // iteArmMetaCollapseDerefSinkChannel + three in-seat rungs) healed the file WHOLE in all four drr 7.x
            // cells - the band's last four files; transcribed from this control's own print (B24-trip1.log),
            // a pure row removal (was == expected minus it).
            // LAW 81 re-pin (seat 33, law C.2): fork=[0, 5, 3] -> fork=[1, 4, 4] - R3a's join deref moved this scan's fork side (LAW 80 IMPROVED-not-whole, planned; QUOM closes at B.24); from C2-trip1.log.
            // LAW 81 re-pin (seat 33, law B.3): fork=[0, 5, 0] -> fork=[0, 5, 3] - the bare-rule invocations + their guarded arg derefs moved this scan's fork side; the file stays BANDED (B.24); from B3-trip1.log.
            // the TotalNotionalQuantity row (fork=[0, 7, 6] golden=[0, 7, 0]) LEFT this list: law D.3 (fnDeepCondBaseConfinedArmChainAdmit,
            // seven rungs) healed the file WHOLE in all four drr 7.x cells; transcribed from this control's own
            // print (D3-trip1.log), a pure row removal (was == expected minus it).
            );

    /**
     * The drr 7.0.0 union domain for this token triple, DERIVED by a read-only walk this seat
     * ran ({@code target/seat32-instruments/drafts32/D1/golden-domain-walk.py}, an
     * {@code os.walk} -- {@code rg} does not follow the corpus junctions -- reproducing
     * {@code codeOnly()} and {@code scan()} exactly):
     * <pre>
     *   591  golden .java files under drr-7.0.0/rosetta-source/src/generated/java bearing
     *        a T1/T2/T3 token (of 7,808 goldens)
     *  - 15  of kinds this harness never emits (5 POJO types + 10 validation/datarule
     *        classes) and which the union assert's retainAll(emittedA) removes
     *  = 576  golden-bearing files inside the emitted set
     *  +  1   the one FORK-only bearer: IndicatorOfTheUnderlyingIndexRule (golden [0,0,0],
     *        fork non-zero)
     *  = 577
     * </pre>
     * which is exactly {@code DefaultMetaJoinSeatTest.DOMAIN_DRR7}, measured at the seat-31
     * chain -- two independent derivations agreeing. This law does not move it: the carrier
     * keeps a non-zero triple (golden's T1 = 1) after the heal. See KNOWN_RESIDUE_7 for the
     * movement law D2 causes at the seat's final head.
     */
    ///PIN: 577 -> 576 at seat 32 (law D.2, one commit after this suite): the healed IndicatorOfTheUnderlyingIndexRule's
    ///PIN: GOLDEN tuple is all-zero, so it leaves the token-bearing union once its fork junk is gone - the move this
    ///PIN: suite's draft predicted for D.2's commit; transcribed from the checkpoint-2 print (ckpt2-repin1.log).
    private static final int GOLDEN_DOMAIN = 576;

    /**
     * corpus_control2 -- LAW 77, BOTH-ROUTES-vs-GOLDEN. {@code visitDefault} on the IR route
     * delegates to this handler ({@code IRExpressionCompiler:3441}), so the heal is inherited
     * by construction and the carrier must be golden-identical there too. The probe measured
     * the seat route-identical (1,216 {@code [P32-DEF]} rows, identical verdict fields); this
     * asserts it per FILE, which is what LAW 77 requires.
     */
    @Test
    @EnabledIf("cellAAndIrProviderAvailable")
    void corpus_control2_irRouteMatchesGoldenForTheCarrier() throws IOException {
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", CELL_A_ROOT),
                new ArrayList<>());
        assertEquals(normalize(Files.readString(GOLDEN_A.resolve(CARRIER))),
                normalize(irOut.get(CARRIER)), "IR route vs GOLDEN: " + CARRIER);
    }

    // =========================================================================
    // The union assert (LAW 73: pin the SET, not the count) -- the
    // DefaultMetaJoinSeatTest definition, verbatim
    // =========================================================================

    private static void assertUnionEqual(Map<String, int[]> a, Map<String, int[]> b,
            java.util.Set<String> emittedA, String aName, String bName, List<String> knownResidue,
            int expectedDomain) {
        // Scoped to the files this harness emits (rule/report/function kinds): golden's
        // POJO/DATA_RULE kinds carry tokens these generators never produce - the D11
        // ring's question, not this suite's.
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
        assertEquals(knownResidue, mismatched,
                "(T1, T2, T3) differ beyond the named residue in " + mismatched.size() + " file(s)");
        assertEquals(expectedDomain, universe.size(),
                "the union domain must equal the oracle's token-bearing files ("
                        + expectedDomain + ")");
    }

    /**
     * (T1, T2, T3) = deref'd-left defaults, meta-left defaults that KEEP the wrapper, hoisted
     * wrapper locals -- the {@code DefaultMetaJoinSeatTest.scan} definition verbatim (LAW 69).
     * T1 is the adjacency {@code .getValue()).getOrDefault(}, the same token the {@code #144}
     * skip predicate reads at {@code FunctionExpressionRenderer}; T2 counts
     * {@code .getOrDefault(} sites whose receiver text names a wrapper and which are NOT
     * already deref'd -- the token this law REMOVES at its carriers; T3 is the consumer-side
     * wrapper hoist (B009's shape). All three run on {@code codeOnly()}, so the
     * {@code "Type coercion"} LABEL is stripped and cannot be used -- the scan keys on
     * structure, which is the point.
     */
    private static Map<String, int[]> scan(Map<String, String> tree) {
        Map<String, int[]> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : tree.entrySet()) {
            String code = codeOnly(e.getValue());
            int t1 = count(code, ".getValue()).getOrDefault(");
            int t2 = 0;
            int t3 = count(code, "final FieldWithMeta") + count(code, "final ReferenceWithMeta");
            for (String line : code.split("\n")) {
                int at = line.indexOf(".getOrDefault(");
                if (at < 0) {
                    continue;
                }
                String receiver = line.substring(0, at);
                if (receiver.contains("WithMeta") && !line.contains(".getValue()).getOrDefault(")) {
                    t2++;
                }
            }
            if (t1 + t2 + t3 > 0) {
                out.put(e.getKey(), new int[] {t1, t2, t3});
            }
        }
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

    /** Comments and string literals stripped -- the DefaultMetaJoinSeatTest definition. */
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

    // =========================================================================
    // Fixture harness (the InLambdaBoolHoistShadowSeatTest pattern, verbatim)
    // =========================================================================

    private record Render(Map<String, String> output, List<String> errors) {}

    private static Render rendered;

    private static Render render() throws IOException {
        if (rendered != null) {
            return rendered;
        }
        RModel main = AstBuilder.buildFromString(MODEL, "seat32d1.rosetta");
        main.setVersion("0.0.0.test");
        List<RModel> models = new ArrayList<>();
        models.add(main);
        models.addAll(loadBuiltinsOnly());
        RWorkspace workspace = RWorkspace.build(models).workspace();
        GeneratorModel gm = new GeneratorModel(workspace,
                m -> "census.seat32d1".equals(m.namespace()));
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

    private static void assertContains(String out, String token) {
        assertTrue(out.contains(token), "expected token missing:\n  " + token + "\nin:\n" + out);
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
            throw new AssertionError("[DefaultJoinDerefAtCollapsedLeftSeatTest] builtins parse"
                    + " failures: " + String.join("; ", failures));
        }
        return models;
    }

    // =========================================================================
    // Corpus harness (the ReceiverRenderTypingSeatTest cell generator, verbatim)
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
                    new D11CorpusRegressionTest.CellSpec("drr", "7.2.0", CELL_B_ROOT), errs);
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

    private static void collect(List<String> sink, List<GenerationException> errors) {
        if (errors != null) {
            errors.forEach(e -> sink.add(e.getTargetPath() + " - " + e));
        }
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n");
    }

}
