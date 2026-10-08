package com.regnosys.rosetta.ast;

import com.regnosys.rosetta.ast.enums.OperationOp;
import com.regnosys.rosetta.ast.enums.RuleKind;
import com.regnosys.rosetta.ast.functions.RCondition;
import com.regnosys.rosetta.ast.functions.RDispatch;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.functions.ROperation;
import com.regnosys.rosetta.ast.functions.RPostCondition;
import com.regnosys.rosetta.ast.functions.RRule;
import com.regnosys.rosetta.ast.functions.RSegment;
import com.regnosys.rosetta.ast.functions.RShortcut;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.supporting.RTypeCall;
import com.regnosys.rosetta.ast.visitor.RExpressionVisitor;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for function and rule declaration nodes (Task 4).
 *
 * <p>These tests construct nodes directly (no parsing) to validate getters,
 * setters, optional fields, list fields, and the {@code children()} traversal.
 */
class FunctionNodeTest extends BaseAstTest {

    // =========================================================================
    // Helper: concrete RExpression for testing
    // =========================================================================

    /**
     * Minimal concrete {@link RExpression} subclass for test use.
     * RExpression is abstract, so tests that need an expression instance
     * use this stub.
     */
    private static class StubExpression extends RExpression {
        @Override
        public <R, C> R accept(RExpressionVisitor<R, C> visitor, C context) {
            throw new UnsupportedOperationException("Test stub");
        }
    }

    // =========================================================================
    // RFunction
    // =========================================================================

    @Test
    void rFunction_defaultState() {
        RFunction fn = new RFunction();
        assertNull(fn.name());
        assertEquals(Optional.empty(), fn.dispatch());
        assertEquals(Optional.empty(), fn.superFunctionName());
        assertEquals(Optional.empty(), fn.definition());
        assertTrue(fn.docReferences().isEmpty());
        assertTrue(fn.annotationRefs().isEmpty());
        assertTrue(fn.inputs().isEmpty());
        assertEquals(Optional.empty(), fn.output());
        assertTrue(fn.shortcuts().isEmpty());
        assertTrue(fn.conditions().isEmpty());
        assertTrue(fn.operations().isEmpty());
        assertTrue(fn.postConditions().isEmpty());
        assertTrue(fn.children().isEmpty());
    }

    @Test
    void rFunction_setNameAndDefinition() {
        RFunction fn = new RFunction();
        fn.setName("Create_TradeState");
        fn.setDefinition("Creates a trade state from inputs.");

        assertEquals("Create_TradeState", fn.name());
        assertEquals(Optional.of("Creates a trade state from inputs."), fn.definition());
    }

    @Test
    void rFunction_setSuperFunctionName() {
        RFunction fn = new RFunction();
        fn.setName("Create_TradeState");
        fn.setSuperFunctionName("com.example.BaseFunction");

        assertEquals(Optional.of("com.example.BaseFunction"), fn.superFunctionName());
    }

    @Test
    void rFunction_noSuperFunctionName() {
        RFunction fn = new RFunction();
        fn.setName("StandaloneFunc");
        assertEquals(Optional.empty(), fn.superFunctionName());
    }

    @Test
    void rFunction_setDispatch() {
        RFunction fn = new RFunction();
        fn.setName("Qualify_Trade");

        RDispatch dispatch = new RDispatch();
        dispatch.setParamName("tradeType");
        dispatch.setEnumRef("com.example.TradeTypeEnum");
        dispatch.setValueName("InterestRate");
        fn.setDispatch(dispatch);

        assertTrue(fn.dispatch().isPresent());
        assertEquals("tradeType", fn.dispatch().get().paramName());
        assertEquals("com.example.TradeTypeEnum", fn.dispatch().get().enumRef());
        assertEquals("InterestRate", fn.dispatch().get().valueName());
    }

    @Test
    void rFunction_implementsRDefinable() {
        RFunction fn = new RFunction();
        assertInstanceOf(RDefinable.class, fn);
    }

    @Test
    void rFunction_extendsRRootElement() {
        RFunction fn = new RFunction();
        assertInstanceOf(RRootElement.class, fn);
        assertInstanceOf(RNode.class, fn);
    }

    @Test
    void rFunction_addInputs() {
        RFunction fn = new RFunction();
        fn.setName("Create_TradeState");

        RAttribute input1 = new RAttribute();
        input1.setName("trade");
        RTypeCall tc1 = new RTypeCall();
        tc1.setTypeName("Trade");
        input1.setTypeCall(tc1);

        RAttribute input2 = new RAttribute();
        input2.setName("event");
        RTypeCall tc2 = new RTypeCall();
        tc2.setTypeName("Event");
        input2.setTypeCall(tc2);

        fn.inputs().add(input1);
        fn.inputs().add(input2);

        assertEquals(2, fn.inputs().size());
        assertEquals("trade", fn.inputs().get(0).name());
        assertEquals("event", fn.inputs().get(1).name());
    }

