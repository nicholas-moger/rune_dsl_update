package com.regnosys.rosetta.ast.expressions.supporting;

import com.regnosys.rosetta.ast.RNode;

/**
 * A declared lambda parameter — upstream's {@code ClosureParameter} node
 * ({@code closureParameter : ID} in the grammar; the {@code x} and {@code y}
 * of {@code items extract x [ x -> sub extract y [ y -> name ] ]}).
 *
 * <p>Until v3.2 seat 8 the fork kept a lambda's parameters as bare names
 * ({@link RInlineFunction#paramNames()}) with no node of their own, so a
 * reference to a closure parameter could only bind to the DECLARING lambda
 * — the {@code RInlineFunction} stood in for upstream's {@code ClosureParameter}
 * everywhere (the resolution spec's § 7 modelling gap, the vendored conformance
 * suite's one frozen exception, and 222 of the chaos cell's 258 L2 rows). This
 * node is the binding target upstream has: a reference to {@code x} resolves to
 * THIS node, whose parent is the lambda that declares it, so the two parameters
 * of {@code reduce a, b [ a + b ]} are two distinct bindings and a qualified
 * name walks the lambda's containers exactly as upstream's does
 * ({@code chaos.s05.a1o3.C5Forms.lambdaIte.x}).
 *
 * <p>The node is a leaf: its source range is the declaring {@code ID} token.
 * It is reached through {@link RInlineFunction#children()} — so parent-wiring,
 * freezing and every {@code children()}-based walk see it — and never through
 * an expression visitor: a parameter declaration is not an expression.
 *
 * <p>The synthetic {@code item} an implicit lambda gains
 * ({@code ImplicitVariableRule}) is upstream's {@code RosettaImplicitVariable},
 * not a closure parameter, and has NO node here: it lives in
 * {@link RInlineFunction#paramNames()} alone.
 */
public class RClosureParameter extends RNode {

    private String name;

    // -- name -----------------------------------------------------------------

    public String name() {
        return name;
    }

    public void setName(String name) {
        checkMutable();
        this.name = name;
    }

    // -- declaring lambda -----------------------------------------------------

    /**
     * The lambda that declares this parameter — its parent once the tree is
     * wired; {@code null} on a detached node.
     */
    public RInlineFunction declaringFunction() {
        return parent() instanceof RInlineFunction fn ? fn : null;
    }
}
