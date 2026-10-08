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
 * PR #386 — the builder-naming / ladder-decomposition sextet (6 byte flips:
 * AnnaDsbDerived + AnnaDsbUnderlyingRecord [drr POJO data types] + Direction2Leg1Rule +
 * Direction2Leg2Rule [drr POJO Rules] + QuantityFrequencyLeg1 + QuantityFrequencyLeg2
 * [drr FUNCTION]).
 *
 * <p><b>listAddSetImplFieldName</b> (ModelObjectGenerator): the list add/set IMPL
 * bodies derive their param names and {@code this.<field>} refs from
 * {@code fieldName(prop)} — the field-identifier SOT (decap + keyword escape) — not
 * the raw attribute name; the sibling-collision set now holds FIELD identifiers (the
 * Seat-1 #331 re-audit). Carriers: the upper-initial LIST attributes
 * {@code UnderlyingRecord} / {@code ReturnUnderlierID} (golden {@code _underlyingRecord}
 * / {@code this.underlyingRecord} / {@code underlyingRecords}); the builder-INTERFACE
 * declarations keep the RAW name.
 *
 * <p><b>extractLadderConfinedArmDecomp</b> (CollectionHandler): the #357
 * then-extract-ladder consumer ctl test relaxes to arm-ctl CONFINED to admissible
 * arm-root chains (the #381 predicate — T386A census: the widened disjunct fires ONLY
 * for the QFL pair corpus-wide); the walk-side plain-arm leaf admit takes the
 * WINDOW-LESS plain-arm bless disjunct, so walk-admit ⟹ bless-drains (a strict
 * subset — the bless also drains bare-invokable/window-blessed shapes the walk
 * declines; Copilot R1 #386 / Seat-1 OBS-4); RFilterExpr
 * propagates its argument's compiled type (element-preserving — the compiled-type-gated
 * nav coercion emits golden's elementwise deref off filter tails); and a
 * DISCARDABLE-OWNER session's bare {@code thenArg} escapes the in-lambda singleton to
 * {@code _thenArg} (the session-vs-scope collision cell; the LoadTypeRule bisect scoped
 * the read to alias-route sessions whose group is final at arm-compile time).
 *
 * <p><b>nestedCondArmLadderDecomp</b> (CollectionHandler): the confinement predicate
 * recurses into arm-ROOT NESTED conditional ladders (golden Direction2Leg2's arm 2 —
 * the inner if/return tree + inner typed ofNull inside the outer rung); a RULE-path
 * enclosing extract whose lambda body ROOT is an invocation carrying the ladder's
 * chain in an argument is TRANSPARENT to the clean-ladder walk (Direction2Leg1's
 * fn-arg block ladder); and the extract-receiver SINGLE-invocation {@code MapperS.of}
 * wrap lands, RENDER-TRUTH locksteped to the extract's own {@code mapMethod} chooser
 * (the fromRule-converted {@code tradeForEvent} ref has {@code gm.isMulti}
 * misreporting MULTI on the synthesized output — the chained {@code mapSingleToItem}
 * is the arbiter).
 *
 * <p>Whole-file byte locks run through the REAL D11 routes (drr full cell + drr
 * FUNCTION) and revert RED without the facets; every witness token is
 * occurrence-counted (python {@code str.count} semantics — the #352 law) and
 * PRE-counted against f-probe-385post (each removal token PRE &ge; 1 / golden 0;
 * each golden token PRE 0 / golden &ge; 1 — witness386.py).
 */
class BuilderNamingLadderDecompSextetComposeTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static final String ANNA_DERIVED =
            "drr/standards/iosco/upi/AnnaDsbDerived.java";
    private static final String ANNA_RECORD =
            "drr/standards/iosco/upi/AnnaDsbUnderlyingRecord.java";
    private static final String DIRECTION2_LEG1 =
            "drr/standards/iosco/cde/version1/party/reports/Direction2Leg1Rule.java";
    private static final String DIRECTION2_LEG2 =
            "drr/standards/iosco/cde/version1/party/reports/Direction2Leg2Rule.java";
    private static final String QUANTITY_FREQUENCY_LEG1 =
            "drr/regulation/common/trade/quantity/functions/QuantityFrequencyLeg1.java";
    private static final String QUANTITY_FREQUENCY_LEG2 =
            "drr/regulation/common/trade/quantity/functions/QuantityFrequencyLeg2.java";

    private static Map<String, String> drrPojoOutput;
    private static Map<String, String> drrFnOutput;

    static boolean drrCellAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (drrCellAvailable()) {
            var drrSpec = new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT);
            drrPojoOutput = generateCell(drrSpec);
            drrFnOutput = generateFunctions(drrSpec);
        }
    }

    /**
     * The D11-shaped POJO/rule generation path — generator wiring identical to
     * {@code D11CorpusRegressionTest}'s cell loop (same emission filter, same
     * {@code generators.doNotPrune} configuration — Copilot R1 #385 — same
     * generator classes and order). The harness-side generator-error capture and
     * the standalone function-file emission are deliberately omitted: this helper
     * serves only the four POJO byte locks, whose renders are byte-verified
     * against golden below.
     */
    private static Map<String, String> generateCell(D11CorpusRegressionTest.CellSpec cell)
            throws IOException {
        var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(),
                D11CorpusRegressionTest.emissionFilter(cell),
                D11CorpusRegressionTest.readDoNotPrune(cell));
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

    // ==== byte locks (all 6 flips through the REAL D11 routes) ====

    /** listAddSetImplFieldName: the model-typed list attribute carrier. */
    @Test
    @EnabledIf("drrCellAvailable")
    void annaDsbDerived_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrPojoOutput, DRR_GOLDEN_DIR, ANNA_DERIVED);
    }

    /** listAddSetImplFieldName: the BASIC (String) list attribute twin. */
    @Test
    @EnabledIf("drrCellAvailable")
    void annaDsbUnderlyingRecord_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrPojoOutput, DRR_GOLDEN_DIR, ANNA_RECORD);
    }

    /** nestedCondArmLadderDecomp: the fn-arg extract ladder + receiver-wrap carrier. */
    @Test
    @EnabledIf("drrCellAvailable")
    void direction2Leg1Rule_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrPojoOutput, DRR_GOLDEN_DIR, DIRECTION2_LEG1);
    }

    /** nestedCondArmLadderDecomp: the nested-conditional-arm carrier. */
    @Test
    @EnabledIf("drrCellAvailable")
    void direction2Leg2Rule_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrPojoOutput, DRR_GOLDEN_DIR, DIRECTION2_LEG2);
    }

    /** extractLadderConfinedArmDecomp: the confined-arm alias-chain carrier, Leg1. */
    @Test
    @EnabledIf("drrCellAvailable")
    void quantityFrequencyLeg1_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFnOutput, DRR_GOLDEN_DIR, QUANTITY_FREQUENCY_LEG1);
    }

    /** extractLadderConfinedArmDecomp: the Leg2 twin. */
    @Test
    @EnabledIf("drrCellAvailable")
    void quantityFrequencyLeg2_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFnOutput, DRR_GOLDEN_DIR, QUANTITY_FREQUENCY_LEG2);
    }

    // ==== occurrence-counted witnesses (python str.count semantics — the #352 law) ====

    /**
     * listAddSetImplFieldName: the decap'd singular param + this-refs (PRE 0 /
     * golden 5), the decap'd plural list param (PRE 0 / golden 2 per file), the
     * decap'd field ref in the add loop (PRE 0 / golden 2); negatives — the
     * raw-name singular param/this-refs {@code _UnderlyingRecord} (PRE 5 / golden 0)
     * and {@code _ReturnUnderlierID} (PRE 5 / golden 0).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void annaDsb_builderImplNaming_witness() {
        String der = drrPojoOutput.get(ANNA_DERIVED);
        String rec = drrPojoOutput.get(ANNA_RECORD);
        assertNotNull(der, "AnnaDsbDerived not generated");
        assertNotNull(rec, "AnnaDsbUnderlyingRecord not generated");
        assertEquals(5, count(der, "_underlyingRecord"),
                "the decap'd singular param + refs land (PRE 0 / golden 5)");
        assertEquals(2, count(der, "(List<? extends AnnaDsbUnderlyingRecord> underlyingRecords)"),
                "the decap'd plural list param lands (PRE 0 / golden 2)");
        assertEquals(2, count(der, "this.underlyingRecord.add("),
                "the impl bodies reference the decap'd FIELD (PRE 0 / golden 2)");
        assertEquals(0, count(der, "_UnderlyingRecord"),
                "the raw-name singular param must be gone (PRE 5 / golden 0)");
        assertEquals(2, count(rec, "(List<String> returnUnderlierIDs)"),
                "the BASIC-list plural param decaps too (PRE 0 / golden 2)");
        assertEquals(0, count(rec, "_ReturnUnderlierID"),
                "the raw-name param must be gone in the basic twin (PRE 5 / golden 0)");
    }

    /**
     * extractLadderConfinedArmDecomp (Leg1): the hoisted invocation base — one per
     * alias method (PRE 0 / golden 2), the per-lambda boolean hoist (PRE 0 /
     * golden 2), the in-arm session-escaped {@code _thenArg} (PRE 0 / golden 1), the
     * typed ofNull tails (PRE 0 / golden 1 each), the elementwise BARE deref off the
     * filter tail (PRE 0 / golden 1); negatives — the runtime {@code .then(} form
     * (PRE 2 / golden 0) and the inline ternary (PRE 14 / golden 0).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void quantityFrequencyLeg1_ladderDecomp_witness() {
        String gen = drrFnOutput.get(QUANTITY_FREQUENCY_LEG1);
        assertNotNull(gen, "QuantityFrequencyLeg1 not generated");
        assertEquals(2, count(gen,
                "final MapperS<Trade> thenArg = MapperS.of(tradeForEvent.evaluate(reportableEvent));"),
                "the invocation base hoists per alias method (PRE 0 / golden 2)");
        assertEquals(2, count(gen, "final Boolean boolean0 = qualify_Commodity_Swap_Basis.evaluate("),
                "the per-lambda boolean hoists land, numbering restarting (PRE 0 / golden 2)");
        assertEquals(1, count(gen, "final MapperC<PriceQuantity> _thenArg = item.<TradableProduct>map"),
                "the in-arm hoist escapes the alias session's bare thenArg (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "return MapperS.<PriceQuantity>ofNull();"),
                "the quantity ladder takes the typed ofNull tail (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "return MapperS.<Frequency>ofNull();"),
                "the frequency ladder takes the typed ofNull tail (PRE 0 / golden 1)");
        assertEquals(1, count(gen, ".<NonNegativeQuantitySchedule>map(\"Type coercion\", "
                        + "fieldWithMetaNonNegativeQuantitySchedule -> "
                        + "fieldWithMetaNonNegativeQuantitySchedule.getValue()).<Frequency>map"),
                "the filter tail derefs elementwise, BARE form (PRE 0 / golden 1)");
        assertEquals(0, count(gen, ".then(item -> item"),
                "the runtime .then( form must be gone (PRE 2 / golden 0)");
        assertEquals(0, count(gen, "getOrDefault(false) ?"),
                "the inline ternary must be gone (PRE 14 / golden 0)");
    }

    /**
     * nestedCondArmLadderDecomp (Leg1): the MapperS.of wrap on the fn-arg extract's
     * invokable receiver (PRE 0 / golden 1), the block-ladder boolean hoist (PRE 0 /
     * golden 1), the typed ofNull tail (PRE 0 / golden 1); negative — the inline
     * ternary arm (PRE 1 / golden 0).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void direction2Leg1_fnArgLadder_witness() {
        String gen = drrPojoOutput.get(DIRECTION2_LEG1);
        assertNotNull(gen, "Direction2Leg1Rule not generated");
        assertEquals(1, count(gen, "MapperS.of(direction2.evaluate(item.get(), "
                        + "MapperS.of(tradeForEvent.evaluate(item.get()))"),
                "the single-invocation extract receiver wraps MapperS.of (PRE 0 / golden 1)");
        assertEquals(1, count(gen,
                "final Boolean boolean0 = isCommoditySwapFloatFloat.evaluate(_item.get());"),
                "the fn-arg block ladder hoists its bare-invokable condition (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "return MapperS.<PayerReceiver>ofNull();"),
                "the ladder takes the typed ofNull tail (PRE 0 / golden 1)");
        assertEquals(0, count(gen, "getOrDefault(false) ? MapperS.of(commodityLeg1.evaluate(_item.get()))"),
                "the inline ternary arm must be gone (PRE 1 / golden 0)");
    }

    /**
     * nestedCondArmLadderDecomp (Leg2): the in-lambda thenArg hoist of the runtime
     * then receiver (PRE 0 / golden 1) and the NESTED arm's ofNull — one per nesting
     * level (PRE 0 / golden 2); negative — the runtime {@code .then(_item ->} form
     * (PRE 1 / golden 0).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void direction2Leg2_nestedCondArm_witness() {
        String gen = drrPojoOutput.get(DIRECTION2_LEG2);
        assertNotNull(gen, "Direction2Leg2Rule not generated");
        assertEquals(1, count(gen, "final MapperS<Product> thenArg = "
                        + "MapperS.of(tradeForEvent.evaluate(item.get())).<TradableProduct>map"),
                "the runtime-then receiver hoists as the in-lambda thenArg (PRE 0 / golden 1)");
        assertEquals(2, count(gen, "return MapperS.<PayerReceiver>ofNull();"),
                "both nesting levels take the typed ofNull tail (PRE 0 / golden 2)");
        assertEquals(0, count(gen, ".then(_item -> _item"),
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
