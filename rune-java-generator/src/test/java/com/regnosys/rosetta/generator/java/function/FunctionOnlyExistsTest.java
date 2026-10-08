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
 * Engine PR (facet {@code onlyexists_path_rendering}) — a two-module (rune-parser +
 * rune-java-generator) fix that renders an {@code only exists} expression like upstream
 * 9.83.0: a parent-navigation {@code Mapper} receiver, the parent type's FULL attribute set
 * (declaration order, super-first) as the {@code allFields} list, and the short leaf names
 * as {@code selectedFields} — replacing the fork's broken placeholder that emits the raw root
 * as the receiver and a fully-qualified dotted path (e.g. {@code "primitiveInstruction.execution"})
 * for BOTH field lists.
 *
 * <p>Upstream algorithm ({@code ExpressionGenerator.xtend} {@code caseOnlyExists}): the first
 * arg's receiver is the shared parent (rendered as {@code MapperS<? extends parentType>}),
 * {@code allAttributes} is the parent type's complete attribute set, and {@code requiredAttributes}
 * is each arg's leaf feature/symbol name (arg order preserved).
 *
 * <p>This test is REVERT-VERIFIED RED against BOTH modules:
 * <ul>
 *   <li>revert the PARSER half (the synthesized receiver wired into
 *       {@code ROnlyExistsElement.children()}) → the receiver's {@code resolvedFeature} /
 *       {@code resolvedSymbol} is never populated → the renderer cannot resolve the parent type
 *       and falls back to the placeholder → the golden {@code MapperS.of(...)} / nav fragment is
 *       absent (positive assertions fail);</li>
 *   <li>revert the RENDERER half ({@code ExistenceHandler.handle(ROnlyExistsExpr)}) → the
 *       FQ-dotted-path placeholder reappears (negative assertions fail).</li>
 * </ul>
 *
 * <p>Determinism follows {@link FunctionMapEnumSwitchTest}: the corpus is the version-frozen
 * {@code cdm/6.20.6} cell, so the generator output is a pure function of the fork's code — this
 * test flips only when the fork's code changes.
 *
 * <p>Anchors (exact golden strings, materialised from
 * {@code test-corpus/cdm/cdm-6.20.6/.../generated/java}):
 * <ul>
 *   <li>SIMPLE {@code NewTradeInstructionOnlyExists} — root is the function parameter
 *       {@code primitiveInstruction} (chain length 1), so the receiver is the bare-mapper
 *       {@code MapperS.of(primitiveInstruction)} and the parent type is
 *       {@code PrimitiveInstruction}. Exercises the multi-element form too
 *       ({@code Arrays.asList("execution", "contractFormation")}).</li>
 *   <li>NAV {@code Qualify_ValuationUpdate} — root is the alias {@code instruction} and the
 *       chain navigates {@code -> primitiveInstruction} before the {@code valuation} leaf, so the
 *       receiver is the full parent navigation
 *       {@code instruction(businessEvent).<PrimitiveInstruction>map("getPrimitiveInstruction", ...)}
 *       (parent type {@code PrimitiveInstruction}). {@code Qualify_ValuationUpdate} is multi-facet,
 *       so only the {@code onlyExists(...)} fragment is asserted, not the whole body.</li>
 * </ul>
 */
class FunctionOnlyExistsTest {

    private static final Path CDM_ROSETTA_DIR =
            Path.of("../test-corpus/cdm/cdm-6.20.6/rosetta-source/src/main/rosetta");
    private static final Path CDM_GOLDEN_DIR =
            Path.of("../test-corpus/cdm/cdm-6.20.6/rosetta-source/src/generated/java");
    private static final Path BUILTINS_DIR = Path.of("../test-corpus/rune-dsl-builtins");

    private static final JavaTypeUtil TYPE_UTIL = new JavaTypeUtil();
    private static final JavaTypeTranslator TYPE_TRANSLATOR = new JavaTypeTranslator(TYPE_UTIL);

    /** The full PrimitiveInstruction attribute list, declaration order (no supertype). */
    private static final String PRIMITIVE_INSTRUCTION_ATTRS =
            "Arrays.asList(\"contractFormation\", \"execution\", \"exercise\", \"partyChange\", "
            + "\"quantityChange\", \"reset\", \"split\", \"termsChange\", \"transfer\", "
            + "\"indexTransition\", \"stockSplit\", \"observation\", \"valuation\")";

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

