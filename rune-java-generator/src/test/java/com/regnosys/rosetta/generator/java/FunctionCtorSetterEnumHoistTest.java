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
 * PR #202 (facet {@code ctorSetterEnum} — the joint bare-enum-resolution + #181 ctor-setter
 * ite-hoist for a cdm6 constructor-setter conditional whose then-arm is a bare enum value).
 * A {@code <Type> { <attr>: if .. exists then <BareEnum> }} carrier needs BOTH:
 * <ul>
 *   <li>PARSER (TypeInferenceEngine Category 13d, {@code enclosingCtorAttributeEnumForDirectValue}):
 *       seed the expected enum from the constructor-pair attribute's enum type so the bare
 *       {@code RSymbolReference} ({@code Physical}, {@code InterestRate}, …) binds to its
 *       {@code REnumValue} via the existing idempotent {@code bindBareEnumValue} worker.</li>
 *   <li>GENERATOR: (a) {@code ControlFlowHandler.thenItemJavaClass} gains an {@code REnumValue} arm
 *       so the now-bound bare enum types the no-else hoist local ({@code <Enum> ifThenElseResult = null;});
 *       (b) {@code ReferenceHandler}'s REnumValue render branch collects the enum import
 *       ({@code enumImportRefs}) — at this ctor-setter seat no sibling contributes an ambient import.</li>
 * </ul>
 *
 * <p>Result: the fork's inline ternary {@code .set<Attr>(cond.getOrDefault(false) ? MapperS.of(<bare>)
 * : MapperC.of().get())} becomes the golden hoist {@code <Enum> ifThenElseResult = null; if
 * (cond.getOrDefault(false)) { ifThenElseResult = <Enum>.<CONST>; } … .set<Attr>(ifThenElseResult)}
 * with the {@code MapperC} import dropped and the enum import added.
 *
 * <p>Green-safe by construction: ZERO goldens carry the {@code getOrDefault(false) ? MapperS.of(}
 * ternary OR the unbound {@code MapperS.of(<UppercaseEnum>)} form (both fork-only, non-compiling), so
 * every carrier was already waivered; the parser bind is symbol-empty + name-matched + expected-enum-gated
 * (a real variable/attribute always wins).
 *
 * <p>Whole-file byte anchors through the REAL D11 loader (transitive rune-fpml dep per PR #184), one
 * per enum shape:
 * <ul>
 *   <li>{@code MapCommoditySwaptionSettlementTerms} — SettlementTypeEnum.PHYSICAL.</li>
 *   <li>{@code MapCommodityExerciseToSettlementTerms} — SettlementTypeEnum.CASH.</li>
 *   <li>{@code MapInitialRate} — PriceTypeEnum.INTEREST_RATE.</li>
 *   <li>{@code MapVolatilitySwapReturnTerms} — PriceTypeEnum.VOLATILITY.</li>
 *   <li>{@code MapCalculationAgent} — AncillaryRoleEnum.CALCULATION_AGENT_INDEPENDENT.</li>
 * </ul>
 */
class FunctionCtorSetterEnumHoistTest {

    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR = CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static Map<String, String> cdm6FunctionOutput;

    static boolean cellsAvailable() {
        return Files.isDirectory(CDM6_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM6_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (cellsAvailable()) {
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

    @Test
    @EnabledIf("cellsAvailable")
    void mapCommoditySwaptionSettlementTerms_cdm6_physical_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/settlement/functions/MapCommoditySwaptionSettlementTerms.java");
    }

    @Test
    @EnabledIf("cellsAvailable")
    void mapCommodityExerciseToSettlementTerms_cdm6_cash_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/settlement/functions/MapCommodityExerciseToSettlementTerms.java");
    }

    @Test
    @EnabledIf("cellsAvailable")
    void mapInitialRate_cdm6_interestRate_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/product/swap/functions/MapInitialRate.java");
    }

    @Test
    @EnabledIf("cellsAvailable")
    void mapVolatilitySwapReturnTerms_cdm6_volatility_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/product/volatilityswap/functions/MapVolatilitySwapReturnTerms.java");
    }

    @Test
    @EnabledIf("cellsAvailable")
    void mapCalculationAgent_cdm6_ancillaryRole_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/tradestate/functions/MapCalculationAgent.java");
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
                + " — the unbound `MapperS.of(<BareEnum>)` inline ternary reappears if the ctorSetterEnum "
                + "fix (TypeInferenceEngine.enclosingCtorAttributeEnumForDirectValue Cat 13d + "
                + "ControlFlowHandler.thenItemJavaClass REnumValue arm + ReferenceHandler enumImportRefs) "
                + "is reverted.");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
