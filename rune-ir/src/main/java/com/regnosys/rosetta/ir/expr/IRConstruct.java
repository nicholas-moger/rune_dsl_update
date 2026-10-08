package com.regnosys.rosetta.ir.expr;

import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.types.ExpressionCardinality;
import com.regnosys.rosetta.types.RMetaAnnotatedType;

import java.util.List;

/**
 * A type-construction expression — {@code Type { field: value, ... }} — the #494 teach of the
 * biggest untargeted family (3,501 events at the #493 census). The neutral facts carried:
 * {@link #typeName()} (the constructed type, from the raw type call), {@link #attributeNames()}
 * (the pair keys in source order) and {@link #spread()} (the {@code ...} operator — render-inert
 * in the Java oracle, recorded because a neutral node must state it).
 *
 * <p><strong>Deliberately SHALLOW this wave</strong> (the #492 {@code IRPointFreeApply} pattern):
 * the pair-VALUE subtrees are NOT carried as IR children — {@link #children()} is empty — so no
 * child-position admission allow-list ({@code isSimpleCallArg} / {@code isNavigableReceiver} /
 * the operand gates / the list-element walk) can observe the new kind, and the adapter lowers the
 * node at the claim ROOT only ({@code adaptConstructor}'s root-only gate): a constructor in ANY
 * child position keeps declining exactly as pre-teach, byte-frozen by construction. Full family
 * coverage still follows, because the seam walk roots every constructor at its own
 * {@code visitConstructor} visit (nested constructors re-enter the compiler through the oracle's
 * own per-pair value compiles and claim at their own roots). The DEEP enrichment — typed
 * field-value IR children for the neutral SPI targets — is a named later wave; the #494 probe's
 * {@code ctorVisit.<...>.valuesLowerable} facet sized it (1,213 of 3,501 today, + 2 pair-free,
 * nested-constructor recursion the top first-blocker at 1,070). That wave must ALSO remove or
 * re-prove the compiler's CONSTRUCT-root post-pin-guard exemption: its proof rests on the render
 * being the literal legacy fallback, which an IR-composed deep render forfeits.
 *
 * <p>At the Java target the ENTIRE render — the multi-line typed builder block with its
 * coercion/hoist facet family ({@code condListCoerce}, {@code hoistReorder},
 * {@code ctorSetterNumericNarrowChain}, {@code ctor_nested_builder_indent}, the SET/SET_VALUE
 * setter naming), or legacy's one-line placeholder where the typed block declines — is a
 * <strong>Java-emission decision</strong> kept off this neutral node (the L-029 split): the
 * emitter delegates to the compiler's range-correlated {@code ConstructRenderer}, which calls
 * {@code super.visitConstructor(site, ctx)} on the source-range-correlated raw node — the LITERAL
 * legacy fallback call ({@code ExpressionCompiler.visitConstructor} is one line:
 * {@code constructionHandler.handle(expr, ctx, this)}), same method, same handler instance, same
 * arguments, so the render is byte-identical to the decline path by the strongest argument. A
 * future Python/Rust emitter renders from the deep wave's field-value children.
 *
 * <p>Fork-authored (PR #494); not present upstream.
 */
public record IRConstruct(
        String typeName,
        List<String> attributeNames,
        boolean spread,
        NodeId nodeId,
        RMetaAnnotatedType type,
        ExpressionCardinality cardinality,
        Optionality optionality,
        SourceRange sourceRange) implements IRExpr {

    public IRConstruct {
        attributeNames = List.copyOf(attributeNames);
    }

    @Override
    public IRExprKind kind() {
        return IRExprKind.CONSTRUCT;
    }

    @Override
    public List<? extends IRExpr> children() {
        return List.of();
    }
}
