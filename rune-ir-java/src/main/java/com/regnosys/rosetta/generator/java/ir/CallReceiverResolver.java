package com.regnosys.rosetta.generator.java.ir;

import com.regnosys.rosetta.ir.expr.IRReference;

/**
 * Resolves the Java {@code @Inject} receiver field name a function CALL ({@link com.regnosys.rosetta.ir.expr.IRApply})
 * needs to render {@code <receiver>.evaluate(...)}, deep in the {@link IRJavaLeafEmitter} recursion where neither the
 * AST node nor the enclosing function is reachable. The neutral {@link IRApply} carries only the callee's simple name
 * (an {@code IRReference} of kind {@link IRReference.ReferenceKind#FUNCTION}); the receiver field name is a
 * Java-emission decision (the lowerCamelCase of the callee's simple name, plus the dependency/shortcut-collision
 * numbering) and is therefore kept OUT of the language-neutral IR — exactly the L-029/L-031 split the
 * {@link AliasOperandResolver} uses for an alias invocation.
 *
 * <p>The compiler ({@link IRExpressionCompiler}) builds an instance per top-level emission (capturing the enclosing
 * function for the collision check) and sets it on the emitter before lowering; a {@code null} resolver, or a
 * {@code null} return for a callee it cannot resolve, makes the call decline to legacy. This keeps the emitter free of
 * any direct dependence on the AST / {@code ExpressionCompiler}, depending only on this focused interface.
 *
 * <p>Lab-authored Phase-2; not present upstream.
 */
@FunctionalInterface
public interface CallReceiverResolver {

    /**
     * The {@code @Inject} receiver field name for a {@code FUNCTION}-callee reference — byte-identical to legacy
     * {@code ReferenceHandler.disambiguateDependencyReceiver}'s full precedence (since the #469 dep-numbering
     * teach): the same-simple-name FUNCTION-dependency collision numbering first (facet injectDepCollision #364,
     * the callee correlated back to its AST call site by the source range the adapter stamps on the reference),
     * then the shortcut-collision {@code "0"} suffix, then the bare lowerCamelCase of the callee's simple name.
     * Returns {@code null} when the receiver cannot be resolved (→ the call declines to legacy).
     */
    String receiverFor(IRReference callee);
}
