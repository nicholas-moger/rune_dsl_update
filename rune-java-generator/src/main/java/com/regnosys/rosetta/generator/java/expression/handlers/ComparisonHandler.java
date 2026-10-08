package com.regnosys.rosetta.generator.java.expression.handlers;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.enums.CardMod;
import com.regnosys.rosetta.ast.expressions.binary.RComparisonExpr;
import com.regnosys.rosetta.ast.expressions.binary.REqualityExpr;
import com.regnosys.rosetta.ast.expressions.literals.RIntLiteral;
import com.regnosys.rosetta.ast.expressions.literals.RNumberLiteral;
import com.regnosys.rosetta.ast.expressions.references.REmptyLiteral;
import com.regnosys.rosetta.ast.expressions.references.REnumValueRef;
import com.regnosys.rosetta.ast.expressions.references.RSymbolReference;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.supporting.REnumValue;
import com.regnosys.rosetta.ast.types.REnumeration;
import com.regnosys.rosetta.generator.GeneratedIdentifier;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.JavaNamingUtil;
import com.regnosys.rosetta.generator.java.enums.EnumHelper;
import com.regnosys.rosetta.generator.java.expression.ExpressionCompiler;
import com.regnosys.rosetta.generator.java.expression.ExpressionContext;
import com.regnosys.rosetta.generator.java.object.MetaFieldGenerator;
import com.regnosys.rosetta.generator.java.statement.builder.JavaExpression;
import com.regnosys.rosetta.generator.java.statement.builder.JavaStatementBuilder;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.generator.java.types.RJavaWithMetaValue;
import com.regnosys.rosetta.types.REnumTypeRef;
import com.regnosys.rosetta.types.RMetaAnnotatedType;
import com.rosetta.util.types.JavaClass;
import com.rosetta.util.types.JavaType;

import java.util.HashSet;
import java.util.Set;

/**
 * Handles code generation for equality and comparison binary expressions.
 *
 * <p>Golden output patterns (verified against CDM golden files):
 * <pre>
 *   a = b      →  areEqual(left, right, CardinalityOperator.All)
 *   a <> b     →  notEqual(left, right, CardinalityOperator.Any)
 *   a < b      →  lessThan(left, right, CardinalityOperator.All)
 *   a > b      →  greaterThan(left, right, CardinalityOperator.All)
 *   a <= b     →  lessThanEquals(left, right, CardinalityOperator.All)
 *   a >= b     →  greaterThanEquals(left, right, CardinalityOperator.All)
 * </pre>
 *
 * <p>The {@code CardinalityOperator} default is <b>operator-dependent</b>, mirroring
 * upstream {@code ExpressionGenerator} ({@code defaultModifier = operator == "<>"
 * ? ANY : ALL}): {@code <>} (not-equals) defaults to {@code Any}, every other
 * comparison defaults to {@code All}. An explicit source cardinality modifier
 * ({@code all}/{@code any}) overrides the default.
 *
 * <p>These are static method calls from
 * {@code com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe},
 * consumed via {@code import static ExpressionOperatorsNullSafe.*;}.
 *
 * <p><b>Type propagation:</b> When one operand is a typed expression (e.g., a
 * {@code number}-typed parameter) and the other is a literal, the typed operand's
 * Rune DSL type is propagated as the expected type for the literal operand. This
 * ensures correct coercion, e.g., {@code arg < 0} where {@code arg: number}
 * renders as {@code lessThan(MapperS.of(arg), MapperS.of(BigDecimal.valueOf(0)), ...)}.
 */
public class ComparisonHandler {

