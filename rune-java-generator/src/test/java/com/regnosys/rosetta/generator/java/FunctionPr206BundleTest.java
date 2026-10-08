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
 * PR #206 — Path A composed disjoint cluster (9 FUNCTION flips), whole-file byte anchors
 * through the REAL D11 loader. Each anchor reverts RED if its law's fix is removed; both
 * laws are green-safe by construction (the pre-fix forms never compiled, so every carrier
 * was already waivered — combined revert-baseline confirmed 0 now-matching pristine).
 *
 * <p><b>Law 1 — enumQualify (7 drr flips).</b> A bare enum comparison operand whose SIBLING
 * is a bare ALIAS or FUNCTION reference with an enum-typed result stayed unqualified
 * ({@code MapperS.of(NEWT)} — a non-compiling undefined symbol) because the sibling-enum
 * resolver ({@code ComparisonHandler.tryBareEnumComparand} → {@code NavigationHandler.leafEnumeration})
 * only handled a direct {@code RFeatureCall} navigation / disguised {@code REnumValueRef} chain.
 * Exemplar {@code if actionType = NEWT} where {@code alias actionType: drrReport -> actionType}
 * renders {@code MapperS<ActionTypeEnum> actionType(...)} → golden {@code MapperS.of(ActionTypeEnum.NEWT)}.
 * Fix: {@code NavigationHandler.siblingComparandEnumeration} (RShortcut alias body / RFunction
 * output enum) + {@code ComparisonHandler.bareEnumOperandName} accepting the {@code REnumValueRef}-empty
 * parse shape.
 *
 * <p><b>Law 2 — negLiteral (2 cdm flips).</b> The unary-minus {@code -(literal * expr)} rewrite
 * hard-coded {@code <BigDecimal,BigDecimal,BigDecimal>} + {@code BigDecimal.valueOf(-1)} (+ a
 * {@code java.math.BigDecimal} import); when both operands are Integer-typed the golden renders
 * {@code MapperMaths.<Integer,Integer,Integer>multiply(MapperS.of(-1), MapperS.of(shiftDays))} with
 * NO BigDecimal import. Fix: {@code ArithmeticHandler} types the rewrite from the operand JOIN via
 * {@code HandlerHelper.numericOperandKind} (the #167 machinery the binary path uses); a {@code number}
 * or unresolved operand DECLINES to the legacy BigDecimal form byte-verbatim.
 */
class FunctionPr206BundleTest {

    private static final Path CDM5_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-5.38.0");
    private static final Path CDM5_GOLDEN_DIR = CDM5_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR = CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR = DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static Map<String, String> cdm5FunctionOutput;
    private static Map<String, String> cdm6FunctionOutput;
    private static Map<String, String> drrFunctionOutput;

    static boolean cdm5Available() {
        return Files.isDirectory(CDM5_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM5_GOLDEN_DIR);
    }

    static boolean cdm6Available() {
        return Files.isDirectory(CDM6_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM6_GOLDEN_DIR);
    }

    static boolean drrAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (cdm5Available()) {
            cdm5FunctionOutput = generateCell(new D11CorpusRegressionTest.CellSpec("cdm", "5.38.0", CDM5_CELL_ROOT));
        }
        if (cdm6Available()) {
            cdm6FunctionOutput = generateCell(new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CDM6_CELL_ROOT));
        }
        if (drrAvailable()) {
            drrFunctionOutput = generateCell(new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT));
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

    /** enumQualify: `if actionType = NEWT` over the alias `actionType: drrReport -> actionType` → ActionTypeEnum. */
    @Test
    @EnabledIf("drrAvailable")
    void createTradeReport33Choice_bareEnumQualifiedFromAliasSibling_byteMatchesGolden() throws IOException {
        assertByteMatches(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/projection/iso20022/asic/rewrite/trade/functions/Create_TradeReport33Choice__1.java",
                "the bare `MapperS.of(NEWT)` operands revert to un-prefixed (undefined-symbol) constants — "
                + "siblingComparandEnumeration / bareEnumOperandName removed");
    }

    /** negLiteral: `-1 * shiftDays` over Integer shiftDays → Integer multiply with a bare negated literal. */
    @Test
    @EnabledIf("cdm5Available")
    void cdm5GenerateObservationPeriod_negIntLiteralBare_byteMatchesGolden() throws IOException {
        assertByteMatches(cdm5FunctionOutput, CDM5_GOLDEN_DIR,
                "cdm/observable/asset/calculatedrate/functions/GenerateObservationPeriod.java",
                "the `<Integer,Integer,Integer>multiply(MapperS.of(-1), ...)` reverts to the "
                + "`<BigDecimal,...>multiply(MapperS.of(BigDecimal.valueOf(-1)), ...)` + java.math.BigDecimal import");
    }

    /** negLiteral cdm6 sibling of the above (same `-1 * shiftDays` Integer rewrite). */
    @Test
    @EnabledIf("cdm6Available")
    void cdm6GenerateObservationPeriod_negIntLiteralBare_byteMatchesGolden() throws IOException {
        assertByteMatches(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/observable/asset/calculatedrate/functions/GenerateObservationPeriod.java",
                "the Integer-typed negated-literal multiply reverts to the BigDecimal form");
    }

    private static void assertByteMatches(Map<String, String> output, Path goldenDir, String path, String revertHint)
            throws IOException {
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
