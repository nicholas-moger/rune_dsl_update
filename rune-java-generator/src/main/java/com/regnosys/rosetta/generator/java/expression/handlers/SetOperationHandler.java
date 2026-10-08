package com.regnosys.rosetta.generator.java.expression.handlers;

import com.regnosys.rosetta.generator.java.SilentDegradation;
import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.enums.ListOp;
import com.regnosys.rosetta.ast.expressions.binary.RContainsExpr;
import com.regnosys.rosetta.ast.expressions.constructors.RConditionalExpr;
import com.regnosys.rosetta.ast.expressions.binary.RDefaultExpr;
import com.regnosys.rosetta.ast.expressions.literals.RListLiteral;
import com.regnosys.rosetta.ast.expressions.binary.RDisjointExpr;
import com.regnosys.rosetta.ast.expressions.binary.RJoinExpr;
import com.regnosys.rosetta.ast.expressions.supporting.RInlineFunction;
import com.regnosys.rosetta.ast.expressions.supporting.RKeyValuePair;
import com.regnosys.rosetta.ast.expressions.binary.RThenExpr;
import com.regnosys.rosetta.ast.expressions.literals.RIntLiteral;
import com.regnosys.rosetta.ast.expressions.references.REnumValueRef;
import com.regnosys.rosetta.ast.expressions.references.RFeatureCall;
import com.regnosys.rosetta.ast.expressions.references.RSymbolReference;
import com.regnosys.rosetta.ast.expressions.unary.RExtractExpr;
import com.regnosys.rosetta.ast.expressions.unary.RListOpExpr;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.functions.RShortcut;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.supporting.REnumValue;
import com.regnosys.rosetta.ast.types.REnumeration;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.enums.EnumHelper;
import com.regnosys.rosetta.generator.java.function.FunctionAliasHelper;
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
import com.regnosys.rosetta.types.RMissingType;
import com.regnosys.rosetta.types.RNumberType;
import com.regnosys.rosetta.types.RType;
import com.rosetta.util.types.JavaClass;
import com.rosetta.util.types.JavaType;

import java.util.HashSet;
import java.util.Set;

/**
 * Handles code generation for set-operation binary expressions:
 * contains, disjoint, default, and join.
 *
 * <p>Golden output patterns (verified against CDM golden files):
 * <pre>
 *   a contains b      →  contains(left, right)           — static call
 *   a disjoint b      →  disjoint(left, right)           — static call
 *   a default b       →  left.getOrDefault(right)        — instance call
 *   a join            →  left.join()                     — no separator
 *   a join separator  →  left.join(separator)            — with separator
 * </pre>
 *
 * <p>Static calls ({@code contains}, {@code disjoint}) are from
 * {@code com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe} via
 * {@code import static ExpressionOperatorsNullSafe.*;}.
 *
 * <p>Note: {@link RJoinExpr} stores its separator in {@code separator()} (an
 * {@code Optional}), not in {@code rawRight()} which is always null for join
 * expressions.
 */
public class SetOperationHandler {

    /**
     * Compiles an {@link RContainsExpr} into a static {@code contains()} call.
     *
     * @param expr     the contains expression node
     * @param ctx      the current expression compilation context
     * @param compiler the parent compiler for recursive compilation of operands
     * @return a {@link JavaExpression} rendering {@code contains(left, right)}
     */
    public JavaStatementBuilder handle(RContainsExpr expr, ExpressionContext ctx, ExpressionCompiler compiler) {
        // PR-A §9.1 C3a.4.k: `contains` is a public static method on
        // ExpressionOperatorsNullSafe (L273 per 2026-04-21 source verification).
        // FunctionGenerator L844-849 trigger ladder does NOT include `contains(`
        // — same latent-bug class as D9 λ (`choice`). Declare
        // EXPRESSION_OPERATORS_NULL_SAFE staticWildcard structurally so C3c.2's
        // regex delete is regression-proof.
        JavaStatementBuilder leftBuilder =
                compiler.compile(expr.rawLeft(),  ctx.expectedType(), ctx.scope());
        JavaStatementBuilder rightBuilder =
                compiler.compile(expr.rawRight(), ctx.expectedType(), ctx.scope());
        // facet containsOperandCardinalityWrap (PR #326, F1b): a CLOSURE-PARAM operand
        // (`extract ec [ GetAcceptedEicCodes contains ec ]`) is ALREADY a Mapper at
        // runtime — the variable path renders every bare name wrappedInMapperSOf, so the
        // fork emitted the double-wrapped `MapperS.of(ec)` (a Mapper-of-Mapper `contains`
        // never byte-matched golden's bare `ec`). Structurally unwrap the wrap (the
        // atomic MAPPER_S ref drop rides the wrappedInMapperSOf factory contract),
        // gated exactly like the #180 evaluate-arg closure-param collapse: a bare
        // no-args symbol whose name an enclosing NON-then lambda declares (a
        // then-declared param binds the whole piped list — declined, no carrier).
        leftBuilder  = unwrapClosureParamOperand(leftBuilder, expr.rawLeft());
        rightBuilder = unwrapClosureParamOperand(rightBuilder, expr.rawRight());
        // facet containsClosureParamMetaDeref (PR #334): a META-element closure-param
        // operand joined against a provably meta-FREE sibling derefs to the bare value
        // — upstream compiles both `contains` operands against
        // MAPPER.wrapExtends(joinMetaAnnotatedTypes(l, r)) (ExpressionGenerator
        // binaryExpr), and a meta side joined with a bare side joins BARE, so golden
        // appends the guarded `.<NaturalPersonRoleEnum>map("Type coercion", w -> w ==
        // null ? null : w.getValue())` on the param (NaturalPersonBuyerOrSeller's
        // `extract r [ PartyIdentifierNaturalPersonRoles contains r ]`). The element
        // meta is recovered from the param's OWNER extract's argument terminal (the
        // SAME metaNavResultType the receiver-coercion arms read); an all-meta or
        // unprovable pairing declines (byte-flat).
        leftBuilder  = derefMetaClosureParamOperand(leftBuilder, expr.rawLeft(), expr.rawRight(), ctx, compiler);
        rightBuilder = derefMetaClosureParamOperand(rightBuilder, expr.rawRight(), expr.rawLeft(), ctx, compiler);
        // Coverage wave D (datarule): a NAV-CHAIN operand whose COMPILED type is a
        // META wrapper, joined against a bare enum-constant sibling, derefs to the
        // bare value — the same joinMetaAnnotatedTypes bare-join law as the #334
        // closure-param arm, at the chain-operand seat (golden Trade*Mortgages:
        // `…getContractualTermsSupplementType()).<ContractualSupplementTypeEnum>map(
        // "Type coercion", fieldWithMetaContractualSupplementTypeEnum ->
        // fieldWithMetaContractualSupplementTypeEnum.getValue())` against
        // `MapperS.of(ContractualSupplementTypeEnum.CD_SON_MBS)`). The deref is the
        // existing coerceNavigationReceiver emission (wrap-kind-aware: the MapperC
        // chain takes the bare elementwise form). Gated to TYPE-CONDITION
        // expressions (the datarule compile context — null for every
        // function/rule-path expression by construction).
        leftBuilder  = derefMetaChainOperandAgainstBareEnum(leftBuilder, expr.rawRight(), ctx, compiler);
        rightBuilder = derefMetaChainOperandAgainstBareEnum(rightBuilder, expr.rawLeft(), ctx, compiler);
        String left  = HandlerHelper.render(leftBuilder);
        String right = HandlerHelper.render(rightBuilder);

        Set<JavaClass<?>> refs = new HashSet<>();
        refs.addAll(leftBuilder.getRefs());
        refs.addAll(rightBuilder.getRefs());

        // facet void_witness_bare_enum (arm D4): an enum-constant operand renders
        // as the BARE dotted constant (ReferenceHandler's enum branches rely on the
        // consumer to wrap); `contains` takes Mapper operands, so the bare form is
        // non-compiling. Apply the SAME MapperS.of wrap the equality/ordering
        // comparison consumers use (HandlerHelper.wrapEnumOperand), matching golden
        // `contains(<chain>, MapperS.of(PartyRoleEnum.CLEARING_ORGANIZATION))`.
        left  = HandlerHelper.wrapEnumOperand(leftBuilder, refs);
        right = HandlerHelper.wrapEnumOperand(rightBuilder, refs);

        // facet containsOperandCardinalityWrap (PR #326, F1a): a bare no-args FUNCTION
        // invocation operand (`GetAcceptedEicCodes contains ec`) renders UNWRAPPED
        // (renderImplicitFunctionInvocation emits the bare `<fn>.evaluate(...)`, a bare
        // List where `contains` wants a Mapper — non-compiling, already waivered). Apply
        // the #298/#301/#302 cardinality-aware wrap at the contains-operand seat: a
        // MULTI-output callee wraps `MapperC.<X>of(...)` (golden IsAcceptedEicCode), a
        // single-output one `MapperS.of(...)` — the shared bareMultiOutputWitness SOT.
        left  = HandlerHelper.wrapBareInvocationOperand(left, expr.rawLeft(), refs, compiler);
        right = HandlerHelper.wrapBareInvocationOperand(right, expr.rawRight(), refs, compiler);

        Set<JavaClass<?>> wildcards = new HashSet<>();
        wildcards.add(HandlerHelper.EXPRESSION_OPERATORS_NULL_SAFE);
        wildcards.addAll(leftBuilder.getStaticWildcardImports());
        wildcards.addAll(rightBuilder.getStaticWildcardImports());

        return JavaExpression.from(
                "contains(" + left + ", " + right + ")",
                null,
                refs,
                wildcards);
    }

    /**
     * Coverage wave D (datarule): the chain-operand meta deref against a bare
     * enum-constant sibling — see the {@code handle(RContainsExpr)} call-site
     * comment. Fires only when the operand's COMPILED item type is a
     * {@link com.regnosys.rosetta.generator.java.types.RJavaWithMetaValue}
     * wrapper (an already-deref'd operand carries the VALUE type — no
     * double-deref), the sibling is a resolved bare enum constant
     * ({@link com.regnosys.rosetta.ast.expressions.references.REnumValueRef}
     * with a present enumeration), and the expression sits inside a DATA-TYPE
     * condition.
     */
    private static JavaStatementBuilder derefMetaChainOperandAgainstBareEnum(
            JavaStatementBuilder builder, RExpression rawSibling,
            ExpressionContext ctx, ExpressionCompiler compiler) {
        if (!(builder instanceof JavaExpression je) || ctx.scope() == null
                || compiler == null || compiler.getTypeUtil() == null) {
            return builder;
        }
        var tu = compiler.getTypeUtil();
        var t = je.getExpressionType();
        if (t == null || !tu.isWrapper(t)
                || !(tu.getItemType(t)
                        instanceof com.regnosys.rosetta.generator.java.types.RJavaWithMetaValue)) {
            return builder;
        }
        if (!(rawSibling instanceof com.regnosys.rosetta.ast.expressions.references.REnumValueRef evr)
                || evr.enumeration().isEmpty()) {
            return builder;
        }
        if (HandlerHelper.findEnclosingTypeCondition(evr) == null) {
            return builder;
        }
        return compiler.coerceNavigationReceiver(builder, ctx.scope());
    }

    /**
     * facet containsOperandCardinalityWrap (PR #326, F1b): structurally unwrap a
     * {@code MapperS.of(<param>)} operand whose raw expression is a bare no-args
     * reference to an enclosing NON-then lambda's declared closure param — the param
     * is already Mapper-typed at runtime, so golden passes it bare ({@code ec}) while
     * the fork's variable path double-wrapped it. The structural
     * {@code unwrapToBuilder} contract carries the atomic {@code MAPPER_S} ref drop
     * (the #179 lesson). A then-declared param declines (the one closure-param shape
     * whose runtime value can be a {@code MapperC} — mirrors the #180 evaluate-arg
     * gate); any non-symbol / chained / non-param operand returns unchanged.
     */
    private static JavaStatementBuilder unwrapClosureParamOperand(JavaStatementBuilder builder,
            RExpression rawOperand) {
        if (!(rawOperand instanceof RSymbolReference ref) || !ref.args().isEmpty()) {
            return builder;
        }
        if (!(builder instanceof JavaExpression expr) || expr.unwrapToBuilder().isEmpty()) {
            return builder;
        }
        JavaStatementBuilder inner = expr.unwrapToBuilder().get();
        if (!(inner instanceof JavaExpression innerExpr)
                || !innerExpr.renderToString().equals(ref.name())) {
            return builder;
        }
        RInlineFunction owner = ReferenceHandler.enclosingClosureParamOwner(ref, ref.name());
        if (owner == null || owner.parent() instanceof RThenExpr) {
            return builder;
        }
        return inner;
    }

    /**
     * facet containsClosureParamMetaDeref (PR #334): append the guarded meta-value
     * deref to a META-element closure-param {@code contains} operand whose SIBLING
     * operand is provably meta-free. Fires only when EVERY link resolves:
     * <ol>
     *   <li>the operand is a bare no-args symbol rendering exactly its own name (the
     *       post-{@link #unwrapClosureParamOperand} closure-param shape);</li>
     *   <li>its owner is a NON-then lambda whose parent is an {@link RExtractExpr}
     *       (the `extract r [ … ]` element-wise owner — the ONE shape whose param is
     *       a single element by construction);</li>
     *   <li>the extract's argument terminal resolves to a META attribute
     *       ({@code NavigationHandler.metaNavResultType}'s own gate — a non-meta
     *       element recovers null, byte-flat);</li>
     *   <li>the sibling operand is provably meta-FREE: a bare no-args FUNCTION
     *       invocation whose output attribute has {@code detectMetaKind == NONE}
     *       (the join law then strips the meta side — upstream
     *       {@code joinMetaAnnotatedTypes} keeps meta only when BOTH sides carry
     *       it).</li>
     * </ol>
     * The deref itself is the existing {@code coerceNavigationReceiver} emission
     * (guarded MapperS form, deferred param naming). Green-safe by construction:
     * the pre-fix render passed the raw wrapper param where the join wants the bare
     * value (non-compiling), so no green file carries it.
     */
    private static JavaStatementBuilder derefMetaClosureParamOperand(JavaStatementBuilder builder,
            RExpression rawOperand, RExpression rawOther,
            ExpressionContext ctx, ExpressionCompiler compiler) {
        if (compiler.getCoercionService() == null || compiler.getTypeUtil() == null) {
            return builder;
        }
        if (!(rawOperand instanceof RSymbolReference ref) || !ref.args().isEmpty()) {
            return builder;
        }
        if (!(builder instanceof JavaExpression paramExpr)
                || !paramExpr.renderToString().equals(ref.name())) {
            return builder;
        }
        RInlineFunction owner = ReferenceHandler.enclosingClosureParamOwner(ref, ref.name());
        if (owner == null || owner.parent() instanceof RThenExpr
                || !(owner.parent() instanceof RExtractExpr ownerExtract)) {
            return builder;
        }
        // The sibling must be provably meta-free (a bare fn invocation with a
        // meta-free output) — otherwise the join could keep the wrapper.
        if (!(rawOther instanceof RSymbolReference otherRef) || !otherRef.args().isEmpty()) {
            return builder;
        }
        RFunction otherFn = otherRef.symbol()
                .filter(RFunction.class::isInstance).map(RFunction.class::cast).orElse(null);
        RAttribute otherOut = otherFn == null ? null : otherFn.output().orElse(null);
        if (otherOut == null
                || MetaFieldGenerator.detectMetaKind(otherOut) != MetaFieldGenerator.MetaKind.NONE) {
            return builder;
        }
        // Element meta from the owner extract's ARGUMENT terminal.
        RExpression ownerArg = ownerExtract.argument();
        RAttribute terminal = null;
        if (ownerArg instanceof RFeatureCall fc) {
            terminal = fc.resolvedFeature().orElse(null);
        } else if (ownerArg instanceof RSymbolReference sr) {
            terminal = sr.symbol().filter(RAttribute.class::isInstance)
                    .map(RAttribute.class::cast).orElse(null);
        } else if (ownerArg instanceof REnumValueRef evr
                && evr.enumeration().isEmpty()
                && evr.resolvedAttributeChain().isPresent()) {
            terminal = evr.resolvedAttributeChain().get().feature();
        }
        if (terminal == null) {
            return builder;
        }
        var throwawayRefs = new HashSet<JavaClass<?>>();
        var metaType = NavigationHandler.metaNavResultType(terminal, compiler, throwawayRefs, false);
        if (metaType == null) {
            return builder;
        }
        return compiler.coerceNavigationReceiver(
                NavigationHandler.retypeBuilder(builder, metaType), ctx.scope());
    }

    /**
     * Compiles an {@link RDisjointExpr} into a static {@code disjoint()} call.
     *
     * @param expr     the disjoint expression node
     * @param ctx      the current expression compilation context
     * @param compiler the parent compiler for recursive compilation of operands
     * @return a {@link JavaExpression} rendering {@code disjoint(left, right)}
     */
    public JavaStatementBuilder handle(RDisjointExpr expr, ExpressionContext ctx, ExpressionCompiler compiler) {
        // PR-A §9.1 C3a.4.k: same D9-λ-style fix as contains —
        // `disjoint` is public static on ExpressionOperatorsNullSafe (L291),
        // not in FunctionGenerator L844-849 trigger ladder.
        JavaStatementBuilder leftBuilder =
                compiler.compile(expr.rawLeft(),  ctx.expectedType(), ctx.scope());
        JavaStatementBuilder rightBuilder =
                compiler.compile(expr.rawRight(), ctx.expectedType(), ctx.scope());
        String left  = HandlerHelper.render(leftBuilder);
        String right = HandlerHelper.render(rightBuilder);

        Set<JavaClass<?>> refs = new HashSet<>();
        refs.addAll(leftBuilder.getRefs());
        refs.addAll(rightBuilder.getRefs());

        // facet void_witness_bare_enum (arm D4): same enum-constant operand wrap
        // as `contains` — upstream compiles BOTH set-operation operands against
        // MAPPER.wrapExtends(joined), so the constant coerces to MapperS.of(E.V).
        left  = HandlerHelper.wrapEnumOperand(leftBuilder, refs);
        right = HandlerHelper.wrapEnumOperand(rightBuilder, refs);

        Set<JavaClass<?>> wildcards = new HashSet<>();
        wildcards.add(HandlerHelper.EXPRESSION_OPERATORS_NULL_SAFE);
        wildcards.addAll(leftBuilder.getStaticWildcardImports());
        wildcards.addAll(rightBuilder.getStaticWildcardImports());

        return JavaExpression.from(
                "disjoint(" + left + ", " + right + ")",
                null,
                refs,
                wildcards);
    }