    @Test
    void rFunction_setOutput() {
        RFunction fn = new RFunction();
        fn.setName("Create_TradeState");

        RAttribute output = new RAttribute();
        output.setName("tradeState");
        RTypeCall tc = new RTypeCall();
        tc.setTypeName("TradeState");
        output.setTypeCall(tc);
        fn.setOutput(output);

        assertTrue(fn.output().isPresent());
        assertEquals("tradeState", fn.output().get().name());
        assertEquals("TradeState", fn.output().get().typeCall().typeName());
    }

    @Test
    void rFunction_addShortcuts() {
        RFunction fn = new RFunction();
        fn.setName("MyFunc");

        RShortcut sc = new RShortcut();
        sc.setName("myAlias");
        sc.setExpression(new StubExpression());
        fn.shortcuts().add(sc);

        assertEquals(1, fn.shortcuts().size());
        assertEquals("myAlias", fn.shortcuts().get(0).name());
    }

    @Test
    void rFunction_addConditions() {
        RFunction fn = new RFunction();
        fn.setName("MyFunc");

        RCondition cond = new RCondition();
        cond.setName("Positive");
        cond.setExpression(new StubExpression());
        fn.conditions().add(cond);

        assertEquals(1, fn.conditions().size());
        assertEquals(Optional.of("Positive"), fn.conditions().get(0).name());
    }

    @Test
    void rFunction_addOperations() {
        RFunction fn = new RFunction();
        fn.setName("MyFunc");

        ROperation op = new ROperation();
        op.setOperator(OperationOp.SET);
        op.setTargetName("result");
        op.setExpression(new StubExpression());
        fn.operations().add(op);

        assertEquals(1, fn.operations().size());
        assertEquals(OperationOp.SET, fn.operations().get(0).operator());
    }

    @Test
    void rFunction_addPostConditions() {
        RFunction fn = new RFunction();
        fn.setName("MyFunc");

        RPostCondition pc = new RPostCondition();
        pc.setName("ResultNotNull");
        pc.setExpression(new StubExpression());
        fn.postConditions().add(pc);

        assertEquals(1, fn.postConditions().size());
        assertEquals(Optional.of("ResultNotNull"), fn.postConditions().get(0).name());
    }

    @Test
    void rFunction_childrenIncludesAllStructural() {
        RFunction fn = new RFunction();

        RDispatch dispatch = new RDispatch();
        dispatch.setParamName("p");
        dispatch.setEnumRef("E");
        dispatch.setValueName("V");
        fn.setDispatch(dispatch);

        RAttribute input = new RAttribute();
        input.setName("in1");
        fn.inputs().add(input);

        RAttribute output = new RAttribute();
        output.setName("out1");
        fn.setOutput(output);

        RShortcut sc = new RShortcut();
        sc.setName("alias1");
        sc.setExpression(new StubExpression());
        fn.shortcuts().add(sc);

        RCondition cond = new RCondition();
        cond.setExpression(new StubExpression());
        fn.conditions().add(cond);

        ROperation op = new ROperation();
        op.setOperator(OperationOp.ADD);
        op.setTargetName("target");
        op.setExpression(new StubExpression());
        fn.operations().add(op);

        RPostCondition pc = new RPostCondition();
        pc.setExpression(new StubExpression());
        fn.postConditions().add(pc);

        List<? extends RNode> children = fn.children();
        // dispatch + input + output + shortcut + condition + operation + postCondition = 7
        assertEquals(7, children.size());
        assertSame(dispatch, children.get(0));
        assertSame(input, children.get(1));
        assertSame(output, children.get(2));
        assertSame(sc, children.get(3));
        assertSame(cond, children.get(4));
        assertSame(op, children.get(5));
        assertSame(pc, children.get(6));
    }

    @Test
    void rFunction_childrenEmptyWithNoContent() {
        RFunction fn = new RFunction();
        fn.setName("EmptyFunc");
        assertTrue(fn.children().isEmpty());
    }

    // =========================================================================
    // RDispatch
    // =========================================================================

    @Test
    void rDispatch_defaultState() {
        RDispatch d = new RDispatch();
        assertNull(d.paramName());
        assertNull(d.enumRef());
        assertNull(d.valueName());
    }

    @Test
    void rDispatch_setAllFields() {
        RDispatch d = new RDispatch();
        d.setParamName("eventType");
        d.setEnumRef("com.example.EventTypeEnum");
        d.setValueName("CreditDefault");

        assertEquals("eventType", d.paramName());
        assertEquals("com.example.EventTypeEnum", d.enumRef());
        assertEquals("CreditDefault", d.valueName());
    }

    @Test
    void rDispatch_extendsRNode() {
        RDispatch d = new RDispatch();
        assertInstanceOf(RNode.class, d);
    }

    @Test
    void rDispatch_doesNotImplementRDefinable() {
        RDispatch d = new RDispatch();
        assertFalse(d instanceof RDefinable);
    }

