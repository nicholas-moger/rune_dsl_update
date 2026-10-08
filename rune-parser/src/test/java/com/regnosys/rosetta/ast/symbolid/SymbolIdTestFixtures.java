package com.regnosys.rosetta.ast.symbolid;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.symbols.RWorkspace;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Shared fixture-loading helper for {@code com.regnosys.rosetta.ast.symbolid} tests.
 *
 * <p>Loads {@code .rosetta} fixture files from
 * {@code rune-parser/src/test/resources/symbolid-fixtures/<subdir>/} and parses
 * them via {@link AstBuilder#buildFromString(String, String)} — the same pattern
 * used by {@code BaseSymbolsTest}.
 *
 * <p>Per plan step 4.1.5 (MF1 fix): uses {@code AstBuilder.buildFromString} directly
 * rather than any file-system parser API, mirroring {@code BaseSymbolsTest.parseModel}.
 */
public final class SymbolIdTestFixtures {

    private SymbolIdTestFixtures() {}

    /**
     * Loads every {@code .rosetta} fixture file under
     * {@code rune-parser/src/test/resources/symbolid-fixtures/<fixtureSubdir>/}
     * and parses each via {@link AstBuilder#buildFromString(String, String)}.
     *
     * <p>Tries the path relative to the Maven module first (for IDE runs), then
     * falls back to the repo-root-relative path (for Maven Surefire). Sorted for
     * deterministic ordering.
     */
    public static List<RModel> parse(String fixtureSubdir) throws Exception {
        // Try module-relative path first (IDE / working-dir = module root)
        Path root = Path.of("src", "test", "resources", "symbolid-fixtures", fixtureSubdir);
        if (!Files.exists(root)) {
            // Fall back to repo-root-relative path (Maven Surefire CWD = repo root)
            root = Path.of("rune-parser", "src", "test", "resources", "symbolid-fixtures", fixtureSubdir);
        }
        List<RModel> models = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(root)) {
            walk.filter(p -> p.toString().endsWith(".rosetta"))
                .sorted()
                .forEach(p -> {
                    try {
                        String content = Files.readString(p);
                        models.add(AstBuilder.buildFromString(content, p.getFileName().toString()));
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to parse fixture: " + p, e);
                    }
                });
        }
        return models;
    }

    /**
     * Finds an {@link RDataType} by fully-qualified name in a built workspace.
     * Uses {@link RWorkspace#namespace(String)} + {@link com.regnosys.rosetta.symbols.RNamespaceScope#lookup(String)}
     * — the same path that {@code RWorkspace.resolve(SymbolId, Class)} traverses.
     *
     * @param ws the built workspace
     * @param fqn fully-qualified name, e.g. {@code "com.test.Child"}
     * @return the resolved RDataType (freeze-attached node)
     * @throws AssertionError if no matching RDataType is found
     */
    public static RDataType findDataType(RWorkspace ws, String fqn) {
        int dot = fqn.lastIndexOf('.');
        String ns = dot < 0 ? "" : fqn.substring(0, dot);
        String name = dot < 0 ? fqn : fqn.substring(dot + 1);
        return ws.namespace(ns)
                .flatMap(n -> n.lookup(name))
                .filter(RDataType.class::isInstance)
                .map(RDataType.class::cast)
                .orElseThrow(() -> new AssertionError(
                        "RDataType " + fqn + " not found in workspace"));
    }
}
