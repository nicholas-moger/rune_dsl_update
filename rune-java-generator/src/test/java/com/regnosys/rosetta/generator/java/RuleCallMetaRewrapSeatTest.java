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
import com.regnosys.rosetta.generator.java.enums.EnumGenerator;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.function.LabelProviderGenerator;
import com.regnosys.rosetta.generator.java.function.LabelProviderGeneratorUtil;
import com.regnosys.rosetta.generator.java.object.ChoiceObjectGenerator;
import com.regnosys.rosetta.generator.java.object.ModelObjectGenerator;
import com.regnosys.rosetta.generator.java.object.datarule.DataRuleGenerator;
import com.regnosys.rosetta.generator.java.reports.ReportGenerator;
import com.regnosys.rosetta.generator.java.reports.RuleGenerator;
import com.regnosys.rosetta.generator.java.spi.IRGeneration;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.symbols.RLinkingResult;
import com.regnosys.rosetta.symbols.RWorkspace;
import com.regnosys.rosetta.utils.DeepFeatureCallUtil;

/**
 * SEAT 25, law A — facet {@code ruleCallMetaRewrap}: <b>a MULTI+meta rule invocation at a NON-lambda
 * rule-body stage re-wraps its elements into the MODEL (meta) type</b> — golden drr 5.61.0
 * {@code CallCurrencyRule} (asic/cftc/jfsa/mas), {@code PutCurrencyRule} (×4) and
 * {@code PackageIdentifierRule} (cftc/jfsa) type the {@code CDECallCurrency}/{@code CDEPutCurrency}/
 * {@code CDEPackageIdentifier} stage {@code MapperC.<FieldWithMetaString>of(<rule>.evaluate(…).stream()
 * .<FieldWithMetaString>map(string -> FieldWithMetaString.builder().setValue(string).build())
 * .collect(Collectors.toList()))} and the CONSUMERS follow off the wrapper type: the next extract
 * stage's arg deref ({@code final FieldWithMetaString fieldWithMetaString = item.get(); …
 * evaluate((fieldWithMetaString == null ? null : fieldWithMetaString.getValue()))}) and the
 * whole-output deref tail (PackageIdentifier's {@code if (fieldWithMetaString == null) … getValue()}).
 *
 * <p><b>The defect.</b> The #360 stream-wrap emitter ({@code ReferenceHandler}, the MapperC arm of
 * {@code renderImplicitRuleInvocation}) was gated {@code lambdaTerminalSeat}-only, so a rule-body
 * STAGE invocation (a bare then-stage, the chain-head receiver, or a then-chain conditional arm) fell
 * to the strippable {@code wrappedInMapperCOfSingle} and rendered the meta-blind
 * {@code MapperC.<String>of(<rule>.evaluate(…))} — 10 whole-file drr 5.61.0 POJO carriers, all
 * COMPILING (the pure parity class: LAW 74 measured the pre-seat forms compile —
 * {@code target/seat25-instruments/javac25/}).
 *
 * <p><b>The law (two halves that agree through the compiled TYPE — LAW 69).</b> (1) The admission:
 * {@code ReferenceHandler.ruleBodyNonRootSeat} admits the invocation to the #360 recovery when it sits
 * inside a reporting rule's body but is NOT the body root. The body-root reference is the whole-body
 * delegation class ({@code output = <rule>.evaluate(input);} — the iosco basket v2→v1 alias chains),
 * which stays on the strippable path the whole-output SET strips back to the bare green form (the
 * cp2b D11 catch). (2) The arm splice: at the ite-hoist arm seat
 * ({@code FunctionExpressionRenderer}, the #333 {@code rawInvocable} wrap) a MapperC-TYPED compiled
 * arm is not RAW — it splices verbatim instead of double-wrapping (the jfsa conditional-arm
 * carriers). Every OTHER consumer follows off the wrapper type by machinery that predates this seat:
 * the stage decl, the next extract stage's arg deref, the ite decl + typed-empty re-type (#144/#330)
 * and the whole-output deref tail (F1).
 *
 * <p><b>Banked latent (corpus-unwitnessed, found by this suite's first fixture):</b> a wrapped stage
 * whose DOWNSTREAM extract function returns the wrapper's own VALUE type (string → string) renders
 * the extract stage's decl at the WRAPPER type while the lambda returns the bare
 * {@code MapperS<String>} — non-compiling, no corpus carrier (every corpus downstream stage returns
 * an enum). The fixture mirrors the corpus (enum-returning {@code Fmt}); a future carrier of the
 * string shape must coordinate the extract-stage decl with the deref'd lambda return.
 *
 * <p><b>LAW 75 — measured before the seat over all 275 matrix rows, BOTH routes (the seat-25 probe,
 * {@code target/seat25-instruments/}):</b> the multi+meta non-lambda population at this seat is
 * EXACTLY the three carrier classes (CallCurrency ×5 sites, PutCurrency ×5, PackageIdentifier ×2) and
 * the two basket body-root delegation classes ({@code BasketConstituentIdentifier} /
 * {@code IdentifierOfBasketConstituents} → v1, ×5 sites each) — no other shape reaches the arm; the
 * probe lines are IDENTICAL between the routes.
 *
 * <p><b>RED at the pre-seat blob — MEASURED</b> (the law commit's parent, this suite kept): 14F =
 * a1, a2, a3, the ten corpus locks c1–c10, corpus_control1; b1, b2, corpus_control0 and
 * corpus_control3 GREEN in both states (the declines + the golden pins). The LAW-81 twin: healing
 * these carriers ALSO heals the two {@code PackageIdentifierRule} entries
 * {@code WholeOutputMetaDerefSeatTest.KNOWN_RESIDUE_561} pinned — that pin is re-pinned to EMPTY in
 * this seat's commit (measured first: with the law applied and the OLD pin, that suite's control1
 * read residue {@code []} against the pinned two-entry list — the exact LAW-81 signature).
 *
 * <p><b>LAW 66/76 mutations</b> (each applied → run → reverted; the failing sets MEASURED by the
 * seat's receipts chain — LAW 82): (i) the {@code ruleBodyNonRootSeat} admission deleted (revert to
 * lambdaTerminal-only) — 15F: a1–a3, c1–c10, corpus_control1 + the LAW-81 twin
 * {@code WholeOutputMetaDerefSeatTest.corpus_control1} (the PackageIdentifier deref tails die with
 * the wrap — the cross-suite receipt); (ii) the body-root decline dropped — 1F: b1 ONLY. The
 * corpus controls did NOT move under (ii): the basket v2→v1 whole-body delegation class lives in
 * the drr 6.34–6.38 cells, OUTSIDE this suite's two cells (5.61.0/7.0.0 carry no whole-body
 * multi+meta delegation), so the b1 fixture is the decline's lock and the full-matrix D11 gate
 * pins those cells' greens at the head; (iii) the {@code rawInvocable} MapperC-typed exclusion
 * deleted (the arm double-wraps) — 3F: a2, c6, c7 (the jfsa conditional-arm locks, isolating).
 */
