package com.regnosys.rosetta.symbols.index;

import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.RRootElement;
import com.regnosys.rosetta.symbols.RNamespaceScope;

import java.util.*;

/**
 * Trigram index over all qualified names in the workspace. Powers
 * {@code RWorkspace.findByName(query)} — exact match first, then fuzzy
 * by trigram overlap.
 *
 * <p>Spec: D6 in {@code docs/specs/2026-04-07-m3-symbols-resolution-design.md}.
 */
public final class NameSearchIndex {

    private final Map<String, List<RNode>> exact = new HashMap<>();
    private final Map<String, Set<String>> trigramToNames = new HashMap<>();

    /** Builds the index from the workspace's namespace map. */
    public static NameSearchIndex build(Map<String, RNamespaceScope> namespaces) {
        NameSearchIndex idx = new NameSearchIndex();
        for (RNamespaceScope ns : namespaces.values()) {
            for (RRootElement decl : ns.allDeclarations()) {
                String localName = nameOf(decl);
                if (localName == null) continue;
                String qualified = ns.qualifiedName() + "." + localName;
                idx.exact.computeIfAbsent(localName, k -> new ArrayList<>()).add(decl);
                idx.exact.computeIfAbsent(qualified, k -> new ArrayList<>()).add(decl);
                for (String tri : trigrams(localName)) {
                    idx.trigramToNames.computeIfAbsent(tri, k -> new HashSet<>()).add(localName);
                }
            }
        }
        return idx;
    }

    /**
     * Looks up a name. Returns exact matches first; if none, returns
     * the top fuzzy matches by trigram overlap (up to 5).
     */
    public List<RNode> findByName(String query) {
        List<RNode> exactHits = exact.get(query);
        if (exactHits != null && !exactHits.isEmpty()) {
            return List.copyOf(exactHits);
        }
        // Fuzzy: rank by trigram overlap with the query
        Set<String> queryTrigrams = trigrams(query);
        Map<String, Integer> overlapByName = new HashMap<>();
        for (String tri : queryTrigrams) {
            Set<String> names = trigramToNames.get(tri);
            if (names == null) continue;
            for (String n : names) {
                overlapByName.merge(n, 1, Integer::sum);
            }
        }
        return overlapByName.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()
                .thenComparing(Map.Entry.comparingByKey()))
            .limit(5)
            .flatMap(e -> exact.getOrDefault(e.getKey(), List.<RNode>of()).stream())
            .toList();
    }

    private static Set<String> trigrams(String s) {
        Set<String> result = new HashSet<>();
        String padded = "  " + s.toLowerCase() + "  ";
        for (int i = 0; i < padded.length() - 2; i++) {
            result.add(padded.substring(i, i + 3));
        }
        return result;
    }

    private static String nameOf(RRootElement decl) {
        try {
            java.lang.reflect.Method m = decl.getClass().getMethod("name");
            Object result = m.invoke(decl);
            return result == null ? null : result.toString();
        } catch (NoSuchMethodException e) {
            return null;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
