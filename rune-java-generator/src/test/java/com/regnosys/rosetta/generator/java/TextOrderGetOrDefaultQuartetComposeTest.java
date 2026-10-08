package com.regnosys.rosetta.generator.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import static com.regnosys.rosetta.testutil.GenerationErrors.assertNoGenerationErrors;

/**
 * PR #378 — the text-order/getOrDefault-arm quartet (4 byte flips: drr CallQuantity +
 * PutQuantity, cdm6 MapUnitTypeWithScheme + MapEntityIdentifierTypeEnum).
 *
 * <p><b>A condDerefTextOrderUnify</b> (FunctionGenerator / JavaStatementScope /
 * StatementHoistSession): a session method-level hoist group sharing its base name
 * with a SURVIVING deferred coercion entry resolves as ONE group ordered by
 * first-occurrence TEXT position in the unified replay — the #329 documented
 * limitation's fix, its carrier surfaced (upstream's single method scope numbers
 * the guarded cond params and the whole-output deref hoists {@code 0..n-1} in build
 * order: cond-param 0, hoist 1, cond-param 2, hoist 3; the pre-fix split resolved
 * the session group {@code 0/1} and {@code _}-escaped the params).
 *
 * <p><b>B getOrDefaultCondEnumArgHoist</b> (SetOperationHandler + the
 * CollectionHandler base-walk admission, one shared predicate — the #376-J
 * lockstep): a CONDITIONAL {@code default} ARG on a to-enum LEFT hoists as
 * upstream's raw-typed conditional local ({@code FinancialUnitEnum ifThenElseResult
 * = null;} initializer form for the no-value else; blank-final general form
 * otherwise), bare arms qualify against the to-enum TARGET (the tryBareEnumArg
 * unresolved/false-resolution gates + the #348 collision-sentinel law), and the arg
 * collapses to the bare local under the #234 dual-consumer MapperS.of wrap — the
 * alias then-hoist + to-string re-root render FREE through the admission.
 *
 * <p><b>C boolHoistElseRungNest</b> (ControlFlowHandler.appendConditionalChain): an
 * else-RUNG whose condition is a bare fn-call Boolean hoists its own
 * {@code final Boolean booleanN} decl and NESTS the rung block one level deeper
 * (upstream declaration-before-use — the #179 nested-else law at the ladder-rung
 * seat; the session group renumbers {@code boolean0/1}). Zero goldens carry the
 * flat wrapped fn-call rung this replaces (the one {@code } else if (MapperS.of(}
 * golden, NotionalLeg, is a navigation chain the bare-fn-call gate rejects).
 *
 * <p>Whole-file byte locks run through the REAL D11 FUNCTION generation path and
 * revert RED without the facets; every witness token is occurrence-counted (python
 * {@code str.count} semantics — the #352 law) and PRE-counted against
 * f-probe-377post (each removal token PRE &ge; 1 / golden 0; each "golden" token
 * PRE 0 / golden 1; the one forward-guard assert is flagged inline — OBS-1).
 */
