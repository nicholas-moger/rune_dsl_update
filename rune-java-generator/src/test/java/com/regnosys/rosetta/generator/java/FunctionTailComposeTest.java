package com.regnosys.rosetta.generator.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
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
 * PR #332 anchors — the FUNCTION-tail compose (27 flips; cdm5 5 + cdm6 12 + drr 10).
 *
 * <ul>
 *   <li><b>CP1 injectRuleDepInFunctions:</b> a plain FUNCTION body's rule reference
 *       injects the {@code @Inject <Name>Rule} field and renders the field receiver
 *       — the collector's {@code ruleGateSimpleName} gate and the renderer's
 *       {@code ruleFamilySeat} gate BOTH dropped, matching upstream
 *       {@code JavaDependencyProvider} (unconditional {@code RosettaRule} injection)
 *       + {@code ExpressionGenerator.callableWithArgsCall} ({@code RosettaRule}
 *       rendered identically to {@code Function}).</li>
 *   <li><b>CP2 ctorEmptyMultiList:</b> an {@code empty} constructor value into a
 *       MULTI attribute renders {@code Collections.<Item>emptyList()} (upstream
 *       {@code TypeCoercionService}'s empty representation) — the pre-#332 decline
 *       dropped WHOLE constructors to the legacy one-line placeholder, so one arm
 *       unlocked the whole ingest-ctor family.</li>
 *   <li><b>CP3 numeric compose:</b> {@code ctorNumericCoerceChainHoist} (a
 *       ctor-setter CHAIN value hoists {@code final <Item> <n> = <chain>.get();} +
 *       the null-guarded {@code intValueExact()}/{@code BigDecimal.valueOf()}
 *       conversion — both directions golden-witnessed) + {@code sumTypedEverywhere}
 *       (the #299/#328 rule/alias host gate dropped; {@code MapperC} has no generic
 *       {@code .sum()}, so every declined seat was non-compiling) +
 *       {@code countOperandBigDecimalValueOf} (a COUNT operand in a
 *       BigDecimal-joined {@code MapperMaths} context renders
 *       {@code MapperS.of(BigDecimal.valueOf(<x>.resultCount()))}).</li>
 *   <li><b>CP4 defaultOperandNullSafe + defaultLeftGetRewrap:</b> a {@code default}
 *       operand of {@code and/orNullSafe} — direct or as a then-chain body — wraps
 *       {@code ComparisonResult.ofNullSafe(...)} ({@code ComparisonResult.andNullSafe}
 *       takes ONLY {@code ComparisonResult}, receiver and argument), and a
 *       {@code .get()}-collapsed only-element default LEFT re-wraps
 *       {@code MapperS.of(...)} (the #243 law at the {@code getOrDefault} receiver
 *       seat).</li>
 *   <li><b>CP5 aliasRuleRefTyping:</b> an alias body's rule reference types the
 *       alias signature from the rule BODY's inferred type (the
 *       {@code RFunction.fromRule} synthetic output carries no {@code typeCall}).</li>
 * </ul>
 *
 * <p>Anchors are WHOLE-FILE byte comparisons of generated output against the frozen
 * 9.83.0 goldens (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached} FUNCTION-kind path
 * ({@code FunctionGenerator.generateWithErrors}).
 */
class FunctionTailComposeTest {

    private static final Path CDM5_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-5.38.0");
    private static final Path CDM5_GOLDEN_DIR =
            CDM5_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    // ==== CP1 flip carriers (FUNCTION-body rule refs — dotted cross-namespace
    // `cde.datetime.EffectiveDate` and 1-call `cde.price.PriceNoFormat` shapes) ====
    private static final String DRR_GET_EXPIRATION_DATE =
            "drr/regulation/common/functions/GetExpirationDate.java";
    private static final String DRR_IS_DEFAULT_PRICE =
            "drr/regulation/common/trade/price/functions/IsDefaultPrice.java";

