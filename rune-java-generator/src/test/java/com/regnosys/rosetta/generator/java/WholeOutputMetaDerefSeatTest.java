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
 * SEAT 22, law F1 — facet {@code wholeOutputMetaDeref}: <b>a rule whose RENDERED value is a meta WRAPPER
 * ({@code FieldWithMetaString}, {@code ReferenceWithMetaX}, …) but whose declared output is the bare value
 * hoists the wrapper and derefs null-guarded — {@code final FieldWithMetaString fieldWithMetaString = …get();
 * if (fieldWithMetaString == null) { output = null; } else { output = fieldWithMetaString.getValue(); }}
 * (upstream {@code TypeCoercionService.convertNullSafe} at the output-assign seat; a builder output takes
 * {@code output = toBuilder(x.getValue());}).</b> The fork's ONE emitter
 * ({@code FunctionExpressionRenderer.renderMetaValueDerefOrNull}) already renders the golden block; the 27
 * whole-file carriers DECLINED because no recovery surfaced the wrapper. Three render-truth rungs on the
 * emitter's EXISTING recovery ladder (emission unchanged; every rung a STRICT fall-through after the
 * pre-seat recoveries decline):
 *
 * <ul>
 *   <li><b>R1</b> (the then-extract collapse arm): the arm's GATE admits a collapse over standalone
 *   reshapes of the rebound pipe ({@code then distinct only-element} — TechnicalRecordId ×8, where the
 *   existing producer recovery then resolves the nav leaf), and a new BINDING rung reads the LAST decl the
 *   collapse consumes ({@code prevRef}, the #350 {@code bindThenArg} channel — the SAME read
 *   {@code SetOperationHandler.tryThenBoundMetaArgDeref} makes at the default-arg seat, LAW 69) when its
 *   compiled ITEM is the wrapper, value-type-equality-gated on the bare output — PTRRID ×2, SwapLinkID,
 *   PackageIdentifier ×3, and the ITE-result collapse (CDEPriceCurrency — the charter's R4: the #351
 *   deep-then ITE sentinel decl IS the binding there).</li>
 *   <li><b>R2</b> (the direct-extract SET seat): the nested-then-chain body shape ({@code extract x [
 *   <chain> ]}) joins the #331 conditional shape — the SAME #264/#270 walker recovers the chain terminal
 *   (its then-walk skips standalone reshape/collapse bodies and joins {@code then default} tails), with
 *   the same value-type equality gate — DTCC_TradeParty1TransactionID ×4 and QuantitySchedule ×4 (the
 *   {@code then default} terminal, a builder output).</li>
 *   <li><b>R3</b> (the (a3) rule-context disguised-leaf walk): a disguised head that names a CALLABLE —
 *   the parser's C1 head binding ({@code REnumValueRef.resolvedHead()}, THE AUTHORITY) — re-roots the
 *   chain on the callable's OUTPUT type (an {@code RFunction} through its declared output attribute; an
 *   {@code RRule} through {@code HandlerHelper.ruleInferredOutputRType} — THE ONE rule-output read,
 *   seat 21) — ReferenceEntity ×4 ({@code ExtractReferenceEntity -> identifier}).</li>
 * </ul>
 *
 * <p><b>LAW 75 / the over-fire risk (the seat-22 runtime probe over all 275 rows):</b> the emitter fired
 * 1,944× and declined 45,529× at the pre-seat head; the 27 carriers all sat in the DECLINE set. The green
 * look-alikes are enumerated and excluded structurally: the green collapse-over-wrapper-pipe rules
 * (CollateralPortfolioCode*, OriginalSwapUTI) FIRE via existing channels (untouched — the rungs are strict
 * fall-throughs); the green single+eq seats reaching the final decline (Counterparty1/2,
 * CollateralisationCategory) are {@code mapSingleToItem} blocks, excluded by R1's collapse-over-pipe shape
 * gate; the 60 green RRule-head + 10 green RFunction-head disguised chains (FixedRate*, FloatingRate*,
 * BasketConstituent*) resolve a META-FREE leaf under R3 and decline at {@code metaWrapperOf}. Green-safety
 * is the whole-cell UNION control + the IR-route control2 (LAW 72/79), never the probe. LAW 74: every
 * carrier is non-compiling today (a wrapper assigned to a bare-value output) — 27 repairs. LAW 77: R3
 * reads the seat-21 IR-guarded {@code ruleInferredOutputRType} family; both routes rendered the 27
 * carriers identically at the pre-seat head (the routediff receipt) — control2 re-proves 0 route split.
 *
 * <p><b>RED at the pre-seat blob</b> ({@code rune-java-generator/src/main} + {@code rune-ir-java/src/main}
 * at {@code e2807ac1} — this suite kept), MEASURED <b>per route</b>: default <b>20 run / 12 F / 2 skip</b>
 * = {@code a1, a3, a4, a4b, a5, a6, c1, c2, c3, c4, c5, control1} (the 2 skips are the IR-gated
 * {@code a7} and {@code control2}); {@code -Pir-on} <b>20 run / 13 F / 0 skip</b> = that same set PLUS
 * {@code a7} — {@code control2} PASSES at RED under {@code -Pir-on}, which is exactly what it asserts
 * (both routes decline the 27 carriers identically at the pre-seat blob). Every {@code b*} +
 * {@code control0} is GREEN in both states; GREEN at the head 20/0F (default, 2 skips = the two IR-route
 * receipts; {@code -Pir-on} 20/20).
 * <b>LAW 66/76 mutations</b> (each applied → run → reverted at the FINAL head, LAW 78; the full receipts
 * are in the PR body §7), MEASURED: (i) R1's binding rung deleted → <b>4 F</b> {@code {a6, c2, c5,
 * control1}}; (ii) R1's gate widening reverted (isolating) → <b>4 F</b> {@code {a1, c1, c5, control1}};
 * (iii) R2's nested-then shape removed (isolating) → <b>5 F</b> {@code {a3, a5, c3, c5, control1}};
 * (iv) R3's callable-head rung deleted at BOTH walks (isolating) → <b>5 F</b> {@code {a4, a4b, c4, c5,
 * control1}}.
 *
 * <p><b>Declared un-mutatable (measured, not assumed).</b> BOTH value-type equality gates — R1's
 * ({@code boundPipeElem.getValueType()} vs the output) and R2's ({@code condBare.equals(recovered
 * .getValueType())}) — are belt-and-braces beside the emitter's own {@code outputTypeName}-equals-
 * {@code metaSimple} decline: a rule's output type is INFERRED FROM ITS BODY, so a model in which a
 * recovered wrapper's value type differs from the output type is not expressible, and the corpus carries
 * no such case. The R2 mutation was RUN (dropping the gate) and moved NOTHING — an information-free
 * mutation by the seat-21 lesson, so it is retired rather than reported as a receipt.
 */
