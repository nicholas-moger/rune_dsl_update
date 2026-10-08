package com.regnosys.rosetta.ast;

import com.regnosys.rosetta.ast.expressions.literals.RBooleanLiteral;
import com.regnosys.rosetta.ast.expressions.literals.RIntLiteral;
import com.regnosys.rosetta.ast.expressions.literals.RListLiteral;
import com.regnosys.rosetta.ast.expressions.literals.RNumberLiteral;
import com.regnosys.rosetta.ast.expressions.literals.RStringLiteral;
import com.regnosys.rosetta.ast.expressions.references.RDeepFeatureCall;
import com.regnosys.rosetta.ast.expressions.references.REmptyLiteral;
import com.regnosys.rosetta.ast.expressions.references.REnumValueRef;
import com.regnosys.rosetta.ast.expressions.references.RFeatureCall;
import com.regnosys.rosetta.ast.expressions.references.RImplicitVariable;
import com.regnosys.rosetta.ast.expressions.references.RSuperCall;
import com.regnosys.rosetta.ast.expressions.references.RSymbolReference;
import com.regnosys.rosetta.ast.visitor.RExpressionVisitor;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for literal and reference expression nodes (Task 9).
 *
 * <p>These tests construct nodes directly (no parsing) to validate getters,
 * setters, optional fields, list fields, boolean getters, and the
 * {@code children()} traversal.
 */
class ExpressionLiteralTest extends BaseAstTest {

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
    // RBooleanLiteral
    // =========================================================================

    @Test
    void rBooleanLiteral_defaultState() {
        RBooleanLiteral lit = new RBooleanLiteral();
        assertFalse(lit.value());
        assertTrue(lit.children().isEmpty());
        assertEquals(Optional.empty(), lit.left());
    }

    @Test
    void rBooleanLiteral_setTrue() {
        RBooleanLiteral lit = new RBooleanLiteral();
        lit.setValue(true);
        assertTrue(lit.value());
    }

    @Test
    void rBooleanLiteral_setFalse() {
        RBooleanLiteral lit = new RBooleanLiteral();
        lit.setValue(false);
        assertFalse(lit.value());
    }

    @Test
    void rBooleanLiteral_extendsRExpression() {
        RBooleanLiteral lit = new RBooleanLiteral();
        assertInstanceOf(RExpression.class, lit);
        assertInstanceOf(RNode.class, lit);
    }

    @Test
    void rBooleanLiteral_isLeafNode() {
        RBooleanLiteral lit = new RBooleanLiteral();
        lit.setValue(true);
        assertTrue(lit.children().isEmpty());
    }

    // =========================================================================
    // RStringLiteral
    // =========================================================================

    @Test
    void rStringLiteral_defaultState() {
        RStringLiteral lit = new RStringLiteral();
        assertNull(lit.value());
        assertTrue(lit.children().isEmpty());
        assertEquals(Optional.empty(), lit.left());
    }

    @Test
    void rStringLiteral_setValue() {
        RStringLiteral lit = new RStringLiteral();
        lit.setValue("hello world");
        assertEquals("hello world", lit.value());
    }

    @Test
    void rStringLiteral_emptyString() {
        RStringLiteral lit = new RStringLiteral();
        lit.setValue("");
        assertEquals("", lit.value());
    }

    @Test
    void rStringLiteral_extendsRExpression() {
        RStringLiteral lit = new RStringLiteral();
        assertInstanceOf(RExpression.class, lit);
        assertInstanceOf(RNode.class, lit);
    }

    @Test
    void rStringLiteral_isLeafNode() {
        RStringLiteral lit = new RStringLiteral();
        lit.setValue("test");
        assertTrue(lit.children().isEmpty());
    }

    // =========================================================================
    // RNumberLiteral
    // =========================================================================

