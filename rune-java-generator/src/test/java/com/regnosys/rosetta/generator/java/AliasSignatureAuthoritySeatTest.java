package com.regnosys.rosetta.generator.java;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.symbols.RWorkspace;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static com.regnosys.rosetta.testutil.GenerationErrors.assertNoGenerationErrors;

/**
 * v3.1 LADDER RETIREMENT — flip seat 3: the alias-analyzer typing walk reads the
 * C1 authority slots ({@code resolvedFeatureNode} / {@code resolvedHead})
 * authority-first-with-legacy-fallback, and the then-item's multiplicity flows
 * elementwise into the walk's cardinality reads.
 *
 * <p><b>The measured defects (the LAW-65 content dump over drr 7.0.0 FUNCTION,
 * 2026-08-17 — 33 of the 85 residue files carry an alias-signature typing
 * defect; the three classes below OVERLAP, 38 class-instances over the 33
 * files):</b>
 * <ul>
 *   <li><b>Raw-name echo (4 files):</b> {@code FunctionAliasHelper}'s walk is
 *       100% legacy by-name resolution; a choice-OPTION hop resolves through
 *       {@code RChoiceTypeRef.asRDataType()}'s id-less deep copies, whose
 *       {@code resolveFromWorkspace} lookup is {@code shouldGenerate}-filtered —
 *       so in a cell navigating vendored-CDM option types every hop after the
 *       option loses typing (the exact seat-1 mechanism at the analyzer), the
 *       shortcut-head lookup nulls, and {@code inferEnumValueRefType}'s terminal
 *       fallback echoes the raw head name into the signature:
 *       {@code MapperS<product>} / {@code MapperS<value>} where golden has
 *       {@code MapperS<? extends EconomicTerms>} /
 *       {@code MapperS<? extends FieldWithMetaString>}
 *       ({@code Create_AnnaDsbUpiRequestFromReportableEventAndUnderlying}).</li>
 *   <li><b>Wrong element type (7 files):</b> a conditional alias whose every arm
 *       dies at an option hop joins to null and leaks the function OUTPUT's raw
 *       type ({@code ExtractCommodityClassification}'s {@code commodityUnderlier}
 *       — golden {@code MapperS<? extends Commodity>}).</li>
 *   <li><b>Cardinality loss, S-vs-C (27 files):</b> a then-body conditional over
 *       a MULTI receiver navigates the implicit item elementwise
 *       ({@code PerformancePayout -> underlier} over {@code payout (1..*)} — a
 *       disguised EVR whose head authority is the item's ChoiceOption), but the
 *       item's multiplicity never flows into the arm walk: every arm reads
 *       single and the signature emits {@code MapperS} where golden has
 *       {@code MapperC} ({@code GetBasketConstituents}'s {@code underliers}).</li>
 * </ul>
 *
 * <p><b>The seat:</b> authority rungs (the R9 feature binding — an
 * {@code RAttribute} directly, or a ChoiceOption claimed through seat-1's
 * {@code projectedOptionAttribute} id+attach pair, LAW 62, via the new
 * workspace-direct overload of {@code NavigationHandler.authorityChoiceOptionAttr})
 * at the walk's FC / SYM-receiver / disguised-EVR seats, in BOTH type walkers
 * ({@code inferExpressionType}-side and {@code inferRTypeFromExpr}); plus the
 * elementwise multiplicity flow — {@code isReceiverMulti} gains the
 * disguised-EVR container arm (head authority {@code resolvedHead}:
 * option-over-item / item-attribute heads read the bound then-item's
 * multiplicity) and {@code isReceiverMultiInner} the implicit-item rung. Empty
 * authority → every existing path unchanged.
 *
 * <p><b>Test geometry (the seat-1/2 pattern):</b> cross-namespace controls (RED
 * before the seat) + same-workspace inert pins (GREEN before AND after — the
 * clean-cell obligation at unit grain) + drr 7.0.0 corpus locks (RED before the
 * seat).
 */
class AliasSignatureAuthoritySeatTest {

    private static final Path BUILTINS_DIR =
            Path.of("../test-corpus/rune-dsl-builtins/rune-runtime/src/main/resources/model");
    private static final Path DRR7_CELL_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path DRR7_GOLDEN_DIR =
            DRR7_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    static boolean builtinsAvailable() {
        return Files.isDirectory(BUILTINS_DIR);
    }