    @Test
    void rDispatch_isLeaf() {
        RDispatch d = new RDispatch();
        d.setParamName("p");
        d.setEnumRef("E");
        d.setValueName("V");
        assertTrue(d.children().isEmpty());
    }

    // =========================================================================
    // RRule
    // =========================================================================

    @Test
    void rRule_defaultState() {
        RRule r = new RRule();
        assertNull(r.kind());
        assertNull(r.name());
        assertEquals(Optional.empty(), r.fromType());
        assertEquals(Optional.empty(), r.definition());
        assertTrue(r.docReferences().isEmpty());
        assertEquals(Optional.empty(), r.expression());
        assertEquals(Optional.empty(), r.alias());
        assertTrue(r.children().isEmpty());
    }

    @Test
    void rRule_setKindAndName() {
        RRule r = new RRule();
        r.setKind(RuleKind.REPORTING);
        r.setName("FpmlIrd1");

        assertEquals(RuleKind.REPORTING, r.kind());
        assertEquals("FpmlIrd1", r.name());
    }

    @Test
    void rRule_eligibilityKind() {
        RRule r = new RRule();
        r.setKind(RuleKind.ELIGIBILITY);
        assertEquals(RuleKind.ELIGIBILITY, r.kind());
    }

    @Test
    void rRule_setFromType() {
        RRule r = new RRule();
        r.setKind(RuleKind.REPORTING);
        r.setName("MyRule");

        RTypeCall tc = new RTypeCall();
        tc.setTypeName("Trade");
        r.setFromType(tc);

        assertTrue(r.fromType().isPresent());
        assertEquals("Trade", r.fromType().get().typeName());
    }

    @Test
    void rRule_setDefinition() {
        RRule r = new RRule();
        r.setDefinition("Extracts the notional amount.");

        assertEquals(Optional.of("Extracts the notional amount."), r.definition());
    }

    @Test
    void rRule_setExpression() {
        RRule r = new RRule();
        StubExpression expr = new StubExpression();
        r.setExpression(expr);

        assertTrue(r.expression().isPresent());
        assertSame(expr, r.expression().get());
    }

    @Test
    void rRule_setAlias() {
        RRule r = new RRule();
        r.setAlias("Notional Amount");

        assertEquals(Optional.of("Notional Amount"), r.alias());
    }

    @Test
    void rRule_noAlias() {
        RRule r = new RRule();
        assertEquals(Optional.empty(), r.alias());
    }

    @Test
    void rRule_implementsRDefinable() {
        RRule r = new RRule();
        assertInstanceOf(RDefinable.class, r);
    }

    @Test
    void rRule_extendsRRootElement() {
        RRule r = new RRule();
        assertInstanceOf(RRootElement.class, r);
        assertInstanceOf(RNode.class, r);
    }

    @Test
    void rRule_childrenIncludesFromTypeAndExpression() {
        RRule r = new RRule();

        RTypeCall tc = new RTypeCall();
        tc.setTypeName("Trade");
        r.setFromType(tc);

        StubExpression expr = new StubExpression();
        r.setExpression(expr);

        List<? extends RNode> children = r.children();
        assertEquals(2, children.size());
        assertSame(tc, children.get(0));
        assertSame(expr, children.get(1));
    }

    @Test
    void rRule_childrenWithOnlyExpression() {
        RRule r = new RRule();

        StubExpression expr = new StubExpression();
        r.setExpression(expr);

        List<? extends RNode> children = r.children();
        assertEquals(1, children.size());
        assertSame(expr, children.get(0));
    }

    @Test
    void rRule_childrenEmpty() {
        RRule r = new RRule();
        assertTrue(r.children().isEmpty());
    }

    // =========================================================================
    // RCondition
    // =========================================================================

    @Test
    void rCondition_defaultState() {
        RCondition c = new RCondition();
        assertEquals(Optional.empty(), c.name());
        assertEquals(Optional.empty(), c.definition());
        assertNull(c.expression());
        assertTrue(c.docReferences().isEmpty());
        assertTrue(c.annotationRefs().isEmpty());
        assertTrue(c.children().isEmpty());
    }

    @Test
    void rCondition_setNameAndDefinition() {
        RCondition c = new RCondition();
        c.setName("PositiveAmount");
        c.setDefinition("Amount must be positive.");

        assertEquals(Optional.of("PositiveAmount"), c.name());
        assertEquals(Optional.of("Amount must be positive."), c.definition());
    }

    @Test
    void rCondition_optionalName() {
        // Conditions can be anonymous
        RCondition c = new RCondition();
        c.setExpression(new StubExpression());

        assertEquals(Optional.empty(), c.name());
        assertNotNull(c.expression());
    }

    @Test
    void rCondition_setExpression() {
        RCondition c = new RCondition();
        StubExpression expr = new StubExpression();
        c.setExpression(expr);

        assertSame(expr, c.expression());
    }

    @Test
    void rCondition_implementsRDefinable() {
        RCondition c = new RCondition();
        assertInstanceOf(RDefinable.class, c);
    }

