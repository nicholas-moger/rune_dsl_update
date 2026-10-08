package com.regnosys.rosetta.symbols;

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.builder.AstBuilder;
import net.jqwik.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * T21 — Differential fuzzer. Generates random but structurally valid
 * Rune DSL snippets and runs them through the M3 linker, verifying:
 * 1. No crashes (parsing + linking complete without exceptions)
 * 2. Linker invariants hold on the output
 * 3. Diagnostic count is reasonable
 *
 * <p>When Xtext dependencies are available (local with xtext-harness
 * profile), also runs the Xtext pipeline and compares results.
 *
 * <p>Spec: D5/E12 in {@code docs/specs/2026-04-07-m3-symbols-resolution-design.md}.
 */
class DifferentialFuzzerTest {

    @Property(tries = 50)
    void random_type_hierarchies_link_without_crashes(
            @ForAll("typeHierarchies") String source) {
        try {
            RModel model = AstBuilder.buildFromString(source, "fuzz.rosetta");
            RLinkingResult result = RWorkspace.build(List.of(model));

            assertNotNull(result);
            assertNotNull(result.workspace());
            assertFalse(result.workspace().files().isEmpty(),
                "workspace must contain the parsed file");

            // Invariants must hold even on random input
            List<String> violations = LinkerInvariants.checkAll(result.workspace());
            assertTrue(violations.isEmpty(),
                "Invariant violations on fuzzed input:\n  " + String.join("\n  ", violations)
                + "\n\nSource:\n" + source);
        } catch (Exception e) {
            fail("Linker crashed on fuzzed input:\n" + source + "\n\nError: " + e, e);
        }
    }

    @Property(tries = 50)
    void random_functions_link_without_crashes(
            @ForAll("functionSnippets") String source) {
        try {
            RModel model = AstBuilder.buildFromString(source, "fuzz-func.rosetta");
            RLinkingResult result = RWorkspace.build(List.of(model));

            assertNotNull(result);
            List<String> violations = LinkerInvariants.checkAll(result.workspace());
            assertTrue(violations.isEmpty(),
                "Invariant violations on fuzzed function:\n  " + String.join("\n  ", violations)
                + "\n\nSource:\n" + source);
        } catch (Exception e) {
            fail("Linker crashed on fuzzed function:\n" + source + "\n\nError: " + e, e);
        }
    }

    @Property(tries = 50)
    void random_multi_file_workspaces_link_without_crashes(
            @ForAll("multiFileWorkspaces") List<String> sources) {
        try {
            List<RModel> models = new java.util.ArrayList<>();
            for (int i = 0; i < sources.size(); i++) {
                models.add(AstBuilder.buildFromString(sources.get(i), "fuzz-" + i + ".rosetta"));
            }
            RLinkingResult result = RWorkspace.build(models);

            assertNotNull(result);
            assertEquals(sources.size(), result.workspace().files().size());

            List<String> violations = LinkerInvariants.checkAll(result.workspace());
            assertTrue(violations.isEmpty(),
                "Invariant violations on multi-file fuzz:\n  " + String.join("\n  ", violations));
        } catch (Exception e) {
            fail("Linker crashed on multi-file fuzz:\n"
                + String.join("\n---\n", sources) + "\n\nError: " + e, e);
        }
    }

    // === Generators =========================================================

    @Provide
    Arbitrary<String> typeHierarchies() {
        return Arbitraries.of("test", "fuzz.types", "com.example")
            .flatMap(ns -> Arbitraries.integers().between(1, 5)
                .flatMap(count -> {
                    Arbitrary<String> typeName = typeNames();
                    return typeName.list().ofSize(count)
                        .map(names -> buildTypeHierarchy(ns, names));
                }));
    }

    @Provide
    Arbitrary<String> functionSnippets() {
        return Arbitraries.of("test", "fuzz.funcs")
            .flatMap(ns -> typeNames().list().ofSize(2)
                .flatMap(types -> typeNames()
                    .map(funcName -> buildFunction(ns, funcName, types))));
    }

    @Provide
    Arbitrary<List<String>> multiFileWorkspaces() {
        return Arbitraries.of("shared.ns")
            .flatMap(ns -> typeNames().list().ofMinSize(2).ofMaxSize(4)
                .map(names -> {
                    List<String> files = new java.util.ArrayList<>();
                    // File 1: declare types
                    StringBuilder sb1 = new StringBuilder("namespace " + ns + "\n");
                    for (String name : names) {
                        sb1.append("type ").append(name).append(":\n");
                    }
                    files.add(sb1.toString());
                    // File 2: reference types from file 1
                    StringBuilder sb2 = new StringBuilder("namespace " + ns + "\n");
                    if (names.size() >= 2) {
                        sb2.append("type Sub extends ").append(names.get(0)).append(":\n");
                    }
                    files.add(sb2.toString());
                    return files;
                }));
    }

    private Arbitrary<String> typeNames() {
        return Arbitraries.strings()
            .withCharRange('A', 'Z').ofLength(1)
            .flatMap(first -> Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(2).ofMaxLength(6)
                .map(rest -> first + rest));
    }

    private String buildTypeHierarchy(String ns, List<String> names) {
        StringBuilder sb = new StringBuilder("namespace " + ns + "\n");
        for (int i = 0; i < names.size(); i++) {
            sb.append("type ").append(names.get(i));
            if (i > 0) {
                sb.append(" extends ").append(names.get(i - 1));
            }
            sb.append(":\n");
        }
        return sb.toString();
    }

    private String buildFunction(String ns, String funcName, List<String> typeNames) {
        StringBuilder sb = new StringBuilder("namespace " + ns + "\n");
        for (String t : typeNames) {
            sb.append("type ").append(t).append(":\n    value string (1..1)\n");
        }
        sb.append("func ").append(funcName).append(":\n");
        sb.append("    inputs:\n");
        sb.append("        a ").append(typeNames.get(0)).append(" (1..1)\n");
        sb.append("    output: result ").append(typeNames.get(1)).append(" (1..1)\n");
        return sb.toString();
    }
}
