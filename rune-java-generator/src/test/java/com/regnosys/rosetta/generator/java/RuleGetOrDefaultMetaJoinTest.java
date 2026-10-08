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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static com.regnosys.rosetta.testutil.GenerationErrors.assertNoGenerationErrors;

/**
 * PR #296 — facet getOrDefaultMetaJoin: ONE green-safe GENERATOR mechanism, 1 drr POJO Rule byte
 * flip (the #295 conditional-meta-type-join at the {@code default} seat — the #295-deferred
 * getOrDefault follow-on).
 *
 * <p><b>The mechanism.</b> A MIXED-baresym {@code default}: the LEFT (defaulted) operand is a
 * disguised baresym function-nav
 * ({@code func -> feature}, an {@link com.regnosys.rosetta.ast.expressions.references.REnumValueRef}
 * whose {@code resolvedSymbol} is an {@code RFunction}) whose leaf is META-annotated, the RIGHT
 * (default) operand a baresym BARE-leaf nav (which forces the bare common type). #280 DECLINED the
 * meta-leaf LEFT, leaving the non-compiling bare type literal {@code MapperS.of(<Type>)}; golden
 * fires the baresym invoke + derefs the wrapper
 * {@code .<X>map("Type coercion", w -> w == null ? null : w.getValue())} so the operand joins on
 * the bare value.
 *
 * <p><b>Where it fires.</b> {@code SetOperationHandler.handle(RDefaultExpr)} pre-scans the operands
 * ({@code isMixedBaresymDefault} via {@code ReferenceHandler.baresymFunctionLeafMetaKind} — the SAME
 * leaf resolution as the #280 gate). When mixed it (a) pushes the #295 scope flag
 * ({@code JavaStatementScope.pushMetaLeafBaresymAllowed}) WHILE compiling the LEFT so the wrapper
 * fires, and (b) derefs it via {@code ExpressionCompiler.coerceNavigationReceiver}. The {@code thenArg}
 * DECLARATION follows via the matching #144-gate extension in {@code FunctionExpressionRenderer}
 * (skip the meta-wrapper recovery when the rendered block carries the
 * {@code .getValue()).getOrDefault(} marker).
 *
 * <p><b>Green-safety (regscan: 1 flipped-out / 0 within-waiver regressions / 0 churn).</b> Confined
 * to the mixed-baresym {@code default} LEFT-operand seat — a meta-leaf baresym in a PLAIN nav
 * (CollateralPortfolioCode), a non-baresym / literal-default getOrDefault, and the conditional-ladder
 * seat (#295) ALL keep the existing rendering. The pre-fix bare type literal never compiled, so only
 * an already-waivered file is touched. byte-oracle / stash-baseline measured exactly 1 flip (drr POJO
 * 423 → 422; clean source = 12 stale waivers, {@code comm -23} = exactly CollateralPortfolioIndicator);
 * ALL FUNCTION cells (cdm5 81 / cdm6 236 / drr 207) + cdm/iso/fpml POJO byte-IDENTICAL.
 *
 * <p>Anchors are WHOLE-FILE byte comparisons of the generated drr Rule output against the frozen
 * goldens (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}. REVERT-VERIFIED RED for the flip lock.
 */
class RuleGetOrDefaultMetaJoinTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static Map<String, String> drrOutput;

    static boolean drrCellAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (drrCellAvailable()) {
            drrOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT));
        }
    }

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
        funcGen.generate(output);
        return output;
    }

    // ==== Flip lock (revert-RED): the mixed-baresym getOrDefault meta-join carrier byte-matches
    //      golden. ====

    /**
     * Flip: a MIXED-baresym {@code default}. {@code CollateralPortfolioIndicator}'s body is
     * {@code (positionForEvent -> collateral) default (tradeForEvent -> collateral)}. The LEFT
     * operand navigates {@code CounterpartyPosition.collateral}
     * ({@code ReferenceWithMetaCollateral}, meta); the RIGHT navigates {@code Trade.collateral}
     * (bare {@code Collateral}) — so the default is mixed-baresym. The LEFT fires its baresym invoke
     * + is deref'd
     * {@code .<Collateral>map("Type coercion", referenceWithMetaCollateral -> … getValue())}, and
     * the {@code thenArg} declares the bare {@code MapperS<Collateral>} (NOT the wrapper, via the
     * extended #144 gate).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void collateralPortfolioIndicator_mixedBaresymDefault_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/standards/iosco/cde/version1/collateral/reports/CollateralPortfolioIndicatorRule.java");
    }

    // ==== Green-safety / gate locks: the mechanism does NOT over-fire outside the mixed-baresym
    //      default LEFT-operand seat. ====

    /**
     * FLIPPED at PR #371 (facet midChainCallableMetaDeref) — the plain-nav follow-on the original
     * lock anticipated LANDED. History: this lock asserted {@code CollateralPortfolioCode}'s
     * plain-nav meta baresym ({@code positionForEvent -> collateral -> …}) kept the #280-declined
     * bare type literal {@code MapperS.of(PositionForEvent)}, proving the #296 getOrDefault meta-join
     * stayed confined to the default LEFT-operand seat. PR #371 admits the MID-CHAIN meta leaf at the
     * bare-FUNCTION nav synthesis itself (the EVR is the receiver of a further hop; the continuation
     * hop's receiver coercion derefs inline), so the carrier is now byte-golden. The #296 confinement
     * this lock guarded still holds — the getOrDefault scope flag is untouched; the flip came through
     * the SEPARATE #359-sibling admission — so the lock now pins the FIRED render + a byte-match.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void collateralPortfolioCode_midChainMetaLeafBaresym_fired() throws IOException {
        assertNotNull(drrOutput, "drr generation did not run — corpus unavailable?");
        String gen = drrOutput.get(
                "drr/standards/iosco/cde/version1/collateral/reports/CollateralPortfolioCodeRule.java");
        assertNotNull(gen, "CollateralPortfolioCodeRule not generated");
        assertFalse(gen.contains("MapperS.of(PositionForEvent)"),
                "the #280-declined bare type literal must be GONE — the PR #371 mid-chain "
                + "callable-meta admission fires the synthesis (FLIPPED-at-#371)");
        assertByteMatchesGolden(
                "drr/standards/iosco/cde/version1/collateral/reports/CollateralPortfolioCodeRule.java");
    }

    /**
     * Green-safety lock — the dominant {@code getOrDefault(false)} boolean-default path is untouched.
     * {@code UpiPreEnrichmentData} is a GREEN drr Rule carrying {@code getOrDefault(false)} (a literal
     * default, RIGHT operand non-baresym, so {@code isMixedBaresymDefault} declines): it must STILL
     * byte-match golden after the meta-join change.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void upiPreEnrichmentData_literalDefault_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/enrichment/common/reports/UpiPreEnrichmentDataRule.java");
    }

    private static void assertByteMatchesGolden(String path) throws IOException {
        assertNotNull(drrOutput, "drr generation did not run — corpus unavailable?");
        String generated = drrOutput.get(path);
        assertNotNull(generated, "Class not generated: " + path
                + " (emission failed or the path differs)");
        Path goldenPath = DRR_GOLDEN_DIR.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated drr output must byte-match the golden (newline-normalized) for "
                + path + " (PR #296 getOrDefaultMetaJoin).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
