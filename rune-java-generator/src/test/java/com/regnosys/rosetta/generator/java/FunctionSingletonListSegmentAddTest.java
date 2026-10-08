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
 * PR #217 facet {@code singletonListSegmentAdd} — a BARE single-cardinality symbol value
 * (a function input) ADDed to a MULTI segment leaf coerces INLINE to the null-guarded
 * {@code Collections.singletonList} form, no hoist.
 *
 * <p>GEN (pre-fix): {@code .addTradeLot(newTradeLot)} (a single item passed to the multi
 * add-method — a byte divergence vs golden). GOLDEN:
 * {@code .addTradeLot((newTradeLot == null ? Collections.<TradeLot>emptyList() :
 * Collections.singletonList(newTradeLot)))}.
 *
 * <p>This is the bare-symbol sibling of PR #212's {@code isoSingletonSetCoerce} /
 * {@code hoistSingleValueIntoMultiLeafOrNull} (which owns ctor / single-output-fn-call
 * values that HOIST). The fork's {@code renderAddSegmentChainOrNull} now falls through to
 * {@code inlineSingleSymbolIntoMultiLeafOrNull} for a bare {@code RSymbolReference} whose
 * declared cardinality is SINGLE. Green-safe by the cardinality discriminator: a MULTI
 * symbol (e.g. a {@code Party (0..*)} input) is already a List and splices bare — the green
 * {@code .addParty(party)} / {@code .addAccount(account)} carriers; only a SINGLE value
 * coerces. {@code getCardinality} is reliable for a bare symbol (declared param cardinality).
 *
 * <p>Whole-file byte anchors through the REAL D11 loader over all 3 clean carriers:
 * {@code AddTradeLot} ({@code newTradeLot TradeLot (1..1)}) in cdm5 + cdm6, and
 * {@code Create_CashflowTermsChangeInstruction} ({@code cashFlow Cashflow (1..1)}) in cdm5.
 * REVERT-VERIFIED RED: reverting the inline arm reverts these to the bare
 * {@code .addTradeLot(newTradeLot)} / {@code .addCashflow(cashFlow)} form + the dropped
 * {@code java.util.Collections} import.
 */
class FunctionSingletonListSegmentAddTest {

    private static final Path CDM5_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-5.38.0");
    private static final Path CDM5_GOLDEN_DIR = CDM5_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR = CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static Map<String, String> cdm5FunctionOutput;
    private static Map<String, String> cdm6FunctionOutput;

    static boolean cdmCellsAvailable() {
        return Files.isDirectory(CDM5_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM6_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"));
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

    @Test
    @EnabledIf("cdmCellsAvailable")
    void addTradeLot_cdm5_inlineSingletonList_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5FunctionOutput, CDM5_GOLDEN_DIR,
                "cdm/product/template/functions/AddTradeLot.java");
    }

    @Test
    @EnabledIf("cdmCellsAvailable")
    void addTradeLot_cdm6_inlineSingletonList_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/product/template/functions/AddTradeLot.java");
    }

    @Test
    @EnabledIf("cdmCellsAvailable")
    void createCashflowTermsChangeInstruction_cdm5_inlineSingletonList_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5FunctionOutput, CDM5_GOLDEN_DIR,
                "cdm/event/common/functions/Create_CashflowTermsChangeInstruction.java");
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
                + " — a bare single-cardinality symbol ADDed to a MULTI segment leaf must coerce inline "
                + "via (name == null ? Collections.<X>emptyList() : Collections.singletonList(name)); "
                + "reverting inlineSingleSymbolIntoMultiLeafOrNull reverts this anchor to the bare "
                + ".add<Leaf>(<value>) form (RED).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