    /**
     * Compiles an {@link RDefaultExpr} into a {@code getOrDefault()} instance call
     * on the left operand.
     *
     * @param expr     the default expression node
     * @param ctx      the current expression compilation context
     * @param compiler the parent compiler for recursive compilation of operands
     * @return a {@link JavaExpression} rendering {@code left.getOrDefault(right)}
     */
    public JavaStatementBuilder handle(RDefaultExpr expr, ExpressionContext ctx, ExpressionCompiler compiler) {
        // .getOrDefault is an instance method — operand refs flow only.
        // facet getOrDefaultMetaJoin (PR #296) — the #295 conditional-meta-type-join at the
        // `default` seat. A MIXED-baresym `default`: the LEFT (defaulted) operand is a disguised
        // baresym function-nav (an REnumValueRef whose resolvedSymbol is an RFunction) whose leaf is
        // META-annotated, the RIGHT (default) a baresym BARE-leaf nav (which forces the bare common
        // type). #280 DECLINED the meta-leaf LEFT, leaving the non-compiling bare type literal
        // `MapperS.of(<Type>)`; golden fires the baresym invoke + derefs the wrapper
        // `.<X>map("Type coercion", w -> w == null ? null : w.getValue())` so the operand joins on
        // the bare value. Resolved via the #295 foundation: push the scope flag (un-declines the
        // #280 meta-leaf gate) WHILE compiling the LEFT so the wrapper fires, then deref it to the
        // bare join via coerceNavigationReceiver. Confined to this seat (the conditional-ladder seat
        // is CollectionHandler.compileLadderConditionalBlock); a HOMOGENEOUS-meta default (both
        // operands meta), a bare LEFT, or a non-baresym operand keeps the #280 decline →
        // byte-unchanged. Green-safe by the #280 argument: the pre-fix bare type literal never
        // compiled, so only an already-waivered file is touched. Rule-body seat; FUNCTION tail
        // byte-unaffected.
        // facet implicit_operand_synthesis (PR #218): a `then default <v>` bare default
        // elides its left operand — the piped value — so rawLeft() is null; substitute
        // the synthetic implicit (→ the bound thenArg).
        boolean metaJoin = isMixedBaresymDefault(expr);
        // facet metaWrapperRecovery (v3.1 C2d retirement family 6, PR #613): whether one of this
        // seat's THREE LEFT-deref arms actually fired, and the wrapper VALUE type the LEFT carried
        // before that deref — the facts the `.getValue()).getOrDefault(` marker was read for.
        boolean leftDerefFired = false;
        JavaClass<?> preDerefLeftValue = null;
        JavaStatementBuilder leftBuilder;
        if (metaJoin) {
            ctx.scope().pushMetaLeafBaresymAllowed();
            try {
                leftBuilder = compiler.compile(
                        HandlerHelper.orSyntheticImplicit(expr.rawLeft(), expr), ctx.expectedType(), ctx.scope());
                // Deref the fired meta wrapper to the bare join (no-op if no wrapper surfaced).
                // facet metaWrapperRecovery (v3.1 C2d retirement family 6, PR #613): identity
                // records whether the deref actually fired (coerceNavigationReceiver returns the
                // receiver itself when it does nothing).
                JavaClass<?> preCoerceValue = leftWrapperValueTypeOrNull(leftBuilder, compiler);
                JavaStatementBuilder preCoerceLeft = leftBuilder;
                leftBuilder = compiler.coerceNavigationReceiver(leftBuilder, ctx.scope());
                if (leftBuilder != preCoerceLeft) {
                    leftDerefFired = true;
                    preDerefLeftValue = preCoerceValue;
                }
            } finally {
                ctx.scope().popMetaLeafBaresymAllowed();
            }
        } else {
            leftBuilder = compiler.compile(
                    HandlerHelper.orSyntheticImplicit(expr.rawLeft(), expr), ctx.expectedType(), ctx.scope());
        }
        // facet metaWrapperRecovery (v3.1 C2d retirement family 6, PR #613): the LEFT that arrived
        // ALREADY deref'd from its own compile — the four fn:QuantityUnitOfMeasure k=4 marker
        // fires per D11 walk where NONE of the three arms above fired — is deliberately NOT
        // captured (the seat's code-quality review, SF-6 — an earlier ledger-window read here
        // never fired and was deleted as unreached): at that class the `default`'s left is
        // ELIDED, so orSyntheticImplicit compiles to the already-deref'd bound thenArg token and
        // the deref rendered at a PREVIOUS rung, outside any window this seat can hold. The
        // record below therefore says false where the marker said true at exactly those four
        // fires — byte-neutral (census § 3d: the consuming composite returns null there either
        // way, and the byte oracle at the swap commit is 275/0F on both routes).
        // facet getOrDefaultCondEnumArgHoist (PR #378, B): a CONDITIONAL default ARG on a
        // to-enum LEFT hoists BEFORE the statement as upstream's raw-typed conditional
        // local and the arg collapses to the bare local — runs BEFORE the right compile
        // (the ternary never compiles; no discarded-attempt registrations, and the
        // ternary's MapperC.of() empty-arm import never collects — golden drops it).
        JavaExpression condEnumArgHoist =
                tryGetOrDefaultCondEnumArgHoist(expr, ctx, compiler, leftBuilder);
        if (condEnumArgHoist != null) {
            return condEnumArgHoist;
        }
        JavaStatementBuilder rightBuilder =
                compiler.compile(expr.rawRight(), ctx.expectedType(), ctx.scope());
        // facet defaultMetaJoinFromRender (seat 28, law 5): upstream compiles BOTH
        // `default` operands at the JOINED meta type (ExpressionGenerator.xtend
        // L452-458, binaryExpr case "default": joinMetaAnnotatedTypes +
        // withExpected(MAPPER.wrapExtends(joined))), and meta survives the join only
        // when BOTH operands carry it. A META LEFT over a meta-FREE RIGHT therefore
        // derefs IN PLACE: golden drr 7.x FixingDateRule
        // `item.<FieldWithMetaDate>map("getAdjustedDate", ...).<Date>map("Type coercion",
        // fieldWithMetaDate -> fieldWithMetaDate == null ? null :
        // fieldWithMetaDate.getValue()).getOrDefault(item.<Date>map("getUnadjustedDate",
        // ...).get())` against the fork's un-deref'd left - which passes a Date to
        // MapperS<FieldWithMetaDate>.getOrDefault(T) and never compiled (LAW 74), so
        // every carrier is an already-waivered mismatch.
        //
        // THE JOIN IS READ FROM RENDER TRUTH, NOT FROM THE INFERRED TYPE (the census
        // producer, REFUTED by PROBE28-F10b): leftInferredMeta=false at EVERY carrier
        // row while the COMPILED left IS a FieldWithMeta... wrapper, rightCompiled=(null)
        // at every row, and expected=null - so joinMetaAnnotatedTypes over the inferred
        // types can never see the meta and a type-driven join has only one side.
        // leftBuilder.getExpressionType() is the channel that IS populated, and
        // coerceNavigationReceiver re-checks BOTH its null and its wrapper conditions
        // itself, so a mis-gate here is byte-inert.
        //
        // The RIGHT-is-meta-free term is LOAD-BEARING, not belt: a HOMOGENEOUS-meta
        // default keeps the wrapper (golden drr MessageID, 9 GREEN cells, both operands
        // FieldWithMetaString, no hop; golden GetBasketConstituents:173 the same class),
        // so a left-only gate would break 9 green files immediately. The right question is
        // answered in two channels (see rightRendersNoMetaWrapper): the COMPILED stamp first
        // - definitive when populated, and the channel that holds the fn:Price filter shape
        // out - then the shared meta WALKER (NavigationHandler.recoverExprMetaWrapper) on the
        // right's AST. The seat-28 refs-superset read was RETIRED at seat 32 law D.1: its
        // false negative (an incidental interior wrapper an arm derefs itself) was NOT
        // byte-flat - it banded UnderlyingIndexIndicatorRule x drr 7.0-7.3.
        //
        // !metaJoin: the #296 branch already coerced this left; re-coercing would
        // double-deref. (After that coerce the stamp is Mapper<? extends Value>, so the
        // wrapper term declines on its own - the gate is self-protecting - but the
        // explicit term states the ownership. Golden CollateralPortfolioIndicatorRule
        // (iosco cde, 9 GREEN cells) already carries this exact form through the #296
        // arm and must stay byte-identical.)
        if (!metaJoin
                && leftRendersMetaWrapper(leftBuilder, compiler)
                && rightRendersNoMetaWrapper(expr.rawRight(), rightBuilder, compiler)) {
            // facet metaWrapperRecovery (v3.1 C2d retirement family 6, PR #613): identity records
            // whether this deref actually fired.
            JavaClass<?> preCoerceValue = leftWrapperValueTypeOrNull(leftBuilder, compiler);
            JavaStatementBuilder preCoerceLeft = leftBuilder;
            leftBuilder = compiler.coerceNavigationReceiver(leftBuilder, ctx.scope());
            if (leftBuilder != preCoerceLeft) {
                leftDerefFired = true;
                preDerefLeftValue = preCoerceValue;
            }
        } else if (!metaJoin) {
            // facet heteroMetaDefaultJoinDeref (v3.1 flip seat 33, law C.2, rung R3a): the THIRD
            // arm of upstream's own join question. The seat-28 arm above answers "a META left over
            // a meta-FREE right derefs the LEFT"; this one answers the case upstream's
            // joinMetaAnnotatedTypes also joins to the bare value - BOTH operands carry meta, but
            // the two annotations DIFFER, so no meta survives the join and BOTH sides deref.
            // Golden drr 7.0-7.3 Price rung 1: `<L>.<PriceSchedule>map("Type coercion",
            // referenceWithMetaPriceSchedule -> referenceWithMetaPriceSchedule.getValue())
            // .getMulti().isEmpty() ? <R>.<PriceSchedule>map("Type coercion",
            // fieldWithMetaPriceSchedule -> fieldWithMetaPriceSchedule.getValue()) : <L>...`
            // against the fork's un-deref'd `<L>.getOrDefault(<R>)`, which hands a
            // MapperC<FieldWithMetaPriceSchedule> to
            // MapperC<ReferenceWithMetaPriceSchedule>.getOrDefault(T) and never compiled.
            // THE COMPILED-STAMP-FIRST ORDER IS UNCHANGED (an `else if`): this arm is reached
            // only when rightRendersNoMetaWrapper has already declined, which is precisely the
            // channel that holds the fn:Price shape out - its javadoc says so by name.
            // MEASURED (LAW 75, [P33-DEFJOIN], BOTH routes, at fa49da010):
            // `sameWrap=false vtEq=true` is 12 rows / 2 `where=` - fn:QuantityUnitOfMeasure 8 and
            // fn:Price 4 - and ZERO of them is green. The `sameWrap` conjunct is the protection,
            // and the GREEN set it protects is measured too (`sameWrap=true --group where`):
            // fn:QuantityUnitOfMeasure 32, rule:QuantitySchedule 20, fn:MessageID 9,
            // fn:GetBasketConstituents 4 - the HOMOGENEOUS-meta default that KEEPS its wrapper,
            // the case the seat-28 javadoc names as its DO-NOT-BREAK. DISCLOSED, as measured at
            // this law's head: QUOM's 8 rows moved here too then (LAW 80 IMPROVED-not-whole).
            // Law B.24, one commit later, gives a THEN-chain right to the then-bound collapse
            // seat (heteroMetaJoinDerefOrNull returns null on `rightExpr instanceof
            // RThenExpr`), so at the seat's final head this arm's population is Price's four
            // rows alone - the m-lawC2-hetero lane names Price only.
            // LAW 69 twice over: "do these wrappers denote the same type?" is
            // NavigationHandler.sameWrapperDenotation (canonical-name, never simple-name - a
            // workspace routinely carries several types sharing a simple name), and the deref
            // STRING is renderCoercedArm, this file's own emitter of the unguarded bare
            // elementwise form. It is NOT coerceNavigationReceiver: that mints the GUARDED
            // NUMBERED form (`... == null ? null : ....getValue()`), which is golden's SINGLE
            // (map-terminal) shape; the measured carrier's operands are both MULTI - a fact
            // about today's carrier, NOT a conjunct (heteroMetaJoinDerefOrNull tests no
            // cardinality); the SINGLE-wrapper case at the then-bound seat is law B.24's.
            JavaStatementBuilder[] heteroDeref =
                    heteroMetaJoinDerefOrNull(expr.rawRight(), leftBuilder, rightBuilder, compiler);
            if (heteroDeref != null) {
                // facet metaWrapperRecovery (v3.1 C2d retirement family 6, PR #613): identity
                // records whether the hetero arm actually re-built the LEFT.
                if (heteroDeref[0] != leftBuilder) {
                    leftDerefFired = true;
                    preDerefLeftValue = leftWrapperValueTypeOrNull(leftBuilder, compiler);
                }
                leftBuilder = heteroDeref[0];
                rightBuilder = heteroDeref[1];
            }
        }
        String left  = HandlerHelper.render(leftBuilder);
        // facet metaWrapperRecovery (v3.1 C2d retirement family 6, PR #613): the `default` seat's
        // own verdict, recorded beside the text it rendered (LAW 69) and BEFORE the seat's many
        // return forms so every form is covered — for the THREE LEFT-deref arms above (the
        // already-deref'd-LEFT class is disclosed above as NOT captured). The LEFT's BARE value
        // type: the coerced builder's own item when that is a plain class, else the wrapper value
        // read beside the arm that fired.
        if (leftDerefFired) {
            JavaClass<?> bareLeftValue = preDerefLeftValue;
            JavaTypeUtil recTu = compiler.getTypeUtil();
            if (recTu != null && leftBuilder.getExpressionType() != null) {
                JavaType coercedItem = recTu.getItemType(leftBuilder.getExpressionType());
                if (coercedItem instanceof JavaClass<?> coercedClass
                        && !(coercedItem instanceof RJavaWithMetaValue)) {
                    bareLeftValue = coercedClass;
                }
            }
            compiler.recordLambdaJoinFacts(expr,
                    new ExpressionCompiler.LambdaJoinFacts(null, false, false, bareLeftValue));
        }

        Set<JavaClass<?>> refs = new HashSet<>();
        refs.addAll(leftBuilder.getRefs());
        refs.addAll(rightBuilder.getRefs());
        Set<JavaClass<?>> wildcards = new HashSet<>();
        wildcards.addAll(leftBuilder.getStaticWildcardImports());
        wildcards.addAll(rightBuilder.getStaticWildcardImports());

        // facet defaultLeftGetRewrap (PR #332): the getOrDefault RECEIVER is a
        // Mapper-consuming seat — a LEFT that rendered as a transparent
        // .get()-collapsed item (the only-element selfUnwrapping collapse, e.g.
        // `distinct(thenArg1).get()`) re-wraps `MapperS.of(...)` exactly like the
        // #243 exists/comparison operands: upstream compiles the default LEFT
        // against the Mapper expected type and the MapperC→MapperS coercion
        // re-wraps the collapsed item (golden GetOrFetchLeiData/MicData
        // `MapperS.of(distinct(thenArg1).get()).getOrDefault(true)`). Green-safe
        // by the #243 argument: a raw item with `.getOrDefault(` chained on it
        // does not compile, and the transparency gate never double-wraps.
        left = HandlerHelper.wrapSelfUnwrappingGetOperand(left, leftBuilder, refs);

        // facet multiDefaultTernary (PR #345): a MULTI-cardinality `default` inside an
        // ALIAS body — both operands MapperC — renders upstream's list-form ternary
        // `<A>.getMulti().isEmpty() ? <B> : <A>` (the LEFT repeated; golden
        // MapCreditIndex `indexId` cdm6: `return MapperS.of(…).<IndexId>mapC(…)
        // .getMulti().isEmpty() ? <B-chain> : <A-chain>;`). The fork's
        // `<mcA>.getOrDefault(<mcB>)` passed a MapperC where the T-typed overload
        // expects the ITEM — non-compiling → green-safe by construction (zero goldens
        // call getOrDefault with a Mapper arg). ALIAS-scoped (findEnclosingShortcut):
        // the statement-seat MULTI default (FilterChangePriceQuantity's whole-add
        // if/else restructure) is a DIFFERENT golden form and stays declined on the
        // legacy render; both operands must PROVE multi (render-truth, monotone) so
        // any unproven default keeps its bytes.
        if (HandlerHelper.findEnclosingShortcut(expr) != null
                && expr.rawLeft() != null
                && NavigationHandler.chainProvesMulti(expr.rawLeft(), compiler)
                && NavigationHandler.chainProvesMulti(expr.rawRight(), compiler)) {
            // facet thenSeatMultiDefaultTernary (v3.1 flip seat 5), Rung C at the #345
            // arm: upstream compiles the arms withExpected(joined), so a meta-elemented
            // RIGHT over a bare-elemented LEFT takes the value deref — golden drr 7.x
            // GetUniqueTransactionIdentifier's utiFromReportableInformation alias body
            // (`…getIdentifier(…)).<String>map("Type coercion", fieldWithMetaString ->
            // fieldWithMetaString.getValue()) : …`). Green-safe: the un-coerced mixed
            // ternary is a generics mismatch against the alias's bare-joined
            // MapperC signature (the SAME walk's own return type) — it never compiled.
            String right = renderCoercedArm(rightBuilder,
                    analyzeDefaultJoin(expr, leftBuilder, rightBuilder, compiler, false), refs);
            return JavaExpression.from(
                    left + ".getMulti().isEmpty() ? " + right + " : " + left,
                    null,
                    refs,
                    wildcards);
        }
        // facet thenSeatMultiDefaultTernary (v3.1 flip seat 5): the MULTI default at
        // the then-BASE seat (`X default Y then …` — the default is the RThenExpr's
        // argument, walking through nested defaults: `(A default B) default C then …`)
        // renders upstream's list-form ternary — the #345 render outside the alias
        // scope. The gate is the ONE predicate the thenArg decl wrapper reads
        // (chainProvesMulti on the DEFAULT node — the seat-5 OR-join arm), so the
        // `final MapperC<…>` declaration and the ternary RHS cannot disagree (the #367
        // shared-predicate law). The seat-5 LAW-65 census carriers: drr 7.x
        // GetUniqueTransactionIdentifier `set uti` (the chained pair, both inner and
        // outer claimed here — the nested LEFT re-renders parenthesized at both its
        // receiver and else-arm positions, the golden shape) and
        // UniqueSwapIdentifierForValuation `set usi` (the mixed-meta pair — the RIGHT
        // arm takes the Rung-C coercion). The returned expression is STAMPED
        // MapperC<joinElem> where the join is proven, so the decl seat's #391
        // compiledStampsBareElement gate keeps the bare joined element (golden
        // `MapperC<String>`, not the #144 wrapper re-leak). The STATEMENT-seat multi
        // default (FilterChangePriceQuantity's whole-add if/else restructure) is a
        // DIFFERENT golden form and stays declined — the parent walk admits only the
        // then-argument position. Green-safe by the #345 argument verbatim: the
        // legacy `getOrDefault(<Mapper>)` passed a Mapper where the T-typed overload
        // expects the ITEM — non-compiling, so every carrier is an already-waivered
        // mismatch.
        if (thenBaseDefaultSeat(expr)
                && expr.rawLeft() != null
                && NavigationHandler.chainProvesMulti(expr, compiler)) {
            MixedJoin join = analyzeDefaultJoin(expr, leftBuilder, rightBuilder, compiler, true);
            String right = renderCoercedArm(rightBuilder, join, refs);
            boolean nestedLeft = expr.rawLeft() instanceof RDefaultExpr;
            String leftForTernary = nestedLeft ? "(" + left + ")" : left;
            JavaType stamped = null;
            if (join != null && compiler.getTypeUtil() != null) {
                stamped = compiler.getTypeUtil().wrap(
                        compiler.getTypeUtil().MAPPER_C, join.bareElem());
            }
            return JavaExpression.from(
                    leftForTernary + ".getMulti().isEmpty() ? " + right + " : " + leftForTernary,
                    stamped,
                    refs,
                    wildcards);
        }

        // facet iteArmMultiDefaultTernary (v3.1 flip seat 33, law C.2, rung R3b): the FOURTH
        // seat of the thenSeatMultiDefaultTernary family - a MULTI `default` that is an ARM of a
        // CONDITIONAL renders upstream's list-form ternary, exactly as the #345 alias arm and the
        // seat-5 then-BASE arm above already do at their own seats. Golden drr 7.x Price rung 1
        // (iosco cde v1 `set price:` line 100: `economicTerms -> payout -> SettlementPayout
        // only-element -> priceQuantity -> priceSchedule default (tradableProduct -> tradeLot ->
        // priceQuantity -> price filter ...)`) against the fork's `<L>.getOrDefault(<R>)`, which
        // passes a MapperC where the T-typed overload expects the ITEM - non-compiling, so every
        // carrier is an already-waivered mismatch (the #345 argument verbatim).
        // I TRACED ALL FIVE EXISTING GATES AND PRICE REACHES NONE: the #345 alias arm needs
        // findEnclosingShortcut != null (Price's ladder is in `set price:`); the seat-5 then-BASE
        // arm needs an RThenExpr parent after the nested-default walk (Price's parent is the
        // rung-1 RConditionalExpr); the D.2 extract-body arm needs a `then extract` inline-function
        // body; the #365 elided-left arm needs an alias scope + rawLeft() == null; the #365
        // ctor-FIELD arm needs an RKeyValuePair parent. It falls through to the plain
        // `left + ".getOrDefault(" + right + ")"` below, which is exactly the fork's bytes.
        // THE BARE PARENT SET IS NOT SAFE AND THE LAW DOES NOT USE IT. MEASURED (LAW 75,
        // [P33-DEFSEAT], both routes): `parentKind=RConditionalExpr` alone is 106 rows / 10
        // `where=` (rule:SingleOrUpperAndLowerBarrier 20, rule:CountryOfCounterparty2 18,
        // fn:ExecutionVenue 16, rule:PlatformAnonymousExecutionIndicator 14,
        // fn:TotalNotionalQuantityLeg2 10, rule:DTCC_SEFOrDCMAnonymousExecutionIndicator 10,
        // fn:ExecutionISIN 8, fn:DTCC_SEFOrDCMAnonymousExecutionIndicator 4, fn:Price 4,
        // rule:TotalNotionalQuantityOfLeg2 2). Adding the both-operands-MULTI conjunct closes it
        // EXACTLY: `parentKind=RConditionalExpr leftMulti=true rightMulti=true` = 4 rows / 1
        // `where=` / fn:Price, identical on the IR route. GREEN WOULD-FIRE: ZERO.
        // Positive control (so the zero above is trusted): the same tag's `thenBase=true` rows
        // are 29 / 5 `where=` incl. fn:GetUniqueTransactionIdentifier 8, and the gate histogram
        // reads fallthrough 935 / kvPair 164 / elidedLeft 60 / thenBase 12 / extractBody 4.
        // LAW 69: the multi proof is NavigationHandler.chainProvesMulti - the SAME predicate the
        // seat-5 arm and the #367 shared reader consult - never a re-implementation of
        // analyzeDefaultJoin. R3a has already coerced both builders above, so `left` (rendered
        // at the top of this method) and `right` (rendered here) both carry their derefs, and the
        // LEFT's deref is present in BOTH of its copies for free.
        // The MapperC stamp is the decl seat's channel (the #364 mapperCIteLift render-truth
        // read): golden assigns this ternary to a `final MapperC<PriceSchedule> thenArg;` ite
        // local, and stamping it is what tells wrapMapperFormIteArm the arm is already MULTI so
        // it stays verbatim rather than taking an item->list lift.
        // The parenthesised nested LEFT mirrors the seat-5 arm one block up and is INERT for the
        // measured carrier (Price's rawLeft is the `... -> priceSchedule` RFeatureCall, read from
        // the corpus source, not an RDefaultExpr); it exists so a future nested carrier cannot
        // splice an unparenthesised `A default B` into two operand positions.
        // The LEFT chainProvesMulti conjunct closes the population (106 bare-parent rows -> 4);
        // the RIGHT one is inert at this corpus (0 `leftMulti=true rightMulti=false` rows at
        // parentKind=RConditionalExpr) but is the arm's soundness guard: the arm renders its
        // right UNCOERCED and stamps MapperC from the LEFT builder, so a SINGLE right would be
        // mis-typed - it stays, with its measured zero.
        if (iteArmDefaultSeat(expr)
                && expr.rawLeft() != null
                && NavigationHandler.chainProvesMulti(expr.rawLeft(), compiler)
                && NavigationHandler.chainProvesMulti(expr.rawRight(), compiler)) {
            String iaRight = HandlerHelper.render(rightBuilder);
            String iaLeft = expr.rawLeft() instanceof RDefaultExpr ? "(" + left + ")" : left;
            JavaType iaStamped = null;
            if (compiler.getTypeUtil() != null) {
                JavaClass<?> iaElem = defaultJoinBareElement(leftBuilder, compiler.getTypeUtil());
                if (iaElem != null) {
                    iaStamped = compiler.getTypeUtil().wrap(
                            compiler.getTypeUtil().MAPPER_C, iaElem);
                }
            }
            return JavaExpression.from(
                    iaLeft + ".getMulti().isEmpty() ? " + iaRight + " : " + iaLeft,
                    iaStamped,
                    refs,
                    wildcards);
        }

        // facet extractBodyMultiDefaultTernary (v3.1 flip seat 32, law D2), rung 1: the
        // MULTI default that is the BODY of a `then extract` inline function, OUTSIDE any
        // alias, renders upstream's list-form ternary - the third seat of the
        // thenSeatMultiDefaultTernary family (the #345 alias arm above and the seat-5
        // then-base arm above it are the other two, and both have already returned, so
        // their carriers keep every byte). Golden drr 7.x IndicatorOfTheUnderlyingIndexRule:
        //   return <L>.getMulti().isEmpty() ? MapperC.of(ifThenElseResult) : <L>;
        // against the fork's `return MapperS.of(<L>.getOrDefault(ifThenElseResult));` - the
        // #234 defaultChainOperandUnwrap arm at :746-753, which is what this arm DISPLACES
        // (measured: [P32-DEFOUT], the :907 fall-through, does NOT print for the carrier).
        // That fork form passes a String to MapperC<FieldWithMetaString>.getOrDefault(T) and
        // never compiled (LAW 74, MEASURED: javac32 pre-javac.txt Seat32Pre.java:830
        // `error: incompatible types: String cannot be converted to FieldWithMetaString`),
        // so every carrier is an already-waivered mismatch.
        //
        // THE SEAT is the ONE shared predicate
        // NavigationHandler.extractBodyMultiDefaultTernary - read by this arm and by
        // ControlFlowHandler's Mapper-form ite slot (rungs 2/3), so the ternary's ARM and
        // the local it splices cannot disagree (the #367 single-predicate law). Its javadoc
        // carries the measured green blast radius (ZERO distinct non-carrier `where=` values
        // move) and the conservative-proof caution.
        //
        // THE RIGHT ARM'S LIFT IS THE COERCER'S, NOT HAND-ROLLED (LAW 69): rung 2 declares
        // the hoisted local `MapperS<joined>`, upstream's JavaConditionalExpression joins the
        // two arm types to the LEFT's MapperC, and WrapperToWrapperCoercer:107-114 is the
        // seat that already emits exactly that text (`MapperC.of(<e>)` + the MapperC ref).
        // The actual type handed to it is rung 2's own declaration - the two halves read one
        // predicate, so asserting it here states that law rather than guessing.
        //
        // THE JOIN IS THE LEFT'S BARE ELEMENT (upstream joinMetaAnnotatedTypes: meta survives
        // the join only when BOTH operands carry it, and the seat-28 law-5 rung above has
        // just established that this RIGHT renders none) - defaultJoinBareElement, the same
        // value-type read analyzeDefaultJoin performs at its own mixed-join arm. An
        // unresolvable/Object element declines the whole arm, byte-flat.
        //
        // THE RConditionalExpr CONJUNCT is the lift's OWN precondition, not part of the seat:
        // the actual type handed to the coercer is rung 2's declaration, and rung 2 lives in
        // the CONDITIONAL handler, so a non-conditional RIGHT would be lifted against a type
        // nothing declared. Measured: every carrier of this seat reads rightKind=
        // RConditionalExpr ([P32-DEF], 1,216 rows), so the conjunct is byte-inert TODAY and
        // exists so a future non-conditional carrier declines loudly instead of double-wrapping.
        if (NavigationHandler.extractBodyMultiDefaultTernary(expr, compiler)
                && expr.rawRight() instanceof RConditionalExpr
                && compiler.getTypeUtil() != null
                && compiler.getCoercionService() != null
                && rightBuilder instanceof JavaExpression) {
            JavaTypeUtil xbTu = compiler.getTypeUtil();
            JavaClass<?> xbElem = defaultJoinBareElement(leftBuilder, xbTu);
            if (xbElem != null) {
                JavaType xbMapperS = xbTu.wrap(xbTu.MAPPER_S, xbElem);
                JavaType xbMapperC = xbTu.wrap(xbTu.MAPPER_C, xbElem);
                JavaExpression xbArm = compiler.getCoercionService().coerceExpression(
                        JavaExpression.from(HandlerHelper.render(rightBuilder), xbMapperS,
                                rightBuilder.getRefs(),
                                rightBuilder.getStaticWildcardImports()),
                        xbMapperS, xbMapperC, true, ctx.scope());
                refs.addAll(xbArm.getRefs());
                wildcards.addAll(xbArm.getStaticWildcardImports());
                return JavaExpression.from(
                        left + ".getMulti().isEmpty() ? " + xbArm.renderToString()
                                + " : " + left,
                        xbMapperC,
                        refs,
                        wildcards);
            }
        }
        // facet multiDefaultTernary (PR #365, GODT widening): two siblings of the #345
        // alias arm above, both PARENTHESIZED (golden MapBreakdown cdm6 carries the
        // parens at both seats — the #345 carrier's unparenthesized splice is its own
        // locked form and stays byte-frozen).
        // (a) The ELIDED-left alias form (`… then <pipe> default <chain>`): rawLeft is
        // null (the #218 elision), the synthetic-implicit LEFT compiled above renders
        // the bound thenArg — its MapperC stamp is the multi proof (render truth);
        // golden `return (thenArg.getMulti().isEmpty() ? novatedAmount(…)\n\t.mapItem(…)
        // : thenArg);`.
        // v3.2 seat 2 (the chaos C5Forms `fallback` rows): the RIGHT of `vals then default [0]` is
        // a LIST LITERAL — multi by construction, which the chain walk (a navigation-shape reader)
        // never proved, so the arm declined to the single `getOrDefault` form (LAW 74: a MapperC
        // has no such method). The right also WIDENS to the left's element: upstream joins the
        // operands (BigDecimal) and coerces the Integer list through the MapperC rung
        // `.<BigDecimal>map("Type coercion", integer -> BigDecimal.valueOf(integer))` — the
        // coercion service's own form (HandlerHelper.widenIntegerMapperToBigDecimal). Golden:
        // `return (thenArg.getMulti().isEmpty() ? MapperC.<Integer>of(MapperS.of(0)).<BigDecimal>
        // map("Type coercion", integer -> BigDecimal.valueOf(integer)) : thenArg);`.
        if (HandlerHelper.findEnclosingShortcut(expr) != null
                && expr.rawLeft() == null
                && leftBuilder instanceof JavaExpression elidedLeftJe
                && compiler.getTypeUtil() != null
                && compiler.getTypeUtil().isMapperC(elidedLeftJe.getExpressionType())
                // (PR #623, round-1 cq review, SF-4: a third disjunct - the right's compiled MapperC
                // STAMP - stood here; the probe W-AI stayed green with it off, so it was withdrawn
                // under the #614 law.)
                && (NavigationHandler.chainProvesMulti(expr.rawRight(), compiler)
                        || expr.rawRight() instanceof RListLiteral)) {
            JavaStatementBuilder typedRight = rightBuilder;
            if (typedRight.getExpressionType() == null) {
                JavaType litType = HandlerHelper.listLiteralMapperTypeOrNull(
                        expr.rawRight(), compiler.getTypeUtil());
                if (litType != null) {
                    typedRight = NavigationHandler.retypeBuilder(typedRight, litType);
                }
            }
            JavaStatementBuilder widenedRight = HandlerHelper.isBigDecimalMapper(
                    leftBuilder, compiler.getTypeUtil())
                    ? HandlerHelper.widenIntegerMapperToBigDecimal(typedRight, compiler, ctx.scope())
                    : typedRight;
            refs.addAll(widenedRight.getRefs());
            wildcards.addAll(widenedRight.getStaticWildcardImports());
            String right = HandlerHelper.render(widenedRight);
            return JavaExpression.from(
                    "(" + left + ".getMulti().isEmpty() ? " + right + " : " + left + ")",
                    null,
                    refs,
                    wildcards);
        }
        // (b) The ctor-FIELD form (RKeyValuePair parent, operands proven by the ONE
        // shared predicate): upstream distributes the List coercion over the ternary
        // arms — each arm takes `.getMulti()` + the #232 defensive ArrayList copy
        // (golden MapBreakdown `.setQuantityValue((old…getMulti().isEmpty() ? new
        // ArrayList<>(new…getMulti()) : new ArrayList<>(old…getMulti())))`).
        // ConstructionHandler splices this verbatim under the SAME predicate
        // (NavigationHandler.multiDefaultTernaryOperands — the #367 hardening of the
        // #365 textually-identical gates into one shared read), so the two seats
        // cannot disagree. The predicate selects on LEFT-multi + a proven-Mapper
        // RIGHT (multi OR a resolved single alias — MapperS.getMulti() is the
        // 0/1-element list; golden MapPartyTradeIdentifier `.setAssignedIdentifier((
        // identifierForTrade(…).getMulti().isEmpty() ? new ArrayList<>(
        // identifierForIssuer(…).getMulti()) : …))`). Green-safe: getOrDefault takes
        // the ITEM type, so a MapperC-into-getOrDefault never compiled — every
        // carrier is waivered.
        if (expr.parent() instanceof RKeyValuePair
                && NavigationHandler.multiDefaultTernaryOperands(expr, compiler)) {
            String right = HandlerHelper.render(rightBuilder);
            refs.add(HandlerHelper.ARRAY_LIST);
            return JavaExpression.from(
                    "(" + left + ".getMulti().isEmpty() ? new ArrayList<>(" + right
                            + ".getMulti()) : new ArrayList<>(" + left + ".getMulti()))",
                    null,
                    refs,
                    wildcards);
        }

        // facet defaultForm (PR #218): a SCALAR-LITERAL default value (`… default False`)
        // is the plain-T getOrDefault overload — the arg renders BARE (not the
        // MapperS.of-wrapped literal form) and the whole result wraps in MapperS.of:
        // golden `MapperS.of(<X>.getOrDefault(false))` (a then-output appends `.get()`, a
        // comparison operand uses it as-is). Built via JavaExpression.from (NO unwrap-to-
        // builder contract) so the then-output's unwrapForAssignment APPENDS `.get()`
        // rather than structurally stripping the MapperS.of wrap. Green-safe: golden never
        // wraps a literal default arg in MapperS.of (12,432 bare getOrDefault(false|true)
        // carriers, zero getOrDefault(MapperS.of(<lit>))), so the fork's pre-fix wrapped-arg
        // form is always waivered; a non-literal default keeps the existing rendering.
        if (HandlerHelper.isScalarLiteral(expr.rawRight()) && rightBuilder instanceof JavaExpression rb
                && rb.unwrapToBuilder().isPresent()) {
            // facet defaultOpJoinedLiteralCoercion (PR #417): upstream compiles the
            // default RIGHT against the JOINED type (vendored ExpressionGenerator.xtend
            // L457-468, binaryExpr case "default": withExpected(joined)), so an INT
            // literal under a NUMBER-joined default coerces to the joined item type —
            // oracle golden expr-default-op (`x number (0..1)`, `set result: x default 0`):
            // `MapperS.of(x).getOrDefault(BigDecimal.valueOf(0))`. The shared right
            // compile above threads ctx.expectedType() (null at the assignment seat —
            // compile-time coercion dormant), so re-compile the literal against
            // BIG_DECIMAL here. Scoped to the mechanism the join can prove: an
            // in-long-range RIntLiteral (a beyond-long literal's BigDecimal-context
            // compile statement-hoists — a side effect this arm must not double) under a
            // number-typed LEFT (inferRuneTypeName — the arithmetic seat's own join
            // heuristic; a null/unresolved/boolean/string/int-typed left declines).
            // Booleans, strings and number literals keep their bytes verbatim;
            // corpus-neutral by the divergent=1 argument (an int-literal-under-number-
            // join carrier would already be byte-divergent — none is).
            JavaStatementBuilder bareRightBuilder = rb.unwrapToBuilder().get();
            // facet bigIntegerLiteralRulePath (seat 24, law F5): a BEYOND-long literal default under a
            // number-typed LEFT hoists through THE ONE producer (LiteralHandler.registerBigInteger-
            // LiteralHoistOrNull — the sink, else the extract-lambda channel) and is consumed as the ITEM
            // ternary (golden asic TotalNotionalQuantityLeg1 / mas TotalNotionalQuantityOfLeg2
            // `.getOrDefault((bigInteger0 == null ? null : new BigDecimal(bigInteger0)))`); the #417 arm
            // below keeps its within-long domain. A raw `new BigInteger("…")` at a BigDecimal
            // getOrDefault never compiled (LAW 74).
            if (expr.rawRight() instanceof RIntLiteral bigLit
                    && bigLit.value().bitLength() > 63
                    && (HandlerHelper.isNumberType(HandlerHelper.inferRuneTypeName(expr.rawLeft()))
                            || leftResolvesToNumberType(expr.rawLeft(), compiler))
                    && compiler.getTypeUtil() != null
                    && compiler.getLiteralHandler() != null) {
                String bigToken = compiler.getLiteralHandler()
                        .registerBigIntegerLiteralHoistOrNull(bigLit, ctx, compiler);
                if (bigToken != null) {
                    bareRightBuilder = JavaExpression.from(HandlerHelper.bigIntegerItemTernary(bigToken),
                            null, Set.of(HandlerHelper.BIG_INTEGER, HandlerHelper.BIG_DECIMAL));
                    refs.addAll(bareRightBuilder.getRefs());
                }
            }
            if (expr.rawRight() instanceof RIntLiteral intLit
                    && intLit.value().bitLength() <= 63
                    && (HandlerHelper.isNumberType(HandlerHelper.inferRuneTypeName(expr.rawLeft()))
                            || leftResolvesToNumberType(expr.rawLeft(), compiler))
                    && compiler.getTypeUtil() != null
                    && compiler.compile(expr.rawRight(), compiler.getTypeUtil().BIG_DECIMAL,
                            ctx.scope()) instanceof JavaExpression coercedRight
                    && coercedRight.unwrapToBuilder().isPresent()) {
                bareRightBuilder = coercedRight.unwrapToBuilder().get();
                refs.addAll(bareRightBuilder.getRefs()); // BIG_DECIMAL — the coerced literal names it
            }
            String bareRight = HandlerHelper.render(bareRightBuilder);
            refs.add(HandlerHelper.MAPPER_S);
            return JavaExpression.from(
                    "MapperS.of(" + left + ".getOrDefault(" + bareRight + "))",
                    null,
                    refs,
                    wildcards);
        }

        // facet bareEnumDefaultArg (PR #239): a BARE enum-value `default` RIGHT operand
        // (`<l> default Name`) renders the un-prefixed `Name` — a non-compiling undefined
        // symbol (enum values are not in function scope), so every carrier is already a
        // waivered mismatch. Qualify it to `EnumName.CONSTANT` + import — the default-position
        // analogue of ComparisonHandler's bare-enum comparand qualify and ConstructionHandler's
        // ctor-setter qualify. The target enum is the default's OWN joined inferred type (the
        // value is consumed AS that enum). Wrapped in MapperS.of WITH the unwrap contract so a
        // bare consumer (the setter / evaluate-arg) strips it to `<l>.getOrDefault(E.V)` (golden)
        // and a Mapper consumer (a return) keeps the wrap — identical to the #234 reduce path,
        // which would otherwise leave the bare value unqualified. Green-safe by construction.
        String bareEnumRight = tryDefaultBareEnumRightOrNull(expr, compiler, refs);
        if (bareEnumRight != null) {
            JavaExpression enumCore = JavaExpression.from(
                    left + ".getOrDefault(" + bareEnumRight + ")",
                    null,
                    refs,
                    wildcards);
            return JavaExpression.wrappedInMapperSOf(enumCore);
        }

        // facet defaultChainOperandUnwrap (PR #234): a single-cardinality `default`
        // compiles its RIGHT operand to the BARE item value (upstream binaryExpr
        // `case "default"`: the right at `withExpected(joined)` — the bare item type —
        // then `<l>.getOrDefault(<r>)` typed `resultType`). The fork's compile-time
        // coercion is dormant for the null-typed Mapper chains these fpml-ingest
        // defaults produce, so the right is reduced by shape and the result wrapped in
        // MapperS.of WITH the unwrap contract: a Mapper consumer (a return) keeps the
        // wrap (golden `return MapperS.of(<l>.getOrDefault(<r>))`), a bare consumer
        // (evaluate-arg / toBuilder / setter) strips it via unwrapToBuilder (golden
        // `<l>.getOrDefault(<r>)`). Green-safe: golden NEVER keeps a Mapper as a
        // `getOrDefault` argument — the right is always the bare item — so reducing
        // toward bare only moves a still-divergent carrier; the value-call
        // (`now.evaluate()`), conditional, bare-symbol and multi (MapperC) shapes
        // DECLINE and keep the legacy rendering byte-for-byte.
        // facet defaultSingleMixedJoinArgDeref (seat 29, law 9 companion): the SINGLE
        // `default` whose PROVEN mixed join (bare LEFT item, meta RIGHT wrapping the
        // same value type - the analyzeDefaultJoin read incl. its refs-recovery scan,
        // LAW 69) derefs the RIGHT at the getOrDefault ARGUMENT seat through a hoisted
        // wrapper local - golden drr 7.x FirstExerciseDateRule's AMERICAN arm:
        //   final FieldWithMetaDate fieldWithMetaDate = trade.<FieldWithMetaDate>map(...).get();
        //   ... .getOrDefault((fieldWithMetaDate == null ? null : fieldWithMetaDate.getValue()))
        // Sited BEFORE the #234 reduce arm, which otherwise claims the disguised-nav
        // RIGHT and renders the WRAPPER's `.get()` where getOrDefault(T) takes the BARE
        // item - non-compiling (LAW 74, javac29 PRE), so every carrier is an
        // already-waivered mismatch and the rung is green-safe by construction (the
        // same monotone argument as the recovery scan's thenBase precedent). SINGLE-only
        // (!chainProvesMulti: the MULTI joins belong to the #345/seat-5 ternary arms
        // above, which have already returned). The drain seat is the STATEMENT sink or
        // the #364 RESTRICTED arm-deref sink (the meta-deref producers' own channel -
        // this rung is that family's third member); no reachable sink declines
        // byte-flat to the reduce arm below. The local's name rides the
        // deferred-coercion-name channel (resolved at the statement root - the #364
        // producer's own machinery), so sibling hoists number correctly.
        if (!NavigationHandler.chainProvesMulti(expr, compiler)) {
            MixedJoin singleJoin =
                    analyzeDefaultJoin(expr, leftBuilder, rightBuilder, compiler, true);
            if (singleJoin != null && singleJoin.rightWrapper() != null) {
                com.regnosys.rosetta.generator.java.scoping.JavaStatementScope singleSink =
                        ctx.scope() == null ? null : ctx.scope().findStatementHoistSink();
                if (singleSink == null && ctx.scope() != null) {
                    singleSink = ctx.scope().findArmDerefHoistSink();
                }
                if (singleSink != null) {
                    String p29Wrapper = singleJoin.rightWrapper().getSimpleName();
                    String p29Base = com.regnosys.rosetta.generator.java.JavaNamingUtil
                            .toFirstLower(p29Wrapper);
                    String p29Token = ctx.scope().registerDeferredCoercionName(
                            ctx.scope().createUniqueIdentifier(p29Base));
                    singleSink.registerStatementHoist("final " + p29Wrapper + " " + p29Token
                            + " = " + HandlerHelper.render(rightBuilder) + ".get();");
                    refs.add(singleJoin.rightWrapper());
                    JavaExpression p29Core = JavaExpression.from(
                            left + ".getOrDefault((" + p29Token + " == null ? null : "
                                    + p29Token + ".getValue()))",
                            null,
                            refs,
                            wildcards);
                    return JavaExpression.selfUnwrapping(p29Core);
                }
            }
        }
        JavaStatementBuilder bareRight = reduceDefaultRightToBare(expr, rightBuilder, compiler);
        if (bareRight != null) {
            // facet iteArmMetaCollapseDerefSinkChannel (v3.1 flip seat 33, law B.24 / B.4, the
            // join-type rung, measured at QuantityUnitOfMeasure rung 6): a bare POJO right (the
            // sink-channel ite's typed consumer reference) joined onto a bare-item left
            // publishes the LEFT's item as the join's type - `getOrDefault(T)` returns the
            // item - so the then-level declaration reads `MapperS<NonNegativeQuantitySchedule>`
            // from render truth instead of falling back to the AST's meta-annotated element
            // (B24-green6: the decl typed `MapperS<FieldWithMeta...>` over a bare join).
            JavaType bareJoinType = null;
            JavaType rightStamp = rightBuilder.getExpressionType();
            JavaType leftStampT = leftBuilder.getExpressionType();
            if (compiler != null && compiler.getTypeUtil() != null
                    && rightStamp instanceof JavaClass<?> rightPojo
                    && !(rightPojo instanceof RJavaWithMetaValue)
                    && !compiler.getTypeUtil().isMapper(rightStamp)
                    && leftStampT != null
                    && compiler.getTypeUtil().getItemType(leftStampT) instanceof JavaClass<?> leftItemT
                    && !(leftItemT instanceof RJavaWithMetaValue)
                    && leftItemT.getCanonicalName().withDots()
                            .equals(rightPojo.getCanonicalName().withDots())) {
                bareJoinType = leftItemT;
            }
            JavaExpression core = JavaExpression.from(
                    left + ".getOrDefault(" + HandlerHelper.render(bareRight) + ")",
                    bareJoinType,
                    refs,
                    wildcards);
            return JavaExpression.wrappedInMapperSOf(core);
        }

        // facet getOrDefaultArgMetaDeref (PR #363) — the RENDER-TRUTH channel the #362
        // cp6b REFUTED-class javadoc banked (the collapseMetaWrapper AST walk at this
        // seat was wrong in BOTH directions: it declined the target carrier's ARGS-form
        // alias descent while resolving a wrapper on Contract_Price_Monetary iosco,
        // whose golden does NOT hoist — over-fire AWAY, ratio 0.6618 → 0.6353). The gate
        // reads the right's chain root through the scope thenArg BINDING (the #350
        // bindThenArg channel, wrapper-typed by the #362 alias-signature decl re-type):
        // a then-chain RIGHT whose LAST body binds a Mapper of an RJavaWithMetaValue
        // element AND whose body root is a COLLAPSING list-op (first/last/only-element —
        // the rendered right is a single wrapper item) derefs at the arg: hoist
        // `final FieldWithMetaString fieldWithMetaString = distinct(thenArg)
        // \t.first().get();` (the #237-convention statement hoist; the drain re-anchors
        // the relative continuation) + `getOrDefault((x == null ? null : x.getValue()))`
        // built SELF-UNWRAPPING — getOrDefault(T) returns the ITEM, so the assignment
        // consumer appends no `.get()` (golden drr GetInternalId). A HOMOGENEOUS-meta LEFT
        // (the same wrapper class both sides) declines — golden keeps the wrapper there, and
        // the #296 mixed-join law owns the baresym half; a HETEROGENEOUS-meta LEFT (a
        // different wrapper class over the same value type) derefs too, facet
        // defaultJoinHeteroMetaDerefBoth (v3.1 flip seat 33 law B.2, in this method). No sink
        // / no binding / non-collapsing body root / bare-element binding all decline to the
        // legacy fall-through below, byte-unchanged.
        JavaExpression metaArgDeref = tryThenBoundMetaArgDeref(
                expr, rightBuilder, left, leftBuilder, compiler, ctx, refs, wildcards);
        if (metaArgDeref != null) {
            return metaArgDeref;
        }
        // facet defaultRightNestedThenHoist (v3.1 flip seat 30, law 4, rung 2): the
        // BARE-ITEM sibling of the #363 arm above. A `default` whose RIGHT is a then-chain
        // that HOISTED and whose LAST body is a COLLAPSING list-op renders a SINGLE-ITEM
        // Mapper (`MapperS.of(thenArgN.get())` for only-element, `<...>.first()` for
        // first/last), and runtime `getOrDefault` takes the ITEM (Mapper.java:26) - so the
        // arg collapses `.get()` and the whole result re-wraps MapperS.of WITH the unwrap
        // contract, serving both consumer seats exactly as the #376 / seat-28 rung-A arm
        // below does for a collapse at rawRight. This arm is that arm ONE LEVEL DOWN: the
        // collapse sits inside a `then` body, so `expr.rawRight() instanceof RListOpExpr`
        // is false and it fell to the legacy tail.
        //
        // Golden drr 7.x CommodityQuantityWithFrequency:
        //   final MapperS<PriceQuantity> thenArg5 =
        //       MapperS.of(thenArg3.getOrDefault(MapperS.of(thenArg4.get()).get()));
        // against the fork's `thenArg3.getOrDefault(<right>)` with NEITHER the arg collapse
        // NOR the outer wrap - a Mapper into the T-typed overload AND a raw value into a
        // MapperS<T> local, non-compiling twice over (LAW 74), so every carrier is already
        // waivered.
        //
        // DISJOINT from every neighbouring arm: #363 has already returned for a META bound
        // item (and this arm re-tests it, so a #363 sink-less decline cannot leak here -
        // deref-by-`.get()` would yield the WRAPPER, not the value); the seat-28 rung-B arm
        // below requires the then body root to be an RDefaultExpr; the #376/rung-A arm
        // requires the collapse at rawRight; reduceDefaultRightToBare declines a `then`
        // chain by its documented contract. Dormant without law 4 rung 1: no hoist means no
        // thenArgRefFor binding (see thenBoundCollapseItem).
        JavaType collapseItem = thenBoundCollapseItem(expr, compiler, ctx);
        if (collapseItem != null && !(collapseItem instanceof RJavaWithMetaValue)
                && rightBuilder instanceof JavaExpression) {
            JavaExpression collapseCore = JavaExpression.from(
                    left + ".getOrDefault(" + HandlerHelper.render(rightBuilder) + ".get())",
                    null,
                    refs,
                    wildcards);
            return JavaExpression.wrappedInMapperSOf(collapseCore);
        }
        // facet defaultCollapsingListOpRight (seat 28, law 3, rung B): the NESTED
        // default - a `then`-chain RIGHT whose BODY is itself a `default` that
        // rendered WRAPPED (the rung-A / #234 wrappedInMapperSOf contract) collapses
        // the arg `.get()` on the WRAP and re-wraps the whole, exactly like the
        // collapsing-list-op arm below. Golden NotionalQuantityRule (drr 7.x,
        // `EquityNotionalQuantity default (CommodityQuantityWithFrequency then value
        // default datedValue -> value first)`):
        //   MapperS.of(<eq>.getOrDefault(MapperS.of(<inner default>).get()))
        // against the fork's bare `<eq>.getOrDefault(<inner default>)` - a MapperS
        // passed to the T-typed getOrDefault overload AND assigned to a MapperS<T>
        // local, non-compiling twice over (LAW 74), so every carrier is waivered.
        // DISJOINT from the #363 meta-arg-deref arm ABOVE by BOTH ordering (it has
        // already returned) and shape (its gate requires the then body ROOT to be a
        // collapsing LIST-OP; this arm requires it to be an RDefaultExpr). The
        // wrap-prefix term is the render-truth channel (a rendered-text read of the
        // same class the retired alias-seam prefix gates were, PR #612): the then-chain
        // compile does NOT propagate the inner
        // JavaExpression's unwrapToBuilder contract (MEASURED at this head - the
        // first cut gated on it and the outer fell to the legacy tail, rendering
        // `<left>.getOrDefault(MapperS.of(<inner>))` with neither the arg collapse
        // nor the outer wrap), so the wrap is read off the rendered text itself -
        // present ONLY when the inner default took a wrapping arm, so an inner
        // that declined keeps today's bytes verbatim.
        //
        // ADJUDICATED (v3.1 flip seat 30, PROBE30-L3B - the #600 review's law-3b
        // bank CLOSED REFUTED-PERMANENT): one instrumented D11 round per route over
        // all 25 cells joined every rung-B entry to its inner default's taken arm
        // (completeness control EXACT: 1256 = 1256 ladder-exit rows, all 13 exits
        // reached).  The rung-B reach set is ONE site x 4 cells (rule
        // NotionalQuantity, drr 7.0-7.3), and the prefix verdict AGREED with the
        // inner's own taken arm 4/4 on BOTH routes - zero false admits, zero false
        // declines.  The bank's drafted static re-derivation was refuted in both
        // directions at the fixture head (law3b-bank.md): a shape-derived verdict
        // is only sound if it consults the SAME context the render did - at which
        // point it IS the render.  The prefix read is therefore LOAD-BEARING and
        // stays; the closure is by the AGREEMENT branch, with the stated caveat
        // that only ONE inner shape exists in today's corpus - a future carrier
        // with a second shape re-opens the question, not this disposition.
        if (expr.rawRight() instanceof RThenExpr nestedThen
                && nestedThen.body().map(RInlineFunction::body).orElse(null)
                        instanceof RDefaultExpr
                && rightBuilder instanceof JavaExpression nestedRb
                && nestedRb.renderToString().startsWith("MapperS.of(")) {
            JavaExpression nestedCore = JavaExpression.from(
                    left + ".getOrDefault(" + HandlerHelper.render(rightBuilder) + ".get())",
                    null,
                    refs,
                    wildcards);
            return JavaExpression.wrappedInMapperSOf(nestedCore);
        }
        // facet getOrDefaultMinMaxArgCollapse (PR #376, G1): a MIN/MAX-collapse RIGHT
        // (`… min [trigger -> level] default … min [trigger -> levelPercentage]`) is a
        // single Mapper-valued chain the #234 reducer deliberately declined — collapse
        // the arg `.get()` (golden never keeps a MIN/MAX-VALUED Mapper as a getOrDefault
        // argument — it collapses `.get()`; runtime getOrDefault takes the ITEM. Seat-1
        // #376 OBS-2 precision: goldens DO carry `.getOrDefault(Mapper…` text at
        // line-wrapped collapsed forms and non-min/max defaults, both outside this
        // arm's rawRight gate) and wrap the
        // whole result MapperS.of WITH the unwrap contract, so a Mapper consumer keeps
        // the wrap (golden SingleOrUpperAndLowerBarrierRule `thenArg1 = MapperS.of(
        // <min>.getOrDefault(<min>.get()));`) and a bare consumer strips it — exactly
        // the #234 dual-consumer behaviour. Green-safe: the bare Mapper arg never
        // compiled, so every carrier is an already-waivered mismatch.
        // facet defaultCollapsingListOpRight (seat 28, law 3, rung A): a COLLAPSING
        // LIST-OP RIGHT (`first`/`last`/`only-element` - isCollapsingListOp, the ONE
        // shared definition this file already carries and consults at the #367 reducer
        // arm, LAW 69) is the SAME single-item-Mapper shape as min/max and takes the
        // SAME collapse + re-wrap: golden MapNonpublicExecutionReportToWorkflowStep
        // `final MapperS<ZonedDateTime> thenArg1 = MapperS.of(thenArg0.getOrDefault(
        // <chain>.first().get()));` against the fork's `thenArg0.getOrDefault(
        // <chain>.first())` - a raw T assigned to a MapperS<T> local, which never
        // compiled (LAW 74), so every carrier is already waivered. DISJOINT from the
        // #367 reducer arm by construction: that arm returns forty-six lines earlier
        // for a list-op over a BARE SYMBOL root (green ExtractProductIdentifierBySource
        // `productIdentifiers first`); only a CHAIN-rooted collapsing right survives to
        // here. MEASURED (PROBE28-F10c, 152 rows/route, route-identical): rightListOp
        // non-null in exactly 20 rows, ALL `FIRST`, ALL band (MapNonpublic 16 +
        // NotionalQuantity 4); `LAST`/`ONLY_ELEMENT` measured ZERO here, so their
        // admission via the shared predicate is byte-inert by measurement and is
        // witnessed only by the fixture. The 20 pre-existing admissions (all RMin/RMax,
        // all on the GREEN SingleOrUpperAndLowerBarrierRule) keep their bytes.
        if ((expr.rawRight() instanceof com.regnosys.rosetta.ast.expressions.unary.RMinExpr
                || expr.rawRight() instanceof com.regnosys.rosetta.ast.expressions.unary.RMaxExpr
                || (expr.rawRight() instanceof RListOpExpr rightCollapse
                        && isCollapsingListOp(rightCollapse)))
                && rightBuilder instanceof JavaExpression) {
            JavaExpression minMaxCore = JavaExpression.from(
                    left + ".getOrDefault(" + HandlerHelper.render(rightBuilder) + ".get())",
                    null,
                    refs,
                    wildcards);
            return JavaExpression.wrappedInMapperSOf(minMaxCore);
        }
        String right = HandlerHelper.render(rightBuilder);
        // v3.2 seat 13 (D53 - the M5a class's to-string half, oracle group tostring-over-default-enum, the chaos s31
        // C31Defaults rows, 13 of 13 non-compiling by the seat-13 census): a QUALIFIED enum-value right
        // (`default SideEnum -> Short`, parser-bound - the producer's enum-constant witness) reached this legacy
        // fallthrough BARE where its bare-name twin (`default Short`, the #239 arm above) returns the MapperS.of
        // wrap WITH the unwrap contract - so a Mapper consumer (the to-string's `.map("to-string", …)`, a return)
        // lost the wrap here and a bare consumer never needed it. ONE wrap for both enum-right forms (LAW 69):
        // golden `MapperS.of(<l>.getOrDefault(SideEnum.SHORT)).map("to-string", SideEnum::toDisplayString)`; every
        // bare consumer strips it to today's bytes through the contract.
        JavaExpression getOrDefaultCore = JavaExpression.from(
                left + ".getOrDefault(" + right + ")",
                null,
                refs,
                wildcards);
        return HandlerHelper.isBareEnumConstant(rightBuilder)
                ? JavaExpression.wrappedInMapperSOf(getOrDefaultCore)
                : getOrDefaultCore;
    }

