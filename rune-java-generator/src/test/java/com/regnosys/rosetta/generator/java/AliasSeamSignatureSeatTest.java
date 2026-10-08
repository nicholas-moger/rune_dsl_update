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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.GenerationException;
import com.regnosys.rosetta.generator.java.function.FunctionAliasHelper;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.function.FunctionTemplateModel;
import com.regnosys.rosetta.generator.java.reports.ReportGenerator;
import com.regnosys.rosetta.generator.java.reports.RuleGenerator;
import com.regnosys.rosetta.generator.java.spi.IRGeneration;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.symbols.RWorkspace;

/**
 * PR #612, C2d retirement family 5 {@code alias-seam-signature} — every consumer that must know the
 * FORM of an alias method's declared return type (a {@code MapperS} / {@code MapperC} Mapper seam or a
 * builder form), its WILDCARD or its ELEMENT spelling moves off the rendered seam STRING
 * ({@code MapperS<X>} / {@code MapperC<? extends X>}) and onto the facts the producer rendered it from.
 *
 * <p><b>The family, verbatim from the triage</b>
 * ({@code scripts/ci/evidence-api-triage.tsv}, family {@code alias-seam-signature}): 27 ledger rows /
 * 31 occurrences over five files — 26 RETIRE-AFTER-CENSUS + 1 DEAD. The reads: a {@code MapperS<} /
 * {@code MapperC<} prefix (CARDINALITY / WRAPPER-KIND — {@code FunctionExpressionRenderer}'s alias
 * return ladder, coerce, sink and rung helpers, {@code FunctionGenerator}'s item-typed-top wrap and
 * signature wrapper import, {@code ReferenceHandler}'s evaluate-arg bridge guard, the optimised
 * route's {@code AliasValueSeamPolicy}), a {@code >} suffix plus a {@code ? extends } infix and a
 * {@code ?} scan (ELEMENT-TYPE — the typed {@code ofNull} empties, the ladder element, the optimised
 * value re-seam and element text), and, inside the walk that renders the string, a
 * {@code FieldWithMeta} / {@code ReferenceWithMeta} name prefix (META-NESS —
 * {@code FunctionAliasHelper.joinWrapperValue}).
 *
 * <p><b>The retirement channel — the producer's own facts.</b>
 * {@code FunctionTemplateModel.AliasSeam(form, wildcarded, element)} is built by the SAME branch
 * that renders the string ({@code FunctionAliasHelper.buildMapperReturnType} /
 * {@code computeReturnType} / {@code buildOutputNavBuilderReturnType}, one {@code SeamRender} each —
 * LAW 69, the producer's decision is the consumer's read, never a re-derivation from text), carried
 * by every {@code AliasModel} copy ({@code getSeam()}), re-seamed to a VALUE form where the
 * optimised hook replaced the string, and threaded through the renderer in place of the
 * {@code String aliasReturnType} parameters. The C2c census found the shared rung helpers fed by
 * THREE producers, so each records its facts beside — or instead of — its string: the alias seam,
 * the if-then-else result ladder ({@code AliasSeam.mapper(multi, false, iteDeclElement)}) and the
 * alias switch ladder ({@code AliasSeam.mapper(elementIsMulti, false, elementSimpleName)}). On the
 * optimised route (LAW 77) {@code AliasValueSeamPolicy} reads the same seam. The #247
 * collision sentinel rides as the element verbatim.
 *
 * <p><b>The census</b> (PR #612; {@code target/seat612-instruments/c2c-f5-patch.py} +
 * {@code c2c-f5-verdicts.md}, local — env-gated probes at every one of the 27 rows' seats, the
 * rendered-text answer beside the typed answer, three walks: the default-route D11 (73,672 probe
 * lines), the IR-route D11 (69,919) and the optimised route's own suite (135,729), REVERTED, never
 * committed): the two answers agreed at EVERY alias-seam arrival; the only "disagreements" (1,842
 * per walk) were the if-then-else result ladder's own {@code MapperS<…>} strings reaching the enum
 * rung wrap with no alias seam — the second producer named above (84 real wraps per walk, which a
 * swap reading the alias seam alone would have dropped); the DEAD row never fired over 24,302
 * policy arrivals; the two META name-prefix arms took zero of 572 / 700 / 934 meta joins.
 *
 * <p><b>The fixtures (Part A)</b> put one alias seam at each consumer seat a reduced model reaches
 * through the fork's own emitters — nine seats of the census's fourteen default-route seats; the
 * sink, ctor-rung, get-rung, element and meta-join seats have no fixture of their own here and are
 * witnessed by the existing alias suites (which fail under the lanes) and by the corpus carriers —
 * so the swap is pinned at unit grain per reached seat: a1 the wildcarded single
 * seam's typed empty ({@code MapperS.<Sub>ofNull()}), a2 the multi seam's typed empty
 * ({@code MapperC.<Sub>ofNull()}), a3 the constructor-bodied alias's {@code MapperS.of} coerce, a4
 * the item-typed {@code count} top's {@code MapperS.of} wrap, a5 the multi seam's signature wrapper
 * import, a6 the enum rungs of an alias return ladder, a7 the enum rungs of a function's
 * if-then-else RESULT ladder (the second producer), a8 the alias switch ladder's enum rungs and typed
 * terminal (the third producer), a9 the single rung's {@code MapperC.of(Collections.singletonList(…))}
 * wrap in a multi seam. Each asserts the wrapped / typed form POSITIVELY and the wrong form ABSENT
 * (LAW 76 witness uniqueness). The evaluate-arg bridge guard and the value-seam ADD strip guard fire
 * on the optimised route only (the reference route never wraps an alias reference) and are witnessed
 * by the optimised suite and the pair gate, not here — disclosed.
 *
 * <p><b>The corpus carriers (Part B)</b>: cdm 6.20.6 {@code MapBasketConstituentWithLocation} (the
 * #247 collision-qualified element riding into the typed empty —
 * {@code MapperS.<cdm.observable.asset.BasketConstituent>ofNull()}) and
 * {@code Create_QuantityChange} (the singleton-list wrap of a single rung in a {@code MapperC} seam),
 * byte-identical on the default route (corpus_c1), golden the oracle (control0), identical on the IR
 * route (control2, LAW 77; skips without the IR provider as its siblings' do).
 *
 * <p><b>Proven able to fail</b> by the mutation lanes ({@code target/seat612-instruments/lanes-f5.py},
 * local) at the ONE point every consumer's read goes through — the {@code AliasSeam} record's own
 * form predicates, so the alias seam, the ite ladder's and the switch ladder's seams and every route
 * lie together: lane A (every seam answers non-Mapper), lane B (every seam SINGLE), lane C (every
 * seam MULTI). The measured failing sets are transcribed into the seat's CHANGELOG entry from the
 * lane logs, never quoted from this comment; a fixture that fails under no lane is dropped or
 * reshaped and disclosed (LAW 72), never kept — a8's first, choice-keyed form was exactly that.
 *
 * <p><b>The element invariant (corpus_c2)</b>: the typed empties and the value re-seam splice
 * {@code element()} verbatim, where the retired text readers declined a {@code ?} in the item. The
 * producer records a bare simple name or the #247 sentinel — never a generic or a wildcard — and
 * corpus_c2 witnesses that over every Mapper alias seam WITH an element (an element-less Mapper seam —
 * the legacy {@code MapperS<?>} fallback — has nothing to spell and is skipped, as the consumers skip it)
 * of the cdm 6.20.6 cell and the reduced model, so a
 * future walk arm that recorded a parameterised element would fail here before it reached an
 * {@code ofNull} terminal.
 */