class WholeOutputMetaDerefSeatTest {

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
     * The fixture: [metadata scheme] string leaves (FieldWithMetaString), a [metadata reference] data-type
     * leaf (ReferenceWithMetaQ — the builder-output twin), and one reporting rule per carrier shape.
     */
    private static final String MODEL = """
            namespace census.seat22f1
            version "1.0.0"

            metaType scheme string
            metaType reference string

            type Q:
                value number (0..1)

            type Ident:
                code string (0..1)
                    [metadata scheme]
                plain string (0..1)

            type Holder:
                ids Ident (0..*)
                ident Ident (0..1)
                sched Q (0..1)
                    [metadata reference]

            type Probe:
                holders Holder (0..*)
                h1 Holder (0..1)
                h2 Holder (0..1)
                name string (0..1)
                flag string (0..1)

            func GetIdent: <"the disguised FUNCTION head (ExtractReferenceEntity twin)">
                inputs:
                    p Probe (1..1)
                output:
                    result Ident (1..1)

            reporting rule IdentRule from Probe: <"the disguised RULE head twin">
                extract h1 -> ident

            reporting rule Allow from Probe: <"the filter">
                extract name exists

            reporting rule A1DistinctOnlyElement from Probe: <"a1 - THE TechnicalRecordId SHAPE: a distinct only-element collapse over a wrapper-typed pipe">
                extract holders
                then extract ident -> code
                then distinct only-element
                    as "a1"

            reporting rule A2First from Probe: <"a2 - the bare collapse over a wrapper-typed pipe (the PTRRID/SwapLinkID family): the BINDING rung">
                extract holders
                then extract ident -> code
                then first
                    as "a2"

            reporting rule A3NestedChain from Probe: <"a3 - THE DTCC_TradeParty1TransactionID SHAPE: a direct extract whose body is a nested then-chain ending in a collapse">
                extract probe [
                    holders -> ident -> code
                        then distinct
                        then only-element
                ]
                    as "a3"

            reporting rule A4FnHead from Probe: <"a4 - THE ReferenceEntity SHAPE: a disguised FUNCTION-head chain">
                filter Allow
                then extract GetIdent -> code
                    as "a4"

            reporting rule A4bRuleHead from Probe: <"a4b - the RULE-head twin (ruleInferredOutputRType, the IR-guarded family)">
                filter Allow
                then extract IdentRule -> code
                    as "a4b"

            reporting rule A5DefaultTerminal from Probe: <"a5 - THE QuantitySchedule SHAPE: a then-default tail on a reference-wrapper leaf, a builder output (the chains param-rooted exactly like QuantitySchedule's)">
                extract probe [
                    probe -> h1 -> sched
                        then default probe -> h2 -> sched
                ]
                    as "a5"

            reporting rule A6IteCollapse from Probe: <"a6 - THE CDEPriceCurrency SHAPE: an ITE whose result local is the wrapper pipe, collapsed at the output">
                filter Allow
                then (if flag = "K"
                    then h1 -> ids -> code
                    else h2 -> ids -> code)
                then only-element
                    as "a6"

            reporting rule B1BarePipe from Probe: <"b1 - a bare-value pipe collapse stays deref-free">
                extract holders
                then extract ident -> plain
                then distinct only-element
                    as "b1"

            reporting rule B2MultiOutput from Probe: <"b2 - a MULTI output keeps the getMulti path (the multiOutput decline)">
                extract holders
                then extract ident -> code
                    as "b2"

            reporting rule B4FnHeadBareLeaf from Probe: <"b4 - R3 green-safety: a callable head whose leaf is meta-FREE stays deref-free">
                filter Allow
                then extract GetIdent -> plain
                    as "b4"

            reporting rule B5NestedChainBareLeaf from Probe: <"b5 - R2 green-safety: a nested chain over a bare leaf stays deref-free">
                extract probe [
                    holders -> ident -> plain
                        then distinct
                        then only-element
                ]
                    as "b5"
            """;

