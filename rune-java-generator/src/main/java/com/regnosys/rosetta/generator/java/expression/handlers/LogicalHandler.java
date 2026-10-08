package com.regnosys.rosetta.generator.java.expression.handlers;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.expressions.binary.RLogicalExpr;
import com.regnosys.rosetta.ast.expressions.binary.RThenExpr;
import com.regnosys.rosetta.ast.expressions.unary.RExtractExpr;
import com.regnosys.rosetta.ast.expressions.constructors.RConditionalExpr;
import com.regnosys.rosetta.ast.expressions.literals.RBooleanLiteral;
import com.regnosys.rosetta.ast.expressions.references.RSymbolReference;
import com.regnosys.rosetta.ast.enums.ListOp;
import com.regnosys.rosetta.ast.expressions.references.REnumValueRef;
import com.regnosys.rosetta.ast.expressions.references.RFeatureCall;
import com.regnosys.rosetta.ast.expressions.unary.RListOpExpr;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.functions.RRule;
import com.regnosys.rosetta.ast.functions.RShortcut;
import com.regnosys.rosetta.generator.java.expression.ExpressionCompiler;
import com.regnosys.rosetta.generator.java.expression.ExpressionContext;
import com.regnosys.rosetta.generator.java.statement.builder.JavaExpression;
import com.regnosys.rosetta.generator.java.statement.builder.JavaStatementBuilder;
import com.rosetta.util.types.JavaClass;
import com.rosetta.util.types.JavaType;

import java.util.HashSet;
import java.util.Set;

/**
 * Handles code generation for logical binary expressions (AND, OR).
 *
 * <p>Golden output patterns (verified against CDM golden files):
 * <pre>
 *   a and b  →  left.andNullSafe(right)
 *   a or  b  →  left.orNullSafe(right)
 * </pre>
 *
 * <p>Both operations are method calls on the compiled left operand rather than
 * static function calls; this mirrors the mapper-chain pattern used throughout
 * the Rosetta runtime.
 */
public class LogicalHandler {