    @Test
    void rCondition_extendsRNode() {
        RCondition c = new RCondition();
        assertInstanceOf(RNode.class, c);
        // RCondition is NOT a root element
        assertFalse(RRootElement.class.isAssignableFrom(RCondition.class));
    }

    @Test
    void rCondition_childrenIncludesExpression() {
        RCondition c = new RCondition();
        StubExpression expr = new StubExpression();
        c.setExpression(expr);

        List<? extends RNode> children = c.children();
        assertEquals(1, children.size());
        assertSame(expr, children.get(0));
    }

    @Test
    void rCondition_childrenEmptyWhenNoExpression() {
        RCondition c = new RCondition();
        assertTrue(c.children().isEmpty());
    }

    // =========================================================================
    // RPostCondition
    // =========================================================================

    @Test
    void rPostCondition_defaultState() {
        RPostCondition pc = new RPostCondition();
        assertEquals(Optional.empty(), pc.name());
        assertEquals(Optional.empty(), pc.definition());
        assertNull(pc.expression());
        assertTrue(pc.children().isEmpty());
    }

    @Test
    void rPostCondition_setNameAndDefinition() {
        RPostCondition pc = new RPostCondition();
        pc.setName("OutputValid");
        pc.setDefinition("Output must be valid.");

        assertEquals(Optional.of("OutputValid"), pc.name());
        assertEquals(Optional.of("Output must be valid."), pc.definition());
    }

    @Test
    void rPostCondition_optionalName() {
        RPostCondition pc = new RPostCondition();
        pc.setExpression(new StubExpression());

        assertEquals(Optional.empty(), pc.name());
        assertNotNull(pc.expression());
    }

    @Test
    void rPostCondition_setExpression() {
        RPostCondition pc = new RPostCondition();
        StubExpression expr = new StubExpression();
        pc.setExpression(expr);

        assertSame(expr, pc.expression());
    }

    @Test
    void rPostCondition_implementsRDefinable() {
        RPostCondition pc = new RPostCondition();
        assertInstanceOf(RDefinable.class, pc);
    }

    @Test
    void rPostCondition_extendsRNode() {
        RPostCondition pc = new RPostCondition();
        assertInstanceOf(RNode.class, pc);
        assertFalse(RRootElement.class.isAssignableFrom(RPostCondition.class));
    }

    @Test
    void rPostCondition_childrenIncludesExpression() {
        RPostCondition pc = new RPostCondition();
        StubExpression expr = new StubExpression();
        pc.setExpression(expr);

        List<? extends RNode> children = pc.children();
        assertEquals(1, children.size());
        assertSame(expr, children.get(0));
    }

    @Test
    void rPostCondition_childrenEmptyWhenNoExpression() {
        RPostCondition pc = new RPostCondition();
        assertTrue(pc.children().isEmpty());
    }

    // =========================================================================
    // ROperation
    // =========================================================================

    @Test
    void rOperation_defaultState() {
        ROperation op = new ROperation();
        assertNull(op.operator());
        assertNull(op.targetName());
        assertEquals(Optional.empty(), op.segment());
        assertEquals(Optional.empty(), op.definition());
        assertNull(op.expression());
        assertFalse(op.isAsKey());
        assertTrue(op.children().isEmpty());
    }

    @Test
    void rOperation_setOperatorAndTarget() {
        ROperation op = new ROperation();
        op.setOperator(OperationOp.SET);
        op.setTargetName("result");

        assertEquals(OperationOp.SET, op.operator());
        assertEquals("result", op.targetName());
    }

    @Test
    void rOperation_addOperator() {
        ROperation op = new ROperation();
        op.setOperator(OperationOp.ADD);
        assertEquals(OperationOp.ADD, op.operator());
    }

    @Test
    void rOperation_setSegment() {
        ROperation op = new ROperation();
        op.setOperator(OperationOp.SET);
        op.setTargetName("trade");

        RSegment seg = new RSegment();
        seg.setName("price");
        op.setSegment(seg);

        assertTrue(op.segment().isPresent());
        assertEquals("price", op.segment().get().name());
    }

    @Test
    void rOperation_setDefinition() {
        ROperation op = new ROperation();
        op.setDefinition("Sets the result value.");

        assertEquals(Optional.of("Sets the result value."), op.definition());
    }

    @Test
    void rOperation_setExpression() {
        ROperation op = new ROperation();
        StubExpression expr = new StubExpression();
        op.setExpression(expr);

        assertSame(expr, op.expression());
    }

    @Test
    void rOperation_setAsKey() {
        ROperation op = new ROperation();
        assertFalse(op.isAsKey());

        op.setAsKey(true);
        assertTrue(op.isAsKey());
    }

    @Test
    void rOperation_implementsRDefinable() {
        ROperation op = new ROperation();
        assertInstanceOf(RDefinable.class, op);
    }

    @Test
    void rOperation_extendsRNode() {
        ROperation op = new ROperation();
        assertInstanceOf(RNode.class, op);
        assertFalse(RRootElement.class.isAssignableFrom(ROperation.class));
    }

