package com.regnosys.rosetta.generator.java;

import com.regnosys.rosetta.generator.GenerationException;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;

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

/**
 * PR #253 — aliasCallMapWitness. A {@code .map(feature)} whose RECEIVER is an
 * <b>alias call</b> ({@code wtPeriod(...)} / {@code calculationResults(...)}, an
 * {@code RSymbolReference} to an {@code RShortcut}) whose alias body is an
 * <b>if-then-else</b>, OR a <b>distinct</b> list-op chain, dropped BOTH (1) the
 * {@code <Type>} generic witness and (2) the type-named lambda param — gen emitted
 * {@code receiver.map("getX", _name -> _name.getX())} where golden emits
 * {@code receiver.<T>map("getX", <elementType> -> <elementType>.getX())}.
 *
 * <p>ONE root cause: {@code NavigationHandler.resolveReceiverDataType} — the SAME walk
 * both the witness path ({@code fallbackResolveFeature}) and the lambda-var path read —
 * had no arm for two element-type-bearing receiver shapes, so the receiver's element
 * type never resolved and BOTH derivations fell back:
 *
 * <ul>
 *   <li><b>if-then-else alias body</b> ({@code alias wtPeriod: if (lookback exists)
 *       then adjustedCalculationPeriod else observationPeriod}) — a new
 *       {@code RConditionalExpr} arm types it as the IDENTICAL-OR-DECLINE join of the
 *       then/else branch types (mirroring the existing {@code RListLiteral} arm);
 *       {@code EvaluateCalculatedRate}'s branches are two functions both output
 *       {@code CalculatedRateDetails}.</li>
 *   <li><b>distinct chain</b> ({@code … -> reason distinct only-element -> value}) — a
 *       new {@code DISTINCT}/{@code REVERSE} arm (element-type transparent) recurses
 *       into the list-op argument.</li>
 * </ul>
 *
 * <p>GREEN-SAFE: both arms are strictly ADDITIVE — these receiver shapes previously
 * returned {@code null}. Golden ALWAYS emits the {@code <T>} witness + type-named param
 * on these shapes (its full type resolution never drops them), so no green file carries
 * the witness-less form on a conditional/distinct receiver — the fix can only ADD a
 * correct witness where one was missing (a currently-divergent, waivered file). Making
 * the distinct/conditional shapes traversable exposed a latent recursion cycle (a
 * then-body that pipes the implicit item back through itself, resolved via
 * {@code implicitItemDataType}); {@code resolveReceiverDataType} now carries an ON-STACK
 * identity cycle guard (add-on-entry / pop-on-return) — remove-on-exit so a legitimate
 * DIAMOND (the same node reached via sibling branches, e.g. a {@code min [ alias -> a,
 * alias -> b ]} list-literal comparator key) succeeds on both visits while only a true
 * cycle is declined.
 *
 * <p>Anchors are WHOLE-FILE byte comparisons against the frozen goldens
 * (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}.
 */
class FunctionAliasCallMapWitnessTest {

    private static final Path CDM5_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-5.38.0");
    private static final Path CDM5_GOLDEN_DIR =
            CDM5_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static Map<String, String> cdm5FunctionOutput;
    private static Map<String, String> cdm6FunctionOutput;
    private static Map<String, String> drrFunctionOutput;

    static boolean cdm5CellAvailable() {
        return Files.isDirectory(CDM5_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM5_GOLDEN_DIR);
    }

    static boolean cdm6CellAvailable() {
        return Files.isDirectory(CDM6_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM6_GOLDEN_DIR);
    }

