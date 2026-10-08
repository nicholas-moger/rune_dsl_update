package com.regnosys.rosetta.generator.java.function;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.enums.ArithOp;
import com.regnosys.rosetta.ast.enums.OperationOp;
import com.regnosys.rosetta.ast.expressions.binary.RArithmeticExpr;
import com.regnosys.rosetta.ast.expressions.binary.RThenExpr;
import com.regnosys.rosetta.ast.expressions.constructors.RConditionalExpr;
import com.regnosys.rosetta.ast.expressions.literals.RBooleanLiteral;
import com.regnosys.rosetta.ast.expressions.literals.RIntLiteral;
import com.regnosys.rosetta.ast.expressions.literals.RListLiteral;
import com.regnosys.rosetta.ast.expressions.references.REnumValueRef;
import com.regnosys.rosetta.ast.expressions.references.RSymbolReference;
import com.regnosys.rosetta.ast.expressions.supporting.RInlineFunction;
import com.regnosys.rosetta.ast.expressions.unary.RExtractExpr;
import com.regnosys.rosetta.ast.expressions.unary.RFilterExpr;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.functions.ROperation;
import com.regnosys.rosetta.ast.functions.RRule;
import com.regnosys.rosetta.ast.functions.RSegment;
import com.regnosys.rosetta.ast.functions.RShortcut;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.expression.ExpressionCompiler;
import com.regnosys.rosetta.generator.java.expression.handlers.CollectionHandler;
import com.regnosys.rosetta.generator.java.expression.handlers.HandlerHelper;
import com.regnosys.rosetta.generator.java.statement.builder.JavaExpression;
import com.regnosys.rosetta.generator.java.statement.builder.JavaStatementBuilder;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.symbols.RWorkspace;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link FunctionExpressionRenderer}.
 *
 * <p>All tests use the real {@link ExpressionCompiler} (zero-arg constructor)
 * and hand-built in-memory AST nodes — no parser, no file I/O.
 *
 * <p>Test matrix:
 * <ul>
 *   <li>renderExpression: int literal → {@code MapperS.of(42)}</li>
 *   <li>renderExpression: arithmetic expression → {@code MapperMaths.&lt;Integer, Integer, Integer&gt;add(...)} (int-literal join; facet numeric_literal_typing)</li>
 *   <li>renderExpression: conditional expression (ternary) — compound builder
 *       collapses correctly</li>
 *   <li>renderOperation: delegates to expression and returns non-null string</li>
 *   <li>renderAlias: delegates to shortcut expression and returns non-null string</li>
 *   <li>renderCondition: delegates to expression and returns non-null string</li>
 *   <li>JavaExpression fast path: direct renderToString without collapsing</li>
 *   <li>Compound result (non-JavaExpression) handled via collapseToSingleExpression</li>
 * </ul>
 */
class FunctionExpressionRendererTest {

    private FunctionExpressionRenderer renderer;

    @BeforeEach
    void setUp() {
        renderer = new FunctionExpressionRenderer(new ExpressionCompiler());
    }

    // =========================================================================
    // Factory helpers
    // =========================================================================

    private static RIntLiteral intLiteral(int value) {
        RIntLiteral lit = new RIntLiteral();
        lit.setValue(value);
        return lit;
    }

    private static RBooleanLiteral boolLiteral(boolean value) {
        RBooleanLiteral lit = new RBooleanLiteral();
        lit.setValue(value);
        return lit;
    }

    private static RArithmeticExpr arithmetic(RExpression left, ArithOp op, RExpression right) {
        RArithmeticExpr expr = new RArithmeticExpr();
        expr.setLeft(left);
        expr.setOp(op);
        expr.setRight(right);
        return expr;
    }

    private static RConditionalExpr conditional(RExpression condition,
                                                 RExpression thenBranch,
                                                 RExpression elseBranch) {
        RConditionalExpr expr = new RConditionalExpr();
        expr.setCondition(condition);
        expr.setThenBranch(thenBranch);
        expr.setElseBranch(elseBranch);
        return expr;
    }

    private static ROperation operation(RExpression expression) {
        return operation("result", OperationOp.SET, expression);
    }

    private static ROperation operation(String targetName, OperationOp operator, RExpression expression) {
        ROperation op = new ROperation();
        op.setTargetName(targetName);
        op.setOperator(operator);
        op.setExpression(expression);
        return op;
    }

    private static RSymbolReference symbolRef(String name) {
        RSymbolReference ref = new RSymbolReference();
        ref.setName(name);
        return ref;
    }

    /**
     * Build a fully-resolved enum-value reference that renders as the bare
     * dotted enum constant {@code EnumType.VALUE} (the ReferenceHandler
     * enumeration-present path). Mirrors {@code ReferenceHandlerTest.resolvedEnumRef}.
     */
    private static REnumValueRef resolvedEnumRef(String enumName, String valueName) {
        REnumValueRef ref = new REnumValueRef();
        ref.setEnumName(enumName);
        ref.setValueName(valueName);
        var mockEnum = new com.regnosys.rosetta.ast.types.REnumeration();
        mockEnum.setName(enumName);
        ref.setResolvedEnum(mockEnum);
        return ref;
    }

    private static RShortcut shortcut(RExpression expression) {
        RShortcut sc = new RShortcut();
        sc.setName("testAlias");
        sc.setExpression(expression);
        return sc;
    }

    /**
     * A filter expression {@code argName filter [item -> predName]}. The
     * {@code CollectionHandler} renders this with a chain-link line break
     * (Engine PR #5): {@code MapperS.of(argName)\n\t.filterSingleNullSafe(item ->
     * MapperS.of(predName))}. Used to exercise the re-indent of a wrapped list-op
     * value at the operation-assembly sites.
     */
    private static RFilterExpr filterOf(String argName, String predName) {
        RFilterExpr f = new RFilterExpr();
        f.setArgument(symbolRef(argName));
        RInlineFunction body = new RInlineFunction();
        body.setImplicit(true);
        body.setBody(symbolRef(predName));
        f.setBody(body);
        return f;
    }

    // =========================================================================
    // renderExpression: simple int literal
    // =========================================================================

    @Test
    void renderExpression_intLiteral_returns_MapperS_of_42() {
        String result = renderer.renderExpression(intLiteral(42)).source();
        assertEquals("MapperS.of(42)", result);
    }

    @Test
    void renderExpression_intLiteral_zero() {
        String result = renderer.renderExpression(intLiteral(0)).source();
        assertEquals("MapperS.of(0)", result);
    }

    // =========================================================================
    // renderExpression: arithmetic expression
    // =========================================================================

    @Test
    void renderExpression_arithmetic_add_returns_MapperMaths_add() {
        // facet numeric_literal_typing: int literals count as join evidence -> Integer witnesses, bare literals
        RArithmeticExpr expr = arithmetic(intLiteral(1), ArithOp.PLUS, intLiteral(2));
        String result = renderer.renderExpression(expr).source();
        assertEquals("MapperMaths.<Integer, Integer, Integer>add(MapperS.of(1), MapperS.of(2))", result);
    }

    @Test
    void renderExpression_arithmetic_subtract() {
        RArithmeticExpr expr = arithmetic(intLiteral(10), ArithOp.MINUS, intLiteral(3));
        String result = renderer.renderExpression(expr).source();
        assertEquals("MapperMaths.<Integer, Integer, Integer>subtract(MapperS.of(10), MapperS.of(3))", result);
    }

    @Test
    void renderExpression_arithmetic_multiply() {
        RArithmeticExpr expr = arithmetic(intLiteral(4), ArithOp.MULTIPLY, intLiteral(5));
        String result = renderer.renderExpression(expr).source();
        assertEquals("MapperMaths.<Integer, Integer, Integer>multiply(MapperS.of(4), MapperS.of(5))", result);
    }

    @Test
    void renderExpression_arithmetic_divide() {
        RArithmeticExpr expr = arithmetic(intLiteral(6), ArithOp.DIVIDE, intLiteral(2));
        String result = renderer.renderExpression(expr).source();
        assertEquals("MapperMaths.<BigDecimal, Integer, Integer>divide(MapperS.of(6), MapperS.of(2))", result);
    }

    // =========================================================================
    // renderExpression: conditional expression (ternary)
    // =========================================================================

    @Test
    void renderExpression_conditional_returns_ternary_string() {
        // if true then MapperS.of(1) else MapperS.of(2)
        RConditionalExpr expr = conditional(boolLiteral(true), intLiteral(1), intLiteral(2));
        String result = renderer.renderExpression(expr).source();
        // The ControlFlowHandler renders the DECLINED-hoist ternary here (null expected
        // type + no statement-hoist sink on this unit compile — facet
        // ifthenelse_result_hoisting hoists otherwise):
        // condition.getOrDefault(false) ? then : else
        assertEquals(
                "MapperS.of(true).getOrDefault(false) ? MapperS.of(1) : MapperS.of(2)",
                result);
    }

    @Test
    void renderExpression_conditional_without_else_uses_null() {
        // if true then MapperS.of(42) (no else branch)
        RConditionalExpr expr = new RConditionalExpr();
        expr.setCondition(boolLiteral(true));
        expr.setThenBranch(intLiteral(42));
        // elseBranch left null
        String result = renderer.renderExpression(expr).source();
        assertEquals(
                "MapperS.of(true).getOrDefault(false) ? MapperS.of(42) : null",
                result);
    }

