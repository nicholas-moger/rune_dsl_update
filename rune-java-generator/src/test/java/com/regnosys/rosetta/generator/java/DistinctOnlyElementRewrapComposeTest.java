package com.regnosys.rosetta.generator.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.java.enums.EnumGenerator;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.object.ChoiceObjectGenerator;
import com.regnosys.rosetta.generator.java.object.ModelObjectGenerator;
import com.regnosys.rosetta.generator.java.reports.ReportGenerator;
import com.regnosys.rosetta.generator.java.reports.RuleGenerator;
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
 * PR #359 — the distinct only-element re-wrap + the near-flip machinery sweep: 24 byte
 * flips (20 drr POJO Rules + 4 drr FUNCTION) + 0 new / 0 away (regscan359 — 11 movers
 * ALL content-verified TOWARD; the cdm cells byte-frozen at every checkpoint).
 *
 * <p><b>F-1 — {@code distinctOnlyElementRewrap}</b> (CollectionHandler): the
 * {@code then distinct only-element} sibling of the bare-item only-element re-wrap at
 * the deep-then decl seat — the collapse renders the type-less
 * {@code distinct(thenArgN).get()} (the #168-armA prefix-distinct law), re-wrapped
 * {@code MapperS.of(distinct(thenArg0).get())} (the ExecutionAgent family ×12).
 *
 * <p><b>F-2 — {@code toEnumImplicitItemLeaf}</b> (ConversionHandler): the C5 to-enum
 * meta-deref resolves a BARE implicit-item source through the {@code implicitItemArgument}
 * owner walk (then-chain last-step + extract-step-body recursion), so the
 * {@code FieldWithMetaString → String} Type-coercion step fires before the
 * {@code checkedMap} (CountryOfCounterparty2Rule).
 *
 * <p><b>F-3 — {@code distinctElementPreserve}</b> (CollectionHandler): a bare-item
 * DISTINCT level's decl element anchors to the RECEIVER's item via prevRef render truth
 * (the flatten/i1b law — golden {@code final MapperC<FieldWithMetaString> thenArg8 =
 * distinct(thenArg7);}, PriorUTIRule iosco).
 *
 * <p><b>F-4 — {@code dateTimeRecordFeatureNav}</b> (NavigationHandler):
 * {@code resolveReceiverRType}'s disguised-EVR arm gains the bare-INVOKABLE head
 * (resolvedSymbol → RFunction → the callee OUTPUT's valueName attribute), so the
 * {@code -> date} record nav fires over fn-rooted 2-name disguises
 * ({@code PositionForEvent -> openDateTime -> date} → {@code .<Date>map("Date", dt ->
 * Date.of(dt.toLocalDate()))}, EffectiveDateRule iosco).
 *
 * <p><b>F-5 — {@code bareFnMetaLeafTerminal}</b> (ReferenceHandler): the #280 meta-leaf
 * decline admits a chain-TERMINAL meta leaf outside conditional arms — the wrapper flows
 * to coordinated consumer seats (the mapSingleToList element + the pre-existing outer
 * coercion; the thenArg decl's #204/#144 wrapper recovery), while mid-chain hops and
 * ladder arms keep the decline (IdentifierOfBasketConstituents iosco +
 * NotionalAmountScheduleLeg1/2 v1/v3).
 *
 * <p><b>F-7 — {@code fnPathChainedBareFunctionThen}</b> (FunctionExpressionRenderer +
 * CollectionHandler): {@code isHoistableThenChain} admits a bare-FUNCTION then-body on
 * the FUNCTION path (the #276-deferred facet its javadoc named) and the terminal
 * {@code MapperS.of} wrap's rule gate drops in lockstep (NotionalAmountLeg1/2).
 *
 * <p><b>F-8 — {@code aliasLadderTerminalThenHoist}</b> (FunctionExpressionRenderer): the
 * alias return-ladder's terminal else drains its own sink window
 * ({@code statementHoistMark}/{@code drainStatementHoistsSince}) and splices the
 * deep-then decls before the return — the #350 strand class at this seat (PriceOfEvent).
 *
 * <p><b>F-9 — {@code ctorSingletonListBareParam}</b> (ConstructionHandler): the
 * singletonList ctor-value hoist elides for a bare-identifier value — golden guards the
 * param directly; zero goldens carry the rebind form, corpus-greped
 * (Create_AlphaTerminationWorkflowStepFromBetaAndGamma).
 *
 * <p>Whole-file byte comparisons run through the REAL D11 generation paths and revert
 * RED without the facets. The negative witnesses are load-bearing per the
 * witness-uniqueness law (OCCURRENCE counts, never line counts): every token below was
 * occurrence-counted in its PRE gen (f-probe-358post; counts noted per witness) and 0
 * in its golden — the flips REMOVE them.
 */
class DistinctOnlyElementRewrapComposeTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    /** The 20 F-1/F-2/F-3/F-4/F-5 drr POJO Rule flip carriers. */
    private static final String[] DRR_POJO_RULES = {
            "drr/regulation/asic/rewrite/margin/reports/ExecutionAgentOfCounterparty1Rule.java",
            "drr/regulation/asic/rewrite/valuation/reports/ExecutionAgentOfTheCounterparty1Rule.java",
            "drr/regulation/asic/rewrite/valuation/reports/ExecutionAgentOfTheCounterparty2DTCCRule.java",
            "drr/regulation/cftc/rewrite/valuation/reports/DTCC_TradeParty1ExecutionAgentIDRule.java",
            "drr/regulation/cftc/rewrite/valuation/reports/DTCC_TradeParty2ExecutionAgentIDRule.java",
            "drr/regulation/common/dtcc/reports/DTCC_TradeParty1ExecutionAgentIDRule.java",
            "drr/regulation/common/trade/party/reports/ExecutionAgentCounterparty2Rule.java",
            "drr/regulation/csa/rewrite/valuation/reports/DTCC_TradeParty1ExecutionAgentIDRule.java",
            "drr/regulation/csa/rewrite/valuation/reports/DTCC_TradeParty2ExecutionAgentIDRule.java",
            "drr/regulation/fca/ukemir/refit/margin/reports/ExecutionAgentOfCounterparty1Rule.java",
            "drr/regulation/fca/ukemir/refit/margin/reports/ExecutionAgentOfCounterparty2Rule.java",
            "drr/regulation/mas/rewrite/valuation/reports/ExecutionAgentOfCounterparty2DTCCRule.java",
            "drr/regulation/common/trade/party/reports/CountryOfCounterparty2Rule.java",
            "drr/standards/iosco/cde/version1/link/reports/PriorUTIRule.java",
            "drr/standards/iosco/cde/version1/datetime/reports/EffectiveDateRule.java",
            "drr/standards/iosco/cde/version1/basket/reports/IdentifierOfBasketConstituentsRule.java",
            "drr/standards/iosco/cde/version1/quantity/reports/NotionalAmountScheduleLeg1Rule.java",
            "drr/standards/iosco/cde/version1/quantity/reports/NotionalAmountScheduleLeg2Rule.java",
            "drr/standards/iosco/cde/version3/quantity/reports/NotionalAmountScheduleLeg1Rule.java",
            "drr/standards/iosco/cde/version3/quantity/reports/NotionalAmountScheduleLeg2Rule.java",
    };

    /** The 4 F-7/F-8/F-9 drr FUNCTION flip carriers. */
    private static final String[] DRR_FUNCTIONS = {
            "drr/standards/iosco/cde/version1/quantity/functions/NotionalAmountLeg1.java",
            "drr/standards/iosco/cde/version1/quantity/functions/NotionalAmountLeg2.java",
            "drr/standards/iosco/cde/base/price/functions/PriceOfEvent.java",
            "drr/enrichment/common/event/functions/Create_AlphaTerminationWorkflowStepFromBetaAndGamma.java",
    };

    private static Map<String, String> drrFnOutput;
    private static Map<String, String> drrCellOutput;

    static boolean drrCellAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (drrCellAvailable()) {
            var drrCell = new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT);
            drrFnOutput = generateFunctions(drrCell);
            drrCellOutput = generateCell(drrCell);
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
        var errors = funcGen.generateWithErrors(output);
        assertTrue(errors.isEmpty(),
                () -> "Function generation reported " + errors.size() + " error(s): " + errors);
        return output;
    }

    /** The REAL D11 full-cell path (the POJO/Rule kinds ride RuleGenerator/ReportGenerator). */
    private static Map<String, String> generateCell(D11CorpusRegressionTest.CellSpec cell)
            throws IOException {
        var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(),
                D11CorpusRegressionTest.emissionFilter(cell),
                D11CorpusRegressionTest.readDoNotPrune(cell));
        var typeUtil = new JavaTypeUtil();
        var typeTranslator = new JavaTypeTranslator(typeUtil);
        var enumGen = new EnumGenerator(gm);
        var pojoGen = new ModelObjectGenerator(gm, typeTranslator, typeUtil);
        var choiceGen = new ChoiceObjectGenerator(gm, typeTranslator, typeUtil, pojoGen);
        var funcGen = new FunctionGenerator(gm, typeTranslator, typeUtil);
        var ruleGen = new RuleGenerator(gm, typeTranslator, funcGen);
        var reportGen = new ReportGenerator(gm, typeTranslator, funcGen);
        Map<String, String> output = new LinkedHashMap<>();
        for (RModel model : corpus.workspace().files()) {
            if (gm.shouldGenerate(model)) {
                String version = gm.version(model);
                assertNoGenerationErrors(enumGen.generateClasses(model, version, output));
                assertNoGenerationErrors(pojoGen.generateClasses(model, version, output));
                assertNoGenerationErrors(choiceGen.generateClasses(model, version, output));
                assertNoGenerationErrors(ruleGen.generateClasses(model, version, output));
                assertNoGenerationErrors(reportGen.generateClasses(model, version, output));
            }
        }
        return output;
    }

    // ---------------------------- F-1..F-5 drr POJO Rule byte locks (20)

    @Test
    @EnabledIf("drrCellAvailable")
    void f1toF5DrrPojoRules_byteMatchGolden() throws IOException {
        for (String path : DRR_POJO_RULES) {
            assertCellByteMatchesGolden(path);
        }
    }

    // ---------------------------- F-7/F-8/F-9 drr FUNCTION byte locks (4)

    @Test
    @EnabledIf("drrCellAvailable")
    void f7toF9DrrFunctions_byteMatchGolden() throws IOException {
        for (String path : DRR_FUNCTIONS) {
            assertDrrFnByteMatchesGolden(path);
        }
    }

    // ------------------------------------------------------- negative witnesses

    /**
     * The F-1 negative witness (asic-margin ExecutionAgentOfCounterparty1): the bare
     * collapse decl value {@code  = distinct(thenArg0).get();} counted EXACTLY 1
     * occurrence in the PRE gen and 0 in the golden — the re-wrap renders
     * {@code = MapperS.of(distinct(thenArg0).get());}.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void executionAgent_bareDistinctCollapseGone() {
        assertEquals(0, count(cell(DRR_POJO_RULES[0]), " = distinct(thenArg0).get();"),
                "The bare distinct-collapse decl value is gone (count 0 in golden)");
    }

    /**
     * The F-2 negative witness (CountryOfCounterparty2): the deref-less checkedMap
     * lambda {@code .mapSingleToItem(item -> item.checkedMap(} counted EXACTLY 1
     * occurrence in the PRE gen and 0 in the golden — the C5 step derefs the
     * FieldWithMetaString before the to-enum.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void countryOfCounterparty2_derefLessCheckedMapGone() {
        assertEquals(0, count(cell(DRR_POJO_RULES[12]),
                        ".mapSingleToItem(item -> item.checkedMap("),
                "The deref-less checkedMap lambda is gone (count 0 in golden)");
    }

    /**
     * The F-3 negative witness (PriorUTIRule iosco): the value-typed distinct decl
     * {@code final MapperC<String> thenArg8 = distinct(thenArg7);} counted EXACTLY 1
     * occurrence in the PRE gen and 0 in the golden — distinct() preserves the
     * receiver's {@code FieldWithMetaString} element.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void priorUti_valueTypedDistinctDeclGone() {
        assertEquals(0, count(cell(DRR_POJO_RULES[13]),
                        "final MapperC<String> thenArg8 = distinct(thenArg7);"),
                "The value-typed distinct decl is gone (count 0 in golden)");
    }

    /**
     * The F-4 negative witness (EffectiveDateRule iosco): the getter-form record nav
     * {@code .map("getDate", dateTime -> dateTime.getDate())} counted EXACTLY 1
     * occurrence in the PRE gen and 0 in the golden — the record nav renders
     * {@code .<Date>map("Date", dt -> Date.of(dt.toLocalDate()))}.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void effectiveDate_getterFormRecordNavGone() {
        assertEquals(0, count(cell(DRR_POJO_RULES[14]),
                        ".map(\"getDate\", dateTime -> dateTime.getDate())"),
                "The getter-form record nav is gone (count 0 in golden)");
    }

    /**
     * The F-5 negative witness (IdentifierOfBasketConstituents iosco): the bare
     * uninvoked symbol {@code MapperS.of(GetBasketConstituentsProductIdentifier)}
     * counted EXACTLY 1 occurrence in the PRE gen and 0 in the golden — the terminal
     * meta-leaf admission invokes {@code MapperC.<ProductIdentifier>of(
     * getBasketConstituentsProductIdentifier.evaluate(item.get()))}.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void basketConstituents_bareSymbolGone() {
        assertEquals(0, count(cell(DRR_POJO_RULES[15]),
                        "MapperS.of(GetBasketConstituentsProductIdentifier)"),
                "The bare uninvoked fn symbol is gone (count 0 in golden)");
    }

    /**
     * The F-5 negative witness (NotionalAmountScheduleLeg1 v1): the bare uninvoked
     * symbol {@code MapperS.of(GetLeg1ResolvablePriceQuantity)} counted EXACTLY 1
     * occurrence in the PRE gen and 0 in the golden.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void notionalSchedule_bareSymbolGone() {
        assertEquals(0, count(cell(DRR_POJO_RULES[16]),
                        "MapperS.of(GetLeg1ResolvablePriceQuantity)"),
                "The bare uninvoked fn symbol is gone (count 0 in golden)");
    }

    /**
     * The F-7 negative witness (NotionalAmountLeg1): the runtime terminal
     * {@code .then(item -> formatToNonNegativeShortFraction5DecimalNumber.evaluate(item.get()))}
     * counted EXACTLY 1 occurrence in the PRE gen and 0 in the golden — the admitted
     * chain hoists thenArg0..2 and inlines the wrapped terminal.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void notionalAmountLeg1_runtimeThenTerminalGone() {
        assertEquals(0, count(drrFn(DRR_FUNCTIONS[0]),
                        ".then(item -> formatToNonNegativeShortFraction5DecimalNumber.evaluate(item.get()))"),
                "The runtime .then( terminal is gone (count 0 in golden)");
    }

    /**
     * The F-9 negative witness (Create_AlphaTerminationWorkflowStep): the rebind local
     * {@code final TradeIdentifier tradeIdentifier = alphaUTI;} counted EXACTLY 1
     * occurrence in the PRE gen and 0 in the golden — golden guards the param directly.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void alphaTerminationWorkflowStep_rebindLocalGone() {
        assertEquals(0, count(drrFn(DRR_FUNCTIONS[3]),
                        "final TradeIdentifier tradeIdentifier = alphaUTI;"),
                "The bare-identifier rebind local is gone (count 0 in golden)");
    }

    // ---------------------------------------------------------------- helpers

    private static int count(String haystack, String needle) {
        int n = 0;
        for (int i = haystack.indexOf(needle); i >= 0; i = haystack.indexOf(needle, i + 1)) {
            n++;
        }
        return n;
    }

    private static String drrFn(String path) {
        assertNotNull(drrFnOutput, "function generation did not run — corpus unavailable?");
        String g = drrFnOutput.get(path);
        assertNotNull(g, "Function not generated: " + path);
        return g;
    }

    private static String cell(String path) {
        assertNotNull(drrCellOutput, "cell generation did not run — corpus unavailable?");
        String g = drrCellOutput.get(path);
        assertNotNull(g, "Class not generated: " + path);
        return g;
    }

    private static void assertDrrFnByteMatchesGolden(String path) throws IOException {
        assertBytes(path, drrFn(path), DRR_GOLDEN_DIR);
    }

    private static void assertCellByteMatchesGolden(String path) throws IOException {
        assertBytes(path, cell(path), DRR_GOLDEN_DIR);
    }

    private static void assertBytes(String path, String gen, Path goldenDir) throws IOException {
        Path goldenPath = goldenDir.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath).replace("\r", "");
        assertEquals(golden, gen.replace("\r", ""), "byte mismatch vs golden: " + path);
    }
}
