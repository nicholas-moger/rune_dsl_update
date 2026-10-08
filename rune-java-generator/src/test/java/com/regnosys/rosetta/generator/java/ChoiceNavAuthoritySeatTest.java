package com.regnosys.rosetta.generator.java;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.object.datarule.DataRuleGenerator;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static com.regnosys.rosetta.testutil.GenerationErrors.assertNoGenerationErrors;

/**
 * v3.1 LADDER RETIREMENT — flip seat 1: the choice-option-nav authority read
 * (the #570 census's drr 7.x SAME_DENOTATION class, 189/cell 100% in-band, plus
 * the FC AUTH_ONLY choice-option mass it chains into).
 *
 * <p><b>The measured defect (probed on drr 7.0.0 before the seat, 2026-08-16):</b>
 * the emitters resolve a choice-option hop ({@code payout -> OptionPayout},
 * {@code underlier -> Observable -> Asset}) by STRUCTURAL re-derivation only —
 * {@code NavigationHandler.fallbackResolveFeature} narrows the receiver's choice
 * via {@code RChoiceTypeRef.asRDataType()} and projects the option to a synthetic
 * attribute whose {@code RTypeCall.deepCopy} deliberately DROPS the resolved
 * {@code referencedTypeId}. Resolution of that floating typeCall then rides
 * {@code GeneratorModel.resolveFromWorkspace}, which filters by
 * {@code shouldGenerate} — so in a cell that navigates option types living in a
 * NON-generated dependency namespace (every drr cell navigating vendored CDM
 * types) the projected attribute is unresolvable:
 * <ul>
 *   <li>the {@code <Type>} witness TEXT still renders (name-driven) but its
 *       import never registers ({@code addWitnessTypeRef} declines) — the E1
 *       import-only mismatch class ({@code IsProductWithUnderlier}'s entire
 *       diff);</li>
 *   <li>every hop chained AFTER the option loses receiver typing — witnesses
 *       drop, lambda params degrade to raw feature names
 *       ({@code Asset -> Asset.getCommodity()}), the meta-coercion witness
 *       drops ({@code IsCommoditySwap_SingleIndex});</li>
 *   <li>same-workspace cells (cdm 6.x) resolve by name-search and stay green —
 *       which is exactly the census's measured cell split.</li>
 * </ul>
 *
 * <p><b>The seat:</b> the emitters consume the C1 AUTHORITY slots
 * ({@code RFeatureCall.resolvedFeatureNode} / {@code REnumValueRef.resolvedFeatureNode})
 * authority-first-with-legacy-fallback at the three Path-1 resolution sites:
 * {@code fallbackResolveFeature} (the single choke point every witness / import /
 * cardinality / lambda-naming consumer routes through), the
 * {@code resolveReceiverDataType0} EVR arm, and the
 * {@code ReferenceHandler.synthesizeFeatureCall} carry. The authority's
 * {@link com.regnosys.rosetta.ast.supporting.RChoiceOption} is a PARENTED node
 * whose typeCall carries the linker's {@code referencedTypeId}, so the projected
 * attribute resolves through {@code workspace.resolveTypeLike} regardless of the
 * generation filter. Where the authority slot is empty or non-option, every
 * existing path runs unchanged (the fallback contract).
 *
 * <p><b>Test geometry:</b>
 * <ul>
 *   <li><b>Cross-namespace controls</b> — the drr mechanics at unit grain: option
 *       types in {@code census.dep} (NOT generated, the vendored-CDM stand-in),
 *       functions in {@code census.gen} (generated). RED before the seat.</li>
 *   <li><b>Same-workspace inert pins</b> — the seat's adversarial PLACEMENT cases
 *       (alias head, input head, chained hop, if-arm, fn-call argument, bare
 *       option in extract, data-type condition, meta chain), frozen byte-for-byte
 *       from the pre-seat render (all green today). The seat must not move ONE of
 *       them: same-workspace resolution already succeeds, and the authority read
 *       resolves the SAME declaration (the census's SAME_DENOTATION predicate).
 *       GREEN before AND after — the clean-cell obligation at unit grain.</li>
 *   <li><b>drr 7.0.0 corpus locks</b> — the census carrier files through the REAL
 *       D11 FUNCTION route: {@code IsProductWithUnderlier} whole-file byte lock
 *       (import-only diff) + {@code IsCommoditySwap_SingleIndex} occurrence-counted
 *       witnesses (python {@code str.count} semantics — the #352 law). RED before
 *       the seat. The deep-path {@code ->>} residue ({@code IndexDeepPathUtil}
 *       wiring) is deliberately NOT this seat's claim and stays unasserted.</li>
 * </ul>
 */
