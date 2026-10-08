package com.regnosys.rosetta.symbols;

import com.regnosys.rosetta.ast.RRootElement;

import java.util.Objects;
import java.util.Optional;

/**
 * One resolved import in an {@link RFileScope}. Created by
 * {@link com.regnosys.rosetta.symbols.linker.ImportResolutionPass}
 * for each {@code import} declaration in the file.
 *
 * <p>Supports four import forms per D8:
 * <ul>
 *   <li>{@code import com.foo.*} — wildcard, no alias</li>
 *   <li>{@code import com.foo.Bar} — named, no alias</li>
 *   <li>{@code import com.foo.* as f} — wildcard, aliased</li>
 *   <li>{@code import com.foo.Bar as f} — named, aliased</li>
 * </ul>
 *
 * <p>Spec: D8 in {@code docs/specs/2026-04-07-m3-symbols-resolution-design.md}.
 */
public final class ImportEntry {

    private final String importedNamespace;
    private final Optional<String> alias;
    private final boolean wildcard;
    private final RNamespaceScope target;

    public ImportEntry(
            String importedNamespace,
            Optional<String> alias,
            boolean wildcard,
            RNamespaceScope target) {
        this.importedNamespace = Objects.requireNonNull(importedNamespace);
        this.alias = Objects.requireNonNull(alias);
        this.wildcard = wildcard;
        this.target = Objects.requireNonNull(target);
    }

    public String importedNamespace() { return importedNamespace; }
    public Optional<String> alias() { return alias; }
    public boolean isWildcard() { return wildcard; }
    public RNamespaceScope target() { return target; }

    /**
     * Returns ALL declarations matching this name through this import (vs
     * {@link #lookup} which returns the first). Used by type-position
     * resolution when the first match is a non-type-node (e.g. a rule with
     * the same name as a type in the same namespace).
     */
    public java.util.List<RRootElement> allMatching(String name) {
        if (wildcard) {
            if (alias.isPresent()) {
                String prefix = alias.get() + ".";
                if (!name.startsWith(prefix)) return java.util.List.of();
                return target.allMatching(name.substring(prefix.length()));
            }
            return target.allMatching(name);
        }
        int lastDot = importedNamespace.lastIndexOf('.');
        String localName = lastDot < 0 ? importedNamespace : importedNamespace.substring(lastDot + 1);
        if (alias.isPresent()) {
            return name.equals(alias.get()) ? target.allMatching(localName) : java.util.List.of();
        }
        return name.equals(localName) ? target.allMatching(localName) : java.util.List.of();
    }

    /**
     * Looks up a name through this import. Returns empty if the name is
     * not visible through this import.
     */
    public Optional<RRootElement> lookup(String name) {
        if (wildcard) {
            if (alias.isPresent()) {
                // Aliased wildcard: import com.foo.* as f → lookup "f.Bar" matches "Bar" in target
                String prefix = alias.get() + ".";
                if (!name.startsWith(prefix)) return Optional.empty();
                return target.lookup(name.substring(prefix.length()));
            }
            // Plain wildcard: import com.foo.* → lookup "Bar" matches "Bar" in target
            return target.lookup(name);
        }
        // Named import: import com.foo.Bar or import com.foo.Bar as MyType
        int lastDot = importedNamespace.lastIndexOf('.');
        String localName = lastDot < 0 ? importedNamespace : importedNamespace.substring(lastDot + 1);
        if (alias.isPresent()) {
            // Aliased named: import com.foo.Bar as MyType → lookup "MyType" matches "Bar"
            return name.equals(alias.get()) ? target.lookup(localName) : Optional.empty();
        }
        // Plain named: import com.foo.Bar → lookup "Bar" matches "Bar"
        return name.equals(localName) ? target.lookup(localName) : Optional.empty();
    }
}