    static boolean drr7Available() {
        return Drr7Corpus.gate(Files.isDirectory(DRR7_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR7_GOLDEN_DIR), AliasSignatureAuthoritySeatTest.class);
    }

    // =========================================================================
    // Control models
    // =========================================================================

    /** The dependency namespace — NOT generated (the vendored-CDM stand-in). */
    private static final String DEP_MODEL = """
            namespace census.dep3
            version "1.0.0"
            type Terms:
                notional number (0..1)
            type Prod:
                economicTerms Terms (1..1)
            type Sec:
                secId string (1..1)
            choice Und:
                Prod
                Sec
            type OptionPay:
                und Und (1..1)
            type CommodityPay:
                und Und (1..1)
            choice Pay:
                OptionPay
                CommodityPay
            type Holder:
                pays Pay (1..*)
                flag boolean (1..1)
            """;

    /**
     * The generated namespace — the three drr defect classes at unit grain, each
     * navigating the NON-generated dependency choice:
     * <ul>
     *   <li>{@code AliasCardinality.unds} — the {@code underliers} S-vs-C shape:
     *       a then-conditional over the MULTI {@code pays (1..*)} whose else arm
     *       ({@code CommodityPay -> und}, a disguised EVR with a ChoiceOption
     *       head authority) navigates the item elementwise — golden semantics
     *       type the alias {@code MapperC};</li>
     *   <li>{@code AliasEcho.terms} — the {@code economicTerms} echo shape: the
     *       {@code prod -> economicTerms} disguised EVR whose shortcut head
     *       walks an option-hop chain that dies cross-namespace, echoing
     *       {@code MapperS<prod>};</li>
     *   <li>{@code AliasElement.cu} — the {@code commodityUnderlier} element
     *       shape: a conditional whose every arm ends in option hops, joining to
     *       null and leaking the fn-output type.</li>
     * </ul>
     */
    private static final String GEN_MODEL = """
            namespace census.gen3
            version "1.0.0"
            import census.dep3.*
            func AliasCardinality:
                inputs:
                    holder Holder (1..1)
                output:
                    result number (0..*)
                alias unds:
                    holder -> pays
                        then if OptionPay exists
                            then OptionPay only-element -> und
                            else CommodityPay -> und
                set result:
                    unds -> Prod -> economicTerms -> notional
            func AliasEcho:
                inputs:
                    holder Holder (1..1)
                output:
                    result number (0..1)
                alias prod:
                    holder -> pays then OptionPay only-element -> und -> Prod
                alias terms: prod -> economicTerms
                set result:
                    terms -> notional
            func AliasElement:
                inputs:
                    holder Holder (1..1)
                output:
                    result string (0..1)
                alias cu:
                    if holder -> flag
                    then holder -> pays -> OptionPay only-element -> und -> Prod
                    else holder -> pays -> CommodityPay only-element -> und -> Prod
                set result:
                    cu -> economicTerms -> notional to-string
            """;

    /**
     * The same-workspace inert pins (everything generated — the name-search
     * resolution the legacy rungs already serve): shapes whose CURRENT signature
     * is upstream-correct and which the seat must not move — the collapse ops,
     * the all-collapse conditional, the genuine-attr chain, and the option-hop
     * chain whose same-workspace projection already resolves.
     */
    private static final String INERT_MODEL = """
            namespace census.inert3
            version "1.0.0"
            type Terms:
                notional number (0..1)
            type Prod:
                economicTerms Terms (1..1)
            type Sec:
                secId string (1..1)
            choice Und:
                Prod
                Sec
            type OptionPay:
                und Und (1..1)
            type CommodityPay:
                und Und (1..1)
            choice Pay:
                OptionPay
                CommodityPay
            type Holder:
                pays Pay (1..*)
                flag boolean (1..1)
            func InertShapes:
                inputs:
                    holder Holder (1..1)
                output:
                    result number (0..1)
                alias one:
                    holder -> pays then only-element
                alias pick:
                    holder -> pays then if item exists then item only-element else item first
                alias hs:
                    holder -> pays
                alias und1:
                    holder -> pays only-element -> OptionPay -> und
                set result:
                    und1 -> Prod -> economicTerms -> notional
            """;

