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
 * PR #357 — the INPUT-form then-hoist + the returnIte MapperC empty + the cond-list
 * chain/literal/Mapper-slot arms: 14 byte flips (6 drr POJO Rules + 5 drr FUNCTION +
 * 3 cdm FUNCTION) + 20 TOWARD movers / 0 new / 0 AWAY (regscan357).
 *
 * <p><b>F-1 — {@code inputFormThenHoist}</b> (CollectionHandler + ReferenceHandler),
 * THREE pieces: (1) {@code thenChainHasUnhandledControlFlow} admits a
 * {@code then extract <ladder>} CONSUMER whose inline-body ROOT is a control-flow-free
 * conditional LADDER (the #341/#352 LOCKSTEP predicate — the cdm cells verified
 * unmoved); (2) {@code ReferenceHandler.handle(RImplicitVariable)} renders the
 * {@code MapperS.of(input)} INPUT-form base for a synthetic elided receiver whose
 * nearest enclosing lambda is a NAMED top-level extract over the bare rule input,
 * scoped INSIDE the active deep-then restructure window (the #356 chain-top flag —
 * outside the window every unconverted runtime {@code .then(} chain keeps the
 * outer-lambda-param re-root byte-frozen); (3) {@code isCleanLadderContext}'s RULE-path
 * bound-then arm RETURNS {@code extractCount <= 1} at the thenArg-BOUND seat (the #354
 * registered-seat law — the deep-seat hoist validated everything above the chain).
 * Carriers: the hkma {@code Counterparty2IdentifierFormatRule} margin/trade/valuation
 * trio (mutually shape-identical; one input-DTO lexeme differs) + the hkma trade
 * {@code Broker}/{@code CentralCounterparty}/{@code ClearingMember}IdentifierFormat
 * rules (same shape + the {@code import cdm.base.staticdata.party.Party;} induced by
 * the typed {@code final MapperS<Party> thenArg} decl).
 *
 * <p><b>F-3 — {@code returnIteMapperCEmpty} + {@code addAliasCallArm}</b>
 * (FunctionExpressionRenderer): the alias return ladder's elseless eligibility + its
 * implicit-else render widen to {@code typedEmptyAnyMapperOrNull} (golden
 * {@code return MapperC.<Date>ofNull();} — MapCommodityOptionToStrikePriceDatedValues);
 * an ALIAS-call arm joins the whole-output ADD distribution's mapper-chain class
 * (golden {@code dateList.addAll(newList(startDate, endDate, businessCenters)
 * .getMulti());} — GenerateDateList cdm5+cdm6).
 *
 * <p><b>F-2a/a2/b — the cond-list arm forms</b> (ControlFlowHandler): a MAPPER-CHAIN
 * arm at the CondListCoerce list seat assigns {@code <chain>.getMulti()} DIRECTLY (no
 * value local, no null-guard — Create_SubmissionHarmonizedRepeatableData_Part45's five
 * arms); an INT-LITERAL arm at a BigDecimal elem singletonLists
 * {@code BigDecimal.valueOf(0)} direct (Create_SubmissionHarmonizedRepeatableData
 * cftc/csa); a LIST-LITERAL element conditional hoists the {@code final MapperS<X>}
 * slot with {@code MapperS.of} arms + the trailing {@code MapperS.<X>ofNull()} else,
 * consumed verbatim by {@code MapperC.<X>of(…)} (Create_CounterpartySpecificData/_2).
 *
 * <p>Whole-file byte comparisons run through the REAL D11 generation paths and revert
 * RED without the facets. The negative witnesses are load-bearing per the
 * witness-uniqueness law (OCCURRENCE counts, never line counts): every token below was
 * occurrence-counted in its PRE gen (f-probe-356post; counts noted per witness) and 0
 * in its golden — the flips REMOVE them.
 */
class InputFormThenHoistComposeTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM5_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-5.38.0");
    private static final Path CDM5_GOLDEN_DIR =
            CDM5_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    /** The 6 F-1 drr POJO Rule flip carriers (the INPUT-form then-hoist proper). */
    private static final String[] IDENTIFIER_FORMAT_RULES = {
            "drr/regulation/hkma/rewrite/margin/reports/Counterparty2IdentifierFormatRule.java",
            "drr/regulation/hkma/rewrite/trade/reports/Counterparty2IdentifierFormatRule.java",
            "drr/regulation/hkma/rewrite/valuation/reports/Counterparty2IdentifierFormatRule.java",
            "drr/regulation/hkma/rewrite/trade/reports/BrokerIdentifierFormatRule.java",
            "drr/regulation/hkma/rewrite/trade/reports/CentralCounterpartyIdentifierFormatRule.java",
            "drr/regulation/hkma/rewrite/trade/reports/ClearingMemberIdentifierFormatRule.java",
    };

    /** The 5 F-2 drr FUNCTION flip carriers (the cond-list arm forms). */
    private static final String PART45 = "drr/projection/dtcc/rds/harmonized/cftc/rewrite/"
            + "trade/functions/Create_SubmissionHarmonizedRepeatableData_Part45.java";
    private static final String REPEATABLE_CFTC = "drr/projection/dtcc/rds/harmonized/cftc/"
            + "rewrite/trade/functions/Create_SubmissionHarmonizedRepeatableData.java";
    private static final String REPEATABLE_CSA = "drr/projection/dtcc/rds/harmonized/csa/"
            + "rewrite/trade/functions/Create_SubmissionHarmonizedRepeatableData.java";
    private static final String COUNTERPARTY_SPECIFIC = "drr/projection/iso20022/fca/ukemir/"
            + "refit/margin/functions/Create_CounterpartySpecificData.java";
    private static final String COUNTERPARTY_SPECIFIC_2 = "drr/projection/iso20022/fca/ukemir/"
            + "refit/margin/functions/Create_CounterpartySpecificData_2.java";

    /** The 3 F-3 cdm FUNCTION flip carriers. */
    private static final String GENERATE_DATE_LIST =
            "cdm/base/datetime/functions/GenerateDateList.java";
    private static final String MAP_COMMODITY_OPTION = "cdm/ingest/fpml/confirmation/product/"
            + "commodityoption/functions/MapCommodityOptionToStrikePriceDatedValues.java";

    private static Map<String, String> drrFnOutput;
    private static Map<String, String> drrCellOutput;
    private static Map<String, String> cdm5FnOutput;
    private static Map<String, String> cdm6FnOutput;

    static boolean drrCellAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR);
    }

    static boolean cdmCellsAvailable() {
        return Files.isDirectory(CDM5_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM5_GOLDEN_DIR)
                && Files.isDirectory(CDM6_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM6_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (drrCellAvailable()) {
            var drrCell = new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT);
            drrFnOutput = generateFunctions(drrCell);
            drrCellOutput = generateCell(drrCell);
        }
        if (cdmCellsAvailable()) {
            cdm5FnOutput = generateFunctions(
                    new D11CorpusRegressionTest.CellSpec("cdm", "5.38.0", CDM5_CELL_ROOT));
            cdm6FnOutput = generateFunctions(
                    new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CDM6_CELL_ROOT));
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

    // ------------------------------------------- F-1 drr POJO Rule byte locks (6)

    @Test
    @EnabledIf("drrCellAvailable")
    void f1IdentifierFormatRules_byteMatchGolden() throws IOException {
        for (String path : IDENTIFIER_FORMAT_RULES) {
            assertCellByteMatchesGolden(path);
        }
    }

    // ------------------------------------------- F-2 drr FUNCTION byte locks (5)

    @Test
    @EnabledIf("drrCellAvailable")
    void f2CondListArmCarriers_byteMatchGolden() throws IOException {
        assertDrrFnByteMatchesGolden(PART45);
        assertDrrFnByteMatchesGolden(REPEATABLE_CFTC);
        assertDrrFnByteMatchesGolden(REPEATABLE_CSA);
        assertDrrFnByteMatchesGolden(COUNTERPARTY_SPECIFIC);
        assertDrrFnByteMatchesGolden(COUNTERPARTY_SPECIFIC_2);
    }

    // ------------------------------------------- F-3 cdm FUNCTION byte locks (3)

    @Test
    @EnabledIf("cdmCellsAvailable")
    void f3ReturnIteAddArmCarriers_byteMatchGolden() throws IOException {
        assertBytes(GENERATE_DATE_LIST, cdmFn(cdm5FnOutput, GENERATE_DATE_LIST),
                CDM5_GOLDEN_DIR);
        assertBytes(GENERATE_DATE_LIST, cdmFn(cdm6FnOutput, GENERATE_DATE_LIST),
                CDM6_GOLDEN_DIR);
        assertBytes(MAP_COMMODITY_OPTION, cdmFn(cdm6FnOutput, MAP_COMMODITY_OPTION),
                CDM6_GOLDEN_DIR);
    }

    // ------------------------------------------------------- negative witnesses

    /**
     * The F-1 negative witness (hkma margin C2IF): the runtime {@code .then(item -> item}
     * chain counted EXACTLY 1 occurrence in the PRE gen and 0 in the golden — the hoist
     * replaces it with the {@code final MapperS<ReferenceWithMetaParty> thenArg =
     * MapperS.of(input)…} decl + the bound {@code thenArg.mapSingleToItem(item -> { if …
     * return …; })} consumer ladder.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void c2ifMargin_runtimeThenGone() {
        assertEquals(0, count(cell(IDENTIFIER_FORMAT_RULES[0]), ".then(item -> item"),
                "The runtime .then( chain is gone (count 0 in golden)");
    }

    /**
     * The F-1 negative witness (hkma trade Broker): the same runtime
     * {@code .then(item -> item} token counted EXACTLY 1 occurrence in the PRE gen and 0
     * in the golden — the bare-invokable base variant ({@code extract Extract_BrokerId})
     * hoists identically, inducing the {@code Party} import via the typed decl.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void brokerIdentifierFormat_runtimeThenGone() {
        assertEquals(0, count(cell(IDENTIFIER_FORMAT_RULES[3]), ".then(item -> item"),
                "The runtime .then( chain is gone (count 0 in golden)");
    }

    /**
     * The F-3a negative witness (cdm6 MapCommodityOptionToStrikePriceDatedValues): the
     * untyped empty terminal {@code MapperC.of()} counted EXACTLY 1 occurrence in the
     * PRE gen (the alias ternary's terminal) and 0 in the golden — the widened ladder
     * renders {@code return MapperC.<Date>ofNull();}.
     */
    @Test
    @EnabledIf("cdmCellsAvailable")
    void mapCommodityOption_untypedEmptyTerminalGone() {
        assertEquals(0, count(cdmFn(cdm6FnOutput, MAP_COMMODITY_OPTION), "MapperC.of()"),
                "The untyped MapperC.of() terminal is gone (count 0 in golden)");
    }

    /**
     * The F-3b negative witness (cdm6 GenerateDateList): the inline ADD ternary's empty
     * arm {@code MapperC.of().getMulti()} counted EXACTLY 1 occurrence in the PRE gen
     * and 0 in the golden — the alias-call arm distribution renders the if/else with
     * {@code Collections.<Date>emptyList()}.
     */
    @Test
    @EnabledIf("cdmCellsAvailable")
    void generateDateList_inlineAddTernaryGone() {
        assertEquals(0, count(cdmFn(cdm6FnOutput, GENERATE_DATE_LIST), "MapperC.of().getMulti()"),
                "The inline ADD-ternary empty arm is gone (count 0 in golden)");
    }

    /**
     * The F-2a negative witness (cftc Part45): the scalar-collapse local
     * {@code final String string0 = } counted EXACTLY 1 occurrence in the PRE gen and 0
     * in the golden — the Mapper-chain arm assigns {@code <chain>.getMulti()} directly
     * (no value local, no null-guard).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void part45_scalarCollapseLocalGone() {
        assertEquals(0, count(drrFn(PART45), "final String string0 = "),
                "The scalar-collapse value local is gone (count 0 in golden)");
    }

    /**
     * The F-2a2 negative witness (cftc RepeatableData): the non-compiling int-to-BigDecimal
     * local {@code final BigDecimal bigDecimal = 0;} counted EXACTLY 1 occurrence in the
     * PRE gen and 0 in the golden — the literal arm renders
     * {@code Collections.singletonList(BigDecimal.valueOf(0))} direct.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void repeatableDataCftc_intLiteralLocalGone() {
        assertEquals(0, count(drrFn(REPEATABLE_CFTC), "final BigDecimal bigDecimal = 0;"),
                "The int-to-BigDecimal local is gone (count 0 in golden)");
    }

    /**
     * The F-2b negative witness (fca Create_CounterpartySpecificData): the bare-typed
     * null-init slot {@code OrganisationIdentification15Choice__1 ifThenElseResult2 =
     * null;} counted EXACTLY 1 occurrence in the PRE gen and 0 in the golden — the
     * list-literal element slot declares {@code final MapperS<OrganisationIdentification15
     * Choice__1> ifThenElseResult2;} with the {@code MapperS.<…>ofNull()} trailing else.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void counterpartySpecificData_bareTypedSlotGone() {
        assertEquals(0, count(drrFn(COUNTERPARTY_SPECIFIC),
                        "OrganisationIdentification15Choice__1 ifThenElseResult2 = null;"),
                "The bare-typed null-init slot is gone (count 0 in golden)");
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

    private static String cdmFn(Map<String, String> output, String path) {
        assertNotNull(output, "cdm function generation did not run — corpus unavailable?");
        String g = output.get(path);
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
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath).replace("\r", "");
        assertEquals(golden, gen.replace("\r", ""),
                "Generated bytes must match the golden for " + path);
    }
}
