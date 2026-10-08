package com.regnosys.rosetta.generator.java;

import com.regnosys.rosetta.utils.DeepFeatureCallUtil;
import com.regnosys.rosetta.generator.java.enums.EnumGenerator;
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
 * Anchor for PR #322's report-OPERATIONS synthesis (GENERATOR-only, parser UNTOUCHED) — the fork
 * analogue of upstream {@code RObjectFactory.generateOperations}, which flips ALL 23 waivered drr
 * {@code *ReportFunction} files (pure-addition diffs: the fork emitted an EMPTY assignOutput
 * where golden carries the {@code @Inject <Rule>Rule} dependency fields + the per-attribute
 * rule-invocation setter chain).
 *
 * <p><b>Synthesis</b> ({@code ReportGenerator.synthesizeReportOperations}): the PR #321 faithful
 * port of upstream {@code RuleReferenceService.traverse} (PROMOTED to the shared
 * {@code RuleReferenceTraversal} with upstream's path tracking) folds every non-explicitly-empty
 * rule association into {@code ROperation(SET, output, assignPath, <rule>(input))} in traversal
 * order — plain leaves render {@code output.getOrCreateX()….setY(yRule.evaluate(input));} through
 * the EXISTING operation pipeline. <b>Coercion arms</b>
 * ({@code FunctionExpressionRenderer.renderReportRuleSetOrNull}, upstream's operation-value
 * coercion): a Void-output rule (rune NOTHING) coerces to the bare {@code null} literal; a
 * MULTI-output rule into a SINGLE leaf collapses {@code MapperC.of(<call>).get()}; a
 * BigDecimal-output rule into an Integer leaf hoists
 * {@code final BigDecimal bigDecimalN = <call>;} (the StatementHoistSession {@code bigDecimal}
 * group law) + narrows {@code (bigDecimalN == null ? null : bigDecimalN.intValueExact())}.
 * <b>Setter naming</b> ({@code leafSetterCall} report-seat branch): the upstream
 * {@code RJavaPojoInterface} incompatible-property law — a SPECIALIZED override (type- or
 * requiredness-changing, equal erasure) renames {@code setXOverriddenAs<ItemType>}; an
 * annotations-only override keeps the PLAIN setter. <b>Dependency fields</b>
 * ({@code FunctionDependencyCollector}): sorted by the class SIMPLE name (case-sensitive — the
 * lowercase {@code barrierRule} class sorts LAST) and {@code _}-escaped when the lowerCamel field
 * would equal the class simple name ({@code @Inject protected barrierRule _barrierRule;}).
 *
 * <p>Anchors are WHOLE-FILE byte comparisons of the generated drr output against the frozen
 * goldens (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}.
 */
class ReportOperationsTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    // Plain-shape flip carrier (F1): top-level `.setX(xRule.evaluate(input));` setters only.
    private static final String MAS_VALUATION =
            "drr/regulation/mas/rewrite/valuation/reports/MASValuationReportFunction.java";
    // Pathed flip carrier (F1): `.getOrCreate…()` intermediate steps on the margin family.
    private static final String ESMA_MARGIN =
            "drr/regulation/esma/emir/refit/margin/reports/ESMAEMIRMarginReportFunction.java";
    // BigDecimal-narrow flip carrier (F2): 4 hoists numbered bigDecimal0..3 + the
    // requiredness-changing override rename (setTechnicalRecordIdOverriddenAsString).
    private static final String JFSA_TRADE =
            "drr/regulation/jfsa/rewrite/trade/reports/JFSATradeReportFunction.java";
    // Void + MapperC + narrow + same-type-override PLAIN flip carrier (F2/F3): DTCC
    // `.setComment1(null);` (Void rule, dep still injected) + `.setTraderLocation(
    // MapperC.of(…).get())` (multi→single) + 6 narrows + the annotations-only overrides
    // (`override comment1 string (0..1)`) keeping their PLAIN setters.
    private static final String CFTC_PART43 =
            "drr/regulation/cftc/rewrite/trade/reports/CFTCPart43ReportFunction.java";
    // Dependency-field flip carrier (F3): the lowercase-named `reporting rule barrier` →
    // class `barrierRule` sorts LAST (type-simple-name sort) with the `_barrierRule`
    // field escape + the `.setBarrier(_barrierRule.evaluate(input));` receiver.
    private static final String HKMA_TRADE =
            "drr/regulation/hkma/rewrite/trade/reports/HKMATradeReportFunction.java";
    // Override-rename flip carrier (F2): setXOverriddenAs<Type> battery (Boolean /
    // ZonedDateTime / CommonContractType / CommonAssetClass / String).
    private static final String MAS_TRADE =
            "drr/regulation/mas/rewrite/trade/reports/MASTradeReportFunction.java";
    // Green-safety pin (RULE path): a GREEN drr Rule class carrying an injected rule
    // dependency (the #263 injectedRuleRef carrier) — the dependency-sort/escape/receiver
    // changes must be byte-neutral on the rule path.
    private static final String VALUATION_METHOD_RULE_GREEN =
            "drr/regulation/asic/rewrite/valuation/reports/ValuationMethodRule.java";
    // Green-safety pin (LABEL path): a GREEN LabelProvider — the RuleReferenceTraversal
    // PROMOTION (LabelProviderGenerator now folds over the shared traversal) must keep
    // every label byte-identical.
    private static final String CFTC_MARGIN_LABEL_GREEN =
            "drr/regulation/cftc/rewrite/margin/labels/CFTCMarginLabelProvider.java";

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
        var enumGen = new EnumGenerator(gm);
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
                assertNoGenerationErrors(enumGen.generateClasses(model, version, output));
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

    // ==== Flip locks (revert-RED): the carriers now byte-match golden. ====

    @Test
    @EnabledIf("drrCellAvailable")
    void masValuationReportFunction_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(MAS_VALUATION);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void esmaEmirMarginReportFunction_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(ESMA_MARGIN);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void jfsaTradeReportFunction_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(JFSA_TRADE);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void cftcPart43ReportFunction_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(CFTC_PART43);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void hkmaTradeReportFunction_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(HKMA_TRADE);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void masTradeReportFunction_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(MAS_TRADE);
    }

    // ==== Positive-content locks (revert-RED). ====

    /**
     * The BigDecimal→Integer narrow: the session-numbered hoist + the null-safe
     * {@code intValueExact()} ternary, and the requiredness-changing override
     * ({@code override technicalRecordId string (1..1)} over the common {@code (0..1)})
     * taking the RENAMED setter.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void jfsaTrade_bigDecimalNarrowHoistAndOverrideRename() {
        String gen = gen(JFSA_TRADE);
        assertTrue(gen.contains(
                "final BigDecimal bigDecimal0 = floatingRateReferencePeriodMultiplierLeg1Rule.evaluate(input);"),
                "Expected the first session-numbered BigDecimal hoist");
        assertTrue(gen.contains(
                ".setFloatingRateReferencePeriodMultiplier((bigDecimal0 == null ? null : bigDecimal0.intValueExact()));"),
                "Expected the null-safe intValueExact narrow consuming the hoist");
        assertTrue(gen.contains(".setTechnicalRecordIdOverriddenAsString(technicalRecordIdRule.evaluate(input));"),
                "Expected the requiredness-specialized override's renamed setter");
    }

    /**
     * The Void-rule null coercion (dep still injected — upstream collects dependencies
     * from the operation AST regardless of the coerced render), the MULTI→SINGLE
     * {@code MapperC.of(…).get()} collapse, and the annotations-only override
     * ({@code override comment1 string (0..1)}, same type + same requiredness) keeping
     * the PLAIN setter (the function-path same-type-override arm must NOT fire here).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void cftcPart43_voidNullMapperCAndPlainOverrides() {
        String gen = gen(CFTC_PART43);
        assertTrue(gen.contains(".setComment1(null);"),
                "Expected the Void-output rule coerced to the bare null literal");
        assertTrue(gen.contains("@Inject protected DTCC_Comment1Rule dTCC_Comment1Rule;"),
                "Expected the Void rule's @Inject dependency despite the null render");
        assertTrue(gen.contains(".setTraderLocation(MapperC.of(traderLocationRule.evaluate(input)).get());"),
                "Expected the MULTI-rule-into-SINGLE-leaf MapperC collapse");
        assertTrue(!gen.contains("OverriddenAsSubmittingPartyIDType"),
                "An annotations-only same-type override must keep the PLAIN setter");
        assertTrue(!gen.contains("setComment1OverriddenAsComment1"),
                "An annotations-only same-type override must keep the PLAIN setter");
    }

    /**
     * The lowercase-named rule class ({@code reporting rule barrier} → class
     * {@code barrierRule}): the {@code _}-escaped field, the escaped receiver, and the
     * type-simple-name dependency sort filing it LAST (after every uppercase-initial
     * class).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void hkmaTrade_lowercaseRuleClassEscapeAndSort() {
        String gen = gen(HKMA_TRADE);
        assertTrue(gen.contains("@Inject protected barrierRule _barrierRule;"),
                "Expected the _-escaped dependency field for the lowercase rule class");
        assertTrue(gen.contains(".setBarrier(_barrierRule.evaluate(input));"),
                "Expected the receiver to use the escaped field name");
        int barrierIdx = gen.indexOf("@Inject protected barrierRule _barrierRule;");
        int variationIdx = gen.indexOf("@Inject protected VariationMarginCollateralPortfolioCodeRule");
        assertTrue(variationIdx >= 0 && barrierIdx > variationIdx,
                "The lowercase-initial class must sort AFTER every uppercase-initial class");
    }

    // ==== Green-safety locks. ====

    /**
     * RULE-path neutrality: a GREEN drr Rule class with an injected rule dependency
     * (the #263 injectedRuleRef carrier) stays byte-identical under the dependency
     * sort/escape/receiver changes (all its dependency classes are uppercase-initial,
     * where the type-simple-name sort equals the previous field-name sort and the
     * escape never fires).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void valuationMethodRule_green_staysByteIdentical() throws IOException {
        assertByteMatchesGolden(VALUATION_METHOD_RULE_GREEN);
    }

    /**
     * LABEL-path neutrality: a GREEN LabelProvider stays byte-identical under the
     * RuleReferenceTraversal promotion (the as-label fold over the shared traversal
     * produces exactly the PR #321 in-place fold's map).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void cftcMarginLabelProvider_green_staysByteIdentical() throws IOException {
        assertByteMatchesGolden(CFTC_MARGIN_LABEL_GREEN);
    }

    // ==== Helpers. ====

    private static String gen(String path) {
        assertNotNull(drrOutput, "drr generation did not run");
        String gen = drrOutput.get(path);
        assertNotNull(gen, "missing generated file: " + path);
        return gen;
    }

    private void assertByteMatchesGolden(String path) throws IOException {
        String gen = gen(path);
        Path goldenPath = DRR_GOLDEN_DIR.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "missing golden: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(gen), "byte mismatch vs golden: " + path);
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