    @Test
    void rOperation_childrenIncludesSegmentAndExpression() {
        ROperation op = new ROperation();

        RSegment seg = new RSegment();
        seg.setName("path");
        op.setSegment(seg);

        StubExpression expr = new StubExpression();
        op.setExpression(expr);

        List<? extends RNode> children = op.children();
        assertEquals(2, children.size());
        assertSame(seg, children.get(0));
        assertSame(expr, children.get(1));
    }

    @Test
    void rOperation_childrenWithExpressionOnly() {
        ROperation op = new ROperation();
        StubExpression expr = new StubExpression();
        op.setExpression(expr);

        List<? extends RNode> children = op.children();
        assertEquals(1, children.size());
        assertSame(expr, children.get(0));
    }

    @Test
    void rOperation_childrenEmpty() {
        ROperation op = new ROperation();
        assertTrue(op.children().isEmpty());
    }

    // =========================================================================
    // RShortcut
    // =========================================================================

    @Test
    void rShortcut_defaultState() {
        RShortcut sc = new RShortcut();
        assertNull(sc.name());
        assertEquals(Optional.empty(), sc.definition());
        assertNull(sc.expression());
        assertTrue(sc.children().isEmpty());
    }

    @Test
    void rShortcut_setNameAndDefinition() {
        RShortcut sc = new RShortcut();
        sc.setName("tradeDate");
        sc.setDefinition("The trade date shortcut.");

        assertEquals("tradeDate", sc.name());
        assertEquals(Optional.of("The trade date shortcut."), sc.definition());
    }

    @Test
    void rShortcut_setExpression() {
        RShortcut sc = new RShortcut();
        StubExpression expr = new StubExpression();
        sc.setExpression(expr);

        assertSame(expr, sc.expression());
    }

    @Test
    void rShortcut_implementsRDefinable() {
        RShortcut sc = new RShortcut();
        assertInstanceOf(RDefinable.class, sc);
    }

    @Test
    void rShortcut_extendsRNode() {
        RShortcut sc = new RShortcut();
        assertInstanceOf(RNode.class, sc);
        assertFalse(RRootElement.class.isAssignableFrom(RShortcut.class));
    }

    @Test
    void rShortcut_childrenIncludesExpression() {
        RShortcut sc = new RShortcut();
        StubExpression expr = new StubExpression();
        sc.setExpression(expr);

        List<? extends RNode> children = sc.children();
        assertEquals(1, children.size());
        assertSame(expr, children.get(0));
    }

    @Test
    void rShortcut_childrenEmptyWhenNoExpression() {
        RShortcut sc = new RShortcut();
        assertTrue(sc.children().isEmpty());
    }

    // =========================================================================
    // RSegment
    // =========================================================================

    @Test
    void rSegment_defaultState() {
        RSegment seg = new RSegment();
        assertNull(seg.name());
        assertEquals(Optional.empty(), seg.next());
        assertTrue(seg.children().isEmpty());
    }

    @Test
    void rSegment_setName() {
        RSegment seg = new RSegment();
        seg.setName("price");

        assertEquals("price", seg.name());
    }

    @Test
    void rSegment_setNext() {
        RSegment seg1 = new RSegment();
        seg1.setName("trade");

        RSegment seg2 = new RSegment();
        seg2.setName("price");
        seg1.setNext(seg2);

        assertTrue(seg1.next().isPresent());
        assertEquals("price", seg1.next().get().name());
    }

    @Test
    void rSegment_recursiveChain() {
        // Build: trade -> price -> amount
        RSegment seg1 = new RSegment();
        seg1.setName("trade");

        RSegment seg2 = new RSegment();
        seg2.setName("price");

        RSegment seg3 = new RSegment();
        seg3.setName("amount");

        seg1.setNext(seg2);
        seg2.setNext(seg3);

        assertEquals("trade", seg1.name());
        assertTrue(seg1.next().isPresent());
        assertEquals("price", seg1.next().get().name());
        assertTrue(seg1.next().get().next().isPresent());
        assertEquals("amount", seg1.next().get().next().get().name());
        assertEquals(Optional.empty(), seg3.next());
    }

    @Test
    void rSegment_extendsRNode() {
        RSegment seg = new RSegment();
        assertInstanceOf(RNode.class, seg);
    }

    @Test
    void rSegment_doesNotImplementRDefinable() {
        RSegment seg = new RSegment();
        assertFalse(seg instanceof RDefinable);
    }

    @Test
    void rSegment_childrenIncludesNext() {
        RSegment seg1 = new RSegment();
        seg1.setName("a");

        RSegment seg2 = new RSegment();
        seg2.setName("b");
        seg1.setNext(seg2);

        List<? extends RNode> children = seg1.children();
        assertEquals(1, children.size());
        assertSame(seg2, children.get(0));
    }

