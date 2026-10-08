package com.regnosys.rosetta.generator.java.expression.handlers;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.expressions.constructors.RConstructorExpr;
import com.regnosys.rosetta.ast.expressions.unary.RCountExpr;
import com.regnosys.rosetta.ast.expressions.literals.RBooleanLiteral;
import com.regnosys.rosetta.ast.expressions.literals.RIntLiteral;
import com.regnosys.rosetta.ast.functions.ROperation;
import com.regnosys.rosetta.ast.expressions.binary.RThenExpr;
import com.regnosys.rosetta.ast.expressions.literals.RListLiteral;
import com.regnosys.rosetta.ast.expressions.literals.RNumberLiteral;
import com.regnosys.rosetta.ast.expressions.literals.RStringLiteral;
import com.regnosys.rosetta.ast.expressions.references.REmptyLiteral;
import com.regnosys.rosetta.ast.expressions.references.REnumValueRef;
import com.regnosys.rosetta.ast.expressions.references.RFeatureCall;
import com.regnosys.rosetta.ast.expressions.references.RImplicitVariable;
import com.regnosys.rosetta.ast.expressions.references.RSymbolReference;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.functions.RShortcut;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.supporting.REnumValue;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.ast.types.REnumeration;
import com.regnosys.rosetta.types.RBasicType;
import com.regnosys.rosetta.types.RDataTypeRef;
import com.regnosys.rosetta.types.REnumTypeRef;
import com.regnosys.rosetta.types.RMetaAnnotatedType;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.expression.ExpressionCompiler;
import com.regnosys.rosetta.generator.java.expression.ExpressionContext;
import com.regnosys.rosetta.generator.java.function.StatementHoistSession;
import com.regnosys.rosetta.generator.java.object.MetaFieldGenerator;
import com.regnosys.rosetta.generator.java.template.JavaStringUtil;
import com.regnosys.rosetta.generator.java.types.RJavaWithMetaValue;
import com.regnosys.rosetta.generator.java.scoping.JavaStatementScope;
import com.regnosys.rosetta.generator.java.statement.builder.JavaExpression;
import com.regnosys.rosetta.generator.java.statement.builder.JavaLiteral;
import com.regnosys.rosetta.generator.java.statement.builder.JavaStatementBuilder;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.rosetta.util.types.JavaClass;
import com.rosetta.util.types.JavaType;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Handles code generation for all six Rune DSL literal expression types.
 *
 * <p>Golden output patterns (verified against CDM golden files):
 * <ul>
 *   <li>int 42 (int context)    → {@code MapperS.of(42)}</li>
 *   <li>int 0 (number context)  → {@code MapperS.of(BigDecimal.valueOf(0))}</li>
 *   <li>number 3.14             → {@code MapperS.of(new BigDecimal("3.14"))}</li>
 *   <li>string "hello"          → {@code MapperS.of("hello")}</li>
 *   <li>boolean True            → {@code MapperS.of(true)}</li>
 *   <li>empty                   → {@code null} (bare null)</li>
 *   <li>list [1, 2]             → {@code MapperC.of(MapperS.of(1), MapperS.of(2))}</li>
 * </ul>
 *
 * <p>Integer literals are context-sensitive: when the expected type is
 * {@code BigDecimal} (Rune DSL {@code number}), the literal is wrapped in
 * {@code BigDecimal.valueOf(N)}. When the expected type is {@code Integer}
 * or unknown, the bare integer is used.
 */
public class LiteralHandler {

    // =========================================================================
    // Int literals
    // =========================================================================

    /**
     * Generates {@code MapperS.of(value)} for an integer literal.
     *
     * <p>Context-aware rendering:
     * <ul>
     *   <li>When {@code expectedType} is {@code BigDecimal}: {@code MapperS.of(BigDecimal.valueOf(N))}</li>
     *   <li>When {@code expectedType} is {@code Integer} or unknown: {@code MapperS.of(N)}</li>
     * </ul>
     *
     * <p>For values in int range: bare int, e.g. {@code MapperS.of(42)}.
     * For values in long range (beyond int): long literal — lowercase {@code l}
     * in BigDecimal contexts (the upstream golden convention, facet
     * long_literal_suffix), uppercase {@code L} in bare-int contexts.
     * For values beyond long range: {@code BigInteger} constructor.
     */
    public JavaStatementBuilder handle(RIntLiteral expr, ExpressionContext ctx, ExpressionCompiler compiler) {
        // PR-A §9.1 C3a.4.e: use wrappedInMapperSOf(inner) factory; inner
        // carries BIG_DECIMAL / BIG_INTEGER as appropriate for its source
        // text; wrapper automatically adds MAPPER_S.
        // COUPLING (PR #332, Seat-1 nit): LogicalHandler.wrapBooleanFunctionOperand's
        // defaultOperandNullSafe arm treats a scalar-literal `default` as
        // already-Mapper-wrapped BY ARM SEMANTICS (isScalarLiteral(right)) — sound
        // because every scalar-literal handler in this class compiles via
        // wrappedInMapperSOf, so SetOperationHandler's #218 arm always fires and
        // renders the MapperS.of-wrapped form. A future literal compile that drops
        // the wrap factory would silently under-wrap that arm to ofNullSafe(<bare>);
        // keep the factory or update the arm's gate.
        BigInteger value = expr.value();

        // v3.2 seat 5 (PR #626, F3): a beyond-long literal that IS a statement-seat VALUE - a conditional
        // ARM or an operation's WHOLE expression - hoists whatever the context, not only the BigDecimal
        // one: upstream's convertNullSafe declares it as a variable whatever the target (an `int` output
        // converts `bigInteger.intValueExact()`, golden conv-bigint-statement-edge IntOut, where the fork
        // used to splice the bare `new BigInteger("...")` into an Integer slot; the whole set `r = new
        // BigInteger("...")`, golden Whole). The witness the hoist returns (JavaExpression
        // .BigIntegerLiteralValue) carries the sentinel for the statement seats; its TEXT stays the lambda
        // channel's Mapper ternary (seat 24's law F5 - byte-identical at every lambda and operand seat).
        // Every OTHER seat keeps the pre-seat flow below - in particular the evaluate-ARGUMENT seat, whose
        // #129/#170 arg route (ReferenceHandler.tryMetaDerefArg) AST-recovers the literal and owns its
        // hoist + item ternary (golden conv-bigint-statement-edge AsArg; a hoist here too rendered TWO locals,
        // the seat's own first-cut catch). No channel -> the inline renders below, exactly as before.
        // Round 1 (the code-quality review's MF-1): the gate is the SHARED predicate the RENDERER reads, not a
        // structural test of its own - a hoist the conversion seat then declines would fall to the generic arm,
        // which appends `.get()` to the value ternary (the non-compiling form this arm removes) on a shape that
        // COMPILED before the seat. `statementSeatConversionItemOrNull` is null where the seat renders no conversion
        // by its OWN admission - a BigInteger-typed output (identity), a meta output, a segment-pathed target, an
        // operand or argument seat - and there upstream declares no variable either, so the pre-seat inline form
        // below is upstream's own. The renderer keeps belts the predicate does not model (named at the helper);
        // whether an admitted hoist can reach one is UNMEASURED - round 2's disclosure, banked, not claimed.
        // Round 2 (the code-quality review's SF-1): the cheap bit-length test FIRST - every int literal of every
        // function and rule reaches this handler, and the predicate walks the AST and translates a type; only a
        // beyond-long literal pays for it.
        if (value.bitLength() > 63
                && HandlerHelper.statementSeatConversionItemOrNull(expr, compiler) != null) {
            JavaStatementBuilder hoisted = hoistBigIntegerLiteralOrNull(expr, value, ctx, compiler);
            if (hoisted != null) {
                return hoisted;
            }
        }
        if (isBigDecimalContext(expr, ctx, compiler)) {
            // facet biginteger_literal_hoist: a beyond-long literal is the
            // only int literal with a non-primitive (reference) Java type, so
            // upstream's TypeCoercionService.convertNullSafe ALWAYS hoists it
            // (declareAsVariable) and null-guards the BigInteger→BigDecimal
            // conversion — the inline nested form below appears in ZERO
            // goldens. In-int/long-range literals keep the primitive-skip
            // inline forms (upstream convertNullSafe lines 351-353). v3.2 seat 5 (PR #626, F3): the
            // STATEMENT-seat values hoist ABOVE whatever the context; every other seat - the seat-24
            // operand carriers (drr FormatToLongFraction20DecimalNumber's `lessThanEquals(..., (bigInteger
            // == null ? ...))`, jfsa GetNtnlQty's comparison operands) - hoists HERE, in the BigDecimal
            // context, exactly as seat 24 left it (the touched suites' catch at the fourth cut: the
            // second cut had dropped this arm and twelve vendored-cell locks went red).
            if (value.bitLength() > 63) {
                JavaStatementBuilder hoisted = hoistBigIntegerLiteralOrNull(expr, value, ctx, compiler);
                if (hoisted != null) {
                    return hoisted;
                }
            }
            // Wrap in BigDecimal.valueOf() — matches upstream golden pattern
            String valueCode = renderIntValueForBigDecimal(value);
            Set<JavaClass<?>> innerRefs = new HashSet<>();
            innerRefs.add(HandlerHelper.BIG_DECIMAL);
            if (value.bitLength() > 63) {
                innerRefs.add(HandlerHelper.BIG_INTEGER);
            }
            return JavaExpression.wrappedInMapperSOf(
                    JavaExpression.from(valueCode, null, innerRefs));
        }

        String valueCode = renderIntValue(value);
        Set<JavaClass<?>> innerRefs = new HashSet<>();
        if (value.bitLength() > 63) {
            innerRefs.add(HandlerHelper.BIG_INTEGER);
        }
        return JavaExpression.wrappedInMapperSOf(
                JavaExpression.from(valueCode, null, innerRefs));
    }

