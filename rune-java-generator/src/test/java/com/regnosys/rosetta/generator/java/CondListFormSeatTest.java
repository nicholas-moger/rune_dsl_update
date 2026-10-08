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
 * SEAT 27, law C — facet {@code segmentAddCondListProjectedLeaf}: <b>the {@code CondListCoerce}
 * handshake reaches a SEGMENT-ADD leaf that the PARSER walk cannot resolve</b>, by recovering the
 * leaf attribute through the #364 {@code projectSegmentHops} projection (the same walk the ADD
 * chain's RENDER already consults — LAW 69). A leaf behind CHOICE-OPTION hops
 * ({@code … -> payout -> OptionPayout -> underlier -> Product -> NonTransferableProduct ->
 * identifier}) has an empty {@code RSegment.resolvedAttribute()}; the projection resolves the
 * option hops off the choice and the leaf off the resulting data type, and the handshake fires.
 *
 * <p>With the handshake pushed, a conditional consumed at that leaf declares
 * {@code final List<T> ifThenElseResultN;} with the per-arm
 * {@code x == null ? Collections.<T>emptyList() : Collections.singletonList(x)} conversion and the
 * {@code else { … = Collections.<T>emptyList(); }} terminal — instead of the null-init item local.
 *
 * <p><b>The golden vs fork shape</b> (drr 7.0.0 {@code Enrich_ReportableEventWithUpiFromAnnaDsb},
 * the {@code .addIdentifier(…)} leaf):
 * <pre>
 * golden:  final List&lt;ProductIdentifier&gt; ifThenElseResult2;
 *          if (…) { final ProductIdentifier productIdentifier0 = ProductIdentifier.builder()…build();
 *                   ifThenElseResult2 = productIdentifier0 == null
 *                           ? Collections.&lt;ProductIdentifier&gt;emptyList()
 *                           : Collections.singletonList(productIdentifier0); }
 *          else   { ifThenElseResult2 = Collections.&lt;ProductIdentifier&gt;emptyList(); }
 * fork:    ProductIdentifier ifThenElseResult2 = null;
 *          if (…) { ifThenElseResult2 = ProductIdentifier.builder()…build(); }
 * </pre>
 *
 * <p><b>The NAMING delta is a KNOCK-ON, not a second law.</b> The same file's second conditional
 * renders golden {@code productIdentifier1} against fork {@code productIdentifier} — because the
 * fork's FIRST conditional never declares a {@code productIdentifier0} at all, so the per-type
 * counter never starts. It heals for free when the list form lands; do NOT write a naming law.
 *
 * <p><b>The probe verdict this law answers (LAW 75 — P27B).</b> The MULTI twin already exists in
 * the ctor-pair loop ({@code ConstructionHandler.condListCoerceFor}, reached with a RESOLVED
 * attribute); the probe printed, per segment-ADD site, the leaf's {@code resolvedAttribute}
 * verdict and the projection's. At {@code Enrich_ReportableEventWithUpiFromAnnaDsb}'s
 * {@code …NonTransferableProduct -> identifier} sites the parser leaf is EMPTY while the
 * projection resolves — so this law is a widened GATE reading an existing walk, not a new
 * handshake registration: {@code segmentAddCondListCoerceOrNull} falls back to
 * {@code projectSegmentHops(…).get(last).attrOrNull} only when the parser leaf is null, and keeps
 * the decline when the projection fails a hop or the leaf hop is itself a choice OPTION
 * ({@code attrOrNull == null}).
 *
 * <p><b>Why the fixture's leaf is unresolvable</b> (the RED's mechanism, not an assumption):
 * {@code TypeDirectedResolver.resolveOperationPath} walks segment by segment and BREAKS the whole
 * walk at the first segment {@code findAttribute} cannot bind; and {@code findAttribute} on an
 * {@code RChoiceTypeRef} returns {@code Optional.empty()} unconditionally (the v3.1 C1 part 2
 * refusal — a choice receiver's plain-arrow scope is its OPTIONS, which are not attributes). So
 * every segment at or after a choice-option hop has an EMPTY {@code resolvedAttribute()}, exactly
 * the corpus condition.
 *
 * <p><b>RED at the pre-seat blob</b>: a1 and a2 (both name tokens the flip ADDS at a leaf the
 * parser walk cannot resolve — the {@code final List<Ident> ifThenElseResult;} blank-final decl
 * and the {@code Collections.<Ident>emptyList()} terminal; the un-handshaken seat rendered the
 * item null-init local instead), corpus_c1, corpus_control1. b1, b2 and b3 GREEN in both states —
 * b1 in particular ENTERS the new fallback (its leaf is unresolved and the projection recovers it)
 * and is turned away by the untouched {@code gm.isMulti(leaf)} gate.
 *
 * <p><b>LAW 81</b>: {@code FunctionCondListCoerceTest.ENRICH_ANNA_DSB} names this same FILE as its
 * A1 flip carrier — AUDITED: that lock is a byte-match against GOLDEN in the drr <b>6.34.1</b>
 * cell (a cell whose leaf already resolved on the parser walk), not a pin of an un-healed fork
 * form, so it neither fires nor needs a re-pin here. The control that DID fire this seat is law
 * B's — {@code BareFnCondHoistEveryContextSeatTest.KNOWN_RESIDUE_DRR561}'s
 * {@code Package_Contract_Price_Monetary} entry.
 *
 * <p><b>LAW 66/76 mutations</b> (each applied → run → reverted; the failing sets MEASURED by
 * the seat's chain at {@code c6585798} — LAW 82): (i) the projected-leaf recovery deleted (the
 * handshake gate back to parser truth) — 4F: a1, a2, corpus_c1, corpus_control1; (ii) the
 * {@code isMulti} leaf gate WIDENED away — 1F: b1 EXACTLY (the witness-unique single-leaf
 * decline pin: a SINGLE leaf recovered through the projection must still decline, and the
 * widening hands it the List form b1 asserts absent — fixture-only BY MEASUREMENT, the
 * single-leaf-behind-choice class is corpus-unwitnessed in the two scanned cells, stated). The
 * skeleton's planned attrOrNull-never-populated control was superseded by (ii): the field's
 * population is already witnessed by every firing in (i)'s set (a null attrOrNull IS parser
 * truth), so the second link with a real distinct witness is the isMulti gate.
 */