    @Test
    void rSegment_childrenEmptyWhenNoNext() {
        RSegment seg = new RSegment();
        seg.setName("leaf");
        assertTrue(seg.children().isEmpty());
    }

    // =========================================================================
    // Integration: Full function tree
    // =========================================================================

    @Test
    void fullFunction_deepChildrenTraversal() {
        // Build: func Create_TradeState(tradeType TradeTypeEnum -> InterestRate)
        //          extends BaseFunction: <"Creates a trade state">
        //     inputs:
        //         trade Trade (1..1)
        //     output:
        //         tradeState TradeState (1..1)
        //     alias tradeDate: <expression>
        //     condition PositiveAmount: <expression>
        //     set result: <expression>
        //     post-condition ResultValid: <expression>
        RFunction fn = new RFunction();
        fn.setName("Create_TradeState");
        fn.setSuperFunctionName("BaseFunction");
        fn.setDefinition("Creates a trade state.");

        RDispatch dispatch = new RDispatch();
        dispatch.setParamName("tradeType");
        dispatch.setEnumRef("TradeTypeEnum");
        dispatch.setValueName("InterestRate");
        fn.setDispatch(dispatch);

        RAttribute input = new RAttribute();
        input.setName("trade");
        RTypeCall inputTc = new RTypeCall();
        inputTc.setTypeName("Trade");
        input.setTypeCall(inputTc);
        fn.inputs().add(input);

        RAttribute output = new RAttribute();
        output.setName("tradeState");
        RTypeCall outputTc = new RTypeCall();
        outputTc.setTypeName("TradeState");
        output.setTypeCall(outputTc);
        fn.setOutput(output);

        StubExpression aliasExpr = new StubExpression();
        RShortcut sc = new RShortcut();
        sc.setName("tradeDate");
        sc.setDefinition("The trade date.");
        sc.setExpression(aliasExpr);
        fn.shortcuts().add(sc);

        StubExpression condExpr = new StubExpression();
        RCondition cond = new RCondition();
        cond.setName("PositiveAmount");
        cond.setExpression(condExpr);
        fn.conditions().add(cond);

        StubExpression opExpr = new StubExpression();
        ROperation op = new ROperation();
        op.setOperator(OperationOp.SET);
        op.setTargetName("result");
        op.setExpression(opExpr);
        fn.operations().add(op);

        StubExpression pcExpr = new StubExpression();
        RPostCondition pc = new RPostCondition();
        pc.setName("ResultValid");
        pc.setExpression(pcExpr);
        fn.postConditions().add(pc);

        // Top-level children: dispatch + input + output + shortcut + condition + operation + postCondition = 7
        List<? extends RNode> fnChildren = fn.children();
        assertEquals(7, fnChildren.size());

        // Verify shortcut children include its expression
        List<? extends RNode> scChildren = sc.children();
        assertEquals(1, scChildren.size());
        assertSame(aliasExpr, scChildren.get(0));

        // Verify condition children include its expression
        List<? extends RNode> condChildren = cond.children();
        assertEquals(1, condChildren.size());
        assertSame(condExpr, condChildren.get(0));

        // Verify operation children include its expression (no segment)
        List<? extends RNode> opChildren = op.children();
        assertEquals(1, opChildren.size());
        assertSame(opExpr, opChildren.get(0));

        // Verify post-condition children include its expression
        List<? extends RNode> pcChildren = pc.children();
        assertEquals(1, pcChildren.size());
        assertSame(pcExpr, pcChildren.get(0));
    }

    @Test
    void fullRule_deepChildrenTraversal() {
        // Build: reporting rule FpmlIrd1 from Trade: <"Extracts notional">
        //     trade -> notional -> amount as "Notional Amount"
        RRule r = new RRule();
        r.setKind(RuleKind.REPORTING);
        r.setName("FpmlIrd1");
        r.setDefinition("Extracts notional.");

        RTypeCall tc = new RTypeCall();
        tc.setTypeName("Trade");
        r.setFromType(tc);

        StubExpression expr = new StubExpression();
        r.setExpression(expr);
        r.setAlias("Notional Amount");

        List<? extends RNode> children = r.children();
        assertEquals(2, children.size());
        assertSame(tc, children.get(0));
        assertSame(expr, children.get(1));

        assertEquals(RuleKind.REPORTING, r.kind());
        assertEquals(Optional.of("Notional Amount"), r.alias());
    }

    @Test
    void operationWithSegmentChain_deepTraversal() {
        // Build: set result -> trade -> price: <expression>
        ROperation op = new ROperation();
        op.setOperator(OperationOp.SET);
        op.setTargetName("result");

        RSegment seg1 = new RSegment();
        seg1.setName("trade");
        RSegment seg2 = new RSegment();
        seg2.setName("price");
        seg1.setNext(seg2);
        op.setSegment(seg1);

        StubExpression expr = new StubExpression();
        op.setExpression(expr);

        // Operation children: segment + expression
        List<? extends RNode> opChildren = op.children();
        assertEquals(2, opChildren.size());
        assertSame(seg1, opChildren.get(0));
        assertSame(expr, opChildren.get(1));

        // Segment children: next segment
        List<? extends RNode> seg1Children = seg1.children();
        assertEquals(1, seg1Children.size());
        assertSame(seg2, seg1Children.get(0));

        // Leaf segment: no children
        assertTrue(seg2.children().isEmpty());
    }

