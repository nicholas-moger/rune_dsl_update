package com.regnosys.rosetta.generator.java.expression.handlers;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.enums.ArithOp;
import com.regnosys.rosetta.ast.expressions.binary.RArithmeticExpr;
import com.regnosys.rosetta.ast.expressions.literals.RIntLiteral;
import com.regnosys.rosetta.ast.expressions.literals.RStringLiteral;
import com.regnosys.rosetta.ast.expressions.references.RImplicitVariable;
import com.regnosys.rosetta.ast.expressions.references.RSymbolReference;
import com.regnosys.rosetta.ast.expressions.unary.RCountExpr;
import com.regnosys.rosetta.ast.expressions.unary.RToStringExpr;
import com.regnosys.rosetta.ast.functions.RShortcut;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.expression.ExpressionCompiler;
import com.regnosys.rosetta.generator.java.expression.ExpressionContext;
import com.regnosys.rosetta.generator.java.expression.TypeCoercionService;
import com.regnosys.rosetta.generator.java.scoping.JavaStatementScope;
import com.regnosys.rosetta.generator.java.statement.builder.JavaExpression;
import com.regnosys.rosetta.generator.java.statement.builder.JavaStatementBuilder;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.generator.java.types.RJavaWithMetaValue;
import com.regnosys.rosetta.types.RBasicType;
import com.regnosys.rosetta.types.RMetaAnnotatedType;
import com.regnosys.rosetta.types.RRecordType;
import com.regnosys.rosetta.types.RType;
import com.regnosys.rosetta.types.RecordKind;
import com.rosetta.util.types.JavaClass;
import com.rosetta.util.types.JavaPrimitiveType;
import com.rosetta.util.types.JavaType;

import java.util.HashSet;
import java.util.Set;

/**
 * Handles code generation for arithmetic binary expressions.
 *
 * <p>Golden output pattern (verified against CDM golden files):
 * <pre>
 *   a + b  →  MapperMaths.&lt;BigDecimal, BigDecimal, BigDecimal&gt;add(MapperS.of(a), MapperS.of(b))
 *   a - b  →  MapperMaths.&lt;BigDecimal, BigDecimal, BigDecimal&gt;subtract(MapperS.of(a), MapperS.of(b))
 *   a * b  →  MapperMaths.&lt;BigDecimal, BigDecimal, BigDecimal&gt;multiply(MapperS.of(a), MapperS.of(b))
 *   a / b  →  MapperMaths.&lt;BigDecimal, BigDecimal, BigDecimal&gt;divide(MapperS.of(a), MapperS.of(b))
 * </pre>
 *
 * <p>Generic type parameters {@code <ResultType, OperandType, OperandType>} follow the
 * upstream law (facet numeric_literal_typing): the operand witnesses are the JOIN of
 * the operands' own types ({@code int ⊔ int → Integer}, any {@code number} →
 * {@code BigDecimal}) and the result witness is the expression's own type — equal to
 * the join except for {@code divide}, whose result is always {@code BigDecimal}
 * (golden's only mixed combo: {@code <BigDecimal, Integer, Integer>divide}). Operands
 * the typed walk cannot resolve decline to the legacy string-name heuristic
 * (Integer iff a resolved int operand and no number operand, else BigDecimal).
 */
public class ArithmeticHandler {