    private static final String DEREF_GUARD = "== null) {";

    // =========================================================================
    // Part A — the seat (RED at the pre-seat blob)
    // =========================================================================

    /** a1 — the TechnicalRecordId shape: the widened gate lets the EXISTING producer recovery fire. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_distinctOnlyElementOverWrapperPipe() throws IOException {
        String out = rule("A1DistinctOnlyElementRule.java");
        assertDerefBlock(out);
    }

    /**
     * b6 — the bare collapse whose PRODUCER is a resolvable nav chain ({@code then extract ident -> code
     * then first}) ALREADY fired pre-seat through {@code recoverCollapsedProducerMeta}: a BYTE-LOCK that
     * the new rungs leave it exactly as it was. Written first as an {@code a2} "binding rung" receipt and
     * DEMOTED on measurement — it is green at the pre-seat base AND under mutation {@code f1-i}, so it
     * pinned nothing; the binding rung's real fixture receipt is {@code a6} (the ITE-result collapse,
     * whose producer the nav walker declines) and its corpus receipts are {@code c2}/{@code c5}/
     * {@code control1}. Kept as the placement pin it actually is (LAW 66: a test that cannot fail under
     * the law it claims is not a receipt for that law).
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b6_alreadyFiringCollapseUnchanged() throws IOException {
        String out = rule("A2FirstRule.java");
        assertDerefBlock(out);
    }

    /** a3 — the DTCC shape: the nested-then-chain body at the direct-extract seat (R2). */
    @Test
    @EnabledIf("builtinsAvailable")
    void a3_nestedChainCollapse() throws IOException {
        String out = rule("A3NestedChainRule.java");
        assertDerefBlock(out);
    }

