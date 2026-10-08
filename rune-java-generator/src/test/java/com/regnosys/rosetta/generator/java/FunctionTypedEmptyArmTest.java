package com.regnosys.rosetta.generator.java;

import com.regnosys.rosetta.utils.DeepFeatureCallUtil;
import com.regnosys.rosetta.generator.GenerationException;
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
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static com.regnosys.rosetta.testutil.GenerationErrors.assertNoGenerationErrors;

/**
 * PR #303 — typedEmptyArm: TWO disjoint green-safe GENERATOR mechanisms, 5 byte flips
 * (4 cdm FUNCTION + 1 drr POJO), the proven clean-compose play. Both fix the SAME theme — an
 * {@code empty} arm ({@code REmptyLiteral}) rendered as the bare {@code return null;} where golden
 * re-presents the signature-typed {@code return MapperS.<X>ofNull();} — at two disjoint render seats.
 *
 * <p><b>Mechanism 1 — alias-return-ladder {@code else empty} (4 cdm FUNCTION).</b> An alias whose body
 * is {@code if c then v else empty} ({@link com.regnosys.rosetta.ast.expressions.references.REmptyLiteral})
 * rendered through {@code FunctionExpressionRenderer.appendReturnLadder}: the fork's {@code hasEffectiveElse}
 * only treated the synthesized empty <em>list</em> literal (the elseless {@code if/then}) as "no value",
 * so an explicit {@code else empty} was compiled to the bare {@code null} ({@code return null;} — a
 * {@code MapperS<X>} method returning {@code null} compiles but is byte-divergent from golden's typed
 * empty). Broadening {@code hasEffectiveElse} to also exclude {@code REmptyLiteral} routes the
 * {@code else empty} to {@code typedEmptyElseOrNull} ({@code MapperS.<X>ofNull()}). All 4 carriers are
 * {@code MapperS<X>} aliases (a trace confirmed only {@code arithmeticOp}/{@code feeType}/
 * {@code terminationDate}/{@code transferExpression} carry a {@code REmptyLiteral} else, all
 * {@code MapperS<X>}): {@code Create_TradeState} (cdm5+cdm6, {@code terminationDate} →
 * {@code MapperS.<Date>ofNull()}), {@code MapAmendmentToPrimitiveInstruction} (cdm6, {@code feeType} →
 * {@code MapperS.<FeeTypeEnum>ofNull()}), {@code MapTerminationToPrimitiveInstruction} (cdm6,
 * {@code transferExpression}). ({@code arithmeticOp} lives in cdm6 {@code MapExecutionDetails}, a
 * co-occupied mismatch at the time — moved toward golden here, then FLIPPED byte-identical at
 * PR #382 [the blank-final enum join + alias type-switch facets;
 * {@code LadderArmMetaSigKeepQuartetComposeTest} carries its whole-file lock].)
 *
 * <p><b>Mechanism 2 — block-lambda ladder rung {@code then empty} (1 drr POJO).</b> The block-lambda
 * ladder renderer ({@code CollectionHandler.compileLadderConditionalBlock}) rendered a rung whose THEN
 * is {@code empty} as {@code if (<cond>) { return null; }} (the compiled {@code empty} literal), while
 * the terminal empty else already used the typed form. Hoisting the same {@code itemType} resolution
 * above the rung loop lets a {@code then empty} rung re-present {@code return MapperS.<X>ofNull();} too.
 * Carrier {@code PriceUnitOfMeasure} (iosco cde version1): its
 * {@code else if priceType = PriceTypeEnum -> InterestRate then empty} rung now renders the typed empty.
 *
 * <p><b>Green-safety.</b> Both seats: golden ALWAYS renders the typed empty for an {@code empty} arm —
 * the bare {@code return null;} goldens in the corpus live at OTHER seats (the {@code *DeepPathUtil}
 * generator + filter-predicate lambda bodies), untouched by either fix. So every carrier was an
 * already-waivered byte-divergence (a {@code MapperS<X>} method/lambda returning {@code null} compiles
 * → COMPILES_DIVERGENT). Confirmed: regscan 0 within-waiver regressions / 5 flipped-out across all 8
 * cells; the effective-else (non-empty) branch is untouched (mechanism 1 only diverts an {@code empty}
 * else; {@code Create_TradeState.valuation}'s {@code create_Valuation} else is preserved); the green
 * {@code ActionTypeRule} ladder (typed-empty terminal) is byte-identical (the refactored terminal
 * computation in mechanism 2 produces the same bytes).
 *
 * <p>Anchors are WHOLE-FILE byte comparisons against the frozen 9.83.0 goldens (newline-normalized),
 * generated through the REAL {@link D11CorpusRegressionTest#loadCellCorpusCached}. REVERT-VERIFIED RED
 * for the 5 flip locks + 2 positive-content locks (7/9); the 2 green-safety locks pass either way.
 */
class FunctionTypedEmptyArmTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM5_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-5.38.0");
    private static final Path CDM5_GOLDEN_DIR =
            CDM5_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static Map<String, String> drrOutput;
    private static Map<String, String> cdm6FunctionOutput;
    private static Map<String, String> cdm5FunctionOutput;

    static boolean drrCellAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR);
    }

    static boolean cdm6CellAvailable() {
        return Files.isDirectory(CDM6_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM6_GOLDEN_DIR);
    }

    static boolean cdm5CellAvailable() {
        return Files.isDirectory(CDM5_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM5_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (drrCellAvailable()) {
            drrOutput = generateCellAll(
                    new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT));
        }
        if (cdm6CellAvailable()) {
            cdm6FunctionOutput = generateCellFunctions(
                    new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CDM6_CELL_ROOT));
        }
        if (cdm5CellAvailable()) {
            cdm5FunctionOutput = generateCellFunctions(
                    new D11CorpusRegressionTest.CellSpec("cdm", "5.38.0", CDM5_CELL_ROOT));
        }
    }

    private static Map<String, String> generateCellAll(D11CorpusRegressionTest.CellSpec cell)
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

    private static Map<String, String> generateCellFunctions(D11CorpusRegressionTest.CellSpec cell)
            throws IOException {
        var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(),
                D11CorpusRegressionTest.emissionFilter(cell));
        var typeUtil = new JavaTypeUtil();
        var gen = new FunctionGenerator(gm, new JavaTypeTranslator(typeUtil), typeUtil);
        Map<String, String> output = new LinkedHashMap<>();
        List<GenerationException> genErrors = gen.generateWithErrors(output);
        assertTrue(genErrors.isEmpty(),
                cell + " FUNCTION generation reported errors: " + genErrors);
        return output;
    }

    // ==== Mechanism 1 flip locks (revert-RED): an alias `else empty` renders the typed empty ====

    /**
     * Flip (cdm5): {@code Create_TradeState}'s {@code terminationDate} alias —
     * {@code if quantityChange exists then resolveAdjustableDate(...) else empty} — now ends
     * {@code return MapperS.<Date>ofNull();}; the fork rendered the bare {@code return null;}.
     */
    @Test
    @EnabledIf("cdm5CellAvailable")
    void createTradeStateCdm5_aliasElseEmpty_byteMatchesGolden() throws IOException {
        assertFnByteMatchesGolden(cdm5FunctionOutput, CDM5_GOLDEN_DIR,
                "cdm/event/common/functions/Create_TradeState.java");
    }

    /** Flip (cdm6): the same {@code Create_TradeState} {@code terminationDate} {@code else empty}. */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void createTradeStateCdm6_aliasElseEmpty_byteMatchesGolden() throws IOException {
        assertFnByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/event/common/functions/Create_TradeState.java");
    }

    /**
     * Flip (cdm6): {@code MapAmendmentToPrimitiveInstruction}'s {@code feeType} alias —
     * {@code if payment exists then FeeTypeEnum -> Renegotiation else empty} — now ends
     * {@code return MapperS.<FeeTypeEnum>ofNull();}.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapAmendmentCdm6_aliasElseEmpty_byteMatchesGolden() throws IOException {
        assertFnByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/workflowstep/functions/MapAmendmentToPrimitiveInstruction.java");
    }

    /**
     * Flip (cdm6): {@code MapTerminationToPrimitiveInstruction}'s {@code transferExpression} alias —
     * {@code if termination exists then FeeTypeEnum -> Termination else empty}.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapTerminationCdm6_aliasElseEmpty_byteMatchesGolden() throws IOException {
        assertFnByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/workflowstep/functions/MapTerminationToPrimitiveInstruction.java");
    }

    // ==== Mechanism 2 flip lock (revert-RED): a block-lambda ladder `then empty` rung renders typed ====

    /**
     * Flip (drr iosco cde version1): {@code PriceUnitOfMeasure}'s nested if/else-if ladder ending in
     * {@code else if priceType = PriceTypeEnum -> InterestRate then empty} renders the rung
     * {@code return MapperS.<String>ofNull();}; the fork rendered {@code if (<cond>) { return null; }}.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void priceUnitOfMeasureIosco_ladderRungThenEmpty_byteMatchesGolden() throws IOException {
        assertDrrByteMatchesGolden(
                "drr/standards/iosco/cde/version1/price/reports/PriceUnitOfMeasureRule.java");
    }

    // ==== Positive-content locks (revert-RED): the typed empty actually fired ====

    /**
     * Positive-content (mechanism 1): {@code feeType} must render {@code return MapperS.<FeeTypeEnum>ofNull();}
     * and carry NO bare {@code return null;}. Reverting the {@code hasEffectiveElse} broadening re-emits the
     * bare {@code return null;} for the {@code else empty} → RED.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapAmendmentFeeType_typedEmptyElse_positiveContent() throws IOException {
        assertNotNull(cdm6FunctionOutput, "cdm6 function generation did not run — corpus unavailable?");
        String gen = cdm6FunctionOutput.get(
                "cdm/ingest/fpml/confirmation/workflowstep/functions/MapAmendmentToPrimitiveInstruction.java");
        assertNotNull(gen, "MapAmendmentToPrimitiveInstruction not generated");
        assertTrue(gen.contains("return MapperS.<FeeTypeEnum>ofNull();"),
                "the `else empty` alias must render the signature-typed MapperS.<FeeTypeEnum>ofNull() "
                + "(PR #303 mechanism 1, alias-return-ladder)");
        assertTrue(!gen.contains("return null;"),
                "the bare `return null;` for the `else empty` must be gone (whitespace-insensitive, "
                + "consistent with priceUnitOfMeasure_ladderRungTypedEmpty_positiveContent)");
    }

    /**
     * Positive-content (mechanism 2): {@code PriceUnitOfMeasure} must carry NO bare {@code return null;}
     * and TWO {@code return MapperS.<String>ofNull();} (the {@code then empty} rung + the empty terminal).
     * Reverting the rung-empty branch re-emits {@code return null;} for the {@code InterestRate} rung → RED.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void priceUnitOfMeasure_ladderRungTypedEmpty_positiveContent() throws IOException {
        assertNotNull(drrOutput, "drr generation did not run — corpus unavailable?");
        String gen = drrOutput.get(
                "drr/standards/iosco/cde/version1/price/reports/PriceUnitOfMeasureRule.java");
        assertNotNull(gen, "PriceUnitOfMeasure not generated");
        assertTrue(!gen.contains("return null;"),
                "a `then empty` rung must NOT render the bare `return null;` (PR #303 mechanism 2)");
        long typed = gen.split("return MapperS.<String>ofNull\\(\\);", -1).length - 1;
        assertEquals(2, typed,
                "both the `then empty` rung AND the empty terminal else must render "
                + "MapperS.<String>ofNull() (PR #303 mechanism 2)");
    }

    // ==== Green-safety locks (pass either way) ====

    /**
     * Green-safety (mechanism 1) — the effective (NON-empty) else branch is untouched. Within the
     * {@code Create_TradeState} flip carrier, the {@code valuation} alias has a REAL else
     * ({@code if notExists then observation else create_Valuation}); broadening {@code hasEffectiveElse}
     * to exclude {@code REmptyLiteral} must NOT divert this effective else to the typed empty —
     * {@code valuation} still ends {@code return MapperS.of(create_Valuation...)}. (Passes on clean
     * source too — the effective-else path was never broken.)
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void createTradeStateValuation_effectiveElse_untouched() throws IOException {
        assertNotNull(cdm6FunctionOutput, "cdm6 function generation did not run — corpus unavailable?");
        String gen = cdm6FunctionOutput.get("cdm/event/common/functions/Create_TradeState.java");
        assertNotNull(gen, "Create_TradeState not generated");
        assertTrue(gen.contains("return MapperS.of(create_Valuation.evaluate("),
                "an effective (non-empty) else must render its value, NOT the typed empty — the "
                + "hasEffectiveElse broadening only diverts an `empty` else (PR #303 mechanism 1 green-safety)");
    }

    /**
     * Green-safety (mechanism 2) — a green block-lambda ladder with a typed-empty TERMINAL (and no
     * empty rung) is byte-identical. {@code ActionTypeRule} (iosco cde version3, the #281 ladder carrier,
     * GREEN since #281) ends its ladder in {@code return MapperS.<ActionTypeEnum>ofNull();}; mechanism 2
     * hoists the terminal {@code itemType} resolution above the rung loop, which must produce the SAME
     * bytes (no rung is empty → {@code typedEmptyReturn} feeds only the terminal). Byte-matches either way.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void actionTypeIosco_ladderTypedEmptyTerminal_byteIdentical() throws IOException {
        assertDrrByteMatchesGolden(
                "drr/standards/iosco/cde/version3/event/reports/ActionTypeRule.java");
    }

    private static void assertDrrByteMatchesGolden(String path) throws IOException {
        assertNotNull(drrOutput, "drr generation did not run — corpus unavailable?");
        String generated = drrOutput.get(path);
        assertNotNull(generated, "Class not generated: " + path
                + " (emission failed or the path differs)");
        Path goldenPath = DRR_GOLDEN_DIR.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated drr output must byte-match the golden (newline-normalized) for "
                + path + " (PR #303 typedEmptyArm).");
    }

    private static void assertFnByteMatchesGolden(Map<String, String> functionOutput,
            Path goldenDir, String path) throws IOException {
        assertNotNull(functionOutput, "Function generation did not run — corpus unavailable?");
        String generated = functionOutput.get(path);
        assertNotNull(generated, "Function not generated: " + path);
        Path goldenPath = goldenDir.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated output must byte-match the golden (newline-normalized) for " + path
                + " (PR #303 typedEmptyArm).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