    // =========================================================================
    // Render harness (the seat-1/2 pattern)
    // =========================================================================

    private record Rendered(Map<String, String> functions) {}

    private static Rendered render(String generatedNamespace, String... sources) {
        List<RModel> models = new ArrayList<>();
        try (var stream = Files.walk(BUILTINS_DIR)) {
            stream.filter(p -> p.toString().endsWith(".rosetta")).sorted()
                    .forEach(p -> models.add(AstBuilder.buildFromFile(p)));
        } catch (IOException e) {
            throw new AssertionError("builtins walk failed: " + e.getMessage(), e);
        }
        for (int i = 0; i < sources.length; i++) {
            models.add(AstBuilder.buildFromString(sources[i], "seat3-control-" + i + ".rosetta"));
        }
        var workspace = RWorkspace.build(models).workspace();
        var gm = generatedNamespace == null
                ? new GeneratorModel(workspace)
                : new GeneratorModel(workspace,
                        m -> generatedNamespace.equals(m.namespace()));
        var typeUtil = new JavaTypeUtil();
        var funcGen = new FunctionGenerator(gm, new JavaTypeTranslator(typeUtil), typeUtil);
        Map<String, String> functions = new LinkedHashMap<>();
        assertNoGenerationErrors(funcGen.generateWithErrors(functions));
        return new Rendered(functions);
    }

    private static Rendered crossNamespace;
    private static Rendered sameWorkspace;

    @BeforeAll
    static void renderControls() {
        if (builtinsAvailable()) {
            crossNamespace = render("census.gen3", DEP_MODEL, GEN_MODEL);
            sameWorkspace = render(null, INERT_MODEL);
        }
    }

    private static String fn(Rendered rendered, String path) {
        String content = rendered.functions().get(path);
        assertNotNull(content, "missing generated function: " + path
                + " (got: " + rendered.functions().keySet() + ")");
        return content;
    }

    /** Occurrence count — python {@code str.count} semantics (the #352 law). */
    private static int count(String haystack, String needle) {
        int n = 0;
        int idx = 0;
        while ((idx = haystack.indexOf(needle, idx)) >= 0) {
            n++;
            idx += needle.length();
        }
        return n;
    }

    // =========================================================================
    // Part A — cross-namespace controls (RED before the seat)
    // =========================================================================

    /**
     * The S-vs-C class at unit grain: the else arm's disguised EVR
     * ({@code CommodityPay -> und}) navigates the MULTI then-item elementwise —
     * the alias is upstream-MULTI (PRE: {@code MapperS} — the item's
     * multiplicity never reaches the arm walk).
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void crossNamespace_thenConditionalElementwiseArm_typesMapperC() {
        String gen = fn(crossNamespace, "census/gen3/functions/AliasCardinality.java");
        assertEquals(1, count(gen, "protected abstract MapperC<? extends Und> unds(Holder holder);"),
                "the elementwise else arm over the multi then-item makes the alias "
                        + "MULTI (PRE 0 — the signature emitted MapperS)");
        assertEquals(0, count(gen, "protected abstract MapperS<? extends Und> unds(Holder holder);"),
                "the MapperS form must be gone (PRE 1)");
    }

    /**
     * The raw-name echo class at unit grain: the {@code prod -> economicTerms}
     * disguised EVR's shortcut head walks an option-hop chain that dies
     * cross-namespace (PRE: {@code MapperS<prod>} — the terminal fallback echoes
     * the head name); the authority rungs type the chain and the leaf resolves.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void crossNamespace_shortcutHeadOptionChain_typesLeafNotEcho() {
        String gen = fn(crossNamespace, "census/gen3/functions/AliasEcho.java");
        assertEquals(1, count(gen, "protected abstract MapperS<? extends Terms> terms(Holder holder);"),
                "the disguised-EVR alias types the leaf attribute through the "
                        + "authority-claimed option chain (PRE 0)");
        assertEquals(0, count(gen, "MapperS<prod>"),
                "the raw-name echo must be gone (PRE 1)");
        assertEquals(1, count(gen, "protected abstract MapperS<? extends Prod> prod(Holder holder);"),
                "the option-chain alias itself types the option's declared type "
                        + "(PRE 0 — the walk nulled and leaked the fn-output type)");
    }

    /**
     * The wrong-element class at unit grain: a conditional alias whose every arm
     * ends in option hops (PRE: every arm null → the fn-output raw-type leak).
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void crossNamespace_conditionalOptionArms_typeJoinedElement() {
        String gen = fn(crossNamespace, "census/gen3/functions/AliasElement.java");
        assertEquals(1, count(gen, "protected abstract MapperS<? extends Prod> cu(Holder holder);"),
                "both arms type the option's declared type and the join keeps it "
                        + "(PRE 0 — the arms nulled and the signature leaked the "
                        + "fn-output type)");
    }

    // =========================================================================
    // Part B — same-workspace inert pins (GREEN before AND after)
    // =========================================================================

    /** The #252 collapse arm: {@code then only-element} stays MapperS. */
    @Test
    @EnabledIf("builtinsAvailable")
    void inertPin_collapseThen() {
        String gen = fn(sameWorkspace, "census/inert3/functions/InertShapes.java");
        assertEquals(1, count(gen, "protected abstract MapperS<? extends Pay> one(Holder holder);"),
                "a collapsing then-body keeps MapperS (frozen — the elementwise "
                        + "flow must not over-multi the collapse ops)");
    }