    /**
     * Check if the expected type context indicates BigDecimal (Rune DSL "number").
     *
     * <p>Checks the {@link ExpressionContext#expectedType()} via the compiler's
     * {@link JavaTypeUtil}. v3.1 flip seat 22 (facet {@code intLiteralNumberSeat}): when the compile
     * carries NO expected type - the fork compiles conditional ARMS with {@code null} by design, the
     * ladder's decl type read from the workspace snapshot AFTER the arms - the seat consults
     * {@link HandlerHelper#intLiteralConditionalArmExpectedType} (LAW 69: the literal must agree with
     * the ladder decl it is assigned into; upstream threads the expected type through every arm, so
     * golden carries {@code MapperS.of(BigDecimal.valueOf(N))} at a number-typed conditional seat).
     * Returns {@code false} when type information is unavailable.
     */
    private boolean isBigDecimalContext(RIntLiteral expr, ExpressionContext ctx, ExpressionCompiler compiler) {
        JavaType expectedType = ctx.expectedType();
        JavaTypeUtil typeUtil = compiler.getTypeUtil();
        if (typeUtil == null) return false;
        if (expectedType == null) {
            return HandlerHelper.intLiteralConditionalArmExpectedType(
                    expr, compiler.getGeneratorModel(), compiler.getTypeTranslator(), typeUtil) != null;
        }
        // Direct BigDecimal check
        if (typeUtil.isBigDecimal(expectedType)) return true;
        // Check if wrapped in MapperS<BigDecimal> or MapperC<BigDecimal>
        if (typeUtil.isWrapper(expectedType)) {
            JavaType itemType = typeUtil.getItemType(expectedType);
            return itemType != null && typeUtil.isBigDecimal(itemType);
        }
        return false;
    }

    /**
     * facet biginteger_literal_hoist — hoist a beyond-long integer literal as
     * a statement local at the nearest statement-hoist sink and consume it
     * through the null-guarded Mapper ternary:
     *
     * <pre>
     * final BigInteger bigInteger = new BigInteger("9999999999999999999999999");
     * … (bigInteger == null ? MapperS.&lt;BigDecimal&gt;ofNull() : MapperS.of(new BigDecimal(bigInteger))) …
     * </pre>
     *
     * The ternary pieces mirror upstream TypeCoercionService — the empty arm
     * ({@code MapperS.<T>ofNull()}, xtend lines 381-382 / fork
     * {@code TypeCoercionService.emptyValueFor}'s MapperS arm), the
     * BigInteger→BigDecimal item conversion ({@code new BigDecimal(x)}, xtend
     * lines 526-528 / fork {@code ItemToItemCoercer}) and the MapperS wrap
     * (xtend lines 588-590) — composed here like
     * {@code ControlFlowHandler.hoistAsComparisonResultOrNull} composes its
     * ComparisonResult forms. Naming/numbering rides the
     * {@link StatementHoistSession} {@code bigInteger} name group (bare for a
     * singleton, {@code bigInteger0..n-1} across the assignOutput body
     * otherwise, independent of the {@code ifThenElseResult} group).
     *
     * <p>Returns {@code null} — the caller keeps the pre-facet inline render —
     * when no sink is reachable: rule/report emission (the session only opens
     * on the function path), lambda interiors (the sink walk stops at lambda
     * boundaries; upstream renders a DIFFERENT in-lambda form there) and every
     * compiler entry without an active session. The explicit-args
     * function-call ARG route ({@code ReferenceHandler.tryMetaDerefArg}, the
     * #129/#170 channel) AST-recovers the same beyond-long literal and owns
     * its bare {@code (bigInteger == null ? null : new BigDecimal(bigInteger))}
     * form — its carriers are all rule-kind, where this arm declines by
     * construction (no session), so the two producers never both fire today;
     * a future function-kind evaluate-arg carrier must coordinate them.
     *
     * <p>The returned expression carries a {@code null} expression type like
     * every literal render here — post-dispatch coercion stays dormant.
     */
    private JavaStatementBuilder hoistBigIntegerLiteralOrNull(RIntLiteral expr, BigInteger value,
            ExpressionContext ctx, ExpressionCompiler compiler) {
        // facet bigIntegerLiteralRulePath (seat 24, law F5): THE producer below owns both channels (the
        // method sink, the extract-lambda drain); this seat consumes the VALUE ternary — bare when the
        // literal IS a conditional arm (golden `return bigInteger == null ? …;` / `ifThenElseResult = …`),
        // parenthesised at an operand seat (golden FormatToShortFraction5DecimalNumber's
        // `lessThanEquals(…, (bigInteger == null ? … ), …)` — the pre-seat function-path bytes).
        String sentinel = registerBigIntegerLiteralHoistOrNull(expr, ctx, compiler);
        if (sentinel == null) {
            return null;
        }
        Set<JavaClass<?>> refs = new HashSet<>();
        refs.add(HandlerHelper.BIG_INTEGER);
        refs.add(HandlerHelper.BIG_DECIMAL);
        refs.add(HandlerHelper.MAPPER_S);
        String ternary = HandlerHelper.bigIntegerValueTernary(sentinel);
        // v3.2 seat 5 (PR #626, F3): the PRODUCER-stamped witness - the statement seats read the sentinel
        // (JavaExpression.BigIntegerLiteralValue), every other consumer the same text as before.
        return JavaExpression.bigIntegerLiteralValue(
                HandlerHelper.isConditionalArm(expr) ? ternary : "(" + ternary + ")",
                refs, sentinel);
    }

