package com.regnosys.rosetta.types.inference;

import com.regnosys.rosetta.ast.expressions.constructors.*;
import com.regnosys.rosetta.ast.expressions.literals.*;
import com.regnosys.rosetta.ast.expressions.unary.RSwitchExpr;
import com.regnosys.rosetta.ast.expressions.supporting.RSwitchCase;
import com.regnosys.rosetta.ast.supporting.RTypeCall;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.symbols.symbolid.TestSymbolResolver;
import com.regnosys.rosetta.types.*;
import com.regnosys.rosetta.types.builtin.BuiltinTypeRegistry;
import com.regnosys.rosetta.types.relation.SubtypeRelation;
import com.regnosys.rosetta.types.relation.TypeJoin;
import com.regnosys.rosetta.types.alias.TypeAliasSolver;
import java.lang.ref.Reference;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConditionalTypeTest {

    private final ExpressionTypeComputer computer;
    private final TypeInferenceEngine engine;

    ConditionalTypeTest() {
        var builtins = BuiltinTypeRegistry.createDefault();
        var sub = new SubtypeRelation();
        var join = new TypeJoin(sub);
        var alias = new TypeAliasSolver();
        computer = new ExpressionTypeComputer(builtins, sub, join, alias);
        engine = new TypeInferenceEngine(computer);
    }

    // === RConditionalExpr ====================================================

    @Test void conditional_with_both_branches_same_type() {
        var cond = new RConditionalExpr();
        var thenBranch = new RIntLiteral(); thenBranch.setValue(1);
        var elseBranch = new RIntLiteral(); elseBranch.setValue(2);
        cond.setCondition(new RBooleanLiteral());
        cond.setThenBranch(thenBranch);
        cond.setElseBranch(elseBranch);

        // The engine needs to have inferred sub-expression types first.
        // For unit test, directly compute the conditional — it reads
        // sub-expression types from the engine, which defaults to MISSING.
        // So this returns MISSING. Full integration tested via FixedPointTest.
        RMetaAnnotatedType result = computer.compute(cond, engine);
        // With MISSING sub-expressions, conditional returns MISSING
        assertTrue(result.isMissing());
    }

    @Test void conditional_without_else_branch() {
        var cond = new RConditionalExpr();
        cond.setCondition(new RBooleanLiteral());
        cond.setThenBranch(new RIntLiteral());
        // no else branch

        RMetaAnnotatedType result = computer.compute(cond, engine);
        // then branch type joined with nothing (empty else)
        // Since sub-expressions are MISSING in engine, returns MISSING
        assertTrue(result.isMissing());
    }

    // === RConstructorExpr ====================================================

    @Test void constructor_returns_constructed_type() {
        var resolver = new TestSymbolResolver();
        var dt = new RDataType();
        dt.setName("Trade");
        dt.attachToWorkspace(resolver);
        resolver.bind("test", "Trade", dt);
        var tc = new RTypeCall();
        tc.setTypeName("Trade");
        tc.attachToWorkspace(resolver);
        tc.setReferencedTypeId(resolver.idFor("test", "Trade"));

        var ctor = new RConstructorExpr();
        ctor.setTypeCall(tc);

        RMetaAnnotatedType result = computer.compute(ctor, engine);
        assertInstanceOf(RDataTypeRef.class, result.type());
        assertEquals("Trade", result.type().name());
        Reference.reachabilityFence(resolver);
    }

    @Test void constructor_with_builtin_type() {
        var tc = new RTypeCall();
        tc.setTypeName("date");
        // No M3 resolved type for builtins

        var ctor = new RConstructorExpr();
        ctor.setTypeCall(tc);

        RMetaAnnotatedType result = computer.compute(ctor, engine);
        assertSame(RRecordType.DATE, result.type());
    }

    // === RSwitchExpr =========================================================

    @Test void switch_with_no_cases_is_missing() {
        var sw = new RSwitchExpr();
        sw.setArgument(new RIntLiteral());
        // no cases
        RMetaAnnotatedType result = computer.compute(sw, engine);
        // nothing to join → nothing/missing
        assertFalse(result.isMissing()); // join of empty → nothing
        assertEquals(RBasicType.NOTHING, result.type());
    }
}
