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
 * SEAT 33, law A.4 -- facet {@code lolDefaultBodyMulti}: <b>a {@code default} body whose
 * operand is the BARE PIPED ITEM keeps the list at a bound list-of-lists receiver.</b>
 *
 * <p><b>GOLDEN &lt;- FORK</b> (drr 7.0-7.3 {@code GetBasketConstituents}, hunks 0/2/3 = sigs
 * B001 + B020 + B008):
 * <pre>
 *   GOLDEN  import java.util.Collections;
 *           final MapperListOfLists&lt;AssetIdentifier&gt; thenArg4 = thenArg3
 *               .mapListToList(item -&gt; MapperC.of(Collections.singletonList(MapperS.of(
 *                   filterAssetIdentifier.evaluate(item.getMulti(), AssetIdTypeEnum.ISIN))
 *                   .getOrDefault(item.get()))));
 *           result.addAll(toBuilder(thenArg4.mapListToItem(item -&gt; { ... })
 *   FORK    (no Collections import)
 *           final MapperC&lt;AssetIdentifier&gt; thenArg4 = thenArg3
 *               .mapListToItem(item -&gt; MapperS.of(filterAssetIdentifier.evaluate(
 *                   item.getMulti(), AssetIdTypeEnum.ISIN)).getOrDefault(item));
 *           result.addAll(toBuilder(thenArg4.mapItem(item -&gt; { ... })
 * </pre>
 * Source: {@code test-corpus/drr/drr-7.0.0/rosetta-source/src/main/rosetta/
 * base-trade-basket-func.rosetta}:49 -- {@code then extract underlier.FilterAssetIdentifier(
 * item, ISIN) default item}.
 *
 * <p><b>THREE DIFFERENCES IN ONE HUNK, hence FOUR RUNGS IN ONE COMMIT</b> (the seat-32 B.1-v1
 * lesson: a whole heal is counted at the FILE, and the seat-32 {@code verdicts32-G} charter of
 * this law as "SIZE S, one disjunct" heals ONE of the three):
 * <ul>
 *   <li><b>rung 1</b> {@code CollectionHandler.mapMethod}'s bound-LoL arm -- the map METHOD.
 *       The DECL and the NEXT step's {@code mapListToItem} ride the same {@code mapMethod} SOT
 *       for free (the #274 two-halves-agree law).</li>
 *   <li><b>rung 2</b> the lambda POSITION follows the selected method
 *       ({@code MapperListOfLists.mapListToList} is {@code Function<MapperC<T>, MapperC<F>>},
 *       rune-runtime {@code MapperListOfLists}:94).</li>
 *   <li><b>rung 3</b> {@code SetOperationHandler.reduceDefaultRightToBare}'s {@code navChain}
 *       set -- the {@code getOrDefault} ARGUMENT collapses {@code .get()}
 *       ({@code Mapper.getOrDefault(T)} takes the ITEM, rune-runtime {@code mapper/Mapper}:26).
 *       A NEW DISJUNCT, not a widening of the nested-then / min-max collapse arms.</li>
 *   <li><b>rung 4</b> {@code CollectionHandler.compileLambda}'s post-body arm ladder -- the
 *       item-to-list coercion, emitted through the EXISTING {@code wrapSingleArmMapperCOf}
 *       (LAW 69). {@code java.util.Collections} (B001) rides its refs.</li>
 * </ul>
 *
 * <p><b>GREEN BLAST RADIUS -- MEASURED, both routes.</b> rung 1: {@code [P32-LOLBODY]} -- the
 * complete bound-LoL population is 60 rows / 6 {@code where=} and the only
 * {@code bodyKind=RDefaultExpr} rows in it are the carrier's 8; the implicit-operand conjunct is
 * strictly narrower. rung 3: {@code [P33-DEFRIGHT]} -- {@code rightKind=RImplicitVariable} is
 * 4 rows / 1 {@code where=} = the carrier, over a 100-row positive-control population showing
 * four right kinds. rungs 1+2+4 by CENSUS over the frozen baseline: {@code mapListToList(}
 * occurs in <b>4 of the 174,141 goldens</b> -- {@code GetBasketConstituents} x drr 7.0-7.3 --
 * and 0 fork files on BOTH routes; {@code .getOrDefault(item)} = <b>0 goldens</b>,
 * {@code .getOrDefault(item.get())} = 4 = the carriers; and {@code default item} occurs in ONE
 * source-file family of the 4,062-file {@code .rosetta} corpus.
 *
 * <p><b>THIS LAW IS AN ENABLER -- the carrier does NOT go whole here</b> (LAW 80). All FIVE GBC
 * laws must land: A.4 (B001 B020 B008), A.3 {@code ladderBlockCtorNestedExtract} (B004 x2),
 * A.5 {@code deepThenIteLadderArmJoinMulti} (B019 B005 B037), A.1
 * {@code ctorSetterMetaDerefFunctionHost} (B010 B026 B011 B025 B009) and A.2
 * {@code aliasCondLadderChoiceJoin} (B016, which takes the file whole). {@code corpus_c1} /
 * {@code corpus_c2} therefore pin the EXACT post-law residue rather than a byte compare, and
 * name the law that flips each row. <b>Every sibling GBC law re-pins those two rows in its own
 * commit</b> -- that is the planned accounting, not a surprise.
 *
 * <p><b>CLAIMED RED at the law's base head</b> (both routes): {@code a1}, {@code corpus_c1},
 * {@code corpus_c2}, {@code corpus_control1} (+ {@code corpus_control2} under {@code -Pir-on}).
 * <b>CLAIMED GREEN after:</b> the whole suite. {@code corpus_control0}, {@code e1} and
 * {@code e2} are GREEN at BOTH states by design -- {@code control0} is the golden-side oracle
 * (prove the instrument can fail) and {@code e1}/{@code e2} are the decline locks.
 *
 * <p><b>MUTATIONS (LAW 66/76) -- CLAIMED, measured by the chain (LAW 82):</b>
 * <ul>
 *   <li>{@code m-lawA4-lolmulti} (sever rung 1's disjunct): {@code a1}, {@code corpus_c1},
 *       {@code corpus_c2}, {@code corpus_control1} fail. Rungs 2 and 4 are keyed on the
 *       SELECTED METHOD, so severing rung 1 disables the whole law -- the lane is the law's
 *       root sever.</li>
 *   <li>{@code m-lawA4-position} (sever rung 2's lift alone): the method and the decl flip but
 *       the lambda stays MapperS-expecting, so the singleton lift never fires --
 *       {@code a1}'s {@code MapperC.of(Collections.singletonList(} + {@code Collections} import
 *       asserts fail, and {@code corpus_c1}/{@code c2}/{@code control1} fail. THE HALF-HEAL
 *       LANE.</li>
 *   <li>{@code m-lawA4-implicitright} (sever rung 3's term): the default falls to the legacy
 *       tail, so the arg keeps the bare Mapper AND loses the unwrap contract that rung 4's
 *       first conjunct reads -- DECLARED: this lane moves BOTH the {@code item.get()} and the
 *       {@code Collections.singletonList(} assertions of {@code a1}, plus
 *       {@code corpus_c1}/{@code c2}/{@code control1}.</li>
 *   <li>{@code m-lawA4-lift} (sever rung 4's arm): {@code a1}'s wrap + import asserts and
 *       {@code corpus_c1}/{@code c2}/{@code control1} fail; the method, decl and arg collapse
 *       survive.</li>
 *   <li>{@code m-lawA4-multiguard} (sever ONLY rung 4's {@code !chainProvesMulti} green
 *       guard): the carrier is unaffected; {@code e2} -- the both-strict MULTI {@code default}
 *       at a {@code mapSingleToList} seat, the shape golden cdm6
 *       {@code MapBasketReferenceInformation} carries -- must fail. It is the ONLY test that
 *       moves under this lane, which is what makes the conjunct load-bearing rather than
 *       decorative.</li>
 * </ul>
 *
 * <p><b>LAW 77.</b> The IR route INHERITS: {@code grep -rn 'mapListToItem|mapListToList|
 * isBodyMulti|MapperListOfLists' rune-ir-java/src/main/java} is EMPTY,
 * {@code IRCollectionHandler extends CollectionHandler}, and
 * {@code IRExpressionCompiler.visitDefault} delegates to {@code super.visitDefault}
 * ({@code :3440-3441}; the IR-claimed path is the {@code correlatedOracleServe} "byte-identical
 * BY IDENTITY" seat at {@code :13980-13983}). {@code corpus_control2} is the route gate, not a
 * formality: it compares the IR-route carrier text against the DEFAULT-route text file-for-file.
 *
 * <p><b>LAW 74.</b> {@code javac33/pre-javac.txt} STANDING rows repaired by this law:
 * {@code Seat33Pre.java:526} {@code incompatible types: MapperC<AssetIdentifier> cannot be
 * converted to AssetIdentifier} (rung 3's target -- the bare Mapper into the T-typed
 * {@code getOrDefault} overload) and {@code :527} {@code inference variable T has incompatible
 * bounds} (the {@code thenArg4} cascade). PRE 2 -&gt; POST 0. The GBC group's other five
 * STANDING rows ({@code :533}, {@code :537}, {@code :544}, {@code :548}, {@code :552}) belong to
 * laws A.1 and A.3 and are NOT claimed here.
 *
 * <p><b>LAW-81 tripwires this law fires in OTHER suites</b> (listed in NOTES.md, not edited
 * here): {@code ChoiceSuperOptionIdCarrySeatTest}, {@code CtorSetterValueWrapperRecoverySeatTest}
 * (its {@code KNOWN_HOIST_RESIDUE_DRR7}), {@code DepFieldTypeCollisionSeedSeatTest},
 * {@code DisguisedRenderChainCardinalitySeatTest}, {@code ImplicitInputDisguiseSeatTest},
 * {@code RulePathFilterLolSeatTest}, {@code ThenArgDeclKindFromCompiledSeatTest},
 * {@code WildcardLocalDeclSeatTest} (each loses its {@code GetBasketConstituents} row -- the
 * fork tuple reaches golden's) and {@code BlockLambdaSingleArmListLiftSeatTest} (its two pinned
 * SITES move down one line, 143/166 -&gt; 144/167, because B001 adds an import above them).
 *
 * <p><b>LAW 82 - MEASURED by the receipts chain, run 1 at ce1a06292 (final33.status; every
 * figure below is transcribed from the chain's own logs, never from this file's earlier
 * CLAIMED paragraphs, which it supersedes).</b> GREEN 8/0F/1skip default (f33-green-default.log) /
 * 8/0F/0skip {@code -Pir-on} (f33-green-on.log); RED at the pre-seat base {@code fa49da010}: default
 * 4F = a1, corpus_c1, corpus_c2, corpus_control1; {@code -Pir-on} 5F = a1, corpus_c1, corpus_c2, corpus_control1, corpus_control2 (f33-red-{default,on}.log).
 * Mutation lanes ({@code mut33.py}, the default profile; {@code lanes33.py --summary}):
 * <ul>
 *   <li><b>{@code m-lawA4-lolmulti}</b> ({@code CH_PAIRS_MUT_M_LAWA4_LOLMULTI}): MEASURED 8/4F/1skip = a1, corpus_c1, corpus_c2, corpus_control1 - MATCH (a1, c1, c2, control1 - the ROOT sever).</li>
 *   <li><b>{@code m-lawA4-position}</b> ({@code CH_PAIRS_MUT_M_LAWA4_POSITION}): MEASURED 8/3F/1skip = a1, corpus_c1, corpus_c2 - RE-SCORED: a1, c1, c2 - control1 HELD: the lift's sever changes the carrier's text without moving the whole-cell scan's tuple; the byte compares carry the witness.</li>
 *   <li><b>{@code m-lawA4-implicitright}</b> ({@code SO_PAIRS_MUT_M_LAWA4_IMPLICITRIGHT}): MEASURED 8/4F/1skip = a1, corpus_c1, corpus_c2, corpus_control1 - MATCH (a1, c1, c2, control1).</li>
 *   <li><b>{@code m-lawA4-lift}</b> ({@code CH_PAIRS_MUT_M_LAWA4_LIFT}): MEASURED 8/3F/1skip = a1, corpus_c1, corpus_c2 - RE-SCORED: a1, c1, c2 - control1 HELD (the same tuple-blind reason as position).</li>
 *   <li><b>{@code m-lawA4-multiguard}</b> ({@code CH_PAIRS_MUT_M_LAWA4_MULTIGUARD}): MEASURED 8/0F/1skip = (none) - RE-SCORED: EMPTY - e2 HELD: the `!chainProvesMulti` green guard is not what declines the e2 shape (it declines earlier); the conjunct is defence-in-depth at this corpus AND at the fixture, its zero now measured.</li>
 * </ul>
 */
class LolDefaultBodyMultiSeatTest {

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

    /** The carrier. Identical in all four drr 7.x cells (one gen md5, one golden md5). */
    private static final String GBC = "drr/base/trade/basket/functions/GetBasketConstituents.java";

    /** Cell A = drr 7.0.0 -- the carrier and the whole-cell union control. */
    private static final Path CELL_A_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path GOLDEN_A = CELL_A_ROOT.resolve("rosetta-source/src/generated/java");
    /** Cell B = drr 7.3.0 -- the SECOND cell (a different transitive CDM and rune-fpml). */
    private static final Path CELL_B_ROOT = Path.of("../test-corpus/drr/drr-7.3.0");
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
    // The golden tokens (transcribed from
    // test-corpus/drr/drr-7.0.0/rosetta-source/src/generated/java/<GBC>).
    // =========================================================================

    /** rungs 1+2+4 in ONE token -- the method, the position and the singleton lift. */
    private static final String GOLDEN_LOL_STEP =
            ".mapListToList(item -> MapperC.of(Collections.singletonList(MapperS.of("
            + "filterAssetIdentifier.evaluate(item.getMulti(), AssetIdTypeEnum.ISIN))"
            + ".getOrDefault(item.get()))));";

    /** rung 3 alone -- the getOrDefault ARGUMENT collapse. */
    private static final String GOLDEN_ARG_COLLAPSE = ".getOrDefault(item.get())";

    /** The fork-only form rung 3 removes; ZERO of the 174,141 goldens carry it. */
    private static final String FORK_BARE_ARG = ".getOrDefault(item)";

    /** rung 4's ref -- sig B001. */
    private static final String GOLDEN_COLLECTIONS_IMPORT = "import java.util.Collections;";

    /** rung 1's DECL consequence (the #274 two-halves-agree law through the mapMethod SOT). */
    private static final String GOLDEN_LOL_DECL = "final MapperListOfLists<AssetIdentifier> thenArg";
    private static final String FORK_MAPPERC_DECL = "final MapperC<AssetIdentifier> thenArg";

    /** rung 1's NEXT-STEP consequence -- sig B008. */
    private static final String FORK_MAP_ITEM = ".mapItem(item ->";

    // =========================================================================
    // Fixtures.
    //
    // A1 is reduced from base-trade-basket-func.rosetta:37-51 (`func GetBasketConstituents`,
    // the FIRST `add result:` chain). It keeps EVERY structure the law reads: the whole-output
    // `add`, a MULTI chain root, a MULTI extract body that mints the list-of-lists
    // (mapItemToList), the `<fn>(item) default item` body whose RIGHT is the bare piped item,
    // the callee's MULTI first input (so the argument renders `item.getMulti()` -- the E.3
    // non-witness lesson: a SINGLE-cardinality reduction of a multi-cardinality carrier is not
    // a witness), and the following constructor extract that reads the now-LoL binding (B008).
    // It drops the ladder, the meta wrappers and the second `add` -- none of which any rung
    // reads. The only deliberate simplification is the callee's second (enum) argument, which
    // belongs to the BareEnumArgExpectedOwner facet, not to this law.
    //
    // E1 is A1 with the default's RIGHT changed from the bare piped item to a second function
    // call -- the exact near-miss rung 1's implicit-operand conjunct exists to decline.
    //
    // E2 is the GREEN population rung 4's fourth conjunct protects: a both-strict MULTI
    // `default` body at a single receiver, which isBodyMulti's #363 arm puts at a
    // mapSingleToList / MapperC-EXPECTING seat. Golden cdm6 MapBasketReferenceInformation
    // (`extract [<mapC chain> default <mapC chain>]`) is its corpus representative.
    // =========================================================================

    private static final String MODEL = """
            namespace census.seat33a4
            version "1.0.0"

            type Id:
                id string (0..1)

            type X:
                ids Id (0..*)
                otherIds Id (0..*)

            type Holder:
                xs X (0..*)

            type Rep:
                id string (0..1)

            func PickId: <"the FilterAssetIdentifier stand-in: a MULTI first input and a SINGLE output, so the call renders MapperS.of(pickId.evaluate(item.getMulti()))">
                inputs:
                    ids Id (0..*)
                output:
                    picked Id (0..1)
                set picked:
                    ids first

            func PickOther: <"e1's right operand: a SECOND call, so the default carries no implicit-item operand">
                inputs:
                    ids Id (0..*)
                output:
                    picked Id (0..1)
                set picked:
                    ids last

            func A1LolDefaultBody: <"a1 - THE GetBasketConstituents SHAPE: a list-of-lists step followed by a `<fn>(item) default item` body and a constructor step that reads the binding">
                inputs:
                    holder Holder (1..1)
                output:
                    result Rep (0..*)
                add result:
                    holder -> xs
                        then extract item -> ids
                        then extract PickId(item) default item
                        then extract Rep {
                            id: item -> id
                        }

            func E1LolDefaultNoImplicitOperand: <"e1 - DECLINE: the SAME list-of-lists seat and the SAME body KIND, but neither operand is the bare piped item - the step stays list-CONSUMING and nothing lifts">
                inputs:
                    holder Holder (1..1)
                output:
                    result Rep (0..*)
                add result:
                    holder -> xs
                        then extract item -> ids
                        then extract PickId(item) default PickOther(item)
                        then extract Rep {
                            id: item -> id
                        }

            func E2MultiDefaultAtMapperCSeat: <"e2 - DECLINE (rung 4's chainProvesMulti guard): a BOTH-STRICT MULTI default body is already MapperC-expecting through the #363 arm, and must take no singleton lift">
                inputs:
                    holder Holder (1..1)
                output:
                    result Id (0..*)
                add result:
                    holder
                        then extract xs -> ids default xs -> otherIds
            """;

    // =========================================================================
    // Part A -- the seat (CLAIMED RED at the base)
    // =========================================================================

    /**
     * a1 -- all four rungs at the reduced carrier.
     *
     * <p>PIN AT RED: the first two asserts are FIXTURE-REACH pins that are GREEN in both states.
     * {@code .mapItemToList(} proves the first step really minted a list-of-lists (without it
     * the second step's receiver is not a {@code MapperListOfLists} and rung 1's arm is never
     * entered), and {@code item.getMulti()} proves the callee's argument is the MULTI inner
     * list -- the cardinality the carrier has and the E.3 lesson says a reduction must keep. If
     * either fails, the fixture is not reaching the seat: RESHAPE THE FIXTURE, never weaken an
     * assert.
     *
     * <p>Every remaining assert fails at the base for the LAW's own reason: the method
     * ({@code mapListToItem} where golden preserves the list), the argument (a Mapper where
     * {@code getOrDefault(T)} takes the item), the wrap, the import, the decl kind and the next
     * step's method.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_lolDefaultBodyKeepsTheListAndCollapsesTheDefaultArg() throws IOException {
        String out = fixtureFunction("A1LolDefaultBody");
        // --- fixture-reach pins (GREEN at both states) ---
        assertEquals(1, count(out, ".mapItemToList("),
                "reach: the first step must mint the list-of-lists this law reads");
        assertTrue(out.contains("item.getMulti()"),
                "reach: the callee's MULTI first input must render item.getMulti() - a"
                + " SINGLE-cardinality reduction of this carrier is a NON-WITNESS:\n" + out);
        // --- rungs 1+2+4: the method, the position and the singleton lift ---
        assertTrue(out.contains(".mapListToList(item -> MapperC.of(Collections.singletonList("),
                "rungs 1+2+4: the list-PRESERVING step must wrap its SINGLE default body"
                + " item-to-list:\n" + out);
        // --- rung 3: the getOrDefault ARGUMENT ---
        assertTrue(out.contains(GOLDEN_ARG_COLLAPSE),
                "rung 3: getOrDefault(T) takes the ITEM, so the bare piped item collapses"
                + " .get():\n" + out);
        assertEquals(0, count(out, FORK_BARE_ARG),
                "rung 3: the bare Mapper argument must be gone (0 of 174,141 goldens carry it)");
        // --- rung 4's ref (B001) ---
        assertTrue(out.contains(GOLDEN_COLLECTIONS_IMPORT),
                "rung 4: the singleton lift's COLLECTIONS ref must reach the import block:\n"
                + out);
        // --- rung 1's decl half (the #274 two-halves-agree law) ---
        assertEquals(2, count(out, "final MapperListOfLists<Id> thenArg"),
                "rung 1 decl: BOTH list-of-lists steps declare the LoL wrapper (base: 1)");
        assertEquals(0, count(out, "final MapperC<Id> thenArg"),
                "rung 1 decl: no step may still declare MapperC (base: 1)");
        // --- rung 1's next-step consequence (B008) ---
        assertEquals(0, count(out, FORK_MAP_ITEM),
                "B008: the NEXT step binds to a MapperListOfLists, so it is list-CONSUMING"
                + " (mapListToItem), never mapItem (base: 1)");
        assertEquals(1, count(out, ".mapListToItem("),
                "B008: exactly one list-consuming step remains -- the constructor step");
    }

    // =========================================================================
    // Part B -- the decline locks
    // =========================================================================

    /**
     * e1 -- the near-miss rung 1's implicit-operand conjunct declines: the SAME bound
     * list-of-lists seat and the SAME body KIND ({@code RDefaultExpr}), but neither operand is
     * the bare piped item. Everything must keep today's bytes: the step stays list-CONSUMING,
     * nothing lifts and no {@code Collections} import appears. This is the fixture form of the
     * measured fact that the corpus-wide {@code bodyKind=RDefaultExpr} population at this arm
     * is the carrier's 8 rows and nothing else.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void e1_lolDefaultWithoutAnImplicitOperandKeepsTheListConsumingStep() throws IOException {
        String out = fixtureFunction("E1LolDefaultNoImplicitOperand");
        assertEquals(1, count(out, ".mapItemToList("),
                "reach: the first step must still mint the list-of-lists");
        assertTrue(out.contains(".mapListToItem(item -> MapperS.of("),
                "the near-miss keeps the list-CONSUMING step and its MapperS body:\n" + out);
        assertEquals(0, count(out, ".mapListToList("),
                "rung 1 must decline: no operand of this default is the bare piped item");
        assertEquals(0, count(out, "Collections.singletonList("),
                "rung 4 cannot fire when the position never lifted");
        assertEquals(0, count(out, GOLDEN_COLLECTIONS_IMPORT),
                "no COLLECTIONS ref may be added at a declined seat");
    }

    /**
     * e2 -- rung 4's {@code !chainProvesMulti} guard, at fixture grain. A {@code default} whose
     * BOTH operands prove MULTI is put at a {@code mapSingleToList} / MapperC-EXPECTING seat by
     * {@code isBodyMulti}'s own #363 arm; rung 4 reads the SAME predicate on the SAME node, so
     * that population can never enter the lift. Golden cdm6
     * {@code MapBasketReferenceInformation} is the corpus representative.
     *
     * <p>The first assert is a HARD reach pin, not decoration: an {@code e} test whose fixture
     * never reaches the seat passes vacuously, which is the failure mode this lock exists to
     * avoid. {@code m-lawA4-multiguard} is the lane that must move this test and nothing else.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void e2_bothStrictMultiDefaultAtAMapperCSeatTakesNoSingletonLift() throws IOException {
        String out = fixtureFunction("E2MultiDefaultAtMapperCSeat");
        assertEquals(1, count(out, ".mapSingleToList("),
                "reach: the both-strict MULTI default must select the *ToList method (the #363"
                + " arm) - without it this lock is vacuous:\n" + out);
        assertEquals(0, count(out, "Collections.singletonList("),
                "rung 4 must decline a MULTI default body: it is already a list");
        assertEquals(0, count(out, ".mapListToList("),
                "rung 1's arm is not entered - this receiver is not a list-of-lists");
    }

    // =========================================================================
    // Part C -- the corpus (LAW 80: an ENABLER, the file is NOT whole here)
    // =========================================================================

    /**
     * control0 -- golden is the oracle (prove the instrument can fail). Every token this suite
     * asserts is read back off GOLDEN first, and the fork-only forms are asserted ABSENT there.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control0_goldenIsTheOracle() throws IOException {
        String g = normalize(Files.readString(GOLDEN_A.resolve(GBC)));
        assertTrue(g.contains(GOLDEN_LOL_STEP),
                "golden must carry the list-preserving step verbatim");
        assertEquals(1, count(g, GOLDEN_ARG_COLLAPSE), "golden collapses the getOrDefault arg");
        assertEquals(0, count(g, FORK_BARE_ARG),
                "golden NEVER keeps a Mapper as a getOrDefault argument");
        assertTrue(g.contains(GOLDEN_COLLECTIONS_IMPORT), "golden imports java.util.Collections");
        assertEquals(3, count(g, GOLDEN_LOL_DECL), "golden declares three LoL thenArgs");
        assertEquals(0, count(g, FORK_MAPPERC_DECL), "golden declares no MapperC<AssetIdentifier>");
        assertEquals(0, count(g, FORK_MAP_ITEM), "golden's constituent step is list-consuming");
        assertEquals(2, count(g, ".mapListToItem("), "golden carries two mapListToItem steps");
    }

    /**
     * corpus_c1 -- the carrier at drr 7.0.0. The law's own tokens, then the EXACT residue this
     * ENABLER leaves, row by row with the law that flips each.
     *
     * <p><b>WHOLE since A.2 / CHECKPOINT 1</b> -- the residue asserts below read
     * {@code assertEquals(0, ...)} on both sides (re-pinned 35/11 -&gt; 11/9 at A.3, -&gt; 7/5 at
     * A.5, -&gt; 1/1 at A.1, -&gt; 0/0 at CHECKPOINT 1 {@code 7e94f2d16}, each from the print). What
     * follows is the pin's PROVENANCE at this law's head, kept as the record.
     *
     * <p><b>The residue after A.4 (MEASURED offline against the seat-33 probe dumps and pinned
     * here; the chain re-measured):</b> 11 fork-only and 35 golden-only trimmed code lines,
     * distributed as
     * <ul>
     *   <li>{@code A.3 ladderBlockCtorNestedExtract} -- the two {@code unitOfMeasure:} inline
     *       ternaries (2 fork-only, 24 golden-only block lines): {@code RESIDUE_A3_TERNARY} = 6
     *       ({@code .getOrDefault(false) ? } x3 per site x 2 sites);</li>
     *   <li>{@code A.1 ctorSetterMetaDerefFunctionHost} -- the un-hoisted setter derefs and the
     *       wrapper-local renumbering: {@code RESIDUE_A1_*};</li>
     *   <li>{@code A.5 deepThenIteLadderArmJoinMulti} -- the {@code underliers} ite-hoist local
     *       and its two lifted arms + terminal: {@code RESIDUE_A5_DECL};</li>
     *   <li>{@code A.2 aliasCondLadderChoiceJoin} -- the escaped alias lambda name:
     *       {@code RESIDUE_A2_LAMBDA}.</li>
     * </ul>
     * Each sibling law RE-PINS these two rows in its own commit; A.3 lands next and takes the
     * pair to 9 fork-only / 11 golden-only.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_drr700CarrierTakesTheLawAndPinsTheRest() throws IOException {
        assertCarrier(drrAOutput, drrAGenErrors, GOLDEN_A, "drr 7.0.0");
    }

    /**
     * corpus_c2 -- the SECOND cell. drr 7.3.0 resolves against a different transitive CDM and
     * rune-fpml than 7.0.0, so this is an independent render even though the two goldens are
     * byte-identical (one md5 across all four drr 7.x cells, as are the four fork texts).
     */
    @Test
    @EnabledIf("cellBAvailable")
    void corpus_c2_drr730CarrierTakesTheLawAndPinsTheRest() throws IOException {
        assertCarrier(drrBOutput, drrBGenErrors, GOLDEN_B, "drr 7.3.0");
    }

    private static void assertCarrier(Map<String, String> output, List<String> errors,
            Path goldenRoot, String cell) throws IOException {
        assertNotNull(output, cell + " generation did not run");
        List<String> own = errors.stream().filter(e -> e.contains(GBC)).toList();
        assertTrue(own.isEmpty(), "generation errors for " + GBC + " (" + cell + "): " + own);
        String gen = output.get(GBC);
        assertNotNull(gen, "not generated (" + cell + "): " + GBC);
        gen = normalize(gen);
        String golden = normalize(Files.readString(goldenRoot.resolve(GBC)));

        // --- the law's own bytes ---
        assertTrue(gen.contains(GOLDEN_LOL_STEP),
                cell + ": the list-preserving step must match golden verbatim:\n" + gen);
        assertEquals(1, count(gen, GOLDEN_ARG_COLLAPSE), cell + ": rung 3's arg collapse");
        assertEquals(0, count(gen, FORK_BARE_ARG), cell + ": the bare Mapper arg must be gone");
        assertTrue(gen.contains(GOLDEN_COLLECTIONS_IMPORT),
                cell + ": B001 -- the java.util.Collections import must render");
        assertEquals(3, count(gen, GOLDEN_LOL_DECL), cell + ": rung 1's decl half (base: 2)");
        assertEquals(0, count(gen, FORK_MAPPERC_DECL), cell + ": no MapperC decl survives");
        assertEquals(0, count(gen, FORK_MAP_ITEM), cell + ": B008 -- mapItem must be gone");
        assertEquals(2, count(gen, ".mapListToItem("), cell + ": two list-consuming steps");

        // --- the NAMED residue this ENABLER leaves (LAW 80) ---
        assertEquals(0, count(gen, RESIDUE_A3_TERNARY),
                cell + ": law A.3's two inline ternaries are GONE (they flipped at A.3 - LAW 81 re-pin 6 -> 0 from this suite's own print, A3-trip1.log)");
        assertEquals(0, count(gen, RESIDUE_A1_BASKET_SETTER),
                cell + ": law A.1 rung 1's basket setter is HOISTED (flipped at A.1 - LAW 81 re-pin 1 -> 0 from this suite's own print, A1-trip1.log)");
        assertEquals(0, count(gen, RESIDUE_A1_POOL_SETTER),
                cell + ": law A.1 rung 2's pool default is HOISTED (flipped at A.1 - LAW 81 re-pin 1 -> 0 from this suite's own print, A1-trip1.log)");
        assertEquals(4, count(gen, RESIDUE_A1_WRAPPER_LOCAL),
                cell + ": all four wrapper locals render, as golden (re-pinned 2 -> 4 at A.1 from A1-trip1.log)");
        assertEquals(0, count(gen, RESIDUE_A5_DECL),
                cell + ": law A.5's ite-hoist local is now MapperC (flipped at A.5 - LAW 81 re-pin 1 -> 0 from this suite's own print, A5-trip1.log)");
        assertEquals(0, count(gen, RESIDUE_A2_LAMBDA),
                cell + ": law A.2's escaped alias lambda name is GONE (flipped at A.2 - LAW 81 re-pin 1 -> 0 from the checkpoint's own gensuite print, ckpt1-gensuite.log)");
        List<String> forkOnly = onlyIn(gen, golden);
        List<String> goldenOnly = onlyIn(golden, gen);
        assertEquals(0, forkOnly.size(),
                cell + ": GetBasketConstituents is WHOLE after A.2 (re-pinned 5 -> 1 at A.1, 1 -> 0 at A.2/CHECKPOINT 1, ckpt1-gensuite.log):\n" + String.join("\n", forkOnly));
        assertEquals(0, goldenOnly.size(),
                cell + ": GetBasketConstituents is WHOLE after A.2 (re-pinned 7 -> 1 at A.1, 1 -> 0 at A.2/CHECKPOINT 1, ckpt1-gensuite.log):\n"
                + String.join("\n", goldenOnly));
    }

    /** law A.3's residue -- the inline ternary the block ladder replaces (golden 0). */
    private static final String RESIDUE_A3_TERNARY = ".getOrDefault(false) ? ";
    /** law A.1 rung 1's residue -- the basket arm's un-hoisted setter deref (golden 0). */
    private static final String RESIDUE_A1_BASKET_SETTER =
            ".setIdentifier(item.<FieldWithMetaString>map(";
    /** law A.1 rung 2's residue -- the pool arm's inline default chain (golden 0). */
    private static final String RESIDUE_A1_POOL_SETTER =
            ".setIdentifier(MapperS.of(filterAssetIdentifier";
    /** law A.1's numbering half -- golden hoists FOUR wrapper locals, the fork two. */
    private static final String RESIDUE_A1_WRAPPER_LOCAL = "final FieldWithMetaString fieldWithMetaString";
    /** law A.5's residue -- the ite-hoist local's Mapper kind (golden MapperC). */
    private static final String RESIDUE_A5_DECL = "final MapperS<Underlier> ifThenElseResult;";
    /** law A.2's residue -- the escaped alias lambda name (golden `underlier -> underlier...`). */
    private static final String RESIDUE_A2_LAMBDA = "_underliers -> _underliers.getObservable()";

    /**
     * control1 -- LAW 79, the whole-cell UNION scan on drr 7.0.0. Per file the
     * (T1, T2, T3, T4) tuple must EQUAL golden's, beyond the named residue.
     * <ul>
     *   <li><b>T1</b> {@code .mapListToList(} -- the law's ADDED method.</li>
     *   <li><b>T2</b> {@code .getOrDefault(item)} -- the law's REMOVED form. ZERO of the
     *       174,141 goldens carry it, so ANY file holding one is a fork-only divergence.</li>
     *   <li><b>T3</b> {@code final MapperListOfLists<} -- the DECL half, deliberately GLOBAL:
     *       a wrapper kind flipped anywhere in the cell fails here, including at seats this law
     *       does not name.</li>
     *   <li><b>T4</b> {@code .mapListToItem(} -- the method-selection net, likewise global.</li>
     * </ul>
     *
     * <p><b>The domain is DERIVED, not sentinelled.</b> A read-only walk of the 7,808 golden
     * {@code .java} files under {@code test-corpus/drr/drr-7.0.0/rosetta-source/src/generated/
     * java} counts <b>37</b> carrying any of T1..T4 (21 under {@code /functions/}, 16 under
     * {@code /reports/}, none of any other kind), and the fork side adds no file of its own --
     * the only band file in this cell that carries any of these tokens is the carrier, which is
     * already in golden's set. The emitted key set of this harness is exactly the
     * {@code /functions/} + {@code /reports/} kinds, so {@code universe.retainAll(emittedA)}
     * removes nothing. The derivation was CALIBRATED against a measured pin before being
     * trusted: the same walk reproduces {@code RulePathFilterLolSeatTest}'s
     * {@code DOMAIN_DRR7 = 37} and {@code ExtractBodyMultiDefaultTernarySeatTest}'s
     * {@code GOLDEN_DOMAIN = 3579} / {@code DOMAIN_DRR700 = 1233} exactly.
     *
     * <p>This control fails when the law OVER-fires (a green file gains T1/T2 or flips a
     * wrapper kind -- a UNION, so a file entering from either side is compared) AND when it
     * UNDER-fires (the carrier's row stays).
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control1_forkDrr700WholeCellEqualsGoldenFileByFile() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");
        assertEquals(List.of(), drrAGenErrors,
                "drr 7.0.0 reported a generation error - the scan is incomplete");
        assertUnionEqual(scan(drrAOutput), scan(readGoldenTree(GOLDEN_A)), drrAOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_DRR700, DOMAIN_DRR700);
    }

    /**
     * The NAMED residue of drr 7.0.0. <b>EMPTY at the law head, and that is the whole point:</b>
     * these four tokens are exactly the ones law A.4 owns, so once it lands the carrier's tuple
     * EQUALS golden's even though the FILE is still banded on four sibling laws. At the base the
     * control prints one row --
     * {@code drr/base/trade/basket/functions/GetBasketConstituents.java fork=[0, 1, 3, 2]
     * golden=[1, 0, 4, 2]} -- which is this control's failing-first evidence at corpus grain.
     * None of A.3 / A.5 / A.1 / A.2 moves any of T1..T4, so this list stays EMPTY for the rest
     * of the seat.
     */
    private static final List<String> KNOWN_RESIDUE_DRR700 = List.of();

    /** DERIVED by the read-only golden walk described on {@code corpus_control1}. */
    private static final int DOMAIN_DRR700 = 37;

    /**
     * control2 -- LAW 77, the IR route. {@code IRCollectionHandler extends CollectionHandler}
     * and {@code IRExpressionCompiler.visitDefault} delegates, so the route should INHERIT the
     * heal; what an inheritance argument cannot see is whether the IR route claims this
     * {@code default} natively. The compare is against the DEFAULT-route text file-for-file
     * (the seat-32 chain's own route-content check), plus the law's tokens on the IR text.
     * Skips unless {@code -Pir-on}.
     */
    @Test
    @EnabledIf("cellAAndIrProviderAvailable")
    void corpus_control2_irRouteMatchesTheDefaultRouteAtTheCarrier() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", CELL_A_ROOT),
                new ArrayList<>());
        String ir = irOut.get(GBC);
        assertNotNull(ir, "not generated on the IR route: " + GBC);
        assertTrue(normalize(ir).contains(GOLDEN_LOL_STEP),
                "the IR route must inherit the list-preserving step:\n" + ir);
        assertEquals(0, count(normalize(ir), FORK_BARE_ARG),
                "the IR route must inherit rung 3's argument collapse");
        assertEquals(normalize(drrAOutput.get(GBC)), normalize(ir),
                "route divergence at " + GBC);
    }

    // =========================================================================
    // The scan + the union assert (LAW 73: pin the SET, not the count)
    // =========================================================================

    private static Map<String, int[]> scan(Map<String, String> tree) {
        Map<String, int[]> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : tree.entrySet()) {
            String code = normalize(e.getValue());
            int[] t = new int[] {
                    count(code, ".mapListToList("),
                    count(code, ".getOrDefault(item)"),
                    count(code, "final MapperListOfLists<"),
                    count(code, ".mapListToItem(")};
            if (t[0] + t[1] + t[2] + t[3] > 0) {
                out.put(e.getKey(), t);
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
        int[] zero = new int[4];
        for (String key : universe) {
            int[] ac = a.getOrDefault(key, zero);
            int[] bc = b.getOrDefault(key, zero);
            if (!java.util.Arrays.equals(ac, bc)) {
                mismatched.add(key + " " + aName + "=" + java.util.Arrays.toString(ac)
                        + " " + bName + "=" + java.util.Arrays.toString(bc));
            }
        }
        assertTrue(expectedDomain >= 0,
                "the union domain is MEASURED and pinned (LAW 73) - transcribe from this print:"
                        + " MEASURED domain=" + universe.size() + " residue=" + mismatched);
        assertEquals(knownResidue, mismatched,
                "the (mapListToList, getOrDefault(item), MapperListOfLists decl, mapListToItem)"
                        + " tuple differs beyond the named residue in " + mismatched.size()
                        + " file(s)");
        assertEquals(expectedDomain, universe.size(),
                "the union domain must equal the emitted token-bearing files (" + expectedDomain
                        + ")");
    }

    /**
     * The trimmed, blank-stripped code lines present in {@code a} more often than in {@code b}
     * (a multiset difference, order-insensitive). Used by {@code corpus_c1}/{@code corpus_c2} to
     * pin the residue an ENABLER leaves without transcribing 35 golden lines into a literal.
     */
    private static List<String> onlyIn(String a, String b) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String line : b.split("\n")) {
            String s = line.trim();
            if (!s.isEmpty()) {
                counts.merge(s, 1, Integer::sum);
            }
        }
        List<String> out = new ArrayList<>();
        for (String line : a.split("\n")) {
            String s = line.trim();
            if (s.isEmpty()) {
                continue;
            }
            Integer left = counts.get(s);
            if (left == null || left == 0) {
                out.add(s);
            } else {
                counts.put(s, left - 1);
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

    // =========================================================================
    // Fixture harness (the IteChainCtorArmMapperWrapSeatTest function renderer)
    // =========================================================================

    private record Render(Map<String, String> output, List<String> errors) {}

    private static Render rendered;

    private static Render render() throws IOException {
        if (rendered != null) {
            return rendered;
        }
        RModel main = AstBuilder.buildFromString(MODEL, "seat33a4.rosetta");
        main.setVersion("0.0.0.test");
        List<RModel> models = new ArrayList<>();
        models.add(main);
        models.addAll(loadBuiltinsOnly());
        RWorkspace workspace = RWorkspace.build(models).workspace();
        GeneratorModel gm = new GeneratorModel(workspace,
                m -> "census.seat33a4".equals(m.namespace()));
        JavaTypeUtil typeUtil = new JavaTypeUtil();
        JavaTypeTranslator tt = new JavaTypeTranslator(typeUtil);
        FunctionGenerator fg = new FunctionGenerator(gm, tt, typeUtil);
        Map<String, String> out = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        fg.generateWithErrors(out)
                .forEach(e -> errors.add(e.getTargetPath() + " - " + e));
        rendered = new Render(out, errors);
        return rendered;
    }

    private static String fixtureFunction(String fnName) throws IOException {
        Render r = render();
        String path = fnName + ".java";
        List<String> own = r.errors().stream().filter(e -> e.contains(path)).toList();
        assertTrue(own.isEmpty(), "the generator reported errors for " + path + ": " + own);
        String out = r.output().entrySet().stream()
                .filter(e -> e.getKey().endsWith(path))
                .map(Map.Entry::getValue)
                .findFirst().orElse(null);
        assertNotNull(out, "not generated: " + path + " (have: " + r.output().keySet() + ")");
        return normalize(out);
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
            throw new AssertionError("[LolDefaultBodyMultiSeatTest] builtins parse failures: "
                    + String.join("; ", failures));
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
                    new D11CorpusRegressionTest.CellSpec("drr", "7.3.0", CELL_B_ROOT), errs);
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
