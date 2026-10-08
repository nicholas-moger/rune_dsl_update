package com.regnosys.rosetta.generator.java.types;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.ref.Reference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.supporting.RChoiceOption;
import com.regnosys.rosetta.ast.supporting.RTypeCall;
import com.regnosys.rosetta.ast.types.RChoice;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.symbols.RLinkingResult;
import com.regnosys.rosetta.symbols.RWorkspace;

/**
 * Locks the P2.1.3c defensive guard at {@link RJavaPojoInterface#RJavaPojoInterface(
 * RChoice, GeneratorModel, JavaTypeTranslator, JavaTypeUtil)} that rejects an
 * empty-options choice. Grammar requires ≥1 option (see {@code RChoice} javadoc);
 * the guard defends against parser-bypass or mutation-bypass paths producing a
 * degenerate empty-options choice that would yield a zero-property POJO.
 */
class RJavaPojoInterfaceChoiceGuardTest {

    /**
     * R4 F4-13: builtins search roots — mirrors
     * {@code ChoiceObjectGeneratorTest#BUILTINS_SEARCH_ROOTS} / {@code
     * D11CorpusRegressionTest#BUILTINS_SEARCH_ROOTS}. Required by
     * {@link #single_option_accepted()} so its single option's {@code "string"}
     * RTypeCall resolves against a loaded builtin rather than failing to resolve
     * (or throwing an unrelated NPE/IAE that the test would misattribute to the
     * guard).
     */
    private static final List<Path> BUILTINS_SEARCH_ROOTS = List.of(
            Path.of("../test-corpus/rune-dsl-builtins/rune-runtime/src/main/resources/model"),
            Path.of("../rune-dsl/rune-runtime/src/main/resources/model")
    );

    private final JavaTypeUtil typeUtil = new JavaTypeUtil();
    private final JavaTypeTranslator translator = new JavaTypeTranslator(typeUtil);

    @Test void empty_options_rejected() {
        var model = new RModel();
        model.setNamespace("com.example");
        var ch = new RChoice();
        ch.setName("EmptyChoice");
        ch.setParent(model);
        model.rootElements().add(ch);
        // Lock the AST so options() is sealed at zero entries.
        // Retain RLinkingResult to prevent GC of the WeakReference-held workspace
        // before the assertion runs (mirrors GeneratorModelTest fence pattern).
        RLinkingResult linkingResult = RWorkspace.build(List.of(model));
        var workspace = linkingResult.workspace();
        var gm = new GeneratorModel(workspace);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new RJavaPojoInterface(ch, gm, translator, typeUtil));
        assertTrue(ex.getMessage().contains("EmptyChoice"),
                "Guard message should name the choice: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("at least 1 option"),
                "Guard message should explain the cardinality contract: " + ex.getMessage());

        Reference.reachabilityFence(linkingResult);
    }

    @Test void single_option_accepted() throws IOException {
        var model = new RModel();
        model.setNamespace("com.example");
        var ch = new RChoice();
        ch.setName("OneOption");
        ch.setParent(model);
        var opt = new RChoiceOption();
        var tc = new RTypeCall();
        tc.setTypeName("string");
        opt.setTypeCall(tc);
        ch.options().add(opt);
        model.rootElements().add(ch);
        // R4 F4-13: load builtins so the option's RTypeCall("string") resolves.
        // Without builtins, downstream resolution would either return RMissingType
        // (silently degrading the test) or throw an unrelated exception that would
        // be misattributed to the empty-options guard logic under test.
        var models = new ArrayList<RModel>();
        models.add(model);
        models.addAll(loadBuiltinsOnly());
        // Retain RLinkingResult to prevent GC of the WeakReference-held workspace.
        RLinkingResult linkingResult = RWorkspace.build(models);
        var gm = new GeneratorModel(linkingResult.workspace());

        // No throw — the choice ctor accepts ≥1 option.
        var pojo = new RJavaPojoInterface(ch, gm, translator, typeUtil);
        assertTrue(pojo.isChoiceType());

        Reference.reachabilityFence(linkingResult);
    }

    /**
     * R4 F4-13: union-resolve builtins across {@link #BUILTINS_SEARCH_ROOTS},
     * dedup'd by filename. Fail-fast on any parse exception (mirrors the
     * {@code ChoiceObjectGeneratorTest} pattern). Returns an empty list when no
     * search root exists; callers that need builtins MUST verify they were loaded.
     */
    private static List<RModel> loadBuiltinsOnly() throws IOException {
        Map<String, Path> resolved = new LinkedHashMap<>();
        for (Path root : BUILTINS_SEARCH_ROOTS) {
            if (!Files.isDirectory(root)) continue;
            try (var stream = Files.walk(root)) {
                stream.filter(p -> p.toString().endsWith(".rosetta"))
                      .forEach(p -> resolved.putIfAbsent(p.getFileName().toString(), p));
            }
        }
        List<RModel> models = new ArrayList<>();
        List<String> failures = new ArrayList<>();
        resolved.values().stream()
                .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                .forEach(p -> {
                    try { models.add(AstBuilder.buildFromFile(p)); }
                    catch (Exception e) {
                        failures.add(p + " — " + e);
                    }
                });
        if (!failures.isEmpty()) {
            throw new AssertionError(
                    "[RJavaPojoInterfaceChoiceGuardTest] loadBuiltinsOnly: "
                    + failures.size() + " parse failure(s) — first: " + failures.get(0));
        }
        return models;
    }
}
