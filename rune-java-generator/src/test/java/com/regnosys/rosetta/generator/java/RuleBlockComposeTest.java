package com.regnosys.rosetta.generator.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
 * PR #333 anchors — the drr POJO block compose (11 flips, all drr Rule POJOs).
 *
 * <ul>
 *   <li><b>CP1 deepThenArmHoistDrain:</b> a deep-then {@code thenArg} decl registered
 *       during an ite-chain ARM compile emits INSIDE the owning branch when the branch
 *       content references its sentinel TRANSITIVELY (the fixed-point closure over the
 *       drained hoists — a chained {@code thenArg1..N} pipe references each predecessor
 *       from the SUCCESSOR's initializer). Pre-#333 the re-register fall-through dropped
 *       the decl silently while the arm kept the dangling sentinel reference.</li>
 *   <li><b>CP2 dtccDeclUseConsistency:</b> the LAMBDA_CHANNEL {@code thenArg} decl
 *       renders the SAME deferred token as its consumer reference, so decl and ref
 *       finalize to ONE name through {@code resolveUnifiedDeferredNames} (the eager
 *       {@code getActualName()} missed the #329 unified seeding — the ref escaped
 *       {@code _thenArg} while the decl shadowed the method local).</li>
 *   <li><b>CP3 ladderMultiLineArm + navLambdaTypeNaming:</b> a multi-line ladder ARM
 *       re-anchors its continuation lines at return-depth (+1 relative) instead of
 *       declining the block form; a first/last/only-element collapse over a disguised
 *       2-name chain names the lambda var from the chain's terminal feature type
 *       ({@code _underlier} → {@code commodityPayout}).</li>
 *   <li><b>CP4 ptrrBlockArmWrap + metaTypedOfNull:</b> a bare {@code .get()}-collapsed
 *       ONLY_ELEMENT then re-wraps {@code MapperS.of(...)} at the elseless-block
 *       return; a null-typed then recovers its meta {@code ofNull} type via the #330
 *       unique-wrapper scan.</li>
 *   <li><b>CP5 conditionalOutputMeta:</b> the whole-output meta deref gains the
 *       CONDITIONAL-body channel via the #331 all-present-arms-agree walker
 *       (extract-unwrapped; declined when any arm proves multi or carries a nested
 *       then-pipe); the ladder typed-empty reads the SAME join.</li>
 *   <li><b>CP7 baresymConditionalMetaJoin:</b> the #280 meta-leaf baresym decline
 *       relaxes when the outermost enclosing conditional JOINS to exactly the leaf's
 *       wrapper (consumer seats coordinate through the same walker).</li>
 * </ul>
 *
 * <p>Anchors are WHOLE-FILE byte comparisons of generated output against the frozen
 * 9.83.0 goldens (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached} rule-kind path.
 */
class RuleBlockComposeTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    // ==== CP1 flip carriers (the in-branch min-hoist decl) ====
    private static final String JFSA_NOTIONAL_LEG1 =
            "drr/regulation/jfsa/rewrite/trade/reports/NotionalAmountLeg1Rule.java";
    private static final String JFSA_NOTIONAL_LEG2 =
            "drr/regulation/jfsa/rewrite/trade/reports/NotionalAmountLeg2Rule.java";

    // ==== CP2 flip carriers (decl==ref via the shared deferred token: `_thenArg`) ====
    private static final String CFTC_DTCC_TXN_ID =
            "drr/regulation/cftc/rewrite/dtcc/reports/DTCC_TradeParty1TransactionIDRule.java";
    private static final String CSA_DTCC_TXN_ID =
            "drr/regulation/csa/rewrite/dtcc/reports/DTCC_TradeParty1TransactionIDRule.java";

    // ==== CP3 flip carriers (block ladder + element-typed lambda naming) ====
    private static final String FCA_BASE_PRODUCT =
            "drr/regulation/fca/ukemir/refit/trade/reports/BaseProductRule.java";
    private static final String FCA_SUB_PRODUCT =
            "drr/regulation/fca/ukemir/refit/trade/reports/SubProductRule.java";
    private static final String FCA_FURTHER_SUB_PRODUCT =
            "drr/regulation/fca/ukemir/refit/trade/reports/FurtherSubProductRule.java";

    // ==== CP4 flip carriers (MapperS.of re-wrap + meta-typed ofNull) ====
    private static final String ESMA_PTRR_SERVICE_PROVIDER =
            "drr/regulation/esma/emir/refit/trade/reports/PTRRServiceProviderRule.java";
    private static final String FCA_PTRR_SERVICE_PROVIDER =
            "drr/regulation/fca/ukemir/refit/trade/reports/PTRRServiceProviderRule.java";

    // ==== CP5 flip carrier (conditional-body whole-output deref + joined typed-empty) ====
    private static final String IOSCO_CUSTOM_BASKET_CODE =
            "drr/standards/iosco/cde/version1/basket/reports/CustomBasketCodeRule.java";

    // ==== CP7 flip carrier (all-meta-join baresym nav) ====
    private static final String COMMON_DTCC_LEG1_COMMODITY_ID =
            "drr/regulation/common/dtcc/reports/DTCC_Leg1CommodityInstrumentIDRule.java";

    // ==== Green pins ====
    // The ladder renderer's single-rung degenerate stays byte-identical post-F1
    // (the #331 green pin, re-locked under the multi-line-arm opening).
    private static final String ESMA_SPREAD_LEG1_GREEN =
            "drr/regulation/esma/emir/refit/trade/reports/SpreadOfLeg1NotationRule.java";
    // A no-collision LAMBDA_CHANNEL thenArg (the #309 carrier): the shared-token decl
    // resolves to the SAME bare name the eager form rendered — byte-identical by the
    // shared-token construction.
    private static final String ESMA_FLOATING_RATE_REF_PERIOD_GREEN =
            "drr/regulation/esma/emir/refit/trade/reports/FloatingRateReferencePeriodOfLeg1MultiplierRule.java";
    // The conditionalOutputMeta DECLINE lock (still waivered): the esma
    // UnderlyingIdentification arms carry nested then-pipes yielding MULTI content
    // golden derefs ELEMENT-WISE — the single-value hoist must NOT fire.
    private static final String ESMA_UNDERLYING_ID_DECLINE =
            "drr/regulation/esma/emir/refit/trade/reports/UnderlyingIdentificationRule.java";

    private static Map<String, String> drrOutput;

    static boolean cellsAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (cellsAvailable()) {
            drrOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT));
        }
    }

    private static Map<String, String> generateCell(D11CorpusRegressionTest.CellSpec cell)
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

    // ==== CP1 flip locks (revert-RED) ====

    @Test
    @EnabledIf("cellsAvailable")
    void jfsaNotionalAmountLeg1_inBranchMinHoist_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(JFSA_NOTIONAL_LEG1);
    }

    @Test
    @EnabledIf("cellsAvailable")
    void jfsaNotionalAmountLeg2_inBranchMinHoist_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(JFSA_NOTIONAL_LEG2);
    }

    // ==== CP2 flip locks ====

    @Test
    @EnabledIf("cellsAvailable")
    void cftcDtccTradeParty1TxnId_declUseConsistency_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(CFTC_DTCC_TXN_ID);
    }

    @Test
    @EnabledIf("cellsAvailable")
    void csaDtccTradeParty1TxnId_declUseConsistency_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(CSA_DTCC_TXN_ID);
    }

    // ==== CP3 flip locks ====

    @Test
    @EnabledIf("cellsAvailable")
    void fcaBaseProduct_multiLineArmBlockLadder_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(FCA_BASE_PRODUCT);
    }

    @Test
    @EnabledIf("cellsAvailable")
    void fcaSubProduct_multiLineArmBlockLadder_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(FCA_SUB_PRODUCT);
    }

    @Test
    @EnabledIf("cellsAvailable")
    void fcaFurtherSubProduct_typedEmptyTerminal_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(FCA_FURTHER_SUB_PRODUCT);
    }

    // ==== CP4 flip locks ====

    @Test
    @EnabledIf("cellsAvailable")
    void esmaPtrrServiceProvider_collapseWrapMetaOfNull_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(ESMA_PTRR_SERVICE_PROVIDER);
    }

    @Test
    @EnabledIf("cellsAvailable")
    void fcaPtrrServiceProvider_collapseWrapMetaOfNull_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(FCA_PTRR_SERVICE_PROVIDER);
    }

    // ==== CP5 flip lock ====

    @Test
    @EnabledIf("cellsAvailable")
    void ioscoCustomBasketCode_conditionalOutputDeref_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(IOSCO_CUSTOM_BASKET_CODE);
    }

    // ==== CP7 flip lock ====

    @Test
    @EnabledIf("cellsAvailable")
    void commonDtccLeg1CommodityInstrumentId_allMetaJoinBaresym_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(COMMON_DTCC_LEG1_COMMODITY_ID);
    }

    // ==== Green pins ====

    @Test
    @EnabledIf("cellsAvailable")
    void spreadOfLeg1Notation_singleRungDegenerate_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(ESMA_SPREAD_LEG1_GREEN);
    }

    @Test
    @EnabledIf("cellsAvailable")
    void floatingRateRefPeriodLeg1Multiplier_noCollisionSharedToken_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(ESMA_FLOATING_RATE_REF_PERIOD_GREEN);
        // The lambda-channel thenArg decl and its reference resolve to the SAME bare
        // name when no outer collision exists (the shared-token construction).
        assertTrue(drrOutput.get(ESMA_FLOATING_RATE_REF_PERIOD_GREEN).contains(" thenArg = "),
                "the no-collision lambda-channel thenArg must stay bare");
    }

    @Test
    @EnabledIf("cellsAvailable")
    void esmaUnderlyingIdentification_pipedArmsDeclineOutputDeref() {
        assertNotNull(drrOutput, "cell output not generated");
        String gen = drrOutput.get(ESMA_UNDERLYING_ID_DECLINE);
        assertNotNull(gen, "missing generated output: " + ESMA_UNDERLYING_ID_DECLINE);
        assertFalse(gen.contains("final FieldWithMetaString fieldWithMetaString = thenArg"),
                "conditionalOutputMeta must DECLINE on nested-then-pipe arms (golden derefs"
                        + " element-wise on this MULTI-output ladder)");
    }

    private static void assertByteMatchesGolden(String path) throws IOException {
        assertNotNull(drrOutput, "cell output not generated");
        String gen = drrOutput.get(path);
        assertNotNull(gen, "missing generated output: " + path);
        Path goldenPath = DRR_GOLDEN_DIR.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "missing golden: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(gen),
                path + " must byte-match the frozen 9.83.0 golden");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