    // =========================================================================
    // renderOperation: SET non-conditional — wraps in assignment
    // =========================================================================

    @Test
    void renderOperation_setIntLiteral_wrapsInAssignment() {
        // SET with int literal: MapperS.of(7) → unwrap to raw value
        ROperation op = operation(intLiteral(7));
        String result = renderer.renderOperation(op).source();
        assertEquals("result = 7;", result);
    }

    @Test
    void renderOperation_setArithmetic_wrapsInAssignmentWithGet() {
        // SET with arithmetic: MapperMaths.add(...) → add .get()
        ROperation op = operation(arithmetic(intLiteral(3), ArithOp.PLUS, intLiteral(4)));
        String result = renderer.renderOperation(op).source();
        assertEquals("result = MapperMaths.<Integer, Integer, Integer>add(MapperS.of(3), MapperS.of(4)).get();", result);
    }

    @Test
    void renderOperation_setSymbolRef_unwrapsMapperS() {
        // SET with symbol reference (MapperS.of(a)) → unwrap to bare "a"
        ROperation op = operation("result", OperationOp.SET, symbolRef("a"));
        String result = renderer.renderOperation(op).source();
        assertEquals("result = a;", result);
    }

    @Test
    void renderOperation_setEnumConstant_noSpuriousGet() {
        // Phase X1 Gap #4: a bare enum constant has no .get() — enum constants
        // are not Mappers. golden: output = DeliveryTypeEnum.CASH;
        // (Mirrors unwrapForEvaluateArg's dotted-enum branch; unwrapForAssignment
        // previously fell through to the .get() suffix → invalid Java.)
        ROperation op = operation("output", OperationOp.SET,
                resolvedEnumRef("DeliveryTypeEnum", "CASH"));
        String result = renderer.renderOperation(op).source();
        assertEquals("output = DeliveryTypeEnum.CASH;", result);
    }

    // =========================================================================
    // renderOperation: SET conditional — renders if/else block
    // =========================================================================

    @Test
    void renderOperation_setConditional_rendersIfElseBlock() {
        // if true then 1 else 2 → if/else block with unwrapped branches
        RConditionalExpr cond = conditional(boolLiteral(true), intLiteral(1), intLiteral(2));
        ROperation op = operation("result", OperationOp.SET, cond);
        String result = renderer.renderOperation(op).source();

        // Verify structure: if (...) { result = 1; } else { result = 2; }
        // facet boolLiteralLadderCondition (PR #372, F-delta-4): a LITERAL boolean
        // condition renders BARE (golden drr GetIndexIndicatorFromFloatingRate) —
        // the pin moved with the facet (was `if (MapperS.of(true).getOrDefault(false))`).
        assertTrue(result.startsWith("if (true) {"),
                "Should start with if block, got: " + result);
        assertTrue(result.contains("result = 1;"),
                "Then branch should assign unwrapped value");
        assertTrue(result.contains("result = 2;"),
                "Else branch should assign unwrapped value");
        assertTrue(result.contains("} else {"),
                "Should have else clause");
    }

    @Test
    void renderOperation_setConditional_withSymbolRefs_unwrapsBranches() {
        // if cond then a else b → result = a / result = b (raw names)
        RConditionalExpr cond = conditional(boolLiteral(true), symbolRef("a"), symbolRef("b"));
        ROperation op = operation("result", OperationOp.SET, cond);
        String result = renderer.renderOperation(op).source();

        assertTrue(result.contains("result = a;"),
                "Then branch should unwrap MapperS.of(a) to bare 'a'");
        assertTrue(result.contains("result = b;"),
                "Else branch should unwrap MapperS.of(b) to bare 'b'");
    }

    @Test
    void renderOperation_setConditional_withArithmeticBranch_addsGet() {
        // if cond then (3+4) else a → result = MapperMaths.add(...).get() / result = a
        RConditionalExpr cond = conditional(
                boolLiteral(true),
                arithmetic(intLiteral(3), ArithOp.MULTIPLY, intLiteral(4)),
                symbolRef("a"));
        ROperation op = operation("result", OperationOp.SET, cond);
        String result = renderer.renderOperation(op).source();

        assertTrue(result.contains("result = MapperMaths.<Integer, Integer, Integer>multiply(MapperS.of(3), MapperS.of(4)).get();"),
                "Arithmetic branch should add .get(), got: " + result);
        assertTrue(result.contains("result = a;"),
                "Symbol ref branch should unwrap to bare name");
    }

    @Test
    void renderOperation_setConditional_withoutElse_assignsNull() {
        RConditionalExpr cond = new RConditionalExpr();
        cond.setCondition(boolLiteral(true));
        cond.setThenBranch(intLiteral(42));
        // No else branch
        ROperation op = operation("result", OperationOp.SET, cond);
        String result = renderer.renderOperation(op).source();

        assertTrue(result.contains("result = 42;"), "Then branch should assign 42");
        assertTrue(result.contains("result = null;"), "Missing else should assign null");
    }

    @Test
    void renderOperation_setNestedConditional_rendersElseIfChain() {
        // Phase X1 Gap #5: `if c1 then a else (if c2 then b else c)` renders as
        // an if / else if / else chain — NOT a nested ternary. The else-branch
        // being itself an RConditionalExpr triggers the recursion in
        // renderConditionalAssignment.
        RConditionalExpr inner = conditional(boolLiteral(false), intLiteral(2), intLiteral(3));
        RConditionalExpr outer = conditional(boolLiteral(true), intLiteral(1), inner);
        ROperation op = operation("result", OperationOp.SET, outer);
        String result = renderer.renderOperation(op).source();

        // facet boolLiteralLadderCondition (PR #372, F-delta-4): literal conditions
        // render BARE at every rung — the pins moved with the facet.
        assertTrue(result.startsWith("if (true) {"),
                "Outer if, got: " + result);
        assertTrue(result.contains("} else if (false) {"),
                "Nested conditional else-branch must render as `else if`; got: " + result);
        assertTrue(result.contains("result = 1;"), "outer then assigns 1");
        assertTrue(result.contains("result = 2;"), "else-if then assigns 2");
        assertTrue(result.contains("result = 3;"), "final else assigns 3");
        assertFalse(result.contains("?"),
                "if/else-if chain must not emit a ternary operator; got: " + result);
    }

    @Test
    void renderOperation_setNestedConditional_withoutInnerElse_finalElseNull() {
        // `if c1 then a else (if c2 then b)` — innermost has no else → final
        // `} else {` assigns null (matching the DeliveryTypeFromSettlement golden,
        // whose source conditional chain ends without an explicit else).
        RConditionalExpr inner = new RConditionalExpr();
        inner.setCondition(boolLiteral(false));
        inner.setThenBranch(intLiteral(2));
        RConditionalExpr outer = conditional(boolLiteral(true), intLiteral(1), inner);
        ROperation op = operation("result", OperationOp.SET, outer);
        String result = renderer.renderOperation(op).source();

        // facet boolLiteralLadderCondition (PR #372, F-delta-4): the bare literal rung.
        assertTrue(result.contains("} else if (false) {"),
                "Nested else must render as `else if`; got: " + result);
        assertTrue(result.contains("} else {"), "innermost missing else → terminal else block");
        assertTrue(result.contains("result = null;"), "terminal else assigns null");
        assertFalse(result.contains("?"), "no ternary; got: " + result);
    }

    @Test
    void renderOperation_setConditional_emptyListLiteralElse_rendersNull() {
        // Phase X1 Gap #5: DefaultElseRule synthesises an empty list literal as
        // the else of an `if/then` with no explicit else. In this
        // single-cardinality conditional-SET path it must coerce to `null`
        // (matching the upstream golden), NOT compile to `MapperC.of().get()`.
        RConditionalExpr cond = new RConditionalExpr();
        cond.setCondition(boolLiteral(true));
        cond.setThenBranch(intLiteral(1));
        cond.setElseBranch(new RListLiteral()); // empty list — synthetic default else
        ROperation op = operation("result", OperationOp.SET, cond);
        String result = renderer.renderOperation(op).source();

        assertTrue(result.contains("result = null;"),
                "empty-list-literal else must coerce to null; got: " + result);
        assertFalse(result.contains("MapperC.of().get()"),
                "empty-list-literal else must not emit MapperC.of().get(); got: " + result);
    }

    // =========================================================================
    // renderOperation: ADD — wraps in addAll
    // =========================================================================

    @Test
    void renderOperation_addIntLiteral_wrapsInAddAll() {
        // ADD with int literal: MapperS.of(7) → unwrap
        ROperation op = operation("result", OperationOp.ADD, intLiteral(7));
        String result = renderer.renderOperation(op).source();
        assertEquals("result.addAll(7);", result);
    }

    @Test
    void renderOperation_addSymbolRef_unwrapsAndAddAll() {
        // ADD with symbol ref: MapperS.of(list) → addAll(list)
        ROperation op = operation("result", OperationOp.ADD, symbolRef("items"));
        String result = renderer.renderOperation(op).source();
        assertEquals("result.addAll(items);", result);
    }

    @Test
    void renderOperation_addArithmetic_getsMulti() {
        // ADD with arithmetic expression: MapperMaths.add(...) → addAll(...getMulti())
        ROperation op = operation("result", OperationOp.ADD,
                arithmetic(intLiteral(1), ArithOp.PLUS, intLiteral(2)));
        String result = renderer.renderOperation(op).source();
        assertEquals("result.addAll(MapperMaths.<Integer, Integer, Integer>add(MapperS.of(1), MapperS.of(2)).getMulti());", result);
    }

