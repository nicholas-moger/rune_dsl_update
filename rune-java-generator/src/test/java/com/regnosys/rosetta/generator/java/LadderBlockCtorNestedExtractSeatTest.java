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
 * SEAT 33, law A.3 -- facet {@code ladderBlockCtorNestedExtract}: <b>an enclosing extract
 * ascended into THROUGH its lambda whose body ROOT is a CONSTRUCTOR is TRANSPARENT to
 * {@code isCleanLadderContext}'s extract count.</b>
 *
 * <p><b>GOLDEN &lt;- FORK</b> (drr 7.0-7.3 {@code GetBasketConstituents}, sig B004, TWO sites
 * per file -- the basket arm's and the pool arm's {@code unitOfMeasure:} ladders):
 * <pre>
 *   GOLDEN  .mapSingleToItem(_item -&gt; {
 *               if (exists(_item.&lt;CapacityUnitEnum&gt;map("getCapacityUnit", ...)).getOrDefault(false)) {
 *                   return MapperS.of(capacityUnitToISO20022UnitOfMeasure.evaluate(...));
 *               }
 *               ... two more rungs ...
 *               return MapperS.&lt;String&gt;ofNull();
 *           }).get())
 *   FORK    .mapSingleToItem(_item -&gt; exists(...).getOrDefault(false) ? MapperS.of(...)
 *               : exists(...).getOrDefault(false) ? MapperS.of(...)
 *               : exists(...).getOrDefault(false) ? MapperC.of(MapperS.of(...)) : MapperC.of()).get())
 * </pre>
 * Source: {@code base-trade-basket-func.rosetta} lines 56-63 (basket) and 80-87 (pool).
 *
 * <p><b>The defect.</b> {@code CollectionHandler.isCleanLadderContext} counts the enclosing
 * {@code RExtractExpr} ancestors of a ladder and admits the block render only at
 * {@code extractCount <= 1}. GBC's ladder ascends through TWO: its own map, and the outer
 * {@code then extract BasketConstituentsReport { ... }} whose lambda body ROOT is an
 * {@code RConstructorExpr}. Neither existing transparency covers that shape --
 * {@code navTailTransparent} wants an {@code RExtractExpr} body, {@code fnArgTransparent} wants
 * an args-carrying {@code RSymbolReference} <em>and</em> a RULE host -- so the count reaches 2
 * and the ladder falls to the inline {@code getOrDefault(false) ? } ternary that <b>ZERO of the
 * 174,141 goldens carry</b> (the #281 law). The ladder is a constructor VALUE, not a nested map
 * body; golden renders the outer step's block identically in both texts.
 *
 * <p><b>A THIRD TRANSPARENCY, NEVER A RAISED BOUND.</b> The {@code extractCount <= 1} bound is
 * untouched at all eight of its sites, and so is the RULE anchor: the arm carries
 * {@code HandlerHelper.findEnclosingRule(cond) == null} because {@code [P32-C3ANCHOR]} measured
 * <b>42 rows / 9 RULE basenames at {@code extractCount=2}, FOUR of them GREEN</b>
 * ({@code ExecutionAgentOfCounterparty1DTCC}, {@code ExecutionAgentOfTheCounterparty1DTCC},
 * {@code ExecutionAgentCounterparty1}, {@code ExecutionAgentOfCounterparty1}).
 * {@code ControlFlowHandler.ternaryMixedArityArmLift} is the SYMPTOM of this defect and is
 * likewise untouched.
 *
 * <p><b>GREEN BLAST RADIUS -- ZERO, MEASURED at this head on BOTH routes.</b>
 * {@code [P33-LADDEREXT]} prints 2,062 rows at this arm; filtering
 * {@code bodyKind=RConstructorExpr} gives <b>8 rows / 1 {@code where=} =
 * {@code fn:GetBasketConstituents}</b> (2 ladders x 4 cells), every row
 * {@code encRule=false ownThen=false extractCount=1 wouldSkip=false}. Seat 32's
 * {@code [P32-LADDERCTX]} independently measured {@code verdict=false} in <b>exactly ONE group
 * at the FUNCTION anchor and it IS the carrier</b> (302 rows / 39 groups); a transparency can
 * only turn {@code false} into {@code true}. Positive control fires ({@code navTail=true} = 16
 * rows / 4 {@code where=}), so the zero is trusted.
 *
 * <p><b>THIS LAW IS AN ENABLER -- the carrier does NOT go whole here</b> (LAW 80). It lands
 * SECOND of the GBC five, after A.4 {@code lolDefaultBodyMulti}; A.5
 * {@code deepThenIteLadderArmJoinMulti}, A.1 {@code ctorSetterMetaDerefFunctionHost} and A.2
 * {@code aliasCondLadderChoiceJoin} (which takes the file whole) still follow.
 * {@code corpus_c1}/{@code corpus_c2} therefore pin the EXACT post-law residue and name the law
 * that flips each row; every later GBC law re-pins those two rows in its own commit.
 *
 * <p><b>CLAIMED RED at this law's base head (= the A.4 head), both routes:</b> {@code a1},
 * {@code corpus_c1}, {@code corpus_c2}, {@code corpus_control1} (+ {@code corpus_control2}
 * under {@code -Pir-on}). <b>CLAIMED GREEN after:</b> the whole suite. {@code corpus_control0}
 * and {@code e1} are GREEN at BOTH states by design.
 *
 * <p><b>MUTATIONS (LAW 66/76) -- CLAIMED, measured by the chain (LAW 82):</b>
 * <ul>
 *   <li>{@code m-lawA3-transparency} (sever the arm): {@code a1}, {@code corpus_c1},
 *       {@code corpus_c2}, {@code corpus_control1} fail -- the law's root sever.</li>
 *   <li>{@code m-lawA3-ownthen} (drop {@code !subtreeCarriesThenOrSwitch(cond)}): {@code e1}
 *       fails -- a ladder whose own subtree carries a {@code then} would block-convert, and its
 *       arm-registered hoists can splice between rungs (the cp1
 *       {@code Create_AnnaDsbUpiRequest} AWAY class the whole transparency family polices).</li>
 *   <li>{@code m-lawA3-fnscope} (drop {@code findEnclosingRule(cond) == null}): <b>DECLARED
 *       EMPTY at this corpus</b> ({@code verdicts33-A} § LAW A.3) -- there are ZERO rule-hosted
 *       {@code bodyKind=RConstructorExpr} rows corpus-wide, so no {@code ExecutionAgent*} file
 *       moves and NO test is expected to fail. The lane is KEPT because the conjunct is the
 *       scoping law of a SHARED walk whose rule anchor carries 42 rows / 9 basenames (4 GREEN)
 *       at {@code extractCount=2}; it must be reported as EMPTY-as-declared, never as a passing
 *       sever.</li>
 * </ul>
 *
 * <p><b>LAW 77.</b> INHERITS. {@code IRCollectionHandler extends CollectionHandler};
 * {@code isLadderConditional} / {@code isCleanLadderContext} are {@code private static};
 * {@code grep -rn 'isCleanLadderContext|ladderConditional' rune-ir-java/src/main/java} is EMPTY.
 * {@code corpus_control2} is still the gate and compares the IR-route carrier text against the
 * DEFAULT-route text file-for-file.
 *
 * <p><b>LAW 74.</b> {@code javac33/pre-javac.txt} STANDING rows repaired by this law:
 * {@code Seat33Pre.java:537} and {@code :552}, both
 * {@code incompatible types: cannot infer type-variable(s) F,T#2,T#2} -- the mixed
 * {@code MapperS}/{@code MapperC} ternary handed to
 * {@code mapSingleToItem(Function<MapperS<T>, MapperS<F>>)}, once per ctor site. <b>PRE 2 -&gt;
 * POST 0.</b> The GBC group's other five STANDING rows belong to laws A.4 ({@code :526},
 * {@code :527}), A.1 ({@code :533}, {@code :544}, {@code :548}) and A.5 ({@code :515},
 * {@code :517}, {@code :521}).
 *
 * <p><b>LAW-81 tripwires this law fires in OTHER suites</b> (listed in NOTES.md, not edited
 * here): {@code BlockLambdaSingleItemChainStampSeatTest} and
 * {@code ExtractBodyMultiDefaultTernarySeatTest} each lose their {@code GetBasketConstituents}
 * row, and {@code BlockLambdaSingleArmListLiftSeatTest}'s {@code TERNARY_EXCLUSION_SITES}
 * becomes {@code List.of()} -- the two sites it pins ARE the two inline ternaries this law
 * removes.
 *
 * <p><b>LAW 82 - MEASURED by the receipts chain, run 1 at ce1a06292 (final33.status; every
 * figure below is transcribed from the chain's own logs, never from this file's earlier
 * CLAIMED paragraphs, which it supersedes).</b> GREEN 7/0F/1skip default (f33-green-default.log) /
 * 7/0F/0skip {@code -Pir-on} (f33-green-on.log); RED at the pre-seat base {@code fa49da010}: default
 * 4F = a1, corpus_c1, corpus_c2, corpus_control1; {@code -Pir-on} 5F = a1, corpus_c1, corpus_c2, corpus_control1, corpus_control2 (f33-red-{default,on}.log).
 * Mutation lanes ({@code mut33.py}, the default profile; {@code lanes33.py --summary}):
 * <ul>
 *   <li><b>{@code m-lawA3-transparency}</b> ({@code CH_PAIRS_MUT_M_LAWA3_TRANSPARENCY}): MEASURED 7/4F/1skip = a1, corpus_c1, corpus_c2, corpus_control1 - MATCH (a1, c1, c2, control1).</li>
 *   <li><b>{@code m-lawA3-ownthen}</b> ({@code CH_PAIRS_MUT_M_LAWA3_OWNTHEN}): MEASURED 7/1F/1skip = e1 - MATCH - e1 alone.</li>
 *   <li><b>{@code m-lawA3-fnscope}</b> ({@code CH_PAIRS_MUT_M_LAWA3_FNSCOPE}): MEASURED 7/0F/1skip = (none) - EMPTY-as-declared (zero rule-hosted ctor-body rows corpus-wide).</li>
 * </ul>
 */
class LadderBlockCtorNestedExtractSeatTest {

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
    // The tokens (transcribed from
    // test-corpus/drr/drr-7.0.0/rosetta-source/src/generated/java/<GBC>).
    // =========================================================================

    /** The ADDED block's rung guard. Golden 17 in the carrier, fork 11 (+3 per ctor site). */
    private static final String BLOCK_GUARD = ".getOrDefault(false)) {";
    /** The ADDED block's typed terminal. Golden 2 in the carrier, fork 0. */
    private static final String BLOCK_TERMINAL = "return MapperS.<String>ofNull();";
    /** The ADDED block's lambda head at the escaped implicit item. Golden 2, fork 0. */
    private static final String BLOCK_HEAD = ".mapSingleToItem(_item -> {";
    /** The REMOVED inline ternary. ZERO of the 174,141 goldens carry it (the #281 law). */
    private static final String INLINE_TERNARY = ".getOrDefault(false) ? ";
    /** The REMOVED ternary's empty terminal / MapperC-lifted third arm. Golden 0, fork 2. */
    private static final String TERNARY_EMPTY = "MapperC.of()";
    /** The method-selection net -- this law must NOT move it. Golden 2, fork 2. */
    private static final String LADDER_STEP = ".mapSingleToItem(";

    // =========================================================================
    // Fixtures.
    //
    // A1 is reduced from base-trade-basket-func.rosetta:51-63 (`func GetBasketConstituents`,
    // the basket arm's `BasketConstituentsReport { ... unitOfMeasure: ... }`). It keeps EVERY
    // structure the walk reads: a MULTI chain root, ONE outer `then extract` whose lambda body
    // root is a CONSTRUCTOR, a constructor FIELD whose value is a `<fn>(item) -> <feature>
    // then extract <ladder>` chain (so the inner then hoists a `thenArg` and the ancestor walk
    // reaches the outer extract with extractCount == 1), and a THREE-rung elseless ladder whose
    // arms are SINGLE-output function calls over the item's own features. It drops the meta
    // wrappers, the identifier fields and the second `add` -- none of which the walk reads.
    //
    // E1 is A1 with a `then` INSIDE one ladder arm: the scoping conjunct
    // `!subtreeCarriesThenOrSwitch(cond)` -- shared verbatim with both existing transparencies
    // -- must decline it, because an arm-interior then still renders the runtime `.then(` form
    // and its arm-registered hoists can splice between rungs (the cp1 AWAY class).
    // =========================================================================

    private static final String MODEL = """
            namespace census.seat33a3
            version "1.0.0"

            enum CapEnum:
                TONNE
                BARREL

            enum WeaEnum:
                HDD
                CDD

            enum FinEnum:
                SHARE
                CONTRACT

            type Unit:
                capacityUnit CapEnum (0..1)
                weatherUnit WeaEnum (0..1)
                financialUnit FinEnum (0..1)

            type Qty:
                unit Unit (0..1)

            type X:
                id string (0..1)
                qty Qty (0..1)

            type Rep:
                id string (0..1)
                uom string (0..1)

            func GetQty: <"the GetQuantityForConstituent stand-in: a SINGLE output whose `-> unit` nav is the ladder's then-base">
                inputs:
                    x X (0..1)
                output:
                    qty Qty (0..1)
                set qty:
                    x -> qty

            func CapToStr:
                inputs:
                    u CapEnum (0..1)
                output:
                    s string (0..1)
                set s:
                    "cap"

            func WeaToStr:
                inputs:
                    u WeaEnum (0..1)
                output:
                    s string (0..1)
                set s:
                    "wea"

            func FinToStr:
                inputs:
                    u FinEnum (0..1)
                output:
                    s string (0..1)
                set s:
                    "fin"

            func A1LadderInCtorValue: <"a1 - THE GetBasketConstituents SHAPE: a three-rung elseless ladder that is a CONSTRUCTOR FIELD's value, two extracts below the function anchor">
                inputs:
                    xs X (0..*)
                output:
                    result Rep (0..*)
                add result:
                    xs
                        then extract Rep {
                            id: item -> id,
                            uom: GetQty(item) -> unit
                                    then extract
                                        if capacityUnit exists
                                        then CapToStr(capacityUnit)
                                        else if weatherUnit exists
                                        then WeaToStr(weatherUnit)
                                        else if financialUnit exists
                                        then FinToStr(financialUnit)
                        }

            func E1LadderCtorValueWithInteriorThen: <"e1 - DECLINE: the SAME ctor-value ladder with a `then` INSIDE one arm keeps the inline ternary - the scoping law shared with both existing transparencies">
                inputs:
                    xs X (0..*)
                output:
                    result Rep (0..*)
                add result:
                    xs
                        then extract Rep {
                            id: item -> id,
                            uom: GetQty(item) -> unit
                                    then extract
                                        if capacityUnit exists
                                        then CapToStr(capacityUnit)
                                        else if weatherUnit exists
                                        then (weatherUnit then extract WeaToStr(item))
                                        else if financialUnit exists
                                        then FinToStr(financialUnit)
                        }
            """;

    // =========================================================================
    // Part A -- the seat (CLAIMED RED at the base)
    // =========================================================================

    /**
     * a1 -- the ctor-value ladder block-converts.
     *
     * <p>PIN AT RED: the first assert is a FIXTURE-REACH pin, GREEN in both states. The ladder's
     * own {@code mapSingleToItem} step must render at all; if it does not, the fixture is not
     * reaching {@code isLadderConditional}'s caller and RESHAPING is the fix, never a weaker
     * assert. Every other assert fails at the base for the LAW's own reason -- the inline
     * ternary is present and the block's guards and typed terminal are absent.
     *
     * <p>The counts are the point (a bare {@code contains} would pass on half a heal): THREE
     * rung guards and ONE typed terminal, with the ternary and its {@code MapperC.of()} empty
     * gone. The lambda parameter name is deliberately NOT asserted -- the escape is the #292
     * facet's, not this law's.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_ctorValueLadderBlockConvertsAtTheFunctionAnchor() throws IOException {
        String out = fixtureFunction("A1LadderInCtorValue");
        assertTrue(out.contains(LADDER_STEP),
                "reach: the ladder's own map step must render:\n" + out);
        assertEquals(0, count(out, INLINE_TERNARY),
                "the inline getOrDefault ternary must be gone -- ZERO of the 174,141 goldens"
                + " carry it (the #281 law):\n" + out);
        assertEquals(3, count(out, BLOCK_GUARD),
                "the block renders one if-guard per rung (base: 0):\n" + out);
        assertTrue(out.contains(BLOCK_TERMINAL),
                "the elseless ladder's block terminal is the typed ofNull (base: absent):\n"
                + out);
        assertEquals(0, count(out, TERNARY_EMPTY),
                "the ternary's MapperC empty terminal must be gone (base: 1)");
    }

    // =========================================================================
    // Part B -- the decline lock
    // =========================================================================

    /**
     * e1 -- the scoping conjunct {@code !subtreeCarriesThenOrSwitch(cond)}, shared verbatim with
     * {@code navTailTransparent} and {@code fnArgTransparent}. A ladder whose OWN subtree carries
     * a {@code then} still renders that arm as a runtime {@code .then(} lambda, and its
     * arm-registered hoists can splice BETWEEN rungs -- the cp1 {@code Create_AnnaDsbUpiRequest}
     * AWAY class (788 -&gt; 794 lines) the whole family polices. It must keep the inline ternary.
     *
     * <p>The first assert is both the reach pin and the decline evidence: if the fixture never
     * reached the ladder seat there would be no ternary either, so this lock cannot pass
     * vacuously. {@code m-lawA3-ownthen} is the lane that must move it.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void e1_ladderWithAnInteriorThenKeepsTheInlineTernary() throws IOException {
        String out = fixtureFunction("E1LadderCtorValueWithInteriorThen");
        assertTrue(count(out, INLINE_TERNARY) > 0,
                "reach + decline: an arm-interior `then` must keep the inline ternary:\n" + out);
        assertEquals(0, count(out, BLOCK_TERMINAL),
                "the transparency must decline: no block terminal at a then-carrying ladder");
        assertEquals(0, count(out, BLOCK_HEAD),
                "the transparency must decline: no block lambda head");
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
        assertEquals(2, count(g, BLOCK_HEAD), "golden renders both ctor ladders as blocks");
        assertEquals(2, count(g, BLOCK_TERMINAL), "golden gives each block a typed ofNull tail");
        assertEquals(17, count(g, BLOCK_GUARD),
                "golden carries 17 block rung guards in this file (11 outside the two ladders"
                + " + 3 per ladder)");
        assertEquals(0, count(g, INLINE_TERNARY),
                "golden carries NO inline getOrDefault ternary (the #281 law)");
        assertEquals(0, count(g, TERNARY_EMPTY), "golden carries no MapperC.of() empty");
        assertEquals(2, count(g, LADDER_STEP), "golden's two mapSingleToItem steps");
    }

    /**
     * corpus_c1 -- the carrier at drr 7.0.0. The law's own tokens, then the EXACT residue this
     * ENABLER leaves, row by row with the law that flips each.
     *
     * <p><b>WHOLE since A.2 / CHECKPOINT 1</b> -- the residue asserts below read
     * {@code assertEquals(0, ...)} on both sides (re-pinned from each sibling's print: 9/11 -&gt;
     * 5/7 at A.5, -&gt; 1/1 at A.1, -&gt; 0/0 at CHECKPOINT 1 {@code 7e94f2d16}). What follows is the
     * pin's PROVENANCE at this law's head, kept as the record.
     *
     * <p><b>The residue after A.4 + A.3</b> (derived offline against the seat-33 probe dumps and
     * pinned here; the chain re-measured): <b>9 fork-only and 11 golden-only</b> trimmed code
     * lines, all of them belonging to
     * <ul>
     *   <li>{@code A.1 ctorSetterMetaDerefFunctionHost} -- the two un-hoisted setter derefs and
     *       the wrapper-local numbering ({@code RESIDUE_A1_*});</li>
     *   <li>{@code A.5 deepThenIteLadderArmJoinMulti} -- the {@code underliers} ite-hoist local,
     *       its two lifted arms and its terminal ({@code RESIDUE_A5_DECL});</li>
     *   <li>{@code A.2 aliasCondLadderChoiceJoin} -- the escaped alias lambda name
     *       ({@code RESIDUE_A2_LAMBDA}), which takes the file WHOLE when it lands last.</li>
     * </ul>
     * Law A.4's own bytes are re-asserted here as a standing lock: they landed one commit ago
     * and this law must not disturb them.
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
        assertEquals(0, count(gen, INLINE_TERNARY),
                cell + ": both ctor ladders must leave the inline ternary (base: 6 guards"
                + " across 2 ternaries)");
        assertEquals(2, count(gen, BLOCK_HEAD), cell + ": both ladders render as blocks");
        assertEquals(2, count(gen, BLOCK_TERMINAL), cell + ": both blocks close with the typed"
                + " ofNull");
        assertEquals(17, count(gen, BLOCK_GUARD), cell + ": 11 + 3 + 3 block rung guards");
        assertEquals(0, count(gen, TERNARY_EMPTY), cell + ": no MapperC.of() empty survives");
        assertEquals(2, count(gen, LADDER_STEP),
                cell + ": the method selection must NOT move (base 2, golden 2)");

        // --- law A.4's bytes, one commit old: a standing lock ---
        assertTrue(gen.contains(LAW_A4_LOL_STEP),
                cell + ": law A.4's list-preserving step must still be here:\n" + gen);
        assertTrue(gen.contains("import java.util.Collections;"),
                cell + ": law A.4's B001 import must still be here");

        // --- the NAMED residue this ENABLER leaves (LAW 80) ---
        assertEquals(0, count(gen, RESIDUE_A1_BASKET_SETTER),
                cell + ": law A.1 rung 1's basket setter is HOISTED (flipped at A.1 - LAW 81 re-pin 1 -> 0 from this suite's own print, A1-trip1.log)");
        assertEquals(0, count(gen, RESIDUE_A1_POOL_SETTER),
                cell + ": law A.1 rung 2's pool default is HOISTED (flipped at A.1 - LAW 81 re-pin 1 -> 0 from this suite's own print, A1-trip1.log)");
        assertEquals(4, count(gen, RESIDUE_A1_WRAPPER_LOCAL),
                cell + ": all four wrapper locals render, as golden (re-pinned 2 -> 4 at A.1 from A1-trip1.log)");
        assertEquals(0, count(gen, RESIDUE_A5_DECL),
                cell + ": law A.5's ite-hoist local is still MapperS");
        assertEquals(0, count(gen, RESIDUE_A2_LAMBDA),
                cell + ": law A.2's escaped alias lambda name is GONE (flipped at A.2 - LAW 81 re-pin 1 -> 0 from the checkpoint's own gensuite print, ckpt1-gensuite.log)");
        List<String> forkOnly = onlyIn(gen, golden);
        List<String> goldenOnly = onlyIn(golden, gen);
        assertEquals(0, forkOnly.size(),
                cell + ": GetBasketConstituents is WHOLE after A.2 (re-pinned 1 -> 0 at A.2/CHECKPOINT 1, ckpt1-gensuite.log):\n" + String.join("\n", forkOnly));
        assertEquals(0, goldenOnly.size(),
                cell + ": GetBasketConstituents is WHOLE after A.2 (re-pinned 1 -> 0 at A.2/CHECKPOINT 1, ckpt1-gensuite.log):\n"
                + String.join("\n", goldenOnly));
    }

    /** Law A.4's step, re-asserted here so this law cannot silently undo the previous commit. */
    private static final String LAW_A4_LOL_STEP =
            ".mapListToList(item -> MapperC.of(Collections.singletonList(MapperS.of(";
    /** law A.1 rung 1's residue -- the basket arm's un-hoisted setter deref (golden 0). */
    private static final String RESIDUE_A1_BASKET_SETTER =
            ".setIdentifier(item.<FieldWithMetaString>map(";
    /** law A.1 rung 2's residue -- the pool arm's inline default chain (golden 0). */
    private static final String RESIDUE_A1_POOL_SETTER =
            ".setIdentifier(MapperS.of(filterAssetIdentifier";
    /** law A.1's numbering half -- golden hoists FOUR wrapper locals, the fork two. */
    private static final String RESIDUE_A1_WRAPPER_LOCAL =
            "final FieldWithMetaString fieldWithMetaString";
    /** law A.5's residue -- the ite-hoist local's Mapper kind (golden MapperC). */
    private static final String RESIDUE_A5_DECL = "final MapperS<Underlier> ifThenElseResult;";
    /** law A.2's residue -- the escaped alias lambda name (golden `underlier -> underlier...`). */
    private static final String RESIDUE_A2_LAMBDA = "_underliers -> _underliers.getObservable()";

    /**
     * control1 -- LAW 79, the whole-cell UNION scan on drr 7.0.0. Per file the (T1, T2) pair must
     * EQUAL golden's, beyond the named residue.
     * <ul>
     *   <li><b>T1</b> {@code .getOrDefault(false) ? } -- the law's REMOVED shape. <b>ZERO of the
     *       174,141 goldens carry it</b> (the #281 law), so ANY file holding one is a fork-only
     *       divergence and the token pins the ENTIRE remaining inline-ternary population of the
     *       cell, not just the carrier's.</li>
     *   <li><b>T2</b> {@code return MapperS.<} -- the block render's ADDED terminal form,
     *       deliberately GLOBAL (246 golden files in this cell), so a block emitted or lost
     *       anywhere fails here, including at seats this law does not name.</li>
     * </ul>
     *
     * <p><b>The domain is DERIVED, not sentinelled.</b> A read-only walk of the 7,808 golden
     * {@code .java} files under {@code test-corpus/drr/drr-7.0.0/rosetta-source/src/generated/
     * java} counts <b>246</b> carrying T1 or T2, of which <b>237</b> are under
     * {@code /functions/} or {@code /reports/} -- the kinds this harness emits -- and 9 are
     * {@code validation/datarule/} classes it does not, which {@code retainAll(emittedA)}
     * removes. The fork side adds TWO files of its own ({@code Price.java} T1=4 and
     * {@code TotalNotionalQuantity.java} T1=2, both golden {@code [0, 0]}), giving
     * <b>237 + 2 = 239</b>. The derivation was CALIBRATED before being trusted: the same walk
     * reproduces {@code ExtractBodyMultiDefaultTernarySeatTest}'s pinned
     * {@code GOLDEN_DOMAIN = 3579}, its {@code T1/T2/T3} totals {@code 15/697/8374} AND its
     * {@code DOMAIN_DRR700 = 1233} (= the functions+reports subset) exactly, and
     * {@code RulePathFilterLolSeatTest}'s {@code DOMAIN_DRR7 = 37}.
     *
     * <p><b>The domain MOVES when Price and TotalNotionalQuantity heal</b> (C.1/C.2 and D.3 later
     * in this seat): each leaves the union as its file reaches golden, 239 -&gt; 238 -&gt; 237.
     * Declared here so those laws' leads re-pin rather than wonder.
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
     * The NAMED residue of drr 7.0.0, sorted as {@code assertUnionEqual} prints it (a
     * {@code TreeSet} universe). At the base this control prints THREE rows; the carrier's --
     * {@code drr/base/trade/basket/functions/GetBasketConstituents.java fork=[6, 0]
     * golden=[0, 2]} -- LEAVES when this law lands, which is the control's failing-first
     * evidence at corpus grain. The two that remain are LAW-81 tripwires this law does NOT own:
     * each leaves the moment its own law lands ({@code Price} at C.1/C.2,
     * {@code TotalNotionalQuantity} at D.3). Re-pin from the fired control's own print, in that
     * law's commit.
     */
    private static final List<String> KNOWN_RESIDUE_DRR700 = List.of(
            // the Price row (fork=[4, 0] golden=[0, 0]) LEFT this list: law C.2 (heteroMetaDefaultJoinDeref + iteArmMultiDefaultTernary) took Price WHOLE in all four drr 7.x cells;
            // this suite was outside that law's LAW-81 batch list and the row was caught by the seat's
            // live-row CENSUS at B.24 (B24-trip3/4.log: `but was: <[]>`), a pure row removal.
            // the TotalNotionalQuantity row (fork=[2, 0] golden=[0, 0]) LEFT this list: law D.3 (fnDeepCondBaseConfinedArmChainAdmit, seven rungs) took TNQ WHOLE in all four drr 7.x cells;
            // this suite was outside that law's LAW-81 batch list and the row was caught by the seat's
            // live-row CENSUS at B.24 (B24-trip3/4.log: `but was: <[]>`), a pure row removal.
            );

    /** DERIVED by the read-only golden walk described on {@code corpus_control1}. */
    // LAW 81 re-pin (seat 33, law B.24's live-row census): 239 -> 237 - Price and TotalNotionalQuantity left the token-bearing
    // UNION domain (golden carries ZERO of these tokens in them and the fork now matches, so neither side
    // bears a token); the CtorSetterAttrType 444 -> 443 precedent (C.1); from this control's own print (B24-trip5.log).
    private static final int DOMAIN_DRR700 = 237;

    /**
     * control2 -- LAW 77, the IR route. {@code isCleanLadderContext} is a private static of
     * {@code CollectionHandler} and {@code IRCollectionHandler} extends it, so the route should
     * INHERIT the heal; the compare is against the DEFAULT-route text file-for-file (the seat-32
     * chain's own route-content check), plus the law's tokens on the IR text. Skips unless
     * {@code -Pir-on}.
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
        assertEquals(0, count(normalize(ir), INLINE_TERNARY),
                "the IR route must inherit the block conversion:\n" + ir);
        assertEquals(2, count(normalize(ir), BLOCK_TERMINAL),
                "the IR route must render both block terminals");
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
                    count(code, INLINE_TERNARY),
                    count(code, "return MapperS.<")};
            if (t[0] + t[1] > 0) {
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
        int[] zero = new int[2];
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
                "the (inline ternary, block terminal) pair differs beyond the named residue in "
                        + mismatched.size() + " file(s)");
        assertEquals(expectedDomain, universe.size(),
                "the union domain must equal the emitted token-bearing files (" + expectedDomain
                        + ")");
    }

    /**
     * The trimmed, blank-stripped code lines present in {@code a} more often than in {@code b}
     * (a multiset difference, order-insensitive). Used by {@code corpus_c1}/{@code corpus_c2} to
     * pin the residue an ENABLER leaves without transcribing eleven golden lines into a literal.
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
        RModel main = AstBuilder.buildFromString(MODEL, "seat33a3.rosetta");
        main.setVersion("0.0.0.test");
        List<RModel> models = new ArrayList<>();
        models.add(main);
        models.addAll(loadBuiltinsOnly());
        RWorkspace workspace = RWorkspace.build(models).workspace();
        GeneratorModel gm = new GeneratorModel(workspace,
                m -> "census.seat33a3".equals(m.namespace()));
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
            throw new AssertionError("[LadderBlockCtorNestedExtractSeatTest] builtins parse"
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
