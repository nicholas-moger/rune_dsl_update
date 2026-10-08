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
 * PR #276 — TWO disjoint, green-safe, rule-scoped (FUNCTION-byte-neutral) GENERATOR mechanisms in the
 * M7b-3 rule-body cluster, composed in one PR. 11 drr POJO Rule byte flips.
 *
 * <p><b>(A) chainedBareFunctionThen (7) — the #254-deferred chained bare-FUNCTION-then.</b> PR #254
 * admitted a chained bare-RULE then ({@code filter … then <rule1> then <rule2>}) to
 * {@code FunctionExpressionRenderer.renderThenExtractSet}'s hoisted {@code final Mapper*<X> thenArgN}
 * form, but DEFERRED the bare-FUNCTION sibling (the "dual wrap-factory check"): a bare-RULE body
 * compiles to a ready {@code MapperS.of(rule.evaluate(…))} wrap-factory ({@code
 * renderImplicitRuleInvocation} always wraps), whereas a bare-FUNCTION body compiles BARE
 * ({@code renderImplicitFunctionInvocation} emits {@code fn.evaluate(…)} with no wrap). So a chain
 * ending in (or carrying) a bare-FUNCTION then declined to the non-compiling inline runtime
 * {@code .then(} (there is no {@code Mapper.then(Function)} method). This PR admits a bare-FUNCTION
 * body on the rule path ({@code isHoistableThenChain}) and WRAPS the bare value {@code MapperS.of(…)}
 * in {@code renderThenExtractSetImpl} — at the chain BASE / an INTERMEDIATE decl
 * ({@code final MapperS<X> thenArgN = MapperS.of(fn.evaluate(prev.get()))}) and the TERMINAL (so
 * {@code isInvocationWrapFactory} recognises it and appends {@code .get()} →
 * {@code output = MapperS.of(formatToBaseOneRate.evaluate(thenArg5.get())).get();}).
 *
 * <p><b>(B) bigIntHoistDrain (4) — the #177 BigInteger decl drained inside the ite-hoist branch.</b>
 * A rule then-chain ending in a conditional whose then-arm passes a beyond-long int literal to a
 * BigDecimal-expecting function ({@code extractCallAmount(thenArg.get(), 9999…)}) registers the #177
 * {@code final BigInteger <id> = new BigInteger("…");} hoist on the sink during the arm compile, but
 * {@code FunctionExpressionRenderer.appendIteHoistChainCore} never drained it INSIDE the
 * {@code if}-branch (golden puts it as the branch's first line), so the decl dropped while the
 * {@code (<id> == null ? null : new BigDecimal(<id>))} reference remained — non-compiling. This PR
 * drains ONLY the {@code new BigInteger(} decl into the branch; every OTHER arm hoist (a #250
 * deep-then {@code thenArg} decl, a #267 meta-deref) is RE-REGISTERED so it flows exactly as before
 * — NOT pulled into the branch (a co-occupied #257-cascade carrier such as {@code PriorUti} must
 * keep its deep-then-hoist fallback; pulling it in moved it AWAY from golden — the regscan caught it).
 *
 * <p><b>Green-safe by construction (both).</b> Zero of the 34,686 goldens carry the runtime
 * {@code .then(} form OR a dangling-BigInteger reference, so every carrier was a non-compiling
 * waivered mismatch — the byte-oracle measured exactly 11 flips, 0 within-waiver regressions, 0 new
 * mismatches, FUNCTION-byte-neutral (both mechanisms gate on {@code findEnclosingRule}). All 11 are
 * NON_COMPILING, so byte AND functional parity each rise by 11; COMPILES_DIVERGENT held 162.
 *
 * <p>Anchors are WHOLE-FILE byte comparisons of the generated drr POJO output (Rule kind) against the
 * frozen goldens (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}. REVERT-VERIFIED RED.
 */
class RuleChainedBareFunctionThenTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static Map<String, String> drrPojoOutput;

    static boolean drrCellAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR);
    }

    @BeforeAll
    static void generatePojo() throws IOException {
        if (drrCellAvailable()) {
            drrPojoOutput = generatePojoCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT));
        }
    }

    private static Map<String, String> generatePojoCell(D11CorpusRegressionTest.CellSpec cell)
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
        return output;
    }

    // ==== (A) chainedBareFunctionThen flip locks (revert-RED): a rule then-chain carrying a
    //      bare-FUNCTION then now hoists `final MapperS<X> thenArgN = MapperS.of(fn.evaluate(…))`
    //      + a terminal `output = MapperS.of(fn.evaluate(thenArgN.get())).get();`. ====

    /** Flip lock: FixedRateOfLeg1OrCoupon (esma) — terminal bare-FUNCTION {@code formatToBaseOneRate}. */
    @Test
    @EnabledIf("drrCellAvailable")
    void fixedRateOfLeg1OrCoupon_esma_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/esma/emir/refit/trade/reports/FixedRateOfLeg1OrCouponRule.java");
    }

    /** Flip lock: FixedRateOfLeg1OrCoupon (fca) — the ukemir sibling. */
    @Test
    @EnabledIf("drrCellAvailable")
    void fixedRateOfLeg1OrCoupon_fca_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/fca/ukemir/refit/trade/reports/FixedRateOfLeg1OrCouponRule.java");
    }

    /** Flip lock: FixedRateOfLeg2 (esma). */
    @Test
    @EnabledIf("drrCellAvailable")
    void fixedRateOfLeg2_esma_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/esma/emir/refit/trade/reports/FixedRateOfLeg2Rule.java");
    }

    /** Flip lock: FixedRateOfLeg2 (fca). */
    @Test
    @EnabledIf("drrCellAvailable")
    void fixedRateOfLeg2_fca_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/fca/ukemir/refit/trade/reports/FixedRateOfLeg2Rule.java");
    }

    /** Flip lock: IndexFactor (esma) — terminal bare-FUNCTION over an implicit chain. */
    @Test
    @EnabledIf("drrCellAvailable")
    void indexFactor_esma_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/esma/emir/refit/trade/reports/IndexFactorRule.java");
    }

    /**
     * Flip lock: OtherPaymentPayer (hkma) — a CHAINED bare-FUNCTION then
     * ({@code … then filter_HKMAPriorityPartyIdentifiers then extract_HKMAPartyIdentifier}) where the
     * #267 meta-deref-sink also composes (the {@code referenceWithMetaParty} hoist + null-guarded arg).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void otherPaymentPayer_hkma_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/hkma/rewrite/trade/reports/OtherPaymentPayerRule.java");
    }

    /** Flip lock: OtherPaymentReceiver (hkma) — the payer sibling. */
    @Test
    @EnabledIf("drrCellAvailable")
    void otherPaymentReceiver_hkma_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/hkma/rewrite/trade/reports/OtherPaymentReceiverRule.java");
    }

    // ==== (B) bigIntHoistDrain flip locks (revert-RED): the #177 `final BigInteger <id> =
    //      new BigInteger("…");` decl now emits as the first line INSIDE the ite-hoist if-branch. ====

    /** Flip lock: CallAmount (csa) — {@code extractCallAmount(thenArg.get(), 9999…)} in the then-arm. */
    @Test
    @EnabledIf("drrCellAvailable")
    void callAmount_csa_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/csa/rewrite/trade/reports/CallAmountRule.java");
    }

    /** Flip lock: CallAmount (jfsa). */
    @Test
    @EnabledIf("drrCellAvailable")
    void callAmount_jfsa_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/jfsa/rewrite/trade/reports/CallAmountRule.java");
    }

    /** Flip lock: PutAmount (csa). */
    @Test
    @EnabledIf("drrCellAvailable")
    void putAmount_csa_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/csa/rewrite/trade/reports/PutAmountRule.java");
    }

    /** Flip lock: PutAmount (jfsa). */
    @Test
    @EnabledIf("drrCellAvailable")
    void putAmount_jfsa_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/jfsa/rewrite/trade/reports/PutAmountRule.java");
    }

    // ==== Green-safety decline locks: GREEN files my mechanisms could touch but correctly leave alone. ====

    /**
     * Green-safety lock (chainedBareFunctionThen DECLINE): {@code UniqueTransactionIdentifier} (asic
     * margin) is a GREEN rule whose then-chain routes through {@code renderThenExtractSet} but carries
     * NO bare-FUNCTION then-body (a green file cannot — the inline {@code .then(} form does not
     * compile). Proves admitting bare-FUNCTION bodies to {@code isHoistableThenChain} did not perturb
     * the implicit / bare-RULE then-chain path it already handled.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void uniqueTransactionIdentifier_asic_staysGreen() throws IOException {
        assertByteMatchesGolden("drr/regulation/asic/rewrite/margin/reports/UniqueTransactionIdentifierRule.java");
    }

    /**
     * Green-safety lock (bigIntHoistDrain DECLINE): {@code Direction2Leg1} (asic trade) is a GREEN rule
     * with a {@code final MapperS<…> ifThenElseResult; if (…) {…}} ite-hoist block but NO
     * {@code new BigInteger(} in any arm. Proves the {@code appendIteHoistChainCore} drain is gated to
     * the BigInteger decl only (other arm hoists are re-registered, not pulled into the branch), so a
     * non-BigInteger ite-hoist rule is byte-unchanged.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void direction2Leg1_asic_staysGreen() throws IOException {
        assertByteMatchesGolden("drr/regulation/asic/rewrite/trade/reports/Direction2Leg1Rule.java");
    }

    private static void assertByteMatchesGolden(String path) throws IOException {
        assertNotNull(drrPojoOutput, "drr POJO generation did not run — corpus unavailable?");
        String generated = drrPojoOutput.get(path);
        assertNotNull(generated, "Rule class not generated: " + path
                + " (RuleGenerator emission failed or the path differs)");
        Path goldenPath = DRR_GOLDEN_DIR.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated drr Rule output must byte-match the golden (newline-normalized) for "
                + path + " (PR #276 chainedBareFunctionThen + bigIntHoistDrain).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
