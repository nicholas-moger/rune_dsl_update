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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
 * SEAT 29, law 9 — facet {@code enumSwitchCaseBodyWiden} (family F15b): <b>an
 * {@code extract switch} whose SUBJECT is an enum ATTRIBUTE of the piped item renders the hoisted
 * {@code switchArgument} if-chain BLOCK, not the {@code Objects.equals(<bare name>, …)} ternary
 * chain</b>.
 *
 * <p><b>The golden vs fork shape</b> (drr 7.0.0
 * {@code drr/standards/iosco/cde/version1/price/reports/FirstExerciseDateRule.java}, ONE hunk that
 * replaces the whole 16-line block — {@code band28-files.txt}: {@code lines=20 hunks=1}, OFF and ON
 * rows identical):
 * <pre>
 * golden: .mapSingleToItem(item -&gt; {
 *             final OptionExerciseStyleEnum switchArgument = item.&lt;OptionExerciseStyleEnum&gt;map("getStyle", exerciseTerms -&gt; exerciseTerms.getStyle()).get();
 *             if (switchArgument == null) { return MapperS.&lt;Date&gt;ofNull(); }
 *             if (switchArgument == OptionExerciseStyleEnum.AMERICAN) {
 *                 final FieldWithMetaDate fieldWithMetaDate = trade.&lt;FieldWithMetaDate&gt;map("getTradeDate", _trade -&gt; _trade.getTradeDate()).get();
 *                 return MapperS.of(… .getOrDefault((fieldWithMetaDate == null ? null : fieldWithMetaDate.getValue())));
 *             }
 *             …
 *             if (switchArgument == OptionExerciseStyleEnum.BERMUDA) {
 *                 return MapperC.&lt;Date&gt;of(…)
 *                     .min();
 *             }
 *             return MapperS.&lt;Date&gt;ofNull(); })
 * fork  : .mapSingleToItem(item -&gt; Objects.equals(American, item.&lt;OptionExerciseStyleEnum&gt;map("getStyle", …)) ? … : null)
 * </pre>
 * {@code American} / {@code European} / {@code Bermuda} are BARE Rosetta names emitted as Java
 * identifiers — undefined symbols, so <b>LAW 74</b>: the PRE javac probe must show them and the
 * POST must exit 0.
 *
 * <p><b>The census framing is INCOMPLETE and this suite pins the correction.</b>
 * {@code v31-close-census.md} F15(b) says the {@code compileEnumSwitchBlockLambda} "admitted
 * case-body set is too narrow". Reading the emitter shows the case-body admit is the SECOND gate
 * and that FIVE gates decline on this carrier's path:
 * <ol>
 *   <li><b>G1</b> the SUBJECT enum ({@code NavigationHandler.implicitItemEnumeration} reads the
 *       enclosing extract's ARGUMENT — an {@code ExerciseTerms} data type, not an enum — while the
 *       switch's own {@code argument()} is the attribute {@code style}). <b>This declines FIRST</b>,
 *       before any case body is looked at;</li>
 *   <li><b>G2</b> the case-body kind set ({@code RStringLiteral} or fn-call only) declines
 *       {@code RDefaultExpr} ×2 and {@code RMinExpr};</li>
 *   <li><b>G3</b> the {@code caseBody instanceof JavaExpression} test, at risk for the AMERICAN arm
 *       whose meta-deref produces a hoist;</li>
 *   <li><b>G4</b> the {@code startsWith("MapperC")} arm test declines BERMUDA, whose golden text IS
 *       a MapperC chain — one that COLLAPSES through {@code .min()};</li>
 *   <li><b>G5</b> the {@code contains("\n")} arm test declines BERMUDA again (golden itself wraps
 *       the {@code .min()} onto the next line);</li>
 * </ol>
 * plus <b>R1</b> the {@code switchArgument} decl rendering {@code <param>.get()} where golden
 * renders the compiled subject navigation, and <b>R2</b> the absent per-arm statement emission.
 * Fixture a1 exercises all of them; a3 isolates G1+R1; a2 is the no-move witness for the emitter's
 * existing (#379) population.
 *
 * <p><b>The probe verdict this law answers (LAW 75).</b> {@code PROBE28-F15b} (the
 * {@code ControlFlowHandler} ternary-side dispatch, reached only after both hoist ladders and both
 * block lambdas decline) fires <b>EXACTLY 4 times in the whole corpus</b>, all on
 * {@code FirstExerciseDateRule}, {@code cases=3 argKind=RSymbolReference
 * caseBodyKinds=case:RDefaultExpr,case:RDefaultExpr,case:RMinExpr admitted=false}, byte-identical
 * on both routes — a MEASURED-ZERO over-fire at the decline destination.
 * {@code PROBE28-F15b2} (the statement-seat {@code switchArgument} if-chain, a DIFFERENT emitter)
 * fires 1,253 times with ZERO band carriers, and the two populations are disjoint
 * ({@code p28f15switch.py}: {@code BOTH (file,cell) = 0}). Golden-corpus scan: <b>0 of 179,209</b>
 * goldens carry {@code Objects.equals(<Capitalized bare>,}. PROBE29-F15b reconfirms both counts at
 * the seat head — the suite's claims are stated against PROBE28 until it does.
 *
 * <p><b>The in-cell negative-control family.</b> The cde <b>version2 / version3</b>
 * {@code FirstExerciseDateRule}s and the six {@code regulation/…/rewrite/trade/reports/} ones in the
 * SAME cells DELEGATE ({@code cdeV1.price.FirstExerciseDate} / {@code cdeV2.price.FirstExerciseDate})
 * and are GREEN — {@code corpus_control1} scans the whole cell, so an over-fire onto them fails
 * there.
 *
 * <p><b>RED at the pre-law head — MEASURED at the seat-29 chain's RED leg ({@code fa277393},
 * identical on BOTH routes)</b>: a1, a1b, a3, corpus_c1, corpus_control1.
 * a2 (the #379 item-subject carrier) and b1 (the decline pin) are GREEN in BOTH states — a2 is the
 * no-move witness for the emitter's existing population, b1 the witness-unique decline lock.
 *
 * <p><b>MEASURED MUTATIONS (LAW 82 — the seat-29 chain's suite-lane loop at {@code 687c8feb};
 * each lane = the named apply script(s) reverted, the suite run, re-applied; every set below is
 * the RECORDED failing set from the f29-mut logs):</b>
 * <ul>
 *   <li><b>m-law9</b> the whole pair reverted ({@code law9b-apply --revert} then
 *       {@code law9-apply --revert}) → MEASURED a1, a1b, a3, corpus_c1, corpus_control1 (5F);
 *       a2/b1 unmoved as designed</li>
 *   <li><b>m-law9b</b> the companion alone reverted ({@code law9b-apply --revert}) → MEASURED a1,
 *       a1b, corpus_c1 (3F); a3 AND corpus_control1 held — the base law alone keeps the
 *       whole-cell control green, so the companion's byte is carrier-scoped (the
 *       FirstExerciseDateRule mixed-join deref), not cell-wide</li>
 * </ul>
 * <p><b>The drafted per-component decomposition 9-i..9-v — design rationale, NOT separately
 * measured.</b> The apply scripts land each law as one unit and expose no per-component sever
 * flags, so the five drafted component claims (subject-enum resolution, case-body kind widening,
 * collapsing-MULTI admit, subject DECL, per-arm statement drain) were not individually run in the
 * seat's chain and are recorded as rationale only. The drafted 9-ii/9-iii same-set question is
 * likewise unresolved by measurement and travels with them.
 */
class EnumSwitchBlockWidenSeatTest {

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

    /** The carrier cell. */
    private static final Path CELL_A_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path GOLDEN_A = CELL_A_ROOT.resolve("rosetta-source/src/generated/java");
    /** The reach cell — the mechanism's OTHER population (the #379 emitter's own carriers). */
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
     * A1 = the {@code FirstExerciseDateRule} shape reduced: an {@code extract switch} whose SUBJECT
     * is an enum ATTRIBUTE of the piped item (not the item), with a {@code default} body whose
     * LEFT is a FN CALL and whose RIGHT operand is a {@code [metadata id]} leaf (the AMERICAN-arm
     * hoist — the fn-call left mirrors the measured carrier's
     * {@code AdjustableDateResolution(…) default trade -> tradeDate}; the deref rung's join reads
     * the left item through the callee output, so a nav-left default is OUTSIDE the evidence and
     * keeps its bytes), an already-admitted
     * fn-call body, and a collapsing {@code min} body over a MULTI fn call (the BERMUDA arm, whose
     * compiled text is MapperC-headed and multi-line). A2 = the #379 shape already admitted
     * (subject IS the item — the switch ELIDES its argument, string-literal bodies) — the no-move
     * witness. A3 = the subject half in isolation (an attribute subject with bodies the emitter
     * ALREADY admits, so only G1 and R1 can move it). B1 = the decline pin: a case body OUTSIDE the
     * widened kind set keeps the ternary.
     *
     * <p>{@code note} is used instead of {@code tag}, and {@code soleUnder} / {@code mark} instead
     * of {@code single} / {@code label} — all three are lexer keywords.
     */
    private static final String MODEL = """
            namespace census.seat29i
            version "1.0.0"

            enum StyleEnum:
                American
                European
                Bermuda

            enum VenueEnum:
                Admitted
                OffVenue

            type Terms:
                style StyleEnum (0..1)
                venue VenueEnum (0..1)
                note string (0..1)
                startDate date (0..1)
                allDates date (0..*)

            type Holder:
                terms Terms (0..1)
                fallback date (0..1)
                    [metadata id]
                mark string (0..1)

            func EndDateOf: <"a bare fn-call case body - the shape the emitter ALREADY admits">
                inputs:
                    t Terms (0..1)
                output:
                    result date (0..1)
                set result:
                    t -> startDate

            func AllDatesOf: <"a MULTI fn call - its bare invocation compiles MapperC-headed">
                inputs:
                    t Terms (0..1)
                output:
                    results date (0..*)
                add results:
                    t -> allDates

            func A1EnumSwitchAttrSubject: <"a1 - the attribute-subject switch: hoisting default + collapsing min">
                inputs:
                    h Holder (1..1)
                output:
                    result date (0..1)
                set result:
                    h -> terms
                        extract [
                            style switch
                                American then EndDateOf(item) default h -> fallback,
                                European then EndDateOf(item),
                                Bermuda then AllDatesOf(item) min
                        ]

            func A2EnumSwitchItemSubject: <"a2 - the #379 shape: the switch ELIDES its argument, string bodies">
                inputs:
                    h Holder (1..1)
                output:
                    result string (0..1)
                set result:
                    h -> terms -> venue
                        extract [
                            switch
                                Admitted then "XOFF",
                                OffVenue then "XXXX"
                        ]

            func A3EnumSwitchAttrSubjectAdmittedBodies: <"a3 - the subject half alone (G1 + R1)">
                inputs:
                    h Holder (1..1)
                output:
                    result date (0..1)
                set result:
                    h -> terms
                        extract [
                            style switch
                                American then EndDateOf(item),
                                European then EndDateOf(item)
                        ]

            func B1EnumSwitchUnadmittedBody: <"b1 - a case body OUTSIDE the widened set keeps the ternary">
                inputs:
                    h Holder (1..1)
                output:
                    result string (0..1)
                set result:
                    h -> terms
                        extract [
                            style switch
                                American then note,
                                European then note
                        ]
            """;

    // =========================================================================
    // Part A — the fixtures
    // =========================================================================

    /**
     * a1 — the attribute-subject switch renders the BLOCK: the {@code switchArgument} decl is the
     * COMPILED subject navigation (not the bare lambda param), the guards are {@code ==} rungs
     * qualified by the subject enum's Java class, the AMERICAN arm's meta-deref hoist sits INSIDE
     * its branch, the BERMUDA arm returns its collapsing MapperC chain, and no
     * {@code Objects.equals} ternary survives.
     *
     * <p>The decl assert stops at the {@code map("getStyle",} prefix on purpose: the generated
     * lambda parameter name is a scope-allocated fact, so it is PINNED from the first green run's
     * log rather than guessed here (the seat's "pin from the log, never from a guess" law). The
     * negative half — that the decl is NOT the bare {@code item.get()} form — is what separates
     * this from a2 and is asserted directly.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_attributeSubjectEnumSwitchRendersHoistedIfChain() throws IOException {
        String out = fn("A1EnumSwitchAttrSubject.java");
        String code = codeOnly(out);
        assertContains(out, "final StyleEnum switchArgument = item.<StyleEnum>map(\"getStyle\",");
        assertTrue(!code.contains("StyleEnum switchArgument = item.get();"),
                "the subject decl must compile the switch's OWN argument, not the bare item:\n" + out);
        assertContains(out, "if (switchArgument == null) {");
        assertContains(out, "if (switchArgument == StyleEnum.AMERICAN) {");
        assertContains(out, "if (switchArgument == StyleEnum.EUROPEAN) {");
        assertContains(out, "if (switchArgument == StyleEnum.BERMUDA) {");
        assertContains(out, "final FieldWithMetaDate fieldWithMetaDate = ");
        assertContains(out, "MapperC.<Date>of(");
        assertContains(out, ".min()");
        assertTrue(!code.contains("Objects.equals("),
                "the block render must leave NO ternary dispatch:\n" + out);
        assertTrue(!code.contains("Objects.equals(American"),
                "the bare Rosetta case name must not be echoed as a Java identifier:\n" + out);
    }

    /**
     * a1b — the AMERICAN arm's hoist is INSIDE its branch, not floated to the lambda top and not
     * dropped. The drain has three possible channels and a wrong one shows up here as a missing or
     * mis-placed declaration rather than as a silent wrong render, so the ORDER of the two lines is
     * the assert (LAW: prove the instrument can fail — a1 alone passes if the hoist floats).
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1b_americanArmHoistSitsInsideItsOwnBranch() throws IOException {
        String out = fn("A1EnumSwitchAttrSubject.java");
        int guard = out.indexOf("if (switchArgument == StyleEnum.AMERICAN) {");
        int hoist = out.indexOf("final FieldWithMetaDate fieldWithMetaDate = ");
        assertTrue(guard >= 0, "the AMERICAN guard rung must render:\n" + out);
        assertTrue(hoist > guard,
                "the arm's meta-deref hoist must be emitted INSIDE the branch, after its guard"
                + " (a floated or lambda-top hoist is the wrong drain channel):\n" + out);
        assertTrue(out.indexOf("fieldWithMetaDate.getValue()", hoist) > hoist,
                "the hoisted local must be consumed by the arm's own default join, AFTER its"
                + " declaration (the paren depth of the join is not asserted — only the order):\n"
                + out);
    }

    /**
     * a2 — the #379 item-subject carrier is UNCHANGED by the widening (it already rendered the
     * block). The no-move witness: its subject decl still reads {@code <param>.get()}, because the
     * switch ELIDES its argument ({@code AstBuilder.visitSwitchWithoutLeftExpr} never calls
     * {@code setArgument}, so {@code sw.argument()} is null and both G1's new rung and R1's new
     * branch are inert by construction).
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_itemSubjectEnumSwitchKeepsTodaysBlock() throws IOException {
        String out = fn("A2EnumSwitchItemSubject.java");
        assertContains(out, " switchArgument = item.get();");
        assertContains(out, "if (switchArgument == VenueEnum.ADMITTED) {");
        assertContains(out, "return MapperS.of(\"XOFF\");");
        assertTrue(!codeOnly(out).contains("Objects.equals("),
                "the #379 carrier already rendered the block and must not regress:\n" + out);
    }

    /**
     * a3 — the SUBJECT half measured apart from the case-body half: bodies the emitter already
     * admits (bare fn calls), subject an ATTRIBUTE. Only the subject-enum resolution (G1) and the
     * subject decl (R1) can carry this file, so it isolates them from G2/G4/G5/R2.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a3_attributeSubjectAloneRendersTheCompiledSubjectDecl() throws IOException {
        String out = fn("A3EnumSwitchAttrSubjectAdmittedBodies.java");
        String code = codeOnly(out);
        assertContains(out, "final StyleEnum switchArgument = item.<StyleEnum>map(\"getStyle\",");
        assertTrue(!code.contains("StyleEnum switchArgument = item.get();"),
                "an attribute subject must not render the bare item decl:\n" + out);
        assertContains(out, "if (switchArgument == StyleEnum.AMERICAN) {");
        assertTrue(!code.contains("Objects.equals("),
                "an attribute-subject switch with admitted bodies must reach the block:\n" + out);
    }

    /**
     * b1 — the decline pin (LAW 76 witness-uniqueness). A case body OUTSIDE the widened kind set (a
     * plain navigation) must keep TODAY's ternary: G1 now resolves the subject, G2 then declines,
     * and the switch falls through to the same ternary it takes today. The witness is a token the
     * flip REMOVES — the {@code Objects.equals(} dispatch itself — asserted PRESENT on
     * {@code codeOnly}; asserting only "no switchArgument block" would also pass if the file failed
     * to generate.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b1_unadmittedCaseBodyKeepsTodaysTernary() throws IOException {
        // v3.2 seat 12 (D52, R1): "today's ternary" is not written any more - a case body OUTSIDE the widened set
        // reaches the residual switch seat, which REFUSES at SWITCH_TERNARY_STUB (the chaos M2 class); law 9's block
        // still does not reach it (no file is emitted at all). The test keeps its name as the seat-29i record.
        fixture();
        assertTrue(fixture().keySet().stream().noneMatch(k -> k.endsWith("/functions/B1EnumSwitchUnadmittedBody.java")),
                "no B1 file is emitted - the seat refuses");
        String refusal = fixtureErrors.stream()
                .filter(e -> e.startsWith("census/seat29i/functions/B1EnumSwitchUnadmittedBody.java ")).findFirst()
                .orElseThrow(() -> new AssertionError("the B1 refusal is not among the fixture's errors: " + fixtureErrors));
        assertTrue(refusal.contains("[SWITCH_TERNARY_STUB]"), refusal);
        assertTrue(refusal.contains("switch at a seat with no ladder renderer"), refusal);
    }

    // =========================================================================
    // Part C — the corpus carriers (4 whole-file rows, one per drr 7.x cell;
    // this suite locks the 7.0.0 representative, the D11 ring locks all four)
    // =========================================================================

    private static final String CARRIER_A =
            "drr/standards/iosco/cde/version1/price/reports/FirstExerciseDateRule.java";

    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_firstExerciseDateRuleByteIdentical() throws IOException {
        lockA(CARRIER_A);
    }

    /**
     * control0 — golden is the oracle: it carries the {@code ==} guard rungs, the in-arm meta-deref
     * hoist and the collapsing MapperC arm, and carries NO {@code Objects.equals(} dispatch.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control0_goldenCarriesTheBlockNotTheTernary() throws IOException {
        String golden = Files.readString(GOLDEN_A.resolve(CARRIER_A));
        assertTrue(golden.contains("final OptionExerciseStyleEnum switchArgument = item."
                + "<OptionExerciseStyleEnum>map(\"getStyle\", exerciseTerms -> exerciseTerms.getStyle()).get();"),
                "golden's subject decl must be the COMPILED switch argument, not the bare item");
        assertTrue(golden.contains("if (switchArgument == OptionExerciseStyleEnum.AMERICAN) {"),
                "golden must carry the enum == guard rung");
        assertTrue(golden.contains("final FieldWithMetaDate fieldWithMetaDate = trade."),
                "golden must hoist the AMERICAN arm's meta-deref INSIDE the branch");
        assertTrue(golden.contains("return MapperC.<Date>of("),
                "golden's BERMUDA arm returns a collapsing MapperC chain");
        assertTrue(golden.contains("\n\t\t\t\t\t\t\t\t.min();"),
                "golden's collapsing arm wraps .min() onto the next line at one tab past its return"
                + " — the continuation depth the block's re-indent must reproduce");
        assertTrue(!golden.contains("Objects.equals("),
                "golden must NOT carry the ternary dispatch");
    }

    /**
     * control1 — LAW 79, the whole-cell UNION scan on drr 7.0.0: per file the (bare-name
     * {@code Objects.equals} sites, {@code switchArgument} declarations, enum {@code ==} guard
     * rungs) triple must equal golden's, file for file over the UNION, beyond the NAMED residue.
     * The delegating cde v2/v3 and the six {@code regulation/*} {@code FirstExerciseDateRule} twins
     * live in this same cell, so an over-fire onto them fails here. Under-fire fails too — the
     * carrier keeping {@code T1=3, T2=0, T3=0} is a mismatch against golden's {@code [0, 1, 3]}.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control1_forkDrr7WholeCellSwitchSitesEqualGoldenFileByFile() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");
        assertEquals(List.of(), drrAGenErrors,
                "drr 7.0.0 reported a generation error — the scan is incomplete");
        assertUnionEqual(scan(drrAOutput), scan(readGoldenTree(GOLDEN_A)), drrAOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_DRR7, DOMAIN_DRR7);
    }

    /** control2 — LAW 77 route parity for the drr 7.0.0 carrier (the probe is 4=4 both routes). */
    @Test
    @EnabledIf("cellAAndIrProviderAvailable")
    void corpus_control2_irRouteMatchesLegacyForCarrier() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 legacy-route generation did not run");
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", CELL_A_ROOT), new ArrayList<>());
        assertEquals(drrAOutput.get(CARRIER_A), irOut.get(CARRIER_A),
                "route divergence: " + CARRIER_A);
    }

    /**
     * control3 — LAW 79: the same scan on drr 5.61.0, the mechanism's OTHER reach cell. The #379
     * emitter's own carriers are mas/asic {@code PlatformIdentifierRule}s, so a subject-enum
     * regression or a render regression on the already-admitted population fires here rather than
     * only in the carrier cell.
     */
    @Test
    @EnabledIf("cellBAvailable")
    void corpus_control3_forkDrr561WholeCellSwitchSitesEqualGoldenFileByFile() throws IOException {
        assertNotNull(drrBOutput, "drr 5.61.0 generation did not run");
        assertEquals(List.of(), drrBGenErrors,
                "drr 5.61.0 reported a generation error — the scan is incomplete");
        assertUnionEqual(scan(drrBOutput), scan(readGoldenTree(GOLDEN_B)), drrBOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_DRR561, DOMAIN_DRR561);
    }

    /**
     * MEASURED at the law's head (LAW 73: the SET, not the count) — TWO pre-existing
     * other-family band rows, both verified PRESENT in the pre-law-9 dump (d29-off) with the
     * same token counts, so law 9 moved NEITHER: {@code UnderlierBasketIdentifier} golden
     * carried one {@code switchArgument} decl the fork lacked (the F15(a) CHOICE-switch block
     * gap, banked to S31), and cde-v1 {@code CustomBasketCodeRule} carried a fork-only
     * {@code switchArgument} block golden renders differently (the S30-banked multi-family
     * carrier). Either row healing OR a new row appearing fails this control — exactly the
     * LAW-81 tripwire the seat wants, <b>and it fired at seat 31 exactly as written</b>.
     *
     * <p><b>RE-MEASURED at the seat-31 chain head {@code f2a4d5c0} — EMPTY</b>, transcribed
     * VERBATIM from control1's own failing print ({@code but was: <[]>}, LAW 81). Both rows LEFT:
     * {@code UnderlierBasketIdentifier} ({@code fork=[0, 0, 0] golden=[0, 1, 0]}) — seat-31 law 4a
     * ({@code choiceOptionNavLadderDeepHop}) healed it WHOLE in all four drr 7.x cells, so the
     * {@code switchArgument} decl golden hoists now renders (golden's T2 = 1 keeps it inside the
     * token-bearing union); {@code CustomBasketCodeRule} ({@code fork=[0, 1, 1] golden=[0, 0, 0]})
     * — seat-31 law 4b ({@code choiceSwitchLambdaOptionGetter}) healed it WHOLE in drr 7.0.0, so
     * the fork's spurious {@code switchArgument} hoist and its null-test are gone and the triple
     * equals golden's ALL-ZERO one, which means the file also LEAVES the token-bearing union
     * ({@code DOMAIN_DRR7} 125 → 124, derived — see the pin's own comment). Any entry that
     * ENTERS this list is a regression.
     */
    private static final List<String> KNOWN_RESIDUE_DRR7 = List.of();

    /**
     * MEASURED at the law's head: the drr 5.61.0 union domain is EMPTY (zero token-bearing
     * files in the emitted set on either side), so the residue is empty and the domain pin
     * below is the whole control — any token appearing in this cell fails it.
     */
    private static final List<String> KNOWN_RESIDUE_DRR561 = List.of();

    /**
     * MEASURED at the seat: fill from the first corpus run — the drr 7.0.0 union domain
     * (golden ∪ fork token-bearing files, intersected with what this harness emits).
     */
    // 125 -> 124 at seat 31: CustomBasketCodeRule's switch triple went all-zero on BOTH sides (law
    // 4b), so it left the token-bearing union; UnderlierBasketIdentifier stays (golden T2 = 1).
    // DERIVED from the residue print, NOT printed by run 1 (the residue assert fires first); the
    // falsifier is this suite's own domain assert on the next chain.
    private static final int DOMAIN_DRR7 = 124;

    /** MEASURED at the seat: fill from the first corpus run — the drr 5.61.0 union domain. */
    private static final int DOMAIN_DRR561 = 0;

    /**
     * (T1, T2, T3) = bare-name {@code Objects.equals} dispatch sites, {@code switchArgument}
     * declarations (both this emitter's {@code final <Enum> switchArgument = …} and the choice
     * block's {@code final <Choice> switchArgument = …}), and enum {@code ==} guard rungs.
     */
    private static final Pattern BARE_EQUALS =
            Pattern.compile("Objects\\.equals\\([A-Z][A-Za-z0-9]*\\s*,");

    private static Map<String, int[]> scan(Map<String, String> tree) {
        Map<String, int[]> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : tree.entrySet()) {
            String code = codeOnly(e.getValue());
            int t1 = 0;
            int t2 = 0;
            int t3 = 0;
            Matcher m = BARE_EQUALS.matcher(code);
            while (m.find()) {
                t1++;
            }
            int i = code.indexOf(" switchArgument = ");
            while (i >= 0) {
                t2++;
                i = code.indexOf(" switchArgument = ", i + 1);
            }
            int j = code.indexOf("if (switchArgument == ");
            while (j >= 0) {
                t3++;
                j = code.indexOf("if (switchArgument == ", j + 1);
            }
            if (t1 + t2 + t3 > 0) {
                out.put(e.getKey(), new int[] {t1, t2, t3});
            }
        }
        return out;
    }

    // =========================================================================
    // The union assert (LAW 73: pin the SET, not the count)
    // =========================================================================

    /**
     * The scan universe is the token-bearing union INTERSECTED with the files this harness emits
     * (rule/report/function kinds): golden's DATA-RULE and POJO kinds can bear tokens these
     * generators never produce, and whether the fork emits every expected FILE is the D11 ring's
     * question (missingOutput), not this suite's.
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
    // Harness (the seat-26/27/28 suite shape verbatim)
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
                + path + " — seat 29 law 9: an attribute-subject enum extract switch renders the"
                + " hoisted switchArgument if-chain block, not the Objects.equals ternary.");
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
    /** The fixture's generation errors at the last render - the declared refusals' messages (v3.2 seat 12). */
    private static List<String> fixtureErrors;
    /** fixture function name -> the register site it refuses at (v3.2 seat 12, D52 R1); asserted as a SET by render(). */
    private static final Map<String, String> DECLARED_REFUSALS = Map.of(
            "B1EnumSwitchUnadmittedBody", "SWITCH_TERNARY_STUB");

    private static void link() throws IOException {
        if (linking == null) {
            RModel main = AstBuilder.buildFromString(MODEL, "seat29i.rosetta");
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
        // v3.2 seat 12 (D52, R1): the fixture's B1EnumSwitchUnadmittedBody REFUSES at SWITCH_TERNARY_STUB by law now
        // (the ternary it pinned is never written). The declared refusal SET is admitted EXACTLY (LAW 73): any other
        // error still fails the fixture loudly, and a declared refusal that did NOT fire fails it too (a heal to re-pin).
        java.util.Set<String> declared = new java.util.TreeSet<>();
        List<String> undeclared = new ArrayList<>();
        for (String e : errors) {
            String fixtureName = DECLARED_REFUSALS.keySet().stream()
                    .filter(n -> e.startsWith("census/seat29i/functions/" + n + ".java ")).findFirst().orElse(null);
            if (fixtureName != null && e.contains("[" + DECLARED_REFUSALS.get(fixtureName) + "]")) {
                declared.add(fixtureName);
            } else {
                undeclared.add(e);
            }
        }
        if (!undeclared.isEmpty()) {
            throw new AssertionError("fixture generation errors (a broken fixture"
                    + " must fail loudly, not skip): " + undeclared);
        }
        if (!declared.equals(DECLARED_REFUSALS.keySet())) {
            throw new AssertionError("the declared refusals did not fire as a SET (a heal to re-pin, never to absorb):"
                    + " expected " + DECLARED_REFUSALS + " fired " + declared);
        }
        fixtureErrors = List.copyOf(errors);
        return out;
    }

    private static Map<String, String> fixture() throws IOException {
        if (fixtureOut == null) {
            fixtureOut = render(m -> "census.seat29i".equals(m.namespace()));
        }
        return fixtureOut;
    }

    /** Functions land under {@code .../functions/}. */
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
            throw new AssertionError("[EnumSwitchBlockWidenSeatTest] builtins parse"
                    + " failures: " + String.join("; ", failures));
        }
        return models;
    }
}