    // ==== CP2 flip carriers ====
    // Nested ctor-as-setter-value: multi-line + new ArrayList(x) + emptyList + bare .build().
    private static final String CDM6_CREATE_ROLL =
            "cdm/event/common/functions/Create_RollPrimitiveInstruction.java";
    // The #198 singletonList hoist composing over a nested ctor whose inner `street: empty`
    // previously declined the whole block.
    private static final String CDM6_MAP_COUNTRY =
            "cdm/ingest/fpml/confirmation/party/functions/MapCountryToContactInformation.java";
    // Chain-arg `.get()` coercion + `.setDatedValue(Collections.<DatedValue>emptyList())`.
    private static final String CDM5_RESOLVE_EQUITY_INITIAL_PRICE =
            "cdm/product/asset/functions/ResolveEquityInitialPrice.java";

    // ==== CP3 flip carriers ====
    // The narrowing hoist: final BigDecimal bigDecimal = <chain>.get(); + intValueExact().
    private static final String DRR_CREATE_FLOATING_RATE =
            "drr/projection/iso20022/mas/rewrite/trade/functions/Create_FloatingRate.java";
    // The widening hoist: final Integer integer = <chain>.get(); + BigDecimal.valueOf(integer).
    private static final String CDM6_MAP_PARAMETRIC_DATES =
            "cdm/ingest/fpml/confirmation/datetime/functions/MapParametricDates.java";
    // sumBigDecimal at the MapperMaths-operand seat + the count BigDecimal.valueOf wrap.
    private static final String CDM5_RESOLVE_OBSERVATION_AVERAGE =
            "cdm/observable/event/functions/ResolveObservationAverage.java";

    // ==== CP4 flip carrier (both arms: ofNullSafe on the direct + then-chain default
    // operands, and the only-element default-left MapperS.of re-wrap) ====
    private static final String DRR_GET_OR_FETCH_MIC_DATA =
            "drr/standards/iso/functions/GetOrFetchMicData.java";

    // ==== CP5 flip carrier (alias signature from the rule's inferred String output) ====
    private static final String DRR_DIRECTION2 =
            "drr/standards/iosco/cde/version1/party/functions/Direction2.java";

    // ==== Green pins ====
    // The count wrap's DECLINE side: an <Integer, Integer, Integer> MapperMaths context
    // keeps the bare MapperS.of(<x>.resultCount()) — no BigDecimal.valueOf.
    private static final String CDM6_QUALIFY_CASH_TRANSFER_GREEN =
            "cdm/event/qualification/functions/Qualify_CashTransfer.java";
    // The pre-existing boolean-FUNCTION ofNullSafe arm's carrier — the new RDefaultExpr
    // arm must not double-wrap an operand that is not a default.
    private static final String CDM6_QUALIFY_PARTIAL_NOVATION_GREEN =
            "cdm/event/qualification/functions/Qualify_PartialNovation.java";

    private static Map<String, String> cdm5Output;
    private static Map<String, String> cdm6Output;
    private static Map<String, String> drrOutput;