    /**
     * Compiles an {@link RLogicalExpr} (and/or) into an andNullSafe/orNullSafe
     * method call.
     *
     * @param expr     the logical expression node
     * @param ctx      the current expression compilation context
     * @param compiler the parent compiler for recursive compilation of operands
     * @return a {@link JavaExpression} rendering the andNullSafe/orNullSafe call
     */
    public JavaStatementBuilder handle(RLogicalExpr expr, ExpressionContext ctx, ExpressionCompiler compiler) {
        // PR-A §9.1 C3a.4.j: operand-refs flowthrough.
        String method = switch (expr.op()) {
            case AND -> "andNullSafe";
            case OR  -> "orNullSafe";
        };

        // facet ifthenelse_result_hoisting (arm A2): a CONDITIONAL operand of a
        // logical and/or compiles against the expected ComparisonResult — the
        // andNullSafe/orNullSafe receiver/argument type — so ControlFlowHandler
        // hoists it as a `final ComparisonResult ifThenElseResultN;` statement
        // local (upstream threads the same expectation into every operand;
        // the targeted RConditionalExpr gate keeps every other operand's
        // compilation byte-identical — and a logical-OPERAND conditional
        // previously rendered the inline ternary, which no golden carries, so
        // every file this threading touches is a waivered mismatch).
        // Coverage wave D (datarule): the A2 logical-combine deferral — upstream
        // allocates each conditional operand's `ifThenElseResult` name at the
        // COMPOSITION (`JavaIfThenElseBuilder.then` collapses the LEFT receiver at
        // combine time, AFTER the right subtree composed during its own javaCode
        // construction), so `A and (B and C)` numbers B=0, C=1, A=2 while emitting
        // in source order (golden CFTCPart43TransactionReport
        // FloatingRateResetFrequencyPeriodCond). Each operand compiles under its
        // own frame; the resolve below registers left-frame entries then
        // right-frame entries. TYPE-CONDITION-gated + sink-gated: function/rule
        // paths never push a frame, and ControlFlowHandler's deferral arm is
        // frame-keyed — their bytes are untouched.
        com.regnosys.rosetta.generator.java.scoping.JavaStatementScope a2Scope =
                ctx.scope() != null
                        && HandlerHelper.findEnclosingTypeCondition(expr) != null
                        && ctx.scope().findStatementHoistSink() != null
                ? ctx.scope() : null;
        com.regnosys.rosetta.generator.java.scoping.JavaStatementScope.A2DeferralFrame leftFrame =
                a2Scope == null ? null
                        : new com.regnosys.rosetta.generator.java.scoping.JavaStatementScope
                                .A2DeferralFrame(expr.rawLeft());
        com.regnosys.rosetta.generator.java.scoping.JavaStatementScope.A2DeferralFrame rightFrame =
                a2Scope == null ? null
                        : new com.regnosys.rosetta.generator.java.scoping.JavaStatementScope
                                .A2DeferralFrame(expr.rawRight());
        JavaStatementBuilder leftBuilder;
        if (leftFrame != null) {
            var prevFrame = a2Scope.pushA2DeferralFrame(leftFrame);
            try {
                leftBuilder = compiler.compile(expr.rawLeft(),
                        operandExpectedType(expr.rawLeft(), ctx, compiler), ctx.scope());
            } finally {
                a2Scope.popA2DeferralFrame(prevFrame);
            }
        } else {
            leftBuilder = compiler.compile(expr.rawLeft(),
                    operandExpectedType(expr.rawLeft(), ctx, compiler), ctx.scope());
        }
        JavaStatementBuilder rightBuilder;
        if (rightFrame != null) {
            var prevFrame = a2Scope.pushA2DeferralFrame(rightFrame);
            try {
                rightBuilder = compiler.compile(expr.rawRight(),
                        operandExpectedType(expr.rawRight(), ctx, compiler), ctx.scope());
            } finally {
                a2Scope.popA2DeferralFrame(prevFrame);
            }
        } else {
            rightBuilder = compiler.compile(expr.rawRight(),
                    operandExpectedType(expr.rawRight(), ctx, compiler), ctx.scope());
        }

        // facet existsMetaSeats (PR #331): a #315 meta-wrap exists/notExists operand compiles
        // to a JavaConditionalExpression whose branches are the DISTRIBUTED (not)exists calls
        // (ComparisonResult-typed after ExistenceHandler's mapExpression). andNullSafe/orNullSafe
        // chain on the WHOLE conditional — golden parenthesizes:
        //   (string == null ? notExists(MapperS.<W>ofNull()) : notExists(MapperS.of(…))).andNullSafe(…)
        // Collapse to the parenthesized single expression (collapseToSingleExpression) in BOTH
        // operand positions (hkma UATPI's second rung carries one on EACH side). Gated on the
        // conditional's type being ComparisonResult — the distributed-existence marker — so a
        // hypothetical non-#315 conditional operand keeps today's path (HandlerHelper.render
        // throws → the generator's crash-stub fail-fast, never silent wrong bytes).
        if (leftBuilder instanceof com.regnosys.rosetta.generator.java.statement.builder.JavaConditionalExpression leftCond
                && isComparisonResultConditional(leftCond)) {
            leftBuilder = leftCond.collapseToSingleExpression(ctx.scope());
        }
        if (rightBuilder instanceof com.regnosys.rosetta.generator.java.statement.builder.JavaConditionalExpression rightCond
                && isComparisonResultConditional(rightCond)) {
            rightBuilder = rightCond.collapseToSingleExpression(ctx.scope());
        }

        Set<JavaClass<?>> refs = new HashSet<>();
        refs.addAll(leftBuilder.getRefs());
        refs.addAll(rightBuilder.getRefs());
        Set<JavaClass<?>> wildcards = new HashSet<>();
        wildcards.addAll(leftBuilder.getStaticWildcardImports());
        wildcards.addAll(rightBuilder.getStaticWildcardImports());

        // or_chain_filter_predicate facet: andNullSafe/orNullSafe are ComparisonResult
        // instance methods, so a boolean-FUNCTION operand (rendered as a raw Boolean or
        // a MapperS<Boolean>, neither of which is a ComparisonResult) must be coerced to
        // a ComparisonResult before chaining. See {@link #wrapBooleanFunctionOperand}.
        String left  = wrapBooleanFunctionOperand(
                HandlerHelper.render(leftBuilder),  expr.rawLeft(),  leftBuilder,  refs, compiler);
        String right = wrapBooleanFunctionOperand(
                HandlerHelper.render(rightBuilder), expr.rawRight(), rightBuilder, refs, compiler);

        // Coverage wave D (datarule): resolve the deferred A2 sentinels at the
        // combine — left frame first, then right (see the frame-push comment
        // above); the sink's pending blocks AND the operand render substitute the
        // same session token.
        if (a2Scope != null) {
            var a2Sink = a2Scope.findStatementHoistSink();
            if (a2Sink == null
                    && (!leftFrame.placeholders.isEmpty()
                            || !rightFrame.placeholders.isEmpty())) {
                // Unreachable by the frame-push gate (a frame is only created when
                // the SAME scope walk reaches a sink, and the walk is over an
                // immutable parent chain) — fail LOUD rather than leak a
                // placeholder into emitted bytes (Seat-1 #409 hardening; the
                // never-silent-wrong-bytes law).
                throw new IllegalStateException(
                        "A2 deferral placeholders pending but the statement-hoist sink "
                        + "vanished between the operand compile and the combine");
            }
            if (a2Sink != null) {
                for (String ph : leftFrame.placeholders) {
                    String actual = a2Sink.statementHoistSession().register(
                            com.regnosys.rosetta.generator.java.function.StatementHoistSession
                                    .IF_THEN_ELSE_RESULT);
                    a2Sink.replaceInStatementHoists(ph, actual);
                    left = left.replace(ph, actual);
                    right = right.replace(ph, actual);
                }
                for (String ph : rightFrame.placeholders) {
                    String actual = a2Sink.statementHoistSession().register(
                            com.regnosys.rosetta.generator.java.function.StatementHoistSession
                                    .IF_THEN_ELSE_RESULT);
                    a2Sink.replaceInStatementHoists(ph, actual);
                    left = left.replace(ph, actual);
                    right = right.replace(ph, actual);
                }
            }
        }

        return JavaExpression.from(
                left + "." + method + "(" + right + ")",
                null,
                refs,
                wildcards);
    }

