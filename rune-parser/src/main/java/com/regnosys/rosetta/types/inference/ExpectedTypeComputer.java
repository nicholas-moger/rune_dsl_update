package com.regnosys.rosetta.types.inference;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.expressions.constructors.RConditionalExpr;
import com.regnosys.rosetta.ast.expressions.constructors.RConstructorExpr;
import com.regnosys.rosetta.ast.expressions.supporting.RKeyValuePair;
import com.regnosys.rosetta.ast.functions.ROperation;
import com.regnosys.rosetta.types.*;
import com.regnosys.rosetta.types.builtin.BuiltinTypeRegistry;

import java.util.Objects;
import java.util.Optional;

/**
 * Computes the expected type for an expression given its parent context.
 * Used for bidirectional type inference — knowing what type is expected
 * helps resolve ambiguous expressions and enables better diagnostics.
 *
 * <p>Spec: section 3.8 in {@code docs/specs/2026-04-08-m4-type-system-design.md}.
 */
public final class ExpectedTypeComputer {

    private final BuiltinTypeRegistry builtins;
    private final ExpressionTypeComputer typeComputer;

    public ExpectedTypeComputer(BuiltinTypeRegistry builtins, ExpressionTypeComputer typeComputer) {
        this.builtins = Objects.requireNonNull(builtins);
        this.typeComputer = Objects.requireNonNull(typeComputer);
    }

    /**
     * Computes the expected type for {@code child} given its {@code parent}
     * expression context. Returns empty if no specific type is expected.
     *
     * @param child the expression whose expected type is being computed
     * @param parent the parent expression/node context (may be null)
     * @return the expected type, or empty if unconstrained
     */
    public Optional<RMetaAnnotatedType> expectedType(RExpression child, RNode parent) {
        if (parent == null) return Optional.empty();

        // Conditional — condition expects boolean
        if (parent instanceof RConditionalExpr cond && child == cond.condition()) {
            return Optional.of(RMetaAnnotatedType.withNoMeta(RBasicType.BOOLEAN));
        }

        // Constructor key-value pairs — value expects the attribute's type
        if (parent instanceof RKeyValuePair kvp) {
            // The expected type would come from the resolved attribute on the
            // constructor's type. This requires type-directed resolution (T15).
            // For now, return empty.
            return Optional.empty();
        }

        // Operation — expression expects target attribute's type
        if (parent instanceof ROperation op && child == op.expression()) {
            // Target type resolution depends on M3 lexical resolution of
            // the targetName + M4 type-directed resolution of segments.
            // For now, return empty until T15-T17 wire this up.
            return Optional.empty();
        }

        return Optional.empty();
    }
}
