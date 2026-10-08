package com.regnosys.rosetta.generator.java;

import com.regnosys.rosetta.ast.model.RModel;
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

/**
 * PR #398 — the effective-else-ctor-meta / elseless-wrapper-join / together-restructure
 * pair (2 byte flips: DTCC_UnderlyingAssetReportRule [drr POJO — the LAST drr POJO; the
 * cell EMPTIES and the CODEGEN_BODY_GAP bucket ELIMINATES] + NotionalLeg [drr FUNCTION —
 * the together-restructure of all twelve aliases in ONE all-or-nothing verdict]).
 *
 * <p><b>effElseCtorSetterMetaDeref</b> (CollectionHandler + ConstructionHandler): the
 * effective-else conditional block registers the #354 {@code blockArmSeatConditionals}
 * channel around each ARM compile (the #360 elseless registration's sibling), admits the
 * marker-classed {@code CtorNavMetaDerefHoist} in its arm-hoist gates, and wraps bare
 * CONSTRUCTOR arms {@code MapperS.of(<ctor>)} including the multi-line chain form. The
 * hoist decl renders through its DEFERRED SENTINEL (the closed-scope law — materialising
 * mid-compile poisoned the #397 cascade's later thenArg mints), and a TYPE-LESS bare
 * implicit-item setter value recovers its wrapper through the #362
 * {@code bareItemThenPipeMetaType} binding channel (the consumer's block lambda).
 *
 * <p><b>elselessLadderWrapperJoin</b> (ControlFlowHandler): the #381 wrapper-preserving
 * join extends to the ELSELESS DeepThenCondArg ladder (with the #354
 * {@code recoverExprMetaWrapper} recovery for a type-less arm) — the ladder decl AND the
 * typed-ofNull terminal keep the WRAPPER ({@code MapperC<FieldWithMetaString>}), the #364
 * deref no-ops, and the deref moves to the re-rooted consumer (the #397 jfsa law).
 *
 * <p><b>fnNotionalTogetherRestructure</b> (CollectionHandler + ControlFlowHandler +
 * NavigationHandler + ReferenceHandler + FunctionExpressionRenderer): the FUNCTION-path
 * ctl-scan arms (extract-conditional consumers / ctor conditional-LADDER fields / the
 * nested-then conditional base) make every NotionalLeg chain read HANDLED so the
 * function-level all-or-nothing verdict passes and golden's whole-function restructure
 * lands in one pass: the collapse-witness MULTI overlay (a {@code then
 * only-element/first/last} first body PROVES the list pipe — the cp6 catch healed three
 * green cond-base carriers an arm-evidence read had flipped), the per-arm RType
 * extends-fold with the {@code ? extends MeasureBase} WILDCARD decl + bare-witness
 * ofNull, the LUB-join {@code <MeasureBase>} JOIN-witness deref, ctor arms wrapping
 * {@code MapperC.of(Collections.singletonList(…))}, the nested-then RUNG's inner
 * if/else, the statement-route EvalArg in-branch relocation, the
 * {@code EvalArgMetaDerefHoist} sentinel render, the DISGUISED 2-name ctor-field typing
 * (the #371 law at the #355 seat), the INT-literal {@code BigDecimal.valueOf} coercion
 * (sibling-arm proof + leaf-attr fallback), the MISSING-pipe LUB fallback re-rooting the
 * consumer's bare {@code unit}/{@code value}, and the #317 collapsed-meta-deref's
 * FUNCTION-path lambda channel (the {@code ReferenceWithMetaPriceSchedule} hoist + the
 * Mapper-guard operand).
 *
 * <p>2 whole-file byte locks through the REAL D11 routes (drr POJO + drr FUNCTION —
 * co-locked at the graduated RuleIteHoistTest / FunctionTailCardinalityMetaTest pins) +
 * 12 occurrence-counted witness tokens PRE-counted against f-probe-397post, ALL PRE 0 /
 * GOLD ≥ 1 (witness398.py; the #352 str.count law).
 */
