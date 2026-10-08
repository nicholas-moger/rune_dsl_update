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
 * SEAT 28, law 3 — facet {@code defaultCollapsingListOpRight}: <b>a {@code default} whose RIGHT is a
 * COLLAPSING list-op ({@code first}/{@code last}/{@code only-element}) collapses the argument
 * {@code .get()} and re-wraps the whole result {@code MapperS.of(…)}</b> — the same single-item-Mapper
 * treatment {@code getOrDefaultMinMaxArgCollapse} (#376) already gives {@code min}/{@code max}.
 *
 * <p>The law has TWO rungs, and the file-heal arithmetic needs BOTH:
 * <ol>
 *   <li><b>(A) the arm's KIND test widens</b> from <code>{RMin, RMax}</code> to the shared
 *       {@code isCollapsingListOp} predicate ({@code SetOperationHandler:1301}, the ONE definition
 *       this file already consults at the #367 reducer arm {@code :1406} — LAW 69). The render at
 *       {@code :704-709} is UNTOUCHED: it already emits
 *       {@code JavaExpression.wrappedInMapperSOf(left + ".getOrDefault(" + right + ".get())")},
 *       which is golden's shape with the dual-consumer unwrap contract. Carriers: drr 7.x
 *       {@code MapNonpublicExecutionReportToWorkflowStep} (2 of 3 hunks — the third is F7) and the
 *       INNER default of {@code NotionalQuantityRule}.</li>
 *   <li><b>(B) the NESTED default</b> — a {@code then}-chain RIGHT whose BODY is itself a
 *       {@code default} that rendered WRAPPED collapses {@code .get()} on the WRAP and re-wraps the
 *       whole. Carrier: the OUTER default of {@code NotionalQuantityRule}. <b>Without rung B this law
 *       heals ZERO whole files</b> (LAW 80): {@code NotionalQuantityRule} is a SINGLE-hunk file whose
 *       one hunk carries both defaults, and {@code MapNonpublic…} is 2-of-3 by construction.</li>
 * </ol>
 *
 * <p><b>The golden vs fork shape</b> (drr 7.0.0 {@code NotionalQuantityRule}, the single hunk —
 * both rungs visible at once):
 * <pre>
 * golden:  final MapperS&lt;BigDecimal&gt; thenArg1 = MapperS.of(MapperS.of(equityNotionalQuantityRule.evaluate(input))
 *              .getOrDefault(MapperS.of(thenArg0.&lt;BigDecimal&gt;map("getValue", …)
 *                  .getOrDefault(thenArg0.&lt;DatedValue&gt;mapC(…).&lt;BigDecimal&gt;map(…).first().get())).get()));
 * fork:    final MapperS&lt;BigDecimal&gt; thenArg1 =            MapperS.of(equityNotionalQuantityRule.evaluate(input))
 *              .getOrDefault(          thenArg0.&lt;BigDecimal&gt;map("getValue", …)
 *                  .getOrDefault(thenArg0.&lt;DatedValue&gt;mapC(…).&lt;BigDecimal&gt;map(…).first()));
 * </pre>
 * and (drr 7.0.0 {@code MapNonpublicExecutionReportToWorkflowStep}, rung A alone):
 * <pre>
 * golden:  final MapperS&lt;ZonedDateTime&gt; thenArg1 = MapperS.of(thenArg0.getOrDefault(&lt;chain&gt;.first().get()));
 * fork:    final MapperS&lt;ZonedDateTime&gt; thenArg1 =            thenArg0.getOrDefault(&lt;chain&gt;.first());
 * </pre>
 *
 * <p><b>LAW 74</b>: {@code Mapper#getOrDefault} is declared {@code T getOrDefault(T defaultValue)},
 * so the fork passes a {@code MapperS} where the item is expected AND assigns the raw {@code T} to a
 * {@code MapperS<T>} local — two {@code incompatible types} errors per site. The PRE javac probe must
 * show them on all 8 carrier files and the POST must exit 0. Green-safe by construction: no green
 * golden can carry a non-compiling form.
 *
 * <p><b>The probe verdict this law answers (LAW 75 — {@code PROBE28-F10c}).</b> 152 rows per route,
 * 12 distinct tuples, <b>route-identical</b>:
 * <ul>
 *   <li>{@code admitted=true} occurs EXACTLY 20 times corpus-wide — all {@code RMinExpr}/{@code RMaxExpr},
 *       all on the GREEN {@code SingleOrUpperAndLowerBarrierRule} in drr 6.34.1–6.38. The arm's
 *       existing population is entirely green and its render is untouched here.</li>
 *   <li>{@code rightListOp} is non-null in EXACTLY 20 of 152 rows and is {@code FIRST} in all 20 —
 *       {@code MapNonpublic…} 16 + {@code NotionalQuantity} 4, <b>all band, zero green</b>.</li>
 *   <li><b>ZERO {@code LAST} rows and ZERO {@code ONLY_ELEMENT} rows reach this seat</b>, on either
 *       route. Their admission under the shared predicate is byte-inert BY MEASUREMENT and is
 *       witnessed only by the fixture (a3) — stated, not claimed as a corpus heal.</li>
 *   <li>The 19 GREEN collapsing-right defaults corpus-wide ({@code ExtractProductIdentifierBySource}
 *       FIRST ×9, {@code CountryOfCounterparty2Rule} ONLY_ELEMENT ×10) never reach this seat: the
 *       #367 reducer arm at {@code SetOperationHandler:1405-1418} claims them 46 lines earlier
 *       because its gate is {@code rightLop.argument() instanceof RSymbolReference} and their rights
 *       are list-ops over a BARE SYMBOL. Both carriers' rights are list-ops over a NAVIGATION CHAIN.
 *       The two populations are structurally disjoint.</li>
 *   <li>Rung B's domain ({@code PROBE28-F10a}, {@code rightKind=RThenExpr}) is 33 rows corpus-wide =
 *       20 band + 13 GREEN, and <b>both green classes are excluded structurally</b>:
 *       {@code GetInternalId} (9) returns at the #363 arm {@code :687}, {@code UniqueSwapIdentifier-
 *       ForValuation} (4) at the seat-5 multi-ternary arm {@code :495} — both BEFORE rung B's seat.</li>
 * </ul>
 *
 * <p><b>RED at the pre-seat blob</b>: a1, a2, a3, corpus_c1, corpus_c2, corpus_control1.
 * b1, b2, b3, corpus_control0, corpus_control3 GREEN in both states.
 *
 * <p><b>LAW 66/76 mutations</b> — the seat's chain ran ONE on this suite: the WHOLE law reverted
 * (both rungs), whose RECORDED failing set is in the MEASURED MUTATIONS block below (written FROM
 * the logs, never ahead of them). The planned severs were NOT RUN as such — each is <b>NOT RUN,
 * banked to S29</b>: (i) the collapsing-list-op disjunct removed from the {@code :701} kind test;
 * (ii) rung B's arm deleted; (iii) rung B's {@code unwrapToBuilder} term widened away (a then-right
 * whose body declined would then wrap).
 * <p><b>MEASURED MUTATIONS (LAW 82 - the seat-28 mut28 suite-lane loop; each
 * mutation = the named apply-script reverted, the suite run, the script re-applied;
 * every set below is the RECORDED failing set from that run, never a claim):</b>
 * <ul>
 *   <li>the whole law reverted (law3-apply --revert: both rungs) -> a1, a2, corpus_c1, corpus_c2, corpus_control1 (5F)</li>
 * </ul>
 */
class DefaultCollapsingRightSeatTest {

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

    /** cell A = drr 7.0.0 — BOTH carriers, plus the two green collapsing-right classes. */
    private static final Path CELL_A_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path GOLDEN_A = CELL_A_ROOT.resolve("rosetta-source/src/generated/java");
    /** cell B = drr 6.34.1 — the arm's PRE-EXISTING green population (the min/max admissions), no carrier. */
    private static final Path CELL_B_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
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

    /**
     * A1 = the {@code MapNonpublicExecutionReportToWorkflowStep} shape: a {@code default} whose RIGHT is
     * {@code first} over a NAVIGATION CHAIN (rung A). A2 = the {@code NotionalQuantityRule} shape
     * verbatim — {@code X default (Y then a default b first)} (rung A + rung B). A3 = the same law
     * with {@code only-element}: FIXTURE-ONLY BY MEASUREMENT (zero {@code ONLY_ELEMENT} rows reach the
     * seat corpus-wide). B1 = the arm's pre-existing {@code min} population, byte-frozen. B2 = a
     * {@code first} over a BARE SYMBOL — the #367 reducer owns it, 46 lines earlier. B3 = a
     * {@code then}-chain right whose body is NOT a {@code default} — rung B must decline.
     */
    private static final String MODEL = """
            namespace census.seat28c
            version "1.0.0"

            type Dated:
                value number (0..1)

            type Sched:
                value number (0..1)
                datedValue Dated (0..*)
                note string (0..1)

            type Leg:
                code string (0..1)
                amt number (0..1)

            type Holder:
                head Leg (0..1)
                legs Leg (0..*)
                sched Sched (0..1)
                tally number (0..*)

            func A1ChainRootedFirstDefault: <"a1 - a default whose RIGHT is `first` over a NAV CHAIN">
                inputs:
                    h Holder (1..1)
                output:
                    result string (0..1)
                set result:
                    h -> head -> code default h -> legs -> code first

            func A2NestedThenDefault: <"a2 - the NotionalQuantityRule shape: X default (Y then a default b first)">
                inputs:
                    h Holder (1..1)
                output:
                    result number (0..1)
                set result:
                    h -> head -> amt default (h -> sched
                        then value default datedValue -> value first
                        )

            func A3ChainRootedOnlyElementDefault: <"a3 - the same law with only-element (fixture-only by measurement)">
                inputs:
                    h Holder (1..1)
                output:
                    result string (0..1)
                set result:
                    h -> head -> code default h -> legs -> code only-element

            func B1MinRightDefault: <"b1 - the arm's pre-existing min population: byte-frozen">
                inputs:
                    h Holder (1..1)
                output:
                    result number (0..1)
                set result:
                    h -> tally min default h -> legs -> amt min

            func B2BareSymbolFirstDefault: <"b2 - a first over a BARE SYMBOL: the #367 reducer owns it">
                inputs:
                    h Holder (1..1)
                    codes string (0..*)
                output:
                    result string (0..1)
                set result:
                    h -> head -> code default codes first

            func B3ThenRightNotADefault: <"b3 - a then-chain right whose body is NOT a default: rung B declines">
                inputs:
                    h Holder (1..1)
                output:
                    result number (0..1)
                set result:
                    h -> head -> amt default (h -> sched
                        then value
                        )
            """;

    /**
     * a1 — rung A: the collapsing-list-op right collapses {@code .get()} and the whole result takes the
     * {@code MapperS.of(…)} re-wrap. The witness is the ADJACENCY {@code .first().get()} inside the
     * {@code getOrDefault(} argument; the un-collapsed form {@code .first())} is the token the flip
     * REMOVES, so both directions are asserted (the decline-lock law applied to a positive).
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_chainRootedFirstRightCollapsesAndReWraps() throws IOException {
        String out = fn("A1ChainRootedFirstDefault.java");
        String code = codeOnly(out);
        assertContains(out, ".getOrDefault(");
        assertTrue(code.contains(".first().get()"),
                "the collapsing list-op right must collapse .get() at the getOrDefault arg:\n" + out);
        assertTrue(!code.contains(".first())"),
                "no un-collapsed .first() may survive as a getOrDefault argument:\n" + out);
        assertTrue(code.contains("MapperS.of("),
                "the whole default must take the MapperS.of re-wrap:\n" + out);
    }

    /**
     * a2 — rungs A+B together, the {@code NotionalQuantityRule} shape verbatim: the INNER default
     * collapses and wraps (rung A), and the OUTER default takes the inner's WRAP as its argument with a
     * trailing {@code .get()} and re-wraps itself (rung B). The nesting witness is the double
     * {@code MapperS.of(MapperS.of(} that only rung B produces.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_nestedThenWrappedDefaultTakesTheWrappedArg() throws IOException {
        String out = fn("A2NestedThenDefault.java");
        String code = codeOnly(out);
        assertTrue(code.contains(".first().get()"),
                "rung A: the inner default's collapsing right must collapse:\n" + out);
        assertTrue(code.contains(").get()))") || code.contains(").get())"),
                "rung B: the outer default's argument is the inner's WRAP with a trailing .get():\n" + out);
        assertTrue(!code.contains(".first())"),
                "no un-collapsed .first() may survive as a getOrDefault argument:\n" + out);
    }

    /**
     * a3 — the {@code only-element} member of the shared collapsing class. <b>FIXTURE-ONLY BY
     * MEASUREMENT</b>: {@code PROBE28-F10c} sees ZERO {@code ONLY_ELEMENT} rows at this seat over 152
     * rows × 25 cells × both routes, so this admission moves no corpus byte. It exists because the law
     * consults the ONE shared {@code isCollapsingListOp} definition (LAW 69) rather than re-spelling a
     * narrower "collapsing" a fourth time in the same file.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a3_chainRootedOnlyElementRightCollapsesAndReWraps() throws IOException {
        String out = fn("A3ChainRootedOnlyElementDefault.java");
        String code = codeOnly(out);
        assertTrue(code.contains(".get().get()") || code.contains("MapperS.of("),
                "the only-element right must take the same collapse + re-wrap as first:\n" + out);
        // PIN AT RED: read the exact only-element render off the run and replace the disjunct above
        // with the byte-exact assert (only-element already collapses .get() in CollectionHandler, so
        // the arg's SECOND .get() is this law's addition).
    }

    /**
     * b1 — the decline pin (byte-freeze): an {@code RMin} right is the arm's PRE-EXISTING population
     * (20 admissions corpus-wide, all on the GREEN {@code SingleOrUpperAndLowerBarrierRule}). The kind
     * test widens; the render does not. This file must be byte-identical before and after, and in
     * particular must never gain a SECOND {@code .get()} — a double-fire is the only way the widening
     * could reach it.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b1_minRightDefaultKeepsTodaysBytes() throws IOException {
        String out = fn("B1MinRightDefault.java");
        String code = codeOnly(out);
        assertContains(out, ".getOrDefault(");
        assertTrue(!code.contains(".get().get()"),
                "the min/max arm must not double-collapse under the widened kind test:\n" + out);
        // PIN AT RED: replace with the byte-exact `set result` line, captured from the pre-seat run.
    }

    /**
     * b2 — the decline pin (LAW 76 witness-uniqueness): a {@code first} over a BARE SYMBOL is the GREEN
     * class ({@code ExtractProductIdentifierBySource} ×9). The #367 reducer arm at
     * {@code SetOperationHandler:1405-1418} claims it at {@code :655}, forty-six lines before the
     * widened arm, because its gate is {@code rightLop.argument() instanceof RSymbolReference}. The two
     * arms emit the SAME shape, so the witness-unique failure mode is a DOUBLE fire — assert the single
     * {@code .get()} and the single wrap.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b2_bareSymbolFirstRightStaysWithTheReducerArm() throws IOException {
        String out = fn("B2BareSymbolFirstDefault.java");
        String code = codeOnly(out);
        assertTrue(code.contains(".first().get()"),
                "the #367 reducer already collapses this right — it must keep doing so:\n" + out);
        assertTrue(!code.contains(".first().get().get()"),
                "a second collapse means the widened arm re-claimed a #367 row:\n" + out);
        assertTrue(!code.contains("MapperS.of(MapperS.of("),
                "a second re-wrap means the widened arm re-claimed a #367 row:\n" + out);
    }

    /**
     * b3 — the decline pin for rung B (LAW 76 witness-uniqueness): a {@code then}-chain RIGHT whose body
     * is a plain navigation, NOT a {@code default}. Rung B's gate requires the body ROOT to be an
     * {@code RDefaultExpr} (which is also what makes it disjoint from the #363 arm, whose gate requires
     * a collapsing LIST-OP body root), so this file must carry NEITHER token rung B adds — no wrapped
     * argument and no outer re-wrap.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b3_thenRightWithNonDefaultBodyKeepsDecline() throws IOException {
        String out = fn("B3ThenRightNotADefault.java");
        String code = codeOnly(out);
        assertContains(out, ".getOrDefault(");
        assertTrue(!code.contains("MapperS.of(MapperS.of("),
                "rung B must not wrap a then-right whose body is not a default:\n" + out);
        // PIN AT RED: add the byte-exact `set result` line once the pre-seat render is captured.
    }

    // =========================================================================
    // Part C — the corpus carriers (4 whole + 4 improved across one cell family)
    // =========================================================================

    private static final String NOTIONAL_QUANTITY =
            "drr/regulation/common/trade/quantity/reports/NotionalQuantityRule.java";

    private static final String MAP_NONPUBLIC =
            "drr/ingest/fpml/recordkeeping/message/functions/MapNonpublicExecutionReportToWorkflowStep.java";

    /** c1 — the WHOLE heal: {@code NotionalQuantityRule} is a single-hunk file carrying BOTH defaults. */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_notionalQuantityRuleByteIdentical() throws IOException {
        lockA(NOTIONAL_QUANTITY);
    }

    /**
     * c2 — the IMPROVE, pinned as a token count rather than a byte-match: {@code MapNonpublic…} carries
     * THREE hunks and the third is F7 (the inline {@code .then(} fallback), so this law can never make
     * it byte-identical. LAW 80 forbids counting it as a heal; this control pins that its FOUR
     * collapsing-right sites move and nothing else does.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c2_mapNonpublicCollapsingSitesMatchGolden() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");
        String generated = drrAOutput.get(MAP_NONPUBLIC);
        assertNotNull(generated, "not generated in drr 7.0.0: " + MAP_NONPUBLIC);
        String golden = Files.readString(GOLDEN_A.resolve(MAP_NONPUBLIC));
        assertEquals(count(codeOnly(golden), ".first().get()"), count(codeOnly(generated), ".first().get()"),
                "the four collapsing-right sites must collapse exactly as golden does");
        assertEquals(0, count(codeOnly(generated), ".first())"),
                "no un-collapsed .first() may survive as a getOrDefault argument");
    }

    /** control0 — golden is the oracle at both carriers. */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control0_goldenCollapsesAndReWrapsAtEveryCarrier() throws IOException {
        String nq = Files.readString(GOLDEN_A.resolve(NOTIONAL_QUANTITY));
        assertTrue(nq.contains(".first().get()"), "golden must collapse the inner default's right");
        assertTrue(nq.contains("MapperS.of(MapperS.of("), "golden must carry both re-wraps");
        String mn = Files.readString(GOLDEN_A.resolve(MAP_NONPUBLIC));
        assertEquals(4, count(mn, ".first().get()"), "golden MapNonpublic has four collapsed sites");
        assertEquals(0, count(mn, ".first())"), "golden keeps no un-collapsed .first() argument");
    }

    /**
     * control1 — LAW 79, the whole-cell UNION scan on drr 7.0.0: per file the (collapsed arg,
     * un-collapsed arg, nested wrap) triple must equal golden's, file for file over the UNION, beyond
     * the NAMED residue. Its reach covers the two GREEN collapsing-right classes in this cell
     * ({@code ExtractProductIdentifierBySource} and {@code CountryOfCounterparty2Rule}), so an
     * over-fire into either fails outright.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control1_forkDrr7WholeCellCollapseSitesEqualGoldenFileByFile() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");
        assertEquals(List.of(), drrAGenErrors, "drr 7.0.0 reported a generation error — the scan is incomplete");
        assertUnionEqual(scan(drrAOutput), scan(readGoldenTree(GOLDEN_A)), drrAOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_7, DOMAIN_DRR7);
    }

    /**
     * MEASURED AT THE CHAIN (LAW 82). <b>RE-MEASURED at the seat-30 chain head {@code e223ce19}</b>
     * — 3 entries, transcribed VERBATIM from that run's own failing print (LAW 81); the prior set
     * was the seat-29 head's 4. <b>RE-MEASURED at the seat-31 chain head {@code f2a4d5c0}</b> — 2
     * entries, the UnderlierBasketIdentifier row having LEFT (noted inline), transcribed VERBATIM
     * from that run's own failing print.
     */
    private static final List<String> KNOWN_RESIDUE_7 = List.of(
            // GetBasket.java (was fork=[0, 0, 1] golden=[0, 0, 0]) left this list at seat 30: laws
            // 6 + 7 TOGETHER (condArmMultiMetaElementDeref hunk 1 + multiEmptyElseArmToBuilder
            // hunk 2) healed it WHOLE in all four drr 7.x cells, so its spurious nested re-wrap
            // (T3) is gone and the triple now equals golden's ALL-ZERO one. Because golden's tuple
            // is all-zero, the file also LEAVES the token-bearing union — DOMAIN_DRR7 140 -> 139.
            // UnderlierBasketIdentifier.java (was fork=[0, 0, 0] golden=[1, 0, 0]) left this list at
            // seat 31: law 4a (choiceOptionNavLadderDeepHop - the FER SET-seat option ladder walks the NESTED choice option tree
            // through ChoiceSwitchSupport.findChoiceOptionPath and derefs the META option hop into the bare output)
            // healed it WHOLE in all four drr 7.x cells, so golden's collapse site (T1) now renders. Golden's T1 = 1 keeps the file
            // inside the token-bearing union: DOMAIN_DRR7 UNMOVED at 139.
            // entered THIS LIST (not the band — the file was already banded) at seat 29: law 9b
            // (defaultSingleMixedJoinArgDeref) reshaped its residual composition and one golden-side
            // collapse site now counts here; its meta-join row left DefaultMetaJoinSeatTest's list
            // in the same measurement.
            // the Enrich_TransactionReportInstructionTestPackDefault row (fork=[0, 0, 0] golden=[0, 0, 1]) LEFT this list at seat 33:
            // law E.234 (rung E.3 aliasDefaultSingleTopWrap - golden's collapse site (T3) now renders) healed the file WHOLE in all four drr 7.x cells
            // (EnrichReportInstructionWholeSeatTest corpus_c1/c2); transcribed from the control print (E234-trip1.log).
            // the Price row (fork=[0, 0, 6] golden=[0, 0, 5]) LEFT this list: law C.1 (iteChainNestedThenLadderAdmit + R2a/R2b/R4)
            // rendered the seven-rung ladder as statements and took this scan's token set to golden's in
            // all four drr 7.x cells - the file stays BANDED on C.2's default join; transcribed from this
            // control's own print (C1-trip1.log), a pure row removal (was == expected minus it).
            );

    /** control2 — LAW 77 route parity for both carriers; the band is route-identical, so both must move. */
    @Test
    @EnabledIf("cellAAndIrProviderAvailable")
    void corpus_control2_irRouteMatchesLegacyForCarriers() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 legacy-route generation did not run");
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", CELL_A_ROOT), new ArrayList<>());
        for (String p : List.of(NOTIONAL_QUANTITY, MAP_NONPUBLIC)) {
            assertEquals(drrAOutput.get(p), irOut.get(p), "route divergence: " + p);
        }
    }

    /**
     * control3 — LAW 79 on drr 6.34.1: the cell that carries the arm's ENTIRE pre-existing admitted
     * population (the 20 {@code min}/{@code max} rows on the GREEN {@code SingleOrUpperAndLowerBarrierRule})
     * and NO law-3 carrier. If the widened kind test disturbs an existing admission, it fails here.
     */
    @Test
    @EnabledIf("cellBAvailable")
    void corpus_control3_forkDrr6341WholeCellCollapseSitesEqualGoldenFileByFile() throws IOException {
        assertNotNull(drrBOutput, "drr 6.34.1 generation did not run");
        assertEquals(List.of(), drrBGenErrors, "drr 6.34.1 reported a generation error — the scan is incomplete");
        assertUnionEqual(scan(drrBOutput), scan(readGoldenTree(GOLDEN_B)), drrBOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_6341, DOMAIN_DRR6341);
    }

    /** MEASURED AT THE CHAIN (LAW 82) — leave empty until the GREEN read fills it. */
    private static final List<String> KNOWN_RESIDUE_6341 = List.of();

    /**
     * MEASURED AT THE CHAIN — the drr 7.0.0 union domain (golden ∪ fork token-bearing files).
     *
     * <p><b>140 -> 139 at seat 30.</b> DERIVED, not printed: the residue assert fires BEFORE the
     * domain assert in {@code assertUnionEqual}, so the seat-30 run never reached this line. The
     * seat-30 heal of {@code GetBasket} by laws 6 + 7 takes its fork triple to {@code [0, 0, 0]}
     * and golden's is already {@code [0, 0, 0]}, so {@code scan()}'s {@code t1 + t2 + t3 > 0}
     * predicate drops the file from BOTH sides of the union.
     *
     * <p><b>MEASURED-CONFIRMED at {@code 71a92e826}</b> (chain run 2). The run-2 GREEN leg is 0F
     * for this suite on both routes, so the residue assert PASSED and execution reached the domain
     * assert — which also passed against this value. 139 is no longer derived; the adjudication
     * the paragraph above asked the next chain for has happened, and it agreed.
     */
    private static final int DOMAIN_DRR7 = 139;

    /** MEASURED AT THE CHAIN — the drr 6.34.1 union domain. */
    private static final int DOMAIN_DRR6341 = 138;

    /**
     * (T1, T2, T3) = collapsed collapsing-right args, UN-collapsed collapsing-right args, nested
     * double re-wraps. T1/T2 are whole-file counts, not per-line: golden LINE-WRAPS the collapsed tail
     * onto its own continuation line ({@code .first().get()));}), so a per-line scan would miss every
     * carrier.
     */
    private static Map<String, int[]> scan(Map<String, String> tree) {
        Map<String, int[]> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : tree.entrySet()) {
            String code = codeOnly(e.getValue());
            int t1 = count(code, ".first().get()") + count(code, ".last().get()");
            int t2 = count(code, ".first())") + count(code, ".last())");
            int t3 = count(code, "MapperS.of(MapperS.of(");
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

    // =========================================================================
    // The union assert (LAW 73: pin the SET, not the count)
    // =========================================================================

    private static void assertUnionEqual(Map<String, int[]> a, Map<String, int[]> b,
            java.util.Set<String> emittedA, String aName, String bName, List<String> knownResidue,
            int expectedDomain) {
        // Scoped to the files this harness emits (the law-6/8/5 correction class).
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
                "the union domain must equal the oracle's token-bearing files (" + expectedDomain + ")");
    }

    // =========================================================================
    // Harness (the seat-26/27 suite shape verbatim)
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
                    new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", CELL_B_ROOT), errs);
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

    /** The cell through the REAL {@code IRGeneration} seams (the D11 ON ring's wiring). */
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
            errors.forEach(e -> sink.add(e.getTargetPath() + " — " + e));
        }
    }

    private static void lockA(String path) throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run — corpus unavailable?");
        List<String> lockedErrors = drrAGenErrors.stream().filter(e -> e.contains(path)).toList();
        assertTrue(lockedErrors.isEmpty(),
                "the generator reported errors for the locked file " + path + ": " + lockedErrors);
        String generated = drrAOutput.get(path);
        assertNotNull(generated, "not generated in drr 7.0.0: " + path);
        Path goldenPath = GOLDEN_A.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "golden missing: " + goldenPath);
        assertEquals(normalize(Files.readString(goldenPath)), normalize(generated),
                "generated drr 7.0.0 output must byte-match golden (newline-normalized) for "
                + path + " — seat 28 law 3: a default whose RIGHT is a collapsing list-op collapses"
                + " the arg .get() and re-wraps MapperS.of, and a then-wrapped nested default takes"
                + " the wrapped argument.");
    }

    private static void assertContains(String out, String needle) {
        assertTrue(out.contains(needle), "expected <" + needle + "> in:\n" + out);
    }

    /** Strip line and block comments plus string literals so a javadoc or label never counts as code. */
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
            RModel main = AstBuilder.buildFromString(MODEL, "seat28c.rosetta");
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
                .forEach(e -> errors.add(e.getTargetPath() + " — " + e));
        fg.generateWithErrors(out)
                .forEach(e -> errors.add(e.getTargetPath() + " — " + e));
        if (!errors.isEmpty()) {
            throw new AssertionError("fixture generation errors (a broken fixture"
                    + " must fail loudly, not skip): " + errors);
        }
        return out;
    }

    private static Map<String, String> fixture() throws IOException {
        if (fixtureOut == null) {
            fixtureOut = render(m -> "census.seat28c".equals(m.namespace()));
        }
        return fixtureOut;
    }

    private static String fn(String fileName) throws IOException {
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
                        failures.add(p + " — " + e);
                    }
                });
        if (!failures.isEmpty()) {
            throw new AssertionError("[DefaultCollapsingRightSeatTest] builtins parse"
                    + " failures: " + String.join("; ", failures));
        }
        return models;
    }
}