    // =========================================================================
    // Engine PR #5: chain-link re-indent at sibling operation-assembly sites
    // =========================================================================

    @Test
    void renderOperation_addFilter_reindentsChainLinkToBase() {
        // Engine PR #5: a wrapped list-op value (CollectionHandler emits the
        // CHAIN_LINK `\n\t.` relative prefix) in the ADD assembly site is
        // re-indented to the statement base (DEFAULT_INDENT_LEVEL = 3), so the
        // continuation lands at indentLevel+1 = 4 tabs — the raw single relative
        // tab must NOT leak through.
        ROperation op = operation("result", OperationOp.ADD, filterOf("items", "predicate"));
        String result = renderer.renderOperation(op).source();
        assertTrue(result.contains(".filterSingleNullSafe(item -> MapperS.of(predicate))"),
                "filter must render (wrapped) inside the addAll(...); got: " + result);
        assertTrue(result.contains("\n\t\t\t\t.filterSingleNullSafe"),
                "chain-link must be re-indented to indentLevel+1 (4 tabs) at the ADD site; got: " + result);
        assertFalse(result.contains("\n\t.filterSingleNullSafe"),
                "raw single relative tab must NOT leak into the ADD assembly site; got: " + result);
    }

    @Test
    void renderOperation_setConditionalWithFilterBranch_reindentsChainLink() {
        // Engine PR #5: the conditional-SET branches splice the value at
        // bodyIndent = indentLevel+1, so a wrapped list-op in a branch is
        // re-indented to indentLevel+1 — the raw single relative tab must NOT
        // leak through. (Segment-SET shares the same reindentContinuation helper
        // and is exercised by the green D11 drr run.)
        RConditionalExpr cond = conditional(boolLiteral(true),
                filterOf("items", "predicate"), intLiteral(0));
        ROperation op = operation("result", OperationOp.SET, cond);
        String result = renderer.renderOperation(op).source();
        assertTrue(result.contains(".filterSingleNullSafe(item -> MapperS.of(predicate))"),
                "filter must render (wrapped) in the then-branch; got: " + result);
        assertTrue(result.contains("\n\t\t\t\t\t.filterSingleNullSafe"),
                "conditional-SET branch chain-link must be re-indented to 5 tabs "
                + "(branch body at indentLevel+1=4, +1 relative); got: " + result);
        assertFalse(result.contains("\n\t.filterSingleNullSafe"),
                "raw single relative tab must NOT leak into the conditional-SET branch; got: " + result);
    }

    @Test
    void renderOperation_segmentSetWithFilter_reindentsChainLink() {
        // Engine PR #5 (Copilot R3 finding): the segment-targeted SET path
        // (renderSetBuilderChain) splices the value into `.set<Name>(...)` at
        // indentLevel+1, so a wrapped list-op value re-indents to indentLevel+2.
        ROperation op = operationWithSegment("X", segments("a"), filterOf("items", "predicate"));
        String result = renderer.renderOperation(op).source();
        String cont2 = "\n" + "\t".repeat(FunctionExpressionRenderer.DEFAULT_INDENT_LEVEL + 2);
        assertTrue(result.contains(".filterSingleNullSafe(item -> MapperS.of(predicate))"),
                "filter must render (wrapped) inside the .setA(...); got: " + result);
        assertTrue(result.contains(cont2 + ".filterSingleNullSafe"),
                "segment-SET chain-link must re-indent to indentLevel+2; got: " + result);
        assertFalse(result.contains("\n\t.filterSingleNullSafe"),
                "raw single relative tab must NOT leak into the segment-SET value; got: " + result);
    }

    @Test
    void renderOperation_setConditionalElseFilter_reindentsChainLink() {
        // Engine PR #5 (Copilot R3 finding): the conditional-SET ELSE branch is a
        // separate compile/unwrap path from the then-branch; lock its re-indent
        // independently (then-branch is a single-line literal here).
        RConditionalExpr cond = conditional(boolLiteral(true), intLiteral(0),
                filterOf("items", "predicate"));
        ROperation op = operation("result", OperationOp.SET, cond);
        String result = renderer.renderOperation(op).source();
        String cont2 = "\n" + "\t".repeat(FunctionExpressionRenderer.DEFAULT_INDENT_LEVEL + 2);
        assertTrue(result.contains("} else {"),
                "must render an if/else block; got: " + result);
        assertTrue(result.contains(".filterSingleNullSafe(item -> MapperS.of(predicate))"),
                "filter must render (wrapped) in the else-branch; got: " + result);
        assertTrue(result.contains(cont2 + ".filterSingleNullSafe"),
                "else-branch chain-link must re-indent to indentLevel+2; got: " + result);
        assertFalse(result.contains("\n\t.filterSingleNullSafe"),
                "raw single relative tab must NOT leak into the else-branch; got: " + result);
    }

    // =========================================================================
    // renderAlias: delegates to shortcut.expression()
    // =========================================================================

    @Test
    void renderAlias_intLiteral_returns_MapperS_of() {
        RShortcut sc = shortcut(intLiteral(99));
        String result = renderer.renderAlias(sc).source();
        assertEquals("MapperS.of(99)", result);
    }

    @Test
    void renderAlias_arithmetic_returns_MapperMaths() {
        RShortcut sc = shortcut(arithmetic(intLiteral(2), ArithOp.MULTIPLY, intLiteral(3)));
        String result = renderer.renderAlias(sc).source();
        assertEquals("MapperMaths.<Integer, Integer, Integer>multiply(MapperS.of(2), MapperS.of(3))", result);
    }

    // =========================================================================
    // renderCondition: delegates to expression
    // =========================================================================

    @Test
    void renderCondition_boolLiteral_returns_MapperS_of_true() {
        String result = renderer.renderCondition(boolLiteral(true)).source();
        assertEquals("MapperS.of(true)", result);
    }

    @Test
    void renderCondition_intLiteral_returns_MapperS_of() {
        String result = renderer.renderCondition(intLiteral(5)).source();
        assertEquals("MapperS.of(5)", result);
    }

    @Test
    void renderCondition_arithmetic_returns_raw_expression_without_wrapping() {
        // Condition rendering should return the raw inner expression,
        // not wrapped in conditionValidator.validate(). The template does the wrapping.
        RArithmeticExpr expr = arithmetic(intLiteral(3), ArithOp.PLUS, intLiteral(4));
        String result = renderer.renderCondition(expr).source();
        assertEquals("MapperMaths.<Integer, Integer, Integer>add(MapperS.of(3), MapperS.of(4))", result);
        // Verify NO wrapping — the expression should NOT contain conditionValidator
        assertFalse(result.contains("conditionValidator"),
                "renderCondition should NOT wrap in conditionValidator — the template handles that");
    }

    @Test
    void renderCondition_symbolRef_returns_raw_MapperS() {
        // For a symbol reference used as a condition, renderCondition should return
        // MapperS.of(symbolName) — the template wraps it
        String result = renderer.renderCondition(symbolRef("isValid")).source();
        assertEquals("MapperS.of(isValid)", result);
    }

    // =========================================================================
    // Rendering strategy: fast path vs compound path
    // =========================================================================

    /**
     * Verify the fast path: a simple int literal compiles to a {@link JavaExpression}
     * and is rendered without calling {@code collapseToSingleExpression}.
     */
    @Test
    void fast_path_JavaExpression_renders_without_collapse() {
        // Use the compiler directly to confirm it returns a JavaExpression
        var compiler = new ExpressionCompiler();
        var scope = new com.regnosys.rosetta.generator.java.scoping.JavaStatementScope("test", null);
        JavaStatementBuilder compiled = compiler.compile(intLiteral(42), null, scope);
        assertInstanceOf(JavaExpression.class, compiled,
                "RIntLiteral should compile to JavaExpression (fast path)");

        // Renderer should return the same string
        assertEquals("MapperS.of(42)", renderer.renderExpression(intLiteral(42)).source());
    }

    /**
     * Verify the compound path: a conditional expression at M7b-1 compiles to a
     * {@link JavaExpression} (ternary), which also takes the fast path via the
     * {@code instanceof JavaExpression} check.
     */
    @Test
    void conditional_renders_to_non_null_string() {
        RConditionalExpr expr = conditional(boolLiteral(false), intLiteral(10), intLiteral(20));
        String result = renderer.renderExpression(expr).source();
        assertNotNull(result);
        assertFalse(result.isEmpty());
        // At M7b-1, ControlFlowHandler returns a JavaExpression (ternary string)
        assertTrue(result.contains("MapperS.of(10)"),
                "Then-branch should be present in rendered output");
        assertTrue(result.contains("MapperS.of(20)"),
                "Else-branch should be present in rendered output");
    }

    /**
     * Verify all four render methods return non-null, non-empty strings for the
     * same underlying expression.
     */
    @Test
    void all_render_methods_return_non_null_non_empty() {
        RExpression expr = intLiteral(42);

        assertNotNull(renderer.renderExpression(expr).source());
        assertFalse(renderer.renderExpression(expr).source().isEmpty());

        assertNotNull(renderer.renderCondition(expr).source());
        assertFalse(renderer.renderCondition(expr).source().isEmpty());

        assertNotNull(renderer.renderOperation(operation(intLiteral(42))).source());
        assertFalse(renderer.renderOperation(operation(intLiteral(42))).source().isEmpty());

        assertNotNull(renderer.renderAlias(shortcut(intLiteral(42))).source());
        assertFalse(renderer.renderAlias(shortcut(intLiteral(42))).source().isEmpty());
    }

