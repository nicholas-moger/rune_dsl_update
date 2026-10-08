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
 * SEAT 28, law 11 — facet {@code implicitInputAttrDisguiseHead}: <b>a disguised
 * {@code <implicit-input attribute> -> <choice-option>} chain HEAD synthesizes its implicit-input
 * navigation</b> ({@code MapperS.of(input).<RateSpecification>map("getRateSpecification", …)})
 * instead of echoing the bare Rosetta head name.
 *
 * <p><b>The golden vs fork shape</b> (drr 7.0.0
 * {@code drr/standards/iosco/cde/version1/price/reports/FixedRateRule.java}, TWO hunks that are
 * ONE defect):
 * <pre>
 * golden: import cdm.product.asset.RateSpecification;
 *         final MapperS&lt;BigDecimal&gt; thenArg = MapperS.of(input)
 *             .&lt;RateSpecification&gt;map("getRateSpecification", interestRatePayout -&gt; interestRatePayout.getRateSpecification())
 *             .&lt;FixedRateSpecification&gt;map("getFixedRateSpecification", rateSpecification -&gt; …)…
 * fork  : (no import)
 *         final MapperS&lt;BigDecimal&gt; thenArg = MapperS.of(rateSpecification)
 *             .&lt;FixedRateSpecification&gt;map("getFixedRateSpecification", rateSpecification -&gt; …)…
 * </pre>
 * The fork's {@code rateSpecification} is the LAMBDA PARAMETER of the very next hop — out of scope
 * at that point. <b>LAW 74</b>: the PRE javac probe must show the undefined symbol and the POST
 * must exit 0. Everything AFTER the head is already byte-correct; only the head and its import are
 * missing, which is why both hunks heal together (LAW 80: WHOLE).
 *
 * <p><b>The probe verdict this law answers (LAW 75).</b>
 * {@code python p28analyze.py dump PROBE28-F15d FixedRateRule OFF} — the tag's ENTIRE output for
 * the carrier, and byte-identical on ON:
 * <pre>
 * [x4  drr/7.0.0..7.3.0] where=rule:FixedRate head=rateSpecification leaf=FixedRateSpecification
 *                        targetKind=none arm=none chainBound=false
 * </pre>
 * <b>{@code targetKind=none}</b> — {@code resolvedSymbol()} is EMPTY, so neither the bare-RULE arm
 * nor the bare-FUNCTION arm of {@code ReferenceHandler.handle(REnumValueRef)} engages;
 * <b>{@code arm=none}</b> — no arm fires; <b>{@code chainBound=false}</b> —
 * {@code resolvedAttributeChain()} is EMPTY too (the parser's Cat-10 binder resolves both names as
 * ATTRIBUTES, and {@code FixedRateSpecification} is a choice OPTION), so the reference falls
 * through to {@code synthesizeFeatureCall}, whose head resolution is function-scope-only — and a
 * RULE body has no enclosing function. The census's producer statement ("a bare-RULE arm and a
 * bare-FUNCTION arm but NO implicit-input-ATTRIBUTE arm") is CONFIRMED verbatim.
 *
 * <p><b>The green twin, and why the discriminator is PLACEMENT, not a new gate.</b>
 * {@code PeriodicPaymentRule} (drr 7.0-7.3, currently GREEN) reports the IDENTICAL probe tuple —
 * {@code head=rateSpecification leaf=FixedRateSpecification targetKind=none arm=none
 * chainBound=false}, ×12 — so no probe FIELD can separate it. Its source puts the same disguise
 * inside a lambda ({@code filter rateSpecification -> FixedRateSpecification exists then extract
 * DayCountConvention}), so the B2 in-lambda arm ({@code synthesizeImplicitItemChain}) already
 * claims it and returns BEFORE law 11's arm is reached. Law 11's arm is therefore placed LAST,
 * immediately before {@code synthesizeFeatureCall}. Fixture b1 is that twin, and
 * {@code corpus_control1} carries the real one.
 *
 * <p><b>RED at the pre-law head</b>: a1, corpus_c1, corpus_control1.
 * b1 and corpus_control0 are GREEN in BOTH states.
 *
 * <p><b>LAW 66/76 mutation sets — CLAIMS until the seat's chain measures them (LAW 82).</b>
 * See {@code law9-13-notes.md}; rewrite this paragraph FROM the chain's logs before merge.
 * <p><b>MEASURED MUTATIONS (LAW 82 - the seat-28 mut28 suite-lane loop; each
 * mutation = the named apply-script reverted, the suite run, the script re-applied;
 * every set below is the RECORDED failing set from that run, never a claim):</b>
 * <ul>
 *   <li>the whole law reverted (law11-apply --revert) -> a1, corpus_c1, corpus_control1 (3F)</li>
 * </ul>
 */