class AliasSeamSignatureSeatTest {

    private static final Path REPO_ROOT =
            Path.of(System.getProperty("user.dir")).resolve("..").normalize();

    private static final List<Path> BUILTINS_SEARCH_ROOTS = List.of(
            REPO_ROOT.resolve("test-corpus/rune-dsl-builtins/rune-runtime/src/main/resources/model"),
            REPO_ROOT.resolve("rune-dsl/rune-runtime/src/main/resources/model"),
            REPO_ROOT.resolve("rune-runtime/src/main/resources/model"));

    static boolean builtinsAvailable() {
        return BUILTINS_SEARCH_ROOTS.stream().anyMatch(Files::isDirectory);
    }

    /** The carrier cell — cdm 6.20.6 (a ring cell; the D11 ring locks it, this suite locks the two files). */
    private static final Path CELL_A_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path GOLDEN_A = CELL_A_ROOT.resolve("rosetta-source/src/generated/java");

    static boolean cellAAvailable() {
        return Files.isDirectory(GOLDEN_A)
                && Files.isDirectory(CELL_A_ROOT.resolve("rosetta-source/src/main/rosetta"));
    }

    static boolean irProviderOnClasspath() {
        try {
            Class.forName("com.regnosys.rosetta.generator.java.ir.IRGenerationProviderImpl");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    static boolean cellAAndIrProviderAvailable() {
        return cellAAvailable() && irProviderOnClasspath();
    }

    /** The #247 sentinel-element carrier: the typed empty spells the collision-qualified element. */
    private static final String CARRIER_SENTINEL =
            "cdm/ingest/fpml/confirmation/pricequantity/functions/MapBasketConstituentWithLocation.java";
    /** The single-rung-in-a-MapperC-seam carrier: the singleton-list wrap. */
    private static final String CARRIER_SINGLETON =
            "cdm/event/common/functions/Create_QuantityChange.java";

    // =========================================================================
    // Part A — the fixtures, one per consumer seat
    // =========================================================================

    private static final String MODEL = """
            namespace census.seat612f5
            version "1.0.0"

            enum KindEnum:
                Cash
                Physical

            type Sub:
                value number (0..1)

            type Leg:
                kind KindEnum (0..1)
                amount number (0..1)
                items number (0..*)
                sub Sub (0..1)
                subs Sub (0..*)

            type Asset:
                name string (0..1)

            type Index extends Asset:
                instrumentId string (0..1)

            func A1SingleTypedEmpty: <"a1 - a wildcarded single Mapper seam: the absent else renders MapperS.<Sub>ofNull()">
                inputs:
                    leg Leg (1..1)
                output:
                    result Sub (0..1)
                alias picked: if leg -> amount exists then leg -> sub else empty
                set result:
                    picked

            func A2MultiTypedEmpty: <"a2 - a multi Mapper seam: the absent else renders MapperC.<Sub>ofNull()">
                inputs:
                    leg Leg (1..1)
                output:
                    result Sub (0..*)
                alias many: if leg -> amount exists then leg -> subs else empty
                set result:
                    many

            func A3CtorAlias: <"a3 - a constructor-bodied alias coerces to its single seam: return MapperS.of(<ctor>)">
                inputs:
                    leg Leg (1..1)
                output:
                    result Sub (0..1)
                alias built: Sub {
                        value: leg -> amount
                    }
                set result:
                    built

            func A4CountTop: <"a4 - an item-typed count top in a single seam wraps: return MapperS.of(<chain>.resultCount())">
                inputs:
                    leg Leg (1..1)
                output:
                    result int (0..1)
                alias n: leg -> items count
                set result:
                    n

            func A5MultiChainImport: <"a5 - a multi seam's signature imports MapperC even though its chain body references none">
                inputs:
                    leg Leg (1..1)
                output:
                    result number (0..*)
                alias vals: leg -> subs -> value
                set result:
                    vals

            func A6AliasLadderEnumRungs: <"a6 - an alias return ladder's enum rungs wrap MapperS.of in a single seam">
                inputs:
                    leg Leg (1..1)
                output:
                    result KindEnum (0..1)
                alias k: if leg -> amount exists then KindEnum -> Cash else KindEnum -> Physical
                set result:
                    k

            func A7IteResultEnumRungs: <"a7 - a then-chain's if-then-else RESULT ladder (the second producer) wraps its enum rungs">
                inputs:
                    leg Leg (1..1)
                output:
                    result KindEnum (0..1)
                set result:
                    leg then if item -> amount exists then KindEnum -> Cash else KindEnum -> Physical

            func A8SwitchAliasEnumRungs: <"a8 - a type-keyed alias switch ladder (the third producer) wraps its enum rungs and types its terminal">
                inputs:
                    asset Asset (0..1)
                output:
                    result KindEnum (0..1)
                alias chosen:
                    asset switch
                        Index then
                            if instrumentId exists
                            then KindEnum -> Cash,
                        default KindEnum -> Physical
                set result:
                    chosen

            func A9SingleRungInMultiSeam: <"a9 - a single rung in a multi seam takes the singleton-list wrap">
                inputs:
                    leg Leg (1..1)
                output:
                    result Sub (0..*)
                alias one: if leg -> amount exists then leg -> subs only-element else leg -> subs
                set result:
                    one
            """;

    /** a1 — the wildcarded single seam's typed empty; the signature stays {@code MapperS<? extends Sub>}. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_singleWildcardedSeamTypedEmpty() throws IOException {
        String code = codeOnly(fn("A1SingleTypedEmpty.java"));
        assertContains(code, "MapperS<? extends Sub> picked(Leg leg)");
        assertContains(code, "return MapperS.<Sub>ofNull();");
        assertAbsent(code, "MapperC.<Sub>ofNull()");
        assertAbsent(code, "MapperS.<? extends Sub>ofNull()");
    }

    /** a2 — the multi seam's typed empty; the signature stays {@code MapperC<? extends Sub>}. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_multiSeamTypedEmpty() throws IOException {
        String code = codeOnly(fn("A2MultiTypedEmpty.java"));
        assertContains(code, "MapperC<? extends Sub> many(Leg leg)");
        assertContains(code, "return MapperC.<Sub>ofNull();");
        assertAbsent(code, "MapperS.<Sub>ofNull()");
    }

    /** a3 — the constructor-bodied alias coerces to its single seam. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a3_ctorAliasCoercesToSingleSeam() throws IOException {
        String code = codeOnly(fn("A3CtorAlias.java"));
        assertContains(code, "MapperS<? extends Sub> built(Leg leg)");
        assertContains(code, "return MapperS.of(Sub.builder()");
        assertAbsent(code, "return Sub.builder()");
    }

    /** a4 — the item-typed {@code count} top wraps in a single seam. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a4_countTopWrapsInSingleSeam() throws IOException {
        String code = codeOnly(fn("A4CountTop.java"));
        assertContains(code, "MapperS<Integer> n(Leg leg)");
        assertContains(code, "return MapperS.of(");
        assertContains(code, ".resultCount());");
        assertAbsent(code, "return MapperC.of(leg.getItems()).resultCount();");
    }

    /** a5 — the multi seam's own wrapper import rides the signature, not the body. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a5_multiSeamImportsItsWrapper() throws IOException {
        String raw = fn("A5MultiChainImport.java");
        String code = codeOnly(raw);
        assertContains(code, "MapperC<BigDecimal> vals(Leg leg)");
        assertContains(raw, "import com.rosetta.model.lib.mapper.MapperC;");
    }

    /** a6 — the alias return ladder's enum rungs wrap in a single seam (the alias producer). */
    @Test
    @EnabledIf("builtinsAvailable")
    void a6_aliasLadderEnumRungsWrap() throws IOException {
        String code = codeOnly(fn("A6AliasLadderEnumRungs.java"));
        assertContains(code, "MapperS<KindEnum> k(Leg leg)");
        assertContains(code, "return MapperS.of(KindEnum.CASH);");
        assertContains(code, "return MapperS.of(KindEnum.PHYSICAL);");
        assertAbsent(code, "return KindEnum.CASH;");
    }

    /** a7 — the if-then-else RESULT ladder's enum rungs wrap (the second producer's own seam). */
    @Test
    @EnabledIf("builtinsAvailable")
    void a7_iteResultLadderEnumRungsWrap() throws IOException {
        String code = codeOnly(fn("A7IteResultEnumRungs.java"));
        assertContains(code, "MapperS.of(KindEnum.CASH)");
        assertContains(code, "MapperS.of(KindEnum.PHYSICAL)");
        assertAbsent(code, "= KindEnum.CASH;");
    }

    /**
     * a8 — the type-keyed alias switch ladder (the corpus carrier's shape: cdm 6.20.6
     * {@code GetUnitTypeForUnderlyingAsset.financialUnit}) renders the instanceof RETURN ladder; its
     * enum rungs wrap and its absent-case terminal is the typed empty — the third producer's own seam.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a8_switchLadderEnumRungsAndTypedTerminal() throws IOException {
        String code = codeOnly(fn("A8SwitchAliasEnumRungs.java"));
        assertContains(code, "MapperS<KindEnum> chosen(Asset asset)");
        assertContains(code, "asset instanceof Index");
        assertContains(code, "return MapperS.of(KindEnum.CASH);");
        assertContains(code, "return MapperS.of(KindEnum.PHYSICAL);");
        assertContains(code, "return MapperS.<KindEnum>ofNull();");
        assertAbsent(code, "return KindEnum.CASH;");
    }

    /** a9 — a single rung in a multi seam takes the singleton-list wrap. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a9_singleRungInMultiSeamWrapsSingletonList() throws IOException {
        String code = codeOnly(fn("A9SingleRungInMultiSeam.java"));
        assertContains(code, "MapperC<? extends Sub> one(Leg leg)");
        assertContains(code, "return MapperC.of(Collections.singletonList(");
        assertContains(code, ".get()));");
    }

    // =========================================================================
    // Part B — the corpus carriers (cdm 6.20.6; the D11 ring locks the cell, this suite locks the
    // two files whose seam facts ride into the emitted text)
    // =========================================================================

    /** corpus_c1 — the whole-file byte lock on both carriers. */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_cdm6206CarriersByteIdentical() throws IOException {
        assertNotNull(cdmAOutput, "cdm 6.20.6 generation did not run — corpus unavailable?");
        for (String carrier : List.of(CARRIER_SENTINEL, CARRIER_SINGLETON)) {
            List<String> own = cdmAGenErrors.stream().filter(e -> e.contains(carrier)).toList();
            assertTrue(own.isEmpty(),
                    "the generator reported errors for the locked file " + carrier + ": " + own);
            String generated = cdmAOutput.get(carrier);
            assertNotNull(generated, "not generated in cdm 6.20.6: " + carrier);
            Path goldenPath = GOLDEN_A.resolve(carrier);
            assertTrue(Files.isRegularFile(goldenPath), "golden missing: " + goldenPath);
            assertEquals(normalize(Files.readString(goldenPath)), normalize(generated),
                    "generated cdm 6.20.6 output must byte-match golden (newline-normalized) for "
                    + carrier + " — PR #612 family 5: the seam consumers move onto the producer's"
                    + " recorded facts with no byte change.");
        }
    }

    /**
     * control0 — golden is the oracle: the sentinel carrier's typed empty spells the
     * collision-qualified element; the singleton carrier wraps its single rung. If either ever stops
     * holding, the file stopped being this family's carrier and every claim above is restated, not
     * patched.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control0_goldensCarryTheSeamForms() throws IOException {
        String sentinel = Files.readString(GOLDEN_A.resolve(CARRIER_SENTINEL));
        assertTrue(sentinel.contains("return MapperS.<cdm.observable.asset.BasketConstituent>ofNull();"),
                "golden must carry the collision-qualified element in the typed empty");
        assertTrue(sentinel.contains(
                "MapperS<? extends cdm.observable.asset.BasketConstituent> basketConstituentWithoutLocation("),
                "golden must declare the sentinel-qualified wildcarded single seam");
        String singleton = Files.readString(GOLDEN_A.resolve(CARRIER_SINGLETON));
        assertTrue(singleton.contains("return MapperC.of(Collections.singletonList("),
                "golden must carry the singleton-list wrap of the single rung in the MapperC seam");
    }

    /**
     * control2 — LAW 77 route parity for the two carriers: the seats are shared-generator-side and
     * the IR route inherits them, so both files must render identically under {@code -Pir-on}.
     * Skips (recorded) without the IR provider.
     */
    @Test
    @EnabledIf("cellAAndIrProviderAvailable")
    void corpus_control2_irRouteMatchesLegacyForCarriers() throws IOException {
        assertNotNull(cdmAOutput, "cdm 6.20.6 legacy-route generation did not run");
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CELL_A_ROOT), new ArrayList<>());
        for (String carrier : List.of(CARRIER_SENTINEL, CARRIER_SINGLETON)) {
            assertEquals(cdmAOutput.get(carrier), irOut.get(carrier), "route divergence: " + carrier);
        }
    }

