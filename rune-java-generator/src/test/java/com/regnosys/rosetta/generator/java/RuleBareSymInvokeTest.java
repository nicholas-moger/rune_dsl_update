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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static com.regnosys.rosetta.testutil.GenerationErrors.assertNoGenerationErrors;

/**
 * PR #280 — facet bareSymInvoke: ONE green-safe GENERATOR mechanism. 21 drr POJO Rule + 3 drr
 * FUNCTION byte flips (24 total; the user-chosen aim-bigger lever — the dominant residual the
 * resume prompt sized at ~123 carriers / ~22 clean-sole; the byte-oracle measured the genuinely
 * clean tail at 24).
 *
 * <p><b>The mechanism.</b> A function used as a navigation receiver — a disguised
 * {@code <Function> -> feature} chain (e.g. {@code extract RateOption -> indexTenor}, or
 * {@code tradeForEvent -> contractDetails}) — parses as an {@link
 * com.regnosys.rosetta.ast.expressions.references.REnumValueRef} (the grammar's
 * {@code EnumName -> ValueName} shape) whose {@code resolvedSymbol} the parser binds to an
 * {@link com.regnosys.rosetta.ast.functions.RFunction}. {@code ReferenceHandler.handle(REnumValueRef)}
 * gains a bare-FUNCTION receiver-nav arm symmetric to the existing bare-RULE arm
 * ({@code synthesizeFunctionReceiverNavigation}): it builds an {@code RFeatureCall} whose receiver
 * carries the RFunction, so the receiver compiles through {@code handle(RSymbolReference)}'s
 * bare-FUNCTION branch and the call navigates to {@code feature}. The fork previously fell through to
 * {@code synthesizeFeatureCall} (function-scope head resolution only) and rendered the bare type
 * literal {@code MapperS.of(RateOption)} (non-compiling).
 *
 * <p><b>The MapperS.of receiver wrap.</b> Unlike the bare-RULE sibling
 * ({@code renderImplicitRuleInvocation}, which {@code MapperS.of}-wraps unconditionally),
 * {@code renderImplicitFunctionInvocation} emits UNWRAPPED at its other seats (mapSingleToList
 * bodies, list-literal elements, filter predicates — PR #278). Golden navigates off the WRAPPED
 * value ({@code MapperS.of(<fn>.evaluate(item.get())).<T>map(...)}), so the method now detects the
 * navigation-receiver position (its synthesized receiver's parent is the feature call) and wraps —
 * the #278 unwrapped seats are NOT feature-call receivers, so they are preserved.
 *
 * <p><b>The @Inject field.</b> {@code FunctionDependencyCollector} injects the function dependency
 * via a new {@code REnumValueRef -> RFunction} branch (the bare-FUNCTION analogue of the
 * {@code REnumValueRef -> RRule} branch), so {@code <fn>.evaluate(...)} resolves the injected field.
 *
 * <p><b>The meta-leaf gate.</b> The arm DECLINES when the navigated feature resolves to a meta
 * wrapper ({@code FieldWithMetaX} / {@code ReferenceWithMetaX}): golden meta-derefs there
 * ({@code .<T>map("Type coercion", x -> x.getValue())}, a SEPARATE co-occupied mechanism), so
 * resolving the receiver without that deref would leak the wrapper type onto an enclosing
 * {@code thenArg} declaration — a within-waiver regression. No clean carrier in this facet navigates
 * a meta leaf.
 *
 * <p><b>Green-safe by construction.</b> The pre-fix bare type literal {@code MapperS.of(<Type>)}
 * never compiled, so no currently-green file carries it — every carrier was a NON_COMPILING waivered
 * mismatch. byte-oracle / stash-baseline measured exactly 24 flips (drr POJO 1982 -> 2003, drr
 * FUNCTION 1034 -> 1037), 0 within-waiver regressions, 83 toward-golden churn; cdm FUNCTION cells
 * (296 / 1044) UNCHANGED.
 *
 * <p>Anchors are WHOLE-FILE byte comparisons of the generated drr Rule/Function output against the
 * frozen goldens (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}. REVERT-VERIFIED RED.
 */