class ImplicitInputDisguiseSeatTest {

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

    /** The carrier cell — also holds the green twin PeriodicPaymentRule and the v2/v3 delegators. */
    private static final Path CELL_A_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path GOLDEN_A = CELL_A_ROOT.resolve("rosetta-source/src/generated/java");
    /** The reach cell — where PROBE28-F15d measures the SAME rule on the BOUND Cat-10 arm. */
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
     * A1 = the {@code FixedRateRule} shape: a TOP-LEVEL rule body whose chain head is an ATTRIBUTE
     * of the rule's from-type and whose next name is a CHOICE OPTION of that attribute's type.
     * B1 = the {@code PeriodicPaymentRule} green twin: the SAME disguise inside a lambda, which
     * the B2 item arm owns and which must NOT gain an {@code input} rooting. B2 = the
     * function-scope decline: a head name that resolves in the enclosing FUNCTION scope must keep
     * today's rendering.
     *
     * <p>{@code note} is used instead of {@code tag} — {@code tag} is a lexer keyword.
     */
    private static final String MODEL = """
            namespace census.seat28k
            version "1.0.0"

            type FixedSpec:
                rate number (0..1)
                note string (0..1)

            type FloatSpec:
                spread number (0..1)

            choice RateSpec:
                FixedSpec
                FloatSpec

            type Payout:
                rateSpec RateSpec (0..1)
                marker string (0..1)

            reporting rule A1InputAttrDisguiseHead from Payout: <"a1 - a TOP-LEVEL <attr> -> <ChoiceOption> chain">
                rateSpec -> FixedSpec -> rate

            reporting rule B1InLambdaDisguiseHead from Payout: <"b1 - the SAME disguise inside a lambda (the green twin)">
                filter rateSpec -> FixedSpec exists
                then extract marker

            func B2FunctionScopeHead: <"b2 - a head that resolves in FUNCTION scope keeps today's bytes">
                inputs:
                    rateSpec RateSpec (0..1)
                output:
                    result number (0..1)
                set result:
                    rateSpec -> FixedSpec -> rate
            """;

    // =========================================================================
    // Part A — the fixtures
    // =========================================================================

