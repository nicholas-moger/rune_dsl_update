package com.regnosys.rosetta.ast.functions;

import com.regnosys.rosetta.ast.RNode;

import java.util.List;
import java.util.Optional;

/**
 * Recursive path segment node, corresponding to the {@code segment} grammar rule.
 *
 * <p>Segments form a linked chain representing a dotted path (e.g.,
 * {@code foo -> bar -> baz} in an operation target). Each segment
 * holds a name and an optional reference to the next segment.
 *
 * <p>Grammar:
 * <pre>
 * segment:
 *     ARROW validID segment?
 * ;
 * </pre>
 */
public class RSegment extends RNode {

    private String name;
    private RSegment next;

    // -- name -----------------------------------------------------------------

    public String name() {
        return name;
    }

    public void setName(String name) {
        checkMutable();
        this.name = name;
    }

    // -- next -----------------------------------------------------------------

    public Optional<RSegment> next() {
        return Optional.ofNullable(next);
    }

    public void setNext(RSegment next) {
        checkMutable();
        this.next = next;
    }

    // -- children (for traversal) ---------------------------------------------

    @Override
    public List<? extends RNode> children() {
        if (next != null) {
            return List.of(next);
        }
        return List.of();
    }

    // === M4 resolved fields (D9) =============================================

    @com.regnosys.rosetta.symbols.linker.CrossRefField(
        category = com.regnosys.rosetta.symbols.diagnostics.DiagnosticCategory.OPERATION_PATH_NOT_FOUND,
        tokenRangeKey = "name")
    private com.regnosys.rosetta.ast.supporting.RAttribute resolvedAttribute;

    public java.util.Optional<com.regnosys.rosetta.ast.supporting.RAttribute> resolvedAttribute() {
        return java.util.Optional.ofNullable(resolvedAttribute);
    }
    public void setResolvedAttribute(com.regnosys.rosetta.ast.supporting.RAttribute resolved) {
        checkMutable();
        this.resolvedAttribute = resolved;
    }
}
