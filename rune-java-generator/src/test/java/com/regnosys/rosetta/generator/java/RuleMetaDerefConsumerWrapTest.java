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
 * PR #267 — TWO disjoint green-safe GENERATOR mechanisms (the M7b-3 rule-body cluster): the
 * consumer/operand {@code MapperS.of(...)} wrap (Facet A) + the evaluate-arg meta-deref STATEMENT_SINK
 * hoist (Facet B). 7 drr POJO Rule byte flips.
 *
 * <p><b>Facet A — consumer/operand wrap</b> (one law, {@code MapperS.of(<value>)} at a
 * {@code MapperS}-expecting seat; extends #218/#256/#266). Three seats:
 * <ul>
 *   <li><b>A1</b> non-scalar {@code default} then-output ({@code FunctionExpressionRenderer
 *       .renderThenExtractSet}): an {@code RDefaultExpr} then-output whose right is NOT a scalar
 *       literal renders {@code output = MapperS.of(<X>.getOrDefault(<right>)).get();} (the wrap +
 *       {@code .get()}, not the stripped bare form). COMPILES_DIVERGENT. Carrier:
 *       {@code CustomBasketCodeIdentifier}.</li>
 *   <li><b>A2</b> to-string bare-invocation operand ({@code ConversionHandler.handle(RToStringExpr)}):
 *       a bare no-args FUNCTION invocation to-string source wraps {@code MapperS.of(<fn>.evaluate(<in>))}
 *       so {@code .map("to-string", …)} applies (the #256 {@code wrapBareInvocationOperand}).
 *       NON_COMPILING. Carrier: {@code Beneficiary}.</li>
 *   <li><b>A3</b> escaped-enum operand (the {@code HandlerHelper.isDottedEnumConstant} of the day): a
 *       digit-leading escaped enum constant ({@code DayCountFractionEnum._30_360}) is recognized as an enum
 *       constant, so {@code wrapEnumOperand} wraps it {@code MapperS.of(...)} at the {@code areEqual}
 *       operand seat. Since PR #611 the recognition is the producer's witness
 *       ({@code HandlerHelper.isBareEnumConstant}), which needs no escape clause at all.
 *       NON_COMPILING. Carrier: {@code DayCountConvention}.</li>
 * </ul>
 *
 * <p><b>Facet B — evaluate-arg meta-deref STATEMENT_SINK hoist</b> (the #237/#262 family, the #263
 * lead-(a)). A meta wrapper ({@code ReferenceWithMetaParty}) passed as a fn-call ARG where the callee
 * wants the bare value hoists {@code final ReferenceWithMetaParty referenceWithMetaParty =
 * <chain>.get();} ahead of the statement + derefs {@code (referenceWithMetaParty == null ? null :
 * referenceWithMetaParty.getValue())}. {@code renderThenExtractSet}'s scope is now marked a
 * statement-hoist sink (it never was — the gap that declined these) and drains at the thenArg-decl
 * (base k==0: {@code InitialMargin*}) + the output line (nested in the output expression:
 * {@code NatureOfTheCounterparty*}); the {@code metaDerefHoistRoute} rule-then-body arm routes a
 * NESTED output-expression call to STATEMENT_SINK. NON_COMPILING. Carriers (4): {@code InitialMargin}
 * esma/hkma + {@code NatureOfTheCounterparty1/2} hkma.
 *
 * <p><b>Green-safe by construction</b> (corpus-verified, frozen 9.83.0, all 5 cells): A — golden
 * ALWAYS wraps the then-output default / to-string invocation / escaped-enum operand (0 green files
 * carry the bare form); B — 0 of 34,686 goldens carry a bare meta-wrapper-as-evaluate-arg vs 752
 * wrapped. FUNCTION-byte-neutral (the route arms + the to-string/default seats stay byte-flat for the
 * function tail; the rule-then-body route arm is {@code findEnclosingRule}-gated).
 *
 * <p>Anchors are WHOLE-FILE byte comparisons of the generated drr POJO output (Rule kind) against
 * the frozen goldens (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}. REVERT-VERIFIED RED.
 */
class RuleMetaDerefConsumerWrapTest {

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

    // ==== Facet A flip locks (revert-RED). ====

    /** A1 (COMPILES_DIVERGENT): non-scalar `default` then-output keeps the MapperS.of wrap + .get(). */
    @Test
    @EnabledIf("drrCellAvailable")
    void customBasketCodeIdentifier_asic_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/asic/rewrite/trade/reports/CustomBasketCodeIdentifierRule.java");
    }

    /** A2 (NON_COMPILING): bare-invocation to-string operand wraps MapperS.of(...). */
    @Test
    @EnabledIf("drrCellAvailable")
    void beneficiary_hkma_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/hkma/rewrite/trade/reports/BeneficiaryRule.java");
    }

    /** A3 (NON_COMPILING): escaped-enum (_30_360 etc.) areEqual operand wraps MapperS.of(...). */
    @Test
    @EnabledIf("drrCellAvailable")
    void dayCountConvention_iosco_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/standards/iosco/cde/version1/payment/reports/DayCountConventionRule.java");
    }

    // ==== Facet B flip locks (revert-RED): evaluate-arg meta-deref STATEMENT_SINK hoist. ====

    /** B base k==0 (NON_COMPILING): the meta wrapper sits in the base then-call's arg. */
    @Test
    @EnabledIf("drrCellAvailable")
    void initialMarginCollectedByCounterparty1PostHaircut_esma_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/esma/emir/refit/margin/reports/InitialMarginCollectedByCounterparty1PostHaircutRule.java");
    }

    /** B base k==0 (NON_COMPILING): hkma sibling of the esma InitialMargin carrier. */
    @Test
    @EnabledIf("drrCellAvailable")
    void initialMarginCollectedByTheReportingCounterparty1PostHaircut_hkma_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/hkma/rewrite/margin/reports/InitialMarginCollectedByTheReportingCounterparty1PostHaircutRule.java");
    }

    /** B output-nested (NON_COMPILING): the meta wrapper sits NESTED in the output expression — the
     * metaDerefHoistRoute rule-then-body NESTED arm routes it STATEMENT_SINK. */
    @Test
    @EnabledIf("drrCellAvailable")
    void natureOfTheCounterparty1_hkma_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/hkma/rewrite/trade/reports/NatureOfTheCounterparty1Rule.java");
    }

    /** B output-nested (NON_COMPILING): Counterparty2 sibling. */
    @Test
    @EnabledIf("drrCellAvailable")
    void natureOfTheCounterparty2_hkma_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/hkma/rewrite/trade/reports/NatureOfTheCounterparty2Rule.java");
    }

    // ==== Green-safety DECLINE locks: green rules at the SAME seats the change must NOT perturb. ====

    /**
     * Green-safety lock (Facet B DECLINE — then-extract with a meta nav consumed INLINE, no
     * evaluate-arg deref): {@code Counterparty1Rule} (asic valuation) is a GREEN drr Rule whose
     * then-extract navigates a {@code <ReferenceWithMetaParty>map(…)} meta leaf and derefs it INLINE
     * via {@code .<Party>map("Type coercion", … -> ….getValue())} — the bare value reaches the output,
     * NOT a fn-call arg. Marking {@code renderThenExtractSet}'s scope a statement-hoist sink is a no-op
     * here ({@code tryMetaDerefArg} declines — no meta wrapper passed where a bare item is wanted), so
     * the file stays byte-identical. Proves the sink-mark + drain does not perturb a green then-extract.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void counterparty1_asicValuation_inlineMetaDeref_staysGreen() throws IOException {
        assertByteMatchesGolden("drr/regulation/asic/rewrite/valuation/reports/Counterparty1Rule.java");
    }

    /**
     * Green-safety lock (Facet A1 DECLINE — scalar-literal default + Facet A3 DECLINE — regular enum):
     * {@code ActionTypeRule} (asic margin) is a GREEN drr Rule with a scalar-literal {@code default}
     * then-output ({@code output = MapperS.of(<X>.getOrDefault(false)).get()}, the #218 form handled by
     * the {@code isScalarLiteralDefault} branch — the A1 non-scalar gate declines) and a regular
     * (non-escaped) enum operand ({@code MapperS.of(MarginActionEnum.MARU)} — already a dotted enum
     * constant, so the A3 escaped-enum relaxation is purely additive and leaves it unchanged). Stays
     * byte-identical.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void actionType_asicMargin_scalarDefaultAndRegularEnum_staysGreen() throws IOException {
        assertByteMatchesGolden("drr/regulation/asic/rewrite/margin/reports/ActionTypeRule.java");
    }

    /**
     * Green-safety lock (Facet A2 DECLINE — to-string on a NON-bare-invocation receiver):
     * {@code ActionTypeRule} (asic valuation) is a GREEN drr Rule whose to-string source is an enum
     * NAV ({@code …map("to-string", ActionTypeEnum::toDisplayString)}) — not a bare no-args FUNCTION
     * invocation — so {@code wrapBareInvocationOperand} declines and the receiver is not double-wrapped.
     * Stays byte-identical.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void actionType_asicValuation_navToStringReceiver_staysGreen() throws IOException {
        assertByteMatchesGolden("drr/regulation/asic/rewrite/valuation/reports/ActionTypeRule.java");
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
                + path + " (PR #267 ruleMetaDerefConsumerWrap).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
