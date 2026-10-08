package com.regnosys.rosetta.ast.builder;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.expressions.binary.RThenExpr;
import com.regnosys.rosetta.ast.expressions.references.RImplicitVariable;
import com.regnosys.rosetta.ast.expressions.references.RSymbolReference;
import com.regnosys.rosetta.ast.expressions.supporting.RInlineFunction;
import com.regnosys.rosetta.ast.expressions.unary.RCountExpr;
import com.regnosys.rosetta.ast.expressions.unary.RExtractExpr;
import com.regnosys.rosetta.ast.expressions.unary.RFilterExpr;
import com.regnosys.rosetta.ast.expressions.unary.RListOpExpr;
import com.regnosys.rosetta.ast.expressions.unary.RMaxExpr;
import com.regnosys.rosetta.ast.expressions.unary.RMinExpr;
import com.regnosys.rosetta.ast.expressions.unary.RReduceExpr;
import com.regnosys.rosetta.ast.expressions.unary.RSortExpr;
import com.regnosys.rosetta.ast.functions.RRule;
import com.regnosys.rosetta.ast.model.RModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Locks the implicit-input materialisation invariant: when a without-left
 * list / extract / filter / conversion / to-string operation is written with
 * the operand elided (e.g. a bare {@code only-element} as a {@code then}-lambda
 * body, or a top-level {@code extract bar} in a rule body), the
 * {@link com.regnosys.rosetta.ast.builder.AstBuilder} must synthesise an
 * {@link RImplicitVariable} as the operand rather than leaving it {@code null}.
 *
 * <p>Mirrors upstream rune-dsl, which materialises {@code RosettaImplicitVariable}
 * for the implicit input. A null operand is an under-specified AST: it forces
 * every downstream consumer (type inference, Java codegen, future IR consumers)
 * to re-derive "null means implicit input" from context. Materialising the node
 * keeps the tree faithful for all consumers from one place.
 */
class AstBuilderImplicitOperandTest {

    @Test
    void elidedListOpOperand_synthesisesImplicitVariable() {
        String source = """
                namespace test

                reporting rule TestRule from Foo:
                    bar then only-element
                """;
        RModel model = AstBuilder.buildFromString(source, "AstBuilderImplicitOperandTest.rosetta");

        RRule rule = (RRule) model.rootElements().get(0);
        RThenExpr then = assertInstanceOf(RThenExpr.class, rule.expression().orElseThrow(),
                "rule body should be a then-chain");
        RInlineFunction lambda = then.body().orElseThrow(
                () -> new AssertionError("then should carry an (implicit) inline-function body"));
        RListOpExpr listOp = assertInstanceOf(RListOpExpr.class, lambda.body(),
                "then-lambda body should be the only-element list op");

        assertNotNull(listOp.argument(),
                "elided list-op operand must not be left null");
        var iv = assertInstanceOf(RImplicitVariable.class, listOp.argument(),
                "elided list-op operand must be materialised as RImplicitVariable");
        assertTrue(iv.isSynthetic(),
                "elided list-op operand must carry isSynthetic()=true so "
                + "downstream predicates can distinguish it from a literal "
                + "`item` keyword used as an explicit receiver");
    }

    @Test
    void elidedExtractOperand_synthesisesImplicitVariable() {
        // Rule body shape: `extract X -> Y` with no LHS — operand is the rule's
        // implicit input. EnrichmentDataRule in DRR 6.29 is the canonical case.
        String source = """
                namespace test

                reporting rule TestRule from Foo:
                    extract bar
                """;
        RModel model = AstBuilder.buildFromString(source, "AstBuilderImplicitOperandTest.rosetta");

        RRule rule = (RRule) model.rootElements().get(0);
        RExtractExpr extract = assertInstanceOf(RExtractExpr.class, rule.expression().orElseThrow(),
                "rule body should be an extract with implicit operand");

        assertNotNull(extract.argument(),
                "elided extract operand must not be left null");
        assertInstanceOf(RImplicitVariable.class, extract.argument(),
                "elided extract operand must be materialised as RImplicitVariable");
    }

