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
 * Facet {@code enumConstantToStringWrap} (PR #244) — a {@code to-string} conversion whose SOURCE
 * renders as a <em>bare dotted enum constant</em> ({@code EnumType.VALUE} — a literal enum value, e.g.
 * the rosetta {@code iso.ActionTypeEnum -> VALU to-string}) has no {@code .map()} method, so the fork's
 * {@code EnumType.VALUE.map("to-string", EnumType::toDisplayString)} does not compile and the carrier is
 * already waivered. Upstream {@code ExpressionGenerator.caseToStringOperation} compiles the to-string
 * argument against {@code MAPPER_S.wrapExtendsWithoutMeta(argument)}, so the item-typed constant coerces
 * to {@code MapperS.of(EnumType.VALUE)} and golden always carries the wrap. The fix applies the SHARED
 * {@link com.regnosys.rosetta.generator.java.expression.handlers.HandlerHelper#wrapEnumOperand}
 * (the same {@code MapperS.of(...)} wrap a comparison / set-operation bare-enum operand uses, gated on
 * the producer's enum-constant witness — {@code HandlerHelper.isBareEnumConstant} since PR #611, the
 * rendered-text {@code isDottedEnumConstant} before it) at the {@code ConversionHandler.handle(RToStringExpr)}
 * source seat.
 *
 * <p>GREEN-SAFE BY CONSTRUCTION (corpus-verified, frozen 9.83.0 baseline, all 5 cells): ZERO golden
 * leaves a bare dotted enum constant before {@code .map("to-string")} — every enum-constant to-string
 * source is {@code MapperS.of(...)}-wrapped (the bare form does not compile) — so the rewrite only ever
 * touches currently-waivered output. A navigation / chained-Mapper source carries no enum-constant
 * witness, so the wrap declines it (no double-wrap) — locked GREEN by
 * {@link #translateIndexNameToId_chainedToStringSourceUnchanged_byteMatchesGolden}.
 *
 * <p>Anchors are WHOLE-FILE byte comparisons against the frozen goldens (newline-normalized),
 * generated through the REAL {@link D11CorpusRegressionTest#loadCellCorpusCached}.
 */
class FunctionEnumConstantToStringWrapTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    // DRR's loadCellCorpus requires the transitive CDM 5.38.0 + ISO20022 1.38.0 .rosetta
    // closures (it AssertionError-throws if they are absent), so the availability predicate
    // must require those dependency source dirs too — otherwise a partial test-corpus checkout
    // would ENABLE the tests and then fail in @BeforeAll instead of skipping (Copilot R1 #244;
    // mirrors FunctionToStringEnumSourceTest's predicate).
    private static final Path CDM5_DEP_ROSETTA_DIR =
            Path.of("../test-corpus/cdm/cdm-5.38.0/rosetta-source/src/main/rosetta");
    private static final Path ISO_DEP_ROSETTA_DIR =
            Path.of("../test-corpus/iso20022/iso20022-1.38.0/rosetta-source/src/main/rosetta");

    private static Map<String, String> drrFunctionOutput;

    static boolean drrCellAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR)
                && Files.isDirectory(CDM5_DEP_ROSETTA_DIR)
                && Files.isDirectory(ISO_DEP_ROSETTA_DIR);
    }

    @BeforeAll
    static void generateFunctions() throws IOException {
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

    // -------------------------------------------------------------------------
    // Carriers — bare enum-constant to-string source wrapped in MapperS.of(...)
    // -------------------------------------------------------------------------

    /**
     * Carrier (drr asic): {@code Create_ValuationReport32Choice__1} compares
     * {@code actionType(drrReport)} against the to-string of the literal enum value
     * {@code iso.ActionTypeEnum -> VALU} — golden wraps the constant
     * {@code MapperS.of(ActionTypeEnum.VALU).map("to-string", ActionTypeEnum::toDisplayString)}.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void createValuationReport32Choice1Asic_enumConstantToStringSourceWrapped_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/projection/iso20022/asic/rewrite/valuation/functions/Create_ValuationReport32Choice__1.java");
    }

    /**
     * Carrier (drr mas): the {@code mas} sibling of the asic carrier — the identical
     * {@code iso.ActionTypeEnum -> VALU to-string} source-wrap divergence.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void createValuationReport32Choice1Mas_enumConstantToStringSourceWrapped_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/projection/iso20022/mas/rewrite/valuation/functions/Create_ValuationReport32Choice__1.java");
    }

    // -------------------------------------------------------------------------
    // Green-safety lock — the discriminator's DECLINE path
    // -------------------------------------------------------------------------

    /**
     * GREEN-SAFETY lock: {@code TranslateIndexNameToId} (green) has a to-string whose source is a
     * chained Mapper expression ({@code MapperS.of(replaceAll.evaluate(...)).checkedMap("to-enum", …)})
     * — it carries no producer enum-constant witness ({@code isBareEnumConstant} DECLINES; before PR #611
     * the rendered parentheses failed the dotted-shape test) and the source is NOT
     * wrapped (an enum-typed but non-bare-constant source). Locks that the wrap does not corrupt a
     * green chained-Mapper to-string source into {@code MapperS.of(MapperS.of(…).checkedMap(…))}.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void translateIndexNameToId_chainedToStringSourceUnchanged_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/enrichment/upi/functions/TranslateIndexNameToId.java");
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
                "Generated output must byte-match the golden (newline-normalized) for " + path
                + " — the enumConstantToStringWrap fix (HandlerHelper.wrapEnumOperand at "
                + "ConversionHandler.handle(RToStringExpr)'s source seat) is missing or regressed if reverted.");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