class CondListFormSeatTest {

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

    private static final Path CELL_A_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path GOLDEN_A = CELL_A_ROOT.resolve("rosetta-source/src/generated/java");
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
     * The dependency namespace — declared, linked, but NOT generated: exactly the corpus's
     * shape, where the ADD's output type and its whole choice-option path live in an IMPORTED
     * model (CDM under drr) and the parser walk's leaf resolution dies on the option hop.
     */
    private static final String DEP_MODEL = """
            namespace census.seat27cdep
            version "1.0.0"

            type Ident:
                value string (0..1)

            type LeafHolder:
                ids Ident (0..*)
                tags Ident (0..*)

            type OtherHolder:
                note string (0..1)

            choice Pick:
                LeafHolder
                OtherHolder

            type Outer:
                pick Pick (0..1)
                direct Ident (0..*)

            type Src:
                flag boolean (0..1)
                token string (0..1)
                spares Ident (0..*)
            """;

    /**
     * A1 = the {@code Enrich_*} shape: a segment ADD whose leaf sits BEHIND a choice-option hop,
     * with a no-else conditional producing a SINGLE constructor — the recovered handshake's full
     * render (List decl + singleton coercion + typed-empty terminal). A2 = the same recovered leaf
     * with a MULTI navigation arm: the handshake fires and lands the List decl + terminal, while
     * the item→list {@code singletonList} conversion correctly declines (an already-list arm).
     * B1 = the {@code !gm.isMulti(leaf)} decline behind the SAME choice-option hop: the projection
     * recovers the leaf and the gate still says no. B2 = the precondition decline: no conditional
     * value at all, so {@code segmentAddCondListCoerceOrNull} is never consulted. B3 = the
     * parser-truth twin: a DIRECTLY resolved leaf whose handshake never needed the projection.
     */
    private static final String GEN_MODEL = """
            namespace census.seat27c
            version "1.0.0"
            import census.seat27cdep.*

            func A1SegmentAddCondCtor: <"a1 - a segment-ADD leaf behind a CHOICE-OPTION hop">
                inputs:
                    s Src (1..1)
                output:
                    out Outer (1..1)
                add out -> pick -> LeafHolder -> ids:
                    if s -> flag = True
                    then Ident { value: s -> token }

            func A2SegmentAddCondMultiArm: <"a2 - the same recovered leaf, a MULTI arm">
                inputs:
                    s Src (1..1)
                output:
                    out Outer (1..1)
                add out -> pick -> LeafHolder -> tags:
                    if s -> flag = True
                    then s -> spares

            func B1SegmentAddCondSingleLeaf: <"b1 - a recovered leaf that is SINGLE: still declines">
                inputs:
                    s Src (1..1)
                output:
                    out Outer (1..1)
                add out -> pick -> OtherHolder -> note:
                    if s -> flag = True
                    then s -> token

            func B2SegmentAddNonConditional: <"b2 - no conditional value: never consulted">
                inputs:
                    s Src (1..1)
                output:
                    out Outer (1..1)
                add out -> pick -> LeafHolder -> ids:
                    s -> spares

            func B3SegmentAddResolvedLeaf: <"b3 - the parser-truth leaf, untouched by the fallback">
                inputs:
                    s Src (1..1)
                output:
                    out Outer (1..1)
                add out -> direct:
                    if s -> flag = True
                    then Ident { value: s -> token }
            """;

