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
 * Engine PR #149 (facet {@code map_enum_function}) — a two-module (rune-parser +
 * rune-java-generator) fix that renders a function whose body is a {@code switch} over a
 * basic type (string) into the upstream {@code switchArgument} if-else-if assignment block,
 * with each case RESULT a resolved enum constant.
 *
 * <p>Frozen probe: cdm {@code MapAccountTypeEnum} ({@code set result: value switch
 * "AggregateClient" then AggregateClient, "Client" then Client, "House" then House, default
 * empty}; output {@code AccountTypeEnum}). On current {@code main} the fork emits the
 * triply-broken chained ternary {@code result = Objects.equals("AggregateClient",
 * MapperS.of(value)) ? MapperS.of(AggregateClient) : … : null.get();} —
 * {@code Objects.equals(String, MapperS)} always-false, {@code MapperS.of(AggregateClient)}
 * a BARE UNRESOLVED symbol, trailing {@code null.get()} non-compiling.
 *
 * <p>This test is REVERT-VERIFIED RED against BOTH modules:
 * <ul>
 *   <li>revert the PARSER half (TypeInferenceEngine Cat 15) → the case result stays an
 *       unresolved bare symbol → {@code MapperS.of(AggregateClient)} reappears (negative
 *       assertion fails);</li>
 *   <li>revert the RENDERER half (FunctionExpressionRenderer SET-path switch block) → the
 *       broken ternary {@code Objects.equals(...) ? ... : null.get()} reappears (negative
 *       assertions fail).</li>
 * </ul>
 *
 * <p>Determinism follows {@link FunctionCountOperandWrapTest} / {@link FunctionRecordDateNavTest}:
 * the corpus is the version-frozen {@code cdm/6.20.6} cell, so the generator output is a pure
 * function of the fork's code — this test flips only when the fork's code changes.
 */
class FunctionMapEnumSwitchTest {

    private static final Path CDM_ROSETTA_DIR =
            Path.of("../test-corpus/cdm/cdm-6.20.6/rosetta-source/src/main/rosetta");
    private static final Path CDM_GOLDEN_DIR =
            Path.of("../test-corpus/cdm/cdm-6.20.6/rosetta-source/src/generated/java");
    private static final Path BUILTINS_DIR = Path.of("../test-corpus/rune-dsl-builtins");

    private static final JavaTypeUtil TYPE_UTIL = new JavaTypeUtil();
    private static final JavaTypeTranslator TYPE_TRANSLATOR = new JavaTypeTranslator(TYPE_UTIL);

    /** The exact golden {@code assignOutput} body (CRLF-normalised, tab-indented). */
    private static final String EXPECTED_SWITCH_BLOCK =
            "\t\t\tfinal MapperS<String> switchArgument = MapperS.of(value);\n"
            + "\t\t\tif (switchArgument.get() == null) {\n"
            + "\t\t\t\tresult = null;\n"
            + "\t\t\t} else if (areEqual(switchArgument, MapperS.of(\"AggregateClient\"), CardinalityOperator.All).get()) {\n"
            + "\t\t\t\tresult = AccountTypeEnum.AGGREGATE_CLIENT;\n"
            + "\t\t\t} else if (areEqual(switchArgument, MapperS.of(\"Client\"), CardinalityOperator.All).get()) {\n"
            + "\t\t\t\tresult = AccountTypeEnum.CLIENT;\n"
            + "\t\t\t} else if (areEqual(switchArgument, MapperS.of(\"House\"), CardinalityOperator.All).get()) {\n"
            + "\t\t\t\tresult = AccountTypeEnum.HOUSE;\n"
            + "\t\t\t} else {\n"
            + "\t\t\t\tresult = null;\n"
            + "\t\t\t}";

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
    void mapEnumStringSwitch_rendersSwitchArgumentAssignmentBlock() {
        String body = requireGenerated(
                "cdm/ingest/fpml/confirmation/other/functions/MapAccountTypeEnum.java");

        // RENDERER half: the upstream switchArgument if-else-if block, byte-exact (locks the
        // tab-indent discipline + the areEqual guards + the resolved enum-constant results).
        assertTrue(body.contains(EXPECTED_SWITCH_BLOCK),
                "MapAccountTypeEnum.assignOutput must render the upstream switchArgument "
                + "if-else-if block byte-for-byte. Body:\n" + body);

        // PARSER half: each case result is a RESOLVED enum constant (E.VALUE), not the bare
        // unresolved symbol the fork emits without TypeInferenceEngine Cat 15.
        assertTrue(body.contains("result = AccountTypeEnum.AGGREGATE_CLIENT;"),
                "the case result must resolve to the enum constant AccountTypeEnum.AGGREGATE_CLIENT "
                + "(parser Cat 15). Body:\n" + body);
        assertFalse(body.contains("MapperS.of(AggregateClient)"),
                "the bare UNRESOLVED case-result symbol MapperS.of(AggregateClient) must be gone "
                + "(it reappears if the parser Cat 15 resolution is reverted). Body:\n" + body);

        // RENDERER half (negative): the broken chained ternary must be gone.
        assertFalse(body.contains("Objects.equals("),
                "the broken Objects.equals(String, MapperS) ternary guard must be gone "
                + "(it reappears if the renderer SET-path switch block is reverted). Body:\n" + body);
        assertFalse(body.contains("null.get()"),
                "the non-compiling trailing null.get() must be gone. Body:\n" + body);
    }