    /**
     * A conditional whose EVERY arm collapses the piped item stays MapperS —
     * the elementwise flow reads the arms, not the conditional shape.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void inertPin_conditionalAllCollapseArms() {
        String gen = fn(sameWorkspace, "census/inert3/functions/InertShapes.java");
        assertEquals(1, count(gen, "protected abstract MapperS<? extends Pay> pick(Holder holder);"),
                "an all-collapse conditional then-body keeps MapperS (frozen)");
    }

    /** A genuine multi attribute chain keeps MapperC — the untouched legacy rung. */
    @Test
    @EnabledIf("builtinsAvailable")
    void inertPin_genuineAttrChain() {
        String gen = fn(sameWorkspace, "census/inert3/functions/InertShapes.java");
        assertEquals(1, count(gen, "protected abstract MapperC<? extends Pay> hs(Holder holder);"),
                "a genuine (1..*) attribute chain keeps MapperC (frozen — LAW 60's "
                        + "untouched-arm control)");
    }

    /**
     * A same-workspace option-hop alias already types correctly through the
     * name-search projection — the authority-first claim must yield the SAME
     * type (the clean-cell obligation at unit grain).
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void inertPin_sameWorkspaceOptionChain() {
        String gen = fn(sameWorkspace, "census/inert3/functions/InertShapes.java");
        assertEquals(1, count(gen, "protected abstract MapperS<? extends Und> und1(Holder holder);"),
                "a same-workspace option-hop alias keeps its type under the "
                        + "authority-first read (frozen byte-for-byte)");
    }

    // =========================================================================
    // Part C — drr 7.0.0 corpus locks (RED before the seat)
    // =========================================================================

    private static Map<String, String> drr7Functions;

    private static Map<String, String> drr7() throws IOException {
        if (drr7Functions == null) {
            var cell = new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", DRR7_CELL_ROOT);
            var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
            var gm = new GeneratorModel(corpus.workspace(),
                    D11CorpusRegressionTest.emissionFilter(cell));
            var typeUtil = new JavaTypeUtil();
            var funcGen = new FunctionGenerator(gm, new JavaTypeTranslator(typeUtil), typeUtil);
            Map<String, String> output = new LinkedHashMap<>();
            assertNoGenerationErrors(funcGen.generateWithErrors(output));
            drr7Functions = output;
        }
        return drr7Functions;
    }

    /**
     * The echo class's corpus carrier: the two signature-garbage lines and the
     * option-chain alias, healed to the golden signatures.
     */
    @Test
    @EnabledIf("drr7Available")
    void drr7_createAnnaDsbUpi_aliasSignaturesTypeGolden() throws IOException {
        String gen = drr7().get(
                "drr/enrichment/upi/functions/Create_AnnaDsbUpiRequestFromReportableEventAndUnderlying.java");
        assertNotNull(gen, "missing generated output: Create_AnnaDsbUpiRequest...");
        assertEquals(1, count(gen,
                "protected abstract MapperS<? extends EconomicTerms> economicTerms(ReportableEventBase reportableEvent);"),
                "the economicTerms alias signature (PRE 0 — MapperS<product>, the raw-name echo)");
        assertEquals(1, count(gen,
                "protected abstract MapperS<? extends FieldWithMetaString> isdaTaxonomy(ReportableEventBase reportableEvent);"),
                "the isdaTaxonomy alias signature (PRE 0 — MapperS<value>, the raw-name echo)");
        assertEquals(1, count(gen,
                "protected abstract MapperS<? extends NonTransferableProduct> product(ReportableEventBase reportableEvent);"),
                "the product alias signature (PRE 0 — the option-chain walk nulled)");
        assertEquals(0, count(gen, "MapperS<product>"), "the product echo (PRE 1)");
        assertEquals(0, count(gen, "MapperS<value>"), "the value echo (PRE 1)");
    }