class RuleCallMetaRewrapSeatTest {

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

    /**
     * A {@code [metadata scheme]} multi leaf behind an inner reporting rule, a plain (meta-less) twin,
     * a format function (the ConvertNonISOToISOCurrency twin), and one outer rule per carrier shape.
     */
    private static final String MODEL = """
            namespace census.seat25f6a
            version "1.0.0"

            type Ident:
                code string (0..1)
                    [metadata scheme]
                name string (0..1)

            type Leg:
                ident Ident (0..1)

            type Instr:
                legs Leg (0..*)
                allowed boolean (0..1)
                isOpt boolean (0..1)

            enum CcyEnum:
                USD
                EUR

            func IsAllowable: <"IsAllowableActionForASIC twin - the function filter">
                inputs:
                    i Instr (0..1)
                output:
                    result boolean (1..1)
                set result:
                    i -> allowed = True

            func IsOpt: <"IsFXOption twin - the conditional-arm guard">
                inputs:
                    i Instr (0..1)
                output:
                    result boolean (1..1)
                set result:
                    i -> isOpt = True

            func Fmt: <"ConvertNonISOToISOCurrency twin - the downstream extract stage (enum output, the corpus shape)">
                inputs:
                    s string (0..1)
                output:
                    r CcyEnum (1..1)
                set r:
                    if s = "USD"
                    then CcyEnum -> USD
                    else CcyEnum -> EUR

            reporting rule InnerCcy from Instr: <"CDECallCurrency twin - a MULTI rule whose rosetta output is meta-typed">
                extract legs -> ident -> code
                    as "inner"

            reporting rule InnerPlain from Instr: <"a MULTI rule with a PLAIN (meta-less) output">
                extract legs -> ident -> name
                    as "plain"

            reporting rule A1ThenStage from Instr: <"a1 - THE asic CallCurrency SHAPE: filter, the bare rule then-stage, a downstream extract, then last">
                filter IsAllowable
                then InnerCcy
                then extract Fmt
                then last
                    as "a1"

            reporting rule A2CondArm from Instr: <"a2 - THE jfsa CallCurrency SHAPE: the rule reference as a then-chain conditional arm (the ite-hoist), a downstream extract, then last">
                filter IsAllowable
                then if IsOpt(item)
                    then InnerCcy
                then extract Fmt
                then last
                    as "a2"

            reporting rule A3ChainHead from Instr: <"a3 - THE cftc CallCurrency SHAPE: the bare rule reference as the chain HEAD receiver">
                InnerCcy
                then extract Fmt
                then last
                    as "a3"

            reporting rule B1Delegate from Instr: <"b1 - THE iosco basket v2-to-v1 SHAPE: the whole-body delegation stays bare (the strippable path)">
                InnerCcy
                    as "b1"

            reporting rule B2PlainStage from Instr: <"b2 - a MULTI stage whose callee has NO meta: no wrap without a recoverable wrapper">
                filter IsAllowable
                then InnerPlain
                then extract Fmt
                then last
                    as "b2"
            """;

