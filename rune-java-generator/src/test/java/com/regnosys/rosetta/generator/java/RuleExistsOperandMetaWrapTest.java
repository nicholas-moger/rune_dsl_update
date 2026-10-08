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
 * Anchor for facet {@code existsOperandMetaWrap} (PR #315): a bare inner-rule invocation used as the
 * OPERAND of an {@code exists}/{@code notExists} inside a map-lambda block whose rosetta OUTPUT is
 * META-typed ({@code [metadata reference/scheme]}) but whose Java {@code evaluate()} returns the BARE
 * value. golden coerces the value INTO the meta wrapper via the upstream convertNullSafe null-safe
 * ternary and DISTRIBUTES the existence check (+ the downstream {@code getOrDefault}) into both
 * branches:
 * <pre>
 *   final PriceSchedule priceSchedule = spreadLeg1Rule.evaluate(item.get());
 *   if ((priceSchedule == null ? exists(MapperS.&lt;ReferenceWithMetaPriceSchedule&gt;ofNull()).getOrDefault(false)
 *          : exists(MapperS.of(ReferenceWithMetaPriceSchedule.builder().setValue(priceSchedule).build())).getOrDefault(false))) {
 * </pre>
 * The fork emitted the bare {@code exists(MapperS.of(spreadLeg1Rule.evaluate(item.get()))).getOrDefault(false)}
 * (no meta round-trip). {@code ReferenceHandler.renderImplicitRuleInvocation}'s #315 arm returns the
 * meta wrap as a LIVE {@link com.regnosys.rosetta.generator.java.statement.builder.JavaConditionalExpression}
 * (with a LAMBDA_CHANNEL value hoist) so {@code ExistenceHandler} distributes {@code exists} over both
 * branches (mapExpression), and {@code CollectionHandler.compileLadderConditionalBlock} distributes the
 * {@code getOrDefault(false)} + drains the value hoist at the block top.
 *
 * <p>Carriers (4, all COMPILES_DIVERGENT — the fork's flat {@code exists(MapperS.of(<value>))} compiles +
 * behaves identically, a semantic round-trip): {@code SpreadOfLeg1NotationRule} /
 * {@code SpreadOfLeg2NotationRule} across esma-emir + fca-ukemir. Only the ladder (else-if guard) block
 * seat is shipped; the {@code asMapper} map-body seat (CustomBasketIndicator) and the STATEMENT_SINK
 * ite-hoist seat (UPI Proprietary) are follow-ons.
 *
 * <p>Green-safe by construction: the fork's flat wrap never byte-matched golden (a green file cannot
 * carry the distributed round-trip form). RULE-family-scoped → FUNCTION-byte-neutral (#232, cdm5 79 /
 * cdm6 232 / drr 206 FUNCTION mismatch UNCHANGED, cdm/iso/fpml POJO byte-IDENTICAL). A non-meta exists
 * operand (SpreadOfLeg1Notation's own {@code getNameOfTheFloatingRateOfLeg1 exists} rung) keeps the flat
 * {@code exists(MapperS.of(...)).getOrDefault(false)}. A CO-OCCUPIED carrier whose block form DECLINES
 * ({@code SpreadofLeg1Rule} — a multi-line ctor arm) SUPPRESSES the wrap
 * ({@code JavaStatementScope.isExistsMetaWrapSuppressed}) so it stays at its clean pre-#315 inline
 * ternary (no within-waiver regression).
 *
 * <p>Anchors are WHOLE-FILE byte comparisons of the generated drr output against the frozen goldens
 * (newline-normalized), generated through the REAL {@link D11CorpusRegressionTest#loadCellCorpusCached}.
 * REVERT-VERIFIED RED 5/7 — the 4 flip locks + the positive-content lock fail on clean source; the
 * suppression lock + the whole-map-body designed-decline lock pass either way.
 */
class RuleExistsOperandMetaWrapTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static final String LEG1_NOTATION_ESMA =
            "drr/regulation/esma/emir/refit/trade/reports/SpreadOfLeg1NotationRule.java";
    private static final String LEG2_NOTATION_ESMA =
            "drr/regulation/esma/emir/refit/trade/reports/SpreadOfLeg2NotationRule.java";
    private static final String LEG1_NOTATION_FCA =
            "drr/regulation/fca/ukemir/refit/trade/reports/SpreadOfLeg1NotationRule.java";
    private static final String LEG2_NOTATION_FCA =
            "drr/regulation/fca/ukemir/refit/trade/reports/SpreadOfLeg2NotationRule.java";
    // The co-occupied SUPPRESSION carrier (esma variant has a lowercase-'of' quirk).
    private static final String LEG1_RULE_ESMA =
            "drr/regulation/esma/emir/refit/trade/reports/SpreadofLeg1Rule.java";
    // A WHOLE-map-body exists (asMapper shape) the arm DECLINES by design (no intervening conditional).
    private static final String CUSTOM_BASKET_INDICATOR =
            "drr/regulation/common/trade/basket/reports/CustomBasketIndicatorRule.java";

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

    // ==== Flip locks (revert-RED): the 4 carriers now byte-match golden. ====

    @Test
    @EnabledIf("drrCellAvailable")
    void spreadOfLeg1Notation_esma_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(LEG1_NOTATION_ESMA);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void spreadOfLeg2Notation_esma_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(LEG2_NOTATION_ESMA);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void spreadOfLeg1Notation_fca_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(LEG1_NOTATION_FCA);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void spreadOfLeg2Notation_fca_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(LEG2_NOTATION_FCA);
    }

    // ==== Positive-content lock (revert-RED): the distributed meta-wrap + value hoist. ====

    /**
     * SpreadOfLeg1Notation (esma) hoists the bare value {@code final PriceSchedule priceSchedule =
     * spreadLeg1Rule.evaluate(item.get());} at the block top and distributes {@code exists(...)
     * .getOrDefault(false)} into the null-safe ternary branches — NOT the fork's flat
     * {@code exists(MapperS.of(spreadLeg1Rule.evaluate(item.get()))).getOrDefault(false)}. The NON-meta
     * sibling rung ({@code getNameOfTheFloatingRateOfLeg1 exists}) keeps the flat form (the wrap fires
     * ONLY for the meta-typed inner rule).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void spreadOfLeg1Notation_distributesExistsMetaWrap() {
        String gen = gen(LEG1_NOTATION_ESMA);
        assertTrue(gen.contains(
                "final PriceSchedule priceSchedule = spreadLeg1Rule.evaluate(item.get());"),
                "Expected the bare PriceSchedule value hoisted at the block top");
        assertTrue(gen.contains(
                "if ((priceSchedule == null ? exists(MapperS.<ReferenceWithMetaPriceSchedule>ofNull())"
                + ".getOrDefault(false) : exists(MapperS.of(ReferenceWithMetaPriceSchedule.builder()"
                + ".setValue(priceSchedule).build())).getOrDefault(false))) {"),
                "Expected the distributed exists-meta-wrap with getOrDefault(false) inside both branches");
        assertTrue(!gen.contains(
                "exists(MapperS.of(spreadLeg1Rule.evaluate(item.get()))).getOrDefault(false)"),
                "The fork's flat exists(MapperS.of(<rule>.evaluate(...))).getOrDefault(false) must be gone");
        assertTrue(gen.contains(
                "if (exists(MapperS.of(getNameOfTheFloatingRateOfLeg1.evaluate(item.get())))"
                + ".getOrDefault(false)) {"),
                "The NON-meta sibling exists rung must keep the flat form (the wrap fires only for meta)");
    }

    // ==== Suppression / green-safety lock (passes on clean source too): a co-occupied decline. ====

    /**
     * {@code SpreadofLeg1Rule} (esma) shares the {@code spreadLeg1Rule exists} meta operand; its
     * map body was CO-OCCUPIED (a multi-line {@code PriceFormat.builder()} ctor arm), so through
     * PR #332 the ladder block form DECLINED on the multi-line arm and the #315 fall-through
     * SUPPRESSED the meta-wrap (the fork kept the flat inline ternary).
     *
     * <p>CONVERTED at PR #333 (anchor-break-is-signal, the #284/#295/#296 precedent): the
     * {@code ladderMultiLineArm} facet OPENS the multi-line ctor arm, so the ladder
     * block-converts and the #315 meta-wrap NOW FIRES — golden's exact form (the
     * {@code final PriceSchedule priceSchedule = spreadLeg1Rule.evaluate(item.get());} value
     * hoist + the distributed {@code priceSchedule == null ?
     * exists(MapperS.<ReferenceWithMetaPriceSchedule>ofNull())} condition are byte-verified
     * present in the frozen golden). The carrier moved 18 → 6 diff lines toward golden
     * (regscan-verified toward-mover); the residual is the ctor-arm MapperS.of wrap + the
     * BigDecimal literal coercion (a #334 facet). This lock now pins the FIRED block+hoist form.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void spreadOfLeg1Rule_multiLineArmOpened_blockConvertsAndWrapFires() {
        String gen = gen(LEG1_RULE_ESMA);
        assertTrue(!gen.contains(
                "exists(MapperS.of(spreadLeg1Rule.evaluate(item.get()))).getOrDefault(false) ?"),
                "the pre-#333 flat inline-ternary exists must be gone (the ladder block-converts)");
        assertTrue(gen.contains(
                "final PriceSchedule priceSchedule = spreadLeg1Rule.evaluate(item.get());"),
                "the #315 meta-wrap value hoist fires inside the opened block ladder (golden's form)");
        assertTrue(gen.contains("priceSchedule == null ? "
                + "exists(MapperS.<ReferenceWithMetaPriceSchedule>ofNull())"),
                "the distributed meta-wrap exists condition renders (golden's form)");
    }

    /**
     * The formerly-DEFERRED whole-map-body `asMapper` seat (`item -> exists(…).asMapper()`, the
     * CustomBasketIndicator shape) — #315 declined it BY DESIGN (the two documented orphan
     * concerns: the JavaExpression-gated asMapper coercion + the drain fall-through dropping a
     * conditional body's hoists).
     *
     * <p>CONVERTED at PR #331 (facet existsMetaSeats CP3a — the anchor-break-is-signal
     * precedent, #284/#295/#296/#299): #331 IS the fix that opens this seat (compileLambda
     * distributes `.asMapper()` into both branches type-preservingly + block-renders the
     * drained value hoist with a bare-ternary return), so the carrier now BLOCK-converts and
     * byte-matches golden (the whole-file lock lives in RuleBucketDrainExistsMetaTest). The
     * #330 CP5 recovery extension resolving `cde.basket.CustomBasketCode` is what unblocked
     * the recovery this seat gates on.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void customBasketIndicator_wholeMapBodyExists_blockConvertsAndWraps() {
        String gen = gen(CUSTOM_BASKET_INDICATOR);
        assertTrue(gen.contains("final String string = customBasketCodeRule.evaluate(item.get());"),
                "The whole-map-body asMapper exists seat must hoist the inner-rule value at the block top");
        assertTrue(gen.contains(
                "string == null ? exists(MapperS.<FieldWithMetaString>ofNull()).asMapper()"
                + " : exists(MapperS.of(FieldWithMetaString.builder().setValue(string).build())).asMapper()"),
                "The exists + .asMapper() must DISTRIBUTE into both null-safe ternary branches");
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
                + path + " (PR #315 existsOperandMetaWrap).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
