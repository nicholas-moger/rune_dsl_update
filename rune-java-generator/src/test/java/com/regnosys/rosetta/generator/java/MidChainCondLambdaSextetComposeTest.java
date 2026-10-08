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
 * PR #392 — the conditional-base/mid-chain-rung then-decomposition sextet (6 byte
 * flips: ExtractReferenceEntity [drr FUNCTION] + MapGenericProductPriceQuantityList
 * [cdm6 FUNCTION] + UnderlyingAssetPriceSourceLeg1/2Rule +
 * ClearingExceptionsAndExemptionsCounterparty1/2Rule [drr POJO]).
 *
 * <p><b>fnCondBaseLadderWrapperElement + fnCondBaseLadderGuardLockstep +
 * fnCondBaseLadderOutputMetaDeref</b> (FunctionExpressionRenderer +
 * CollectionHandler): the FUNCTION-path SET-seat k==0 conditional-base blank-final
 * ladder — the #372 F-β Object-erased decl element recovers via the all-arms-agree
 * wrapper join (alias-call arms through the signature walk, nav arms through the
 * terminal-meta walk, bare-item single-collapse then-steps unwrapped); the
 * all-or-nothing guard consults the compiler-carrying overload at operation-root
 * chains ONLY (the #351-i2 lockstep — the clean alias chains unblock together with
 * the F-β restructure); the collapse consumption reads the DECLARED wrapper element
 * off prevRef (render truth) so the whole-output meta-deref block fires.
 *
 * <p><b>inLambdaArgSeatCondAdmit + ctorArgAliasRefIteType +
 * inLambdaArgBareSymbolIteType + ctorThenExtractFnMetaSetter +
 * thenArgFnExtractCalleeMetaElement</b> (CollectionHandler + ControlFlowHandler +
 * ConstructionHandler): a {@code then extract <Fn>(…)} body whose ONLY ctl is
 * ELSELESS arg-root conditionals reads HANDLED (the #355 lambda channel hoists the
 * mutable-null {@code <T> ifThenElseResult = null; if …} block); the #365-seat
 * typing ladder gains the bare alias-ref and fn-arg bare-symbol arms; a then-chain
 * ctor value proves meta from its LAST fn-extract body's callee output (the
 * setQuantity/setObservable wrapper setters); the deep-seat k&gt;0 decl element
 * types the WRAPPER from a meta-annotated callee output.
 *
 * <p><b>ruleMidChainCondLambdaAdmit</b> (CollectionHandler + ControlFlowHandler +
 * NavigationHandler): the RULE-path lambda channel admits conditional levels at ANY
 * chain position (the #374/#384 admits generalized — per-scope literal naming, the
 * #383 law); the ifThenElseResult tokens create LATE inside the handshake AFTER its
 * arm walk (consumption order — the #327 law: golden UAPSL1 numbers the
 * arm-interior ite 0 and its outer rung 1); the #381-D4b arm drain pulls
 * DeepThenCondArgHoist blocks in-branch; the rung-body predicate gains the
 * RULE-path confined-arm-chain disjunct; a resolved meta-FREE nav leaf level skips
 * the #144 wrapper re-leak; chainRendersMapperC gains the with-args MULTI-callee
 * fn-call receiver arm and the bound-then-pipe implicit binding read (the #362
 * channel), so MapperC-piped meta derefs render the BARE non-registering form (the
 * #310 law) and the sibling filter params stay unescaped.
 *
 * <p>Whole-file byte locks run through the REAL D11 routes (FUNCTION: cdm6 + drr;
 * POJO/Rule: drr) and revert RED without the facets; every witness token is
 * occurrence-counted (python {@code str.count} semantics — the #352 law) and
 * PRE-counted against f-probe-391post (witness392.py).
 */
class MidChainCondLambdaSextetComposeTest {

    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static final String ERE =
            "drr/regulation/common/functions/ExtractReferenceEntity.java";
    private static final String MGPPQL =
            "cdm/ingest/fpml/confirmation/product/genericproduct/functions/MapGenericProductPriceQuantityList.java";
    private static final String UAPSL1 =
            "drr/standards/iosco/cde/version3/underlier/reports/UnderlyingAssetPriceSourceLeg1Rule.java";
    private static final String UAPSL2 =
            "drr/standards/iosco/cde/version3/underlier/reports/UnderlyingAssetPriceSourceLeg2Rule.java";
    private static final String CE1 =
            "drr/regulation/csa/rewrite/trade/reports/ClearingExceptionsAndExemptionsCounterparty1Rule.java";
    private static final String CE2 =
            "drr/regulation/csa/rewrite/trade/reports/ClearingExceptionsAndExemptionsCounterparty2Rule.java";

    private static Map<String, String> cdm6FnOutput;
    private static Map<String, String> drrFnOutput;
    private static Map<String, String> drrPojoOutput;

    static boolean cellsAvailable() {
        return Files.isDirectory(CDM6_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM6_GOLDEN_DIR)
                && Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (cellsAvailable()) {
            cdm6FnOutput = generateFunctions(
                    new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CDM6_CELL_ROOT));
            drrFnOutput = generateFunctions(
                    new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT));
            drrPojoOutput = generatePojoCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT));
        }
    }

    /**
     * The REAL D11 FUNCTION-cell generation path (readDoNotPrune deliberately
     * omitted — correct for function cells, the Seat-1 #386 OBS-5 note).
     */
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

    /**
     * The REAL D11 POJO-cell generation path (Rule/Report/LabelProvider included) —
     * mirrors {@code D11CorpusRegressionTest#pojo_comparison}'s wiring (the
     * RuleIteHoistTest harness).
     */
    private static Map<String, String> generatePojoCell(D11CorpusRegressionTest.CellSpec cell)
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
        return output;
    }

    // ==== byte locks (all 6 flips through the REAL D11 routes) ====

    /** fnCondBaseLadder*: the k==0 conditional-base blank-final ladder carrier. */
    @Test
    @EnabledIf("cellsAvailable")
    void extractReferenceEntity_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFnOutput, DRR_GOLDEN_DIR, ERE);
    }

    /** inLambdaArgSeatCondAdmit + the setter/typing arms: the 4-zone carrier. */
    @Test
    @EnabledIf("cellsAvailable")
    void mapGenericProductPriceQuantityList_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, MGPPQL);
    }

    /** ruleMidChainCondLambdaAdmit: the nested arm-chain + consumption-order carrier. */
    @Test
    @EnabledIf("cellsAvailable")
    void underlyingAssetPriceSourceLeg1_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrPojoOutput, DRR_GOLDEN_DIR, UAPSL1);
    }

    /** ruleMidChainCondLambdaAdmit: the flat mid-chain rung carrier (bCCcc). */
    @Test
    @EnabledIf("cellsAvailable")
    void underlyingAssetPriceSourceLeg2_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrPojoOutput, DRR_GOLDEN_DIR, UAPSL2);
    }

    /** the bound-pipe MapperC bare-deref carrier (csa). */
    @Test
    @EnabledIf("cellsAvailable")
    void clearingExceptionsCounterparty1_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrPojoOutput, DRR_GOLDEN_DIR, CE1);
    }

    /** the bound-pipe MapperC bare-deref twin (csa). */
    @Test
    @EnabledIf("cellsAvailable")
    void clearingExceptionsCounterparty2_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrPojoOutput, DRR_GOLDEN_DIR, CE2);
    }

    // ==== occurrence-counted witnesses (python str.count semantics — the #352 law) ====

    /**
     * fnCondBaseLadderWrapperElement + fnCondBaseLadderOutputMetaDeref (drr
     * ExtractReferenceEntity): the blank-final wrapper-element ladder (PRE 0 /
     * golden 1), the MapperS-alias-call arm's MapperC.of join at both alias arms
     * and the arm-interior collapse (PRE 0 / golden 1 each), the whole-output
     * meta-deref hoist with the method-wide numbered coercion param (PRE 0 /
     * golden 1 each); negatives — the Object-typed base decl and the runtime then
     * form are gone (PRE 1 and 6 / golden 0).
     */
    @Test
    @EnabledIf("cellsAvailable")
    void extractReferenceEntity_condBaseLadder_witness() {
        String gen = drrFnOutput.get(ERE);
        assertNotNull(gen, "ExtractReferenceEntity not generated");
        assertEquals(1, count(gen, "final MapperC<FieldWithMetaString> thenArg1;"),
                "the blank-final ladder declares the walk-joined WRAPPER element"
                + " (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "thenArg1 = MapperC.of(referenceEntityByType(reportableEvent));"),
                "the MapperS-typed alias-call arm joins via MapperC.of (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "thenArg1 = MapperC.of(thenArg0"),
                "the arm-interior then-chain drains in-branch and its collapse joins"
                + " via MapperC.of (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "final FieldWithMetaString fieldWithMetaString1 = thenArg1"),
                "the whole-output meta-deref hoists off the DECLARED wrapper element"
                + " (PRE 0 / golden 1)");
        assertEquals(1, count(gen,
                "fieldWithMetaString0 == null ? null : fieldWithMetaString0.getValue()"),
                "the arm-2 guarded coercion param joins the method-wide numbered group"
                + " (PRE 0 / golden 1)");
        assertEquals(0, count(gen, "final MapperC<Object> thenArg"),
                "the Object-typed base decl is gone (PRE 1 / golden 0)");
        assertEquals(0, count(gen, ".then(item ->"),
                "the runtime then form is gone (PRE 6 / golden 0)");
    }

    /**
     * inLambdaArgSeatCondAdmit + ctorArgAliasRefIteType + ctorThenExtractFnMetaSetter
     * + thenArgFnExtractCalleeMetaElement (cdm6 MapGenericProductPriceQuantityList):
     * the ctor-arg alias-ref blank-final (PRE 0 / golden 1), the in-lambda
     * mutable-null ite per extract lambda (PRE 0 / golden 2), the wrapper setters
     * (PRE 0 / golden 1 each), the callee-meta decl element (PRE 0 / golden 1), the
     * elseless SINGLE base + the alias-return collapse re-wrap (PRE 0 / golden 1
     * each); negatives — the Value setters and the runtime then form are gone
     * (PRE 1 + 1 + 5 / golden 0).
     */
    @Test
    @EnabledIf("cellsAvailable")
    void mapGenericProductPriceQuantityList_inLambdaIte_witness() {
        String gen = cdm6FnOutput.get(MGPPQL);
        assertNotNull(gen, "MapGenericProductPriceQuantityList not generated");
        assertEquals(1, count(gen, "final FieldWithMetaString ifThenElseResult;"),
                "the ctor-arg alias-ref conditional hoists the blank-final effective-else"
                + " form (PRE 0 / golden 1)");
        assertEquals(2, count(gen, "Currency ifThenElseResult = null;"),
                "the fn-arg conditionals hoist the mutable-null form per extract lambda"
                + " (PRE 0 / golden 2)");
        assertEquals(1, count(gen, ".setQuantity(thenArg0"),
                "the quantity setter keeps the PLAIN wrapper form (PRE 0 / golden 1)");
        assertEquals(1, count(gen, ".setObservable(thenArg1"),
                "the observable setter keeps the PLAIN wrapper form (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "final MapperC<FieldWithMetaString> thenArg1 = thenArg0"),
                "the k>0 decl element types the callee-output WRAPPER (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "final MapperS<QuotationCharacteristicsModel> thenArg;"),
                "the elseless SINGLE conditional base hoists blank-final (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "return MapperS.of(thenArg1.get());"),
                "the alias-return collapse re-wraps (PRE 0 / golden 1)");
        assertEquals(0, count(gen, ".setQuantityValue("),
                "the bare Value setter is gone (PRE 1 / golden 0)");
        assertEquals(0, count(gen, ".setObservableValue("),
                "the bare Value setter is gone (PRE 1 / golden 0)");
        assertEquals(0, count(gen, ".then(item ->"),
                "the runtime then form is gone (PRE 5 / golden 0)");
    }

    /**
     * ruleMidChainCondLambdaAdmit consumption-order numbering (drr UAPSL1): the
     * arm-interior ite numbers 0 and its OUTER rung numbers 1 (the #327 law — the
     * late-created handshake tokens; PRE 0 / golden 1 each), the in-arm collapse
     * re-wrap assigns the outer rung (PRE 0 / golden 1), the plain-nav-leaf decl
     * keeps the BARE element with the bare MapperC deref beside it (PRE 0 /
     * golden 1 + 2); negative — the runtime then form is gone (PRE 4 / golden 0).
     */
    @Test
    @EnabledIf("cellsAvailable")
    void underlyingAssetPriceSourceLeg1_consumptionOrder_witness() {
        String gen = drrPojoOutput.get(UAPSL1);
        assertNotNull(gen, "UnderlyingAssetPriceSourceLeg1Rule not generated");
        assertEquals(1, count(gen, "final MapperS<String> ifThenElseResult1;"),
                "the OUTER rung's ite numbers AFTER its arm-interior (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "final MapperC<ReportablePriceSource> ifThenElseResult0;"),
                "the arm-interior ite numbers FIRST (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "ifThenElseResult1 = MapperS.of(distinct(thenArg2).get());"),
                "the in-arm distinct-collapse re-wraps into the outer rung"
                + " (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "final MapperC<String> thenArg2 = ifThenElseResult0"),
                "the plain-nav-leaf decl keeps the BARE element — no #144 re-leak"
                + " (PRE 0 / golden 1)");
        assertEquals(2, count(gen, "fieldWithMetaString -> fieldWithMetaString.getValue()"),
                "the MULTI-callee comparand derefs the bare MapperC form"
                + " (PRE 0 / golden 2)");
        assertEquals(0, count(gen, ".then(item ->"),
                "the runtime then form is gone (PRE 4 / golden 0)");
    }

    /**
     * the bound-pipe MapperC binding read (drr CE1): the rung-condition deref over
     * the bound MapperC pipe renders the BARE non-registering form (PRE 0 /
     * golden 1), so the sibling filter params keep their unescaped names —
     * negatives: the escaped params and the runtime then form are gone
     * (PRE 6 + 6 + 13 / golden 0).
     */
    @Test
    @EnabledIf("cellsAvailable")
    void clearingExceptionsCounterparty1_barePipeDeref_witness() {
        String gen = drrPojoOutput.get(CE1);
        assertNotNull(gen, "ClearingExceptionsAndExemptionsCounterparty1Rule not generated");
        assertEquals(1, count(gen,
                "fieldWithMetaRegimeNameEnum -> fieldWithMetaRegimeNameEnum.getValue()"),
                "the bound-pipe rung-condition deref renders the bare MapperC form"
                + " (PRE 0 / golden 1)");
        assertEquals(0, count(gen, "_fieldWithMetaRegimeNameEnum"),
                "the filter params stay unescaped (PRE 6 / golden 0)");
        assertEquals(0, count(gen, "_fieldWithMetaSupervisoryBodyEnum"),
                "the filter params stay unescaped (PRE 6 / golden 0)");
        assertEquals(0, count(gen, ".then(item ->"),
                "the runtime then form is gone (PRE 13 / golden 0)");
    }

    // ==== helpers ====

    /** Occurrence count — python {@code str.count} semantics (the #352 law). */
    private static int count(String haystack, String needle) {
        int n = 0;
        int idx = 0;
        while ((idx = haystack.indexOf(needle, idx)) >= 0) {
            n++;
            idx += needle.length();
        }
        return n;
    }

    private void assertByteMatchesGolden(Map<String, String> output, Path goldenDir, String path)
            throws IOException {
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
        return s.replace("\r\n", "\n");
    }
}
