package com.regnosys.rosetta.ir.expr;

import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.types.ExpressionCardinality;
import com.regnosys.rosetta.types.RMetaAnnotatedType;

import java.util.List;

/**
 * A choice-OPTION selection spelled as a qualified name — {@code head -> OptionType} where the
 * leaf segment resolved through the pass-6 choice-option channel
 * ({@code REnumValueRef.resolvedChoiceOption}, the #451 clearing arm's bind) — the #505
 * {@code choiceOption} teach (239 sole at the #504 SOT, cdm6-dominant; the #505 enumSeatGate
 * census read the dominant face {@code headNone.RChoice} — a LOCAL head navigating to a global
 * choice/data-type option — beside the {@code head:RChoiceTypeRef.optOnHead} static-selection
 * minority).
 *
 * <p><strong>Deliberately SHALLOW</strong> (the {@link IRSymbolNav} pattern): the two name
 * segments ({@link #headName()}, {@link #optionName()}) are the neutral facts; the head's own
 * resolution (a scoped attribute, an implicit-item member, a global choice type) and the
 * option-selection render (the choice projection, the case narrowing — legacy's
 * {@code NavigationHandler}/{@code ReferenceHandler} choice-option arms) are legacy's own
 * render decisions (the L-029 split), so neither is carried as an IR child ({@link #children()}
 * is empty). The DISTINCT kind is the safety (the #499 law): the consumers that admit it are
 * NAMED — the nav-receiver gate (chained hops compose {@code FieldAccess} over it), the
 * equality and existence operand gates and the call-arg gate — and the Java emitter routes
 * every containing claim root through the compiler's oracle-root serve (the shared
 * {@code containsOracleLeaf} walk, incl. the #505 {@code enumChain} dispatch leg — the literal
 * {@code super.visitEnumValueRef} line), so the option machinery is byte-identical BY IDENTITY
 * while this kind itself has NO leaf-emitter arm (a native compose can never reach it — the
 * routing safety, the #500 arm-A relocation).
 *
 * <p>Fork-authored (PR #505); not present upstream.
 */
public record IRChoiceOptionNav(
        String headName,
        String optionName,
        NodeId nodeId,
        RMetaAnnotatedType type,
        ExpressionCardinality cardinality,
        Optionality optionality,
        SourceRange sourceRange) implements IRExpr {

    @Override
    public IRExprKind kind() {
        return IRExprKind.CHOICE_OPTION_NAV;
    }

    @Override
    public List<? extends IRExpr> children() {
        return List.of();
    }
}
