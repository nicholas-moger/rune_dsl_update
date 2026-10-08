package com.regnosys.rosetta.types.inference;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.expressions.literals.*;
import com.regnosys.rosetta.types.*;
import com.regnosys.rosetta.types.builtin.BuiltinTypeRegistry;
import com.regnosys.rosetta.types.relation.SubtypeRelation;
import com.regnosys.rosetta.types.relation.TypeJoin;
import com.regnosys.rosetta.types.alias.TypeAliasSolver;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.*;

class LiteralTypeTest {

    private final ExpressionTypeComputer computer;
    private final TypeInferenceEngine engine;

    LiteralTypeTest() {
        var builtins = BuiltinTypeRegistry.createDefault();
        var sub = new SubtypeRelation();
        var join = new TypeJoin(sub);
        var alias = new TypeAliasSolver();
        computer = new ExpressionTypeComputer(builtins, sub, join, alias);
        engine = new TypeInferenceEngine(computer);
    }

    // === Int literals =========================================================

    @Test void int_literal_42() {
        var lit = new RIntLiteral();
        lit.setValue(42);
        RMetaAnnotatedType t = computer.compute(lit, engine);
        assertInstanceOf(RNumberType.class, t.type());
        var n = (RNumberType) t.type();
        assertTrue(n.isInteger());
        assertEquals(2, n.digits().orElseThrow()); // 42 has 2 digits
    }

    @Test void int_literal_0() {
        var lit = new RIntLiteral();
        lit.setValue(0);
        RMetaAnnotatedType t = computer.compute(lit, engine);
        assertInstanceOf(RNumberType.class, t.type());
        assertEquals(1, ((RNumberType) t.type()).digits().orElseThrow());
    }

    @Test void int_literal_large() {
        var lit = new RIntLiteral();
        lit.setValue(new BigInteger("12345678901234567890"));
        RMetaAnnotatedType t = computer.compute(lit, engine);
        assertInstanceOf(RNumberType.class, t.type());
        assertEquals(20, ((RNumberType) t.type()).digits().orElseThrow());
    }

    // === Number (decimal) literals ============================================

    @Test void decimal_literal() {
        var lit = new RNumberLiteral();
        lit.setValue(new BigDecimal("3.14"));
        RMetaAnnotatedType t = computer.compute(lit, engine);
        assertInstanceOf(RNumberType.class, t.type());
        var n = (RNumberType) t.type();
        assertFalse(n.isInteger());
        assertEquals(3, n.digits().orElseThrow()); // 3.14 has 3 digits total
        assertEquals(2, n.fractionalDigits().orElseThrow()); // 2 after decimal
    }

    // === String literals =====================================================

    @Test void string_literal() {
        var lit = new RStringLiteral();
        lit.setValue("hello");
        RMetaAnnotatedType t = computer.compute(lit, engine);
        assertInstanceOf(RStringType.class, t.type());
        var s = (RStringType) t.type();
        assertEquals(5, s.minLength().orElseThrow());
        assertEquals(5, s.maxLength().orElseThrow());
    }

    @Test void empty_string_literal() {
        var lit = new RStringLiteral();
        lit.setValue("");
        RMetaAnnotatedType t = computer.compute(lit, engine);
        assertInstanceOf(RStringType.class, t.type());
        assertEquals(0, ((RStringType) t.type()).minLength().orElseThrow());
    }

    // === Boolean literals ====================================================

    @Test void boolean_true() {
        var lit = new RBooleanLiteral();
        lit.setValue(true);
        RMetaAnnotatedType t = computer.compute(lit, engine);
        assertEquals(RBasicType.BOOLEAN, t.type());
    }

    @Test void boolean_false() {
        var lit = new RBooleanLiteral();
        lit.setValue(false);
        RMetaAnnotatedType t = computer.compute(lit, engine);
        assertEquals(RBasicType.BOOLEAN, t.type());
    }

    // === Empty literal =======================================================

    @Test void empty_literal_is_nothing() {
        var lit = new com.regnosys.rosetta.ast.expressions.references.REmptyLiteral();
        RMetaAnnotatedType t = computer.compute(lit, engine);
        assertEquals(RBasicType.NOTHING, t.type());
    }

    // === List literal ========================================================

    @Test void list_literal_empty_is_nothing() {
        var lit = new RListLiteral();
        RMetaAnnotatedType t = computer.compute(lit, engine);
        assertEquals(RBasicType.NOTHING, t.type());
    }
}
