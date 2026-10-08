package com.regnosys.rosetta.ir.expr;

import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.types.ExpressionCardinality;
import com.regnosys.rosetta.types.RMetaAnnotatedType;

import java.util.List;

/**
 * A deep-path feature navigation — {@code receiver ->> feature}, the
 * {@code DeepFeatureCallExpr} grammar operator — the #507 {@code RDeepFeatureCall:noAdaptArm}
 * teach (191 sole + 71 untargeted root visits at the #506 SOT, all cdm6 FUNCTION; the family
 * had NO adapt arm at all, so the generic fallback token carried the whole class).
 *
 * <p>The RECEIVER subtree is a real IR child — the #507 deepGate census read the interior
 * population 100% {@code featHit} + 100% {@code recvLowers}, so the arm's receiver-lowers gate
 * admits the whole live class and the composition is the honest representation. The deep-path
 * RESOLUTION and render — legacy's generated {@code DeepPathUtil.choose<Feature>} routing, the
 * multi-candidate attribute walk behind the {@code ->>} semantics — are legacy's own render
 * decisions (the L-029 split), so only the resolved feature's NAME is carried (the neutral
 * fact; the linker's {@code resolvedFeature} bind is the arm's proof gate, not IR payload).
 *
 * <p>The DISTINCT kind is the safety (the #499 law): the consumers that admit it are NAMED —
 * the nav-receiver gate (chained {@code ->> a -> b} hops compose {@link FieldAccess} over it),
 * the equality and existence operand gates and the call-arg gate — and the Java emitter routes
 * every containing claim root through the compiler's oracle-root serve (the shared
 * {@code containsOracleLeaf} walk, incl. the #507 {@code deepChain} dispatch leg — the literal
 * {@code super.visitDeepFeatureCall} line), so the deep-path machinery is byte-identical BY
 * IDENTITY while this kind itself has NO leaf-emitter arm (a native compose can never reach
 * it — the routing safety, the #500 arm-A relocation).
 *
 * <p>Fork-authored (PR #507); not present upstream.
 */
public record IRDeepFeatureNav(
        IRExpr receiver,
        String featureName,
        NodeId nodeId,
        RMetaAnnotatedType type,
        ExpressionCardinality cardinality,
        Optionality optionality,
        SourceRange sourceRange) implements IRExpr {

    @Override
    public IRExprKind kind() {
        return IRExprKind.DEEP_FEATURE_NAV;
    }

    @Override
    public List<? extends IRExpr> children() {
        return List.of(receiver);
    }
}
