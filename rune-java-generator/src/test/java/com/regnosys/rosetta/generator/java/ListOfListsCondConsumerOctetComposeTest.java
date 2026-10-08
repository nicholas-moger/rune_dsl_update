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
 * PR #384 — the listoflists/wrapper-keep/cond-consumer octet (8 byte flips:
 * Create_AdjustmentPrimitiveInstruction cdm5+cdm6 + Create_RepricePrimitiveInstruction
 * cdm5+cdm6 + Qualify_OnDemandRateChange cdm5+cdm6 [FUNCTION] +
 * UnderlyingIdOtherDTCCRule + UnderlyingIdOtherSourceDTCCRule [drr POJO mas]).
 *
 * <p><b>bareSymbolBodyCardinality</b> (CollectionHandler.isBodyMulti +
 * NavigationHandler): a BARE-symbol extract body whose {@code RSymbolReference} never
 * resolved (symbol EMPTY — T384A: {@code symEmpty=true, card=SINGLE}) but whose name
 * is a MULTI attribute of the implicit item proves the body multi — the 1-name
 * sibling of the #347 disguised 2-name arm, same render-truth walk
 * ({@code implicitItemDataTypeOrInferred} + {@code findAttributeOnDataType}),
 * monotone add-only by the #325 mirror argument. The
 * {@code mapItemToList}/{@code MapperListOfLists} hoist + the wrapper-kept flatten
 * element + the per-use Type-coercion derefs all flow from the existing #293/#350
 * machinery once the method selects right ({@code oldPriceQuantity extract price
 * then flatten …} — price is 0..* FieldWithMetaPriceSchedule on PriceQuantity).
 *
 * <p><b>listOfListsAliasSig</b> (FunctionAliasHelper): a {@code then extract <body>}
 * step over a MULTI receiver is elementwise MULTI regardless of the body's own
 * cardinality (upstream extract PRESERVES receiver cardinality), so the alias
 * signature types {@code MapperC<BigDecimal>} — the (iii) attr-root composition
 * deliberately excluded extract-shaped bodies.
 *
 * <p><b>listOfListsWrapperKeep</b> (ConstructionHandler + NavigationHandler):
 * (i) {@code valueCarriesAttributeMeta} gains the element-preserving FILTER recurse
 * + the bare-symbol-EMPTY implicit-item META resolve, so the ctor setter keeps the
 * PLAIN form over wrapper pipes (golden {@code .setPrice(…)} — the #348 corpus law:
 * ZERO goldens carry {@code set<X>Value} over wrapper elements); (ii) the #342
 * closureParamDirectNav arm recovers a bare-symbol-EMPTY owner argument's terminal
 * attribute via the same walk, stamping the explicit filter param as the wrapper so
 * {@code coerceNavigationReceiver} emits golden's per-use deref inside the predicate.
 *
 * <p><b>inLambdaCondConsumerDecomp</b> (CollectionHandler + ControlFlowHandler): the
 * CONSUMER twin of the #374 base admit — a RULE-path in-lambda then-chain whose ONLY
 * cond level is the LAST body admits the lambda channel (rule-path hoist naming is
 * per-scope literal, the #383 renumber-safety law; the #351 n==1 consumer decline
 * strands nothing). The consumer pre-creates a per-scope {@code ifThenElseResult}
 * deferred token and pushes the #351 handshake on the #374 4-arg lambda-route ctor;
 * the blessed arm's {@code DeepThenCondArgHoist} ladder drains INTO the owning rung
 * with the level decls; a #204-class mis-bound bare-symbol arm the enum recovery
 * QUALIFIED ({@code then Other} → {@code ProductIdTypeEnum.OTHER}) wraps
 * {@code MapperS.of} by the #371 render-truth arbiter (dotted parenless null-typed
 * render); and the block-ladder deref pass gains the COMPILED-bare sibling route (a
 * leaf arm whose compiled item type IS the meta-blind join beside a compiled
 * wrapper-of-the-join arm joins BARE — both sides compiled-type-only, the #361-cp6
 * containment), so the DTCC identifier leaf derefs at the rung return.
 *
 * <p>Whole-file byte locks run through the REAL D11 routes (drr full cell +
 * cdm5/cdm6 FUNCTION) and revert RED without the facets; every witness token is
 * occurrence-counted (python {@code str.count} semantics — the #352 law) and
 * PRE-counted against f-probe-383post (each removal token PRE &ge; 1 / golden 0;
 * each golden token PRE 0 / golden &ge; 1).
 */
class ListOfListsCondConsumerOctetComposeTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM5_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-5.38.0");
    private static final Path CDM5_GOLDEN_DIR =
            CDM5_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static final String ADJUSTMENT =
            "cdm/event/common/functions/Create_AdjustmentPrimitiveInstruction.java";
    private static final String REPRICE =
            "cdm/event/common/functions/Create_RepricePrimitiveInstruction.java";
    private static final String QUALIFY =
            "cdm/event/common/functions/Qualify_OnDemandRateChange.java";
    private static final String MAS_DTCC =
            "drr/regulation/mas/rewrite/trade/reports/UnderlyingIdOtherDTCCRule.java";
    private static final String MAS_SOURCE_DTCC =
            "drr/regulation/mas/rewrite/trade/reports/UnderlyingIdOtherSourceDTCCRule.java";

    private static Map<String, String> drrOutput;
    private static Map<String, String> cdm5FnOutput;
    private static Map<String, String> cdm6FnOutput;

    static boolean drrCellAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR);
    }

    static boolean cdm5CellAvailable() {
        return Files.isDirectory(CDM5_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM5_GOLDEN_DIR);
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
        if (cdm5CellAvailable()) {
            cdm5FnOutput = generateFunctions(
                    new D11CorpusRegressionTest.CellSpec("cdm", "5.38.0", CDM5_CELL_ROOT));
        }
        if (cdm6CellAvailable()) {
            cdm6FnOutput = generateFunctions(
                    new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CDM6_CELL_ROOT));
        }
    }

    /**
     * The D11-shaped POJO/rule generation path — generator wiring identical to
     * {@code D11CorpusRegressionTest}'s cell loop (same emission filter, same
     * generator classes and order). The harness-side extras (doNotPrune config,
     * generator-error capture) and the standalone function-file emission are
     * deliberately omitted: this helper serves only the two mas POJO rule byte
     * locks, whose renders are byte-verified against golden below (Copilot R1
     * #384 — the label and the function-emission cost).
     */
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
        return output;
    }

    /** The REAL D11 FUNCTION-cell generation path. */
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

    // ==== byte locks (all 8 flips through the REAL D11 routes) ====

    /** bareSymbolBodyCardinality: the pure listoflists carrier, cdm5. */
    @Test
    @EnabledIf("cdm5CellAvailable")
    void adjustmentCdm5_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5FnOutput, CDM5_GOLDEN_DIR, ADJUSTMENT);
    }

    /** bareSymbolBodyCardinality: the pure listoflists carrier, cdm6. */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void adjustmentCdm6_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, ADJUSTMENT);
    }

    /** bareSymbolBodyCardinality: the reprice sibling, cdm5. */
    @Test
    @EnabledIf("cdm5CellAvailable")
    void repriceCdm5_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5FnOutput, CDM5_GOLDEN_DIR, REPRICE);
    }

    /** bareSymbolBodyCardinality: the reprice sibling, cdm6. */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void repriceCdm6_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, REPRICE);
    }

    /** listOfListsAliasSig + listOfListsWrapperKeep: the composite carrier, cdm5. */
    @Test
    @EnabledIf("cdm5CellAvailable")
    void qualifyCdm5_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5FnOutput, CDM5_GOLDEN_DIR, QUALIFY);
    }

    /** listOfListsAliasSig + listOfListsWrapperKeep: the composite carrier, cdm6. */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void qualifyCdm6_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, QUALIFY);
    }

    /** inLambdaCondConsumerDecomp: the deref-carrying mas twin. */
    @Test
    @EnabledIf("drrCellAvailable")
    void masUnderlyingIdOtherDTCCRule_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrOutput, DRR_GOLDEN_DIR, MAS_DTCC);
    }

    /** inLambdaCondConsumerDecomp: the enum-arm mas twin. */
    @Test
    @EnabledIf("drrCellAvailable")
    void masUnderlyingIdOtherSourceDTCCRule_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrOutput, DRR_GOLDEN_DIR, MAS_SOURCE_DTCC);
    }

    // ==== occurrence-counted witnesses (python str.count semantics — the #352 law) ====

    /**
     * bareSymbolBodyCardinality: the mapItemToList hoist (PRE 0 / golden 1), the
     * wrapper-kept flatten decl (PRE 0 / golden 1), the MapperListOfLists import
     * (PRE 0 / golden 1); negatives — the mapItem wrong-method form (PRE 1 /
     * golden 0) and the meta-stripped flatten decl (PRE 1 / golden 0).
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void adjustment_listOfListsHoist_witness() {
        String gen = cdm6FnOutput.get(ADJUSTMENT);
        assertNotNull(gen, "Create_AdjustmentPrimitiveInstruction not generated");
        assertEquals(1, count(gen,
                ".mapItemToList(item -> item.<FieldWithMetaPriceSchedule>mapC("),
                "the list-typed body hoists via mapItemToList (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "final MapperC<FieldWithMetaPriceSchedule> thenArg1"),
                "the flatten continuation keeps the META element (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "import com.rosetta.model.lib.mapper.MapperListOfLists;"),
                "the MapperListOfLists import lands (PRE 0 / golden 1)");
        assertEquals(0, count(gen,
                ".mapItem(item -> item.<FieldWithMetaPriceSchedule>mapC("),
                "the mapItem wrong-method form must be gone (PRE 1 / golden 0)");
        assertEquals(0, count(gen, "final MapperC<PriceSchedule> thenArg1"),
                "the meta-stripped flatten decl must be gone (PRE 1 / golden 0)");
    }

    /**
     * listOfListsAliasSig + listOfListsWrapperKeep: the MapperC alias signature
     * (PRE 0 / golden 1), the PLAIN wrapper setter (PRE 0 / golden 2); negatives —
     * the MapperS signature echo (PRE 1 / golden 0) and the Value-form setter over
     * the wrapper pipe (PRE 2 / golden 0).
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void qualify_aliasSigSetterWrapperKeep_witness() {
        String gen = cdm6FnOutput.get(QUALIFY);
        assertNotNull(gen, "Qualify_OnDemandRateChange not generated");
        assertEquals(1, count(gen,
                "protected abstract MapperC<BigDecimal> beforePriceQuantityRateOnly"),
                "the then-extract alias signature types MapperC (PRE 0 / golden 1)");
        assertEquals(2, count(gen, ".setPrice(item.<FieldWithMetaPriceSchedule>mapC("),
                "the ctor keeps the PLAIN setter over the wrapper pipe (PRE 0 / golden 2)");
        assertEquals(0, count(gen,
                "protected abstract MapperS<BigDecimal> beforePriceQuantityRateOnly"),
                "the single-form signature echo must be gone (PRE 1 / golden 0)");
        assertEquals(0, count(gen, ".setPriceValue(item.<FieldWithMetaPriceSchedule>mapC("),
                "the Value-form setter over the wrapper pipe must be gone (PRE 2 / golden 0)");
    }

    /**
     * inLambdaCondConsumerDecomp (the enum-arm twin): the receiver hoist (PRE 0 /
     * golden 1), the blank-final ladder decl (PRE 0 / golden 1), the wrapped
     * recovery-qualified enum arm (PRE 0 / golden 1); negative — the runtime
     * {@code .then(} form (PRE 1 / golden 0).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void masSourceDTCC_condConsumerLadder_witness() {
        String gen = drrOutput.get(MAS_SOURCE_DTCC);
        assertNotNull(gen, "UnderlyingIdOtherSourceDTCCRule not generated");
        assertEquals(1, count(gen,
                "final MapperS<String> _thenArg0 = MapperS.of(extractCommodityClassification"),
                "the base hoists through the per-scope _thenArg channel (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "final MapperS<ProductIdTypeEnum> ifThenElseResult;"),
                "the blank-final ladder decl lands in-branch (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "ifThenElseResult = MapperS.of(ProductIdTypeEnum.OTHER);"),
                "the recovery-qualified enum arm wraps MapperS.of (PRE 0 / golden 1)");
        assertEquals(0, count(gen, ".then(_item ->"),
                "the runtime .then( form must be gone (PRE 1 / golden 0)");
    }

    /**
     * inLambdaCondConsumerDecomp (the deref twin): the wrapped string-literal arm
     * (PRE 0 / golden 1), the compiled-bare-vs-wrapper join deref at the rung
     * return (PRE 0 / golden 1), the blank-final decl (PRE 0 / golden 1);
     * negative — the runtime {@code .then(} form (PRE 1 / golden 0).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void masDTCC_condConsumerDeref_witness() {
        String gen = drrOutput.get(MAS_DTCC);
        assertNotNull(gen, "UnderlyingIdOtherDTCCRule not generated");
        assertEquals(1, count(gen, "ifThenElseResult = MapperS.of(\"OTHER\");"),
                "the string-literal arm wraps MapperS.of (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "getIdentifier()).<String>map(\"Type coercion\""),
                "the identifier leaf derefs at the rung return (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "final MapperS<String> ifThenElseResult;"),
                "the blank-final ladder decl lands in-branch (PRE 0 / golden 1)");
        assertEquals(0, count(gen, ".then(_item ->"),
                "the runtime .then( form must be gone (PRE 1 / golden 0)");
    }

    // ==== helpers ====

    private static void assertByteMatchesGolden(Map<String, String> output, Path goldenDir,
            String relPath) throws IOException {
        assertNotNull(output, "cell not generated");
        String gen = output.get(relPath);
        assertNotNull(gen, relPath + " not generated");
        Path goldenPath = goldenDir.resolve(relPath);
        assertTrue(Files.isRegularFile(goldenPath), "golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath).replace("\r\n", "\n").replace("\r", "\n");
        assertEquals(golden, gen.replace("\r\n", "\n").replace("\r", "\n"),
                relPath + " must byte-match golden");
    }

    /** Occurrence count — python str.count semantics (the #352 law). */
    private static int count(String haystack, String needle) {
        int n = 0;
        int idx = 0;
        while ((idx = haystack.indexOf(needle, idx)) >= 0) {
            n++;
            idx += needle.length();
        }
        return n;
    }
}