    /**
     * SIMPLE receiver: root is a function parameter, chain length 1 ({@code primitiveInstruction
     * -> execution}). The receiver renders as the bare {@code MapperS.of(primitiveInstruction)},
     * the parent type is {@code PrimitiveInstruction}, {@code allFields} is its full attribute set,
     * and the leaf names are the selected fields. The two-element form
     * ({@code Arrays.asList("execution", "contractFormation")}) locks the shared-parent + ordered
     * leaf-list behaviour.
     */
    @Test
    @EnabledIf("cdmAvailable")
    void onlyExists_simpleParameterReceiver_rendersMapperSOfWithFullParentAttrs() {
        String body = requireGenerated(
                "cdm/event/common/functions/NewTradeInstructionOnlyExists.java");

        // Single-element: MapperS.of(parameter) receiver + full parent attrs + single leaf.
        assertTrue(body.contains(
                "onlyExists(MapperS.of(primitiveInstruction), " + PRIMITIVE_INSTRUCTION_ATTRS
                + ", Arrays.asList(\"execution\"))"),
                "single-element only-exists must render the bare-mapper receiver "
                + "MapperS.of(primitiveInstruction), the full PrimitiveInstruction attribute set, "
                + "and the short leaf name \"execution\". Body:\n" + body);

        // Multi-element: same shared parent, ordered leaf list ["execution", "contractFormation"].
        assertTrue(body.contains(
                "onlyExists(MapperS.of(primitiveInstruction), " + PRIMITIVE_INSTRUCTION_ATTRS
                + ", Arrays.asList(\"execution\", \"contractFormation\"))"),
                "multi-element only-exists must share the first element's parent and list the "
                + "short leaf names in order [\"execution\", \"contractFormation\"]. Body:\n" + body);

        // Negative: the FQ-dotted-path placeholder must be gone (reappears if the renderer is
        // reverted).
        assertFalse(body.contains("\"primitiveInstruction.execution\""),
                "the fully-qualified dotted-path field name \"primitiveInstruction.execution\" "
                + "must be gone (it reappears if the ExistenceHandler placeholder is restored). "
                + "Body:\n" + body);
        assertFalse(body.contains("onlyExists(primitiveInstruction,"),
                "the raw-root receiver onlyExists(primitiveInstruction, ...) must be gone — the "
                + "receiver is now the MapperS.of(...) wrap. Body:\n" + body);
    }

    /**
     * Alias-root navigation now renders the UPSTREAM nav form (facet
     * lambda_item_body_coercion, arm C): {@code Qualify_ValuationUpdate}'s
     * {@code instruction -> primitiveInstruction -> valuation} (where
     * {@code instruction} is an {@code RShortcut} alias) previously declined to the
     * legacy dotted placeholder because {@code HandlerHelper.resolveValueDataType}
     * filters out {@code RShortcut} symbols; the parent type now resolves through
     * the gm-aware {@code NavigationHandler.resolveReceiverDataType} fallback
     * (alias-body recursion, only-element-transparent, cycle-guarded), and the
     * receiver compiles to the alias METHOD CALL the body machinery already
     * rendered correctly elsewhere. Golden carries this exact form (corpus law:
     * ZERO of 219 golden onlyExists calls carry a dotted path). This is the
     * expectation-flip the previous decline-lock anticipated ("if alias output-type
     * inference is later wired ... this test must be updated to assert the nav
     * form") — resolved generator-side, not via parser type inference.
     */
    @Test
    @EnabledIf("cdmAvailable")
    void onlyExists_aliasRootNavigation_rendersUpstreamNavForm() {
        String body = requireGenerated(
                "cdm/event/qualification/functions/Qualify_ValuationUpdate.java");

        // The upstream 3-arg form: alias-method-call receiver + the parent type's
        // FULL sibling-attribute simple-name list + the leaf simple name.
        assertTrue(body.contains(
                "onlyExists(instruction(businessEvent).<PrimitiveInstruction>map(\"getPrimitiveInstruction\", "
                + "_instruction -> _instruction.getPrimitiveInstruction()), "
                + PRIMITIVE_INSTRUCTION_ATTRS + ", Arrays.asList(\"valuation\"))"),
                "an alias-root only-exists must render the upstream nav form — the alias method "
                + "call receiver with the parent type's simple-name attribute lists. Body:\n" + body);

        // The legacy FQ-dotted-path placeholder must be gone.
        assertFalse(body.contains("\"instruction.primitiveInstruction.valuation\""),
                "the dotted-path placeholder must be gone (it reappears if the gm-aware "
                + "parent-type fallback is reverted). Body:\n" + body);
    }

    // =========================================================================
    // Helpers (mirror FunctionMapEnumSwitchTest)
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