    /**
     * Compiles an {@link RArithmeticExpr} into a {@code MapperMaths} method call.
     *
     * @param expr     the arithmetic expression node
     * @param ctx      the current expression compilation context
     * @param compiler the parent compiler for recursive compilation of operands
     * @return a {@link JavaExpression} rendering the MapperMaths call
     */
    public JavaStatementBuilder handle(RArithmeticExpr expr, ExpressionContext ctx, ExpressionCompiler compiler) {
        // PR-A §9.1 C3a.4.a: refs flow structurally via JavaExpression.from
        // factory. No compiler.trackType(...) calls — the emission sites
        // below carry MAPPER_MATHS / MAPPER_S / BIG_DECIMAL explicitly and
        // union operand refs via getRefs().

        // Handle unary prefix: (PLUS | MINUS) expression — rawLeft() is null
        if (expr.rawLeft() == null) {
            if (expr.op() == ArithOp.PLUS) {
                // Unary plus is a no-op — pass through the operand
                return compiler.compile(expr.rawRight(), ctx.expectedType(), ctx.scope());
            }
            // Unary minus: check for the common pattern -(literal * expr) and rewrite
            // as (-literal * expr) to match the upstream golden output. The upstream
            // parser treats "-1 * arg" as multiply(-1, arg), but our grammar parses it
            // as negate(multiply(1, arg)).
            if (expr.rawRight() instanceof RArithmeticExpr inner
                    && (inner.op() == ArithOp.MULTIPLY || inner.op() == ArithOp.DIVIDE)
                    && inner.rawLeft() instanceof RIntLiteral lit) {
                // Negate the literal and re-render as binary: (-lit op right).
                long negValue = lit.value().negate().longValueExact();
                String method = inner.op() == ArithOp.MULTIPLY ? "multiply" : "divide";
                // facet negLiteral (PR #206): type the rewritten `(-lit) op expr` from the
                // JOIN of the inner operands (mirroring the binary path below / facet
                // numeric_literal_typing), not a blanket BigDecimal. When BOTH operands
                // resolve INT (neither is `number`), the operation is Integer-typed and the
                // negated literal renders BARE (`MapperS.of(-1)`) with NO `java.math.BigDecimal`
                // import — matching the golden (e.g. `-1 * shiftDays` over Integer
                // `shiftDays` in GenerateObservationPeriod). A `number` operand or ANY
                // unresolved operand DECLINES to the legacy BigDecimal form byte-verbatim.
                // divide's result is always BigDecimal (RosettaTypeProvider.caseDivideOperation).
                // The negated literal must fit a 32-bit int so the bare `MapperS.of(-N)` is a
                // valid Java int literal — a beyond-int value (still within long range, so
                // longValueExact above did not throw) would render an int-overflow compile
                // error, so it DECLINES to the BigDecimal branch (BigDecimal.valueOf(-N), valid
                // for any long — the pre-#206 behaviour). No corpus carrier exercises this
                // (every -(literal * expr) flip is a small literal), but the gate keeps the
                // Integer branch sound for an arbitrary RIntLiteral magnitude (Copilot R1).
                boolean intArithmetic = compiler.getTypeUtil() != null
                        && negValue >= Integer.MIN_VALUE && negValue <= Integer.MAX_VALUE
                        && HandlerHelper.numericOperandKind(lit, compiler) == HandlerHelper.NumericKind.INT
                        && HandlerHelper.numericOperandKind(inner.rawRight(), compiler) == HandlerHelper.NumericKind.INT;
                JavaExpression negLeftBuilder;
                String typeParams;
                // v3.1 C2d family 1 (numeric-literal-kind): whether the MapperMaths witness names
                // BigDecimal is decided HERE, beside each typeParams assignment, from the typed
                // branch that spells it - no longer recovered from the witness text.
                boolean bigDecimalWitness;
                JavaStatementBuilder rightBuilder;
                if (intArithmetic) {
                    negLeftBuilder = JavaExpression.wrappedInMapperSOf(
                            JavaExpression.from(Long.toString(negValue), null, Set.of()));
                    boolean divide = inner.op() == ArithOp.DIVIDE;
                    String resultType = divide ? "BigDecimal" : "Integer";
                    typeParams = "<" + resultType + ", Integer, Integer>";
                    bigDecimalWitness = divide;
                    rightBuilder = compiler.compile(inner.rawRight(),
                            compiler.getTypeUtil().INTEGER, ctx.scope());
                } else {
                    // Legacy BigDecimal form (byte-verbatim — preserves every green carrier).
                    negLeftBuilder = JavaExpression.wrappedInMapperSOf(
                            JavaExpression.from(
                                    "BigDecimal.valueOf(" + negValue + ")",
                                    null,
                                    Set.of(HandlerHelper.BIG_DECIMAL)));
                    typeParams = "<BigDecimal, BigDecimal, BigDecimal>";
                    bigDecimalWitness = true;
                    rightBuilder = compiler.compile(inner.rawRight(), ctx.expectedType(), ctx.scope());
                }
                Set<JavaClass<?>> refs = new HashSet<>();
                refs.add(HandlerHelper.MAPPER_MATHS);
                if (bigDecimalWitness) {
                    refs.add(HandlerHelper.BIG_DECIMAL);
                }
                refs.addAll(negLeftBuilder.getRefs());
                refs.addAll(rightBuilder.getRefs());
                Set<JavaClass<?>> wildcards = new HashSet<>();
                wildcards.addAll(negLeftBuilder.getStaticWildcardImports());
                wildcards.addAll(rightBuilder.getStaticWildcardImports());
                return JavaExpression.from(
                        "MapperMaths." + typeParams + method + "("
                                + negLeftBuilder.renderToString() + ", "
                                + HandlerHelper.render(rightBuilder) + ")",
                        null,
                        refs,
                        wildcards);
            }
            // facet bareNegLiteral (PR #351): a unary-negated BARE int literal (`-1`) renders
            // as the negated literal itself, mirroring LiteralHandler's context law — NOT the
            // general multiply-by--1 desugar below. Golden carriers: AddBusinessDays cdm5/cdm6
            // (`MapperS.of(-1)` at an Integer seat), MapPrincipalPayment's multiply operand
            // (`MapperS.of(BigDecimal.valueOf(-1))` at a number seat), GenerateDateList's
            // evaluate arg (`-1` raw via the wrappedInMapperSOf unwrap contract). The #206
            // rewrite arm above handles `-(lit * expr)`; this arm handles the literal ALONE.
            // Range gates per the #206 precedent: the bare int form needs an int-range value
            // (a valid Java int literal); the BigDecimal form needs long range
            // (BigDecimal.valueOf(long)). Beyond-range values DECLINE to the legacy multiply
            // form byte-verbatim (no corpus carrier — every bare negated literal is small).
            // Green-safe: goldens render multiply-by--1 ONLY for the `-(lit * expr)` shape
            // (Abs), which the rewrite arm owns — a bare RIntLiteral operand never rendered
            // green through the general path (the multiply(valueOf(-1), MapperS.of(<lit>))
            // form appears in ZERO goldens).
            if (expr.rawRight() instanceof RIntLiteral bareLit) {
                java.math.BigInteger negBig = bareLit.value().negate();
                boolean bigDecimalContext = false;
                if (ctx.expectedType() != null && compiler.getTypeUtil() != null) {
                    var tu = compiler.getTypeUtil();
                    if (tu.isBigDecimal(ctx.expectedType())) {
                        bigDecimalContext = true;
                    } else if (tu.isWrapper(ctx.expectedType())) {
                        var itemType = tu.getItemType(ctx.expectedType());
                        bigDecimalContext = itemType != null && tu.isBigDecimal(itemType);
                    }
                }
                if (bigDecimalContext && negBig.bitLength() <= 63) {
                    return JavaExpression.wrappedInMapperSOf(
                            JavaExpression.from(
                                    "BigDecimal.valueOf(" + negBig + ")",
                                    null,
                                    Set.of(HandlerHelper.BIG_DECIMAL)));
                }
                if (!bigDecimalContext
                        && negBig.compareTo(java.math.BigInteger.valueOf(Integer.MIN_VALUE)) >= 0
                        && negBig.compareTo(java.math.BigInteger.valueOf(Integer.MAX_VALUE)) <= 0) {
                    return JavaExpression.wrappedInMapperSOf(
                            JavaExpression.from(negBig.toString(), null, Set.of()));
                }
                // Beyond-range: fall through to the legacy multiply-by--1 form.
            }
            // General unary minus: render as multiply by -1
            JavaExpression negOneBuilder = JavaExpression.wrappedInMapperSOf(
                    JavaExpression.from(
                            "BigDecimal.valueOf(-1)",
                            null,
                            Set.of(HandlerHelper.BIG_DECIMAL)));
            JavaStatementBuilder unaryRightBuilder =
                    compiler.compile(expr.rawRight(), ctx.expectedType(), ctx.scope());
            String typeParams = "<BigDecimal, BigDecimal, BigDecimal>";
            Set<JavaClass<?>> refs = new HashSet<>();
            refs.add(HandlerHelper.MAPPER_MATHS);
            refs.addAll(negOneBuilder.getRefs());
            refs.addAll(unaryRightBuilder.getRefs());
            Set<JavaClass<?>> wildcards = new HashSet<>();
            wildcards.addAll(negOneBuilder.getStaticWildcardImports());
            wildcards.addAll(unaryRightBuilder.getStaticWildcardImports());
            return JavaExpression.from(
                    "MapperMaths." + typeParams + "multiply("
                            + negOneBuilder.renderToString() + ", "
                            + HandlerHelper.render(unaryRightBuilder) + ")",
                    null,
                    refs,
                    wildcards);
        }

        String method = switch (expr.op()) {
            case PLUS     -> "add";
            case MINUS    -> "subtract";
            case MULTIPLY -> "multiply";
            case DIVIDE   -> "divide";
        };

        // facet numeric_literal_typing (mechanism 2): upstream derives the operand
        // witnesses from the JOIN of the operands' OWN types and the result witness
        // from the expression's own type (ExpressionGenerator.binaryExpr:
        // `MapperMaths.<resultType, joined, joined>method(l, r)` with both operands
        // compiled against the join) — divide's result is ALWAYS number
        // (RosettaTypeProvider.caseDivideOperation = UNCONSTRAINED_NUMBER,
        // unconditionally), so int/int division renders golden's only mixed combo
        // `<BigDecimal, Integer, Integer>divide(MapperS.of(1), …)` with the int
        // literal BARE (the literal counts as int evidence in the join, unlike the
        // legacy heuristic below which deliberately ignores literals). The typed
        // per-operand walk (HandlerHelper.numericOperandKind) resolves literals,
        // attributes, function-call outputs, alias bodies, resolution-blind nav
        // leaves, and nested arithmetic; ANY unresolved operand DECLINES to the
        // FULL legacy heuristic byte-verbatim — never a blanket BigDecimal — so a
        // green line the heuristic resolves from one int operand (e.g. drr
        // PeriodCalculation's `<Integer, Integer, Integer>multiply`) cannot change.
        // Corpus law (frozen 9.83.0 baseline): golden witness combos are
        // exhaustively <BD,BD,BD> ×182 / <I,I,I> ×69 / <String,String,String> ×33
        // (add only; string operands decline to legacy here — a W42 decline-gate,
        // reach-only) / <BD,I,I> ×24 (divide only) — operand witnesses always equal.
        // ⚠ LOCKSTEP (facet fnIteHoistSeats, PR #361): the witness-selection
        // ladder from here through the legacy-heuristic else block is MIRRORED by
        // binaryResultItemJavaClass below (the #361 conditional-hoist decl type) —
        // any change to the selection MUST be applied to both.
        HandlerHelper.NumericKind leftKind =
                HandlerHelper.numericOperandKind(expr.rawLeft(), compiler);
        HandlerHelper.NumericKind rightKind = leftKind == HandlerHelper.NumericKind.UNKNOWN
                ? HandlerHelper.NumericKind.UNKNOWN
                : HandlerHelper.numericOperandKind(expr.rawRight(), compiler);
        // facet deepThenSentinelOperandTyping (PR #389): upgrade an UNKNOWN kind from
        // the render-truth then-step BINDING (MISSING-snapshot implicits only — the
        // #351 sentinel-bound item). The resolved kind routes the operand through the
        // #334 wrapper-level coercion, whose identity arm keeps the Mapper-typed
        // sentinel BARE (golden cdm6 StandardizedScheduleVarianceSwapNotionalAmount
        // `multiply(MapperS.of(new BigDecimal("0.01")), ifThenElseResult0)`) — the
        // item-level legacy path entry-coerced the non-compiling `.get()`. A
        // scope-dependent PRE-step, deliberately OUTSIDE the numericOperandKind
        // ladder (the binaryResultItemJavaClass LOCKSTEP above is untouched: a
        // scope-less consumer cannot read the binding, and this carrier's decl type
        // resolves through the handshake's own recovery, not the mirror).
        JavaTypeUtil bindTu = compiler.getTypeUtil();
        if (bindTu != null) {
            if (leftKind == HandlerHelper.NumericKind.UNKNOWN) {
                JavaType lb = HandlerHelper.implicitThenBindingItemType(
                        expr.rawLeft(), ctx.scope(), compiler);
                if (lb != null && bindTu.isBigDecimal(lb)) {
                    leftKind = HandlerHelper.NumericKind.NUMBER;
                } else if (lb != null && bindTu.isInteger(lb)) {
                    leftKind = HandlerHelper.NumericKind.INT;
                }
                if (leftKind != HandlerHelper.NumericKind.UNKNOWN) {
                    rightKind = HandlerHelper.numericOperandKind(expr.rawRight(), compiler);
                }
            }
            if (rightKind == HandlerHelper.NumericKind.UNKNOWN
                    && leftKind != HandlerHelper.NumericKind.UNKNOWN) {
                JavaType rb = HandlerHelper.implicitThenBindingItemType(
                        expr.rawRight(), ctx.scope(), compiler);
                if (rb != null && bindTu.isBigDecimal(rb)) {
                    rightKind = HandlerHelper.NumericKind.NUMBER;
                } else if (rb != null && bindTu.isInteger(rb)) {
                    rightKind = HandlerHelper.NumericKind.INT;
                }
            }
        }

        // facet arithStringJoin (PR #334): a `+` whose BOTH operands are provably
        // string-typed joins to String — upstream binaryExpr's NON-numeric branch
        // (`<resultType, leftType, rightType>` = `<String, String, String>`, each
        // operand compiled against MAPPER.wrapExtends(ownType)). The numeric walk
        // classifies string operands UNKNOWN, so this previously declined to the
        // legacy heuristic's <BigDecimal, BigDecimal, BigDecimal> — the documented
        // W42 reach-only decline. Corpus law (frozen 9.83.0 baseline): golden add
        // witness combos are exhaustively <BD,BD,BD> / <I,I,I> / <String,String,
        // String> / <BD,I,I(divide)>, so a green file cannot carry the BigDecimal
        // witness over string operands — the arm only ever touches waivered files.
        // Positive-evidence only (string literal / to-string / resolved `string`
        // attribute / a PLUS of such): a numeric or unresolved operand can never
        // classify string, and a mixed pair declines to legacy byte-verbatim.
        boolean stringJoin = compiler.getTypeUtil() != null
                && expr.op() == ArithOp.PLUS
                && (leftKind == HandlerHelper.NumericKind.UNKNOWN
                        || rightKind == HandlerHelper.NumericKind.UNKNOWN)
                && isStringOperand(expr.rawLeft(), compiler, ctx.scope())
                && isStringOperand(expr.rawRight(), compiler, ctx.scope());

        // facet dateArithWitness (W42 finding #3, PR #424): upstream's NON-numeric
        // arithmetic branch for the date−date day-count algebra
        // (ExpressionGenerator.binaryExpr L434-438: operands compile against their
        // OWN wrapped types, witness `<resultType, leftType, rightType>`; the result
        // type is the type provider's — the facet dateArithTyping arm types
        // date − date as int → Integer). POSITIVE-evidence gate: a MINUS whose BOTH
        // operands resolve to the `date` record through the shared three-channel
        // resolution (engine-inferred alias-stripped + alias transparency + the
        // disguised alias-body chain — see isDateOperand) — golden
        // `MapperMaths.<Integer, Date, Date>subtract(…)`, the expr-date-subtract
        // oracle group. date + time is HEALED by the adjacent dateTimeAdd arm
        // (PR #426 — the formerly documented follow-on); any unresolved or
        // non-date operand declines byte-verbatim. Operands compile ITEM-level
        // (the resolvedJoin wrapper-coerce channel is not joined): for plain
        // Date-typed navs both channels emit identical bytes (6/6 proven) — a
        // META-WRAPPED date operand has no witness yet and remains the open
        // follow-on (the meta gates decline it on every channel). Corpus census
        // (frozen 9.83.0 goldens): ZERO MapperMaths witnesses carry a date/time
        // type (87 BigDecimal-witness files / 120 MapperMaths files / 0 Date), so
        // the arm cannot touch a green file.
        boolean dateSubtract = compiler.getTypeUtil() != null
                && expr.op() == ArithOp.MINUS
                && (leftKind == HandlerHelper.NumericKind.UNKNOWN
                        || rightKind == HandlerHelper.NumericKind.UNKNOWN)
                && isDateOperand(expr.rawLeft(), compiler)
                && isDateOperand(expr.rawRight(), compiler);

        // facet dateTimeAddWitness (the #424 add-side follow-on, PR #426): upstream's
        // NON-numeric branch for date + time — the LEFT operand engine-resolves
        // (alias-stripped) to the `date` record, the RIGHT to the `time` basic —
        // witness `<LocalDateTime, Date, LocalTime>add` (the facet dateArithTyping arm
        // already types the result DATE_TIME; the render side previously fell to the
        // BigDecimal triple, NON-COMPILING over Date/LocalTime mappers). Byte witness:
        // the expr-date-time-add oracle group (×2 setters) + the two #425
        // upstream-witnessed leg-C carriers (target/425-expected/calc4/5.txt). Same
        // positive-evidence + meta gates as dateSubtract (one resolution law); any
        // unresolved, meta-wrapped or non-date/time operand declines byte-verbatim.
        // Corpus census (frozen 9.83.0 goldens, the #424 count): ZERO MapperMaths
        // witnesses carry a date/time type, so the arm cannot touch a green file.
        boolean dateTimeAdd = compiler.getTypeUtil() != null
                && expr.op() == ArithOp.PLUS
                && (leftKind == HandlerHelper.NumericKind.UNKNOWN
                        || rightKind == HandlerHelper.NumericKind.UNKNOWN)
                && isDateOperand(expr.rawLeft(), compiler)
                && isTimeOperand(expr.rawRight(), compiler);

        com.rosetta.util.types.JavaType operandExpectedType = null;
        com.rosetta.util.types.JavaType rightOperandExpectedType = null;
        String typeParams;
        // v3.1 C2d family 1 (numeric-literal-kind): whether the MapperMaths witness names BigDecimal
        // is decided beside each typeParams assignment, from the typed branch that spells it — the
        // BIG_DECIMAL ref below reads this boolean, no longer the witness text (the family's C2c
        // census: 2,249 arrivals on the default route / 1,291 on the IR route, text == typed on
        // every one).
        boolean bigDecimalWitness;
        if (stringJoin) {
            operandExpectedType = compiler.getTypeUtil().STRING;
            typeParams = "<String, String, String>";
            bigDecimalWitness = false;
        } else if (dateSubtract) {
            operandExpectedType = compiler.getTypeUtil().DATE;
            typeParams = "<Integer, Date, Date>";
            bigDecimalWitness = false;
        } else if (dateTimeAdd) {
            // Upstream compiles each operand against its OWN wrapped type
            // (ExpressionGenerator.binaryExpr L434-438) — asymmetric here: Date
            // left, LocalTime right (dateSubtract's symmetric DATE covers both
            // sides there; the rightOperandExpectedType channel below keeps every
            // other arm byte-verbatim on the shared expected).
            operandExpectedType = compiler.getTypeUtil().DATE;
            rightOperandExpectedType = compiler.getTypeUtil().LOCAL_TIME;
            typeParams = "<LocalDateTime, Date, LocalTime>";
            bigDecimalWitness = false;
        } else if (rightKind != HandlerHelper.NumericKind.UNKNOWN && compiler.getTypeUtil() != null) {
            boolean joinIsNumber = leftKind == HandlerHelper.NumericKind.NUMBER
                    || rightKind == HandlerHelper.NumericKind.NUMBER;
            String operandType = joinIsNumber ? "BigDecimal" : "Integer";
            String resultType = expr.op() == ArithOp.DIVIDE ? "BigDecimal" : operandType;
            operandExpectedType = joinIsNumber
                    ? compiler.getTypeUtil().BIG_DECIMAL
                    : compiler.getTypeUtil().INTEGER;
            typeParams = "<" + resultType + ", " + operandType + ", " + operandType + ">";
            bigDecimalWitness = joinIsNumber || expr.op() == ArithOp.DIVIDE;
        } else {
            // Legacy heuristic (decline-to-legacy path) — arithmetic is BigDecimal by
            // default, but for int-typed operands (from resolved attribute types, not
            // literals) use Integer. Literals alone don't determine the type — "1 + 2"
            // in a number context is BigDecimal.
            String leftRuneType = HandlerHelper.inferRuneTypeName(expr.rawLeft());
            String rightRuneType = HandlerHelper.inferRuneTypeName(expr.rawRight());
            boolean hasResolvedIntType = HandlerHelper.isResolvedIntType(leftRuneType, expr.rawLeft())
                    || HandlerHelper.isResolvedIntType(rightRuneType, expr.rawRight());
            boolean hasNumberType = HandlerHelper.isNumberType(leftRuneType)
                    || HandlerHelper.isNumberType(rightRuneType);
            boolean isIntArithmetic = hasResolvedIntType && !hasNumberType;

            String javaNumType = "BigDecimal";
            bigDecimalWitness = true;
            if (isIntArithmetic && compiler.getTypeUtil() != null) {
                operandExpectedType = compiler.getTypeUtil().INTEGER;
                javaNumType = "Integer";
                bigDecimalWitness = false;
                // Integer is java.lang — no import needed.
            } else if (compiler.getTypeUtil() != null) {
                operandExpectedType = compiler.getTypeUtil().BIG_DECIMAL;
            }
            typeParams = "<" + javaNumType + ", " + javaNumType + ", " + javaNumType + ">";
        }

        // facet arithOperandWrapperCoerce (PR #334): upstream compiles arithmetic
        // operands against the WRAPPER-level expected `MAPPER.wrapExtends(joined)`
        // (ExpressionGenerator.binaryExpr L430-443), so a TYPED operand coerces at
        // wrapper level — a meta-element chain (MapperS<FieldWithMetaString>) gets
        // the inline guarded `.<String>map("Type coercion", w -> w == null ? null :
        // w.getValue())` and an Integer-item wrapper in a BigDecimal join gets the
        // guarded `BigDecimal.valueOf` map — where the fork's ITEM-level expected
        // let compile()'s entry terminal coercion COLLAPSE the wrapper to the
        // non-compiling `.get().getValue()` deref (a bare value in a Mapper-typed
        // MapperMaths parameter — never green). RESOLVED-join operands compile
        // INTERIOR (the same threaded expected for every interior consumer, no
        // entry coercion) + the explicit wrapper-level coercion below; a null-typed
        // operand is byte-identical (the entry coercion was dormant on it), an
        // ITEM-typed operand keeps the pre-facet item-level coercion. The UNRESOLVED
        // (legacy-heuristic) path keeps compile() byte-verbatim.
        boolean resolvedJoin = operandExpectedType != null
                && (stringJoin || rightKind != HandlerHelper.NumericKind.UNKNOWN)
                && compiler.getCoercionService() != null;
        JavaStatementBuilder leftBuilder;
        JavaStatementBuilder rightBuilder;
        if (resolvedJoin) {
            leftBuilder = compileOperandWrapperCoerced(expr.rawLeft(), operandExpectedType, ctx, compiler);
            rightBuilder = compileOperandWrapperCoerced(expr.rawRight(), operandExpectedType, ctx, compiler);
        } else {
            leftBuilder = compiler.compile(expr.rawLeft(),  operandExpectedType, ctx.scope());
            // facet dateTimeAddWitness (PR #426): the asymmetric right expected —
            // LocalTime on the dateTimeAdd arm, the shared expected everywhere else.
            rightBuilder = compiler.compile(expr.rawRight(),
                    rightOperandExpectedType != null ? rightOperandExpectedType : operandExpectedType,
                    ctx.scope());
        }
        String left  = HandlerHelper.render(leftBuilder);
        String right = HandlerHelper.render(rightBuilder);

        // facet lambda_item_body_coercion (arm S1): a COUNT operand renders the
        // bare item-typed `<arg>.resultCount()` (an int — CollectionHandler's
        // RCountExpr emission carries a null expression type), but upstream
        // compiles arithmetic operands against MAPPER.wrapExtends(...) and the
        // item->MapperS coercion wraps it (TypeCoercionService
        // getItemToMapperSConversionExpression) — golden
        // `MapperMaths.<I,I,I>add(MapperS.of(x(...).resultCount()), ...)`.
        // Mirrors ComparisonHandler.wrapCountOperand: gated strictly on the raw
        // operand node being RCountExpr (a bare int in a Mapper-typed
        // MapperMaths parameter does not compile, so no green file carries the
        // unwrapped form; corpus law: 12/12 golden MapperMaths count operands
        // are MapperS.of-wrapped).
        boolean leftIsCount = expr.rawLeft() instanceof RCountExpr;
        boolean rightIsCount = expr.rawRight() instanceof RCountExpr;
        // facet countOperandBigDecimalValueOf (PR #332): a COUNT operand in a
        // BigDecimal-joined MapperMaths context coerces the primitive-int
        // `resultCount()` to the operand type INSIDE the wrap —
        // `MapperS.of(BigDecimal.valueOf(<x>.resultCount()))` (upstream compiles the
        // operand against the joined BigDecimal expected type; an int primitive
        // cannot be null, so no guard — golden cdm ResolveObservationAverage's
        // divide denominator). Corpus law (frozen 9.83.0 baseline): ZERO golden
        // count operands render bare inside a <BigDecimal,…> MapperMaths call —
        // every bare `MapperS.of(<x>.resultCount())` sits in an <Integer,…> context
        // — so the wrap cannot regress a green file.
        boolean countAsBigDecimal = compiler.getTypeUtil() != null
                && operandExpectedType != null
                && operandExpectedType.equals(compiler.getTypeUtil().BIG_DECIMAL);
        // v3.2 seat 1 (F2 dropped-coercion): the count operand is INT-typed since this seat, so
        // on a resolved join the typed coercion (compileOperandWrapperCoerced) has ALREADY
        // wrapped it — its builder reports a wrapper type — and the node-keyed text wrap must
        // yield or it double-wraps. PR #622's review round MEASURED the wrap's reach with a probe
        // at this decision (probe-countwrap.py: an unconditional print at every count-operand
        // arrival, both sides, both branches; applied to the commit-6 working tree at d8f1f0f9a
        // plus the then-uncommitted fixes and REVERTED - scratch/probe-s1c6.status names the
        // modified files) over all three walks: the default-route D11 over the 26 cells + the
        // seat fixtures, the IR-route D11, the optimised route - 72 / 72 / 40 arrivals, every one
        // on the RESOLVED join and already wrapped, so the text wrap declined at 100% of them and
        // the plain-compile branch saw NO count operand. The round-2 review proposed a valid shape
        // that could reach that branch - a count beside a LITERAL-bodied alias, which
        // numericOperandKind declines when the inference engine has no type for it - and the
        // fixture a8 measured it (c8-probe-a8.log): the engine types the alias, the join RESOLVES,
        // the count arrives wrapped. The BigDecimal-typed-item guard the branch once needed (an
        // entry coercion converting the count before the wrap) was DELETED on the corpus zero (the
        // cq review's SF-6) and stays deleted on the counter-input's measurement; a converted count
        // reaching the wrap would double `BigDecimal.valueOf` and not compile - LOUD at javac, an
        // UNMEASURED shape with no known valid producer. The text wrap itself is the pre-seat form,
        // kept for the plain-compile branch and byte-inert at the population; a retirement
        // candidate with its own census, not this seat's.
        JavaTypeUtil countTu = compiler.getTypeUtil();
        JavaType leftType = leftBuilder.getExpressionType();
        JavaType rightType = rightBuilder.getExpressionType();
        boolean leftAlreadyWrapped = countTu != null && leftType != null && countTu.isWrapper(leftType);
        boolean rightAlreadyWrapped = countTu != null && rightType != null && countTu.isWrapper(rightType);
        if (leftIsCount && !leftAlreadyWrapped) {
            left = countAsBigDecimal
                    ? "MapperS.of(BigDecimal.valueOf(" + left + "))"
                    : "MapperS.of(" + left + ")";
        }
        if (rightIsCount && !rightAlreadyWrapped) {
            right = countAsBigDecimal
                    ? "MapperS.of(BigDecimal.valueOf(" + right + "))"
                    : "MapperS.of(" + right + ")";
        }

        // Refs: always MAPPER_MATHS; BIG_DECIMAL when the witness names BigDecimal —
        // the boolean recorded beside each typeParams assignment above (the
        // all-BigDecimal paths, the mixed <BigDecimal, Integer, Integer> divide
        // result witness, and the typeUtil==null legacy-test edge case where
        // javaNumType defaults to "BigDecimal" regardless of isIntArithmetic).
        // StaticWildcardImports flow through from the operand builders so
        // nested comparison/existence expressions inside arithmetic operands
        // retain their ExpressionOperatorsNullSafe.* wildcard.
        Set<JavaClass<?>> refs = new HashSet<>();
        refs.add(HandlerHelper.MAPPER_MATHS);
        if (leftIsCount || rightIsCount) {
            refs.add(HandlerHelper.MAPPER_S);
        }
        if (bigDecimalWitness) {
            refs.add(HandlerHelper.BIG_DECIMAL);
        }
        // facet dateArithWitness: the <Integer, Date, Date> witness names the records
        // Date class (Integer is java.lang). Keyed on the arm, not a "Date" text
        // sniff — "LocalDateTime" would false-positive a substring match.
        if (dateSubtract) {
            refs.add(compiler.getTypeUtil().DATE);
        }
        // facet dateTimeAddWitness (PR #426): the <LocalDateTime, Date, LocalTime>
        // witness names all three non-java.lang classes — keyed on the arm, exactly
        // like the dateSubtract ref above.
        if (dateTimeAdd) {
            refs.add(compiler.getTypeUtil().DATE);
            refs.add(compiler.getTypeUtil().LOCAL_DATE_TIME);
            refs.add(compiler.getTypeUtil().LOCAL_TIME);
        }
        refs.addAll(leftBuilder.getRefs());
        refs.addAll(rightBuilder.getRefs());
        Set<JavaClass<?>> wildcards = new HashSet<>();
        wildcards.addAll(leftBuilder.getStaticWildcardImports());
        wildcards.addAll(rightBuilder.getStaticWildcardImports());

        return JavaExpression.from(
                "MapperMaths." + typeParams + method + "(" + left + ", " + right + ")",
                null,
                refs,
                wildcards);
    }

