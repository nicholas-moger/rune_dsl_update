package com.regnosys.rosetta.symbols;

import java.util.Objects;

/**
 * Opaque handle to a symbol declared in an {@link RWorkspace}.
 *
 * <p>Replaces direct {@link com.regnosys.rosetta.ast.RNode RNode} pointers as
 * the cross-reference primitive on AST nodes. Resolution is lazy — call
 * {@link RWorkspace#resolve(SymbolId, Class)} to obtain the target node.
 *
 * <p>The {@code generation} token is the workspace's monotonic counter,
 * captured at the time the ID was issued. Mismatched generations on resolve
 * surface as {@link StaleSymbolIdException} so callers cannot accidentally
 * resolve an ID against a re-built workspace.
 *
 * <p>Per audit hook H7 in the development audit "p1-parser-architectural-audit".
 * Manifest entry U005 in {@code docs/upgrades/U005-symbolid-cross-references.md}.
 *
 * @param namespace fully-qualified package name (rune-dsl uses {@code .} separator).
 *                  Empty string for the root namespace; never {@code null}.
 * @param localName simple name of the symbol (last segment of the FQN);
 *                  never {@code null} or blank.
 * @param generation workspace generation token at issue time.
 */
public record SymbolId(String namespace, String localName, long generation) {

    public SymbolId {
        Objects.requireNonNull(namespace, "namespace must not be null; use \"\" for root namespace");
        Objects.requireNonNull(localName, "localName must not be null");
        // namespace: allow empty (root namespace) but reject whitespace-only,
        // which would produce nonsensical fqn() like "   .Foo" and break
        // RWorkspace.resolve lookups silently. The builtin sentinel
        // SymbolResolver.BUILTIN_NAMESPACE ("<builtin>") is non-blank and
        // therefore unaffected.
        if (!namespace.isEmpty() && namespace.isBlank()) {
            throw new IllegalArgumentException(
                    "namespace must be empty (root) or non-blank: \"" + namespace + "\"");
        }
        if (localName.isBlank()) {
            throw new IllegalArgumentException("localName must not be blank: \"" + localName + "\"");
        }
    }

    /**
     * Factory method. Equivalent to invoking the canonical constructor today;
     * provided as the canonical call form so future construction can interpose
     * interning, caching, or namespace normalisation without breaking callers.
     */
    public static SymbolId of(String namespace, String localName, long generation) {
        return new SymbolId(namespace, localName, generation);
    }

    /**
     * Returns the fully-qualified name as {@code namespace + "." + localName},
     * or just {@code localName} when the namespace is empty (root namespace).
     */
    public String fqn() {
        return namespace.isEmpty() ? localName : namespace + "." + localName;
    }
}
