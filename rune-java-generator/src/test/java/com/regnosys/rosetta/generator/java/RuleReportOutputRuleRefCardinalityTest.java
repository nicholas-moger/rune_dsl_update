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
 * Anchor for facet {@code reportOutputRuleRefCardinality} (PR #318): a bare REPORTING-RULE reference
 * used as a navigation RECEIVER / extract body — {@code then extract cdeV3.payment.OtherPayment}, where
 * {@code cdeV3.payment.OtherPayment} is a MULTI-output rule — carries its OUTPUT cardinality. Unlike a
 * FUNCTION (whose declared output cardinality the frozen {@code CardinalityComputer.compute} reads), a
 * rule has no explicit output type; its cardinality comes from the body, which {@code compute}
 * (thenAware=false) reads SINGLE. {@code renderThenExtractSet} therefore declared the interior thenArg
 * {@code final MapperS<OtherPayment> thenArg1 = thenArg0.mapSingleToList(...)} (a type mismatch —
 * {@code mapSingleToList} returns a {@code MapperC}) and the downstream then chose
 * {@code thenArg1.mapSingleToItem(...)} (single receiver), where golden carries
 * {@code final MapperC<OtherPayment> thenArg1} + {@code thenArg1.mapItem(...)}.
 *
 * <p>The producer METHOD ({@code mapSingleToList}) was already correct — {@code CollectionHandler.isBodyMulti}
 * resolves the bare RRule ref as MULTI via the rule-aware {@code CardinalityComputer.computeRuleBody}
 * (#274) — but the DECL cardinality ({@code renderThenExtractSetImpl}'s {@code single} =
 * {@code compute(argExpr)==SINGLE && !chainProvesMulti}) read SINGLE because
 * {@code NavigationHandler.chainProvesMulti}'s {@code RSymbolReference} arm handled {@code RFunction}
 * (#293) but NOT a bare {@code RRule} reference. A new {@code RRule} arm (mirroring the #293
 * {@code RFunction} arm, but rule-aware via {@code computeRuleBody}) makes the DECL + downstream
 * cardinality agree with the already-multi METHOD. RULE-scoped ({@code findEnclosingRule}) →
 * FUNCTION-byte-neutral (#232; cdm5 79 / cdm6 232 / drr 206 FUNCTION mismatch UNCHANGED, cdm/iso/fpml
 * POJO byte-IDENTICAL). Monotone (add-only multi): {@code computeRuleBody} reads the referenced rule's
 * OWN body cardinality, never a false positive.
 *
 * <p>Green-safe by construction: a {@code MapperS<X> thenArg = <mapSingleToList producing MapperC>}
 * never compiled, so every carrier is an already-waivered NON_COMPILING mismatch; a green rule already
 * agrees with golden's rule-aware cardinality (its method + decl are consistent — see
 * {@code settlementCurrencyLeg1_singleRuleDelegation_staysGreen}). Compounds toward golden:
 * {@code OriginalSwapUTIRule} cftc/jfsa move their thenArg1 {@code MapperS<String>} → {@code MapperC<String>}
 * (matching golden's {@code MapperC} cardinality; they stay divergent on the harder whole-output
 * meta-deref facet, #288 disguised-chain territory).
 *
 * <p>Anchors are WHOLE-FILE byte comparisons of the generated drr output against the frozen goldens
 * (newline-normalized), generated through the REAL {@link D11CorpusRegressionTest#loadCellCorpusCached}.
 * REVERT-VERIFIED RED 3/4 — the 1 flip lock + 2 positive-content locks fail on clean source; the green-
 * safety decline lock ({@code SettlementCurrencyLeg1}) passes either way (its bare-rule-ref delegations
 * are all to SINGLE-output rules, so the arm declines and the plain {@code MapperS}/{@code mapSingleToItem}
 * form stays byte-matching golden).
 */
class RuleReportOutputRuleRefCardinalityTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    // Flip carrier: the interior thenArg1 (`then extract cdeV3.payment.OtherPayment`) is a MULTI-output
    // rule extract that the fork mis-typed SINGLE (decl + downstream method).
    private static final String OTHER_PAYMENT_P43 =
            "drr/regulation/cftc/rewrite/trade/reports/OtherPaymentP43Rule.java";
    // Compounding carrier: OriginalSwapUTI cftc — the cardinality lifts toward golden (MapperC) but the
    // file stays divergent on the whole-output meta-deref facet.
    private static final String ORIGINAL_SWAP_UTI_CFTC =
            "drr/regulation/cftc/rewrite/trade/reports/OriginalSwapUTIRule.java";
    // GREEN rule whose bare-rule-ref extract delegations are all to SINGLE-output rules
    // (ProductForEvent / SettlementTermsLeg1 / SettlementCurrency) — the RRule arm's computeRuleBody
    // reads SINGLE → declines → the plain MapperS/mapSingleToItem form stays, byte-matching golden.
    private static final String SETTLEMENT_CURRENCY_LEG1 =
            "drr/regulation/common/trade/execution/reports/SettlementCurrencyLeg1Rule.java";

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

    // ==== Flip lock (revert-RED): the carrier now byte-matches golden. ====

    @Test
    @EnabledIf("drrCellAvailable")
    void otherPaymentP43_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(OTHER_PAYMENT_P43);
    }

    // ==== Positive-content locks (revert-RED). ====

    /**
     * OtherPaymentP43: the interior thenArg1 (`then extract cdeV3.payment.OtherPayment`, a MULTI-output
     * rule) is declared {@code MapperC<OtherPayment>} and the downstream then uses {@code mapItem}
     * (multi receiver) — NOT the fork's {@code MapperS<OtherPayment>} + {@code mapSingleToItem}.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void otherPaymentP43_thenArgMapperCAndMapItem() {
        String gen = gen(OTHER_PAYMENT_P43);
        assertTrue(gen.contains("final MapperC<OtherPayment> thenArg1 = thenArg0"),
                "Expected the interior thenArg1 declared MapperC (multi rule-ref extract cardinality)");
        assertTrue(gen.contains(".mapItem(item -> MapperS.of(OtherPayment.builder()"),
                "Expected the downstream then to use mapItem (multi receiver)");
        assertTrue(!gen.contains("final MapperS<OtherPayment> thenArg1 = thenArg0"),
                "The fork's single MapperS<OtherPayment> thenArg1 decl must be gone");
        assertTrue(!gen.contains(".mapSingleToItem(item -> MapperS.of(OtherPayment.builder()"),
                "The fork's single mapSingleToItem on thenArg1 must be gone");
    }

    /**
     * Compounding — OriginalSwapUTI cftc: the cardinality lift moves thenArg1
     * {@code MapperS<String>} → {@code MapperC} (matching golden's {@code MapperC} cardinality).
     * UPDATED at PR #330: the recovery-walker extension (extract-descent + disguised-chain
     * leaf) now ALSO recovers golden's META element — the decl reads
     * {@code MapperC<FieldWithMetaString>} (this lock's own #318 javadoc named
     * FieldWithMetaString as golden's element type; the carrier moved 0.8596 → 0.8644
     * toward golden). Still NOT a byte-match — the residual whole-output deref facets
     * remain — so only the DECL is asserted.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void originalSwapUtiCftc_thenArgCardinalityLiftedToMapperC() {
        String gen = gen(ORIGINAL_SWAP_UTI_CFTC);
        assertTrue(gen.contains("final MapperC<FieldWithMetaString> thenArg1 = thenArg0"),
                "Expected OriginalSwapUTI cftc thenArg1 lifted to MapperC with golden's meta element");
        assertTrue(!gen.contains("final MapperS<String> thenArg1 = thenArg0"),
                "The fork's single MapperS<String> thenArg1 decl must be gone");
    }

    // ==== Green-safety / decline lock (passes on clean source too). ====

    /**
     * Green-safety — {@code SettlementCurrencyLeg1Rule} delegates through bare-rule-ref extracts to
     * SINGLE-output rules (ProductForEvent / SettlementTermsLeg1 / SettlementCurrency). The RRule arm's
     * {@code computeRuleBody} reads SINGLE for each, so it declines and the plain
     * {@code MapperS}/{@code mapSingleToItem} form stays — byte-matching golden on clean source too
     * (proving the arm does not over-fire on a single-output rule delegation).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void settlementCurrencyLeg1_singleRuleDelegation_staysGreen() throws IOException {
        assertByteMatchesGolden(SETTLEMENT_CURRENCY_LEG1);
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
                + path + " (PR #318 reportOutputRuleRefCardinality).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
