package com.regnosys.rosetta.maven;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Arm-for-arm port of the upstream 9.83.0 {@code generators.namespaces} filter
 * ({@code com.regnosys.rosetta.config.file.NamespaceFilter}, vendored tree):
 *
 * <ul>
 *   <li>a pattern whose LAST dot-segment is {@code *} is GENERIC: it matches
 *       any namespace that starts, segment-wise, with the pattern's parent
 *       path — {@code cdm.*} matches {@code cdm}, {@code cdm.base},
 *       {@code cdm.base.staticdata}, ... but NOT {@code cdmx.foo} (upstream
 *       compares {@code DottedPath} segments, never raw string prefixes);</li>
 *   <li>any other pattern is SPECIFIC: exact namespace equality;</li>
 *   <li>an EMPTY pattern list matches everything (upstream's
 *       {@code test(...)} short-circuits to {@code true}).</li>
 * </ul>
 *
 * <p>A {@code null} namespace matches nothing: upstream's filter is only ever
 * consulted with a real model name (the grammar requires a namespace header),
 * and a namespace-less model cannot equal or segment-prefix-match any pattern.
 */
public final class NamespaceFilter implements Predicate<String> {

    private final List<String[]> genericNamespaces = new ArrayList<>();
    private final List<String[]> specificNamespaces = new ArrayList<>();

    public NamespaceFilter(List<String> allowedNamespacePatterns) {
        for (String pattern : allowedNamespacePatterns) {
            if (pattern == null || pattern.isBlank()) {
                throw new IllegalArgumentException(
                        "generators.namespaces entries must be non-blank; got: " + pattern);
            }
            String[] segments = pattern.split("\\.", -1);
            if (segments[segments.length - 1].equals("*")) {
                String[] parent = new String[segments.length - 1];
                System.arraycopy(segments, 0, parent, 0, parent.length);
                genericNamespaces.add(parent);
            } else {
                specificNamespaces.add(segments);
            }
        }
    }

    @Override
    public boolean test(String namespace) {
        if (genericNamespaces.isEmpty() && specificNamespaces.isEmpty()) {
            return true;
        }
        if (namespace == null) {
            return false;
        }
        String[] segments = namespace.split("\\.", -1);
        return genericNamespaces.stream().anyMatch(parent -> startsWithSegments(segments, parent))
                || specificNamespaces.stream().anyMatch(specific -> equalsSegments(segments, specific));
    }

    private static boolean startsWithSegments(String[] segments, String[] prefix) {
        if (segments.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (!segments[i].equals(prefix[i])) {
                return false;
            }
        }
        return true;
    }

    private static boolean equalsSegments(String[] a, String[] b) {
        if (a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (!a[i].equals(b[i])) {
                return false;
            }
        }
        return true;
    }
}