    /**
     * facet bigIntegerLiteralRulePath (seat 24, law F5): THE ONE producer of a beyond-long literal's
     * {@code final BigInteger <name> = new BigInteger("…");} hoist — consulted by this literal seat (the VALUE
     * ternary) and by {@code SetOperationHandler}'s {@code default} seat (the ITEM ternary); the then-RECEIVER
     * evaluate-argument seat reaches the sink channel through {@code ReferenceHandler.tryMetaDerefArg}'s existing
     * BigInteger→BigDecimal arm once {@code FunctionExpressionRenderer} marks the base argument's sink. Two channels:
     * the nearest statement-hoist SINK (the function path, the rule then-chain's statement seats — the decl registers
     * on the sink and lifts ahead of the statement), else — inside an {@code extract}/{@code map} lambda — the
     * pending LAMBDA channel (a {@link HandlerHelper.BigIntegerLiteralHoist} the block forms drain into the owning
     * branch before the return that consumes it). The name rides the method session's {@code bigInteger} group in
     * either channel ({@code findStatementHoistSessionAnyDepth} inside the lambda — golden numbers
     * {@code bigInteger0/1} across the method, bare for a singleton: mas TotalNotionalQuantityOfLeg2Rule). The
     * D43 IR seam: the base lexeme comes from {@link #bigIntegerBaseName} (the IR-routed override sources it from
     * the neutral ANF substrate). Returns the sentinel, or {@code null} when neither channel is reachable (the
     * caller keeps the inline render — a stated decline, corpus-unwitnessed since this seat: T5raw = 0 in every
     * golden cell).
     */
    public String registerBigIntegerLiteralHoistOrNull(RIntLiteral expr, ExpressionContext ctx,
            ExpressionCompiler compiler) {
        if (ctx.scope() == null || expr.value().bitLength() <= 63) {
            return null;
        }
        String digits = expr.value().toString();
        JavaStatementScope sink = ctx.scope().findStatementHoistSink();
        if (sink != null && sink.statementHoistSession() != null) {
            String sentinel = sink.statementHoistSession()
                    .register(bigIntegerBaseName(expr, compiler.getGeneratorModel()));
            sink.registerStatementHoist(HandlerHelper.bigIntegerLiteralDecl(sentinel, digits));
            return sentinel;
        }
        if (!HandlerHelper.literalInsideExtractLambda(expr)) {
            return null;
        }
        StatementHoistSession session = ctx.scope().findStatementHoistSessionAnyDepth();
        if (session == null) {
            return null;
        }
        String sentinel = session.register(bigIntegerBaseName(expr, compiler.getGeneratorModel()));
        ctx.scope().registerPendingLambdaHoist(new HandlerHelper.BigIntegerLiteralHoist(sentinel, digits));
        return sentinel;
    }

    /**
     * The base lexeme of the {@code bigInteger} literal-hoist name group (facet
     * {@code biginteger_literal_hoist}) — the extracted D43 IR seam (the lab's Wave-6 Phase-C
     * slice-5, decision-log L-068). The base impl returns the fixed
     * {@link StatementHoistSession#BIG_INTEGER} constant; the IR-routed subclass (supplied via
     * the {@code ExpressionCompiler.createLiteralHandler} factory) overrides it to SOURCE the
     * name from the neutral ANF substrate (the L-029 split — the neutral name vs the legacy
     * render), asserting it equals this constant. {@code expr}/{@code gm} are the oracle inputs
     * the override needs (the literal node + the workspace); the base impl ignores them because
     * the {@code bigInteger} base is value-independent. Mirrors
     * {@code CollectionHandler.thenArgBaseName} (slice-4).
     */
    protected String bigIntegerBaseName(RIntLiteral expr, GeneratorModel gm) {
        return StatementHoistSession.BIG_INTEGER;
    }

    /**
     * Render an integer value as {@code BigDecimal.valueOf(N)} for number contexts.
     *
     * <p>Facet long_literal_suffix: a value beyond int range needs the long
     * suffix or the bare digits are a non-compiling out-of-range int literal —
     * LOWERCASE {@code l}, the upstream Xtend convention the goldens carry
     * ({@code BigDecimal.valueOf(99999999999l)}). In-int-range values stay
     * suffix-free (the green corpus convention); the bare-int context's
     * {@link #renderIntValue} carried an uppercase {@code L} with "no golden
     * divergence on that branch" until v3.2 seat 13 — the branch had NO golden;
     * the oracle group {@code conv-bigint-alias-long} pins the plugin's LOWERCASE
     * {@code l} there too ({@code MapperS.of(123456789012l)}), one convention.
     * ONE method for BOTH Java routes since #634 round 1 (LAW 77 by identity, the
     * code-quality seat's SF-2): {@code IRJavaLeafEmitter.emitInt}'s BigDecimal-context
     * arm calls it as its int-context arm calls {@link #intLiteralCode} - the mirror
     * that drifted at the long band's suffix (commit 5's catch) has no twin left.
     */
    public static String renderIntValueForBigDecimal(BigInteger value) {
        if (value.bitLength() <= 31) {
            return "BigDecimal.valueOf(" + value.toString() + ")";
        }
        // BigDecimal.valueOf accepts long — suffix the beyond-int band
        if (value.bitLength() <= 63) {
            return "BigDecimal.valueOf(" + value.toString() + "l)";
        }
        // Extremely large values: new BigDecimal(new BigInteger("..."))
        return "new BigDecimal(new BigInteger(\"" + value.toString() + "\"))";
    }

    private String renderIntValue(BigInteger value) {
        return intLiteralCode(value);
    }

    /**
     * The bare-int-context render of an integer literal — ONE method for BOTH Java routes (LAW 77 by
     * identity, v3.2 seat 13): the IR route's leaf emitter ({@code IRJavaLeafEmitter.emitInt}) had
     * mirrored this arm with an uppercase {@code L} on the long band, so the seat's M5c heal (the
     * lowercase {@code l} the oracle group {@code conv-bigint-alias-long} pins) moved the default
     * route alone — the chaos D11 read 924 identical on the default route and 911 on the IR route
     * until both routes rendered through here. In-int-range values are suffix-free, the long band
     * carries the lowercase {@code l} (the upstream Xtend convention), beyond-long values take the
     * {@code BigInteger} constructor.
     */
    public static String intLiteralCode(BigInteger value) {
        // Check if fits in int
        if (value.bitLength() <= 31) {
            return value.toString();
        }
        // Check if fits in long - the LOWERCASE suffix (v3.2 seat 13, oracle conv-bigint-alias-long)
        if (value.bitLength() <= 63) {
            return value.toString() + "l";
        }
        // Arbitrary precision — use BigInteger constructor
        return "new BigInteger(\"" + value.toString() + "\")";
    }

    // =========================================================================
    // Number literals
    // =========================================================================

