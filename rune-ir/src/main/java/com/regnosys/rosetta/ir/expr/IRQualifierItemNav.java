package com.regnosys.rosetta.ir.expr;

import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.types.ExpressionCardinality;
import com.regnosys.rosetta.types.RMetaAnnotatedType;

import java.util.List;

/**
 * A METADATA-QUALIFIER read off a bound USER item — {@code item -> scheme} where the implicit
 * item's registering filter/extract binder ranges the elements of a {@code [metadata …]}-
 * annotated feature and the nav's feature name IS one of that feature's metadata qualifiers
 * (never a member — the linker binds no channel, the {@code featureUnresolved} class) — the
 * #512 arm-B2 teach of the {@code RFeatureCall:featureUnresolved.nonSymbolReceiver} qualifier
 * class (the #509-banked nsrGate census read the class {@code RImplicitVariable.recvLowers:
 * IRVariable + mMiss + qMiss} over {@code entityId string (0..*) [metadata scheme]}-family
 * sources, the {@code X -> entityId then filter (… item -> scheme …)} spellings; the census's
 * own shallow source read hit the elided-pipe implicit — {@code other:RImplicitVariable} —
 * which the #512 DEEP PIPE WALK ({@code terminalBindingSourceFeature}) resolves through the
 * then-chain to the terminal resolved feature carrying the qualifier).
 *
 * <p>The proof is the walk's (prove-or-decline): the binder source's TERMINAL resolved
 * feature must carry {@code [metadata <qualifierName>]}; an unprovable source keeps
 * declining. The item's engine type reads the UNWRAPPED element form ({@code basic:string} at
 * the census — the wrapper is invisible to the type channel), so the AST-level walk is the
 * only sound signal (the {@code itemBindingMetaSourced} precedent).
 *
 * <p>Deliberately SHALLOW (the {@link IRMetaItemNav} pattern): the qualifier's name is the
 * neutral fact; the item's Java binding (the lambda parameter) and the FieldWithMetaX
 * qualifier deref (legacy's {@code getMeta().getScheme()}-family composition) are legacy's
 * own render decisions (the L-029 split) — NOT carried as IR children. The DISTINCT kind is
 * the safety (the #499 law): the consumers that admit it are NAMED — the equality operand
 * gate (the census's {@code interior.REqualityExpr} claim roots — the {@code item -> scheme
 * = "…"} extract-if class) and the call-arg gate (the {@code StringContains(item -> scheme,
 * …)} filter class) — and every containing claim root renders through the compiler's
 * oracle-root serve (the shared {@code containsOracleLeaf} walk), byte-identical BY
 * IDENTITY, while the kind itself has NO leaf-emitter arm (the routing safety).
 *
 * <p>Fork-authored (PR #512); not present upstream.
 */
public record IRQualifierItemNav(
        String qualifierName,
        NodeId nodeId,
        RMetaAnnotatedType type,
        ExpressionCardinality cardinality,
        Optionality optionality,
        SourceRange sourceRange) implements IRExpr {

    @Override
    public IRExprKind kind() {
        return IRExprKind.QUALIFIER_ITEM_NAV;
    }

    @Override
    public List<? extends IRExpr> children() {
        return List.of();
    }
}