    @Test
    void elidedToStringOperand_synthesisesImplicitVariable() {
        // Copilot R3 F1 — RToStringExpr is a separate class from RConversionExpr;
        // visitToStringWithoutLeftExpr now also synthesises the elided operand.
        String source = """
                namespace test

                reporting rule TestRule from Foo:
                    bar then to-string
                """;
        RModel model = AstBuilder.buildFromString(source, "AstBuilderImplicitOperandTest.rosetta");

        RRule rule = (RRule) model.rootElements().get(0);
        RThenExpr then = assertInstanceOf(RThenExpr.class, rule.expression().orElseThrow(),
                "rule body should be a then-chain");
        RInlineFunction lambda = then.body().orElseThrow(
                () -> new AssertionError("then should carry an (implicit) inline-function body"));
        var toStr = assertInstanceOf(
                com.regnosys.rosetta.ast.expressions.unary.RToStringExpr.class, lambda.body(),
                "then-lambda body should be the to-string op");

        assertNotNull(toStr.argument(),
                "elided to-string operand must not be left null");
        assertInstanceOf(RImplicitVariable.class, toStr.argument(),
                "elided to-string operand must be materialised as RImplicitVariable");
    }

    @Test
    void elidedFilterOperand_synthesisesImplicitVariable() {
        // Rule body shape: `filter <predicate>` with no LHS — operand is the
        // rule's implicit input.
        String source = """
                namespace test

                reporting rule TestRule from Foo:
                    filter bar exists
                """;
        RModel model = AstBuilder.buildFromString(source, "AstBuilderImplicitOperandTest.rosetta");

        RRule rule = (RRule) model.rootElements().get(0);
        RFilterExpr filter = assertInstanceOf(RFilterExpr.class, rule.expression().orElseThrow(),
                "rule body should be a filter with implicit operand");

        assertNotNull(filter.argument(),
                "elided filter operand must not be left null");
        assertInstanceOf(RImplicitVariable.class, filter.argument(),
                "elided filter operand must be materialised as RImplicitVariable");
    }

    @Test
    void explicitExtractOperand_preservesOperand() {
        // Regression guard: explicit-LHS extract must keep its operand.
        String source = """
                namespace test

                reporting rule TestRule from Foo:
                    bar extract baz
                """;
        RModel model = AstBuilder.buildFromString(source, "AstBuilderImplicitOperandTest.rosetta");

        RRule rule = (RRule) model.rootElements().get(0);
        RExtractExpr extract = assertInstanceOf(RExtractExpr.class, rule.expression().orElseThrow(),
                "rule body should be an extract with explicit operand");

        RExpression arg = extract.argument();
        assertNotNull(arg, "explicit operand must be present");
        assertInstanceOf(RSymbolReference.class, arg,
                "explicit operand must stay the written symbol reference, not be replaced");
    }

    @Test
    void explicitFilterOperand_preservesOperand() {
        // Regression guard: explicit-LHS filter must keep its operand.
        String source = """
                namespace test

                reporting rule TestRule from Foo:
                    bar filter baz exists
                """;
        RModel model = AstBuilder.buildFromString(source, "AstBuilderImplicitOperandTest.rosetta");

        RRule rule = (RRule) model.rootElements().get(0);
        RFilterExpr filter = assertInstanceOf(RFilterExpr.class, rule.expression().orElseThrow(),
                "rule body should be a filter with explicit operand");

        RExpression arg = filter.argument();
        assertNotNull(arg, "explicit operand must be present");
        assertInstanceOf(RSymbolReference.class, arg,
                "explicit operand must stay the written symbol reference, not be replaced");
    }

    // === Engine PR #2 Bucket A — residual 5 without-left visitors ============

