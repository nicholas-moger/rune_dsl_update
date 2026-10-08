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
 * Anchor for facet {@code collapsedMetaDeref} (PR #317): a {@code <meta feature> only-element ->
 * <bare feature>} navigation — the FEATURE-NAV analogue of #314's
 * {@code ConversionHandler.getCollapsedMetaDeref} (#314 did the {@code to-enum}/{@code checkedMap}
 * variant with a FIELD_WITH_META-{@code string} receiver). The {@code only-element} collapses a MULTI
 * meta chain ({@code quantity}, a {@code [metadata reference]} feature of
 * {@code product.template.BasketConstituent} — {@code ReferenceWithMetaNonNegativeQuantitySchedule}) to
 * a bare wrapper ({@code .get()}), which {@code NavigationHandler.handle(RFeatureCall)} re-wrapped
 * {@code MapperS.of(<X>.get())} and then navigated a getter of the BARE value type
 * ({@code .<UnitType>map("getUnit", nonNegativeQuantitySchedule -> nonNegativeQuantitySchedule.getUnit())})
 * — a value getter invoked on the wrapper-typed lambda param → non-compiling, so no green file carries
 * the pre-#317 form (every carrier is an already-waivered NON_COMPILING mismatch).
 *
 * <p>Golden DEREFERENCES the wrapper first (upstream TypeCoercionService convertNullSafe):
 * <pre>
 *   final ReferenceWithMetaNonNegativeQuantitySchedule referenceWithMetaNonNegativeQuantitySchedule = &lt;X&gt;.get();
 *   … (referenceWithMetaNonNegativeQuantitySchedule == null ? MapperS.&lt;NonNegativeQuantitySchedule&gt;ofNull()
 *        : MapperS.of(referenceWithMetaNonNegativeQuantitySchedule.getValue())).&lt;…&gt;map("get…", …)
 * </pre>
 * The hoist drains on the LAMBDA_CHANNEL ({@code BasketConstituentUnitOfMeasure}, inside a
 * {@code mapSingleToItem} lambda → {@code CollectionHandler.compileLambda} block-converts it,
 * {@code registerPendingLambdaHoist} — the #314 pattern) or the STATEMENT_SINK
 * ({@code BasketConstituentNumberOfUnits}, a whole-output rule body →
 * {@code FunctionExpressionRenderer.prependStatementHoists} lifts it, {@code registerStatementHoist} —
 * the #237 pattern). RULE-scoped ({@code findEnclosingRule}) → FUNCTION-byte-neutral (#232; cdm5 79 /
 * cdm6 232 / drr 206 FUNCTION mismatch UNCHANGED, cdm/iso/fpml POJO byte-IDENTICAL). The navigated
 * feature must be an attribute of the bare value {@code RDataType} ({@code resolveReceiverDataType}, the
 * type-transparent {@code only-element} collapse) so a metafield nav off the wrapper ({@code -> reference}
 * / {@code -> scheme}) keeps the plain re-wrap.
 *
 * <p>Anchors are WHOLE-FILE byte comparisons of the generated drr output against the frozen goldens
 * (newline-normalized), generated through the REAL {@link D11CorpusRegressionTest#loadCellCorpusCached}.
 * REVERT-VERIFIED RED 4/6 — the 2 flip locks + the 2 positive-content locks fail on clean source; the 2
 * non-meta-only-element decline locks ({@code FixingDateLeg1/2}) pass either way (their {@code getOptionPayout}
 * / {@code getForwardPayout} collapse is NON-meta, so the arm declines and the plain re-wrap stays).
 */
class RuleCollapsedMetaDerefTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    // Flip carriers (the STATEMENT_SINK whole-output + the LAMBDA_CHANNEL map-lambda seat).
    private static final String NUMBER_OF_UNITS =
            "drr/regulation/common/trade/basket/reports/BasketConstituentNumberOfUnitsRule.java";
    private static final String UNIT_OF_MEASURE =
            "drr/regulation/common/trade/basket/reports/BasketConstituentUnitOfMeasureRule.java";
    // GREEN rules whose only-element collapse is a NON-meta type (getOptionPayout -> OptionPayout /
    // getForwardPayout -> ForwardPayout): the arm declines (leaf not meta → wrapper null), keeping the
    // plain MapperS.of(<X>.get()).<SettlementTerms>map(...) re-wrap, so they stay byte-matching golden.
    private static final String FIXING_DATE_LEG1 =
            "drr/regulation/common/trade/datetime/reports/FixingDateLeg1Rule.java";
    private static final String FIXING_DATE_LEG2 =
            "drr/regulation/common/trade/datetime/reports/FixingDateLeg2Rule.java";

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

    // ==== Flip locks (revert-RED): the 2 carriers now byte-match golden. ====

    @Test
    @EnabledIf("drrCellAvailable")
    void basketConstituentNumberOfUnits_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(NUMBER_OF_UNITS);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void basketConstituentUnitOfMeasure_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(UNIT_OF_MEASURE);
    }

    // ==== Positive-content locks (revert-RED): the hoist + null-guard-reconstruct form. ====

    /**
     * NumberOfUnits (the STATEMENT_SINK whole-output seat): the collapsed wrapper is hoisted to a
     * statement local and the output receiver is the null-guard reconstruct — NOT the fork's plain
     * {@code MapperS.of(<X>.get()).<BigDecimal>map(...)}.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void numberOfUnits_hoistsWrapperAndReconstructs() {
        String gen = gen(NUMBER_OF_UNITS);
        assertTrue(gen.contains("final ReferenceWithMetaNonNegativeQuantitySchedule "
                + "referenceWithMetaNonNegativeQuantitySchedule = MapperS.of(input)."
                + "<ReferenceWithMetaNonNegativeQuantitySchedule>mapC(\"getQuantity\", "
                + "basketConstituent -> basketConstituent.getQuantity()).get();"),
                "Expected the collapsed wrapper hoisted to a statement local before the output assignment");
        assertTrue(gen.contains("output = (referenceWithMetaNonNegativeQuantitySchedule == null ? "
                + "MapperS.<NonNegativeQuantitySchedule>ofNull() : "
                + "MapperS.of(referenceWithMetaNonNegativeQuantitySchedule.getValue()))."
                + "<BigDecimal>map(\"getValue\", "),
                "Expected the null-guard-reconstruct receiver for the downstream getValue nav");
        assertTrue(!gen.contains("output = MapperS.of(MapperS.of(input)."
                + "<ReferenceWithMetaNonNegativeQuantitySchedule>mapC(\"getQuantity\", "
                + "basketConstituent -> basketConstituent.getQuantity()).get()).<BigDecimal>map("),
                "The fork's plain MapperS.of(<wrapper>.get()).<BigDecimal>map re-wrap must be gone");
        assertTrue(gen.contains("import cdm.base.math.NonNegativeQuantitySchedule;"),
                "The bare value type import must be added for the <NonNegativeQuantitySchedule> witness");
    }

    /**
     * UnitOfMeasure (the LAMBDA_CHANNEL map-lambda seat): the {@code mapSingleToItem} expression lambda
     * is block-converted, hoisting the collapsed wrapper and returning the null-guard reconstruct — NOT
     * the fork's plain {@code item -> MapperS.of(item.<…>mapC(…).get()).<UnitType>map(…)}.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void unitOfMeasure_blockConvertsLambdaAndReconstructs() {
        String gen = gen(UNIT_OF_MEASURE);
        assertTrue(gen.contains("final ReferenceWithMetaNonNegativeQuantitySchedule "
                + "referenceWithMetaNonNegativeQuantitySchedule = item."
                + "<ReferenceWithMetaNonNegativeQuantitySchedule>mapC(\"getQuantity\", "
                + "basketConstituent -> basketConstituent.getQuantity()).get();"),
                "Expected the collapsed wrapper hoisted inside the block-converted map lambda");
        assertTrue(gen.contains("return (referenceWithMetaNonNegativeQuantitySchedule == null ? "
                + "MapperS.<NonNegativeQuantitySchedule>ofNull() : "
                + "MapperS.of(referenceWithMetaNonNegativeQuantitySchedule.getValue()))."
                + "<UnitType>map(\"getUnit\", "),
                "Expected the null-guard-reconstruct receiver for the downstream getUnit nav");
        assertTrue(!gen.contains(".mapSingleToItem(item -> MapperS.of(item."
                + "<ReferenceWithMetaNonNegativeQuantitySchedule>mapC(\"getQuantity\", "
                + "basketConstituent -> basketConstituent.getQuantity()).get()).<UnitType>map("),
                "The fork's plain expression-lambda MapperS.of(<wrapper>.get()).<UnitType>map re-wrap must be gone");
    }

    // ==== Green-safety / decline locks (pass on clean source too): a NON-meta only-element. ====

    /**
     * Green-safety — {@code FixingDateLeg1Rule} collapses a NON-meta {@code getOptionPayout} /
     * {@code getForwardPayout} ({@code OptionPayout} / {@code ForwardPayout}) via {@code only-element}
     * then navigates {@code getSettlementTerms}. The {@code only-element} leaf is not meta, so
     * {@code collapsedMetaDerefRewrapOrNull} declines ({@code metaWrapperOf} null) and the plain
     * {@code MapperS.of(<X>.get()).<SettlementTerms>map(...)} re-wrap stays — byte-matching golden on
     * clean source too.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void fixingDateLeg1_nonMetaOnlyElement_declinesStaysGreen() throws IOException {
        assertByteMatchesGolden(FIXING_DATE_LEG1);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void fixingDateLeg2_nonMetaOnlyElement_declinesStaysGreen() throws IOException {
        assertByteMatchesGolden(FIXING_DATE_LEG2);
    }

    private static String gen(String path) {
        assertNotNull(drrOutput, "drr generation did not run — corpus unavailable?");
        String g = drrOutput.get(path);
        assertNotNull(g, "Class not generated: " + path);
        return g;
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
                + path + " (PR #317 collapsedMetaDeref).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