    /**
     * facet existsMetaSeats (PR #331): the distributed-existence marker — the
     * conditional's joined branch type is {@code ComparisonResult} (both branches
     * are the ExistenceHandler-distributed {@code (not)exists(…)} calls). See the
     * call-site comment for why only this shape collapses.
     */
    private static boolean isComparisonResultConditional(
            com.regnosys.rosetta.generator.java.statement.builder.JavaConditionalExpression cond) {
        JavaType t = cond.getExpressionType();
        return t != null && t.equals(HandlerHelper.COMPARISON_RESULT);
    }

    /**
     * facet ifthenelse_result_hoisting (arm A2): the expected type for one
     * logical operand — {@code ComparisonResult} when the operand is a
     * conditional (see the call-site comment), the context's expectation
     * otherwise (today's behaviour for every other shape).
     */
    private static JavaType operandExpectedType(RExpression operand, ExpressionContext ctx,
            ExpressionCompiler compiler) {
        if (operand instanceof RConditionalExpr && compiler.getTypeUtil() != null) {
            return compiler.getTypeUtil().COMPARISON_RESULT;
        }
        return ctx.expectedType();
    }

    /**
     * Coerce a boolean-FUNCTION operand of a logical {@code and}/{@code or} to a
     * {@code ComparisonResult} (the receiver type {@code andNullSafe}/{@code orNullSafe}
     * require), matching the golden chained-predicate form. The
     * {@code or_chain_filter_predicate} facet (sibling of PR #139's
     * {@code areEqual_operand_wrap}, extended from a single comparison to a logical
     * chain).
     *
     * <p>An operand that is an {@link RSymbolReference} resolving to an {@link RFunction}
     * is a boolean function invocation that {@code ReferenceHandler} renders as either a
     * raw {@code <fn>.evaluate(<arg>)} (a no-args bare invocation via
     * {@code renderImplicitFunctionInvocation}) or a {@code MapperS.of(<fn>.evaluate(...))}
     * (an explicit-args call, wrapped via {@link JavaExpression#wrappedInMapperSOf}). Neither
     * is a {@code ComparisonResult}, so {@code <operand>.orNullSafe(...)} /
     * {@code .andNullSafe(...)} does NOT compile. Golden wraps each such operand
     * {@code ComparisonResult.ofNullSafe(MapperS.of(...))}; we add only the missing layer(s):
     * <ul>
     *   <li>bare invocation (no {@code MapperS.of} wrap — {@link JavaExpression#unwrapToBuilder}
     *       absent) → {@code ComparisonResult.ofNullSafe(MapperS.of(<rendered>))};</li>
     *   <li>explicit-args invocation (already {@code MapperS.of}-wrapped — {@code unwrapToBuilder}
     *       present) → {@code ComparisonResult.ofNullSafe(<rendered>)} (no double-wrap).</li>
     * </ul>
     *
     * <p><b>Regression-safe by construction:</b> a raw {@code Boolean} / {@code MapperS<Boolean>}
     * with {@code .orNullSafe}/{@code .andNullSafe} chained on it does not compile in ANY
     * position, so no byte-identical golden file carries the unwrapped form — the coercion only
     * ever rewrites currently-non-compiling (waivered) output. Operands that are already a
     * {@code ComparisonResult} (a nested {@code RLogicalExpr} sub-chain, an
     * {@code REqualityExpr}/{@code RComparisonExpr} comparison, an {@code RExistenceExpr})
     * are NOT {@link RFunction} references, so they pass through untouched — left-associative
     * chains wrap exactly the bare-function leaves, never the intermediate ComparisonResults.
     *
     * <p>Package-visible static since facet {@code ifthenelse_result_hoisting}:
     * {@code ControlFlowHandler}'s A2 arm renderer applies the same
     * missing-layer wrap to bare boolean-FUNCTION branch values of a hoisted
     * {@code ComparisonResult} conditional — and, since seat 19 (facet
     * {@code booleanLiteralOperandNullSafe}), to its boolean-LITERAL branch values, which it
     * previously wrapped locally: LAW 69, one helper for both halves.
     */
    static String wrapBooleanFunctionOperand(String rendered, RExpression rawOperand,
            JavaStatementBuilder operandBuilder, Set<JavaClass<?>> refs) {
        return wrapBooleanFunctionOperand(rendered, rawOperand, operandBuilder, refs, null);
    }

