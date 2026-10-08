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
 * Facet {@code alias_receiver_typing} — recoveries rooted in the resolution-blind
 * treatment of a function-body ALIAS ({@code RShortcut}) used as a value (census over
 * f-probe-163; the alias method itself was already emitted with the correct
 * {@code MapperS/MapperC<? extends Type>} signature — only the USE sites were blind
 * to it). The docs (CHANGELOG / waiver header) enumerate FIVE mechanisms; this class
 * anchors the four with a dedicated byte shape — the fifth (the MULTI alias's
 * {@code MapperC} method-signature import, docs mechanism 2) has no isolated anchor
 * and is locked by the whole-file byte anchors below plus the D11 gate on all 22
 * flipped files. The numbering below is this class's own:
 *
 * <ol>
 *   <li><b>Alias-call receiver typing.</b> A navigation chain rooted at an alias call
 *       ({@code openEconomicTerms -> terminationDate}, rendering
 *       {@code openEconomicTerms(businessEvent).map(…)}) lost the {@code <Type>}
 *       witness, the {@code map}/{@code mapC} cardinality selection, the witness
 *       IMPORTS, and the type-derived lambda var ({@code _openEconomicTerms} underscore
 *       fallback instead of golden's {@code economicTerms}) on the first step and every
 *       step after it, because the gm-aware
 *       {@code NavigationHandler.resolveReceiverDataType} had no {@code RShortcut} base
 *       case. The base case resolves the alias's value type by recursing into the
 *       alias's own expression through the SAME walk, cascading witness + cardinality +
 *       import + naming recovery through the whole chain
 *       ({@code openEconomicTerms(businessEvent).<Payout>mapC("getPayout",
 *       economicTerms -> economicTerms.getPayout())}).</li>
 *   <li><b>Alias-call multi-cardinality unwrap.</b> A MULTI-cardinality alias call
 *       passed to a {@code List}-typed function parameter unwrapped with the
 *       non-compiling scalar {@code .get()} instead of golden's {@code .getMulti()}
 *       ({@code generateWeights.evaluate(weightingDates(…).getMulti())}), because the
 *       parser {@code CardinalityComputer} read every {@code RShortcut} symbol
 *       reference as SINGLE — upstream's {@code CardinalityProvider} resolves a
 *       shortcut to its expression's cardinality.</li>
 *   <li><b>Multi-output sort-assignment unwrap.</b> A {@code sort} result assigned to a
 *       MULTI-cardinality output unwrapped with the wrong-arity {@code .get()} instead
 *       of golden's {@code .getMulti()}
 *       ({@code adjustedValuationDates = MapperC.<Date>of(…).sort().getMulti();}) —
 *       {@code FunctionExpressionRenderer.unwrapForAssignment}'s fall-through was
 *       cardinality-blind; the direct-SET path now selects {@code .getMulti()} when
 *       both the output attribute and the value expression are multi.</li>
 *   <li><b>Chained lambda-var scope disambiguation.</b> A CHAINED navigation step's
 *       type-derived lambda var skipped {@code scope.disambiguate}, so a var colliding
 *       with an in-scope name (the function input {@code product}) rendered the
 *       non-compiling shadow {@code product -> product.getForeignExchange()} instead of
 *       golden's escaped {@code _product} (upstream's
 *       {@code createUniqueIdentifier(type.toFirstLower)} always escapes against the
 *       enclosing scope).</li>
 * </ol>
 *
 * <p>This test is REVERT-VERIFIED RED: reverting any one mechanism reverts its anchors
 * to the divergent shape. Anchors are WHOLE-FILE byte comparisons against the frozen
 * goldens (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}, chosen from the facet's
 * waivered files (census over f-probe-163):
 * <ul>
 *   <li>cdm/6.20.6 {@code Qualify_Adjustment} — mechanism 1: alias-receiver witness +
 *       {@code mapC} + lambda var + the {@code AdjustableOrRelativeDate} witness
 *       import;</li>
 *   <li>cdm/6.20.6 {@code GenerateWeightings} — mechanism 2 ALONE: the multi alias-call
 *       argument unwraps {@code .getMulti()};</li>
 *   <li>cdm/6.20.6 {@code AdjustedValuationDates} — mechanism 3 ALONE: the sort
 *       assignment to the multi output unwraps {@code .getMulti()};</li>
 *   <li>drr/6.34.1 {@code IsFXForward} — mechanism 4 ALONE: the chained lambda var
 *       escapes to {@code _product} against the {@code product} input.</li>
 * </ul>
 */
class FunctionAliasReceiverTypingTest {

    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static Map<String, String> cdm6FunctionOutput;
    private static Map<String, String> drrFunctionOutput;

    static boolean cdm6CellAvailable() {
        return Files.isDirectory(CDM6_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM6_GOLDEN_DIR);
    }

    static boolean drrCellAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR);
    }

    @BeforeAll
    static void generateFunctions() throws IOException {
        if (cdm6CellAvailable()) {
            cdm6FunctionOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CDM6_CELL_ROOT));
        }
        if (drrCellAvailable()) {
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

    /** Mechanism 1: alias-receiver witness + mapC + lambda var + witness import. */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void qualifyAdjustment_aliasReceiverTyping_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/event/common/functions/Qualify_Adjustment.java");
    }

    /** Mechanism 2 alone: the multi alias-call argument unwraps .getMulti(). */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void generateWeightings_aliasArgGetMulti_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/observable/asset/calculatedrate/functions/GenerateWeightings.java");
    }

    /** Mechanism 3 alone: the sort assignment to the multi output unwraps .getMulti(). */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void adjustedValuationDates_sortAssignmentGetMulti_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/event/common/functions/AdjustedValuationDates.java");
    }

    /** Mechanism 4 alone: the chained lambda var escapes against the colliding input. */
    @Test
    @EnabledIf("drrCellAvailable")
    void isFXForward_chainedLambdaVarEscape_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/regulation/common/functions/IsFXForward.java");
    }

    private static void assertByteMatchesGolden(
            Map<String, String> functionOutput, Path goldenDir, String path) throws IOException {
        assertNotNull(functionOutput, "Function generation did not run — corpus unavailable?");
        String generated = functionOutput.get(path);
        assertNotNull(generated, "Function not generated: " + path);
        Path goldenPath = goldenDir.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated output must byte-match the golden (newline-normalized) for "
                + path + " — an alias-rooted navigation loses its <Type> witnesses, "
                + "map/mapC cardinality, witness imports and type-derived lambda var; a "
                + "multi alias-call argument / multi-output sort assignment unwraps the "
                + "wrong-arity .get(); and a chained lambda var shadows an in-scope name "
                + "if the respective mechanism is reverted.");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