    // =========================================================================
    // renderOperation: ADD with toBuilder wrapping (Rosetta model types)
    // =========================================================================

    @Test
    void renderOperation_addWithToBuilder_wrapsInToBuilder() {
        // ADD with symbol ref + outputNeedsBuilder: addAll(toBuilder(items))
        ROperation op = operation("result", OperationOp.ADD, symbolRef("items"));
        String result = renderer.renderOperation(op, FunctionExpressionRenderer.DEFAULT_INDENT_LEVEL, true).source();
        assertEquals("result.addAll(toBuilder(items));", result);
    }

    @Test
    void renderOperation_addWithToBuilder_arithmetic_wrapsGetMultiInToBuilder() {
        // ADD with arithmetic + outputNeedsBuilder: addAll(toBuilder(expr.getMulti()))
        ROperation op = operation("result", OperationOp.ADD,
                arithmetic(intLiteral(1), ArithOp.PLUS, intLiteral(2)));
        String result = renderer.renderOperation(op, FunctionExpressionRenderer.DEFAULT_INDENT_LEVEL, true).source();
        assertEquals("result.addAll(toBuilder(MapperMaths.<Integer, Integer, Integer>add(MapperS.of(1), MapperS.of(2)).getMulti()));", result);
    }

    @Test
    void renderOperation_addWithoutToBuilder_noWrapping() {
        // ADD without outputNeedsBuilder (basic types): no toBuilder
        ROperation op = operation("result", OperationOp.ADD, symbolRef("items"));
        String result = renderer.renderOperation(op, FunctionExpressionRenderer.DEFAULT_INDENT_LEVEL, false).source();
        assertEquals("result.addAll(items);", result);
    }

    // =========================================================================
    // unwrapForAssignment: direct unit tests (PR-A v6.2 C3a.3 — builder-level)
    // =========================================================================
    //
    // Signature change: (String) → (JavaStatementBuilder). Each test pins one
    // branch of the dispatch ladder:
    //   1. null builder → synthetic "null" with empty refs
    //   2. structural unwrap (wrappedInMapperSOf → unwrapToBuilder)
    //   3. "null"-source passthrough (same inbound instance)
    //   4. legacy MapperS.of string-scan with atomic MAPPER_S drop
    //      (TRANSITIONAL until C3a.4 migrates LiteralHandler + coercers)
    //   5. fall-through .get() with refs preserved verbatim
    //
    // Promoted from static to instance method — the helper uses
    // renderToString(builder) which is an instance method.

    @Test
    void unwrapForAssignment_structural_strips_wrapper_and_drops_mapperS_ref() {
        // wrappedInMapperSOf(inner) → unwrap returns inner; MAPPER_S drops atomically.
        JavaExpression inner = JavaExpression.from("a", null, Set.of());
        JavaExpression wrapped = JavaExpression.wrappedInMapperSOf(inner);
        JavaStatementBuilder unwrapped = renderer.unwrapForAssignment(wrapped);
        assertEquals("a", ((JavaExpression) unwrapped).renderToString());
        assertEquals(Set.of(), unwrapped.getRefs(),
                "structural unwrap must drop MAPPER_S atomically with source strip");
    }

    @Test
    void unwrapForAssignment_legacy_string_scan_strips_mapperS_of_wrapper() {
        // Raw JavaExpression.from("MapperS.of(...)", ...) with MAPPER_S in refs
        // (legacy emitters — LiteralHandler, coercers). String-scan strips AND
        // drops MAPPER_S. TRANSITIONAL path, C3a.4 removal.
        JavaExpression wrapped = JavaExpression.from(
                "MapperS.of(BigDecimal.valueOf(0))", null,
                Set.of(HandlerHelper.MAPPER_S, HandlerHelper.BIG_DECIMAL));
        JavaStatementBuilder unwrapped = renderer.unwrapForAssignment(wrapped);
        assertEquals("BigDecimal.valueOf(0)",
                ((JavaExpression) unwrapped).renderToString());
        assertEquals(Set.of(HandlerHelper.BIG_DECIMAL), unwrapped.getRefs(),
                "legacy string-scan unwrap drops MAPPER_S, preserves other refs (BIG_DECIMAL)");
    }

    @Test
    void unwrapForAssignment_mapper_maths_falls_through_to_get() {
        // Complex mapper expression — no wrap-factory route, no MapperS.of-wrap
        // shape. Falls through to `.get()` suffix with refs preserved verbatim.
        JavaExpression mapperMaths = JavaExpression.from(
                "MapperMaths.<BigDecimal, BigDecimal, BigDecimal>multiply(MapperS.of(a), MapperS.of(b))",
                null,
                Set.of(HandlerHelper.MAPPER_MATHS, HandlerHelper.MAPPER_S, HandlerHelper.BIG_DECIMAL));
        JavaStatementBuilder unwrapped = renderer.unwrapForAssignment(mapperMaths);
        assertEquals(
                "MapperMaths.<BigDecimal, BigDecimal, BigDecimal>multiply(MapperS.of(a), MapperS.of(b)).get()",
                ((JavaExpression) unwrapped).renderToString());
        assertEquals(
                Set.of(HandlerHelper.MAPPER_MATHS, HandlerHelper.MAPPER_S, HandlerHelper.BIG_DECIMAL),
                unwrapped.getRefs(),
                "fall-through .get() preserves all inbound refs verbatim");
    }

    @Test
    void unwrapForAssignment_comparison_falls_through_to_get() {
        // Same fall-through branch — pin the dispatch on a different shape
        // (comparison helper result).
        JavaExpression cmp = JavaExpression.from(
                "areEqual(MapperS.of(s1), MapperS.of(s2), CardinalityOperator.All)",
                null, Set.of(HandlerHelper.CARDINALITY_OPERATOR, HandlerHelper.COMPARISON_RESULT));
        JavaStatementBuilder unwrapped = renderer.unwrapForAssignment(cmp);
        assertEquals(
                "areEqual(MapperS.of(s1), MapperS.of(s2), CardinalityOperator.All).get()",
                ((JavaExpression) unwrapped).renderToString());
    }

    @Test
    void unwrapForAssignment_null_builder_returns_synthetic_null() {
        JavaStatementBuilder fromNull = renderer.unwrapForAssignment(null);
        assertEquals("null", ((JavaExpression) fromNull).renderToString());
        assertEquals(Set.of(), fromNull.getRefs());
    }

    @Test
    void unwrapForAssignment_null_source_passthrough() {
        // A JavaExpression whose rendered source is "null" — passthrough with
        // reference equality (distinct from the null-builder branch which
        // synthesises a new instance).
        JavaExpression nullExpr = JavaExpression.from("null", null, Set.of());
        JavaStatementBuilder unwrapped = renderer.unwrapForAssignment(nullExpr);
        assertSame(nullExpr, unwrapped,
                "'null'-source JavaExpression must passthrough unchanged");
    }

    @Test
    void unwrapForAssignment_bare_value_falls_through_to_get() {
        JavaExpression bare = JavaExpression.from(
                "someExpression", null, Set.of(HandlerHelper.MAPPER_S));
        JavaStatementBuilder unwrapped = renderer.unwrapForAssignment(bare);
        assertEquals("someExpression.get()",
                ((JavaExpression) unwrapped).renderToString());
        assertEquals(Set.of(HandlerHelper.MAPPER_S), unwrapped.getRefs(),
                "fall-through must preserve inbound refs verbatim");
    }

    @Test
    void unwrapForAssignment_composite_builder_falls_through_via_renderToString() {
        // R2 finding #1 (MINOR) from C3a.3 review: pin the composite-builder
        // (non-JavaExpression) input path. No current call site produces this
        // shape (ControlFlowHandler collapses ternaries before they reach
        // unwrapForAssignment), but M7b-4 handler output-type widening is a
        // plausible future drift vector. The invariant being pinned: the
        // composite collapse path via renderToString's 3-tier strategy yields
        // a valid unwrapped JavaExpression carrying the composite's refs.
        //
        // Construction: a JavaBlockBuilder wraps a leading JavaStatementList
        // plus an ending JavaExpression. Refs union both sides
        // (JavaBlockBuilder.getRefs() — exercised by
        // JavaStatementBuilderRefsTest.refs_aggregate_through_javablockbuilder).
        // Ending expression renders as "someExpression" — a bare non-null,
        // non-MapperS-of, non-enum identifier that routes to the fall-through
        // .get() branch. Leading-statement refs (MAPPER_C) must survive the
        // fall-through so downstream imports are complete.
        com.regnosys.rosetta.generator.java.statement.builder.JavaBlockBuilder block =
                new com.regnosys.rosetta.generator.java.statement.builder.JavaBlockBuilder(
                        com.regnosys.rosetta.generator.java.statement.JavaStatementList.of(
                                new com.regnosys.rosetta.generator.java.statement.JavaReturnStatement(
                                        JavaExpression.from("leading",
                                                null, Set.of(HandlerHelper.MAPPER_C)))),
                        JavaExpression.from("someExpression", null,
                                Set.of(HandlerHelper.MAPPER_S)));
        JavaStatementBuilder unwrapped = renderer.unwrapForAssignment(block);
        // Composite fall-through must produce a JavaExpression suitable for
        // further rendering — assertInstanceOf avoids a ClassCastException
        // silently masking a wrong-type return from the helper.
        assertInstanceOf(JavaExpression.class, unwrapped,
                "composite-builder fall-through must yield a JavaExpression");
        // Refs preserved VERBATIM across both sides of the block
        // (MAPPER_C from leading + MAPPER_S from ending) — neither MAPPER_S
        // is dropped (no wrapping detected) nor MAPPER_C silently lost.
        assertEquals(
                Set.of(HandlerHelper.MAPPER_C, HandlerHelper.MAPPER_S),
                unwrapped.getRefs(),
                "composite-builder refs (leadingStatements ∪ lastStatement) must "
                        + "propagate through the fall-through branch");
    }

