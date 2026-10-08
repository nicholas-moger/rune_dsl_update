package com.regnosys.rosetta.generator.java.expression.handlers;

import com.regnosys.rosetta.ast.enums.SwitchGuardKind;
import com.regnosys.rosetta.ast.enums.SwitchGuardLiteralKind;
import com.regnosys.rosetta.ast.expressions.constructors.RConditionalExpr;
import com.regnosys.rosetta.ast.expressions.literals.RBooleanLiteral;
import com.regnosys.rosetta.ast.expressions.literals.RIntLiteral;
import com.regnosys.rosetta.ast.expressions.literals.RStringLiteral;
import com.regnosys.rosetta.ast.expressions.references.RSymbolReference;
import com.regnosys.rosetta.ast.expressions.supporting.RSwitchCase;
import com.regnosys.rosetta.ast.expressions.supporting.RSwitchCaseGuard;
import com.regnosys.rosetta.ast.expressions.unary.RSwitchExpr;
import com.regnosys.rosetta.generator.java.SilentDegradation;
import com.regnosys.rosetta.generator.java.expression.ExpressionCompiler;
import com.regnosys.rosetta.generator.java.expression.ExpressionCompilerTest;
import com.regnosys.rosetta.generator.java.expression.ExpressionContext;
import com.regnosys.rosetta.generator.java.statement.builder.JavaExpression;
import com.regnosys.rosetta.generator.java.statement.builder.JavaStatementBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pins the handler's INLINE forms — the conditional's ternary fallbacks, and (since v3.2 seat 12 - D52, the
 * R1 register site) the REFUSAL a switch meets at this seat where the chained-ternary fallback used to be
 * written (the switch section's own note).
 * Every scenario here exercises the DECLINED-hoist path by construction (null
 * expected type + unmarked unit-compiler scopes — no statement-hoist sink AND
 * no {@code GeneratorModel}), so these pins are unaffected by BOTH hoist arms:
 * facet {@code ifthenelse_result_hoisting}'s ComparisonResult statement-local
 * arm AND facet {@code ctor_setter_ite_hoist}'s item-typed initializer-form arm
 * ({@code hoistAsItemLocalOrNull}) — the latter declines here because the unit
 * compiler has no {@code GeneratorModel}/{@code TypeTranslator} and the test
 * scope marks no statement-hoist sink. The hoists' pure logic is pinned in
 * {@code StatementHoistSessionTest}, their byte shapes in
 * {@code FunctionIfThenElseHoistTest} + {@code FunctionCtorSetterIteHoistTest},
 * and the live-sink decline gates (effective-else, MULTI cardinality) by the
 * full 5-cell D11 matrix.
 */
class ControlFlowHandlerTest {

    private ControlFlowHandler handler;
    private ExpressionCompiler compiler;
    private ExpressionContext ctx;

    @BeforeEach
    void setUp() {
        handler  = new ControlFlowHandler();
        compiler = new ExpressionCompiler();
        ctx      = ExpressionContext.of(null, ExpressionCompilerTest.createTestScope());
    }

    // =========================================================================
    // Helper
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

    private RBooleanLiteral boolLit(boolean value) {
        var lit = new RBooleanLiteral();
        lit.setValue(value);
        return lit;
    }

    private RIntLiteral intLit(int value) {
        var lit = new RIntLiteral();
        lit.setValue(value);
        return lit;
    }

    private RStringLiteral strLit(String value) {
        var lit = new RStringLiteral();
        lit.setValue(value);
        return lit;
    }

    // =========================================================================
    // Conditional with both branches
    // =========================================================================

    @Test
    void conditional_with_else_generates_ternary() {
        var expr = new RConditionalExpr();
        expr.setCondition(boolLit(true));
        expr.setThenBranch(intLit(1));
        expr.setElseBranch(intLit(0));

        assertEquals(
            "MapperS.of(true).getOrDefault(false) ? MapperS.of(1) : MapperS.of(0)",
            render(handler.handle(expr, ctx, compiler)));
    }

    // =========================================================================
    // Conditional without else
    // =========================================================================

    @Test
    void conditional_without_else_generates_ternary_with_null() {
        var expr = new RConditionalExpr();
        expr.setCondition(symbolRef("isActive"));
        expr.setThenBranch(strLit("active"));
        // no else branch

        assertEquals(
            "MapperS.of(isActive).getOrDefault(false) ? MapperS.of(\"active\") : null",
            render(handler.handle(expr, ctx, compiler)));
    }

    /**
     * Facet {@code ctor_setter_ite_hoist} decline (no statement-hoist sink):
     * the item-typed initializer-form arm ({@code hoistAsItemLocalOrNull})
     * fires only when {@code findStatementHoistSink() != null} — the unit
     * compiler's test scope marks no sink (and carries no
     * {@code GeneratorModel}), so a NO-ELSE single-cardinality conditional with
     * a String-typed then keeps the inline ternary, NOT a hoisted
     * {@code String ifThenElseResult = null; if (…) {…}} local. This is the
     * sink-half of the rule/alias freeze (the session opens only on the
     * function-body path) AND the lambda-interior decline (the sink walk stops
     * at the lambda boundary) at the unit level.
     */
    @Test
    void ctorSetterIteHoist_declinesWithoutStatementHoistSink_keepsInlineTernary() {
        var expr = new RConditionalExpr();
        expr.setCondition(symbolRef("isActive"));
        expr.setThenBranch(strLit("active"));
        // no else branch — the hoist arm's shape, but no sink in the unit scope

        String rendered = render(handler.handle(expr, ctx, compiler));
        assertEquals(
            "MapperS.of(isActive).getOrDefault(false) ? MapperS.of(\"active\") : null",
            rendered);
        assertFalse(rendered.contains("ifThenElseResult"),
            "no-sink scope must NOT hoist an ifThenElseResult local");
    }

    // =========================================================================
    // Conditional dispatches via ExpressionCompiler
    // =========================================================================

    @Test
    void compiler_dispatches_conditional() {
        var expr = new RConditionalExpr();
        expr.setCondition(boolLit(false));
        expr.setThenBranch(intLit(42));
        expr.setElseBranch(intLit(99));

        var scope  = ExpressionCompilerTest.createTestScope();
        var result = compiler.compile(expr, null, scope);

        assertInstanceOf(JavaExpression.class, result);
        assertEquals(
            "MapperS.of(false).getOrDefault(false) ? MapperS.of(42) : MapperS.of(99)",
            ((JavaExpression) result).renderToString());
    }

    // =========================================================================
    // Switch with cases - THE R1 LAW since v3.2 seat 12 (D52, COUNTERS FIRST)
    // =========================================================================
    //
    // Every test below builds a bare switch and hands it to the handler with the stateless unit compiler - the
    // seat with NO ladder renderer. Until seat 12 that seat wrote the inline chained ternary
    // `Objects.equals(<guard>, MapperS.of(<subject>)) ? .. : ..` and these tests pinned its text and renderGuard's
    // literal forms (a boxed INT, a quoted STRING whatever its shape, the boolean keyword, the boxed DECIMAL, the
    // octal-safe leading zero). Upstream folds EVERY switch into a hoisted `switchArgument` if-ladder and the goldens
    // carry no chained ternary; the fork's ternary compared a literal against a Mapper (always false) or a raw
    // source name against a Mapper render (non-compiling) - the chaos M2 rows. Seat 12's R1 REFUSES at that seat
    // (SilentDegradation.Site.SWITCH_TERNARY_STUB) AFTER the case loop, so the per-case type-keyed refusal keeps its
    // precedence; the ternary is never written and renderGuard's literal forms have no emitting consumer any more.
    // The tests keep their names and pin the law at the seat now: the refusal, its site, and the guard KINDS the
    // message names (`guards LITERAL/default`, the front-end kind channel these tests were about); a literal-form
    // pin that was the whole point of a test is recorded in its javadoc as RETIRED WITH THE RENDER. The live literal
    // renderer is FunctionExpressionRenderer.renderSwitchGuardMapper at the blessed ladder seats (pinned by the
    // D11 rings); the SET-position / data-rule / lambda-block ladders are the seats a switch renders at.

    /**
     * The R1 law at this seat: the handler refuses with the register's own exception, the site named, the guard
     * kinds listed in case order (default cases as {@code default}), the subject's node class named.
     */
    private void assertRefusedAtSwitchTernaryStub(RSwitchExpr expr, String expectedGuardKinds) {
        var refusal = assertThrows(SilentDegradation.Refusal.class, () -> handler.handle(expr, ctx, compiler));
        assertEquals(SilentDegradation.Site.SWITCH_TERNARY_STUB, refusal.site());
        assertTrue(refusal.getMessage().contains("[SWITCH_TERNARY_STUB]"), refusal.getMessage());
        assertTrue(refusal.getMessage().contains("guards " + expectedGuardKinds + ";"),
                "the guard kinds in case order: " + refusal.getMessage());
        assertTrue(refusal.getMessage().contains("subject RSymbolReference"), refusal.getMessage());
    }

    /** The pre-seat pin was the chained ternary `Objects.equals("A", ..) ? .. : Objects.equals("B", ..) ? .. : ..`. */
    @Test
    void switch_with_literal_guards_generates_chained_ternaries() {
        // switch argument: case "A" then 1, case "B" then 2, default then 0
        var expr = new RSwitchExpr();
        expr.setArgument(symbolRef("status"));

        var case1 = new RSwitchCase();
        case1.setDefault(false);
        var guard1 = new RSwitchCaseGuard();
        guard1.setKind(SwitchGuardKind.LITERAL);
        guard1.setLiteralValue("A");
        case1.setGuard(guard1);
        case1.setExpression(intLit(1));
        expr.cases().add(case1);

        var case2 = new RSwitchCase();
        case2.setDefault(false);
        var guard2 = new RSwitchCaseGuard();
        guard2.setKind(SwitchGuardKind.LITERAL);
        guard2.setLiteralValue("B");
        case2.setGuard(guard2);
        case2.setExpression(intLit(2));
        expr.cases().add(case2);

        var defaultCase = new RSwitchCase();
        defaultCase.setDefault(true);
        defaultCase.setExpression(intLit(0));
        expr.cases().add(defaultCase);

        assertRefusedAtSwitchTernaryStub(expr, "LITERAL/LITERAL/default");
    }

    /**
     * A NAME guard that names an ENUM VALUE (not a type) passes renderGuard's type-keyed refusal and reaches R1 - the
     * pre-seat pin was `Objects.equals(EventTypeEnum.TRADE, MapperS.of(eventType)) ? .. : ..`. A NAME guard naming a
     * TYPE keeps its own site (TYPE_SWITCH_TERNARY_STUB, v3.1 C0 - the per-case precedence R1 preserves).
     */
    @Test
    void switch_with_name_guard_generates_qualified_name() {
        var expr = new RSwitchExpr();
        expr.setArgument(symbolRef("eventType"));

        var case1 = new RSwitchCase();
        case1.setDefault(false);
        var guard1 = new RSwitchCaseGuard();
        guard1.setKind(SwitchGuardKind.NAME);
        guard1.setQualifiedName("EventTypeEnum.TRADE");
        case1.setGuard(guard1);
        case1.setExpression(strLit("trade"));
        expr.cases().add(case1);

        var defaultCase = new RSwitchCase();
        defaultCase.setDefault(true);
        defaultCase.setExpression(strLit("other"));
        expr.cases().add(defaultCase);

        assertRefusedAtSwitchTernaryStub(expr, "NAME/default");
    }

    /**
     * v3.1 C2d family 1: the INT arm keyed on the guard's lexer-terminal KIND boxed the value
     * (`Objects.equals(Integer.valueOf(42), ..)`). RETIRED WITH THE RENDER at seat 12's R1: the boxed form is
     * never emitted from this seat; the kind channel is exercised by the refusal's guard-kind list.
     */
    @Test
    void switch_with_numeric_literal_guard_generates_boxed_value() {
        // switch argument: case 42 then 1, default then 0 - an INT-kind guard
        var expr = new RSwitchExpr();
        expr.setArgument(symbolRef("code"));

        var case1 = new RSwitchCase();
        case1.setDefault(false);
        var guard1 = new RSwitchCaseGuard();
        guard1.setKind(SwitchGuardKind.LITERAL);
        guard1.setLiteralValue("42");
        guard1.setLiteralKind(SwitchGuardLiteralKind.INT);
        case1.setGuard(guard1);
        case1.setExpression(intLit(1));
        expr.cases().add(case1);

        var defaultCase = new RSwitchCase();
        defaultCase.setDefault(true);
        defaultCase.setExpression(intLit(0));
        expr.cases().add(defaultCase);

        assertRefusedAtSwitchTernaryStub(expr, "LITERAL/default");
    }

    /**
     * v3.1 C2d family 1 ({@code numeric-literal-kind}), the four DEAD rows: the Long / Float / Double
     * arms of {@code renderGuard} classified the guard's VALUE TEXT ({@code 1L}, {@code 1.0f},
     * {@code 1.5}) and boxed it — shapes the grammar cannot produce as a numeric literal
     * ({@code INT_LITERAL : DIGIT+}; {@code BIG_DECIMAL} carries no suffix) and that no STRING guard in
     * the corpus carries either (the parse-tree census {@code SwitchGuardLiteralKindCensusTest}:
     * 17,427 STRING guards, 0 long/float/decimal/exponent-shaped). With the arms deleted such a
     * value could only be what the front-end said it was — a STRING guard — and rendered as one
     * ({@code Objects.equals("1L", ..)}). RETIRED WITH THE RENDER at seat 12's R1: a STRING-kind guard
     * of any text reaches the same refusal; the four tests keep their names as the family's record.
     */
    @Test
    void switch_with_long_suffixed_string_guard_renders_as_string_literal() {
        assertRefusedAtSwitchTernaryStub(literalGuardSwitch("code", "1L", SwitchGuardLiteralKind.STRING), "LITERAL/default");
    }

    @Test
    void switch_with_float_suffixed_string_guard_renders_as_string_literal() {
        assertRefusedAtSwitchTernaryStub(literalGuardSwitch("code", "1.0f", SwitchGuardLiteralKind.STRING), "LITERAL/default");
    }

    @Test
    void switch_with_decimal_shaped_string_guard_renders_as_string_literal() {
        assertRefusedAtSwitchTernaryStub(literalGuardSwitch("code", "1.5", SwitchGuardLiteralKind.STRING), "LITERAL/default");
        assertRefusedAtSwitchTernaryStub(literalGuardSwitch("code", "3e2", SwitchGuardLiteralKind.STRING), "LITERAL/default");
    }

    /**
     * v3.1 C2d family 1, the Integer arm retired onto the front-end's kind channel: an int-shaped
     * value was boxed only when the lexer said INT; a STRING guard {@code "42"} rendered as a string, a
     * BOOLEAN guard as the Java literal, a kind-less hand-built guard as a string. RETIRED WITH THE RENDER at
     * seat 12's R1 (every kind reaches the refusal at this seat); the kind channel's live consumer is the
     * blessed ladders' {@code renderSwitchGuardMapper}.
     */
    @Test
    void switch_with_int_shaped_string_guard_renders_as_string_literal() {
        assertRefusedAtSwitchTernaryStub(literalGuardSwitch("code", "42", SwitchGuardLiteralKind.STRING), "LITERAL/default");
    }

    @Test
    void switch_with_boolean_guard_renders_the_java_literal() {
        assertRefusedAtSwitchTernaryStub(literalGuardSwitch("flag", "True", SwitchGuardLiteralKind.BOOLEAN), "LITERAL/default");
        assertRefusedAtSwitchTernaryStub(literalGuardSwitch("flag", "False", SwitchGuardLiteralKind.BOOLEAN), "LITERAL/default");
    }

    @Test
    void switch_with_decimal_guard_renders_boxed_double() {
        // the three lexer shapes of a BIG_DECIMAL guard (fraction, exponent, leading dot) - every one refuses here
        assertRefusedAtSwitchTernaryStub(literalGuardSwitch("d", "3.14", SwitchGuardLiteralKind.DECIMAL), "LITERAL/default");
        assertRefusedAtSwitchTernaryStub(literalGuardSwitch("d", "1e3", SwitchGuardLiteralKind.DECIMAL), "LITERAL/default");
        assertRefusedAtSwitchTernaryStub(literalGuardSwitch("d", ".5", SwitchGuardLiteralKind.DECIMAL), "LITERAL/default");
    }

    @Test
    void switch_with_leading_zero_int_guard_renders_the_decimal_integer() {
        // INT_LITERAL is DIGIT+: `007` once had to avoid the octal Java literal 007 - the boxed form is not emitted now
        assertRefusedAtSwitchTernaryStub(literalGuardSwitch("code", "007", SwitchGuardLiteralKind.INT), "LITERAL/default");
    }

    @Test
    void switch_with_string_guard_spelled_null_renders_the_quoted_string() {
        // absence is literalValue().isEmpty(); a STRING guard whose text is `null` is a string - and refuses like any
        assertRefusedAtSwitchTernaryStub(literalGuardSwitch("s", "null", SwitchGuardLiteralKind.STRING), "LITERAL/default");
    }

    @Test
    void switch_with_kindless_int_shaped_guard_renders_as_string_literal() {
        assertRefusedAtSwitchTernaryStub(literalGuardSwitch("code", "42"), "LITERAL/default");
    }

    /** {@code <subject> switch <literal> then 1, default 0} with a kind-less LITERAL guard carrying {@code value}. */
    private RSwitchExpr literalGuardSwitch(String subject, String value) {
        return literalGuardSwitch(subject, value, null);
    }

    private RSwitchExpr literalGuardSwitch(String subject, String value, SwitchGuardLiteralKind kind) {
        var expr = new RSwitchExpr();
        expr.setArgument(symbolRef(subject));
        var case1 = new RSwitchCase();
        case1.setDefault(false);
        var guard1 = new RSwitchCaseGuard();
        guard1.setKind(SwitchGuardKind.LITERAL);
        guard1.setLiteralValue(value);
        if (kind != null) {
            guard1.setLiteralKind(kind);
        }
        case1.setGuard(guard1);
        case1.setExpression(intLit(1));
        expr.cases().add(case1);
        var defaultCase = new RSwitchCase();
        defaultCase.setDefault(true);
        defaultCase.setExpression(intLit(0));
        expr.cases().add(defaultCase);
        return expr;
    }

    /** The pre-seat pin: a default case written FIRST was normalised to the ternary's tail. The kinds keep source order. */
    @Test
    void switch_default_before_nondefault_is_normalized_to_last() {
        var expr = new RSwitchExpr();
        expr.setArgument(symbolRef("status"));

        var defaultCase = new RSwitchCase();
        defaultCase.setDefault(true);
        defaultCase.setExpression(intLit(0));
        expr.cases().add(defaultCase);

        var case1 = new RSwitchCase();
        case1.setDefault(false);
        var guard1 = new RSwitchCaseGuard();
        guard1.setKind(SwitchGuardKind.LITERAL);
        guard1.setLiteralValue("A");
        case1.setGuard(guard1);
        case1.setExpression(intLit(1));
        expr.cases().add(case1);

        assertRefusedAtSwitchTernaryStub(expr, "default/LITERAL");
    }

    /** The pre-seat pin: a switch with no default case ended the ternary with `: null`. */
    @Test
    void switch_no_default_case_ends_with_null() {
        var expr = new RSwitchExpr();
        expr.setArgument(symbolRef("status"));

        var case1 = new RSwitchCase();
        case1.setDefault(false);
        var guard1 = new RSwitchCaseGuard();
        guard1.setKind(SwitchGuardKind.LITERAL);
        guard1.setLiteralValue("A");
        case1.setGuard(guard1);
        case1.setExpression(intLit(1));
        expr.cases().add(case1);

        assertRefusedAtSwitchTernaryStub(expr, "LITERAL");
    }

    /**
     * The compiler dispatches a switch to this handler. The pre-seat pin was the bare default (`MapperS.of(0)`) for a
     * default-only switch; R1 refuses it too (upstream renders the null-guarded ladder even then - D52), and the
     * refusal surfaces through the compiler's dispatch unchanged.
     */
    @Test
    void compiler_dispatches_switch() {
        var expr = new RSwitchExpr();
        expr.setArgument(symbolRef("x"));

        var defaultCase = new RSwitchCase();
        defaultCase.setDefault(true);
        defaultCase.setExpression(intLit(0));
        expr.cases().add(defaultCase);

        var scope  = ExpressionCompilerTest.createTestScope();
        var refusal = assertThrows(SilentDegradation.Refusal.class, () -> compiler.compile(expr, null, scope));
        assertEquals(SilentDegradation.Site.SWITCH_TERNARY_STUB, refusal.site());
        assertTrue(refusal.getMessage().contains("guards default;"), refusal.getMessage());
    }
}
