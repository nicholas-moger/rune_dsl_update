package com.regnosys.rosetta.ir.expr;

import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.types.ExpressionCardinality;
import com.regnosys.rosetta.types.RMetaAnnotatedType;

import java.util.List;

/**
 * A bare reference to a META-ANNOTATED function input from inside its own body — the #514
 * {@code metaParam} teach (the #513 parent-facet decode: RDefaultExpr-operand + ROperation
 * statement + RKeyValuePair ctor-value seats, plus the
 * {@code receiverNotExpressible.child.recv:metaParam.*} disguised-nav HEAD slice — the
 * {@code price -> value} chains over a {@code [metadata scheme]}-annotated param). Legacy
 * variable-paths the param name and threads the {@code FieldWithMetaX} wrapper machinery —
 * the value deref on a navigation hop, the wrapper pass-through on a statement assignment —
 * inside its own render lines.
 *
 * <p><strong>Deliberately SHALLOW</strong> (the {@link IRDispatchInputRef} pattern): the
 * param's name ({@link #paramName()}) and its attribute-channel type (the engine's own
 * read — PLAIN for qualifier-only annotations like {@code [metadata scheme]}, the
 * annotation itself being the arm's gate) are the neutral facts; every wrapper coercion is
 * legacy's own (the L-029 split). The DISTINCT kind is the containment (the #499 law): the call-argument
 * seats whose renders need legacy's unwrap coercion are NOT admitted (the decode read the
 * {@code pPlain}/{@code argSeatMiss} slices — those keep declining at the callArgs gate,
 * where the generic {@code arg:<Class>} token now names them), while the admitted seats
 * route every containing claim root through the compiler's oracle-root serve (the shared
 * {@code containsOracleLeaf} walk), byte-identical BY IDENTITY; the kind itself has NO
 * leaf-emitter arm (a native compose can never reach it — the routing safety).
 *
 * <p>Fork-authored (PR #514); not present upstream.
 */
public record IRMetaParamRef(
        String paramName,
        NodeId nodeId,
        RMetaAnnotatedType type,
        ExpressionCardinality cardinality,
        Optionality optionality,
        SourceRange sourceRange) implements IRExpr {

    @Override
    public IRExprKind kind() {
        return IRExprKind.META_PARAM_REF;
    }

    @Override
    public List<? extends IRExpr> children() {
        return List.of();
    }
}
