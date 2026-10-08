// SEAT 33 LAW C.1 - facet iteChainNestedThenLadderAdmit (landed at a611f4085; the law's record is
// target/seat33-instruments/drafts33/C1/NOTES.md). Drafted before any run; every value marked
// ///PIN: was then TRANSCRIBED from the assert's own print at RED/GREEN (C1-red2 / C1-green logs), and
// the ENABLER's residue (law C.2's rung-1 `default` join) was MEASURED by the C.1-applied dry run
// (b4dry33.ps1) before landing. corpus_c1/corpus_c2 EVOLVED into whole-file byte compares when law C.2
// landed one commit later (the LAW-81 re-pin in the C.2 commit, 358046781).
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
 * SEAT 33, law C.1 - facet {@code iteChainNestedThenLadderAdmit} + rungs
 * {@code nestedElseStatementSeatCapture} (R2b), {@code mapperFormIteArmSingleWrapperDeref} (R4)
 * and {@code collapsedMetaDerefChoiceOptionFeature} (R2a): <b>the FUNCTION-path ite-chain renders
 * golden {@code Price}'s whole seven-rung ladder as STATEMENTS - a nested-THEN rung by its own
 * ladder walk one level deeper, a nested-ELSE rung whose condition needs a declaration seat as a
 * block rather than an {@code else if} header, and a SINGLE wrapper arm with its guarded value
 * deref.</b>
 *
 * <p><b>THIS LAW IS AN ENABLER: WHOLE = 0.</b> {@code Price} restructures here and goes WHOLE
 * only with law <b>C.2</b> ({@code heteroMetaDefaultJoinDeref} + {@code iteArmMultiDefaultTernary}),
 * which lands immediately after and owns the one residue this suite pins: rung 1's
 * {@code default} join, where golden renders
 * {@code <L>.getMulti().isEmpty() ? <R> : <L>} with BOTH operands deref'd and the fork renders
 * {@code <L>.getOrDefault(<R>)} with neither.
 *
 * <p><b>THE TEN RUNGS AND THE MEASUREMENT BEHIND EACH.</b>
 * <ul>
 *   <li><b>B4, nine pairs</b> (the seat-32 draft, re-validated exact-once at this head): the
 *       {@code isHoistableThenConditional} / {@code appendIteHoistChainCore} ladder admission.
 *       MEASURED (LAW 75, {@code [P32-B4]}, both routes, 33,115 rows):
 *       {@code firstFail=allowNestedThen} is 21 rows / 6 basenames; {@code fn:Price} x4 is the
 *       ONLY {@code fn:} row and the other 17 are {@code rule:}-scoped and BYTE-GREEN. Positive
 *       control non-zero: {@code verdict=true} 1,549 rows / 171 {@code where=}.</li>
 *   <li><b>THE TENTH RUNG</b> - {@code nestedThenLadder} threaded through the
 *       {@code nestedElseBoolTail} TAIL recursion. MEASURED as MISSING: the b4dry dry run applied
 *       all nine pairs at {@code fa49da010} and {@code Price}'s bytes did not move one line toward
 *       golden ({@code diff -u golden fork} = 68 lines / 3 hunks BEFORE and AFTER, all four cells
 *       byte-identical). {@code [P33-NESTELSE] --where fn:Price} printed 5 rows/cell (ONE walk,
 *       not two) and {@code [P33-DEFSEAT]} 2/cell (probe + fall-through, never 3): the walk
 *       entered and died at level 2, where the tail recursion's 13-arg overload delegates
 *       {@code nestedThenLadder = false}. Golden's carrier ladder rung sits THREE tail recursions
 *       deep. {@code a2} is this rung's own witness and {@code m-c1-b4tail} its lane.</li>
 *   <li><b>R2b</b> {@code nestedElseStatementSeatCapture}. MEASURED: the widening's UPPER bound
 *       is {@code [P33-NESTELSE] bareFn=false} = 563 rows / 28 {@code where=} (OFF and ON
 *       histograms byte-identical); intersecting those 28 with the {@code [P33-COLLDEREF]} files
 *       that REACH the wrapper gate is <b>EMPTY</b>, and the only two files in both tags at all
 *       ({@code fn:Create_AnnaDsbUpiRequestFromReportableEventAndUnderlying},
 *       {@code fn:PlatformIdentifier}) carry only {@code decline:notMeta} /
 *       {@code decline:leafNull}. Green would-fire ZERO. {@code [P33-NESTELSE] condHoistsSoFar}
 *       is 647/647 CONSTANT-0 and is cited by nothing here.</li>
 *   <li><b>R4</b> {@code mapperFormIteArmSingleWrapperDeref}. MEASURED, the cleanest number in the
 *       seat: {@code [P33-ARMFORM] multiArm=false wrapperOverItem=true} is <b>0 rows</b>
 *       corpus-wide on BOTH routes, and 4 rows / 1 {@code where=} ({@code fn:Price}) under the
 *       b4dry ladder admission - the quadrant is EMPTY until this law opens it. DISCLOSED
 *       UNDER-COUNT: the b4dry walk truncated at level 2, so 4 is a LOWER BOUND and the true
 *       count with the tenth rung is 8 (2/cell: golden's rung 4 and rung 5-inner-arm-1). The
 *       positive control is the sibling quadrant {@code multiArm=true wrapperOverItem=true} =
 *       312 rows (the landed #364 carriers).</li>
 *   <li><b>R2a</b> {@code collapsedMetaDerefChoiceOptionFeature}. MEASURED:
 *       {@code [P33-COLLDEREF] verdict=decline:featNotOnValue} is <b>4 rows / 1 {@code where=}</b>
 *       ({@code fn:Price}) - the ENTIRE {@code featNotOnValue} population corpus-wide, identical
 *       on the IR route. The widened predicate can only admit rows reading that verdict, so the
 *       green would-fire set is EMPTY by measurement. It is the SIBLING of law F.A rung (a), not
 *       the same rung: F.A owns {@code decline:noDrain} at conjunct 4, C.1 owns
 *       {@code decline:featNotOnValue} at conjunct 3; different lines, zero overlap.</li>
 * </ul>
 *
 * <p><b>CROSS-LAW PRECONDITION (verdicts33-C section 0).</b> Law <b>D.12</b> (charter row 5,
 * {@code iteProbeSessionSnapshotRestore}) is a HARD precondition. The #372 F-beta seat renders a
 * PROBE into a throwaway StringBuilder on a throwaway scope before the real render, but the
 * {@code StatementHoistSession} is a renderer FIELD, so the probe BURNS the ladder's Boolean
 * names. Measured under b4dry: {@code Price}'s {@code boolean0/1/2} became {@code boolean1/2/3} -
 * away from golden. Pre-C.1 Price never enters the quadrant so the burn is invisible; the moment
 * this law opens the gate, Price needs D.12's restore or its Booleans can never match.
 * {@code corpus_c1}'s {@code final Boolean boolean0 = isCommoditySwapFixedFloat} token is the
 * standing witness that D.12 held.
 *
 * <p><b>LAW 77 - INHERITS, no IR twin, for every rung.</b>
 * {@code grep -R "isHoistableThenConditional\|appendIteHoistChainCore\|wrapMapperFormIteArm\|
 * nestedElseBoolTail\|conditionNeedsStatementSeat" rune-ir-java/src/main/java} is EMPTY;
 * {@code IRFunctionExpressionRenderer extends FunctionExpressionRenderer} and overrides only the
 * naming/statement-shell hooks. R2a: {@code [P33-IRCOLL] feature=Asset} = 24 rows, ALL
 * {@code onlyElem=false} (0 of 24 true), so {@code IRJavaLeafEmitter.emitFieldAccess}'s
 * only-element re-wrap arm never claims Price's hop - it delegates. {@code corpus_control2}
 * measures the route rather than asserting it.
 *
 * <p><b>LAW 74 - COMPILE.</b> {@code javac33} base row <b>C11 = 4</b>:
 * {@code Seat33Pre.java:620, 621, 622 x2  incompatible types: bad type in conditional expression}
 * - sig B007, the mixed-pole ternary the hoist deletes. <b>Cite 4, never 5.</b> The C11 harness
 * reduces the ladder conditions to boolean parameters
 * ({@code pre_price_assignOutput(... boolean c0, c1, c2, PriceSchedule ifThenElseResult0,
 * ifThenElseResult1)}), so R2a's {@code getAsset()}-on-a-wrapper non-compile is <b>NOT
 * WITNESSED</b> - analytic only, and no commit message may claim it. R2b and R4 are BYTE rungs,
 * PRE 0 (their text does not exist in the fork today).
 *
 * <p><b>CLAIMED RED at the law's base head</b> (the thirteen earlier seat-33 laws applied;
 * default route, and the chain measures both): {@code a1}, {@code a2}, {@code a3}, {@code a4},
 * {@code corpus_c1}, {@code corpus_c2}, {@code corpus_control1} (Price's tuple differs).
 * <b>GREEN AT BOTH HEADS</b> (controls): {@code e1}, {@code e2}, {@code corpus_control0},
 * {@code corpus_control3}. <b>CLAIMED GREEN after the law</b>: the whole suite
 * ({@code corpus_control2} skips without {@code -Pir-on}).
 *
 * <p><b>MUTATION LANES (LAW 66/76) - CLAIMED; LAW 82 applies (the javadoc is rewritten from the
 * chain's own lane logs, and a measured-EMPTY lane against a non-empty claim is a re-scoring, not
 * a shrug).</b>
 * <table>
 *   <caption>lanes</caption>
 *   <tr><th>lane</th><th>severed</th><th>CLAIMED failing set</th></tr>
 *   <tr><td>{@code m-c1-b4whole}</td><td>the flag un-threaded at the ONE seat</td>
 *       <td>{@code a1}, {@code a2}, {@code corpus_c1}, {@code corpus_c2},
 *           {@code corpus_control1}</td></tr>
 *   <tr><td>{@code m-c1-b4tail}</td><td>THE TENTH RUNG only</td>
 *       <td>{@code a2}, {@code corpus_c1}, {@code corpus_c2}, {@code corpus_control1};
 *           {@code a1} stays GREEN - the lane that proves the tenth rung is load-bearing</td></tr>
 *   <tr><td>{@code m-c1-nestelse}</td><td>the bare-fn-only capture gate restored</td>
 *       <td>{@code a3} (the hoist emits ABOVE the decl again), {@code corpus_c1},
 *           {@code corpus_c2}, {@code corpus_control1}. Corpus-grain green movement EMPTY
 *           (declared, measured)</td></tr>
 *   <tr><td>{@code m-c1-armderef}</td><td>the {@code !multiArm} deref self-gate falsified</td>
 *       <td>{@code a4}, {@code corpus_c1}, {@code corpus_c2}, {@code corpus_control1}. Green
 *           movement EMPTY - 0 rows corpus-wide</td></tr>
 *   <tr><td>{@code m-c1-featchoice}</td><td>the choice-option disjunct</td>
 *       <td>{@code a3} (both R2a tokens), {@code corpus_c1}, {@code corpus_c2},
 *           {@code corpus_control1}. Green movement EMPTY - no green {@code where=} is in the
 *           {@code featNotOnValue} population</td></tr>
 * </table>
 *
 * <p><b>LAW 81 - this law is the first to move {@code Price}'s bytes.</b> 23 files / 30 rows carry
 * a pinned {@code Price.java} residue (21 suites / 22 rows + {@code band-off.tsv} and
 * {@code band-on.tsv}, 4 rows each). Every one is re-pinned from ITS OWN print in the same commit;
 * {@code NOTES.md} lists them by file and line.
 *
 * <p><b>LAW 82 - MEASURED by the receipts chain, run 1 at ce1a06292 (final33.status; every
 * figure below is transcribed from the chain's own logs, never from this file's earlier
 * CLAIMED paragraphs, which it supersedes).</b> GREEN 12/0F/1skip default (f33-green-default.log) /
 * 12/0F/0skip {@code -Pir-on} (f33-green-on.log); RED at the pre-seat base {@code fa49da010}: default
 * 9F = a1, a2, a3, a4, corpus_c1, corpus_c2, corpus_control0, corpus_control1, corpus_control3; {@code -Pir-on} 9F = a1, a2, a3, a4, corpus_c1, corpus_c2, corpus_control0, corpus_control1, corpus_control3 (f33-red-{default,on}.log).
 * Mutation lanes ({@code mut33.py}, the default profile; {@code lanes33.py --summary}):
 * <ul>
 *   <li><b>{@code m-lawC1-b4whole}</b> ({@code FER_PAIRS_MUT_B4WHOLE}): MEASURED 12/6F/1skip = a1, a2, a3, corpus_c1, corpus_c2, corpus_control1 - RE-SCORED: a1, a2, a3, c1, c2, control1 - a4 HELD: R4's single-wrapper-arm deref does not depend on the ladder admit, so the claim's 'a3, a4 also fall' was half right.</li>
 *   <li><b>{@code m-lawC1-b4tail}</b> ({@code FER_PAIRS_MUT_B4TAIL}): MEASURED 12/4F/1skip = a2, corpus_c1, corpus_c2, corpus_control1 - MATCH (a2, c1, c2, control1; a1 held - the tenth rung is load-bearing).</li>
 *   <li><b>{@code m-lawC1-nestelse}</b> ({@code FER_PAIRS_MUT_NESTELSE}): MEASURED 12/3F/1skip = a3, corpus_c1, corpus_c2 - RE-SCORED: a3, c1, c2 - control1 HELD: the capture gate's sever changes statement ORDER inside the ladder, not the scanned tuple.</li>
 *   <li><b>{@code m-lawC1-armderef}</b> ({@code FER_PAIRS_MUT_ARMDEREF}): MEASURED 12/4F/1skip = a4, corpus_c1, corpus_c2, corpus_control1 - MATCH (a4, c1, c2, control1).</li>
 *   <li><b>{@code m-lawC1-featchoice}</b> ({@code NH_PAIRS_MUT_FEATCHOICE}): MEASURED 12/4F/1skip = a3, corpus_c1, corpus_c2, corpus_control1 - MATCH (a3, c1, c2, control1).</li>
 * </ul>
 */
class PriceLadderEnablerSeatTest {

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

    private static final String PRICE =
            "drr/standards/iosco/cde/version1/price/functions/Price.java";

    /** Cell A = drr 7.0.0 - a carrier cell. */
    private static final Path CELL_A_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path GOLDEN_A = CELL_A_ROOT.resolve("rosetta-source/src/generated/java");
    /** Cell B = drr 7.1.0 - the SECOND carrier cell (7.2/7.3 carry the identical rows). */
    private static final Path CELL_B_ROOT = Path.of("../test-corpus/drr/drr-7.1.0");
    private static final Path GOLDEN_B = CELL_B_ROOT.resolve("rosetta-source/src/generated/java");
    /**
     * Cell C = drr 5.61.0 - a NON-carrier cell, dense in ite ladders and in #317 collapsed-meta
     * derefs. Every measured population of this law's four rungs has ZERO rows here, so its
     * whole-cell tuple must be UNMOVED beyond its own (other-family) residue.
     */
    private static final Path CELL_C_ROOT = Path.of("../test-corpus/drr/drr-5.61.0");
    private static final Path GOLDEN_C = CELL_C_ROOT.resolve("rosetta-source/src/generated/java");

    static boolean cellAAvailable() {
        return Files.isDirectory(GOLDEN_A);
    }

    static boolean cellBAvailable() {
        return Files.isDirectory(GOLDEN_B);
    }

    static boolean cellCAvailable() {
        return Files.isDirectory(GOLDEN_C);
    }

    static boolean cellAAndIrProviderAvailable() {
        return cellAAvailable() && irProviderOnClasspath();
    }

    // =========================================================================
    // The fixture - reduced from the carrier's REAL source
    // =========================================================================

    /**
     * Every function below is reduced from
     * {@code test-corpus/drr/drr-7.0.0/rosetta-source/src/main/rosetta/
     * standards-iosco-cde-version1-price-func.rosetta:73-131} ({@code func Price}, {@code set
     * price:} - a 7-rung {@code if / else if} ladder that is the BASE of a {@code then
     * only-element} chain), one rung shape per test. The meta/choice shapes are reduced from
     * {@code test-corpus/cdm/cdm-6.20.2/rosetta-source/src/main/rosetta/
     * observable-asset-type.rosetta:203-206} ({@code choice Observable: Asset / Basket / Index})
     * and {@code :212} ({@code type BasketConstituent extends Observable}) with
     * {@code :209 basketConstituent BasketConstituent (1..*) [metadata location]}.
     *
     * <p>FAITHFULNESS (the seat-32 E.3 {@code a1} lesson - a single-cardinality reduction of a
     * multi-cardinality carrier is a NON-WITNESS): every ladder below keeps MULTI arms so the
     * thenAware {@code computeRuleBody} reads MULTI and the k==0 FUNCTION MULTI SET seat is the
     * one reached; every output is {@code (0..1)} closed by {@code then only-element}, as Price's
     * is; {@code Bag.holder} keeps the {@code (0..*)} + {@code [metadata location]} pair that
     * makes the {@code only-element} collapse a {@code FieldWithMeta} wrapper; and
     * {@code Cell.leg} keeps {@code (0..1)} + {@code [metadata reference]} so a SINGLE
     * map-terminal arm compiles to a {@code ReferenceWithMeta} over the ladder's BARE join
     * element, which is R4's whole discriminator.
     */
    private static final String MODEL = """
            namespace census.seat33c1
            version "1.0.0"

            type Leg:
                code string (0..1)
                code2 string (0..1)

            type Square:
                tagText string (0..1)

            type Circle:
                radius string (0..1)

            choice Shape:
                Square
                Circle

            type Holder extends Shape:
                labelText string (0..1)

            type Bag:
                holder Holder (0..*)
                    [metadata location]
                plainHolder Holder (0..*)

            type Cell:
                leg Leg (0..1)
                    [metadata reference]

            type Root:
                legsA Leg (0..*)
                legsB Leg (0..*)
                legsC Leg (0..*)
                flag boolean (0..1)
                flag2 boolean (0..1)
                flag3 boolean (0..1)
                flag4 boolean (0..1)
                bag Bag (0..1)
                cellA Cell (0..1)
                cellB Cell (0..1)

            func IsWide: <"the bare-fn condition that captures the nested-else tail (Price rung 4's IsCommoditySwapFixedFloat)">
                inputs:
                    r Root (1..1)
                output:
                    result boolean (1..1)
                set result:
                    r -> flag2 exists

            func A1LadderBaseAdmit: <"a1 - THE CARRIER shape: a MULTI ite ladder as the base of a then-chain, rung 0 a nested then with a REAL else, rung 1 a bare-fn tail">
                inputs:
                    r Root (1..1)
                output:
                    out Leg (0..1)
                set out:
                    if r -> flag exists
                    then (if r -> flag3 exists
                        then r -> legsA
                        else r -> legsB)
                    else if IsWide(r)
                    then r -> legsC
                    else r -> legsA
                        then only-element

            func A2TailLadderRung: <"a2 - THE TENTH RUNG: the ladder rung sits INSIDE the bare-fn tail recursion, so the admission must be threaded through it">
                inputs:
                    r Root (1..1)
                output:
                    out Leg (0..1)
                set out:
                    if r -> flag exists
                    then (if r -> flag3 exists
                        then r -> legsA
                        else r -> legsB)
                    else if IsWide(r)
                    then (if r -> flag4 exists
                        then r -> legsC
                        else if r -> flag3 exists
                        then r -> legsA)
                    else r -> legsB
                        then only-element

            func A3StatementSeatElse: <"a3 - R2b + R2a: a nested-else rung whose CONDITION carries the #317 collapsed-meta only-element choice-option hop needs a statement seat">
                inputs:
                    r Root (1..1)
                output:
                    out Leg (0..1)
                set out:
                    if r -> flag exists
                    then r -> legsA
                    else if r -> bag -> holder only-element -> Square -> tagText = "x"
                    then r -> legsC
                    else r -> legsB
                        then only-element

            func A4SingleWrapperArm: <"a4 - R4: TWO SINGLE map-terminal wrapper arms over a BARE-joined decl element take the guarded numbered deref">
                inputs:
                    r Root (1..1)
                output:
                    out Leg (0..1)
                set out:
                    if r -> flag exists
                    then r -> cellA -> leg
                    else if r -> flag2 exists
                    then r -> cellB -> leg
                    else r -> legsA
                        then only-element

            func E2PlainNestedElseStaysFlat: <"e2 - R2b DECLINE LOCK: the same only-element nav on a NON-meta attribute registers no hoist, so the rung keeps flattening">
                inputs:
                    r Root (1..1)
                output:
                    out Leg (0..1)
                set out:
                    if r -> flag exists
                    then r -> legsA
                    else if r -> bag -> plainHolder only-element -> labelText = "x"
                    then r -> legsC
                    else r -> legsB
                        then only-element

            reporting rule E1RuleNestedThenRealElse from Root: <"e1 - B4 DECLINE LOCK: the same nested-then-with-a-real-else, RULE-scoped">
                extract legsA
                then only-element
                then (if item -> code exists
                    then (if item -> code2 exists
                        then item -> code
                        else item -> code2))
            """;

    // =========================================================================
    // Part A - the mechanism pins (CLAIMED failing-first)
    // =========================================================================

    ///PIN: the two indentation pins are the LAW'S OWN nesting shape at a func assignOutput seat,
    ///PIN: read off golden Price (the outer `if` at 3 tabs, the spliced inner ladder's `if` at 4,
    ///PIN: its assignment at 5). If the fixture renders at a different depth, RE-PIN FROM THE
    ///PIN: FAILURE PRINT - do not delete the assert; the nesting IS the law.
    /**
     * a1 - the ladder is HOISTED, not ternary-ised, and rung 0's nested THEN renders as a NESTED
     * {@code if / else} block INSIDE the outer rung.
     *
     * <p>Pre-law the whole chain declines at {@code isHoistableThenConditional} (rung 0's inner
     * else is a real navigation, which the #385 cap refuses), the base compiles to the inline
     * ternary and the decl carries an {@code =}; post-law the decl is BLOCK-form and the
     * assignments are statements. The FIRST assert is the law's own token.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_functionLadderWithNestedThenRealElseHoistsAsStatements() throws IOException {
        String out = normalize(fn("A1LadderBaseAdmit.java"));
        assertContains(out, "final MapperC<Leg> thenArg;");
        assertTrue(!out.contains("final MapperC<Leg> thenArg = "),
                "the inline-ternary decl must be gone:\n" + out);
        assertTrue(!out.contains("ifThenElseResult"),
                "no item-typed ifThenElseResult hoist survives the ladder admission:\n" + out);
        assertContains(out, "\n\t\t\t\tif (");
        assertContains(out, "\n\t\t\t\t\tthenArg = ");
        assertTrue(count(out, "thenArg = ") >= 4,
                "the ladder must assign in at least four branches:\n" + out);
        assertTrue(!out.contains("__S33_NESTED_THEN_LADDER__"),
                "the ladder placeholder must be substituted for the real local:\n" + out);
    }

    /**
     * a2 - THE TENTH RUNG. The ladder rung is INSIDE the bare-fn nested-else tail, so the
     * admission must survive the {@code nestedElseBoolTail} recursion. This is the test the b4dry
     * dry run proved the seat-32 nine pairs cannot pass: they enter the core and die at the level
     * where the tail recursion delegates {@code nestedThenLadder = false}.
     *
     * <p>The inner nesting is an ELSELESS {@code else if} ladder, which the fixed 4-element #385
     * level cannot express and only the RECURSION can - so {@code MapperC.<Leg>ofNull();} (the
     * inner ladder's typed terminal) is the shape assert that says the rung went through it.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_tailRecursionCarriesTheLadderAdmission() throws IOException {
        String out = normalize(fn("A2TailLadderRung.java"));
        assertContains(out, "final MapperC<Leg> thenArg;");
        assertTrue(!out.contains("final MapperC<Leg> thenArg = "),
                "the inline-ternary decl must be gone:\n" + out);
        assertContains(out, "final Boolean ");
        assertContains(out, "} else if (");
        assertContains(out, "MapperC.<Leg>ofNull();");
        assertTrue(count(out, "thenArg = ") >= 5,
                "rung 0's two arms + the tail's inner three-arm ladder:\n" + out);
        assertTrue(!out.contains("__S33_NESTED_THEN_LADDER__"), out);
    }

    ///PIN: the `} else {` + hoist token below carries the fixture's own indentation (the else at
    ///PIN: 3 tabs, the hoisted decl at 4 - golden Price's shape). RE-PIN FROM THE PRINT if the
    ///PIN: fixture renders at another depth; the ORDER assert beneath it is indentation-free and
    ///PIN: is the law's real statement.
    /**
     * a3 - R2a + R2b together, and they are separable by their asserts.
     *
     * <p>R2a: the {@code <meta attr> only-element -> <choice option>} hop DEREFS through the #317
     * guarded reconstruct instead of navigating a value getter on the wrapper. Pre-law
     * {@code collapsedMetaDerefRewrapOrNull} declines at conjunct 3 ({@code featNotOnValue}),
     * because {@code findAttributeOnDataType} walks {@code superType()} and a
     * {@code type Holder extends Shape} edge lives on {@code choiceSuperType()}.
     *
     * <p>R2b: that hoist cannot live in an {@code } else if (...)} header, so golden gives the
     * rung a STATEMENT seat - the decl sits INSIDE the else, BELOW the ite decl. Under lane
     * {@code m-c1-nestelse} the deref still fires but its decl drains to {@code condHoistDecls}
     * and emits ABOVE the ite decl, which is exactly what the order assert catches.
     *
     * <p>The two PRECONDITION asserts come first and hold at BOTH heads (this fixture's ladder
     * has no nested THEN, so it reaches the k==0 quadrant before the law as well as after): if
     * either fails, the fixture stopped reaching the seat and must be RESHAPED, never weakened.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a3_nestedElseWithACollapsedMetaConditionTakesAStatementSeat() throws IOException {
        String out = normalize(fn("A3StatementSeatElse.java"));
        assertContains(out, "final MapperC<Leg> thenArg;");
        assertContains(out, "getHolder");
        assertContains(out, "final FieldWithMetaHolder fieldWithMetaHolder = ");
        assertContains(out, "(fieldWithMetaHolder == null ? MapperS.<Holder>ofNull()"
                + " : MapperS.of(fieldWithMetaHolder.getValue())).<Square>map(\"getSquare\", ");
        assertContains(out, "} else {\n\t\t\t\tfinal FieldWithMetaHolder ");
        int declAt = out.indexOf("final MapperC<Leg> thenArg;");
        int hoistAt = out.indexOf("final FieldWithMetaHolder ");
        assertTrue(declAt >= 0 && hoistAt > declAt,
                "the collapsed-meta decl must sit INSIDE the else block, BELOW the ite decl"
                        + " (decl@" + declAt + " hoist@" + hoistAt + "):\n" + out);
    }

    /**
     * a4 - R4. TWO SINGLE ({@code map}-terminal) arms whose compiled item is a
     * {@code ReferenceWithMetaLeg} over the ladder's BARE {@code Leg} join element take the
     * GUARDED NUMBERED coercion deref; the MULTI else arm stays verbatim. The pair numbers
     * {@code 0}/{@code 1} because the coercion group has two members - exactly golden Price's
     * {@code referenceWithMetaPriceSchedule0} / {@code ...1}.
     *
     * <p>Pre-law the {@code !multiArm} tail of {@code wrapMapperFormIteArm} returns the bare
     * {@code MapperC.of(<chain>)} with no deref, so the first assert fails on R4's own token.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a4_singleMapperFormArmDerefsItsWrapper() throws IOException {
        String out = normalize(fn("A4SingleWrapperArm.java"));
        assertContains(out, "final MapperC<Leg> thenArg;");
        assertContains(out, ".<Leg>map(\"Type coercion\", referenceWithMetaLeg0 ->"
                + " referenceWithMetaLeg0 == null ? null : referenceWithMetaLeg0.getValue())");
        assertContains(out, ".<Leg>map(\"Type coercion\", referenceWithMetaLeg1 ->"
                + " referenceWithMetaLeg1 == null ? null : referenceWithMetaLeg1.getValue())");
        assertEquals(2, count(out, "MapperC.of("),
                "exactly the two SINGLE wrapper arms lift item->list; the MULTI else arm stays"
                        + " verbatim:\n" + out);
    }

    /**
     * e1 - the B4 DECLINE LOCK, RE-SCORED AT RED (C1-red2.log). The draft expected the rule path
     * to render "today's inline ternary" - MEASURED, it does not: the RULE-path nested-then
     * real-else ladder ALREADY renders the ite hoist ({@code final MapperS<String>
     * ifThenElseResult;} + a nested if/else inside the outer branch) through the rule path's own
     * #257/#351 machinery, so {@code .getOrDefault(false) ? } never existed pre-law and the
     * drafted assert was a non-witness. What the lock must hold is what it measured: the rule
     * path keeps ITS shape and never gains the FUNCTION-path statement-ladder tokens this law
     * mints (the blank-final {@code final MapperC<Leg> thenArg;} and the typed
     * {@code MapperC.<Leg>ofNull()} terminal) - the flag is threaded from one FUNCTION-path seat
     * and both widened branches re-ask {@code findEnclosingRule(cond) == null}. GREEN at both
     * heads, now for the measured reason.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void e1_ruleScopedNestedThenRealElseIsLeftAlone() throws IOException {
        String out = normalize(fixtureRule("E1RuleNestedThenRealElseRule"));
        assertContains(out, "final MapperS<String> ifThenElseResult;");
        assertTrue(!out.contains("final MapperC<Leg> thenArg;"),
                "a rule-scoped nested-then must NOT take the FUNCTION-path statement ladder:\n" + out);
        assertTrue(!out.contains("MapperC.<Leg>ofNull()"),
                "a rule-scoped nested-then must NOT mint the ladder's typed terminal:\n" + out);
        assertTrue(!out.contains("__S33_NESTED_THEN_LADDER__"), out);
    }

    /**
     * e2 - the R2b DECLINE LOCK, and it is a lock on the PREDICATE, not on the seat: the fixture
     * is the same {@code only-element} nav at the same threaded seat, on a NON-meta attribute. The
     * {@code metaWrapperOf} conjunct declines, so no declaration is registered, so the rung has no
     * reason to take a statement seat and must keep flattening to {@code } else if (...)}. GREEN
     * at both heads; the lane that would break it is a widening on "any hoist", which this law
     * deliberately does not do.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void e2_plainOnlyElementConditionKeepsTheElseIfRung() throws IOException {
        String out = normalize(fn("E2PlainNestedElseStaysFlat.java"));
        assertContains(out, "final MapperC<Leg> thenArg;");
        assertContains(out, "} else if (");
        assertTrue(!out.contains("final FieldWithMeta"),
                "a non-meta only-element nav registers no wrapper hoist:\n" + out);
        assertTrue(!out.contains("MapperS.<Holder>ofNull()"),
                "the #317 guarded reconstruct must not fire on a meta-free leaf:\n" + out);
    }

    // =========================================================================
    // Part B - the corpus residue pins and the whole-cell controls
    // =========================================================================

    ///PIN: DERIVED by reading golden `Price.java` in drr 7.0.0 whole (lines 112-155 of the
    ///PIN: p33-off golden dump). Each token is owned by a named rung - see NOTES.md section
    ///PIN: "the hunk attribution".
    private static final List<String> C1_OWNED_TOKENS = List.of(
            // B4 + the tenth rung: the block-form decl and the ladder's typed terminals
            "final MapperC<PriceSchedule> thenArg;",
            "thenArg = MapperC.<PriceSchedule>ofNull();",
            "import java.util.Collections;",
            "thenArg = MapperC.of(Collections.singletonList("
                    + "priceOfZeroCouponSwaps.evaluate(reportableEvent)));",
            // D.12's restore (the precondition witness - the probe must not have burned a name)
            "final Boolean boolean0 = isCommoditySwapFixedFloat.evaluate(",
            // R2a: the guarded reconstruct and the import its witness seeds
            "import cdm.observable.asset.BasketConstituent;",
            "(fieldWithMetaBasketConstituent == null ? MapperS.<BasketConstituent>ofNull()"
                    + " : MapperS.of(fieldWithMetaBasketConstituent.getValue()))"
                    + ".<Asset>map(\"getAsset\", basketConstituent -> basketConstituent.getAsset())",
            // R2b: the statement seat
            "} else {\n\t\t\t\tfinal FieldWithMetaBasketConstituent fieldWithMetaBasketConstituent = ",
            // R4: the two guarded numbered arm derefs
            ".<PriceSchedule>map(\"Type coercion\", referenceWithMetaPriceSchedule0 ->"
                    + " referenceWithMetaPriceSchedule0 == null ? null :"
                    + " referenceWithMetaPriceSchedule0.getValue())",
            ".<PriceSchedule>map(\"Type coercion\", referenceWithMetaPriceSchedule1 ->"
                    + " referenceWithMetaPriceSchedule1 == null ? null :"
                    + " referenceWithMetaPriceSchedule1.getValue())");

    ///PIN: DERIVED by reading the fork's `Price.java` in the p33-off dump: the two item-typed
    ///PIN: hoists and the inline ternary the ladder admission deletes.
    private static final List<String> C1_FORK_TOKENS = List.of(
            "final PriceSchedule ifThenElseResult0;",
            "final PriceSchedule ifThenElseResult1;",
            "final MapperC<PriceSchedule> thenArg = ComparisonResult.ofNullSafe(",
            "__S33_NESTED_THEN_LADDER__");

    ///PIN: (RETIRED at law C.2 - kept as the record of this law's head; the compare below is whole-file now.)
    ///PIN: PRINT-FIRST SENTINEL. The line-multiset residue C.1 LEAVES on Price cannot be derived
    ///PIN: without running the generator; the lead measures it with the C.1-applied dry run
    ///PIN: (b4dry33.ps1 pointed at drafts33/C1) BEFORE landing, and transcribes the number the
    ///PIN: sentinel assert prints. Reader C's hunk attribution says what it must be: the rung-1
    ///PIN: `default` join and nothing else (hunks 2/3/6/7 + parts of 4/5 -> B4 + the tenth rung,
    ///PIN: hunk 1 + part of 4 -> R2a, the `} else {` -> R2b, parts of 5/6 -> R4; what remains is
    ///PIN: R3a's two derefs and R3b's ternary form).
    private static final int PRICE_RESIDUE_LINES = 0;   // RETIRED at law C.2 (was 4 = the two-line default join, both sides; Price is whole - see assertPriceResidue)

    /**
     * corpus_c1 - the ENABLER's pin for {@code Price} in drr 7.0.0. At this law's head it was NOT
     * a whole-file byte compare (this law is declared PARTIAL and law <b>C.2</b> flips the last
     * hunk); since C.2 landed (LAW 81 re-pin, {@code C2-trip1.log}) it IS one - see the note at
     * the compare below.
     *
     * <p>Three things are asserted, in this order: every token C.1 OWNS is present; every fork
     * token it DELETES is gone; and the remaining line-multiset difference against golden is
     * EMPTY (at this law's head it was exactly rung 1's {@code default} join, C.2's business and
     * nothing else - the retired classifier and {@code PRICE_RESIDUE_LINES} above are that
     * head's record).
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_drr700PriceResidueIsExactlyTheDefaultJoin() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");
        List<String> own = drrAGenErrors.stream().filter(e -> e.contains(PRICE)).toList();
        assertTrue(own.isEmpty(), "generation errors for " + PRICE + ": " + own);
        String gen = normalize(drrAOutput.get(PRICE));
        assertNotNull(gen, "not generated: " + PRICE);
        assertPriceResidue(gen, normalize(Files.readString(GOLDEN_A.resolve(PRICE))));
    }

    /** corpus_c2 - the same residue pin in drr 7.1.0 (7.2/7.3 carry the identical rows). */
    @Test
    @EnabledIf("cellBAvailable")
    void corpus_c2_drr710PriceResidueIsExactlyTheDefaultJoin() throws IOException {
        assertNotNull(drrBOutput, "drr 7.1.0 generation did not run");
        String gen = normalize(drrBOutput.get(PRICE));
        assertNotNull(gen, "not generated: " + PRICE);
        assertPriceResidue(gen, normalize(Files.readString(GOLDEN_B.resolve(PRICE))));
    }

    private static void assertPriceResidue(String gen, String golden) {
        for (String token : C1_OWNED_TOKENS) {
            assertTrue(gen.contains(token),
                    "law C.1 owns this golden token and it is missing:\n  " + token + "\nin:\n" + gen);
        }
        for (String token : C1_FORK_TOKENS) {
            assertTrue(!gen.contains(token),
                    "law C.1 deletes this fork token and it survives:\n  " + token + "\nin:\n" + gen);
        }
        // LAW 81 re-pin (v3.1 flip seat 33, law C.2 / C2-trip1.log): law C.2 landed and took the
        // rung-1 default join - the ONE residue this enabler left - so Price is WHOLE and this
        // test is now a whole-file byte compare, which its javadoc at this law's head explicitly
        // said it was NOT yet (the A.1 -> A.2 precedent: the enabler's pin evolves when the
        // closer lands). The enabler's own-token asserts above still lock every rung of C.1; the
        // former residue classifier (the two-line join tokens) and PRICE_RESIDUE_LINES = 4 were
        // the measured pre-C.2 truth and are retired with it.
        List<String> residue = lineResidue(golden, gen);
        assertEquals(List.of(), residue,
                "Price must be WHOLE after C.2 (the default join was its last residue): " + residue);
        assertEquals(golden, gen, "the whole-file byte compare");
    }

    ///PIN: the seven files below are the law's MEASURED blast radius outside the carrier, and
    ///PIN: every one must be BYTE-IDENTICAL to golden at BOTH heads.
    ///PIN:  * Create_AnnaDsbUpiRequestFromReportableEventAndUnderlying - the ONLY GREEN file that
    ///PIN:    enters the nested-else tail recursion at all ([P33-NESTELSE] taken=true, 40 rows,
    ///PIN:    identical on ON): the tenth rung's entire measured blast radius.
    ///PIN:  * StrikePrice - the OTHER file in that entrant set (40 rows). It is a band file that
    ///PIN:    law D.12 takes WHOLE ten commits earlier, so it is byte-clean at this law's base
    ///PIN:    head and this control says the tenth rung leaves it that way.
    ///PIN:  * the five rule: files carrying the 17 measured green `firstFail=allowNestedThen`
    ///PIN:    rows, which ride the NARROW #385 allowNestedThen branch this law must not widen.
    private static final List<String> CELL_A_UNMOVED = List.of(
            "drr/enrichment/upi/functions/Create_AnnaDsbUpiRequestFromReportableEventAndUnderlying.java",
            "drr/standards/iosco/cde/version1/price/functions/StrikePrice.java",
            "drr/regulation/csa/rewrite/trade/reports/CryptoAssetUnderlyingIndicatorLeg1Rule.java",
            "drr/regulation/csa/rewrite/trade/reports/CryptoAssetUnderlyingIndicatorLeg2Rule.java",
            "drr/regulation/csa/rewrite/trade/reports/UnderlyingAssetPriceSourceLeg1Rule.java",
            "drr/regulation/csa/rewrite/trade/reports/UnderlyingAssetPriceSourceLeg2Rule.java",
            "drr/regulation/common/trade/execution/functions/PlatformIdentifier.java");

    /**
     * corpus_control0 - the named-file no-move control (verdicts33-C section 6, items 2 and 3).
     * GREEN at BOTH heads by construction; it fails the instant the tenth rung or the widened
     * #385 branch reaches a file it must not.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control0_theTailRecursionEntrantsAndTheGreenRuleRowsStayWhole() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");
        for (String path : CELL_A_UNMOVED) {
            String gen = drrAOutput.get(path);
            assertNotNull(gen, "not generated: " + path);
            assertEquals(normalize(Files.readString(GOLDEN_A.resolve(path))), normalize(gen),
                    "this file must stay byte-identical to golden across law C.1: " + path);
        }
    }

    /**
     * corpus_control1 - LAW 79, the whole-cell UNION scan on drr 7.0.0. The domain is every file
     * whose GOLDEN <b>or</b> FORK text carries one of six ladder-form tokens; per file the tuple
     * must equal golden's beyond the NAMED residue.
     *
     * <p>The six counters are scanned INDEPENDENTLY on purpose - this law turns assigned ite
     * locals into block-form ones (T1/T2), inline ternaries into ladder terminals (T3/T4), adds
     * #317 guarded reconstructs (T5) and adds guarded numbered coercion derefs (T6), so a control
     * that derived one from another would be blind to exactly the half that regressed. It fails
     * when the law OVER-fires (a green file's form moves) AND when it UNDER-fires (Price's row
     * does not move).
     *
     * <p>{@code GOLDEN_DOMAIN_DRR700} is an instrument-integrity pin DERIVED BY A READ-ONLY WALK:
     * {@code python target/seat33-instruments/drafts33/C1/derive-domain.py} from the repo root,
     * which mirrors {@code scan()} exactly and printed {@code token-bearing=2305 total=7808}.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control1_forkDrr700WholeCellLadderFormsEqualGoldenFileByFile() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");
        assertEquals(List.of(), drrAGenErrors,
                "drr 7.0.0 reported a generation error - the scan is incomplete");
        Map<String, int[]> golden = scan(readGoldenTree(GOLDEN_A));
        assertEquals(GOLDEN_DOMAIN_DRR700, golden.size(),
                "the GOLDEN-side token-bearing file count is the instrument's own integrity pin"
                        + " (derive-domain.py)");
        assertUnionEqual(scan(drrAOutput), golden, drrAOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_DRR700, UNION_DOMAIN_DRR700);
    }

    /**
     * corpus_control2 - LAW 77 route parity for the carrier. Every seat this law touches is
     * inherited by {@code rune-ir-java}, so the IR route must render the same bytes as the legacy
     * route for {@code Price}. Skips without {@code -Pir-on}.
     */
    @Test
    @EnabledIf("cellAAndIrProviderAvailable")
    void corpus_control2_irRouteMatchesLegacyForPrice() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 legacy-route generation did not run");
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", CELL_A_ROOT),
                new ArrayList<>());
        assertEquals(drrAOutput.get(PRICE), irOut.get(PRICE), "route divergence: " + PRICE);
    }

    /**
     * corpus_control3 - LAW 79 in a NON-carrier cell (drr 5.61.0), the same six-tuple. Every
     * measured population of this law's four rungs has ZERO rows in this cell, so
     * {@code KNOWN_RESIDUE_DRR561} must be UNMOVED - a change here is an over-fire, not a re-pin.
     */
    @Test
    @EnabledIf("cellCAvailable")
    void corpus_control3_forkDrr561WholeCellLadderFormsEqualGoldenFileByFile() throws IOException {
        assertNotNull(drrCOutput, "drr 5.61.0 generation did not run");
        assertEquals(List.of(), drrCGenErrors,
                "drr 5.61.0 reported a generation error - the scan is incomplete");
        Map<String, int[]> golden = scan(readGoldenTree(GOLDEN_C));
        assertEquals(GOLDEN_DOMAIN_DRR561, golden.size(),
                "the GOLDEN-side token-bearing file count (derive-domain.py)");
        assertUnionEqual(scan(drrCOutput), golden, drrCOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_DRR561, UNION_DOMAIN_DRR561);
    }

    ///PIN: PRINT-FIRST SENTINELS - `List.of("PIN-AT-RED")` is not a residue, it is a marker that
    ///PIN: makes the union assert fail with the MEASURED list in its message. The residue cannot
    ///PIN: be derived here: THIRTEEN earlier seat-33 laws land before this one and the band at
    ///PIN: this head is 8 files (Price x4 + QuantityUnitOfMeasure x4), all in the drr 7.x cells,
    ///PIN: with every other file byte-identical to golden. So cell A's list can only name
    ///PIN: Price.java and QuantityUnitOfMeasure.java, and cell C's can only be EMPTY. The lead
    ///PIN: transcribes both from the control's own RED print, then re-runs and confirms that the
    ///PIN: ONLY row that moved is Price's.
    private static final List<String> KNOWN_RESIDUE_DRR700 = List.of(
            // TRANSCRIBED AT RED (C1-red1.log, the pre-law head 0f38a214b) - the print-first sentinel; Price is
            // re-pinned again at GREEN with the post-law tuple, QUOM stays (C.1 does not touch it).
            // the Price row (fork=[2, 1, 4, 0, 0, 10] golden=[1, 0, 0, 2, 1, 12]) LEFT this list at GREEN
            // (C1-green1.log): law C.1 took the ladder-form tuple to golden's - the ONLY row that moved,
            // as the RED pin predicted; the file stays BANDED on C.2's default join.
            // the QuantityUnitOfMeasure row (fork=[0, 11, 0, 0, 0, 5] golden=[0, 10, 0, 0, 0, 7]) LEFT this list: law B.24 (defaultJoinHeteroMetaDerefBoth +
            // iteArmMetaCollapseDerefSinkChannel + three in-seat rungs) healed the file WHOLE in all four drr 7.x
            // cells - the band's last four files; transcribed from this control's own print (B24-trip1.log),
            // a pure row removal (was == expected minus it).
            );
            // LAW 81 re-pin (seat 33, law C.2): fork [0,10,0,0,0,4] -> [0,11,0,0,0,5] - R3a moved QUOM (LAW 80 IMPROVED-not-whole, planned; closes at B.24); from C2-trip1.log.
    private static final List<String> KNOWN_RESIDUE_DRR561 = List.of();   // TRANSCRIBED AT RED (C1-red1.log): drr 5.61.0 carries no ladder-form residue, as bounded

    ///PIN: MEASURED by a READ-ONLY WALK over the golden trees (derive-domain.py, mirroring scan()
    ///PIN: exactly): drr 7.0.0 = 2305 token-bearing of 7808 .java goldens; drr 5.61.0 = 1701 of
    ///PIN: 5249. The goldens are frozen, so these should never move.
    private static final int GOLDEN_DOMAIN_DRR700 = 2305;
    private static final int GOLDEN_DOMAIN_DRR561 = 1701;

    ///PIN: PRINT-FIRST SENTINEL (the seat-30 discipline). The UNION domain is golden-side UNION
    ///PIN: fork-side, RESTRICTED to what the suite actually emits - it cannot be derived from the
    ///PIN: golden tree alone (the suite emits rules/reports/functions, not model POJOs). -1 makes
    ///PIN: the domain assert fail with "MEASURED domain=N" in its own message; transcribe N here.
    private static final int UNION_DOMAIN_DRR700 = 1890;   // TRANSCRIBED from the control's own print (C1-red2.log)
    private static final int UNION_DOMAIN_DRR561 = 1335;   // TRANSCRIBED from the control's own print (C1-red2.log)

    /**
     * (T1..T6) per file: BLOCK-form ite locals (a {@code final ...} line naming {@code thenArg} or
     * {@code ifThenElseResult} with NO {@code =}), ASSIGNED ite locals (the same line WITH an
     * {@code =} - the inline-ternary form), {@code .getOrDefault(false) ? } inline-ternary sites,
     * {@code >ofNull();} ladder terminals, {@code == null ? MapperS.<} #317 guarded reconstructs,
     * and {@code  == null ? null : } guarded numbered coercion derefs.
     */
    private static Map<String, int[]> scan(Map<String, String> tree) {
        Map<String, int[]> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : tree.entrySet()) {
            int[] t = new int[6];
            for (String line : normalize(e.getValue()).split("\n")) {
                String s = line.trim();
                if (s.startsWith("final ")
                        && (s.contains(" thenArg") || s.contains(" ifThenElseResult"))) {
                    if (s.contains(" = ")) {
                        t[1]++;
                    } else if (s.endsWith(";")) {
                        t[0]++;
                    }
                }
                t[2] += count(s, ".getOrDefault(false) ? ");
                t[3] += count(s, ">ofNull();");
                t[4] += count(s, "== null ? MapperS.<");
                t[5] += count(s, " == null ? null : ");
            }
            if (t[0] + t[1] + t[2] + t[3] + t[4] + t[5] > 0) {
                out.put(e.getKey(), t);
            }
        }
        return out;
    }

    private static int count(String s, String needle) {
        int n = 0;
        int from = 0;
        while ((from = s.indexOf(needle, from)) >= 0) {
            n++;
            from += needle.length();
        }
        return n;
    }

    /**
     * The order-insensitive LINE MULTISET difference between two renders: every golden line the
     * fork does not carry (prefixed {@code - }) and every fork line golden does not carry
     * ({@code + }). Order-insensitive on purpose - a residue pin must see a line that MOVED as
     * well as one that changed, and a whole-file restructure moves many.
     */
    private static List<String> lineResidue(String golden, String gen) {
        List<String> out = new ArrayList<>();
        List<String> goldenLines = List.of(golden.split("\n", -1));
        List<String> genLines = List.of(gen.split("\n", -1));
        List<String> pool = new ArrayList<>(genLines);
        for (String line : goldenLines) {
            if (!pool.remove(line)) {
                out.add("- " + line);
            }
        }
        pool = new ArrayList<>(goldenLines);
        for (String line : genLines) {
            if (!pool.remove(line)) {
                out.add("+ " + line);
            }
        }
        return out;
    }

    // =========================================================================
    // The union assert (LAW 73: pin the SET, not the count) - the seat-30/31/32 shape
    // =========================================================================

    private static void assertUnionEqual(Map<String, int[]> a, Map<String, int[]> b,
            java.util.Set<String> emittedA, String aName, String bName, List<String> knownResidue,
            int expectedDomain) {
        List<String> mismatched = new ArrayList<>();
        java.util.Set<String> universe = new java.util.TreeSet<>(a.keySet());
        universe.addAll(b.keySet());
        universe.retainAll(emittedA);
        int[] zero = new int[6];
        for (String key : universe) {
            int[] ac = a.getOrDefault(key, zero);
            int[] bc = b.getOrDefault(key, zero);
            if (!java.util.Arrays.equals(ac, bc)) {
                mismatched.add(key + " " + aName + "=" + java.util.Arrays.toString(ac)
                        + " " + bName + "=" + java.util.Arrays.toString(bc));
            }
        }
        assertEquals(knownResidue, mismatched,
                "the ladder-form tuple differs beyond the named residue in " + mismatched.size()
                        + " file(s)");
        // The domain-pin flow (LAW 73): the sentinel failure PRINTS the measured value so the pin
        // is transcribed from this assert's own output, never invented.
        assertTrue(expectedDomain >= 0,
                "the union domain is MEASURED and pinned (LAW 73) - transcribe from this print:"
                        + " MEASURED domain=" + universe.size());
        assertEquals(expectedDomain, universe.size(),
                "the union domain must equal the emitted token-bearing files (" + expectedDomain
                        + ")");
    }

    // =========================================================================
    // Harness - the seat-30/31/32 suite shape
    // =========================================================================

    private static Map<String, String> drrAOutput;
    private static List<String> drrAGenErrors;
    private static Map<String, String> drrBOutput;
    private static List<String> drrBGenErrors;
    private static Map<String, String> drrCOutput;
    private static List<String> drrCGenErrors;

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
        if (cellCAvailable()) {
            List<String> errs = new ArrayList<>();
            drrCOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "5.61.0", CELL_C_ROOT), errs);
            drrCGenErrors = errs;
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

    private static String normalize(String s) {
        return s.replace("\r\n", "\n");
    }

    private static void assertContains(String out, String token) {
        assertTrue(out.contains(token), "expected token missing:\n  " + token + "\nin:\n" + out);
    }

    // =========================================================================
    // Fixture harness (the seat-30 fn()/fixture() shape)
    // =========================================================================

    private static RLinkingResult linking;
    private static RModel mainModel;
    private static Map<String, String> fixtureOut;

    private static void link() throws IOException {
        if (linking == null) {
            RModel main = AstBuilder.buildFromString(MODEL, "seat33c1.rosetta");
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
            throw new AssertionError("fixture generation errors (a broken fixture must fail"
                    + " loudly, not skip): " + errors);
        }
        return out;
    }

    private static Map<String, String> fixture() throws IOException {
        if (fixtureOut == null) {
            fixtureOut = render(m -> "census.seat33c1".equals(m.namespace()));
        }
        return fixtureOut;
    }

    private static String fn(String fileName) throws IOException {
        return lookup(fixture(), "functions/" + fileName);
    }

    private static String fixtureRule(String ruleName) throws IOException {
        return lookup(fixture(), ruleName + ".java");
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
            throw new AssertionError("[PriceLadderEnablerSeatTest] builtins parse failures: "
                    + String.join("; ", failures));
        }
        return models;
    }
}