    /** a4 — the ReferenceEntity shape: the disguised FUNCTION head (R3). */
    @Test
    @EnabledIf("builtinsAvailable")
    void a4_disguisedFunctionHead() throws IOException {
        String out = rule("A4FnHeadRule.java");
        assertDerefBlock(out);
    }

    /** a4b — the RULE-head twin: ruleInferredOutputRType (the IR-guarded ONE read). */
    @Test
    @EnabledIf("builtinsAvailable")
    void a4b_disguisedRuleHead() throws IOException {
        String out = rule("A4bRuleHeadRule.java");
        assertDerefBlock(out);
    }

    /** a5 — the QuantitySchedule shape: the then-default tail, a builder output (toBuilder deref). */
    @Test
    @EnabledIf("builtinsAvailable")
    void a5_defaultTerminalBuilderOutput() throws IOException {
        String out = rule("A5DefaultTerminalRule.java");
        assertContains(out, "final ReferenceWithMetaQ referenceWithMetaQ");
        assertContains(out, DEREF_GUARD);
        assertContains(out, "output = toBuilder(referenceWithMetaQ.getValue());");
    }

    /** a6 — the CDEPriceCurrency shape: the ITE-result wrapper pipe collapsed at the output (R1's binding). */
    @Test
    @EnabledIf("builtinsAvailable")
    void a6_iteResultCollapse() throws IOException {
        String out = rule("A6IteCollapseRule.java");
        assertDerefBlock(out);
    }

    /**
     * a7 — LAW 77: the {@code a6} ITE-result collapse (the shape whose producer the nav walker declines,
     * so the BINDING rung is the only thing that can serve it) rendered through the REAL
     * {@code IRGeneration.functionGenerator} seam. Re-pointed from the {@code a2} shape on measurement:
     * that shape already fired pre-seat on both routes and so could not witness the rung (see {@code b6}).
     */
    @Test
    @EnabledIf("builtinsAndIrProviderAvailable")
    void a7_bindingRungOnIrRoute() throws IOException {
        String out = lookup(fixtureOnIrRoute(), "reports/A6IteCollapseRule.java");
        assertDerefBlock(out);
    }

    private static void assertDerefBlock(String out) {
        assertContains(out, "final FieldWithMetaString fieldWithMetaString");
        assertContains(out, "if (fieldWithMetaString == null) {");
        assertContains(out, "output = null;");
        assertContains(out, "output = fieldWithMetaString.getValue();");
    }

    // =========================================================================
    // Part B — placement pins (GREEN in BOTH states)
    // =========================================================================

    /** b1 — a bare-value pipe collapse stays deref-free. */
    @Test
    @EnabledIf("builtinsAvailable")
    void b1_barePipeStaysDerefFree() throws IOException {
        String out = rule("B1BarePipeRule.java");
        assertNotContains(out, "FieldWithMeta");
        assertNotContains(out, DEREF_GUARD);
    }

    /** b2 — a MULTI output keeps the getMulti path: no wrapper hoist. */
    @Test
    @EnabledIf("builtinsAvailable")
    void b2_multiOutputKeepsGetMulti() throws IOException {
        String out = rule("B2MultiOutputRule.java");
        assertNotContains(out, "final FieldWithMetaString fieldWithMetaString =");
        assertNotContains(out, "output = null;\n\t\t\t} else {");
    }

    /** b4 — R3 green-safety: a callable head with a meta-FREE leaf stays deref-free. */
    @Test
    @EnabledIf("builtinsAvailable")
    void b4_callableHeadBareLeafStaysDerefFree() throws IOException {
        String out = rule("B4FnHeadBareLeafRule.java");
        assertNotContains(out, "FieldWithMeta");
        assertNotContains(out, DEREF_GUARD);
    }

    /** b5 — R2 green-safety: a nested chain over a bare leaf stays deref-free. */
    @Test
    @EnabledIf("builtinsAvailable")
    void b5_nestedChainBareLeafStaysDerefFree() throws IOException {
        String out = rule("B5NestedChainBareLeafRule.java");
        assertNotContains(out, "FieldWithMeta");
        assertNotContains(out, DEREF_GUARD);
    }