class ChoiceNavAuthoritySeatTest {

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
                && Files.isDirectory(DRR7_GOLDEN_DIR), ChoiceNavAuthoritySeatTest.class);
    }

    // =========================================================================
    // Control models
    // =========================================================================

    /** The dependency namespace — NOT generated (the vendored-CDM stand-in). */
    private static final String DEP_MODEL = """
            namespace census.dep
            version "1.0.0"
            type Basket:
                identifier string (1..1)
            type Security:
                identifier string (1..1)
                notional number (1..1)
            choice Underlier:
                Basket
                Security
            type Wrapper:
                underlier Underlier (1..1)
            type Observation:
                obsId string (1..1)
            type Direct:
                dirId string (1..1)
            choice Source:
                Observation
                    [metadata reference]
                Direct
            type MetaHolder:
                source Source (1..1)
            """;

    /** The generated namespace navigating census.dep's choices — the drr shape. */
    private static final String GEN_MODEL = """
            namespace census.gen
            version "1.0.0"
            import census.dep.*
            func AliasOptionNav:
                inputs:
                    wrapper Wrapper (1..1)
                output:
                    result boolean (1..1)
                alias und: wrapper -> underlier
                set result:
                    und -> Basket exists or und -> Security exists
            func ChainAfterOption:
                inputs:
                    wrapper Wrapper (1..1)
                output:
                    result number (0..1)
                set result:
                    wrapper -> underlier -> Security -> notional
            func MetaOptionChain:
                inputs:
                    holder MetaHolder (1..1)
                output:
                    result string (0..1)
                set result:
                    holder -> source -> Observation -> obsId
            """;

    /** Same-workspace placements — every type and function generated together. */
    private static final String SEAT_MODEL = """
            namespace census.seat
            version "1.0.0"
            type Basket:
                identifier string (1..1)
            type Security:
                identifier string (1..1)
                notional number (1..1)
            choice Underlier:
                Basket
                Security
            type Wrapper:
                underlier Underlier (1..1)
            type Holder:
                underlier Underlier (1..*)
                condition BasketOnly:
                    underlier -> Basket exists
            func AliasHeadNav:
                inputs:
                    wrapper Wrapper (1..1)
                output:
                    result boolean (1..1)
                alias und: wrapper -> underlier
                set result:
                    und -> Basket exists or und -> Security exists
            func InputHeadNav:
                inputs:
                    und Underlier (1..1)
                output:
                    result string (0..1)
                set result:
                    und -> Basket -> identifier
            func ChainAfterOption:
                inputs:
                    wrapper Wrapper (1..1)
                output:
                    result number (0..1)
                set result:
                    wrapper -> underlier -> Security -> notional
            func IfArmNav:
                inputs:
                    und Underlier (1..1)
                    flag boolean (1..1)
                output:
                    result string (0..1)
                set result:
                    if flag = True
                    then und -> Basket -> identifier
                    else und -> Security -> identifier
            func TakesId:
                inputs:
                    id string (0..1)
                output:
                    result boolean (1..1)
                set result:
                    id exists
            func ArgSeatNav:
                inputs:
                    und Underlier (1..1)
                output:
                    result boolean (1..1)
                set result:
                    TakesId(und -> Basket -> identifier)
            func BareOptionExtract:
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

    /** Same-workspace meta-annotated option — the coercion-witness chain shape. */
    private static final String SEAT_META_MODEL = """
            namespace census.seatmeta
            version "1.0.0"
            type Observation:
                obsId string (1..1)
            type Direct:
                dirId string (1..1)
            choice Source:
                Observation
                    [metadata reference]
                Direct
            type Holder:
                source Source (1..1)
            func MetaOptionChain:
                inputs:
                    holder Holder (1..1)
                output:
                    result string (0..1)
                set result:
                    holder -> source -> Observation -> obsId
            """;

    // =========================================================================
    // Render harnesses
    // =========================================================================

    private record Rendered(Map<String, String> functions, Map<String, String> dataRules) {}

    /**
     * Renders the given models' functions + data rules; {@code generatedNamespace}
     * limits emission (null = generate everything), reproducing the drr cells'
     * generated-vs-dependency namespace split.
     */
    private static Rendered render(String generatedNamespace, String... sources) {
        List<RModel> models = new ArrayList<>();
        try (var stream = Files.walk(BUILTINS_DIR)) {
            stream.filter(p -> p.toString().endsWith(".rosetta")).sorted()
                    .forEach(p -> models.add(AstBuilder.buildFromFile(p)));
        } catch (IOException e) {
            throw new AssertionError("builtins walk failed: " + e.getMessage(), e);
        }
        for (int i = 0; i < sources.length; i++) {
            models.add(AstBuilder.buildFromString(sources[i], "seat-control-" + i + ".rosetta"));
        }
        var workspace = RWorkspace.build(models).workspace();
        var gm = generatedNamespace == null
                ? new GeneratorModel(workspace)
                : new GeneratorModel(workspace,
                        m -> generatedNamespace.equals(m.namespace()));
        var typeUtil = new JavaTypeUtil();
        var typeTranslator = new JavaTypeTranslator(typeUtil);
        var funcGen = new FunctionGenerator(gm, typeTranslator, typeUtil);
        Map<String, String> functions = new LinkedHashMap<>();
        assertNoGenerationErrors(funcGen.generateWithErrors(functions));
        Map<String, String> dataRules = new LinkedHashMap<>();
        var dataRuleGen = new DataRuleGenerator(gm, typeTranslator, typeUtil);
        for (RModel model : workspace.files()) {
            if (gm.shouldGenerate(model)) {
                assertNoGenerationErrors(dataRuleGen.generateClasses(model, "1.0.0", dataRules));
            }
        }
        return new Rendered(functions, dataRules);
    }

    private static Rendered crossNamespace;
    private static Rendered sameWorkspace;
    private static Rendered sameWorkspaceMeta;

    @BeforeAll
    static void renderControls() {
        if (builtinsAvailable()) {
            crossNamespace = render("census.gen", DEP_MODEL, GEN_MODEL);
            sameWorkspace = render(null, SEAT_MODEL);
            sameWorkspaceMeta = render(null, SEAT_META_MODEL);
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
     * The E1 import-only class at unit grain: the option-nav witness TEXT renders
     * today, its import does not (PRE: text 1/1, imports 0/0).
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void crossNamespace_aliasOptionNav_witnessImportsRegister() {
        String gen = fn(crossNamespace, "census/gen/functions/AliasOptionNav.java");
        assertEquals(1, count(gen, ".<Basket>map(\"getBasket\", underlier -> underlier.getBasket())"),
                "the option hop's witness text (PRE 1 — name-driven either way)");
        assertEquals(1, count(gen, ".<Security>map(\"getSecurity\", underlier -> underlier.getSecurity())"),
                "the second option hop's witness text (PRE 1)");
        assertEquals(1, count(gen, "import census.dep.Basket;"),
                "the option type's witness import must register through the authority "
                        + "slot's resolved typeCall (PRE 0 — resolveFromWorkspace is "
                        + "shouldGenerate-filtered, the floating deep-copy never resolves)");
        assertEquals(1, count(gen, "import census.dep.Security;"),
                "the second option type's witness import (PRE 0)");
    }

    /**
     * The degraded-chain class at unit grain: every hop AFTER the option loses its
     * witness and its lambda param degrades to the raw feature name (PRE: the
     * degraded {@code Security -> Security.getNotional()} form).
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void crossNamespace_chainAfterOption_witnessesCascade() {
        String gen = fn(crossNamespace, "census/gen/functions/ChainAfterOption.java");
        assertEquals(1, count(gen, ".<Security>map(\"getSecurity\", underlier -> underlier.getSecurity())"),
                "the option hop's witness (PRE 1)");
        assertEquals(1, count(gen, ".<BigDecimal>map(\"getNotional\", security -> security.getNotional())"),
                "the hop AFTER the option must recover its witness + typed lambda param "
                        + "(PRE 0 — the projected option attribute is unresolvable, so the "
                        + "chained receiver typing dies)");
        assertEquals(0, count(gen, "Security -> Security.getNotional()"),
                "the degraded feature-name-as-param form is gone (PRE 1)");
        assertEquals(1, count(gen, "import census.dep.Security;"),
                "the option type's witness import (PRE 0)");
    }

    /**
     * The meta-option coercion class at unit grain: the wrapper witness, the
     * coercion witness and the trailing hop all recover (PRE: all dropped).
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void crossNamespace_metaOptionChain_coercionWitnessRecovers() {
        String gen = fn(crossNamespace, "census/gen/functions/MetaOptionChain.java");
        assertEquals(1, count(gen, ".<ReferenceWithMetaObservation>map(\"getObservation\", source -> source.getObservation())"),
                "the meta option hop's wrapper witness (PRE 0)");
        assertEquals(1, count(gen, ".<Observation>map(\"Type coercion\""),
                "the meta-deref coercion witness (PRE 0)");
        assertEquals(1, count(gen, ".<String>map(\"getObsId\", observation -> observation.getObsId())"),
                "the hop after the deref recovers its witness + typed param (PRE 0)");
        assertEquals(1, count(gen, "import census.dep.Observation;"),
                "the option base type's import (PRE 0)");
        assertEquals(1, count(gen, "import census.dep.metafields.ReferenceWithMetaObservation;"),
                "the wrapper's import (PRE 0)");
    }

    // =========================================================================
    // Part B — same-workspace inert pins (GREEN before AND after — the
    // clean-cell obligation at unit grain; frozen from the pre-seat render)
    // =========================================================================

    @Test
    @EnabledIf("builtinsAvailable")
    void inertPin_aliasHeadNav() {
        String gen = fn(sameWorkspace, "census/seat/functions/AliasHeadNav.java");
        assertEquals(1, count(gen,
                "result = exists(und(wrapper).<Basket>map(\"getBasket\", underlier -> underlier.getBasket())).orNullSafe(exists(und(wrapper).<Security>map(\"getSecurity\", underlier -> underlier.getSecurity()))).get();"),
                "the alias-head option nav's assignOutput is byte-frozen");
        assertEquals(1, count(gen, "import census.seat.Basket;"), "the import set is frozen");
        assertEquals(1, count(gen, "import census.seat.Security;"), "the import set is frozen");
    }

    @Test
    @EnabledIf("builtinsAvailable")
    void inertPin_inputHeadNav() {
        String gen = fn(sameWorkspace, "census/seat/functions/InputHeadNav.java");
        assertEquals(1, count(gen,
                "result = MapperS.of(und).<Basket>map(\"getBasket\", underlier -> underlier.getBasket()).<String>map(\"getIdentifier\", basket -> basket.getIdentifier()).get();"),
                "the input-head option nav's assignOutput is byte-frozen");
    }

    @Test
    @EnabledIf("builtinsAvailable")
    void inertPin_chainAfterOption() {
        String gen = fn(sameWorkspace, "census/seat/functions/ChainAfterOption.java");
        assertEquals(1, count(gen,
                "result = MapperS.of(wrapper).<Underlier>map(\"getUnderlier\", _wrapper -> _wrapper.getUnderlier()).<Security>map(\"getSecurity\", underlier -> underlier.getSecurity()).<BigDecimal>map(\"getNotional\", security -> security.getNotional()).get();"),
                "the same-workspace chained option nav's assignOutput is byte-frozen");
    }

    @Test
    @EnabledIf("builtinsAvailable")
    void inertPin_ifArmNav() {
        String gen = fn(sameWorkspace, "census/seat/functions/IfArmNav.java");
        assertEquals(1, count(gen,
                "result = MapperS.of(und).<Basket>map(\"getBasket\", underlier -> underlier.getBasket()).<String>map(\"getIdentifier\", basket -> basket.getIdentifier()).get();"),
                "the then-arm option nav is byte-frozen");
        assertEquals(1, count(gen,
                "result = MapperS.of(und).<Security>map(\"getSecurity\", underlier -> underlier.getSecurity()).<String>map(\"getIdentifier\", security -> security.getIdentifier()).get();"),
                "the else-arm option nav is byte-frozen");
    }

    @Test
    @EnabledIf("builtinsAvailable")
    void inertPin_argSeatNav() {
        String gen = fn(sameWorkspace, "census/seat/functions/ArgSeatNav.java");
        assertEquals(1, count(gen,
                "result = takesId.evaluate(MapperS.of(und).<Basket>map(\"getBasket\", underlier -> underlier.getBasket()).<String>map(\"getIdentifier\", basket -> basket.getIdentifier()).get());"),
                "the fn-call-argument option nav is byte-frozen");
    }

    /** The a2-adjacent negative control: a BARE option name is NOT this seat's arm. */
    @Test
    @EnabledIf("builtinsAvailable")
    void inertPin_bareOptionExtract() {
        String gen = fn(sameWorkspace, "census/seat/functions/BareOptionExtract.java");
        assertEquals(1, count(gen,
                "if (exists(item.<Basket>map(\"getBasket\", underlier -> underlier.getBasket())).getOrDefault(false)) {"),
                "the bare-option-in-extract render is byte-frozen (the SYM-side class "
                        + "is deliberately NOT this seat)");
    }

    /** The data-type-condition placement — the cdm 6.x SAME_DENOTATION obligation shape. */
    @Test
    @EnabledIf("builtinsAvailable")
    void inertPin_conditionDataRule() {
        String gen = sameWorkspace.dataRules()
                .get("census/seat/validation/datarule/HolderBasketOnly.java");
        assertNotNull(gen, "missing datarule render (got: "
                + sameWorkspace.dataRules().keySet() + ")");
        assertEquals(1, count(gen,
                "return exists(MapperS.of(holder).<Underlier>mapC(\"getUnderlier\", _holder -> _holder.getUnderlier()).<Basket>map(\"getBasket\", underlier -> underlier.getBasket()));"),
                "the condition-seated option nav's datarule render is byte-frozen");
    }

    @Test
    @EnabledIf("builtinsAvailable")
    void inertPin_metaOptionChain() {
        String gen = fn(sameWorkspaceMeta, "census/seatmeta/functions/MetaOptionChain.java");
        assertEquals(1, count(gen,
                "result = MapperS.of(holder).<Source>map(\"getSource\", _holder -> _holder.getSource()).<ReferenceWithMetaObservation>map(\"getObservation\", source -> source.getObservation()).<Observation>map(\"Type coercion\", referenceWithMetaObservation -> referenceWithMetaObservation == null ? null : referenceWithMetaObservation.getValue()).<String>map(\"getObsId\", observation -> observation.getObsId()).get();"),
                "the same-workspace meta option chain's assignOutput is byte-frozen");
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
     * The census's cleanest carrier: the body is ALREADY byte-identical and the
     * whole diff is the two unregistered option-type witness imports (PRE: red on
     * exactly {@code import cdm.product.template.OptionPayout;} +
     * {@code import cdm.product.template.SettlementPayout;}).
     */
    @Test
    @EnabledIf("drr7Available")
    void drr7_isProductWithUnderlier_byteMatchesGolden() throws IOException {
        String path = "drr/base/qualification/product/functions/IsProductWithUnderlier.java";
        String gen = drr7().get(path);
        assertNotNull(gen, "missing generated output: " + path);
        String golden = Files.readString(DRR7_GOLDEN_DIR.resolve(path));
        assertEquals(golden.replace("\r\n", "\n"), gen.replace("\r\n", "\n"),
                path + " must byte-match the frozen 9.83.0 golden (the whole pre-seat "
                        + "diff is the two witness imports)");
    }

    /**
     * The degraded-chain carrier ({@code commodityUnderlier -> Observable -> Asset
     * -> Commodity -> identifier}): occurrence-counted witnesses. The deep-path
     * {@code ->> assetClass} residue ({@code IndexDeepPathUtil} wiring) is NOT
     * this seat's claim and is deliberately unasserted.
     */
    @Test
    @EnabledIf("drr7Available")
    void drr7_isCommoditySwapSingleIndex_chainWitnesses() throws IOException {
        String gen = drr7().get(
                "drr/base/qualification/product/functions/IsCommoditySwap_SingleIndex.java");
        assertNotNull(gen, "missing generated output: IsCommoditySwap_SingleIndex");
        assertEquals(1, count(gen, "import cdm.observable.asset.Observable;"),
                "the Observable option's witness import (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "import cdm.base.staticdata.asset.common.Asset;"),
                "the Asset option's witness import (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "import cdm.base.staticdata.asset.common.Commodity;"),
                "the Commodity option's witness import (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "import cdm.product.asset.CommodityPayout;"),
                "the CommodityPayout option's witness import (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "import cdm.observable.asset.metafields.ReferenceWithMetaObservable;"),
                "the meta option's wrapper import (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "import cdm.observable.asset.Index;"),
                "the Index option's witness import (PRE 0 / golden 1)");
        assertEquals(2, count(gen, ".<Observable>map(\"Type coercion\""),
                "the meta-deref coercion witnesses (PRE 0 / golden 2)");
        assertEquals(1, count(gen, ".<Asset>map(\"getAsset\", observable -> observable.getAsset())"),
                "the Asset option hop's witness + typed param (PRE 0 / golden 1)");
        assertEquals(1, count(gen, ".<Commodity>map(\"getCommodity\", asset -> asset.getCommodity())"),
                "the Commodity option hop's witness + typed param (PRE 0 / golden 1)");
        assertEquals(1, count(gen, ".<Index>map(\"getIndex\", observable -> observable.getIndex())"),
                "the Index option hop's witness + typed param (PRE 0 / golden 1)");
        assertEquals(0, count(gen, "Asset -> Asset.getCommodity()"),
                "the degraded feature-name-as-param form is gone (PRE 1 / golden 0)");
        assertEquals(0, count(gen, "Commodity -> Commodity.getIdentifier()"),
                "the degraded feature-name-as-param form is gone (PRE 1 / golden 0)");
    }
}
