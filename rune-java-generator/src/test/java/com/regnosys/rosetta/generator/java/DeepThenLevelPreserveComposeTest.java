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
 * PR #362 — the nested-tree mixed meta join + the deep-then level element preserve +
 * the group-P counterparty block relocate + the meta-collapse consumer type stamp +
 * the alias default join + the SortIdentifiers pair: 17 byte flips (14 drr POJO Rules
 * + 2 drr FUNCTION + 1 cdm6 FUNCTION) + 0 new / 0 away (regscan362 — 18 TOWARD movers
 * all content-verified at checkpoints).
 *
 * <p><b>nestedTreeMixedMetaJoin</b> (CollectionHandler + ReferenceHandler): the #295
 * mixed bare+meta arm-join law at the NESTED-TREE renderer
 * ({@code compileNestedConditionalBlock}) — {@code nestedTreeArmLeafMetaKind} widens
 * the ladder's evidence set by the date-RECORD leaf arm (the
 * {@code tryRecordFeatureNav} admission reused, same-walk), the scope flag un-declines
 * the #280/#359 conditional-arm meta-leaf gate for baresym META arms, and
 * {@code appendNestedArm} derefs wrapper-typed arms in-arm
 * ({@code coerceNavigationReceiver} — no-op on bare); the typed-empty terminal follows
 * the bare join via the coerced firstArm (EffectiveDateRule common).
 *
 * <p><b>deepThenLevelElementPreserve</b> (CollectionHandler + NavigationHandler +
 * LiteralHandler + ComparisonHandler + HandlerHelper): the bare-item FILTER and
 * IDENTITY-arm-conditional extract LEVELS preserve the receiver element (the
 * flatten/i1b/distinct law extended — render truth via prevRef); the #337 list-literal
 * witness arm gains the #360 (a3) RECOVERY-LOCAL rule-context disguised-leaf fallback
 * ({@code ruleContextDisguisedLeafMeta} — the shared resolver stays function-gated);
 * the in-lambda consumers follow through {@code bareItemThenPipeMetaType} (the scope's
 * {@code thenArgRefFor} compiled-type channel): the bare-item comparison-operand
 * retype at both ComparisonHandler seats (the #326 F4b pattern) and the identity-arm
 * elseless block's ofNull; the #358/#360 self-shadow pre-escape extends to the
 * implicit-receiver from-type naming arm (UPIRule esma/fca + the
 * DTCC_TradeParty1/2ReportingDestination octet ×8).
 *
 * <p><b>groupPCounterpartyBlockRelocate</b> (ReferenceHandler + CollectionHandler):
 * the {@code tryMetaDerefArg} BLOCK-route decl is marker-classed
 * ({@code EvalArgMetaDerefHoist}) and {@code compileEffectiveElseConditionalBlock}'s
 * THEN/ELSE drains admit it — golden relocates an arm-position evaluate-arg hoist
 * INSIDE the owning branch before its consuming return; the #354 nullTypedElseMetaDeref
 * derefs the first-collapse else for free (Counterparty2Rule asic/mas valuation).
 *
 * <p><b>fnAliasListLiteralJoinSanitize + fnBooleanParamCondition</b>
 * (FunctionAliasHelper + FunctionExpressionRenderer): the alias-signature list-literal
 * arm sanitizes the lowercase symbol-echo (the #349 law) via the parser-side element
 * JOIN fallback, BASIC joins only ({@code MapperC<indexIdentifierFiltered>} →
 * {@code MapperC<String>}); a bare single Boolean INPUT-param condition renders
 * upstream's raw null-guard ternary {@code (isMin == null ? false : isMin)} with no
 * hoist and no Mapper wrap — 0 of the 34,686 goldens carry the wrap form
 * (SortIdentifiers).
 *
 * <p><b>metaCollapseConsumerTypeStamp</b> (CollectionHandler +
 * FunctionExpressionRenderer): the render-truth COMPILED-TYPE channel at the
 * whole-output collapse seats (the #361-cp6 REFUTED AST-walk class replaced) — a
 * type-less bare-item single-collapsing terminal whose previous level's decl element
 * is a META WRAPPER stamps MapperS<element> from prevRef at BOTH the deep-then hoist
 * consumer and the SET-path terminal; the parser-synthesized flatten-only-element tail
 * joins the S3/#266 {@code MapperS.of(…).get()} re-wrap branch with the same stamp; a
 * MapperC-stamped RHS at the deref seat re-presents the #308-style rewrap. THREE
 * containment gates, each a caught in-suite D11 regression class (OriginalSwapUTIRule
 * cftc + GetQuantityForConstituent).
 *
 * <p><b>aliasDefaultJoin</b> (NavigationHandler): {@code resolveReceiverDataType}
 * gains the RDefaultExpr arm (ancestorJoin of the operands — upstream binaryExpr
 * {@code default} types at the joined operand); the ONE root restores the whole
 * witness/arity/naming/import cascade
 * (MapEarlyTerminationProvisionToAncillaryParty cdm6).
 *
 * <p>Whole-file byte comparisons run through the REAL D11 generation paths and revert
 * RED without the facets. The negative witnesses are load-bearing per the
 * witness-uniqueness law (OCCURRENCE counts, never line counts): every token below was
 * occurrence-counted in its PRE gen (f-probe-361post; counts noted per witness) and 0
 * in its golden — the flips REMOVE them.
 */
class DeepThenLevelPreserveComposeTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    /** The 14 drr POJO Rule flip carriers. */
    private static final String[] DRR_POJO_RULES = {
            "drr/regulation/asic/rewrite/valuation/reports/Counterparty2Rule.java",
            "drr/regulation/cftc/rewrite/margin/reports/DTCC_TradeParty1ReportingDestinationRule.java",
            "drr/regulation/cftc/rewrite/trade/reports/OriginalSwapUTIRule.java",
            "drr/regulation/cftc/rewrite/valuation/reports/DTCC_TradeParty1ReportingDestinationRule.java",
            "drr/regulation/cftc/rewrite/valuation/reports/DTCC_TradeParty2ReportingDestinationRule.java",
            "drr/regulation/common/dtcc/reports/DTCC_TradeParty1ReportingDestinationRule.java",
            "drr/regulation/common/dtcc/reports/DTCC_TradeParty2ReportingDestinationRule.java",
            "drr/regulation/common/trade/datetime/reports/EffectiveDateRule.java",
            "drr/regulation/csa/rewrite/margin/reports/DTCC_TradeParty1ReportingDestinationRule.java",
            "drr/regulation/csa/rewrite/valuation/reports/DTCC_TradeParty1ReportingDestinationRule.java",
            "drr/regulation/csa/rewrite/valuation/reports/DTCC_TradeParty2ReportingDestinationRule.java",
            "drr/regulation/esma/emir/refit/trade/reports/UPIRule.java",
            "drr/regulation/fca/ukemir/refit/trade/reports/UPIRule.java",
            "drr/regulation/mas/rewrite/valuation/reports/Counterparty2Rule.java",
    };

    /** The 2 drr FUNCTION flip carriers. */
    private static final String[] DRR_FUNCTIONS = {
            "drr/regulation/common/trade/basket/functions/GetQuantityForConstituent.java",
            "drr/regulation/common/trade/link/functions/SortIdentifiers.java",
    };

    /** The 1 cdm6 FUNCTION flip carrier. */
    private static final String[] CDM6_FUNCTIONS = {
            "cdm/ingest/fpml/confirmation/party/functions/MapEarlyTerminationProvisionToAncillaryParty.java",
    };

    private static Map<String, String> drrFnOutput;
    private static Map<String, String> drrCellOutput;
    private static Map<String, String> cdm6FnOutput;

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
            var drrCell = new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT);
            drrFnOutput = generateFunctions(drrCell);
            drrCellOutput = generateCell(drrCell);
        }
        if (cdm6CellAvailable()) {
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

    // -------------------------------------------- drr POJO Rule byte locks (14)

    @Test
    @EnabledIf("drrCellAvailable")
    void drrPojoRules_byteMatchGolden() throws IOException {
        for (String path : DRR_POJO_RULES) {
            assertCellByteMatchesGolden(path);
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

    // -------------------------------------------- cdm6 FUNCTION byte lock (1)

    @Test
    @EnabledIf("cdm6CellAvailable")
    void cdm6Functions_byteMatchGolden() throws IOException {
        for (String path : CDM6_FUNCTIONS) {
            assertBytes(path, fn(cdm6FnOutput, path), CDM6_GOLDEN_DIR);
        }
    }

    // ------------------------------------------------------- negative witnesses

    /**
     * The nestedTreeMixedMetaJoin witness (EffectiveDateRule common): the legacy
     * bare-symbol junk render {@code MapperS.of(TradeForEvent)} counted EXACTLY 1
     * occurrence in the PRE gen (f-probe-361post) and 0 in the golden — the mixed-join
     * flag lets the baresym META arm fire the wrapper nav + the in-arm deref.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void effectiveDate_baresymJunkGone() {
        assertEquals(0, count(cell(DRR_POJO_RULES[7]), "MapperS.of(TradeForEvent)"),
                "The bare-symbol junk render is gone (count 0 in golden)");
    }

    /**
     * The deepThenLevelElementPreserve base-witness (esma UPIRule): the meta-stripped
     * list-literal witness {@code MapperC.<ProductIdentifier>of(} counted EXACTLY 1
     * occurrence in the PRE gen and 0 in the golden — the #360 (a3) recovery-local
     * fallback lifts the wrapper witness.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void upiRule_strippedLiteralWitnessGone() {
        assertEquals(0, count(cell(DRR_POJO_RULES[11]), "MapperC.<ProductIdentifier>of("),
                "The meta-stripped literal witness is gone (count 0 in golden)");
    }

    /**
     * The deepThenLevelElementPreserve filter-level witness (esma UPIRule): the
     * meta-stripped level decl {@code final MapperC<ProductIdentifier> _thenArg1}
     * counted EXACTLY 1 occurrence in the PRE gen and 0 in the golden — the bare-item
     * FILTER level preserves the receiver element.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void upiRule_strippedFilterLevelDeclGone() {
        assertEquals(0, count(cell(DRR_POJO_RULES[11]),
                        "final MapperC<ProductIdentifier> _thenArg1"),
                "The meta-stripped filter-level decl is gone (count 0 in golden)");
    }

    /**
     * The identity-arm ofNull witness (cftc-margin DTCC_TradeParty1ReportingDestination):
     * the bare-element terminal {@code return MapperS.<SupervisoryBodyEnum>ofNull();}
     * counted EXACTLY 1 occurrence in the PRE gen and 0 in the golden — the identity-arm
     * elseless block's ofNull follows the wrapper element through the then-pipe binding.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void reportingDestination_bareOfNullGone() {
        assertEquals(0, count(cell(DRR_POJO_RULES[1]),
                        "return MapperS.<SupervisoryBodyEnum>ofNull();"),
                "The bare-element ofNull terminal is gone (count 0 in golden)");
    }

    /**
     * The bare-item comparison-operand witness (cftc-margin
     * DTCC_TradeParty1ReportingDestination): the un-deref'd operand
     * {@code , item, CardinalityOperator.Any)} counted EXACTLY 2 occurrences in the PRE
     * gen and 0 in the golden — the retyped operands deref
     * {@code .<SupervisoryBodyEnum>map("Type coercion", …)} with numbered params.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void reportingDestination_bareItemOperandsGone() {
        assertEquals(0, count(cell(DRR_POJO_RULES[1]), ", item, CardinalityOperator.Any)"),
                "The un-deref'd bare-item comparison operands are gone (count 0 in golden)");
    }

    /**
     * The groupPCounterpartyBlockRelocate witness (asic valuation Counterparty2Rule):
     * the inline ternary {@code .getOrDefault(false) ? MapperS.of(partyLeiAndPersonByRoles}
     * counted EXACTLY 1 occurrence in the PRE gen and 0 in the golden — the
     * effective-else block admits the evaluate-arg hoists into the if-branch.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void counterparty2_inlineTernaryGone() {
        assertEquals(0, count(cell(DRR_POJO_RULES[0]),
                        ".getOrDefault(false) ? MapperS.of(partyLeiAndPersonByRoles"),
                "The inline-ternary form is gone (count 0 in golden)");
    }

    /**
     * The fnAliasListLiteralJoinSanitize witness (SortIdentifiers): the symbol-echo
     * signature {@code MapperC<indexIdentifierFiltered>} counted EXACTLY 2 occurrences
     * in the PRE gen (abstract + impl) and 0 in the golden — the parser-side element
     * JOIN replaces the echo with {@code MapperC<String>}.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void sortIdentifiers_symbolEchoSignatureGone() {
        assertEquals(0, count(fn(drrFnOutput, DRR_FUNCTIONS[1]),
                        "MapperC<indexIdentifierFiltered>"),
                "The symbol-echo alias signature is gone (count 0 in golden)");
    }

    /**
     * The fnBooleanParamCondition witness (SortIdentifiers): the Mapper-wrapped
     * condition {@code MapperS.of(isMin).getOrDefault(false)} counted EXACTLY 1
     * occurrence in the PRE gen and 0 in the golden — the raw-param null-guard ternary
     * replaces it.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void sortIdentifiers_wrappedBoolConditionGone() {
        assertEquals(0, count(fn(drrFnOutput, DRR_FUNCTIONS[1]),
                        "MapperS.of(isMin).getOrDefault(false)"),
                "The Mapper-wrapped Boolean-param condition is gone (count 0 in golden)");
    }

    /**
     * The metaCollapseConsumerTypeStamp SET witness (cftc OriginalSwapUTIRule): the
     * bare whole-output assign {@code output = thenArg1} counted EXACTLY 1 occurrence
     * in the PRE gen and 0 in the golden — the stamped consumer fires the hoist +
     * if-null deref block.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void originalSwapUti_bareOutputAssignGone() {
        assertEquals(0, count(cell(DRR_POJO_RULES[2]), "output = thenArg1"),
                "The bare whole-output assign is gone (count 0 in golden)");
    }

    /**
     * The metaCollapseConsumerTypeStamp flatten-collapse witness
     * (GetQuantityForConstituent): the bare toBuilder assign
     * {@code result = toBuilder(thenArg1} counted EXACTLY 1 occurrence in the PRE gen
     * and 0 in the golden — the S3 re-wrap + stamp fire the hoist + if-null block over
     * {@code MapperS.of(thenArg1.flattenList().get()).get()}.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void getQuantityForConstituent_bareToBuilderGone() {
        assertEquals(0, count(fn(drrFnOutput, DRR_FUNCTIONS[0]), "result = toBuilder(thenArg1"),
                "The bare toBuilder whole-output assign is gone (count 0 in golden)");
    }

    /**
     * The aliasDefaultJoin witness (MapEarlyTerminationProvisionToAncillaryParty cdm6):
     * the witness-less nav {@code .map("getCalculationAgent", _mandatoryEarlyTermination}
     * counted EXACTLY 2 occurrences in the PRE gen and 0 in the golden — the default-join
     * resolution restores the {@code <CalculationAgent>} witness cascade.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapEarlyTermination_witnesslessNavGone() {
        assertEquals(0, count(fn(cdm6FnOutput, CDM6_FUNCTIONS[0]),
                        ".map(\"getCalculationAgent\", _mandatoryEarlyTermination"),
                "The witness-less getCalculationAgent nav is gone (count 0 in golden)");
    }

    // ----------------------------------------------------------------- helpers

    private static void assertCellByteMatchesGolden(String path) throws IOException {
        assertBytes(path, cell(path), DRR_GOLDEN_DIR);
    }

    private static String cell(String path) {
        assertNotNull(drrCellOutput, "drr cell generation did not run — corpus unavailable?");
        String generated = drrCellOutput.get(path);
        assertNotNull(generated, "Class not generated: " + path);
        return generated;
    }

    private static String fn(Map<String, String> output, String path) {
        assertNotNull(output, "function generation did not run — corpus unavailable?");
        String generated = output.get(path);
        assertNotNull(generated, "Class not generated: " + path);
        return generated;
    }

    private static void assertBytes(String path, String generated, Path goldenDir)
            throws IOException {
        Path goldenPath = goldenDir.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated output must byte-match the golden (newline-normalized) for "
                + path + " (PR #362).");
    }

    private static int count(String text, String token) {
        int n = 0;
        int i = text.indexOf(token);
        while (i >= 0) {
            n++;
            i = text.indexOf(token, i + token.length());
        }
        return n;
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