    /**
     * a1 — the top-level disguise synthesizes {@code MapperS.of(input).<RateSpec>map("getRateSpec",
     * …)} as the chain head, carries the head type's import, and leaves NO bare head name.
     *
     * <p>The exact hop text (witness type, getter name, lambda variable) is derived by
     * {@code NavigationHandler} from the SAME resolved {@link com.regnosys.rosetta.ast.functions.RAttribute}
     * the Cat-10 and B2 arms feed it — assert the load-bearing substrings, and pin the full line
     * only from the FIRST green run (never write an unmeasured exact render into a javadoc — the
     * LAW-82 claim rule applies to asserts too).
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_topLevelDisguiseHeadSynthesizesInputNavigation() throws IOException {
        String out = rule("A1InputAttrDisguiseHeadRule.java");
        assertContains(out, "MapperS.of(input)");
        assertContains(out, "map(\"getRateSpec\",");
        assertContains(out, "map(\"getFixedSpec\",");
        assertTrue(!codeOnly(out).contains("MapperS.of(rateSpec)"),
                "the bare Rosetta head name must not be echoed as a Java identifier:\n" + out);
    }

    /**
     * b1 — the green-twin decline pin (LAW 76 witness-uniqueness). The SAME disguise inside a
     * lambda is owned by the B2 in-lambda arm; law 11's arm sits AFTER it and must never see it.
     * The witness is the token law 11 would ADD if it mis-fired ({@code MapperS.of(input)}),
     * asserted ABSENT, PLUS the positive item-rooted token so the pin cannot pass by the file
     * failing to render.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b1_inLambdaDisguiseKeepsItemRooting() throws IOException {
        String out = rule("B1InLambdaDisguiseHeadRule.java");
        String code = codeOnly(out);
        assertContains(out, "map(\"getRateSpec\",");
        // MEASURED at the law-11 head (witness uniqueness): the rule's own chain root is
        // `MapperS.of(input)` in BOTH states, so absence alone is no witness - the arm's
        // over-fire would add a SECOND occurrence inside the lambda. The count is the pin,
        // and the item rooting is asserted positively.
        assertEquals(1, count(code, "MapperS.of(input)"),
                "the in-lambda twin is the B2 arm's, and must NOT gain an input rooting:\n" + out);
        assertContains(out, "item -> exists(item.<RateSpec>map(\"getRateSpec\",");
    }

    /**
     * b2 — the function-scope decline: the head name IS a function input, so
     * {@code nameResolvesInFunctionScope} declines and the reference keeps today's rendering. The
     * witness is again the token law 11 would add.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b2_functionScopeHeadKeepsTodaysBytes() throws IOException {
        String out = fn("B2FunctionScopeHead.java");
        assertTrue(!codeOnly(out).contains("MapperS.of(input)"),
                "a FUNCTION-scope head must not be re-rooted on a rule input:\n" + out);
    }

    // =========================================================================
    // Part C — the corpus carriers (4 whole-file rows, one per drr 7.x cell)
    // =========================================================================

    private static final String CARRIER_A =
            "drr/standards/iosco/cde/version1/price/reports/FixedRateRule.java";

    private static final String GREEN_TWIN_A =
            "drr/standards/iosco/cde/version1/payment/reports/PeriodicPaymentRule.java";

    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_fixedRateRuleByteIdentical() throws IOException {
        lockA(CARRIER_A);
    }

    /** c2 — the GREEN TWIN must stay byte-identical: the named negative witness, locked. */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c2_periodicPaymentRuleStaysByteIdentical() throws IOException {
        lockA(GREEN_TWIN_A);
    }

    /**
     * control0 — golden is the oracle for BOTH sides of the discriminator: the carrier's golden
     * carries the input rooting AND the head-type import; the twin's golden carries NEITHER for
     * this chain.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control0_goldenRootsTheCarrierOnInputAndTheTwinOnItem() throws IOException {
        String carrier = Files.readString(GOLDEN_A.resolve(CARRIER_A));
        assertTrue(carrier.contains("MapperS.of(input).<RateSpecification>map(\"getRateSpecification\","),
                "golden must root the top-level disguise on the rule input");
        assertTrue(carrier.contains("import cdm.product.asset.RateSpecification;"),
                "golden must carry the head type's import");
        assertTrue(!carrier.contains("MapperS.of(rateSpecification)"),
                "golden must NOT carry the bare head name");
        String twin = Files.readString(GOLDEN_A.resolve(GREEN_TWIN_A));
        assertTrue(twin.contains(".<RateSpecification>map(\"getRateSpecification\","),
                "the twin's golden navigates the same head");
        assertTrue(!twin.contains("MapperS.of(input).<RateSpecification>map(\"getRateSpecification\","),
                "the twin's golden roots that chain on the bound ITEM, not the input");
    }

    /**
     * control1 — LAW 79, the whole-cell UNION scan on drr 7.0.0: per file the
     * (input-rooted heads, bare lower-camel {@code MapperS.of} operands, import lines) triple must
     * equal golden's, file for file over the UNION, beyond the NAMED residue. T2 is compared
     * fork-vs-golden and never asserted zero — golden legitimately carries
     * {@code MapperS.of(<functionInput>)}. T3 is the dropped-import half and a cheap whole-file
     * tripwire for a navigation law.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control1_forkDrr7WholeCellChainHeadsEqualGoldenFileByFile() throws IOException {
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
     * control3 — LAW 79: the same scan on drr 6.34.1, where {@code PROBE28-F15d} measures the SAME
     * rule taking the BOUND Cat-10 arm ({@code arm=chain chainBound=true}, ×45 per leaf) — so a
     * regression that steals that arm's population fires here.
     */
    @Test
    @EnabledIf("cellBAvailable")
    void corpus_control3_forkDrr634WholeCellChainHeadsEqualGoldenFileByFile() throws IOException {
        assertNotNull(drrBOutput, "drr 6.34.1 generation did not run");
        assertEquals(List.of(), drrBGenErrors,
                "drr 6.34.1 reported a generation error — the scan is incomplete");
        assertUnionEqual(scan(drrBOutput), scan(readGoldenTree(GOLDEN_B)), drrBOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_DRR634, DOMAIN_DRR634);
    }