    // =========================================================================
    // Part A — the seat (RED at the pre-seat blob)
    // =========================================================================

    /** a1 — the bare then-stage wraps element-wise and the downstream extract stage derefs the wrapper. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_thenStageWrapsAndDownstreamDerefs() throws IOException {
        String out = rule("A1ThenStageRule.java");
        assertContains(out, "MapperC.<FieldWithMetaString>of(innerCcyRule.evaluate(thenArg0.get()).stream()");
        assertContains(out, ".<FieldWithMetaString>map(string -> FieldWithMetaString.builder().setValue(string).build())");
        assertContains(out, ".collect(Collectors.toList())");
        assertContains(out, "final FieldWithMetaString fieldWithMetaString = item.get();");
        assertContains(out, "fmt.evaluate((fieldWithMetaString == null ? null : fieldWithMetaString.getValue()))");
        assertContains(out, "import java.util.stream.Collectors;");
        assertNotContains(out, "MapperC.<String>of(innerCcyRule");
    }

    /** a2 — the then-chain conditional arm wraps and the ite-hoist decl + typed-empty tail re-type to the wrapper. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_condArmWrapsAndLadderRetypes() throws IOException {
        String out = rule("A2CondArmRule.java");
        assertContains(out, "final MapperC<FieldWithMetaString> ifThenElseResult;");
        assertContains(out, "ifThenElseResult = MapperC.<FieldWithMetaString>of(innerCcyRule.evaluate(");
        assertContains(out, "ifThenElseResult = MapperC.<FieldWithMetaString>ofNull();");
        assertNotContains(out, "MapperC.<String>of(innerCcyRule");
        assertNotContains(out, "MapperC.<String>ofNull();");
    }

    /** a3 — the chain-HEAD receiver position (cftc: no stage before the rule reference) wraps too. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a3_chainHeadWraps() throws IOException {
        String out = rule("A3ChainHeadRule.java");
        assertContains(out, "MapperC.<FieldWithMetaString>of(innerCcyRule.evaluate(input).stream()");
        assertContains(out, ".<FieldWithMetaString>map(string -> FieldWithMetaString.builder().setValue(string).build())");
        assertNotContains(out, "MapperC.<String>of(innerCcyRule");
    }

    /** a4 — LAW 77: the a1 shape through the REAL {@code IRGeneration.functionGenerator} seam ({@code -Pir-on}). */
    @Test
    @EnabledIf("builtinsAndIrProviderAvailable")
    void a4_thenStageWrapsOnIrRoute() throws IOException {
        String out = lookup(fixtureOnIrRoute(), "reports/A1ThenStageRule.java");
        assertContains(out, "MapperC.<FieldWithMetaString>of(innerCcyRule.evaluate(thenArg0.get()).stream()");
        assertContains(out, ".<FieldWithMetaString>map(string -> FieldWithMetaString.builder().setValue(string).build())");
    }

