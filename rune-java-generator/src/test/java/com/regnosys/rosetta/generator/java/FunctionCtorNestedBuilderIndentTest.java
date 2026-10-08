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
 * Facet {@code ctor_nested_builder_indent} — a RENDERER-ONLY indent fix in
 * {@code ConstructionHandler.tryTypedBuilderBlock}: a NESTED constructor used as a
 * setter argument (the PR #157 deferred NESTED_BUILDER census family, e.g.
 * {@code .setId(GenericIdentification175__1.builder(…))}) already renders through
 * the typed-block recursion with the correct text, but its continuation lines carry
 * the same {@code \n\t} relative indent as the OUTER block's — so every nesting
 * level flattens to {@code indentLevel + 1} where the upstream 9.83.0 golden ladders
 * one tab deeper per level:
 *
 * <pre>
 *   id = toBuilder(PartyIdentification248Choice__1.builder()
 *       .setLgl(LegalPersonIdentification1__1.builder()
 *           .setId(create_OrganisationIdentification15Choice__1.evaluate(lei))
 *           .build())
 *       .build());
 * </pre>
 *
 * <p>Upstream reference (in-tree 9.83.0 {@code ExpressionGenerator.caseConstructorExpression}):
 * the indent falls out of the code-block TREE structure — a nested builder block is a
 * child of its setter line, so its lines sit one level deeper. The fork's fix mirrors
 * that at the embed site: each embedded argument newline gains one tab (a literal
 * {@code String.replace("\n", "\n\t")} — no regex on structured content), and the
 * constructor recursion compounds it naturally for deeper nesting (the f-probe-158
 * census shows golden-vs-fork tab deltas of 1..4 = nesting depth).
 *
 * <p>This test is REVERT-VERIFIED RED: reverting the embed-site indent reverts every
 * anchor to the flattened all-at-one-tab shape. Anchors are WHOLE-FILE byte
 * comparisons against the frozen goldens (newline-normalized), generated through the
 * REAL {@link D11CorpusRegressionTest#loadCellCorpusCached}, chosen one per nesting
 * depth from the 77 sole-mechanism waivered files (all drr/6.34.1):
 * <ul>
 *   <li>depth 1 — asic {@code Create_PartyIdentification248Choice__1} (one nested
 *       builder, two +1-tab lines);</li>
 *   <li>depth 2 — esma emir refit {@code Create_FixedRate} (nested-in-nested,
 *       +1 and +2 tab lines);</li>
 *   <li>depth 3 — asic {@code GetNtnlQty} (three levels under the conditional
 *       SET path);</li>
 *   <li>depth 4 — hkma dtcc {@code Create_PartyIdentification248Choice__4}
 *       (the deepest corpus shape: choice → org-id → other → generic-id).</li>
 * </ul>
 */
class FunctionCtorNestedBuilderIndentTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");
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
            var cell = new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT);
            var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
            var gm = new GeneratorModel(corpus.workspace(),
                    D11CorpusRegressionTest.emissionFilter(cell));
            var typeUtil = new JavaTypeUtil();
            var gen = new FunctionGenerator(gm, new JavaTypeTranslator(typeUtil), typeUtil);
            Map<String, String> output = new LinkedHashMap<>();
            List<GenerationException> genErrors = gen.generateWithErrors(output);
            assertTrue(genErrors.isEmpty(),
                    cell + " FUNCTION generation reported errors: " + genErrors);
            drrFunctionOutput = output;
        }
    }

    /** Depth 1 — one nested builder arg; its two continuation lines sit at +1 tab. */
    @Test
    @EnabledIf("drrCellAvailable")
    void asicCreatePartyIdentification248Choice1_singleNesting_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/projection/iso20022/asic/rewrite/trade/functions/Create_PartyIdentification248Choice__1.java");
    }

    /** Depth 2 — a nested builder inside a nested builder (+1 and +2 tab lines). */
    @Test
    @EnabledIf("drrCellAvailable")
    void esmaCreateFixedRate_doubleNesting_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/projection/iso20022/esma/emir/refit/trade/functions/Create_FixedRate.java");
    }

    /** Depth 3 — three nesting levels under the conditional SET path. */
    @Test
    @EnabledIf("drrCellAvailable")
    void asicGetNtnlQty_tripleNesting_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/projection/iso20022/asic/rewrite/trade/functions/GetNtnlQty.java");
    }

    /** Depth 4 — the deepest corpus shape (choice → org-id → other → generic-id). */
    @Test
    @EnabledIf("drrCellAvailable")
    void hkmaDtccCreatePartyIdentification248Choice4_quadrupleNesting_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/projection/iso20022/hkma/rewrite/trade/dtcc/functions/Create_PartyIdentification248Choice__4.java");
    }

    private static void assertByteMatchesGolden(String path) throws IOException {
        assertNotNull(drrFunctionOutput, "Function generation did not run — corpus unavailable?");
        String generated = drrFunctionOutput.get(path);
        assertNotNull(generated, "Function not generated: " + path);
        Path goldenPath = DRR_GOLDEN_DIR.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated output must byte-match the golden (newline-normalized) for "
                + path + " — nested-builder continuation lines flatten to the outer "
                + "block's indent if the ConstructionHandler embed-site +1-tab indent "
                + "is reverted.");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
