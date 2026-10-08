package com.regnosys.rosetta.ast;

import com.regnosys.rosetta.ast.enums.ArithOp;
import com.regnosys.rosetta.ast.enums.CardMod;
import com.regnosys.rosetta.ast.enums.CompOp;
import com.regnosys.rosetta.ast.enums.EqOp;
import com.regnosys.rosetta.ast.enums.LogOp;
import com.regnosys.rosetta.ast.expressions.binary.RArithmeticExpr;
import com.regnosys.rosetta.ast.expressions.binary.RComparisonExpr;
import com.regnosys.rosetta.ast.expressions.binary.RContainsExpr;
import com.regnosys.rosetta.ast.expressions.binary.RDefaultExpr;
import com.regnosys.rosetta.ast.expressions.binary.RDisjointExpr;
import com.regnosys.rosetta.ast.expressions.binary.REqualityExpr;
import com.regnosys.rosetta.ast.expressions.binary.RJoinExpr;
import com.regnosys.rosetta.ast.expressions.binary.RLogicalExpr;
import com.regnosys.rosetta.ast.expressions.binary.RThenExpr;
import com.regnosys.rosetta.ast.expressions.supporting.RInlineFunction;
import com.regnosys.rosetta.ast.visitor.RExpressionVisitor;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for binary expression nodes (Task 9).
 *
 * <p>These tests construct nodes directly (no parsing) to validate getters,
 * setters, optional fields, enum fields, and the {@code children()} traversal
 * for all binary expression types.
 */
class ExpressionBinaryTest extends BaseAstTest {

    // =========================================================================
    // Helper: concrete RExpression for testing
    // =========================================================================

    private static class StubExpression extends RExpression {
        @Override
        public <R, C> R accept(RExpressionVisitor<R, C> visitor, C context) {
            throw new UnsupportedOperationException("Test stub");
        }
    }

    // =========================================================================
    // RArithmeticExpr
    // =========================================================================

    @Test
    void rArithmeticExpr_defaultState() {
        RArithmeticExpr expr = new RArithmeticExpr();
        assertNull(expr.op());
        assertNull(expr.rawLeft());
        assertNull(expr.rawRight());
        assertTrue(expr.children().isEmpty());
    }

    @Test
    void rArithmeticExpr_setOp() {
        RArithmeticExpr expr = new RArithmeticExpr();
        expr.setOp(ArithOp.PLUS);
        assertEquals(ArithOp.PLUS, expr.op());

        expr.setOp(ArithOp.MULTIPLY);
        assertEquals(ArithOp.MULTIPLY, expr.op());
    }

    @Test
    void rArithmeticExpr_setLeftAndRight() {
        RArithmeticExpr expr = new RArithmeticExpr();
        StubExpression left = new StubExpression();
        StubExpression right = new StubExpression();
        expr.setLeft(left);
        expr.setRight(right);
        expr.setOp(ArithOp.MINUS);

        assertSame(left, expr.rawLeft());
        assertSame(right, expr.rawRight());
        assertEquals(Optional.of(left), expr.left());
    }

    @Test
    void rArithmeticExpr_childrenIncludesLeftAndRight() {
        RArithmeticExpr expr = new RArithmeticExpr();
        StubExpression left = new StubExpression();
        StubExpression right = new StubExpression();
        expr.setLeft(left);
        expr.setRight(right);

        List<? extends RNode> children = expr.children();
        assertEquals(2, children.size());
        assertSame(left, children.get(0));
        assertSame(right, children.get(1));
    }

    @Test
    void rArithmeticExpr_childrenFiltersNulls() {
        RArithmeticExpr expr = new RArithmeticExpr();
        StubExpression right = new StubExpression();
        expr.setRight(right);

        List<? extends RNode> children = expr.children();
        assertEquals(1, children.size());
        assertSame(right, children.get(0));
    }

    @Test
    void rArithmeticExpr_allOps() {
        for (ArithOp op : ArithOp.values()) {
            RArithmeticExpr expr = new RArithmeticExpr();
            expr.setOp(op);
            assertEquals(op, expr.op());
        }
    }

    @Test
    void rArithmeticExpr_extendsRBinaryExpression() {
        RArithmeticExpr expr = new RArithmeticExpr();
        assertInstanceOf(RBinaryExpression.class, expr);
        assertInstanceOf(RExpression.class, expr);
        assertInstanceOf(RNode.class, expr);
    }

    // =========================================================================
    // RComparisonExpr
    // =========================================================================

    @Test
    void rComparisonExpr_defaultState() {
        RComparisonExpr expr = new RComparisonExpr();
        assertNull(expr.op());
        assertEquals(Optional.empty(), expr.mod());
        assertNull(expr.rawLeft());
        assertNull(expr.rawRight());
        assertTrue(expr.children().isEmpty());
    }

