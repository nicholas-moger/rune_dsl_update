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
 * PR #292 — the clean finisher compose: TWO disjoint green-safe GENERATOR mechanisms, 2 drr POJO
 * Rule byte flips. The byte-clean drr-POJO tail is DRAINED (the user-chosen compose; the originally
 * targeted QuantityUnitOfMeasureLeg2 csa + SmallScaleBuySide were REFUTED by the byte-oracle and
 * deferred — see the decline lock below).
 *
 * <p><b>(A) lambdaParamItemEscape</b> ({@code HandlerHelper.escapedImplicitItemName} +
 * {@code CollectionHandler.resolveParamName} + {@code ReferenceHandler.handle(RImplicitVariable)}).
 * A nested implicit-item lambda escapes its {@code item} param to {@code _item} (… per nesting
 * depth) to match golden's collision escape (upstream {@code JavaScope.createUniqueIdentifier};
 * {@code GeneratorScope.escapeName} returns {@code "_" + name}), instead of shadowing the enclosing
 * {@code item}. The depth counts enclosing OPERATION lambdas (an {@code extract}/{@code filter}/
 * {@code map} body) but EXCLUDES {@code then}-chain step lambdas (an {@code RInlineFunction} whose
 * parent is an {@code RThenExpr} — merged with their operation lambda + hoisted to a separate
 * {@code thenArg} statement, so their {@code item} never collides). Green-safe by construction: ZERO
 * of the 34,686 9.83.0 goldens carry a nested un-escaped {@code item -> … item ->} shadow (289 carry
 * the escaped {@code _item} form, max depth 1). Carrier: DTCC_ProductGrade common (the #291 residual
 * — cardinality now correct, the inner {@code delivery -> commodityGrade extract to-string}'s lambda
 * escapes {@code item -> _item} because the outer {@code .mapItem(item -> ProductGradeReport{…})}
 * already bound {@code item}).
 *
 * <p><b>(B) lcuConditionalArmMeta</b> ({@code NavigationHandler.recoverMetaFromExpr}). An add-only
 * (MONOTONE) {@code RConditionalExpr} case descends an if-then-else body's arms — all arms join to
 * one output type, so the terminal meta is identical, and the walker previously returned null for a
 * conditional, so this only RECOVERS meta where there was none. Flips SwapLinkID (hkma) via the #265
 * lambda-seat value→meta-wrap: the delegated inner rule {@code common.link.SwapLinkID} body is
 * {@code if IsFXSwap(ProductForEvent) then cde.link.PackageIdentifier} (an elseless conditional), so
 * {@code recoverInnerRuleMetaWrapper} now recovers the inner rule's meta wrapper → golden wraps the
 * bare String result {@code MapperS.of(FieldWithMetaString.builder().setValue(string).build())}
 * inside the {@code .mapSingleToItem} block + derefs at output.
 *
 * <p>byte-oracle / stash-baseline measured exactly 2 flips (drr POJO 436 → 434; 14 now-matching = 2
 * mine + 12 stale, clean-source re-dump 3× = exactly the 12 stale, no DTCC/SwapLinkID); regscan 0
 * within-waiver regressions / 0 new mismatches / 2 flipped-out / 2 toward-golden + 84 neutral across
 * all 8 cells. (A) is a SHARED seat: the FUNCTION cells move toward-golden/neutral (13 byte-changed,
 * 0 regressions) — NOT byte-neutral, but green-safe (a green file cannot carry the shadow form).
 *
 * <p>Anchors are WHOLE-FILE byte comparisons of the generated drr output against the frozen goldens
 * (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}. REVERT-VERIFIED RED (the 2 flip locks fail
 * on clean source; the green-safety + decline locks pass either way).
 */
class RuleCleanFinisherComposeTest {

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

    // ==== Flip locks (revert-RED): the 2 carriers now byte-match golden. ====

    /**
     * Flip (A) — common DTCC_ProductGradeRule. The whole output is
     * {@code … then extract ProductGradeReport { productGrade: delivery -> commodityGrade extract
     * to-string } }. After #291 fixed the cardinality, the only residual was the lambda-param escape:
     * the inner {@code extract to-string} renders {@code .mapItem(_item -> _item.map("to-string",
     * ProductGradeEnum::toDisplayString))} ({@code _item}, escaped) because the outer
     * {@code .mapItem(item -> MapperS.of(ProductGradeReport.builder()…))} already bound {@code item};
     * the fork emitted the un-escaped {@code item} (shadow). REVERT-RED.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void dtccProductGrade_common_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/common/dtcc/reports/DTCC_ProductGradeRule.java");
    }

    /**
     * Flip (B) — hkma SwapLinkIDRule ({@code filter IsAllowableActionForHKMA then extract
     * common.link.SwapLinkID}). The delegated inner rule {@code common.link.SwapLinkID} body is
     * {@code if IsFXSwap(ProductForEvent) then cde.link.PackageIdentifier} (an elseless conditional);
     * the #265 lambda-seat wrap now recovers its meta via the new {@code recoverMetaFromExpr}
     * conditional-arm descent, so golden's {@code .mapSingleToItem(item -> { final String string = …;
     * return string == null ? MapperS.<FieldWithMetaString>ofNull() :
     * MapperS.of(FieldWithMetaString.builder().setValue(string).build()); }).get()} + output deref
     * fires. REVERT-RED.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void swapLinkID_hkma_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/hkma/rewrite/trade/reports/SwapLinkIDRule.java");
    }

    // ==== Green-safety / decline locks (pass on clean source AND with the fix). ====

    /**
     * Green-safety (A) — GetClassificationValueByOrdinal (drr function). A then-chain
     * {@code … filterItemNullSafe(item -> …) … then distinct then extract [item -> …]} whose filter
     * lambda and final extract lambda are SEPARATE then-steps (hoisted to separate {@code thenArg}
     * statements), so BOTH keep {@code item} — golden does NOT escape either. This file was the
     * canonical over-escape regression: an AST walk that counted {@code then}-chain step lambdas
     * (parent {@code RThenExpr}) wrongly escaped the extract's {@code item} to {@code _item}; the
     * then-lambda exclusion in {@code escapedImplicitItemName} keeps it {@code item}. Locks that the
     * escape fires ONLY on genuine same-statement nesting, never on then-chain siblings. Passes on
     * clean source too (a pure green-safety guard).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void getClassificationValueByOrdinal_thenChainItemStaysUnescaped() throws IOException {
        assertByteMatchesGolden(
                "drr/enrichment/upi/functions/GetClassificationValueByOrdinal.java");
    }

    // Decline lock (B) REMOVED at PR #336. This carrier — csa QuantityUnitOfMeasureLeg2Rule — was
    // DEFERRED here: the consumer `then extract QuantityUnitOfMeasure` passes `item.get()` (the
    // ReferenceWithMeta wrapper) into the sub-rule's bare-value param, and PR #292 left it divergent
    // because tryMetaDerefArg's null-compiled-type fallback was never reached and pushing past that
    // gate was judged to risk the green bare-item population. PR #336's ruleCalleeMetaDeref fires
    // WITHOUT that fallback — it explicitly types the piped item (enclosingThenArgType) and restores
    // the rule's from-type on the synthetic RFunction.fromRule callee, so the param-type gate resolves
    // and the deref fires. The #292 "risk" did NOT materialise (regscan 0 away / 0 byteChanged, D11
    // 20/20, full gensuite green). The flip is now locked by RuleCalleeMetaDerefTest.

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
                + path + " (PR #292 cleanFinisherCompose).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