    /**
     * facet thenSeatMultiDefaultTernary (v3.1 flip seat 5): the then-BASE seat gate —
     * the default is the {@link RThenExpr}'s ARGUMENT, walked through nested defaults
     * ({@code (A default B) default C then …}: both the inner and the outer sit on the
     * then-argument side; a then-BODY default's parent is the {@link RInlineFunction},
     * so it declines, and the STATEMENT-seat default (no enclosing then) declines —
     * the FilterChangePriceQuantity if/else-restructure class keeps its bytes).
     */
    private static boolean thenBaseDefaultSeat(RDefaultExpr expr) {
        Object p = expr.parent();
        while (p instanceof RDefaultExpr d) {
            p = d.parent();
        }
        return p instanceof RThenExpr;
    }

    /**
     * facet iteArmMultiDefaultTernary (v3.1 flip seat 33, law C.2, rung R3b): the ite-ARM seat
     * gate - the {@code default} is an ARM of an {@link RConditionalExpr}, walked through nested
     * defaults exactly as {@link #thenBaseDefaultSeat} walks them to the then-argument
     * ({@code (A default B) default C} inside an arm: both the inner and the outer sit on the arm
     * side). The SIBLING of that gate, one parent kind across.
     *
     * <p>This predicate ALONE is not safe and is never used alone: {@code parentKind=
     * RConditionalExpr} is 106 rows over 10 {@code where=} values ({@code [P33-DEFSEAT]},
     * measured both routes). The caller conjoins {@code chainProvesMulti} on BOTH raw operands,
     * which closes the population to 4 rows / 1 {@code where=} ({@code fn:Price}) with ZERO green.
     */
    private static boolean iteArmDefaultSeat(RDefaultExpr expr) {
        Object p = expr.parent();
        while (p instanceof RDefaultExpr d) {
            p = d.parent();
        }
        return p instanceof RConditionalExpr;
    }

