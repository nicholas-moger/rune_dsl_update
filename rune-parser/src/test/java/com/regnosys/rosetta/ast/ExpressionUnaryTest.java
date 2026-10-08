package com.regnosys.rosetta.ast;

import com.regnosys.rosetta.ast.enums.CardCheckOp;
import com.regnosys.rosetta.ast.enums.ConversionKind;
import com.regnosys.rosetta.ast.enums.ExistenceOp;
import com.regnosys.rosetta.ast.enums.ExistsModifier;
import com.regnosys.rosetta.ast.enums.ListOp;
import com.regnosys.rosetta.ast.enums.Necessity;
import com.regnosys.rosetta.ast.enums.SwitchGuardKind;
import com.regnosys.rosetta.ast.expressions.constructors.RConditionalExpr;
import com.regnosys.rosetta.ast.expressions.constructors.RConstructorExpr;
import com.regnosys.rosetta.ast.expressions.supporting.RInlineFunction;
import com.regnosys.rosetta.ast.expressions.supporting.RKeyValuePair;
import com.regnosys.rosetta.ast.expressions.supporting.ROnlyExistsElement;
import com.regnosys.rosetta.ast.expressions.supporting.RSwitchCase;
import com.regnosys.rosetta.ast.expressions.supporting.RSwitchCaseGuard;
import com.regnosys.rosetta.ast.expressions.supporting.RWithMetaEntry;
import com.regnosys.rosetta.ast.expressions.unary.RCardinalityCheckExpr;
import com.regnosys.rosetta.ast.expressions.unary.RConversionExpr;
import com.regnosys.rosetta.ast.expressions.unary.RCountExpr;
import com.regnosys.rosetta.ast.expressions.unary.RExistenceExpr;
import com.regnosys.rosetta.ast.expressions.unary.RExtractExpr;
import com.regnosys.rosetta.ast.expressions.unary.RFilterExpr;
import com.regnosys.rosetta.ast.expressions.unary.RListOpExpr;
import com.regnosys.rosetta.ast.expressions.unary.RMaxExpr;
import com.regnosys.rosetta.ast.expressions.unary.RMinExpr;
import com.regnosys.rosetta.ast.expressions.unary.ROnlyExistsExpr;
import com.regnosys.rosetta.ast.expressions.unary.RReduceExpr;
import com.regnosys.rosetta.ast.expressions.unary.RSortExpr;
import com.regnosys.rosetta.ast.expressions.unary.RSwitchExpr;
import com.regnosys.rosetta.ast.expressions.unary.RToStringExpr;
import com.regnosys.rosetta.ast.expressions.unary.RWithMetaExpr;
import com.regnosys.rosetta.ast.supporting.RTypeCall;
import com.regnosys.rosetta.ast.visitor.RExpressionVisitor;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for unary/postfix expression nodes, supporting expression types,
 * and constructor/control flow nodes (Task 9).
 *
 * <p>These tests construct nodes directly (no parsing) to validate getters,
 * setters, optional fields, list fields, boolean getters, and the
 * {@code children()} traversal.
 */
class ExpressionUnaryTest extends BaseAstTest {

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
    // RExistenceExpr
    // =========================================================================

    @Test
    void rExistenceExpr_defaultState() {
        RExistenceExpr expr = new RExistenceExpr();
        assertNull(expr.argument());
        assertNull(expr.op());
        assertEquals(Optional.empty(), expr.modifier());
        assertEquals(Optional.empty(), expr.left());
        assertTrue(expr.children().isEmpty());
    }

    @Test
    void rExistenceExpr_exists() {
        RExistenceExpr expr = new RExistenceExpr();
        StubExpression arg = new StubExpression();
        expr.setArgument(arg);
        expr.setOp(ExistenceOp.EXISTS);

        assertSame(arg, expr.argument());
        assertEquals(ExistenceOp.EXISTS, expr.op());
        assertEquals(Optional.of(arg), expr.left());
    }

    @Test
    void rExistenceExpr_existsWithModifier() {
        RExistenceExpr expr = new RExistenceExpr();
        expr.setOp(ExistenceOp.EXISTS);
        expr.setModifier(ExistsModifier.SINGLE);

        assertEquals(Optional.of(ExistsModifier.SINGLE), expr.modifier());
    }

    @Test
    void rExistenceExpr_absent() {
        RExistenceExpr expr = new RExistenceExpr();
        StubExpression arg = new StubExpression();
        expr.setArgument(arg);
        expr.setOp(ExistenceOp.ABSENT);

        assertEquals(ExistenceOp.ABSENT, expr.op());
        assertEquals(Optional.empty(), expr.modifier());
    }

    @Test
    void rExistenceExpr_childrenIncludesArgument() {
        RExistenceExpr expr = new RExistenceExpr();
        StubExpression arg = new StubExpression();
        expr.setArgument(arg);
        expr.setOp(ExistenceOp.EXISTS);

        List<? extends RNode> children = expr.children();
        assertEquals(1, children.size());
        assertSame(arg, children.get(0));
    }

    @Test
    void rExistenceExpr_noArgumentEmptyChildren() {
        RExistenceExpr expr = new RExistenceExpr();
        expr.setOp(ExistenceOp.EXISTS);
        assertTrue(expr.children().isEmpty());
    }

    @Test
    void rExistenceExpr_extendsRExpression() {
        RExistenceExpr expr = new RExistenceExpr();
        assertInstanceOf(RExpression.class, expr);
        assertFalse(RBinaryExpression.class.isAssignableFrom(expr.getClass()));
    }

    // =========================================================================
    // ROnlyExistsExpr
    // =========================================================================

    @Test
    void rOnlyExistsExpr_defaultState() {
        ROnlyExistsExpr expr = new ROnlyExistsExpr();
        assertTrue(expr.elements().isEmpty());
        assertEquals(Optional.empty(), expr.left());
        assertTrue(expr.children().isEmpty());
    }

