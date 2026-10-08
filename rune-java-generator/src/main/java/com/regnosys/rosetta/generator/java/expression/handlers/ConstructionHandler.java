package com.regnosys.rosetta.generator.java.expression.handlers;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.expressions.binary.RDefaultExpr;
import com.regnosys.rosetta.ast.expressions.binary.RThenExpr;
import com.regnosys.rosetta.ast.expressions.constructors.RConditionalExpr;
import com.regnosys.rosetta.ast.expressions.constructors.RConstructorExpr;
import com.regnosys.rosetta.ast.expressions.literals.RListLiteral;
import com.regnosys.rosetta.ast.expressions.references.REmptyLiteral;
import com.regnosys.rosetta.ast.expressions.references.REnumValueRef;
import com.regnosys.rosetta.ast.expressions.references.RImplicitVariable;
import com.regnosys.rosetta.ast.expressions.references.RSymbolReference;
import com.regnosys.rosetta.ast.expressions.supporting.RInlineFunction;
import com.regnosys.rosetta.ast.expressions.supporting.RKeyValuePair;
import com.regnosys.rosetta.ast.expressions.supporting.RWithMetaEntry;
import com.regnosys.rosetta.ast.enums.ListOp;
import com.regnosys.rosetta.ast.expressions.unary.RExtractExpr;
import com.regnosys.rosetta.ast.expressions.unary.RFilterExpr;
import com.regnosys.rosetta.ast.expressions.unary.RListOpExpr;
import com.regnosys.rosetta.ast.expressions.unary.RWithMetaExpr;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.functions.RShortcut;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.supporting.REnumValue;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.ast.types.REnumeration;
import com.regnosys.rosetta.generator.GeneratedIdentifier;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.JavaNamingUtil;
import com.regnosys.rosetta.generator.java.SilentDegradation;
import com.regnosys.rosetta.generator.java.enums.EnumHelper;
import com.regnosys.rosetta.generator.java.expression.ExpressionCompiler;
import com.regnosys.rosetta.generator.java.expression.ExpressionContext;
import com.regnosys.rosetta.generator.java.function.StatementHoistSession;
import com.regnosys.rosetta.generator.java.object.MetaFieldGenerator;
import com.regnosys.rosetta.generator.java.scoping.CondListCoerce;
import com.regnosys.rosetta.generator.java.scoping.CondSingleCoerce;
import com.regnosys.rosetta.generator.java.scoping.JavaStatementScope;
import com.regnosys.rosetta.generator.java.statement.JavaLocalVariableDeclarationStatement;
import com.regnosys.rosetta.generator.java.statement.JavaRawStatement;
import com.regnosys.rosetta.generator.java.statement.builder.JavaExpression;
import com.regnosys.rosetta.generator.java.statement.builder.JavaStatementBuilder;
import com.regnosys.rosetta.generator.java.template.ImportCollisionResolver;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.generator.java.types.RJavaWithMetaValue;
import com.regnosys.rosetta.types.ExpressionCardinality;
import com.regnosys.rosetta.types.RBasicType;
import com.regnosys.rosetta.types.RChoiceTypeRef;
import com.regnosys.rosetta.types.RDataTypeRef;
import com.regnosys.rosetta.types.REnumTypeRef;
import com.regnosys.rosetta.types.RType;
import com.regnosys.rosetta.types.RMetaAnnotatedType;
import com.rosetta.util.types.JavaClass;
import com.rosetta.util.types.JavaType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Handles code generation for construction and metadata expressions:
 * {@link RConstructorExpr} and {@link RWithMetaExpr}.
 *
 * <p>Golden output patterns (simplified for M7b-1):
 * <pre>
 *   Type { key1: val1, key2: val2 }   →  Type.builder().setKey1(val1).setKey2(val2).build()
 *   expr with-meta { scheme: val }    →  arg.toBuilder().setMeta(MetaFields.builder().setScheme(val).build()).build()
 * </pre>
 *
 * <p>Full constructor compilation requires type resolution for correct
 * setter names and type coercions. At M7b-1 a simplified builder chain
 * pattern is generated.
 */
public class ConstructionHandler {

    // =========================================================================
    // Constructor expression
    // =========================================================================

    /**
     * Compiles a constructor expression into a builder chain.
     *
     * <p>Pattern: {@code TypeName.builder().setField1(value1).setField2(value2).build()}
     *
     * @param expr     the constructor expression node
     * @param ctx      the current expression compilation context
     * @param compiler the parent compiler for recursive compilation of value expressions
     * @return a {@link JavaExpression} rendering the builder chain
     */
    public JavaStatementBuilder handle(RConstructorExpr expr, ExpressionContext ctx, ExpressionCompiler compiler) {
        // Each pair value is compiled ONCE and shared between the typed-block and
        // legacy paths (PR #157 indep-review advisory): the former speculative
        // double-compile re-ran scope-mutating value compiles on the decline path,
        // so the legacy placeholder rendered with recompile-suffixed locals.
        List<RKeyValuePair> pairs = expr.pairs();
        List<JavaStatementBuilder> compiledValues = new ArrayList<>(pairs.size());
        // facet condListCoerce (PR #327, arm A1): a MULTI attribute's conditional value
        // compiles under a CondListCoerce handshake so ControlFlowHandler emits the
        // LIST-typed per-branch coercion hoist (fired → coerceCtorArg splices the local
        // BARE instead of the #198 singletonList re-coercion). Pre-resolved attributes
        // (only when some pair value IS a conditional — the resolution duplicates
        // tryTypedBuilderBlock's header, which must not itself compile).
        Map<String, RAttribute> condListAttrs = condListPreAttrs(expr, pairs, compiler);
        List<CondListCoerce> condListPairs = new ArrayList<>(pairs.size());
        // facet ctorCondSingleCoerce (seat 28, law A): the SINGLE twin of the same
        // pre-resolution - a meta-NONE single attribute's conditional value compiles under a
        // CondSingleCoerce handshake so ControlFlowHandler declares the hoisted local at the
        // ATTRIBUTE's type and converts each arm in place (fired -> coerceCtorArg splices the
        // local BARE instead of re-narrowing at the setter).
        List<CondSingleCoerce> condSinglePairs = new ArrayList<>(pairs.size());
        // facet hoistReorder (PR #327, facet C): record each pair's compile-phase hoist
        // watermark (the sink's pending count AFTER the pair's value compiled) so the
        // coercion phase can INSERT its own hoists at the pair's CONSUMPTION position —
        // upstream declareAsVariable runs at consumption, so a pair's #198/#236 value
        // local precedes any LATER pair's ifThenElseResult block (the cdm6
        // Map*ExerciseTerms adjustableOrRelativeDate-before-ifThenElseResult order).
        JavaStatementScope ctorHoistSink = ctx.scope() == null ? null
                : ctx.scope().findStatementHoistSink();
        // facet ctorSetterNumericNarrowChain (PR #361): the LAMBDA-channel twin of the
        // #327 watermark — a lambda-interior ctor's coercion-phase hoists (the #356
        // numericCoerceChainInLambdaHoist pending-channel locals) relocate to their
        // pair's CONSUMPTION position exactly like the sink channel, interleaving
        // each numeric value local right after its own pair's compile-phase thenArg
        // statements (golden iosco v1/v2/v3 PeriodicPaymentRule `…thenArg4;
        // bigDecimal0; …thenArg5; bigDecimal1;` — the pre-#361 tail-append put both
        // bigDecimals after thenArg5). Byte-neutral for single-pair carriers
        // (GetPackg): the pair's end-mark IS the tail there.
        JavaStatementScope ctorLambdaBoundary = ctx.scope() == null ? null
                : ctx.scope().findPendingLambdaHoistBoundary();
        int[] pairCompileEndMarks = new int[pairs.size()];
        int[] pairLambdaEndMarks = new int[pairs.size()];
        for (int i = 0; i < pairs.size(); i++) {
            RKeyValuePair pair = pairs.get(i);
            CondListCoerce clc = null;
            CondSingleCoerce csc = null;
            if (condListAttrs != null && ctx.scope() != null
                    && pair.value() instanceof RConditionalExpr condValue) {
                RAttribute attr = condListAttrs.get(pair.key());
                GeneratorModel cgm = compiler.getGeneratorModel();
                if (attr != null && cgm != null && cgm.isMulti(attr)) {
                    clc = condListCoerceFor(attr, condValue, cgm,
                            compiler.getTypeTranslator(), compiler.getTypeUtil(), true);
                } else if (attr != null && cgm != null) {
                    csc = condSingleCoerceFor(attr, condValue, cgm,
                            compiler.getTypeTranslator());
                }
            }
            condListPairs.add(clc);
            condSinglePairs.add(csc);
            if (clc == null && csc == null) {
                compiledValues.add(compiler.compile(pair.value(), ctx.expectedType(), ctx.scope()));
            } else if (clc != null) {
                CondListCoerce previous = ctx.scope().pushCondListCoerce(clc);
                try {
                    compiledValues.add(compiler.compile(pair.value(), ctx.expectedType(), ctx.scope()));
                } finally {
                    ctx.scope().popCondListCoerce(previous);
                }
            } else {
                CondSingleCoerce previousSingle = ctx.scope().pushCondSingleCoerce(csc);
                try {
                    compiledValues.add(compiler.compile(pair.value(), ctx.expectedType(), ctx.scope()));
                } finally {
                    ctx.scope().popCondSingleCoerce(previousSingle);
                }
            }
            pairCompileEndMarks[i] = ctorHoistSink == null ? 0
                    : ctorHoistSink.statementHoistMark();
            pairLambdaEndMarks[i] = ctorLambdaBoundary == null ? 0
                    : ctorLambdaBoundary.pendingLambdaHoistMark();
        }

        // facet ctor_set_rendering: the upstream 9.83.0 form is a MULTI-LINE typed
        // builder block whose setter args are coerced to the ATTRIBUTE's item type
        // (ExpressionGenerator.caseConstructorExpression + evaluateConstructorValue).
        // Try it first; on ANY unsupported shape decline to the byte-identical legacy
        // one-line placeholder below (zero regression — the file stays waivered; no
        // green golden contains a constructor emission, verified corpus-wide).
        JavaStatementBuilder typedBlock = tryTypedBuilderBlock(expr, ctx, compiler, pairs,
                compiledValues, condListPairs, condSinglePairs, pairCompileEndMarks, ctorHoistSink,
                pairLambdaEndMarks, ctorLambdaBoundary);
        if (typedBlock != null) {
            return typedBlock;
        }

        // PR-A §9.1 C3a.4.h: operand-refs flowthrough. Constructor emits a
        // chain of setters whose values are recursively compiled; each
        // compiled value carries its own refs + staticWildcards, all of
        // which must round-trip through the outer builder.
        String typeName = expr.typeCall() != null ? expr.typeCall().typeName() : "Unknown";

        StringBuilder sb = new StringBuilder();
        sb.append(typeName).append(".builder()");

        Set<JavaClass<?>> refs = new HashSet<>();
        Set<JavaClass<?>> wildcards = new HashSet<>();

        for (int i = 0; i < pairs.size(); i++) {
            RKeyValuePair pair = pairs.get(i);
            String setterName = toSetterName(pair.key());
            JavaStatementBuilder valueBuilder = compiledValues.get(i);
            String value = HandlerHelper.render(valueBuilder);
            sb.append('.').append(setterName).append('(').append(value).append(')');
            refs.addAll(valueBuilder.getRefs());
            wildcards.addAll(valueBuilder.getStaticWildcardImports());
        }

        sb.append(".build()");

        return JavaExpression.from(sb.toString(), null, refs, wildcards);
    }

    // =========================================================================
    // Typed builder block (facet ctor_set_rendering)
    // =========================================================================

    /**
     * Continuation-line indent for the multi-line builder block, RELATIVE to the
     * enclosing statement: the statement renderer re-indents embedded newlines by the
     * statement depth, so one tab here lands setter/build lines exactly ONE tab deeper
     * than the assignment line — the golden shape (verified: all 217 sole-mechanism
     * goldens carry a 3-tab statement line + 4-tab setter/build lines). A constructor
     * NESTED as a setter argument keeps this same relative form; the embed site in
     * {@link #tryTypedBuilderBlock} indents the embedded block's newlines by one more
     * tab per nesting level (facet ctor_nested_builder_indent), matching upstream's
     * code-block tree shape.
     */
    private static final String CTOR_CONTINUATION_INDENT = "\n\t";

    /**
     * Upstream-shaped constructor emission ({@code ExpressionGenerator.caseConstructorExpression}
     * + {@code evaluateConstructorValue}, in-tree 9.83.0 reference):
     *
     * <pre>
     *   Type.builder()
     *       .set&lt;Attr&gt;[Value](&lt;value coerced to the attribute's item type&gt;)
     *       .build()
     * </pre>
     *
     * <p>The block is wrapped {@link JavaExpression#selfUnwrapping} so assignment /
     * evaluate-arg strip sites use it VERBATIM (upstream types the block as the POJO
     * class — a builder is not a Mapper, so no {@code .get()} fall-through applies).
     *
     * <p>Per-pair value coercion (the fork's eager-wrap equivalent of upstream's
     * expected-type-driven generation):
     * <ul>
     *   <li>single attr + structurally-unwrappable value (a wrap-factory MapperS/MapperC
     *       wrap: bare param, bare call result, literal) → the RAW inner; an integer
     *       literal targeting a {@code Long} attribute renders {@code (long) N};</li>
     *   <li>single attr + enum constant (by the producer's witness) → unchanged;</li>
     *   <li>single attr + Mapper chain → {@code <chain>.get()};</li>
     *   <li>multi attr + structurally-unwrappable MULTI value → {@code new ArrayList(<raw>)}
     *       ({@code java.util.ArrayList} ref added; raw type matching the golden) — UNLESS
     *       the value's item type is provably non-pojo (enum/basic/record), which splices
     *       the raw value bare (facet {@code ingest_setter_value_form} arm C2r — upstream
     *       wraps only wildcard = pojo-item List actuals);</li>
     *   <li>multi attr + Mapper chain → {@code <chain>.getMulti()};</li>
     *   <li>anything else → {@code null} (the WHOLE constructor declines to the legacy
     *       placeholder — byte-identical to pre-facet output).</li>
     * </ul>
     *
     * <p>Setter naming mirrors upstream {@code SET} vs {@code SET_VALUE}: an attribute
     * carrying a {@code [metadata …]} annotation uses {@code set<Name>Value} for a
     * meta-FREE value, and keeps the PLAIN {@code set<Name>} when the value provably
     * carries attribute-meta itself (facet {@code ingest_setter_value_form} arm C1 —
     * see {@link #ctorSetterName}).
     *
     * <p>A NESTED constructor as a setter argument (facet {@code ctor_nested_builder_indent},
     * the PR #157 deferred NESTED_BUILDER family) renders through this same emission
     * recursively; the embed site indents the embedded argument's newlines by ONE tab
     * so each nesting level lands one tab deeper than the enclosing block's own
     * continuation lines — upstream's code-block tree shape (a literal
     * {@code String.replace}, no regex on structured content; recursion compounds the
     * indent for deeper nesting — corpus depth reaches 4).
     *
     * @return the typed builder block, or {@code null} to decline to the legacy form
     */
    private JavaStatementBuilder tryTypedBuilderBlock(
            RConstructorExpr expr, ExpressionContext ctx, ExpressionCompiler compiler,
            List<RKeyValuePair> pairs, List<JavaStatementBuilder> compiledValues,
            List<CondListCoerce> condListPairs, List<CondSingleCoerce> condSinglePairs,
            int[] pairCompileEndMarks,
            JavaStatementScope ctorHoistSink,
            int[] pairLambdaEndMarks, JavaStatementScope ctorLambdaBoundary) {
        if (expr.typeCall() == null) {
            return null;
        }
        GeneratorModel gm = compiler.getGeneratorModel();
        var translator = compiler.getTypeTranslator();
        if (gm == null || translator == null) {
            return null;
        }
        JavaTypeUtil typeUtil = compiler.getTypeUtil();
        RType resolved = gm.resolveTypeCall(expr.typeCall());
        RDataType dataType = null;
        if (resolved instanceof RDataTypeRef dtr) {
            dataType = dtr.astNode();
        } else if (resolved instanceof RChoiceTypeRef ctr) {
            dataType = ctr.asRDataType();
        }
        if (dataType == null) {
            return null;
        }
        JavaClass<?> clazz = translator.toJavaReferenceType(resolved);
        if (clazz == null) {
            return null;
        }

        // facet ctorChoiceSuperAttrs (PR #389): the choice-super-aware walk — a
        // data-extends-choice ctor (`BasketConstituent { Index: …, Asset: … }`, cdm6
        // MapBasketConstituentWithLocation) keys its option pairs by the PROJECTED
        // choice-option attributes; the choice-blind walk returned attr==null for the
        // capital option keys and dropped the WHOLE ctor to the legacy placeholder
        // (T389A). Green-safe: an option-keyed pair previously forced the
        // non-compiling legacy form, so every carrier was already waivered.
        Map<String, RAttribute> attrsByName = new HashMap<>();
        for (RAttribute attr : HandlerHelper.allAttributesIncludingChoiceSuper(dataType)) {
            attrsByName.put(attr.name(), attr);
        }

        StringBuilder sb = new StringBuilder();
        // facet fqnWitness (PR #227): emit the builder-ctor type as a first-claim-wins
        // sentinel (carrying the canonical name) rather than the bare simple name. When the
        // simple name collides with a different-canonical fpml witness in the same class, the
        // render-order resolver (FunctionGenerator.buildStandardModel) decides bare-vs-FQN; a
        // non-colliding ctor resolves byte-identically to clazz.getSimpleName().
        sb.append(ImportCollisionResolver.typeRef(clazz.getCanonicalName().withDots()))
          .append(".builder()");
        Set<JavaClass<?>> refs = new HashSet<>();
        Set<JavaClass<?>> wildcards = new HashSet<>();
        refs.add(clazz);

        // Sink rollback (PR #332, Seat-1 should-fix): a pair's coercion-phase hoist
        // (#198 singletonList / #236 meta-deref / #208-chain numeric locals) registers
        // + inserts into the sink DURING the loop; a LATER pair's decline aborts the
        // whole typed block with the earlier pairs' hoists already in the sink —
        // orphaning `final …` decls above the legacy placeholder and inflating the
        // session's name groups for the discarded attempt (the #170 class). Snapshot
        // the sink list + the session numbering at loop entry and restore on every
        // decline: the legacy path re-renders the SAME compiledValues, whose
        // COMPILE-phase hoists pre-date this method and are inside the snapshot, so
        // they survive the restore untouched. Green-safe today (a declining ctor
        // forces the non-compiling legacy placeholder — every carrier waivered) but
        // a latent byte-noise machine for future ctor facets without it.
        List<String> sinkSnapshot = ctorHoistSink == null
                ? null : ctorHoistSink.snapshotStatementHoists();
        Map<String, Integer> sessionSnapshot =
                ctorHoistSink == null || ctorHoistSink.statementHoistSession() == null
                        ? null : ctorHoistSink.statementHoistSession().snapshot();
        Runnable rollback = () -> {
            if (ctorHoistSink != null) {
                ctorHoistSink.restoreStatementHoists(sinkSnapshot);
                if (sessionSnapshot != null) {
                    ctorHoistSink.statementHoistSession().restore(sessionSnapshot);
                }
            }
        };
        int coercionHoistsInserted = 0;
        int lambdaCoercionInserted = 0;
        for (int i = 0; i < pairs.size(); i++) {
            RKeyValuePair pair = pairs.get(i);
            RAttribute attr = attrsByName.get(pair.key());
            if (attr == null) {
                rollback.run();
                return null;
            }
            JavaStatementBuilder compiled = compiledValues.get(i);
            // facet hoistReorder (PR #327, facet C): this pair's coercion-phase hoists
            // (#198 singletonList / #236 meta-deref / #208 numeric locals) relocate to
            // the pair's CONSUMPTION position — immediately after the pair's own
            // compile-phase blocks, BEFORE any later pair's ifThenElseResult block
            // (upstream declares at consumption; the phase-ordered append put them
            // after ALL compile-phase blocks). Append-equivalent (byte-neutral) when
            // no later pair registered a compile-phase hoist.
            int beforeCoerce = ctorHoistSink == null ? 0 : ctorHoistSink.statementHoistMark();
            // facet ctorSetterNumericNarrowChain (PR #361): the LAMBDA-channel twin
            // window — see the phase-1 watermark comment.
            int beforeCoerceLambda = ctorLambdaBoundary == null ? 0
                    : ctorLambdaBoundary.pendingLambdaHoistMark();
            // The coerced arg carries the refs of the PATH TAKEN: a structurally
            // unwrapped value contributes the INNER's refs (the discarded MapperS/
            // MapperC wrap must not pin a stale import — the wrap factories add the
            // Mapper class to the WRAPPER's refs only), a chain contributes its own.
            // facet ctorAsKeyReference (PR #437, finding #35): a fired as-key arm
            // produces the WRAPPER value, so the setter is the PLAIN form
            // (upstream requiresValueAssignment: the Value form only when the
            // target has meta AND the value does NOT — golden `.setAttrSingle(…)`
            // over the bare-typed `key as-key`, where the raw value took
            // `.setAttrSingleValue`). A declined as-key keeps today's name.
            boolean[] ctorAsKeyFired = new boolean[1];
            JavaExpression arg = coerceCtorArg(attr, pair, compiled, condListPairs.get(i),
                    condSinglePairs.get(i), ctx, gm, translator, typeUtil, compiler,
                    ctorAsKeyFired);
            if (arg == null) {
                rollback.run();
                return null;
            }
            if (ctorHoistSink != null && pairCompileEndMarks != null) {
                List<String> coercionHoists = ctorHoistSink.drainStatementHoistsSince(beforeCoerce);
                if (!coercionHoists.isEmpty()) {
                    ctorHoistSink.insertStatementHoists(
                            pairCompileEndMarks[i] + coercionHoistsInserted, coercionHoists);
                    coercionHoistsInserted += coercionHoists.size();
                }
            }
            if (ctorLambdaBoundary != null && pairLambdaEndMarks != null) {
                List<com.regnosys.rosetta.generator.java.statement.JavaStatement>
                        lambdaCoercionHoists = ctorLambdaBoundary
                                .drainPendingLambdaHoistsSince(beforeCoerceLambda);
                if (!lambdaCoercionHoists.isEmpty()) {
                    ctorLambdaBoundary.insertPendingLambdaHoists(
                            pairLambdaEndMarks[i] + lambdaCoercionInserted, lambdaCoercionHoists);
                    lambdaCoercionInserted += lambdaCoercionHoists.size();
                }
            }
            // A multi-line arg is a CHILD of this setter line: its relative
            // continuation lines sit one nesting level deeper than this block's
            // own (golden ladders one tab per level; see f-probe-158). Literal
            // newline replace — a no-op for single-line args. Today the only
            // multi-line shape reaching here is a nested constructor's typed
            // block; a Mapper-chain arg carrying CHAIN_LINK newlines would land
            // at the same child depth, the upstream tree shape.
            String argSrc = arg.renderToString().replace("\n", "\n\t");
            sb.append(CTOR_CONTINUATION_INDENT)
              .append('.').append(ctorAsKeyFired[0]
                      ? "set" + JavaNamingUtil.toFirstUpper(attr.name())
                      : ctorSetterName(attr, pair, compiled, compiler, condListPairs.get(i)))
              .append('(').append(argSrc).append(')');
            refs.addAll(arg.getRefs());
            wildcards.addAll(arg.getStaticWildcardImports());
        }

        sb.append(CTOR_CONTINUATION_INDENT).append(".build()");
        JavaExpression inner = JavaExpression.from(sb.toString(), null, refs, wildcards);
        return JavaExpression.selfUnwrapping(inner);
    }