    @Test
    void rNumberLiteral_defaultState() {
        RNumberLiteral lit = new RNumberLiteral();
        assertNull(lit.value());
        assertTrue(lit.children().isEmpty());
        assertEquals(Optional.empty(), lit.left());
    }

    @Test
    void rNumberLiteral_setDecimalValue() {
        RNumberLiteral lit = new RNumberLiteral();
        lit.setValue(new BigDecimal("3.14"));
        assertEquals(new BigDecimal("3.14"), lit.value());
    }

    @Test
    void rNumberLiteral_setWholeNumber() {
        RNumberLiteral lit = new RNumberLiteral();
        lit.setValue(BigDecimal.ZERO);
        assertEquals(BigDecimal.ZERO, lit.value());
    }

    @Test
    void rNumberLiteral_extendsRExpression() {
        RNumberLiteral lit = new RNumberLiteral();
        assertInstanceOf(RExpression.class, lit);
        assertInstanceOf(RNode.class, lit);
    }

    @Test
    void rNumberLiteral_isLeafNode() {
        RNumberLiteral lit = new RNumberLiteral();
        lit.setValue(BigDecimal.TEN);
        assertTrue(lit.children().isEmpty());
    }

    // =========================================================================
    // RIntLiteral
    // =========================================================================

    @Test
    void rIntLiteral_defaultState() {
        RIntLiteral lit = new RIntLiteral();
        assertNull(lit.value());
        assertTrue(lit.children().isEmpty());
        assertEquals(Optional.empty(), lit.left());
    }

    @Test
    void rIntLiteral_setPositive() {
        RIntLiteral lit = new RIntLiteral();
        lit.setValue(42);
        assertEquals(java.math.BigInteger.valueOf(42), lit.value());
    }

    @Test
    void rIntLiteral_setNegative() {
        RIntLiteral lit = new RIntLiteral();
        lit.setValue(-7);
        assertEquals(java.math.BigInteger.valueOf(-7), lit.value());
    }

    @Test
    void rIntLiteral_setBigInteger() {
        // Verifies arbitrary-precision support: real Rune DSL files contain
        // integer literals exceeding long range.
        RIntLiteral lit = new RIntLiteral();
        java.math.BigInteger huge = new java.math.BigInteger("9999999999999999999999999");
        lit.setValue(huge);
        assertEquals(huge, lit.value());
    }

    @Test
    void rIntLiteral_setLong() {
        RIntLiteral lit = new RIntLiteral();
        lit.setValue(Long.MAX_VALUE);
        assertEquals(java.math.BigInteger.valueOf(Long.MAX_VALUE), lit.value());
    }

    @Test
    void rIntLiteral_extendsRExpression() {
        RIntLiteral lit = new RIntLiteral();
        assertInstanceOf(RExpression.class, lit);
        assertInstanceOf(RNode.class, lit);
    }

    @Test
    void rIntLiteral_isLeafNode() {
        RIntLiteral lit = new RIntLiteral();
        lit.setValue(99);
        assertTrue(lit.children().isEmpty());
    }

    // =========================================================================
    // RListLiteral
    // =========================================================================

    @Test
    void rListLiteral_defaultState() {
        RListLiteral lit = new RListLiteral();
        assertTrue(lit.elements().isEmpty());
        assertTrue(lit.children().isEmpty());
        assertEquals(Optional.empty(), lit.left());
    }

    @Test
    void rListLiteral_addElements() {
        RListLiteral lit = new RListLiteral();
        StubExpression e1 = new StubExpression();
        StubExpression e2 = new StubExpression();
        lit.elements().add(e1);
        lit.elements().add(e2);

        assertEquals(2, lit.elements().size());
        assertSame(e1, lit.elements().get(0));
        assertSame(e2, lit.elements().get(1));
    }