    /**
     * facet heteroMetaDefaultJoinDeref (v3.1 flip seat 33, law C.2, rung R3a): the
     * HETEROGENEOUS-meta join. Upstream's {@code joinMetaAnnotatedTypes} keeps meta only when
     * BOTH operands carry the SAME annotation, so two operands under DIFFERENT wrappers over the
     * SAME value type join to the BARE value and both sides deref. Returns {@code {left, right}}
     * with both builders re-stamped at {@code Mapper*<value>} and their rendered text carrying
     * the deref, or {@code null} to leave both untouched.
     *
     * <p><b>The two channels, in the seat-28 order.</b> The LEFT is read from the COMPILED STAMP
     * only - the channel {@link #leftRendersMetaWrapper} already trusts at this seat. The RIGHT
     * is read from the stamp FIRST and, where the stamp is silent, from the shared meta WALKER
     * {@code NavigationHandler.recoverExprMetaWrapper} - the same two-channel order, in the same
     * sequence, that {@link #rightRendersNoMetaWrapper} uses, so the two readings of "is the
     * RIGHT meta" cannot disagree (LAW 69). That pairing is also exactly what
     * {@code [P33-DEFJOIN]} measured, so the law's population at its own head WAS the measured
     * 12 rows (Price 4 + QUOM 8); QUOM's eight then-chain rights moved to law B.24's seat one
     * commit later, leaving Price's four here.
     *
     * <p><b>{@code sameWrapperDenotation} is the protection.</b> A HOMOGENEOUS-meta default keeps
     * its wrapper and must not hop: golden drr {@code MessageID} (9 GREEN cells, both operands
     * {@code FieldWithMetaString}), {@code GetBasketConstituents:173},
     * {@code rule:QuantitySchedule} (20 rows) and 32 further {@code fn:QuantityUnitOfMeasure}
     * rows all read {@code sameWrap=true}. The comparison is CANONICAL, never simple-name - the
     * predicate {@code NavigationHandler} promoted at seat 33 law A.1 for this class of question.
     *
     * <p><b>The deref string is {@link #renderCoercedArm}'s</b>, the unguarded bare elementwise
     * form golden's MULTI operands carry - NOT {@code coerceNavigationReceiver}'s guarded
     * numbered form, which is golden's SINGLE (map-terminal) shape. Same law, two Mapper forms;
     * the wrong one costs the file.
     */
    private static JavaStatementBuilder[] heteroMetaJoinDerefOrNull(RExpression rightExpr,
            JavaStatementBuilder leftBuilder, JavaStatementBuilder rightBuilder,
            ExpressionCompiler compiler) {
        JavaTypeUtil tu = compiler == null ? null : compiler.getTypeUtil();
        if (tu == null || !(leftBuilder instanceof JavaExpression)
                || !(rightBuilder instanceof JavaExpression)) {
            return null;
        }
        // facet defaultJoinHeteroMetaDerefBoth (v3.1 flip seat 33, law B.24 / B.2 - the
        // OWNERSHIP cut, measured at the QUOM carrier): a THEN-chain right is the then-bound
        // collapse seat's (tryThenBoundMetaArgDeref, #363 + law B.2), whose golden form is the
        // hoisted WRAPPER local + the guarded deref as the getOrDefault ARGUMENT + the
        // MapperS.of( re-wrap (drr 7.x QuantityUnitOfMeasure:68-69). This mapper-form deref
        // pre-derefing such a right rendered the wrapper local with a coercion appended to its
        // own initializer and dropped the re-wrap (B24-red1/green1: rung 5's double deref),
        // so a then-right declines HERE and the then-bound seat owns it. Price's rung-1 right
        // is a plain navigation, unaffected.
        if (rightExpr instanceof RThenExpr) {
            return null;
        }
        JavaType leftType = leftBuilder.getExpressionType();
        if (leftType == null
                || !(tu.getItemType(leftType) instanceof RJavaWithMetaValue leftWrapper)) {
            return null;
        }
        JavaType rightType = rightBuilder.getExpressionType();
        RJavaWithMetaValue rightWrapper =
                rightType != null && tu.getItemType(rightType) instanceof RJavaWithMetaValue stampW
                        ? stampW
                        : NavigationHandler.recoverExprMetaWrapper(rightExpr, compiler);
        if (rightWrapper == null
                || NavigationHandler.sameWrapperDenotation(leftWrapper, rightWrapper)
                // value-type EQUALITY - a deliberate narrowing of upstream's joinMetaAnnotatedTypes
                // LCS join, at a measured cost of zero: 0 both-wrapped vtEq=false rows on both routes
                || !leftWrapper.getValueType().equals(rightWrapper.getValueType())
                || !(leftWrapper.getValueType() instanceof JavaClass<?> joinElem)
                || "Object".equals(joinElem.getSimpleName())) {
            return null;
        }
        return new JavaStatementBuilder[] {
                heteroDerefArm(leftBuilder, leftWrapper, joinElem, tu),
                heteroDerefArm(rightBuilder, rightWrapper, joinElem, tu) };
    }