    /**
     * Per-pair value coercion to the attribute's item type — see
     * {@link #tryTypedBuilderBlock} for the supported ladder. Returns {@code null}
     * when the (attribute, value-shape) combination is unsupported, declining the
     * whole constructor to the legacy placeholder.
     */
    private JavaExpression coerceCtorArg(RAttribute attr, RKeyValuePair pair,
            JavaStatementBuilder compiled, CondListCoerce condList, CondSingleCoerce condSingle,
            ExpressionContext ctx,
            GeneratorModel gm,
            com.regnosys.rosetta.generator.java.types.JavaTypeTranslator translator,
            JavaTypeUtil typeUtil, ExpressionCompiler compiler, boolean[] asKeyFired) {
        if (!(compiled instanceof JavaExpression compiledExpr)) {
            return null;
        }
        // facet condListCoerce (PR #327, arm A1): the conditional value ALREADY hoisted
        // the LIST-typed per-branch coercion (ControlFlowHandler.hoistAsListLocalOrNull
        // fired on this pair's handshake) — the compiled result is the bare List-typed
        // `ifThenElseResultN` local, spliced VERBATIM into the multi setter
        // (`.setX(ifThenElseResultN)`); the #198 hoistSingleValueIntoMultiOrNull
        // re-coercion and the `.getMulti()` fall-through must both be bypassed.
        // facet ctorCondSingleCoerce (seat 28, law A): the SINGLE twin joins the SAME
        // bypass. The local is already declared at the attribute's Java type with the
        // conversion inside each arm, so the setter takes it BARE
        // (`.setBsisPtSprd(ifThenElseResult3)`, golden drr 5.61.0 Create_FloatingRate).
        // This bypass is a CORRECTNESS requirement, not a byte preference: without it the
        // #208 tryCtorNumericNarrow below re-fires on the sentinel (its item type is
        // recovered from gm.workspace().getInferredType(pair.value()) - the conditional's
        // own `number` inference, which the arm conversion does not change) and emits
        // `ifThenElseResult3.intValueExact()` on an Integer local, which does not compile.
        if ((condList != null && condList.fired)
                || (condSingle != null && condSingle.fired)) {
            JavaStatementBuilder inner = compiledExpr.unwrapToBuilder()
                    .map(b -> (JavaStatementBuilder) b).orElse(compiledExpr);
            return JavaExpression.from(HandlerHelper.render(inner), null,
                    inner.getRefs(), inner.getStaticWildcardImports());
        }
        // facet ctorAsKeyReference (PR #390): an `as-key` ctor field renders the
        // upstream meta-key copy (the #328 renderAsKeySetOrNull law at the ctor
        // seat) — hoist the wrapper + the value-deref pair, splice the
        // reference-only builder. Declines (null) fall THROUGH to the arms below,
        // which render today's whole-wrapper splice bytes.
        JavaExpression asKeyArg = tryCtorAsKeyReference(
                attr, pair, compiledExpr, ctx, gm, translator, typeUtil, compiler);
        if (asKeyArg != null) {
            asKeyFired[0] = true;
            return asKeyArg;
        }
        // facet cat16BindEnumSeats (PR #452): a BIND-STAMPED bare ctor value — the
        // engine's Category-16 expected-type bind resolved `identifierType: Name` to
        // the attribute enum's REnumValue, and ReferenceHandler's REnumValue branch
        // rendered it as a plain JavaExpression (NO unwrap contract) qualified — before
        // v3.1 flip seat 12 — by the DECLARING parent with the PARENT's import on
        // refs (since seat 12 that root arm qualifies by the node's INFERRED = expected
        // enum, so this rung's re-qualification is a no-op there), where golden
        // qualifies by the attribute's CHILD enum (golden cdm6 MapAssetIdentifierList
        // `.setIdentifierType(AssetIdTypeEnum.NAME)`, NAME declared on
        // ProductIdTypeEnum — the #211/#358 flatten law). Fired BEFORE the unwrap
        // ladder: the plain resolved render never satisfies `unwrap.isPresent()`, so
        // the #209 tryCtorEnumQualify arm below is structurally unreachable for this
        // shape. Same-instance gated + parent-import-swapped exactly like the #215
        // tryInheritedEnumRequalify.
        JavaExpression boundRequalified = tryCtorBoundEnumRequalify(
                pair, attr, gm, translator, compiledExpr);
        if (boundRequalified != null) {
            return boundRequalified;
        }
        boolean attrMulti = gm.isMulti(attr);
        Optional<JavaStatementBuilder> unwrap = compiledExpr.unwrapToBuilder();
        if (unwrap.isPresent()) {
            JavaStatementBuilder inner = unwrap.get();
            String raw = HandlerHelper.render(inner);
            ExpressionCardinality valueCard = gm.workspace().getCardinality(pair.value());
            // THE T3 BRIDGE-CARDINALITY GUARD (PR-24): a FLIPPED-alias ctor value's
            // cardinality authority is the SEAM's own S/C bit (the § 3 render
            // channel, riding the same policy the bridge wrap consulted) — the
            // workspace inference is BLIND on some conditional-bodied alias
            // references (a SINGLE fallback), and a MULTI bridge admitted into the
            // singleton-hoist arm below hands the raw List to an item-typed local
            // (the Create_QuantityChange newTradeLots differential-gate compile
            // catch: `final TradeLot _tradeLot = <List-valued call>;`). The seam
            // channel is the reference route's constant null — reference bytes
            // inert BY CODE, and pre-T3 no bridged value reached this block (the
            // unwrap gate above admits only wrap-factory values).
            if (pair.value() instanceof RSymbolReference aliasRef) {
                RShortcut aliasTarget = aliasRef.symbol()
                        .filter(RShortcut.class::isInstance).map(RShortcut.class::cast)
                        .orElse(null);
                Boolean seamMulti = aliasTarget == null ? null
                        : compiler.aliasValueSeamIsMultiOrNull(
                                HandlerHelper.findEnclosingFunction(pair.value()), aliasTarget);
                if (seamMulti != null) {
                    valueCard = seamMulti ? ExpressionCardinality.MULTI
                            : ExpressionCardinality.SINGLE;
                }
            }
            // facet ctorRuleValueCardinality (seat 25, law D): a bare RULE-reference ctor
            // value's cardinality authority is the shared ruleOutputProvesMulti verdict
            // (the #367 law — the workspace read is BLIND to the callee rule's cardinality
            // and reports SINGLE): the esma/fca DeliveryRule `daysOfTheWeek` value is the
            // MULTI DaysOfTheWeek rule consumed at a MULTI attribute, and the SINGLE
            // misread hoisted `final String string = …;` + the Collections.singletonList
            // coercion golden does not carry (golden passes the callee's List straight
            // through the setter). The seat-25 P25C probe measured the whole population:
            // attrMulti=true ∧ provesMulti=true ∧ valueCard=SINGLE at exactly these two
            // sites corpus-wide; every other bare-rule ctor value reads provesMulti=false
            // and keeps today's bytes. MULTI-attribute seats ONLY (the review's B-2): the
            // override must not reach the `!attrMulti` branch below, where a MULTI verdict
            // returns null and coerceCtorArg's caller rolls back and declines the WHOLE
            // constructor — provably byte-neutral to add (the P25C population carries ZERO
            // attrMulti=false ∧ provesMulti=true rows), confining the law to its measured
            // population and its stated form.
            if (attrMulti
                    && pair.value() instanceof RSymbolReference ruleValueRef
                    && ruleValueRef.symbol().filter(
                            com.regnosys.rosetta.ast.functions.RRule.class::isInstance).isPresent()
                    && NavigationHandler.ruleOutputProvesMulti(pair.value(), compiler)) {
                valueCard = ExpressionCardinality.MULTI;
            }
            if (!attrMulti) {
                if (valueCard == ExpressionCardinality.MULTI) {
                    // multi value into a single attribute — upstream coerces with a
                    // list-to-item collapse this ladder does not reproduce; decline.
                    return null;
                }
                // ASCII-digit char-scan, NOT a regex (engineering-standards: no regex
                // on structured content — the rendered arg is Java source). A Java
                // identifier can never be all digits (JLS 3.8), so this matches only
                // an integer-literal render.
                if (!raw.isEmpty() && raw.chars().allMatch(c -> c >= '0' && c <= '9')) {
                    // integer literal — upstream renders it at the attribute's item
                    // type: a Long attribute gets the (long) cast; a BigDecimal
                    // (`number`) attribute the `BigDecimal.valueOf(N)` coercion
                    // (upstream TypeCoercionService int→BigDecimal; facet
                    // ctorLiteralBigDecimalValueOf PR #334 — SpreadofLeg1/SpreadOfLeg2
                    // `basis: 0`; a bare int into a BigDecimal setter never compiled,
                    // so no green file carries the pre-fix form); every other numeric
                    // target keeps the bare literal in the supported corpus.
                    JavaClass<?> attrJava = ctorAttrJavaType(attr, gm, translator);
                    if (attrJava == null) {
                        return null;
                    }
                    String lit;
                    Set<JavaClass<?>> litRefs = inner.getRefs();
                    if ("Long".equals(attrJava.getSimpleName())) {
                        lit = "(long) " + raw;
                    } else if ("BigDecimal".equals(attrJava.getSimpleName())) {
                        lit = "BigDecimal.valueOf(" + raw + ")";
                        litRefs = HandlerHelper.union(litRefs, Set.of(HandlerHelper.BIG_DECIMAL));
                    } else {
                        lit = raw;
                    }
                    return JavaExpression.from(lit, null,
                            litRefs, inner.getStaticWildcardImports());
                }
                // facet ctorSetterEnumQualify (PR #209): a BARE unresolved enum value
                // in a ctor-setter (`style: European`) renders the bare rune name
                // `European` — a non-compiling undefined symbol, because enum values are
                // not in function scope and the parser leaves the value an UNRESOLVED
                // RSymbolReference (PR #154/#206) — where the attribute is an enum type.
                // Qualify it to the Java constant `OptionExerciseStyleEnum.EUROPEAN`
                // + import, resolved DIRECTLY from the attribute's enum RType (no sibling
                // inference, unlike the comparison-operand #206 enumQualify). Green-safe
                // by construction: every carrier was already a waivered mismatch.
                JavaExpression enumQualified = tryCtorEnumQualify(
                        pair, attr, gm, translator, compiler, inner);
                if (enumQualified != null) {
                    return enumQualified;
                }
                // facet ctorSetterNumericNarrow (PR #208): a numeric ctor value whose
                // item type is WIDER than the attribute's bounded-integer Java type
                // (a `number` BigDecimal value into an ISO20022 `int` setter such as
                // setBsisPtSprd) narrows via the upstream null-guarded exact conversion
                // `(<v> == null ? null : <v>.intValueExact())` — the ctor-setter
                // analogue of the PR #109/#128 evaluate-arg deref. The fork spliced the
                // value BARE (a BigDecimal where Integer is expected — non-compiling, so
                // every carrier was already waivered → green-safe by construction).
                JavaExpression narrowed = tryCtorNumericNarrow(
                        raw, compiledExpr, attr, pair.value(), ctx, gm, translator, typeUtil, compiler, inner);
                if (narrowed != null) {
                    return narrowed;
                }
                // facet ctorSetterMetaDerefHoist (PR #236): a complex value that PROVABLY
                // produces an attribute-meta wrapper (a direct fn call / extract whose
                // callee output carries a value-level [metadata ...] annotation) consumed
                // by a NON-meta single setter (which takes the bare item) hoists the
                // wrapper to a `final <Wrapper> <name> = <call>;` local — it is referenced
                // TWICE (the null check and the getValue), so it cannot stay inline — and
                // unwraps null-safely `(<name> == null ? null : <name>.getValue())`:
                // upstream's convertNullSafe meta->item deref at the ctor-setter seat (the
                // SETTER analogue of the PR #143/#170 evaluate-arg deref + the PR #208
                // numeric narrow). The fork spliced the wrapper BARE (a FieldWithMetaX
                // where the bare item is expected — non-compiling, so every carrier was
                // already a waivered mismatch → green-safe by construction).
                JavaExpression metaDeref = hoistMetaDerefCtorValueOrNull(
                        attr, pair, inner, raw, ctx, gm, translator, typeUtil);
                if (metaDeref != null) {
                    return metaDeref;
                }
                // facet ctorSetterMetaDerefFunctionHost (v3.1 flip seat 33, law A.1, rung 2): a
                // `default`-VALUED ctor pair whose collapsed render already IS the item
                // (`Mapper.getOrDefault(T)` returns the VALUE) reaches THIS fall-through, not
                // the S2 arm 100 lines below - `compiledExpr.unwrapToBuilder()` is present for
                // it, so `coerceCtorArg` returns from this block first. MEASURED:
                // [P33-CTORDEFAULT], the anchor built for the S2 arm, carries 80 rows over
                // exactly two `where=` (fn:Create_SubmissionHeader, fn:Create_SubmissionCore)
                // and ZERO carrier rows, so the dossier's rung-2 siting is REFUTED and the
                // bare splice below is the fork's `.setIdentifier(<whole default chain>)`.
                // golden hoists that render VERBATIM into the enclosing map lambda and derefs
                // it at the setter (LAW 74, javac33 line 548).
                JavaExpression defaultMetaDeref = hoistMetaDerefCtorDefaultInLambdaOrNull(
                        attr, pair, inner, raw, ctx, gm, translator, typeUtil, compiler);
                if (defaultMetaDeref != null) {
                    return defaultMetaDeref;
                }
                return JavaExpression.from(raw, null,
                        inner.getRefs(), inner.getStaticWildcardImports());
            }
            if (valueCard == ExpressionCardinality.MULTI) {
                // facet ingest_setter_value_form (arm C2r): upstream inserts the
                // raw-typed defensive copy `new ArrayList(...)` only when the
                // actual List type carries a WILDCARD argument
                // (TypeCoercionService.xtend:342-344), and only generated-pojo
                // item types are ever wildcard (JavaTypeUtil
                // .wrapExtendsIfNotFinal) — a builtin/record/enum item splices
                // BARE (golden `.setAdjustedDateValue(fpmlDateList)` /
                // `.setDaysOfTheWeek(getDaysOfTheWeek.evaluate(...))`). The
                // pojo test is RType-level (data/choice type refs — the
                // translator's pojo JavaClass is not the JavaPojoInterface
                // subclass on this path); meta-annotated, alias-typed and
                // unresolvable values DECLINE to the wrap (today's render, the
                // only regression-free direction).
                if (ctorValueItemIsBareSpliced(pair.value(), gm, compiler)) {
                    return JavaExpression.from(raw, null,
                            inner.getRefs(), inner.getStaticWildcardImports());
                }
                // List<? extends T> param into the List<T> setter — golden form is the
                // raw-typed copy constructor (no diamond), import via refs.
                Set<JavaClass<?>> withArrayList = new HashSet<>(inner.getRefs());
                withArrayList.add(HandlerHelper.ARRAY_LIST);
                return JavaExpression.from("new ArrayList(" + raw + ")", null,
                        withArrayList, inner.getStaticWildcardImports());
            }
            // facet ctorSingletonListHoist: a SINGLE-cardinality value into a MULTI
            // (List) attribute. Upstream's TypeCoercionService.convertNullSafe hoists
            // the value to a `final <Item> <name> = <call>;` local — it is consumed
            // TWICE (the null-check and the singletonList), so it cannot stay inline —
            // and coerces item->list null-safely:
            //   (<name> == null ? Collections.<Item>emptyList()
            //                   : Collections.singletonList(<name>))
            // (the non-meta, item-into-list sibling of #190's meta singletonList wrap
            // and #192's emptyMultiArg emptyList). Pre-facet this declined to the
            // legacy placeholder, whose `MapperS.of(<value>)` into a List<Item> setter
            // does NOT compile (every carrier was already a waivered mismatch —
            // green-safe by construction). Activating the arm renders the whole typed
            // block (its single-attribute setters already drop the MapperS.of wrap);
            // the selfUnwrapping block drops the `.build().get()` tail and
            // prependStatementHoists lifts the `final` decl before the assignment.
            JavaExpression singletonHoist =
                    hoistSingleValueIntoMultiOrNull(attr, pair, inner, raw, ctx, gm, translator,
                            typeUtil, compiledExpr, compiler);
            if (singletonHoist != null) {
                return singletonHoist;
            }
            return null;
        }
        String src = HandlerHelper.render(compiledExpr);
        if (HandlerHelper.isBareEnumConstant(compiledExpr)) {
            return attrMulti ? null : JavaExpression.from(src, null,
                    compiledExpr.getRefs(), compiledExpr.getStaticWildcardImports());
        }
        // An `empty` value literal compiles to JavaLiteral.NULL ("null") — not a
        // Mapper, so the .get()/.getMulti() fall-through would render the invalid
        // `null.get()` (Copilot PR #157 finding). A single attribute passes the
        // null through (upstream generates the literal at the item type).
        if ("null".equals(src)) {
            if (!attrMulti) {
                return JavaExpression.from(src, null,
                        compiledExpr.getRefs(), compiledExpr.getStaticWildcardImports());
            }
            // facet ctorEmptyMultiList (PR #332): an `empty` value into a MULTI (List)
            // attribute renders upstream's empty representation at the expected List
            // type — `Collections.<Item>emptyList()` (TypeCoercionService.xtend:377-380,
            // the in-tree 9.83.0 reference; golden `.setLotIdentifier(Collections
            // .<Identifier>emptyList())` Create_Roll, `.setDatedValue(Collections
            // .<DatedValue>emptyList())` ResolveEquityInitialPrice, `.setStreet(
            // Collections.<String>emptyList())` MapCountry). This decline was the
            // pre-#332 gate that dropped WHOLE constructors to the legacy one-line
            // placeholder for every ingest-family carrier with one absent multi
            // attribute. The item type is sentinel-wrapped (the #227 fqnWitness
            // first-claim-wins collision form, matching the #198 singletonList arm's
            // emptyList witness at this same seat). A META-annotated multi attribute
            // DECLINES (its golden empty form — plain vs wrapper item — is unverified;
            // the pre-#332 legacy placeholder stands). Green-safe by construction:
            // the legacy placeholder rendered `.set<Attr>(null)` into a List setter
            // AND the whole-ctor `.build().get()` tail — non-compiling, so no green
            // file carries the declined form. The meta test is deliberately WIDER
            // than MetaFieldGenerator.detectMetaKind (the #236/#332 chain-hoist
            // gate): ANY [metadata …] annotation declines, including the type-level
            // key/template kinds detectMetaKind maps to NONE — no golden empty-list
            // form is verified for ANY meta-annotated multi attribute, so the
            // conservative superset keeps them all at the pre-#332 legacy bytes
            // (Seat-1 #332 nit: intentional, not an inconsistency).
            for (var annotationRef : attr.annotationRefs()) {
                if ("metadata".equals(annotationRef.annotationName())) {
                    return null;
                }
            }
            JavaClass<?> itemJava = ctorAttrJavaType(attr, gm, translator);
            if (itemJava == null) {
                return null;
            }
            String itemTypeText =
                    ImportCollisionResolver.typeRef(itemJava.getCanonicalName().withDots());
            Set<JavaClass<?>> emptyRefs = new HashSet<>(compiledExpr.getRefs());
            emptyRefs.add(itemJava);
            emptyRefs.add(HandlerHelper.COLLECTIONS);
            return JavaExpression.from(
                    "Collections.<" + itemTypeText + ">emptyList()", null,
                    emptyRefs, compiledExpr.getStaticWildcardImports());
        }
        // facet lambda_item_body_coercion (arm S2): a single-cardinality `default`
        // value is ALREADY item-typed — Mapper.getOrDefault(T) returns the VALUE,
        // not a Mapper (upstream binaryExpr case "default" types the result
        // toJavaReferenceType and appends nothing), so the fall-through's
        // `.get()` rendered the non-compiling `.getOrDefault(now.evaluate()).get()`.
        // Corpus law: ZERO of 12,484 golden `.getOrDefault(` sites are followed
        // by `.get()`. Gated on the value NODE being RDefaultExpr with a single
        // target attribute and non-MULTI value cardinality; every other value
        // keeps the cardinality-keyed extraction below.
        if (pair.value() instanceof RDefaultExpr && !attrMulti
                && gm.workspace().getCardinality(pair.value()) != ExpressionCardinality.MULTI) {
            return JavaExpression.from(src, null,
                    compiledExpr.getRefs(), compiledExpr.getStaticWildcardImports());
        }
        // facet multiDefaultTernary (PR #365, GODT widening; #367 shared-predicate
        // hardening): a MULTI attribute whose `default` passes the ONE shared gate
        // (NavigationHandler.multiDefaultTernaryOperands — LEFT-multi + proven-Mapper
        // RIGHT) splices VERBATIM — SetOperationHandler's ctor-FIELD arm (gated on
        // the IDENTICAL predicate + the RKeyValuePair parent) already rendered the
        // seat form `(l.getMulti().isEmpty() ? new ArrayList<>(r.getMulti()) : new
        // ArrayList<>(l.getMulti()))` (golden MapBreakdown cdm6 setQuantityValue;
        // MapPartyTradeIdentifier setAssignedIdentifier the single-alias-right
        // widening), so the `.getMulti()` fall-through below must not append.
        if (pair.value() instanceof RDefaultExpr multiDef && attrMulti
                && NavigationHandler.multiDefaultTernaryOperands(multiDef, compiler)) {
            return JavaExpression.from(src, null,
                    compiledExpr.getRefs(), compiledExpr.getStaticWildcardImports());
        }
        // facet ctorArgArrayListCopy (PR #232): a MULTI attribute whose value is a
        // DIRECT alias/function CALL (RSymbolReference → RShortcut/RFunction, no
        // trailing feature navigation) returning a Mapper takes upstream's defensive
        // List copy `new ArrayList<>(<call>.getMulti())` — the WITH-diamond chain
        // sibling of arm C2r's no-diamond `new ArrayList(evaluate(...))` (the
        // unwrap.isPresent() branch above, which handles List-returning evaluate()).
        // Corpus law: every golden direct-call multi ctor-setter wraps (41 sites, 0
        // bare); a NAVIGATION chain value (RFeatureCall) stays BARE (107 green sites)
        // and declines here; the 3 PR #214 bare-green statement setters live at the
        // getOrCreate().set() seat (FunctionExpressionRenderer), not this ctor-arg
        // seat. Green-safe by construction: the fork's bare `<call>.getMulti()` shares
        // the source list reference (a subsequent addAll would mutate the source) — a
        // divergence golden never carries, so every carrier was already a waivered
        // mismatch.
        if (attrMulti && ctorValueIsDirectCall(pair.value())) {
            // facet ctorListFieldNoCopy (seat 28, law 13) - LAW 69: the COPY exists only
            // because the emitted Mapper signature carries a WILDCARD element (only
            // generated-pojo item types are ever wildcarded), so the direct-call arm must
            // read the SAME element-kind law arm C2r already reads. Golden proves both
            // verdicts in ONE ctor: drr MapCorporateSector emits
            // `.setFinancialSector(financialSector(...).getMulti())` bare off
            // `MapperC<FinancialSectorEnum>` and
            // `.setNonFinancialSector(new ArrayList<>(nonFinancialSector(...).getMulti()))`
            // off `MapperC<? extends NonFinancialSector>`. The #232 javadoc's
            // "41 sites, 0 bare" census is RETIRED by that witness and re-derived at this
            // head: 233 wrapped / 4 bare corpus-wide at this seat, the 4 bare being this
            // carrier - the element-kind discriminator separates them with zero
            // exceptions.
            if (!ctorValueItemIsBareSpliced(pair.value(), gm, compiler)) {
                Set<JavaClass<?>> withArrayList = new HashSet<>(compiledExpr.getRefs());
                withArrayList.add(HandlerHelper.ARRAY_LIST);
                return JavaExpression.from("new ArrayList<>(" + src + ".getMulti())", null,
                        withArrayList, compiledExpr.getStaticWildcardImports());
            }
            return JavaExpression.from(src + ".getMulti()", null,
                    compiledExpr.getRefs(), compiledExpr.getStaticWildcardImports());
        }
        // facet ctorSetterMetaDerefLambda (PR #312): a ctor-setter value that is a NAVIGATION
        // producing a meta wrapper (compiled item type RJavaWithMetaValue, e.g.
        // `item.<FieldWithMetaString>map("getIdentifier", …)`) consumed by a NON-meta single
        // setter (which takes the bare item), INSIDE a mapItem/mapSingleToItem lambda. golden
        // hoists `final <Wrapper> <name> = <nav>.get();` through the LAMBDA_CHANNEL
        // (registerPendingLambdaHoist → compileLambda's drainPendingLambdaHoists converts the
        // expression lambda to a brace block) + derefs `(<name> == null ? null : <name>.getValue())`
        // — the #236 ctorSetterMetaDerefHoist analogue for a NAV value at the LAMBDA_CHANNEL seat
        // (#236 handles fn-call values at the STATEMENT_SINK). The fork spliced the wrapper BARE
        // (a FieldWithMetaX where the bare item is expected — non-compiling, so every carrier was
        // already a waivered mismatch → green-safe by construction).
        if (!attrMulti) {
            JavaExpression lambdaMetaDeref = hoistMetaDerefCtorNavInLambdaOrNull(
                    attr, pair, compiledExpr, src, ctx, gm, typeUtil, compiler);
            if (lambdaMetaDeref != null) {
                return lambdaMetaDeref;
            }
            // facet ctorNumericCoerceChainHoist (PR #332): a CHAIN value whose numeric
            // item type must convert to the attribute's numeric Java type — the hoist
            // form the #208 tryCtorNumericNarrow javadoc deferred ("a CHAIN (which the
            // golden HOISTS first, as in the mas Create_FloatingRate) DECLINES").
            JavaExpression chainNumeric = hoistNumericCoerceCtorChainOrNull(
                    attr, pair.value(), compiledExpr, src, ctx, gm, translator, typeUtil, compiler);
            if (chainNumeric != null) {
                return chainNumeric;
            }
        }
        // facet onlyElementMapperSRoundTrip (PR #345, W3): a ctor-setter value that is a
        // THEN-CHAIN whose tail is an only-element collapse over a PROVEN-MapperC pipe
        // (`… then distinct only-element`, MapGenericProductEconomicTerms cdm6) renders
        // golden's identity round-trip `MapperS.of(<mc>.get()).get()`. The compiled
        // collapse reaches this tail as a bare `.get()` text (its selfUnwrapping marker
        // was lost through the then-machinery), so the legacy fall-through below appended
        // ANOTHER `.get()` — the non-compiling `<…>.get().get()` (zero goldens carry a
        // doubled `.get()`), already waivered → green-safe by construction. The THEN-TAIL
        // gate keeps a DIRECT nav-collapse value on the legacy path (golden leaves those
        // bare — the #345 cp1 evaluate-arg over-fire lesson applied to this seat), and
        // the monotone multi-proof keeps every MapperS-pipe collapse declined.
        if (!attrMulti && HandlerHelper.collapsedMultiOnlyElementThenTail(pair.value(), compiler)) {
            // unwrapMapperSOf = the WHOLE-wrap test; a chain-rooted `MapperS.of(x)…get()`
            // is NOT pre-wrapped (the #345 cp1 lesson).
            String wrappedSrc = HandlerHelper.unwrapMapperSOf(src) != null
                    ? src
                    : "MapperS.of(" + src + ")";
            Set<JavaClass<?>> roundTripRefs = new HashSet<>(compiledExpr.getRefs());
            roundTripRefs.add(HandlerHelper.MAPPER_S);
            return JavaExpression.from(wrappedSrc + ".get()", null,
                    roundTripRefs, compiledExpr.getStaticWildcardImports());
        }
        // facet onlyElementCtorBareFnSplice (PR #345, W6): a SINGLE-attribute ctor-setter
        // value that is a FUNCTION invocation splices the compiled `<fn>.evaluate(…)`
        // VERBATIM — a bare invocation is already the callee's scalar output, never a
        // Mapper, so the fall-through's `.get()` rendered the non-compiling
        // `evaluate(…).get()` (a bare value has no .get(); zero green files carry it —
        // the ctor-arg mirror of the evaluate-arg argIsBareFnInvocation law, PR #132).
        // Golden: `.setReferenceEntity(mapLegalEntity.evaluate(item.get()))`
        // (MapCreditIndex cdm6). Wrapped invocations (MapperS.of(<fn>.evaluate(…)) with
        // the unwrap contract) took the structural branch above and never reach here.
        if (!attrMulti && pair.value() instanceof RSymbolReference ctorFnRef
                && ctorFnRef.symbol().filter(RFunction.class::isInstance).isPresent()) {
            return JavaExpression.from(src, null,
                    compiledExpr.getRefs(), compiledExpr.getStaticWildcardImports());
        }
        // facet innerCtorMapperSRoundTrip (PR #369, cluster E): a SINGLE ctor-setter
        // value that is ITSELF a nested CONSTRUCTOR — bare, or the FINAL body of a
        // restructured then-chain (`<nav> then AdjustableDates { … }`, the #368
        // ctorFieldNestedThenAdmit shape whose hoisted-thenArg consumption lands the
        // built inner ctor here) — renders golden's identity round-trip
        // `MapperS.of(<Inner>.builder()….build()).get()` (the #218 scalar wrap at the
        // nested-ctor setter-arg seat — MapAdjustableOrRelativeDates cdm6). The
        // compiled inner ctor is a bare built value, so the legacy fall-through's
        // `.get()` rendered the non-compiling `.build().get()` (a built POJO has no
        // get(); zero green files carry it — green-safe by construction). Mirrors the
        // #345 W3 whole-wrap guard: an already whole-wrapped value keeps its wrap.
        if (!attrMulti && ctorSetterValueEndsInCtor(pair.value())) {
            String wrappedSrc = HandlerHelper.unwrapMapperSOf(src) != null
                    ? src
                    : "MapperS.of(" + src + ")";
            Set<JavaClass<?>> roundTripRefs = new HashSet<>(compiledExpr.getRefs());
            roundTripRefs.add(HandlerHelper.MAPPER_S);
            return JavaExpression.from(wrappedSrc + ".get()", null,
                    roundTripRefs, compiledExpr.getStaticWildcardImports());
        }
        // facet thenWrappedDefaultRewrap (seat 28, law 4, rung A): a ctor-setter value
        // that is a THEN-chain whose BODY is a `default` renders upstream's identity
        // round-trip `MapperS.of(<item>).get()`. The `default` body is item-typed
        // (Mapper.getOrDefault(T) returns the VALUE - the #S2 law), but the enclosing
        // `then` is Mapper-typed, so upstream re-wraps the item and the ctor-setter's
        // own item extraction collapses it again: golden drr 7.x QuantityFrequency
        // `.setPeriod(MapperS.of(thenArg.getOrDefault(FrequencyPeriodEnum.ADHO)).get())`
        // against the fork's `.setPeriod(thenArg.getOrDefault(FrequencyPeriodEnum.ADHO)
        // .get())` - a `.get()` on a bare enum, which never compiled (LAW 74), so every
        // carrier is an already-waivered mismatch and no green file can carry the
        // fork's form. DISJOINT from the #S2 arm: that arm gates on the value NODE
        // being an RDefaultExpr (isThen=false - its live rows splice BARE); this rung's
        // value is an RThenExpr and its form is the round-trip. MEASURED
        // (PROBE28-F10d, 773 lines corpus-wide): thenBodyIsDefault=true occurs EXACTLY
        // 4 times, all band, all fn:QuantityFrequency - zero green. The cardinality
        // read needs no redirect: the probe measured valueCard=SINGLE on the RThenExpr
        // node itself. The whole-wrap guard is the #345-cp1 / #369 law verbatim - a
        // then body already rooted in MapperS.of must not be double-wrapped.
        if (!attrMulti && ctorSetterValueEndsInDefault(pair.value())
                && gm.workspace().getCardinality(pair.value()) != ExpressionCardinality.MULTI) {
            String wrappedSrc = HandlerHelper.unwrapMapperSOf(src) != null
                    ? src
                    : "MapperS.of(" + src + ")";
            Set<JavaClass<?>> roundTripRefs = new HashSet<>(compiledExpr.getRefs());
            roundTripRefs.add(HandlerHelper.MAPPER_S);
            return JavaExpression.from(wrappedSrc + ".get()", null,
                    roundTripRefs, compiledExpr.getStaticWildcardImports());
        }
        // facet ctorSetterElementwiseWrapperDeref (PR #373, F-eps): a MULTI meta-FREE
        // attribute whose Mapper-chain value carries META-WRAPPER items derefs
        // ELEMENTWISE before the `.getMulti()` collapse — the #349-S2
        // multiArgElementwiseWrapperDeref law at the CTOR-SETTER seat, routed through
        // the standard coercion (WrappedItemCoercer picks the form from the compiled
        // wrapper kind: MapperS → the null-guarded numbered deferred-coercion param,
        // MapperC → the bare unnumbered lambda — golden drr DTCC_UnderlyingAssetNameRule
        // `.<String>map("Type coercion", fieldWithMetaString0 -> fieldWithMetaString0 ==
        // null ? null : fieldWithMetaString0.getValue()).getMulti()` ×2 MapperS-guarded +
        // 1 MapperC-bare). Value-type equality with the attr element is the gate; a
        // meta-annotated attribute keeps the wrapper (its setter takes wrappers — the
        // #347 inverse law). Green-safe by construction: wrapper items spliced into a
        // plain-element List setter never compiled, so every carrier is an
        // already-waivered mismatch.
        // v3.2 seat 1 (F2 dropped-coercion, the chaos C19Make rows): a MULTI field whose value is
        // a LIST LITERAL of a NARROWER numeric item (`[1, 2, 3]` at a `number (0..*)` field) takes
        // upstream's wrapper→wrapper item conversion BEFORE the `.getMulti()` — the MapperC arm's
        // UNGUARDED map rung (a MapperC's items are non-null):
        // `.<BigDecimal>map("Type coercion", integer -> BigDecimal.valueOf(integer))`. The typed
        // channel is the literal's joined item (LiteralHandler.listItemJavaType — the SAME channel
        // the multi-output SET seat coerces list literals through, FunctionExpressionRenderer's
        // multiToMultiSet arm); a same-item list is identity. Corpus law: ZERO vendored goldens
        // carry the rung at a ctor list field (no real model writes an int literal list into a
        // number list) and the bare Integer list never compiled, so no green file moves.
        if (attrMulti && compiler != null && typeUtil != null && translator != null
                && compiler.getCoercionService() != null && ctx != null && ctx.scope() != null
                && pair.value() instanceof com.regnosys.rosetta.ast.expressions.literals.RListLiteral listLit
                && MetaFieldGenerator.detectMetaKind(attr) == MetaFieldGenerator.MetaKind.NONE) {
            JavaClass<?> attrElem = ctorAttrJavaType(attr, gm, translator);
            JavaClass<?> litJoined = LiteralHandler.listItemJavaType(listLit, compiler);
            // the element gate compares CANONICAL names - the SAME comparison the multi-output SET
            // arm it cites performs (FunctionExpressionRenderer's multiToMultiSet list-literal arm;
            // PR #622, the reviews' NIT-1): two JavaClass values for one class must agree
            if (attrElem != null && litJoined != null
                    && !attrElem.getCanonicalName().withDots().equals(litJoined.getCanonicalName().withDots())
                    && typeUtil.extendsNumber(litJoined) && typeUtil.extendsNumber(attrElem)) {
                JavaStatementBuilder coercedList = compiler.getCoercionService().coerce(
                        compiledExpr,
                        typeUtil.wrap(typeUtil.MAPPER_C, litJoined),
                        typeUtil.wrap(typeUtil.MAPPER_C, attrElem),
                        ctx.scope());
                // A non-expression coerce result (a block builder - no MapperC->MapperC conversion
                // produces one today) DECLINES to the arms below, the uncoerced splice; recorded
                // here, not silent - the SET arm's own decline note (Seat-1 #420 OBS-5).
                if (coercedList instanceof JavaExpression coercedListExpr) {
                    return JavaExpression.from(coercedListExpr.renderToString() + ".getMulti()", null,
                            coercedListExpr.getRefs(), coercedListExpr.getStaticWildcardImports());
                }
            }
        }
        if (attrMulti && compiler != null && typeUtil != null
                && compiledExpr.getExpressionType() != null
                && MetaFieldGenerator.detectMetaKind(attr) == MetaFieldGenerator.MetaKind.NONE
                && typeUtil.getItemType(compiledExpr.getExpressionType())
                        instanceof RJavaWithMetaValue chainWrapper) {
            JavaClass<?> attrElem = ctorAttrJavaType(attr, gm, translator);
            if (attrElem != null && attrElem.equals(chainWrapper.getValueType())) {
                // The #339 drained-chain re-root: the value's render roots at the bound
                // thenArg token (a deferred sentinel) whose DECL is MapperC, while the
                // per-element nav compile stamped the MapperS view — the wrapper KIND
                // follows the binding (render truth: the chain's rendered head IS the
                // binding's token), so the coercion emits upstream's BARE MapperC element
                // map (golden `fieldWithMetaString -> fieldWithMetaString.getValue()`),
                // not the guarded numbered MapperS form. An explicit chain rooted
                // elsewhere never matches the token prefix and keeps its stamped kind.
                JavaStatementBuilder coerceSource = compiledExpr;
                // facet thenBindingOverlay (v3.1 C2d retirement family 9, PR #616) — VERDICT-MOVED
                // RETIRE-AFTER-CENSUS -> JUSTIFIED-KEPT (the seat-30 bar; S26) on THREE independent
                // grounds, any one sufficient. c9 census, 45 arrivals (15 / 15 / 15), one carrier,
                // with bindingPresent=true and boundTypeNull=false at 45/45. (1) THE PROPOSED AST
                // SPINE CHANNEL IS INAPPLICABLE at 45/45: a bounded receiver-spine descent never
                // bottoms out on an implicit variable, and at the 15 arrivals where the render-root
                // test is TRUE the spine stops at DEPTH 0 because the value IS a bare symbol
                // reference — the re-root is a RENDER fact (2x2: T/T 0, T/F 15, F/T 0, F/F 30). (2)
                // MEASURED-INERT: the verdict is false at 45/45. (3) THE TRIAGE'S STATED
                // JUSTIFICATION IS REFUTED — the comment above says the two typed channels
                // "disagree by design", and boundKind == compiledKind at 45/45 (S+w/S+w 30, C+w/C+w
                // 15), so the #339 disagreement this row exists to arbitrate is UNPOPULATED at this
                // corpus, which is why the seat never fires. A triage JUSTIFICATION is a hypothesis
                // exactly as a triage VERDICT is.
                JavaExpression thenBinding = enclosingBoundThenArg(pair.value(), ctx);
                if (thenBinding != null && thenBinding.getExpressionType() != null
                        && typeUtil.isMapperC(thenBinding.getExpressionType())
                        && typeUtil.isMapperS(compiledExpr.getExpressionType())
                        && src.startsWith(thenBinding.renderToString() + ".")) {
                    coerceSource = JavaExpression.from(src,
                            typeUtil.wrap(typeUtil.MAPPER_C, chainWrapper),
                            compiledExpr.getRefs(), compiledExpr.getStaticWildcardImports());
                }
                if (compiler.coerceNavigationReceiver(coerceSource, ctx.scope(), attrElem)
                        instanceof JavaExpression derefedExpr) {
                    return JavaExpression.from(derefedExpr.renderToString() + ".getMulti()",
                            null, derefedExpr.getRefs(),
                            derefedExpr.getStaticWildcardImports());
                }
            }
        }
        // Mapper chain (navigation / conditional result) — extract at the attribute's
        // cardinality, mirroring upstream's expected-type collapse.
        return JavaExpression.from(src + (attrMulti ? ".getMulti()" : ".get()"), null,
                compiledExpr.getRefs(), compiledExpr.getStaticWildcardImports());
    }

