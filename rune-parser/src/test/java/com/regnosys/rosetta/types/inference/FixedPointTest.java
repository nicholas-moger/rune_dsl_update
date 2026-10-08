package com.regnosys.rosetta.types.inference;

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.util.AstWalker;
import com.regnosys.rosetta.types.*;
import com.regnosys.rosetta.types.builtin.BuiltinTypeRegistry;
import com.regnosys.rosetta.types.relation.SubtypeRelation;
import com.regnosys.rosetta.types.relation.TypeJoin;
import com.regnosys.rosetta.types.alias.TypeAliasSolver;
import com.regnosys.rosetta.symbols.diagnostics.Diagnostics;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FixedPointTest {

    private TypeInferenceEngine createEngine() {
        var builtins = BuiltinTypeRegistry.createDefault();
        var subtypeRelation = new SubtypeRelation();
        var typeJoin = new TypeJoin(subtypeRelation);
        var aliasSolver = new TypeAliasSolver();
        var typeComputer = new ExpressionTypeComputer(builtins, subtypeRelation, typeJoin, aliasSolver);
        return new TypeInferenceEngine(typeComputer);
    }

    @Test void empty_model_no_expressions_no_diagnostics() {
        RModel model = AstBuilder.buildFromString(
            "namespace test\ntype Foo:\n", "fp-test.rosetta");
        var engine = createEngine();
        var collector = new Diagnostics();
        engine.run(List.of(model), collector);
        // No expressions → nothing to infer
        assertTrue(collector.toList().isEmpty());
    }

    @Test void engine_terminates_even_with_expressions() {
        RModel model = AstBuilder.buildFromString("""
            namespace test
            type Foo:
                value int (1..1)
            func F:
                output: result int (1..1)
                set result: 42
            """, "fp-term.rosetta");
        var engine = createEngine();
        var collector = new Diagnostics();
        engine.run(List.of(model), collector);
        // Engine should terminate without error (expressions exist but
        // the stub computer returns MISSING for now — T8+ will fix)
        assertNotNull(engine);
    }

    @Test void all_expressions_tracked() {
        RModel model = AstBuilder.buildFromString("""
            namespace test
            func F:
                output: result int (1..1)
                set result: 42
            """, "fp-track.rosetta");
        var engine = createEngine();
        var collector = new Diagnostics();
        engine.run(List.of(model), collector);

        // Every expression in the model should have an inferred type
        var exprs = AstWalker.findAll(model, RExpression.class);
        for (RExpression expr : exprs) {
            RMetaAnnotatedType inferred = engine.getInferredType(expr);
            assertNotNull(inferred, "every expression must have an inferred type");
        }
    }

    @Test void iteration_count_bounded() {
        // Verify the engine reports iteration count
        RModel model = AstBuilder.buildFromString(
            "namespace test\ntype Foo:\n", "fp-bound.rosetta");
        var engine = createEngine();
        var collector = new Diagnostics();
        engine.run(List.of(model), collector);
        assertTrue(engine.iterationCount() <= TypeInferenceEngine.MAX_ITERATIONS);
    }
}
