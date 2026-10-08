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
 * PR #340 anchor — the four-facet compose (12 flips: 10 drr POJO Rule + 1 drr FUNCTION +
 * 1 cdm6 FUNCTION).
 *
 * <p><b>Facet 1 — {@code enumSingletonListCondArm}</b> (7 POJO flips, the UTI family): the
 * #312/#319 conditional-arm exclusion on the #269 enum-into-multi-param singletonList
 * coercion CLOSED at the effective-else block seat — the FIFTH deferred-decline-lock flip
 * in a row (the #319 {@code RuleEnumSingletonListLambdaTest} asic decline lock named the
 * missing per-arm drain capability, which PR #339 built). The identity-keyed
 * blessed-conditional handshake ({@code pushEnumConstArgDrainableCond}, the #327/#339
 * single-slot pattern) admits EXACTLY the args the compiling conditional directly owns
 * ({@code isDirectlyInBlessedConditional} — a nested conditional/lambda declines, the
 * #219/#250 cascade guard); the NAMED {@code ReferenceHandler.EnumConstArgHoist} pending
 * (the DeepThenArgHoist pattern) drains cond-position BEFORE the {@code if (} (the new
 * render slot) and arm-position inside the owning branch; two occurrences number
 * {@code supervisoryBodyEnum0/1} per the #170/#333 same-scope collision-group law.
 *
 * <p><b>Facet 2 — {@code ruleOutputGetValueDeref}</b> (3 POJO flips): two whole-output
 * meta-deref recovery gaps at the rule output-assignment seat ({@code final
 * FieldWithMetaString fieldWithMetaString = <rhs>; if (…null…) output = null; else output
 * = ….getValue();}) — (a) the #290 collapse-over-pipe channel reaches the chain-BASE
 * producer ({@code extract [<nav -> metaLeaf>] then last}, n == 1 — esma/fca
 * CollateralPortfolioCode; the #297/#320 UTI/USI conditional-base family stays declined by
 * the walker's nav-only-ness), and (b) the #333 conditionalOutputMeta pipe-blanket gains
 * the provable-single carve-out (a piped arm whose LAST body is a single-card leaf fed by
 * a bare SINGLE-output invokable — asic UnderlyingIdOther arm 1).
 *
 * <p><b>Facet 3 — {@code filterPredicateBooleanNavGet}</b> (1 cdm6 FUNCTION flip): a
 * boolean-NAVIGATION filter-predicate body is a {@code Mapper<Boolean>}, not the Boolean
 * the {@code filter*NullSafe} signature demands — coerced {@code .get()} like the four
 * sibling arms; the type-ERASED disguised 2-name chain resolves through a LOCAL #282-style
 * item-type re-root (filter element → head → leaf), with a rendered-shape belt against
 * double-append.
 *
 * <p><b>Facet 4 — {@code functionImplicitItemArgMeta}</b> (1 drr FUNCTION flip): the
 * #144/#285 implicit-arg meta-deref reaches the FUNCTION ctor-extract seat
 * (BarrierFromTriggerEvent's {@code <nav -> currency> extract ConvertNonISOToISOCurrency})
 * — the #285 walk went path-agnostic while its original tryMetaDerefArg call site keeps
 * the rule scope verbatim; three belts (drainable-lambda directness, an open scope, the
 * statement-direct ancestor walk) each encode a cp4 over-fire catch.
 *
 * <p>Every pre-fix form was NON_COMPILING (a bare enum where a {@code List} is demanded; a
 * meta wrapper assigned/spliced where the bare value is demanded; a {@code Mapper<Boolean>}
 * where a {@code Boolean} is demanded) — no green file carries any pre-fix form. Whole-file
 * byte comparisons run through the REAL D11 generation paths and revert RED without the
 * facets.
 */
class EnumCondArmGetValueComposeTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    // Facet 1 flip carriers (drr POJO Rule — the UTI margin/valuation family).
    private static final String UTI_ASIC_MARGIN =
            "drr/regulation/asic/rewrite/margin/reports/ASICUniqueTransactionIdentifierRule.java";
    private static final String UTI_ASIC_VALUATION =
            "drr/regulation/asic/rewrite/valuation/reports/ASICUniqueTransactionIdentifierRule.java";
    private static final String UTI_CFTC_MARGIN =
            "drr/regulation/cftc/rewrite/margin/reports/UniqueTransactionIdentifierRule.java";
    private static final String UTI_CFTC_VALUATION =
            "drr/regulation/cftc/rewrite/valuation/reports/UniqueTransactionIdentifierRule.java";
    private static final String UTI_HKMA_MARGIN =
            "drr/regulation/hkma/rewrite/margin/reports/HKMAUniqueTransactionIdentifierRule.java";
    private static final String UTI_MAS_MARGIN =
            "drr/regulation/mas/rewrite/margin/reports/MASUniqueTransactionIdentifierRule.java";
    private static final String UTI_MAS_VALUATION =
            "drr/regulation/mas/rewrite/valuation/reports/MASUniqueTransactionIdentifierRule.java";
    // Facet 2 flip carriers (drr POJO Rule).
    private static final String UNDERLYING_ID_OTHER_ASIC =
            "drr/regulation/asic/rewrite/trade/reports/UnderlyingIdOtherRule.java";
    private static final String COLLATERAL_PORTFOLIO_CODE_ESMA =
            "drr/regulation/esma/emir/refit/margin/reports/CollateralPortfolioCodeRule.java";
    private static final String COLLATERAL_PORTFOLIO_CODE_FCA =
            "drr/regulation/fca/ukemir/refit/margin/reports/CollateralPortfolioCodeRule.java";
    // Facet 3 flip carrier (cdm6 FUNCTION).
    private static final String CHECK_ELIGIBILITY_CDM6 =
            "cdm/product/collateral/functions/CheckEligibilityByDetails.java";
    // Facet 4 flip carrier (drr FUNCTION).
    private static final String BARRIER_FROM_TRIGGER_EVENT =
            "drr/regulation/common/trade/price/functions/BarrierFromTriggerEvent.java";
    // Facet 2 partial-heal lock — the deref fires on the co-occupied hkma trade file too
    // (dl 28 → 23, regscan-verified TOWARD): the whole-output getValue block lands in
    // golden's exact form; the residual is the deferred base-conditional facet.
    private static final String UNDERLIER_ID_OTHER_HKMA =
            "drr/regulation/hkma/rewrite/trade/reports/UnderlierIdOtherRule.java";
    // Facet 4 decline lock — an OPERAND-seat bare-fn lambda (an areEqual/logical chain
    // condition) must keep its flat pre-#340 form: the #257 double-render machinery
    // re-renders the seat, where a wrapper-local identifier lands in a finalized scope
    // and degrades the whole assignOutput to the TODO comment form (the cp4 catch,
    // dl 5 → 36 AWAY before the statement-direct gate).
    private static final String QUALIFY_ASSET_CLASS_EQUITY_CDM6 =
            "cdm/product/qualification/functions/Qualify_AssetClass_Equity.java";
    // Facet 2 deferral breadcrumb — the sized Group-C sibling: golden hoists
    // `final FieldWithMetaString fieldWithMetaString2/3 = <chain>.get();` as ladder-arm
    // STATEMENTS (the #276 levelHoists channel) with hoistSession numbering; the fork
    // still renders the collapsed inline `.get().getValue().get()` arg. A SEPARATE
    // mechanism (ladder-arm statement hoists + session numbering) — returned to the
    // near-flip pool at #340 scope; when that facet lands, this lock converts.
    private static final String EXCHANGE_RATE_BASIS_CDE =
            "drr/standards/iosco/cde/version1/price/reports/ExchangeRateBasisRule.java";

    private static Map<String, String> drrFnOutput;
    private static Map<String, String> drrRuleOutput;
    private static Map<String, String> cdm6FnOutput;

    static boolean drrCellAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR);
    }

    static boolean cdm6CellAvailable() {
        return Files.isDirectory(CDM6_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM6_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (drrCellAvailable()) {
            drrFnOutput = generateFunctions(
                    new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT));
            drrRuleOutput = generateRuleKinds(
                    new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT));
        }
        if (cdm6CellAvailable()) {
            cdm6FnOutput = generateFunctions(
                    new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CDM6_CELL_ROOT));
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

    // ==== facet 1 flip locks (revert-RED) ====

    @Test
    @EnabledIf("drrCellAvailable")
    void utiAsicMargin_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrRuleOutput, DRR_GOLDEN_DIR, UTI_ASIC_MARGIN);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void utiAsicValuation_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrRuleOutput, DRR_GOLDEN_DIR, UTI_ASIC_VALUATION);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void utiCftcMargin_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrRuleOutput, DRR_GOLDEN_DIR, UTI_CFTC_MARGIN);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void utiCftcValuation_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrRuleOutput, DRR_GOLDEN_DIR, UTI_CFTC_VALUATION);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void utiHkmaMargin_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrRuleOutput, DRR_GOLDEN_DIR, UTI_HKMA_MARGIN);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void utiMasMargin_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrRuleOutput, DRR_GOLDEN_DIR, UTI_MAS_MARGIN);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void utiMasValuation_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrRuleOutput, DRR_GOLDEN_DIR, UTI_MAS_VALUATION);
    }

    /**
     * Positive-content lock (revert-RED): the cftc margin UTI carries the numbered
     * per-occurrence hoists ({@code supervisoryBodyEnum0} before the {@code if (},
     * {@code supervisoryBodyEnum1} inside the then-arm — the #170/#333 collision-group
     * law), the null-guarded singletonList coercions, and the {@code java.util.Collections}
     * import.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void utiCftcMargin_enumHoistedPerOccurrenceAndCoerced() {
        String gen = gen(drrRuleOutput, UTI_CFTC_MARGIN);
        assertTrue(gen.contains("import java.util.Collections;"),
                "Expected the java.util.Collections import");
        assertTrue(gen.contains("final SupervisoryBodyEnum supervisoryBodyEnum0 = SupervisoryBodyEnum.CFTC;"),
                "Expected the cond-position hoist (numbered 0) before the if");
        assertTrue(gen.contains("final SupervisoryBodyEnum supervisoryBodyEnum1 = SupervisoryBodyEnum.CFTC;"),
                "Expected the then-arm hoist (numbered 1) inside the branch");
        assertTrue(gen.contains("(supervisoryBodyEnum0 == null ? Collections.<SupervisoryBodyEnum>emptyList()"
                        + " : Collections.singletonList(supervisoryBodyEnum0))"),
                "Expected the cond-position singletonList coercion");
    }

    // ==== facet 2 flip locks (revert-RED) ====

    @Test
    @EnabledIf("drrCellAvailable")
    void underlyingIdOtherAsic_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrRuleOutput, DRR_GOLDEN_DIR, UNDERLYING_ID_OTHER_ASIC);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void collateralPortfolioCodeEsma_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrRuleOutput, DRR_GOLDEN_DIR, COLLATERAL_PORTFOLIO_CODE_ESMA);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void collateralPortfolioCodeFca_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrRuleOutput, DRR_GOLDEN_DIR, COLLATERAL_PORTFOLIO_CODE_FCA);
    }

    /**
     * GRADUATED at PR #385 (this lock's javadoc named the residual as "the deferred
     * base-conditional facet" — the anticipated-seat law): the facet
     * {@code nestedThenIteLadder} landed exactly that base-conditional as golden's
     * NESTED {@code ifThenElseResult} if/else, so the whole-output wrapper hoist now
     * re-roots on {@code ifThenElseResult} (the original gen-side {@code thenArg1}
     * receiver pin), and the file byte-matches golden (the #329/#380 converting-lock
     * graduation law). The null-guarded getValue block this lock pinned survives
     * verbatim inside the byte lock.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void underlierIdOtherHkma_nestedLadderConverted_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrRuleOutput, DRR_GOLDEN_DIR, UNDERLIER_ID_OTHER_HKMA);
    }

    // ==== facet 3 flip lock (revert-RED) ====

    @Test
    @EnabledIf("cdm6CellAvailable")
    void checkEligibilityByDetailsCdm6_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, CHECK_ELIGIBILITY_CDM6);
    }

    // ==== facet 4 flip lock (revert-RED) ====

    @Test
    @EnabledIf("drrCellAvailable")
    void barrierFromTriggerEvent_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFnOutput, DRR_GOLDEN_DIR, BARRIER_FROM_TRIGGER_EVENT);
    }

    // ==== decline locks (pass on clean source too) ====

    /**
     * Facet 4 green-safety (operand-seat NON-degradation): the {@code Qualify_AssetClass_Equity}
     * bare-fn lambda sits in an areEqual/logical OPERAND chain — the #257 double-render
     * class where the implicit-arg meta hoist degraded the whole {@code assignOutput} to
     * the TODO comment form (the #340 cp4 catch). At #340 the statement-direct ancestor
     * gate DECLINED the operand class; PR #346's itemGetMetaDerefBlock converted that gate
     * to a ROUTE selector — the operand class now fires via the sentinel LAMBDA channel
     * (the deferred-token decl that never getActualName()-closes the ancestor scopes
     * mid-statement) and this file byte-flipped. The lock held across BOTH states exactly
     * as designed — witness-unique: the degradation marker counts 0 in every golden AND
     * in every correct gen, and the statement-level {@code referenceWithMetaObservable0}
     * hoists survive in golden's flipped form too.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void qualifyAssetClassEquity_operandSeatNotDegraded() {
        String gen = gen(cdm6FnOutput, QUALIFY_ASSET_CLASS_EQUITY_CDM6);
        assertTrue(!gen.contains("Cannot create a new identifier"),
                "The operand-seat bare-fn lambda must NOT degrade to the closed-scope TODO form");
        assertTrue(gen.contains("final ReferenceWithMetaObservable referenceWithMetaObservable0"),
                "The statement-level hoists must survive (golden's flipped form carries them)");
    }

    /**
     * CONVERTED at PR #360 (the deferred-carrier law — the breadcrumb's own javadoc named
     * the conversion): facet evaluateArgExpectedTypeReset compiles call args NEUTRAL so
     * the meta-wrapper arg reaches tryMetaDerefArg at the ite-hoist arm seat, and
     * armHoistPullSet admits the FieldWithMeta decl class into the branch — the
     * {@code fieldWithMetaString2/3} ladder-arm statement hoists now render with
     * hoistSession numbering and the collapsed inline {@code .getValue().get())} arg
     * vanishes (the breadcrumb's witness: golden carries ZERO such collapses).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void exchangeRateBasis_ladderArmHoistFlipped() throws IOException {
        String gen = gen(drrRuleOutput, EXCHANGE_RATE_BASIS_CDE);
        assertTrue(!gen.contains(".getValue().get())"),
                "The collapsed inline arg must vanish on the legit flip (the breadcrumb witness)");
        assertTrue(gen.contains("final FieldWithMetaString fieldWithMetaString2"),
                "The ladder-arm statement hoists must render inside the branch");
        assertByteMatchesGolden(drrRuleOutput, DRR_GOLDEN_DIR, EXCHANGE_RATE_BASIS_CDE);
    }

    private static String gen(Map<String, String> output, String path) {
        assertNotNull(output, "generation did not run — corpus unavailable?");
        String g = output.get(path);
        assertNotNull(g, "Class not generated: " + path);
        return g;
    }

    private static void assertByteMatchesGolden(Map<String, String> output, Path goldenDir,
            String path) throws IOException {
        assertNotNull(output, "generation did not run — corpus unavailable?");
        String generated = output.get(path);
        assertNotNull(generated, "Class not generated: " + path
                + " (emission failed or the path differs)");
        Path goldenPath = goldenDir.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated output must byte-match the golden (newline-normalized) for "
                + path + " (PR #340 the four-facet compose).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