    @Test
    void rListLiteral_childrenMatchElements() {
        RListLiteral lit = new RListLiteral();
        StubExpression e1 = new StubExpression();
        StubExpression e2 = new StubExpression();
        StubExpression e3 = new StubExpression();
        lit.elements().add(e1);
        lit.elements().add(e2);
        lit.elements().add(e3);

        List<? extends RNode> children = lit.children();
        assertEquals(3, children.size());
        assertSame(e1, children.get(0));
        assertSame(e2, children.get(1));
        assertSame(e3, children.get(2));
    }

    @Test
    void rListLiteral_extendsRExpression() {
        RListLiteral lit = new RListLiteral();
        assertInstanceOf(RExpression.class, lit);
        assertInstanceOf(RNode.class, lit);
    }

    // =========================================================================
    // RSymbolReference
    // =========================================================================

    @Test
    void rSymbolReference_defaultState() {
        RSymbolReference ref = new RSymbolReference();
        assertNull(ref.name());
        assertTrue(ref.args().isEmpty());
        assertTrue(ref.children().isEmpty());
        assertEquals(Optional.empty(), ref.left());
    }

    @Test
    void rSymbolReference_setName() {
        RSymbolReference ref = new RSymbolReference();
        ref.setName("myParam");
        assertEquals("myParam", ref.name());
    }

    @Test
    void rSymbolReference_addArgs() {
        RSymbolReference ref = new RSymbolReference();
        ref.setName("MyFunction");
        StubExpression arg1 = new StubExpression();
        StubExpression arg2 = new StubExpression();
        ref.args().add(arg1);
        ref.args().add(arg2);

        assertEquals(2, ref.args().size());
        assertSame(arg1, ref.args().get(0));
        assertSame(arg2, ref.args().get(1));
    }

    @Test
    void rSymbolReference_childrenMatchArgs() {
        RSymbolReference ref = new RSymbolReference();
        ref.setName("Func");
        StubExpression arg1 = new StubExpression();
        ref.args().add(arg1);

        List<? extends RNode> children = ref.children();
        assertEquals(1, children.size());
        assertSame(arg1, children.get(0));
    }

    @Test
    void rSymbolReference_noArgsNoChildren() {
        RSymbolReference ref = new RSymbolReference();
        ref.setName("simpleRef");
        assertTrue(ref.children().isEmpty());
    }

    @Test
    void rSymbolReference_extendsRExpression() {
        RSymbolReference ref = new RSymbolReference();
        assertInstanceOf(RExpression.class, ref);
        assertInstanceOf(RNode.class, ref);
    }

    // =========================================================================
    // RFeatureCall
    // =========================================================================

    @Test
    void rFeatureCall_defaultState() {
        RFeatureCall fc = new RFeatureCall();
        assertNull(fc.receiver());
        assertNull(fc.featureName());
        assertTrue(fc.children().isEmpty());
        assertEquals(Optional.empty(), fc.left());
    }

    @Test
    void rFeatureCall_setReceiverAndFeatureName() {
        RFeatureCall fc = new RFeatureCall();
        StubExpression receiver = new StubExpression();
        fc.setReceiver(receiver);
        fc.setFeatureName("price");

        assertSame(receiver, fc.receiver());
        assertEquals("price", fc.featureName());
    }

    @Test
    void rFeatureCall_leftReturnsReceiver() {
        RFeatureCall fc = new RFeatureCall();
        StubExpression receiver = new StubExpression();
        fc.setReceiver(receiver);

        assertEquals(Optional.of(receiver), fc.left());
    }

    @Test
    void rFeatureCall_childrenIncludesReceiver() {
        RFeatureCall fc = new RFeatureCall();
        StubExpression receiver = new StubExpression();
        fc.setReceiver(receiver);
        fc.setFeatureName("name");

        List<? extends RNode> children = fc.children();
        assertEquals(1, children.size());
        assertSame(receiver, children.get(0));
    }

    @Test
    void rFeatureCall_noReceiverEmptyChildren() {
        RFeatureCall fc = new RFeatureCall();
        fc.setFeatureName("name");
        assertTrue(fc.children().isEmpty());
    }