    /**
     * Generates {@code MapperS.of(new BigDecimal("value"))} for a number literal.
     */
    public JavaStatementBuilder handle(RNumberLiteral expr, ExpressionContext ctx, ExpressionCompiler compiler) {
        // PR-A §9.1 C3a.4.e: inner `new BigDecimal("…")` carries BIG_DECIMAL;
        // wrapper adds MAPPER_S.
        String valueStr = expr.value().toPlainString();
        return JavaExpression.wrappedInMapperSOf(
                JavaExpression.from(
                        "new BigDecimal(\"" + valueStr + "\")",
                        null,
                        Set.of(HandlerHelper.BIG_DECIMAL)));
    }

    // =========================================================================
    // String literals
    // =========================================================================

    /**
     * Generates {@code MapperS.of("value")} for a string literal.
     */
    public JavaStatementBuilder handle(RStringLiteral expr, ExpressionContext ctx, ExpressionCompiler compiler) {
        // PR-A §9.1 C3a.4.e: inner string literal carries no refs; wrapper
        // adds MAPPER_S.
        // v3.1 flip seat 22 (facet labelUnicodeEscape, LAW 69): THE ONE Java string-literal escape —
        // JavaStringUtil.escapeJava (commons-text 1.12.0 escapeJava byte-for-byte; upstream's
        // JavaLiteral.from(String) uses the same StringEscapeUtils call as the template emitters).
        // The five-case private mirror that lived here is retired; the IR-route leaf
        // (IRJavaLeafEmitter.emitString) consults the same method.
        String escaped = JavaStringUtil.escapeJava(expr.value());
        return JavaExpression.wrappedInMapperSOf(
                JavaExpression.from("\"" + escaped + "\"", null));
    }

    // =========================================================================
    // Boolean literals
    // =========================================================================

    /**
     * Generates {@code MapperS.of(true)} or {@code MapperS.of(false)}.
     */
    public JavaStatementBuilder handle(RBooleanLiteral expr, ExpressionContext ctx, ExpressionCompiler compiler) {
        // PR-A §9.1 C3a.4.e: boolean literal is java.lang; wrapper adds MAPPER_S.
        // COUPLING (PR #591, seat 19 — the #590 review's SF-1 convention): LogicalHandler's
        // booleanLiteralOperandNullSafe arm (wrapBooleanFunctionOperandShapeArms) adds ONLY
        // the ComparisonResult layer — `ComparisonResult.ofNullSafe(<rendered>)` — because
        // this handler always renders the literal Mapper-wrapped via wrappedInMapperSOf,
        // and it ignores ctx (the single-arg factory carries no expression type, so the
        // expected-type coercion never unwraps it). A future literal compile that drops the
        // wrap factory would silently under-wrap that arm to ofNullSafe(<bare>); keep the
        // factory or give the arm the alreadyMapperWrapped branch its sibling arms carry.
        String boolValue = expr.value() ? "true" : "false";
        return JavaExpression.wrappedInMapperSOf(
                JavaExpression.from(boolValue, null));
    }

    // =========================================================================
    // Empty literals
    // =========================================================================

    /**
     * Returns {@link JavaLiteral#NULL} (bare {@code null}) for the empty literal.
     */
    public JavaStatementBuilder handle(REmptyLiteral expr, ExpressionContext ctx, ExpressionCompiler compiler) {
        return JavaLiteral.NULL;
    }

    // =========================================================================
    // List literals
    // =========================================================================

