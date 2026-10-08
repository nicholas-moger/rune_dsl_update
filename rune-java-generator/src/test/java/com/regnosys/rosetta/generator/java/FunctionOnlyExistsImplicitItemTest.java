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
 * Facet {@code onlyexists_implicit_item} — a CHAIN-LESS bare onlyExists
 * element ({@code foreignExchange only exists} inside an extract lambda)
 * parses with a {@code null} receiverExpression BY DESIGN
 * ({@code AstBuilder.buildOnlyExistsPath}); upstream's
 * {@code caseOnlyExists} ELSE branch renders the IMPLICIT VARIABLE as the
 * shared parent with the item type's full attribute set:
 *
 * <pre>
 * onlyExists(item, Arrays.asList("contractualProduct", …, "basket"), Arrays.asList("foreignExchange"))
 * </pre>
 *
 * The fork's legacy placeholder rendered the leaf name as parent + both lists
 * ({@code onlyExists(foreignExchange, asList("foreignExchange"),
 * asList("foreignExchange"))}) — a fingerprint ZERO goldens carry.
 * {@code ExistenceHandler} gains the receiver-less arm (gated
 * {@code !isRootItem() && featureChain().isEmpty()} plus the arm-B2 decline
 * ladder), synthesizing the bare synthetic {@code RImplicitVariable} + the
 * gm-aware item type, falling into the existing emission.
 *
 * <p>Green-safety: zero goldens carry the legacy chain-less fingerprint, and
 * the only TWO {@code onlyExists(item, …} goldens corpus-wide are this
 * facet's carriers (this SOLE flip + the MIXED drr
 * ExtractFinalContractualSettlementDate reach site); the full 5-cell D11
 * matrix (20/20) is the empirical arbiter.
 *
 * <p>The anchor is a WHOLE-FILE byte comparison against the frozen golden
 * (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}.
 */
class FunctionOnlyExistsImplicitItemTest {

    private static final Path CDM5_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-5.38.0");
    private static final Path CDM5_GOLDEN_DIR =
            CDM5_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static Map<String, String> cdm5FunctionOutput;

    static boolean cdm5CellAvailable() {
        return Files.isDirectory(CDM5_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM5_GOLDEN_DIR);
    }

    @BeforeAll
    static void generateFunctions() throws IOException {
        if (cdm5CellAvailable()) {
            cdm5FunctionOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("cdm", "5.38.0", CDM5_CELL_ROOT));
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

    /**
     * The SOLE carrier: {@code … extract foreignExchange only exists} inside
     * a basketConstituent (Product-typed) extract lambda renders the implicit
     * item as parent + Product's 8 attributes in upstream order, recovering
     * the golden bytes with NO other divergence in the file (cdm5).
     */
    @Test
    @EnabledIf("cdm5CellAvailable")
    void qualifyForeignExchangeParameterReturnCorrelationCdm5_implicitItemParent_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(
                "cdm/product/qualification/functions/Qualify_ForeignExchange_ParameterReturnCorrelation.java");
    }

    private static void assertByteMatchesGolden(String path) throws IOException {
        assertNotNull(cdm5FunctionOutput, "Function generation did not run — corpus unavailable?");
        String generated = cdm5FunctionOutput.get(path);
        assertNotNull(generated, "Function not generated: " + path);
        Path goldenPath = CDM5_GOLDEN_DIR.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated output must byte-match the golden (newline-normalized) for "
                + path + " — the chain-less onlyExists implicit-item parent is missing "
                + "or regressed, if the onlyexists_implicit_item recovery is reverted.");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n");
    }
}