    @Test
    void rFeatureCall_extendsRExpression() {
        RFeatureCall fc = new RFeatureCall();
        assertInstanceOf(RExpression.class, fc);
        assertInstanceOf(RNode.class, fc);
    }

    // =========================================================================
    // RDeepFeatureCall
    // =========================================================================

    @Test
    void rDeepFeatureCall_defaultState() {
        RDeepFeatureCall dfc = new RDeepFeatureCall();
        assertNull(dfc.receiver());
        assertNull(dfc.featureName());
        assertTrue(dfc.children().isEmpty());
        assertEquals(Optional.empty(), dfc.left());
    }

    @Test
    void rDeepFeatureCall_setReceiverAndFeatureName() {
        RDeepFeatureCall dfc = new RDeepFeatureCall();
        StubExpression receiver = new StubExpression();
        dfc.setReceiver(receiver);
        dfc.setFeatureName("identifier");

        assertSame(receiver, dfc.receiver());
        assertEquals("identifier", dfc.featureName());
    }

    @Test
    void rDeepFeatureCall_leftReturnsReceiver() {
        RDeepFeatureCall dfc = new RDeepFeatureCall();
        StubExpression receiver = new StubExpression();
        dfc.setReceiver(receiver);

        assertEquals(Optional.of(receiver), dfc.left());
    }

    @Test
    void rDeepFeatureCall_childrenIncludesReceiver() {
        RDeepFeatureCall dfc = new RDeepFeatureCall();
        StubExpression receiver = new StubExpression();
        dfc.setReceiver(receiver);
        dfc.setFeatureName("id");

        List<? extends RNode> children = dfc.children();
        assertEquals(1, children.size());
        assertSame(receiver, children.get(0));
    }

    @Test
    void rDeepFeatureCall_noReceiverEmptyChildren() {
        RDeepFeatureCall dfc = new RDeepFeatureCall();
        dfc.setFeatureName("id");
        assertTrue(dfc.children().isEmpty());
    }

    @Test
    void rDeepFeatureCall_extendsRExpression() {
        RDeepFeatureCall dfc = new RDeepFeatureCall();
        assertInstanceOf(RExpression.class, dfc);
        assertInstanceOf(RNode.class, dfc);
    }

    // =========================================================================
    // REnumValueRef
    // =========================================================================

    @Test
    void rEnumValueRef_defaultState() {
        REnumValueRef ref = new REnumValueRef();
        assertNull(ref.enumName());
        assertNull(ref.valueName());
        assertTrue(ref.children().isEmpty());
        assertEquals(Optional.empty(), ref.left());
    }

    @Test
    void rEnumValueRef_setEnumAndValue() {
        REnumValueRef ref = new REnumValueRef();
        ref.setEnumName("TradeTypeEnum");
        ref.setValueName("NewTrade");

        assertEquals("TradeTypeEnum", ref.enumName());
        assertEquals("NewTrade", ref.valueName());
    }

    @Test
    void rEnumValueRef_extendsRExpression() {
        REnumValueRef ref = new REnumValueRef();
        assertInstanceOf(RExpression.class, ref);
        assertInstanceOf(RNode.class, ref);
    }

    @Test
    void rEnumValueRef_isLeafNode() {
        REnumValueRef ref = new REnumValueRef();
        ref.setEnumName("E");
        ref.setValueName("V");
        assertTrue(ref.children().isEmpty());
    }

    // =========================================================================
    // RSuperCall
    // =========================================================================

    @Test
    void rSuperCall_defaultState() {
        RSuperCall sc = new RSuperCall();
        assertTrue(sc.children().isEmpty());
        assertEquals(Optional.empty(), sc.left());
    }

    @Test
    void rSuperCall_extendsRExpression() {
        RSuperCall sc = new RSuperCall();
        assertInstanceOf(RExpression.class, sc);
        assertInstanceOf(RNode.class, sc);
    }