    // =========================================================================
    // Integration: parse-and-build tests
    // =========================================================================

    @Nested
    class IntegrationParseAndBuild {

        @Test
        void parseFunctionMinimal() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "func MyFunc:\n"
                    + "    inputs:\n"
                    + "        value number (1..1)\n"
                    + "    output:\n"
                    + "        result number (1..1)");
            assertEquals(1, model.rootElements().size());
            assertInstanceOf(RFunction.class, model.rootElements().get(0));

            RFunction fn = (RFunction) model.rootElements().get(0);
            assertEquals("MyFunc", fn.name());
            assertEquals(Optional.empty(), fn.dispatch());
            assertEquals(Optional.empty(), fn.superFunctionName());
            assertEquals(Optional.empty(), fn.definition());

            assertEquals(1, fn.inputs().size());
            assertEquals("value", fn.inputs().get(0).name());
            assertEquals("number", fn.inputs().get(0).typeCall().typeName());

            assertTrue(fn.output().isPresent());
            assertEquals("result", fn.output().get().name());
            assertEquals("number", fn.output().get().typeCall().typeName());

            assertTrue(fn.shortcuts().isEmpty());
            assertTrue(fn.conditions().isEmpty());
            assertTrue(fn.operations().isEmpty());
            assertTrue(fn.postConditions().isEmpty());
            assertSourceRangeSet(fn);
        }

        @Test
        void parseFunctionWithDefinition() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "func MyFunc: <\"A simple function.\">\n"
                    + "    output:\n"
                    + "        result number (1..1)");
            RFunction fn = (RFunction) model.rootElements().get(0);
            assertEquals(Optional.of("A simple function."), fn.definition());
            assertTrue(fn.inputs().isEmpty());
            assertTrue(fn.output().isPresent());
        }

        @Test
        void parseFunctionWithExtends() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "func MyFunc extends BaseFunc:\n"
                    + "    output:\n"
                    + "        result string (1..1)");
            RFunction fn = (RFunction) model.rootElements().get(0);
            assertEquals("MyFunc", fn.name());
            assertEquals(Optional.of("BaseFunc"), fn.superFunctionName());
        }

        @Test
        void parseFunctionWithDispatch() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "func Qualify_Trade(tradeType : TradeTypeEnum -> InterestRate):\n"
                    + "    output:\n"
                    + "        is_product boolean (1..1)");
            RFunction fn = (RFunction) model.rootElements().get(0);
            assertEquals("Qualify_Trade", fn.name());

            assertTrue(fn.dispatch().isPresent());
            RDispatch d = fn.dispatch().get();
            assertEquals("tradeType", d.paramName());
            assertEquals("TradeTypeEnum", d.enumRef());
            assertEquals("InterestRate", d.valueName());
        }

        @Test
        void parseFunctionWithMultipleInputs() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "func Create_TradeState:\n"
                    + "    inputs:\n"
                    + "        trade Trade (1..1)\n"
                    + "        event Event (0..1)\n"
                    + "    output:\n"
                    + "        result TradeState (1..1)");
            RFunction fn = (RFunction) model.rootElements().get(0);
            assertEquals(2, fn.inputs().size());
            assertEquals("trade", fn.inputs().get(0).name());
            assertEquals("Trade", fn.inputs().get(0).typeCall().typeName());
            assertEquals("event", fn.inputs().get(1).name());
            assertEquals("Event", fn.inputs().get(1).typeCall().typeName());
            assertTrue(fn.output().isPresent());
            assertEquals("result", fn.output().get().name());
        }

        @Test
        void parseFunctionWithConditionAndOperation() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "func MyFunc:\n"
                    + "    inputs:\n"
                    + "        value number (1..1)\n"
                    + "    output:\n"
                    + "        result number (1..1)\n"
                    + "    condition PositiveValue: <\"Value must be positive.\">\n"
                    + "        value > 0\n"
                    + "    set result:\n"
                    + "        value + 1");
            RFunction fn = (RFunction) model.rootElements().get(0);
            assertEquals(1, fn.conditions().size());

            RCondition cond = fn.conditions().get(0);
            assertEquals(Optional.of("PositiveValue"), cond.name());
            assertEquals(Optional.of("Value must be positive."), cond.definition());
            assertSourceRangeSet(cond);

            assertEquals(1, fn.operations().size());
            ROperation op = fn.operations().get(0);
            assertEquals(OperationOp.SET, op.operator());
            assertEquals("result", op.targetName());
            assertSourceRangeSet(op);
        }

        @Test
        void parseFunctionWithAddOperationAndSegment() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "func MyFunc:\n"
                    + "    output:\n"
                    + "        result TradeState (1..1)\n"
                    + "    add result -> parties:\n"
                    + "        party");
            RFunction fn = (RFunction) model.rootElements().get(0);
            assertEquals(1, fn.operations().size());

            ROperation op = fn.operations().get(0);
            assertEquals(OperationOp.ADD, op.operator());
            assertEquals("result", op.targetName());
            assertTrue(op.segment().isPresent());
            assertEquals("parties", op.segment().get().name());
        }

        @Test
        void parseFunctionWithShortcut() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "func MyFunc:\n"
                    + "    inputs:\n"
                    + "        trade Trade (1..1)\n"
                    + "    output:\n"
                    + "        result number (1..1)\n"
                    + "    alias tradeDate: <\"The trade date.\">\n"
                    + "        trade -> tradeDate\n"
                    + "    set result:\n"
                    + "        1");
            RFunction fn = (RFunction) model.rootElements().get(0);
            assertEquals(1, fn.shortcuts().size());

            RShortcut sc = fn.shortcuts().get(0);
            assertEquals("tradeDate", sc.name());
            assertEquals(Optional.of("The trade date."), sc.definition());
            assertSourceRangeSet(sc);
        }

        @Test
        void parseFunctionWithPostCondition() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "func MyFunc:\n"
                    + "    output:\n"
                    + "        result number (1..1)\n"
                    + "    set result:\n"
                    + "        42\n"
                    + "    post-condition ResultPositive: <\"Result must be positive.\">\n"
                    + "        result > 0");
            RFunction fn = (RFunction) model.rootElements().get(0);
            assertEquals(1, fn.postConditions().size());

            RPostCondition pc = fn.postConditions().get(0);
            assertEquals(Optional.of("ResultPositive"), pc.name());
            assertEquals(Optional.of("Result must be positive."), pc.definition());
            assertSourceRangeSet(pc);
        }

        @Test
        void parseFunctionNoInputsOrOutput() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "func Empty:");
            RFunction fn = (RFunction) model.rootElements().get(0);
            assertEquals("Empty", fn.name());
            assertTrue(fn.inputs().isEmpty());
            assertEquals(Optional.empty(), fn.output());
        }

        @Test
        void parseReportingRule() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "reporting rule FpmlIrd1 from Trade: <\"Extracts the notional.\">\n"
                    + "    trade -> notional -> amount as \"Notional Amount\"");
            assertEquals(1, model.rootElements().size());
            assertInstanceOf(RRule.class, model.rootElements().get(0));

            RRule rule = (RRule) model.rootElements().get(0);
            assertEquals(RuleKind.REPORTING, rule.kind());
            assertEquals("FpmlIrd1", rule.name());
            assertTrue(rule.fromType().isPresent());
            assertEquals("Trade", rule.fromType().get().typeName());
            assertEquals(Optional.of("Extracts the notional."), rule.definition());
            assertEquals(Optional.of("Notional Amount"), rule.alias());
            assertSourceRangeSet(rule);
        }

        @Test
        void parseEligibilityRule() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "eligibility rule IsReportable from Trade:\n"
                    + "    trade -> status = TradeStatusEnum -> Confirmed");
            RRule rule = (RRule) model.rootElements().get(0);
            assertEquals(RuleKind.ELIGIBILITY, rule.kind());
            assertEquals("IsReportable", rule.name());
            assertTrue(rule.fromType().isPresent());
            assertEquals("Trade", rule.fromType().get().typeName());
            assertEquals(Optional.empty(), rule.alias());
        }

        @Test
        void parseRuleNoFromType() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "reporting rule SimpleRule:\n"
                    + "    True");
            RRule rule = (RRule) model.rootElements().get(0);
            assertEquals("SimpleRule", rule.name());
            assertEquals(Optional.empty(), rule.fromType());
        }

        @Test
        void parseFunctionWithAnnotationRef() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "func MyFunc:\n"
                    + "    [rootType]\n"
                    + "    output:\n"
                    + "        result number (1..1)");
            RFunction fn = (RFunction) model.rootElements().get(0);
            assertEquals(1, fn.annotationRefs().size());
            assertEquals("rootType", fn.annotationRefs().get(0).annotationName());
        }

        @Test
        void parseFunctionWithDocReference() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "body Authority TestBody\n"
                    + "corpus Regulation TestBody \"TestCorpus\" TestCorpus\n"
                    + "func MyFunc:\n"
                    + "    [regulatoryReference TestBody TestCorpus provision \"Some provision\"]\n"
                    + "    output:\n"
                    + "        result number (1..1)");
            RFunction fn = (RFunction) model.rootElements().get(2);
            assertEquals("MyFunc", fn.name());
            assertEquals(1, fn.docReferences().size());
            var docRef = fn.docReferences().get(0);
            assertTrue(docRef.isRegulatoryReference());
            assertTrue(docRef.provision().isPresent());
            assertEquals("Some provision", docRef.provision().get());
        }
    }
}