    /**
     * facet mapperBooleanOperandNullSafe (PR #372, F-delta-1): the compiler-aware
     * overload adds two Mapper&lt;Boolean&gt; operand shapes the shape-only arms
     * miss — a COLLAPSE over an invokable/alias call ({@code postAllocation(…)
     * first} — cdm6 MapIntent) and a boolean attribute-NAVIGATION chain
     * ({@code maturityRange -> upperBound -> inclusive} — cdm CheckMaturity ×2
     * cells; the ControlFlowHandler KNOWN-GAP class, now with logical-seat
     * carriers). Both render Mapper-typed Booleans, never the ComparisonResult
     * the and/orNullSafe receiver requires — green-safe by construction (the
     * unwrapped form compiles in NO position).
     */
    static String wrapBooleanFunctionOperand(String rendered, RExpression rawOperand,
            JavaStatementBuilder operandBuilder, Set<JavaClass<?>> refs,
            com.regnosys.rosetta.generator.java.expression.ExpressionCompiler compiler) {
        // Collapse-over-invokable: `<alias/fn/rule>(…) first|last|only-element` — the
        // callee returns a Mapper, the collapse keeps the Mapper kind; the rendered
        // form is already Mapper-wrapped (the `.first()` chain), so only the
        // ComparisonResult layer is missing.
        if (rawOperand instanceof RListOpExpr collapseOp
                && (collapseOp.op() == ListOp.FIRST || collapseOp.op() == ListOp.LAST
                        || collapseOp.op() == ListOp.ONLY_ELEMENT)
                && collapseOp.argument() instanceof RSymbolReference collapseCallee
                && collapseCallee.symbol().filter(s -> s instanceof RShortcut
                        || s instanceof RFunction || s instanceof RRule).isPresent()) {
            refs.add(HandlerHelper.COMPARISON_RESULT);
            return "ComparisonResult.ofNullSafe(" + rendered + ")";
        }
        // Boolean attribute-navigation chain: the leaf attr resolves BOOLEAN via the
        // leaf walk (resolvedFeature, else the SAME fallbackResolveFeature backup
        // numericOperandKind uses — the parser leaves these condition-helper navs
        // unresolved and the workspace inference is MISSING there, the P372D probe).
        if (rawOperand instanceof RFeatureCall fcOp
                && !HandlerHelper.isComparisonResultExpr(rawOperand)) {
            com.regnosys.rosetta.ast.supporting.RAttribute leafOp =
                    fcOp.resolvedFeature().orElse(null);
            if (leafOp == null && compiler != null) {
                leafOp = NavigationHandler.fallbackResolveFeature(fcOp, compiler);
            }
            if (leafOp != null && leafOp.typeCall() != null
                    && "boolean".equals(leafOp.typeCall().typeName())) {
                refs.add(HandlerHelper.COMPARISON_RESULT);
                return "ComparisonResult.ofNullSafe(" + rendered + ")";
            }
        }
        // facet boolNavLogicalLift (W42 finding #10, PR #426): the DISGUISED 2-name
        // chain sibling of the attribute-nav arm above — `foo -> attrBoolean` parses
        // as REnumValueRef(enumName=foo, valueName=attrBoolean) with an EMPTY
        // enumeration() (the #288 lineage), so the RFeatureCall arm never sees the
        // one-hop boolean nav (the #372 carrier was a 3-name chain, which DOES top
        // out as an RFeatureCall). Resolve the leaf through the SAME shared disguise
        // resolution the sibling handlers use (NavigationHandler.resolveDisguisedFeature,
        // the #342 compiler-carrying walk); a boolean leaf lifts
        // `ComparisonResult.ofNullSafe(<rendered>)` exactly like the sibling arms
        // (golden expr-bool-nav-logical ×3 — the operand renders
        // `MapperS.of(foo).<Boolean>map(…)`, a Mapper the and/orNullSafe
        // receiver/argument never accepts, so the unlifted form compiles in NO
        // position — green-safe by construction; upstream lifts via
        // TypeCoercionService's Mapper→ComparisonResult under the binaryExpr
        // COMPARISON_RESULT operand expectation). Like the RFeatureCall sibling
        // arm above — and unlike the date/time operand gates — there is
        // deliberately NO meta gate here: a meta-wrapped boolean leaf's operand
        // is equally non-compiling unlifted in every position, so the lift is
        // green-safe either way (Seat-1 #426 OBS-3, the documented asymmetry).
        if (rawOperand instanceof REnumValueRef disguisedNav
                && disguisedNav.enumeration().isEmpty()
                && !HandlerHelper.isComparisonResultExpr(rawOperand)
                && compiler != null) {
            com.regnosys.rosetta.ast.supporting.RAttribute disguisedLeaf =
                    NavigationHandler.resolveDisguisedFeature(disguisedNav, compiler, null);
            if (disguisedLeaf != null && disguisedLeaf.typeCall() != null
                    && "boolean".equals(disguisedLeaf.typeCall().typeName())) {
                refs.add(HandlerHelper.COMPARISON_RESULT);
                return "ComparisonResult.ofNullSafe(" + rendered + ")";
            }
        }
        // Coverage wave D (datarule): the bare-NAME sibling of the attribute-nav arm
        // above — a type-condition operand the linker bound to a BOOLEAN-typed
        // declaring-type attribute renders as the synthesized instance nav
        // (`MapperS.of(<instance>).<Boolean>map(…)`), a Mapper the and/orNullSafe
        // receiver never accepts (golden drr ESMAEMIRTransactionReportEMIR_VR_1011_01
        // `ComparisonResult.ofNullSafe(MapperS.of(eSMAEMIRTransactionReport)
        // .<Boolean>map("getCounterparty2IdentifierType", …)).andNullSafe(…)`).
        // TYPE-CONDITION-gated; green-safe by construction (the unwrapped form
        // compiles in no position).
        if (rawOperand instanceof RSymbolReference condBoolRef
                && condBoolRef.args().isEmpty()
                && !HandlerHelper.isComparisonResultExpr(rawOperand)
                && HandlerHelper.findEnclosingTypeCondition(rawOperand) != null) {
            com.regnosys.rosetta.ast.supporting.RAttribute boundAttr = condBoolRef.symbol()
                    .filter(com.regnosys.rosetta.ast.supporting.RAttribute.class::isInstance)
                    .map(com.regnosys.rosetta.ast.supporting.RAttribute.class::cast)
                    .orElse(null);
            if (boundAttr != null && boundAttr.typeCall() != null
                    && "boolean".equals(boundAttr.typeCall().typeName())) {
                refs.add(HandlerHelper.COMPARISON_RESULT);
                return "ComparisonResult.ofNullSafe(" + rendered + ")";
            }
        }
        return wrapBooleanFunctionOperandShapeArms(rendered, rawOperand, operandBuilder, refs);
    }