    // =========================================================================
    // unwrapForAddAssignment: direct unit tests (PR-A v6.2 C3a.3 — builder-level)
    // =========================================================================

    @Test
    void unwrapForAddAssignment_null_builder_returns_collections_emptylist_with_ref() {
        // null builder → "Collections.emptyList()" synthetic carrying the
        // COLLECTIONS ref so ImportCollector can import it at C3c.1 consumption.
        JavaStatementBuilder fromNull = renderer.unwrapForAddAssignment(null);
        assertEquals("Collections.emptyList()",
                ((JavaExpression) fromNull).renderToString());
        assertEquals(Set.of(HandlerHelper.COLLECTIONS), fromNull.getRefs(),
                "null-builder synthetic must carry COLLECTIONS ref for imports");
    }

    @Test
    void unwrapForAddAssignment_structural_strips_wrapper() {
        // wrappedInMapperSOf(inner) → unwrap returns inner with MAPPER_S dropped.
        JavaExpression inner = JavaExpression.from("items", null, Set.of());
        JavaExpression wrapped = JavaExpression.wrappedInMapperSOf(inner);
        JavaStatementBuilder unwrapped = renderer.unwrapForAddAssignment(wrapped);
        assertEquals("items", ((JavaExpression) unwrapped).renderToString());
        assertEquals(Set.of(), unwrapped.getRefs());
    }

    @Test
    void unwrapForAddAssignment_raw_MapperS_of_text_falls_through_after_the_strip_deletion() {
        // facet mapperWrapPrefix (v3.1 C2d retirement family 8, PR #615): the legacy string-scan
        // strip in unwrapForAddAssignment is DELETED (dead at 0 of 8,211 census arrivals — an
        // exact tie with the method's whole non-structural population, i.e. every non-structural
        // arrival reached the strip and the strip matched nothing). RETARGETED, not removed: a
        // RAW `MapperS.of(...)` text with no marker now takes the `.getMulti()` fall-through and
        // keeps its refs verbatim — the unit-level decline lock for the deletion.
        JavaExpression wrapped = JavaExpression.from(
                "MapperS.of(items)", null, Set.of(HandlerHelper.MAPPER_S));
        JavaStatementBuilder unwrapped = renderer.unwrapForAddAssignment(wrapped);
        assertEquals("MapperS.of(items).getMulti()",
                ((JavaExpression) unwrapped).renderToString(),
                "an unmarked MapperS.of text must take the .getMulti() fall-through");
        assertEquals(Set.of(HandlerHelper.MAPPER_S), unwrapped.getRefs(),
                "the fall-through preserves refs verbatim — the source still names MapperS");
    }

    @Test
    void unwrapForAssignment_preserves_staticWildcardImports_across_legacy_string_scan() {
        // Copilot round-6 finding: the legacy MapperS.of string-scan branch
        // must preserve staticWildcardImports. Previously dropped via 3-arg
        // JavaExpression.from — breaks EXPRESSION_OPERATORS_NULL_SAFE
        // propagation once C3a.4.b/d populate the channel.
        JavaExpression wrapped = JavaExpression.from(
                "MapperS.of(areEqual(left, right, CardinalityOperator.All))",
                null, Set.of(HandlerHelper.MAPPER_S),
                Set.of(HandlerHelper.EXPRESSION_OPERATORS_NULL_SAFE));
        JavaStatementBuilder unwrapped = renderer.unwrapForAssignment(wrapped);
        assertEquals(Set.of(HandlerHelper.EXPRESSION_OPERATORS_NULL_SAFE),
                unwrapped.getStaticWildcardImports(),
                "legacy string-scan unwrap must preserve staticWildcardImports");
    }

    @Test
    void unwrapForAssignment_preserves_staticWildcardImports_across_get_fallthrough() {
        // Copilot round-6 finding (companion to above): the fall-through .get()
        // branch must preserve staticWildcardImports.
        JavaExpression bare = JavaExpression.from(
                "areEqual(left, right, CardinalityOperator.All)",
                null, Set.of(HandlerHelper.CARDINALITY_OPERATOR),
                Set.of(HandlerHelper.EXPRESSION_OPERATORS_NULL_SAFE));
        JavaStatementBuilder unwrapped = renderer.unwrapForAssignment(bare);
        assertEquals(Set.of(HandlerHelper.EXPRESSION_OPERATORS_NULL_SAFE),
                unwrapped.getStaticWildcardImports(),
                "fall-through .get() branch must preserve staticWildcardImports");
    }

    @Test
    void unwrapForAddAssignment_preserves_staticWildcardImports_across_legacy_string_scan() {
        // Copilot round-6 finding: MapperS.of string-scan in unwrapForAddAssignment
        // must also preserve staticWildcardImports (symmetric with
        // unwrapForAssignment).
        JavaExpression wrapped = JavaExpression.from(
                "MapperS.of(items)", null, Set.of(HandlerHelper.MAPPER_S),
                Set.of(HandlerHelper.EXPRESSION_OPERATORS_NULL_SAFE));
        JavaStatementBuilder unwrapped = renderer.unwrapForAddAssignment(wrapped);
        assertEquals(Set.of(HandlerHelper.EXPRESSION_OPERATORS_NULL_SAFE),
                unwrapped.getStaticWildcardImports(),
                "addAssign legacy string-scan unwrap must preserve staticWildcardImports");
    }

    @Test
    void unwrapForAddAssignment_preserves_staticWildcardImports_across_getMulti_fallthrough() {
        // Copilot round-6 finding (companion): .getMulti() fall-through must
        // preserve staticWildcardImports.
        JavaExpression mapperC = JavaExpression.from(
                "MapperC.<T>of(list)", null, Set.of(HandlerHelper.MAPPER_C),
                Set.of(HandlerHelper.EXPRESSION_OPERATORS_NULL_SAFE));
        JavaStatementBuilder unwrapped = renderer.unwrapForAddAssignment(mapperC);
        assertEquals(Set.of(HandlerHelper.EXPRESSION_OPERATORS_NULL_SAFE),
                unwrapped.getStaticWildcardImports(),
                "addAssign fall-through .getMulti() branch must preserve staticWildcardImports");
    }

    @Test
    void unwrapForAddAssignment_mapper_falls_through_to_getMulti() {
        // Complex mapper expression — fall-through to .getMulti() with refs preserved.
        JavaExpression mapperC = JavaExpression.from(
                "MapperC.<T>of(list).mapItem(...)", null,
                Set.of(HandlerHelper.MAPPER_C));
        JavaStatementBuilder unwrapped = renderer.unwrapForAddAssignment(mapperC);
        assertEquals(
                "MapperC.<T>of(list).mapItem(...).getMulti()",
                ((JavaExpression) unwrapped).renderToString());
        assertEquals(Set.of(HandlerHelper.MAPPER_C), unwrapped.getRefs(),
                "fall-through .getMulti() preserves inbound refs verbatim");
    }

    // =========================================================================
    // renderOperation: SET with segment path — builder-chain rendering (M7b-4)
    // =========================================================================

    private static RSegment segment(String name) {
        RSegment s = new RSegment();
        s.setName(name);
        return s;
    }

    private static RSegment segments(String... names) {
        // Build a linked chain: segments("a","b","c") → a -> b -> c
        if (names.length == 0) {
            return null;
        }
        RSegment head = segment(names[0]);
        RSegment cur = head;
        for (int i = 1; i < names.length; i++) {
            RSegment next = segment(names[i]);
            cur.setNext(next);
            cur = next;
        }
        return head;
    }

    private static ROperation operationWithSegment(String targetName, RSegment seg, RExpression expression) {
        ROperation op = new ROperation();
        op.setTargetName(targetName);
        op.setOperator(OperationOp.SET);
        op.setSegment(seg);
        op.setExpression(expression);
        return op;
    }

    /**
     * Continuation-line indent prefix pinned to {@code DEFAULT_INDENT_LEVEL + 1}.
     * Using this constant (rather than a raw {@code "\n\t\t\t\t"}) means the
     * assertions track the indent RULE, not a magic tab count — changing
     * {@code DEFAULT_INDENT_LEVEL} no longer silently shifts test meaning.
     */
    private static final String CONT =
            "\n" + "\t".repeat(FunctionExpressionRenderer.DEFAULT_INDENT_LEVEL + 1);

