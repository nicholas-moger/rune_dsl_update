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
 * PR #302 — TWO disjoint green-safe GENERATOR mechanisms, 4 byte flips (2 drr POJO + 2 cdm FUNCTION),
 * the proven clean-compose follow-on path (the #298/#301 + #293 mechanisms extended to new seats).
 *
 * <p><b>Mechanism 1 — existsOperandMapperCWrap at the comparison-operand + list-literal-element seats
 * (2 drr POJO).</b> The #301 (C) cardinality-aware bare-invocation MapperC wrap, extended from the
 * exists-operand seat to two more bare-operand seats. A bare no-args FUNCTION operand whose callee
 * OUTPUT is MULTI wraps {@code MapperC.<X>of(<fn>.evaluate(...))} not {@code MapperS.of(...)}:
 * <ul>
 *   <li><b>Comparison operand</b> ({@code ComparisonHandler.wrapBareFunctionOperand} now passes the
 *   compiler): {@code PortfolioContainingNonReportableComponentIndicator} csa renders
 *   {@code areEqual(MapperC.<SupervisoryBodyEnum>of(supervisoryBodyForCSA.evaluate()), …)} — the
 *   #301 note's "future multi-output bare comparison operand" landed.</li>
 *   <li><b>List-literal element</b> ({@code LiteralHandler}'s #278 A2 arm now cardinality-aware):
 *   {@code ExchangeRate} iosco renders {@code MapperC.<PriceSchedule>of(contract_Price.evaluate(input))}
 *   for each of two {@code PriceSchedule (0..*)} sub-functions.</li>
 * </ul>
 * Both delegate to the shared {@code HandlerHelper.bareMultiOutputWitness} SOT (the witness resolver)
 * + {@code bareMultiOutputWitnessFqn} (the #301 Copilot R1 follow-on FQN-collision guard). A
 * SINGLE-output bare-fn operand keeps {@code MapperS.of} (the #256/#278 form unchanged). Green-safe by
 * construction: {@code MapperS.of} over a multi value is a non-compiling type mismatch, so only an
 * already-waivered file is ever touched.
 *
 * <p><b>Mechanism 2 — smallScaleBuySideListOfLists at the BASE thenArg (k==0) seat (2 cdm FUNCTION).</b>
 * The #293 list-of-lists decl-type fix only re-rooted on the PRECEDING then (k&gt;0); a then-chain whose
 * BASE is itself a {@code mapItemToList} extract ({@code MapperC.<X>of(<multi>).mapItemToList(...)} IS
 * thenArg0's value) declared the non-compiling {@code MapperC<X>}. {@code BusinessCenterHolidaysMultiple}
 * (cdm5+cdm6) now declares {@code MapperListOfLists<Date> thenArg0}; the next then re-roots
 * {@code thenArg0.flattenList()} → {@code MapperC<Date>}. The method itself was already
 * {@code mapItemToList} ({@code CollectionHandler}); only the thenArg0 decl was wrong. Reuses the same
 * {@code producesListOfLists} SOT (the #274 two-halves-agree pattern). Green-safe by construction: a
 * {@code MapperC<X> = <multi-extract>.mapItemToList(...)} assignment never compiled.
 *
 * <p><b>Gates (byte-oracle / stash-baseline 4, regscan 0 within-waiver regressions / 4 flipped-out /
 * 15 toward-golden + 15 neutral churn, all FUNCTION cells + cdm/iso/fpml POJO byte-IDENTICAL except the
 * 2 flips).</b> {@code comm -23} (POST vs same-session clean PRE) = exactly the 2 drr POJO carriers; the
 * 2 cdm FUNCTION flips separated by the same-session re-dump.
 *
 * <p>Anchors are WHOLE-FILE byte comparisons against the frozen 9.83.0 goldens (newline-normalized),
 * generated through the REAL {@link D11CorpusRegressionTest#loadCellCorpusCached}. REVERT-VERIFIED RED
 * for the flip locks + the positive-content lock.
 */
class RuleMapperCWrapListOfListsTest {

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

    // ==== Mechanism 1 flip locks (revert-RED): a MULTI-output bare FUNCTION operand wraps MapperC.of ====

    /**
     * Flip (comparison-operand seat): {@code PortfolioContainingNonReportableComponentIndicator} (csa
     * margin) compares the bare FUNCTION {@code supervisoryBodyForCSA} (output
     * {@code SupervisoryBodyEnum (0..*)}) in an {@code areEqual} — golden wraps
     * {@code areEqual(MapperC.<SupervisoryBodyEnum>of(supervisoryBodyForCSA.evaluate()), …)}; the fork
     * wrapped {@code MapperS.of(…)} (a non-compiling single over a multi value) before the fix.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void portfolioContainingCsa_multiOutputBareFnComparisonOperand_byteMatchesGolden() throws IOException {
        assertDrrByteMatchesGolden(
                "drr/regulation/csa/rewrite/margin/reports/PortfolioContainingNonReportableComponentIndicatorRule.java");
    }

    /**
     * Flip (list-literal-element seat): {@code ExchangeRate} (iosco cde version1) builds
     * {@code MapperC.<PriceSchedule>of([contract_Price, contract_StrikePrice])} over two bare FUNCTIONs
     * each returning {@code PriceSchedule (0..*)} — golden wraps each element
     * {@code MapperC.<PriceSchedule>of(contract_Price.evaluate(input))}; the fork wrapped
     * {@code MapperS.of(…)} (the #278 A2 single-wrap, non-compiling over a multi value).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void exchangeRateIosco_multiOutputBareFnListElement_byteMatchesGolden() throws IOException {
        assertDrrByteMatchesGolden(
                "drr/standards/iosco/cde/version1/price/reports/ExchangeRateRule.java");
    }

    /**
     * Positive-content lock: the comparison-operand seat actually emits the cardinality-aware MapperC
     * wrap. {@code PortfolioContaining} must contain
     * {@code MapperC.<SupervisoryBodyEnum>of(supervisoryBodyForCSA.evaluate())} (the multi wrap),
     * NOT the pre-fix {@code MapperS.of(supervisoryBodyForCSA.evaluate())}. Reverting the
     * ComparisonHandler compiler-pass re-emits the bare {@code MapperS.of} → RED.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void portfolioContaining_comparisonOperandMapperCWrap_positiveContent() throws IOException {
        assertNotNull(drrOutput, "drr generation did not run — corpus unavailable?");
        String gen = drrOutput.get(
                "drr/regulation/csa/rewrite/margin/reports/PortfolioContainingNonReportableComponentIndicatorRule.java");
        assertNotNull(gen, "PortfolioContaining not generated");
        assertTrue(gen.contains("MapperC.<SupervisoryBodyEnum>of(supervisoryBodyForCSA.evaluate())"),
                "a MULTI-output bare-fn comparison operand must wrap MapperC.<X>of — the cardinality-aware "
                + "wrap fired at the areEqual-operand seat (PR #302 mechanism 1, comparison seat)");
        assertTrue(!gen.contains("MapperS.of(supervisoryBodyForCSA.evaluate())"),
                "the pre-fix MapperS.of single-wrap of the multi-output operand must be gone");
    }

    // ==== Mechanism 1 green-safety / cardinality-gate locks: a SINGLE-output bare-fn operand keeps
    //      MapperS.of (the #256/#278 form), so the GREEN file stays byte-identical. ====

    /**
     * Green-safety (comparison-operand seat) — a SINGLE-output bare FUNCTION comparison operand keeps
     * {@code MapperS.of}. {@code UniqueTransactionIdentifierProprietary} (asic margin, GREEN) compares
     * {@code isMax32UpperCaseAlphanumericText} (a single-output Boolean predicate) —
     * {@code areEqual(MapperS.of(isMax32UpperCaseAlphanumericText.evaluate(item.get())), …)}. The
     * cardinality gate ({@code gm.isMulti(out)} false) declines the MapperC wrap, so the GREEN file
     * stays byte-identical.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void uniqueTransactionIdentifierProprietaryAsic_singleOutputBareFnComparison_keepsMapperSof()
            throws IOException {
        assertNotNull(drrOutput, "drr generation did not run — corpus unavailable?");
        String gen = drrOutput.get(
                "drr/regulation/asic/rewrite/margin/reports/UniqueTransactionIdentifierProprietaryRule.java");
        assertNotNull(gen, "UniqueTransactionIdentifierProprietary not generated");
        assertTrue(gen.contains("MapperS.of(isMax32UpperCaseAlphanumericText.evaluate(item.get()))"),
                "a SINGLE-output bare-fn comparison operand must keep MapperS.of — the cardinality gate "
                + "declines the MapperC wrap for a single-output callee");
        assertDrrByteMatchesGolden(
                "drr/regulation/asic/rewrite/margin/reports/UniqueTransactionIdentifierProprietaryRule.java");
    }

    /**
     * Green-safety (list-literal-element seat) — SINGLE-output bare FUNCTION list elements keep
     * {@code MapperS.of}. {@code SettlementLocation} (iosco cde version1, GREEN — the #278 carrier)
     * builds {@code MapperC.<SettlementTerms>of(MapperS.of(settlementTermsLeg1.evaluate(item.get())),
     * MapperS.of(settlementTermsLeg2.evaluate(item.get())))} over two SINGLE-output sub-functions. The
     * cardinality gate declines the MapperC element wrap, preserving the #278 {@code MapperS.of} form,
     * so the GREEN file stays byte-identical.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void settlementLocationIosco_singleOutputBareFnListElement_keepsMapperSof() throws IOException {
        assertNotNull(drrOutput, "drr generation did not run — corpus unavailable?");
        String gen = drrOutput.get(
                "drr/standards/iosco/cde/version1/execution/reports/SettlementLocationRule.java");
        assertNotNull(gen, "SettlementLocation not generated");
        assertTrue(gen.contains("MapperS.of(settlementTermsLeg1.evaluate(item.get()))"),
                "a SINGLE-output bare-fn list element must keep MapperS.of — the cardinality gate "
                + "declines the MapperC element wrap for a single-output callee");
        assertDrrByteMatchesGolden(
                "drr/standards/iosco/cde/version1/execution/reports/SettlementLocationRule.java");
    }

    // ==== Mechanism 2 flip locks (revert-RED): a BASE (k==0) mapItemToList extract declares
    //      MapperListOfLists, not MapperC. ====

    /**
     * Flip (cdm6): {@code BusinessCenterHolidaysMultiple} —
     * {@code businessCenters extract [businessCenterHolidays]} (both MULTI) is the BASE thenArg0 whose
     * value is {@code MapperC.<BusinessCenterEnum>of(businessCenters).mapItemToList(...)}. Golden
     * declares {@code MapperListOfLists<Date> thenArg0}; the fork declared the non-compiling
     * {@code MapperC<Date>} (the #293 k&gt;0 check did not cover the BASE seat).
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void businessCenterHolidaysMultipleCdm6_listOfListsBaseDecl_byteMatchesGolden() throws IOException {
        assertFnByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/base/datetime/functions/BusinessCenterHolidaysMultiple.java");
    }

    /** Flip (cdm5): the same {@code BusinessCenterHolidaysMultiple} BASE list-of-lists shape in cdm5. */
    @Test
    @EnabledIf("cdm5CellAvailable")
    void businessCenterHolidaysMultipleCdm5_listOfListsBaseDecl_byteMatchesGolden() throws IOException {
        assertFnByteMatchesGolden(cdm5FunctionOutput, CDM5_GOLDEN_DIR,
                "cdm/base/datetime/functions/BusinessCenterHolidaysMultiple.java");
    }

    /**
     * Positive-content lock: the BASE thenArg0 declares {@code MapperListOfLists<Date>} while the
     * downstream {@code flattenList()} then re-roots {@code MapperC<Date> thenArg1} (the existing #293
     * {@code producesListOfLists(flatten)} false). Reverting the k==0 base check re-emits
     * {@code MapperC<Date> thenArg0} → RED.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void businessCenterHolidaysMultiple_listOfListsBaseDecl_positiveContent() throws IOException {
        assertNotNull(cdm6FunctionOutput, "cdm6 function generation did not run — corpus unavailable?");
        String gen = cdm6FunctionOutput.get("cdm/base/datetime/functions/BusinessCenterHolidaysMultiple.java");
        assertNotNull(gen, "BusinessCenterHolidaysMultiple not generated");
        assertTrue(gen.contains("final MapperListOfLists<Date> thenArg0 = MapperC.<BusinessCenterEnum>of(businessCenters)"),
                "the BASE mapItemToList extract must declare MapperListOfLists<Date> thenArg0 (PR #302 mechanism 2)");
        assertTrue(gen.contains("final MapperC<Date> thenArg1 = thenArg0"),
                "the downstream flattenList() then must re-root MapperC<Date> thenArg1 (the #293 producesListOfLists(flatten) declines)");
    }

    /**
     * Green-safety / decline lock — a green cdm6 function whose BASE thenArg is NOT a
     * {@code mapItemToList} extract keeps its {@code MapperC} wrapper (the k==0 listOfLists check
     * declines). {@code ExtractCounterpartyByRole} (GREEN) declares a {@code MapperC<…> thenArg0}
     * over a non-list-of-lists base, so the producesListOfLists(base) gate is false and the file
     * stays byte-identical.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void extractCounterpartyByRoleCdm6_nonListOfListsBase_keepsMapperC() throws IOException {
        assertFnByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/base/staticdata/party/functions/ExtractCounterpartyByRole.java");
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
                + path + " (PR #302 mapperCWrapOperandSeats / listOfListsBaseDecl).");
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
                + " (PR #302 listOfListsBaseDecl).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
