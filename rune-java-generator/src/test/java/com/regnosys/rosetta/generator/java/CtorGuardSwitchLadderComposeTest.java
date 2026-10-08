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
 * PR #367 — the getOrDefault/ctor-guard family + the switch instanceof ladder + the
 * nested-else restructure: 6 byte flips (3 cdm6 FUNCTION + 2 drr FUNCTION + 1 drr POJO
 * Rule) + 0 new / 0 away (regscan367: 6/0/0; the three F6 riders line-ratio TOWARD —
 * IsActionTypeMODI dl 30→6, IsCommodityBullion/Metal dl 40→36 with witness counts
 * "} else {" 1→2 toward golden 3 and "} else if (" 2→1 toward golden 0).
 *
 * <p><b>F1 setMultiOutputCtorGuard + multiDefaultTernaryOperands + defaultAliasArgDeref</b>
 * (FunctionExpressionRenderer + NavigationHandler + SetOperationHandler +
 * ConstructionHandler): a WHOLE-OUTPUT SET on a MULTI output whose value is a single
 * model ctor hoists {@code final TradeIdentifier tradeIdentifier = …;} and distributes
 * the null-check if/else over the {@code toBuilder(Collections.<T>emptyList())} /
 * {@code toBuilder(Collections.singletonList(x))} assignment (the #328 meta-output arm's
 * MULTI sibling); the ctor-FIELD MULTI-default ternary's ONE shared predicate selects on
 * LEFT-multi + a proven-Mapper RIGHT (a resolved single alias proves the Mapper —
 * MapperS.getMulti() is the 0/1-element list); a DIRECT fn-call alias body with a single
 * non-meta output proves the getOrDefault-arg {@code .get()} reduction the type walk
 * cannot (enum items): MapPartyTradeIdentifierToTradeIdentifierList cdm6.
 *
 * <p><b>F2 mapItemParamBareOperand + thenBodyDefaultCollapseFold</b> (ComparisonHandler +
 * SetOperationHandler): an explicit extract/mapItem lambda param is ALREADY the MapperS
 * item, so an equality operand that is the bare param re-wrapped {@code MapperS.of(src)}
 * unwraps to the bare name (render-equals guarded; then-owners decline — the #175/#180
 * closure-param law); a collapsing list-op RIGHT (first/last/only-element over a
 * non-meta attribute) reduces via {@code .get()} and the standard wrappedInMapperSOf
 * contract serves both seats (the #267 then-output arm keeps the wrap + {@code .get()}):
 * ExtractProductIdentifierBySource drr.
 *
 * <p><b>F3 fnCallMetaSourceHoist</b> (ConversionHandler): a to-enum whose source is a
 * DIRECT function call returning a single FIELD_WITH_META string hoists {@code final
 * FieldWithMetaString fieldWithMetaString = mapStringWithScheme.evaluate(…);} (the #331
 * string-registered sink hoist, the #314 lambda arm's statement-sink sibling) and
 * null-guard-reconstructs the MapperS&lt;String&gt; before the checkedMap (the #317
 * guarded rewrap); the SettlementRateOption import-collision winner swapped FREE — the
 * hoist relocates the fpml witnesses textually first and the claims table is text-order
 * first-claim: MapSettlementProvisionToSettlementTerms cdm6.
 *
 * <p><b>F4 namedExtractPipedRebind + toEnumBoundItemDeref</b> (ReferenceHandler +
 * ConversionHandler): {@code then extract <name> [ … ]} parses as an implicit WRAPPER fn
 * holding the RExtractExpr whose own body is the EXPLICIT-param fn — one fn deeper than
 * the k-loop's thenArg binding; inside an ACTIVE #356 restructure window an implicit
 * that IS the flagged chain's level-0 receiver and whose nearest boundary is the unbound
 * NON-implicit named fn walks out one fn to the wrapper's binding (the #364 non-rebind
 * law at the piped seat; triple-fenced from the cp4/cp4b/cp4d caught over-fires — the
 * isImplicit gate is load-bearing, P365A materializes {@code item} into paramNames);
 * enumArgMetaUnwrapStep gains the IMPLICIT-item arm (the meta-string proof rides the
 * first BOUND enclosing thenArg ref — render truth): CountryOfCounterparty2Rule asic drr.
 *
 * <p><b>F5 ctorSwitchInstanceofLadder</b> (ControlFlowHandler): a TYPE-keyed switch at a
 * ctor-FIELD seat over an extends-based model subject hoists the upstream instanceof
 * ASSIGNMENT ladder ({@code final FloatingRateIndex ifThenElseResult0;} null-guard head +
 * per-case instanceof + cast local + {@code evaluate(castLocal)} via the #221
 * bindSwitchSubject re-root + else null) and splices the bare sentinel — the #181
 * family's SWITCH sibling; the block drains into the enclosing conditional arm via the
 * #327 mark/drain: MapRateOptionWithLocation cdm6.
 *
 * <p><b>F6 nestedElseCondHoistRestructure</b> (FunctionExpressionRenderer): a rung
 * condition registering ANY statement-channel hoist (the EvalArgMetaDerefHoist class)
 * forces the nested-else restructure — predicted by a SPECULATIVE probe-compile on a
 * throwaway sink scope (the #351-i3 isolation; deferred scopes truncate per the #365-R1
 * law), with the hoist drained at the else-block top by the restructured recursion's
 * condition channel: GetInstrumentType drr.
 *
 * <p>Whole-file byte comparisons run through the REAL D11 generation paths and revert
 * RED without the facets. The negative witnesses are load-bearing per the
 * witness-uniqueness law (OCCURRENCE counts, never line counts): every token below was
 * occurrence-counted EXACTLY 1 in its PRE gen (f-probe-366post) and 0 in its golden —
 * the flips REMOVE them.
 */
class CtorGuardSwitchLadderComposeTest {

    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    /** The 3 cdm6 FUNCTION flip carriers (F1 / F3 / F5). */
    private static final String[] CDM6_FUNCTIONS = {
            "cdm/ingest/fpml/confirmation/header/functions/MapPartyTradeIdentifierToTradeIdentifierList.java",
            "cdm/ingest/fpml/confirmation/settlement/functions/MapSettlementProvisionToSettlementTerms.java",
            "cdm/ingest/fpml/confirmation/pricequantity/functions/MapRateOptionWithLocation.java",
    };

    /** The 2 drr FUNCTION flip carriers (F2 / F6). */
    private static final String[] DRR_FUNCTIONS = {
            "drr/regulation/common/functions/ExtractProductIdentifierBySource.java",
            "drr/regulation/common/functions/GetInstrumentType.java",
    };

    /** The 1 drr POJO Rule flip carrier (F4). */
    private static final String[] DRR_POJO_RULES = {
            "drr/regulation/asic/rewrite/trade/reports/CountryOfCounterparty2Rule.java",
    };

    private static Map<String, String> cdm6FnOutput;
    private static Map<String, String> drrFnOutput;
    private static Map<String, String> drrCellOutput;

    static boolean cdm6CellAvailable() {
        return Files.isDirectory(CDM6_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM6_GOLDEN_DIR);
    }

    static boolean drrCellAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (cdm6CellAvailable()) {
            cdm6FnOutput = generateFunctions(
                    new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CDM6_CELL_ROOT));
        }
        if (drrCellAvailable()) {
            drrFnOutput = generateFunctions(
                    new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT));
            drrCellOutput = generateCell(
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

    // -------------------------------------------- cdm6 FUNCTION byte locks (3)

    @Test
    @EnabledIf("cdm6CellAvailable")
    void cdm6Functions_byteMatchGolden() throws IOException {
        for (String path : CDM6_FUNCTIONS) {
            assertBytes(path, fn(cdm6FnOutput, path), CDM6_GOLDEN_DIR);
        }
    }

    // -------------------------------------------- drr FUNCTION byte locks (2)

    @Test
    @EnabledIf("drrCellAvailable")
    void drrFunctions_byteMatchGolden() throws IOException {
        for (String path : DRR_FUNCTIONS) {
            assertBytes(path, fn(drrFnOutput, path), DRR_GOLDEN_DIR);
        }
    }

    // -------------------------------------------- drr POJO Rule byte lock (1)

    @Test
    @EnabledIf("drrCellAvailable")
    void drrPojoRule_byteMatchGolden() throws IOException {
        for (String path : DRR_POJO_RULES) {
            assertBytes(path, cell(path), DRR_GOLDEN_DIR);
        }
    }

    // ------------------------------------------------------- negative witnesses

    /**
     * The F1 witnesses (MapPartyTradeIdentifier cdm6): the un-guarded whole-output
     * assign {@code cdmTradeIdentifier = toBuilder(TradeIdentifier.builder()}, the
     * getOrDefault MULTI chain {@code .getOrDefault(identifierForIssuer(
     * fpmlPartyTradeIdentifier)).getMulti()}, and the un-dereffed alias default arg
     * {@code .getOrDefault(identifierTypeForIssuer(fpmlPartyTradeIdentifier))} each
     * counted EXACTLY 1 occurrence in the PRE gen (f-probe-366post) and 0 in the
     * golden — the guard block, the isEmpty ternary, and the {@code .get()} reduction
     * replace them.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapPartyTradeIdentifier_guardFoldDerefWitnesses() {
        String gen = fn(cdm6FnOutput, CDM6_FUNCTIONS[0]);
        assertEquals(0, count(gen, "cdmTradeIdentifier = toBuilder(TradeIdentifier.builder()"),
                "The un-guarded whole-output ctor assign is gone (count 0 in golden)");
        assertEquals(0, count(gen,
                        ".getOrDefault(identifierForIssuer(fpmlPartyTradeIdentifier)).getMulti()"),
                "The getOrDefault MULTI chain is gone (count 0 in golden)");
        assertEquals(0, count(gen,
                        ".getOrDefault(identifierTypeForIssuer(fpmlPartyTradeIdentifier))"),
                "The un-dereffed alias default arg is gone (count 0 in golden)");
    }

    /**
     * The F2 witness (ExtractProductIdentifierBySource drr): the double-wrapped mapItem
     * param operand {@code MapperS.of(src)} counted EXACTLY 1 occurrence in the PRE gen
     * and 0 in the golden — the param is already the MapperS item and splices bare.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void extractProductIdentifier_doubleWrappedParamGone() {
        assertEquals(0, count(fn(drrFnOutput, DRR_FUNCTIONS[0]), "MapperS.of(src)"),
                "The double-wrapped mapItem param operand is gone (count 0 in golden)");
    }

    /**
     * The F3 witnesses (MapSettlementProvision cdm6): the un-hoisted wrapped evaluate
     * {@code MapperS.of(mapStringWithScheme.evaluate(} and the collision-loser import
     * {@code import cdm.observable.asset.SettlementRateOption;} each counted EXACTLY 1
     * occurrence in the PRE gen and 0 in the golden — the statement hoist + guarded
     * rewrap replace the wrap, and the fpml witness claims the simple name first
     * (text-order first-claim), so the cdm builder renders FQN with the fpml import
     * kept instead.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapSettlementProvision_hoistAndCollisionWitnesses() {
        String gen = fn(cdm6FnOutput, CDM6_FUNCTIONS[1]);
        assertEquals(0, count(gen, "MapperS.of(mapStringWithScheme.evaluate("),
                "The un-hoisted wrapped evaluate is gone (count 0 in golden)");
        assertEquals(0, count(gen, "import cdm.observable.asset.SettlementRateOption;"),
                "The collision-loser cdm import is gone (count 0 in golden)");
    }

    /**
     * The F4 witnesses (CountryOfCounterparty2Rule asic drr): the param-rooted inner
     * decl {@code thenArg0 = reportInstruction} and the deref-less to-enum lambda
     * {@code .mapSingleToItem(item -> item.checkedMap(} each counted EXACTLY 1
     * occurrence in the PRE gen and 0 in the golden — the piped rebind roots the decl
     * on the outer {@code thenArg} and the bound-item C5 deref precedes the checkedMap.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void countryOfCounterparty2_rebindAndDerefWitnesses() {
        String gen = cell(DRR_POJO_RULES[0]);
        assertEquals(0, count(gen, "thenArg0 = reportInstruction"),
                "The param-rooted inner decl is gone (count 0 in golden)");
        assertEquals(0, count(gen, ".mapSingleToItem(item -> item.checkedMap("),
                "The deref-less to-enum lambda is gone (count 0 in golden)");
    }

    /**
     * The F5 witness (MapRateOptionWithLocation cdm6): the broken type-literal ternary
     * {@code Objects.equals(fpml.FloatingRateCalculation, MapperS.of(fpmlRate))} counted
     * EXACTLY 1 occurrence in the PRE gen and 0 in the golden — the instanceof
     * ASSIGNMENT ladder + the bare ifThenElseResult splice replace it.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapRateOptionWithLocation_typeLiteralTernaryGone() {
        assertEquals(0, count(fn(cdm6FnOutput, CDM6_FUNCTIONS[2]),
                        "Objects.equals(fpml.FloatingRateCalculation, MapperS.of(fpmlRate))"),
                "The type-literal switch ternary is gone (count 0 in golden)");
    }

    /**
     * The F6 witness (GetInstrumentType drr): the FLAT SWAP rung
     * "} else if (ComparisonResult.ofNullSafe(MapperS.of(qualify_ForeignExchange_Swap"
     * counted EXACTLY 1 occurrence in the PRE gen and 0 in the golden — the
     * hoist-carrying rung restructures into "} else {" with the
     * {@code final FieldWithMetaString} hoist at the block top and the rung nested
     * one level deeper.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void getInstrumentType_flatSwapRungGone() {
        assertEquals(0, count(fn(drrFnOutput, DRR_FUNCTIONS[1]),
                        "} else if (ComparisonResult.ofNullSafe(MapperS.of(qualify_ForeignExchange_Swap"),
                "The flat hoist-carrying SWAP rung is gone (count 0 in golden)");
    }

    // ------------------------------------------------------------------ helpers

    private static String fn(Map<String, String> output, String path) {
        String gen = output.get(path);
        assertNotNull(gen, () -> "Function output missing for " + path);
        return gen;
    }

    private static String cell(String path) {
        String gen = drrCellOutput.get(path);
        assertNotNull(gen, () -> "Cell output missing for " + path);
        return gen;
    }

    private static void assertBytes(String path, String gen, Path goldenDir) throws IOException {
        Path goldenPath = goldenDir.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), () -> "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath).replace("\r\n", "\n");
        assertEquals(golden, gen.replace("\r\n", "\n"),
                () -> "Generated bytes must match golden for " + path);
    }

    /** OCCURRENCE count (the witness-uniqueness law — never line counts). */
    private static int count(String text, String token) {
        int n = 0;
        int i = text.indexOf(token);
        while (i >= 0) {
            n++;
            i = text.indexOf(token, i + 1);
        }
        return n;
    }
}