    /**
     * Compiles an {@link REqualityExpr} (= or <>) into an ExpressionOperatorsNullSafe call.
     *
     * @param expr     the equality expression node
     * @param ctx      the current expression compilation context
     * @param compiler the parent compiler for recursive compilation of operands
     * @return a {@link JavaExpression} rendering the areEqual/notEqual call
     */
    public JavaStatementBuilder handle(REqualityExpr expr, ExpressionContext ctx, ExpressionCompiler compiler) {
        // PR-A §9.1 C3a.4.b: CARDINALITY_OPERATOR emitted as structured ref;
        // EXPRESSION_OPERATORS_NULL_SAFE emitted as structured staticWildcardImport.
        // Operand refs (e.g., MAPPER_S from literal wraps) are unioned in via
        // leftBuilder.getRefs() / rightBuilder.getRefs() so the returned
        // builder carries the complete ref set visible in its source text —
        // required once C3c.2 makes the structured channel the sole feeder.
        String method = switch (expr.op()) {
            case EQ  -> "areEqual";
            case NEQ -> "notEqual";
        };
        // Operator-dependent default cardinality (mirrors upstream
        // ExpressionGenerator: <> defaults to ANY, = defaults to ALL).
        String defaultCard = switch (expr.op()) {
            case EQ  -> "All";
            case NEQ -> "Any";
        };

        JavaType numericType = inferNumericType(expr.rawLeft(), expr.rawRight(), compiler);
        JavaType leftExpected = numericType != null ? numericType : ctx.expectedType();
        JavaType rightExpected = numericType != null ? numericType : ctx.expectedType();
        // facet numeric_literal_typing (mechanism 1) — see literalSiblingNumericType:
        // when both arms above missed, an int-literal operand whose SIBLING resolves
        // numerically through the typed-model walk (alias body / resolution-blind nav
        // leaf) takes the sibling's type as ITS OWN expected type — asymmetric, the
        // sibling keeps ctx.expectedType(), bounding the byte-delta to the literal.
        if (numericType == null) {
            JavaType literalExpected =
                    literalSiblingNumericType(expr.rawLeft(), expr.rawRight(), compiler, ctx);
            if (literalExpected != null) {
                if (expr.rawLeft() instanceof RIntLiteral) {
                    leftExpected = literalExpected;
                } else {
                    rightExpected = literalExpected;
                }
            }
        }

        // facet implicit_operand_synthesis (PR #218): a `then`-body bare comparison
        // (`… then all = True`) elides its LHS — the piped value — so rawLeft() is
        // null; substitute the synthetic implicit (→ the bound thenArg / lambda item).
        JavaStatementBuilder leftBuilder = compileComparisonOperand(
                HandlerHelper.orSyntheticImplicit(expr.rawLeft(), expr), leftExpected,
                numericType != null, ctx, compiler);
        JavaStatementBuilder rightBuilder = compileComparisonOperand(
                HandlerHelper.orSyntheticImplicit(expr.rawRight(), expr), rightExpected,
                numericType != null, ctx, compiler);
        // v3.2 seat 2 (Law 2): a primitive-typed operand (the count's `int`) lifts to the Mapper
        // upstream expects — by TYPE, whatever node produced it (HandlerHelper.liftPrimitiveOperand).
        leftBuilder = HandlerHelper.liftPrimitiveOperand(leftBuilder, compiler, ctx.scope());
        rightBuilder = HandlerHelper.liftPrimitiveOperand(rightBuilder, compiler, ctx.scope());
        // facet filter_predicate_item_typing (comparison-operand meta-strip):
        // upstream compiles every comparison operand against the meta-STRIPPED
        // join of the operand types (ExpressionGenerator's
        // `MAPPER.wrapExtendsWithoutMeta(joined)`), so an operand whose chain ends
        // on a `[metadata]` attribute carries the Type-coercion deref
        // (`.<String>map("Type coercion", fieldWithMetaString ->
        // fieldWithMetaString == null ? null : fieldWithMetaString.getValue())`).
        // coerceNavigationReceiver is the fork's single-purpose meta-strip lever
        // (the same one navigation receivers use): it dispatches the byte-validated
        // WrappedItemCoercer arms (null-guarded MapperS / bare MapperC) when the
        // operand's item type is an RJavaWithMetaValue and is a NO-OP otherwise —
        // including every null-typed operand — so non-meta comparisons are
        // byte-unchanged. Regression-safe: no in-scope green golden carries a
        // meta-typed comparison operand without the coercion (corpus-verified;
        // upstream always strips).
        //
        // facet aliasOperandMetaCoerce (PR #326, F4b): a bare ALIAS-call operand renders
        // NULL-typed (ReferenceHandler's alias-invocation JavaExpression carries no type),
        // so the meta-strip below could not see a META alias element and the fork left the
        // wrapper bare (Qualify_Transaction_OIS: `areEqual(floatingRateIndex(economicTerms),
        // MapperS.of(FloatingRateIndexEnum.…))` vs golden's `.…<FloatingRateIndexEnum>map(
        // "Type coercion", fieldWithMetaFloatingRateIndexEnum -> ….getValue())` deref on
        // every operand). Retype the null-typed operand from the SAME FunctionAliasHelper
        // walk that renders the alias method signature into the same file
        // (NavigationHandler.tryAliasReceiverMapperType — non-null for a META alias, the
        // #178 gate, and since the #334 widening for basic-scalar aliases too — the basic
        // retype is byte-inert here, coerceNavigationReceiver's own meta gate no-ops it),
        // so the EXISTING lever fires with the byte-validated
        // WrappedItemCoercer arms (bare un-registered param iff MapperC — the #170 law).
        // Green-safe: upstream strips every meta comparison operand, so the fork's bare
        // wrapper form never byte-matched golden (already-waivered carriers only); a
        // non-meta / unresolvable alias keeps the null type (no-op).
        leftBuilder  = retypeNullTypedAliasOperand(leftBuilder, expr.rawLeft(), compiler);
        rightBuilder = retypeNullTypedAliasOperand(rightBuilder, expr.rawRight(), compiler);
        // facet deepThenLevelElementPreserve (PR #362): the bare-item sibling of the #326
        // alias retype — see retypeNullTypedBareItemOperand.
        leftBuilder  = retypeNullTypedBareItemOperand(leftBuilder,
                HandlerHelper.orSyntheticImplicit(expr.rawLeft(), expr), ctx, compiler);
        rightBuilder = retypeNullTypedBareItemOperand(rightBuilder,
                HandlerHelper.orSyntheticImplicit(expr.rawRight(), expr), ctx, compiler);
        leftBuilder  = compiler.coerceNavigationReceiver(leftBuilder, ctx.scope());
        rightBuilder = compiler.coerceNavigationReceiver(rightBuilder, ctx.scope());
        // (v3.2 seat 2, PR #623's witness sweep: the typed numeric widening of the inequality seat
        // below is NOT mirrored here - no carrier reaches an Integer-alias-vs-BigDecimal EQUALITY,
        // lane R stayed green with the mirror off, so it was withdrawn under the #614 law; banked
        // until a carrier appears.)
        String left  = HandlerHelper.render(leftBuilder);
        String right = HandlerHelper.render(rightBuilder);
        String cardOp = cardinalityOperator(expr.mod().orElse(null), defaultCard);

        Set<JavaClass<?>> refs = new HashSet<>();
        refs.add(HandlerHelper.CARDINALITY_OPERATOR);
        refs.addAll(leftBuilder.getRefs());
        refs.addAll(rightBuilder.getRefs());

        // bare_enum_comparison_operand facet: a BARE enum value used as a comparison
        // operand (`<nav> = Clearing`) is an UNRESOLVED RSymbolReference (enum values
        // are not in function scope; the parser type-directs it to its REnumValue only
        // when a sibling already types as the enum — but the sibling nav's enum type is
        // MISSING via the resolvedFeature IR gap), so it renders as the non-compiling
        // bare variable `MapperS.of(Clearing)`. Resolve the enum from the SIBLING operand
        // and emit the qualified `EnumName.CONSTANT`; the existing wrapEnumOperand below
        // then applies the MapperS.of wrap, exactly as for a source-qualified operand.
        // facet enumConstantWitness (PR #611): the operand whose witness the wrap below reads — the
        // compiled builder, or the re-qualification arm's own enum-constant witness when it fires.
        JavaStatementBuilder leftOperand = leftBuilder;
        JavaExpression bareLeft = tryBareEnumComparand(expr.rawLeft(), expr.rawRight(), compiler, refs);
        if (bareLeft != null) {
            leftOperand = bareLeft;
        } else {
            // facet enumQualifyInheritedComparand (PR #215): a RESOLVED enum operand whose declaring
            // enum is a SUPER of the sibling's child enum re-qualifies to the child (the comparison
            // type). Mutually exclusive with the bare arm above (which requires an UNRESOLVED operand).
            JavaExpression reqLeft = tryInheritedEnumRequalify(expr.rawLeft(), expr.rawRight(), compiler, refs);
            if (reqLeft != null) {
                leftOperand = reqLeft;
            }
        }
        JavaStatementBuilder rightOperand = rightBuilder;
        JavaExpression bareRight = tryBareEnumComparand(expr.rawRight(), expr.rawLeft(), compiler, refs);
        if (bareRight != null) {
            rightOperand = bareRight;
        } else {
            JavaExpression reqRight = tryInheritedEnumRequalify(expr.rawRight(), expr.rawLeft(), compiler, refs);
            if (reqRight != null) {
                rightOperand = reqRight;
            }
        }

        // Phase X1 Gap #3: a bare enum-constant operand must be wrapped in
        // MapperS.of(...) so the comparison sees a Mapper operand.
        left  = wrapEnumOperand(leftOperand, refs);
        right = wrapEnumOperand(rightOperand, refs);

        // areEqual_operand_wrap facet: a bare implicit-FUNCTION-invocation operand
        // renders UNWRAPPED; a comparison needs a Mapper operand — wrap it.
        left  = wrapBareFunctionOperand(left, expr.rawLeft(), refs, compiler);
        right = wrapBareFunctionOperand(right, expr.rawRight(), refs, compiler);

        // (v3.2 seat 2: the count_result_operand_wrap node-keyed read - gated on the RAW operand being an
        // RCountExpr - stood here; retired onto the typed lift at the operand compile,
        // HandlerHelper.liftPrimitiveOperand.)

        // facet filterPredArmComparandCollapseHoist (PR #394): inside the widened
        // #365 elseless filter-predicate arm, a comparand that is a `.get()`-collapsed
        // REFERENCE_WITH_META chain hoists `final <Wrapper> <n> = <collapse>;`
        // in-branch (registerPendingLambdaHoist — the elseless block drains it ahead
        // of the arm's return) and compares the guarded reconstruct
        // `(<n> == null ? MapperS.<Value>ofNull() : MapperS.of(<n>.getValue()))` —
        // the maxMinCollapsedMetaKeyDerefOrNull pattern at the comparison-operand
        // seat, parenthesized (operand position). Runs BEFORE the selfUnwrapping
        // re-wrap below so the collapse hoists instead of wrapping (the replaced
        // text no longer ends `.get()`, so the #243 wrap never double-fires).
        String hoistLeft = tryFilterPredArmComparandCollapseHoist(left, leftBuilder,
                expr.rawLeft(), ctx, compiler, refs);
        if (hoistLeft != null) {
            left = hoistLeft;
        }
        String hoistRight = tryFilterPredArmComparandCollapseHoist(right, rightBuilder,
                expr.rawRight(), ctx, compiler, refs);
        if (hoistRight != null) {
            right = hoistRight;
        }

        // navGetWrap facet (PR #243): an operand that rendered as a transparent
        // item-collapse (a selfUnwrapping JavaExpression whose text ends in `.get()`)
        // must be re-wrapped MapperS.of(...) so the comparison sees a Mapper operand.
        left  = HandlerHelper.wrapSelfUnwrappingGetOperand(left, leftBuilder, refs);
        right = HandlerHelper.wrapSelfUnwrappingGetOperand(right, rightBuilder, refs);

        // facet mapItemParamBareOperand (PR #367): an explicit extract/mapItem lambda
        // param is ALREADY the MapperS item (upstream binds the mapItem lambda var as
        // MapperS<T>), so an equality operand that is the BARE param re-wrapped
        // `MapperS.of(<name>)` unwraps back to the bare name — golden drr
        // ExtractProductIdentifierBySource `areEqual(item.<ProductIdTypeEnum>map(…),
        // src, CardinalityOperator.All)`. Render-equals guarded (only the exact
        // `MapperS.of(<name>)` text unwraps — a chained/converted operand never
        // matches) + THEN-owners decline (a then param binds the WHOLE piped list —
        // the #175/#180 closure-param law). Green-safe by the never-golden shape:
        // upstream's operand compile sees the param's Mapper type and passes it
        // through bare, so no green file wraps a Mapper-typed closure param at an
        // equality operand (the double-wrap compiles — areEqual is unconstrained
        // across operand types — but golden never carries it).
        left  = unwrapClosureParamOperand(left, expr.rawLeft());
        right = unwrapClosureParamOperand(right, expr.rawRight());

        Set<JavaClass<?>> staticWildcards = new HashSet<>();
        staticWildcards.add(HandlerHelper.EXPRESSION_OPERATORS_NULL_SAFE);
        staticWildcards.addAll(leftBuilder.getStaticWildcardImports());
        staticWildcards.addAll(rightBuilder.getStaticWildcardImports());

        return JavaExpression.from(
                method + "(" + left + ", " + right + ", " + cardOp + ")",
                null,
                refs,
                staticWildcards);
    }

    /**
     * facet mapItemParamBareOperand (PR #367): unwrap the {@code MapperS.of(<name>)}
     * re-wrap of an explicit NON-then closure param at an equality operand seat —
     * see the call-site comment for the law. Returns {@code rendered} untouched
     * unless the raw operand is an args-empty {@link RSymbolReference} whose name
     * matches an enclosing lambda's declared param (the
     * {@code ReferenceHandler.enclosingClosureParamOwner} walk — the #168/#180
     * owner variant) with a non-{@code RThenExpr} owner AND the rendered text is
     * exactly the bare-name wrap.
     */
    private static String unwrapClosureParamOperand(String rendered, RExpression rawOperand) {
        if (!(rawOperand instanceof RSymbolReference symRef) || !symRef.args().isEmpty()
                || symRef.name() == null
                || !rendered.equals("MapperS.of(" + symRef.name() + ")")) {
            return rendered;
        }
        var owner = ReferenceHandler.enclosingClosureParamOwner(symRef, symRef.name());
        if (owner == null
                || owner.parent() instanceof com.regnosys.rosetta.ast.expressions.binary.RThenExpr) {
            return rendered;
        }
        return symRef.name();
    }