class RuleBareSymInvokeTest {

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
        funcGen.generate(output); // FUNCTION classes (one pass over the GeneratorModel)
        return output;
    }

    // ==== Flip locks (revert-RED): bareSymInvoke carriers now byte-match golden. ====

    /** POJO flip: a {@code period -> ...} FloatingRateReferencePeriod nav off a function receiver. */
    @Test
    @EnabledIf("drrCellAvailable")
    void floatingRateReferencePeriodLeg1_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/common/trade/underlier/reports/FloatingRateReferencePeriodLeg1Rule.java");
    }

    /** POJO flip (iosco cde): an OptionPremium/Valuation function-receiver nav. */
    @Test
    @EnabledIf("drrCellAvailable")
    void valuationAmount_iosco_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/standards/iosco/cde/version1/valuation/reports/ValuationAmountRule.java");
    }

    /** POJO flip (hkma): the {@code tradeForEvent -> ...} cascade exemplar (NON-meta first arm). */
    @Test
    @EnabledIf("drrCellAvailable")
    void finalContractualSettlementDate_hkma_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/hkma/rewrite/trade/reports/FinalContractualSettlementDateRule.java");
    }

    /** POJO flip: a NameOfTheFloatingRate function-receiver nav. */
    @Test
    @EnabledIf("drrCellAvailable")
    void nameOfTheFloatingRateOfLeg1_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/common/trade/underlier/reports/NameOfTheFloatingRateOfLeg1Rule.java");
    }

    /** FUNCTION flip: the bareSymInvoke arm is NOT rule-scoped — it fires in a function body too. */
    @Test
    @EnabledIf("drrCellAvailable")
    void getNameOfTheFloatingRateOfLeg1_function_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/common/functions/GetNameOfTheFloatingRateOfLeg1.java");
    }

    // ==== Meta-leaf gate lock (revert-RED for the gate): the decline keeps the type correct. ====

    /**
     * Gate lock — REPOINTED at PR #295 (was {@code MasterAgreementTypeRule}). #295 (conditionalMetaJoin)
     * is the #280 meta-leaf residual: it LIFTS the meta-leaf decline inside a MIXED-baresym conditional
     * ladder, so {@code MasterAgreementType} / {@code OtherMasterAgreementType} now FIRE + deref (they
     * flipped to byte-match golden — see {@code RuleConditionalMetaJoinTest}). The #280 meta-leaf gate
     * still holds EVERYWHERE ELSE, so this lock moved to {@code DTCC_Leg1CommodityInstrumentIDRule} — a
     * SINGLE/homogeneous-meta carrier whose only meta-leaf baresym arm {@code underlierForProduct ->
     * commodity} (CommodityProduct.commodity, META {@code ReferenceWithMetaCommodity}) is NOT in a
     * mixed-baresym ladder.
     *
     * <p>CONVERTED at PR #333 (anchor-break-is-signal, the #284/#295/#296 precedent): the
     * {@code baresymConditionalMetaJoin} facet lifts the #280 decline exactly here — the
     * carrier's OUTERMOST enclosing conditional JOINS to the leaf's wrapper
     * ({@code ReferenceWithMetaCommodity}) via the #331 all-present-arms-agree walker, so the
     * consumer seats (the #333 typedEmptyReturn / thenArg decl arms) coordinate on the wrapper
     * and the nav fires golden's form; the carrier flipped byte-identical (byte-locked in
     * {@code RuleBlockComposeTest}). This lock now pins the FIRED wrapper form; a MIXED or
     * different-wrapper join still declines (the #280 gate holds everywhere else).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void dtccLeg1CommodityInstrumentId_allMetaJoin_firesWrapperNav() {
        assertNotNull(drrOutput, "drr generation did not run — corpus unavailable?");
        String gen = drrOutput.get(
                "drr/regulation/common/dtcc/reports/DTCC_Leg1CommodityInstrumentIDRule.java");
        assertNotNull(gen, "DTCC_Leg1CommodityInstrumentIDRule not generated");
        assertFalse(gen.contains("MapperS.of(UnderlierForProduct)"),
                "the pre-#333 bare type literal must be gone — the all-meta-join relaxation fires");
        assertTrue(gen.contains(
                "MapperS.of(underlierForProduct.evaluate(item.get())).<ReferenceWithMetaCommodity>map"),
                "the baresym nav renders golden's wrapper form under the all-meta conditional join");
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
                + path + " (PR #280 bareSymInvoke).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
