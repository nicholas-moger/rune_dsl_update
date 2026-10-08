package com.regnosys.rosetta.ir.expr;

import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.types.ExpressionCardinality;
import com.regnosys.rosetta.types.RMetaAnnotatedType;

import java.util.List;

/**
 * A resolved-feature navigation off a META-SOURCED bound USER item — the #508 arm-A5 teach of
 * the {@code itemMetaSourced} residue (67 sole at the #507 SOT): the receiver lowers to a bound
 * {@code IRVariable{USER_ITEM}} whose registering binder ranges a META-annotated source (a
 * {@code FieldWithMetaX}-wrapped element list — the filter/extract direct meta-feature-call
 * sources and the #506-widened then/min/max/sort legs' unprovable element forms), so the L-029
 * meta split declines the NATIVE item-nav compose: legacy's per-element binding threads its own
 * null-safe {@code "Type coercion"} FieldWithMetaX deref legs the neutral IR deliberately does
 * not model. The #508 metaSrcGate census read the accessed feature 100% PLAIN
 * ({@code featPlain}) over {@code metaFc}/{@code alias}/pipe sources, all typed — the neutral
 * fact carried: {@link #featureName()} (the RESOLVED feature's own name; the feature gate
 * passed — only the RECEIVER's meta-source proof declined).
 *
 * <p><strong>Deliberately SHALLOW</strong> (the {@link IRSynItemNav} pattern): the item's Java
 * binding — the lambda variable name, the thenArg local — and every hop's meta/unwrap coercion
 * are legacy's own render decisions (the L-029 split), so the item receiver and the hop are NOT
 * carried as IR children ({@link #children()} is empty). The DISTINCT kind is the safety (the
 * #499 law): the consumers that admit it are NAMED, and the Java emitter routes every
 * containing claim root through the compiler's oracle-root serve (the shared
 * {@code containsOracleLeaf} walk): the root renders the LITERAL legacy line
 * ({@code super.visitX}), so the binder-scope machinery — variable naming, the FieldWithMetaX
 * derefs included — is byte-identical BY IDENTITY, while this kind itself has NO leaf-emitter
 * arm (a native compose can never reach it — the routing safety, the #500 arm-A relocation).
 *
 * <p>Fork-authored (PR #508); not present upstream.
 */
public record IRMetaItemNav(
        String featureName,
        NodeId nodeId,
        RMetaAnnotatedType type,
        ExpressionCardinality cardinality,
        Optionality optionality,
        SourceRange sourceRange) implements IRExpr {

    @Override
    public IRExprKind kind() {
        return IRExprKind.META_ITEM_NAV;
    }

    @Override
    public List<? extends IRExpr> children() {
        return List.of();
    }
}