    @Test
    void rOnlyExistsExpr_addElements() {
        ROnlyExistsExpr expr = new ROnlyExistsExpr();
        ROnlyExistsElement el1 = new ROnlyExistsElement();
        el1.setRoot("price");
        el1.setRootIsItem(false);
        ROnlyExistsElement el2 = new ROnlyExistsElement();
        el2.setRoot("quantity");
        el2.setRootIsItem(false);
        expr.elements().add(el1);
        expr.elements().add(el2);

        assertEquals(2, expr.elements().size());
    }

    @Test
    void rOnlyExistsExpr_childrenIncludesElements() {
        ROnlyExistsExpr expr = new ROnlyExistsExpr();
        ROnlyExistsElement el1 = new ROnlyExistsElement();
        el1.setRoot("name");
        el1.setRootIsItem(false);
        ROnlyExistsElement el2 = new ROnlyExistsElement();
        el2.setRoot("price");
        el2.setRootIsItem(false);
        expr.elements().add(el1);
        expr.elements().add(el2);

        List<? extends RNode> children = expr.children();
        assertEquals(2, children.size());
        assertSame(el1, children.get(0));
        assertSame(el2, children.get(1));
    }

    @Test
    void rOnlyExistsExpr_extendsRExpression() {
        ROnlyExistsExpr expr = new ROnlyExistsExpr();
        assertInstanceOf(RExpression.class, expr);
        assertFalse(RBinaryExpression.class.isAssignableFrom(expr.getClass()));
    }

    // =========================================================================
    // RCountExpr
    // =========================================================================

    @Test
    void rCountExpr_defaultState() {
        RCountExpr expr = new RCountExpr();
        assertNull(expr.argument());
        assertEquals(Optional.empty(), expr.left());
        assertTrue(expr.children().isEmpty());
    }

    @Test
    void rCountExpr_setArgument() {
        RCountExpr expr = new RCountExpr();
        StubExpression arg = new StubExpression();
        expr.setArgument(arg);

        assertSame(arg, expr.argument());
        assertEquals(Optional.of(arg), expr.left());
    }

    @Test
    void rCountExpr_childrenIncludesArgument() {
        RCountExpr expr = new RCountExpr();
        StubExpression arg = new StubExpression();
        expr.setArgument(arg);

        List<? extends RNode> children = expr.children();
        assertEquals(1, children.size());
        assertSame(arg, children.get(0));
    }

    @Test
    void rCountExpr_extendsRExpression() {
        RCountExpr expr = new RCountExpr();
        assertInstanceOf(RExpression.class, expr);
        assertFalse(RBinaryExpression.class.isAssignableFrom(expr.getClass()));
    }

    // =========================================================================
    // RListOpExpr
    // =========================================================================

    @Test
    void rListOpExpr_defaultState() {
        RListOpExpr expr = new RListOpExpr();
        assertNull(expr.argument());
        assertNull(expr.op());
        assertEquals(Optional.empty(), expr.left());
        assertTrue(expr.children().isEmpty());
    }

    @Test
    void rListOpExpr_setOp() {
        RListOpExpr expr = new RListOpExpr();
        expr.setOp(ListOp.DISTINCT);
        assertEquals(ListOp.DISTINCT, expr.op());
    }

    @Test
    void rListOpExpr_allOps() {
        for (ListOp op : ListOp.values()) {
            RListOpExpr expr = new RListOpExpr();
            expr.setOp(op);
            assertEquals(op, expr.op());
        }
    }

    @Test
    void rListOpExpr_childrenIncludesArgument() {
        RListOpExpr expr = new RListOpExpr();
        StubExpression arg = new StubExpression();
        expr.setArgument(arg);
        expr.setOp(ListOp.FLATTEN);

        List<? extends RNode> children = expr.children();
        assertEquals(1, children.size());
        assertSame(arg, children.get(0));
    }

    @Test
    void rListOpExpr_extendsRExpression() {
        RListOpExpr expr = new RListOpExpr();
        assertInstanceOf(RExpression.class, expr);
        assertFalse(RBinaryExpression.class.isAssignableFrom(expr.getClass()));
    }

    // =========================================================================
    // RCardinalityCheckExpr
    // =========================================================================

    @Test
    void rCardinalityCheckExpr_defaultState() {
        RCardinalityCheckExpr expr = new RCardinalityCheckExpr();
        assertNull(expr.argument());
        assertNull(expr.op());
        assertEquals(Optional.empty(), expr.necessity());
        assertTrue(expr.attributes().isEmpty());
        assertEquals(Optional.empty(), expr.left());
        assertTrue(expr.children().isEmpty());
    }

    @Test
    void rCardinalityCheckExpr_oneOf() {
        RCardinalityCheckExpr expr = new RCardinalityCheckExpr();
        StubExpression arg = new StubExpression();
        expr.setArgument(arg);
        expr.setOp(CardCheckOp.ONE_OF);

        assertEquals(CardCheckOp.ONE_OF, expr.op());
        assertEquals(Optional.empty(), expr.necessity());
        assertTrue(expr.attributes().isEmpty());
    }

    @Test
    void rCardinalityCheckExpr_choice() {
        RCardinalityCheckExpr expr = new RCardinalityCheckExpr();
        StubExpression arg = new StubExpression();
        expr.setArgument(arg);
        expr.setOp(CardCheckOp.CHOICE);
        expr.setNecessity(Necessity.REQUIRED);
        expr.attributes().add("attr1");
        expr.attributes().add("attr2");

        assertEquals(CardCheckOp.CHOICE, expr.op());
        assertEquals(Optional.of(Necessity.REQUIRED), expr.necessity());
        assertEquals(2, expr.attributes().size());
        assertEquals("attr1", expr.attributes().get(0));
        assertEquals("attr2", expr.attributes().get(1));
    }