    @Test
    void rComparisonExpr_setOpAndMod() {
        RComparisonExpr expr = new RComparisonExpr();
        expr.setOp(CompOp.GT);
        expr.setMod(CardMod.ALL);

        assertEquals(CompOp.GT, expr.op());
        assertEquals(Optional.of(CardMod.ALL), expr.mod());
    }

    @Test
    void rComparisonExpr_noMod() {
        RComparisonExpr expr = new RComparisonExpr();
        expr.setOp(CompOp.LTE);

        assertEquals(CompOp.LTE, expr.op());
        assertEquals(Optional.empty(), expr.mod());
    }

    @Test
    void rComparisonExpr_allOps() {
        for (CompOp op : CompOp.values()) {
            RComparisonExpr expr = new RComparisonExpr();
            expr.setOp(op);
            assertEquals(op, expr.op());
        }
    }

    @Test
    void rComparisonExpr_childrenWithLeftAndRight() {
        RComparisonExpr expr = new RComparisonExpr();
        StubExpression left = new StubExpression();
        StubExpression right = new StubExpression();
        expr.setLeft(left);
        expr.setRight(right);

        List<? extends RNode> children = expr.children();
        assertEquals(2, children.size());
    }

    @Test
    void rComparisonExpr_extendsRBinaryExpression() {
        RComparisonExpr expr = new RComparisonExpr();
        assertInstanceOf(RBinaryExpression.class, expr);
    }

    // =========================================================================
    // REqualityExpr
    // =========================================================================

    @Test
    void rEqualityExpr_defaultState() {
        REqualityExpr expr = new REqualityExpr();
        assertNull(expr.op());
        assertEquals(Optional.empty(), expr.mod());
        assertNull(expr.rawLeft());
        assertNull(expr.rawRight());
        assertTrue(expr.children().isEmpty());
    }

    @Test
    void rEqualityExpr_setOpAndMod() {
        REqualityExpr expr = new REqualityExpr();
        expr.setOp(EqOp.EQ);
        expr.setMod(CardMod.ANY);

        assertEquals(EqOp.EQ, expr.op());
        assertEquals(Optional.of(CardMod.ANY), expr.mod());
    }

    @Test
    void rEqualityExpr_noMod() {
        REqualityExpr expr = new REqualityExpr();
        expr.setOp(EqOp.NEQ);
        assertEquals(Optional.empty(), expr.mod());
    }

    @Test
    void rEqualityExpr_allOps() {
        for (EqOp op : EqOp.values()) {
            REqualityExpr expr = new REqualityExpr();
            expr.setOp(op);
            assertEquals(op, expr.op());
        }
    }

    @Test
    void rEqualityExpr_childrenWithLeftAndRight() {
        REqualityExpr expr = new REqualityExpr();
        StubExpression left = new StubExpression();
        StubExpression right = new StubExpression();
        expr.setLeft(left);
        expr.setRight(right);

        assertEquals(2, expr.children().size());
    }

    @Test
    void rEqualityExpr_extendsRBinaryExpression() {
        REqualityExpr expr = new REqualityExpr();
        assertInstanceOf(RBinaryExpression.class, expr);
    }

    // =========================================================================
    // RLogicalExpr
    // =========================================================================

    @Test
    void rLogicalExpr_defaultState() {
        RLogicalExpr expr = new RLogicalExpr();
        assertNull(expr.op());
        assertNull(expr.rawLeft());
        assertNull(expr.rawRight());
        assertTrue(expr.children().isEmpty());
    }

    @Test
    void rLogicalExpr_setOp() {
        RLogicalExpr expr = new RLogicalExpr();
        expr.setOp(LogOp.AND);
        assertEquals(LogOp.AND, expr.op());

        expr.setOp(LogOp.OR);
        assertEquals(LogOp.OR, expr.op());
    }

    @Test
    void rLogicalExpr_childrenWithLeftAndRight() {
        RLogicalExpr expr = new RLogicalExpr();
        StubExpression left = new StubExpression();
        StubExpression right = new StubExpression();
        expr.setLeft(left);
        expr.setRight(right);
        expr.setOp(LogOp.AND);

        List<? extends RNode> children = expr.children();
        assertEquals(2, children.size());
        assertSame(left, children.get(0));
        assertSame(right, children.get(1));
    }

    @Test
    void rLogicalExpr_extendsRBinaryExpression() {
        RLogicalExpr expr = new RLogicalExpr();
        assertInstanceOf(RBinaryExpression.class, expr);
    }

    // =========================================================================
    // RContainsExpr
    // =========================================================================

    @Test
    void rContainsExpr_defaultState() {
        RContainsExpr expr = new RContainsExpr();
        assertNull(expr.rawLeft());
        assertNull(expr.rawRight());
        assertTrue(expr.children().isEmpty());
    }

