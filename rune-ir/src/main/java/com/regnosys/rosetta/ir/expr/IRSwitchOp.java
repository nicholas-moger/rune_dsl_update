package com.regnosys.rosetta.ir.expr;

import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.types.ExpressionCardinality;
import com.regnosys.rosetta.types.RMetaAnnotatedType;

import java.util.List;

/**
 * A {@code switch} expression root — {@code <arg> switch <guard> then <result>, …[, default
 * then <result>]} — the #513 noAdaptArm-sweep teach of the {@code RSwitchExpr} family
 * ({@code RSwitchExpr:noAdaptArm} 33 sole + 2 untargeted root visits at the #512 SOT; the
 * standing lambdaVisit/condVisit/ctorVisit censuses read the class dominated by
 * extract-BODY seats — {@code lambdaVisit.extract.implicitBare.typed.blocked.body.RSwitchExpr}
 * 29 of the cdm6 32 — plus one conditional-THEN and one choice-ctor VALUE seat).
 *
 * <p>Deliberately SHALLOW (the #500 {@link IRPipe} family-arm pattern): the case count and
 * the default-case presence are the neutral facts; the argument subtree, the case guards
 * (literal / enum-value / type patterns — non-expression {@code RSwitchCaseGuard} records)
 * and the case result subtrees are NOT carried as IR children. The whole render — legacy's
 * switch ladder (the guard dispatch, the case casts, the default arm) — is legacy's own
 * (the L-029 split): a claim ROOT serves through the compiler's {@code switchOp} oracle leg
 * (the literal {@code super.visitSwitch} line), whose legacy re-walk visits every interior
 * node at its own seat (the #504 re-entrant law — interiors claim or decline on their own
 * verdicts, so the walk accounting stays honest node-by-node). The DISTINCT kind is the
 * safety (the #499 law): the consumers that admit it are NAMED — the equality/comparison/
 * existence operand gates, the call-arg gate and the nav-receiver gate — and every
 * containing claim root renders through the shared {@code containsOracleLeaf} routing,
 * byte-identical BY IDENTITY, while the kind itself has NO leaf-emitter arm.
 *
 * <p>Fork-authored (PR #513); not present upstream.
 */
public record IRSwitchOp(
        int caseCount,
        boolean hasDefault,
        NodeId nodeId,
        RMetaAnnotatedType type,
        ExpressionCardinality cardinality,
        Optionality optionality,
        SourceRange sourceRange) implements IRExpr {

    @Override
    public IRExprKind kind() {
        return IRExprKind.SWITCH_OP;
    }

    @Override
    public List<? extends IRExpr> children() {
        return List.of();
    }
}
