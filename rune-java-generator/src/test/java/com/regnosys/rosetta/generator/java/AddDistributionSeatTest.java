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
 * SEAT 27, law B — facet {@code addCondDistributionTopLevel}: <b>a whole-output ADD whose VALUE is
 * a conditional distributes {@code <target>.addAll(toBuilder(<arm>.getMulti()))} into every arm</b>,
 * with {@code Collections.<T>emptyList()} at the bottom — instead of hoisting ONE item-typed
 * {@code ifThenElseResult} local and emitting a single {@code addAll(<local>)}.
 *
 * <p>The law has TWO disjoint halves, one per declining gate:
 * <ol>
 *   <li><b>(i) the TOP-level bare-fn-call condition ADMITS</b> — it renders the #390 boolHoist
 *       form AHEAD of the ladder ({@code final Boolean booleanN = <call>;} then
 *       {@code if ((booleanN == null ? false : booleanN)) {}), the top-level sibling of the
 *       nested-else boolHoist. Carriers: drr 5.61.0 {@code Contract_Price_Monetary} +
 *       {@code Package_Contract_Price_Monetary}.</li>
 *   <li><b>(ii) an {@link com.regnosys.rosetta.ast.expressions.references.RDeepFeatureCall} arm
 *       joins the mapper-chain distribution class</b> — {@code ->>} compiles to a Mapper chain
 *       exactly like {@code RFeatureCall}, so the arm takes {@code .addAll(toBuilder(<chain>
 *       .getMulti()))} with no cardinality proof. Carriers: drr 7.0-7.3
 *       {@code GetUnderlierProductIdentifierLeg2} (whole) and {@code …Leg1} (improved).</li>
 * </ol>
 *
 * <p><b>The golden vs fork shape</b> (drr 7.0.0 {@code GetUnderlierProductIdentifierLeg2};
 * drr 5.61.0 {@code Contract_Price_Monetary} is the same law):
 * <pre>
 * golden:  if (…) { productId.addAll(toBuilder(&lt;arm&gt;.&lt;AssetIdentifier&gt;mapC(…).getMulti())); }
 *          else if (…) { … }
 *          else       { productId.addAll(toBuilder(Collections.&lt;AssetIdentifier&gt;emptyList())); }
 * fork:    final AssetIdentifier ifThenElseResult;
 *          if (…) { ifThenElseResult = &lt;arm&gt;.&lt;AssetIdentifier&gt;mapC(…).get(); }
 *          … else { ifThenElseResult = null; }
 *          productId.addAll(toBuilder(ifThenElseResult));      // does not compile
 * </pre>
 * The fork also drops the {@code java.util.Collections} import and loses the trailing
 * {@code .<T>map("Type coercion", …)} because it compiles at the item type.
 *
 * <p><b>LAW 74</b>: {@code List.addAll(Collection)} handed a single {@code AssetIdentifier} /
 * {@code PriceSchedule} is an {@code incompatible types} error — the PRE javac probe must show it
 * and the POST must exit 0.
 *
 * <p><b>The probe verdict this law answers (LAW 75 — P27A).</b> The distribution machinery
 * ALREADY EXISTS and merely declined at two independent gates; the probe measured them
 * separately, and neither is the other's cause:
 * <ul>
 *   <li>P27A / the top-level gate: of 652 whole-output ADD conditionals corpus-wide exactly
 *       TWO declined on {@code addDistributionAdmissible}'s retired {@code topLevel} bare-fn
 *       arm — drr 5.61.0 {@code Contract_Price_Monetary} + {@code Package_Contract_Price_Monetary};
 *       the SAME two functions in drr 6.34.1-6.38.0 already render the distribution byte-green
 *       (only the top condition's shape differs there), the five-cell natural control that
 *       retires the pre-seat "corpus-unverified at this seat" premise.</li>
 *   <li>P27A / the arm gate: the deep-call omission was measured at the
 *       {@code GetUnderlierProductIdentifierLeg1/2} arm walks — 8 of the corpus's 10 declining
 *       whole-output ADD conditionals. {@code Contract_Price_Monetary} has NO deep-call arm and
 *       {@code GetUnderlierProductIdentifierLeg2} has NO bare-fn top condition, so a single
 *       widening claimed for all six rows would have been LAW-72 malpractice.</li>
 * </ul>
 *
 * <p><b>RED at the pre-seat blob</b>: a1 (no {@code final Boolean boolean0} / no distributed arm —
 * the whole ladder declined to the item local), a2 (the deep-call arm declined the whole ladder),
 * corpus_c1, corpus_c2, corpus_control1, corpus_control3. b1 GREEN in both states.
 *
 * <p><b>LAW 81</b>: {@code BareFnCondHoistEveryContextSeatTest.KNOWN_RESIDUE_DRR561} pinned
 * {@code drr/regulation/common/functions/Package_Contract_Price_Monetary.java fork=[0, 0, 0]
 * golden=[5, 5, 0]} — law B lands the five per-arm boolean hoists, that control FIRES, and the
 * entry is REMOVED (re-pinned) in the same commit as this suite. Content locks audited alongside:
 * {@code ChoiceLadderAliasJoinDistributionQuartetComposeTest} (Contract_Price_Monetary),
 * {@code RuntimeThenRestructureComposeTest} (GUPILeg2),
 * {@code CardinalityJoinMetaParamComposeTest} + {@code CtlThenRestructureFoundationComposeTest}
 * (GUPILeg1, a partial row that only IMPROVES here).
 *
 * <p><b>LAW 66/76 mutations</b> (each applied → run → reverted; the failing sets MEASURED by
 * the seat's chain at {@code c6585798} — LAW 82): (i) the topLevel bare-fn ADMISSIBILITY
 * decline restored — 4F: a1, corpus_c1, corpus_control3 + the cross-suite
 * {@code BareFnCondHoistEveryContextSeatTest.corpus_control1} (CPM/PCPM re-enter the 5.61.0
 * hoist census — the LAW-81 tripwire on the seat's OWN re-pin); (ii) the {@code RDeepFeatureCall}
 * ADMISSIBILITY admit deleted — 3F: a2, corpus_c2, corpus_control1; (iii) the topLevel boolHoist
 * RENDER branch disabled (the flat inline {@code .getOrDefault(false)} form) — 3F: a1,
 * corpus_c1, BareFn·control1 — (iii)'s set is (i)'s minus corpus_control3: the flat form still
 * DISTRIBUTES (the add-token census holds) but drops golden's boolean hoist, so only the
 * hoist-census and byte oracles move — the admit and render halves measured apart. The
 * skeleton's planned third set (the render-side RDeepFeatureCall) was superseded WITHOUT a
 * measurement of its own — that sever was never run (LAW 82: stating its failing set would be a
 * claim). The render half's witness is the byte oracles instead: a2's exact-render assert +
 * corpus_c2/control1 lock the rendered arm form, so a render-list omission cannot pass them;
 * the residual gap (a non-meta MULTI deep-call arm conceivably rendering byte-identically
 * through the cardinality fall-through) is stated, not measured. The boolHoist render branch
 * is the seat's measured third link.
 */
class AddDistributionSeatTest {

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

    private static final Path CELL_A_ROOT = Path.of("../test-corpus/drr/drr-5.61.0");
    private static final Path GOLDEN_A = CELL_A_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CELL_B_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
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
     * A1 = the {@code Contract_Price_Monetary} shape: a whole-output ADD whose conditional has a
     * BARE FN-CALL top condition (law B half (i)). A2 = the
     * {@code GetUnderlierProductIdentifierLeg2} shape: a whole-output ADD whose then-arm is a
     * DEEP feature call {@code ->>} (law B half (ii)). B1 = the decline pin: a SEGMENT-pathed ADD
     * carrying the SAME bare-fn top condition is not this law's seat and must not move.
     */
    private static final String MODEL = """
            namespace census.seat27b
            version "1.0.0"

            type Leg:
                code string (0..1)
                amt number (0..1)

            type SubA:
                legs Leg (0..*)

            type SubB:
                legs Leg (0..*)

            type Idx:
                subA SubA (0..1)
                subB SubB (0..1)

            type Holder:
                legs Leg (0..*)
                spare Leg (0..*)
                idx Idx (0..1)
                flag boolean (0..1)

            type Bag:
                ids Leg (0..*)

            func IsBig: <"a BARE fn-call condition - the retired top-level decline's carrier">
                inputs:
                    h Holder (0..1)
                output:
                    result boolean (1..1)
                set result:
                    h -> flag = True

            func A1AddCondBareFnTop: <"a1 - the whole-output ADD with a bare-fn top condition">
                inputs:
                    h Holder (1..1)
                output:
                    result Leg (0..*)
                add result:
                    if IsBig(h)
                    then h -> legs
                    else if h -> spare exists
                    then h -> spare
                    else empty

            func A2AddCondDeepCallArm: <"a2 - the whole-output ADD with a DEEP-call then arm">
                inputs:
                    h Holder (1..1)
                output:
                    result Leg (0..*)
                add result:
                    if h -> flag = True
                    then h -> idx ->> legs
                    else empty

            func B1SegmentAddCond: <"b1 - a SEGMENT-pathed ADD keeps its bytes (not this seat)">
                inputs:
                    h Holder (1..1)
                output:
                    out Bag (1..1)
                add out -> ids:
                    if IsBig(h)
                    then h -> legs
            """;

    /**
     * a1 — the bare-fn top condition hoists its boolean AHEAD of the ladder AND the addAll
     * distributes into every arm. The hoist sentinel is the session's Boolean name: a SINGLE
     * site in this fixture renders {@code _boolean} (the corpus carriers, with five sites,
     * render {@code boolean0..4}).
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_wholeOutputAddWithBareFnTopConditionDistributes() throws IOException {
        String out = fn("A1AddCondBareFnTop.java");
        assertContains(out, "final Boolean _boolean = isBig.evaluate(h);");
        assertContains(out, "if ((_boolean == null ? false : _boolean)) {");
        assertContains(out, "result.addAll(toBuilder(MapperS.of(h).<Leg>mapC(\"getLegs\","
                + " holder -> holder.getLegs()).getMulti()));");
        assertContains(out, "} else {");
        assertContains(out, "result.addAll(toBuilder(Collections.<Leg>emptyList()));");
        assertContains(out, "import java.util.Collections;");
        assertTrue(!codeOnly(out).contains("ifThenElseResult"),
                "a distributed ADD must hoist NO item local:\n" + out);
    }

    /** a2 — the DEEP-call arm distributes; the arm takes {@code .getMulti()} with no cardinality proof. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_wholeOutputAddWithDeepCallArmDistributes() throws IOException {
        String out = fn("A2AddCondDeepCallArm.java");
        assertContains(out, "result.addAll(toBuilder(MapperS.of(h).<Idx>map(\"getIdx\","
                + " holder -> holder.getIdx()).<Leg>mapC(\"chooseLegs\","
                + " idx -> idxDeepPathUtil.chooseLegs(idx)).getMulti()));");
        assertContains(out, "result.addAll(toBuilder(Collections.<Leg>emptyList()));");
        assertTrue(!codeOnly(out).contains("ifThenElseResult"),
                "a distributed ADD must hoist NO item local:\n" + out);
    }

    /**
     * b1 — the decline pin (LAW 76 witness-uniqueness): the SAME bare-fn top condition on a
     * SEGMENT-pathed ADD. The distribution entry ({@code renderAddConditionalDistributionOrNull})
     * is reached only from the WHOLE-OUTPUT ADD seat, so this file must carry NEITHER token the
     * flip adds — no {@code addAll(toBuilder(} distributed statement and no
     * {@code Collections.<Leg>emptyList()} bottom arm — while keeping its own builder-chain
     * {@code .addIds(…)} render.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b1_segmentPathedAddKeepsTodaysBytes() throws IOException {
        String out = fn("B1SegmentAddCond.java");
        String code = codeOnly(out);
        assertContains(out, "Leg ifThenElseResult = null;");
        assertContains(out, ".addIds(ifThenElseResult);");
        assertTrue(!code.contains("addAll(toBuilder("),
                "law B's distributed statement must NOT reach the segment seat:\n" + out);
        assertTrue(!code.contains("Collections.<Leg>emptyList()"),
                "law B's typed-empty bottom arm must NOT reach the segment seat:\n" + out);
    }

    // =========================================================================
    // Part C — the corpus carriers (6 whole-file rows across two cells)
    // =========================================================================

    private static final List<String> CARRIERS_A = List.of(
            "drr/regulation/common/functions/Contract_Price_Monetary.java",
            "drr/regulation/common/functions/Package_Contract_Price_Monetary.java");

    private static final String GUPI_LEG2 =
            "drr/regulation/common/functions/GetUnderlierProductIdentifierLeg2.java";

    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_drr561CarriersByteIdentical() throws IOException {
        for (String p : CARRIERS_A) {
            lockA(p);
        }
    }

    /** c2 — the drr 7.0.0 carrier; the sibling cells 7.1/7.2/7.3 carry the same row. */
    @Test
    @EnabledIf("cellBAvailable")
    void corpus_c2_gupiLeg2ByteIdentical() throws IOException {
        assertNotNull(drrBOutput, "drr 7.0.0 generation did not run");
        assertEquals(normalize(Files.readString(GOLDEN_B.resolve(GUPI_LEG2))),
                normalize(drrBOutput.get(GUPI_LEG2)),
                "generated drr 7.0.0 output must byte-match golden for " + GUPI_LEG2);
    }

    /** control0 — golden is the oracle: it distributes, and carries NO item-local addAll. */
    @Test
    @EnabledIf("cellBAvailable")
    void corpus_control0_goldenDistributesAtEveryCarrier() throws IOException {
        String golden = Files.readString(GOLDEN_B.resolve(GUPI_LEG2));
        assertTrue(golden.contains("productId.addAll(toBuilder("),
                "golden must distribute the addAll into the arms");
        assertTrue(golden.contains("Collections.<AssetIdentifier>emptyList()"),
                "golden's bottom arm is the typed empty list");
        assertTrue(!golden.contains("addAll(toBuilder(ifThenElseResult))"),
                "golden must NOT carry the item-local addAll");
    }

    /**
     * control1 — LAW 79, the whole-cell UNION scan on drr 7.0.0: per file the
     * (distributed addAll, item-local addAll, typed-empty bottom) triple must equal golden's,
     * file for file over the UNION, beyond the NAMED residue.
     */
    @Test
    @EnabledIf("cellBAvailable")
    void corpus_control1_forkDrr7WholeCellAddSitesEqualGoldenFileByFile() throws IOException {
        assertNotNull(drrBOutput, "drr 7.0.0 generation did not run");
        assertEquals(List.of(), drrBGenErrors, "drr 7.0.0 reported a generation error — the scan is incomplete");
        assertUnionEqual(scan(drrBOutput), scan(readGoldenTree(GOLDEN_B)), drrBOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_7, DOMAIN_DRR7);
    }

    /**
     * The NAMED residue of drr 7.0.0 (LAW 73: pin the SET, not the count) — the files whose
     * (T1, T2, T3) triple still differs from golden's AFTER law B. MEASURED EMPTY: over the whole
     * cell's 180 token-bearing files the fork's distribution triple equals golden's file for file,
     * so an under-fire (a carrier still hoisting the item local) and an over-fire (a distribution
     * golden does not have) both fail this control outright.
     */
    private static final List<String> KNOWN_RESIDUE_7 = List.of();

    /** control2 — LAW 77 route parity for the drr 5.61.0 carriers. */
    @Test
    @EnabledIf("cellAAndIrProviderAvailable")
    void corpus_control2_irRouteMatchesLegacyForCarriers() throws IOException {
        assertNotNull(drrAOutput, "drr 5.61.0 legacy-route generation did not run");
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "5.61.0", CELL_A_ROOT), new ArrayList<>());
        for (String p : CARRIERS_A) {
            assertEquals(drrAOutput.get(p), irOut.get(p), "route divergence: " + p);
        }
    }

    /** control3 — LAW 79: the same scan on the drr 5.61.0 cell, the mechanism's OTHER carrier cell. */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control3_forkDrr561WholeCellAddSitesEqualGoldenFileByFile() throws IOException {
        assertNotNull(drrAOutput, "drr 5.61.0 generation did not run");
        assertEquals(List.of(), drrAGenErrors, "drr 5.61.0 reported a generation error — the scan is incomplete");
        assertUnionEqual(scan(drrAOutput), scan(readGoldenTree(GOLDEN_A)), drrAOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_561, DOMAIN_DRR561);
    }

    /** The NAMED residue of drr 5.61.0 — see {@link #KNOWN_RESIDUE_7}; MEASURED EMPTY over 27 files. */
    private static final List<String> KNOWN_RESIDUE_561 = List.of();

    /** MEASURED: the drr 7.0.0 union domain — golden ∪ fork token-bearing files. */
    private static final int DOMAIN_DRR7 = 180;

    /** MEASURED: the drr 5.61.0 union domain. */
    private static final int DOMAIN_DRR561 = 27;

    /**
     * (T1, T2, T3) = distributed addAll sites, item-local addAll sites, typed-empty bottom arms.
     * Every token is scoped to the distribution's own render — verified corpus-wide (drr 7.0.0 and
     * drr 5.61.0) to occur ONLY under {@code …/functions/} and {@code …/reports/}, the kinds this
     * harness emits, so the union domain is exactly the law's domain (LAW 79).
     */
    private static Map<String, int[]> scan(Map<String, String> tree) {
        Map<String, int[]> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : tree.entrySet()) {
            String code = codeOnly(e.getValue());
            int t1 = 0;
            int t2 = 0;
            int t3 = 0;
            for (String line : code.split("\n")) {
                String s = line.trim();
                if (s.contains(".addAll(toBuilder(") && !s.contains("ifThenElseResult")) {
                    t1++;
                }
                if (s.contains(".addAll(toBuilder(ifThenElseResult")
                        || s.contains(".addAll(ifThenElseResult")) {
                    t2++;
                }
                // The DISTRIBUTED bottom arm only — a bare `Collections.<T>emptyList()` anywhere
                // else (validation data-rule classes carry one) is not this law's token.
                if (s.contains(".addAll(") && s.contains("Collections.<")
                        && s.contains(">emptyList()")) {
                    t3++;
                }
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

    private static void assertUnionEqual(Map<String, int[]> a, Map<String, int[]> b,
            java.util.Set<String> emittedA, String aName, String bName, List<String> knownResidue,
            int expectedDomain) {
        List<String> mismatched = new ArrayList<>();
        List<String> notEmitted = new ArrayList<>();
        java.util.Set<String> universe = new java.util.TreeSet<>(a.keySet());
        universe.addAll(b.keySet());
        int[] zero = new int[3];
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
                "(T1, T2, T3) differ beyond the named residue in " + mismatched.size() + " file(s)");
        assertEquals(List.of(), notEmitted,
                "token-bearing files " + bName + " carries that " + aName + " does not emit at all");
        assertEquals(expectedDomain, universe.size(),
                "the union domain must equal the oracle's token-bearing files (" + expectedDomain + ")");
    }

    // =========================================================================
    // Harness (the seat-26 suite shape verbatim)
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
                    new D11CorpusRegressionTest.CellSpec("drr", "5.61.0", CELL_A_ROOT), errs);
            drrAGenErrors = errs;
        }
        if (cellBAvailable()) {
            List<String> errs = new ArrayList<>();
            drrBOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", CELL_B_ROOT), errs);
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
        assertNotNull(drrAOutput, "drr 5.61.0 generation did not run — corpus unavailable?");
        List<String> lockedErrors = drrAGenErrors.stream().filter(e -> e.contains(path)).toList();
        assertTrue(lockedErrors.isEmpty(),
                "the generator reported errors for the locked file " + path + ": " + lockedErrors);
        String generated = drrAOutput.get(path);
        assertNotNull(generated, "not generated in drr 5.61.0: " + path);
        Path goldenPath = GOLDEN_A.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "golden missing: " + goldenPath);
        assertEquals(normalize(Files.readString(goldenPath)), normalize(generated),
                "generated drr 5.61.0 output must byte-match golden (newline-normalized) for "
                + path + " — seat 27 law B: a whole-output ADD whose value is a conditional DISTRIBUTES into every arm.");
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
            RModel main = AstBuilder.buildFromString(MODEL, "seat27b.rosetta");
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
            fixtureOut = render(m -> "census.seat27b".equals(m.namespace()));
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
            throw new AssertionError("[AddDistributionSeatTest] builtins parse"
                    + " failures: " + String.join("; ", failures));
        }
        return models;
    }
}
