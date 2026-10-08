package com.regnosys.rosetta.ir.expr;

import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.types.ExpressionCardinality;
import com.regnosys.rosetta.types.RMetaAnnotatedType;

import java.util.List;

/**
 * A TOP-LEVEL bare rule-input navigation ({@code attr} at rule-body top level, legacy's
 * synthesized {@code MapperS.of(input) -> attr} render — {@code buildImplicitInputReceiver}'s
 * non-lambda branch) — the #514 {@code attrOutsideFunction.ruleInputNav} teach (the decode
 * read the face at equality/existence/call-arg/list-receiver seats, ALL typed; the probe2
 * catch pivoted the arm from the native {@code FieldAccess{IRVariable{PARAM,"input"}}} form —
 * the rule cell's native nav render is unproven and the claims leaked to the emitter's
 * decline site — to THIS oracle leaf, the #513-p3 spine precedent one hop shorter). The
 * META-FEATURED slice ({@code ruleInputNav.offShape.IRMetaAccess} — the probe2-entered 33)
 * mints the SAME kind: the equivalent lowers through the #499 meta arm either way, and the
 * serve carries legacy's own FieldWithMetaX machinery BY IDENTITY.
 *
 * <p><strong>Deliberately SHALLOW</strong> (the {@link IRSynItemNav} pattern): only the
 * resolved feature's NAME is carried (the neutral fact); the {@code input} receiver
 * synthesis and every hop coercion are legacy's own (the L-029 split). The DISTINCT kind is
 * the safety (the #499 law): the consumers that admit it are NAMED — the
 * equality/existence operand, call-arg and nav-receiver gates — and every containing claim
 * root renders through the compiler's oracle-root serve (the shared
 * {@code containsOracleLeaf} walk — the literal {@code super.visitX} lines), while the kind
 * itself has NO leaf-emitter arm (a native compose can never reach it — the routing
 * safety).
 *
 * <p>Fork-authored (PR #514); not present upstream.
 */
public record IRRuleInputNav(
        String featureName,
        NodeId nodeId,
        RMetaAnnotatedType type,
        ExpressionCardinality cardinality,
        Optionality optionality,
        SourceRange sourceRange) implements IRExpr {

    @Override
    public IRExprKind kind() {
        return IRExprKind.RULE_INPUT_NAV;
    }

    @Override
    public List<? extends IRExpr> children() {
        return List.of();
    }
}
