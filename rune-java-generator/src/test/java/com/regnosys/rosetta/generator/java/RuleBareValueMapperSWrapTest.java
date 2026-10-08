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
 * PR #256 — facet {@code bareValueMapperSWrap} (rule-body emission, M7b-3).
 *
 * <p>A bare VALUE consumed at a Mapper-expecting seat is wrapped {@code MapperS.of(...)} by golden
 * but emitted BARE by the fork (so it does not compile and the carrier is already waivered). The
 * bare values come from {@link com.regnosys.rosetta.generator.java.expression.handlers.ReferenceHandler}
 * rendering a no-args FUNCTION invocation UNWRAPPED ({@code <fn>.evaluate(<arg>)}) and a bare enum
 * constant UNWRAPPED ({@code Enum.VALUE}); the seats are an {@code exists}/{@code notExists}
 * argument, a {@code MapperS<…>}-typed {@code ifThenElseResult} arm, and a {@code MapperS}-returning
 * map-lambda {@code return}. A bare RULE invocation already wraps
 * ({@code renderImplicitRuleInvocation} returns {@code wrappedInMapperSOf}), so the fix is its
 * bare-FUNCTION / bare-enum analogue applied per-seat.
 *
 * <p><b>Fix (GENERATOR-only, 5 source files).</b>
 * <ul>
 *   <li>{@code HandlerHelper.wrapBareInvocationOperand} — the single-source-of-truth bare-no-args
 *       FUNCTION wrap (RFunction-only; NOT RRule, which already arrives wrapped);
 *       {@code ComparisonHandler.wrapBareFunctionOperand} now delegates to it.</li>
 *   <li>{@code ExistenceHandler.handle(RExistenceExpr)} — applies that wrap to the existence
 *       argument (the exists/notExists carriers).</li>
 *   <li>{@code FunctionExpressionRenderer.appendThenConditionalBlock} — wraps a bare-enum
 *       ({@code wrapEnumRungInMapperSOf}) or bare-function ({@code wrapBareInvocationOperand}) arm;
 *       this ladder declares the local UNCONDITIONALLY {@code MapperS<…>}-typed, which is the
 *       discriminator, so it is DISTINCT from the item-typed {@code <T> ifThenElseResult = null;}
 *       ladder ({@code ControlFlowHandler.hoistAsItemLocalOrNull}, the #181 form) which must stay
 *       bare.</li>
 *   <li>{@code CollectionHandler.compileElselessConditionalBlock} /
 *       {@code compileEffectiveElseConditionalBlock} — wrap a bare-enum / bare-function lambda
 *       {@code return} arm.</li>
 * </ul>
 *
 * <p><b>Green-safe by construction</b> (corpus-verified, frozen 9.83.0 baseline): every gate keys on
 * the raw-operand AST node KIND (a no-args {@code RFunction} reference / a bare enum reference) at a
 * Mapper-expecting seat where golden NEVER leaves the value bare (0 bare / 532 wrapped exists, 0 bare
 * arms in the 124 {@code MapperS<…> ifThenElseResult} goldens), so the rewrite only ever touches
 * currently-waivered (non-compiling) output and never double-wraps the legitimate nested
 * {@code MapperS.of(MapperS.of(...))} goldens.
 *
 * <p>The byte-oracle + stash-baseline confirm 13 drr POJO Rule files flip (the byte-oracle's 25
 * now-matching include 12 pre-existing stale rule-family waivers that matched clean main, left
 * waivered per #231); the full all-kinds D11 stays 20/20 with zero regression.
 *
 * <p>Anchors are WHOLE-FILE byte comparisons of the generated drr POJO output (Rule kind) against
 * the frozen goldens (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}. REVERT-VERIFIED RED.
 */
class RuleBareValueMapperSWrapTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR = DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static Map<String, String> drrPojoOutput;

    static boolean drrCellAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR);
    }

    @BeforeAll
    static void generatePojo() throws IOException {
        if (drrCellAvailable()) {
            drrPojoOutput = generatePojoCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT));
        }
    }

    /**
     * Generate the drr POJO cell (Rule/Report/LabelProvider included), mirroring
     * {@link D11CorpusRegressionTest#pojo_comparison}'s generator wiring — the rule-body Rule
     * classes this facet touches are emitted by {@link RuleGenerator} (which delegates to
     * {@link FunctionGenerator}, the shared expression compiler).
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
                // Rule-family generators are still converging on body emission (M7b-3); a failure
                // for an already-waivered element is tolerated debt. The anchored carriers below
                // MUST emit + byte-match, so a swallowed failure surfaces as a null lookup in
                // assertByteMatchesGolden.
                assertNoGenerationErrors(ruleGen.generateClasses(model, version, output));
                assertNoGenerationErrors(reportGen.generateClasses(model, version, output));
                assertNoGenerationErrors(labelProviderGen.generateClasses(model, version, output));
            }
        }
        return output;
    }

    // ---- Flip locks (revert-RED), one per seat × region.

    /** exists-arg bare-FUNCTION (edit B): {@code exists(MapperS.of(tradeForEvent.evaluate(thenArg.get())))}. */
    @Test
    @EnabledIf("drrCellAvailable")
    void ptrrRuleEsma_existsArgWrap_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/esma/emir/refit/trade/reports/PTRRRule.java");
    }

    /** exists-arg bare-FUNCTION, cross-region (edit B): hkma. */
    @Test
    @EnabledIf("drrCellAvailable")
    void productDescriptionRuleHkma_existsArgWrap_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/hkma/rewrite/trade/reports/ProductDescriptionRule.java");
    }

    /** exists-arg bare-FUNCTION inside a mapSingleToItem .asMapper() body (edit B): common. */
    @Test
    @EnabledIf("drrCellAvailable")
    void packageIndicatorRuleCommon_existsAsMapperWrap_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/common/trade/link/reports/PackageIndicatorRule.java");
    }

    /** ifThenElseResult bare-ENUM arm (edit C): {@code ifThenElseResult = MapperS.of(EventTypeEnum.TRAD)}. */
    @Test
    @EnabledIf("drrCellAvailable")
    void eventTypeRuleCftc_ifThenElseResultEnumWrap_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/cftc/rewrite/trade/reports/EventTypeRule.java");
    }

    /** exists-arg + ifThenElseResult bare-FUNCTION arm (edits B+C): jfsa. */
    @Test
    @EnabledIf("drrCellAvailable")
    void underlyingIdentificationTypeRuleJfsa_existsAndIteFnWrap_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/jfsa/rewrite/trade/reports/UnderlyingIdentificationTypeRule.java");
    }

    /** elseless-conditional block-lambda bare-FUNCTION return (edit D1): {@code return MapperS.of(extractReferenceEntity.evaluate(item.get()))}. */
    @Test
    @EnabledIf("drrCellAvailable")
    void referenceEntityRuleEsma_elselessBlockFnReturnWrap_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/esma/emir/refit/trade/reports/ReferenceEntityRule.java");
    }

    /** effective-else block-lambda bare-ENUM return (edit D2): {@code return MapperS.of(ClearedEnum.N)}. */
    @Test
    @EnabledIf("drrCellAvailable")
    void clearedRuleEsma_effectiveElseBlockEnumReturnWrap_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/esma/emir/refit/trade/reports/ClearedRule.java");
    }

    /** notExists-arg + map-lambda return bare-FUNCTION (edits B+D): esma. */
    @Test
    @EnabledIf("drrCellAvailable")
    void reportTrackingNumberRuleEsma_notExistsAndReturnWrap_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/esma/emir/refit/trade/reports/ReportTrackingNumberRule.java");
    }

    // ---- Green-safety lock: an already-correctly-wrapped seat must stay byte-identical.

    /**
     * Green-safety lock: {@code DTCC_OriginalSwapSDRIDTypeRule} (cftc) is a GREEN drr Rule that
     * exercises TWO touched seats correctly and must stay byte-identical:
     * <ol>
     *   <li>{@code exists(MapperS.of(originalSwapSDRIdentifierRule.evaluate(item.get())))} — the
     *       exists operand is a bare RULE invocation, ALREADY wrapped by
     *       {@code renderImplicitRuleInvocation}. The new {@code wrapBareInvocationOperand} gate is
     *       RFunction-ONLY, so it must NOT fire here — if it wrongly broadened to {@code RRule} it
     *       would double-wrap {@code exists(MapperS.of(MapperS.of(...)))} and this file would
     *       diverge.</li>
     *   <li>{@code filterSingleNullSafe(item -> isAllowableActionForCFTC.evaluate(item.get()))} — a
     *       bare-FUNCTION invocation at a FILTER_PREDICATE (Boolean-expecting) position, which is
     *       correctly UNWRAPPED. The exists/conditional/return wraps must NOT touch the
     *       filter-predicate seat.</li>
     * </ol>
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void dtccOriginalSwapSDRIDTypeRuleCftc_bareRuleAndFilterPredicate_staysByteIdentical()
            throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/cftc/rewrite/dtcc/reports/DTCC_OriginalSwapSDRIDTypeRule.java");
    }

    private static void assertByteMatchesGolden(String path) throws IOException {
        assertNotNull(drrPojoOutput, "drr POJO generation did not run — corpus unavailable?");
        String generated = drrPojoOutput.get(path);
        assertNotNull(generated, "Rule class not generated: " + path
                + " (RuleGenerator emission failed or the path differs)");
        Path goldenPath = DRR_GOLDEN_DIR.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated drr Rule output must byte-match the golden (newline-normalized) for "
                + path + " — a bare value consumed at a Mapper-expecting seat (exists/notExists arg, "
                + "MapperS<…> ifThenElseResult arm, MapperS-returning lambda return) must be wrapped "
                + "MapperS.of(...) (facet bareValueMapperSWrap, PR #256).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
