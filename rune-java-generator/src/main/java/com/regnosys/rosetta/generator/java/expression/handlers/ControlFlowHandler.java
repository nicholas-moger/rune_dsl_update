package com.regnosys.rosetta.generator.java.expression.handlers;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.enums.SwitchGuardKind;
import com.regnosys.rosetta.ast.enums.SwitchGuardLiteralKind;
import com.regnosys.rosetta.ast.expressions.binary.RComparisonExpr;
import com.regnosys.rosetta.ast.expressions.binary.REqualityExpr;
import com.regnosys.rosetta.ast.expressions.binary.RThenExpr;
import com.regnosys.rosetta.ast.expressions.constructors.RConditionalExpr;
import com.regnosys.rosetta.ast.expressions.constructors.RConstructorExpr;
import com.regnosys.rosetta.ast.expressions.literals.RBooleanLiteral;
import com.regnosys.rosetta.ast.expressions.literals.RIntLiteral;
import com.regnosys.rosetta.ast.expressions.literals.RListLiteral;
import com.regnosys.rosetta.ast.expressions.literals.RStringLiteral;
import com.regnosys.rosetta.ast.expressions.references.REmptyLiteral;
import com.regnosys.rosetta.ast.expressions.references.REnumValueRef;
import com.regnosys.rosetta.ast.expressions.references.RFeatureCall;
import com.regnosys.rosetta.ast.expressions.references.RSymbolReference;
import com.regnosys.rosetta.ast.expressions.unary.RConversionExpr;
import com.regnosys.rosetta.ast.expressions.unary.RExtractExpr;
import com.regnosys.rosetta.ast.expressions.unary.RToStringExpr;
import com.regnosys.rosetta.ast.expressions.supporting.RSwitchCase;
import com.regnosys.rosetta.ast.expressions.supporting.RSwitchCaseGuard;
import com.regnosys.rosetta.ast.supporting.REnumValue;
import com.regnosys.rosetta.generator.java.SilentDegradation;
import com.regnosys.rosetta.ast.expressions.unary.RCardinalityCheckExpr;
import com.regnosys.rosetta.ast.expressions.unary.RSwitchExpr;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.JavaNamingUtil;
import com.regnosys.rosetta.generator.java.expression.ExpressionCompiler;
import com.regnosys.rosetta.generator.java.expression.ExpressionContext;
import com.regnosys.rosetta.generator.java.function.StatementHoistSession;
import com.regnosys.rosetta.generator.java.scoping.CondListCoerce;
import com.regnosys.rosetta.generator.java.scoping.CondSingleCoerce;
import com.regnosys.rosetta.generator.java.scoping.JavaStatementScope;
import com.regnosys.rosetta.generator.java.statement.JavaRawStatement;
import com.regnosys.rosetta.generator.java.statement.JavaStatement;
import com.regnosys.rosetta.generator.java.statement.builder.JavaExpression;
import com.regnosys.rosetta.generator.java.template.ImportCollisionResolver;
import com.regnosys.rosetta.generator.java.statement.builder.JavaStatementBuilder;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.generator.java.object.MetaFieldGenerator;
import com.regnosys.rosetta.generator.java.types.RJavaWithMetaValue;
import com.regnosys.rosetta.types.ExpressionCardinality;
import com.regnosys.rosetta.types.inference.CardinalityComputer;
import com.regnosys.rosetta.types.REnumTypeRef;
import com.regnosys.rosetta.types.RType;
import com.rosetta.util.types.JavaClass;
import com.rosetta.util.types.JavaType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Handles code generation for control flow expressions:
 * {@link RConditionalExpr} and {@link RSwitchExpr}.
 *
 * <p><b>Conditionals (facet {@code ifthenelse_result_hoisting}):</b> a
 * {@code ComparisonResult}-expected conditional compiled inside a
 * statement-hoist sink takes upstream's hoisted statement-local form —
 * <pre>
 *   final ComparisonResult ifThenElseResultN;
 *   if (cond.getOrDefault(false)) { ifThenElseResultN = then; }
 *   else { ifThenElseResultN = else; }
 * </pre>
 * — registered on the sink and consumed as the bare local (see
 * {@link #buildIfThenElseHoistBlock}; the pathed conditional-SET arm of the
 * same facet renders in {@code FunctionExpressionRenderer}).
 *
 * <p><b>Facet {@code ctor_setter_ite_hoist}:</b> a NO-ELSE single-cardinality
 * conditional at any other single-expression seat (ctor pair value, evaluate
 * arg) inside a sink hoists the ITEM-typed initializer form —
 * <pre>
 *   &lt;ItemType&gt; ifThenElseResultN = null;
 *   if (cond.getOrDefault(false)) { ifThenElseResultN = then; }
 * </pre>
 * — consumed as the bare local ({@link #hoistAsItemLocalOrNull}; upstream
 * {@code JavaIfThenElseBuilder.collapseToSingleExpression}). Everywhere the
 * hoists DECLINE (effective-else ladders, MULTI seats, unmarked compilation
 * paths — rule conditions, POJO conditions, aliases, then-bodies — and
 * lambda interiors) the original M7b-1 inline forms remain:
 * <pre>
 *   if cond then a else b  →  condition.getOrDefault(false) ? thenExpr : elseExpr
 *   if cond then a         →  condition.getOrDefault(false) ? thenExpr : null
 *   switch arg ...          →  (until v3.2 seat 12 — a type-name g1 until v3.1 C0)
 *                              Objects.equals(g1, arg) ? r1 : … : defaultResult
 * </pre>
 * No golden carries the {@code getOrDefault(false) ? } ternary, so every file
 * the inline form touches is a waivered mismatch. Switch expressions have NO inline
 * form on this handler's path since v3.2 seat 12 (D52, R1): a switch no ladder
 * renderer claims is REFUSED at {@code SilentDegradation.Site.SWITCH_TERNARY_STUB}
 * (statement-form basic switches render in
 * {@code FunctionExpressionRenderer.renderSwitchAssignment}; the hoisted ladders in
 * {@link #handle(RSwitchExpr, ExpressionContext, ExpressionCompiler)}).
 */
public class ControlFlowHandler {

    /**
     * AST-static cardinality reads for the #432 conditional seats (the
     * extract-receiver Mapper-form slot guard + the mixed-cardinality ternary arm
     * lift) — the same computer {@code CollectionHandler}'s receiver-multi reads
     * use; the workspace channel stays untouched for every existing consult.
     */
    private static final CardinalityComputer CARDINALITY = new CardinalityComputer();

    // =========================================================================
    // Conditional (if-then-else)
    // =========================================================================

    /**
     * Compiles a conditional expression — the hoisted statement-local form
     * when the A2 gate accepts ({@link #hoistAsComparisonResultOrNull}), the
     * inline ternary {@code condition.getOrDefault(false) ? thenExpr : elseExpr}
     * on every declined path (see the class javadoc).
     *
     * @param expr     the conditional expression node
     * @param ctx      the current expression compilation context
     * @param compiler the parent compiler for recursive compilation of sub-expressions
     * @return a {@link JavaExpression} — the bare hoisted local reference, or
     *         the inline ternary rendering
     */
    public JavaStatementBuilder handle(RConditionalExpr expr, ExpressionContext ctx, ExpressionCompiler compiler) {
        // facet deepThenCtlRestructure (PR #351): a then-LEVEL body whose ROOT is this
        // exact conditional (the deep-seat handshake) hoists golden's Mapper-typed
        // `final Mapper*<X> ifThenElseResult; if/else[-if]` block into the statement
        // channel and renders as the bare sentinel — the deep-seat twin of the SET-seat
        // #257/#350-F2 appendIteHoistChainCore forms. FIRST in the arm chain: the
        // handshake node must not fall into the generic arms (the cp4 catch — the
        // item-typed `final Boolean` arm fired on a blessed node, my unfired-handshake
        // decline then declined the whole chain, and the stranded block Franken-rendered
        // alongside the inline fallback). Handshake-gated (node identity), so every
        // non-blessed conditional keeps its existing route BY CONSTRUCTION. Declines
        // (null) on: no handshake / node mismatch, no sink, an unresolvable (Object)
        // item type, or a multi-statement sub-compile — the deep seat then declines the
        // WHOLE chain (no partial restructure — the #232/#350-F5b law).
        JavaStatementBuilder deepThenHoisted = hoistAsDeepThenMapperLocalOrNull(expr, ctx, compiler);
        if (deepThenHoisted != null) {
            return deepThenHoisted;
        }

        // facet ifthenelse_result_hoisting (arm A2): a conditional consumed at
        // a ComparisonResult-expected position inside a statement-hoist sink
        // hoists `final ComparisonResult ifThenElseResultN; if/else {...}` and
        // renders as the bare local — upstream compiles EVERY Rosetta
        // conditional as a JavaIfThenElseBuilder and any mid-expression
        // consumption collapses it via declareAsVariable("ifThenElseResult")
        // (the goldens carry ZERO `getOrDefault(false) ? :` ternaries, so every
        // file the ternary touches is a waivered mismatch — green-safe by
        // construction). Declines (null) to the ternary below everywhere else:
        // non-CR expected types, unmarked compilation paths (rule conditions,
        // POJO conditions, aliases, then-bodies), and lambda interiors
        // (upstream's block-lambda form is a different, out-of-scope shape).
        JavaStatementBuilder hoisted = hoistAsComparisonResultOrNull(expr, ctx, compiler);
        if (hoisted != null) {
            return hoisted;
        }

        // facet condListCoerce (PR #327, arm A1): a no-real-else SINGLE-cardinality
        // conditional whose CONSUMER seat is MULTI (a List ctor-setter pair / a multi
        // segment-ADD leaf — the consumer pushed a CondListCoerce handshake for THIS
        // node) hoists the LIST-typed per-branch coercion form instead of the item
        // form: upstream compiles the conditional at the consumer's List expected
        // type, so each arm coerces item→list (TypeCoercionService.convertNullSafe —
        // then-arm value hoisted to a type-named local + the null-guarded
        // singletonList/emptyList ternary; absent else = emptyList) and the consumer
        // splices the local BARE. Declines (null) to the item arm below when no
        // handshake targets this node.
        JavaStatementBuilder listHoisted = hoistAsListLocalOrNull(expr, ctx, compiler);
        if (listHoisted != null) {
            return listHoisted;
        }

        // facet ctor_setter_ite_hoist: a no-else single-cardinality conditional
        // consumed at a single-expression seat (ctor pair value, evaluate arg)
        // inside a statement-hoist sink hoists an ITEM-typed initializer-form
        // local — upstream JavaIfThenElseBuilder.collapseToSingleExpression →
        // declareAsVariable(true, "ifThenElseResult") at EVERY mid-expression
        // consumption (ctor entry ExpressionGenerator.xtend:1205-1207, method
        // args JavaStatementBuilder.invokeMethod). Declines (null) to the
        // ternary below on effective-else / MULTI / sink-less / untypable-then
        // shapes (see the method javadoc).
        JavaStatementBuilder itemHoisted = hoistAsItemLocalOrNull(expr, ctx, compiler);
        if (itemHoisted != null) {
            return itemHoisted;
        }

        // The declined-hoist ternary keeps TODAY's bytes: a ComparisonResult
        // expectation only ever arrives from LogicalHandler's targeted
        // conditional-operand threading (this facet), so stripping it restores
        // the exact pre-facet null-expected arm compilation.
        JavaType ternaryExpected = ctx.expectedType();
        JavaTypeUtil typeUtil = compiler.getTypeUtil();
        if (ternaryExpected != null && typeUtil != null
                && typeUtil.isComparisonResult(ternaryExpected)) {
            ternaryExpected = null;
        }

        // PR-A §9.1 C3a.4.i: operand-refs flowthrough from all three branches.
        // facet deep_value_then_hoist (PR #250): the inline ternary is a cascade-UNSAFE
        // seat for the deep VALUE-then hoist (CollectionHandler.tryDeepThenHoist) —
        // hoisting a `final Mapper*<X> thenArg = …;` decl out of a
        // `getOrDefault(false) ? : ` arm would force the ternary into an if/else block and
        // renumber co-resident hoist groups (the #219 regression). Suppress the deep
        // value-then hoist while compiling the condition + arms so a value-then there
        // declines to its inline form; a ComparisonResult-output then is unaffected (it
        // hoists through its own boolean-context path, independently green-safe).
        Set<JavaClass<?>> refs = new HashSet<>();
        Set<JavaClass<?>> wildcards = new HashSet<>();
        String condition;
        String thenBranch;
        String elseBranch;
        // facet mapperWrapPrefix (v3.1 C2d retirement family 8, PR #615): the two ARM BUILDERS are
        // HOISTED out of the `try` (and the else arm out of its `.map(e -> …)` lambda) so the
        // mixed-arity lift below can read the wrap factory's own marker instead of the arm's
        // rendered prefix. DECLARATIONS MOVE, NOTHING ELSE DOES — the two `compiler.compile(…)`
        // calls, the render calls, the refs/wildcard collection and the
        // pushThenValueHoistSuppression()/popThenValueHoistSuppression() bracket stay exactly where
        // they were, and the absent-else case still renders the literal "null" (the builder stays
        // null and the lift declines, as the text test declined on "null" before).
        JavaStatementBuilder thenBuilder = null;
        JavaStatementBuilder elseBuilder = null;
        ctx.scope().pushThenValueHoistSuppression();
        try {
            JavaStatementBuilder conditionBuilder =
                    compiler.compile(expr.condition(), ternaryExpected, ctx.scope());
            thenBuilder = compiler.compile(expr.thenBranch(), ternaryExpected, ctx.scope());
            condition = HandlerHelper.render(conditionBuilder);
            thenBranch = HandlerHelper.render(thenBuilder);
            refs.addAll(conditionBuilder.getRefs());
            refs.addAll(thenBuilder.getRefs());
            wildcards.addAll(conditionBuilder.getStaticWildcardImports());
            wildcards.addAll(thenBuilder.getStaticWildcardImports());

            RExpression elseNode = expr.elseBranch().orElse(null);
            if (elseNode != null) {
                elseBuilder = compiler.compile(elseNode, ternaryExpected, ctx.scope());
                refs.addAll(elseBuilder.getRefs());
                wildcards.addAll(elseBuilder.getStaticWildcardImports());
                elseBranch = HandlerHelper.render(elseBuilder);
            } else {
                elseBranch = "null";
            }
        } finally {
            ctx.scope().popThenValueHoistSuppression();
        }

        // facet ternaryMixedArityArmLift (W42 finding #20, PR #432): DIFFERING arm
        // cardinalities leave the ternary's poles as MapperC vs MapperS — no common
        // Mapper type, so a Mapper-consuming seat (the mapSingleToList lambda return)
        // cannot infer its type variable (upstream FunctionGeneratorTest
        // canReturnDifferingCardinalitiesInIfThenElseBranches: `42 extract if False
        // then [1, 2] else 0`). Upstream compiles every conditional arm at the seat's
        // expected type, so its MapperS→MapperC conversion lifts the SINGLE arm with
        // `MapperC.of(<mapper>)` (TypeCoercionService's actual-isMapperS /
        // expected-isMapperC case — no type witness). AST-static gate (one arm MULTI,
        // one SINGLE) + the arm's own WRAP MARKER (`unwrapToBuilder()`), so a bare-item /
        // hoisted-local arm keeps its bytes; the class javadoc's zero-golden law makes
        // the whole ternary corpus-neutral by construction.
        //
        // THE MARKER REPLACED THE ARM'S RENDERED `MapperS.of(` PREFIX at facet mapperWrapPrefix
        // (v3.1 C2d retirement family 8, PR #615). Its sibling half — LiteralHandler's list-literal
        // element seat, whose `.endsWith(".get()")` half retired at facet collapseGetSuffix (v3.1
        // C2d retirement family 7, PR #614) — keeps a SPLICE guard of its own, a newline test on
        // the text it is about to splice; the two halves are not clause-for-clause mirrors, and
        // that is deliberate.
        // facet mapperWrapPrefix (v3.1 C2d retirement family 8, PR #615): BOTH arm tests are
        // RETIRED onto the hoisted builders' own wrap marker. c8 census, 54 arrivals at each seat
        // (18 / 18 / 18 — default-route D11, IR-route D11, optimised), carriers
        // `rule:UnderlierIdOther` and `rule:UnderlierIdOtherSource`: the producer-registered
        // unwrap fact agrees with the retired text test at 54/54 on BOTH seats, with no
        // disagreement signature and 100.00% producer recovery. TWO priors were measured rather
        // than argued — the survey's ZERO-D11-ARRIVAL prediction is REFUTED (the method IS entered,
        // 54 times), while the class javadoc's ZERO-GOLDEN law SURVIVES intact (it is about the
        // LIFT firing, and the lift never fires: `thenCard=SINGLE AND elseCard=MULTI` at 54/54, so
        // the else seat's gate is closed at every arrival and the then seat's gate opens and then
        // DECLINES on the arm).
        //
        // THE AGREEMENT IS DECLINE-ONLY, AND THAT IS SAID PLAINLY: all 54 are
        // `unwrap=no AND text=false`. The corpus supplies a decline lock and nothing more, so the
        // POSITIVE polarity's witness is the unit fixture (LAW 72) — the upstream
        // `canReturnDifferingCardinalitiesInIfThenElseBranches` shape, whose SINGLE arm is a scalar
        // literal and therefore compiles through `LiteralHandler`, ALREADY migrated to
        // `wrappedInMapperSOf`, so the marker IS present and the swap fires exactly where the text
        // fired. `MapperWrapPrefixSeatTest` carries it.
        //
        // THE `instanceof JavaExpression` NARROWING IS A CONJUNCT, not plumbing (the code-quality
        // review's SF-7): the retired text test read a String the caller already held, while the
        // marker lives on the BUILDER, so an arm hoisted as a non-JavaExpression statement builder
        // now declines here where the text test would still have been evaluated. Corpus-inert and
        // measured: `kind=JavaExpression*` at 54/54 census arrivals on both seats, so no arrival
        // in the corpus is decided by the narrowing rather than by the marker.
        if (expr.elseBranch().isPresent()) {
            ExpressionCardinality thenCard = CARDINALITY.compute(expr.thenBranch());
            ExpressionCardinality elseCard = CARDINALITY.compute(expr.elseBranch().get());
            if (thenCard == ExpressionCardinality.MULTI
                    && elseCard != ExpressionCardinality.MULTI
                    && elseBuilder instanceof JavaExpression elseArmExpr
                    && elseArmExpr.unwrapToBuilder().isPresent()) {
                elseBranch = "MapperC.of(" + elseBranch + ")";
                refs.add(HandlerHelper.MAPPER_C);
            } else if (elseCard == ExpressionCardinality.MULTI
                    && thenCard != ExpressionCardinality.MULTI
                    && thenBuilder instanceof JavaExpression thenArmExpr
                    && thenArmExpr.unwrapToBuilder().isPresent()) {
                thenBranch = "MapperC.of(" + thenBranch + ")";
                refs.add(HandlerHelper.MAPPER_C);
            }
        }

        // v3.2 seat 13 (D53, THE CLOSING SEAT - the LOUD register's conditional site R14, the chaos M9 rows
        // C30MultiThenAdd): this inline ternary is the LAST-RESORT render of a conditional every hoist seat declined.
        // It is UPSTREAM'S OWN render too - inside a lambda (`greaterThan(a, b, All).getOrDefault(false) ? a : b` in a
        // reduce, the ported UpstreamListOperationPortTest expectations) and at every declined-hoist seat whose arms
        // agree - so the ternary is refused ONLY where javac refuses it: the two arms' ITEM types are both known and
        // DIFFER (the M9 class - `add w -> scores: if n > 0 then [n, n + 1] else [0]`, a BigDecimal list against an
        // Integer list, whose consumer appends `.getMulti()` to the ELSE arm alone; 12 of 12 non-compiling by the
        // seat-13 census, SILENT). The arm's item type is its compiled stamp or, for a list literal, the SAME
        // LiteralHandler.listItemJavaType walk the ADD / SET seats' element coercion reads (LAW 69); an untyped arm
        // keeps the ternary. The first cut of this site (commit 4's first pass) refused the ternary WHOLE on the
        // "zero goldens carry `.getOrDefault(false) ? `" census - true of the vendored goldens, false of the render's
        // legitimacy: the whole generator suite on the offload box (twenty red tests, the port expectations among
        // them) and the optimised route's rule face on vendored drr 6.34.1 (its overlay run) refuted the whole-site
        // refusal before this commit. The hoist at the M9 seat is the v3.3 heal (D53 decision 2).
        JavaTypeUtil ternaryTu = compiler.getTypeUtil();
        JavaType thenItem = ternaryArmItemType(expr.thenBranch(), thenBuilder, ternaryTu, compiler);
        JavaType elseItem = expr.elseBranch().isPresent()
                ? ternaryArmItemType(expr.elseBranch().get(), elseBuilder, ternaryTu, compiler) : null;
        if (thenItem != null && elseItem != null && !thenItem.equals(elseItem)) {
            throw SilentDegradation.refuse(SilentDegradation.Site.INLINE_CONDITIONAL_TERNARY,
                    "conditional at a seat with no hoist renderer whose arms' item types differ (then "
                            + describeArm(expr.thenBranch()) + " " + thenItem.getSimpleName() + ", else "
                            + expr.elseBranch().map(ControlFlowHandler::describeArm).orElse("absent") + " "
                            + elseItem.getSimpleName() + ") - the pre-seat render was the inline ternary `" + condition
                            + ".getOrDefault(false) ? " + thenBranch + " : " + elseBranch + "`, whose consumer's"
                            + " `.getMulti()` javac refuses across the two element types",
                    expr);
        }
        return JavaExpression.from(
                condition + ".getOrDefault(false) ? " + thenBranch + " : " + elseBranch,
                null,
                refs,
                wildcards);
    }

    /**
     * v3.2 seat 13 (site R14): a ternary arm's ITEM type - the compiled stamp's item, or for a LIST LITERAL the
     * {@code LiteralHandler.listItemJavaType} walk (the ADD / SET seats' own element read, LAW 69); {@code null} when
     * neither knows (the arm keeps the ternary).
     */
    private static JavaType ternaryArmItemType(RExpression arm, JavaStatementBuilder compiled, JavaTypeUtil tu,
            ExpressionCompiler compiler) {
        if (tu == null) {
            return null;
        }
        JavaType t = compiled == null ? null : compiled.getExpressionType();
        JavaType item = t == null ? null : tu.getItemType(t);
        if (item == null && arm instanceof RListLiteral lit) {
            item = LiteralHandler.listItemJavaType(lit, compiler);
        }
        return item;
    }

    /** v3.2 seat 13 (site R14): the arm's AST kind for the refusal's witness - never its render. */
    private static String describeArm(RExpression arm) {
        return arm == null ? "absent" : arm.getClass().getSimpleName();
    }

    /**
     * v3.2 seat 2 (Law 3): a literal case result's Java item type - the signature walk's literal
     * mapping at the ONE witnessed class, the int literal ({@code CollectionHandler
     * .isDeepThenLiteralSwitchConsumer} admits no other result at this seat since PR #623's
     * witness sweep; the number / string / boolean mappings stayed green when narrowed away and
     * are banked, #614).
     */
    private static JavaClass<?> literalItemTypeOrNull(RExpression e, JavaTypeUtil typeUtil) {
        if (e instanceof RIntLiteral) {
            return typeUtil.INTEGER;
        }
        return null;
    }

    /**
     * v3.2 seat 2 (Law 3, the chaos C5Forms {@code graded} rows): the deep-then SWITCH consumer —
     * the switch twin of {@link #hoistAsDeepThenMapperLocalOrNull}. A then-chain whose LAST body
     * is a STRING-literal-guarded int-valued switch over the piped item ({@code CollectionHandler
     * .isDeepThenLiteralSwitchConsumer} — the ONE predicate the control-flow scan, the consumer
     * wiring and this render all read; the one witnessed class, the other literal classes banked
     * under the #614 law at PR #623's witness sweep) hoists upstream's lowering into the statement channel:
     * <pre>
     * final MapperS&lt;Integer&gt; ifThenElseResult;
     * if (thenArg2.get() == null) {
     *     ifThenElseResult = MapperS.&lt;Integer&gt;ofNull();
     * } else if (areEqual(thenArg2, MapperS.of("a"), CardinalityOperator.All).get()) {
     *     ifThenElseResult = MapperS.of(1);
     * } else {
     *     ifThenElseResult = MapperS.of(0);
     * }
     * </pre>
     * and renders as the bare sentinel (golden {@code return ifThenElseResult;}). The subject is
     * the bound {@code thenArg} of the switch's own then-body (the same binding the piped-item
     * navigation renders from); each rung is the SET-position switch ladder's rung form
     * ({@code renderSwitchAssignment}: the null guard first, then {@code areEqual(subject, guard,
     * All).get()}); the guard renders from the front-end literal KIND ({@code HandlerHelper
     * .literalGuardMapper}); the arms take {@link #wrapDeepThenIteArm} (a Mapper-typed arm as-is,
     * a raw value wrapped); the local's element is the arms' agreed item type — the same join the
     * alias signature walk performs ({@code FunctionAliasHelper.switchValueCaseJoin}'s literal-
     * result class), so the {@code MapperS<Integer> graded} signature and this decl cannot
     * disagree. Declines (null → the whole chain declines, no partial restructure) on: no
     * handshake / node mismatch / the lambda route, no sink, an unbound subject, a non-MapperS
     * subject, a multi-statement case compile, or arms whose item types disagree.
     */
    private JavaStatementBuilder hoistAsDeepThenSwitchLadderOrNull(RSwitchExpr sw,
            ExpressionContext ctx, ExpressionCompiler compiler) {
        JavaStatementScope.DeepThenIteHoist handshake = ctx.scope() == null ? null
                : ctx.scope().findDeepThenIteHoistCond();
        if (handshake == null || handshake.cond() != sw || handshake.lambdaRoute()) {
            return null;
        }
        JavaStatementScope sink = ctx.scope().findStatementHoistSink();
        if (sink == null || sink.statementHoistSession() == null) {
            return null;
        }
        JavaTypeUtil typeUtil = compiler.getTypeUtil();
        if (typeUtil == null || compiler.getTypeTranslator() == null
                || compiler.getGeneratorModel() == null) {
            return null;
        }
        if (!(sw.parent() instanceof com.regnosys.rosetta.ast.expressions.supporting.RInlineFunction owner)) {
            return null;
        }
        JavaExpression subjectRef = ctx.scope().thenArgRefFor(owner);
        if (subjectRef == null || subjectRef.getExpressionType() == null
                || !typeUtil.isMapperS(subjectRef.getExpressionType())) {
            return null;
        }
        String subject = HandlerHelper.render(subjectRef);
        Set<JavaClass<?>> refs = new HashSet<>();
        Set<JavaClass<?>> wildcards = new HashSet<>();
        List<String> guardStrs = new ArrayList<>();
        List<JavaExpression> caseExprs = new ArrayList<>();
        List<RExpression> caseNodes = new ArrayList<>();
        JavaExpression defaultExpr = null;
        RExpression defaultNode = null;
        JavaClass<?> itemType = null;
        for (RSwitchCase sc : sw.cases()) {
            int caseMark = sink.statementHoistMark();
            JavaStatementBuilder compiled = compiler.compile(sc.expression(), null, ctx.scope());
            if (!sink.drainStatementHoistsSince(caseMark).isEmpty()
                    || !(compiled instanceof JavaExpression caseExpr)) {
                return null;
            }
            JavaType caseType = caseExpr.getExpressionType();
            JavaType caseItem = caseType == null ? null : typeUtil.getItemType(caseType);
            if (caseItem == null) {
                // a literal result compiles null-stamped (`MapperS.of(1)`); its Java item is the
                // literal's own - the SAME mapping the signature walk's literal arm performs
                // (Integer, the one witnessed class), so the decl and the signature agree.
                caseItem = literalItemTypeOrNull(sc.expression(), typeUtil);
            }
            if (!(caseItem instanceof JavaClass<?> caseItemClass)
                    || "Object".equals(caseItemClass.getSimpleName())) {
                return null;
            }
            if (itemType == null) {
                itemType = caseItemClass;
            } else if (!itemType.getCanonicalName().withDots()
                    .equals(caseItemClass.getCanonicalName().withDots())) {
                return null;
            }
            if (sc.isDefault()) {
                defaultExpr = caseExpr;
                defaultNode = sc.expression();
                continue;
            }
            RSwitchCaseGuard guard = sc.guard().orElse(null);
            String guardStr = guard == null ? null : HandlerHelper.literalGuardMapper(guard, refs);
            if (guardStr == null) {
                return null;
            }
            guardStrs.add(guardStr);
            caseExprs.add(caseExpr);
            caseNodes.add(sc.expression());
        }
        if (itemType == null || guardStrs.isEmpty()) {
            return null;
        }
        String sentinel = sink.statementHoistSession().register("ifThenElseResult");
        StringBuilder block = new StringBuilder();
        // the decl's element is the simple name: itemType is Integer at every admitted case (the
        // predicate's int-literal result term), so the #389 import-collision qualification the alias
        // ladder threads (ladderElementCollisionQualify) is not reached here - a java.lang class.
        block.append("final MapperS<").append(itemType.getSimpleName()).append("> ")
                .append(sentinel).append(";");
        block.append("\nif (").append(subject).append(".get() == null) {");
        block.append("\n\t").append(sentinel).append(" = MapperS.<")
                .append(itemType.getSimpleName()).append(">ofNull();");
        for (int i = 0; i < guardStrs.size(); i++) {
            refs.addAll(caseExprs.get(i).getRefs());
            wildcards.addAll(caseExprs.get(i).getStaticWildcardImports());
            block.append("\n} else if (areEqual(").append(subject).append(", ")
                    .append(guardStrs.get(i)).append(", CardinalityOperator.All).get()) {");
            block.append("\n\t").append(sentinel).append(" = ")
                    .append(wrapDeepThenIteArm(caseExprs.get(i), caseNodes.get(i), false,
                            itemType, typeUtil, refs).replace("\n", "\n\t"))
                    .append(";");
        }
        block.append("\n} else {");
        if (defaultExpr != null) {
            refs.addAll(defaultExpr.getRefs());
            wildcards.addAll(defaultExpr.getStaticWildcardImports());
            block.append("\n\t").append(sentinel).append(" = ")
                    .append(wrapDeepThenIteArm(defaultExpr, defaultNode, false, itemType,
                            typeUtil, refs).replace("\n", "\n\t"))
                    .append(";");
        } else {
            block.append("\n\t").append(sentinel).append(" = MapperS.<")
                    .append(itemType.getSimpleName()).append(">ofNull();");
        }
        block.append("\n}");
        refs.add(HandlerHelper.MAPPER_S);
        refs.add(HandlerHelper.CARDINALITY_OPERATOR);
        refs.add(itemType);
        wildcards.add(HandlerHelper.EXPRESSION_OPERATORS_NULL_SAFE);
        sink.registerStatementHoist(block.toString());
        handshake.markFired();
        return JavaExpression.from(sentinel, typeUtil.wrap(typeUtil.MAPPER_S, itemType), refs,
                wildcards);
    }

    /**
     * facet deepThenCtlRestructure (PR #351): the deep-seat then-LEVEL conditional hoist —
     * golden's {@code final Mapper*<X> ifThenElseResult; if (…) { ifThenElseResult = <arm>; }
     * else { … }} block registered on the statement channel, rendered as the bare sentinel
     * the NEXT then-level (or the consumer) re-roots on. Fired ONLY for the exact node the
     * deep seat blessed via {@link JavaStatementScope.DeepThenIteHoist} (the #327/#339/#340
     * single-slot handshake pattern); everything else keeps its existing route. Arm laws
     * (the #350-F2 mapperFormArms lineage): arms compile INTERIOR and KEEP their Mapper
     * form; a raw (non-Mapper) arm wraps {@code MapperS.of(…)} single /
     * {@code MapperC.<X>of(…)} multi; an absent else takes the typed
     * {@code Mapper*.<X>ofNull()}. The sentinel registers on the session's independent
     * {@code ifThenElseResult} group (bare when the method has one, numbered when several —
     * the #177 multi-group law). Declines (null) leave the deep seat to reject the WHOLE
     * chain — a partially-restructured chain is the #232/#350-F5b regression class.
     */
    private JavaStatementBuilder hoistAsDeepThenMapperLocalOrNull(RConditionalExpr expr,
            ExpressionContext ctx, ExpressionCompiler compiler) {
        JavaStatementScope.DeepThenIteHoist handshake = ctx.scope().findDeepThenIteHoistCond();
        if (handshake == null || handshake.cond() != expr) {
            return null;
        }
        // facet lambdaCondBaseThenArg (PR #374): the LAMBDA route has no statement-hoist
        // sink by construction (the lambda boundary) — the block registers through
        // registerPendingLambdaHoist below, and the forcedName is the caller's pre-created
        // deferred token, so no session use happens on this route.
        JavaStatementScope sink = ctx.scope().findStatementHoistSink();
        if (!handshake.lambdaRoute()
                && (sink == null || sink.statementHoistSession() == null)) {
            return null;
        }
        JavaTypeUtil typeUtil = compiler.getTypeUtil();
        var typeTranslator = compiler.getTypeTranslator();
        GeneratorModel gm = compiler.getGeneratorModel();
        if (typeUtil == null || typeTranslator == null || gm == null) {
            return null;
        }
        boolean multi = handshake.multi();
        String mapperSimple = multi ? "MapperC" : "MapperS";
        Set<JavaClass<?>> refs = new HashSet<>();
        Set<JavaClass<?>> wildcards = new HashSet<>();
        List<JavaExpression> condExprs = new ArrayList<>();
        List<RExpression> condNodes = new ArrayList<>();
        List<JavaExpression> armExprs = new ArrayList<>();
        List<RExpression> armNodes = new ArrayList<>();
        // facet inLambdaCondBaseArmChainDecomp (PR #381, D4b): per-arm decl prefixes —
        // an arm whose ROOT is an admitted nested then-chain hoists its levels through
        // the in-lambda channel, and those DeepThenArgHoist decls render INSIDE the
        // owning branch above the assignment (golden iosco cde v3
        // UnderlyingAssetTradingPlatformIdentifierLeg1/2's `_thenArg0.._thenArg2`).
        List<String> armHoistPrefixes = new ArrayList<>();
        // facet fnNotionalTogetherRestructure (PR #398, arm c): ONE-LEVEL nested-THEN
        // rungs — the inner conditional renders as an if/else INSIDE the owning rung
        // (the #385 nestedThenIteLadder shape at this deep seat; NotionalLeg
        // commodityOptionNotional's `else if averagingStrikeFeature exists then if
        // lastAvailableSpotPrice exists then Measure {…} else <chain>`). Parallel by
        // rung index: a null innerConds entry = a flat rung — every pre-#398 carrier
        // takes the null path byte-identically. armExprs/armNodes carry the INNER THEN
        // for a nested rung so the join/deref/wrap passes see the real arms.
        List<JavaExpression> innerConds = new ArrayList<>();
        List<JavaExpression> innerElses = new ArrayList<>();
        List<RExpression> innerElseNodes = new ArrayList<>();
        List<String> innerElsePrefixes = new ArrayList<>();
        String elseHoistPrefix = "";
        JavaExpression elseArmExpr = null;
        boolean hasRealElse = false;
        RConditionalExpr cur = expr;
        while (true) {
            JavaStatementBuilder condCompiled =
                    compiler.compile(cur.condition(), null, ctx.scope());
            List<JavaStatement> priorThenPendings = ctx.scope().drainPendingLambdaHoists();
            JavaStatementBuilder thenCompiled;
            String thenArmPrefix;
            RExpression rungArmNode = cur.thenBranch();
            JavaExpression rungInnerCond = null;
            JavaExpression rungInnerElse = null;
            RExpression rungInnerElseNode = null;
            String rungInnerElsePrefix = "";
            if (cur.thenBranch() instanceof RConditionalExpr innerCond
                    && !(innerCond.thenBranch() instanceof RConditionalExpr)
                    && innerCond.elseBranch().isPresent()
                    && !(innerCond.elseBranch().get() instanceof RConditionalExpr)) {
                JavaStatementBuilder innerCondCompiled =
                        compiler.compile(innerCond.condition(), null, ctx.scope());
                List<JavaStatement> innerPriorThen = new ArrayList<>(priorThenPendings);
                innerPriorThen.addAll(ctx.scope().drainPendingLambdaHoists());
                int innerThenStmtMark = sink == null ? -1 : sink.statementHoistMark();
                thenCompiled = compiler.compileInterior(innerCond.thenBranch(), null, ctx.scope());
                thenArmPrefix = drainArmChainDeclsOrReRegister(innerCond.thenBranch(),
                        innerPriorThen, ctx, refs, wildcards);
                thenArmPrefix = relocatedStatementHoistPrefix(sink, innerThenStmtMark)
                        + thenArmPrefix;
                List<JavaStatement> innerPriorElse = ctx.scope().drainPendingLambdaHoists();
                int innerElseStmtMark = sink == null ? -1 : sink.statementHoistMark();
                JavaStatementBuilder innerElseCompiled = compiler.compileInterior(
                        innerCond.elseBranch().get(), null, ctx.scope());
                rungInnerElsePrefix = drainArmChainDeclsOrReRegister(
                        innerCond.elseBranch().get(), innerPriorElse, ctx, refs, wildcards);
                rungInnerElsePrefix = relocatedStatementHoistPrefix(sink, innerElseStmtMark)
                        + rungInnerElsePrefix;
                if (!(innerCondCompiled instanceof JavaExpression icc)
                        || !(innerElseCompiled instanceof JavaExpression iec)) {
                    return null;
                }
                rungInnerCond = icc;
                rungInnerElse = iec;
                rungInnerElseNode = innerCond.elseBranch().get();
                rungArmNode = innerCond.thenBranch();
                refs.addAll(icc.getRefs());
                wildcards.addAll(icc.getStaticWildcardImports());
                refs.addAll(iec.getRefs());
                wildcards.addAll(iec.getStaticWildcardImports());
            } else {
                int flatStmtMark = sink == null ? -1 : sink.statementHoistMark();
                thenCompiled = compiler.compileInterior(cur.thenBranch(), null, ctx.scope());
                thenArmPrefix = drainArmChainDeclsOrReRegister(cur.thenBranch(),
                        priorThenPendings, ctx, refs, wildcards);
                thenArmPrefix = relocatedStatementHoistPrefix(sink, flatStmtMark)
                        + thenArmPrefix;
            }
            if (!(condCompiled instanceof JavaExpression condExpr)
                    || !(thenCompiled instanceof JavaExpression thenExpr)) {
                return null;
            }
            // facet deepThenLambdaNestedBoolHoist (PR #393): condition refs accumulate in
            // the pre-build pass below (per-rung — a restructured rung contributes its
            // UNWRAPPED inner's refs only, the #217 law), not eagerly here.
            refs.addAll(thenExpr.getRefs());
            wildcards.addAll(thenExpr.getStaticWildcardImports());
            condExprs.add(condExpr);
            condNodes.add(cur.condition());
            armExprs.add(thenExpr);
            armNodes.add(rungArmNode);
            armHoistPrefixes.add(thenArmPrefix);
            innerConds.add(rungInnerCond);
            innerElses.add(rungInnerElse);
            innerElseNodes.add(rungInnerElseNode);
            innerElsePrefixes.add(rungInnerElsePrefix);
            RExpression els = cur.elseBranch().orElse(null);
            // facet deepThenIteEmptyElse (PR #371, F-E2): the parser MATERIALIZES an
            // absent else as an empty list literal (P358I) — treat it exactly like the
            // absent else so the typed `Mapper*.<T>ofNull()` terminal renders (golden
            // Create_ValuationDetailsFromReportableEvent `thenArg = MapperC.
            // <TradeIdentifier>ofNull();`); compiling it rendered the witness-less
            // `MapperC.of()` (a bare empty against the typed decl — non-compiling at the
            // MapperS seat and witness-diverged at the MapperC seat, zero green carriers).
            if (els == null || els instanceof REmptyLiteral || isEmptyListLiteral(els)) {
                break;
            }
            if (els instanceof RConditionalExpr nested) {
                cur = nested;
                continue;
            }
            List<JavaStatement> priorElsePendings = ctx.scope().drainPendingLambdaHoists();
            int elseStmtMark = sink == null ? -1 : sink.statementHoistMark();
            JavaStatementBuilder elseCompiled = compiler.compileInterior(els, null, ctx.scope());
            elseHoistPrefix = drainArmChainDeclsOrReRegister(els, priorElsePendings,
                    ctx, refs, wildcards);
            elseHoistPrefix = relocatedStatementHoistPrefix(sink, elseStmtMark)
                    + elseHoistPrefix;
            if (!(elseCompiled instanceof JavaExpression elseExpr)) {
                return null;
            }
            refs.addAll(elseExpr.getRefs());
            wildcards.addAll(elseExpr.getStaticWildcardImports());
            elseArmExpr = elseExpr;
            armNodes.add(els);
            hasRealElse = true;
            break;
        }
        // The ladder's ITEM type: the snapshot when it resolves; else the RENDER-TRUTH
        // recovery from the compiled arms (the #204 law's twin — the workspace snapshot
        // reports MISSING/Object for the StandardizedSchedule* conditionals while the
        // arms' compiled Mapper types carry the concrete BigDecimal element).
        JavaClass<?> itemType = typeTranslator.toJavaReferenceType(
                gm.workspace().getInferredType(expr).type());
        if ("Object".equals(itemType.getSimpleName())) {
            List<JavaExpression> allArms = new ArrayList<>(armExprs);
            if (elseArmExpr != null) {
                allArms.add(elseArmExpr);
            }
            for (JavaExpression arm : allArms) {
                JavaType armType = arm.getExpressionType();
                JavaType armItem = armType == null ? null : typeUtil.getItemType(armType);
                if (armItem instanceof JavaClass<?> armItemClass
                        && !"Object".equals(armItemClass.getSimpleName())) {
                    itemType = armItemClass;
                    break;
                }
            }
        }
        if ("Object".equals(itemType.getSimpleName())) {
            // Second recovery: the #238 gm-aware AST walk over the arm nodes (a nav arm
            // resolves its element-producing leaf even when the compiled expression
            // carries no type — the MapperMaths arith arms stamp null).
            for (RExpression armNode : armNodes) {
                RType walked = NavigationHandler.recoverThenArgItemRType(armNode, compiler);
                JavaClass<?> recovered = walked == null ? null
                        : typeTranslator.toJavaReferenceType(walked);
                if (recovered != null && !"Object".equals(recovered.getSimpleName())) {
                    itemType = recovered;
                    break;
                }
            }
        }
        // facet fnNotionalTogetherRestructure (PR #398): the per-arm RTYPE walk — a
        // ctor arm contributes its CONSTRUCTED type (the workspace snapshot), a
        // nav/chain arm its recovered element — folded pairwise at the nearest common
        // EXTENDS supertype (the #396 join law at this ladder seat). Feeds (a) the
        // elseless-ctor-ladder Object recovery (ctor arms carry no compiled type, so
        // both recoveries above miss — the commodityOptionNotional probe-decline
        // class) and (b) the WILDCARD-decl discriminator: a PROPER-supertype join
        // (arms disagree) decls `Mapper*<? extends J>` with the bare `<J>` ofNull
        // witness (golden NotionalLeg: `final MapperC<? extends MeasureBase>
        // thenArg0; … thenArg0 = MapperC.<MeasureBase>ofNull();`). An unresolvable
        // or non-data-type arm aborts the walk — every pre-#398 carrier keeps its
        // agreed/aborted read (no wildcard, today's bytes).
        boolean lubJoin = false;
        boolean declWildcardOnly = false;
        {
            List<RExpression> lubNodes = new ArrayList<>(armNodes);
            for (RExpression ien : innerElseNodes) {
                if (ien != null) {
                    lubNodes.add(ien);
                }
            }
            // The fold itself now lives at
            // FunctionAliasHelper.properSupertypeJoinOrNull (v3.1 flip seat 33, law D.12):
            // FunctionExpressionRenderer's ite-hoist DECL seat needs the SAME pairwise
            // EXTENDS-supertype fold, and LAW 69 says the two seats consult ONE walk rather
            // than re-state it. A PURE extraction - the helper returns the join under exactly
            // this seat's old verdict (walkOk && folded != null && anyDisagree) and null
            // otherwise, so no byte here can move. The NODE COLLECTION deliberately stays:
            // this seat's arm list is COMPILE-driven (armNodes + innerElseNodes, built by the
            // walk above); the renderer's is an AST-only ladder walk. Only the fold is shared.
            com.regnosys.rosetta.generator.java.function.FunctionAliasHelper.LadderArmsFold
                    armsFold = com.regnosys.rosetta.generator.java.function.FunctionAliasHelper
                            .foldLadderArms(lubNodes, gm, compiler);
            // The seat-31 aliasSigWildcardDecl arm below still reads the walk's two flags
            // (`!anyDisagree && walkOk` = every arm resolved AND all agree), so the helper
            // hands back the FULL verdict, not just the join - the first GREEN run of this
            // law caught the join-only extraction as a compile error at that read.
            boolean anyDisagree = armsFold.anyDisagree();
            boolean walkOk = armsFold.walkOk();
            RType folded = armsFold.properSupertypeJoinOrNull();
            if (folded != null) {
                JavaClass<?> foldedJ = typeTranslator.toJavaReferenceType(folded);
                if (foldedJ != null && !"Object".equals(foldedJ.getSimpleName())) {
                    itemType = foldedJ;
                    lubJoin = true;
                }
            }
            // facet aliasSigWildcardDecl at the deep-then seat (v3.1 flip seat 31, law 2b):
            // AGREEING arms can still force the wildcard DECL when every arm is a BARE call
            // to a wildcard-signed ALIAS whose signature element IS the item type - the arm
            // assigns Mapper*<? extends T>, so the invariant decl is an incompatible-types
            // assignment (LAW 74; golden drr 7.x UnderlierProductIdentifier
            // `final MapperC<? extends ReferenceObligation> thenArg0;` under all-agreeing
            // ReferenceObligation alias arms). The verdict is the SAME
            // aliasSignatureWildcardItemType walk FER's E4 ladder decl uses (LAW 69, one
            // walk two seats) - and it is DECL-ONLY: `lubJoin` itself stays false, so the
            // arm pipeline's lub gates (the deref value-type arm, the wrap passes) keep
            // today's bytes; only the declared local widens to what its arms already assign.
            // FEATURE-CALL arms rooted at a wildcard alias (the StrikePrice half) are
            // DECLINED here: the [P31-F9LADDER] RFeatureCall population includes green
            // ladders with invariant golden decls and no measured discriminator - that half
            // is BANKED behind its own probe.
            if (!lubJoin && !anyDisagree && walkOk && !lubNodes.isEmpty()) {
                boolean allWildcardAliasArms = true;
                for (RExpression lubNode : lubNodes) {
                    JavaClass<?> sigElem =
                            NavigationHandler.aliasSignatureWildcardItemType(lubNode, compiler);
                    if (sigElem == null || sigElem instanceof RJavaWithMetaValue
                            || !sigElem.getCanonicalName().withDots()
                                    .equals(itemType.getCanonicalName().withDots())) {
                        allWildcardAliasArms = false;
                        break;
                    }
                }
                if (allWildcardAliasArms) {
                    declWildcardOnly = true;
                }
            }
        }
        if ("Object".equals(itemType.getSimpleName())) {
            return null;
        }
        // facet inLambdaCondBaseArmChainDecomp (PR #381, D4b): the meta-blind snapshot
        // STRIPS a wrapper join — when EVERY compiled arm (including a real else)
        // carries the SAME RJavaWithMetaValue element whose value type equals the
        // snapshot join, the local keeps the WRAPPER (the #144/#269/#334
        // meta-preserving join law at the handshake seat; golden iosco cde v3
        // UnderlyingAssetTradingPlatformIdentifierLeg1/2: `final MapperC<FieldWith-
        // MetaString> thenArg3` with BOTH arms wrapper-typed). The #364 deref pass
        // below then no-ops (armItem == join). A bare arm anywhere — or an elseless
        // ladder (no carrier; the typed-ofNull terminal keeps its stripped join) —
        // keeps the stripped join and today's deref behavior.
        if (hasRealElse) {
            RJavaWithMetaValue wrapperJoin = null;
            boolean allWrapperArms = true;
            List<JavaExpression> joinArms = new ArrayList<>(armExprs);
            joinArms.add(elseArmExpr);
            for (JavaExpression joinArm : joinArms) {
                JavaType joinArmType = joinArm.getExpressionType();
                JavaType joinArmItem = joinArmType == null ? null
                        : typeUtil.getItemType(joinArmType);
                if (joinArmItem instanceof RJavaWithMetaValue armWrap
                        && itemType.equals(armWrap.getValueType())
                        && (wrapperJoin == null || wrapperJoin.equals(armWrap))) {
                    wrapperJoin = armWrap;
                } else {
                    allWrapperArms = false;
                    break;
                }
            }
            if (allWrapperArms && wrapperJoin != null) {
                itemType = wrapperJoin;
            }
        }
        // facet elselessLadderWrapperJoin (PR #398): the #381 wrapper-preserving join
        // extends to the ELSELESS ladder — the very shape the #381 javadoc left with the
        // stripped join for want of a carrier. When EVERY compiled arm carries the SAME
        // RJavaWithMetaValue element whose value type equals the snapshot join — with the
        // #354 recoverExprMetaWrapper recovery for a TYPE-LESS arm (a mapItem/extract
        // consumer compiles unstamped; its then-chain terminal still proves the wrapper) —
        // the local AND the typed-ofNull terminal keep the WRAPPER, the #364 deref pass
        // below no-ops, and the deref moves to the re-rooted CONSUMER (the #397 jfsa
        // wrapper-decl law at this ladder seat; golden drr csa DTCC_UnderlyingAssetReport-
        // Rule: `final MapperC<FieldWithMetaString> ifThenElseResult; … ifThenElseResult =
        // thenArg3.<FieldWithMetaString>mapC("getEntityId", …); } else { ifThenElseResult
        // = MapperC.<FieldWithMetaString>ofNull(); }`). A bare arm anywhere or an
        // unrecoverable type-less arm keeps the stripped join and today's bytes.
        if (!hasRealElse) {
            RJavaWithMetaValue wrapperJoin = null;
            boolean allWrapperArms = !armExprs.isEmpty();
            for (int i = 0; i < armExprs.size() && allWrapperArms; i++) {
                JavaType joinArmType = armExprs.get(i).getExpressionType();
                JavaType joinArmItem = joinArmType == null ? null
                        : typeUtil.getItemType(joinArmType);
                RJavaWithMetaValue armWrap;
                if (joinArmItem instanceof RJavaWithMetaValue typedWrap) {
                    armWrap = typedWrap;
                } else if (joinArmType == null) {
                    armWrap = NavigationHandler.recoverExprMetaWrapper(armNodes.get(i), compiler);
                } else {
                    armWrap = null;
                }
                if (armWrap != null && itemType.equals(armWrap.getValueType())
                        && (wrapperJoin == null || wrapperJoin.equals(armWrap))) {
                    wrapperJoin = armWrap;
                } else {
                    allWrapperArms = false;
                }
            }
            // facet fnNotionalTogetherRestructure (PR #398): a nested rung's inner
            // ELSE joins like any other arm.
            for (int i = 0; i < innerElses.size() && allWrapperArms; i++) {
                if (innerElses.get(i) == null) {
                    continue;
                }
                JavaType ieType = innerElses.get(i).getExpressionType();
                JavaType ieItem = ieType == null ? null : typeUtil.getItemType(ieType);
                RJavaWithMetaValue ieWrap = ieItem instanceof RJavaWithMetaValue tw ? tw
                        : ieType == null ? NavigationHandler.recoverExprMetaWrapper(
                                innerElseNodes.get(i), compiler)
                        : null;
                if (ieWrap != null && itemType.equals(ieWrap.getValueType())
                        && (wrapperJoin == null || wrapperJoin.equals(ieWrap))) {
                    wrapperJoin = ieWrap;
                } else {
                    allWrapperArms = false;
                }
            }
            if (allWrapperArms && wrapperJoin != null) {
                itemType = wrapperJoin;
            }
        }
        // facet mapperCIteLift (PR #364): an arm whose Mapper ELEMENT is the meta
        // wrapper of the join item type derefs to the join — the MapperC receiver
        // takes upstream's BARE element map (no null guard, the #320 MapperC law;
        // golden GetReportTrackingNumber `thenArg2.<String>map("Type coercion",
        // fieldWithMetaString -> fieldWithMetaString.getValue())`). The standard
        // coerceNavigationReceiver route (the #354 arm-deref pattern) — a no-op on
        // every non-meta / already-joined arm.
        for (int i = 0; i < armExprs.size(); i++) {
            armExprs.set(i, derefDeepThenIteArmOrKeep(
                    armExprs.get(i), itemType, lubJoin, ctx, compiler, typeUtil, refs));
        }
        if (elseArmExpr != null) {
            elseArmExpr = derefDeepThenIteArmOrKeep(
                    elseArmExpr, itemType, lubJoin, ctx, compiler, typeUtil, refs);
        }
        // facet fnNotionalTogetherRestructure (PR #398, arm c): a nested rung's inner
        // ELSE derefs like any other arm.
        for (int i = 0; i < innerElses.size(); i++) {
            if (innerElses.get(i) != null) {
                innerElses.set(i, derefDeepThenIteArmOrKeep(
                        innerElses.get(i), itemType, lubJoin, ctx, compiler, typeUtil, refs));
            }
        }
        // facet deepThenLambdaNestedBoolHoist (PR #393): a NESTED-ELSE rung whose
        // condition is a bare fn/rule call (HandlerHelper.isBareFunctionCallCondition +
        // the structural MapperS.of invocation-wrap witness — the same gate pairing as
        // renderConditionalAssignment/appendAddDistributionBoolHoistElse) takes the #179
        // nested-else boolHoist restructure at this seat: golden (drr iosco cde v1
        // ExpirationDateRule, the only carrier) DEEPENS the ladder remainder one level —
        // `} else {\n\tfinal Boolean _boolean = <bare call>;\n\tif ((_boolean == null ?
        // false : _boolean)) { … } else if (…) { … } else { … }\n}` — instead of
        // flattening to `} else if (`. LAMBDA-route-scoped (the per-lambda `_boolean`
        // singleton law, #179/#281; the statement route keeps the flat form — zero
        // carriers, its naming would be the session boolean group) and SINGLETON-scoped
        // (>1 bare-fn rung would need the boolean0..n-1 numbering — zero carriers at
        // this seat; the flat form keeps today's bytes). The TOP rung (i == 0) never
        // restructures (no enclosing else to deepen into — the #390 admissibility law).
        boolean[] boolHoistRung = new boolean[condExprs.size()];
        JavaStatementBuilder[] boolHoistInner = new JavaStatementBuilder[condExprs.size()];
        int boolHoistCount = 0;
        // facet bareFnCondHoistEveryContext (seat 24, law F4): the TOP rung (i == 0) joins the
        // bare-fn census — on the lambda route it hoists `final Boolean _boolean = <call>;` ABOVE
        // the then-arg declaration and guards the top header (the #217 top-condition placement;
        // golden asic/cftc NotionalAmountLeg1/2, NotionalCurrencyLeg1/2, esma/fca NotionalAmountOf*,
        // NotionalCurrency1/2, mas NotionalAmountOf* — the seat-24 LAW-75 probe's 22 ladders, every
        // one `bareRungs=1`). A witnessed MapperS.of wrap takes its inner exactly as the rungs do;
        // an UN-witnessed compile — the in-lambda rule-path bare Boolean, the probe's `witness=false
        // type=null` — IS the value (render truth: `.getOrDefault(false)` on it never compiled,
        // LAW 74). The singleton law below now counts EVERY bare-fn rung, witnessed or not: a second
        // one anywhere in the ladder resets every restructure (the per-lambda `_boolean` singleton;
        // no corpus ladder carries two — the probe's `bareRungs=1` — the suite's b2 pins it). The
        // STATEMENT route has no carrier (every probed real compile is `lambdaRoute=true`) and
        // keeps its bytes — a stated decline.
        boolean topBoolHoist = false;
        if (handshake.lambdaRoute()) {
            int bareRungs = 0;
            for (int i = 0; i < condExprs.size(); i++) {
                if (!HandlerHelper.isBareFunctionCallCondition(condNodes.get(i))) {
                    continue;
                }
                bareRungs++;
                JavaStatementBuilder inner = condExprs.get(i).unwrapToBuilder().orElse(null);
                if (inner == null && i == 0) {
                    inner = condExprs.get(0);
                }
                if (inner != null) {
                    boolHoistRung[i] = true;
                    boolHoistInner[i] = inner;
                    boolHoistCount++;
                }
            }
            // the singleton law counts EVERY bare-fn rung, witnessed or not (a second one anywhere
            // suppresses the restructure everywhere — the per-lambda `_boolean` singleton).
            if (bareRungs > 1) {
                for (int i = 0; i < boolHoistRung.length; i++) {
                    boolHoistRung[i] = false;
                    boolHoistInner[i] = null;
                }
                boolHoistCount = 0;
            }
            topBoolHoist = boolHoistRung.length > 0 && boolHoistRung[0];
        }
        // Per-rung condition refs: a flat rung contributes its compiled wrapper's refs;
        // a restructured rung the UNWRAPPED inner's only (the #217 alias-ladder boolHoist
        // law — the discarded MapperS.of wrap must not pin a stale import).
        for (int i = 0; i < condExprs.size(); i++) {
            JavaStatementBuilder condRefSrc = boolHoistRung[i]
                    ? boolHoistInner[i] : condExprs.get(i);
            refs.addAll(condRefSrc.getRefs());
            wildcards.addAll(condRefSrc.getStaticWildcardImports());
        }
        List<String[]> armLevels = new ArrayList<>();
        // facet fnNotionalTogetherRestructure (PR #398, arm c): the nested rung's inner
        // else wraps/derefs through the SAME arm pipeline (null for flat rungs).
        List<String> innerElseArmStrs = new ArrayList<>();
        for (int i = 0; i < armExprs.size(); i++) {
            armLevels.add(new String[] {
                    HandlerHelper.render(condExprs.get(i)),
                    wrapDeepThenIteArm(armExprs.get(i), armNodes.get(i), multi, itemType,
                            typeUtil, refs) });
            innerElseArmStrs.add(innerElses.get(i) == null ? null
                    : wrapDeepThenIteArm(innerElses.get(i), innerElseNodes.get(i), multi,
                            itemType, typeUtil, refs));
        }
        String elseStr = hasRealElse
                ? wrapDeepThenIteArm(elseArmExpr, armNodes.get(armNodes.size() - 1), multi,
                        itemType, typeUtil, refs)
                : mapperSimple + ".<" + itemType.getSimpleName() + ">ofNull()";
        // facet deepThenCtlRestructure (PR #351, i3): a forcedName (the base-thenArg
        // mode) names the local exactly and skips the ifThenElseResult group — the base
        // hoist IS the thenArg decl (the #316 forcedName pattern at the deep seat).
        // facet ruleMidChainCondLambdaAdmit (PR #392): a lambda-route handshake with NO
        // forcedName creates its per-scope deferred ifThenElseResult token HERE — after
        // the arm walk above, so an arm-interior nested chain's own ite numbered FIRST
        // (the #327 consumption law; golden UAPSL1 ifThenElseResult0 inner /
        // ifThenElseResult1 outer; rule-path naming is per-scope literal — the #383 law).
        String sentinel;
        if (handshake.forcedName() != null) {
            sentinel = handshake.forcedName();
        } else if (handshake.lambdaRoute()) {
            sentinel = ctx.scope().registerDeferredCoercionName(
                    ctx.scope().createUniqueIdentifier("ifThenElseResult"));
        } else {
            sentinel = sink.statementHoistSession().register("ifThenElseResult");
        }
        StringBuilder block = new StringBuilder();
        // facet bareFnCondHoistEveryContext (seat 24, law F4): the TOP rung's Boolean hoist is
        // the line ABOVE the then-arg declaration (golden `final Boolean _boolean = …;` then
        // `final MapperS<TradableProduct> _thenArg0;`), at the block's own depth.
        if (topBoolHoist) {
            block.append("final Boolean _boolean = ")
                    .append(HandlerHelper.render(boolHoistInner[0])).append(";\n");
        }
        // facet fnNotionalTogetherRestructure (PR #398): a LUB-joined ladder (arms are
        // PROPER subtypes of the join) decls the wildcard `Mapper*<? extends J>`; the
        // ofNull terminal keeps the bare `<J>` witness (golden NotionalLeg).
        block.append("final ").append(mapperSimple).append("<")
                .append(lubJoin || declWildcardOnly ? "? extends " : "")
                .append(itemType.getSimpleName()).append("> ").append(sentinel).append(";");
        // facet deepThenIteArmContinuation (PR #371, F-E1): a multi-line arm's embedded
        // CHAIN_LINK continuations re-anchor ONE level below the arm's own assignment
        // line (the block-relative `\t` + 1 = `\t\t`) — golden's uniform assignment+1
        // convention at this seat (Create_ValuationDetailsFromReportableEvent
        // `\t\t\t\tthenArg = MapperS.of(…)` / `\t\t\t\t\t.filterItemNullSafe(…)`; the
        // corpus scan reads 4/5, 5/6, 6/7 with zero exceptions). Green-safe by the
        // convention's uniformity: a fired-and-green carrier with a multi-line arm
        // would already need the +1 form, so only single-line-arm greens exist (the
        // replace is a no-op on them).
        // facet deepThenLambdaNestedBoolHoist (PR #393): extraDepth threads the #179
        // deepening — every line from a restructured rung onward re-anchors one level
        // deeper (block-relative; with zero boolHoist rungs every repeat/replace below
        // is the identity, keeping all pre-#393 carriers byte-frozen).
        int extraDepth = 0;
        for (int i = 0; i < armLevels.size(); i++) {
            String rungTabs = "\t".repeat(extraDepth);
            if (i == 0 && topBoolHoist) {
                // facet bareFnCondHoistEveryContext (seat 24, law F4): the top header is the
                // null-safe guard on the hoisted local — no `} else {` deepening (there is no
                // enclosing else; the #390 admissibility law).
                block.append("\n").append("if ((_boolean == null ? false : _boolean)) {");
            } else if (boolHoistRung[i]) {
                block.append("\n").append(rungTabs).append("} else {");
                extraDepth++;
                rungTabs = "\t".repeat(extraDepth);
                block.append("\n").append(rungTabs).append("final Boolean _boolean = ")
                        .append(HandlerHelper.render(boolHoistInner[i])
                                .replace("\n", "\n" + rungTabs)).append(";");
                block.append("\n").append(rungTabs)
                        .append("if ((_boolean == null ? false : _boolean)) {");
            } else {
                block.append("\n").append(rungTabs).append(i == 0 ? "if (" : "} else if (")
                        .append(armLevels.get(i)[0]).append(".getOrDefault(false)) {");
            }
            if (innerConds.get(i) != null) {
                // facet fnNotionalTogetherRestructure (PR #398, arm c): the ONE-LEVEL
                // nested-then rung — the inner if/else INSIDE the owning rung, arms one
                // level deeper than a flat rung's assignment (golden NotionalLeg
                // commodityOptionNotional: `} else if (…) {\n\tif (…) {\n\t\tthenArg0 =
                // MapperC.of(Collections.singletonList(…));\n\t} else {\n\t\tthenArg0 =
                // …;\n\t}\n}` block-relative).
                block.append("\n").append(rungTabs).append("\t").append("if (")
                        .append(HandlerHelper.render(innerConds.get(i)))
                        .append(".getOrDefault(false)) {")
                        .append(armHoistPrefixes.get(i).replace("\n", "\n" + rungTabs + "\t"))
                        .append("\n").append(rungTabs).append("\t\t").append(sentinel)
                        .append(" = ")
                        .append(armLevels.get(i)[1].replace("\n", "\n" + rungTabs + "\t\t"))
                        .append(";")
                        .append("\n").append(rungTabs).append("\t} else {")
                        .append(innerElsePrefixes.get(i).replace("\n", "\n" + rungTabs + "\t"))
                        .append("\n").append(rungTabs).append("\t\t").append(sentinel)
                        .append(" = ")
                        .append(innerElseArmStrs.get(i).replace("\n", "\n" + rungTabs + "\t\t"))
                        .append(";")
                        .append("\n").append(rungTabs).append("\t}");
                continue;
            }
            block.append(armHoistPrefixes.get(i).replace("\n", "\n" + rungTabs))
                    .append("\n").append(rungTabs).append("\t").append(sentinel).append(" = ")
                    .append(armLevels.get(i)[1].replace("\n", "\n" + rungTabs + "\t"))
                    .append(";");
        }
        String elseTabs = "\t".repeat(extraDepth);
        block.append("\n").append(elseTabs).append("} else {")
                .append(elseHoistPrefix.replace("\n", "\n" + elseTabs))
                .append("\n").append(elseTabs).append("\t").append(sentinel).append(" = ")
                .append(elseStr.replace("\n", "\n" + elseTabs + "\t")).append(";")
                .append("\n").append(elseTabs).append("}");
        for (int d = extraDepth; d >= 1; d--) {
            block.append("\n").append("\t".repeat(d - 1)).append("}");
        }
        refs.add(multi ? HandlerHelper.MAPPER_C : HandlerHelper.MAPPER_S);
        refs.add(itemType);
        if (handshake.lambdaRoute()) {
            // facet lambdaCondBaseThenArg (PR #374): the in-lambda block rides the LAMBDA
            // channel as a marker-classed statement — the owning block form drains it into
            // the branch (renderDrainedHoists re-anchors the multi-line block per line).
            ctx.scope().registerPendingLambdaHoist(new CollectionHandler.DeepThenCondArgHoist(
                    block.toString(), new HashSet<>(refs), new HashSet<>(wildcards)));
        } else {
            sink.registerStatementHoist(block.toString());
        }
        handshake.markFired();
        return JavaExpression.from(sentinel,
                multi ? typeUtil.wrap(typeUtil.MAPPER_C, itemType)
                        : typeUtil.wrap(typeUtil.MAPPER_S, itemType),
                refs, wildcards);
    }

    /**
     * facet inLambdaCondBaseArmChainDecomp (PR #381, D4b): segregate the pending
     * lambda hoists an ARM compile registered — an arm whose ROOT is an admitted
     * nested then-chain hoists its levels through the in-lambda channel, and those
     * {@link CollectionHandler.DeepThenArgHoist} decls belong INSIDE the owning
     * branch above the assignment (golden iosco cde v3
     * UnderlyingAssetTradingPlatformIdentifierLeg1/2), not at the lambda top. The
     * prior pendings re-register FIRST, then every non-chain-decl arm pending, both
     * in original order (the pre-#381 destination — arm compiles at this seat never
     * produced chain decls before the base-arm admission, so nothing existing
     * relocates; a non-chain arm's pendings all re-register). Relocated decls carry
     * their refs into the block's sets. The returned prefix is block-relative
     * (each line at {@code \n\t}, continuations one deeper — the #371 assignment+1
     * convention).
     */
    private String drainArmChainDeclsOrReRegister(RExpression armNode,
            List<JavaStatement> priorPendings, ExpressionContext ctx,
            Set<JavaClass<?>> refs, Set<JavaClass<?>> wildcards) {
        List<JavaStatement> armPendings = ctx.scope().drainPendingLambdaHoists();
        for (JavaStatement prior : priorPendings) {
            ctx.scope().registerPendingLambdaHoist(prior);
        }
        StringBuilder prefix = new StringBuilder();
        for (JavaStatement st : armPendings) {
            // facet ruleCondBaseEvalArgDeref (PR #388): an ARM-position
            // ItemGetMetaDerefHoist (the sentinel-decl the deepThenIteCondSeat
            // admission registers during THIS arm compile) belongs INSIDE the owning
            // branch above the assignment — golden csa CountryAndProvinceOrTerritory-
            // OfIndividualRule places `final ReferenceWithMetaParty
            // referenceWithMetaParty1 = item.get();` before the `thenArg =` assign.
            // Exactly scoped: at this seat no OTHER producer can register the class
            // (blockArmSeatConditionals is unregistered here, a filter predicate owns
            // its inner lambda's channel, and route BLOCK is impossible under an ite
            // arm), so every drained instance is the #388 admission's own.
            // facet ruleMidChainCondLambdaAdmit (PR #392): an arm-interior nested
            // chain's OWN ite block (DeepThenCondArgHoist — its mid/consumer cond rung
            // hoisted through the same lambda route) belongs INSIDE the owning branch
            // exactly like its DeepThenArgHoist siblings, in registration order (golden
            // UAPSL1's in-branch `final MapperC<ReportablePriceSource>
            // ifThenElseResult0; if …` between the thenArg1/thenArg2 decls).
            // facet fnNotionalTogetherRestructure (PR #398): an ARM-position
            // evaluate-arg meta-deref decl (ReferenceHandler.EvalArgMetaDerefHoist)
            // relocates INSIDE the owning branch above the assignment — the #362
            // groupPCounterpartyBlockRelocate law at this ladder seat (golden
            // NotionalLeg commodityNotional's in-branch `final ReferenceWithMeta
            // NonNegativeQuantitySchedule referenceWithMetaNonNegativeQuantity
            // Schedule0 = …get();` before the singletonList ctor assignment).
            if ((armNode instanceof RThenExpr
                        && (st instanceof CollectionHandler.DeepThenArgHoist
                                || st instanceof CollectionHandler.DeepThenCondArgHoist))
                    || st instanceof ReferenceHandler.ItemGetMetaDerefHoist
                    || st instanceof ReferenceHandler.EvalArgMetaDerefHoist) {
                StringBuilder line = new StringBuilder();
                st.render(line);
                while (line.length() > 0 && line.charAt(line.length() - 1) == '\n') {
                    line.setLength(line.length() - 1);
                }
                prefix.append("\n\t").append(line.toString().replace("\n", "\n\t"));
                refs.addAll(st.getRefs());
                wildcards.addAll(st.getStaticWildcardImports());
            } else {
                ctx.scope().registerPendingLambdaHoist(st);
            }
        }
        return prefix.toString();
    }

    /**
     * facet deepThenCtlRestructure (PR #351): arm-value form for the deep-then ite hoist —
     * a compiled arm whose expression type is already a Mapper wrapper renders as-is (the
     * interior compile kept the chain); a raw value wraps {@code MapperS.of(…)} single /
     * {@code MapperC.<X>of(…)} multi so the assignment compiles against the Mapper-typed
     * local.
     */
    private String wrapDeepThenIteArm(JavaExpression arm, RExpression armNode, boolean multi,
            JavaClass<?> itemType, JavaTypeUtil typeUtil, Set<JavaClass<?>> refs) {
        // facet mapperCIteLift (PR #364): a SINGLE wrap-factory literal arm at a MULTI
        // seat coerces item→list — upstream's singletonList coercion at the Mapper
        // level (golden GetReportTrackingNumber `ifThenElseResult =
        // MapperC.of(Collections.singletonList("RTNNotProvided"));` against the
        // MapperC<String> local). Gated on the recognizable MapperS-typed single wrap
        // (unwrapToBuilder present) so nav/extract/arith arms keep their form.
        // facet nullTypedIteLiteralArmLift (seat 16, PR #588): the SAME lift, for an arm whose
        // COMPILED EXPRESSION TYPE IS NULL. A literal carries no expression type, so
        // `isMapperS(getExpressionType())` can never be consulted and #364's gate skipped
        // exactly the shape its own javadoc names above — a string-literal arm.
        //
        // PRODUCER CONFIRMED BY RUNTIME PROBE (LAW 72), not by attribution: instrumenting this
        // method and generating drr 7.0.0 printed 117 arm sites, of which the csa
        // JurisdictionOfCounterparty1/2 carrier appears TWICE, verbatim —
        // `multi=true type=null isMapperS=false unwrap=true node=RStringLiteral
        // render=MapperS.of("NON-CA")`. Reached, correctly MULTI, recognisably a single wrap,
        // and failing ONLY the null check. (The seat's BANKED producer line was
        // `ReferenceHandler:3839-3857`, which is a different carrier set entirely — that site's
        // own javadoc names csa UATPI Leg1/2, the META-wrapper family. Fourth seat running whose
        // charted producer needed a probe; see target/seat16-charter.md §1.)
        //
        // ROOT CAUSE, one layer down and named there already: JavaExpression's 1-arg
        // `wrappedInMapperSOf(inner)` falls back to `inner.getExpressionType()` when no wrapper
        // type is supplied, and its own javadoc calls out LiteralHandler as the caller whose
        // inner type is null (see JavaExpression:166-169, and the recorded finding at :149-154
        // that callers who KNOW the wrapper type should use the 2-arg overload). All four
        // literal sites use the 1-arg form (LiteralHandler:122, :131, :285, :303, :326). The
        // producer fix is therefore known — but that type feeds coercion decisions across the
        // whole compiler, so re-typing every literal is a far wider change than this seat's
        // evidence supports. This arm patches the CONSUMER deliberately, and the producer fix is
        // banked rather than smuggled in. (LAW 69: the two halves are reconciled by the identity
        // test below, which needs no expression type at all.)
        //
        // WHY AN EXACT IDENTITY AND NOT A PREFIX TEST — and the reason is STRUCTURAL, not the
        // measurement an earlier revision of this comment cited. (That revision claimed a
        // `startsWith` test "would steal the eight RConstructorExpr arms"; the #588 independent
        // review REFUTED it: a ctor arm renders through ImportCollisionResolver.typeRef, so it
        // begins with the private-use sentinel U+E000, and `startsWith("MapperS.of(")` is false
        // for it exactly as `equals` is. Recorded because a safety argument that is wrong about
        // WHY it is safe is worth no more than luck.)
        //
        // The real argument is an enumeration. Only FOUR factories ever set unwrapToBuilder —
        // JavaExpression:177 wrappedInMapperSOf (renders `MapperS.of(`), :349 MapperCOfSingleWrap
        // (`MapperC.<`), :377 witnesslessForm (`MapperC.of(`) and :401 selfUnwrapping (renders
        // its inner VERBATIM). Only `selfUnwrapping` can render an arbitrary prefix, so it is the
        // only shape a prefix test could wrongly admit — and `equals` structurally cannot admit
        // it, because that would require render(inner) == "MapperS.of(" + render(inner) + ")".
        // Hence: the identity holds IFF the arm is a wrappedInMapperSOf wrap, where the unwrapped
        // inner IS that wrap's own inner by construction. That is cell-independent, so it does
        // not rest on a single-cell probe.
        //
        // NOT mirrored onto the sibling inLambdaCondBaseArmChainDecomp arm (PR #381), and the
        // reason is MEASURED rather than asserted: among null-typed MULTI arms whose
        // unwrapToBuilder is EMPTY, four render `MapperS.of(x).mapC(...)` — chains whose RESULT
        // is already MapperC, which a render-prefix test would wrap a second time. The
        // pre-existing unwrapToBuilder().isPresent() condition excludes them from this arm.
        //
        // Green-safe by the #339 argument, and it is the PREDICATE that carries it (never a
        // token — the #585 REFUTATION): where this fires, a MapperS single wrap is being
        // assigned to a MapperC-typed local, which never compiled, so no byte-identical file can
        // carry the pre-fix form at a site the gate accepts. The #588 review verified that over
        // the FULL population — all 174,141 goldens across 25 cells, paren-balanced — and found
        // ZERO green files carrying the pre-fix form at an accepted site.
        //
        // `multi` leads the conjunction deliberately: the identity test renders the arm, and at a
        // SINGLE seat that render would be pure cost on a population this seat never measured.
        // The unwrapped inner is rendered AT MOST ONCE and reused by both the predicate and the
        // emission (Copilot #588). `unwrapToBuilder()` itself is a field accessor
        // (JavaExpression:435), so only the RENDER is worth hoisting.
        String iteArmInner = null;
        boolean singleWrapArm = false;
        if (multi && arm.unwrapToBuilder().isPresent()) {
            iteArmInner = HandlerHelper.render(arm.unwrapToBuilder().get());
            singleWrapArm = arm.getExpressionType() != null
                    ? typeUtil.isMapperS(arm.getExpressionType())
                    : HandlerHelper.render(arm).equals("MapperS.of(" + iteArmInner + ")");
        }
        if (singleWrapArm) {
            refs.add(HandlerHelper.MAPPER_C);
            refs.add(HandlerHelper.COLLECTIONS);
            return "MapperC.of(Collections.singletonList(" + iteArmInner + "))";
        }
        // facet inLambdaCondBaseArmChainDecomp (PR #381, D4b): a MapperS-typed
        // NON-factory arm at a MULTI seat wraps `MapperC.of(…)` — the single-arm
        // item→list coercion at the Mapper level (golden iosco cde v3
        // UnderlyingAssetTradingPlatformIdentifierLeg1/2: `thenArg3 = MapperC.of(
        // _thenArg2\n\t.first());` against the MapperC<FieldWithMetaString> local).
        // A MapperS arm assigned to a MapperC local never compiled (generics
        // mismatch), so no green render changes (the #339 green-safe-by-construction
        // argument); wrap-factory arms took the singletonList arm above.
        if (multi && arm.getExpressionType() != null
                && typeUtil.isMapperS(arm.getExpressionType())
                && arm.unwrapToBuilder().isEmpty()) {
            refs.add(HandlerHelper.MAPPER_C);
            return "MapperC.of(" + HandlerHelper.render(arm) + ")";
        }
        // facet deepThenIteLadderArmJoinMulti (v3.1 flip seat 33, law A.5, rung 2 - the ARM
        // LIFT): a NULL-TYPED, contract-less NAV-CHAIN arm at a MULTI seat. The #381 rung above
        // is this exact case WITH a type; the only-element re-wrap `MapperS.of(<chain>.get())
        // .<X>map(...)` reaches this seat with NO expression type and NO unwrap contract, so
        // both lifts above decline and the arm rendered AS-IS - a MapperS assigned to the
        // MapperC local rung 1 now declares, which would never compile (LAW 74: javac33's PRE
        // errors 515/517 witness the file-level MapperC decl rung 1 introduces; rung 2's own arms
        // compile clean in Seat33Pre - rung 2 prevents the regression rung 1 alone would introduce).
        // GOLDEN <- FORK (drr 7.0-7.3 GetBasketConstituents `underliers`):
        // `ifThenElseResult = MapperC.of(MapperS.of(thenArg.<OptionPayout>map(...).get())
        // .<Underlier>map("getUnderlier", ...));` <- the fork's bare chain.
        //
        // MEASURED, not attributed (LAW 72 - the draft's "one flag, three signatures" claim
        // was WRONG: the flag moved the decl and the terminal, not the arms): the
        // [P33-A5ARM] runtime probe at this seat over drr 7.0.0 + 7.3.0 + the seat fixture
        // printed 238 arm rows; `multi=true type=- unwrap=false node=RFeatureCall` is 6 rows
        // = the carrier's four arms + the fixture's two, every one `card=SINGLE` and rooted at
        // `MapperS.of(`. The other MapperS.of-rooted null-typed MULTI arms are 8 RFilterExpr
        // rows (SchedulePeriod x4, TradeIdentifier x4) and the ladder's own two bare option
        // arms are REnumValueRef nodes rooted at the MapperC hoist - all excluded by the NODE
        // KIND, which is the discriminator; the AST cardinality and the render root are
        // defence-in-depth (a `.mapC(`-re-listed chain is MULTI by the computer). Green-safe
        // by the #339 argument on top: where this fires, a MapperS chain was being assigned
        // to a MapperC local, so no byte-identical file can carry the pre-fix form.
        if (multi && arm.getExpressionType() == null && arm.unwrapToBuilder().isEmpty()
                && armNode instanceof RFeatureCall
                && CARDINALITY.compute(armNode) != ExpressionCardinality.MULTI) {
            String navRendered = HandlerHelper.render(arm);
            // facet mapperWrapPrefix (v3.1 C2d retirement family 8, PR #615) — VERDICT-MOVED
            // RETIRE-AFTER-CENSUS -> JUSTIFIED-KEPT BY CONSTRUCTION: this seat has no marker and no
            // type LEFT to read, because its own precondition one line above has already
            // established `arm.getExpressionType() == null && arm.unwrapToBuilder().isEmpty()`, and
            // the two AST facts a swap could use are already conjuncts of the same `if`. The c8
            // census re-took the gate figures over 3,986 arrivals (1,339 / 1,339 / 1,308 —
            // default-route D11, IR-route D11, optimised): the rung-1 / #381 / rung-2 gates fire
            // 280 / 60 / 24, would-flip 0/24 and 0/60, the 24 all at `fn:GetBasketConstituents` —
            // the carrier this heal names. DISCLOSED BOUND: `prod=none` at 3,986/3,986, so
            // "would-flip = 0" means "no arrival carried one of the 28 registered producers", an
            // upper bound of zero over a set the instrument enumerates, not a proof over all
            // producers.
            if (navRendered.startsWith("MapperS.of(")) {
                refs.add(HandlerHelper.MAPPER_C);
                return "MapperC.of(" + navRendered + ")";
            }
        }
        // facet fnNotionalTogetherRestructure (PR #398): a CONSTRUCTOR arm compiles as
        // the RAW builder chain (never a Mapper) — at the MULTI seat it wraps the
        // item→list `MapperC.of(Collections.singletonList(<ctor>))` (golden NotionalLeg
        // `thenArg0 = MapperC.of(Collections.singletonList(Measure.builder()…build()));`
        // — the #397 cascade's nested-rung terminal form at this ladder seat), at the
        // single seat `MapperS.of(<ctor>)` (the #360 ctorArm law). Render-truth prefix
        // check per the #218 law; a raw builder against a Mapper local never compiled —
        // green-safe by construction.
        // facet iteChainCtorArmMapperWrap (v3.1 flip seat 32, law B.1): the #398 rung above
        // is now ONE shared static (HandlerHelper.wrapCtorIteArm), because BOTH arm
        // pipelines of FunctionExpressionRenderer.appendIteHoistChainCore declare the SAME
        // Mapper-typed local and carried no constructor rung at all (golden drr 7.0-7.3
        // FXLeg1/FXLeg2 `ifThenElseResult = MapperS.of(Cashflow.builder()...build());`
        // against the fork's bare `ifThenElseResult = Cashflow.builder()...build();`, in
        // the then arm AND in the terminal else of the same ladder).
        // LAW 69, one walk three call sites - the shape comparisonResultIteArmNeedsAsMapper
        // already established at these seats. THIS seat is BYTE-FROZEN: the helper carries
        // the same predicate in the same order and emits the same strings and the same
        // refs, and the kind test is kept HERE too so the walk is still entered only for a
        // constructor arm - the render cost the seat-16 note above weighs is unchanged. The
        // wrap can never equal its own input (it is strictly longer), so the equality below
        // is exactly the old `declined` branch.
        if (armNode instanceof RConstructorExpr) {
            String ctorRendered = HandlerHelper.render(arm);
            String ctorWrapped = HandlerHelper.wrapCtorIteArm(ctorRendered, armNode, multi,
                    refs);
            if (!ctorWrapped.equals(ctorRendered)) {
                return ctorWrapped;
            }
        }
        // Default BARE: an interior-compiled arm keeps its Mapper form — navs, extracts,
        // MapperMaths ariths, and wrap-factory literals all render as-is (the cp6b catch:
        // a null-typed MapperMaths arm double-wrapped under the old type-keyed default).
        // Wrap ONLY an INVOKABLE arm (RSymbolReference → RFunction/RRule — a fn call
        // compiles to the bare `fn.evaluate(…)` via renderImplicitFunctionInvocation;
        // rule calls come pre-wrapped and the unwrapToBuilder test keeps them bare) —
        // the #339/#333 dual wrap-factory law at the ite-arm seat.
        boolean invokableArm = armNode instanceof RSymbolReference bref
                && bref.symbol().filter(s -> s instanceof RFunction
                        || s instanceof com.regnosys.rosetta.ast.functions.RRule).isPresent();
        // facet inLambdaCondConsumerDecomp (PR #384): a bare enum-constant arm compiles to
        // the RAW `Enum.CONST` (never a Mapper), so at the Mapper-typed slot it wraps like
        // the invokable class (golden mas UnderlyingIdOtherSourceDTCC `ifThenElseResult =
        // MapperS.of(ProductIdTypeEnum.OTHER);`). A bare enum constant assigned to a Mapper
        // local never compiled — the #339 green-safety argument. The DISGUISED (unresolved,
        // enumeration-empty) evr class keeps the bare default: it never compiles to a constant.
        // facet enumConstantWitness (v3.1 C2d retirement family 4, PR #611): ONE read for the
        // two constant classes this seat used to tell apart by hand — the RESOLVED `Enum ->
        // Value` reference (`REnumValueRef.enumeration()` present) and the #204-class
        // bare-symbol arm "the enum recovery QUALIFIED" (`then Other` → `ProductIdTypeEnum
        // .OTHER`), which the retired inline twin recognised by its RENDER (a null-typed,
        // unwrap-free, no-args bare-symbol arm whose text was dotted, parenless and
        // single-line — the #371 render-truth arbiter). The producer's witness
        // (JavaExpression.enumConstant, read through HandlerHelper.isBareEnumConstant) is that
        // fact at its source. The PR #611 C2c census (both routes, every cell) found the twin
        // firing on exactly nine arrivals per route, all in regulation-mas-rewrite-trade-rule
        // .rosetta and every one a PARSER-BOUND bare value (`RSymbolReference.symbol()` = the
        // REnumValue, rendered by ReferenceHandler's bare-bound arm) — the triage's premise that
        // such a node "carries no resolved enum value" no longer holds since the v3.1 C1
        // resolution rebuild; the resolved-reference class had zero arrivals at this seat.
        String armRendered = HandlerHelper.render(arm);
        boolean enumConstantArm = HandlerHelper.isBareEnumConstant(arm);
        // facet ruleCondBaseEvalArgDeref (PR #388): a nav-only-element arm collapses to
        // the bare `.get()` form (`MapperS.of(<fn>.evaluate(…)).map(…).mapC(…).get()` —
        // its compiled value is the BARE item, never a Mapper), so at the Mapper-typed
        // slot it re-wraps `MapperS.of(…)` — the #243/#345 roundtrip law at the ite-arm
        // assign seat (golden csa CountryAndProvinceOrTerritoryOfIndividualRule
        // `thenArg = MapperS.of(MapperS.of(naturalPersonBuyerOrSeller.evaluate(…))…
        // .get());`). A bare item assigned to the Mapper-typed local never compiled — the
        // #339 green-safety argument. SINGLE seat only: the MULTI (MapperC) slot has no
        // carrier and keeps the default.
        // facet collapseGetSuffix (v3.1 C2d retirement family 7, PR #614): the arbiter the
        // paragraph above called "the TRAILING `.get()`" is now the TYPED one —
        // HandlerHelper.bareOnlyElementCollapse (an ONLY_ELEMENT root over a null-typed compiled
        // arm). The c7b census at this seat: 1,223 / 1,223 / 1,192 arrivals (default-route,
        // IR-route, optimised), text=true at 18, and
        // `astNav AND ONLY_ELEMENT AND typeNull` selects exactly those 18 on all three walks; the
        // sibling conjunct isNavOnlyElementLocal already agrees at all 18 (it requires the same
        // ONLY_ELEMENT root), so the arbiter's addition here is its TYPE leg. The row's other named
        // channel, `isMapperS(arm.getExpressionType())`, is REFUTED by the same census: the type is
        // null at every fire, so it is UNPOPULATED, not merely weaker.
        // The single-line test stays: it guards the head/tail SPLICE below against a multi-line
        // arm whose continuations were already re-anchored (own-output text, the #218 law).
        if (!multi && CollectionHandler.isNavOnlyElementLocal(armNode)
                && HandlerHelper.bareOnlyElementCollapse(armNode, arm)
                && armRendered.indexOf('\n') < 0) {
            refs.add(HandlerHelper.MAPPER_S);
            return "MapperS.of(" + armRendered + ")";
        }
        // Coverage wave D (datarule): a COMPARISON-RESULT arm at the Mapper-typed slot
        // coerces `.asMapper()` — upstream's addCoercions CR→Mapper at the assignment
        // (golden drr ReportableInformationMandatorilyClearableCondition{CFTC,ESMA,FCA}
        // `ifThenElseResult = areEqual(…).asMapper();` against the MapperS<Boolean>
        // local); a bare ComparisonResult against the Mapper-typed local never compiled —
        // green-safe by construction.
        // facet comparisonResultIteArmAsMapper (seat 23, law F21): the wave-D TYPE-CONDITION
        // gate retired — the law is context-free (upstream coerces at every assignment), and
        // ONE predicate now serves this seat and the then-chain ifThenElseResult hoist
        // (FunctionExpressionRenderer.appendIteHoistChainCore, the PTRRRule carrier's seat —
        // LAW 69). The seat-23 runtime probe found all 31 ComparisonResult arms reaching
        // this seat inside type conditions, so the retirement moves nothing by measurement.
        if (HandlerHelper.comparisonResultIteArmNeedsAsMapper(armNode, multi)) {
            return HandlerHelper.render(arm) + ".asMapper()";
        }
        if (!(invokableArm || enumConstantArm)
                || arm.unwrapToBuilder().isPresent()) {
            return HandlerHelper.render(arm);
        }
        if (multi) {
            refs.add(HandlerHelper.MAPPER_C);
            return "MapperC.<" + itemType.getSimpleName() + ">of(" + HandlerHelper.render(arm) + ")";
        }
        refs.add(HandlerHelper.MAPPER_S);
        return "MapperS.of(" + HandlerHelper.render(arm) + ")";
    }

    /**
     * facet mapperCIteLift (PR #364): deref an arm whose Mapper ELEMENT is the meta
     * wrapper of the join {@code itemType} — the standard
     * {@code coerceNavigationReceiver} route (the #354 arm-deref pattern): a MapperC
     * receiver takes upstream's BARE element map, a MapperS receiver the null-guarded
     * form. Keeps every non-meta / mismatched-value arm untouched.
     */
    /**
     * facet fnNotionalTogetherRestructure (PR #398): STATEMENT-route hoists a ladder
     * ARM compile registered on the sink (the evaluate-arg meta-deref string decls —
     * ReferenceHandler's statementSink route) relocate INSIDE the owning branch, above
     * the assignment (golden NotionalLeg commodityNotional's in-branch
     * `final ReferenceWithMetaNonNegativeQuantitySchedule …0 = …get();` — the #362
     * relocation law at the statement-route ladder seat). Returns the block-relative
     * prefix ({@code \n\t<line>} per line); empty when the sink is absent (the lambda
     * route) or nothing registered — every pre-#398 arm compile registers nothing
     * here, so the empty-prefix path is byte-identical.
     */
    private static String relocatedStatementHoistPrefix(JavaStatementScope sink, int mark) {
        if (sink == null || mark < 0) {
            return "";
        }
        List<String> relocated = sink.drainStatementHoistsSince(mark);
        if (relocated.isEmpty()) {
            return "";
        }
        StringBuilder prefix = new StringBuilder();
        for (String block : relocated) {
            for (String line : block.split("\n", -1)) {
                if (!line.isEmpty()) {
                    prefix.append("\n\t").append(line);
                }
            }
        }
        return prefix.toString();
    }

    private JavaExpression derefDeepThenIteArmOrKeep(JavaExpression arm,
            JavaClass<?> itemType, boolean lubJoin, ExpressionContext ctx,
            ExpressionCompiler compiler, JavaTypeUtil typeUtil, Set<JavaClass<?>> refs) {
        JavaType armType = arm.getExpressionType();
        JavaType armItem = armType == null ? null : typeUtil.getItemType(armType);
        if (!(armItem instanceof RJavaWithMetaValue metaItem)) {
            return arm;
        }
        // facet fnNotionalTogetherRestructure (PR #398): under a LUB join a wrapper
        // arm whose VALUE type is a proper SUBTYPE of the join (rune joined it there —
        // the walk proved the chain) derefs with the JOIN witness on the bare MapperC
        // element map (the #320 no-guard law; golden NotionalLeg `…filterItemNullSafe(…)
        // .<MeasureBase>map("Type coercion", fieldWithMetaNonNegativeQuantitySchedule ->
        // fieldWithMetaNonNegativeQuantitySchedule.getValue())`). The equal-valueType
        // case keeps the standard coerceNavigationReceiver route below.
        if (lubJoin && !itemType.equals(metaItem.getValueType())
                && armType != null && typeUtil.isMapperC(armType)) {
            String param = ctx.scope().registerDeferredLambdaParam(
                    JavaNamingUtil.toFirstLower(metaItem.getSimpleName()));
            Set<JavaClass<?>> derefRefs = new HashSet<>(arm.getRefs());
            derefRefs.add(itemType);
            JavaExpression coercedLub = JavaExpression.from(
                    HandlerHelper.render(arm) + ".<" + itemType.getSimpleName()
                            + ">map(\"Type coercion\", " + param + " -> " + param
                            + ".getValue())",
                    typeUtil.wrap(typeUtil.MAPPER_C, itemType),
                    derefRefs, arm.getStaticWildcardImports());
            refs.addAll(derefRefs);
            return coercedLub;
        }
        if (!itemType.equals(metaItem.getValueType())) {
            return arm;
        }
        if (compiler.coerceNavigationReceiver(arm, ctx.scope())
                instanceof JavaExpression coerced) {
            refs.addAll(coerced.getRefs());
            return coerced;
        }
        return arm;
    }

    // =========================================================================
    // Statement hoisting (facet ifthenelse_result_hoisting)
    // =========================================================================

    /**
     * The per-arm value contract for {@link #buildIfThenElseHoistBlock}: how a
     * non-conditional branch renders coerced to the hoisted local's declared
     * type, and what value an ABSENT (or DefaultElseRule empty-list) else
     * synthesizes. Arm A2 supplies the ComparisonResult coercions here; the
     * pathed conditional SET (arm A1, {@code FunctionExpressionRenderer})
     * supplies the setter-value unwrap ladder.
     */
    public interface HoistArmRenderer {
        /**
         * Render one branch VALUE (no assignment, no semicolon) coerced to the
         * declared type. Continuation lines, if any, stay RELATIVE — the
         * ladder builder re-anchors them at the arm's depth.
         */
        String renderArm(RExpression arm);

        /**
         * facet choiceDeepNavLadderArm (PR #396): the scope-parametric overload —
         * a statement-form arm (the choice deep-nav ladder) compiles its CASE
         * bodies on a per-case scope carrying the switch-subject binding, not the
         * renderer's captured scope. Default delegates (every pre-#396 impl).
         */
        default String renderArm(RExpression arm, JavaStatementScope armScope) {
            return renderArm(arm);
        }

        /**
         * facet choiceDeepNavLadderArm (PR #396): render one branch as a
         * STATEMENT BLOCK assigning {@code targetRef} at every leaf — non-null
         * ONLY when the arm is a RUNE CHOICE-subject switch of the deep
         * option-nav class (golden cdm6 Qualify_AssetClass_Credit's
         * ifThenElseResult2 ladder). The returned text is pre-anchored at
         * {@code depth} tabs with NO leading indent on its first line and no
         * trailing newline. A {@code null} keeps the single-expression arm path
         * byte-identically (every pre-#396 arm).
         */
        default String renderArmStatements(RExpression arm, String targetRef, int depth) {
            return null;
        }

        /**
         * facet condChainRungHoist (PR #399): compile a nested-rung CONDITION that is a
         * hoistable ctl-free then-chain in a FULL-sink child scope on the method session
         * (the #375-A4 renderArm relay pattern at the CONDITION seat), relaying the
         * hoisted decls onto the ARM-DEREF sink so the #364 pre-{@code if} window drains
         * them at the frame's depth (golden AnnaDsb-FRE commodity {@code final
         * MapperC<ReferenceWithMetaProductIdentifier> thenArg7 = …
         * .filterItemNullSafe(…);} above {@code if (thenArg7\n\t.first()\n\t
         * .mapSingleToItem(…)).getOrDefault(false))}). Non-null ONLY when a decl
         * actually relayed; default {@code null} — every pre-#399 impl keeps the plain
         * condition compile byte-identically.
         */
        default JavaStatementBuilder compileRungConditionChain(RExpression condition) {
            return null;
        }

        /** The synthesized value for an absent/empty else. */
        String emptyElseValue();
    }

    /**
     * The base lexeme of the expression-compiler-reached {@code ifThenElseResult} hoist local —
     * the NEUTRAL name decision for the seats this handler mints it at (arm A2
     * {@link #hoistAsComparisonResultOrNull} and the ctor item-local arm
     * {@link #hoistAsItemLocalOrNull}, incl. its #327-deferred lambda-scoped/sink registrations),
     * extracted as an overridable seam (the D43 IR seam; symmetric to
     * {@code CollectionHandler.thenArgBaseName} / {@code LiteralHandler.bigIntegerBaseName}) so
     * the IR-routed subclass (supplied via the {@code ExpressionCompiler.createControlFlowHandler}
     * factory) can SOURCE it from the neutral ANF substrate (the lab's Wave-6 Phase C slice 6)
     * instead of this constant. The surrounding Java — the {@code 0..n-1} numbering + the
     * {@code _boolean}-style keyword escape ({@link StatementHoistSession#resolve(String)}), the
     * decl type, the arm bodies, the boolean guard and the render-walk ORDER — all stay in the
     * legacy oracle (the L-029 name-driving split). The base returns the legacy constant, so
     * Path-1 is byte-identical; the IR override returns the ANF {@code TempName} base, which
     * equals this constant by construction (Path-2 byte-identical too). The post-pin seats this
     * handler grew AFTER the lab's slice (the list-conditional {@code hoistAsListLocalOrNull},
     * the switch-ite sibling, the in-lambda arg-seat family) keep the constant directly — the
     * lab's own deliberate-uncovered-seat precedent; they widen at the drift wave if the IR route
     * reaches them.
     *
     * <p>The handler is stateless (all type infra is passed per call), so the workspace oracle
     * the override needs is threaded via {@code gm}; the base ignores it.
     *
     * @param expr the conditional whose {@code ifThenElseResult} local hoists
     * @param gm   the generator model (its workspace types/lowers the conditional; unused by the
     *             base)
     * @return the hoist base lexeme — {@link StatementHoistSession#IF_THEN_ELSE_RESULT} on Path-1
     */
    protected String ifThenElseResultBaseName(RConditionalExpr expr, GeneratorModel gm) {
        return StatementHoistSession.IF_THEN_ELSE_RESULT;
    }

    /**
     * Arm A2 — hoist a ComparisonResult-expected conditional into the nearest
     * statement-hoist sink and render as the (sentinel-named) local. Returns
     * {@code null} to decline to the inline ternary.
     */
    private JavaStatementBuilder hoistAsComparisonResultOrNull(RConditionalExpr expr,
            ExpressionContext ctx, ExpressionCompiler compiler) {
        if (ctx.scope() == null || ctx.expectedType() == null) {
            return null;
        }
        JavaTypeUtil typeUtil = compiler.getTypeUtil();
        if (typeUtil == null || !typeUtil.isComparisonResult(ctx.expectedType())) {
            return null;
        }
        JavaStatementScope sink = ctx.scope().findStatementHoistSink();
        if (sink == null) {
            return null;
        }
        // Coverage wave D (datarule): inside a TYPE-CONDITION compile the sentinel
        // registers AFTER the arms — upstream's right-fold numbers a NESTED
        // conditional-operand cascade INNERMOST-FIRST (golden
        // TradeNovationContentChoice2: the outermost decl is ifThenElseResult2,
        // the deepest 0 — the #355 in-lambda seat's registration-order precedent
        // at the sink seat). Every function/rule-path compile keeps the pre-wave
        // register-first order byte-identically (context-gated; no nested-A2
        // carrier exists in those populations — they are TRUE 100% under the
        // pre-wave order).
        boolean conditionContext = HandlerHelper.findEnclosingTypeCondition(expr) != null;
        // Coverage wave D (datarule): the LOGICAL-COMBINE deferral — when this
        // conditional IS the operand root a LogicalHandler frame is active for
        // (node identity), its sentinel registration defers past the sibling
        // operand's compile entirely (upstream collapses the LEFT operand at the
        // combine, AFTER the right subtree composed — golden
        // CFTCPart43TransactionReportFloatingRateResetFrequencyPeriodCond's
        // B=0/C=1/A=2 numbering). The block registers in the sink NOW (emission
        // keeps source order) carrying a unique placeholder the combine
        // substitutes. A nested conditional inside this operand's arms sees the
        // frame but fails the root identity — it keeps the after-arms
        // registration below.
        JavaStatementScope.A2DeferralFrame a2Frame = conditionContext
                ? ctx.scope().findA2DeferralFrame() : null;
        boolean a2Deferred = a2Frame != null && a2Frame.operandRoot == expr;
        String sentinel = a2Deferred
                ? "__DATARULE_A2_DEFER_" + A2_DEFER_SEQ.incrementAndGet() + "__"
                : conditionContext
                ? "__DATARULE_A2_ITE_PLACEHOLDER__"
                : sink.statementHoistSession()
                        .register(ifThenElseResultBaseName(expr, compiler.getGeneratorModel()));
        Set<JavaClass<?>> refs = new HashSet<>();
        Set<JavaClass<?>> wildcards = new HashSet<>();
        refs.add(HandlerHelper.COMPARISON_RESULT);
        HoistArmRenderer arms = new HoistArmRenderer() {
            @Override
            public String renderArm(RExpression arm) {
                return renderArm(arm, ctx.scope());
            }

            @Override
            public String renderArm(RExpression arm, JavaStatementScope armScope) {
                JavaStatementBuilder armBuilder = compiler.compile(arm, null, armScope);
                refs.addAll(armBuilder.getRefs());
                wildcards.addAll(armBuilder.getStaticWildcardImports());
                String rendered = HandlerHelper.render(armBuilder);
                // A boolean-literal arm compiles to `MapperS.of(true|false)` —
                // golden coerces it ComparisonResult.ofNullSafe(...) (upstream
                // addCoercions item→ComparisonResult). Since seat 19 (PR #591,
                // facet booleanLiteralOperandNullSafe) that arm lives in the
                // SHARED operand wrapper, which this renderer CONSULTS for the
                // literal exactly as it always has for bare boolean-FUNCTION
                // arms — LAW 69: one helper, so the logical-OPERAND seat and
                // this conditional-ARM seat agree by construction. The string
                // is the one this site emitted locally before (the hoist half
                // is byte-neutral; the ring is the receipt). Every
                // already-ComparisonResult arm (exists/areEqual/logical chains)
                // passes through untouched.
                // KNOWN GAP (no flip carrier): a boolean attribute-NAVIGATION
                // arm renders its Mapper chain unwrapped — upstream would
                // ofNullSafe-wrap it; such files carried the inline ternary
                // pre-facet and stay waivered either way.
                return LogicalHandler.wrapBooleanFunctionOperand(rendered, arm, armBuilder, refs);
            }

            @Override
            public String renderArmStatements(RExpression arm, String targetRef, int depth) {
                // facet choiceDeepNavLadderArm (PR #396): the ComparisonResult-expected
                // conditional-arm seat is the statement-form venue — a RUNE
                // CHOICE-subject switch arm renders the deep option-nav ladder
                // (golden cdm6 Qualify_AssetClass_Credit ifThenElseResult2).
                if (!(arm instanceof RSwitchExpr sw)) {
                    return null;
                }
                return renderChoiceDeepNavLadderArmOrNull(sw, targetRef, depth, this,
                        ctx, compiler, refs, wildcards);
            }

            @Override
            public String emptyElseValue() {
                // Upstream TypeCoercionService empty→ComparisonResult — the
                // drr *_Validation elseless carriers.
                return "ComparisonResult.ofEmpty()";
            }
        };
        String block = buildIfThenElseHoistBlock(
                expr, sentinel, "ComparisonResult", compiler, ctx.scope(), arms, refs, wildcards);
        if (a2Deferred) {
            // The combine registers + substitutes (LogicalHandler) — the block and
            // the returned expression carry the unique placeholder until then.
            a2Frame.placeholders.add(sentinel);
        } else if (conditionContext) {
            String registered = sink.statementHoistSession()
                    .register(StatementHoistSession.IF_THEN_ELSE_RESULT);
            block = block.replace(sentinel, registered);
            sentinel = registered;
        }
        sink.registerStatementHoist(block);
        return JavaExpression.from(sentinel, null, refs, wildcards);
    }

    /**
     * Coverage wave D (datarule): the deferred-A2 placeholder sequence — uniqueness
     * only (every placeholder is substituted away at the logical combine, so the
     * counter never reaches emitted bytes).
     */
    private static final java.util.concurrent.atomic.AtomicInteger A2_DEFER_SEQ =
            new java.util.concurrent.atomic.AtomicInteger();

    /**
     * facet choiceDeepNavLadderArm (PR #396): render a RUNE CHOICE-subject switch that is a
     * hoisted-conditional ARM VALUE as the upstream DEEP option-nav ladder, assigning the
     * blank-final target at every leaf (golden cdm6 Qualify_AssetClass_Credit
     * {@code ifThenElseResult2}):
     *
     * <pre>
     * final MapperS&lt;? extends Underlier&gt; switchArgument = performanceUnderlier(economicTerms);
     * if (switchArgument.get() == null) {
     *     ifThenElseResult2 = ComparisonResult.ofEmpty();
     * } else if (switchArgument.&lt;ReferenceWithMetaObservable&gt;map("getObservable", underlier -&gt;
     *         underlier.getObservable()).&lt;Observable&gt;map("Type coercion", …).&lt;Asset&gt;map(
     *         "getAsset", …).&lt;Instrument&gt;map("getInstrument", …).&lt;Loan&gt;map("getLoan", …)
     *         .get() != null) {
     *     final MapperS&lt;Loan&gt; loan = &lt;the same chain&gt;;
     *     ifThenElseResult2 = ComparisonResult.ofNullSafe(MapperS.of(true));
     * } … else {
     *     ifThenElseResult2 = ComparisonResult.ofNullSafe(MapperS.of(false));
     * }
     * </pre>
     *
     * <p>The #394 SET-seat option-nav ladder widened along three axes (upstream
     * {@code caseSwitchOperation}'s RChoiceType branch): (a) the SEAT — a conditional-arm
     * value inside {@link #buildIfThenElseHoistBlock} (the {@link HoistArmRenderer}
     * statement-form hook) instead of the SET assignment; (b) the SUBJECT — a bare ALIAS-call
     * Mapper, declared verbatim with the alias signature's {@code MapperS<? extends T>}
     * wildcard element (the same-walk law: {@code FunctionAliasHelper.buildMapperReturnType}
     * renders the model element exactly so) instead of a bare-identifier {@code MapperS.of}
     * wrap; (c) the RUNGS — each case type resolves through the NESTED-choice option tree
     * (upstream {@code findOptionPath}: DFS the projected options in declaration order, first
     * reachable option wins; a META option renders the wrapper witness + the guarded
     * {@code Type coercion} deref whose lambda param registers on the STATEMENT scope so it
     * NUMBERS with the method's same-named deref hoists — golden
     * {@code referenceWithMetaObservable0..3}) instead of single-hop direct options. Case
     * locals claim their names on the SHARED statement scope (sibling-arm escape visibility:
     * the Loan rung's {@code getLoan} param sees the Instrument arm's {@code instrument}
     * local → {@code _instrument}); the case body compiles with the implicit item BOUND to
     * the Mapper-typed local on a per-case body scope (the #221 {@code bindSwitchSubject}
     * re-root — the #396 deep-call consult {@code switchCaseNarrowedImplicitGuardRType}
     * resolves the SAME guard type AST-statically for the {@code <type>DeepPathUtil}
     * field/@Inject pair); arms COMPILE in REVERSE case order (upstream's right-fold — the
     * session numbering law) and EMIT in text order; the default compiles FIRST (upstream
     * compiles the default before the fold).
     *
     * <p>Declines ({@code null} → the caller's single-expression arm path — the residual seat's
     * {@code TYPE_SWITCH_TERNARY_STUB} refusal per resolvable case type, else {@code SWITCH_TERNARY_STUB} — R1 —
     * for an unresolvable one; TYPE_ since v3.1 C0, R1 since v3.2 seat 12; before each the
     * broken {@code Objects.equals} type-literal ternary, which never compiled, so every carrier was
     * waivered and the fire set green-safe by construction): a missing scope/sink/type
     * service, a non-choice subject, a non-alias-call subject, an unresolvable case type or
     * option path, or a multi-line / hoist-registering arm or default compile. A
     * post-registration decline restores the hoist session ({@code switchArgument}) but NOT
     * scope-level deferred name entries — the #394-OBS-1 precedent, contained to the
     * single-carrier fire population (a stranded entry can only renumber names inside a file
     * that stays divergent either way).
     */
    private String renderChoiceDeepNavLadderArmOrNull(RSwitchExpr sw, String targetRef,
            int depth, HoistArmRenderer arms, ExpressionContext ctx, ExpressionCompiler compiler,
            Set<JavaClass<?>> refs, Set<JavaClass<?>> wildcards) {
        JavaStatementScope scope = ctx.scope();
        GeneratorModel gm = compiler.getGeneratorModel();
        var tt = compiler.getTypeTranslator();
        JavaTypeUtil tu = compiler.getTypeUtil();
        if (scope == null || gm == null || tt == null || tu == null
                || !ChoiceSwitchSupport.isChoiceTypeSwitch(sw, compiler)) {
            return null;
        }
        JavaStatementScope sink = scope.findStatementHoistSink();
        if (sink == null) {
            return null;
        }
        // The subject: a bare ALIAS call whose inferred type (alias-stripped) is a RUNE
        // CHOICE — the carrier shape; the decl reuses the alias signature's
        // `MapperS<? extends T>` element (the same-walk law). The workspace leaves the
        // alias-CALL's own inferred type MISSING at this seat (the T396A read), so the
        // choice resolves off the alias BODY's inferred type — the SAME expression the
        // alias signature's type walk reads, so the two cannot disagree.
        RExpression subjectExpr = HandlerHelper.orSyntheticImplicit(sw.argument(), sw);
        if (!(subjectExpr instanceof RSymbolReference subjRef)
                || !(subjRef.symbol().orElse(null)
                        instanceof com.regnosys.rosetta.ast.functions.RShortcut shortcut)) {
            return null;
        }
        RType subjT = choiceSubjectType(subjectExpr, shortcut, compiler);
        if (!(subjT instanceof com.regnosys.rosetta.types.RChoiceTypeRef choiceRef)
                || choiceRef.asRDataType() == null
                || !(tt.toJavaReferenceType(choiceRef) instanceof JavaClass<?> choiceJC)) {
            return null;
        }
        List<RSwitchCase> valueCases = new ArrayList<>();
        RExpression defaultExpr = null;
        for (RSwitchCase sc : sw.cases()) {
            if (sc.isDefault()) {
                defaultExpr = sc.expression();
                continue;
            }
            valueCases.add(sc);
        }
        if (valueCases.isEmpty()) {
            return null;
        }
        // Pre-pass (pure — no registrations): every case type must resolve to an option
        // PATH through the nested-choice tree, so a structurally-ineligible switch never
        // strands a deferred-name registration. The guard resolves by SIMPLE NAME against
        // the option tree ITSELF (namespace-correct by construction — the cp1 catch: a
        // workspace lookup resolved cdm6 QAC's `Index` to fpml.consolidated's Index).
        List<List<ChoiceSwitchSupport.ChoiceOptionHop>> paths = new ArrayList<>();
        for (RSwitchCase sc : valueCases) {
            RSwitchCaseGuard guard = sc.guard().orElse(null);
            String qn = guard == null || guard.kind() != SwitchGuardKind.NAME
                    ? null : guard.qualifiedName().orElse(null);
            if (qn == null || sc.expression() == null) {
                return null;
            }
            String caseSimple = qn.substring(qn.lastIndexOf('.') + 1);
            List<ChoiceSwitchSupport.ChoiceOptionHop> path = new ArrayList<>();
            if (!ChoiceSwitchSupport.findChoiceOptionPath(choiceRef, caseSimple, compiler, path, new HashSet<>())) {
                return null;
            }
            paths.add(path);
        }
        JavaStatementBuilder subjCompiled = compiler.compile(subjectExpr, null, scope);
        if (!(subjCompiled instanceof JavaExpression)) {
            return null;
        }
        String subjText = HandlerHelper.render(subjCompiled);
        if (subjText.contains("\n")) {
            return null;
        }
        refs.addAll(subjCompiled.getRefs());
        wildcards.addAll(subjCompiled.getStaticWildcardImports());
        refs.add(choiceJC);
        // ---- registrations begin.
        StatementHoistSession session = sink.statementHoistSession();
        java.util.Map<String, Integer> sessionSnap = session.snapshot();
        String subject = session.register("switchArgument");
        // The default compiles FIRST (upstream compiles the default before the fold).
        String defaultRhs = null;
        if (defaultExpr != null && !(defaultExpr instanceof REmptyLiteral)
                && !isEmptyListLiteral(defaultExpr)) {
            int mark = sink.statementHoistMark();
            String rhs = arms.renderArm(defaultExpr);
            if (!sink.drainStatementHoistsSince(mark).isEmpty() || rhs.contains("\n")) {
                session.restore(sessionSnap);
                return null;
            }
            defaultRhs = rhs;
        }
        if (defaultRhs == null) {
            defaultRhs = arms.emptyElseValue();
        }
        int n = valueCases.size();
        String[] navTexts = new String[n];
        String[] localTokens = new String[n];
        String[] caseSimples = new String[n];
        String[] armValues = new String[n];
        String choiceSimple = choiceJC.getSimpleName();
        // Compile REVERSE (session numbering: upstream's right-fold), emit text order.
        for (int i = n - 1; i >= 0; i--) {
            RSwitchCase sc = valueCases.get(i);
            List<ChoiceSwitchSupport.ChoiceOptionHop> path = paths.get(i);
            JavaClass<?> caseJC = path.get(path.size() - 1).optJC();
            String caseSimple = caseJC.getSimpleName();
            StringBuilder nav = new StringBuilder(subject);
            String recvSimple = choiceSimple;
            for (ChoiceSwitchSupport.ChoiceOptionHop hop : path) {
                RJavaWithMetaValue wrapper = NavigationHandler.metaWrapperOf(hop.attr(), compiler);
                String hopParam = scope.registerDeferredLambdaParam(
                        JavaNamingUtil.toFirstLower(recvSimple));
                String witness = wrapper != null
                        ? wrapper.getSimpleName() : hop.optJC().getSimpleName();
                nav.append(".<").append(witness).append(">map(\"get").append(hop.attr().name())
                   .append("\", ").append(hopParam).append(" -> ").append(hopParam)
                   .append(".get").append(hop.attr().name()).append("())");
                if (wrapper != null) {
                    refs.add(wrapper);
                    // The guarded deref param registers on the STATEMENT scope — the
                    // method-wide computeActualNames group, so it numbers with the
                    // method's same-named deref hoists (golden
                    // referenceWithMetaObservable0..3 — the outer evaluate-arg hoist
                    // registered first, the rungs follow in reverse-fold order).
                    String cp = scope.registerDeferredCoercionParam(
                            JavaNamingUtil.toFirstLower(wrapper.getSimpleName()));
                    nav.append(".<").append(hop.optJC().getSimpleName())
                       .append(">map(\"Type coercion\", ").append(cp).append(" -> ").append(cp)
                       .append(" == null ? null : ").append(cp).append(".getValue())");
                }
                refs.add(hop.optJC());
                recvSimple = hop.optJC().getSimpleName();
            }
            // The case LOCAL claims its name on the SHARED scope (sibling-arm escape
            // visibility); the body compiles with the implicit BOUND to the Mapper local
            // on a per-case body scope.
            com.regnosys.rosetta.generator.GeneratedIdentifier localId =
                    scope.createUniqueIdentifier(JavaNamingUtil.toFirstLower(caseSimple));
            String localToken = scope.registerDeferredCoercionName(localId);
            JavaStatementScope caseScope = scope.bodyScope();
            caseScope.bindSwitchSubject(sw, JavaExpression.from(localToken,
                    tu.wrap(tu.MAPPER_S, caseJC)));
            int mark = sink.statementHoistMark();
            String armValue = arms.renderArm(sc.expression(), caseScope);
            if (!sink.drainStatementHoistsSince(mark).isEmpty() || armValue.contains("\n")) {
                session.restore(sessionSnap);
                return null;
            }
            refs.add(caseJC);
            navTexts[i] = nav.toString();
            localTokens[i] = localToken;
            caseSimples[i] = caseSimple;
            armValues[i] = armValue;
        }
        String t = tabs(depth);
        String t1 = tabs(depth + 1);
        StringBuilder sb = new StringBuilder();
        sb.append("final MapperS<? extends ").append(choiceSimple).append("> ").append(subject)
          .append(" = ").append(subjText).append(";\n");
        sb.append(t).append("if (").append(subject).append(".get() == null) {\n");
        sb.append(t1).append(targetRef).append(" = ").append(arms.emptyElseValue()).append(";\n");
        for (int i = 0; i < n; i++) {
            sb.append(t).append("} else if (").append(navTexts[i]).append(".get() != null) {\n");
            sb.append(t1).append("final MapperS<").append(caseSimples[i]).append("> ")
              .append(localTokens[i]).append(" = ").append(navTexts[i]).append(";\n");
            sb.append(t1).append(targetRef).append(" = ").append(armValues[i]).append(";\n");
        }
        sb.append(t).append("} else {\n");
        sb.append(t1).append(targetRef).append(" = ").append(defaultRhs).append(";\n");
        sb.append(t).append('}');
        return sb.toString();
    }

    /**
     * Coverage wave D (datarule): the executeDataRule RETURN-ladder form of a
     * whole-condition-body switch over a CHOICE-valued attribute — upstream
     * {@code caseSwitchOperation}'s RChoiceType branch statementized by
     * {@code completeAsReturn} (golden ExerciseInstructionIsOptionPayout):
     *
     * <pre>
     * final MapperS&lt;Payout&gt; switchArgument = MapperS.of(exerciseInstruction)
     *         .&lt;ReferenceWithMetaPayout&gt;map("getExerciseOption", …)
     *         .&lt;Payout&gt;map("Type coercion", referenceWithMetaPayout -&gt; … .getValue());
     * if (switchArgument.get() == null) {
     *     return ComparisonResult.ofEmpty();
     * }
     * if (switchArgument.&lt;OptionPayout&gt;map("getOptionPayout", payout -&gt; …).get() != null) {
     *     final MapperS&lt;OptionPayout&gt; optionPayout = switchArgument.&lt;OptionPayout&gt;map(…);
     *     return exists(optionPayout);
     * }
     * return ComparisonResult.ofNullSafe(MapperS.of(false));   // ← the returned consumer
     * </pre>
     *
     * <p>The ladder statements (subject decl + null-guard + sequential case
     * {@code if} blocks with their case locals and {@code return} arms) hoist as
     * ONE relative-indent block into the statement sink; the compiled DEFAULT
     * expression is returned as the consumer (the datarule general path emits
     * {@code return ComparisonResult.ofNullSafe(<consumer>);}). The case bodies
     * compile with the implicit BOUND to the case local (the #221
     * {@code bindSwitchSubject} re-root); a META-wrapped subject appends the
     * guarded {@code Type coercion} deref with a scope-registered param. Gates:
     * the switch must BE the condition's whole expression, carry a hoist sink, a
     * choice-valued subject attribute, all-pathable case names
     * ({@code ChoiceSwitchSupport.findChoiceOptionPath}) and a non-empty default — anything else
     * declines to the residual path ({@code TYPE_SWITCH_TERNARY_STUB} per resolvable case - the
     * choice-option guards here - since v3.1 C0, else the R1 refusal at {@code SWITCH_TERNARY_STUB}
     * since v3.2 seat 12; the inline ternary, waivered, before them).
     */
    private JavaStatementBuilder hoistDataRuleSwitchReturnLadderOrNull(RSwitchExpr sw,
            ExpressionContext ctx, ExpressionCompiler compiler) {
        JavaStatementScope scope = ctx.scope();
        GeneratorModel gm = compiler.getGeneratorModel();
        var tt = compiler.getTypeTranslator();
        JavaTypeUtil tu = compiler.getTypeUtil();
        if (scope == null || gm == null || tt == null || tu == null) {
            return null;
        }
        com.regnosys.rosetta.ast.functions.RCondition condition =
                HandlerHelper.findEnclosingTypeCondition(sw);
        if (condition == null || !isDataRuleReturnSeat(sw, condition)) {
            return null;
        }
        JavaStatementScope sink = scope.findStatementHoistSink();
        if (sink == null) {
            return null;
        }
        RExpression subjectExpr = HandlerHelper.orSyntheticImplicit(sw.argument(), sw);
        RAttribute subjectAttr = null;
        if (subjectExpr instanceof RSymbolReference sr
                && sr.symbol().orElse(null) instanceof RAttribute a) {
            subjectAttr = a;
        } else if (subjectExpr instanceof RFeatureCall fc) {
            subjectAttr = fc.resolvedFeature().orElse(null);
        }
        if (subjectAttr == null) {
            return null;
        }
        RType subjT = stripAliases(gm.getType(subjectAttr));
        if (!(subjT instanceof com.regnosys.rosetta.types.RChoiceTypeRef choiceRef)
                || choiceRef.asRDataType() == null
                || !(tt.toJavaReferenceType(choiceRef) instanceof JavaClass<?> choiceJC)) {
            return null;
        }
        List<RSwitchCase> valueCases = new ArrayList<>();
        RExpression defaultExpr = null;
        for (RSwitchCase sc : sw.cases()) {
            if (sc.isDefault()) {
                defaultExpr = sc.expression();
                continue;
            }
            valueCases.add(sc);
        }
        if (valueCases.isEmpty() || defaultExpr == null
                || defaultExpr instanceof REmptyLiteral || isEmptyListLiteral(defaultExpr)) {
            return null;
        }
        List<List<ChoiceSwitchSupport.ChoiceOptionHop>> paths = new ArrayList<>();
        for (RSwitchCase sc : valueCases) {
            RSwitchCaseGuard guard = sc.guard().orElse(null);
            String qn = guard == null || guard.kind() != SwitchGuardKind.NAME
                    ? null : guard.qualifiedName().orElse(null);
            if (qn == null || sc.expression() == null) {
                return null;
            }
            String caseSimple = qn.substring(qn.lastIndexOf('.') + 1);
            List<ChoiceSwitchSupport.ChoiceOptionHop> path = new ArrayList<>();
            if (!ChoiceSwitchSupport.findChoiceOptionPath(choiceRef, caseSimple, compiler, path, new HashSet<>())) {
                return null;
            }
            paths.add(path);
        }
        JavaStatementBuilder subjCompiled = compiler.compile(subjectExpr, null, scope);
        if (!(subjCompiled instanceof JavaExpression)) {
            return null;
        }
        String subjText = HandlerHelper.render(subjCompiled);
        if (subjText.contains("\n")) {
            return null;
        }
        Set<JavaClass<?>> refs = new HashSet<>(subjCompiled.getRefs());
        Set<JavaClass<?>> wildcards = new HashSet<>(subjCompiled.getStaticWildcardImports());
        refs.add(choiceJC);
        refs.add(HandlerHelper.COMPARISON_RESULT);
        String choiceSimple = choiceJC.getSimpleName();
        // A META-wrapped subject derefs to the choice VALUE (the guarded
        // Type-coercion hop; the param registers on the statement scope).
        RJavaWithMetaValue subjWrapper = NavigationHandler.metaWrapperOf(subjectAttr, compiler);
        if (subjWrapper != null) {
            String cp = scope.registerDeferredCoercionParam(
                    JavaNamingUtil.toFirstLower(subjWrapper.getSimpleName()));
            subjText = subjText + ".<" + choiceSimple + ">map(\"Type coercion\", " + cp
                    + " -> " + cp + " == null ? null : " + cp + ".getValue())";
            refs.add(subjWrapper);
        }
        StatementHoistSession session = sink.statementHoistSession();
        java.util.Map<String, Integer> sessionSnap = session.snapshot();
        String subject = session.register("switchArgument");
        // The default compiles FIRST (upstream compiles the default before the
        // fold) — it becomes the RETURNED consumer.
        int defaultMark = sink.statementHoistMark();
        JavaStatementBuilder defaultCompiled = compiler.compile(defaultExpr, null, scope);
        if (!sink.drainStatementHoistsSince(defaultMark).isEmpty()
                || !(defaultCompiled instanceof JavaExpression)
                || HandlerHelper.render(defaultCompiled).contains("\n")) {
            session.restore(sessionSnap);
            return null;
        }
        refs.addAll(defaultCompiled.getRefs());
        wildcards.addAll(defaultCompiled.getStaticWildcardImports());
        int n = valueCases.size();
        String[] navTexts = new String[n];
        String[] localTokens = new String[n];
        String[] caseSimples = new String[n];
        String[] armValues = new String[n];
        // Compile REVERSE (session numbering: upstream's right-fold), emit text order.
        for (int i = n - 1; i >= 0; i--) {
            RSwitchCase sc = valueCases.get(i);
            List<ChoiceSwitchSupport.ChoiceOptionHop> path = paths.get(i);
            JavaClass<?> caseJC = path.get(path.size() - 1).optJC();
            String caseSimple = caseJC.getSimpleName();
            StringBuilder nav = new StringBuilder(subject);
            String recvSimple = choiceSimple;
            for (ChoiceSwitchSupport.ChoiceOptionHop hop : path) {
                RJavaWithMetaValue wrapper = NavigationHandler.metaWrapperOf(hop.attr(), compiler);
                String hopParam = scope.registerDeferredLambdaParam(
                        JavaNamingUtil.toFirstLower(recvSimple));
                String witness = wrapper != null
                        ? wrapper.getSimpleName() : hop.optJC().getSimpleName();
                nav.append(".<").append(witness).append(">map(\"get").append(hop.attr().name())
                   .append("\", ").append(hopParam).append(" -> ").append(hopParam)
                   .append(".get").append(hop.attr().name()).append("())");
                if (wrapper != null) {
                    refs.add(wrapper);
                    String cp = scope.registerDeferredCoercionParam(
                            JavaNamingUtil.toFirstLower(wrapper.getSimpleName()));
                    nav.append(".<").append(hop.optJC().getSimpleName())
                       .append(">map(\"Type coercion\", ").append(cp).append(" -> ").append(cp)
                       .append(" == null ? null : ").append(cp).append(".getValue())");
                }
                refs.add(hop.optJC());
                recvSimple = hop.optJC().getSimpleName();
            }
            com.regnosys.rosetta.generator.GeneratedIdentifier localId =
                    scope.createUniqueIdentifier(JavaNamingUtil.toFirstLower(caseSimple));
            String localToken = scope.registerDeferredCoercionName(localId);
            JavaStatementScope caseScope = scope.bodyScope();
            caseScope.bindSwitchSubject(sw, JavaExpression.from(localToken,
                    tu.wrap(tu.MAPPER_S, caseJC)));
            int mark = sink.statementHoistMark();
            JavaStatementBuilder armCompiled = compiler.compile(sc.expression(), null, caseScope);
            if (!sink.drainStatementHoistsSince(mark).isEmpty()
                    || !(armCompiled instanceof JavaExpression)) {
                session.restore(sessionSnap);
                return null;
            }
            String armSrc = HandlerHelper.render(armCompiled);
            if (armSrc.contains("\n")) {
                session.restore(sessionSnap);
                return null;
            }
            refs.addAll(armCompiled.getRefs());
            wildcards.addAll(armCompiled.getStaticWildcardImports());
            // The arm returns CR-typed directly; a non-CR arm takes the same
            // expected-CR completeAsReturn coercion as the ladder rungs.
            boolean armCr = HandlerHelper.isComparisonResultExpr(sc.expression())
                    || sc.expression() instanceof RCardinalityCheckExpr;
            armValues[i] = armCr ? armSrc : "ComparisonResult.ofNullSafe(" + armSrc + ")";
            refs.add(caseJC);
            navTexts[i] = nav.toString();
            localTokens[i] = localToken;
            caseSimples[i] = caseSimple;
        }
        StringBuilder block = new StringBuilder();
        block.append("final MapperS<").append(choiceSimple).append("> ").append(subject)
             .append(" = ").append(subjText).append(";\n");
        block.append("if (").append(subject).append(".get() == null) {\n");
        block.append("\treturn ComparisonResult.ofEmpty();\n");
        block.append("}\n");
        for (int i = 0; i < n; i++) {
            block.append("if (").append(navTexts[i]).append(".get() != null) {\n");
            block.append("\tfinal MapperS<").append(caseSimples[i]).append("> ")
                 .append(localTokens[i]).append(" = ").append(navTexts[i]).append(";\n");
            block.append("\treturn ").append(armValues[i]).append(";\n");
            block.append(i == n - 1 ? "}" : "}\n");
        }
        sink.registerStatementHoist(block.toString());
        refs.add(HandlerHelper.MAPPER_S);
        return JavaExpression.from(HandlerHelper.render(defaultCompiled), null, refs, wildcards);
    }

    /**
     * Coverage wave D (datarule): true when the switch occupies a RETURN seat of
     * the condition body — the whole expression, or a conditional then/else arm
     * reachable from the condition through conditional arms only (the
     * completeAsReturn ladder statementizes exactly those positions — golden
     * ExerciseInstructionIsOptionPayout's rung-seat switch vs
     * FinalCalculationPeriodDateAdjustment…'s whole-body switch). An OPERAND
     * seat (e.g. inside an {@code and}) must decline — a hoisted
     * {@code return} ladder there would exit mid-expression.
     */
    private static boolean isDataRuleReturnSeat(RSwitchExpr sw,
            com.regnosys.rosetta.ast.functions.RCondition condition) {
        com.regnosys.rosetta.ast.RNode child = sw;
        com.regnosys.rosetta.ast.RNode p = sw.parent();
        int depth = 0;
        while (p != null && depth++ < HandlerHelper.PARENT_WALK_LIMIT) {
            if (p == condition) {
                return true;
            }
            if (p instanceof RConditionalExpr pc
                    && (pc.thenBranch() == child || pc.elseBranch().orElse(null) == child)) {
                child = p;
                p = p.parent();
                continue;
            }
            return false;
        }
        return false;
    }

    /*
     * The choice option-path walk (`ChoiceOptionHop` + `findChoiceOptionPath`) was PROMOTED
     * to {@link ChoiceSwitchSupport} at seat 31 (facet choiceOptionNavLadderDeepHop) so the
     * SET-seat option-nav ladder (FunctionExpressionRenderer) consults the SAME walk as the
     * two ladder forms here — the two-halves-agree law. Both call sites below re-point.
     */

    /** Alias-strip an {@link RType}. */
    private static RType stripAliases(RType t) {
        t = HandlerHelper.stripAliases(t);
        return t;
    }

    /**
     * facet choiceDeepNavLadderArm (PR #396): the switch SUBJECT's alias-stripped type —
     * the subject expression's own inferred type when present, else the ALIAS BODY's
     * inferred type (the workspace leaves a fn-scoped alias CALL's type MISSING at the
     * switch seat; the body walk is the SAME source the alias signature's type derives
     * from, so the ladder's choice read and the declared {@code MapperS<? extends T>}
     * element cannot disagree).
     */
    static RType choiceSubjectType(RExpression subjectExpr,
            com.regnosys.rosetta.ast.functions.RShortcut shortcut, ExpressionCompiler compiler) {
        GeneratorModel gm = compiler.getGeneratorModel();
        if (gm == null || gm.workspace() == null) {
            return null;
        }
        com.regnosys.rosetta.types.RMetaAnnotatedType inferred =
                gm.workspace().getInferredType(subjectExpr);
        RType subjT = inferred == null ? null : stripAliases(inferred.type());
        if (subjT instanceof com.regnosys.rosetta.types.RChoiceTypeRef) {
            return subjT;
        }
        if (shortcut != null && shortcut.expression() != null) {
            // The workspace inference is MISSING for the alias body too (the T396A
            // read) — the body's LEAF feature attr resolves through the SAME
            // gm-aware receiver walk the witness path uses (typeCall resolution:
            // namespace-correct by the resolved-feature identity).
            RType bodyT = NavigationHandler.resolveReceiverRType(
                    shortcut.expression(), gm, compiler);
            if (bodyT instanceof com.regnosys.rosetta.types.RChoiceTypeRef) {
                return bodyT;
            }
        }
        return subjT;
    }

    /**
     * facet ctor_setter_ite_hoist — item-typed local hoist for a no-else
     * single-cardinality conditional consumed at a single-expression seat (ctor
     * pair value, evaluate arg, comparison operand). Upstream law: EVERY
     * mid-expression consumption of a compiled conditional collapses it via
     * {@code JavaIfThenElseBuilder.collapseToSingleExpression} →
     * {@code declareAsVariable(true, "ifThenElseResult")} (in-tree 9.83.0
     * JavaIfThenElseBuilder.java:131-134; seats: ctor entry
     * ExpressionGenerator.xtend:1205-1207, assign-with-path
     * FunctionGenerator.xtend:450-452, method args
     * JavaStatementBuilder.invokeMethod:92-112). The local types at the join of
     * the seat-coerced branch types — the absent else compiles to
     * {@code JavaLiteral.NULL} whose NULL_TYPE joins away, so the declared type
     * is the THEN branch's ITEM type ({@link #thenItemJavaClass}); the
     * JavaLiteral else selects upstream's INITIALIZER form
     * {@code <T> ifThenElseResultN = null;} + if-without-else
     * ({@code declareAsVariable}'s literal-else rule). The assignment value is
     * the ITEM-form (wrapperToItem at the seat: structural wraps strip to the
     * bare inner — fn calls, ctor blocks; Mapper chains take {@code .get()};
     * enum constants (by the producer's witness) and the null literal pass bare). The returned
     * sentinel is {@link JavaExpression#selfUnwrapping} so the ctor-arg
     * coercion ladder splices the bare local ({@code .setX(ifThenElseResultN)})
     * without the chain {@code .get()} fall-through; numbering rides the
     * method-spanning {@link StatementHoistSession} ifThenElseResult group
     * (singleton bare, 0..n-1 in registration = consumption source order —
     * upstream GeneratorScope.computeActualNames).
     *
     * <p>An EFFECTIVE else (facet ctorIteEffElse, PR #203 — the
     * {@code final <T> ifThenElseResult; if(..){=A;} else if(..){=B;} else
     * {=null;}} uninitialized if/else-ladder form) now ALSO hoists: it routes
     * through the SAME {@link #buildIfThenElseHoistBlock}, whose
     * {@code initializerForm = !hasEffectiveElse && null-else} selects the
     * {@code final}-uninitialized form + emits the trailing else. Declines (→ the
     * inline ternary, today's bytes): MULTI cardinality (the list-seat golden form
     * differs: {@code Collections.<T>emptyList()} else — still gated below), sink-less
     * compilation paths
     * (rule/alias/POJO-condition emission never sees a sink —
     * hoistSessionEligible freezing; LAMBDA interiors — the sink walk stops at
     * the lambda boundary, upstream's block-body lift is the deferred in-lambda
     * variant), and then-branches outside the typing ladder. Green-safe by
     * construction: zero goldens carry the {@code getOrDefault(false) ? }
     * ternary in ANY seat (corpus-wide grep over all 34,686 goldens), so every
     * convertible seat lives in a waivered file.
     *
     * <p>The named seats (ctor pair value, evaluate arg) are ILLUSTRATIVE, not
     * a gate — the arm is seat-agnostic (it keys only on the sink + cardinality
     * + no-else + typable-then conditions). It can therefore also fire at a
     * comparison/logical operand seat where an {@link RConditionalExpr} is the
     * operand; the operand-wrap helpers gate on {@code RSymbolReference}/
     * {@code RCountExpr}/the enum-constant witness and so leave the bare local unwrapped =
     * non-compiling, but HARMLESSLY: such a file was already waivered (it
     * emitted the equally non-compiling ternary operand pre-facet), so the
     * green-safety argument above still holds (churned bytes in an
     * already-divergent file, never a green→red flip).
     */
    private JavaStatementBuilder hoistAsItemLocalOrNull(RConditionalExpr expr,
            ExpressionContext ctx, ExpressionCompiler compiler) {
        // facet ctorIteEffElse (PR #203): an EFFECTIVE-else single-card
        // conditional now hoists the `final <T> …; if/else-if/else` ladder form
        // (buildIfThenElseHoistBlock's non-initializer branch); the MULTI gate
        // below still declines the list-seat (Collections.emptyList() else) form.
        if (ctx.scope() == null) {
            return null;
        }
        JavaStatementScope sink = ctx.scope().findStatementHoistSink();
        // facet inLambdaArgSeatIteHoist (PR #355): the LAMBDA_CHANNEL twin — the
        // same item-typed hoist for an arg-seat conditional INSIDE a map/extract
        // lambda (upstream's block-body lift, the deferred in-lambda variant the
        // #181 javadoc names). The block registers on the enclosing lambda's
        // pending-hoist channel (registerPendingLambdaHoist → compileLambda's
        // drain converts the expression lambda to a brace block, the #312
        // pattern); the naming rides the per-METHOD session's per-LAMBDA
        // sub-groups (registerLambdaScoped, keyed by the boundary scope —
        // singleton bare / 0..n-1 in consumption order / _-escaped under
        // method-level ifThenElseResult hoists), resolved at the method-wide
        // session.resolve pass — the #346-safe token pattern, no
        // GeneratedIdentifier renders at drain time. Declines (→ the inline
        // ternary, today's bytes) when: value-then hoisting is SUPPRESSED (an
        // enclosing conditional is rendering its arms as an inline ternary — the
        // P352A suppress-class law: statements cannot splice into a ternary
        // arm); the nearest enclosing lambda is not a drainable map/extract
        // lambda; or no session/boundary is reachable (session-less emission
        // paths). Green-safe by the #181 construction: zero goldens carry the
        // `getOrDefault(false) ? ` ternary in ANY seat, so every seat this arm
        // converts lived in an already-waivered file.
        StatementHoistSession lambdaSession = null;
        JavaStatementScope lambdaBoundary = null;
        if (sink == null) {
            // A conditional that IS the whole lambda body (its parent is the
            // RInlineFunction) belongs to the block-lambda family — the
            // elseless/effective-else/ladder if/return forms and the deep-then
            // Mapper restructure own that seat (the cp1 GetUnderlier* AWAY class:
            // splicing a bare item-typed local as the body's return renders a
            // form no golden carries). The arg-seat channel serves INTERIOR
            // consumption seats only.
            if (expr.parent() instanceof
                        com.regnosys.rosetta.ast.expressions.supporting.RInlineFunction
                    || ctx.scope().isThenValueHoistSuppressed()
                    || !HandlerHelper.isInsideDrainableMapLambdaAllowingConditionalArms(expr)) {
                return null;
            }
            // A seat inside an UNRESTRUCTURED runtime `.then(` lambda declines —
            // golden restructures those chains FIRST (hoisted thenArg steps), so a
            // block spliced at the un-restructured nesting depth adds lines that
            // cannot align (the cp3 Create_SubmissionSchedules / AnnaDsb AWAY
            // class). Render truth via the #350 handshake: a RESTRUCTURED then
            // body is thenArg-BOUND in scope; an unbound then-body ancestor means
            // the chain still renders `.then(`.
            // facet thenBindingOverlay (v3.1 C2d retirement family 9, PR #616) — VERDICT-MOVED
            // RETIRE-AFTER-CENSUS -> JUSTIFIED-KEPT (the seat-30 bar; S27), and HAZARD H1 RESOLVED:
            // this is the same `while` loop byte-for-byte as ConstructionHandler:1394 and :3580,
            // which the triage verdicted JUSTIFIED-KEPT-CANDIDATE on an incompatible reason —
            // identical code now carries an identical verdict on identical measurements. c9 census,
            // 7,786 arrivals (2,598 / 2,598 / 2,590) = 3,893 entry + 3,893 pass, sawUnbound=false
            // at 3,893/3,893: the third and LARGEST site never declines either, so the three sites
            // make 4,265 combined passes with ZERO declines. The channel the RAC verdict named —
            // "recording the restructure AST-keyed on the scope" — DOES NOT EXIST at this head, and
            // the nearest existing render-truth signal, the #356 chain-top window, is TRUE at 135
            // of the 3,893 passes (agreement 3,758/3,893, 96.53%), so substituting it would DECLINE
            // 135 item-local hoists the code admits today, on all three routes. ARITHMETIC
            // CORRECTED: the triage says "8 sibling seats"; the live count of `thenArgRefFor(...)
            // == null` gates is FIVE (S13, S25 x2, S27, S33). 28 carriers, fn:GetOptn 1,548 the
            // largest; node kind RConditionalExpr at 3,893/3,893. Locator drift recorded: the
            // triage's L2119-2125 is live at 2177-2183.
            RNode curThen = expr.parent();
            int thenDepth = 0;
            while (curThen != null && thenDepth++ < 64) {
                if (curThen instanceof com.regnosys.rosetta.ast.expressions.supporting
                            .RInlineFunction inlineThen
                        && inlineThen.parent() instanceof RThenExpr thenAncestor
                        && thenAncestor.body().orElse(null) == inlineThen
                        && ctx.scope().thenArgRefFor(inlineThen) == null) {
                    return null;
                }
                if (curThen instanceof RFunction
                        || curThen instanceof com.regnosys.rosetta.ast.functions.RRule) {
                    break;
                }
                curThen = curThen.parent();
            }
            lambdaBoundary = ctx.scope().findPendingLambdaHoistBoundary();
            lambdaSession = ctx.scope().findStatementHoistSessionAnyDepth();
            if (lambdaBoundary == null || lambdaSession == null) {
                return null;
            }
        }
        GeneratorModel gm = compiler.getGeneratorModel();
        var translator = compiler.getTypeTranslator();
        JavaTypeUtil typeUtil = compiler.getTypeUtil();
        // Mirror the A2 arm's guard — typeUtil is dereferenced in
        // thenItemJavaClass's literal/conversion branches; decline (not crash)
        // on a partial compiler lacking it (ExpressionCompiler permits a null
        // typeUtil; unreachable on the function-body sink path, where all three
        // are non-null, but defensive-consistent with hoistAsComparisonResultOrNull).
        if (gm == null || translator == null || typeUtil == null) {
            return null;
        }
        // A workspace-cardinality MULTI guard stood here from the seat's birth and
        // was removed at the seat-26 close as unreachable: this method's parameter
        // is the conditional itself, and the global-path computer is hardwired
        // `case RConditionalExpr c -> SINGLE` (CardinalityComputer's #289
        // reportOutputConditionalCardinality law), so the guard — and the seat-26
        // law-D collapse exception carved into it — never ran. Law D's live half is
        // the `default`-operand leaf-typing arm below.
        JavaClass<?> declWork = thenItemJavaClass(expr.thenBranch(), gm, translator, typeUtil,
                compiler);
        // facet inLambdaArgSeatIteHoist (PR #355): a NESTED-THEN arm folds into the
        // same local (buildIfThenElseHoistBlock's inner if/else), so the decl types
        // from the nested conditional's own branches (golden esma GetOptn:
        // `String ifThenElseResult0` under `if … { if len>3 { =subString… } else
        // { =…get(); } }`); and a null/empty THEN with a REAL else types from the
        // ELSE branch (upstream joins the branch types and the null literal joins
        // away — golden asic Create_ContractType15__1 `final CurrencyExchange23__1
        // ifThenElseResult5; if … { = null; } else { = …builder()….build(); }`).
        // Both recoveries are decline→hoist transitions at zero-golden ternary
        // seats (green-safe by the shared construction).
        // The fallbacks below DECLINE at a then-chain BASE seat (the conditional is
        // an RThenExpr's argument): that consumer renders a Mapper-typed decl
        // (`final MapperS<X> thenArgN = …`), owned by the #351 Mapper-form ite
        // machinery — an ITEM-typed local spliced there is a form no golden carries
        // (the cp4 AnnaDsb AWAY: one bad insertion + 45 renumber ripples). The
        // pre-#355 then-typed primary lookup above is untouched either way.
        boolean fallbackSeat = !(expr.parent() instanceof RThenExpr);
        if (declWork == null && fallbackSeat
                && expr.thenBranch() instanceof RConditionalExpr nestedThen) {
            declWork = thenItemJavaClass(nestedThen.thenBranch(), gm, translator, typeUtil,
                    compiler);
            if (declWork == null && nestedThen.elseBranch().isPresent()) {
                declWork = thenItemJavaClass(nestedThen.elseBranch().get(), gm, translator,
                        typeUtil, compiler);
            }
        }
        if (declWork == null && fallbackSeat && hasEffectiveElse(expr)
                && !(expr.elseBranch().get() instanceof RConditionalExpr)) {
            declWork = thenItemJavaClass(expr.elseBranch().get(), gm, translator, typeUtil,
                    compiler);
        }
        // facet ctorSetterNavCollapseType (PR #365): an ALIAS-CALL-rooted nav-chain
        // then-arm (`fpmlVariance(fpmlVarianceLeg) -> varianceAmount -> currency ->
        // value`) resolves NULL through resolveReceiverRType (the alias-call root)
        // AND carries an EMPTY resolvedFeature (the known alias-rooted IR gap), so
        // type the local from the LEAF attribute via onlyElementLeafAttribute — whose
        // gm-aware fallbackResolveFeature IS the resolution the rendered `<String>`
        // witness on the same line uses (the #178 same-walk law). Golden cdm6
        // MapVarianceLegToVarianceReturnTerms: `String ifThenElseResult1 = null;
        // if (exists(…)) { ifThenElseResult1 = fpmlVariance(…)….get(); }` +
        // `.setCurrencyValue(ifThenElseResult1)`; the Asset sibling renumbers 1→2 by
        // the session's consumption order. Green-safe by the #181 construction (zero
        // goldens carry the seat ternary).
        // SEAT-gated to the ctor-FIELD value (RKeyValuePair parent): the #181
        // seat-agnostic fire is only harmless where a decline follows — at a
        // chain-RECEIVER seat the newly-typable hoist RENDERS a form golden keeps
        // inline (the cp8c Contract_Price_Monetary over-fire: golden carries ZERO
        // ifThenElseResult tokens; the ratio flagged TOWARD but the content grew a
        // non-golden hoist + renumber — the #332/#356 both-directions law).
        // facet fnNotionalTogetherRestructure (PR #398): the DISGUISED 2-name sibling at
        // the ctor-FIELD seat — `quantitySchedule -> unit` parses as an unresolved
        // REnumValueRef (the #371 disguised-nav law), so the RFeatureCall gate missed
        // it (the T398H probe: declWork=null at both ladder fields);
        // onlyElementLeafAttribute routes the disguised class through the SAME
        // resolveDisguisedFeature walk the nav render uses (the #395 arg-seat
        // precedent at this ctor-FIELD seat — golden NotionalLeg interestRateNotional/
        // cashflowNotional `final UnitType ifThenElseResult0;` else-if ladders).
        if (declWork == null && fallbackSeat
                && (expr.thenBranch() instanceof RFeatureCall
                        || (expr.thenBranch() instanceof REnumValueRef fieldEvr
                                && fieldEvr.enumeration().isEmpty()))
                && expr.parent()
                        instanceof com.regnosys.rosetta.ast.expressions.supporting.RKeyValuePair) {
            RAttribute thenLeafAttr =
                    NavigationHandler.onlyElementLeafAttribute(expr.thenBranch(), compiler);
            if (thenLeafAttr != null) {
                declWork = toRefOrNull(translator, unalias(gm.getType(thenLeafAttr)));
            }
        }
        // facet stmtSeatItemCondNullInit (seat 26, law D): the `default`-operand ARG
        // seat — a then-arm whose workspace snapshot is blind (the deep-path `->>`
        // leaf under an only-element collapse: `… -> Product ->> economicTerms
        // only-element`) types the local from the LEAF attribute through the SAME
        // onlyElementLeafAttribute walk the #365 ctor-field arm uses (golden drr
        // EconomicTermsForProduct: `EconomicTerms ifThenElseResult = null; if
        // ((_boolean == null ? false : _boolean)) { ifThenElseResult = <nav>.get(); }
        // … .getOrDefault(ifThenElseResult)` — the fork's inline ternary handed
        // MapperC poles to getOrDefault(EconomicTerms) and never compiled, the LAW-74
        // PRE class). SEAT-gated to the RDefaultExpr operand (the cp8c over-fire law);
        // an unresolvable leaf keeps the ternary (today's bytes).
        if (declWork == null && fallbackSeat
                && expr.parent()
                        instanceof com.regnosys.rosetta.ast.expressions.binary.RDefaultExpr defaultOwner) {
            RAttribute defaultLeafAttr =
                    NavigationHandler.onlyElementLeafAttribute(expr.thenBranch(), compiler);
            if (defaultLeafAttr != null) {
                declWork = toRefOrNull(translator, unalias(gm.getType(defaultLeafAttr)));
            }
            // The deep-path `->>` leaf resolves through NEITHER walk — the DEFAULT's
            // LEFT operand joins the same output type (rune's default semantics), so
            // its plain nav leaf is the render-truth type source (`product ->
            // economicTerms` → EconomicTerms).
            if (declWork == null && defaultOwner.left().isPresent()) {
                RExpression defaultLeft = defaultOwner.left().get();
                RAttribute leftLeaf = defaultLeft instanceof RFeatureCall defaultLeftNav
                        ? NavigationHandler.navLeafAttrOrNull(defaultLeftNav, compiler)
                        // `product -> economicTerms` parses as a DISGUISED nav (an
                        // unresolved REnumValueRef — the #371 disguised-nav law), so
                        // the leaf routes through the SAME onlyElementLeafAttribute
                        // walk the #365/#398 arms use.
                        : NavigationHandler.onlyElementLeafAttribute(defaultLeft, compiler);
                if (leftLeaf != null) {
                    declWork = toRefOrNull(translator, unalias(gm.getType(leftLeaf)));
                }
            }
        }
        // facet ctorArgAliasRefIteType (PR #392): a BARE alias-ref then-arm at the
        // ctor-FIELD seat types the local from the SAME signature walk that renders
        // the alias method (tryAliasReceiverMapperType — the #327-B1 meta-item gate;
        // the arm value then takes the Mapper `.get()` deref per the wrapperToItem
        // law). Golden cdm6 MapGenericProductPriceQuantityList: `final
        // FieldWithMetaString ifThenElseResult; if (exists(unitFromQuotation(…))
        // .getOrDefault(false)) { ifThenElseResult = unitFromQuotation(…).get(); }
        // else { ifThenElseResult = unitFromNotional(…).get(); }` consumed bare by
        // `.setCurrency(ifThenElseResult)`. SEAT-gated like the #365 arm above (the
        // cp8c over-fire law); a model-typed alias walks to null → the ternary
        // (today's bytes). Green-safe by the #181 construction (zero goldens carry
        // the seat ternary).
        if (declWork == null && fallbackSeat
                && expr.parent()
                        instanceof com.regnosys.rosetta.ast.expressions.supporting.RKeyValuePair
                && expr.thenBranch() instanceof RSymbolReference aliasArm
                && aliasArm.args().isEmpty()
                && aliasArm.symbol().filter(s ->
                        s instanceof com.regnosys.rosetta.ast.functions.RShortcut).isPresent()) {
            JavaType aliasMapper =
                    NavigationHandler.tryAliasReceiverMapperType(expr.thenBranch(), compiler);
            JavaType aliasItem = aliasMapper == null ? null : typeUtil.getItemType(aliasMapper);
            if (aliasItem instanceof JavaClass<?> aliasItemClass) {
                declWork = aliasItemClass;
            }
        }
        // facet inLambdaArgBareSymbolIteType (PR #392): at the fn-call ARG seat (the
        // #355 lambda channel — parent is the invocation's RSymbolReference), a
        // BARE-symbol then-arm that is an implicit-item FEATURE types the local from
        // the SAME walk the bare-symbol nav synthesis renders from
        // (implicitItemDataTypeOrInferred + findAttributeOnDataType — the #384
        // render-truth pattern; golden cdm6 MapGenericProductPriceQuantityList
        // `Currency ifThenElseResult = null; if (exists(item.<Currency>map(
        // "getCurrency", …)).getOrDefault(false)) { ifThenElseResult = item
        // .<Currency>map("getCurrency", …).get(); }` consumed bare at
        // `mapCurrency.evaluate(ifThenElseResult)`). A resolvable-elsewhere or
        // meta-free-missing name walks to null → the ternary (today's bytes) —
        // green-safe by the #181 zero-golden-ternary construction.
        if (declWork == null && fallbackSeat
                && expr.parent() instanceof RSymbolReference argOwner
                && argOwner.symbol().filter(s -> s instanceof RFunction).isPresent()
                && expr.thenBranch() instanceof RSymbolReference bareSym
                && bareSym.args().isEmpty() && bareSym.symbol().isEmpty()
                && bareSym.name() != null) {
            com.regnosys.rosetta.ast.types.RDataType bareItemT =
                    NavigationHandler.implicitItemDataTypeOrInferred(bareSym, compiler);
            RAttribute bareFeatAttr = bareItemT == null ? null
                    : HandlerHelper.findAttributeOnDataType(bareItemT, bareSym.name());
            if (bareFeatAttr != null) {
                declWork = toRefOrNull(translator, unalias(gm.getType(bareFeatAttr)));
            }
        }
        // facet fnThenFlattenLoLDecomp (PR #395): at the SAME fn-call ARG seat (the #392
        // arm above), a NAV-CHAIN then-arm types the local from its LEAF attribute via
        // onlyElementLeafAttribute — the #365 ctorSetterNavCollapseType walk widened from
        // the ctor-FIELD seat to the arg-owner seat (both SEAT-gated per the #365 cp8c
        // over-fire law: a chain-RECEIVER seat keeps the decline). A META leaf types the
        // BARE value (gm.getType reads the typeCall — `currency [metadata scheme]` →
        // String); the #368 F-A3 iteArmMetaCollapseDeref arm then hoists the
        // fieldWithMetaStringN wrapper local in-arm and assigns the guarded deref
        // (golden GRD ifThenElseResult2). Golden GRD's evaluate args: `final Quantity
        // ifThenElseResult0;` / `final BigDecimal ifThenElseResult1;` blank-final
        // else-if ladders with `else { … = null; }` terminals inside the mapItem block
        // lambda. Green-safe by the #181 construction (zero goldens carry the seat
        // ternary this replaces).
        if (declWork == null && fallbackSeat
                && expr.parent() instanceof RSymbolReference navArgOwner
                && navArgOwner.symbol().filter(s -> s instanceof RFunction).isPresent()
                && (expr.thenBranch() instanceof RFeatureCall
                        // The DISGUISED 2-name sibling (`delivery -> deliveryCapacity`,
                        // an unresolved REnumValueRef — the alias-rooted GRD else-branch
                        // arm): onlyElementLeafAttribute routes it through the SAME
                        // resolveDisguisedFeature walk the nav render uses.
                        || (expr.thenBranch() instanceof REnumValueRef argEvr
                                && argEvr.enumeration().isEmpty()))) {
            RAttribute argLeaf =
                    NavigationHandler.onlyElementLeafAttribute(expr.thenBranch(), compiler);
            if (argLeaf != null) {
                declWork = toRefOrNull(translator, unalias(gm.getType(argLeaf)));
            }
        }
        // facet inLambdaArgSeatIteHoist (PR #355): the numeric JOIN — an
        // Integer-typed then (an int literal) with a BigDecimal-typed else widens
        // the local to BigDecimal (upstream types the local at the join of the
        // seat-coerced branch types; golden asic/mas PriceRule `final BigDecimal
        // ifThenElseResult; … = BigDecimal.valueOf(99999999999l); … =
        // packageTransactionPriceNoFormatRule.evaluate(item.get());`). Green-safe:
        // an Integer local assigned a beyond-int literal never compiled, so every
        // affected seat lived in a waivered file.
        if (declWork != null
                && ("Integer".equals(declWork.getSimpleName())
                        || "Long".equals(declWork.getSimpleName()))
                && hasEffectiveElse(expr)
                && !(expr.elseBranch().get() instanceof RConditionalExpr)) {
            JavaClass<?> elseItem =
                    thenItemJavaClass(expr.elseBranch().get(), gm, translator, typeUtil,
                            compiler);
            if (elseItem != null && "BigDecimal".equals(elseItem.getSimpleName())) {
                declWork = elseItem;
            }
        }
        // v3.2 seat 13 (D53 - the M5b heal's second mechanism, the chaos s23 C23CmpArm rows): a conditional at a
        // COMPARISON / EQUALITY operand types its Mapper-form slot at the operator's JOIN - upstream compiles both
        // operands against `MAPPER.wrapExtends(joined)`, so int-literal arms beside a `number` sibling declare
        // `final MapperS<BigDecimal>` and coerce `BigDecimal.valueOf(N)` (ComparisonHandler's literal-sibling law at
        // the conditional-operand seat; golden C23CmpArm `ifThenElseResult1 = MapperS.of(BigDecimal.valueOf(1));`
        // against the fork's `MapperS<Integer>` / `MapperS.of(1)`, which lessThan cannot apply to a BigDecimal
        // sibling - LAW 74). An Integer or untypable sibling keeps the Integer slot byte-identically.
        boolean comparisonJoinWork = false;
        if (declWork != null && "Integer".equals(declWork.getSimpleName())
                && (expr.parent() instanceof RComparisonExpr || expr.parent() instanceof REqualityExpr)
                && expr.parent() instanceof com.regnosys.rosetta.ast.RBinaryExpression cmpParent) {
            RExpression sibling = cmpParent.rawLeft() == expr ? cmpParent.rawRight() : cmpParent.rawLeft();
            if (HandlerHelper.numericOperandKind(sibling, compiler) == HandlerHelper.NumericKind.NUMBER) {
                declWork = HandlerHelper.BIG_DECIMAL;
                comparisonJoinWork = true;
            }
        }
        final boolean comparisonJoin = comparisonJoinWork;
        // facet blankFinalCtorLadderEnumJoin (PR #382, E-i): a #204-class MIS-BOUND bare
        // then-arm (`CashPrice` binds the RDataType CashPrice — the bare-symbol root-
        // element collision) types from a SIBLING ladder arm's parser-BOUND enum value
        // (`InterestRate` → REnumValue → PriceTypeEnum) when the mis-bound name matches
        // a value in that enum's hierarchy — rune's join semantics collapse the arms to
        // ONE type, and the sibling binding is the evidence channel (the #354/#358
        // requalify family at the ladder-arm seat; golden cdm6 MapExecutionDetails
        // `final PriceTypeEnum ifThenElseResult; if … { = PriceTypeEnum.CASH_PRICE; }
        // else if … { = PriceTypeEnum.INTEREST_RATE; } else { = null; }` consumed bare
        // by `.setPriceType(ifThenElseResult)`). Green-safe by the #181 construction:
        // this fires only where every existing recovery declined to the zero-golden
        // ternary, so every affected seat lives in a waivered file.
        com.regnosys.rosetta.ast.types.REnumeration ladderEnumJoinWork = null;
        if (declWork == null && fallbackSeat) {
            com.regnosys.rosetta.ast.types.REnumeration sibEn =
                    ladderSiblingBoundEnumeration(expr);
            if (sibEn != null
                    && misBoundLadderArmEnumValue(expr.thenBranch(), sibEn) != null) {
                ladderEnumJoinWork = sibEn;
                declWork = toRefOrNull(translator, new REnumTypeRef(sibEn));
            }
        }
        final com.regnosys.rosetta.ast.types.REnumeration ladderEnumJoin = ladderEnumJoinWork;
        if (declWork == null) {
            return null;
        }
        final JavaClass<?> declClazz = declWork;
        // facet listLiteralMapperIteSlot (PR #357): a conditional that IS a LIST-LITERAL
        // element hoists the MAPPER-form slot — upstream compiles literal elements at the
        // Mapper element type, so the slot declares `final MapperS<X>`, the arms assign
        // `MapperS.of(<item>)`, and the absent else takes the trailing
        // `MapperS.<X>ofNull()` else branch (the #328 blank-final mechanism; golden fca
        // Create_CounterpartySpecificData `final MapperS<OrganisationIdentification15
        // Choice__1> ifThenElseResult2; if … { = MapperS.of(…builder()….build()); } else
        // { = MapperS.<…>ofNull(); }`, consumed verbatim by `MapperC.<…>of(r2, r3)`).
        // Green-safe by construction: MapperC.of accepts only List/Mapper arguments, so
        // the pre-#357 bare-typed slot spliced into MapperC.of never compiled.
        // facet fnIteHoistSeats (PR #361): an ARITHMETIC-OPERAND conditional takes the
        // SAME Mapper-form slot — upstream compiles arithmetic operands against
        // MAPPER.wrapExtends(joined) (the #334 wrapper-level law), so the consumption
        // seat is Mapper-typed and declareAsVariable types the local from it (golden
        // cdm5/cdm6 GenerateObservationDates `final MapperS<Integer> ifThenElseResult;
        // if (exists(MapperS.of(lockoutDays)).getOrDefault(false)) { = MapperS.of(
        // lockoutDays); } else { = MapperS.of(0); } return MapperMaths.<Integer,
        // Integer, Integer>add(MapperS.of(1), ifThenElseResult);` — the sentinel
        // splices BARE as the operand). An ITEM-form local at a MapperMaths parameter
        // never compiles, so every converted seat lived in a waivered file.
        // facet defaultOperandMapperIteSlot (PR #383): the DEFAULT-operator LEFT operand
        // joins the Mapper-form slot family — upstream renders `<left>.getOrDefault(<right>)`
        // (the left consumed AS a Mapper), so the hoisted local types MapperS and the arms
        // wrap (golden drr TotalNotionalQuantityLeg1: `final MapperS<BigDecimal>
        // ifThenElseResult; … = MapperS.of(<fn>.evaluate(…)); … = MapperS.<BigDecimal>
        // ofNull();` consumed by `MapperS.of(ifThenElseResult.getOrDefault(defaultValue))`).
        // LEFT-only: the RIGHT operand is getOrDefault's ITEM argument, never a Mapper. An
        // ITEM-form local at a getOrDefault receiver never compiles, so every converted
        // seat lived in a waivered file (the #357/#361 green-safety argument).
        // facet condExtractReceiverMapperSlot (W42 finding #20, PR #432): a conditional
        // that IS an extract's RECEIVER joins the Mapper-form slot family — upstream
        // compiles the map receiver against MAPPER_S.wrapExtends(argument)
        // (ExpressionGenerator.caseMapOperation's single branch), so the consumption
        // seat is Mapper-typed exactly like the #361 arithmetic operand: the slot
        // declares `final MapperS<X>`, the arms assign `MapperS.of(<item>)`, and the
        // chain's `.mapSingleToItem(…)` lands on a Mapper local (upstream
        // FunctionGeneratorTest canChainAfterConditional: `(if True then 42 else 0)
        // extract item + 1`). SINGLE-ARMED conditionals only — the guard reads the
        // ARM cardinalities directly (the Seat-1 #432 MF-1: the whole-node
        // compute() returns SINGLE for EVERY conditional on the non-thenAware
        // path, so a whole-node read is inert): a MULTI arm would bind MapperC,
        // out of evidence, and declining keeps the arm's loud pre-#432 bytes (the
        // bare-typed local + the Mapper-member CFS — never a silent collapse).
        // Green-safe by the #357/#361 construction: an ITEM-form local under a
        // Mapper member call never compiles, so no green file carries the
        // bare-typed form at this seat.
        // facet condMapperFormSeats (seat 7, Rung M): the RULE-path then-body-ROOT
        // conditional joins the Mapper-form slot family — the consumer is the rule's
        // output-assign chain, where golden is ALWAYS the Mapper-typed hoist consumed
        // `.get()` (the seat-7 charter scans over all 34,686 goldens: the replaced
        // consumer shape `output = ifThenElseResultN;` has ZERO golden carriers vs
        // 755 files carrying `output = ifThenElseResultN.get();` — golden hkma
        // UnderlyingIdentificationTypeRule + csa CryptoAssetUnderlyingIndicatorLeg1/2).
        // The bare DECL alone is legitimate golden form at evaluate-arg/ctor seats, so
        // the gate keys on the CONSUMER SEAT (a sink path + the conditional IS the
        // then-body root + the RULE path), never on the decl shape — the seat suite's
        // b5 evaluate-arg pin freezes that boundary. Meta-wrapper decls
        // (RJavaWithMetaValue) keep the #328 builder-empty form byte-identically
        // (that class has GREEN carriers and no Mapper-form evidence). The sentinel
        // returns TYPED non-self-unwrapping below so the SET consumer's
        // unwrapForAssignment fall-through appends the `.get()`.
        final boolean ruleThenBodyRootSlot = sink != null
                && expr.parent() instanceof
                        com.regnosys.rosetta.ast.expressions.supporting.RInlineFunction rootFn
                && rootFn.body() == expr
                && HandlerHelper.findEnclosingRule(expr) != null
                && !(declClazz instanceof RJavaWithMetaValue);
        // facet extractBodyMultiDefaultTernary (v3.1 flip seat 32, law D2), rung 2: the
        // DEFAULT-operator RIGHT operand joins the Mapper-form slot family EXACTLY where the
        // default renders the list-form ternary. The #383 disjunct below is LEFT-only for a
        // stated reason - "the RIGHT operand is getOrDefault's ITEM argument, never a Mapper"
        // - and that reason is TRUE of the single `getOrDefault` render and FALSE of the
        // ternary render, whose right IS a Mapper-typed arm (upstream compiles it at
        // MAPPER.wrapExtends(joined), ExpressionGenerator.xtend L459). Golden drr 7.x
        // IndicatorOfTheUnderlyingIndexRule: `final MapperS<String> ifThenElseResult; ... }
        // else { ifThenElseResult = MapperS.<String>ofNull(); }` against the fork's
        // `String ifThenElseResult = null;` with no else arm at all.
        // The gate is the ONE shared predicate the render arm reads
        // (NavigationHandler.extractBodyMultiDefaultTernary - the #367 single-predicate law):
        // the NECESSARY condition both halves key on. The render arm carries its own
        // preconditions on top (a resolvable joined element from the LEFT's compiled stamp via
        // defaultJoinBareElement, plus the type-util / coercion-service / JavaExpression guards)
        // and this slot its own (the hoist path being reached at all); at every measured carrier
        // both fire - the 4 [P32-DEF] rows, leftType=MapperC<FieldWithMetaString> (the seat-32
        // review's L2-05 correction). Every other RIGHT-operand conditional in the
        // corpus keeps the item form byte-identically (the predicate's measured blast radius:
        // ZERO distinct non-carrier `where=` values, the green alias-scoped
        // MapBasketReferenceInformation explicitly excluded).
        final boolean ternaryArmSlot = expr.parent()
                        instanceof com.regnosys.rosetta.ast.expressions.binary.RDefaultExpr
                                ternaryDflt
                && ternaryDflt.rawRight() == expr
                && NavigationHandler.extractBodyMultiDefaultTernary(ternaryDflt, compiler);
        final boolean mapperFormSlot = ruleThenBodyRootSlot
                || ternaryArmSlot
                || expr.parent() instanceof RListLiteral
                || expr.parent() instanceof com.regnosys.rosetta.ast.expressions.binary.RArithmeticExpr
                // v3.2 seat 2 (Law 5, the chaos C5Seats rows): a COMPARISON operand is a Mapper seat
                // — upstream's binaryExpr compiles every operand against `MAPPER.wrapExtends(joined)`,
                // so the conditional's collapse local is the Mapper-typed `final MapperS<BigDecimal>
                // ifThenElseResult;` (golden C5Seats' condition seat); the item form the fork rendered
                // here handed a bare BigDecimal to greaterThanEquals (LAW 74: `cannot be applied to
                // given types`). The EQUALITY parent is NOT admitted (PR #623's witness sweep): no
                // carrier reaches a conditional at an equality operand, lane Y stayed green with it
                // on, so it was withdrawn under the #614 law and banked.
                || expr.parent() instanceof com.regnosys.rosetta.ast.expressions.binary.RComparisonExpr
                // v3.2 seat 13 (D53 - the M5b heal, the chaos s23 C23CmpArm rows and the hold-out byte pin
                // ExistsThenConditional of #630, 13 of 13 non-compiling by the seat-13 census): the EQUALITY parent
                // joins the Mapper-form slot family - upstream's binaryExpr compiles an equality's operands against
                // `MAPPER.wrapExtends(joined)` exactly as a comparison's, so `(if flag then "a" else "b") = "a"` hoists
                // `final MapperS<String> ifThenElseResult0;` with MapperS.of arms (golden C23CmpArm); the item form the
                // fork rendered here (`String ifThenElseResult0 = "b";`) handed a bare String to areEqual (LAW 74).
                // PR #623 had withdrawn this disjunct for want of a carrier; the chaos cell and the pin now carry it.
                || expr.parent() instanceof REqualityExpr
                || (expr.parent() instanceof com.regnosys.rosetta.ast.expressions.binary.RDefaultExpr dfltParent
                        && dfltParent.rawLeft() == expr)
                || (expr.parent() instanceof RExtractExpr extRecvParent
                        && extRecvParent.argument() == expr
                        && CARDINALITY.compute(expr.thenBranch()) != ExpressionCardinality.MULTI
                        && expr.elseBranch()
                                .map(e -> CARDINALITY.compute(e) != ExpressionCardinality.MULTI)
                                .orElse(true));

        // facet ctorCondSingleCoerce (seat 28, law A): the consumer->conditional handshake
        // the ctor pair seat pushed for EXACTLY this node. Read HERE - after declClazz (the
        // ONE type walk) and after mapperFormSlot - because the fire verdict needs both:
        // the differs test compares the walk against the seat's expected item type, and a
        // Mapper-form slot is a DIFFERENT declaration law (the #357/#361/#383/#432/seat-7
        // family) whose local is a MapperS, never an item. `null` = decline = today's bytes
        // in every byte of this method.
        final CondSingleCoerce single =
                condSingleCoerceOrNull(expr, declClazz, mapperFormSlot, ctx, compiler, typeUtil);
        final JavaClass<?> singleDeclClazz = single == null ? declClazz : single.targetType;
        // facet condListCoerce (PR #327): the local's name-group entry registers AFTER
        // the arms compile (the block builds against a placeholder, substituted below) —
        // upstream declareAsVariable runs at CONSUMPTION, so a conditional nested in an
        // arm VALUE numbers its own ifThenElseResult FIRST (the fca
        // Create_TradeCounterpartyReport20__1 inner-before-outer order). Order-neutral
        // for every arm that registers nothing in the group.
        String placeholder = "__CONDL_ITE_PLACEHOLDER__";
        Set<JavaClass<?>> refs = new HashSet<>();
        Set<JavaClass<?>> wildcards = new HashSet<>();
        refs.add(declClazz);
        final StatementHoistSession lambdaNaming = lambdaSession;
        final JavaStatementScope lambdaKey = lambdaBoundary;
        HoistArmRenderer baseArms = new HoistArmRenderer() {
            @Override
            public String renderArm(RExpression arm) {
                // facet blankFinalCtorLadderEnumJoin (PR #382, E-i): a #204-class
                // mis-bound bare enum arm renders the hierarchy-qualified constant
                // (the #358 flatten law's qualification at the ladder-arm seat;
                // golden `ifThenElseResult = PriceTypeEnum.CASH_PRICE;`) — the
                // SAME misBoundLadderArmEnumValue evidence the decl-type recovery
                // fired on, so the two cannot disagree. Runs BEFORE the arm
                // compile (the mis-bound compile would only contribute junk refs);
                // the parser-BOUND sibling arm falls through to the normal compile
                // (ReferenceHandler's REnumValue branch → the dotted constant).
                if (ladderEnumJoin != null && arm instanceof RSymbolReference misEnumRef) {
                    com.regnosys.rosetta.ast.supporting.REnumValue misEv =
                            misBoundLadderArmEnumValue(misEnumRef, ladderEnumJoin);
                    if (misEv != null) {
                        return declClazz.getSimpleName() + "."
                                + com.regnosys.rosetta.generator.java.enums.EnumHelper
                                        .convertValue(misEv);
                    }
                }
                // facet inLambdaArgSeatIteHoist (PR #355): an INT-LITERAL arm at a
                // BigDecimal-typed slot compiles at the slot's expected type on the
                // LAMBDA channel — upstream compiles every consumption arm against
                // the seat's Java type, so the literal coerces to
                // `BigDecimal.valueOf(N)` / `BigDecimal.valueOf(Nl)` (the
                // mechanism-4 conditionalArmExpectedType law at this seat; golden
                // asic PackageTransactionPriceRule `BigDecimal.valueOf(99999999999l)`).
                // The wrap factory carries the unwrap contract, so the shared
                // unwrap branch below strips to the bare coerced item. The SINK
                // path keeps its null-expected compile byte-identically.
                // facet ctorCondSingleCoerceLiteralArm (v3.1 flip seat 32, law B2): the
                // #355 lambda-channel BigDecimal arm seat keys on declClazz (the WALK
                // type), which is no longer the slot type once the ctor handshake fires -
                // the slot is `single.targetType`. Whenever `single` is non-null the
                // differs-gate has already established declClazz != targetType by a
                // witnessed numeric conversion, so a BigDecimal declClazz there means an
                // Integer/Long slot and this rung would compile the literal at the WRONG
                // type. `single == null` is every byte of today's behaviour.
                // v3.2 seat 13 (D53 - the M5b heal): the comparison-JOIN slot compiles its int-literal arms at
                // BigDecimal on the SINK path too (`MapperS.of(BigDecimal.valueOf(1))`, golden C23CmpArm); every
                // other sink-path arm keeps its null-expected compile byte-identically.
                JavaType armExpected = single == null && (lambdaNaming != null || comparisonJoin)
                        && arm instanceof RIntLiteral
                        && "BigDecimal".equals(declClazz.getSimpleName())
                        ? typeUtil.BIG_DECIMAL
                        : null;
                JavaStatementBuilder armBuilder = compiler.compile(arm, armExpected, ctx.scope());
                // v3.2 seat 13 (D53, site R10 - the chaos s30 C30TwoOp / s24 C24Segments rows): a WRAPPER-typed
                // local (the ctor-setter / pathed-leaf META seat) with a BARE-valued arm refuses HERE, before the
                // unwrap branch below strips the arm to the bare item it would assign (LAW 69: HandlerHelper's ONE
                // predicate, consulted by the FER pathed-conditional seat too).
                HandlerHelper.refuseIfBareValueIntoMetaLocal(arm, armBuilder, declClazz, compiler,
                        "a META-typed hoist local", () -> HandlerHelper.render(armBuilder instanceof JavaExpression armJeR
                                && armJeR.unwrapToBuilder().isPresent() ? armJeR.unwrapToBuilder().get() : armBuilder));
                // facet iteArmBareInvokableMetaDeref (PR #377, U): a bare-INVOKABLE arm
                // whose callee's declared OUTPUT is a META wrapper assigned to a
                // VALUE-typed local hoists the wrapper local INSIDE the owning branch
                // (the #327 arm mark/drain window relocates a statement hoist registered
                // during the arm render; the sink-less lambda channel rides the #368
                // F-A3 pending route) and assigns the guarded deref — golden cdm6
                // MapUnitTypeWithScheme ×3: `final FieldWithMetaCapacityUnitEnum
                // fieldWithMetaCapacityUnitEnum = mapCapacityUnitWithScheme.evaluate(
                // value, scheme); ifThenElseResult0 = fieldWithMetaCapacityUnitEnum ==
                // null ? null : fieldWithMetaCapacityUnitEnum.getValue();`. The BARE
                // null-init local stays (the F4 pin's law — the wrapper-typed local
                // lift was the AWAY-moving first cut). A wrapper-typed local or a
                // bare-output callee declines by the value-type equality; no placement
                // channel keeps today's bytes.
                if (!mapperFormSlot && typeUtil != null
                        && arm instanceof RSymbolReference armCallRef
                        && armCallRef.symbol().orElse(null) instanceof RFunction armCallee) {
                    RAttribute armOut = armCallee.output().orElse(null);
                    RJavaWithMetaValue outWrapper = armOut == null ? null
                            : NavigationHandler.metaWrapperOf(armOut, compiler);
                    JavaStatementScope armDerefSink = ctx.scope().findStatementHoistSink();
                    if (outWrapper instanceof JavaClass<?> wrapperClass
                            && outWrapper.getValueType() instanceof JavaClass<?> wrapperValue
                            && wrapperValue.getCanonicalName().withDots()
                                    .equals(declClazz.getCanonicalName().withDots())
                            && (armDerefSink != null || lambdaKey != null)) {
                        JavaStatementBuilder bareCall = armBuilder instanceof JavaExpression armJeU
                                ? armJeU.unwrapToBuilder().orElse(armBuilder)
                                : armBuilder;
                        String callSrc = HandlerHelper.render(bareCall);
                        refs.addAll(bareCall.getRefs());
                        refs.add(wrapperClass);
                        wildcards.addAll(bareCall.getStaticWildcardImports());
                        String wname = ctx.scope().disambiguate(
                                JavaNamingUtil.toFirstLower(wrapperClass.getSimpleName()));
                        String decl = "final " + wrapperClass.getSimpleName() + " " + wname
                                + " = " + callSrc + ";";
                        if (armDerefSink != null) {
                            armDerefSink.registerStatementHoist(decl);
                        } else {
                            Set<JavaClass<?>> declRefs = new HashSet<>(bareCall.getRefs());
                            declRefs.add(wrapperClass);
                            lambdaKey.registerPendingLambdaHoist(new JavaRawStatement(
                                    decl, declRefs, bareCall.getStaticWildcardImports()));
                        }
                        return wname + " == null ? null : " + wname + ".getValue()";
                    }
                }
                // ITEM-form value ladder (upstream wrapperToItem at the seat):
                // a structural wrap (MapperS.of(fnCall), selfUnwrapping ctor
                // block) strips to the bare inner — INNER refs only, the
                // discarded wrap must not pin a stale Mapper import; an enum
                // constant (by the producer's witness) and the null literal pass bare; a Mapper chain
                // appends .get().
                if (armBuilder instanceof JavaExpression je) {
                    Optional<JavaStatementBuilder> unwrap = je.unwrapToBuilder();
                    if (unwrap.isPresent()) {
                        JavaStatementBuilder inner = unwrap.get();
                        refs.addAll(inner.getRefs());
                        wildcards.addAll(inner.getStaticWildcardImports());
                        // facet listLiteralMapperIteSlot (PR #357): the Mapper-form slot
                        // re-wraps every non-null ITEM value MapperS.of(…) — the slot IS
                        // a Mapper (golden's literal-element law).
                        return mapperFormSlot
                                ? "MapperS.of(" + HandlerHelper.render(inner) + ")"
                                : HandlerHelper.render(inner);
                    }
                }
                refs.addAll(armBuilder.getRefs());
                wildcards.addAll(armBuilder.getStaticWildcardImports());
                String src = HandlerHelper.render(armBuilder);
                if ("null".equals(src)) {
                    return src;
                }
                if (HandlerHelper.isBareEnumConstant(armBuilder)) {
                    return mapperFormSlot ? "MapperS.of(" + src + ")" : src;
                }
                // facet iteArmMetaCollapseDeref (PR #368, F-A3): an arm whose Mapper
                // item is a META wrapper assigned to a VALUE-typed local hoists the
                // wrapper local INSIDE the owning branch (the #355 arm-relocation
                // window drains the pending registered here during renderArm) and
                // assigns the guarded deref — golden iosco OtherPaymentRule
                // `final FieldWithMetaString fieldWithMetaString0 = <chain>
                //     .first().get();
                // ifThenElseResult0 = fieldWithMetaString0 == null ? null :
                // fieldWithMetaString0.getValue();` (numbered by the per-lambda
                // sub-group across the payer/receiver arms). LAMBDA-channel-only
                // (a sink seat keeps its #364 arm-deref window byte-identically);
                // a wrapper-typed local (declClazz IS the wrapper) declines by the
                // value-type equality.
                // facet iteArmMetaCollapseDerefSinkChannel (v3.1 flip seat 33, law B.4 of
                // B.24): the #368 arm above is a CHANNEL widening, not a new rung — open its
                // gate to the STATEMENT-SINK seat as well as the lambda channel. A
                // method-statement ifThenElseResult ladder (QUOM's `then default if … then
                // … else …`) has no lambdaNaming/lambdaKey at all, so a wrapper-item arm
                // assigned to a value-typed local fell through to the bare `src + ".get()"` at
                // the terminal below and assigned the WRAPPER into a value-typed local — non-
                // compiling (LAW 74, javac33 C7 lines 479 and 482, one per arm), so every carrier
                // is already waivered. Golden drr 7.x QuantityUnitOfMeasure:78-84 hoists
                // `final FieldWithMetaNonNegativeQuantitySchedule fieldWithMetaNonNegative
                // QuantitySchedule{1,2} = <arm chain>.first().get();` INSIDE the owning branch and
                // assigns the guarded deref. LAW 69: the placement fork is the #377-U arm's
                // (`:2587-2612`, facet iteArmBareInvokableMetaDeref) verbatim — same sink
                // lookup, same drain windows. The channel is findStatementHoistSink() ONLY:
                // [P33-ITEARM] reads `armDerefSink=false` on every carrier row, so the #364
                // arm-deref sink is dead here and adding it would only widen the radius.
                // MEASURED radius, both routes: `lambdaCh=false armWrapper=true vtEqDecl=true` is
                // 36 rows over TWO `where=` — fn:QuantityUnitOfMeasure 16 and fn:Price 20 —
                // with ZERO green; Price's 20 are excluded by the EXISTING !mapperFormSlot
                // conjunct once its Mapper-form slot law has landed, which is why this law lands
                // after it. The value-type equality below is UNCHANGED: it is what declines the
                // 29-file PriorUti/PriorUTI population whose local IS the wrapper.
                JavaStatementScope armMetaSink = ctx.scope() == null
                        ? null : ctx.scope().findStatementHoistSink();
                if (!mapperFormSlot
                        && ((lambdaNaming != null && lambdaKey != null) || armMetaSink != null)
                        && armBuilder instanceof JavaExpression armJe
                        && typeUtil != null) {
                    JavaType armType = armJe.getExpressionType();
                    JavaType armItem = armType == null ? null : typeUtil.getItemType(armType);
                    if (armItem == null) {
                        // The collapse compile is TYPE-less (the P368A probe) — recover
                        // the CONCRETE wrapper from the LEAF attribute (the #365
                        // ctorSetterNavCollapseType law: the leaf resolution IS the one
                        // the rendered witness on the same chain used; metaWrapperOf,
                        // NOT toMetaJavaType — the latter returns the GENERIC
                        // FieldWithMeta<T>, the P368B probe).
                        RAttribute armLeaf =
                                NavigationHandler.onlyElementLeafAttribute(arm, compiler);
                        if (armLeaf != null) {
                            armItem = NavigationHandler.metaWrapperOf(armLeaf, compiler);
                        }
                    }
                    if (armItem instanceof RJavaWithMetaValue armWrapper
                            && armWrapper instanceof JavaClass<?> armWrapperClass
                            && armWrapper.getValueType() instanceof JavaClass<?> armValueClass
                            && armValueClass.getCanonicalName().withDots()
                                    .equals(declClazz.getCanonicalName().withDots())) {
                        // The PLACEMENT fork, the #377-U shape verbatim: the LAMBDA channel is
                        // byte-for-byte today's code; the SINK channel registers the same decl as
                        // a statement hoist on the sink the two arm windows drain
                        // (ControlFlowHandler:4201-4206 then-arm, :4391-4396 else-arm) and takes
                        // its name from the sink's OWN StatementHoistSession group — the #363
                        // `register(…)` idiom, never JavaStatementScope.disambiguate, which
                        // does not enter that group. That group membership is the whole reason
                        // this rung shares a commit with law B.2: golden numbers the three
                        // method-level locals fieldWithMetaNonNegativeQuantitySchedule0 (B.2's
                        // right-hand local, registered first), 1 (this then-arm) and 2 (this
                        // else-arm), and a group of the wrong size numbers every member wrongly.
                        // The refs of the arm chain are already in `refs`/`wildcards` (added at
                        // the ITEM-form terminal above), so the sink branch adds only the wrapper
                        // class, exactly as the lambda branch does.
                        if (lambdaNaming != null && lambdaKey != null) {
                            String wname = lambdaNaming.registerLambdaScoped(
                                    JavaNamingUtil.toFirstLower(armWrapperClass.getSimpleName()),
                                    lambdaKey);
                            Set<JavaClass<?>> declRefs = new HashSet<>(armBuilder.getRefs());
                            declRefs.add(armWrapperClass);
                            refs.add(armWrapperClass);
                            lambdaKey.registerPendingLambdaHoist(new JavaRawStatement(
                                    "final " + armWrapperClass.getSimpleName() + " " + wname
                                            + " = " + src + ".get();",
                                    declRefs, armBuilder.getStaticWildcardImports()));
                            return wname + " == null ? null : " + wname + ".getValue()";
                        } else if (armMetaSink != null) {
                            String sinkName = armMetaSink.statementHoistSession().register(
                                    JavaNamingUtil.toFirstLower(armWrapperClass.getSimpleName()));
                            refs.add(armWrapperClass);
                            armMetaSink.registerStatementHoist(
                                    "final " + armWrapperClass.getSimpleName() + " " + sinkName
                                            + " = " + src + ".get();");
                            return sinkName + " == null ? null : " + sinkName + ".getValue()";
                        }
                    }
                }
                // facet ctorSetterArgIteHoist (PR #397): a bare-FUNCTION-call arm renders
                // the RAW value (the #308 bare-invocation contract — evaluate() returns
                // the item, never a Mapper), so the Mapper-chain `.get()` append below is
                // a non-compiling form no golden carries (`ifThenElseResultN =
                // create_AnnaDsbEmpty.evaluate();` assigned BARE — golden AnnaDsb
                // BaseProductForCommodity ×30). A RULE call (always MapperS.of-wrapped)
                // took the unwrap branch above; an alias call (a Mapper-typed method,
                // RShortcut not RFunction) keeps the append.
                if (arm instanceof RSymbolReference bareFnArm
                        && bareFnArm.symbol().filter(s -> s instanceof RFunction).isPresent()) {
                    return mapperFormSlot ? "MapperS.of(" + src + ")" : src;
                }
                // (The two annotated paragraphs below are history for two retired rungs - the seat-31
                // rung i and the seat-32 rung 3; the LIVE guard is rung ii, the recovery block.)
                // facet mapperFormRuleRootArmKeep (v3.1 flip seat 31, law 1b): a RULE
                // then-body-ROOT slot does not re-present a Mapper-chain arm - the slot's
                // local is a MapperS and the chain already is one, so golden assigns it
                // DIRECT (mas PlatformIdentifierRule, the ONLY corpus carrier at this
                // terminal under ruleThenBodyRootSlot - [P31-MASFORM]/[P31-MASWRAP]
                // measured the six ruleRoot rules (21 rows) and only mas reaches here; the
                // fn:Create_* projection greens ride the OTHER mapperFormSlot disjuncts
                // and keep the wrap byte-identically). Since v3.2 seat 2 the GENERAL
                // terminal below supplies that identity for EVERY mapperFormSlot (the seat-2
                // verbatim arm, lane H + a9 + the C5Seats locks; ruleThenBodyRootSlot is one
                // of mapperFormSlot's disjuncts), so this law's rung i - `return src` - is the
                // terminal's own answer and was folded into it at PR #623 (round-2 cq review,
                // SF-2); what remains here is rung ii ONLY.
                // facet extractBodyMultiDefaultTernary (v3.1 flip seat 32, law D2), rung 3 -
                // RETIRED at v3.2 seat 2, PR #623 (round-1 cq review, SF-7) for the same
                // reason: the ternary-ARM slot's Mapper-chain arm (drr 7.x
                // IndicatorOfTheUnderlyingIndexRule `ifThenElseResult = <chain>.map("to-string",
                // FloatingRateIndexEnum::toDisplayString);`) is the terminal's identity too.
                // rung ii: a TYPE-LESS arm (the `extract <meta-output rule>` #265
                // construct block publishes no chain type) whose META wrapper is
                // recoverable from the arm NODE and whose value type IS the decl
                // class derefs IN-CHAIN - the SAME unrendered-arm coerce channel
                // law 1's rung B uses at the FER seat, handed an OPEN CHILD scope
                // (the seat-31 closed-scope law: this is a statement seat, and the
                // coercion's lambda param must not register into a scope the arm
                // compile may already have closed). Declines fall through to the
                // general terminal (behaviour-neutral: rung i's `return src` == the
                // terminal's `src` under mapperFormSlot).
                if (ruleThenBodyRootSlot
                        && compiler.getCoercionService() != null && typeUtil != null
                        && ctx.scope() != null
                        && armBuilder instanceof JavaExpression mfArmJe
                        && mfArmJe.getExpressionType() == null) {
                    RJavaWithMetaValue mfWrapper =
                            NavigationHandler.recoverExprMetaWrapper(arm, compiler);
                    if (mfWrapper != null
                            && mfWrapper.getValueType() instanceof JavaClass<?> mfValue
                            && mfValue.getCanonicalName().withDots()
                                    .equals(declClazz.getCanonicalName().withDots())) {
                        JavaStatementBuilder mfCoerced = compiler.getCoercionService()
                                .coerce(mfArmJe,
                                        typeUtil.wrap(typeUtil.MAPPER_S, mfWrapper),
                                        typeUtil.wrapExtends(typeUtil.MAPPER,
                                                mfWrapper.getValueType()),
                                        ctx.scope().lambdaScope());
                        if (mfCoerced instanceof JavaExpression) {
                            refs.addAll(mfCoerced.getRefs());
                            wildcards.addAll(mfCoerced.getStaticWildcardImports());
                            return HandlerHelper.render(mfCoerced);
                        }
                    }
                }
                // v3.2 seat 2 (Law 5, the chaos C5Seats rows): the fall-through arm is a Mapper CHAIN by
                // this renderer's own contract - the item form appends `.get()` to it (golden's ctor-
                // setter item ladders prove that read), so in the Mapper form the same chain is ALREADY
                // the Mapper and splices verbatim (`ifThenElseResult = MapperS.of(it).<BigDecimal>map(
                // "getOpt", ...);`) - upstream's wrapper->wrapper coercion at a same-item join is
                // identity - which is why both the rule-then-root slot's rung i (folded in here at
                // PR #623's round-2 review) and the ternary-arm slot's rung 3 (retired at its round-1
                // review) are this terminal's own answer. The pre-seat `MapperS.of(<chain>.get())`
                // round trip was the item-form text wrapped again - a form no golden carries (it
                // also drops the chain's own nullness).
                return mapperFormSlot ? src : src + ".get()";
            }

            @Override
            public String emptyElseValue() {
                // facet metaIteArmWrapperTyping (PR #328, F4): a WRAPPER-typed local's
                // absent/empty else takes the #193 meta-EMPTY builder (upstream
                // empty(JavaType) for an RJavaWithMetaValue expected type) — which also
                // flips buildIfThenElseHoistBlock to the blank-final + trailing-else
                // form (golden MapNumberOfOptionsAndOptionEntitlementToQuantity
                // `final FieldWithMetaString ifThenElseResult0; … else {
                // ifThenElseResult0 = FieldWithMetaString.builder().build(); }`).
                // Bare-typed locals keep the null-initializer form byte-identically.
                // facet listLiteralMapperIteSlot (PR #357): the Mapper-form slot's
                // absent else is the typed MapperS.<X>ofNull() (same trailing-else
                // mechanism).
                if (mapperFormSlot) {
                    return "MapperS.<" + declClazz.getSimpleName() + ">ofNull()";
                }
                return declClazz instanceof RJavaWithMetaValue
                        ? declClazz.getSimpleName() + ".builder().build()"
                        : "null";
            }
        };
        // facet ctorCondSingleCoerce (seat 28, law A): the handshake wraps the arm renderer
        // rather than reaching inside it - every existing arm keeps its exact return value and
        // its exact declClazz-keyed behaviour, and the null-safe conversion to the seat's
        // expected type is layered on TOP of whatever the ladder produced (upstream compiles
        // each arm at the consumption seat's type; the fork's ladder produces the walk-typed
        // value and this step is the missing convertNullSafe). `renderArmStatements` is NOT
        // overridden: the interface default returns null and the base class does not override
        // it either, so the #396 statement-block arm path is unreachable at this seat and
        // there is no arm that assigns the target itself.
        final HoistArmRenderer arms = single == null ? baseArms : new HoistArmRenderer() {
            @Override
            public String renderArm(RExpression arm) {
                return condSingleArmValue(arm, baseArms.renderArm(arm), declClazz,
                        single, ctx, compiler, lambdaNaming, lambdaKey, typeUtil,
                        refs, wildcards);
            }

            @Override
            public String emptyElseValue() {
                return baseArms.emptyElseValue();
            }
        };
        // facet fqnWitness (PR #227): emit the hoisted ifThenElseResult-local decl type as a
        // first-claim-wins sentinel so this (often the FIRST) cdm construction-type claim is seen
        // by the render-order resolver — without it a same-simple-name fpml witness lower in the
        // body would wrongly claim the bare name (a within-waiver regression on co-occupied files).
        // facet ctorCondSingleCoerce (seat 28, law A): under the handshake the sentinel
        // carries the CONSUMER's type - golden drr 5.61.0 `Integer ifThenElseResult3 = null;`
        // where the walk says BigDecimal. The walk type stays on refs: it is the in-arm value
        // local's declared type on the chain-arm carriers (GetPackg's `final BigDecimal
        // bigDecimal`) and is independently referenced by the function signature on the
        // bare-arm carriers, so no import is orphaned either way.
        String declTypeText =
                ImportCollisionResolver.typeRef(singleDeclClazz.getCanonicalName().withDots());
        if (single != null) {
            refs.add(single.targetType);
        }
        if (mapperFormSlot) {
            declTypeText = "MapperS<" + declTypeText + ">";
            refs.add(HandlerHelper.MAPPER_S);
        }
        String block = buildIfThenElseHoistBlock(expr, placeholder,
                declTypeText,
                compiler, ctx.scope(), arms, refs, wildcards,
                lambdaNaming == null ? null
                        : base -> lambdaNaming.registerLambdaScoped(base, lambdaKey));
        // facet ctorCondSingleCoerce (seat 28, law A): the block is built, so every arm ran
        // through the conversion wrapper and the decl carries the consumer's type - the
        // consumer must now splice the local BARE. Set here, not at the two returns below, so
        // both the sink and the LAMBDA_CHANNEL paths get it from ONE line and the
        // ruleThenBodyRootSlot early return cannot reach it un-set (that slot is
        // mapperFormSlot-implied and `single` is null there by the CFH-2 gate).
        if (single != null) {
            single.fired = true;
        }
        if (lambdaNaming != null) {
            // facet inLambdaArgSeatIteHoist (PR #355): the outer sentinel registers
            // AFTER the arms compiled (an arm-interior conditional registered its
            // own slot DURING the arm compile — the #327 inner-before-outer
            // consumption order, here falling out of the call order on the
            // per-lambda sub-group); the block re-anchors at its drain site
            // (relative-indent law), and the raw-token statement renders no
            // GeneratedIdentifier (the #346 scope-close law).
            String sentinel = lambdaNaming.registerLambdaScoped(
                    ifThenElseResultBaseName(expr, gm), lambdaKey);
            lambdaKey.registerPendingLambdaHoist(new JavaRawStatement(
                    block.replace(placeholder, sentinel), refs, wildcards));
            return JavaExpression.selfUnwrapping(
                    JavaExpression.from(sentinel, null, refs, wildcards));
        }
        String sentinel = sink.statementHoistSession()
                .register(ifThenElseResultBaseName(expr, gm));
        sink.registerStatementHoist(block.replace(placeholder, sentinel));
        if (ruleThenBodyRootSlot) {
            // facet condMapperFormSeats (seat 7, Rung M): the rule then-body-root
            // sentinel is a LIVE MapperS<X> local — returned TYPED and
            // non-self-unwrapping so unwrapForAssignment's Mapper fall-through
            // appends the `.get()` (the 755-golden consumer form). Every other
            // slot keeps the self-unwrapping bare splice byte-identically.
            return JavaExpression.from(sentinel,
                    typeUtil.wrap(typeUtil.MAPPER_S, declClazz), refs, wildcards);
        }
        // facet iteArmMetaCollapseDerefSinkChannel (v3.1 flip seat 33, law B.24 / B.4, the
        // consumer-type rung, measured at QuantityUnitOfMeasure rung 6): the sink-channel
        // ite's consumer reference carried NO type, so a downstream `default` join recovered
        // the RIGHT's wrapper from the AST (the arms' meta navigations) and hoisted
        // `x = ifThenElseResult.get()` on a POJO local whose arms had ALREADY deref'd
        // (B24-green3: `.get()` on a bare NonNegativeQuantitySchedule). The reference is
        // typed with the declaration's own class - render truth, the `<declClazz> local =
        // null;` line this block just emitted - so a bare decl reads bare and golden's
        // `MapperS.of(thenArg7.getOrDefault(ifThenElseResult))` follows from the plain path.
        return JavaExpression.selfUnwrapping(
                JavaExpression.from(sentinel, declClazz, refs, wildcards));
    }

    /**
     * facet condListCoerce (PR #327, arm A1) — the LIST-typed per-branch coercion hoist
     * for a no-real-else SINGLE-cardinality conditional consumed at a MULTI seat. Fires
     * ONLY on the {@link CondListCoerce} handshake a consumer pushed for EXACTLY this
     * node ({@link JavaStatementScope#findCondListCoerce()}, identity-compared) — the
     * consumer knows the seat's List element/local types and meta law
     * ({@code ConstructionHandler.hoistSingleValueIntoMultiOrNull}'s split); this arm
     * owns the emission:
     *
     * <pre>
     * final List&lt;Elem&gt; ifThenElseResultN;
     * if (&lt;cond&gt;.getOrDefault(false)) {
     *     &lt;arm-interior hoists&gt;                       // nested conditionals relocate here
     *     final &lt;Local&gt; &lt;name&gt;N = &lt;then value, ITEM form&gt;;
     *     ifThenElseResultN = &lt;name&gt;N == null ? Collections.&lt;Elem&gt;emptyList()
     *             : Collections.singletonList(&lt;name&gt;N | Elem.builder().setValue(&lt;name&gt;N).build());
     * } else {
     *     ifThenElseResultN = Collections.&lt;Elem&gt;emptyList();
     * }
     * </pre>
     *
     * <p>Upstream compiles the conditional at the consumer's List expected type, so each
     * arm coerces item→list per {@code TypeCoercionService.convertNullSafe} (the value
     * consumed twice — null check + singletonList — hoists to a type-named local INSIDE
     * the branch) and the absent else synthesizes {@code empty(List)} = emptyList. The
     * outer sentinel registers AFTER the then-arm compiles — a nested arm-VALUE
     * conditional's own hoist therefore numbers FIRST, matching upstream's
     * consumption-order {@code declareAsVariable} (the fca
     * {@code Create_TradeCounterpartyReport20__1} inner-8/outer-9 order). Arm-interior
     * hoists drain into the branch via the sink's mark/drain window.
     *
     * <p>Declines (null — the item arm / inline ternary keeps today's bytes), ALL
     * AST-level so the decline is side-effect-free: no handshake / wrong node / already
     * fired; no reachable sink (rule/alias/POJO paths, lambda interiors); a MULTI
     * conditional; a nested-in-THEN conditional; a REAL else value (only absent /
     * {@code empty} / the empty list literal coerce to emptyList); a bare-function-call
     * condition (the #179 boolHoist form is corpus-unverified at this seat).
     */
    private JavaStatementBuilder hoistAsListLocalOrNull(RConditionalExpr expr,
            ExpressionContext ctx, ExpressionCompiler compiler) {
        if (ctx.scope() == null) {
            return null;
        }
        CondListCoerce coerce = ctx.scope().findCondListCoerce();
        if (coerce == null || coerce.node != expr || coerce.fired) {
            return null;
        }
        JavaStatementScope sink = ctx.scope().findStatementHoistSink();
        if (sink == null) {
            return null;
        }
        GeneratorModel gm = compiler.getGeneratorModel();
        if (gm == null || gm.workspace() == null
                || gm.workspace().getCardinality(expr) == ExpressionCardinality.MULTI) {
            return null;
        }
        // facet singletonListCoerce (PR #361): the else-if LADDER admit — an
        // intermediate RConditionalExpr else chains a `} else if (` level (each
        // level's then is a leaf arm; the TERMINAL else must still be absent /
        // `empty` / the empty list literal, which coerces to the emptyList arm).
        // The walk pre-validates the WHOLE ladder AST-level so every decline stays
        // side-effect-free (the #327 law): a conditional THEN, a bare-fn-call
        // condition, or a real terminal else anywhere declines the whole node.
        // A single level walks to exactly the pre-#361 checks byte-identically.
        // Golden cdm5/cdm6 Create_Reset (`add reset -> resetHistory: if … then
        // ResolvePerformanceReset(…) else if … then ResolveInterestRateReset(…)`):
        // `final List<Reset> ifThenElseResult; if (…) { final Reset reset0 = …;
        // ifThenElseResult = reset0 == null ? Collections.<Reset>emptyList() :
        // Collections.singletonList(reset0); } else if (…) { …reset1… } else {
        // ifThenElseResult = Collections.<Reset>emptyList(); }` consumed by
        // `.addResetHistory(ifThenElseResult)`.
        List<RConditionalExpr> levels = new ArrayList<>();
        RConditionalExpr walk = expr;
        while (true) {
            if (walk.thenBranch() instanceof RConditionalExpr) {
                return null;
            }
            if (HandlerHelper.isBareFunctionCallCondition(walk.condition())) {
                return null;
            }
            levels.add(walk);
            RExpression walkEls = walk.elseBranch().orElse(null);
            if (walkEls instanceof RConditionalExpr nextLevel) {
                walk = nextLevel;
                continue;
            }
            if (walkEls != null && !(walkEls instanceof REmptyLiteral)
                    && !isEmptyListLiteral(walkEls)) {
                return null;
            }
            break;
        }
        String localBase = JavaNamingUtil.toFirstLower(coerce.localType.getSimpleName());
        if (!javax.lang.model.SourceVersion.isIdentifier(localBase)) {
            return null;
        }

        Set<JavaClass<?>> refs = new HashSet<>();
        Set<JavaClass<?>> wildcards = new HashSet<>();
        // Per-level compiles in SOURCE order (cond1, then1, cond2, then2, …): each
        // condition first — its hoists (none in the corpus carriers) register on
        // the sink and stack BEFORE this block, the order a pre-if evaluation
        // needs — then its arm, whose interior hoists drain INTO that branch
        // (nested arm-value conditionals number ahead of the outer sentinel,
        // registered below only after every arm compiled — the #327
        // inner-before-outer consumption order).
        List<JavaStatementBuilder> levelConds = new ArrayList<>();
        List<List<String>> levelArmHoists = new ArrayList<>();
        List<JavaStatementBuilder> levelArms = new ArrayList<>();
        List<Boolean> levelBigLitArm = new ArrayList<>();
        // Per-level then-arm — interior hoists drain INTO the branch (nested
        // conditionals number ahead of the outer sentinel, registered below only
        // after this compile).
        // facet condListBigDecimalIntLiteralArm (PR #357): an INT-LITERAL arm at a
        // BigDecimal-elem list seat singletonLists the seat-coerced literal DIRECTLY —
        // `ifThenElseResultN = Collections.singletonList(BigDecimal.valueOf(0));` (a
        // literal is never null, so upstream's convertNullSafe collapses to the plain
        // wrap; the pre-#357 `final BigDecimal bigDecimal = 0;` local never compiled).
        // The arm compiles AT the BigDecimal expected type (the #355 mechanism-4
        // conditionalArmExpectedType law), so LiteralHandler's renderIntValueForBigDecimal
        // owns every band — `BigDecimal.valueOf(0)` in int range, the lowercase-l
        // `BigDecimal.valueOf(…l)` long band, and the compiling
        // `new BigDecimal(new BigInteger("…"))` beyond long (the Copilot R1 catch: the
        // null-expected render's beyond-long `new BigInteger("…")` has no
        // BigDecimal.valueOf overload). Carriers: cftc/csa
        // Create_SubmissionHarmonizedRepeatableData. Evidence-scoped: RIntLiteral +
        // non-meta BigDecimal elem only.
        JavaTypeUtil condListTypeUtil = compiler.getTypeUtil();
        for (RConditionalExpr level : levels) {
            JavaStatementBuilder levelCond =
                    compiler.compile(level.condition(), null, ctx.scope());
            refs.addAll(levelCond.getRefs());
            wildcards.addAll(levelCond.getStaticWildcardImports());
            levelConds.add(levelCond);
            RExpression levelThen = level.thenBranch();
            boolean bigDecimalIntLiteralArm = levelThen instanceof RIntLiteral
                    && !coerce.wrapValueInMeta
                    && "BigDecimal".equals(coerce.listElemType.getSimpleName())
                    && condListTypeUtil != null;
            int armMark = sink.statementHoistMark();
            JavaStatementBuilder armBuilder = compiler.compile(levelThen,
                    bigDecimalIntLiteralArm ? condListTypeUtil.BIG_DECIMAL : null, ctx.scope());
            // facet condListMetaElemDeref (PR #371, F-E3): a MAPPER-CHAIN arm whose compiled
            // ELEMENT is a META wrapper of the list element derefs ELEMENT-WISE before the
            // `.getMulti()` list coercion — golden Create_ValuationDetailsFromReportableEvent
            // `…mapC("getProductIdentifier", …).<ProductIdentifier>map("Type coercion",
            // referenceWithMetaProductIdentifier -> referenceWithMetaProductIdentifier
            // .getValue()).getMulti()`. Value-type-equality-gated (the standard wrapper-vs-
            // bare gate); the bare `List<ProductIdentifier> = MapperC<ReferenceWithMeta…>
            // .getMulti()` form never compiled, so green files cannot carry it.
            if (!coerce.wrapValueInMeta && condListTypeUtil != null
                    && compiler.getCoercionService() != null
                    && armBuilder.getExpressionType() != null
                    && condListTypeUtil.getItemType(armBuilder.getExpressionType())
                            instanceof RJavaWithMetaValue armMetaElem
                    && armMetaElem.getValueType().equals(coerce.listElemType)) {
                JavaStatementBuilder derefArm =
                        compiler.coerceNavigationReceiver(armBuilder, ctx.scope());
                if (derefArm != null) {
                    armBuilder = derefArm;
                }
            }
            levelArmHoists.add(sink.drainStatementHoistsSince(armMark));
            levelArms.add(armBuilder);
            levelBigLitArm.add(bigDecimalIntLiteralArm);
        }
        StatementHoistSession session = sink.statementHoistSession();
        String sentinel = session.register(StatementHoistSession.IF_THEN_ELSE_RESULT);
        // The fqnWitness law (#227), exactly as hoistSingleValueIntoMultiOrNull: a meta
        // wrapper never collides with an fpml witness (plain simple name); a non-meta
        // type routes through the first-claim-wins resolver.
        String elemText = coerce.listElemType instanceof RJavaWithMetaValue
                ? coerce.listElemType.getSimpleName()
                : ImportCollisionResolver.typeRef(coerce.listElemType.getCanonicalName().withDots());

        StringBuilder sb = new StringBuilder();
        sb.append("final List<").append(elemText).append("> ").append(sentinel).append(";\n");
        for (int i = 0; i < levels.size(); i++) {
            sb.append(i == 0 ? "if (" : "} else if (")
              .append(HandlerHelper.render(levelConds.get(i))).append(".getOrDefault(false)) {\n");
            for (String hoist : levelArmHoists.get(i)) {
                sb.append('\t').append(hoist.replace("\n", "\n\t")).append('\n');
            }
            // ITEM-form value ladder — byte-identical to hoistAsItemLocalOrNull's arms
            // renderer (structural wrap strips to the bare inner; enum constants (by the
            // producer's witness) and the null literal pass bare; a Mapper chain appends .get()).
            // facet condListChainArmGetMulti (PR #357): a MAPPER-CHAIN arm (the else-branch
            // fall-through — no wrap-factory contract, not an enum constant / null literal)
            // coerces Mapper→List DIRECTLY (upstream TypeCoercionService: `<chain>.getMulti()`
            // assigned to the sentinel, NO value local and NO null-guard — an empty Mapper's
            // getMulti IS the empty list; golden Create_SubmissionHarmonizedRepeatableData_
            // Part45's five `ifThenElseResultN = MapperS.of(drrReport).<String>map(…).getMulti();`
            // arms). The ITEM-form arms (wrap-factory ctors/invocations — the green fca
            // Create_TradeCounterpartyReport20__1 class) keep the local + convertNullSafe
            // singletonList emission byte-frozen; a meta-wrapped elem keeps the local form too
            // (the builder consumes the value local — no chain carrier exists).
            JavaStatementBuilder armBuilder = levelArms.get(i);
            String itemValue;
            boolean chainArm = false;
            if (armBuilder instanceof JavaExpression je && je.unwrapToBuilder().isPresent()) {
                JavaStatementBuilder inner = je.unwrapToBuilder().get();
                refs.addAll(inner.getRefs());
                wildcards.addAll(inner.getStaticWildcardImports());
                itemValue = HandlerHelper.render(inner);
            } else {
                refs.addAll(armBuilder.getRefs());
                wildcards.addAll(armBuilder.getStaticWildcardImports());
                String src = HandlerHelper.render(armBuilder);
                if (HandlerHelper.isBareEnumConstant(armBuilder) || "null".equals(src)) {
                    itemValue = src;
                } else if (!coerce.wrapValueInMeta) {
                    chainArm = true;
                    itemValue = src + ".getMulti()";
                } else {
                    itemValue = src + ".get()";
                }
            }
            // ORDERING assumption (the #357 Seat-1 OBS-3): literals compile wrap-factory
            // (unwrap-present — the BigDecimal-expected compile above returns
            // wrappedInMapperSOf), so they take the ITEM branch of the value ladder above
            // and chainArm stays false — the render's `if (chainArm) else if
            // (bigDecimalIntLiteralArm)` order is safe; a future non-wrap-factory literal
            // compile would need the flag checked FIRST.
            if (chainArm) {
                sb.append('\t').append(sentinel).append(" = ")
                  .append(reanchorContinuation(itemValue, 1)).append(";\n");
            } else if (levelBigLitArm.get(i)) {
                // The seat-coerced itemValue already carries the full BigDecimal form
                // (renderIntValueForBigDecimal via the expected-type compile above).
                sb.append('\t').append(sentinel).append(" = Collections.singletonList(")
                  .append(itemValue).append(");\n");
            } else {
                String valueLocal = session.register(localBase);
                String localText = coerce.localType instanceof RJavaWithMetaValue
                        ? coerce.localType.getSimpleName()
                        : ImportCollisionResolver.typeRef(coerce.localType.getCanonicalName().withDots());
                String singletonValue = coerce.wrapValueInMeta
                        ? elemText + ".builder().setValue(" + valueLocal + ").build()"
                        : valueLocal;
                sb.append("\tfinal ").append(localText).append(' ').append(valueLocal).append(" = ")
                  .append(reanchorContinuation(itemValue, 1)).append(";\n");
                sb.append('\t').append(sentinel).append(" = ").append(valueLocal)
                  .append(" == null ? Collections.<").append(elemText).append(">emptyList()")
                  .append(" : Collections.singletonList(").append(singletonValue).append(");\n");
                refs.add(coerce.localType);
            }
        }
        sb.append("} else {\n");
        sb.append('\t').append(sentinel).append(" = Collections.<").append(elemText)
          .append(">emptyList();\n");
        sb.append('}');

        refs.add(HandlerHelper.LIST);
        refs.add(HandlerHelper.COLLECTIONS);
        refs.add(coerce.listElemType);
        sink.registerStatementHoist(sb.toString());
        coerce.fired = true;
        return JavaExpression.selfUnwrapping(
                JavaExpression.from(sentinel, null, refs, wildcards));
    }

    /**
     * facet ctorCondSingleCoerce (seat 28, law A) - <b>THE DIFFERS-GATE</b>. The whole
     * fire-vs-decline verdict of law A lives in this ONE method; every other edit the law makes
     * is unconditional plumbing that a {@code null} here renders byte-inert.
     *
     * <p>Fires only when ALL of:
     * <ol>
     *   <li>the seat is not a Mapper-form slot (the #357/#361/#383/#432/seat-7 family declares a
     *       {@code MapperS<X>}, never an item - a different declaration law);</li>
     *   <li>a handshake exists for EXACTLY this node and has not fired (node-identity keying, so
     *       a stale or nested read is a no-op - the {@link CondListCoerce} law);</li>
     *   <li><b>the walk DIFFERS from the seat</b> - {@code declClazz} is not the attribute's Java
     *       type. Equality is the dominant corpus case and MUST stay byte-frozen: golden drr
     *       5.61.0 {@code Create_FloatingRate} types {@code dcml}'s local {@code BigDecimal} (walk
     *       == attribute) and {@code sgn}'s {@code Boolean}, and cdm 6.20.6
     *       {@code MapUnitTypeWithScheme} types all three of its enum locals at the walk;</li>
     *   <li>the pair is a WITNESSED numeric conversion - {@link #condSingleCoercible}, the SAME
     *       predicate {@code ConstructionHandler.tryCtorNumericNarrow} and
     *       {@code hoistNumericCoerceCtorChainOrNull} apply at the two NON-conditional twins of
     *       this seat (LAW 69: three seats, one conversion class, no possible disagreement);</li>
     *   <li>the coercion service produces a REAL (non-identity) conversion - probed with an
     *       UNREGISTERED name so a service that does not implement the pair leaks no session
     *       registration (the #170 discarded-registration lesson, exactly as
     *       {@code hoistNumericCoerceCtorChainOrNull} probes);</li>
     *   <li><b>no arm of the ladder is a scalar literal this seat cannot assign BARE at the
     *       target type</b> - {@link #condSingleLadderHasUnconvertibleLiteralArm}, whose per-arm
     *       unit {@link #condSingleBareLiteralArm} is the SAME test {@link #condSingleArmValue}
     *       reads (LAW 69; v3.1 flip seat 32, law B.2, facet {@code ctorCondSingleCoerceLiteralArm}).
     *       A literal is never null, so upstream's convertNullSafe collapses to a plain
     *       seat-typed render and golden writes the bare literal ({@code ifThenElseResult1 = 1;},
     *       drr 7.x {@code AdjustFrequencyPeriod}); an int literal whose magnitude fits the
     *       Integer/Long slot is assigned bare and the else arm converts through the slot type.
     *       The residual decline is the literal class with NO bare form at an Integer/Long slot -
     *       {@code RBooleanLiteral} (13 band / 745 GREEN carriers), decimal and string literals -
     *       and a beyond-range int literal.</li>
     * </ol>
     *
     * <p><b>The isolation point.</b> Condition (4) is the seat's one open question: if the
     * mega-probe shows a GREEN pair whose walk differs from its attribute by a NON-numeric
     * conversion, this predicate is already as narrow as the evidence; if it shows the class is
     * exclusively numeric, (4) can be widened to "any real coercion" by deleting the one call.
     * Nothing else in the law changes either way.
     */
    private static CondSingleCoerce condSingleCoerceOrNull(RConditionalExpr expr,
            JavaClass<?> declClazz, boolean mapperFormSlot, ExpressionContext ctx,
            ExpressionCompiler compiler, JavaTypeUtil typeUtil) {
        if (mapperFormSlot || ctx == null || ctx.scope() == null || typeUtil == null
                || compiler == null || compiler.getCoercionService() == null) {
            return null;
        }
        CondSingleCoerce single = ctx.scope().findCondSingleCoerce();
        if (single == null || single.node != expr || single.fired) {
            return null;
        }
        if (declClazz == null || declClazz.equals(single.targetType)) {
            return null;
        }
        if (!condSingleCoercible(declClazz, single.targetType, typeUtil)) {
            return null;
        }
        if (condSingleLadderHasUnconvertibleLiteralArm(expr, single.targetType, typeUtil)) {
            return null;
        }
        String probeName = "condSingleCoerceProbe";
        JavaExpression probe = JavaExpression.from(probeName, declClazz, Set.of(), Set.of());
        if (compiler.getCoercionService()
                .coerceExpression(probe, declClazz, single.targetType, true, ctx.scope())
                .renderToString().equals(probeName)) {
            return null;
        }
        return single;
    }

    /**
     * facet ctorCondSingleCoerce (seat 28, law A): the witnessed conversion class - a copy of
     * the {@code coercible} test in {@code ConstructionHandler.hoistNumericCoerceCtorChainOrNull}
     * (whose own narrowing half is {@code tryCtorNumericNarrow}'s). Both directions are
     * golden-witnessed at the CTOR-SETTER seat: narrowing {@code BigDecimal/BigInteger/Long ->
     * Integer} and {@code BigDecimal/BigInteger -> Long} (the {@code intValueExact()} /
     * {@code longValueExact()} guards), and widening {@code Integer -> BigDecimal}
     * ({@code BigDecimal.valueOf(...)}). LAW 69: the conditional seat and the two non-conditional
     * seats must classify the same pair the same way, or the same file would narrow at the arm
     * in one pair and at the setter in the next.
     */
    private static boolean condSingleCoercible(JavaClass<?> from, JavaClass<?> to,
            JavaTypeUtil typeUtil) {
        return (typeUtil.isInteger(to) && (typeUtil.isBigDecimal(from)
                        || typeUtil.isBigInteger(from) || typeUtil.isLong(from)))
                || (typeUtil.isLong(to) && (typeUtil.isBigDecimal(from)
                        || typeUtil.isBigInteger(from)))
                || (typeUtil.isBigDecimal(to) && typeUtil.isInteger(from));
    }

    /**
     * facet ctorCondSingleCoerceLiteralArm (v3.1 flip seat 32, law B2): the seat-28 law-A
     * literal-arm decline, NARROWED from "any scalar literal arm" to "a scalar literal arm this
     * seat cannot assign BARE at the target type" - golden {@code final Integer
     * ifThenElseResult1; ifThenElseResult1 = 1;} (drr 7.0-7.3 {@code AdjustFrequencyPeriod})
     * where the fork writes {@code final BigDecimal ifThenElseResult1; ifThenElseResult1 =
     * BigDecimal.valueOf(1);}.
     *
     * <p>Seat 28 declined the WHOLE ladder because reaching golden needed the LITERAL to compile
     * at the seat type, which the fork's literal seat could not then do: it reads the ladder
     * ROOT's workspace inference ({@code HandlerHelper.intLiteralConditionalArmExpectedType},
     * consulted by BOTH routes). Rung (b) of this law makes that seat prefer the CTOR ATTRIBUTE
     * over the root when the root IS a key-value pair's value, so an int literal at an
     * {@code int} attribute now renders the bare Java token and this ladder can be admitted.
     *
     * <p><b>Only that one class is admitted.</b> {@link #condSingleBareLiteralArm} is the ONE
     * test this gate and {@link #condSingleArmValue} both read (LAW 69 - the two halves cannot
     * disagree about which arm skips the null guard). Every other scalar literal still declines
     * the WHOLE ladder: {@code RBooleanLiteral} is 13 band but <b>745 GREEN</b> (the seat-28
     * {@code probe28-verdicts.md:387} DO-NOT-WIDEN), and a decimal or string literal has no bare
     * form at an {@code Integer}/{@code Long} slot. AST-level, so the decline stays
     * side-effect-free (the #327 law).
     *
     * <p><b>MEASURED reach</b> (LAW-75 probe {@code [P32-B2]}, whole matrix, both routes,
     * route-identical): the 9,861-row gate space is four clean buckets - 8,876 with no differing
     * walk, 971 held out by {@code differs}/{@code coercible} DESPITE carrying a literal arm, 10
     * already firing, and <b>4</b> reading {@code differs=true coercible=true hasLiteralArm=true
     * fired=false}, all four {@code fn:AdjustFrequencyPeriod}. So narrowing this gate moves
     * exactly those 4 rows and nothing else; the 971 are held out by conjuncts this law does not
     * touch. This predicate has ONE call site, so that measurement is complete.
     */
    private static boolean condSingleLadderHasUnconvertibleLiteralArm(RConditionalExpr expr,
            JavaClass<?> target, JavaTypeUtil typeUtil) {
        RConditionalExpr cur = expr;
        int depth = 0;
        while (cur != null && depth++ < 32) {
            if (unconvertibleLiteralArm(cur.thenBranch(), target, typeUtil)) {
                return true;
            }
            RExpression els = cur.elseBranch().orElse(null);
            if (els instanceof RConditionalExpr next) {
                cur = next;
                continue;
            }
            return els != null && unconvertibleLiteralArm(els, target, typeUtil);
        }
        return false;
    }

    /**
     * facet ctorCondSingleCoerceLiteralArm (v3.1 flip seat 32, law B2): the ladder-level
     * decline's per-arm unit - a scalar literal arm this seat can NOT assign bare at
     * {@code target}. A NON-literal arm is not this gate's business at all: it converts through
     * {@link #condSingleArmValue}'s null-safe guard, which is the seat-28 law that already
     * works.
     */
    private static boolean unconvertibleLiteralArm(RExpression arm, JavaClass<?> target,
            JavaTypeUtil typeUtil) {
        return HandlerHelper.isScalarLiteral(arm)
                && !condSingleBareLiteralArm(arm, target, typeUtil);
    }

    /**
     * facet ctorCondSingleCoerceLiteralArm (v3.1 flip seat 32, law B2): the ONE test the ladder
     * gate and the arm renderer both read (LAW 69) - an arm that assigns BARE at the seat's
     * target type, with no null guard and no in-arm value local, exactly as golden writes it
     * ({@code ifThenElseResult1 = 1;}).
     *
     * <p>An int literal is never null, so the guard {@link #condSingleArmValue} normally emits
     * would render {@code 1 == null ? null : ...}; and {@code 1} is not a simple Java
     * identifier, so that method's twice-read-operand hoist would otherwise mint a value local
     * ({@code final BigDecimal bigDecimal = 1;}) no golden carries. Once rung (b) has stopped
     * the ladder root widening it to {@code BigDecimal}, the arm renders as the bare Java int
     * token, which is assignment-compatible with an {@code Integer} or {@code Long} slot EXACTLY
     * when its magnitude fits.
     *
     * <p><b>The range test is the whole of the law's safety here.</b> The grammar's
     * {@code INT_LITERAL} token has no upper bound - {@code RIntLiteral} stores a
     * {@code BigInteger}, and seat 24's law F5 lifted the literal seat's own within-long
     * exclusion - so a beyond-int literal at an {@code int} attribute must keep the ladder's
     * decline rather than emit an assignment that does not compile (LAW 74).
     *
     * <p>{@code false} for every other literal kind (the {@code RBooleanLiteral} 745-green
     * DO-NOT-WIDEN) and for a {@code BigDecimal} target, where the literal legitimately keeps
     * its {@code BigDecimal.valueOf(...)} seat - the #355 WIDENING direction, untouched.
     */
    private static boolean condSingleBareLiteralArm(RExpression arm, JavaClass<?> target,
            JavaTypeUtil typeUtil) {
        if (!(arm instanceof RIntLiteral intArm) || target == null || typeUtil == null
                || intArm.value() == null) {
            return false;
        }
        int bits = intArm.value().bitLength();
        return (typeUtil.isInteger(target) && bits < 32)
                || (typeUtil.isLong(target) && bits < 64);
    }

    /**
     * facet ctorCondSingleCoerce (seat 28, law A): convert ONE rendered arm value from the
     * walk type to the seat's expected type, null-safely, in the ASSIGNMENT context.
     *
     * <pre>
     * bare-identifier arm  ->  spreadBasis == null ? null : spreadBasis.intValueExact()
     * chain arm            ->  final BigDecimal bigDecimal = &lt;chain&gt;.get();      // registered here
     *                          bigDecimal == null ? null : bigDecimal.intValueExact()
     * </pre>
     *
     * <p><b>No outer parentheses</b> - the caller emits {@code ifThenElseResultN = &lt;this&gt;;},
     * where golden writes the bare ternary (drr 5.61.0 {@code Create_FloatingRate:93}). The
     * parenthesised sibling belongs to the SETTER-ARG context and is emitted by
     * {@code ConstructionHandler} for the non-conditional twin.
     *
     * <p><b>The value local.</b> The guard reads its operand TWICE, so anything that is not a
     * simple Java identifier hoists first - upstream's {@code convertNullSafe} law, and the same
     * decl {@code hoistNumericCoerceCtorChainOrNull} emits (same base name = the walk type's
     * lowercased simple name, same session group, so a singleton renders bare {@code bigDecimal}
     * and siblings renumber {@code bigDecimal0..n-1}). The decl registers DURING {@code renderArm},
     * so {@code appendConditionalChain}'s mark/drain window relocates it INSIDE the owning
     * branch (the #327/#355/#364 arm window at {@code ControlFlowHandler:3753-3799}) - exactly
     * golden drr 5.61.0 {@code GetPackg:110-112}.
     *
     * <p><b>Declines (returns the arm value UNCHANGED - today's bytes for that arm).</b> A
     * {@code null} literal arm (upstream's convertNullSafe of null IS null, and a guard on it
     * would render {@code null == null ? null : null.intValueExact()}); a walk type whose
     * lowercased simple name is not a Java identifier; no reachable registration channel.
     *
     * <p><b>Refs.</b> The base renderer has already unioned each arm's own refs into the shared
     * {@code refs} set by the time this runs, so the LAMBDA_CHANNEL raw statement is given that
     * union plus the walk class - a superset whose every member is genuinely referenced by this
     * same block, never an orphan import. The SINK path's {@code registerStatementHoist(String)}
     * takes text only and rides the shared set exactly like the #377 arm.
     */
    private static String condSingleArmValue(RExpression arm, String armValue,
            JavaClass<?> fromClass,
            CondSingleCoerce single, ExpressionContext ctx, ExpressionCompiler compiler,
            StatementHoistSession lambdaNaming, JavaStatementScope lambdaKey,
            JavaTypeUtil typeUtil,
            Set<JavaClass<?>> refs, Set<JavaClass<?>> wildcards) {
        if (armValue == null || "null".equals(armValue)) {
            return armValue;
        }
        // facet ctorCondSingleCoerceLiteralArm (v3.1 flip seat 32, law B2): a BARE-assignable
        // literal arm returns UNCHANGED - golden `ifThenElseResult1 = 1;`, not the null guard
        // this method exists to add. Both reasons are load-bearing: a literal is never null, so
        // the guard would render `1 == null ? null : ...`; and `1` is not a simple Java
        // identifier, so the hoist below would mint `final BigDecimal bigDecimal = 1;`. The
        // SAME condSingleBareLiteralArm the ladder gate admitted the arm by (LAW 69) - a
        // literal the gate DECLINED never reaches here, because the whole ladder declined.
        if (condSingleBareLiteralArm(arm, single.targetType, typeUtil)) {
            return armValue;
        }
        String raw = armValue;
        if (!isSimpleJavaIdentifier(raw)) {
            String base = JavaNamingUtil.toFirstLower(fromClass.getSimpleName());
            if (!javax.lang.model.SourceVersion.isIdentifier(base)) {
                return armValue;
            }
            JavaStatementScope armSink = ctx.scope().findStatementHoistSink();
            if (armSink == null && (lambdaNaming == null || lambdaKey == null)) {
                return armValue;
            }
            String name = armSink != null
                    ? armSink.statementHoistSession().register(base)
                    : lambdaNaming.registerLambdaScoped(base, lambdaKey);
            String decl = "final " + fromClass.getSimpleName() + " " + name + " = " + raw + ";";
            refs.add(fromClass);
            if (armSink != null) {
                armSink.registerStatementHoist(decl);
            } else {
                Set<JavaClass<?>> declRefs = new HashSet<>(refs);
                lambdaKey.registerPendingLambdaHoist(
                        new JavaRawStatement(decl, declRefs, new HashSet<>(wildcards)));
            }
            raw = name;
        }
        JavaExpression rawRef = JavaExpression.from(raw, fromClass, Set.of(), Set.of());
        JavaExpression conv = compiler.getCoercionService()
                .coerceExpression(rawRef, fromClass, single.targetType, true, ctx.scope());
        refs.addAll(conv.getRefs());
        wildcards.addAll(conv.getStaticWildcardImports());
        return raw + " == null ? null : " + conv.renderToString();
    }

    /**
     * An ASCII char-scan for a bare Java identifier, NOT a regex (engineering standards: no regex
     * on structured content - the argument is rendered Java source). Byte-identical to the gate
     * {@code ConstructionHandler.tryCtorNumericNarrow} applies for the same reason (the null
     * guard evaluates its operand twice, so only an identifier may stay inline).
     */
    private static boolean isSimpleJavaIdentifier(String raw) {
        return !raw.isEmpty() && Character.isJavaIdentifierStart(raw.charAt(0))
                && raw.chars().allMatch(Character::isJavaIdentifierPart);
    }

    /**
     * The hoisted local's declared type, from the THEN branch's ITEM type
     * (upstream: the branch compiles seat-coerced and the local types at the
     * branch join; the absent-else NULL_TYPE joins away). Ladder: fn-call
     * output attribute, ctor type call, navigation terminal attribute
     * (resolved or disguised 2-name chain), enum value, literals,
     * conversions; {@code null} (decline) for every other shape — under-firing
     * keeps the file waivered, the only regression-free direction.
     */
    /**
     * facet blankFinalCtorLadderEnumJoin (PR #382, E-i): the FIRST ladder arm (walking
     * the else-if chain) whose value is a parser-BOUND bare enum value — its parent
     * {@link com.regnosys.rosetta.ast.types.REnumeration} is the join evidence for a
     * #204-class mis-bound sibling arm. {@code null} when no arm carries a bound
     * enum value.
     */
    private static com.regnosys.rosetta.ast.types.REnumeration
            ladderSiblingBoundEnumeration(RConditionalExpr cond) {
        RConditionalExpr cur = cond;
        int depth = 0;
        while (cur != null && depth++ < 32) {
            if (cur.thenBranch() instanceof RSymbolReference sr
                    && sr.symbol().orElse(null)
                            instanceof com.regnosys.rosetta.ast.supporting.REnumValue ev
                    && ev.parent()
                            instanceof com.regnosys.rosetta.ast.types.REnumeration en) {
                return en;
            }
            RExpression els = cur.elseBranch().orElse(null);
            cur = els instanceof RConditionalExpr next ? next : null;
        }
        return null;
    }

    /**
     * facet blankFinalCtorLadderEnumJoin (PR #382, E-i): the enum value a #204-class
     * MIS-BOUND bare arm re-qualifies to — the arm is a no-args
     * {@link RSymbolReference} whose symbol is EMPTY or a non-value root element
     * (the SAME class list as {@code NavigationHandler.misBoundSiblingAttributeEnumeration}
     * — keep in lockstep) and whose NAME matches a value in {@code en}'s hierarchy
     * ({@link HandlerHelper#findEnumValueInHierarchy} — the #358 flatten law).
     * {@code null} declines: a properly-bound arm, an args-bearing call, or a
     * non-matching name.
     */
    private static com.regnosys.rosetta.ast.supporting.REnumValue misBoundLadderArmEnumValue(
            RExpression arm, com.regnosys.rosetta.ast.types.REnumeration en) {
        if (!(arm instanceof RSymbolReference sr) || !sr.args().isEmpty()) {
            return null;
        }
        var sym = sr.symbol().orElse(null);
        boolean nonValueMisBind = sym == null
                || sym instanceof com.regnosys.rosetta.ast.types.RDataType
                || sym instanceof com.regnosys.rosetta.ast.types.RChoice
                || sym instanceof com.regnosys.rosetta.ast.types.RRecordType
                || sym instanceof com.regnosys.rosetta.ast.types.REnumeration
                || sym instanceof com.regnosys.rosetta.ast.types.RBasicType
                || sym instanceof com.regnosys.rosetta.ast.regulatory.RSegmentDef
                || sym instanceof com.regnosys.rosetta.ast.annotations.RAnnotation;
        if (!nonValueMisBind) {
            return null;
        }
        return HandlerHelper.findEnumValueInHierarchy(en, sr.name());
    }

    private static JavaClass<?> thenItemJavaClass(RExpression then, GeneratorModel gm,
            com.regnosys.rosetta.generator.java.types.JavaTypeTranslator translator,
            JavaTypeUtil typeUtil, ExpressionCompiler compiler) {
        if (then instanceof RConstructorExpr ctor && ctor.typeCall() != null) {
            return toRefOrNull(translator, unalias(gm.resolveTypeCall(ctor.typeCall())));
        }
        if (then instanceof RSymbolReference sr) {
            var sym = sr.symbol().orElse(null);
            if (sym instanceof RFunction fn) {
                RAttribute out = fn.output().orElse(null);
                if (out == null) {
                    return null;
                }
                JavaClass<?> bare = toRefOrNull(translator, unalias(gm.getType(out)));
                // facet metaIteArmWrapperTyping (PR #328, F4): a META-output callee's
                // evaluate() returns the WRAPPER (the #186 metaWit law), so the hoisted
                // local types as FieldWithMetaX/ReferenceWithMetaX — upstream compiles
                // the conditional at the CONSUMER's expected type, so the lift fires
                // ONLY when the consuming ctor-pair attribute is itself META + single
                // (golden MapResolvablePriceQuantity `final ReferenceWithMetaNonNegative
                // QuantitySchedule ifThenElseResult0;` — the pre-fix BARE-typed local
                // never compiled against the wrapper-returning call, green-safe by
                // construction). A BARE consumer keeps the bare local byte-identically —
                // golden derefs the meta arm INSIDE the branch there (cdm6
                // MapUnitTypeWithScheme, the regscan-caught AWAY-mover of the
                // unconditional first cut; the in-branch deref form is a deferred
                // lead). The consuming setter keys PLAIN in lock-step via the
                // valueCarriesAttributeMeta RConditionalExpr arm (#327-B law).
                MetaFieldGenerator.MetaKind outKind = MetaFieldGenerator.detectMetaKind(out);
                if (bare != null && outKind != MetaFieldGenerator.MetaKind.NONE
                        && typeUtil != null
                        && conditionalCtorConsumerIsMeta(then, gm)) {
                    return RJavaWithMetaValue.create(
                            outKind == MetaFieldGenerator.MetaKind.REFERENCE_WITH_META,
                            bare, typeUtil);
                }
                return bare;
            }
            if (sym instanceof RAttribute attr) {
                return toRefOrNull(translator, unalias(gm.getType(attr)));
            }
            // facet ctorSetterEnum (PR #202): a bare enum value bound by the parser
            // (TypeInferenceEngine Cat 13d) is an RSymbolReference resolving to an
            // REnumValue — type the hoist local from the value's enumeration so the
            // no-else ctor-setter conditional (`if .. then Physical`) hoists
            // `<Enum> ifThenElseResult = null; …`. renderArm then compiles the bound
            // ref via ReferenceHandler's REnumValue branch → `Enum.VALUE`
            // (the producer's enum-constant witness → passes bare). The qualified REnumValueRef shape
            // is handled by the dedicated branch below.
            //
            // facet boundEnumInferredOwner (v3.1 flip seat 12, the indep review's
            // NIT-1 — LAW 69): the local's type CONSULTS the same owner computation
            // the constant renders with (HandlerHelper.boundEnumInferredOwner — the
            // node's INFERRED = expected enum under the #215 same-instance gate,
            // else the declaring enum). Typing the local from `ev.parent()` beside a
            // constant qualified by the expected CHILD enum would not compile
            // (`ProductId x = AssetId.ISIN`) — and the pre-seat-12 pair (`ProductId x
            // = ProductId.ISIN` handed to `setIdType(AssetId)`) did not compile
            // either: zero corpus carriers, byte-neutral corpus-wide (both rings
            // EXACT), pinned at unit grain by BoundEnumInferredOwnerSeatTest.a6.
            if (sym instanceof com.regnosys.rosetta.ast.supporting.REnumValue ev
                    && ev.parent() instanceof com.regnosys.rosetta.ast.types.REnumeration en) {
                return toRefOrNull(translator, new REnumTypeRef(
                        HandlerHelper.boundEnumInferredOwner(sr, ev, en, gm)));
            }
            // facet inLambdaArgSeatIteHoist (PR #355): an inner-RULE arm types from
            // the rule's INFERRED output (a rule's output is not stored on the node —
            // the NavigationHandler.resolveReceiverOutputType idiom): the asic/mas
            // PriceRule else arm `cde.price.PackageTransactionPriceNoFormat` types
            // BigDecimal, joining the Integer-literal then-arm up to golden's
            // `final BigDecimal ifThenElseResult`. Types BARE (a meta-output rule
            // arm keeps whatever byte-shape it had — every reachable seat renders
            // the zero-golden ternary today, so under- or mis-typing here stays
            // within waivered files).
            if (sym instanceof com.regnosys.rosetta.ast.functions.RRule rule
                    && gm.workspace() != null) {
                // seat 21: the ONE rule-output read (HandlerHelper.ruleInferredOutputRType);
                // this consumer keeps its own alias-strip + Java-ref translation.
                com.regnosys.rosetta.types.RType inferred =
                        HandlerHelper.ruleInferredOutputRType(rule, gm);
                if (inferred == null) {
                    return null;
                }
                return toRefOrNull(translator, unalias(inferred));
            }
            return null;
        }
        if (then instanceof RFeatureCall thenFc) {
            JavaClass<?> nav = toRefOrNull(translator,
                    unalias(NavigationHandler.resolveReceiverRType(then, gm)));
            if (nav != null) {
                return nav;
            }
            // facet ctorEvalArgRecordDateIteHoist (PR #389): a then-chain whose LEAF is
            // the `date` RECORD feature has no RAttribute (resolveReceiverRType returns
            // null — a record is not an RDataType), so type the local from the SAME
            // gate the record-nav render fires on (tryRecordFeatureNav's
            // isDateRecordFeature — the walk-render alignment law): the rendered arm IS
            // `.<Date>map("Date", zdt -> Date.of(zdt.toLocalDate())).get()`, so the
            // local declares com.rosetta.model.lib.records.Date (golden cdm6
            // MapFxFixingScheduleToObservationDates `final Date ifThenElseResult0;`
            // at the ctor-setter evaluate-arg seat). Green-safe by the #181
            // construction: a fired seat previously rendered the zero-golden ternary.
            if (typeUtil != null && compiler != null
                    && NavigationHandler.resolveReceiverRType(thenFc.receiver(), gm, compiler)
                            instanceof com.regnosys.rosetta.types.RRecordType thenRt
                    && NavigationHandler.isDateRecordFeature(thenFc.featureName(), thenRt)) {
                return typeUtil.DATE;
            }
            return null;
        }
        // facet fnIteHoistSeats (PR #361): a BINARY-ARITHMETIC then-arm types
        // from the SAME witness-selection ladder the render's MapperMaths
        // emission uses (ArithmeticHandler.binaryResultItemJavaClass — typed
        // operand-kind join → #334 string join → legacy heuristic, the #178
        // same-walk invariant kept in LOCKSTEP inside ArithmeticHandler; the
        // workspace inference is MISSING here for alias operands like
        // MapPrincipalPayment's `amount * -1`, whose unary-minus right operand
        // ALSO classifies UNKNOWN — the pair resolves on the legacy rung, the
        // same BigDecimal the render's witness carries). A unary-topped then
        // declines inside the helper (no carrier). Golden cdm6
        // MapPrincipalPayment `final BigDecimal ifThenElseResult;
        // if (lessThan(…)) { = MapperMaths.<BigDecimal, BigDecimal, BigDecimal>
        // multiply(…).get(); } else { = amount(…).get(); }` consumed by
        // `.setValue(ifThenElseResult)` — the arm value renders item-form via
        // the existing renderArm Mapper-chain `.get()` rung.
        if (then instanceof com.regnosys.rosetta.ast.expressions.binary.RArithmeticExpr arith
                && compiler != null && typeUtil != null
                && !isThenBaseConditionalBranch(then)) {
            return ArithmeticHandler.binaryResultItemJavaClass(arith, compiler);
        }
        // facet fnIteHoistSeats (PR #361): a COLLAPSING list-op then-arm (first /
        // last / only-element — the single-item collapses) types from its
        // ARGUMENT's item type recursively: the collapse preserves the element
        // type, so the hoisted local declares the argument chain's item (golden
        // esma/fca Create_ContractType14__1 `String ifThenElseResult2 = null;
        // if (areEqual(…)) { ifThenElseResult2 = MapperC.of(…).last().get(); }`
        // — the LAST argument is a disguised 2-name chain → the REnumValueRef
        // arm below → String). The stay-multi ops (flatten / reverse / distinct
        // / sum) keep the decline — their hoist local would need the LIST form
        // this item ladder does not model.
        if (then instanceof com.regnosys.rosetta.ast.expressions.unary.RListOpExpr lop
                && (lop.op() == com.regnosys.rosetta.ast.enums.ListOp.FIRST
                        || lop.op() == com.regnosys.rosetta.ast.enums.ListOp.LAST
                        || lop.op() == com.regnosys.rosetta.ast.enums.ListOp.ONLY_ELEMENT)
                && lop.argument() != null
                && !isThenBaseConditionalBranch(then)) {
            return thenItemJavaClass(lop.argument(), gm, translator, typeUtil, compiler);
        }
        if (then instanceof REnumValueRef evr) {
            if (evr.enumeration().isPresent()) {
                return toRefOrNull(translator, new REnumTypeRef(evr.enumeration().get()));
            }
            // DISGUISED 2-name chain `head -> feature` (the #170 shape): type from
            // the enclosing function's head attribute, then the feature's item type.
            return disguisedChainJavaClass(evr, gm, translator);
        }
        if (then instanceof RBooleanLiteral) {
            return typeUtil.BOOLEAN;
        }
        if (then instanceof RStringLiteral) {
            return typeUtil.STRING;
        }
        if (then instanceof RIntLiteral) {
            return typeUtil.INTEGER;
        }
        if (then instanceof RToStringExpr) {
            return typeUtil.STRING;
        }
        if (then instanceof RConversionExpr conv) {
            return switch (conv.kind()) {
                case ENUM -> conv.targetEnum()
                        .map(en -> toRefOrNull(translator, new REnumTypeRef(en)))
                        .orElse(null);
                case INT -> typeUtil.INTEGER;
                case NUMBER -> typeUtil.BIG_DECIMAL;
                case DATE -> typeUtil.DATE;
                default -> null;
            };
        }
        return null;
    }

    /**
     * Type a DISGUISED {@code head -> feature} 2-name chain (the grammar parses
     * it as {@link REnumValueRef}; no resolved enumeration): the head matches
     * an enclosing-function input/output attribute, the feature an attribute of
     * the head's data type — mirroring the rendering route
     * ({@code ReferenceHandler.handle(REnumValueRef)} fall-through →
     * {@code synthesizeFeatureCall} → navigation), so the declared type and the
     * rendered chain resolve from the same attributes. Prefers the parse-time
     * Cat-10 {@code resolvedAttributeChain} binding when present.
     */
    private static JavaClass<?> disguisedChainJavaClass(REnumValueRef evr, GeneratorModel gm,
            com.regnosys.rosetta.generator.java.types.JavaTypeTranslator translator) {
        var chain = evr.resolvedAttributeChain().orElse(null);
        if (chain != null && chain.feature() != null) {
            return toRefOrNull(translator, unalias(gm.getType(chain.feature())));
        }
        String head = evr.enumName();
        String feature = evr.valueName();
        if (head == null || feature == null) {
            return null;
        }
        // Share the renderer's depth-bounded SOT walk (the same
        // HandlerHelper.findEnclosingFunction synthesizeFeatureCall resolves
        // against) so the declared type and the rendered chain agree on the
        // enclosing function.
        RFunction fn = HandlerHelper.findEnclosingFunction(evr);
        if (fn == null) {
            return null;
        }
        RAttribute headAttr = null;
        for (RAttribute in : fn.inputs()) {
            if (head.equals(in.name())) {
                headAttr = in;
            }
        }
        if (headAttr == null && fn.output().filter(o -> head.equals(o.name())).isPresent()) {
            headAttr = fn.output().get();
        }
        if (headAttr == null) {
            return null;
        }
        RType headType = unalias(gm.getType(headAttr));
        com.regnosys.rosetta.ast.types.RDataType dt = null;
        if (headType instanceof com.regnosys.rosetta.types.RDataTypeRef dtr) {
            dt = dtr.astNode();
        } else if (headType instanceof com.regnosys.rosetta.types.RChoiceTypeRef ctr) {
            dt = ctr.asRDataType();
        }
        if (dt == null) {
            return null;
        }
        for (RAttribute attr : HandlerHelper.allAttributesInUpstreamOrder(dt)) {
            if (feature.equals(attr.name())) {
                return toRefOrNull(translator, unalias(gm.getType(attr)));
            }
        }
        return null;
    }

    private static JavaClass<?> toRefOrNull(
            com.regnosys.rosetta.generator.java.types.JavaTypeTranslator translator, RType t) {
        if (t == null || t instanceof com.regnosys.rosetta.types.RMissingType) {
            return null;
        }
        try {
            return translator.toJavaReferenceType(t);
        } catch (UnsupportedOperationException e) {
            return null;
        }
    }

    /**
     * facet metaIteArmWrapperTyping (PR #328, F4): {@code true} iff the arm's
     * owning conditional (walking up through an else-if chain of
     * {@link RConditionalExpr} parents) is consumed by a ctor pair
     * ({@link RKeyValuePair}) whose attribute is value-level META + SINGLE —
     * the consumer expectation the wrapper lift keys on (upstream compiles the
     * conditional at the consumer's expected type). Resolves the pair's
     * attribute from the enclosing {@link RConstructorExpr}'s type exactly as
     * {@code ConstructionHandler.condListPreAttrs}. Every other consumer
     * (a bare/non-meta pair, a MULTI pair — the CondListCoerce seat — or a
     * non-ctor seat) returns {@code false}: the bare local keeps today's bytes.
     */
    /** Bound on the else-if-chain parent walk in {@link #conditionalCtorConsumerIsMeta}:
     *  consecutive {@link RConditionalExpr} parents between the arm and its consumer.
     *  Real ladders are a handful of rungs; a deeper chain stops the walk and the
     *  {@code RKeyValuePair} check below then declines (the bare-local polarity). */
    private static final int CONSUMER_ELSE_IF_WALK_LIMIT = 16;

    private static boolean conditionalCtorConsumerIsMeta(RExpression arm, GeneratorModel gm) {
        if (arm == null || gm == null) {
            return false;
        }
        com.regnosys.rosetta.ast.RNode cur = arm.parent();
        int depth = 0;
        while (cur instanceof RConditionalExpr && depth++ < CONSUMER_ELSE_IF_WALK_LIMIT) {
            cur = cur.parent();
        }
        if (!(cur instanceof com.regnosys.rosetta.ast.expressions.supporting.RKeyValuePair kvp)
                || kvp.key() == null
                || !(kvp.parent() instanceof RConstructorExpr ctor)
                || ctor.typeCall() == null
                || !(kvp.value() instanceof RConditionalExpr wholeConditional)) {
            return false;
        }
        // Seat-1 #328 lock-step hardening: the WHOLE conditional (the pair's value)
        // must prove the SAME meta kind the arm's callee output carries — a
        // hypothetical MIXED conditional (a META then arm + a BARE real-else arm)
        // joins to NONE at the setter proof (valueProvenMetaKind), so lifting its
        // local to the wrapper would let the local and the setter disagree. No
        // corpus carrier (empirically bounded by the #328 set-diff); the shipped
        // carriers' conditionals all prove their arm's kind, byte-neutral.
        MetaFieldGenerator.MetaKind wholeKind =
                ConstructionHandler.valueProvenMetaKind(wholeConditional);
        if (wholeKind == MetaFieldGenerator.MetaKind.NONE) {
            return false;
        }
        RType resolved = gm.resolveTypeCall(ctor.typeCall());
        com.regnosys.rosetta.ast.types.RDataType dt = null;
        if (resolved instanceof com.regnosys.rosetta.types.RDataTypeRef dtr) {
            dt = dtr.astNode();
        } else if (resolved instanceof com.regnosys.rosetta.types.RChoiceTypeRef ctr) {
            dt = ctr.asRDataType();
        }
        if (dt == null) {
            return false;
        }
        for (RAttribute attr : HandlerHelper.allAttributesInUpstreamOrder(dt)) {
            if (kvp.key().equals(attr.name())) {
                return MetaFieldGenerator.detectMetaKind(attr) != MetaFieldGenerator.MetaKind.NONE
                        && !gm.isMulti(attr);
            }
        }
        return false;
    }

    private static RType unalias(RType t) {
        t = HandlerHelper.stripAliases(t);
        return t;
    }

    /**
     * Build a hoisted if-then-else block at RELATIVE indent (interior lines
     * carry only their depth-tabs; the draining renderer re-anchors every line
     * to the statement indent). Two declaration forms, selected by the else
     * VALUE — the null-literal SUBSET of upstream
     * {@code JavaIfThenElseBuilder.declareAsVariable}'s JavaLiteral-else rule
     * (an EXPLICIT literal else — {@code else 0} — would take upstream's
     * initializer form but keeps the general form here; zero corpus goldens
     * carry a non-null {@code ifThenElseResult} declaration initializer, so
     * the subset is carrier-exact):
     *
     * <pre>
     * // initializer form — absent else synthesizing the null literal:
     * &lt;Type&gt; &lt;local&gt; = null;
     * if (&lt;cond&gt;.getOrDefault(false)) {
     *     &lt;local&gt; = &lt;then&gt;;
     * }
     * // general form — explicit else or a non-literal synthesis:
     * final &lt;Type&gt; &lt;local&gt;;
     * if (&lt;cond&gt;.getOrDefault(false)) {
     *     &lt;local&gt; = &lt;then&gt;;
     * } else {
     *     &lt;local&gt; = &lt;else&gt;;
     * }
     * </pre>
     *
     * Nested Rosetta conditionals FOLD into the same local — a nested
     * then-branch renders as an inner if/else block one level deeper, a nested
     * else-branch chains {@code } else if (} at the same level (upstream
     * {@code completeAsAssignment} distribution + the unbraced else-if
     * statement form); the terminal absent else inside a ladder always emits
     * the synthesized else block (only the TOP level's null-literal else drops
     * it).
     *
     * <p>Remaining out-of-scope shapes (files stay waivered): a conditional
     * nested in an arm VALUE — not in branch position — registers on the
     * statement sink in outer-before-inner order and stacks at the statement
     * indent, where upstream nests the inner hoist INSIDE the branch block;
     * and arm-interior coercion hoists (upstream's in-branch
     * {@code final FieldWithMetaX … = …;} guards) have no channel here —
     * both appear only in ternary-waivered carriers (e.g. the drr AnnaDsb
     * ladder family).
     */
    public static String buildIfThenElseHoistBlock(RConditionalExpr expr, String localRef,
            String declTypeName, ExpressionCompiler compiler, JavaStatementScope scope,
            HoistArmRenderer arms, Set<JavaClass<?>> refs, Set<JavaClass<?>> wildcards) {
        return buildIfThenElseHoistBlock(expr, localRef, declTypeName, compiler, scope, arms,
                refs, wildcards, null);
    }

    /**
     * facet inLambdaArgSeatIteHoist (PR #355): the {@code hoistNameRegistrar} overload —
     * non-null ONLY on the LAMBDA_CHANNEL path, where auxiliary hoist locals (the #217
     * boolHoist {@code final Boolean …} condition extract) have no statement-hoist sink
     * to register on and ride the caller's per-lambda naming sub-groups instead
     * ({@code StatementHoistSession.registerLambdaScoped} — golden asic/mas PriceRule's
     * in-lambda {@code _boolean}). A {@code null} registrar keeps the sink-based
     * behaviour byte-identically (every pre-#355 caller).
     */
    public static String buildIfThenElseHoistBlock(RConditionalExpr expr, String localRef,
            String declTypeName, ExpressionCompiler compiler, JavaStatementScope scope,
            HoistArmRenderer arms, Set<JavaClass<?>> refs, Set<JavaClass<?>> wildcards,
            java.util.function.Function<String, String> hoistNameRegistrar) {
        // facet condListCoerce (PR #327): an explicit `else empty` (REmptyLiteral — the
        // parser's no-value else, DISTINCT from the empty-LIST literal DefaultElseRule
        // synthesizes) also selects the initializer form at a null-empty seat — upstream
        // compiles `empty` to JavaLiteral.NULL, whose declareAsVariable literal-else rule
        // is exactly the elseless one (the #303 hasEffectiveElse broadening, applied here
        // at the item seat; the general-form seats — a non-null emptyElseValue — keep
        // their bytes: the fca Create_TradeCounterpartyReport20__1 rltshRcrd carrier).
        boolean noValueElse = !hasEffectiveElse(expr)
                || expr.elseBranch().map(e -> e instanceof REmptyLiteral).orElse(false);
        boolean initializerForm = noValueElse && "null".equals(arms.emptyElseValue());
        // v3.2 seat 1 (F2 dropped-coercion, the chaos C19Pick rows): upstream's declareAsVariable
        // literal-else rule reaches past the null literal — a conditional whose BOTH arms are
        // value literals takes the initializer form with the else literal as the initializer
        // (`String r = "neg"; if (c) { r = "pos"; }`). The corpus census at the seat: 9,247
        // initializer forms, ALL null-initialised; every non-null literal else the corpus writes
        // sits under a chained or statement-bearing then and keeps the `final` form — so the gate
        // is the witnessed shape exactly (both arms literal, the null-empty item seat) and no
        // vendored file moves.
        // Only the GATE is decided here; the else literal renders below, after the top condition
        // has compiled and the boolHoist sentinel (if any) has registered, and BEFORE the then arm -
        // the position the initializer holds in the emitted text. (The pre-existing final-form path
        // renders the else arm inside appendConditionalChain, AFTER the then arm; a hoist the literal
        // registers - a beyond-long int literal's `bigInteger` decl - therefore numbers ahead of the
        // then arm's here, the emission order of the decl line. Both arms are value literals by the
        // gate and the corpus has no carrier - 9,247 vendored initializer forms, all null-initialised
        // - so the then/else order is immaterial at the population; PR #622, the reviews' SF-4 and
        // the round-2 NIT-2.)
        boolean literalElseForm = !initializerForm && "null".equals(arms.emptyElseValue())
                && isValueLiteral(expr.thenBranch())
                && expr.elseBranch().map(ControlFlowHandler::isValueLiteral).orElse(false);
        StringBuilder sb = new StringBuilder();

        // facet boolHoistInIte (PR #217): a bare function-call TOP condition compiles
        // ITEM-typed Boolean (MapperS.of-wrapped) and — per the PR #179
        // boolean_condition_hoist law applied at the ifThenElseResult hoist seat —
        // hoists `final Boolean <id> = <inner>;` BEFORE the ifThenElseResult decl
        // (the golden line order) and guards the if with `(<id> == null ? false :
        // <id>)` instead of the inline `.getOrDefault(false)`. The shared
        // HandlerHelper.isBareFunctionCallCondition gate + the structural MapperS.of
        // invocation-wrap witness (unwrapToBuilder().isPresent()) mirror
        // FunctionExpressionRenderer.renderConditionalAssignment exactly (the
        // same-law invariant). The decl rides the sink's StatementHoistSession
        // `boolean` group (the SAME session instance the ifThenElseResult sentinel
        // rides — markStatementHoistSink wires the renderer's session into the
        // scope): singleton -> `_boolean`, n >= 2 -> `boolean0..n-1`. The TOP
        // condition is compiled ONCE here and threaded into appendConditionalChain
        // (no recompile / double hoist-registration); nested conditions keep the
        // inline `.getOrDefault(false)` (no carrier). Declines without a hoist sink
        // (rule/POJO-condition paths) — green-safe: zero goldens carry the inline
        // MapperS.of(<fn>.evaluate(...)).getOrDefault(false) condition in this seat.
        JavaStatementBuilder topCond = compiler.compile(expr.condition(), null, scope);
        String boolSentinel = null;
        JavaStatementScope sink = scope == null ? null : scope.findStatementHoistSink();
        if ((sink != null || hoistNameRegistrar != null)
                && HandlerHelper.isBareFunctionCallCondition(expr.condition())
                && topCond instanceof JavaExpression condExpr
                && (condExpr.unwrapToBuilder().isPresent() || hoistNameRegistrar != null)) {
            // boolHoist renders the UNWRAPPED inner — collect ITS refs only (the
            // wrap's MAPPER_S drops atomically per the JavaExpression.wrappedInMapperSOf
            // wrap-factory contract); adding topCond's refs would leave a stale
            // MapperS import (Copilot R1).
            // facet inLambdaArgSeatIteHoist (PR #355): on the LAMBDA channel a bare
            // fn-call condition compiles UNWRAPPED already (the in-lambda
            // Boolean-typed `isX.evaluate(item.get())` — no MapperS.of witness), so
            // the compiled expression IS the hoist value (golden asic/mas PriceRule
            // `final Boolean _boolean = isDefaultPrice.evaluate(item.get());`). The
            // sink path keeps the wrap-witness requirement byte-identically.
            JavaStatementBuilder inner = condExpr.unwrapToBuilder().isPresent()
                    ? condExpr.unwrapToBuilder().get()
                    : condExpr;
            refs.addAll(inner.getRefs());
            wildcards.addAll(inner.getStaticWildcardImports());
            boolSentinel = hoistNameRegistrar != null
                    ? hoistNameRegistrar.apply(StatementHoistSession.BOOLEAN)
                    : sink.statementHoistSession().register(StatementHoistSession.BOOLEAN);
            sb.append("final Boolean ").append(boolSentinel).append(" = ")
              .append(HandlerHelper.render(inner)).append(";\n");
        } else if (!(expr.condition() instanceof RBooleanLiteral)) {
            // not hoisting: appendConditionalChain renders topCond as-is with the
            // inline `.getOrDefault(false)`, so its (MapperS-bearing) refs ARE used.
            // facet ifCondLiteralBare (PR #437, finding #34): a boolean-LITERAL
            // condition renders the bare Java literal instead (appendConditionalChain),
            // discarding the compiled MapperS.of wrap AND its MAPPER_S ref (golden
            // func-if-literal-call B carries no MapperS import).
            refs.addAll(topCond.getRefs());
            wildcards.addAll(topCond.getStaticWildcardImports());
        }

        String literalElseInit = null;
        if (literalElseForm) {
            // rendered HERE, right before its one use - after the condition and the sentinel, before
            // the then arm (the reviews' SF-4; the ordering note at the gate above); a null render -
            // not expected of a value literal - keeps the `final` form, and appendConditionalChain
            // then renders the else arm itself
            literalElseInit = arms.renderArm(expr.elseBranch().get());
            initializerForm = literalElseInit != null;
        }
        if (initializerForm) {
            sb.append(declTypeName).append(' ').append(localRef).append(" = ")
              .append(literalElseInit != null ? literalElseInit : "null").append(";\n");
        } else {
            sb.append("final ").append(declTypeName).append(' ').append(localRef).append(";\n");
        }
        appendConditionalChain(sb, expr, localRef, 0, false, !initializerForm,
                compiler, scope, arms, refs, wildcards, topCond, boolSentinel,
                hoistNameRegistrar);
        return sb.toString();
    }

    /**
     * v3.2 seat 1: a VALUE literal arm — a string, int, number or boolean literal (upstream's
     * {@code JavaLiteral} class minus the null/empty case the elseless law owns). The
     * initializer-form gate above requires BOTH arms to be one. Its byte-identical twin
     * {@code ChoiceSwitchSupport.isBlockSwitchLiteral} serves the block-lambda switch seats' arm
     * admission - a DIFFERENT law over the same four kinds, declared as a twin rather than
     * consulted (v3.2 seat 7 round 3, the code-quality review's NIT-5).
     */
    static boolean isValueLiteral(RExpression e) {
        return e instanceof com.regnosys.rosetta.ast.expressions.literals.RStringLiteral
                || e instanceof com.regnosys.rosetta.ast.expressions.literals.RIntLiteral
                || e instanceof com.regnosys.rosetta.ast.expressions.literals.RNumberLiteral
                || e instanceof com.regnosys.rosetta.ast.expressions.literals.RBooleanLiteral;
    }

    /**
     * Append one {@code if (…) { local = …; }} level — recursing for folded
     * nested conditionals — at {@code depth} tabs of relative indent.
     *
     * @param chainContinuation true when this level renders right after a
     *        {@code } else } (the else-if chain) — the leading depth-tabs are
     *        already on the line
     * @param emitElse false ONLY for the top level of the initializer form
     *        (the null-literal else is the declaration's initializer)
     */
    private static void appendConditionalChain(StringBuilder sb, RConditionalExpr expr,
            String localRef, int depth, boolean chainContinuation, boolean emitElse,
            ExpressionCompiler compiler, JavaStatementScope scope, HoistArmRenderer arms,
            Set<JavaClass<?>> refs, Set<JavaClass<?>> wildcards,
            JavaStatementBuilder precompiledCond, String boolSentinel,
            java.util.function.Function<String, String> hoistNameRegistrar) {
        // facet boolHoistInIte: the TOP condition is compiled (and any boolHoist
        // sentinel registered + its decl emitted) by buildIfThenElseHoistBlock — so
        // when precompiledCond is supplied we reuse it (no recompile / double
        // hoist-registration) and the if-guard becomes the null-safe ternary when a
        // boolSentinel is present. Nested conditions (recursive calls pass null/null)
        // compile here and keep the inline `.getOrDefault(false)` (no carrier).
        JavaStatementBuilder condBuilder;
        // facet guardedDerefHoist (PR #364): a NESTED conditional's condition compiles in
        // this recursive frame — a guarded deref hoist it registers on the ARM-DEREF
        // channel (an evaluate-arg meta deref: golden AnnaDsbUpiRequestUnderlyingForCredit's
        // `final FieldWithMetaString fieldWithMetaString0 = indexName(…).get();` line
        // BEFORE the nested `if (`) drains HERE at this frame's depth. CHANNEL-only (a
        // real statement-hoist sink keeps its pre-#364 statement-level lift) and
        // non-continuation frames only (an else-if header has no legal pre-`if` line;
        // the else-nesting relocate is a separate facet — no #364 carrier).
        JavaStatementScope nestedCondDerefSink = precompiledCond == null
                && !chainContinuation && scope != null
                && scope.findStatementHoistSink() == null
                ? scope.findArmDerefHoistSink() : null;
        int nestedCondMark = nestedCondDerefSink == null
                ? 0 : nestedCondDerefSink.statementHoistMark();
        boolean condChainHoisted = false;
        // facet ifCondLiteralBare (PR #437, finding #34): a boolean-LITERAL condition
        // renders the bare Java literal (`if (true)`/`} else if (false)`) with NO
        // `.getOrDefault(false)` suffix and NO compiled-cond refs — upstream coerces
        // the condition to primitive boolean and a literal compiles to the bare
        // JavaLiteral (golden func-if-literal-call B). Non-literal conditions keep
        // the wrapped form byte-identically.
        boolean bareLiteralCond = expr.condition() instanceof RBooleanLiteral;
        if (precompiledCond != null) {
            condBuilder = precompiledCond;          // refs already collected by the caller
        } else {
            // facet condChainRungHoist (PR #399): a nested-rung condition that is a
            // hoistable then-chain compiles through the arm renderer's full-sink relay
            // (the #375-A4 pattern at the CONDITION seat) — the relayed decls land on
            // the SAME #364 window this frame drains pre-`if` below. Non-null ONLY
            // when a decl actually relayed; every other condition keeps the plain
            // compile byte-identically.
            condBuilder = nestedCondDerefSink != null
                    ? arms.compileRungConditionChain(expr.condition()) : null;
            condChainHoisted = condBuilder != null;
            if (condBuilder == null) {
                condBuilder = compiler.compile(expr.condition(), null, scope);
            }
            if (!bareLiteralCond) {
                refs.addAll(condBuilder.getRefs());
                wildcards.addAll(condBuilder.getStaticWildcardImports());
            }
        }
        if (nestedCondDerefSink != null) {
            for (String hoist : nestedCondDerefSink.drainStatementHoistsSince(nestedCondMark)) {
                sb.append(tabs(depth))
                  .append(hoist.replace("\n", "\n" + tabs(depth))).append('\n');
            }
        }

        if (!chainContinuation) {
            sb.append(tabs(depth));
        }
        if (boolSentinel != null) {
            sb.append("if ((").append(boolSentinel).append(" == null ? false : ")
              .append(boolSentinel).append(")) {\n");
        } else {
            // facet condChainRungHoist (PR #399): the re-rooted consumer condition is
            // multi-line (`thenArg7\n\t.first()\n\t.mapSingleToItem(…)`) — re-anchor its
            // continuations at this frame's depth (golden's `.first()` one tab deeper
            // than the `if (`). Gated on the hoist so every pre-#399 single-line
            // condition renders byte-identically.
            if (bareLiteralCond) {
                // facet ifCondLiteralBare (PR #437, finding #34): the bare literal rung.
                sb.append("if (")
                  .append(((RBooleanLiteral) expr.condition()).value())
                  .append(") {\n");
            } else if (condBuilder instanceof
                    com.regnosys.rosetta.generator.java.statement.builder.JavaConditionalExpression
                            condTernary) {
                // facet ruleMetaLiftResidueSeats (seat 6, the ladder-rung consumer): the
                // #331 STATEMENT-seat exists-wrap returns a LIVE JavaConditionalExpression;
                // at this rung-condition seat the render previously threw ("Expected
                // JavaExpression but got JavaConditionalExpression") and the WHOLE body
                // refused to a TODO comment (the hkma/asic UnderlyingIdentificationTypeRule
                // class). Golden collapses to the parenthesized inline ternary with the
                // existence check's `.getOrDefault(false)` DISTRIBUTED into both branches:
                //   if ((string == null ? exists(MapperS.<FieldWithMetaString>ofNull())
                //        .getOrDefault(false) : exists(MapperS.of(FieldWithMetaString
                //        .builder().setValue(string).build())).getOrDefault(false))) {
                // — the pre-`if` statement-hoist drain above places the value decl exactly
                // where golden puts it (inside the owning branch, ahead of the inner if).
                // Green-safe by construction: the pre-fix render REFUSED the whole body, so
                // no green file carries ANY form of this seat.
                // mapExpression with a JavaExpression-returning mapper on a conditional
                // always yields a conditional (its L96-98 contract) — the cast is total
                // (the indep review's NIT-1: the instanceof-ternary's else arm was dead).
                // Copilot #576 R1: the distributed branches are stamped BOOLEAN —
                // `.getOrDefault(false)` yields the primitive, and keeping the branch's
                // pre-call Mapper type would mistype any later typed processing.
                JavaExpression collapsedCond =
                        ((com.regnosys.rosetta.generator.java.statement.builder.JavaConditionalExpression)
                                condTernary.mapExpression(e -> JavaExpression.from(
                                        e.renderToString() + ".getOrDefault(false)",
                                        com.rosetta.util.types.JavaPrimitiveType.BOOLEAN, e.getRefs(),
                                        e.getStaticWildcardImports())))
                                .collapseToSingleExpression(scope);
                refs.addAll(collapsedCond.getRefs());
                wildcards.addAll(collapsedCond.getStaticWildcardImports());
                sb.append("if (").append(collapsedCond.renderToString()).append(") {\n");
            } else {
                String condRender = HandlerHelper.render(condBuilder);
                if (condChainHoisted) {
                    condRender = reanchorContinuation(condRender, depth);
                }
                // v3.2 seat 13 (D53 - the bare-boolean-input CONDITION law at the hoist-block seat, the chaos s23
                // C23CmpArm / s30 C30TwoOp / s24 C24Segments rows): a bare boolean INPUT as the top condition renders
                // the raw-local guard `if ((flag == null ? false : flag)) {` - the released plugin's convertNullSafe of
                // the Boolean item, the SAME HandlerHelper.bareBooleanInputGuardOrNull the then-chain ladder seats read
                // (LAW 69) - where this seat had rendered `if (MapperS.of(flag).getOrDefault(false)) {` (compiling,
                // byte-different; zero vendored goldens carry that form at any seat).
                String inputGuard = HandlerHelper.bareBooleanInputGuardOrNull(expr.condition(), condRender);
                if (inputGuard != null) {
                    sb.append("if (").append(inputGuard).append(") {\n");
                } else {
                    sb.append("if (").append(condRender).append(".getOrDefault(false)) {\n");
                }
            }
        }

        RExpression thenBranch = expr.thenBranch();
        if (thenBranch instanceof RConditionalExpr nestedThen) {
            // A nested then folds as a FULL inner if/else block one level
            // deeper, assigning the SAME local (upstream completeAsAssignment
            // distributes; nested levels always emit their else).
            // facet condMapperFormSeats (seat 7, Rung M2): a nested-THEN inner
            // condition that is a bare fn-call Boolean hoists its own
            // `final Boolean <sentinel> = …;` INSIDE the owning branch before the
            // inner `if` — declaration-before-use inside the already-open block,
            // the #378-C rung law at the nested-THEN seat (golden csa
            // CryptoAssetUnderlyingIndicatorLeg1/2Rule `final Boolean _boolean =
            // qualify_AssetClass_Commodity.evaluate(…); if ((_boolean == null ?
            // false : _boolean)) {`). The gate mirrors the #217/#378-C arms
            // exactly (isBareFunctionCallCondition + the MapperS.of wrap witness
            // on the sink path, the registrar's unwrapped compile on the lambda
            // channel); or-chain/plain inner conditions keep the inline
            // `.getOrDefault(false)` byte-identically (golden CryptoAsset's own
            // second rung). Green-safe by the CORRECTED seat-7 scan (the indep
            // review's MUST-FIX — the first scan's `\t` ERE matched a literal
            // 't', a broken instrument): goldens carry 206 nested
            // `if (MapperS.of(` conditions but EVERY one is a NAVIGATION chain
            // (`.<X>map(…)` between the wrap and `.getOrDefault(false)`), which
            // isBareFunctionCallCondition rejects; the bare-fn-call form this
            // hoist replaces (`if (MapperS.of(<fn>.evaluate(…)).getOrDefault(
            // false))` with NO navigation) has ZERO golden carriers — the hoist
            // is the only golden shape at the replaced seat.
            JavaStatementScope innerSink = scope == null
                    ? null : scope.findStatementHoistSink();
            JavaStatementBuilder innerPrecompiled = null;
            String innerSentinel = null;
            if ((innerSink != null || hoistNameRegistrar != null)
                    && HandlerHelper.isBareFunctionCallCondition(nestedThen.condition())) {
                JavaStatementBuilder innerCond =
                        compiler.compile(nestedThen.condition(), null, scope);
                boolean innerWitness = innerCond instanceof JavaExpression innerCondExpr
                        && innerCondExpr.unwrapToBuilder().isPresent();
                if (innerWitness || (hoistNameRegistrar != null
                        && innerCond instanceof JavaExpression)) {
                    JavaStatementBuilder inner = innerWitness
                            ? ((JavaExpression) innerCond).unwrapToBuilder().get()
                            : innerCond;
                    refs.addAll(inner.getRefs());
                    wildcards.addAll(inner.getStaticWildcardImports());
                    innerSentinel = hoistNameRegistrar != null
                            ? hoistNameRegistrar.apply(StatementHoistSession.BOOLEAN)
                            : innerSink.statementHoistSession()
                                    .register(StatementHoistSession.BOOLEAN);
                    innerPrecompiled = innerCond;
                    sb.append(tabs(depth + 1)).append("final Boolean ").append(innerSentinel)
                      .append(" = ").append(HandlerHelper.render(inner)).append(";\n");
                } else {
                    // Compiled but no witness — thread the compiled condition into the
                    // recursion (refs collected here), keeping the inline form.
                    refs.addAll(innerCond.getRefs());
                    wildcards.addAll(innerCond.getStaticWildcardImports());
                    innerPrecompiled = innerCond;
                }
            }
            appendConditionalChain(sb, nestedThen, localRef, depth + 1, false, true,
                    compiler, scope, arms, refs, wildcards, innerPrecompiled, innerSentinel,
                    hoistNameRegistrar);
            sb.append('\n');
        } else {
            // facet condListCoerce (PR #327): an arm-VALUE-interior hoist (a conditional
            // nested in the arm's ctor arg, an evaluate-arg meta-deref, …) relocates
            // INSIDE the owning branch — upstream declareAsVariable declares at the
            // consumption scope, i.e. the branch block (the previously-documented
            // out-of-scope gap: the fork stacked them at the statement indent). Drained
            // via the sink's mark/drain window around the arm render; no-op (empty
            // drain) for every arm that registers nothing — the dominant shape.
            // facet inLambdaArgSeatIteHoist (PR #355): the LAMBDA_CHANNEL twin window —
            // on the sink-less path an arm-interior arg-seat conditional registered its
            // raw-token block on the enclosing lambda's pending channel during
            // renderArm; relocate it INSIDE the owning branch (golden GetOptn esma:
            // ifThenElseResult0/1 declared inside the ifThenElseResult2 branch).
            // facet choiceDeepNavLadderArm (PR #396): a RUNE CHOICE-subject switch arm
            // renders as the upstream deep option-nav ladder — a STATEMENT block
            // assigning the target at every leaf (golden cdm6
            // Qualify_AssetClass_Credit ifThenElseResult2). Null for every other arm
            // (the pre-#396 single-expression path, byte-identical).
            String armStatements = arms.renderArmStatements(thenBranch, localRef, depth + 1);
            if (armStatements != null) {
                sb.append(tabs(depth + 1)).append(armStatements).append('\n');
            } else {
            JavaStatementScope armSink = scope == null ? null : scope.findStatementHoistSink();
            // facet guardedDerefHoist (PR #364): the ARM-DEREF channel joins the same
            // relocate-into-branch window — the pathed-conditional arm scope carries
            // no full sink (deliberately), so a guarded deref hoist registered during
            // the arm compile drains through this fallback (golden AnnaDsbCredit's
            // in-arm fieldWithMetaString1/2; UpdateIndexTransition's in-arm operand
            // hoist pair). Fallback-only: a real sink keeps its pre-#364 window.
            if (armSink == null && scope != null) {
                armSink = scope.findArmDerefHoistSink();
            }
            int armMark = armSink == null ? 0 : armSink.statementHoistMark();
            JavaStatementScope armLambdaBoundary = armSink != null || scope == null
                    ? null : scope.findPendingLambdaHoistBoundary();
            int armLambdaMark = armLambdaBoundary == null ? 0
                    : armLambdaBoundary.pendingLambdaHoistMark();
            // facet pathedLadderArmSeatRegistration (PR #399): the FUNCTION-path arm
            // compile registers its owning rung on the #354 blockArmSeatConditionals
            // channel — the arm IS a statement seat mid-BLOCK-render here (this ladder
            // renders if/else statements by construction), so an arm-interior extract's
            // nested-then/ladder conditional block-converts through the EXISTING
            // isCleanLadderContext transparency (the #398/R6 ite-core law at the
            // pathed-SET appendConditionalChain seat; golden AnnaDsb-FRE's
            // ifThenElseResult14 else-arm `.mapSingleToItem(item -> { if … })`).
            // Registrar-carrying (pathed-SET) seats only; the RULE path keeps its
            // unregistered bytes.
            java.util.List<RExpression> prevThenArmSeat = null;
            if (hoistNameRegistrar != null && scope != null
                    && HandlerHelper.findEnclosingRule(expr) == null) {
                prevThenArmSeat = scope.pushBlockArmSeatConditionals(java.util.List.of(expr));
            }
            String armValue;
            try {
                armValue = arms.renderArm(thenBranch);
            } finally {
                if (prevThenArmSeat != null) {
                    scope.popBlockArmSeatConditionals(prevThenArmSeat);
                }
            }
            if (armSink != null) {
                for (String hoist : armSink.drainStatementHoistsSince(armMark)) {
                    sb.append(tabs(depth + 1))
                      .append(hoist.replace("\n", "\n" + tabs(depth + 1))).append('\n');
                }
            }
            if (armLambdaBoundary != null) {
                relocateRawLambdaHoists(sb, armLambdaBoundary, armLambdaMark, depth + 1);
            }
            sb.append(tabs(depth + 1)).append(localRef).append(" = ")
              .append(reanchorContinuation(armValue, depth + 1))
              .append(";\n");
            }
        }

        Optional<RExpression> elseBranch = expr.elseBranch();
        if (elseBranch.isPresent() && elseBranch.get() instanceof RConditionalExpr nestedElse
                && !(emitElse && trailingRunElidesToNull(nestedElse))) {
            // facet boolHoistElseRungNest (PR #378, C): an else-RUNG whose condition is a
            // bare fn-call Boolean hoists its own `final Boolean booleanN = …;` — the decl
            // needs a block, so the rung renders NESTED (`} else {` + decl + inner if/else
            // one level deeper) instead of the flat `} else if (` (upstream declaration-
            // before-use, the #179 nested-else law at the ladder-rung seat; golden cdm6
            // MapEntityIdentifierTypeEnum's boolean1 — and the second member renumbers the
            // session group boolean0/1). The gate mirrors the #217 TOP-condition hoist
            // exactly (isBareFunctionCallCondition + the MapperS.of wrap witness + a real
            // sink — the LAMBDA-channel registrar is not threaded into rungs, so those
            // paths keep the flat chain); the rung condition compiles ONCE here and
            // threads into the recursion on BOTH paths (no recompile / double
            // registration). Zero goldens carry the flat wrapped fn-call rung this
            // replaces (corpus-verified — the one `} else if (MapperS.of(` golden,
            // NotionalLeg, is a navigation chain the bare-fn-call gate rejects).
            JavaStatementScope rungSink = scope == null ? null : scope.findStatementHoistSink();
            // facet inLambdaBoolRungNest (PR #383): the #378-C rung hoist gains its
            // LAMBDA-CHANNEL twin — the registrar threads into the rungs now (golden drr
            // TotalNotionalQuantityLeg1: five bare-fn rung conditions hoist through the
            // per-lambda `boolean` sub-group, numbering boolean0..4 in pre-order, each
            // rung nesting `} else { final Boolean booleanN = …; if (…) {…} }`). The
            // lambda channel mirrors the #355 TOP-condition law exactly: an in-lambda
            // bare-fn condition compiles UNWRAPPED (no MapperS.of witness required).
            // Green-safe: ZERO goldens carry a `_boolean` singleton alongside a wrapped
            // `} else if (MapperS.of(` fn-call rung (corpus-verified — golden always
            // hoists bare-fn rungs on both channels), so no green `_boolean` renumbers.
            if ((rungSink != null || hoistNameRegistrar != null)
                    && HandlerHelper.isBareFunctionCallCondition(nestedElse.condition())) {
                JavaStatementBuilder rungCond =
                        compiler.compile(nestedElse.condition(), null, scope);
                boolean rungWitness = rungCond instanceof JavaExpression rungCondExpr
                        && rungCondExpr.unwrapToBuilder().isPresent();
                if (rungWitness || (hoistNameRegistrar != null
                        && rungCond instanceof JavaExpression)) {
                    JavaStatementBuilder inner = rungWitness
                            ? ((JavaExpression) rungCond).unwrapToBuilder().get()
                            : rungCond;
                    refs.addAll(inner.getRefs());
                    wildcards.addAll(inner.getStaticWildcardImports());
                    String rungSentinel = hoistNameRegistrar != null
                            ? hoistNameRegistrar.apply(StatementHoistSession.BOOLEAN)
                            : rungSink.statementHoistSession()
                                    .register(StatementHoistSession.BOOLEAN);
                    sb.append(tabs(depth)).append("} else {\n");
                    sb.append(tabs(depth + 1)).append("final Boolean ").append(rungSentinel)
                      .append(" = ").append(HandlerHelper.render(inner)).append(";\n");
                    appendConditionalChain(sb, nestedElse, localRef, depth + 1, false, true,
                            compiler, scope, arms, refs, wildcards, rungCond, rungSentinel,
                            hoistNameRegistrar);
                    sb.append('\n').append(tabs(depth)).append('}');
                    return;
                }
                // Compiled but no wrap witness — keep the flat chain byte-identically,
                // threading the already-compiled condition (refs collected here, exactly
                // the no-hoist arm of the top-condition seat).
                refs.addAll(rungCond.getRefs());
                wildcards.addAll(rungCond.getStaticWildcardImports());
                sb.append(tabs(depth)).append("} else ");
                appendConditionalChain(sb, nestedElse, localRef, depth, true, true,
                        compiler, scope, arms, refs, wildcards, rungCond, null,
                        hoistNameRegistrar);
                return;
            }
            // facet ruleMetaLiftResidueSeats (seat 6, the ladder-rung nested-else form):
            // an else-RUNG whose condition is an EXISTS over a rule invocation may
            // compile to the #315/#331 distributed JavaConditionalExpression with its
            // value hoist registered on the statement sink; the hoist needs a block, so
            // the rung renders NESTED (`} else {` + the drained `final String string =
            // …;` + the inner if with the collapsed parenthesized ternary — golden
            // hkma/asic UnderlyingIdentificationTypeRule) instead of the flat
            // `} else if (`, exactly the #378-C declaration-before-use law at this rung
            // class. The condition compiles ONCE here and threads into the recursion on
            // both paths (no recompile / double registration; a decline re-registers
            // any drained hoists so the top-level prepend keeps today's bytes).
            // Green-safe by construction: this rung class REFUSED whole pre-seat
            // ("Expected JavaExpression but got JavaConditionalExpression"), so no
            // green file carries any form of it.
            JavaStatementScope existsRungSink = scope == null
                    ? null : scope.findStatementHoistSink();
            if (existsRungSink != null
                    && nestedElse.condition()
                            instanceof com.regnosys.rosetta.ast.expressions.unary.RExistenceExpr rungExists
                    && rungExists.argument() instanceof RSymbolReference rungRef
                    && rungRef.symbol()
                            .filter(com.regnosys.rosetta.ast.functions.RRule.class::isInstance)
                            .isPresent()) {
                int rungHoistMark = existsRungSink.statementHoistMark();
                JavaStatementBuilder rungCondC =
                        compiler.compile(nestedElse.condition(), null, scope);
                java.util.List<String> rungHoists =
                        existsRungSink.drainStatementHoistsSince(rungHoistMark);
                if (rungCondC instanceof
                        com.regnosys.rosetta.generator.java.statement.builder.JavaConditionalExpression
                        && !rungHoists.isEmpty()) {
                    sb.append(tabs(depth)).append("} else {\n");
                    for (String hoist : rungHoists) {
                        sb.append(tabs(depth + 1))
                          .append(hoist.replace("\n", "\n" + tabs(depth + 1))).append('\n');
                    }
                    appendConditionalChain(sb, nestedElse, localRef, depth + 1, false, true,
                            compiler, scope, arms, refs, wildcards, rungCondC, null,
                            hoistNameRegistrar);
                    sb.append('\n').append(tabs(depth)).append('}');
                    return;
                }
                for (String hoist : rungHoists) {
                    existsRungSink.registerStatementHoist(hoist);
                }
                // The recursion skips refs on a precompiled condition (the #378
                // "refs already collected by the caller" contract) — collect here.
                refs.addAll(rungCondC.getRefs());
                wildcards.addAll(rungCondC.getStaticWildcardImports());
                sb.append(tabs(depth)).append("} else ");
                appendConditionalChain(sb, nestedElse, localRef, depth, true, true,
                        compiler, scope, arms, refs, wildcards, rungCondC, null,
                        hoistNameRegistrar);
                return;
            }
            // Nested else chains `} else if (…) {` at the SAME level.
            sb.append(tabs(depth)).append("} else ");
            appendConditionalChain(sb, nestedElse, localRef, depth, true, true,
                    compiler, scope, arms, refs, wildcards, null, null,
                    hoistNameRegistrar);
        } else if (elseBranch.isPresent() && elseBranch.get() instanceof RConditionalExpr
                && emitElse) {
            // facet nullResultArmElision (PR #345): a TRAILING run of `else if (C)
            // then empty` levels folds into the final null else — upstream's ladder
            // ends `else { result = null; }` without materialising arms whose value
            // IS the null the fall-through produces (`if C then empty else empty ≡
            // empty`). Golden FinancialUnitToISO20022UnitOfMeasure drr: the
            // VALUE_PER_DAY / VALUE_PER_PERCENT `then empty` cases elide while the
            // NON-trailing first null arm stays explicit (the elision is
            // trailing-run-only). A run ending in a REAL else value declines
            // (trailingRunElidesToNull false) and keeps the explicit chain.
            sb.append(tabs(depth)).append("} else {\n");
            sb.append(tabs(depth + 1)).append(localRef).append(" = ")
              .append(arms.emptyElseValue()).append(";\n");
            sb.append(tabs(depth)).append('}');
        } else if (!emitElse) {
            sb.append(tabs(depth)).append('}');
        } else if (elseBranch.isPresent() && !isEmptyListLiteral(elseBranch.get())) {
            // A present non-empty-list else (incl. an REmptyLiteral reaching a
            // GENERAL-form seat, e.g. the ComparisonResult arm — byte-preserved via
            // renderArm exactly as before) — the arm-interior relocation applies
            // exactly as the then-arm (both windows).
            JavaStatementScope armSink = scope == null ? null : scope.findStatementHoistSink();
            // facet guardedDerefHoist (PR #364): the ARM-DEREF channel fallback — the
            // else-arm twin of the then-arm window above.
            if (armSink == null && scope != null) {
                armSink = scope.findArmDerefHoistSink();
            }
            int armMark = armSink == null ? 0 : armSink.statementHoistMark();
            JavaStatementScope armLambdaBoundary = armSink != null || scope == null
                    ? null : scope.findPendingLambdaHoistBoundary();
            int armLambdaMark = armLambdaBoundary == null ? 0
                    : armLambdaBoundary.pendingLambdaHoistMark();
            // facet pathedLadderArmSeatRegistration (PR #399): the else-arm twin of the
            // then-arm #354 registration above (golden AnnaDsb-FRE's ifThenElseResult14
            // trailing-else block lambda).
            java.util.List<RExpression> prevElseArmSeat = null;
            if (hoistNameRegistrar != null && scope != null
                    && HandlerHelper.findEnclosingRule(expr) == null) {
                prevElseArmSeat = scope.pushBlockArmSeatConditionals(java.util.List.of(expr));
            }
            String elseValue;
            try {
                elseValue = arms.renderArm(elseBranch.get());
            } finally {
                if (prevElseArmSeat != null) {
                    scope.popBlockArmSeatConditionals(prevElseArmSeat);
                }
            }
            sb.append(tabs(depth)).append("} else {\n");
            if (armSink != null) {
                for (String hoist : armSink.drainStatementHoistsSince(armMark)) {
                    sb.append(tabs(depth + 1))
                      .append(hoist.replace("\n", "\n" + tabs(depth + 1))).append('\n');
                }
            }
            if (armLambdaBoundary != null) {
                relocateRawLambdaHoists(sb, armLambdaBoundary, armLambdaMark, depth + 1);
            }
            sb.append(tabs(depth + 1)).append(localRef).append(" = ")
              .append(reanchorContinuation(elseValue, depth + 1)).append(";\n");
            sb.append(tabs(depth)).append('}');
        } else {
            sb.append(tabs(depth)).append("} else {\n");
            sb.append(tabs(depth + 1)).append(localRef).append(" = ")
              .append(arms.emptyElseValue()).append(";\n");
            sb.append(tabs(depth)).append('}');
        }
    }

    /**
     * facet inLambdaArgSeatIteHoist (PR #355): splice the RAW-token pending lambda
     * hoists an arm compile registered (at or after {@code mark} on the enclosing
     * lambda's channel) INSIDE the owning branch at {@code depth} tabs of relative
     * indent — upstream declares an arm-value-interior hoist at its consumption
     * scope, the branch block. ONLY {@link JavaRawStatement} entries relocate
     * (token-only content, render-safe mid-compile); any other pending kind is
     * RE-REGISTERED for the lambda-top drain — rendering a decl-object statement
     * here would resolve its {@code GeneratedIdentifier} and close naming scopes
     * mid-compile (the #346 eager-render law).
     */
    private static void relocateRawLambdaHoists(StringBuilder sb, JavaStatementScope boundary,
            int mark, int depth) {
        List<JavaStatement> drained = boundary.drainPendingLambdaHoistsSince(mark);
        if (drained.isEmpty()) {
            return;
        }
        for (JavaStatement stmt : drained) {
            if (!(stmt instanceof JavaRawStatement)) {
                boundary.registerPendingLambdaHoist(stmt);
                continue;
            }
            StringBuilder tmp = new StringBuilder();
            stmt.render(tmp);
            String text = tmp.toString();
            while (text.endsWith("\n")) {
                text = text.substring(0, text.length() - 1);
            }
            sb.append(tabs(depth)).append(text.replace("\n", "\n" + tabs(depth))).append('\n');
        }
    }

    /**
     * True when the conditional carries a REAL else: present and not the
     * zero-element list literal DefaultElseRule synthesizes for an elseless
     * {@code if/then} (both denote "no value" — the renderer's
     * {@code isEmptyListLiteral} twin).
     */
    private static boolean hasEffectiveElse(RConditionalExpr expr) {
        return expr.elseBranch().filter(e -> !isEmptyListLiteral(e)).isPresent();
    }

    /**
     * facet fnIteHoistSeats (PR #361): seat discipline for the NEW #361 typing
     * arms — {@code true} when {@code branch} is a direct branch of a
     * conditional that is itself a then-chain BASE ({@code parent} is an
     * {@link RThenExpr}). That seat's consumer renders a Mapper-typed decl
     * owned by the #351 Mapper-form ite machinery — an ITEM-typed local
     * spliced there is a form no golden carries (the csa
     * CountryAndProvinceOrTerritoryOfIndividual cp2 AWAY: a bare item local
     * consumed by {@code .then(…)}). The pre-#361 arms keep their reach — this
     * gate mirrors the existing {@code fallbackSeat} discipline for the arms
     * this PR adds, without touching the primary-lookup behaviour of the
     * older arms.
     */
    private static boolean isThenBaseConditionalBranch(RExpression branch) {
        return branch.parent() instanceof RConditionalExpr owner
                && owner.parent() instanceof RThenExpr;
    }

    /**
     * facet nullResultArmElision (PR #345): delegate to the shared
     * {@link HandlerHelper#trailingRunElidesToNull} predicate — the whole-output
     * conditional SET seat ({@code FunctionExpressionRenderer
     * .renderConditionalAssignment}) consumes the SAME law.
     */
    private static boolean trailingRunElidesToNull(RConditionalExpr cond) {
        return HandlerHelper.trailingRunElidesToNull(cond);
    }

    private static boolean isEmptyListLiteral(RExpression expr) {
        return expr instanceof RListLiteral list && list.elements().isEmpty();
    }

    /**
     * Re-anchor an arm value's RELATIVE continuation lines (the
     * {@code \n\t.chainLink} convention) to the arm's depth within the block —
     * the block-relative twin of the renderer's {@code reindentContinuation}.
     */
    private static String reanchorContinuation(String rendered, int depth) {
        if (rendered.indexOf('\n') < 0) {
            return rendered;
        }
        return rendered.replace("\n", "\n" + tabs(depth));
    }

    private static String tabs(int n) {
        return "\t".repeat(n);
    }

    // =========================================================================
    // Switch
    // =========================================================================

    /**
     * Compiles a switch expression: one of the blessed statement-hoist ladders (the deep-then handshake, the
     * ctor instanceof ladder, the data-rule whole-body return ladder — each an upstream form on a measured
     * carrier) or, since v3.2 seat 12 (D52, R1), a REFUSAL for every other switch — three sites in render order:
     * {@code LITERAL_SWITCH_TERNARY_STUB} (seat 7) for a literal guard over a lambda-bound subject, BEFORE the
     * case loop; {@code TYPE_SWITCH_TERNARY_STUB} (v3.1 C0) per case whose NAME guard resolves to a type, INSIDE
     * the loop; {@code SWITCH_TERNARY_STUB} (R1) at the tail for everything else.
     *
     * <p>Until seat 12 the residual path rendered each case as a nested ternary using {@link Objects#equals}
     * ({@code Objects.equals(guard1, arg) ? result1 : … : defaultResult}, the default last, {@code null} when
     * absent) — never upstream's bytes: the goldens carry no chained ternary, upstream folds EVERY switch into
     * a hoisted {@code switchArgument} if-ladder, and the render was non-compiling or always false. The case
     * loop still runs so the per-case type-keyed refusal ({@code TYPE_SWITCH_TERNARY_STUB}) keeps its
     * precedence; its assembled text is discarded (its deletion BANKED — see {@link #renderGuard}).
     *
     * @param expr     the switch expression node
     * @param ctx      the current expression compilation context
     * @param compiler the parent compiler for recursive compilation of sub-expressions
     * @return a hoisted-ladder {@link JavaStatementBuilder}; never a ternary — the residual path throws
     */
    public JavaStatementBuilder handle(RSwitchExpr expr, ExpressionContext ctx, ExpressionCompiler compiler) {
        // facet ctorSwitchInstanceofLadder (PR #367): a TYPE-keyed switch at a
        // ctor-FIELD seat over an extends-based model subject hoists the upstream
        // instanceof ASSIGNMENT ladder + splices the bare local — the #181 family's
        // SWITCH sibling (golden cdm6 MapRateOptionWithLocation
        // `.setFloatingRateIndex(ifThenElseResult0)` with the hoisted
        // `final FloatingRateIndex ifThenElseResult0; if (fpmlRate == null) {…}
        // else if (fpmlRate instanceof FloatingRateCalculation) { final
        // FloatingRateCalculation floatingRateCalculation = (FloatingRateCalculation)
        // fpmlRate; ifThenElseResult0 = mapFloatingRateIndex.evaluate(
        // floatingRateCalculation); } else {…}`). Declines to the residual path
        // (TYPE_SWITCH_TERNARY_STUB per resolvable case since v3.1 C0, else the R1 refusal since v3.2 seat 12;
        // the legacy ternary before them) on any non-clean shape.
        // v3.2 seat 2 (Law 3, the chaos C5Forms `graded` rows): the deep-then literal-switch
        // CONSUMER handshake — FIRST in the arm chain, node-identity gated exactly like the
        // conditional twin in handle(RConditionalExpr); every non-blessed switch keeps its
        // existing route by construction.
        JavaStatementBuilder deepThenSwitch = hoistAsDeepThenSwitchLadderOrNull(expr, ctx, compiler);
        if (deepThenSwitch != null) {
            return deepThenSwitch;
        }
        JavaStatementBuilder ctorLadder = hoistCtorSwitchInstanceofLadderOrNull(expr, ctx, compiler);
        if (ctorLadder != null) {
            return ctorLadder;
        }
        // Coverage wave D (datarule): a WHOLE-BODY switch over a CHOICE-valued
        // attribute inside a DATA-TYPE condition hoists the upstream RETURN
        // ladder (golden ExerciseInstructionIsOptionPayout /
        // FinalCalculationPeriodDateAdjustmentIsInterestRatePayout) through the
        // statement-hoist sink; the compiled DEFAULT is the returned consumer
        // (the datarule render's ofNullSafe coercion wraps it). Declines to the
        // residual path below (TYPE_SWITCH_TERNARY_STUB per resolvable case - the choice-option guards -
        // since v3.1 C0, else the R1 refusal since v3.2 seat 12) on any non-carrier shape.
        JavaStatementBuilder dataRuleLadder = hoistDataRuleSwitchReturnLadderOrNull(expr, ctx, compiler);
        if (dataRuleLadder != null) {
            return dataRuleLadder;
        }
        // PR-A §9.1 C3a.4.i: operand-refs flowthrough from argument and all
        // case expressions.
        // facet implicit_operand_synthesis (PR #218): a bare `then switch …` /
        // extract-lambda `switch` over the piped value elides its argument, so
        // argument() is null; substitute the synthetic implicit (→ thenArg / item).
        // facet deep_value_then_hoist (PR #250): the inline chained-ternary switch
        // (`Objects.equals(g, arg) ? r : …` - never written since v3.2 seat 12: the loop's product is
        // discarded and R1 refuses after it; the suppression stays for the per-case refusal's
        // precedence) is a cascade-UNSAFE seat for the deep
        // VALUE-then hoist — same rationale as the if/else ternary in handle(RConditionalExpr):
        // a `final Mapper*<X> thenArg = …;` decl hoisted out of an inline case arm would
        // force a block restructure. Suppress the deep value-then hoist while compiling the
        // switch argument + case expressions (a value-then there declines to its inline form;
        // the SET-position block switch is FunctionExpressionRenderer.renderSwitchAssignment,
        // not this inline path).
        // v3.2 seat 7 round 2 (the code-quality review's MF-1) - THE LOUD REGISTER's literal twin: a LITERAL-guarded
        // switch whose subject is bound by an enclosing inline function reaching this ternary compared the guard
        // against a MAPPER render (`Objects.equals("r", item)`) - it COMPILED and was ALWAYS FALSE, the wrong-result
        // emission the seat's literal-keyed control measured before the literal-guard block existed (REFUSED at
        // LITERAL_SWITCH_TERNARY_STUB before the case loop since). The block renders
        // the witnessed shapes (CollectionHandler.compileLiteralSwitchBlockLambda); every shape it declines REFUSES
        // here, by name, instead of emitting silently wrong Java - the round-1 narrowing of the literal arm kinds had
        // called this fall-through "the LOUD ternary refusal" when the type-keyed refusal below fires for NAME guards
        // alone. Scoped to the MEASURED class (a lambda-bound subject); a literal guard against any other Mapper-rendered
        // subject would be as false, a hypothesis no fixture measures - banked. No corpus carrier (the vendored census
        // pins zero literal-guarded lambda-direct switches; the chaos `switched` alias renders the block); the seat
        // suite's control5 proves the site able to fire.
        if (literalGuardedOverInlineFunctionSubject(expr)) {
            throw SilentDegradation.refuse(SilentDegradation.Site.LITERAL_SWITCH_TERNARY_STUB,
                    "literal-keyed switch over a lambda-bound subject at a seat with no block renderer"
                            + " (the ternary would compare the literal against a Mapper - always false)",
                    expr);
        }
        ctx.scope().pushThenValueHoistSuppression();
        try {
            JavaStatementBuilder argBuilder =
                    compiler.compile(HandlerHelper.orSyntheticImplicit(expr.argument(), expr), ctx.expectedType(), ctx.scope());
            String argument = HandlerHelper.render(argBuilder);

            Set<JavaClass<?>> refs = new HashSet<>(argBuilder.getRefs());
            Set<JavaClass<?>> wildcards = new HashSet<>(argBuilder.getStaticWildcardImports());

            StringBuilder sb = new StringBuilder();

            String defaultResult = "null";
            boolean first = true;
            for (RSwitchCase switchCase : expr.cases()) {
                JavaStatementBuilder caseBuilder =
                        compiler.compile(switchCase.expression(), ctx.expectedType(), ctx.scope());
                refs.addAll(caseBuilder.getRefs());
                wildcards.addAll(caseBuilder.getStaticWildcardImports());
                String caseResult = HandlerHelper.render(caseBuilder);
                if (switchCase.isDefault()) {
                    defaultResult = caseResult;
                    continue;
                }
                if (!first) {
                    sb.append(" : ");
                }
                String guard = renderGuard(switchCase.guard().orElse(null));
                sb.append("Objects.equals(").append(guard).append(", ").append(argument).append(") ? ").append(caseResult);
                first = false;
            }
            // v3.2 seat 12 (D52, COUNTERS FIRST - the LOUD register's RESIDUAL switch site): a switch that passed the
            // two guarded sites above (the literal-over-lambda-subject check and renderGuard's type-keyed refusal per
            // case) and still reached this seat would have rendered `Objects.equals(<raw source name>, <Mapper>) ? ..
            // : ..` - non-compiling (`Red` / `Long` / `long` undeclared) or always false (a literal against a Mapper),
            // never upstream's bytes: upstream folds EVERY switch into a hoisted `switchArgument` if-ladder and the
            // goldens carry no chained ternary. The seat-2 `java.util.Objects` ref this tail registered rode a render
            // that is not made any more. REFUSED by name instead (the chaos M2 rows, s26 / s27 / s31 - the census's
            // 92; re-measured at this seat's D11 on both routes, the IR route serving the switch through this handler
            // as an oracle root); the heal by hoist channel is the seats after this one (D52 decision 3). The case loop
            // above still runs so the per-case type-keyed refusal keeps its precedence and its rows.
            StringBuilder guardKinds = new StringBuilder();
            for (RSwitchCase switchCase : expr.cases()) {
                if (guardKinds.length() > 0) {
                    guardKinds.append('/');
                }
                guardKinds.append(switchCase.isDefault() ? "default"
                        : switchCase.guard().map(g -> g.kind().name()).orElse("none"));
            }
            throw SilentDegradation.refuse(SilentDegradation.Site.SWITCH_TERNARY_STUB,
                    "switch at a seat with no ladder renderer (guards " + guardKinds + "; subject "
                            + (expr.argument() == null ? "elided" : expr.argument().getClass().getSimpleName())
                            + ") - the inline ternary would compare the raw source name against a Mapper render",
                    expr);
        } finally {
            ctx.scope().popThenValueHoistSuppression();
        }
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * facet ctorSwitchInstanceofLadder (PR #367): the ctor-FIELD TYPE-keyed switch
     * hoist — the #181 {@code hoistAsItemLocalOrNull} sibling for {@link RSwitchExpr}
     * values, rendering the upstream instanceof ASSIGNMENT ladder (the #365
     * aliasSwitchValueLadder's RETURN form at the statement seat):
     *
     * <pre>
     * final &lt;CaseFnOutput&gt; ifThenElseResultN;
     * if (&lt;subject&gt; == null) {
     *     ifThenElseResultN = null;
     * } else if (&lt;subject&gt; instanceof &lt;CaseType&gt;) {
     *     final &lt;CaseType&gt; &lt;caseVar&gt; = (&lt;CaseType&gt;) &lt;subject&gt;;
     *     ifThenElseResultN = &lt;caseFn&gt;.evaluate(&lt;caseVar&gt;);
     * } else {
     *     ifThenElseResultN = null;
     * }
     * </pre>
     *
     * <p>The case body compiles under the #221 {@code bindSwitchSubject} re-root
     * (the narrowed implicit resolves to the cast case var, so the evaluate-arg
     * unwraps to the bare name — the #226/#353 caseScope pattern), the sentinel
     * rides the method-spanning {@link StatementHoistSession} ifThenElseResult
     * group, and the block drains INTO an enclosing conditional arm via the #327
     * mark/drain relocation. Green-safe by the #181 construction: the pre-fix
     * seat rendered {@code Objects.equals(<rosetta-name-echo>, MapperS.of(<subj>))
     * ? <fn>.evaluate(item.get()) : null.get()} — a type-literal-vs-Mapper
     * comparison that never compiled, so every carrier was waivered BEFORE this
     * ladder (PR #367; golden cdm6 MapRateOptionWithLocation, byte-identical since —
     * the #181 construction above is the conditional sibling's, not this ladder's PR);
     * a shape it declines is a refusal row (TYPE_ per resolvable case since v3.1 C0, R1 since v3.2 seat 12).
     *
     * <p>Declines ({@code null} → the residual path: {@code TYPE_SWITCH_TERNARY_STUB} per resolvable
     * case since v3.1 C0 — every non-default case the guard gate below admits is one, so a decline past
     * that gate reaches TYPE_; a switch the gate itself declines reaches TYPE_ at its first resolvable
     * case, else the R1 refusal since v3.2 seat 12; the legacy ternary before them) unless EVERY
     * gate passes: a reachable statement-hoist sink; the RKeyValuePair parent
     * (the #365 positive-seat-gate law); an identifier-shaped single MODEL-typed
     * subject whose resolved type is an extends-based {@code RDataType} (a model
     * CHOICE dispatches on option presence, not instanceof — the #365
     * disjointness); every non-default case a resolvable TYPE guard whose result
     * is a BARE single-output non-meta fn reference of ONE agreed output type;
     * and an absent-or-{@code empty} default.
     */
    private JavaStatementBuilder hoistCtorSwitchInstanceofLadderOrNull(RSwitchExpr expr,
            ExpressionContext ctx, ExpressionCompiler compiler) {
        if (ctx.scope() == null
                || !(expr.parent()
                        instanceof com.regnosys.rosetta.ast.expressions.supporting.RKeyValuePair)) {
            return null;
        }
        JavaStatementScope sink = ctx.scope().findStatementHoistSink();
        GeneratorModel gm = compiler.getGeneratorModel();
        var translator = compiler.getTypeTranslator();
        if (sink == null || gm == null || translator == null) {
            return null;
        }
        // The subject: a single MODEL-typed value rendering a bare identifier
        // (the null-guard + instanceof need a plain expression — golden's raw
        // `fpmlRate` input param). An extends-based RDataType only: a model
        // CHOICE's generated class has one-of option attributes, not subtypes.
        if (expr.argument() == null
                || !(expr.argument() instanceof RSymbolReference subjRef)
                || subjRef.symbol().filter(RAttribute.class::isInstance).isEmpty()) {
            return null;
        }
        RAttribute subjAttr = subjRef.symbol().filter(RAttribute.class::isInstance)
                .map(RAttribute.class::cast).orElseThrow();
        RType subjType = unalias(gm.getType(subjAttr));
        if (gm.isMulti(subjAttr)
                || !(subjType instanceof com.regnosys.rosetta.types.RDataTypeRef)) {
            return null;
        }
        JavaStatementBuilder subjBuilder = compiler.compile(expr.argument(), null, ctx.scope());
        String subject = subjBuilder instanceof JavaExpression subjExpr
                && subjExpr.unwrapToBuilder().isPresent()
                ? HandlerHelper.render(subjExpr.unwrapToBuilder().get())
                : HandlerHelper.render(subjBuilder);
        if (!javax.lang.model.SourceVersion.isIdentifier(subject)) {
            return null;
        }
        record LadderArm(JavaClass<?> caseType, String caseVar, String caseValue) { }
        List<LadderArm> arms = new ArrayList<>();
        JavaClass<?> declType = null;
        Set<JavaClass<?>> refs = new HashSet<>();
        Set<JavaClass<?>> wildcards = new HashSet<>();
        for (RSwitchCase switchCase : expr.cases()) {
            if (switchCase.isDefault()) {
                // Only an `empty` default folds into the null else arms (the
                // #226/#353 decline law for non-empty defaults).
                RExpression defExpr = switchCase.expression();
                if (defExpr != null && !(defExpr instanceof REmptyLiteral)) {
                    return null;
                }
                continue;
            }
            RSwitchCaseGuard guard = switchCase.guard().orElse(null);
            JavaClass<?> caseType = guard == null ? null
                    : ChoiceSwitchSupport.resolveCaseType(guard, compiler);
            if (caseType == null) {
                return null;
            }
            // The case result: a BARE fn reference (the implicit-invocation form)
            // whose output is a single non-meta model value — the declared local
            // type; every case must agree on ONE output type (the join).
            if (!(switchCase.expression() instanceof RSymbolReference caseRef)
                    || !caseRef.args().isEmpty()) {
                return null;
            }
            RFunction caseFn = caseRef.symbol().filter(RFunction.class::isInstance)
                    .map(RFunction.class::cast).orElse(null);
            RAttribute caseOut = caseFn == null ? null : caseFn.output().orElse(null);
            if (caseOut == null || gm.isMulti(caseOut)
                    || MetaFieldGenerator.detectMetaKind(caseOut)
                            != MetaFieldGenerator.MetaKind.NONE) {
                return null;
            }
            JavaType caseOutJt = translator.toJavaReferenceType(unalias(gm.getType(caseOut)));
            if (!(caseOutJt instanceof JavaClass<?> caseOutClass)) {
                return null;
            }
            if (declType == null) {
                declType = caseOutClass;
            } else if (!declType.equals(caseOutClass)) {
                return null;
            }
            String caseVar = ctx.scope().disambiguate(
                    com.regnosys.rosetta.generator.java.function.FunctionDependencyCollector
                            .lowerCamelCase(caseType.getSimpleName()));
            // Bind the narrowed subject to the cast case var (wrapped so an
            // evaluate-arg unwraps to the bare name — the #221/#226 re-root) and
            // compile the case body in a child scope carrying the binding.
            JavaStatementScope caseScope = ctx.scope().bodyScope();
            caseScope.bindSwitchSubject(expr, JavaExpression.wrappedInMapperSOf(
                    JavaExpression.from(caseVar, caseType)));
            JavaStatementBuilder caseBody =
                    compiler.compile(switchCase.expression(), null, caseScope);
            if (!(caseBody instanceof JavaExpression caseBodyExpr)) {
                return null;
            }
            // The assignment value is the ITEM form: a bare implicit invocation
            // compiles UNWRAPPED (renderImplicitFunctionInvocation); a wrapped
            // form strips structurally. Multi-line values decline.
            JavaStatementBuilder caseValueBuilder = caseBodyExpr.unwrapToBuilder()
                    .orElse(caseBodyExpr);
            String caseValue = HandlerHelper.render(caseValueBuilder);
            if (caseValue.contains("\n")) {
                return null;
            }
            refs.add(caseType);
            refs.addAll(caseValueBuilder.getRefs());
            wildcards.addAll(caseValueBuilder.getStaticWildcardImports());
            arms.add(new LadderArm(caseType, caseVar, caseValue));
        }
        if (arms.isEmpty() || declType == null) {
            return null;
        }
        refs.add(declType);
        refs.addAll(subjBuilder instanceof JavaExpression sje
                && sje.unwrapToBuilder().isPresent()
                ? sje.unwrapToBuilder().get().getRefs()
                : subjBuilder.getRefs());
        // The decl type emits as a #227 first-claim-wins sentinel (a same-simple
        // fpml witness elsewhere in the body must not wrongly claim the bare name).
        String declTypeText =
                ImportCollisionResolver.typeRef(declType.getCanonicalName().withDots());
        String placeholder = "__CTOR_SWITCH_LADDER__";
        StringBuilder sb = new StringBuilder();
        sb.append("final ").append(declTypeText).append(' ').append(placeholder).append(";\n");
        sb.append("if (").append(subject).append(" == null) {\n");
        sb.append('\t').append(placeholder).append(" = null;\n");
        sb.append('}');
        for (LadderArm arm : arms) {
            String caseSimple = arm.caseType().getSimpleName();
            sb.append(" else if (").append(subject).append(" instanceof ")
              .append(caseSimple).append(") {\n");
            sb.append('\t').append("final ").append(caseSimple).append(' ')
              .append(arm.caseVar()).append(" = (").append(caseSimple).append(") ")
              .append(subject).append(";\n");
            sb.append('\t').append(placeholder).append(" = ").append(arm.caseValue())
              .append(";\n");
            sb.append('}');
        }
        sb.append(" else {\n");
        sb.append('\t').append(placeholder).append(" = null;\n");
        sb.append('}');
        String sentinel = sink.statementHoistSession()
                .register(StatementHoistSession.IF_THEN_ELSE_RESULT);
        sink.registerStatementHoist(sb.toString().replace(placeholder, sentinel));
        return JavaExpression.selfUnwrapping(
                JavaExpression.from(sentinel, null, refs, wildcards));
    }

    /**
     * Renders a switch-case guard as a Java expression — the first argument of the retired
     * {@code Objects.equals(guard, arg)} comparison until v3.2 seat 12; since then the caller's loop discards
     * the text and the NAME arm's {@code TYPE_SWITCH_TERNARY_STUB} refusal is this method's only effect.
     *
     * <p>The literal's KIND is the front-end's ({@link RSwitchCaseGuard#literalKind()} — the
     * lexer terminal the AST builder recorded beside the value text), never classified from the
     * text:
     * <ul>
     *   <li>INT guards         → boxed, {@code Integer.valueOf(42)} (a primitive could not be passed
     *       to the retired {@code Objects.equals}); the digits are normalised through {@code BigInteger}, so a
     *       leading-zero guard ({@code 007}) does not render an octal Java literal
     *   <li>DECIMAL guards     → boxed, {@code Double.valueOf(3.14)} — the render a grammatical
     *       {@code BIG_DECIMAL} guard always had, now keyed on the kind (no import is needed)
     *   <li>BOOLEAN guards     → the Java literal {@code true}/{@code false} (auto-boxed by javac;
     *       the rosetta keywords are {@code True}/{@code False})
     *   <li>STRING guards      → quoted — a STRING guard {@code "42"} (or {@code "null"}) is a
     *       string, whatever its text
     *   <li>an absent value    → {@code null}
     *   <li>NAME guards        → qualified name (enum constant or type reference)
     * </ul>
     *
     * <p>v3.1 C2d, retirement family 1 ({@code numeric-literal-kind}). This ladder once classified
     * the guard's VALUE TEXT: Long / Float / Double arms ({@code 1L}, {@code 1.0f}, {@code 1.5})
     * — DEAD, deleted as one unit: the grammar cannot produce them as numeric literals
     * ({@code INT_LITERAL : DIGIT+}; {@code BIG_DECIMAL} admits no {@code l/L/f/F} suffix), and the
     * only other channel — a quote-stripped STRING guard, stored in the same {@code literalValue()}
     * as a numeric one — carries none of those shapes anywhere in the corpus (the parse-tree census
     * {@code SwitchGuardLiteralKindCensusTest}: 19,829 guards over 4,137 {@code .rosetta}, 0 numeric
     * literals, 0 long/float/decimal/exponent-shaped strings; a DECIMAL guard has no corpus
     * carrier here and takes the {@code Double.valueOf} render from the kind). The Integer arm's
     * {@code [+-]?\d+} text test then retired onto the kind channel: the family's C2c census
     * (both routes, every cell) found ZERO corpus arrivals at this ternary path at all — every
     * literal guard renders through {@code FunctionExpressionRenderer.renderSwitchGuardMapper}
     * (the {@code areEqual} path, keyed on the switch subject's inferred type) — so the census
     * found none of the 240 int-shaped STRING guards the text test would have boxed arriving
     * here; a kind-less literal guard (a hand-built node) renders as a string. Witnesses:
     * {@code ControlFlowHandlerTest}.
     * v3.2 seat 12 (D52, COUNTERS FIRST - R1): the ternary this method's LITERAL arms feed is NEVER EMITTED any
     * more - the caller's loop runs so the NAME arm's type-keyed refusal below keeps its per-case precedence, then
     * refuses at SWITCH_TERNARY_STUB; the literal forms' text is discarded. {@code ControlFlowHandlerTest}'s fifteen
     * switch tests witness the refusal now (their literal-form pins retired with the render). The LITERAL arms are
     * EXECUTED AND DISCARDED from this seat on - not unreachable: a hand-built INT guard with non-digit text would
     * still meet {@code BigInteger}'s NumberFormatException before the refusal (no grammar path builds one). BANKED as
     * ONE housekeeping deletion, a ledger-gated commit (round 1, cq SF-1 / NIT-3): these arms; the caller's dead
     * assembly in {@code handle(RSwitchExpr)} - the rendered argument, {@code sb}, {@code refs}, {@code wildcards},
     * {@code defaultResult}, {@code first}, built and never read after the loop - with the two evidence-api ledger
     * rows on its {@code refs} propagation ({@code Set<JavaClass<?>> refs = new HashSet<>(argBuilder.getRefs());} and
     * {@code refs.addAll(caseBuilder.getRefs());}, both PROPAGATION / KEPT-BY-CLASS - round 2, cq NIT-5); {@code HandlerHelper.OBJECTS} (unused since R1); and lane L1 of
     * {@code lanes-s12.py}, whose mutation re-inserts {@code refs.add(HandlerHelper.OBJECTS)} and is re-cut with it.
     * BANKED (LAW 69, the sibling half of the same question): the {@code areEqual} renderer
     * {@code FunctionExpressionRenderer.renderSwitchGuardMapper} still classifies a guard's value
     * TEXT for its {@code True}/{@code False} lowering and its int-shape number coercion — a
     * {@code .equals} / digit-scan shape the ledger's nets do not claim, inert on the corpus by
     * the census (every int-shaped STRING guard sits under a {@code string} subject, every
     * boolean under a {@code boolean} one) and the next consumer of {@code literalKind()}.
     */
    private String renderGuard(RSwitchCaseGuard guard) {
        if (guard == null) {
            return "null";
        }
        if (guard.kind() == SwitchGuardKind.LITERAL) {
            if (guard.literalValue().isEmpty()) return "null";
            String value = guard.literalValue().get();
            SwitchGuardLiteralKind literalKind = guard.literalKind().orElse(SwitchGuardLiteralKind.STRING);
            switch (literalKind) {
                case BOOLEAN:
                    // True/False -> true/false; auto-boxed by javac
                    return value.toLowerCase(java.util.Locale.ROOT);
                case INT:
                    // Numeric literals must be explicitly boxed; primitive types can't be
                    // dereferenced and would produce invalid Java in an equals() call. The
                    // digits through BigInteger: INT_LITERAL is DIGIT+, and `007` must not
                    // become the octal Java literal 007.
                    return "Integer.valueOf(" + new java.math.BigInteger(value) + ")";
                case DECIMAL:
                    return "Double.valueOf(" + value + ")";
                case STRING:
                default:
                    return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
            }
        }
        // NAME — qualified enum or type reference (e.g., EnumType.VALUE)
        String qualified = guard.qualifiedName().orElse("null");
        // v3.1 C0 item 1 — THE LOUD REGISTER. Reaching here with a guard that names a
        // TYPE (rather than an enum value) meant, until v3.1 C0, that the type-keyed
        // switch had fallen past every instanceof-ladder renderer — ChoiceSwitchSupport's
        // function-body and in-lambda seats, the ctor-field hoist, the data-rule return
        // ladder — and was about to be rendered as `Objects.equals(<TypeName>, <subject>)`;
        // the NAME arm below refuses since C0 ("C0 CONVERTED THIS TO A REFUSAL", below), and since
        // v3.2 seat 12 the loop's product — still assembled — is discarded at the R1 tail. A type name is
        // not a Java expression: this is the stub ChoiceSwitchSupport's javadoc called "the
        // still-waivered ControlFlowHandler ternary" until v3.2 seat 12 re-cut that phrase to name the
        // residual seat's refusal (root cause E5, whose witness is a switch in an ALIAS body — a seat with no
        // renderer at all). Counted here rather than at the decline sites because this is where the
        // broken bytes were written — until v3.1 C0 for a type name, until v3.2 seat 12 for the rest (the
        // loop's product is discarded and the seat refuses since); a decline that some other renderer
        // then claims is not a degradation.
        //
        // C0 CONVERTED THIS TO A REFUSAL. Measured across the 25x2 matrix: 29 hits on band
        // cells, ZERO on any clean cell — nothing green renders a type name into a
        // ternary, because doing so has never produced compiling Java. The element refuses
        // by name instead; C3b retires the site by giving the alias seat the
        // instanceof-ladder renderer the function-body and in-lambda seats already have.
        if (guardNamesAType(guard)) {
            throw SilentDegradation.refuse(SilentDegradation.Site.TYPE_SWITCH_TERNARY_STUB,
                    "type-keyed switch case '" + qualified + "' at a seat with no instanceof-ladder"
                            + " renderer (the ternary would compare against a type name)",
                    guard);
        }
        return qualified;
    }

    /**
     * v3.2 seat 7 round 2: a LITERAL guard on any case AND a subject the nearest enclosing inline function binds -
     * the implicit item (an elided or explicit {@code item}) or one of the function's closure parameters by name.
     * The AST alone decides it (no rendered text). "Any case" means a MIXED guard set (a literal guard beside a
     * NAME guard, the seat-2 banked shape) over such a subject is claimed here, before {@code renderGuard}'s
     * type-keyed refusal the name guard alone would reach - the register reads 0 at the site on every cell and both
     * routes at the chain of record (round 3, the code-quality review's NIT-3). The closure-parameter leg is BROADER
     * than the literal-guard block's admission (the block admits the FIRST parameter alone): a first-parameter
     * subject renders the block and never reaches this seat, any other parameter is declined there and refused here.
     */
    private static boolean literalGuardedOverInlineFunctionSubject(RSwitchExpr sw) {
        boolean literal = false;
        for (RSwitchCase sc : sw.cases()) {
            RSwitchCaseGuard g = sc.guard().orElse(null);
            if (g != null && g.kind() == SwitchGuardKind.LITERAL) {
                literal = true;
            }
        }
        if (!literal) {
            return false;
        }
        com.regnosys.rosetta.ast.expressions.supporting.RInlineFunction owner =
                ReferenceHandler.nearestEnclosingInlineFunction(sw);
        if (owner == null) {
            return false;
        }
        RExpression subject = sw.argument();
        if (subject == null
                || subject instanceof com.regnosys.rosetta.ast.expressions.references.RImplicitVariable) {
            return true;
        }
        return subject instanceof RSymbolReference ref && ref.args().isEmpty()
                && owner.paramNames().contains(ref.name());
    }

    /**
     * Whether a NAME guard names a TYPE rather than an enum value — the shape whose
     * ternary render is invalid Java (see {@link #renderGuard}). Reads the guard's
     * RESOLVED node, so an unresolved guard is not counted here: that is the resolver's
     * own failure ({@link SilentDegradation.Site#UNRESOLVED_SYMBOL_ECHO}), and counting it
     * twice would double-bill one defect across two sites.
     */
    private static boolean guardNamesAType(RSwitchCaseGuard guard) {
        // Resolvers bind a guard to an REnumValue for an enum-keyed switch (renders as a
        // real Java constant) and to an RDataType / RChoice / choice-option node for a
        // type-keyed one (renders as a type name — not an expression).
        Object resolved = guard.resolvedGuard().orElse(null);
        return resolved != null && !(resolved instanceof REnumValue);
    }

}