class TextOrderGetOrDefaultQuartetComposeTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static final String CALL_QUANTITY =
            "drr/standards/iosco/cde/base/quantity/functions/CallQuantity.java";
    private static final String PUT_QUANTITY =
            "drr/standards/iosco/cde/base/quantity/functions/PutQuantity.java";
    private static final String MAP_UNIT_TYPE_WITH_SCHEME =
            "cdm/ingest/fpml/confirmation/common/functions/MapUnitTypeWithScheme.java";
    private static final String MAP_ENTITY_IDENTIFIER_TYPE_ENUM =
            "cdm/ingest/fpml/confirmation/party/functions/MapEntityIdentifierTypeEnum.java";

    private static Map<String, String> drrFnOutput;
    private static Map<String, String> cdm6FnOutput;

    static boolean drrCellAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR);
    }

    static boolean cdm6CellAvailable() {
        return Files.isDirectory(CDM6_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM6_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (drrCellAvailable()) {
            drrFnOutput = generateFunctions(
                    new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT));
        }
        if (cdm6CellAvailable()) {
            cdm6FnOutput = generateFunctions(
                    new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CDM6_CELL_ROOT));
        }
    }

    /** The REAL D11 FUNCTION-kind generation path (function_comparison). */
    private static Map<String, String> generateFunctions(D11CorpusRegressionTest.CellSpec cell)
            throws IOException {
        var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(),
                D11CorpusRegressionTest.emissionFilter(cell));
        var typeUtil = new JavaTypeUtil();
        var funcGen = new FunctionGenerator(gm, new JavaTypeTranslator(typeUtil), typeUtil);
        Map<String, String> output = new LinkedHashMap<>();
        assertNoGenerationErrors(funcGen.generateWithErrors(output));
        return output;
    }

    // ==== byte locks (all 4 flips through the REAL D11 FUNCTION route) ====

    /** A: the text-order numbering pair. */
    @Test
    @EnabledIf("drrCellAvailable")
    void textOrderPair_byteMatchGolden() throws IOException {
        assertByteMatchesGolden(drrFnOutput, DRR_GOLDEN_DIR, CALL_QUANTITY);
        assertByteMatchesGolden(drrFnOutput, DRR_GOLDEN_DIR, PUT_QUANTITY);
    }

    /** B (+C joint): the getOrDefault conditional-arg hoist pair. */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void getOrDefaultArgPair_byteMatchGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, MAP_UNIT_TYPE_WITH_SCHEME);
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, MAP_ENTITY_IDENTIFIER_TYPE_ENUM);
    }

    // ==== occurrence-counted witnesses (tokens PRE-counted vs f-probe-377post) ====

    /**
     * A: the unified group numbers cond-param/hoist/cond-param/hoist in TEXT order
     * (the golden {@code …Schedule2} cond param PRE 0 / golden 1; the hoist takes
     * {@code …Schedule3} PRE 0 / golden 1) and the {@code _}-escaped params are gone
     * (PRE 6 occurrences / golden 0).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void callQuantity_textOrderNumbering() {
        String gen = generated(drrFnOutput, CALL_QUANTITY);
        assertEquals(0, count(gen, "_referenceWithMetaNonNegativeQuantitySchedule"),
                "the escaped cond params must be gone (PRE 6, golden 0)");
        assertEquals(1, count(gen, "referenceWithMetaNonNegativeQuantitySchedule2 ->"),
                "the second cond param must number 2 in text order (PRE 0, golden 1)");
        assertEquals(1, count(gen,
                "final ReferenceWithMetaNonNegativeQuantitySchedule referenceWithMetaNonNegativeQuantitySchedule3 ="),
                "the second branch hoist must number 3 in text order (PRE 0, golden 1)");
    }

    /**
     * B: the elseless getOrDefault-arg conditional hoists as the null-init raw
     * singleton (PRE 0 / golden 1), the arg collapses to the bare local under the
     * kept MapperS.of wrap (PRE 0 / golden 1), the to-string step re-roots on the
     * hoisted thenArg (PRE 0 / golden 1), and the ternary's MapperC.of() empty-arm
     * import drops (PRE 1 / golden 0).
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapUnitType_getOrDefaultSingletonHoist() {
        String gen = generated(cdm6FnOutput, MAP_UNIT_TYPE_WITH_SCHEME);
        assertEquals(1, count(gen, "FinancialUnitEnum ifThenElseResult = null;"),
                "the null-init raw singleton must land (PRE 0, golden 1)");
        assertEquals(1, count(gen, ".getOrDefault(ifThenElseResult))"),
                "the bare-local arg collapse under the MapperS.of wrap must land (PRE 0, golden 1)");
        assertEquals(1, count(gen,
                "return thenArg.map(\"to-string\", FinancialUnitEnum::toDisplayString);"),
                "the to-string re-root on the hoisted thenArg must land (PRE 0, golden 1)");
        assertEquals(0, count(gen, "import com.rosetta.model.lib.mapper.MapperC;"),
                "the ternary empty-arm MapperC import must drop (PRE 1, golden 0)");
    }

    /**
     * B general form: the elseful ladder hoists blank-final (PRE 0 / golden 1) with
     * the bare-local collapse dropping the {@code .get()} tail (PRE 0 / golden 1)
     * and the qualified arm (PRE 0 / golden 1).
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapEntityIdentifier_blankFinalLadder() {
        String gen = generated(cdm6FnOutput, MAP_ENTITY_IDENTIFIER_TYPE_ENUM);
        assertEquals(1, count(gen, "final EntityIdentifierTypeEnum ifThenElseResult;"),
                "the blank-final elseful decl must land (PRE 0, golden 1)");
        assertEquals(1, count(gen, ".getOrDefault(ifThenElseResult);"),
                "the bare-local collapse (no .get() tail) must land (PRE 0, golden 1)");
        assertEquals(1, count(gen, "EntityIdentifierTypeEnum.REDID"),
                "the qualified bare-enum arm must land (PRE 0, golden 1)");
    }

    /**
     * C: the else-rung bool hoist nests (the boolean1 decl inside the else block —
     * PRE 0 / golden 1) and the second member renumbers the session group
     * (boolean0 PRE 0 / golden 1). The {@code _boolean} count-0 assert is a FORWARD
     * guard only (PRE 0 too — the pre-fix form was the fully inline ternary, no
     * hoist at all; the Seat-1 #378 OBS-1 precision): revert-RED for this method
     * is carried by the boolean0/boolean1 positive witnesses.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapEntityIdentifier_elseRungBoolNest() {
        String gen = generated(cdm6FnOutput, MAP_ENTITY_IDENTIFIER_TYPE_ENUM);
        assertEquals(1, count(gen,
                "final Boolean boolean1 = stringContains.evaluate(productIdScheme, \"iso3166\");"),
                "the nested rung bool decl must land (PRE 0, golden 1)");
        assertEquals(1, count(gen,
                "final Boolean boolean0 = stringContains.evaluate(productIdScheme, \"-id-RED-\");"),
                "the group must renumber boolean0 (PRE 0, golden 1)");
        assertEquals(0, count(gen, "_boolean"),
                "no _boolean singleton escape (PRE 0 — inline ternary pre-fix; forward guard)");
    }

    // ==== helpers ====

    private String generated(Map<String, String> output, String path) {
        assertNotNull(output, "cell output not generated");
        String gen = output.get(path);
        assertNotNull(gen, "missing generated output: " + path);
        return gen;
    }

    /** Occurrence count (python str.count semantics — the #352 law, NOT line count). */
    private static int count(String haystack, String needle) {
        int n = 0;
        int from = 0;
        while ((from = haystack.indexOf(needle, from)) >= 0) {
            n++;
            from += needle.length();
        }
        return n;
    }

    private void assertByteMatchesGolden(Map<String, String> output, Path goldenDir,
            String path) throws IOException {
        String gen = generated(output, path);
        Path goldenPath = goldenDir.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "missing golden: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(gen),
                path + " must byte-match the frozen 9.83.0 golden");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