    /**
     * corpus_c2 — the element invariant, corpus-wide: every Mapper seam the producer records for
     * every alias of every function in the cdm 6.20.6 cell (and of the reduced model) carries a
     * bare simple name or the #247 sentinel as its element — never a generic, a wildcard or a
     * space. The typed empties and the optimised value re-seam splice the element verbatim; the
     * retired text readers had declined a {@code ?} in the item, and this is the producer-side
     * witness that replaced that decline.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c2_everyAliasSeamElementIsABareNameOrTheSentinel() throws IOException {
        var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(
                new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CELL_A_ROOT));
        var gm = new GeneratorModel(corpus.workspace(),
                D11CorpusRegressionTest.emissionFilter(new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CELL_A_ROOT)));
        int checked = assertSeamElements(gm, corpus.workspace().files());
        assertTrue(checked > 300, "expected hundreds of Mapper alias seams in cdm 6.20.6, saw " + checked);
        RModel main = AstBuilder.buildFromString(MODEL, "seat612f5.rosetta");
        main.setVersion("0.0.0.test");
        List<RModel> models = new ArrayList<>();
        models.add(main);
        models.addAll(loadBuiltinsOnly());
        RWorkspace workspace = RWorkspace.build(models).workspace();
        GeneratorModel reduced = new GeneratorModel(workspace, m -> "census.seat612f5".equals(m.namespace()));
        assertTrue(assertSeamElements(reduced, workspace.files()) >= 8,
                "the reduced model's Mapper alias seams were not all analysed");
    }

    /** Analyse every function's aliases; assert each Mapper seam's element; return the Mapper seams seen. */
    private static int assertSeamElements(GeneratorModel gm, List<RModel> models) {
        JavaTypeUtil typeUtil = new JavaTypeUtil();
        JavaTypeTranslator tt = new JavaTypeTranslator(typeUtil);
        FunctionAliasHelper helper = new FunctionAliasHelper(gm, tt, typeUtil);
        int mapperSeams = 0;
        for (RModel model : models) {
            if (!gm.shouldGenerate(model)) {
                continue;
            }
            for (var element : model.rootElements()) {
                if (!(element instanceof RFunction func)) {
                    continue;
                }
                for (FunctionTemplateModel.AliasModel alias : helper.analyze(func)) {
                    FunctionTemplateModel.AliasSeam seam = alias.getSeam();
                    if (!seam.isMapper() || seam.element() == null) {
                        continue;
                    }
                    mapperSeams++;
                    String el = seam.element();
                    boolean sentinel = el.indexOf(com.regnosys.rosetta.generator.java.template
                            .ImportCollisionResolver.OPEN) >= 0;
                    String bare = sentinel ? el.substring(1, el.length() - 1) : el;
                    assertTrue(!bare.isEmpty() && bare.indexOf('?') < 0 && bare.indexOf('<') < 0
                            && bare.indexOf(' ') < 0 && Character.isUpperCase(bare.charAt(bare.lastIndexOf('.') + 1)),
                            func.name() + "." + alias.getName() + ": the seam element is not a bare simple name"
                            + " or the #247 sentinel: <" + el + "> (rendered " + alias.getReturnType() + ")");
                }
            }
        }
        return mapperSeams;
    }

