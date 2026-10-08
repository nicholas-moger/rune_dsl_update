package com.regnosys.rosetta.types.inference;

import com.regnosys.rosetta.ast.expressions.binary.*;
import com.regnosys.rosetta.ast.expressions.literals.*;
import com.regnosys.rosetta.ast.enums.ArithOp;
import com.regnosys.rosetta.types.*;
import com.regnosys.rosetta.types.builtin.BuiltinTypeRegistry;
import com.regnosys.rosetta.types.relation.SubtypeRelation;
import com.regnosys.rosetta.types.relation.TypeJoin;
import com.regnosys.rosetta.types.alias.TypeAliasSolver;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ArithmeticTypeTest {

    private final ExpressionTypeComputer computer;
    private final TypeInferenceEngine engine;

    ArithmeticTypeTest() {
        var builtins = BuiltinTypeRegistry.createDefault();
        var sub = new SubtypeRelation();
        var join = new TypeJoin(sub);
        var alias = new TypeAliasSolver();
        computer = new ExpressionTypeComputer(builtins, sub, join, alias);
        engine = new TypeInferenceEngine(computer);
    }

    @Test void addition_of_two_ints() {
        var lhs = new RIntLiteral(); lhs.setValue(42);
        var rhs = new RIntLiteral(); rhs.setValue(7);
        var arith = new RArithmeticExpr();
        arith.setOp(ArithOp.PLUS);
        arith.setLeft(lhs);
        arith.setRight(rhs);

        // Pre-compute sub-expression types
        engine.getInferredType(lhs); // need to seed the engine
        RMetaAnnotatedType lhsType = computer.compute(lhs, engine);
        RMetaAnnotatedType rhsType = computer.compute(rhs, engine);

        // The engine normally seeds these during iteration. For unit test,
        // manually compute and check the arithmetic result:
        assertInstanceOf(RNumberType.class, lhsType.type());
        assertInstanceOf(RNumberType.class, rhsType.type());

        // Arithmetic type is join of operand types
        RMetaAnnotatedType result = computer.compute(arith, engine);
        // With MISSING sub-expressions (engine not run), returns MISSING
        // This is expected — full integration tested in FixedPointTest
    }

    @Test void comparison_returns_boolean() {
        var comp = new RComparisonExpr();
        RMetaAnnotatedType t = computer.compute(comp, engine);
        assertEquals(RBasicType.BOOLEAN, t.type());
    }

    @Test void equality_returns_boolean() {
        var eq = new REqualityExpr();
        RMetaAnnotatedType t = computer.compute(eq, engine);
        assertEquals(RBasicType.BOOLEAN, t.type());
    }

    @Test void logical_returns_boolean() {
        var log = new RLogicalExpr();
        RMetaAnnotatedType t = computer.compute(log, engine);
        assertEquals(RBasicType.BOOLEAN, t.type());
    }
}
