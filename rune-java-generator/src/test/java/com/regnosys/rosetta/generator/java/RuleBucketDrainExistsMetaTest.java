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
 * PR #331 anchors — the bucket-drain + meta compose.
 *
 * <ul>
 *   <li><b>CP1 isoPruneConfig:</b> the model project's {@code rosetta-config.yml}
 *       {@code generators.doNotPrune} entries (upstream
 *       {@code RosettaGeneratorsConfiguration} → {@code IShouldPrune.Default})
 *       render the builder {@code prune()} KEEP form
 *       ({@code if (x!=null) x.prune();}) + the {@code hasData()} presence-only
 *       form for a config-disabled (type, attribute) pair. Sole carriers: the 3
 *       iso20022 hkma {@code ClearingPartyAndTime*Choice__1.dtls} entries — the
 *       whole iso CODEGEN_DRIFT population.</li>
 *   <li><b>CP2 fpmlListParamEscape:</b> the {@code add(List)}/{@code set(List)}
 *       overload param (the pluralized property name) escapes with {@code _}
 *       when it names a SIBLING property whose builder field it would shadow
 *       (upstream {@code JavaClassScope.createUniqueIdentifier}). Sole carrier:
 *       fpml {@code CommodityMarketDisruption} — the last CODEGEN_DRIFT file
 *       (the bucket is ELIMINATED at this PR).</li>
 *   <li><b>CP3 existsMetaSeats:</b> the two #315-deferred exists-operand meta-wrap
 *       seats, unblocked by the #330 recovery extension — (a) the WHOLE-map-body
 *       {@code item -> exists(<metaRule>).asMapper()} (compileLambda distributes
 *       {@code .asMapper()} into both branches + block-renders the value hoist
 *       with a bare-ternary return); (b) the STATEMENT seat (the value decl
 *       registers on the statement-hoist sink; {@code appendIteHoistChainCore}'s
 *       new condition-hoist channel emits it ahead of the {@code ifThenElseResult}
 *       decl; {@code LogicalHandler} collapses the distributed conditional to its
 *       parenthesized form at both {@code andNullSafe} positions — also
 *       un-crashing the hkma UATPI ladder) + per-RUNG ladder condition-hoist
 *       drains (each rung's value decls land immediately before its
 *       {@code if (}).</li>
 *   <li><b>CP4 conditionalArmMetaWrap + multiElemMetaDeref:</b> the #265 wrap
 *       fires at the CONDITIONAL-ARM seat ({@code extract [if <cond> then
 *       <metaRule>]}), gated by the #330 all-present-arms-agree JOIN over the
 *       whole conditional ({@code recoverExprMetaWrapper}); the whole-output
 *       deref recovers via the SAME walker at the generic SET seat. A MULTI rule
 *       output whose then-body element is a meta wrapper derefs ELEMENT-WISE
 *       before the List collapse ({@code .<String>map("Type coercion", …)
 *       .getMulti()} — the #287 multi-output decline completed).</li>
 * </ul>
 *
 * <p>Anchors are WHOLE-FILE byte comparisons of generated output against the
 * frozen 9.83.0 goldens (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached} + the REAL
 * {@link D11CorpusRegressionTest#readDoNotPrune} config channel.
 */
class RuleBucketDrainExistsMetaTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path ISO_CELL_ROOT = Path.of("../test-corpus/iso20022/iso20022-1.38.0");
    private static final Path ISO_GOLDEN_DIR =
            ISO_CELL_ROOT.resolve("rosetta-source/target/classes/generated/java");
    private static final Path FPML_CELL_ROOT = Path.of("../test-corpus/rune-fpml/rune-fpml-2.0.0");
    private static final Path FPML_GOLDEN_DIR =
            FPML_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    // ==== CP1 flip carriers (the 3 doNotPrune-configured iso choice POJOs) ====
    private static final String ISO_DTCC_CPT22 =
            "iso20022/auth030/hkma/dtcc/ClearingPartyAndTime22Choice__1.java";
    private static final String ISO_TR_CPT21 =
            "iso20022/auth030/hkma/tr/ClearingPartyAndTime21Choice__1.java";
    private static final String ISO_TR_CPT22 =
            "iso20022/auth030/hkma/tr/ClearingPartyAndTime22Choice__1.java";
    // CP1 green pin: an asic variant of the SAME shape, NOT in the doNotPrune config —
    // keeps the NULLIFY prune form + the .hasData() recursion byte-identically.
    private static final String ISO_ASIC_CPT22_GREEN =
            "iso20022/auth030/asic/ClearingPartyAndTime22Choice__1.java";

    // ==== CP2 flip carrier (the last CODEGEN_DRIFT file) ====
    private static final String FPML_COMMODITY_MARKET_DISRUPTION =
            "fpml/consolidated/com/CommodityMarketDisruption.java";
    // CP2 green pin: a plural-adder POJO with NO sibling-name collision — the
    // add(List)/set(List) params stay UNESCAPED byte-identically.
    private static final String FPML_BULLION_PHYSICAL_LEG_GREEN =
            "fpml/consolidated/com/BullionPhysicalLeg.java";

    // ==== CP3 flip carriers ====
    // (a) the whole-map-body exists/asMapper seat (inner rule cde.basket.CustomBasketCode).
    private static final String DRR_CUSTOM_BASKET_INDICATOR =
            "drr/regulation/common/trade/basket/reports/CustomBasketIndicatorRule.java";
    // (b) the STATEMENT seat — notExists in a rule-body ite-hoist condition
    // (inner rule upi.UniqueProductIdentifier; the #318-blocked carrier).
    private static final String DRR_HKMA_UPI_PROPRIETARY =
            "drr/regulation/hkma/rewrite/trade/reports/UniqueProductIdentifierProprietaryRule.java";
    // (b)+per-rung drains — the #330-surfaced CRASH carrier (JavaConditionalExpression
    // at the andNullSafe operand): the multi-rung ladder now renders with each rung's
    // string0/string1+string2 decls before its own `if (`, and the MIXED terminal arm
    // (a bare Extract_ function) keeps the arms BARE per the all-arms-agree join.
    private static final String DRR_HKMA_UATPI =
            "drr/regulation/hkma/rewrite/trade/reports/UnderlyingAssetTradingPlatformIdentifierRule.java";
    // CP3 green pin: the #315 SINGLE-rung carrier — the per-rung drain degenerates to
    // the #315 block-top position, byte-identical.
    private static final String DRR_SPREAD_LEG1_GREEN =
            "drr/regulation/esma/emir/refit/trade/reports/SpreadOfLeg1NotationRule.java";

    // ==== CP4 flip carriers ====
    // The conditional-ARM meta wrap + whole-output deref (elseless meta arm — the
    // all-arms-agree join keeps the wrapper).
    private static final String DRR_CSA_UATPI_LEG1 =
            "drr/regulation/csa/rewrite/trade/reports/UnderlyingAssetTradingPlatformIdentifierLeg1Rule.java";
    private static final String DRR_CSA_UATPI_LEG2 =
            "drr/regulation/csa/rewrite/trade/reports/UnderlyingAssetTradingPlatformIdentifierLeg2Rule.java";
    // The MULTI element-wise deref (the #287 multi-output decline completed).
    private static final String DRR_ORIGINAL_SWAP_UTI =
            "drr/regulation/common/trade/link/reports/OriginalSwapUTIRule.java";

    private static Map<String, String> drrOutput;
    private static Map<String, String> isoOutput;
    private static Map<String, String> fpmlOutput;

    static boolean cellsAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR)
                && Files.isDirectory(ISO_GOLDEN_DIR)
                && Files.isDirectory(FPML_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (cellsAvailable()) {
            drrOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT));
            isoOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("iso20022", "1.38.0", ISO_CELL_ROOT));
            fpmlOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("rune-fpml", "2.0.0", FPML_CELL_ROOT));
        }
    }

    private static Map<String, String> generateCell(D11CorpusRegressionTest.CellSpec cell)
            throws IOException {
        var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
        // The REAL production config channel: the iso cell's rosetta-config.yml
        // doNotPrune entries (CP1); every other cell resolves to the empty set.
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
    void isoDtccClearingPartyAndTime22_keepFormPrune_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(isoOutput, ISO_GOLDEN_DIR, ISO_DTCC_CPT22);
    }

    @Test
    @EnabledIf("cellsAvailable")
    void isoTrClearingPartyAndTime21_keepFormPrune_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(isoOutput, ISO_GOLDEN_DIR, ISO_TR_CPT21);
    }

    @Test
    @EnabledIf("cellsAvailable")
    void isoTrClearingPartyAndTime22_keepFormPrune_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(isoOutput, ISO_GOLDEN_DIR, ISO_TR_CPT22);
    }

    // ==== CP2 flip lock (revert-RED) ====

    @Test
    @EnabledIf("cellsAvailable")
    void fpmlCommodityMarketDisruption_siblingCollisionEscape_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(fpmlOutput, FPML_GOLDEN_DIR, FPML_COMMODITY_MARKET_DISRUPTION);
    }

    // ==== CP3 flip locks (revert-RED) ====

    @Test
    @EnabledIf("cellsAvailable")
    void customBasketIndicator_wholeBodyExistsAsMapper_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrOutput, DRR_GOLDEN_DIR, DRR_CUSTOM_BASKET_INDICATOR);
    }

    @Test
    @EnabledIf("cellsAvailable")
    void hkmaUpiProprietary_statementSeatNotExists_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrOutput, DRR_GOLDEN_DIR, DRR_HKMA_UPI_PROPRIETARY);
    }

    @Test
    @EnabledIf("cellsAvailable")
    void hkmaUatpi_ladderPerRungHoists_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrOutput, DRR_GOLDEN_DIR, DRR_HKMA_UATPI);
    }

    // ==== CP4 flip locks (revert-RED) ====

    @Test
    @EnabledIf("cellsAvailable")
    void csaUatpiLeg1_conditionalArmMetaWrap_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrOutput, DRR_GOLDEN_DIR, DRR_CSA_UATPI_LEG1);
    }

    @Test
    @EnabledIf("cellsAvailable")
    void csaUatpiLeg2_conditionalArmMetaWrap_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrOutput, DRR_GOLDEN_DIR, DRR_CSA_UATPI_LEG2);
    }

    @Test
    @EnabledIf("cellsAvailable")
    void originalSwapUti_multiElementWiseDeref_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrOutput, DRR_GOLDEN_DIR, DRR_ORIGINAL_SWAP_UTI);
    }

    // ==== Green-safety pins (pass on clean source AND with the facets) ====

    @Test
    @EnabledIf("cellsAvailable")
    void isoAsicClearingPartyAndTime22_nonConfiguredKeepsNullify_byteMatchesGolden()
            throws IOException {
        // NOT in the doNotPrune config — the nullify prune + .hasData() recursion stay.
        assertByteMatchesGolden(isoOutput, ISO_GOLDEN_DIR, ISO_ASIC_CPT22_GREEN);
        assertTrue(isoOutput.get(ISO_ASIC_CPT22_GREEN).contains(".prune().hasData()"),
                "the non-configured asic variant must keep the NULLIFY prune form");
    }

    @Test
    @EnabledIf("cellsAvailable")
    void fpmlBullionPhysicalLeg_noCollisionParamsUnescaped_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(fpmlOutput, FPML_GOLDEN_DIR, FPML_BULLION_PHYSICAL_LEG_GREEN);
    }

    @Test
    @EnabledIf("cellsAvailable")
    void spreadOfLeg1Notation_singleRungBlockTopDrain_byteMatchesGolden() throws IOException {
        // The #315 single-rung carrier: the per-rung drain degenerates to the block-top
        // position — byte-identical under CP3.
        assertByteMatchesGolden(drrOutput, DRR_GOLDEN_DIR, DRR_SPREAD_LEG1_GREEN);
    }

    // ==== helpers ====

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