    /**
     * facet heteroMetaDefaultJoinDeref (v3.1 flip seat 33, law C.2, rung R3a): one operand's
     * elementwise deref, re-stamped at the BARE join element. The Mapper KIND is preserved from
     * the operand's own stamp (a {@code map} over a MapperC is a MapperC), so the ite-arm decl
     * seat's render-truth read sees the same cardinality it saw before the hop. The refs are the
     * operand's own UNION the join element - the option-F preservation invariant
     * ({@code JavaExpression.from}'s 4-arg javadoc: an emission that takes a compiled inner
     * builder and produces a new outer source MUST carry both channels through).
     */
    private static JavaStatementBuilder heteroDerefArm(JavaStatementBuilder operand,
            RJavaWithMetaValue wrapper, JavaClass<?> joinElem, JavaTypeUtil tu) {
        Set<JavaClass<?>> derefRefs = new HashSet<>(operand.getRefs());
        String derefText = renderCoercedArm(operand, new MixedJoin(joinElem, wrapper), derefRefs);
        JavaType stamped = tu.isMapperC(operand.getExpressionType())
                ? tu.wrap(tu.MAPPER_C, joinElem)
                : tu.wrap(tu.MAPPER_S, joinElem);
        return JavaExpression.from(derefText, stamped, derefRefs,
                operand.getStaticWildcardImports());
    }

    /**
     * facet thenSeatMultiDefaultTernary (v3.1 flip seat 5): the proven join of a
     * {@code default}'s operand elements. {@code bareElem} is the bare (non-meta)
     * LEFT element; {@code rightWrapper} is non-null when the RIGHT element is the
     * {@link RJavaWithMetaValue} wrapper OVER that same bare element (the mixed-meta
     * join — the arm derefs; the #296 mixed-join law: the join is the VALUE type).
     * {@code null} verdict = unproven (either element unresolvable, a meta LEFT —
     * unmeasured, declined — or differing elements): no coercion, no stamp, the
     * ternary renders un-coerced with today's decl machinery untouched.
     */
    private record MixedJoin(JavaClass<?> bareElem, RJavaWithMetaValue rightWrapper) {}

    /**
     * facet defaultMetaJoinFromRender (seat 28, law 5): the RENDER-truth read of "the LEFT
     * operand rendered a meta wrapper". The inferred-type channel is REFUTED at this seat
     * (PROBE28-F10b: {@code leftInferredMeta=false} while {@code leftCompiled} is a
     * {@code FieldWithMeta...}); the compiled stamp is the channel that answers. Declines
     * safely on a null stamp - that channel is only partially populated corpus-wide, so a
     * decline is the common case and must be byte-flat.
     */
    private static boolean leftRendersMetaWrapper(JavaStatementBuilder leftBuilder,
                                                  ExpressionCompiler compiler) {
        if (compiler == null || compiler.getTypeUtil() == null) {
            return false;
        }
        JavaType t = leftBuilder.getExpressionType();
        return t != null
                && compiler.getTypeUtil().getItemType(t) instanceof RJavaWithMetaValue;
    }

    /**
     * facet metaWrapperRecovery (v3.1 C2d retirement family 6, PR #613): the VALUE type of the
     * meta wrapper the LEFT operand carries, read BEFORE a deref arm strips it — the bare element
     * the join lands on. {@code null} when the LEFT is untyped, is not wrapper-typed, or the
     * wrapper's value is not a plain class. Read only to record the producer's own decision — the
     * consumer is {@code FunctionExpressionRenderer}'s block-arm read, which asks the recorded
     * {@code derefdLeftValue} where it used to scan the rendered value for a
     * {@code .getValue()).getOrDefault(} tail.
     */
    private static JavaClass<?> leftWrapperValueTypeOrNull(JavaStatementBuilder leftBuilder,
                                                           ExpressionCompiler compiler) {
        if (compiler == null || compiler.getTypeUtil() == null || leftBuilder == null) {
            return null;
        }
        JavaType t = leftBuilder.getExpressionType();
        if (t == null
                || !(compiler.getTypeUtil().getItemType(t) instanceof RJavaWithMetaValue wrapper)) {
            return null;
        }
        return wrapper.getValueType() instanceof JavaClass<?> value ? value : null;
    }

    /**
     * facet defaultMetaJoinFromRender (seat 28, law 5): "the RIGHT operand renders NO meta
     * wrapper" - upstream's own join question ({@code joinMetaAnnotatedTypes}: meta survives
     * the join only when BOTH operands carry it), read from the two channels that can answer
     * it, in this order.
     *
     * <p><b>(1) The COMPILED stamp, when present, is definitive and is checked FIRST.</b>
     * It is what holds the {@code fn:Price} shape out: measured at exactly ONE
     * {@code where=} value corpus-wide ({@code rightItemIsMeta=true} with
     * {@code rightAstMetaCF=null}, 4 rows, {@code rightType=MapperC<
     * FieldWithMetaPriceSchedule>}, {@code rightKind=RFilterExpr} - a {@code filter} right
     * has no walker arm, so the AST channel is silent there while the render is not).
     * Price is a six-sig band file no seat-32 law owns; the stamp keeps it byte-flat.
     *
     * <p><b>(2) Where the stamp is silent the question goes to the shared meta WALKER, not
     * to the refs superset.</b> {@link JavaStatementBuilder#getRefs()} carries the classes
     * textually present in {@code renderToString()}, so an INCIDENTAL interior wrapper - one
     * an arm of a conditional right derefs elementwise and that never reaches the join -
     * reads as "the right is meta" and declines. That false negative is what banded the two
     * drr 7.0-7.3 carriers below.
     *
     * @param rightExpr the RIGHT operand's AST node - the channel the join question is
     *        actually about; {@code null} declines to the walker's own null-safe answer
     */
    private static boolean rightRendersNoMetaWrapper(RExpression rightExpr,
                                                     JavaStatementBuilder rightBuilder,
                                                     ExpressionCompiler compiler) {
        if (compiler == null || compiler.getTypeUtil() == null) {
            return false;
        }
        JavaType t = rightBuilder.getExpressionType();
        if (t != null
                && compiler.getTypeUtil().getItemType(t) instanceof RJavaWithMetaValue) {
            return false;
        }
        // facet defaultJoinDerefAtCollapsedLeft (v3.1 flip seat 32, law D1): golden derefs a
        // meta LEFT in place over a meta-free RIGHT - drr 7.0-7.3
        // UnderlyingIndexIndicatorRule `.first().<String>map("Type coercion",
        // fieldWithMetaString -> fieldWithMetaString == null ? null :
        // fieldWithMetaString.getValue()).getOrDefault(ifThenElseResult)` against the fork's
        // un-deref'd `.first().getOrDefault(ifThenElseResult)`, which passes a String to
        // MapperS<FieldWithMetaString>.getOrDefault(T) and does not compile (LAW 74:
        // javac32 PRE, Seat32Pre.java:870 "incompatible types: String cannot be converted to
        // FieldWithMetaString"). The channel that decides "is the RIGHT meta-free" is the
        // shared meta walker - the same arm-by-arm join seat 14's defaultJoinMetaRecovery
        // already consults (LAW 69: one walk, two consumers) - never the refs superset. Its
        // false negative declined the law at UnderlyingIndexIndicator and
        // IndicatorOfTheUnderlyingIndex, where the conditional RIGHT's arm text names a
        // FieldWithMetaFloatingRateIndexEnum the arms deref themselves: measured
        // `rightRefsHasMeta=true rightAstMetaCF=null` at those two `where=` values and at NO
        // other, over all 1,216 [P32-DEF] rows, on BOTH routes. The would-fire set is 8 rows
        // / 2 `where=` values, both this seat's own carriers; the regression quadrant (fires
        // today, would decline) is ZERO rows. The GREEN meta-RIGHT population keeps
        // declining, now on the right channel rather than by accident: fn:MessageID (9 rows,
        // the 9 GREEN cells the seat-28 javadoc protects) and fn:GetBasketConstituents (4)
        // recover FieldWithMetaString, rule:QuantitySchedule (20) and fn:QuantityUnitOfMeasure
        // (16) recover their own wrappers.
        return NavigationHandler.recoverExprMetaWrapper(rightExpr, compiler) == null;
    }

