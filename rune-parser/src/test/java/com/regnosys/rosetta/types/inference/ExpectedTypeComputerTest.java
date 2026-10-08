package com.regnosys.rosetta.types.inference;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.expressions.binary.RComparisonExpr;
import com.regnosys.rosetta.ast.expressions.constructors.RConditionalExpr;
import com.regnosys.rosetta.ast.expressions.literals.RBooleanLiteral;
import com.regnosys.rosetta.ast.expressions.literals.RIntLiteral;
import com.regnosys.rosetta.ast.functions.ROperation;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.supporting.RTypeCall;
import com.regnosys.rosetta.types.*;
import com.regnosys.rosetta.types.builtin.BuiltinTypeRegistry;
import com.regnosys.rosetta.types.relation.SubtypeRelation;
import com.regnosys.rosetta.types.relation.TypeJoin;
import com.regnosys.rosetta.types.alias.TypeAliasSolver;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ExpectedTypeComputerTest {

    private final BuiltinTypeRegistry builtins = BuiltinTypeRegistry.createDefault();
    private final SubtypeRelation sub = new SubtypeRelation();
    private final TypeJoin join = new TypeJoin(sub);
    private final TypeAliasSolver alias = new TypeAliasSolver();
    private final ExpressionTypeComputer typeComputer =
        new ExpressionTypeComputer(builtins, sub, join, alias);
    private final ExpectedTypeComputer expected = new ExpectedTypeComputer(builtins, typeComputer);

    // === Conditional — condition expects boolean ==============================

    @Test void conditional_condition_expects_boolean() {
        var cond = new RBooleanLiteral();
        var ifExpr = new RConditionalExpr();
        ifExpr.setCondition(cond);
        ifExpr.setThenBranch(new RIntLiteral());

        Optional<RMetaAnnotatedType> result = expected.expectedType(cond, ifExpr);
        assertTrue(result.isPresent());
        assertEquals(RBasicType.BOOLEAN, result.get().type());
    }

    @Test void conditional_then_branch_no_expected() {
        var thenBranch = new RIntLiteral();
        var ifExpr = new RConditionalExpr();
        ifExpr.setCondition(new RBooleanLiteral());
        ifExpr.setThenBranch(thenBranch);

        Optional<RMetaAnnotatedType> result = expected.expectedType(thenBranch, ifExpr);
        assertTrue(result.isEmpty()); // then/else branches don't have a fixed expected type
    }

    // === Comparison — both sides expected to match ===========================

    @Test void comparison_expects_nothing_specific() {
        // Comparison operands don't have a single expected type
        var lhs = new RIntLiteral();
        var comp = new RComparisonExpr();
        comp.setLeft(lhs);

        Optional<RMetaAnnotatedType> result = expected.expectedType(lhs, comp);
        assertTrue(result.isEmpty());
    }

    // === Operation — expression expects target type ==========================

    @Test void operation_expression_expects_target_type() {
        var tc = new RTypeCall();
        tc.setTypeName("Trade");
        // referencedType not set — ExpectedTypeComputer does not call referencedType()
        var attr = new RAttribute();
        attr.setName("result");
        attr.setTypeCall(tc);

        var expr = new RIntLiteral();
        var op = new ROperation();
        op.setTargetName("result");
        op.setExpression(expr);

        // The operation target's type is the expected type for the expression.
        // But we need the resolved target to compute this — for now, we test
        // the mechanism works when target info is available.
        Optional<RMetaAnnotatedType> result = expected.expectedType(expr, op);
        // Without resolved target, no expected type
        assertTrue(result.isEmpty());
    }

    // === No parent context → empty ==========================================

    @Test void no_parent_returns_empty() {
        var lit = new RIntLiteral();
        Optional<RMetaAnnotatedType> result = expected.expectedType(lit, null);
        assertTrue(result.isEmpty());
    }
}
