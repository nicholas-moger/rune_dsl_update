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
 * SEAT 33, law B.3 -- facet {@code bareRuleRefFunctionHost} (census F22; the seat-32 G.1 draft,
 * re-measured): <b>a no-arg bare {@code RRule} reference sitting INSIDE an extract/filter/then
 * lambda renders the injected rule invocation in a FUNCTION host too. In a lambda the receiver
 * argument IS the lambda binding, so the host test is "a rule OR a function encloses this", not
 * "a rule encloses this".</b>
 *
 * <p><b>The carrier</b> (drr 7.x {@code standards-iosco-cde-version1-quantity-func.rosetta:338-359},
 * {@code func QuantityUnitOfMeasure}, four cells) references the reporting rule
 * {@code quantity.UnitOfMeasureFromQuantity} bare at THREE seats inside one function body:
 * <pre>
 * then default (payout -&gt; tradeLot -&gt; priceQuantity -&gt; quantity
 *         then filter unit -&gt; financialUnit exists
 *         then filter quantity.UnitOfMeasureFromQuantity exists   &lt;-- seat 1, LAMBDA_CHANNEL
 *         then only-element
 *         )
 * then default if (payout -&gt; tradeLot -&gt; priceQuantity -&gt; quantity
 *             extract quantity.UnitOfMeasureFromQuantity          &lt;-- seat 2, BLOCK
 *             then distinct count = 1)
 *         then ...
 * then extract quantity.UnitOfMeasureFromQuantity                 &lt;-- seat 3, BLOCK
 * </pre>
 *
 * <p><b>Golden vs fork</b> (drr 7.0.0 dump, {@code golden:65-66} / {@code fork:62}):
 * <pre>
 * golden: final FieldWithMetaNonNegativeQuantitySchedule fieldWithMetaNonNegativeQuantitySchedule = item.get();
 *         return exists(MapperS.of(unitOfMeasureFromQuantityRule.evaluate(
 *                 (fieldWithMetaNonNegativeQuantitySchedule == null ? null
 *                         : fieldWithMetaNonNegativeQuantitySchedule.getValue())))).get();
 * fork:   return exists(MapperS.of(quantity.UnitOfMeasureFromQuantity)).get();
 * </pre>
 * A namespace-qualified RUNE name spliced as a Java identifier. The fork's OWN output already
 * carries {@code @Inject protected UnitOfMeasureFromQuantityRule unitOfMeasureFromQuantityRule;}
 * (fork:34, byte-identical to golden:34) -- PR #332 landed two of its three halves; this is the
 * third, and the gate's own justification comment (which cited a
 * {@code FunctionDependencyCollector} rule-host gate #332 had already dropped) is corrected in the
 * same commit (SDLC Rule 2).
 *
 * <p><b>MEASURED radius ZERO, BOTH routes</b> ({@code [P33-RULEARG]}, probe round 2 at the
 * seat-33 base head {@code fa49da010}; control TRUSTED at 14,335 admitting rows OFF / 171 ON):
 * {@code verdict=decline encRule=- inLambda=true} is <b>24 rows and ONE {@code where=}</b>
 * ({@code fn:QuantityUnitOfMeasure}), present and identical row-for-row on the IR route -- route
 * safety MEASURED here, not merely inherited. The argument deref comes FREE at every seat
 * ({@code derefWouldFire=true} 24/24: 16 {@code route=BLOCK RExtractExpr} +
 * 8 {@code route=LAMBDA_CHANNEL RFilterExpr}, all {@code piped=MapperS&lt;...FieldWithMeta...&gt;});
 * facet {@code ruleCalleeMetaDeref} (PR #336) is already at the seat and its fifth conjunct
 * ({@code itemCompiled instanceof JavaExpression}) is the one the probe could not read -- a2 is
 * what measures it.
 *
 * <p><b>Green safety, twice over.</b> By MEASUREMENT (the 24 declining rows are one file) and by
 * CONSTRUCTION (the pre-fix render is a dotted rune name, not a Java identifier, so no compiling
 * file can carry it). LAW 74: this law repairs {@code javac33} C7 lines <b>472, 474, 487</b>
 * ({@code cannot find symbol: variable quantity}) <b>and 487's cascade</b>
 * ({@code Object cannot be converted to String} at the {@code String} output sink) = 4 of C7's 8,
 * x4 cells.
 *
 * <p><b>LAW 77 -- legacy-only, and the IR gates are DO-NOT-TOUCH.</b>
 * {@code IRExpressionCompiler.ruleDelegationDeclines} ({@code :14306-14310}) returns {@code true}
 * for a function host and {@code buildRuleDelegationRenderer} ({@code :12965}) returns
 * {@code null} on the same condition, so the ON route falls through to THIS seat and inherits the
 * law. That decline is kept: its own comment names the one IR-only band regression it protects
 * (the #274 {@code PriorUniqueTransactionIdentifierRule} {@code MapperC} witness). {@code a3} and
 * {@code corpus_control2} re-prove the inheritance on the real seam.
 *
 * <p><b>THIS LAW IS AN ENABLER -- {@code QuantityUnitOfMeasure} does NOT go whole here.</b> Its
 * whole-file heal needs all four rungs of the seat-33 B group: <b>B.1</b>
 * ({@code setSeatNestedValueThenTogetherHoist} -- the inner chain's hoist, landed before this
 * law), <b>B.2</b> ({@code defaultJoinHeteroMetaDerefBoth}) and <b>B.4</b>
 * ({@code iteArmMetaCollapseDerefSinkChannel}), which land together as seat-33 law B.24. What
 * this law delivers at the carrier is the RULE-INVOCATION census: {@code Rule.evaluate(} 1 -&gt; 4,
 * equal to golden's 4 ({@code corpus_c1} / {@code corpus_c2}). The remaining divergence is PINNED,
 * not hidden, as {@code corpus_control1}'s {@code KNOWN_RESIDUE_DRR7} row -- B.24 removes it.
 *
 * <p><b>One ORDERING fact the residue encodes</b> (verdicts33-B section 3, "One correction to the
 * seat table"): golden gives seat 3 a BARE {@code item.get()} argument (golden:89) precisely
 * because {@code thenArg9} is {@code MapperS&lt;NonNegativeQuantitySchedule&gt;} -- which is true
 * only once B.2 lands. Between this law and B.24 seat 3 renders an OVER-deref, which is exactly
 * what the pinned residue row records; {@code e2} is the fixture that locks the no-deref shape so
 * the over-deref is a state of the CARRIER's numbering, never of the predicate.
 *
 * <p><b>CLAIMED sets -- measured by the chain, not by this file.</b>
 * <ul>
 *   <li><b>RED at the law's parent head</b> (default route AND {@code -Pir-on}):
 *       {@code a1}, {@code a2}, {@code a3} (ON only), <b>{@code e2}</b>, {@code corpus_c1},
 *       {@code corpus_c2}, {@code corpus_control1}. Only {@code e1},
 *       {@code corpus_control0} and {@code corpus_control2} are GREEN in both states.
 *       <b>{@code e2} moves WITH the law and is stated so up front</b>: its discriminating half is
 *       a negative ({@code final FieldWithMetaSeat33Sched} must NOT appear), but its PREMISE
 *       asserts the invocation the law creates, so it cannot be green before the law. It is an
 *       over-fire lock on the DEREF, not a decline lock on the HOST gate — {@code e1} is the
 *       decline lock.</li>
 *   <li><b>GREEN at the law's head</b>: all, both routes.</li>
 *   <li><b>MUTATION LANE {@code m-lawB3-fnhost}</b> (sever
 *       {@code || HandlerHelper.findEnclosingFunction(expr) != null}): CLAIMED
 *       {@code a1}, {@code a2}, {@code e2}, {@code corpus_c1}, {@code corpus_c2},
 *       {@code corpus_control1} FAIL; {@code e1} stays GREEN -- that split is the lane's whole
 *       point (the rule-host population is untouched). NOT empty: 24 carrier rows.</li>
 * </ul>
 *
 * <p><b>LAW-81 tripwires this law fires in OTHER suites</b> (named, not edited here): every suite
 * whose {@code KNOWN_RESIDUE} row for
 * {@code drr/standards/iosco/cde/version1/quantity/functions/QuantityUnitOfMeasure.java} scans a
 * token this law moves. See the law's {@code NOTES.md} for the enumerated list; each is re-pinned
 * from its own failing print in this same commit.
 *
 * <p><b>LAW 82 - MEASURED by the receipts chain, run 1 at ce1a06292 (final33.status; every
 * figure below is transcribed from the chain's own logs, never from this file's earlier
 * CLAIMED paragraphs, which it supersedes).</b> GREEN 10/0F/2skip default (f33-green-default.log) /
 * 10/0F/0skip {@code -Pir-on} (f33-green-on.log); RED at the pre-seat base {@code fa49da010}: default
 * 6F = a1, a2, corpus_c1, corpus_c2, corpus_control1, e2; {@code -Pir-on} 7F = a1, a2, a3, corpus_c1, corpus_c2, corpus_control1, e2 (f33-red-{default,on}.log).
 * Mutation lanes ({@code mut33.py}, the default profile; {@code lanes33.py --summary}):
 * <ul>
 *   <li><b>{@code m-lawB3-fnhost}</b> ({@code RH_PAIRS_MUT_FNHOST}): MEASURED 10/6F/2skip = a1, a2, corpus_c1, corpus_c2, corpus_control1, e2 - MATCH (a1, a2, e2, c1, c2, control1; e1 held - the FUNCTION/RULE split the lane exists for).</li>
 * </ul>
 */
class BareRuleRefFunctionHostSeatTest {

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

    /** Cell A = drr 7.0.0 -- the carrier and all three named green controls live here. */
    private static final Path CELL_A_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path GOLDEN_A = CELL_A_ROOT.resolve("rosetta-source/src/generated/java");

    /**
     * Cell B = drr 7.1.0 -- the second of the carrier's four cells. Its
     * {@code QuantityUnitOfMeasure} golden and fork renders are BYTE-IDENTICAL to drr 7.0.0's
     * (measured over the probe-round band dumps for all four of 7.0/7.1/7.2/7.3), so the second
     * cell proves the heal is not a one-cell accident without doubling the scan cost.
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
    private static final String MESSAGE_ID =
            "drr/regulation/common/trade/link/functions/MessageID.java";
    private static final String QUANTITY_SCHEDULE_RULE =
            "drr/base/trade/quantity/reports/QuantityScheduleRule.java";

    /**
     * The fixture. Every construct is a REDUCTION of {@code func QuantityUnitOfMeasure}'s own
     * source, with the carrier's cardinalities and metadata preserved:
     * {@code quantity} is MULTI and meta-annotated exactly as CDM's
     * {@code PriceQuantity.quantity NonNegativeQuantitySchedule (0..*) [metadata location]} is, so
     * the piped item is a {@code FieldWithMeta...} wrapper and the callee's {@code from} type is
     * the BARE value -- the shape that makes the argument deref load-bearing rather than
     * cosmetic. (The seat-32 E.3 lesson: a SINGLE reduction of a MULTI carrier is a non-witness.)
     *
     * <p>Lexer-safe identifiers: no {@code tag}, {@code single}, {@code label}, {@code value},
     * {@code key}.
     */
    private static final String MODEL = """
            namespace census.seat33f22
            version "1.0.0"

            type Seat33Sched:
                uom string (0..1)

            type Seat33PriceQty:
                quantity Seat33Sched (0..*)
                    [metadata location]
                bareQuantity Seat33Sched (0..*)

            type Seat33Lot:
                priceQuantity Seat33PriceQty (0..*)

            type Seat33Payout:
                tradeLot Seat33Lot (0..1)

            reporting rule UomFromQty from Seat33Sched: <"the UnitOfMeasureFromQuantity twin - a from-typed rule whose input is the BARE value type, so a meta-wrapper item must deref at the call">
                extract uom
                    as "uom"

            func A1BareRuleInFunctionLambda: <"a1/a2 - THE QuantityUnitOfMeasure SHAPE: a no-arg bare RRule reference inside a filter predicate AND inside an extract body, in a FUNCTION host, over a MULTI meta-annotated chain">
                inputs:
                    payout Seat33Payout (0..1)
                output:
                    picked string (0..1)
                set picked:
                    payout -> tradeLot -> priceQuantity -> quantity
                        then filter UomFromQty exists
                        then extract UomFromQty
                        then only-element

            func E2BareItemFunctionLambda: <"e2 - the NO-DEREF pin: the SAME function-host in-lambda reference over a meta-FREE chain. The host gate admits; ruleCalleeMetaDeref must self-decline and the argument stays the bare item.get() - golden QuantityUnitOfMeasure:89's shape.">
                inputs:
                    payout Seat33Payout (0..1)
                output:
                    picked string (0..1)
                set picked:
                    payout -> tradeLot -> priceQuantity -> bareQuantity
                        then extract UomFromQty
                        then only-element

            reporting rule E1RuleHostInLambda from Seat33Payout: <"e1 - the DECLINE LOCK for the new disjunct: the SAME in-lambda bare reference in a RULE host, which the pre-law gate ALREADY admits. Its bytes must not move, and the m-lawB3-fnhost lane must leave it GREEN.">
                tradeLot -> priceQuantity -> quantity
                    then extract UomFromQty
                    then only-element
                    as "e1"
            """;

    // =========================================================================
    // Part A -- the seat (RED at the law's parent head)
    // =========================================================================

    /**
     * a1 -- THE LAW. A bare no-arg rule reference inside a lambda in a FUNCTION host renders the
     * injected rule invocation. Witness-uniqueness: {@code uomFromQtyRule} is a token the flip
     * CREATES and that the pre-law render cannot contain, and the bare splice
     * {@code MapperS.of(UomFromQty)} is a token the flip REMOVES and that the post-law render
     * cannot contain -- neither assertion can pass vacuously in both directions.
     *
     * <p>The {@code @Inject} field assertion is the fork's own evidence that PR #332's collector
     * half already landed: the field is declared at a FUNCTION host today, with nothing to use it.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_bareRuleReferenceInAFunctionLambdaRendersTheInvocation() throws IOException {
        String code = collapse(codeOnly(func("A1BareRuleInFunctionLambda.java")));
        assertContains(code, "uomFromQtyRule.evaluate(");
        assertNotContains(code, "MapperS.of(UomFromQty)");
        assertContains(collapse(func("A1BareRuleInFunctionLambda.java")),
                "@Inject protected UomFromQtyRule uomFromQtyRule;");
    }

    /**
     * a2 -- THE ARGUMENT DEREF, the half {@code [P33-RULEARG]} could only predict
     * ({@code derefWouldFire=true} 24/24) and the half whose fifth conjunct
     * ({@code itemCompiled instanceof JavaExpression}) no probe could read. The piped item is a
     * {@code FieldWithMetaSeat33Sched} and the rule's {@code from} type is the bare
     * {@code Seat33Sched}, so facet {@code ruleCalleeMetaDeref} (PR #336) must hoist the wrapper
     * local and pass the null-guarded deref -- golden {@code QuantityUnitOfMeasure:65-66} /
     * {@code :72-73} verbatim, with the fixture's own type names.
     *
     * <p>Both fragments are transcribed from the golden dump, collapsed; the second cannot exist
     * pre-law at all (it names {@code uomFromQtyRule}), so a2's RED is on the LAW's token.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_theMetaWrapperItemDerefsIntoTheRulesFromType() throws IOException {
        String code = collapse(codeOnly(func("A1BareRuleInFunctionLambda.java")));
        assertContains(code,
                "final FieldWithMetaSeat33Sched fieldWithMetaSeat33Sched = item.get();");
        assertContains(code, "uomFromQtyRule.evaluate((fieldWithMetaSeat33Sched == null ? null"
                + " : fieldWithMetaSeat33Sched.getValue()))");
    }

    /**
     * a3 -- LAW 77: the a1 shape through the REAL {@code IRGeneration.functionGenerator} seam.
     * The IR route DECLINES bare rule delegation for a function host
     * ({@code ruleDelegationDeclines} returns true when {@code findEnclosingRule == null}) and
     * falls through to this legacy seat, so the law is INHERITED rather than mirrored. The gates
     * themselves are DO-NOT-TOUCH; this test is the proof that the inheritance is real on the
     * seam and not an argument about it.
     */
    @Test
    @EnabledIf("builtinsAndIrProviderAvailable")
    void a3_theSameInvocationRendersOnTheIrRoute() throws IOException {
        String code = collapse(codeOnly(
                lookup(fixtureOnIrRoute(), "functions/A1BareRuleInFunctionLambda.java")));
        assertContains(code, "uomFromQtyRule.evaluate(");
        assertNotContains(code, "MapperS.of(UomFromQty)");
    }

    // =========================================================================
    // Part B -- the decline / no-over-fire locks
    // =========================================================================

    /**
     * e1 -- THE DECLINE LOCK for the new disjunct. The same in-lambda bare reference in a RULE
     * host is what the PRE-law gate already admitted (the {@code [P33-RULEARG]} control's 14,335
     * admitting rows), so the law must add nothing here: the rendered invocation is present in
     * BOTH states and the bare splice in neither.
     *
     * <p>This is the lane's green side: under {@code m-lawB3-fnhost} (the function-host disjunct
     * severed) a1/a2 fail and e1 stays GREEN. A lane that moved e1 would mean the widening had
     * reached the rule path, which is precisely the reach this fixture bounds.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void e1_ruleHostInLambdaReferenceIsUnmoved() throws IOException {
        String code = collapse(codeOnly(rule("E1RuleHostInLambdaRule.java")));
        assertContains(code, "uomFromQtyRule.evaluate(");
        assertNotContains(code, "MapperS.of(UomFromQty)");
    }

    /**
     * e2 -- THE NO-DEREF LOCK. Same host, same lambda, meta-FREE chain: the host gate admits (the
     * law fires) but facet {@code ruleCalleeMetaDeref}'s own meta gate must self-decline, so the
     * argument stays the bare {@code item.get()} -- golden {@code QuantityUnitOfMeasure:89}'s
     * shape, {@code .mapSingleToItem(item -> MapperS.of(<rule>.evaluate(item.get())))}.
     *
     * <p>PREMISE first (the seat-30 b4 pattern): a file-wide negative passes vacuously if the
     * fixture never reached the shape, so assert the invocation before asserting the absence of
     * the guard.
     *
     * <p><b>e2 is RED at the parent head, and that is by design, not an accident.</b> Its premise
     * IS the law's own token, so it moves with the law; what it locks is the SECOND half — that
     * admitting the host does not also drag the meta deref onto a bare item. The
     * {@code m-lawB3-fnhost} lane therefore fails e2 as well as a1/a2, and only {@code e1} splits
     * the two populations.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void e2_aBareItemArgumentTakesNoDeref() throws IOException {
        String code = collapse(codeOnly(func("E2BareItemFunctionLambda.java")));
        assertContains(code, "uomFromQtyRule.evaluate(");
        assertContains(code, "uomFromQtyRule.evaluate(item.get())");
        assertNotContains(code, "final FieldWithMetaSeat33Sched");
    }

    // =========================================================================
    // Part C -- the corpus carrier (4 cells; drr 7.0.0 and 7.1.0 locked here)
    // =========================================================================

    /**
     * control0 -- golden is the oracle and it must DISCRIMINATE (prove the instrument can fail).
     * Golden's carrier carries the invocation at all three seats and the guarded deref at the two
     * hoisting seats; NO golden anywhere carries the dotted rune splice; and the file the fork
     * must emit is the one this suite locks.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control0_goldenCarriesTheInvocationAndNeverTheDottedSplice() throws IOException {
        String g = collapse(codeOnly(Files.readString(GOLDEN_A.resolve(QUOM))));
        assertEquals(4, count(g, "Rule.evaluate("),
                "golden's carrier must carry FOUR rule invocations (three UomFromQuantity seats"
                + " plus the explicit-args CommodityFixedPriceQuantity one)");
        assertEquals(2, count(g,
                "unitOfMeasureFromQuantityRule.evaluate((fieldWithMetaNonNegativeQuantitySchedule"
                + " == null ? null : fieldWithMetaNonNegativeQuantitySchedule.getValue()))"),
                "golden must carry the guarded deref at BOTH hoisting seats");
        assertContains(g,
                ".mapSingleToItem(item -> MapperS.of(unitOfMeasureFromQuantityRule"
                + ".evaluate(item.get()))).get();");
        assertNotContains(g, "MapperS.of(quantity.UnitOfMeasureFromQuantity)");
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");
        assertNotNull(drrAOutput.get(QUOM), "the fork must emit the carrier " + QUOM);
    }

    /**
     * c1 -- the carrier on drr 7.0.0. This law is an ENABLER, so the lock is the law's OWN census
     * at the carrier, not a byte-whole compare: the fork's rule-invocation count must EQUAL
     * golden's (1 -&gt; 4 at this law), the dotted splice must be gone, and the guarded deref must
     * be present at both hoisting seats exactly as golden spells it. The file's remaining
     * divergence is B.2's and B.4's and is pinned by {@code corpus_control1}.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_quomRuleReferencesRenderTheInvocationOnDrr700() throws IOException {
        assertCarrierRuleCensus(drrAOutput, GOLDEN_A, "drr 7.0.0");
    }

    /** c2 -- the same carrier, second cell. See {@link #corpus_c1_quomRuleReferencesRenderTheInvocationOnDrr700}. */
    @Test
    @EnabledIf("cellBAvailable")
    void corpus_c2_quomRuleReferencesRenderTheInvocationOnDrr710() throws IOException {
        assertEquals(List.of(), drrBGenErrors,
                "drr 7.1.0 reported a generation error - the carrier lock is unsound");
        assertCarrierRuleCensus(drrBOutput, GOLDEN_B, "drr 7.1.0");
    }

    private static void assertCarrierRuleCensus(Map<String, String> out, Path goldenRoot,
            String cell) throws IOException {
        assertNotNull(out, cell + " generation did not run - corpus unavailable?");
        String generated = out.get(QUOM);
        assertNotNull(generated, "not generated in " + cell + ": " + QUOM);
        String f = collapse(codeOnly(generated));
        String g = collapse(codeOnly(Files.readString(goldenRoot.resolve(QUOM))));
        assertNotContains(f, "MapperS.of(quantity.UnitOfMeasureFromQuantity)");
        assertEquals(count(g, "Rule.evaluate("), count(f, "Rule.evaluate("),
                "the carrier's rule-invocation census must equal golden's in " + cell
                + " - the three bare splices become injected invocations");
        assertContains(f,
                "unitOfMeasureFromQuantityRule.evaluate((fieldWithMetaNonNegativeQuantitySchedule"
                + " == null ? null : fieldWithMetaNonNegativeQuantitySchedule.getValue()))");
    }

    /**
     * control1 -- LAW 79, the whole-cell UNION scan on drr 7.0.0. The declining population is
     * enumerable (24 rows, one file), but the widened host test is reachable from EVERY bare
     * no-arg rule reference in the cell, so the scan is the only instrument that can see an
     * over-fire at a site the {@code [P33-RULEARG]} census never named.
     *
     * <p><b>The columns</b> (T1, T2, T3), each a direction the law moves or must not move:
     * <ul>
     *   <li><b>T1</b> {@code Rule.evaluate(} -- the invocation family the law ADDS. Golden is the
     *       oracle: a moved T1 in any file but the carrier is an over-fire, and a T1 that fails
     *       to move at the carrier is an under-fire.</li>
     *   <li><b>T2</b> {@code @Inject protected } -- the injected-dependency census. The law
     *       changes only the RENDER, never {@code FunctionDependencyCollector}, so this column
     *       must not move ANYWHERE, in either direction.</li>
     *   <li><b>T3</b> {@code == null ? null : } -- the guarded-deref census. The argument deref
     *       adds two of these at the carrier; a move anywhere else is an over-deref.</li>
     * </ul>
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control1_forkDrr7WholeCellRuleShapesEqualGoldenFileByFile() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");
        assertEquals(List.of(), drrAGenErrors,
                "drr 7.0.0 reported a generation error - the scan is incomplete");
        assertTrue(drrAOutput.containsKey(QUOM) && drrAOutput.containsKey(MESSAGE_ID)
                        && drrAOutput.containsKey(QUANTITY_SCHEDULE_RULE),
                "the carrier and the two named rule-host green witnesses must be INSIDE this"
                + " scan's domain, else control1 proves nothing about them (LAW: a control scans"
                + " the domain it claims)");
        assertUnionEqual(scan(drrAOutput), scan(readGoldenTree(GOLDEN_A)), drrAOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_DRR7, DOMAIN_DRR7);
    }

    /**
     * control2 -- LAW 77 route parity, per FILE. The law sits in {@code ReferenceHandler}, which
     * the IR-routed compiler reaches by DECLINING bare rule delegation for a function host; the
     * probe measured the 24 gate rows identical on both routes and this re-proves it on the real
     * IR seam for the carrier and the two green witnesses.
     */
    @Test
    @EnabledIf("cellAAndIrProviderAvailable")
    void corpus_control2_irRouteMatchesLegacyForTheCarrierAndTheWitnesses() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 legacy-route generation did not run");
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", CELL_A_ROOT), new ArrayList<>());
        for (String path : List.of(QUOM, MESSAGE_ID, QUANTITY_SCHEDULE_RULE)) {
            assertEquals(drrAOutput.get(path), irOut.get(path), "route divergence: " + path);
        }
    }

    /**
     * The residue this ENABLER leaves, PINNED (the brief's {@code KNOWN_RESIDUE} discipline for a
     * partial law), transcribed from control1's own printed mismatch set at the law's head.
     *
     * <ul>
     *   <li><b>{@code QuantityUnitOfMeasure}</b> -- the carrier. T1 reaches golden's 4 at this
     *       law; T3 does not, because B.2's LEFT deref and its right-hand hoist, and B.4's two
     *       arm derefs, are three more guarded derefs this law does not own. <b>Seat-33 law B.24
     *       removes this row</b> (the file goes byte-whole there).</li>
     *   <li><b>{@code Price}</b> -- an unrelated band file at this head, chartered to seat-33 laws
     *       C.1/C.2 which land AFTER this one. Its T3 delta is the ite-ladder mapper-form
     *       divergence, nothing this law touches. C.2 removes this row.</li>
     * </ul>
     *
     * <p><b>The values below are DERIVED, not yet measured</b> (this suite is drafted without a
     * JVM): {@code Price}'s row is the fork-vs-golden count measured over the probe round's drr
     * 7.0.0 band dump at the seat-33 base head {@code fa49da010}, and {@code QuantityUnitOfMeasure}'s
     * is that same measurement plus this law's own three invocations and two derefs (fork
     * {@code [1, 2, 1]} -&gt; {@code [4, 2, 4]}; golden is {@code [4, 2, 7]}). Both are to be
     * TRANSCRIBED VERBATIM from control1's failing print at the law's head -- never widened, and
     * never used to weaken an assert.
     */
    private static final List<String> KNOWN_RESIDUE_DRR7 = List.of(
            // the Price row (fork=[0, 16, 10] golden=[0, 16, 12]) LEFT this list: law C.2 took Price WHOLE in all four drr 7.x cells; this
            // suite was NOT in C.2's 28-suite LAW-81 batch and its row was caught by B.24's batch (B24-trip1.log),
            // a pure row removal (was == expected minus it) - the C.2 tripwire the batch list missed.
            // the QuantityUnitOfMeasure row (fork=[4, 2, 4] golden=[4, 2, 7]) LEFT this list: law B.24 (defaultJoinHeteroMetaDerefBoth +
            // iteArmMetaCollapseDerefSinkChannel + three in-seat rungs) healed the file WHOLE in all four drr 7.x
            // cells - the band's last four files; transcribed from this control's own print (B24-trip1.log),
            // a pure row removal (was == expected minus it).
            );

    /**
     * The union domain, pinned (LAW 73) -- a negative value means an UNPINNED call site and
     * {@link #assertUnionEqual} fails loudly rather than passing vacuously.
     *
     * <p><b>DERIVED, to be re-pinned from the print.</b> 3,759 is the number of drr 7.0.0 GOLDEN
     * files under a {@code /functions/} or {@code /reports/} path that carry at least one of the
     * three tokens, measured by a read-only walk over
     * {@code test-corpus/drr/drr-7.0.0/rosetta-source/src/generated/java} at the seat-33 base head
     * (the walk mirrors this suite's own {@code codeOnly} + {@code collapse} + literal count).
     * The live union is that set UNIONED with the fork's token-bearing files and INTERSECTED with
     * what the fork emits, so it can exceed the derivation if the fork emits a token in a file
     * golden has none in. Transcribe the measured value from this assert's own
     * {@code MEASURED domain=} print.
     */
    private static final int DOMAIN_DRR7 = 3759;

    private static Map<String, int[]> scan(Map<String, String> tree) {
        Map<String, int[]> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : tree.entrySet()) {
            String flat = collapse(codeOnly(e.getValue()));
            int t1 = count(flat, "Rule.evaluate(");
            int t2 = count(flat, "@Inject protected ");
            int t3 = count(flat, "== null ? null : ");
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

    // =========================================================================
    // The union assert (LAW 73: pin the SET, not the count) - the seat-29/30 shape verbatim
    // =========================================================================

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
        // The domain-pin flow (LAW 73): the sentinel failure PRINTS the measured values so
        // the pin is transcribed from this assert's own output, never invented.
        assertTrue(expectedDomain >= 0,
                "the union domain is MEASURED and pinned (LAW 73) - transcribe from this"
                        + " print: MEASURED domain=" + universe.size()
                        + " residue=" + mismatched);
        assertEquals(knownResidue, mismatched,
                "(T1, T2, T3) differ beyond the named residue in " + mismatched.size() + " file(s)");
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
            RModel main = AstBuilder.buildFromString(MODEL, "seat33f22.rosetta");
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
            fixtureOut = render(m -> "census.seat33f22".equals(m.namespace()));
        }
        return fixtureOut;
    }

    private static Map<String, String> fixtureOnIrRoute() throws IOException {
        if (fixtureIrOut == null) {
            fixtureIrOut = renderOnIrRoute(m -> "census.seat33f22".equals(m.namespace()));
        }
        return fixtureIrOut;
    }

    private static String rule(String fileName) throws IOException {
        return lookup(fixture(), "reports/" + fileName);
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
            throw new AssertionError("[BareRuleRefFunctionHostSeatTest] builtins parse"
                    + " failures: " + String.join("; ", failures));
        }
        return models;
    }
}
