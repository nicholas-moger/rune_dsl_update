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
 * SEAT 29, law 4 -- facet {@code setTerminalReparentedRuleMulti}: <b>a reporting rule whose body
 * is an {@code extract} over a provably MULTI receiver reads {@code .getMulti()} at the
 * whole-output terminal, even though the extract's own BODY is single</b>.
 * {@code FunctionExpressionRenderer.isMultiToMultiSet}'s rule arm read only
 * {@code CollectionHandler.isBodyMulti(termExtract)} and never the receiver; law 4 widens that
 * ONE conjunct to {@code isBodyMulti(termExtract) || NavigationHandler.ruleOutputProvesMulti(
 * expression, compiler)}.
 *
 * <p><b>The golden vs fork shape</b> (drr 7.0.0 iosco cde version1
 * {@code quantity/reports/NotionalQuantityScheduleRule}, the file's ONLY hunk, a single token):
 * <pre>
 * golden:  output = toBuilder(MapperC.&lt;ValuePeriod&gt;of(...).mapItem(item -&gt; { ... }).getMulti());
 * fork:    output = toBuilder(MapperC.&lt;ValuePeriod&gt;of(...).mapItem(item -&gt; { ... }).get());
 * </pre>
 *
 * <p><b>The arm's KEY is untouched; only its PROOF changes.</b> The arm already reads
 * {@code findEnclosingRule(EXPRESSION)} (the probe's {@code ruleOfExpr}) and
 * {@code expression instanceof RExtractExpr} (the probe's {@code exprKind}) -- which is exactly
 * the reparented-case key. What it lacked was the RECEIVER read.
 *
 * <p><b>The chartered law is REFUTED-AS-SITED, and is NOT implemented (LAW 72).</b> The charter
 * was "give {@code isMultiToMultiSet}'s rule arm at :16668-16671 the same {@code chainProvesMulti}
 * OR-fallback the function path has at :16696". <b>That arm is DEAD.</b> It is keyed on
 * {@code findEnclosingRule(OPERATION)}, and {@code PROBE29-F11t} measured {@code ruleOfOp = false}
 * on <b>31,413 / 31,413 rows</b> corpus-wide -- the #229 reparented-function gap the comment
 * above the arm names is not an occasional miss but total. Any counterfactual built on it moves
 * NOTHING ({@code MOVED = 0}, both routes). {@code b2} pins that arm's deadness structurally.
 *
 * <p><b>The gate scoring (LAW 75, over the 24,098 {@code setTerminal} rows, both routes).</b>
 * <pre>
 * gate                                                       MOVED  movBand  movGreen  grpGreen
 * T0 probe cf: ruleOfExpr &amp;&amp; chainProvesMulti                  140       12       128        81
 * T1 re-key the dead arm to ruleOfExpr (ruleOutputProvesMulti)  140       12       128        81
 * T2 T1 AND chainProvesMulti                                    140       12       128        81
 * T6 widen THIS arm to isBodyMulti || ruleOutputProvesMulti       4        4         0         0  &lt;-- THE LAW
 * </pre>
 * <b>{@code exprKind} is the separating field, and it is a field the arm already reads.</b> The
 * 12 moved BAND rows split into two vectors, and only ONE of them --
 * {@code exprKind=RExtractExpr, liftedWrapper=null} -- is unique to the band; the other
 * ({@code exprKind=RSymbolReference, liftedWrapper=C}) is SHARED with all 128 green rows. So the
 * {@code RExtractExpr} conjunct is what keeps the 81 green groups out:
 * {@code BasketConstituentIdentifier}, {@code BasketConstituents}, {@code NotionalAmountSchedule},
 * {@code NotionalQuantityScheduleLeg1/Leg2}, {@code PriceSchedule}, {@code StrikePriceSchedule}
 * and 7 more. Scored at BOTH terminal seats and BOTH routes, the law measures
 * {@code MOVED=8 band / 0 green} at the {@code isMultiToMultiSet} producer and
 * {@code MOVED=4 band / 0 green} at the {@code setTerminal} emission -- identical OFF and ON.
 *
 * <p><b>No new predicate is invented.</b> {@code ruleOutputProvesMulti} is the receiver read the
 * dead arm below already names, and this is the #274 two-halves pattern again: the rule's output
 * is MULTI, so the terminal reads {@code .getMulti()}. Monotone add-only and green-safe by
 * construction -- {@code .get()} on a multi value assigned to a {@code List} output never
 * compiled, so every carrier was already a waivered mismatch (the standing multi-output argument).
 *
 * <p><b>LAW-2 INTERACTION -- INDEPENDENT for this carrier, MEASURED.</b> This seat's law 2 patches
 * {@code NavigationHandler.ruleOutputProvesMulti}, which is the callee of law 4's new disjunct, so
 * the interaction was checked rather than assumed:
 * <ul>
 *   <li>The {@code PROBE29-F10sig} row for {@code drr/7.0.0 rule:NotionalQuantitySchedule} reads
 *       {@code declaredMulti=true engineRuleBody=MULTI engineGlobal=MULTI wholeProvesMulti=true
 *       bodyKind=RExtractExpr defaultSeen=false leftKind=- cfMulti=false}. The proof is ALREADY
 *       true through the base path, and law 2's {@code default}-on-the-spine disjunct does not
 *       engage at all ({@code defaultSeen=false}, {@code cfMulti=false}). Law 4's four heals do
 *       not depend on law 2, and law 2 cannot widen what law 4 proves here.</li>
 *   <li>The converse was checked too: law 2's own flip carrier,
 *       {@code IndicatorOfTheUnderlyingIndex}, has 24 {@code PROBE29-F11t} rows and <b>all 24 are
 *       {@code seat=thenTerminal}</b> -- ZERO at {@code setTerminal} or {@code multiToMultiSet}.
 *       So law 4's new consult site cannot amplify law 2's flip on that rule either.</li>
 *   <li>The RESIDUAL interaction, stated not measured: law 4 makes
 *       {@code ruleOutputProvesMulti} reachable from a seat that never consulted it, so ANY future
 *       widening of that predicate now also reaches this terminal. Per the charter order (law 2
 *       lands before law 4), the whole-matrix measurement taken at law 4's head already carries
 *       law 2, so the joint effect is measured, not projected. {@code corpus_control1} is the
 *       control that would catch it.</li>
 * </ul>
 *
 * <p><b>RED at the pre-law head -- MEASURED at the seat-29 chain's RED leg ({@code fa277393},
 * identical on BOTH routes)</b>: a1, a2, corpus_c1, corpus_control1 (a1 and a2 were rendered
 * at the pre-law head and both emit the single-arity terminal). b1, b2 GREEN in both states;
 * corpus_control0 GREEN in both states (it reads golden only).
 *
 * <p><b>MEASURED MUTATIONS (LAW 82 -- the seat-29 chain's suite-lane loop at {@code 687c8feb};
 * the set below is the RECORDED failing set from the f29-mut-m-law4 log):</b>
 * <ul>
 *   <li><b>m-law4</b> ({@code law4-apply29.py --revert}: the whole disjunct removed) -- MEASURED
 *       a1, a2, corpus_c1, corpus_control1 (4F -- exactly the drafted claim); b1/b2/control0
 *       unmoved as designed.</li>
 * </ul>
 * <p><b>Designed manual sever -- stated, NOT run in the seat-29 chain</b> (no apply-script flag;
 * its failing set remains a claim):
 * <ul>
 *   <li><b>m-law4-exprkind</b> (the {@code instanceof RExtractExpr} guard escaped, so the arm
 *       reads {@code (expression instanceof RExtractExpr e && isBodyMulti(e))
 *       || ruleOutputProvesMulti(expression, compiler)}) -- CLAIMED failing set:
 *       <b>corpus_control1 and corpus_control3 ONLY</b>, naming the 81 green groups. a1/a2/b1/b2
 *       are expected to PASS: the green exposure has no fixture-scale witness in this model (see
 *       the MODEL javadoc). This is declared UP FRONT so a measured all-fixtures-pass is read as
 *       the predicted result and not as a mutation that failed to fire; if the CORPUS controls
 *       also come back green, the conjunct is NOT load-bearing at this corpus and the law should
 *       be re-scored before merge.</li>
 * </ul>
 */
class SetTerminalRuleMultiSeatTest {

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

    /** Cell A = drr 7.0.0 -- the carrier cell (7.1/7.2/7.3 carry the same single-token row). */
    private static final Path CELL_A_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path GOLDEN_A = CELL_A_ROOT.resolve("rosetta-source/src/generated/java");
    /**
     * Cell B = drr 5.61.0 -- the OTHER reach cell. All 4 flip rows are drr 7.x, so this is the
     * EMPTY-DOMAIN guard; it is chosen over a cdm cell because it is dense in exactly the rule
     * shapes the 81 would-be-green groups live in ({@code NotionalAmountSchedule},
     * {@code PriceSchedule}, {@code BasketConstituent*}), so an {@code exprKind} slip shows up
     * here as a whole-cell mismatch rather than as silence.
     */
    private static final Path CELL_B_ROOT = Path.of("../test-corpus/drr/drr-5.61.0");
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
     * a1 = the reparented set-terminal shape, reduced from
     *      {@code iosco cde version1 NotionalQuantitySchedule}: the rule body is
     *      {@code <multi fn call> extract <constructor>} -- an {@code RExtractExpr} whose RECEIVER
     *      is provably MULTI and whose BODY is a single constructor. Terminal must be
     *      {@code .getMulti()}. <b>MEASURED RED at the pre-law head</b>: the fixture renders
     *      {@code MapperC.<Seat29Entry>of(seat29Entries.evaluate(input)).mapItem(item -> ...).get()},
     *      the carrier's shape token for token.
     * a2 = the same vector with a NAV body instead of a constructor
     *      ({@code <multi fn call> extract <single nav>}). A second, structurally distinct carrier
     *      of the ONE conjunct: if a1 alone moved, the law could be mis-read as constructor-shaped.
     * b1 = the {@code exprKind} pin: the SAME chain written {@code then extract} instead of
     *      {@code extract} is an {@code RThenExpr}, so the arm must not apply and its bytes must
     *      not move. (Measured: it already reads {@code .getMulti()} through the pre-existing
     *      path, so the assertion is byte-STABILITY, not a token.)
     * b2 = the DEAD arm pinned structurally: an extract whose lambda BODY is multi is admitted by
     *      the PRE-EXISTING {@code isBodyMulti} half, so it must be {@code .getMulti()} in both
     *      states -- witness-uniqueness for a1 depends on a1's body being SINGLE, and b2 is what
     *      proves the two halves of the disjunct are distinguishable at all.
     *
     * <p><b>What this fixture set can and cannot prove (LAW: a control scans the domain it
     * claims).</b> The 128 green rows / 81 green groups a looser gate admits are a CORPUS-scale
     * phenomenon: probing this reduced model, every non-{@code RExtractExpr} multi-output rule
     * terminal already reads {@code .getMulti()} through the {@code isBodyMulti} half or is a
     * direct assignment with no Mapper terminal at all, so the green exposure has no fixture-scale
     * witness. b1 therefore pins the SHAPE (an {@code RThenExpr} terminal is untouched) and the
     * 81-group exposure is pinned by {@code corpus_control1} and {@code corpus_control3}, whose
     * domains contain those families by construction. The {@code m-law4-exprkind} mutation is
     * expected to be caught by the CORPUS controls, not by b1 -- stated here rather than
     * discovered when the mutation measures a smaller set than claimed.
     *
     * <p>Lexer-safe identifiers: no {@code tag}, {@code single} or {@code label}; the date-ish
     * attribute is {@code startsOn}.
     */
    private static final String MODEL = """
            namespace census.seat29f11t
            version "1.0.0"

            type Seat29Entry:
                amount number (0..1)
                startsOn date (0..1)

            type Seat29Period:
                amount number (0..1)
                startsOn date (0..1)

            type Seat29Leg:
                entries Seat29Entry (0..*)
                marker boolean (0..1)

            func Seat29Entries: <"EnrichDatedValueWithEndDate twin - a MULTI-output function call as the extract RECEIVER">
                inputs:
                    leg Seat29Leg (1..1)
                output:
                    picked Seat29Entry (0..*)
                set picked:
                    leg -> entries

            func Seat29LegOk: <"the chain-head filter predicate">
                inputs:
                    leg Seat29Leg (1..1)
                output:
                    result boolean (1..1)
                set result:
                    leg -> marker = True

            reporting rule A1ReparentedSetTerminal from Seat29Leg: <"a1 - THE NotionalQuantitySchedule SHAPE: <multi fn call> extract <constructor> - the receiver is MULTI, the extract body is SINGLE, so only the receiver read can prove the terminal">
                Seat29Entries(item)
                    extract
                        Seat29Period {
                            amount: amount,
                            startsOn: startsOn
                        }
                as "a1"

            reporting rule A2ReparentedNavBody from Seat29Leg: <"a2 - the SAME vector with a NAV body instead of a constructor: <multi fn call> extract <single nav>">
                Seat29Entries(item)
                    extract amount
                as "a2"

            reporting rule B1ThenPipeTerminalUnmoved from Seat29Leg: <"b1 - the exprKind pin: the SAME chain written `then extract` is an RThenExpr, NOT an RExtractExpr, so law 4's arm must not apply and the bytes must not move">
                Seat29Entries(item)
                    then extract amount
                as "b1"

            reporting rule B2BodyMultiTerminalAlreadyMulti from Seat29Leg: <"b2 - the PRE-EXISTING isBodyMulti half: an extract whose BODY is multi was already admitted by #275 and must be .getMulti() in BOTH states">
                extract Seat29Entries(item)
                as "b2"
            """;

    // =========================================================================
    // Part A -- the positive fixture
    // =========================================================================

    /**
     * a1 -- the reparented carrier shape. The rule body is an {@code RExtractExpr} whose receiver
     * is a MULTI function call and whose body is a single constructor, so
     * {@code CollectionHandler.isBodyMulti} is FALSE and the arm's pre-law proof declines. The
     * rule OUTPUT is multi, so the whole-output terminal must be {@code .getMulti()}.
     *
     * <p>Witness-uniqueness: the assertion keys on {@code .getMulti()}, the token the flip ADDS,
     * and on the absence of the {@code .get())} terminator the flip REMOVES -- never on the
     * presence of {@code output =}, which is there in both states.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_reparentedRuleExtractTerminalReadsGetMulti() throws IOException {
        String code = collapse(codeOnly(rule("A1ReparentedSetTerminalRule.java")));
        assertTrue(code.contains(".getMulti());"),
                "the reparented rule-extract whole-output terminal must read .getMulti():\n" + code);
        // The REMOVED token, quoted from the measured pre-law render: the constructor body's
        // `.build()))` closes the mapItem lambda, so the single-arity terminal reads
        // `.build())).get());`. Keying on a shape this specific is what keeps the negative
        // assertion from passing vacuously in BOTH states.
        assertTrue(!code.contains(".build())).get());"),
                "the single-arity terminal must be gone:\n" + code);
    }

    /**
     * a2 -- the SAME conjunct on a structurally different body. The receiver is the same MULTI
     * function call, but the extract body is a NAV rather than a constructor, so the law is shown
     * to turn on the RECEIVER read and not on the body's shape. Measured RED at the pre-law head:
     * {@code MapperC.<Seat29Entry>of(...).mapItem(item -> item.<BigDecimal>map("getAmount", ...)).get()}
     * against a {@code List<BigDecimal>} output.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_reparentedRuleExtractWithNavBodyAlsoReadsGetMulti() throws IOException {
        // The output here is a plain List<BigDecimal>, so there is no toBuilder(...) wrap and the
        // terminal closes the STATEMENT rather than an argument -- `.getMulti();`, not
        // `.getMulti());`. Quoted from the measured pre/post texts, not assumed.
        String code = collapse(codeOnly(rule("A2ReparentedNavBodyRule.java")));
        assertTrue(code.contains(".getMulti();"),
                "the nav-body twin of the carrier vector must also read .getMulti():\n" + code);
        assertTrue(!code.contains(")).get();"),
                "the single-arity terminal must be gone:\n" + code);
    }

    // =========================================================================
    // Part B -- the decline pins
    // =========================================================================

    /**
     * b1 -- the {@code exprKind} pin. The SAME chain as a2, written {@code then extract} instead
     * of {@code extract}, is an {@code RThenExpr} at the terminal seat, so law 4's arm -- which
     * requires {@code expression instanceof RExtractExpr} -- must not apply to it.
     *
     * <p>This rule already reads {@code .getMulti()} through the pre-existing path, so the
     * assertion cannot be witness-unique on a token; it is a byte-STABILITY pin instead, and the
     * whole-output assignment is compared against its measured pre-law text. The 81 green groups a
     * looser gate admits have no fixture-scale witness (see the MODEL javadoc); they are pinned by
     * {@code corpus_control1} / {@code corpus_control3}.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b1_thenPipeRuleTerminalIsUnmoved() throws IOException {
        // RAW text, not codeOnly: the pinned line carries the "getAmount" string literal that
        // codeOnly deliberately strips.
        String raw = collapse(rule("B1ThenPipeTerminalUnmovedRule.java"));
        assertTrue(raw.contains("output = thenArg .mapItem(item -> item"
                + ".<BigDecimal>map(\"getAmount\", seat29Entry -> seat29Entry.getAmount()))"
                + ".getMulti();"),
                "an RThenExpr rule terminal must be byte-unmoved by law 4:\n" + raw);
    }

    /**
     * b2 -- the pre-existing {@code isBodyMulti} half. #275 admitted this shape (an extract whose
     * lambda BODY is multi) and it must be {@code .getMulti()} both before and after law 4:
     * the law is an OR-widening, so the half that already fired keeps firing.
     *
     * <p>This is also the structural pin for the REFUTED charter (LAW 72): the dead arm at
     * {@code isMultiToMultiSet}:16668-16671 is keyed on {@code findEnclosingRule(OPERATION)} and
     * measured false on 31,413/31,413 rows. If a future edit "fixes" the terminal by reviving that
     * arm instead of widening this one, a1 would pass while this suite's mutation lane loses its
     * discrimination -- so b2 fixes the pre-law baseline that a1's delta is measured against.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b2_bodyMultiRuleTerminalIsUnchanged() throws IOException {
        String code = collapse(codeOnly(rule("B2BodyMultiTerminalAlreadyMultiRule.java")));
        assertTrue(code.contains(".getMulti()"),
                "the pre-existing isBodyMulti half must still admit this shape:\n" + code);
    }

    // =========================================================================
    // Part C -- the corpus carrier (4 whole-file rows; drr 7.0.0 shown, 7.1/7.2/7.3 identical)
    // =========================================================================

    private static final String NQS =
            "drr/standards/iosco/cde/version1/quantity/reports/NotionalQuantityScheduleRule.java";

    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_notionalQuantityScheduleRuleByteIdentical() throws IOException {
        lockA(NQS);
    }

    /**
     * control0 -- golden is the oracle (prove the instrument can fail). Golden's
     * {@code NotionalQuantityScheduleRule} carries the multi-arity terminal and NOT the
     * single-arity one, and a named green sibling is pinned as a no-move witness -- so the oracle
     * discriminates rather than merely agreeing.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control0_goldenCarriesGetMultiAtTheCarrierAndGetAtTheGreenSibling()
            throws IOException {
        String g = collapse(Files.readString(GOLDEN_A.resolve(NQS)));
        assertTrue(g.contains(".getMulti());"),
                "golden must read .getMulti() at the carrier's whole-output terminal");
        assertTrue(!g.contains("}).get());"),
                "golden must NOT carry the single-arity terminal at the carrier");

        // The green sibling (the exprKind=RSymbolReference liftedWrapper=C vector). Its
        // whole-output terminal DOES read .getMulti() in golden - supplied by the #275
        // termGetMulti signal on its mapSingleToList body, NOT by this law's arm (the probe
        // row reads chosen=get at this seat while the emitted text is already multi) - so
        // the drafted "sibling keeps .get()" claim was WRONG and is replaced by the two
        // facts that ARE golden-stable: the sibling's terminal line is pinned verbatim, and
        // the FORK emits the sibling byte-identically (the no-move witness: an over-fire of
        // this law onto the RSymbolReference vector moves these bytes and fails here; the
        // conjunct's load-bearing proof lives in the m-law4 mutation lane + control1).
        Path sibling = GOLDEN_A.resolve(
                "drr/regulation/asic/rewrite/trade/reports/PriceScheduleRule.java");
        assertTrue(Files.isRegularFile(sibling),
                "the green-sibling oracle must exist: " + sibling);
        String gs = collapse(Files.readString(sibling));
        assertTrue(gs.contains(".mapSingleToList(item -> MapperC.<PricePeriod>of("
                + "getReportablePricePeriod.evaluate(item.get(),"
                + " DefaultingType.DEFAULT_PERCENTAGETO_DECIMAL))).getMulti());"),
                "the sibling's pinned terminal line must hold in golden");
        String forkSibling = drrAOutput == null ? null
                : drrAOutput.get("drr/regulation/asic/rewrite/trade/reports/PriceScheduleRule.java");
        assertNotNull(forkSibling, "the fork must emit the green sibling");
        assertEquals(gs, collapse(forkSibling),
                "the green sibling must stay byte-identical (the no-move witness)");
    }

    /**
     * control1 -- LAW 79, the whole-cell UNION scan on drr 7.0.0. The 81 would-be-green groups a
     * looser gate admits are spread across the cell, and a carrier-scoped lock cannot see them;
     * this can.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control1_forkDrr7WholeCellTerminalsEqualGoldenFileByFile() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");
        assertEquals(List.of(), drrAGenErrors,
                "drr 7.0.0 reported a generation error - the scan is incomplete");
        assertUnionEqual(scan(drrAOutput), scan(readGoldenTree(GOLDEN_A)), drrAOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_DRR7, DOMAIN_DRR7);
    }

    /**
     * control2 -- LAW 77 route parity. Law 4 sits in {@code FunctionExpressionRenderer}, the class
     * the IR route substitutes, so route parity is the load-bearing control for this seat rather
     * than a formality: the probe measured both routes identical ({@code MOVED=8/4 band, 0 green}
     * OFF and ON) and this re-proves it per FILE on the real IR seams.
     */
    @Test
    @EnabledIf("cellAAndIrProviderAvailable")
    void corpus_control2_irRouteMatchesLegacyForTheCarrier() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 legacy-route generation did not run");
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", CELL_A_ROOT), new ArrayList<>());
        assertEquals(drrAOutput.get(NQS), irOut.get(NQS), "route divergence: " + NQS);
    }

    /**
     * control3 -- LAW 79 on the OTHER reach cell, drr 5.61.0: the EMPTY-DOMAIN guard. All 4 flip
     * rows are drr 7.x, so the law must be byte-inert here, and the cell is dense in the
     * {@code PriceSchedule} / {@code NotionalAmountSchedule} / {@code BasketConstituent*} families
     * whose vector a dropped {@code exprKind} conjunct would move.
     */
    @Test
    @EnabledIf("cellBAvailable")
    void corpus_control3_forkDrr561WholeCellTerminalsEqualGoldenFileByFile() throws IOException {
        assertNotNull(drrBOutput, "drr 5.61.0 generation did not run");
        assertEquals(List.of(), drrBGenErrors,
                "drr 5.61.0 reported a generation error - the scan is incomplete");
        assertTrue(drrBOutput.keySet().stream()
                        .anyMatch(k -> k.endsWith("/PriceScheduleRule.java")),
                "the green vector's family must be INSIDE this scan's domain, else control3 proves"
                + " nothing about it (LAW: a control scans the domain it claims)");
        assertUnionEqual(scan(drrBOutput), scan(readGoldenTree(GOLDEN_B)), drrBOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_DRR561, DOMAIN_DRR561);
    }

    /**
     * The NAMED residue of drr 7.0.0 (LAW 73: pin the SET, not the count) -- MEASURED at the
     * seat-29 chain ({@code 687c8feb}: suite GREEN with this set on both routes).
     *
     * <p><b>RE-MEASURED at the seat-30 chain head {@code e223ce19}</b> — 5 entries, transcribed
     * VERBATIM from this control's own failing print (LAW 81), down from 7. Both departures keep a
     * non-zero GOLDEN tuple, so both files stay inside the union domain and {@code DOMAIN_DRR7} is
     * UNMOVED at 1231.
     */
    private static final List<String> KNOWN_RESIDUE_DRR7 = List.of(
            // GetBasket.java (was fork=[0, 0, 1] golden=[1, 0, 1]) left this list at seat 30: laws
            // 6 + 7 TOGETHER (condArmMultiMetaElementDeref hunk 1 + multiEmptyElseArmToBuilder
            // hunk 2) healed it WHOLE in all four drr 7.x cells, so its missing whole-output SET
            // terminal (T1) now renders.
            // the GetBasketConstituents row (fork=[0, 0, 7] golden=[0, 1, 7]) LEFT this list: law A.1
            // (ctorSetterMetaDerefFunctionHost + the rung-3 ctorSetterHoistTextOrder numbering) took its tuples to
            // golden's in all four drr 7.x cells; the file stays BANDED on law A.2's lambda-name line, which this
            // tuple set cannot see; transcribed from this control's own print (A1-trip1.log).
            // the IndicatorOfTheUnderlyingIndexRule row (fork=[0, 1, 1] golden=[0, 1, 2]) LEFT this list at seat 32: law D.2
            // (extractBodyMultiDefaultTernary, on law D.1's left deref) healed it WHOLE in all four drr 7.x cells;
            // transcribed from this control's own print (D2-trip1.log).
            // CommodityQuantityWithFrequency.java (was fork=[0, 1, 0] golden=[0, 2, 0]) left this
            // list at seat 30: law 4 (the d=4 function-level all-or-nothing decline at the RThenExpr
            // inline fallback, CollectionHandler.tryDeepThenHoist) healed it WHOLE in all four
            // drr 7.x cells, so its second T2 terminal now renders.
            // the Price row (fork=[0, 3, 0] golden=[0, 3, 1]) LEFT this list: law C.2 (heteroMetaDefaultJoinDeref + iteArmMultiDefaultTernary)
            // took the rung-1 default join, the last residue after C.1 - Price is WHOLE in all four drr 7.x cells;
            // transcribed from this control's own print (C2-trip1.log), a pure row removal.
            // the QuantityUnitOfMeasure row (fork=[0, 2, 0] golden=[0, 1, 0]) LEFT this list: law B.3
            // (bareRuleRefFunctionHost) - B.1's disclosed two-terminal overshoot resolved once the
            // filter lambda block-converted, and the terminal token set now equals golden's; from
            // B3-trip1.log, a pure row removal (was == expected minus it).
            // LAW 81 re-pin (seat 33, law B.1): fork=[0, 0, 0] -> fork=[0, 2, 0] - the enabler's k>0 window moved this scan's fork side; the file stays BANDED (B.3 + B.24); from B1-trip1.log.
            // the TotalNotionalQuantity row (fork=[0, 0, 0] golden=[0, 1, 0]) LEFT this list: law D.3 (fnDeepCondBaseConfinedArmChainAdmit,
            // seven rungs) healed the file WHOLE in all four drr 7.x cells; transcribed from this control's own
            // print (D3-trip1.log), a pure row removal (was == expected minus it).
            );

    /**
     * The NAMED residue of drr 5.61.0 (LAW 73) -- MEASURED at the seat-29 chain
     * ({@code 687c8feb}). The law's own contribution here must be EMPTY (the empty-domain guard).
     */
    private static final List<String> KNOWN_RESIDUE_DRR561 = List.of(
            // the four cftc Notional* rows LEFT this list at seat 32: law C.3 (ruleThenArmLadderNestedTreeAdmit) healed
            // NotionalAmountLeg1/2 + NotionalCurrencyLeg2 WHOLE in drr 5.61.0 and IMPROVED NotionalCurrencyLeg1 to a tuple this scan reads as golden's; transcribed from this control's own print (C3-trip1*.log).
            );  // LAW 81 (seat 33, law F.B): the NotionalLeg2Rule row LEFT - the file is WHOLE (FB-trip1.log print)
    // LAW 81 re-pin (v3.1 flip seat 31, law 1b): the mas PlatformIdentifierRule row LEFT
    // this list - the file healed WHOLE (byte-identical to golden, locked by
    // MapperFormRuleRootArmSeatTest corpus_c1). Re-pinned from the control's own measured
    // print (delta = the one row REMOVED, nothing else).

    /**
     * The union domain is MEASURED and pinned (LAW 73) - a negative value means an unpinned call
     * site. Set from the chain's control1 output at the law-4 head.
     */
    private static final int DOMAIN_DRR7 = 1231;

    /**
     * The union domain is MEASURED and pinned (LAW 73) - a negative value means an unpinned call
     * site. Set from the chain's control3 output at the law-4 head.
     */
    private static final int DOMAIN_DRR561 = 851;

    /**
     * (T1, T2, T3) per file:
     * <ul>
     *   <li><b>T1</b> -- the law's ADDED shape: whole-output SET terminals reading
     *       {@code .getMulti());} in the whitespace-collapsed code.</li>
     *   <li><b>T2</b> -- the law's REMOVED shape: whole-output SET terminals reading
     *       {@code .get());}. The carrier moves T1 +1 / T2 -1; every other file must match golden
     *       exactly.</li>
     *   <li><b>T3</b> -- the OVER-FIRE NET: the file's TOTAL {@code .getMulti()} count.
     *       Deliberately global (not terminal-scoped), so a spurious multi-arity read ANYWHERE in
     *       the cell fails the control, including at seats this law does not name.</li>
     * </ul>
     */
    private static Map<String, int[]> scan(Map<String, String> tree) {
        Map<String, int[]> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : tree.entrySet()) {
            String flat = collapse(codeOnly(e.getValue()));
            int t1 = count(flat, ".getMulti());");
            int t2 = count(flat, ".get());");
            int t3 = count(flat, ".getMulti()");
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
    // The union assert (LAW 73: pin the SET, not the count)
    // =========================================================================

    /**
     * The scan universe is the token-bearing union INTERSECTED with the files this harness
     * actually emits (rule/report/function kinds): whether the fork emits every expected FILE is
     * the D11 ring's question (missingOutput), not this suite's.
     */
    private static void assertUnionEqual(Map<String, int[]> a, Map<String, int[]> b,
            java.util.Set<String> emittedA, String aName, String bName, List<String> knownResidue,
            int expectedDomain) {
        assertTrue(expectedDomain >= 0,
                "the union domain is MEASURED and pinned (LAW 73) - a negative value means an"
                + " unpinned call site");
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
                "the union domain must equal the emitted token-bearing files (" + expectedDomain + ")");
    }

    // =========================================================================
    // Harness (the seat-28 FilterPredicateMetaDerefSeatTest shape verbatim)
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
                    new D11CorpusRegressionTest.CellSpec("drr", "5.61.0", CELL_B_ROOT), errs);
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
            errors.forEach(e -> sink.add(e.getTargetPath() + " - " + e));
        }
    }

    private static void lockA(String path) throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run - corpus unavailable?");
        List<String> lockedErrors = drrAGenErrors.stream().filter(e -> e.contains(path)).toList();
        assertTrue(lockedErrors.isEmpty(),
                "the generator reported errors for the locked file " + path + ": " + lockedErrors);
        String generated = drrAOutput.get(path);
        assertNotNull(generated, "not generated in drr 7.0.0: " + path);
        Path goldenPath = GOLDEN_A.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "golden missing: " + goldenPath);
        assertEquals(normalize(Files.readString(goldenPath)), normalize(generated),
                "generated drr 7.0.0 output must byte-match golden (newline-normalized) for "
                + path + " - seat 29 law 4: a rule-extract whole-output terminal over a provably"
                + " MULTI receiver reads .getMulti().");
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
            RModel main = AstBuilder.buildFromString(MODEL, "seat29f11t.rosetta");
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
            fixtureOut = render(m -> "census.seat29f11t".equals(m.namespace()));
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
            throw new AssertionError("[SetTerminalRuleMultiSeatTest] builtins parse"
                    + " failures: " + String.join("; ", failures));
        }
        return models;
    }
}
