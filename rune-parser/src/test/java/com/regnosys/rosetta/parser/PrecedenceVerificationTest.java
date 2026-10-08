package com.regnosys.rosetta.parser;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies operator precedence is correct in the expression rule.
 * ANTLR4 left-recursive rules: alternatives listed FIRST = HIGHEST precedence (tightest binding).
 *
 * We verify by parsing expressions and checking which alternative label
 * is at the outermost level of the expression tree.
 */
class PrecedenceVerificationTest {

    private RosettaParser.ExpressionContext parseExpr(String expr) {
        String source = "namespace test.precedence\nfunc F:\n    inputs: x number (1..1)\n    output: r number (1..1)\n    set r: " + expr;
        RosettaParseResult result = RosettaParserFacade.parseString(source);
        assertTrue(result.errors().isEmpty(), "Parse errors: " + result.errors());

        // Navigate: rosettaModel -> rootElement -> function -> operation -> expressionWithAsKey -> expression
        RosettaParser.RosettaModelContext model = (RosettaParser.RosettaModelContext) result.tree();
        RosettaParser.RootElementContext root = model.rootElement(0);
        RosettaParser.FunctionContext func = root.function();
        assertNotNull(func, "Expected function in parse tree");
        RosettaParser.OperationContext op = func.operation(0);
        assertNotNull(op, "Expected operation in function");
        RosettaParser.ExpressionWithAsKeyContext ewak = op.expressionWithAsKey();
        assertNotNull(ewak, "Expected expressionWithAsKey in operation");
        // exprWithThen is the then-chain wrapper; .expression() unwraps to the
        // then-free expression whose runtime type is the labelled operator alt.
        return ewak.exprWithThen().expression();
    }

    @Test
    void multiplicationBindsTighterThanAddition() {
        // 1 + 2 * 3 should have + at the top, * nested inside
        RosettaParser.ExpressionContext expr = parseExpr("1 + 2 * 3");
        assertInstanceOf(RosettaParser.AdditiveExprContext.class, expr,
            "Outer expression should be additive (+), with multiplicative (*) nested inside");
    }

    @Test
    void additionBindsTighterThanComparison() {
        // x + 1 > 0 should have > at the top, + nested inside
        RosettaParser.ExpressionContext expr = parseExpr("x + 1 > 0");
        assertInstanceOf(RosettaParser.ComparisonExprContext.class, expr,
            "Outer expression should be comparison (>), with additive (+) nested inside");
    }

    @Test
    void comparisonBindsTighterThanLogicalAnd() {
        // x > 0 and x < 10 should have 'and' at the top
        RosettaParser.ExpressionContext expr = parseExpr("x > 0 and x < 10");
        assertInstanceOf(RosettaParser.AndExprContext.class, expr,
            "Outer expression should be 'and', with comparisons nested inside");
    }

    @Test
    void logicalAndBindsTighterThanOr() {
        // a and b or c should have 'or' at the top
        RosettaParser.ExpressionContext expr = parseExpr("x = 1 and x = 2 or x = 3");
        assertInstanceOf(RosettaParser.OrExprContext.class, expr,
            "Outer expression should be 'or', with 'and' nested inside");
    }

    @Test
    void existsBindsTighterThanOr() {
        // Phase X1 Gap #6: a postfix operator (`exists`) binds TIGHTER than the
        // boolean `or`, so `a = b or c exists` parses as `(a = b) or (c exists)`
        // — the OrExpr is outermost, with ExistsExpr bound to the right operand.
        // Before the grammar fix (postfix alternatives listed after the binary
        // ops) this mis-parsed as `((a = b) or c) exists`, producing
        // `exists(areEqual(...).orNullSafe(...))` instead of
        // `areEqual(...).orNullSafe(exists(...))`.
        RosettaParser.ExpressionContext expr = parseExpr("x = 1 or x exists");
        assertInstanceOf(RosettaParser.OrExprContext.class, expr,
            "Outer expression should be 'or', with 'exists' bound to the right operand "
                + "(postfix binds tighter than boolean or)");
    }
}