    /**
     * The element class's corpus carrier: {@code commodityUnderlier} types the
     * option chain's leaf, {@code taxonomy} carries the (0..*) leaf's MULTI.
     */
    @Test
    @EnabledIf("drr7Available")
    void drr7_extractCommodityClassification_aliasSignaturesTypeGolden() throws IOException {
        String gen = drr7().get(
                "drr/regulation/common/functions/ExtractCommodityClassification.java");
        assertNotNull(gen, "missing generated output: ExtractCommodityClassification");
        assertEquals(1, count(gen,
                "protected abstract MapperS<? extends Commodity> commodityUnderlier(EconomicTerms economicTerms, TaxonomySourceEnum taxonomySource, Integer ordinal);"),
                "the commodityUnderlier alias signature (PRE 0 — the conditional's "
                        + "arms died at the option hops and leaked the fn-output type)");
        assertEquals(1, count(gen,
                "protected abstract MapperC<? extends Taxonomy> taxonomy(EconomicTerms economicTerms, TaxonomySourceEnum taxonomySource, Integer ordinal);"),
                "the taxonomy alias signature (PRE 0 — the disguised-EVR head "
                        + "echoed and the (0..*) leaf's MULTI was lost)");
    }

    /**
     * The S-vs-C class's corpus carrier: the {@code underliers} alias is
     * upstream-MULTI (the {@code PerformancePayout -> underlier} /
     * {@code CommodityPayout -> underlier} arms navigate the multi then-item
     * elementwise).
     */
    @Test
    @EnabledIf("drr7Available")
    void drr7_getBasketConstituents_underliersTypesMapperC() throws IOException {
        String gen = drr7().get("drr/base/trade/basket/functions/GetBasketConstituents.java");
        assertNotNull(gen, "missing generated output: GetBasketConstituents");
        assertEquals(1, count(gen, "protected abstract MapperC<? extends Underlier> underliers(Trade trade);"),
                "the underliers alias signature (PRE 0 — MapperS, the item "
                        + "multiplicity never flowed into the conditional's arms)");
    }

    /**
     * WHOLE-FILE lock FROM THE POST-WIRE MEASUREMENT (the seat-2 pattern): the
     * densest all-signature carrier heals whole — {@code commodityUnderlier} +
     * {@code taxonomy} were the file's ENTIRE diff, and 25 drr 7.0.0 FUNCTION
     * files left the band with it (85 → 60, zero new; the failing-first receipt
     * is the signature locks above). The body-side alias-call forms
     * ({@code mapSingleToList} vs {@code mapItemToList}, lambda naming) read the
     * ENGINE-side cardinality, not this walk — the remaining
     * {@code GetBasketConstituents}-class residue is that next lever's, and the
     * standing band backlog's, deliberately unasserted here.
     */
    @Test
    @EnabledIf("drr7Available")
    void drr7_extractCommodityClassification_byteMatchesGolden() throws IOException {
        String path = "drr/regulation/common/functions/ExtractCommodityClassification.java";
        String gen = drr7().get(path);
        assertNotNull(gen, "missing generated output: " + path);
        String golden = Files.readString(DRR7_GOLDEN_DIR.resolve(path));
        assertEquals(golden.replace("\r\n", "\n"), gen.replace("\r\n", "\n"),
                path + " must byte-match the frozen 9.83.0 golden (the whole "
                        + "pre-seat diff was the two alias signatures)");
    }
}
