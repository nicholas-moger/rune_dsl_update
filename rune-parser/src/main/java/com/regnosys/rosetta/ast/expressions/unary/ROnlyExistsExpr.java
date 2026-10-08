package com.regnosys.rosetta.ast.expressions.unary;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.expressions.supporting.ROnlyExistsElement;
import com.regnosys.rosetta.ast.visitor.RExpressionVisitor;

import java.util.ArrayList;
import java.util.List;

/**
 * Only-exists expression node, corresponding to the
 * {@code OnlyExistsExpr} grammar alternative.
 *
 * <p>Grammar:
 * <pre>
 * onlyExistsExpression:
 *     ( onlyExistsElement
 *     | LPAREN onlyExistsElement (COMMA onlyExistsElement)* RPAREN
 *     ) ONLY EXISTS
 * ;
 * </pre>
 *
 * <p>Represents the {@code only exists} pattern that asserts only the
 * specified elements (paths) exist in a structure. Unlike other unary
 * expressions in this package, {@code ROnlyExistsExpr} has no left/argument
 * operand — the elements list IS the operand.
 */
public class ROnlyExistsExpr extends RExpression {

    private final List<ROnlyExistsElement> elements = new ArrayList<>();

    // -- elements -------------------------------------------------------------

    public List<ROnlyExistsElement> elements() {
        return elements;
    }

    // -- children (for traversal) ---------------------------------------------

    @Override
    public List<? extends RNode> children() {
        return List.copyOf(elements);
    }

    @Override
    public <R, C> R accept(RExpressionVisitor<R, C> visitor, C context) {
        return visitor.visitOnlyExists(this, context);
    }
}
