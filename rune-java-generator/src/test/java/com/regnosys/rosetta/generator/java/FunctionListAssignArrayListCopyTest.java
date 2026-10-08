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
 * PR #200 (facet {@code listAssignArrayListCopy} — a whole-output {@code SET} that assigns a LIST value
 * to a MULTI output LATER MUTATED by an {@code ADD} on that output defensively copies the value into a
 * fresh {@code new ArrayList<>(...)}, so the {@code addAll} does not mutate the source list).
 *
 * <p>THE LAW — mirrors upstream {@code FunctionGenerator.xtend} L416-424:
 * <pre>
 *   needsToCopy = op.ROperationType == SET
 *                 &amp;&amp; effectiveExprType.isList
 *                 &amp;&amp; function.operations.exists[o| o.ROperationType == ADD]
 *   if (needsToCopy) javaExpr = new ArrayList&lt;&gt;(javaExpr)
 * </pre>
 * The fork implements it (in {@code FunctionExpressionRenderer.needsDefensiveListCopy} + the whole-output
 * SET render) as: a non-segment SET whose enclosing function OUTPUT is multi (so the value coerces to the
 * output's {@code List} type — {@code effectiveExprType.isList} for a whole-output SET equals
 * output-is-multi; the DECLARED output cardinality sidesteps the unreliable per-value getCardinality the
 * PR #199 lesson flagged) and the function carries at least one ADD operation. It fires only on the
 * non-builder path (the else of upstream's {@code needsBuilder(attribute)} test), so a model-typed output
 * keeps the {@code toBuilder} path.
 *
 * <p>Green-safe by construction: the fork's bare {@code output = <list>} shares the source reference,
 * which the subsequent {@code addAll} would mutate — a behavioural divergence the golden never carries,
 * so every carrier was already a waivered mismatch and no green FUNCTION file can carry it.
 *
 * <p>Whole-file byte anchors through the REAL D11 loader. Anchored: {@code AppendDateToList} in BOTH the
 * cdm5 and cdm6 cells (base/datetime) — {@code set newList: origDates} (a bare list-param reference) then
 * {@code add newList: newDate}; the SET renders {@code newList = new ArrayList<>(origDates);} (its
 * {@code addAll} item→list coercion already shipped at PR #199). The heavily co-occupied
 * {@code GetAllBusinessCenters} carries the SAME defensive-copy fix on a chain value
 * ({@code distinct(...).getMulti()}) but stays waivered (other un-shipped diffs), so it is NOT anchored.
 */
class FunctionListAssignArrayListCopyTest {

    private static final Path CDM5_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-5.38.0");
    private static final Path CDM5_GOLDEN_DIR = CDM5_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR = CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");

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

    /** cdm5: {@code set newList: origDates} + {@code add newList: newDate} → {@code newList = new ArrayList<>(origDates);}. */
    @Test
    @EnabledIf("cellsAvailable")
    void appendDateToList_cdm5_defensiveArrayListCopy_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5FunctionOutput, CDM5_GOLDEN_DIR,
                "cdm/base/datetime/functions/AppendDateToList.java");
    }

    /** cdm6: same defensive-copy SET (the cdm6 model carries the identical AppendDateToList definition). */
    @Test
    @EnabledIf("cellsAvailable")
    void appendDateToList_cdm6_defensiveArrayListCopy_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/base/datetime/functions/AppendDateToList.java");
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
                + " — the non-defensive `newList = origDates;` (a shared-reference SET the later addAll "
                + "would mutate) reappears if the listAssignArrayListCopy fix "
                + "(FunctionExpressionRenderer.needsDefensiveListCopy → `new ArrayList<>(<value>)`) is reverted.");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
