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
 * PR #211 facet mapperCofMapperSWrap (4 cdm FUNCTION flips: cdm5 1 + cdm6 3), whole-file byte
 * anchors through the REAL D11 loader. Each anchor reverts RED if the fix is removed; the law is
 * green-safe by construction (the pre-fix bare constructor element never matched golden — golden
 * ALWAYS wraps a MapperS-expecting constructor in MapperS.of, the PR #180 law — so every carrier
 * was already waivered; the stash-baseline confirmed 0 now-matching pristine and
 * {@code function_comparison} stays green).
 *
 * <p><b>The law.</b> Inside a {@code MapperC.<X>of(<element>, ...)} list-literal seat each element
 * must be a {@code Mapper}. A CONSTRUCTOR element ({@code RConstructorExpr} — {@code X.builder()
 * ...build()}) compiles BARE ({@code ExpressionCompiler.visitConstructor}; the expected-type
 * coercion never wraps a constructor — PR #180 established this at the lambda-body seat, adding
 * {@code wrappedInMapperSOf} there), where golden wraps it {@code MapperS.of(X.builder()...build())}
 * — the SAME PR #180 {@code wrappedInMapperSOf} law at the list-literal element position. The
 * function-call element sibling is already wrapped via expected-type coercion (visible in the same
 * {@code Create_*PrimitiveInstruction} files: {@code setBreakdown(MapperC.<T>of(MapperS.of(<call>)))}
 * renders correctly while the {@code addBreakdown([ <ctor> ])} list-literal lost the wrap). Fix:
 * {@code LiteralHandler.handle(RListLiteral)} adds an {@code RConstructorExpr} element arm mirroring
 * the existing bare-enum arms. Carriers: {@code Create_OnDemand/PartialDeliveryPrimitiveInstruction}
 * + the cdm6 ingest {@code MapWorkflowStep} (whose sole remaining divergence was this wrap).
 */
class FunctionPr211MapperCofMapperSWrapTest {

    private static final Path CDM5_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-5.38.0");
    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM5_GOLDEN_DIR = CDM5_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM6_GOLDEN_DIR = CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static Map<String, String> cdm5FunctionOutput;
    private static Map<String, String> cdm6FunctionOutput;

    static boolean cdmCellsAvailable() {
        return Files.isDirectory(CDM5_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM5_GOLDEN_DIR)
                && Files.isDirectory(CDM6_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM6_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (cdmCellsAvailable()) {
            cdm5FunctionOutput = generateCell(new D11CorpusRegressionTest.CellSpec("cdm", "5.38.0", CDM5_CELL_ROOT));
            cdm6FunctionOutput = generateCell(new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CDM6_CELL_ROOT));
        }
    }

    private static Map<String, String> generateCell(D11CorpusRegressionTest.CellSpec cell) throws IOException {
        var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(), D11CorpusRegressionTest.emissionFilter(cell));
        var typeUtil = new JavaTypeUtil();
        var gen = new FunctionGenerator(gm, new JavaTypeTranslator(typeUtil), typeUtil);
        Map<String, String> output = new LinkedHashMap<>();
        List<GenerationException> genErrors = gen.generateWithErrors(output);
        assertTrue(genErrors.isEmpty(), cell + " FUNCTION generation reported errors: " + genErrors);
        return output;
    }

    private static final String REVERT =
            "the MapperS.of(...) wrap around the constructor element inside MapperC.<X>of(...) reverts "
            + "to the bare X.builder()...build() — the RConstructorExpr arm in LiteralHandler was removed";

    /** cdm6 Create_OnDemandRateChange: addBreakdown([ PrimitiveInstruction {...} ]) wraps the ctor. */
    @Test
    @EnabledIf("cdmCellsAvailable")
    void cdm6CreateOnDemandRateChange_ctorElementWrap_byteMatchesGolden() throws IOException {
        assertByteMatches(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/event/common/functions/Create_OnDemandRateChangePrimitiveInstruction.java", REVERT);
    }

    /** cdm6 Create_PartialDelivery: the sibling primitive-instruction constructor. */
    @Test
    @EnabledIf("cdmCellsAvailable")
    void cdm6CreatePartialDelivery_ctorElementWrap_byteMatchesGolden() throws IOException {
        assertByteMatches(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/event/common/functions/Create_PartialDeliveryPrimitiveInstruction.java", REVERT);
    }

    /** cdm6 MapWorkflowStep: the ingest carrier whose sole remaining divergence was this wrap. */
    @Test
    @EnabledIf("cdmCellsAvailable")
    void cdm6MapWorkflowStep_ctorElementWrap_byteMatchesGolden() throws IOException {
        assertByteMatches(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/workflowstep/functions/MapWorkflowStep.java", REVERT);
    }

    /** cdm5 Create_PartialDelivery: the same law one model version back. */
    @Test
    @EnabledIf("cdmCellsAvailable")
    void cdm5CreatePartialDelivery_ctorElementWrap_byteMatchesGolden() throws IOException {
        assertByteMatches(cdm5FunctionOutput, CDM5_GOLDEN_DIR,
                "cdm/event/common/functions/Create_PartialDeliveryPrimitiveInstruction.java", REVERT);
    }

    private static void assertByteMatches(Map<String, String> output, Path goldenDir,
            String path, String revertHint) throws IOException {
        assertNotNull(output, "Function generation did not run — corpus unavailable?");
        String generated = output.get(path);
        assertNotNull(generated, "Function not generated: " + path);
        Path goldenPath = goldenDir.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated output must byte-match the golden (newline-normalized) for " + path
                + " — " + revertHint + ".");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
