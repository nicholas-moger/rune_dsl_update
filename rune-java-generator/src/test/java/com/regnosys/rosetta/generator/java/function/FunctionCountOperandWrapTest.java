package com.regnosys.rosetta.generator.java.function;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.symbols.RLinkingResult;
import com.regnosys.rosetta.symbols.RWorkspace;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Engine PR #148 (facet {@code count_result_operand_wrap}) — guards that a {@code count}
 * expression used as a comparison operand ({@code <nav> count = N}) is wrapped in
 * {@code MapperS.of(...)} so the comparison sees a Mapper operand, matching the golden
 * {@code areEqual(MapperS.of(<chain>.resultCount()), MapperS.of(N), CardinalityOperator.All)}
 * — not the non-compiling bare-{@code int} form {@code areEqual(<chain>.resultCount(), …)}.
 *
 * <p>Root cause: {@code CollectionHandler.handle(RCountExpr)} returns the count with a
 * {@code null} expression type (it renders the bare {@code int} chain {@code <nav>.resultCount()}),
 * so {@code ExpressionCompiler} skips type coercion; {@code ComparisonHandler} then passes
 * ITEM-typed expected types (Integer/BigDecimal via {@code inferNumericType}), never a
 * {@code MAPPER.wrapExtends(...)} expected type, so the bare {@code int} count operand is
 * never lifted into a {@code MapperS}. Upstream
 * ({@code ExpressionGenerator.caseCountOperation} returns {@code JavaPrimitiveType.INT};
 * the comparison cases compile each operand with expected {@code MAPPER.wrapExtends(...)})
 * lets the coercion service do the wrap. {@code ComparisonHandler.wrapCountOperand} restores
 * it (gated strictly on {@code RCountExpr}, mirroring the sibling {@code wrapEnumOperand} /
 * {@code wrapBareFunctionOperand} operand wraps).
 *
 * <p>Probe: {@code Qualify_Compression} ({@code businessEvent -> instruction -> primitiveInstruction
 * -> execution count = 1 and ... -> quantityChange count = 1}) is a clean SOLE flip — its only
 * residual diff was the missing {@code MapperS.of(...)} wrap around both the {@code areEqual} and
 * {@code greaterThan} count operands. The right-hand literal ({@code MapperS.of(1)}) already wraps
 * via {@code inferNumericType} (count resolves to Integer), so only the count side needs wrapping.
 *
 * <p>Determinism follows {@link FunctionRecordDateNavTest} / {@link FunctionMultiArgGetMultiTest}:
 * the corpus is the version-frozen {@code cdm/6.20.6} cell, so the generator output is a pure
 * function of the fork's code — this test flips only when the generator changes.
 */
class FunctionCountOperandWrapTest {

    private static final Path CDM_ROSETTA_DIR =
            Path.of("../test-corpus/cdm/cdm-6.20.6/rosetta-source/src/main/rosetta");
    private static final Path CDM_GOLDEN_DIR =
            Path.of("../test-corpus/cdm/cdm-6.20.6/rosetta-source/src/generated/java");
    private static final Path BUILTINS_DIR = Path.of("../test-corpus/rune-dsl-builtins");

    private static final JavaTypeUtil TYPE_UTIL = new JavaTypeUtil();
    private static final JavaTypeTranslator TYPE_TRANSLATOR = new JavaTypeTranslator(TYPE_UTIL);

    private static Map<String, String> functionOutput;

    static boolean cdmAvailable() {
        if (!Files.isDirectory(CDM_ROSETTA_DIR) || !Files.isDirectory(CDM_GOLDEN_DIR)) {
            return false;
        }
        try (var stream = Files.walk(CDM_ROSETTA_DIR)) {
            return stream.anyMatch(p -> p.toString().endsWith(".rosetta"));
        } catch (IOException e) {
            return false;
        }
    }

    @BeforeAll
    static void generateAllFunctions() throws IOException {
        if (!cdmAvailable()) return;
        var corpus = loadFullCorpus();
        var gm = new GeneratorModel(corpus.workspace());
        var gen = new FunctionGenerator(gm, TYPE_TRANSLATOR, TYPE_UTIL);
        functionOutput = new LinkedHashMap<>();
        gen.generate(functionOutput);
    }

    @Test
    @EnabledIf("cdmAvailable")
    void countComparisonOperand_wrappedInMapperS_notBareInt() {
        String body = requireGenerated(
                "cdm/event/qualification/functions/Qualify_Compression.java");

        // count = 1 comparison operands must wrap the bare-int .resultCount() chain in
        // MapperS.of(...) so the comparison sees a Mapper operand (golden form).
        assertTrue(body.contains(".resultCount()), MapperS.of(1), CardinalityOperator.All)"),
                "a `count` comparison operand must render wrapped — "
                + "MapperS.of(<chain>.resultCount()) — so the comparison sees a Mapper operand; "
                + "wrapCountOperand did not fire. Body:\n" + body);

        // The non-compiling bare-int form (a primitive int .resultCount() passed straight to
        // areEqual/greaterThan, whose signature takes Mapper operands) must be gone.
        assertFalse(body.contains(".resultCount(), MapperS.of(1), CardinalityOperator.All)"),
                "the bare-int .resultCount() comparison operand (non-compiling against the "
                + "Mapper-operand areEqual/greaterThan signature) must be gone. Body:\n" + body);
    }

    // =========================================================================
    // Helpers (mirror FunctionRecordDateNavTest)
    // =========================================================================

    private static String requireGenerated(String path) {
        assertNotNull(functionOutput,
                "Function generation did not run — CDM corpus unavailable?");
        String generated = functionOutput.get(path);
        assertNotNull(generated, "Function not generated: " + path);
        return generated.replace("\r\n", "\n").replace("\r", "\n");
    }

    private static RLinkingResult loadFullCorpus() throws IOException {
        List<RModel> models = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        if (Files.isDirectory(BUILTINS_DIR)) {
            try (var stream = Files.walk(BUILTINS_DIR)) {
                stream.filter(Files::isRegularFile)
                      .filter(p -> p.toString().endsWith(".rosetta"))
                      .sorted()
                      .forEach(p -> {
                          try { models.add(AstBuilder.buildFromFile(p)); }
                          catch (Exception e) {
                              skipped.add(p + ": " + e.getMessage());
                          }
                      });
            }
        }
        try (var stream = Files.walk(CDM_ROSETTA_DIR)) {
            stream.filter(Files::isRegularFile)
                  .filter(p -> p.toString().endsWith(".rosetta"))
                  .sorted()
                  .forEach(p -> {
                      try {
                          RModel model = AstBuilder.buildFromFile(p);
                          model.setVersion("0.0.0.master-SNAPSHOT");
                          models.add(model);
                      } catch (Exception e) {
                          skipped.add(p + ": " + e.getMessage());
                      }
                  });
        }
        if (!skipped.isEmpty()) {
            throw new IOException(
                    "Corpus loader failed to parse " + skipped.size()
                            + " file(s); refusing to produce partial workspace: "
                            + String.join("; ", skipped));
        }
        return RWorkspace.build(models);
    }
}