    /**
     * a1 — the LIST-form decl, the per-arm singleton coercion, the typed-empty else terminal,
     * landing behind the {@code getOrCreatePick().getOrCreateLeafHolder()} choice-option chain
     * whose leaf the parser walk cannot resolve.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_segmentAddBehindChoiceOptionDeclaresListForm() throws IOException {
        String out = fn("A1SegmentAddCondCtor.java");
        assertContains(out, "final List<Ident> ifThenElseResult;");
        assertContains(out,
                "ifThenElseResult = ident == null ? Collections.<Ident>emptyList()"
                + " : Collections.singletonList(ident);");
        assertContains(out, "} else {");
        assertContains(out, "ifThenElseResult = Collections.<Ident>emptyList();");
        assertContains(out, ".getOrCreatePick()");
        assertContains(out, ".getOrCreateLeafHolder()");
        assertContains(out, ".addIds(ifThenElseResult);");
        assertTrue(!codeOnly(out).contains("Ident ifThenElseResult = null;"),
                "the item null-init form must be gone:\n" + out);
    }

    /**
     * a2 — the same recovered leaf with a MULTI navigation arm: the handshake fires (List decl +
     * typed-empty terminal) while the item→list {@code singletonList} conversion declines, because
     * the arm is already a list. The coercion's OWN gate, inside a firing handshake.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_multiArmAtRecoveredLeafTakesListFormWithoutTheItemCoercion() throws IOException {
        String out = fn("A2SegmentAddCondMultiArm.java");
        assertContains(out, "final List<Ident> ifThenElseResult;");
        assertContains(out, "ifThenElseResult = MapperS.of(s).<Ident>mapC(\"getSpares\","
                + " src -> src.getSpares()).getMulti();");
        assertContains(out, "ifThenElseResult = Collections.<Ident>emptyList();");
        assertContains(out, ".addTags(ifThenElseResult);");
        assertTrue(!codeOnly(out).contains("Collections.singletonList("),
                "a MULTI arm must NOT take the item→list coercion:\n" + out);
    }

    /**
     * b1 — the decline pin (LAW 76 witness-uniqueness): the projection recovers the leaf behind
     * the SAME choice-option hop, but the leaf is SINGLE, so {@code !gm.isMulti(leaf)} keeps the
     * decline. The file must carry NONE of the three tokens the handshake's render adds — no
     * {@code final List<…> ifThenElseResult;}, no {@code Collections.singletonList(}, no
     * {@code Collections.<…>emptyList()} terminal — and keep the item null-init form instead.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b1_singleRecoveredLeafKeepsTodaysBytes() throws IOException {
        String out = fn("B1SegmentAddCondSingleLeaf.java");
        String code = codeOnly(out);
        assertContains(out, "String ifThenElseResult = null;");
        assertTrue(!code.contains("final List<"),
                "a SINGLE leaf must NOT take the List-form decl:\n" + out);
        assertTrue(!code.contains("Collections.singletonList("),
                "a SINGLE leaf must NOT take the item→list coercion:\n" + out);
        assertTrue(!code.contains("Collections.<"),
                "a SINGLE leaf must NOT take the coercion's typed-empty terminal:\n" + out);
    }

    /**
     * b2 — the precondition decline: the value is not a conditional, so
     * {@code segmentAddCondListCoerceOrNull} is never consulted at all. None of the handshake's
     * tokens may appear.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b2_nonConditionalValueNeverConsultsTheHandshake() throws IOException {
        String code = codeOnly(fn("B2SegmentAddNonConditional.java"));
        assertTrue(!code.contains("ifThenElseResult"),
                "a non-conditional value must hoist no local at all:\n" + code);
        assertTrue(!code.contains("Collections.singletonList("),
                "a non-conditional value must not take the coercion:\n" + code);
    }

    /** b3 — the parser-truth leaf: resolved without the projection, so the fallback never runs. */
    @Test
    @EnabledIf("builtinsAvailable")
    void b3_directlyResolvedLeafKeepsTodaysBytes() throws IOException {
        String out = fn("B3SegmentAddResolvedLeaf.java");
        assertContains(out, "final List<Ident> ifThenElseResult;");
        assertContains(out, ".addDirect(ifThenElseResult);");
        assertTrue(!codeOnly(out).contains("Ident ifThenElseResult = null;"),
                "the parser-truth seat already carried the List form pre-seat:\n" + out);
    }