    /**
     * Generates {@code MapperC.<Item>of(element1, element2, ...)} for a list literal,
     * mirroring upstream {@code ExpressionGenerator.caseListLiteral}: the item-type
     * generic witness is always emitted, and each element renders as a {@code Mapper}.
     *
     * <p>Each element is compiled recursively via the compiler. Literals self-wrap
     * in {@code MapperS.of(...)} (their own handlers); variables / function calls /
     * navigation chains are already {@code Mapper}-valued. A bare
     * {@link REnumValueRef} (the {@code EnumName -> valueName} form — e.g.
     * {@code [ActionTypeEnum -> NEWT, ...]}) renders the qualified constant
     * {@code Enum.VALUE} UNWRAPPED with a null expression type (so the
     * expected-type coercion never fires); golden wraps it as
     * {@code MapperS.of(Enum.VALUE)}, so it is wrapped explicitly here. Bare enum
     * VALUES resolved through an expected enum type (e.g. {@code [CA_AB_ASC, ...]}
     * for an enum-typed output — bound by the parser's Cat 13c, facet
     * void_witness_bare_enum arm D2) are {@link RSymbolReference}s carrying an
     * {@code REnumValue} symbol and take the SAME wrap (arm D3).
     *
     * <p>The witness item type is read from the inference engine
     * ({@code workspace().getInferredType}, the joined element type). When the type
     * is unavailable the bare {@code MapperC.of(...)} form is emitted unchanged.
     */
    public JavaStatementBuilder handle(RListLiteral expr, ExpressionContext ctx, ExpressionCompiler compiler) {
        // PR-A §9.1 C3a.4.e: use wrappedInMapperCOf(inners) factory which
        // adds MAPPER_C ref and unions inner element refs + staticWildcards.
        List<RExpression> elements = expr.elements();
        List<JavaExpression> inners = new ArrayList<>(elements.size());
        JavaClass<?> joinedItem = listItemJavaType(expr, compiler);
        for (RExpression element : elements) {
            JavaStatementBuilder compiled = compiler.compile(element, ctx.expectedType(), ctx.scope());
            if (!(compiled instanceof JavaExpression javaExpr)) {
                throw new IllegalStateException(
                        "List element compiled to non-expression type: " + compiled.getClass().getSimpleName());
            }
            if (element instanceof REmptyLiteral
                    && joinedItem != null
                    && HandlerHelper.findEnclosingTypeCondition(expr) != null) {
                // Coverage wave D (datarule): an `empty` LIST-LITERAL element
                // re-presents as the TYPED absent Mapper `MapperS.<T>ofNull()` at the
                // MapperC.of element seat (T = the literal's joined item witness) —
                // the bare `null` the empty literal compiles to never satisfied
                // MapperC.of's Mapper element signature (golden drr
                // ESMAEMIRTransactionReportEMIR_VR_2009_01
                // `[productClassification, empty] first exists` →
                // `MapperS.<String>ofNull()`). The #269 typed-empty law at the
                // element position, TYPE-CONDITION-gated: function/rule paths keep
                // their bytes.
                Set<JavaClass<?>> emptyRefs = new HashSet<>(javaExpr.getRefs());
                emptyRefs.add(HandlerHelper.MAPPER_S);
                emptyRefs.add(joinedItem);
                JavaTypeUtil emptyTu = compiler.getTypeUtil();
                inners.add(JavaExpression.from(
                        "MapperS.<" + joinedItem.getSimpleName() + ">ofNull()",
                        emptyTu == null ? null : emptyTu.wrap(emptyTu.MAPPER_S, joinedItem),
                        emptyRefs, javaExpr.getStaticWildcardImports()));
                continue;
            }
            if (element instanceof REnumValueRef ref && ref.enumeration().isPresent()) {
                // Bare qualified Enum.VALUE — wrap to the golden MapperS.of(Enum.VALUE)
                // mapper form (same factory the literal element kinds self-wrap with).
                javaExpr = JavaExpression.wrappedInMapperSOf(javaExpr);
            } else if (element instanceof RSymbolReference sr
                    && sr.symbol().filter(REnumValue.class::isInstance).isPresent()) {
                // facet void_witness_bare_enum (arm D3): a bare element name the
                // Cat-13c parser pass (TypeInferenceEngine) bound to an enum VALUE
                // renders the qualified constant UNWRAPPED (ReferenceHandler's
                // Cat-13 branch is unwrapped by design — its other consumers
                // self-wrap); the list-literal element position is one such
                // consumer, so it takes the SAME wrap as the qualified
                // REnumValueRef element form above.
                javaExpr = JavaExpression.wrappedInMapperSOf(javaExpr);
            } else if (element instanceof RConstructorExpr) {
                // facet mapperCofMapperSWrap (PR #211): a CONSTRUCTOR element
                // (RConstructorExpr — X.builder()...build()) compiles BARE
                // (ExpressionCompiler.visitConstructor; the expected-type coercion never
                // wraps a constructor — PR #180 established this at the lambda-body seat,
                // adding wrappedInMapperSOf there), but the MapperC.<X>of(...) list-literal
                // element seat expects each element to be a Mapper. Golden wraps it
                // MapperS.of(X.builder()...build()) — the SAME #180 wrappedInMapperSOf law at
                // the list-literal element position (the function-call element sibling is
                // already wrapped via expected-type coercion in compile, above).
                javaExpr = JavaExpression.wrappedInMapperSOf(javaExpr);
            } else if (element instanceof RSymbolReference sr
                    && sr.args().isEmpty()
                    && sr.symbol().filter(RFunction.class::isInstance).isPresent()
                    && HandlerHelper.findEnclosingRule(element) != null) {
                // facet bareInvocationMapperCWrap (PR #278), arm A2: a bare no-args
                // FUNCTION-invocation element (`settlementTermsLeg1`) — which
                // ReferenceHandler.renderImplicitFunctionInvocation emits UNWRAPPED as
                // <fn>.evaluate(item.get()) (the bare-FUNCTION render contract, distinct
                // from an EXPLICIT-args call whose carried type lets the expected-type
                // coercion wrap it) — at the MapperC.<X>of(...) list-literal element seat
                // expects each element to be a Mapper. Golden wraps it
                // MapperS.of(<fn>.evaluate(...)) — the SAME #180/#211 wrappedInMapperSOf law
                // as the enum / constructor element arms above, at the bare-FUNCTION
                // element position. Green-safe by construction: a bare item at a
                // MapperC.of element position does not compile (MapperC.of expects Mappers),
                // so golden ALWAYS wraps it and NO green file carries the bare form.
                // RULE-scoped (findEnclosingRule != null) -> FUNCTION-byte-neutral. Corpus
                // witness: drr SettlementLocationRule (iosco cde version1,
                // [settlementTermsLeg1, settlementTermsLeg2] over two sub-functions).
                //
                // facet existsOperandMapperCWrap (PR #302): when the element function's OUTPUT is
                // MULTI, golden wraps MapperC.<X>of(...) not MapperS.of(...) — the #301 (C)
                // cardinality-aware wrap at the list-literal element seat. Corpus witness: drr
                // ExchangeRateRule (iosco cde, [contract_Price, contract_StrikePrice] over two
                // PriceSchedule (0..*) sub-functions -> MapperC.<PriceSchedule>of(...) elements).
                // A SINGLE-output element keeps MapperS.of (the #278 A2 form unchanged). Shares the
                // HandlerHelper SOT cardinality decision with the exists/comparison operand seats.
                JavaClass<?> multiWitness = HandlerHelper.bareMultiOutputWitness(element, compiler);
                if (multiWitness != null) {
                    JavaTypeUtil typeUtil = compiler.getTypeUtil();
                    String fqnWitness =
                            HandlerHelper.bareMultiOutputWitnessFqn(multiWitness, element, compiler);
                    javaExpr = JavaExpression.wrappedInMapperCOfSingle(javaExpr,
                            typeUtil.wrap(typeUtil.MAPPER_C, multiWitness), multiWitness, fqnWitness);
                } else {
                    javaExpr = JavaExpression.wrappedInMapperSOf(javaExpr);
                }
            } else if (element instanceof RCountExpr) {
                // facet countListLiteralElementWrap (W42 finding #19, PR #432): a COUNT
                // element compiles to the bare `int` (`<recv>.resultCount()` — upstream
                // caseCountOperation's own JavaPrimitiveType.INT stamp), but the
                // MapperC.<X>of(...) element seat expects each element to be a Mapper —
                // upstream compiles every literal element against the Mapper element
                // type (caseListLiteral's withExpected(MapperS.wrapExtends(itemType))),
                // so its item→MapperS coercion wraps golden's
                // `MapperS.of(<recv>.resultCount())` (the #429 MAPPER_EXPECTING
                // RCountExpr law at the list-literal element position; the same
                // wrappedInMapperSOf convention as the enum/constructor/bare-function
                // element arms above). Green-safe by the #357 construction: a bare int
                // at a MapperC.of element position never compiled, so no green file
                // carries the unwrapped form.
                javaExpr = JavaExpression.wrappedInMapperSOf(javaExpr);
            } else {
                // facet metaValueDerefHoist (PR #361): an element that COLLAPSES to a
                // META-WRAPPER item (a single-line `.get()`-terminal render whose
                // element is FieldWithMetaX — a bare item at a MapperC.of element
                // position never compiles, the #357 law) hoists the collapsed wrapper
                // and re-presents the VALUE guarded through the SHARED
                // metaCollapseDerefHoistOrNull emission (golden drr SortIdentifiers
                // `final FieldWithMetaString fieldWithMetaString0..4 = <collapse>;`
                // consumed as `(x == null ? MapperS.<String>ofNull() : MapperS.of(
                // x.getValue()))` elements). Declines (bytes unchanged) without a
                // reachable sink or when no meta wrapper proves.
                // facet collapseGetSuffix (v3.1 C2d retirement family 7, PR #614): the
                // "does this element COLLAPSE to a bare item?" half reads the family's shared
                // arbiter (an ONLY_ELEMENT-rooted element over a null-typed compiled value); the
                // single-line half STAYS as the splice guard it was written to be — elementText is
                // spliced verbatim into the hoisted decl below. The c7b census at this seat:
                // 26,512 / 26,512 / 15,336 arrivals (default-route, IR-route, optimised),
                // text=true at 40 (the fn:SortIdentifiers class), and
                // `ONLY_ELEMENT AND typeNull` selects exactly those 40 on all three walks. The
                // row's named channel would NOT have: the recovered wrapper is FieldWithMetaString
                // at every fire but ALSO at 36 same-typeNull arrivals differing only by root op, so
                // the wrapper alone over-fires.
                String elementText = HandlerHelper.render(javaExpr);
                if (HandlerHelper.bareOnlyElementCollapse(element, javaExpr)
                        && !elementText.contains("\n")) {
                    RJavaWithMetaValue elemMeta =
                            ConversionHandler.collapseMetaWrapper(element, compiler);
                    if (elemMeta != null) {
                        Set<JavaClass<?>> derefRefs = new HashSet<>(javaExpr.getRefs());
                        String deref = HandlerHelper.metaCollapseDerefHoistOrNull(
                                elementText, elemMeta, ctx.scope(), derefRefs);
                        if (deref != null) {
                            javaExpr = JavaExpression.from(deref, null, derefRefs,
                                    javaExpr.getStaticWildcardImports());
                        }
                    }
                }
                // facet ruleInteriorBoundChainDecomp (PR #395): a MapperC-kinded META
                // element whose wrapper VALUE type equals the literal's joined bare
                // item coerces at the element seat — upstream coerces every element at
                // the joined type, so golden appends the #310 bare MapperC deref
                // (`.<PriceSchedule>map("Type coercion", fieldWithMetaPriceSchedule ->
                // fieldWithMetaPriceSchedule.getValue())` — the fca/esma FER list
                // literal's filter-chain element, whose type the #395 FILTER stamp
                // threads out). Routed through the SAME TypeCoercionService the
                // expected-type path uses (witness + deferred param + refs — no
                // hand-rolled drift). An all-meta literal joins to the WRAPPER
                // (listItemJavaType's #327 B3 lift) → value-type mismatch → no fire;
                // a type-less element keeps its bytes.
                JavaTypeUtil coerceTu = compiler.getTypeUtil();
                JavaType elemT = javaExpr.getExpressionType();
                if (coerceTu != null && joinedItem != null && elemT != null
                        && compiler.getCoercionService() != null
                        && coerceTu.isMapperC(elemT)
                        && coerceTu.getItemType(elemT) instanceof RJavaWithMetaValue elemWrap
                        && elemWrap.getValueType() instanceof JavaClass<?> elemBare
                        && elemBare.getCanonicalName().withDots()
                                .equals(joinedItem.getCanonicalName().withDots())) {
                    JavaStatementBuilder coerced = compiler.getCoercionService().coerce(
                            javaExpr, elemT, coerceTu.wrap(coerceTu.MAPPER_C, joinedItem),
                            ctx.scope());
                    if (coerced instanceof JavaExpression coercedExpr) {
                        javaExpr = coercedExpr;
                    }
                }
            }
            inners.add(javaExpr);
        }
        return JavaExpression.wrappedInMapperCOf(inners, null, joinedItem);
    }