    @Test
    void elidedCountOperand_synthesisesImplicitVariable() {
        // `count` without left receiver at top level — engine PR #2 extension.
        String source = """
                namespace test

                reporting rule TestRule from Foo:
                    count
                """;
        RModel model = AstBuilder.buildFromString(source, "AstBuilderImplicitOperandTest.rosetta");
        RRule rule = (RRule) model.rootElements().get(0);
        var count = assertInstanceOf(RCountExpr.class, rule.expression().orElseThrow());
        var iv = assertInstanceOf(RImplicitVariable.class, count.argument(),
                "elided count operand must be materialised as RImplicitVariable");
        assertTrue(iv.isSynthetic(), "elided count operand must carry isSynthetic()=true");
    }

    @Test
    void elidedSortOperand_synthesisesImplicitVariable() {
        String source = """
                namespace test

                reporting rule TestRule from Foo:
                    sort
                """;
        RModel model = AstBuilder.buildFromString(source, "AstBuilderImplicitOperandTest.rosetta");
        RRule rule = (RRule) model.rootElements().get(0);
        var sort = assertInstanceOf(RSortExpr.class, rule.expression().orElseThrow());
        var iv = assertInstanceOf(RImplicitVariable.class, sort.argument(),
                "elided sort operand must be materialised as RImplicitVariable");
        assertTrue(iv.isSynthetic(), "elided sort operand must carry isSynthetic()=true");
    }

    @Test
    void elidedMinOperand_synthesisesImplicitVariable() {
        String source = """
                namespace test

                reporting rule TestRule from Foo:
                    min
                """;
        RModel model = AstBuilder.buildFromString(source, "AstBuilderImplicitOperandTest.rosetta");
        RRule rule = (RRule) model.rootElements().get(0);
        var min = assertInstanceOf(RMinExpr.class, rule.expression().orElseThrow());
        var iv = assertInstanceOf(RImplicitVariable.class, min.argument(),
                "elided min operand must be materialised as RImplicitVariable");
        assertTrue(iv.isSynthetic(), "elided min operand must carry isSynthetic()=true");
    }

    @Test
    void elidedMaxOperand_synthesisesImplicitVariable() {
        String source = """
                namespace test

                reporting rule TestRule from Foo:
                    max
                """;
        RModel model = AstBuilder.buildFromString(source, "AstBuilderImplicitOperandTest.rosetta");
        RRule rule = (RRule) model.rootElements().get(0);
        var max = assertInstanceOf(RMaxExpr.class, rule.expression().orElseThrow());
        var iv = assertInstanceOf(RImplicitVariable.class, max.argument(),
                "elided max operand must be materialised as RImplicitVariable");
        assertTrue(iv.isSynthetic(), "elided max operand must carry isSynthetic()=true");
    }

    @Test
    void elidedReduceOperand_synthesisesImplicitVariable() {
        String source = """
                namespace test

                reporting rule TestRule from Foo:
                    reduce [a, b -> a]
                """;
        RModel model = AstBuilder.buildFromString(source, "AstBuilderImplicitOperandTest.rosetta");
        RRule rule = (RRule) model.rootElements().get(0);
        var reduce = assertInstanceOf(RReduceExpr.class, rule.expression().orElseThrow());
        var iv = assertInstanceOf(RImplicitVariable.class, reduce.argument(),
                "elided reduce operand must be materialised as RImplicitVariable");
        assertTrue(iv.isSynthetic(), "elided reduce operand must carry isSynthetic()=true");
    }

    @Test
    void explicitListOpOperand_preservesOperand() {
        // Regression guard: the with-argument form must keep its explicit operand
        // (the fix must only fire when the operand is elided).
        String source = """
                namespace test

                reporting rule TestRule from Foo:
                    bar only-element
                """;
        RModel model = AstBuilder.buildFromString(source, "AstBuilderImplicitOperandTest.rosetta");

        RRule rule = (RRule) model.rootElements().get(0);
        RListOpExpr listOp = assertInstanceOf(RListOpExpr.class, rule.expression().orElseThrow(),
                "rule body should be a list op with an explicit operand");

        RExpression arg = listOp.argument();
        assertNotNull(arg, "explicit operand must be present");
        assertInstanceOf(RSymbolReference.class, arg,
                "explicit operand must stay the written symbol reference, not be replaced");
    }
}