    // =========================================================================
    // Part C — the corpus (drr 5.61.0 + drr 7.0.0: the carriers + the whole-cell controls)
    // =========================================================================

    private static final Path DRR561_CELL_ROOT = Path.of("../test-corpus/drr/drr-5.61.0");
    private static final Path DRR561_GOLDEN = DRR561_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path DRR7_CELL_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path DRR7_GOLDEN = DRR7_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    static boolean cellsAvailable() {
        return Files.isDirectory(DRR561_GOLDEN) && Files.isDirectory(DRR7_GOLDEN);
    }

    static boolean cellsAndIrProviderAvailable() {
        return cellsAvailable() && irProviderOnClasspath();
    }

    /** The 15 drr 5.61.0 whole-file carriers. */
    private static final List<String> DRR561_CARRIERS = List.of(
            "drr/regulation/asic/rewrite/margin/reports/TechnicalRecordIdRule.java",
            "drr/regulation/asic/rewrite/trade/reports/TechnicalRecordIdRule.java",
            "drr/regulation/asic/rewrite/valuation/reports/TechnicalRecordIdRule.java",
            "drr/regulation/jfsa/rewrite/margin/reports/TechnicalRecordIdRule.java",
            "drr/regulation/jfsa/rewrite/trade/reports/TechnicalRecordIdRule.java",
            "drr/regulation/mas/rewrite/margin/reports/TechnicalRecordIdRule.java",
            "drr/regulation/mas/rewrite/trade/reports/TechnicalRecordIdRule.java",
            "drr/regulation/mas/rewrite/valuation/reports/TechnicalRecordIdRule.java",
            "drr/regulation/esma/emir/refit/trade/reports/PackageIdentifierRule.java",
            "drr/regulation/fca/ukemir/refit/trade/reports/PackageIdentifierRule.java",
            "drr/regulation/mas/rewrite/trade/reports/PackageIdentifierRule.java",
            "drr/regulation/esma/emir/refit/trade/reports/PTRRIDRule.java",
            "drr/regulation/fca/ukemir/refit/trade/reports/PTRRIDRule.java",
            "drr/regulation/mas/rewrite/trade/reports/SwapLinkIDRule.java",
            "drr/standards/iosco/cde/reports/CDEPriceCurrencyRule.java");

    /** The 3 drr 7.0.0 whole-file carriers (the 7.1–7.3 twins ride the ring). */
    private static final List<String> DRR7_CARRIERS = List.of(
            "drr/regulation/common/dtcc/valuation/reports/DTCC_TradeParty1TransactionIDRule.java",
            "drr/base/trade/quantity/reports/QuantityScheduleRule.java",
            "drr/regulation/hkma/rewrite/trade/reports/ReferenceEntityRule.java");

    /**
     * The mas UnderlyingRule stays IN-BAND: its whole-output deref is BLOCKED by an early in-lambda arm
     * deref of another family (the fork derefs the wrapper INSIDE the block lambda and types the ofNull
     * tail inconsistently), so the F1 rung cannot fire there yet — named in {@link #KNOWN_RESIDUE_561}.
     */
    private static final String DRR561_PARTIAL = "drr/regulation/mas/rewrite/trade/reports/UnderlyingRule.java";

    /** c1 — asic margin TechnicalRecordIdRule whole-file lock (R1 gate widening + producer recovery). */
    @Test
    @EnabledIf("cellsAvailable")
    void corpus_c1_asicMarginTechnicalRecordIdRuleByteIdentical() throws IOException {
        lock(drr561Output, drr561GenErrors, DRR561_GOLDEN, DRR561_CARRIERS.get(0), "drr 5.61.0");
    }

    /** c2 — esma PTRRIDRule whole-file lock (R1 binding rung). */
    @Test
    @EnabledIf("cellsAvailable")
    void corpus_c2_esmaPtrridRuleByteIdentical() throws IOException {
        lock(drr561Output, drr561GenErrors, DRR561_GOLDEN, DRR561_CARRIERS.get(11), "drr 5.61.0");
    }

