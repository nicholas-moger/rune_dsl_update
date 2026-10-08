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
 * v3.1 LADDER RETIREMENT — flip seat 4: the BODY-side cardinality read for a
 * then-chain rooted at a disguised ALIAS-head navigation consults the alias's
 * wrapper kind even when the disguised LEAF does not resolve.
 *
 * <p><b>The measured defect (the LAW-65 content dump over drr 7.0.0 FUNCTION,
 * 2026-08-17, post-#573):</b> {@code GetBasketConstituents}'s alias signature
 * is healed ({@code protected abstract MapperC<? extends Underlier>
 * underliers(Trade)} — byte-identical to golden) but the body still declares
 * {@code final MapperS<Basket> thenArg0 = underliers(trade).…} — a MapperC
 * chain assigned to a MapperS local, non-compiling, banded. The decl wrapper
 * reads {@code CardinalityComputer.compute() == SINGLE &&
 * !NavigationHandler.chainProvesMulti(...)}
 * ({@code FunctionExpressionRenderer.renderThenExtractSetImpl}, same pair at
 * {@code CollectionHandler.tryDeepThenHoist} / {@code mapMethod});
 * {@code chainProvesMulti}'s disguised-EVR arm resolves the leaf FIRST
 * ({@code underliers -> Observable}: valueName = a choice OPTION of the
 * cross-namespace {@code Und} — {@code resolveDisguisedFeature}'s alias arm
 * has no {@code findChoiceSuperOption} fallback and returns early), and the
 * leaf-independent #345 alias-root consult
 * ({@code disguisedAliasRootIsMulti} → {@code aliasIsMulti} =
 * {@code FunctionAliasHelper.inferShortcutIsMulti}, the SAME walk that renders
 * the signature) sits INSIDE the {@code attr != null} gate and never runs.
 *
 * <p><b>The seat (Rung A):</b> run the #345 alias-root consult in
 * {@code chainProvesMulti}'s disguised-EVR arm even when the leaf resolution
 * declines — mirroring {@code chainRendersMapperC}'s own UNGATED consult
 * (facet interior_position_coercion), so the cardinality walk and the
 * wrapper-kind walk read the alias root identically. Monotone add-only multi;
 * green-safe by the #325/#345 same-walk law (a TRUE consult means the alias
 * method in the same file is MapperC-signatured, so the chain's rendered RHS
 * rides MapperC and a MapperS-decl / mapSingleTo* consumer never compiled).
 *
 * <p><b>Test geometry (the seat-1/2/3 pattern):</b> cross-namespace controls
 * (RED before the seat) + same-workspace inert pins (GREEN before AND after)
 * + drr 7.0.0 corpus locks (RED before the seat).
 */