    static boolean cellsAvailable() {
        // Every generated cell's SOURCE dir is checked (not just cdm5's) — a partially
        // cloned corpus (cdm5 present, cdm6/drr absent) must skip rather than fail at
        // generation time (Copilot #332 R1).
        return Files.isDirectory(CDM5_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM6_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM5_GOLDEN_DIR)
                && Files.isDirectory(CDM6_GOLDEN_DIR)
                && Files.isDirectory(DRR_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (cellsAvailable()) {
            cdm5Output = generateFunctions(
                    new D11CorpusRegressionTest.CellSpec("cdm", "5.38.0", CDM5_CELL_ROOT));
            cdm6Output = generateFunctions(
                    new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CDM6_CELL_ROOT));
            drrOutput = generateFunctions(
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

    // ==== CP1 flip locks (revert-RED) ====

    @Test
    @EnabledIf("cellsAvailable")
    void getExpirationDate_functionBodyRuleRefsInject_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrOutput, DRR_GOLDEN_DIR, DRR_GET_EXPIRATION_DATE);
    }

    @Test
    @EnabledIf("cellsAvailable")
    void isDefaultPrice_functionBodyRuleRefInjects_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrOutput, DRR_GOLDEN_DIR, DRR_IS_DEFAULT_PRICE);
    }

    // ==== CP2 flip locks (revert-RED) ====

    @Test
    @EnabledIf("cellsAvailable")
    void createRoll_nestedCtorEmptyMultiList_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6Output, CDM6_GOLDEN_DIR, CDM6_CREATE_ROLL);
    }

    @Test
    @EnabledIf("cellsAvailable")
    void mapCountry_singletonHoistOverNestedCtor_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6Output, CDM6_GOLDEN_DIR, CDM6_MAP_COUNTRY);
    }

    @Test
    @EnabledIf("cellsAvailable")
    void resolveEquityInitialPrice_ctorChainArgsAndEmptyList_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(cdm5Output, CDM5_GOLDEN_DIR, CDM5_RESOLVE_EQUITY_INITIAL_PRICE);
    }

    // ==== CP3 flip locks (revert-RED) ====

    @Test
    @EnabledIf("cellsAvailable")
    void createFloatingRate_chainNarrowHoistIntValueExact_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(drrOutput, DRR_GOLDEN_DIR, DRR_CREATE_FLOATING_RATE);
    }

    @Test
    @EnabledIf("cellsAvailable")
    void mapParametricDates_chainWideningHoistBigDecimalValueOf_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(cdm6Output, CDM6_GOLDEN_DIR, CDM6_MAP_PARAMETRIC_DATES);
    }

    @Test
    @EnabledIf("cellsAvailable")
    void resolveObservationAverage_sumBigDecimalAndCountValueOf_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(cdm5Output, CDM5_GOLDEN_DIR, CDM5_RESOLVE_OBSERVATION_AVERAGE);
    }

    // ==== CP4 flip lock (revert-RED) ====

    @Test
    @EnabledIf("cellsAvailable")
    void getOrFetchMicData_defaultOperandOfNullSafeAndLeftRewrap_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(drrOutput, DRR_GOLDEN_DIR, DRR_GET_OR_FETCH_MIC_DATA);
    }

    // ==== CP5 flip lock (revert-RED) ====

    @Test
    @EnabledIf("cellsAvailable")
    void direction2_aliasSignatureFromRuleInferredOutput_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(drrOutput, DRR_GOLDEN_DIR, DRR_DIRECTION2);
        // The load-bearing content: the alias signature types MapperS<String> from the
        // Counterparty1 rule's inferred String output, not the enclosing enum leak.
        assertTrue(drrOutput.get(DRR_DIRECTION2)
                        .contains("MapperS<String> reportingParty("),
                "the alias signature must type from the rule's inferred output");
    }

    // ==== Green-safety pins (pass on clean source AND with the facets) ====

    @Test
    @EnabledIf("cellsAvailable")
    void qualifyCashTransfer_integerCountContextStaysBare_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(cdm6Output, CDM6_GOLDEN_DIR, CDM6_QUALIFY_CASH_TRANSFER_GREEN);
        assertFalse(cdm6Output.get(CDM6_QUALIFY_CASH_TRANSFER_GREEN)
                        .contains("BigDecimal.valueOf"),
                "an <Integer,...> MapperMaths count operand must stay bare");
    }

    @Test
    @EnabledIf("cellsAvailable")
    void qualifyPartialNovation_existingOfNullSafeArmNotDoubleWrapped_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(cdm6Output, CDM6_GOLDEN_DIR,
                CDM6_QUALIFY_PARTIAL_NOVATION_GREEN);
    }

    // ==== helpers ====

    private static void assertByteMatchesGolden(Map<String, String> output, Path goldenDir,
            String path) throws IOException {
        assertNotNull(output, "cell output not generated");
        String gen = output.get(path);
        assertNotNull(gen, "missing generated output: " + path);
        Path goldenPath = goldenDir.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "missing golden: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(gen),
                path + " must byte-match the frozen 9.83.0 golden");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
