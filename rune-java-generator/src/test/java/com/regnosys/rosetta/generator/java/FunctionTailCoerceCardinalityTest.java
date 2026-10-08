package com.regnosys.rosetta.generator.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.java.enums.EnumGenerator;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.function.LabelProviderGenerator;
import com.regnosys.rosetta.generator.java.function.LabelProviderGeneratorUtil;
import com.regnosys.rosetta.generator.java.object.ChoiceObjectGenerator;
import com.regnosys.rosetta.generator.java.object.ModelObjectGenerator;
import com.regnosys.rosetta.generator.java.reports.ReportGenerator;
import com.regnosys.rosetta.generator.java.reports.RuleGenerator;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.utils.DeepFeatureCallUtil;
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
 * PR #326 anchor — facet functionTailCoerceCardinality: the #325 FUNCTION-tail
 * re-opening continued with seven coordinated coerce/cardinality mechanisms
 * (6 byte flips: cdm5 2 + cdm6 2 + drr 2; the cdm5 siblings are byte-oracle-verified,
 * see the deliberate-gap note below).
 *
 * <ul>
 *   <li><b>F1 — the contains-operand pair</b> ({@code SetOperationHandler}): a bare
 *       no-args MULTI-output FUNCTION first operand wraps {@code MapperC.<X>of(...)}
 *       (the #298/#301/#302 {@code bareMultiOutputWitness} SOT at the contains seat),
 *       and an already-Mapper CLOSURE-PARAM operand structurally unwraps its
 *       {@code MapperS.of} double-wrap (then-declared params decline, the #180 gate)
 *       — IsAcceptedEicCode (drr).</li>
 *   <li><b>F2 — the FUNCTION-path then-terminal {@code .getMulti()}</b>
 *       ({@code FunctionExpressionRenderer}): the #272 rule arm mirrored — a
 *       MULTI-output function whose then-chain SET body ALSO computes MULTI
 *       collapses with {@code .getMulti()} (two-halves-agree) —
 *       ExtractRegimeInformation (drr).</li>
 *   <li><b>F3 — MapperListOfLists decl + alias element join</b>
 *       ({@code CollectionHandler.tryDeepThenHoist} k==0/k&gt;0 arms via the
 *       {@code producesListOfLists} SOT + {@code LiteralHandler.walkedItemType}'s
 *       RShortcut arm via the #238 {@code aliasDerivedThenArgItemType} body walk) —
 *       DetermineObservationPeriod (cdm5 + cdm6).</li>
 *   <li><b>F4 — the alias-call comparison operand</b> ({@code HandlerHelper} +
 *       {@code ComparisonHandler}): {@code findEnclosingFunction}/{@code findEnclosingRule}
 *       walk with a dedicated {@code CONTAINER_WALK_LIMIT} (the 64-step bound truncated
 *       the 86-comparison or-chain's deepest 26 operands → arg-less alias calls), and a
 *       NULL-typed META-alias operand retypes from the SAME FunctionAliasHelper
 *       signature walk so the EXISTING meta-strip fires (bare un-registered param iff
 *       MapperC, the #170 law) — Qualify_Transaction_OIS (cdm5 + cdm6).</li>
 * </ul>
 *
 * <p>Anchors are WHOLE-FILE byte comparisons of generated output against the frozen
 * 9.83.0 goldens (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}.
 *
 * <p><b>Deliberate coverage gap (the #325 S1 convention):</b> the cdm/5.38.0 flips —
 * Qualify_Transaction_OIS + DetermineObservationPeriod — have no byte lock here:
 * locking them would load a THIRD cell corpus for two same-family files. They are
 * verified by the PR #326 byte-oracle + probe dumps, and the cdm6 SAME-FAMILY locks
 * (identical rosetta bodies) cover both mechanisms.
 */
class FunctionTailCoerceCardinalityTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    // F1 flip carrier (drr): the contains-operand pair.
    private static final String IS_ACCEPTED_EIC_CODE =
            "drr/enrichment/eic/functions/IsAcceptedEicCode.java";
    // F2 flip carrier (drr): the function-path then-terminal .getMulti().
    private static final String EXTRACT_REGIME_INFORMATION =
            "drr/regulation/common/trade/party/functions/ExtractRegimeInformation.java";
    // F4 flip carrier (cdm6): the 86-operand or-chain alias comparison.
    private static final String QUALIFY_TRANSACTION_OIS =
            "cdm/product/qualification/functions/Qualify_Transaction_OIS.java";
    // F3 flip carrier (cdm6): MapperListOfLists decl + BusinessCenters element witness.
    private static final String DETERMINE_OBSERVATION_PERIOD =
            "cdm/observable/asset/calculatedrate/functions/DetermineObservationPeriod.java";
    // Green-safety pin (cdm6): GREEN IsHoliday's golden carries a BARE alias-call first
    // contains-operand (`contains(holidays(checkDate, businessCenters), MapperS.of(checkDate))`)
    // + a MapperS.of-wrapped INPUT second operand — F1a must NOT wrap an alias call (the
    // RFunction-symbol gate) and F1b must NOT unwrap a non-closure-param name.
    private static final String IS_HOLIDAY_GREEN =
            "cdm/base/datetime/functions/IsHoliday.java";
    // Green-safety pin (drr): GREEN IsEmirTradingVenue's golden contains-operand pair is a
    // LIST LITERAL + a nav chain — both F1 arms must decline; the file also exercises the
    // F2/F4-adjacent comparison seats and must stay byte-identical.
    private static final String IS_EMIR_TRADING_VENUE_GREEN =
            "drr/regulation/esma/emir/refit/trade/functions/IsEmirTradingVenue.java";

    private static Map<String, String> drrOutput;
    private static Map<String, String> cdm6Output;

    static boolean drrCellAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR);
    }

    static boolean cdm6CellAvailable() {
        return Files.isDirectory(CDM6_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM6_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (drrCellAvailable()) {
            drrOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT));
        }
        if (cdm6CellAvailable()) {
            cdm6Output = generateCell(
                    new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CDM6_CELL_ROOT));
        }
    }

    private static Map<String, String> generateCell(D11CorpusRegressionTest.CellSpec cell)
            throws IOException {
        var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(),
                D11CorpusRegressionTest.emissionFilter(cell));
        var typeUtil = new JavaTypeUtil();
        var typeTranslator = new JavaTypeTranslator(typeUtil);
        var enumGen = new EnumGenerator(gm);
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
                assertNoGenerationErrors(enumGen.generateClasses(model, version, output));
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

    @Test
    @EnabledIf("drrCellAvailable")
    void isAcceptedEicCode_containsOperandPair_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrOutput, DRR_GOLDEN_DIR, IS_ACCEPTED_EIC_CODE);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void extractRegimeInformation_thenTerminalGetMulti_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrOutput, DRR_GOLDEN_DIR, EXTRACT_REGIME_INFORMATION);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void qualifyTransactionOis_aliasOperandMetaCoerce_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6Output, CDM6_GOLDEN_DIR, QUALIFY_TRANSACTION_OIS);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void determineObservationPeriod_listOfListsDeclAndElementJoin_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(cdm6Output, CDM6_GOLDEN_DIR, DETERMINE_OBSERVATION_PERIOD);
    }

    // ==== Positive-content locks (revert-RED). ====

    @Test
    @EnabledIf("drrCellAvailable")
    void isAcceptedEicCode_wrapsBareMultiFnAndBaresClosureParam() {
        String gen = gen(drrOutput, IS_ACCEPTED_EIC_CODE);
        assertTrue(gen.contains(
                "contains(MapperC.<String>of(getAcceptedEicCodes.evaluate()), ec)"),
                "the contains pair must wrap the bare multi-fn first operand in"
                        + " MapperC.<String>of AND pass the closure param bare");
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void qualifyTransactionOis_derefsEveryOperandWithArgs() {
        String gen = gen(cdm6Output, QUALIFY_TRANSACTION_OIS);
        // ALL 86 or-chain operands carry BOTH the enclosing-input arg (the
        // CONTAINER_WALK_LIMIT fix — the deepest 26 rendered the arg-less
        // `floatingRateIndex()` pre-fix) AND the bare MapperC Type-coercion deref
        // (the null-typed-alias retype — 0 derefs pre-fix).
        int derefs = countOccurrences(gen,
                "floatingRateIndex(economicTerms).<FloatingRateIndexEnum>map(\"Type coercion\","
                        + " fieldWithMetaFloatingRateIndexEnum ->"
                        + " fieldWithMetaFloatingRateIndexEnum.getValue())");
        assertEquals(86, derefs,
                "every or-chain operand must render the arg-ful alias call + the bare"
                        + " MapperC Type-coercion deref");
        assertFalse(gen.contains("floatingRateIndex()"),
                "no operand may render the arg-less alias call (the walk-depth truncation)");
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void determineObservationPeriod_declaresListOfListsWithAliasElementWitness() {
        String gen = gen(cdm6Output, DETERMINE_OBSERVATION_PERIOD);
        assertTrue(gen.contains(
                "final MapperListOfLists<BusinessCenterEnum> thenArg ="
                        + " MapperC.<BusinessCenters>of(businessDays("),
                "the deep-then hoist must declare MapperListOfLists with the alias-element"
                        + " BusinessCenters list-literal witness");
    }

    // ==== Green-safety locks (pass with AND without the facets). ====

    @Test
    @EnabledIf("cdm6CellAvailable")
    void isHoliday_greenAliasContainsOperands_stayByteIdentical() throws IOException {
        assertByteMatchesGolden(cdm6Output, CDM6_GOLDEN_DIR, IS_HOLIDAY_GREEN);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void isEmirTradingVenue_greenListLiteralContains_staysByteIdentical() throws IOException {
        assertByteMatchesGolden(drrOutput, DRR_GOLDEN_DIR, IS_EMIR_TRADING_VENUE_GREEN);
    }

    private static int countOccurrences(String haystack, String needle) {
        int count = 0;
        int idx = 0;
        while ((idx = haystack.indexOf(needle, idx)) >= 0) {
            count++;
            idx += needle.length();
        }
        return count;
    }

    private static String gen(Map<String, String> output, String path) {
        assertNotNull(output, "generation did not run for the cell of " + path);
        String gen = output.get(path);
        assertNotNull(gen, "missing generated file: " + path);
        return gen;
    }

    private void assertByteMatchesGolden(Map<String, String> output, Path goldenDir,
            String path) throws IOException {
        String gen = gen(output, path);
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