    /**
     * facet fnIteHoistSeats (PR #361): the RESULT item type of a BINARY
     * arithmetic render, for the conditional-hoist decl
     * ({@code ControlFlowHandler.thenItemJavaClass}'s {@code RArithmeticExpr}
     * arm) and, since PR #622, for the numeric-output statement form's hoisted
     * item decl ({@code FunctionExpressionRenderer.renderNumericOutputCoerceOrNull}
     * arm b: a null-typed binary render whose result item names the local's
     * class; a non-numeric result declines there) — the decl and the rendered
     * {@code MapperMaths.<T, J, J>} witness
     * must come from ONE selection (the #178 same-walk law), so this mirrors
     * {@link #handle}'s witness ladder EXACTLY, in LOCKSTEP:
     * (1) the typed operand-kind join (facet numeric_literal_typing) —
     *     divide's result is always BigDecimal, else the join type;
     * (2) the #334 arithStringJoin PLUS-of-provable-strings arm — String;
     * (2b) the #424 dateArithWitness MINUS-of-provable-dates arm — Integer
     *     (the day-count result of the {@code <Integer, Date, Date>} witness);
     * (3) the legacy heuristic — Integer only for a resolved-int operand with
     *     no number evidence, else BigDecimal (NO divide override here: the
     *     legacy path renders {@code <Integer, Integer, Integer>divide} for
     *     int-resolved operands and the decl must agree with those bytes).
     * A UNARY plus/minus ({@code rawLeft() == null}) declines — its render
     * routes through the dedicated unary arms in {@link #handle}, and no hoist
     * carrier is unary-topped (golden cdm6 MapPrincipalPayment's carrier is
     * the BINARY `amount * -1`, whose unary-minus RIGHT OPERAND classifies
     * UNKNOWN and lands the pair on ladder rung (3) → BigDecimal — exactly
     * the legacy witness the render emits). Any edit to {@link #handle}'s
     * witness selection MUST update this mirror (and vice versa) — the
     * pairing is byte-load-bearing at every #361 hoist seat.
     */
    public static JavaClass<?> binaryResultItemJavaClass(RArithmeticExpr expr,
            ExpressionCompiler compiler) {
        JavaTypeUtil tu = compiler.getTypeUtil();
        if (tu == null || expr.rawLeft() == null || expr.rawRight() == null) {
            return null;
        }
        HandlerHelper.NumericKind leftKind =
                HandlerHelper.numericOperandKind(expr.rawLeft(), compiler);
        HandlerHelper.NumericKind rightKind = leftKind == HandlerHelper.NumericKind.UNKNOWN
                ? HandlerHelper.NumericKind.UNKNOWN
                : HandlerHelper.numericOperandKind(expr.rawRight(), compiler);
        if (rightKind != HandlerHelper.NumericKind.UNKNOWN) {
            boolean joinIsNumber = leftKind == HandlerHelper.NumericKind.NUMBER
                    || rightKind == HandlerHelper.NumericKind.NUMBER;
            return expr.op() == ArithOp.DIVIDE || joinIsNumber ? tu.BIG_DECIMAL : tu.INTEGER;
        }
        if (expr.op() == ArithOp.PLUS
                && isStringOperand(expr.rawLeft(), compiler)
                && isStringOperand(expr.rawRight(), compiler)) {
            return tu.STRING;
        }
        // facet dateArithWitness (W42 finding #3, PR #424): the date−date subtract
        // RESULT item is Integer — LOCKSTEP with handle()'s dateSubtract rung.
        if (expr.op() == ArithOp.MINUS
                && isDateOperand(expr.rawLeft(), compiler)
                && isDateOperand(expr.rawRight(), compiler)) {
            return tu.INTEGER;
        }
        // facet dateTimeAddWitness (PR #426): the date + time ADD result item is
        // LocalDateTime — LOCKSTEP with handle()'s dateTimeAdd rung.
        if (expr.op() == ArithOp.PLUS
                && isDateOperand(expr.rawLeft(), compiler)
                && isTimeOperand(expr.rawRight(), compiler)) {
            return tu.LOCAL_DATE_TIME;
        }
        String leftRuneType = HandlerHelper.inferRuneTypeName(expr.rawLeft());
        String rightRuneType = HandlerHelper.inferRuneTypeName(expr.rawRight());
        boolean hasResolvedIntType =
                HandlerHelper.isResolvedIntType(leftRuneType, expr.rawLeft())
                        || HandlerHelper.isResolvedIntType(rightRuneType, expr.rawRight());
        boolean hasNumberType = HandlerHelper.isNumberType(leftRuneType)
                || HandlerHelper.isNumberType(rightRuneType);
        return hasResolvedIntType && !hasNumberType ? tu.INTEGER : tu.BIG_DECIMAL;
    }

