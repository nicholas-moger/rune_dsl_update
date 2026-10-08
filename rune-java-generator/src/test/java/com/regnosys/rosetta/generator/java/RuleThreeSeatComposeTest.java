package com.regnosys.rosetta.generator.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.java.enums.EnumGenerator;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.object.ChoiceObjectGenerator;
import com.regnosys.rosetta.generator.java.object.ModelObjectGenerator;
import com.regnosys.rosetta.generator.java.reports.ReportGenerator;
import com.regnosys.rosetta.generator.java.reports.RuleGenerator;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import static com.regnosys.rosetta.testutil.GenerationErrors.assertNoGenerationErrors;

/**
 * PR #337 anchor — the three-seat compose (5 flips, drr POJO Rule).
 *
 * <p><b>Facet 1 — {@code listLiteralNavMetaDeref}</b> ({@code DTCC_Leg1/2FloatingRateIndexRule}):
 * a list literal ALL of whose elements navigate (through {@code first}/{@code only-element}
 * list-ops) to the SAME meta-annotated leaf witnesses the WRAPPER
 * ({@code MapperC.<FieldWithMetaString>of(...)} — {@code LiteralHandler.listItemJavaType}'s NAV
 * sibling of the #327 B3 call-truth lift), and the ladder rung whose arm is that literal's bare
 * {@code .get()} collapse hoists the wrapper + returns the null-guarded value reconstruct
 * ({@code CollectionHandler.compileLadderConditionalBlock} — the terminal-output convertNullSafe
 * analogue of #314 to-enum / #317 feature-nav). The {@code to-string} sibling branch declines
 * (conversion elements, not meta-nav leafs).
 *
 * <p><b>Facet 2 — {@code extractDefaultAssociativity}</b> ({@code OptionPremiumCurrencyRule}
 * common, PARSER): upstream's stratified grammar takes AT MOST ONE {@code default} inside an
 * implicit inline-function body, so {@code extract A default B default C} re-associates to
 * {@code Default(Extract(body=Default(A,B)), C)} — the second default's implicit input is the
 * RULE input ({@code .getOrDefault(notionalCurrencyLeg1Rule.evaluate(input))} OUTSIDE the
 * lambda). {@code AstBuilder.reassociateExtractTrailingDefaults}.
 *
 * <p><b>Facet 3 — {@code flattenedMetaElemDeref}</b> ({@code DeliveryPointOrZoneRule} esma+fca):
 * a bare {@code then flatten} last-body over a thenArg DECLARED
 * {@code MapperListOfLists<FieldWithMetaString>} derefs elementwise before the List collapse
 * ({@code .<String>map("Type coercion", fieldWithMetaString -> fieldWithMetaString.getValue())
 * .getMulti()}) — recovered LOCALLY from the renderer's own declaration; the shared
 * {@code recoverExprMetaWrapper} walker stays FROZEN (the #272 FLATTEN exclusion).
 *
 * <p>Four of the five pre-fix forms were non-compiling (witness type lie / {@code List<Wrapper>}
 * into {@code List<String>}); the fifth — OptionPremiumCurrency — COMPILED (the mis-associated
 * default bound {@code item.get()}, the same input type as golden's {@code input}: semantically
 * wrong, type-correct — COMPILES_DIVERGENT per the compile-gate verdict). No green golden
 * carries any pre-fix form (the chained-default shape has exactly one, divergent, carrier).
 * Whole-file byte comparisons run through the REAL D11 rule-kind generation path and revert
 * RED without the facets.
 */
class RuleThreeSeatComposeTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    // Facet 1 flip carriers — the `[<meta-nav> first, …] only-element` ladder rung.
    private static final String DTCC_LEG1 =
            "drr/regulation/common/dtcc/reports/DTCC_Leg1FloatingRateIndexRule.java";
    private static final String DTCC_LEG2 =
            "drr/regulation/common/dtcc/reports/DTCC_Leg2FloatingRateIndexRule.java";
    // Facet 2 flip carrier — the sole chained-default expression in the frozen corpus.
    private static final String OPTION_PREMIUM_CCY =
            "drr/regulation/common/trade/price/reports/OptionPremiumCurrencyRule.java";
    // Facet 3 flip carriers — bare `then flatten` over MapperListOfLists<FieldWithMetaString>.
    private static final String DPZ_ESMA =
            "drr/regulation/esma/emir/refit/trade/reports/DeliveryPointOrZoneRule.java";
    private static final String DPZ_FCA =
            "drr/regulation/fca/ukemir/refit/trade/reports/DeliveryPointOrZoneRule.java";
    // Facet 1 partial-heal lock — the IRSwap arm's literal witness lifts (golden-matching) but
    // the file stays divergent on its other, multi-mechanism lines.
    private static final String NAME_OF_UNDERLYING_INDEX =
            "drr/regulation/common/trade/underlier/reports/NameOfTheUnderlyingIndexRule.java";
    // Facet 3 decline lock (DTCC_OtherPaymentPayerIDTypeRule, the constant below) — that rule
    // derefs INSIDE its mapItemToList lambda, so the flattened element is already bare String;
    // the declared-element gate must decline (no inserted deref step).
    // FLIPPED at PR #370 (listOfListsCardinality): the file is now byte-identical — the
    // assertions hold on the GREEN bytes (golden itself carries `.flattenList().getMulti()`
    // and zero `flattenList().<`), so the lock doubles as a green regression pin.
    private static final String DTCC_OTHER_PAYMENT_PAYER =
            "drr/regulation/common/dtcc/reports/DTCC_OtherPaymentPayerIDTypeRule.java";

    private static Map<String, String> drrRuleOutput;

    static boolean cellsAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (cellsAvailable()) {
            drrRuleOutput = generateRuleKinds(
                    new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT));
        }
    }

    /** The REAL D11 rule-kind generation path (the drr POJO Rule carriers). */
    private static Map<String, String> generateRuleKinds(D11CorpusRegressionTest.CellSpec cell)
            throws IOException {
        var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(),
                D11CorpusRegressionTest.emissionFilter(cell),
                D11CorpusRegressionTest.readDoNotPrune(cell));
        var typeUtil = new JavaTypeUtil();
        var typeTranslator = new JavaTypeTranslator(typeUtil);
        var enumGen = new EnumGenerator(gm);
        var pojoGen = new ModelObjectGenerator(gm, typeTranslator, typeUtil);
        var choiceGen = new ChoiceObjectGenerator(gm, typeTranslator, typeUtil, pojoGen);
        var funcGen = new FunctionGenerator(gm, typeTranslator, typeUtil);
        var ruleGen = new RuleGenerator(gm, typeTranslator, funcGen);
        var reportGen = new ReportGenerator(gm, typeTranslator, funcGen);
        Map<String, String> output = new LinkedHashMap<>();
        for (RModel model : corpus.workspace().files()) {
            if (gm.shouldGenerate(model)) {
                String version = gm.version(model);
                assertNoGenerationErrors(enumGen.generateClasses(model, version, output));
                assertNoGenerationErrors(pojoGen.generateClasses(model, version, output));
                assertNoGenerationErrors(choiceGen.generateClasses(model, version, output));
                assertNoGenerationErrors(ruleGen.generateClasses(model, version, output));
                assertNoGenerationErrors(reportGen.generateClasses(model, version, output));
            }
        }
        return output;
    }

    // ==== flip locks (revert-RED) ====

    @Test
    @EnabledIf("cellsAvailable")
    void dtccLeg1FloatingRateIndex_listLiteralNavMetaDeref_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(DTCC_LEG1);
    }

    @Test
    @EnabledIf("cellsAvailable")
    void dtccLeg2FloatingRateIndex_listLiteralNavMetaDeref_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(DTCC_LEG2);
    }

    @Test
    @EnabledIf("cellsAvailable")
    void optionPremiumCurrency_extractDefaultAssociativity_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(OPTION_PREMIUM_CCY);
    }

    @Test
    @EnabledIf("cellsAvailable")
    void deliveryPointOrZoneEsma_flattenedMetaElemDeref_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(DPZ_ESMA);
    }

    @Test
    @EnabledIf("cellsAvailable")
    void deliveryPointOrZoneFca_flattenedMetaElemDeref_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(DPZ_FCA);
    }

    // ==== partial-heal lock (facet 1) — GRADUATED at PR #387 ====

    /**
     * NameOfTheUnderlyingIndexRule's IRSwap arm carries the SAME {@code [<meta-nav> first, …]
     * only-element} literal — under a {@code to-string} conversion. Facet 1's witness lift fires
     * (the literal's elements ARE meta-navs; gen carries golden's
     * {@code MapperC.<FieldWithMetaString>of} at that construct).
     *
     * <p>GRADUATED at PR #387 (the anticipated-seat law — the pre-#387 javadoc named the exact
     * residual: "cardinality + in-lambda derefs are NOT this PR's facets"): the
     * toStringLadderArmBareEvidence / drainConsumerChainTypeStamp / toStringCollapsedMetaRetype
     * trio landed the in-lambda join-bare derefs and the whole file is byte-identical now —
     * the partial-heal lock became a whole-file byte lock, and the witness assertion runs
     * against GREEN bytes (golden's in-rung hoist decl carries the same
     * {@code MapperC.<FieldWithMetaString>of} literal).
     */
    @Test
    @EnabledIf("cellsAvailable")
    void nameOfTheUnderlyingIndex_converted_byteMatchesGolden() throws IOException {
        String gen = generated(NAME_OF_UNDERLYING_INDEX);
        assertTrue(gen.contains("MapperC.<FieldWithMetaString>of"),
                "the facet-1 witness lift must fire on the IRSwap arm's meta-nav literal");
        assertByteMatchesGolden(NAME_OF_UNDERLYING_INDEX);
    }

    // ==== decline lock (facet 3) ====

    /**
     * DTCC_OtherPaymentPayerIDTypeRule flattens a {@code MapperListOfLists<String>} — its
     * mapItemToList lambda ALREADY derefs the meta wrapper inside, so the flattened element is
     * bare and golden consumes {@code .flattenList().getMulti()} with NO elementwise deref.
     * Facet 3's declared-element gate (the thenArg element must be an {@code RJavaWithMetaValue})
     * must decline: gen keeps the bare collapse and never gains a {@code flattenList().<}
     * deref step (golden's own count of that token is 0 — the witness-uniqueness law).
     * FLIPPED at PR #370 (listOfListsCardinality): the whole file is byte-identical now,
     * so both assertions run against GREEN bytes — the decline lock became a green pin.
     */
    @Test
    @EnabledIf("cellsAvailable")
    void dtccOtherPaymentPayer_bareFlattenElement_derefDeclines() throws IOException {
        String gen = generated(DTCC_OTHER_PAYMENT_PAYER);
        assertTrue(gen.contains(".flattenList().getMulti()"),
                "the bare-element flatten collapse must keep its uncoerced form");
        assertFalse(gen.contains("flattenList().<"),
                "facet 3 must NOT insert an elementwise deref on a bare-element flatten");
    }

    // ==== helpers ====

    private String generated(String path) {
        assertNotNull(drrRuleOutput, "cell output not generated");
        String gen = drrRuleOutput.get(path);
        assertNotNull(gen, "missing generated output: " + path);
        return gen;
    }

    private String golden(String path) throws IOException {
        Path goldenPath = DRR_GOLDEN_DIR.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "missing golden: " + goldenPath);
        return Files.readString(goldenPath);
    }

    private void assertByteMatchesGolden(String path) throws IOException {
        assertEquals(normalize(golden(path)), normalize(generated(path)),
                path + " must byte-match the frozen 9.83.0 golden");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