    // =========================================================================
    // Fixture harness (the EnumConstantWitnessSeatTest renderer, verbatim)
    // =========================================================================

    private record Render(Map<String, String> output, List<String> errors) {}

    private static Render rendered;

    private static Render render() throws IOException {
        if (rendered != null) {
            return rendered;
        }
        RModel main = AstBuilder.buildFromString(MODEL, "seat612f5.rosetta");
        main.setVersion("0.0.0.test");
        List<RModel> models = new ArrayList<>();
        models.add(main);
        models.addAll(loadBuiltinsOnly());
        RWorkspace workspace = RWorkspace.build(models).workspace();
        GeneratorModel gm = new GeneratorModel(workspace,
                m -> "census.seat612f5".equals(m.namespace()));
        JavaTypeUtil typeUtil = new JavaTypeUtil();
        JavaTypeTranslator tt = new JavaTypeTranslator(typeUtil);
        FunctionGenerator fg = new FunctionGenerator(gm, tt, typeUtil);
        Map<String, String> out = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        fg.generateWithErrors(out)
                .forEach(e -> errors.add(e.getTargetPath() + " — " + e));
        rendered = new Render(out, errors);
        return rendered;
    }

    /** Functions land under {@code .../functions/}. */
    private static String fn(String fileName) throws IOException {
        Render r = render();
        List<String> own = r.errors().stream().filter(e -> e.contains(fileName)).toList();
        assertTrue(own.isEmpty(),
                "the generator reported errors for " + fileName + " (a broken fixture must fail"
                + " loudly, not skip): " + own);
        String out = lookupOrNull(r.output(), "functions/" + fileName);
        assertNotNull(out, "not generated: " + fileName + " (have: " + r.output().keySet() + ")");
        return out;
    }