    /**
     * Locks the parser TYPE-SHADOW override (Category 15, {@code isTypeSymbol} for an
     * {@code RDataType}). {@code MapAssetClassEnum}'s case results {@code Commodity} and
     * {@code ForeignExchange} collide with cdm DATA TYPES of the same name, so
     * GlobalResolutionPass binds the bare symbol to the type — the bind would be skipped
     * by the strict {@code symbol().isEmpty()} guard and emit {@code MapperS.of(Commodity)}.
     * REVERT-VERIFIED RED against the type-shadow override branch.
     */
    @Test
    @EnabledIf("cdmAvailable")
    void mapEnum_typeShadowedCaseResult_rebindsToOutputEnumValue() {
        String body = requireGenerated(
                "cdm/ingest/fpml/confirmation/other/functions/MapAssetClassEnum.java");
        assertTrue(body.contains("result = AssetClassEnum.COMMODITY;"),
                "the type-shadowed case result `Commodity` (also a cdm data type) must rebind to "
                + "AssetClassEnum.COMMODITY. Body:\n" + body);
        assertTrue(body.contains("result = AssetClassEnum.FOREIGN_EXCHANGE;"),
                "the type-shadowed case result `ForeignExchange` must rebind to "
                + "AssetClassEnum.FOREIGN_EXCHANGE. Body:\n" + body);
        assertFalse(body.contains("MapperS.of(Commodity)") || body.contains("MapperS.of(ForeignExchange)"),
                "the type-shadowed bare-symbol form (MapperS.of(Commodity)/MapperS.of(ForeignExchange)) "
                + "must be gone — it reappears if the type-shadow override is reverted. Body:\n" + body);
    }

    /**
     * Locks the parser CHOICE-SHADOW override (Category 15, {@code isTypeSymbol} for an
     * {@code RChoice}). {@code MapMarginTypeEnum}'s case result {@code Instrument} collides
     * with the cdm {@code choice Instrument}, so the bare symbol binds to the choice type —
     * skipped by the strict guard and the {@code RDataType}-only type set, emitting
     * {@code MapperS.of(Instrument)}. REVERT-VERIFIED RED against the {@code RChoice} branch.
     */
    @Test
    @EnabledIf("cdmAvailable")
    void mapEnum_choiceShadowedCaseResult_rebindsToOutputEnumValue() {
        String body = requireGenerated(
                "cdm/ingest/fpml/confirmation/other/functions/MapMarginTypeEnum.java");
        assertTrue(body.contains("result = MarginTypeEnum.INSTRUMENT;"),
                "the choice-shadowed case result `Instrument` (also a cdm choice type) must rebind "
                + "to MarginTypeEnum.INSTRUMENT. Body:\n" + body);
        assertFalse(body.contains("MapperS.of(Instrument)"),
                "the choice-shadowed bare-symbol form (MapperS.of(Instrument)) must be gone — it "
                + "reappears if RChoice is dropped from isTypeSymbol. Body:\n" + body);
    }

    // =========================================================================
    // Helpers (mirror FunctionCountOperandWrapTest)
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