    /**
     * facet innerCtorMapperSRoundTrip (PR #369, cluster E): true when a ctor-setter
     * VALUE terminates in a BARE nested CONSTRUCTOR — a bare {@link RConstructorExpr},
     * or a then-chain whose FINAL body ROOT is one (`<nav> then AdjustableDates {…}`).
     * The bare-root restructure hoists the chain's receiver into thenArg locals and
     * compiles the ctor STANDALONE (a built value — needs the MapperS.of round-trip);
     * the {@code then extract <ctor>} wrap deliberately DECLINES: it renders as an
     * in-chain {@code .map(… -> MapperS.of(<ctor>…))} lambda whose result is a real
     * Mapper chain taking the plain {@code .get()} (the GREEN MapQuantityMultiplier /
     * MapFxOptionToSettlementTerms / MapOptionStrikeReferenceSwapCurve forms — the
     * cp9 over-fire catch: admitting the extract wrap broke all three).
     */
    private static boolean ctorSetterValueEndsInCtor(RExpression value) {
        if (value instanceof RConstructorExpr) {
            return true;
        }
        if (value instanceof RThenExpr then) {
            RInlineFunction body = then.body().orElse(null);
            RExpression b = body == null ? null : body.body();
            return b instanceof RConstructorExpr;
        }
        return false;
    }

    /**
     * facet thenWrappedDefaultRewrap (seat 28, law 4): true when a ctor-setter VALUE is a
     * then-chain whose FINAL body ROOT is an {@link RDefaultExpr} - the `then default <x>`
     * shape. The `default` renders ITEM-typed (the #S2 law), the enclosing `then` is
     * Mapper-typed, so the seat takes the {@code MapperS.of(<item>).get()} identity
     * round-trip. A BARE {@link RDefaultExpr} value deliberately DECLINES - it belongs to
     * the #S2 arm and keeps its bare splice - and so does a then-chain whose body is
     * anything else (the cardinality fall-through owns those).
     */
    private static boolean ctorSetterValueEndsInDefault(RExpression value) {
        if (!(value instanceof RThenExpr then)) {
            return false;
        }
        RInlineFunction body = then.body().orElse(null);
        return body != null && body.body() instanceof RDefaultExpr;
    }

    /**
     * facet condListCoerce (PR #327, arm A1): the pre-compile attribute resolution for the
     * handshake — {@code null} (no handshake anywhere) unless some pair value IS a
     * conditional; otherwise the ctor type's attribute map, resolved WITHOUT compiling
     * (duplicating {@code tryTypedBuilderBlock}'s header, which runs only after the
     * values are compiled).
     */
    private static Map<String, RAttribute> condListPreAttrs(RConstructorExpr expr,
            List<RKeyValuePair> pairs, ExpressionCompiler compiler) {
        if (pairs.stream().noneMatch(p -> p.value() instanceof RConditionalExpr)
                || expr.typeCall() == null) {
            return null;
        }
        GeneratorModel gm = compiler.getGeneratorModel();
        if (gm == null) {
            return null;
        }
        RType resolved = gm.resolveTypeCall(expr.typeCall());
        RDataType dataType = null;
        if (resolved instanceof RDataTypeRef dtr) {
            dataType = dtr.astNode();
        } else if (resolved instanceof RChoiceTypeRef ctr) {
            dataType = ctr.asRDataType();
        }
        if (dataType == null) {
            return null;
        }
        // facet ctorChoiceSuperAttrs (PR #389): the SAME choice-super-aware walk
        // tryTypedBuilderBlock keys its pairs by — the condListCoerce pre-resolution
        // must see the identical attribute set or a conditional-valued choice-option
        // pair would miss its handshake (lockstep by construction; no such pair
        // exists on the corpus today).
        Map<String, RAttribute> attrsByName = new HashMap<>();
        for (RAttribute attr : HandlerHelper.allAttributesIncludingChoiceSuper(dataType)) {
            attrsByName.put(attr.name(), attr);
        }
        return attrsByName;
    }

    /**
     * facet condListCoerce (PR #327, arm A1): build the consumer→conditional handshake for
     * a MULTI attribute's conditional value — the List element / hoisted-local type split
     * mirrors {@link #hoistSingleValueIntoMultiOrNull}'s meta law, keyed on the THEN-arm
     * (the value actually coerced): a META attribute whose arm value provably carries
     * attribute-meta lists the wrapper with a wrapper local; a meta attribute with a
     * meta-FREE arm splits on the SEAT — see {@code ctorValueSetterSeat}; a non-meta
     * attribute lists the bare item. {@code null} (no handshake — today's bytes) on an
     * unresolved item type or absent type utility. Shared with the segment-ADD leaf seat
     * ({@code FunctionExpressionRenderer}).
     *
     * <p>facet ctorCondValueSetterList (PR #383): {@code ctorValueSetterSeat} keys the
     * meta-FREE-arm law on the CONSUMER seat, exactly as upstream's
     * {@code requiresValueAssignment} does for the setter choice. The CTOR key-value seat
     * ({@code true}) lists the BARE item end-to-end — golden consumes the bare value list
     * through the {@code set<Name>Value} setter ({@code final List<PriceSchedule>
     * ifThenElseResult0; … Collections.singletonList(priceSchedule); …
     * .setPriceScheduleValue(ifThenElseResult0)} — cdm6 MapSwapPayout), the SAME law
     * {@link #hoistSingleValueIntoMultiOrNull} already applies to a DIRECT meta-free value
     * at this seat, and {@link #ctorSetterName} agrees for free (a bare-element handshake
     * falls through its wrapper-element arm to the {@code Value} form). The segment-ADD
     * leaf seat ({@code false}) keeps the #190 wrap law byte-frozen —
     * {@code Wrapper.builder().setValue(local).build()} inside the singletonList consumed
     * by the PLAIN adder (golden {@code .addProductIdentifier(ifThenElseResult2)} — drr
     * Enrich_ReportableEventWithUpiFromAnnaDsb, the conditional channel's one GREEN wrap
     * carrier). Before #383 the ctor seat took the wrap law too, but it was LATENT (the
     * #327 SF-2 note: every A1 ctor carrier was non-meta) — MapSwapPayout, its first
     * carrier, proved golden takes the Value form there.
     */
    public static CondListCoerce condListCoerceFor(RAttribute attr, RConditionalExpr value,
            GeneratorModel gm,
            com.regnosys.rosetta.generator.java.types.JavaTypeTranslator translator,
            JavaTypeUtil typeUtil, boolean ctorValueSetterSeat) {
        if (gm == null || translator == null || typeUtil == null) {
            return null;
        }
        RType itemType = gm.getType(attr);
        if (itemType == null || itemType instanceof com.regnosys.rosetta.types.RMissingType) {
            return null;
        }
        JavaClass<?> baseType = translator.toJavaReferenceType(itemType);
        if (baseType == null) {
            return null;
        }
        MetaFieldGenerator.MetaKind metaKind = MetaFieldGenerator.detectMetaKind(attr);
        if (metaKind == MetaFieldGenerator.MetaKind.NONE) {
            return new CondListCoerce(value, baseType, baseType, false);
        }
        JavaClass<?> wrapper = RJavaWithMetaValue.create(
                metaKind == MetaFieldGenerator.MetaKind.REFERENCE_WITH_META, baseType, typeUtil);
        if (valueCarriesAttributeMeta(value.thenBranch())) {
            return new CondListCoerce(value, wrapper, wrapper, false);
        }
        return ctorValueSetterSeat
                ? new CondListCoerce(value, baseType, baseType, false)
                : new CondListCoerce(value, wrapper, baseType, true);
    }

    /**
     * facet ctorCondSingleCoerce (seat 28, law A): build the consumer->conditional handshake
     * for a SINGLE, meta-NONE attribute's conditional value. Unlike
     * {@link #condListCoerceFor} this factory carries NO decision - it supplies the CONSUMER's
     * expected item type and nothing else; the fire-vs-decline verdict belongs to
     * {@code ControlFlowHandler.condSingleCoerceOrNull}, which owns the ONE type walk
     * ({@code declWork}) both halves must agree on (LAW 69). Pushing a handshake the consumer
     * declines is byte-inert by construction: {@code fired} stays false, so
     * {@code coerceCtorArg}'s bypass does not engage and every existing arm runs unchanged.
     *
     * <p>{@code null} (no handshake - today's bytes) on:
     * <ul>
     *   <li>an absent translator (the partial-compiler guard {@link #condListCoerceFor} keeps);</li>
     *   <li>a META-annotated attribute - {@code detectMetaKind != NONE}. THIS IS THE SEAT-27
     *       GUARD: a meta attribute's Java type is the WRAPPER, while the hoist's own walk and
     *       the #377/#368 in-arm deref arms key on the wrapper's VALUE type, so re-typing the
     *       decl from the attribute would silence those arms (the measured 51-entered class -
     *       cdm 6.20.6 {@code MapUnitTypeWithScheme}'s three {@code FieldWithMetaXEnum} arms).
     *       {@code hoistNumericCoerceCtorChainOrNull} carries the identical guard at the
     *       non-conditional twin;</li>
     *   <li>an unresolved or missing attribute item type.</li>
     * </ul>
     * The same guard triple is mirrored at {@code HandlerHelper.ctorPairAttributeTypeOrNull}
     * (v3.1 flip seat 32, facet {@code ctorCondSingleCoerceLiteralArm}), which must key the
     * identical population.
     */
    public static CondSingleCoerce condSingleCoerceFor(RAttribute attr, RConditionalExpr value,
            GeneratorModel gm,
            com.regnosys.rosetta.generator.java.types.JavaTypeTranslator translator) {
        if (gm == null || translator == null) {
            return null;
        }
        if (MetaFieldGenerator.detectMetaKind(attr) != MetaFieldGenerator.MetaKind.NONE) {
            return null;
        }
        RType itemType = gm.getType(attr);
        if (itemType == null || itemType instanceof com.regnosys.rosetta.types.RMissingType) {
            return null;
        }
        JavaClass<?> attrJava = translator.toJavaReferenceType(itemType);
        return attrJava == null ? null : new CondSingleCoerce(value, attrJava);
    }

    /**
     * facet ctorArgArrayListCopy (PR #232): true iff the ctor value is a DIRECT
     * alias or function call — an {@link RSymbolReference} resolving to an
     * {@link RShortcut} (alias) or {@link RFunction} (function), i.e. the receiver
     * IS the call (no trailing feature navigation, which would be an
     * {@code RFeatureCall}). Such a multi value takes the defensive
     * {@code new ArrayList<>(…getMulti())} copy; a navigation chain declines (stays
     * bare). Mirrors the value classification in {@link #ctorValueItemIsBareSpliced}.
     */
    private static boolean ctorValueIsDirectCall(RExpression value) {
        return value instanceof RSymbolReference ref
                && ref.symbol()
                        .filter(s -> s instanceof RShortcut || s instanceof RFunction)
                        .isPresent();
    }

    /**
     * facet ctorSetterNumericNarrow (PR #208): coerce a SINGLE-cardinality numeric ctor
     * value whose item type is WIDER than the attribute's bounded-integer Java type to
     * the upstream null-guarded exact conversion
     * {@code (<value> == null ? null : <value>.intValueExact())} — the
     * {@code ConstructionHandler} setter analogue of {@link ReferenceHandler}'s evaluate-arg deref
     * ({@code tryMetaDerefArg}, PR #109/#128). Upstream compiles every ctor value against
     * the attribute's expected type; the fork spliced it BARE, so a {@code number}
     * (BigDecimal) value into an ISO20022 {@code int} setter (e.g. {@code setBsisPtSprd})
     * rendered the non-compiling bare reference — every carrier was already a waivered
     * mismatch, so activating the narrowing is green-safe by construction.
     *
     * <p>Gated to (1) a SIMPLE-identifier value — the null-guard evaluates {@code raw}
     * twice, so a Mapper-chain value (which the golden hoists to a {@code final} local
     * first, as in the mas {@code Create_FloatingRate}) DECLINES (stays waivered,
     * deferred to a future hoist facet); (2) a value/attribute pair the coercer narrows
     * to a bounded integer via a method-call exact conversion (the only shapes the
     * golden null-guards: BigDecimal/BigInteger/Long → Integer, BigDecimal/BigInteger →
     * Long); and (3) the coercion service producing a REAL (non-identity) conversion.
     * Returns {@code null} (the caller splices BARE) in every other case.
     */
    private JavaExpression tryCtorNumericNarrow(String raw, JavaExpression compiledExpr,
            RAttribute attr, RExpression valueExpr, ExpressionContext ctx, GeneratorModel gm,
            com.regnosys.rosetta.generator.java.types.JavaTypeTranslator translator,
            JavaTypeUtil typeUtil, ExpressionCompiler compiler, JavaStatementBuilder inner) {
        if (compiler == null || compiler.getCoercionService() == null) {
            return null;
        }
        // A simple Java identifier only — the null-guard references `raw` twice, so a
        // chained value would double-evaluate (the golden hoists those instead).
        if (raw.isEmpty() || !Character.isJavaIdentifierStart(raw.charAt(0))
                || !raw.chars().allMatch(Character::isJavaIdentifierPart)) {
            return null;
        }
        JavaClass<?> attrJava = ctorAttrJavaType(attr, gm, translator);
        JavaType compiledType = compiledExpr.getExpressionType();
        JavaType valueItem = compiledType == null ? null : typeUtil.getItemType(compiledType);
        // An ALIAS-call value (`MapperS.of(spreadOfLeg1Basis)`) renders with a null
        // expression type — recover the value item type from the rune inference
        // (the SAME signal ComparisonHandler.numericOperandKind reads for the literal-
        // typing law), so the alias's declared type and this narrowing cannot disagree.
        if (valueItem == null) {
            RMetaAnnotatedType inferred = gm.workspace().getInferredType(valueExpr);
            if (inferred != null && !inferred.isMissing()) {
                valueItem = translator.toJavaReferenceType(inferred.type());
            }
        }
        if (attrJava == null || valueItem == null || valueItem.equals(attrJava)) {
            return null;
        }
        // Only a narrowing to a bounded integer target — the conversions whose
        // throwOnFail=true forms are method calls (intValueExact / longValueExact /
        // Math.toIntExact) that the golden wraps in a null guard.
        boolean narrowing =
                (typeUtil.isInteger(attrJava) && (typeUtil.isBigDecimal(valueItem)
                        || typeUtil.isBigInteger(valueItem) || typeUtil.isLong(valueItem)))
                || (typeUtil.isLong(attrJava) && (typeUtil.isBigDecimal(valueItem)
                        || typeUtil.isBigInteger(valueItem)));
        if (!narrowing) {
            return null;
        }
        JavaExpression rawRef = JavaExpression.from(raw, valueItem,
                inner.getRefs(), inner.getStaticWildcardImports());
        JavaExpression conv = compiler.getCoercionService()
                .coerceExpression(rawRef, valueItem, attrJava, true, ctx.scope());
        String convStr = conv.renderToString();
        if (convStr.equals(raw)) {
            return null;
        }
        Set<JavaClass<?>> refs = new HashSet<>(inner.getRefs());
        refs.addAll(conv.getRefs());
        Set<JavaClass<?>> wildcards = new HashSet<>(inner.getStaticWildcardImports());
        wildcards.addAll(conv.getStaticWildcardImports());
        return JavaExpression.from("(" + raw + " == null ? null : " + convStr + ")",
                null, refs, wildcards);
    }