    /**
     * facet dateArithWitness (W42 finding #3, PR #424) + facet dateTimeAddWitness
     * (PR #426): true when the operand resolves to the {@code date} record — the
     * positive-evidence gate for upstream's non-numeric date arithmetic branches
     * ({@code ExpressionGenerator.binaryExpr}'s else-arm over
     * {@code extendsNumber}). THREE resolution channels, one law: the
     * ENGINE-inferred alias-stripped type (the SAME inference channel as
     * {@code HandlerHelper.numericOperandKind}'s engine arm), ALIAS TRANSPARENCY
     * (an alias-head operand recurses into the shortcut's body — see the in-arm
     * comment), and the DISGUISED alias-body chain (inference-MISSING on that
     * channel — the literal declared-type gate). An unresolved, MISSING,
     * META-ANNOTATED, or non-date operand returns {@code false}, declining the
     * pair to the legacy heuristic byte-verbatim (the meta gate keeps the code
     * and the arm's stated scope in lockstep on EVERY channel — a
     * {@code date [metadata …]} operand rides a wrapper-typed mapper the
     * item-level operand compile has no witness for; Copilot #424 R1).
     */
    private static boolean isDateOperand(RExpression e, ExpressionCompiler compiler) {
        return isDateOperand(e, compiler, 0);
    }

