package com.regnosys.rosetta.symbols;

import com.regnosys.rosetta.ast.RRootElement;

import java.util.*;

/**
 * Serializes a workspace's symbol table to deterministic text. Sorted at
 * every level so two builds of the same workspace produce byte-identical
 * output (D11).
 */
public final class SymbolTableSerializer {

    public static String serialize(RWorkspace workspace) {
        StringBuilder sb = new StringBuilder();
        sb.append("# M7b linker + type-inference snapshot\n");
        sb.append("# Files: ").append(workspace.files().size()).append("\n");
        sb.append("# Namespaces: ").append(workspace.namespaces().size()).append("\n");
        sb.append("# Diagnostics: ").append(workspace.linkingDiagnostics().size()).append("\n\n");

        List<RNamespaceScope> namespaces = new ArrayList<>(workspace.namespaces());
        namespaces.sort(Comparator.comparing(RNamespaceScope::qualifiedName));

        for (RNamespaceScope ns : namespaces) {
            sb.append("namespace ").append(ns.qualifiedName()).append("\n");
            Map<String, List<String>> byKind = new TreeMap<>();
            for (RRootElement decl : ns.allDeclarations()) {
                String kind = decl.getClass().getSimpleName();
                String name = nameOf(decl);
                if (name != null) {
                    byKind.computeIfAbsent(kind, k -> new ArrayList<>()).add(name);
                }
            }
            for (Map.Entry<String, List<String>> e : byKind.entrySet()) {
                Collections.sort(e.getValue());
                for (String n : e.getValue()) {
                    sb.append("  ").append(e.getKey()).append(" ").append(n).append("\n");
                }
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private static String nameOf(RRootElement decl) {
        try {
            return decl.getClass().getMethod("name").invoke(decl).toString();
        } catch (Exception e) {
            return null;
        }
    }
}
