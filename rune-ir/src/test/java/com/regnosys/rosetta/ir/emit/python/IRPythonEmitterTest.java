package com.regnosys.rosetta.ir.emit.python;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.ast.enums.ArithOp;
import com.regnosys.rosetta.ast.enums.CompOp;
import com.regnosys.rosetta.ast.enums.ExistenceOp;
import com.regnosys.rosetta.ast.enums.ExistsModifier;
import com.regnosys.rosetta.ast.enums.ListOp;
import com.regnosys.rosetta.ast.enums.LogOp;
import com.regnosys.rosetta.ast.expressions.binary.RArithmeticExpr;
import com.regnosys.rosetta.ast.expressions.binary.RComparisonExpr;
import com.regnosys.rosetta.ast.expressions.binary.RLogicalExpr;
import com.regnosys.rosetta.ast.expressions.binary.RThenExpr;
import com.regnosys.rosetta.ast.expressions.constructors.RConditionalExpr;
import com.regnosys.rosetta.ast.expressions.literals.RBooleanLiteral;
import com.regnosys.rosetta.ast.expressions.literals.RIntLiteral;
import com.regnosys.rosetta.ast.expressions.literals.RNumberLiteral;
import com.regnosys.rosetta.ast.expressions.literals.RStringLiteral;
import com.regnosys.rosetta.ast.expressions.references.REmptyLiteral;
import com.regnosys.rosetta.ast.expressions.references.RFeatureCall;
import com.regnosys.rosetta.ast.expressions.references.RImplicitVariable;
import com.regnosys.rosetta.ast.expressions.references.RSymbolReference;
import com.regnosys.rosetta.ast.expressions.supporting.RInlineFunction;
import com.regnosys.rosetta.ast.expressions.unary.RCountExpr;
import com.regnosys.rosetta.ast.expressions.unary.RExistenceExpr;
import com.regnosys.rosetta.ast.expressions.unary.RListOpExpr;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.supporting.RCardinality;
import com.regnosys.rosetta.ir.expr.BinaryOp;
import com.regnosys.rosetta.ir.expr.Existence;
import com.regnosys.rosetta.ir.expr.FieldAccess;
import com.regnosys.rosetta.ir.expr.IRApply;
import com.regnosys.rosetta.ir.expr.IRConditional;
import com.regnosys.rosetta.ir.expr.IREmptyLiteral;
import com.regnosys.rosetta.ir.expr.IRExpr;
import com.regnosys.rosetta.ir.expr.IRExprKind;
import com.regnosys.rosetta.ir.expr.IRListConstruct;
import com.regnosys.rosetta.ir.expr.IRListOp;
import com.regnosys.rosetta.ir.expr.IRLiteral;
import com.regnosys.rosetta.ir.expr.IRReference;
import com.regnosys.rosetta.ir.expr.IRVariable;
import com.regnosys.rosetta.ir.expr.Let;
import com.regnosys.rosetta.ir.expr.NodeId;
import com.regnosys.rosetta.ir.expr.Optionality;
import com.regnosys.rosetta.ir.adapter.IREnumNode;
import com.regnosys.rosetta.ir.adapter.IREnumValueNode;
import com.regnosys.rosetta.ir.adapter.IRFieldNode;
import com.regnosys.rosetta.ir.adapter.IRMetadata;
import com.regnosys.rosetta.ir.adapter.IRTypeNode;
import com.regnosys.rosetta.ir.core.Cardinality;
import com.regnosys.rosetta.ir.core.IREnumValue;
import com.regnosys.rosetta.ir.core.IRField;
import com.regnosys.rosetta.ir.core.IRKind;
import com.regnosys.rosetta.ir.core.IRType;
import com.regnosys.rosetta.ir.emit.EmitterException;
import com.regnosys.rosetta.ir.expr.adapter.ExpressionToIRAdapter;
import com.regnosys.rosetta.symbols.RWorkspace;
import com.regnosys.rosetta.testutil.IRSamples;
import com.regnosys.rosetta.ast.types.REnumeration;
import com.regnosys.rosetta.types.ExpressionCardinality;
import com.regnosys.rosetta.types.REnumTypeRef;
import com.regnosys.rosetta.types.RMetaAnnotatedType;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Multi-target oracle for {@link IRPythonEmitter} (the active mainline, decision-log L-081) — proves the
 * language-neutral Phase-2 expression IR ({@code com.regnosys.rosetta.ir.expr}) drives a <em>second</em>
 * target (Python) end-to-end, with no Java-emitter dependency.
 *
 * <p>Current coverage: Wave-0 scalar literals ({@code int}/{@code number}/{@code string}/{@code boolean}) +
 * the {@code empty} absent leaf + a variable/parameter reference; the complete {@link BinaryOp} family
 * (comparison/equality/ordering/logical {@code and}/{@code or} + arithmetic {@code + - * /}, including
 * the OPTIONAL-operand declines for arithmetic and ordered comparison and the MULTI-operand decline for all
 * binary ops); the {@link Existence} tier (exists/is absent + single/multiple modifiers, keyed on operand
 * cardinality); the flat list-op tier ({@link IRListOp} — count/first/last/reverse/distinct); the full
 * {@link FieldAccess} navigation tier (single-result, list-nav, flat-map, flatten); the neutral
 * {@link Let} binding (then/Let multi-statement function body); and a function call ({@link IRApply}).
 * Everything else declines via {@link com.regnosys.rosetta.ir.emit.AbstractIRExprEmitter}'s choke.
 *
 * <p>Three oracles, none a Java byte-golden (none exists for a 2nd target):
 * <ol>
 *   <li><strong>Emission</strong> — the emitted Python source is exactly as expected (deterministic,
 *       always runs even when {@code python}/builtins are absent — this is where the function-shell text
 *       and the navigation forms ({@code _get}/{@code _get_all}/{@code _map}/{@code _flat_map}) are pinned);</li>
 *   <li><strong>Parse-and-run</strong> — the emitted Python, executed by a real interpreter, evaluates to
 *       a value matching the Rune semantics (runs when {@code python} is on the PATH). Headline examples:
 *       a chain with an absent intermediate hop propagates to {@code None} (absorbing-monoid monad), a
 *       flat-map drops per-element absents, and a {@code then}-chain runs to the composed value;</li>
 *   <li><strong>Same-tree</strong> — feeds the <em>actual adapter output</em> (the identical neutral tree
 *       the Java emitter consumes) to the Python emitter, including real adapter-produced navigations,
 *       arithmetic, existence, list-ops, and a then-chain run; skips when the sibling rune-dsl builtins
 *       are not on disk.</li>
 * </ol>
 */
class IRPythonEmitterTest {

    private final IRPythonEmitter emitter = new IRPythonEmitter();

    // ---- emission oracle (always runs) ----------------------------------------------------

    @Test
    void emitsIntLiteralAsPythonInt() {
        assertEquals("42", emitter.emit(intLit(42)));
        assertEquals("0", emitter.emit(intLit(0)));
    }

    @Test
    void emitsBeyondLongIntAsPythonInt() {
        // The Java target defers/hoists a `bigInteger` local for a value beyond Java long
        // (bitLength > 63); Python's int is unbounded, so the neutral IR emits it directly.
        assertEquals("9223372036854775808",
                emitter.emit(lit(IRLiteral.LiteralKind.INT, new BigInteger("9223372036854775808"))));
    }

    @Test
    void emitsNumberLiteralAsDecimal() {
        assertEquals("Decimal('1.5')", emitter.emit(lit(IRLiteral.LiteralKind.NUMBER, new BigDecimal("1.5"))));
        assertEquals("Decimal('100.00')", emitter.emit(lit(IRLiteral.LiteralKind.NUMBER, new BigDecimal("100.00"))));
    }

    @Test
    void emitsStringLiteralEscaped() {
        assertEquals("'hello'", emitter.emit(lit(IRLiteral.LiteralKind.STRING, "hello")));
        assertEquals("'it\\'s'", emitter.emit(lit(IRLiteral.LiteralKind.STRING, "it's")));
        assertEquals("'a\\\\b'", emitter.emit(lit(IRLiteral.LiteralKind.STRING, "a\\b")));
    }

    @Test
    void emitsStringLiteralEscapingControlChars() {
        // The whitespace-control escape branches, pinned deterministically (always runs) — a raw newline/tab in
        // a single-quoted Python literal is a SyntaxError, and a low control char must hex-escape (F4).
        assertEquals("'a\\nb\\tc'", emitter.emit(lit(IRLiteral.LiteralKind.STRING, "a\nb\tc")));
        // carriage return / backspace / form-feed escape to their named forms
        assertEquals("'\\r\\b\\f'", emitter.emit(lit(IRLiteral.LiteralKind.STRING, "\r\b\f")));
        // a low control char (0x01) → \x01 via the default branch's c < 0x20 hex path
        assertEquals("'x\\x01y'", emitter.emit(lit(IRLiteral.LiteralKind.STRING, "x" + (char) 1 + "y")));
    }

    @Test
    void emitsBooleanLiteral() {
        assertEquals("True", emitter.emit(lit(IRLiteral.LiteralKind.BOOLEAN, Boolean.TRUE)));
        assertEquals("False", emitter.emit(lit(IRLiteral.LiteralKind.BOOLEAN, Boolean.FALSE)));
    }

    @Test
    void emitsEmptyAsNone() {
        // The forced monad-identity decision (the scoping audit's biggest risk): absent -> None.
        assertEquals("None", emitter.emit(empty()));
    }

    @Test
    void emitsVariableAsName() {
        // A parameter/variable reference lowers to its bare Python name (the recursion building block
        // for a real adapter-produced comparison, which is never two literals).
        assertEquals("n", emitter.emit(var("n")));
    }

    @Test
    void emitsComparisonOfTwoLiterals() {
        // NOTE ON PROVENANCE: these comparison trees are SYNTHETIC. The adapter declines a
        // literal-vs-literal comparison (no resolved sibling to type the literal), so this shape is
        // never produced by ExpressionToIRAdapter. This test proves the emitter's switch dispatch +
        // children() recursion in isolation; the *real adapter-produced* comparison proof (the same
        // tree the Java path consumes) is emitsActualAdapterOutput below.
        assertEquals("(5 == 3)", emitter.emit(cmp(BinaryOp.BinOp.EQ, intLit(5), intLit(3))));
        assertEquals("(7 != 7)", emitter.emit(cmp(BinaryOp.BinOp.NEQ, intLit(7), intLit(7))));
        assertEquals("(3 < 5)", emitter.emit(cmp(BinaryOp.BinOp.LT, intLit(3), intLit(5))));
        assertEquals("(5 > 3)", emitter.emit(cmp(BinaryOp.BinOp.GT, intLit(5), intLit(3))));
        assertEquals("(2 <= 2)", emitter.emit(cmp(BinaryOp.BinOp.LTE, intLit(2), intLit(2))));
        assertEquals("(9 >= 2)", emitter.emit(cmp(BinaryOp.BinOp.GTE, intLit(9), intLit(2))));
    }

    @Test
    void declinesArithmeticOverUnsupportedOperand() {
        // The whole BinaryOp OPERATOR family now emits, so the decline is operand-driven: an arithmetic node
        // over a not-yet-lowerable operand (here a list construct — in the real corpus, an alias/call) declines
        // because the operand's emit throws and the parent propagates it.
        IRExpr listOperand = new IRListConstruct(List.of(), NodeId.ROOT, RMetaAnnotatedType.MISSING,
                ExpressionCardinality.MULTI, Optionality.PRESENT, SourceRange.NONE);
        assertThrows(EmitterException.class,
                () -> emitter.emit(cmp(BinaryOp.BinOp.ADD, listOperand, intLit(1))));
    }

    @Test
    void declinesOutOfSliceKind() {
        IRExpr listConstruct = new IRListConstruct(List.of(), NodeId.ROOT, RMetaAnnotatedType.MISSING,
                ExpressionCardinality.MULTI, Optionality.PRESENT, SourceRange.NONE);
        assertThrows(EmitterException.class, () -> emitter.emit(listConstruct));
    }

    @Test
    void declinesBinaryOpWithMultiOperand() {
        // J2 (round-4 defensive guard): a MULTI-cardinality operand on ANY binary op declines fail-closed.
        // The adapter currently never produces such a tree (the all/any modifier is deferred), so this guard
        // is purely defensive — a future all/any admission cannot silently emit `(list > scalar)`.
        // Construct the MULTI node directly (the adapter won't produce one for binary operators).
        IRVariable multi = multiVar("xs");
        for (BinaryOp.BinOp op : BinaryOp.BinOp.values()) {
            final BinaryOp.BinOp o = op;
            assertThrows(EmitterException.class, () -> emitter.emit(cmp(o, multi, var("b"))),
                    op + " with a MULTI left operand must decline");
            assertThrows(EmitterException.class, () -> emitter.emit(cmp(o, var("a"), multi)),
                    op + " with a MULTI right operand must decline");
        }
        // GUARD: existing SINGLE-operand binary ops are NOT affected (the guard is MULTI-specific).
        // An optional (SINGLE cardinality, OPTIONAL optionality) comparison still emits for EQ/NEQ.
        assertEquals("(x == b)", emitter.emit(cmp(BinaryOp.BinOp.EQ, optionalVar("x"), var("b"))));
    }

    // ---- boolean condition tier (logical + existence): emission (always runs) --------------

    @Test
    void emitsLogicalAndOr() {
        // and/or over two comparison operands → fully parenthesized Python and/or.
        assertEquals("((a >= b) and (c < d))", emitter.emit(cmp(BinaryOp.BinOp.AND,
                cmp(BinaryOp.BinOp.GTE, var("a"), var("b")), cmp(BinaryOp.BinOp.LT, var("c"), var("d")))));
        assertEquals("((x == y) or (p != q))", emitter.emit(cmp(BinaryOp.BinOp.OR,
                cmp(BinaryOp.BinOp.EQ, var("x"), var("y")), cmp(BinaryOp.BinOp.NEQ, var("p"), var("q")))));
        // nested: parenthesization makes precedence unambiguous.
        assertEquals("(((a > b) and (c > d)) or (e == f))", emitter.emit(cmp(BinaryOp.BinOp.OR,
                cmp(BinaryOp.BinOp.AND, cmp(BinaryOp.BinOp.GT, var("a"), var("b")),
                        cmp(BinaryOp.BinOp.GT, var("c"), var("d"))),
                cmp(BinaryOp.BinOp.EQ, var("e"), var("f")))));
    }

    @Test
    void emitsExistenceScalar() {
        // a scalar operand is None for absent (L-082), so exists/is absent are None comparisons.
        assertEquals("(foo is not None)", emitter.emit(exists(Existence.ExistOp.EXISTS, var("foo"))));
        assertEquals("(bar is None)", emitter.emit(exists(Existence.ExistOp.ABSENT, var("bar"))));
    }

    @Test
    void emitsExistenceMulti() {
        // a multi operand emits a Python list (present elements), so exists/is absent are length tests.
        assertEquals("(len(legs) > 0)", emitter.emit(exists(Existence.ExistOp.EXISTS, multiVar("legs"))));
        assertEquals("(len(legs) == 0)", emitter.emit(exists(Existence.ExistOp.ABSENT, multiVar("legs"))));
    }

    @Test
    void emitsExistenceCardinalityModifiers() {
        // `single`/`multiple` exists (resultCount == 1 / > 1), keyed on operand cardinality (L-089).
        // (This was a DECLINE pre-L-089 — declinesExistenceCardinalityModifier; the modifier now emits.)
        // MULTI operand → a length test.
        assertEquals("(len(legs) == 1)",
                emitter.emit(existsMod(Existence.ExistOp.EXISTS, Existence.ExistMod.SINGLE, multiVar("legs"))));
        assertEquals("(len(legs) > 1)",
                emitter.emit(existsMod(Existence.ExistOp.EXISTS, Existence.ExistMod.MULTIPLE, multiVar("legs"))));
        // SCALAR operand → the cardinality-fact constant fold: single ⟺ present, multiple ⟺ structurally False.
        assertEquals("(foo is not None)",
                emitter.emit(existsMod(Existence.ExistOp.EXISTS, Existence.ExistMod.SINGLE, var("foo"))));
        assertEquals("False",
                emitter.emit(existsMod(Existence.ExistOp.EXISTS, Existence.ExistMod.MULTIPLE, var("foo"))));
        // `is absent` IGNORES any modifier (legacy notExists; no single/multiple "is absent") — dead-but-safe.
        assertEquals("(len(legs) == 0)",
                emitter.emit(existsMod(Existence.ExistOp.ABSENT, Existence.ExistMod.SINGLE, multiVar("legs"))));
        assertEquals("(bar is None)",
                emitter.emit(existsMod(Existence.ExistOp.ABSENT, Existence.ExistMod.MULTIPLE, var("bar"))));
    }