class EffElseWrapperJoinTogetherRestructureComposeTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static final String DTCC_UAR =
            "drr/regulation/csa/rewrite/dtcc/reports/DTCC_UnderlyingAssetReportRule.java";
    private static final String NOTIONAL_LEG =
            "drr/standards/iosco/cde/base/quantity/functions/NotionalLeg.java";

    private static Map<String, String> drrFnOutput;
    private static Map<String, String> drrPojoOutput;

    static boolean cellsAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (cellsAvailable()) {
            drrFnOutput = generateFunctions(
                    new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT));
            drrPojoOutput = generatePojoCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT));
        }
    }

    private static Map<String, String> generateFunctions(D11CorpusRegressionTest.CellSpec cell)
            throws IOException {
        var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(),
                D11CorpusRegressionTest.emissionFilter(cell));
        var typeUtil = new JavaTypeUtil();
        var funcGen = new FunctionGenerator(gm, new JavaTypeTranslator(typeUtil), typeUtil);
        Map<String, String> output = new LinkedHashMap<>();
        var errors = funcGen.generateWithErrors(output);
        if (!errors.isEmpty()) {
            System.err.println("[" + cell.corpus() + "-" + cell.version()
                    + " FUNCTION] generation errors tolerated (D11 parity): " + errors.size());
        }
        return output;
    }

    private static Map<String, String> generatePojoCell(D11CorpusRegressionTest.CellSpec cell)
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
        int generationErrors = 0;
        for (RModel model : corpus.workspace().files()) {
            if (gm.shouldGenerate(model)) {
                String version = gm.version(model);
                generationErrors += pojoGen.generateClasses(model, version, output).size();
                generationErrors += choiceGen.generateClasses(model, version, output).size();
                generationErrors += ruleGen.generateClasses(model, version, output).size();
                generationErrors += reportGen.generateClasses(model, version, output).size();
                generationErrors += labelProviderGen.generateClasses(model, version, output).size();
            }
        }
        if (generationErrors > 0) {
            System.err.println("[" + cell.corpus() + "-" + cell.version()
                    + " POJO] generation errors tolerated (D11 parity): " + generationErrors);
        }
        return output;
    }

    // ==== byte locks (one carrier per flip through the REAL D11 routes) ====

    /** the effective-else ctor-meta + elseless-wrapper-join carrier (the LAST drr POJO). */
    @Test
    @EnabledIf("cellsAvailable")
    void dtccUnderlyingAssetReport_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrPojoOutput, DRR_GOLDEN_DIR, DTCC_UAR);
    }

    /** the whole-function together-restructure carrier (all twelve aliases in one verdict). */
    @Test
    @EnabledIf("cellsAvailable")
    void notionalLeg_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFnOutput, DRR_GOLDEN_DIR, NOTIONAL_LEG);
    }

    // ==== occurrence-counted witnesses (PRE 0 / GOLD >= 1 — witness398.py) ====

    /** effElseCtorSetterMetaDeref: the numbered arm hoists + the guarded deref + the consumer. */
    @Test
    @EnabledIf("cellsAvailable")
    void dtcc_effElseArmHoists_witness() {
        String gen = gen(drrPojoOutput, DTCC_UAR);
        assertEquals(1, count(gen,
                "final FieldWithMetaString fieldWithMetaString0 = _item.<FieldWithMetaString>"
                        + "map(\"getIdentifier\""),
                "the FIRST inner-mapItem arm's numbered wrapper hoist (PRE 0 / GOLD 1)");
        assertEquals(1, count(gen,
                "(fieldWithMetaString0 == null ? null : fieldWithMetaString0.getValue())"),
                "the guarded .getValue() deref consuming the arm hoist (PRE 0 / GOLD 1)");
        assertEquals(1, count(gen, "final FieldWithMetaString fieldWithMetaString = _item.get();"),
                "the final consumer's UNNUMBERED block-lambda hoist (PRE 0 / GOLD 1)");
    }

    /** elselessLadderWrapperJoin: the wrapper-typed ladder decl + the wrapper ofNull terminal. */
    @Test
    @EnabledIf("cellsAvailable")
    void dtcc_wrapperJoinLadder_witness() {
        String gen = gen(drrPojoOutput, DTCC_UAR);
        assertEquals(1, count(gen, "final MapperC<FieldWithMetaString> ifThenElseResult;"),
                "the elseless ladder keeps the WRAPPER element (PRE 0 / GOLD 1 — the pre "
                        + "state carried MapperC<String>)");
        assertEquals(1, count(gen, "ifThenElseResult = MapperC.<FieldWithMetaString>ofNull();"),
                "the wrapper-typed ofNull terminal (PRE 0 / GOLD 1)");
    }

    /** the cond-base ladder: wildcard LUB decl + singletonList ctor arms + the JOIN-witness deref. */
    @Test
    @EnabledIf("cellsAvailable")
    void notionalLeg_lubLadder_witness() {
        String gen = gen(drrFnOutput, NOTIONAL_LEG);
        assertEquals(2, count(gen, "final MapperC<? extends MeasureBase> thenArg0;"),
                "the `? extends LUB` wildcard decl at both cond-base ladders (PRE 0 / GOLD 2)");
        assertEquals(4, count(gen,
                "thenArg0 = MapperC.of(Collections.singletonList(Measure.builder()"),
                "the MULTI-seat ctor singletonList arms (PRE 0 / GOLD 4)");
        assertEquals(2, count(gen,
                ".<MeasureBase>map(\"Type coercion\", fieldWithMetaNonNegativeQuantitySchedule"
                        + " -> fieldWithMetaNonNegativeQuantitySchedule.getValue())"),
                "the LUB-join JOIN-witness deref on the chain arms (PRE 0 / GOLD 2)");
    }

    /** the in-lambda residuals: the literal coercion + the ladder field + the collapse guard. */
    @Test
    @EnabledIf("cellsAvailable")
    void notionalLeg_inLambdaResiduals_witness() {
        String gen = gen(drrFnOutput, NOTIONAL_LEG);
        assertEquals(3, count(gen, "return MapperS.of(BigDecimal.valueOf(1));"),
                "the INT-literal BigDecimal.valueOf coercion at the BigDecimal-joined "
                        + "return seats (PRE 0 / GOLD 3)");
        assertEquals(1, count(gen, "final UnitType ifThenElseResult0;"),
                "the ctor-field else-if LADDER hoist — the disguised 2-name typing "
                        + "(PRE 0 / GOLD 1)");
        assertEquals(1, count(gen,
                "final ReferenceWithMetaPriceSchedule referenceWithMetaPriceSchedule = "
                        + "item.<ReferenceWithMetaPriceSchedule>mapC(\"getPriceSchedule\""),
                "the #317 FUNCTION-path collapse hoist (PRE 0 / GOLD 1)");
        assertEquals(1, count(gen,
                "(referenceWithMetaPriceSchedule == null ? MapperS.<PriceSchedule>ofNull() : "
                        + "MapperS.of(referenceWithMetaPriceSchedule.getValue()))"),
                "the Mapper-guard wrapper deref operand (PRE 0 / GOLD 1)");
    }

    private static int count(String s, String token) {
        int n = 0;
        for (int i = s.indexOf(token); i >= 0; i = s.indexOf(token, i + token.length())) {
            n++;
        }
        return n;
    }

    private static String gen(Map<String, String> output, String path) {
        assertNotNull(output, "generation did not run — corpus unavailable?");
        String gen = output.get(path);
        assertNotNull(gen, "missing generated file: " + path);
        return gen;
    }

    private static void assertByteMatchesGolden(Map<String, String> output, Path goldenDir,
            String path) throws IOException {
        String generated = gen(output, path);
        Path goldenPath = goldenDir.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(golden.replace("\r\n", "\n"), generated.replace("\r\n", "\n"),
                "byte-identity REGRESSED: " + path);
    }
}
