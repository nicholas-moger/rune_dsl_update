package com.regnosys.rosetta.generator.java.function;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.symbols.RLinkingResult;
import com.regnosys.rosetta.symbols.RWorkspace;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Engine PR (facet {@code choice_option_nav_witness}) — a RENDERER-ONLY fix
 * ({@code NavigationHandler}) that recovers the {@code <Type>} generic witness and the
 * chained/owner lambda variable for a navigation step that traverses a CHOICE OPTION,
 * matching upstream 9.83.0.
 *
 * <p>Background: in cdm 6.20.6 several model types became {@code choice} types
 * ({@code Payout}, {@code RateSpecification}, …). A navigation that selects an option of a
 * choice — {@code payout -> OptionPayout}, {@code rateSpecification -> FixedRateSpecification}
 * — parses as an {@link com.regnosys.rosetta.ast.expressions.references.RFeatureCall} whose
 * {@code featureName} is the OPTION TYPE NAME (capitalised) and whose {@code resolvedFeature}
 * is unset (a choice option is not an {@code RAttribute}). The fork's
 * {@code NavigationHandler.resolveReceiverDataType} / {@code attributeToDataType} resolve a
 * receiver's data type via the AST {@code referencedType()} slot, which is an
 * {@code RChoice} (not an {@code RDataType}) for a choice-typed receiver — so it returns
 * {@code null}, and the witness ({@code resolveTypeParam}) + chained/owner lambda var
 * ({@code resolveLambdaVarName}) for the choice-option step (and every step chained after it)
 * collapse to the witness-less / raw-feature-name form.
 *
 * <p>On current {@code main} the fork emits, for {@code GetFixedRate}:
 * {@code .map("getFixedRateSpecification", rateSpecification -> …)} (no {@code <…>} witness)
 * then {@code .map("getRateSchedule", FixedRateSpecification -> FixedRateSpecification.…)}
 * (witness-less AND the raw capitalised option name as the lambda var). Upstream
 * ({@code ExpressionGenerator.attributeCall}: {@code t = receiverRType instanceof RChoiceType
 * ? asRDataType : (RDataType) receiverRType}) narrows the choice to its data-type projection,
 * so the witness resolves and the lambda var is {@code fixedRateSpecification}.
 *
 * <p>The fix adds a strictly-additive gm-aware resolution: when the AST {@code referencedType}
 * path returns null, fall back to {@code GeneratorModel.resolveTypeCall} (by-name workspace
 * resolution) — an {@code RChoiceTypeRef} is projected via {@code asRDataType()} so the
 * options become findable attributes, cascading witness + lambda recovery through the whole
 * chain (including a {@code .get()} only-element collapse, reusing the PR #152 machinery).
 *
 * <p>This test is REVERT-VERIFIED RED: revert the {@code NavigationHandler} change → the
 * choice-option step loses its witness/lambda and the witness-less / capitalised-lambda forms
 * reappear (negative assertions fail; positive assertions fail).
 *
 * <p>Determinism follows {@link FunctionMapEnumSwitchTest}: the corpus is the version-frozen
 * {@code cdm/6.20.6} cell, so the generator output is a pure function of the fork's code —
 * this test flips only when the fork's code changes.
 */
class FunctionChoiceOptionNavWitnessTest {

    private static final Path CDM_ROSETTA_DIR =
            Path.of("../test-corpus/cdm/cdm-6.20.6/rosetta-source/src/main/rosetta");
    private static final Path CDM_GOLDEN_DIR =
            Path.of("../test-corpus/cdm/cdm-6.20.6/rosetta-source/src/generated/java");
    private static final Path BUILTINS_DIR = Path.of("../test-corpus/rune-dsl-builtins");

    private static final JavaTypeUtil TYPE_UTIL = new JavaTypeUtil();
    private static final JavaTypeTranslator TYPE_TRANSLATOR = new JavaTypeTranslator(TYPE_UTIL);

    private static Map<String, String> functionOutput;

    static boolean cdmAvailable() {
        if (!Files.isDirectory(CDM_ROSETTA_DIR) || !Files.isDirectory(CDM_GOLDEN_DIR)) {
            return false;
        }
        try (var stream = Files.walk(CDM_ROSETTA_DIR)) {
            return stream.anyMatch(p -> p.toString().endsWith(".rosetta"));
        } catch (IOException e) {
            return false;
        }
    }

    @BeforeAll
    static void generateAllFunctions() throws IOException {
        if (!cdmAvailable()) return;
        var corpus = loadFullCorpus();
        var gm = new GeneratorModel(corpus.workspace());
        var gen = new FunctionGenerator(gm, TYPE_TRANSLATOR, TYPE_UTIL);
        functionOutput = new LinkedHashMap<>();
        gen.generate(functionOutput);
    }

    /**
     * Only-element cascade: {@code economicTerms -> payout -> OptionPayout} (choice option)
     * then {@code .get() -> settlementTerms -> settlementType}. Locks the choice-option
     * witness ({@code <OptionPayout>}) AND the post-{@code .get()} owner-derived lambda var
     * ({@code optionPayout}) AND the downstream witnesses ({@code <SettlementTerms>},
     * {@code <SettlementTypeEnum>}).
     */
    @Test
    @EnabledIf("cdmAvailable")
    void choiceOptionNav_onlyElementCascade_recoversWitnessAndOwnerLambda() {
        String body = requireGenerated(
                "cdm/product/qualification/functions/Qualify_Commodity_Option_Cash.java");

        assertTrue(body.contains(
                ".<OptionPayout>map(\"getOptionPayout\", payout -> payout.getOptionPayout())"),
                "the choice-option step `payout -> OptionPayout` must recover the <OptionPayout> "
                + "witness. Body:\n" + body);
        assertTrue(body.contains(
                ".get()).<SettlementTerms>map(\"getSettlementTerms\", optionPayout -> optionPayout.getSettlementTerms())"),
                "the post-.get() step must recover the <SettlementTerms> witness and the owner-derived "
                + "lambda var `optionPayout` (cascaded through the choice option). Body:\n" + body);
        assertTrue(body.contains(
                ".<SettlementTypeEnum>map(\"getSettlementType\", settlementTerms -> settlementTerms.getSettlementType())"),
                "the downstream step must recover the <SettlementTypeEnum> witness. Body:\n" + body);

        // Negative: the pre-fix witness-less / default-lambda forms must be gone.
        assertFalse(body.contains(".map(\"getOptionPayout\""),
                "the witness-less `.map(\"getOptionPayout\", …)` form must be gone (it reappears if "
                + "the choice-option resolution is reverted). Body:\n" + body);
        assertFalse(body.contains(".map(\"getSettlementTerms\", _settlementTerms ->"),
                "the witness-less / default-lambda `.map(\"getSettlementTerms\", _settlementTerms -> …)` "
                + "form must be gone. Body:\n" + body);
    }

    /**
     * Chained choice-option (NOT via {@code .get()}): {@code interestRatePayout ->
     * rateSpecification -> FixedRateSpecification -> rateSchedule}. {@code RateSpecification} is
     * a choice; {@code FixedRateSpecification} is one of its options. Locks the choice-option
     * witness ({@code <FixedRateSpecification>}) AND the chained lambda var derived from the
     * choice-option type ({@code fixedRateSpecification}, not the raw capitalised feature name)
     * AND the downstream {@code <RateSchedule>} witness.
     */
    @Test
    @EnabledIf("cdmAvailable")
    void choiceOptionNav_chained_recoversWitnessAndChainedLambda() {
        String body = requireGenerated(
                "cdm/product/asset/calculation/functions/GetFixedRate.java");

        assertTrue(body.contains(
                ".<FixedRateSpecification>map(\"getFixedRateSpecification\", rateSpecification -> rateSpecification.getFixedRateSpecification())"),
                "the choice-option step `rateSpecification -> FixedRateSpecification` must recover the "
                + "<FixedRateSpecification> witness. Body:\n" + body);
        assertTrue(body.contains(
                ".<RateSchedule>map(\"getRateSchedule\", fixedRateSpecification -> fixedRateSpecification.getRateSchedule())"),
                "the step chained after the choice option must recover the <RateSchedule> witness AND "
                + "the chained lambda var `fixedRateSpecification` (lowerCamel of the choice-option "
                + "type, not the raw capitalised feature name). Body:\n" + body);

        // Negative: the pre-fix witness-less + raw-capitalised-lambda forms must be gone.
        assertFalse(body.contains(".map(\"getFixedRateSpecification\""),
                "the witness-less `.map(\"getFixedRateSpecification\", …)` form must be gone. Body:\n" + body);
        assertFalse(body.contains("\"getRateSchedule\", FixedRateSpecification ->"),
                "the raw capitalised lambda var `FixedRateSpecification` on the chained step must be "
                + "gone (it reappears if the chained choice-option lambda resolution is reverted). "
                + "Body:\n" + body);
    }

    // =========================================================================
    // Helpers (mirror FunctionMapEnumSwitchTest)
    // =========================================================================

    private static String requireGenerated(String path) {
        assertNotNull(functionOutput,
                "Function generation did not run — CDM corpus unavailable?");
        String generated = functionOutput.get(path);
        assertNotNull(generated, "Function not generated: " + path);
        return generated.replace("\r\n", "\n").replace("\r", "\n");
    }

    private static RLinkingResult loadFullCorpus() throws IOException {
        List<RModel> models = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        if (Files.isDirectory(BUILTINS_DIR)) {
            try (var stream = Files.walk(BUILTINS_DIR)) {
                stream.filter(Files::isRegularFile)
                      .filter(p -> p.toString().endsWith(".rosetta"))
                      .sorted()
                      .forEach(p -> {
                          try { models.add(AstBuilder.buildFromFile(p)); }
                          catch (Exception e) {
                              skipped.add(p + ": " + e.getMessage());
                          }
                      });
            }
        }
        try (var stream = Files.walk(CDM_ROSETTA_DIR)) {
            stream.filter(Files::isRegularFile)
                  .filter(p -> p.toString().endsWith(".rosetta"))
                  .sorted()
                  .forEach(p -> {
                      try {
                          RModel model = AstBuilder.buildFromFile(p);
                          model.setVersion("0.0.0.master-SNAPSHOT");
                          models.add(model);
                      } catch (Exception e) {
                          skipped.add(p + ": " + e.getMessage());
                      }
                  });
        }
        if (!skipped.isEmpty()) {
            throw new IOException(
                    "Corpus loader failed to parse " + skipped.size()
                            + " file(s); refusing to produce partial workspace: "
                            + String.join("; ", skipped));
        }
        return RWorkspace.build(models);
    }
}