    // ---- list-op tier (count/first/last/reverse/distinct): emission (always runs) ----------

    @Test
    void emitsListOps() {
        // The flat list-op tier (L-090) — each over a present-only list (the list-monad).
        assertEquals("len(xs)", emitter.emit(listOp(IRListOp.Kind.COUNT, multiVar("xs"))));
        assertEquals("_first(xs)", emitter.emit(listOp(IRListOp.Kind.FIRST, multiVar("xs"))));
        assertEquals("_last(xs)", emitter.emit(listOp(IRListOp.Kind.LAST, multiVar("xs"))));
        assertEquals("list(reversed(xs))", emitter.emit(listOp(IRListOp.Kind.REVERSE, multiVar("xs"))));
        assertEquals("_distinct(xs)", emitter.emit(listOp(IRListOp.Kind.DISTINCT, multiVar("xs"))));
        // COUNT over a SINGLE/optional child is resultCount() ∈ {0,1}, NOT len(scalar) (a TypeError): it folds
        // on the child's neutral cardinality, mirroring the existence tier (F1). The MULTI form stays len(...).
        assertEquals("(0 if foo is None else 1)", emitter.emit(listOp(IRListOp.Kind.COUNT, var("foo"))));
        // count composes in a comparison (the `xs count >= 1` cardinality-check shape the adapter admits).
        // COUNT is a SCALAR result (SINGLE cardinality) in the real adapter; construct it with SINGLE here
        // so the J2 defensive MULTI guard in emitBinaryOp does not falsely fire on a synthetic test node.
        IRListOp countSingle = new IRListOp(IRListOp.Kind.COUNT, multiVar("xs"), NodeId.ROOT,
                RMetaAnnotatedType.MISSING, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        assertEquals("(len(xs) >= 1)", emitter.emit(cmp(BinaryOp.BinOp.GTE, countSingle, intLit(1))));
        // a count over a real-shaped nav receiver composes by recursion through the child.
        FieldAccess legs = new FieldAccess(var("trade"), "legs", NodeId.ROOT, RMetaAnnotatedType.MISSING,
                ExpressionCardinality.MULTI, ExpressionCardinality.MULTI, Optionality.PRESENT, SourceRange.NONE);
        assertEquals("len(_get_all(trade, 'legs'))", emitter.emit(listOp(IRListOp.Kind.COUNT, legs)));
    }

    @Test
    void declinesFlattenListOp() {
        // FLATTEN (a list-of-lists flatten, MapperListOfLists.flattenList) is a follow-up — decline.
        assertThrows(EmitterException.class,
                () -> emitter.emit(listOp(IRListOp.Kind.FLATTEN, multiVar("xs"))));
    }

    @Test
    void listOpHelpersArePinned() {
        // Pin the three list-op helpers the emission assumes, deterministically (independent of python).
        assertEquals("def _first(xs):\n    return xs[0] if xs else None", IRPythonEmitter.FIRST_HELPER);
        assertEquals("def _last(xs):\n    return xs[-1] if xs else None", IRPythonEmitter.LAST_HELPER);
        assertEquals("def _distinct(xs):\n    out = []\n    for x in (xs or []):\n"
                + "        if x not in out:\n            out.append(x)\n    return out", IRPythonEmitter.DISTINCT_HELPER);
    }

    // ---- APPLY (function call): emission (always runs) -------------------------------------

    @Test
    void emitsFunctionCall() {
        // <callee>(<args>) — the callee name snake_cased (step-11), no Mapper wrap (L-091).
        assertEquals("foo(a, b)", emitter.emit(apply("Foo", var("a"), var("b"))));
        // a mixed param/literal arg
        assertEquals("day_count(p, 365)", emitter.emit(apply("DayCount", var("p"), intLit(365))));
        // a no-arg call
        assertEquals("now()", emitter.emit(apply("Now")));
        // the call composes as an operand (recurses through children()).
        assertEquals("(foo(a, b) + c)",
                emitter.emit(cmp(BinaryOp.BinOp.ADD, apply("Foo", var("a"), var("b")), var("c"))));
    }

    @Test
    void declinesCallWithReferenceArg() {
        // ALIAS call arg: still declines (no ALIAS arm).
        assertThrows(EmitterException.class,
                () -> emitter.emit(apply("Foo", new IRReference("alias", IRReference.ReferenceKind.ALIAS,
                        NodeId.ROOT, RMetaAnnotatedType.MISSING, ExpressionCardinality.SINGLE, Optionality.PRESENT,
                        SourceRange.NONE))));
        // ENUM_VALUE arg with a non-enum (MISSING) type: declines via the defensive type-guard (NOT "no arm" —
        // a GENUINE enum-typed arg now LOWERS; see lowersEnumValueCallArgument).
        assertThrows(EmitterException.class,
                () -> emitter.emit(apply("Foo", new IRReference("VALUE", IRReference.ReferenceKind.ENUM_VALUE,
                        NodeId.ROOT, RMetaAnnotatedType.MISSING, ExpressionCardinality.SINGLE, Optionality.PRESENT,
                        SourceRange.NONE))));
    }

    @Test
    void declinesApplyWithNonReferenceCallee() {
        // G7: the emitApply instanceof-guard (F8). The adaptApply invariant guarantees an IRReference callee, so
        // a non-IRReference callee is corpus-impossible — but a malformed tree must fail through the emitter's
        // own choke point (EmitterException with a clear message), NOT a raw ClassCastException. Here the callee
        // is an IRVariable rather than an IRReference.
        IRApply badCallee = new IRApply(var("x"), List.of(), NodeId.ROOT, RMetaAnnotatedType.MISSING,
                ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        assertThrows(EmitterException.class, () -> emitter.emit(badCallee));
    }

    // ---- arithmetic (+ - * /): emission (always runs) -------------------------------------

    @Test
    void emitsArithmeticAddSubMul() {
        // + - * render as fully-parenthesized infix operators (exact under the MAX_PREC module context).
        assertEquals("(a + b)", emitter.emit(cmp(BinaryOp.BinOp.ADD, var("a"), var("b"))));
        assertEquals("(a - b)", emitter.emit(cmp(BinaryOp.BinOp.SUB, var("a"), var("b"))));
        assertEquals("(a * b)", emitter.emit(cmp(BinaryOp.BinOp.MUL, var("a"), var("b"))));
    }

    @Test
    void emitsArithmeticDivAsHelper() {
        // / lowers to the _div helper (decimal division at DECIMAL128), NOT a bare Python / (which would be
        // float division for ints and would not terminate under the MAX_PREC module context).
        assertEquals("_div(a, b)", emitter.emit(cmp(BinaryOp.BinOp.DIV, var("a"), var("b"))));
        // the compound day-count shape over params: (p1 / 365) + (p2 / 366)
        BinaryOp compound = cmp(BinaryOp.BinOp.ADD,
                cmp(BinaryOp.BinOp.DIV, var("p1"), lit(IRLiteral.LiteralKind.INT, BigInteger.valueOf(365))),
                cmp(BinaryOp.BinOp.DIV, var("p2"), lit(IRLiteral.LiteralKind.INT, BigInteger.valueOf(366))));
        assertEquals("(_div(p1, 365) + _div(p2, 366))", emitter.emit(compound));
    }

    @Test
    void divHelperIsDecimal128Division() {
        // Pin the division helper, deterministically (the DECIMAL128 = prec-34 HALF_EVEN context).
        assertEquals("from decimal import Context, ROUND_HALF_EVEN\n"
                        + "_DIV_CTX = Context(prec=34, rounding=ROUND_HALF_EVEN)  # java.math.MathContext.DECIMAL128\n"
                        + "def _div(a, b):\n    return _DIV_CTX.divide(Decimal(a), Decimal(b))",
                IRPythonEmitter.DIV_HELPER);
    }

    @Test
    void declinesArithmeticOverOptionalOperand() {
        // FAITHFULNESS (G1): Rune's MapperMaths absorbs an absent operand to absent (never errors), but the
        // emitted bare `+ - *` / `_div` would raise a TypeError on a None operand. We do NOT reproduce the
        // absent-absorption (no propagating helpers) — an OPTIONAL arithmetic operand DECLINES fail-closed (the
        // FLATTEN stance). Present-operand arithmetic (the day-count tests above) is unaffected: it reproduces
        // the VALUE only. The adapter admits an OPTIONAL scalar param as an arithmetic operand, so this is a
        // real (corpus-reachable) decline, not a synthetic one.
        IRVariable optional = optionalVar("x");
        for (BinaryOp.BinOp op : List.of(BinaryOp.BinOp.ADD, BinaryOp.BinOp.SUB, BinaryOp.BinOp.MUL, BinaryOp.BinOp.DIV)) {
            assertThrows(EmitterException.class, () -> emitter.emit(cmp(op, optional, var("b"))),
                    op + " with an OPTIONAL left operand must decline");
            assertThrows(EmitterException.class, () -> emitter.emit(cmp(op, var("a"), optional)),
                    op + " with an OPTIONAL right operand must decline");
        }
        // A comparison / equality / logical op over the SAME optional operand is UNCHANGED (still emits) — the
        // decline is scoped to arithmetic, where MapperMaths' absent-absorption (not reproduced) is the gap.
        assertEquals("(x == b)", emitter.emit(cmp(BinaryOp.BinOp.EQ, optional, var("b"))));
        assertEquals("(x and b)", emitter.emit(cmp(BinaryOp.BinOp.AND, optional, var("b"))));
    }

    @Test
    void declinesOrderedComparisonOverOptionalOperand() {
        // H1 (round-3): Rune folds an absent ordered-comparison operand into a total ComparisonResult (no crash),
        // but Python `None < 5` / `None > 5` / `None <= 5` / `None >= 5` raises TypeError. The fix extends the
        // existing arithmetic fail-closed guard to the four ordered operators (LT/GT/LTE/GTE). EQ/NEQ are None-safe
        // in Python (`None == 5` is False, `None != 5` is True) and remain unaffected — do NOT decline those.
        // AND/OR are likewise unaffected (total-boolean operands per the adapter invariant).
        //
        // This test is RED before the fix: the ordered operators fall through to bare infix and emit, not decline.
        IRVariable optional = optionalVar("x");
        for (BinaryOp.BinOp op : List.of(BinaryOp.BinOp.LT, BinaryOp.BinOp.GT, BinaryOp.BinOp.LTE, BinaryOp.BinOp.GTE)) {
            assertThrows(EmitterException.class, () -> emitter.emit(cmp(op, optional, var("b"))),
                    op + " with an OPTIONAL left operand must decline");
            assertThrows(EmitterException.class, () -> emitter.emit(cmp(op, var("a"), optional)),
                    op + " with an OPTIONAL right operand must decline");
        }
        // EQ/NEQ over an OPTIONAL operand remain unchanged (None-safe in Python) — must still emit.
        assertEquals("(x == b)", emitter.emit(cmp(BinaryOp.BinOp.EQ, optional, var("b"))));
        assertEquals("(x != b)", emitter.emit(cmp(BinaryOp.BinOp.NEQ, optional, var("b"))));
        // AND/OR also remain unchanged.
        assertEquals("(x and b)", emitter.emit(cmp(BinaryOp.BinOp.AND, optional, var("b"))));
        assertEquals("(x or b)", emitter.emit(cmp(BinaryOp.BinOp.OR, optional, var("b"))));
    }

    // ---- navigation + function shell: emission (always runs) -------------------------------

    @Test
    void emitsSingleHopNavigation() {
        // `observation -> price` → None-propagating attribute access (the Rune feature name verbatim).
        assertEquals("_get(observation, 'price')",
                emitter.emit(fieldAccess(var("observation"), "price")));
    }

    @Test
    void emitsNavigationChainByRecursion() {
        // `a -> b -> c` composes by recursion through children(): _get(_get(a, 'b'), 'c').
        FieldAccess chain = fieldAccess(fieldAccess(var("a"), "b"), "c");
        assertEquals("_get(_get(a, 'b'), 'c')", emitter.emit(chain));
    }

    @Test
    void emitsRunnableFunctionShellExactText() {
        // The function-shell text is pinned EXACTLY in an always-runs assert (no python guard) so a
        // shell / indentation / _get-name regression is caught deterministically even in a bare CI.
        assertEquals("def my_func(observation):\n    return _get(observation, 'price')",
                emitter.emitFunction("MyFunc", List.of("observation"),
                        fieldAccess(var("observation"), "price")));
        // a multi-parameter shell over a comparison body
        assertEquals("def g(x, y):\n    return (x >= y)",
                emitter.emitFunction("g", List.of("x", "y"),
                        cmp(BinaryOp.BinOp.GTE, var("x"), var("y"))));
    }

    @Test
    void navHelperIsTheNonePropagatingGetter() {
        // Pin the helper the navigation emission assumes, deterministically (independent of python).
        assertEquals("def _get(o, a):\n    return None if o is None else getattr(o, a, None)",
                IRPythonEmitter.NAV_HELPER);
    }

    @Test
    void listNavHelperIsTheEmptyListCollapse() {
        // Pin the list-nav helper (absent list → []), deterministically. The `is not None` guard (not
        // truthiness) preserves a present empty list as [] rather than re-defaulting it.
        assertEquals("def _get_all(o, a):\n    xs = None if o is None else getattr(o, a, None)\n"
                        + "    return xs if xs is not None else []",
                IRPythonEmitter.LIST_NAV_HELPER);
    }

    @Test
    void emitsMultiFeatureOffSingleReceiver() {
        // `trade -> legs` (legs multi, trade single): a MULTI feature off a SINGLE receiver → list nav.
        // The accumulated cardinality is MULTI, the receiver is SINGLE, so the gate routes to _get_all.
        FieldAccess multiFeature = new FieldAccess(var("trade"), "legs", NodeId.ROOT,
                RMetaAnnotatedType.MISSING, ExpressionCardinality.MULTI, ExpressionCardinality.MULTI,
                Optionality.PRESENT, SourceRange.NONE);
        assertEquals("_get_all(trade, 'legs')", emitter.emit(multiFeature));

        // `a -> b -> legs`: a single-receiver chain ending in a multi feature composes by recursion — the
        // SINGLE receiver `a -> b` emits via the scalar _get path, the final multi hop wraps it in _get_all.
        FieldAccess chainToMulti = new FieldAccess(fieldAccess(var("a"), "b"), "legs", NodeId.ROOT,
                RMetaAnnotatedType.MISSING, ExpressionCardinality.MULTI, ExpressionCardinality.MULTI,
                Optionality.PRESENT, SourceRange.NONE);
        assertEquals("_get_all(_get(a, 'b'), 'legs')", emitter.emit(chainToMulti));
    }

    @Test
    void mapNavHelperDropsPerElementAbsents() {
        // Pin the flat-map helper, deterministically. It maps each element via _get and drops the Nones —
        // Rune's runtime list semantic (MapperC error-items are excluded from getMulti()).
        assertEquals("def _map(xs, a):\n    return [v for v in (_get(x, a) for x in (xs or [])) if v is not None]",
                IRPythonEmitter.MAP_NAV_HELPER);
    }

    @Test
    void emitsFlatMapThroughMultiReceiver() {
        // `legs -> rate` (legs a MULTI param base, rate single): a SINGLE feature off a MULTI receiver — a
        // flat-map. The receiver (a MULTI IRVariable) emits as the bare param; featureCardinality SINGLE
        // routes to _map (vs _flat_map for a multi feature). (This was a DECLINE in L-083; this slice emits it.)
        IRVariable multiBase = new IRVariable("legs", IRVariable.VariableKind.PARAM, NodeId.ROOT,
                RMetaAnnotatedType.MISSING, ExpressionCardinality.MULTI, Optionality.PRESENT, SourceRange.NONE);
        FieldAccess flatMap = new FieldAccess(multiBase, "rate", NodeId.ROOT, RMetaAnnotatedType.MISSING,
                ExpressionCardinality.MULTI, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        assertEquals("_map(legs, 'rate')", emitter.emit(flatMap));

        // `trade -> legs -> rate`: a single feature off a multi receiver that is itself `trade -> legs`
        // (a multi feature off a single receiver → _get_all). The flat-map wraps the list-nav receiver.
        FieldAccess legs = new FieldAccess(var("trade"), "legs", NodeId.ROOT, RMetaAnnotatedType.MISSING,
                ExpressionCardinality.MULTI, ExpressionCardinality.MULTI, Optionality.PRESENT, SourceRange.NONE);
        FieldAccess chain = new FieldAccess(legs, "rate", NodeId.ROOT, RMetaAnnotatedType.MISSING,
                ExpressionCardinality.MULTI, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        assertEquals("_map(_get_all(trade, 'legs'), 'rate')", emitter.emit(chain));
    }

    @Test
    void flattenNavHelperConcatenatesAndDropsNulls() {
        // Pin the flatten helper, deterministically. It maps each element to its feature list (via _get_all)
        // and concatenates, dropping null elements — Rune's mapC semantic (getMapperItems error-items).
        assertEquals("def _flat_map(xs, a):\n    return [v for x in (xs or []) for v in _get_all(x, a) if v is not None]",
                IRPythonEmitter.FLATTEN_NAV_HELPER);
    }

    @Test
    void emitsFlattenMultiFeatureOffMultiReceiver() {
        // A MULTI feature off a MULTI receiver (`legs -> counterparties`, both multi): a flatten —
        // concatenate per-element lists. featureCardinality MULTI is the discriminator from the
        // single-feature flat-map (`_map`, featureCardinality SINGLE). (This was a DECLINE in L-084.)
        IRVariable multiBase = new IRVariable("legs", IRVariable.VariableKind.PARAM, NodeId.ROOT,
                RMetaAnnotatedType.MISSING, ExpressionCardinality.MULTI, Optionality.PRESENT, SourceRange.NONE);
        FieldAccess flatten = new FieldAccess(multiBase, "counterparties", NodeId.ROOT, RMetaAnnotatedType.MISSING,
                ExpressionCardinality.MULTI, ExpressionCardinality.MULTI, Optionality.PRESENT, SourceRange.NONE);
        assertEquals("_flat_map(legs, 'counterparties')", emitter.emit(flatten));

        // `trade -> legs -> counterparties` (trade single, legs+counterparties multi): the flatten wraps the
        // list-nav receiver `trade -> legs` (a multi feature off a single receiver → _get_all).
        FieldAccess legs = new FieldAccess(var("trade"), "legs", NodeId.ROOT, RMetaAnnotatedType.MISSING,
                ExpressionCardinality.MULTI, ExpressionCardinality.MULTI, Optionality.PRESENT, SourceRange.NONE);
        FieldAccess chain = new FieldAccess(legs, "counterparties", NodeId.ROOT, RMetaAnnotatedType.MISSING,
                ExpressionCardinality.MULTI, ExpressionCardinality.MULTI, Optionality.PRESENT, SourceRange.NONE);
        assertEquals("_flat_map(_get_all(trade, 'legs'), 'counterparties')", emitter.emit(chain));
    }

    // ---- then/Let (multi-statement body): emission (always runs) ---------------------------

    @Test
    void emitsLetAsMultiStatementBody() {
        // A single `Let` body flattens to ONE `binder = value` statement + a final return — the
        // multi-statement function body (L-088). `let _then0 = (a + b) in _then0`.
        Let let = letNode("_then0", cmp(BinaryOp.BinOp.ADD, var("a"), var("b")), letBinder("_then0"));
        assertEquals("def my_func(a, b):\n    _then0 = (a + b)\n    return _then0",
                emitter.emitFunction("MyFunc", List.of("a", "b"), let));
    }

    @Test
    void emitsTransformingLetChainAsOrderedStatements() {
        // A SYNTHETIC value-is-Let chain with TRANSFORMING bodies — the shape the real then-desugar produces
        // (`Let{value=Let, in=expr}`) but with continuations that reference the binder (the live adapter
        // declines an item-transforming then-body, so this path is synthetic, like emitsComparisonOfTwoLiterals).
        // `let x0 = (a * 2) in (let x1 = (x0 + 1) in (x1 * 10))` — the OUTER value is the inner Let.
        Let inner = letNode("x0", cmp(BinaryOp.BinOp.MUL, var("a"), intLit(2)),
                cmp(BinaryOp.BinOp.ADD, letBinder("x0"), intLit(1)));
        Let outer = letNode("x1", inner, cmp(BinaryOp.BinOp.MUL, letBinder("x1"), intLit(10)));
        // The exact ORDERED two-binding text is the load-bearing assert (a run value alone can't distinguish a
        // flattened chain from a collapsed one); the binder references in BOTH inner and outer `in` are pinned.
        assertEquals("def my_func(a):\n    x0 = (a * 2)\n    x1 = (x0 + 1)\n    return (x1 * 10)",
                emitter.emitFunction("MyFunc", List.of("a"), outer));
    }

    @Test
    void declinesLetInOperandPosition() {
        // A `Let` is a statement construct — it lowers ONLY in the function-body path (flattenLet), never as a
        // nested expression operand (a walrus `:=` is a follow-up). emit(LET) declines via the base `unsupported` choke (`EmitterException`).
        Let let = letNode("x", intLit(1), letBinder("x"));
        assertThrows(EmitterException.class, () -> emitter.emit(let));
    }

    // ---- conditional (if-then-else, step-12 task-2): emission (always runs) ----------------

    @Test
    void emitsConditionalWithElseBranch() {
        // `if c then a else b`, both branches SINGLE, genuine else — the base D6 ternary shape.
        IRConditional cond = conditional(var("c"), var("a"), var("b"),
                ExpressionCardinality.SINGLE, Optionality.PRESENT);
        assertEquals("(a if c else b)", emitter.emit(cond));
    }

    @Test
    void emitsNoElseSingleConditionalWithNoneAbsent() {
        // `if c then a` (else omitted, SINGLE) — D7: the no-else absent value is None for a SINGLE conditional.
        IRConditional cond = conditional(var("c"), var("a"), null,
                ExpressionCardinality.SINGLE, Optionality.OPTIONAL);
        assertEquals("(a if c else None)", emitter.emit(cond));
    }

    @Test
    void emitsNoElseMultiConditionalWithEmptyListAbsent() {
        // `if c then xs` (else omitted, MULTI then-branch) — D7: the no-else absent value is [] for a MULTI
        // conditional (keyed on node.cardinality(), the adapter's accurate branch-join — D4), matching the
        // emitter's established list-monad convention (LIST_NAV_HELPER: absent list -> [], not None).
        IRConditional cond = conditional(var("c"), multiVar("xs"), null,
                ExpressionCardinality.MULTI, Optionality.OPTIONAL);
        assertEquals("(xs if c else [])", emitter.emit(cond));
    }

    @Test
    void declinesConditionalWithMultiCondition() {
        // D8 (defensive): a MULTI-cardinality condition declines — unreachable via the adapter today
        // (ExpressionValidator.checkConditionIsSingle enforces a single condition) but guarded fail-closed,
        // mirroring emitBinaryOp's MULTI-operand guard. Branches are SINGLE (so D10's mismatch check is a
        // no-op), isolating the D8 guard so the decline is unambiguously attributable to it.
        IRConditional cond = conditional(multiVar("cs"), var("a"), var("b"),
                ExpressionCardinality.SINGLE, Optionality.PRESENT);
        assertThrows(EmitterException.class, () -> emitter.emit(cond));
    }

    @Test
    void declinesMixedCardinalityGenuineElseConditional() {
        // D10: a genuine else whose branches disagree in cardinality (then SINGLE, else MULTI) declines
        // fail-closed. The condition is deliberately SINGLE (so D8 is a no-op), isolating the D10 guard —
        // D8 is checked first, but a SINGLE condition passes it, so this mixed-cardinality node reaches D10.
        IRConditional cond = conditional(var("c"), var("a"), multiVar("xs"),
                ExpressionCardinality.MULTI, Optionality.PRESENT);
        assertThrows(EmitterException.class, () -> emitter.emit(cond));
    }

    @Test
    void emitsBothMultiGenuineElseConditional() {
        // D10 allow-path: a genuine else whose branches AGREE (both MULTI) emits normally — the guard is
        // about a cardinality MISMATCH, not about MULTI branches per se.
        IRConditional cond = conditional(var("c"), multiVar("xs"), multiVar("ys"),
                ExpressionCardinality.MULTI, Optionality.PRESENT);
        assertEquals("(xs if c else ys)", emitter.emit(cond));
    }

    @Test
    void emitsNestedElseIfConditional() {
        // `if c1 then a else (if c2 then b else d)` — a nested else-if: the else child is itself an
        // IRConditional, recursed through emit() exactly like any other child.
        IRConditional inner = conditional(var("c2"), var("b"), var("d"),
                ExpressionCardinality.SINGLE, Optionality.PRESENT);
        IRConditional outer = conditional(var("c1"), var("a"), inner,
                ExpressionCardinality.SINGLE, Optionality.PRESENT);
        assertEquals("(a if c1 else (b if c2 else d))", emitter.emit(outer));
    }

    @Test
    void emitsIRSamplesConditionalAsTernary() {
        // The flip: IRSamples.expr(CONDITIONAL) (else-less, SINGLE, condition=then=var "x") used to decline
        // (the base AbstractIRExprEmitter unsupported choke, pre-step-12-task-2); it now emits via the D7
        // no-else/SINGLE path. This was the ONLY test asserting a CONDITIONAL decline (confirmed by grepping
        // CONDITIONAL across the test tree), so the flip is total, not partial.
        assertEquals("(x if x else None)", emitter.emit(IRSamples.expr(IRExprKind.CONDITIONAL)));
    }

    @Test
    void declinesToString() {
        assertThrows(EmitterException.class,
                () -> emitter.emit(IRSamples.expr(IRExprKind.TO_STRING)));
    }

    // ── keyword sanitization (step-8) ─────────────────────────────────────────────
    @Test void sanitizesKeywordNavFeature() {
        assertEquals("_get(o, 'global_')", emitter.emit(fieldAccess(var("o"), "global")));
    }
    @Test void nonKeywordNavFeatureUnchanged() {
        assertEquals("_get(o, 'price')", emitter.emit(fieldAccess(var("o"), "price")));
    }
    @Test void sanitizesKeywordVariable() {
        assertEquals("global_", emitter.emit(var("global")));
    }
    @Test void sanitizesKeywordCallee() {
        assertEquals("global_(a)", emitter.emit(apply("global", var("a"))));
    }
    @Test void nonKeywordCalleeSnakeCased() {
        // Pre-step-11 this callee was emitted verbatim ("unchanged"); it now snake_cases like any other
        // value-level identifier — the contrast with sanitizesKeywordCallee above is keyword-suffix vs.
        // case-conversion, not "changes" vs. "unchanged".
        assertEquals("foo(a, b)", emitter.emit(apply("Foo", var("a"), var("b"))));
    }
    @Test void sanitizesKeywordFunctionNameAndParams() {
        // function name + param def + the param's body ref all mangle identically (def == use).
        assertEquals("def global_(class_):\n    return class_",
                emitter.emitFunction("global", java.util.List.of("class"), var("class")));
    }
    @Test void sanitizesKeywordLetBinder() {
        // a then/let binder named `global` and its continuation reference both mangle to global_.
        assertEquals("def f():\n    global_ = (a + b)\n    return global_",
                emitter.emitFunction("f", java.util.List.of(),
                        letNode("global", cmp(BinaryOp.BinOp.ADD, var("a"), var("b")), letBinder("global"))));
    }
    @Test void declinesDuplicateSanitizedParams() {
        // params global + global_ both sanitize to global_ -> would be `def f(global_, global_)` (a SyntaxError).
        assertThrows(EmitterException.class,
                () -> emitter.emitFunction("f", List.of("global", "global_"), var("a")));
    }
    @Test void declinesNonAsciiNavFeature() {
        assertThrows(EmitterException.class, () -> emitter.emit(fieldAccess(var("o"), "café")));
    }

    // ── snake_case value identifiers (step-11) ─────────────────────────────────────
    @Test void snakeCasedNavigationKeyMatchesDeclaration() {
        // Access key = snakeName("dayCountFraction") = "day_count_fraction" — the SAME string the declaration
        // emitter produces for the same Rune name (see camel-case-names.py). Mirrors the existing :604 pattern
        // `_get(o, 'global_')` (which stays snake-inert).
        assertEquals("_get(o, 'day_count_fraction')",
                emitter.emit(fieldAccess(var("o"), "dayCountFraction")));
    }

    @Test void snakeCasedNavigationKeyResolvesAtRuntime() throws Exception {
        Assumptions.assumeTrue(pythonAvailable(), "python not on PATH — run skipped");
        // read(o) -> _get(o, 'day_count_fraction'); construct with the snaked attribute -> resolves.
        // Use an int payload: runNav prints repr(<call>), and repr(7) == "7" (repr(Decimal('0.25')) would be
        // "Decimal('0.25')", not "0.25" — the existing Decimal run tests at :677/:917 expect the repr form).
        String func = emitter.emitFunction("read", List.of("o"), fieldAccess(var("o"), "dayCountFraction"));
        assertEquals("7", runNav(func, "read(SimpleNamespace(day_count_fraction=7))"));
    }

    @Test void snakeCasesParamAtSignatureAndBodyReference() {
        // A camelCase param `dayCount` -> `day_count` in the def signature (314) AND its body reference (353),
        // so the two agree (param<->ref by construction). (`passThrough` also re-checks the Task-3 def flip.)
        String out = emitter.emitFunction("passThrough", List.of("dayCount"), var("dayCount"));
        assertEquals("def pass_through(day_count):\n    return day_count", out);
    }

    @Test void snakeCasesLetBinderAtBindingAndReference() {
        // A camelCase Let binder `orderId` -> `order_id` at the binding (338) AND the reference (353).
        // Uses the file's letNode(binder, value, in) (:1567) + letBinder(name) (:1573) + intLit helpers.
        Let let = letNode("orderId", intLit(5), letBinder("orderId"));
        String out = emitter.emitFunction("f", List.of(), let);
        assertEquals("def f():\n    order_id = 5\n    return order_id", out);
    }

    // ---- parse-and-run oracle (runs when python is available) -----------------------------

    @Test
    void emittedPythonParsesAndEvaluates() throws Exception {
        Assumptions.assumeTrue(pythonAvailable(), "python not on PATH — parse-and-run oracle skipped");
        assertEquals("42", runPython(emitter.emit(intLit(42))));
        assertEquals("Decimal('1.5')", runPython(emitter.emit(lit(IRLiteral.LiteralKind.NUMBER, new BigDecimal("1.5")))));
        assertEquals("'hello'", runPython(emitter.emit(lit(IRLiteral.LiteralKind.STRING, "hello"))));
        assertEquals("True", runPython(emitter.emit(lit(IRLiteral.LiteralKind.BOOLEAN, Boolean.TRUE))));
        assertEquals("None", runPython(emitter.emit(empty())));
        assertEquals("False", runPython(emitter.emit(cmp(BinaryOp.BinOp.EQ, intLit(5), intLit(3)))));
        assertEquals("True", runPython(emitter.emit(cmp(BinaryOp.BinOp.LT, intLit(3), intLit(5)))));
        // beyond-long: the Java target hoists a BigInteger; Python evaluates the unbounded int natively.
        assertEquals("9223372036854775808",
                runPython(emitter.emit(lit(IRLiteral.LiteralKind.INT, new BigInteger("9223372036854775808")))));
    }

    @Test
    void escapedStringLiteralRoundTripsThroughCPython() throws Exception {
        Assumptions.assumeTrue(pythonAvailable(), "python not on PATH — parse-and-run oracle skipped");
        // The escaping must ROUND-TRIP: CPython parses the emitted literal back to the EXACT original characters
        // (newline, tab, a 0x01 control char), not merely run without a SyntaxError (F4). The expected value is
        // built independently with chr(), so the comparison cannot pass by coincidentally re-applying the escape.
        String raw = "a\nb\tc" + (char) 1 + "d";
        String emitted = emitter.emit(lit(IRLiteral.LiteralKind.STRING, raw));
        String expected = "'a' + chr(10) + 'b' + chr(9) + 'c' + chr(1) + 'd'";
        assertEquals("True", runProgram("print(" + emitted + " == (" + expected + "))"));
    }

    @Test
    void navigationFunctionRunsAndPropagatesNoneThroughAnAbsentIntermediate() throws Exception {
        Assumptions.assumeTrue(pythonAvailable(), "python not on PATH — parse-and-run oracle skipped");
        // `a -> b -> c`: an optional single-result chain, wrapped in a runnable function.
        FieldAccess chain = fieldAccess(fieldAccess(var("a"), "b"), "c");
        String func = emitter.emitFunction("MyFunc", List.of("a"), chain);
        assertEquals("def my_func(a):\n    return _get(_get(a, 'b'), 'c')", func);

        // present chain → the navigated leaf value
        assertEquals("Decimal('1.5')",
                runNav(func, "my_func(SimpleNamespace(b=SimpleNamespace(c=Decimal('1.5'))))"));
        // absent INTERMEDIATE hop → the inner _get(a,'b') is None and the outer _get(None,'c') must
        // short-circuit. A single-hop-leaf absent would only prove _get(None,…)→None; only an absent
        // INTERMEDIATE proves propagation THROUGH the chain (the absorbing-monoid headline).
        assertEquals("None", runNav(func, "my_func(SimpleNamespace(b=None))"));
        // absent base → None
        assertEquals("None", runNav(func, "my_func(None)"));
    }

    @Test
    void multiNavigationFunctionRunsAndCollapsesAbsentToEmptyList() throws Exception {
        Assumptions.assumeTrue(pythonAvailable(), "python not on PATH — parse-and-run oracle skipped");
        // `trade -> legs`: a MULTI feature off a single receiver, wrapped in a runnable function.
        FieldAccess multiFeature = new FieldAccess(var("trade"), "legs", NodeId.ROOT,
                RMetaAnnotatedType.MISSING, ExpressionCardinality.MULTI, ExpressionCardinality.MULTI,
                Optionality.PRESENT, SourceRange.NONE);
        String func = emitter.emitFunction("MyFunc", List.of("trade"), multiFeature);
        assertEquals("def my_func(trade):\n    return _get_all(trade, 'legs')", func);

        // present non-empty list → the list itself
        assertEquals("[1, 2, 3]", runNav(func, "my_func(SimpleNamespace(legs=[1, 2, 3]))"));
        // present EMPTY list → [] (the `is not None` guard preserves it, not truthiness)
        assertEquals("[]", runNav(func, "my_func(SimpleNamespace(legs=[]))"));
        // absent list (attribute None) → [] (the list-monad decision, directly observed)
        assertEquals("[]", runNav(func, "my_func(SimpleNamespace(legs=None))"));
        // absent receiver → [] (an absent navigation base collapses to the empty list, not None)
        assertEquals("[]", runNav(func, "my_func(None)"));
    }

    @Test
    void flatMapFunctionRunsAndDropsPerElementAbsents() throws Exception {
        Assumptions.assumeTrue(pythonAvailable(), "python not on PATH — parse-and-run oracle skipped");
        // `legs -> rate`: a SINGLE feature mapped over a MULTI receiver (the bare multi param), wrapped in a
        // runnable function. Each element's rate is navigated; absent rates are dropped (Rune's MapperC
        // error-item semantic), so the result is the list of present rates only.
        IRVariable multiBase = new IRVariable("legs", IRVariable.VariableKind.PARAM, NodeId.ROOT,
                RMetaAnnotatedType.MISSING, ExpressionCardinality.MULTI, Optionality.PRESENT, SourceRange.NONE);
        FieldAccess flatMap = new FieldAccess(multiBase, "rate", NodeId.ROOT, RMetaAnnotatedType.MISSING,
                ExpressionCardinality.MULTI, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        String func = emitter.emitFunction("MyFunc", List.of("legs"), flatMap);
        assertEquals("def my_func(legs):\n    return _map(legs, 'rate')", func);

        // all features present → the full list of rates
        assertEquals("[1, 2, 3]", runNav(func,
                "my_func([SimpleNamespace(rate=1), SimpleNamespace(rate=2), SimpleNamespace(rate=3)])"));
        // a MIDDLE element's feature absent → the None is DROPPED (the headline: [1, 3], not [1, None, 3]).
        // A present-only run would pass even with the filter removed; only the some-absent case proves the drop.
        assertEquals("[1, 3]", runNav(func,
                "my_func([SimpleNamespace(rate=1), SimpleNamespace(rate=None), SimpleNamespace(rate=3)])"));
        // a None ELEMENT → also dropped (_get(None, 'rate') is None)
        assertEquals("[1, 2]", runNav(func,
                "my_func([SimpleNamespace(rate=1), None, SimpleNamespace(rate=2)])"));
        // empty source list → []
        assertEquals("[]", runNav(func, "my_func([])"));
        // absent source (None) → [] (the `xs or []` guard)
        assertEquals("[]", runNav(func, "my_func(None)"));
    }

    @Test
    void flattenFunctionRunsConcatenatingAndDroppingNulls() throws Exception {
        Assumptions.assumeTrue(pythonAvailable(), "python not on PATH — parse-and-run oracle skipped");
        // `legs -> counterparties`: a MULTI feature off a MULTI receiver (the bare multi param), wrapped in a
        // runnable function. Each leg's counterparties list is flattened into one list (Rune mapC).
        IRVariable multiBase = new IRVariable("legs", IRVariable.VariableKind.PARAM, NodeId.ROOT,
                RMetaAnnotatedType.MISSING, ExpressionCardinality.MULTI, Optionality.PRESENT, SourceRange.NONE);
        FieldAccess flatten = new FieldAccess(multiBase, "counterparties", NodeId.ROOT, RMetaAnnotatedType.MISSING,
                ExpressionCardinality.MULTI, ExpressionCardinality.MULTI, Optionality.PRESENT, SourceRange.NONE);
        String func = emitter.emitFunction("MyFunc", List.of("legs"), flatten);
        assertEquals("def my_func(legs):\n    return _flat_map(legs, 'counterparties')", func);

        // two legs, each with a counterparties list → the CONCATENATED list (proves flatten).
        assertEquals("[1, 2, 3, 4]", runNav(func, "my_func([SimpleNamespace(counterparties=[1, 2]), "
                + "SimpleNamespace(counterparties=[3, 4])])"));
        // a leg with an empty list + a leg with an absent (None) list → both contribute nothing.
        assertEquals("[1, 2]", runNav(func, "my_func([SimpleNamespace(counterparties=[]), "
                + "SimpleNamespace(counterparties=[1, 2]), SimpleNamespace(counterparties=None)])"));
        // a null ELEMENT within a per-element list → dropped (the mapC error-item semantic).
        assertEquals("[1, 3]", runNav(func, "my_func([SimpleNamespace(counterparties=[1, None]), "
                + "SimpleNamespace(counterparties=[3])])"));
        // a null RECEIVER element (a None leg) → contributes nothing (_get_all(None, ...) → []).
        assertEquals("[5]", runNav(func, "my_func([None, SimpleNamespace(counterparties=[5])])"));
        // empty source → []; absent source (None) → []
        assertEquals("[]", runNav(func, "my_func([])"));
        assertEquals("[]", runNav(func, "my_func(None)"));
    }

    @Test
    void logicalFunctionRunsBothPolarities() throws Exception {
        Assumptions.assumeTrue(pythonAvailable(), "python not on PATH — parse-and-run oracle skipped");
        // `(a >= b) and (c < d)` wrapped in a function — runs to True/False.
        BinaryOp and = cmp(BinaryOp.BinOp.AND,
                cmp(BinaryOp.BinOp.GTE, var("a"), var("b")), cmp(BinaryOp.BinOp.LT, var("c"), var("d")));
        String func = emitter.emitFunction("MyFunc", List.of("a", "b", "c", "d"), and);
        assertEquals("def my_func(a, b, c, d):\n    return ((a >= b) and (c < d))", func);
        assertEquals("True", runNav(func, "my_func(5, 3, 1, 2)"));    // 5>=3 and 1<2
        assertEquals("False", runNav(func, "my_func(5, 3, 2, 1)"));   // 5>=3 and 2<1 → False
    }

    @Test
    void existenceFunctionRunsBothPolarities() throws Exception {
        Assumptions.assumeTrue(pythonAvailable(), "python not on PATH — parse-and-run oracle skipped");
        // scalar exists → present True, None False
        String scalarExists = emitter.emitFunction("MyFunc", List.of("foo"),
                exists(Existence.ExistOp.EXISTS, var("foo")));
        assertEquals("def my_func(foo):\n    return (foo is not None)", scalarExists);
        assertEquals("True", runNav(scalarExists, "my_func(42)"));
        assertEquals("False", runNav(scalarExists, "my_func(None)"));
        // a present-but-FALSY scalar (0) still exists (is not None, not truthiness)
        assertEquals("True", runNav(scalarExists, "my_func(0)"));

        // multi exists → non-empty True, empty False
        String multiExists = emitter.emitFunction("MyFunc", List.of("legs"),
                exists(Existence.ExistOp.EXISTS, multiVar("legs")));
        assertEquals("def my_func(legs):\n    return (len(legs) > 0)", multiExists);
        assertEquals("True", runNav(multiExists, "my_func([1, 2])"));
        assertEquals("False", runNav(multiExists, "my_func([])"));

        // scalar is absent → None True, present False; multi is absent → [] True
        String scalarAbsent = emitter.emitFunction("MyFunc", List.of("foo"),
                exists(Existence.ExistOp.ABSENT, var("foo")));
        assertEquals("True", runNav(scalarAbsent, "my_func(None)"));
        assertEquals("False", runNav(scalarAbsent, "my_func(7)"));
        String multiAbsent = emitter.emitFunction("MyFunc", List.of("legs"),
                exists(Existence.ExistOp.ABSENT, multiVar("legs")));
        assertEquals("True", runNav(multiAbsent, "my_func([])"));
        assertEquals("False", runNav(multiAbsent, "my_func([1])"));
    }

    @Test
    void existenceCardinalityModifiersRun() throws Exception {
        Assumptions.assumeTrue(pythonAvailable(), "python not on PATH — parse-and-run oracle skipped");
        // Pin exists / single / multiple on the SAME inputs so a single↔multiple swap is a guaranteed failure.
        String plain = emitter.emitFunction("MyFunc", List.of("xs"), exists(Existence.ExistOp.EXISTS, multiVar("xs")));
        String single = emitter.emitFunction("MyFunc", List.of("xs"),
                existsMod(Existence.ExistOp.EXISTS, Existence.ExistMod.SINGLE, multiVar("xs")));
        String multiple = emitter.emitFunction("MyFunc", List.of("xs"),
                existsMod(Existence.ExistOp.EXISTS, Existence.ExistMod.MULTIPLE, multiVar("xs")));
        assertEquals("def my_func(xs):\n    return (len(xs) == 1)", single);
        assertEquals("def my_func(xs):\n    return (len(xs) > 1)", multiple);
        // [1] — the first swap-catcher: exists True, single True, multiple False.
        assertEquals("True", runNav(plain, "my_func([1])"));
        assertEquals("True", runNav(single, "my_func([1])"));
        assertEquals("False", runNav(multiple, "my_func([1])"));
        // [1, 2] — the second swap-catcher: exists True, single False, multiple True.
        assertEquals("True", runNav(plain, "my_func([1, 2])"));
        assertEquals("False", runNav(single, "my_func([1, 2])"));
        assertEquals("True", runNav(multiple, "my_func([1, 2])"));
        // [] — the boundary: all False.
        assertEquals("False", runNav(single, "my_func([])"));
        assertEquals("False", runNav(multiple, "my_func([])"));

        // SCALAR fold: single ⟺ present; multiple ⟺ structurally False (the operand folds away → `return False`).
        String scalarSingle = emitter.emitFunction("MyFunc", List.of("foo"),
                existsMod(Existence.ExistOp.EXISTS, Existence.ExistMod.SINGLE, var("foo")));
        String scalarMultiple = emitter.emitFunction("MyFunc", List.of("foo"),
                existsMod(Existence.ExistOp.EXISTS, Existence.ExistMod.MULTIPLE, var("foo")));
        assertEquals("def my_func(foo):\n    return False", scalarMultiple);
        assertEquals("True", runNav(scalarSingle, "my_func(42)"));
        assertEquals("False", runNav(scalarSingle, "my_func(None)"));
        // scalar multiple is the folded constant — False for any input (the pure operand is dropped).
        assertEquals("False", runNav(scalarMultiple, "my_func(42)"));
        assertEquals("False", runNav(scalarMultiple, "my_func(None)"));
    }

    @Test
    void listOpsRun() throws Exception {
        Assumptions.assumeTrue(pythonAvailable(), "python not on PATH — parse-and-run oracle skipped");
        // Swap-catcher matrix over ONE input [3, 1, 2] — any switch(op) mis-wire is a guaranteed failure.
        String count = emitter.emitFunction("MyFunc", List.of("xs"), listOp(IRListOp.Kind.COUNT, multiVar("xs")));
        String first = emitter.emitFunction("MyFunc", List.of("xs"), listOp(IRListOp.Kind.FIRST, multiVar("xs")));
        String last = emitter.emitFunction("MyFunc", List.of("xs"), listOp(IRListOp.Kind.LAST, multiVar("xs")));
        String reverse = emitter.emitFunction("MyFunc", List.of("xs"), listOp(IRListOp.Kind.REVERSE, multiVar("xs")));
        String distinct = emitter.emitFunction("MyFunc", List.of("xs"), listOp(IRListOp.Kind.DISTINCT, multiVar("xs")));
        assertEquals("3", runNav(count, "my_func([3, 1, 2])"));
        assertEquals("3", runNav(first, "my_func([3, 1, 2])"));
        assertEquals("2", runNav(last, "my_func([3, 1, 2])"));
        assertEquals("[2, 1, 3]", runNav(reverse, "my_func([3, 1, 2])"));
        assertEquals("[3, 1, 2]", runNav(distinct, "my_func([3, 1, 2])"));

        // first/last on an EMPTY list → None (the absorbing-absent headline for this tier).
        assertEquals("None", runNav(first, "my_func([])"));
        assertEquals("None", runNav(last, "my_func([])"));
        assertEquals("0", runNav(count, "my_func([])"));

        // distinct is ORDER-PRESERVING value-equality dedup.
        assertEquals("[1, 2, 3]", runNav(distinct, "my_func([1, 2, 2, 3, 1])"));
        // distinct over UNHASHABLE elements (lists) — the `x not in out` path works; a dict.fromkeys impl
        // would raise `unhashable type: 'list'`. This is the load-bearing proof of the helper choice.
        assertEquals("[[1], [2]]", runNav(distinct, "my_func([[1], [1], [2]])"));

        // count composes in a cardinality-check comparison (the bonus payoff: `xs count >= 1`).
        // COUNT is a SCALAR result (SINGLE cardinality) in the real adapter; construct it with SINGLE here
        // so the J2 defensive MULTI guard in emitBinaryOp does not falsely fire on a synthetic test node.
        IRListOp countSingle = new IRListOp(IRListOp.Kind.COUNT, multiVar("xs"), NodeId.ROOT,
                RMetaAnnotatedType.MISSING, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        String countGe1 = emitter.emitFunction("MyFunc", List.of("xs"),
                cmp(BinaryOp.BinOp.GTE, countSingle, intLit(1)));
        assertEquals("True", runNav(countGe1, "my_func([1])"));
        assertEquals("False", runNav(countGe1, "my_func([])"));
    }

    @Test
    void countOverSingleChildRunsAsZeroOrOne() throws Exception {
        Assumptions.assumeTrue(pythonAvailable(), "python not on PATH — parse-and-run oracle skipped");
        // `count` over a SINGLE/optional child is resultCount() ∈ {0,1} (MapperS.resultCount() is a pure null
        // check), NOT len(scalar) — len(None)/len(42) raises a TypeError. The emitter must fold to 0/1 on the
        // child's neutral cardinality (F1 regression: a SINGLE-child count previously emitted len(scalar)). The
        // emission form is pinned deterministically in emitsListOps; this oracle proves the runtime 0/1.
        String func = emitter.emitFunction("MyFunc", List.of("foo"), listOp(IRListOp.Kind.COUNT, var("foo")));
        assertEquals("0", runNav(func, "my_func(None)"));   // absent → 0 (was a len(None) TypeError)
        assertEquals("1", runNav(func, "my_func(42)"));     // present → 1 (was a len(42) TypeError)
        assertEquals("1", runNav(func, "my_func(0)"));      // present-but-falsy scalar still counts (is None, not truthiness)
    }

    @Test
    void functionCallRuns() throws Exception {
        Assumptions.assumeTrue(pythonAvailable(), "python not on PATH — parse-and-run oracle skipped");
        // The call invokes the callee with correctly-ORDERED args. A NON-commutative stub (_sub = a - b) makes
        // an arg-order swap a guaranteed failure (the swap-catcher discipline).
        String body = emitter.emitFunction("MyFunc", List.of("a", "b"), apply("_sub", var("a"), var("b")));
        assertEquals("def my_func(a, b):\n    return _sub(a, b)", body);
        String prog = "def _sub(a, b):\n    return a - b\n" + body;
        assertEquals("7", runNav(prog, "my_func(10, 3)"));    // 10 - 3 = 7 (a swap would give -7)
        // a literal arg: _sub(a, 4)
        String litBody = emitter.emitFunction("MyFunc", List.of("a"), apply("_sub", var("a"), intLit(4)));
        assertEquals("6", runNav("def _sub(a, b):\n    return a - b\n" + litBody, "my_func(10)"));
    }

    @Test
    void arithmeticAddSubMulRunsExact() throws Exception {
        Assumptions.assumeTrue(pythonAvailable(), "python not on PATH — parse-and-run oracle skipped");
        // int + - * over int operands → exact int (the value, regardless of Rune's int/number typing)
        assertEquals("5", runNav(emitter.emitFunction("MyFunc", List.of("a", "b"),
                cmp(BinaryOp.BinOp.ADD, var("a"), var("b"))), "my_func(2, 3)"));
        assertEquals("6", runNav(emitter.emitFunction("MyFunc", List.of("a", "b"),
                cmp(BinaryOp.BinOp.SUB, var("a"), var("b"))), "my_func(10, 4)"));
        assertEquals("42", runNav(emitter.emitFunction("MyFunc", List.of("a", "b"),
                cmp(BinaryOp.BinOp.MUL, var("a"), var("b"))), "my_func(6, 7)"));
        // Decimal + Decimal → exact Decimal; mixed int * Decimal → Decimal
        String add = emitter.emitFunction("MyFunc", List.of("a", "b"), cmp(BinaryOp.BinOp.ADD, var("a"), var("b")));
        assertEquals("Decimal('4.0')", runNav(add, "my_func(Decimal('1.5'), Decimal('2.5'))"));
        assertEquals("Decimal('4.5')",
                runNav(emitter.emitFunction("MyFunc", List.of("a", "b"), cmp(BinaryOp.BinOp.MUL, var("a"), var("b"))),
                        "my_func(3, Decimal('1.5'))"));
        // EXACT multiply regression: 33 significant digits — would round to 28 under Python's DEFAULT context.
        // The MAX_PREC module context keeps + - * exact (matching Rune's unbounded BigDecimal arithmetic).
        assertEquals("Decimal('1.00000000000000020000000000000001')",
                runNav(emitter.emitFunction("MyFunc", List.of("a", "b"), cmp(BinaryOp.BinOp.MUL, var("a"), var("b"))),
                        "my_func(Decimal('1.0000000000000001'), Decimal('1.0000000000000001'))"));
    }

    @Test
    void divisionRunsAtDecimal128Precision() throws Exception {
        Assumptions.assumeTrue(pythonAvailable(), "python not on PATH — parse-and-run oracle skipped");
        String div = emitter.emitFunction("MyFunc", List.of("a", "b"), cmp(BinaryOp.BinOp.DIV, var("a"), var("b")));
        assertEquals("def my_func(a, b):\n    return _div(a, b)", div);
        // terminating divisions → exact (these would pass even under Python's default 28-digit context)
        assertEquals("Decimal('3.5')", runNav(div, "my_func(7, 2)"));
        assertEquals("Decimal('0.25')", runNav(div, "my_func(1, 4)"));
        // THE HEADLINE: a non-terminating division → exactly 34 significant digits (MathContext.DECIMAL128).
        // Python's default context gives 28 threes, so this case PROVES the prec-34 decision, not just division.
        assertEquals("Decimal('0.3333333333333333333333333333333333')", runNav(div, "my_func(1, 3)"));
        // the compound day-count shape: (p1 / 365) + (p2 / 366) — two DECIMAL128 quotients added EXACTLY
        // (the + runs under MAX_PREC, so the 34-digit sum is not re-rounded to 28).
        BinaryOp compound = cmp(BinaryOp.BinOp.ADD,
                cmp(BinaryOp.BinOp.DIV, var("p1"), lit(IRLiteral.LiteralKind.INT, BigInteger.valueOf(365))),
                cmp(BinaryOp.BinOp.DIV, var("p2"), lit(IRLiteral.LiteralKind.INT, BigInteger.valueOf(366))));
        assertEquals("Decimal('0.8204206901714200164682985253387229')",
                runNav(emitter.emitFunction("MyFunc", List.of("p1", "p2"), compound), "my_func(100, 200)"));
    }

    @Test
    void transformingLetChainRunsToTheComposedValue() throws Exception {
        Assumptions.assumeTrue(pythonAvailable(), "python not on PATH — parse-and-run oracle skipped");
        // The synthetic transforming chain RUNS to the composed value: x0 = a*2; x1 = x0+1; return x1*10.
        // a=3 → x0=6, x1=7, return 70 — a non-degenerate composition (a dropped/reordered binding → NameError,
        // never a coincidental match), proving the value-is-Let flatten + the binder references run correctly.
        Let inner = letNode("x0", cmp(BinaryOp.BinOp.MUL, var("a"), intLit(2)),
                cmp(BinaryOp.BinOp.ADD, letBinder("x0"), intLit(1)));
        Let outer = letNode("x1", inner, cmp(BinaryOp.BinOp.MUL, letBinder("x1"), intLit(10)));
        assertEquals("70", runNav(emitter.emitFunction("MyFunc", List.of("a"), outer), "my_func(3)"));
    }

    @Test void keywordFieldNavigatesEndToEnd() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(pythonAvailable(), "python not on PATH — skipped");
        // The DECLARATION emitter names the attribute global_ and decorates the class @dataclass (the program
        // injects the dataclass import; the builtin `int` field needs no type preamble):
        String classSrc = new IRPythonDeclarationEmitter().emit(declStructWithGlobalIntField());
        org.junit.jupiter.api.Assertions.assertEquals("@dataclass\nclass Foo:\n    global_: int = None", classSrc); // pin the decl side
        // The EXPRESSION emitter reads it the SAME way:
        String nav = emitter.emit(fieldAccess(var("o"), "global"));                               // _get(o, 'global_')
        // Run both together: instantiate the REAL emitted class, set the attribute, navigate.
        String program = "from dataclasses import dataclass\n"
                + IRPythonEmitter.NAV_HELPER + "\n"
                + classSrc + "\n"
                + "o = Foo()\n"
                + "o.global_ = 7\n"
                + "print(repr(" + nav + "))";
        org.junit.jupiter.api.Assertions.assertEquals("7", runProgram(program)); // pre-step-8 raw 'global' would print None
    }

    // ── enum-value references (step-10) ──────────────────────────────────────────
    @Test void emitsEnumValueReference() {
        assertEquals("Color.RED", emitter.emit(enumValueRef("Color", "RED")));
    }
    @Test void enumValueReferenceUsesSimpleEnumName() {
        assertEquals("Color.RED", emitter.emit(enumValueRef("ns.Color", "RED")));
    }
    @Test void enumValueReferenceSanitizesKeywordMember() {
        // The member IDENTIFIER (None_), matching the noun emitter `None_ = "None"` — NOT the value "None".
        assertEquals("CompoundingMethodEnum.None_", emitter.emit(enumValueRef("CompoundingMethodEnum", "None")));
    }
    @Test void declinesEnumValueReferenceWithNonEnumType() {
        IRReference bad = new IRReference("VALUE", IRReference.ReferenceKind.ENUM_VALUE, NodeId.ROOT,
                RMetaAnnotatedType.MISSING, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        assertThrows(EmitterException.class, () -> emitter.emit(bad));
    }
    @Test void declinesEnumValueReferenceWithNullType() {
        IRReference bad = new IRReference("VALUE", IRReference.ReferenceKind.ENUM_VALUE, NodeId.ROOT,
                null, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        assertThrows(EmitterException.class, () -> emitter.emit(bad));
    }
    @Test void declinesNonEnumReferenceKinds() {
        // The default arm declines every non-ENUM_VALUE kind (FUNCTION is corpus-impossible as a standalone
        // reference — it is APPLY's callee, read directly by emitApply — but the default arm covers it too).
        for (IRReference.ReferenceKind k : new IRReference.ReferenceKind[]{
                IRReference.ReferenceKind.FUNCTION, IRReference.ReferenceKind.SUPER,
                IRReference.ReferenceKind.ALIAS, IRReference.ReferenceKind.RULE}) {
            IRReference ref = new IRReference("x", k, NodeId.ROOT, RMetaAnnotatedType.MISSING,
                    ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
            assertThrows(EmitterException.class, () -> emitter.emit(ref));
        }
    }
    @Test void lowersEnumValueCallArgument() {
        // The reachable side effect: an enum-value call arg now lowers (was a decline).
        assertEquals("foo(Color.RED)", emitter.emit(apply("Foo", enumValueRef("Color", "RED"))));
    }

    @Test void enumReferenceResolvesAgainstTheRealEmittedEnum() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(pythonAvailable(), "python not on PATH — skipped");
        // The NOUN emitter emits the real Python enum class …
        IREnumNode colorEnum = new IREnumNode("Color", List.<IREnumValue>of(
                new IREnumValueNode("RED", Optional.empty(), Optional.empty(), IRMetadata.EMPTY),
                new IREnumValueNode("GREEN", Optional.empty(), Optional.empty(), IRMetadata.EMPTY)),
                Optional.empty(), IRMetadata.EMPTY);
        String enumSrc = new IRPythonDeclarationEmitter().emit(colorEnum);
        // … and the VERB emitter references its members the SAME way (Color.RED / Color.GREEN).
        String same = emitter.emit(cmp(BinaryOp.BinOp.EQ, enumValueRef("Color", "RED"), enumValueRef("Color", "RED")));
        String diff = emitter.emit(cmp(BinaryOp.BinOp.EQ, enumValueRef("Color", "RED"), enumValueRef("Color", "GREEN")));
        String program = IRPythonDeclarationEmitter.DECL_PREAMBLE + "\n" + enumSrc + "\n"
                + "print(repr((" + same + ", " + diff + ")))";
        org.junit.jupiter.api.Assertions.assertEquals("(True, False)", runProgram(program));
    }

    // ---- "same tree the Java emitter consumes" (skips without builtins) --------------------

    @Test
    void emitsActualAdapterOutput() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT),
                "rune-dsl builtins absent (" + BUILTINS_ROOT.toAbsolutePath() + ") — skipped");
        RWorkspace ws = workspace();
        ExpressionToIRAdapter adapter = new ExpressionToIRAdapter();

        RIntLiteral intLit = new RIntLiteral();
        intLit.setValue(42);
        assertEquals("42", emitter.emit(adapter.adapt(intLit, ws).orElseThrow()));

        RStringLiteral strLit = new RStringLiteral();
        strLit.setValue("hello");
        assertEquals("'hello'", emitter.emit(adapter.adapt(strLit, ws).orElseThrow()));

        RBooleanLiteral boolLit = new RBooleanLiteral();
        boolLit.setValue(true);
        assertEquals("True", emitter.emit(adapter.adapt(boolLit, ws).orElseThrow()));

        RNumberLiteral numLit = new RNumberLiteral();
        numLit.setValue(new BigDecimal("1.5"));
        assertEquals("Decimal('1.5')", emitter.emit(adapter.adapt(numLit, ws).orElseThrow()));

        assertEquals("None", emitter.emit(adapter.adapt(new REmptyLiteral(), ws).orElseThrow()));

        // The headline non-circular proof: a REAL adapter-produced comparison tree — the same
        // BinaryOp record the Java emitter (IRJavaLeafEmitter) consumes — drives Python end-to-end,
        // recursing through children(). `n1 >= n2` over two scalar params → BinaryOp{GTE, IRVariable,
        // IRVariable}. (The adapter only emits comparisons over resolved operands, never two literals.)
        RAttribute n1 = scalarParam("n1");
        RAttribute n2 = scalarParam("n2");
        functionWith(ws, n1, n2);
        RComparisonExpr paramCmp = new RComparisonExpr();
        paramCmp.setOp(CompOp.GTE);
        paramCmp.setLeft(paramRef("n1", n1, ws));
        paramCmp.setRight(paramRef("n2", n2, ws));
        paramCmp.attachToWorkspace(ws);
        IRExpr paramCmpIr = adapter.adapt(paramCmp, ws).orElseThrow();
        assertEquals(IRExprKind.BINARY_OP, paramCmpIr.kind(), "the adapter produced a real BinaryOp");
        assertEquals("(n1 >= n2)", emitter.emit(paramCmpIr));

        // `n < 0` — a real adapter-produced comparison mixing a param and a literal operand: the
        // children()-recursion descends into an actual adapter IRVariable AND an actual adapter IRLiteral.
        RAttribute n = scalarParam("n");
        functionWith(ws, n);
        RComparisonExpr paramVsLiteral = new RComparisonExpr();
        paramVsLiteral.setOp(CompOp.LT);
        paramVsLiteral.setLeft(paramRef("n", n, ws));
        paramVsLiteral.setRight(astInt(0));
        paramVsLiteral.attachToWorkspace(ws);
        assertEquals("(n < 0)", emitter.emit(adapter.adapt(paramVsLiteral, ws).orElseThrow()));
    }

    @Test
    void emitsActualAdapterNavigation() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT),
                "rune-dsl builtins absent (" + BUILTINS_ROOT.toAbsolutePath() + ") — skipped");
        RWorkspace ws = workspace();
        ExpressionToIRAdapter adapter = new ExpressionToIRAdapter();

        // `foo -> bar`: a REAL adapter-produced single-hop FieldAccess off a scalar param — the same
        // record the Java emitter (IRJavaLeafEmitter) consumes — drives Python navigation non-circularly.
        RAttribute foo = scalarParam("foo");
        functionWith(ws, foo);
        RAttribute bar = scalarParam("bar"); // single, non-meta navigated feature
        IRExpr nav = adapter.adapt(featureCall("foo", foo, "bar", bar, ws), ws).orElseThrow();
        assertEquals(IRExprKind.FIELD_ACCESS, nav.kind(), "the adapter produced a real FieldAccess");
        assertEquals("_get(foo, 'bar')", emitter.emit(nav));

        // `trade -> legs` (legs multi): a REAL adapter MULTI feature off a SINGLE receiver → list nav.
        RAttribute trade = scalarParam("trade");
        functionWith(ws, trade);
        RAttribute legs = multiParam("legs"); // (0..*) → accumulated MULTI result, SINGLE receiver
        IRExpr multiNav = adapter.adapt(hop(paramRef("trade", trade, ws), "legs", legs, ws), ws).orElseThrow();
        assertEquals(ExpressionCardinality.MULTI, multiNav.cardinality(), "a real adapter multi-result nav");
        assertEquals("_get_all(trade, 'legs')", emitter.emit(multiNav));

        // `legs2 -> rate` (legs2 a multi PARAM base, rate single): a REAL adapter SINGLE feature off a
        // MULTI receiver → flat-map. (This was a DECLINE in L-083; this slice emits it.)
        RAttribute legs2 = multiParam("legs2");
        functionWith(ws, legs2);
        RAttribute rate = scalarParam("rate"); // single feature off the multi base
        IRExpr flatMap = adapter.adapt(hop(paramRef("legs2", legs2, ws), "rate", rate, ws), ws).orElseThrow();
        assertEquals(ExpressionCardinality.MULTI, flatMap.cardinality(), "a real adapter flat-map result");
        FieldAccess flatMapFa = assertInstanceOf(FieldAccess.class, flatMap);
        assertEquals(ExpressionCardinality.MULTI, flatMapFa.receiver().cardinality(), "MULTI receiver");
        assertEquals(ExpressionCardinality.SINGLE, flatMapFa.featureCardinality(), "SINGLE feature → a map");
        assertEquals("_map(legs2, 'rate')", emitter.emit(flatMap));

        // `cps -> counterparties` (cps multi base, counterparties multi feature): a REAL adapter FLATTEN —
        // a MULTI feature off a MULTI receiver → _flat_map. featureCardinality MULTI is the sole
        // discriminator from the single-feature flat-map above. (This was a DECLINE in L-084.)
        RAttribute cps = multiParam("cps");
        functionWith(ws, cps);
        RAttribute counterparties = multiParam("counterparties"); // multi feature off the multi base
        IRExpr flatten = adapter.adapt(hop(paramRef("cps", cps, ws), "counterparties", counterparties, ws), ws)
                .orElseThrow();
        FieldAccess flattenFa = assertInstanceOf(FieldAccess.class, flatten);
        assertEquals(ExpressionCardinality.MULTI, flattenFa.receiver().cardinality(), "MULTI receiver");
        assertEquals(ExpressionCardinality.MULTI, flattenFa.featureCardinality(), "MULTI feature → a flatten");
        assertEquals("_flat_map(cps, 'counterparties')", emitter.emit(flatten));
    }

    @Test
    void runsActualAdapterMultiNavigationToList() throws Exception {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT),
                "rune-dsl builtins absent (" + BUILTINS_ROOT.toAbsolutePath() + ") — skipped");
        RWorkspace ws = workspace();
        ExpressionToIRAdapter adapter = new ExpressionToIRAdapter();