    @Test
    void set_with_single_segment_emits_set_call() {
        // set X -> a: expr → X<CONT>.setA(value);
        // Use symbolRef so MapperS.of(x) is stripped to bare "x"
        ROperation op = operationWithSegment("X", segments("a"), symbolRef("x"));
        String result = renderer.renderOperation(op).source();
        assertEquals("X" + CONT + ".setA(x);", result);
    }

    @Test
    void set_with_two_segments_emits_getOrCreate_then_set() {
        // set X -> a -> b: expr → X<CONT>.getOrCreateA()<CONT>.setB(value);
        ROperation op = operationWithSegment("X", segments("a", "b"), symbolRef("x"));
        String result = renderer.renderOperation(op).source();
        assertEquals("X" + CONT + ".getOrCreateA()" + CONT + ".setB(x);", result);
    }

    @Test
    void set_with_three_segments_emits_two_getOrCreate_then_set() {
        // set X -> a -> b -> c: expr
        ROperation op = operationWithSegment("X", segments("a", "b", "c"), symbolRef("x"));
        String result = renderer.renderOperation(op).source();
        assertEquals(
                "X" + CONT + ".getOrCreateA()" + CONT + ".getOrCreateB()" + CONT + ".setC(x);",
                result);
    }

    @Test
    void set_with_multi_word_segment_name_capitalizes_correctly() {
        // Pins the JavaNamingUtil.toFirstUpper contract: only the first char
        // flips; the rest of a lowerCamelCase attribute stays intact.
        ROperation op = operationWithSegment("X",
                segments("adjustableDate", "businessDayConvention"), symbolRef("x"));
        String result = renderer.renderOperation(op).source();
        assertEquals(
                "X" + CONT + ".getOrCreateAdjustableDate()" + CONT + ".setBusinessDayConvention(x);",
                result);
    }

    @Test
    void set_without_segment_still_uses_simple_assignment_regression() {
        // Regression: segment().isEmpty() → plain "result = x;"
        ROperation op = operation("result", OperationOp.SET, symbolRef("x"));
        assertNull(op.segment().orElse(null), "pre-condition: segment must be absent");
        String result = renderer.renderOperation(op).source();
        assertEquals("result = x;", result);
    }

    @Test
    void set_with_segment_unwraps_MapperS_of_to_bare_value() {
        // symbolRef("myValue") → MapperS.of(myValue) → unwrap to "myValue"
        ROperation op = operationWithSegment("X", segments("field"), symbolRef("myValue"));
        String result = renderer.renderOperation(op).source();
        assertEquals("X" + CONT + ".setField(myValue);", result);
    }

    @Test
    void set_with_segment_arithmetic_expression_uses_get_suffix() {
        // Arithmetic → MapperMaths.add(...) → must get .get() appended
        ROperation op = operationWithSegment("X", segments("total"),
                arithmetic(intLiteral(1), ArithOp.PLUS, intLiteral(2)));
        String result = renderer.renderOperation(op).source();
        assertEquals(
                "X" + CONT + ".setTotal(MapperMaths.<Integer, Integer, Integer>add("
                        + "MapperS.of(1), MapperS.of(2)).get());",
                result);
    }

    // =========================================================================
    // Engine PR #6 facet (c) — bare-rule-then detection predicates
    // =========================================================================

    private static RRule resolvedRule(String name) {
        RModel model = new RModel();
        model.setNamespace("com.example");
        RRule rule = new RRule();
        rule.setName(name);
        rule.setParent(model);
        model.rootElements().add(rule);
        return rule;
    }

    private static RSymbolReference noArgRefTo(RRule rule) {
        RSymbolReference ref = new RSymbolReference();
        ref.setName(rule.name());
        ref.setResolvedSymbol(rule);
        return ref;
    }

    private static RThenExpr thenOf(RExpression argument, RExpression bodyExpr) {
        RInlineFunction body = new RInlineFunction();
        body.setImplicit(true);
        body.setBody(bodyExpr);
        RThenExpr then = new RThenExpr();
        then.setArgument(argument);
        then.setBody(body);
        return then;
    }

    @Test
    void isBareRuleThen_true_for_filter_then_bareRule() {
        RThenExpr then = thenOf(filterOf("input", "IsAllowableActionForASIC"),
                noArgRefTo(resolvedRule("MaturityDateOfTheUnderlier")));
        assertTrue(CollectionHandler.isBareRuleThen(then));
        assertEquals("MaturityDateOfTheUnderlier",
                CollectionHandler.bareRuleThenTarget(then).name());
    }

    @Test
    void isBareRuleThen_false_for_then_extract() {
        // body is an extract (RExtractExpr), not a bare rule reference.
        RExtractExpr extract = new RExtractExpr();
        extract.setArgument(symbolRef("input"));
        RInlineFunction extractBody = new RInlineFunction();
        extractBody.setImplicit(true);
        extractBody.setBody(symbolRef("field"));
        extract.setBody(extractBody);
        RThenExpr then = thenOf(symbolRef("input"), extract);
        assertFalse(CollectionHandler.isBareRuleThen(then));
    }

    @Test
    void isBareRuleThen_false_for_then_function() {
        // symbol resolves to an RFunction, not an RRule.
        RFunction fn = new RFunction();
        fn.setName("ProductForEvent");
        RModel m = new RModel();
        m.setNamespace("com.example");
        fn.setParent(m);
        RSymbolReference ref = new RSymbolReference();
        ref.setName("ProductForEvent");
        ref.setResolvedSymbol(fn);
        RThenExpr then = thenOf(symbolRef("input"), ref);
        assertFalse(CollectionHandler.isBareRuleThen(then));
    }

    @Test
    void isBareRuleThen_false_for_rule_with_args() {
        RSymbolReference ref = noArgRefTo(resolvedRule("SomeRule"));
        ref.args().add(intLiteral(1));
        RThenExpr then = thenOf(symbolRef("input"), ref);
        assertFalse(CollectionHandler.isBareRuleThen(then));
    }

    @Test
    void isBareRuleThen_false_for_chained_then() {
        // … then A then B — the argument is itself an RThenExpr (single-then gate).
        RThenExpr inner = thenOf(symbolRef("input"),
                noArgRefTo(resolvedRule("InnerRule")));
        RThenExpr outer = thenOf(inner, noArgRefTo(resolvedRule("OuterRule")));
        assertFalse(CollectionHandler.isBareRuleThen(outer));
    }

    @Test
    void isBareRuleThen_false_for_no_body_then() {
        RThenExpr then = new RThenExpr();
        then.setArgument(symbolRef("input"));
        // body left absent
        assertFalse(CollectionHandler.isBareRuleThen(then));
    }

    @Test
    void renderOperation_then_function_falls_through_to_generic_then_path() {
        // A then whose body resolves to a function is NOT bare-rule-then. Under
        // the ZERO-ARG compiler the then-statement branches' wired-compiler
        // predicate declines (facet then_statement_hoisting widened the context
        // gate, so the operative guard here is the missing generator model, not
        // the absent enclosing rule), and the generic arg.then(lambda) path is
        // used; the bare-invokable branch is never taken.
        RFunction fn = new RFunction();
        fn.setName("SomeFunc");
        RModel m = new RModel();
        m.setNamespace("com.example");
        fn.setParent(m);
        RSymbolReference ref = new RSymbolReference();
        ref.setName("SomeFunc");
        ref.setResolvedSymbol(fn);
        ROperation op = operation("output", OperationOp.SET, thenOf(symbolRef("input"), ref));
        String result = renderer.renderOperation(op, 3).source();
        assertTrue(result.contains(".then("),
                "Non-bare-rule then must use the generic .then(lambda) path, got: " + result);
        assertFalse(result.contains("thenArg"),
                "Generic then path must not emit a thenArg declaration");
    }

    // =========================================================================
    // Engine PR #6 facet (c) — byte-exact 2-statement render (wired compiler)
    // =========================================================================

    private static final Path BUILTINS_DIR = Path.of("../test-corpus/rune-dsl-builtins");

    static boolean builtinsAvailable() {
        return Files.isDirectory(BUILTINS_DIR);
    }

