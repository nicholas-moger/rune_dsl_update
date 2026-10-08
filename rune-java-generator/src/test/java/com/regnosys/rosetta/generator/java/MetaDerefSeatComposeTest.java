package com.regnosys.rosetta.generator.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
 * PR #334 anchors — the meta-deref seat compose (32 flips: 8 FUNCTION + 24 drr POJO).
 *
 * <p>The upstream law (verified against the vendored
 * {@code ExpressionGenerator.binaryExpr} + {@code TypeCoercionService}): every
 * consumption seat compiles its operand against the WRAPPER-level meta-free expected
 * ({@code MAPPER.wrapExtends(joined)}), so a meta-typed operand derefs inline (guarded
 * MapperS / bare MapperC) instead of collapsing to the non-compiling
 * {@code .get().getValue()}. The #334 facets open the FIRING gates seat by seat — the
 * emission machinery ({@code WrappedItemCoercer} / {@code coerceNavigationReceiver})
 * was already byte-faithful:
 *
 * <ul>
 *   <li><b>CP1 arithStringJoin + arithOperandWrapperCoerce:</b> a string {@code +}
 *       joins {@code <String, String, String>} (the documented W42 reach-only decline
 *       opened) and RESOLVED-join arithmetic operands compile interior + coerce at
 *       wrapper level; alias operands re-type through the A1 {@code FunctionAliasHelper}
 *       channel (widened to basic item types).</li>
 *   <li><b>CP2 condArmMixedMetaJoin:</b> a META-typed conditional arm whose sibling arm
 *       is PROVABLY bare (arithmetic/to-string/literal/count — never a nav or
 *       {@code empty}) joins bare and derefs in-arm, AST-gated so registration order
 *       keeps golden's method-wide {@code fieldWithMetaString0/1} numbering.</li>
 *   <li><b>CP3 depFieldNamingSeeds:</b> injected {@code @Inject} dependency field names
 *       join the unified method naming seeds, so type-derived deferred lambda params
 *       escape against them ({@code _technicalRecordId}).</li>
 *   <li><b>CP4 metaItemFilterChainRecovery:</b> the mechanism-3 implicit-item fallback
 *       descends FILTER layers (element-preserving) to the chain terminal, double-gated
 *       by {@code metaValueHasNavFeature} + {@code metaNavResultType}'s meta gate.</li>
 *   <li><b>CP5 containsClosureParamMetaDeref:</b> a META-element closure-param
 *       {@code contains} operand joined against a provably meta-free bare-fn sibling
 *       derefs guarded.</li>
 *   <li><b>CP6 ptrrBlockArmWrap at the LADDER:</b> the #333/#243 bare-{@code .get()}
 *       collapse re-wrap extended to the ladder rungs + terminal.</li>
 *   <li><b>CP7 listOpBareMultiFnReceiverWrap:</b> a wrap-form list op over a bare
 *       MULTI-output fn invocation wraps the witnessed {@code MapperC.<X>of(...)} (the
 *       #308 raw-List contract at the receiver seat).</li>
 *   <li><b>CP8 ctorArmMapperSWrap + ctorLiteralBigDecimalValueOf:</b> a CONSTRUCTOR
 *       ladder arm wraps {@code MapperS.of(...)}; an int literal into a BigDecimal
 *       ({@code number}) ctor setter renders {@code BigDecimal.valueOf(N)}.</li>
 *   <li><b>CP9 tostringAliasEnumSource:</b> a to-string ALIAS source re-types through
 *       the A1 channel and resolves its enum through the alias BODY + conditional-arm
 *       descent ({@code FloatingRateIndexEnum::toDisplayString}).</li>
 * </ul>
 *
 * <p>Anchors are WHOLE-FILE byte comparisons of generated output against the frozen
 * 9.83.0 goldens (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached} paths (FUNCTION-kind for the
 * function carriers, rule-kind for the drr POJO carriers).
 */
class MetaDerefSeatComposeTest {

    private static final Path CDM5_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-5.38.0");
    private static final Path CDM5_GOLDEN_DIR =
            CDM5_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    // ==== CP1 flip carriers (wrapper-level arithmetic operand coercion; the alias
    // operand types through the A1 channel — Integer item in a BigDecimal join gains
    // the guarded BigDecimal.valueOf map) ====
    private static final String CDM5_GENERATE_WEIGHTS =
            "cdm/observable/asset/calculatedrate/functions/GenerateWeights.java";
    private static final String CDM6_GENERATE_WEIGHTS =
            "cdm/observable/asset/calculatedrate/functions/GenerateWeights.java";

    // ==== CP1+CP2+CP3 flip carriers (the string join + in-arm guarded deref +
    // method-wide 0/1 numbering + the `_technicalRecordId` dep-field escape) ====
    private static final String DRR_ASIC_TECHNICAL_RECORD_ID =
            "drr/regulation/asic/rewrite/margin/reports/TechnicalRecordIdRule.java";
    private static final String DRR_CFTC_DTCC_MESSAGE_ID =
            "drr/regulation/cftc/rewrite/margin/reports/DTCC_MessageIDRule.java";

    // ==== CP4 flip carriers (the filter-descend implicit-item recovery) ====
    private static final String CDM5_COMPARE_TRADE_LOT =
            "cdm/product/template/functions/CompareTradeLotToAmount.java";
    private static final String DRR_GET_COLLATERAL_BALANCES =
            "drr/regulation/common/functions/GetCollateralBalancesForMarginType.java";

    // ==== CP5 flip carrier (the contains closure-param guarded deref) ====
    private static final String DRR_NATURAL_PERSON_BUYER_OR_SELLER =
            "drr/regulation/common/functions/NaturalPersonBuyerOrSeller.java";

    // ==== CP6 flip carriers (the ladder-rung/terminal bare-collapse re-wrap) ====
    private static final String DRR_DTCC_DELIVERY_LOCATION =
            "drr/regulation/common/dtcc/reports/DTCC_DeliveryLocationRule.java";
    private static final String DRR_FCA_INTERCONNECTION_POINT =
            "drr/regulation/fca/ukemir/refit/trade/reports/InterconnectionPointRule.java";

    // ==== CP7 flip carrier (the witnessed MapperC.<X>of raw-List wrap at first()) ====
    private static final String DRR_UNDERLIER_ID_OTHER_SOURCE_LEG1 =
            "drr/standards/iosco/cde/version3/underlier/reports/UnderlierIDOtherSourceLeg1Rule.java";

    // ==== CP8 flip carrier (the ctor arm MapperS.of wrap + BigDecimal.valueOf(0)) ====
    private static final String DRR_ESMA_SPREADOF_LEG1 =
            "drr/regulation/esma/emir/refit/trade/reports/SpreadofLeg1Rule.java";

    // ==== CP9 flip carrier (the alias to-string deref + toDisplayString) ====
    private static final String DRR_UPI_FLOATING_RATE_INDEX =
            "drr/enrichment/upi/functions/FloatingRateIndex.java";

    // ==== Green pins ====
    // depFieldNamingSeeds decline-side (Seat-1 #334 note — the zero-carrier argument,
    // banked in lieu of a dedicated pin): a green file whose dep-named lambda param
    // must stay BARE cannot exist — upstream registers dependency fields on the CLASS
    // scope the method scope inherits and ALWAYS escapes a colliding lambda param, so
    // a fork file rendering the bare colliding name could never have byte-matched.
    // The D11 strict gate (20/20 green pre- and post-facet) + the full gensuite are
    // the mechanical backstop.
    // The ladder single-rung degenerate stays byte-identical under the CP6/CP8 ladder
    // arm-wrap widenings (the #331/#333 pin re-locked).
    private static final String ESMA_SPREAD_LEG1_NOTATION_GREEN =
            "drr/regulation/esma/emir/refit/trade/reports/SpreadOfLeg1NotationRule.java";
    // The CP1 string-join DECLINE side: a numeric MapperMaths context keeps its
    // <Integer, Integer, Integer> witness + bare count operand (the #332 pin re-locked
    // under the operand-compile switch to interior + wrapper-level coercion).
    private static final String CDM6_QUALIFY_CASH_TRANSFER_GREEN =
            "cdm/event/qualification/functions/Qualify_CashTransfer.java";

    // (The #334 DECLINE-lock constant DRR_UNDERLIER_ID_OTHER_LEG1_DECLINE was removed at
    // PR #338 — the carrier flipped via ladderBareCalleeMixedJoin; see the superseded
    // note at the former test site below + ThenArgMetaPreserveLadderJoinTest.)

    private static Map<String, String> cdm5FnOutput;
    private static Map<String, String> cdm6FnOutput;
    private static Map<String, String> drrFnOutput;
    private static Map<String, String> drrRuleOutput;

    static boolean cellsAvailable() {
        // Every generated cell's SOURCE dir is checked (not just cdm5's) — a partially
        // cloned corpus must skip rather than fail at generation time (Copilot #332 R1).
        return Files.isDirectory(CDM5_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM6_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM5_GOLDEN_DIR)
                && Files.isDirectory(CDM6_GOLDEN_DIR)
                && Files.isDirectory(DRR_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (cellsAvailable()) {
            cdm5FnOutput = generateFunctions(
                    new D11CorpusRegressionTest.CellSpec("cdm", "5.38.0", CDM5_CELL_ROOT));
            cdm6FnOutput = generateFunctions(
                    new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CDM6_CELL_ROOT));
            drrFnOutput = generateFunctions(
                    new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT));
            drrRuleOutput = generateRuleKinds(
                    new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT));
        }
    }

    /** The REAL D11 FUNCTION-kind generation path (function_comparison). */
    private static Map<String, String> generateFunctions(D11CorpusRegressionTest.CellSpec cell)
            throws IOException {
        var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(),
                D11CorpusRegressionTest.emissionFilter(cell));
        var typeUtil = new JavaTypeUtil();
        var funcGen = new FunctionGenerator(gm, new JavaTypeTranslator(typeUtil), typeUtil);
        Map<String, String> output = new LinkedHashMap<>();
        assertNoGenerationErrors(funcGen.generateWithErrors(output));
        return output;
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

    // ==== CP1 flip locks (revert-RED) ====

    @Test
    @EnabledIf("cellsAvailable")
    void cdm5GenerateWeights_aliasOperandWrapperCoerce_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5FnOutput, CDM5_GOLDEN_DIR, CDM5_GENERATE_WEIGHTS);
    }

    @Test
    @EnabledIf("cellsAvailable")
    void cdm6GenerateWeights_aliasOperandWrapperCoerce_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, CDM6_GENERATE_WEIGHTS);
    }

    // ==== CP1+CP2+CP3 flip locks ====

    @Test
    @EnabledIf("cellsAvailable")
    void asicTechnicalRecordId_stringJoinArmDerefDepEscape_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(drrRuleOutput, DRR_GOLDEN_DIR, DRR_ASIC_TECHNICAL_RECORD_ID);
    }

    @Test
    @EnabledIf("cellsAvailable")
    void cftcDtccMessageId_stringJoinArmDerefDepEscape_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrRuleOutput, DRR_GOLDEN_DIR, DRR_CFTC_DTCC_MESSAGE_ID);
    }

    // ==== CP4 flip locks ====

    @Test
    @EnabledIf("cellsAvailable")
    void cdm5CompareTradeLot_filterChainItemRecovery_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5FnOutput, CDM5_GOLDEN_DIR, CDM5_COMPARE_TRADE_LOT);
    }

    @Test
    @EnabledIf("cellsAvailable")
    void drrGetCollateralBalances_filterChainItemRecovery_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(drrFnOutput, DRR_GOLDEN_DIR, DRR_GET_COLLATERAL_BALANCES);
    }

    // ==== CP5 flip lock ====

    @Test
    @EnabledIf("cellsAvailable")
    void naturalPersonBuyerOrSeller_containsClosureParamDeref_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(drrFnOutput, DRR_GOLDEN_DIR, DRR_NATURAL_PERSON_BUYER_OR_SELLER);
    }

    // ==== CP6 flip locks ====

    @Test
    @EnabledIf("cellsAvailable")
    void dtccDeliveryLocation_ladderRungCollapseWrap_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrRuleOutput, DRR_GOLDEN_DIR, DRR_DTCC_DELIVERY_LOCATION);
    }

    @Test
    @EnabledIf("cellsAvailable")
    void fcaInterconnectionPoint_ladderRungCollapseWrap_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrRuleOutput, DRR_GOLDEN_DIR, DRR_FCA_INTERCONNECTION_POINT);
    }

    // ==== CP7 flip lock ====

    @Test
    @EnabledIf("cellsAvailable")
    void underlierIdOtherSourceLeg1_bareMultiFnFirstReceiverWrap_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(drrRuleOutput, DRR_GOLDEN_DIR,
                DRR_UNDERLIER_ID_OTHER_SOURCE_LEG1);
    }

    // ==== CP8 flip lock ====

    @Test
    @EnabledIf("cellsAvailable")
    void esmaSpreadofLeg1_ctorArmWrapAndLiteralValueOf_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrRuleOutput, DRR_GOLDEN_DIR, DRR_ESMA_SPREADOF_LEG1);
    }

    // ==== CP9 flip lock ====

    @Test
    @EnabledIf("cellsAvailable")
    void upiFloatingRateIndex_aliasToStringEnumSource_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFnOutput, DRR_GOLDEN_DIR, DRR_UPI_FLOATING_RATE_INDEX);
    }

    // ==== Green pins ====

    @Test
    @EnabledIf("cellsAvailable")
    void spreadOfLeg1Notation_singleRungDegenerate_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrRuleOutput, DRR_GOLDEN_DIR, ESMA_SPREAD_LEG1_NOTATION_GREEN);
    }

    @Test
    @EnabledIf("cellsAvailable")
    void qualifyCashTransfer_integerJoinStaysBare_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, CDM6_QUALIFY_CASH_TRANSFER_GREEN);
    }

    // ==== DECLINE lock — SUPERSEDED at PR #338 ====
    //
    // The #334 underlierIdOtherLeg1_noBareSibling_armKeepsWrapperTypedEmpty lock asserted
    // the wrapper-typed empty (`MapperS.<FieldWithMetaString>ofNull()`) because the CP2
    // provably-bare gate had no sibling evidence at this seat: the bare sibling is a bare
    // FUNCTION-CALL arm (`GetOtherUnderlierLeg1`, declared output plain string) — invisible
    // to the #295 baresym pre-scan, and navs are never provably bare (the #295 trap).
    // PR #338's ladderBareCalleeMixedJoin widens the ladder pre-scan to the declared-output
    // shapes, the mixed ladder joins bare, and the carrier flips byte-identical. The flip
    // (whole-file byte lock) and the widened gate's decline coverage now live in
    // ThenArgMetaPreserveLadderJoinTest.

    private static void assertByteMatchesGolden(Map<String, String> output, Path goldenDir,
            String path) throws IOException {
        assertNotNull(output, "cell output not generated");
        String gen = output.get(path);
        assertNotNull(gen, "missing generated output: " + path);
        Path goldenPath = goldenDir.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "missing golden: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(gen),
                path + " must byte-match the frozen 9.83.0 golden");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