    /** MEASURED at the chain head c6871ed5 (LAW 73: pin the SET, not the count); re-measured at the
     * seat-29 chain — 6 entries after the three seat-29 movements noted inline; RE-MEASURED at the
     * seat-30 chain head {@code e223ce19} — 5 entries, transcribed VERBATIM from that run's own
     * failing print (LAW 81); RE-MEASURED at the seat-31 chain head {@code f2a4d5c0} — 3 entries,
     * two rows having LEFT (both noted inline), transcribed VERBATIM from that run's own failing
     * print. */
    private static final List<String> KNOWN_RESIDUE_DRR7 = List.of(
            // fork T3 52 -> 53 at seat 29 (law 6, choiceSuperOptionIdCarry — the recovered Asset
            // import adds one line to the fork render; the file stays banded).
            // LAW 81 re-pin (v3.1 flip seat 33, law A.4): the GetBasketConstituents row (fork=[0, 1, 53] golden=[0, 1, 54]) LEFT this list -
            // facet lolDefaultBodyMulti moved this scan's tuple to golden's (the file stays BANDED on its A.3/A.5/A.1/A.2
            // residue; LolDefaultBodyMultiSeatTest pins that residue by name); transcribed from the control print (A4-trip1.log).
            // UnderlierBasketIdentifier.java (was fork=[0, 2, 12] golden=[0, 1, 15]) left this list at
            // seat 31: law 4a (choiceOptionNavLadderDeepHop - the FER SET-seat option ladder walks the NESTED choice option tree
            // through ChoiceSwitchSupport.findChoiceOptionPath and derefs the META option hop into the bare output)
            // healed it WHOLE in all four drr 7.x cells, so its chain-head count (T2) and import block (T3) now equal golden's.
            // Golden's tuple stays non-zero, so the file remains inside the union domain.
            // the three IndicatorOfTheUnderlyingIndexRule rows left this list at seat 29 (law 2,
            // ruleOutputDefaultSpineMulti — the esma/fca consumers healed whole and the common-emir
            // producer's chain-head sites now equal golden's; its residue sits in other seats).
            // CustomBasketCodeRule.java (was fork=[0, 4, 17] golden=[0, 1, 17]) left this list at seat 31:
            // law 4b (choiceSwitchLambdaOptionGetter - the in-lambda CHOICE switch lowers its case guards to option-getter
            // null-tests with MAPPER-typed case locals, the live-bound naming rung hoisted) healed it WHOLE in drr 7.0.0
            // ONLY (7.1-7.3 IMPROVED 50 -> 49 lines and stay banded on a DIFFERENT mechanism - the in-lambda nested
            // then-chain hoist, S32's), so the three chain heads the instanceof ladder's casts disguised (T2 4 -> 1) are
            // gone. Golden's tuple stays non-zero, so the file remains inside the union domain and
            // DOMAIN_DRR7 is UNMOVED at 3278.
            // ExecutionTimestampRule.java (was fork=[3, 3, 11] golden=[3, 3, 12]) left this list at
            // seat 30: law 3 (the disguised-nav REnumValueRef arm in chainProvesMulti, the LEAF
            // rung) healed it WHOLE in all four drr 7.x cells, so its import count (T3) now equals
            // golden's. Golden's triple stays non-zero, so the file remains inside the union domain
            // and DOMAIN_DRR7 is UNMOVED at 3278.
            // the TotalNotionalQuantity row (fork=[0, 1, 24] golden=[0, 1, 25]) LEFT this list: law D.3 (fnDeepCondBaseConfinedArmChainAdmit,
            // seven rungs) healed the file WHOLE in all four drr 7.x cells; transcribed from this control's own
            // print (D3-trip1.log), a pure row removal (was == expected minus it).
            );

