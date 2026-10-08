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
 * Facet {@code ifthenelse_result_hoisting} (the ITE arm of the statement-hoisting
 * family, deferred at PR #172) — upstream compiles EVERY Rosetta if-then-else
 * EXPRESSION as a hoisted statement local, never an inline
 * {@code cond.getOrDefault(false) ? A : B} ternary (the goldens carry ZERO such
 * ternaries): upstream {@code caseConditionalExpression} always composes a
 * {@code JavaIfThenElseBuilder}, and any mid-expression consumption collapses it
 * via {@code declareAsVariable(true, "ifThenElseResult", scope)} into
 * <pre>
 *   final &lt;Type&gt; ifThenElseResultN;
 *   if (&lt;cond&gt;.getOrDefault(false)) { ifThenElseResultN = &lt;then&gt;; }
 *   else { ifThenElseResultN = &lt;else&gt;; }
 * </pre>
 * with each branch coerced to the local's DECLARED type, or — when the else
 * value is the {@code null} literal (upstream's JavaLiteral-else special form,
 * the dominant elseless shape) — the NON-final initializer form
 * {@code <Type> ifThenElseResultN = null;} plus an if-block with NO else.
 * Numbering follows the {@code computeActualNames} group law over the WHOLE
 * assignOutput body (one upstream {@code assignOutputBodyScope} spans all
 * operations): a singleton group keeps the bare {@code ifThenElseResult};
 * &ge;2 number {@code 0..n-1} in source order. Nested/chained Rosetta
 * conditionals FOLD into the same local ({@code } else if (} ladders /
 * nested if-blocks) — they never mint a second local.
 *
 * <p>Two fork arms:
 *
 * <ol>
 *   <li><b>A1 — pathed conditional SET.</b> A SET with a non-empty segment path
 *       whose body is a conditional hoists the local typed from the path-LEAF
 *       attribute (concrete meta wrapper when the leaf is meta-annotated) and
 *       consumes it through the standard {@code .getOrCreateX().setY(local)}
 *       builder chain — the fork previously routed these through
 *       {@code renderConditionalAssignment}, assigning raw arm values to the
 *       ROOT output builder variable with the setter path dropped
 *       (non-compiling). An absent Rosetta else synthesizes {@code null}
 *       (initializer form), or {@code <Wrapper>.builder().build()} for a meta
 *       leaf (final/else form) — cdm {@code ConvertToAdjustableOrRelativeDate},
 *       {@code Create_EffectiveOrTerminationDateTermChangeInstruction}, drr
 *       {@code Create_ReportableEventFromInstruction}.</li>
 *   <li><b>A2 — ComparisonResult logical-operand conditional.</b> A conditional
 *       consumed as an {@code and}/{@code or} operand (argument or receiver of
 *       {@code andNullSafe}/{@code orNullSafe}) compiles against the expected
 *       {@code ComparisonResult} and hoists at the consuming statement's indent
 *       — including INSIDE a conditional-assignment branch (the #172-S2
 *       placement). Branch coercions: an already-ComparisonResult arm passes
 *       through; a boolean literal arm wraps
 *       {@code ComparisonResult.ofNullSafe(MapperS.of(true|false))}; an absent
 *       else synthesizes {@code ComparisonResult.ofEmpty()} (cdm
 *       {@code Qualify_AssetClass_Equity},
 *       {@code Qualify_EquityOption_PriceReturnBasicPerformance_*}, drr
 *       {@code *_Validation}).</li>
 * </ol>
 *
 * <p>Deliberately OUT of scope (stay waivered): lambda-interior conditionals
 * (upstream's block-lambda early-return form — cdm {@code ReplaceParty});
 * raw-enum-typed locals with hoisted Boolean conditions (cdm6
 * {@code MapEntityIdentifierTypeEnum} — needs the S2b getOrDefault-ARG family);
 * constructor-field-value conditionals (drr asic {@code Create_TradeReport*} —
 * blocked by Cat-14 alias-sibling bare enums); the ReferenceWithMeta→FieldWithMeta
 * conversion producer ({@code ResolveInterestRateObservationIdentifiers} — same
 * hoist channel, different producer); ADD-operation conditionals; rule bodies
 * (the hoist session is bracketed around FUNCTION operation compilation only).
 *
 * <p>Green-safety: the goldens carry ZERO {@code getOrDefault(false) ? } inline
 * ternaries (verified across all 52,431 baseline files), so every file the
 * ternary form touches is already a waivered mismatch; and the pathed
 * conditional SET's previous root-variable assignment never compiled, so no
 * byte-matching file carries either pre-fix shape — a misjudged carrier stays
 * waivered rather than regressing.
 *
 * <p>Anchors are WHOLE-FILE byte comparisons against the frozen goldens
 * (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}.
 */
class FunctionIfThenElseHoistTest {

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

    /**
     * A1: seven pathed conditional SETs hoist {@code ifThenElseResult0..6} —
     * six elseless initializer-form locals ({@code Date/BusinessCenters/…
     * ifThenElseResultN = null;} + if-only) and ONE meta-leaf final/else form
     * ({@code final ReferenceWithMetaDate ifThenElseResult3;} with the
     * synthesized {@code ReferenceWithMetaDate.builder().build()} else), each
     * consumed by its {@code .getOrCreateRelativeDate().set…(local)} chain (cdm5).
     */
    @Test
    @EnabledIf("cdm5CellAvailable")
    void convertToAdjustableOrRelativeDateCdm5_pathedConditionalSetHoists_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(cdm5FunctionOutput, CDM5_GOLDEN_DIR,
                "cdm/base/datetime/functions/ConvertToAdjustableOrRelativeDate.java");
    }

    /** A1: the mirror function pins the same seven-hoist shape in the cdm6 cell. */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void convertToAdjustableOrAdjustedOrRelativeDateCdm6_pathedConditionalSetHoists_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/base/datetime/functions/ConvertToAdjustableOrAdjustedOrRelativeDate.java");
    }

    /**
     * A1: explicit-else conditionals take the final/else form
     * ({@code final AdjustableOrRelativeDate ifThenElseResult0;}) consumed by a
     * DEEP {@code .getOrCreateProduct()….setEffectiveDate(local)} chain (cdm5).
     */
    @Test
    @EnabledIf("cdm5CellAvailable")
    void createEffectiveOrTerminationDateTermChangeInstructionCdm5_explicitElseDeepSet_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(cdm5FunctionOutput, CDM5_GOLDEN_DIR,
                "cdm/event/common/functions/Create_EffectiveOrTerminationDateTermChangeInstruction.java");
    }

    /**
     * A1: three hoists with enum-literal and dependency-call arms
     * ({@code ifThenElseResult0 = ReportableActionEnum.ERROR;} /
     * {@code create_AcceptedWorkflowStepFromInstruction.evaluate(…)}) and a
     * depth-1 {@code .setOriginatingWorkflowStep(local)} consumer (drr).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void createReportableEventFromInstructionDrr_enumAndCallArms_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/enrichment/common/trade/functions/Create_ReportableEventFromInstruction.java");
    }

    /**
     * A2: a conditional as the LAST {@code andNullSafe} argument hoists a bare
     * {@code final ComparisonResult ifThenElseResult;} with the boolean-literal
     * else coerced {@code ComparisonResult.ofNullSafe(MapperS.of(true))} (cdm5).
     */
    @Test
    @EnabledIf("cdm5CellAvailable")
    void qualifyEquityOptionPriceReturnBasicPerformanceIndexCdm5_comparisonResultOperand_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(cdm5FunctionOutput, CDM5_GOLDEN_DIR,
                "cdm/product/qualification/functions/Qualify_EquityOption_PriceReturnBasicPerformance_Index.java");
    }

    /**
     * A2: TWO logical-operand conditionals in ONE statement number
     * {@code ifThenElseResult0}/{@code ifThenElseResult1} in expression order,
     * both blocks emitted consecutively before the consuming statement (cdm5).
     */
    @Test
    @EnabledIf("cdm5CellAvailable")
    void qualifyAssetClassEquityCdm5_twoOperandHoistsNumbered_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(cdm5FunctionOutput, CDM5_GOLDEN_DIR,
                "cdm/product/qualification/functions/Qualify_AssetClass_Equity.java");
    }

    /**
     * A2: a conditional as the RECEIVER of an {@code .andNullSafe(…)} chain
     * inside a conditional-assignment then-branch hoists at the BRANCH indent
     * with the elseless {@code ComparisonResult.ofEmpty()} synthesis (drr).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void priceScheduleEffectiveDateValidationDrr_branchIndentReceiverHoist_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/regulation/common/trade/price/functions/PriceScheduleEffectiveDate_Validation.java");
    }

    private static void assertByteMatchesGolden(Map<String, String> functionOutput,
            Path goldenDir, String path) throws IOException {
        assertNotNull(functionOutput, "Function generation did not run — corpus unavailable?");
        String generated = functionOutput.get(path);
        assertNotNull(generated, "Function not generated: " + path);
        Path goldenPath = goldenDir.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated output must byte-match the golden (newline-normalized) for "
                + path + " — the pathed conditional-SET hoist (A1), the "
                + "ComparisonResult logical-operand hoist (A2), the declaration-form "
                + "selection (initializer vs final/else), or the function-body "
                + "ifThenElseResultN numbering is missing or regressed, if the "
                + "ifthenelse_result_hoisting recovery is reverted.");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n");
    }
}
