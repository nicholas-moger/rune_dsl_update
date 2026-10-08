package com.regnosys.rosetta.symbols.linker;

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.ast.model.RImport;
import com.regnosys.rosetta.symbols.ImportEntry;
import com.regnosys.rosetta.symbols.RFileScope;
import com.regnosys.rosetta.symbols.RNamespaceScope;
import com.regnosys.rosetta.symbols.diagnostics.DiagnosticCategory;
import com.regnosys.rosetta.symbols.diagnostics.Diagnostics;
import com.regnosys.rosetta.symbols.index.DependencyIndex;

import java.util.*;

/**
 * Pass 2 — for each {@link RModel}, builds an {@link RFileScope} wrapping
 * the workspace symbol table with the file's resolved imports.
 *
 * <p>Spec: D7 (pass 2) + D8 in {@code docs/specs/2026-04-07-m3-symbols-resolution-design.md}.
 */
public final class ImportResolutionPass implements LinkerPass {

    private static final String BUILTIN_NAMESPACE = "com.rosetta.model";

    public Map<RModel, RFileScope> run(
            List<RModel> files,
            Map<String, RNamespaceScope> namespaces,
            DependencyIndex dependencyIndex,
            Diagnostics collector) {
        return run(files, namespaces, dependencyIndex, collector,
                com.regnosys.rosetta.symbols.CellPreference.NONE);
    }

    /** PR #445 — the build's entry point, threading the workspace cell preference into every file scope. */
    public Map<RModel, RFileScope> run(
            List<RModel> files,
            Map<String, RNamespaceScope> namespaces,
            DependencyIndex dependencyIndex,
            Diagnostics collector,
            com.regnosys.rosetta.symbols.CellPreference cellPreference) {

        RNamespaceScope builtins = namespaces.get(BUILTIN_NAMESPACE);

        Map<RModel, RFileScope> fileScopes = new IdentityHashMap<>();
        for (RModel file : files) {
            String ownNs = file.namespace();
            RNamespaceScope own = namespaces.get(ownNs);
            if (own == null) {
                own = new RNamespaceScope(ownNs == null ? "" : ownNs);
            }
            dependencyIndex.registerDeclaringNamespace(file, ownNs);

            List<ImportEntry> resolvedImports = new ArrayList<>();
            List<RModel> dependencies = new ArrayList<>();
            for (RImport imp : file.imports()) {
                String qn = imp.qualifiedName();
                Optional<String> alias = imp.alias();
                boolean wildcard = imp.isWildcard();

                // Wildcard imports name the namespace: import cdm.foo.*
                // Named imports name a specific element: import cdm.foo.Bar
                String namespaceName = wildcard ? qn : parentNamespace(qn);
                RNamespaceScope target = namespaces.get(namespaceName);

                if (target == null) {
                    // Phase X1 Gap G1 — a WILDCARD import may legitimately target
                    // a "parent-only" namespace: one with no direct declarations
                    // of its own, only registered sub-namespaces (e.g.
                    // `import drr.base.qualification.*` where only
                    // `drr.base.qualification.event` and `.product` carry
                    // content). Such an import brings every sub-namespace into
                    // scope, so a qualified reference like `event.IsCleared` (the
                    // `event` segment under the imported parent) must resolve via
                    // GlobalResolutionPass#resolveQualifiedOrLocal's sub-namespace
                    // traversal. Register it with a synthetic empty scope so the
                    // traversal (which re-resolves via the global `namespaces`
                    // map) can reach the descendant declarations. The dependency
                    // edges are taken from every contributing sub-namespace file.
                    // Non-wildcard imports still error: a named import to a
                    // parent-only namespace genuinely names nothing.
                    if (wildcard && isParentOfRegisteredNamespace(namespaceName, namespaces)) {
                        RNamespaceScope synthetic = new RNamespaceScope(namespaceName);
                        resolvedImports.add(new ImportEntry(qn, alias, wildcard, synthetic));
                        // Register the dependency against each ACTUAL declaring
                        // sub-namespace, NOT the parent name. The parent
                        // namespace is declared by no file, so keying the reverse
                        // index (DependencyIndex#getDependents) by it would leave
                        // a change to a contributing sub-namespace file unable to
                        // surface this importer as a dependent. Both forward and
                        // reverse edges are keyed by the real declaring
                        // namespaces.
                        String childPrefix = namespaceName + ".";
                        for (Map.Entry<String, RNamespaceScope> e : namespaces.entrySet()) {
                            if (!e.getKey().startsWith(childPrefix)) continue;
                            dependencyIndex.registerImport(file, e.getKey());
                            for (RModel f : e.getValue().contributingFiles()) {
                                if (f != file && !dependencies.contains(f)) dependencies.add(f);
                            }
                        }
                        continue;
                    }
                    SourceRange range = imp.tokenRanges().getOrDefault("name", imp.sourceRange());
                    collector.error(
                        DiagnosticCategory.IMPORT_UNRESOLVED,
                        range,
                        namespaceName,
                        "Imported namespace '" + namespaceName + "' not found",
                        candidatesNear(namespaceName, namespaces.keySet()));
                    continue;
                }

                resolvedImports.add(new ImportEntry(qn, alias, wildcard, target));
                dependencyIndex.registerImport(file, namespaceName);
                for (RModel f : target.contributingFiles()) {
                    if (f != file && !dependencies.contains(f)) dependencies.add(f);
                }
            }

            dependencyIndex.cacheDependencies(file, dependencies);
            fileScopes.put(file, new RFileScope(file, own, resolvedImports, builtins, cellPreference));
        }
        return fileScopes;
    }

    private static String parentNamespace(String qualifiedName) {
        int lastDot = qualifiedName.lastIndexOf('.');
        return lastDot < 0 ? qualifiedName : qualifiedName.substring(0, lastDot);
    }

    /**
     * True iff {@code prefix} is the parent of at least one registered
     * namespace (i.e. some key equals {@code prefix + "."} + a suffix). Used to
     * accept wildcard imports of parent-only namespaces (Phase X1 Gap G1).
     */
    private static boolean isParentOfRegisteredNamespace(
            String prefix, Map<String, RNamespaceScope> namespaces) {
        String childPrefix = prefix + ".";
        for (String key : namespaces.keySet()) {
            if (key.startsWith(childPrefix)) return true;
        }
        return false;
    }

    private static List<String> candidatesNear(String missing, Set<String> known) {
        return known.stream()
            .filter(n -> sharedPrefixLength(n, missing) >= 3)
            .sorted(Comparator.comparingInt((String n) -> -sharedPrefixLength(n, missing)))
            .limit(3)
            .toList();
    }

    private static int sharedPrefixLength(String a, String b) {
        int n = Math.min(a.length(), b.length());
        int i = 0;
        while (i < n && a.charAt(i) == b.charAt(i)) i++;
        return i;
    }
}