    private static MixedJoin analyzeDefaultJoin(RDefaultExpr expr,
            JavaStatementBuilder leftBuilder, JavaStatementBuilder rightBuilder,
            ExpressionCompiler compiler, boolean thenBaseSeat) {
        JavaType leftItem = defaultOperandItemType(expr.rawLeft(), leftBuilder, compiler, expr);
        if (!(leftItem instanceof JavaClass<?> bare) || leftItem instanceof RJavaWithMetaValue) {
            return null;
        }
        JavaType rightItem = defaultOperandItemType(expr.rawRight(), rightBuilder, compiler, expr);
        if (rightItem instanceof RJavaWithMetaValue metaWrap
                && metaWrap.getValueType() instanceof JavaClass<?> valueClass
                && valueClass.getCanonicalName().withDots()
                        .equals(bare.getCanonicalName().withDots())) {
            return new MixedJoin(bare, metaWrap);
        }
        if (rightItem instanceof JavaClass<?> rightClass
                && !(rightItem instanceof RJavaWithMetaValue)
                && rightClass.getCanonicalName().withDots()
                        .equals(bare.getCanonicalName().withDots())) {
            return new MixedJoin(bare, null);
        }
        // The re-rooted then-chain RIGHT (a k>=1 continuation off the bound thenArg —
        // the corpus UniqueSwapIdentifierForValuation shape) loses its stamp in the
        // re-rooting, so its wrapper is recovered from the RIGHT's OWN AST.
        // facet metaWrapperRecovery (v3.1 C2d retirement family 6, PR #613): the #331
        // all-present-arms-agree walker NavigationHandler.recoverExprMetaWrapper — the same
        // walker the seat-32 law D.1 precedent already put in this file's sibling read
        // (rightRendersNoMetaWrapper) — value-type-matched against the proven bare LEFT,
        // instead of a unique-candidate scan of the right compile's emitted-import set. The
        // census measured this seat at 197 reached arrivals / 4 fires with 0 disagreements on
        // every walk. RECOVERY SEATS ONLY (the then-base seat and the seat-29 single-arg-deref
        // rung, both with ZERO green carriers by the non-compiling argument): the #345 alias
        // arm's carriers can be GREEN (a same-typed ternary compiles), so a recovery must never
        // coerce a green bare-leaf arm — at the then-base seat no green carrier exists at all
        // (the legacy getOrDefault(Mapper) never compiled), so the recovery is monotone there.
        // A NESTED-default RIGHT declines (the indep review's finding): a claimed
        // inner ternary renders null-stamped, and a deref appended to its
        // UNPARENTHESIZED text would bind only to the inner's else arm by Java
        // precedence — the recovery's charter shape is the re-rooted then-chain RIGHT
        // (the UniqueSwapIdentifierForValuation trail), never a nested default.
        if (thenBaseSeat && rightItem == null && rightBuilder != null
                && !(expr.rawRight() instanceof RDefaultExpr)) {
            RJavaWithMetaValue unique =
                    NavigationHandler.recoverExprMetaWrapper(expr.rawRight(), compiler);
            if (unique != null
                    && unique.getValueType() instanceof JavaClass<?> valueClass2
                    && valueClass2.getCanonicalName().withDots()
                            .equals(bare.getCanonicalName().withDots())) {
                return new MixedJoin(bare, unique);
            }
        }
        return null;
    }

    /**
     * facet thenSeatMultiDefaultTernary (v3.1 flip seat 5), Rung C: the rendered
     * RIGHT arm, with the joined-type value deref appended when the mixed-meta join
     * is proven — golden's {@code .<String>map("Type coercion", fieldWithMetaString ->
     * fieldWithMetaString.getValue())} (the SHORT form — the measured golden at the
     * ternary arms; the null-ternary form belongs to the SINGLE-default #296 family).
     * The lambda parameter is the decapitalized wrapper simple name (the
     * CollectionHandler wrapper-param convention). An unproven join returns the arm
     * verbatim — monotone: the un-coerced mixed ternary at a typed seat never
     * compiled, so every coerced carrier is an already-waivered mismatch.
     */
    private static String renderCoercedArm(JavaStatementBuilder rightBuilder,
            MixedJoin join, Set<JavaClass<?>> refs) {
        String right = HandlerHelper.render(rightBuilder);
        if (join == null || join.rightWrapper() == null) {
            return right;
        }
        String wrapperName = join.rightWrapper().getSimpleName();
        String param = Character.toLowerCase(wrapperName.charAt(0)) + wrapperName.substring(1);
        refs.add(join.bareElem());
        return right + ".<" + join.bareElem().getSimpleName() + ">map(\"Type coercion\", "
                + param + " -> " + param + ".getValue())";
    }

    /**
     * facet extractBodyMultiDefaultTernary (v3.1 flip seat 32, law D2): the JOINED bare
     * element of a {@code default}, read from the LEFT operand's compiled stamp alone.
     *
     * <p>Upstream joins the two operands' meta-annotated types
     * ({@code joinMetaAnnotatedTypes}) and meta survives only when BOTH carry it, so a
     * meta-free RIGHT -- which the seat-28 law-5 rung above this arm has already established
     * for every carrier of this seat -- makes the join the LEFT's VALUE type. That is the
     * SAME read {@link #analyzeDefaultJoin} performs on its own mixed-join arm
     * ({@code metaWrap.getValueType()}), kept in one place so the stamp and the coercion
     * cannot disagree.
     *
     * <p>Reads RENDER TRUTH (the compiled stamp), never the inferred type -- the channel
     * PROBE28-F10b REFUTED at this seat. {@code null} (an absent stamp, an unresolvable
     * {@code Object} item -- the {@link #defaultOperandItemType} decline condition verbatim
     * -- or a wrapper with no value class) declines the whole arm -- byte-flat ONLY where the
     * Mapper-form ite slot ({@code ControlFlowHandler}, the seat-32 law D.2 rung) did not fire on
     * the same predicate; the two halves share the NECESSARY condition, not this decline.
     */
    private static JavaClass<?> defaultJoinBareElement(JavaStatementBuilder leftBuilder,
            JavaTypeUtil tu) {
        JavaType stamped = leftBuilder == null ? null : leftBuilder.getExpressionType();
        JavaType item = (tu == null || stamped == null) ? null : tu.getItemType(stamped);
        if (item == null || "Object".equals(item.getSimpleName())) {
            return null;
        }
        if (item instanceof RJavaWithMetaValue meta) {
            return meta.getValueType() instanceof JavaClass<?> value ? value : null;
        }
        return item instanceof JavaClass<?> bare ? bare : null;
    }

    /**
     * facet thenSeatMultiDefaultTernary (v3.1 flip seat 5): a default OPERAND's item
     * type, read through three decline-ordered channels: (1) the compiled builder's
     * STAMPED item (render truth — a nested default claimed by the seat arm stamps
     * {@code MapperC<join>}, so the outer's LEFT reads its inner's join); (2) a bare
     * alias-ref operand through {@link FunctionAliasHelper#inferShortcutMapperJavaType}
     * — the SAME walk that renders the alias method signature (the #178/#325/#345
     * same-walk law: {@code MapperC<? extends FieldWithMetaString> utiFromTrade}
     * and this read cannot disagree); (3) a with-args function-call operand through
     * the callee's DECLARED output ({@link NavigationHandler#metaWrapperOf} for a
     * {@code [metadata …]} output, else the resolved bare type — declared truth, the
     * #308 read). Everything else returns {@code null} — the join stays unproven and
     * the caller declines.
     */
    private static JavaType defaultOperandItemType(RExpression raw,
            JavaStatementBuilder builder, ExpressionCompiler compiler, RDefaultExpr expr) {
        JavaTypeUtil tu = compiler.getTypeUtil();
        if (tu != null && builder != null && builder.getExpressionType() != null) {
            JavaType item = tu.getItemType(builder.getExpressionType());
            if (item != null && !"Object".equals(item.getSimpleName())) {
                return item;
            }
        }
        if (raw instanceof RSymbolReference symRef && symRef.args().isEmpty()) {
            RShortcut shortcut = symRef.symbol()
                    .filter(RShortcut.class::isInstance)
                    .map(RShortcut.class::cast)
                    .orElse(null);
            RFunction enclosing = HandlerHelper.findEnclosingFunction(expr);
            if (shortcut != null && enclosing != null && tu != null
                    && compiler.getGeneratorModel() != null
                    && compiler.getTypeTranslator() != null) {
                JavaType mapper = new FunctionAliasHelper(compiler.getGeneratorModel(),
                        compiler.getTypeTranslator(), tu)
                        .inferShortcutMapperJavaType(shortcut, enclosing);
                if (mapper != null) {
                    return tu.getItemType(mapper);
                }
            }
        }
        if (raw instanceof RSymbolReference callRef && !callRef.args().isEmpty()) {
            RFunction callee = callRef.symbol()
                    .filter(RFunction.class::isInstance)
                    .map(RFunction.class::cast)
                    .orElse(null);
            RAttribute out = callee == null ? null : callee.output().orElse(null);
            if (out != null) {
                RJavaWithMetaValue metaWrap = NavigationHandler.metaWrapperOf(out, compiler);
                if (metaWrap != null) {
                    return metaWrap;
                }
                GeneratorModel gm = compiler.getGeneratorModel();
                JavaTypeTranslator translator = compiler.getTypeTranslator();
                if (gm != null && translator != null && out.typeCall() != null) {
                    RType outType = gm.resolveTypeCall(out.typeCall());
                    if (outType != null && !(outType instanceof RMissingType)) {
                        return translator.toJavaReferenceType(outType);
                    }
                }
            }
        }
        return null;
    }

    /**
     * facet defaultOpAliasJoin (PR #418): true when the default LEFT's workspace-inferred
     * type resolves THROUGH type aliases to a non-int number — upstream computes the join
     * on the RESOLVED type, so a {@code typeAlias MyNum: number} left joins as number
     * (oracle golden {@code expr-alias-default}: {@code getOrDefault(BigDecimal.valueOf(0))};
     * the #417 declared-name heuristic reads "MyNum" and declines). The alias strip mirrors
     * {@code FunctionExpressionRenderer.renderSwitchAssignment}'s idiom
     * ({@code getInferredType} + the {@code RAliasType.refersTo()} walk); ANY missing
     * model/workspace/inference state declines to the declared-name verdict (today's
     * bytes). Deliberately NOT folded into {@link HandlerHelper#inferRuneTypeName} — its
     * UNRESOLVED verdicts feed the arithmetic seat's legacy heuristic, where a global
     * alias-resolve would shift corpus-green Integer-vs-BigDecimal choices.
     */
    private static boolean leftResolvesToNumberType(RExpression left, ExpressionCompiler compiler) {
        GeneratorModel gm = compiler.getGeneratorModel();
        if (left == null || gm == null) {
            return false;
        }
        try {
            RType t = gm.workspace().getInferredType(left).type();
            t = HandlerHelper.stripAliases(t);
            return t instanceof RNumberType num && !num.isInteger();
        } catch (RuntimeException e) {
            // v3.1 C0: a REFUSAL is never recovery — it must reach the per-element
            // boundary (JavaClassGenerator), which attaches the target path and reports
            // it as a generation error. Locked by scripts/ci/refusal-propagation-lint.py.
            if (e instanceof SilentDegradation.Refusal __refusal) throw __refusal;
            return false;
        }
    }

    /**
     * facet getOrDefaultCondEnumArgHoist (PR #378, B): the SHARED admission predicate —
     * consulted by the producer below AND by
     * {@code CollectionHandler.thenChainHasUnhandledControlFlow}'s base walk (the #376-J
     * shared-predicate lockstep: the then-chain admission and the render read the SAME
     * gates, so they cannot disagree). True iff the {@code default} RIGHT is a
     * control-flow-free conditional ladder whose every value arm is a BARE
     * (unresolved / collision-mis-bound) name matching a value of the LEFT to-enum
     * conversion's TARGET enum — the shape whose ternary-as-getOrDefault-arg render
     * never compiled (zero goldens carry a conditional getOrDefault argument), so
     * every carrier is an already-waivered mismatch.
     */
    public static boolean getOrDefaultCondEnumArgHoistable(RDefaultExpr expr) {
        if (!(expr.rawRight() instanceof RConditionalExpr cond)) {
            return false;
        }
        if (!(expr.rawLeft() instanceof com.regnosys.rosetta.ast.expressions.unary.RConversionExpr conv)
                || conv.kind() != com.regnosys.rosetta.ast.enums.ConversionKind.ENUM) {
            return false;
        }
        REnumeration target = conv.targetEnum().orElse(null);
        if (target == null) {
            return false;
        }
        if (CollectionHandler.subtreeHasControlFlow(conv)
                || CollectionHandler.condLadderHasControlFlow(cond)) {
            return false;
        }
        return condArmsAllBareTargetEnum(cond, target);
    }

    /**
     * facet getOrDefaultCondEnumArgHoist (PR #378, B): every VALUE arm of the ladder
     * (nested then/else conditionals folding into the same local) is a bare
     * target-enum value; an absent / {@code empty} else is fine (the initializer
     * form's synthesized null).
     */
    private static boolean condArmsAllBareTargetEnum(RConditionalExpr cond, REnumeration target) {
        RExpression then = cond.thenBranch();
        if (then instanceof RConditionalExpr nestedThen) {
            if (!condArmsAllBareTargetEnum(nestedThen, target)) {
                return false;
            }
        } else if (bareTargetEnumValueOrNull(then, target) == null) {
            return false;
        }
        RExpression els = cond.elseBranch().orElse(null);
        if (els == null || CollectionHandler.isEmptyLiteral(els)) {
            return true;
        }
        if (els instanceof RConditionalExpr nestedElse) {
            return condArmsAllBareTargetEnum(nestedElse, target);
        }
        return bareTargetEnumValueOrNull(els, target) != null;
    }

    /**
     * facet getOrDefaultCondEnumArgHoist (PR #378, B): the {@code tryBareEnumArg} gate
     * at the conditional-arm seat — a bare args-empty {@link RSymbolReference} that is
     * UNRESOLVED (type-directed resolution does not fire in arm position) or
     * false-resolved to a Java-representation-less declaration (the #143 RBody /
     * #368 RDataType / #376-M4 RCorpus collision classes), whose name matches a value
     * in the target enum's hierarchy (the #358 Java-enum flatten law). Any other
     * shape declines (the inline ternary stays — today's bytes).
     *
     * <p>facet cat16BindEnumSeats (PR #452): the engine's Category-16 BIND now stamps
     * exactly this arm shape with the position enum's {@link REnumValue}
     * ({@code symbol()} PRESENT — golden cdm6 MapUnitTypeWithScheme's
     * {@code IndexUnit} arm), which the pre-#452 present-symbol gate would decline —
     * collapsing the WHOLE hoist route (this producer AND
     * {@code CollectionHandler.thenChainHasUnhandledControlFlow}'s base walk, the
     * #376-J shared-predicate lockstep) to the inline-ternary restructure. Admit the
     * stamped value under the #215 SAME-INSTANCE descend-only law: the
     * hierarchy-found value must BE the bound instance, so a same-name value bound
     * on an UNRELATED enum still declines and every unresolved/collision admission
     * is byte-untouched.
     */
    private static REnumValue bareTargetEnumValueOrNull(RExpression arm, REnumeration target) {
        if (!(arm instanceof RSymbolReference bareRef) || !bareRef.args().isEmpty()) {
            return null;
        }
        if (bareRef.symbol().isPresent()
                && !(bareRef.symbol().get() instanceof com.regnosys.rosetta.ast.regulatory.RBody)
                && !(bareRef.symbol().get() instanceof com.regnosys.rosetta.ast.types.RDataType)
                && !(bareRef.symbol().get()
                        instanceof com.regnosys.rosetta.ast.regulatory.RCorpus)
                && !(bareRef.symbol().get() instanceof REnumValue)) {
            return null;
        }
        String valueName = bareRef.name();
        if (valueName == null || valueName.isEmpty()
                || !Character.isUpperCase(valueName.charAt(0))) {
            return null;
        }
        REnumValue found = HandlerHelper.findEnumValueInHierarchy(target, valueName);
        if (found != null
                && bareRef.symbol().isPresent()
                && bareRef.symbol().get() instanceof REnumValue bound
                && found != bound) {
            // The bind-stamped rung's same-instance gate (PR #452, the #215 law):
            // the target hierarchy's flattened value must BE the bound value.
            return null;
        }
        return found;
    }