    // =========================================================================
    // Part B — placement pins (GREEN in BOTH states)
    // =========================================================================

    /** b1 — the whole-body delegation (the body-root reference) stays bare: the SET strips the wrap. */
    @Test
    @EnabledIf("builtinsAvailable")
    void b1_wholeBodyDelegationStaysBare() throws IOException {
        String out = rule("B1DelegateRule.java");
        assertContains(out, "output = innerCcyRule.evaluate(input);");
        assertNotContains(out, ".stream()");
        assertNotContains(out, "FieldWithMetaString.builder()");
    }

    /** b2 — a MULTI stage with NO recoverable meta wrapper stays on the bare {@code MapperC.<String>of} form. */
    @Test
    @EnabledIf("builtinsAvailable")
    void b2_plainMultiStageStaysBare() throws IOException {
        String out = rule("B2PlainStageRule.java");
        assertContains(out, "MapperC.<String>of(innerPlainRule.evaluate(");
        assertNotContains(out, ".stream()");
        assertNotContains(out, "FieldWithMetaString");
    }

    // =========================================================================
    // Part C — the corpus (drr 5.61.0: the ten carriers + the whole-cell control; drr 7.0.0: LAW 79)
    // =========================================================================

    private static final Path DRR561_CELL_ROOT = Path.of("../test-corpus/drr/drr-5.61.0");
    private static final Path DRR561_GOLDEN = DRR561_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path DRR7_CELL_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path DRR7_GOLDEN = DRR7_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    static boolean drr561Available() {
        return Files.isDirectory(DRR561_GOLDEN);
    }

    static boolean drr7Available() {
        return Drr7Corpus.gate(Files.isDirectory(DRR7_GOLDEN), RuleCallMetaRewrapSeatTest.class);
    }

    static boolean drr561AndIrProviderAvailable() {
        return drr561Available() && irProviderOnClasspath();
    }

    /** The 10 drr 5.61.0 whole-file carriers. */
    private static final List<String> DRR561_CARRIERS = List.of(
            "drr/regulation/asic/rewrite/trade/reports/CallCurrencyRule.java",
            "drr/regulation/asic/rewrite/trade/reports/PutCurrencyRule.java",
            "drr/regulation/cftc/rewrite/reports/CallCurrencyRule.java",
            "drr/regulation/cftc/rewrite/reports/PutCurrencyRule.java",
            "drr/regulation/cftc/rewrite/reports/PackageIdentifierRule.java",
            "drr/regulation/jfsa/rewrite/trade/reports/CallCurrencyRule.java",
            "drr/regulation/jfsa/rewrite/trade/reports/PutCurrencyRule.java",
            "drr/regulation/jfsa/rewrite/trade/reports/PackageIdentifierRule.java",
            "drr/regulation/mas/rewrite/trade/reports/CallCurrencyRule.java",
            "drr/regulation/mas/rewrite/trade/reports/PutCurrencyRule.java");

    @Test
    @EnabledIf("drr561Available")
    void corpus_c1_asicCallCurrencyByteIdentical() throws IOException {
        lock(DRR561_CARRIERS.get(0));
    }

    @Test
    @EnabledIf("drr561Available")
    void corpus_c2_asicPutCurrencyByteIdentical() throws IOException {
        lock(DRR561_CARRIERS.get(1));
    }

    @Test
    @EnabledIf("drr561Available")
    void corpus_c3_cftcCallCurrencyByteIdentical() throws IOException {
        lock(DRR561_CARRIERS.get(2));
    }

    @Test
    @EnabledIf("drr561Available")
    void corpus_c4_cftcPutCurrencyByteIdentical() throws IOException {
        lock(DRR561_CARRIERS.get(3));
    }