    private static String wrapBooleanFunctionOperandShapeArms(String rendered,
            RExpression rawOperand, JavaStatementBuilder operandBuilder, Set<JavaClass<?>> refs) {
        // facet booleanLiteralOperandNullSafe (PR #591, seat 19): a boolean LITERAL operand of
        // and/or — `True and True` (drr 5.61.0 CFTC Part 43/45 `condition …: True and True` ×13,
        // the WHOLE operand-position population of the corpus: 4,321 goldens carry
        // `ofNullSafe(MapperS.of(true|false))`, exactly 13 of them in operand position) — renders
        // `MapperS.of(true)` (LiteralHandler, wrappedInMapperSOf), a MapperS<Boolean>, never the
        // ComparisonResult the and/orNullSafe RECEIVER and ARGUMENT both require (rune-runtime
        // ComparisonResult.java:97/150 take ONLY ComparisonResult). LAW 74, javac against
        // rune-runtime 9.83.0: the receiver side is `cannot find symbol: method
        // andNullSafe(MapperS<Boolean>)`, the argument side `MapperS<T> cannot conform to
        // ComparisonResult` — so the un-coerced form compiles in NO position and no byte-identical
        // golden carries it (green-safe by construction). Golden wraps
        // `ComparisonResult.ofNullSafe(MapperS.of(true))` on BOTH sides: upstream compiles each
        // and/or operand under the COMPARISON_RESULT expectation (ExpressionGenerator.xtend
        // binaryExpr, `javaCode(left, context.withExpected(COMPARISON_RESULT))`) and the
        // TypeCoercionService item→ComparisonResult conversion writes exactly this wrap. NO
        // context gate here either — a FUNCTION-seat literal operand wraps too. The literal
        // renders Mapper-wrapped by its own handler, so only the ComparisonResult layer is added.
        // LAW 69: ControlFlowHandler's hoist-arm renderer had carried this exact arm LOCALLY for
        // its literal CONDITIONAL ARMS (the A2 `ifThenElseResult` hoist) and then delegated every
        // other arm here; since this seat it delegates the literal too, so the two halves CONSULT
        // one helper — byte-neutral for that half (the same string; the ring is the receipt).
        // The IR route reaches this SAME arm by construction, not by luck: IRExpressionCompiler
        // .visitLogical declines to legacy for operands that do not render as a ComparisonResult
        // (bare boolean literals among them — its own javadoc) and IRJavaLeafEmitter.emitLogical
        // composes no coercion, so both routes' logical seats are this wrapper.
        if (rawOperand instanceof RBooleanLiteral) {
            refs.add(HandlerHelper.COMPARISON_RESULT);
            return "ComparisonResult.ofNullSafe(" + rendered + ")";
        }
        // facet inline_then_hoist (PR #219): a `then`-chain operand whose output is a
        // ComparisonResult (`… then exists` / `… then all = True`) is hoisted by
        // CollectionHandler.tryDeepThenHoist to `exists(thenArg).asMapper()` (a Mapper) — an
        // and/orNullSafe operand needs the ComparisonResult.ofNullSafe coercion the receiver
        // method requires. Green-safe: the inline `arg.then(item -> ...)` operand never
        // compiled, so no byte-identical golden carries the un-coerced form.
        if (rawOperand instanceof RThenExpr thenOp
                && thenOp.body().map(b -> HandlerHelper.isComparisonResultExpr(b.body()))
                        .orElse(false)) {
            refs.add(HandlerHelper.COMPARISON_RESULT);
            return "ComparisonResult.ofNullSafe(" + rendered + ")";
        }
        // facet onlyElementMapperSRoundTrip (PR #345, W5): the EXTRACT sibling of the
        // then-chain arm above — an `extract` operand whose lambda body is a
        // ComparisonResult (`… only-element extract (… only exists)`, Qualify_Repurchase
        // cdm5+cdm6) compiles to `<recv>.mapSingleToItem(item -> onlyExists(…).asMapper())`
        // — a MapperS<Boolean>, NOT the ComparisonResult and/orNullSafe requires
        // (rune-runtime ComparisonResult.java:97/150 take ONLY ComparisonResult), so the
        // un-coerced operand never compiled — green-safe by construction. Golden wraps
        // `ComparisonResult.ofNullSafe(<rendered>)` (151 golden andNullSafe(ofNullSafe(
        // occurrences — the established coercion at this seat).
        if (rawOperand instanceof RExtractExpr extractOp
                && extractOp.body() != null
                && HandlerHelper.isComparisonResultExpr(extractOp.body().body())) {
            refs.add(HandlerHelper.COMPARISON_RESULT);
            return "ComparisonResult.ofNullSafe(" + rendered + ")";
        }
        // facet ofNullSafe (PR #220): an ALIAS (RShortcut) operand of and/orNullSafe is a
        // MapperS<Boolean> alias-method call (e.g. `unitMatches(p1, p2)`), NOT a
        // ComparisonResult, so `<alias(...)>.andNullSafe(...)` does not compile. Golden wraps
        // it ComparisonResult.ofNullSafe(<alias(...)>) WITHOUT a MapperS.of — the alias method
        // already returns MapperS<Boolean> (so it is NOT the MapperS.of-wrap RFunction path
        // below, which would double-wrap). The RShortcut-as-boolean-operand only ever occurs
        // in this and/orNullSafe position, so it is boolean by construction. Green-safe: the
        // un-coerced `MapperS<Boolean>.andNullSafe(...)` never compiled, so no golden carries it.
        if (rawOperand instanceof RSymbolReference aliasRef
                && aliasRef.symbol().filter(RShortcut.class::isInstance).isPresent()) {
            refs.add(HandlerHelper.COMPARISON_RESULT);
            return "ComparisonResult.ofNullSafe(" + rendered + ")";
        }
        // facet logicalParamCompResultLift (PR #420): a bare argless reference the
        // linker bound to a BOOLEAN-typed RAttribute (a function INPUT param —
        // `a or b and c` over `a boolean (1..1)` inputs) renders Mapper-wrapped
        // (`MapperS.of(a)` — the oracle-witnessed shape; a MULTI boolean param's
        // MapperC form takes the same wrap by the compile argument, UNWITNESSED —
        // Seat-1 #420 OBS-3) — a Mapper,
        // never the ComparisonResult the and/orNullSafe receiver requires, so the
        // unwrapped form compiles in NO position (green-safe by construction; the
        // corpus census: zero goldens carry `ofNullSafe(MapperS.of(<param>))` —
        // the shape is corpus-absent). Golden lifts each such operand
        // `ComparisonResult.ofNullSafe(<rendered>)` (oracle golden expr-logical:
        // `ComparisonResult.ofNullSafe(MapperS.of(a)).orNullSafe(...)`), upstream's
        // TypeCoercionService Mapper→ComparisonResult under the binaryExpr
        // COMPARISON_RESULT expectation. The TYPE-CONDITION sibling arm (the
        // compiler-aware overload) fires first for datarule seats and emits the
        // IDENTICAL wrap, so entries through either overload agree. A
        // boolean-typed ALIAS left (`typeAlias MyBool: boolean`) declines — the
        // literal typeName test mirrors the sibling arms (no alias walk here;
        // zero witnesses).
        if (rawOperand instanceof RSymbolReference paramRef
                && paramRef.args().isEmpty()
                && !HandlerHelper.isComparisonResultExpr(rawOperand)) {
            com.regnosys.rosetta.ast.supporting.RAttribute boolParam = paramRef.symbol()
                    .filter(com.regnosys.rosetta.ast.supporting.RAttribute.class::isInstance)
                    .map(com.regnosys.rosetta.ast.supporting.RAttribute.class::cast)
                    .orElse(null);
            if (boolParam != null && boolParam.typeCall() != null
                    && "boolean".equals(boolParam.typeCall().typeName())) {
                refs.add(HandlerHelper.COMPARISON_RESULT);
                return "ComparisonResult.ofNullSafe(" + rendered + ")";
            }
        }
        // facet defaultOperandNullSafe (PR #332): a `default` operand of and/orNullSafe
        // (`<chain> default True` — drr GetOrFetchLeiData/MicData `micValidation
        // default True`, DIRECT or as a then-chain body `… then distinct only-element
        // default True`) renders `MapperS.of(<left>.getOrDefault(<lit>))` (the #218
        // scalar-literal form) — a MapperS<Boolean>, NOT the ComparisonResult the
        // receiver methods require (ComparisonResult.andNullSafe/orNullSafe take ONLY
        // ComparisonResult — rune-runtime ComparisonResult.java:97/150 — so neither a
        // Mapper argument nor a Mapper receiver compiles; no green file carries the
        // un-coerced form). Golden wraps ComparisonResult.ofNullSafe(<rendered>)
        // (upstream TypeCoercionService Mapper→ComparisonResult, xtend:299). The
        // already-Mapper-wrapped test admits the #218 scalar-literal form BY ARM
        // SEMANTICS (it is built via JavaExpression.from with NO unwrap contract, so
        // unwrapToBuilder alone would misread it as bare and double-wrap — the CP4
        // first-cut bug); a default whose render is genuinely bare takes the item→
        // ComparisonResult form with the MapperS.of layer added (xtend:595).
        com.regnosys.rosetta.ast.RExpression defaultNode = rawOperand;
        if (rawOperand instanceof RThenExpr thenChain) {
            defaultNode = thenChain.body().map(b -> b.body()).orElse(null);
        }
        if (defaultNode instanceof com.regnosys.rosetta.ast.expressions.binary.RDefaultExpr defaultOp) {
            refs.add(HandlerHelper.COMPARISON_RESULT);
            boolean mapperWrapped = (operandBuilder instanceof JavaExpression je
                    && je.unwrapToBuilder().isPresent())
                    || HandlerHelper.isScalarLiteral(defaultOp.rawRight());
            if (mapperWrapped) {
                return "ComparisonResult.ofNullSafe(" + rendered + ")";
            }
            refs.add(HandlerHelper.MAPPER_S);
            return "ComparisonResult.ofNullSafe(MapperS.of(" + rendered + "))";
        }
        // facet ruleOperandNullSafe (PR #308): a bare REPORTING-RULE (RRule) operand of
        // and/orNullSafe (e.g. the csa BasketConstituents filter `IsAllowableActionForCSA
        // and CustomBasketIndicator`, where CustomBasketIndicator → customBasketIndicatorRule)
        // renders MapperS.of(<rule>.evaluate(...)) via renderImplicitRuleInvocation — a
        // MapperS<Boolean>, NOT a ComparisonResult — so `<operand>.andNullSafe(...)` does not
        // compile; golden wraps it ComparisonResult.ofNullSafe(MapperS.of(...)). The rule
        // invocation is always MapperS.of-wrapped (renderImplicitRuleInvocation), so it takes
        // the alreadyMapperWrapped path below (ComparisonResult.ofNullSafe(<rendered>), no
        // double MapperS.of). The RRule-as-boolean-operand only ever occurs in this
        // and/orNullSafe position (boolean by construction). Green-safe: the un-coerced
        // MapperS<Boolean>.andNullSafe(...) never compiled, so no green file carries the form.
        boolean booleanFunctionOperand = rawOperand instanceof RSymbolReference ref
                && ref.symbol().filter(s -> s instanceof RFunction || s instanceof RRule).isPresent();
        if (!booleanFunctionOperand) {
            return rendered;
        }
        refs.add(HandlerHelper.COMPARISON_RESULT);
        boolean alreadyMapperWrapped = operandBuilder instanceof JavaExpression je
                && je.unwrapToBuilder().isPresent();
        if (alreadyMapperWrapped) {
            return "ComparisonResult.ofNullSafe(" + rendered + ")";
        }
        refs.add(HandlerHelper.MAPPER_S);
        return "ComparisonResult.ofNullSafe(MapperS.of(" + rendered + "))";
    }

}