    private static boolean isDateOperand(RExpression e, ExpressionCompiler compiler, int depth) {
        GeneratorModel gm = compiler.getGeneratorModel();
        if (gm == null || gm.workspace() == null || e == null) {
            return false;
        }
        RMetaAnnotatedType inferred = gm.workspace().getInferredType(e);
        if (inferred != null && !inferred.isMissing()) {
            if (inferred.hasMeta()) {
                return false;
            }
            RType stripped = HandlerHelper.stripAliases(inferred.type());
            return stripped instanceof RRecordType rec && rec.kind() == RecordKind.DATE;
        }
        // facet dateTimeAddWitness (PR #426): ALIAS TRANSPARENCY — the engine
        // inference channel above does not cover shortcut references (golden
        // expr-date-time-add: `arg1`/`arg2` are alias refs over date/time navs),
        // so an alias-head operand recurses into the shortcut's BODY through the
        // SAME gate (upstream's type provider types an alias by its body — one
        // resolution law, no second channel). The head resolves through the
        // resolution tryAliasReceiverMapperType uses
        // (NavigationHandler.aliasShortcutOrNull — linker-bound symbol or the
        // name-matched fallback); the depth bound is a defensive cycle guard
        // (rune aliases cannot recurse, so it never binds on valid input).
        RShortcut aliasHead = depth < 8 ? NavigationHandler.aliasShortcutOrNull(e) : null;
        if (aliasHead != null && aliasHead.expression() != null) {
            return isDateOperand(aliasHead.expression(), compiler, depth + 1);
        }
        // facet dateTimeAddWitness (PR #426): the DISGUISED 2-name chain
        // (`funIn -> val1` — an REnumValueRef with an empty enumeration(), the
        // #288 lineage) inside an ALIAS BODY is inference-MISSING (the alias-body
        // channel — the P372D-class gap the probe measured), so the leaf resolves
        // through the shared disguise resolution and gates on its DECLARED type
        // name — positive evidence, the literal `date` (a typeAlias-named leaf
        // declines, mirroring the #420 lift arm's documented no-alias-walk gate);
        // the meta gate stays lockstep via detectMetaKind (the #424 law: a
        // meta-wrapped date operand declines).
        if (e instanceof com.regnosys.rosetta.ast.expressions.references.REnumValueRef evr
                && evr.enumeration().isEmpty()) {
            com.regnosys.rosetta.ast.supporting.RAttribute leaf =
                    NavigationHandler.resolveDisguisedFeature(evr, compiler, null);
            return leaf != null && leaf.typeCall() != null
                    && com.regnosys.rosetta.generator.java.object.MetaFieldGenerator
                            .detectMetaKind(leaf)
                            == com.regnosys.rosetta.generator.java.object.MetaFieldGenerator.MetaKind.NONE
                    && "date".equals(leaf.typeCall().typeName());
        }
        return false;
    }