    // =========================================================================
    // Part C — the corpus carrier (drr 7.0.0; 4 whole-file rows across 7.0-7.3)
    // =========================================================================

    private static final String ENRICH_ANNA_DSB =
            "drr/enrichment/upi/functions/Enrich_ReportableEventWithUpiFromAnnaDsb.java";

    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_enrichAnnaDsbByteIdentical() throws IOException {
        lockA(ENRICH_ANNA_DSB);
    }

    /** control0 — golden is the oracle: the LIST decl, the singleton coercion, the empty terminal. */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control0_goldenCarriesTheListFormAndBothCounters() throws IOException {
        String golden = Files.readString(GOLDEN_A.resolve(ENRICH_ANNA_DSB));
        assertTrue(golden.contains("final List<ProductIdentifier> ifThenElseResult2;"),
                "golden must declare the segment-ADD local at the List form");
        assertTrue(golden.contains("Collections.singletonList(productIdentifier0)"),
                "golden's first arm coerces the hoisted ctor local to a singleton");
        assertTrue(golden.contains("final ProductIdentifier productIdentifier1 ="),
                "golden's SECOND conditional numbers from the first — the knock-on this law heals");
    }

    /**
     * control1 — LAW 79, the whole-cell UNION scan on drr 7.0.0: per file the
     * (List-form decl, item null-init decl, singletonList site) triple must equal golden's,
     * file for file over the UNION, beyond the NAMED residue.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control1_forkDrr7WholeCellCondListSitesEqualGoldenFileByFile() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");
        assertEquals(List.of(), drrAGenErrors, "drr 7.0.0 reported a generation error — the scan is incomplete");
        assertUnionEqual(scan(drrAOutput), scan(readGoldenTree(GOLDEN_A)), drrAOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_7, DOMAIN_DRR7);
    }

    /**
     * The NAMED residue of drr 7.0.0 (LAW 73: pin the SET, not the count) — the files whose
     * (T1, T2, T3) triple still differs from golden's AFTER law C, MEASURED from this suite's
     * first GREEN read over the cell's 297 token-bearing files and re-pinned in the commit of
     * whichever seat heals them. The single entry is an OTHER family's residue: an
     * item-null-init declaration the fork still hoists where golden hoists none at all (a
     * ternary-flattened rule ladder, not a segment-ADD leaf — this law's gate never sees it).
     */
    private static final List<String> KNOWN_RESIDUE_7 = List.of();
            // the IndicatorOfTheUnderlyingIndexRule row (fork=[0, 1, 0] golden=[0, 0, 0]) LEFT this list at seat 32: law D.2
            // (extractBodyMultiDefaultTernary, on law D.1's left deref) healed it WHOLE in all four drr 7.x cells;
            // transcribed from this control's own print (D2-trip1.log).