    @Test
    void rContainsExpr_setLeftAndRight() {
        RContainsExpr expr = new RContainsExpr();
        StubExpression left = new StubExpression();
        StubExpression right = new StubExpression();
        expr.setLeft(left);
        expr.setRight(right);

        assertSame(left, expr.rawLeft());
        assertSame(right, expr.rawRight());
    }

    @Test
    void rContainsExpr_childrenWithLeftAndRight() {
        RContainsExpr expr = new RContainsExpr();
        StubExpression left = new StubExpression();
        StubExpression right = new StubExpression();
        expr.setLeft(left);
        expr.setRight(right);

        assertEquals(2, expr.children().size());
    }

    @Test
    void rContainsExpr_extendsRBinaryExpression() {
        RContainsExpr expr = new RContainsExpr();
        assertInstanceOf(RBinaryExpression.class, expr);
    }

    // =========================================================================
    // RDisjointExpr
    // =========================================================================

    @Test
    void rDisjointExpr_defaultState() {
        RDisjointExpr expr = new RDisjointExpr();
        assertNull(expr.rawLeft());
        assertNull(expr.rawRight());
        assertTrue(expr.children().isEmpty());
    }

    @Test
    void rDisjointExpr_setLeftAndRight() {
        RDisjointExpr expr = new RDisjointExpr();
        StubExpression left = new StubExpression();
        StubExpression right = new StubExpression();
        expr.setLeft(left);
        expr.setRight(right);

        assertSame(left, expr.rawLeft());
        assertSame(right, expr.rawRight());
    }

    @Test
    void rDisjointExpr_childrenWithLeftAndRight() {
        RDisjointExpr expr = new RDisjointExpr();
        StubExpression left = new StubExpression();
        StubExpression right = new StubExpression();
        expr.setLeft(left);
        expr.setRight(right);

        assertEquals(2, expr.children().size());
    }

    @Test
    void rDisjointExpr_extendsRBinaryExpression() {
        RDisjointExpr expr = new RDisjointExpr();
        assertInstanceOf(RBinaryExpression.class, expr);
    }

    // =========================================================================
    // RDefaultExpr
    // =========================================================================

    @Test
    void rDefaultExpr_defaultState() {
        RDefaultExpr expr = new RDefaultExpr();
        assertNull(expr.rawLeft());
        assertNull(expr.rawRight());
        assertTrue(expr.children().isEmpty());
    }

    @Test
    void rDefaultExpr_setLeftAndRight() {
        RDefaultExpr expr = new RDefaultExpr();
        StubExpression left = new StubExpression();
        StubExpression right = new StubExpression();
        expr.setLeft(left);
        expr.setRight(right);

        assertSame(left, expr.rawLeft());
        assertSame(right, expr.rawRight());
    }

    @Test
    void rDefaultExpr_childrenWithLeftAndRight() {
        RDefaultExpr expr = new RDefaultExpr();
        StubExpression left = new StubExpression();
        StubExpression right = new StubExpression();
        expr.setLeft(left);
        expr.setRight(right);

        assertEquals(2, expr.children().size());
    }

    @Test
    void rDefaultExpr_extendsRBinaryExpression() {
        RDefaultExpr expr = new RDefaultExpr();
        assertInstanceOf(RBinaryExpression.class, expr);
    }

    // =========================================================================
    // RJoinExpr
    // =========================================================================

    @Test
    void rJoinExpr_defaultState() {
        RJoinExpr expr = new RJoinExpr();
        assertNull(expr.rawLeft());
        assertNull(expr.rawRight());
        assertEquals(Optional.empty(), expr.separator());
        assertTrue(expr.children().isEmpty());
    }

    @Test
    void rJoinExpr_setLeftRightAndSeparator() {
        RJoinExpr expr = new RJoinExpr();
        StubExpression left = new StubExpression();
        StubExpression right = new StubExpression();
        StubExpression sep = new StubExpression();
        expr.setLeft(left);
        expr.setRight(right);
        expr.setSeparator(sep);

        assertSame(left, expr.rawLeft());
        assertSame(right, expr.rawRight());
        assertEquals(Optional.of(sep), expr.separator());
    }

    @Test
    void rJoinExpr_noSeparator() {
        RJoinExpr expr = new RJoinExpr();
        assertEquals(Optional.empty(), expr.separator());
    }

    @Test
    void rJoinExpr_childrenIncludesAll() {
        RJoinExpr expr = new RJoinExpr();
        StubExpression left = new StubExpression();
        StubExpression right = new StubExpression();
        StubExpression sep = new StubExpression();
        expr.setLeft(left);
        expr.setRight(right);
        expr.setSeparator(sep);

        List<? extends RNode> children = expr.children();
        assertEquals(3, children.size());
        assertSame(left, children.get(0));
        assertSame(right, children.get(1));
        assertSame(sep, children.get(2));
    }

