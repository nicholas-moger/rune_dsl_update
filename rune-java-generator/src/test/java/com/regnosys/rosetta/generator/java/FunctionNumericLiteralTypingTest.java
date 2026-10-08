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
 * Facet {@code numeric_literal_typing} — four RENDERER-ONLY recoveries sharing one
 * upstream law: a numeric literal's Java emission is decided by the EXPECTED type at
 * its consumption site (upstream 9.83.0 {@code ExpressionGenerator.binaryExpr} joins
 * the operand types and compiles each operand against the join;
 * {@code TypeCoercionService} then coerces {@code int} → {@code BigDecimal.valueOf(N)}
 * iff the expected item type is BigDecimal; {@code FunctionGenerator.assign} threads
 * the OUTPUT attribute's type as the expected type of a path-less SET). The fork
 * compiled these sites with a string-name heuristic or a {@code null} expected type:
 *
 * <ol>
 *   <li><b>Comparison-operand literal vs alias sibling.</b> An int literal compared
 *       against a number-typed ALIAS call ({@code initialPriceValue > 0}) stayed bare
 *       ({@code MapperS.of(0)}) instead of golden's
 *       {@code MapperS.of(BigDecimal.valueOf(0))}, because both
 *       {@code ComparisonHandler.inferNumericType} arms miss an {@code RShortcut}
 *       sibling (the inference engine hard-codes shortcut types MISSING). The new
 *       last-resort arm resolves the alias's defining expression (dual path:
 *       {@code symbol()} primary, by-name against the enclosing function's shortcuts
 *       fallback, alias-of-alias recursion behind an identity-set cycle guard) and a
 *       resolution-blind nav-chain leaf via the gm-aware leaf-attribute walk, and
 *       applies the resolved type to the LITERAL operand only.</li>
 *   <li><b>Arithmetic witnesses + literal operand.</b> {@code 1 / DayCountBasis(dcf)}
 *       (int literal ÷ int-output function call) emitted the uniform
 *       {@code MapperMaths.<BigDecimal, BigDecimal, BigDecimal>divide(MapperS.of(BigDecimal.valueOf(1)), …)}
 *       where golden carries {@code <BigDecimal, Integer, Integer>divide(MapperS.of(1), …)}:
 *       upstream's operand witnesses are the JOIN of the operands' own types (int ⊔ int
 *       = Integer — the literal counts as int evidence and stays bare) while divide's
 *       RESULT witness is always BigDecimal
 *       ({@code RosettaTypeProvider.caseDivideOperation} = unconstrained number,
 *       unconditionally). The typed per-operand walk resolves literals, attributes,
 *       function-call outputs, alias bodies, count, and nested arithmetic; ANY
 *       unresolved operand declines to the FULL legacy heuristic byte-verbatim (which
 *       green files such as drr {@code PeriodCalculation}'s
 *       {@code <Integer, Integer, Integer>multiply} rely on).</li>
 *   <li><b>Switch-arm literal assignment.</b> An int-literal switch case result
 *       assigned to the int-typed output spliced the compiled value verbatim
 *       ({@code result = MapperS.of(1);}, non-compiling) instead of golden's bare
 *       {@code result = 1;} — the case value now structurally unwraps to its item like
 *       the conditional path always did. Enum-constant case results (the PR #149 green
 *       family) keep the verbatim embed untouched.</li>
 *   <li><b>Conditional-arm literal assignment.</b> An int-literal conditional arm
 *       assigned to the number-typed output emitted the un-coerced bare
 *       {@code periodMultiplierValue = 1;} (non-compiling) instead of golden's
 *       {@code periodMultiplierValue = BigDecimal.valueOf(1);} — the arm now compiles
 *       against the output's BigDecimal item type so the existing
 *       {@code LiteralHandler} coercion + structural unwrap produce the coerced item.
 *       Integer-typed outputs are EXCLUDED by gate: green cdm/6.20.6
 *       {@code MapFrequencyToPeriodMultiplier} carries the bare-int arm
 *       ({@code periodMultiplier = 1;}) as its golden shape.</li>
 * </ol>
 *
 * <p>Corpus law (frozen 9.83.0 baseline, all 5 cells): golden MapperMaths witness
 * combos are exhaustively {@code <BD,BD,BD>} ×182 / {@code <I,I,I>} ×69 /
 * {@code <String,String,String>} ×33 (add only) / {@code <BD,I,I>} ×24 (divide only) —
 * operand witnesses always equal; comparison-operand int literals are wrapped iff the
 * sibling is number-typed (364 wrapped vs 573 bare, zero counter-examples either
 * direction); zero goldens assign {@code MapperS.of(<numeric literal>)} to an
 * item-typed output.
 *
 * <p>Anchors are WHOLE-FILE byte comparisons against the frozen goldens
 * (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}, one sole-mechanism waivered
 * file per mechanism:
 * <ul>
 *   <li>cdm/5.38.0 {@code RateOfReturn} — mechanism 1 ALONE (the cdm/6.20.6 sibling is
 *       byte-identical in shape);</li>
 *   <li>cdm/5.38.0 {@code YearFractionForOneDay} — mechanism 2 ALONE (the only owners
 *       of the mixed {@code <BigDecimal, Integer, Integer>} combo are
 *       {@code YearFraction}/{@code YearFractionForOneDay} ×2 cells);</li>
 *   <li>cdm/6.20.6 {@code MapCommodityClassificationOrdinal} — mechanism 3 ALONE (the
 *       corpus's only numeric-armed switchArgument golden, 6 arms);</li>
 *   <li>drr/6.34.1 {@code AdjustPeriodMultiplier} — mechanism 4 ALONE (one of exactly
 *       two {@code = BigDecimal.valueOf(N);} direct assignments in the corpus).</li>
 * </ul>
 */
class FunctionNumericLiteralTypingTest {

    private static final Path CDM5_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-5.38.0");
    private static final Path CDM5_GOLDEN_DIR =
            CDM5_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static Map<String, String> cdm5FunctionOutput;
    private static Map<String, String> cdm6FunctionOutput;
    private static Map<String, String> drrFunctionOutput;

    static boolean cdm5CellAvailable() {
        return Files.isDirectory(CDM5_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM5_GOLDEN_DIR);
    }

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
        if (cdm5CellAvailable()) {
            cdm5FunctionOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("cdm", "5.38.0", CDM5_CELL_ROOT));
        }
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

    /** Mechanism 1: int literal vs number-typed alias sibling wraps BigDecimal.valueOf. */
    @Test
    @EnabledIf("cdm5CellAvailable")
    void rateOfReturn_comparisonOperandLiteral_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5FunctionOutput, CDM5_GOLDEN_DIR,
                "cdm/event/common/functions/RateOfReturn.java");
    }

    /** Mechanism 2: int/int divide carries golden's mixed witnesses + bare literal. */
    @Test
    @EnabledIf("cdm5CellAvailable")
    void yearFractionForOneDay_arithmeticWitnesses_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5FunctionOutput, CDM5_GOLDEN_DIR,
                "cdm/base/datetime/daycount/functions/YearFractionForOneDay.java");
    }

    /** Mechanism 3: int-literal switch case results assign the bare item. */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapCommodityClassificationOrdinal_switchArmLiteral_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/pricequantity/functions/MapCommodityClassificationOrdinal.java");
    }

    /** Mechanism 4: int-literal conditional arm coerces to the number output's BigDecimal. */
    @Test
    @EnabledIf("drrCellAvailable")
    void adjustPeriodMultiplier_conditionalArmLiteral_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/regulation/common/functions/AdjustPeriodMultiplier.java");
    }

    private static void assertByteMatchesGolden(Map<String, String> functionOutput,
            Path goldenDir, String path) throws IOException {
        assertNotNull(functionOutput, "Function generation did not run — corpus unavailable?");
        String generated = functionOutput.get(path);
        assertNotNull(generated, "Function not generated: " + path);
        Path goldenPath = goldenDir.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated output must byte-match the golden (newline-normalized) for "
                + path + " — a numeric literal renders untyped (bare int where the "
                + "consumption site demands BigDecimal.valueOf, BigDecimal-defaulted "
                + "MapperMaths witnesses where the operand join is Integer, or a "
                + "MapperS-wrapped literal where the assignment demands the bare item) "
                + "if the numeric_literal_typing recovery is reverted.");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
