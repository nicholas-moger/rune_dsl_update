package com.regnosys.rosetta.ast.expressions.unary;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.enums.ConversionKind;
import com.regnosys.rosetta.ast.visitor.RExpressionVisitor;

import com.regnosys.rosetta.symbols.diagnostics.DiagnosticCategory;
import com.regnosys.rosetta.symbols.linker.CrossRefField;

import java.util.List;
import java.util.Optional;

/**
 * Conversion (to-xxx) expression node, consolidating 7 grammar
 * alternatives into one.
 *
 * <p>Represents type conversion operations: {@code to-number}, {@code to-int},
 * {@code to-time}, {@code to-enum}, {@code to-date}, {@code to-date-time},
 * and {@code to-zoned-date-time}.
 *
 * <p>7-to-1 consolidation of EMF's separate conversion expression types.
 */
public class RConversionExpr extends RExpression {

    private RExpression argument;
    private ConversionKind kind;
    private String targetEnumName;

    // -- argument (the expression being converted) ----------------------------

    public RExpression argument() {
        return argument;
    }

    public void setArgument(RExpression argument) {
        checkMutable();
        this.argument = argument;
    }

    @Override
    public Optional<RExpression> left() {
        return Optional.ofNullable(argument);
    }

    // -- kind -----------------------------------------------------------------

    public ConversionKind kind() {
        return kind;
    }

    public void setKind(ConversionKind kind) {
        checkMutable();
        this.kind = kind;
    }

    // -- targetEnumName (optional — only for ConversionKind.ENUM) ---------------

    /**
     * Returns the target enum type name for {@code to-enum qualifiedName} conversions.
     * Empty for all other conversion kinds.
     */
    public Optional<String> targetEnumName() {
        return Optional.ofNullable(targetEnumName);
    }

    public void setTargetEnumName(String targetEnumName) {
        checkMutable();
        this.targetEnumName = targetEnumName;
    }

    // -- children (for traversal) ---------------------------------------------

    @Override
    public List<? extends RNode> children() {
        if (argument != null) {
            return List.of(argument);
        }
        return List.of();
    }

    // === M3 resolved fields (D2) =============================================

    @CrossRefField(category = DiagnosticCategory.ENUM_NOT_FOUND)
    private com.regnosys.rosetta.ast.types.REnumeration resolvedTargetEnum;

    public java.util.Optional<com.regnosys.rosetta.ast.types.REnumeration> targetEnum() { return java.util.Optional.ofNullable(resolvedTargetEnum); }
    public void setResolvedTargetEnum(com.regnosys.rosetta.ast.types.REnumeration resolved) { checkMutable(); this.resolvedTargetEnum = resolved; }

    @Override
    public <R, C> R accept(RExpressionVisitor<R, C> visitor, C context) {
        return visitor.visitConversion(this, context);
    }
}
