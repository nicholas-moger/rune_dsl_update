package com.regnosys.rosetta.generator.java;

import com.regnosys.rosetta.utils.DeepFeatureCallUtil;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.function.LabelProviderGenerator;
import com.regnosys.rosetta.generator.java.function.LabelProviderGeneratorUtil;
import com.regnosys.rosetta.generator.java.object.ChoiceObjectGenerator;
import com.regnosys.rosetta.generator.java.object.ModelObjectGenerator;
import com.regnosys.rosetta.generator.java.reports.ReportGenerator;
import com.regnosys.rosetta.generator.java.reports.RuleGenerator;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.ast.model.RModel;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static com.regnosys.rosetta.testutil.GenerationErrors.assertNoGenerationErrors;

/**
 * PR #307 — facet reportOutputFunctionChainCardinality: the report-output cardinality FOUNDATION
 * (the #289/#291/#293 lineage) extended to a FUNCTION-headed disguised chain. ONE green-safe,
 * thenAware-gated (FUNCTION-byte-neutral) parser-side mechanism; 5 drr POJO Rule byte flips. 1 source
 * file ({@code CardinalityComputer}).
 *
 * <p><b>The bug.</b> A reporting rule whose whole-output body is {@code … then extract [<fn> -> <feature>]}
 * where {@code <fn>} is a function with a MULTI output is multi-valued: the function is invoked PER
 * ITEM and produces a list per item, and the per-element leaf navigation ({@code -> source}) preserves
 * the list cardinality. The chain {@code GetBasketConstituentsProductIdentifier -> source}
 * ({@code trade (1..1)} → {@code productIdentifiers (0..*)}, then {@code -> source}) parses as an
 * {@code REnumValueRef} whose {@code resolvedSymbol} is the RFunction, but
 * {@code CardinalityComputer.disguisedChainCardinality} read only {@code resolvedAttributeChain} /
 * {@code resolvedInputFeature} (which resolve an ATTRIBUTE head, not a function head) → SINGLE. So the
 * iosco cde-version BasketConstituent/Identifier reporting-rule family read SINGLE
 * ({@code ReportFunction<I, T>} + {@code mapSingleToItem(...).get()}) where golden carries
 * {@code ReportFunction<I, List<T>>} + {@code mapSingleToList(...).getMulti()}.
 *
 * <p><b>The fix.</b> {@code disguisedChainCardinality} recognizes a function-headed chain
 * ({@code resolvedSymbol instanceof RFunction}) whose function OUTPUT is multi → MULTI. The fix
 * cascades through the existing rule-aware machinery without a generator change: the whole-rule
 * cardinality ({@code getRuleBodyCardinality}) drives the {@code List<…>} signature + the
 * {@code .getMulti()} terminal ({@code isMultiToMultiSet}), and {@code CollectionHandler.isBodyMulti}
 * already consults {@code computeRuleBody(extract.body.body)} to select {@code mapSingleToList}.
 * thenAware-gated → the global {@code compute} (function tail) is byte-frozen (#232).
 *
 * <p><b>Green-safe by construction.</b> Gated on the function OUTPUT being multi: a function that
 * CONSUMES the list and returns a SINGLE ({@code GetProductIdentifierFilteringISIN}:
 * {@code productIdentifiers (0..*)} → {@code productIdentifier (0..1)}) keeps its
 * {@code <fn> -> feature} chain SINGLE — the distinct #291 collapse case
 * ({@code isElementWiseThenBody}'s RFunction exclusion). A {@code <multi-output-fn> -> feature} chain
 * IS a list (golden always renders it multi), so moving the fork to MULTI only moves toward golden — a
 * waivered NON-COMPILING/divergent file flips, green→red is structurally impossible. byte-oracle /
 * stash-baseline measured exactly 5 flips (drr POJO 377 → 372; 17 now-matching = 5 mine + 12 stale,
 * clean-main re-dump = exactly the 12 stale, {@code comm -23} = exactly the 5 carriers); regscan 0
 * within-waiver regressions / 0 new / 5 flipped-out / 3 toward-golden (UnderlyingIdentification +
 * IdentifierOfBasketConstituents esma/fca compounding) + 3 neutral (the Field↔Reference wobble); all
 * FUNCTION cells (cdm5 79 / cdm6 232 / drr 206) + cdm/iso/fpml POJO byte-IDENTICAL.
 *
 * <p>Anchors are WHOLE-FILE byte comparisons of the generated drr output against the frozen goldens
 * (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}. REVERT-VERIFIED RED — the 5 flip locks + the
 * positive-content lock fail on clean source; the 2 green-safety locks pass either way (the
 * function-collapse rule's chain stays SINGLE because its function output is single — my
 * {@code fn.output() == MULTI} gate does not fire on it).
 */
class RuleReportOutputFunctionChainCardinalityTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static Map<String, String> drrOutput;

    static boolean drrCellAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (drrCellAvailable()) {
            drrOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT));
        }
    }

    private static Map<String, String> generateCell(D11CorpusRegressionTest.CellSpec cell)
            throws IOException {
        var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(),
                D11CorpusRegressionTest.emissionFilter(cell));
        var typeUtil = new JavaTypeUtil();
        var typeTranslator = new JavaTypeTranslator(typeUtil);
        var pojoGen = new ModelObjectGenerator(gm, typeTranslator, typeUtil);
        var choiceGen = new ChoiceObjectGenerator(gm, typeTranslator, typeUtil, pojoGen);
        var funcGen = new FunctionGenerator(gm, typeTranslator, typeUtil);
        var ruleGen = new RuleGenerator(gm, typeTranslator, funcGen);
        var reportGen = new ReportGenerator(gm, typeTranslator, funcGen);
        var labelProviderGen = new LabelProviderGenerator(
                gm, typeTranslator, new DeepFeatureCallUtil(gm::getType),
                new LabelProviderGeneratorUtil());
        Map<String, String> output = new LinkedHashMap<>();
        for (RModel model : corpus.workspace().files()) {
            if (gm.shouldGenerate(model)) {
                String version = gm.version(model);
                assertNoGenerationErrors(pojoGen.generateClasses(model, version, output));
                assertNoGenerationErrors(choiceGen.generateClasses(model, version, output));
                assertNoGenerationErrors(ruleGen.generateClasses(model, version, output));
                assertNoGenerationErrors(reportGen.generateClasses(model, version, output));
                assertNoGenerationErrors(labelProviderGen.generateClasses(model, version, output));
            }
        }
        funcGen.generate(output);
        return output;
    }

    // ==== Flip locks (revert-RED): the 5 iosco cde-version BasketConstituent/Identifier carriers
    //      now read MULTI (ReportFunction<I, List<T>> + mapSingleToList(...).getMulti()). ====

    /**
     * Flip — iosco cde version1 SourceOfTheIdentifierOfTheBasketConstituents: the BASE rule
     * {@code extract TradeForEvent then extract [GetBasketConstituentsProductIdentifier -> source]}.
     * {@code GetBasketConstituentsProductIdentifier} ({@code trade (1..1)} →
     * {@code productIdentifiers (0..*)}) is multi-output, so the {@code -> source} chain is MULTI and
     * the rule output becomes {@code ReportFunction<I, List<ProductIdTypeEnum>>} +
     * {@code .mapSingleToList(...).getMulti()}.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void sourceOfTheIdentifier_v1_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/standards/iosco/cde/version1/basket/reports/SourceOfTheIdentifierOfTheBasketConstituentsRule.java");
    }

    /**
     * Flip — iosco cde version2 SourceOfTheIdentifierOfTheBasketConstituents: the version2 delegation
     * of the version1 base (its cardinality recurses into the base body via the #273 {@code RRule}
     * case), same MULTI function-chain cascade.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void sourceOfTheIdentifier_v2_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/standards/iosco/cde/version2/basket/reports/SourceOfTheIdentifierOfTheBasketConstituentsRule.java");
    }

    /**
     * Flip — iosco cde version2 IdentifierOfBasketConstituents:
     * {@code extract TradeForEvent then extract [GetBasketConstituentsProductIdentifier -> identifier]}
     * — the multi function-chain makes the whole output multi (the {@code -> identifier} meta leaf's
     * Type-coercion deref renders inside the {@code mapSingleToList} lambda, but the cardinality is the
     * facet's contribution).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void identifierOfBasketConstituents_v2_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/standards/iosco/cde/version2/basket/reports/IdentifierOfBasketConstituentsRule.java");
    }

    /**
     * Flip — iosco cde version3 BasketConstituentIdentifier: the version3 basket rule whose whole
     * output navigates the multi {@code GetBasketConstituentsProductIdentifier} function chain.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void basketConstituentIdentifier_v3_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/standards/iosco/cde/version3/basket/reports/BasketConstituentIdentifierRule.java");
    }

    /**
     * Flip — iosco cde version3 BasketConstituentIdentifierSource: the version3 basket rule
     * ({@code -> source}). NB this is a DIFFERENT rule from the regulation-trade
     * BasketConstituentIdentifierSource (the #291 green-safety collapse case below) — the iosco
     * version3 body expands via the multi {@code GetBasketConstituents…} function, the regulation
     * variant collapses via the single-output {@code GetProductIdentifierFilteringISIN}.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void basketConstituentIdentifierSource_v3_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/standards/iosco/cde/version3/basket/reports/BasketConstituentIdentifierSourceRule.java");
    }

    // ==== Positive-content lock (revert-RED): the rendered MULTI form, not just byte-match. ====

    /**
     * Positive-content — SourceOfTheIdentifier (v1) renders the MULTI form: the
     * {@code ReportFunction<ReportableEvent, List<ProductIdTypeEnum>>} signature, the
     * {@code .mapSingleToList(} extract method, and the {@code .getMulti()} terminal. On clean source
     * the fork renders the single form ({@code ReportFunction<…, ProductIdTypeEnum>} +
     * {@code .mapSingleToItem(…).get()}), so this lock fails RED on revert.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void sourceOfTheIdentifier_v1_rendersMultiForm() {
        assertNotNull(drrOutput, "drr generation did not run — corpus unavailable?");
        String gen = drrOutput.get(
                "drr/standards/iosco/cde/version1/basket/reports/SourceOfTheIdentifierOfTheBasketConstituentsRule.java");
        assertNotNull(gen, "Class not generated");
        assertTrue(gen.contains("ReportFunction<ReportableEvent, List<ProductIdTypeEnum>>"),
                "Expected the MULTI output signature ReportFunction<…, List<ProductIdTypeEnum>>");
        assertTrue(gen.contains(".mapSingleToList("),
                "Expected the mapSingleToList extract method (multi body)");
        assertTrue(gen.contains(".getMulti()"),
                "Expected the .getMulti() whole-output terminal");
    }

    // ==== Green-safety decline locks: the function-COLLAPSE case (single output) stays SINGLE. ====

    /**
     * Green-safety lock — csa BasketConstituentIdentifierSourceRule STAYS byte-matching golden. Its
     * common-body conditional ladder collapses via {@code then GetProductIdentifierFilteringISIN ->
     * source}, where {@code GetProductIdentifierFilteringISIN} ({@code productIdentifiers (0..*)} →
     * {@code productIdentifier (0..1)}) CONSUMES the list and returns a SINGLE. My
     * {@code fn.output() == MULTI} gate does NOT fire on it (output is single), so the chain stays
     * SINGLE — matching golden. This is the load-bearing green-safety guard: a function-collapse head
     * (single output) must NOT be widened to multi. (Also locked by
     * {@code RuleDtccProductGradeCardinalityTest.basketConstituentIdentifierSource_csa_staysGreen},
     * which guards the coupled {@code isElementWiseThenBody} fix.)
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void basketConstituentIdentifierSource_csa_collapse_staysGreen() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/csa/rewrite/trade/reports/BasketConstituentIdentifierSourceRule.java");
    }

    /**
     * Green-safety lock — hkma BasketConstituentIdentifierSourceRule STAYS byte-matching golden (the
     * hkma-regime sibling of the csa collapse green-safety guard, same single-output function-collapse
     * shape).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void basketConstituentIdentifierSource_hkma_collapse_staysGreen() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/hkma/rewrite/trade/reports/BasketConstituentIdentifierSourceRule.java");
    }

    private static void assertByteMatchesGolden(String path) throws IOException {
        assertNotNull(drrOutput, "drr generation did not run — corpus unavailable?");
        String generated = drrOutput.get(path);
        assertNotNull(generated, "Class not generated: " + path
                + " (emission failed or the path differs)");
        Path goldenPath = DRR_GOLDEN_DIR.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated drr output must byte-match the golden (newline-normalized) for "
                + path + " (PR #307 reportOutputFunctionChainCardinality).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
