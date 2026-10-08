package com.regnosys.rosetta.symbols.index;

import com.regnosys.rosetta.ast.model.RModel;

import java.util.*;

/**
 * Tracks file-to-file dependency relationships. Maintains the reverse
 * direction (importing files keyed by namespace) explicitly; the forward
 * direction is derived from each file's resolved import entries.
 *
 * <p>Spec: D6 in {@code docs/specs/2026-04-07-m3-symbols-resolution-design.md}.
 */
public final class DependencyIndex {

    /** Reverse index: imported namespace → files that import that namespace. */
    private final Map<String, List<RModel>> importingFilesByNamespace = new HashMap<>();

    /** Forward index: file → namespaces it declares. Used by getDependents. */
    private final Map<RModel, Set<String>> namespacesDeclaredByFile = new IdentityHashMap<>();

    /** Forward dependencies cache: file → files it imports from. */
    private final Map<RModel, List<RModel>> dependenciesByFile = new IdentityHashMap<>();

    public void registerImport(RModel file, String importedNamespace) {
        importingFilesByNamespace
            .computeIfAbsent(importedNamespace, k -> new ArrayList<>())
            .add(file);
    }

    public void registerDeclaringNamespace(RModel file, String declaredNamespace) {
        namespacesDeclaredByFile
            .computeIfAbsent(file, k -> new HashSet<>())
            .add(declaredNamespace);
    }

    public void cacheDependencies(RModel file, List<RModel> dependencies) {
        dependenciesByFile.put(file, List.copyOf(dependencies));
    }

    /** Forward: "what does this file import from?" */
    public List<RModel> getDependencies(RModel file) {
        return dependenciesByFile.getOrDefault(file, List.of());
    }

    /** Reverse: "who imports from this file's namespace?"
     *  Returns a deterministic list sorted by source file path (D11). */
    public List<RModel> getDependents(RModel file) {
        Set<String> namespaces = namespacesDeclaredByFile.getOrDefault(file, Set.of());
        Set<RModel> dependents = Collections.newSetFromMap(new IdentityHashMap<>());
        for (String ns : namespaces) {
            dependents.addAll(importingFilesByNamespace.getOrDefault(ns, List.of()));
        }
        List<RModel> sorted = new ArrayList<>(dependents);
        sorted.sort(Comparator.comparing(m ->
            m.sourceRange() != null ? m.sourceRange().file() : ""));
        return List.copyOf(sorted);
    }
}