    /**
     * The Java reference type of a list literal's items — the {@code <Item>} witness
     * in {@code MapperC.<Item>of(...)}. Read from the inference engine's joined
     * element type ({@code ExpressionTypeComputer.computeListLiteral}); the same
     * type map {@code RuleGenerator} reads. Returns {@code null} when the model,
     * translator, or inferred type is unavailable (→ bare {@code MapperC.of(...)},
     * no witness).
     *
     * <p>PUBLIC since facet listLiteralItemCoerce (PR #420):
     * {@code FunctionExpressionRenderer}'s whole-output-SET coercion arm reads the
     * SAME walk this render witnessed the {@code MapperC.<Item>of} with, so the
     * coercion's actual-item and the emitted witness cannot disagree (the #178
     * same-walk law).
     */
    public static JavaClass<?> listItemJavaType(RListLiteral expr, ExpressionCompiler compiler) {
        if (expr.elements().isEmpty()) {
            // Empty list literal: the inferred item type is NOTHING (the join
            // identity), which the translator maps to Void — emitting a
            // MapperC.<Void>of() witness would be wrong (and NOTHING is not
            // RMissingType, so the isMissing() guard below would not catch it).
            // Keep the bare MapperC.of() form (unchanged from before the witness).
            return null;
        }
        if (compiler.getGeneratorModel() == null || compiler.getTypeTranslator() == null) {
            return null;
        }
        RMetaAnnotatedType inferred = compiler.getGeneratorModel().workspace().getInferredType(expr);
        if (inferred == null || inferred.isMissing()) {
            return null;
        }
        // facet void_witness_bare_enum (arm D1): the inference-side element join
        // treats MISSING elements as bottom (TypeJoin), so a literal whose
        // elements the cache-only inference map cannot type joins to NOTHING —
        // which the translator maps to Void, a witness NO golden carries
        // (MapperC.of(MapperBuilder<? extends T>...) cannot accept a non-Void
        // element, so the pre-fix render is non-compiling). Gated EXACTLY on
        // that shape, re-derive the witness from the SAME gm-aware per-element
        // type walk the navigation renderer types the elements' own chains with
        // — so the witness and the element bytes cannot disagree. Declines
        // (keeping today's bytes) unless every element walks to ONE identical
        // type; meta-annotated leafs decline (the concrete FieldWithMetaX
        // witness is not derivable here — those carriers are multi-mechanism).
        // The isMissing()/empty bare-MapperC.of paths above are NOT touched.
        if (inferred.type() instanceof RBasicType b && "nothing".equals(b.name())) {
            JavaClass<?> walked = walkedElementItemType(expr, compiler);
            if (walked != null) {
                return walked;
            }
        }
        JavaClass<?> bare = compiler.getTypeTranslator().toJavaReferenceType(inferred.type());
        // facet setterNameValueMeta (PR #327, facet B3 witness): upstream's element-type
        // join KEEPS meta, so a literal ALL of whose elements are direct function calls
        // with the SAME attribute-meta-annotated output witnesses the WRAPPER
        // (`MapperC.<FieldWithMetaX>of(...)` — the cdm6 MapMessageInformation /
        // MapFxVarianceSwapPriceQuantityList / MapReturnLegToPriceQuantity carriers);
        // the parser-side join strips it to the bare type. Lifted from the element
        // PROOF alone (ConstructionHandler.valueProvenMetaKind per element — call-truth),
        // consumer-blind; mixed/unproven/none elements keep the bare witness (today's
        // bytes). The wrapper class rides the witness refs channel atomically, so the
        // stale bare-type import drops with the lift.
        if (bare != null && compiler.getTypeUtil() != null) {
            MetaFieldGenerator.MetaKind lifted = allElementsProvenMetaKind(expr);
            if (lifted != MetaFieldGenerator.MetaKind.NONE) {
                return RJavaWithMetaValue.create(
                        lifted == MetaFieldGenerator.MetaKind.REFERENCE_WITH_META,
                        bare, compiler.getTypeUtil());
            }
        }
        // facet listLiteralNavMetaWitness (PR #337): the NAV sibling of #327 B3's call-truth
        // lift. A list literal ALL of whose elements NAVIGATE (through first/only-element
        // list-ops) to the SAME meta-annotated LEAF attribute witnesses the WRAPPER
        // (`MapperC.<FieldWithMetaString>of(...)`), not the parser-stripped value type — the
        // parser-side element join strips meta to the bare value (the #327 comment above). The
        // elements are wrapper-typed Mappers (`<meta-nav> first` = MapperS<FieldWithMetaString>),
        // so the bare `<String>` witness is a type lie (`MapperC.<String>of(...)` over
        // FieldWithMetaString args) that never compiled → no green golden carries it (green-safe
        // by construction; the wrapper-witnessed collapse then deref-reconstructs at the arm
        // return — the part-2 sibling in FunctionExpressionRenderer). Carriers: drr
        // DTCC_Leg1/2FloatingRateIndexRule `[InterestRateLeg1 -> … -> indexId first, … ->
        // indexId first] only-element`. The `to-string` sibling branch (`[… floatingRateIndex
        // to-string, …] only-element`) DECLINES — its elements are CONVERSIONS, not meta-nav
        // leafs, so onlyElementLeafAttribute returns null and the bare String witness (already
        // golden) stays. Gate: the lifted wrapper's VALUE type must equal the parser-stripped
        // inferred `bare` (the join and the per-element wrapper agree), else keep today's bytes.
        if (bare != null && compiler.getTypeUtil() != null) {
            RJavaWithMetaValue navWrapper = allElementsNavMetaWrapper(expr, compiler, bare);
            if (navWrapper != null) {
                return navWrapper;
            }
        }
        // facet listLiteralAliasMetaWitness (PR #345): the ALIAS sibling of #327 B3's
        // call-truth / #337's nav-truth lifts. A literal ALL of whose elements are
        // ALIAS invocations whose signature element (the SAME
        // FunctionAliasHelper.inferShortcutMapperJavaType walk that renders each
        // `protected abstract MapperC<? extends FieldWithMetaString> entityIdFrom…(…)`
        // method into this file — render-truth lockstep) is ONE meta wrapper whose
        // value type equals the parser-stripped join witnesses the WRAPPER
        // (`MapperC.<FieldWithMetaString>of(entityIdFromLegalEntitySequence(…), …)`,
        // MapLegalEntity cdm6; 16 golden MapperC.<FieldWithMeta…>of( files). The
        // elements are wrapper-typed Mappers, so the bare `<String>` witness is a
        // type lie that never compiled → no green golden carries it (green-safe by
        // construction). Mixed / non-alias / non-meta / disagreeing-wrapper element
        // sets decline (today's bytes).
        if (bare != null && compiler.getTypeUtil() != null) {
            RJavaWithMetaValue aliasWrapper = null;
            boolean allAliasMeta = true;
            for (RExpression element : expr.elements()) {
                RJavaWithMetaValue wrapper =
                        NavigationHandler.aliasInvocationMetaWrapper(element, compiler);
                if (wrapper == null || !wrapper.getValueType().equals(bare)
                        || (aliasWrapper != null && !wrapper.equals(aliasWrapper))) {
                    allAliasMeta = false;
                    break;
                }
                aliasWrapper = wrapper;
            }
            if (allAliasMeta && aliasWrapper != null) {
                return aliasWrapper;
            }
        }
        return bare;
    }

