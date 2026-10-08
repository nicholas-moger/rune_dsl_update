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
 * PR #191 (facet {@code tailMulti} — the evaluate-arg {@code .get()} &rarr; {@code .getMulti()}
 * CARDINALITY dimension, the remaining direction of PR #171's {@code evaluate_arg_consumption}
 * law).
 *
 * <p>The law: an evaluate argument's unwrap accessor follows the CALLEE PARAMETER's
 * cardinality, not the argument expression's. A SINGLE-cardinality {@code Mapper} chain
 * passed to a MULTI ({@code 0..*}, {@code List}-typed) callee parameter coerces via
 * {@code .getMulti()} (upstream {@code ExpressionGenerator} coerces any {@code Mapper} to a
 * {@code List} parameter with {@code getMulti()}; {@code MapperS.getMulti()} yields a 0-or-1
 * element {@code List}). The fork emitted the non-compiling scalar {@code .get()} (a single
 * {@code T} where {@code List<T>} is expected), e.g.
 * {@code MapBondOptionAccountPartyReference}:
 * <pre>
 *   mapBuyerSellerToAccountPartyReference.evaluate(fpmlAccount,
 *       MapperS.of(fpmlBondOption).&lt;BuyerSellerModel&gt;map("getBuyerSellerModel",
 *           bondOption -&gt; bondOption.getBuyerSellerModel()).getMulti())
 * </pre>
 * because the {@code MapBuyerSellerToAccountPartyReference} input
 * {@code fpmlBuyerSellerModelModelList fpml.BuyerSellerModel (0..*)} is MULTI while the
 * navigation {@code getBuyerSellerModel} is single, so the existing
 * {@link com.regnosys.rosetta.generator.java.expression.handlers.ReferenceHandler}
 * {@code evaluateArgIsMulti} probe (which required BOTH callee-param-multi AND
 * arg-cardinality-multi, PR #131/#146/#171) declined and unwrapped with the scalar
 * {@code .get()}.
 *
 * <p>Wiring (revert-locked by these whole-file anchors): the evaluate-arg unwrap accessor is
 * now driven by {@code evaluateParamIsMulti} (callee param multi by arg index) instead of the
 * tighter {@code evaluateArgIsMulti}, so a single {@code Mapper} chain into a multi parameter
 * unwraps with {@code .getMulti()}. {@code evaluateArgIsMulti} (param-multi AND arg-multi) is
 * unchanged and still gates the meta-free arg coercion and the closure-param {@code .get()}
 * append — those positions keep their PR #171/#180 behaviour exactly.
 *
 * <p>The widening only ever reaches the {@code unwrapForEvaluateArg} fall-through, which fires
 * solely for chained {@code Mapper} expressions (every such expression has {@code getMulti()});
 * a scalar {@code .get()} into a {@code List} parameter never compiled, so no green file
 * carries the pre-fix form (green-safe by construction). {@code MapCancelableProvisionToAncillaryParty}
 * proves the per-argument precision: arg index 1 (a MULTI {@code mapAncillaryParty} param)
 * flips to {@code .getMulti()} while the SINGLE arg index 2 stays {@code .get()} on the SAME
 * call.
 *
 * <p>This is the revert-verified lock: it generates through
 * {@link D11CorpusRegressionTest#loadCellCorpusCached} (the REAL D11 loader) and byte-compares
 * WHOLE FILES against the frozen goldens (newline-normalized — fragment assertions are
 * insufficient per the PR #153 lesson). All carriers are cdm 6.20.6 ingest-fpml.
 */
class FunctionEvaluateArgMultiTest {

    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static Map<String, String> cdm6FunctionOutput;

    static boolean cellsAvailable() {
        return Files.isDirectory(CDM6_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM6_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (cellsAvailable()) {
            cdm6FunctionOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CDM6_CELL_ROOT));
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

    // ---- evaluate-arg single-Mapper-into-multi-param .getMulti() coercion -------

    /** A SINGLE getBuyerSellerModel navigation into the MULTI (0..*) callee param coerces with .getMulti(). */
    @Test
    @EnabledIf("cellsAvailable")
    void mapBondOptionAccountPartyReference_cdm6_singleChainIntoMultiParam_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/product/bondoption/functions/MapBondOptionAccountPartyReference.java");
    }

    /** Per-arg precision: arg 1 flips to .getMulti() (MULTI param), arg 2 stays .get() (SINGLE param). */
    @Test
    @EnabledIf("cellsAvailable")
    void mapCancelableProvisionToAncillaryParty_cdm6_perArgCardinality_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/party/functions/MapCancelableProvisionToAncillaryParty.java");
    }

    /** A second AccountPartyReference carrier for family breadth. */
    @Test
    @EnabledIf("cellsAvailable")
    void mapEquityOptionAccountPartyReference_cdm6_singleChainIntoMultiParam_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/product/equityoption/functions/MapEquityOptionAccountPartyReference.java");
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
                + " — the non-compiling scalar .get() at the multi-cardinality evaluate-arg "
                + "position reappears if the evaluateParamIsMulti unwrap gate is reverted.");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