    /** c3 — drr 7.0.0 DTCC_TradeParty1TransactionIDRule whole-file lock (R2). */
    @Test
    @EnabledIf("cellsAvailable")
    void corpus_c3_dtccTradeParty1TransactionIdRuleByteIdentical() throws IOException {
        lock(drr7Output, drr7GenErrors, DRR7_GOLDEN, DRR7_CARRIERS.get(0), "drr 7.0.0");
    }

    /** c4 — drr 7.0.0 ReferenceEntityRule whole-file lock (R3). */
    @Test
    @EnabledIf("cellsAvailable")
    void corpus_c4_referenceEntityRuleByteIdentical() throws IOException {
        lock(drr7Output, drr7GenErrors, DRR7_GOLDEN, DRR7_CARRIERS.get(2), "drr 7.0.0");
    }

    /** c5 — ALL 18 whole-file carriers of the two generated cells byte-identical + the partial carrier's deref line. */
    @Test
    @EnabledIf("cellsAvailable")
    void corpus_c5_allCarriersHealedOrLineLocked() throws IOException {
        for (String path : DRR561_CARRIERS) {
            lock(drr561Output, drr561GenErrors, DRR561_GOLDEN, path, "drr 5.61.0");
        }
        for (String path : DRR7_CARRIERS) {
            lock(drr7Output, drr7GenErrors, DRR7_GOLDEN, path, "drr 7.0.0");
        }
        // The blocked residue file is generated and still deref-free (the KNOWN_RESIDUE_561 pin is the
        // authority for its count; this just proves the file did not silently vanish).
        assertNotNull(drr561Output.get(DRR561_PARTIAL), "not generated: " + DRR561_PARTIAL);
    }

    /**
     * control0 — golden is the oracle (the frozen trees): the whole-output deref-block populations at
     * their exact census counts per cell, and every carrier carries at least one.
     */
    @Test
    @EnabledIf("cellsAvailable")
    void corpus_control0_goldenIsTheOracle() throws IOException {
        DerefScan g561 = scan(readGoldenTree(DRR561_GOLDEN));
        DerefScan g7 = scan(readGoldenTree(DRR7_GOLDEN));
        assertEquals(GOLDEN_561_FILES, g561.perFile.size(), "golden drr 5.61.0 deref-bearing files");
        assertEquals(GOLDEN_561_SITES, g561.sites(), "golden drr 5.61.0 deref-block sites");
        assertEquals(GOLDEN_7_FILES, g7.perFile.size(), "golden drr 7.0.0 deref-bearing files");
        assertEquals(GOLDEN_7_SITES, g7.sites(), "golden drr 7.0.0 deref-block sites");
        for (String carrier : DRR561_CARRIERS) {
            assertTrue(g561.perFile.containsKey(carrier), "golden carrier carries the deref: " + carrier);
        }
        for (String carrier : DRR7_CARRIERS) {
            assertTrue(g7.perFile.containsKey(carrier), "golden carrier carries the deref: " + carrier);
        }
    }

    // The golden census pins (measured over the frozen trees — target/seat22-instruments/f1-golden-scan.py).
    private static final int GOLDEN_561_FILES = 223;
    private static final int GOLDEN_561_SITES = 229;
    private static final int GOLDEN_7_FILES = 341;
    private static final int GOLDEN_7_SITES = 344;

    /**
     * The named PRE-EXISTING residue: the only per-file deref-block-count differences between the fork's
     * whole cells and golden AFTER the seat (other families' in-band files — LAW 73: the set, not the
     * count). Any entry that leaves is re-pinned in the same commit that heals it; a carrier may NEVER
     * appear here.
     */
    private static final List<String> KNOWN_RESIDUE_561 = List.of();
            // the two PackageIdentifierRule entries (cftc/jfsa — "the F1 deref cannot fire until the S25
            // rule-call META-wrap family lands") HEALED at seat 25 (law A, facet ruleCallMetaRewrap): the
            // MULTI+meta rule-call stage now re-wraps its elements, the pipe decl is the wrapper, and the
            // whole-output deref lands — both files are byte-identical to golden (the law-A suite's
            // corpus_c5/corpus_c8 lock them). Re-pinned here in the healing seat (LAW 81, the second time:
            // the mas UnderlyingRule entry healed the same way at seat 24's F28).
    private static final List<String> KNOWN_RESIDUE_7 = List.of();