    /**
     * facet dateTimeAddWitness (PR #426): true when the operand's ENGINE-inferred,
     * alias-stripped type is the {@code time} basic (a singleton — reference
     * compare, exactly {@code JavaTypeTranslator.caseBasicType}'s dispatch) — the
     * positive-evidence RIGHT gate for upstream's date + time add branch. Same
     * inference channel, meta gate and alias-transparency recursion as
     * {@link #isDateOperand} (one resolution law); an unresolved, MISSING,
     * META-ANNOTATED or non-time operand returns {@code false}, declining the
     * pair to the legacy heuristic byte-verbatim.
     */
    private static boolean isTimeOperand(RExpression e, ExpressionCompiler compiler) {
        return isTimeOperand(e, compiler, 0);
    }

    private static boolean isTimeOperand(RExpression e, ExpressionCompiler compiler, int depth) {
        GeneratorModel gm = compiler.getGeneratorModel();
        if (gm == null || gm.workspace() == null || e == null) {
            return false;
        }
        RMetaAnnotatedType inferred = gm.workspace().getInferredType(e);
        if (inferred != null && !inferred.isMissing()) {
            if (inferred.hasMeta()) {
                return false;
            }
            return HandlerHelper.stripAliases(inferred.type()) == RBasicType.TIME;
        }
        // Alias transparency — see isDateOperand's arm (the shared law).
        RShortcut aliasHead = depth < 8 ? NavigationHandler.aliasShortcutOrNull(e) : null;
        if (aliasHead != null && aliasHead.expression() != null) {
            return isTimeOperand(aliasHead.expression(), compiler, depth + 1);
        }
        // The disguised 2-name alias-body chain — see isDateOperand's arm (the
        // shared law); positive evidence = the literal `time` declared name.
        if (e instanceof com.regnosys.rosetta.ast.expressions.references.REnumValueRef evr
                && evr.enumeration().isEmpty()) {
            com.regnosys.rosetta.ast.supporting.RAttribute leaf =
                    NavigationHandler.resolveDisguisedFeature(evr, compiler, null);
            return leaf != null && leaf.typeCall() != null
                    && com.regnosys.rosetta.generator.java.object.MetaFieldGenerator
                            .detectMetaKind(leaf)
                            == com.regnosys.rosetta.generator.java.object.MetaFieldGenerator.MetaKind.NONE
                    && "time".equals(leaf.typeCall().typeName());
        }
        return false;
    }

    /**
     * facet arithStringJoin (PR #334): true when the operand is PROVABLY
     * string-typed — a string literal, a {@code to-string} conversion, a nested
     * string {@code +} join, or a navigation/reference whose resolved Rune type
     * is {@code string}. Positive evidence only: an unresolved or numeric
     * operand always returns {@code false}, declining the pair to the legacy
     * heuristic byte-verbatim.
     */
    private static boolean isStringOperand(RExpression e, ExpressionCompiler compiler) {
        return isStringOperand(e, compiler, null);
    }

    /**
     * facet stringJoinTradeResidual (PR #364): the SCOPE-aware overload — a BARE
     * implicit item piped by a then ({@code thenArg.mapSingleToItem(item -> item + "_"
     * + …)}) types through the render-true binding channel the #362 comparison seats
     * read ({@link HandlerHelper#bareItemThenPipeMetaType}): provably string when the
     * pipe element is a meta wrapper whose VALUE type is String (golden trade
     * TechnicalRecordId ×4 — {@code MapperS<FieldWithMetaString>} items join
     * {@code <String, String, String>} with the guarded in-join deref). The scope-less
     * 2-arg form (the {@code binaryResultItemJavaClass} mirror — a hoist decl seat
     * never hosts a bare-item pipe join, so the mirror's ladder stays pinned) keeps the
     * arm inert.
     */
    private static boolean isStringOperand(RExpression e, ExpressionCompiler compiler,
            JavaStatementScope scope) {
        if (e instanceof RStringLiteral) {
            return true;
        }
        if (e instanceof RToStringExpr) {
            return true;
        }
        if (e instanceof RArithmeticExpr inner) {
            return inner.op() == ArithOp.PLUS
                    && inner.rawLeft() != null
                    && inner.rawRight() != null
                    && isStringOperand(inner.rawLeft(), compiler, scope)
                    && isStringOperand(inner.rawRight(), compiler, scope);
        }
        if (scope != null && e instanceof RImplicitVariable
                && compiler.getTypeUtil() != null) {
            com.regnosys.rosetta.generator.java.types.RJavaWithMetaValue pipeMeta =
                    HandlerHelper.bareItemThenPipeMetaType(e, scope, compiler);
            if (pipeMeta != null
                    && compiler.getTypeUtil().STRING.equals(pipeMeta.getValueType())) {
                return true;
            }
        }
        if ("string".equals(HandlerHelper.inferRuneTypeName(e))) {
            return true;
        }
        // Leg-C #16 typing-channel heal (2026-07-18): the ENGINE-inference arm —
        // the SAME workspace channel isDateOperand/isTimeOperand read (one
        // resolution law), with the same meta gate and alias strip. A closure
        // parameter (`stringList reduce a, b [ a + b ]`) resolves to its declaring
        // RInlineFunction, which inferRuneTypeName's static walk cannot type but
        // the workspace inference now can (the closure-param typing heal); the
        // numeric triple derivation already reads this channel via
        // numericOperandKind — this arm brings the string gate into lockstep.
        // Positive evidence only: MISSING/meta/non-string falls through to the
        // remaining arms byte-verbatim, and no green file carries the BigDecimal
        // witness over string operands (the #347 corpus law), so a newly-proven
        // string operand can only move an already-divergent render.
        if (compiler != null && compiler.getGeneratorModel() != null
                && compiler.getGeneratorModel().workspace() != null) {
            RMetaAnnotatedType inferred =
                    compiler.getGeneratorModel().workspace().getInferredType(e);
            if (inferred != null && !inferred.isMissing() && !inferred.hasMeta()
                    && HandlerHelper.stripAliases(inferred.type())
                            instanceof com.regnosys.rosetta.types.RStringType) {
                return true;
            }
        }
        // facet arithStringJoinAlias (PR #347): an ALIAS (RShortcut) operand resolves
        // through the SAME walk that renders the alias method signature
        // (tryAliasReceiverMapperType → inferShortcutMapperJavaType) — provably string
        // when the item type is String OR a meta wrapper whose VALUE type is String
        // (drr PartyLeiAndPersonByRoles: reportingPartyLei/partyLei1 = MapperS<String>,
        // partyPersonId = MapperC<FieldWithMetaString>; the wrapper operand then derefs
        // via the #334 wrapper-level coercion — unguarded for a MapperC receiver, exactly
        // golden's form). The #331 basic-type-consumer law is byte-justified here by the
        // corpus law above (golden add witnesses are exhaustively BD/I/String triples;
        // 0 green files carry the BigDecimal witness over string operands).
        if (e instanceof RSymbolReference symRef
                && symRef.symbol().filter(RShortcut.class::isInstance).isPresent()
                && compiler != null && compiler.getTypeUtil() != null) {
            JavaType aliasType = NavigationHandler.tryAliasReceiverMapperType(e, compiler);
            if (aliasType != null) {
                JavaType item = compiler.getTypeUtil().getItemType(aliasType);
                if (item instanceof RJavaWithMetaValue withMeta) {
                    item = withMeta.getValueType();
                }
                return compiler.getTypeUtil().STRING.equals(item);
            }
        }
        return false;
    }

