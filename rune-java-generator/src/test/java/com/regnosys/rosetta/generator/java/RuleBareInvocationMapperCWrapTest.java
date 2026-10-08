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
 * PR #278 — facet bareInvocationMapperCWrap: ONE green-safe, FUNCTION-byte-neutral GENERATOR
 * mechanism (two arms) in the M7b-3 rule-body cluster. 2 drr POJO Rule byte flips (the user-chosen
 * clean-small-finisher; the byte-oracle measured the clean tail at 2 — numCoerce/enum-qualify/sum
 * proved deeper and were deferred to #279).
 *
 * <p>The MULTI-cardinality sibling of PR #277's {@code bareEnumLambdaWrap} (which wrapped a bare ENUM
 * map body in {@code MapperS.of} at the MapperS-expecting seat) and the bare-FUNCTION analogue of
 * PR #274's bare-RULE {@code renderImplicitRuleInvocation} {@code MapperC.of} wrap. A bare FUNCTION
 * invocation — which {@code ReferenceHandler.renderImplicitFunctionInvocation} emits UNWRAPPED (unlike
 * {@code renderImplicitRuleInvocation}, which wraps a bare RULE itself) — at a Mapper-expecting MapperC
 * seat is re-presented wrapped by golden:
 *
 * <p><b>(A1) mapSingleToList body wrap (1).</b> A multi-cardinality extract body that is a bare
 * FUNCTION invocation selects {@code mapSingleToList} (lambda signature
 * {@code Function<MapperS<T>, MapperC<F>>}); golden wraps the body
 * {@code MapperC.<Elem>of(<fn>.evaluate(item.get()))}. The fork emitted the bare List-returning
 * {@code <fn>.evaluate(item.get())} (non-compiling). {@code CollectionHandler.compileLambda} gains a
 * new {@code MAPPER_C_EXPECTING} position (used by {@code handle(RExtractExpr)} for an
 * {@code isBodyMulti} extract) with a bare-FUNCTION arm wrapping {@code wrappedInMapperCOfSingle}
 * (witness = the body's inferred element type). Carrier: {@code BasketConstituentsRule} (hkma — the
 * {@code getBasketConstituents} sub-function returning {@code List<BasketConstituent>}).
 *
 * <p><b>(A2) MapperC.of element wrap (1).</b> A bare no-args FUNCTION-invocation ELEMENT of a
 * {@code MapperC.<X>of(...)} list literal is wrapped {@code MapperS.of(<fn>.evaluate(...))} by golden
 * (the SAME #180/#211 wrappedInMapperSOf law as the enum / constructor element arms, at the
 * bare-FUNCTION element position). {@code LiteralHandler.handle(RListLiteral)} gains the bare-FUNCTION
 * element arm. Carrier: {@code SettlementLocationRule} (iosco cde version1 —
 * {@code [settlementTermsLeg1, settlementTermsLeg2]} over two sub-functions).
 *
 * <p><b>Green-safe by construction.</b> A bare List at a MapperC-expecting lambda body / a bare item
 * at a MapperC.of element position both do not compile (MapperC.of expects Mappers), so golden ALWAYS
 * wraps them and NO green file carries the bare form — every carrier was a NON_COMPILING waivered
 * mismatch. Both arms are RULE-scoped ({@code findEnclosingRule != null}) → FUNCTION-byte-neutral by
 * construction (the shared compileLambda / LiteralHandler seats keep the function tail untouched; the
 * byte-oracle confirmed FUNCTION cells cdm5 83 / cdm6 239 / drr 211 UNCHANGED). Stash-baseline measured
 * exactly 2 flips (drr POJO mismatches 541 → 539), 0 within-waiver regressions, 9 improvement-churn
 * (the MAPPER_C_EXPECTING change pre-stages 9 co-occupied carriers toward golden). Both NON_COMPILING,
 * so byte AND functional parity each rise by 2.
 *
 * <p>Anchors are WHOLE-FILE byte comparisons of the generated drr POJO output (Rule kind) against the
 * frozen goldens (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}. REVERT-VERIFIED RED.
 */
class RuleBareInvocationMapperCWrapTest {

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

    // ==== Flip locks (revert-RED): the 2 bareInvocationMapperCWrap carriers now byte-match golden. ====

    /**
     * Flip lock (A1): BasketConstituents (hkma) — a {@code mapSingleToList(item -> …)} body that is a
     * bare FUNCTION invocation ({@code getBasketConstituents} returning {@code List<BasketConstituent>})
     * now wraps {@code MapperC.<BasketConstituent>of(getBasketConstituents.evaluate(item.get()))}.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void basketConstituents_hkma_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/hkma/rewrite/trade/reports/BasketConstituentsRule.java");
    }

    /**
     * Flip lock (A2): SettlementLocation (iosco cde version1) — bare FUNCTION-invocation ELEMENTS of a
     * {@code MapperC.<SettlementTerms>of(…)} list literal now each wrap
     * {@code MapperS.of(settlementTermsLeg1.evaluate(item.get()))}.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void settlementLocation_iosco_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/standards/iosco/cde/version1/execution/reports/SettlementLocationRule.java");
    }

    // ==== Green-safety decline locks: GREEN rules my arms could touch but correctly leave alone. ====

    /**
     * Green-safety lock (A1 DECLINE): BasketConstituents (asic) is a GREEN rule whose
     * {@code mapSingleToList} body is a bare RULE ({@code basketConstituentsRule.evaluate(item.get())},
     * already wrapped {@code MapperC.<BasketConstituentsReport>of(…)} by PR #274's
     * {@code renderImplicitRuleInvocation}). Proves the new MAPPER_C_EXPECTING arm is
     * {@code isBareFunctionInvocation}-scoped (RFunction only) — it declines on a bare RULE and does not
     * double-wrap the #274 form. Same rule NAME as the hkma flip; the discriminator is exactly
     * function-vs-rule.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void basketConstituents_asic_staysGreen() throws IOException {
        assertByteMatchesGolden("drr/regulation/asic/rewrite/trade/reports/BasketConstituentsRule.java");
    }

    /**
     * Green-safety lock (A2 DECLINE): DTCC_Leg1CommodityUnderlyerID (cftc) is a GREEN rule carrying a
     * {@code MapperC.<X>of(MapperS.of(…))} list literal whose elements are already correctly wrapped by
     * the existing enum / constructor / explicit-args-coercion element arms (NOT bare no-args FUNCTION
     * invocations). Proves the new bare-FUNCTION element arm in {@code LiteralHandler.handle(RListLiteral)}
     * declines on a non-bare-function element and does not double-wrap a green list literal.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void dtccLeg1CommodityUnderlyerID_cftc_staysGreen() throws IOException {
        assertByteMatchesGolden("drr/regulation/cftc/rewrite/dtcc/reports/DTCC_Leg1CommodityUnderlyerIDRule.java");
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
                + path + " (PR #278 bareInvocationMapperCWrap).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
