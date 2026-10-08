package com.regnosys.rosetta.ir.expr;

import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.types.ExpressionCardinality;
import com.regnosys.rosetta.types.RMetaAnnotatedType;

import java.util.List;

/**
 * A disguised SYMBOL-receiver navigation — {@code <Rule|Function> -> <feature>}, parsed as a
 * bound-chain {@code REnumValueRef} whose {@code resolvedSymbol} is a workspace rule/function
 * (the #480 MF-1 head-name-collision shape: the typing engine's chain-bind deliberately leaves
 * {@code resolvedSymbol} in place, and legacy {@code handle(REnumValueRef)} renders the
 * RULE/FUNCTION-receiver navigation BEFORE any chain arm) — the #501 arm-C teach of the
 * {@code attributeChain.headSymbolNav} face (1,819 node-unit at the #501 census: RFunction
 * 1,725 + RRule 94, 100% typed, 100% INTERIOR). The neutral fact carried:
 * {@link #symbolKind()} ({@code "rule"}/{@code "function"} — which legacy receiver branch
 * serves the head).
 *
 * <p><strong>Deliberately SHALLOW</strong> (the #494 {@link IRConstruct} / #500 {@link IRPipe}
 * pattern): the receiver invocation and the feature hop are NOT carried as IR children —
 * {@link #children()} is empty — because the receiver is a BARE rule/function invocation whose
 * deep lowering is the deferred bare-delegation class, and legacy's render runs a
 * branch-selection ladder (the bare-RRule gate, the #280/#295/#333/#359/#371 meta-leaf ladder)
 * that only legacy itself can decide. The DISTINCT kind is the safety (the #499 law): no native
 * consumer gate admits it silently — the #501 consumer admissions
 * ({@code isNavigableReceiver}/{@code isScalarOperand}[SINGLE]/{@code isExistenceOperand}/
 * {@code isSimpleCallArg}/{@code producesComparisonResult}) name it explicitly, and the Java
 * emitter routes every containing claim root through the compiler's oracle-root serve (the
 * shared {@code containsOracleLeaf} walk): the root renders the LITERAL legacy line
 * ({@code super.visitX}), so the whole disguised ladder — branch selection included — is
 * byte-identical BY IDENTITY, while this kind itself has NO leaf-emitter arm (a native compose
 * can never reach it — the routing safety, the #500 arm-A relocation one kind further).
 *
 * <p>Fork-authored (PR #501); not present upstream.
 */
public record IRSymbolNav(
        String symbolKind,
        NodeId nodeId,
        RMetaAnnotatedType type,
        ExpressionCardinality cardinality,
        Optionality optionality,
        SourceRange sourceRange) implements IRExpr {

    @Override
    public IRExprKind kind() {
        return IRExprKind.SYMBOL_NAV;
    }

    @Override
    public List<? extends IRExpr> children() {
        return List.of();
    }
}