    @Test
    void rSuperCall_isLeafNode() {
        RSuperCall sc = new RSuperCall();
        assertTrue(sc.children().isEmpty());
    }

    // =========================================================================
    // RImplicitVariable
    // =========================================================================

    @Test
    void rImplicitVariable_defaultState() {
        RImplicitVariable iv = new RImplicitVariable();
        assertTrue(iv.children().isEmpty());
        assertEquals(Optional.empty(), iv.left());
    }

    @Test
    void rImplicitVariable_extendsRExpression() {
        RImplicitVariable iv = new RImplicitVariable();
        assertInstanceOf(RExpression.class, iv);
        assertInstanceOf(RNode.class, iv);
    }

    @Test
    void rImplicitVariable_isLeafNode() {
        RImplicitVariable iv = new RImplicitVariable();
        assertTrue(iv.children().isEmpty());
    }

    // =========================================================================
    // REmptyLiteral
    // =========================================================================

    @Test
    void rEmptyLiteral_defaultState() {
        REmptyLiteral el = new REmptyLiteral();
        assertTrue(el.children().isEmpty());
        assertEquals(Optional.empty(), el.left());
    }

    @Test
    void rEmptyLiteral_extendsRExpression() {
        REmptyLiteral el = new REmptyLiteral();
        assertInstanceOf(RExpression.class, el);
        assertInstanceOf(RNode.class, el);
    }

    @Test
    void rEmptyLiteral_isLeafNode() {
        REmptyLiteral el = new REmptyLiteral();
        assertTrue(el.children().isEmpty());
    }

    // =========================================================================
    // Integration: feature call chain
    // =========================================================================

    @Test
    void integration_featureCallChain() {
        // trade -> price -> amount
        RSymbolReference trade = new RSymbolReference();
        trade.setName("trade");

        RFeatureCall priceCall = new RFeatureCall();
        priceCall.setReceiver(trade);
        priceCall.setFeatureName("price");

        RFeatureCall amountCall = new RFeatureCall();
        amountCall.setReceiver(priceCall);
        amountCall.setFeatureName("amount");

        // Verify chain
        assertSame(priceCall, amountCall.receiver());
        assertSame(trade, priceCall.receiver());
        assertEquals("amount", amountCall.featureName());
        assertEquals("price", priceCall.featureName());
        assertEquals("trade", trade.name());

        // Children traversal
        assertEquals(1, amountCall.children().size());
        assertSame(priceCall, amountCall.children().get(0));
        assertEquals(1, priceCall.children().size());
        assertSame(trade, priceCall.children().get(0));
    }

    @Test
    void integration_symbolReferenceWithLiteralArgs() {
        // MyFunc(42, "hello")
        RSymbolReference func = new RSymbolReference();
        func.setName("MyFunc");

        RIntLiteral intArg = new RIntLiteral();
        intArg.setValue(42);
        func.args().add(intArg);

        RStringLiteral strArg = new RStringLiteral();
        strArg.setValue("hello");
        func.args().add(strArg);

        assertEquals(2, func.args().size());
        List<? extends RNode> children = func.children();
        assertEquals(2, children.size());
        assertSame(intArg, children.get(0));
        assertSame(strArg, children.get(1));
    }

    @Test
    void integration_listLiteralWithMixedElements() {
        // [1, "two", True]
        RListLiteral list = new RListLiteral();

        RIntLiteral intLit = new RIntLiteral();
        intLit.setValue(1);
        list.elements().add(intLit);

        RStringLiteral strLit = new RStringLiteral();
        strLit.setValue("two");
        list.elements().add(strLit);

        RBooleanLiteral boolLit = new RBooleanLiteral();
        boolLit.setValue(true);
        list.elements().add(boolLit);

        assertEquals(3, list.elements().size());
        List<? extends RNode> children = list.children();
        assertEquals(3, children.size());
    }
}