    /**
     * control1 — the FORK's WHOLE generated cells (every kind, LAW 72): over the UNION of the files either
     * tree carries a whole-output deref block in (a missing side counts as all-zero, LAW 79), the per-file
     * counts agree FILE BY FILE except the named residue — an over-fire (a green file gaining a block
     * golden lacks) and an under-fire (a carrier keeping none) both fail here.
     */
    @Test
    @EnabledIf("cellsAvailable")
    void corpus_control1_forkWholeCellDerefCountsEqualGoldenFileByFile() throws IOException {
        assertNotNull(drr561Output, "drr 5.61.0 generation did not run");
        assertNotNull(drr7Output, "drr 7.0.0 generation did not run");
        assertEquals(KNOWN_RESIDUE_561,
                mismatches(scan(drr561Output), scan(readGoldenTree(DRR561_GOLDEN))),
                "drr 5.61.0 deref-block counts differ from golden beyond the named residue");
        assertEquals(KNOWN_RESIDUE_7,
                mismatches(scan(drr7Output), scan(readGoldenTree(DRR7_GOLDEN))),
                "drr 7.0.0 deref-block counts differ from golden beyond the named residue");
    }

    /**
     * control2 — LAW 77 route parity: the drr 7.0.0 cell generated through the REAL {@code IRGeneration}
     * seams ({@code -Pir-on}) carries the SAME per-file deref-block counts as the legacy-route render —
     * the only control that can move under an IR-route mutation (R3 reads the IR-guarded
     * {@code ruleInferredOutputRType} family).
     */
    @Test
    @EnabledIf("cellsAndIrProviderAvailable")
    void corpus_control2_irRouteDrr7DerefCountsEqualLegacyRouteFileByFile() throws IOException {
        assertNotNull(drr7Output, "drr 7.0.0 legacy-route generation did not run");
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", DRR7_CELL_ROOT), new ArrayList<>());
        List<String> mismatched = mismatches(scan(irOut), scan(drr7Output));
        assertEquals(List.of(), mismatched,
                "deref-block counts differ between the routes in " + mismatched.size() + " file(s)");
    }

    private static List<String> mismatches(DerefScan a, DerefScan b) {
        List<String> out = new ArrayList<>();
        java.util.Set<String> universe = new java.util.TreeSet<>(a.perFile.keySet());
        universe.addAll(b.perFile.keySet());
        for (String key : universe) {
            int av = a.perFile.getOrDefault(key, 0);
            int bv = b.perFile.getOrDefault(key, 0);
            if (av != bv) {
                out.add(key + " fork=" + av + " golden=" + bv);
            }
        }
        return out;
    }

    // =========================================================================
    // The scan — whole-output deref blocks per file over CODE only (comments stripped)
    // =========================================================================

    private static final class DerefScan {
        final Map<String, Integer> perFile = new LinkedHashMap<>();

        int sites() {
            int n = 0;
            for (int c : perFile.values()) n += c;
            return n;
        }
    }

    private static DerefScan scan(Map<String, String> tree) {
        DerefScan r = new DerefScan();
        for (Map.Entry<String, String> e : tree.entrySet()) {
            int n = derefBlockSites(codeOnly(e.getValue()));
            if (n > 0) {
                r.perFile.put(e.getKey(), n);
            }
        }
        return r;
    }

    /**
     * A whole-output deref block: {@code == null) {} ws {output = null;} ws {} else {} ws {output = } —
     * matched by literal token steps (no structural regex), exactly the emitter's rule-seat emission
     * (a builder output's {@code output = toBuilder(} form shares the {@code output = } prefix).
     */
    private static int derefBlockSites(String code) {
        int sites = 0, i = code.indexOf(DEREF_GUARD);
        while (i >= 0) {
            int p = skipWs(code, i + DEREF_GUARD.length());
            if (code.startsWith("output = null;", p)) {
                p = skipWs(code, p + "output = null;".length());
                if (code.startsWith("} else {", p)) {
                    p = skipWs(code, p + "} else {".length());
                    if (code.startsWith("output = ", p)) {
                        sites++;
                    }
                }
            }
            i = code.indexOf(DEREF_GUARD, i + DEREF_GUARD.length());
        }
        return sites;
    }

