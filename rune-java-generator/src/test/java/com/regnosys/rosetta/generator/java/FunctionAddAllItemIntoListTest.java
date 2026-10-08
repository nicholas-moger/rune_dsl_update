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
 * PR #199 (facet {@code addAllItemIntoList} — a whole-output ADD whose value is SINGLE-cardinality
 * into a MULTI (List) output coerces the item into a list via the null-guarded if/else BLOCK:
 *
 * <pre>
 * [final &lt;Item&gt; &lt;name&gt; = &lt;value&gt;;]         // hoisted only when &lt;value&gt; is not a bare identifier
 * if (&lt;name&gt; == null) {
 *     &lt;list&gt;.addAll(Collections.&lt;Item&gt;emptyList());
 * } else {
 *     &lt;list&gt;.addAll(Collections.singletonList(&lt;name&gt;));
 * }
 * </pre>
 *
 * <p>The ADD-operation ({@code isAdd}) sibling of PR #198's {@code ctorSingletonListHoist} (the ctor-arg
 * ternary): upstream's {@code TypeCoercionService.convertNullSafe} coerces a single value consumed at a
 * MULTI attribute identically, but a {@code .addAll(...)} is a STATEMENT so the coercion stays an if/else
 * block (mapped distributively over the addAll) rather than collapsing to a ternary argument. The value
 * is hoisted to a {@code final <Item> <name> = <value>;} local (via {@code declareAsVariable}) ONLY when
 * it is not already a bare variable reference — a function-call value hoists ({@code MapNotifyingPartyList}'s
 * {@code mapCounterpartyRoleEnum.evaluate(...)}), a parameter value stays inline ({@code AppendToVector}'s
 * {@code value}).
 *
 * <p>Green-safe by construction: the fork's pre-fix form is {@code <list>.addAll(<single item>)} — a
 * single item passed to {@code List.addAll(Collection)} is a Java compile error, so every carrier was
 * already a waivered, non-compiling mismatch and no green FUNCTION file can carry it. The arm declines
 * (keeps today's bytes) on a segment-carrying ADD, a non-MULTI output, a MULTI (list) value, a meta
 * output, and the rule path (no statement-hoist session open).
 *
 * <p>Whole-file byte anchors through the REAL D11 loader (which loads the transitive rune-fpml dep per
 * PR #184). Anchored:
 * <ul>
 *   <li>{@code MapNotifyingPartyList} (cdm6 creditdefaultswap) — the HOIST sub-case: two whole-output
 *       ADDs of a {@code mapCounterpartyRoleEnum.evaluate(...)} function-call value, each hoisting a
 *       {@code final CounterpartyRoleEnum counterpartyRoleEnum0/1 = ...;} local numbered through the
 *       {@code StatementHoistSession} name group, then the if/else block.</li>
 *   <li>{@code AppendToVector} (cdm6 base/math) — the INLINE sub-case: a single ADD of a bare parameter
 *       value {@code value} (a {@code BigDecimal}); NO hoist, the if/else uses {@code value} directly +
 *       the {@code java.util.Collections} import added.</li>
 * </ul>
 */
class FunctionAddAllItemIntoListTest {

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

    // ---- single value into a MULTI output ADD → hoist (or inline) + addAll(singletonList) null-guard ----

    /** HOIST sub-case: function-call value, two numbered {@code counterpartyRoleEnum0/1} hoists. */
    @Test
    @EnabledIf("cellsAvailable")
    void mapNotifyingPartyList_cdm6_hoistedSingletonAdd_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/product/creditdefaultswap/functions/MapNotifyingPartyList.java");
    }

    /** INLINE sub-case: bare parameter value {@code value}, NO hoist + Collections import added. */
    @Test
    @EnabledIf("cellsAvailable")
    void appendToVector_cdm6_inlineSingletonAdd_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/base/math/functions/AppendToVector.java");
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
                + " — the non-compiling <list>.addAll(<single item>) reappears if the addAllItemIntoList "
                + "fix (FunctionExpressionRenderer.renderOperationInner isAdd branch — hoist a single value "
                + "into a final local + the addAll(Collections.<Item>emptyList())/singletonList if/else "
                + "block) is reverted.");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
