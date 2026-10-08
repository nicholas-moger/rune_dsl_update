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
 * Facet {@code tobuilder_output_assignment} — recoveries in the function-output
 * BUILDER-ASSIGNMENT path (census over f-probe-164): a whole-output SET of a Rosetta
 * model type must coerce its value into the output's builder form exactly the way
 * upstream {@code FunctionGenerator.assign} does, and the deep-path SET machinery
 * around it (path segments, zero-input call values, only-element collapses) must
 * render the same bytes. FIVE mechanisms:
 *
 * <ol>
 *   <li><b>Conditional-arm toBuilder wrap.</b> A whole-output SET whose body is a
 *       conditional rendered bare arm assignments ({@code output = <value>;}) for a
 *       model-typed output where golden wraps every non-null arm
 *       ({@code output = toBuilder(<value>);}) — upstream maps the wrap over the
 *       compiled conditional distributively ({@code mapExpressionIfNotNull} via
 *       {@code JavaIfThenElseBuilder}), skipping {@code null} arms.
 *       {@code FunctionExpressionRenderer.renderConditionalAssignment} received
 *       {@code outputNeedsBuilder} but never consulted it at its two leaf
 *       assignment emissions.</li>
 *   <li><b>Deep-operations supplier arg.</b> A function with at least one deep
 *       (path-tailed) operation wraps its whole-output SET as
 *       {@code toBuilder(<value>, () -> <Output>.builder())} — the supplier keeps the
 *       builder non-null for the subsequent {@code .getOrCreate…} chains — mirroring
 *       upstream's {@code functionHasDeepOperations} flag
 *       ({@code operations.filter[pathTail.size > 0].size > 0}). The fork emitted the
 *       one-arg form unconditionally.</li>
 *   <li><b>Only-element witnessless collapse.</b> A multi value collapsed to an item
 *       by {@code only-element} takes upstream's WITNESSLESS List-to-item unwrap
 *       ({@code MapperC.of(<list>).get()}, {@code TypeCoercionService}'s
 *       {@code getListToItemConversionExpression}); only STAY-multi List-to-Mapper
 *       coercions carry the {@code MapperC.<Item>of(...)} witness. The fork's
 *       consumer-blind multi-value wrap emitted the witnessed form under the collapse
 *       too.</li>
 *   <li><b>List-segment getOrCreate index.</b> An intermediary deep-path segment whose
 *       attribute is multi renders {@code .getOrCreate<Name>(0)} (the POJO builder's
 *       list overload — upstream {@code «IF prop.type.isList»0«ENDIF»}); the fork
 *       hardcoded the no-arg form, which does not even compile against the fork's own
 *       list-property builders.</li>
 *   <li><b>Zero-real-input call value.</b> An explicit call to a function with NO real
 *       inputs (every input is the {@code __synthesized_input__} placeholder) renders
 *       the argless {@code <receiver>.evaluate()} and, as a deep-path SET value, passes
 *       the bare POJO result verbatim into the setter — the fork synthesized a spurious
 *       implicit {@code item.get()} argument (a free identifier in a function body) and
 *       appended a spurious {@code .get()} through the unwrap fall-through.</li>
 * </ol>
 *
 * <p>This test is REVERT-VERIFIED RED: reverting any one mechanism reverts its anchors
 * to the divergent shape. Anchors are WHOLE-FILE byte comparisons against the frozen
 * goldens (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}, chosen from the facet's
 * waivered files (census over f-probe-164):
 * <ul>
 *   <li>cdm/6.20.6 {@code UnderlierForProduct} — mechanism 1 ALONE, nav-chain
 *       {@code .get()} arm values through an {@code else if} cascade;</li>
 *   <li>drr/6.34.1 {@code ProductForEvent} — mechanism 1 ALONE, function-call arm
 *       values ({@code product = toBuilder(productForTrade.evaluate(…));});</li>
 *   <li>drr/6.34.1 {@code Create_SubmissionCore_Part43} — mechanism 2 ALONE: the
 *       supplier arg on {@code toBuilder(commonCore, () -> Core.builder())};</li>
 *   <li>cdm/6.20.6 {@code ExtractOpenEconomicTerms} — mechanism 3 ALONE: golden's
 *       witnessless {@code MapperC.of(filterOpenTradeStates.evaluate(…)).get()};</li>
 *   <li>cdm/6.20.6 {@code Create_NonTransferableProduct} — mechanism 4 ALONE: the
 *       {@code .getOrCreatePayout(0)} list index;</li>
 *   <li>cdm/6.20.6 {@code TradeNoExecutionDetails} — mechanisms 2 + 5 together: the
 *       supplier arg plus the argless
 *       {@code .setExecutionDetails(emptyExecutionDetails.evaluate());}.</li>
 * </ul>
 */
class FunctionToBuilderOutputAssignmentTest {

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

    /** Mechanism 1 alone: conditional-arm toBuilder wrap, nav-chain arm values. */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void underlierForProduct_conditionalArmToBuilder_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/margin/schedule/functions/UnderlierForProduct.java");
    }

    /** Mechanism 1 alone: conditional-arm toBuilder wrap, function-call arm values. */
    @Test
    @EnabledIf("drrCellAvailable")
    void productForEvent_conditionalArmToBuilderFnCall_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/regulation/common/functions/ProductForEvent.java");
    }

    /** Mechanism 2 alone: the deep-operations supplier arg on the whole-output SET. */
    @Test
    @EnabledIf("drrCellAvailable")
    void createSubmissionCorePart43_toBuilderSupplierArg_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/projection/dtcc/rds/harmonized/cftc/rewrite/trade/functions/Create_SubmissionCore_Part43.java");
    }

    /** Mechanism 3 alone: the only-element collapse drops the MapperC witness. */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void extractOpenEconomicTerms_onlyElementWitnessless_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/event/common/functions/ExtractOpenEconomicTerms.java");
    }

    /** Mechanism 4 alone: the list-segment getOrCreate index. */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void createNonTransferableProduct_getOrCreateListIndex_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/event/common/functions/Create_NonTransferableProduct.java");
    }

    /** Mechanisms 2 + 5: the supplier arg plus the argless zero-input call value. */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void tradeNoExecutionDetails_supplierAndZeroInputEvaluate_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/event/common/functions/TradeNoExecutionDetails.java");
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
                + path + " — a conditional-arm whole-output SET loses its toBuilder(...) "
                + "wrap; a deep-operations function loses the () -> T.builder() supplier "
                + "arg; an only-element collapse carries a spurious MapperC witness; a "
                + "multi path segment loses its getOrCreate(0) index; and a zero-input "
                + "call value gains a spurious item.get() argument and .get() unwrap "
                + "if the respective mechanism is reverted.");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