    /**
     * facet getOrDefaultCondEnumArgHoist (PR #378, B): the producer — upstream's
     * "every mid-expression consumption of a compiled conditional collapses via
     * {@code declareAsVariable(true, "ifThenElseResult")}" law (the #181 seat list:
     * method args, {@code JavaStatementBuilder.invokeMethod}) applied at the
     * {@code getOrDefault} ARGUMENT seat, where the #181 arm declined on the
     * untypable bare-enum then-arm. The ladder rides
     * {@link ControlFlowHandler#buildIfThenElseHoistBlock} (initializer form for the
     * no-value else — golden cdm6 MapUnitTypeWithScheme
     * {@code FinancialUnitEnum ifThenElseResult = null;}; blank-final general form
     * for the effective else — golden cdm6 MapEntityIdentifierTypeEnum — with the
     * #217 top-condition bool hoist for free), the arms render as the QUALIFIED
     * dotted constants of the to-enum TARGET (the same
     * collision-sentinel-vs-simple-name law as the left's {@code checkedMap} render),
     * and the whole result wraps {@code MapperS.of} WITH the unwrap contract (the
     * #234 dual-consumer law: a Mapper consumer keeps the wrap — the alias then-base
     * {@code thenArg} decl; a bare consumer strips to
     * {@code <left>.getOrDefault(ifThenElseResult)} — the assignment seat, golden's
     * dropped {@code .get()}). Declines (null → the caller's existing arms, byte-
     * unchanged): no sink (rule/alias-legacy/lambda-interior paths), any gate of the
     * shared predicate above.
     */
    private JavaExpression tryGetOrDefaultCondEnumArgHoist(RDefaultExpr expr,
            ExpressionContext ctx, ExpressionCompiler compiler, JavaStatementBuilder leftBuilder) {
        if (!getOrDefaultCondEnumArgHoistable(expr)) {
            return null;
        }
        com.regnosys.rosetta.generator.java.scoping.JavaStatementScope sink =
                ctx.scope() == null ? null : ctx.scope().findStatementHoistSink();
        if (sink == null) {
            return null;
        }
        RConditionalExpr cond = (RConditionalExpr) expr.rawRight();
        var conv = (com.regnosys.rosetta.ast.expressions.unary.RConversionExpr) expr.rawLeft();
        REnumeration target = conv.targetEnum().orElseThrow();
        Set<JavaClass<?>> refs = new HashSet<>(leftBuilder.getRefs());
        Set<JavaClass<?>> wildcards = new HashSet<>(leftBuilder.getStaticWildcardImports());
        String left = HandlerHelper.wrapSelfUnwrappingGetOperand(
                HandlerHelper.render(leftBuilder), leftBuilder, refs);
        // The target enum's rendered name — the SAME collision law as the left's
        // checkedMap witness (the #348 claims sentinel on collision, the #215 simple
        // name + import otherwise); the ref registers unconditionally (the resolver
        // suppresses losers).
        String collisionFqn = ConversionHandler.enumTargetCollisionFqn(conv, compiler);
        String enumTypeRef = collisionFqn != null
                ? com.regnosys.rosetta.generator.java.template.ImportCollisionResolver
                        .typeRef(collisionFqn)
                : ConversionHandler.targetEnumSimpleName(conv, compiler);
        JavaTypeTranslator translator = compiler.getTypeTranslator();
        if (translator != null) {
            refs.add(translator.toJavaReferenceType(new REnumTypeRef(target)));
        }
        String sentinel = sink.statementHoistSession()
                .register(com.regnosys.rosetta.generator.java.function.StatementHoistSession
                        .IF_THEN_ELSE_RESULT);
        ControlFlowHandler.HoistArmRenderer arms = new ControlFlowHandler.HoistArmRenderer() {
            @Override
            public String renderArm(RExpression arm) {
                // Pre-gated by the shared predicate: every reachable arm is a bare
                // target-enum value.
                REnumValue v = bareTargetEnumValueOrNull(arm, target);
                return enumTypeRef + "." + EnumHelper.convertValue(v);
            }

            @Override
            public String emptyElseValue() {
                return "null";
            }
        };
        String block = ControlFlowHandler.buildIfThenElseHoistBlock(
                cond, sentinel, enumTypeRef, compiler, ctx.scope(), arms, refs, wildcards);
        sink.registerStatementHoist(block);
        JavaExpression core = JavaExpression.from(
                left + ".getOrDefault(" + sentinel + ")",
                null,
                refs,
                wildcards);
        return JavaExpression.wrappedInMapperSOf(core);
    }

    /**
     * facet defaultRightNestedThenHoist (v3.1 flip seat 30, law 4, rung 2): the ITEM type
     * of the {@code thenArg} binding behind a HOISTED then-chain {@code default} RIGHT
     * whose LAST body is a COLLAPSING list-op ({@code first}/{@code last}/
     * {@code only-element}). This is the #363 gate's first half, EXTRACTED so the META
     * deref ({@link #tryThenBoundMetaArgDeref}) and the BARE-item collapse (the arm at the
     * {@code getOrDefault} seat) read ONE copy and cannot drift apart (LAW 69 - they are
     * the two halves of one gate, separated only by whether the bound item is an
     * {@link RJavaWithMetaValue}). The collapse test now goes through
     * {@link #isCollapsingListOp}, the file's own single definition of that set, instead
     * of the inline triple it duplicated.
     *
     * <p>Returns {@code null} - both callers keep the legacy render - when the right is
     * not a then chain, its last body is not a collapse, or <b>the chain did not
     * HOIST</b>: {@code thenArgRefFor} is the #350 bindThenArg channel, populated by the
     * hoist, so a chain that declined to the inline runtime {@code .then(...)} form has no
     * binding at all. That is what makes rung 2 dormant until rung 1 lands.
     */
    private static JavaType thenBoundCollapseItem(RDefaultExpr expr,
            ExpressionCompiler compiler, ExpressionContext ctx) {
        if (!(expr.rawRight() instanceof RThenExpr rightThen) || compiler == null
                || compiler.getTypeUtil() == null || ctx == null || ctx.scope() == null) {
            return null;
        }
        RInlineFunction thenFn = rightThen.body().orElse(null);
        if (thenFn == null || thenFn.body() == null) {
            return null;
        }
        if (!(thenFn.body() instanceof RListOpExpr lop) || !isCollapsingListOp(lop)) {
            return null;
        }
        // facet thenBindingOverlay (v3.1 C2d retirement family 9, PR #616) — VERDICT-MOVED
        // RETIRE-AFTER-CENSUS -> JUSTIFIED-KEPT (the seat-30 bar; S36): NOT dormant — the javadoc's
        // own expectation above is REFUTED — and the #613 channel answers at only 12 of 63. c9
        // census, 894 arrivals (298 / 298 / 298) = 831 entry + 63 read (21 per walk):
        // rightIsThen=true at 150 of the entries and bodyIsCollapse=true at 63/63 of the reads, so
        // the seat REACHES ITS READ ON EVERY WALK and "rung 2 dormant until rung 1 lands" is
        // measurably false. bound=true, boundTypeNull=false and boundKind=C+w at 63/63. THE
        // CHANNEL, NavigationHandler.recoverExprMetaWrapper, answers a wrapper at 12 and null at 51
        // — agreement 12/63 (19.05%), T/F 51, F/T 0, a one-directional UNDER-fire that cannot
        // supply the item at 81% of arrivals and cannot supply the PLAIN items at all. Only the
        // TYPE half was ever retirable; the null-binding case IS the "chain did not hoist" gate.
        // Locator drift corrected: the triage's L1742-1746 is live at 1817-1821.
        JavaExpression binding = ctx.scope().thenArgRefFor(thenFn);
        if (binding == null || binding.getExpressionType() == null) {
            return null;
        }
        return compiler.getTypeUtil().getItemType(binding.getExpressionType());
    }

    /**
     * facet getOrDefaultArgMetaDeref (PR #363): the then-BOUND meta-collapse deref at
     * the {@code getOrDefault} argument seat — see the call-site comment for the law.
     * Returns {@code null} (caller keeps the legacy render) unless EVERY render-truth
     * gate passes: the right is an {@link RThenExpr} whose LAST body's
     * {@code thenArgRefFor} binding carries a Mapper whose ITEM is an
     * {@link RJavaWithMetaValue}, the body root is a collapsing list-op
     * (first/last/only-element — the single-item witness), a statement-hoist sink is
     * reachable, and the LEFT's compiled item is EITHER not a wrapper at all (the #363
     * shape) OR a DIFFERENT wrapper class over the SAME value type (facet
     * defaultJoinHeteroMetaDerefBoth, v3.1 flip seat 33 law B.2 — the join annihilates the
     * annotation and the LEFT derefs too). A same-wrapper (homogeneous-meta) LEFT still
     * returns {@code null}: golden keeps the wrapper there.
     */
    private static JavaExpression tryThenBoundMetaArgDeref(RDefaultExpr expr,
            JavaStatementBuilder rightBuilder, String left, JavaStatementBuilder leftBuilder,
            ExpressionCompiler compiler, ExpressionContext ctx,
            Set<JavaClass<?>> refs, Set<JavaClass<?>> wildcards) {
        JavaType bindingItem = thenBoundCollapseItem(expr, compiler, ctx);
        if (!(bindingItem instanceof RJavaWithMetaValue wrapper)) {
            return null;
        }
        // facet defaultJoinHeteroMetaDerefBoth (v3.1 flip seat 33, law B.2 of B.24): a
        // HETEROGENEOUS-meta `default` — the LEFT's compiled item and the RIGHT's bound
        // collapse item are DIFFERENT wrapper classes over the SAME value type — joins at the
        // BARE value and BOTH sides deref. Upstream's own rule, not a corpus heuristic:
        // ExpressionGenerator.xtend L452-458 compiles both operands at
        // joinMetaAnnotatedTypes(left, right) and meta survives the join ONLY when both
        // operands carry it, so two different annotations over one value type annihilate.
        // Golden drr 7.x QuantityUnitOfMeasure:69 `MapperS.of(thenArg3.<NonNegative
        // QuantitySchedule>map("Type coercion", referenceWithMetaNonNegativeQuantitySchedule
        // -> … == null ? null : ….getValue()).getOrDefault((fieldWithMetaNonNegative
        // QuantitySchedule0 == null ? null : ….getValue())))` against the fork's
        // `thenArg3.getOrDefault(<inline chain>)` typed `MapperS<FieldWithMetaNonNegative
        // QuantitySchedule>` — a NonNegativeQuantitySchedule into a FieldWithMeta…-typed
        // slot, non-compiling (LAW 74, javac33 C7 line 485), so every carrier is already
        // waivered.
        //
        // The HOMOGENEOUS-meta default is UNCHANGED and that is the load-bearing half: golden
        // KEEPS the wrapper when both operands carry the same annotation (drr MessageID, 9 GREEN
        // cells, FieldWithMetaString both sides; rule:QuantitySchedule, 20 GREEN rows;
        // GetBasketConstituents:173, the same class), so the disjunct requires the wrapper
        // CLASSES to DIFFER — canonical names, not simple names (the #349 import-collision
        // law) — AND their value types to be EQUAL. Every same-wrapper row returns null on
        // exactly the line it returns null on today.
        //
        // The LEFT deref is the SAME coerceNavigationReceiver call the :450 arm already makes for
        // the meta-LEFT-over-bare-RIGHT case (facet defaultMetaJoinFromRender, seat 28 law 5 as
        // amended by seat-32 law D.1), so the two heterogeneous shapes cannot render the left
        // differently; it runs only after the sink is proven, so a declining path leaves the
        // scope's naming state untouched. The slot type follows from render truth without a
        // second law: the deref'd left publishes `.<Value>map("Type coercion", …)` +
        // `.getValue()).getOrDefault(`, which is exactly what FunctionExpressionRenderer's
        // blockArmDerefsToBareLeaf test (#391, FER:6828-6833) reads to KEEP the bare inferred
        // element on the then-arg declaration.
        JavaType leftType = leftBuilder.getExpressionType();
        JavaType leftItem = leftType == null ? null
                : compiler.getTypeUtil().getItemType(leftType);
        boolean heteroMetaJoin = false;
        if (leftItem instanceof RJavaWithMetaValue leftWrapper) {
            heteroMetaJoin = leftWrapper instanceof JavaClass<?> leftWrapperClass
                    && wrapper instanceof JavaClass<?> rightWrapperClass
                    && !leftWrapperClass.getCanonicalName().withDots()
                            .equals(rightWrapperClass.getCanonicalName().withDots())
                    && leftWrapper.getValueType() instanceof JavaClass<?> leftValueClass
                    && wrapper.getValueType() instanceof JavaClass<?> rightValueClass
                    && leftValueClass.getCanonicalName().withDots()
                            .equals(rightValueClass.getCanonicalName().withDots());
            if (!heteroMetaJoin) {
                return null;
            }
        }
        com.regnosys.rosetta.generator.java.scoping.JavaStatementScope sink =
                ctx.scope().findStatementHoistSink();
        if (sink == null) {
            return null;
        }
        String joinLeft = left;
        if (heteroMetaJoin) {
            // The LEFT deref is the SAME renderer law C.2's heteroDerefArm uses (LAW 69: one
            // guarded, numbered "Type coercion" deref for every heterogeneous join) - the
            // drafted coerceNavigationReceiver call rendered the UNGUARDED `x -> x.getValue()`
            // form where golden QuantityUnitOfMeasure:69 carries `x -> x == null ? null :
            // x.getValue()` (measured B24-green2).
            // The SINGLE (MapperS) left takes upstream's null-SAFE guarded form - the deferred
            // numbered coercion param law C.1's rung R4 mints (`cp -> cp == null ? null :
            // cp.getValue()`), NOT the MULTI mapC form C.2's heteroDerefArm renders for
            // Price's MapperC join (`x -> x.getValue()`, golden Price:123). Golden
            // QuantityUnitOfMeasure:69 is the guarded form; B24-green5 measured the unguarded
            // one off by exactly that guard.
            RJavaWithMetaValue leftWrapperS = (RJavaWithMetaValue) leftItem;
            JavaClass<?> leftValueS = (JavaClass<?>) leftWrapperS.getValueType();
            String cpL = ctx.scope().registerDeferredCoercionParam(
                    com.regnosys.rosetta.generator.java.JavaNamingUtil.toFirstLower(
                            leftWrapperS.getSimpleName()));
            refs.add(leftValueS);
            joinLeft = left + ".<" + leftValueS.getSimpleName() + ">map(\"Type coercion\", "
                    + cpL + " -> " + cpL + " == null ? null : " + cpL + ".getValue())";
        }
        String wrapperSimple = wrapper.getSimpleName();
        String sentinel = sink.statementHoistSession().register(
                com.regnosys.rosetta.generator.java.JavaNamingUtil.toFirstLower(wrapperSimple));
        sink.registerStatementHoist("final " + wrapperSimple + " " + sentinel + " = "
                + HandlerHelper.render(rightBuilder) + ".get();");
        refs.add(wrapper);
        JavaExpression core = JavaExpression.from(
                joinLeft + ".getOrDefault((" + sentinel + " == null ? null : "
                        + sentinel + ".getValue()))",
                null,
                refs,
                wildcards);
        // facet defaultJoinHeteroMetaDerefBoth (law B.24 / B.2, measured at QUOM:69): the
        // heterogeneous join's consumer is a then-LEVEL declaration (`final MapperS<X>
        // thenArgN = ...`), and golden re-wraps the item-valued getOrDefault as
        // `MapperS.of(...)` - the seat-30 rung-2 / #376 wrappedInMapperSOf action - where the
        // #363 bare-left carriers (a `set` consumer, no `.get()` appended) keep the
        // selfUnwrapping marker. B24-green3 measured the marker alone leaving the wrap off.
        return heteroMetaJoin ? JavaExpression.wrappedInMapperSOf(core)
                : JavaExpression.selfUnwrapping(core);
    }

    /**
     * facet getOrDefaultMetaJoin (PR #296): {@code true} iff this {@link RDefaultExpr} is a
     * MIXED-baresym {@code default} — the LEFT (defaulted) operand is a disguised baresym
     * function-nav (an {@link REnumValueRef} whose {@code resolvedSymbol} is an {@code RFunction})
     * whose leaf is META-annotated, AND the RIGHT (default) operand is a baresym BARE-leaf nav
     * (which forces the bare common type so golden derefs the meta LEFT). Detected via
     * {@link ReferenceHandler#baresymFunctionLeafMetaKind} — the SAME leaf resolution as the #280
     * meta-leaf gate, so this pre-scan and that gate cannot disagree. A homogeneous-meta default
     * (both operands meta — golden keeps the wrapper), a bare LEFT, or a non-baresym operand
     * (a {@code null} kind) returns {@code false}: no flag, no deref, byte-unchanged.
     */
    private static boolean isMixedBaresymDefault(RDefaultExpr expr) {
        RExpression left = expr.rawLeft();
        RExpression right = expr.rawRight();
        if (left == null || right == null) {
            return false;
        }
        MetaFieldGenerator.MetaKind leftKind = ReferenceHandler.baresymFunctionLeafMetaKind(left);
        MetaFieldGenerator.MetaKind rightKind = ReferenceHandler.baresymFunctionLeafMetaKind(right);
        boolean leftMetaLeaf = leftKind != null && leftKind != MetaFieldGenerator.MetaKind.NONE;
        boolean rightBareLeaf = rightKind == MetaFieldGenerator.MetaKind.NONE; // non-null baresym + bare leaf
        return leftMetaLeaf && rightBareLeaf;
    }