    /**
     * facet arithOperandWrapperCoerce (PR #334): compile a RESOLVED-join operand
     * interior (no entry terminal coercion — every interior consumer of the
     * threaded item-level expected behaves as before) and coerce the result at
     * the WRAPPER level, mirroring upstream's
     * {@code javaCode(operand, context.withExpected(MAPPER.wrapExtends(joined)))}:
     * <ul>
     *   <li>null-typed operand — byte-identical (coercion dormant, the dominant
     *       shape);</li>
     *   <li>ALIAS-call operand with a null compiled type — re-typed from the
     *       {@code FunctionAliasHelper} signature walk (the arm-A1 channel,
     *       {@code NavigationHandler.tryAliasReceiverMapperType}), so a typed
     *       alias operand coerces exactly like a typed navigation;</li>
     *   <li>wrapper-typed operand — coerced to {@code Mapper<? extends
     *       <operandItem>>}: a meta item emits the guarded inline
     *       {@code .<Value>map("Type coercion", …)} unwrap, an Integer item in a
     *       BigDecimal join the guarded {@code BigDecimal.valueOf} map, an
     *       already-matching item is identity;</li>
     *   <li>ITEM-typed operand — the pre-facet item-level coercion
     *       {@code compile()} would have applied at entry.</li>
     * </ul>
     */
    private static JavaStatementBuilder compileOperandWrapperCoerced(RExpression rawOperand,
            JavaType operandItemType, ExpressionContext ctx, ExpressionCompiler compiler) {
        // facet fnIoMetaOperandValueDeref (PR #433, finding #21): a BARE meta-input
        // param operand is the raw wrapper Java LOCAL, so upstream's operand
        // conversion derefs it with the null-guarded PARAM form `(myInput == null ?
        // MapperS.<String>ofNull() : MapperS.of(myInput.getValue()))` (convertNullSafe
        // at the variable seat — the func-meta-scheme-arith oracle golden), NOT the
        // Mapper-chain "Type coercion" map step the #334 wrapper-level coercion below
        // applies to CHAIN operands. Gated to the exact witnessed shape: the deref's
        // VALUE type must equal the operand's expected item type (String==String);
        // any other meta operand keeps the pre-facet channel byte-verbatim.
        JavaExpression metaOperandDeref =
                NavigationHandler.metaInputValueDeref(rawOperand, compiler);
        if (metaOperandDeref != null && compiler.getTypeUtil() != null
                && operandItemType != null
                && operandItemType.equals(
                        compiler.getTypeUtil().getItemType(metaOperandDeref.getExpressionType()))) {
            return metaOperandDeref;
        }
        JavaStatementBuilder b = compiler.compileInterior(rawOperand, operandItemType, ctx.scope());
        TypeCoercionService svc = compiler.getCoercionService();
        JavaTypeUtil tu = compiler.getTypeUtil();
        if (svc == null || tu == null) {
            return b;
        }
        // v3.2 seat 1 (F2 dropped-coercion, the chaos C8Scale rows): the NUMERIC twin of the
        // #433 meta-input deref above — a bare single-cardinality INPUT parameter whose Java type
        // is a boxed numeric the join must WIDEN (an Integer `q` in a BigDecimal multiply; a
        // `number(fractionalDigits: 0)` alias and a plain `int` input are the same Integer) is
        // upstream's item→wrapper convertNullSafe at the VARIABLE seat, guarded OUTSIDE the wrap
        // because a boxed parameter can be null:
        // `(q == null ? MapperS.<BigDecimal>ofNull() : MapperS.of(BigDecimal.valueOf(q)))`.
        // Gated on the declared-input identity + the numeric pair; the conversion text and the
        // typed empty come from the coercion service. Corpus law: ZERO vendored goldens carry the
        // param form (no real function joins an Integer input into BigDecimal arithmetic) and the
        // bare `MapperS.of(q)` never compiled there, so no green file moves.
        JavaExpression numericParam = NavigationHandler.numericInputParamCoerceOrNull(
                rawOperand, b, operandItemType, ctx.scope(), compiler);
        if (numericParam != null) {
            return numericParam;
        }
        if (b.getExpressionType() == null) {
            JavaType aliasType = NavigationHandler.tryAliasReceiverMapperType(rawOperand, compiler);
            if (aliasType != null) {
                b = NavigationHandler.retypeBuilder(b, aliasType);
            } else if (rawOperand instanceof com.regnosys.rosetta.ast.expressions.references
                            .REnumValueRef operandEvr
                    && operandEvr.enumeration().isEmpty() && tu != null
                    && compiler.getTypeTranslator() != null) {
                // facet disguisedAliasChainOperandRetype (PR #389): a DISGUISED 2-name
                // chain over an ALIAS head (`frequency -> periodMultiplier`) compiles
                // null-typed (the alias-rooted chain gap the #352 note names), so
                // retype from the SAME resolveDisguisedFeature walk the render's
                // witness used — the NUMERIC leaf's Java type under the render's
                // Mapper kind. The #334 wrapper-level coercion below then emits the
                // guarded valueOf hop (golden drr QuantityToDeliveryCapacity ×16
                // `.<BigDecimal>map("Type coercion", integerN -> integerN == null ?
                // null : BigDecimal.valueOf(integerN))` with method-wide deferred
                // numbering). NUMERIC leaves only — the byte-justified class; an
                // Integer leaf at an Integer join coerces identity (no byte change),
                // and golden ALWAYS hops an Integer operand in a BigDecimal join
                // (upstream compiles operands wrapper-level), so no green file
                // carries the bare form.
                com.regnosys.rosetta.ast.supporting.RAttribute operandLeaf =
                        NavigationHandler.resolveDisguisedFeature(
                                operandEvr, compiler, new HashSet<>());
                com.regnosys.rosetta.generator.java.GeneratorModel leafGm =
                        compiler.getGeneratorModel();
                JavaType leafJava = operandLeaf == null || leafGm == null ? null
                        : compiler.getTypeTranslator()
                                .toJavaReferenceType(leafGm.getType(operandLeaf));
                if (leafJava != null
                        && (tu.isInteger(leafJava) || tu.isBigDecimal(leafJava))
                        && leafJava instanceof JavaClass<?> leafClass) {
                    // Separate wrap calls — the MAPPER_C/MAPPER_S ternary captures a
                    // wildcard (the #350 inference note).
                    JavaType operandWrapped =
                            NavigationHandler.chainProvesMulti(operandEvr, compiler)
                                    ? tu.wrap(tu.MAPPER_C, leafClass)
                                    : tu.wrap(tu.MAPPER_S, leafClass);
                    b = NavigationHandler.retypeBuilder(b, operandWrapped);
                }
            }
        }
        // facet stringJoinTradeResidual (PR #364): the #362 comparison-seat retype at
        // the ARITHMETIC seat — a NULL-typed BARE implicit-item operand types from its
        // render-true then-pipe binding (bareItemThenPipeMetaType), so the wrapper-level
        // coercion below emits the guarded inline deref (golden trade TechnicalRecordId
        // ×4: `item.<String>map("Type coercion", _fieldWithMetaString ->
        // _fieldWithMetaString == null ? null : _fieldWithMetaString.getValue())` — the
        // `_` escape falls out of the per-lambda scope seeing the arm-deref's method-
        // level `fieldWithMetaString` registration, the #363 escaped-iff-taken law).
        if (b.getExpressionType() == null && rawOperand instanceof RImplicitVariable) {
            com.regnosys.rosetta.generator.java.types.RJavaWithMetaValue pipeMeta =
                    HandlerHelper.bareItemThenPipeMetaType(rawOperand, ctx.scope(), compiler);
            if (pipeMeta != null) {
                b = NavigationHandler.retypeBuilder(b, tu.wrap(tu.MAPPER_S, pipeMeta));
            }
        }
        JavaType actual = b.getExpressionType();
        if (actual == null) {
            JavaStatementBuilder hoisted =
                    fnCallOperandNumericHoistOrNull(rawOperand, b, operandItemType, ctx, compiler, tu);
            if (hoisted != null) {
                return hoisted;
            }
            JavaStatementBuilder wrapped =
                    arglessFnCallOperandWrapOrNull(rawOperand, b, operandItemType, compiler, tu);
            if (wrapped != null) {
                return wrapped;
            }
            return b;
        }
        if (tu.isWrapper(actual)) {
            return svc.coerce(b, actual, tu.wrapExtends(tu.MAPPER, operandItemType), ctx.scope());
        }
        // v3.2 seat 1 (F2 dropped-coercion, the chaos C20Shade rows): a PRIMITIVE-typed operand —
        // the count operator's `int`, which a `then count` chain delivers past the node-keyed
        // wrap in handle() — compiles against the SAME Mapper expectation upstream gives every
        // arithmetic operand (`MAPPER.wrapExtends(item)`): the item→wrapper arm converts the
        // item first (`BigDecimal.valueOf(<int>)` at a BigDecimal join, identity at an Integer
        // one) and wraps `MapperS.of(...)` — the text-keyed wrap's exact forms, now type-driven
        // and reachable from any operand shape. A bare primitive in a MapperMaths parameter
        // never compiled, so no green file carries the unwrapped form.
        // THE s1a CHAIN'S CATCH (PR #622; both routes, ten CDM cells, golden cdm5/cdm6
        // GenerateObservationDates `days` + the #361 lock FnIteHoistSeatsComposeTest): the arm
        // as first written admitted EVERY item-typed operand, and the Mapper-form ite slot's
        // sentinel (ControlFlowHandler.hoistAsItemLocalOrNull — a `final MapperS<X>
        // ifThenElseResult;` local returned with its ITEM class as its stamp and marked
        // self-unwrapping, the "splice me verbatim" contract every consumer honours) is exactly
        // an item-typed builder whose Java value is ALREADY the Mapper, so the arm double-wrapped
        // it (`MapperS.of(ifThenElseResult)`). A boxed-item stamp is therefore NOT yet a
        // trustworthy "not a Mapper" witness in the fork — the #615 census's stamp inversion,
        // banked for the typing seat — while a primitive stamp is (no Java primitive is ever a
        // Mapper local): the arm is gated on the primitive, and a boxed item keeps the pre-seat
        // item-level coercion (identity at a same-item join — the sentinel splices BARE).
        if (!(actual instanceof JavaPrimitiveType)) {
            return svc.coerce(b, actual, operandItemType, ctx.scope());
        }
        return svc.coerce(b, actual, tu.wrapExtends(tu.MAPPER, operandItemType), ctx.scope());
    }

