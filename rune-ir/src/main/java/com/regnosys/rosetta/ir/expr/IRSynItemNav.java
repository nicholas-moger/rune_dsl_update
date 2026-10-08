package com.regnosys.rosetta.ir.expr;

import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.types.ExpressionCardinality;
import com.regnosys.rosetta.types.RMetaAnnotatedType;

import java.util.List;

/**
 * A resolved-feature navigation off an UN-RETYPEABLE synthetic implicit item — the #504 arm-B
 * teach of the {@code receiverSyntheticItem} residue (749 sole at the #503 SOT, + the bare-attr
 * arm's 271 {@code attrOutsideFunction.receiverSyntheticItem} equivalents riding the same
 * channel): the receiver lowers to an {@code IRVariable{SYNTHETIC_ITEM}} but BOTH retype arms
 * decline — the #479 filter/extract retype (the binder source is meta-typed, deep-piped or
 * unprovable) and the #492 alias retype — the #502 synItemGate census's walked residue
 * (metaFeature/deep-pipe bottoms dominant · switchCase 71 · namedExtract ~43 · otherBinder 25).
 * The neutral fact carried: {@link #featureName()} (the RESOLVED feature's own name — the
 * feature gate passed; only the RECEIVER's element proof failed).
 *
 * <p><strong>Deliberately SHALLOW</strong> (the {@link IRSymbolNav} pattern): the item's Java
 * binding — the lambda variable name, the then-arg local, the switch-case cast var — and every
 * hop's meta/unwrap coercion are legacy's own render decisions (the L-029 split), so the item
 * receiver and the hop are NOT carried as IR children ({@link #children()} is empty). The
 * DISTINCT kind is the safety (the #499 law): the consumers that admit it are NAMED — the
 * nav-receiver gate (chained hops compose {@code FieldAccess} over it), the equality and
 * existence operand gates and the call-arg gate (the post-arm probe's own exposed frontier,
 * operand:IRSynItemNav 190/33/6) — the Java emitter routes every containing claim root through the
 * compiler's oracle-root serve (the shared {@code containsOracleLeaf} walk): the root renders
 * the LITERAL legacy line ({@code super.visitX}), so the binder-scope machinery — variable
 * naming, meta derefs, case narrowing included — is byte-identical BY IDENTITY, while this kind
 * itself has NO leaf-emitter arm (a native compose can never reach it — the routing safety,
 * the #500 arm-A relocation).
 *
 * <p>Fork-authored (PR #504); not present upstream.
 */
public record IRSynItemNav(
        String featureName,
        NodeId nodeId,
        RMetaAnnotatedType type,
        ExpressionCardinality cardinality,
        Optionality optionality,
        SourceRange sourceRange) implements IRExpr {

    @Override
    public IRExprKind kind() {
        return IRExprKind.SYN_ITEM_NAV;
    }

    @Override
    public List<? extends IRExpr> children() {
        return List.of();
    }
}