    @Test
    @EnabledIf("drr561Available")
    void corpus_c5_cftcPackageIdentifierByteIdentical() throws IOException {
        lock(DRR561_CARRIERS.get(4));
    }

    @Test
    @EnabledIf("drr561Available")
    void corpus_c6_jfsaCallCurrencyByteIdentical() throws IOException {
        lock(DRR561_CARRIERS.get(5));
    }

    @Test
    @EnabledIf("drr561Available")
    void corpus_c7_jfsaPutCurrencyByteIdentical() throws IOException {
        lock(DRR561_CARRIERS.get(6));
    }

    @Test
    @EnabledIf("drr561Available")
    void corpus_c8_jfsaPackageIdentifierByteIdentical() throws IOException {
        lock(DRR561_CARRIERS.get(7));
    }

    @Test
    @EnabledIf("drr561Available")
    void corpus_c9_masCallCurrencyByteIdentical() throws IOException {
        lock(DRR561_CARRIERS.get(8));
    }

    @Test
    @EnabledIf("drr561Available")
    void corpus_c10_masPutCurrencyByteIdentical() throws IOException {
        lock(DRR561_CARRIERS.get(9));
    }

    /**
     * control0 — golden is the oracle (the frozen drr 5.61.0 tree, every kind): the wrap-token
     * populations at their exact census counts — T1 the wrap-open line ({@code >of(…evaluate(….stream()}),
     * T2 the element re-wrap map line — and each carrier carries exactly (1, 1).
     */
    @Test
    @EnabledIf("drr561Available")
    void corpus_control0_goldenDrr561IsTheOracle() throws IOException {
        Map<String, int[]> g = scan(readGoldenTree(DRR561_GOLDEN));
        assertEquals(15, sites(g, 0), "golden drr 5.61.0 T1 (wrap-open) sites");
        assertEquals(15, sites(g, 1), "golden drr 5.61.0 T2 (re-wrap map) sites");
        assertEquals(15, g.size(), "golden drr 5.61.0 token-bearing files");
        for (String carrier : DRR561_CARRIERS) {
            assertEquals(List.of(1, 1), counts(g.get(carrier)), "golden carrier (T1, T2): " + carrier);
        }
    }

    /**
     * control1 — the FORK's WHOLE generated drr 5.61.0 cell (every kind, LAW 72): over the UNION of the
     * files either tree carries a token in (a missing side counts as all-zero, LAW 79), the per-file
     * (T1, T2) pairs agree FILE BY FILE — an over-fire (a delegation gaining a wrap) and an under-fire
     * (a carrier keeping none) both fail here; the domain equals golden's.
     */
    @Test
    @EnabledIf("drr561Available")
    void corpus_control1_forkDrr561WholeCellWrapTokensEqualGoldenFileByFile() throws IOException {
        assertNotNull(drr561Output, "drr 5.61.0 generation did not run");
        assertEquals(List.of(), drr561GenErrors, "drr 5.61.0 reported a generation error — the scan is incomplete");
        assertUnionEqual(scan(drr561Output), scan(readGoldenTree(DRR561_GOLDEN)), drr561Output.keySet(),
                "fork", "golden", List.of(), 15);
    }