    /** MEASURED EMPTY at the chain head c6871ed5. */
    private static final List<String> KNOWN_RESIDUE_DRR634 = List.of();

    /** MEASURED at the chain head c6871ed5: the drr 7.0.0 union domain (golden ∪ fork token-bearing files). */
    private static final int DOMAIN_DRR7 = 3278;

    /** MEASURED at the chain head c6871ed5: the drr 6.34.1 union domain. */
    private static final int DOMAIN_DRR634 = 2689;

    /**
     * (T1, T2, T3) = input-rooted chain heads, bare lower-camel {@code MapperS.of} operands, and
     * import lines.
     */
    private static final Pattern BARE_LOWER_OPERAND =
            Pattern.compile("MapperS\\.of\\([a-z][A-Za-z0-9]*\\)");

    private static Map<String, int[]> scan(Map<String, String> tree) {
        Map<String, int[]> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : tree.entrySet()) {
            String code = codeOnly(e.getValue());
            int t1 = 0;
            int t2 = 0;
            int t3 = 0;
            int i = code.indexOf("MapperS.of(input).");
            while (i >= 0) {
                t1++;
                i = code.indexOf("MapperS.of(input).", i + 1);
            }
            Matcher m = BARE_LOWER_OPERAND.matcher(code);
            while (m.find()) {
                t2++;
            }
            for (String line : code.split("\n")) {
                if (line.startsWith("import ")) {
                    t3++;
                }
            }
            if (t1 + t2 > 0) {
                out.put(e.getKey(), new int[] {t1, t2, t3});
            }
        }
        return out;
    }

    // =========================================================================
    // The union assert (LAW 73) + harness — the seat-27 shape verbatim
    // =========================================================================

    private static void assertUnionEqual(Map<String, int[]> a, Map<String, int[]> b,
            java.util.Set<String> emittedA, String aName, String bName, List<String> knownResidue,
            int expectedDomain) {
        // Scoped to the files this harness emits (the seat-28 correction class).
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
                + path + " — seat 28 law 11: a top-level <attr> -> <ChoiceOption> disguise head"
                + " synthesizes its implicit-input navigation.");
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

    private static void assertContains(String out, String needle) {
        assertTrue(out.contains(needle), "expected <" + needle + "> in:\n" + out);
    }

    /** Strip line and block comments plus string literals so a javadoc or marker never counts as code. */
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
            RModel main = AstBuilder.buildFromString(MODEL, "seat28k.rosetta");
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
            fixtureOut = render(m -> "census.seat28k".equals(m.namespace()));
        }
        return fixtureOut;
    }

    private static String rule(String fileName) throws IOException {
        return lookup(fixture(), "reports/" + fileName);
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
            throw new AssertionError("[ImplicitInputDisguiseSeatTest] builtins parse"
                    + " failures: " + String.join("; ", failures));
        }
        return models;
    }
}