    /**
     * facet setterNameValueMeta (PR #327, facet B3): the single attribute-meta kind EVERY
     * element of {@code expr} provably carries ({@code ConstructionHandler
     * .valueProvenMetaKind} — a direct fn call whose output is meta-annotated, or an
     * extract over one), or {@link MetaFieldGenerator.MetaKind#NONE} when any element is
     * unproven or the kinds disagree.
     */
    private static MetaFieldGenerator.MetaKind allElementsProvenMetaKind(RListLiteral expr) {
        MetaFieldGenerator.MetaKind agreed = null;
        for (RExpression element : expr.elements()) {
            MetaFieldGenerator.MetaKind kind = ConstructionHandler.valueProvenMetaKind(element);
            if (kind == MetaFieldGenerator.MetaKind.NONE) {
                return MetaFieldGenerator.MetaKind.NONE;
            }
            if (agreed == null) {
                agreed = kind;
            } else if (agreed != kind) {
                return MetaFieldGenerator.MetaKind.NONE;
            }
        }
        return agreed == null ? MetaFieldGenerator.MetaKind.NONE : agreed;
    }

    /**
     * facet listLiteralNavMetaWitness (PR #337): the single {@link RJavaWithMetaValue} wrapper
     * every element of {@code expr} navigates to — each element resolves (through its
     * {@code first}/{@code only-element} list-ops, via
     * {@link NavigationHandler#onlyElementLeafAttribute}) to a meta-annotated LEAF attribute, and
     * ALL elements agree on the SAME wrapper whose VALUE type equals the parser-stripped inferred
     * {@code bare} type. Returns {@code null} (→ the bare witness, today's bytes) when any element
     * is not a meta-nav leaf (a conversion / non-nav / non-meta element — the {@code to-string}
     * sibling branch), the wrappers disagree, or the wrapper's value type does not match
     * {@code bare}.
     */
    private static RJavaWithMetaValue allElementsNavMetaWrapper(
            RListLiteral expr, ExpressionCompiler compiler, JavaClass<?> bare) {
        if (bare == null) {
            return null;
        }
        RJavaWithMetaValue agreed = null;
        for (RExpression element : expr.elements()) {
            RAttribute leaf = NavigationHandler.onlyElementLeafAttribute(element, compiler);
            // facet listLiteralThenMetaWitness (PR #376, J): the THEN-CHAIN sibling of
            // the #337 nav arm — an element that is a then-chain (`<nav> filter … then
            // first extract item -> identifier`) resolves its leaf through the LAST
            // then-body's expression (the chain's element type IS that body's result),
            // so a homogeneous meta-leaf literal witnesses the WRAPPER
            // (`MapperC.<FieldWithMetaString>of(…)` — golden GetCreditUnderlierISIN;
            // the whole-output guarded `.getValue()` deref then fires off the join's
            // compiled type, the existing #331-class consumer). The same
            // meta/agreement/value-type gates below apply; a filter/arith/collapse-less
            // last body resolves no leaf and declines to the bare witness (today's
            // bytes). Green-safe by the #337 argument: the bare `<String>` witness over
            // wrapper-typed Mapper args never compiled.
            if (leaf == null && element instanceof RThenExpr elemThen) {
                RExpression lastBody = elemThen.body()
                        .map(com.regnosys.rosetta.ast.expressions.supporting.RInlineFunction::body)
                        .orElse(null);
                // A `then … extract [<nav>]` last body wraps the leaf-producing nav in an
                // RExtractExpr — the element type is the extract BODY's result, so
                // resolve through it (the #352 `then extract [chain]` wrapping).
                if (lastBody instanceof com.regnosys.rosetta.ast.expressions.unary.RExtractExpr ext
                        && ext.body() != null) {
                    lastBody = ext.body().body();
                }
                if (lastBody != null) {
                    leaf = NavigationHandler.onlyElementLeafAttribute(lastBody, compiler);
                }
            }
            RJavaWithMetaValue wrapper;
            if (leaf != null) {
                if (MetaFieldGenerator.detectMetaKind(leaf) == MetaFieldGenerator.MetaKind.NONE) {
                    return null;
                }
                wrapper = NavigationHandler.metaWrapperOf(leaf, compiler);
            } else {
                // facet deepThenLevelElementPreserve (PR #362): a RULE-context element whose
                // disguised 2-name chain roots at the enclosing extract's ITEM resolves
                // through the #360 (a3) RECOVERY-LOCAL read (the shared
                // resolveDisguisedFeature is function-gated — the cp2e green-regression
                // boundary — so onlyElementLeafAttribute's EVR arm declines in rules). The
                // read returns the leaf's wrapper directly; the same bare-value-type
                // agreement gates below apply, so a meta-free or disagreeing element still
                // declines to the bare witness (golden esma/fca UPIRule:
                // `MapperC.<ReferenceWithMetaProductIdentifier>of(…)` over the
                // [contractualProduct -> productIdentifier, security -> productIdentifier]
                // base literal — the bare witness over wrapper-typed Mapper args never
                // compiled, the #337 green-safety argument unchanged).
                wrapper = NavigationHandler.ruleContextDisguisedLeafMeta(element, compiler);
            }
            if (wrapper == null || !bare.equals(wrapper.getValueType())) {
                return null;
            }
            if (agreed == null) {
                agreed = wrapper;
            } else if (!agreed.getSimpleName().equals(wrapper.getSimpleName())) {
                return null;
            }
        }
        return agreed;
    }

