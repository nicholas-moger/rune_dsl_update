package com.regnosys.rosetta.symbols.linker;

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.RRootElement;
import com.regnosys.rosetta.symbols.RNamespaceScope;
import com.regnosys.rosetta.symbols.diagnostics.Diagnostics;

import java.lang.reflect.Method;
import java.util.*;

/**
 * Pass 1 — walks every {@link RModel} and registers every top-level
 * declaration into the workspace symbol table under its declared namespace.
 * Files are walked in stable sorted order (by namespace then file path) so
 * registration is deterministic per D11.4.
 *
 * <p>Spec: D7 (pass 1) + D11 in {@code docs/specs/2026-04-07-m3-symbols-resolution-design.md}.
 */
public final class SymbolRegistrationPass implements LinkerPass {

    /**
     * Runs pass 1. Returns the populated namespace map.
     */
    public Map<String, RNamespaceScope> run(List<RModel> files, Diagnostics collector) {
        Map<String, RNamespaceScope> namespaces = new TreeMap<>();

        // Stable file ordering: by namespace, then by source file name (D11.4)
        List<RModel> sortedFiles = new ArrayList<>(files);
        sortedFiles.sort(Comparator
            .comparing((RModel m) -> m.namespace() == null ? "" : m.namespace())
            .thenComparing(m -> m.sourceRange().file()));

        for (RModel file : sortedFiles) {
            String nsName = file.namespace();
            if (nsName == null || nsName.isEmpty()) continue;

            RNamespaceScope ns = namespaces.computeIfAbsent(nsName, RNamespaceScope::new);
            ns.addContributingFile(file);

            for (RRootElement decl : file.rootElements()) {
                String localName = nameOf(decl);
                if (localName != null) {
                    ns.register(localName, decl);
                }
            }
        }
        return namespaces;
    }

    /**
     * Resolves the local name of a top-level declaration via reflection.
     * Every concrete RRootElement subclass exposes a {@code name()} method.
     * Some subclasses (e.g. RImport, RQualifiableConfig) do not have local
     * names — they are not registered.
     */
    private static String nameOf(RRootElement decl) {
        try {
            Method m = decl.getClass().getMethod("name");
            Object result = m.invoke(decl);
            return result == null ? null : result.toString();
        } catch (NoSuchMethodException e) {
            return null;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                "failed to read name() on " + decl.getClass().getSimpleName(), e);
        }
    }
}
