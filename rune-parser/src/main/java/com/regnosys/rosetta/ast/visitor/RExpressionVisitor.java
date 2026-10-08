package com.regnosys.rosetta.ast.visitor;

import com.regnosys.rosetta.ast.expressions.binary.*;
import com.regnosys.rosetta.ast.expressions.constructors.*;
import com.regnosys.rosetta.ast.expressions.literals.*;
import com.regnosys.rosetta.ast.expressions.references.*;
import com.regnosys.rosetta.ast.expressions.unary.*;

/**
 * Exhaustive visitor interface for Rosetta expression compilation.
 * Every concrete RExpression subclass has exactly one visit method.
 *
 * <p>Exhaustiveness is enforced by the interface contract: implementors
 * must provide all 38 methods. This is complementary to {@link
 * com.regnosys.rosetta.ast.util.AstVisitor} which provides coarse-grained
 * opt-in dispatch for generic traversal.
 *
 * @param <R> return type
 * @param <C> context type
 */
public interface RExpressionVisitor<R, C> {

    // Binary operations (8)
    R visitArithmetic(RArithmeticExpr expr, C context);
    R visitLogical(RLogicalExpr expr, C context);
    R visitEquality(REqualityExpr expr, C context);
    R visitComparison(RComparisonExpr expr, C context);
    R visitDefault(RDefaultExpr expr, C context);
    R visitContains(RContainsExpr expr, C context);
    R visitDisjoint(RDisjointExpr expr, C context);
    R visitJoin(RJoinExpr expr, C context);

    // Navigation (2)
    R visitFeatureCall(RFeatureCall expr, C context);
    R visitDeepFeatureCall(RDeepFeatureCall expr, C context);

    // References (4)
    R visitSymbolReference(RSymbolReference expr, C context);
    R visitEnumValueRef(REnumValueRef expr, C context);
    R visitImplicitVariable(RImplicitVariable expr, C context);
    R visitSuperCall(RSuperCall expr, C context);

    // Collections (8)
    R visitFilter(RFilterExpr expr, C context);
    R visitExtract(RExtractExpr expr, C context);
    R visitSort(RSortExpr expr, C context);
    R visitReduce(RReduceExpr expr, C context);
    R visitMax(RMaxExpr expr, C context);
    R visitMin(RMinExpr expr, C context);
    R visitListOp(RListOpExpr expr, C context);
    R visitCount(RCountExpr expr, C context);

    // Piping (1)
    R visitThen(RThenExpr expr, C context);

    // Control flow (2)
    R visitConditional(RConditionalExpr expr, C context);
    R visitSwitch(RSwitchExpr expr, C context);

    // Existence & cardinality (3)
    R visitExistence(RExistenceExpr expr, C context);
    R visitOnlyExists(ROnlyExistsExpr expr, C context);
    R visitCardinalityCheck(RCardinalityCheckExpr expr, C context);

    // Literals (6)
    R visitIntLiteral(RIntLiteral expr, C context);
    R visitNumberLiteral(RNumberLiteral expr, C context);
    R visitStringLiteral(RStringLiteral expr, C context);
    R visitBooleanLiteral(RBooleanLiteral expr, C context);
    R visitListLiteral(RListLiteral expr, C context);
    R visitEmptyLiteral(REmptyLiteral expr, C context);

    // Construction & metadata (2)
    R visitConstructor(RConstructorExpr expr, C context);
    R visitWithMeta(RWithMetaExpr expr, C context);

    // Conversion (2)
    R visitConversion(RConversionExpr expr, C context);
    R visitToString(RToStringExpr expr, C context);
}