    /**
     * facet {@code ctorNumericCoerceChainHoist} (PR #332) — coerce a ctor-setter CHAIN
     * value (a Mapper navigation the {@code .get()} fall-through would splice bare) whose
     * numeric item type must convert to the attribute's numeric Java type, by hoisting the
     * collapsed value to a statement local and consuming the null-guarded conversion:
     *
     * <pre>
     * final &lt;Item&gt; &lt;item&gt; = &lt;chain&gt;.get();
     * … .set&lt;Attr&gt;((&lt;item&gt; == null ? null : &lt;item&gt;.intValueExact())) …
     * </pre>
     *
     * <p>The CHAIN counterpart of {@link #tryCtorNumericNarrow} (whose identifier gate
     * declines chains — a chained value would double-evaluate in the guard, so the golden
     * hoists it; that javadoc named mas {@code Create_FloatingRate} as the deferred
     * carrier). Both conversion DIRECTIONS are golden-witnessed at this seat: narrowing
     * {@code BigDecimal → Integer} ({@code bigDecimal.intValueExact()} —
     * {@code Create_FloatingRate} {@code setBsisPtSprd}, cdm6
     * {@code MapEquityMultipleExercise} {@code setMinimum/MaximumNumberOfOptions}, the
     * numbered {@code bigDecimal0}/{@code bigDecimal1} group) and widening
     * {@code Integer → BigDecimal} ({@code BigDecimal.valueOf(integer)} — cdm6
     * {@code MapParametricDates} {@code setDayFrequency}); the widening leg extends the
     * #208 narrowing set with the one witnessed pair only. The local registers on the
     * nearest statement-hoist sink ({@link StatementHoistSession} group = the lowercased
     * item simple name — bare for a singleton, {@code base0..n-1} otherwise), exactly like
     * {@link #hoistMetaDerefCtorValueOrNull}.
     *
     * <p>Declines (bare {@code .get()} splice — today's non-compiling bytes, already a
     * waivered mismatch → green-safe by construction) when: the attribute is
     * meta-annotated (its setter takes the wrapper); either type fails to resolve or the
     * pair is not a witnessed numeric conversion; neither a
     * statement-hoist sink nor the #356 lambda channel is reachable (facet
     * numericCoerceChainInLambdaHoist: a lambda-interior seat registers the decl on the
     * pending-lambda-hoist channel with the #355 per-lambda sub-group naming, under the
     * suppression / drainable-lambda / unbound-then render-truth declines); the derived
     * base name is not a Java identifier; or the coercion service returns the identity.
     *
     * <p>facet ctorSetterNumericNarrowChain (PR #361): the former MULTI-LINE decline
     * ("the single-line hoist decl form is the only golden-witnessed shape") is
     * FALSIFIED by the iosco v1/v2/v3 PeriodicPaymentRule trio — golden hoists the
     * multi-line chain verbatim ({@code final BigDecimal bigDecimal0 = thenArg4
     * \n\t.mapSingleToItem(…).get();}, the relative CHAIN_LINK continuation
     * re-anchored +1 by the drain), interleaved per ctor VALUE right after its
     * source thenArg statement (registration order — each value's chain hoist then
     * its narrow hoist, so the interleave falls out of the per-value compile order
     * for free). The gate is dropped; the drain sites re-anchor newlines at their
     * own indent (the #179 relative-form law), so no new emission machinery.
     */
    private JavaExpression hoistNumericCoerceCtorChainOrNull(RAttribute attr,
            RExpression valueExpr,
            JavaExpression compiledExpr, String src, ExpressionContext ctx, GeneratorModel gm,
            com.regnosys.rosetta.generator.java.types.JavaTypeTranslator translator,
            JavaTypeUtil typeUtil, ExpressionCompiler compiler) {
        if (ctx == null || ctx.scope() == null || typeUtil == null || translator == null
                || compiler == null || compiler.getCoercionService() == null) {
            return null;
        }
        if (MetaFieldGenerator.detectMetaKind(attr) != MetaFieldGenerator.MetaKind.NONE) {
            return null;
        }
        JavaClass<?> attrJava = ctorAttrJavaType(attr, gm, translator);
        JavaType compiledType = compiledExpr.getExpressionType();
        JavaType valueItem = compiledType == null ? null : typeUtil.getItemType(compiledType);
        // A navigation-chain ctor value renders with a null expression type at this
        // seat — recover the item type from the rune inference, exactly like
        // tryCtorNumericNarrow's alias-value recovery (the SAME signal, so the
        // declared type and this coercion cannot disagree).
        if (valueItem == null) {
            RMetaAnnotatedType inferred = gm.workspace().getInferredType(valueExpr);
            if (inferred != null && !inferred.isMissing()) {
                valueItem = translator.toJavaReferenceType(inferred.type());
            }
        }
        // A navigation chain the workspace inference also left untyped (the cdm6
        // MapParametricDates `daysModel(...) -> ... -> dayNumber` shape, an
        // alias-rooted chain whose feature the linker left unresolved) still knows
        // its item type from the render-truth walk: the SAME gm-aware
        // NavigationHandler.resolveReceiverRType the witness emission resolves the
        // terminal step's type through, so this coercion and the rendered
        // `<Integer>map(...)` witness cannot disagree.
        if (valueItem == null) {
            RType chainType = NavigationHandler.resolveReceiverRType(valueExpr, gm, compiler);
            if (chainType != null
                    && !(chainType instanceof com.regnosys.rosetta.types.RMissingType)) {
                valueItem = translator.toJavaReferenceType(chainType);
            }
        }
        if (attrJava == null || !(valueItem instanceof JavaClass<?> itemClass)
                || valueItem.equals(attrJava)) {
            return null;
        }
        boolean coercible =
                (typeUtil.isInteger(attrJava) && (typeUtil.isBigDecimal(valueItem)
                        || typeUtil.isBigInteger(valueItem) || typeUtil.isLong(valueItem)))
                || (typeUtil.isLong(attrJava) && (typeUtil.isBigDecimal(valueItem)
                        || typeUtil.isBigInteger(valueItem)))
                || (typeUtil.isBigDecimal(attrJava) && typeUtil.isInteger(valueItem));
        if (!coercible) {
            return null;
        }
        JavaStatementScope sink = ctx.scope().findStatementHoistSink();
        // facet numericCoerceChainInLambdaHoist (PR #356): the LAMBDA_CHANNEL twin — the
        // same hoist+guard for a ctor-setter chain value INSIDE a map/extract lambda
        // (golden GetPackg ×6: `final BigDecimal bigDecimal = <chain>.get();` before the
        // return + `.setBsisPtSprd((bigDecimal == null ? null : bigDecimal.intValueExact()))`).
        // The decl registers on the enclosing lambda's pending-hoist channel
        // (registerPendingLambdaHoist as a #346-safe raw-token statement) named by the
        // per-METHOD session's per-LAMBDA sub-groups (registerLambdaScoped — the #355 F-1
        // naming law: singleton bare / 0..n-1 consumption order / per-name `_`-escape).
        // Render-truth declines (→ the bare splice, today's non-compiling bytes): value-then
        // SUPPRESSION (a statement cannot splice into an inline-ternary arm — the P352A
        // law); a non-drainable enclosing lambda; a seat inside an UNRESTRUCTURED runtime
        // `.then(` lambda (the #350 thenArgRefFor handshake read — the #355 cp3 class); or
        // no reachable session/boundary.
        StatementHoistSession lambdaSession = null;
        JavaStatementScope lambdaBoundary = null;
        if (sink == null) {
            if (ctx.scope().isThenValueHoistSuppressed()
                    || !HandlerHelper.isInsideDrainableMapLambdaAllowingConditionalArms(valueExpr)) {
                return null;
            }
            // facet thenBindingOverlay (v3.1 C2d retirement family 9, PR #616) — JUSTIFIED-KEPT
            // CONFIRMED (S25, occurrence 1 of 2), MEASURED-INERT: the belt never trips. c9 census
            // at this occurrence, 552 arrivals (184 / 184 / 184) = 276 entry + 276 pass, with
            // sawUnbound=false, unboundDepth=-1 and boundCountOnPath=0 at 276/276,
            // chainTopFlag=false at 552/552 and sinkPresent=false at every entry — the `return
            // null` fires on NO walk. The triage's stated reason, emission-ORDER dependence, is
            // UNTESTED here because the gate never fires, so this keep stands on the measured
            // inertness plus the LAW-69 tie to the second occurrence below and to the
            // byte-identical third site at ControlFlowHandler:2191. Carriers: fn:GetPackg 324 +
            // rule:PeriodicPayment 228.
            RNode curThen = valueExpr.parent();
            int thenDepth = 0;
            while (curThen != null && thenDepth++ < 64) {
                if (curThen instanceof RInlineFunction inlineThen
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
        String itemSimpleName = itemClass.getSimpleName();
        String baseName = JavaNamingUtil.toFirstLower(itemSimpleName);
        if (!javax.lang.model.SourceVersion.isIdentifier(baseName)) {
            return null;
        }
        // Probe the conversion with the UNREGISTERED base name first — a coercion the
        // service does not implement (identity) must not leak a session registration
        // (a phantom group member would renumber sibling same-base hoists, the #170
        // discarded-registration lesson). Only a real conversion registers.
        JavaExpression probeRef = JavaExpression.from(baseName, valueItem,
                compiledExpr.getRefs(), compiledExpr.getStaticWildcardImports());
        if (compiler.getCoercionService()
                .coerceExpression(probeRef, valueItem, attrJava, true, ctx.scope())
                .renderToString().equals(baseName)) {
            return null;
        }
        String sentinel = sink != null
                ? sink.statementHoistSession().register(baseName)
                : lambdaSession.registerLambdaScoped(baseName, lambdaBoundary);
        JavaExpression localRef = JavaExpression.from(sentinel, valueItem,
                compiledExpr.getRefs(), compiledExpr.getStaticWildcardImports());
        JavaExpression conv = compiler.getCoercionService()
                .coerceExpression(localRef, valueItem, attrJava, true, ctx.scope());
        String convStr = conv.renderToString();
        String declText = "final " + itemSimpleName + " " + sentinel + " = " + src + ".get();";
        Set<JavaClass<?>> declRefs = new HashSet<>(compiledExpr.getRefs());
        declRefs.add(itemClass);
        if (sink != null) {
            sink.registerStatementHoist(declText);
        } else {
            // facet numericCoerceChainInLambdaHoist (PR #356): the raw-token statement —
            // no GeneratedIdentifier renders at drain time (the #346 scope-close law).
            lambdaBoundary.registerPendingLambdaHoist(new JavaRawStatement(declText,
                    declRefs, compiledExpr.getStaticWildcardImports()));
        }
        Set<JavaClass<?>> refs = new HashSet<>(declRefs);
        refs.addAll(conv.getRefs());
        Set<JavaClass<?>> wildcards = new HashSet<>(compiledExpr.getStaticWildcardImports());
        wildcards.addAll(conv.getStaticWildcardImports());
        return JavaExpression.from("(" + sentinel + " == null ? null : " + convStr + ")",
                null, refs, wildcards);
    }

    /**
     * facet cat16BindEnumSeats (PR #452): re-qualify a BIND-STAMPED bare enum ctor value by the
     * attribute's CHILD enum — the RESOLVED sibling of {@link #tryCtorEnumQualify}, mirroring
     * {@code ComparisonHandler.tryInheritedEnumRequalify} (PR #215) at the ctor-setter seat. The
     * engine's Category-16 expected-type bind resolves {@code identifierType: Name} to the
     * attribute enum's {@link REnumValue}, whose DECLARING enum may be a SUPER of the attribute's
     * ({@code AssetIdTypeEnum extends ProductIdTypeEnum} — {@code Name}/{@code Other}
     * parent-declared); the resolved render (ReferenceHandler's REnumValue branch) qualified —
     * before v3.1 flip seat 12 — by the declaring parent and registered the PARENT's import,
     * where golden qualifies by the attribute's child enum, whose generated Java flattens
     * inherited values under its own name (the #211/#358 flatten law). Since seat 12 the root
     * arm itself qualifies by the node's INFERRED = expected enum ({@code HandlerHelper
     * .boundEnumInferredOwner}), so this rung recomputes the same answer and its parent-import
     * swap is a no-op on a ref set that already holds the child.
     *
     * <p>Gates: the value is a bind-stamped bare reference ({@link HandlerHelper#boundBareEnumValue});
     * the attribute's RType is an {@link REnumTypeRef}; and the attribute enum's hierarchy flattens
     * the EXACT bound instance ({@code findEnumValueInHierarchy(en, name) == bound} — the #215
     * same-instance descend-only law, so a same-name value bound on an UNRELATED enum keeps its
     * resolved rendering). On fire: renders {@code ChildEnum.CONSTANT}, swaps the declaring
     * parent's import on the carried refs for the child's (an own-declared bound value nets a
     * no-op swap and identical bytes, so pre-#452 carriers cannot move). Returns {@code null}
     * everywhere else — every other value shape keeps today's route byte-for-byte.
     */
    private JavaExpression tryCtorBoundEnumRequalify(RKeyValuePair pair, RAttribute attr,
            GeneratorModel gm, com.regnosys.rosetta.generator.java.types.JavaTypeTranslator translator,
            JavaExpression compiledExpr) {
        if (translator == null) {
            return null;
        }
        com.regnosys.rosetta.ast.supporting.REnumValue bound =
                HandlerHelper.boundBareEnumValue(pair.value());
        if (bound == null) {
            return null;
        }
        RType at = gm.getType(attr);
        if (!(at instanceof REnumTypeRef enumRef)) {
            return null;
        }
        REnumeration en = enumRef.astNode();
        REnumValue match = HandlerHelper.findEnumValueInHierarchy(en, bound.name());
        if (match == null || match != bound) {
            return null;
        }
        Set<JavaClass<?>> refs = new HashSet<>(compiledExpr.getRefs());
        if (bound.parent() instanceof REnumeration declaring) {
            // the resolved render contributed the DECLARING enum's import; the child supersedes it.
            JavaClass<?> parentClass = translator.toJavaReferenceType(new REnumTypeRef(declaring));
            if (parentClass != null) {
                refs.remove(parentClass);
            }
        }
        JavaClass<?> childClass = translator.toJavaReferenceType(new REnumTypeRef(en));
        if (childClass != null) {
            refs.add(childClass);
        }
        return JavaExpression.enumConstant(en.name() + "." + EnumHelper.convertValue(match),
                null, refs, compiledExpr.getStaticWildcardImports());
    }

    /**
     * facet ctorSetterEnumQualify (PR #209): qualify a BARE unresolved enum value used as a
     * ctor-setter argument to its Java enum constant {@code EnumName.CONSTANT} + import — the
     * {@code ConstructionHandler} setter analogue of {@link ComparisonHandler}'s
     * {@code tryBareEnumComparand} (PR #154/#206). A bare enum value (e.g. {@code style:
     * European}) parses as an UNRESOLVED {@link RSymbolReference} (enum values are not in
     * function scope), so the fork renders the bare rune name {@code European} — a non-compiling
     * undefined symbol; every carrier was therefore already a waivered mismatch, so qualifying it
     * is green-safe by construction.
     *
     * <p>Unlike the comparison-operand {@code enumQualify} (which recovers the enum from a sibling
     * operand), the ctor-setter target enum is known DIRECTLY from the attribute's RType, so no
     * receiver-chain inference is needed. Gated to (1) a value NODE that is an unresolved bare enum
     * reference ({@link HandlerHelper#bareEnumValueName}: an unresolved no-arg {@link RSymbolReference},
     * or an {@link REnumValueRef} with empty
     * {@code enumeration()}/{@code resolvedSymbol()}/{@code resolvedAttributeChain()}) — a resolved
     * enum-typed variable carries a present symbol and renders correctly, so it is excluded (a
     * BIND-STAMPED enum value — facet cat16BindEnumSeats, PR #452 — renders as a plain
     * {@link JavaExpression} with NO unwrap contract and is re-qualified by the standalone
     * {@code tryCtorBoundEnumRequalify} arm BEFORE the unwrap ladder ever runs, so it never
     * reaches this method) — OR
     * (PR #239) a TYPE-SHADOWED bare value ({@link HandlerHelper#typeShadowEnumValueName}: the simple
     * name collides with a model type, so it resolved its symbol to that type); and (2) the
     * attribute's RType being an {@link REnumTypeRef} whose enum DECLARES a value of the bare name —
     * the (2) match is the load-bearing gate that keeps a genuine type reference from being rewritten.
     * Returns {@code null} (the caller splices BARE) in every other case.
     */
    private JavaExpression tryCtorEnumQualify(RKeyValuePair pair, RAttribute attr,
            GeneratorModel gm, com.regnosys.rosetta.generator.java.types.JavaTypeTranslator translator,
            ExpressionCompiler compiler, JavaStatementBuilder inner) {
        if (translator == null) {
            return null;
        }
        // A STRICT bare enum value (unresolved RSymbolReference / disguised REnumValueRef), or
        // facet PR #239: a TYPE-SHADOWED bare enum value whose simple name collides with a model
        // type so it resolved its symbol to that TYPE (e.g. `priceType: CashPrice` where CashPrice
        // is both the PriceTypeEnum.CashPrice value AND the CashPrice data type) — bareEnumValueName
        // declines, typeShadowEnumValueName admits the candidate name, and the attribute-enum
        // value-match below is the load-bearing gate that keeps a genuine type reference from being
        // rewritten.
        String valueName = HandlerHelper.bareEnumValueName(pair.value());
        if (valueName == null) {
            valueName = HandlerHelper.typeShadowEnumValueName(pair.value());
        }
        if (valueName == null) {
            return null;
        }
        RType at = gm.getType(attr);
        if (!(at instanceof REnumTypeRef enumRef)) {
            return null;
        }
        REnumeration en = enumRef.astNode();
        // facet enumQualifyInherited (PR #211): the bare value may be declared on a SUPER-enum
        // (e.g. AssetIdTypeEnum extends ProductIdTypeEnum — `Other`/`Name` are inherited, not
        // direct). The generated Java enum FLATTENS inherited values under the CHILD name, so
        // golden qualifies AssetIdTypeEnum.OTHER (en — the attribute's declared enum), NOT the
        // declaring parent; we therefore search the whole extends chain but keep `en` as the
        // qualifier. #209 searched only en.values() (direct) and missed the inherited carriers.
        // facet enumQualifyInherited (PR #211) — the bare value may be declared on a super-enum,
        // FLATTENED under the child name in the generated Java enum; the shared helper walks the
        // extends chain (promoted to HandlerHelper at PR #215, also used by the comparison-operand
        // re-qualification arm). The caller always qualifies with the child enum name {@code en.name()}.
        REnumValue match = HandlerHelper.findEnumValueInHierarchy(en, valueName);
        if (match != null) {
            Set<JavaClass<?>> refs = new HashSet<>(inner.getRefs());
            JavaClass<?> enumClass = translator.toJavaReferenceType(new REnumTypeRef(en));
            if (enumClass != null) {
                refs.add(enumClass);
            }
            return JavaExpression.enumConstant(en.name() + "." + EnumHelper.convertValue(match),
                    null, refs, inner.getStaticWildcardImports());
        }
        return null;
    }

    /**
     * facet {@code ctorSingletonListHoist} — coerce a SINGLE-cardinality ctor value into a
     * MULTI (List) attribute by hoisting it to a statement local and consuming the null-guarded
     * {@code Collections.<Item>emptyList()} / {@code singletonList} ternary:
     *
     * <pre>
     * final &lt;Item&gt; &lt;name&gt; = &lt;call&gt;;
     * … .set&lt;Attr&gt;((&lt;name&gt; == null ? Collections.&lt;Item&gt;emptyList()
     *                                : Collections.singletonList(&lt;name&gt;))) …
     * </pre>
     *
     * <p>The local registers on the nearest statement-hoist sink ({@link StatementHoistSession},
     * name group = the lowercased item-type simple name — bare for a singleton, {@code base0..n-1}
     * otherwise — exactly like the #177 {@code bigInteger} / #173 {@code ifThenElseResult} hoists);
     * {@code FunctionExpressionRenderer.prependStatementHoists} lifts the {@code final} declaration
     * ahead of the assignment. The item type is the attribute's translated Java item type,
     * meta-wrapped via {@link RJavaWithMetaValue#create} when the attribute carries a
     * {@code [metadata …]} annotation (PR #186/#193) AND the value provably carries
     * attribute-meta itself — i.e. exactly when {@link #ctorSetterName} picks the PLAIN
     * {@code set<Name>} (which takes {@code List<FieldWithMetaX>}); a meta attribute with a
     * meta-FREE value takes upstream's {@code set<Name>Value} ({@code List<RawValue>}), so the
     * local stays the BARE item type (the same {@link #valueCarriesAttributeMeta} gate the
     * setter-name uses — the local TYPE and the SETTER chosen must agree). {@code raw} is the
     * already-unwrapped value render ({@code <call>}, the {@code MapperS.of} wrap stripped by the
     * caller's {@code unwrapToBuilder}).
     *
     * <p>Returns {@code null} — declining the whole constructor to the legacy placeholder (today's
     * bytes) — when no sink is reachable AND the seat is not inside a drainable map/extract
     * lambda (the rule/report path never opens a session; a NON-drainable lambda interior
     * still declines — facet inLambdaCtorSingletonHoist, PR #366, routes the drainable-lambda
     * interior through the per-lambda channel instead), the type utility is absent, the
     * attribute item type is unresolved, or the derived base name is not identifier-shaped.
     * The decline keeps the file byte-identical to pre-facet output (and the non-compiling
     * {@code MapperS.of(value)} into a {@code List} setter means it was already a waivered
     * mismatch).
     */
    private JavaExpression hoistSingleValueIntoMultiOrNull(RAttribute attr, RKeyValuePair pair,
            JavaStatementBuilder inner, String raw, ExpressionContext ctx, GeneratorModel gm,
            com.regnosys.rosetta.generator.java.types.JavaTypeTranslator translator,
            JavaTypeUtil typeUtil, JavaExpression compiledExpr, ExpressionCompiler compiler) {
        if (ctx == null || ctx.scope() == null || typeUtil == null) {
            return null;
        }
        JavaStatementScope sink = ctx.scope().findStatementHoistSink();
        boolean lambdaChannel = false;
        if (sink == null) {
            // facet inLambdaCtorSingletonHoist (PR #366): the LAMBDA_CHANNEL sibling —
            // inside a drainable map/extract lambda the SAME hoist+guard renders at the
            // lambda top (compileLambda's drainPendingLambdaHoists converts the body to
            // a block: `item -> { final AssignedIdentifier _assignedIdentifier = …;
            // return MapperS.of(Identifier.builder().setAssignedIdentifier((
            // _assignedIdentifier == null ? Collections.<AssignedIdentifier>emptyList()
            // : Collections.singletonList(_assignedIdentifier))) …); }` — golden
            // MapEventIdentifier cdm6, whose STATEMENT-seat sibling in the same file
            // already renders through the sink path above). Naming rides the
            // #170/#237 deferred-coercion machinery (the #312/#360 in-lambda ctor
            // producer pattern) so an outer statement-seat local claiming the bare
            // name escapes this one to `_<name>` at unified finalization — exactly
            // golden's `assignedIdentifier` (outer) / `_assignedIdentifier` (lambda).
            if (isInsideDrainableMapLambda(pair.value())) {
                lambdaChannel = true;
            } else {
                return null;
            }
        }
        RType itemType = gm.getType(attr);
        if (itemType == null || itemType instanceof com.regnosys.rosetta.types.RMissingType) {
            return null;
        }
        JavaClass<?> baseType = translator.toJavaReferenceType(itemType);
        if (baseType == null) {
            return null;
        }
        // Mirror ctorSetterName: the local TYPE must match the SETTER chosen. A
        // meta-annotated attribute uses the wrapper (set<Name> -> List<FieldWithMetaX>)
        // ONLY when the value provably carries attribute-meta; a meta-free value takes
        // upstream's set<Name>Value -> List<RawValue>, where the local stays the BARE
        // item type. Otherwise the singletonList element type would mismatch the setter's
        // List element type (non-compiling).
        MetaFieldGenerator.MetaKind metaKind = MetaFieldGenerator.detectMetaKind(attr);
        // facet setterNameValueMeta (PR #327, facet B): the SAME widened proof
        // ctorSetterName reads (B1 meta-alias / B2 compiled-wrapper / B3 list-literal) —
        // the local TYPE and the SETTER chosen must stay in lock-step.
        // v3.1 flip seat 30, law 8: the THIRD value-side proof lands here too. This
        // method's own comment states the invariant - "the local TYPE must match the SETTER
        // chosen ... otherwise the singletonList element type would mismatch the setter's
        // List element type (non-compiling)" - so widening ctorSetterName's proof without
        // widening this one is exactly the divergence LAW 69 forbids. Byte-neutral for the
        // law's measured carrier (UnderlierProductIdentifier's `identifier` is (1..1), so it
        // never reaches the single-into-multi hoist); the whole-matrix checkpoint measures
        // whether any other file does. If one enters, NARROW THE SHARED PREDICATE - do not
        // split these two halves apart again.
        boolean wrapAsMeta = metaKind != MetaFieldGenerator.MetaKind.NONE
                && (valueCarriesAttributeMeta(pair.value(), compiler)
                        || compiledValueIsMetaWrapped(compiledExpr, compiler)
                        || valueRecoversAttributeMeta(pair.value(), compiler));
        JavaClass<?> declType = wrapAsMeta
                ? RJavaWithMetaValue.create(
                        metaKind == MetaFieldGenerator.MetaKind.REFERENCE_WITH_META,
                        baseType, typeUtil)
                : baseType;
        String itemSimpleName = declType.getSimpleName();
        String baseName = JavaNamingUtil.toFirstLower(itemSimpleName);
        if (!javax.lang.model.SourceVersion.isIdentifier(baseName)) {
            return null;
        }
        // facet fqnWitness (PR #227): emit the singletonList item type (the local-var decl type
        // AND the Collections.<X>emptyList() witness) as a first-claim-wins sentinel carrying the
        // canonical name. A meta wrapper (FieldWithMetaX) never collides with an fpml witness, so
        // only the plain (non-meta) base type is sentinel-wrapped; the render-order resolver
        // decides bare-vs-FQN, resolving byte-identically when there is no collision.
        String itemTypeText = wrapAsMeta
                ? itemSimpleName
                : ImportCollisionResolver.typeRef(declType.getCanonicalName().withDots());
        // facet ctorSingletonListBareParam (PR #359, F-9): a value that is ALREADY a
        // bare identifier (a function param — `alphaUTI`) hoists NOTHING: golden
        // guards the identifier directly (no re-binding local, no session entry —
        // Create_AlphaTerminationWorkflowStepFromBetaAndGamma's setTradeId). A bare
        // identifier is single-evaluation-safe by construction; ZERO goldens carry
        // the hoisted-rebind form for this producer (corpus-greped), so the elision
        // cannot regress a green file. Compound values keep the hoist.
        if (javax.lang.model.SourceVersion.isIdentifier(raw)) {
            Set<JavaClass<?>> bareRefs = new HashSet<>(inner.getRefs());
            bareRefs.add(declType);
            bareRefs.add(HandlerHelper.COLLECTIONS);
            return JavaExpression.from(
                    "(" + raw + " == null ? Collections.<" + itemTypeText
                            + ">emptyList() : Collections.singletonList(" + raw + "))",
                    null, bareRefs, inner.getStaticWildcardImports());
        }
        if (lambdaChannel) {
            // facet inLambdaCtorSingletonHoist (PR #366): the decl registers on the
            // per-lambda channel (compileLambda drains it as the block's leading
            // statement); the shared deferred-name token ties the decl and the guard
            // splice to ONE finalized name (the #333 decl-use-consistency law), and the
            // unified resolution escapes it `_<name>` when an outer statement-seat
            // local claimed the bare name (golden MapEventIdentifier).
            GeneratedIdentifier lamId = ctx.scope().createUniqueIdentifier(baseName);
            String lamToken = ctx.scope().registerDeferredCoercionName(lamId);
            Set<JavaClass<?>> lamRefs = new HashSet<>(inner.getRefs());
            lamRefs.add(declType);
            lamRefs.add(HandlerHelper.COLLECTIONS);
            ctx.scope().registerPendingLambdaHoist(new CtorSingletonListHoist(
                    itemTypeText, lamToken, raw, lamRefs, inner.getStaticWildcardImports()));
            return JavaExpression.from(
                    "(" + lamToken + " == null ? Collections.<" + itemTypeText
                            + ">emptyList() : Collections.singletonList(" + lamToken + "))",
                    null, lamRefs, inner.getStaticWildcardImports());
        }
        String sentinel = sink.statementHoistSession().register(baseName);
        sink.registerStatementHoist("final " + itemTypeText + " " + sentinel + " = " + raw + ";");
        Set<JavaClass<?>> refs = new HashSet<>(inner.getRefs());
        refs.add(declType);
        refs.add(HandlerHelper.COLLECTIONS);
        return JavaExpression.from(
                "(" + sentinel + " == null ? Collections.<" + itemTypeText
                        + ">emptyList() : Collections.singletonList(" + sentinel + "))",
                null, refs, inner.getStaticWildcardImports());
    }

    /**
     * facet inLambdaCtorSingletonHoist (PR #366): the marker for a ctor single-into-multi
     * value hoisted through the LAMBDA channel ({@code hoistSingleValueIntoMultiOrNull}'s
     * lambda branch). Renders the decl through the DEFERRED token — the
     * {@code CollectionHandler.DeepThenArgHoist} token-statement pattern, NOT the
     * eager-{@code GeneratedIdentifier} form — so the decl and the guard splice finalize
     * to ONE name through {@code resolveUnifiedDeferredNames} (the #333
     * decl-use-consistency law: an eager decl render resolves on the ORIGINAL scope
     * WITHOUT the #329 session seeding and stays bare while the seeded consumer escapes
     * — the cp3 `assignedIdentifier` decl vs `_assignedIdentifier` use split). Kept a
     * distinct class so arm-scoped drains can instanceof-gate producers independently.
     */
    public static final class CtorSingletonListHoist
            extends com.regnosys.rosetta.generator.java.statement.JavaStatement {
        private final String declType;
        private final String token;
        private final String declValue;
        private final Set<JavaClass<?>> refs;
        private final Set<JavaClass<?>> wildcards;

        CtorSingletonListHoist(String declType, String token, String declValue,
                Set<JavaClass<?>> refs, Set<JavaClass<?>> wildcards) {
            this.declType = declType;
            this.token = token;
            this.declValue = declValue;
            this.refs = refs;
            this.wildcards = wildcards;
        }

        @Override
        public void render(StringBuilder sb) {
            sb.append("final ").append(declType).append(' ')
              .append(token).append(" = ")
              .append(declValue).append(";\n");
        }

        @Override
        public Set<JavaClass<?>> getRefs() {
            return refs;
        }

        @Override
        public Set<JavaClass<?>> getStaticWildcardImports() {
            return wildcards;
        }
    }

    /**
     * facet {@code ctorAsKeyReference} (PR #390) — render an {@code as-key} ctor field
     * as the upstream meta-key copy: the reference-only wrapper builder consuming a
     * hoisted (wrapper, value) local pair. The
     * {@code FunctionExpressionRenderer.renderAsKeySetOrNull} (#328 F1) law at the
     * CONSTRUCTOR seat — the first {@link RKeyValuePair#isAsKey()} generator consumer
     * (the segment-SET seat reads {@code ROperation.isAsKey()}):
     *
     * <pre>
     * final ReferenceWithMetaParty referenceWithMetaParty0 = &lt;chain&gt;.get();
     * final Party reportingParty = referenceWithMetaParty0 == null ? null : referenceWithMetaParty0.getValue();
     * … .setReportingParty(ReferenceWithMetaParty.builder()
     *     .setGlobalReference(Optional.ofNullable(reportingParty)
     *         .map(r -&gt; r.getMeta())
     *         .map(m -&gt; m.getGlobalKey())
     *         .orElse(null))
     *     .setExternalReference(…getExternalKey()…)
     *     .build()) …
     * </pre>
     *
     * <p>Golden anchors: drr Create_ReportingSideFromReportableEvent (4 as-key pairs —
     * the wrapper locals number {@code referenceWithMetaParty0..3} through the
     * statement-hoist session group, interleaved w/v per pair by the #327
     * consumption-position window) and cdm5/cdm6 Create_PartyChange (one pair per
     * alias method — the per-method alias session leaves the wrapper local BARE; the
     * #376 aliasLadderArmThenHoist drain lands both decls INSIDE the owning
     * if-branch). The VALUE local is the ctor FIELD name emitted literally
     * (unregistered — the #328 precedent: golden never numbers it and no corpus
     * field name collides with a session group base); the WRAPPER local registers on
     * the session (name group = the lowercased wrapper simple name — the #236 law).
     *
     * <p>The compiled chain moves INTO the wrapper hoist string, so the returned
     * arg carries the chain's refs (plus the wrapper, the bare value type and
     * {@code Optional}) — a plain hoist string carries no refs of its own.
     *
     * <p>Declines (null — the caller's arms keep today's whole-wrapper splice, a
     * waivered mismatch in every carrier): a non-as-key pair; an attribute that is
     * not single-cardinality REFERENCE_WITH_META; a MULTI value (upstream's
     * element-wise as-key form — 0 corpus carriers); a compiled item type that is
     * not the attribute's own wrapper (render-truth lockstep); a wrap-factory value
     * (a bare identifier needs no hoist — 0 corpus carriers at this seat); no
     * reachable statement-hoist sink (lambda interiors — 0 carriers).
     */
    private JavaExpression tryCtorAsKeyReference(RAttribute attr, RKeyValuePair pair,
            JavaExpression compiledExpr, ExpressionContext ctx, GeneratorModel gm,
            com.regnosys.rosetta.generator.java.types.JavaTypeTranslator translator,
            JavaTypeUtil typeUtil, ExpressionCompiler compiler) {
        if (!pair.isAsKey()) {
            return null;
        }
        if (ctx == null || ctx.scope() == null || typeUtil == null || translator == null
                || gm == null || gm.workspace() == null) {
            return null;
        }
        if (MetaFieldGenerator.detectMetaKind(attr) != MetaFieldGenerator.MetaKind.REFERENCE_WITH_META) {
            return null;
        }
        JavaClass<?> bareType = ctorAttrJavaType(attr, gm, translator);
        if (bareType == null) {
            return null;
        }
        boolean valueMulti =
                gm.workspace().getCardinality(pair.value()) == ExpressionCardinality.MULTI;
        if (gm.isMulti(attr)) {
            // facet ctorAsKeyReference (PR #437, finding #35 — the MULTI arm): a MULTI
            // as-key value into a MULTI reference attr appends the shared element-wise
            // stream tail to the compiled MapperC receiver (upstream assignAsKey's
            // multi arm; golden func-ctor-as-key-ref `.setAttrMulti(MapperC
            // .<TypeWithKey>of(MapperS.of(key), MapperS.of(key))\n\t.getItems()…)` —
            // the SAME tail as the segment-ADD seat, HandlerHelper.asKeyMultiStreamTail,
            // one SOT). A single value into the MULTI attr declines (no witness).
            if (!valueMulti) {
                return null;
            }
            RJavaWithMetaValue multiWrapper = RJavaWithMetaValue.create(true, bareType, typeUtil);
            String item = ctx.scope().disambiguate("item");
            Set<JavaClass<?>> multiRefs = new HashSet<>(compiledExpr.getRefs());
            multiRefs.add(multiWrapper);
            multiRefs.add(HandlerHelper.COLLECTORS);
            String multiValue = HandlerHelper.render(compiledExpr)
                    + HandlerHelper.asKeyMultiStreamTail(item, multiWrapper.getSimpleName());
            return JavaExpression.from(multiValue, null, multiRefs,
                    compiledExpr.getStaticWildcardImports());
        }
        if (valueMulti) {
            return null;
        }
        JavaType exprType = compiledExpr.getExpressionType();
        JavaType itemType = exprType == null ? null : typeUtil.getItemType(exprType);
        JavaClass<?> attrWrapper = RJavaWithMetaValue.create(true, bareType, typeUtil);
        boolean itemIsWrapper = itemType instanceof RJavaWithMetaValue compiledWrapper
                && compiledWrapper.getSimpleName().equals(attrWrapper.getSimpleName());
        if (compiledExpr.unwrapToBuilder().isPresent()) {
            // facet ctorAsKeyReference (PR #437, finding #35 — the two IDENTIFIER
            // arms): a bare no-arg symbol value (the wrap-factory shape the #390 arm
            // declined) renders the single Optional-chain reference copy DIRECTLY:
            // (a) a value whose compiled item type IS the attr wrapper (a meta input —
            //     golden func-ctor-as-key-meta) hoists the upstream value deref into
            //     the ctor-FIELD-named local (`final Foo b = myInput == null ? null :
            //     myInput.getValue();`) and the chain reads the LOCAL;
            // (b) a value of the BARE keyed type itself (golden func-ctor-as-key-ref
            //     `attrSingle: key as-key`) needs NO hoist — the chain reads the
            //     identifier (`Optional.ofNullable(key)…`).
            // Any other wrap-factory shape keeps today's bytes.
            if (!(pair.value() instanceof RSymbolReference symValue)
                    || !symValue.args().isEmpty()) {
                return null;
            }
            JavaStatementBuilder innerValue = compiledExpr.unwrapToBuilder().get();
            String name = HandlerHelper.render(innerValue);
            if (!javax.lang.model.SourceVersion.isIdentifier(name)) {
                // THE FLIPPED-ALIAS as-key pair (PR #607, the widened pair gate's
                // defect 6 — a BEHAVIOURAL divergence, drr 7.0–7.3): the optimised
                // route's value-typed alias invocation arrives here as the structural
                // `MapperS.of(reportingParty(reportableEvent))` bridge, so the
                // unwrap channel is present but the inner render is a CALL, not an
                // identifier, and this arm declined — the pair fell through to the
                // `...Value(` setter and the optimised tree set the VALUE where the
                // reference (the seat-33 E.4 arm, on the Mapper-returning alias)
                // copies the global/external KEYS: `$.getReportingSide` non-null on
                // one side only, the harness's catch. The twin arm below.
                return tryCtorAsKeyFlippedAliasReference(symValue, pair, innerValue, name,
                        bareType, attrWrapper, ctx, gm, translator, compiler);
            }
            Set<JavaClass<?>> idRefs = new HashSet<>(innerValue.getRefs());
            idRefs.add(attrWrapper);
            idRefs.add(HandlerHelper.OPTIONAL);
            if (itemIsWrapper) {
                JavaStatementScope idSink = ctx.scope().findStatementHoistSink();
                String fieldLocal = pair.key();
                if (idSink == null || !javax.lang.model.SourceVersion.isIdentifier(fieldLocal)) {
                    return null;
                }
                idSink.registerStatementHoist("final " + bareType.getSimpleName() + " "
                        + fieldLocal + " = " + name + " == null ? null : "
                        + name + ".getValue();");
                idRefs.add(bareType);
                return JavaExpression.from(
                        asKeySingleOptionalChain(attrWrapper.getSimpleName(), fieldLocal),
                        null, idRefs, innerValue.getStaticWildcardImports());
            }
            boolean itemIsBare = itemType instanceof JavaClass<?> itemClass
                    && !(itemType instanceof RJavaWithMetaValue)
                    && itemClass.getSimpleName().equals(bareType.getSimpleName());
            if (!itemIsBare && itemType == null) {
                // The compiled type can be absent for a bare input read — the AST
                // channel answers instead: the symbol resolves to a META-FREE
                // RAttribute whose translated type IS the attr's bare keyed type
                // (the #436 input-identity-walk law at the value-type seat).
                if (symValue.symbol().orElse(null) instanceof RAttribute inputAttr
                        && MetaFieldGenerator.detectMetaKind(inputAttr)
                                == MetaFieldGenerator.MetaKind.NONE) {
                    RType inputType = gm.getType(inputAttr);
                    if (inputType != null
                            && !(inputType instanceof com.regnosys.rosetta.types.RMissingType)) {
                        JavaClass<?> inputJava = translator.toJavaReferenceType(inputType);
                        itemIsBare = inputJava != null
                                && inputJava.getSimpleName().equals(bareType.getSimpleName());
                    }
                }
            }
            if (itemIsBare) {
                return JavaExpression.from(
                        asKeySingleOptionalChain(attrWrapper.getSimpleName(), name),
                        null, idRefs, innerValue.getStaticWildcardImports());
            }
            return null;
        }
        // The pre-#437 #390 Mapper-chain arm — byte-frozen. Render-truth lockstep:
        // the compiled chain's item type must BE the attribute's own reference
        // wrapper (both carriers agree; a mismatched or null-typed value keeps
        // today's bytes).
        if (!itemIsWrapper) {
            // facet ctorAsKeyBareValueReference (v3.1 flip seat 33, law E.234 rung E.4): the
            // BARE-VALUE as-key pair - an `as-key` ctor field whose value is an ALIAS CALL
            // that already yields the keyed type's BARE value, so the compiled chain's item
            // can never BE the attribute's reference wrapper and the #390 arm above declines.
            // GOLDEN <- FORK: golden hoists the bare value into a local and copies the meta
            // KEYS into a reference-only wrapper builder (`final Party _reportingParty =
            // reportingParty(reportableEvent).get();` ... `.setReportingParty(
            // ReferenceWithMetaParty.builder().setGlobalReference(Optional.ofNullable(
            // _reportingParty).map(r -> r.getMeta()).map(m -> m.getGlobalKey()).orElse(null))
            // ...)`) where the fork splices the whole VALUE into the `...Value(` overload
            // (`.setReportingPartyValue(reportingParty(reportableEvent).get())`).
            // Carrier: Enrich_TransactionReportInstructionTestPackDefault's `reportingSide`
            // alias x drr 7.0-7.3, four pairs per file - 42 of the file's 52 diff lines.
            //
            // A BYTE law, said plainly: the fork's form COMPILES (the generated builder
            // carries both the wrapper setter and the `...Value` overload). The divergence is
            // SEMANTIC - the fork sets the VALUE where upstream's assignAsKey copies the
            // global/external REFERENCE - and the green argument is a corpus scan, not a
            // compiler verdict: of the 174,141 goldens under test-corpus, 115 carry the
            // as-key reference idiom `setGlobalReference(Optional.ofNullable(` and ZERO of
            // those 115 carry any `.set<X>Value(` call. No golden mixes the two forms.
            //
            // THE ADMITTER IS `!itemIsWrapper` and nothing else needs to discriminate:
            // measured over the 76 as-key ctor pairs in the corpus, `itemIsWrapper=false`
            // selects EXACTLY the 16 carrier rows (4 pairs x 4 cells) and the other 60 - the
            // GREEN `Create_ReportingSideFromReportableEvent` (40) and `Create_PartyChange`
            // (20) - all read `itemIsWrapper=true` and keep the #390 arm. The two conjuncts
            // below are DEFENCE-IN-DEPTH with their zeros recorded (LAW 82): all 16 carriers
            // read `exprType=null` (the alias call renders type-less by construction) and ALL
            // 76 read a reachable sink, so `bvSink != null` excludes nothing today; `unwrap`
            // is not re-tested because the `unwrapToBuilder().isPresent()` block above returns
            // unconditionally - that conjunct is discharged by position.
            //
            // THE NAME CHANNEL is GeneratorScope.disambiguate, which is public and documented
            // NON-MUTATING and whose escape loop is `"_" + name`: it returns golden's four
            // names verbatim and in golden's order (`_reportingParty`,
            // `_partyResponsibleForReporting`, `_reportSubmittingParty` - each colliding with
            // the alias METHOD of the same name - and the un-escaped `reportingCounterparty`,
            // whose ctor key matches no method). registerDeferredCoercionParam would enrol the
            // name in the numbered 0..n-1 coercion-param group, which golden does not do here,
            // and the #390/#437 arms' LITERAL `pair.key()` locals stay untouched (measured:
            // every one of the 60 green rows reads the key un-escaped).
            JavaStatementScope bvSink = ctx.scope().findStatementHoistSink();
            if (exprType == null && bvSink != null) {
                String bvLocal = ctx.scope().disambiguate(pair.key());
                if (javax.lang.model.SourceVersion.isIdentifier(bvLocal)) {
                    bvSink.registerStatementHoist("final " + bareType.getSimpleName() + " "
                            + bvLocal + " = " + HandlerHelper.render(compiledExpr) + ".get();");
                    Set<JavaClass<?>> bvRefs = new HashSet<>(compiledExpr.getRefs());
                    bvRefs.add(attrWrapper);
                    bvRefs.add(bareType);
                    bvRefs.add(HandlerHelper.OPTIONAL);
                    return JavaExpression.from(
                            asKeySingleOptionalChain(attrWrapper.getSimpleName(), bvLocal),
                            null, bvRefs, compiledExpr.getStaticWildcardImports());
                }
            }
            return null;
        }
        JavaStatementScope sink = ctx.scope().findStatementHoistSink();
        if (sink == null) {
            return null;
        }
        String wrapperSimpleName = attrWrapper.getSimpleName();
        String wrapperBase = JavaNamingUtil.toFirstLower(wrapperSimpleName);
        String fieldLocal = pair.key();
        if (!javax.lang.model.SourceVersion.isIdentifier(wrapperBase)
                || !javax.lang.model.SourceVersion.isIdentifier(fieldLocal)) {
            return null;
        }
        String wSentinel = sink.statementHoistSession().register(wrapperBase);
        sink.registerStatementHoist("final " + wrapperSimpleName + " " + wSentinel + " = "
                + HandlerHelper.render(compiledExpr) + ".get();");
        sink.registerStatementHoist("final " + bareType.getSimpleName() + " " + fieldLocal + " = "
                + wSentinel + " == null ? null : " + wSentinel + ".getValue();");
        Set<JavaClass<?>> refs = new HashSet<>(compiledExpr.getRefs());
        refs.add(attrWrapper);
        refs.add(bareType);
        refs.add(HandlerHelper.OPTIONAL);
        return JavaExpression.from(
                asKeySingleOptionalChain(wrapperSimpleName, fieldLocal),
                null, refs, compiledExpr.getStaticWildcardImports());
    }

    /**
     * THE FLIPPED-ALIAS as-key arm (PR #607, defect 6) — the optimised route's twin
     * of the seat-33 E.4 bare-value arm. E.4 admits the Mapper-returning alias call
     * (reference route: type-less, no unwrap channel) and hoists
     * {@code final Party _reportingParty = reportingParty(reportableEvent).get();}
     * before the key-copy builder. When the alias is emitted VALUE-typed (the § 6.3
     * seam — {@code aliasValueSeamOrNull} non-null, which the reference route never
     * answers: this arm is byte-inert there by construction), the invocation already
     * yields the bare value and rides the structural {@code MapperS.of(...)} bridge,
     * so the hoist is the call itself, no {@code .get()}, and the key copy reads the
     * local exactly as E.4's does — the SAME as-key semantics on both routes (upstream
     * assignAsKey copies the meta keys; a value setter would embed the object).
     *
     * <p>Admits exactly: a bare alias reference ({@code reportingParty as-key}) whose
     * shortcut belongs to the enclosing function and is flipped SINGLE, whose
     * inferred type is the attribute's own bare keyed type (meta-free — the golden
     * form for E.4's carriers is the bare local), with a reachable statement sink.
     * Declines (null) keep the standing bytes. Carriers: drr 7.0–7.3
     * {@code Enrich_TransactionReportInstructionTestPackDefault.reportingSide}, four
     * pairs per file — the only as-key ctor pairs in the corpus whose value is a
     * flipped alias (the reach census over all twenty overlays: 4 files).
     */
    private JavaExpression tryCtorAsKeyFlippedAliasReference(RSymbolReference symValue,
            RKeyValuePair pair, JavaStatementBuilder innerValue, String innerRender,
            JavaClass<?> bareType, JavaClass<?> attrWrapper, ExpressionContext ctx,
            GeneratorModel gm,
            com.regnosys.rosetta.generator.java.types.JavaTypeTranslator translator,
            ExpressionCompiler compiler) {
        if (compiler == null) {
            return null;
        }
        RFunction enclosing = HandlerHelper.findEnclosingFunction(pair);
        if (enclosing == null) {
            return null;
        }
        RShortcut alias = symValue.symbol().filter(RShortcut.class::isInstance)
                .map(RShortcut.class::cast).orElse(null);
        if (alias == null) {
            // The resolver leaves alias references symbol-empty on some paths — the
            // same name-based resolution ReferenceHandler's alias arm uses.
            for (RShortcut sc : enclosing.shortcuts()) {
                if (sc.name().equals(symValue.name())) {
                    alias = sc;
                    break;
                }
            }
        }
        if (alias == null) {
            return null;
        }
        // The NON-COUNTING predicate twin: this seat READS the seam's authority, the
        // invocation seat (ReferenceHandler's alias arm) already tallied the one leaf
        // bridge wrap — a counting query here moved the pinned wrap breakdown by
        // exactly the four carrier pairs (S=1850 -> 1854 on drr 7.3.0, the first run).
        Boolean seamMulti = compiler.aliasValueSeamIsMultiOrNull(enclosing, alias);
        if (seamMulti == null || seamMulti) {
            return null;
        }
        RMetaAnnotatedType aliasType = gm.workspace().getInferredType(pair.value());
        if (aliasType == null || aliasType.isMissing() || aliasType.hasMeta()) {
            return null;
        }
        JavaClass<?> aliasJava = translator.toJavaReferenceType(aliasType.type());
        // CANONICAL equality, not simple-name (the review's catch — the import-collision
        // family this PR's defects 1 and 3 exist for: an fpml `Party` into a cdm `Party`
        // attribute must decline, never hoist a same-named other type).
        if (aliasJava == null || !aliasJava.getCanonicalName().withDots()
                .equals(bareType.getCanonicalName().withDots())) {
            return null;
        }
        JavaStatementScope sink = ctx.scope().findStatementHoistSink();
        if (sink == null) {
            return null;
        }
        String local = ctx.scope().disambiguate(pair.key());
        if (!javax.lang.model.SourceVersion.isIdentifier(local)) {
            return null;
        }
        sink.registerStatementHoist("final " + bareType.getSimpleName() + " " + local + " = "
                + innerRender + ";");
        Set<JavaClass<?>> refs = new HashSet<>(innerValue.getRefs());
        refs.add(attrWrapper);
        refs.add(bareType);
        refs.add(HandlerHelper.OPTIONAL);
        return JavaExpression.from(
                asKeySingleOptionalChain(attrWrapper.getSimpleName(), local),
                null, refs, innerValue.getStaticWildcardImports());
    }

    /**
     * facet ctorAsKeyReference — the single-form as-key reference copy (upstream
     * assignAsKey's single arm): the reference-only wrapper builder reading the
     * subject's meta keys through null-safe {@code Optional} chains — GLOBAL first
     * then external (the OPPOSITE of the multi stream tail's external-first order,
     * both upstream's own). One SOT for the #390 Mapper-chain arm and the #437
     * identifier arms. Relative {@code \n\t}/{@code \n\t\t} continuations — the
     * ctor emit re-anchors. The caller adds the wrapper + {@code Optional} refs.
     */
    private static String asKeySingleOptionalChain(String wrapperSimpleName, String subject) {
        return wrapperSimpleName + ".builder()"
                + "\n\t.setGlobalReference(Optional.ofNullable(" + subject + ")"
                + "\n\t\t.map(r -> r.getMeta())"
                + "\n\t\t.map(m -> m.getGlobalKey())"
                + "\n\t\t.orElse(null))"
                + "\n\t.setExternalReference(Optional.ofNullable(" + subject + ")"
                + "\n\t\t.map(r -> r.getMeta())"
                + "\n\t\t.map(m -> m.getExternalKey())"
                + "\n\t\t.orElse(null))"
                + "\n\t.build()";
    }

    /**
     * facet {@code ctorSetterMetaDerefHoist} (PR #236) — coerce a SINGLE-cardinality ctor
     * value that PROVABLY produces an attribute-meta wrapper
     * ({@code FieldWithMeta*}/{@code ReferenceWithMeta*}) into a NON-meta setter (which
     * takes the bare item) by hoisting the wrapper to a statement local and consuming the
     * null-guarded {@code getValue()} deref:
     *
     * <pre>
     * final &lt;Wrapper&gt; &lt;name&gt; = &lt;call&gt;;
     * … .set&lt;Attr&gt;((&lt;name&gt; == null ? null : &lt;name&gt;.getValue())) …
     * </pre>
     *
     * <p>The value is a direct function call / extract whose callee output carries a
     * value-level {@code [metadata …]} annotation ({@link #valueProvenMetaKind}); the
     * wrapper element type is the SETTER's bare Java type ({@link #ctorAttrJavaType}) and
     * the {@code FieldWithMeta}/{@code ReferenceWithMeta} shell is the value's proven meta
     * kind ({@link RJavaWithMetaValue#create}). The value is referenced TWICE (the null
     * check + the {@code getValue}), so it cannot stay inline — the local registers on the
     * nearest statement-hoist sink ({@link StatementHoistSession}, name group = the
     * lowercased wrapper simple name — bare for a singleton, {@code base0..n-1} otherwise —
     * exactly like {@link #hoistSingleValueIntoMultiOrNull} and the #173/#177 hoists);
     * {@code FunctionExpressionRenderer.prependStatementHoists} lifts the {@code final}
     * declaration ahead of the assignment. {@code raw} is the already-unwrapped value
     * render ({@code <call>}, the {@code MapperS.of} wrap stripped by the caller's
     * {@code unwrapToBuilder}).
     *
     * <p>Returns {@code null} — declining to the bare splice (today's non-compiling bytes,
     * already a waivered mismatch → green-safe by construction) — when the attribute is
     * itself meta-annotated (its setter takes the wrapper, no deref), the value does not
     * provably carry value-level meta, no statement-hoist sink is reachable (the
     * rule/report path never opens a session; a lambda interior stops the walk), or the
     * bare item type / derived base name is unresolved.
     */
    private JavaExpression hoistMetaDerefCtorValueOrNull(RAttribute attr, RKeyValuePair pair,
            JavaStatementBuilder inner, String raw, ExpressionContext ctx, GeneratorModel gm,
            com.regnosys.rosetta.generator.java.types.JavaTypeTranslator translator,
            JavaTypeUtil typeUtil) {
        if (ctx == null || ctx.scope() == null || typeUtil == null || translator == null) {
            return null;
        }
        // The setter must take the BARE item — a meta-annotated attribute's setter takes
        // the wrapper itself, so there is nothing to deref.
        if (MetaFieldGenerator.detectMetaKind(attr) != MetaFieldGenerator.MetaKind.NONE) {
            return null;
        }
        // facet metaIteArmWrapperTyping (PR #328, F4): a CONDITIONAL value declines —
        // valueProvenMetaKind now proves through conditional arms, but upstream's
        // bare-consumer form derefs the meta arm INSIDE the branch of the (bare-typed)
        // ite-hoist, NOT via this post-hoc local deref (cdm6 MapUnitTypeWithScheme;
        // the in-branch deref form is a deferred lead — declining keeps its
        // pre-#328 bytes).
        if (pair.value() instanceof RConditionalExpr) {
            return null;
        }
        MetaFieldGenerator.MetaKind valueKind = valueProvenMetaKind(pair.value());
        if (valueKind == MetaFieldGenerator.MetaKind.NONE) {
            return null;
        }
        JavaClass<?> bareType = ctorAttrJavaType(attr, gm, translator);
        if (bareType == null) {
            return null;
        }
        JavaStatementScope sink = ctx.scope().findStatementHoistSink();
        if (sink == null) {
            return null;
        }
        JavaClass<?> wrapper = RJavaWithMetaValue.create(
                valueKind == MetaFieldGenerator.MetaKind.REFERENCE_WITH_META, bareType, typeUtil);
        String wrapperSimpleName = wrapper.getSimpleName();
        String baseName = JavaNamingUtil.toFirstLower(wrapperSimpleName);
        if (!javax.lang.model.SourceVersion.isIdentifier(baseName)) {
            return null;
        }
        String sentinel = sink.statementHoistSession().register(baseName);
        sink.registerStatementHoist("final " + wrapperSimpleName + " " + sentinel + " = " + raw + ";");
        Set<JavaClass<?>> refs = new HashSet<>(inner.getRefs());
        refs.add(wrapper);
        return JavaExpression.from(
                "(" + sentinel + " == null ? null : " + sentinel + ".getValue())",
                null, refs, inner.getStaticWildcardImports());
    }

    /**
     * facet ctorSetterMetaDerefLambda (PR #312): hoist + null-safe deref a ctor-setter value that
     * is a NAVIGATION producing a meta wrapper ({@link RJavaWithMetaValue} compiled item type)
     * consumed by a NON-meta single setter, INSIDE a map/extract lambda — the LAMBDA_CHANNEL
     * counterpart of {@link #hoistMetaDerefCtorValueOrNull} (which serves fn-call values at a
     * statement-hoist sink). golden hoists {@code final <Wrapper> <name> = <nav>.get();} at the
     * lambda top + derefs {@code (<name> == null ? null : <name>.getValue())}; the value is
     * referenced twice (null check + {@code getValue}) so it cannot stay inline. The hoist
     * registers on the lambda-body scope ({@code registerPendingLambdaHoist});
     * {@code CollectionHandler.compileLambda} drains it into the brace block. The naming rides the
     * #170/#237/#309 deferred-coercion machinery (one {@link GeneratedIdentifier} shared by the
     * decl — resolved at render — and the sentinel-embedded deref — resolved at finalization — so
     * both agree).
     *
     * <p>Declines (bare splice, today's still-waivered non-compiling bytes → green-safe by
     * construction) when: the attribute is itself meta-annotated (its setter takes the wrapper);
     * the value is MULTI (a
     * {@code .get()} collapse would be invalid); the compiled value's item type is not a concrete
     * meta wrapper (a co-occupied conditional value surfaces a null item type + only REF wrappers,
     * so it declines here — except a TYPE-LESS bare implicit-item value, which recovers its
     * wrapper through the #362 {@code bareItemThenPipeMetaType} binding channel per facet
     * effElseCtorSetterMetaDeref, PR #398); a statement-hoist sink IS reachable (a top-level rule/function ctor value
     * routes through {@link #hoistMetaDerefCtorValueOrNull} instead); the enclosing lambda is not a
     * drainable map/extract lambda ({@code RExtractExpr}); or the derived base name is not a Java
     * identifier.
     */
    private JavaExpression hoistMetaDerefCtorNavInLambdaOrNull(RAttribute attr, RKeyValuePair pair,
            JavaExpression compiledExpr, String src, ExpressionContext ctx, GeneratorModel gm,
            JavaTypeUtil typeUtil, ExpressionCompiler compiler) {
        if (ctx == null || ctx.scope() == null || typeUtil == null) {
            return null;
        }
        // The setter must take the BARE item — a meta-annotated attribute's setter takes the
        // wrapper itself, so there is nothing to deref.
        if (MetaFieldGenerator.detectMetaKind(attr) != MetaFieldGenerator.MetaKind.NONE) {
            return null;
        }
        // facet ctorSetterMetaDerefFunctionHost (v3.1 flip seat 33, law A.1, rung 1): the #232
        // RULE gate is DROPPED. It was a blast-radius hold, never a discriminator - the FUNCTION
        // host reaches this seat with every OTHER conjunct of the #312 ladder already passing,
        // and the RULE path renders golden's exact shape today (the #360 javadoc below names
        // drr esma/fca IdentifierOfBasketConstituentsRule, and [P32-CTORHOIST] measured that
        // rule firing on the identical configuration - LAW 69, the two halves of one question
        // agree). golden `.setIdentifier((fieldWithMetaString0 == null ? null :
        // fieldWithMetaString0.getValue()))` <- fork
        // `.setIdentifier(item.<FieldWithMetaString>map("getIdentifier", ...).get())`: a
        // FieldWithMetaString handed to a String setter, which does not compile (LAW 74,
        // javac33 line 533).
        //
        // MEASURED radius ZERO over the whole matrix on BOTH routes ([P32-CTORHOIST], 28,150
        // rows, identical group set off/on): `encRule=- wrapper=true card=SINGLE sinkNull=true
        // drainable=true attrMeta=NONE` = 4 rows / 1 `where=` = drr 7.0.0/7.1.0/7.2.0/7.3.0
        // GetBasketConstituents's basket arm and nothing else. The discrimination lives in the
        // conjuncts that remain: the non-meta attribute test above, the MULTI decline below,
        // the compiled-item wrapper proof (with the #398 bareItemThenPipeMetaType recovery for
        // a type-less bare implicit item), the LAMBDA_CHANNEL sink-null gate and the
        // drainable-map-lambda gate - the last two are what keep a rule/report top-level value
        // on hoistMetaDerefCtorValueOrNull's #236 statement-hoist sink instead of this one.
        // A MULTI value would collapse with .getMulti(), not .get() — decline (the deref hoist is
        // the single-value shape).
        if (gm.workspace().getCardinality(pair.value()) == ExpressionCardinality.MULTI) {
            return null;
        }
        // The compiled value's item type must be a CONCRETE meta wrapper — the nav's `<Wrapper>map`
        // witness surfaces it directly on the MapperS item type; a co-occupied conditional value
        // leaves the item type null (only REF wrappers) and declines here.
        JavaType exprType = compiledExpr.getExpressionType();
        RJavaWithMetaValue wrapper = null;
        if (exprType != null) {
            JavaType itemType = typeUtil.getItemType(exprType);
            if (itemType instanceof RJavaWithMetaValue typedWrapper) {
                wrapper = typedWrapper;
            }
        } else if (pair.value()
                instanceof com.regnosys.rosetta.ast.expressions.references.RImplicitVariable iv) {
            // facet effElseCtorSetterMetaDeref (PR #398), consumer arm: a BARE implicit-item
            // setter value compiles TYPE-LESS, but its render-true then-pipe binding (the
            // #362 compiled-type channel — never an AST recovery) proves the wrapper when
            // the piped decl element is meta (the #398 elselessLadderWrapperJoin makes the
            // DTCC consumer's `ifThenElseResult` a MapperC<FieldWithMetaString>, so the
            // mapItem's `_item.get()` is the wrapper needing golden's hoist + guarded
            // deref: `final FieldWithMetaString fieldWithMetaString = _item.get();`).
            // A bare-element or unbound pipe returns null — today's bytes.
            wrapper = HandlerHelper.bareItemThenPipeMetaType(iv, ctx.scope(), compiler);
        }
        if (wrapper == null) {
            return null;
        }
        // LAMBDA_CHANNEL only: no statement-hoist sink is reachable (a lambda interior stops the
        // walk). A top-level rule/function ctor value HAS a sink (the #262 session) and routes
        // through hoistMetaDerefCtorValueOrNull.
        if (ctx.scope().findStatementHoistSink() != null) {
            return null;
        }
        // The enclosing lambda must be a drainable map/extract lambda (compileLambda drains pending
        // lambda hoists for RExtractExpr map lambdas — the mapItem/mapSingleToItem seat). A
        // non-drainable enclosing lambda would orphan the decl → decline.
        // facet ctorNavMetaDerefBlockArm (PR #360): the CONDITIONAL-ARM seat admits when the
        // FIRST RConditionalExpr ancestor is REGISTERED on the #354 blockArmSeatConditionals
        // channel — true ONLY while compileElselessConditionalBlock is compiling that
        // conditional's arm (the block form drains the marker-classed decl INSIDE the owning
        // if-branch — golden's placement, drr esma/fca IdentifierOfBasketConstituentsRule).
        // The inline-ternary fallback/re-compile runs channel-EMPTY, so it keeps the #312
        // conditional-arm exclusion (isInsideDrainableMapLambda's load-bearing decline — the
        // DTCC_UnderlyingAssetReport ternary class) and today's bare-splice bytes.
        boolean blockArmSeat = isInsideRegisteredBlockArmConditional(pair.value(), ctx);
        if (!isInsideDrainableMapLambda(pair.value()) && !blockArmSeat) {
            return null;
        }
        String wrapperSimpleName = wrapper.getSimpleName();
        String baseName = JavaNamingUtil.toFirstLower(wrapperSimpleName);
        if (!javax.lang.model.SourceVersion.isIdentifier(baseName)) {
            return null;
        }
        GeneratedIdentifier id = ctx.scope().createUniqueIdentifier(baseName);
        // facet ctorSetterHoistTextOrder (rung 3): the MARKED registration - a coercion-phase
        // ctor-setter hoist numbers with its replay group in TEXT order (see
        // JavaStatementScope.registerDeferredCoercionNameTextOrdered).
        String nameToken = ctx.scope().registerDeferredCoercionNameTextOrdered(id);
        Set<JavaClass<?>> refs = new HashSet<>(compiledExpr.getRefs());
        if (wrapper instanceof JavaClass<?> wrapperClass) {
            refs.add(wrapperClass);
        }
        // The hoisted decl value is the .get()-collapsed navigation (the caller's fall-through
        // appends .get() for a single value; the hoist reproduces that inside the local).
        JavaExpression declValue = JavaExpression.from(src + ".get()", wrapper, refs,
                compiledExpr.getStaticWildcardImports());
        // facet ctorNavMetaDerefBlockArm (PR #360): the marker class lets the elseless-block
        // in-branch drain admit this decl by instanceof (the #301 numeric-only filter would
        // otherwise DECLINE the block form); the direct-body route's compileLambda drain is
        // class-agnostic, so the marker is behaviour-neutral there.
        // facet effElseCtorSetterMetaDeref (PR #398): the decl renders through the SAME
        // deferred sentinel as its consuming guard (the DeepThenArgHoist pre-rendered-text
        // precedent) — rendering must NOT getActualName-close the scope chain, because the
        // effective-else seat's enclosing cascade (#397 ruleCascadeElselessBlock) still
        // mints thenArg identifiers on those scopes after this block is accepted (the
        // cp1 closed-scope catch). Resolution moves to the standard finalization hook;
        // the group law yields the identical actual names.
        ctx.scope().registerPendingLambdaHoist(
                new CtorNavMetaDerefHoist(wrapper, id, declValue, nameToken));
        return JavaExpression.from(
                "(" + nameToken + " == null ? null : " + nameToken + ".getValue())",
                null, refs, compiledExpr.getStaticWildcardImports());
    }

    /**
     * facet ctorSetterMetaDerefFunctionHost (v3.1 flip seat 33, law A.1, rung 2): the
     * {@code default}-VALUED sibling of {@link #hoistMetaDerefCtorNavInLambdaOrNull} - same
     * LAMBDA channel, same {@link CtorNavMetaDerefHoist} marker, same deferred-coercion naming,
     * same guarded deref - with exactly TWO differences, both forced by what a {@code default}
     * renders to:
     * <ol>
     *   <li>the decl value is {@code raw} <b>verbatim</b>, never {@code raw + ".get()"}: the
     *       collapsed {@code default} IS the item ({@code Mapper.getOrDefault(T)} returns the
     *       VALUE), which is the same law arm S2 states at {@code :749-762} for its own
     *       verbatim return;</li>
     *   <li>the wrapper comes from the {@code default}'s own JOIN rather than from the value's
     *       compiled item type, because the {@code default} fall-through hands back a
     *       null-typed {@link JavaExpression}. Render truth is still tried FIRST (the collapsed
     *       expression's own compiled stamp); only when that is absent are the two OPERANDS
     *       recovered through the shared walker
     *       {@link NavigationHandler#recoverExprMetaWrapper} and required to denote the SAME
     *       wrapper ({@link NavigationHandler#sameWrapperDenotation}, the seat-14 canonical
     *       comparator - never a simple-name compare).</li>
     * </ol>
     *
     * <p>Golden drr 7.0-7.3 {@code GetBasketConstituents} pool arm:
     * {@code final FieldWithMetaString fieldWithMetaString = MapperS.of(filterAssetIdentifier
     * .evaluate(...)).<FieldWithMetaString>map("getIdentifier", ...).getOrDefault(item
     * .<FieldWithMetaString>map("getIdentifier", ...).get());} +
     * {@code .setIdentifier((fieldWithMetaString == null ? null :
     * fieldWithMetaString.getValue()))}, against the fork's whole-chain bare splice. The source
     * is {@code base-trade-basket-func.rosetta:75},
     * {@code underlier.FilterAssetIdentifier(item, ISIN) -> identifier default item ->
     * identifier} - a HOMOGENEOUS-meta {@code default} whose operands both stamp
     * {@code FieldWithMetaString} ({@code [P33-DEFJOIN]}: {@code leftWrap=…FieldWithMetaString
     * rightWrap=…FieldWithMetaString rightChan=stamp sameWrap=true vtEq=true}), which is
     * exactly why {@code SetOperationHandler:430-433} names this class as the one that KEEPS
     * the wrapper: the deref belongs at the SETTER, not inside the join.
     *
     * <p><b>Its own negative control sits in the same carrier</b> - the sibling
     * {@code source: … -> identifierType default item -> identifierType} pair is an
     * {@code AssetIdTypeEnum} with no metadata, recovers no wrapper on either operand
     * ({@code [P33-DEFJOIN]}: {@code sameWrap=false vtEq=false rightChan=walker}), declines
     * here and stays byte-identical, which is what golden does with it.
     *
     * <p>Returns {@code null} - declining to today's bare splice, a non-compiling byte that is
     * already a banded mismatch, so the decline direction can never regress a green file - when
     * the value is not an {@link RDefaultExpr}, the attribute is itself meta-annotated (its
     * setter takes the wrapper), a statement-hoist sink IS reachable (a top-level rule/function
     * ctor value routes through {@link #hoistMetaDerefCtorValueOrNull} instead), the enclosing
     * lambda is not a drainable map/extract lambda (the #312 exclusion, load-bearing), the two
     * operands do not denote one wrapper, the wrapper's value type is not the setter's own bare
     * Java type, or the derived base name is not a Java identifier.
     */
    private JavaExpression hoistMetaDerefCtorDefaultInLambdaOrNull(RAttribute attr,
            RKeyValuePair pair, JavaStatementBuilder inner, String raw, ExpressionContext ctx,
            GeneratorModel gm,
            com.regnosys.rosetta.generator.java.types.JavaTypeTranslator translator,
            JavaTypeUtil typeUtil, ExpressionCompiler compiler) {
        if (ctx == null || ctx.scope() == null || typeUtil == null || translator == null
                || compiler == null || !(pair.value() instanceof RDefaultExpr defaultValue)) {
            return null;
        }
        // The setter must take the BARE item - a meta-annotated attribute's setter takes the
        // wrapper itself, so there is nothing to deref.
        if (MetaFieldGenerator.detectMetaKind(attr) != MetaFieldGenerator.MetaKind.NONE) {
            return null;
        }
        // LAMBDA_CHANNEL only: a reachable statement-hoist sink means a top-level rule/function
        // ctor value, which is #236's seat, not this one.
        if (ctx.scope().findStatementHoistSink() != null) {
            return null;
        }
        if (!isInsideDrainableMapLambda(pair.value())) {
            return null;
        }
        // RENDER TRUTH FIRST: the collapsed default's own compiled item type when it carries
        // one. The AST join is the fall-back, not the preference.
        RJavaWithMetaValue wrapper = null;
        JavaType exprType = inner.getExpressionType();
        if (exprType != null
                && typeUtil.getItemType(exprType) instanceof RJavaWithMetaValue stamped) {
            wrapper = stamped;
        }
        if (wrapper == null) {
            RJavaWithMetaValue leftWrap = NavigationHandler.recoverExprMetaWrapper(
                    defaultValue.rawLeft(), compiler);
            RJavaWithMetaValue rightWrap = NavigationHandler.recoverExprMetaWrapper(
                    defaultValue.rawRight(), compiler);
            if (NavigationHandler.sameWrapperDenotation(leftWrap, rightWrap)) {
                wrapper = leftWrap;
            }
        }
        if (wrapper == null) {
            return null;
        }
        // The value-type-equality gate the recovery walker's javadoc mandates at the CALLER:
        // the recovered wrapper must unwrap to exactly the setter's own bare Java type.
        JavaClass<?> bareType = ctorAttrJavaType(attr, gm, translator);
        if (bareType == null || !bareType.equals(wrapper.getValueType())) {
            return null;
        }
        String baseName = JavaNamingUtil.toFirstLower(wrapper.getSimpleName());
        if (!javax.lang.model.SourceVersion.isIdentifier(baseName)) {
            return null;
        }
        GeneratedIdentifier id = ctx.scope().createUniqueIdentifier(baseName);
        // facet ctorSetterHoistTextOrder (rung 3): the MARKED registration - a coercion-phase
        // ctor-setter hoist numbers with its replay group in TEXT order (see
        // JavaStatementScope.registerDeferredCoercionNameTextOrdered).
        String nameToken = ctx.scope().registerDeferredCoercionNameTextOrdered(id);
        Set<JavaClass<?>> refs = new HashSet<>(inner.getRefs());
        if (wrapper instanceof JavaClass<?> wrapperClass) {
            refs.add(wrapperClass);
        }
        // `raw` VERBATIM - no `.get()` collapse (see the javadoc's difference 1).
        JavaExpression declValue = JavaExpression.from(raw, wrapper, refs,
                inner.getStaticWildcardImports());
        ctx.scope().registerPendingLambdaHoist(
                new CtorNavMetaDerefHoist(wrapper, id, declValue, nameToken));
        return JavaExpression.from(
                "(" + nameToken + " == null ? null : " + nameToken + ".getValue())",
                null, refs, inner.getStaticWildcardImports());
    }

    /**
     * facet ctorNavMetaDerefBlockArm (PR #360): the marker for a #312 ctor-setter NAV
     * meta-deref hoist registered at the CONDITIONAL-ARM seat (or the direct body — the
     * marker is admit-neutral there). {@code CollectionHandler.compileElselessConditionalBlock}'s
     * in-branch drain admits it by {@code instanceof}, so the decl lands INSIDE the owning
     * if-branch (golden drr esma/fca IdentifierOfBasketConstituentsRule: the
     * {@code final FieldWithMetaString fieldWithMetaString = …get();} before the wrapped ctor
     * return). Behaviour is identical to the plain declaration statement (the
     * {@code ReferenceHandler.MetaWrapValueHoist} pattern).
     */
    public static final class CtorNavMetaDerefHoist extends JavaLocalVariableDeclarationStatement {
        private final String sentinelName;
        private final JavaType declType;
        private final JavaExpression init;

        public CtorNavMetaDerefHoist(JavaType type,
                GeneratedIdentifier id, JavaExpression initializer) {
            this(type, id, initializer, null);
        }

        // facet effElseCtorSetterMetaDeref (PR #398): with a sentinel, render emits the
        // deferred token in the decl position (resolved with the consuming guard's token
        // at finalization — one registration, one group entry) instead of materialising
        // the identifier, so rendering never closes the scope chain mid-compile (the
        // cascade seats mint thenArg identifiers after this decl renders).
        public CtorNavMetaDerefHoist(JavaType type,
                GeneratedIdentifier id, JavaExpression initializer, String sentinelName) {
            super(true, type, id, initializer);
            this.sentinelName = sentinelName;
            this.declType = type;
            this.init = initializer;
        }

        @Override
        public void render(StringBuilder sb) {
            if (sentinelName == null) {
                super.render(sb);
                return;
            }
            sb.append("final ").append(declType.getSimpleName()).append(' ')
                    .append(sentinelName).append(" = ");
            init.render(sb);
            sb.append(";\n");
        }
    }

    /**
     * facet ctorNavMetaDerefBlockArm (PR #360): true iff the FIRST {@link RConditionalExpr}
     * ancestor of {@code node} (walking up, stopping at the enclosing rule/function root) is
     * registered on the #354 {@code blockArmSeatConditionals} channel — i.e. an elseless-block
     * render is compiling THAT conditional's arm right now. Identity compare per the channel's
     * law; no registration → false (the inline-ternary owner keeps the #312 exclusion).
     */
    private static boolean isInsideRegisteredBlockArmConditional(RNode node, ExpressionContext ctx) {
        if (ctx == null || ctx.scope() == null) {
            return false;
        }
        // Copilot #360 R1: stop at the enclosing rule/function root (matching the javadoc)
        // + the standard parent-walk bound — the FIRST RConditionalExpr short-circuits for
        // every real carrier, so both guards are behaviour-identical belts.
        RNode cur = node == null ? null : node.parent();
        int depth = 0;
        while (cur != null && depth++ < HandlerHelper.PARENT_WALK_LIMIT) {
            if (cur instanceof RConditionalExpr cond) {
                return ctx.scope().isBlockArmSeatConditional(cond);
            }
            if (cur instanceof com.regnosys.rosetta.ast.functions.RRule
                    || cur instanceof RFunction) {
                return false;
            }
            cur = cur.parent();
        }
        return false;
    }

    /**
     * facet ctorSetterMetaDerefLambda (PR #312): true iff {@code node} sits DIRECTLY in a DRAINABLE
     * map/extract lambda body — the NEAREST enclosing {@link RInlineFunction} is a map/extract lambda
     * (its parent is an {@link RExtractExpr}, the {@code mapItem}/{@code mapSingleToItem} seat that
     * {@code CollectionHandler.compileLambda} drains pending lambda hoists for) AND no
     * {@link RConditionalExpr} sits between {@code node} and that lambda. Walks UP the parent chain
     * (the value's own argument lambdas are children, never on this path), stopping at the enclosing
     * rule/function root. A closer non-map inline function (filter predicate / sort comparator /
     * reduce) returns false so the hoist is never orphaned.
     *
     * <p>The conditional-arm exclusion is load-bearing (the regscan caught the over-fire): when the
     * ctor sits inside a conditional arm of the lambda body (a block-ladder golden RESTRUCTURES —
     * DTCC_UnderlyingAssetReport's `item -> exists(…) ? Report.builder()… : …`), a pending lambda
     * hoist makes {@code compileLambda}'s conditional-block form DECLINE to the inline ternary (its
     * {@code !hasPendingLambdaHoists()} gate), mangling the output and moving the (co-occupied,
     * still-divergent) file AWAY from golden. Only the DIRECT simple-expression body
     * (BasketConstituents' {@code item -> MapperS.of(ctor)}) is a clean single-statement
     * block-convert, so the hoist fires there alone.
     */
    private static boolean isInsideDrainableMapLambda(RNode node) {
        // Promoted to HandlerHelper at PR #314 (shared with ConversionHandler's
        // getCollapsedMetaDeref block-conversion arm); this delegates to keep one source of truth.
        return HandlerHelper.isInsideDrainableMapLambda(node);
    }

    /**
     * The proven value-level meta {@link MetaFieldGenerator.MetaKind} of a ctor value that
     * produces an attribute-meta wrapper — the kind-returning variant of
     * {@link #valueCarriesAttributeMeta}: a direct function call ({@code RSymbolReference}
     * → {@code RFunction}) whose output carries a value-level {@code [metadata …]}
     * annotation, or an {@code extract} over such a call. Returns {@link
     * MetaFieldGenerator.MetaKind#NONE} for every other shape (navigation chains,
     * literals, aliases, unresolved symbols, type-level key/template meta).
     */
    static MetaFieldGenerator.MetaKind valueProvenMetaKind(RExpression value) {
        if (value instanceof RSymbolReference ref) {
            var resolved = ref.symbol().orElse(null);
            if (resolved instanceof RFunction fn) {
                return fn.output()
                        .map(MetaFieldGenerator::detectMetaKind)
                        .orElse(MetaFieldGenerator.MetaKind.NONE);
            }
            return MetaFieldGenerator.MetaKind.NONE;
        }
        if (value instanceof RExtractExpr extract && extract.body() != null) {
            return valueProvenMetaKind(extract.body().body());
        }
        // facet metaIteArmWrapperTyping (PR #328, F4): a CONDITIONAL value proves meta
        // when its THEN arm proves a kind and every PRESENT else arm (the else-if
        // recursion) proves the SAME kind — the absent/`empty` else is neutral (it
        // compiles to the #193 meta-EMPTY builder under the wrapper-typed ite-hoist,
        // so the joined value stays the wrapper). This keeps the consuming setter's
        // plain-vs-Value choice in LOCK-STEP with the ControlFlowHandler wrapper-typed
        // local (the #327-B invariant: the local's type IS the value's proven kind).
        if (value instanceof RConditionalExpr cond) {
            MetaFieldGenerator.MetaKind thenKind = valueProvenMetaKind(cond.thenBranch());
            if (thenKind == MetaFieldGenerator.MetaKind.NONE) {
                return MetaFieldGenerator.MetaKind.NONE;
            }
            RExpression els = cond.elseBranch().orElse(null);
            if (isNoValueElse(els)) {
                return thenKind;
            }
            return valueProvenMetaKind(els) == thenKind ? thenKind
                    : MetaFieldGenerator.MetaKind.NONE;
        }
        return MetaFieldGenerator.MetaKind.NONE;
    }

    /**
     * facet metaIteArmWrapperTyping (PR #328, F4): a conditional's "no value" else —
     * absent, an explicit {@code else empty} ({@link REmptyLiteral}), or the EMPTY
     * {@link RListLiteral} the parser's DefaultElseRule SYNTHESIZES for an elseless
     * conditional (the #199/#289 synthetic-else shape — the reason a plain
     * {@code elseBranch().isEmpty()} check under-fires).
     */
    private static boolean isNoValueElse(RExpression els) {
        return els == null || els instanceof REmptyLiteral
                || (els instanceof RListLiteral l && l.elements().isEmpty());
    }

    /**
     * facet ctorSetterElementwiseWrapperDeref (PR #373, F-eps): the nearest enclosing
     * {@link RInlineFunction} carrying a {@code thenArg} scope binding (walking THROUGH
     * unbound inlines, mirroring {@code ReferenceHandler.enclosingThenArgType}), returned
     * as the bound reference itself so the caller can compare the RENDER root (the
     * binding's deferred token) and read the decl's wrapper KIND. {@code null} when no
     * bound inline encloses the value.
     */
    private static JavaExpression enclosingBoundThenArg(RNode expr, ExpressionContext ctx) {
        JavaStatementScope sc = ctx.scope();
        if (sc == null || expr == null) {
            return null;
        }
        RNode cur = expr.parent();
        int depth = 0;
        while (cur != null && depth++ < HandlerHelper.PARENT_WALK_LIMIT) {
            if (cur instanceof RInlineFunction inline) {
                JavaExpression ref = sc.thenArgRefFor(inline);
                if (ref != null) {
                    return ref;
                }
            }
            cur = cur.parent();
        }
        return null;
    }


    /**
     * Setter name for a constructor pair — {@code set<Name>} or, for a
     * {@code [metadata …]}-annotated attribute with a meta-FREE value,
     * upstream's {@code SET_VALUE} form {@code set<Name>Value}.
     *
     * <p>facet {@code ingest_setter_value_form} (arm C1): upstream's
     * {@code requiresValueAssignment} (ExpressionGenerator.xtend:1233-1240) picks
     * the {@code Value} form only when the target attribute has meta AND the
     * value expression does NOT — a value that provably carries attribute-meta
     * (a direct fn call whose callee OUTPUT is attribute-meta-annotated, or an
     * extract over one) keeps the PLAIN setter: its items already ARE the
     * wrapper type (golden {@code .setContractualParty(new ArrayList(
     * mapContractualParty.evaluate(...)))} — cdm6 MapLegalAgreement /
     * MapAncillaryParty). Unproven value shapes decline to the {@code Value}
     * form (today's render) — under-firing keeps the file waivered, over-firing
     * is the only regression direction. Green pins: cdm6
     * MapAdjustedDateToAdjustableDate / MapDateToAdjustableDate legitimately
     * carry {@code .set*Value(} for meta-free values — their gates stay on the
     * {@code Value} path.
     */
    private String ctorSetterName(RAttribute attr, RKeyValuePair pair) {
        return ctorSetterName(attr, pair, null, null, null);
    }

    /**
     * facet setterNameValueMeta (PR #327, facet B) — the proof-aware overload: the PLAIN
     * setter is also selected when (B1) the value is a META-alias call, (B2) the
     * COMPILED value's item type is the wrapper (render-truth — an un-deref'd
     * meta-terminal navigation chain), or (B3) a list literal all of whose elements
     * prove meta ({@link #valueCarriesAttributeMeta(RExpression, ExpressionCompiler)}).
     * Upstream {@code requiresValueAssignment} picks the {@code Value} form ONLY when
     * the target has meta AND the value expression does NOT — these arms complete the
     * value-side proof the fn-call/extract check alone missed.
     */
    private String ctorSetterName(RAttribute attr, RKeyValuePair pair,
            JavaStatementBuilder compiledValue, ExpressionCompiler compiler,
            CondListCoerce condList) {
        String base = "set" + JavaNamingUtil.toFirstUpper(attr.name());
        for (var annotationRef : attr.annotationRefs()) {
            if ("metadata".equals(annotationRef.annotationName())) {
                // facet condListCoerce (PR #327, Seat-1 SF-2 lock-step): a FIRED
                // handshake whose list element is the META wrapper produced the wrapper
                // list itself (the arms carry attribute-meta values), so the PLAIN
                // setter is the only type-correct choice — the sentinel local's null
                // expression type would otherwise fall through to the Value form and
                // disagree with the hoisted list's element type. facet
                // ctorCondValueSetterList (PR #383): a meta attribute with meta-FREE
                // arms now takes a BARE-element handshake at this seat (see
                // condListCoerceFor), so it deliberately falls THROUGH this arm to the
                // Value form — the setter and the hoisted list's element type stay in
                // lock-step in both directions (golden .setPriceScheduleValue over
                // List<PriceSchedule> — cdm6 MapSwapPayout).
                if (condList != null && condList.fired
                        && condList.listElemType instanceof RJavaWithMetaValue) {
                    return base;
                }
                // v3.1 flip seat 30, law 8 - facet ctorSetterValueWrapperRecovery: the
                // render-truth proof (B2) reads compiled.getExpressionType() and returns
                // FALSE on an ERASED value (a list-op collapse, a bare `item.get()` off a
                // Mapper*<FieldWithMetaX> lambda, a deep-feature terminal), and the AST
                // proof above has no arm for that shape either. Those are exactly the ctor
                // values golden assigns through the PLAIN setter, so the fork fell to the
                // `Value` form and spliced a FieldWithMetaString into setIdentifierValue,
                // which takes the bare String (golden drr UnderlierProductIdentifier
                // `.setIdentifier(floatingRateIndex(...).<FieldWithMetaString>map("getName",
                // ...).get())`). Recover from the AST with the SAME transitive walk the arms
                // above already reach (NavigationHandler.recoverExprMetaWrapper, the #331
                // expression-level entry), so the three value-side proofs cannot disagree
                // about the same expression. LAW 69.
                //
                // MEASURED reach (LAW 72/75): of 1,147 site=attrMetaValue rows corpus-wide
                // exactly 4 sit in this cell (meta attribute + Value form chosen + erased
                // value type + a recovered wrapper) - one per drr 7.x cell, all
                // UnderlierProductIdentifier. The javadoc's named GREEN pins (cdm6
                // MapAdjustedDateToAdjustableDate / MapDateToAdjustableDate) have meta-FREE
                // values, so no recovery resolves for them and they keep the Value form.
                if (valueCarriesAttributeMeta(pair.value(), compiler)
                        || compiledValueIsMetaWrapped(compiledValue, compiler)
                        || valueRecoversAttributeMeta(pair.value(), compiler)) {
                    return base;
                }
                return base + "Value";
            }
        }
        return base;
    }

    /**
     * True iff a ctor value expression PROVABLY produces attribute-meta-wrapped
     * items: a direct function call ({@code RSymbolReference} resolving to
     * {@code RFunction}) whose output attribute carries an attribute-meta
     * {@code [metadata ...]} annotation (reference/address/scheme/id/location —
     * {@link MetaFieldGenerator#detectMetaKind}; type-level key/template stay
     * NONE), or an {@code extract} whose lambda body is such a call (the
     * bare-function-operand form {@code list extract MapFn}). Everything else —
     * navigation chains, literals, aliases, unresolved symbols — returns false.
     */
    private static boolean valueCarriesAttributeMeta(RExpression value) {
        return valueCarriesAttributeMeta(value, null);
    }

    /**
     * facet setterNameValueMeta (PR #327, facet B) — the compiler-aware overload: beside
     * the fn-call/extract proof above, (B1) an ALIAS-call value proves meta through the
     * #178 {@code NavigationHandler.tryAliasReceiverMapperType} gate (non-null ONLY for a
     * META alias — the SAME walk that renders the alias method's wrapper signature, so
     * the proof and the rendered value type cannot disagree), and (B3) a LIST-LITERAL
     * value proves meta when EVERY element does (upstream's element-type join KEEPS
     * meta, so the joined List type carries the wrapper). The compiler-less overload
     * (a {@code null} compiler) keeps the pre-#327 proof exactly.
     */
    private static boolean valueCarriesAttributeMeta(RExpression value, ExpressionCompiler compiler) {
        if (value instanceof RSymbolReference ref) {
            var resolved = ref.symbol().orElse(null);
            if (resolved instanceof RFunction fn) {
                return fn.output()
                        .map(out -> MetaFieldGenerator.detectMetaKind(out)
                                != MetaFieldGenerator.MetaKind.NONE)
                        .orElse(false);
            }
            if (resolved instanceof RShortcut && compiler != null) {
                return NavigationHandler.tryAliasReceiverMapperType(ref, compiler) != null;
            }
            // facet listOfListsWrapperKeep (PR #384): a BARE-symbol value whose
            // reference never resolved (symbol EMPTY — the #384
            // bareSymbolBodyCardinality class) but whose NAME is an attribute-meta
            // feature of the implicit item proves meta via the SAME walk the
            // bare-symbol mapC synthesis renders from (implicitItemDataTypeOrInferred
            // + findAttributeOnDataType — render-truth): `price: price filter p […]`
            // pipes FieldWithMetaPriceSchedule items, so upstream's
            // requiresValueAssignment keeps the PLAIN setter (golden `.setPrice(
            // item.<FieldWithMetaPriceSchedule>mapC("getPrice", …)…getMulti())` —
            // cdm Qualify_OnDemandRateChange; the #348 corpus law: ZERO goldens
            // carry set<X>Value over wrapper elements). A meta-free / unresolvable
            // name walks to null → the Value form (today's render) — under-firing
            // is the only miss direction.
            if (resolved == null && compiler != null && ref.name() != null) {
                RDataType itemT =
                        NavigationHandler.implicitItemDataTypeOrInferred(ref, compiler);
                RAttribute bareAttr = itemT == null ? null
                        : HandlerHelper.findAttributeOnDataType(itemT, ref.name());
                return bareAttr != null && MetaFieldGenerator.detectMetaKind(bareAttr)
                        != MetaFieldGenerator.MetaKind.NONE;
            }
            return false;
        }
        if (value instanceof RExtractExpr extract && extract.body() != null) {
            return valueCarriesAttributeMeta(extract.body().body(), compiler);
        }
        // facet listOfListsWrapperKeep (PR #384): an element-preserving FILTER pipes
        // its receiver's meta-ness through (the #329 M5 element-preservation law at
        // the AST level) — `price filter p [ p -> priceType <> … ]` keeps the
        // wrapper element the bare `price` arm above proves, so the setter reads
        // through the filter to the piped element.
        if (value instanceof RFilterExpr filterVal && filterVal.left().isPresent()) {
            return valueCarriesAttributeMeta(filterVal.left().get(), compiler);
        }
        // facet ctorImplicitItemMetaValue (PR #348): a bare implicit-item ctor value
        // (`quantity: item` inside `q extract PriceQuantity {…}` — cdm6
        // MapPriceQuantity) proves attribute-meta through the #285
        // implicitItemArgMeta walk: the extract pipe's ELEMENT is the wrapper
        // (MapperC<FieldWithMetaNonNegativeQuantitySchedule> q), so upstream's
        // requiresValueAssignment keeps the PLAIN setter (golden
        // `.setQuantity(item.getMulti())` — the corpus law is absolute: ZERO goldens
        // carry set<X>Value(<mapper>.getMulti()) over wrapper elements). A meta-free
        // item walks to null → the Value form (today's render) — under-firing is the
        // only miss direction.
        if (compiler != null && value instanceof RImplicitVariable) {
            if (NavigationHandler.implicitItemArgMeta(value, compiler) != null) {
                return true;
            }
            // The ALIAS-rooted pipe (`q extract PriceQuantity {…}` — q a meta alias):
            // the shared recovery walk has no alias arm, and widening it would widen
            // the #285 lambda-channel deref gates too — so the alias consultation is
            // SCOPED here: resolve the item's owning chain arg and ask the SAME walk
            // that renders the alias signature (tryAliasReceiverMapperType — non-null
            // ONLY for a meta alias, the #327-B1 gate; render-truth lockstep).
            RExpression owning = NavigationHandler.implicitItemArgument(value);
            RExpression chainArg = owning instanceof RImplicitVariable
                    ? NavigationHandler.thenOwnerArgument(owning) : owning;
            if (chainArg instanceof RSymbolReference chainRef
                    && NavigationHandler.tryAliasReceiverMapperType(chainRef, compiler) != null) {
                return true;
            }
        }
        if (compiler != null && value instanceof RListLiteral list && !list.elements().isEmpty()) {
            return list.elements().stream()
                    .allMatch(e -> valueCarriesAttributeMeta(e, compiler));
        }
        // facet ctorWithMetaReference (PR #348): a with-meta ctor value IS the wrapper
        // by construction — upstream requiresValueAssignment reads the expression's own
        // meta and keeps the PLAIN setter (golden `.setNotionaReference(
        // ReferenceWithMetaMoney.builder()…)`, never set<X>Value around a with-meta).
        if (value instanceof RWithMetaExpr) {
            return true;
        }
        // facet metaIteArmWrapperTyping (PR #328, F4): a CONDITIONAL value proves meta
        // when its THEN arm does and every PRESENT else arm (else-if recursion) does too
        // — the absent/`empty` else is neutral (under the F4 wrapper-typed ite-hoist it
        // compiles to the #193 meta-EMPTY builder, so the joined value stays the
        // wrapper). Lock-step with the ControlFlowHandler wrapper-typed local + the
        // valueProvenMetaKind conditional arm (the #327-B invariant): golden
        // MapNumberOfOptionsAndOptionEntitlementToQuantity keys `.setCurrency(
        // ifThenElseResult0)` — the PLAIN setter — for the FieldWithMetaString local.
        if (value instanceof RConditionalExpr cond) {
            if (!valueCarriesAttributeMeta(cond.thenBranch(), compiler)) {
                return false;
            }
            RExpression els = cond.elseBranch().orElse(null);
            return isNoValueElse(els) || valueCarriesAttributeMeta(els, compiler);
        }
        // facet mapperCIteLift setId (PR #366): a THEN-CHAIN ctor value whose every
        // then-body is element-PRESERVING (distinct/only-element list-ops over the
        // implicit item) proves meta through its BASE leaf — the element type rides
        // the chain unchanged, so a meta-annotated base attribute keeps the wrapper
        // at the setter (golden drr TechnicalRecordId `id: technicalRecordId then
        // distinct only-element` → `.setId(MapperS.of(distinct(thenArg2).get())
        // .get())`, the PLAIN setter over the FieldWithMetaString element). A
        // non-preserving body (extract/map/fn) declines — the element changes.
        if (value instanceof RThenExpr thenVal) {
            // facet ctorThenExtractFnMetaSetter (PR #392): a then-chain whose LAST body
            // is a fn-call extract (with-args or bare) PRODUCES the callee's output as
            // the element — prove meta from the callee via the existing extract/fn-call
            // arms (the direct-fn-call proof one chain seat deeper; golden cdm6
            // MapGenericProductPriceQuantityList `.setQuantity(thenArg0\n\t.mapItem(…)
            // .getMulti())` + `.setObservable(thenArg1\n\t.mapSingleToItem(…).get())`
            // — the WithLocation callees' outputs are location-annotated). The #348
            // corpus law keeps this green-safe in both directions: golden never
            // carries set<X>Value over wrapper elements, and a green Value-setter
            // file's value is meta-free so this proof cannot fire there.
            RExpression thenLastBody = thenVal.body().map(f -> f.body()).orElse(null);
            if (thenLastBody instanceof RExtractExpr
                    || (thenLastBody instanceof RSymbolReference lbRef
                            && lbRef.symbol().filter(s -> s instanceof RFunction).isPresent())) {
                return valueCarriesAttributeMeta(thenLastBody, compiler);
            }
            RExpression curT = thenVal;
            while (curT instanceof RThenExpr t2) {
                RExpression body = t2.body().map(f -> f.body()).orElse(null);
                if (!isElementPreservingItemListOps(body)) {
                    return false;
                }
                curT = t2.argument();
            }
            if (curT instanceof RSymbolReference baseRef
                    && baseRef.symbol().orElse(null) instanceof RAttribute baseAttr) {
                return MetaFieldGenerator.detectMetaKind(baseAttr)
                        != MetaFieldGenerator.MetaKind.NONE;
            }
            return false;
        }
        return false;
    }

    /**
     * facet mapperCIteLift setId (PR #366): true iff {@code body} is a chain of
     * element-PRESERVING list-ops ({@code distinct} / {@code only-element}) bottoming
     * out at the implicit item — the then-body shapes whose element type equals the
     * then-ARGUMENT's element type.
     */
    private static boolean isElementPreservingItemListOps(RExpression body) {
        RExpression cur = body;
        while (cur instanceof RListOpExpr op
                && (op.op() == ListOp.DISTINCT || op.op() == ListOp.ONLY_ELEMENT)) {
            cur = op.argument();
        }
        return cur instanceof RImplicitVariable;
    }

    /**
     * facet setterNameValueMeta (PR #327, facet B2) — render-truth meta proof: the
     * COMPILED value's item type is an {@link RJavaWithMetaValue} wrapper (a navigation
     * chain whose terminal step navigates TO the {@code FieldWithMetaX} /
     * {@code ReferenceWithMetaX} itself, un-deref'd). Keying on the compiled type — not
     * the AST leaf — means a chain the compiler derefs (a Type-coercion step to the bare
     * value) correctly stays on the {@code Value} form: the proof cannot disagree with
     * the rendered bytes.
     */
    private static boolean compiledValueIsMetaWrapped(JavaStatementBuilder compiled,
            ExpressionCompiler compiler) {
        JavaTypeUtil typeUtil = compiler == null ? null : compiler.getTypeUtil();
        if (typeUtil == null || compiled == null || compiled.getExpressionType() == null) {
            return false;
        }
        return typeUtil.getItemType(compiled.getExpressionType()) instanceof RJavaWithMetaValue;
    }

    /**
     * facet ctorSetterValueWrapperRecovery (v3.1 flip seat 30, law 8) — the AST-recovered
     * twin of {@link #compiledValueIsMetaWrapped} for an ERASED ctor value. The render-truth
     * proof returns {@code false} the moment the compiled expression type is {@code null},
     * which is the shape of every list-op collapse, bare {@code item.get()} and deep-feature
     * terminal; {@link #valueCarriesAttributeMeta} has no arm for those shapes either. This
     * consults {@link NavigationHandler#recoverExprMetaWrapper} — the SAME transitive walker
     * the sibling arms already reach through {@code recoverInnerRuleMetaWrapper} — so the
     * three value-side proofs share one answer for one expression (LAW 69).
     *
     * <p>Strictly a THIRD disjunct: it can only ever move a decision from the
     * {@code set<Name>Value} form to the PLAIN {@code set<Name>}, never the other way, so a
     * green file whose value is meta-free (the cdm6 {@code MapAdjustedDateToAdjustableDate} /
     * {@code MapDateToAdjustableDate} pins named on {@link #ctorSetterName}) recovers
     * {@code null} and is byte-unchanged.
     *
     * <p>MEASURED reach (the seat-30 LAW-75 probe): 4 of 1,147 {@code site=attrMetaValue}
     * rows corpus-wide, all four {@code fn:UnderlierProductIdentifier} {@code attr=identifier}
     * ({@code cfRecover=FieldWithMetaString}).
     */
    private static boolean valueRecoversAttributeMeta(RExpression value,
            ExpressionCompiler compiler) {
        return value != null && compiler != null
                && NavigationHandler.recoverExprMetaWrapper(value, compiler) != null;
    }

    /** The attribute's translated Java item type, or {@code null} if unresolved. */
    private JavaClass<?> ctorAttrJavaType(RAttribute attr, GeneratorModel gm,
            com.regnosys.rosetta.generator.java.types.JavaTypeTranslator translator) {
        RType itemType = gm.getType(attr);
        if (itemType == null || itemType instanceof com.regnosys.rosetta.types.RMissingType) {
            return null;
        }
        return translator.toJavaReferenceType(itemType);
    }

    /**
     * True iff a multi ctor value PROVABLY has a non-pojo item type — the
     * arm-C2r bare-splice gate. A bare input/variable ({@code RSymbolReference}
     * → {@code RAttribute}) takes its declared type, a direct fn call
     * ({@code RSymbolReference} → {@code RFunction}) the callee output's; the
     * item is non-pojo iff its {@link RType} is neither a data- nor a
     * choice-type ref (enum/basic/number/string/record — never wildcard in the
     * golden List params, so upstream never wraps them). Returns {@code false}
     * (→ decline, keep today's ArrayList wrap) for every other value shape,
     * for attribute-meta-annotated sources (their runtime item is the
     * {@code FieldWithMeta*}/{@code ReferenceWithMeta*} wrapper — a pojo), for
     * alias-typed items (unverified) and for every resolution failure.
     */
    private boolean ctorValueItemIsBareSpliced(RExpression value, GeneratorModel gm,
            ExpressionCompiler compiler) {
        RAttribute source = null;
        if (value instanceof RSymbolReference ref) {
            var resolved = ref.symbol().orElse(null);
            if (resolved instanceof RAttribute attr) {
                source = attr;
            } else if (resolved instanceof RFunction fn) {
                source = fn.output().orElse(null);
            } else if (resolved instanceof RShortcut shortcut && shortcut.expression() != null) {
                // facet ctorListFieldNoCopy (seat 28, law 13): the ALIAS-valued ctor value
                // - the sibling of the seat-25 RULE arm below. LAW 69: the COPY exists
                // exactly where the emitted alias signature carries the WILDCARD element,
                // so the definitive verdict is law D's OWN signature channel
                // (NavigationHandler.aliasSignatureWildcardItemType -> FunctionAliasHelper
                // .aliasSignatureWildcardElementOrNull, the same single inferExpressionType
                // walk the signature came from). The first cut classified by the BODY's
                // inferred element type and REGRESSED the green drr-cell cdm file
                // MapLegalEntity (its DATA-element alias mis-read as bare; golden keeps
                // `new ArrayList<>(entityIdFromLegalEntity(...).getMulti())`) - measured
                // by the suite's whole-cell control, re-pointed here per the design's own
                // coupling note. Golden proves both verdicts in ONE ctor
                // (MapCorporateSector: `MapperC<FinancialSectorEnum>` bare vs
                // `MapperC<? extends NonFinancialSector>` wildcarded).
                if (NavigationHandler.aliasSignatureWildcardItemType(ref, compiler) != null) {
                    // wildcard-signed (model or meta element): the copy stays.
                    return false;
                }
                // A null verdict is AMBIGUOUS (a basic/enum-element alias - the bare
                // class - or a resolution failure). The inferred-element read separates
                // them, with every failure declining to the copy (conservative).
                RMetaAnnotatedType aliasType =
                        gm.workspace().getInferredType(shortcut.expression());
                if (aliasType == null || aliasType.isMissing()
                        || NavigationHandler.recoverExprMetaWrapper(shortcut.expression(),
                                compiler) != null) {
                    return false;
                }
                RType aliasItem = HandlerHelper.stripAliases(aliasType.type());
                if (aliasItem == null
                        || aliasItem instanceof com.regnosys.rosetta.types.RMissingType
                        || aliasItem instanceof com.regnosys.rosetta.types.RAliasType) {
                    return false;
                }
                return !(aliasItem instanceof RDataTypeRef)
                        && !(aliasItem instanceof RChoiceTypeRef);
            } else if (resolved instanceof com.regnosys.rosetta.ast.functions.RRule rule) {
                // facet ctorRuleValueCardinality (seat 25, law D): a RULE-valued ctor value
                // classifies by the CALLEE's inferred output — the golden witness this
                // gate's javadoc cites (`.setDaysOfTheWeek(getDaysOfTheWeek.evaluate(…))`)
                // is the rune-fpml FUNCTION twin of exactly this drr RULE shape (the
                // esma/fca DeliveryRule `daysOfTheWeek` value). A rule declares no output
                // attribute: read ruleInferredOutputRType (the ONE rule-output read) and
                // DECLINE (→ today's ArrayList wrap) on a recoverable META output — the
                // runtime item is the FieldWithMeta* wrapper, a pojo — mirroring the
                // attribute-meta decline below; every resolution failure declines too.
                RType ruleOut = HandlerHelper.ruleInferredOutputRType(rule, gm);
                if (ruleOut == null || ruleOut instanceof com.regnosys.rosetta.types.RMissingType
                        || ruleOut instanceof com.regnosys.rosetta.types.RAliasType
                        || NavigationHandler.recoverInnerRuleMetaWrapper(rule, compiler) != null) {
                    return false;
                }
                return !(ruleOut instanceof RDataTypeRef)
                        && !(ruleOut instanceof RChoiceTypeRef);
            }
        }
        if (source == null
                || MetaFieldGenerator.detectMetaKind(source) != MetaFieldGenerator.MetaKind.NONE) {
            return false;
        }
        RType itemType = gm.getType(source);
        if (itemType == null || itemType instanceof com.regnosys.rosetta.types.RMissingType
                || itemType instanceof com.regnosys.rosetta.types.RAliasType) {
            return false;
        }
        return !(itemType instanceof RDataTypeRef)
                && !(itemType instanceof RChoiceTypeRef);
    }

    // =========================================================================
    // With-meta expression
    // =========================================================================

    /**
     * Compiles a with-meta expression into a builder chain with metadata.
     *
     * <p>Pattern:
     * {@code argument.toBuilder().setMeta(MetaFields.builder().setMetaName(value).build()).build()}
     *
     * <p>Full with-meta compilation requires type information to determine
     * the correct meta wrapper class (FieldWithMeta*, ReferenceWithMeta*).
     * At M7b-1 a simplified pattern using {@code MetaFields} is generated.
     *
     * @param expr     the with-meta expression node
     * @param ctx      the current expression compilation context
     * @param compiler the parent compiler for recursive compilation
     * @return a {@link JavaExpression} rendering the with-meta builder chain
     */
    public JavaStatementBuilder handle(RWithMetaExpr expr, ExpressionContext ctx, ExpressionCompiler compiler) {
        // facet convertToMeta (PR #202): the upstream caseWithMetaOperation law
        // (ExpressionGenerator.xtend:1419) — build the concrete meta wrapper
        // (FieldWithMetaX scheme/scopedKey/externalKey; ReferenceWithMetaX
        // externalReference/Reference) instead of the M7b-1 stub's non-compiling
        // `<arg>.toBuilder().setMeta(MetaFields.builder()...).build()`. Declines to
        // the legacy stub below on every unsupported shape (type-meta key/template,
        // non-meta output, sink-less path); the stub is non-compiling so EVERY
        // with-meta carrier is already a waivered mismatch — green-safe by
        // construction (zero goldens carry the stub form).
        JavaStatementBuilder typed = tryTypedWithMeta(expr, ctx, compiler);
        if (typed != null) {
            return typed;
        }
        // facet metaLocationConstruction (PR #214): the POJO-meta complement —
        // a `with-meta { key/id/scheme: … }` on a constructor whose OUTPUT is a plain
        // POJO (no meta wrapper) populates the POJO's OWN MetaFields via upstream's
        // getOrCreateMeta() statement form. Declines (→ the stub below) on a wrapper
        // output, a non-model output, a non-mappable entry, or a sink-less path.
        JavaStatementBuilder pojoMeta = tryPojoMetaWithMeta(expr, ctx, compiler);
        if (pojoMeta != null) {
            return pojoMeta;
        }
        // facet withMetaOwnTypeDerive (PR #421): where BOTH prior arms decline
        // (a plain, un-annotated output — e.g. `value with-meta {scheme: …}` on a
        // bare `string` output), derive the wrapper from the EXPRESSION's own meta
        // shape (argument type + entry keys), upstream's getRMetaAnnotatedType law.
        // Returns TYPED so the assignment seat can unwrap. Declines (→ the stub)
        // on every unproven shape; the stub is non-compiling, so every carrier
        // this arm takes over was already a waivered mismatch (green-safe).
        JavaStatementBuilder ownTyped = tryOwnTypeWithMeta(expr, ctx, compiler);
        if (ownTyped != null) {
            return ownTyped;
        }
        JavaStatementBuilder argBuilder =
                compiler.compile(expr.argument(), ctx.expectedType(), ctx.scope());
        String argument = HandlerHelper.render(argBuilder);

        StringBuilder metaBuilder = new StringBuilder();
        metaBuilder.append("MetaFields.builder()");

        Set<JavaClass<?>> refs = new HashSet<>(argBuilder.getRefs());
        Set<JavaClass<?>> wildcards = new HashSet<>(argBuilder.getStaticWildcardImports());

        for (RWithMetaEntry entry : expr.entries()) {
            String setterName = toSetterName(entry.metaName());
            JavaStatementBuilder valueBuilder =
                    compiler.compile(entry.value(), ctx.expectedType(), ctx.scope());
            String value = HandlerHelper.render(valueBuilder);
            metaBuilder.append('.').append(setterName).append('(').append(value).append(')');
            refs.addAll(valueBuilder.getRefs());
            wildcards.addAll(valueBuilder.getStaticWildcardImports());
        }

        metaBuilder.append(".build()");

        // v3.2 seat 13 (D53, THE CLOSING SEAT - the LOUD register's with-meta site R5, the chaos M3-PARKED rows
        // C24WithMetaExists / C24WithMetaPlain): this M7b-1 stub was documented non-compiling at PR #202 ("zero goldens
        // carry the stub form") and every carrier was a SILENT byte row - on the chaos cell a with-meta construction
        // over a `nothing` value (`null.toBuilder().setMeta(...)`), 26 of 26 non-compiling by the seat-13 census. The
        // stub is refused whole: a with-meta whose wrapper none of the three typed arms above could derive has no
        // compiling render here (the released plugin's own render of the Void shape is the v3.3 heal, D53 decision 2).
        throw SilentDegradation.refuse(SilentDegradation.Site.WITH_META_UNTYPED_STUB,
                "with-meta construction over `" + argument + "` whose meta wrapper no typed arm could derive (the"
                        + " annotated-output, POJO-meta and own-type arms declined) - the pre-seat render was the M7b-1"
                        + " stub `" + argument + ".toBuilder().setMeta(" + metaBuilder + ").build()`",
                expr);
    }

    /**
     * facet {@code convertToMeta} (PR #202) — the typed meta-wrapper construction,
     * mirroring upstream {@code ExpressionGenerator.caseWithMetaOperation}
     * (in-tree 9.83.0 reference). The expected wrapper type
     * ({@code FieldWithMetaX} / {@code ReferenceWithMetaX}) comes from the
     * enclosing function output's {@code [metadata …]} annotation
     * ({@link MetaFieldGenerator#detectMetaKind} + {@link RJavaWithMetaValue#create}
     * over {@code gm.getType(output)}); the value compiles null-expected and
     * item-coerces ({@link #withMetaItemForm}).
     *
     * <p>Two arms:
     * <ul>
     *   <li><b>FieldWithMeta</b> (scheme/location/id — attribute-meta only): hoist
     *       {@code final <ValueType|Builder> withMetaArgument = <arg|arg==null?null:arg.toBuilder()>;}
     *       on the statement-hoist sink, then return
     *       {@code <Wrapper>.builder().setValue(withMetaArgument).setMeta(MetaFields.builder().set<X>(<val>)…)}
     *       (NO trailing {@code .build()} — the consumer wraps in {@code toBuilder(…)}).</li>
     *   <li><b>ReferenceWithMeta</b> (reference/address): return
     *       {@code <Wrapper>.builder().setValue(<arg>)<non-address .set…>[.setReference(Reference.builder().setReference(<addr>))].build()}.</li>
     * </ul>
     *
     * <p>Returns {@code null} (→ the legacy stub, today's non-compiling-but-waivered
     * bytes) on a type-meta entry (key/template — upstream's
     * {@code getOrCreateMeta()} statement form, a co-occupied deferred shape), a
     * non-meta / unresolvable output, a missing type utility, or (FieldWithMeta) a
     * sink-less path (rule/report/lambda interior — no hoist slot). Green-safe by
     * construction: the stub compiles to nothing valid, so no carrier was ever
     * green.
     */
    private JavaStatementBuilder tryTypedWithMeta(RWithMetaExpr expr, ExpressionContext ctx,
            ExpressionCompiler compiler) {
        if (ctx == null || ctx.scope() == null) {
            return null;
        }
        GeneratorModel gm = compiler.getGeneratorModel();
        var translator = compiler.getTypeTranslator();
        JavaTypeUtil typeUtil = compiler.getTypeUtil();
        if (gm == null || translator == null || typeUtil == null) {
            return null;
        }
        // The expected meta-wrapper type from the enclosing function output's meta
        // annotation (upstream's getRMetaAnnotatedType(expr)/context.expectedType).
        // NOTE: deriving the wrapper from the OUTPUT (rather than the with-meta
        // expression's OWN inferred RMetaAnnotatedType, as upstream does) is an
        // intentional simplification for the WHOLE-OUTPUT SET seat — the only seat
        // the flip carriers occupy. A NESTED with-meta whose type differs from the
        // output would mis-derive here, but harmlessly: the M7b-1 stub it replaces is
        // non-compiling, so such a file is already a waivered mismatch (green-safe).
        // facet ctorWithMetaReference (PR #348): the with-meta IS a ctor FIELD value
        // (`notionaReference: empty with-meta { reference: <href chain> first }` —
        // cdm6 MapMultipleExercise / MapEuropeanExerciseTerms). Upstream derives the
        // wrapper from the expression's OWN expected meta type; at the ctor seat that
        // is the TARGET attribute's [metadata …] annotation (notionaReference on
        // MultipleExercise → ReferenceWithMetaMoney), NOT the enclosing function
        // output's. The output derivation below stays the whole-output SET seat's law.
        // An unresolvable ctor pair falls through to the output walk (pre-facet bytes).
        RAttribute output = null;
        if (expr.parent() instanceof RKeyValuePair metaPair
                && metaPair.parent() instanceof RConstructorExpr metaCtor
                && metaCtor.typeCall() != null) {
            RType ctorResolved = gm.resolveTypeCall(metaCtor.typeCall());
            RDataType ctorDt = ctorResolved instanceof RDataTypeRef dtr ? dtr.astNode()
                    : ctorResolved instanceof RChoiceTypeRef ctr ? ctr.asRDataType() : null;
            if (ctorDt != null) {
                output = HandlerHelper.findAttributeOnDataType(ctorDt, metaPair.key());
            }
        }
        if (output == null) {
            RFunction fn = HandlerHelper.findEnclosingFunction(expr);
            if (fn == null) {
                return null;
            }
            output = fn.output().orElse(null);
        }
        if (output == null) {
            return null;
        }
        MetaFieldGenerator.MetaKind kind = MetaFieldGenerator.detectMetaKind(output);
        if (kind == MetaFieldGenerator.MetaKind.NONE) {
            return null;
        }
        RType valueRType = gm.getType(output);
        if (valueRType == null || valueRType instanceof com.regnosys.rosetta.types.RMissingType) {
            return null;
        }
        JavaClass<?> valueJavaType = translator.toJavaReferenceType(valueRType);
        if (valueJavaType == null) {
            return null;
        }
        // typedReturn=false — the output-derived arm keeps its historical NULL item
        // type (its consumers wrap in toBuilder(…)/renderSetBuilderChain and never
        // read the type; the corpus carriers are byte-frozen on this shape). Only
        // the own-type arm below returns typed, so the whole-output unwrap
        // coercion can see the wrapper.
        return buildTypedWithMeta(expr, ctx, compiler, kind, valueRType, valueJavaType,
                typeUtil, false);
    }

    /**
     * facet withMetaOwnTypeDerive (PR #421) — the with-meta expression's OWN
     * meta-type derivation, the third try-arm behind {@link #tryTypedWithMeta}
     * (output-annotation-derived) and {@link #tryPojoMetaWithMeta} (ctor+POJO
     * getOrCreateMeta). Upstream derives the wrapper from the EXPRESSION itself,
     * not the assignment target: {@code getRMetaAnnotatedType(expr)} carries the
     * ARGUMENT's RType plus the entry keys' meta attributes, and
     * {@code deriveJavaTypeWithDefault} (ExpressionGenerator.xtend L1504-1509)
     * falls back to the expected type ONLY when that RType is NOTHING — so
     * {@code value with-meta {scheme: …}} on a plain {@code string} output builds
     * {@code FieldWithMetaString} regardless of the output's annotations, and the
     * assignment seat then coerces the wrapper DOWN to the output's value type
     * (hold-out witness expr-with-meta: {@code final FieldWithMetaString
     * fieldWithMetaString = FieldWithMetaString.builder().setValue(withMetaArgument)
     * .setMeta(MetaFields.builder().setScheme("…"));} + the null-guarded
     * {@code .getValue()} unwrap — the renderer's setWithMetaValueUnwrap arm).
     *
     * <p>Declines ({@code null} → the legacy stub) on: a missing compile context;
     * an empty entry list (no meta to set — unwitnessed); any type-meta entry
     * (key/template — the getOrCreateMeta statement form, arm 2's class); an
     * entry set that yields no attribute-meta kind; an argument whose inferred
     * type is null, missing or NOTHING — the NOTHING case is upstream's
     * ReferenceWithMetaVoid mapping (a wrapper over {@code java.lang.Void}), the
     * cdm 6.20.6 upstream defect the fork deliberately does not reproduce (the
     * #405 user-directed PERMANENT waiver; the MetaFieldGenerator collection walk
     * excludes the same class) — or an untranslatable argument type.
     *
     * <p>Green-safe by construction (the #202 argument verbatim): this arm runs
     * only where BOTH prior arms decline, i.e. exactly the seats that land in the
     * legacy stub today — and the stub's {@code <arg>.toBuilder().setMeta(…)}
     * output compiles nowhere (MapperS carries no toBuilder), so every affected
     * carrier is a non-compiling waivered mismatch, never a green file. The kind
     * comes from {@link MetaFieldGenerator#entryMetaKind} — the SAME resolution
     * the collection walk uses, so the wrapper class this body references and the
     * wrapper class the generator emits cannot disagree.
     */
    private JavaStatementBuilder tryOwnTypeWithMeta(RWithMetaExpr expr, ExpressionContext ctx,
            ExpressionCompiler compiler) {
        if (ctx == null || ctx.scope() == null) {
            return null;
        }
        GeneratorModel gm = compiler.getGeneratorModel();
        var translator = compiler.getTypeTranslator();
        JavaTypeUtil typeUtil = compiler.getTypeUtil();
        if (gm == null || gm.workspace() == null || translator == null || typeUtil == null) {
            return null;
        }
        if (expr.entries().isEmpty()) {
            return null;
        }
        MetaFieldGenerator.MetaKind kind = MetaFieldGenerator.entryMetaKind(expr.entries());
        if (kind == MetaFieldGenerator.MetaKind.NONE) {
            return null;
        }
        RMetaAnnotatedType inferred = gm.workspace().getInferredType(expr.argument());
        if (inferred == null || inferred.isMissing() || inferred.type() == RBasicType.NOTHING) {
            return null;
        }
        RType valueRType = inferred.type();
        JavaClass<?> valueJavaType = translator.toJavaReferenceType(valueRType);
        if (valueJavaType == null) {
            return null;
        }
        return buildTypedWithMeta(expr, ctx, compiler, kind, valueRType, valueJavaType,
                typeUtil, true);
    }

    /**
     * The shared typed-wrapper construction tail (PR #421 extraction — the body is
     * the #202 arm's verbatim, parameterized by the derivation seat): builds the
     * {@code FieldWithMetaX}/{@code ReferenceWithMetaX} chain for {@code expr}
     * over the given value type. {@code typedReturn} controls the returned
     * expression's item type: {@code false} = the historical NULL type
     * (output-derived arm — byte-frozen consumers), {@code true} = the wrapper
     * type (own-type arm — lets the assignment-seat coercion unwrap
     * {@code .getValue()}).
     */
    private JavaStatementBuilder buildTypedWithMeta(RWithMetaExpr expr, ExpressionContext ctx,
            ExpressionCompiler compiler, MetaFieldGenerator.MetaKind kind, RType valueRType,
            JavaClass<?> valueJavaType, JavaTypeUtil typeUtil, boolean typedReturn) {
        // Type-meta entries (key/template) take upstream's getOrCreateMeta()
        // statement form — a co-occupied deferred shape; decline to the stub.
        for (RWithMetaEntry entry : expr.entries()) {
            if (isTypeMeta(entry.metaName())) {
                return null;
            }
        }
        JavaClass<?> wrapperType = RJavaWithMetaValue.create(
                kind == MetaFieldGenerator.MetaKind.REFERENCE_WITH_META, valueJavaType, typeUtil);

        Set<JavaClass<?>> refs = new HashSet<>();
        Set<JavaClass<?>> wildcards = new HashSet<>();
        refs.add(wrapperType);

        // The argument (the value being annotated), null-expected + item-coerced —
        // the same expected the stub uses (null at the function-body SET seat).
        JavaStatementBuilder argCompiled =
                compiler.compile(expr.argument(), ctx.expectedType(), ctx.scope());
        String argItem = withMetaItemForm(argCompiled, refs, wildcards);

        // needsBuilder = the value is a Rosetta MODEL type (RDataType/RChoiceType) — upstream's
        // argumentExpression then takes the null-guarded builder form `v == null ? null : v.toBuilder()`
        // (ExpressionGenerator.xtend:1434-1436, applied to BOTH wrapper kinds before the setValue). NOT
        // typeUtil.hasBuilderType: the translator returns an RGeneratedJavaClass (not a JavaPojoInterface)
        // for a data type, so hasBuilderType reads false (the #169 RTYPE-level pojo-ness lesson) — gate on
        // the RType. A null/basic value (Date, String, enum) stays bare. JavaLiteral.NULL never wraps
        // (upstream needsBuilder excludes it; the item-form already rendered "null").
        boolean needsBuilder = (valueRType instanceof RDataTypeRef
                || valueRType instanceof RChoiceTypeRef) && !"null".equals(argItem);
        String simpleName = valueJavaType.getSimpleName();
        String argValue = needsBuilder
                ? argItem + " == null ? null : " + argItem + ".toBuilder()"
                : argItem;

        if (kind == MetaFieldGenerator.MetaKind.REFERENCE_WITH_META) {
            // ReferenceWithMeta: setValue(<arg>) + non-address sets +
            // (address) setReference(Reference.builder().setReference(<addr>)); .build().
            StringBuilder sb = new StringBuilder();
            sb.append(wrapperType.getSimpleName()).append(".builder().setValue(").append(argValue).append(')');
            for (RWithMetaEntry entry : expr.entries()) {
                String setter = metaSetterName(entry);
                String val = withMetaItemForm(
                        compiler.compile(entry.value(), ctx.expectedType(), ctx.scope()), refs, wildcards);
                if ("address".equals(entry.metaName())) {
                    sb.append('.').append(setter).append("(Reference.builder().").append(setter)
                      .append('(').append(val).append("))");
                    refs.add(typeUtil.REFERENCE);
                } else {
                    sb.append('.').append(setter).append('(').append(val).append(')');
                }
            }
            sb.append(".build()");
            return JavaExpression.selfUnwrapping(
                    JavaExpression.from(sb.toString(), typedReturn ? wrapperType : null,
                            refs, wildcards));
        }

        // FieldWithMeta: hoist withMetaArgument (= argValue), then
        // Wrapper.builder().setValue(withMetaArgument).setMeta(MetaFields.builder().set<X>(<val>)…).
        JavaStatementScope sink = ctx.scope().findStatementHoistSink();
        if (sink == null) {
            return null;
        }
        // v3.2 seat 5 (PR #626, F6 — the chaos C9Stamp rows, the oracle groups withmeta-wrapped-argument / -edge): an
        // argument whose OWN Java type is the wrapper takes upstream's `argumentJavaType instanceof RJavaWithMetaValue`
        // arms (ExpressionGenerator.xtend:1443-1463) — the wrapper's BUILDER hoisted null-guarded
        // (`<arg> == null ? null : <arg>.toBuilder()`, the argument text twice, upstream's needsBuilder form), every
        // attribute-meta entry stamped as a `withMetaArgument.getOrCreateMeta().set<X>(<val>);` STATEMENT, and the
        // variable itself the result (a wrapper output renders `toBuilder(withMetaArgument)`, a plain output the
        // null-guarded `.getValue()` unwrap). The read is the compiled argument's expression TYPE (a typed channel,
        // LAW 69 with the SET seat's unwrap arm), never its rendered text; the wrapper class decides, not the meta
        // kind (an `id`-wrapped attribute under a `scheme` entry takes this arm too — golden SchemeOnId). The
        // value-type hoist below stays for a plain argument (goldens Plain / Literal, byte-frozen) and MetaFields is
        // never referenced here (the golden imports none).
        RJavaWithMetaValue argWrapper = withMetaArgumentWrapper(argCompiled, expr, compiler, typeUtil);
        if (argWrapper != null && argWrapper.equals(wrapperType)) {
            String wrapperSimple = wrapperType.getSimpleName();
            String sentinel = sink.statementHoistSession().register("withMetaArgument");
            // Round 1 (the code-quality review's SF-6): the wrapper's OUTER type goes through the same import-collision
            // sentinel the value-type hoist below uses (facet importCollisionFqn, PR #245) - two namespaces can each
            // generate a `ReferenceWithMetaKeyed`, and the loser of the first claim renders FQN-inline with its import
            // suppressed; off-collision the sentinel resolves to the bare simple name (byte-identical to the first cut).
            String wrapperRef = ImportCollisionResolver.typeRef(wrapperType.getCanonicalName().withDots());
            // round 2 (the code-quality review's NIT-2): the hoist's OWN type rides the refs as the value-type hoist's
            // does below - the output type names the same wrapper at every reachable site, so this is byte-inert
            // today (the chain measures it), and a stated guarantee rather than an incidental one.
            refs.add(wrapperType);
            sink.registerStatementHoist("final " + wrapperRef + "." + wrapperSimple + "Builder " + sentinel
                    + " = " + argItem + " == null ? null : " + argItem + ".toBuilder();");
            for (RWithMetaEntry entry : expr.entries()) {
                String setter = metaSetterName(entry);
                String val = withMetaItemForm(
                        compiler.compile(entry.value(), ctx.expectedType(), ctx.scope()), refs, wildcards);
                sink.registerStatementHoist(sentinel + ".getOrCreateMeta()." + setter + "(" + val + ");");
            }
            // Round 1 (the code-quality review's SF-3): the result is the PRODUCER-stamped witness that this value IS
            // the hoisted variable (JavaExpression.HoistedWithMetaVariable - the selfUnwrapping contract plus the
            // name), so the SET seat's in-place unwrap reads the fact, never a test of the rendered text.
            return JavaExpression.hoistedWithMetaVariable(
                    JavaExpression.from(sentinel, typedReturn ? wrapperType : null, refs, wildcards), sentinel);
        }
        // facet importCollisionFqn (PR #245): emit the value type as a first-claim-wins
        // sentinel so a body-internal collision with a different-canonical same-simple-name
        // type (typically the fpml INPUT param vs this cdm value type — e.g.
        // MapQuotedCurrencyPairWithLocation's `QuotedCurrencyPair`) renders this decl type
        // FQN-inline with its import suppressed. The nested `.<X>Builder` is appended AFTER
        // the sentinel, so only the OUTER (importable) type FQN-s; off-collision the sentinel
        // resolves to the bare simple name = the pre-facet form (byte-identical).
        String typeSentinel = ImportCollisionResolver.typeRef(valueJavaType.getCanonicalName().withDots());
        String hoistType = needsBuilder ? typeSentinel + "." + simpleName + "Builder" : typeSentinel;
        refs.add(valueJavaType);
        String sentinel = sink.statementHoistSession().register("withMetaArgument");
        sink.registerStatementHoist("final " + hoistType + " " + sentinel + " = " + argValue + ";");

        StringBuilder meta = new StringBuilder("MetaFields.builder()");
        for (RWithMetaEntry entry : expr.entries()) {
            String setter = metaSetterName(entry);
            String val = withMetaItemForm(
                    compiler.compile(entry.value(), ctx.expectedType(), ctx.scope()), refs, wildcards);
            meta.append('.').append(setter).append('(').append(val).append(')');
        }
        refs.add(typeUtil.META_FIELDS);
        String src = wrapperType.getSimpleName() + ".builder().setValue(" + sentinel + ").setMeta(" + meta + ")";
        return JavaExpression.selfUnwrapping(
                JavaExpression.from(src, typedReturn ? wrapperType : null, refs, wildcards));
    }

    /**
     * v3.2 seat 5 (PR #626, F6): the with-meta ARGUMENT's own meta wrapper, read the way the fork's
     * other meta seats read it (the #368 arm, the #613 channel) — the compiled Mapper's ITEM type when
     * the compile is typed, else the wrapper RECOVERED from the AST by
     * {@link NavigationHandler#recoverExprMetaWrapper} (a path's leaf attribute, an alias body, a
     * call's declared output — ONE typed walker, LAW 69); {@code null} when neither knows one (a plain
     * argument — the value-type hoist's domain).
     */
    private static RJavaWithMetaValue withMetaArgumentWrapper(JavaStatementBuilder argCompiled,
            RWithMetaExpr expr, ExpressionCompiler compiler, JavaTypeUtil typeUtil) {
        if (argCompiled instanceof JavaExpression argExpr && argExpr.getExpressionType() != null
                && typeUtil != null) {
            JavaType item = typeUtil.getItemType(argExpr.getExpressionType());
            if (item instanceof RJavaWithMetaValue typed) {
                return typed;
            }
        }
        return NavigationHandler.recoverExprMetaWrapper(expr.argument(), compiler);
    }

    /**
     * facet {@code metaLocationConstruction} (PR #214) — the POJO-meta complement of
     * {@link #tryTypedWithMeta}. A {@code with-meta { key/id/scheme: <val> }} on a
     * constructor whose OUTPUT is a plain Rosetta POJO (no meta wrapper, so
     * {@link MetaFieldGenerator#detectMetaKind} == {@code NONE}) populates the POJO's
     * OWN {@code MetaFields} through upstream's {@code getOrCreateMeta()} statement
     * form (FunctionGenerator.xtend metaClass path + PojoPropertyUtil key/id →
     * externalKey, scheme → scheme):
     *
     * <pre>
     * final &lt;Type&gt;.&lt;Type&gt;Builder withMetaArgument = &lt;ctor&gt; == null ? null : &lt;ctor&gt;.toBuilder();
     * withMetaArgument.getOrCreateMeta().setExternalKey(&lt;val&gt;.get());
     * &lt;out&gt; = toBuilder(withMetaArgument);                      // the SET consumer wraps the bare local
     * </pre>
     *
     * <p>Replaces the M7b-1 legacy stub's
     * {@code <ctor>.toBuilder().setMeta(MetaFields.builder().setKey(<val>)…)} —
     * {@code MetaFields} has no such external-key setter, so the stub is a
     * waivered mismatch on EVERY carrier; green-safe by construction (zero goldens
     * carry the stub form). Declines (→ the stub) on a meta-WRAPPER output (handled
     * by {@link #tryTypedWithMeta}), a non-model output, a meta entry that does not
     * map to a POJO meta setter (reference/address/location/template — no corpus
     * carrier), a missing type utility, or a sink-less path (rule/report/lambda
     * interior — no hoist slot).
     */
    private JavaStatementBuilder tryPojoMetaWithMeta(RWithMetaExpr expr, ExpressionContext ctx,
            ExpressionCompiler compiler) {
        if (ctx == null || ctx.scope() == null) {
            return null;
        }
        GeneratorModel gm = compiler.getGeneratorModel();
        var translator = compiler.getTypeTranslator();
        JavaTypeUtil typeUtil = compiler.getTypeUtil();
        if (gm == null || translator == null || typeUtil == null) {
            return null;
        }
        RFunction fn = HandlerHelper.findEnclosingFunction(expr);
        if (fn == null) {
            return null;
        }
        RAttribute output = fn.output().orElse(null);
        if (output == null) {
            return null;
        }
        // Complement of tryTypedWithMeta: only when the OUTPUT carries no meta wrapper
        // (a wrapper output is tryTypedWithMeta's domain — declining here avoids a
        // double-claim and the wrong POJO-meta shape for a FieldWithMetaX output).
        // facet withMetaArgumentHoist (PR #363): the #359 double-with-meta relax the
        // #358 gotcha banked — at a NESTED seat (this with-meta is the ARGUMENT of an
        // ENCLOSING RWithMetaExpr: `<ctor> with-meta { key: … } with-meta
        // { reference: … }`) the wrapper belongs to the OUTER op, and THIS op's
        // POJO-ness is proven by its ARGUMENT ctor (the RConstructorExpr gate below),
        // exactly upstream caseWithMetaOperation's per-node type derivation
        // (getRMetaAnnotatedType(expr) — the inner node is the plain POJO). Golden
        // cdm6 MapCreditEventsReferenceWithReference: the inner `key:` takes this
        // getOrCreateMeta() statement form, the outer `reference:` wraps the returned
        // withMetaArgument local (`.setValue(withMetaArgument == null ? null :
        // withMetaArgument.toBuilder())`). A NON-nested wrapper-output with-meta keeps
        // the decline (tryTypedWithMeta's domain).
        if (MetaFieldGenerator.detectMetaKind(output) != MetaFieldGenerator.MetaKind.NONE
                && !(expr.parent() instanceof RWithMetaExpr)) {
            return null;
        }
        // Derive the value type from the with-meta ARGUMENT's own constructor (NOT the
        // function output): the value being annotated is the constructed object, and for
        // a NESTED with-meta (a constructor member, e.g. Payout{commodityPayout: Foo{…}
        // with-meta{…}}) the argument type differs from the output — deriving from the
        // ctor keeps the `final <Foo>.<Foo>Builder withMetaArgument` hoist type correct.
        RType valueRType;
        boolean variableArgument = false;
        if (expr.argument() instanceof RConstructorExpr ctor && ctor.typeCall() != null) {
            valueRType = gm.resolveTypeCall(ctor.typeCall());
        } else if (expr.argument() instanceof RSymbolReference argRef && gm.workspace() != null) {
            variableArgument = true;
            // v3.2 seat 5 (PR #626, F6): a MODEL-typed symbol reference takes the POJO-meta form - upstream's
            // argument-typed arm (ExpressionGenerator.xtend:1452-1456) makes no distinction between its shapes, so the
            // gate admits every symbol reference the workspace types as a model value: an INPUT (golden KeyOnKeyed,
            // `final Keyed.KeyedBuilder withMetaArgument = k == null ? null : k.toBuilder();` +
            // `withMetaArgument.getOrCreateMeta().setExternalKey(…)`), an ALIAS (golden AliasKeyed, round 1) and a
            // function CALL returning a model value (golden CallKeyed, round 2 - pinned from the released plugin and
            // byte-identical on the fork's first run, `keyOnKeyed.evaluate(k) == null ? null :
            // keyOnKeyed.evaluate(k).toBuilder()`; the code-quality review had proposed narrowing the gate to bare
            // symbols, which the oracle refutes - the narrowing is lane A7's mutation). The legacy stub rendered
            // `toBuilder()` on a MapperS here — non-compiling, so every carrier was a mismatch (green-safe).
            RMetaAnnotatedType inferred = gm.workspace().getInferredType(argRef);
            valueRType = inferred == null || inferred.isMissing() ? null : inferred.type();
        } else {
            return null;
        }
        if (valueRType == null || valueRType instanceof com.regnosys.rosetta.types.RMissingType) {
            return null;
        }
        // Only a Rosetta MODEL value carries a meta field + builder + getOrCreateMeta().
        if (!(valueRType instanceof RDataTypeRef || valueRType instanceof RChoiceTypeRef)) {
            return null;
        }
        JavaClass<?> valueJavaType = translator.toJavaReferenceType(valueRType);
        if (valueJavaType == null) {
            return null;
        }
        // Every entry must map to a POJO meta setter (key/id → ExternalKey,
        // scheme → Scheme); a non-mappable entry declines the WHOLE op to the stub.
        List<String> metaProps = new ArrayList<>();
        for (RWithMetaEntry entry : expr.entries()) {
            String prop = pojoMetaSetterProp(entry.metaName());
            if (prop == null) {
                return null;
            }
            metaProps.add(prop);
        }
        JavaStatementScope sink = ctx.scope().findStatementHoistSink();
        // facet withMetaExternalKeyLambda (PR #358, F-C): the LAMBDA_CHANNEL twin — the
        // same withMetaArgument hoist + getOrCreateMeta() setter for a POJO-meta
        // with-meta INSIDE a map/extract lambda (golden MapNonNegativeStepList/
        // MapQuantityStepList/MapScheduleToDatedValueList cdm6: the block mapItem
        // lambda hoists `final DatedValue.DatedValueBuilder withMetaArgument = <ctor>
        // == null ? null : <ctor>.toBuilder();` + `withMetaArgument.getOrCreateMeta()
        // .setExternalKey(<id chain>.get());` and returns `MapperS.of(withMetaArgument)`
        // — the mapItem Mapper contract). The decl + setter register on the enclosing
        // lambda's pending-hoist channel (registerPendingLambdaHoist as #346-safe
        // raw-token statements) named by the per-METHOD session's per-LAMBDA sub-groups
        // (registerLambdaScoped — the #355 F-1 naming law). Render-truth declines (→ the
        // legacy stub, today's non-compiling bytes) copy the #355/#356 producer set:
        // value-then SUPPRESSION; a non-drainable enclosing lambda; a seat inside an
        // UNRESTRUCTURED runtime `.then(` lambda (the #350 thenArgRefFor read); no
        // reachable session/boundary.
        StatementHoistSession lambdaSession = null;
        JavaStatementScope lambdaBoundary = null;
        if (sink == null) {
            if (ctx.scope().isThenValueHoistSuppressed()
                    || !HandlerHelper.isInsideDrainableMapLambdaAllowingConditionalArms(expr)) {
                return null;
            }
            // facet thenBindingOverlay (v3.1 C2d retirement family 9, PR #616) — JUSTIFIED-KEPT
            // CONFIRMED (S25, occurrence 2 of 2 — ONE ledger row, two occurrences), MEASURED-INERT.
            // c9 census at this occurrence, 192 arrivals (72 / 72 / 48) = 96 entry + 96 pass, with
            // sawUnbound=false, unboundDepth=-1 and boundCountOnPath=0 at 96/96 and
            // chainTopFlag=false at 192/192 — the `return null` fires on no walk here either. Three
            // MapTo*List carriers, 64 arrivals each, node kind RWithMetaExpr at 96/96. Hazard H1 is
            // resolved by keeping all THREE sites together: see occurrence 1 above and
            // ControlFlowHandler:2191.
            RNode curThen = expr.parent();
            int thenDepth = 0;
            while (curThen != null && thenDepth++ < 64) {
                if (curThen instanceof RInlineFunction inlineThen
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

        Set<JavaClass<?>> refs = new HashSet<>();
        Set<JavaClass<?>> wildcards = new HashSet<>();
        refs.add(valueJavaType);

        // The constructed argument rendered RAW — the same bytes the legacy stub uses
        // (a constructor terminates in `.build()`; NO trailing `.get()` — withMetaItemForm
        // would wrongly append one to a non-Mapper). needsBuilder wraps a model value in
        // the null-guarded `v == null ? null : v.toBuilder()` double-construction the
        // golden carries (mirrors tryTypedWithMeta's needsBuilder form).
        JavaStatementBuilder argCompiled =
                compiler.compile(expr.argument(), ctx.expectedType(), ctx.scope());
        String argItem;
        if (variableArgument) {
            // v3.2 seat 5 (PR #626, F6): the variable's ITEM form through withMetaItemForm - THE ONE declaration of
            // the item ladder (LAW 69; round 1, the code-quality review's SF-1): a structural wrap unwraps to its
            // inner (an input `k`, never `MapperS.of(k)` - golden KeyOnKeyed), and a Mapper CHAIN - an alias call
            // `src(kh)`, the reference route's bare Mapper-returning call - appends `.get()` (golden AliasKeyed,
            // pinned from the released plugin at round 1: `src(kh).get() == null ? null : src(kh).get().toBuilder()`).
            // The first cut had copied the unwrap half alone and fell to the constructor branch's RAW render for
            // the alias - `src(kh).toBuilder()` on a MapperS, non-compiling. The constructor branch below stays
            // RAW on purpose (a `.build()`-terminated block takes no `.get()`).
            argItem = withMetaItemForm(argCompiled, refs, wildcards);
        } else {
            refs.addAll(argCompiled.getRefs());
            wildcards.addAll(argCompiled.getStaticWildcardImports());
            argItem = HandlerHelper.render(argCompiled);
        }
        boolean nullLit = "null".equals(argItem);
        String simpleName = valueJavaType.getSimpleName();
        String argValue = nullLit ? argItem
                : argItem + " == null ? null : " + argItem + ".toBuilder()";
        // facet withMetaArgumentHoist (PR #363): the decl type goes through the #245
        // first-claim import-collision sentinel (the tryTypedWithMeta L2018 law) — the
        // MapCreditEvents value type collides with the fpml INPUT's CreditEvents, so
        // golden declares `final cdm.observable.event.CreditEvents.CreditEventsBuilder
        // withMetaArgument`. Off-collision the sentinel resolves to the bare simple
        // name — byte-identical for every existing #214/#358 carrier.
        String typeSentinel = ImportCollisionResolver.typeRef(
                valueJavaType.getCanonicalName().withDots());
        String hoistType = nullLit ? typeSentinel
                : typeSentinel + "." + simpleName + "Builder";

        String sentinel = sink != null
                ? sink.statementHoistSession().register("withMetaArgument")
                : lambdaSession.registerLambdaScoped("withMetaArgument", lambdaBoundary);
        String declText = "final " + hoistType + " " + sentinel + " = " + argValue + ";";
        if (sink != null) {
            sink.registerStatementHoist(declText);
        } else {
            Set<JavaClass<?>> declRefs = new HashSet<>(argCompiled.getRefs());
            declRefs.add(valueJavaType);
            lambdaBoundary.registerPendingLambdaHoist(new JavaRawStatement(declText,
                    declRefs, argCompiled.getStaticWildcardImports()));
        }
        int i = 0;
        for (RWithMetaEntry entry : expr.entries()) {
            Set<JavaClass<?>> valRefs = new HashSet<>();
            Set<JavaClass<?>> valWildcards = new HashSet<>();
            String val = withMetaItemForm(
                    compiler.compile(entry.value(), ctx.expectedType(), ctx.scope()),
                    valRefs, valWildcards);
            refs.addAll(valRefs);
            wildcards.addAll(valWildcards);
            String setterText =
                    sentinel + ".getOrCreateMeta().set" + metaProps.get(i) + "(" + val + ");";
            if (sink != null) {
                sink.registerStatementHoist(setterText);
            } else {
                lambdaBoundary.registerPendingLambdaHoist(
                        new JavaRawStatement(setterText, valRefs, valWildcards));
            }
            i++;
        }
        if (sink != null) {
            return JavaExpression.selfUnwrapping(JavaExpression.from(sentinel, null, refs, wildcards));
        }
        Set<JavaClass<?>> retRefs = new HashSet<>(refs);
        retRefs.add(HandlerHelper.MAPPER_S);
        return JavaExpression.from("MapperS.of(" + sentinel + ")", null, retRefs, wildcards);
    }

    /**
     * Meta-name → POJO {@code getOrCreateMeta().set<Prop>} property (upstream
     * PojoPropertyUtil): {@code key}/{@code id} → {@code ExternalKey},
     * {@code scheme} → {@code Scheme}; {@code null} for any other / wrapper meta.
     */
    private static String pojoMetaSetterProp(String metaName) {
        // seat 21: the NAME comes from the ONE table (HandlerHelper.metaPojoPropertyName); the
        // admission set of this setter seat is unchanged (key/id/scheme — the other faces have
        // zero assignment-segment carriers in every cell and stay declined here).
        return switch (metaName) {
            case "key", "id", "scheme" -> {
                String prop = HandlerHelper.metaPojoPropertyName(metaName);
                yield Character.toUpperCase(prop.charAt(0)) + prop.substring(1);
            }
            default -> null;
        };
    }

    /**
     * Item-form of a compiled with-meta value (the bare inner): a structural
     * MapperS/MapperC wrap strips to its inner (INNER refs only — the discarded
     * wrap must not pin a stale Mapper import), a {@code null} literal and an
     * enum constant (by the producer's witness) pass bare, and a Mapper chain appends {@code .get()}.
     * Mirrors the {@code coerceCtorArg}/{@code HoistArmRenderer} item ladders.
     */
    private String withMetaItemForm(JavaStatementBuilder compiled,
            Set<JavaClass<?>> refs, Set<JavaClass<?>> wildcards) {
        if (compiled instanceof JavaExpression je) {
            Optional<JavaStatementBuilder> unwrap = je.unwrapToBuilder();
            if (unwrap.isPresent()) {
                JavaStatementBuilder inner = unwrap.get();
                refs.addAll(inner.getRefs());
                wildcards.addAll(inner.getStaticWildcardImports());
                return HandlerHelper.render(inner);
            }
        }
        refs.addAll(compiled.getRefs());
        wildcards.addAll(compiled.getStaticWildcardImports());
        String src = HandlerHelper.render(compiled);
        if (HandlerHelper.isBareEnumConstant(compiled) || "null".equals(src)) {
            return src;
        }
        return src + ".get()";
    }

    /**
     * The with-meta constructor's {@code set<Prop>} setter for one {@code [meta = value]} entry,
     * named through THE ONE meta-face → POJO property-name table
     * ({@code HandlerHelper.metaPojoPropertyName} — upstream {@code PojoPropertyUtil.toPojoPropertyName};
     * the complete mirror that lived here was retired at the seat-21 review, LAW 69). An entry
     * without a meta name is a parser defect and is REFUSED (v3.1 C0): the table's {@code null}
     * passthrough would otherwise render the literal {@code .setnull(…)} into generated Java.
     */
    static String metaSetterName(RWithMetaEntry entry) {
        String metaName = entry.metaName();
        if (metaName == null) {
            throw SilentDegradation.refuse(SilentDegradation.Site.WITH_META_ENTRY_UNNAMED,
                    "a with-meta constructor entry without a meta name (the `.setnull(…)` setter)",
                    entry);
        }
        return "set" + JavaNamingUtil.toFirstUpper(HandlerHelper.metaPojoPropertyName(metaName));
    }

    /** Type-level meta (upstream {@code RosettaEcoreUtil.isTypeMeta}): key / template. */
    private static boolean isTypeMeta(String metaName) {
        return "key".equals(metaName) || "template".equals(metaName);
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Converts a Rune field/meta name to the corresponding Java setter name.
     *
     * <p>Example: {@code "price"} -> {@code "setPrice"}.
     */
    private String toSetterName(String fieldName) {
        if (fieldName == null || fieldName.isEmpty()) {
            return "set";
        }
        return "set" + JavaNamingUtil.toFirstUpper(fieldName);
    }

}
