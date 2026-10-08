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
 * Anchor for facet {@code consumerGetWrap} (PR #313): an evaluate-arg whose compiled source ALREADY
 * ends in a bare-item collapse {@code .get()} (an {@code only-element}/{@code first}/{@code last}
 * terminal on a {@code Mapper}) is a BARE ITEM, not a {@code Mapper} —
 * {@code ReferenceHandler.unwrapForEvaluateArg}'s fall-through would append ANOTHER {@code .get()},
 * emitting the non-compiling {@code <…>.get().get()}. golden re-wraps the collapsed bare item
 * {@code MapperS.of(<…>.get()).get()} (a behaviour-neutral single-wrap/unwrap round-trip) so the
 * arg-collapse accessor is valid. The fall-through now detects a source ending in {@code .get()} and
 * wraps {@code MapperS.of(source)} before the accessor (+ {@code MAPPER_S} ref).
 *
 * <p>Carriers (2): {@code DTCC_TradeParty1TransactionIDRule} cftc/csa VALUATION — {@code GetInternalId}'s
 * arg3 ({@code reportableInformation -> transactionInformation filter [supervisoryBody = CFTC] then
 * cftcTransactionInformation -> internalTradeIdentifier only-element}; the {@code only-element} renders
 * {@code <MapperC>.get()}). The 2 dtcc variants got the same wrap (toward-golden) but stay divergent on
 * a {@code _thenArg} scope-collision escape (the fork renders the outer SET-path thenArg as a literal,
 * invisible to {@code GeneratorScope.isNameTaken}, so a same-named nested-lambda hoist does not escape —
 * a systematic scope-structure divergence, DEFERRED).
 *
 * <p>Green-safe by construction: ZERO of the 34,686 goldens carry {@code .get().get()}, so every arg
 * reaching this branch with a {@code .get()}-terminal source is an already-waivered non-compiling
 * mismatch (a green file cannot carry the firing shape). SHARED rule+function seat but
 * FUNCTION-byte-NEUTRAL (cdm5 79 / cdm6 232 / drr 206 FUNCTION mismatch UNCHANGED, cdm/iso/fpml POJO
 * byte-IDENTICAL): a function evaluate-arg whose source is a plain {@code Mapper} nav (the
 * {@code .get()} is APPENDED by the fall-through) does NOT end in {@code .get()}, so the gate declines.
 *
 * <p>Anchors are WHOLE-FILE byte comparisons of the generated drr output against the frozen goldens
 * (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}. REVERT-VERIFIED RED 3/5 — the 2 flip locks +
 * the positive-content lock fail on clean source; the 2 green-safety locks pass either way.
 */
class RuleConsumerGetWrapTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static final String CFTC_VALUATION =
            "drr/regulation/cftc/rewrite/valuation/reports/DTCC_TradeParty1TransactionIDRule.java";
    private static final String CSA_VALUATION =
            "drr/regulation/csa/rewrite/valuation/reports/DTCC_TradeParty1TransactionIDRule.java";
    private static final String CLEARED_ASIC =
            "drr/regulation/asic/rewrite/trade/reports/ClearedRule.java";

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

    // ==== Flip locks (revert-RED): the carriers now byte-match golden. ====

    /** Flip — DTCC_TradeParty1TransactionIDRule cftc VALUATION: GetInternalId's arg3 consumer-wrap. */
    @Test
    @EnabledIf("drrCellAvailable")
    void cftcValuation_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(CFTC_VALUATION);
    }

    /** Flip — DTCC_TradeParty1TransactionIDRule csa VALUATION: the same consumer-wrap. */
    @Test
    @EnabledIf("drrCellAvailable")
    void csaValuation_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(CSA_VALUATION);
    }

    // ==== Positive-content lock (revert-RED): the MapperS.of wrap, not just byte-match. ====

    /**
     * arg3 ({@code … internalTradeIdentifier only-element}) is re-wrapped
     * {@code MapperS.of(thenArg.<…>.<…>.get()).get()}, NOT the fork's non-compiling
     * {@code thenArg.<…>.<…>.get().get()}. The {@code only-element} already collapsed the MapperC to a
     * bare {@code .get()}; the fall-through's second {@code .get()} would be invalid on a bare item.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void cftcValuation_arg3RendersMapperSWrap() {
        String gen = gen(CFTC_VALUATION);
        assertTrue(gen.contains(
                "MapperS.of(thenArg.<CommonTransactionInformation>map(\"getCftcTransactionInformation\", "
                + "transactionInformation -> transactionInformation.getCftcTransactionInformation())"
                + ".<String>map(\"getInternalTradeIdentifier\", commonTransactionInformation -> "
                + "commonTransactionInformation.getInternalTradeIdentifier()).get()).get()"),
                "Expected arg3's collapsed bare item re-wrapped MapperS.of(<…>.get()).get()");
        assertTrue(!gen.contains("getInternalTradeIdentifier()).get().get()"),
                "The fork's non-compiling .get().get() double-collapse must be gone");
    }

    // ==== Green-safety locks (pass on clean source too). ====

    /**
     * Green-safety — arg2 ({@code reportingSide}) STAYS bare {@code item.<ReportingSide>map(…).get()},
     * NOT wrapped: its compiled source is a plain {@code MapperS} nav ({@code item.<ReportingSide>map(…)},
     * no trailing {@code .get()} — the {@code .get()} is APPENDED by the fall-through), so
     * {@code source.endsWith(".get()")} is false and the gate declines. Demonstrates the gate
     * discriminates a plain-nav evaluate-arg (arg2) from an already-collapsed one (arg3). Passes on
     * clean source (arg2 was always bare).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void cftcValuation_reportingSideArg_staysBare() {
        String gen = gen(CFTC_VALUATION);
        assertTrue(gen.contains(
                "item.<ReportingSide>map(\"getReportingSide\", valuationReportInstruction -> "
                + "valuationReportInstruction.getReportingSide()).get()"),
                "arg2 (reportingSide) must stay a bare nav .get()");
        assertTrue(!gen.contains(
                "MapperS.of(item.<ReportingSide>map(\"getReportingSide\""),
                "arg2 (a plain-nav source, not ending in .get()) must NOT be wrapped MapperS.of(...)");
    }

    /**
     * Green-safety — a GREEN rule ({@code ClearedRule} asic) with an evaluate-arg through the SAME
     * fall-through stays byte-identical: its arg {@code isCleared.evaluate(item.<WorkflowStep>map(
     * "getOriginatingWorkflowStep", …).get())} has a plain {@code MapperS} nav source (the {@code .get()}
     * is APPENDED), so the gate declines and the file keeps byte-matching golden. Proves the fix does
     * not over-fire on the broad green evaluate-arg population.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void clearedAsic_evaluateArg_staysBareAndByteMatches() throws IOException {
        String gen = gen(CLEARED_ASIC);
        assertTrue(gen.contains(
                "isCleared.evaluate(item.<WorkflowStep>map(\"getOriginatingWorkflowStep\", "
                + "transactionReportInstruction -> transactionReportInstruction.getOriginatingWorkflowStep())"
                + ".get())"),
                "ClearedRule's plain-nav evaluate-arg must stay bare .get()");
        assertTrue(!gen.contains(
                "MapperS.of(item.<WorkflowStep>map(\"getOriginatingWorkflowStep\""),
                "ClearedRule's plain-nav evaluate-arg must NOT be wrapped");
        assertByteMatchesGolden(CLEARED_ASIC);
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
                + path + " (PR #313 consumerGetWrap).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