    /** control2 — LAW 77 route parity for the carrier. */
    @Test
    @EnabledIf("cellAAndIrProviderAvailable")
    void corpus_control2_irRouteMatchesLegacyForCarrier() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 legacy-route generation did not run");
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", CELL_A_ROOT), new ArrayList<>());
        assertEquals(drrAOutput.get(ENRICH_ANNA_DSB), irOut.get(ENRICH_ANNA_DSB),
                "route divergence: " + ENRICH_ANNA_DSB);
    }

    /** control3 — LAW 79: the mechanism's reach into the OTHER cell (drr 5.61.0) is pinned too. */
    @Test
    @EnabledIf("cellBAvailable")
    void corpus_control3_forkDrr561WholeCellCondListSitesEqualGoldenFileByFile() throws IOException {
        assertNotNull(drrBOutput, "drr 5.61.0 generation did not run");
        assertEquals(List.of(), drrBGenErrors, "drr 5.61.0 reported a generation error — the scan is incomplete");
        assertUnionEqual(scan(drrBOutput), scan(readGoldenTree(GOLDEN_B)), drrBOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_561, DOMAIN_DRR561);
    }

    /** The NAMED residue of drr 5.61.0 — see {@link #KNOWN_RESIDUE_7}; MEASURED EMPTY over 143 files. */
    private static final List<String> KNOWN_RESIDUE_561 = List.of();

    /** MEASURED: the drr 7.0.0 union domain — golden ∪ fork token-bearing files. */
    ///PIN: 297 -> 296 at seat 32 (law D.2): the healed IndicatorOfTheUnderlyingIndexRule's GOLDEN tuple is all-zero, so the
    ///PIN: file leaves the token-bearing union once its fork junk is gone; transcribed from this control's own print (D2-trip2.log).
    private static final int DOMAIN_DRR7 = 296;

    /** MEASURED: the drr 5.61.0 union domain. */
    private static final int DOMAIN_DRR561 = 143;

    /**
     * (T1, T2, T3) = List-form decls, item null-init DECLARATIONS, singletonList coercion sites.
     * Every token is scoped to the coercion's own render — verified corpus-wide (drr 7.0.0 and
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
                if (s.startsWith("final List<") && s.contains("ifThenElseResult")) {
                    t1++;
                }
                // The DECLARATION form only — the leading space rules out a POJO's
                // `ifThenElseResult = null;` blank-final ASSIGNMENT (never this law's seat).
                if (s.contains(" ifThenElseResult") && s.endsWith("= null;")) {
                    t2++;
                }
                if (s.contains("Collections.singletonList(") && s.contains("ifThenElseResult")) {
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
                + path + " — seat 27 law C: the segment-ADD conditional declares the LIST form with per-arm singleton coercion.");
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
    private static RModel genModel;
    private static Map<String, String> fixtureOut;

    private static void link() throws IOException {
        if (linking == null) {
            RModel dep = AstBuilder.buildFromString(DEP_MODEL, "seat27cdep.rosetta");
            dep.setVersion("0.0.0.test");
            RModel gen = AstBuilder.buildFromString(GEN_MODEL, "seat27c.rosetta");
            gen.setVersion("0.0.0.test");
            List<RModel> models = new ArrayList<>();
            models.add(dep);
            models.add(gen);
            models.addAll(loadBuiltinsOnly());
            linking = RWorkspace.build(models);
            genModel = gen;
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
        ruleGen.generateClasses(genModel, "1.0", out)
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
            fixtureOut = render(m -> "census.seat27c".equals(m.namespace()));
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
            throw new AssertionError("[CondListFormSeatTest] builtins parse"
                    + " failures: " + String.join("; ", failures));
        }
        return models;
    }
}