    private static String lookupOrNull(Map<String, String> output, String suffix) {
        return output.entrySet().stream()
                .filter(e -> e.getKey().endsWith("/" + suffix))
                .map(Map.Entry::getValue)
                .findFirst().orElse(null);
    }

    private static void assertContains(String code, String needle) {
        assertTrue(code.contains(needle), "expected <" + needle + "> in:\n" + code);
    }

    private static void assertAbsent(String code, String needle) {
        assertTrue(!code.contains(needle), "did NOT expect <" + needle + "> in:\n" + code);
    }

    /**
     * Strip line and block comments plus string literals so a javadoc, a label or a
     * {@code "getKind"} literal never counts as code. Every assertion runs on this.
     */
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
            throw new AssertionError("[AliasSeamSignatureSeatTest] builtins parse failures: "
                    + String.join("; ", failures));
        }
        return models;
    }

    // =========================================================================
    // Corpus harness (the sibling seat suites' cell generator, verbatim)
    // =========================================================================

    private static Map<String, String> cdmAOutput;
    private static List<String> cdmAGenErrors;

    @BeforeAll
    static void generateCells() throws IOException {
        if (cellAAvailable()) {
            List<String> errs = new ArrayList<>();
            cdmAOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CELL_A_ROOT), errs);
            cdmAGenErrors = errs;
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

    private static void collect(List<String> sink, List<GenerationException> errors) {
        if (errors != null) {
            errors.forEach(e -> sink.add(e.getTargetPath() + " — " + e));
        }
    }
}