        // The headline list-monad proof on a REAL adapter tree: `trade -> legs` wrapped in a function,
        // run to a list (present) and to [] (absent receiver / absent list).
        RAttribute trade = scalarParam("trade");
        functionWith(ws, trade);
        RAttribute legs = multiParam("legs");
        IRExpr multiNav = adapter.adapt(hop(paramRef("trade", trade, ws), "legs", legs, ws), ws).orElseThrow();
        String func = emitter.emitFunction("MyFunc", List.of("trade"), multiNav);
        assertEquals("def my_func(trade):\n    return _get_all(trade, 'legs')", func);

        Assumptions.assumeTrue(pythonAvailable(), "python not on PATH — run skipped");
        assertEquals("[1, 2]", runNav(func, "my_func(SimpleNamespace(legs=[1, 2]))"));
        assertEquals("[]", runNav(func, "my_func(SimpleNamespace(legs=None))"));
        assertEquals("[]", runNav(func, "my_func(None)"));
    }

    @Test
    void runsActualAdapterFlatMapDroppingAbsents() throws Exception {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT),
                "rune-dsl builtins absent (" + BUILTINS_ROOT.toAbsolutePath() + ") — skipped");
        RWorkspace ws = workspace();
        ExpressionToIRAdapter adapter = new ExpressionToIRAdapter();

        // The headline flat-map proof on a REAL adapter tree: `trade -> legs -> rate` (legs multi, rate
        // single) → _map(_get_all(trade, 'legs'), 'rate') — the flat-map wraps the list-nav receiver.
        RAttribute trade = scalarParam("trade");
        functionWith(ws, trade);
        RAttribute legs = multiParam("legs");
        RAttribute rate = scalarParam("rate");
        IRExpr chain = adapter.adapt(
                hop(hop(paramRef("trade", trade, ws), "legs", legs, ws), "rate", rate, ws), ws).orElseThrow();
        String func = emitter.emitFunction("MyFunc", List.of("trade"), chain);
        assertEquals("def my_func(trade):\n    return _map(_get_all(trade, 'legs'), 'rate')", func);

        Assumptions.assumeTrue(pythonAvailable(), "python not on PATH — run skipped");
        // present legs, all with a rate → the list of rates
        assertEquals("[1, 2]", runNav(func,
                "my_func(SimpleNamespace(legs=[SimpleNamespace(rate=1), SimpleNamespace(rate=2)]))"));
        // a middle leg with no rate → dropped (the drop-absents headline on a real adapter tree)
        assertEquals("[1, 3]", runNav(func, "my_func(SimpleNamespace(legs="
                + "[SimpleNamespace(rate=1), SimpleNamespace(rate=None), SimpleNamespace(rate=3)]))"));
        // absent legs (the multi feature absent) → [] (the receiver list-nav collapses, then _map([]) → [])
        assertEquals("[]", runNav(func, "my_func(SimpleNamespace(legs=None))"));
        // absent trade → [] (the whole chain collapses)
        assertEquals("[]", runNav(func, "my_func(None)"));
    }

    @Test
    void runsActualAdapterFlattenConcatenating() throws Exception {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT),
                "rune-dsl builtins absent (" + BUILTINS_ROOT.toAbsolutePath() + ") — skipped");
        RWorkspace ws = workspace();
        ExpressionToIRAdapter adapter = new ExpressionToIRAdapter();

        // The headline flatten proof on a REAL adapter tree: `cps -> counterparties` (both multi) →
        // _flat_map(cps, 'counterparties') — concatenate each element's list.
        RAttribute cps = multiParam("cps");
        functionWith(ws, cps);
        RAttribute counterparties = multiParam("counterparties");
        IRExpr flatten = adapter.adapt(hop(paramRef("cps", cps, ws), "counterparties", counterparties, ws), ws)
                .orElseThrow();
        String func = emitter.emitFunction("MyFunc", List.of("cps"), flatten);
        assertEquals("def my_func(cps):\n    return _flat_map(cps, 'counterparties')", func);

        Assumptions.assumeTrue(pythonAvailable(), "python not on PATH — run skipped");
        // two elements, each with a list → concatenated; a null element within a list → dropped
        assertEquals("[1, 2, 3]", runNav(func, "my_func([SimpleNamespace(counterparties=[1, 2]), "
                + "SimpleNamespace(counterparties=[3, None])])"));
        assertEquals("[]", runNav(func, "my_func(None)"));
    }

    @Test
    void emitsAndRunsActualAdapterOptionalChain() throws Exception {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT),
                "rune-dsl builtins absent (" + BUILTINS_ROOT.toAbsolutePath() + ") — skipped");
        RWorkspace ws = workspace();
        ExpressionToIRAdapter adapter = new ExpressionToIRAdapter();

        // `a -> b -> c` with an OPTIONAL intermediate `b` (0..1) — a REAL adapter absorbing-monoid chain:
        // SINGLE accumulated cardinality (so the emitter accepts it) but OPTIONAL optionality.
        RAttribute a = scalarParam("a");
        functionWith(ws, a);
        RAttribute optB = optionalParam("b"); // (0..1) optional single intermediate feature
        RAttribute c = scalarParam("c");       // (1..1) present final feature
        IRExpr chain = adapter.adapt(hop(featureCall("a", a, "b", optB, ws), "c", c, ws), ws).orElseThrow();
        assertEquals(IRExprKind.FIELD_ACCESS, chain.kind());
        assertEquals(ExpressionCardinality.SINGLE, chain.cardinality(), "no multi hop → single result");
        assertEquals(Optionality.OPTIONAL, chain.optionality(),
                "an optional intermediate makes the chain optional (absorbing monoid)");

        String func = emitter.emitFunction("MyFunc", List.of("a"), chain);
        assertEquals("def my_func(a):\n    return _get(_get(a, 'b'), 'c')", func);

        // The headline monad proof on a REAL adapter tree: when python is present, RUN it and assert the
        // absent intermediate propagates to None (not just an emission-string assert).
        Assumptions.assumeTrue(pythonAvailable(), "python not on PATH — run skipped");
        assertEquals("None", runNav(func, "my_func(SimpleNamespace(b=None))"));
        assertEquals("'leaf'", runNav(func, "my_func(SimpleNamespace(b=SimpleNamespace(c='leaf')))"));
    }

    @Test
    void emitsActualAdapterBooleanConditions() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT),
                "rune-dsl builtins absent (" + BUILTINS_ROOT.toAbsolutePath() + ") — skipped");
        RWorkspace ws = workspace();
        ExpressionToIRAdapter adapter = new ExpressionToIRAdapter();

        // `(n1 > n2) and (n3 >= n4)`: a REAL adapter logical BinaryOp{AND} over two comparison operands.
        RAttribute n1 = scalarParam("n1");
        RAttribute n2 = scalarParam("n2");
        RAttribute n3 = scalarParam("n3");
        RAttribute n4 = scalarParam("n4");
        functionWith(ws, n1, n2, n3, n4);
        RLogicalExpr and = new RLogicalExpr();
        and.setOp(LogOp.AND);
        and.setLeft(comparisonExpr(CompOp.GT, "n1", n1, "n2", n2, ws));
        and.setRight(comparisonExpr(CompOp.GTE, "n3", n3, "n4", n4, ws));
        and.attachToWorkspace(ws);
        assertEquals("((n1 > n2) and (n3 >= n4))", emitter.emit(adapter.adapt(and, ws).orElseThrow()));

        // `foo exists`: a REAL adapter scalar Existence.
        RAttribute foo = scalarParam("foo");
        functionWith(ws, foo);
        assertEquals("(foo is not None)",
                emitter.emit(adapter.adapt(existenceExpr(ExistenceOp.EXISTS, "foo", foo, ws), ws).orElseThrow()));

        // `xs exists`: a REAL adapter MULTI Existence → a length test.
        RAttribute xs = multiParam("xs");
        functionWith(ws, xs);
        assertEquals("(len(xs) > 0)",
                emitter.emit(adapter.adapt(existenceExpr(ExistenceOp.EXISTS, "xs", xs, ws), ws).orElseThrow()));

        // `trade -> legs is absent`: a REAL adapter MULTI navigation existence → an empty-list test.
        RAttribute trade = scalarParam("trade");
        functionWith(ws, trade);
        RAttribute legs = multiParam("legs");
        RExistenceExpr navAbsent = new RExistenceExpr();
        navAbsent.setOp(ExistenceOp.ABSENT);
        navAbsent.setArgument(hop(paramRef("trade", trade, ws), "legs", legs, ws));
        navAbsent.attachToWorkspace(ws);
        assertEquals("(len(_get_all(trade, 'legs')) == 0)",
                emitter.emit(adapter.adapt(navAbsent, ws).orElseThrow()));
    }

    @Test
    void emitsAndRunsActualAdapterExistenceModifiers() throws Exception {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT),
                "rune-dsl builtins absent (" + BUILTINS_ROOT.toAbsolutePath() + ") — skipped");
        RWorkspace ws = workspace();
        ExpressionToIRAdapter adapter = new ExpressionToIRAdapter();

        // `xs single exists` / `xs multiple exists`: REAL adapter Existence{EXISTS, SINGLE/MULTIPLE} over a multi
        // param — the SAME modifier-bearing record the Java path consumes (toExistMod passes it through) → the
        // non-circular cross-target proof of the modifier arms.
        RAttribute xs = multiParam("xs");
        functionWith(ws, xs);
        IRExpr single = adapter.adapt(existenceExpr(ExistenceOp.EXISTS, ExistsModifier.SINGLE, "xs", xs, ws), ws)
                .orElseThrow();
        assertEquals(IRExprKind.EXISTENCE, single.kind(), "a real adapter modifier-bearing Existence");
        assertEquals("(len(xs) == 1)", emitter.emit(single));
        IRExpr multiple = adapter.adapt(existenceExpr(ExistenceOp.EXISTS, ExistsModifier.MULTIPLE, "xs", xs, ws), ws)
                .orElseThrow();
        assertEquals("(len(xs) > 1)", emitter.emit(multiple));

        Assumptions.assumeTrue(pythonAvailable(), "python not on PATH — run skipped");
        String singleFunc = emitter.emitFunction("MyFunc", List.of("xs"), single);
        String multipleFunc = emitter.emitFunction("MyFunc", List.of("xs"), multiple);
        assertEquals("True", runNav(singleFunc, "my_func([1])"));
        assertEquals("False", runNav(singleFunc, "my_func([1, 2])"));
        assertEquals("False", runNav(multipleFunc, "my_func([1])"));
        assertEquals("True", runNav(multipleFunc, "my_func([1, 2])"));
    }

    @Test
    void emitsAndRunsActualAdapterListOps() throws Exception {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT),
                "rune-dsl builtins absent (" + BUILTINS_ROOT.toAbsolutePath() + ") — skipped");
        RWorkspace ws = workspace();
        ExpressionToIRAdapter adapter = new ExpressionToIRAdapter();

        // REAL adapter list-ops over a multi param `xs` — the same IRListOp records the Java path consumes
        // (adaptListOp/adaptCount). count's SCALAR cardinality is pinned against the real adapter (load-bearing
        // for composition); first/reverse/distinct emit their helper forms.
        RAttribute xs = multiParam("xs");
        functionWith(ws, xs);
        IRExpr count = adapter.adapt(countExpr(paramRef("xs", xs, ws), ws), ws).orElseThrow();
        assertEquals(IRExprKind.LIST_OP, count.kind(), "the adapter produced a real IRListOp");
        assertEquals(ExpressionCardinality.SINGLE, count.cardinality(), "count is a scalar result");
        assertEquals("len(xs)", emitter.emit(count));
        IRExpr first = adapter.adapt(listOpExpr(ListOp.FIRST, paramRef("xs", xs, ws), ws), ws).orElseThrow();
        assertEquals("_first(xs)", emitter.emit(first));
        IRExpr reverse = adapter.adapt(listOpExpr(ListOp.REVERSE, paramRef("xs", xs, ws), ws), ws).orElseThrow();
        assertEquals("list(reversed(xs))", emitter.emit(reverse));
        IRExpr distinct = adapter.adapt(listOpExpr(ListOp.DISTINCT, paramRef("xs", xs, ws), ws), ws).orElseThrow();
        assertEquals("_distinct(xs)", emitter.emit(distinct));

        Assumptions.assumeTrue(pythonAvailable(), "python not on PATH — run skipped");
        assertEquals("3", runNav(emitter.emitFunction("MyFunc", List.of("xs"), count), "my_func([3, 1, 2])"));
        assertEquals("3", runNav(emitter.emitFunction("MyFunc", List.of("xs"), first), "my_func([3, 1, 2])"));
        assertEquals("[2, 1, 3]", runNav(emitter.emitFunction("MyFunc", List.of("xs"), reverse), "my_func([3, 1, 2])"));
        assertEquals("[3, 1, 2]",
                runNav(emitter.emitFunction("MyFunc", List.of("xs"), distinct), "my_func([3, 1, 2, 3])"));
    }

    @Test
    void emitsAndRunsActualAdapterFunctionCall() throws Exception {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT),
                "rune-dsl builtins absent (" + BUILTINS_ROOT.toAbsolutePath() + ") — skipped");
        RWorkspace ws = workspace();
        ExpressionToIRAdapter adapter = new ExpressionToIRAdapter();

        // A REAL function call to the Rune function `Sub` (emits as `sub(x, y)`, step-11) — the same IRApply
        // the Java path consumes. The enclosing function's inputs x,y are the args; the callee `Sub` is a
        // DISTINCT RFunction with a non-meta scalar output
        // (adaptApply requires output().isPresent()) + SINGLE-cardinality inputs (the faithful-emission case).
        RAttribute x = scalarParam("x");
        RAttribute y = scalarParam("y");
        functionWith(ws, x, y);
        RFunction callee = new RFunction();
        callee.setName("Sub");
        callee.inputs().add(scalarParam("a"));
        callee.inputs().add(scalarParam("b"));
        callee.setOutput(scalarParam("out"));
        callee.attachToWorkspace(ws);
        RSymbolReference call = new RSymbolReference();
        call.setName("Sub");
        call.setResolvedSymbol(callee);
        call.args().add(paramRef("x", x, ws));
        call.args().add(paramRef("y", y, ws));
        call.attachToWorkspace(ws);

        IRExpr ir = adapter.adapt(call, ws).orElseThrow();
        assertEquals(IRExprKind.APPLY, ir.kind(), "the adapter produced a real IRApply");
        assertEquals("sub(x, y)", emitter.emit(ir));

        Assumptions.assumeTrue(pythonAvailable(), "python not on PATH — run skipped");
        // Run with a non-commutative stub so the arg order is proven (10 - 3 = 7, a swap would give -7).
        String prog = "def sub(a, b):\n    return a - b\n" + emitter.emitFunction("MyFunc", List.of("x", "y"), ir);
        assertEquals("7", runNav(prog, "my_func(10, 3)"));
    }

    @Test
    void emitsAndRunsActualAdapterArithmetic() throws Exception {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT),
                "rune-dsl builtins absent (" + BUILTINS_ROOT.toAbsolutePath() + ") — skipped");
        RWorkspace ws = workspace();
        ExpressionToIRAdapter adapter = new ExpressionToIRAdapter();

        // `n1 + n2` and `n1 / n2`: REAL adapter arithmetic BinaryOps over two scalar params.
        RAttribute n1 = scalarParam("n1");
        RAttribute n2 = scalarParam("n2");
        functionWith(ws, n1, n2);
        IRExpr add = adapter.adapt(arithmeticExpr(ArithOp.PLUS, "n1", n1, "n2", n2, ws), ws).orElseThrow();
        assertEquals("(n1 + n2)", emitter.emit(add));
        IRExpr div = adapter.adapt(arithmeticExpr(ArithOp.DIVIDE, "n1", n1, "n2", n2, ws), ws).orElseThrow();
        assertEquals("_div(n1, n2)", emitter.emit(div));

        Assumptions.assumeTrue(pythonAvailable(), "python not on PATH — run skipped");
        // the real adapter-produced `n1 / n2` runs at DECIMAL128 precision (the non-circular cross-target proof)
        String divFunc = emitter.emitFunction("MyFunc", List.of("n1", "n2"), div);
        assertEquals("Decimal('0.3333333333333333333333333333333333')", runNav(divFunc, "my_func(1, 3)"));
        assertEquals("5", runNav(emitter.emitFunction("MyFunc", List.of("n1", "n2"), add), "my_func(2, 3)"));
    }

    @Test
    void emitsActualAdapterThenChainToLet() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT),
                "rune-dsl builtins absent (" + BUILTINS_ROOT.toAbsolutePath() + ") — skipped");
        RWorkspace ws = workspace();
        ExpressionToIRAdapter adapter = new ExpressionToIRAdapter();

        // `(a + b) then item`: a REAL then-chain desugared by adaptThenChainToLet (a byte-inert SEPARATE entry
        // point — the live `adapt` has no RThenExpr arm, so the Java path never sees a Let). The bare-item
        // continuation renames to the binder; the VALUE is a real arithmetic BinaryOp. .orElseThrow() +
        // assertEquals(LET) make a decline a HARD failure, never a silently-skipped assertion.
        RAttribute a = scalarParam("a");
        RAttribute b = scalarParam("b");
        functionWith(ws, a, b);
        IRExpr sumLet = adapter.adaptThenChainToLet(
                thenItem(arithmeticExpr(ArithOp.PLUS, "a", a, "b", b, ws), ws), NodeId.ROOT, ws).orElseThrow();
        assertEquals(IRExprKind.LET, sumLet.kind(), "the adapter produced a real Let");
        assertEquals("def my_func(a, b):\n    _then0 = (a + b)\n    return _then0",
                emitter.emitFunction("MyFunc", List.of("a", "b"), sumLet));

        // `(foo -> bar) then item`: binds a real NAVIGATION value (None-propagating) with an identity continuation.
        RAttribute foo = scalarParam("foo");
        functionWith(ws, foo);
        RAttribute bar = scalarParam("bar");
        IRExpr navLet = adapter.adaptThenChainToLet(
                thenItem(featureCall("foo", foo, "bar", bar, ws), ws), NodeId.ROOT, ws).orElseThrow();
        assertEquals("def my_func(foo):\n    _then0 = _get(foo, 'bar')\n    return _then0",
                emitter.emitFunction("MyFunc", List.of("foo"), navLet));

        // `n then item then item`: a real CHAIN → two nested Lets → TWO ordered bindings, innermost = _then0.
        // The two-binding emission text is the load-bearing assert (a run value alone can't tell a flattened
        // chain from a collapsed one); the binder NUMBERING (inner _then0, outer _then1) is pinned too.
        RAttribute n = scalarParam("n");
        functionWith(ws, n);
        RThenExpr chainAst = thenItem(thenItem(paramRef("n", n, ws), ws), ws);
        IRExpr chainLet = adapter.adaptThenChainToLet(chainAst, NodeId.ROOT, ws).orElseThrow();
        Let outer = assertInstanceOf(Let.class, chainLet);
        assertEquals("_then1", outer.binder(), "outermost binder");
        Let inner = assertInstanceOf(Let.class, outer.value(), "the outer Let's value is the inner Let");
        assertEquals("_then0", inner.binder(), "innermost binder");
        assertEquals("def my_func(n):\n    _then0 = n\n    _then1 = _then0\n    return _then1",
                emitter.emitFunction("MyFunc", List.of("n"), chainLet));
    }

    @Test
    void runsActualAdapterThenChain() throws Exception {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT),
                "rune-dsl builtins absent (" + BUILTINS_ROOT.toAbsolutePath() + ") — skipped");
        RWorkspace ws = workspace();
        ExpressionToIRAdapter adapter = new ExpressionToIRAdapter();

        RAttribute a = scalarParam("a");
        RAttribute b = scalarParam("b");
        functionWith(ws, a, b);
        String sumFunc = emitter.emitFunction("MyFunc", List.of("a", "b"), adapter.adaptThenChainToLet(
                thenItem(arithmeticExpr(ArithOp.PLUS, "a", a, "b", b, ws), ws), NodeId.ROOT, ws).orElseThrow());

        RAttribute foo = scalarParam("foo");
        functionWith(ws, foo);
        RAttribute bar = scalarParam("bar");
        String navFunc = emitter.emitFunction("MyFunc", List.of("foo"), adapter.adaptThenChainToLet(
                thenItem(featureCall("foo", foo, "bar", bar, ws), ws), NodeId.ROOT, ws).orElseThrow());

        Assumptions.assumeTrue(pythonAvailable(), "python not on PATH — run skipped");
        // (a + b) then item → bind the sum, return it
        assertEquals("5", runNav(sumFunc, "my_func(2, 3)"));
        // (foo -> bar) then item → the L-082 absorbing-monoid headline INSIDE a Let: an absent bar → None
        assertEquals("None", runNav(navFunc, "my_func(SimpleNamespace(bar=None))"));
        assertEquals("42", runNav(navFunc, "my_func(SimpleNamespace(bar=42))"));
    }

    // ---- conditional (if-then-else, step-12 task-2): same-tree oracle (skips without builtins) ----

    @Test
    void emitsAndRunsActualAdapterNoElseMultiConditional() throws Exception {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT),
                "rune-dsl builtins absent (" + BUILTINS_ROOT.toAbsolutePath() + ") — skipped");
        RWorkspace ws = workspace();
        ExpressionToIRAdapter adapter = new ExpressionToIRAdapter();

        // `if trade->active then trade->legs` (legs MULTI, else omitted) — a REAL adapter-produced no-else
        // conditional whose then-branch is MULTI. CardinalityComputer stamps every RConditionalExpr SINGLE
        // (conservative), so a correctly-emitted `else []` (not `else None`) end-to-end proves the arm
        // computes the branch join itself (D4) and the emitter keys the no-else absent value on it (D7).
        RAttribute trade = scalarParam("trade");
        functionWith(ws, trade);
        RAttribute active = scalarParam("active");
        RAttribute legs = multiParam("legs");
        RConditionalExpr cond = conditionalExpr(
                featureCall("trade", trade, "active", active, ws),
                featureCall("trade", trade, "legs", legs, ws), null, ws);

        IRExpr ir = adapter.adapt(cond, ws).orElseThrow();
        assertEquals(ExpressionCardinality.MULTI, ir.cardinality(),
                "a MULTI then-branch drives the conditional MULTI (D4 then-MULTI disjunct)");
        assertEquals("(_get_all(trade, 'legs') if _get(trade, 'active') else [])", emitter.emit(ir));

        String func = emitter.emitFunction("MyFunc", List.of("trade"), ir);
        assertEquals("def my_func(trade):\n"
                + "    return (_get_all(trade, 'legs') if _get(trade, 'active') else [])", func);

        Assumptions.assumeTrue(pythonAvailable(), "python not on PATH — run skipped");
        // true branch → the navigated list
        assertEquals("[1, 2]", runNav(func, "my_func(SimpleNamespace(active=True, legs=[1, 2]))"));
        // false branch → [] wrapped in len(...) == 0, NOT None: if the arm degenerately stored SINGLE
        // (ignoring the then-branch's MULTI cardinality), this would emit `else None` and len(None) would
        // raise a TypeError — this is the D4-branch-join-reaches-D7 end-to-end proof.
        assertEquals("0", runNav(func, "len(my_func(SimpleNamespace(active=False, legs=[1, 2])))"));
    }

    @Test
    void emitsAndRunsActualAdapterWithElseScalarConditional() throws Exception {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT),
                "rune-dsl builtins absent (" + BUILTINS_ROOT.toAbsolutePath() + ") — skipped");
        RWorkspace ws = workspace();
        ExpressionToIRAdapter adapter = new ExpressionToIRAdapter();

        // `if trade->active then trade->foo else trade->bar` — a REAL adapter-produced genuine-else
        // conditional, both branches SINGLE — the base D6 ternary, selecting the correct branch for both
        // condition polarities.
        RAttribute trade = scalarParam("trade");
        functionWith(ws, trade);
        RAttribute active = scalarParam("active");
        RAttribute foo = scalarParam("foo");
        RAttribute bar = scalarParam("bar");
        RConditionalExpr cond = conditionalExpr(
                featureCall("trade", trade, "active", active, ws),
                featureCall("trade", trade, "foo", foo, ws),
                featureCall("trade", trade, "bar", bar, ws), ws);

        IRExpr ir = adapter.adapt(cond, ws).orElseThrow();
        assertEquals(ExpressionCardinality.SINGLE, ir.cardinality(), "both branches SINGLE (D4)");
        assertEquals("(_get(trade, 'foo') if _get(trade, 'active') else _get(trade, 'bar'))", emitter.emit(ir));

        String func = emitter.emitFunction("MyFunc", List.of("trade"), ir);
        Assumptions.assumeTrue(pythonAvailable(), "python not on PATH — run skipped");
        assertEquals("1", runNav(func, "my_func(SimpleNamespace(active=True, foo=1, bar=2))"));
        assertEquals("2", runNav(func, "my_func(SimpleNamespace(active=False, foo=1, bar=2))"));
    }

    @Test
    void emitsAndRunsActualAdapterOptionalConditionSelectsElse() throws Exception {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT),
                "rune-dsl builtins absent (" + BUILTINS_ROOT.toAbsolutePath() + ") — skipped");
        RWorkspace ws = workspace();
        ExpressionToIRAdapter adapter = new ExpressionToIRAdapter();

        // `if trade->flag then 1 else 2` (flag a REAL adapter-produced (0..1) OPTIONAL boolean-nav
        // condition) — the faithful half of D8 (an OPTIONAL condition lowers, it does NOT decline): Rune's
        // ControlFlowHandler.getOrDefault(false) selects the else on an absent condition; Python's None is
        // falsy, so `1 if None else 2` already agrees — no decline, no propagating helper needed.
        RAttribute trade = scalarParam("trade");
        functionWith(ws, trade);
        RAttribute flag = optionalParam("flag");
        RConditionalExpr cond = conditionalExpr(
                featureCall("trade", trade, "flag", flag, ws), astInt(1), astInt(2), ws);

        IRConditional ir = assertInstanceOf(IRConditional.class, adapter.adapt(cond, ws).orElseThrow());
        assertEquals(Optionality.OPTIONAL, ir.condition().optionality(),
                "flag is a real adapter-produced OPTIONAL condition");
        assertEquals("(1 if _get(trade, 'flag') else 2)", emitter.emit(ir));

        String func = emitter.emitFunction("MyFunc", List.of("trade"), ir);
        Assumptions.assumeTrue(pythonAvailable(), "python not on PATH — run skipped");
        assertEquals("1", runNav(func, "my_func(SimpleNamespace(flag=True))"));
        // the headline: an ABSENT condition (None) selects the else — matching getOrDefault(false).
        assertEquals("2", runNav(func, "my_func(SimpleNamespace(flag=None))"));
        assertEquals("2", runNav(func, "my_func(SimpleNamespace(flag=False))"));
    }

    // ---- helpers --------------------------------------------------------------------------

    /**
     * A synthetic single-result {@link FieldAccess} for the emission/run oracles (OPTIONAL, since
     * navigation may be absent). The same-tree oracle uses real adapter-produced records instead.
     */
    private static FieldAccess fieldAccess(IRExpr receiver, String feature) {
        return new FieldAccess(receiver, feature, NodeId.ROOT, RMetaAnnotatedType.MISSING,
                ExpressionCardinality.SINGLE, ExpressionCardinality.SINGLE, Optionality.OPTIONAL,
                SourceRange.NONE);
    }

    private static IRLiteral intLit(long value) {
        return lit(IRLiteral.LiteralKind.INT, BigInteger.valueOf(value));
    }

    private static IRLiteral lit(IRLiteral.LiteralKind kind, Object value) {
        return new IRLiteral(kind, value, NodeId.ROOT, RMetaAnnotatedType.MISSING,
                ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
    }

    private static IREmptyLiteral empty() {
        return new IREmptyLiteral(IREmptyLiteral.EmptySource.USER_EMPTY, NodeId.ROOT,
                RMetaAnnotatedType.MISSING, ExpressionCardinality.SINGLE, Optionality.OPTIONAL, SourceRange.NONE);
    }

    private static BinaryOp cmp(BinaryOp.BinOp op, IRExpr left, IRExpr right) {
        return new BinaryOp(op, left, right, NodeId.ROOT, RMetaAnnotatedType.MISSING,
                ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
    }

    private static IRVariable var(String name) {
        return new IRVariable(name, IRVariable.VariableKind.PARAM, NodeId.ROOT, RMetaAnnotatedType.MISSING,
                ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
    }

    private static IRVariable multiVar(String name) {
        return new IRVariable(name, IRVariable.VariableKind.PARAM, NodeId.ROOT, RMetaAnnotatedType.MISSING,
                ExpressionCardinality.MULTI, Optionality.PRESENT, SourceRange.NONE);
    }

    /** A scalar OPTIONAL ({@code [0..1]}) param reference — an absent-capable arithmetic operand. */
    private static IRVariable optionalVar(String name) {
        return new IRVariable(name, IRVariable.VariableKind.PARAM, NodeId.ROOT, RMetaAnnotatedType.MISSING,
                ExpressionCardinality.SINGLE, Optionality.OPTIONAL, SourceRange.NONE);
    }

    /** A synthetic unmodified existence check over an operand (the node is a total SINGLE boolean). */
    private static Existence exists(Existence.ExistOp op, IRExpr arg) {
        return new Existence(op, null, arg, NodeId.ROOT, RMetaAnnotatedType.MISSING,
                ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
    }

    /** A synthetic existence check carrying a {@code single}/{@code multiple} cardinality modifier. */
    private static Existence existsMod(Existence.ExistOp op, Existence.ExistMod mod, IRExpr arg) {
        return new Existence(op, mod, arg, NodeId.ROOT, RMetaAnnotatedType.MISSING,
                ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
    }

    /**
     * A synthetic flat list-op over a child. The node's own cardinality is not load-bearing for emission
     * (the emitter reads only {@code op()} + the child); the same-tree tests pin the real adapter cardinality.
     */
    private static IRListOp listOp(IRListOp.Kind op, IRExpr child) {
        return new IRListOp(op, child, NodeId.ROOT, RMetaAnnotatedType.MISSING,
                ExpressionCardinality.MULTI, Optionality.PRESENT, SourceRange.NONE);
    }

    /** A genuine ENUM_VALUE reference: type() wraps a real REnumTypeRef named `enumName`; target() is `member`. */
    private static IRReference enumValueRef(String enumName, String member) {
        REnumeration ast = new REnumeration();
        ast.setName(enumName);
        RMetaAnnotatedType type = RMetaAnnotatedType.withNoMeta(new REnumTypeRef(ast));
        return new IRReference(member, IRReference.ReferenceKind.ENUM_VALUE, NodeId.ROOT,
                type, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
    }

    /** A synthetic function call — an {@link IRApply} with an {@link IRReference.ReferenceKind#FUNCTION} callee. */
    private static IRApply apply(String calleeName, IRExpr... args) {
        IRReference callee = new IRReference(calleeName, IRReference.ReferenceKind.FUNCTION, NodeId.ROOT,
                RMetaAnnotatedType.MISSING, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        return new IRApply(callee, List.of(args), NodeId.ROOT, RMetaAnnotatedType.MISSING,
                ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
    }

    /**
     * A synthetic {@code let binder = value in in} (the desugared then-pipe). The same-tree tests feed the REAL
     * adapter's {@code adaptThenChainToLet} output; these synthetic helpers exercise the emitter's
     * TRANSFORMING-body flatten path the live adapter cannot yet produce (it declines an item-transforming
     * then-body) — the documented limitation, precedented by {@link #emitsComparisonOfTwoLiterals}'s synthetic tree.
     */
    private static Let letNode(String binder, IRExpr value, IRExpr in) {
        return new Let(binder, value, in, NodeId.ROOT, RMetaAnnotatedType.MISSING,
                ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
    }

    /** A let-binder reference — the continuation references the binder via a {@code LET_BINDER} variable. */
    private static IRVariable letBinder(String name) {
        return new IRVariable(name, IRVariable.VariableKind.LET_BINDER, NodeId.ROOT, RMetaAnnotatedType.MISSING,
                ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
    }

    /**
     * A synthetic {@link IRConditional} (the 8-arg ctor per {@code
     * IRPrinterExpressionTest.printsConditionalWithElseBranch}). {@code cardinality}/{@code optionality} are
     * parameters, not fixed constants like the other synthetic-node helpers above, because the D8/D10 guards
     * in {@code emitConditional} key on the condition's and branches' OWN cardinality — a caller must choose
     * them deliberately so the intended guard (or the plain emit path) is the one actually exercised.
     */
    private static IRConditional conditional(IRExpr condition, IRExpr thenBranch, IRExpr elseBranch,
            ExpressionCardinality cardinality, Optionality optionality) {
        return new IRConditional(condition, thenBranch, elseBranch, NodeId.ROOT, RMetaAnnotatedType.MISSING,
                cardinality, optionality, SourceRange.NONE);
    }

    // --- AST builders for the real adapter-produced comparison (mirror ExpressionToIRAdapterTest) ---

    private static RIntLiteral astInt(int value) {
        RIntLiteral lit = new RIntLiteral();
        lit.setValue(value);
        return lit;
    }

    private static RAttribute scalarParam(String name) {
        RAttribute attr = new RAttribute();
        attr.setName(name);
        RCardinality card = new RCardinality();
        card.setInf(1);
        card.setSup(1);
        attr.setCardinality(card);
        return attr;
    }

    /** A single MULTI attribute {@code (0..*)} — accumulated-MULTI navigation result. */
    private static RAttribute multiParam(String name) {
        return attribute(name, cardinality(0, true));
    }

    /** An optional single attribute {@code (0..1)} — single cardinality, optional optionality. */
    private static RAttribute optionalParam(String name) {
        return attribute(name, cardinality(0, false));
    }

    private static RAttribute attribute(String name, RCardinality cardinality) {
        RAttribute attr = new RAttribute();
        attr.setName(name);
        attr.setCardinality(cardinality);
        return attr;
    }

    private static RCardinality cardinality(int inf, boolean unbounded) {
        RCardinality card = new RCardinality();
        card.setInf(inf);
        if (unbounded) {
            card.setUnbounded(true);     // (inf..*)
        } else {
            card.setSup(1);              // (inf..1)
        }
        return card;
    }

    /** A single-hop feature navigation {@code receiver -> feature} over an already-wired param receiver. */
    private static RFeatureCall featureCall(String recvName, RAttribute recv,
            String featureName, RAttribute feature, RWorkspace ws) {
        return hop(paramRef(recvName, recv, ws), featureName, feature, ws);
    }

    /** A feature navigation hop {@code receiver -> feature} over an already-built receiver expression
     *  (the receiver may itself be a navigation, building a chain {@code a -> b -> c}). */
    private static RFeatureCall hop(RExpression receiver, String featureName, RAttribute feature, RWorkspace ws) {
        RFeatureCall fc = new RFeatureCall();
        fc.setReceiver(receiver);
        fc.setFeatureName(featureName);
        fc.setResolvedFeature(feature);
        fc.attachToWorkspace(ws);
        return fc;
    }

    private static RFunction functionWith(RWorkspace ws, RAttribute... inputs) {
        RFunction func = new RFunction();
        func.setName("MyFunc");
        for (RAttribute in : inputs) {
            func.inputs().add(in);
            in.setParent(func);
            in.attachToWorkspace(ws);
        }
        func.attachToWorkspace(ws);
        return func;
    }

    private static RSymbolReference paramRef(String name, RAttribute resolved, RWorkspace ws) {
        RSymbolReference ref = new RSymbolReference();
        ref.setName(name);
        ref.setResolvedSymbol(resolved);
        ref.attachToWorkspace(ws);
        return ref;
    }

    /** A param/param ordered comparison over two already-wired function-input attributes. */
    private static RComparisonExpr comparisonExpr(CompOp op, String leftName, RAttribute left,
            String rightName, RAttribute right, RWorkspace ws) {
        RComparisonExpr cmp = new RComparisonExpr();
        cmp.setOp(op);
        cmp.setLeft(paramRef(leftName, left, ws));
        cmp.setRight(paramRef(rightName, right, ws));
        cmp.attachToWorkspace(ws);
        return cmp;
    }

    /** An existence/absence check over an already-wired function-input attribute. */
    private static RExistenceExpr existenceExpr(ExistenceOp op, String name, RAttribute attr, RWorkspace ws) {
        return existenceExpr(op, null, name, attr, ws);
    }

    /** An existence check carrying a {@code single}/{@code multiple} modifier over a wired function input. */
    private static RExistenceExpr existenceExpr(ExistenceOp op, ExistsModifier mod, String name,
            RAttribute attr, RWorkspace ws) {
        RExistenceExpr e = new RExistenceExpr();
        e.setOp(op);
        if (mod != null) {
            e.setModifier(mod);
        }
        e.setArgument(paramRef(name, attr, ws));
        e.attachToWorkspace(ws);
        return e;
    }

    /** A flat list operation ({@code distinct}/{@code first}/{@code last}/{@code reverse}) over a built receiver. */
    private static RListOpExpr listOpExpr(ListOp op, RExpression argument, RWorkspace ws) {
        RListOpExpr e = new RListOpExpr();
        e.setOp(op);
        e.setArgument(argument);
        e.attachToWorkspace(ws);
        return e;
    }

    /** A {@code count} over a built receiver. */
    private static RCountExpr countExpr(RExpression argument, RWorkspace ws) {
        RCountExpr e = new RCountExpr();
        e.setArgument(argument);
        e.attachToWorkspace(ws);
        return e;
    }

    /** A param/param arithmetic operation over two already-wired function-input attributes. */
    private static RArithmeticExpr arithmeticExpr(ArithOp op, String leftName, RAttribute left,
            String rightName, RAttribute right, RWorkspace ws) {
        RArithmeticExpr arith = new RArithmeticExpr();
        arith.setOp(op);
        arith.setLeft(paramRef(leftName, left, ws));
        arith.setRight(paramRef(rightName, right, ws));
        arith.attachToWorkspace(ws);
        return arith;
    }

    /**
     * A conditional {@code if condition then thenBranch (else elseBranch)?} over already-built branch
     * expressions (each may be a {@code featureCall}, an {@code astInt}, etc.). A {@code null} {@code
     * elseBranch} builds the else-less form (never calls {@code setElseBranch}, so the adapter sees a
     * genuine no-else — D2). Mirrors the {@code comparisonExpr}/{@code arithmeticExpr}/{@code existenceExpr}
     * AST-builder helpers so the same-tree conditional oracles don't repeat the new/set/attach boilerplate.
     */
    private static RConditionalExpr conditionalExpr(RExpression condition, RExpression thenBranch,
            RExpression elseBranch, RWorkspace ws) {
        RConditionalExpr cond = new RConditionalExpr();
        cond.setCondition(condition);
        cond.setThenBranch(thenBranch);
        if (elseBranch != null) {
            cond.setElseBranch(elseBranch);
        }
        cond.attachToWorkspace(ws);
        return cond;
    }

    /**
     * A {@code <argument> then item} — a {@code then} whose body is an IMPLICIT single-{@code item} inline
     * function returning the bare {@code item} keyword (the only then-body the live adapter lowers; an
     * item-transforming body declines, see the slice design note). Nesting it as {@code argument} builds a
     * chain {@code e0 then item then item}.
     */
    private static RThenExpr thenItem(RExpression argument, RWorkspace ws) {
        RThenExpr then = new RThenExpr();
        then.setArgument(argument);
        RInlineFunction lambda = new RInlineFunction();
        lambda.setImplicit(true);
        lambda.setBody(item(ws));
        then.setBody(lambda);
        then.attachToWorkspace(ws);
        return then;
    }

    /** The implicit {@code item} keyword (non-synthetic), attached to the workspace. */
    private static RImplicitVariable item(RWorkspace ws) {
        RImplicitVariable item = new RImplicitVariable();
        item.setSynthetic(false);
        item.attachToWorkspace(ws);
        return item;
    }

    /** The shared oracle's gate (#630): {@code python --version} exits 0 within 10 s. */
    private static boolean pythonAvailable() {
        return PythonOracle.pythonAvailable();
    }

    /**
     * Runs a python invocation through the shared {@link PythonOracle} (#630: its {@value PythonOracle#TIMEOUT_SECONDS} s budget,
     * its concurrent drain, the elapsed and the partial output on a timeout — the private 30 s read-after-wait runner this replaces
     * fired on this box's process-start tail in two of three chains of record) and returns the stripped merged output.
     */
    private static String runPythonProcess(ProcessBuilder pb, String label) throws Exception {
        return PythonOracle.run(pb, label).output().strip();
    }

    /** Evaluates a single Python expression and returns {@code repr(value)} from stdout. */
    private static String runPython(String pyExpr) throws Exception {
        // Supply the emitter's OWN declared preamble (not a hidden harness assumption) so NUMBER's
        // Decimal('...') resolves; everything else is self-contained.
        String program = IRPythonEmitter.MODULE_PREAMBLE + "\nprint(repr(" + pyExpr + "))";
        return runPythonProcess(new ProcessBuilder("python", "-c", program), pyExpr);
    }

    /**
     * Defines the emitter's navigation function (with its declared {@link IRPythonEmitter#NAV_HELPER} +
     * {@link IRPythonEmitter#LIST_NAV_HELPER} + {@link IRPythonEmitter#MAP_NAV_HELPER} +
     * {@link IRPythonEmitter#FLATTEN_NAV_HELPER} + {@link IRPythonEmitter#MODULE_PREAMBLE}, plus
     * {@code SimpleNamespace} as the runtime object) and evaluates {@code call}, returning
     * {@code repr(value)} from stdout. All four nav helpers are injected in dependency order
     * ({@code _get} before {@code _map}; {@code _get_all} before {@code _flat_map}) so a deep chain mixing
     * scalar, list, flat-map and flatten navigation resolves; the helpers are the emitter's OWN declared
     * dependencies — not a hidden harness assumption.
     */
    private static String runNav(String functionDef, String call) throws Exception {
        String program = IRPythonEmitter.MODULE_PREAMBLE + "\n"
                + "from types import SimpleNamespace\n"
                + IRPythonEmitter.NAV_HELPER + "\n"
                + IRPythonEmitter.LIST_NAV_HELPER + "\n"
                + IRPythonEmitter.MAP_NAV_HELPER + "\n"
                + IRPythonEmitter.FLATTEN_NAV_HELPER + "\n"
                + IRPythonEmitter.DIV_HELPER + "\n"
                + IRPythonEmitter.FIRST_HELPER + "\n"
                + IRPythonEmitter.LAST_HELPER + "\n"
                + IRPythonEmitter.DISTINCT_HELPER + "\n"
                + functionDef + "\n"
                + "print(repr(" + call + "))";
        return runPythonProcess(new ProcessBuilder("python", "-c", program), call);
    }

    /** A STRUCT {@code Foo} with one keyword field {@code global} of builtin type {@code int} (so the emitted class needs no preamble). */
    private static IRTypeNode declStructWithGlobalIntField() {
        IRType intRef = new IRTypeNode("int", IRKind.STRUCT, List.of(), Optional.empty(),
                false, Optional.empty(), IRMetadata.EMPTY);
        IRField field = new IRFieldNode("global", intRef, Cardinality.ONE_TO_ONE, Optional.empty(), IRMetadata.EMPTY);
        return new IRTypeNode("Foo", IRKind.STRUCT, List.of(field), Optional.empty(),
                false, Optional.empty(), IRMetadata.EMPTY);
    }

    /** Writes the program to a temp .py file and runs it (a temp file, not {@code python -c}, dodges any
     *  Windows command-line quoting of the program payload), returning stripped stdout. */
    private static String runProgram(String program) throws Exception {
        Path tmp = Files.createTempFile("xemit_", ".py");
        try {
            Files.writeString(tmp, program, StandardCharsets.UTF_8);
            return runPythonProcess(new ProcessBuilder("python", tmp.toString()), "program");
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    /** Vendored rune-dsl builtins, sibling module (mirrors {@code ExpressionToIRAdapterTest}). */
    private static final Path BUILTINS_ROOT =
            Path.of("../rune-dsl/rune-runtime/src/main/resources/model");

    private static RWorkspace cachedWorkspace;

    private static RWorkspace workspace() throws IOException {
        if (cachedWorkspace == null) {
            List<RModel> models = new ArrayList<>();
            try (Stream<Path> s = Files.walk(BUILTINS_ROOT)) {
                for (Path p : s.filter(x -> x.toString().endsWith(".rosetta")).sorted().toList()) {
                    models.add(AstBuilder.buildFromFile(p));
                }
            }
            cachedWorkspace = RWorkspace.build(models).workspace();
        }
        return cachedWorkspace;
    }
}