    @Test
    void rCardinalityCheckExpr_childrenIncludesArgument() {
        RCardinalityCheckExpr expr = new RCardinalityCheckExpr();
        StubExpression arg = new StubExpression();
        expr.setArgument(arg);
        expr.setOp(CardCheckOp.ONE_OF);

        List<? extends RNode> children = expr.children();
        assertEquals(1, children.size());
        assertSame(arg, children.get(0));
    }

    @Test
    void rCardinalityCheckExpr_extendsRExpression() {
        RCardinalityCheckExpr expr = new RCardinalityCheckExpr();
        assertInstanceOf(RExpression.class, expr);
        assertFalse(RBinaryExpression.class.isAssignableFrom(expr.getClass()));
    }

    // =========================================================================
    // RConversionExpr
    // =========================================================================

    @Test
    void rConversionExpr_defaultState() {
        RConversionExpr expr = new RConversionExpr();
        assertNull(expr.argument());
        assertNull(expr.kind());
        assertEquals(Optional.empty(), expr.left());
        assertTrue(expr.children().isEmpty());
    }

    @Test
    void rConversionExpr_setKind() {
        RConversionExpr expr = new RConversionExpr();
        expr.setKind(ConversionKind.NUMBER);
        assertEquals(ConversionKind.NUMBER, expr.kind());
    }

    @Test
    void rConversionExpr_allKinds() {
        for (ConversionKind kind : ConversionKind.values()) {
            RConversionExpr expr = new RConversionExpr();
            expr.setKind(kind);
            assertEquals(kind, expr.kind());
        }
    }

    @Test
    void rConversionExpr_childrenIncludesArgument() {
        RConversionExpr expr = new RConversionExpr();
        StubExpression arg = new StubExpression();
        expr.setArgument(arg);
        expr.setKind(ConversionKind.DATE);

        List<? extends RNode> children = expr.children();
        assertEquals(1, children.size());
        assertSame(arg, children.get(0));
    }

    @Test
    void rConversionExpr_extendsRExpression() {
        RConversionExpr expr = new RConversionExpr();
        assertInstanceOf(RExpression.class, expr);
        assertFalse(RBinaryExpression.class.isAssignableFrom(expr.getClass()));
    }

    // =========================================================================
    // RToStringExpr
    // =========================================================================

    @Test
    void rToStringExpr_defaultState() {
        RToStringExpr expr = new RToStringExpr();
        assertNull(expr.argument());
        assertEquals(Optional.empty(), expr.left());
        assertTrue(expr.children().isEmpty());
    }

    @Test
    void rToStringExpr_setArgument() {
        RToStringExpr expr = new RToStringExpr();
        StubExpression arg = new StubExpression();
        expr.setArgument(arg);

        assertSame(arg, expr.argument());
        assertEquals(Optional.of(arg), expr.left());
    }

    @Test
    void rToStringExpr_childrenIncludesArgument() {
        RToStringExpr expr = new RToStringExpr();
        StubExpression arg = new StubExpression();
        expr.setArgument(arg);

        List<? extends RNode> children = expr.children();
        assertEquals(1, children.size());
        assertSame(arg, children.get(0));
    }

    @Test
    void rToStringExpr_extendsRExpression() {
        RToStringExpr expr = new RToStringExpr();
        assertInstanceOf(RExpression.class, expr);
        assertFalse(RBinaryExpression.class.isAssignableFrom(expr.getClass()));
    }

    // =========================================================================
    // RSwitchExpr
    // =========================================================================

    @Test
    void rSwitchExpr_defaultState() {
        RSwitchExpr expr = new RSwitchExpr();
        assertNull(expr.argument());
        assertTrue(expr.cases().isEmpty());
        assertEquals(Optional.empty(), expr.left());
        assertTrue(expr.children().isEmpty());
    }

    @Test
    void rSwitchExpr_addCases() {
        RSwitchExpr expr = new RSwitchExpr();
        RSwitchCase c1 = new RSwitchCase();
        c1.setDefault(false);
        RSwitchCase c2 = new RSwitchCase();
        c2.setDefault(true);
        expr.cases().add(c1);
        expr.cases().add(c2);

        assertEquals(2, expr.cases().size());
    }

    @Test
    void rSwitchExpr_childrenIncludesArgumentAndCases() {
        RSwitchExpr expr = new RSwitchExpr();
        StubExpression arg = new StubExpression();
        expr.setArgument(arg);
        RSwitchCase c = new RSwitchCase();
        c.setDefault(true);
        StubExpression caseExpr = new StubExpression();
        c.setExpression(caseExpr);
        expr.cases().add(c);

        List<? extends RNode> children = expr.children();
        assertEquals(2, children.size());
        assertSame(arg, children.get(0));
        assertSame(c, children.get(1));
    }

    @Test
    void rSwitchExpr_childrenWithoutArgument() {
        RSwitchExpr expr = new RSwitchExpr();
        RSwitchCase c = new RSwitchCase();
        c.setDefault(true);
        expr.cases().add(c);

        List<? extends RNode> children = expr.children();
        assertEquals(1, children.size());
        assertSame(c, children.get(0));
    }

    @Test
    void rSwitchExpr_extendsRExpression() {
        RSwitchExpr expr = new RSwitchExpr();
        assertInstanceOf(RExpression.class, expr);
        assertFalse(RBinaryExpression.class.isAssignableFrom(expr.getClass()));
    }

    // =========================================================================
    // RWithMetaExpr
    // =========================================================================

    @Test
    void rWithMetaExpr_defaultState() {
        RWithMetaExpr expr = new RWithMetaExpr();
        assertNull(expr.argument());
        assertTrue(expr.entries().isEmpty());
        assertEquals(Optional.empty(), expr.left());
        assertTrue(expr.children().isEmpty());
    }

    @Test
    void rWithMetaExpr_addEntries() {
        RWithMetaExpr expr = new RWithMetaExpr();
        RWithMetaEntry e1 = new RWithMetaEntry();
        e1.setMetaName("scheme");
        RWithMetaEntry e2 = new RWithMetaEntry();
        e2.setMetaName("key");
        expr.entries().add(e1);
        expr.entries().add(e2);

        assertEquals(2, expr.entries().size());
    }