    static boolean drrCellAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR);
    }

    @BeforeAll
    static void generateFunctions() throws IOException {
        if (cdm5CellAvailable()) {
            cdm5FunctionOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("cdm", "5.38.0", CDM5_CELL_ROOT));
        }
        if (cdm6CellAvailable()) {
            cdm6FunctionOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CDM6_CELL_ROOT));
        }
        if (drrCellAvailable()) {
            drrFunctionOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT));
        }
    }

    private static Map<String, String> generateCell(D11CorpusRegressionTest.CellSpec cell)
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

    // ---- flip carriers (RConditionalExpr alias-body arm) ----

    /**
     * cdm5 {@code DetermineWeightingDates}: the alias {@code wtPeriod} body is
     * {@code if (lookback exists) then adjustedCalculationPeriod else observationPeriod}
     * (both branches {@code CalculationPeriodBase}). The {@code wtPeriod(...) -> adjustedEndDate}
     * step gains the {@code <Date>} witness + {@code calculationPeriodBase} lambda var (was the
     * witness-less {@code .map("getAdjustedEndDate", _wtPeriod -> ...)}). Reverting the
     * {@code RConditionalExpr} arm reverts this (RED).
     */
    @Test
    @EnabledIf("cdm5CellAvailable")
    void determineWeightingDatesCdm5_conditionalAliasBody_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5FunctionOutput, CDM5_GOLDEN_DIR,
                "cdm/observable/asset/calculatedrate/functions/DetermineWeightingDates.java");
    }

    /**
     * cdm5 {@code EvaluateCalculatedRate}: the alias {@code calculationResults} body is
     * {@code if isCompounding then ApplyCompoundingFormula(...) else ApplyAveragingFormula(...)}
     * (both functions output {@code CalculatedRateDetails} — the identical-or-decline join
     * resolves it via the function-output {@code RFunction} arm). The
     * {@code calculationResults(...) -> calculatedRate} step gains the {@code <BigDecimal>}
     * witness + {@code calculatedRateDetails} lambda var. Reverting the {@code RConditionalExpr}
     * arm reverts this (RED).
     */
    @Test
    @EnabledIf("cdm5CellAvailable")
    void evaluateCalculatedRateCdm5_conditionalFunctionBranches_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5FunctionOutput, CDM5_GOLDEN_DIR,
                "cdm/observable/asset/calculatedrate/functions/EvaluateCalculatedRate.java");
    }

    /**
     * cdm6 {@code DetermineWeightingDates}: the same {@code wtPeriod} conditional-alias-body
     * shape in the cdm6 cell — locks the fix across both cdm cells. Reverting the
     * {@code RConditionalExpr} arm reverts this (RED).
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void determineWeightingDatesCdm6_conditionalAliasBody_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/observable/asset/calculatedrate/functions/DetermineWeightingDates.java");
    }

    /**
     * cdm6 {@code EvaluateCalculatedRate}: the same {@code calculationResults}
     * conditional-function-branch shape in the cdm6 cell (1:1 flip-carrier coverage — both
     * cdm cells carry both conditional-alias carriers). Reverting the {@code RConditionalExpr}
     * arm reverts this (RED).
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void evaluateCalculatedRateCdm6_conditionalFunctionBranches_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/observable/asset/calculatedrate/functions/EvaluateCalculatedRate.java");
    }

    // ---- flip carrier (DISTINCT list-op arm) ----

    /**
     * cdm6 {@code MapExecutionAdviceToWorkflowStep}: the {@code … -> reason distinct
     * only-element -> value} chain. The {@code only-element} receiver recurses into
     * {@code distinct(...)}, whose new arm recurses into the {@code mapC("getReason")}
     * chain → {@code WithdrawalReason}. The {@code -> value} step gains the {@code <String>}
     * witness + {@code withdrawalReason} lambda var (was the witness-less
     * {@code .map("getValue", _value -> ...)}). Reverting the {@code DISTINCT} arm reverts
     * this (RED).
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapExecutionAdviceToWorkflowStepCdm6_distinctChain_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/message/functions/MapExecutionAdviceToWorkflowStep.java");
    }

    /**
     * cdm6 {@code MapExecutionAdviceRetractedToWorkflowStep}: the sibling carrier of
     * {@code MapExecutionAdviceToWorkflowStep} — the same {@code … -> reason distinct
     * only-element -> value} chain (1:1 flip-carrier coverage). Reverting the {@code DISTINCT}
     * arm reverts this (RED).
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapExecutionAdviceRetractedToWorkflowStepCdm6_distinctChain_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/message/functions/MapExecutionAdviceRetractedToWorkflowStep.java");
    }

    /**
     * drr {@code DeliveryTypeForProducts}: a drr-cell carrier whose multiple alias/distinct
     * map steps all resolve once the arms land (the census {@code dl=8} was several instances
     * of the SAME facet, not co-occupation). Locks the drr cell + that the fix flips a file
     * fully, not partially. Reverting either arm reverts this (RED).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void deliveryTypeForProductsDrr_aliasAndDistinct_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/regulation/common/functions/DeliveryTypeForProducts.java");
    }

    // ---- green-safety / cycle-guard decline lock ----

    /**
     * drr {@code FXSwapLeg1} (a GREEN, non-waivered file): its {@code min [ farLeg(product)
     * -> ... -> exchangedCurrency1, farLeg(product) -> ... -> exchangedCurrency2 ]} comparator
     * key is a list-literal DIAMOND — both elements navigate the SAME {@code farLeg} alias.
     * The {@code RListLiteral} join resolves each element via {@code resolveReceiverDataType},
     * so the shared alias body node is reached twice in one walk. An ADD-ONLY cycle guard would
     * decline the second visit (false cycle) → the min item type drops → the comparator chain
     * loses its witnesses → REGRESSION (empirically: an add-only first cut regressed
     * FXSwapLeg1/2). The ON-STACK guard (pop-on-return) clears the first element's path before
     * the second descends, so both resolve and this green file stays byte-identical. Asserting
     * it byte-matches golden locks the on-stack semantics: reverting to add-only reverts this
     * (RED).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void fxSwapLeg1Drr_listLiteralDiamond_staysGreen() throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/regulation/common/functions/FXSwapLeg1.java");
    }

    private static void assertByteMatchesGolden(Map<String, String> cellOutput, Path goldenDir, String path)
            throws IOException {
        assertNotNull(cellOutput, "Function generation did not run — corpus unavailable?");
        String generated = cellOutput.get(path);
        assertNotNull(generated, "Function not generated: " + path);
        Path goldenPath = goldenDir.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated output must byte-match the golden (newline-normalized) for " + path
                + " — a .map step off an if-then-else alias body (RConditionalExpr arm) or a distinct "
                + "chain (DISTINCT arm) must resolve the receiver's element type so the <Type> witness + "
                + "type-named lambda var render; the cycle guard must be on-stack so list-literal diamonds "
                + "stay green. Reverting an arm or the on-stack guard reverts this anchor (RED).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
