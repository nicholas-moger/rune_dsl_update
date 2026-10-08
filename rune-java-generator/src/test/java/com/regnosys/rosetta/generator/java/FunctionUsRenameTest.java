package com.regnosys.rosetta.generator.java;

import com.regnosys.rosetta.generator.GenerationException;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PR #194 (facet {@code usRename} — a FUNCTION output variable is escaped with a leading
 * {@code "_"} when its name collides with an injected same-named function/rule DEPENDENCY).
 *
 * <p>The law (upstream {@code FunctionGenerator.xtend} + {@code GeneratorScope}): each injected
 * dependency is registered on the function's CLASS scope under its emitted field name
 * ({@code simpleName.toFirstLower}); the output variable is allocated on a CHILD method/body
 * scope whose taken-name set is seeded from the parent, so a same-named output collides and
 * {@code escapeName} prepends a single {@code "_"} ({@code _notionalAmount}, {@code
 * _interestRateLeg1}). The dependency INVOCATION resolves by a different key and keeps the bare
 * name, yielding {@code _interestRateLeg1 = toBuilder(interestRateLeg1.evaluate(product))}. The
 * fork emitted the raw attribute name for the output local in two uncoordinated pipelines — the
 * ST4 {@code ParamModel.name} (signature/locals/return/{@code @return} javadoc) and the renderer's
 * {@code ROperation.targetName} (the SET left-hand side) — so the output local SHADOWED the
 * {@code @Inject} field and {@code <name>.evaluate(...)} resolved to a value, not a method →
 * NON-COMPILING. Hence every collision file was already a waivered mismatch; no green file can
 * carry the un-prefixed colliding form (green-safe by construction). The escape fires ONLY on an
 * EXACT name match against the dependency-field set ({@code .equals}, not prefix/substring), so a
 * near-miss like output {@code interestRateLeg1} alongside the longer deps
 * {@code interestRateLeg1Basis}/{@code interestRateLeg1CrossCurrency} (which do NOT exactly equal
 * the output) leaves those deps bare and escapes only against the exact {@code interestRateLeg1}.
 *
 * <p>Wiring (revert-locked by these whole-file anchors): {@code FunctionGenerator.buildStandardModel}
 * computes {@code escapedOutputName} (a {@code "_"} prefix iff the output name equals a collected
 * {@code DependencyModel.getFieldName()}), feeds it to the output {@code ParamModel} (the whole
 * ST4 template), the {@code @return} javadoc, and — threaded through {@code compileOperations} →
 * {@link com.regnosys.rosetta.generator.java.function.FunctionExpressionRenderer}{@code
 * .renderOperation} — the renderer's SET left-hand side (substituted for {@code targetName} when
 * {@code operation.targetName()} equals the raw output name).
 *
 * <p>This is the revert-verified lock: it generates through
 * {@link D11CorpusRegressionTest#loadCellCorpusCached} (the REAL D11 loader) and byte-compares
 * WHOLE FILES against the frozen goldens (newline-normalized — fragment assertions are
 * insufficient per the PR #153 lesson). All carriers are drr/6.34.1.
 *
 * <p>Anchored:
 * <ul>
 *   <li>{@code CSAInterestRateLeg1} — output {@code interestRateLeg1} (a MODEL output:
 *       {@code InterestRatePayout}, the builder + {@code objectValidator} path) collides with the
 *       exact dependency {@code interestRateLeg1}; proves the escape across the {@code doEvaluate}
 *       builder local, the {@code assignOutput} signature param, the renderer SET-LHS, the
 *       {@code return}, and the {@code @return} javadoc, while the longer sibling deps stay bare.</li>
 *   <li>{@code CommodityLeg1} — output {@code commodityLeg1} ({@code CommodityPayout}); a second
 *       csa-rewrite carrier.</li>
 *   <li>{@code NotionalAmountFormat} — output {@code notionalAmount} is a NON-model
 *       {@code BigDecimal} (no builder/validator path), proving the escape reaches the
 *       primitive-output template shape (the bare {@code assignOutput(BigDecimal _notionalAmount,
 *       …)} + {@code _notionalAmount = …evaluate(notionalAmount.evaluate(…))}).</li>
 *   <li>{@code NotionalAmountLeg1} — output {@code notionalAmountLeg1}, the iosco CDE
 *       version2 sibling, proving the cross-namespace path.</li>
 * </ul>
 */
class FunctionUsRenameTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static Map<String, String> drrFunctionOutput;

    static boolean cellsAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (cellsAvailable()) {
            drrFunctionOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT));
        }
    }

    private static Map<String, String> generateCell(D11CorpusRegressionTest.CellSpec cell)
            throws IOException {
        var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(),
                D11CorpusRegressionTest.emissionFilter(cell));
        var typeUtil = new JavaTypeUtil();
        var gen = new FunctionGenerator(gm, new JavaTypeTranslator(typeUtil), typeUtil);
        Map<String, String> output = new LinkedHashMap<>();
        List<GenerationException> genErrors = gen.generateWithErrors(output);
        assertTrue(genErrors.isEmpty(),
                cell + " FUNCTION generation reported errors: " + genErrors);
        return output;
    }

    // ---- output-name collision escape (output name == injected dependency name) ----

    /** MODEL output (InterestRatePayout) — builder + objectValidator path; exact collision. */
    @Test
    @EnabledIf("cellsAvailable")
    void csaInterestRateLeg1_drr_outputCollidesWithDependency_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/regulation/csa/rewrite/trade/functions/CSAInterestRateLeg1.java");
    }

    /** MODEL output (CommodityPayout) — second csa-rewrite carrier. */
    @Test
    @EnabledIf("cellsAvailable")
    void commodityLeg1_drr_outputCollidesWithDependency_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/regulation/csa/rewrite/trade/functions/CommodityLeg1.java");
    }

    /** NON-model output (BigDecimal) — proves the escape reaches the primitive-output shape. */
    @Test
    @EnabledIf("cellsAvailable")
    void notionalAmountFormat_drr_nonModelOutputCollides_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/standards/iosco/cde/version1/quantity/functions/NotionalAmountFormat.java");
    }

    /** iosco CDE version2 sibling — cross-namespace path. */
    @Test
    @EnabledIf("cellsAvailable")
    void notionalAmountLeg1_drr_outputCollidesWithDependency_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/standards/iosco/cde/version2/quantity/functions/NotionalAmountLeg1.java");
    }

    private static void assertByteMatchesGolden(Map<String, String> output, Path goldenDir, String path)
            throws IOException {
        assertNotNull(output, "Function generation did not run — corpus unavailable?");
        String generated = output.get(path);
        assertNotNull(generated, "Function not generated: " + path);
        Path goldenPath = goldenDir.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated output must byte-match the golden (newline-normalized) for " + path
                + " — the un-prefixed output local (which shadows the same-named injected "
                + "dependency field) reappears if the usRename `_`-escape "
                + "(FunctionGenerator.escapedOutputName + the renderer targetName substitution) "
                + "is reverted.");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
