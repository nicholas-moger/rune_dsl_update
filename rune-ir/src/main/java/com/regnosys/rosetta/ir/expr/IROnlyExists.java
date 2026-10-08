package com.regnosys.rosetta.ir.expr;

import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.types.ExpressionCardinality;
import com.regnosys.rosetta.types.RMetaAnnotatedType;

import java.util.List;

/**
 * An {@code only exists} check root — {@code <path>[, <path>…] only exists} — the #500 arm-B
 * teach of the {@code ROnlyExistsExpr} family (200 events at the #500 census: 197 of 200
 * receiver-expression rooted — 149 single-path + 46 two-path + 2 three-plus — plus 3 named
 * roots). The neutral fact carried:
 * {@link #pathCount()} (the element count — the parent-attribute sibling set legacy's
 * {@code onlyExists} runtime check reads).
 *
 * <p><strong>Deliberately SHALLOW this wave</strong> (the #494 {@link IRConstruct} pattern): the
 * per-path receiver subtrees + feature chains are NOT carried as IR children —
 * {@link #children()} is empty — so no child-position admission allow-list can observe the new
 * kind, and the adapter lowers the node at the claim ROOT only ({@code adaptOnlyExists}'s
 * dispatch arm): the path elements are heterogeneous {@code ROnlyExistsElement} records (root
 * name / implicit-item flag / feature chain / receiver expression), a composite the deep wave
 * models with its own element node — the same trade the construct wave made.
 *
 * <p>At the Java target the ENTIRE render — the 3-arg {@code onlyExists(<receiver>,
 * Arrays.asList(<all>), Arrays.asList(<selected>))} wrap (the deprecated list form is rendered
 * by no generator), the per-path {@code MapperS} chains and the parent-sibling derivation — is a
 * <strong>Java-emission decision</strong> kept off this neutral node (the L-029 split): the
 * emitter delegates to the compiler's range-correlated oracle-root renderer, which calls
 * {@code super.visitOnlyExists(site, ctx)} on the source-range-correlated raw node — the LITERAL
 * legacy fallback call ({@code ExpressionCompiler.visitOnlyExists} is one line:
 * {@code existenceHandler.handle(expr, ctx, this)}), same method, same handler instance, same
 * arguments, so the render is byte-identical to the decline path by the strongest argument.
 *
 * <p>Fork-authored (PR #500); not present upstream.
 */
public record IROnlyExists(
        int pathCount,
        NodeId nodeId,
        RMetaAnnotatedType type,
        ExpressionCardinality cardinality,
        Optionality optionality,
        SourceRange sourceRange) implements IRExpr {

    @Override
    public IRExprKind kind() {
        return IRExprKind.ONLY_EXISTS;
    }

    @Override
    public List<? extends IRExpr> children() {
        return List.of();
    }
}