    /**
     * Compiles an {@link RComparisonExpr} (&lt;, &gt;, &lt;=, &gt;=) into an
     * ExpressionOperatorsNullSafe call.
     *
     * @param expr     the comparison expression node
     * @param ctx      the current expression compilation context
     * @param compiler the parent compiler for recursive compilation of operands
     * @return a {@link JavaExpression} rendering the lessThan/greaterThan/etc. call
     */
    public JavaStatementBuilder handle(RComparisonExpr expr, ExpressionContext ctx, ExpressionCompiler compiler) {
        // PR-A §9.1 C3a.4.b: same pattern as handle(REqualityExpr) —
        // CARDINALITY_OPERATOR + EXPRESSION_OPERATORS_NULL_SAFE wildcard +
        // operand-refs union.
        String method = switch (expr.op()) {
            case LT  -> "lessThan";
            case GT  -> "greaterThan";
            case LTE -> "lessThanEquals";
            case GTE -> "greaterThanEquals";
        };

        JavaType numericType = inferNumericType(expr.rawLeft(), expr.rawRight(), compiler);
        JavaType leftExpected = numericType != null ? numericType : ctx.expectedType();
        JavaType rightExpected = numericType != null ? numericType : ctx.expectedType();
        // facet numeric_literal_typing (mechanism 1) — see handle(REqualityExpr);
        // identical asymmetric literal-only arm for the inequality operators
        // (RateOfReturn's `initialPriceValue > 0` is this path).
        if (numericType == null) {
            JavaType literalExpected =
                    literalSiblingNumericType(expr.rawLeft(), expr.rawRight(), compiler, ctx);
            if (literalExpected != null) {
                if (expr.rawLeft() instanceof RIntLiteral) {
                    leftExpected = literalExpected;
                } else {
                    rightExpected = literalExpected;
                }
            }
        }

        // facet implicit_operand_synthesis (PR #218): a `then`-body bare comparison
        // (`… then all = True`) elides its LHS — the piped value — so rawLeft() is
        // null; substitute the synthetic implicit (→ the bound thenArg / lambda item).
        JavaStatementBuilder leftBuilder = compileComparisonOperand(
                HandlerHelper.orSyntheticImplicit(expr.rawLeft(), expr), leftExpected,
                numericType != null, ctx, compiler);
        JavaStatementBuilder rightBuilder = compileComparisonOperand(
                HandlerHelper.orSyntheticImplicit(expr.rawRight(), expr), rightExpected,
                numericType != null, ctx, compiler);
        // v3.2 seat 2 (Law 2): the same typed lift as the equality twin above.
        leftBuilder = HandlerHelper.liftPrimitiveOperand(leftBuilder, compiler, ctx.scope());
        rightBuilder = HandlerHelper.liftPrimitiveOperand(rightBuilder, compiler, ctx.scope());
        // Comparison-operand meta-strip — see handle(REqualityExpr); identical
        // rationale and no-op guarantee for the inequality operators. The alias-operand
        // meta retype (facet aliasOperandMetaCoerce, PR #326 F4b) mirrors the equality
        // seat: a null-typed META-alias operand gains its signature type so the strip fires.
        leftBuilder  = retypeNullTypedAliasOperand(leftBuilder, expr.rawLeft(), compiler);
        rightBuilder = retypeNullTypedAliasOperand(rightBuilder, expr.rawRight(), compiler);
        // facet deepThenLevelElementPreserve (PR #362): the bare-item retype mirrors the
        // equality seat — see retypeNullTypedBareItemOperand.
        leftBuilder  = retypeNullTypedBareItemOperand(leftBuilder,
                HandlerHelper.orSyntheticImplicit(expr.rawLeft(), expr), ctx, compiler);
        rightBuilder = retypeNullTypedBareItemOperand(rightBuilder,
                HandlerHelper.orSyntheticImplicit(expr.rawRight(), expr), ctx, compiler);
        leftBuilder  = compiler.coerceNavigationReceiver(leftBuilder, ctx.scope());
        rightBuilder = compiler.coerceNavigationReceiver(rightBuilder, ctx.scope());
        // facet comparisonIntWiden (PR #372, F-delta-3): the numeric-JOIN elementwise
        // widening at the INEQUALITY seat — when the join says BigDecimal and a compiled
        // operand is a Mapper whose ITEM is Integer, coerce through the service (the
        // WrappedItemCoercer hop `.<BigDecimal>map("Type coercion", integerN -> integerN
        // == null ? null : BigDecimal.valueOf(integerN))`, deferred method-wide param
        // numbering — golden cdm CheckMaturity ×4 per cell). Upstream compiles operands
        // against MAPPER.wrapExtends(joined), so a green mixed-type inequality cannot
        // exist (greaterThan's single type parameter rejects Mapper<Integer> vs
        // Mapper<BigDecimal> — non-compiling, waivered). SCOPED to the inequality
        // operators: areEqual's unbounded wildcards COMPILE mixed, so equality seats
        // could carry green un-widened forms and stay untouched.
        HandlerHelper.NumericKind leftKind =
                HandlerHelper.numericOperandKind(expr.rawLeft(), compiler);
        HandlerHelper.NumericKind rightKind =
                HandlerHelper.numericOperandKind(expr.rawRight(), compiler);
        if (leftKind == HandlerHelper.NumericKind.INT
                && rightKind == HandlerHelper.NumericKind.NUMBER) {
            leftBuilder = widenIntegerNavOperand(leftBuilder, expr.rawLeft(), ctx);
        } else if (rightKind == HandlerHelper.NumericKind.INT
                && leftKind == HandlerHelper.NumericKind.NUMBER) {
            rightBuilder = widenIntegerNavOperand(rightBuilder, expr.rawRight(), ctx);
        }
        // v3.2 seat 2 (the chaos C5Forms rows — `(fallback then sum) >= asInt`): the TYPED twin of the
        // node-keyed arm above, for operands the node read cannot see — an ALIAS-call operand re-typed
        // from its signature walk (`MapperS<Integer> asInt`) against a BigDecimal-typed hoisted chain.
        // Upstream joins the operand types and coerces the Integer side (the guarded MapperS rung);
        // HandlerHelper.widenIntegerMapperToBigDecimal is the coercion service's own render. Reads the
        // stamps only, so a navigation the arm above already widened (re-stamped null) is never touched.
        JavaTypeUtil widenTu = compiler.getTypeUtil();
        if (HandlerHelper.isBigDecimalMapper(leftBuilder, widenTu)) {
            rightBuilder = HandlerHelper.widenIntegerMapperToBigDecimal(rightBuilder, compiler, ctx.scope());
        } else if (HandlerHelper.isBigDecimalMapper(rightBuilder, widenTu)) {
            leftBuilder = HandlerHelper.widenIntegerMapperToBigDecimal(leftBuilder, compiler, ctx.scope());
        }
        String left  = HandlerHelper.render(leftBuilder);
        String right = HandlerHelper.render(rightBuilder);
        // Inequalities (<, <=, >, >=) always default to All.
        String cardOp = cardinalityOperator(expr.mod().orElse(null), "All");

        Set<JavaClass<?>> refs = new HashSet<>();
        refs.add(HandlerHelper.CARDINALITY_OPERATOR);
        refs.addAll(leftBuilder.getRefs());
        refs.addAll(rightBuilder.getRefs());

        // Phase X1 Gap #3: a bare enum-constant operand must be wrapped in
        // MapperS.of(...) so the comparison sees a Mapper operand.
        left  = wrapEnumOperand(leftBuilder, refs);
        right = wrapEnumOperand(rightBuilder, refs);

        // areEqual_operand_wrap facet: a bare implicit-FUNCTION-invocation operand
        // renders UNWRAPPED; a comparison needs a Mapper operand — wrap it.
        left  = wrapBareFunctionOperand(left, expr.rawLeft(), refs, compiler);
        right = wrapBareFunctionOperand(right, expr.rawRight(), refs, compiler);

        // (v3.2 seat 2: the count_result_operand_wrap node-keyed read - gated on the RAW operand being an
        // RCountExpr - stood here too; retired onto the typed lift at the operand compile,
        // HandlerHelper.liftPrimitiveOperand.)

        // navGetWrap facet (PR #243): see handle(REqualityExpr) — identical
        // item-collapse wrap for the inequality operators (green-safe by the
        // same corpus law: no golden leaves a `.get()` operand bare at a
        // comparison seat).
        left  = HandlerHelper.wrapSelfUnwrappingGetOperand(left, leftBuilder, refs);
        right = HandlerHelper.wrapSelfUnwrappingGetOperand(right, rightBuilder, refs);

        Set<JavaClass<?>> staticWildcards = new HashSet<>();
        staticWildcards.add(HandlerHelper.EXPRESSION_OPERATORS_NULL_SAFE);
        staticWildcards.addAll(leftBuilder.getStaticWildcardImports());
        staticWildcards.addAll(rightBuilder.getStaticWildcardImports());

        return JavaExpression.from(
                method + "(" + left + ", " + right + ", " + cardOp + ")",
                null,
                refs,
                staticWildcards);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * facet filterPredArmComparandCollapseHoist (PR #394): hoist a {@code .get()}-collapsed
     * REFERENCE_WITH_META comparand inside an ACTIVE elseless filter-predicate arm window
     * ({@link CollectionHandler#filterPredArmWindowActive}) as {@code final <Wrapper>
     * <name> = <collapse>;} (registerPendingLambdaHoist — the elseless block drains it
     * in-branch ahead of the arm's return) and return the guarded PARENTHESIZED
     * reconstruct {@code (<name> == null ? MapperS.<Value>ofNull() : MapperS.of(
     * <name>.getValue()))} — the {@code maxMinCollapsedMetaKeyDerefOrNull} pattern at
     * the comparison-operand seat (its javadoc's "REFERENCE wrappers have no corpus
     * carrier" note names exactly this missing cell; golden drr PTRRIDRule's
     * FilterPartyRole/FilterRelatedPartyByRole comparands, whose hoist locals join the
     * per-lambda {@code referenceWithMetaParty} collision group interleaved with the
     * sibling coercion params — creation order is compile order). Terminal resolution =
     * {@code NavigationHandler.terminalNavAttr} + {@code metaWrapperOf} (the #237 pair —
     * render truth). Declines ({@code null} — the #243 selfUnwrapping re-wrap keeps
     * today's bytes) outside the window, for a non-collapse operand, an
     * unresolvable / non-REFERENCE_WITH_META terminal (FIELD wrappers have no corpus
     * carrier at this seat — the maxMin note's mirror), or a non-identifier name.
     * Green-safe by construction: the window is only active inside the widened
     * elseless block's arm compiles, which no green file renders.
     */
    private static String tryFilterPredArmComparandCollapseHoist(String rendered,
            JavaStatementBuilder builder, RExpression rawOperand, ExpressionContext ctx,
            ExpressionCompiler compiler, Set<JavaClass<?>> refs) {
        // facet collapseGetSuffix (v3.1 C2d retirement family 7, PR #614): the "did the comparand
        // compile to a bare `.get()` collapse?" gate reads the family's shared arbiter (an
        // ONLY_ELEMENT-rooted operand over a null-typed compiled value) instead of the render's
        // suffix. The c7b census inside the ACTIVE window: 44 arrivals on EACH of the three
        // walks (default-route, IR-route and optimised alike), text=true at 22,
        // and `RListOpExpr root AND typeNull` selects exactly those 22 on all three walks — the
        // other 22 are RSymbolReference operands with a non-null wrapper-kinded type. The row's own
        // named channel (`isWrapper(getExpressionType())` false) is the WEAKER half of that: the
        // type is null at every fire, so the wrapper test never had an answer to give.
        //
        // DISCLOSED CENSUS GAP: the probe logged the operand's expression CLASS (RListOpExpr), not
        // its list OP, so "ONLY_ELEMENT" — which the installed read requires — is consistent with
        // every logged fact rather than directly measured (the #613 §6 precedent; the same gap
        // FunctionExpressionRenderer.wrapSingleRungInMapperCOfSingletonList discloses at its own
        // seat). Two things close it: the PTRRIDRule whole-file byte lock in
        // CollapseGetSuffixSeatTest — this row's own charter carrier, and one of the locks that
        // FAIL under mutation lane A — and the o1 oracle at the swap content (both D11 routes
        // 275/0F, the optimised suite 12/0F).
        if (!CollectionHandler.filterPredArmWindowActive()
                || rawOperand == null || compiler == null || ctx == null
                || ctx.scope() == null
                || !(builder instanceof JavaExpression bodyExpr)
                || !HandlerHelper.bareOnlyElementCollapse(rawOperand, bodyExpr)) {
            return null;
        }
        RAttribute leaf = NavigationHandler.terminalNavAttr(rawOperand, compiler);
        if (leaf == null || MetaFieldGenerator.detectMetaKind(leaf)
                != MetaFieldGenerator.MetaKind.REFERENCE_WITH_META) {
            return null;
        }
        RJavaWithMetaValue wrapper = NavigationHandler.metaWrapperOf(leaf, compiler);
        if (wrapper == null) {
            return null;
        }
        String baseName = JavaNamingUtil.toFirstLower(wrapper.getSimpleName());
        if (!javax.lang.model.SourceVersion.isIdentifier(baseName)) {
            return null;
        }
        GeneratedIdentifier id = ctx.scope().createUniqueIdentifier(baseName);
        String nameToken = ctx.scope().registerDeferredCoercionName(id);
        Set<JavaClass<?>> declRefs = new HashSet<>(bodyExpr.getRefs());
        declRefs.add(wrapper);
        // The decl renders the deferred TOKEN (the #333 dtccDeclUseConsistency law —
        // a JavaLocalVariableDeclarationStatement render would getActualName() and
        // CLOSE the scope chain before the sibling arm's creates, the #346 hazard;
        // T394A cp2: the second arm's coercion param threw on the closed scope).
        ctx.scope().registerPendingLambdaHoist(new CollectionHandler.DeepThenArgHoist(
                wrapper.getSimpleName(), nameToken, rendered, declRefs,
                bodyExpr.getStaticWildcardImports()));
        String valueSimple = wrapper.getValueType().getSimpleName();
        refs.add(wrapper);
        refs.add(HandlerHelper.MAPPER_S);
        return "(" + nameToken + " == null ? MapperS.<" + valueSimple + ">ofNull() : MapperS.of("
                + nameToken + ".getValue()))";
    }

    /**
     * Wrap a bare implicit-FUNCTION-invocation operand (a no-args
     * {@link RSymbolReference} resolving to an {@link RFunction} — e.g. a
     * reporting-rule filter predicate's {@code IsMax32UpperCaseAlphanumericText})
     * in {@code MapperS.of(...)} so the comparison sees a Mapper operand.
     *
     * <p>{@code ReferenceHandler.renderImplicitFunctionInvocation} emits the
     * bare-function invocation UNWRAPPED ({@code <fn>.evaluate(<arg>)} — correct
     * for the boolean filter-predicate position it was built for, see
     * {@code CollectionHandler.filter_with_function_predicate_invokes_injected_instance}),
     * but a comparison operand must be a Mapper ({@code areEqual(Mapper, Mapper, …)});
     * golden wraps it:
     * {@code areEqual(MapperS.of(isMax32…Text.evaluate(item.get())), MapperS.of(true), …)}.
     *
     * <p><b>Regression-safe by construction:</b> an UNWRAPPED bare-function
     * comparison operand does not compile against the {@code areEqual}/{@code lessThan}/…
     * Mapper-operand signature, so no byte-identical golden file carries one — the
     * wrap only ever touches currently-non-compiling (waivered) output. Mutually
     * exclusive with {@link #wrapEnumOperand} (an enum value resolves to an
     * {@code REnumValue}, never an {@code RFunction}) and with already-Mapper
     * operands (navigations / literal wraps render {@code MapperS.of(...)} via a
     * different path and are not bare-function {@link RSymbolReference}s — so they
     * never reach this branch and cannot double-wrap).
     */
    private String wrapBareFunctionOperand(String rendered, RExpression rawOperand,
            Set<JavaClass<?>> refs, ExpressionCompiler compiler) {
        // facet bareValueMapperSWrap (PR #256): delegate to the single-source-of-truth helper so
        // the comparison-operand seat and the exists-argument / conditional-arm / lambda-return
        // seats cannot drift on what a "bare function operand" is (the #243 SOT-comment pattern).
        // facet existsOperandMapperCWrap (PR #302): pass the compiler so a MULTI-output bare
        // function operand wraps MapperC.<X>of(...) (the #301 (C) extension from the exists-operand
        // seat to the areEqual/comparison-operand seat — the #301 note's "future carrier" landed:
        // PortfolioContainingNonReportableComponentIndicatorRule csa's
        // areEqual(MapperC.<SupervisoryBodyEnum>of(supervisoryBodyForCSA.evaluate()), …)).
        return HandlerHelper.wrapBareInvocationOperand(rendered, rawOperand, refs, compiler);
    }

    /**
     * Wrap a bare enum-constant operand (by the producer's witness) (e.g. {@code EnumType.VALUE}) in
     * {@code MapperS.of(...)} so a comparison sees a Mapper operand, and
     * contribute {@link HandlerHelper#MAPPER_S} to {@code refs}.
     *
     * <p>{@link ReferenceHandler} renders an enum value reference as the BARE
     * dotted constant (relying on the enclosing context to apply any Mapper wrap
     * — see ReferenceHandler enumeration-present branches); the comparison
     * operand context is one such context, matching the golden:
     * {@code areEqual(<lhs-mapper>, MapperS.of(SettlementTypeEnum.CASH), ...)}.
     *
     * <p>Operands that are already Mapper expressions (navigations, literal /
     * variable wraps emitted as {@code MapperS.of(...)}, chained mappers) carry no
     * producer enum-constant witness, so {@link HandlerHelper#isBareEnumConstant}
     * returns {@code false} for them and they pass through unchanged — no double-wrap
     * (PR #611: the witness replaced the rendered-text shape test).
     *
     * <p>Delegates to {@link HandlerHelper#wrapEnumOperand} — the SAME wrap
     * {@code SetOperationHandler} applies to contains/disjoint operands (facet
     * void_witness_bare_enum arm D4), kept single-sourced so the comparison and
     * set-operation consumers cannot disagree.
     */
    private String wrapEnumOperand(JavaStatementBuilder operand, Set<JavaClass<?>> refs) {
        return HandlerHelper.wrapEnumOperand(operand, refs);
    }

    /**
     * facet aliasOperandMetaCoerce (PR #326, F4b): retype a NULL-typed bare ALIAS-call
     * comparison operand with the alias's Mapper signature type so the downstream
     * {@code coerceNavigationReceiver} meta-strip can fire. The type comes from
     * {@link NavigationHandler#tryAliasReceiverMapperType} — the SAME
     * {@code FunctionAliasHelper} walk that renders the alias method's
     * {@code Mapper*<? extends FieldWithMetaX>} signature into the same file (the
     * #163/#178 SAME-WALK invariant), and non-null ONLY for a META alias — so a
     * non-meta / unresolvable alias operand keeps its null type and the comparison is
     * byte-unchanged. Only the TYPE changes; the rendered source/refs pass through — and an
     * enum-constant witness passes through by IDENTITY (PR #611, below).
     */
    private static JavaStatementBuilder retypeNullTypedAliasOperand(JavaStatementBuilder builder,
            RExpression rawOperand, ExpressionCompiler compiler) {
        // facet enumConstantWitness (PR #611, the code review's MF-1): the retype below re-creates
        // the expression from its text and would DROP the producer's enum-constant witness that
        // the operand wrap downstream (HandlerHelper.wrapEnumOperand) reads — a witnessed constant
        // is null-typed like an alias call, so nothing else here declines it (a function alias
        // named like the enum value would reach the alias-name lookup). Identity for the witness:
        // zero corpus population (the census found no witness lost on either route), the hop
        // closed structurally rather than left to the alias-name coincidence.
        if (HandlerHelper.isBareEnumConstant(builder)) {
            return builder;
        }
        if (rawOperand == null || !(builder instanceof JavaExpression je)
                || je.getExpressionType() != null) {
            return builder;
        }
        JavaType aliasType = NavigationHandler.tryAliasReceiverMapperType(rawOperand, compiler);
        if (aliasType == null) {
            return builder;
        }
        return JavaExpression.from(je.renderToString(), aliasType, je.getRefs(),
                je.getStaticWildcardImports());
    }

    /**
     * facet deepThenLevelElementPreserve (PR #362): retype a NULL-typed BARE implicit-item
     * comparison operand from the item's render-true then-pipe binding
     * ({@link HandlerHelper#bareItemThenPipeMetaType} — the scope's {@code thenArgRefFor}
     * compiled-type channel, the #326 F4b retype pattern at the bare-item seat) so the
     * downstream {@code coerceNavigationReceiver} meta-strip fires the numbered
     * {@code .<Value>map("Type coercion", fieldWithMetaX0 -> …getValue())} deref (golden
     * drr DTCC_TradeParty1/2ReportingDestination: the identity-filter {@code mapItem}
     * lambda's {@code item} compared against bare enum lists). Non-null ONLY when the
     * binding's decl element is a meta wrapper — upstream compiles every comparison
     * operand against the meta-STRIPPED join, so golden ALWAYS derefs there (the #218
     * green-safety argument unchanged); every other operand keeps its null type
     * (byte-unchanged no-op).
     */
    private static JavaStatementBuilder retypeNullTypedBareItemOperand(JavaStatementBuilder builder,
            RExpression operand, ExpressionContext ctx, ExpressionCompiler compiler) {
        if (!(builder instanceof JavaExpression je) || je.getExpressionType() != null) {
            return builder;
        }
        com.regnosys.rosetta.generator.java.types.RJavaWithMetaValue meta =
                HandlerHelper.bareItemThenPipeMetaType(operand, ctx.scope(), compiler);
        if (meta == null || compiler.getTypeUtil() == null) {
            return builder;
        }
        JavaTypeUtil tu = compiler.getTypeUtil();
        // v3.2 seat 13 (D53, site R12 - the chaos C29InLambda rows): an EXPLICIT filter / extract parameter reaches
        // this retype since the binding read admits it (HandlerHelper.bareItemThenPipeMetaType), and upstream binds
        // that lambda variable AS the MapperS item (the #367 mapItemParamBareOperand law), so the strip renders on the
        // bare name - `c.<String>map("Type coercion", ...)`, never `MapperS.of(c).<String>map(...)`. The SAME #367
        // unwrap the seat's tail applies (unwrapClosureParamOperand: the exact `MapperS.of(<name>)` text of a
        // non-then closure parameter) is applied here, before the type is stamped (LAW 69).
        return JavaExpression.from(unwrapClosureParamOperand(je.renderToString(), operand),
                tu.wrap(tu.MAPPER_S, meta), je.getRefs(), je.getStaticWildcardImports());
    }

    /**
     * Resolve a BARE enum value used as a comparison operand to its qualified Java constant
     * ({@code bare_enum_comparison_operand}). Mirrors {@code ReferenceHandler.tryBareEnumArg}
     * (PR #137/#143), but the expected enum is pinned by the SIBLING operand's leaf feature
     * rather than a callee parameter.
     *
     * <p>{@code bareRaw} qualifies only when it is an UNRESOLVED no-arg {@link RSymbolReference}:
     * a bare enum value never resolves at link time (enum values are not in function scope), so
     * the variable path renders the non-compiling bare value name {@code MapperS.of(Clearing)}.
     * A sibling-seeded bare enum that the parser DID resolve carries a present {@link REnumValue}
     * symbol and already renders correctly via {@code ReferenceHandler}'s bare-REnumValue branch,
     * so the {@code symbol().isPresent()} guard excludes it (no double-handling). The enum is
     * recovered from {@code siblingRaw} via {@link NavigationHandler#leafEnumeration} — an
     * {@link com.regnosys.rosetta.ast.expressions.references.RFeatureCall} nav OR a disguised
     * {@link com.regnosys.rosetta.ast.expressions.references.REnumValueRef} nav
     * ({@code spreadLeg1 -> spreadNotation}); when the bare name matches a value of that enum,
     * emit {@code EnumName.CONSTANT} ({@link EnumHelper#convertValue} maps source {@code Clearing}
     * → Java {@code CLEARING}) and register the enum import (the same {@code REnumTypeRef} →
     * JavaClass the qualified {@code handle(REnumValueRef)} path uses — covers the case where the
     * sibling's {@code <Type>} witness does not already contribute it).
     *
     * <p><b>Regression-safe by construction:</b> the pre-fix rendering is the bare value name
     * ({@code MapperS.of(Clearing)}), a non-compiling undefined symbol, so no byte-identical
     * golden file carries it — the rewrite only ever touches currently-divergent (waivered)
     * output. The value-name match against the sibling enum is the strong gate: a non-enum
     * unresolved bare ref matches no enum value and declines. Returns {@code null} (leaving the
     * caller's rendering intact) when the operand is not a bare unresolved ref, the sibling has
     * no resolvable leaf enum, no value matches, or the translator is unavailable (stateless
     * unit calls — the import is then skipped exactly as in {@code enumImportRefs}).
     * The constant is returned as the producer's witness ({@link JavaExpression#enumConstant},
     * PR #611) so the caller's {@code wrapEnumOperand} keys on it — the arm IS the verdict.
     */
    private JavaExpression tryBareEnumComparand(RExpression bareRaw, RExpression siblingRaw,
            ExpressionCompiler compiler, Set<JavaClass<?>> refs) {
        // A STRICT bare enum operand (unresolved RSymbolReference / disguised REnumValueRef), or
        // facet PR #239: a TYPE-SHADOWED bare enum value whose simple name collides with a model
        // type, so it resolved its symbol to that TYPE (e.g. `Commodity` is both the
        // AssetClassEnum.Commodity value AND the Commodity data type) — bareEnumValueName declines,
        // typeShadowEnumValueName admits the candidate name, and the sibling-enum value-match below
        // is the load-bearing gate that keeps a genuine type reference from being rewritten.
        String valueName = HandlerHelper.bareEnumValueName(bareRaw);
        if (valueName == null) {
            valueName = HandlerHelper.typeShadowEnumValueName(bareRaw);
        }
        if (valueName == null) {
            return null;
        }
        REnumeration en = NavigationHandler.leafEnumeration(siblingRaw, compiler);
        if (en == null) {
            // facet enumQualify (PR #206): the sibling is a bare ALIAS or FUNCTION
            // reference whose result is enum-typed (e.g. `if actionType = NEWT` where the
            // alias `actionType: drrReport -> actionType` -> ActionTypeEnum). leafEnumeration
            // only resolves a direct navigation; recover the enum from the alias body /
            // callee output type.
            en = NavigationHandler.siblingComparandEnumeration(siblingRaw, compiler);
        }
        if (en == null) {
            // facet bare_enum_comparison_operand (PR #239): the sibling is a disguised
            // REnumValueRef navigation whose ALIAS-headed receiver resolves only through the
            // compiler-carrying disguise resolver (`quotedCurrencyPair(...) -> quoteBasis`);
            // leafEnumeration's REnumValueRef arm uses the compiler-less variant and returns null.
            en = NavigationHandler.disguisedSiblingEnumeration(siblingRaw, compiler);
        }
        if (en == null) {
            // facet bareEnumQualifyRBodyShadow (PR #299): the sibling is a bare
            // RSymbolReference resolving directly to an enum-typed RAttribute (a
            // filter-predicate attribute reference `supervisoryBody` / `source` /
            // `regimeName` / `identifierType`); siblingComparandEnumeration handles only
            // RShortcut/RFunction symbols (and is shared with ConversionHandler's green
            // source detection, so it is not widened).
            en = NavigationHandler.siblingAttributeEnumeration(siblingRaw, compiler);
        }
        if (en == null) {
            // facet enumRequalifyComparison (PR #348): the sibling is a DEEP feature
            // call (`observable -> Index ->> assetClass` — the DeepPathUtil
            // chooseAssetClass render, typed AssetClassEnum) — the FIFTH scoped rung,
            // via the SAME resolveDeepFeature walk the render uses. Golden cdm6
            // ObservableIsCommodity: the type-shadowed bare `Commodity` requalifies
            // to `MapperS.of(AssetClassEnum.COMMODITY)`; the corpus law re-verified
            // at #348 sizing — ZERO of 34,686 goldens carry `MapperS.of(<Capitalized
            // bare name>)`, so every firing seat is an already-waivered mismatch.
            en = NavigationHandler.deepSiblingEnumeration(siblingRaw, compiler);
        }
        if (en == null) {
            // facet annotationShadowedSiblingEnum (PR #354): the sibling is a bare
            // implicit-item attribute the linker MIS-BOUND to a same-named root element
            // (`filter qualification = confirmationDateTime` binds the `qualification`
            // ANNOTATION — the #204 class; the P354M2 probe), so the #299 attribute rung
            // declines. Recover through the SAME re-root synthesis the render uses.
            en = NavigationHandler.misBoundSiblingAttributeEnumeration(siblingRaw, compiler);
        }
        if (en == null) {
            // facet bareItemPipeSiblingEnum (PR #391): the sibling is the BARE implicit
            // item of a then-step lambda (`… then first then extract [if item = LEI …]`)
            // — the pipe's element-producing body resolves through the same rung ladder
            // (drr Extract_ReferenceEntityFormat's mapSingleToItem ladder seats).
            en = NavigationHandler.bareItemPipeSiblingEnumeration(siblingRaw, compiler);
        }
        if (en == null) {
            return null;
        }
        // facet bareEnumComparandHierarchy (PR #391): the value may be DECLARED on a
        // SUPER enum (`EntityIdentifierTypeEnum extends PartyIdentifierTypeEnum` —
        // LEI/CountryCode live on the parent) while the emission qualifies by the
        // SIBLING's child enum, whose generated Java flattens inherited values under
        // its own name (the #211/#358 flatten law — findEnumValueInHierarchy + the
        // CHILD-name qualification, identical to requalifyMisBoundEnumCase). An
        // own-value match finds the same value first, so every pre-#391 firing seat
        // is byte-identical.
        REnumValue val = HandlerHelper.findEnumValueInHierarchy(en, valueName);
        if (val != null) {
            JavaTypeTranslator translator = compiler.getTypeTranslator();
            Set<JavaClass<?>> witnessRefs = Set.of();
            if (translator != null) {
                JavaClass<?> enumClass = translator.toJavaReferenceType(new REnumTypeRef(en));
                refs.add(enumClass);
                if (enumClass != null) {
                    witnessRefs = Set.of(enumClass);
                }
            }
            // the witness carries the enum class it names (the factory's refs invariant, PR #611)
            return JavaExpression.enumConstant(en.name() + "." + EnumHelper.convertValue(val),
                    null, witnessRefs, Set.of());
        }
        return null;
    }

    /**
     * facet enumQualifyInheritedComparand (PR #215): a comparison operand that RESOLVED to an enum
     * value qualified by the DECLARING enum, where the sibling operand types as a CHILD enum that
     * INHERITS that value. Upstream qualifies the constant by the sibling's child enum (the comparison
     * type) — the generated Java child enum flattens the inherited value under its own name — but the
     * fork emits the declaring parent ({@code LeiIdentifierFormatEnum.LEI} where golden has
     * {@code PartyIdentifierFormat2Enum.LEI}, the type of the sibling {@code idFormat}). This is the
     * RESOLVED sibling of {@link #tryBareEnumComparand} (which only re-qualifies an UNRESOLVED bare
     * operand): here {@code operandRaw} carries a present enumeration/symbol, so
     * {@link HandlerHelper#bareEnumValueName} declines and the operand renders via
     * {@code handle(REnumValueRef)} (an EXPLICIT {@code Enum -> Value} — the author's enum) or, for a
     * BOUND bare symbol, via {@code handle(RSymbolReference)}'s bare-enum arm — which before v3.1
     * flip seat 12 qualified by the DECLARING enum and since seat 12 qualifies by the node's
     * INFERRED = expected enum ({@code HandlerHelper.boundEnumInferredOwner}, the same typeof(LHS)
     * the parser bound against), so this rung's bound-symbol branch recomputes the same answer and
     * stays load-bearing only for the EXPLICIT operand form.
     *
     * <p>Returns the child-qualified {@code ChildEnum.CONSTANT} as the producer's witness
     * ({@link JavaExpression#enumConstant}, PR #611; UNWRAPPED — {@code wrapEnumOperand}
     * applies the {@code MapperS.of} wrap on that witness, exactly as for the bare arm) and swaps the parent enum
     * import on {@code refs} for the child, or {@code null} when: the operand is not a resolved enum
     * value; the sibling has no resolvable leaf enum; the sibling enum IS the declaring enum; or the
     * sibling's hierarchy does not contain the value. That last gate is load-bearing — searching from
     * the sibling enum UP its {@code extends} chain finds the value only when the sibling is a CHILD
     * (or the declaring enum itself), so a sibling that is a SUPER of the declaring enum (the value is
     * below it) declines, and the re-qualification can only ever move DOWN to the comparison's child
     * type. Regression-safe: the parent-qualified comparison form appears in no byte-identical golden
     * (the corpus-wide grep at PR #215), so the rewrite only touches currently-waivered output.
     */
    private JavaExpression tryInheritedEnumRequalify(RExpression operandRaw, RExpression siblingRaw,
            ExpressionCompiler compiler, Set<JavaClass<?>> refs) {
        REnumeration parent;
        String valueName;
        if (operandRaw instanceof REnumValueRef evr && evr.enumeration().isPresent()) {
            parent = evr.enumeration().get();
            valueName = evr.valueName();
        } else if (operandRaw instanceof RSymbolReference ref
                && ref.symbol().isPresent()
                && ref.symbol().get() instanceof REnumValue ev
                && ev.parent() instanceof REnumeration p) {
            parent = p;
            valueName = ev.name();
        } else {
            return null;
        }
        if (valueName == null || valueName.isEmpty()) {
            return null;
        }
        REnumeration sibling = NavigationHandler.leafEnumeration(siblingRaw, compiler);
        if (sibling == null) {
            sibling = NavigationHandler.siblingComparandEnumeration(siblingRaw, compiler);
        }
        if (sibling == null) {
            sibling = symbolReferenceEnumeration(siblingRaw);
        }
        if (sibling == null) {
            // facet listOfListsCardinality (PR #370, F-D): the RULE-output rung — a bare
            // RULE-invocation sibling (`if OtherPaymentPayerFormat = Other`) types the
            // comparison by its inferred OUTPUT enum, so the operand qualifies by the
            // child (`PartyIdentifierFormatEnum.OTHER` where the value is declared on the
            // PartyIdentifierFormat2Enum super — golden hkma OtherPaymentPayer/
            // ReceiverSchemeNameRule ×2). Same #215 same-instance descend-only gate below.
            sibling = ruleOutputEnumeration(siblingRaw, compiler);
        }
        if (sibling == null) {
            // PR #447 (the typed-alias bind follow-through): the bare-PIPE-ITEM rung,
            // mirroring the #391 bareItemPipeSiblingEnumeration rung on the UNRESOLVED
            // arm (tryBareEnumComparand). The #447 alias bind types then-pipe chains,
            // so the engine's Category-14 expected-type binding now RESOLVES a bare
            // comparand inside a pipe lambda (`… then first then extract [if item =
            // LEI …]` — hkma Extract_ReferenceEntityFormat) to its super-enum-chain
            // declaration (LEI lives on PartyIdentifierTypeEnum), routing it through
            // THIS resolved arm instead of the bare arm — without this rung the
            // sibling item's enum is unrecoverable here and the operand keeps the
            // parent qualification, where upstream qualifies by the sibling's child
            // enum (EntityIdentifierTypeEnum — the #211/#358 flatten law). The #215
            // same-instance descend-only gate below stays the safety: a same-name
            // value on an UNRELATED enum declines.
            sibling = NavigationHandler.bareItemPipeSiblingEnumeration(siblingRaw, compiler);
        }
        if (sibling == null || sibling == parent) {
            return null;
        }
        // The sibling's hierarchy must contain the EXACT SAME REnumValue instance the operand
        // resolved to in the declaring enum — proving the sibling genuinely DESCENDS from `parent`
        // (it inherits/flattens THIS value), not merely that it happens to declare a value of the
        // same simple name (an unrelated enum sharing e.g. OTHER/UNKNOWN). A same-name but
        // different-instance match declines, so the re-qualification can only ever move DOWN the
        // operand value's own inheritance chain (Copilot PR #215 R1).
        REnumValue declared = HandlerHelper.findEnumValueInHierarchy(parent, valueName);
        REnumValue match = HandlerHelper.findEnumValueInHierarchy(sibling, valueName);
        if (match == null || match != declared) {
            return null;
        }
        JavaTypeTranslator translator = compiler.getTypeTranslator();
        Set<JavaClass<?>> witnessRefs = Set.of();
        if (translator != null) {
            // the resolved operand contributed the PARENT enum import (an EXPLICIT operand; a
            // BOUND bare symbol already contributed the child's since v3.1 flip seat 12 — the
            // remove is then a no-op); the child supersedes it.
            JavaClass<?> parentClass = translator.toJavaReferenceType(new REnumTypeRef(parent));
            if (parentClass != null) {
                refs.remove(parentClass);
            }
            JavaClass<?> childClass = translator.toJavaReferenceType(new REnumTypeRef(sibling));
            if (childClass != null) {
                refs.add(childClass);
                witnessRefs = Set.of(childClass);
            }
        }
        // the witness carries the enum class it names (the factory's refs invariant, PR #611)
        return JavaExpression.enumConstant(sibling.name() + "." + EnumHelper.convertValue(match),
                null, witnessRefs, Set.of());
    }

    /**
     * The enum type of a comparison sibling that is a bare symbol reference to an enum-typed attribute
     * (function input / output / local) — the sibling shape {@link NavigationHandler#leafEnumeration}
     * does not cover (it resolves navigations and disguised feature calls, not a plain parameter ref
     * like {@code idFormat = Lei}). Returns {@code null} for any unresolved or non-enum reference.
     */
    private static REnumeration symbolReferenceEnumeration(RExpression sibling) {
        if (sibling instanceof RSymbolReference ref
                && ref.symbol().isPresent()
                && ref.symbol().get() instanceof RAttribute attr
                && attr.typeCall() != null) {
            return attr.typeCall().referencedType()
                    .filter(REnumeration.class::isInstance)
                    .map(REnumeration.class::cast)
                    .orElse(null);
        }
        return null;
    }

    /**
     * facet listOfListsCardinality (PR #370, F-D): the enum OUTPUT type of a comparison
     * sibling that is a bare RULE invocation — the rule's output type is inferred-only
     * (not stored on the node), so read it from the workspace inference over the rule's
     * expression, exactly as {@code NavigationHandler.resolveReceiverOutputType}'s RRule
     * arm does for the data-type case. {@code null} for any other sibling shape or a
     * non-enum output (the pre-#370 rung ladder's answer stands).
     */
    private static REnumeration ruleOutputEnumeration(RExpression sibling,
            ExpressionCompiler compiler) {
        if (!(sibling instanceof RSymbolReference ref)
                || ref.symbol()
                        .filter(com.regnosys.rosetta.ast.functions.RRule.class::isInstance)
                        .isEmpty()) {
            return null;
        }
        com.regnosys.rosetta.ast.functions.RRule rule =
                (com.regnosys.rosetta.ast.functions.RRule) ref.symbol().get();
        // seat 21: the ONE rule-output read (HandlerHelper.ruleInferredOutputRType); this
        // consumer keeps its own enum post-filter.
        return HandlerHelper.ruleInferredOutputRType(rule, compiler.getGeneratorModel())
                instanceof com.regnosys.rosetta.types.REnumTypeRef enumRef
                ? enumRef.astNode()
                : null;
    }

    /**
     * facet comparisonIntWiden (PR #372, F-delta-3): append the elementwise
     * Integer-to-BigDecimal hop to a NAV-chain inequality operand whose kind-join
     * sibling is NUMBER ({@code .<BigDecimal>map("Type coercion", integerN ->
     * integerN == null ? null : BigDecimal.valueOf(integerN))} — golden cdm
     * CheckMaturity ×4 per cell). The param rides the deferred-coercion channel
     * (method-wide numbering: integer0/integer1). Hand-rolled — the compiled chain
     * carries a null type in the condition-helper context (the P372E probe), so
     * the service route cannot see it; nav shapes only (a literal keeps the #207
     * literal-typing arm; a bare identifier keeps its bytes).
     */
    private static JavaStatementBuilder widenIntegerNavOperand(JavaStatementBuilder b,
            RExpression rawOperand, ExpressionContext ctx) {
        if (!(b instanceof JavaExpression je)) {
            return b;
        }
        if (!(rawOperand instanceof com.regnosys.rosetta.ast.expressions.references.RFeatureCall)
                && !(rawOperand instanceof REnumValueRef)) {
            return b;
        }
        String rendered = je.renderToString();
        if (javax.lang.model.SourceVersion.isIdentifier(rendered)) {
            return b;
        }
        String param = ctx.scope().registerDeferredCoercionParam("integer");
        Set<JavaClass<?>> hopRefs = new HashSet<>(je.getRefs());
        hopRefs.add(HandlerHelper.BIG_DECIMAL);
        String hop = rendered + ".<BigDecimal>map(\"Type coercion\", " + param + " -> "
                + param + " == null ? null : BigDecimal.valueOf(" + param + "))";
        return JavaExpression.from(hop, null, hopRefs, je.getStaticWildcardImports());
    }

    /**
     * Infer the numeric Java type for a comparison by examining both operands.
     *
     * <p>If either operand has a known Rune DSL type that maps to a numeric Java type
     * (e.g., {@code number} → {@code BigDecimal}), returns that type. The "widest"
     * type wins: if one operand is {@code number} and the other is {@code int},
     * the result is {@code BigDecimal} (matching the Rune DSL implicit promotion rule).
     *
     * @return the inferred numeric JavaType, or {@code null} if not inferrable
     */
    private JavaType inferNumericType(RExpression left, RExpression right, ExpressionCompiler compiler) {
        JavaTypeUtil typeUtil = compiler.getTypeUtil();
        if (typeUtil == null) return null;

        String leftType = HandlerHelper.inferRuneTypeName(left);
        String rightType = HandlerHelper.inferRuneTypeName(right);

        // If either operand is "number", the comparison type is BigDecimal
        if (HandlerHelper.isNumberType(leftType) || HandlerHelper.isNumberType(rightType)) {
            return typeUtil.BIG_DECIMAL;
        }
        // If either operand is "int" from a resolved source (not a literal), use Integer.
        // Literals alone don't determine the comparison type — they can appear in both contexts.
        if (HandlerHelper.isResolvedIntType(leftType, left)
                || HandlerHelper.isResolvedIntType(rightType, right)) {
            return typeUtil.INTEGER;
        }
        // Fallback — the string heuristic above only recognises the directly-declared
        // builtin names ("number"/"int"). For a number/int type-ALIAS operand it
        // returns the ALIAS name (e.g. Max18D13Number, ShortFraction5DecimalNumber),
        // which isNumberType/isResolvedIntType do not match, so the comparison type
        // collapses to null and an int literal operand stays unwrapped — but upstream
        // wraps it (BigDecimal.valueOf(N)) because the alias resolves to BigDecimal.
        // Resolve the operand's underlying Java type through the inference engine +
        // translator (which resolves aliases transitively, as Max3Number → Integer /
        // Max18D13Number → BigDecimal) so a number-alias operand drives the BigDecimal
        // context and an int-alias drives Integer.
        JavaType leftJava = resolveOperandJavaType(left, compiler);
        JavaType rightJava = resolveOperandJavaType(right, compiler);
        if ((leftJava != null && typeUtil.isBigDecimal(leftJava))
                || (rightJava != null && typeUtil.isBigDecimal(rightJava))) {
            return typeUtil.BIG_DECIMAL;
        }
        if ((leftJava != null && typeUtil.isInteger(leftJava))
                || (rightJava != null && typeUtil.isInteger(rightJava))) {
            return typeUtil.INTEGER;
        }
        return null;
    }

    /**
     * Resolve a comparison operand's underlying Java type through the type
     * inference engine + translator, for number/int type-ALIAS operands the
     * string-name heuristic ({@link HandlerHelper#inferRuneTypeName}) names by
     * their alias rather than their base (e.g. {@code Max18D13Number} →
     * {@code BigDecimal}, {@code Max3Number} → {@code Integer}). Mirrors the
     * alias-through-translator resolution in
     * {@code NavigationHandler.resolveJavaSimpleName} and the inferred-type
     * read in {@code LiteralHandler.listItemJavaType}.
     *
     * <p>Operands the inference engine leaves untyped ({@code MISSING}) — e.g.
     * a shortcut-reference, whose type is only computed for its defining
     * expression — yield {@code null} here and remain unresolved (their literal
     * comparand stays unwrapped, still waivered); that is a separate facet.
     *
     * <p>Literal operands are intentionally NOT resolved here — an integer
     * literal can appear in both {@code int} and {@code number} contexts, so it
     * must not determine the comparison type (same invariant the
     * {@link HandlerHelper#isResolvedIntType} guard enforces for the string
     * path). Returns {@code null} when the model / translator are unavailable
     * (stateless unit calls) or the inferred type is missing.
     */
    private JavaType resolveOperandJavaType(RExpression operand, ExpressionCompiler compiler) {
        if (operand instanceof RIntLiteral || operand instanceof RNumberLiteral) {
            return null;
        }
        GeneratorModel gm = compiler.getGeneratorModel();
        JavaTypeTranslator tt = compiler.getTypeTranslator();
        if (gm == null || tt == null) {
            return null;
        }
        RMetaAnnotatedType inferred = gm.workspace().getInferredType(operand);
        if (inferred == null || inferred.isMissing()) {
            // (v3.2 seat 3: an alias-owner arm for the engine-MISSING implicit item was tried
            // here and measured to carry no witness — lane J green — the literal widening at an
            // alias condition is carried by HandlerHelper.numericOperandKind's alias-owner arm
            // through the literal handler's sibling-kind read; withdrawn under the #614 law.)
            return null;
        }
        return tt.toJavaReferenceType(inferred.type());
    }

    /**
     * Leg-C #429 (the typing-channel heal's consumer guard): compile a
     * comparison operand under the {@link #inferNumericType} ITEM-level
     * expected WITHOUT the entry-level terminal coercion for NON-literal
     * operands. Upstream compiles every comparison operand against the
     * Mapper-WRAPPED join ({@code MAPPER.wrapExtends(joined)}) — a
     * Mapper-valued operand NEVER derefs at the operand boundary; the
     * fork's item-level {@code numericType} channel exists for the interior
     * consumers (the int-literal {@code BigDecimal.valueOf} conversion, the
     * count comparand's Integer context), and threading it through
     * {@link ExpressionCompiler#compileInterior} preserves exactly those
     * (the context still carries the expected) while suppressing the entry
     * deref. Before the #429 closure/alias typing heal the channel was
     * inert for non-literals (their inferred types were MISSING, so
     * {@code numericType} stayed null or their compiled types were null and
     * the entry coercion never fired); the first typed carrier
     * ({@code StandardizedScheduleVarianceSwapNotionalAmount}'s
     * {@code then if item >= 1} — the alias-body pipe now types BigDecimal)
     * took a spurious {@code .get()} against golden. LITERAL operands keep
     * {@link ExpressionCompiler#compile} — their wrap conversion
     * ({@code MapperS.of(BigDecimal.valueOf(N))}, the 364-golden law) rides
     * the entry coercion. The {@code numericType == null} path keeps
     * {@code compile} for every operand (today's corpus-locked behavior).
     */
    private static JavaStatementBuilder compileComparisonOperand(RExpression operand,
            JavaType expected, boolean numericChannel, ExpressionContext ctx,
            ExpressionCompiler compiler) {
        // facet emptyComparisonOperandOfNull (W42 finding #19, PR #432): an EMPTY
        // literal operand renders upstream's canonical typed empty at the Mapper
        // operand seat — binaryExpr compiles both operands Mapper-expected, and the
        // null→MapperS coercion is `MapperS.<X>ofNull()` (TypeCoercionService's
        // empty-to-wrapper case; empty types NOTHING → Void, the same translation
        // the then-binding decl uses). The unclaimed compile splices the BARE null
        // (`areEqual(thenArg, null, All)` — upstream FunctionGeneratorTest
        // thenOperationTest F1 `empty then item = empty`), which NPEs the runtime's
        // null-safe equality walk (it dispatches on the Mapper OBJECT) — so no
        // green file carries the bare form and the arm is corpus-neutral by
        // construction (the D11 population run is the oracle). Declines (today's
        // bytes) without the type machinery.
        if (operand instanceof REmptyLiteral && compiler.getTypeUtil() != null) {
            JavaTypeUtil tu = compiler.getTypeUtil();
            Set<JavaClass<?>> emptyRefs = new HashSet<>();
            emptyRefs.add(HandlerHelper.MAPPER_S);
            emptyRefs.add(tu.VOID);
            return JavaExpression.from("MapperS.<Void>ofNull()",
                    tu.wrap(tu.MAPPER_S, tu.VOID), emptyRefs, Set.of());
        }
        boolean literal = operand instanceof RIntLiteral || operand instanceof RNumberLiteral;
        return numericChannel && !literal
                ? compiler.compileInterior(operand, expected, ctx.scope())
                : compiler.compile(operand, expected, ctx.scope());
    }

    /**
     * facet numeric_literal_typing (mechanism 1) — the LAST-resort comparison
     * typing arm, strictly after {@link #inferNumericType}'s string-heuristic and
     * engine arms (both byte-untouched): when EXACTLY ONE operand is an int
     * literal and the sibling operand resolves numerically through the
     * typed-model walk ({@link HandlerHelper#numericOperandKind} — an ALIAS
     * sibling recursing into its defining expression, or a resolution-blind
     * navigation chain resolving its leaf attribute through the gm-aware walk),
     * returns the sibling's numeric Java type as the expected type FOR THE
     * LITERAL ONLY. Upstream compiles both operands against the joined type
     * ({@code ExpressionGenerator.binaryExpr} →
     * {@code MAPPER.wrapExtends(leftRtype.join(rightRtype))}, then
     * {@code TypeCoercionService} wraps the int literal as
     * {@code BigDecimal.valueOf(N)} iff the join is BigDecimal); for the operand
     * kinds this arm resolves, the asymmetric form is behavior-identical — an
     * alias-call rendering carries a null expression type and never reads its
     * expected type — and bounds the byte-delta surface to
     * {@code LiteralHandler.isBigDecimalContext} exactly.
     *
     * <p>Corpus law (frozen 9.83.0 baseline, all 5 cells): of the comparison
     * shapes with one int-literal operand, the wrapped form
     * ({@code MapperS.of(BigDecimal.valueOf(N))}) appears 364 times — every
     * sibling number-typed — and the bare form 573 times — every sibling
     * int-typed (count results, period multipliers, record day/month fields);
     * zero counter-examples in either direction, and ZERO goldens compare an
     * alias-call operand against a bare int literal — so a green file cannot
     * carry the pre-fix bare emission for any shape this arm fires on. An
     * INTEGER-resolving sibling returns INTEGER, under which the literal renders
     * bare exactly as today (no byte delta). Declines (null → both operands keep
     * {@code ctx.expectedType()}): zero or two literal operands, unresolvable or
     * non-numeric siblings (after the PR #352 alias-signature fallback below),
     * literal-bodied aliases, missing typeUtil.
     */
    private JavaType literalSiblingNumericType(RExpression left, RExpression right,
            ExpressionCompiler compiler, ExpressionContext ctx) {
        boolean leftIsIntLiteral = left instanceof RIntLiteral;
        boolean rightIsIntLiteral = right instanceof RIntLiteral;
        if (leftIsIntLiteral == rightIsIntLiteral) {
            return null;
        }
        JavaTypeUtil typeUtil = compiler.getTypeUtil();
        if (typeUtil == null) {
            return null;
        }
        RExpression sibling = leftIsIntLiteral ? right : left;
        // facet deepThenSentinelOperandTyping (PR #389): an IMPLICIT sibling whose
        // MISSING-snapshot item is bound to a typed Mapper (the #351 consumer
        // sentinel) classifies from the render-truth binding — golden cdm6
        // StandardizedScheduleVarianceSwapNotionalAmount `greaterThanEquals(
        // ifThenElseResult0, MapperS.of(BigDecimal.valueOf(1)), All)`. The
        // asymmetric contract holds exactly: the literal ALONE takes the expected
        // type; the sentinel sibling keeps ctx.expectedType() and renders bare.
        JavaType bound = ctx == null ? null
                : HandlerHelper.implicitThenBindingItemType(sibling, ctx.scope(), compiler);
        if (bound != null && typeUtil.isBigDecimal(bound)) {
            return typeUtil.BIG_DECIMAL;
        }
        if (bound != null && typeUtil.isInteger(bound)) {
            return typeUtil.INTEGER;
        }
        switch (HandlerHelper.numericOperandKind(sibling, compiler)) {
            case NUMBER: return typeUtil.BIG_DECIMAL;
            case INT: return typeUtil.INTEGER;
            case UNKNOWN: break;
        }
        // facet comparisonAliasSiblingType (PR #352): an ALIAS sibling whose body the
        // AST walk above cannot type (a then-chain body — numericOperandKind's alias
        // arm recurses the BODY and RThenExpr has no arm there) resolves through the
        // SAME walk that renders the alias method's signature
        // (NavigationHandler.tryAliasReceiverMapperType → inferShortcutMapperJavaType,
        // whose RThenExpr arm is the #351 render-truth walk): the signature's ITEM type
        // classifies the join — a meta item derefs to its VALUE type first (upstream
        // joins meta-annotated operands on the value type, the #349
        // joinMetaAnnotatedTypes law; same deref as ArithmeticHandler's #347
        // arithStringJoinAlias arm). Qualify_StockSplit: `afterPrice > 0` where
        // afterPrice = `… filter … then extract value then only-element` renders
        // MapperS<BigDecimal>, so the literal wraps BigDecimal.valueOf(0) exactly like
        // its beforeNoOfUnits sibling. Green-safe per the corpus law above: ZERO
        // goldens compare an alias-call operand against a bare int literal, so every
        // fire is on an already-waivered file; an Integer-item alias returns INTEGER,
        // under which the literal renders bare exactly as today.
        JavaType aliasType = NavigationHandler.tryAliasReceiverMapperType(sibling, compiler);
        if (aliasType == null) {
            return null;
        }
        JavaType aliasItem = typeUtil.getItemType(aliasType);
        if (aliasItem instanceof RJavaWithMetaValue withMeta) {
            aliasItem = withMeta.getValueType();
        }
        if (typeUtil.isBigDecimal(aliasItem)) {
            return typeUtil.BIG_DECIMAL;
        }
        if (typeUtil.isInteger(aliasItem)) {
            return typeUtil.INTEGER;
        }
        return null;
    }

    /**
     * Render the {@code CardinalityOperator} for a comparison. An explicit source
     * modifier ({@code any}/{@code all}) wins; with no modifier ({@code mod == null})
     * the operator-dependent {@code defaultOp} applies ({@code "Any"} for {@code <>},
     * {@code "All"} otherwise). Mirrors upstream {@code toCardinalityOperator}.
     */
    private String cardinalityOperator(CardMod mod, String defaultOp) {
        if (mod == CardMod.ANY) {
            return "CardinalityOperator.Any";
        }
        if (mod == CardMod.ALL) {
            return "CardinalityOperator.All";
        }
        return "CardinalityOperator." + defaultOp;
    }
}
