package com.regnosys.rosetta.ir.expr;

import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.types.ExpressionCardinality;
import com.regnosys.rosetta.types.RMetaAnnotatedType;

import java.util.List;

/**
 * A CLOSURE-PARAM reference — a bare name naming an enclosing inline function's declared lambda
 * parameter — the #502 arm-A teach of the closure-param family, in BOTH flavors the census read:
 * <ul>
 *   <li>the linker-BOUND named-param reference ({@code symbolNotAttribute} whose resolved symbol
 *       is the declaring {@code RInlineFunction} — {@code extract unitOfAmount [ …
 *       unitOfAmount … ]}; pass 5 registers closure params against the declaring lambda, so the
 *       bind lands on the inline-function node itself);</li>
 *   <li>the legacy re-entrant SYNTHESIS head resolved by name
 *       ({@code symbolUnresolved.synthetic.closureParam} — a render-time receiver piece whose
 *       head resolver binds inputs/output ONLY, so a closure-param head stays symbol-EMPTY by
 *       construction and the adapter resolves it against the registering binder's declared
 *       param names).</li>
 * </ul>
 * The neutral fact carried: {@link #paramName()}. The type/cardinality channels read the node's
 * own engine facts (typed on the parsed bound flavor, MISSING on the synthetic flavor — the
 * cache-boundary class).
 *
 * <p><strong>Deliberately SHALLOW</strong> (the #501 {@link IRSymbolNav} pattern):
 * {@link #children()} is empty — the binder resolution and the scope-live Java variable naming
 * (named extract params, collision-escaped witnesses) are legacy's own render decisions (the
 * L-029 split). The DISTINCT kind is the safety (the #499 law): no native consumer gate admits
 * it silently — the #502 consumer admissions name it explicitly — and the Java emitter routes
 * every containing claim root through the compiler's oracle-root serve (the shared
 * {@code containsOracleLeaf} walk): the root renders the LITERAL legacy line
 * ({@code super.visitX}), so the binder's own scope machinery serves the name byte-identically,
 * while this kind itself has NO leaf-emitter arm (a native compose can never reach it).
 *
 * <p>Optionality is NORMALIZED to {@code PRESENT} (the #500/#501 shallow-kind convention: the
 * seat carries no optionality semantics of its own, and optionality() is serialized — a
 * builder-happenstance split would fork non-Java consumers on nothing).
 *
 * <p>Fork-authored (PR #502); not present upstream.
 */
public record IRClosureParam(
        String paramName,
        NodeId nodeId,
        RMetaAnnotatedType type,
        ExpressionCardinality cardinality,
        Optionality optionality,
        SourceRange sourceRange) implements IRExpr {

    @Override
    public IRExprKind kind() {
        return IRExprKind.CLOSURE_PARAM;
    }

    @Override
    public List<? extends IRExpr> children() {
        return List.of();
    }
}
