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
 * PR #290 — facet collapsedPipeMeta: the whole-output meta-deref for a then-chain whose LAST
 * then-body is a STANDALONE single-collapse list-op ({@code then last}/{@code first}/{@code
 * only-element}) over the rebound pipe. ONE green-safe, RULE-scoped (FUNCTION-byte-neutral)
 * GENERATOR mechanism, 2 drr POJO Rule byte flips. 2 source files
 * ({@code NavigationHandler} + {@code FunctionExpressionRenderer}).
 *
 * <p><b>The bug.</b> A reporting-rule whole-output {@code … then extract assignedIdentifier ->
 * identifier then last} (OriginalSwapUSI / UniqueSwapIdentifier, cftc rewrite trade) produces a
 * MULTI {@code MapperC<FieldWithMetaString>} thenArg (the {@code identifier} leaf is
 * {@code [metadata scheme]}), which the downstream {@code last} (or {@code only-element}) collapses
 * to a single {@code FieldWithMetaString}; golden hoists the wrapper + null-guards
 * {@code output = fieldWithMetaString.getValue()}, but the fork assigns the wrapper bare to the
 * {@code String} output (NON_COMPILING). The whole-output meta recoveries all decline: the
 * collapse argument is an {@link com.regnosys.rosetta.ast.expressions.references.RImplicitVariable}
 * (the rebound pipe), so {@code terminalNavAttr} descends the {@code last}/{@code only-element} to
 * it and returns null — {@code tryTerminalMetaMapperType} (#237/#249) and
 * {@code recoverDisguisedTerminalMetaWrapper} (#287) both decline, and the compiled type erases to
 * null; the {@code recoverInnerRuleMetaWrapper} walker STOPS at the disguised-chain producer body
 * ({@code assignedIdentifier -> identifier}, an {@code REnumValueRef} whose 1-arg
 * {@code resolveDisguisedFeature} cannot re-root the chain).
 *
 * <p><b>The fix.</b> {@code NavigationHandler.recoverCollapsedProducerMeta} recovers the leaf
 * wrapper from the UPSTREAM PRODUCER body (the then-body before the standalone collapse): a plain
 * nav leaf via {@code tryTerminalMetaMapperType}, a DISGUISED 2-name chain via
 * {@code resolveDisguisedChainLeafAttr} with {@code descendListOps == false} — which SKIPS the #287
 * multi-head gate (correct here because the downstream single-collapse terminal already guarantees a
 * SINGLE output). {@code FunctionExpressionRenderer.renderThenExtractSet} passes it as the
 * {@code renderMetaValueDerefOrNull} precomputed meta (the #265/#287 channel) when the last then-body
 * is a standalone single-collapse list-op over the pipe, RULE-scoped + single-output
 * ({@code !multiOutput}) + only when the #265/#287 recoveries declined.
 *
 * <p><b>Green-safe by construction.</b> A green rule with this shape would emit the bare
 * wrapper-into-bare-output (NON_COMPILING) → already waivered, never green; an output that IS the
 * wrapper declines at {@code renderMetaValueDerefOrNull}'s outputTypeName-equals-metaSimple guard; a
 * non-meta producer recovers null. byte-oracle / stash-baseline measured exactly 2 flips (drr POJO
 * 440 → 438; 14 now-matching = 2 mine + 12 stale, clean-main re-dump = exactly the 12 stale); all
 * FUNCTION cells (cdm5 81 / cdm6 236 / drr 207) + cdm/iso/fpml POJO byte-IDENTICAL (#232); regscan 0
 * within-waiver regressions.
 *
 * <p>Anchors are WHOLE-FILE byte comparisons of the generated drr output against the frozen goldens
 * (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}. REVERT-VERIFIED RED.
 */
class RuleCollapsedPipeMetaTest {

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

    // ==== Flip locks (revert-RED): the collapsed-pipe meta-deref carriers now byte-match. ====

    /**
     * Flip — cftc OriginalSwapUSIRule: {@code … then extract assignedIdentifier -> identifier then
     * last}. The producer is a disguised 2-name chain ({@code assignedIdentifier -> identifier},
     * {@code identifier} a {@code FieldWithMetaString} leaf), collapsed to a single by {@code last};
     * golden hoists {@code final FieldWithMetaString fieldWithMetaString = thenArg5.last().get();}
     * + {@code output = fieldWithMetaString.getValue()}.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void originalSwapUSI_cftc_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/cftc/rewrite/trade/reports/OriginalSwapUSIRule.java");
    }

    /**
     * Flip — cftc (rewrite trade) UniqueSwapIdentifierRule: the {@code only-element} sibling
     * ({@code … then extract assignedIdentifier -> identifier then only-element}), whole-output
     * {@code output = MapperS.of(thenArg5.get()).get()} → golden hoists + derefs the wrapper.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void uniqueSwapIdentifier_cftcTrade_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/cftc/rewrite/trade/reports/UniqueSwapIdentifierRule.java");
    }

    // ==== Facet-boundary / green-safety decline locks: the recovery is scoped to the byte-clean
    //      trade variant + declines for a non-nav producer (the co-occupied siblings stay divergent). ====

    /**
     * CONVERTED at PR #354 (facet nullTypedElseMetaDeref, F-m1b) — the cftc-MARGIN
     * UniqueSwapIdentifierRule now byte-matches golden. The pre-#354 scope lock guarded the
     * #331 collapsedPipeMeta recovery from spuriously flipping this co-occupied sibling
     * (its {@code distinct}-over-block-lambda producer still declines that recovery,
     * unchanged); the sibling's OWN divergence — the un-deref'd {@code FieldWithMetaString}
     * terminal inside the effective-else block's deep-hoisted then-chain else arm — closed
     * via the NULL-typed-arm recovery ({@code recoverExprMetaWrapper} case (b) + the
     * re-stamped coercion route; the outer {@code thenArg0} decl element followed through
     * the #294 {@code blockArmDerefsToBareLeaf} marker gate).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void uniqueSwapIdentifier_cftcMargin_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/cftc/rewrite/margin/reports/UniqueSwapIdentifierRule.java");
    }

    /**
     * CONVERTED at PR #354 (facet nullTypedElseMetaDeref, F-m1b) — the cftc-VALUATION
     * sibling of the margin variant, same shape, same conversion.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void uniqueSwapIdentifier_cftcValuation_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/cftc/rewrite/valuation/reports/UniqueSwapIdentifierRule.java");
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
                + path + " (PR #290 collapsedPipeMeta).");
    }

    private static void assertStaysDivergent(String path) throws IOException {
        assertNotNull(drrOutput, "drr generation did not run — corpus unavailable?");
        String generated = drrOutput.get(path);
        assertNotNull(generated, "Class not generated: " + path);
        Path goldenPath = DRR_GOLDEN_DIR.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertFalse(normalize(golden).equals(normalize(generated)),
                path + " must STAY divergent (the collapsedPipeMeta recovery is scoped to the "
                + "byte-clean trade variant; this co-occupied sibling declines).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