    @Test
    void rJoinExpr_childrenWithoutSeparator() {
        RJoinExpr expr = new RJoinExpr();
        StubExpression left = new StubExpression();
        StubExpression right = new StubExpression();
        expr.setLeft(left);
        expr.setRight(right);

        List<? extends RNode> children = expr.children();
        assertEquals(2, children.size());
    }

    @Test
    void rJoinExpr_extendsRBinaryExpression() {
        RJoinExpr expr = new RJoinExpr();
        assertInstanceOf(RBinaryExpression.class, expr);
    }

    // =========================================================================
    // RThenExpr
    // =========================================================================

    @Test
    void rThenExpr_defaultState() {
        RThenExpr expr = new RThenExpr();
        assertNull(expr.argument());
        assertEquals(Optional.empty(), expr.body());
        assertTrue(expr.children().isEmpty());
    }

    @Test
    void rThenExpr_setArgumentAndBody() {
        RThenExpr expr = new RThenExpr();
        StubExpression arg = new StubExpression();
        RInlineFunction body = new RInlineFunction();
        body.setImplicit(true);
        expr.setArgument(arg);
        expr.setBody(body);

        assertSame(arg, expr.argument());
        assertEquals(Optional.of(arg), expr.left());
        assertEquals(Optional.of(body), expr.body());
    }

    @Test
    void rThenExpr_noBody() {
        RThenExpr expr = new RThenExpr();
        assertEquals(Optional.empty(), expr.body());
    }

    @Test
    void rThenExpr_childrenIncludesAll() {
        RThenExpr expr = new RThenExpr();
        StubExpression arg = new StubExpression();
        RInlineFunction body = new RInlineFunction();
        expr.setArgument(arg);
        expr.setBody(body);

        List<? extends RNode> children = expr.children();
        assertEquals(2, children.size());
        assertSame(arg, children.get(0));
        assertSame(body, children.get(1));
    }

    @Test
    void rThenExpr_childrenWithoutBody() {
        RThenExpr expr = new RThenExpr();
        StubExpression arg = new StubExpression();
        expr.setArgument(arg);

        List<? extends RNode> children = expr.children();
        assertEquals(1, children.size());
        assertSame(arg, children.get(0));
    }

    @Test
    void rThenExpr_extendsRExpression() {
        RThenExpr expr = new RThenExpr();
        assertInstanceOf(RExpression.class, expr);
        assertFalse(RBinaryExpression.class.isAssignableFrom(RThenExpr.class));
    }

    // =========================================================================
    // Integration: nested binary expressions
    // =========================================================================

    @Test
    void integration_nestedArithmetic() {
        // (a + b) * c
        StubExpression a = new StubExpression();
        StubExpression b = new StubExpression();
        StubExpression c = new StubExpression();

        RArithmeticExpr add = new RArithmeticExpr();
        add.setOp(ArithOp.PLUS);
        add.setLeft(a);
        add.setRight(b);

        RArithmeticExpr mul = new RArithmeticExpr();
        mul.setOp(ArithOp.MULTIPLY);
        mul.setLeft(add);
        mul.setRight(c);

        assertSame(add, mul.rawLeft());
        assertSame(c, mul.rawRight());
        assertEquals(2, mul.children().size());
        assertEquals(2, add.children().size());
    }

    @Test
    void integration_logicalWithComparisons() {
        // (x > 0) and (y < 10)
        StubExpression x = new StubExpression();
        StubExpression zero = new StubExpression();
        StubExpression y = new StubExpression();
        StubExpression ten = new StubExpression();

        RComparisonExpr gt = new RComparisonExpr();
        gt.setOp(CompOp.GT);
        gt.setLeft(x);
        gt.setRight(zero);

        RComparisonExpr lt = new RComparisonExpr();
        lt.setOp(CompOp.LT);
        lt.setLeft(y);
        lt.setRight(ten);

        RLogicalExpr and = new RLogicalExpr();
        and.setOp(LogOp.AND);
        and.setLeft(gt);
        and.setRight(lt);

        assertEquals(2, and.children().size());
        assertSame(gt, and.children().get(0));
        assertSame(lt, and.children().get(1));
    }

    @Test
    void integration_thenWithInlineFunction() {
        // expr then [x] x + 1
        StubExpression input = new StubExpression();
        StubExpression bodyExpr = new StubExpression();

        RInlineFunction fn = new RInlineFunction();
        fn.paramNames().add("x");
        fn.setImplicit(false);
        fn.setBody(bodyExpr);

        RThenExpr then = new RThenExpr();
        then.setArgument(input);
        then.setBody(fn);

        assertEquals(2, then.children().size());
        assertSame(input, then.children().get(0));
        assertSame(fn, then.children().get(1));
    }
}
