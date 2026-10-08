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
 * PR #190 (facet {@code convertNullSafe}, slice 4 — the A value&rarr;meta WRAP at a
 * MULTI-cardinality meta leaf) — the continuation of PR #187's {@code convertNullSafe}
 * slice-1 A setter-wrap into the deferred MULTI-cardinality position characterized as
 * {@code strictA} in {@code target/wf187-reversal.json} (local).
 *
 * <p>The law: a bare value SET on a MULTI value-level meta leaf
 * ({@link com.regnosys.rosetta.generator.java.types.RJavaWithMetaValue}
 * {@code ReferenceWithMetaX} / {@code FieldWithMetaX}) null-safe-wraps the single value
 * into a singleton list of the meta builder — exactly the upstream
 * {@code TypeCoercionService.convertNullSafe} + {@code itemToMetaConversionExpression}
 * at LIST arity:
 * <pre>
 *   .setCollateralPortfolio((newCollateralPortfolio == null
 *           ? Collections.&lt;ReferenceWithMetaCollateralPortfolio&gt;emptyList()
 *           : Collections.singletonList(ReferenceWithMetaCollateralPortfolio.builder()
 *                   .setValue(newCollateralPortfolio).build())));
 * </pre>
 * Pre-fix the fork emitted the bare non-compiling {@code .setCollateralPortfolio(newCollateralPortfolio)}
 * (a {@code List&lt;ReferenceWithMetaCollateralPortfolio&gt; = CollateralPortfolio} type
 * mismatch), because PR #187's {@link
 * com.regnosys.rosetta.generator.java.function.FunctionExpressionRenderer}
 * {@code wrapMetaSetterValueOrNull} handled only the SINGLE-cardinality leaf
 * ({@code (v == null ? Meta.builder().build() : Meta.builder().setValue(v).build())}) and
 * declined {@code gm.isMulti(leafAttr)}.
 *
 * <p>Wiring (revert-locked by these whole-file anchors): {@code wrapMetaSetterValueOrNull}
 * now fires for a MULTI leaf when the value is a simple identifier and is itself
 * single-cardinality ({@code multiLeafValue} false — a provably-multi extract value keeps
 * the existing {@code .getMulti()} path and declines the singleton-list wrap), emitting the
 * {@code Collections.singletonList(...)} / {@code Collections.<Meta>emptyList()} ternary and
 * adding the {@code java.util.Collections} import.
 *
 * <p>This is the revert-verified lock: it generates through
 * {@link D11CorpusRegressionTest#loadCellCorpusCached} (the REAL D11 loader) and
 * byte-compares WHOLE FILES against the frozen goldens (newline-normalized —
 * fragment assertions are insufficient per the PR #153 lesson).
 *
 * <p>Anchored: {@code Create_SubstitutionInstruction} in both cdm 5.38.0 and cdm 6.20.6
 * (the {@code setCollateralPortfolio} MULTI {@code ReferenceWithMetaCollateralPortfolio}
 * singleton-list wrap). PR #187 already flipped the single-cardinality A carrier
 * {@code PostedCreditSupportItemAmount}, so it is no longer in the residue.
 *
 * <p>Deferred at #329, PARTIALLY landed at PR #347: the ADD-position gap closed —
 * {@code renderAddSegmentChainOrNull} now carries the facet addSingleMetaSingletonWrap arm
 * (a SINGLE bare-identifier value into a MULTI value-level-META leaf delegates to
 * {@code wrapMetaSetterValueOrNull}'s MULTI-leaf ternary), so the four
 * {@code Resolve{InterestRate,Performance}Reset} carriers healed to their dl=2
 * {@code lambdaParamUS} double-underscore naming-only residual (that NAMING facet stays
 * deferred — the #170 law; the carriers stay waivered on it alone).
 * {@code Enrich_ReportableEventWithUpiFromAnnaDsb} flipped before #347 (its wrap sits at an
 * {@code ifThenElseResultN} list hoist the fork already renders).
 */
class FunctionConvertNullSafeMultiWrapTest {

    private static final Path CDM5_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-5.38.0");
    private static final Path CDM5_GOLDEN_DIR =
            CDM5_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static Map<String, String> cdm5FunctionOutput;
    private static Map<String, String> cdm6FunctionOutput;

    static boolean cellsAvailable() {
        return Files.isDirectory(CDM5_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM5_GOLDEN_DIR)
                && Files.isDirectory(CDM6_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM6_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (cellsAvailable()) {
            cdm5FunctionOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("cdm", "5.38.0", CDM5_CELL_ROOT));
            cdm6FunctionOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CDM6_CELL_ROOT));
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

    // ---- MULTI meta leaf singleton-list wrap (the A-multi slice) ---------------

    /** cdm5: {@code setCollateralPortfolio} MULTI ReferenceWithMetaCollateralPortfolio singleton-list wrap. */
    @Test
    @EnabledIf("cellsAvailable")
    void createSubstitutionInstruction_cdm5_multiMetaSingletonListWrap_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5FunctionOutput, CDM5_GOLDEN_DIR,
                "cdm/event/common/functions/Create_SubstitutionInstruction.java");
    }

    /** cdm6 sibling of the same MULTI-meta singleton-list wrap. */
    @Test
    @EnabledIf("cellsAvailable")
    void createSubstitutionInstruction_cdm6_multiMetaSingletonListWrap_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/event/common/functions/Create_SubstitutionInstruction.java");
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
                + " — the bare non-compiling MULTI meta-wrapper SET reappears if the "
                + "wrapMetaSetterValueOrNull Collections.singletonList multi-leaf arm is reverted.");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
