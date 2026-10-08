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
 * PR #366 — the Void chain constant fold + the in-lambda ctor singletonList hoist + the
 * FQN self-collision + the DATE-accessor method refs: 8 byte flips (2 cdm6 FUNCTION +
 * 5 drr FUNCTION + 1 drr POJO Rule) + 0 new / 1 line-ratio-flagged mover content-verified
 * TOWARD (regscan366: 8/0/1 — ExtractFinalContractualSettlementDate's witness counts
 * moved decl 1→8 toward golden's 16 and runtime-then 16→9 toward golden's 0; the ratio
 * metric lies in both directions, the #365 cp8c law).
 *
 * <p><b>date_accessor_method_ref</b> (NavigationHandler): the {@code year}/{@code month}/
 * {@code day} accessors on a DATE-record receiver render the upstream RecordJavaUtil
 * RDateType dispatch form — {@code .<Integer>map("Year", Date::getYear)}, the
 * capitalized-feature label + the bare method ref, no lambda-var registration — at the
 * direct record-nav seat, behind an own nav-seat-only gate (the shared
 * {@code isDateRecordFeature} consumers untouched, the #365 positive-seat-gate law):
 * CompareDateTo drr.
 *
 * <p><b>voidChainConstantFold</b> (FunctionExpressionRenderer + ReferenceHandler):
 * upstream {@code TypeCoercionService.addCoercions} DISCARDS any builder whose ITEM type
 * is Void — statements included — and returns the canonical empty of the expected type.
 * The rule then-chain loop folds a NOTHING-typed step ({@code final MapperS<Void>
 * thenArg1 = MapperS.<Void>ofNull();}), skips every dead decl before it (numbering
 * preserved through the session), re-roots the next step on a FRESH constant (upstream's
 * key synonym holds the coerced constant expression, never the declared name), and the
 * implicit fn-arg over a Void-piped item renders the bare {@code null}
 * ({@code convertNonISOToISOCurrency.evaluate(null)}): SettlementCurrency2Rule esma drr.
 *
 * <p><b>inLambdaCtorSingletonHoist</b> (ConstructionHandler): the single-into-multi ctor
 * value inside a drainable map/extract lambda hoists through the per-lambda channel
 * (compileLambda converts the body to a block with the decl at the top) and splices the
 * null-guarded {@code Collections.<X>emptyList() : Collections.singletonList(x)}
 * ternary; the decl renders through the DEFERRED token (the DeepThenArgHoist
 * token-statement pattern) so decl + splice finalize to ONE name and escape together
 * against an outer statement-seat session local ({@code _assignedIdentifier}, the #333
 * decl-use-consistency law): MapEventIdentifier + MapReturnSwapDividendReturnTerms cdm6
 * + GetNrgySpcfcAttrbts esma + fca drr (the #365 family-yield law — the census tagged
 * one solo, the mechanism landed four).
 *
 * <p><b>condAliasWildcardThenArg</b> (NavigationHandler): a CONDITIONAL alias body (the
 * if/else-if nav ladder) recovers its element from the first MODEL-typed ladder arm the
 * shared walk resolves, so the thenArg decl agrees with the alias method signature's
 * {@code MapperC<? extends Product>} (the #178 same-walk law; RDataTypeRef/RChoiceTypeRef
 * at the RTYPE level per the #169 law): GetBasketConstituents drr.
 *
 * <p><b>fqnSelfCollision</b> (FunctionGenerator + FunctionTemplateModel + the ST
 * templates): a model OUTPUT type whose simple name equals the ACTUALLY-EMITTED class's
 * own name renders FULLY-QUALIFIED at every template site with its import suppressed
 * (typeName=FQN, typeFqn=null — the #195/#6 convention; gated on
 * collisionHostSimpleName, so a rule named X producing type X emits class XRule with no
 * collision); the resolver seeds the class's OWN canonical first so body sentinels agree;
 * the {@code <Type>.<Type>Builder} compositions take the SIMPLE name for the Builder
 * segment: TechnicalRecordId drr.
 *
 * <p><b>the mapperCIteLift item-rooted-nav widening + the arm-interior
 * together-restructure</b> (FunctionExpressionRenderer + CollectionHandler +
 * ConstructionHandler): a then-arm NAVIGATING off the piped item over a MapperC-bound
 * pipe lifts the ite decl to MapperC; the #257 conditional-slot arm compile pushes the
 * #356 chain-top flag so tryDeepThenHoist's all-or-nothing guard exempts arm-interior
 * chains (golden's together-restructure — the in-arm {@code final
 * MapperC<FieldWithMetaString> thenArg2} hoist); the ite arm-hoist drains widen to the
 * session-active FUNCTION path (a function-path arm-registered hoist previously VANISHED
 * leaving a dangling sentinel — non-compiling, so green-safe by construction);
 * valueCarriesAttributeMeta gains the element-preserving THEN-CHAIN arm (setId, the
 * PLAIN setter over the wrapper element): TechnicalRecordId drr (with fqnSelfCollision).
 *
 * <p>Whole-file byte comparisons run through the REAL D11 generation paths and revert
 * RED without the facets. The negative witnesses are load-bearing per the
 * witness-uniqueness law (OCCURRENCE counts, never line counts): every token below was
 * occurrence-counted in its PRE gen (f-probe-365post; counts noted per witness) and 0
 * in its golden — the flips REMOVE them.
 */
class VoidCtorHoistFqnComposeTest {

    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    /** The 2 cdm6 FUNCTION flip carriers (inLambdaCtorSingletonHoist). */
    private static final String[] CDM6_FUNCTIONS = {
            "cdm/ingest/fpml/confirmation/workflowstep/functions/MapEventIdentifier.java",
            "cdm/ingest/fpml/confirmation/product/returnswap/functions/MapReturnSwapDividendReturnTerms.java",
    };

    /** The 5 drr FUNCTION flip carriers. */
    private static final String[] DRR_FUNCTIONS = {
            "drr/regulation/common/util/functions/CompareDateTo.java",
            "drr/projection/iso20022/esma/emir/refit/trade/functions/GetNrgySpcfcAttrbts.java",
            "drr/projection/iso20022/fca/ukemir/refit/trade/functions/GetNrgySpcfcAttrbts.java",
            "drr/regulation/common/trade/basket/functions/GetBasketConstituents.java",
            "drr/regulation/common/trade/link/functions/TechnicalRecordId.java",
    };

    /** The 1 drr POJO Rule flip carrier (voidChainConstantFold). */
    private static final String[] DRR_POJO_RULES = {
            "drr/regulation/esma/emir/refit/trade/reports/SettlementCurrency2Rule.java",
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

    // -------------------------------------------- cdm6 FUNCTION byte locks (2)

    @Test
    @EnabledIf("cdm6CellAvailable")
    void cdm6Functions_byteMatchGolden() throws IOException {
        for (String path : CDM6_FUNCTIONS) {
            assertBytes(path, fn(cdm6FnOutput, path), CDM6_GOLDEN_DIR);
        }
    }

    // -------------------------------------------- drr FUNCTION byte locks (5)

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
     * The date_accessor_method_ref witness (CompareDateTo drr): the getter-lambda form
     * {@code map("getYear", _date -> _date.getYear())} counted EXACTLY 2 occurrences in
     * the PRE gen (f-probe-365post) and 0 in the golden — the record-nav arm renders
     * {@code .<Integer>map("Year", Date::getYear)}.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void compareDateTo_getterLambdaGone() {
        assertEquals(0, count(fn(drrFnOutput, DRR_FUNCTIONS[0]),
                        "map(\"getYear\", _date -> _date.getYear())"),
                "The getter-lambda date accessor is gone (count 0 in golden)");
    }

    /**
     * The voidChainConstantFold witnesses (SettlementCurrency2Rule esma): the null-body
     * step {@code .mapSingleToItem(item -> null)} and the un-folded item arg
     * {@code convertNonISOToISOCurrency.evaluate(item.get())} each counted EXACTLY 1
     * occurrence in the PRE gen and 0 in the golden — the fold renders the constant decl
     * + the fresh-constant re-root + {@code evaluate(null)}.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void settlementCurrency2_voidFoldWitnesses() {
        String gen = cell(DRR_POJO_RULES[0]);
        assertEquals(0, count(gen, ".mapSingleToItem(item -> null)"),
                "The null-body then step is gone (count 0 in golden)");
        assertEquals(0, count(gen, "convertNonISOToISOCurrency.evaluate(item.get())"),
                "The un-folded Void item arg is gone (count 0 in golden)");
    }

    /**
     * The inLambdaCtorSingletonHoist witnesses: the INLINE nested-ctor setter args —
     * {@code .setAssignedIdentifier(AssignedIdentifier.builder()} (MapEventIdentifier),
     * {@code .setDividendPayoutRatio(DividendPayoutRatio.builder()}
     * (MapReturnSwapDividendReturnTerms), {@code .setDlvryIntrvl(TimePeriodDetails1
     * .builder()} (GetNrgySpcfcAttrbts esma + fca) — each counted EXACTLY 1 occurrence
     * in its PRE gen and 0 in its golden: the value hoists to the lambda-top local and
     * the setter splices the null-guarded singletonList ternary.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void inLambdaCtorHoist_cdm6InlineNestedCtorsGone() {
        assertEquals(0, count(fn(cdm6FnOutput, CDM6_FUNCTIONS[0]),
                        ".setAssignedIdentifier(AssignedIdentifier.builder()"),
                "MapEventIdentifier's inline nested ctor is gone (count 0 in golden)");
        assertEquals(0, count(fn(cdm6FnOutput, CDM6_FUNCTIONS[1]),
                        ".setDividendPayoutRatio(DividendPayoutRatio.builder()"),
                "MapReturnSwapDividendReturnTerms's inline nested ctor is gone (0 in golden)");
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void inLambdaCtorHoist_drrInlineNestedCtorsGone() {
        assertEquals(0, count(fn(drrFnOutput, DRR_FUNCTIONS[1]),
                        ".setDlvryIntrvl(TimePeriodDetails1.builder()"),
                "GetNrgySpcfcAttrbts esma's inline nested ctor is gone (0 in golden)");
        assertEquals(0, count(fn(drrFnOutput, DRR_FUNCTIONS[2]),
                        ".setDlvryIntrvl(TimePeriodDetails1.builder()"),
                "GetNrgySpcfcAttrbts fca's inline nested ctor is gone (0 in golden)");
    }

    /**
     * The condAliasWildcardThenArg witness (GetBasketConstituents drr): the INVARIANT
     * decl {@code final MapperC<Product> thenArg0 = basketConstituents(trade);} counted
     * EXACTLY 1 occurrence in the PRE gen and 0 in the golden — the conditional-alias
     * element recovery types the decl {@code MapperC<? extends Product>}, agreeing with
     * the alias method signature.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void getBasketConstituents_invariantDeclGone() {
        assertEquals(0, count(fn(drrFnOutput, DRR_FUNCTIONS[3]),
                        "final MapperC<Product> thenArg0 = basketConstituents(trade);"),
                "The invariant alias-call thenArg decl is gone (count 0 in golden)");
    }

    /**
     * The fqnSelfCollision + together-restructure witnesses (TechnicalRecordId drr): the
     * self-colliding import {@code import drr.regulation.common.TechnicalRecordId;}, the
     * VALUE setter {@code .setIdValue(}, and the spurious rebind {@code final
     * MapperS<TechnicalRecordId> thenArg2 = ifThenElseResult;} each counted EXACTLY 1
     * occurrence in the PRE gen and 0 in the golden — the class claims its simple name
     * (every model-type mention FQN, import suppressed), the element-preserving
     * then-chain proof keeps the wrapper at the PLAIN {@code setId} setter, and the
     * MapperC-lifted ite binds the next step directly.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void technicalRecordId_selfCollisionWitnesses() {
        String gen = fn(drrFnOutput, DRR_FUNCTIONS[4]);
        assertEquals(0, count(gen, "import drr.regulation.common.TechnicalRecordId;"),
                "The self-colliding model-type import is gone (count 0 in golden)");
        assertEquals(0, count(gen, ".setIdValue("),
                "The VALUE setter over the wrapper element is gone (count 0 in golden)");
        assertEquals(0, count(gen, "final MapperS<TechnicalRecordId> thenArg2 = ifThenElseResult;"),
                "The spurious ite rebind decl is gone (count 0 in golden)");
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