    @Test
    void rWithMetaExpr_childrenIncludesArgumentAndEntries() {
        RWithMetaExpr expr = new RWithMetaExpr();
        StubExpression arg = new StubExpression();
        expr.setArgument(arg);
        RWithMetaEntry entry = new RWithMetaEntry();
        entry.setMetaName("scheme");
        expr.entries().add(entry);

        List<? extends RNode> children = expr.children();
        assertEquals(2, children.size());
        assertSame(arg, children.get(0));
        assertSame(entry, children.get(1));
    }

    @Test
    void rWithMetaExpr_childrenWithoutArgument() {
        RWithMetaExpr expr = new RWithMetaExpr();
        RWithMetaEntry entry = new RWithMetaEntry();
        entry.setMetaName("scheme");
        expr.entries().add(entry);

        List<? extends RNode> children = expr.children();
        assertEquals(1, children.size());
        assertSame(entry, children.get(0));
    }

    @Test
    void rWithMetaExpr_extendsRExpression() {
        RWithMetaExpr expr = new RWithMetaExpr();
        assertInstanceOf(RExpression.class, expr);
        assertFalse(RBinaryExpression.class.isAssignableFrom(expr.getClass()));
    }

    // =========================================================================
    // RSortExpr
    // =========================================================================

    @Test
    void rSortExpr_defaultState() {
        RSortExpr expr = new RSortExpr();
        assertNull(expr.argument());
        assertEquals(Optional.empty(), expr.body());
        assertEquals(Optional.empty(), expr.left());
        assertTrue(expr.children().isEmpty());
    }

    @Test
    void rSortExpr_setArgumentAndBody() {
        RSortExpr expr = new RSortExpr();
        StubExpression arg = new StubExpression();
        RInlineFunction body = new RInlineFunction();
        expr.setArgument(arg);
        expr.setBody(body);

        assertSame(arg, expr.argument());
        assertEquals(Optional.of(body), expr.body());
    }

    @Test
    void rSortExpr_noBody() {
        RSortExpr expr = new RSortExpr();
        StubExpression arg = new StubExpression();
        expr.setArgument(arg);

        assertEquals(Optional.empty(), expr.body());
    }

