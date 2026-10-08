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
 * Facet {@code boolean_condition_hoist} — an if-condition that compiles
 * ITEM-typed Boolean (a bare function-call condition: upstream
 * {@code evaluateCall} types calls at the output item type) coerces to the
 * primitive boolean the conditional expects via the upstream
 * {@code TypeCoercionService.convertNullSafe} hoist
 * ({@code declareAsVariable(true, actual.simpleName.toFirstLower, scope)}):
 *
 * <pre>
 * final Boolean _boolean = fn.evaluate(args);
 * if ((_boolean == null ? false : _boolean)) {
 * </pre>
 *
 * replacing the fork's {@code if (MapperS.of(fn.evaluate(args))
 * .getOrDefault(false))} — a shape ZERO goldens carry in STATEMENT position
 * (at a statement-hoist sink; the 16 golden carriers of the form sit in
 * LAMBDA-INTERIOR position — 15 rule-kind reports + one waivered function —
 * outside the gate's reach, since the sink walk stops at lambda boundaries
 * and the rule path never opens a session). WRAPPER-typed conditions
 * (ComparisonResult from exists/areEqual/logical chains, MapperS navigation
 * chains) keep the inline {@code .getOrDefault(false)} byte-exact —
 * upstream's {@code wrapperToItem} special case, the decline gate protecting
 * the 3,918 goldens carrying that form (every green carrier among them). Naming rides the {@code StatementHoistSession}
 * {@code boolean} name group: a singleton group's bare name is a Java keyword
 * and escapes to {@code _boolean} (the upstream {@code GeneratorScope}
 * '_'-prefix loop); n &ge; 2 number {@code boolean0..n-1} — valid identifiers,
 * NEVER underscore-escaped ({@code _boolean0} has zero golden carriers).
 * Placement: a top-level condition's hoist rides the caller-collected
 * condition channel before the whole statement; a hoist-carrying NESTED-ELSE
 * conditional cannot stay in the {@code } else if (} chain (no legal decl
 * slot — upstream's {@code JavaIfThenElseStatement.appendTo} collapses to
 * else-if ONLY for a bare nested if) and restructures to
 * {@code } else { <decl>; if (…) { … } }} at one deeper indent, while
 * hoist-free ComparisonResult rungs keep {@code } else if (} chaining INSIDE
 * the same ladder.
 *
 * <p>Green-safety rests on two verified facts: (a) the divergent form
 * {@code if (MapperS.of(<fn>.evaluate(…)).getOrDefault(false))} appears in
 * ZERO goldens in statement position — every golden bare-fn-call condition at
 * a statement-hoist sink hoists; the 698 hoist-shaped golden carriers of
 * {@code final Boolean} decompose as 520 validation/datarule files OUTSIDE
 * the D11-compared population + 80 FUNCTION-kind files ALL waivered pre-facet
 * + 98 POJO-kind rule-family files (CODEGEN_BODY_GAP), and the fork emits
 * {@code final Boolean} in NO channel pre-facet, so there is no green channel
 * to collide with (unlike the #177 BigInteger arg-channel case); and
 * (b) the hoist gate keys on the active statement-hoist sink
 * ({@code FunctionGenerator.compileOperations hoistSessionEligible}), so the
 * rule/report path declines to the pre-facet bytes. The full 5-cell D11
 * matrix (20/20) is the empirical arbiter.
 *
 * <p>Anchors are WHOLE-FILE byte comparisons against the frozen goldens
 * (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}.
 */
class FunctionBooleanConditionHoistTest {

    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static Map<String, String> cdm6FunctionOutput;
    private static Map<String, String> drrFunctionOutput;

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

    /**
     * SINGLETON group, statement position: one hoist in the method — the bare
     * base name 'boolean' is a Java keyword and escapes to {@code _boolean};
     * the decl rides the condition channel immediately before the if.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void productOrUnderlierProductDrr_singletonKeywordEscape_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/regulation/common/functions/ProductOrUnderlierProduct.java");
    }

    /**
     * Second SINGLETON {@code _boolean} carrier (drr side): a bare-input
     * evaluate arg keeps its compiled bytes — only the MapperS.of wrap + the
     * .getOrDefault(false) suffix are replaced by the hoist + guard — and the
     * gen-only derived {@code MapperS} import drops with the wrap (the same
     * import-drop mechanism the MapProductIdType anchor pins on cdm6).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void getTradeForQuantityDrr_singletonBareArgImportDrop_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/standards/iosco/cde/base/quantity/functions/GetTradeForQuantity.java");
    }

    /**
     * DEEP nested-else ladder, n = 8: every hoisted rung's {@code } else if (}
     * collapses to {@code } else { <decl>; if … }} one level deeper, numbering
     * {@code boolean0..boolean7} in registration order through ONE
     * method-spanning group — with hoist-free {@code exists(…)}
     * ComparisonResult rungs interleaved inline in the same ladder.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void interestRateLeg1Drr_nestedElseLadderNumbering_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/regulation/common/functions/InterestRateLeg1.java");
    }

    /**
     * MIXED ladder: hoisted fn-call rungs restructure while the interleaved
     * ComparisonResult rung (an isGenericIRS-or-isEquitySwap {@code orNullSafe}
     * chain — fn-calls in LOGICAL-OPERAND position stay inline per upstream
     * {@code itemToWrapper}) keeps the {@code } else if (….getOrDefault(false))}
     * chain byte-exact — the wrapper decline gate INSIDE one ladder.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void getLeg2ResolvablePriceQuantityDrr_mixedLadderComparisonRungsInline_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/regulation/common/functions/GetLeg2ResolvablePriceQuantity.java");
    }

    /**
     * Ladder nested INSIDE an outer conditional branch: the restructure
     * compounds with the branch indent (decls at the inner statement depth),
     * numbering {@code boolean0..3} across the nested rungs.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void settlementTermsLeg1Drr_ladderInsideOuterBranch_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/regulation/common/functions/SettlementTermsLeg1.java");
    }

    /**
     * cdm6 cell ladder whose gen-only {@code MapperS} import existed ONLY for
     * the condition wraps: the hoist collects refs from the INNER call (the
     * wrap's MAPPER_S ref drops atomically), so the derived import disappears
     * with the fix — import collection closes with the body bytes.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapProductIdTypeCdm6_ladderDerivedImportDrop_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/common/functions/MapProductIdType.java");
    }

    /**
     * cdm6 multi-arg evaluate ladder ({@code stringContains.evaluate(scheme,
     * "literal")}): the hoisted RHS carries the full compiled arg list
     * byte-exact, numbering {@code boolean0..2}.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapTaxonomySourceEnumCdm6_multiArgEvaluateLadder_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/common/functions/MapTaxonomySourceEnum.java");
    }

    private static void assertByteMatchesGolden(Map<String, String> output, Path goldenDir,
            String path) throws IOException {
        assertNotNull(output, "Function generation did not run — corpus unavailable?");
        String generated = output.get(path);
        assertNotNull(generated, "Function not generated: " + path);
        Path goldenPath = goldenDir.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated output must byte-match the golden (newline-normalized) for "
                + path + " — the item-Boolean condition statement hoist + null-guard "
                + "ternary (or its nested-else ladder restructure / boolean name-group "
                + "numbering) is missing or regressed, if the boolean_condition_hoist "
                + "recovery is reverted.");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n");
    }
}