    /**
     * control2 — LAW 77 route parity: the whole drr 5.61.0 cell generated through the REAL
     * {@code IRGeneration} seams ({@code -Pir-on}) carries the SAME per-file pairs as the legacy-route
     * render, file by file over the UNION.
     */
    @Test
    @EnabledIf("drr561AndIrProviderAvailable")
    void corpus_control2_irRouteDrr561WrapTokensEqualLegacyRouteFileByFile() throws IOException {
        assertNotNull(drr561Output, "drr 5.61.0 legacy-route generation did not run");
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "5.61.0", DRR561_CELL_ROOT), new ArrayList<>());
        assertUnionEqual(scan(irOut), scan(drr561Output), irOut.keySet(), "ir", "legacy", List.of(),
                scan(drr561Output).size());
    }

    /**
     * control3 — LAW 79: the drr 7.0.0 cell, where the wrap population is entirely GREEN (the #360
     * lambda-seat carriers — ClearingSwapUSIs/UTIs, DTCC_TradeParty1/2ReportingDestination,
     * OriginalSwapUTI): the seat must move NOTHING there, file by file over the UNION. The basket
     * v2→v1 body-root delegation class is NOT here — it lives in the drr 6.34–6.38 cells, OUTSIDE
     * this suite's two cells (the review's B-4 correction) — so the body-root decline is locked by
     * the b1 fixture plus the full-matrix D11 gate, not by this control.
     */
    @Test
    @EnabledIf("drr7Available")
    void corpus_control3_forkDrr7WholeCellWrapTokensEqualGoldenFileByFile() throws IOException {
        List<String> errs = new ArrayList<>();
        Map<String, String> out = generateCell(
                new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", DRR7_CELL_ROOT), errs);
        assertEquals(List.of(), errs, "drr 7.0.0 reported a generation error — the scan is incomplete");
        Map<String, int[]> g = scan(readGoldenTree(DRR7_GOLDEN));
        assertEquals(11, sites(g, 0), "golden drr 7.0.0 T1 (wrap-open) sites");
        assertEquals(11, sites(g, 1), "golden drr 7.0.0 T2 (re-wrap map) sites");
        assertUnionEqual(scan(out), g, out.keySet(), "fork", "golden", List.of(), g.size());
    }

    private static List<Integer> counts(int[] c) {
        assertNotNull(c, "the file carries no token");
        return List.of(c[0], c[1]);
    }

    private static void assertUnionEqual(Map<String, int[]> a, Map<String, int[]> b,
            java.util.Set<String> emittedA, String aName, String bName, List<String> knownResidue,
            int expectedDomain) {
        List<String> mismatched = new ArrayList<>();
        List<String> notEmitted = new ArrayList<>();
        java.util.Set<String> universe = new java.util.TreeSet<>(a.keySet());
        universe.addAll(b.keySet());
        int[] zero = new int[2];
        for (String key : universe) {
            if (!emittedA.contains(key)) {
                notEmitted.add(key);
                continue;
            }
            int[] ac = a.getOrDefault(key, zero);
            int[] bc = b.getOrDefault(key, zero);
            if (!java.util.Arrays.equals(ac, bc)) {
                mismatched.add(key + " " + aName + "=" + java.util.Arrays.toString(ac)
                        + " " + bName + "=" + java.util.Arrays.toString(bc));
            }
        }
        assertEquals(knownResidue, mismatched,
                "(T1, T2) differ beyond the named residue in " + mismatched.size() + " file(s)");
        assertEquals(List.of(), notEmitted,
                "token-bearing files " + bName + " carries that " + aName + " does not emit at all");
        assertEquals(expectedDomain, universe.size(),
                "the union domain must equal the oracle's token-bearing files (" + expectedDomain + ")");
    }

    // =========================================================================
    // The scan — (T1, T2) per file over CODE only (comments stripped; a line walk)
    // =========================================================================

    static int[] countTokens(String java) {
        int[] c = new int[2];
        for (String raw : codeOnly(java).split("\n")) {
            String s = raw.strip();
            if (s.contains(">of(") && s.contains(".evaluate(") && s.endsWith(".stream()")) {
                c[0]++;
            }
            if (s.contains(">map(") && s.contains(".builder().setValue(") && s.endsWith(".build())")) {
                c[1]++;
            }
        }
        return c;
    }

    private static Map<String, int[]> scan(Map<String, String> tree) {
        Map<String, int[]> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : tree.entrySet()) {
            if (!e.getValue().contains(".stream()") && !e.getValue().contains(".builder().setValue(")) {
                continue;
            }
            int[] c = countTokens(e.getValue());
            if (c[0] + c[1] > 0) {
                out.put(e.getKey(), c);
            }
        }
        return out;
    }

    private static int sites(Map<String, int[]> per, int idx) {
        int n = 0;
        for (int[] c : per.values()) {
            n += c[idx];
        }
        return n;
    }

    /** Strip line and block comments so a javadoc never counts as code. */
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
                sb.append(java, i, Math.min(j + 1, n));
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
            for (Path p : stream.filter(q -> q.toString().endsWith(".java")).sorted().toList()) {
                out.put(root.relativize(p).toString().replace('\\', '/'), Files.readString(p));
            }
        }
        return out;
    }

    // =========================================================================
    // Cell generation (drr 5.61.0 / 7.0.0, the legacy route; the IR route for control2)
    // =========================================================================

    private static Map<String, String> drr561Output;
    private static List<String> drr561GenErrors;

    @BeforeAll
    static void generateCells() throws IOException {
        if (drr561Available()) {
            List<String> errs = new ArrayList<>();
            drr561Output = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "5.61.0", DRR561_CELL_ROOT), errs);
            drr561GenErrors = errs;
        }
    }

    private static Map<String, String> generateCell(D11CorpusRegressionTest.CellSpec cell,
            List<String> errors) throws IOException {
        var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(),
                D11CorpusRegressionTest.emissionFilter(cell));
        var typeUtil = new JavaTypeUtil();
        var typeTranslator = new JavaTypeTranslator(typeUtil);
        var pojoGen = new ModelObjectGenerator(gm, typeTranslator, typeUtil);
        var choiceGen = new ChoiceObjectGenerator(gm, typeTranslator, typeUtil, pojoGen);
        var enumGen = new EnumGenerator(gm);
        var funcGen = new FunctionGenerator(gm, typeTranslator, typeUtil);
        var ruleGen = new RuleGenerator(gm, typeTranslator, funcGen);
        var reportGen = new ReportGenerator(gm, typeTranslator, funcGen);
        var dataRuleGen = new DataRuleGenerator(gm, typeTranslator, typeUtil);
        var labelProviderGen = new LabelProviderGenerator(
                gm, typeTranslator, new DeepFeatureCallUtil(gm::getType),
                new LabelProviderGeneratorUtil());
        Map<String, String> output = new LinkedHashMap<>();
        for (RModel model : corpus.workspace().files()) {
            if (gm.shouldGenerate(model)) {
                String version = gm.version(model);
                collect(errors, pojoGen.generateClasses(model, version, output));
                collect(errors, choiceGen.generateClasses(model, version, output));
                collect(errors, enumGen.generateClasses(model, version, output));
                collect(errors, ruleGen.generateClasses(model, version, output));
                collect(errors, reportGen.generateClasses(model, version, output));
                collect(errors, dataRuleGen.generateClasses(model, version, output));
                collect(errors, labelProviderGen.generateClasses(model, version, output));
            }
        }
        collect(errors, funcGen.generateWithErrors(output));
        return output;
    }

    /**
     * The cell through the REAL {@code IRGeneration} seams — the D11 ON ring's wiring (the flag set for
     * the render and restored after; the provider asserted present; the function seam asserted to hand
     * back the IR-route generator, so this can never silently be an OFF-route render).
     */
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
            var pojoGen = IRGeneration.modelObjectGenerator(gm, typeTranslator, typeUtil);
            var choiceGen = IRGeneration.choiceObjectGenerator(gm, typeTranslator, typeUtil, pojoGen);
            var enumGen = IRGeneration.enumGenerator(gm);
            FunctionGenerator funcGen = IRGeneration.functionGenerator(gm, typeTranslator, typeUtil);
            assertTrue(!funcGen.getClass().equals(FunctionGenerator.class),
                    "the seam must hand back the IR-route FunctionGenerator, got " + funcGen.getClass());
            var ruleGen = new RuleGenerator(gm, typeTranslator, funcGen);
            var reportGen = new ReportGenerator(gm, typeTranslator, funcGen);
            var dataRuleGen = new DataRuleGenerator(gm, typeTranslator, typeUtil);
            var labelProviderGen = new LabelProviderGenerator(
                    gm, typeTranslator, new DeepFeatureCallUtil(gm::getType),
                    new LabelProviderGeneratorUtil());
            Map<String, String> output = new LinkedHashMap<>();
            for (RModel model : corpus.workspace().files()) {
                if (gm.shouldGenerate(model)) {
                    String version = gm.version(model);
                    collect(errors, IRGeneration.generateClasses(pojoGen, model, version, output));
                    collect(errors, IRGeneration.generateClasses(choiceGen, model, version, output));
                    collect(errors, IRGeneration.generateClasses(enumGen, model, version, output));
                    collect(errors, ruleGen.generateClasses(model, version, output));
                    collect(errors, reportGen.generateClasses(model, version, output));
                    collect(errors, dataRuleGen.generateClasses(model, version, output));
                    collect(errors, labelProviderGen.generateClasses(model, version, output));
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
            errors.forEach(e -> sink.add(e.getTargetPath() + " — " + e));
        }
    }

    private static void lock(String path) throws IOException {
        assertNotNull(drr561Output, "drr 5.61.0 generation did not run — corpus unavailable?");
        List<String> lockedErrors = drr561GenErrors.stream().filter(e -> e.contains(path)).toList();
        assertTrue(lockedErrors.isEmpty(),
                "the generator reported errors for the locked file " + path + ": " + lockedErrors);
        String generated = drr561Output.get(path);
        assertNotNull(generated, "not generated in drr 5.61.0: " + path);
        Path goldenPath = DRR561_GOLDEN.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "golden missing: " + goldenPath);
        assertEquals(normalize(Files.readString(goldenPath)), normalize(generated),
                "generated drr 5.61.0 output must byte-match golden (newline-normalized) for "
                + path + " — seat 25 law A: the MULTI+meta rule-call stage re-wraps its elements.");
    }

    // =========================================================================
    // Fixture harness
    // =========================================================================

    private static RLinkingResult linking;
    private static RModel mainModel;
    private static Map<String, String> fixtureOut;

    private static void link() throws IOException {
        if (linking == null) {
            RModel main = AstBuilder.buildFromString(MODEL, "seat25f6a.rosetta");
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
            fixtureOut = render(m -> "census.seat25f6a".equals(m.namespace()));
        }
        return fixtureOut;
    }

    private static Map<String, String> fixtureOutIr;

    /** The fixture's rules through the REAL {@code IRGeneration.functionGenerator} seam (the ON route). */
    private static Map<String, String> fixtureOnIrRoute() throws IOException {
        if (fixtureOutIr == null) {
            link();
            String previous = System.getProperty(IRGeneration.PROPERTY);
            System.setProperty(IRGeneration.PROPERTY, "true");
            try {
                assertNotNull(IRGeneration.providerOrNull(),
                        "the IR provider must be resolvable under -Pir-on, else this is not an ON-route render");
                GeneratorModel gm = new GeneratorModel(linking.workspace(),
                        m -> "census.seat25f6a".equals(m.namespace()));
                JavaTypeUtil typeUtil = new JavaTypeUtil();
                JavaTypeTranslator tt = new JavaTypeTranslator(typeUtil);
                FunctionGenerator fg = IRGeneration.functionGenerator(gm, tt, typeUtil);
                assertTrue(!fg.getClass().equals(FunctionGenerator.class),
                        "the seam must hand back the IR-route FunctionGenerator, got " + fg.getClass());
                RuleGenerator ruleGen = new RuleGenerator(gm, tt, fg);
                Map<String, String> out = new LinkedHashMap<>();
                List<String> errors = new ArrayList<>();
                ruleGen.generateClasses(mainModel, "1.0", out)
                        .forEach(e -> errors.add(e.getTargetPath() + " — " + e));
                fg.generateWithErrors(out)
                        .forEach(e -> errors.add(e.getTargetPath() + " — " + e));
                if (!errors.isEmpty()) {
                    throw new AssertionError("fixture generation errors on the IR route: " + errors);
                }
                fixtureOutIr = out;
            } finally {
                if (previous == null) {
                    System.clearProperty(IRGeneration.PROPERTY);
                } else {
                    System.setProperty(IRGeneration.PROPERTY, previous);
                }
            }
        }
        return fixtureOutIr;
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
                        failures.add(p + " — " + e);
                    }
                });
        if (!failures.isEmpty()) {
            throw new AssertionError("[RuleCallMetaRewrapSeatTest] builtins parse"
                    + " failures: " + String.join("; ", failures));
        }
        return models;
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }

    private static void assertContains(String out, String needle) {
        assertTrue(out.contains(needle),
                "expected needle missing:\n" + needle + "\n--- in output:\n" + out);
    }

    private static void assertNotContains(String out, String token) {
        assertFalse(out.contains(token),
                "forbidden token present: " + token + "\n--- in output:\n" + out);
    }
}
