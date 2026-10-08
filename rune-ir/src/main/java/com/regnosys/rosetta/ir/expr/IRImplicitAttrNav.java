package com.regnosys.rosetta.ir.expr;

import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.types.ExpressionCardinality;
import com.regnosys.rosetta.types.RMetaAnnotatedType;

import java.util.List;

/**
 * A bare ATTRIBUTE reference whose receiver legacy synthesizes ({@code attr} naming a member
 * of the enclosing lambda's element type — or of a rule/report scope — which legacy expands
 * to an implicit input/item navigation inside {@code ReferenceHandler.handle}) — the #528
 * teach of the L-113 relabel belt. The adapter's own bare-attr item-nav arm ({@code
 * adaptBareAttrItemNav}) claims every shape whose equivalent navigation it can BUILD and
 * lower; this kind claims the residue that arm leaves — the walk-family declines the #497
 * {@code implicitAttrRoot} census names ({@code shortcutCollision} / {@code
 * noFilterExtractBinder} / {@code sourceElementUnresolved}), where the equivalent cannot be
 * synthesized at all, so the IR states the fact it genuinely knows (WHICH attribute is read)
 * and leaves the receiver synthesis to legacy.
 *
 * <p>The accounting is why the kind exists (the #516 law): the L-113 belt sat inside the
 * compiler's ir-EMPTY branch, so its claims counted DELEGATED — a claim with no IR lowering
 * whose relabel gate claimed the raw shape. A counter flip alone would have been a relabel,
 * not a conversion; minting the lowering is what makes the claims honestly IR-LOWERED, with
 * the render unchanged (the belt's {@code super.visitSymbolReference} line IS the oracle
 * dispatch's own {@code bareSymbolRef} serve — byte-identical BY IDENTITY).
 *
 * <p><strong>Deliberately SHALLOW</strong> (the {@link IRRuleInputNav} pattern, one seat
 * over — that kind carries the rule-body TOP-LEVEL bare attribute, this one the residue at
 * every other bare-attribute seat): only the attribute's NAME is carried (the neutral fact);
 * the receiver synthesis, the binder resolution and every hop coercion are legacy's own (the
 * L-029 split). The DISTINCT kind is the safety (the #499 law): the consumer admissions that
 * name it are listed on {@link IRExprKind#IMPLICIT_ATTR_NAV} (none at #528; the #529 equality
 * operand; PR #640's receiver / operand / sibling / argument admissions), every containing claim root renders through the compiler's oracle-root serve (the
 * shared {@code containsOracleLeaf} walk; at-root claims serve through the standing
 * {@code bareSymbolRef} dispatch leg), and the kind has no leaf-emitter arm — so a native
 * compose can never reach it.
 *
 * <p>Fork-authored (PR #528); not present upstream.
 */
public record IRImplicitAttrNav(
        String attributeName,
        NodeId nodeId,
        RMetaAnnotatedType type,
        ExpressionCardinality cardinality,
        Optionality optionality,
        SourceRange sourceRange) implements IRExpr {

    @Override
    public IRExprKind kind() {
        return IRExprKind.IMPLICIT_ATTR_NAV;
    }

    @Override
    public List<? extends IRExpr> children() {
        return List.of();
    }
}