    private static int skipWs(String s, int i) {
        while (i < s.length() && (s.charAt(i) == ' ' || s.charAt(i) == '\t'
                || s.charAt(i) == '\n' || s.charAt(i) == '\r')) {
            i++;
        }
        return i;
    }

    private static Map<String, String> readGoldenTree(Path dir) throws IOException {
        Map<String, String> out = new LinkedHashMap<>();
        try (var stream = Files.walk(dir)) {
            for (Path p : (Iterable<Path>) stream.filter(q -> q.toString().endsWith(".java"))::iterator) {
                out.put(dir.relativize(p).toString().replace('\\', '/'), Files.readString(p));
            }
        }
        return out;
    }

    private static String codeOnly(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        int i = 0;
        while (i < s.length()) {
            if (s.startsWith("/*", i)) {
                int end = s.indexOf("*/", i + 2);
                i = end < 0 ? s.length() : end + 2;
            } else if (s.startsWith("//", i)) {
                int end = s.indexOf('\n', i);
                i = end < 0 ? s.length() : end;
            } else if (s.charAt(i) == '"') {
                int j = i + 1;
                while (j < s.length() && s.charAt(j) != '"') {
                    if (s.charAt(j) == '\\') j++;
                    j++;
                }
                sb.append(s, i, Math.min(j + 1, s.length()));
                i = j + 1;
            } else {
                sb.append(s.charAt(i));
                i++;
            }
        }
        return sb.toString();
    }

    // =========================================================================
    // Corpus generation (the same harness every seat suite uses)
    // =========================================================================

    private static Map<String, String> drr561Output;
    private static Map<String, String> drr7Output;
    private static List<String> drr561GenErrors = new ArrayList<>();
    private static List<String> drr7GenErrors = new ArrayList<>();

    @BeforeAll
    static void generateCells() throws IOException {
        if (cellsAvailable()) {
            List<String> errs561 = new ArrayList<>();
            drr561Output = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "5.61.0", DRR561_CELL_ROOT), errs561);
            drr561GenErrors = errs561;
            List<String> errs7 = new ArrayList<>();
            drr7Output = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", DRR7_CELL_ROOT), errs7);
            drr7GenErrors = errs7;
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

    /** The cell through the REAL {@code IRGeneration} seams — the D11 ON ring's wiring. */
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

    private static void lock(Map<String, String> output, List<String> genErrors, Path goldenRoot,
            String path, String cellName) throws IOException {
        assertNotNull(output, cellName + " generation did not run — corpus unavailable?");
        List<String> lockedErrors = genErrors.stream().filter(e -> e.contains(path)).toList();
        assertTrue(lockedErrors.isEmpty(),
                "the generator reported errors for the locked file " + path + ": " + lockedErrors);
        String generated = output.get(path);
        assertNotNull(generated, "not generated in " + cellName + ": " + path);
        Path goldenPath = goldenRoot.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "golden missing: " + goldenPath);
        assertEquals(normalize(Files.readString(goldenPath)), normalize(generated),
                "generated " + cellName + " output must byte-match golden (newline-normalized) for "
                + path + " — seat 22 F1: the whole-output META deref.");
    }

    // =========================================================================
    // Fixture harness
    // =========================================================================

    private static RLinkingResult linking;
    private static RModel mainModel;
    private static Map<String, String> fixtureOut;
    private static Map<String, String> fixtureOutIr;

    private static void link() throws IOException {
        if (linking == null) {
            RModel main = AstBuilder.buildFromString(MODEL, "seat22f1.rosetta");
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
            fixtureOut = render(m -> "census.seat22f1".equals(m.namespace()));
        }
        return fixtureOut;
    }

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
                        m -> "census.seat22f1".equals(m.namespace()));
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
            throw new AssertionError("[WholeOutputMetaDerefSeatTest] builtins parse"
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
