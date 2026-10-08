package com.regnosys.rosetta.ir.expr;

import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.types.ExpressionCardinality;
import com.regnosys.rosetta.types.RMetaAnnotatedType;

import java.util.List;

/**
 * A {@code then}-pipe chain root — {@code <arg> then <body>} — the #500 arm-B teach of the
 * {@code RThenExpr} family (582 events at the #500 census, 100% implicit-bare binders). The
 * neutral fact carried: {@link #spineLength()} (the count of {@code RThenExpr} nodes on the
 * left-associative {@code argument()} spine — the {@code thenArg} hoist count the legacy render
 * opens).
 *
 * <p><strong>Deliberately SHALLOW this wave</strong> (the #494 {@link IRConstruct} pattern): the
 * argument/body subtrees are NOT carried as IR children — {@link #children()} is empty — so no
 * child-position admission allow-list can observe the new kind, and the adapter lowers the node
 * at the claim ROOT only ({@code adaptPipe}'s dispatch arm, census-narrow on the implicit-bare
 * binder — the only live form): a {@code then} in any child position keeps declining exactly as
 * pre-teach. The DEEP alternative exists as the SEPARATE Python-side entry point
 * {@code adaptThenChainToLet} (which stays byte-inert on the Java path per its own ⚠ contract —
 * this shallow node does not touch it); the census sized the deep-lowerable slice thin
 * (implicit-bare bare-leaf bodies only), so the shallow claim serves the whole family now and the
 * deep enrichment is a named later wave carrying the same remove-or-re-prove obligation as the
 * construct wave.
 *
 * <p>At the Java target the ENTIRE render — the {@code thenArg} hoist family, the per-link
 * binding and every body compile — is a <strong>Java-emission decision</strong> kept off this
 * neutral node (the L-029 split): the emitter delegates to the compiler's range-correlated
 * oracle-root renderer, which calls {@code super.visitThen(site, ctx)} on the
 * source-range-correlated raw node — the LITERAL legacy fallback call
 * ({@code ExpressionCompiler.visitThen} is one line:
 * {@code collectionHandler.handle(expr, ctx, this)}), same method, same handler instance, same
 * arguments, so the render is byte-identical to the decline path by the strongest argument.
 *
 * <p>Fork-authored (PR #500); not present upstream.
 */
public record IRPipe(
        int spineLength,
        NodeId nodeId,
        RMetaAnnotatedType type,
        ExpressionCardinality cardinality,
        Optionality optionality,
        SourceRange sourceRange) implements IRExpr {

    @Override
    public IRExprKind kind() {
        return IRExprKind.PIPE;
    }

    @Override
    public List<? extends IRExpr> children() {
        return List.of();
    }
}
