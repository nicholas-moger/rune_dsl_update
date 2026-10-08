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
 * v3.1 LADDER RETIREMENT — flip seat 2: the bare-option SYM authority read and the
 * EVR.left chain-head authority read (the #570 census's remaining REBIND classes:
 * SYM 13/cell drr7 + 11/cell cdm6, EVR.left GAP_A_SYMBOL 13/cell drr7).
 *
 * <p><b>The measured defects (probed on drr 7.0.0 at the seat-2 open, 2026-08-17;
 * the waivered-dir dump materialised by this PR's commit 2/n):</b>
 * <ul>
 *   <li><b>SYM bare option (the a2 class):</b> a bare option name inside an
 *       implicit extract lambda ({@code extract [ CommodityPayout exists ]} over a
 *       choice item) renders through
 *       {@code ReferenceHandler.synthesizeImplicitItemBareNav}, which re-resolves
 *       the name STRUCTURALLY on the item type — the found attribute's typeCall is
 *       an id-less deep copy, so in a cell whose option types live in a
 *       NON-generated dependency namespace the witness import never registers
 *       ({@code resolveFromWorkspace} is {@code shouldGenerate}-filtered): the
 *       witness TEXT renders name-driven, the IMPORT drops — measured whole on
 *       {@code IsSingleCommodityPayoutProduct} (the entire pre-seat diff is
 *       {@code import cdm.product.asset.CommodityPayout;}). Same-workspace cells
 *       (cdm 6.x) resolve by name-search and stay green — the census's cell split
 *       at the BARE seat, the exact seat-1 mechanism one arm over.</li>
 *   <li><b>EVR.left GAP_A head (the alias-shadow class):</b> a chain headed by a
 *       name that is BOTH a local alias and a global callable
 *       ({@code TradeForEvent -> product} under {@code alias TradeForEvent:} where
 *       {@code drr.base.trade.TradeForEvent} exists) renders alias-shaped
 *       (name-driven, byte-correct) — but {@code FunctionDependencyCollector}
 *       reads the legacy GAP_A rung ({@code resolvedSymbol()}), which bound the
 *       GLOBAL function, and injects a FALSE dependency: measured on
 *       {@code Create_AnnaDsbUpiRequestFromReportableEventAndUnderlying} as the
 *       two extra lines {@code import drr.base.trade.functions.TradeForEvent;} +
 *       {@code @Inject protected TradeForEvent tradeForEvent;} the golden does
 *       not carry.</li>
 * </ul>
 *
 * <p><b>The seat:</b> authority-first-with-legacy-fallback at the two consuming
 * reads — {@code synthesizeImplicitItemBareNav} claims the C1 authority slot
 * ({@code RSymbolReference.resolvedFeatureNode}) through seat 1's
 * {@code projectedOptionAttribute} (the id+attach pair, LAW 62) before the
 * structural name-search; {@code FunctionDependencyCollector}'s three EVR head
 * reads resolve the head authority-first ({@code REnumValueRef.resolvedHead})
 * so a local-alias head is never a function/rule dependency. Empty authority →
 * every existing path unchanged.
 *
 * <p><b>Test geometry (the seat-1 pattern):</b> cross-namespace controls (RED
 * before the seat) + same-workspace inert pins (GREEN before AND after; the
 * clean-cell obligation — cdm 6.x REBIND rows are byte-exact today and must not
 * move) + drr 7.0.0 corpus locks (RED before the seat). Seat 1's
 * {@code inertPin_bareOptionExtract} (the same-workspace bare-option render)
 * doubles as this seat's primary inert enforcement and stays green in its own
 * class.
 */
class BareOptionChainHeadAuthoritySeatTest {

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
                && Files.isDirectory(DRR7_GOLDEN_DIR), BareOptionChainHeadAuthoritySeatTest.class);
    }

    // =========================================================================
    // Control models
    // =========================================================================

    /** The dependency namespace — NOT generated (the vendored-CDM stand-in). */
    private static final String DEP_MODEL = """
            namespace census.dep2
            version "1.0.0"
            type Basket:
                identifier string (1..1)
            type Security:
                identifier string (1..1)
            choice Underlier:
                Basket
                Security
            type Holder:
                underlier Underlier (1..*)
            """;

    /**
     * The generated namespace: a bare option name inside an implicit extract
     * lambda over the dependency choice — the drr a2 shape
     * ({@code IsSingleCommodityPayoutProduct}) at unit grain. The bare
     * {@code Basket} is ALSO a global type name in census.dep2, so the legacy
     * SYMBOL slot binds the global (the a2 REBIND), the authority slot binds
     * Underlier's ChoiceOption.
     */
    private static final String GEN_MODEL = """
            namespace census.gen2
            version "1.0.0"
            import census.dep2.*
            func BareOptionQualify:
                inputs:
                    holder Holder (1..1)
                output:
                    result boolean (0..*)
                set result:
                    holder -> underlier
                        extract
                            if Basket exists
                            then True
                            else False
            """;

    /**
     * The GAP_A shadow at unit grain, same-workspace: {@code TradeLookup} is BOTH
     * a global function and a local alias; the chain {@code TradeLookup -> tradeId}
     * must render the ALIAS (it does today, name-driven) and must NOT inject the
     * global function as a dependency (it does today — the false-dependency
     * defect).
     */
    private static final String GAPA_MODEL = """
            namespace census.gapa
            version "1.0.0"
            type Trade:
                tradeId string (1..1)
            type Holder:
                trade Trade (1..1)
            func TradeLookup:
                inputs:
                    holder Holder (1..1)
                output:
                    trade Trade (0..1)
                set trade:
                    holder -> trade
            func UseShadowedAlias:
                inputs:
                    holder Holder (1..1)
                output:
                    result string (0..1)
                alias TradeLookup:
                    holder -> trade
                set result:
                    TradeLookup -> tradeId
            """;

    /**
     * The GENUINE function-head controls (LAW 60 — the fallback direction):
     * {@code UseFnHead}'s parenthesized call head parses as an
     * {@link com.regnosys.rosetta.ast.expressions.references.RSymbolReference}
     * (the collector's UNTOUCHED arm — the regression control for the
     * neighbouring read); {@code UseBareFnHead}'s parens-less bare head parses
     * as the {@code REnumValueRef} shape the seat's {@code evrChainHead} read
     * serves — with NO local alias the authority slot is empty, the legacy
     * GAP_A rung claims, and the dependency injection must survive (the
     * fallback leg of the CHANGED read, exercised directly).
     */
    private static final String GENUINE_FN_HEAD_MODEL = """
            namespace census.fnhead
            version "1.0.0"
            type Trade:
                tradeId string (1..1)
            type Holder:
                trade Trade (1..1)
            func LookupTrade:
                inputs:
                    holder Holder (1..1)
                output:
                    trade Trade (0..1)
                set trade:
                    holder -> trade
            func UseFnHead:
                inputs:
                    holder Holder (1..1)
                output:
                    result string (0..1)
                set result:
                    LookupTrade(holder) -> tradeId
            func UseBareFnHead:
                inputs:
                    holder Holder (1..1)
                output:
                    result string (0..1)
                set result:
                    LookupTrade -> tradeId
            """;

    // =========================================================================
    // Render harness (the seat-1 pattern)
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
            models.add(AstBuilder.buildFromString(sources[i], "seat2-control-" + i + ".rosetta"));
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
    private static Rendered gapaShadow;
    private static Rendered genuineFnHead;

    @BeforeAll
    static void renderControls() {
        if (builtinsAvailable()) {
            crossNamespace = render("census.gen2", DEP_MODEL, GEN_MODEL);
            gapaShadow = render(null, GAPA_MODEL);
            genuineFnHead = render(null, GENUINE_FN_HEAD_MODEL);
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
    // Part A — the SYM bare-option authority read (RED before the seat)
    // =========================================================================

    /**
     * The a2 witness-import class at unit grain: the bare option's witness TEXT
     * renders today (name-driven), its import does not (PRE: text 1, import 0 —
     * the id-less structural projection at the bare seat).
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void crossNamespace_bareOptionExtract_witnessImportRegisters() {
        String gen = fn(crossNamespace, "census/gen2/functions/BareOptionQualify.java");
        assertEquals(1, count(gen, ".<Basket>map(\"getBasket\", underlier -> underlier.getBasket())"),
                "the bare option's witness text (PRE 1 — name-driven either way)");
        assertEquals(1, count(gen, "import census.dep2.Basket;"),
                "the bare option type's witness import must register through the "
                        + "authority slot's resolved typeCall (PRE 0 — the projected "
                        + "attribute's id-less deep copy never resolves through the "
                        + "shouldGenerate-filtered name-search)");
    }

    // =========================================================================
    // Part B — the EVR.left GAP_A head authority read (RED before the seat)
    // =========================================================================

    /**
     * The false-dependency class at unit grain: the alias-shadowed chain renders
     * the ALIAS (byte-frozen anchor below) but the dependency collector reads the
     * legacy GAP_A rung and injects the shadowed GLOBAL function (PRE: @Inject 1,
     * golden semantics 0).
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void gapaShadow_aliasHeadedChain_noFalseDependency() {
        String gen = fn(gapaShadow, "census/gapa/functions/UseShadowedAlias.java");
        assertEquals(0, count(gen, "@Inject protected TradeLookup tradeLookup;"),
                "a local-alias head is NOT a function dependency — the authority "
                        + "slot binds the RShortcut, the GAP_A claim yields (PRE 1)");
        assertEquals(1, count(gen, "protected abstract MapperS<? extends Trade> TradeLookup(Holder holder);"),
                "the alias helper declaration is byte-frozen (PRE 1 — the render "
                        + "was already alias-shaped, only the dependency was false)");
        assertEquals(1, count(gen, "TradeLookup(holder).<String>map(\"getTradeId\", trade -> trade.getTradeId())"),
                "the alias-headed chain render is byte-frozen (PRE 1)");
    }

    /**
     * The neighbouring-arm regression control: a parenthesized call head is an
     * {@code RSymbolReference} — the collector arm this seat does NOT touch —
     * and keeps its dependency.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void genuineFnHead_dependencySurvives() {
        String gen = fn(genuineFnHead, "census/fnhead/functions/UseFnHead.java");
        assertEquals(1, count(gen, "@Inject protected LookupTrade lookupTrade;"),
                "a genuine function-call head keeps its injected dependency "
                        + "(GREEN before and after — the untouched RSymbolReference arm)");
    }

    /**
     * The fallback direction of the CHANGED read (LAW 60 — the positive control,
     * the indep review's IMPORTANT-1): a parens-less bare function head is the
     * {@code REnumValueRef} shape {@code evrChainHead} serves; with no shadowing
     * alias the authority slot is empty, the legacy GAP_A rung claims, and the
     * dependency survives.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void genuineBareFnHead_evrFallbackDependencySurvives() {
        String gen = fn(genuineFnHead, "census/fnhead/functions/UseBareFnHead.java");
        assertEquals(1, count(gen, "@Inject protected LookupTrade lookupTrade;"),
                "a genuine BARE function head (the EVR shape) keeps its injected "
                        + "dependency through the authority-first read's empty-authority "
                        + "fallback (GREEN before and after)");
    }

    // =========================================================================
    // Part C — drr 7.0.0 corpus locks (the whole-file + false-dependency locks
    // RED before the seat; the GetBasketConstituents import lock added from the
    // post-wire measurement)
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
     * The a2 class's cleanest carrier: the body is ALREADY byte-identical and the
     * whole pre-seat diff is the one unregistered bare-option witness import
     * ({@code import cdm.product.asset.CommodityPayout;} — the census SYM REBIND
     * row at {@code base-qualification-product-func.rosetta} offset 5749).
     */
    @Test
    @EnabledIf("drr7Available")
    void drr7_isSingleCommodityPayoutProduct_byteMatchesGolden() throws IOException {
        String path = "drr/base/qualification/product/functions/IsSingleCommodityPayoutProduct.java";
        String gen = drr7().get(path);
        assertNotNull(gen, "missing generated output: " + path);
        String golden = Files.readString(DRR7_GOLDEN_DIR.resolve(path));
        assertEquals(golden.replace("\r\n", "\n"), gen.replace("\r\n", "\n"),
                path + " must byte-match the frozen 9.83.0 golden (the whole pre-seat "
                        + "diff is the bare option's witness import)");
    }

    /**
     * The a2 class's densest carrier: five bare-option witness imports healed by
     * this seat (PRE 0 each / golden 1 each — the census SYM REBIND rows at
     * {@code base-trade-basket-func.rosetta} + {@code regulation-common-func}).
     * LOCKED FROM THE POST-WIRE MEASUREMENT (the gen-vs-golden dump diffs), not
     * the red run — the failing-first receipt covers the four cases above; this
     * lock freezes what the measured heal delivered. The file's residual band
     * diff (deep-path {@code ->>} imports, the {@code underliers}
     * alias-signature cardinality, body forms) is the standing band backlog and
     * deliberately unasserted.
     */
    @Test
    @EnabledIf("drr7Available")
    void drr7_getBasketConstituents_bareOptionWitnessImports() throws IOException {
        String gen = drr7().get("drr/base/trade/basket/functions/GetBasketConstituents.java");
        assertNotNull(gen, "missing generated output: GetBasketConstituents");
        assertEquals(1, count(gen, "import cdm.product.asset.CommodityPayout;"),
                "the CommodityPayout bare-option witness import (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "import cdm.product.template.OptionPayout;"),
                "the OptionPayout bare-option witness import (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "import cdm.product.template.PerformancePayout;"),
                "the PerformancePayout bare-option witness import (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "import cdm.product.template.SettlementPayout;"),
                "the SettlementPayout bare-option witness import (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "import cdm.observable.asset.Index;"),
                "the Index bare-option witness import (PRE 0 / golden 1)");
    }

    /**
     * The GAP_A class's carrier: the two false-dependency lines the golden does
     * not carry (PRE 1/1), with the genuine alias member pinned as the anchor
     * proving the heal removes the dependency and nothing else.
     */
    @Test
    @EnabledIf("drr7Available")
    void drr7_createAnnaDsbUpi_noFalseTradeForEventDependency() throws IOException {
        String gen = drr7().get(
                "drr/enrichment/upi/functions/Create_AnnaDsbUpiRequestFromReportableEventAndUnderlying.java");
        assertNotNull(gen, "missing generated output: Create_AnnaDsbUpiRequest...");
        assertEquals(0, count(gen, "import drr.base.trade.functions.TradeForEvent;"),
                "the shadowed global function's import is the false-dependency "
                        + "artefact (PRE 1 / golden 0)");
        assertEquals(0, count(gen, "@Inject protected TradeForEvent tradeForEvent;"),
                "the false injected dependency (PRE 1 / golden 0)");
        assertEquals(1, count(gen, "protected abstract MapperS<? extends Trade> TradeForEvent(ReportableEventBase reportableEvent);"),
                "the genuine local alias member survives (PRE 1 / golden 1 — the anchor)");
    }
}
