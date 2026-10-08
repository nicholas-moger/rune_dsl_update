package com.regnosys.rosetta.generator.java.ir;

import com.regnosys.rosetta.ir.expr.IRReference;

import java.util.List;

/**
 * Resolves the Java-emission facts an {@code ALIAS} reference ({@link IRReference.ReferenceKind#ALIAS}) needs
 * when it appears as an arithmetic OPERAND, deep in the {@link IRJavaLeafEmitter} recursion where neither the
 * AST node nor the enclosing function is reachable. The neutral IR carries only the alias name (and a MISSING
 * type — shortcut type inference is unimplemented upstream); everything below is a compiler-side fact assembled
 * from the enclosing function (mirroring the L-031 split for the top-level alias path: render facts are
 * Java-emission decisions kept out of the language-neutral IR).
 *
 * <p>The compiler ({@link IRExpressionCompiler}) builds an instance per top-level emission (capturing the
 * enclosing function) and sets it on the emitter before lowering; a {@code null} resolver, or {@code null}
 * {@link Facts} for an alias the enclosing function does not declare, makes the alias decline to legacy. This
 * keeps the emitter free of any direct dependence on the AST / {@code ExpressionCompiler}, depending only on
 * this focused interface.
 *
 * <p>Lab-authored Phase-2; not present upstream.
 */
@FunctionalInterface
public interface AliasOperandResolver {

    /**
     * The render + classification facts for an alias reference, or {@code null} when the enclosing function
     * does not declare a shortcut of that name (→ the alias declines to legacy).
     */
    Facts factsFor(IRReference alias);

    /**
     * The Java-emission facts for an alias invocation:
     * <ul>
     *   <li>{@code collidesWithDependency} — whether the alias name collides with a function dependency, so the
     *       invocation takes the {@code "1"} suffix (legacy {@code disambiguateAliasInvocation});</li>
     *   <li>{@code enclosingInputNames} — the enclosing function's input names in declaration order (the alias
     *       helper-method's parameter order);</li>
     *   <li>{@code numericWitness} — the alias's int/number classification for the {@code MapperMaths<R,O,O>}
     *       witness ({@code "Integer"} / {@code "BigDecimal"}, or {@code null} for non-numeric), computed by
     *       recursing the shortcut body through legacy {@code HandlerHelper.numericOperandKind} (the oracle's
     *       own classifier — not a re-implemented type inference).</li>
     * </ul>
     */
    record Facts(boolean collidesWithDependency, List<String> enclosingInputNames, String numericWitness) {}
}
