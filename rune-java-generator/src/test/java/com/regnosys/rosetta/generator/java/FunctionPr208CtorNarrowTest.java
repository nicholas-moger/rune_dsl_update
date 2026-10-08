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
 * PR #208 — facet ctorSetterNumericNarrow (7 drr FUNCTION flips), whole-file byte anchors
 * through the REAL D11 loader. Each anchor reverts RED if the fix is removed; the law is
 * green-safe by construction (the pre-fix bare splice never compiled, so every carrier was
 * already waivered — stash-baseline confirmed 0 now-matching pristine).
 *
 * <p><b>The law.</b> A SINGLE-cardinality numeric ctor value whose item type is WIDER than the
 * attribute's bounded-integer Java type (a {@code number} BigDecimal value into an ISO20022
 * {@code int} setter such as {@code setBsisPtSprd}) must narrow via the upstream null-guarded
 * exact conversion {@code (<v> == null ? null : <v>.intValueExact())}. The fork spliced the
 * value BARE ({@code .setBsisPtSprd(spreadOfLeg1Basis)} — a BigDecimal where Integer is
 * expected, non-compiling). Root cause: {@code ConstructionHandler.coerceCtorArg}'s
 * single-attribute path emitted the rendered value verbatim with no expected-type coercion;
 * the {@code ItemToItemCoercer} BigDecimal→Integer narrowing ({@code .intValueExact()}) already
 * existed but was never reached at the setter position. Fix: {@code tryCtorNumericNarrow} —
 * recover the value item type (rune inference for an alias-call value that renders null-typed),
 * gate to a simple-identifier value (the null-guard evaluates it twice; a Mapper-chain value
 * declines, deferred to a hoist facet), and emit the coercer's narrowing wrapped in a null
 * guard. Exemplars: the cross-regime drr {@code Create_FloatingRate*}'s {@code setBsisPtSprd}.
 */
class FunctionPr208CtorNarrowTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR = DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static Map<String, String> drrFunctionOutput;

    static boolean drrAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (drrAvailable()) {
            drrFunctionOutput = generateCell(new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT));
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

    /** asic Create_FloatingRate: setBsisPtSprd(spreadOfLeg1Basis) → null-guarded intValueExact. */
    @Test
    @EnabledIf("drrAvailable")
    void asicCreateFloatingRate_bsisPtSprdNarrow_byteMatchesGolden() throws IOException {
        assertByteMatches("drr/projection/iso20022/asic/rewrite/trade/functions/Create_FloatingRate.java",
                "the `.setBsisPtSprd((spreadOfLeg1Basis == null ? null : spreadOfLeg1Basis.intValueExact()))` "
                + "reverts to the bare `.setBsisPtSprd(spreadOfLeg1Basis)` — tryCtorNumericNarrow removed");
    }

    /** asic Create_FloatingRate2: the Leg2 sibling (setBsisPtSprd(spreadOfLeg2Basis)). */
    @Test
    @EnabledIf("drrAvailable")
    void asicCreateFloatingRate2_bsisPtSprdNarrow_byteMatchesGolden() throws IOException {
        assertByteMatches("drr/projection/iso20022/asic/rewrite/trade/functions/Create_FloatingRate2.java",
                "the BigDecimal→Integer ctor-setter narrowing reverts to the bare value");
    }

    /** esma Create_FloatingRate: the same law in a different regime namespace (cross-regime cluster). */
    @Test
    @EnabledIf("drrAvailable")
    void esmaCreateFloatingRate_bsisPtSprdNarrow_byteMatchesGolden() throws IOException {
        assertByteMatches("drr/projection/iso20022/esma/emir/refit/trade/functions/Create_FloatingRate.java",
                "the cross-regime BigDecimal→Integer ctor-setter narrowing reverts to the bare value");
    }

    /** jfsa Create_FloatingRate13__1: the numbered-variant carrier (different builder type). */
    @Test
    @EnabledIf("drrAvailable")
    void jfsaCreateFloatingRate13_bsisPtSprdNarrow_byteMatchesGolden() throws IOException {
        assertByteMatches("drr/projection/iso20022/jfsa/rewrite/trade/functions/Create_FloatingRate13__1.java",
                "the BigDecimal→Integer ctor-setter narrowing reverts to the bare value");
    }

    private static void assertByteMatches(String path, String revertHint) throws IOException {
        assertNotNull(drrFunctionOutput, "Function generation did not run — corpus unavailable?");
        String generated = drrFunctionOutput.get(path);
        assertNotNull(generated, "Function not generated: " + path);
        Path goldenPath = DRR_GOLDEN_DIR.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated output must byte-match the golden (newline-normalized) for " + path
                + " — " + revertHint + ".");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