    @Test
    void rSortExpr_childrenIncludesArgumentAndBody() {
        RSortExpr expr = new RSortExpr();
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
    void rSortExpr_childrenArgumentOnly() {
        RSortExpr expr = new RSortExpr();
        StubExpression arg = new StubExpression();
        expr.setArgument(arg);

        List<? extends RNode> children = expr.children();
        assertEquals(1, children.size());
        assertSame(arg, children.get(0));
    }

    @Test
    void rSortExpr_extendsRExpression() {
        RSortExpr expr = new RSortExpr();
        assertInstanceOf(RExpression.class, expr);
        assertFalse(RBinaryExpression.class.isAssignableFrom(expr.getClass()));
    }

    // =========================================================================
    // RMinExpr
    // =========================================================================

    @Test
    void rMinExpr_defaultState() {
        RMinExpr expr = new RMinExpr();
        assertNull(expr.argument());
        assertEquals(Optional.empty(), expr.body());
        assertEquals(Optional.empty(), expr.left());
        assertTrue(expr.children().isEmpty());
    }

    @Test
    void rMinExpr_setArgumentAndBody() {
        RMinExpr expr = new RMinExpr();
        StubExpression arg = new StubExpression();
        RInlineFunction body = new RInlineFunction();
        expr.setArgument(arg);
        expr.setBody(body);

        assertSame(arg, expr.argument());
        assertEquals(Optional.of(body), expr.body());
    }

    @Test
    void rMinExpr_childrenIncludesAll() {
        RMinExpr expr = new RMinExpr();
        StubExpression arg = new StubExpression();
        RInlineFunction body = new RInlineFunction();
        expr.setArgument(arg);
        expr.setBody(body);

        assertEquals(2, expr.children().size());
    }

    @Test
    void rMinExpr_extendsRExpression() {
        RMinExpr expr = new RMinExpr();
        assertInstanceOf(RExpression.class, expr);
    }

    // =========================================================================
    // RMaxExpr
    // =========================================================================

    @Test
    void rMaxExpr_defaultState() {
        RMaxExpr expr = new RMaxExpr();
        assertNull(expr.argument());
        assertEquals(Optional.empty(), expr.body());
        assertEquals(Optional.empty(), expr.left());
        assertTrue(expr.children().isEmpty());
    }

    @Test
    void rMaxExpr_setArgumentAndBody() {
        RMaxExpr expr = new RMaxExpr();
        StubExpression arg = new StubExpression();
        RInlineFunction body = new RInlineFunction();
        expr.setArgument(arg);
        expr.setBody(body);

        assertSame(arg, expr.argument());
        assertEquals(Optional.of(body), expr.body());
    }

    @Test
    void rMaxExpr_childrenIncludesAll() {
        RMaxExpr expr = new RMaxExpr();
        StubExpression arg = new StubExpression();
        RInlineFunction body = new RInlineFunction();
        expr.setArgument(arg);
        expr.setBody(body);

        assertEquals(2, expr.children().size());
    }

    @Test
    void rMaxExpr_extendsRExpression() {
        RMaxExpr expr = new RMaxExpr();
        assertInstanceOf(RExpression.class, expr);
    }

    // =========================================================================
    // RFilterExpr
    // =========================================================================

    @Test
    void rFilterExpr_defaultState() {
        RFilterExpr expr = new RFilterExpr();
        assertNull(expr.argument());
        assertNull(expr.body());
        assertEquals(Optional.empty(), expr.left());
        assertTrue(expr.children().isEmpty());
    }

    @Test
    void rFilterExpr_setArgumentAndBody() {
        RFilterExpr expr = new RFilterExpr();
        StubExpression arg = new StubExpression();
        RInlineFunction body = new RInlineFunction();
        body.setImplicit(true);
        expr.setArgument(arg);
        expr.setBody(body);

        assertSame(arg, expr.argument());
        assertSame(body, expr.body());
    }

    @Test
    void rFilterExpr_childrenIncludesAll() {
        RFilterExpr expr = new RFilterExpr();
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
    void rFilterExpr_extendsRExpression() {
        RFilterExpr expr = new RFilterExpr();
        assertInstanceOf(RExpression.class, expr);
        assertFalse(RBinaryExpression.class.isAssignableFrom(expr.getClass()));
    }

    // =========================================================================
    // RExtractExpr
    // =========================================================================

    @Test
    void rExtractExpr_defaultState() {
        RExtractExpr expr = new RExtractExpr();
        assertNull(expr.argument());
        assertNull(expr.body());
        assertEquals(Optional.empty(), expr.left());
        assertTrue(expr.children().isEmpty());
    }

    @Test
    void rExtractExpr_setArgumentAndBody() {
        RExtractExpr expr = new RExtractExpr();
        StubExpression arg = new StubExpression();
        RInlineFunction body = new RInlineFunction();
        expr.setArgument(arg);
        expr.setBody(body);

        assertSame(arg, expr.argument());
        assertSame(body, expr.body());
    }

    @Test
    void rExtractExpr_childrenIncludesAll() {
        RExtractExpr expr = new RExtractExpr();
        StubExpression arg = new StubExpression();
        RInlineFunction body = new RInlineFunction();
        expr.setArgument(arg);
        expr.setBody(body);

        assertEquals(2, expr.children().size());
    }

    @Test
    void rExtractExpr_extendsRExpression() {
        RExtractExpr expr = new RExtractExpr();
        assertInstanceOf(RExpression.class, expr);
    }

    // =========================================================================
    // RReduceExpr
    // =========================================================================

    @Test
    void rReduceExpr_defaultState() {
        RReduceExpr expr = new RReduceExpr();
        assertNull(expr.argument());
        assertNull(expr.body());
        assertEquals(Optional.empty(), expr.left());
        assertTrue(expr.children().isEmpty());
    }

    @Test
    void rReduceExpr_setArgumentAndBody() {
        RReduceExpr expr = new RReduceExpr();
        StubExpression arg = new StubExpression();
        RInlineFunction body = new RInlineFunction();
        expr.setArgument(arg);
        expr.setBody(body);

        assertSame(arg, expr.argument());
        assertSame(body, expr.body());
    }

    @Test
    void rReduceExpr_childrenIncludesAll() {
        RReduceExpr expr = new RReduceExpr();
        StubExpression arg = new StubExpression();
        RInlineFunction body = new RInlineFunction();
        expr.setArgument(arg);
        expr.setBody(body);

        assertEquals(2, expr.children().size());
    }

    @Test
    void rReduceExpr_extendsRExpression() {
        RReduceExpr expr = new RReduceExpr();
        assertInstanceOf(RExpression.class, expr);
    }

    // =========================================================================
    // Supporting: RInlineFunction
    // =========================================================================

    @Test
    void rInlineFunction_defaultState() {
        RInlineFunction fn = new RInlineFunction();
        assertTrue(fn.paramNames().isEmpty());
        assertFalse(fn.isImplicit());
        assertNull(fn.body());
        assertTrue(fn.children().isEmpty());
    }

    @Test
    void rInlineFunction_explicit() {
        RInlineFunction fn = new RInlineFunction();
        fn.paramNames().add("x");
        fn.paramNames().add("y");
        fn.setImplicit(false);
        StubExpression body = new StubExpression();
        fn.setBody(body);

        assertEquals(2, fn.paramNames().size());
        assertEquals("x", fn.paramNames().get(0));
        assertEquals("y", fn.paramNames().get(1));
        assertFalse(fn.isImplicit());
        assertSame(body, fn.body());
    }

    @Test
    void rInlineFunction_implicit() {
        RInlineFunction fn = new RInlineFunction();
        fn.setImplicit(true);
        StubExpression body = new StubExpression();
        fn.setBody(body);

        assertTrue(fn.isImplicit());
        assertTrue(fn.paramNames().isEmpty());
    }

    @Test
    void rInlineFunction_childrenIncludesBody() {
        RInlineFunction fn = new RInlineFunction();
        StubExpression body = new StubExpression();
        fn.setBody(body);

        List<? extends RNode> children = fn.children();
        assertEquals(1, children.size());
        assertSame(body, children.get(0));
    }

    @Test
    void rInlineFunction_noBodyEmptyChildren() {
        RInlineFunction fn = new RInlineFunction();
        assertTrue(fn.children().isEmpty());
    }

    @Test
    void rInlineFunction_extendsRNode() {
        RInlineFunction fn = new RInlineFunction();
        assertInstanceOf(RNode.class, fn);
        assertFalse(RExpression.class.isAssignableFrom(fn.getClass()));
    }

    // =========================================================================
    // Supporting: RSwitchCase
    // =========================================================================

    @Test
    void rSwitchCase_defaultState() {
        RSwitchCase sc = new RSwitchCase();
        assertFalse(sc.isDefault());
        assertEquals(Optional.empty(), sc.guard());
        assertNull(sc.expression());
        assertTrue(sc.children().isEmpty());
    }

    @Test
    void rSwitchCase_guardedCase() {
        RSwitchCase sc = new RSwitchCase();
        sc.setDefault(false);
        RSwitchCaseGuard guard = new RSwitchCaseGuard();
        guard.setKind(SwitchGuardKind.LITERAL);
        guard.setLiteralValue("42");
        sc.setGuard(guard);
        StubExpression expr = new StubExpression();
        sc.setExpression(expr);

        assertFalse(sc.isDefault());
        assertEquals(Optional.of(guard), sc.guard());
        assertSame(expr, sc.expression());
    }

    @Test
    void rSwitchCase_defaultCase() {
        RSwitchCase sc = new RSwitchCase();
        sc.setDefault(true);
        StubExpression expr = new StubExpression();
        sc.setExpression(expr);

        assertTrue(sc.isDefault());
        assertEquals(Optional.empty(), sc.guard());
    }

    @Test
    void rSwitchCase_childrenIncludesGuardAndExpression() {
        RSwitchCase sc = new RSwitchCase();
        RSwitchCaseGuard guard = new RSwitchCaseGuard();
        guard.setKind(SwitchGuardKind.NAME);
        guard.setQualifiedName("MyEnum.Value1");
        sc.setGuard(guard);
        StubExpression expr = new StubExpression();
        sc.setExpression(expr);

        List<? extends RNode> children = sc.children();
        assertEquals(2, children.size());
        assertSame(guard, children.get(0));
        assertSame(expr, children.get(1));
    }

    @Test
    void rSwitchCase_childrenDefaultCaseNoGuard() {
        RSwitchCase sc = new RSwitchCase();
        sc.setDefault(true);
        StubExpression expr = new StubExpression();
        sc.setExpression(expr);

        List<? extends RNode> children = sc.children();
        assertEquals(1, children.size());
        assertSame(expr, children.get(0));
    }

    @Test
    void rSwitchCase_extendsRNode() {
        RSwitchCase sc = new RSwitchCase();
        assertInstanceOf(RNode.class, sc);
        assertFalse(RExpression.class.isAssignableFrom(sc.getClass()));
    }

    // =========================================================================
    // Supporting: RSwitchCaseGuard
    // =========================================================================

    @Test
    void rSwitchCaseGuard_defaultState() {
        RSwitchCaseGuard g = new RSwitchCaseGuard();
        assertNull(g.kind());
        assertEquals(Optional.empty(), g.literalValue());
        assertEquals(Optional.empty(), g.literalKind());
        assertEquals(Optional.empty(), g.qualifiedName());
    }

    @Test
    void rSwitchCaseGuard_literalGuard() {
        RSwitchCaseGuard g = new RSwitchCaseGuard();
        g.setKind(SwitchGuardKind.LITERAL);
        g.setLiteralValue("true");

        assertEquals(SwitchGuardKind.LITERAL, g.kind());
        assertEquals(Optional.of("true"), g.literalValue());
        assertEquals(Optional.empty(), g.qualifiedName());
    }

    @Test
    void rSwitchCaseGuard_nameKind() {
        RSwitchCaseGuard g = new RSwitchCaseGuard();
        g.setKind(SwitchGuardKind.NAME);
        g.setQualifiedName("TradeTypeEnum.NewTrade");

        assertEquals(SwitchGuardKind.NAME, g.kind());
        assertEquals(Optional.empty(), g.literalValue());
        assertEquals(Optional.of("TradeTypeEnum.NewTrade"), g.qualifiedName());
    }

    @Test
    void rSwitchCaseGuard_isLeafNode() {
        RSwitchCaseGuard g = new RSwitchCaseGuard();
        g.setKind(SwitchGuardKind.LITERAL);
        assertTrue(g.children().isEmpty());
    }

    @Test
    void rSwitchCaseGuard_extendsRNode() {
        RSwitchCaseGuard g = new RSwitchCaseGuard();
        assertInstanceOf(RNode.class, g);
        assertFalse(RExpression.class.isAssignableFrom(g.getClass()));
    }

    // =========================================================================
    // Supporting: RWithMetaEntry
    // =========================================================================

    @Test
    void rWithMetaEntry_defaultState() {
        RWithMetaEntry e = new RWithMetaEntry();
        assertNull(e.metaName());
        assertNull(e.value());
        assertTrue(e.children().isEmpty());
    }

    @Test
    void rWithMetaEntry_setMetaNameAndValue() {
        RWithMetaEntry e = new RWithMetaEntry();
        e.setMetaName("scheme");
        StubExpression val = new StubExpression();
        e.setValue(val);

        assertEquals("scheme", e.metaName());
        assertSame(val, e.value());
    }

    @Test
    void rWithMetaEntry_childrenIncludesValue() {
        RWithMetaEntry e = new RWithMetaEntry();
        e.setMetaName("key");
        StubExpression val = new StubExpression();
        e.setValue(val);

        List<? extends RNode> children = e.children();
        assertEquals(1, children.size());
        assertSame(val, children.get(0));
    }

    @Test
    void rWithMetaEntry_noValueEmptyChildren() {
        RWithMetaEntry e = new RWithMetaEntry();
        e.setMetaName("key");
        assertTrue(e.children().isEmpty());
    }

    @Test
    void rWithMetaEntry_extendsRNode() {
        RWithMetaEntry e = new RWithMetaEntry();
        assertInstanceOf(RNode.class, e);
        assertFalse(RExpression.class.isAssignableFrom(e.getClass()));
    }

    // =========================================================================
    // Supporting: RKeyValuePair
    // =========================================================================

    @Test
    void rKeyValuePair_defaultState() {
        RKeyValuePair kvp = new RKeyValuePair();
        assertNull(kvp.key());
        assertNull(kvp.value());
        assertFalse(kvp.isAsKey());
        assertTrue(kvp.children().isEmpty());
    }

    @Test
    void rKeyValuePair_setKeyAndValue() {
        RKeyValuePair kvp = new RKeyValuePair();
        kvp.setKey("price");
        StubExpression val = new StubExpression();
        kvp.setValue(val);

        assertEquals("price", kvp.key());
        assertSame(val, kvp.value());
    }

    @Test
    void rKeyValuePair_setAsKey() {
        RKeyValuePair kvp = new RKeyValuePair();
        kvp.setKey("partyId");
        kvp.setAsKey(true);

        assertTrue(kvp.isAsKey());
    }

    @Test
    void rKeyValuePair_childrenIncludesValue() {
        RKeyValuePair kvp = new RKeyValuePair();
        kvp.setKey("name");
        StubExpression val = new StubExpression();
        kvp.setValue(val);

        List<? extends RNode> children = kvp.children();
        assertEquals(1, children.size());
        assertSame(val, children.get(0));
    }

    @Test
    void rKeyValuePair_noValueEmptyChildren() {
        RKeyValuePair kvp = new RKeyValuePair();
        kvp.setKey("name");
        assertTrue(kvp.children().isEmpty());
    }

    @Test
    void rKeyValuePair_extendsRNode() {
        RKeyValuePair kvp = new RKeyValuePair();
        assertInstanceOf(RNode.class, kvp);
        assertFalse(RExpression.class.isAssignableFrom(kvp.getClass()));
    }

    // =========================================================================
    // Supporting: ROnlyExistsElement
    // =========================================================================

    @Test
    void rOnlyExistsElement_defaultState() {
        ROnlyExistsElement el = new ROnlyExistsElement();
        assertNull(el.root());
        assertFalse(el.isRootItem());
        assertTrue(el.featureChain().isEmpty());
        assertTrue(el.children().isEmpty());
    }

    @Test
    void rOnlyExistsElement_namedRoot() {
        ROnlyExistsElement el = new ROnlyExistsElement();
        el.setRoot("trade");
        el.setRootIsItem(false);

        assertEquals("trade", el.root());
        assertFalse(el.isRootItem());
    }

    @Test
    void rOnlyExistsElement_itemRoot() {
        ROnlyExistsElement el = new ROnlyExistsElement();
        el.setRoot("item");
        el.setRootIsItem(true);

        assertEquals("item", el.root());
        assertTrue(el.isRootItem());
    }

    @Test
    void rOnlyExistsElement_featureChain() {
        ROnlyExistsElement el = new ROnlyExistsElement();
        el.setRoot("trade");
        el.setRootIsItem(false);
        el.featureChain().add("price");
        el.featureChain().add("amount");

        assertEquals(2, el.featureChain().size());
        assertEquals("price", el.featureChain().get(0));
        assertEquals("amount", el.featureChain().get(1));
    }

    @Test
    void rOnlyExistsElement_isLeafNode() {
        ROnlyExistsElement el = new ROnlyExistsElement();
        el.setRoot("trade");
        el.setRootIsItem(false);
        assertTrue(el.children().isEmpty());
    }

    @Test
    void rOnlyExistsElement_extendsRNode() {
        ROnlyExistsElement el = new ROnlyExistsElement();
        assertInstanceOf(RNode.class, el);
        assertFalse(RExpression.class.isAssignableFrom(el.getClass()));
    }

    // =========================================================================
    // RConstructorExpr
    // =========================================================================

    @Test
    void rConstructorExpr_defaultState() {
        RConstructorExpr expr = new RConstructorExpr();
        assertNull(expr.typeCall());
        assertTrue(expr.pairs().isEmpty());
        assertFalse(expr.isSpread());
        assertTrue(expr.children().isEmpty());
        assertEquals(Optional.empty(), expr.left());
    }

    @Test
    void rConstructorExpr_setTypeCall() {
        RConstructorExpr expr = new RConstructorExpr();
        RTypeCall tc = new RTypeCall();
        tc.setTypeName("Party");
        expr.setTypeCall(tc);

        assertSame(tc, expr.typeCall());
    }

    @Test
    void rConstructorExpr_addPairs() {
        RConstructorExpr expr = new RConstructorExpr();
        RKeyValuePair p1 = new RKeyValuePair();
        p1.setKey("name");
        RKeyValuePair p2 = new RKeyValuePair();
        p2.setKey("role");
        expr.pairs().add(p1);
        expr.pairs().add(p2);

        assertEquals(2, expr.pairs().size());
        assertEquals("name", expr.pairs().get(0).key());
        assertEquals("role", expr.pairs().get(1).key());
    }

    @Test
    void rConstructorExpr_setSpread() {
        RConstructorExpr expr = new RConstructorExpr();
        expr.setSpread(true);
        assertTrue(expr.isSpread());
    }

    @Test
    void rConstructorExpr_childrenIncludesTypeCallAndPairs() {
        RConstructorExpr expr = new RConstructorExpr();
        RTypeCall tc = new RTypeCall();
        tc.setTypeName("Party");
        expr.setTypeCall(tc);
        RKeyValuePair p = new RKeyValuePair();
        p.setKey("name");
        expr.pairs().add(p);

        List<? extends RNode> children = expr.children();
        assertEquals(2, children.size());
        assertSame(tc, children.get(0));
        assertSame(p, children.get(1));
    }

    @Test
    void rConstructorExpr_childrenWithoutTypeCall() {
        RConstructorExpr expr = new RConstructorExpr();
        RKeyValuePair p = new RKeyValuePair();
        p.setKey("name");
        expr.pairs().add(p);

        List<? extends RNode> children = expr.children();
        assertEquals(1, children.size());
        assertSame(p, children.get(0));
    }

    @Test
    void rConstructorExpr_extendsRExpression() {
        RConstructorExpr expr = new RConstructorExpr();
        assertInstanceOf(RExpression.class, expr);
        assertFalse(RBinaryExpression.class.isAssignableFrom(expr.getClass()));
    }

    // =========================================================================
    // RConditionalExpr
    // =========================================================================

    @Test
    void rConditionalExpr_defaultState() {
        RConditionalExpr expr = new RConditionalExpr();
        assertNull(expr.condition());
        assertNull(expr.thenBranch());
        assertEquals(Optional.empty(), expr.elseBranch());
        assertTrue(expr.children().isEmpty());
        assertEquals(Optional.empty(), expr.left());
    }

    @Test
    void rConditionalExpr_ifThen() {
        RConditionalExpr expr = new RConditionalExpr();
        StubExpression cond = new StubExpression();
        StubExpression then = new StubExpression();
        expr.setCondition(cond);
        expr.setThenBranch(then);

        assertSame(cond, expr.condition());
        assertSame(then, expr.thenBranch());
        assertEquals(Optional.empty(), expr.elseBranch());
    }

    @Test
    void rConditionalExpr_ifThenElse() {
        RConditionalExpr expr = new RConditionalExpr();
        StubExpression cond = new StubExpression();
        StubExpression then = new StubExpression();
        StubExpression elseExpr = new StubExpression();
        expr.setCondition(cond);
        expr.setThenBranch(then);
        expr.setElseBranch(elseExpr);

        assertEquals(Optional.of(elseExpr), expr.elseBranch());
    }

    @Test
    void rConditionalExpr_childrenIfThen() {
        RConditionalExpr expr = new RConditionalExpr();
        StubExpression cond = new StubExpression();
        StubExpression then = new StubExpression();
        expr.setCondition(cond);
        expr.setThenBranch(then);

        List<? extends RNode> children = expr.children();
        assertEquals(2, children.size());
        assertSame(cond, children.get(0));
        assertSame(then, children.get(1));
    }

    @Test
    void rConditionalExpr_childrenIfThenElse() {
        RConditionalExpr expr = new RConditionalExpr();
        StubExpression cond = new StubExpression();
        StubExpression then = new StubExpression();
        StubExpression elseExpr = new StubExpression();
        expr.setCondition(cond);
        expr.setThenBranch(then);
        expr.setElseBranch(elseExpr);

        List<? extends RNode> children = expr.children();
        assertEquals(3, children.size());
        assertSame(cond, children.get(0));
        assertSame(then, children.get(1));
        assertSame(elseExpr, children.get(2));
    }

    @Test
    void rConditionalExpr_extendsRExpression() {
        RConditionalExpr expr = new RConditionalExpr();
        assertInstanceOf(RExpression.class, expr);
        assertFalse(RBinaryExpression.class.isAssignableFrom(expr.getClass()));
    }

    // =========================================================================
    // Integration: complex expression tree
    // =========================================================================

    @Test
    void integration_switchWithGuardedAndDefaultCases() {
        // myVar switch:
        //   "yes" then True,
        //   TradeType.New then False,
        //   default then empty
        RSwitchExpr switchExpr = new RSwitchExpr();
        StubExpression arg = new StubExpression();
        switchExpr.setArgument(arg);

        // Literal guard case
        RSwitchCase c1 = new RSwitchCase();
        c1.setDefault(false);
        RSwitchCaseGuard g1 = new RSwitchCaseGuard();
        g1.setKind(SwitchGuardKind.LITERAL);
        g1.setLiteralValue("yes");
        c1.setGuard(g1);
        StubExpression c1Expr = new StubExpression();
        c1.setExpression(c1Expr);

        // Named guard case
        RSwitchCase c2 = new RSwitchCase();
        c2.setDefault(false);
        RSwitchCaseGuard g2 = new RSwitchCaseGuard();
        g2.setKind(SwitchGuardKind.NAME);
        g2.setQualifiedName("TradeType.New");
        c2.setGuard(g2);
        StubExpression c2Expr = new StubExpression();
        c2.setExpression(c2Expr);

        // Default case
        RSwitchCase c3 = new RSwitchCase();
        c3.setDefault(true);
        StubExpression c3Expr = new StubExpression();
        c3.setExpression(c3Expr);

        switchExpr.cases().add(c1);
        switchExpr.cases().add(c2);
        switchExpr.cases().add(c3);

        // Verify structure
        assertEquals(3, switchExpr.cases().size());
        assertFalse(switchExpr.cases().get(0).isDefault());
        assertFalse(switchExpr.cases().get(1).isDefault());
        assertTrue(switchExpr.cases().get(2).isDefault());

        // Verify children: arg + 3 cases
        List<? extends RNode> children = switchExpr.children();
        assertEquals(4, children.size());
        assertSame(arg, children.get(0));
    }

    @Test
    void integration_constructorWithPairs() {
        // Party { name: "ACME", id: 42 as-key }
        RConstructorExpr ctor = new RConstructorExpr();
        RTypeCall tc = new RTypeCall();
        tc.setTypeName("Party");
        ctor.setTypeCall(tc);

        RKeyValuePair p1 = new RKeyValuePair();
        p1.setKey("name");
        StubExpression nameVal = new StubExpression();
        p1.setValue(nameVal);
        p1.setAsKey(false);

        RKeyValuePair p2 = new RKeyValuePair();
        p2.setKey("id");
        StubExpression idVal = new StubExpression();
        p2.setValue(idVal);
        p2.setAsKey(true);

        ctor.pairs().add(p1);
        ctor.pairs().add(p2);

        // Verify
        assertEquals("Party", ctor.typeCall().typeName());
        assertEquals(2, ctor.pairs().size());
        assertFalse(ctor.pairs().get(0).isAsKey());
        assertTrue(ctor.pairs().get(1).isAsKey());

        // children: typeCall + 2 pairs
        assertEquals(3, ctor.children().size());
    }

    @Test
    void integration_filterWithImplicitInlineFunction() {
        // myList filter item > 0
        RFilterExpr filter = new RFilterExpr();
        StubExpression listArg = new StubExpression();
        filter.setArgument(listArg);

        RInlineFunction fn = new RInlineFunction();
        fn.setImplicit(true);
        StubExpression body = new StubExpression();
        fn.setBody(body);
        filter.setBody(fn);

        assertSame(listArg, filter.argument());
        assertTrue(filter.body().isImplicit());
        assertEquals(2, filter.children().size());
    }

    @Test
    void integration_conditionalWithElse() {
        // if cond then x else y
        RConditionalExpr cond = new RConditionalExpr();
        StubExpression condExpr = new StubExpression();
        StubExpression thenExpr = new StubExpression();
        StubExpression elseExpr = new StubExpression();
        cond.setCondition(condExpr);
        cond.setThenBranch(thenExpr);
        cond.setElseBranch(elseExpr);

        assertEquals(3, cond.children().size());
        assertSame(condExpr, cond.children().get(0));
        assertSame(thenExpr, cond.children().get(1));
        assertSame(elseExpr, cond.children().get(2));
    }
}