    /**
     * facet arithFnCallOperandNumericHoist (PR #349, S5): a DIRECT function-call
     * arithmetic operand ({@code DateDifference(firstDate, secondDate)} at a
     * BigDecimal-typed MapperMaths seat) compiles with a NULL expression type (an
     * invocation carries no Java type and is no alias), so the coercion ladder above
     * could not fire and the raw {@code MapperS.of(<call>)} splice was the
     * non-compiling incompatible-bounds form (cdm6 DateDifferenceYears — already
     * waivered, green-safe by construction). Upstream compiles the call ITEM-typed
     * and {@code convertNullSafe}'s item→wrapper arm hoists + guards:
     * <pre>
     * final Integer integer = dateDifference.evaluate(firstDate, secondDate);
     * … divide((integer == null ? MapperS.&lt;BigDecimal&gt;ofNull() : MapperS.of(BigDecimal.valueOf(integer))), …)
     * </pre>
     * Gates (decline = null, today's bytes): the operand is an
     * {@link RSymbolReference} to an {@link com.regnosys.rosetta.ast.functions.RFunction}
     * with a SINGLE Integer output; the expected operand item type is BigDecimal;
     * the compiled operand is the recognizable single wrap (unwrapToBuilder present —
     * the bare invocation recovers from it); a statement-hoist sink is reachable.
     * The hoist rides the #237 sentinel-string channel (never a decl OBJECT
     * mid-expression — the #346 eager-render scope-close law). A literal operand
     * ({@code MapperS.of(new BigDecimal("365.0"))}) is not a symbol reference and
     * declines; an Integer-expected seat has no mismatch and declines.
     */
    /**
     * The ARG-LESS function-call arithmetic operand ({@code MyFunc() + dep.MyFunc()} —
     * a zero-input callee, so {@code renderImplicitFunctionInvocation} returns the bare
     * {@code <dep>.evaluate()} UNWRAPPED by contract, deferring coercion to the
     * enclosing operation) wraps in {@code MapperS.of(...)} at the Mapper-typed
     * MapperMaths seat — upstream compiles every arithmetic operand against
     * {@code MAPPER.wrapExtends(...)} and the item→MapperS coercion
     * ({@code TypeCoercionService#getItemToMapperSConversionExpression}) wraps the
     * item-typed invocation, exactly as the corpus-witnessed EXPLICIT-args calls
     * arrive pre-wrapped ({@code MapperS.of(technicalRecordId.evaluate(...))}, the
     * string-join family). Hold-out witness: name-escaping {@code UseFunctions}
     * (PR #410).
     *
     * <p>Gates (decline = null, today's bytes): an {@link RSymbolReference} resolving
     * to an {@link com.regnosys.rosetta.ast.functions.RFunction} with NO real inputs
     * (the synthesized-input placeholder excluded — an explicit-args call compiles
     * pre-wrapped and never reaches here bare), a SINGLE-cardinality output whose
     * Java type EQUALS the seat's operand item type (the identity value-coercion
     * case — the Integer-output/BigDecimal-seat mismatch takes the #349 hoist arm
     * above), and a render not already {@code MapperS.of(}-prefixed (the
     * render-truth double-wrap belt). Green-safe by construction: a bare item-typed
     * value in a Mapper-typed MapperMaths parameter never compiled, so no green
     * file carries the unwrapped form.
     */
    private static JavaStatementBuilder arglessFnCallOperandWrapOrNull(RExpression rawOperand,
            JavaStatementBuilder b, JavaType operandItemType,
            ExpressionCompiler compiler, JavaTypeUtil tu) {
        if (!(rawOperand instanceof RSymbolReference symRef)
                || compiler.getGeneratorModel() == null
                || compiler.getTypeTranslator() == null
                || operandItemType == null) {
            return null;
        }
        var fn = symRef.symbol()
                .filter(com.regnosys.rosetta.ast.functions.RFunction.class::isInstance)
                .map(com.regnosys.rosetta.ast.functions.RFunction.class::cast)
                .orElse(null);
        if (fn == null || fn.output().isEmpty()) {
            return null;
        }
        boolean hasRealInputs = fn.inputs().stream()
                .anyMatch(in -> !com.regnosys.rosetta.generator.java.function.FunctionGenerator
                        .isSynthesizedInput(in));
        if (hasRealInputs) {
            return null;
        }
        var out = fn.output().get();
        if (compiler.getGeneratorModel().isMulti(out)) {
            return null;
        }
        var outRType = compiler.getGeneratorModel().getType(out);
        JavaClass<?> outJava = outRType == null
                ? null
                : compiler.getTypeTranslator().toJavaReferenceType(outRType);
        if (outJava == null || !outJava.equals(operandItemType)) {
            return null;
        }
        // facet mapperWrapPrefix (v3.1 C2d retirement family 8, PR #615) — VERDICT-MOVED
        // RETIRE-AFTER-CENSUS -> JUSTIFIED-KEPT, and the measurement needed a FOURTH walk to
        // exist. The three main census walks (default-route D11, IR-route D11, optimised) record
        // 0 / 0 / 0 arrivals here: this row's only named carrier is a HOLD-OUT golden
        // (`HoldOutByteCompareTest`, the `name-escaping/import-overlap` group), which none of the
        // three walks runs — instrument reach, not corpus absence. The h1 hold-out walk (201
        // tests / 0F) measured it: 8 arrivals, all at `fn:UseFunctions`, all identical, and the
        // belt's TRUE polarity fired at 0 of 8 — the argless-fn operand arrives BARE, never
        // already-wrapped (`typeNull=true` at 8/8, confirming the enclosing `actual == null` gate).
        // The proposed marker channel agrees at the decline polarity (`unwrap=no` 8/8) but its
        // TRUE polarity is carried by NO producer contract (`prod=none` at 8/8 — the operand
        // producers are raw `JavaExpression.from` sites, and this charter migrates none of them),
        // so retiring the belt onto the marker would leave it silently dead against a
        // raw-wrapped operand — the exact double wrap it exists to stop. A LAW-74 belt with no
        // populated typed channel, kept with its measurement (the family-7 #340 precedent).
        if (!(b instanceof JavaExpression je)
                || je.renderToString().startsWith("MapperS.of(")) {
            return null;
        }
        Set<JavaClass<?>> refs = new HashSet<>(je.getRefs());
        refs.add(HandlerHelper.MAPPER_S);
        return JavaExpression.from(
                "MapperS.of(" + je.renderToString() + ")",
                null, refs, je.getStaticWildcardImports());
    }

    private static JavaStatementBuilder fnCallOperandNumericHoistOrNull(RExpression rawOperand,
            JavaStatementBuilder b, JavaType operandItemType, ExpressionContext ctx,
            ExpressionCompiler compiler, JavaTypeUtil tu) {
        if (!(rawOperand instanceof RSymbolReference symRef)
                || compiler.getGeneratorModel() == null
                || compiler.getTypeTranslator() == null) {
            return null;
        }
        var fn = symRef.symbol()
                .filter(com.regnosys.rosetta.ast.functions.RFunction.class::isInstance)
                .map(com.regnosys.rosetta.ast.functions.RFunction.class::cast)
                .orElse(null);
        if (fn == null || fn.output().isEmpty()) {
            return null;
        }
        var out = fn.output().get();
        if (compiler.getGeneratorModel().isMulti(out)) {
            return null;
        }
        var outRType = compiler.getGeneratorModel().getType(out);
        JavaClass<?> outJava = outRType == null
                ? null
                : compiler.getTypeTranslator().toJavaReferenceType(outRType);
        if (!tu.isInteger(outJava) || !tu.isBigDecimal(operandItemType)) {
            return null;
        }
        if (!(b instanceof JavaExpression wrapped)
                || wrapped.unwrapToBuilder().isEmpty()) {
            return null;
        }
        var sink = ctx.scope().findStatementHoistSink();
        if (sink == null) {
            return null;
        }
        if (!(wrapped.unwrapToBuilder().get() instanceof JavaExpression inner)) {
            return null;
        }
        var id = ctx.scope().createUniqueIdentifier("integer");
        String tok = ctx.scope().registerDeferredCoercionName(id);
        sink.registerStatementHoist(
                "final Integer " + tok + " = " + inner.renderToString() + ";");
        Set<JavaClass<?>> refs = new HashSet<>(inner.getRefs());
        refs.add(HandlerHelper.MAPPER_S);
        refs.add(tu.BIG_DECIMAL);
        return JavaExpression.from(
                "(" + tok + " == null ? MapperS.<BigDecimal>ofNull() : MapperS.of(BigDecimal.valueOf("
                        + tok + ")))",
                null, refs, inner.getStaticWildcardImports());
    }

}