    /**
     * facet listLiteralNavMetaWitness (PR #337): the {@link RJavaWithMetaValue} wrapper of a list
     * literal ALL of whose elements navigate to the SAME meta-annotated leaf (see
     * {@link #allElementsNavMetaWrapper}), or {@code null}. Package-visible so the part-2
     * arm-return reconstruct fires on EXACTLY the shape part 1 lifts the witness for — a
     * `[meta-nav first, …] only-element` collapse consumed at the value output.
     */
    public static RJavaWithMetaValue listLiteralNavMetaWrapper(RListLiteral expr, ExpressionCompiler compiler) {
        if (expr.elements().isEmpty()
                || compiler.getGeneratorModel() == null
                || compiler.getTypeTranslator() == null
                || compiler.getTypeUtil() == null) {
            return null;
        }
        RMetaAnnotatedType inferred = compiler.getGeneratorModel().workspace().getInferredType(expr);
        if (inferred == null || inferred.isMissing()) {
            return null;
        }
        JavaClass<?> bare = compiler.getTypeTranslator().toJavaReferenceType(inferred.type());
        return allElementsNavMetaWrapper(expr, compiler, bare);
    }

    /**
     * The single Java item type every element of {@code expr} walks to via the
     * generator-side gm-aware type walk, or {@code null} when any element's type
     * is unknown, meta-annotated, or disagrees with a sibling's (arm D1's decline
     * ladder — no LUB machinery here; every corpus carrier joins to one type).
     */
    private static JavaClass<?> walkedElementItemType(RListLiteral expr, ExpressionCompiler compiler) {
        JavaClass<?> agreed = null;
        for (RExpression element : expr.elements()) {
            JavaClass<?> elemType = walkedItemType(element, compiler);
            if (elemType == null) {
                return null;
            }
            if (agreed == null) {
                agreed = elemType;
            } else if (!agreed.equals(elemType)) {
                return null;
            }
        }
        return agreed;
    }

    /**
     * One element's walked item type:
     * <ul>
     *   <li>a navigation chain ({@link RFeatureCall} or a disguised
     *       {@link REnumValueRef}) — the LEAF attribute via the SAME gm-aware
     *       resolution the navigation renderer's {@code <Type>} witnesses use
     *       ({@code NavigationHandler.fallbackResolveFeature} /
     *       {@code resolveDisguisedFeature}); declines meta-annotated leafs and
     *       non-data-typed leafs;</li>
     *   <li>a bare attribute reference — the attribute's data type (same meta
     *       decline);</li>
     *   <li>a symbol bound to an enum VALUE (post-Cat-13c) — the parent
     *       enumeration;</li>
     *   <li>a qualified enum constant ({@code E -> V}) — the enumeration;</li>
     *   <li>the implicit {@code item} — the enclosing lambda's item type
     *       ({@code NavigationHandler.implicitItemDataType}, the same walk the
     *       lambda-naming/witness mechanisms read).</li>
     * </ul>
     * Every other shape returns {@code null} (decline).
     */
    private static JavaClass<?> walkedItemType(RExpression element, ExpressionCompiler compiler) {
        if (element instanceof RFeatureCall fc) {
            RAttribute leaf = fc.resolvedFeature()
                    .orElseGet(() -> NavigationHandler.fallbackResolveFeature(fc, compiler));
            return leafAttributeItemType(leaf, compiler);
        }
        if (element instanceof REnumValueRef evr) {
            if (evr.enumeration().isPresent()
                    && evr.enumeration().get() instanceof REnumeration en) {
                return compiler.getTypeTranslator().toJavaReferenceType(new REnumTypeRef(en));
            }
            if (evr.enumeration().isEmpty()) {
                RAttribute leaf = NavigationHandler.resolveDisguisedFeature(evr, compiler, null);
                return leafAttributeItemType(leaf, compiler);
            }
            return null;
        }
        if (element instanceof RSymbolReference sr) {
            var sym = sr.symbol().orElse(null);
            if (sym instanceof REnumValue ev
                    && ev.parent() instanceof REnumeration en) {
                return compiler.getTypeTranslator().toJavaReferenceType(new REnumTypeRef(en));
            }
            if (sym instanceof RAttribute attr) {
                return leafAttributeItemType(attr, compiler);
            }
            if (sym instanceof RShortcut) {
                // facet listLiteralAliasElementJoin (PR #326, F3b): an ALIAS-call element
                // joins at the alias's OWN body element type — the parser-side element
                // join yields NOTHING for alias elements, so DetermineObservationPeriod's
                // `[businessDays, additionalBusinessDays]` witnessed the non-compiling
                // MapperC.<Void>of where golden carries MapperC.<BusinessCenters>of. The
                // non-meta item comes from the SAME #238 body walk the alias-leaf thenArg
                // decls use (aliasDerivedThenArgItemType), so the witness and the alias
                // method's rendered element cannot disagree. Unresolvable walks decline
                // (null).
                //
                // facet listLiteralMetaAliasElementJoin (PR #390): a META alias element
                // joins at its CONCRETE wrapper — the item of the SAME
                // tryAliasReceiverMapperType walk that renders the alias signature
                // (golden drr IsCommodityBullion/Metal productClass:
                // `MapperC.<FieldWithMetaString>of(productTaxonomy(product),
                // productIdentifier(product))`; the #326 null pre-dated a golden
                // carrier — the wrapper IS derivable here, unlike the attribute arm's
                // leaf-only walk). The D1 caller fires only on NOTHING-joins, whose
                // pre-fix Void witness never compiled — green-safe by construction.
                JavaType aliasMapper = NavigationHandler.tryAliasReceiverMapperType(sr, compiler);
                if (aliasMapper != null) {
                    JavaType aliasItem = compiler.getTypeUtil() == null ? null
                            : compiler.getTypeUtil().getItemType(aliasMapper);
                    return aliasItem instanceof RJavaWithMetaValue
                            && aliasItem instanceof JavaClass<?> wrapperClass
                            ? wrapperClass : null;
                }
                return NavigationHandler.aliasDerivedThenArgItemType(sr, compiler);
            }
            return null;
        }
        if (element instanceof RImplicitVariable) {
            // The implicit-item walk yields the plain RDataType (a meta-wrapped
            // item would witness its value type) — acceptable: the arm is only
            // reachable on NOTHING-join carriers whose pre-fix render is
            // non-compiling, and no corpus carrier binds a meta-wrapped item.
            RDataType itemType = NavigationHandler.implicitItemDataType(element, compiler);
            return itemType == null
                    ? null
                    : compiler.getTypeTranslator().toJavaReferenceType(new RDataTypeRef(itemType));
        }
        return null;
    }

    /**
     * A leaf attribute's witness item type: its data type when the attribute is
     * NOT meta-annotated; {@code null} (decline) for meta leafs (golden witnesses
     * the concrete {@code FieldWithMetaX} there) and non-data-typed leafs.
     */
    private static JavaClass<?> leafAttributeItemType(RAttribute leaf, ExpressionCompiler compiler) {
        if (leaf == null
                || MetaFieldGenerator.detectMetaKind(leaf) != MetaFieldGenerator.MetaKind.NONE) {
            return null;
        }
        RDataType dt = NavigationHandler.attributeToDataType(leaf, compiler);
        return dt == null
                ? null
                : compiler.getTypeTranslator().toJavaReferenceType(new RDataTypeRef(dt));
    }
}
