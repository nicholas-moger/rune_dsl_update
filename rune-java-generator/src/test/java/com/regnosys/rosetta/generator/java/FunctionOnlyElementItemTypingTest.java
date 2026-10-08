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
 * Facet {@code onlyelement_item_typing} — three rendering recoveries that the cdm
 * {@code QuantityDecreased}/{@code QuantityIncreased}/{@code QuantityDecreasedToZero}
 * family exposes (census over f-probe-162; the drr
 * {@code SingleTradeLot}/{@code InterestRateLeg1CapFloor}/{@code InterestRateReturnSwap}
 * trio carries mechanism 1 alone on the SET-assignment path):
 *
 * <ol>
 *   <li><b>Only-element bare-value unwrap.</b> An {@code only-element} list-op renders
 *       as the bare terminal collapse {@code <chain>.get()} — already a bare ITEM, not
 *       a Mapper — but its compiled {@link com.regnosys.rosetta.generator.java.statement.builder.JavaExpression}
 *       carried no structural-unwrap marker, so every value-consuming strip site
 *       ({@code ReferenceHandler.unwrapForEvaluateArg},
 *       {@code FunctionExpressionRenderer.unwrapForAssignment},
 *       {@code ConstructionHandler.coerceCtorArg}) fell through to the
 *       {@code .get()}-append arm and emitted the non-compiling DOUBLE unwrap
 *       {@code <chain>.get().get()}. The emission now wraps via
 *       {@link com.regnosys.rosetta.generator.java.statement.builder.JavaExpression#selfUnwrapping},
 *       so the strip sites structurally take the already-bare form — golden's single
 *       {@code .get()}.</li>
 *   <li><b>Implicit-item navigation chain typing.</b> A navigation chain rooted at the
 *       lambda's implicit {@code item} ({@code item -> trade -> tradeLot}) lost the
 *       {@code <Type>} witness AND the {@code mapC} cardinality on every step
 *       ({@code item.map("getTrade", …).map("getTradeLot", …)}), because the gm-aware
 *       {@code NavigationHandler.resolveReceiverDataType} had no
 *       {@code RImplicitVariable} base case — the deliberately-deferred sibling of the
 *       PR #161 lambda-var/attribute-synthesis mechanisms. The base case resolves the
 *       lambda's ITEM data type through the SAME {@code implicitItemDataType} walk
 *       those mechanisms read, cascading witness + {@code map}/{@code mapC} recovery
 *       through the whole chain
 *       ({@code item.<Trade>map("getTrade", …).<TradeLot>mapC("getTradeLot", …)}).</li>
 *   <li><b>Map-body ComparisonResult coercion.</b> An extract/map lambda body that is
 *       a top-level comparison ({@code CompareTradeLot(…) = True}) or logical chain
 *       ({@code … = True and … = True}) compiles to a {@code ComparisonResult} — a
 *       {@code Mapper<Boolean>}, NOT the {@code MapperS} the {@code mapItem} lambda
 *       signature demands — and golden coerces it with {@code .asMapper()}, exactly as
 *       the existing existence-body arm does ({@code CollectionHandler.isExistenceBody}
 *       → {@code appendAsMapper}); the comparison/logical bodies were the missing
 *       arms (the MAPPER_EXPECTING inverse of the PR #139/#140/#161 FILTER_PREDICATE
 *       {@code .get()} arms).</li>
 * </ol>
 *
 * <p>This test is REVERT-VERIFIED RED: reverting any one mechanism reverts its anchors
 * to the divergent shape. Anchors are WHOLE-FILE byte comparisons against the frozen
 * goldens (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}, chosen from the family's
 * waivered files (census over f-probe-162):
 * <ul>
 *   <li>{@code QuantityDecreasedToZero} — mechanism 1 ALONE: the only-element
 *       evaluate-arg renders golden's single {@code .get()};</li>
 *   <li>{@code QuantityDecreased} — mechanisms 1 + 2 + 3 (logical body): typed
 *       implicit-item chain + single {@code .get()} + {@code .asMapper()} on the
 *       {@code andNullSafe} mapItem body;</li>
 *   <li>{@code QuantityIncreased} — mechanisms 1 + 2 + 3 (comparison body): the
 *       single-comparison mapItem body carries the {@code .asMapper()} coercion.</li>
 * </ul>
 */
class FunctionOnlyElementItemTypingTest {

    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static Map<String, String> cdm6FunctionOutput;

    static boolean cdm6CellAvailable() {
        return Files.isDirectory(CDM6_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM6_GOLDEN_DIR);
    }

    @BeforeAll
    static void generateFunctions() throws IOException {
        if (cdm6CellAvailable()) {
            var cell = new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CDM6_CELL_ROOT);
            var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
            var gm = new GeneratorModel(corpus.workspace(),
                    D11CorpusRegressionTest.emissionFilter(cell));
            var typeUtil = new JavaTypeUtil();
            var gen = new FunctionGenerator(gm, new JavaTypeTranslator(typeUtil), typeUtil);
            Map<String, String> output = new LinkedHashMap<>();
            List<GenerationException> genErrors = gen.generateWithErrors(output);
            assertTrue(genErrors.isEmpty(),
                    cell + " FUNCTION generation reported errors: " + genErrors);
            cdm6FunctionOutput = output;
        }
    }

    /** Mechanism 1 alone: the only-element evaluate-arg keeps golden's single .get(). */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void quantityDecreasedToZero_onlyElementSingleGet_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("cdm/event/common/functions/QuantityDecreasedToZero.java");
    }

    /** Mechanisms 1 + 2 + 3 (logical body): typed implicit-item chain + .asMapper(). */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void quantityDecreased_implicitItemChainTyping_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("cdm/event/common/functions/QuantityDecreased.java");
    }

    /** Mechanisms 1 + 2 + 3 (comparison body): single-comparison mapItem body .asMapper(). */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void quantityIncreased_comparisonBodyAsMapper_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("cdm/event/common/functions/QuantityIncreased.java");
    }

    private static void assertByteMatchesGolden(String path) throws IOException {
        assertNotNull(cdm6FunctionOutput, "Function generation did not run — corpus unavailable?");
        String generated = cdm6FunctionOutput.get(path);
        assertNotNull(generated, "Function not generated: " + path);
        Path goldenPath = CDM6_GOLDEN_DIR.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated output must byte-match the golden (newline-normalized) for "
                + path + " — an only-element value consumer appends a doubled .get(), an "
                + "implicit-item navigation chain loses its <Type> witnesses and mapC "
                + "cardinality, and a comparison/logical mapItem body misses the "
                + ".asMapper() coercion if the respective mechanism is reverted.");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
