package com.regnosys.rosetta.ir.expr;

import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.types.ExpressionCardinality;
import com.regnosys.rosetta.types.RMetaAnnotatedType;

import java.util.List;

/**
 * A by-name reference to a declared symbol that takes no argument list of its own
 * (Wave-0 family 9 and the nullary cases of families 8/10): an enum value, a function
 * referred to without a call, or {@code super}. Corresponds to {@code RSuperCall} and
 * the symbol/enum-value reference cases of {@code RSymbolReference}/{@code REnumValueRef}.
 *
 * <p>{@link #target()} is the qualified name of the referent (for {@code SUPER} it is the
 * empty string — the referent is the enclosing definition's overridden form; for
 * {@code ALIAS} it is the alias/shortcut name). The {@code SUPER} kind deliberately drops
 * the Java "super-dispatcher / vtable" framing: it means only "invoke the enclosing
 * rule/function's overridden definition", which a functional target can render as ordinary
 * delegation.
 *
 * <p>{@code ALIAS} is a reference to a local alias (a function {@code RShortcut} — a named
 * sub-expression bound in the function body). It is a <em>let-binding reference</em>: the
 * neutral fact is "the value named X", nothing more. How a backend realises it is a lowering
 * choice — Java hoists each alias to a helper method threading the enclosing function's
 * inputs ({@code aliasName(input1, input2, …)}), a functional target may inline the bound
 * expression or capture a closure — so the input-threading and the Java name disambiguation
 * are deliberately NOT modelled here; they are the emitter's Java concern (computed against
 * the enclosing function, mirroring the L-029 split that keeps Java-name decisions out of the
 * neutral layer).
 *
 * <p>{@code RULE} is a reference to a reporting/eligibility rule invoked on the enclosing
 * rule's implicit input (the bare-delegation shape {@code output = SomeRule}). Like
 * {@code FUNCTION} it is the callee of an {@link IRApply}; the neutral fact is "invoke the
 * rule named X on the implicit input", and the Java realisation — the {@code @Inject}
 * {@code <Name>Rule} receiver field name and the {@code input} binding — is the emitter's
 * concern (the L-029 split), supplied by a compiler-side resolver exactly like {@code FUNCTION}.
 *
 * <p>A reference that is then <em>applied</em> to arguments is modelled as an
 * {@link IRApply} whose callee is one of these references.
 */
public record IRReference(
        String target,
        ReferenceKind referenceKind,
        NodeId nodeId,
        RMetaAnnotatedType type,
        ExpressionCardinality cardinality,
        Optionality optionality,
        SourceRange sourceRange) implements IRExpr {

    /** The flavour of referent: an enum value, a function, {@code super}, a local alias, or a rule. */
    public enum ReferenceKind { ENUM_VALUE, FUNCTION, SUPER, ALIAS, RULE }

    @Override
    public IRExprKind kind() {
        return IRExprKind.REFERENCE;
    }

    @Override
    public List<? extends IRExpr> children() {
        return List.of();
    }
}