    /**
     * facet bareEnumDefaultArg (PR #239): the qualified Java enum constant {@code EnumName.CONSTANT}
     * for a BARE enum-value {@code default} RIGHT operand, registering the enum import on {@code refs},
     * or {@code null} when the right operand is not a bare enum or the default's type is not an enum.
     *
     * <p>The bare value is either a STRICT bare reference ({@link HandlerHelper#bareEnumValueName} — an
     * unresolved {@link RSymbolReference} like {@code Name}, or a disguised {@link REnumValueRef}), a
     * TYPE-SHADOWED value ({@link HandlerHelper#typeShadowEnumValueName} — a name colliding with a model
     * type), or (facet cat16BindEnumSeats, PR #452) a BIND-STAMPED value
     * ({@link HandlerHelper#boundBareEnumValue} — the engine's Category-16 expected-type bind resolved
     * the name to the position enum's {@link REnumValue}). The target enum is the
     * {@code RDefaultExpr}'s OWN joined inferred type for the unresolved/shadow rungs; the BOUND rung
     * derives it from the LEFT operand's actual type instead (the engine walker's own derivation —
     * the join would widen to the declaring PARENT for an inherited value) and additionally requires
     * the #215 same-instance match. The
     * {@link HandlerHelper#findEnumValueInHierarchy} match (which walks the {@code extends} chain —
     * {@code AssetIdTypeEnum.Name} is inherited from {@code ProductIdTypeEnum}) is the load-bearing
     * gate: a resolved enum-typed variable / non-enum default declines, so a real reference is never
     * rewritten. Regression-safe: the un-prefixed bare value is a non-compiling undefined symbol, so
     * every carrier is already a waivered mismatch.
     */
    private String tryDefaultBareEnumRightOrNull(RDefaultExpr expr, ExpressionCompiler compiler,
            Set<JavaClass<?>> refs) {
        RExpression right = expr.rawRight();
        String valueName = HandlerHelper.bareEnumValueName(right);
        if (valueName == null) {
            valueName = HandlerHelper.typeShadowEnumValueName(right);
        }
        // facet cat16BindEnumSeats (PR #452): the third rung — the engine's
        // Category-16 BIND stamps the bare RIGHT with the position enum's
        // REnumValue (symbol PRESENT, so both rungs above correctly decline),
        // and the resolved rendering — before v3.1 flip seat 12 — qualified by
        // the DECLARING parent (since seat 12 the root arm reads the same
        // inferred type this rung reads, HandlerHelper.boundEnumInferredOwner —
        // the two agree by construction and the parent-import swap below is a
        // no-op) where golden qualifies by the LEFT's child enum (golden cdm6
        // MapIndexIdToAssetIdentifier `.getOrDefault(AssetIdTypeEnum.NAME)`,
        // NAME declared on ProductIdTypeEnum — the #211/#358 flatten law).
        REnumValue bound = null;
        if (valueName == null) {
            bound = HandlerHelper.boundBareEnumValue(right);
            if (bound != null) {
                valueName = bound.name();
            }
        }
        if (valueName == null) {
            return null;
        }
        GeneratorModel gm = compiler.getGeneratorModel();
        JavaTypeTranslator translator = compiler.getTypeTranslator();
        if (gm == null || translator == null) {
            return null;
        }
        REnumeration en;
        if (bound != null) {
            // The BOUND rung derives the qualifier from the LEFT operand's
            // ACTUAL type — the SAME derivation the engine's position walker
            // bound against (expectedEnumViaPosition's default-RHS arm =
            // enumOfBranch(rawLeft); upstream caseDefaultOperation). The
            // default expr's OWN joined type would widen to the declaring
            // PARENT for an inherited value and mis-qualify.
            RExpression left = expr.rawLeft();
            if (left == null) {
                return null;
            }
            RMetaAnnotatedType leftType = gm.workspace().getInferredType(left);
            if (leftType == null || leftType.isMissing()
                    || !(leftType.type() instanceof REnumTypeRef leftEnumRef)) {
                return null;
            }
            en = leftEnumRef.astNode();
        } else {
            RMetaAnnotatedType inferred = gm.workspace().getInferredType(expr);
            if (inferred == null || inferred.isMissing() || !(inferred.type() instanceof REnumTypeRef enumRef)) {
                return null;
            }
            en = enumRef.astNode();
        }
        REnumValue match = HandlerHelper.findEnumValueInHierarchy(en, valueName);
        if (match == null || (bound != null && match != bound)) {
            // The bound rung's #215 same-instance descend-only gate: the
            // qualifying enum's hierarchy must flatten the EXACT bound value.
            return null;
        }
        if (bound != null && bound.parent() instanceof REnumeration declaring) {
            // The discarded resolved render (compiled at the caller before the
            // arms run) contributed the DECLARING enum's import to refs; the
            // child supersedes it — the #215 swap. An own-declared bound value
            // nets a no-op (remove-then-add of the same class).
            JavaClass<?> parentClass = translator.toJavaReferenceType(new REnumTypeRef(declaring));
            if (parentClass != null) {
                refs.remove(parentClass);
            }
        }
        JavaClass<?> enumClass = translator.toJavaReferenceType(new REnumTypeRef(en));
        if (enumClass != null) {
            refs.add(enumClass);
        }
        return en.name() + "." + EnumHelper.convertValue(match);
    }

    /**
     * facet thenBodyDefaultCollapseFold (PR #367): a collapsing list-op — the
     * single-item-producing {@code first}/{@code last}/{@code only-element}
     * class (the #363 "collapsing body root" set, here as the default-operand
     * shape gate).
     */
    private static boolean isCollapsingListOp(RListOpExpr lop) {
        return lop.op() == ListOp.FIRST || lop.op() == ListOp.LAST
                || lop.op() == ListOp.ONLY_ELEMENT;
    }

    /**
     * facet defaultChainOperandUnwrap (PR #234): reduce a single-cardinality
     * {@code default} right operand to its BARE item value, or return {@code null} to
     * keep the legacy rendering (a strict superset of the prior behaviour — declines
     * are byte-identical).
     *
     * <ul>
     *   <li>a navigation Mapper chain (an {@link RFeatureCall} or a disguised 2-name
     *       {@link REnumValueRef} {@code head -> feature} chain) whose LEAF feature is
     *       single-cardinality → append {@code .get()} (the coercion-equivalent unwrap
     *       the dormant null-type coercion skips). A MULTI right leaf DECLINES — a multi
     *       mapper ({@code MapperC}) has no single item to {@code .get()}-unwrap (golden
     *       reduces a multi-ish right via a {@code .first().get()} / element-collapse
     *       shape, not a bare {@code .get()}), so declining yields a missed flip, never a
     *       wrong byte. ({@link NavigationHandler#navLeafFeatureMulti} is the same
     *       {@code mapC}-grade resolution the navigation render uses, so this RIGHT-operand
     *       cardinality cannot disagree with the emitted mapper tail. NB this is the
     *       right-operand leaf cardinality — distinct from upstream's {@code left.isMulti}
     *       gate, which selects the whole {@code getMulti().isEmpty()} conditional form
     *       over {@code getOrDefault}, a separate left-cardinality facet not handled here);</li>
     *   <li>a structurally-unwrappable {@code MapperS.of(value)} wrap (a bare param /
     *       variable) whose default-expression type RESOLVES → the bare inner. The
     *       resolvable-type gate excludes the multi {@code MapperC.<T>of(value)}
     *       single-wrap (whose inferred type is {@code MISSING}), which shares the
     *       {@code unwrapToBuilder} contract but must not be unwrapped into a single
     *       {@code getOrDefault}. The whole-{@code RDefaultExpr} inferred type is a safe
     *       discriminator here: the {@code default}'s joined type resolving to a concrete
     *       single type implies the single-wrap shape (a multi {@code MapperC} operand
     *       leaves the join {@code MISSING} in this fpml-ingest tail);</li>
     *   <li>an ALIAS-call right (a linker-resolved {@code RShortcut} symbol ref) whose
     *       same-walk signature proves {@code MapperS} with a non-meta item → append
     *       {@code .get()} (facet defaultAliasArgDeref, PR #352 — see the arm comment);</li>
     *   <li>otherwise (a function {@code .evaluate()} value call, a conditional, a bare
     *       symbol already rendered bare, a {@code then} chain) → {@code null}.</li>
     * </ul>
     */
    private JavaStatementBuilder reduceDefaultRightToBare(RDefaultExpr expr,
                                                          JavaStatementBuilder rightBuilder,
                                                          ExpressionCompiler compiler) {
        RExpression rightNode = expr.rawRight();
        if (rightNode == null) {
            return null;
        }
        // facet sortKeyDefaultArgCollapse (PR #345): a bare RE-ROOTED feature RIGHT
        // (`… default adjustedPrincipalExchangeDate` inside an item lambda — an
        // empty-symbol OR foreign-RAttribute-bound RSymbolReference the renderer
        // re-roots to `item.<ZonedDateTime>map(…)`, a Mapper) joins the nav-chain
        // arm: the T-typed getOrDefault overload needs the BARE item, so the Mapper
        // splice never compiled (green-safe by construction — zero goldens pass a
        // Mapper to getOrDefault). Golden MapPrincipalPaymentSchedule cdm6:
        // `.sort(item -> MapperS.of(item.<ZonedDateTime>map(…).getOrDefault(
        // item.<ZonedDateTime>map(…).get())))` — the reduction ALSO restores the
        // #234 MapperS.of sort-key wrap. An ENCLOSING-FUNCTION param/output ref
        // (`getOrDefault(fpmlStrikePercentage)` — the render is the strippable
        // MapperS.of(<name>), golden keeps the BARE name; the cp4 over-fire catch:
        // MapOptionStrikePrice + 2 green siblings) is EXCLUDED — it keeps the
        // unwrap-contract strip arm below, exactly the pre-#345 bytes.
        // facet lolDefaultBodyMulti (v3.1 flip seat 33, law A.4, rung 3): the BARE PIPED
        // ITEM is a `getOrDefault` argument like every other nav right. `Mapper.getOrDefault(T)`
        // takes the ITEM (rune-runtime mapper/Mapper.java:26), so the Mapper the item renders
        // as must collapse `.get()`. GOLDEN <- FORK: golden GetBasketConstituents
        // `.getOrDefault(item.get())` against the fork's `.getOrDefault(item)`. This method's
        // own javadoc already states the law - "golden NEVER keeps a Mapper as a getOrDefault
        // argument - the right is always the bare item" - the RImplicitVariable right was
        // simply not in the shape set.
        //
        // MEASURED (LAW 75, [P33-DEFRIGHT], route-identical): over the whole fall-through
        // population the `rightKind=RImplicitVariable` rows are 4 / 1 `where=` = the carrier
        // (`rightRender=item`, `leftKind=RSymbolReference`), and the frozen baseline agrees -
        // `.getOrDefault(item)` = 0 of 174,141 goldens, `.getOrDefault(item.get())` = 4 = the
        // carriers; `default item` occurs in ONE source-file family of the 4,062-file .rosetta
        // corpus (base-trade-basket-func.rosetta x drr 7.0-7.3). A NEW DISJUNCT here, not a
        // widening of the nested-then (:918-929) or min/max-collapse (:961-972) arms.
        // `navLeafFeatureMulti` answers false for an implicit variable, so the arm's MULTI
        // decline below is a structural no-op for this shape.
        // LAW 74: javac33 PRE 526 (`MapperC<AssetIdentifier> cannot be converted to
        // AssetIdentifier`) -> 0.
        boolean navChain = rightNode instanceof RFeatureCall
                || rightNode
                        instanceof com.regnosys.rosetta.ast.expressions.references.RImplicitVariable
                || (rightNode instanceof REnumValueRef evr && evr.enumeration().isEmpty())
                || (rightNode instanceof RSymbolReference bareSr
                        && bareSr.args().isEmpty()
                        && (bareSr.symbol().isEmpty()
                            || (bareSr.symbol().filter(RAttribute.class::isInstance).isPresent()
                                && !isEnclosingFunctionParam(bareSr))));
        if (navChain) {
            if (NavigationHandler.navLeafFeatureMulti(rightNode, compiler)) {
                return null;
            }
            return JavaExpression.from(
                    HandlerHelper.render(rightBuilder) + ".get()",
                    null,
                    rightBuilder.getRefs(),
                    rightBuilder.getStaticWildcardImports());
        }
        // facet defaultAliasArgDeref (PR #352): an ALIAS-call RIGHT operand (a
        // linker-resolved RShortcut symbol ref — renders `aliasName(args)`, a Mapper)
        // reduces via `.get()` when the SAME walk that renders the alias method's
        // signature (NavigationHandler.tryAliasReceiverMapperType →
        // inferShortcutMapperJavaType) proves MapperS with a NON-meta item — the
        // single-item unwrap the nav-chain arm above applies, by the same #234 law
        // (golden NEVER keeps a Mapper as a getOrDefault argument). Golden
        // MapBuyerSellerToAccountPartyReference cdm: `.setExternalReference(
        // buyerPartyReference(…).getOrDefault(sellerPartyReference(…).get()))` — the
        // wrappedInMapperSOf contract below strips at the setter seat exactly like the
        // nav-chain reduction. A MapperC-signatured alias (no single item), a META item
        // (the join would deref the wrapper to its VALUE — `.get()` alone yields the
        // wrapper), or a walk decline keeps the legacy render byte-for-byte.
        // facet thenBodyDefaultCollapseFold (PR #367): a RIGHT that is a COLLAPSING
        // list-op (first/last/only-element — the single-item-producing #363 class)
        // over a non-meta attribute reduces via `.get()` — the rendered right is a
        // single-item Mapper (`MapperC.<ProductIdentifier>of(productIdentifiers)
        // .first()`), so the unwrap is the same #234 law as the nav-chain arm above.
        // The standard wrappedInMapperSOf contract then serves BOTH consumer seats:
        // the #267 then-output arm keeps the wrap + appends `.get()` (golden drr
        // ExtractProductIdentifierBySource `productIdentifier = toBuilder(MapperS.of(
        // thenArg.first().getOrDefault(MapperC.<ProductIdentifier>of(
        // productIdentifiers).first().get())).get());`), a bare setter strips.
        // Meta-item roots decline (`.get()` alone would yield the wrapper — the
        // #363 hoist route owns those).
        if (rightNode instanceof RListOpExpr rightLop
                && isCollapsingListOp(rightLop)
                && rightLop.argument() instanceof RSymbolReference rightRoot
                && rightRoot.symbol().filter(RAttribute.class::isInstance)
                        .map(RAttribute.class::cast)
                        .filter(a -> MetaFieldGenerator.detectMetaKind(a)
                                == MetaFieldGenerator.MetaKind.NONE)
                        .isPresent()) {
            return JavaExpression.from(
                    HandlerHelper.render(rightBuilder) + ".get()",
                    null,
                    rightBuilder.getRefs(),
                    rightBuilder.getStaticWildcardImports());
        }
        if (rightNode instanceof RSymbolReference aliasRef
                && aliasRef.symbol().filter(RShortcut.class::isInstance).isPresent()) {
            JavaTypeUtil tu = compiler.getTypeUtil();
            JavaType aliasType = NavigationHandler.tryAliasReceiverMapperType(rightNode, compiler);
            if (tu != null && aliasType != null && tu.isMapperS(aliasType)
                    && !(tu.getItemType(aliasType) instanceof RJavaWithMetaValue)) {
                return JavaExpression.from(
                        HandlerHelper.render(rightBuilder) + ".get()",
                        null,
                        rightBuilder.getRefs(),
                        rightBuilder.getStaticWildcardImports());
            }
            // facet defaultAliasArgDeref (PR #367 widening): the type walk above
            // resolves meta and basic items only — an ENUM/model-item alias declines
            // it (the #352 carry). A DIRECT fn-call alias body whose called output is
            // SINGLE + non-meta proves the same MapperS single item from the SAME
            // fn-output resolution the alias method's signature renders
            // (MapperS<TradeIdentifierTypeEnum>), so the `.get()` reduction cannot
            // disagree with the rendered wrapper. Golden MapPartyTradeIdentifier:
            // `.setIdentifierType(identifierTypeForTrade(…).getOrDefault(
            // identifierTypeForIssuer(…).get()))`. Meta items keep the decline (the
            // wrapper's `.get()` yields the wrapper, not the value).
            if (NavigationHandler.aliasSingleNonMetaFnCallOutput(rightNode, compiler)) {
                return JavaExpression.from(
                        HandlerHelper.render(rightBuilder) + ".get()",
                        null,
                        rightBuilder.getRefs(),
                        rightBuilder.getStaticWildcardImports());
            }
            return null;
        }
        if (rightBuilder instanceof JavaExpression rb && rb.unwrapToBuilder().isPresent()) {
            GeneratorModel gm = compiler.getGeneratorModel();
            if (gm == null) {
                return null;
            }
            RMetaAnnotatedType inferred = gm.workspace().getInferredType(expr);
            if (inferred == null || inferred.isMissing()) {
                return null;
            }
            return rb.unwrapToBuilder().get();
        }
        return null;
    }

    /**
     * facet sortKeyDefaultArgCollapse (PR #345): true when the bare symbol resolves
     * to an INPUT or the OUTPUT of its enclosing function — the reference renders as
     * the strippable {@code MapperS.of(<name>)} wrap (golden keeps the bare name at
     * the getOrDefault arg), NOT the re-rooted item navigation the nav-chain arm
     * collapses. Identity-compared against the enclosing function's declared
     * attributes (the same nodes the parser binds).
     */
    private static boolean isEnclosingFunctionParam(RSymbolReference sr) {
        RAttribute bound = sr.symbol()
                .filter(RAttribute.class::isInstance)
                .map(RAttribute.class::cast)
                .orElse(null);
        if (bound == null) {
            return false;
        }
        com.regnosys.rosetta.ast.functions.RFunction fn = HandlerHelper.findEnclosingFunction(sr);
        if (fn == null) {
            return false;
        }
        if (fn.output().filter(out -> out == bound).isPresent()) {
            return true;
        }
        for (RAttribute input : fn.inputs()) {
            if (input == bound) {
                return true;
            }
        }
        return false;
    }

    /**
     * Compiles an {@link RJoinExpr} into a {@code join()} instance call on the
     * left operand.
     *
     * <p>{@link RJoinExpr} stores the optional separator in {@code separator()},
     * not in {@code rawRight()} (which is always null for join). When a separator
     * is present it is passed as the sole argument; when absent the no-arg overload
     * is used.
     *
     * @param expr     the join expression node
     * @param ctx      the current expression compilation context
     * @param compiler the parent compiler for recursive compilation of operands
     * @return a {@link JavaExpression} rendering {@code left.join(separator)} or
     *         {@code left.join()}
     */
    public JavaStatementBuilder handle(RJoinExpr expr, ExpressionContext ctx, ExpressionCompiler compiler) {
        // .join is an instance method — operand refs flow only.
        // facet implicit_operand_synthesis (PR #218): a `then join <sep>` bare join
        // elides its left operand — the piped value — so rawLeft() is null;
        // substitute the synthetic implicit (→ the bound thenArg).
        JavaStatementBuilder leftBuilder =
                compiler.compile(HandlerHelper.orSyntheticImplicit(expr.rawLeft(), expr), ctx.expectedType(), ctx.scope());
        String left = HandlerHelper.render(leftBuilder);
        return expr.separator()
                .map(sep -> {
                    JavaStatementBuilder sepBuilder =
                            compiler.compile(sep, ctx.expectedType(), ctx.scope());
                    String compiledSeparator = HandlerHelper.render(sepBuilder);
                    Set<JavaClass<?>> refs = new HashSet<>();
                    refs.addAll(leftBuilder.getRefs());
                    refs.addAll(sepBuilder.getRefs());
                    Set<JavaClass<?>> wildcards = new HashSet<>();
                    wildcards.addAll(leftBuilder.getStaticWildcardImports());
                    wildcards.addAll(sepBuilder.getStaticWildcardImports());
                    return JavaExpression.from(
                            left + ".join(" + compiledSeparator + ")",
                            null,
                            refs,
                            wildcards);
                })
                .orElseGet(() -> JavaExpression.from(
                        left + ".join()",
                        null,
                        leftBuilder.getRefs(),
                        leftBuilder.getStaticWildcardImports()));
    }

}
