package com.regnosys.rosetta.generator.java.expression.handlers;

import com.regnosys.rosetta.ast.enums.CardCheckOp;
import com.regnosys.rosetta.ast.enums.ExistenceOp;
import com.regnosys.rosetta.ast.enums.ExistsModifier;
import com.regnosys.rosetta.ast.enums.Necessity;
import com.regnosys.rosetta.ast.expressions.literals.RIntLiteral;
import com.regnosys.rosetta.ast.expressions.references.RSymbolReference;
import com.regnosys.rosetta.ast.expressions.supporting.ROnlyExistsElement;
import com.regnosys.rosetta.ast.expressions.unary.RCardinalityCheckExpr;
import com.regnosys.rosetta.ast.expressions.unary.RExistenceExpr;
import com.regnosys.rosetta.ast.expressions.unary.ROnlyExistsExpr;
import com.regnosys.rosetta.generator.java.expression.ExpressionCompiler;
import com.regnosys.rosetta.generator.java.expression.ExpressionCompilerTest;
import com.regnosys.rosetta.generator.java.expression.ExpressionContext;
import com.regnosys.rosetta.generator.java.statement.builder.JavaExpression;
import com.regnosys.rosetta.generator.java.statement.builder.JavaStatementBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ExistenceHandlerTest {

    private ExistenceHandler handler;
    private ExpressionCompiler compiler;
    private ExpressionContext ctx;

    @BeforeEach
    void setUp() {
        handler  = new ExistenceHandler();
        compiler = new ExpressionCompiler();
        ctx      = ExpressionContext.of(null, ExpressionCompilerTest.createTestScope());
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private String render(JavaStatementBuilder result) {
        assertInstanceOf(JavaExpression.class, result);
        return ((JavaExpression) result).renderToString();
    }

    private RSymbolReference symbolRef(String name) {
        var ref = new RSymbolReference();
        ref.setName(name);
        return ref;
    }

    private RIntLiteral intLiteral(int value) {
        var lit = new RIntLiteral();
        lit.setValue(value);
        return lit;
    }

    // =========================================================================
    // EXISTS (plain)
    // =========================================================================

    @Test
    void exists_no_modifier_generates_exists_call() {
        var expr = new RExistenceExpr();
        expr.setArgument(symbolRef("trade"));
        expr.setOp(ExistenceOp.EXISTS);

        assertEquals("exists(MapperS.of(trade))", render(handler.handle(expr, ctx, compiler)));
    }

    // =========================================================================
    // ABSENT
    // =========================================================================

    @Test
    void absent_generates_notExists_call() {
        var expr = new RExistenceExpr();
        expr.setArgument(symbolRef("trade"));
        expr.setOp(ExistenceOp.ABSENT);

        assertEquals("notExists(MapperS.of(trade))", render(handler.handle(expr, ctx, compiler)));
    }

    // =========================================================================
    // EXISTS + SINGLE modifier
    // =========================================================================

    @Test
    void single_exists_generates_singleExists_call() {
        var expr = new RExistenceExpr();
        expr.setArgument(symbolRef("price"));
        expr.setOp(ExistenceOp.EXISTS);
        expr.setModifier(ExistsModifier.SINGLE);

        assertEquals("singleExists(MapperS.of(price))", render(handler.handle(expr, ctx, compiler)));
    }

    // =========================================================================
    // EXISTS + MULTIPLE modifier
    // =========================================================================

    @Test
    void multiple_exists_generates_multipleExists_call() {
        var expr = new RExistenceExpr();
        expr.setArgument(symbolRef("trades"));
        expr.setOp(ExistenceOp.EXISTS);
        expr.setModifier(ExistsModifier.MULTIPLE);

        assertEquals("multipleExists(MapperS.of(trades))", render(handler.handle(expr, ctx, compiler)));
    }

    // =========================================================================
    // Only-exists
    // =========================================================================

    @Test
    void only_exists_single_element_generates_onlyExists_call() {
        var element = new ROnlyExistsElement();
        element.setRoot("trade");
        element.featureChain().add("price");

        var expr = new ROnlyExistsExpr();
        expr.elements().add(element);

        assertEquals(
            "onlyExists(trade, Arrays.asList(\"trade.price\"), Arrays.asList(\"trade.price\"))",
            render(handler.handle(expr, ctx, compiler)));
    }

    @Test
    void only_exists_item_root_generates_item_receiver() {
        var element = new ROnlyExistsElement();
        element.setRootIsItem(true);
        element.featureChain().add("quantity");

        var expr = new ROnlyExistsExpr();
        expr.elements().add(element);

        assertEquals(
            "onlyExists(item, Arrays.asList(\"item.quantity\"), Arrays.asList(\"item.quantity\"))",
            render(handler.handle(expr, ctx, compiler)));
    }

    // =========================================================================
    // Cardinality check — ONE_OF
    // =========================================================================

    @Test
    void one_of_generates_choice_required() {
        var expr = new RCardinalityCheckExpr();
        expr.setArgument(symbolRef("party"));
        expr.setOp(CardCheckOp.ONE_OF);
        expr.attributes().add("buyer");
        expr.attributes().add("seller");

        assertEquals(
            "choice(MapperS.of(party), Arrays.asList(\"buyer\", \"seller\"), ChoiceRuleValidationMethod.REQUIRED)",
            render(handler.handle(expr, ctx, compiler)));
    }

    // =========================================================================
    // Cardinality check — CHOICE + OPTIONAL
    // =========================================================================

    @Test
    void choice_optional_generates_choice_optional() {
        var expr = new RCardinalityCheckExpr();
        expr.setArgument(symbolRef("event"));
        expr.setOp(CardCheckOp.CHOICE);
        expr.setNecessity(Necessity.OPTIONAL);
        expr.attributes().add("attr1");
        expr.attributes().add("attr2");

        assertEquals(
            "choice(MapperS.of(event), Arrays.asList(\"attr1\", \"attr2\"), ChoiceRuleValidationMethod.OPTIONAL)",
            render(handler.handle(expr, ctx, compiler)));
    }

    // =========================================================================
    // Cardinality check — CHOICE + REQUIRED
    // =========================================================================

    @Test
    void choice_required_generates_choice_required() {
        var expr = new RCardinalityCheckExpr();
        expr.setArgument(intLiteral(1));
        expr.setOp(CardCheckOp.CHOICE);
        expr.setNecessity(Necessity.REQUIRED);
        expr.attributes().add("fieldA");

        assertEquals(
            "choice(MapperS.of(1), Arrays.asList(\"fieldA\"), ChoiceRuleValidationMethod.REQUIRED)",
            render(handler.handle(expr, ctx, compiler)));
    }

    // =========================================================================
    // Dispatch via ExpressionCompiler
    // =========================================================================

    @Test
    void compiler_dispatches_existence() {
        var expr = new RExistenceExpr();
        expr.setArgument(symbolRef("position"));
        expr.setOp(ExistenceOp.EXISTS);

        var scope  = ExpressionCompilerTest.createTestScope();
        var result = compiler.compile(expr, null, scope);

        assertInstanceOf(JavaExpression.class, result);
        assertEquals("exists(MapperS.of(position))", ((JavaExpression) result).renderToString());
    }

    @Test
    void compiler_dispatches_cardinality_check() {
        var expr = new RCardinalityCheckExpr();
        expr.setArgument(symbolRef("contract"));
        expr.setOp(CardCheckOp.ONE_OF);
        expr.attributes().add("x");

        var scope  = ExpressionCompilerTest.createTestScope();
        var result = compiler.compile(expr, null, scope);

        assertInstanceOf(JavaExpression.class, result);
        assertEquals(
            "choice(MapperS.of(contract), Arrays.asList(\"x\"), ChoiceRuleValidationMethod.REQUIRED)",
            ((JavaExpression) result).renderToString());
    }

    // =========================================================================
    // PR-A §9.1 C3a.4.d — refs-carrying invariant tests (D6 ε + D9 λ)
    // =========================================================================

    @Test
    void existence_exists_emission_carries_expression_operators_null_safe_wildcard() {
        var expr = new RExistenceExpr();
        expr.setArgument(symbolRef("trade"));
        expr.setOp(ExistenceOp.EXISTS);

        var result = handler.handle(expr, ctx, compiler);

        Set<?> wildcards = result.getStaticWildcardImports();
        assertTrue(wildcards.contains(HandlerHelper.EXPRESSION_OPERATORS_NULL_SAFE),
                "L78 exists emission must carry EXPRESSION_OPERATORS_NULL_SAFE "
                        + "staticWildcardImport (exists/notExists/singleExists/"
                        + "multipleExists are static methods on that class)");
    }

    @Test
    void only_exists_emission_carries_arrays_ref_and_wildcard() {
        var element = new ROnlyExistsElement();
        element.setRoot("trade");
        element.featureChain().add("price");

        var expr = new ROnlyExistsExpr();
        expr.elements().add(element);

        var result = handler.handle(expr, ctx, compiler);

        Set<?> refs = result.getRefs();
        assertTrue(refs.contains(HandlerHelper.ARRAYS),
                "L124 onlyExists emission textually contains Arrays.asList — "
                        + "must carry ARRAYS ref");

        Set<?> wildcards = result.getStaticWildcardImports();
        assertTrue(wildcards.contains(HandlerHelper.EXPRESSION_OPERATORS_NULL_SAFE),
                "L124 onlyExists is a static method on ExpressionOperatorsNullSafe — "
                        + "must carry EXPRESSION_OPERATORS_NULL_SAFE wildcard");
    }

    @Test
    void choice_emission_carries_arrays_choice_rule_validation_method_and_wildcard() {
        var expr = new RCardinalityCheckExpr();
        expr.setArgument(symbolRef("contract"));
        expr.setOp(CardCheckOp.ONE_OF);
        expr.attributes().add("x");

        var result = handler.handle(expr, ctx, compiler);

        Set<?> refs = result.getRefs();
        assertTrue(refs.contains(HandlerHelper.ARRAYS),
                "L184 choice emission textually contains Arrays.asList — "
                        + "must carry ARRAYS ref");
        assertTrue(refs.contains(HandlerHelper.CHOICE_RULE_VALIDATION_METHOD),
                "L184 choice emission emits ChoiceRuleValidationMethod.REQUIRED/"
                        + "OPTIONAL inline — must carry CHOICE_RULE_VALIDATION_METHOD "
                        + "ref (D6 ε)");

        Set<?> wildcards = result.getStaticWildcardImports();
        assertTrue(wildcards.contains(HandlerHelper.EXPRESSION_OPERATORS_NULL_SAFE),
                "L184 choice() is a public static method on "
                        + "ExpressionOperatorsNullSafe (L477/L480 per 2026-04-21 "
                        + "source verification). D9 λ Case (a): must carry the "
                        + "wildcard structurally, fixing the latent import bug "
                        + "where FunctionGenerator L844-849 trigger list doesn't "
                        + "include `choice(`");
    }
}
