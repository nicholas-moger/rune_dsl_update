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
 * PR #299 — TWO disjoint green-safe GENERATOR mechanisms, 8 drr POJO Rule byte flips.
 *
 * <p><b>(A) sumTypedMethod (4 flips).</b> {@code MapperC} has NO generic {@code sum()} member — only
 * the typed {@code sum«ItemType»()} variants ({@code sumBigDecimal} / {@code sumInteger} /
 * {@code sumLong} / {@code sumBigInteger}; upstream {@code ExpressionGenerator.caseSumOperation} renders
 * {@code "sum" + itemType.simpleName} via the WRAPPING {@code buildListOperationNoBody}). The fork's
 * inline {@code .sum()} form referenced a non-existent method, so EVERY SUM emission was a non-compiling
 * waivered mismatch. {@code CollectionHandler.handle(RListOpExpr)} now resolves the typed method from the
 * argument's inferred element type, RULE-SCOPED + not-inside-lambda + numeric-type gated (the three gates
 * were each verified load-bearing by the same-session regscan: rule-scope keeps the function-body sums
 * inline — co-occupied with the then-hoist facet; not-inside-lambda keeps a {@code mapSingleToItem}
 * lambda-ternary sum inline — co-occupied with block-lambda; numeric-type declines an Object-erased type
 * that would dispatch to a non-existent {@code sumObject()}). Carriers: CommodityOptionNotional,
 * CommodityTotalNotionalQuantity, FixedPriceTotalNotionalQuantity, OptionTotalNotionalQuantity (iosco cde).
 *
 * <p><b>(B) bareEnumQualifyRBodyShadow (4 flips).</b> A bare enum value ({@code CFTC} / {@code ISDA} /
 * {@code HKMA}) used as a filter-comparison operand ({@code filter regimeInformation -> supervisoryBody
 * any = CFTC}) binds to a {@code body Authority CFTC} regulatory-body declaration (an {@code RBody}) — a
 * NAME COLLISION with {@code SupervisoryBodyEnum.CFTC} — so the fork rendered the bare RBody name
 * {@code MapperS.of(CFTC)} (a non-compiling undefined symbol). Golden qualifies
 * {@code MapperS.of(SupervisoryBodyEnum.CFTC)}. TWO coordinated changes:
 * {@code HandlerHelper.typeShadowEnumValueName} admits the RBody-shadow (the #239 type-shadow precedent),
 * and the sibling-enum recovery is extended to the two shapes the failing carriers use —
 * {@code NavigationHandler.disguisedSiblingEnumeration} reads the already-resolved
 * {@code resolvedAttributeChain} leaf (a disguised {@code regimeInformation -> supervisoryBody} nav), and
 * the new {@code NavigationHandler.siblingAttributeEnumeration} recovers a bare {@code RSymbolReference}
 * resolving directly to an enum-typed {@code RAttribute} (kept SEPARATE from
 * {@code siblingComparandEnumeration}, which is shared with {@code ConversionHandler}'s green source
 * detection). This is the deferred #298 (B) completed with the meta-annotated-sibling-leaf-enum recovery
 * arm. Carriers: AffiliatedCounterparty (cftc), PortfolioContaining (cftc / hkma), DTCC_ProductID (common).
 *
 * <p><b>Green-safety (regscan: 8 flipped-out / 0 within-waiver regressions / 1 toward-golden / 6 neutral;
 * ALL FUNCTION cells + cdm/iso/fpml POJO byte-IDENTICAL).</b> For (A): no green file carries the fork's
 * {@code .sum()} (MapperC has no such member), and the three gates keep every co-occupied / function /
 * Object-erased sum on the inline form. For (B): the bare RBody name {@code MapperS.of(CFTC)} never
 * compiled, and the value-name match against the recovered sibling enum is the load-bearing gate (a
 * genuine regulatory-body reference recovers no matching enum value and declines). byte-oracle /
 * stash-baseline measured exactly 8 flips (drr POJO 399 → 391; clean source = 12 stale waivers,
 * {@code comm -23} = exactly these 8 carriers).
 *
 * <p>Anchors are WHOLE-FILE byte comparisons of the generated drr Rule output against the frozen
 * goldens (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}. REVERT-VERIFIED RED for the 8 flip locks.
 */
class RuleSumTypedBareEnumQualifyTest {

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

    // ==== (A) sumTypedMethod flip locks (revert-RED): a rule-body-terminal sum over a numeric
    //      MapperC renders the typed sum«ItemType»() wrap, not the non-existent .sum(). ====

    /**
     * Flip: {@code CommodityOptionNotional} (iosco cde) — the rule body multiplies a strike price by
     * a quantity sum; golden renders the second operand's {@code …<BigDecimal>map("getValue", …)
     * .sumBigDecimal()}, the fork emitted {@code .sum()} (no such MapperC member).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void commodityOptionNotional_sumBigDecimal_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/standards/iosco/cde/base/quantity/reports/CommodityOptionNotionalRule.java");
    }

    /** Flip: {@code CommodityTotalNotionalQuantity} (iosco cde) — a BigDecimal-element quantity sum. */
    @Test
    @EnabledIf("drrCellAvailable")
    void commodityTotalNotionalQuantity_sumBigDecimal_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/standards/iosco/cde/base/quantity/reports/CommodityTotalNotionalQuantityRule.java");
    }

    /** Flip: {@code FixedPriceTotalNotionalQuantity} (iosco cde) — a BigDecimal-element quantity sum. */
    @Test
    @EnabledIf("drrCellAvailable")
    void fixedPriceTotalNotionalQuantity_sumBigDecimal_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/standards/iosco/cde/base/quantity/reports/FixedPriceTotalNotionalQuantityRule.java");
    }

    /** Flip: {@code OptionTotalNotionalQuantity} (iosco cde) — a BigDecimal-element quantity sum. */
    @Test
    @EnabledIf("drrCellAvailable")
    void optionTotalNotionalQuantity_sumBigDecimal_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/standards/iosco/cde/base/quantity/reports/OptionTotalNotionalQuantityRule.java");
    }

    // ==== (B) bareEnumQualifyRBodyShadow flip locks (revert-RED): a bare enum value that binds to an
    //      RBody (name collision) qualifies EnumType.CONSTANT via the sibling-enum recovery. ====

    /**
     * Flip: {@code AffiliatedCounterparty} (cftc) — {@code filter regimeInformation -> supervisoryBody
     * any = CFTC} where the sibling is a disguised {@code REnumValueRef} nav (resolvedAttributeChain
     * present); golden qualifies {@code MapperS.of(SupervisoryBodyEnum.CFTC)}, the fork emitted the bare
     * RBody name {@code MapperS.of(CFTC)}.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void affiliatedCounterpartyCftc_bareEnumRBodyShadow_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/cftc/rewrite/margin/reports/AffiliatedCounterpartyForMarginAndCapitalIndicatorRule.java");
    }

    /**
     * Flip: {@code PortfolioContaining} (cftc) — a bare {@code RSymbolReference -> RAttribute}
     * ({@code supervisoryBody}) sibling recovered via the new {@code siblingAttributeEnumeration}.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void portfolioContainingCftc_bareEnumRBodyShadow_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/cftc/rewrite/margin/reports/PortfolioContainingNonReportableComponentIndicatorRule.java");
    }

    /** Flip: {@code PortfolioContaining} (hkma) — the same RAttribute-sibling shape, HKMA regime. */
    @Test
    @EnabledIf("drrCellAvailable")
    void portfolioContainingHkma_bareEnumRBodyShadow_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/hkma/rewrite/margin/reports/PortfolioContainingNonReportableComponentIndicatorRule.java");
    }

    /**
     * Flip: {@code DTCC_ProductID} (common) — {@code ISDA} binds to an RBody; the sibling {@code source}
     * (an RAttribute typed {@code TaxonomySourceEnum}) qualifies {@code TaxonomySourceEnum.ISDA} — a
     * DIFFERENT enum than the SupervisoryBodyEnum carriers, proving per-carrier sibling recovery.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void dtccProductIdCommon_bareEnumRBodyShadow_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/common/dtcc/reports/DTCC_ProductIDRule.java");
    }

    // ==== Green-safety / decline locks ====

    /**
     * GRADUATED decline lock (the #329/#380 law) — the not-inside-lambda gate (A) deferred
     * {@code TotalNotionalQuantityOfLeg2} (esma) to the block-conversion facet, which landed at
     * PR #383 (facet ruleLadderPlainArmChainDecomp + sumBlockArmSeat): the mapSingleToItem
     * ladder block-converts (numbered {@code boolean0/1} hoists, the nested inner ladder, the
     * plain in-rung {@code final MapperS<ReferenceWithMetaNonNegativeQuantitySchedule} thenArg}
     * hoist) and the arm's sum — now at a {@code return} statement seat via the #354
     * block-arm-seat channel — renders golden's {@code .sumBigDecimal()} continuation. The
     * file is byte-identical to golden (0×{@code .sum()} / 1×{@code .sumBigDecimal()}).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void totalNotionalQuantityOfLeg2_blockConverted_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/esma/emir/refit/trade/reports/TotalNotionalQuantityOfLeg2Rule.java");
    }

    /**
     * Green-safety / value-name-match lock (B) — the bare-enum recovery resolves the CORRECT sibling
     * enum per carrier (not a hardcoded one): {@code AffiliatedCounterparty} qualifies
     * {@code SupervisoryBodyEnum.CFTC} (from the {@code supervisoryBody} sibling) while
     * {@code DTCC_ProductID} qualifies {@code TaxonomySourceEnum.ISDA} (from the {@code source} sibling).
     * The value-name match against the recovered enum is the gate that keeps a genuine non-matching
     * RBody reference from being rewritten.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void bareEnum_qualifiesCorrectSiblingEnumPerCarrier() throws IOException {
        assertNotNull(drrOutput, "drr generation did not run — corpus unavailable?");
        String affiliated = drrOutput.get(
                "drr/regulation/cftc/rewrite/margin/reports/AffiliatedCounterpartyForMarginAndCapitalIndicatorRule.java");
        assertNotNull(affiliated, "AffiliatedCounterparty not generated");
        assertTrue(affiliated.contains("MapperS.of(SupervisoryBodyEnum.CFTC)"),
                "AffiliatedCounterparty must qualify SupervisoryBodyEnum.CFTC (the supervisoryBody sibling enum)");
        String dtcc = drrOutput.get("drr/regulation/common/dtcc/reports/DTCC_ProductIDRule.java");
        assertNotNull(dtcc, "DTCC_ProductID not generated");
        assertTrue(dtcc.contains("TaxonomySourceEnum.ISDA"),
                "DTCC_ProductID must qualify TaxonomySourceEnum.ISDA (the source sibling enum)");
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
                + path + " (PR #299 sumTypedMethod + bareEnumQualifyRBodyShadow).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