class AliasHeadThenChainCardinalitySeatTest {

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
                && Files.isDirectory(DRR7_GOLDEN_DIR), AliasHeadThenChainCardinalitySeatTest.class);
    }

    // =========================================================================
    // Control models
    // =========================================================================

    /** The dependency namespace — NOT generated (the vendored-CDM stand-in). */
    private static final String DEP_MODEL = """
            namespace census.dep4
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
     * The generated namespace — the {@code GetBasketConstituents} body shape at
     * unit grain: a MULTI choice-ladder alias (the seat-3 healed signature
     * shape) consumed by a then-chain whose BASE is the disguised alias-head
     * navigation {@code unds -> Prod -> economicTerms} ({@code unds -> Prod}
     * parses as {@code REnumValueRef(enumName=alias, valueName=choice-option)}
     * whose leaf dies cross-namespace) piped into a {@code then extract} step
     * — forcing the hoisted {@code thenArg} declarations.
     */
    private static final String GEN_MODEL = """
            namespace census.gen4
            version "1.0.0"
            import census.dep4.*
            func HeadChain:
                inputs:
                    holder Holder (1..1)
                output:
                    result number (0..*)
                alias unds:
                    holder -> pays
                        then if OptionPay exists
                            then OptionPay only-element -> und
                            else CommodityPay -> und
                add result:
                    unds -> Prod -> economicTerms
                        then extract notional
            """;

    /**
     * The same-workspace inert pins (everything generated — the resolutions the
     * existing rungs already serve): the SINGLE-alias head chain (the rung's
     * consult reads FALSE — LAW 60's untouched-arm control), the genuine
     * input-root disguised head (the {@code attr != null} path, untouched), and
     * the same-workspace MULTI-alias head chain (whatever the gated path
     * already produces — the clean-cell obligation, pinned GREEN before/after).
     */
    private static final String INERT_MODEL = """
            namespace census.inert4
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
            func SingleHead:
                inputs:
                    holder Holder (1..1)
                output:
                    result number (0..*)
                alias p1:
                    holder -> pays only-element
                add result:
                    p1 -> OptionPay -> und -> Prod -> economicTerms
                        then extract notional
            func InputHead:
                inputs:
                    holder Holder (1..1)
                output:
                    result number (0..*)
                add result:
                    holder -> pays -> OptionPay -> und -> Prod -> economicTerms
                        then extract notional
            func MultiHeadLocal:
                inputs:
                    holder Holder (1..1)
                output:
                    result number (0..*)
                alias unds:
                    holder -> pays
                        then if OptionPay exists
                            then OptionPay only-element -> und
                            else CommodityPay -> und
                add result:
                    unds -> Prod -> economicTerms
                        then extract notional
            func ResolvedLeafHead:
                inputs:
                    holder Holder (1..1)
                output:
                    result number (0..*)
                alias prods:
                    holder -> pays -> OptionPay -> und -> Prod
                add result:
                    prods -> economicTerms
                        then extract notional
            func CollapseHead:
                inputs:
                    holder Holder (1..1)
                output:
                    result number (0..1)
                alias unds:
                    holder -> pays
                        then if OptionPay exists
                            then OptionPay only-element -> und
                            else CommodityPay -> und
                add result:
                    unds -> Prod only-element
                        then extract economicTerms -> notional
            """;

    // =========================================================================
    // Render harness (the seat-1/2/3 pattern)
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
            models.add(AstBuilder.buildFromString(sources[i], "seat4-control-" + i + ".rosetta"));
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
            crossNamespace = render("census.gen4", DEP_MODEL, GEN_MODEL);
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
    // Part A — cross-namespace control (RED before the seat)
    // =========================================================================

    /**
     * The corpus shape at unit grain: the MULTI choice-ladder alias's disguised
     * head chain declares the thenArg {@code MapperC} and the extract selects
     * the receiver-MULTI method (PRE: {@code final MapperS<Terms> thenArg =
     * unds(holder)…} + {@code .mapSingleToItem} — a MapperS local over the
     * MapperC-signatured alias call, non-compiling; the probe-recorded receipt).
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void crossNamespace_multiAliasHeadChain_declaresMapperC() {
        String gen = fn(crossNamespace, "census/gen4/functions/HeadChain.java");
        assertEquals(1, count(gen, "final MapperC<Terms> thenArg = unds(holder)"),
                "the disguised alias-head chain rides the alias's MapperC, so the "
                        + "hoisted decl reads MapperC (PRE 0 — the leaf-gated walk "
                        + "never consulted the alias root)");
        assertEquals(0, count(gen, "MapperS<Terms> thenArg"),
                "the MapperS decl over the MapperC-riding chain must be gone (PRE 1)");
        assertEquals(1, count(gen, ".mapItem(item -> "),
                "the extract over the multi receiver selects mapItem (PRE 0 — "
                        + "mapSingleToItem)");
        assertEquals(0, count(gen, ".mapSingleToItem(item -> "),
                "the single-receiver method must be gone (PRE 1)");
    }

    // =========================================================================
    // Part B — same-workspace control (RED before the seat): the defect is
    // namespace-INDEPENDENT — the alias arm's receiver-type walk dies on the
    // choice-LADDER body itself, so the leaf gate blocks the alias-root consult
    // even where every type is generated.
    // =========================================================================

    @Test
    @EnabledIf("builtinsAvailable")
    void sameWorkspace_multiAliasHeadChain_declaresMapperC() {
        String gen = fn(sameWorkspace, "census/inert4/functions/MultiHeadLocal.java");
        assertEquals(1, count(gen, "final MapperC<Terms> thenArg = unds(holder)"),
                "the same-workspace ladder-alias head chain declares MapperC "
                        + "(PRE 0 — the ladder body defeats the receiver-type walk "
                        + "and the gated consult never runs, same as cross-namespace)");
        assertEquals(0, count(gen, "MapperS<Terms> thenArg"),
                "the MapperS decl must be gone (PRE 1)");
        assertEquals(1, count(gen, ".mapItem(item -> "),
                "mapItem selected (PRE 0 — mapSingleToItem)");
    }

    // =========================================================================
    // Part C — same-workspace inert pins (GREEN before AND after; the
    // probe-recorded PRE forms, frozen)
    // =========================================================================

    /**
     * LAW 60's untouched-arm control: a SINGLE alias head ({@code aliasIsMulti}
     * FALSE) keeps the MapperS decl + single method — the rung's consult never
     * fires.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void inertPin_singleAliasHeadKeepsMapperS() {
        String gen = fn(sameWorkspace, "census/inert4/functions/SingleHead.java");
        assertEquals(1, count(gen, "final MapperS<Terms> thenArg = p1(holder)"),
                "a single-alias head chain keeps the MapperS decl (frozen)");
        assertEquals(1, count(gen, ".mapSingleToItem(item -> "),
                "the single-receiver method stays (frozen)");
    }

    /**
     * The {@code attr != null} path, untouched: a genuine INPUT-root disguised
     * head already declares MapperC through the resolved leaf's walk.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void inertPin_inputRootHeadAlreadyMapperC() {
        String gen = fn(sameWorkspace, "census/inert4/functions/InputHead.java");
        assertEquals(1, count(gen, "final MapperC<Terms> thenArg = MapperS.of(holder)"),
                "an input-root disguised head chain already declares MapperC "
                        + "through the resolved-leaf path (frozen)");
        assertEquals(1, count(gen, ".mapItem(item -> "),
                "mapItem stays (frozen)");
    }

    /**
     * The clean-cell obligation: a plain-nav MULTI alias whose disguised leaf
     * RESOLVES (a genuine attribute, not a choice option) already declares
     * MapperC through the gated #345 consult — the hoisted consult must yield
     * the SAME answer on the already-served path.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void inertPin_resolvedLeafAliasHeadAlreadyMapperC() {
        String gen = fn(sameWorkspace, "census/inert4/functions/ResolvedLeafHead.java");
        assertEquals(1, count(gen, "final MapperC<Terms> thenArg = prods(holder)"),
                "a resolvable-leaf alias-head chain already declares MapperC "
                        + "(frozen — the clean-cell obligation)");
        assertEquals(1, count(gen, ".mapItem(item -> "),
                "mapItem stays (frozen)");
    }

    /**
     * The collapse barrier (the adversarial placement pin): a MULTI alias head
     * COLLAPSED mid-chain ({@code unds -> Prod only-element}) keeps the
     * MapperS decl — {@code chainProvesMulti}'s list-op arm answers without
     * recursing, so the hoisted consult must not leak multi through the
     * collapse.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void inertPin_collapsedAliasHeadKeepsMapperS() {
        String gen = fn(sameWorkspace, "census/inert4/functions/CollapseHead.java");
        assertEquals(1, count(gen, "final MapperS<Prod> thenArg = MapperS.of(unds(holder)"),
                "a collapsed alias-head chain keeps the MapperS decl + the "
                        + "MapperS.of re-wrap (frozen — the collapse barrier)");
        assertEquals(1, count(gen, ".mapSingleToItem(item -> "),
                "the single-receiver method stays (frozen)");
    }

    // =========================================================================
    // Part D — drr 7.0.0 corpus locks (RED before the seat)
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
     * The flagship carrier's seat-owned lines (scoped, NOT whole-file — the
     * default-shape / lambda-naming / hoisted-locals / deep-path families
     * co-occupy this file and stay banded): the thenArg0 decl rides the healed
     * {@code MapperC underliers} signature, and the boundMapperC cascade
     * selects {@code mapItemToList} + the {@code MapperListOfLists} decl at
     * thenArg1 (golden-quoted needles; the naming divergence is excluded from
     * every needle).
     */
    @Test
    @EnabledIf("drr7Available")
    void drr7_getBasketConstituents_headChainDeclaresMapperC() throws IOException {
        String gen = drr7().get("drr/base/trade/basket/functions/GetBasketConstituents.java");
        assertNotNull(gen, "missing generated output: GetBasketConstituents");
        assertEquals(1, count(gen, "final MapperC<Basket> thenArg0 = underliers(trade)"),
                "the alias-head chain decl rides the healed MapperC signature "
                        + "(PRE 0 — MapperS<Basket>, non-compiling over the "
                        + "MapperC-returning underliers call)");
        assertEquals(0, count(gen, "MapperS<Basket> thenArg0"),
                "the MapperS decl must be gone (PRE 1; golden count 0)");
        assertEquals(1, count(gen,
                ".mapItemToList(item -> MapperC.<BasketConstituent>of(getBasket.evaluate(item.get())))"),
                "the extract over the multi receiver selects mapItemToList "
                        + "(PRE 0 — mapSingleToList with the identical lambda body)");
        assertEquals(1, count(gen, "final MapperListOfLists<BasketConstituent> thenArg1"),
                "the mapItemToList producer declares the list-of-lists wrapper "
                        + "(PRE 0 — MapperC<BasketConstituent>)");
    }
}