    /**
     * Parse builtins + a minimal inline model that reproduces the anchor's
     * bare-rule-then shape, build a fully-wired {@link FunctionExpressionRenderer},
     * synthesise the rule via {@link RFunction#fromRule(RRule)}, and assert the
     * exact 2-statement byte string the golden anchor emits at indentLevel 3.
     *
     * <p>Golden anchor (drr/.../asic/.../MaturityDateOfTheUnderlierRule.java L41-43):
     * <pre>
     * final MapperS&lt;Underlier&gt; thenArg = MapperS.of(input)
     *     .filterSingleNullSafe(item -&gt; isAllowableActionForASIC.evaluate(item.get()));
     * output = MapperS.of(theDateRule.evaluate(thenArg.get())).get();
     * </pre>
     */
    @Test
    @org.junit.jupiter.api.condition.EnabledIf("builtinsAvailable")
    void renderBareInvokableThenSet_rule_emits_exact_two_statement_block() throws IOException {
        String source = """
                namespace com.example

                type Underlier:
                    name string (0..1)

                func IsAllowableActionForASIC:
                    inputs: u Underlier (1..1)
                    output: result boolean (1..1)
                    set result: True

                reporting rule TheDate from Underlier:
                    extract name

                reporting rule MaturityDateOfTheUnderlier from Underlier:
                    filter IsAllowableActionForASIC
                    then TheDate
                """;

        List<RModel> models = loadBuiltins();
        RModel model = AstBuilder.buildFromString(source, "example.rosetta");
        model.setVersion("0.0.0.test-SNAPSHOT");
        models.add(model);
        var linking = RWorkspace.build(models);
        var gm = new GeneratorModel(linking.workspace());
        JavaTypeUtil typeUtil = new JavaTypeUtil();
        JavaTypeTranslator typeTranslator = new JavaTypeTranslator(typeUtil);
        FunctionExpressionRenderer wiredRenderer = new FunctionExpressionRenderer(
                new ExpressionCompiler(gm, typeTranslator, typeUtil));

        RRule rule = model.rootElements().stream()
                .filter(e -> e instanceof RRule r && "MaturityDateOfTheUnderlier".equals(r.name()))
                .map(e -> (RRule) e)
                .findFirst()
                .orElseThrow(() -> new AssertionError("MaturityDateOfTheUnderlier rule not found"));

        RFunction synthetic = RFunction.fromRule(rule);
        assertFalse(synthetic.operations().isEmpty(), "fromRule must synthesise the SET operation");
        ROperation op = synthetic.operations().get(0);

        // Sanity: the operation expression is the bare-rule-then shape.
        assertTrue(op.expression() instanceof RThenExpr, "expected RThenExpr operation body");
        assertTrue(CollectionHandler.isBareRuleThen((RThenExpr) op.expression()),
                "operation body must be a bare-rule-then");

        String result = wiredRenderer.renderOperation(op, 3).source();

        // Element type is the rule's from-type (filter preserves receiver type).
        // 2-statement block: declaration (unprefixed first line + 1-tab continuation
        // re-indented to indentLevel+1=4) then the output assignment at indentLevel=3.
        String expected =
                "final MapperS<Underlier> thenArg = MapperS.of(input)\n"
                + "\t\t\t\t.filterSingleNullSafe(item -> isAllowableActionForASIC.evaluate(item.get()));\n"
                + "\t\t\toutput = MapperS.of(theDateRule.evaluate(thenArg.get())).get();";
        assertEquals(expected, result);
    }

    @Test
    @org.junit.jupiter.api.condition.EnabledIf("builtinsAvailable")
    void renderBareInvokableThenSet_rule_wraps_toBuilder_for_model_typed_output() throws IOException {
        // Copilot R2: a bare-rule-then SET whose output is a Rosetta model object
        // (outputNeedsBuilder=true) must wrap the assignment in toBuilder(...),
        // mirroring the generic non-segment SET path + upstream
        // FunctionGenerator.assign's needsBuilder(attribute). Only the output
        // assignment is wrapped; the thenArg declaration is unchanged. Locks the
        // outputNeedsBuilder=true branch directly (D11 covers it at integration
        // level via the drr barrier rules; this is the unit double-cover).
        String source = """
                namespace com.example

                type Underlier:
                    name string (0..1)

                func IsAllowableActionForASIC:
                    inputs: u Underlier (1..1)
                    output: result boolean (1..1)
                    set result: True

                reporting rule TheDate from Underlier:
                    extract name

                reporting rule MaturityDateOfTheUnderlier from Underlier:
                    filter IsAllowableActionForASIC
                    then TheDate
                """;

        List<RModel> models = loadBuiltins();
        RModel model = AstBuilder.buildFromString(source, "example.rosetta");
        model.setVersion("0.0.0.test-SNAPSHOT");
        models.add(model);
        var linking = RWorkspace.build(models);
        var gm = new GeneratorModel(linking.workspace());
        JavaTypeUtil typeUtil = new JavaTypeUtil();
        JavaTypeTranslator typeTranslator = new JavaTypeTranslator(typeUtil);
        FunctionExpressionRenderer wiredRenderer = new FunctionExpressionRenderer(
                new ExpressionCompiler(gm, typeTranslator, typeUtil));

        RRule rule = model.rootElements().stream()
                .filter(e -> e instanceof RRule r && "MaturityDateOfTheUnderlier".equals(r.name()))
                .map(e -> (RRule) e)
                .findFirst()
                .orElseThrow(() -> new AssertionError("MaturityDateOfTheUnderlier rule not found"));

        RFunction synthetic = RFunction.fromRule(rule);
        ROperation op = synthetic.operations().get(0);
        assertTrue(CollectionHandler.isBareRuleThen((RThenExpr) op.expression()),
                "operation body must be a bare-rule-then");

        // outputNeedsBuilder=true → the output assignment wraps in toBuilder(...).
        String result = wiredRenderer.renderOperation(op, 3, true).source();

        String expected =
                "final MapperS<Underlier> thenArg = MapperS.of(input)\n"
                + "\t\t\t\t.filterSingleNullSafe(item -> isAllowableActionForASIC.evaluate(item.get()));\n"
                + "\t\t\toutput = toBuilder(MapperS.of(theDateRule.evaluate(thenArg.get())).get());";
        assertEquals(expected, result);
    }

    @Test
    @org.junit.jupiter.api.condition.EnabledIf("builtinsAvailable")
    void renderBareInvokableThenSet_rule_seeds_mapperS_and_item_refs() throws IOException {
        String source = """
                namespace com.example

                type Underlier:
                    name string (0..1)

                func IsAllowableActionForASIC:
                    inputs: u Underlier (1..1)
                    output: result boolean (1..1)
                    set result: True

                reporting rule TheDate from Underlier:
                    extract name

                reporting rule MaturityDateOfTheUnderlier from Underlier:
                    filter IsAllowableActionForASIC
                    then TheDate
                """;

        List<RModel> models = loadBuiltins();
        RModel model = AstBuilder.buildFromString(source, "example.rosetta");
        model.setVersion("0.0.0.test-SNAPSHOT");
        models.add(model);
        var linking = RWorkspace.build(models);
        var gm = new GeneratorModel(linking.workspace());
        JavaTypeUtil typeUtil = new JavaTypeUtil();
        JavaTypeTranslator typeTranslator = new JavaTypeTranslator(typeUtil);
        FunctionExpressionRenderer wiredRenderer = new FunctionExpressionRenderer(
                new ExpressionCompiler(gm, typeTranslator, typeUtil));

        RRule rule = model.rootElements().stream()
                .filter(e -> e instanceof RRule r && "MaturityDateOfTheUnderlier".equals(r.name()))
                .map(e -> (RRule) e)
                .findFirst().orElseThrow();
        ROperation op = RFunction.fromRule(rule).operations().get(0);

        var refs = wiredRenderer.renderOperation(op, 3).refs();
        boolean hasMapperS = refs.stream().anyMatch(c -> c.getSimpleName().equals("MapperS"));
        boolean hasItem = refs.stream().anyMatch(c -> c.getSimpleName().equals("Underlier"));
        assertTrue(hasMapperS, "thenArg declaration must seed the MapperS ref. refs=" + refs);
        assertTrue(hasItem, "thenArg declaration must seed the generic item ref (Underlier). refs=" + refs);
    }

    /**
     * Engine PR #13 (facet F6): the bare-FUNCTION-then sibling of
     * {@link #renderBareInvokableThenSet_rule_emits_exact_two_statement_block}.
     * A {@code then <bare-function>} body routes through the SAME
     * {@code renderBareInvokableThenSet} 2-statement form; the only difference is
     * the receiver — the function's injected lowerCamel field name (here
     * {@code fmtRate}), derived from the resolved {@link RFunction}'s simple
     * {@code name()} via {@code FunctionDependencyCollector.lowerCamelCase}. The
     * RHS {@code output = MapperS.of(fmtRate.evaluate(thenArg.get())).get();} is
     * fully deterministic (independent of the then-argument's navigation render),
     * so it is asserted exactly; the broken inline {@code .then(...)} form must be
     * absent.
     */
    @Test
    @org.junit.jupiter.api.condition.EnabledIf("builtinsAvailable")
    void renderBareInvokableThenSet_function_emits_thenArg_form_with_function_receiver() throws IOException {
        String source = """
                namespace com.example

                type Underlier:
                    val number (0..1)

                func FmtRate:
                    inputs: x number (1..1)
                    output: result number (1..1)
                    set result: x

                reporting rule RateOfTheUnderlier from Underlier:
                    val then FmtRate
                """;

        List<RModel> models = loadBuiltins();
        RModel model = AstBuilder.buildFromString(source, "example.rosetta");
        model.setVersion("0.0.0.test-SNAPSHOT");
        models.add(model);
        var linking = RWorkspace.build(models);
        var gm = new GeneratorModel(linking.workspace());
        JavaTypeUtil typeUtil = new JavaTypeUtil();
        JavaTypeTranslator typeTranslator = new JavaTypeTranslator(typeUtil);
        FunctionExpressionRenderer wiredRenderer = new FunctionExpressionRenderer(
                new ExpressionCompiler(gm, typeTranslator, typeUtil));

        RRule rule = model.rootElements().stream()
                .filter(e -> e instanceof RRule r && "RateOfTheUnderlier".equals(r.name()))
                .map(e -> (RRule) e)
                .findFirst()
                .orElseThrow(() -> new AssertionError("RateOfTheUnderlier rule not found"));
        ROperation op = RFunction.fromRule(rule).operations().get(0);

        // Sanity: the operation body is a bare-FUNCTION-then (not a bare-rule-then).
        assertTrue(op.expression() instanceof RThenExpr, "expected RThenExpr operation body");
        RThenExpr then = (RThenExpr) op.expression();
        assertTrue(CollectionHandler.isBareFunctionThen(then),
                "operation body must be a bare-function-then");
        assertFalse(CollectionHandler.isBareRuleThen(then),
                "a bare-function-then must NOT be classified as a bare-rule-then");

        String result = wiredRenderer.renderOperation(op, 3).source();

        assertFalse(result.contains(".then("),
                "bare-function-then must NOT emit the broken inline `.then(...)` form; got:\n" + result);
        assertTrue(result.contains("final MapperS<BigDecimal> thenArg = MapperS.of(input)"),
                "must declare `final MapperS<BigDecimal> thenArg = MapperS.of(input)…`; got:\n" + result);
        // The RHS is deterministic — receiver = lowerCamelCase(FmtRate) = fmtRate.
        assertTrue(result.contains("output = MapperS.of(fmtRate.evaluate(thenArg.get())).get();"),
                "the SET RHS must invoke the injected function instance on thenArg.get(); got:\n" + result);
    }

