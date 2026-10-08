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
 * PR #338 anchor — the spine-and-ladder compose (5 flips: 3 drr FUNCTION + 2 drr POJO Rule).
 *
 * <p><b>Facet 1 — {@code thenArgCollapseMetaPreserve}</b> (the Existing*
 * {@code .first()}-preservation trio): the fork eagerly stripped the meta wrapper element at
 * then-arg collapse seats where golden preserves it in the declaration and derefs lazily at the
 * next VALUE navigation. Four arms: the #204/#144 RE-KEY (an Object-erased alias-headed chain
 * re-runs the unique-wrapper scan on the recovered bare element), the #297 BASE-FILTER anchor
 * ({@code k == 1} + {@code RFilterExpr} base — the {@code pj < 0} UTI/USI over-fire guard is
 * preserved: their bases are conditional lambdas, not filters), the Object-relaxed value-type
 * guard (prevRef's own declaration is render-truth), and the last-then EXTRACT item reading its
 * OWNING pipe's then-arg declaration via an EXACT {@code thenArgRefFor} lookup (no skip-walk;
 * gated resolvedFeature-present + MapperS-single + meta element).
 *
 * <p><b>Facet 2 — {@code ladderBareCalleeMixedJoin}</b> ({@code UnderlierIDOtherLeg1/2Rule}):
 * the #295 mixed-baresym ladder pre-scan widened by {@code ladderArmLeafMetaKind} to two
 * declared-output-provable shapes — a bare FUNCTION-CALL arm ({@code GetOtherUnderlierLeg1},
 * plain string output = bare-join evidence) and a feature nav over an element-preserving
 * collapse of one ({@code GetUnderlierProductIdentifierLeg1 first -> identifier} = meta-leaf
 * evidence). The mixed ladder joins bare: the meta arm derefs in-arm and the typed empty takes
 * {@code MapperS.<String>ofNull()}. SUPERSEDES the #334 UnderlierIDOtherLeg1 no-bare-sibling
 * DECLINE lock (the bare sibling existed — it just was not provable then).
 *
 * <p>All five pre-fix forms were NON_COMPILING (generics mismatch / javac lambda-join inference
 * cascade) per the per-carrier compile-gate verdicts; no green golden carries any pre-fix form.
 * Whole-file byte comparisons run through the REAL D11 generation paths and revert RED without
 * the facets.
 */
class ThenArgMetaPreserveLadderJoinTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    // Facet 1 flip carriers — the Existing* trio (drr FUNCTION).
    private static final String EXISTING_ISIN =
            "drr/enrichment/upi/functions/ExistingIsin.java";
    private static final String EXISTING_OTC_ISIN =
            "drr/enrichment/upi/functions/Existing_OtcIsin.java";
    private static final String EXISTING_UPI =
            "drr/enrichment/upi/functions/Existing_Upi.java";
    // Facet 2 flip carriers — the bare-callee mixed ladder (drr POJO Rule).
    private static final String UNDERLIER_ID_OTHER_LEG1 =
            "drr/standards/iosco/cde/version3/underlier/reports/UnderlierIDOtherLeg1Rule.java";
    private static final String UNDERLIER_ID_OTHER_LEG2 =
            "drr/standards/iosco/cde/version3/underlier/reports/UnderlierIDOtherLeg2Rule.java";
    // Facet 2 partial-heal lock — the widened mixed verdict lets the EXISTING #295 machinery
    // fire the TradeForEvent->tradeDate baresym arm (rule-evaluate + FieldWithMetaDate witness
    // + deref, golden's exact line); the file stays divergent on its then-chain-expansion
    // residual (NOT this PR's mechanism).
    private static final String CDE_V1_EFFECTIVE_DATE =
            "drr/standards/iosco/cde/version1/datetime/reports/EffectiveDateRule.java";
    // Facet 1 decline lock — a then-item nav of the WRAPPER-level `scheme` metafield resolves
    // NO feature (resolvedFeature EMPTY), so arm (iv) must NOT retype/deref (the checkpoint
    // over-fire catch).
    private static final String HKMA_SCHEME_NAME =
            "drr/regulation/hkma/rewrite/trade/functions/Extract_HKMASchemeName.java";
    // Facet 1 decline lock — the nav's implicit item belongs to a nested in-lambda then whose
    // pipe lands on an UNBOUND lambda fn (no thenArgRefFor), so arm (iv) must NOT read the
    // OUTER then-arg's decl (the second checkpoint over-fire catch).
    private static final String COUNTRY_AND_PROVINCE =
            "drr/regulation/csa/rewrite/trade/reports/CountryAndProvinceOrTerritoryOfIndividualRule.java";

    private static Map<String, String> drrFnOutput;
    private static Map<String, String> drrRuleOutput;

    static boolean cellsAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (cellsAvailable()) {
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

    // ==== facet 1 flip locks (revert-RED) ====

    @Test
    @EnabledIf("cellsAvailable")
    void existingIsin_thenArgCollapseMetaPreserve_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFnOutput, EXISTING_ISIN);
    }

    @Test
    @EnabledIf("cellsAvailable")
    void existingOtcIsin_thenArgCollapseMetaPreserve_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFnOutput, EXISTING_OTC_ISIN);
    }

    @Test
    @EnabledIf("cellsAvailable")
    void existingUpi_thenArgCollapseMetaPreserve_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFnOutput, EXISTING_UPI);
    }

    // ==== facet 2 flip locks (revert-RED; supersede the #334 decline lock) ====

    @Test
    @EnabledIf("cellsAvailable")
    void underlierIdOtherLeg1_ladderBareCalleeMixedJoin_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrRuleOutput, UNDERLIER_ID_OTHER_LEG1);
    }

    @Test
    @EnabledIf("cellsAvailable")
    void underlierIdOtherLeg2_ladderBareCalleeMixedJoin_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrRuleOutput, UNDERLIER_ID_OTHER_LEG2);
    }

    // ==== facet 2 partial-heal lock ====

    /**
     * The cde v1 datetime EffectiveDateRule ladder gains widened bare evidence, so the EXISTING
     * #295 machinery fires its {@code TradeForEvent -> tradeDate} baresym meta arm: gen carries
     * golden's exact {@code tradeForEvent.evaluate(item.get())} rule-invocation with the
     * {@code FieldWithMetaDate} witness and in-arm deref. CONVERTED at PR #359 (facet
     * dateTimeRecordFeatureNav — the #330/#357/#358 decline-pin conversion precedent): the
     * then-chain-expansion residual this lock predicted was the {@code PositionForEvent ->
     * openDateTime -> date} getter-form record nav, and the F-4 bare-INVOKABLE-head arm on
     * resolveReceiverRType landed golden's {@code .<Date>map("Date", dt ->
     * Date.of(dt.toLocalDate()))} — the file is now BYTE-IDENTICAL (this lock's own javadoc
     * named the residual; the deferred-carrier law paid).
     */
    @Test
    @EnabledIf("cellsAvailable")
    void cdeV1EffectiveDate_baresymArmHealed_byteMatchesGolden() throws IOException {
        String gen = generated(drrRuleOutput, CDE_V1_EFFECTIVE_DATE);
        assertTrue(gen.contains(
                "MapperS.of(tradeForEvent.evaluate(item.get())).<FieldWithMetaDate>map(\"getTradeDate\""),
                "the widened mixed verdict must fire the #295 machinery on the baresym arm");
        assertByteMatchesGolden(drrRuleOutput, CDE_V1_EFFECTIVE_DATE);
    }

    // ==== facet 1 decline locks (the checkpoint over-fire catches) ====

    /**
     * Extract_HKMASchemeName's then-item navigates the WRAPPER-level {@code scheme} metafield —
     * resolvedFeature is EMPTY, so arm (iv) must not retype the item and no value deref may be
     * inserted before {@code getScheme}. Witness-unique: the bad-fire token
     * {@code getValue()).map("getScheme"} counts 0 in gen AND golden, so a future legitimate
     * flip keeps this lock green. Pin MOVED at PR #348 (facet metaPathShortForm): the scheme
     * read now renders upstream's a->a short form on the UNCHANGED wrapper item
     * ({@code item.map("getMeta", a->a.getMeta()).map("getScheme", …)} — golden's exact
     * fragment, 5/5 seats), which still proves arm (iv) declined: the receiver keeps the
     * wrapper, and the no-deref negative below is unchanged.
     */
    @Test
    @EnabledIf("cellsAvailable")
    void hkmaSchemeName_metafieldNav_armIvDeclines() {
        String gen = generated(drrFnOutput, HKMA_SCHEME_NAME);
        assertTrue(gen.contains("item.map(\"getMeta\", a->a.getMeta()).map(\"getScheme\""),
                "the wrapper-level scheme nav must keep the wrapper receiver (the #348 "
                + "a->a short form — no arm-(iv) retype)");
        assertFalse(gen.contains("getValue()).map(\"getScheme\""),
                "arm (iv) must NOT deref a then-item ahead of a metafield nav");
    }

    /**
     * CountryAndProvinceOrTerritoryOfIndividualRule's {@code country}/{@code state} navs belong
     * to a nested in-lambda then whose pipe fn carries NO thenArgRef — arm (iv)'s EXACT lookup
     * must decline rather than read the OUTER {@code MapperS<ReferenceWithMetaParty>} decl.
     * Witness-unique: the bad-fire token {@code .<Party>map("Type coercion"} counts 0 in gen AND
     * golden.
     */
    @Test
    @EnabledIf("cellsAvailable")
    void countryAndProvince_innerPipeItem_armIvDeclines() {
        String gen = generated(drrRuleOutput, COUNTRY_AND_PROVINCE);
        assertTrue(gen.contains("_item.<FieldWithMetaString>map(\"getCountry\""),
                "the inner-lambda item nav must keep its own element typing");
        assertFalse(gen.contains(".<Party>map(\"Type coercion\""),
                "arm (iv) must NOT stamp the outer then-arg's element onto a nested pipe's item");
    }

    // ==== helpers ====

    private String generated(Map<String, String> output, String path) {
        assertNotNull(output, "cell output not generated");
        String gen = output.get(path);
        assertNotNull(gen, "missing generated output: " + path);
        return gen;
    }

    private String golden(String path) throws IOException {
        Path goldenPath = DRR_GOLDEN_DIR.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "missing golden: " + goldenPath);
        return Files.readString(goldenPath);
    }

    private void assertByteMatchesGolden(Map<String, String> output, String path)
            throws IOException {
        assertEquals(normalize(golden(path)), normalize(generated(output, path)),
                path + " must byte-match the frozen 9.83.0 golden");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
