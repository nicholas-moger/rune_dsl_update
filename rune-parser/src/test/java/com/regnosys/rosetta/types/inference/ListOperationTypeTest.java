package com.regnosys.rosetta.types.inference;

import com.regnosys.rosetta.ast.enums.ConversionKind;
import com.regnosys.rosetta.ast.enums.ListOp;
import com.regnosys.rosetta.ast.expressions.unary.*;
import com.regnosys.rosetta.types.*;
import com.regnosys.rosetta.types.builtin.BuiltinTypeRegistry;
import com.regnosys.rosetta.types.relation.SubtypeRelation;
import com.regnosys.rosetta.types.relation.TypeJoin;
import com.regnosys.rosetta.types.alias.TypeAliasSolver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ListOperationTypeTest {

    private final ExpressionTypeComputer computer;
    private final TypeInferenceEngine engine;

    ListOperationTypeTest() {
        var builtins = BuiltinTypeRegistry.createDefault();
        var sub = new SubtypeRelation();
        var join = new TypeJoin(sub);
        var alias = new TypeAliasSolver();
        computer = new ExpressionTypeComputer(builtins, sub, join, alias);
        engine = new TypeInferenceEngine(computer);
    }

    @Test void count_returns_int() {
        var count = new RCountExpr();
        assertEquals(RNumberType.intType(), computer.compute(count, engine).type());
    }

    @Test void existence_returns_boolean() {
        var ex = new RExistenceExpr();
        assertEquals(RBasicType.BOOLEAN, computer.compute(ex, engine).type());
    }

    @Test void cardinality_check_returns_boolean() {
        var cc = new RCardinalityCheckExpr();
        assertEquals(RBasicType.BOOLEAN, computer.compute(cc, engine).type());
    }

    @Test void only_exists_returns_boolean() {
        var oe = new ROnlyExistsExpr();
        assertEquals(RBasicType.BOOLEAN, computer.compute(oe, engine).type());
    }

    @Test void to_string_returns_string() {
        var ts = new RToStringExpr();
        assertEquals(RStringType.unconstrained(), computer.compute(ts, engine).type());
    }

    // facet sumArgItemTyping (W42 finding #19, PR #432): a sum types as its
    // ARGUMENT's item type (upstream RosettaTypeProvider.caseSumOperation =
    // safeRType(expr.getArgument())), not the unconstrained-number fallback the
    // pre-#432 computer forced. The follows-argument positive lives in
    // SmokeTypeInferenceTest (sum_follows_argument_item_type) — an
    // argument-routed op needs the full workspace channel this detached harness
    // does not build; here the argument-less node computes MISSING like the
    // other argument-routed ops.
    @Test void list_op_sum_without_argument_is_missing() {
        var lop = new RListOpExpr();
        lop.setOp(ListOp.SUM);
        assertInstanceOf(RMissingType.class, computer.compute(lop, engine).type());
    }

    @Test void conversion_to_int() {
        var conv = new RConversionExpr();
        conv.setKind(ConversionKind.INT);
        assertInstanceOf(RNumberType.class, computer.compute(conv, engine).type());
        assertTrue(((RNumberType) computer.compute(conv, engine).type()).isInteger());
    }

    @Test void conversion_to_date() {
        var conv = new RConversionExpr();
        conv.setKind(ConversionKind.DATE);
        assertSame(RRecordType.DATE, computer.compute(conv, engine).type());
    }

    @Test void conversion_to_number() {
        var conv = new RConversionExpr();
        conv.setKind(ConversionKind.NUMBER);
        assertInstanceOf(RNumberType.class, computer.compute(conv, engine).type());
    }

    @Test void join_returns_string() {
        var join = new com.regnosys.rosetta.ast.expressions.binary.RJoinExpr();
        assertInstanceOf(RStringType.class, computer.compute(join, engine).type());
    }

    @Test void contains_returns_boolean() {
        var cont = new com.regnosys.rosetta.ast.expressions.binary.RContainsExpr();
        assertEquals(RBasicType.BOOLEAN, computer.compute(cont, engine).type());
    }

    @Test void disjoint_returns_boolean() {
        var dis = new com.regnosys.rosetta.ast.expressions.binary.RDisjointExpr();
        assertEquals(RBasicType.BOOLEAN, computer.compute(dis, engine).type());
    }
}