    /**
     * Engine PR #14 (facet F6 — coercion variant): when the bare-function-then's
     * target takes a single input whose Java type DIFFERS from the piped
     * ({@code thenArg}) element type, the evaluate argument is numerically
     * coerced — mirroring upstream {@code TypeCoercionService.convertNullSafe}:
     * hoist the unwrapped value into a {@code final <ItemType> <var> = thenArg.get();}
     * local, then pass the null-guarded conversion
     * {@code (<var> == null ? null : <coerceFn>(<var>))} to {@code evaluate(...)}.
     * Here the rule pipes an {@code int} (→ {@code Integer}) into a
     * {@code number}-input function (→ {@code BigDecimal}), so the conversion is
     * {@code BigDecimal.valueOf}. The conversion expression itself is produced by
     * the (PR-A) {@code TypeCoercionService}, NOT hardcoded.
     *
     * <p>Real-corpus anchor: {@code drr/standards/iosco/cde/version1/payment/
     * reports/PaymentFrequencyPeriodMultiplierRule.java} (body
     * {@code … then FormatToMax3Number}, piped Integer periodMultiplier coerced
     * to the function's number input).
     */
    @Test
    @org.junit.jupiter.api.condition.EnabledIf("builtinsAvailable")
    void renderBareInvokableThenSet_function_coercesArg_whenInputTypeDiffers() throws IOException {
        String source = """
                namespace com.example

                type Underlier:
                    mult int (0..1)

                func FmtNum:
                    inputs: x number (1..1)
                    output: result number (1..1)
                    set result: x

                reporting rule MultOfTheUnderlier from Underlier:
                    mult then FmtNum
                """;

        List<RModel> models = loadBuiltins();
        RModel model = AstBuilder.buildFromString(source, "example.rosetta");
        model.setVersion("0.0.0.test-SNAPSHOT");
        models.add(model);
        var linking = RWorkspace.build(models);
        var gm = new GeneratorModel(linking.workspace());
        JavaTypeUtil typeUtil = new JavaTypeUtil();
        JavaTypeTranslator typeTranslator = new JavaTypeTranslator(typeUtil);
        FunctionExpressionRenderer wiredRenderer = new FunctionExpressionRenderer(
                new ExpressionCompiler(gm, typeTranslator, typeUtil));

        RRule rule = model.rootElements().stream()
                .filter(e -> e instanceof RRule r && "MultOfTheUnderlier".equals(r.name()))
                .map(e -> (RRule) e)
                .findFirst()
                .orElseThrow(() -> new AssertionError("MultOfTheUnderlier rule not found"));
        ROperation op = RFunction.fromRule(rule).operations().get(0);

        RThenExpr then = (RThenExpr) op.expression();
        assertTrue(CollectionHandler.isBareFunctionThen(then),
                "operation body must be a bare-function-then");

        String result = wiredRenderer.renderOperation(op, 3).source();

        // thenArg declared as the piped Integer wrapper (no coercion yet).
        assertTrue(result.contains("final MapperS<Integer> thenArg = MapperS.of(input)"),
                "must declare `final MapperS<Integer> thenArg = MapperS.of(input)…`; got:\n" + result);
        // Upstream convertNullSafe: hoist the unwrapped value into a local before
        // the null-guard. Var name = toFirstLower(Integer) = `integer`.
        assertTrue(result.contains("final Integer integer = thenArg.get();"),
                "the coercion variant must hoist `final Integer integer = thenArg.get();` "
                + "(upstream convertNullSafe declareAsVariable); got:\n" + result);
        // The receiver = lowerCamelCase(FmtNum) = fmtNum; the evaluate arg is the
        // null-guarded BigDecimal.valueOf coercion produced by the service.
        assertTrue(result.contains(
                "output = MapperS.of(fmtNum.evaluate((integer == null ? null : BigDecimal.valueOf(integer)))).get();"),
                "the SET RHS must pass the null-guarded coercion of the hoisted local; got:\n" + result);
        // The bare (uncoerced) arg form must be ABSENT.
        assertFalse(result.contains("evaluate(thenArg.get())"),
                "the coercion case must NOT pass the bare `thenArg.get()`; got:\n" + result);
    }

    private static List<RModel> loadBuiltins() throws IOException {
        List<RModel> models = new ArrayList<>();
        if (Files.isDirectory(BUILTINS_DIR)) {
            try (var stream = Files.list(BUILTINS_DIR)) {
                stream.filter(p -> p.toString().endsWith(".rosetta"))
                        .sorted()
                        .forEach(p -> {
                            try {
                                models.add(AstBuilder.buildFromFile(p));
                            } catch (Exception e) {
                                System.err.println("Builtin parse error: " + p.getFileName()
                                        + " — " + e.getMessage());
                            }
                        });
            }
        }
        return models;
    }

    /**
     * facet returnIte — pin the {@link FunctionExpressionRenderer#typedEmptyElseOrNull}
     * DECLINE boundary directly. The corpus byte-anchors in {@code FunctionReturnIteTest}
     * all sit on the FIRING path, so this is the only test that goes RED if a future
     * change broadened THIS shared helper's seat gate (a MAPPER_MULTI / non-Mapper /
     * element-less seam). Since PR #357 (facet
     * returnIteMapperCEmpty) the LADDER's eligibility + implicit-else render consult the
     * MapperC-admitting sibling {@code typedEmptyAnyMapperOrNull} instead — the
     * {@code MapperC.<T>ofNull()} form now FIRES at the ladder seats — but this
     * MapperS-only helper keeps its boundary (its other consumers stay MapperS-scoped).
     *
     * <p>facet aliasSeamSignature (PR #612): re-keyed from the rendered signature STRING
     * onto the typed {@link FunctionTemplateModel.AliasSeam} the producer recorded beside
     * it. The retired {@code "MapperS<List<?>>"} and {@code "int"} cases have no seam
     * equivalent and are dropped: a seam's element is a simple name or the #247 sentinel,
     * never a generic, and a non-Mapper signature is a FORM, covered by the UNKNOWN and
     * builder cases below.
     */
    @Test
    void returnIteTypedEmptyElse_firesOnConcreteMapperS_declinesDeferredForms() {
        // FIRES: a concrete single-valued Mapper seam -> MapperS.<Item>ofNull().
        assertEquals("MapperS.<Foo>ofNull()", FunctionExpressionRenderer.typedEmptyElseOrNull(
                FunctionTemplateModel.AliasSeam.mapper(false, false, "Foo")));
        // FIRES: a witnessless WILDCARDED single seam needs no strip — its recorded element
        // already IS the bound (golden's ofNull witness is the bound, not the wildcard).
        assertEquals("MapperS.<FloatingRateSettingDetails>ofNull()",
                FunctionExpressionRenderer.typedEmptyElseOrNull(
                        FunctionTemplateModel.AliasSeam.mapper(
                                false, true, "FloatingRateSettingDetails")));
        // DECLINES (-> null) — the non-MAPPER_SINGLE families (the ladder seats route
        // MAPPER_MULTI through typedEmptyAnyMapperOrNull since #357):
        assertNull(FunctionExpressionRenderer.typedEmptyElseOrNull(
                        FunctionTemplateModel.AliasSeam.mapper(true, false, "Foo")),
                "a MAPPER_MULTI seam must decline THIS MapperS-only helper "
                + "(typedEmptyAnyMapperOrNull owns the MapperC.<T>ofNull form)");
        assertNull(FunctionExpressionRenderer.typedEmptyElseOrNull(
                        FunctionTemplateModel.AliasSeam.UNKNOWN),
                "an UNKNOWN-form seam (the `Object` / ComparisonResult signatures) must "
                + "decline (deferred ofEmpty form)");
        assertNull(FunctionExpressionRenderer.typedEmptyElseOrNull(
                        FunctionTemplateModel.AliasSeam.builder(false)),
                "a usesOutput builder-form seam must decline (not a Mapper seat)");
        assertNull(FunctionExpressionRenderer.typedEmptyElseOrNull(
                        FunctionTemplateModel.AliasSeam.mapper(false, true, null)),
                "an element-less single seam (the legacy MapperS<?> fallback) must decline "
                + "(no concrete bound for the ofNull witness)");
        assertNull(FunctionExpressionRenderer.typedEmptyElseOrNull(null));
    }
}
