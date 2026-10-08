package com.regnosys.rosetta.ir.expr.adapter;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.expressions.binary.RThenExpr;
import com.regnosys.rosetta.ast.expressions.constructors.RConditionalExpr;
import com.regnosys.rosetta.ast.expressions.constructors.RConstructorExpr;
import com.regnosys.rosetta.ir.expr.anf.BigIntegerHoist;
import com.regnosys.rosetta.ir.expr.anf.BooleanConditionHoist;
import com.regnosys.rosetta.ir.expr.anf.ConditionalHoist;
import com.regnosys.rosetta.ir.expr.anf.ThenChainHoist;
import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.annotations.RAnnotation;
import com.regnosys.rosetta.ast.annotations.RAnnotationRef;
import com.regnosys.rosetta.ast.regulatory.RBody;
import com.regnosys.rosetta.ast.regulatory.RCorpus;
import com.regnosys.rosetta.ast.regulatory.RSegmentDef;
import com.regnosys.rosetta.ast.enums.ArithOp;
import com.regnosys.rosetta.ast.enums.CompOp;
import com.regnosys.rosetta.ast.enums.ConversionKind;
import com.regnosys.rosetta.ast.enums.EqOp;
import com.regnosys.rosetta.ast.enums.ExistenceOp;
import com.regnosys.rosetta.ast.enums.ExistsModifier;
import com.regnosys.rosetta.ast.enums.ListOp;
import com.regnosys.rosetta.ast.enums.LogOp;
import com.regnosys.rosetta.ast.RBinaryExpression;
import com.regnosys.rosetta.ast.expressions.binary.RArithmeticExpr;
import com.regnosys.rosetta.ast.expressions.binary.RComparisonExpr;
import com.regnosys.rosetta.ast.expressions.binary.RContainsExpr;
import com.regnosys.rosetta.ast.expressions.binary.RDefaultExpr;
import com.regnosys.rosetta.ast.expressions.binary.RDisjointExpr;
import com.regnosys.rosetta.ast.expressions.binary.REqualityExpr;
import com.regnosys.rosetta.ast.expressions.binary.RJoinExpr;
import com.regnosys.rosetta.ast.expressions.binary.RLogicalExpr;
import com.regnosys.rosetta.ast.expressions.unary.RConversionExpr;
import com.regnosys.rosetta.ast.expressions.unary.RCountExpr;
import com.regnosys.rosetta.ast.expressions.unary.RExistenceExpr;
import com.regnosys.rosetta.ast.expressions.unary.RExtractExpr;
import com.regnosys.rosetta.ast.expressions.unary.RFilterExpr;
import com.regnosys.rosetta.ast.expressions.unary.RListOpExpr;
import com.regnosys.rosetta.ast.expressions.unary.RMaxExpr;
import com.regnosys.rosetta.ast.expressions.unary.RMinExpr;
import com.regnosys.rosetta.ast.expressions.unary.ROnlyExistsExpr;
import com.regnosys.rosetta.ast.expressions.unary.RReduceExpr;
import com.regnosys.rosetta.ast.expressions.unary.RSortExpr;
import com.regnosys.rosetta.ast.expressions.unary.RToStringExpr;
import com.regnosys.rosetta.ast.expressions.unary.RWithMetaExpr;
import com.regnosys.rosetta.ast.expressions.supporting.RInlineFunction;
import com.regnosys.rosetta.ast.expressions.supporting.RKeyValuePair;
import com.regnosys.rosetta.ast.expressions.supporting.ROnlyExistsElement;
import com.regnosys.rosetta.ast.expressions.supporting.RSwitchCase;
import com.regnosys.rosetta.ast.expressions.supporting.RSwitchCaseGuard;
import com.regnosys.rosetta.ast.enums.SwitchGuardKind;
import com.regnosys.rosetta.ast.expressions.unary.RSwitchExpr;
import com.regnosys.rosetta.ast.expressions.literals.RBooleanLiteral;
import com.regnosys.rosetta.ast.expressions.literals.RIntLiteral;
import com.regnosys.rosetta.ast.expressions.literals.RListLiteral;
import com.regnosys.rosetta.ast.expressions.literals.RNumberLiteral;
import com.regnosys.rosetta.ast.expressions.literals.RStringLiteral;
import com.regnosys.rosetta.ast.expressions.references.RDeepFeatureCall;
import com.regnosys.rosetta.ast.expressions.references.REmptyLiteral;
import com.regnosys.rosetta.ast.expressions.references.REnumValueRef;
import com.regnosys.rosetta.ast.expressions.references.RFeatureCall;
import com.regnosys.rosetta.ast.expressions.references.RImplicitVariable;
import com.regnosys.rosetta.ast.expressions.references.RSuperCall;
import com.regnosys.rosetta.ast.expressions.references.RSymbolReference;
import com.regnosys.rosetta.ast.functions.RCondition;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.functions.ROperation;
import com.regnosys.rosetta.ast.functions.RRule;
import com.regnosys.rosetta.ast.functions.RShortcut;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.supporting.REnumValue;
import com.regnosys.rosetta.ast.supporting.RChoiceOption;
import com.regnosys.rosetta.ast.supporting.RRecordFeature;
import com.regnosys.rosetta.ast.supporting.RTypeCall;
import com.regnosys.rosetta.ast.types.RBasicType;
import com.regnosys.rosetta.ast.types.RChoice;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.ast.types.REnumeration;
import com.regnosys.rosetta.ast.types.RLibraryFunction;
import com.regnosys.rosetta.ast.types.RRecordType;
import com.regnosys.rosetta.ast.types.RTypeAlias;
import com.regnosys.rosetta.ir.expr.BinaryOp;
import com.regnosys.rosetta.ir.expr.Existence;
import com.regnosys.rosetta.ir.expr.FieldAccess;
import com.regnosys.rosetta.ir.expr.IRAllAnyCompare;
import com.regnosys.rosetta.ir.expr.IRApply;
import com.regnosys.rosetta.ir.expr.IRClosureParam;
import com.regnosys.rosetta.ir.expr.IRConditional;
import com.regnosys.rosetta.ir.expr.IRConstruct;
import com.regnosys.rosetta.ir.expr.IRConversion;
import com.regnosys.rosetta.ir.expr.IRLambdaOp;
import com.regnosys.rosetta.ir.expr.IRLibraryApply;
import com.regnosys.rosetta.ir.expr.IREmptyLiteral;
import com.regnosys.rosetta.ir.expr.IRExpr;
import com.regnosys.rosetta.ir.expr.IRLiteral;
import com.regnosys.rosetta.ir.expr.IRListConstruct;
import com.regnosys.rosetta.ir.expr.IROutputAliasNav;
import com.regnosys.rosetta.ir.expr.IRListOp;
import com.regnosys.rosetta.ir.expr.IRMetaAccess;
import com.regnosys.rosetta.ir.expr.IRMetaOutputApply;
import com.regnosys.rosetta.ir.expr.IROnlyExists;
import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.ir.expr.IRPipe;
import com.regnosys.rosetta.ir.expr.IRPointFreeApply;
import com.regnosys.rosetta.ir.expr.IRReference;
import com.regnosys.rosetta.ir.expr.IRSymbolNav;
import com.regnosys.rosetta.ir.expr.IRChoiceOptionNav;
import com.regnosys.rosetta.ir.expr.IRDeepFeatureNav;
import com.regnosys.rosetta.ir.expr.IRDispatchInputRef;
import com.regnosys.rosetta.ir.expr.IRMetaParamRef;
import com.regnosys.rosetta.ir.expr.IROutputRef;
import com.regnosys.rosetta.ir.expr.IRImplicitAttrNav;
import com.regnosys.rosetta.ir.expr.IRConditionInstance;
import com.regnosys.rosetta.ir.expr.IRRuleInputNav;
import com.regnosys.rosetta.ir.expr.IRRecordFeatureNav;
import com.regnosys.rosetta.ir.expr.IRChoiceReceiverNav;
import com.regnosys.rosetta.ir.expr.IRRecordReceiverNav;
import com.regnosys.rosetta.ir.expr.IRQualifierItemNav;
import com.regnosys.rosetta.ir.expr.IRQualifierReceiverNav;
import com.regnosys.rosetta.ir.expr.IRSwitchOp;
import com.regnosys.rosetta.ir.expr.IRDefaultOp;
import com.regnosys.rosetta.ir.expr.IRMembershipOp;
import com.regnosys.rosetta.ir.expr.IRCollectOp;
import com.regnosys.rosetta.ir.expr.IRWithMetaOp;
import com.regnosys.rosetta.ir.expr.IRJoinOp;
import com.regnosys.rosetta.ir.expr.IRMetaItemNav;
import com.regnosys.rosetta.ir.expr.IRSynItemNav;
import com.regnosys.rosetta.ir.expr.IRToString;
import com.regnosys.rosetta.ir.expr.IRVariable;
import com.regnosys.rosetta.ir.expr.Let;
import com.regnosys.rosetta.ir.expr.NodeId;
import com.regnosys.rosetta.ir.expr.Optionality;
import com.regnosys.rosetta.symbols.RWorkspace;
import com.regnosys.rosetta.types.ExpressionCardinality;
import com.regnosys.rosetta.types.RChoiceTypeRef;
import com.regnosys.rosetta.types.RDataTypeRef;
import com.regnosys.rosetta.types.REnumTypeRef;
import com.regnosys.rosetta.types.RMetaAnnotatedType;
import com.regnosys.rosetta.types.RNumberType;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Lowers a Rune {@code RExpression} subtree into the neutral {@link IRExpr} hierarchy.
 *
 * <h2>Total over the seam, partial over the wave</h2>
 * {@link #adapt(RExpression, RWorkspace)} returns a present {@link IRExpr} <em>iff the
 * whole subtree is expressible with the node kinds this wave has landed</em>; if any
 * node is not yet modelled it returns {@link Optional#empty()}. That all-or-nothing
 * contract is exactly what the strangler seam needs: a seam root either lowers fully to
 * IR (and may be IR-driven) or stays on the legacy generator — never a half-built tree
 * that would disagree with the legacy emitter on surrounding context (design §7).
 *
 * <p><strong>Wave 0</strong> models the unambiguous leaf primitives: the four scalar
 * literals, {@code empty}, list literals over already-expressible elements, the implicit
 * {@code item} variable, and {@code super}. The <strong>references tier</strong> has begun
 * with the bare <em>function-parameter</em> case of {@code RSymbolReference} (scalar →
 * {@code MapperS.of}, multi → {@code MapperC.<Item>of}; {@code IRVariable{PARAM}}, see
 * {@link #adaptSymbolReference}) and the bare
 * <em>enum-value</em> reference ({@code IRReference{ENUM_VALUE}}); the <strong>structural
 * tier</strong> with the ordered <em>comparison</em> and the <em>equality</em> of two scalar
 * parameters and the <em>logical</em> {@code and}/{@code or} that composes those boolean leaves
 * ({@link BinaryOp}, see {@link #adaptComparison} / {@link #adaptEquality} / {@link #adaptLogical}),
 * plus the unary <em>existence</em> check over a scalar parameter ({@link Existence}, see
 * {@link #adaptExistence}) — itself a boolean leaf that composes through logical — and the
 * <em>feature navigation</em> (single hop, {@code a -> b -> c} chains, multi features, chains through a
 * multi hop, and a MULTI-parameter base) off any function-parameter or navigation receiver
 * ({@link FieldAccess}, see
 * {@link #adaptFeatureCall}). An alias/shortcut reference now lowers to {@code IRReference{ALIAS}},
 * a bare rule delegation to an argument-less {@link IRApply} over an {@code IRReference{RULE}} callee,
 * and the flat plain-function call to {@link IRApply} (see {@link #adaptApply}); a bare point-free
 * FUNCTION reference stays a deliberate decline (L-109), and meta features and the remaining compound
 * families return empty until their increment — each a labelled extension point
 * in {@link #adapt(RExpression, NodeId, RWorkspace)}. {@link #declineReason} names the first
 * declining gate for any node this adapter does not lower (the #477 per-ARM decode channel).
 *
 * <h2>Computed facts</h2>
 * {@code type} and {@code cardinality} are read from the workspace's memoized inference
 * ({@link RWorkspace#getInferredType}/{@link RWorkspace#getCardinality}) — O(1) lookups,
 * never a re-walk. {@code optionality} is set from leaf rules here (literals/list/item:
 * PRESENT; {@code empty}: OPTIONAL); the absorbing-monoid chain derivation ({@link #chainOptionality}) is computed for
 * navigation (design §6). {@link NodeId}s are assigned top-down as child-slot paths over
 * the IR's own {@link IRExpr#children()} order.
 *
 * <p>Stateless and side-effect-free; safe to share across models. Lab-authored Phase-2.
 */
public final class ExpressionToIRAdapter {

    /**
     * Lower {@code expr}, treated as a seam root (identity {@link NodeId#ROOT}). Returns
     * empty when the subtree contains any not-yet-modelled node (see class docs).
     *
     * @param expr      the expression to lower; must be non-null
     * @param workspace the resolved workspace providing memoized type/cardinality facts
     */
    public Optional<IRExpr> adapt(RExpression expr, RWorkspace workspace) {
        return adapt(expr, NodeId.ROOT, workspace);
    }

    private Optional<IRExpr> adapt(RExpression expr, NodeId id, RWorkspace ws) {
        if (expr instanceof RIntLiteral lit) {
            return Optional.of(new IRLiteral(IRLiteral.LiteralKind.INT, lit.value(), id,
                    type(lit, ws), card(lit, ws), Optionality.PRESENT, lit.sourceRange()));
        }
        if (expr instanceof RNumberLiteral lit) {
            return Optional.of(new IRLiteral(IRLiteral.LiteralKind.NUMBER, lit.value(), id,
                    type(lit, ws), card(lit, ws), Optionality.PRESENT, lit.sourceRange()));
        }
        if (expr instanceof RStringLiteral lit) {
            return Optional.of(new IRLiteral(IRLiteral.LiteralKind.STRING, lit.value(), id,
                    type(lit, ws), card(lit, ws), Optionality.PRESENT, lit.sourceRange()));
        }
        if (expr instanceof RBooleanLiteral lit) {
            return Optional.of(new IRLiteral(IRLiteral.LiteralKind.BOOLEAN, lit.value(), id,
                    type(lit, ws), card(lit, ws), Optionality.PRESENT, lit.sourceRange()));
        }
        if (expr instanceof REmptyLiteral empty) {
            return Optional.of(new IREmptyLiteral(IREmptyLiteral.EmptySource.USER_EMPTY, id,
                    type(empty, ws), card(empty, ws), Optionality.OPTIONAL, empty.sourceRange()));
        }
        if (expr instanceof RImplicitVariable var) {
            IRVariable.VariableKind kind = var.isSynthetic()
                    ? IRVariable.VariableKind.SYNTHETIC_ITEM
                    : IRVariable.VariableKind.USER_ITEM;
            return Optional.of(new IRVariable("item", kind, id,
                    type(var, ws), card(var, ws), Optionality.PRESENT, var.sourceRange()));
        }
        if (expr instanceof RSuperCall superCall) {
            return Optional.of(new IRReference("", IRReference.ReferenceKind.SUPER, id,
                    type(superCall, ws), card(superCall, ws), Optionality.PRESENT, superCall.sourceRange()));
        }
        if (expr instanceof RListLiteral list) {
            // #526: a claim-ROOT list's elements are ADMITTED slots (the root-only law's FOURTH
            // named seat — see adaptAdmittedSlot); a child-position list keeps the plain dispatch.
            return adaptListLiteral(list, id, ws, id.path().isEmpty());
        }

        if (expr instanceof RSymbolReference ref) {
            return adaptSymbolReference(ref, id, ws);
        }
        if (expr instanceof REnumValueRef enr) {
            return adaptEnumValueRef(enr, id, ws);
        }
        if (expr instanceof RComparisonExpr cmp) {
            return adaptComparison(cmp, id, ws);
        }
        if (expr instanceof REqualityExpr eq) {
            return adaptEquality(eq, id, ws);
        }
        if (expr instanceof RLogicalExpr log) {
            return adaptLogical(log, id, ws);
        }
        if (expr instanceof RExistenceExpr exist) {
            return adaptExistence(exist, id, ws);
        }
        if (expr instanceof RFeatureCall fc) {
            return adaptFeatureCall(fc, id, ws);
        }
        if (expr instanceof RDeepFeatureCall deep) {
            return adaptDeepFeatureCall(deep, id, ws);
        }
        if (expr instanceof RArithmeticExpr arith) {
            return adaptArithmetic(arith, id, ws);
        }
        if (expr instanceof RListOpExpr listOp) {
            return adaptListOp(listOp, id, ws);
        }
        if (expr instanceof RCountExpr count) {
            return adaptCount(count, id, ws);
        }
        if (expr instanceof RToStringExpr toString) {
            return adaptToString(toString, id, ws);
        }
        if (expr instanceof RConditionalExpr cond) {
            return adaptConditional(cond, id, ws);
        }
        if (expr instanceof RConstructorExpr ctor) {
            return adaptConstructor(ctor, id, ws);
        }
        if (expr instanceof RExtractExpr extract) {
            return adaptLambdaOp(IRLambdaOp.Op.EXTRACT, extract, extract.argument(), extract.body(), id, ws);
        }
        if (expr instanceof RFilterExpr filter) {
            return adaptLambdaOp(IRLambdaOp.Op.FILTER, filter, filter.argument(), filter.body(), id, ws);
        }
        if (expr instanceof RConversionExpr conv) {
            return adaptConversion(conv, id, ws);
        }
        if (expr instanceof RThenExpr then) {
            return adaptPipe(then, id, ws);
        }
        if (expr instanceof ROnlyExistsExpr onlyExists) {
            return adaptOnlyExists(onlyExists, id, ws);
        }
        if (expr instanceof RSwitchExpr sw) {
            return adaptSwitchOp(sw, id, ws);
        }
        if (expr instanceof RDefaultExpr def) {
            return adaptDefaultOp(def, id, ws);
        }
        if (expr instanceof RContainsExpr contains) {
            return adaptMembershipOp(IRMembershipOp.Op.CONTAINS, contains, id, ws);
        }
        if (expr instanceof RDisjointExpr disjoint) {
            return adaptMembershipOp(IRMembershipOp.Op.DISJOINT, disjoint, id, ws);
        }
        if (expr instanceof RMaxExpr max) {
            return adaptCollectOp(IRCollectOp.Op.MAX, max, max.argument(),
                    max.body().orElse(null), id, ws);
        }
        if (expr instanceof RMinExpr min) {
            return adaptCollectOp(IRCollectOp.Op.MIN, min, min.argument(),
                    min.body().orElse(null), id, ws);
        }
        if (expr instanceof RSortExpr sort) {
            return adaptCollectOp(IRCollectOp.Op.SORT, sort, sort.argument(),
                    sort.body().orElse(null), id, ws);
        }
        if (expr instanceof RWithMetaExpr withMeta) {
            return adaptWithMetaOp(withMeta, id, ws);
        }
        if (expr instanceof RJoinExpr join) {
            return adaptJoinOp(join, id, ws);
        }

        // --- Extension points (return empty → seam stays on legacy fallback) ---------
        // References tier (started): the bare scalar function-parameter case of
        //          RSymbolReference and bare enum-value refs lower above; aliases,
        //          function/rule references and calls, and the rest of the 6-way
        //          REnumValueRef split (design §3 Group B′) still return empty.
        // Structural (started): COMPARISON, EQUALITY and LOGICAL and/or (the boolean binary
        //          operators), the unary EXISTENCE check, and FEATURE-CALL navigation (single hop,
        //          a -> b -> c chains, multi features, chains THROUGH a multi hop, and a MULTI
        //          parameter base, off any param/navigation receiver, single/multi non-meta getter)
        //          lower above; arithmetic, only-exists/cardinality checks, meta features and the
        //          witness-heavy reference arms follow.
        // Later:   existence; collections; conversion/construction; control flow + then.
        //          IRApply drives the slice-1 function call (scalar args, scalar single output, plain RFunction)
        //          via adaptSymbolReference → adaptApply; multi/meta/nav-arg/nested/bare-reference calls defer.
        return Optional.empty();
    }

    /**
     * Extract the {@link ConditionalHoist} facts of a conditional expression — the {@code ifThenElseResult}
     * hoist's structural key, declared type and {@code else}-presence. <strong>Dual-use:</strong> the Phase-A
     * offline ANF harness ({@code Wave6AnfSkeletonTest}) AND the live Wave-6 Phase-C renderer
     * ({@code IRFunctionExpressionRenderer}, which sources the hoist NAME from the resulting Bind). It is
     * deliberately a <em>separate</em> entry point from the shared
     * {@link #adapt(RExpression, NodeId, RWorkspace)} dispatch: even though Phase C calls it live, and even
     * though {@code adapt} now DOES lower a {@code RConditionalExpr} to a full {@link IRConditional} (step-12;
     * this method predates and is independent of that arm, and is never itself called from {@code adapt}),
     * byte/§4.2 inertness of the COMPILER path is unaffected — it rests on the Java-side facts (no
     * {@code visitConditional} override, no {@code IRJavaLeafEmitter} case) documented in
     * {@link #adaptConditional}'s byte-safety note (design §4, L-132), NOT on {@code adapt}'s own inertness; the
     * renderer-side hoist driving here is gated by the byte gate, not §4.2 (decision-log
     * L-053/L-055/L-060/L-061). (Formerly {@code adaptConditionalForHarness}, before Phase C made it live.)
     *
     * <p>The declared type is the THEN branch's inferred type ({@code getInferredType(thenBranch)}), which
     * equals the conditional's own type only when {@code else} is absent (Rune's absent-else ⇒ then value or
     * {@code null}); it resolves even when the branch interior does not lower to IR (the type engine resolves
     * a feature-call from its linked attribute before touching an L-032-{@code MISSING} alias receiver), so
     * the skeleton's local-decl type is reproducible offline.
     *
     * @param conditional the AST conditional (a SET-position {@code RConditionalExpr})
     * @param id          the conditional's {@link NodeId} within its seam (the harness adapts each SET
     *                    operation's RHS as its own seam root, so this is typically {@link NodeId#ROOT})
     * @param ws          the linked workspace, for type inference
     */
    public ConditionalHoist adaptConditionalFacts(RConditionalExpr conditional, NodeId id, RWorkspace ws) {
        // The condition hoists a `final Boolean` temp (rendered BEFORE the ifThenElseResult it guards) ONLY when
        // it is a BARE FUNCTION CALL (dump §6); a comparison/equality/existence condition renders inline as a
        // ComparisonResult and is NOT hoisted. The hoist is detected STRUCTURALLY (the function-call AST node),
        // not by lowering the condition — so a condition that does not lower (e.g. a call with a still-declining
        // arg: a meta-nav, a post-#489 argNav decline face, or an empty arg, the separate argEmpty gate) still
        // hoists. The decl type comes from the condition's inferred `boolean` type.
        RExpression condition = conditional.condition();
        boolean conditionHoists = isBareFunctionCall(condition);
        RMetaAnnotatedType conditionType = conditionHoists ? type(condition, ws) : RMetaAnnotatedType.MISSING;
        return new ConditionalHoist(id, type(conditional.thenBranch(), ws), hasGenuineElse(conditional),
                id.child(0), conditionHoists, conditionType);
    }

    /**
     * Extract the {@link BooleanConditionHoist} facts of a renderer-direct {@code boolean} condition hoist — the
     * structural decision to hoist plus the temporary's {@link NodeId} key and its declared {@code Boolean} type.
     * Used by the live Wave-6 Phase-C renderer ({@code IRFunctionExpressionRenderer.booleanHoistBaseName}, which
     * sources the renderer-direct {@code boolean} hoist NAME from the resulting {@link com.regnosys.rosetta.ir.expr.anf.Bind},
     * slice-3, L-063). Like {@link #adaptConditionalFacts} / {@link #adaptThenChainFacts}, a SEPARATE entry point
     * NOT wired into the shared {@link #adapt(RExpression, NodeId, RWorkspace)} dispatch — this method itself is
     * never reached from {@code adapt} (unaffected by {@code adapt}'s own {@code RConditionalExpr} arm, step-12);
     * the COMPILER path's byte/§4.2 inertness rests on the same Java-side facts in {@link #adaptConditional}'s
     * byte-safety note (design §4, L-132), not on {@code adapt}'s dispatch shape, even though Phase C calls this
     * live (decision-log L-053/L-055/L-061/L-062).
     *
     * <p>Unlike {@link #adaptConditionalFacts} this infers ONLY the condition's own type (when it hoists), NOT the
     * then-branch type — the renderer-direct seat (a whole-output SET-conditional or alias return-ladder rung)
     * renders the conditional INLINE with no {@code ifThenElseResult} local whose declared type would need it. The
     * hoist is detected STRUCTURALLY ({@link #isBareFunctionCall} — the IDENTICAL node predicate as the legacy
     * {@code HandlerHelper.isBareFunctionCallCondition} gate), so it agrees with the renderer wherever the legacy
     * boolean-hoist branch fired ({@code conditionHoists} is the AST half — a superset of the legacy decision,
     * which also pairs the {@code MapperS.of} unwrap witness; the override is invoked only inside that branch).
     *
     * @param conditional the SET-conditional / return-ladder-rung conditional whose condition may hoist
     * @param id          the conditional's {@link NodeId} within its seam (typically {@link NodeId#ROOT})
     * @param ws          the linked workspace, for the condition's type inference
     */
    public BooleanConditionHoist adaptBooleanConditionFacts(RConditionalExpr conditional, NodeId id, RWorkspace ws) {
        RExpression condition = conditional.condition();
        boolean conditionHoists = isBareFunctionCall(condition);
        RMetaAnnotatedType conditionType = conditionHoists ? type(condition, ws) : RMetaAnnotatedType.MISSING;
        return new BooleanConditionHoist(id.child(0), conditionType, conditionHoists);
    }

    /**
     * Whether an expression is a <em>bare function call</em> — an {@link RSymbolReference} whose resolved
     * symbol is an {@link RFunction} (the shape {@code adaptApply} drives). Such a boolean-typed condition
     * renders as a bare {@code Boolean} and is hoisted to a {@code final Boolean} temporary; everything else
     * (comparisons, equalities, existence checks) renders as a {@code ComparisonResult} and stays inline.
     */
    private static boolean isBareFunctionCall(RExpression expr) {
        return expr instanceof RSymbolReference ref && ref.symbol().orElse(null) instanceof RFunction;
    }

    /**
     * Whether a conditional has a <em>genuine</em>, value-bearing {@code else} branch — as opposed to the
     * synthetic empty-list else that {@code DefaultElseRule} stamps (with {@code SourceRange.NONE}) onto
     * every {@code if X then Y} that omits {@code else}. An absent or empty else means the generator's
     * absent-else lowering ({@code <Type> t = null; if(cond){…}}, the local stays {@code null}); a genuine
     * else value is a join lowering that Phase-A slice 1 does not yet handle. The discriminator is
     * structural emptiness ({@code empty} / an empty list literal), so a user-written {@code else empty}
     * is treated identically to the injected default (both yield the {@code null} form).
     */
    private static boolean hasGenuineElse(RConditionalExpr conditional) {
        return conditional.elseBranch()
                .map(e -> !(e instanceof REmptyLiteral
                        || (e instanceof RListLiteral list && list.elements().isEmpty())))
                .orElse(false);
    }

    /**
     * Lowers a conditional {@code if <condition> then <thenBranch> (else <elseBranch>)?}
     * ({@link RConditionalExpr}) to {@link IRConditional} — the first control-flow {@link IRExpr}, and
     * (step-12) the first arm wired into the shared {@link #adapt(RExpression, NodeId, RWorkspace)}
     * dispatch that touches conditionals (the sibling {@code adapt*Facts} methods above are separate,
     * never-dispatched entry points for the Wave-6 hoist tier; see their javadoc). Recurses all three
     * slots all-or-nothing (the {@code RListLiteral} / {@link #adaptToString} template): the condition
     * takes child slot 0, the then-branch slot 1, and a genuine else-branch (see
     * {@link #hasGenuineElse}) slot 2.
     *
     * <h2>D1 — all-or-nothing recursion</h2>
     * If the condition, the then-branch, or a genuine else-branch does not itself adapt, the whole
     * conditional stays {@link Optional#empty()} — never a half-built node that would disagree with the
     * legacy emitter on surrounding context. The corpus-dominant conditionals whose branch interiors are
     * {@code MISSING}-typed alias/item receivers (L-032) therefore stay adapter-declined rather than
     * fabricating a lowering the emitter side cannot yet honour.
     *
     * <h2>D3 — {@code type} is {@code type(expr, ws)}, the branch join</h2>
     * Resolves via {@code ExpressionTypeComputer.computeConditional}, which joins the then/else branch
     * types. The parser's {@code DefaultElseRule} stamps a synthetic empty-list else (typed
     * {@code NOTHING}, the join identity) on every else-omitting conditional, so a no-else conditional's
     * type equals its then-branch's type exactly.
     *
     * <h2>D4 — {@code cardinality} is the branch JOIN, never {@code card(expr, ws)}</h2>
     * {@code CardinalityComputer.compute} stamps every {@link RConditionalExpr} to {@code SINGLE}
     * unconditionally ("conservative: single") — a degenerate value that ignores list-valued branches
     * and would mislead every cardinality-keyed consumer (the Python absent-value choice, {@code count},
     * future targets). This arm instead computes the neutral-correct fact directly from the ADAPTED
     * branches — {@code MULTI} if either the then-branch or a genuine else-branch is {@code MULTI}, else
     * {@code SINGLE} — mirroring {@link #adaptFeatureCall}'s {@code accumulatedCard}, which likewise
     * favours a directly-computed monotone-multi overlay over the conservative inferred cardinality.
     *
     * <h2>D5 — {@code optionality} is the absorbing rule</h2>
     * {@code OPTIONAL} when there is no genuine else (an absent/false condition then yields an absent
     * value, mirroring {@link IREmptyLiteral}'s {@code OPTIONAL} leaf rule) OR either branch is itself
     * {@code OPTIONAL} (mirroring {@code chainOptionality}'s absorbing-monoid composition);
     * {@code PRESENT} only when a genuine else exists and both branches are {@code PRESENT}. An
     * {@code OPTIONAL} <em>condition</em> does NOT make the result optional — an absent condition
     * deterministically selects the else, which is present when both branches are.
     *
     * <h2>Byte-safety (the #495 oracle-closed consumption superseding the step-12 inertness)</h2>
     * At step-12 this arm was byte-inert by UNREACHABILITY — {@code IRExpressionCompiler} had no
     * {@code visitConditional} claim and {@code IRJavaLeafEmitter} no {@code IRConditional} case (the
     * {@code docs/superpowers/specs/2026-07-01-ir-conditional-step12-design.md} §4 / L-132 argument).
     * Since #495 the Java seam DOES consume it, oracle-closed instead: the compiler claims a ROOT
     * conditional and the leaf emitter renders it WHOLESALE via the range-correlated
     * {@code ConditionalRenderer} — {@code super.visitConditional(site, ctx)}, the literal legacy
     * fallback, byte-identical at every exit by the strongest argument (the #494 construct pattern);
     * a child-position {@code IRConditional} inside another claim's lowered tree still declines at
     * the emitter (the renderer is installed for conditional claim roots only) — the pre-#495 bytes.
     *
     * <h2>The #495 ctor-slot admission (the root-only law's FIRST named exception; the #496
     * lambda body-slot admission is the second; RECURSIVE through glue since #526)</h2>
     * A claim-ROOT conditional's direct slots recurse through {@link #adaptConditionalSlot} —
     * since #526 the {@link #adaptAdmittedSlot} dispatch: a slot that IS a constructor admits
     * via {@link #adaptConstructorShallow} exactly as at #495 (the condVisit probe read 320
     * {@code if <cond> then Type{...}} conditionals blocked SOLELY by the #494 gate), and a
     * glue slot (conditional / extract / filter / list literal) recurses with its OWN slots
     * admitted (the neGate census's 38-claim glue-reachable pool). Root-gated at the chain's
     * head: a chain-external conditional's slots take the plain dispatch, so no other parent
     * lowering can observe the kind and the #494 blast-radius law holds unchanged.
     *
     * @param conditional the AST conditional
     * @param id          this node's {@link NodeId} within its seam
     * @param ws          the linked workspace, for type/cardinality inference
     */
    private Optional<IRExpr> adaptConditional(RConditionalExpr conditional, NodeId id, RWorkspace ws) {
        return adaptConditional(conditional, id, ws, id.path().isEmpty());
    }

    /**
     * The seat-taking overload — {@code admittedSeat} threads the #526 admitted-slot recursion:
     * a conditional REACHED through an unbroken chain of admitted glue seats (see
     * {@link #adaptAdmittedSlot}) treats its own slots as admitted exactly as a claim-root
     * conditional does; every plain dispatch derives the seat from the position
     * ({@code id.path().isEmpty()} — the #495 gate unchanged).
     */
    private Optional<IRExpr> adaptConditional(RConditionalExpr conditional, NodeId id, RWorkspace ws,
            boolean admittedSeat) {
        boolean atClaimRoot = admittedSeat; // the #495 ctor-slot admission's gate (#526: seat-threaded)
        Optional<IRExpr> condition = adaptConditionalSlot(conditional.condition(), id.child(0), atClaimRoot, ws);
        if (condition.isEmpty()) {
            return Optional.empty(); // condition not yet Wave-0-expressible (D1)
        }
        Optional<IRExpr> thenBranch = adaptConditionalSlot(conditional.thenBranch(), id.child(1), atClaimRoot, ws);
        if (thenBranch.isEmpty()) {
            return Optional.empty(); // then-branch not yet Wave-0-expressible (D1)
        }
        boolean genuineElse = hasGenuineElse(conditional);
        IRExpr elseIr = null;
        if (genuineElse) {
            Optional<IRExpr> elseBranch =
                    adaptConditionalSlot(conditional.elseBranch().get(), id.child(2), atClaimRoot, ws);
            if (elseBranch.isEmpty()) {
                return Optional.empty(); // genuine else-branch not yet Wave-0-expressible (D1)
            }
            elseIr = elseBranch.get();
        }
        ExpressionCardinality cardinality = (thenBranch.get().cardinality() == ExpressionCardinality.MULTI
                || (elseIr != null && elseIr.cardinality() == ExpressionCardinality.MULTI))
                ? ExpressionCardinality.MULTI : ExpressionCardinality.SINGLE; // D4
        Optionality optionality = (elseIr == null
                || thenBranch.get().optionality() == Optionality.OPTIONAL
                || elseIr.optionality() == Optionality.OPTIONAL)
                ? Optionality.OPTIONAL : Optionality.PRESENT; // D5 (elseIr == null ⇔ no genuine else)
        return Optional.of(new IRConditional(condition.get(), thenBranch.get(), elseIr, id,
                type(conditional, ws), cardinality, optionality, conditional.sourceRange())); // D3
    }

    /**
     * #495 — one conditional slot's recursion: the shared {@code adapt} dispatch, except that a
     * claim-ROOT conditional's direct slot is an ADMITTED slot (the #492 explicit-position
     * law's FIRST named admission — the #496 {@link #adaptLambdaBodySlot} is the second; the
     * condVisit probe read 320 {@code if <cond> then Type{...}} conditionals — every one an
     * else-less seam claim root — blocked SOLELY by the #494 position gate). Since #526 the
     * admission routes through {@link #adaptAdmittedSlot} — a direct constructor exactly as
     * before, PLUS the recursive glue chains (the neGate census read the whole 38-claim pool
     * 100% glue-reachable). Root-gated ({@code atClaimRoot} — seat-threaded through the
     * admitted recursion): a child-position conditional's slots recurse the plain dispatch, so
     * parent lowerings elsewhere cannot observe the kind and the #494 blast-radius law holds
     * unchanged. The admitted node is render-inert at the Java seam — the conditional claim
     * renders WHOLESALE via the literal legacy fallback, and the ctor slot re-enters the seam
     * through the oracle's own branch compiles, claiming at its OWN root exactly like every
     * nested constructor since #494.
     */
    private Optional<IRExpr> adaptConditionalSlot(RExpression slot, NodeId slotId, boolean atClaimRoot,
            RWorkspace ws) {
        if (atClaimRoot) {
            return adaptAdmittedSlot(slot, slotId, ws);
        }
        return adapt(slot, slotId, ws);
    }

    /**
     * #526 — the ADMITTED-slot dispatch (the root-only law's named admissions, made RECURSIVE
     * through glue): the shared {@code adapt} dispatch, except that an admitted slot — a
     * claim-ROOT conditional's direct slots (#495), a claim-ROOT extract/filter's direct body
     * (#496), a claim-ROOT list literal's elements (#526, the FOURTH named seat) and every slot
     * REACHED through an unbroken chain of those admitted GLUE seats — admits a constructor via
     * {@link #adaptConstructorShallow} at ANY glue depth (the #496 doc's named future lever,
     * landed: the neGate census read the whole 38-claim pool's blocker paths 100% glue —
     * ct/ce/eb/le steps only, ZERO {@code x:} breakers). Glue = conditional slots +
     * extract/filter bodies + list elements — EXACTLY the seats whose claim-root parents render
     * WHOLESALE via their standing kind-gated / oracle-root renderers (IRConditional/IRLambdaOp
     * roots — the literal {@code super.visitConditional} / {@code super.visitExtract} /
     * {@code super.visitFilter}; IRListConstruct roots — the #500 oracle-root serve; every one
     * byte-identical BY IDENTITY), so every admitted interior is render-inert and the bytes are
     * the decline's own. The #494 blast-radius law holds unchanged: a NON-admitted parent's
     * slots take the plain dispatch, and no other parent lowering can observe the kind. The
     * #512 arg-position lambda-body seat deliberately keeps its ctor-ONLY admission
     * ({@code argCtorSlot} — see {@link #adaptLambdaBodySlot}): its reachable set stays exact.
     */
    private Optional<IRExpr> adaptAdmittedSlot(RExpression slot, NodeId slotId, RWorkspace ws) {
        if (slot instanceof RConstructorExpr ctor) {
            return adaptConstructorShallow(ctor, slotId, ws);
        }
        if (slot instanceof RConditionalExpr cond) {
            return adaptConditional(cond, slotId, ws, true);
        }
        if (slot instanceof RExtractExpr extract) {
            return adaptLambdaOp(IRLambdaOp.Op.EXTRACT, extract, extract.argument(), extract.body(),
                    slotId, ws, BodySeat.ADMITTED);
        }
        if (slot instanceof RFilterExpr filter) {
            return adaptLambdaOp(IRLambdaOp.Op.FILTER, filter, filter.argument(), filter.body(),
                    slotId, ws, BodySeat.ADMITTED);
        }
        if (slot instanceof RListLiteral list) {
            return adaptListLiteral(list, slotId, ws, true);
        }
        return adapt(slot, slotId, ws);
    }

    /**
     * The {@link RListLiteral} arm ({@code IRListConstruct} — the standing all-or-nothing
     * element recursion), seat-taking since #526: a claim-ROOT list's elements (and an
     * admitted-slot list's elements — the glue recursion) route through
     * {@link #adaptAdmittedSlot}; a child-position list keeps the plain dispatch element walk
     * byte-identically (the #494 blast-radius law).
     */
    private Optional<IRExpr> adaptListLiteral(RListLiteral list, NodeId id, RWorkspace ws,
            boolean admittedSeat) {
        List<RExpression> sourceElements = list.elements();
        List<IRExpr> elements = new ArrayList<>(sourceElements.size());
        for (int i = 0; i < sourceElements.size(); i++) {
            RExpression sourceElement = sourceElements.get(i);
            Optional<IRExpr> element = admittedSeat
                    ? adaptAdmittedSlot(sourceElement, id.child(i), ws)
                    : adapt(sourceElement, id.child(i), ws);
            if (element.isEmpty()) {
                return Optional.empty(); // an element is not yet Wave-0-expressible
            }
            elements.add(element.get());
        }
        return Optional.of(new IRListConstruct(elements, id,
                type(list, ws), card(list, ws), Optionality.PRESENT, list.sourceRange()));
    }

    /**
     * #496 — the shared lambda-op arm (the monster wave's leg 1): lowers an {@link RExtractExpr}
     * / {@link RFilterExpr} to the DEEP {@link IRLambdaOp} — D1 all-or-nothing over the two
     * children (receiver, then lambda body, the adapter's recursion order), never a half-built
     * lambda. The #496 lambdaVisit probe sized the deep gate BEFORE this arm landed (bodyLower
     * 2,802 of 5,154 = 54.4%, above the wave's armed falsifiability bar; typeMissing ZERO on both
     * families) and read the two one-level position-divergent classes this recursion must name
     * (see {@link #adaptLambdaBodySlot}).
     *
     * <p><strong>The binder.</strong> {@code binderName} carries the single declared closure
     * parameter (or {@code null} for the implicit-or-paramless {@code item} binding — the L-080
     * class); MORE than one declared parameter never lowers (the {@code multiParamBinder} belt —
     * the extract/filter grammar carries at most one; the probe read zero). Body-interior
     * {@code item}/named references bind through the SAME gates they bind through today (the
     * L-080 filter/extract-binder machinery and the head-resolution mirrors walk the RAW AST
     * parent chain — position-independent, so the child-position recursion reads exactly what the
     * probe's root re-adapt read).
     *
     * <p><strong>The #496 body-slot admission (the root-only law's SECOND named exception;
     * RECURSIVE through glue since #526).</strong>
     * A claim-ROOT lambda's body slot is an ADMITTED slot ({@link #adaptAdmittedSlot}) — a
     * DIRECT {@link RConstructorExpr} via {@link #adaptConstructorShallow} exactly as at #495's
     * pattern (the probe read 194 {@code blocked.ctorSlot.body} extract events + 2
     * {@code blocked.condCtorSlot.body}; the former claimed at #496, the latter — the #496-era
     * "named future lever" — landed at #526 with the recursive glue dispatch). Root-gated
     * ({@code id.path().isEmpty()}, or seat-threaded through the admitted recursion): a
     * chain-external lambda's body takes the plain dispatch, so no other parent lowering can
     * observe the kind and the #494 blast-radius law holds unchanged.
     * The admitted node is render-inert at the Java seam (the wholesale oracle render re-enters
     * the seam through the body compile and the ctor claims at its OWN {@code visitConstructor}
     * visit — the #494 nested-ctor law verbatim).
     *
     * <p><strong>Typing is the emitter's gate</strong> (the #491 convention, exactly like
     * {@link #adaptConditional}): the node's cache type is stamped as read, and
     * {@code IRJavaLeafEmitter.emitLambdaOp} declines a MISSING-typed claim. Cardinality and
     * optionality are the neutral facts documented on {@link IRLambdaOp} (EXTRACT: the D4-style
     * join / the absorbing rule; FILTER: the receiver's cardinality / {@code OPTIONAL}
     * unconditionally — a predicate can reject everything), render-inert at the Java target's
     * wholesale oracle render.
     */
    private Optional<IRExpr> adaptLambdaOp(IRLambdaOp.Op op, RExpression expr, RExpression argument,
            RInlineFunction body, NodeId id, RWorkspace ws) {
        return adaptLambdaOp(op, expr, argument, body, id, ws, BodySeat.PLAIN);
    }

    /**
     * The #526 body-seat flavor for {@link #adaptLambdaBodySlot}: {@code PLAIN} = every plain
     * dispatch (the body admits only at a claim-ROOT lambda — the #496 gate);
     * {@code ARG_CTOR_ONLY} = the #512 call-ARG seat (a DIRECT-constructor body admits, nothing
     * wider — the seat's reachable set stays exact); {@code ADMITTED} = the #526 admitted-slot
     * recursion (the body is itself an admitted slot — the full glue dispatch).
     */
    private enum BodySeat {
        PLAIN, ARG_CTOR_ONLY, ADMITTED
    }

    /**
     * The seat-taking overload — {@code seat} carries the #512 call-ARG ctor-slot admission and
     * the #526 admitted-slot recursion (see {@link BodySeat} and {@link #adaptLambdaBodySlot});
     * every plain dispatch passes {@link BodySeat#PLAIN}.
     */
    private Optional<IRExpr> adaptLambdaOp(IRLambdaOp.Op op, RExpression expr, RExpression argument,
            RInlineFunction body, NodeId id, RWorkspace ws, BodySeat seat) {
        if (argument == null || body == null || body.body() == null) {
            return Optional.empty(); // parse-robustness belts (the probe's nullSlot faces — zero in corpus)
        }
        if (body.paramNames().size() > 1) {
            return Optional.empty(); // the multiParamBinder belt — the grammar carries at most one
        }
        Optional<IRExpr> receiver = adapt(argument, id.child(0), ws);
        if (receiver.isEmpty()) {
            return Optional.empty(); // receiver not yet expressible (D1)
        }
        Optional<IRExpr> bodyIr = adaptLambdaBodySlot(body.body(), id, ws, seat);
        if (bodyIr.isEmpty()) {
            return Optional.empty(); // body not yet expressible (D1)
        }
        ExpressionCardinality cardinality = op == IRLambdaOp.Op.FILTER
                ? receiver.get().cardinality()
                : (receiver.get().cardinality() == ExpressionCardinality.MULTI
                        || bodyIr.get().cardinality() == ExpressionCardinality.MULTI
                                ? ExpressionCardinality.MULTI : ExpressionCardinality.SINGLE);
        Optionality optionality = op == IRLambdaOp.Op.FILTER
                ? Optionality.OPTIONAL
                : (receiver.get().optionality() == Optionality.OPTIONAL
                        || bodyIr.get().optionality() == Optionality.OPTIONAL
                                ? Optionality.OPTIONAL : Optionality.PRESENT);
        // isImplicit FIRST: the ImplicitVariableRule derived-state pass injects a SYNTHETIC
        // "item" parameter into every bare implicit lambda — the neutral binder fact is the
        // DECLARED name only (null = the implicit item binding, whatever its synthetic spelling).
        String binderName = body.isImplicit() || body.paramNames().isEmpty()
                ? null : body.paramNames().get(0);
        return Optional.of(new IRLambdaOp(op, receiver.get(), binderName, bodyIr.get(), id,
                type(expr, ws), cardinality, optionality, expr.sourceRange()));
    }

    /**
     * #496 — the lambda body slot's recursion: the shared {@code adapt} dispatch, except that a
     * claim-ROOT lambda's body is an ADMITTED slot (the #495 conditional-slot admission's
     * pattern — the #492 explicit-position law's SECOND named admission; the lambdaVisit probe
     * read 194 direct-ctor extract bodies blocked SOLELY by the #494 position gate). Since #526
     * the root-gated admission routes through {@link #adaptAdmittedSlot} — a direct constructor
     * exactly as before, PLUS the recursive glue chains (the #496-era "wide child-position
     * admission remains a named future lever" note, landed for the root-REACHABLE glue set).
     * Root-gated ({@code id.path().isEmpty()} at the LAMBDA — the body slot is
     * {@code id.child(1)} — or {@link BodySeat#ADMITTED}, the seat-threaded recursion): a
     * child-position lambda's body recurses the plain dispatch, so parent lowerings elsewhere
     * cannot observe the kind and the #494 blast-radius law holds unchanged — except the #512
     * call-ARG seat ({@link BodySeat#ARG_CTOR_ONLY}): an ARG-position lambda's DIRECT-ctor body
     * admits through the shallow build ALONE (deliberately NOT the #526 glue recursion — the
     * seat's reachable set stays exact), because the containing call routes WHOLE through the
     * oracle-root callArgs serve (the #512 apply-arg shape leg — the parent lowering that
     * observes the kind is exactly the one the serve renders by identity; the probe2-cycle
     * catch: the cdm arg:IRLambdaOp faces are `F(xs extract T {…})` spellings whose live
     * child-position read declined while the probe mirror's root read lowered — the live/mirror
     * meter divergence this named seat closes).
     */
    private Optional<IRExpr> adaptLambdaBodySlot(RExpression bodyExpr, NodeId id, RWorkspace ws,
            BodySeat seat) {
        if (id.path().isEmpty() || seat == BodySeat.ADMITTED) {
            return adaptAdmittedSlot(bodyExpr, id.child(1), ws);
        }
        if (seat == BodySeat.ARG_CTOR_ONLY && bodyExpr instanceof RConstructorExpr ctor) {
            return adaptConstructorShallow(ctor, id.child(1), ws);
        }
        return adapt(bodyExpr, id.child(1), ws);
    }

    /**
     * Extract the {@link ThenChainHoist} facts of a {@code then}-chain — the ordered {@code thenArg} hoist keys +
     * declared types. <strong>Dual-use:</strong> the Phase-A offline ANF harness ({@code Wave6AnfSkeletonTest})
     * AND the live Wave-6 Phase-C renderer ({@code IRFunctionExpressionRenderer}, which sources the SET-position
     * {@code thenArg} hoist NAME from the resulting Bind, L-062). Like {@link #adaptConditionalFacts}, it is
     * deliberately a <em>separate</em> entry point, NOT wired into the shared
     * {@link #adapt(RExpression, NodeId, RWorkspace)} dispatch (which has no {@code RThenExpr} arm): so even though
     * Phase C calls it live, byte/§4.2 inertness of the COMPILER path stays <em>structural</em> — the live
     * {@code adapt} dispatch has no path that produces a then-hoist or a {@code Let}, so the §4.2 counters are
     * untouched; the renderer-side hoist driving is gated by the byte gate, not §4.2. (Formerly
     * {@code adaptThenChainForHarness}, before Phase C made it live.)
     *
     * <p><strong>The {@code thenArg}-count rule (dump §5; verified against the live generator
     * {@code FunctionExpressionRenderer.renderThenExtractSet}).</strong> A chain {@code e0 then f1 then … then
     * fN} parses left-associatively, so walking the {@code argument()} spine from the outermost {@code RThenExpr}
     * collects {@code N} {@code RThenExpr} nodes. The generator opens {@code N} {@code thenArg}s: {@code thenArg0}
     * = the chain base (the innermost {@code then}'s {@code argument()}, hoisted even when a trivial atom),
     * {@code thenArg_k} = the k-th {@code then}'s body re-rooted onto {@code thenArg_{k−1}} (k=1..N−1); the
     * <em>outermost</em> {@code then}'s body is the residual consumer, NOT a hoist. So {@code #thenArg = N} (the
     * node count). This is uniform across body kinds — a {@code then <list-primitive>} (e.g. {@code then
     * only-element}) hoists its argument exactly the same way (dump §5.1; the design §5 V6 "no Let binder"
     * hypothesis is corrected by the trace).
     *
     * <p>The branch bodies do not lower at the #219 pin (they are {@code item}/alias-receiver constructs, L-032),
     * so the offline harness validates the hoist <em>structure</em> — the count, the single→bare vs
     * multi→{@code 0..N−1} numbering, the initializer render form — not the body/type bytes (a "C" cross-check,
     * L-057/L-058). Each binder key is positional ({@code id.child(0..N−1)}); the render <em>number</em> is the
     * list position, so any distinct, ordered keys reproduce it. The declared types are
     * {@link RMetaAnnotatedType#MISSING} (the {@code MapperC}/{@code MapperS} wrapper + element type are render
     * decisions the live generator derives via its own oracles).
     *
     * @param outermost the outermost {@link RThenExpr} of the chain (the SET RHS, or a sub-tree of it — e.g. the
     *                  right operand of an {@code or}; the harness finds it by a stop-on-match tree walk)
     * @param id        the chain's {@link NodeId} within its seam (typically {@link NodeId#ROOT})
     * @param ws        the linked workspace (the types are MISSING offline + unused for the live NAME; present for
     *                  symmetry with {@link #adaptConditionalFacts} and a future re-vendor that resolves them)
     */
    public ThenChainHoist adaptThenChainFacts(RThenExpr outermost, NodeId id, RWorkspace ws) {
        // Count the RThenExpr NODES on the argument() spine (NOT the descents — a node count, mirroring the
        // generator's `while (cur instanceof RThenExpr t) { thens.add(0, t); cur = t.argument(); }`): N = the
        // number of `then`s = the number of thenArg hoists.
        int n = 0;
        RExpression cur = outermost;
        while (cur instanceof RThenExpr t) {
            n++;
            cur = t.argument();
        }
        List<NodeId> binderKeys = new ArrayList<>(n);
        List<RMetaAnnotatedType> binderTypes = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            binderKeys.add(id.child(i)); // distinct + ordered; the render number is the list position, not the key
            binderTypes.add(RMetaAnnotatedType.MISSING); // decl type is a "C" cross-check (render-derived; L-057/L-058)
        }
        return new ThenChainHoist(binderKeys, binderTypes);
    }

    /**
     * Extract the {@link BigIntegerHoist} facts of a beyond-{@code long} integer literal — the 4th + last Wave-6
     * hoist family (facet {@code biginteger_literal_hoist}). Like {@link #adaptThenChainFacts} /
     * {@link #adaptBooleanConditionFacts} / {@link #adaptConditionalFacts}, a SEPARATE entry point NOT wired into
     * the shared {@link #adapt(RExpression, NodeId, RWorkspace)} dispatch — so the COMPILER path's byte/§4.2
     * inertness stays <em>structural</em> even though Phase C calls this live. (The live {@code adapt} dispatch
     * already lowers a beyond-{@code long} {@code RIntLiteral} to an {@link IRLiteral} the leaf emitter then
     * DECLINES — an EXISTING §4.2 decline this slice does not change.)
     *
     * <p>Validates the family's defining precondition ({@code value.bitLength() > 63}) so the facts are
     * non-vacuous and the live handler's {@code try}/{@code catch} (R3) guards a real check. {@code ws} is present
     * for symmetry with the sibling {@code *Facts} adapters and is UNUSED: the {@code bigInteger} base lexeme is
     * value-independent, so no type inference is needed (the decl type is always {@code BigInteger}, rendered by
     * the legacy oracle — the L-029 name-driving split).
     *
     * @param expr the beyond-{@code long} integer literal whose hoist NAME the IR drives
     * @param id   the hoist's {@link NodeId} within its seam (typically {@link NodeId#ROOT})
     * @param ws   the linked workspace — unused, present for symmetry (see above)
     * @throws IllegalArgumentException if {@code expr} is not beyond-{@code long} (the family's precondition)
     */
    public BigIntegerHoist adaptBigIntegerFacts(RIntLiteral expr, NodeId id, RWorkspace ws) {
        if (expr.value().bitLength() <= 63) {
            throw new IllegalArgumentException(
                    "adaptBigIntegerFacts requires a beyond-long literal (bitLength > 63): " + expr.value());
        }
        return new BigIntegerHoist(id);
    }

    /**
     * Desugar a {@code then}-chain ({@code e0 then f1 then … then fN}) into a flattened chain of neutral
     * {@link Let} bindings ({@code let _then0 = e0 in (let _then1 = f1[item↦_then0] in … )}) — the
     * <strong>multi-statement function body</strong> a Python/Rust/Morphir target lowers to a statement
     * sequence (design §5 {@code then → Let}). It is the desugar half of the multi-target {@code then}/{@code Let}
     * increment (decision-log L-088); a Python emitter ({@code IRPythonEmitter.emitFunction}) flattens the
     * resulting {@code Let}s into {@code <binder> = <value>} statements + a final {@code return}.
     *
     * <p><strong>A SEPARATE, byte-inert entry point</strong> — NOT wired into the shared
     * {@link #adapt(RExpression, NodeId, RWorkspace)} dispatch (which has no {@code RThenExpr} arm), exactly like
     * {@link #adaptThenChainFacts} / {@link #adaptConditionalFacts}. So the live Java compiler path never
     * produces a {@link Let}, and this method cannot change Java bytes.
     *
     * <p><strong>⚠ This is the FIRST separate entry point that returns a tree-bearing {@link IRExpr} (a real
     * {@link Let}), not a name-only {@code *Hoist} record.</strong> The {@code *Facts} siblings are safe even
     * when wired live (Phase C wired them) because they only drive a render NAME; a {@code Let} drives a whole
     * TREE the Java renderers cannot consume. So this method must <strong>never</strong> be called from
     * {@code rune-java-generator} (it has, and must keep, zero callers there) without re-running the full byte
     * gate and confirming the {@code Let} flattens identically to the legacy {@code thenArg} hoist
     * ({@code com.regnosys.rosetta.ir.expr.anf}, the Java-target lowering). The byte gate is the guard.
     *
     * <p><strong>Scope (all-or-nothing, returns empty otherwise).</strong> Only the <em>implicit single-{@code
     * item}</em> {@code then} lambda is desugared (design §5: the {@code [item↦binder]} substitution is valid
     * only for the implicit case); an explicit-param lambda, an identity pipe (absent body), and any
     * non-lowerable argument/body decline. A then-body lowers via {@link #adapt}, whose {@code item} gates
     * (operand/receiver) admit {@code item} only as a <em>bare leaf</em> — so the binder substitution is a
     * top-level rename, with a guard declining a body where {@code item} survives nested (see
     * {@link #substituteThenBinder}).
     *
     * @param outermost the outermost {@link RThenExpr} of the chain (the seam root or a sub-tree)
     * @param id        the chain's {@link NodeId} within its seam (typically {@link NodeId#ROOT})
     * @param ws        the linked workspace, for the operand lowering
     * @return the flattened {@link Let} chain, or empty if any part is outside the desugar's scope
     */
    public Optional<IRExpr> adaptThenChainToLet(RThenExpr outermost, NodeId id, RWorkspace ws) {
        return adaptThen(outermost, id, spineLength(outermost) - 1, ws);
    }

    /** The number of {@link RThenExpr} nodes on the left-associative {@code argument()} spine of a chain. */
    private static int spineLength(RThenExpr outermost) {
        int n = 0;
        RExpression cur = outermost;
        while (cur instanceof RThenExpr t) {
            n++;
            cur = t.argument();
        }
        return n;
    }

    /**
     * Desugar one {@code then} node into a {@link Let}, recursing on a nested {@code then} argument. {@code index}
     * is the binder's position in the chain — innermost {@code _then0}, increasing outward — so the
     * {@code argument()} spine walk assigns {@code _then0} to the chain base's pipe and the outermost {@code then}
     * the highest index.
     *
     * <p>The binder name {@code _then<index>} is a fresh, target-agnostic identifier: its only neutral guarantee
     * is freshness / non-collision (a Rune parameter cannot be named {@code item} either). A Python emitter prints
     * it verbatim; the Java target discards it and keys its own {@code thenArg} temp by {@link NodeId} (the L-029
     * split — {@code Let.binder()} is never read on the Java path), so any target may re-case or re-prefix it. The
     * positional {@code _then<index>} (vs design §5:360's {@code fresh(thenNodeId)}) is a deliberate choice — both
     * are deterministic + target-agnostic.
     */
    private Optional<IRExpr> adaptThen(RThenExpr then, NodeId id, int index, RWorkspace ws) {
        RInlineFunction body = then.body().orElse(null);
        if (body == null || !body.isImplicit() || !body.paramNames().isEmpty()) {
            return Optional.empty(); // identity pipe / explicit-param lambda — defer (implicit single-item only)
        }
        RExpression arg = then.argument();
        Optional<IRExpr> value = (arg instanceof RThenExpr inner)
                ? adaptThen(inner, id.child(0), index - 1, ws)   // a nested then — recurse the chain
                : adapt(arg, id.child(0), ws);                   // the chain base
        Optional<IRExpr> in = adapt(body.body(), id.child(1), ws);
        if (value.isEmpty() || in.isEmpty()) {
            return Optional.empty(); // arg/body not lowerable (item-transforming, alias, call, nested lambda…)
        }
        IRExpr substituted = substituteThenBinder(in.get(), "_then" + index);
        if (substituted == null) {
            return Optional.empty(); // a nested item survives (count/listop/tostring/[item]) — out of slice
        }
        // The then's result IS the continuation `in`, so its type/cardinality/optionality are `in`'s. Derive them
        // from the already-lowered `in` — NOT re-inference on the un-adapted RThenExpr (no `adapt` arm ⇒ no
        // memoized type). The emitter reads only binder/value/in, so these are not load-bearing for emission.
        return Optional.of(new Let("_then" + index, value.get(), substituted, id,
                in.get().type(), in.get().cardinality(), in.get().optionality(), then.sourceRange()));
    }

    /**
     * Substitute the {@code then}-bound implicit {@code item} for the {@link Let} binder
     * ({@link IRVariable.VariableKind#LET_BINDER}) in a lowered then-body — the {@code [item↦binder]} of design
     * §5:360. Returns the rewritten body, or {@code null} when the body is OUTSIDE this slice (⇒ the {@code then}
     * declines).
     *
     * <p><strong>Why a top-level rename + a decline guard, not a recursive rewrite.</strong> The live
     * {@link #adapt} dispatch admits {@code item} as an arithmetic/comparison/equality OPERAND (never — see
     * {@link #isArithmeticOperand}) and as a navigation RECEIVER only when filter/extract-bound (never for a
     * {@code then}-bound {@code item} — see {@link #isFilterOrExtractBound}); a <em>bare</em> {@code item} lowers
     * unconditionally to a {@link IRVariable.VariableKind#USER_ITEM}. So a lowered then-body is the bare
     * {@code item} (renamed here), OR item-free (returned as-is), OR a postfix form ({@code count} / a flat
     * list-op / {@code to-string} / a {@code [item]} list literal) that recurses {@link #adapt} on a child and
     * thus carries a <em>nested</em> {@code item}. The last class is declined ({@code null}) via the read-only
     * {@link #containsUserItem} walk: it is SOUND (no unrenamed {@code item} ever reaches an emitter) and
     * LOSSLESS for a Python target (those body kinds are exactly the ones a Python emitter cannot yet lower, so
     * they would decline at emission anyway).
     *
     * <p>A future slice admitting an item-<em>transforming</em> then-body (the Python analog of {@code thenArg})
     * must replace this with a capture-free recursive rename; the no-nested-implicit-lambda invariant (lambda-
     * bodied ops do not lower via {@link #adapt}) keeps that walk capture-free (design §5:365-367, V4).
     */
    private static IRExpr substituteThenBinder(IRExpr in, String binder) {
        if (in instanceof IRVariable v && v.variableKind() == IRVariable.VariableKind.USER_ITEM) {
            return new IRVariable(binder, IRVariable.VariableKind.LET_BINDER, v.nodeId(),
                    v.type(), v.cardinality(), v.optionality(), v.sourceRange()); // bare item → the binder
        }
        return containsUserItem(in) ? null : in; // nested item → decline (out of slice); item-free → as-is
    }

    /** Whether any {@link IRVariable.VariableKind#USER_ITEM} appears anywhere in the tree (read-only, no rebuild). */
    private static boolean containsUserItem(IRExpr node) {
        if (node instanceof IRVariable v && v.variableKind() == IRVariable.VariableKind.USER_ITEM) {
            return true;
        }
        for (IRExpr child : node.children()) {
            if (containsUserItem(child)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Lowers a <em>flat</em> postfix list operation ({@code distinct}/{@code flatten}/{@code first}/
     * {@code last}/{@code reverse}/{@code only-element} — the last since the #498 teach —
     * plus {@code sum} since #519) to an
     * {@link IRListOp} — but ONLY when its receiver subtree itself
     * lowers through the full {@link #adapt} dispatch (params, navigations, and — as the
     * #499 cut-2 census read live — calls, aliases and items too; a receiver outside every
     * taught arm returns empty here, so the whole node declines and stays on the legacy
     * handler). The hoisting / lambda-bodied ops are NOT admitted (they fall through
     * to the empty return → legacy). The receiver is the {@link IRListOp}'s single load-bearing child;
     * the flat Java wrap (method name + chain link + the {@code distinct} import) is a Java-emission
     * decision the emitter reproduces by reusing the legacy {@code CollectionHandler} oracle (L-049),
     * so it is NOT carried on the neutral node.
     *
     * <p>#519: {@code sum} joins the taught set (the opSum face — 5/8/3/7 sole across the four
     * probed cells at the #518 SOT, the RListOpExpr family WHOLE). Its "deeper-diff render"
     * (the element-type-derived method legacy composes inside its own line) is exactly why the
     * kind gets NO native emitter arm: a top-level sum root serves through the standing
     * {@code CollectionOpRenderer} slot (the verbatim {@code CollectionHandler.handle} call —
     * the RAW-class dispatch is kind-agnostic, proven at the #499 collapse teach) and an
     * interior sum is an ORACLE LEAF ({@code containsOracleLeaf}'s SUM-scoped leg), so every
     * containing root renders whole-legacy — byte-identical BY IDENTITY on both routes. The
     * dominant live receiver is the parser-synthesized implicit ({@code … then sum} —
     * {@code syntheticImplicitInput}), which lowers unconditionally to a
     * {@code SYNTHETIC_ITEM} variable at the {@link RImplicitVariable} arm.
     */
    private Optional<IRExpr> adaptListOp(RListOpExpr expr, NodeId id, RWorkspace ws) {
        IRListOp.Kind op = switch (expr.op()) {
            case DISTINCT -> IRListOp.Kind.DISTINCT;
            case FLATTEN -> IRListOp.Kind.FLATTEN;
            case FIRST -> IRListOp.Kind.FIRST;
            case LAST -> IRListOp.Kind.LAST;
            case REVERSE -> IRListOp.Kind.REVERSE;
            // #498: only-element joins the taught set — the render stays the CollectionHandler
            // oracle verbatim (the same call the legacy fallback makes), so the inline `.get()`
            // collapse and its selfUnwrapping consumer-marker ride the oracle's own
            // JavaExpression unchanged; the #498 census read the whole gate population typed
            // with universally-lowering receivers.
            case ONLY_ELEMENT -> IRListOp.Kind.ONLY_ELEMENT;
            // #519: sum joins the taught set — oracle-served on every route (method javadoc).
            case SUM -> IRListOp.Kind.SUM;
        };
        Optional<IRExpr> child = adapt(expr.argument(), id.child(0), ws);
        if (child.isEmpty()) {
            return Optional.empty(); // receiver not yet IR-expressible (alias/item/call — L-032) → legacy
        }
        return Optional.of(new IRListOp(op, child.get(), id,
                type(expr, ws), card(expr, ws), Optionality.PRESENT, expr.sourceRange()));
    }

    /**
     * Lowers a {@code count} ({@link RCountExpr}) to an {@link IRListOp} of {@link IRListOp.Kind#COUNT}
     * ({@code <child>.resultCount()}), gated on the receiver subtree lowering exactly like
     * {@link #adaptListOp}. {@code count} is structurally the flat postfix sibling of the list ops (it
     * just lives on a distinct AST node), so it shares the node + the verbatim-oracle render.
     */
    private Optional<IRExpr> adaptCount(RCountExpr expr, NodeId id, RWorkspace ws) {
        Optional<IRExpr> child = adapt(expr.argument(), id.child(0), ws);
        if (child.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new IRListOp(IRListOp.Kind.COUNT, child.get(), id,
                type(expr, ws), card(expr, ws), Optionality.PRESENT, expr.sourceRange()));
    }

    /**
     * Lowers a {@code to-string} conversion ({@link RToStringExpr}) to {@link IRToString}, gated on the receiver
     * subtree lowering exactly like {@link #adaptListOp} / {@link #adaptCount} (the L-050 child-lowering /
     * neutrality discipline — the node is genuinely IR-modelled, and lowerable to a 2nd target, only when its
     * {@code child} is; an alias / implicit {@code item} / call receiver does not lower, L-032, so
     * the whole node declines and stays on the legacy handler). The Java render reuses the legacy
     * {@code ConversionHandler.handle(RToStringExpr)} oracle verbatim (the {@code "to-string"} lexeme, the
     * source-enum {@code ::toDisplayString} vs {@code Object::toString} selection, the {@code coerceNavigationReceiver}
     * meta-unwrap and the source-enum import are Java-emission decisions kept off this neutral node — the L-029/L-050
     * split), so it is byte-identical to Path-1 by construction; this node carries only the neutral structure + the
     * firing gate, for a future Python/Rust emitter.
     */
    private Optional<IRExpr> adaptToString(RToStringExpr expr, NodeId id, RWorkspace ws) {
        Optional<IRExpr> child = adapt(expr.argument(), id.child(0), ws);
        if (child.isEmpty()) {
            return Optional.empty(); // receiver not yet IR-expressible (alias/item/call — L-032) → legacy
        }
        // to-string is a pure optionality-preserving map (absent in → absent out), so it inherits the child's
        // optionality (no feature hop to compose, unlike navigation's chainOptionality).
        return Optional.of(new IRToString(child.get(), id,
                type(expr, ws), card(expr, ws), child.get().optionality(), expr.sourceRange()));
    }

    /**
     * The #500 arm-B conversion teach: lowers a typed {@code RConversionExpr} — {@code to-enum} /
     * {@code to-number} / {@code to-int} / the temporal kinds ({@code to-string} is the separate
     * {@link RToStringExpr} family, {@link #adaptToString}) — to the DEEP {@link IRConversion}
     * whose single child is the lowered argument (all-or-nothing, the {@link IRToString}
     * template; the #500 census read the argument lowering in 948 of 956 events — 946 of the
     * 954 to-enum plus both to-int). Kind-blind
     * across the seven {@code ConversionKind} constants (the render is the identity oracle —
     * {@code super.visitConversion} re-walks the raw node, so the kind never conditions the
     * byte surface; the census read {@code toEnum} 954 + {@code toInt} 2 live). The
     * {@code to-enum} target's name rides as a neutral fact (the resolved enumeration's name,
     * falling back to the raw target token for an unresolved target — zero live at the census,
     * kept for parse-robustness). Optionality inherits the child's (the conversion is an
     * optionality-preserving map, the {@link #adaptToString} argument).
     */
    private Optional<IRExpr> adaptConversion(RConversionExpr conv, NodeId id, RWorkspace ws) {
        RExpression arg = conv.argument();
        if (arg == null || conv.kind() == null) {
            return Optional.empty(); // parse-robustness: a kind-less/argument-less node has no render
        }
        Optional<IRExpr> child = adapt(arg, id.child(0), ws);
        if (child.isEmpty()) {
            return Optional.empty(); // argument not lowerable — the census's ~10-event residue
        }
        String target = conv.kind() == ConversionKind.ENUM
                ? conv.targetEnum().map(REnumeration::name)
                        .orElse(conv.targetEnumName().orElse(null))
                : null;
        return Optional.of(new IRConversion(child.get(), conv.kind().name(), target, id,
                type(conv, ws), card(conv, ws), child.get().optionality(), conv.sourceRange()));
    }

    /**
     * The #500 arm-B pipe teach: lowers a {@code then}-chain root ({@link RThenExpr}) to the
     * deliberately-SHALLOW {@link IRPipe} (the #494 {@link IRConstruct} pattern — no IR children,
     * so every child-position admission allow-list stays byte-frozen and the node claims at its
     * visit ROOT only; nested chain links render inside the oracle's own walk). Census-narrow on
     * the binder form: the #500 census read the family's 582 events 100% implicit-bare, so an
     * explicit-param or identity (body-less) pipe declines — the drift face, zero live events.
     * The spine length (the {@code thenArg} hoist count) rides as the neutral fact. The DEEP
     * {@code Let} desugar stays the separate Python-side {@link #adaptThenChainToLet} entry
     * point, untouched and Java-inert per its own ⚠ contract — this arm does not call it.
     */
    private Optional<IRExpr> adaptPipe(RThenExpr then, NodeId id, RWorkspace ws) {
        if (then.argument() == null) {
            return Optional.empty(); // parse-robustness: an argument-less pipe has no render (zero live)
        }
        RInlineFunction body = then.body().orElse(null);
        if (body == null || !body.isImplicit()) {
            return Optional.empty(); // census-narrow: implicit-bare binders only (the only live form)
        }
        int spine = 1;
        RExpression cur = then.argument();
        while (cur instanceof RThenExpr t) {
            spine++;
            cur = t.argument();
        }
        return Optional.of(new IRPipe(spine, id,
                type(then, ws), card(then, ws), Optionality.PRESENT, then.sourceRange()));
    }

    /**
     * The #500 arm-B only-exists teach: lowers an {@code only exists} check root
     * ({@link ROnlyExistsExpr}) to the deliberately-SHALLOW {@link IROnlyExists} (the #494
     * {@link IRConstruct} pattern): the path elements are heterogeneous
     * {@link ROnlyExistsElement} records (root name / implicit-item flag / feature chain /
     * receiver expression — the #500 census read 197 of 200 receiver-expression rooted), a
     * composite the deep wave models with its own element node. The path count rides as the
     * neutral fact; a path-less node declines (parse-robustness, zero live events).
     */
    private Optional<IRExpr> adaptOnlyExists(ROnlyExistsExpr onlyExists, NodeId id, RWorkspace ws) {
        if (onlyExists.elements().isEmpty()) {
            return Optional.empty(); // parse-robustness: a path-less check has no render
        }
        return Optional.of(new IROnlyExists(onlyExists.elements().size(), id,
                type(onlyExists, ws), card(onlyExists, ws), Optionality.PRESENT,
                onlyExists.sourceRange()));
    }

    /**
     * The #513 noAdaptArm-sweep switch teach: lowers a {@code switch} expression root
     * ({@link RSwitchExpr}) to the deliberately-SHALLOW {@link IRSwitchOp} (the #500
     * {@link IRPipe} family-arm pattern — no IR children; the argument, the non-expression
     * case guards and the case results all render inside the oracle's own re-walk, where
     * every interior node claims or declines at its OWN seat, the #504 re-entrant law). The
     * case count and default-case presence ride as the neutral facts; a case-less or
     * result-less node declines (parse-robustness, zero live events — the corpus parses
     * diagnostic-clean).
     */
    private Optional<IRExpr> adaptSwitchOp(RSwitchExpr sw, NodeId id, RWorkspace ws) {
        // #513 probe5: the ARGUMENT-null form is ACCEPTED — the grammar's without-left
        // alternative (SwitchWithoutLeftExpr; switch is NOT among the ten elided-operand
        // synthesis slots, so a piped `… then switch …` keeps a null argument by parse —
        // probe4 read the class at 11 events, 10 cdm6 + 1 drr RULE). The shallow mint
        // reads no argument fact and the render is the whole-legacy oracle serve either
        // way (legacy's own withoutLeft machinery inside super.visitSwitch — byte-identical
        // BY IDENTITY), so only the case gates hold.
        if (sw.cases().isEmpty()) {
            return Optional.empty(); // parse-robustness: a case-less switch has no render
        }
        boolean hasDefault = false;
        for (RSwitchCase switchCase : sw.cases()) {
            if (switchCase.expression() == null) {
                return Optional.empty(); // parse-robustness: a result-less case has no render
            }
            hasDefault |= switchCase.isDefault();
        }
        return Optional.of(new IRSwitchOp(sw.cases().size(), hasDefault, id,
                type(sw, ws), card(sw, ws), Optionality.PRESENT, sw.sourceRange()));
    }

    /**
     * The #513 noAdaptArm-sweep default teach: lowers a {@code default} fallback
     * ({@link RDefaultExpr}) to the deliberately-SHALLOW {@link IRDefaultOp} (the #500
     * {@link IRPipe} family-arm pattern — no facts beyond the shared tail, no IR children;
     * legacy's null-safe default composition renders inside the oracle's own re-walk). A
     * one-sided node declines (parse-robustness, zero live events).
     */
    private Optional<IRExpr> adaptDefaultOp(RDefaultExpr def, NodeId id, RWorkspace ws) {
        // #513 probe5: the LEFT-null form is ACCEPTED — the grammar's without-left
        // alternative (DefaultWithoutLeftExpr; default is NOT among the ten
        // elided-operand synthesis slots, so a piped `… then default …` keeps a null
        // left by parse — probe4 read the class at 6 cdm6 events). The render is the
        // whole-legacy oracle serve either way; the fallback side stays required.
        if (def.rawRight() == null) {
            return Optional.empty(); // parse-robustness: a fallback-less default has no render
        }
        return Optional.of(new IRDefaultOp(id, type(def, ws), card(def, ws),
                Optionality.PRESENT, def.sourceRange()));
    }

    /**
     * The #513 noAdaptArm-sweep membership teach: lowers a {@code contains}/{@code disjoint}
     * test ({@link RContainsExpr}/{@link RDisjointExpr}) to the deliberately-SHALLOW
     * {@link IRMembershipOp} (the #500 {@link IRPipe} family-arm pattern — the operator is
     * the neutral fact, no IR children; legacy's {@code contains(...)}/{@code disjoint(...)}
     * runtime composition renders inside the oracle's own re-walk). A one-sided node
     * declines (parse-robustness, zero live events).
     */
    private Optional<IRExpr> adaptMembershipOp(IRMembershipOp.Op op, RBinaryExpression test,
            NodeId id, RWorkspace ws) {
        if (test.rawLeft() == null || test.rawRight() == null) {
            return Optional.empty(); // parse-robustness: a one-sided membership test has no render
        }
        return Optional.of(new IRMembershipOp(op, id, type(test, ws), card(test, ws),
                Optionality.PRESENT, test.sourceRange()));
    }

    /**
     * The #513 noAdaptArm-sweep collect teach: lowers a {@code max}/{@code min}/{@code sort}
     * postfix op ({@link RMaxExpr}/{@link RMinExpr}/{@link RSortExpr}) to the
     * deliberately-SHALLOW {@link IRCollectOp} (the #500 {@link IRPipe} family-arm pattern —
     * the operator and key-body presence are the neutral facts, no IR children; legacy's
     * {@code CollectionHandler} comparator composition renders inside the oracle's own
     * re-walk, the key body's interior nodes claiming or declining at their own seats). An
     * argument-less node declines (parse-robustness, zero live events).
     */
    private Optional<IRExpr> adaptCollectOp(IRCollectOp.Op op, RExpression expr,
            RExpression argument, RInlineFunction body, NodeId id, RWorkspace ws) {
        if (argument == null) {
            return Optional.empty(); // parse-robustness: an argument-less collect has no render
        }
        return Optional.of(new IRCollectOp(op, body != null, id, type(expr, ws),
                card(expr, ws), Optionality.PRESENT, expr.sourceRange()));
    }

    /**
     * The #515 untargeted-close with-meta teach: lowers a {@code with-meta} annotation
     * expression ({@link RWithMetaExpr}) to the deliberately-SHALLOW {@link IRWithMetaOp}
     * (the #500 {@link IRPipe} family-arm pattern — the entry count is the neutral fact, no
     * IR children; legacy's FieldWithMetaX builder threading and scheme/reference/id entry
     * setters render inside the oracle's own re-walk, every interior claiming or declining
     * at its OWN seat, the #504 re-entrant law). UNCONDITIONAL: the grammar's
     * {@code WithMetaWithoutLeftExpr} alternative keeps a null argument by parse (the #513
     * without-left law — accepted, legacy's own withoutLeft machinery inside
     * {@code super.visitWithMeta} is the serve either way) and the entry braces are
     * optional by grammar, so an entry-less node (count 0) is a valid parse with a render.
     */
    private Optional<IRExpr> adaptWithMetaOp(RWithMetaExpr withMeta, NodeId id, RWorkspace ws) {
        return Optional.of(new IRWithMetaOp(withMeta.entries().size(), id,
                type(withMeta, ws), card(withMeta, ws), Optionality.PRESENT,
                withMeta.sourceRange()));
    }

    /**
     * The #515 untargeted-close join teach: lowers a {@code join} concatenation
     * ({@link RJoinExpr}) to the deliberately-SHALLOW {@link IRJoinOp} (the #500
     * {@link IRPipe} family-arm pattern — the separator's presence is the neutral fact, no
     * IR children; legacy's {@code join(...)} runtime composition renders inside the
     * oracle's own re-walk). The separator lives in its OWN {@code separator()} slot —
     * never {@code rawRight}, the #513 Copilot R1 catch (the {@code rawRight} slot of the
     * binary shell is never populated for join). UNCONDITIONAL: the grammar's
     * {@code JoinWithoutLeftExpr} alternative keeps a null left by parse (the #513
     * without-left law — accepted) and the separator is optional by grammar.
     */
    private Optional<IRExpr> adaptJoinOp(RJoinExpr join, NodeId id, RWorkspace ws) {
        return Optional.of(new IRJoinOp(join.separator().isPresent(), id,
                type(join, ws), card(join, ws), Optionality.PRESENT, join.sourceRange()));
    }

    /**
     * Lowers the <em>bare function-parameter</em> case of {@link RSymbolReference} to an
     * {@link IRVariable} of kind {@link IRVariable.VariableKind#PARAM} — {@code SINGLE} (→
     * {@code MapperS.of(name)}) or {@code MULTI} (→ the witnessed {@code MapperC.<Item>of(name)}) —
     * and returns {@link Optional#empty()} (→ legacy fallback) for every other shape. The strangler
     * net keeps those on the proven legacy {@code ReferenceHandler}:
     * <ul>
     *   <li>arguments present — a function call, routed to {@link #adaptApply} (the slice-1 plain-{@code RFunction}
     *       scalar-arg case lowers to {@link IRApply}; every other call shape defers there);</li>
     *   <li>the symbol is unresolved or not an {@link RAttribute} — an alias, enum value,
     *       bare function/rule reference, or implicit-item navigation;</li>
     *   <li>the attribute is not a plain-{@code func} input parameter — e.g. a data-type
     *       field (parent is an {@code RDataType}), a function output, or a rule/report
     *       synthetic input ({@link RFunction.Origin} {@code RULE}/{@code REPORT});</li>
     *   <li>the name collides with an enclosing shortcut — legacy renders it as an alias
     *       invocation via {@code isAliasReference}'s name-match fallback;</li>
     *   <li>the parameter carries a {@code [metadata …]} annotation — with-meta lowering
     *       arrives with a later increment.</li>
     * </ul>
     *
     * <p>A MULTI parameter is lowered <em>unconditionally</em> (modulo the gates above): the rare
     * {@code MapperC.<Item>of} witness/output simple-name collision (whose legacy FQN-inline render is a
     * Java-name decision the neutral adapter cannot make) is resolved in the emitter against the enclosing
     * function's output Java type (L-029), not deferred here.
     *
     * <p>This mirrors the legacy bare-variable arm: it emits {@code MapperS.of(name)} when
     * {@code tryMultiValueWrap} declines ({@code !gm.isMulti(attr)}) and the witnessed
     * {@code MapperC.<Item>of(name)} when it claims (a non-meta multi parameter). The
     * {@link IRVariable}'s cardinality is taken from the DECLARED cardinality (the same
     * {@code gm.isMulti} signal — reliable for a bare reference, whose expression cardinality IS the
     * variable's). A too-strict gate is harmless (the reference stays on legacy, byte-green); only a
     * too-loose gate could diverge, and that is caught by the FUNCTION byte gate.
     */
    private Optional<IRExpr> adaptSymbolReference(RSymbolReference ref, NodeId id, RWorkspace ws) {
        if (!ref.args().isEmpty()) {
            return adaptApply(ref, id, ws); // arguments present — a function call
        }
        RNode symbol = ref.symbol().orElse(null);
        if (symbol instanceof RShortcut) {
            // Alias/shortcut reference (legacy isAliasReference arm 1): the symbol resolved to an
            // enclosing-function shortcut — a named local sub-expression. Lower to a neutral
            // IRReference{ALIAS} carrying the alias name. The Java render `aliasName(enclosingInputs)`
            // — the input threading AND the dependency-collision name disambiguation — is a JAVA-emission
            // concern, computed in the compiler/emitter against the enclosing function (the L-029 split
            // that keeps Java-name decisions out of the language-neutral adapter), NOT here. args-present
            // was already declined above (legacy throws for an alias carrying arguments). The name-match
            // fallback (an UNRESOLVED symbol whose name matches an enclosing shortcut) stays on legacy for
            // now — a partial-resolution edge case, deferred.
            // #503 arm-A3 — the alias TYPE sourced from the BODY's cached type when the node's
            // own reads MISSING (the L-032 gap: shortcut type inference is unimplemented
            // upstream, so a bound alias reference's node type never resolves; the BODY sits
            // in the inference fixed point). The body channel is the SAME read the #492
            // alias-nav retype already trusts, and it is what makes the emitter's
            // enumOperandRequalifies comparable on the alias side (the census read EVERY
            // enumSibling row's body cached-typed REnumTypeRef:<Enum> — the qualifier lever).
            RMetaAnnotatedType aliasType = type(ref, ws);
            if ((aliasType == null || aliasType.isMissing()) && ws != null
                    && ((RShortcut) symbol).expression() != null) {
                aliasType = ws.getInferredType(((RShortcut) symbol).expression());
                // #518 arm-B1: the SYMBOL-channel fallback (the #503 sourcing one channel
                // deeper) — an RSymbolReference-shaped body the expression channel leaves
                // untyped types from its referenced symbol's own channel (the same walk the
                // aliasOperandBodyFacet admission reads — the #489 one-walk law). Render-
                // invisible by the standing contracts: emitAlias stamps a null expression
                // type and the arithmetic witness classifies through the legacy resolver,
                // so a newly-typed node changes no compose; only the admission oracle and
                // the emitter's enumOperandRequalifies read it.
                if (aliasType == null || aliasType.isMissing()) {
                    aliasType = symbolChannelType(((RShortcut) symbol).expression(),
                            enclosingFunction(ref), ws);
                }
            }
            return Optional.of(new IRReference(ref.name(), IRReference.ReferenceKind.ALIAS, id,
                    aliasType, card(ref, ws), Optionality.PRESENT, ref.sourceRange()));
        }
        if (symbol instanceof REnumValue ev && ev.parent() instanceof REnumeration) {
            // Bare enum-value reference (legacy arm 2: a Cat-13 type-directed bare symbol
            // resolved to an enum value) → EnumName.CONSTANT. Defer only the alias-collision
            // case: if the name matches an enclosing shortcut, legacy's isAliasReference
            // name-match fallback renders it as an alias invocation, not the enum constant.
            RFunction enclosing = enclosingFunction(ref);
            if (enclosing != null && collidesWithShortcut(enclosing, ref.name())) {
                return Optional.empty();
            }
            return Optional.of(new IRReference(ev.name(), IRReference.ReferenceKind.ENUM_VALUE, id,
                    type(ref, ws), card(ref, ws), Optionality.PRESENT, ref.sourceRange()));
        }
        if (symbol instanceof RRule rule) {
            // Bare rule delegation: a no-arg reference to a reporting/eligibility RRule — the
            // {@code output = SomeRule} (top-level) or {@code … then SomeRule} (in-lambda) shape that invokes
            // the rule on the enclosing rule's implicit input. Lower to an argument-less {@link IRApply} whose
            // callee is an {@code IRReference{RULE}} carrying the rule's simple name (purely structural — the
            // neutral fact is "invoke this rule on the implicit input"). The CONTEXT gate (which binding the
            // implicit input takes — the rule `input` at top level, or the lambda `item.get()`/`thenArg.get()`
            // inside an extract/filter/then body) and the {@code <Name>Rule} receiver name are Java-emission
            // decisions kept off the neutral IR (the L-029 split): the compiler reuses legacy's own parent-walk
            // oracles to decline the non-driving shapes (a rule ref in a plain function body, a from-type-less
            // top-level ref) and to RENDER the rest — the top-level `input` form via a {@code RuleReceiverResolver}
            // (L-045), the in-lambda form by reusing legacy {@code renderImplicitRuleInvocation} verbatim via a
            // {@code RuleDelegationRenderer} (L-049). args() is empty on THIS arm (the bare-delegation shape
            // — the dispatch routes args-present references to {@link #adaptApply} before the bare arms run);
            // the args-present rule invocation ({@code <Rule>(<arg>)} — live in DRR as the wrapper factories'
            // synthesized per-field calls and the source-level cross-namespace delegations, the #498 census)
            // lowers on adaptApply's #498 rule leg to an args-carrying {@code IRApply{RULE}}.
            return Optional.of(new IRApply(
                    new IRReference(rule.name(), IRReference.ReferenceKind.RULE, id.child(0),
                            type(ref, ws), card(ref, ws), Optionality.PRESENT, ref.sourceRange()),
                    List.of(), id,
                    type(ref, ws), card(ref, ws), Optionality.PRESENT, ref.sourceRange()));
        }
        if (symbol instanceof RFunction pointFreeCallee) {
            // #492 — the point-free teach (the L-109 deferral RESOLVED): the bare no-arg FUNCTION
            // reference lowers to its OWN kind, IRPointFreeApply — deliberately NOT an
            // empty-args IRApply, so the four IRApply-keyed admission gates stay byte-frozen and
            // each position admits EXPLICITLY (#492: the call-argument + nav-receiver positions —
            // both render through the range-correlated PointFreeRenderer reusing legacy
            // renderImplicitFunctionInvocation verbatim, the same public oracle the top-level
            // point-free claim already reuses). The aliasCollision face declines (legacy
            // isAliasReference wins over the bare-function arm, rendering an ALIAS CALL).
            return adaptPointFree(ref, pointFreeCallee, id, ws);
        }
        if (!(symbol instanceof RAttribute attr)) {
            // #502 arm-1 — the ALIAS NAME-MATCH fallback (the alias arm's own documented
            // deferred edge, RESOLVED): legacy isAliasReference renders ANY reference whose
            // name matches an enclosing function's shortcut as the alias invocation
            // `aliasName(inputs)` — resolution-free (the resolver leaves these symbol-EMPTY on
            // the re-entrant synthesis path; the #502 census read the face 532 node-unit, 100%
            // lowers:IRReference on the linker-bound equivalent, 100% bodyTyped). Mirror the
            // EXACT legacy predicate: the name-match EXCLUDES the reference's own enclosing
            // shortcut (the #453 self-scope law — a self-name is the shadowed item feature,
            // never a self-call; the coarse collidesWithShortcut form used by the DECLINE gates
            // is deliberately NOT used here, because a claiming arm must not claim the
            // self-name slice). Precedence: FIRST among the non-attribute arms, mirroring
            // legacy handle()'s isAliasReference-first dispatch — a closure-param or
            // inline-fn-bound name that ALSO matches a shortcut renders as the alias. The
            // only-exists subtree is excluded (legacy renders only-exists navigations from the
            // element's own strings — those synthesized receivers never render through the
            // reference channel; the #486 facet ladder's own precedence). Lowers to the SAME
            // IRReference{ALIAS} the linker-bound arm mints — the render seam is the standing
            // L-031 emitAlias + AliasOperandResolver machinery, which is NAME-keyed against
            // the enclosing function and therefore resolution-free by construction.
            if (!hasOnlyExistsElementAncestor(ref)) {
                RFunction aliasEnclosing = aliasMatchEnclosingFunction(ref);
                if (aliasEnclosing != null && ref.name() != null) {
                    RShortcut matched = shortcutByName(aliasEnclosing, ref.name());
                    if (matched != null && matched != enclosingShortcut(ref)) {
                        // #503 arm-A3: the same BODY-channel type sourcing as the bound arm
                        // (the synthetic node's own type is ALWAYS missing — cache-invisible).
                        RMetaAnnotatedType nameMatchType = type(ref, ws);
                        if ((nameMatchType == null || nameMatchType.isMissing()) && ws != null
                                && matched.expression() != null) {
                            nameMatchType = ws.getInferredType(matched.expression());
                            // #518 arm-B1: the SYMBOL-channel fallback — the bound arm's
                            // sourcing restated (the one-walk law; see the bound arm's note).
                            if (nameMatchType == null || nameMatchType.isMissing()) {
                                nameMatchType = symbolChannelType(matched.expression(),
                                        aliasEnclosing, ws);
                            }
                        }
                        return Optional.of(new IRReference(ref.name(),
                                IRReference.ReferenceKind.ALIAS, id,
                                nameMatchType, card(ref, ws), Optionality.PRESENT,
                                ref.sourceRange()));
                    }
                }
                // #502 arm-2 — the CLOSURE-PARAM family, both census flavors: the linker-BOUND
                // named-param reference (the symbol is the parameter's own RClosureParameter node
                // since v3.2 seat 8, or the declaring RInlineFunction for a name-only parameter —
                // RInlineFunction.declaringLambdaOf reads both; `extract x [ … x … ]`) and
                // the legacy re-entrant SYNTHESIS head resolved by name (symbol EMPTY — the
                // render-time receiver's head resolver binds inputs/output only, so a
                // closure-param head stays unresolved by construction; the #486 mechanism
                // decode). Both lower to the DISTINCT shallow IRClosureParam (the 22nd
                // IRExprKind — the #499 distinct-kind law: no native gate admits it silently;
                // the #502 consumer admissions name it, and every containing claim root
                // oracle-serves whole through the literal legacy line, where the binder's own
                // scope machinery renders the name). The type/cardinality channels read the
                // node's own engine facts (typed on the parsed bound flavor; MISSING on the
                // synthetic flavor — the cache-boundary class); Optionality NORMALIZED PRESENT
                // (the #500/#501 shallow-kind convention).
                boolean boundClosureParam =
                        RInlineFunction.declaringLambdaOf(symbol, ref.name()).isPresent();
                boolean syntheticClosureParam = symbol == null && ref.name() != null
                        && isEnclosingClosureParamName(ref, ref.name());
                if (boundClosureParam || syntheticClosureParam) {
                    // #521: the binder-source typing — the synthetic flavor mints
                    // cache-invisible (the #502 note above), which left every
                    // closure-param-ROOTED arg nav on the #491 root-type belt
                    // (argNav.typeMissing.root:IRClosureParam.* — the whole #520-SOT
                    // face). The param's value IS its registering binder's element/
                    // whole value (the #506 cpSym law), so a missing type reads the
                    // binder's own pipe source through the CACHED channel (the #519
                    // synthesis's source-type read; type-channel only — the render
                    // routes and the cardinality channel are untouched, and a
                    // source-untyped param stays MISSING, the honest residue).
                    RMetaAnnotatedType cpType = type(ref, ws);
                    if (cpType == null || cpType.isMissing()) {
                        RExpression cpBinderSource = closureParamBinderSource(ref, ref.name());
                        if (cpBinderSource != null) {
                            RMetaAnnotatedType cpSourceType = type(cpBinderSource, ws);
                            if (cpSourceType != null && !cpSourceType.isMissing()) {
                                cpType = cpSourceType;
                            }
                        }
                    }
                    return Optional.of(new IRClosureParam(ref.name(), id,
                            cpType, card(ref, ws), Optionality.PRESENT,
                            ref.sourceRange()));
                }
                // #505 arm-C — the DISPATCH-BASE input reference (the symbolUnresolved
                // .synthetic.absent face's decoded class, 232 sole at the #504 SOT; the #505
                // dispatchGate census proved it 100% variant + base-input-hit,
                // [calculation]-dominant, the cdm-twin 116/116): inside a per-enum-value
                // dispatch VARIANT a body reference to a BASE declaration input resolves
                // through legacy's dispatch scope-join (the PR #369 facet) while the fork's
                // linker leaves it EMPTY by construction (the variant's own inputs() is the
                // synthesized placeholder). Lowers to the DISTINCT shallow
                // IRDispatchInputRef (an ORACLE LEAF — the oracle-first design the #505
                // charter mandates for the [calculation]-body byte risk: every containing
                // claim root renders through the compiler's oracle-root serve, the literal
                // legacy line, byte-identical BY IDENTITY). Precedence: AFTER the alias-match
                // and closure-param arms (legacy's own dispatch order restated).
                if (symbol == null && ref.name() != null) {
                    Optional<IRExpr> dispatchInput = adaptDispatchBaseInputRef(ref, id, ws);
                    if (dispatchInput.isPresent()) {
                        return dispatchInput;
                    }
                }
            }
            // #508 arm-C — the NODE-LOCAL seat-scoped enum requalify (the symbolNotAttribute
            // face's recovery classes CLAIMED at the node — 166 sole at the #507 SOT; the
            // symNotGate census re-ran the STANDING #504 ladders in the REAL parent context
            // and read the dominant rows argFires/eqFires: the #504 arms' facts hold, but a
            // PARENT-side mint cannot serve a node the blocker walk re-adapts STANDALONE, and
            // an apply deferred on its own gates never reaches its arg loop). The SAME #504
            // mirrors run here keyed on the PARENT seat — exactly legacy's own recovery
            // seats, the strong-guard-per-leg law: the call-ARG seat against the resolved
            // callee's positional declared enum parameter (tryBareEnumArg's seat — RFunction
            // callees of ANY origin AND rule callees through the fromRule bridge, per the
            // TechnicalRecordIdRule golden's RegimeNameEnum.ASIC render) and the EQUALITY
            // seat against the sibling's cached enumeration (tryBareEnumComparand's seat).
            // The minted IRReference{ENUM_VALUE} is IDENTICAL to the #504 parent-side mint
            // (same builder, same id and type channels), so whichever path fires first the
            // parent-seat renders cannot move; every OTHER parent seat keeps declining
            // (legacy has no recovery there — the honest residue).
            if (ref.parent() instanceof RSymbolReference enclosingCall
                    && !enclosingCall.args().isEmpty()) {
                int argIdx = enclosingCall.args().indexOf(ref);
                RNode calleeSym = enclosingCall.symbol().orElse(null);
                RFunction calleeFn = calleeSym instanceof RFunction f ? f
                        : calleeSym instanceof RRule rule ? RFunction.fromRule(rule) : null;
                if (calleeFn != null && argIdx >= 0 && argIdx < calleeFn.inputs().size()) {
                    Optional<IRExpr> requalified = requalifiedEnumArg(ref,
                            calleeFn.inputs().get(argIdx), id, ws);
                    if (requalified.isPresent()) {
                        return requalified;
                    }
                }
            }
            if (ref.parent() instanceof REqualityExpr enclosingEq) {
                RExpression sibling = enclosingEq.rawLeft() == ref ? enclosingEq.rawRight()
                        : enclosingEq.rawLeft();
                Optional<IRExpr> requalified = requalifiedEnumComparand(ref, sibling, id, ws);
                if (requalified.isPresent()) {
                    return requalified;
                }
            }
            // #530 arm-C — the CONTEXT-TYPED seat pair (the kvpGate census's whole
            // population): legacy's SCOPING resolves a bare value name against the seat's
            // expected ENUM at the constructor-value and conditional-branch seats — the
            // fork's context-free linker binds the global type/rule instead (goldens
            // PriceTypeEnum.CASH_PRICE / IndexEnum.ESTR) — so the SAME requalify mints
            // there: the KVP seat against the constructed type's KEY attribute declared
            // enum, and the conditional-BRANCH seat (then/else, never the condition)
            // against the conditional's contextual enum — the else-if chain ascended
            // branch-by-branch to the nearest non-conditional seat (the KVP key one level
            // out, or the plain un-segmented set-target output attribute). The minted
            // IRReference{ENUM_VALUE} is the #504/#508 builder verbatim (the seat
            // attribute's own type channel), so the ctor-value and conditional-branch
            // renders treat it exactly like a genuinely-bound bare enum value.
            // Census-narrow: the admitted false-bind classes are the census's own
            // (RDataType/RBody — see requalifiedEnumContext); every other seat keeps
            // declining (legacy has no scoping context there — the honest residue).
            Optional<IRExpr> ctxRequalified = requalifiedEnumAtContextSeat(ref, id, ws);
            if (ctxRequalified.isPresent()) {
                return ctxRequalified;
            }
            // #522 arm-B — the BY-NAME implicit-item bare-nav admission (the residual
            // symbolNotAttribute classes): a bare name the linker bound to a GLOBAL
            // non-attribute declaration is, at legacy's render, the enclosing binder's
            // implicit-ITEM feature read — the ladder, the equivalent and the byte story
            // live on the arm (the #480 adaptBareAttrItemNav pattern at the BY-NAME seat).
            // Precedence: LAST among the non-attribute arms, mirroring legacy
            // handle(RSymbolReference)'s own dispatch (the alias/enum-recovery seats win).
            Optional<IRExpr> bareItemNav = adaptNonAttrBareItemNav(ref, symbol, id, ws);
            if (bareItemNav.isPresent()) {
                return bareItemNav;
            }
            // alias / navigation — defer (the L-109 point-free class lowers above since #492).
            return Optional.empty();
        }
        if (isSyntheticRuleInputRef(attr)) {
            // #497 (the conversion cluster's seat-2 arm): the bare RULE-INPUT read — the ref's
            // symbol is the fromRule/fromReport WRAPPER's ORPHAN synthetic `input` attribute
            // (name "input", parent NEVER wired — the factories build it detached; a genuine
            // model attribute always has a declaring parent), read inside a from-typed
            // rule OR report-wrapper function (the identity is self-contained precisely so BOTH
            // wrapper contexts claim — see the predicate's javadoc). Legacy's ladder
            // variable-paths it POSITION-INDEPENDENTLY (every synthesizer
            // branch null-guards on identity against REAL type features, which the orphan can
            // never be) to `MapperS.of(input)` — the wrapper's own evaluate parameter, the Wave-0
            // PARAM leaf render. Lower to exactly that: IRVariable{PARAM, "input"} with the
            // declared facts (the #497 census read the class at 2,210, ALL in the drr RULE cell,
            // ALL the varPath ladder face — the sample witness's `attr=input declaring=<orphan>`
            // decode).
            boolean inputMulti = isMultiCardinality(attr);
            // Optionality NORMALIZED to the class's semantic fact (Copilot #497 R2 suppressed-valid,
            // escalating Seat-1 OBS-2): the wrapper input is (0..1) — the factories declare it, and
            // legacy's buildImplicitInputReceiver twin simply populates no cardinality — and
            // optionality() is SERIALIZED (IR JSON/printer), so deriving it via optionalityOf would
            // split the same semantic "rule/report input" read on builder happenstance
            // (OPTIONAL vs PRESENT) for non-Java consumers. Byte-inert at the Java target (the
            // PARAM SINGLE render ignores optionality — ring-proven at population).
            return Optional.of(new IRVariable(ref.name(), IRVariable.VariableKind.PARAM, id,
                    type(ref, ws),
                    inputMulti ? ExpressionCardinality.MULTI : ExpressionCardinality.SINGLE,
                    Optionality.OPTIONAL, ref.sourceRange()));
        }
        if (!(attr.parent() instanceof RFunction func)
                || func.origin() != RFunction.Origin.FUNCTION
                || !containsByIdentity(func.inputs(), attr)) {
            // #480: a bare ITEM-feature read inside an implicit/paramless filter/extract lambda
            // lowers via its legacy-equivalent item navigation (the bare-attr arm — the identity
            // guard admits only the element type's OWN feature, so a rule/report-scoped attr or a
            // function output referenced by name never passes); everything else keeps deferring.
            // #640 arm A: legacy's condition-instance reference (a parentless synthetic attribute named after the
            // enclosing type) lowers to the shallow IRConditionInstance oracle leaf BEFORE the item-nav walk
            Optional<IRExpr> condInstance = adaptSyntheticConditionInstance(ref, attr, id);
            if (condInstance.isPresent()) {
                return condInstance;
            }
            Optional<IRExpr> itemNav = adaptBareAttrItemNav(ref, attr, id, ws);
            if (itemNav.isPresent()) {
                return itemNav;
            }
            // #514 ARM-1: the bare function-OUTPUT reference (the notAnInputParam.out decode —
            // 100% output-by-identity, declaring=RFunction; legacy variable-paths the output
            // holder, the implicitAttrRoot census's noLambda.varPath rows). Lowers to the
            // DISTINCT shallow IROutputRef oracle leaf (the #505 IRDispatchInputRef pattern):
            // the name + the attribute-channel type are the facts, the varPath render is
            // legacy's own inside the containing root's oracle serve. Gates prove-or-decline:
            // output-by-identity, non-meta (a meta output's wrapper coercion is legacy's own),
            // the attribute channel types it.
            if (attr.parent() instanceof RFunction outFn
                    && outFn.origin() == RFunction.Origin.FUNCTION
                    && outFn.output().orElse(null) == attr
                    && !isMetaAnnotated(attr)) {
                RMetaAnnotatedType outType = ws == null ? null : ws.getInferredAttributeType(attr);
                if (outType != null && !outType.isMissing()) {
                    // Optionality = the OUTPUT's declared fact (the Copilot #514 R1 catch,
                    // class-swept across the three mints): one declaration site, no #497-
                    // style builder happenstance — the serialized channel carries the
                    // honest read; byte-inert at the Java target (oracle serves only).
                    return Optional.of(new IROutputRef(ref.name(), id, outType,
                            isMultiCardinality(attr) ? ExpressionCardinality.MULTI
                                    : ExpressionCardinality.SINGLE,
                            optionalityOf(attr), ref.sourceRange()));
                }
            }
            // #528 arm-5 — THE L-113 CONVERSION: the bare-attr arm's own residue lowers to the
            // DISTINCT shallow IRImplicitAttrNav (the 45th kind — the #514 IRRuleInputNav
            // pattern one seat over). The arm above claims every shape whose equivalent
            // navigation it can BUILD; what reaches here are the walk-family declines the #497
            // implicitAttrRoot census names (shortcutCollision / noFilterExtractBinder /
            // sourceElementUnresolved — the equivalent cannot be synthesized at all), so the IR
            // states the fact it genuinely knows (WHICH attribute is read) and leaves the
            // receiver synthesis to legacy.
            //
            // The ACCOUNTING is the point (the #516 law): these claims were the compiler's
            // L-113 relabel belt, which sits inside the ir-EMPTY branch and therefore counted
            // DELEGATED — 13 of the 13 delegated events at the #527 SOT. A counter flip alone
            // would have been a RELABEL, not a conversion; minting the lowering is what makes
            // them honestly IR-LOWERED. The render does not move: the belt rendered
            // super.visitSymbolReference, which is exactly the oracle dispatch's own
            // bareSymbolRef serve for an args-empty symbol reference — byte-identical BY
            // IDENTITY. Meta-annotated attributes stayed OUT at #528 (the belt excluded them too: a
            // FieldWithMetaX retype is legacy's own, the #514 ARM-3 kind owns the input seat) UNTIL
            // PR #640's arm E below admitted them to the mint (round 1, cq SF-2: this note re-cut).
            // The type/cardinality carry the attribute channel's own reads and are
            // non-load-bearing (every containing root oracle-serves, the #504 convention; at #528
            // no consumer admitted the kind - since #529 and PR #640 the admitting consumers are
            // NAMED on IRExprKind.IMPLICIT_ATTR_NAV's javadoc, the #499 law).
            //
            // CENSUS-NARROW (the standing law): the mint fires only for the three walk
            // families the #497 census actually names ({@link #isImplicitAttrMintFamily} —
            // shortcutCollision / noFilterExtractBinder / sourceElementUnresolved, the
            // equivalent-un-buildable classes) AND only at the attr-outside-function leg —
            // the census faces are attrOutsideFunction.* spellings, so a function-parented
            // bind (a rule/report-scoped attr or a function output/shadow referenced by
            // name) keeps its standing decline even when its gate walk would read a mint
            // family (the Seat-1 #528 OBS-3 narrowing: without the leg guard the mint also
            // claimed those legs, bypassing the #514 ARM-1 untyped-output decline). Every
            // OTHER decline of the bare-attr arm keeps its standing behaviour: the
            // identity-guard residue (the attribute is not the element type's own feature —
            // a shape whose legacy render this kind's neutral fact would MISSTATE), the
            // #514 rule-input sub-faces (that arm's own prove-or-decline ladder) and any
            // equivalent-nav sub-reason (a real lowering gap, not a synthesis gap) all
            // still defer, so the wave claims its decoded population and nothing else.
            // The attribute channel must TYPE the mint (the #514 IRRuleInputNav pattern's
            // own guard, the Copilot #528 R1 catch — the serialized channel assumes a
            // non-null type); an untypeable carrier declines to the belt, whose frozen-zero
            // meter names it (prove-or-decline).
            // #640 arm E (v3.3 seat 4): the META-annotated bare attribute joins the mint (the aiming read of the first offload box
            // cut - 119 of cdm 6.20.6's 177 residual claims - is UNPRINTED: that status was overwritten by the second run
            // under the same name, PR #640 round 1 spec SF-2; the class is `quantity exists` over a [metadata reference]
            // attribute, and the printed outcome is ir-share-s4c4.print's) - the kind
            // is an oracle leaf, so the containing root renders WHOLE through legacy and the FieldWithMetaX retype
            // the #528 note guarded (a NATIVE render's concern) never arises; the minted type is the reference's own
            // (with its meta) when the engine types it, else the attribute channel's
            if (!(attr.parent() instanceof RFunction)
                    && ref.name() != null
                    && isImplicitAttrMintFamily(bareAttrItemNavDeclineGate(ref, attr, ws))) {
                RMetaAnnotatedType implicitType = implicitAttrMintType(ref, attr, ws);
                if (implicitType == null || implicitType.isMissing()) {
                    return Optional.empty(); // untypeable — the belt keeps the claim, metered
                }
                return Optional.of(new IRImplicitAttrNav(ref.name(), id, implicitType,
                        isMultiCardinality(attr) ? ExpressionCardinality.MULTI
                                : ExpressionCardinality.SINGLE,
                        optionalityOf(attr), ref.sourceRange()));
            }
            return Optional.empty();
        }
        if (collidesWithShortcut(func, ref.name())) {
            return Optional.empty(); // legacy renders this as an alias invocation — defer
        }
        if (isMetaAnnotated(attr)) {
            // #514 ARM-3 (the #513 "later increment" landed): the META-ANNOTATED input lowers
            // to the DISTINCT shallow IRMetaParamRef oracle leaf (the #505 IRDispatchInputRef
            // pattern) — the name + the attribute-channel WRAPPED type are the facts; every
            // FieldWithMetaX coercion is legacy's own inside the containing root's oracle
            // serve. The CONTAINMENT is the kind (the #499 law): the unwrap-needing call-arg
            // seats are NOT admitted (the pPlain/argSeatMiss decode slices keep declining at
            // the callArgs gate), so no coercion-needing render can route natively.
            RMetaAnnotatedType mpType = ws == null ? null : ws.getInferredAttributeType(attr);
            if (mpType == null || mpType.isMissing()) {
                return Optional.empty(); // the attribute channel could not type the input
            }
            // Optionality = the input's declared fact (the Copilot #514 R1 class sweep).
            return Optional.of(new IRMetaParamRef(ref.name(), id, mpType,
                    isMultiCardinality(attr) ? ExpressionCardinality.MULTI
                            : ExpressionCardinality.SINGLE,
                    optionalityOf(attr), ref.sourceRange()));
        }
        boolean multi = isMultiCardinality(attr);
        // The MULTI witness/output simple-name collision (the FQN-inline render) is a JAVA-emission
        // concern, resolved in the emitter against the enclosing function's output Java type (L-029) —
        // NOT here: the language-neutral adapter cannot name Java types, and a Rune-name proxy mis-keys
        // the cross-namespace case. So the adapter lowers the multi param unconditionally and the emitter
        // renders bare-witness-or-FQN-inline.
        // Cardinality from the DECLARED cardinality (matching legacy gm.isMulti, and reliable for a
        // bare reference whose expression cardinality IS the variable's): SINGLE → MapperS.of(name),
        // MULTI → MapperC.<Item>of(name) in the emitter. The strict isScalarParam keeps a MULTI param
        // out of the scalar-only comparison/equality/existence gates.
        ExpressionCardinality cardinality = multi ? ExpressionCardinality.MULTI : ExpressionCardinality.SINGLE;
        return Optional.of(new IRVariable(ref.name(), IRVariable.VariableKind.PARAM, id,
                type(ref, ws), cardinality, optionalityOf(attr), ref.sourceRange()));
    }

    /**
     * Lowers a <em>qualified</em> enum-value reference — {@code SomeEnum -> VALUE} — to a neutral
     * {@link IRReference} of kind {@link IRReference.ReferenceKind#ENUM_VALUE}, the same node the bare
     * enum-value arm of {@link #adaptSymbolReference} produces (L-016). The emitter renders the unwrapped
     * constant {@code EnumName.CONSTANT} via {@code IRJavaLeafEmitter.emitEnumValue} (the enum simple name
     * from the node's resolved {@code type()}; the constant {@code formatEnumName(stripEscape(target))} on
     * the carried value name; plus the enum import) — byte-identical to the genuine-enum arm of legacy
     * {@code ReferenceHandler.handle(REnumValueRef)}. As a function-call argument the constant takes the
     * dotted-enum pass-through of {@code ReferenceHandler.unwrapForEvaluateArg} (no {@code MapperS.of} wrap,
     * no {@code .get()}); as a standalone/operand expression it is the existing {@code ENUM_VALUE} render.
     *
     * <p><strong>The 6-way {@code REnumValueRef} gate (the load-bearing parity guard).</strong> The grammar
     * parses every {@code a -> b} as an {@link REnumValueRef}, so the node carries SIX mutually-distinct
     * resolved channels — a genuine enum value, a callable-symbol record-field navigation, an
     * {@code attribute -> Subtype} type-restriction downcast, a choice-option narrowing, an implicit-input
     * attribute-feature chain, and a disguised feature-call. Only the GENUINE enum value renders a constant;
     * the rest render {@code Mapper} navigation chains. This arm claims the genuine case; of the five
     * disguises, the input-feature navigation lowers through its legacy-equivalent feature call (the #478
     * {@link #adaptDisguisedInputNav} arm) and the other four defer, mirroring
     * {@code ExpressionTypeComputer.computeEnumValueRef}'s resolution precedence
     * EXACTLY: {@code resolvedAttributeChain} → {@code resolvedChoiceOption} → {@code resolvedTypeRestriction}
     * are checked BEFORE {@code enumeration()}, so a node with any of those present types to a NON-enum
     * {@code RType} and must not lower here (were it admitted, {@code emitEnumValue} would render against a
     * non-enum {@code type().name()} — a wrong constant, not a decline, since the emitter declines only on a
     * {@code null} type). Requiring {@code enumValue().isPresent()} pins the value half to legacy's exact
     * {@code EnumHelper.convertValue(enumValue)} render and excludes the {@code enumeration}-only fallback
     * (which legacy renders from the raw {@code valueName()} string). A too-strict gate is harmless (the ref
     * stays on legacy, byte-green); only a too-loose gate could diverge, and the FUNCTION byte gate catches it.
     *
     * <p>No shortcut-collision defer (unlike the bare arm): legacy {@code handle(REnumValueRef)} renders the
     * constant unconditionally on {@code enumeration().isPresent()} with no {@code isAliasReference} name-match
     * fallback, so a qualified enum-value ref never collides with an enclosing shortcut.
     */
    /**
     * #505 arm-C: lowers a dispatch-base input reference — an UNRESOLVED bare name inside a
     * per-enum-value dispatch VARIANT body that names a BASE declaration input — to the
     * DISTINCT shallow {@link IRDispatchInputRef} (see the kind's javadoc for the class
     * decode). The gates, prove-or-decline: the enclosing function carries a
     * {@code dispatch()} header (the variant identity); the SAME-FILE base resolves
     * ({@code RFunction.dispatchBase()} — the parser-model mirror of legacy
     * {@code HandlerHelper.dispatchBaseOf}; a cross-file base under-claims byte-safely); the
     * name matches a base input EXACTLY; the {@code itemAttr} precedence exclusion (the #486
     * facet ladder's own order — a name that ALSO resolves as the enclosing binder's element
     * member keeps legacy's item resolution; zero-population at the census, the drift belt);
     * a meta-annotated input declines (the wrapper coercion is legacy's own); the input types
     * through the ATTRIBUTE channel (the census read the NODES 100% typeMissing — the
     * synthesized pieces the cache never saw — so the declared-typeCall channel is the only
     * honest source, the #478 retype law).
     */
    private Optional<IRExpr> adaptDispatchBaseInputRef(RSymbolReference ref, NodeId id,
            RWorkspace ws) {
        RAttribute input = dispatchBaseInputOf(ref);
        if (input == null) {
            return Optional.empty(); // outside the variant + base-input class
        }
        if (nearestEnclosingInlineFunction(ref) != null) {
            RExpression src = filterExtractSourceOfArmBinder(ref);
            RDataType elementType = src == null ? null : sourceElementDataType(src, 0);
            if (elementType != null
                    && findAttributeOnDataTypeByName(elementType, ref.name()) != null) {
                return Optional.empty(); // the itemAttr precedence — legacy's item resolution wins
            }
        }
        if (isMetaAnnotated(input)) {
            return Optional.empty(); // a meta input's wrapper coercion is legacy's own — defer
        }
        RMetaAnnotatedType inputType = ws == null ? null : ws.getInferredAttributeType(input);
        if (inputType == null || inputType.isMissing()) {
            return Optional.empty(); // the attribute channel could not type the input
        }
        return Optional.of(new IRDispatchInputRef(ref.name(), id, inputType,
                isMultiCardinality(input) ? ExpressionCardinality.MULTI
                        : ExpressionCardinality.SINGLE,
                Optionality.PRESENT, ref.sourceRange()));
    }

    /**
     * #505 arm-C: the dispatch-base input the reference names, or {@code null} — the shared
     * class gate ({@link #adaptDispatchBaseInputRef} + the feature-recovery leg in
     * {@link #adaptFeatureCall} + the twins): the enclosing function is a dispatch VARIANT
     * ({@code dispatch()} present), its SAME-FILE base resolves, and the base declares an
     * input with EXACTLY this name.
     */
    private RAttribute dispatchBaseInputOf(RSymbolReference ref) {
        if (ref.name() == null || ref.name().isEmpty() || !ref.args().isEmpty()) {
            return null;
        }
        // #507: the walk itself extracted to the NAME-keyed core (dispatchBaseInputNamed) so the
        // record arm shares it verbatim — behavior byte-identical (pure code motion).
        return dispatchBaseInputNamed(ref, ref.name());
    }

    private Optional<IRExpr> adaptEnumValueRef(REnumValueRef enr, NodeId id, RWorkspace ws) {
        if (!isGenuineEnumValueRef(enr)) {
            // #478: the input-feature-nav disguise LOWERS via its legacy-equivalent feature call;
            // every other disguise (and the raw-valueName fallback) keeps deferring to legacy.
            return adaptDisguisedInputNav(enr, id, ws);
        }
        return Optional.of(new IRReference(enr.enumValue().get().name(), IRReference.ReferenceKind.ENUM_VALUE, id,
                type(enr, ws), card(enr, ws), Optionality.PRESENT, enr.sourceRange()));
    }

    /**
     * Whether a qualified enum-value reference is the <em>genuine</em> enum-value case — the only shape that
     * renders an {@code EnumName.CONSTANT} (legacy {@code ReferenceHandler}'s genuine arm). The grammar parses
     * every {@code a -> b} as an {@link REnumValueRef} carrying SIX mutually-distinct resolved channels; this
     * claims ONLY the genuine enum value and excludes the five disguises: an attribute-chain navigation, a
     * choice-option narrowing and a type-restriction downcast — all resolved BEFORE {@code enumeration()} in
     * {@code ExpressionTypeComputer.computeEnumValueRef} (so a node with any present types to a NON-enum
     * {@code RType} and must never render a constant), plus the {@code enumeration}-only fallback (excluded by
     * requiring {@code enumValue()}, which pins the value to legacy's {@code EnumHelper.convertValue(enumValue)}
     * render rather than the raw {@code valueName()} string). The exact De Morgan complement of
     * {@link #adaptEnumValueRef}'s defer-gate (L-044).
     *
     * <p><strong>Shared</strong> by {@link #adaptEnumValueRef} (the genuine-constant gate; a non-genuine ref
     * routes to the #478 {@link #adaptDisguisedInputNav} arm, which lowers the disguises its own gates admit
     * — the input-feature nav, the #480 chains, the #505 closure-param-head/choice-option arms — and defers
     * the rest) and the live {@code IRExpressionCompiler.visitEnumValueRef} seam (L-069), which TARGETS
     * this genuine sub-family at claim roots; since the #496/#497 conversion the L-111 disguised slice
     * takes the seat's QUIET adapter-first claim (residue on the byte-proven relabel), and since #505 the
     * REMAINING disguised residue is converted too (the L-109c exclusion retired — every disguised root
     * consults the IR: lowers drive, declines join the counted population; no uncounted sub-channel
     * remains at the seat). A neutral, purely-structural
     * predicate over the resolved AST — no Java-name decision (the L-029 split).
     */
    public static boolean isGenuineEnumValueRef(REnumValueRef enr) {
        return enr.enumeration().isPresent()
                && enr.enumValue().isPresent()
                && enr.resolvedAttributeChain().isEmpty()
                && enr.resolvedChoiceOption().isEmpty()
                && enr.resolvedTypeRestriction().isEmpty();
    }

    /**
     * Lowers a <em>disguised input-feature navigation</em> — a non-genuine {@link REnumValueRef}
     * carrying the typing engine's {@code resolvedInputFeature} bind (the L-111 channel: the grammar
     * parses {@code <head> -> <feature>} as an enum-value ref; the fixed-point inference resolved
     * the head lexically and the feature on its type) — by adapting the EXACT equivalent
     * {@link RFeatureCall} legacy {@code ReferenceHandler.synthesizeFeatureCall} builds for the same
     * node, through {@link #adaptFeatureCall}'s own gates (the #478 arm). The equivalence IS the
     * byte argument: under a legacy-rendered claim the disguised ref reaches
     * {@code handle(REnumValueRef)}'s fall-through, which synthesizes that equivalent and routes it
     * back through {@code visitFeatureCall} — where this adapter already lowers it and the emitter
     * renders it (the LIVE route at population). This arm builds the same equivalent from the same
     * resolution walk, so the {@link FieldAccess} it returns is the one the live route proves; the
     * #478 nav-gate witness measured the claimable slice ({@code inputFeatureNav:headInput:lowers})
     * with the legacy synthesizer itself, and the post-teach probe re-read is the standing drift
     * meter (a residual {@code headInput:lowers} bucket means this mirror and legacy's diverged).
     *
     * <p>The equivalent's head resolution mirrors legacy {@code resolveNameInFunction} exactly
     * (enclosing-function inputs by name, then the output, then null — a SHORTCUT head nulls HERE
     * and routes to the #492 {@link #adaptAliasHeadNav} leg below, whose claims render natively at
     * the seat since #497; the un-admitted alias faces stay on legacy, the L-032 class) via
     * {@link #resolveNameInFunction}; the feature resolves on the head's DECLARED type
     * ({@link #resolveFeatureOnDeclaredType} — legacy {@code resolveFeatureOnAttribute}'s walk),
     * NOT from the node's typing-only bind, so the arm renders exactly what legacy would even where
     * the two resolutions could disagree. Legacy's data-type-condition head branch is deliberately
     * NOT mirrored (the #478 witness measured its population at ZERO — {@code headCondAttr} appears
     * in no cell); a condition-seated disguise builds an unresolved-receiver equivalent and
     * declines, which is always byte-safe. Everything the equivalent's gates defer (meta features,
     * unresolved/alias/output heads, the receiver-lowering gates) keeps returning empty → legacy
     * fallback, byte-green by construction.
     *
     * <p><strong>The retype (the cache boundary).</strong> {@code RWorkspace.getInferredType} is an
     * expression-node-keyed cache populated by the fixed point over the PARSED tree — a synthesized
     * equivalent has no entry and would carry MISSING types, which the emitter declines ("cannot
     * name the witness/lambda var"; measured live: the first #478 ON ring moved every
     * adapter-unlocked event into the leafEmitter bucket). So the arm delegates the GATES to
     * {@link #adaptFeatureCall} on the equivalent, then rebuilds the proven single-hop
     * {@link FieldAccess} with the two types the caches DO hold: the head's ATTRIBUTE-channel type
     * ({@code RWorkspace.getInferredAttributeType} — computed from the declared {@code typeCall},
     * the emitter's lambda-var source, exactly the receiver-type channel legacy
     * {@code NavigationHandler} derives its lambda variable from) and the RAW node's own inferred
     * type (the #279 binder bind — the feature's type, the emitter's witness source, exactly the
     * resolved-feature channel legacy renders its witness from). An untypeable head/node declines
     * (named by the mirror twin — {@code headTypeMissing}/{@code navTypeMissing}).
     *
     * <p>The claim-ROOT seat CONSUMES this arm since the #496 leg-2 conversion (the Seat-1 #497
     * MF-2 recut of the original "root seat unmoved" story): {@code
     * IRExpressionCompiler.visitEnumValueRef}'s disguised branch consults the adapter FIRST
     * through the quiet claim and renders the proven {@link FieldAccess} shapes natively (the
     * IRVariable input-head slice #496, the IRReference alias-head slice #497); the residue keeps
     * the byte-proven L-111 relabel. The #478-era unlock (NESTED positions inside larger claims)
     * stands unchanged.
     */
    private Optional<IRExpr> adaptDisguisedInputNav(REnumValueRef enr, NodeId id, RWorkspace ws) {
        if (enr.resolvedAttributeChain().isPresent()) {
            // #480: a BOUND-chain disguise (the typing precedence winner over the input-feature
            // bind) routes to the implicit-item CHAIN arm; its own gates decline everything
            // outside the filter/extract item-chain slice (ruleInput chains, scope heads, …).
            return adaptImplicitItemChain(enr, id, ws);
        }
        if (enr.resolvedInputFeature().isEmpty()
                || enr.enumName() == null || enr.enumName().isEmpty()
                || enr.valueName() == null || enr.valueName().isEmpty()) {
            // #505 arm-A2: the choice-OPTION disguise CLAIMED (the choiceOption face — 239
            // sole at the #504 SOT, cdm6-dominant; the #505 enumSeatGate census read the
            // shape: a head segment outside the global type namespace navigating to a
            // resolved global RChoice/RDataType OPTION, beside the head:RChoiceTypeRef
            // static-selection minority). The pass-6 #451 clearing arm bound the OPTION
            // (resolvedChoiceOption); the head's own resolution and the option-selection
            // render (the choice projection, the case narrowing) are legacy's own — the mint
            // is the DISTINCT shallow IRChoiceOptionNav (an ORACLE LEAF: every containing
            // claim root renders through the compiler's oracle-root serve, incl. the #505
            // enumChain leg at the converted seat — the literal legacy line, byte-identical
            // BY IDENTITY). The channel precedence mirrors the twin's order EXACTLY: the
            // chain bind routed above; an inputFeature-present node never reaches this arm.
            if (enr.resolvedInputFeature().isEmpty() && enr.resolvedChoiceOption().isPresent()
                    && enr.enumName() != null && !enr.enumName().isEmpty()
                    && enr.valueName() != null && !enr.valueName().isEmpty()) {
                RMetaAnnotatedType optionNavType = type(enr, ws);
                if (optionNavType != null && !optionNavType.isMissing()) {
                    return Optional.of(new IRChoiceOptionNav(enr.enumName(), enr.valueName(),
                            id, optionNavType, card(enr, ws), Optionality.PRESENT,
                            enr.sourceRange()));
                }
            }
            // #507 arm-B: the RECORD-feature leg — the TRUE no-channel shape (the #444
            // clear-without-binding population: `startDate -> day` on a record-typed
            // input/dispatch-base input) lowers to the shallow IRRecordFeatureNav oracle
            // leaf; the arm's own gates decline everything else (headMiss/headNotRecord/
            // leafMiss — the honest residue, incl. the census's lambda-scope meta reads).
            Optional<IRExpr> recordNav = adaptRecordFeatureNav(enr, id, ws);
            if (recordNav.isPresent()) {
                return recordNav;
            }
            // #529 — the IMPLICIT-ITEM head at the DISGUISED seat (the itemHeadGate
            // census's whole pool: noResolutionChannel.headMiss.other 38 = 30 cdm6-f + 8
            // drr-f at the #528 SOT, 100% elem:data.head:attrHit — every head lives on the
            // enclosing BINDER's element form, the channel the #507 record arm's
            // function-scope ladder never searches). The single-arrow nav re-adapts as the
            // EXACT bare-head feature-call equivalent legacy's own implicit-item machinery
            // reads (the #478 equivalent-adapt convention at the #524 seat — the fc-seat
            // EMPTY-symbol head recovery runs the SAME shared bareItemMemberResolution
            // ladder, so the two parse seats cannot drift), and the accept is
            // CENSUS-NARROW to the two receiver-carrying mints the census read
            // (leaf:qual → IRQualifierReceiverNav 27 · leaf:recHit → IRRecordReceiverNav
            // 11 — both ORACLE LEAVES, identity-serve only), re-stamped with the RAW
            // node's id and range (the #524/#525 convention). Every other equivalent
            // verdict declines whole — prove-or-decline, the residue keeps the honest
            // headMiss.other spelling.
            Optional<IRExpr> itemHeadNav = adaptEnrItemHeadNav(enr, id, ws);
            if (itemHeadNav.isPresent()) {
                return itemHeadNav;
            }
            return Optional.empty(); // not the L-111 precedence-winner shape — defer to legacy
        }
        RFunction enclosing = enclosingFunction(enr);
        RAttribute head = enclosing == null ? null : resolveNameInFunction(enclosing, enr.enumName());
        if (enclosing == null) {
            head = conditionOwnerAttribute(enr, enr.enumName()); // #640 arm C: a condition's own attribute as the head
        }
        if (head == null) {
            // #492: the ALIAS-head leg (the L-032 teach) — resolveNameInFunction nulls a shortcut
            // match by legacy's own contract, so the equivalent path below can never claim it;
            // the arm lowers the single-hop nav DIRECTLY over a body-retyped IRReference{ALIAS}
            // when the shared walk admits. The decline faces fall through to the equivalent path,
            // whose mirror keeps minting the probed spellings (twin-exact).
            Optional<IRExpr> aliasNav = adaptAliasHeadNav(enr, enclosing, id, ws);
            if (aliasNav.isPresent()) {
                return aliasNav;
            }
        }
        Optional<IRExpr> lowered = adaptFeatureCall(disguisedInputNavEquivalent(enr, head), id, ws);
        if (lowered.isPresent() && lowered.get() instanceof IRMetaAccess condMeta
                && condMeta.receiver() instanceof IRImplicitAttrNav) {
            // #640 arm F: a meta feature (`-> scheme` / `-> reference`) over the condition-attribute head - an oracle
            // leaf over an oracle leaf, re-ranged to the chain (the first cut's aiming read - 25 of cdm 6.20.6's 177 residual claims - is UNPRINTED, PR #640
            // round 1 spec SF-2; ir-share-s4c4.print is the printed outcome). The #508 arm-A1 block below heads ITS OWN `if`.
            RMetaAnnotatedType condMetaType = type(enr, ws);
            if (condMetaType == null || condMetaType.isMissing()) {
                condMetaType = condMeta.type();
            }
            return Optional.of(new IRMetaAccess(rerangedImplicitAttrNav(condMeta.receiver(), enr), condMeta.feature(), condMeta.metaQualifiers(), id,
                    condMetaType, condMeta.cardinality(), condMeta.featureCardinality(), condMeta.optionality(),
                    enr.sourceRange()));
        }
        // #508 arm-A1 (the inputFeatureNav.metaFeature face CLAIMED — 96 sole at the #507
        // SOT, the #507 mirror fix's own named class): a META single-hop equivalent lowers
        // via the #499 IRMetaAccess arm (the metaHopGate census read the class 100%
        // recv:IRVariable — the typed input-param head — with reference/location/address/
        // scheme qualifiers, every row typed on the node's own channel). The arm ACCEPTS the
        // shape, re-ranged link-by-link like the 2-link spines (the raw node's range on both
        // nodes; the claim id + the node's own engine type on the top — the #480 top-hop
        // convention): at the L-111 root seat the #507 identity-serve leg renders the LITERAL
        // legacy line (byte-identical BY IDENTITY); an interior occurrence rides its
        // meta-bearing root's #500 oracle-root serve — the census read the class 100%
        // INTERIOR, so the standing routing law carries every live row.
        if (lowered.isPresent() && lowered.get() instanceof IRMetaAccess metaHop
                && (metaHop.receiver() instanceof IRVariable
                        // #517 arm-B2: the DISPATCH-INPUT meta receiver joins the #508
                        // arm-A1 acceptance (the inputFeatureNav.metaFeature.q:scheme
                        // residue — 6 delegated at the #516 l111Residue census, the
                        // ProcessFloatingRateReset-class scheme hops over a variant-body
                        // BASE input: the equivalent's head lowers to the #505
                        // IRDispatchInputRef oracle leaf and the #499 meta arm already
                        // lowers the hop over it — only THIS acceptance gate rejected the
                        // receiver kind). The routing is the standing meta law: a
                        // meta-bearing root serves whole-legacy (at the L-111 seat the
                        // #507 identity-serve leg — the literal super.visitEnumValueRef
                        // line, byte-identical BY IDENTITY); an interior hop rides its
                        // containing root's serve through the IRMetaAccess kind walk.
                        || metaHop.receiver() instanceof IRDispatchInputRef)) {
            RMetaAnnotatedType metaNavType = type(enr, ws);
            if (metaNavType == null || metaNavType.isMissing()) {
                metaNavType = metaHop.type();
            }
            IRExpr rangedMetaRecv;
            if (metaHop.receiver() instanceof IRVariable metaRecv) {
                rangedMetaRecv = new IRVariable(metaRecv.name(), metaRecv.variableKind(),
                        metaRecv.nodeId(), metaRecv.type(), metaRecv.cardinality(),
                        metaRecv.optionality(), enr.sourceRange());
            } else {
                IRDispatchInputRef dispatchRecv = (IRDispatchInputRef) metaHop.receiver();
                rangedMetaRecv = new IRDispatchInputRef(dispatchRecv.inputName(),
                        dispatchRecv.nodeId(), dispatchRecv.type(), dispatchRecv.cardinality(),
                        dispatchRecv.optionality(), enr.sourceRange());
            }
            return Optional.of(new IRMetaAccess(rangedMetaRecv, metaHop.feature(),
                    metaHop.metaQualifiers(), id, metaNavType, metaHop.cardinality(),
                    metaHop.featureCardinality(), metaHop.optionality(), enr.sourceRange()));
        }
        // #514 ARM-1b/ARM-3b: the OUTPUT-head and META-PARAM-head single hops ACCEPTED — the
        // equivalent's receiver now lowers to the IROutputRef/IRMetaParamRef oracle leaf (the
        // recv:notAnInputParam.out and recv:metaParam.* decode classes — `identifiers ->
        // observable` over the function's own output, `price -> value` over a
        // [metadata]-annotated input), re-ranged link-by-link like the #508 arm-A1 meta hop
        // (the raw node's range on both nodes; the claim id + the node's own engine type on
        // the top). The render is the standing oracle routing: the base is an ORACLE LEAF, so
        // the claim root (and every containing root) serves whole-legacy BY IDENTITY.
        // #517 arm-B1: the DISPATCH-INPUT head joins the SAME acceptance (the
        // offShape.fa:IRDispatchInputRef face — the L-111 belt's largest blocked class, 38
        // delegated + the probed twins: a variant-body `<base-input> -> <feature>` disguise
        // whose head lowers to the #505 IRDispatchInputRef oracle leaf). The identical
        // routing law carries it: at the L-111 root seat the #507 identity-serve leg renders
        // the literal super.visitEnumValueRef line (byte-identical BY IDENTITY — the same
        // line the belt's relabel renders); an interior occurrence routes its containing
        // root through the kind walk (IRDispatchInputRef is a standing oracle leaf) — the
        // L-112 receiver chains and the probed-pool operand positions ride the same law.
        if (lowered.isPresent() && lowered.get() instanceof FieldAccess leafHop
                && (leafHop.receiver() instanceof IROutputRef
                        || leafHop.receiver() instanceof IRMetaParamRef
                        || leafHop.receiver() instanceof IRDispatchInputRef)) {
            RMetaAnnotatedType leafNavType = type(enr, ws);
            if (leafNavType == null || leafNavType.isMissing()) {
                if (leafHop.receiver() instanceof IRDispatchInputRef) {
                    // #517 arm-B1: a variant-body disguise may be engine-UNTYPED (the #505
                    // arm-C census read the dispatch-scope nodes 100% typeMissing — the
                    // fixed-point cache never saw the synthesized pieces), so the hop's own
                    // FEATURE-channel type stands in (the #478 declared-typeCall retype law
                    // — the #508 meta leg's exact fallback posture). Byte-inert: the
                    // lowering renders only through the whole-legacy serves, and no
                    // emission decision reads the type at a served root.
                    leafNavType = leafHop.type();
                    if (leafNavType == null || leafNavType.isMissing()) {
                        return Optional.empty(); // both channels blind — the honest decline
                    }
                } else {
                    return Optional.empty(); // untypeable — the twin names it
                }
            }
            IRExpr rangedBase;
            if (leafHop.receiver() instanceof IROutputRef outBase) {
                rangedBase = new IROutputRef(outBase.outputName(), outBase.nodeId(),
                        outBase.type(), outBase.cardinality(), outBase.optionality(),
                        enr.sourceRange());
            } else if (leafHop.receiver() instanceof IRMetaParamRef metaBase) {
                rangedBase = new IRMetaParamRef(metaBase.paramName(), metaBase.nodeId(),
                        metaBase.type(), metaBase.cardinality(), metaBase.optionality(),
                        enr.sourceRange());
            } else {
                IRDispatchInputRef dispatchBase = (IRDispatchInputRef) leafHop.receiver();
                rangedBase = new IRDispatchInputRef(dispatchBase.inputName(),
                        dispatchBase.nodeId(), dispatchBase.type(), dispatchBase.cardinality(),
                        dispatchBase.optionality(), enr.sourceRange());
            }
            return Optional.of(new FieldAccess(rangedBase, leafHop.feature(), id, leafNavType,
                    leafHop.cardinality(), leafHop.featureCardinality(), leafHop.optionality(),
                    enr.sourceRange()));
        }
        if (lowered.isPresent() && lowered.get() instanceof FieldAccess condHop
                && condHop.receiver() instanceof IRImplicitAttrNav) {
            // #640 arm C: the condition-attribute head minted as the oracle leaf, the hop re-ranged to the chain
            RMetaAnnotatedType condNavType = type(enr, ws);
            if (condNavType == null || condNavType.isMissing()) {
                condNavType = condHop.type();
            }
            return Optional.of(new FieldAccess(rerangedImplicitAttrNav(condHop.receiver(), enr), condHop.feature(), id, condNavType,
                    condHop.cardinality(), condHop.featureCardinality(), condHop.optionality(), enr.sourceRange()));
        }
        if (lowered.isEmpty()
                || !(lowered.get() instanceof FieldAccess fa)
                || !(fa.receiver() instanceof IRVariable recv)) {
            return Optional.empty(); // the equivalent's own gates declined (or a non-single-hop shape — defensive)
        }
        RMetaAnnotatedType headType = head == null ? null : ws.getInferredAttributeType(head);
        RMetaAnnotatedType navType = type(enr, ws);
        if (headType == null || headType.isMissing() || navType == null || navType.isMissing()) {
            return Optional.empty(); // untypeable — the emitter could not name the lambda var / witness
        }
        IRVariable typedRecv = new IRVariable(recv.name(), IRVariable.VariableKind.PARAM, recv.nodeId(),
                headType, recv.cardinality(), recv.optionality(), enr.sourceRange());
        return Optional.of(new FieldAccess(typedRecv, fa.feature(), id, navType,
                fa.cardinality(), fa.featureCardinality(), fa.optionality(), enr.sourceRange()));
    }

    /**
     * The EXACT equivalent navigation legacy {@code ReferenceHandler.synthesizeFeatureCall} builds
     * for a disguised {@code <head> -> <feature>} ref — a fresh {@link RFeatureCall} over a fresh
     * {@link RSymbolReference} receiver (the raw head name), the caller-resolved head wired as the
     * receiver's symbol and the feature resolved against the head's declared type
     * ({@link #resolveFeatureOnDeclaredType}), both parented to the ref's own parent (the
     * synthesized nodes join no tree — the same throwaway construction legacy performs on this
     * exact path at render time). Shared by {@link #adaptDisguisedInputNav} and the
     * {@code inputFeatureNav} branch of {@link #reasonForEnumValueRef} — the arm and its reason
     * twin compute over the SAME equivalent by construction (both resolve the head via
     * {@link #resolveNameInFunction} and thread it here).
     */
    private static RFeatureCall disguisedInputNavEquivalent(REnumValueRef enr, RAttribute head) {
        RSymbolReference receiver = new RSymbolReference();
        receiver.setName(enr.enumName());
        receiver.setParent(enr.parent());
        RFeatureCall fc = new RFeatureCall();
        fc.setReceiver(receiver);
        fc.setFeatureName(enr.valueName());
        fc.setParent(enr.parent());
        if (head != null) {
            receiver.setResolvedSymbol(head);
            RAttribute feature = resolveFeatureOnDeclaredType(head, enr.valueName());
            if (feature != null) {
                fc.setResolvedFeature(feature);
            }
        }
        return fc;
    }

    /**
     * Mirror of legacy {@code ReferenceHandler.resolveNameInFunction}: the head name against the
     * enclosing function's inputs (first), then its output. A SHORTCUT match returns null by
     * legacy's own contract (an alias head cannot resolve to a single attribute — the receiver
     * stays unresolved and the equivalent declines; the null routes the caller to the #492
     * {@code adaptAliasHeadNav} leg, whose admitted claims render natively at the seat since
     * #497 while the un-admitted faces stay on legacy), which
     * the plain not-found fall-through shares — so the arm folds the two null arms into one.
     */
    private static RAttribute resolveNameInFunction(RFunction func, String name) {
        for (RAttribute input : func.inputs()) {
            if (name.equals(input.name())) {
                return input;
            }
        }
        if (func.output().isPresent() && name.equals(func.output().get().name())) {
            return func.output().get();
        }
        return null; // incl. legacy's explicit shortcut-match null arm — the same result either way
    }

    /**
     * Lowers a deep-path feature navigation ({@code receiver ->> feature} — the
     * {@code DeepFeatureCallExpr} operator) to {@link IRDeepFeatureNav} — the #507 arm-A teach
     * ({@code RDeepFeatureCall:noAdaptArm}, 191 sole + 71 untargeted roots at the #506 SOT, all
     * cdm6 FUNCTION; the family had NO adapt arm, so the generic fallback token carried the
     * whole class). The gates, in exit order (prove-or-decline — the #506 strong-guard law;
     * each has a {@code reasonForDeepFeatureCall} twin):
     * <ol>
     *   <li>the linker's deep-feature resolution ({@code resolvedFeature} — the
     *       DEEP_FEATURE_NOT_FOUND channel) present, with a non-empty written feature name
     *       ({@code deepFeatureUnresolved});</li>
     *   <li>the resolved feature NOT meta-annotated ({@code deepFeatureMeta} — the L-029
     *       boundary's conservative belt; the #507 deepGate census read the live population
     *       100% {@code featHit} with ZERO meta rows, so the gate is an explicit-zero
     *       belt, not a live residue);</li>
     *   <li>the receiver subtree lowers ({@code receiverNotExpressible} — the census read
     *       100% {@code recvLowers});</li>
     *   <li>the node's own inferred type present ({@code navTypeMissing} — the census read
     *       100% {@code typed}; the honest belt for the cache boundary).</li>
     * </ol>
     * The deep-path RESOLUTION and render — legacy's generated {@code DeepPathUtil
     * .choose<Feature>} routing behind the {@code ->>} semantics — are legacy's own render
     * decisions (the L-029 split): the mint carries the receiver's lowering plus the feature
     * NAME (the neutral fact), and every containing claim root renders through the compiler's
     * oracle-root serve (incl. the #507 {@code deepChain} dispatch leg — the literal
     * {@code super.visitDeepFeatureCall} line), byte-identical BY IDENTITY; the kind has NO
     * leaf-emitter arm.
     */
    private Optional<IRExpr> adaptDeepFeatureCall(RDeepFeatureCall deep, NodeId id, RWorkspace ws) {
        RAttribute feature = deep.resolvedFeature().orElse(null);
        if (feature == null || deep.featureName() == null || deep.featureName().isEmpty()) {
            return Optional.empty(); // deepFeatureUnresolved
        }
        if (isMetaAnnotated(feature)) {
            return Optional.empty(); // deepFeatureMeta — the conservative L-029 belt (census: zero live)
        }
        RExpression rawReceiver = deep.receiver();
        if (rawReceiver == null) {
            return Optional.empty(); // receiverNotExpressible (the parse-robustness shape)
        }
        Optional<IRExpr> receiver = adapt(rawReceiver, id.child(0), ws);
        if (receiver.isEmpty()) {
            return Optional.empty(); // receiverNotExpressible
        }
        RMetaAnnotatedType navType = type(deep, ws);
        if (navType == null || navType.isMissing()) {
            return Optional.empty(); // navTypeMissing — the cache-boundary belt (census: 100% typed)
        }
        return Optional.of(new IRDeepFeatureNav(receiver.get(), deep.featureName(), id,
                navType, card(deep, ws), Optionality.PRESENT, deep.sourceRange()));
    }

    /**
     * Lowers a RECORD-feature read disguised as a single-arrow nav ({@code head -> feature}
     * with NO resolution channel bound — the #444 clear-without-binding population) to
     * {@link IRRecordFeatureNav} — the #507 arm-B teach ({@code REnumValueRef:
     * noResolutionChannel}, 175 sole at the #506 SOT; the #507 noChanGate census read the
     * dominant faces {@code dispatchInput.rec:date.leafRecHit} — the YearFraction-family
     * dispatch bodies — and {@code input.rec:date/zonedDateTime.leafRecHit}, the plain-input
     * drr slice). The gates, in exit order (prove-or-decline; each has a twin inside
     * {@link #reasonForEnumValueRef}'s refined {@code noResolutionChannel.<facet>} tokens):
     * <ol>
     *   <li>the TRUE no-channel identity — every resolution channel empty (chain / input
     *       feature / choice option / type restriction / enumeration / head symbol); a node
     *       with ANY bound channel is another arm's shape, never this one's;</li>
     *   <li>both written segments present (the parse-robustness shape);</li>
     *   <li>the head resolves in the enclosing function's scope by legacy's OWN order —
     *       {@link #resolveNameInFunction} (inputs, then the output), then the dispatch-base
     *       scope-join ({@link #dispatchBaseInputNamed} — the PR #369 facet legacy applies
     *       and the fork's linker leaves unbound) ({@code headMiss} — the honest residue:
     *       the census's lambda-scope meta reads stay declined);</li>
     *   <li>the head's DECLARED type is a RECORD type ({@code headNotRecord});</li>
     *   <li>the leaf names one of the record's OWN features — {@code RRecordType.features()},
     *       the #444 membership rule ({@code leafMiss}).</li>
     * </ol>
     * The node's inferred type is MISSING BY CONSTRUCTION (the resolver binds nothing, so the
     * fixed point never types the node) — the mint carries the sentinel and NO typing gate
     * applies (unlike the choice-option mint): the kind is served only BY IDENTITY (the #505
     * {@code enumChain} dispatch leg — the literal {@code super.visitEnumValueRef} line,
     * legacy's whole record-feature ladder inside it), never emitter-composed.
     */
    private Optional<IRExpr> adaptRecordFeatureNav(REnumValueRef enr, NodeId id, RWorkspace ws) {
        if (enr.resolvedAttributeChain().isPresent() || enr.resolvedInputFeature().isPresent()
                || enr.resolvedChoiceOption().isPresent() || enr.resolvedTypeRestriction().isPresent()
                || enr.enumeration().isPresent() || enr.resolvedSymbol().isPresent()) {
            return Optional.empty(); // a bound channel — another arm's shape (the no-channel identity)
        }
        if (enr.enumName() == null || enr.enumName().isEmpty()
                || enr.valueName() == null || enr.valueName().isEmpty()) {
            return Optional.empty(); // the parse-robustness shape
        }
        RFunction enclosing = enclosingFunction(enr);
        RAttribute head = enclosing == null ? null : resolveNameInFunction(enclosing, enr.enumName());
        if (head == null) {
            head = dispatchBaseInputNamed(enr, enr.enumName());
        }
        if (head == null) {
            return Optional.empty(); // headMiss — the honest residue (lambda-scope heads stay declined)
        }
        RRecordType record = declaredRecordTypeOf(head);
        if (record == null) {
            return Optional.empty(); // headNotRecord
        }
        RRecordFeature feature = null;
        for (RRecordFeature f : record.features()) {
            if (enr.valueName().equals(f.name())) {
                feature = f;
                break;
            }
        }
        if (feature == null) {
            return Optional.empty(); // leafMiss — not a feature of the record's own list
        }
        return Optional.of(new IRRecordFeatureNav(enr.enumName(), enr.valueName(), record.name(),
                id, type(enr, ws), card(enr, ws), Optionality.PRESENT, enr.sourceRange()));
    }

    /**
     * The head attribute's DECLARED record type — its {@code typeCall}'s referenced
     * {@link RRecordType} ({@code date}/{@code dateTime}/{@code zonedDateTime} per
     * {@code basictypes.rosetta}); null for a null/unresolved/non-record declared type. The
     * {@link #declaredDataTypeOf} sibling for the #507 record arm.
     */
    private static RRecordType declaredRecordTypeOf(RAttribute attr) {
        if (attr == null || attr.typeCall() == null) {
            return null;
        }
        return attr.typeCall().referencedType().orElse(null) instanceof RRecordType record
                ? record : null;
    }

    /**
     * #529 — the head attribute's declared-record feature by name: the
     * {@link #declaredRecordTypeOf} record's own feature list checked for the leaf (the
     * #444 membership rule); null when the head declares no record or the record carries no
     * such feature. The #529 record-leaf leg's gate (the itemHeadGate census's
     * {@code leaf:recHit:<record>} read, BY the same calls).
     */
    private static RRecordFeature recordLeafOf(RAttribute headAttr, String leafName) {
        RRecordType rec = declaredRecordTypeOf(headAttr);
        if (rec == null || leafName == null) {
            return null;
        }
        for (RRecordFeature f : rec.features()) {
            if (leafName.equals(f.name())) {
                return f;
            }
        }
        return null;
    }

    /**
     * #509 arm-B4 — mirror of legacy {@code NavigationHandler.findChoiceSuperOption} (the PR
     * #207 witnessDrop lever): resolve a navigate-by-type choice-option step whose target
     * option lives on the receiver type's CHOICE supertype. Walks the data-supertype chain
     * (each level may carry its own {@code choiceSuperType()}) and matches the feature name
     * against the choice options by TYPE NAME (the option's {@code typeCall} IS its identity).
     * Returns a synthetic {@link RAttribute} carrying the option's deep-copied
     * {@link RTypeCall} and copied metadata annotation refs — EXACTLY the legacy lever's own
     * projection (single-parent contract: fresh nodes, never the option's own subtree) — or
     * {@code null} when no choice supertype carries the option.
     */
    private static RAttribute findChoiceSuperOption(RDataType type, String featureName) {
        if (type == null || featureName == null) {
            return null;
        }
        Set<RDataType> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        for (RDataType t = type; t != null && visited.add(t); t = t.superType().orElse(null)) {
            RChoice choice = t.choiceSuperType().orElse(null);
            if (choice == null) {
                continue;
            }
            for (RChoiceOption opt : choice.options()) {
                RTypeCall tc = opt.typeCall();
                if (tc != null && featureName.equals(tc.typeName())) {
                    return choiceOptionAsAttribute(opt);
                }
            }
        }
        return null;
    }

    /**
     * The choice-option → synthetic-attribute projection ({@link #findChoiceSuperOption}'s
     * own build, factored at #522 so the DIRECT-choice channel of
     * {@link #adaptNonAttrBareItemNav} shares the single source): the option's deep-copied
     * {@link RTypeCall} + copied metadata annotation refs — EXACTLY the legacy lever's
     * projection (single-parent contract: fresh nodes, never the option's own subtree).
     */
    private static RAttribute choiceOptionAsAttribute(RChoiceOption opt) {
        RTypeCall tc = opt.typeCall();
        RAttribute attr = new RAttribute();
        attr.setName(tc.typeName());
        attr.setTypeCall(RTypeCall.deepCopy(tc));
        for (RAnnotationRef srcRef : opt.annotationRefs()) {
            RAnnotationRef refCopy = new RAnnotationRef();
            refCopy.setAnnotationName(srcRef.annotationName());
            srcRef.qualifierName().ifPresent(refCopy::setQualifierName);
            attr.annotationRefs().add(refCopy);
        }
        return attr;
    }

    /**
     * The NAME-keyed core of the #505 dispatch-base scope-join ({@link #dispatchBaseInputOf}
     * delegates here): the enclosing function is a dispatch VARIANT, its SAME-FILE base
     * resolves, and the base declares an input with EXACTLY this name — the join legacy
     * applies ({@code HandlerHelper.dispatchBaseOf}, the PR #369 facet) and the fork's linker
     * leaves unbound. Shared by the #505 symbol arm and the #507 record arm (single-source —
     * the two walks cannot drift).
     */
    private RAttribute dispatchBaseInputNamed(RNode site, String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        RFunction enclosing = enclosingFunction(site);
        if (enclosing == null || enclosing.dispatch().isEmpty()) {
            return null;
        }
        RFunction base = enclosing.dispatchBase().orElse(null);
        if (base == null) {
            return null;
        }
        for (RAttribute in : base.inputs()) {
            if (name.equals(in.name())) {
                return in;
            }
        }
        return null;
    }

    /**
     * Mirror of legacy {@code ReferenceHandler.resolveFeatureOnAttribute}: the feature name against
     * the head attribute's DECLARED type — its {@code typeCall}'s referenced {@link RDataType} —
     * walking the supertype chain (legacy {@code HandlerHelper.findAttributeOnDataType}'s
     * cycle-guarded walk, shared via {@link #findAttributeOnDataTypeByName} since #480). Null for a
     * null/unresolved/non-data declared type; the equivalent then carries no resolved feature and
     * declines at {@link #adaptFeatureCall}'s feature gate.
     */
    private static RAttribute resolveFeatureOnDeclaredType(RAttribute receiverAttr, String featureName) {
        RDataType dataType = declaredDataTypeOf(receiverAttr);
        return dataType == null ? null : findAttributeOnDataTypeByName(dataType, featureName);
    }

    /**
     * v3.3 seat 5 (PR #640 round 1, cq NIT-2): the condition-attribute head of a disguised chain re-ranged to the chain's
     * own node - the link-by-link convention of the three sibling acceptances (the raw node's range on BOTH nodes); the
     * synthetic equivalent minted it with {@code SourceRange.NONE}. Every other component is carried unchanged.
     */
    private static IRExpr rerangedImplicitAttrNav(IRExpr receiver, REnumValueRef enr) {
        IRImplicitAttrNav nav = (IRImplicitAttrNav) receiver;
        return new IRImplicitAttrNav(nav.attributeName(), nav.nodeId(), nav.type(), nav.cardinality(), nav.optionality(),
                enr.sourceRange());
    }

    /**
     * #640 arm E: the type the implicit-attribute mint carries - the reference's own engine type (with its meta when the
     * attribute is meta-annotated) when the engine has one, else the attribute channel's read (the #528 shape). ONE
     * reader for the arm and its mirror (the lockstep law).
     */
    private static RMetaAnnotatedType implicitAttrMintType(RSymbolReference ref, RAttribute attr, RWorkspace ws) {
        RMetaAnnotatedType attrType = ws == null ? null : ws.getInferredAttributeType(attr);
        if (isMetaAnnotated(attr) && ws != null) {
            RMetaAnnotatedType refType = type(ref, ws);
            if (refType != null && !refType.isMissing()) {
                return refType;
            }
        }
        return attrType;
    }

    /**
     * #640 (v3.3 seat 4): legacy's CONDITION-INSTANCE reference - the bare reference to the condition method's own
     * parameter ({@code adjustableDates} inside {@code type AdjustableDates}) that {@code ExistenceHandler} /
     * {@code ReferenceHandler} synthesize during a data-rule render and re-enter the compiler with: a PARENTLESS
     * {@link RAttribute} whose {@code typeCall} names the enclosing data type and never resolves, so the #528 mint
     * read it {@code attrOutsideFunction.noFilterExtractBinder.untyped} - the class the blocker probe ranked FIRST on
     * the DATA_RULE seam (9,393 sole claims on DRR 7.3, 1,078 on cdm 5.38.0; probe-s4.print at f22655948). The owner
     * is found through the enclosing {@link RCondition}; the reference must be named as legacy names the parameter
     * (the lower-camel type name, keyword-escaped with the {@code "_"} PREFIX - legacy's
     * {@code HandlerHelper.conditionInstanceName} through {@code JavaNamingUtil.escapeJavaKeyword}: {@code Default} is
     * {@code _default}; PR #640 round 1, cq SF-1: the first cut accepted a trailing underscore legacy never writes).
     * Returns the owner, else null.
     */
    private static RDataType syntheticConditionInstanceOwner(RSymbolReference ref, RAttribute attr) {
        if (attr.parent() != null || attr.typeCall() == null || attr.typeCall().referencedType().isPresent()
                || attr.typeCall().typeName() == null || ref.name() == null) {
            return null;
        }
        RDataType owner = enclosingConditionOwner(ref);
        if (owner == null || owner.name() == null || owner.name().isEmpty()
                || !attr.typeCall().typeName().equals(owner.name())) {
            return null;
        }
        String instanceName = Character.toLowerCase(owner.name().charAt(0)) + owner.name().substring(1);
        String legacyName = javax.lang.model.SourceVersion.isName(instanceName) ? instanceName : "_" + instanceName;
        return ref.name().equals(legacyName) ? owner : null;
    }

    /** #640: the data type whose condition encloses {@code node} (a twin parented on the type itself counts), else null. */
    private static RDataType enclosingConditionOwner(RNode node) {
        RNode cur = node == null ? null : node.parent();
        int depth = 0;
        while (cur != null && depth++ < PARENT_WALK_LIMIT) {
            if (cur instanceof RCondition cond) {
                return cond.parent() instanceof RDataType dt ? dt : null;
            }
            if (cur instanceof RDataType dt) {
                return dt;
            }
            if (cur instanceof RFunction || cur instanceof RRule) {
                return null;
            }
            cur = cur.parent();
        }
        return null;
    }

    /**
     * #640 arm A: the condition-instance reference lowers to the shallow {@link IRConditionInstance} oracle leaf, typed
     * as the owning data type - the containing root renders WHOLE through legacy (the leaf is in the compiler's
     * {@code containsOracleLeaf} / {@code isOracleRootShape} sets), so the render is legacy's own by identity.
     */
    private Optional<IRExpr> adaptSyntheticConditionInstance(RSymbolReference ref, RAttribute attr, NodeId id) {
        RDataType owner = syntheticConditionInstanceOwner(ref, attr);
        if (owner == null) {
            return Optional.empty();
        }
        return Optional.of(new IRConditionInstance(owner.name(), id,
                RMetaAnnotatedType.withNoMeta(new RDataTypeRef(owner)),
                ExpressionCardinality.SINGLE, Optionality.PRESENT, ref.sourceRange()));
    }

    /**
     * #640 arm C: the enclosing condition's OWN attribute by name (supertypes included) - the head of a disguised
     * {@code head -> leaf} chain inside a condition (the linker marks it {@code resolvedInputFeature} with no enclosing
     * function to resolve the head in; 4,256 sole {@code headUnresolved} claims on DRR 7.3). Null outside a condition.
     */
    private static RAttribute conditionOwnerAttribute(RNode node, String name) {
        RDataType owner = enclosingConditionOwner(node);
        return owner == null || name == null ? null : findAttributeOnDataTypeByName(owner, name);
    }

    /**
     * Mirror of legacy {@code HandlerHelper.findAttributeOnDataType}: the attribute name against a
     * data type's own attributes, walking the supertype chain cycle-guarded. Shared by the #478
     * declared-type feature resolution and the #480 arms' identity guards.
     */
    private static RAttribute findAttributeOnDataTypeByName(RDataType dataType, String name) {
        Set<RDataType> visited = new HashSet<>();
        RDataType dt = dataType;
        while (dt != null && visited.add(dt)) {
            for (RAttribute attr : dt.attributes()) {
                if (name.equals(attr.name())) {
                    return attr;
                }
            }
            dt = dt.superType().orElse(null);
        }
        return null;
    }

    /** #480: an attribute's DECLARED data type ({@code typeCall}'s referenced {@link RDataType}), else null. */
    private static RDataType declaredDataTypeOf(RAttribute attr) {
        if (attr == null || attr.typeCall() == null) {
            return null;
        }
        return attr.typeCall().referencedType().orElse(null) instanceof RDataType dataType
                ? dataType : null;
    }

    /**
     * #514: the {@code sourceElementUnresolved} sub-facet (probe-only, composed at both twin
     * seats — the #513 re-bank's decode deepened one level): mirrors
     * {@link #sourceElementDataType}'s FIRST step and names WHERE the walk bottomed for the two
     * dominant source families. The implicit ladder: {@code outer:<Class>} (the body-slice
     * recursion re-bottomed one binder out — the outer source's family) / {@code
     * binderShape.p:<Class>} (an implicit-ish binder OUTSIDE the filter/extract body slice — the
     * walk's own coverage gap, e.g. a then-body) / {@code namedBinder.p:<Class>} / {@code noBind}
     * (no binder, no provable rule input) / {@code resNull} / {@code deep:<Class>} (the pipe
     * resolved to a non-implicit that bottomed). The feature-call ladder: {@code unres} /
     * {@code tcNull} / {@code refMiss} / {@code basic:<name>} / {@code nonData:<Class>} /
     * {@code dataOdd} (the contract-impossible belt — a data-typed feature cannot bottom here).
     * Other families keep the flat class name.
     */
    private static String sourceElementUnresolvedFacet(RExpression source) {
        if (source instanceof RImplicitVariable) {
            RExpression resolved = resolveElidedPipedSource(source);
            if (resolved instanceof RImplicitVariable iv) {
                RInlineFunction outerBinder = nearestInlineFunctionBeforeSwitch(iv);
                if (outerBinder != null
                        && (outerBinder.isImplicit() || outerBinder.paramNames().isEmpty())) {
                    RNode binderParent = outerBinder.parent();
                    if (binderParent instanceof RFilterExpr filter && filter.body() == outerBinder) {
                        return "src:RImplicitVariable.outer:" + (filter.argument() == null
                                ? "null" : filter.argument().getClass().getSimpleName());
                    }
                    if (binderParent instanceof RExtractExpr extract
                            && extract.body() == outerBinder) {
                        return "src:RImplicitVariable.outer:" + (extract.argument() == null
                                ? "null" : extract.argument().getClass().getSimpleName());
                    }
                    return "src:RImplicitVariable.binderShape.p:" + (binderParent == null
                            ? "orphan" : binderParent.getClass().getSimpleName());
                }
                if (outerBinder != null) {
                    return "src:RImplicitVariable.namedBinder.p:" + (outerBinder.parent() == null
                            ? "orphan" : outerBinder.parent().getClass().getSimpleName());
                }
                return "src:RImplicitVariable.noBind";
            }
            return resolved == null ? "src:RImplicitVariable.resNull"
                    : "src:RImplicitVariable.deep:" + resolved.getClass().getSimpleName()
                            + sourceShapeQualifier(resolved, 0);
        }
        if (source instanceof RFeatureCall fc) {
            RAttribute f = fc.resolvedFeature().orElse(null);
            if (f == null) {
                return "src:RFeatureCall.unres";
            }
            if (f.typeCall() == null) {
                return "src:RFeatureCall.tcNull";
            }
            RNode referenced = f.typeCall().referencedType().orElse(null);
            if (referenced == null) {
                return "src:RFeatureCall.refMiss";
            }
            if (referenced instanceof RDataType) {
                return "src:RFeatureCall.dataOdd";
            }
            return referenced instanceof RBasicType basic
                    ? "src:RFeatureCall.basic:" + basic.name()
                    : "src:RFeatureCall.nonData:" + referenced.getClass().getSimpleName();
        }
        return "src:" + source.getClass().getSimpleName() + sourceShapeQualifier(source, 0);
    }

    /**
     * #521: the structural bottom-qualifier (probe-only — the #514 facet deepened one more
     * level, the facet-first decode law): the flat {@code src:}/{@code deep:} class names
     * hid WHERE the recursive derivation actually bottoms (the probe1 residue — the
     * deep-then/extract shapes), so the qualifier names the recursion-relevant child
     * SHAPE up to three levels ({@code .b:} the pipe/extract body, {@code .a:} the
     * op argument, {@code .named} the named-extract gate, {@code .sym:} the bound symbol
     * class, {@code .iv} an interior elided implicit). Purely structural — no derivation
     * re-runs, no behavior; the census tokens refine in place and the teach reads the
     * named class at the next probe.
     */
    private static String sourceShapeQualifier(RExpression source, int depth) {
        if (source == null || depth >= 3) {
            return "";
        }
        if (source instanceof RThenExpr then) {
            RExpression body = then.body().map(RInlineFunction::body).orElse(null);
            return ".b:" + (body == null ? "noBody"
                    : body.getClass().getSimpleName() + sourceShapeQualifier(body, depth + 1));
        }
        if (source instanceof RExtractExpr extract) {
            RInlineFunction bodyFn = extract.body();
            if (bodyFn == null || !(bodyFn.isImplicit() || bodyFn.paramNames().isEmpty())) {
                return ".named";
            }
            RExpression body = bodyFn.body();
            return ".b:" + (body == null ? "noBody"
                    : body.getClass().getSimpleName() + sourceShapeQualifier(body, depth + 1));
        }
        if (source instanceof RListOpExpr listOp) {
            RExpression arg = listOp.argument();
            return "." + listOp.op() + ".a:" + (arg == null ? "null"
                    : arg.getClass().getSimpleName() + sourceShapeQualifier(arg, depth + 1));
        }
        if (source instanceof RMinExpr minOp) {
            RExpression arg = minOp.argument();
            return ".a:" + (arg == null ? "null"
                    : arg.getClass().getSimpleName() + sourceShapeQualifier(arg, depth + 1));
        }
        if (source instanceof RMaxExpr maxOp) {
            // the min twin (the Copilot #521 R1 symmetry point — the wave taught the
            // ordering ops as a pair, so the qualifier names both bottoms).
            RExpression arg = maxOp.argument();
            return ".a:" + (arg == null ? "null"
                    : arg.getClass().getSimpleName() + sourceShapeQualifier(arg, depth + 1));
        }
        if (source instanceof RDefaultExpr defaultOp) {
            // the wave's other taught leg (the same symmetry class): a default-bottomed
            // residue names BOTH arm classes flat — the join's decline cause can be
            // either arm, and the arm classes alone locate the leg to chase.
            RExpression defL = defaultOp.left().orElse(null);
            RExpression defR = defaultOp.right().orElse(null);
            return ".l:" + (defL == null ? "null" : defL.getClass().getSimpleName())
                    + ".r:" + (defR == null ? "null" : defR.getClass().getSimpleName());
        }
        if (source instanceof RSymbolReference sym) {
            RNode symbol = sym.symbol().orElse(null);
            return ".sym:" + (symbol == null ? "unbound" : symbol.getClass().getSimpleName());
        }
        if (source instanceof RImplicitVariable) {
            return ".iv";
        }
        return "";
    }

    /**
     * Lowers a bare in-lambda ITEM-feature read — an argument-less {@link RSymbolReference} the
     * linker bound (Cat 9) to an attribute of the enclosing filter/extract lambda's implicit-item
     * element type — by adapting the EXACT equivalent {@link RFeatureCall} legacy
     * {@code ReferenceHandler.synthesizeImplicitItemNavigation} builds for the same node (a fresh
     * synthetic {@link RImplicitVariable} receiver + the resolved feature, parented at the ref's
     * own parent), through {@link #adaptFeatureCall}'s own gates — where the #479
     * {@link #retypedSyntheticFilterExtractItem} admission types the synthetic receiver from the
     * binder SOURCE at the cache boundary (the #478 arm's pattern at the #479-taught base). The
     * equivalence IS the byte argument: under a legacy-rendered claim this bare read reaches
     * {@code handle(RSymbolReference)}'s ladder, which synthesizes that equivalent and routes it
     * through {@code visitFeatureCall} — the LIVE route at population; the #480 witness's
     * {@code bareAttrArm} restatement measured the claimable slice at EXACTLY the
     * {@code bareAttr:itemNav:lowers} pre-sizing (336 = 57/86/138/55), and the post-teach probe
     * re-read is the standing drift meter.
     *
     * <p>The admission, in exit order (each gate mirrors legacy's own precedence or narrows it
     * prove-or-decline):
     * <ul>
     *   <li>the RULE-INPUT precedence ({@link #wouldSynthesizeRuleInputNav} — legacy's ladder
     *       renders the {@code input}-rooted navigation FIRST where it fires) — since #485
     *       refined to the TOP-LEVEL slice only ({@link #hasEnclosingRuleLambda} false: the
     *       {@code MapperS.of(input)} receiver, a render this arm cannot produce); the
     *       IN-LAMBDA slice proceeds — legacy's own receiver there is a synthetic IMPLICIT
     *       (the item ≡ input value identity), the exact equivalent shape the arm lowers,
     *       with the source gates proving or declining it (the #485 rule-input source
     *       arm);</li>
     *   <li>the shortcut name-match precedence (legacy's {@code isAliasReference} fallback — the
     *       adapter's standing {@code collidesWithShortcut} class);</li>
     *   <li>the binder gates ({@link #filterExtractSourceOfArmBinder} — the #479 retype's own:
     *       before-switch, implicit-or-paramless, filter/extract BODY);</li>
     *   <li>the structural element derivation ({@link #sourceElementDataType} — the declared
     *       {@code typeCall} channel over the #479 allowlist shapes, prove-or-decline);</li>
     *   <li>legacy {@code synthesizeImplicitItemNavigation}'s own IDENTITY guard (the bound attr
     *       IS the element type's own feature — a same-named shadow declines);</li>
     *   <li>the delegation ({@link #adaptFeatureCall} on the equivalent — the #479 retype + every
     *       standing receiver/feature gate);</li>
     *   <li>the raw node's own cached type (the #279/Cat-9 bind — the emitter's witness source;
     *       an untypeable node declines).</li>
     * </ul>
     * The rebuild re-stamps the lowered receiver and hop with the RAW node's source range — the
     * #480 correlation key the emitter's synthetic-item oracle renderer resolves the lambda
     * binding from (the raw node is a PARSED tree member; the fresh equivalent is not) — and the
     * raw node's cached type on the hop (the #478 cache-boundary law: the fresh equivalent reads
     * MISSING through the node-keyed cache).
     */
    private Optional<IRExpr> adaptBareAttrItemNav(RSymbolReference ref, RAttribute attr, NodeId id,
            RWorkspace ws) {
        if (wouldSynthesizeRuleInputNav(ref, attr) && !hasEnclosingRuleLambda(ref)) {
            // #485: the TOP-LEVEL rule-input navigation precedence — legacy renders the
            // input-ROOTED receiver there (`MapperS.of(input)`, buildImplicitInputReceiver's
            // non-lambda branch). #514 ARM-2: that render IS producible — the #497 orphan-input
            // arm lowers the `input` receiver to IRVariable{PARAM,"input"} (the proven Wave-0
            // render at 2,210 drr RULE events) and the hop is the standing FieldAccess compose
            // (the #501 isTwoLinkSpineOverItemBase allowInputParam precedent one link shorter)
            // — so the arm now claims the slice through the SAME throwaway equivalent the
            // 2-link disguised-chain arm builds (the #478 construction convention). The
            // IN-LAMBDA slice keeps proceeding through the source gates below.
            return adaptRuleInputTopNav(ref, attr, id, ws);
        }
        RFunction enclosing = enclosingFunction(ref);
        if (enclosing != null && collidesWithShortcut(enclosing, ref.name())) {
            return Optional.empty(); // legacy renders the alias invocation — defer
        }
        RExpression source = filterExtractSourceOfArmBinder(ref);
        // #510 arm-D2a: the SWITCH-CASE narrowing leg — outside every binder slice, a bare
        // attr inside a TYPE-guard switch case resolves against the CASE-NARROWED subject
        // (legacy's #221 machinery; the walkBindGate census's switchCrossed class, cdm6
        // FUNCTION-dominant). The element type is the case guard's linker-resolved data type
        // (caseNarrowedElementType — the read-only mirror of legacy NavigationHandler
        // .caseNarrowedImplicitType), and the SAME identity guard + equivalent adapt run on
        // it; the equivalent's synthetic receiver falls OUTSIDE the #479 retype slice (the
        // retype's own walk stops at the switch), so the claim mints the IRSynItemNav
        // ORACLE LEAF and renders whole-legacy BY IDENTITY — the cast-var naming, the
        // narrowed-type λ derivation and every hop stay legacy's own.
        RDataType elementType = source != null ? sourceElementDataType(source, 0)
                : caseNarrowedElementType(ref);
        if (elementType == null) {
            return Optional.empty(); // outside the binder/case-narrowing slice, or unprovable — defer
        }
        if (findAttributeOnDataTypeByName(elementType, attr.name()) != attr) {
            return Optional.empty(); // legacy's identity guard — a shadowing bind declines
        }
        Optional<IRExpr> lowered = adaptFeatureCall(implicitItemNavEquivalent(ref, attr), id, ws);
        if (lowered.isEmpty()) {
            return Optional.empty(); // the equivalent's own gates declined
        }
        if (lowered.get() instanceof FieldAccess fa
                && fa.receiver() instanceof IRVariable recv
                && recv.variableKind() == IRVariable.VariableKind.SYNTHETIC_ITEM) {
            RMetaAnnotatedType navType = type(ref, ws);
            if (navType == null || navType.isMissing()) {
                return Optional.empty(); // untypeable — the emitter could not name the witness
            }
            IRVariable rangedRecv = new IRVariable(recv.name(), recv.variableKind(), recv.nodeId(),
                    recv.type(), recv.cardinality(), recv.optionality(), ref.sourceRange());
            return Optional.of(new FieldAccess(rangedRecv, fa.feature(), id, navType,
                    fa.cardinality(), fa.featureCardinality(), fa.optionality(), ref.sourceRange()));
        }
        // #501 C4 (the bareMeta face — the #499 equivalentMetaAccess class CLAIMED): a
        // meta-annotated bare attr's equivalent lowers through the meta arm to
        // IRMetaAccess{item} — re-ranged exactly like the plain hop above (the raw node's range
        // on both nodes; the #480 synthetic-item index already carries attr-bound
        // RSymbolReference claim nodes, so the base correlates through the standing renderer).
        // The render: an interior claim rides its meta-bearing root's #500 oracle-root serve;
        // an AT-ROOT claim (the census read 180) takes the compiler's #501 disguised-meta
        // oracle-root slot (super.visitSymbolReference — the literal legacy line).
        if (lowered.get() instanceof IRMetaAccess ma
                && ma.receiver() instanceof IRVariable metaRecv
                && metaRecv.variableKind() == IRVariable.VariableKind.SYNTHETIC_ITEM) {
            RMetaAnnotatedType navType = type(ref, ws);
            if (navType == null || navType.isMissing()) {
                return Optional.empty(); // untypeable — the consumer gates could not read the node
            }
            IRVariable rangedRecv = new IRVariable(metaRecv.name(), metaRecv.variableKind(),
                    metaRecv.nodeId(), metaRecv.type(), metaRecv.cardinality(),
                    metaRecv.optionality(), ref.sourceRange());
            return Optional.of(new IRMetaAccess(rangedRecv, ma.feature(), ma.metaQualifiers(), id,
                    navType, ma.cardinality(), ma.featureCardinality(), ma.optionality(),
                    ref.sourceRange()));
        }
        // #504 arm-B2 (the attrOutsideFunction.receiverSyntheticItem face, 271 sole at the
        // #503 SOT): the equivalent's receiver fell OUTSIDE the #479 retype slice and the
        // equivalent now lowers to the arm-B IRSynItemNav mint — the bare attr IS that same
        // un-retypeable item nav, re-minted with the RAW node's range (the oracle-root
        // bareSymbolRef serve correlates by range) and the raw node's cached type (the same
        // navType gate the plain leg carries — the twin's navTypeMissing face keeps the
        // untypeable residue).
        if (lowered.get() instanceof IRSynItemNav syn) {
            RMetaAnnotatedType navType = type(ref, ws);
            if (navType == null || navType.isMissing()) {
                return Optional.empty(); // untypeable — the twin's navTypeMissing face
            }
            return Optional.of(new IRSynItemNav(syn.featureName(), id, navType,
                    syn.cardinality(), syn.optionality(), ref.sourceRange()));
        }
        return Optional.empty(); // a non-nav shape outside the accepted forms — defensive
    }

    /**
     * #522 arm-B — the BY-NAME implicit-item bare-nav admission (the residual
     * {@code symbolNotAttribute} classes, 61 sole at the #521 SOT): an argument-less bare
     * name the linker bound to a GLOBAL non-attribute declaration (a data type / choice /
     * record / enumeration / basic type / segment / annotation — legacy
     * {@code ReferenceHandler.handle(RSymbolReference)}'s own admitted-symbol list;
     * {@code RBody} is deliberately ABSENT there, so a body-bound name stays the honest
     * residue face) is, at legacy's render, the enclosing filter/extract/then binder's
     * implicit-ITEM feature read: legacy {@code synthesizeImplicitItemBareNav} re-resolves
     * the NAME on the item's element form — attributes by name, then the
     * choice-SUPER-option projection — and renders the synthesized {@code item -> <name>}
     * through {@code visitFeatureCall}, the LIVE route at population. The fork's linker
     * binds the global where legacy's residual precedence resolves the local feature: the
     * drr {@code pmtDt: date} constructor reads (the {@code segment date} shadowing
     * OtherPayment's own {@code date} — legacy's #346 builtinTypeBoundItemNav class), the
     * drr rule {@code filter qualification = …} reads (the builtin {@code annotation
     * qualification} shadowing EventTimestamp's attribute), and the cdm6 {@code payouts
     * extract [ PerformancePayout exists ]} navigate-by-type reads (the bare name selects
     * a DIRECT-choice option by TYPE NAME — the #521-A2 idiom at the BARE-symbol seat).
     *
     * <p>The arm is {@link #adaptBareAttrItemNav}'s BY-NAME sibling: it adapts the EXACT
     * equivalent legacy builds ({@link #implicitItemNavEquivalent}) through
     * {@link #adaptFeatureCall}'s own gates, with legacy's BY-NAME decline ladder in place
     * of the Cat-9 identity guard — every leg prove-or-decline:
     * <ul>
     *   <li>the symbol-class gate (legacy's admitted-symbol list verbatim);</li>
     *   <li>the only-exists exclusion (those elements render from the element's own
     *       strings — the #486/#502 precedence, the adapter-walk-only shape);</li>
     *   <li>the closure-param name decline (a lambda-local read — legacy declines the
     *       synthesis first);</li>
     *   <li>the function-scope decline ({@link #nameResolvesInFunctionScope} — an
     *       input/output/alias name keeps legacy's variable/alias seats; population ~0
     *       here because the fork's linker prefers locals, the pure safety mirror);</li>
     *   <li>the binder gates ({@link #filterExtractSourceOfArmBinder} — the bound twin's
     *       own slice) + the structural element derivation
     *       ({@link #sourceElementDataType}), with the DIRECT-choice channel
     *       ({@link #declaredDirectChoiceOf} + the option-by-TYPE-NAME read) where the
     *       element form IS a choice — the #521 by-name leg's two channels restated at
     *       the bare seat;</li>
     *   <li>the by-name resolution ({@link #findAttributeOnDataTypeByName}, then
     *       {@link #findChoiceSuperOption} — legacy's own fallback order);</li>
     *   <li>the delegation ({@link #adaptFeatureCall} on the equivalent) and the bound
     *       twin's accepted lowered shapes, re-stamped with the RAW node's range;</li>
     *   <li>the type channel: the raw node's cached type first (the bound twin's read),
     *       else the resolved attribute's inferred type, else the PROVEN option element
     *       ({@code withNoMeta(RDataTypeRef)} — the #521 elem-proven-typing pattern; the
     *       choice projection deep-copies the option's typeCall WITHOUT resolution state,
     *       so the proven element is read from the ATTACHED original, the #521 Copilot-R2
     *       law). Untypeable declines — the honest residue.</li>
     * </ul>
     * The byte story is the serve route: the accepted lowerings are the bound twin's own
     * (FieldAccess/IRMetaAccess over the synthetic item — the #479/#480 proven composes —
     * and the IRSynItemNav ORACLE LEAF, whose containing roots render whole-legacy), and
     * every oracle serve re-renders the RAW bare name through legacy's OWN ladder —
     * byte-identical BY IDENTITY.
     *
     * <p>#524 — the EMPTY-symbol admission: legacy's dispatch admits {@code expr.symbol()
     * .isEmpty()} into the SAME ladder (the {@code handle(RSymbolReference)} gate lists the
     * empty symbol FIRST, before the seven type-node classes — the #506 admission-lag law
     * at the last unmirrored member), so a truly-UNRESOLVED bare name joins the arm: the
     * parse-time in-lambda reads the linker left EMPTY (the lambdaSrcUnprovable.global
     * spellings — the facet's own walk never ran the DIRECT-choice channel, so the
     * {@code payouts extract InterestRatePayout exists} unbracketed option reads spelled
     * "unprovable" while THIS ladder proves them) and the render-time SYNTHESIZED
     * disguised-nav receivers (the symbolUnresolved.synthetic.itemAttr[Meta] faces — the
     * #486 up-only-parented mechanism; the parent walks run on the scope-parented node
     * exactly as legacy's own re-entrant render does). The #524 census (iaGate) read the
     * whole 55-event pool BY CALL against legacy's ladder: every claimable face fires the
     * SAME synthesis this arm mirrors, and the equivalents lower to the bound twin's own
     * accepted shapes. Also since #524: the {@code scheme}/{@code reference} short-form
     * leg over meta-wrapper items (legacy's #348 {@code metaPathShortForm} — the #522
     * OBS-3 named gap) mints the {@link IRQualifierItemNav} ORACLE LEAF through
     * {@link #adaptFeatureCall}'s own qualifier gates.
     */
    private Optional<IRExpr> adaptNonAttrBareItemNav(RSymbolReference ref, RNode symbol,
            NodeId id, RWorkspace ws) {
        if (!(symbol == null || symbol instanceof RDataType || symbol instanceof RChoice
                || symbol instanceof RRecordType || symbol instanceof REnumeration
                || symbol instanceof RBasicType || symbol instanceof RSegmentDef
                || symbol instanceof RAnnotation)) {
            return Optional.empty(); // legacy's variable path (RBody & co) — the honest residue
        }
        String name = ref.name();
        BareItemResolution res = bareItemMemberResolution(ref);
        if (res == null) {
            return Optional.empty(); // a decline gate fired — legacy's ladder order
        }
        RAttribute attr = res.attr();
        RDataType provenOption = res.provenOption();
        RChoice provenChoice = res.provenChoice();
        if (attr == null) {
            // #524 — the metaPathShortForm leg (legacy #348, the #522 OBS-3 named gap): a
            // bare `scheme`/`reference` read over a META-WRAPPER item has no model element
            // form (the structural walk AND the DIRECT-choice channel both null — legacy's
            // own itemType==null branch position), and legacy synthesizes the SAME
            // unresolved feature-call shape the parser produces for an explicit
            // `item -> scheme` (receiver = synthetic implicit item, resolvedFeature
            // deliberately EMPTY — the render seat resolves the qualifier). The adapter's
            // OWN qualifier gates carry the meta proof (the binder source's TERMINAL
            // resolved feature meta-annotated + the name a qualifier it carries — the
            // adaptFeatureCall mint's prove-or-decline), so the leg lowers to the
            // IRQualifierItemNav ORACLE LEAF re-stamped with the RAW node's range: every
            // containing root renders whole-legacy BY IDENTITY, the #348 short form
            // legacy's own. A non-qualifier name (or a provable element form) never
            // reaches here — the honest declines above keep legacy's variable path; a
            // DIRECT-choice binder is excluded too (legacy's gm-aware walk types the
            // choice PROJECTION, so its own short-form leg never runs there).
            if (res.elementType() == null && res.directChoice() == null
                    && ("scheme".equals(name) || "reference".equals(name))) {
                Optional<IRExpr> msfLowered =
                        adaptFeatureCall(msfItemNavEquivalent(ref, name), id, ws);
                if (msfLowered.isPresent() && msfLowered.get() instanceof IRQualifierItemNav q) {
                    return Optional.of(new IRQualifierItemNav(q.qualifierName(), id, q.type(),
                            q.cardinality(), q.optionality(), ref.sourceRange()));
                }
            }
            return Optional.empty(); // no exact-name feature on the element form — legacy's variable path
        }
        Optional<IRExpr> lowered = adaptFeatureCall(implicitItemNavEquivalent(ref, attr), id, ws);
        if (lowered.isEmpty()) {
            return Optional.empty(); // the equivalent's own gates declined
        }
        RMetaAnnotatedType navType = type(ref, ws);
        if ((navType == null || navType.isMissing()) && ws != null) {
            navType = ws.getInferredAttributeType(attr);
        }
        if ((navType == null || navType.isMissing()) && provenOption != null) {
            navType = RMetaAnnotatedType.withNoMeta(new RDataTypeRef(provenOption));
        }
        if ((navType == null || navType.isMissing()) && provenChoice != null) {
            // #524: the proven CHOICE element — the engine's own reference form (the
            // TypeDirectedResolver shape), the #521 elem-proven-typing law one type
            // class wider.
            navType = RMetaAnnotatedType.withNoMeta(new RChoiceTypeRef(provenChoice.name(),
                    List.of(), provenChoice));
        }
        if (navType == null || navType.isMissing()) {
            return Optional.empty(); // untypeable — the bound twin's honest residue restated
        }
        if (lowered.get() instanceof FieldAccess fa
                && fa.receiver() instanceof IRVariable recv
                && recv.variableKind() == IRVariable.VariableKind.SYNTHETIC_ITEM) {
            IRVariable rangedRecv = new IRVariable(recv.name(), recv.variableKind(),
                    recv.nodeId(), recv.type(), recv.cardinality(), recv.optionality(),
                    ref.sourceRange());
            return Optional.of(new FieldAccess(rangedRecv, fa.feature(), id, navType,
                    fa.cardinality(), fa.featureCardinality(), fa.optionality(),
                    ref.sourceRange()));
        }
        if (lowered.get() instanceof IRMetaAccess ma
                && ma.receiver() instanceof IRVariable metaRecv
                && metaRecv.variableKind() == IRVariable.VariableKind.SYNTHETIC_ITEM) {
            IRVariable rangedRecv = new IRVariable(metaRecv.name(), metaRecv.variableKind(),
                    metaRecv.nodeId(), metaRecv.type(), metaRecv.cardinality(),
                    metaRecv.optionality(), ref.sourceRange());
            return Optional.of(new IRMetaAccess(rangedRecv, ma.feature(), ma.metaQualifiers(),
                    id, navType, ma.cardinality(), ma.featureCardinality(), ma.optionality(),
                    ref.sourceRange()));
        }
        if (lowered.get() instanceof IRSynItemNav syn) {
            return Optional.of(new IRSynItemNav(syn.featureName(), id, navType,
                    syn.cardinality(), syn.optionality(), ref.sourceRange()));
        }
        return Optional.empty(); // a non-nav shape outside the accepted forms — defensive
    }

    /**
     * #529 — the ENR-seat arm for the implicit-item head class (see the call-site comment):
     * the no-channel identity re-checked (the #507 record arm's own first gate —
     * single-source safety), then the bare-head feature-call EQUIVALENT
     * ({@link #enrBareItemNavEquivalent}) adapted through the fc-seat recovery ladder, the
     * CENSUS-NARROW accept re-stamping the two receiver-carrying oracle-leaf mints with the
     * RAW node's id and range (the #524 accepted-shape convention — the inner receiver
     * keeps its as-built ids).
     */
    private Optional<IRExpr> adaptEnrItemHeadNav(REnumValueRef enr, NodeId id, RWorkspace ws) {
        if (enr.resolvedAttributeChain().isPresent() || enr.resolvedInputFeature().isPresent()
                || enr.resolvedChoiceOption().isPresent()
                || enr.resolvedTypeRestriction().isPresent()
                || enr.enumeration().isPresent() || enr.resolvedSymbol().isPresent()) {
            return Optional.empty(); // a bound channel — another arm's shape
        }
        if (enr.enumName() == null || enr.enumName().isEmpty()
                || enr.valueName() == null || enr.valueName().isEmpty()) {
            return Optional.empty(); // the parse-robustness shape
        }
        Optional<IRExpr> lowered = adaptFeatureCall(enrBareItemNavEquivalent(enr), id, ws);
        if (lowered.isEmpty()) {
            return Optional.empty(); // the equivalent's own gates declined — the honest residue
        }
        if (lowered.get() instanceof IRQualifierReceiverNav q) {
            return Optional.of(new IRQualifierReceiverNav(q.receiver(), q.qualifierName(), id,
                    q.type(), q.cardinality(), q.optionality(), enr.sourceRange()));
        }
        if (lowered.get() instanceof IRRecordReceiverNav r) {
            return Optional.of(new IRRecordReceiverNav(r.receiver(), r.featureName(),
                    r.recordTypeName(), id, r.type(), r.cardinality(), r.optionality(),
                    enr.sourceRange()));
        }
        return Optional.empty(); // outside the census-narrow accept — prove-or-decline
    }

    /**
     * #529: the EXACT bare-head equivalent the fc-seat #524 EMPTY-symbol head recovery
     * reads for a disguised single-arrow nav — a fresh un-bound {@link RSymbolReference}
     * head carrying the ENR's head segment + the leaf as the feature name, the CALL
     * parented at the ref's own parent (the #478 throwaway-construction convention — the
     * binder walks see the original tree position) and the HEAD parented at the CALL
     * itself. The head's parent is the RE-ENTRANCY FENCE (the #524 OBS-1 pattern,
     * structural): {@code wouldRequalifyAtParentSeat} keys on the head's IMMEDIATE parent
     * (an equality/arg seat), so a head parented at the synthesized call can never re-enter
     * the requalify machinery — which would re-adapt the enclosing equality's OTHER side,
     * i.e. the ORIGINAL ENR, an unbounded cycle (the probe1 StackOverflowError this fence
     * closes). The ancestor walks only ASCEND, so the binder resolution still reads the
     * original position through the call's parent. Shared by
     * {@link #adaptEnrItemHeadNav} and its {@link #recordArmDeclineGate} twin (the two
     * reads cannot drift).
     */
    private static RFeatureCall enrBareItemNavEquivalent(REnumValueRef enr) {
        RFeatureCall fc = new RFeatureCall();
        RSymbolReference head = new RSymbolReference();
        head.setName(enr.enumName());
        head.setParent(fc);
        fc.setReceiver(head);
        fc.setFeatureName(enr.valueName());
        fc.setParent(enr.parent());
        return fc;
    }

    /**
     * #524: the shared bare-item resolution state — {@code attr} null when the walk ran
     * but no exact-name feature resolved on the element form; the proven option/choice
     * carry the #521/#524 elem-proven typing channels; {@code elementType}/
     * {@code directChoice} expose the walk's own channel verdicts (the MSF leg's gate).
     */
    private record BareItemResolution(RAttribute attr, RDataType provenOption,
            RChoice provenChoice, RDataType elementType, RChoice directChoice) {}

    /**
     * #524: the #522 arm's decline ladder + by-name member resolution factored
     * single-source — {@link #adaptNonAttrBareItemNav} and the fc-seat EMPTY-symbol HEAD
     * recovery both read it (the two seats cannot drift). Null when a DECLINE gate fires
     * (name/only-exists/closure-param/function-scope/binder — legacy's ladder order
     * verbatim); otherwise the walked state.
     */
    private BareItemResolution bareItemMemberResolution(RSymbolReference ref) {
        String name = ref.name();
        if (name == null || name.isEmpty() || hasOnlyExistsElementAncestor(ref)) {
            return null; // only-exists elements render from the element's own strings
        }
        if (isEnclosingClosureParamName(ref, name)) {
            return null; // a lambda-local read — legacy declines the synthesis first
        }
        RFunction enclosing = enclosingFunction(ref);
        if (enclosing != null && nameResolvesInFunctionScope(enclosing, name)) {
            return null; // an input/output/alias name — legacy's variable/alias seats win
        }
        RExpression source = filterExtractSourceOfArmBinder(ref);
        if (source == null) {
            return null; // outside the binder slice — prove-or-decline
        }
        RAttribute attr = null;
        RDataType provenOption = null;
        RChoice provenChoice = null;
        RChoice directChoice = null;
        RDataType elementType = sourceElementDataType(source, 0);
        if (elementType != null) {
            attr = findAttributeOnDataTypeByName(elementType, name);
            if (attr == null) {
                attr = findChoiceSuperOption(elementType, name);
                if (attr != null) {
                    provenOption = choiceSuperOptionElementType(elementType, name);
                    if (provenOption == null) {
                        // #524: the CHOICE-typed option channel — a super-option whose OWN
                        // type is a choice (cdm6 Observable's Asset/Index — the #347 class's
                        // nested-choice members) proves through the choice ref exactly like
                        // the data-typed leg (the #521 elem-proven-typing law widened one
                        // type class; the RDataType leg keeps priority).
                        provenChoice = choiceSuperOptionElementChoice(elementType, name);
                    }
                }
            }
        } else {
            // the DIRECT-choice channel (the #521 by-name leg's other channel at the bare
            // seat): the binder element IS a choice — the name selects an option by TYPE
            // NAME, projected onto the same synthetic attribute the super-option read
            // builds; the proven element reads the ATTACHED original option's typeCall.
            directChoice = declaredDirectChoiceOf(source);
            if (directChoice != null) {
                for (RChoiceOption option : directChoice.options()) {
                    RTypeCall optionCall = option.typeCall();
                    if (optionCall != null && name.equals(optionCall.typeName())) {
                        attr = choiceOptionAsAttribute(option);
                        RNode optRef = optionCall.referencedType().orElse(null);
                        provenOption = optRef instanceof RDataType optType ? optType : null;
                        // #524: the CHOICE-typed option channel at the DIRECT seat too
                        // (the same one-class widening as the super-option leg).
                        provenChoice = optRef instanceof RChoice optChoice ? optChoice : null;
                        break;
                    }
                }
            }
        }
        return new BareItemResolution(attr, provenOption, provenChoice, elementType,
                directChoice);
    }

    /**
     * #522: mirror of legacy {@code ReferenceHandler.nameResolvesInFunctionScope} — the
     * function-scope names (inputs, the output, shortcuts) legacy's bare-item ladder
     * declines BEFORE the item-type resolution, keeping every green function-local
     * reference on the variable/alias paths.
     */
    private static boolean nameResolvesInFunctionScope(RFunction func, String name) {
        for (RAttribute input : func.inputs()) {
            if (name.equals(input.name())) {
                return true;
            }
        }
        if (func.output().isPresent() && name.equals(func.output().get().name())) {
            return true;
        }
        for (RShortcut shortcut : func.shortcuts()) {
            if (name.equals(shortcut.name())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Lowers an in-lambda disguised ITEM-feature CHAIN — a bound-chain {@link REnumValueRef}
     * ({@code head -> leaf}) whose head is an attribute of the enclosing filter/extract lambda's
     * implicit-item element type — by adapting the EXACT 2-hop equivalent legacy
     * {@code ReferenceHandler.synthesizeImplicitItemChain} builds for the same node, through
     * {@link #adaptFeatureCall}'s own gates (the #479 retype at the synthetic base + the #479
     * attribute-channel hop typing). The decline ladder mirrors legacy's own in source order
     * (closure-param head / function-scope head with the #389 self-shadow-shortcut exemption /
     * the rule-input chain precedence via {@link #wouldSynthesizeRuleInputChain}) and then narrows
     * prove-or-decline (the filter/extract binder gates, the structural element derivation, the
     * bound-head identity guard with the bound leaf — or the by-name leaf against the head's
     * DECLARED data type; a choice-projected or record-leaf head is not provable and declines,
     * staying legacy's). The #480 witness's {@code chainArm} restatement measured the claimable
     * slice at EXACTLY the {@code chainFall:itemChain:lowers} pre-sizing (370 = 39/50/229/52).
     * The rebuild re-stamps the whole spine (base + head hop + top) with the RAW node's source
     * range (the emitter's correlation key) and the raw node's cached type on the top hop.
     */
    private Optional<IRExpr> adaptImplicitItemChain(REnumValueRef enr, NodeId id, RWorkspace ws) {
        if (enr.resolvedSymbol().isPresent()) {
            // #480 MF-1: legacy handle(REnumValueRef) renders the RULE/FUNCTION-receiver
            // navigation whenever resolvedSymbol is present — BEFORE the item-chain arm — and
            // the typing engine's chain-bind deliberately leaves resolvedSymbol in place on the
            // documented head-name collision shape (an item attribute AND a workspace func/rule
            // sharing the name). #501 C2: the precedence CLAIM — the class lowers to the
            // DISTINCT shallow IRSymbolNav kind (the census read 1,819 node-unit, 100% typed,
            // 100% interior; the render is the containing root's oracle-root serve — legacy's
            // own branch-selection ladder runs whole, byte-identical BY IDENTITY).
            return adaptSymbolReceiverNav(enr, id, ws);
        }
        String headName = enr.enumName();
        String leafName = enr.valueName();
        if (headName == null || headName.isEmpty() || leafName == null || leafName.isEmpty()) {
            return Optional.empty();
        }
        if (isEnclosingClosureParamName(enr, headName)) {
            // #505 arm-A1: the closure-param-head chain CLAIMED (the attributeChain
            // .closureParamHead face — 441 sole at the #504 SOT, the family's top gate; the
            // #505 enumSeatGate census read the dominant slice twData:<Type>.leafHit — the
            // binder-source walk PROVES the param's element type and the VALUE segment
            // resolves by name on it). `p -> leaf` where the head names a real Java lambda
            // var: legacy's ladder renders the navigation off the param binding — never a
            // re-root — so the mint is FieldAccess{IRClosureParam} (the #502 kind, an ORACLE
            // LEAF): every containing claim root renders through the compiler's oracle-root
            // serve (incl. the #505 enumChain leg at the converted seat) — the literal
            // legacy line, the param binding + every hop coercion legacy's own, byte-
            // identical BY IDENTITY. The unprovable/miss/meta-leaf residue declines with its
            // own composed faces (the twin restates).
            return adaptClosureParamHeadChain(enr, headName, leafName, id, ws);
        }
        RFunction enclosing = enclosingFunction(enr);
        RShortcut selfShadow = enclosingShortcut(enr);
        boolean headIsSelfShortcut = selfShadow != null && headName.equals(selfShadow.name());
        if (enclosing != null && !headIsSelfShortcut
                && (resolveNameInFunction(enclosing, headName) != null
                        || collidesWithShortcut(enclosing, headName))) {
            return Optional.empty(); // a function-scope head roots at the variable/alias render
        }
        if (wouldSynthesizeRuleInputChain(enr)) {
            // #501 C1: the rule-input chain precedence CLAIMED — adapt the EXACT input-rooted
            // equivalent legacy synthesizeImplicitInputChain builds (the census read 1,232
            // node-unit with 1,077 equivalents lowering: top-level roots at the #497 ORPHAN
            // synthetic input, in-lambda roots at the synthetic item — both proven render
            // classes).
            return adaptDisguisedInputChain(enr, id, ws);
        }
        RExpression source = filterExtractSourceOfArmBinder(enr);
        // #510 arm-D2a: the SWITCH-CASE narrowing leg at the CHAIN seat — the disguised
        // `head -> leaf` inside a TYPE-guard switch case resolves its head against the
        // CASE-NARROWED subject (legacy caseNarrowedDisguisedLeaf's own ladder: head by name
        // on the narrowed type, leaf on the head's declared type — exactly the gates below);
        // the equivalent falls OUTSIDE the #479 retype slice, so the claim lowers to the
        // IRSynItemNav-based spine and renders whole-legacy BY IDENTITY (the #505 seat
        // conversion's accepted form).
        RDataType elementType = source != null ? sourceElementDataType(source, 0)
                : caseNarrowedElementType(enr);
        if (elementType == null) {
            return Optional.empty(); // outside the binder/case-narrowing slice, or unprovable — defer
        }
        RAttribute headAttr = findAttributeOnDataTypeByName(elementType, headName);
        if (headAttr == null) {
            return Optional.empty(); // legacy's own head gate — not the element type's feature
        }
        REnumValueRef.AttributeChain chain = enr.resolvedAttributeChain().orElse(null);
        RAttribute boundHead = chain == null ? null : chain.attributeOpt().orElse(null);
        RAttribute leafAttr;
        if (boundHead != null) {
            if (boundHead != headAttr) {
                return Optional.empty(); // legacy's identity guard — a shadowing bind declines
            }
            leafAttr = chain.feature();
        } else {
            RDataType headType = declaredDataTypeOf(headAttr);
            leafAttr = headType == null ? null : findAttributeOnDataTypeByName(headType, leafName);
        }
        if (leafAttr == null) {
            return Optional.empty(); // incl. legacy's record-leaf + choice-projection classes — defer
        }
        Optional<IRExpr> lowered = adaptFeatureCall(implicitItemChainEquivalent(enr, headAttr, leafAttr),
                id, ws);
        if (lowered.isEmpty()) {
            return Optional.empty(); // the equivalent's own gates declined
        }
        if (lowered.get() instanceof FieldAccess top
                && top.receiver() instanceof FieldAccess headHop
                && headHop.receiver() instanceof IRVariable recv
                && recv.variableKind() == IRVariable.VariableKind.SYNTHETIC_ITEM) {
            RMetaAnnotatedType navType = type(enr, ws);
            if (navType == null || navType.isMissing()) {
                return Optional.empty(); // untypeable — the emitter could not name the witness
            }
            IRVariable rangedRecv = new IRVariable(recv.name(), recv.variableKind(), recv.nodeId(),
                    recv.type(), recv.cardinality(), recv.optionality(), enr.sourceRange());
            FieldAccess rangedHead = new FieldAccess(rangedRecv, headHop.feature(), headHop.nodeId(),
                    headHop.type(), headHop.cardinality(), headHop.featureCardinality(),
                    headHop.optionality(), enr.sourceRange());
            return Optional.of(new FieldAccess(rangedHead, top.feature(), id, navType,
                    top.cardinality(), top.featureCardinality(), top.optionality(), enr.sourceRange()));
        }
        // #501 C4 (the chainMeta faces — the #500 equivalentMetaAccess class CLAIMED): a
        // meta-bearing 2-link spine over the synthetic item — IRMetaAccess{FieldAccess{item}}
        // (metaLeaf, the meta arm's lowering of a meta LEAF), FieldAccess{IRMetaAccess{item}}
        // (metaHead — the #500 consumer admission lowers the plain hop over the meta head) or
        // IRMetaAccess{IRMetaAccess{item}} (metaBoth) — re-ranged link-by-link exactly like the
        // plain spine above (the raw node's range on every node — the emitter's correlation
        // key; the #480 synthetic-item index already carries bound-chain claim nodes, so the
        // base correlates through the standing renderer). The render: an interior claim rides
        // its meta-bearing root's #500 oracle-root serve; the census read the class 100%
        // INTERIOR (atRoot 0). The plain 2-link shape returned above, so this leg serves
        // exactly the meta-bearing residue.
        if (isTwoLinkSpineOverItemBase(lowered.get(), false)) {
            RMetaAnnotatedType navType = type(enr, ws);
            if (navType == null || navType.isMissing()) {
                return Optional.empty(); // untypeable — the emitter could not gate consumers
            }
            return Optional.of(reRangeTwoLinkSpine(lowered.get(), id, navType, enr.sourceRange()));
        }
        // #505 (the equivalentSynItemSpine face RESOLVED — 67 sole at the #504 SOT): the #504
        // arm-B mint broke the plain-spine implication — a claiming equivalent can lower to
        // the IRSynItemNav-BASED spine (FieldAccess{IRSynItemNav}: the un-retypeable head hop
        // minted, the leaf hop composed over it). The #504 arm deliberately EXCLUDED it
        // because the enum family had no oracle dispatch leg (an accepted claim would have
        // declined at the emitter's null slot); the #505 seat conversion INSTALLS that leg
        // (the enumChain oracle serve — the L-109c exclusion retired), so the spine is now
        // accepted and re-ranged exactly like the plain form: the base is an ORACLE LEAF, so
        // every containing root (and the converted seat's own claim) renders whole-legacy,
        // byte-identical BY IDENTITY.
        if (lowered.get() instanceof FieldAccess synTop
                && synTop.receiver() instanceof IRSynItemNav synHead) {
            RMetaAnnotatedType navType = type(enr, ws);
            if (navType == null || navType.isMissing()) {
                return Optional.empty(); // untypeable — the emitter could not gate consumers
            }
            IRSynItemNav rangedHead = new IRSynItemNav(synHead.featureName(), synHead.nodeId(),
                    synHead.type(), synHead.cardinality(), synHead.optionality(),
                    enr.sourceRange());
            return Optional.of(new FieldAccess(rangedHead, synTop.feature(), id, navType,
                    synTop.cardinality(), synTop.featureCardinality(), synTop.optionality(),
                    enr.sourceRange()));
        }
        // #508 arm-A3 (the equivalentMetaAccess face CLAIMED — 137 sole at the #507 SOT): the
        // META twin of the #505 IRSynItemNav-spine acceptance directly above. A meta LEAF over
        // an un-retypeable synthetic-item head lowers as IRMetaAccess{IRSynItemNav} (the #499
        // meta arm over the #504 head mint — the metaHopGate census read the class 100% this
        // ONE shape, 100% q:scheme, all typed), which the 2-link item-base gate excludes (the
        // base is the mint, not the bare item variable). The acceptance re-ranges exactly like
        // the plain synTop form: the base is an ORACLE LEAF and the top is META-BEARING, so
        // every containing root (and the converted seat's own claim) renders whole-legacy —
        // the #500 meta-root routing law — byte-identical BY IDENTITY.
        if (lowered.get() instanceof IRMetaAccess metaTop
                && metaTop.receiver() instanceof IRSynItemNav metaSynHead) {
            RMetaAnnotatedType navType = type(enr, ws);
            if (navType == null || navType.isMissing()) {
                return Optional.empty(); // untypeable — the emitter could not gate consumers
            }
            IRSynItemNav rangedHead = new IRSynItemNav(metaSynHead.featureName(),
                    metaSynHead.nodeId(), metaSynHead.type(), metaSynHead.cardinality(),
                    metaSynHead.optionality(), enr.sourceRange());
            return Optional.of(new IRMetaAccess(rangedHead, metaTop.feature(),
                    metaTop.metaQualifiers(), id, navType, metaTop.cardinality(),
                    metaTop.featureCardinality(), metaTop.optionality(), enr.sourceRange()));
        }
        return Optional.empty(); // a non-chain shape outside the accepted spines — defensive
    }

    /**
     * #505 arm-A1: lowers a closure-param-head disguised chain — {@code p -> leaf} where the
     * head segment names an enclosing inline function's declared param (a real Java lambda
     * var; legacy's ladder renders the navigation off the param binding, never a re-root) —
     * to {@code FieldAccess} over the DISTINCT shallow {@link IRClosureParam} base (the #502
     * kind, an ORACLE LEAF: no native emitter arm, every containing claim root renders
     * through the compiler's oracle-root serve — the literal legacy line, so the binder's
     * scope machinery and every hop coercion are byte-identical BY IDENTITY). The gates are
     * the #503 arm-B2 walks restated at the CHAIN shape: {@link #closureParamElementType}
     * derives the param's element type from its registering binder's SOURCE (the L-029
     * allowlist + the structural derivation, prove-or-decline) and the VALUE segment resolves
     * by name on it ({@link #findAttributeOnDataTypeByName}); a meta-annotated leaf declines
     * (the meta deref is legacy's own — the #499 meta arm's boundary, mirrored at this
     * mint); the hop type sources from the resolved leaf's ATTRIBUTE channel when the node's
     * own reads MISSING (the #478 cache-boundary law — the #503 closure-param chains' own
     * type-sourcing leg). The base's type is the walk's OWN proven element on the
     * elem-proven legs (#521 — the #491 argNav root-type belt made it load-bearing;
     * render-inert) and the honest MISSING on the metaSrc leg (no plain element fact
     * exists — the L-029 allowlist declined the source); its cardinality is SINGLE (the
     * binder's per-element bind — the walked derivation's own unit).
     */
    private Optional<IRExpr> adaptClosureParamHeadChain(REnumValueRef enr, String headName,
            String leafName, NodeId id, RWorkspace ws) {
        RDataType elem = closureParamElementType(enr, headName);
        if (elem == null) {
            // #508 arm-A4 (the closureParamHead.twNull face CLAIMED — 118 sole at the #507
            // SOT): the element walk's L-029 allowlist declines META-SOURCED binder sources BY
            // DESIGN (the #506-kept aw0tw1 slice — the metaSrcGate census read the class 100%
            // meta-wrapped element lists [metaFc/bareRef-meta/implicit sources] with the VALUE
            // segment resolving PLAIN on the census's own structural read, and the node's OWN
            // engine type present on EVERY row). The mint needs NEITHER the element proof nor
            // the leaf attribute: the render is the containing root's whole-legacy oracle
            // serve (IRClosureParam has no native arm — legacy re-derives the binding and
            // every FieldWithMetaX deref itself, byte-identical BY IDENTITY), and the node's
            // own engine type is the honest consumer channel (the #478 cache-boundary law's
            // richer branch — the engine models the deref, so the type reads the PLAIN leaf).
            // The UNTYPED residue keeps the standing twNull decline (strong-guard-per-leg:
            // this leg's gate is the node's own typing; the proven legs stay byte-unchanged).
            RMetaAnnotatedType ownType = type(enr, ws);
            if (ownType == null || ownType.isMissing()) {
                return Optional.empty(); // the twNull residue — untyped, the walk stays declined
            }
            IRClosureParam metaSrcBase = new IRClosureParam(headName, id.child(0),
                    RMetaAnnotatedType.MISSING, ExpressionCardinality.SINGLE,
                    Optionality.PRESENT, enr.sourceRange());
            ExpressionCardinality ownCard = card(enr, ws);
            // Optionality normalized to PRESENT (the #501 IRSymbolNav / #497 OBS-2 law): no
            // resolved leaf attribute exists to derive it from, and the render never consults
            // it — every serve is the containing root's whole-legacy oracle line.
            return Optional.of(new FieldAccess(metaSrcBase, leafName, id, ownType,
                    ownCard, ownCard, Optionality.PRESENT, enr.sourceRange()));
        }
        RAttribute leafAttr = findAttributeOnDataTypeByName(elem, leafName);
        if (leafAttr == null) {
            return Optional.empty(); // not the element type's feature — the leafMiss residue
        }
        if (isMetaAnnotated(leafAttr)) {
            // #514 ARM-5 (the closureParamHead.metaLeaf face CLAIMED — the decode read the
            // face 100% TYPED): the meta-LEAF twin of the #508 arm-A4 metaSrc leg directly
            // below — the base is the IRClosureParam ORACLE LEAF, so every containing root
            // (and an at-root claim) renders whole-legacy and the FieldWithMetaX deref is
            // legacy's own BY IDENTITY. The gate is the node's OWN engine type ONLY (the
            // checker models the deref, so it reads the PLAIN leaf form — the attribute
            // channel would carry the WRAPPED form and is deliberately NOT consulted); the
            // untyped residue keeps the decline (strong-guard-per-leg).
            RMetaAnnotatedType mlType = type(enr, ws);
            if (mlType == null || mlType.isMissing()) {
                return Optional.empty(); // the metaLeaf.untyped residue — the walk stays declined
            }
            // #521: the base types from the walk's OWN proven element (the #502
            // missing-type convention retired on the PROVEN legs — the #491 argNav
            // root-type belt made the base's type load-bearing, and elem IS the
            // derivation's fact; render-inert, every serve stays the whole-legacy line).
            IRClosureParam mlBase = new IRClosureParam(headName, id.child(0),
                    RMetaAnnotatedType.withNoMeta(new RDataTypeRef(elem)),
                    ExpressionCardinality.SINGLE,
                    Optionality.PRESENT, enr.sourceRange());
            ExpressionCardinality mlCard = isMultiCardinality(leafAttr)
                    ? ExpressionCardinality.MULTI : ExpressionCardinality.SINGLE;
            // The hop optionality = the plain-leaf path's own derivation (the Copilot
            // #514 R1 class sweep — the leaf IS resolved here, unlike the A4 leg).
            return Optional.of(new FieldAccess(mlBase, leafName, id, mlType,
                    mlCard, mlCard, chainOptionality(mlBase, leafAttr), enr.sourceRange()));
        }
        RMetaAnnotatedType navType = type(enr, ws);
        if ((navType == null || navType.isMissing()) && ws != null) {
            navType = ws.getInferredAttributeType(leafAttr);
        }
        if (navType == null || navType.isMissing()) {
            return Optional.empty(); // untypeable — the consumer gates could not read the node
        }
        // #521: the base types from the walk's OWN proven element (the #502 missing-type
        // convention retired on the PROVEN legs — see the metaLeaf twin above).
        IRClosureParam base = new IRClosureParam(headName, id.child(0),
                RMetaAnnotatedType.withNoMeta(new RDataTypeRef(elem)),
                ExpressionCardinality.SINGLE,
                Optionality.PRESENT, enr.sourceRange());
        ExpressionCardinality leafCard = isMultiCardinality(leafAttr)
                ? ExpressionCardinality.MULTI : ExpressionCardinality.SINGLE;
        return Optional.of(new FieldAccess(base, leafName, id, navType,
                leafCard, leafCard, chainOptionality(base, leafAttr), enr.sourceRange()));
    }

    /**
     * #501 C2: lowers a disguised SYMBOL-receiver navigation — the {@code resolvedSymbol}-present
     * bound-chain shape legacy {@code handle(REnumValueRef)} serves through its RULE/FUNCTION
     * receiver branches (the #480 MF-1 precedence this arm previously yielded to) — to the
     * DISTINCT shallow {@link IRSymbolNav} kind (the #499 distinct-kind law: no native consumer
     * gate admits it silently; the #501 admissions name it, and every containing Java claim root
     * renders through the compiler's oracle-root serve — the literal legacy line, so the whole
     * branch-selection ladder incl. the bare-RRule gate and the meta-leaf ladder stays legacy's
     * own, byte-identical BY IDENTITY). The gates restate legacy's own branch guards
     * (RRule/RFunction symbol + both names present) plus the typing gate every shallow kind
     * carries (the consumer gates read cardinality/type off the node).
     */
    private Optional<IRExpr> adaptSymbolReceiverNav(REnumValueRef enr, NodeId id, RWorkspace ws) {
        RNode symbol = enr.resolvedSymbol().orElse(null);
        String symbolKind = symbol instanceof RRule ? "rule"
                : symbol instanceof RFunction ? "function" : null;
        if (symbolKind == null) {
            return Optional.empty(); // outside legacy's RULE/FUNCTION receiver branches — residue
        }
        if (enr.enumName() == null || enr.enumName().isEmpty()
                || enr.valueName() == null || enr.valueName().isEmpty()) {
            return Optional.empty(); // legacy's own branch guards — both names present
        }
        RMetaAnnotatedType navType = type(enr, ws);
        if (navType == null || navType.isMissing()) {
            return Optional.empty(); // untypeable — the consumer gates could not read the node
        }
        // Optionality is NORMALIZED to PRESENT for the whole kind — the #500 IRPipe/IROnlyExists
        // shallow-kind convention and the #497 OBS-2 law (a SERIALIZED fact must not split on
        // per-node happenstance): the render never consults it (the kind has no leaf-emitter
        // arm — every serve is the containing root's whole-legacy oracle line, where legacy
        // derives its own null-safety from the raw chain), so the class-level constant is the
        // honest serialized fact rather than a per-node derivation nothing consumes.
        return Optional.of(new IRSymbolNav(symbolKind, id, navType, card(enr, ws),
                Optionality.PRESENT, enr.sourceRange()));
    }

    /**
     * #501 C1: lowers a rule-input disguised chain — the {@code wouldSynthesizeRuleInputChain}
     * precedence this arm previously yielded to — by adapting the EXACT input-rooted 3-hop
     * equivalent legacy {@code synthesizeImplicitInputChain} builds
     * ({@link #disguisedInputChainEquivalent} — the {@code buildImplicitInputReceiver} split
     * mirrored: rule-body TOP LEVEL roots at the #497 ORPHAN synthetic {@code input}
     * [{@code isSyntheticRuleInputRef} — the {@code MapperS.of(input)} render], IN-LAMBDA roots
     * at the synthetic implicit item [the #479/#480 item render]) through
     * {@link #adaptFeatureCall}'s own gates, then re-ranging the whole spine with the raw
     * node's range (the emitter's correlation key — the #480 pattern; the meta-bearing spines
     * ride the same {@link #reRangeTwoLinkSpine} the chainMeta leg uses). The census read the
     * equivalents lowering 1,077 of 1,232 (FieldAccess 645 + IRMetaAccess-rooted 432); the
     * declines (the in-lambda synthetic item outside the taught retype slice) keep composed
     * residue faces at the twin.
     */
    private Optional<IRExpr> adaptDisguisedInputChain(REnumValueRef enr, NodeId id, RWorkspace ws) {
        REnumValueRef.AttributeChain chain = enr.resolvedAttributeChain().orElse(null);
        RAttribute boundHead = chain == null ? null : chain.attributeOpt().orElse(null);
        RAttribute chainLeaf = chain == null ? null : chain.feature();
        RRule rule = enclosingRule(enr);
        RTypeCall fromTypeCall = rule == null ? null : rule.fromType().orElse(null);
        if (boundHead == null || chainLeaf == null || fromTypeCall == null) {
            return Optional.empty(); // the gate proved the bound head + from-type — defensive
        }
        Optional<IRExpr> lowered = adaptFeatureCall(
                disguisedInputChainEquivalent(enr, boundHead, chainLeaf, fromTypeCall), id, ws);
        if (lowered.isEmpty()) {
            return Optional.empty(); // the equivalent's own gates declined
        }
        if (isTwoLinkSpineOverItemBase(lowered.get(), true)) {
            RMetaAnnotatedType navType = type(enr, ws);
            if (navType == null || navType.isMissing()) {
                return Optional.empty(); // untypeable — the emitter could not name the witness
            }
            return Optional.of(reRangeTwoLinkSpine(lowered.get(), id, navType, enr.sourceRange()));
        }
        // #513 (the ruleInputChain.shapeGate face RESOLVED — 28 sole at the #512 SOT; the
        // #513 shape facet read the class 100% ONE form, inner:IRSynItemNav): the #505
        // IRSynItemNav-spine acceptance mirrored at THIS arm — the equivalent's head hop
        // lowered to the #504 un-retypeable synthetic-item mint (the in-lambda rule-input
        // roots outside the taught retype slice), and the leaf hop composed over it. The
        // acceptance re-ranges exactly like the #505 form: the base is an ORACLE LEAF, so
        // the converted seat's claim (and every containing root) renders whole-legacy
        // through the enumChain serve, byte-identical BY IDENTITY.
        if (lowered.get() instanceof FieldAccess synTop
                && synTop.receiver() instanceof IRSynItemNav synHead) {
            RMetaAnnotatedType navType = type(enr, ws);
            if (navType == null || navType.isMissing()) {
                return Optional.empty(); // untypeable — the emitter could not gate consumers
            }
            IRSynItemNav rangedHead = new IRSynItemNav(synHead.featureName(), synHead.nodeId(),
                    synHead.type(), synHead.cardinality(), synHead.optionality(),
                    enr.sourceRange());
            return Optional.of(new FieldAccess(rangedHead, synTop.feature(), id, navType,
                    synTop.cardinality(), synTop.featureCardinality(), synTop.optionality(),
                    enr.sourceRange()));
        }
        // #513 probe3: the META twin (probe2 read the shapeGate residue 6 — 100% the
        // IRMetaAccess{IRSynItemNav} form the FieldAccess acceptance above misses): the
        // #508 arm-A3 acceptance mirrored at THIS arm — a meta LEAF over the un-retypeable
        // synthetic-item head, re-ranged identically; the base is an ORACLE LEAF and the
        // top is META-BEARING, so the seat's claim renders whole-legacy through the
        // enumChain serve (the #500 meta-root routing law), byte-identical BY IDENTITY.
        if (lowered.get() instanceof IRMetaAccess metaTop
                && metaTop.receiver() instanceof IRSynItemNav metaSynHead) {
            RMetaAnnotatedType navType = type(enr, ws);
            if (navType == null || navType.isMissing()) {
                return Optional.empty(); // untypeable — the emitter could not gate consumers
            }
            IRSynItemNav rangedHead = new IRSynItemNav(metaSynHead.featureName(),
                    metaSynHead.nodeId(), metaSynHead.type(), metaSynHead.cardinality(),
                    metaSynHead.optionality(), enr.sourceRange());
            return Optional.of(new IRMetaAccess(rangedHead, metaTop.feature(),
                    metaTop.metaQualifiers(), id, navType, metaTop.cardinality(),
                    metaTop.featureCardinality(), metaTop.optionality(), enr.sourceRange()));
        }
        return Optional.empty(); // an off-spine shape — the shapeGate residue (zero live at probe2)
    }

    /**
     * The EXACT equivalent legacy {@code synthesizeImplicitInputChain} +
     * {@code buildImplicitInputReceiver} build for a rule-input disguised chain — the receiver
     * split on {@link #hasEnclosingRuleLambda}: TOP LEVEL = the synthetic
     * {@code RSymbolReference("input")} resolved to a DETACHED synthetic attribute carrying the
     * rule's from-type (name {@code "input"}, parent never wired, typeCall present — exactly
     * the #497 {@link #isSyntheticRuleInputRef} orphan-input class, so the head lowers to the
     * proven {@code IRVariable{PARAM, "input"}}); IN-LAMBDA = a fresh synthetic
     * {@link RImplicitVariable} (legacy's item ≡ input value identity). Every node parented at
     * the ref's own parent (legacy's own throwaway construction on this exact render path).
     * Shared by {@link #adaptDisguisedInputChain} and its {@link #implicitItemChainDeclineGate}
     * twin.
     */
    private static RFeatureCall disguisedInputChainEquivalent(REnumValueRef enr, RAttribute headAttr,
            RAttribute leafAttr, RTypeCall fromTypeCall) {
        RExpression receiver;
        if (hasEnclosingRuleLambda(enr)) {
            RImplicitVariable item = new RImplicitVariable();
            item.setSynthetic(true);
            item.setParent(enr.parent());
            receiver = item;
        } else {
            RAttribute syntheticInput = new RAttribute();
            syntheticInput.setName("input");
            syntheticInput.setTypeCall(fromTypeCall);
            RSymbolReference inputRef = new RSymbolReference();
            inputRef.setName("input");
            inputRef.setParent(enr.parent());
            inputRef.setResolvedSymbol(syntheticInput);
            receiver = inputRef;
        }
        RFeatureCall headCall = new RFeatureCall();
        headCall.setReceiver(receiver);
        headCall.setFeatureName(headAttr.name());
        headCall.setResolvedFeature(headAttr);
        headCall.setParent(enr.parent());
        RFeatureCall leafCall = new RFeatureCall();
        leafCall.setReceiver(headCall);
        leafCall.setFeatureName(leafAttr.name());
        leafCall.setResolvedFeature(leafAttr);
        leafCall.setParent(enr.parent());
        return leafCall;
    }

    /**
     * #501 — whether a lowered chain equivalent is a 2-link navigation spine
     * ({@link FieldAccess}/{@link IRMetaAccess} links in either combination) over the accepted
     * base: an {@code IRVariable{SYNTHETIC_ITEM}} (the item-rooted equivalents), or — when
     * {@code allowInputParam} — the {@code IRVariable{PARAM, "input"}} the #497 orphan-input arm
     * lowers (the rule-top input-rooted equivalents). The shape gate for the C1/C4 re-range legs.
     */
    private static boolean isTwoLinkSpineOverItemBase(IRExpr node, boolean allowInputParam) {
        if (!(node instanceof FieldAccess || node instanceof IRMetaAccess)) {
            return false;
        }
        IRExpr inner = spineReceiver(node);
        if (!(inner instanceof FieldAccess || inner instanceof IRMetaAccess)) {
            return false;
        }
        IRExpr base = spineReceiver(inner);
        return base instanceof IRVariable v
                && (v.variableKind() == IRVariable.VariableKind.SYNTHETIC_ITEM
                        || (allowInputParam && v.variableKind() == IRVariable.VariableKind.PARAM
                                && "input".equals(v.name())));
    }

    /** The receiver of a navigation spine link ({@link FieldAccess} or {@link IRMetaAccess}). */
    private static IRExpr spineReceiver(IRExpr link) {
        return link instanceof FieldAccess fa ? fa.receiver() : ((IRMetaAccess) link).receiver();
    }

    /**
     * #513: the spine-shape facet (probe-only, the twin channel) — names the first
     * off-{@link #isTwoLinkSpineOverItemBase} form of a lowered chain, outermost-first:
     * {@code top:<Class>} (the top link is not a spine link), {@code inner:<Class>} (the
     * inner is not), or {@code base:<Class>[.<VariableKind>]} (both links spine-shaped, the
     * base off-item).
     */
    private static String spineShapeFacet(IRExpr lowered) {
        if (!(lowered instanceof FieldAccess || lowered instanceof IRMetaAccess)) {
            return "top:" + lowered.getClass().getSimpleName();
        }
        IRExpr inner = spineReceiver(lowered);
        if (!(inner instanceof FieldAccess || inner instanceof IRMetaAccess)) {
            return "inner:" + inner.getClass().getSimpleName();
        }
        IRExpr base = spineReceiver(inner);
        return "base:" + base.getClass().getSimpleName()
                + (base instanceof IRVariable bv ? "." + bv.variableKind() : "");
    }

    /**
     * #501 — re-stamp a 2-link navigation spine with the RAW claim node's range on every node
     * (the #480 re-range pattern generalized over {@link FieldAccess}/{@link IRMetaAccess}
     * links): the base variable, the inner link (own id/type kept) and the top link (the claim's
     * {@code id} + the raw node's cached {@code navType} — the #480 top-hop convention).
     */
    private static IRExpr reRangeTwoLinkSpine(IRExpr top, NodeId id, RMetaAnnotatedType navType,
            SourceRange range) {
        IRExpr inner = spineReceiver(top);
        IRVariable base = (IRVariable) spineReceiver(inner);
        IRVariable rangedBase = new IRVariable(base.name(), base.variableKind(), base.nodeId(),
                base.type(), base.cardinality(), base.optionality(), range);
        IRExpr rangedInner = rebuildSpineLink(inner, rangedBase, null, null, range);
        return rebuildSpineLink(top, rangedInner, id, navType, range);
    }

    /**
     * Rebuild one spine link over a new receiver with the re-range stamp; a non-null
     * {@code idOrKeep}/{@code typeOrKeep} replaces the link's own (the top-link convention).
     */
    private static IRExpr rebuildSpineLink(IRExpr link, IRExpr newReceiver, NodeId idOrKeep,
            RMetaAnnotatedType typeOrKeep, SourceRange range) {
        if (link instanceof FieldAccess fa) {
            return new FieldAccess(newReceiver, fa.feature(),
                    idOrKeep != null ? idOrKeep : fa.nodeId(),
                    typeOrKeep != null ? typeOrKeep : fa.type(),
                    fa.cardinality(), fa.featureCardinality(), fa.optionality(), range);
        }
        IRMetaAccess ma = (IRMetaAccess) link;
        return new IRMetaAccess(newReceiver, ma.feature(), ma.metaQualifiers(),
                idOrKeep != null ? idOrKeep : ma.nodeId(),
                typeOrKeep != null ? typeOrKeep : ma.type(),
                ma.cardinality(), ma.featureCardinality(), ma.optionality(), range);
    }

    /**
     * The EXACT equivalent legacy {@code synthesizeImplicitItemNavigation} builds — a fresh
     * synthetic {@link RImplicitVariable} receiver + the resolved feature, both parented at the
     * ref's own parent (the synthesized nodes join no tree — legacy's own throwaway construction
     * on this exact render path). Shared by {@link #adaptBareAttrItemNav} and its
     * {@link #bareAttrItemNavDeclineGate} twin.
     */
    private static RFeatureCall implicitItemNavEquivalent(RSymbolReference ref, RAttribute attr) {
        RImplicitVariable item = new RImplicitVariable();
        item.setSynthetic(true);
        item.setParent(ref.parent());
        RFeatureCall fc = new RFeatureCall();
        fc.setReceiver(item);
        fc.setFeatureName(attr.name());
        fc.setResolvedFeature(attr);
        fc.setParent(ref.parent());
        return fc;
    }

    /**
     * #524 — the EXACT equivalent legacy's {@code metaPathShortForm} leg builds (the #348
     * synthesis inside {@code synthesizeImplicitItemBareNav}'s {@code itemType == null}
     * branch): a fresh synthetic {@link RImplicitVariable} receiver + the QUALIFIER name
     * with {@code resolvedFeature} deliberately EMPTY (the render seat resolves the
     * short form), both parented at the ref's own parent. Shared by
     * {@link #adaptNonAttrBareItemNav}'s short-form leg and (through the arm) its twin.
     */
    private static RFeatureCall msfItemNavEquivalent(RSymbolReference ref, String qualifierName) {
        RImplicitVariable item = new RImplicitVariable();
        item.setSynthetic(true);
        item.setParent(ref.parent());
        RFeatureCall fc = new RFeatureCall();
        fc.setReceiver(item);
        fc.setFeatureName(qualifierName);
        fc.setParent(ref.parent());
        return fc;
    }

    /**
     * The EXACT 2-hop equivalent legacy {@code synthesizeImplicitItemChain} builds —
     * {@code item -> head -> leaf} over a fresh synthetic implicit, every node parented at the
     * ref's own parent. Shared by {@link #adaptImplicitItemChain} and its
     * {@link #implicitItemChainDeclineGate} twin.
     */
    private static RFeatureCall implicitItemChainEquivalent(REnumValueRef enr, RAttribute headAttr,
            RAttribute leafAttr) {
        RImplicitVariable item = new RImplicitVariable();
        item.setSynthetic(true);
        item.setParent(enr.parent());
        RFeatureCall headCall = new RFeatureCall();
        headCall.setReceiver(item);
        headCall.setFeatureName(headAttr.name());
        headCall.setResolvedFeature(headAttr);
        headCall.setParent(enr.parent());
        RFeatureCall leafCall = new RFeatureCall();
        leafCall.setReceiver(headCall);
        leafCall.setFeatureName(leafAttr.name());
        leafCall.setResolvedFeature(leafAttr);
        leafCall.setParent(enr.parent());
        return leafCall;
    }

    /**
     * #480: mirrors legacy {@code synthesizeImplicitInputNavigation}'s admission — the rule-input
     * precedence the bare-attr arm must yield to (an enclosing rule with a data-type from-type
     * whose identity-matched feature IS the bound attribute; legacy renders the {@code input}
     * navigation there, never the item navigation). Since #485 the arm yields only on the
     * TOP-LEVEL slice ({@link #hasEnclosingRuleLambda} false — the {@code MapperS.of(input)}
     * receiver): legacy's IN-LAMBDA receiver is a synthetic IMPLICIT (the
     * {@code buildImplicitInputReceiver} lambda branch — the item ≡ input value identity), the
     * same equivalent shape the arm lowers.
     */
    private static boolean wouldSynthesizeRuleInputNav(RSymbolReference ref, RAttribute attr) {
        RRule rule = enclosingRule(ref);
        if (rule == null || rule.fromType().isEmpty()) {
            return false;
        }
        return rule.fromType().get().referencedType().orElse(null) instanceof RDataType fromType
                && findAttributeOnDataTypeByName(fromType, attr.name()) == attr;
    }

    /**
     * #514 ARM-2: lowers the TOP-LEVEL bare rule-input navigation — a bare attr read at rule
     * top level where legacy synthesizes the {@code input}-rooted navigation
     * ({@code MapperS.of(input) -> attr}, {@code buildImplicitInputReceiver}'s non-lambda
     * branch) — through the SAME throwaway equivalent the #513 2-link disguised-chain arm
     * builds ({@link #disguisedInputChainEquivalent}'s non-lambda receiver, one link shorter):
     * a synthetic orphan {@code input} attribute carrying the rule's OWN from-type typeCall
     * (the #497 {@link #isSyntheticRuleInputRef} identity), whose reference lowers through the
     * #497 arm to {@code IRVariable{PARAM,"input"}} (the proven Wave-0 render), then the hop
     * through {@link #adaptFeatureCall}'s own gates. The claim accepts exactly the
     * {@code FieldAccess{IRVariable{PARAM,"input"}}} shape (the {@link
     * #isTwoLinkSpineOverItemBase} allowInputParam base one link up), re-stamped with the RAW
     * node's range and cached type (the #478 cache-boundary law — the fresh equivalent reads
     * MISSING through the node-keyed cache; an untypeable raw node declines,
     * prove-or-decline).
     */
    private Optional<IRExpr> adaptRuleInputTopNav(RSymbolReference ref, RAttribute attr, NodeId id,
            RWorkspace ws) {
        RRule rule = enclosingRule(ref);
        if (rule == null || rule.fromType().isEmpty()) {
            return Optional.empty(); // the caller's guard re-checked — defensive
        }
        RMetaAnnotatedType navType = type(ref, ws);
        if (navType == null || navType.isMissing()) {
            return Optional.empty(); // untypeable — the consumer gates could not read the node
        }
        Optional<IRExpr> lowered =
                adaptFeatureCall(ruleInputTopNavEquivalent(ref, attr, rule), id, ws);
        if (lowered.isEmpty()) {
            return Optional.empty(); // the equivalent's own gates declined
        }
        // #514 probe2 pivot: the claim MINTS the shallow IRRuleInputNav oracle leaf for BOTH
        // proven equivalent shapes — the plain hop (FieldAccess over the #497 PARAM leaf) AND
        // the meta-featured hop (IRMetaAccess over it, the probe2-entered offShape 33) — never
        // the native FieldAccess form (the rule cell's native nav render is unproven; the
        // first-cut claims leaked to the emitter decline site, the probe2 catch). The render
        // is the containing root's whole-legacy oracle serve BY IDENTITY — legacy synthesizes
        // the `input` receiver and threads every coercion (FieldWithMetaX included) itself.
        boolean plainHop = lowered.get() instanceof FieldAccess fa
                && fa.receiver() instanceof IRVariable recv
                && recv.variableKind() == IRVariable.VariableKind.PARAM
                && "input".equals(recv.name());
        boolean metaHop = lowered.get() instanceof IRMetaAccess ma
                && ma.receiver() instanceof IRVariable metaRecv
                && metaRecv.variableKind() == IRVariable.VariableKind.PARAM
                && "input".equals(metaRecv.name());
        if (plainHop || metaHop) {
            // Optionality = the hop feature's declared fact (the Copilot #514 R1 class sweep).
            return Optional.of(new IRRuleInputNav(attr.name(), id, navType, card(ref, ws),
                    optionalityOf(attr), ref.sourceRange()));
        }
        return Optional.empty(); // an off-shape lowering — defensive, the twin names it
    }

    /**
     * #514 ARM-2: the shared throwaway equivalent ({@link #disguisedInputChainEquivalent}'s
     * non-lambda receiver, one link shorter) — a synthetic orphan {@code input} attribute
     * carrying the rule's from-type typeCall (the #497 {@link #isSyntheticRuleInputRef}
     * identity, so the receiver lowers through the #497 arm to the PARAM leaf) under the
     * single resolved hop. Shared by {@link #adaptRuleInputTopNav} and its
     * {@link #bareAttrItemNavDeclineGate} twin (the shared-builder law).
     */
    private static RFeatureCall ruleInputTopNavEquivalent(RSymbolReference ref, RAttribute attr,
            RRule rule) {
        RAttribute syntheticInput = new RAttribute();
        syntheticInput.setName("input");
        syntheticInput.setTypeCall(rule.fromType().get());
        RSymbolReference inputRef = new RSymbolReference();
        inputRef.setName("input");
        inputRef.setParent(ref.parent());
        inputRef.setResolvedSymbol(syntheticInput);
        RFeatureCall hopCall = new RFeatureCall();
        hopCall.setReceiver(inputRef);
        hopCall.setFeatureName(attr.name());
        hopCall.setResolvedFeature(attr);
        hopCall.setParent(ref.parent());
        return hopCall;
    }

    /**
     * #497 — whether {@code ref} is a bare read of a rule/report wrapper's SYNTHETIC
     * {@code input} attribute (the {@code RFunction.fromRule}/{@code fromReport} factories'
     * detached input — name {@code "input"}, parent NEVER wired; a genuine model attribute
     * always carries its declaring parent) with a PRESENT typeCall (the from-typed wrapper's
     * input always carries the copied from-type; a typeCall-less orphan is the from-less edge
     * whose {@code MapperS.of(input)} render could not compile — prove-or-decline). The
     * identity is deliberately SELF-CONTAINED — no context walk: the first #497 arm cut gated on
     * {@code enclosingRule} and claimed only the rule-parented slice (239 of the census's
     * class); the residue's witness read the SAME orphan under report-wrapper functions, where
     * the parent chain reaches an {@link RFunction} instead. Legacy's ladder variable-paths the
     * orphan class position-independently ({@code MapperS.of(input)} — every synthesizer branch
     * null-guards on identity against REAL type features, which the orphan can never be), so
     * the arm lowers it to the PARAM variable leaf and the render is the proven Wave-0
     * emission. The OPTIONALITY fact is NORMALIZED to {@code OPTIONAL} at the arm (Seat-1 #497
     * OBS-2 → Copilot R2 suppressed-valid): the two orphan creators differ on populated
     * cardinality (the factory input carries (0..1); {@code
     * ReferenceHandler.buildImplicitInputReceiver}'s twin carries none), and optionality() is
     * serialized — the class must not split on builder happenstance.
     */
    private static boolean isSyntheticRuleInputRef(RAttribute attr) {
        return "input".equals(attr.name()) && attr.parent() == null && attr.typeCall() != null;
    }

    /**
     * #485: restates legacy {@code ReferenceHandler.hasEnclosingRuleLambda} (the
     * {@code buildImplicitInputReceiver} receiver split): an inline function between the node
     * and the enclosing {@link RRule} means the synthesized input-navigation receiver is the
     * lambda's synthetic IMPLICIT, not the {@code MapperS.of(input)} root; stops at the first
     * {@link RRule}/{@link RFunction}.
     */
    private static boolean hasEnclosingRuleLambda(RNode start) {
        RNode cur = start == null ? null : start.parent();
        int depth = 0;
        while (cur != null && depth++ < PARENT_WALK_LIMIT) {
            if (cur instanceof RInlineFunction) {
                return true;
            }
            if (cur instanceof RRule || cur instanceof RFunction) {
                return false;
            }
            cur = cur.parent();
        }
        return false;
    }

    /**
     * #480: mirrors legacy {@code synthesizeImplicitInputChain}'s admission — the rule-input chain
     * precedence the chain arm must yield to (a BOUND head that is identity-matched on the
     * enclosing rule's from-type; legacy roots the chain at {@code input} there).
     */
    private static boolean wouldSynthesizeRuleInputChain(REnumValueRef enr) {
        REnumValueRef.AttributeChain chain = enr.resolvedAttributeChain().orElse(null);
        RAttribute boundHead = chain == null ? null : chain.attributeOpt().orElse(null);
        if (boundHead == null) {
            return false; // the one-segment closure-param shape — legacy's own null case
        }
        RRule rule = enclosingRule(enr);
        if (rule == null || rule.fromType().isEmpty()) {
            return false;
        }
        return rule.fromType().get().referencedType().orElse(null) instanceof RDataType fromType
                && findAttributeOnDataTypeByName(fromType, boundHead.name()) == boundHead;
    }

    /**
     * #480: the filter/extract SOURCE the arms derive from — the raw node's nearest inline fn
     * walked with SWITCH precedence ({@link #nearestInlineFunctionBeforeSwitch} — the #479
     * retype's own walk), admitted only when the binder is an IMPLICIT-or-paramless filter/extract
     * BODY; the returned expression is that filter/extract's argument. #506: the THEN-BODY
     * widening — an implicit/paramless {@link RThenExpr} body binder returns the then's
     * ARGUMENT (the #481 value identity: the then-item takes the WHOLE pipe-argument value, and
     * the walks' element derivation is cardinality-blind — the same law
     * {@link #closureParamElementType} has carried for NAMED then params since #503; the #506
     * census read the {@code bodyNotFilterExtract} class 320-of-368 THEN-bodied with the
     * element derivable + head/leaf resolving on it in the dominant rows). #510: the
     * SINGLE-NAMED filter/extract binders (arm-D2b) and the implicit MIN/MAX key binders
     * (arm-D2c) join the slice — see the leg comments. {@code null} outside the arms' binder
     * slice (sort/reduce/multi-named binders, a named then-body, a switch boundary, no lambda).
     */
    private static RExpression filterExtractSourceOfArmBinder(RNode raw) {
        RInlineFunction binder = nearestInlineFunctionBeforeSwitch(raw);
        if (binder == null) {
            return null;
        }
        boolean implicitish = binder.isImplicit() || binder.paramNames().isEmpty();
        // #510 arm-D2b: a SINGLE-NAMED filter/extract binder joins the slice (the walkBindGate
        // census's named.RExtractExpr class — drr-dominant: `then extract ReportableEvent
        // [ …bare attrs… ]`). The named param IS the item by legacy's own scope semantics
        // (a bare attr resolves against the param's element exactly as against the implicit
        // item), so the ELEMENT derivation is the source's own either way; the RENDER stays
        // legacy's whole (the #357/#364/#367/#375 scope-live machinery runs inside the
        // claims' oracle serves — the arms' equivalents fall OUTSIDE the #479 retype slice
        // for named binders, so they mint the IRSynItemNav oracle leaf, never the native
        // hop compose). The then-body leg keeps its implicit-only scope (zero named-then
        // census rows).
        boolean singleNamed = !implicitish && binder.paramNames().size() == 1;
        RNode parent = binder.parent();
        if (parent instanceof RFilterExpr filter && filter.body() == binder
                && (implicitish || singleNamed)) {
            return filter.argument();
        }
        if (parent instanceof RExtractExpr extract && extract.body() == binder
                && (implicitish || singleNamed)) {
            return extract.argument();
        }
        if (parent instanceof RThenExpr then && then.body().orElse(null) == binder
                && implicitish) {
            return then.argument();
        }
        // #510 arm-D2c: the implicit MIN/MAX key-extractor binders join the slice (the
        // walkBindGate census's other.RMinExpr/other.RMaxExpr class — drr-RULE: `[knockIn,
        // knockOut] min [ trigger -> level ]`). A min/max key lambda ranges the ARGUMENT's
        // elements exactly like a filter body (the result is an element of the source — the
        // Cat-8 law verbatim), so the source is the op's argument; the render stays
        // legacy's whole by the same IRSynItemNav oracle-leaf routing (the #479 retype
        // slice deliberately excludes non-filter/extract parents, so the equivalents mint
        // the leaf, never the native hop compose).
        if (parent instanceof RMinExpr min && min.body().orElse(null) == binder && implicitish) {
            return min.argument();
        }
        if (parent instanceof RMaxExpr max && max.body().orElse(null) == binder && implicitish) {
            return max.argument();
        }
        return null;
    }

    /**
     * #510 arm-D2a: the CASE-NARROWED element type — the read-only adapter mirror of legacy
     * {@code NavigationHandler.caseNarrowedImplicitType} (the #221 case-narrowed subject
     * machinery's own derivation, walk-for-walk): ascend from the node; an
     * {@link RInlineFunction} encountered FIRST means the lambda's own item wins (null — the
     * binder walks own that class); the nearest {@link RSwitchCase} with a NAME-kind guard
     * narrows the subject to the guard's type. The type read is the LINKER's authoritative
     * {@code resolvedGuard} binding alone ({@code GlobalResolutionPass.resolveSwitchGuardType}
     * — the import/alias-aware ladder legacy itself consults first); legacy's
     * {@code gm.resolveTypeByName} fallback is generator-side state the adapter cannot carry,
     * so an unresolved guard declines — prove-or-decline, the residue stays legacy's. A
     * non-data resolution (an enum VALUE guard, a choice) declines the same way: bare-attr
     * resolution against a non-data subject is not the claimed class.
     */
    private static RDataType caseNarrowedElementType(RNode start) {
        RNode cur = start == null ? null : start.parent();
        int depth = 0;
        RSwitchCase scase = null;
        while (cur != null && depth++ < PARENT_WALK_LIMIT) {
            if (cur instanceof RInlineFunction) {
                return null;
            }
            if (cur instanceof RSwitchCase c) {
                scase = c;
                break;
            }
            cur = cur.parent();
        }
        if (scase == null) {
            return null;
        }
        RSwitchCaseGuard guard = scase.guard().orElse(null);
        if (guard == null || guard.kind() != SwitchGuardKind.NAME) {
            return null;
        }
        return guard.resolvedGuard().orElse(null) instanceof RDataType dt ? dt : null;
    }

    /**
     * #481: the elided-PIPE resolver — walk a grammar-elided PIPED implicit to the enclosing
     * THEN's ARGUMENT. Legacy {@code ReferenceHandler.handle(RImplicitVariable)}'s thenBody face
     * serves the pipe input's VALUE through both render channels (the hoisted {@code thenArg}
     * local when the scope binding is live, the enclosing lambda's own binding on the runtime
     * {@code .then(} route — the render decision stays legacy's at emit time), so the implicit's
     * ELEMENT FORM is the argument's own by the value identity — the only fact the widening
     * reads. The walk mirrors the #479 retype's conservatism exactly
     * ({@link #nearestInlineFunctionBeforeSwitch}): a switch before the lambda (legacy's thenArg
     * arm walks past switches when its scope binding is live and only the binding-dead face
     * reaches the #221 case-narrowed subject machinery — liveness is unknowable statically, so
     * the face declines whole, the safe direction), a missing binder (the rule-input /
     * condition-instance
     * top-level faces), a NAMED binder (the #357/#364/#367/#375 scope-live walk-out class), or a
     * non-then implicit binder (the filter/extract item piggy-back) stops the resolution — the
     * implicit returns UNRESOLVED and every caller declines it, prove-or-decline. Chained elided
     * hops resolve iteratively under the same depth bound the structural walks use (a pipe chain
     * strictly ascends, so the walk is finite). Returns the resolved (non-elided) expression, the
     * unresolved implicit itself, or {@code null} for a null pipe argument.
     */
    private static RExpression resolveElidedPipedSource(RExpression source) {
        RExpression cur = source;
        int hops = 0;
        while (cur instanceof RImplicitVariable iv && iv.isSynthetic() && hops++ < 8) {
            RInlineFunction binder = nearestInlineFunctionBeforeSwitch(iv);
            if (binder == null || !(binder.isImplicit() || binder.paramNames().isEmpty())) {
                return cur;
            }
            if (!(binder.parent() instanceof RThenExpr then) || then.body().orElse(null) != binder) {
                return cur;
            }
            cur = then.argument();
        }
        return cur;
    }

    /**
     * #512 arm-B2 — the DEEP PIPE WALK: the TERMINAL resolved feature whose elements a
     * binder source ranges, or {@code null} where the derivation cannot PROVE one
     * (prove-or-decline). The FEATURE-channel sibling of {@link #sourceElementDataType}
     * (the shared #506 walk family — same {@link #SOURCE_WALK_DEPTH_LIMIT} bound, same
     * elided-pipe resolution, same element-form laws), needed because the qualifier proof
     * lives on the defining ATTRIBUTE ({@code entityId string (0..*) [metadata scheme]}),
     * not on the element TYPE (a basic {@code string} carries nothing — the type walk's
     * data-type contract cannot serve it, and the engine types the bound item to the
     * UNWRAPPED element form, so the type-level {@code hasMeta()} is blind here — the
     * {@code itemBindingMetaSourced} precedent). The arms, mirroring the type walk's
     * structure: an elided-pipe implicit resolves through the then-chain
     * ({@link #resolveElidedPipedSource}) and a still-implicit source through its enclosing
     * implicit/paramless filter/extract binder (the #501 binder-recursion sibling); a
     * {@code then} pipe derives on its BODY result (the #482 law); an implicit-or-paramless
     * {@code extract} on its BODY (the #483 law — a NAMED extract declines); a
     * {@code filter} and the strictly element-preserving flat list ops
     * (first/last/distinct/reverse/only-element — the same element INSTANCES, reordered or
     * subsetted) on their ARGUMENT; a resolved feature call IS the terminal; a bare symbol
     * bound to an attribute IS the terminal, and one bound to a shortcut derives on the
     * shortcut's body. Everything else — flatten/sum (element-reshaping), calls, literals,
     * unresolved symbols — declines ({@code null}).
     */
    private static RAttribute terminalBindingSourceFeature(RExpression source, int depth) {
        if (source == null || depth >= SOURCE_WALK_DEPTH_LIMIT) {
            return null;
        }
        if (source instanceof RImplicitVariable) {
            RExpression resolved = resolveElidedPipedSource(source);
            if (resolved instanceof RImplicitVariable iv) {
                RInlineFunction outerBinder = nearestInlineFunctionBeforeSwitch(iv);
                if (outerBinder != null
                        && (outerBinder.isImplicit() || outerBinder.paramNames().isEmpty())) {
                    RNode binderParent = outerBinder.parent();
                    if (binderParent instanceof RFilterExpr filter
                            && filter.body() == outerBinder) {
                        return terminalBindingSourceFeature(filter.argument(), depth + 1);
                    }
                    if (binderParent instanceof RExtractExpr extract
                            && extract.body() == outerBinder) {
                        return terminalBindingSourceFeature(extract.argument(), depth + 1);
                    }
                    // The then-binder leg (the #481 pipe-value identity restated for a
                    // NON-synthetic elided implicit the resolver above leaves in place):
                    // a then-bound implicit IS the pipe value, so the terminal derives on
                    // the then's own argument.
                    if (binderParent instanceof RThenExpr then
                            && then.body().orElse(null) == outerBinder) {
                        return terminalBindingSourceFeature(then.argument(), depth + 1);
                    }
                }
                return null;
            }
            return terminalBindingSourceFeature(resolved, depth + 1);
        }
        if (source instanceof RThenExpr then) {
            RExpression body = then.body().map(RInlineFunction::body).orElse(null);
            return body == null ? null : terminalBindingSourceFeature(body, depth + 1);
        }
        if (source instanceof RExtractExpr extract) {
            RInlineFunction bodyFn = extract.body();
            if (bodyFn == null || !(bodyFn.isImplicit() || bodyFn.paramNames().isEmpty())) {
                return null;
            }
            RExpression body = bodyFn.body();
            return body == null ? null : terminalBindingSourceFeature(body, depth + 1);
        }
        if (source instanceof RFilterExpr filter) {
            return terminalBindingSourceFeature(filter.argument(), depth + 1);
        }
        if (source instanceof RListOpExpr listOp) {
            return switch (listOp.op()) {
                case FIRST, LAST, DISTINCT, REVERSE, ONLY_ELEMENT ->
                    terminalBindingSourceFeature(listOp.argument(), depth + 1);
                case FLATTEN, SUM -> null;
            };
        }
        if (source instanceof RFeatureCall fc) {
            RAttribute resolved = fc.resolvedFeature().orElse(null);
            if (resolved != null) {
                return resolved;
            }
            // The ALIAS-HEAD continuation (the probe2/probe3 catches — the corpus class's
            // dominant source spelling `referenceEntityProduct -> entityId` over alias
            // bodies that are themselves alias-headed chains): a shortcut head carries no
            // declared type for the linker's feature resolution (the #508 fact), so the
            // terminal hop resolves BY NAME against the receiver's derived element form —
            // the type walk's own legs first, and where THEY bottom (the linker-unbound
            // alias-headed chains) this walk's OWN terminal feature hop-by-hop (the
            // receiver's terminal attribute names the receiver's element type, and the hop
            // resolves on it — prove-or-decline at every step; the shared depth bound is
            // the termination guard).
            if (fc.featureName() == null) {
                return null;
            }
            RDataType recvElem = sourceElementDataType(fc.receiver(), depth + 1);
            if (recvElem == null) {
                RAttribute recvTerminal =
                        terminalBindingSourceFeature(fc.receiver(), depth + 1);
                recvElem = recvTerminal == null ? null : declaredDataTypeOf(recvTerminal);
            }
            return recvElem == null ? null
                    : findAttributeOnDataTypeByName(recvElem, fc.featureName());
        }
        if (source instanceof RConditionalExpr cond) {
            // The #487 law at the FEATURE channel: the arms' COMMON terminal by instance
            // identity (both branches of the corpus class end at the SAME attribute
            // declaration — `… -> referenceEntity` under both if-arms); an empty branch
            // derives the genuine one alone (the checker's join-with-NOTHING bottom rules,
            // both mirrors); divergent terminals decline.
            RAttribute thenTerm = terminalBindingSourceFeature(cond.thenBranch(), depth + 1);
            if (thenTerm == null) {
                if (isStructurallyEmptyBranch(cond.thenBranch()) && hasGenuineElse(cond)) {
                    return terminalBindingSourceFeature(cond.elseBranch().orElse(null),
                            depth + 1);
                }
                return null;
            }
            if (!hasGenuineElse(cond)) {
                return thenTerm;
            }
            RAttribute elseTerm =
                    terminalBindingSourceFeature(cond.elseBranch().orElse(null), depth + 1);
            return elseTerm == thenTerm ? thenTerm : null;
        }
        if (source instanceof RSymbolReference sr) {
            if (!sr.args().isEmpty()) {
                // The args-present call (the #501 type-walk leg's mirror): the call's value
                // IS the callee's OUTPUT, so the output attribute is the terminal-defining
                // feature (its declared type carries the hop resolution; its metadata
                // carries the qualifier verdict — an output-declared `[metadata …]` list
                // piped into a binder IS the element-defining feature).
                RNode callSymbol = sr.symbol().orElse(null);
                return callSymbol instanceof RFunction callFn
                        ? callFn.output().orElse(null) : null;
            }
            RNode sym = sr.symbol().orElse(null);
            if (sym instanceof RAttribute attr) {
                return attr;
            }
            // The alias dual path (the #508 seats' own law): a linker-BOUND shortcut OR the
            // name-collision fallback — the corpus's alias references are dominantly
            // UNBOUND (the metaHopGate census read the live rows 100% collision), so the
            // by-name leg is the load-bearing one.
            RShortcut shortcut = sym instanceof RShortcut bound ? bound
                    : sym == null ? shortcutByName(enclosingFunction(sr), sr.name()) : null;
            if (shortcut != null) {
                return terminalBindingSourceFeature(shortcut.expression(), depth + 1);
            }
            // #530 arm-B3b — the RULE-seat bare-name continuation (the #512 disclosed
            // residue TAUGHT: `entityId then filter …` inside a rule-extract body — the
            // census's inRule qMiss rows): an argless bare name the ladder above did not
            // serve — UNBOUND (the live corpus class), or false-bound to a non-attribute
            // non-shortcut root (the attr return and the alias path both sit ABOVE this
            // leg, so a bound-RAttribute or shortcut name never reaches it — the Seat-1
            // #530 OBS-2 scope note) — resolves BY NAME against the enclosing binder's
            // element form, through the SAME dual channel the FC-receiver continuation
            // above reads: the type walk's element derivation first (whose #530-A
            // applied-rule/conditional arms now reach the deep rule pipes), this walk's
            // own terminal + declared type next. Prove-or-decline at every step; the
            // shared depth bound is the termination guard.
            if (sr.args().isEmpty() && sr.name() != null) {
                RExpression bareBinderSrc = filterExtractSourceOfArmBinder(sr);
                if (bareBinderSrc != null) {
                    RDataType bareElem = sourceElementDataType(bareBinderSrc, depth + 1);
                    if (bareElem == null) {
                        RAttribute bareTerminal =
                                terminalBindingSourceFeature(bareBinderSrc, depth + 1);
                        bareElem = bareTerminal == null ? null
                                : declaredDataTypeOf(bareTerminal);
                    }
                    if (bareElem != null) {
                        return findAttributeOnDataTypeByName(bareElem, sr.name());
                    }
                }
            }
            return null;
        }
        if (source instanceof REnumValueRef evr) {
            // The DISGUISED two-segment chain (`h -> ids` / `referenceEntityProduct ->
            // entityId` — the single-arrow parse law: the corpus class's binder sources ARE
            // this shape). The FEATURE-channel mirror of the type walk's own ENR arm: the
            // linker's bound channels first (the chain/input-feature binds — legacy's own
            // resolution), the alias-head leg with the SAME identity guard next (composed
            // with this walk's terminal continuation where the type walk's body derivation
            // bottoms on unbound alias-headed chains), and legacy's FUNCTION-rooted by-name
            // ladder as the tail (resolveDisguisedFeature — never reached by a bound chain).
            if (evr.enumeration().isPresent() || evr.enumName() == null
                    || evr.valueName() == null) {
                return null;
            }
            RNode headSymbol = evr.resolvedSymbol().orElse(null);
            if (headSymbol instanceof RRule || headSymbol instanceof RFunction) {
                if (evr.resolvedAttributeChain().isPresent()) {
                    return evr.resolvedAttributeChain().get().feature();
                }
                return evr.resolvedInputFeature().orElse(null);
            }
            RFunction enclosing = enclosingFunction(evr);
            if (enclosing != null) {
                RShortcut aliasHead = shortcutByName(enclosing, evr.enumName());
                if (aliasHead != null) {
                    RDataType bodyElem =
                            sourceElementDataType(aliasHead.expression(), depth + 1);
                    if (bodyElem == null) {
                        RAttribute bodyTerminal =
                                terminalBindingSourceFeature(aliasHead.expression(),
                                        depth + 1);
                        bodyElem = bodyTerminal == null ? null
                                : declaredDataTypeOf(bodyTerminal);
                    }
                    RAttribute aliasLeaf = bodyElem == null ? null
                            : findAttributeOnDataTypeByName(bodyElem, evr.valueName());
                    RAttribute boundLeaf = evr.resolvedAttributeChain()
                            .map(REnumValueRef.AttributeChain::feature).orElse(null);
                    if (aliasLeaf == null || (boundLeaf != null && boundLeaf != aliasLeaf)) {
                        return null;
                    }
                    return aliasLeaf;
                }
                if (evr.resolvedAttributeChain().isPresent()) {
                    return evr.resolvedAttributeChain().get().feature();
                }
                if (evr.resolvedInputFeature().isPresent()) {
                    return evr.resolvedInputFeature().orElse(null);
                }
                RAttribute head = resolveNameInFunction(enclosing, evr.enumName());
                return head == null ? null
                        : resolveFeatureOnDeclaredType(head, evr.valueName());
            }
            if (enclosingRule(evr) != null) {
                if (evr.resolvedAttributeChain().isPresent()) {
                    return evr.resolvedAttributeChain().get().feature();
                }
                return evr.resolvedInputFeature().orElse(null);
            }
            return null;
        }
        return null;
    }

    /**
     * #530 arm-B3 — the JOIN-LICENSED qualifier verdict: whether a binder source's EVERY
     * contributing element-defining feature carries {@code qualifier} as a metadata
     * qualifier. The first step is {@link #terminalBindingSourceFeature}'s own read (a
     * proven single terminal answers directly — the standing #512-B2 slice, preserved BY
     * CONSTRUCTION); where that walk bottoms, the DIVERGENT-conditional continuation
     * applies the #487 join rules on the LICENSE channel (the drr
     * `(if … then (… extract identifier …) else entityId) then filter (… item -> scheme …)`
     * class — the branch terminals are DIFFERENT attributes, so the instance-identity join
     * correctly nulls, but every element instance comes from ONE branch and the qualifier
     * read is licensed exactly when EACH branch's defining feature carries it — legacy
     * renders the uniform {@code getMeta().getScheme()} deref over the joined
     * FieldWithMetaX stream, the golden's own line): an empty THEN derives on the genuine
     * ELSE alone, a missing else on the THEN alone (the checker's join-with-NOTHING
     * bottoms, both mirrors), and both-genuine branches must EACH license (recursing this
     * same verdict — a nested conditional joins the same way). A non-conditional bottom
     * stays unlicensed (prove-or-decline; a DEEP interior divergence under a then/extract
     * descent keeps the honest decline — the census read the live class 100% at the
     * resolved pipe top). Read-only; the B2 mint is the only consumer.
     */
    private static boolean bindingSourceQualifierLicensed(RExpression source, String qualifier,
            int depth) {
        if (source == null || depth >= SOURCE_WALK_DEPTH_LIMIT || qualifier == null) {
            return false;
        }
        RAttribute terminal = terminalBindingSourceFeature(source, depth);
        if (terminal != null) {
            return isMetaAnnotated(terminal) && metaQualifierNames(terminal).contains(qualifier);
        }
        RExpression resolved = source instanceof RImplicitVariable
                ? resolveElidedPipedSource(source) : source;
        if (!(resolved instanceof RConditionalExpr cond)) {
            return false;
        }
        boolean hasElse = hasGenuineElse(cond);
        if (isStructurallyEmptyBranch(cond.thenBranch()) && hasElse) {
            return bindingSourceQualifierLicensed(cond.elseBranch().orElse(null), qualifier,
                    depth + 1);
        }
        boolean thenLicensed =
                bindingSourceQualifierLicensed(cond.thenBranch(), qualifier, depth + 1);
        if (!hasElse) {
            return thenLicensed;
        }
        return thenLicensed && bindingSourceQualifierLicensed(cond.elseBranch().orElse(null),
                qualifier, depth + 1);
    }

    /**
     * #485: the RULE-INPUT arm's admission — the element data type of a TRUE-noBinder rule-top
     * elided implicit, or {@code null} outside the claim slice (prove-or-decline). The elided
     * implicit at RULE top level takes the RULE INPUT's value: legacy
     * {@code ReferenceHandler.handle(RImplicitVariable)}'s {@code isElidedOperandTopLevel}
     * route renders {@code MapperS.of(input)} — the {@code RFunction.fromRule}-synthesized
     * input parameter, named literally {@code "input"} and typed by the rule's {@code from}
     * type — and the checker types the same face off the rule's from-type
     * ({@code TypeInferenceEngine.computeImplicitItemType}'s top-level branch →
     * {@code inferRuleFromType}, always {@code withNoMeta}): the element form is the rule's
     * declared from-type, a DECLARATION read (no cached-type channel — the #484
     * output-attribute pattern). The admission mirrors legacy's gate ladder EXACTLY:
     * <ul>
     *   <li>synthetic + the {@code isElidedOperandTopLevel} slot gate
     *       ({@link #isElidedOperandArgumentSlot} — the argument of one of the ten
     *       without-left ops; a parsed synthetic implicit always sits there, the gate is the
     *       legacy-precedence restatement);</li>
     *   <li>a CLEAN walk to the {@link RRule} root: a lambda re-binds (legacy's thenArg /
     *       piggy-back arms — though the callers only reach here after
     *       {@link #resolveElidedPipedSource} stopped, the walk re-establishes it), a SWITCH
     *       routes legacy through the #221 case-narrowed subject machinery (liveness
     *       unknowable statically — decline whole, the standing conservatism), an
     *       {@link RFunction} root fails legacy's own gate (a function body's input is not
     *       named {@code input}), an {@link RCondition} root is the condition-instance face
     *       (a DIFFERENT value identity — its own future widening) — every crossing
     *       declines;</li>
     *   <li>the {@code from} type resolves to an {@link RDataType} (the declared-typeCall
     *       channel; the fromRule-synthesized input attribute carries no annotations and the
     *       checker's read is {@code withNoMeta} by construction, so the non-meta proof IS
     *       the admission — no meta seat exists on a rule's {@code from} clause).</li>
     * </ul>
     * RECURSION-FREE — a pure parent walk + declaration read; the #482 termination measure is
     * untouched (the #484 pattern).
     */
    private static RDataType provableRuleInputElementType(RImplicitVariable iv) {
        if (!iv.isSynthetic() || !isElidedOperandArgumentSlot(iv)) {
            return null;
        }
        RNode cur = iv.parent().parent();
        int depth = 0;
        while (cur != null && depth++ < PARENT_WALK_LIMIT) {
            if (cur instanceof RInlineFunction || cur instanceof RSwitchExpr
                    || cur instanceof RFunction || cur instanceof RCondition) {
                return null;
            }
            if (cur instanceof RRule rule) {
                RNode ref = rule.fromType().flatMap(RTypeCall::referencedType).orElse(null);
                return ref instanceof RDataType data ? data : null;
            }
            cur = cur.parent();
        }
        return null;
    }

    /**
     * #485: restates legacy {@code ReferenceHandler.isElidedOperandTopLevel}'s slot gate — the
     * implicit must be the ARGUMENT of one of the ten without-left ops (the elided-operand
     * synthesis seats; AstBuilder mints synthetic implicits only there, so the gate is a
     * legacy-precedence restatement + drift detector, never a live filter).
     */
    private static boolean isElidedOperandArgumentSlot(RImplicitVariable iv) {
        RNode parent = iv.parent();
        return (parent instanceof RListOpExpr op && op.argument() == iv)
                || (parent instanceof RConversionExpr conv && conv.argument() == iv)
                || (parent instanceof RToStringExpr ts && ts.argument() == iv)
                || (parent instanceof RExtractExpr ext && ext.argument() == iv)
                || (parent instanceof RFilterExpr filt && filt.argument() == iv)
                || (parent instanceof RCountExpr count && count.argument() == iv)
                || (parent instanceof RSortExpr sort && sort.argument() == iv)
                || (parent instanceof RMinExpr min && min.argument() == iv)
                || (parent instanceof RMaxExpr max && max.argument() == iv)
                || (parent instanceof RReduceExpr reduce && reduce.argument() == iv);
    }

    /**
     * #480: the STRUCTURAL element data type of an allowlist-shaped source — the defining
     * feature/attribute's DECLARED type (the same declared-{@code typeCall} channel legacy's
     * structural walk reads for these shapes): a feature call's resolved last hop, a bare
     * symbol's attribute, a disguised {@link REnumValueRef} re-derived through legacy's OWN
     * FUNCTION-rooted by-name ladder (the {@code resolveDisguisedFeature} shape: the head against
     * the enclosing function's inputs/output, the leaf on the head's declared type — the linker's
     * chain/input-feature binds are NOT legacy's channel here, and in a RULE legacy's derivation
     * is structurally null, so a rule-scoped disguised source DECLINES — the Seat-1 #480 MF-2
     * recut), an all-IDENTICAL list literal, a filter's inner (element-preserving), and — the
     * #481 widening — a grammar-elided PIPED implicit resolved to the enclosing then's argument
     * ({@link #resolveElidedPipedSource}) and derived THERE — the #482 widening: a NESTED
     * {@link RThenExpr} pipe derived on its BODY result (the element form of {@code A then B}
     * is B's body's own — the value identity one structural seat deeper) — the #483
     * widening: an {@link RExtractExpr} with an implicit-or-paramless body derived on its BODY
     * result (the element form of {@code A extract B} is B's own result per element — the same
     * body-descent law one construct wider; a named extract declines) — the #484 widening:
     * a bare (no-arg) function reference derived on the callee's OUTPUT attribute's declared
     * type (the callable-output identity — the L-109 point-free application's value IS the
     * output; the aliasShadow precedence declines first) — the #485 widening: an
     * UNRESOLVED rule-top elided implicit derived on the enclosing RULE's declared from-type
     * ({@link #provableRuleInputElementType} — the rule-input identity, legacy's
     * {@code MapperS.of(input)} face; the declaration read) — and the #487 widening: an
     * {@link RConditionalExpr} derived as the arms' COMMON form (instance identity — the
     * list-literal all-IDENTICAL law lifted to the two-armed choice; an empty else derives
     * the then form alone, the checker's own {@code join(thenT, NOTHING) = thenT} bottom
     * rule; mixed forms decline). {@code null}
     * where the derivation cannot PROVE a single data type (the arm declines — prove-or-decline;
     * the #479 allowlist's element-form law applied to the TYPE channel).
     *
     * <p>The no-depth entry is public for the #502 census's walk-unification cross-read (the
     * probe-only {@code sourceWalkVerdicts} consumer in the compiler — the #501
     * probe-visibility precedent); no standing-render caller outside this class. The
     * depth-taking overload below stays private (the recursion bound is an implementation
     * detail — the Copilot #502 R1 API-surface point, mirroring the
     * {@link #isProvablyNonMetaElementSource} entry/overload split exactly).
     */
    public static RDataType sourceElementDataType(RExpression source) {
        return sourceElementDataType(source, 0);
    }

    /** The bounded internal form — see the public entry's javadoc. */
    private static RDataType sourceElementDataType(RExpression source, int depth) {
        if (source == null || depth >= SOURCE_WALK_DEPTH_LIMIT) {
            return null;
        }
        if (source instanceof RImplicitVariable) {
            // #481: the elided-pipe widening — the value identity (the implicit IS the pipe
            // input), so derive on the resolved argument. #485: an UNRESOLVED implicit
            // derives through the rule-input arm (the rule's declared from-type — the
            // declaration read; null outside the claim slice, prove-or-decline).
            RExpression resolved = resolveElidedPipedSource(source);
            if (resolved instanceof RImplicitVariable iv) {
                // #501 (the srcElem widening — the census's dominant 793-node face): an implicit
                // bound by an ENCLOSING implicit/paramless filter/extract binder IS that
                // binder's element (the checker's computeImplicitItemType rule — a filter
                // preserves its source's elements, and an extract-BODIED implicit is the
                // extract source's element), so its element form derives on the binder's OWN
                // source — the value identity one binder out, recursing through the same
                // walk (a nested `xs filter [ item extract [ head -> leaf … ] ]` bottoms at
                // the outer source). Outside that binder slice the #485 rule-input arm keeps
                // the tail (prove-or-decline).
                RInlineFunction outerBinder = nearestInlineFunctionBeforeSwitch(iv);
                if (outerBinder != null
                        && (outerBinder.isImplicit() || outerBinder.paramNames().isEmpty())) {
                    RNode binderParent = outerBinder.parent();
                    if (binderParent instanceof RFilterExpr filter && filter.body() == outerBinder) {
                        return sourceElementDataType(filter.argument(), depth + 1);
                    }
                    if (binderParent instanceof RExtractExpr extract && extract.body() == outerBinder) {
                        return sourceElementDataType(extract.argument(), depth + 1);
                    }
                }
                return provableRuleInputElementType(iv);
            }
            return resolved == null ? null : sourceElementDataType(resolved, depth + 1);
        }
        if (source instanceof RThenExpr then) {
            // #482: the nested-pipe widening — the element form of `A then B` is B's BODY
            // result (the value identity one seat deeper), so derive on the body expression;
            // a bodyless then declines.
            RExpression body = then.body().map(RInlineFunction::body).orElse(null);
            return body == null ? null : sourceElementDataType(body, depth + 1);
        }
        if (source instanceof RExtractExpr extract) {
            // #483: the extract-bodied widening — the element form of `A extract B` is B's
            // OWN result per element (the map routes' elements ARE the body's values; the
            // checker types the extract off its body), so derive on the body expression.
            // #521: the NAMED-body descent — the #483 named-extract decline retired: the
            // per-element value identity holds REGARDLESS of the param's naming (`A extract
            // x [ B ]` — B's result per element either way; the probe2 qualifier read the
            // dominant drr residue 100% this shape), and a body reference to the named
            // param derives through the standing #506 cpSym leg (closureParamBinderSource —
            // the scope-live walk-out the #483 conservatism feared resolves through the
            // walk's own binder-source recursion, prove-or-decline as ever). The identity
            // `extract item` face declines through the resolver arm's recursion.
            RInlineFunction bodyFn = extract.body();
            if (bodyFn == null) {
                return null;
            }
            RExpression body = bodyFn.body();
            return body == null ? null : sourceElementDataType(body, depth + 1);
        }
        if (source instanceof RConditionalExpr cond) {
            // #487: the conditional JOIN derivation — the arms' COMMON form by instance
            // identity (the list-literal all-IDENTICAL law lifted to the two-armed choice;
            // mixed forms decline — the #487 decode read that face at ZERO population mass);
            // an EMPTY else derives the THEN form alone (no elements — the checker's own
            // join(thenT, NOTHING) = thenT bottom rule). The formGap face (arms provable,
            // no single data form — basic-typed arms …) correctly derives null here: the
            // retype channel serves it off the source's own cached join type, the nav
            // channels decline — exactly the mixed-list asymmetry the precedent carries.
            RDataType thenT = sourceElementDataType(cond.thenBranch(), depth + 1);
            if (thenT == null) {
                // #506: the EMPTY-THEN mirror of the #487 empty-else rule — a structurally
                // empty then-branch (`if c then empty else Y`) contributes NO elements, so the
                // form derives on the genuine ELSE alone (the checker's join(NOTHING, elseT) =
                // elseT bottom rule, symmetric; the census read the class at the cond.thenNull
                // bottoms). A non-empty underivable then keeps the decline.
                if (isStructurallyEmptyBranch(cond.thenBranch()) && hasGenuineElse(cond)) {
                    return sourceElementDataType(cond.elseBranch().orElse(null), depth + 1);
                }
                return null;
            }
            if (!hasGenuineElse(cond)) {
                return thenT;
            }
            RDataType elseT = sourceElementDataType(cond.elseBranch().orElse(null), depth + 1);
            return elseT == thenT ? thenT : null;
        }
        if (source instanceof RFeatureCall fc) {
            RAttribute resolvedFeature = fc.resolvedFeature().orElse(null);
            if (resolvedFeature != null) {
                return declaredDataTypeOf(resolvedFeature);
            }
            // #521: the unresolved-feature by-name leg — a linker-unbound feature step in
            // element-source position (the cdm6 navigate-by-type shape `payout ->
            // InterestRatePayout`: the name selects a CHOICE OPTION, never a plain
            // attribute, so resolvedFeature is structurally empty) derives by resolving
            // the feature NAME against the receiver's own element form: attributes by
            // name + the choice-SUPER-option projection on a derived data type, and the
            // option-by-TYPE-NAME read on a DIRECT choice-declared receiver (the same
            // projections legacy's receiver walk applies — prove-or-decline; the arm's
            // downstream identity guard keeps a wrong derivation from ever claiming).
            return unresolvedFeatureSourceElementType(fc, depth);
        }
        if (source instanceof RSymbolReference sym) {
            if (!sym.args().isEmpty()) {
                // #501 (the srcElem widening's decisive leg — the census's dominant piped-source
                // shape, `F(args) then extract [head -> leaf …]`, the elided implicit resolving
                // through the pipe to the CALL): the element form of an ARGS-PRESENT function
                // call is the callee's OUTPUT declared type — the #484 callable-output law with
                // the argument list irrelevant to the element form (legacy renders
                // `<callee>.evaluate(<args>)` and the checker types the call off fn.output()
                // either way). The #484 bare-name alias-shadow precedence CANNOT apply here — an
                // alias invocation carries no argument list, so an args-present parse binds the
                // FUNCTION symbol directly.
                // #530 arm-A: the applied RULE-callee leg — the #521 rule-reference law at the
                // APPLIED seat (the pipeCondGate census's whole population: the drr
                // `then extract if … then cde.price.Spread_InterestRate(…) …` conditional
                // branches): the applied value IS the referenced rule's own body result
                // (legacy renders the rule invocation whose output carries the body's value —
                // the callable-output law at the RULE kind, argument list irrelevant exactly
                // as at the FUNCTION leg), so the element form derives on the rule's OWN body
                // expression through this same walk — the depth bound the termination guard
                // (the body is NOT a subtree of the call). Every other callee class declines,
                // prove-or-decline.
                RNode callSymbol = sym.symbol().orElse(null);
                if (callSymbol instanceof RFunction callFn) {
                    return declaredDataTypeOf(callFn.output().orElse(null));
                }
                if (callSymbol instanceof RRule callRule) {
                    return sourceElementDataType(callRule.expression().orElse(null), depth + 1);
                }
                return null;
            }
            RNode symbol = sym.symbol().orElse(null);
            if (symbol instanceof RAttribute attr) {
                return declaredDataTypeOf(attr);
            }
            if (symbol instanceof RFunction calleeFn) {
                // #484: the callable-output widening — a bare (no-arg) function reference in
                // element-source position is the L-109 point-free application, and its applied
                // value IS the callee's OUTPUT (legacy renderImplicitFunctionInvocation; the
                // checker types the bare reference off fn.output()), so the element data type
                // is the OUTPUT attribute's declared type — the same declared-typeCall channel,
                // cardinality-agnostic like the bare-attribute leg. The aliasShadow precedence
                // declines FIRST, mirroring the allowlist arm exactly.
                RFunction enclosing = enclosingFunction(sym);
                if (enclosing != null && collidesWithShortcut(enclosing, sym.name())) {
                    return null;
                }
                return declaredDataTypeOf(calleeFn.output().orElse(null));
            }
            if (symbol instanceof RShortcut shortcut) {
                // #502: the ALIAS leg — a linker-BOUND shortcut reference in element-source
                // position: the alias's value IS its body's (legacy renders aliasName(inputs)
                // whose helper returns the body's Mapper — the value identity through the
                // helper), so the element form derives on the shortcut's OWN body expression.
                // The depth bound is the termination guard here (the body is NOT a subtree of
                // the reference — the #482 structural measure does not apply to this leg;
                // alias-to-alias chains bottom at the bound).
                return sourceElementDataType(shortcut.expression(), depth + 1);
            }
            if (RInlineFunction.declaringLambdaOf(symbol, sym.name()).isPresent()) {
                // #506: the cpSym leg — a NAMED closure param as the element source: the
                // param's value is its registering binder's element (a filter/extract param
                // ranges the source's elements; a then param takes the whole argument value,
                // element ≡ value on the cardinality-blind derivation — the #503-B2 law at
                // the SOURCE seat), so the form derives on the binder's OWN source through
                // this same walk. The depth bound guards the non-structural descent exactly
                // like the alias leg.
                return sourceElementDataType(closureParamBinderSource(sym, sym.name()), depth + 1);
            }
            if (symbol instanceof RRule ruleRef) {
                // #521: the RULE-reference leg — a bare rule reference in element-source
                // position (DRR's rule-chaining pipe idiom: `… then extract <rule>` feeding
                // the next stage): the applied value IS the referenced rule's own body result
                // (legacy renders the rule invocation whose output carries the body's value —
                // the #484 callable-output law at the RULE kind), so the element form derives
                // on the rule's OWN body expression through this same walk. The depth bound is
                // the termination guard (the body is NOT a subtree of the reference — the
                // #502 alias-leg law; rule-to-rule chains bottom at the bound).
                return sourceElementDataType(ruleRef.expression().orElse(null), depth + 1);
            }
            return null;
        }
        if (source instanceof REnumValueRef evr) {
            if (evr.enumeration().isPresent() || evr.enumName() == null || evr.valueName() == null) {
                return null; // legacy's disguised-receiver arm gates on enumeration-EMPTY
            }
            RNode headSymbol = evr.resolvedSymbol().orElse(null);
            if (headSymbol instanceof RRule || headSymbol instanceof RFunction) {
                // the rule/function-invocation-rooted source (legacy's own callable-receiver
                // channel — the head renders as the callee invocation): the element form is the
                // bound nav LEAF's declared type, the same feature legacy's receiver walk
                // resolves by name on the callee's output type.
                if (evr.resolvedAttributeChain().isPresent()) {
                    return declaredDataTypeOf(evr.resolvedAttributeChain().get().feature());
                }
                return declaredDataTypeOf(evr.resolvedInputFeature().orElse(null));
            }
            RFunction enclosing = enclosingFunction(evr);
            if (enclosing != null) {
                RShortcut aliasHead = shortcutByName(enclosing, evr.enumName());
                if (aliasHead != null) {
                    // #506: the ALIAS-HEAD leg — legacy's alias precedence (isAliasReference
                    // renders a shortcut-matching head as the alias invocation BEFORE any
                    // by-name feature resolution), so `<alias> -> <leaf>` derives on the
                    // alias BODY's element with the leaf resolved by name on it (the #502
                    // alias leg composed with the leaf hop — the census's dominant cpTw
                    // bottom, 171 rows mislabeled `metaLeaf` by the aw-centric namer: bound
                    // NON-meta leaves the fn-rooted ladder below cannot see). The identity
                    // guard: a linker-BOUND chain must agree with the by-name resolution
                    // (bound leaf ≡ resolved leaf) — divergence declines, drift-proof.
                    RDataType bodyElem =
                            sourceElementDataType(aliasHead.expression(), depth + 1);
                    RAttribute aliasLeaf = bodyElem == null || evr.valueName() == null ? null
                            : findAttributeOnDataTypeByName(bodyElem, evr.valueName());
                    RAttribute boundLeaf = evr.resolvedAttributeChain()
                            .map(REnumValueRef.AttributeChain::feature).orElse(null);
                    if (aliasLeaf == null || (boundLeaf != null && boundLeaf != aliasLeaf)) {
                        return null;
                    }
                    return declaredDataTypeOf(aliasLeaf);
                }
                if (insideAnyLambda(evr)) {
                    // #506: the FUNCTION-scope in-lambda leg — the probe5 residue's head
                    // class (neither alias nor input/output; the linker's in-lambda
                    // itemType-rooted pass bound the chain, the same resolution the checker
                    // types the node by, so the bound LEAF's declared type is the element
                    // form — the RULE branch's #358 in-lambda leg mirrored at the function
                    // seat; the aw walk's ENR leg has read this bound channel all along,
                    // the #502 walk-unification asymmetry closed). Prove-or-decline: an
                    // unbound in-lambda chain falls through to the by-name ladder below.
                    if (evr.resolvedAttributeChain().isPresent()) {
                        RAttribute inLambdaLeaf = evr.resolvedAttributeChain().get().feature();
                        if (inLambdaLeaf != null) {
                            return declaredDataTypeOf(inLambdaLeaf);
                        }
                    } else if (evr.resolvedInputFeature().isPresent()) {
                        return declaredDataTypeOf(evr.resolvedInputFeature().orElse(null));
                    }
                }
                // legacy resolveDisguisedFeature — FUNCTION-rooted by-name (never the linker's bind)
                RAttribute head = resolveNameInFunction(enclosing, evr.enumName());
                RAttribute leaf = head == null ? null
                        : resolveFeatureOnDeclaredType(head, evr.valueName());
                return declaredDataTypeOf(leaf);
            }
            if (enclosingRule(evr) != null) {
                if (wouldSynthesizeRuleInputChain(evr)) {
                    // the rule-INPUT chain source (legacy's own synthesizeImplicitInputChain
                    // channel — the bound head identity-proven the from-type's feature): the
                    // element form is the bound LEAF's declared type.
                    return declaredDataTypeOf(evr.resolvedAttributeChain().get().feature());
                }
                if (insideAnyLambda(evr)) {
                    // legacy's #358 RULE-gated in-lambda fallback resolves here (the head by
                    // NAME on the enclosing lambda's item type — the same itemType-rooted
                    // resolution the linker's in-lambda bind performs), so the bound LEAF's
                    // declared type is the byte-proven element form.
                    if (evr.resolvedAttributeChain().isPresent()) {
                        return declaredDataTypeOf(evr.resolvedAttributeChain().get().feature());
                    }
                    return declaredDataTypeOf(evr.resolvedInputFeature().orElse(null));
                }
                // the Seat-1 #480 MF-2 face: a rule-TOP-LEVEL disguised source — every leg of
                // legacy's ladder is structurally null there (prove-or-decline: decline).
                return null;
            }
            return null;
        }
        if (source instanceof RListLiteral list) {
            if (list.elements().isEmpty()) {
                return null;
            }
            RDataType common = null;
            for (RExpression element : list.elements()) {
                RDataType elementType = sourceElementDataType(element, depth + 1);
                if (elementType == null || (common != null && elementType != common)) {
                    return null; // unprovable or mixed element types — decline
                }
                common = elementType;
            }
            return common;
        }
        if (source instanceof RFilterExpr filter) {
            return sourceElementDataType(filter.argument(), depth + 1);
        }
        if (source instanceof RConstructorExpr ctor) {
            // #521: the constructor leg — a constructor in element-source position builds
            // instances of EXACTLY its declared type (the checker types the node off its
            // typeCall; legacy renders the builder chain whose value is that type), so the
            // element form IS the constructed data type — the declared-typeCall channel,
            // cardinality-blind like the bare-attribute leg. (The dominant rule-chain
            // bottom: DRR rule bodies end in constructors, so the #521 RULE-reference
            // leg's recursion lands here.)
            return ctor.typeCall() != null
                    && ctor.typeCall().referencedType().orElse(null) instanceof RDataType ctorType
                            ? ctorType : null;
        }
        if (source instanceof RMinExpr minOp) {
            // #521: the min/max element-preserving legs — min and max pick ONE element of
            // their argument (with or without a comparison-key body: the result is an
            // ELEMENT of the receiver either way, the FIRST/ONLY_ELEMENT law at the
            // ordering ops), so the element form is the argument's own — the #502
            // collapse-leg pattern at the two remaining order-op kinds. The
            // value-collapsing ops (sum/…) stay declined.
            return sourceElementDataType(minOp.argument(), depth + 1);
        }
        if (source instanceof RMaxExpr maxOp) {
            return sourceElementDataType(maxOp.argument(), depth + 1);
        }
        if (source instanceof RDefaultExpr defaultOp) {
            // #521: the default-join leg — `a default b` yields a's value when present else
            // b's (the checker joins the operand types), so the element form is the arms'
            // COMMON form by instance identity — the #487 conditional-join law at the
            // default op. Mixed or partially-unprovable forms decline: the value could be
            // EITHER operand's, so both must prove and agree (no empty-branch asymmetry
            // here — `default` has no structurally-empty arm).
            RDataType defLeft = sourceElementDataType(defaultOp.left().orElse(null), depth + 1);
            RDataType defRight = sourceElementDataType(defaultOp.right().orElse(null), depth + 1);
            return defLeft != null && defLeft == defRight ? defLeft : null;
        }
        if (source instanceof RListOpExpr listOp) {
            // #502: the element-form-PRESERVING collapse legs (the census's dominant proof
            // bottoms — thenPipe>listOp.ONLY_ELEMENT/FLATTEN and the direct listOp faces):
            // only-element and first pick ONE element of the argument (the element form is the
            // argument's own — legacy renders the collapse accessor over the same chain), and
            // flatten concatenates inner lists whose element DATA TYPE is the same declared
            // terminal (the declared-typeCall channel is cardinality-blind — the listness lives
            // on the cardinality axis, so the type derivation passes through unchanged).
            // Census-narrow: the three #502 census-live ops; #506 adds DISTINCT (dedup — the
            // surviving elements ARE the argument's own) and LAST (picks ONE element — the
            // FIRST/ONLY_ELEMENT law at the other end), the two ops the #506 op-naming census
            // read at the flat listOp stops (39 + 29 rows). The value-collapsing ops (sum/…)
            // and the remaining order ops (reverse/sort — zero census rows) decline.
            ListOp op = listOp.op();
            if (op == ListOp.ONLY_ELEMENT || op == ListOp.FIRST || op == ListOp.FLATTEN
                    || op == ListOp.DISTINCT || op == ListOp.LAST) {
                return sourceElementDataType(listOp.argument(), depth + 1);
            }
            return null;
        }
        return null;
    }

    /**
     * #521: the unresolved-feature by-name derivation (the {@code src:RFeatureCall.unres}
     * class): resolve {@code fc}'s feature NAME against its receiver's own element form.
     * Two channels, both prove-or-decline: (a) the receiver's element form derives as a
     * DATA type through {@link #sourceElementDataType} — the name resolves as an attribute
     * ({@link #findAttributeOnDataTypeByName}) or a choice-SUPER-option read
     * ({@link #choiceSuperOptionElementType} — the attached-original read, the Copilot
     * #521 R2 point); (b) the receiver's own
     * bound attribute declares a DIRECT {@link RChoice} type — the name selects an option
     * by TYPE NAME (the navigate-by-type step legacy resolves through the choice
     * projection), the option's declared data type the element form. Null anywhere the
     * ladder cannot prove; the bare-attr arm's downstream identity guard additionally
     * re-derives the OUTER attribute by name on the result, so a wrong derivation
     * declines, never claims.
     */
    private static RDataType unresolvedFeatureSourceElementType(RFeatureCall fc, int depth) {
        String featureName = fc.featureName();
        if (featureName == null || featureName.isEmpty() || fc.receiver() == null) {
            return null;
        }
        RDataType receiverElement = sourceElementDataType(fc.receiver(), depth + 1);
        if (receiverElement != null) {
            RAttribute byName = findAttributeOnDataTypeByName(receiverElement, featureName);
            if (byName != null) {
                return declaredDataTypeOf(byName);
            }
            return choiceSuperOptionElementType(receiverElement, featureName);
        }
        RChoice directChoice = declaredDirectChoiceOf(fc.receiver());
        if (directChoice != null) {
            for (RChoiceOption option : directChoice.options()) {
                RTypeCall optionCall = option.typeCall();
                if (optionCall != null && featureName.equals(optionCall.typeName())
                        && optionCall.referencedType().orElse(null) instanceof RDataType optionType) {
                    return optionType;
                }
            }
        }
        return null;
    }

    /**
     * #521 (the Copilot R2 point): the choice-SUPER-option element read on the
     * ORIGINAL option node — the #509 {@link #findChoiceSuperOption} projection
     * deep-copies the option's {@link RTypeCall} WITHOUT resolution state (the
     * deliberate linker-independent contract), so the synthetic attribute's
     * {@code referencedType()} is structurally EMPTY here (no crash — the null id
     * short-circuits before any workspace read — but no PROOF either). This walk
     * reads the attached option's own typeCall instead (the direct-choice channel's
     * read applied at the SUPER seat), prove-or-decline as ever.
     */
    private static RDataType choiceSuperOptionElementType(RDataType type, String featureName) {
        if (type == null || featureName == null) {
            return null;
        }
        Set<RDataType> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        for (RDataType t = type; t != null && visited.add(t); t = t.superType().orElse(null)) {
            RChoice choice = t.choiceSuperType().orElse(null);
            if (choice == null) {
                continue;
            }
            for (RChoiceOption option : choice.options()) {
                RTypeCall optionCall = option.typeCall();
                if (optionCall != null && featureName.equals(optionCall.typeName())
                        && optionCall.referencedType().orElse(null) instanceof RDataType optType) {
                    return optType;
                }
            }
        }
        return null;
    }

    /**
     * #524: the CHOICE-typed twin of {@link #choiceSuperOptionElementType} — the same
     * supertype/choice-super walk, resolving the matched option's OWN type as a
     * {@link RChoice} (cdm6 Observable's Asset/Index — nested-choice options the
     * data-typed leg cannot prove). Same attached-original read; null anywhere the
     * walk cannot prove.
     */
    private static RChoice choiceSuperOptionElementChoice(RDataType type, String featureName) {
        if (type == null || featureName == null) {
            return null;
        }
        Set<RDataType> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        for (RDataType t = type; t != null && visited.add(t); t = t.superType().orElse(null)) {
            RChoice choice = t.choiceSuperType().orElse(null);
            if (choice == null) {
                continue;
            }
            for (RChoiceOption option : choice.options()) {
                RTypeCall optionCall = option.typeCall();
                if (optionCall != null && featureName.equals(optionCall.typeName())
                        && optionCall.referencedType().orElse(null) instanceof RChoice optChoice) {
                    return optChoice;
                }
            }
        }
        return null;
    }

    /**
     * #521: the DIRECT-choice receiver read for the by-name leg — the receiver's own
     * bound attribute (a resolved feature step or a bare argument-less attribute
     * reference) declares a {@link RChoice} type; null for every other receiver shape
     * (prove-or-decline). Split from the by-name leg so the two projection channels
     * (supertype-carried vs directly-declared choice) stay separately readable.
     */
    private static RChoice declaredDirectChoiceOf(RExpression receiver) {
        RAttribute receiverAttr = null;
        if (receiver instanceof RFeatureCall receiverFc) {
            receiverAttr = receiverFc.resolvedFeature().orElse(null);
        } else if (receiver instanceof RSymbolReference receiverSym
                && receiverSym.args().isEmpty()
                && receiverSym.symbol().orElse(null) instanceof RAttribute bound) {
            receiverAttr = bound;
        } else if (receiver instanceof REnumValueRef enr
                && enr.enumeration().isEmpty()
                && enr.enumName() != null && enr.valueName() != null) {
            // the disguised `a -> b` head (the single-arrow parse — the corpus's dominant
            // receiver shape, `economicTerms -> payout` under `… -> <ChoiceOption>`): the
            // bound channels first, then the FUNCTION-rooted by-name ladder — the
            // sourceElementDataType ENR legs' own resolution order, prove-or-decline.
            if (enr.resolvedAttributeChain().isPresent()) {
                receiverAttr = enr.resolvedAttributeChain().get().feature();
            } else if (enr.resolvedInputFeature().isPresent()) {
                receiverAttr = enr.resolvedInputFeature().orElse(null);
            } else {
                RFunction enclosing = enclosingFunction(enr);
                RAttribute head = enclosing == null ? null
                        : resolveNameInFunction(enclosing, enr.enumName());
                receiverAttr = head == null ? null
                        : resolveFeatureOnDeclaredType(head, enr.valueName());
            }
        }
        if (receiverAttr == null || receiverAttr.typeCall() == null) {
            return null;
        }
        return receiverAttr.typeCall().referencedType().orElse(null) instanceof RChoice choice
                ? choice : null;
    }

    /** #480: mirrors legacy {@code isEnclosingClosureParam} (bounded walk, stops at rule/function). */
    private static boolean isEnclosingClosureParamName(RNode start, String name) {
        RNode cur = start == null ? null : start.parent();
        int depth = 0;
        while (cur != null && depth++ < PARENT_WALK_LIMIT) {
            if (cur instanceof RInlineFunction inline && inline.paramNames().contains(name)) {
                return true;
            }
            if (cur instanceof RRule || cur instanceof RFunction) {
                return false;
            }
            cur = cur.parent();
        }
        return false;
    }

    /**
     * #480: the nearest enclosing {@link RShortcut} (the #389 self-shadow exemption's subject),
     * else null.
     *
     * <p>#502 (the Seat-1 MF-1 bound alignment): this walk pairs with the equally-bounded
     * {@link #aliasMatchEnclosingFunction} inside the alias name-match CLAIM gates, and legacy pairs
     * {@code HandlerHelper.findEnclosingFunction}/{@code findEnclosingShortcut} on the SAME
     * container bound (4096) — so the pair can never disagree there. The former 64-hop
     * {@code PARENT_WALK_LIMIT} could: a self-name deeper than 64 parent links inside its own
     * shortcut's body would reach the function but MISS the shortcut, and the claiming arm
     * would over-claim the self-name slice (the #453 divergence class, corpus-absent but
     * constructible). The bound now mirrors legacy's container limit; the RRule/RFunction
     * stops terminate every real walk long before it.
     */
    private static RShortcut enclosingShortcut(RNode start) {
        RNode cur = start == null ? null : start.parent();
        int depth = 0;
        while (cur != null && depth++ < CONTAINER_WALK_LIMIT) {
            if (cur instanceof RShortcut shortcut) {
                return shortcut;
            }
            if (cur instanceof RRule || cur instanceof RFunction) {
                return null;
            }
            cur = cur.parent();
        }
        return null;
    }

    /** #502 (the Seat-1 MF-1 alignment): legacy {@code HandlerHelper.CONTAINER_WALK_LIMIT}'s value. */
    private static final int CONTAINER_WALK_LIMIT = 4096;

    /**
     * #502 (the Copilot R2 pair-alignment point, the MF-1 class one level deeper): the
     * enclosing-function read used by the alias NAME-MATCH claim gate and its two twin
     * restatements, bounded to the SAME container limit as {@link #enclosingShortcut} — legacy
     * bounds BOTH walks at {@code CONTAINER_WALK_LIMIT}, so at >4096 parent hops legacy's
     * name-match fallback sees NO enclosing function and renders no alias; an unbounded
     * function read paired with the bounded shortcut read would re-open the over-claim
     * direction there (function found past the shortcut bound ⇒ the self-name slice claims).
     * One shared helper keeps the pair aligned BY CONSTRUCTION at all three seats. The
     * standing {@link #enclosingFunction} keeps its unbounded form for its other callers (the
     * #486 OBS-1 hygiene note's scope).
     */
    private static RFunction aliasMatchEnclosingFunction(RNode start) {
        RNode cur = start == null ? null : start.parent();
        int depth = 0;
        while (cur != null && depth++ < CONTAINER_WALK_LIMIT) {
            if (cur instanceof RFunction fn) {
                return fn;
            }
            cur = cur.parent();
        }
        return null;
    }

    /** #480: whether the node sits inside ANY inline function (bounded walk, stops at rule/function). */
    private static boolean insideAnyLambda(RNode start) {
        RNode cur = start == null ? null : start.parent();
        int depth = 0;
        while (cur != null && depth++ < PARENT_WALK_LIMIT) {
            if (cur instanceof RInlineFunction) {
                return true;
            }
            if (cur instanceof RRule || cur instanceof RFunction) {
                return false;
            }
            cur = cur.parent();
        }
        return false;
    }

    /** #480: the nearest enclosing {@link RRule} (bounded walk; a function boundary returns null). */
    private static RRule enclosingRule(RNode start) {
        RNode cur = start == null ? null : start.parent();
        int depth = 0;
        while (cur != null && depth++ < PARENT_WALK_LIMIT) {
            if (cur instanceof RRule rule) {
                return rule;
            }
            if (cur instanceof RFunction) {
                return null;
            }
            cur = cur.parent();
        }
        return null;
    }

    /**
     * Lowers a function CALL — an {@link RSymbolReference} carrying arguments whose symbol resolves to a plain
     * {@link RFunction} — to {@link IRApply}: the callee as an {@link IRReference} of kind
     * {@link IRReference.ReferenceKind#FUNCTION} (taking child slot 0, matching {@link IRApply#children()}'s
     * callee-first order) over the recursively-lowered argument subtrees (child slots {@code 1..n}). The emitter
     * renders {@code MapperS.of(<receiver>.evaluate(<unwrapped args>))} where {@code <receiver>} =
     * {@code lowerCamelCase(callee.name())} — the {@code @Inject} field name, a Java-emission decision computed by
     * a compiler-supplied {@code CallReceiverResolver} (the L-029/L-031 split: the neutral node carries only the
     * callee's simple name).
     *
     * <p><strong>The #498 rule-callee leg.</strong> An args-present reference whose symbol resolves to an
     * {@link RRule} — DRR's rule-as-function invocation ({@code <Rule>(<arg>)}), live in the corpus as the
     * {@code RFunction.fromRule}/{@code fromReport} wrapper factories' own synthesized per-field calls (the
     * dominant, type-fixpoint-invisible face the #498 census read) plus the source-level cross-namespace
     * rule-to-rule delegations — lowers on this same arm: the callee FACTS read through the SAME
     * {@link RFunction#fromRule} bridge legacy's own render derives its receiver from (the injected
     * {@code <name>Rule} field, {@code ReferenceHandler}'s #263/#322/#332 path), the shared output/meta/arg
     * gates below run against the bridge, and the callee reference carries kind
     * {@link IRReference.ReferenceKind#RULE} (the bare-delegation arm's own spelling, now with args). The
     * emitter serves the claim ROOT via the whole-reference oracle ({@code RuleApplyRenderer} — the literal
     * legacy fallback), so the gm-aware receiver derivation and arg unwraps stay legacy-side (L-029).
     *
     * <p><strong>The flat inline call.</strong> Claims a call when it renders the flat
     * {@code MapperS.of(name.evaluate(args))} (SINGLE output) or {@code MapperC.<Item>of(name.evaluate(args))} (MULTI
     * output — the wrap + the {@code <Item>} witness/output collision are emitter decisions, reusing the multi-param
     * leaf machinery) with no arg coercion/unwrap/hoist: the symbol is a plain {@code RFunction}
     * ({@link RFunction.Origin#FUNCTION} — a rule/report bridge routes through a different legacy branch and defers);
     * the output is non-meta (a meta output coerces to a {@code FieldWithMetaX} wrapper the witness path cannot name);
     * every callee parameter is non-meta (a meta param threads a meta deref — defer); a MULTI param IS admitted (its
     * arg unwraps with {@code .getMulti()} instead of {@code .get()} — legacy facet {@code tailMulti}, the accessor an
     * emitter decision via {@code CallParamMultiResolver}); and every argument is a byte-safe {@link #isSimpleCallArg}
     * — a scalar/multi param or a literal (unwrapped STRUCTURALLY to the bare {@code evaluate} slot), an alias/shortcut
     * reference (the {@code Mapper} that takes the emitter's {@code .get()}/{@code .getMulti()} fall-through), a
     * qualified enum-value constant (the dotted-constant passthrough), a nested simple call (L-107), or — the #489
     * L-042 revival — a meta-free typed navigation chain rooted at a param / filter-extract item / lowered call
     * ({@link #isNavCallArg}: the SAME chained-{@code Mapper} fall-through render family as the alias leg) — or a
     * filter/extract-bound implicit {@code item} operand (L-108). The
     * alias-into-multi-param case is the multi-PARAM lever: a single alias renders {@code aliasName(inputs).getMulti()}
     * (the single→{@code List} coercion {@code MapperS.getMulti()} provides), a non-meta multi alias the same (legacy's
     * {@code coerceNavigationReceiver} is a no-op on a non-meta item; a meta alias is declined by the compiler's
     * meta-deref guard). Everything else —
     * a meta/untyped or non-admitted-root navigation ({@link #isNavCallArg}'s decline faces, the
     * {@link #argNavFacet} drift detectors), any other lowered form (list-ops, conditionals, operators —
     * the generic {@code arg:<IRClass>} tokens), {@code empty} args, a bare no-arg implicit-input
     * reference — keeps returning {@link Optional#empty()}
     * (→ legacy fallback, byte-green by construction). The receiver name + its collision suffix are emitter (Java)
     * decisions, so this adapter arm stays purely structural.
     */
    private Optional<IRExpr> adaptApply(RSymbolReference ref, NodeId id, RWorkspace ws) {
        RNode rawSymbol = ref.symbol().orElse(null);
        RFunction callee;
        boolean ruleCallee = false;
        if (rawSymbol instanceof RFunction fn && fn.origin() == RFunction.Origin.FUNCTION) {
            callee = fn;
        } else if (rawSymbol instanceof RRule rule) {
            // #498: the args-present rule invocation — the callee facts read through the same
            // fromRule bridge legacy's own receiver derivation uses (see the javadoc leg).
            callee = RFunction.fromRule(rule);
            ruleCallee = true;
        } else if (rawSymbol instanceof RLibraryFunction libraryCallee) {
            // #520 — the LIBRARY-function application (the calleeNotFunction face RESOLVED,
            // 20 sole at the #519 SOT; the calleeGate census read the class 100%
            // RLibraryFunction, 100% atRoot, 100% typeMissing: 7 two-arg [Min/Max] + 3
            // one-arg [IsLeapYear] per cdm FUNCTION cell — the daycount `Min(endDate ->
            // day, 30)` family and the leap-year alias bodies). Lowers to the DISTINCT
            // childless shallow IRLibraryApply (the 43rd IRExprKind — the #502
            // IRMetaOutputApply pattern verbatim): the args are NOT carried (a deep form
            // would invite native argument consumption while the call's render is legacy's
            // own external-function threading), and NO consumer admission names it BY
            // DESIGN — an at-root claim renders through the compiler's oracle-root
            // callArgs dispatch (super.visitSymbolReference — the literal legacy call
            // line), an interior occurrence routes its containing root whole-legacy
            // through containsOracleLeaf, and a drift arrival at the leaf emitter
            // declines at the kind-dispatch end. Unconditional for the symbol class (the
            // type channel is stamped as read — the #491 convention; the census's live
            // class reads MISSING and nothing consumes it at render).
            return Optional.of(new IRLibraryApply(libraryCallee.name(), id,
                    type(ref, ws), card(ref, ws), Optionality.PRESENT, ref.sourceRange()));
        } else {
            return Optional.empty(); // a rule/report bridge, an unresolved symbol, or a non-function callee — defer
        }
        RAttribute output = callee.output().orElse(null);
        if (output == null) {
            return Optional.empty(); // null output (partial resolution) — defer
        }
        if (isMetaAnnotated(output)) {
            // #502 arm-4 — the META-OUTPUT call (the calleeMetaOutput face RESOLVED, 406 sole
            // at the #501 SOT; the census read 382 atRoot / 33 interior node-unit, 100%
            // FUNCTION callees, every argument independently lowerable, six qualifier
            // spellings [scheme 127 · location 108 · reference 94 · address 82 · id 3 ·
            // id+scheme 1]). Lowers to the DISTINCT shallow IRMetaOutputApply (the 23rd
            // IRExprKind — the #499 distinct-kind law): childless (the args are NOT carried —
            // a deep form would invite native argument consumption while the call's VALUE is
            // the meta-wrapped output only legacy can thread), and NO consumer admission
            // names it BY DESIGN — the routing argument, not a corpus claim: an interior
            // occurrence declines its containing claim honestly at the kind-dispatch end (or
            // the containing root serves whole through its standing oracle slot), while a
            // claim ROOT renders through the compiler's oracle-root callArgs dispatch
            // (super.visitSymbolReference — the literal legacy call line with the
            // FieldWithMetaX coercion intact). Census-narrow: FUNCTION callees only (the
            // bridged RULE callee keeps the standing defer); the calleeMetaParam sibling
            // face below keeps declining.
            if (ruleCallee) {
                return Optional.empty();
            }
            return Optional.of(new IRMetaOutputApply(callee.name(), id,
                    type(ref, ws), card(ref, ws), Optionality.PRESENT, ref.sourceRange()));
        }
        // #528 arm-4 — THE META-PARAM ARMS-DEAD RECUT (the #527 law at the meta seat). The
        // pre-gate here used to defer every call with a {@code [metadata …]}-annotated INPUT
        // on the premise that "a meta param coerces to a FieldWithMetaX wrapper / meta-deref".
        // The opResGate census asked legacy BY CALL instead of assuming, and legacy's own
        // source says the opposite — a meta-annotated callee param makes its ENTIRE arg
        // machinery INERT, every branch keyed on the param declining in the same direction
        // (ReferenceHandler, read at source): the arm-B2 receiver coercion + the #342 witness
        // follow fire only when {@code detectMetaKind(param) == NONE}; the meta-input wrapper
        // deref declines for a meta param ("a meta-expecting param takes the wrapper RAW");
        // the #349 elementwise multi deref declines for a meta param ("expects the wrappers —
        // the #347 inverse law"); the #346 item-get deref block requires a meta-FREE param;
        // and {@code tryMetaDerefArg}'s core carries the #347 {@code inverseN7CalleeParamMeta}
        // gate outright ({@code metaDeref && detectMetaKind(param) != NONE → return null} —
        // "upstream's type-directed coercion is IDENTITY there, so golden passes the whole nav
        // chain's wrapper BARE"), whose own comment names this census's witness function. The
        // numeric hoist arms cannot substitute: they need a BigDecimal-mapping param, and a
        // meta-annotated param maps to its FieldWithMetaX wrapper. So the arg renders RAW at a
        // meta param exactly as at a plain one, the seat's standing per-argument admissions
        // remain the only guard, and the pre-gate was pure conservatism.
        //
        // The live carriers (the calleeMetaParam face — 3 sole at the #527 SOT, ONE census
        // spelling: a FUNCTION callee whose input 1 carries [metadata scheme], six lowering
        // args, atRoot, {@code func:Create_OnDemandInterestPaymentPrimitiveInstruction})
        // serve WHOLE through the oracle-root callArgs dispatch by their own IRMetaAccess
        // argument — a standing #500 oracle-leaf admission whose routing law is exactly this
        // one ("the claim ROOT containing any meta hop renders WHOLE through the compiler's
        // oracle-root renderer, so the meta-wrapper arg coercion family stays legacy's own").
        // The class was a 100%-decline face pre-teach (the #504 conservation argument).

        // A MULTI param is now admitted (the .getMulti() arg unwrap, legacy facet tailMulti): a Mapper-chain arg (an
        // alias) into a multi parameter unwraps with .getMulti() instead of .get(). That accessor is an emitter (Java)
        // decision driven by the compiler's CallParamMultiResolver — kept off this neutral node, which only states the
        // call shape; the param cardinality is derivable from the resolved callee, so it is not duplicated here.
        List<RExpression> rawArgs = ref.args();
        List<IRExpr> args = new ArrayList<>(rawArgs.size());
        for (int i = 0; i < rawArgs.size(); i++) {
            Optional<IRExpr> arg = adapt(rawArgs.get(i), id.child(i + 1), ws); // child(0) is the callee reference
            // A filter/extract-bound implicit `item` arg is ALSO admitted (L-108 call-as-item-arg): the emitter
            // renders the bare lambda param `item` (the L-080 emitVariable arm), which unwrapForEvaluateArg strips
            // to `item.get()` for the evaluate slot — byte-identical to legacy's implicit-item call argument. The
            // SAME predicate the bare-item-OPERAND slice uses (USER_ITEM, SINGLE, typed, non-meta, filter/extract-
            // bound), so a then/switch/sort/reduce-bound item (re-rooted by legacy) is NOT admitted.
            RAttribute argParam = i < callee.inputs().size() ? callee.inputs().get(i) : null;
            if (arg.isEmpty()) {
                // #504 arm-C1a: the tryBareEnumArg mirror — a declining bare reference the
                // linker false-bound to a colliding document/type root re-qualifies against
                // the callee's positional declared enum parameter (see requalifiedEnumArg).
                arg = requalifiedEnumArg(rawArgs.get(i), argParam, id.child(i + 1), ws);
            }
            if (arg.isEmpty() && rawArgs.get(i) instanceof RConstructorExpr ctorArg) {
                // #512 — the CALL-ARG slot joins the root-only law's named admissions (the
                // #495 conditional-slot / #496 lambda-body-slot pattern — the THIRD named
                // seat): a constructor ARGUMENT lowers through the same shallow build, so
                // the arg:IRConstruct class can claim (pre-#512 the live path never lowered
                // a non-root constructor, so the gate below never saw the kind — the face's
                // events were the probe mirror's own root-position read). The routing
                // safety is the #512 apply-arg SHAPE leg (containsOracleLeaf carries the
                // apply-with-a-construct-arg shape), so every containing claim root renders
                // WHOLE through the oracle-root callArgs serve — the construct's own render
                // stays legacy's ConstructRenderer-family line inside it.
                arg = adaptConstructorShallow(ctorArg, id.child(i + 1), ws);
            }
            if (arg.isEmpty() && rawArgs.get(i) instanceof RExtractExpr exArg) {
                // #512 — the SAME named seat one level deeper: an ARG-position extract whose
                // body is a DIRECT constructor (`F(xs extract T {…})` — the cdm
                // arg:IRLambdaOp faces' own spelling) lowers with the ctor-slot admitted
                // (see adaptLambdaBodySlot's argCtorSlot leg — the live/mirror meter
                // divergence's close). The same apply-arg shape-leg routing carries it.
                arg = adaptLambdaOp(IRLambdaOp.Op.EXTRACT, exArg, exArg.argument(),
                        exArg.body(), id.child(i + 1), ws, BodySeat.ARG_CTOR_ONLY);
            }
            if (arg.isEmpty()
                    || !(isSimpleCallArg(arg.get())
                            || isNavCallArg(arg.get(), rawArgs.get(i), argParam, ws)
                            || isFilterExtractBoundItemOperand(arg.get(), rawArgs.get(i))
                            // #502 arm-3: the binder-UNBOUNDED item arg (the argItem face) —
                            // see isTypedUserItemArg.
                            || isTypedUserItemArg(arg.get(), rawArgs.get(i))
                            // #529: the DECLARATION-channel item arg (the #528 argItem
                            // residue — the untyped switch-case item whose meta-freedom the
                            // subject INPUT's declaration proves) — see
                            // isDeclProvenMetaFreeItemArg.
                            || isDeclProvenMetaFreeItemArg(arg.get(), rawArgs.get(i))
                            // #530 arm-D: the DEREF-PROVEN meta-param argument (the
                            // metaArgGate census's whole population — dir:deref 100%):
                            // legacy tryMetaDerefArg's firing preconditions restated on
                            // the DECLARATIONS; the deref render is legacy's own inside
                            // the oracle-root callArgs serve (IRMetaParamRef is a
                            // standing containsOracleLeaf member) — see
                            // isDerefProvenMetaParamArg.
                            || isDerefProvenMetaParamArg(arg.get(), rawArgs.get(i), argParam)
                            // #504 arm-C2: the explicit `empty` argument (the argEmpty face,
                            // 239 sole at the #503 SOT — the census read EVERY sibling arg
                            // lowering, p1-dominant against typed params). The lowered
                            // IREmptyLiteral IS the faithful arg; the render stays legacy's
                            // own null-threading — the routing safety is the SHAPE leg
                            // (containsOracleLeaf carries the apply-with-an-empty-arg shape),
                            // so every claim root containing this call renders WHOLE through
                            // the oracle-root callArgs serve and the emitter's evaluate-slot
                            // pipeline can never reach the empty arg.
                            // #640 arm D (D54 item 3): the bare condition attribute as a call argument (`Fn(attr)`)
                            || arg.get() instanceof IRImplicitAttrNav
                            || arg.get() instanceof IREmptyLiteral
                            // #505 arm-B: the LIST-OP argument (the arg:IRListOp face, 229
                            // sole at the #504 SOT — onlyElement/fieldAccess-child dominant
                            // at the #499 listOpGate census). The lowered IRListOp IS the
                            // faithful arg; the collapse render and the evaluate-slot unwrap
                            // stay legacy's own — the routing safety is the SHAPE leg
                            // (containsOracleLeaf carries the apply-with-a-list-op-arg
                            // shape), so every claim root containing this call renders WHOLE
                            // through the oracle-root callArgs serve; the shape was a
                            // 100%-decline face pre-teach, so no standing native compose
                            // reroutes (the #504 conservation argument).
                            || arg.get() instanceof IRListOp
                            // #511: the BINARY argument (the arg:BinaryOp face, 47 sole at
                            // the #510 SOT — `F(a + b)`-family compound args). The lowered
                            // BinaryOp IS the faithful arg; the operand render and the
                            // evaluate-slot unwrap stay legacy's own — the routing safety
                            // is the SHAPE leg (containsOracleLeaf carries the
                            // apply-with-a-BinaryOp-arg shape), so every claim root
                            // containing this call renders WHOLE through the oracle-root
                            // callArgs serve; the shape was a 100%-decline face pre-teach
                            // (the #504 conservation argument).
                            || arg.get() instanceof BinaryOp
                            // #512 — the ARG-KIND SWEEP (the #511 arg:BinaryOp / #505
                            // arg:IRListOp admission pattern VERBATIM, five kinds): the
                            // to-string conversion (arg:IRToString, 51 sole at the #511
                            // SOT), the list literal (arg:IRListConstruct 28), the
                            // lambda-bodied collection op (arg:IRLambdaOp 26), the type
                            // construction (arg:IRConstruct 22) and the conditional
                            // (arg:IRConditional 16). Each lowered node IS the faithful
                            // arg; the render and the evaluate-slot unwrap stay legacy's
                            // own — the routing safety: IRToString is a standing
                            // containsOracleLeaf KIND (the #503 leaf — the child walk
                            // routes every containing root), and the other four carry NEW
                            // composed-shape apply-arg legs (their kinds keep native root
                            // renders elsewhere — LambdaOpRenderer / ConstructRenderer /
                            // the conditional and list-literal arms — so the leaf is the
                            // COMPOSED shape, not the bare kind); every shape was a
                            // 100%-decline face pre-teach, so no standing native compose
                            // reroutes (the #504 conservation argument).
                            || arg.get() instanceof IRToString
                            || arg.get() instanceof IRListConstruct
                            || arg.get() instanceof IRLambdaOp
                            || arg.get() instanceof IRConstruct
                            || arg.get() instanceof IRConditional)) {
                return Optional.empty(); // a still-declining nav face / meta arg defers (#489: plain navs claim)
            }
            args.add(arg.get());
        }
        IRReference calleeRef = new IRReference(callee.name(),
                ruleCallee ? IRReference.ReferenceKind.RULE : IRReference.ReferenceKind.FUNCTION,
                id.child(0), type(ref, ws), card(ref, ws), Optionality.PRESENT, ref.sourceRange());
        return Optional.of(new IRApply(calleeRef, args, id,
                type(ref, ws), card(ref, ws), Optionality.PRESENT, ref.sourceRange()));
    }

    /**
     * #504 arm-C1a — legacy {@code ReferenceHandler.tryBareEnumArg}'s mirror (the #137/#143/
     * #368/#376 collision ladder): a still-declining bare (no-arg) reference whose symbol is
     * UNRESOLVED (legacy's #137 shape — zero live corpus rows, kept for ladder fidelity) or
     * false-bound to a root the linker name-collided on ({@link RBody} — the regime acronyms
     * {@code ASIC}/{@code CFTC}; {@link RDataType} — the type-shadowed values; {@link RCorpus}
     * — the {@code EMIR} corpus collisions; the EXACT legacy class set — {@code RChoice}/
     * {@code RAnnotation}/{@code RSegmentDef} binds keep declining exactly as legacy's arg arm
     * declines them), re-qualified against the callee's POSITIONAL declared enum parameter:
     * when the param's referenced type is an enumeration whose OWN values carry the name
     * (legacy's {@code en.values()} walk — deliberately NO super-enum chain), the argument IS
     * that enum constant (golden {@code RegimeNameEnum.ASIC}), minted as
     * {@code IRReference{ENUM_VALUE}} carrying the PARAM's attribute-channel type — the
     * standing {@link #isSimpleCallArg} ENUM_VALUE admission accepts it, and the RENDER routes
     * through the compiler's callArgs ORACLE serve (the apply-with-an-enum-arg shape leg in
     * {@code containsOracleLeaf} — which ALSO reroutes the standing qualified-enum-arg native
     * composes, byte-identical BY IDENTITY: the serve is legacy's own literal line, the #504
     * Seat-1 MF-4 disclosure). The alias-collision precedence cannot apply to the BOUND
     * collision classes (legacy's class gates exclude a shortcut-bound symbol; a shortcut-BOUND
     * reference lowers as an alias upstream of this fallback). For the UNRESOLVED shape the
     * mirror requalifies only when adapt() declined — legacy instead OVERRIDES its compiled
     * render post-hoc, so an unresolved name matching BOTH an enclosing shortcut and the
     * param's enum would diverge (alias here, constant there): ZERO corpus carriers, the named
     * #504 OBS-7 lock if one appears. The #504 census: {@code qualNameGate} argOf rows 264
     * node-unit, 100% {@code enumHit}, RegimeNameEnum/SupervisoryBodyEnum dominant.
     */
    private Optional<IRExpr> requalifiedEnumArg(RExpression rawArg, RAttribute argParam,
                                                NodeId id, RWorkspace ws) {
        if (ws == null || argParam == null || !(rawArg instanceof RSymbolReference bare)
                || !bare.args().isEmpty() || bare.name() == null || bare.name().isEmpty()) {
            return Optional.empty();
        }
        RNode symbol = bare.symbol().orElse(null);
        if (symbol != null && !(symbol instanceof RBody) && !(symbol instanceof RDataType)
                && !(symbol instanceof RCorpus)) {
            return Optional.empty();
        }
        RTypeCall paramType = argParam.typeCall();
        RNode referenced = paramType == null ? null : paramType.referencedType().orElse(null);
        if (!(referenced instanceof REnumeration en)) {
            return Optional.empty();
        }
        for (REnumValue value : en.values()) {
            if (bare.name().equals(value.name())) {
                return Optional.of(new IRReference(value.name(),
                        IRReference.ReferenceKind.ENUM_VALUE, id,
                        ws.getInferredAttributeType(argParam), ExpressionCardinality.SINGLE,
                        Optionality.PRESENT, bare.sourceRange()));
            }
        }
        return Optional.empty();
    }

    /**
     * #504 arm-C1b — legacy {@code ComparisonHandler.tryBareEnumComparand}'s mirror at the
     * EQUALITY seat (the #239/#299 type-shadow ladder): a still-declining bare (no-arg)
     * reference whose symbol is UNRESOLVED (legacy {@code bareEnumValueName}'s shape) or
     * false-bound to a type/document root ({@link RDataType}/{@link RChoice}/{@link RBody} —
     * legacy {@code typeShadowEnumValueName}'s EXACT class set; {@code RCorpus} deliberately
     * NOT admitted here — legacy's equality ladder never was, and the census read zero
     * corpus-bound equality rows), re-qualified against the SIBLING operand's cached
     * enumeration: when the sibling's engine type names an enumeration whose OWN values carry
     * the name (census-narrow — the #391 super-enum hierarchy leg stays declined, keeping the
     * emitter's declaring-enum qualification exact), the operand IS that enum constant, minted
     * as {@code IRReference{ENUM_VALUE}} carrying the SIBLING's enum type: the standing
     * equality enum-operand admission + {@code MapperS.of(EnumName.CONSTANT)} wrap render it
     * through the PROVEN native channel, and the emitter's {@code enumOperandRequalifies} gate
     * compares two EQUAL enum classes — no requalify decline. The alias-collision precedence
     * cannot apply to the BOUND classes (legacy's typeShadow gate excludes shortcut-bound
     * symbols); for the UNRESOLVED shape legacy OVERRIDES its compiled render post-hoc while
     * this mirror fires only on a declined adapt — the shortcut+enum double-match would
     * diverge (ZERO corpus carriers, the #504 OBS-7 lock if one appears). The #504 census:
     * {@code qualNameGate} eqSib rows 37
     * node-unit {@code enumHit} (RBody 30 · RDataType 7); the RAnnotation {@code enumMiss}
     * rows stay declined (the #204 item-re-root class — a different legacy recovery arm).
     */
    /**
     * #508 arm-C — the node-local requalify's SEAT test, shared by the arm and its mirror
     * twins: whether the reference sits at one of legacy's own recovery seats (a call-ARG
     * position against a resolvable positional enum parameter — RFunction callees of any
     * origin, rule callees through the {@link RFunction#fromRule} bridge — or an EQUALITY
     * operand against an enum-typed sibling) AND the corresponding #504 mirror mints. The
     * NodeId is irrelevant to the verdict (the mint's presence is the fact), so the twin
     * passes ROOT.
     */
    /**
     * #524 (the Seat-1 OBS-1 re-entrancy fence): whether an expression is ITSELF an
     * admissible bare enum-comparand shape — a no-arg {@link RSymbolReference} whose
     * symbol is unresolved or one of {@code requalifiedEnumComparand}'s admitted
     * false-bind classes. The sibling-adapt channel skips such siblings: adapting one
     * re-enters the comparand seat with the roles swapped (two dangling names — legacy
     * renders neither through the equality recovery, so declining is the honest mirror).
     */
    private static boolean isAdmissibleBareComparandShape(RExpression expr) {
        if (!(expr instanceof RSymbolReference bare) || !bare.args().isEmpty()) {
            return false;
        }
        RNode symbol = bare.symbol().orElse(null);
        return symbol == null || symbol instanceof RDataType || symbol instanceof RChoice
                || symbol instanceof RBody;
    }

    private boolean wouldRequalifyAtParentSeat(RSymbolReference ref, RWorkspace ws) {
        if (ref.parent() instanceof RSymbolReference call && !call.args().isEmpty()) {
            int idx = call.args().indexOf(ref);
            RNode calleeSym = call.symbol().orElse(null);
            RFunction calleeFn = calleeSym instanceof RFunction f ? f
                    : calleeSym instanceof RRule rule ? RFunction.fromRule(rule) : null;
            if (calleeFn != null && idx >= 0 && idx < calleeFn.inputs().size()
                    && requalifiedEnumArg(ref, calleeFn.inputs().get(idx), NodeId.ROOT, ws)
                            .isPresent()) {
                return true;
            }
        }
        if (ref.parent() instanceof REqualityExpr eq) {
            RExpression sibling = eq.rawLeft() == ref ? eq.rawRight() : eq.rawLeft();
            return requalifiedEnumComparand(ref, sibling, NodeId.ROOT, ws).isPresent();
        }
        // #530 arm-C twin: the context-typed seat pair (the KVP / conditional-branch
        // seats) — the mirror reads the SAME walk + mint the arm runs (the NodeId is
        // irrelevant to the verdict, the #508 twin law).
        return requalifiedEnumAtContextSeat(ref, NodeId.ROOT, ws).isPresent();
    }

    /**
     * #530 arm-C: the context-typed seat requalify — the seat walk
     * ({@link #contextSeatExpectedAttribute}) composed with the mint
     * ({@link #requalifiedEnumContext}); empty when either leg declines.
     */
    private Optional<IRExpr> requalifiedEnumAtContextSeat(RSymbolReference ref, NodeId id,
            RWorkspace ws) {
        RAttribute seatAttr = contextSeatExpectedAttribute(ref);
        return seatAttr == null ? Optional.empty()
                : requalifiedEnumContext(ref, seatAttr, id, ws);
    }

    /**
     * #530 arm-C: the EXPECTED-attribute walk for the context-typed seats — the attribute
     * whose declared type legacy's scoping resolves a bare value name against. The KVP
     * seat: the enclosing {@link RConstructorExpr}'s constructed data type's KEY attribute
     * (by-name — the ctor render's own channel). The conditional-BRANCH seat (then/else,
     * never the boolean condition): the else-if chain ascends branch-by-branch to the
     * nearest NON-conditional seat and reads ITS channel — the KVP key one level out, or
     * the plain un-segmented {@link ROperation} target when it names the enclosing
     * function's own output (census-narrow: a segmented target keeps declining — zero
     * carriers at the #530 census). Every other seat returns {@code null} (legacy has no
     * scoping context there).
     */
    private RAttribute contextSeatExpectedAttribute(RSymbolReference ref) {
        RNode child = ref;
        int depth = 0;
        while (depth++ < PARENT_WALK_LIMIT) {
            RNode parent = child.parent();
            if (parent instanceof RConditionalExpr cond) {
                if (cond.condition() == child) {
                    return null; // the boolean seat — never an expected-enum seat
                }
                child = cond; // a then/else branch — ascend the else-if chain
                continue;
            }
            if (parent instanceof RKeyValuePair kvp && kvp.value() == child
                    && kvp.parent() instanceof RConstructorExpr ctor) {
                RNode ctorType = ctor.typeCall() == null ? null
                        : ctor.typeCall().referencedType().orElse(null);
                return ctorType instanceof RDataType ctorData && kvp.key() != null
                        ? findAttributeOnDataTypeByName(ctorData, kvp.key()) : null;
            }
            if (parent instanceof ROperation op && op.expression() == child) {
                if (op.segment().isPresent()) {
                    return null; // census-narrow: the plain target only
                }
                RFunction enclosing = enclosingFunction(ref);
                RAttribute out = enclosing == null ? null : enclosing.output().orElse(null);
                return out != null && out.name() != null
                        && out.name().equals(op.targetName()) ? out : null;
            }
            return null; // any other seat — no legacy scoping context
        }
        return null;
    }

    /**
     * #530 arm-C: the context-seat mint — {@link #requalifiedEnumArg}'s core keyed on the
     * SEAT attribute's declared enumeration (the same {@code en.values()} walk, no
     * super-enum chain; the same {@code IRReference{ENUM_VALUE}} builder carrying the seat
     * attribute's own inferred type — the ctor-value / conditional-branch renders treat it
     * exactly like a genuinely-bound bare enum value). Census-narrow class set: the #530
     * kvpGate census's OWN false-bind classes ({@link RDataType} the type-shadowed values ·
     * {@link RBody} the rule-name collisions) — a new class at these seats discloses
     * itself as a fresh census row first, the instrument-first order.
     */
    private Optional<IRExpr> requalifiedEnumContext(RExpression rawValue, RAttribute seatAttr,
            NodeId id, RWorkspace ws) {
        if (ws == null || seatAttr == null || !(rawValue instanceof RSymbolReference bare)
                || !bare.args().isEmpty() || bare.name() == null || bare.name().isEmpty()) {
            return Optional.empty();
        }
        RNode symbol = bare.symbol().orElse(null);
        if (!(symbol instanceof RDataType) && !(symbol instanceof RBody)) {
            return Optional.empty(); // census-narrow — the #530 kvpGate classes only
        }
        RTypeCall seatType = seatAttr.typeCall();
        RNode referenced = seatType == null ? null : seatType.referencedType().orElse(null);
        if (!(referenced instanceof REnumeration en)) {
            return Optional.empty();
        }
        for (REnumValue value : en.values()) {
            if (bare.name().equals(value.name())) {
                return Optional.of(new IRReference(value.name(),
                        IRReference.ReferenceKind.ENUM_VALUE, id,
                        ws.getInferredAttributeType(seatAttr), ExpressionCardinality.SINGLE,
                        Optionality.PRESENT, bare.sourceRange()));
            }
        }
        return Optional.empty();
    }

    private Optional<IRExpr> requalifiedEnumComparand(RExpression rawOperand,
            RExpression rawSibling, NodeId id, RWorkspace ws) {
        if (ws == null || rawSibling == null || !(rawOperand instanceof RSymbolReference bare)
                || !bare.args().isEmpty() || bare.name() == null || bare.name().isEmpty()) {
            return Optional.empty();
        }
        RNode symbol = bare.symbol().orElse(null);
        if (symbol != null && !(symbol instanceof RDataType) && !(symbol instanceof RChoice)
                && !(symbol instanceof RBody)) {
            return Optional.empty();
        }
        RMetaAnnotatedType sibType = type(rawSibling, ws);
        if ((sibType == null || sibType.isMissing())
                && !isAdmissibleBareComparandShape(rawSibling)) {
            // #524 — the ARM-typed sibling channel: a sibling the engine cache never typed
            // but the adapter LOWERS carries its lowered node's own type (e.g. the
            // #522-taught annotation-shadowed `filter qualification = <VALUE>` LHS — the
            // by-name item read whose navType is the attribute channel's enum). Legacy's
            // {@code tryBareEnumComparand} types the SAME sibling through its own compiled
            // render, so the mirror stays seat-and-mechanism-exact; every other gate below
            // is unchanged (prove-or-decline). The shape guard above is the RE-ENTRANCY
            // fence (the Seat-1 #524 OBS-1 close): a sibling that is ITSELF an admissible
            // bare comparand would re-enter this seat with the roles swapped (adapt →
            // requalify → adapt — two dangling names, unbounded), and such a pair is two
            // unresolved bare reads legacy renders through neither recovery — declining
            // is the honest mirror. Stateless by construction (a structural test, no
            // re-entrancy state — the adapter's own contract).
            IRExpr sibLowered = adapt(rawSibling, ws).orElse(null);
            sibType = sibLowered == null ? null : sibLowered.type();
        }
        if (sibType == null || sibType.isMissing()
                || !(sibType.type() instanceof REnumTypeRef enumRef)) {
            return Optional.empty();
        }
        for (REnumValue value : enumRef.astNode().values()) {
            if (bare.name().equals(value.name())) {
                return Optional.of(new IRReference(value.name(),
                        IRReference.ReferenceKind.ENUM_VALUE, id, sibType,
                        ExpressionCardinality.SINGLE, Optionality.PRESENT, bare.sourceRange()));
            }
        }
        return Optional.empty();
    }

    /**
     * A byte-safe call argument the emitter can render into a bare {@code evaluate(...)} slot:
     * <ul>
     *   <li>a function parameter of EITHER cardinality (single → {@code MapperS.of(name)}, slice 1 L-041; multi →
     *       {@code MapperC.<Item>of(name)}, the multi-param-ARG sub-slice L-048) or any {@link IRLiteral}
     *       (int/number/string/boolean) — its emitted {@code Mapper} wrap strips STRUCTURALLY (the
     *       {@code unwrapToBuilder} branch of {@code unwrapForEvaluateArg}) to the bare {@code evaluate} slot, no
     *       accessor suffix, no per-param coercion. A multi param strips at the SAME branch as the scalar (the
     *       {@code .getMulti()} accessor is reached only by a chained {@code Mapper}, never by a structurally-wrapped
     *       leaf), and its {@code <Item>} witness is discarded by the strip — byte-identical to legacy, which compiles
     *       a bare multi parameter to the same {@code MapperC} wrap and strips it identically;</li>
     *   <li>an alias/shortcut reference ({@link IRReference.ReferenceKind#ALIAS}) — the alias method returns a
     *       {@code Mapper}, so its {@code aliasName(inputs)} render (L-031 {@code emitAlias}) takes the legacy
     *       oracle's {@code .get()} fall-through ({@code aliasName(inputs).get()} — the golden
     *       {@code economicTerms(trade).get()} shape). Byte-identical by construction: the emitter reuses the
     *       L-031 alias render + the same {@code ReferenceHandler.unwrapForEvaluateArg} oracle Path-1 calls. This
     *       is the BIGGEST call-arg lever — a firing-ceiling measurement found alias args block ~2557 otherwise-
     *       simple calls across the 3 cells (navigation args measured 0 AT THAT TIME — the reverted L-042 dud;
     *       the #478–#485 + #487 arm teaches then created the population by lowering the navs, the #488
     *       {@link #argNavFacet} decode read it at 2,300 sole, FUNCTION-dominant, and the #489
     *       {@link #isNavCallArg} revival claimed it — the historical figure is measurement-era only).</li>
     *   <li>a qualified enum-value reference ({@link IRReference.ReferenceKind#ENUM_VALUE}, {@code Enum -> VALUE})
     *       — renders the unwrapped constant {@code EnumName.CONSTANT}, which {@code unwrapForEvaluateArg} passes
     *       through unchanged (no {@code MapperS.of} wrap, no {@code .get()} — the dotted-enum-constant branch).
     *       The next call-arg lever after alias args (firing-ceiling ~1360 across the 3 cells); the genuine-enum
     *       gate lives in {@link #adaptEnumValueRef} (the five disguised {@code REnumValueRef} channels never
     *       lower to {@code ENUM_VALUE}, so they cannot reach here).</li>
     * </ul>
     * <p>A NESTED function CALL ({@link IRApply}) is ALSO admitted (L-107 call-as-nested-arg): {@code f(g(x))} —
     * the inner call {@code g(x)} lowered to an {@link IRApply} (its own args simple, so adaptApply built it), and
     * {@code emitApply} renders it recursively as {@code MapperS.of(g.evaluate(x))} / {@code MapperC.<Item>of(...)},
     * which {@code unwrapForEvaluateArg} strips into the bare {@code evaluate} slot via the SAME oracle the
     * top-level call uses — byte-identical by construction, no emitter change. The arg's cardinality is handled by
     * the per-param {@code asMulti} accessor ({@code .get()}/{@code .getMulti()}) exactly as for a parameter arg.
     * <p>A NAVIGATION chain ({@link FieldAccess}) is ALSO admitted since #489 — the L-042 REVIVAL —
     * but at the CALL SEATS' or-composition, not here: the nav admission ({@link #isNavCallArg})
     * needs the RAW argument node, the positional callee PARAMETER and the workspace (the
     * guarded-item-family mirror + the type-agreement gate), which a shape-only predicate cannot
     * carry. This predicate stays the shape-only leg of the seats'
     * {@code isSimpleCallArg || isNavCallArg || isFilterExtractBoundItemOperand} admission.
     * Still DEFERRED — each trips a later-slice arg-unwrap/coercion facet: a still-declining
     * navigation ({@link #argNavFacet}'s decline faces — the drift detectors plus the #489
     * admission-residue faces), the implicit {@code item} outside the L-108 filter/extract binding
     * (L-032), an {@code empty}/{@code null} arg, or a meta operand.
     */
    private static boolean isSimpleCallArg(IRExpr expr) {
        return (expr instanceof IRVariable var && var.variableKind() == IRVariable.VariableKind.PARAM)
                || expr instanceof IRLiteral
                || expr instanceof IRApply
                // #492: a point-free arg — the oracle renders the implicit invocation off the raw
                // node and the arg pipeline runs legacy's own unwrapForEvaluateArg, so the slot
                // composition is byte-identical by construction (the probed callArg face).
                || expr instanceof IRPointFreeApply
                // #500: a meta-hop arg (the #499-exposed arg:IRMetaAccess face, 179 node-unit at
                // the census) — the claim ROOT containing any meta hop renders WHOLE through the
                // compiler's oracle-root renderer (super.visitSymbolReference — the literal
                // legacy line), never the emitter's composed evaluate-slot pipeline, so the
                // meta-wrapper arg coercion family stays legacy's own (the distinct-kind law's
                // consumer admission: admitted at the gate, served only at oracle roots).
                || expr instanceof IRMetaAccess
                // #501: the ORACLE-LEAF arg admissions — a lowered conversion (the #500-exposed
                // arg:IRConversion face, 28 sole / 100% FUNCTION callees at the census), a
                // lowered then-pipe (arg:IRPipe 6) and a lowered disguised symbol-receiver
                // navigation (the arm-C IRSymbolNav kind). Same routing safety as the meta leg
                // above: the containing claim root renders whole through the oracle-root serve
                // (the callArgs dispatch — legacy's own evaluate-arg pipeline, coercions
                // included), and no emitter arm exists for the shallow kinds, so a native
                // compose can never reach them.
                || expr instanceof IRConversion || expr instanceof IRPipe
                || expr instanceof IRSymbolNav
                // #502: a lowered closure-param reference (the arm-2 IRClosureParam kind — the
                // interior.RSymbolReference census class: a named/synthetic lambda param passed
                // as a call argument). Same routing safety: the containing claim root renders
                // whole through the oracle-root callArgs dispatch (legacy's own evaluate-arg
                // pipeline resolves the binding), and no emitter arm exists for the kind.
                || expr instanceof IRClosureParam
                || (expr instanceof IRReference ref
                        && (ref.referenceKind() == IRReference.ReferenceKind.ALIAS
                                || ref.referenceKind() == IRReference.ReferenceKind.ENUM_VALUE))
                // #504 arm-B consumer admission: a lowered un-retypeable synthetic-item nav
                // as a call argument (arg:IRSynItemNav 6 sole at the post-arm probe) — the
                // kind is an oracle leaf, so the containing claim root renders whole through
                // the oracle-root callArgs serve (the #501/#502 shallow-kind arg-admission
                // law), and no emitter arm exists for the kind.
                || expr instanceof IRSynItemNav
                // #508 arm-A5 consumer admission: a lowered meta-sourced USER-item nav as a
                // call argument (the metaSrcGate census's interior.RSymbolReference rows) —
                // the same oracle-leaf routing law.
                || expr instanceof IRMetaItemNav
                // #505 consumer admissions: the choice-option nav and the dispatch-base
                // input as call arguments — both oracle leaves (the same routing law).
                || expr instanceof IRChoiceOptionNav
                || expr instanceof IRDispatchInputRef
                // #507 consumer admissions: the deep-path nav and the record-feature read as
                // call arguments — both oracle leaves (the same routing law: the containing
                // claim root renders whole through the oracle-root callArgs serve).
                || expr instanceof IRDeepFeatureNav
                || expr instanceof IRRecordFeatureNav
                // #512 arm-B consumer admissions: the receiver-carrying record read (the
                // nsrGate census's interior.RSymbolReference claim roots — record navs as
                // call arguments) and the item-qualifier read (the StringContains(item ->
                // scheme, …) filter class) — both oracle leaves (the same routing law).
                || expr instanceof IRRecordReceiverNav
                || expr instanceof IRQualifierItemNav
                // #513: the noAdaptArm-sweep kinds as call arguments (the sweep families'
                // pre-arm faces hid the arg seats behind the minimal-blocker attribution)
                // — all four oracle leaves (the same routing law: the containing claim
                // root renders whole through the oracle-root callArgs serve).
                || isNoAdaptArmSweepKind(expr)
                // #514 ARM-1 consumer admission: a lowered function-OUTPUT reference as a
                // call argument (the notAnInputParam.out.p:RSymbolReference decode rows) —
                // an oracle leaf (the same routing law). IRMetaParamRef is deliberately NOT
                // admitted here: the pPlain/argSeatMiss decode slices need legacy's unwrap
                // coercion, so those seats keep declining (the kind's containment).
                || expr instanceof IROutputRef
                // #514 ARM-2 consumer admission: the top-level rule-input nav as a call
                // argument (the ruleInputNav.p:RSymbolReference decode rows) — an oracle
                // leaf (the same routing law).
                || expr instanceof IRRuleInputNav;
    }

    /**
     * #489: the L-042 REVIVAL — a byte-safe NAVIGATION call argument (the #488 {@link #argNavFacet}
     * decode's named successor): admitted exactly when the classifier returns a {@code null} decline
     * face. The admission (the classifier's null path): every hop typed (non-{@code isMissing()}) and
     * meta-free, the root ∈ {@code param} (1,678 of the 2,300 sole) / {@code item} / {@code synItem}
     * (20 + 523 — hop-clean AND un-chained-raw AND meta-free-rooted: the #469 guarded-item families'
     * adapter mirror, the #489 ring decode) / a lowered explicit call ({@link IRApply} — 79, the #484
     * composition; {@code adaptApply} is reachable only with explicit args present, so an
     * {@code IRApply} root never hides a point-free bare reference whose implicit input the render
     * would drop), AND the chain's result R-type ≡ the callee parameter's declared R-type (the
     * type-agreement gate — legacy's guarded arg-coercion machinery stays unreachable).
     * <p>The render exists BY CONSTRUCTION — the chain lowered, so the nav arms' own admissions held
     * and the emitter's standing chain render applies; the evaluate-slot unwrap is legacy
     * {@code ReferenceHandler.unwrapForEvaluateArg}'s chained-{@code Mapper} fall-through — the SAME
     * oracle path the alias-argument leg takes ({@code .get()} into a SINGLE callee param,
     * {@code .getMulti()} into a MULTI one — facet {@code tailMulti}, served per-call by the
     * range-correlated {@code CallParamMultiResolver}; golden witness
     * {@code resolveAdjustableDates.evaluate(MapperS.of(valuationDates).<...>map(...).get())},
     * cdm6 {@code AdjustedValuationDates}). Legacy's remaining per-arg machinery is unreachable for
     * the admitted shapes, each verified at source: the arg compiles NEUTRAL ({@code compile(arg,
     * null, scope)} — the #360 {@code evaluateArgExpectedTypeReset} facet, mirrored by the IR
     * emitter's neutralized arg context); the guarded arg-coercion seat fires only on a
     * param-vs-item Java-type MISMATCH ({@code ReferenceHandler}'s {@code paramJavaType.equals(
     * actualItemType)} precondition read at source — the type-agreement gate keeps it inert, R-equal
     * ⇒ Java-equal); and the bare-fn-raw-pass / then-collapse-round-trip / closure-param-name-append
     * special cases match no raw shape that lowers to a {@link FieldAccess}. The decline faces —
     * the #488 drift detectors plus the #489 admission-residue faces — are {@link #argNavFacet}'s
     * non-null returns, the arm's conservation instrument.
     */
    private static boolean isNavCallArg(IRExpr lowered, RExpression rawArg, RAttribute param, RWorkspace ws) {
        return lowered instanceof FieldAccess nav && argNavFacet(nav, rawArg, param, ws) == null;
    }

    /**
     * Lowers an ordered COMPARISON ({@code < > <= >=}) to {@link BinaryOp} — the first
     * structural lowering, recursing into both operand subtrees all-or-nothing (the
     * {@code RListLiteral} template). Claims a comparison when both operands are byte-safe
     * comparison operands ({@link #isComparisonOperand}): a scalar {@link IRVariable.VariableKind#PARAM}
     * or single navigation result ({@link #isScalarOperand} — the {@code n1 > n2} / {@code foo -> bar = baz}
     * shapes), a {@code count} list-op ({@link #isCountOperand} — the {@code xs count >= 1} cardinality-check
     * shape, a resolved {@code Integer}-valued scalar), OR a numeric ({@code int}/{@code number}) literal whose
     * SIBLING is such a resolved scalar / count operand (the {@code arg < 0} / {@code count >= 1} shapes — legacy
     * threads the sibling's resolved numeric type into the literal's render via
     * {@code ComparisonHandler.inferNumericType}: an {@code int} literal against a {@code number} sibling →
     * {@code MapperS.of(BigDecimal.valueOf(N))}, against an {@code int}/{@code count} sibling → bare
     * {@code MapperS.of(N)}). The sibling constraint is the byte-safety linchpin — the emitter needs a resolved
     * numeric sibling to determine the coercion, which a scalar param / single navigation / count always is when
     * the other operand is a numeric literal (Rune forbids ordering a number against a non-number). The int/number
     * JOIN, the {@code BigDecimal.valueOf} coercion, the operand witness, and the {@code count} {@code MapperS.of}
     * wrap are Java-emission decisions and stay in the emitter (the L-029 split). Still deferred: two numeric
     * literals ({@code 0 = 0} — no resolved sibling to type them), bare enum-value operands (legacy
     * {@code wrapEnumOperand} re-wraps the dotted constant; not a comparison shape anyway), the implicit
     * {@code item} and alias/call/multi-navigation operands (not yet emitter-expressible — they fail the scalar +
     * count gate), and the non-{@code count} list-ops as existence operands. An explicit {@code all}/{@code any}
     * modifier lowers to the DISTINCT {@code IRAllAnyCompare} kind since the #503 arm-A1 (unconditional at the
     * gate below; the containing root oracle-serves the claim), so only the default modifier reaches the
     * admitted-operator path — which keeps defaulting to {@code CardinalityOperator.All}.
     */
    private Optional<IRExpr> adaptComparison(RComparisonExpr cmp, NodeId id, RWorkspace ws) {
        if (cmp.mod().isPresent()) {
            // #503 arm-A1 — the modified COMPARISON mirror of the equality leg above (the
            // same allAnyModifier reason spelling; the comparison family's census tail).
            // The same shallow lowering; the raw-family split is recovered at the oracle
            // dispatch by the range-correlated node class (super.visitComparison).
            return Optional.of(new IRAllAnyCompare(cmp.op().name(), cmp.mod().get().name(), id,
                    type(cmp, ws), card(cmp, ws), Optionality.PRESENT, cmp.sourceRange()));
        }
        RExpression rawLeft = cmp.rawLeft();
        RExpression rawRight = cmp.rawRight();
        if (rawLeft == null || rawRight == null) {
            return Optional.empty(); // a then-body elided operand — defer
        }
        Optional<IRExpr> left = adapt(rawLeft, id.child(0), ws);
        Optional<IRExpr> right = adapt(rawRight, id.child(1), ws);
        if (left.isEmpty() || right.isEmpty()) {
            return Optional.empty(); // an operand is not yet Wave-0-expressible
        }
        boolean leftOk = isComparisonOperand(left.get(), right.get())
                || isBinderBlindItemOperand(left.get(), rawLeft);
        boolean rightOk = isComparisonOperand(right.get(), left.get())
                || isBinderBlindItemOperand(right.get(), rawRight);
        if (!leftOk || !rightOk) {
            return Optional.empty(); // scalar/nav operands, a numeric literal with a scalar sibling, or a filter/extract item
        }
        return Optional.of(new BinaryOp(toBinOp(cmp.op()), left.get(), right.get(), id,
                type(cmp, ws), card(cmp, ws), Optionality.PRESENT, cmp.sourceRange()));
    }

    /**
     * A byte-safe COMPARISON ({@code < > <= >=}) operand, sibling-aware: a scalar param / single navigation
     * ({@link #isScalarOperand}), a {@code count} list-op ({@link #isCountOperand} — a resolved {@code Integer}-valued
     * scalar, the {@code xs count >= 1} shape), OR a numeric ({@code int}/{@code number}) literal whose {@code sibling}
     * is one of those resolved scalar operands. The sibling constraint admits {@code arg < 0} / {@code count >= 1} (a
     * resolved numeric sibling the emitter can read the coercion type from) while deferring {@code 0 = 0} (two literals,
     * no resolved sibling) and a literal against an alias / call / multi-navigation sibling (which fails the scalar +
     * count gate). The int/number JOIN + {@code BigDecimal.valueOf} coercion stay in the emitter; a {@code count}
     * operand classifies as {@code Integer} and wraps {@code MapperS.of(<chain>.resultCount())} there (the L-040 /
     * L-050 split).
     *
     * <p>Public since #528 as the opResGate census's own BY-CALL vocabulary (the #524 census
     * BY-CALL law — the census reads the COMPARISON seat's failing side off this exact
     * sibling-aware gate, in the mirror's own left-then-right order); read-only.
     */
    public static boolean isComparisonOperand(IRExpr operand, IRExpr sibling) {
        // #507: the record-feature read and the deep-path nav join the comparison operand set
        // AND the numeric-literal sibling set (the record teach's own exposed frontier — the
        // `startDate -> day > 29` corpus shape: day/month/year are int-valued record features,
        // ordered in the corpus by design). The routing safety: both kinds are oracle leaves,
        // so the containing comparison root renders WHOLE through the oracle-root comparison
        // serve (the literal super.visitComparison line — legacy's own numeric-type threading
        // and literal coercion inside it); the emitter has no arm for either kind, so a native
        // compose can never read the operand.
        return isScalarOperand(operand) || isCountOperand(operand)
                || isOracleNavOperand(operand)
                // #510 arm-A4c: a lowered alias/shortcut reference as a comparison operand
                // (operandAlias.clean — 53 comparison sole at the #509 SOT; the non-clean
                // comparison facets ride the same admission). The #372 comparisonIntWiden
                // lever — a numeric-alias inequality firing a widen hop on its sibling —
                // was the NATIVE render's decline reason; at a whole-legacy render the
                // widen is legacy's own, so EVERY alias body admits. The routing safety is
                // the SHAPE leg (the compiler's containsOracleLeaf carries the
                // comparison-with-an-alias-operand shape): the containing root renders
                // WHOLE through the oracle-root comparison serve (the literal
                // super.visitComparison line).
                || isAliasReference(operand)
                // #511: a lowered non-COUNT list op joins the comparison operand set AND the
                // numeric-literal sibling set (operand:IRListOp — 2 comparison sole at the
                // #510 SOT; the #507 precedent for the paired licensing). The routing safety
                // is the SHAPE leg (the compiler's containsOracleLeaf carries the
                // comparison-with-a-non-COUNT-list-op-operand shape): the containing root
                // renders WHOLE through the oracle-root comparison serve. COUNT stays OUT —
                // `xs count >= 1` is the standing NATIVE compose.
                || isNonCountListOp(operand)
                // #513: the noAdaptArm-sweep kinds join the comparison operand set AND the
                // numeric-literal sibling set (the #507/#511 paired-licensing precedent;
                // the sweep families' pre-arm faces hid the comparison seats behind the
                // minimal-blocker attribution). The routing safety is the standing law:
                // all four are oracle leaves with no emitter arm, so the containing root
                // renders WHOLE through the oracle-root comparison serve (the literal
                // super.visitComparison line — legacy's own numeric-type threading and
                // literal coercion inside it).
                || isNoAdaptArmSweepKind(operand)
                // #514 ARM-1 consumer admission: a lowered function-OUTPUT reference as a
                // comparison operand (the notAnInputParam.out.p:RComparisonExpr decode rows
                // — the `<output> > 0` shapes) — an oracle leaf (the same routing law), with
                // the numeric-literal licensing pair below (the #507/#511/#513 precedent).
                || operand instanceof IROutputRef
                // #528 arm-2: a lowered POINT-FREE reference joins the comparison operand set
                // (the operand:IRPointFreeApply face — 4 sole at the #527 SOT, one census
                // DECODE across the position-split pair of bucket tokens (at-root vs
                // interior — the Seat-1 MF-3 recut): a `date`-typed SINGLE point-free apply
                // as the RIGHT operand against a nav sibling,
                // `func:GetLastFloatingReferenceResetDate`). The #504
                // arm-A admitted the kind at the LOGICAL, EXISTENCE and EQUALITY seats; the
                // comparison seat is its last un-admitted BOOLEAN-PRODUCING operand seat —
                // the ARITHMETIC seat keeps its standing decline (isArithmeticOperand omits
                // the kind; its census belt can still spell the face). Legacy's ComparisonHandler
                // composes the implicit-invocation render + the operand threading inside its
                // own line, so the routing safety is the SHAPE leg (the compiler's comparison
                // leg grows the kind — the containing root renders WHOLE through the
                // oracle-root comparison serve, the literal super.visitComparison line);
                // point-free args and receivers keep their standing native composes. The
                // shape was a 100%-decline face pre-teach (the #504 conservation argument).
                || operand instanceof IRPointFreeApply
                // #640 arm D (D54 item 3): the bare condition attribute as a comparison operand (`attr > 0`) - an
                // oracle leaf, so the containing root renders WHOLE through the oracle-root comparison serve; the
                // numeric-literal sibling set gains it below (the #528 arm-3 precedent: an operand arm alone moves
                // the face to operandNumericLiteral.sib:IRImplicitAttrNav)
                || operand instanceof IRImplicitAttrNav
                || (isNumericLiteral(operand)
                        && (isScalarOperand(sibling) || isCountOperand(sibling)
                                || isOracleNavOperand(sibling)
                                || isNonCountListOp(sibling)
                                || isNoAdaptArmSweepKind(sibling)
                                // #513: the ALIAS sibling joins the comparison licensing
                                // set (operandNumericLiteral.sib:alias — the #513 facet
                                // read the face 100% this ONE class, 15 + 17 sole; the
                                // #510 arm-A4c admitted the alias OPERAND, and the #507/
                                // #511 paired-licensing precedent completes the pair).
                                // Identity-serve routing: the alias operand itself
                                // triggers the standing comparison-with-an-alias-operand
                                // shape leg, so the containing root oracle-serves WHOLE
                                // and the literal's coercion is legacy's own inside that
                                // line.
                                || isAliasReference(sibling)
                                // #514: the OUTPUT-reference sibling joins the same
                                // licensing pair (the paired-licensing law).
                                || sibling instanceof IROutputRef
                                // #528 arm-3: the ITEM sibling joins the comparison licensing
                                // set — the gap the #513 numeric-literal decode NAMED
                                // ({@code sib:item} = "the bound-item leg neither licensing
                                // set carries") and the arm-3 carriers' own shape (the
                                // opResGate census read BOTH at sib:IRLiteral, so admitting
                                // the item operand without this pair would only move the
                                // face to the literal side). Identity-serve routing, the
                                // #513 alias-sibling argument verbatim: the item operand
                                // itself trips the standing KIND-WIDE comparison shape leg,
                                // so the containing root oracle-serves WHOLE and the
                                // literal's numeric-type threading is legacy's own inside
                                // that line.
                                || isItemOperand(sibling)
                                // #528: the POINT-FREE sibling joins the same licensing pair
                                // (the #507/#511/#513/#514 paired-licensing law — the live
                                // carriers pair against a nav sibling, so this leg is the
                                // law's completion, not a corpus claim). Identity-serve
                                // routing: the point-free operand itself trips the standing
                                // comparison shape leg, so the containing root oracle-serves
                                // WHOLE and the literal's coercion is legacy's own inside
                                // that line.
                                || sibling instanceof IRPointFreeApply
                                // #640 arm D: the bare-attribute sibling (`0 < attr`, `attr > 0`) - the pair
                                || sibling instanceof IRImplicitAttrNav));
    }

    /**
     * The #507 oracle-leaf nav operands (the record-feature read / the deep-path nav) admitted
     * by the sibling-aware comparison and equality gates — both kinds render only BY IDENTITY
     * (the containing root's oracle serve; no emitter arm exists), so the numeric-literal
     * sibling's coercion stays legacy's own inside that literal line. #520 probe2: the
     * RECEIVER-carrying record read joins (the #512 kind at the same seats — the probe1
     * headOtherGate census's interior.RComparisonExpr rows, the {@code expiryDate <=
     * GetExecutionTimestamp(…) -> date} shapes); the same routing law — the kind is a
     * standing kind-wide {@code containsOracleLeaf} entry since #512, so the containing
     * comparison/equality root renders WHOLE through the oracle serve with no new compiler
     * leg.
     */
    private static boolean isOracleNavOperand(IRExpr expr) {
        return expr instanceof IRRecordFeatureNav || expr instanceof IRDeepFeatureNav
                || expr instanceof IRRecordReceiverNav;
    }

    /**
     * Whether a lowered operand is a {@code count} list-op ({@link IRListOp} of {@link IRListOp.Kind#COUNT}) — a
     * resolved {@code Integer}-valued scalar operand of the comparison / equality idioms (the {@code xs count = 1} /
     * {@code trade -> legs count >= 1} cardinality-check shapes). Admitted by the sibling-aware comparison / equality
     * gates exactly as a scalar param is: it both serves as the resolved operand AND licenses a numeric-literal
     * sibling. The {@link IRListOp} already lowered all-or-nothing (its receiver subtree is IR-expressible, L-050), so
     * the operand emits {@code <chain>.resultCount()} and the emitter wraps it {@code MapperS.of(...)} (mirroring
     * legacy {@code ComparisonHandler.wrapCountOperand}); the {@code Integer} witness + the wrap are Java-emission
     * decisions kept in the emitter (the L-029 split). {@code COUNT} is the only list-op kind the NATIVE operand
     * pipeline supports — since #511 a non-{@code COUNT} list-op admits too, but through the SEPARATE
     * {@link #isNonCountListOp} leg whose containing root renders WHOLE through the oracle serve (the COUNT
     * carve-out on both sides keeps this native compose and that identity serve from ever trading places).
     */
    private static boolean isCountOperand(IRExpr expr) {
        return expr instanceof IRListOp listOp && listOp.op() == IRListOp.Kind.COUNT;
    }

    /**
     * #511: a lowered list op of any kind EXCEPT {@code COUNT} — the equality/comparison operand
     * admissions' shape test, twin-exact with the compiler's {@code containsOracleLeaf}
     * equality/comparison shape legs (the same kind fact off the same node). {@code COUNT} is
     * carved out because {@code xs count = 1} / {@code xs count >= 1} are the standing NATIVE
     * composes ({@link #isCountOperand} + the emitter's {@code resultCount} wrap) — the exclusion
     * keeps them composing natively while every other kind routes its containing root through
     * the oracle serve.
     */
    private static boolean isNonCountListOp(IRExpr expr) {
        return expr instanceof IRListOp listOp && listOp.op() != IRListOp.Kind.COUNT;
    }

    /**
     * Lowers an ARITHMETIC binary operation ({@code + - * /}) to {@link BinaryOp} — the numeric sibling of
     * {@link #adaptComparison}. Recurses both operand subtrees all-or-nothing and claims the node when both
     * operands are byte-safe arithmetic operands ({@link #isArithmeticOperand} — the scalar-param /
     * literal / alias / nested-arithmetic core plus the #510/#514/#517 widenings listed there): the
     * {@code a + b}, {@code daysInPeriod / 365},
     * {@code 1 / x}, and the COMPOUND {@code (daysInNonLeapPeriod / 365) + (daysInLeapYearPeriod / 366)} shapes.
     * The unary prefix form ({@code rawLeft == null}, a negate / pass-through) defers, as does a
     * MULTI-cardinality param operand (the {@code operandMultiParam} face).
     *
     * <p>For the NATIVE-emitted numeric subset (the scalar-param / numeric-literal / alias /
     * nested-arithmetic core — the pre-#517 set): the emitter renders
     * {@code MapperMaths.<R,O,O>method(l, r)} with the operand witness {@code O} and result witness {@code R}
     * derived from the operands' int/number JOIN (divide's result is always {@code BigDecimal}). A numeric
     * literal operand is byte-safe because the emitter re-renders it against the join expected type (an int
     * literal in a number join → {@code BigDecimal.valueOf(N)}); a resolved (param) operand whose kind differs
     * from the join would be coerced, so the emitter DECLINES such a pair. That witness classification, the
     * join, and the coercion gate are Java-emission decisions and live in the emitter, so this adapter arm
     * claims param/literal arithmetic neutrally. The #510/#514/#517 widened operand forms — including
     * the NON-numeric {@code +} shapes (string concatenation) — never reach that native arm: their
     * containing roots route WHOLE through the oracle serves (the shape legs / the kind walk), where
     * legacy's own {@code ArithmeticHandler} derives every witness inside its own composition.
     */
    private Optional<IRExpr> adaptArithmetic(RArithmeticExpr arith, NodeId id, RWorkspace ws) {
        RExpression rawLeft = arith.rawLeft();
        RExpression rawRight = arith.rawRight();
        if (rawLeft == null || rawRight == null) {
            return Optional.empty(); // unary prefix (+/-) — defer (negate / pass-through is a later increment)
        }
        Optional<IRExpr> left = adapt(rawLeft, id.child(0), ws);
        Optional<IRExpr> right = adapt(rawRight, id.child(1), ws);
        if (left.isEmpty() || right.isEmpty()) {
            return Optional.empty(); // an operand is not yet Wave-0-expressible
        }
        if (!isArithmeticOperand(left.get()) || !isArithmeticOperand(right.get())) {
            return Optional.empty(); // scalar-param OR numeric-literal operands only — navs/calls/counts/nesting defer
        }
        return Optional.of(new BinaryOp(toBinOp(arith.op()), left.get(), right.get(), id,
                type(arith, ws), card(arith, ws), Optionality.PRESENT, arith.sourceRange()));
    }

    /**
     * A byte-safe arithmetic operand: a scalar {@code SINGLE} parameter ({@link #isScalarParam}, the L-033
     * case), any {@link IRLiteral} (a numeric {@code int}/{@code number} literal composes natively — the
     * standing pre-#517 admission; a NON-numeric literal — the drr-RULE string-concat class — routes its
     * containing root through the oracle serve via the compiler's non-numeric shape leg), an alias/shortcut
     * reference ({@link IRReference.ReferenceKind#ALIAS} — the {@code daysInPeriod / 365} day-count shape,
     * L-035), or a nested arithmetic {@link BinaryOp} ({@link #isNestedArithmetic} — the COMPOUND day-count
     * shape {@code (daysInNonLeapPeriod / 365) + (daysInLeapYearPeriod / 366)}). The alias's numeric kind
     * (its IR {@code type()} is MISSING upstream) is classified compiler-side by recursing the shortcut body
     * through the legacy classifier, and it renders {@code aliasName(inputs)} with a null expression type →
     * no coercion in operand position. The witness / join / coercion decisions are the emitter's. The #510
     * arm-A5 nav/meta/conditional legs, the #514 output-reference leg and the #517 operand-cluster
     * admissions below widen the set — each rides the oracle-root arithmetic serve (the routing-safety
     * notes inline).
     */
    private static boolean isArithmeticOperand(IRExpr expr) {
        return isScalarParam(expr) || expr instanceof IRLiteral || isAliasReference(expr)
                || isNestedArithmetic(expr)
                // #510 arm-A5: a lowered navigation (operandNav — 96 arithmetic sole at the
                // #509 SOT), a lowered meta-annotated feature access (the drr-RULE
                // string-concat class the metaAccessGate census read at the arithmetic seat
                // — scheme-meta operands of `+`) and a lowered conditional as arithmetic
                // operands. Legacy's ArithmeticHandler derives the witness/join/coercion for
                // every operand form inside its own MapperMaths composition — renders the
                // emitter's native arithmetic arm was never proven for — so the routing
                // safety is the SHAPE leg for the nav/conditional (the compiler's
                // containsOracleLeaf carries the arithmetic-with-a-nav/conditional-operand
                // shapes; bare navs and conditionals keep their standing native composes
                // elsewhere) and the standing kind walk for the meta access (THE original
                // #500 oracle leaf): the containing root renders WHOLE through the
                // oracle-root arithmetic serve (the literal super.visitArithmetic line).
                || expr instanceof FieldAccess || expr instanceof IRMetaAccess
                || expr instanceof IRConditional
                // #514 ARM-1 consumer admission: a lowered function-OUTPUT reference as an
                // arithmetic operand (the notAnInputParam.out.p:RArithmeticExpr decode rows
                // — the FixedAmountCalculation-family compounds) — an oracle leaf (the same
                // routing law: the containing root renders WHOLE through the oracle-root
                // arithmetic serve, legacy's own varPath + MapperMaths composition inside
                // it).
                || expr instanceof IROutputRef
                // #517 — the ARITHMETIC operand cluster (the #2 board family taken nearly
                // whole: operandCall 16 · operandCount 14 · operandItem 16 ·
                // operand:IRLiteral 21 [the non-numeric widening above] ·
                // operand:IRToString 13 · operand:IRSynItemNav 19 ·
                // operand:IRRecordFeatureNav 3 at the #516 SOT). Legacy's ArithmeticHandler
                // derives the witness/join/coercion for EVERY operand form inside its own
                // MapperMaths composition — renders the emitter's native arithmetic arm was
                // never proven for — so the routing safety is the #510 arm-A5 law: the
                // containing root renders WHOLE through the oracle-root arithmetic serve
                // (the literal super.visitArithmetic line). The call, list-op and item
                // operands (native kinds elsewhere) take twin-exact SHAPE legs in the
                // compiler's containsOracleLeaf (each was a 100%-decline face pre-teach —
                // the #504 conservation argument: no standing native compose reroutes);
                // the to-string / synthetic-item-nav / record-feature-read operands are
                // standing ORACLE-LEAF kinds — the kind walk already routes their
                // containing roots. The list-op admission is deliberately KIND-WIDE
                // (COUNT included): the arithmetic seat has NO native list-op compose to
                // protect — unlike the comparison/equality COUNT carve-outs, whose native
                // resultCount wrap this admission does not touch (different operators).
                || expr instanceof IRApply
                || expr instanceof IRListOp
                || isItemOperand(expr)
                || expr instanceof IRToString
                || expr instanceof IRSynItemNav
                || expr instanceof IRRecordFeatureNav;
    }

    /**
     * #517 — whether a lowered operand is the implicit item variable ({@link IRVariable} of
     * {@link IRVariable.VariableKind#USER_ITEM} / {@link IRVariable.VariableKind#SYNTHETIC_ITEM}
     * — the {@code item * 2} lambda-body shapes): the arithmetic operand admission's item test,
     * twin-exact with the compiler's {@code isItemVariableOperand} shape leg (the same kind facts
     * off the same node). Since #528 also the comparison licensing set's SIBLING test (the arm-3
     * numeric-literal pair — cardinality-blind by construction there: an {@code RImplicitVariable}
     * is structurally SINGLE, the per-element binding). PARAM variables stay on
     * {@link #isScalarParam}'s own gate (the cardinality-checked native compose); item variables
     * ride the oracle serve.
     */
    private static boolean isItemOperand(IRExpr expr) {
        return expr instanceof IRVariable var
                && (var.variableKind() == IRVariable.VariableKind.USER_ITEM
                        || var.variableKind() == IRVariable.VariableKind.SYNTHETIC_ITEM);
    }

    /**
     * Whether a lowered operand is itself a nested arithmetic {@link BinaryOp} ({@code + - * /}) — the COMPOUND
     * day-count shape ({@code (daysInNonLeapPeriod / 365) + (daysInLeapYearPeriod / 366)},
     * {@code (360 * (endYear - startYear) + 30 * (endMonth - startMonth) + (endDay - startDay)) / 360}). The
     * nested node was already lowered all-or-nothing by a recursive {@link #adaptArithmetic} (so its own operands
     * are byte-safe arithmetic operands), and the emitter recurses into it: it classifies the nested node's
     * result witness from the nested node's own {@code type()} — the engine-inferred arithmetic result type the
     * outer JOIN consumes, {@code number} for {@code divide} (UNCONSTRAINED) — exactly the kind legacy
     * {@code HandlerHelper.numericOperandKind} feeds the operand-type join (its engine arm), and re-renders the
     * nested {@code MapperMaths.<R,O,O>method(...)} call with a null expression type → no coercion in operand
     * position (the {@link IRReference.ReferenceKind#ALIAS} byte-safety argument, generalized to any null-typed
     * compound render). Comparison / logical {@link BinaryOp}s are EXCLUDED — they yield a {@code ComparisonResult},
     * never a numeric operand; the exhaustive switch forces a decision if a new {@link BinaryOp.BinOp} is added.
     */
    private static boolean isNestedArithmetic(IRExpr expr) {
        return expr instanceof BinaryOp binary
                && switch (binary.op()) {
                    case ADD, SUB, MUL, DIV -> true;
                    case LT, GT, LTE, GTE, EQ, NEQ, AND, OR -> false;
                };
    }

    /** Whether a lowered operand is an alias/shortcut reference ({@link IRReference.ReferenceKind#ALIAS}). */
    private static boolean isAliasReference(IRExpr expr) {
        return expr instanceof IRReference ref
                && ref.referenceKind() == IRReference.ReferenceKind.ALIAS;
    }

    /** Whether a lowered operand is an {@code int} or {@code number} literal (the numeric literal operands). */
    private static boolean isNumericLiteral(IRExpr expr) {
        return expr instanceof IRLiteral lit
                && (lit.literalKind() == IRLiteral.LiteralKind.INT
                        || lit.literalKind() == IRLiteral.LiteralKind.NUMBER);
    }

    /**
     * Lowers an EQUALITY ({@code =} / {@code <>}) to {@link BinaryOp} — the equality sibling of
     * {@link #adaptComparison}, recursing into both operand subtrees all-or-nothing and claiming
     * the node when both operands are byte-safe equality operands ({@link #isEqualityOperand}): a
     * scalar {@link IRVariable.VariableKind#PARAM} or single navigation ({@link #isScalarOperand} — the
     * {@code s1 = s2} / {@code foo -> bar = baz} shapes), a <strong>BOOLEAN or STRING literal</strong> (the
     * {@code <scalar/nav> = True/False} flag-check and {@code <scalar/nav> = "FOO"} code-check shapes — the
     * literal already renders the wrapped {@code MapperS.of(true)} / {@code MapperS.of("FOO")}, so no coercion is
     * threaded, unlike a numeric literal), or a <strong>bare enum-value</strong> (the
     * {@code <scalar/nav> = SomeEnum.VALUE} status-check shape — the emitter
     * wraps the dotted constant {@code MapperS.of(EnumName.VALUE)}, mirroring legacy
     * {@code HandlerHelper.wrapEnumOperand}). A NUMERIC literal operand licenses through the
     * sibling-aware overload ({@link #isEqualityOperand(IRExpr, IRExpr)} — the resolved-sibling
     * sets); an UNRESOLVED bare enum re-qualifies via the sibling's cached enumeration (the
     * #504 {@code requalifiedEnumComparand} mirror of legacy {@code tryBareEnumComparand});
     * the implicit {@code item} and call operands joined at the #518 cluster (the oracle-serve
     * routing — see {@link #isEqualityOperand(IRExpr)}); an explicit {@code all}/{@code any}
     * modifier lowers to the #503 {@link IRAllAnyCompare} (the arm below), the UNMODIFIED
     * operator's default cardinality ({@code All} for {@code =}, {@code Any} for {@code <>})
     * staying the emitter's derivation from {@link BinaryOp#op()}.
     */
    private Optional<IRExpr> adaptEquality(REqualityExpr eq, NodeId id, RWorkspace ws) {
        if (eq.mod().isPresent()) {
            // #503 arm-A1 — the ALL/ANY-modified equality (the allAnyModifier face RESOLVED,
            // 1,166 sole at the #502 SOT across both binary mirrors; the census read ~96%
            // ANY.EQ with BOTH operands lowerable, 100% typed). Lowers to the DISTINCT shallow
            // IRAllAnyCompare (the 24th IRExprKind — the #499 distinct-kind law): childless
            // (the operands are NOT carried — legacy renders the whole modified comparison
            // through its own operator family with the modifier threaded as the
            // CardinalityOperator argument, the unmodified render's implicit default made
            // explicit), admitted ONLY at producesComparisonResult (the census's dominant
            // interior.RLogicalExpr consumers), and every containing claim root renders
            // through the compiler's oracle-root serve (the standing equality/logical
            // dispatch legs — the literal super.visitX lines). Unconditional at the shape:
            // the render is whole-legacy regardless of operand lowerability (the low0/low1
            // census tail rides the same serve).
            return Optional.of(new IRAllAnyCompare(eq.op().name(), eq.mod().get().name(), id,
                    type(eq, ws), card(eq, ws), Optionality.PRESENT, eq.sourceRange()));
        }
        RExpression rawLeft = eq.rawLeft();
        RExpression rawRight = eq.rawRight();
        if (rawLeft == null || rawRight == null) {
            return Optional.empty(); // a then-body elided operand — defer
        }
        Optional<IRExpr> left = adapt(rawLeft, id.child(0), ws);
        Optional<IRExpr> right = adapt(rawRight, id.child(1), ws);
        // #504 arm-C1b: the tryBareEnumComparand mirror — a declining bare reference the
        // linker false-bound to a colliding type/document root re-qualifies against the
        // SIBLING operand's cached enumeration (see requalifiedEnumComparand); the minted
        // ENUM_VALUE then takes the standing native enum-operand render.
        if (left.isEmpty()) {
            left = requalifiedEnumComparand(rawLeft, rawRight, id.child(0), ws);
        }
        if (right.isEmpty()) {
            right = requalifiedEnumComparand(rawRight, rawLeft, id.child(1), ws);
        }
        if (left.isEmpty() || right.isEmpty()) {
            return Optional.empty(); // an operand is not yet Wave-0-expressible
        }
        boolean leftOk = isEqualityOperand(left.get(), right.get())
                || isFilterExtractBoundItemOperand(left.get(), rawLeft)
                || isAdmittedAliasOperand(left.get(), rawLeft, right.get(), ws);
        boolean rightOk = isEqualityOperand(right.get(), left.get())
                || isFilterExtractBoundItemOperand(right.get(), rawRight)
                || isAdmittedAliasOperand(right.get(), rawRight, left.get(), ws);
        if (!leftOk || !rightOk) {
            return Optional.empty(); // scalar/nav, boolean/string/enum, count, a numeric literal with a scalar sibling, or a filter/extract item
        }
        return Optional.of(new BinaryOp(toBinOp(eq.op()), left.get(), right.get(), id,
                type(eq, ws), card(eq, ws), Optionality.PRESENT, eq.sourceRange()));
    }

    /**
     * A byte-safe EQUALITY ({@code =} / {@code <>}) operand, sibling-aware: any non-numeric equality operand
     * ({@link #isEqualityOperand(IRExpr)} — scalar/nav, boolean/string literal, or enum value), a {@code count}
     * list-op ({@link #isCountOperand} — the {@code xs count = 1} shape), OR a numeric ({@code int}/{@code number})
     * literal whose {@code sibling} is a resolved scalar / count operand (the {@code count = 1} / {@code amount <> 0}
     * shapes — legacy threads the sibling's resolved numeric type into the literal's render via
     * {@code ComparisonHandler.inferNumericType}, exactly as for comparison; a {@code count} sibling resolves to
     * {@code Integer}). Two numeric literals ({@code 1 = 2}) defer (no resolved sibling); a numeric literal against an
     * alias / call / multi-nav sibling defers (the sibling fails the {@link #isScalarOperand} + {@link #isCountOperand}
     * gate). The int/number JOIN + {@code BigDecimal.valueOf} coercion + the {@code count} wrap stay in the emitter
     * (the L-029 split).
     */
    private static boolean isEqualityOperand(IRExpr operand, IRExpr sibling) {
        // #507: the numeric-literal sibling set widens to the oracle-leaf navs (the record
        // teach's own exposed frontier — operandNumericLiteral 23+23 at the post-arm probe:
        // `endDay = 30`-family equalities whose other operand is a now-lowered record read);
        // the operand set itself widens through the #505-pattern kind admissions below
        // (isEqualityOperand(IRExpr) already admits both kinds). Identity-serve routing — the
        // sibling's coercion is legacy's own inside the oracle-served literal line.
        return isEqualityOperand(operand) || isCountOperand(operand)
                // #510 arm-A2: a lowered MULTI-cardinality navigation as an equality operand
                // (operandMultiNav — 76 equality sole at the #509 SOT, cdm-FUNCTION-only).
                // Legacy's equality render derives the cardinality modifier's default
                // (All for =, Any for <>) and threads the MapperC join inside its own
                // ComparisonHandler composition — renders the emitter's native equality arm
                // was never proven for (isScalarOperand deliberately kept it SINGLE), so the
                // routing safety is the SHAPE leg: the compiler's containsOracleLeaf carries
                // the equality-with-a-multi-nav-operand shape (NOT the bare FieldAccess kind
                // — single navs keep every standing native compose), and the containing root
                // renders WHOLE through the oracle-root equality serve (the literal
                // super.visitEquality line).
                || isMultiNavOperand(operand)
                || (isNumericLiteral(operand)
                        && (isScalarOperand(sibling) || isCountOperand(sibling)
                                || isOracleNavOperand(sibling)
                                // #511: the numeric-literal sibling set widens to the newly
                                // admitted BinaryOp / non-COUNT list-op operands (the
                                // `a + b = 0` / `xs distinct = 1`-family pairs — the #507
                                // precedent: the admitted kind joins BOTH the operand set
                                // and the licensing set). Identity-serve routing — the
                                // sibling operand itself triggers the oracle route, so the
                                // literal's coercion is legacy's own inside that line.
                                || sibling instanceof BinaryOp
                                || isNonCountListOp(sibling)
                                // #513: the noAdaptArm-sweep kinds join the licensing set
                                // (the #507/#511 precedent — an admitted kind joins BOTH
                                // the operand set and the licensing set). Identity-serve
                                // routing: the sibling triggers the oracle route, so the
                                // literal's coercion is legacy's own inside that line.
                                || isNoAdaptArmSweepKind(sibling)
                                // #514: the OUTPUT-reference sibling joins the equality
                                // licensing set (the paired-licensing law — the operand
                                // set admits the kind via isEqualityOperand).
                                || sibling instanceof IROutputRef
                                // #640 arm G: the bare condition attribute as the literal's sibling (`qty = 0`,
                                // `0 = qty`) - the #529 operand admission's missing pair (the #528 arm-3 law)
                                || sibling instanceof IRImplicitAttrNav));
    }

    /**
     * #510 arm-A2: whether a lowered operand is a MULTI-cardinality plain navigation
     * ({@link FieldAccess} with accumulated {@code MULTI}) — the equality-operand admission's
     * shape test, twin-exact with the compiler's {@code containsOracleLeaf} equality shape leg
     * (both read the same accumulated-cardinality fact off the same node).
     */
    private static boolean isMultiNavOperand(IRExpr expr) {
        return expr instanceof FieldAccess fa && fa.cardinality() == ExpressionCardinality.MULTI;
    }

    /**
     * A byte-safe EQUALITY ({@code =} / {@code <>}) operand: a scalar param / single navigation
     * ({@link #isScalarOperand}), a BOOLEAN literal, or a bare enum-value reference
     * ({@link IRReference.ReferenceKind#ENUM_VALUE}). The boolean literal and the enum value carry NO numeric
     * coercion (a boolean literal renders the already-wrapped {@code MapperS.of(true)}; an enum constant the
     * emitter wraps {@code MapperS.of(EnumName.VALUE)}), unlike a numeric/string literal — so equality admits
     * them while {@link #isScalarOperand}-based comparison ({@code < > <= >=}) does not (ordering an enum /
     * boolean is not a corpus shape).
     */
    private static boolean isEqualityOperand(IRExpr expr) {
        // #503 arm-A2: a lowered to-string conversion joins the equality operand set (the
        // #501 census-narrow lock's own named next gate — operand:IRToString 440 sole at the
        // #502 SOT, the left-vs-literal pairs dominant). The routing safety: IRToString joins
        // containsOracleLeaf, so the containing equality root renders WHOLE through the
        // oracle-root equality serve (the literal super.visitEquality line — legacy's own
        // to-string + coercion composition), and the emitter's nested-IRToString decline keeps
        // a native compose impossible (the standing choke point).
        // #504 arm-A: a lowered point-free reference joins too (operand:IRPointFreeApply,
        // 347 equality sole at the #503 SOT — the census read boolean-vs-literal and
        // enum-vs-enum-reference pairs, RULE-dominant). The routing safety is the SHAPE leg
        // (containsOracleLeaf carries the equality-with-a-pf-operand shape, NOT the bare
        // kind — point-free args and receivers keep their standing native composes): the
        // containing root renders WHOLE through the oracle-root equality serve (the literal
        // super.visitEquality line — legacy's own implicit-invocation + wrap composition).
        // #504 arm-B consumer admission: a lowered un-retypeable synthetic-item nav joins
        // (the mint's own exposed frontier — operand:IRSynItemNav 190 equality sole at the
        // post-arm probe). The kind IS an oracle leaf (containsOracleLeaf carries it), so the
        // containing root always renders whole through the oracle serve — the #501 shallow-kind
        // consumer-admission law.
        return isScalarOperand(expr) || isBooleanOrStringLiteral(expr) || isEnumValueReference(expr)
                || expr instanceof IRToString || expr instanceof IRPointFreeApply
                || expr instanceof IRSynItemNav
                // #508 arm-A5 consumer admission: a lowered meta-sourced USER-item nav as an
                // equality operand (the metaSrcGate census's interior.REqualityExpr rows —
                // the filter-predicate `item -> x = …` class) — the same oracle-leaf routing
                // law.
                || expr instanceof IRMetaItemNav
                // #505 consumer admissions: the choice-option nav and the dispatch-base
                // input as equality operands — both oracle leaves, so the containing root
                // always renders whole through the oracle-root equality serve (the #501
                // shallow-kind consumer-admission law).
                || expr instanceof IRChoiceOptionNav
                || expr instanceof IRDispatchInputRef
                // #507 consumer admissions: the deep-path nav and the record-feature read as
                // equality operands — both oracle leaves (the same routing law).
                || expr instanceof IRDeepFeatureNav
                || expr instanceof IRRecordFeatureNav
                // #512 arm-B2 consumer admission: the item-qualifier read as an equality
                // operand (the nsrGate census's interior.REqualityExpr claim roots — the
                // `item -> scheme = "…"` extract-if class) — an oracle leaf (the same
                // routing law: the containing root always renders whole through the
                // oracle-root equality serve).
                || expr instanceof IRQualifierItemNav
                // #529 consumer admission: the synthesized-receiver bare attribute as an
                // equality operand (the #528 mint's own disclosed frontier —
                // operand:IRImplicitAttrNav 2 at the #528 SOT; the eqImplGate census read
                // the class 100% s:L.sib:IRLiteral — the `filter intentToClear = True`-
                // family bare-attr-vs-literal predicates, witness MapIntent). The kind is
                // already an oracle leaf KIND-WIDE (the #528 containsOracleLeaf leg), so
                // the containing root always renders whole through the oracle-root
                // equality serve (the literal super.visitEquality line — legacy's own
                // implicit-receiver synthesis + coercion composition inside it).
                || expr instanceof IRImplicitAttrNav
                // #525 consumer admission: the receiver-carrying qualifier read as an
                // equality operand (the `filter partyReference -> reference = …` /
                // `filter identifier -> scheme = "…"` predicate class — the #512 twin's
                // one-hop-deeper sibling) — an oracle leaf (the same routing law).
                || expr instanceof IRQualifierReceiverNav
                // #529 consumer admission: the receiver-carrying choice-option read as an
                // equality operand (the declMiss teach's chain shells — the #525 insurance
                // pattern at the new kind) — an oracle leaf (the same routing law).
                || expr instanceof IRChoiceReceiverNav
                // #510 arm-A1: a lowered meta-annotated feature access as an equality operand
                // (operand:IRMetaAccess — 78 equality sole at the #509 SOT; the metaAccessGate
                // census read the class 100% low2/typed: meta-vs-meta pairs — reference=
                // reference, scheme=scheme, location=address — plus the mixed-shape tails).
                // The kind is THE original #500 oracle leaf, so the containing root always
                // renders whole through the oracle-root equality serve (the literal
                // super.visitEquality line — legacy's own meta-deref + coercion composition
                // inside it); no shape leg is needed, the standing kind walk routes.
                || expr instanceof IRMetaAccess
                // #511: a lowered nested binary (operand:BinaryOp — 58 equality sole at the
                // #510 SOT: the `a + b = c` / `a + b = 0` compound-vs-value class) and a
                // lowered non-COUNT list op (operand:IRListOp — 34 equality sole) join.
                // Legacy's ComparisonHandler composes the nested operand render + the
                // coercion/witness threading inside its own line — renders the emitter's
                // native equality arm was never proven for — so the routing safety is the
                // SHAPE legs (the compiler's containsOracleLeaf carries the
                // equality-with-a-BinaryOp / non-COUNT-list-op-operand shapes, twin-exact
                // with these admissions): the containing root renders WHOLE through the
                // oracle-root equality serve (the literal super.visitEquality line). COUNT
                // stays OUT of the list-op leg — `xs count = 1` is the standing NATIVE
                // compose (isCountOperand + the emitter's resultCount wrap), and the
                // exclusion keeps it composing natively; both new shapes were 100%-decline
                // faces pre-teach, so no standing native compose reroutes (the #504
                // conservation argument).
                || expr instanceof BinaryOp || isNonCountListOp(expr)
                // #513: the noAdaptArm-sweep kinds join the equality operand set (the
                // sweep families' pre-arm faces hid every consumer seat behind the
                // minimal-blocker attribution — the child family carried the face — so the
                // admissions are the sweep's own exposed frontier, taken up front). All
                // four are oracle leaves with NO emitter arm, so the routing safety is the
                // standing law: the containing root renders WHOLE through the oracle-root
                // equality serve (the literal super.visitEquality line — legacy's own
                // operand composition inside it), and a native compose can never reach
                // them.
                || isNoAdaptArmSweepKind(expr)
                // #514 ARM-1 consumer admission: a lowered function-OUTPUT reference as an
                // equality operand (the notAnInputParam.out.p:REqualityExpr decode rows) —
                // an oracle leaf (the same routing law: the containing root renders WHOLE
                // through the oracle-root equality serve, legacy's own varPath render
                // inside it).
                || expr instanceof IROutputRef
                // #514 ARM-2 consumer admission: the top-level rule-input nav as an equality
                // operand (the ruleInputNav.p:REqualityExpr decode rows — the drr RULE
                // dominant seat) — an oracle leaf (the same routing law).
                || expr instanceof IRRuleInputNav
                // #518 — the EQUALITY operand cluster (the #517 arithmetic pattern at the
                // equality seat; the #3 board family taken whole at the #517 SOT:
                // operandItem 24 · operand:Existence 16 · operand:IRListConstruct 3 ·
                // operandMultiCall 3 · operandMultiParam 2+2 · operand:IRConversion 2 ·
                // operand:IRLambdaOp 1+1). Legacy's ComparisonHandler composes every
                // operand render + the cardinality-modifier default + the coercion/witness
                // threading inside its own line — renders the emitter's native equality
                // arm was never proven for — so the routing safety is the twin-exact SHAPE
                // legs in the compiler's containsOracleLeaf (item / Existence /
                // list-construct / MULTI-call / MULTI-param / lambda-op), and the standing
                // kind walk for IRConversion (a #501 oracle-leaf kind — no leg needed).
                // The call leg is the MULTI slice ONLY ({@link #isMultiCallOperand} — the
                // #510 multi-nav pattern at the CALL kind): a SINGLE call is the STANDING
                // isScalarOperand native admission whose claims ride the proven compose +
                // the postPin scan (the #471–#474/#516 serve arms) — a kind-wide leg would
                // hijack that class into the oracle exemption and migrate the
                // postPinServeLowered receipt (caught by the #516 router witness at arm
                // time). Every shape except the item was a 100%-decline face pre-teach
                // (the #504 conservation argument — no standing native compose reroutes);
                // the ITEM leg is kind-wide and REROUTES the standing
                // filter/extract-bound native composes to the oracle serve BY IDENTITY
                // (the #504 arm-C1a/C2 precedent: the leg cannot key the bound subset from
                // the lowered tree — the binding is a RAW-side fact — so the serve carries
                // the moved mass on the oracleRootLowered receipt; the serve IS legacy's
                // own line, bytes cannot move). isFilterExtractBoundItemOperand stays
                // composed at the gate for ladder fidelity — isItemOperand subsumes it.
                || isItemOperand(expr)
                || expr instanceof Existence
                || expr instanceof IRListConstruct
                || isMultiCallOperand(expr)
                || isMultiParamOperand(expr)
                || expr instanceof IRConversion
                || expr instanceof IRLambdaOp;
    }

    /**
     * #518 — whether a lowered operand is a MULTI-cardinality call (the
     * {@code operandMultiCall} equality faces — the drr-RULE list-returning invocations):
     * the equality-operand admission's MULTI-call test, twin-exact with the compiler's
     * {@code isMultiCallOperand} shape leg (the same kind + accumulated-cardinality facts
     * off the same node — the #510 {@link #isMultiNavOperand} pattern at the CALL kind).
     * SINGLE calls stay on {@link #isScalarOperand}'s own standing admission (the proven
     * native compose + the postPin-scan route — the #471–#474/#516 serve arms); MULTI
     * calls ride the oracle serve.
     */
    private static boolean isMultiCallOperand(IRExpr expr) {
        return expr instanceof IRApply apply
                && apply.cardinality() == ExpressionCardinality.MULTI;
    }

    /**
     * #518 — whether a lowered operand is a MULTI-cardinality PARAM variable (the
     * {@code operandMultiParam} equality faces — {@code values = …} list-vs-list pairs):
     * the equality-operand admission's MULTI-param test, twin-exact with the compiler's
     * {@code isMultiParamOperand} shape leg (the same kind + accumulated-cardinality facts
     * off the same node — the #510 {@link #isMultiNavOperand} pattern at the PARAM kind).
     * SINGLE params stay on {@link #isScalarParam}'s own gate (the proven native compose);
     * MULTI params ride the oracle serve (legacy derives the modifier default + the MapperC
     * join inside its own composition).
     */
    private static boolean isMultiParamOperand(IRExpr expr) {
        return expr instanceof IRVariable var
                && var.variableKind() == IRVariable.VariableKind.PARAM
                && var.cardinality() == ExpressionCardinality.MULTI;
    }

    /**
     * #513: the noAdaptArm-sweep kinds ({@link IRSwitchOp}/{@link IRDefaultOp}/
     * {@link IRMembershipOp}/{@link IRCollectOp}) — the shared consumer-admission shape
     * test (equality/comparison/existence operands, the numeric-literal licensing sets and
     * the call-arg gate all admit the same four), twin-exact with the compiler's
     * {@code containsOracleLeaf} walk (the same kind facts off the same nodes). All four
     * are SHALLOW oracle leaves with no emitter arm — served only at oracle-rendered
     * roots, never natively composed (the #499 law's uniform routing safety).
     */
    private static boolean isNoAdaptArmSweepKind(IRExpr expr) {
        return expr instanceof IRSwitchOp || expr instanceof IRDefaultOp
                || expr instanceof IRMembershipOp || expr instanceof IRCollectOp;
    }

    /**
     * Whether a lowered operand is a BOOLEAN ({@code True}/{@code False}) or STRING ({@code "FOO"}) literal — the
     * two non-numeric literal kinds, both byte-safe in equality because their leaf already renders the wrapped
     * {@code MapperS.of(true)} / {@code MapperS.of("FOO")} with NO coercion (legacy {@code LiteralHandler} ignores
     * the expected type for both, and {@code literalSiblingNumericType} fires only for an {@code RIntLiteral}). A
     * NUMERIC literal is excluded HERE — it threads the sibling's numeric type / a {@code BigDecimal.valueOf}
     * coercion, so it is admitted only through the sibling-aware {@link #isEqualityOperand(IRExpr, IRExpr)} /
     * {@link #isComparisonOperand(IRExpr, IRExpr)} gate (its sibling must be a resolved scalar operand).
     */
    private static boolean isBooleanOrStringLiteral(IRExpr expr) {
        return expr instanceof IRLiteral lit
                && (lit.literalKind() == IRLiteral.LiteralKind.BOOLEAN
                        || lit.literalKind() == IRLiteral.LiteralKind.STRING);
    }

    /** Whether a lowered operand is a bare enum-value reference ({@link IRReference.ReferenceKind#ENUM_VALUE}). */
    private static boolean isEnumValueReference(IRExpr expr) {
        return expr instanceof IRReference ref
                && ref.referenceKind() == IRReference.ReferenceKind.ENUM_VALUE;
    }

    /**
     * Lowers a LOGICAL {@code and}/{@code or} to {@link BinaryOp} — completing the boolean binary
     * tier by <em>composing</em> the comparison/equality leaves. Recurses into both operand subtrees
     * all-or-nothing and claims the node ONLY when each operand lowers to a boolean {@link BinaryOp}
     * (a comparison, equality or nested logical — i.e. something that renders as a
     * {@code ComparisonResult}, see {@link #producesComparisonResult}). That gate is the byte-safety
     * linchpin: the legacy {@code LogicalHandler} emits {@code left.andNullSafe(right)} /
     * {@code orNullSafe} — instance methods on a {@code ComparisonResult} — so a non-ComparisonResult
     * operand (a bare boolean variable rendering {@code MapperS.of(flag)}, a boolean literal, a
     * boolean-function invocation needing {@code ComparisonResult.ofNullSafe} coercion, a {@code then}-chain)
     * would not compile and is left to legacy. Unlike comparison/equality there is no cardinality
     * modifier to lower ({@link RLogicalExpr} carries none — the #503 {@code IRAllAnyCompare} arm
     * has no logical seat).
     */
    private Optional<IRExpr> adaptLogical(RLogicalExpr log, NodeId id, RWorkspace ws) {
        RExpression rawLeft = log.rawLeft();
        RExpression rawRight = log.rawRight();
        if (rawLeft == null || rawRight == null) {
            return Optional.empty(); // a then-body elided operand — defer
        }
        Optional<IRExpr> left = adapt(rawLeft, id.child(0), ws);
        Optional<IRExpr> right = adapt(rawRight, id.child(1), ws);
        if (left.isEmpty() || right.isEmpty()) {
            return Optional.empty(); // an operand is not yet Wave-0-expressible
        }
        if (!producesComparisonResult(left.get()) || !producesComparisonResult(right.get())) {
            return Optional.empty(); // operands must render to ComparisonResult — see method docs
        }
        return Optional.of(new BinaryOp(toBinOp(log.op()), left.get(), right.get(), id,
                type(log, ws), card(log, ws), Optionality.PRESENT, log.sourceRange()));
    }

    /**
     * Lowers a unary EXISTENCE check ({@code <arg> exists} / {@code is absent} / {@code single}/
     * {@code multiple} exists) to {@link Existence} — the first unary structural lowering, recursing
     * into the single operand subtree all-or-nothing. It claims a check when the operand is a byte-safe
     * existence operand ({@link #isExistenceOperand} — a function-parameter or a {@link FieldAccess}
     * navigation result, of <em>either</em> cardinality: {@code foo exists}, {@code xs exists},
     * {@code foo -> bar exists}, {@code trade -> legs exists}). Existence is operand-cardinality-agnostic
     * (legacy {@code ExistenceHandler} wraps whatever the operand renders — {@code MapperS.of} or
     * {@code MapperC.<Item>of} / a {@code mapC} chain — in the same {@code {method}(arg)} call), so a multi
     * operand is byte-safe (the witness/meta/collision cases are already deferred when the operand is
     * lowered). A function CALL operand is ALSO admitted now ({@code someFunc(args) exists} — the L-105
     * call-as-existence-operand slice, see {@link #isExistenceOperand}). Since #519 the implicit-{@code item}
     * operands admit KIND-WIDE ({@link #isItemOperand} — the oracle-serve routing) and the {@code then}/
     * {@code filter}-elided null argument SYNTHESIZES ({@link #synthesizeElidedExistenceOperand} — the
     * #464-era lab-port "later increment" landed at #519: legacy substitutes its own synthetic implicit
     * inside the serve's re-walk, so the synthesized IR operand is accounting-only). The {@code op} and optional {@code modifier}
     * are carried through verbatim; the emitter folds them into the method name.
     */
    private Optional<IRExpr> adaptExistence(RExistenceExpr exist, NodeId id, RWorkspace ws) {
        RExpression rawArg = exist.argument();
        if (rawArg == null) {
            // #519: the then/filter-elided implicit operand SYNTHESIZES (`… then exists` — the
            // existence builder carries a genuinely-null argument at this seat, UNLIKE the
            // list-op builder's syntheticImplicitInput; a parser change would move the legacy/
            // OFF render path, so the synthesis is adapter-side, IR-only). The SYNTHETIC_ITEM
            // is typed from the nearest enclosing lambda's own pipe source (the #479
            // Category-8 law applied at synthesis — `item` ranges over the source), and the
            // containing root ALWAYS renders whole-legacy: the synthesized variable trips the
            // compiler's KIND-WIDE Existence-item oracle leg, and the serve re-walks the RAW
            // node — legacy's own implicit handling renders (byte-identical BY IDENTITY; the
            // IR operand is accounting-only). The #469/#479 item-renderer indexes never carry
            // the node (no RAW RImplicitVariable behind it), so a drift arrival at any native
            // arm declines — never a guessed binding.
            Optional<IRVariable> synthesized = synthesizeElidedExistenceOperand(exist, id, ws);
            if (synthesized.isEmpty()) {
                return Optional.empty(); // no enclosing lambda / untyped source — the honest residue
            }
            return Optional.of(new Existence(toExistOp(exist.op()),
                    toExistMod(exist.modifier().orElse(null)), synthesized.get(), id,
                    type(exist, ws), card(exist, ws), Optionality.PRESENT, exist.sourceRange()));
        }
        Optional<IRExpr> arg = adapt(rawArg, id.child(0), ws);
        if (arg.isEmpty()) {
            return Optional.empty(); // the operand is not yet Wave-0-expressible
        }
        // #504 arm-A: a lowered point-free reference joins the existence operand set
        // (operand:IRPointFreeApply, 142 existence sole at the #503 SOT — `SomeFunc exists`,
        // output-type-agnostic exactly like every existence operand). The routing safety is
        // the SHAPE leg (containsOracleLeaf carries the existence-with-a-pf-operand shape):
        // the containing root renders WHOLE through the oracle-root existence serve (the
        // literal super.visitExistence line — legacy's own implicit-invocation + wrap).
        // #504 arm-B consumer admission: a lowered un-retypeable synthetic-item nav joins
        // (operand:IRSynItemNav 33 existence sole at the post-arm probe) — the kind is an
        // oracle leaf, so the containing root always oracle-serves (the #501 law).
        if (!(isExistenceOperand(arg.get()) || isFilterExtractBoundItemOperand(arg.get(), rawArg)
                || isAdmittedAliasOperand(arg.get(), rawArg, null, ws)
                || arg.get() instanceof IRPointFreeApply
                || arg.get() instanceof IRSynItemNav
                // #508 arm-A5 consumer admission: a lowered meta-sourced USER-item nav as an
                // existence operand (the metaSrcGate census's interior.RExistenceExpr rows)
                // — the same oracle-leaf routing law.
                || arg.get() instanceof IRMetaItemNav
                // #505 consumer admissions: the choice-option nav and the dispatch-base
                // input as existence operands — both oracle leaves (the same routing law).
                || arg.get() instanceof IRChoiceOptionNav
                || arg.get() instanceof IRDispatchInputRef
                // #507 consumer admissions: the deep-path nav and the record-feature read as
                // existence operands — both oracle leaves (the same routing law).
                || arg.get() instanceof IRDeepFeatureNav
                || arg.get() instanceof IRRecordFeatureNav)) {
            return Optional.empty(); // a param/navigation operand of ANY cardinality, a filter/extract item, or a proven alias — byte-safe
        }
        return Optional.of(new Existence(toExistOp(exist.op()), toExistMod(exist.modifier().orElse(null)),
                arg.get(), id, type(exist, ws), card(exist, ws), Optionality.PRESENT, exist.sourceRange()));
    }

    /**
     * #519 — the decoded binding for an ELIDED existence operand: either a decline facet (the
     * walk's own honest stop key — the facet-first law) or the enclosing lambda's pipe source.
     * {@code elementBound} distinguishes the per-element binders (filter/extract — the implicit
     * ranges over ONE element, SINGLE) from the {@code then} binder (the WHOLE piped value —
     * the source's own cardinality). ONE walk IMPLEMENTATION serves both callers — the
     * {@link #adaptExistence} arm and the {@link #reasonForExistence} mirror each invoke it
     * independently (the #489 one-walk law: a single shared helper so the arm and its mirror
     * can never drift, not a shared/cached execution).
     */
    private record ElidedImplicitBinding(String declineFacet, RExpression source, boolean elementBound) {}

    /**
     * #519 — the nearest-enclosing-lambda walk for an elided existence operand. Mirrors the
     * {@link #isFilterOrExtractBound} stop-at-the-first-lambda contract (an implicit is bound by
     * its NEAREST enclosing lambda — never skip an inner lambda to an outer one) and the bounded
     * parent-walk convention; the lambda's OWNER kind names the binder semantics ({@code then} /
     * {@code filter} / {@code extract} — any other owner declines with its own facet, the
     * drift-visible residue).
     */
    private static ElidedImplicitBinding elidedImplicitBinding(RExistenceExpr exist) {
        RNode cur = exist.parent();
        int depth = 0;
        while (cur != null && depth++ < PARENT_WALK_LIMIT) {
            if (cur instanceof RInlineFunction inline) {
                // The nearest lambda must be an IMPLICIT-binding form (bare-implicit or the
                // empty-param bracket — the same disjunction the item-binding walks use): a
                // NAMED-parameter lambda binds NO implicit, so legacy's implicit resolution
                // would not bind here and a synthesis from THIS lambda's pipe source would
                // state the wrong fact — decline with the dedicated facet instead (the
                // Copilot R2 gate; the honest residue).
                if (!(inline.isImplicit() || inline.paramNames().isEmpty())) {
                    return new ElidedImplicitBinding("elidedOperand.namedLambda", null, false);
                }
                RNode p = inline.parent();
                if (p instanceof RThenExpr then && then.body().orElse(null) == inline) {
                    return new ElidedImplicitBinding(null, then.argument(), false);
                }
                if (p instanceof RFilterExpr filter && filter.body() == inline) {
                    return new ElidedImplicitBinding(null, filter.argument(), true);
                }
                if (p instanceof RExtractExpr extract && extract.body() == inline) {
                    return new ElidedImplicitBinding(null, extract.argument(), true);
                }
                return new ElidedImplicitBinding("elidedOperand.lambdaOf:"
                        + (p == null ? "null" : p.getClass().getSimpleName()), null, false);
            }
            cur = cur.parent();
        }
        return new ElidedImplicitBinding("elidedOperand.noEnclosing", null, false);
    }

    /**
     * #519 — synthesize the elided existence operand: a {@code SYNTHETIC_ITEM} variable typed
     * from the enclosing lambda's pipe source (the #479 Category-8 law applied at synthesis —
     * the implicit ranges over the source, so the source's own cached item type IS the
     * implicit's type; a {@code then} binder additionally carries the source's cardinality,
     * the whole-piped-value semantics). Declines (empty) when the walk stops or the source is
     * engine-untyped — the {@link #reasonForExistence} mirror names both stops with their own
     * facets. The variable carries the EXISTENCE's own source range (the synthesizing
     * construct — the documented collision convention protects the #479 synthetic-renderer
     * index) and the operand-slot {@code NodeId} the non-elided path uses.
     */
    private Optional<IRVariable> synthesizeElidedExistenceOperand(RExistenceExpr exist, NodeId id,
            RWorkspace ws) {
        ElidedImplicitBinding binding = elidedImplicitBinding(exist);
        if (binding.declineFacet() != null) {
            return Optional.empty();
        }
        RMetaAnnotatedType sourceType = type(binding.source(), ws);
        if (sourceType == null || sourceType.isMissing()) {
            return Optional.empty(); // the pipe source is engine-untyped — the honest residue
        }
        ExpressionCardinality cardinality = binding.elementBound()
                ? ExpressionCardinality.SINGLE
                : card(binding.source(), ws);
        return Optional.of(new IRVariable("item", IRVariable.VariableKind.SYNTHETIC_ITEM,
                id.child(0), sourceType, cardinality, Optionality.PRESENT, exist.sourceRange()));
    }

    /**
     * Lowers a feature navigation ({@code receiver -> feature}) to {@link FieldAccess}, including a
     * navigation <em>chain</em> ({@code a -> b -> c}) whose receiver is itself a navigation. It claims
     * the dominant getter arm of {@code NavigationHandler} — a resolved, non-meta getter feature (SINGLE renders {@code map}, MULTI renders {@code mapC}, per hop) on a
     * plain data type — off a <em>scalar function-parameter base</em> or a <em>single non-meta
     * navigation hop</em>, so a chain composes by recursion (the emitter walks the receiver subtree and
     * appends one getter step, every hop's lambda variable disambiguating independently against the
     * shared render scope). That gate auto-defers every shape whose legacy render the neutral emitter
     * cannot (yet) reproduce:
     * <ul>
     *   <li>an unresolved feature (a disguised enum-value/choice navigation, or a built-in
     *       <em>record</em> feature like {@code dateTime -> date} with no getter) — defer;</li>
     *   <li>a {@code [metadata …]} feature (the {@code ReferenceWithMetaX}/{@code FieldWithMetaX}
     *       wrapper + meta-strip coercion) — defer;</li>
     *   <li>a receiver that is neither a function parameter (of either cardinality, including a MULTI
     *       <em>parameter</em> base {@code legs -> rate}), a navigation hop ({@code FieldAccess}), a
     *       call ({@code IRApply}/{@code IRPointFreeApply}), nor — since #511 — ANY list op
     *       (kind-wide: the #499 collapse class keeps the emitter's NATIVE render, its
     *       {@code IRJavaLeafEmitter.isCollapseNavBase} boundary now the compiler's
     *       oracle-routing shape-leg key for every other kind/child): an alias receiver still
     *       drives a different lambda-variable / receiver-typing / facet-conditional path —
     *       defer (see
     *       {@link #isNavigableReceiver}). The implicit-{@code item} receivers are admitted per slice:
     *       the filter/extract-bound USER {@code item} ({@link #isFilterExtractBoundItemReceiver}) and,
     *       since #479, the filter/extract-bound SYNTHETIC item retyped at the boundary
     *       ({@link #retypedSyntheticFilterExtractItem}); every other item shape defers;</li>
     * </ul>
     * <p>The rare witness/output simple-name collision (FQN-inline witness render) is resolved in the
     * emitter against the enclosing function's output Java type (L-029), not deferred here.
     * The emitter composes {@code receiver.<Witness>map("getFeature", v -> v.getFeature())} from the
     * receiver subtree + the node's result {@code type} (the witness) + cardinality (map/mapC).
     */
    private Optional<IRExpr> adaptFeatureCall(RFeatureCall fc, NodeId id, RWorkspace ws) {
        RExpression rawReceiver = fc.receiver();
        if (rawReceiver == null) {
            return Optional.empty(); // a then/filter-elided receiver — defer
        }
        RAttribute feature = fc.resolvedFeature().orElse(null);
        if (feature == null) {
            // #503 arm-B2 — the closure-param-head by-name feature resolution (the
            // featureUnresolved.headUnresolved face's dominant class, the #502-exposed
            // parent-gate: the head lowers as a closure param since #502 but the linker never
            // typed the param, so the FEATURE stayed unresolved; the census read
            // closureParamName.twData:<Type>.memberHit dominant — the binder-source walk
            // proves the element type AND the by-name lookup hits). The resolution mirrors
            // the #486 itemAttr law at the closure-param head: the param's value is the
            // binder SOURCE's element (Cat-8; a then-binder param takes the whole argument
            // value — element ≡ value on the walked derivation), so the feature resolves by
            // name against sourceElementDataType(source) — gated on the SAME two proofs the
            // #502 retype uses (the L-029 allowlist on the source + the structural
            // derivation, prove-or-decline) and on the #502 arm's own head identity (either
            // bind flavor, behind the alias-match precedence: a shortcut-colliding head is
            // legacy's alias render — the #453-family exclusion the #502 arm establishes).
            if (rawReceiver instanceof RSymbolReference head && head.name() != null
                    && fc.featureName() != null
                    && isClosureParamHead(head)) {
                RDataType elem = closureParamElementType(head, head.name());
                if (elem == null) {
                    // #509 arm-B2 — the UNPROVEN-source element leg (the headUnGate census
                    // read the cpBind residue 40/40 symNull + srcUnproven + twData + the
                    // member resolving PLAIN): the #503 walk's strong allowlist declines
                    // meta-wrapped/unprovable binder sources BY DESIGN, but the resolution
                    // needs only the element's NAME UNIVERSE — and the render is byte-safe
                    // REGARDLESS of the proof, because the head lowers to IRClosureParam (an
                    // ORACLE LEAF: the claim root and every containing root render the
                    // literal legacy line BY IDENTITY — legacy re-derives the binding and
                    // every FieldWithMetaX deref itself; the #508 arm-A4 recognition at the
                    // parsed seat). The structural derivation alone resolves the element
                    // (prove-or-decline stays: an underivable source keeps declining).
                    RExpression unprovenSrc = closureParamBinderSource(head, head.name());
                    elem = unprovenSrc == null ? null : sourceElementDataType(unprovenSrc);
                }
                RAttribute resolved = elem == null ? null
                        : findAttributeOnDataTypeByName(elem, fc.featureName());
                if (resolved != null) {
                    feature = resolved;
                }
            }
            // #503 arm-B1 — the CHOICE-OPTION selection (the featureUnresolved.
            // nonSymbolReceiver face's dominant class: the census read the receivers'
            // cached types ct:RChoiceTypeRef:Payout/Underlier/Asset/… — `asset ->
            // Instrument -> Security`-style option navigation by the option's TYPE-NAME
            // spelling, which the linker's lexical passes never resolve). The resolution
            // mirrors legacy NavigationHandler's own recovery (the resolvedFeature-unset
            // RChoice arms): the option resolves by name against the choice's
            // RDataType PROJECTION (RChoiceTypeRef.asRDataType — the option's typeCall IS
            // its identity, the type name the attribute name, the option's meta
            // annotations carried), and the projected attribute's declared facts feed the
            // SAME FieldAccess build every resolved nav takes (a meta-annotated option
            // routes through the #499 meta arm exactly like a meta attribute).
            if (feature == null && fc.featureName() != null && ws != null) {
                RMetaAnnotatedType recvType = type(rawReceiver, ws);
                if (recvType != null && !recvType.isMissing()
                        && recvType.type() instanceof RChoiceTypeRef choiceRef) {
                    RDataType projected = choiceRef.asRDataType();
                    RAttribute option = projected == null ? null
                            : findAttributeOnDataTypeByName(projected, fc.featureName());
                    if (option != null) {
                        feature = option;
                    }
                }
                // #509 arm-B4 — the CHOICE-SUPERTYPE option selection (the nsrGate census
                // read the dt:TransferableProduct.mMiss class 33: `-> Instrument` over a
                // DATA-typed receiver whose ANCESTOR is a choice — `type TransferableProduct
                // extends Asset` where Asset is a `choice`). RDataType.superType() is EMPTY
                // for an extends-choice relationship (the resolved choice node is held by
                // choiceSuperType(), a SEPARATE accessor), so the #503 arm-B1 direct-choice
                // recovery and the data-supertype member walk both miss the option. The
                // resolution mirrors legacy NavigationHandler.findChoiceSuperOption (the PR
                // #207 witnessDrop lever — fallbackResolveFeature's own second step): walk
                // the data-supertype chain, at each level match the (type-named) feature
                // against the choice supertype's options BY TYPE NAME, and project the hit
                // exactly as RChoiceTypeRef.asRDataType does. The projected attribute feeds
                // the SAME FieldAccess/meta build every resolved nav takes (the render class
                // is the #503 ring-proven option-getter compose over the lowered receiver —
                // the census read every live receiver recvLowers:FieldAccess + typed).
                if (feature == null && recvType != null && !recvType.isMissing()
                        && recvType.type() instanceof RDataTypeRef recvDataRef) {
                    RAttribute superOption =
                            findChoiceSuperOption(recvDataRef.astNode(), fc.featureName());
                    if (superOption != null) {
                        feature = superOption;
                    }
                }
            }
            // #505 arm-C — the dispatch-base-input head recovery (the interior slice of the
            // dispatchGate census: the nav `<baseInput> -> <feature>` inside a variant body
            // declines because the head never resolves, so the FEATURE stayed unresolved
            // too). The head's base input resolves through the shared class gate
            // (dispatchBaseInputOf) and the feature by name against the input's DECLARED
            // data type (the #503 arm-B2 pattern at the dispatch head; a record-typed input
            // — the baseInput:date slice — has no RDataType and keeps declining, the honest
            // record-leaf residue).
            if (feature == null && rawReceiver instanceof RSymbolReference dispatchHead
                    && dispatchHead.symbol().isEmpty() && fc.featureName() != null) {
                RAttribute baseInput = dispatchBaseInputOf(dispatchHead);
                RDataType headData = baseInput == null ? null : declaredDataTypeOf(baseInput);
                RAttribute resolved = headData == null ? null
                        : findAttributeOnDataTypeByName(headData, fc.featureName());
                if (resolved != null) {
                    feature = resolved;
                }
            }
            // #524 — the EMPTY-symbol bare-item HEAD recovery (the headUnGate census's
            // headLowers:IRSynItemNav class: the synthesized disguised-chain equivalents
            // whose heads the #524 admission lowers — the drr positionBase/tradableProduct
            // rule chains + the cdm6 Asset/Index nested-choice chains). The head resolves
            // through the SAME shared ladder the #524 arm runs (bareItemMemberResolution —
            // the two seats cannot drift), and the LEAF resolves by name against the head
            // attribute's DECLARED element form: the declared data type first (attr-by-name
            // then super-option — the arm's own order), else the declared CHOICE's options
            // BY TYPE NAME (the #503 arm-B1 projection at the DECLARED seat — the engine's
            // ct channel never types these synthesized heads). The resolved feature feeds
            // the SAME build every resolved nav takes; the head's own lowering composes as
            // the receiver, and a containing root carrying the IRSynItemNav oracle leaf
            // renders whole-legacy BY IDENTITY (the routing safety). Since #525 the
            // qualifier-LEAF class over IRMetaAccess heads mints the distinct
            // IRQualifierReceiverNav (the member walk stays FIRST — the leg below);
            // prove-or-decline holds everywhere else.
            if (feature == null && rawReceiver instanceof RSymbolReference bareHead
                    && bareHead.symbol().isEmpty() && bareHead.args().isEmpty()
                    && fc.featureName() != null) {
                BareItemResolution headRes = bareItemMemberResolution(bareHead);
                RAttribute headAttr = headRes == null ? null : headRes.attr();
                if (headAttr != null) {
                    RDataType headData = declaredDataTypeOf(headAttr);
                    if (headData == null) {
                        // A PROJECTED option head deep-copies its typeCall WITHOUT
                        // resolution state — the element reads from the ATTACHED
                        // original (the #521 Copilot-R2 law, the arm's own channel).
                        headData = headRes.provenOption();
                    }
                    RAttribute resolved = null;
                    if (headData != null) {
                        resolved = findAttributeOnDataTypeByName(headData, fc.featureName());
                        if (resolved == null) {
                            resolved = findChoiceSuperOption(headData, fc.featureName());
                        }
                    } else if (headRes.provenChoice() != null) {
                        // The CHOICE-typed head (the Asset/Index nested-choice chains) —
                        // the leaf selects the ATTACHED choice's option by TYPE NAME (the
                        // #503 arm-B1 projection at the DECLARED seat).
                        for (RChoiceOption option : headRes.provenChoice().options()) {
                            RTypeCall optionCall = option.typeCall();
                            if (optionCall != null
                                    && fc.featureName().equals(optionCall.typeName())) {
                                resolved = choiceOptionAsAttribute(option);
                                break;
                            }
                        }
                    }
                    if (resolved != null) {
                        feature = resolved;
                    } else if (recordLeafOf(headAttr, fc.featureName()) != null) {
                        // #529 — the RECORD leaf over the lowered member head (the
                        // itemHeadGate census's leaf:recHit:zonedDateTime class, 11 of the
                        // headMiss.other 38 — the `value -> date` / `timestamp -> date`
                        // reads whose implicit-item member head declares a RECORD type;
                        // witnesses MapCommodityPhysicalEuropeanExercise / GetValuation /
                        // MapFxOptionToSettlementTerms). The leaf resolves on the head
                        // attribute's DECLARED record (the #444 membership rule — a record
                        // head has no data members, so this leg slots between the member
                        // walk and the qualifier leg in legacy's own type-features-first
                        // order), the head lowers by the #524 admission's own gate, and the
                        // nav mints the receiver-carrying IRRecordReceiverNav (the #512-B1
                        // kind at the DECLARED channel — the engine never types these
                        // synthesized heads, so the #512-B1 engine-type gate can never
                        // spell them; same kind, disjoint channel, no double-mint). An
                        // ORACLE LEAF — every containing root renders whole-legacy
                        // (containsOracleLeaf), the record ladder legacy's own inside it.
                        RRecordType headRec = declaredRecordTypeOf(headAttr);
                        Optional<IRExpr> headLowered = adapt(bareHead, id.child(0), ws);
                        if (headLowered.isPresent()) {
                            return Optional.of(new IRRecordReceiverNav(headLowered.get(),
                                    fc.featureName(), headRec.name(), id, type(fc, ws),
                                    card(fc, ws), Optionality.PRESENT, fc.sourceRange()));
                        }
                    } else if (isMetaAnnotated(headAttr)
                            && metaQualifierNames(headAttr).contains(fc.featureName())) {
                        // #525 — the QUALIFIER leaf over the lowered member head (the #524
                        // exposed frontier CLAIMED: the headUnGate census's noMatch.
                        // headLowers:IRMetaAccess.ctMiss rows — the `filter partyReference
                        // -> reference exists` ingest-fpml chains + the `filter identifier
                        // -> scheme = "…"` drr product-taxonomy reads). NO member resolves
                        // a qualifier name (the member walk above stays FIRST — legacy
                        // resolves type features ahead of metafields), but the head
                        // attribute's OWN annotation carries it: the #512 qualifier-gate
                        // proof (isMetaAnnotated + the name ∈ metaQualifierNames — the
                        // same channel the #499 IRMetaAccess mint reads) at the MEMBER
                        // head. The head lowers by the arm's own gate (the census's
                        // uniform headLowers fact; a blocked head keeps the honest
                        // decline) and the nav mints the DISTINCT receiver-carrying
                        // IRQualifierReceiverNav (the 44th kind — the #499 law; the
                        // IRQualifierItemNav → this-kind relationship is
                        // IRRecordFeatureNav → IRRecordReceiverNav at the qualifier
                        // seat): an ORACLE LEAF — at-root claims serve through the
                        // standing navChain dispatch leg (the literal
                        // super.visitFeatureCall line) and every containing root renders
                        // whole-legacy through containsOracleLeaf, byte-identical BY
                        // IDENTITY (the FieldWithMetaX/ReferenceWithMetaX qualifier deref
                        // is legacy's own, the L-029 split).
                        Optional<IRExpr> headLowered = adapt(bareHead, id.child(0), ws);
                        if (headLowered.isPresent()) {
                            return Optional.of(new IRQualifierReceiverNav(headLowered.get(),
                                    fc.featureName(), id, type(fc, ws), card(fc, ws),
                                    Optionality.PRESENT, fc.sourceRange()));
                        }
                    }
                }
            }
            // #509 arm-B1 — the RECORD-feature nav at the PARSED seat (the headUnGate census
            // read the headUnresolved dispatch class 72/72 `dispatchInput.rec:date.mRecHit`
            // — `startDate -> day` in the YearFraction-family dispatch bodies, the #507 ENR
            // teach's own parsed-seat twin — and the headAttr face carries the resolved-head
            // sibling). The head resolves through the #507 record arm's OWN core (a bound
            // attribute symbol, else legacy's resolveNameInFunction order, else the
            // dispatchBaseInputNamed scope-join — single-source, the two seats cannot
            // drift), the declared type is a RECORD and the leaf is one of the record's OWN
            // features (RRecordType.features() — the #444 membership rule) → the nav lowers
            // to the SAME shallow IRRecordFeatureNav (head-based, childless; the node's
            // inferred type MISSING BY CONSTRUCTION — no typing gate, identity-serve only:
            // the claim root and every containing root render the literal legacy line
            // through the standing oracle-root serve, legacy's whole record-feature ladder
            // inside it). The #505 record-typed residue lock RECUTS to this arm's
            // acceptance witness (the refine-in-place precedent).
            if (feature == null && rawReceiver instanceof RSymbolReference recordHead
                    && recordHead.name() != null && fc.featureName() != null) {
                RAttribute recHeadAttr =
                        recordHead.symbol().orElse(null) instanceof RAttribute bound ? bound
                                : null;
                if (recHeadAttr == null && recordHead.symbol().isEmpty()) {
                    RFunction recEnclosing = enclosingFunction(fc);
                    recHeadAttr = recEnclosing == null ? null
                            : resolveNameInFunction(recEnclosing, recordHead.name());
                    if (recHeadAttr == null) {
                        recHeadAttr = dispatchBaseInputNamed(recordHead, recordHead.name());
                    }
                }
                RRecordType record = declaredRecordTypeOf(recHeadAttr);
                if (record != null) {
                    for (RRecordFeature f : record.features()) {
                        if (fc.featureName().equals(f.name())) {
                            return Optional.of(new IRRecordFeatureNav(recordHead.name(),
                                    fc.featureName(), record.name(), id, type(fc, ws),
                                    card(fc, ws), Optionality.PRESENT, fc.sourceRange()));
                        }
                    }
                }
            }
            // #508 arm-B1/arm-A2 — the ALIAS-head recovery at the PARSED seat (the
            // featureUnresolved.aliasHead.bodyTyped.featureOnBody[Meta] faces CLAIMED — 143
            // plain + 67 meta sole at the #507 SOT): the nav `<shortcut> -> <feature>` the
            // parser kept as an RFeatureCall declines here because a shortcut head carries no
            // declared type for the linker's feature resolution (the same fact that nulls
            // resolveNameInFunction on the disguised seat). The head resolves through legacy
            // isAliasReference's own dual path (linker-BOUND shortcut OR the name-collision
            // fallback — the metaHopGate census read the live rows 100% collision; the bound
            // leg kept for ladder fidelity), and the SHARED #492 core lowers the navigation
            // over the body-retyped IRReference{ALIAS} — the plain leg's FieldAccess (the
            // ring-proven #492 render) or the #508 meta leg's IRMetaAccess (the oracle-routed
            // serve). The un-admitted faces fall through to the standing featureUnresolved
            // decline, whose mirror keeps the probed spellings.
            if (feature == null && rawReceiver instanceof RSymbolReference aliasHead
                    && fc.featureName() != null) {
                RShortcut shortcut =
                        aliasHead.symbol().orElse(null) instanceof RShortcut bound ? bound
                                : aliasHead.symbol().isEmpty()
                                        ? shortcutByName(enclosingFunction(fc), aliasHead.name())
                                        : null;
                if (shortcut != null) {
                    Optional<IRExpr> aliasNav = aliasHeadNavLowering(shortcut,
                            enclosingFunction(fc), aliasHead.name(), fc.featureName(), fc, id,
                            ws);
                    // The PLAIN leg at THIS seat takes the STRONG allowlist proof on the
                    // shortcut BODY (isProvablyNonMetaElementSource, prove-or-decline — the
                    // #506 strong-guard-per-leg law; the #508 probe2 catch: an engine-clean
                    // body whose Java value is a FieldWithMetaX Mapper — the #326 weaker-
                    // oracle class — rendered the native alias-nav WITHOUT legacy
                    // NavigationHandler's null-safe "Type coercion" deref legs. The disguised
                    // REnumValueRef seat keeps its BYTE-UNCHANGED compiler-side protection —
                    // the #491 post-pin guard's ALIAS_NAV_RECEIVER_META arm + the #507
                    // identity serve — which this parsed claim seat has no equivalent of, so
                    // the unproven slice declines to legacy's own render). The META leg needs
                    // no guard: an IRMetaAccess-rooted claim renders WHOLE through the
                    // oracle-root serve, byte-identical BY IDENTITY.
                    if (aliasNav.isPresent() && aliasNav.get() instanceof FieldAccess
                            && !isProvablyNonMetaElementSource(shortcut.expression())) {
                        aliasNav = Optional.empty();
                    }
                    if (aliasNav.isPresent()) {
                        return aliasNav;
                    }
                }
            }
            // #512 arm-B1 — the RECORD-feature read over a LOWERED compound receiver (the
            // #509-banked nsrGate census's record class: recvLowers:FieldAccess/IRSymbolNav +
            // rec:zonedDateTime/dateTime + mRecHit — the `… -> unadjustedDate -> value ->
            // date`-family chains the #507/#509 SYMBOL-head record arms cannot spell, because
            // the head here is a compound navigation, not a name). The proof mirrors the
            // census classifier EXACTLY (twin-exact BY CONSTRUCTION with the compiler's
            // memberOnTypeToken mRecHit arm): the receiver's ENGINE type is a RECORD type and
            // the leaf is one of the record's OWN features (RRecordType.features() — the #444
            // membership rule), and the receiver subtree LOWERS (the census's uniform
            // recvLowers fact — a blocked receiver keeps the honest nonSymbolReceiver
            // residue, the ctMiss/noCt census rows). The nav lowers to the DISTINCT
            // receiver-carrying IRRecordReceiverNav (the 31st kind — the #499 distinct-kind
            // law; the record-feature render is legacy's own, the L-029 split): an ORACLE
            // LEAF, so every containing claim root renders the literal legacy line through
            // the standing oracle-root serve, byte-identical BY IDENTITY. The SYMBOL-head
            // seats keep their #507/#509 childless IRRecordFeatureNav mints (the receiver
            // class guard — the two shapes cannot double-mint).
            // #520 probe2 — the FUNCTION-head slice joins the #512-B1 receiver class (the
            // probe1 headOtherGate census read the featureUnresolved.headOther face 100%
            // RFunction.mRecHit, 100% drr-FUNCTION: the `GetExecutionTimestamp(
            // reportableEvent) -> date` record reads over a record-returning CALL head —
            // 4 atRoot + 16 interior [RComparisonExpr 4 / RExtractExpr 4 / RLogicalExpr
            // 8]). The head is a NAME, but the render class is the RECEIVER-carrying one:
            // legacy renders the call receiver's own evaluate composition + the record
            // read inside it — the childless #507/#509 IRRecordFeatureNav (whose headName
            // renders BY NAME at an attribute/shortcut head) cannot carry it, so the
            // slice takes the #512-B1 receiver-carrying mint (the receiver lowers to the
            // standing IRApply / IRPointFreeApply). The attribute/shortcut symbol heads
            // keep their standing childless arms — disjoint by symbol class (the #512
            // double-mint guard extended, not weakened). The gate is ORIGIN-agnostic
            // (any RFunction symbol — wider than the census's plain-func class; the
            // Seat-1 #520 OBS-3 scope note): byte-safe regardless, because the mint
            // still requires the feature==null seat, the record member hit and a
            // LOWERED receiver, and every route is the whole-legacy serve.
            if (feature == null && fc.featureName() != null && ws != null
                    && (!(rawReceiver instanceof RSymbolReference)
                            || (rawReceiver instanceof RSymbolReference fnHead
                                    && fnHead.symbol().orElse(null) instanceof RFunction))) {
                RMetaAnnotatedType recvT = type(rawReceiver, ws);
                com.regnosys.rosetta.types.RRecordType rec = recvT != null && !recvT.isMissing()
                        && recvT.type() instanceof com.regnosys.rosetta.types.RRecordType engineRec
                                ? engineRec : null;
                Optional<IRExpr> recRecv = Optional.empty();
                if (rec == null
                        && !(rawReceiver instanceof RSymbolReference bareRecHead
                                && bareRecHead.symbol().isEmpty())) {
                    // #530 arm-B1 — the LOWERED-type channel (the #529 choice channel-(b)
                    // pattern at the RECORD seat): the engine-blind ingest receivers (the
                    // nsrCtGate census's break:root:RImplicitVariable.feat:date pool — the
                    // `value -> date` reads over IdentifiedDate items, 9 cdm6-f + 2 drr-f;
                    // goldens render legacy's own record deref, Date.of(zdt.toLocalDate()))
                    // carry the RECORD fact on the LOWERED receiver's attribute-channel
                    // type where the engine cache reads MISSING (the #479 cache-boundary
                    // class). The bare un-bound symbol-head skip is the #529 re-entrancy
                    // fence verbatim (those heads belong to the bare-item ladders above).
                    recRecv = adapt(rawReceiver, id.child(0), ws);
                    if (recRecv.isPresent() && recRecv.get().type() != null
                            && !recRecv.get().type().isMissing()
                            && recRecv.get().type().type()
                                    instanceof com.regnosys.rosetta.types.RRecordType lowRec) {
                        rec = lowRec;
                    }
                }
                if (rec != null) {
                    boolean recMember = false;
                    for (com.regnosys.rosetta.types.RecordFeature f : rec.features()) {
                        if (fc.featureName().equals(f.name())) {
                            recMember = true;
                            break;
                        }
                    }
                    if (recMember) {
                        if (recRecv.isEmpty()) {
                            recRecv = adapt(rawReceiver, id.child(0), ws);
                        }
                        if (recRecv.isPresent()) {
                            return Optional.of(new IRRecordReceiverNav(recRecv.get(),
                                    fc.featureName(), rec.name(), id, type(fc, ws),
                                    card(fc, ws), Optionality.PRESENT, fc.sourceRange()));
                        }
                    }
                }
            }
            // #529 — the CHOICE-OPTION selection at the PARSED seat (the headAttrGate
            // census's whole pool: featureUnresolved.headAttr.declMiss 35 cdm6-f sole at
            // the #528 SOT, 100% ht:choice:<name>.qual:noMeta — Observable 16 / Payout 9 /
            // CollateralCriteria 6 / Instrument 2 / Underlier 2, the `observable -> Asset`-
            // family navigations; witnesses ObservableIsCommodity / InterestCashSettlement-
            // Amount / CloneEligibleCollateralWithChangedTreatment / Qualify_InstrumentType-
            // Equity / UnderlierQualification). The head attribute DECLARES a choice —
            // declaredDataTypeOf can never derive it, so the #492 facet read declMiss — and
            // the feature segment selects one of the choice's own options BY TYPE NAME (the
            // #503 arm-B1 projection at the parsed seat — the #524 leg's channel at the
            // BOUND-head seat). TWO head channels, one mint: (a) a SYMBOL head bound to a
            // choice-declaring attribute (the census's whole live pool); (b) a LOWERED
            // receiver whose carried type is the PROVEN choice (RChoiceTypeRef — the
            // #521/#524 attached-original channel): the chain's deeper hops (`observable ->
            // Asset -> Commodity` — Asset itself a choice), which the symbol-head teach
            // EXPOSES at this same wave (the #494 blast-radius law: teaching the head hop
            // makes the outer hop the next blocker, so the leg lands receiver-general at
            // birth). The mint is the DISTINCT receiver-carrying IRChoiceReceiverNav (the
            // 46th kind — the #499 law; the IRChoiceOptionNav → this-kind relationship is
            // IRRecordFeatureNav → IRRecordReceiverNav at the choice seat, the #525 twin
            // convention): an ORACLE LEAF, so every containing claim root renders the
            // literal legacy line through the standing whole-legacy serves — the
            // option-selection render (the plain member map, the with-meta coercion legs
            // where an option carries them) stays legacy's own inside them (the L-029
            // split). The node types from the ATTACHED option's referenced type (data →
            // RDataTypeRef / choice → RChoiceTypeRef — the #521 elem-proven-typing law); an
            // unresolvable option declines whole (prove-or-decline).
            if (feature == null && fc.featureName() != null && ws != null) {
                RChoice headChoice = null;
                Optional<IRExpr> choiceRecv = Optional.empty();
                if (rawReceiver instanceof RSymbolReference choiceHead
                        && choiceHead.args().isEmpty()
                        && choiceHead.symbol().orElse(null) instanceof RAttribute choiceHeadAttr
                        && choiceHeadAttr.typeCall() != null
                        && choiceHeadAttr.typeCall().referencedType()
                                .orElse(null) instanceof RChoice declaredChoice) {
                    headChoice = declaredChoice;
                    choiceRecv = adapt(rawReceiver, id.child(0), ws);
                } else if (!(rawReceiver instanceof RSymbolReference bareRecv
                        && bareRecv.symbol().isEmpty())) {
                    // The channel-(b) receiver probe skips BARE un-bound symbol heads —
                    // those belong to the #522/#524 bare-item ladders (which already ran
                    // above and declined), and re-adapting one from this seat is the
                    // probe1 re-entrancy cycle's other entry edge (adapt → the requalify
                    // machinery → the enclosing equality's sibling → the original node —
                    // the #524 OBS-1 fence class).
                    Optional<IRExpr> lowered = adapt(rawReceiver, id.child(0), ws);
                    if (lowered.isPresent() && lowered.get().type() != null
                            && !lowered.get().type().isMissing()
                            && lowered.get().type().type() instanceof RChoiceTypeRef ctr
                            && ctr.astNode() != null) {
                        headChoice = ctr.astNode();
                        choiceRecv = lowered;
                    }
                    // #530 arm-B2 — the STRUCTURAL item channel (channel (c)): an
                    // implicit-ITEM receiver whose binder SOURCE's element form IS a
                    // declared DIRECT choice (the #521 declaredDirectChoiceOf channel)
                    // proves the choice from the DECLARATION where the item's
                    // cache-boundary type never can — the nsrCtGate census's
                    // item.term:RSymbolReference bareRef pool (2 cdm6-f: the
                    // `payout extract [ item -> OptionPayout ]` navigate-by-type reads
                    // over an untypeable item, witness Create_CashflowFromPayout).
                    if (headChoice == null && lowered.isPresent()
                            && rawReceiver instanceof RImplicitVariable cIv
                            && isFilterOrExtractBound(cIv)) {
                        RChoice direct =
                                declaredDirectChoiceOf(filterExtractBindingSource(cIv));
                        if (direct != null) {
                            headChoice = direct;
                            choiceRecv = lowered;
                        }
                    }
                }
                if (headChoice != null && choiceRecv.isPresent()) {
                    for (RChoiceOption option : headChoice.options()) {
                        RTypeCall optionCall = option.typeCall();
                        if (optionCall != null
                                && fc.featureName().equals(optionCall.typeName())) {
                            RNode optRef = optionCall.referencedType().orElse(null);
                            RMetaAnnotatedType optType = optRef instanceof RDataType optData
                                    ? RMetaAnnotatedType.withNoMeta(new RDataTypeRef(optData))
                                    : optRef instanceof RChoice optChoice
                                            ? RMetaAnnotatedType.withNoMeta(new RChoiceTypeRef(
                                                    optChoice.name(), List.of(), optChoice))
                                            : null;
                            if (optType == null) {
                                break; // an unresolvable option — prove-or-decline
                            }
                            return Optional.of(new IRChoiceReceiverNav(choiceRecv.get(),
                                    fc.featureName(), headChoice.name(), id, optType,
                                    card(fc, ws), Optionality.PRESENT, fc.sourceRange()));
                        }
                    }
                }
            }
            // #512 arm-B2 — the METADATA-QUALIFIER read off a bound USER item (the
            // #509-banked nsrGate census's qualifier class: RImplicitVariable.recvLowers:
            // IRVariable + mMiss + qMiss — the `X -> entityId then filter (… item -> scheme
            // …)`-family spellings over `string (0..*) [metadata scheme]` elements; the
            // census's own shallow source read bottomed on the elided-pipe implicit,
            // other:RImplicitVariable). The proof is the #512 DEEP PIPE WALK
            // (terminalBindingSourceFeature — the shared #506 walk family's FEATURE-channel
            // sibling, prove-or-decline): the item's filter/extract binder source resolves
            // through the then-chain to its TERMINAL resolved feature, and the nav's feature
            // name must be one of that feature's metadata qualifiers (the same
            // metaQualifierNames channel the #499 IRMetaAccess mint reads — twin-named). The
            // nav lowers to the DISTINCT shallow IRQualifierItemNav (the 32nd kind — the
            // #499 distinct-kind law; the item's Java binding and the FieldWithMetaX
            // qualifier deref are legacy's own, the L-029 split): an ORACLE LEAF, so every
            // containing claim root renders the literal legacy line through the standing
            // oracle-root serve, byte-identical BY IDENTITY. An unprovable source keeps the
            // honest nonSymbolReceiver residue (census-narrow: the filter/extract binder
            // classes only — the census's own rows).
            if (feature == null && fc.featureName() != null
                    && rawReceiver instanceof RImplicitVariable qIv
                    && isFilterOrExtractBound(qIv)
                    // #530 arm-B3: the license verdict subsumes the standing terminal read
                    // (its first step IS terminalBindingSourceFeature — the #512-B2 slice
                    // preserved BY CONSTRUCTION) and adds the divergent-conditional JOIN
                    // license + the rule-seat bare-name continuation (arm-B3b in the walk).
                    && bindingSourceQualifierLicensed(filterExtractBindingSource(qIv),
                            fc.featureName(), 0)) {
                return Optional.of(new IRQualifierItemNav(fc.featureName(), id,
                        type(fc, ws), card(fc, ws), Optionality.PRESENT,
                        fc.sourceRange()));
            }
            if (feature == null) {
                return Optional.empty(); // disguised / record feature — defer
            }
        }
        if (isMetaAnnotated(feature)) {
            // #499 — the metaNav conversion arm (the L-109d relabel belt's convertible slice,
            // sized by the #497/#499 censuses: recvLowers 1,794 + the recursive cascade 111 of
            // the 2,962-event seat, plus the 1,989-node INTERIOR population read 100%
            // recvLowers). The meta hop lowers to the DISTINCT IRMetaAccess kind whenever its
            // receiver subtree lowers (D1 all-or-nothing — ANY lowered receiver is faithful,
            // and a meta-over-meta chain lowers bottom-up through this same arm: the cascade
            // class); the residue (a receiver that does not lower) stays on the belt. The
            // FieldWithMetaX retype + coercion stay Java-emission decisions (the L-029 split):
            // the emitter serves the claim ROOT with the range-correlated MetaNavRenderer —
            // super.visitFeatureCall, the ONE line the relabel belt IS — so the bytes cannot
            // move. Since #500 the nav/operand/arg/existence gates ADMIT the kind (the consumer
            // admissions off the #499-exposed frontier), and the safety RELOCATES to the render
            // routing: a meta-bearing claim ROOT renders WHOLE through the compiler's
            // oracle-root renderer (the literal super.visit<X> lines), while an interior meta
            // hop under a natively-composed root still declines at the emitter's
            // uncorrelated-renderer gate — the distinct kind stays what makes that gate
            // possible. NO type gate — the
            // #497 census read the seat's typing heavily fragmented and the oracle render never
            // consults it (the delegation family's type-blind posture, the #498 rule-apply
            // precedent).
            Optional<IRExpr> metaReceiver = adapt(rawReceiver, id.child(0), ws);
            if (metaReceiver.isEmpty()) {
                return Optional.empty(); // receiver not expressible — the belt keeps the residue
            }
            ExpressionCardinality metaFeatureCard = isMultiCardinality(feature)
                    ? ExpressionCardinality.MULTI : ExpressionCardinality.SINGLE;
            ExpressionCardinality metaAccumulatedCard = (metaFeatureCard == ExpressionCardinality.MULTI
                    || metaReceiver.get().cardinality() == ExpressionCardinality.MULTI)
                    ? ExpressionCardinality.MULTI : ExpressionCardinality.SINGLE;
            return Optional.of(new IRMetaAccess(metaReceiver.get(), fc.featureName(),
                    metaQualifierNames(feature), id, type(fc, ws), metaAccumulatedCard,
                    metaFeatureCard, chainOptionality(metaReceiver.get(), feature), fc.sourceRange()));
        }
        // The witness/output simple-name collision (FQN-inline render) is resolved in the emitter against
        // the enclosing function's output Java type (L-029), not here — see adaptSymbolReference's note.
        Optional<IRExpr> receiver = adapt(rawReceiver, id.child(0), ws);
        if (receiver.isEmpty()) {
            return Optional.empty(); // the receiver subtree is not expressible
        }
        IRExpr receiverNode = receiver.get();
        if (!(isNavigableReceiver(receiverNode)
                || isFilterExtractBoundItemReceiver(receiverNode, rawReceiver))) {
            // #479: the SYNTHETIC filter/extract item, RETYPED at the adapter boundary — the
            // synthetic node always reads MISSING through the expression-node-keyed type cache
            // (the #478 cache-boundary class), so the admissible slice rebuilds the lowered item
            // with the binder SOURCE's element type (the engine's own Cat-8 law). Null off-case:
            // a scalar-param base, a navigation hop, a filter/extract-lambda USER item, or a
            // synthetic shape outside the taught slice — defer.
            receiverNode = retypedSyntheticFilterExtractItem(receiverNode, rawReceiver, ws);
            if (receiverNode == null) {
                // #492: the alias-receiver leg of the same cache-boundary law — a bound-shortcut
                // receiver rebuilt with the BODY's engine type/cardinality when the shared walk
                // admits (see retypedAliasNavReceiver).
                receiverNode = retypedAliasNavReceiver(receiver.get(), rawReceiver, ws);
            }
            if (receiverNode == null) {
                // #504 arm-B: the un-retypeable SYNTHETIC-item nav — the receiverSyntheticItem
                // residue (749 sole at the #503 SOT; the #502 synItemGate census's walked
                // classes: meta/deep-pipe binder-source bottoms dominant · switchCase 71 ·
                // namedExtract ~43 · otherBinder 25) lowers to the DISTINCT shallow
                // IRSynItemNav (the 25th IRExprKind — the #499 distinct-kind law; the resolved
                // feature's name the neutral fact; the item's Java binding and every hop
                // coercion legacy's own, the L-029 split). Unconditional at the shape: the
                // render is whole-legacy regardless of the binder class (every containing
                // claim root renders through the compiler's oracle-root serve — the
                // containsOracleLeaf routing; the kind has no leaf-emitter arm). The type
                // sources from the attribute channel when the node's own reads MISSING (the
                // #479 cache-boundary law); the cardinality carries the hop's own declared
                // step card (routing makes it non-load-bearing — every consumer that admits
                // the kind [the nav-receiver gate + the eq/exist operand gates + the call-arg
                // gate, the post-arm probe's exposed frontier] oracle-serves its root, so no
                // native compose ever reads it). The bare-attr arm's attrOutsideFunction.receiverSyntheticItem
                // equivalents (271 sole) ride this same channel through their equivalent-nav
                // adapt.
                if (receiver.get() instanceof IRVariable synItem
                        && synItem.variableKind() == IRVariable.VariableKind.SYNTHETIC_ITEM
                        && fc.featureName() != null) {
                    RMetaAnnotatedType synNavType = type(fc, ws);
                    if ((synNavType == null || synNavType.isMissing()) && ws != null) {
                        synNavType = ws.getInferredAttributeType(feature);
                    }
                    return Optional.of(new IRSynItemNav(fc.featureName(), id, synNavType,
                            isMultiCardinality(feature) ? ExpressionCardinality.MULTI
                                    : ExpressionCardinality.SINGLE,
                            Optionality.PRESENT, fc.sourceRange()));
                }
                // #508 arm-A5: the META-SOURCED bound USER item — the itemMetaSourced residue
                // (67 sole at the #507 SOT; the metaSrcGate census read the accessed feature
                // 100% PLAIN over metaFc/alias/pipe sources, all typed). The receiver's own
                // admission declined at the L-029 meta split (legacy threads its null-safe
                // FieldWithMetaX "Type coercion" derefs the neutral IR does not model), so the
                // nav lowers to the DISTINCT shallow IRMetaItemNav (the 30th kind — the #504
                // IRSynItemNav pattern EXACTLY: the resolved feature's name the neutral fact;
                // the item's Java binding and every hop coercion legacy's own; no native
                // emitter arm — every containing claim root renders through the compiler's
                // oracle-root serve, byte-identical BY IDENTITY). The type sources from the
                // attribute channel when the node's own reads MISSING (the #479 cache-boundary
                // law); the cardinality carries the hop's own declared step card (routing
                // makes it non-load-bearing — the #504 convention).
                if (receiver.get() instanceof IRVariable metaItem
                        && metaItem.variableKind() == IRVariable.VariableKind.USER_ITEM
                        && rawReceiver instanceof RImplicitVariable metaIv
                        && itemBindingBound(metaIv) && itemBindingMetaSourced(metaIv)
                        && fc.featureName() != null) {
                    RMetaAnnotatedType metaItemNavType = type(fc, ws);
                    if ((metaItemNavType == null || metaItemNavType.isMissing()) && ws != null) {
                        metaItemNavType = ws.getInferredAttributeType(feature);
                    }
                    return Optional.of(new IRMetaItemNav(fc.featureName(), id, metaItemNavType,
                            isMultiCardinality(feature) ? ExpressionCardinality.MULTI
                                    : ExpressionCardinality.SINGLE,
                            Optionality.PRESENT, fc.sourceRange()));
                }
                // v3.3 seat 2 (PR #638): the RULE-INPUT bound USER item - the `noLambda` residue of
                // itemBindingBound's binder gate. The census (the D11's own walkBindGate decode, the #476
                // probe on, at 993a2ee14) read it 152 arrivals per drr 7.x RULE cell, 100%
                // itemBind.noLambda.leafPlain...inRule.typed, and ZERO on any FUNCTION seam; the
                // sole-reason class RFeatureCall:itemNotFilterExtractBound is 129 claims per cell, the
                // largest single mechanism of the vendored adapterGap population. The claim is the #514
                // bare-attr claim's twin one form wider and mints the SAME kind: legacy renders both forms
                // on the same `input` root at this same seat (handle(RImplicitVariable)'s
                // itemNavReceiverInputSlot arm, PR #579, whose own note calls the bare-attr form its
                // two-forms-agree mirror), so the NEUTRAL fact is identical - navigate this feature off the
                // rule input. The shallow IRRuleInputNav carries only the feature's name; the `input`
                // receiver synthesis and every hop coercion are legacy's own (the L-029 split), the kind has
                // NO leaf-emitter arm, and every containing claim root renders through the compiler's
                // oracle-root serve (containsOracleLeaf routing) - byte-identical BY IDENTITY, so what moves
                // is the SHARE and not a byte. Unconditional at the shape (the #504 IRSynItemNav precedent:
                // a whole-legacy render cannot be changed by the binder class); disjoint from the two arms
                // above by USER_ITEM (not SYNTHETIC_ITEM) and !itemBindingBound (the #508 arm's converse),
                // so every standing admission stays byte-frozen. The type sources from the attribute channel
                // when the node's own reads MISSING (the #479 cache-boundary law); the cardinality carries
                // the hop's own declared step card and the optionality the hop feature's declared fact (the
                // kind's own #514 convention), both non-load-bearing under the routing.
                if (receiver.get() instanceof IRVariable ruleItem
                        && ruleItem.variableKind() == IRVariable.VariableKind.USER_ITEM
                        && rawReceiver instanceof RImplicitVariable ruleIv
                        && !itemBindingBound(ruleIv)
                        && ruleInputBoundItem(ruleIv) != null
                        && fc.featureName() != null) {
                    // the type may still be null or missing here, where the #514 arm minting the SAME kind
                    // declines an untypeable node outright ("untypeable - the consumer gates could not read
                    // the node"). The two twins differ on purpose: that arm mints for a consumer gate to
                    // READ, while this claim is always an oracle-served root, so the field is not
                    // load-bearing - the same convention the #504 / #508 arms beside it keep. The census
                    // measured 100% `typed` at the corpus; this clause is why a zero there is not a gate
                    // (PR #638 round 1, cq NIT-3).
                    RMetaAnnotatedType ruleItemNavType = type(fc, ws);
                    if ((ruleItemNavType == null || ruleItemNavType.isMissing()) && ws != null) {
                        ruleItemNavType = ws.getInferredAttributeType(feature);
                    }
                    return Optional.of(new IRRuleInputNav(fc.featureName(), id, ruleItemNavType,
                            isMultiCardinality(feature) ? ExpressionCardinality.MULTI
                                    : ExpressionCardinality.SINGLE,
                            optionalityOf(feature), fc.sourceRange()));
                }
                // #505 arm-C/arm-A2: the shallow ORACLE-LEAF receivers compose (the dispatch
                // -base input head under a nav — the census's interior.RFeatureCall slice —
                // and the chained choice-option hop). The FieldAccess composes over the kind
                // exactly like the #504 IRSynItemNav chains: the base is an oracle leaf, so
                // every containing root renders whole-legacy (containsOracleLeaf routing) —
                // no native compose ever reads the hop.
                if (receiver.get() instanceof IRDispatchInputRef
                        || receiver.get() instanceof IRChoiceOptionNav
                        // #507: the deep-path nav and the record-feature read as chained-hop
                        // receivers (the census's interior.RFeatureCall slice — the
                        // `x ->> a -> b` continuation shape). Both are oracle leaves, so
                        // every containing root renders whole-legacy (containsOracleLeaf
                        // routing) — no native compose ever reads the hop.
                        || receiver.get() instanceof IRDeepFeatureNav
                        || receiver.get() instanceof IRRecordFeatureNav) {
                    receiverNode = receiver.get();
                } else {
                    return Optional.empty();
                }
            }
        }
        // featureCardinality = THIS hop's own declared step card (the emitter keys map/mapC on it,
        // mirroring legacy resolveMapMethod — per-hop, receiver-agnostic). The accumulated cardinality
        // is a MONOTONE multi overlay (design §6 V2-B): MULTI if this hop OR any receiver hop is,
        // computed from the declared signals (reliable) rather than the conservative inferred card —
        // the operand gates read it (a multi chain result must not pass as a scalar operand).
        ExpressionCardinality featureCard = isMultiCardinality(feature)
                ? ExpressionCardinality.MULTI : ExpressionCardinality.SINGLE;
        ExpressionCardinality accumulatedCard = (featureCard == ExpressionCardinality.MULTI
                || receiverNode.cardinality() == ExpressionCardinality.MULTI)
                ? ExpressionCardinality.MULTI : ExpressionCardinality.SINGLE;
        RMetaAnnotatedType hopType = type(fc, ws);
        if ((hopType == null || hopType.isMissing())
                && (isSyntheticItemBasedChain(receiverNode) || isAliasBasedChain(receiverNode)
                        // #503: the closure-param-rooted chains join the same attribute-channel
                        // type sourcing (the parsed nav over an unlinked param head reads
                        // MISSING on its own node — the #478 cache-boundary class).
                        || isClosureParamBasedChain(receiverNode)
                        // #505: the dispatch-input and choice-option-rooted chains join the
                        // same sourcing (an unresolved head's nav node reads MISSING on its
                        // own node — the same cache-boundary class).
                        || isShallowLeafBasedChain(receiverNode))) {
            // #479: a legacy-synthesized re-entrant nav node (the in-lambda implicit-item
            // equivalents legacy builds and routes back through visitFeatureCall) reads MISSING
            // through the expression-node-keyed cache — the #478 cache-boundary class. Source the
            // hop's type from the resolved feature's ATTRIBUTE channel (the #478 retype law via
            // the same RWorkspace delegate), SCOPED to synthetic-item-based chains (a
            // SYNTHETIC_ITEM base only ever lowers through the #479 retype arm) and — since #492 —
            // alias-based chains (an ALIAS base only ever lowers through the #492 retype arms; a
            // parsed nav over an alias head reads MISSING through the L-032 gap on its own node),
            // so no previously-adapted claim's type sourcing moves. A raw (cache-resident) node
            // keeps its own richer node-channel type when present.
            hopType = ws.getInferredAttributeType(feature);
        }
        return Optional.of(new FieldAccess(receiverNode, fc.featureName(), id, hopType,
                accumulatedCard, featureCard, chainOptionality(receiverNode, feature), fc.sourceRange()));
    }

    /**
     * Whether a lowered receiver's spine bottoms out at a {@link IRVariable.VariableKind#SYNTHETIC_ITEM}
     * base — the #479 taught chains (a synthetic item only ever lowers through
     * {@link #retypedSyntheticFilterExtractItem}, so this identifies exactly the re-entrant
     * synthesized-equivalent population whose hop nodes the type cache never saw) — or, since
     * #504, at an {@link IRSynItemNav} base (the un-retypeable residue's own mint: an outer hop
     * over the minted kind reads MISSING through the same cache-boundary class, so its hop type
     * sources from the attribute channel identically).
     */
    private static boolean isSyntheticItemBasedChain(IRExpr receiver) {
        IRExpr cur = receiver;
        while (cur instanceof FieldAccess fa) {
            cur = fa.receiver();
        }
        return (cur instanceof IRVariable var
                && var.variableKind() == IRVariable.VariableKind.SYNTHETIC_ITEM)
                || cur instanceof IRSynItemNav;
    }

    /**
     * #505: whether a lowered receiver's spine bottoms out at a dispatch-input or
     * choice-option base (the two #505 shallow oracle-leaf kinds) — the hop-type sourcing's
     * cache-boundary identification, exactly the {@link #isSyntheticItemBasedChain} pattern:
     * these bases only ever lower through their own #505 arms, so an outer hop over one
     * reads MISSING through the expression-node-keyed cache and sources from the attribute
     * channel.
     */
    private static boolean isShallowLeafBasedChain(IRExpr receiver) {
        IRExpr cur = receiver;
        while (cur instanceof FieldAccess fa) {
            cur = fa.receiver();
        }
        // #507: the deep-path nav and the record-feature read join the spine-bottom set —
        // both only ever lower through their own #507 arms, so an outer hop over either
        // reads MISSING through the expression-node-keyed cache and sources from the
        // attribute channel (the same cache-boundary identification).
        return cur instanceof IRDispatchInputRef || cur instanceof IRChoiceOptionNav
                || cur instanceof IRDeepFeatureNav || cur instanceof IRRecordFeatureNav;
    }

    /**
     * #503 — the closure-param sibling of {@link #isSyntheticItemBasedChain}: the lowered
     * receiver's spine bottoms out at an {@link IRClosureParam} base. A closure-param-rooted
     * {@link FieldAccess} only ever exists through the #502 head arm + the #503 by-name feature
     * resolution (the navigability gate blocked the shape before #502), so this identifies
     * exactly the population whose hop nodes the type cache never typed (the parsed nav over an
     * unlinked param head reads MISSING on its own node — the #478 cache-boundary class), and
     * the hop type sources from the resolved feature's ATTRIBUTE channel like the other two
     * retype families.
     */
    private static boolean isClosureParamBasedChain(IRExpr receiver) {
        IRExpr cur = receiver;
        while (cur instanceof FieldAccess fa) {
            cur = fa.receiver();
        }
        return cur instanceof IRClosureParam;
    }

    /**
     * #503 arm-B2 — the closure-param HEAD identity (the #502 arm's two bind flavors, behind
     * the alias-match precedence): the head names an enclosing inline function's declared param
     * (bound: the parameter's own {@code RClosureParameter} node, or the declaring
     * {@link RInlineFunction} for a name-only parameter — the two shapes
     * {@link RInlineFunction#declaringLambdaOf} reads since v3.2 seat 8; synthetic: the
     * symbol-null name-resolve), and the name does NOT match an enclosing shortcut (legacy isAliasReference
     * wins — the #453-family exclusion, read through the SAME 4096-bounded pair the #502 claim
     * gates use). The only-exists subtree stays excluded (the #486 ladder's precedence).
     */
    private static boolean isClosureParamHead(RSymbolReference head) {
        if (head.name() == null || hasOnlyExistsElementAncestor(head)) {
            return false;
        }
        RFunction enclosing = aliasMatchEnclosingFunction(head);
        if (enclosing != null) {
            RShortcut matched = shortcutByName(enclosing, head.name());
            if (matched != null && matched != enclosingShortcut(head)) {
                return false; // the alias-match precedence — legacy renders the alias invocation
            }
        }
        RNode symbol = head.symbol().orElse(null);
        return RInlineFunction.declaringLambdaOf(symbol, head.name()).isPresent()
                || (symbol == null && isEnclosingClosureParamName(head, head.name()));
    }

    /**
     * #503 arm-B2 — the closure param's ELEMENT data type: the registering binder's SOURCE
     * derived through the shared structural walk (the Cat-8 law — a filter/extract param's
     * value is the source's element; a then-binder param takes the whole argument value,
     * element ≡ value on the walked derivation), gated on the #479 allowlist (the L-029
     * FieldWithMeta-value protection — prove-or-decline, exactly the #502 retype's two proofs).
     * {@code null} outside the provable slice.
     */
    private static RDataType closureParamElementType(RNode start, String name) {
        RExpression source = closureParamBinderSource(start, name);
        if (source == null || !isProvablyNonMetaElementSource(source)) {
            return null;
        }
        return sourceElementDataType(source, 0);
    }

    /**
     * #506: {@link #closureParamElementType}'s registration-and-source half, extracted for the
     * cpSym legs of the two source walks (a named param in element-SOURCE position derives on
     * this same binder source — the walks apply their own gates over it): the nearest enclosing
     * inline function whose declared param names carry {@code name}, its parent
     * filter/extract/then's source expression. {@code null} when the registration walk misses
     * or the parent shape is outside the derivable set.
     */
    private static RExpression closureParamBinderSource(RNode start, String name) {
        RNode cur = start == null ? null : start.parent();
        int depth = 0;
        RInlineFunction binder = null;
        while (cur != null && depth++ < PARENT_WALK_LIMIT) {
            if (cur instanceof RInlineFunction inline && inline.paramNames().contains(name)) {
                binder = inline;
                break;
            }
            if (cur instanceof RRule || cur instanceof RFunction) {
                return null;
            }
            cur = cur.parent();
        }
        if (binder == null) {
            return null;
        }
        RNode parent = binder.parent();
        return parent instanceof RFilterExpr filter && filter.body() == binder
                ? filter.argument()
                : parent instanceof RExtractExpr extract && extract.body() == binder
                        ? extract.argument()
                        : parent instanceof RThenExpr then && then.body().orElse(null) == binder
                                ? then.argument() : null;
    }

    /**
     * #506: the structural-emptiness discriminator of {@link #hasGenuineElse}, shared with the
     * empty-THEN mirror rule in the two source walks ({@code empty} / an empty list literal —
     * the branch contributes NO elements, so the conditional's element form is the OTHER
     * branch's alone, the checker's join-with-NOTHING bottom rule in either direction).
     */
    private static boolean isStructurallyEmptyBranch(RExpression branch) {
        return branch instanceof REmptyLiteral
                || (branch instanceof RListLiteral list && list.elements().isEmpty());
    }

    /**
     * #492 — the alias sibling of {@link #isSyntheticItemBasedChain}: the lowered receiver's spine
     * bottoms out at an {@code IRReference{ALIAS}} base. An ALIAS-rooted {@link FieldAccess} only
     * ever exists through the #492 retype arms ({@link #adaptAliasHeadNav} /
     * {@link #retypedAliasNavReceiver} — the navigability gate blocked the shape before), so this
     * identifies exactly the population whose hop nodes the type cache never typed (the parsed
     * nav's own node reads MISSING through the L-032 alias-head gap).
     */
    private static boolean isAliasBasedChain(IRExpr receiver) {
        IRExpr cur = receiver;
        while (cur instanceof FieldAccess fa) {
            cur = fa.receiver();
        }
        return cur instanceof IRReference ref
                && ref.referenceKind() == IRReference.ReferenceKind.ALIAS;
    }

    /**
     * Whether a lowered operand renders as a {@code ComparisonResult} — the only byte-safe operand
     * of a logical {@code and}/{@code or}. True for a boolean {@link BinaryOp} (a comparison,
     * equality or nested logical) and for an {@link Existence} check — both emitter idioms yield a
     * {@code ComparisonResult}, so an {@code exists}/{@code is absent} check composes through logical
     * ({@code foo exists and bar exists}). The exhaustive {@code switch} is deliberate: when an
     * arithmetic operator joins {@link BinaryOp.BinOp} it will force a compile error here, so it cannot
     * be silently admitted as a logical operand (Rune's type system forbids it anyway — a logical
     * operand is always boolean).
     *
     * <p>Public since #528 as the opResGate census's own BY-CALL vocabulary (the #524 census
     * BY-CALL law — the census reads the LOGICAL seat's failing side off this exact predicate,
     * in the mirror's own left-then-right order, rather than re-deriving the gate); read-only.
     */
    public static boolean producesComparisonResult(IRExpr expr) {
        if (expr instanceof Existence) {
            return true;
        }
        // #501: the ORACLE-LEAF operand admissions — a lowered only-exists check (the
        // #500-exposed operand:IROnlyExists face, 246 sole / 100% RLogicalExpr at the census), a
        // lowered then-pipe (operand:IRPipe 12 — legacy coerces the Mapper chain via
        // ComparisonResult.ofNullSafe inside its own render) and a lowered disguised
        // symbol-receiver navigation (the #501 arm-C IRSymbolNav kind). None of the three
        // renders a ComparisonResult through THIS emitter's composed pipeline — the safety is
        // render ROUTING (the #500 arm-A relocation): every claim root containing an
        // oracle-leaf kind renders WHOLE through the compiler's oracle-root serve (the literal
        // super.visit<Binary> line — legacy's own operand composition, coercions included), and
        // an interior one under a natively-composed root declines at the leaf emitter's
        // kind-dispatch end (no emitter arm exists for the shallow kinds — never a native
        // compose). Admitted at the gate, served only at oracle roots.
        if (expr instanceof IROnlyExists || expr instanceof IRPipe
                || expr instanceof IRSymbolNav
                // #502: a lowered closure-param reference (the arm-2 IRClosureParam kind — the
                // interior.RLogicalExpr census class: a boolean-valued lambda param composing
                // in a logical). Same routing safety: the claim root oracle-serves whole (the
                // literal super.visit<Binary> line — legacy's own operand composition), and no
                // emitter arm exists for the kind.
                || expr instanceof IRClosureParam
                // #503: a lowered ALL/ANY-modified comparison (the arm-A1 IRAllAnyCompare
                // kind — the census's dominant interior.RLogicalExpr consumers: a modified
                // equality composing in a logical, which legacy composes as the SAME
                // ComparisonResult family). Same routing safety.
                || expr instanceof IRAllAnyCompare
                // #504 arm-A: the ComparisonResult.ofNullSafe COERCION family — a lowered
                // boolean CALL (operandBareBooleanCall, 829 sole at the #503 SOT; the census
                // read 100% FUNCTION callees, 100% boolean SINGLE outputs) and a lowered
                // point-free reference (operand:IRPointFreeApply, 332 logical sole; boolean
                // outputs, both-pf pairs live). Legacy LogicalHandler coerces every
                // non-ComparisonResult operand ComparisonResult.ofNullSafe(<render>) inside
                // its own composition — NOT this emitter's andNullSafe/orNullSafe chain — so
                // the routing safety is the SHAPE leg (the compiler's containsOracleLeaf
                // carries the coerced-logical shape, not the bare kinds: point-free args and
                // call receivers keep their standing NATIVE composes): every claim root
                // containing a logical with a coerced operand renders WHOLE through the
                // oracle-root logical serve (the literal super.visitLogical line).
                || expr instanceof IRApply || expr instanceof IRPointFreeApply
                // #510 arms A3/A4: a lowered conditional (operand:IRConditional — 86 logical
                // sole at the #509 SOT: `(if … then … else …) and …` boolean branches) and a
                // lowered alias/shortcut reference (operandAlias FLAT — 73 logical sole,
                // drr-dominant: the ComparisonResult.ofNullSafe coercion family the #491
                // decode named) join the coerced-operand family. Legacy LogicalHandler
                // coerces every non-ComparisonResult operand ComparisonResult.ofNullSafe(
                // <render>) inside its own composition — for ANY alias body (the #491 body
                // facets are irrelevant at a whole-legacy render) and any conditional branch
                // form — so the routing safety is the SHAPE leg (the compiler's
                // isCoercedBooleanOperand grows both kinds: the containing root renders
                // WHOLE through the oracle-root logical serve, the literal super.visitLogical
                // line; bare conditionals and alias refs keep their standing native composes
                // elsewhere).
                || expr instanceof IRConditional || isAliasReference(expr)
                // #511: a lowered list op joins the coerced-operand family (operand:IRListOp
                // — 6 logical sole at the #510 SOT). Legacy LogicalHandler coerces every
                // non-ComparisonResult operand ComparisonResult.ofNullSafe(<render>) inside
                // its own composition — for ANY list-op render — so the routing safety is
                // the SHAPE leg (the compiler's isCoercedBooleanOperand grows the kind: the
                // containing root renders WHOLE through the oracle-root logical serve, the
                // literal super.visitLogical line; no logical had a list-op operand's native
                // compose pre-teach).
                || expr instanceof IRListOp
                // #528 arm-1: a lowered NAVIGATION joins the coerced-operand family (the
                // operandBareBooleanNav face — 4 sole at the #527 SOT, ONE census spelling in
                // both cdm cells: a boolean-typed SINGLE param-rooted chain as the LEFT
                // operand against an Existence sibling, `func:CheckMaturity`). Legacy
                // LogicalHandler coerces EVERY non-ComparisonResult operand
                // ComparisonResult.ofNullSafe(<render>) inside its own composition — for a
                // bare boolean nav exactly as for the #504 call / #510 conditional+alias /
                // #511 list-op / #520 lambda-op members — so the routing safety is the SHAPE
                // leg (the compiler's isCoercedBooleanOperand grows the kind: the containing
                // root renders WHOLE through the oracle-root logical serve, the literal
                // super.visitLogical line). Kind-wide is the #504 conservation argument
                // EXACTLY: no logical with a nav operand had a native compose pre-teach (the
                // gate rejected every one — the face IS the whole population), and navs keep
                // their standing native composes at every OTHER seat.
                || expr instanceof FieldAccess
                // #513: a lowered membership test joins (a contains/disjoint IS a boolean
                // producer — legacy composes it inside its own logical render; the sweep
                // families' pre-arm faces hid the logical seats behind the minimal-blocker
                // attribution). Same routing safety: the kind is an oracle leaf with no
                // emitter arm, so the containing root renders WHOLE through the oracle-root
                // logical serve (the compiler's isCoercedBooleanOperand grows the kind,
                // twin-exact).
                || expr instanceof IRMembershipOp
                // #513 probe2: a lowered default fallback joins the coerced family (the
                // sweep's own exposed frontier — operand:IRDefaultOp 6 logical sole at
                // probe1, drr FUNCTION: the boolean `(a default b) and …` class; legacy
                // LogicalHandler coerces the render ComparisonResult.ofNullSafe(...)
                // inside its own composition). Census-narrow: switch/collect read ZERO
                // logical faces at probe1 and stay out (the strong-guard-per-leg law).
                || expr instanceof IRDefaultOp
                // #520: a lowered lambda-bodied collection op joins the coerced family
                // (operand:IRLambdaOp — 10 logical sole at the #519 SOT: 4+4+2 per cell,
                // the #518 equality lambda-op admission's LOGICAL-seat sibling). Legacy
                // LogicalHandler coerces every non-ComparisonResult operand
                // ComparisonResult.ofNullSafe(<render>) inside its own composition — for
                // ANY lambda-op render — so the routing safety is the SHAPE leg (the
                // compiler's isCoercedBooleanOperand grows the kind, twin-exact: the
                // containing root renders WHOLE through the oracle-root logical serve, the
                // literal super.visitLogical line; no logical had a lambda-op operand's
                // native compose pre-teach — the face was 100%-decline, the #504
                // conservation argument — and a drift arrival at the leaf emitter's
                // frame-slot-gated lambda arm declines, never a native compose).
                || expr instanceof IRLambdaOp) {
            return true;
        }
        return expr instanceof BinaryOp bin && switch (bin.op()) {
            case LT, GT, LTE, GTE, EQ, NEQ, AND, OR -> true;
            // Arithmetic yields a NUMBER, not a ComparisonResult — it cannot be a logical operand
            // (Rune's type system forbids it anyway). The exhaustive switch made this an explicit decision.
            case ADD, SUB, MUL, DIV -> false;
        };
    }

    /**
     * Whether a lowered operand is a <em>scalar</em> ({@code SINGLE}) function-parameter variable — the
     * only byte-safe operand of the comparison/equality/existence idioms. The {@code SINGLE} guard is
     * load-bearing now that a MULTI param also lowers to an {@link IRVariable.VariableKind#PARAM} (it
     * renders {@code MapperC.<Item>of}, not the {@code MapperS.of} these idioms compose).
     */
    private static boolean isScalarParam(IRExpr expr) {
        return expr instanceof IRVariable var
                && var.variableKind() == IRVariable.VariableKind.PARAM
                && var.cardinality() == ExpressionCardinality.SINGLE;
    }

    /**
     * Whether a lowered operand is a byte-safe SINGLE scalar-valued operand of the comparison /
     * equality / existence idioms — a scalar {@code SINGLE} parameter ({@link #isScalarParam}), a
     * single {@link FieldAccess} navigation result, a SINGLE-output function CALL ({@link IRApply},
     * the L-106 call-as-cmp/eq-operand slice), OR (since #500) a SINGLE meta hop
     * ({@link IRMetaAccess} — served only at oracle-rendered roots, the arm comment below). The
     * native composes render as a plain {@code MapperS}-style value
     * (no cardinality witness, no enum/count/bare-function wrap): a {@code FieldAccess} is always
     * single + non-meta (a MULTI navigation fails the SINGLE guard, and a meta feature never builds a
     * {@code FieldAccess} — since #499 it lowers to the distinct {@code IRMetaAccess} kind, whose
     * meta-bearing claims render whole-oracle, never through this native pipeline) and a SINGLE {@code IRApply} renders the
     * chainable {@code MapperS.of(recv.evaluate(args))} ({@code emitApply}, non-meta output by adaptApply's
     * gate), so the comparison/existence meta-strip ({@code coerceNavigationReceiver}) and numeric-literal
     * typing are no-ops on them — the fixed idioms compose a navigation OR call operand byte-identically
     * (e.g. {@code foo -> bar = baz}, {@code someFunc(x) = baz}, {@code someFunc(x) >= 1}; a navigation
     * {@code a -> b -> c} chain also lowers to a single {@link FieldAccess}). The {@code SINGLE} guard is
     * load-bearing: a MULTI-output call (rendering {@code MapperC}) is not a scalar operand, so it stays on
     * legacy. The numeric path classifies the call's resolved output type via {@code numericWitness} and
     * declines a kind that mismatches the int/number join (the emitter's mixed-kind guard) — byte-safe.
     *
     * <p><strong>§4.2 note (L-106, the binary-absorption trap):</strong> this slice drives MORE comparison/
     * equality nodes (real coverage UP, byte-GREEN) but the §4.2 <em>proxy</em> dips, because a driven binary
     * node absorbs its two previously-separately-driven operand sub-counts (net −1 driven/node). Shipped on
     * Nick's coverage-over-proxy steer (the roadmap goal is 100% IR-DRIVE, i.e. coverage). The navigation
     * <em>receiver</em> gate is the separate {@link #isNavigableReceiver} (which also admits {@code IRApply},
     * but of any cardinality, since a receiver is cardinality-agnostic).
     */
    private static boolean isScalarOperand(IRExpr expr) {
        return isScalarParam(expr)
                || (expr instanceof FieldAccess fa && fa.cardinality() == ExpressionCardinality.SINGLE)
                || (expr instanceof IRApply a && a.cardinality() == ExpressionCardinality.SINGLE)
                // #500: a SINGLE meta-hop operand (the #499-exposed operand:IRMetaAccess face —
                // REquality 202 + RExistence 233 node-unit at the census) — the claim ROOT
                // containing any meta hop renders WHOLE through the compiler's oracle-root
                // renderer (the literal super.visit<Binary> line), never this emitter's composed
                // operand pipeline, so legacy's comparison meta-strip (coerceNavigationReceiver)
                // runs inside legacy's own render (the distinct-kind law's consumer admission:
                // admitted at the gate, served only at oracle roots). The SINGLE guard mirrors
                // the FieldAccess leg — a MULTI hop is not a scalar operand on either route.
                || (expr instanceof IRMetaAccess ma
                        && ma.cardinality() == ExpressionCardinality.SINGLE)
                // #501: a SINGLE disguised symbol-receiver navigation (the arm-C IRSymbolNav
                // kind) — the same routing safety and the same SINGLE guard as the meta leg.
                || (expr instanceof IRSymbolNav sn
                        && sn.cardinality() == ExpressionCardinality.SINGLE)
                // #502: a SINGLE closure-param reference (the arm-2 IRClosureParam kind — the
                // interior.REqualityExpr census class) — the same routing safety and the same
                // SINGLE guard.
                || (expr instanceof IRClosureParam cp
                        && cp.cardinality() == ExpressionCardinality.SINGLE);
    }

    /**
     * Whether a lowered operand is a byte-safe operand of the <em>existence</em> idioms
     * ({@code exists}/{@code is absent}/{@code single}/{@code multiple} exists) — a function-parameter
     * ({@link #isParameterReceiver}) or a {@link FieldAccess} navigation result, of <em>either</em>
     * cardinality. Unlike comparison/equality (which require {@link #isScalarOperand}'s SINGLE result),
     * existence is <strong>operand-cardinality-agnostic</strong>: legacy {@code ExistenceHandler} renders
     * {@code {method}(arg)} where {@code method} folds the operator + optional modifier ONLY (never the
     * operand's cardinality), wrapping whatever the operand compiles to — {@code MapperS.of(name)} for a
     * scalar or {@code MapperC.<Item>of(name)} / a {@code mapC} chain for a multi — identically. Both render
     * as a plain {@code Mapper} value (the multi cases are the L-021/L-025/L-026 byte-proven leaves), and the
     * {@code exists(…)} call returns a {@code ComparisonResult} regardless, so a multi-operand check still
     * composes through logical ({@code xs exists and ys exists}). The witness/output collision and meta cases
     * are already deferred when the operand itself is lowered (so they never reach here).
     *
     * <p>An {@link IRApply} (a function CALL result) is ALSO admitted — the call-as-existence-operand slice (L-105),
     * the direct generalization of {@link #isNavigableReceiver}'s call-as-nav-base. {@code someFunc(args) exists}
     * composes byte-identically: the emitter renders the call as {@code MapperS.of(recv.evaluate(args))} /
     * {@code MapperC.<Item>of(...)} ({@code emitApply}) and {@code emitExistence} wraps it in the same
     * cardinality-agnostic {@code {method}(arg)} call legacy {@code ExistenceHandler} produces (the operator +
     * optional modifier fold into {@code method}, never the operand's cardinality, so a multi-output call is
     * byte-safe). Only the call shapes {@code adaptApply} already drives reach here (plain {@code FUNCTION} callee,
     * non-meta output, simple args); every other call shape declines at {@code adaptApply}, so the operand is never
     * built — keeping the byte surface exactly the established CALL + existence tiers, composed.
     */
    private static boolean isExistenceOperand(IRExpr expr) {
        // #500: a meta-hop operand of EITHER cardinality (existence is operand-cardinality-
        // agnostic — the class javadoc) joins: the claim ROOT containing any meta hop renders
        // WHOLE through the compiler's oracle-root renderer (the literal super.visitExistence
        // line), so the wrapper-typed operand render is legacy's own (the distinct-kind law's
        // consumer admission: admitted at the gate, served only at oracle roots).
        // #501: a disguised symbol-receiver navigation (the arm-C IRSymbolNav kind) joins by
        // the same routing argument, cardinality-agnostic like every other existence operand.
        // #502: a closure-param reference (the arm-2 IRClosureParam kind) joins by the same
        // routing argument, cardinality-agnostic like every other existence operand.
        return isParameterReceiver(expr) || expr instanceof FieldAccess || expr instanceof IRApply
                // #640 arm D (D54 item 3): the bare condition attribute as an existence operand (`attr exists`) - an
                // oracle leaf, cardinality-agnostic like every operand here; the mirror reads this set
                || expr instanceof IRImplicitAttrNav
                || expr instanceof IRMetaAccess || expr instanceof IRSymbolNav
                || expr instanceof IRClosureParam
                // #511: a lowered list op of ANY kind joins (operand:IRListOp — 18 existence
                // sole at the #510 SOT: the `(xs distinct) exists`-family checks; existence
                // is operand-cardinality-agnostic like every other operand here, and no
                // list-op existence operand had a native compose pre-teach). The routing
                // safety is the SHAPE leg (the compiler's containsOracleLeaf carries the
                // existence-with-a-list-op-operand shape, twin-exact): the containing root
                // renders WHOLE through the oracle-root existence serve (the literal
                // super.visitExistence line — legacy's own operand wrap inside it).
                || expr instanceof IRListOp
                // #513: the noAdaptArm-sweep kinds join (existence is operand-cardinality-
                // agnostic like every other operand here; the sweep families' pre-arm faces
                // hid the existence seats behind the minimal-blocker attribution). Same
                // oracle-leaf routing law: the containing root renders WHOLE through the
                // oracle-root existence serve.
                || isNoAdaptArmSweepKind(expr)
                // #514 ARM-2 consumer admission: the top-level rule-input nav as an
                // existence operand (the ruleInputNav.p:RExistenceExpr decode rows — the
                // `leg exists` rule-top class) — an oracle leaf (the same routing law).
                || expr instanceof IRRuleInputNav
                // #519 — the EXISTENCE operand cluster (the #518 equality admissions'
                // siblings at the existence seat; the #518-SOT faces): the implicit-item
                // variables KIND-WIDE (operandItem 2+2+1+6 — the then/lambda-bound
                // `item exists` shapes; the twin-exact compiler Existence-shape leg is
                // deliberately KIND-WIDE too and REROUTES the standing filter/extract-bound
                // native existence composes to the oracle serve BY IDENTITY — the #518
                // equality-item precedent: the bound/unbound split is a RAW-side fact the
                // lowered tree cannot key, the serve IS legacy's own super.visitExistence
                // line, and the oracleRootLowered existence receipt carries the moved
                // mass; isFilterExtractBoundItemOperand stays composed at the adaptExistence
                // gate for ladder fidelity — isItemOperand subsumes it), the conversion
                // (operand:IRConversion 4+1 — a standing #501 oracle-leaf kind, no compiler
                // leg needed) and the #518-minted output-alias nav (operand:IROutputAliasNav
                // 11 — the mint's own exposed frontier, a standing oracle-leaf kind, no
                // compiler leg needed: the #501 shallow-kind law's consumer admission).
                || isItemOperand(expr)
                || expr instanceof IRConversion
                || expr instanceof IROutputAliasNav
                // #525 consumer admission: the receiver-carrying qualifier read as an
                // existence operand (the `filter partyReference -> reference exists`
                // predicate class — the #512 twin's one-hop-deeper sibling; existence is
                // operand-cardinality-agnostic like every other admission here) — an
                // oracle leaf (the same routing law: the containing root renders WHOLE
                // through the oracle-root existence serve).
                || expr instanceof IRQualifierReceiverNav
                // #529 consumer admission: the receiver-carrying choice-option read as an
                // existence operand (the `observable -> Asset exists` guard class — the
                // declMiss teach's own claim-root shells once the inner hops lower; the
                // #525 insurance pattern at the new kind) — an oracle leaf (the same
                // routing law).
                || expr instanceof IRChoiceReceiverNav
                // #519: a list-literal operand joins (operand:IRListConstruct 1 — the
                // `[a, b] exists`-class check); the routing safety is the twin-exact
                // Existence-shape leg in the compiler's containsOracleLeaf (the kind keeps
                // its standing native root render elsewhere — the #518 equality
                // list-construct leg's sibling). Existence is operand-cardinality-agnostic
                // for every admission here, like every other operand in this set.
                || expr instanceof IRListConstruct;
    }

    /**
     * Whether a lowered receiver is a valid base for a feature navigation — a function-parameter
     * ({@link #isParameterReceiver}, of <em>either</em> cardinality) OR any {@link FieldAccess}
     * navigation result (single or multi, e.g. a chain through a multi hop {@code trade -> legs -> rate}).
     * A {@code FieldAccess} receiver is what lets a navigation <em>chain</em> ({@code a -> b -> c})
     * lower: the emitter walks the receiver subtree and appends one more getter hop, so an arbitrarily
     * long all-single, non-meta chain off a parameter base composes byte-identically — legacy
     * {@code disambiguate} is non-mutating, so every hop's lambda variable is resolved independently
     * against the shared render scope regardless of emission order, and {@code map}/{@code mapC} is
     * chosen per hop from that hop's own feature cardinality (the {@code MapperS}/{@code MapperC}
     * runtime is polymorphic, so a single feature off a MULTI receiver renders {@code map} off the
     * {@code MapperC}, byte-identically). A meta/unresolved intermediate hop never builds a
     * {@link FieldAccess} in the first place, so such chains stay on legacy via the per-hop gates.
     *
     * <p>Distinct from {@link #isScalarOperand}: a navigation receiver admits any cardinality, but a
     * comparison/existence operand requires a SINGLE result (a multi navigation is not a scalar operand).
     *
     * <p>An {@link IRApply} (a function CALL result) is ALSO admitted — the call-as-nav-base slice. Its output type
     * is engine-resolved (the parser already types function-call outputs), so the navigation off it (e.g.
     * {@code someFunc(args) -> feature}) composes byte-identically: the emitter renders the call as the chainable
     * {@code MapperS.of(recv.evaluate(args))} / {@code MapperC.<Item>of(...)} ({@code emitApply}) and appends the
     * {@code .<Witness>map("getF", v -> v.getF())} hop off it, with the lambda var derived from the call's result
     * type via the same generic {@code receiverTypeName} every receiver uses. Only the call shapes {@code adaptApply}
     * already drives reach here (plain {@code FUNCTION} callee, non-meta output, simple args); every other call shape
     * declines at {@code adaptApply}, so the receiver is never built — keeping the byte surface exactly the
     * established CALL + navigation tiers, composed.
     *
     * <p>An {@link IRListOp} collapse of kind {@code ONLY_ELEMENT} or {@code FIRST} over a param/nav child is
     * ALSO admitted (#499 — the #498 ONLY_ELEMENT teach's own exposed frontier, {@code receiver:IRListOp};
     * the census read the population 100% typed with the collapse result type present and those two kinds
     * only; the class boundary lives emitter-side as {@code IRJavaLeafEmitter.isCollapseNavBase} since the
     * #511 kind-wide admission). The emitter reproduces legacy's two receiver forms exactly: the
     * ONLY_ELEMENT collapse renders the bare-item
     * {@code <recv>.get()} and the consuming navigation re-wraps it {@code MapperS.of(<recv>.get())} with the
     * {@code MapperS} ref (legacy {@code NavigationHandler}'s {@code nav_after_get_rewrap} facet verbatim — the
     * {@code collapsedMetaDeref} sibling arm takes the plain re-wrap on BOTH routes by a TWO-LEG argument, the
     * Seat-1 #499 MF-6 recut: for param/nav children this adapter's meta gates prove the chain non-meta at
     * every hop; for the admitted call/alias children legacy's own leaf resolution
     * ({@code onlyElementLeafAttribute}'s {@code RAttribute} filter) declines a non-attribute symbol, so
     * {@code metaWrapperOf(null)} is null and the deref arm never fires — the name-collision
     * {@code implicitItemSymbolLeaf} fallback is the one named drift face, zero live carriers, the ring's
     * watch); the FIRST collapse renders the Mapper-valued
     * {@code <recv>\n\t.first()} member call and the navigation chains {@code .map} directly (no re-wrap —
     * legacy's {@code onlyElementReceiver} gate is ONLY_ELEMENT-shaped). The lambda var derives from the
     * collapse's engine-typed result (the element type — legacy {@code resolveReceiverDataType}'s
     * type-transparent collapse case) via the same generic {@code receiverTypeName}. The #499 NATIVE
     * render stays narrow on both axes by census ({@code LAST} was the byte-identical render sibling
     * with zero live events — an unverifiable native teach), but since #511 the kinds/children OUTSIDE
     * the collapse class no longer decline: the receiver admits KIND-WIDE and the containing claim root
     * renders WHOLE through the compiler's oracle-root serve (the nav-over-list-op SHAPE leg, keyed on
     * the emitter's own {@code isCollapseNavBase} native boundary — byte-identical BY IDENTITY, so no
     * per-kind render proof is owed; the {@code receiver:IRListOp} token is now a drift face at its
     * seat).
     */
    private static boolean isNavigableReceiver(IRExpr expr) {
        // #492: a point-free receiver (`BareF -> field`) — the oracle renders the wrapped
        // MapperS.of(...) receiver itself (its navReceiver detection reads the RAW node's own
        // parent), so the nav composition is byte-identical by construction (the probed
        // navReceiver face); the node carries the callee OUTPUT's type for the lambda naming.
        // #499: an ONLY_ELEMENT / FIRST collapse receiver (`xs only-element -> f` / `xs first
        // -> f`) — the #498 teach's own exposed frontier (receiver:IRListOp, the census read
        // 100% typed with the collapse result type present, kinds onlyElement+first ONLY). The
        // emitter reproduces legacy's two receiver forms exactly: the ONLY_ELEMENT bare-item
        // `.get()` collapse re-wrapped `MapperS.of(<recv>.get())` at the nav (legacy
        // NavigationHandler's nav_after_get_rewrap facet + its MAPPER_S ref) and the
        // Mapper-valued FIRST member call chained directly. The #499 NATIVE render is
        // deliberately NARROW on BOTH axes — the census-live kinds only, AND the collapse's
        // own child restricted to the L-051 param/nav byte-equivalence class (the cut-2
        // census: 989 of 1,228 recv nodes; a call child would reach legacy's #334
        // bareMultiFnReceiverWrap, an item/alias child the item/alias facets — each a
        // facet-conditional render the plain composition must not shadow) — but since #511
        // that narrowness bounds only WHICH ROUTE renders, not whether the receiver admits:
        // see the kind-wide IRListOp leg below.
        return isParameterReceiver(expr) || expr instanceof FieldAccess || expr instanceof IRApply
                // #640 arm B: the bare condition attribute (`attr -> x` in a condition) and the condition instance
                // (legacy's `<instance> -> attr`) as navigation receivers - both oracle leaves, so the containing
                // root renders WHOLE through legacy (the #529 argument; the probe's receiver:IRImplicitAttrNav class)
                || expr instanceof IRImplicitAttrNav || expr instanceof IRConditionInstance
                || expr instanceof IRPointFreeApply
                // #500: a meta-hop receiver (the #499-exposed receiver:IRMetaAccess face — the
                // #1 named lever, 762 node-unit at the census, 100% RFeatureCall consumers: the
                // plain-hop-over-meta-chain class) joins: the claim ROOT containing any meta hop
                // renders WHOLE through the compiler's oracle-root renderer
                // (super.visitFeatureCall — the SAME literal line the MetaNavRenderer is), so
                // legacy's meta-typed receiver semantics (the "Type coercion" deref the L-029
                // split kept legacy-side) run inside legacy's own render (the distinct-kind
                // law's consumer admission: admitted at the gate, served only at oracle roots;
                // an interior meta hop under a natively-composed root declines at the emitter's
                // uncorrelated-renderer gate exactly as pre-teach).
                || expr instanceof IRMetaAccess
                // #501: a disguised symbol-receiver navigation receiver (the arm-C IRSymbolNav
                // kind — `(Rule -> feature) -> more`, the census's dominant interior.RFeatureCall
                // class) joins by the same routing argument: the containing root renders whole
                // through the oracle-root serve, and the kind has no emitter arm.
                || expr instanceof IRSymbolNav
                // #502: a closure-param navigation base (the arm-2 IRClosureParam kind — the
                // census's dominant interior.RFeatureCall class: `param -> feature` off a
                // named/synthetic lambda param) joins by the same routing argument.
                || expr instanceof IRClosureParam
                // #504 arm-B: an un-retypeable synthetic-item nav base (the IRSynItemNav
                // kind — the census's interior.RFeatureCall positions: `item -> a -> b`
                // chains whose innermost hop minted the kind) joins by the same routing
                // argument: the containing root renders whole through the oracle-root serve,
                // and the kind has no emitter arm.
                || expr instanceof IRSynItemNav
                // #508 arm-A5: a meta-sourced USER-item nav base (the IRMetaItemNav kind —
                // the metaSrcGate census's interior.RFeatureCall positions) joins by the
                // same routing argument.
                || expr instanceof IRMetaItemNav
                // #511: EVERY list-op receiver joins (the receiver:IRListOp face, 86 sole at
                // the #510 SOT — the excluded collapse kinds/children the #499 teach left on
                // the drift face). The #499 census-narrow collapse class keeps its NATIVE
                // render at the emitter (the IRJavaLeafEmitter isCollapseNavBase twin — the
                // nav_after_get_rewrap / FIRST member-call composes, byte-live); every shape
                // OUTSIDE that class routes the containing claim root through the compiler's
                // oracle-root serve (the containsOracleLeaf nav-over-list-op SHAPE leg,
                // twin-exact with the emitter's own native boundary BY IDENTITY — the same
                // predicate gates both sides), so legacy's kind- and child-conditional
                // receiver renders (#334 bare-multi-fn wrap, item round-trips, the #361
                // disguised-chain hoist, the #399G FIRST/LAST meta-element stamp) run inside
                // legacy's own line. The shape was a 100%-decline face pre-teach, so no
                // standing native compose reroutes (the #504 conservation argument); the
                // pre-#511 adapter-side census-narrow gate (isCollapseNavBase) dissolves into
                // this kind-wide admission — the class boundary lives on ONLY as the
                // emitter/compiler native-vs-oracle routing twin.
                || expr instanceof IRListOp
                // #514 ARM-1 + ARM-3 consumer admissions: a lowered function-OUTPUT reference
                // base (`<output> -> feature` — the receiverNotExpressible.child.recv:
                // notAnInputParam.out disguised-nav heads AND the parsed p:RFeatureCall/
                // p:RListOpExpr decode rows) and a lowered META-input reference base
                // (`<metaParam> -> feature` — the recv:metaParam.* heads, the `price ->
                // value` chains). Both oracle leaves: the containing claim root renders
                // WHOLE through the oracle-root serve (legacy's own varPath + FieldWithMetaX
                // deref machinery inside it), and neither kind has an emitter arm, so a
                // native compose can never reach them (the routing safety).
                || expr instanceof IROutputRef
                || expr instanceof IRMetaParamRef
                // #514 ARM-2 consumer admission: the top-level rule-input nav base (the
                // ruleInputNav.p:RListOpExpr decode rows — `attr distinct`-family rule
                // pipelines) — an oracle leaf (the same routing law).
                || expr instanceof IRRuleInputNav
                // #520: a lambda-bodied collection-op receiver joins (the
                // receiver:IRLambdaOp face — 9 sole at the #519 SOT: 3 cdm6 + 6 drr-f,
                // the `(xs extract/filter […]) -> feature` chains; the #511 kind-wide
                // IRListOp receiver admission's lambda-bodied sibling). The routing is
                // the #511 argument with NO native carve-out: the shape was a
                // 100%-decline face pre-teach (no nav-over-lambda-op native compose
                // exists — the #504 conservation argument), so the compiler's
                // containsOracleLeaf nav-over-lambda-op SHAPE leg routes EVERY containing
                // claim root whole-legacy (legacy NavigationHandler's own receiver
                // handling runs inside the serve), and a drift arrival at the leaf
                // emitter's frame-slot-gated lambda arm declines, never a native compose.
                || expr instanceof IRLambdaOp;
    }

    /** Defensive bound on the parent walk (cyclic parent pointers should never occur); mirrors legacy's limit. */
    private static final int PARENT_WALK_LIMIT = 64;

    /**
     * #506: the shared recursion bound of the two source-element walks
     * ({@link #sourceElementDataType} / {@link #isProvablyNonMetaElementSource}) — raised from
     * the original 8 (the #506 census read ~170 {@code deepNest} bottoms: real drr pipe chains
     * exhaust 8 structural links before proving; the walks stay strictly bounded, and the
     * non-structural alias/cpSym legs keep this bound as their termination guard exactly as
     * before). ONE constant for both walks — the walk-unification law (a bound drift between
     * them would mint artificial {@code aw×tw} disagreement cells). PUBLIC for the probe-only
     * census walker's cross-read (the #501 probe-visibility precedent; the Copilot #506 R1
     * point — a display walk hard-coding its own bound drifts the moment this one moves).
     */
    public static final int SOURCE_WALK_DEPTH_LIMIT = 16;

    /**
     * Whether a lowered receiver is the implicit {@code item} of a {@code filter}/{@code extract} lambda body —
     * the lambda-bodied-collection extension to {@link #isNavigableReceiver} (the extract/filter percolation
     * slice). The {@code item} reaches the adapter because the legacy compiler recurses lambda bodies into the
     * IR compiler at emission time; today it declines (the receiver gate rejects it). Admitted as a navigation
     * base when, and ONLY when:
     * <ul>
     *   <li>the lowered receiver is a NON-synthetic {@link IRVariable.VariableKind#USER_ITEM} (the literal
     *       {@code item} keyword — a synthetic elided operand renders the enclosing lambda binding, NOT bare
     *       {@code item}, so it is excluded);</li>
     *   <li>it carries a resolved, NON-meta element type. The engine's Category-8 inference
     *       ({@code TypeInferenceEngine.computeImplicitItemType}) types a filter/extract {@code item} to the
     *       source's element type, so {@code item}'s {@code type} is already populated; a MISSING type means the
     *       source itself did not resolve — decline; a meta-annotated element would need the legacy
     *       {@code MapperS<FieldWithMetaX>} receiver retype the neutral nav does not reproduce — decline;</li>
     *   <li>the {@code item}'s binding is one {@link #itemBindingBound} proves: the original
     *       {@link RFilterExpr}/{@link RExtractExpr} pair, the #506 implicit/paramless
     *       {@code then}/{@code min}/{@code max}/{@code sort} legs, or — since #523 arm-B — a
     *       lambda-less TYPE-guarded SWITCH-CASE subject ({@link #switchCaseNarrowedBinding}).
     *       The re-rooting binders legacy resolves INSIDE {@code handle(RImplicitVariable)}
     *       ({@code thenArg} locals, comparator params, the #221 cast case-subject) are safe
     *       BECAUSE the emitter reuses that handler verbatim with the live scope — the #491
     *       binding-general law; the residue (named binders, reduce, guard-less cases) keeps
     *       the decline.</li>
     * </ul>
     * The emitter renders the admitted {@code item} via the {@code ImplicitItemRenderer}'s verbatim
     * {@code ReferenceHandler.handle(RImplicitVariable)} reuse (the binding decision legacy's own at
     * emit time) and composes {@code item.<Witness>map("getF", v -> v.getF())} — byte-identical to
     * legacy {@code NavigationHandler} off the same receiver.
     */
    private static boolean isFilterExtractBoundItemReceiver(IRExpr recv, RExpression rawReceiver) {
        return recv instanceof IRVariable var
                && var.variableKind() == IRVariable.VariableKind.USER_ITEM
                && recv.type() != null && !recv.type().isMissing() && !recv.type().hasMeta()
                && rawReceiver instanceof RImplicitVariable
                // #506: the binding gate widened from the filter/extract pair to the implicit
                // then/min/max/sort bodies ({@link #itemBindingBound} — the #491 emitter
                // renders the USER item via legacy handle(RImplicitVariable) VERBATIM, so the
                // binding decision — lambda param, thenArg local, comparator param — is
                // legacy's own at emit time, and the historical filter/extract bound was
                // admission lag exactly like the #502 ARG-seat unbounding; the #506 itemBind
                // census read the residue 192-of-194 then/min/max/sort-bound, typed,
                // plain-leafed).
                && itemBindingBound((RImplicitVariable) rawReceiver)
                // The item's NEUTRAL type (above) can be a non-meta unwrapped value even when the item's Java VALUE
                // is a FieldWithMeta wrapper — when the binding op's SOURCE is a meta-annotated feature.
                // Navigating off such an item needs legacy NavigationHandler's gm-aware "Type coercion"
                // (FieldWithMeta -> value) that the neutral IR cannot reproduce, so decline (the L-029 split). This
                // AST-level meta signal catches what the type-level hasMeta() cannot once the engine types the item
                // to its unwrapped element type (the 3c60acec head->feature cascade fix exposed this in CompareTradeLot).
                && !itemBindingMetaSourced((RImplicitVariable) rawReceiver);
    }

    /**
     * #506: the RECEIVER-seat binding gate — the original filter/extract pair
     * ({@link #isFilterOrExtractBound}, byte-unchanged) OR an implicit/paramless
     * {@code then}/{@code min}/{@code max}/{@code sort} body ({@link #newLegItemBindingSource}
     * — the widened legs; a NAMED comparator/pipe body carries the scope-live walk-out class,
     * decline, zero census rows) OR — since #523 arm-B — a lambda-less TYPE-guarded SWITCH-CASE
     * subject ({@link #switchCaseNarrowedBinding}: legacy's #221 switchChoiceHoist re-root
     * lives INSIDE {@code handle(RImplicitVariable)}, which the emitter reuses verbatim with
     * the live scope — the same admission-lag class as the #506 legs; the walkBindGate census
     * read the residue {@code itemBind.switchCrossed.leafPlain} typed at both seats).
     */
    private static boolean itemBindingBound(RImplicitVariable iv) {
        return isFilterOrExtractBound(iv) || newLegItemBindingSource(iv) != null
                || switchCaseNarrowedBinding(iv) != null;
    }

    /**
     * v3.3 seat 2 (PR #638): the enclosing {@link RRule} whose own INPUT binds {@code iv} - the
     * {@code noLambda} residue of {@link #itemBindingBound}'s binder gate - or {@code null} when a closer
     * binder owns {@code item}. At rule-body top level there is no lambda to bind {@code item}: the implicit
     * value IS the rule input, and legacy says so at the seat that RENDERS it -
     * {@code ReferenceHandler.handle(RImplicitVariable)}'s {@code itemNavReceiverInputSlot} arm (PR #579)
     * renders a literal {@code item} rooting a navigation at rule-body top level as
     * {@code MapperS.of(input)}, the same input root {@code synthesizeImplicitInputNavigation} gives the
     * bare-attr form (#579's own two-forms-agree mirror).
     *
     * <p>The walk mirrors legacy's gate ({@code nearestEnclosingInlineFunction(expr) == null &&
     * findEnclosingRule(expr) != null}) and adds the three stops the probe's own {@code itemBind} classifier
     * names this residue against, so the predicate claims EXACTLY the measured {@code noLambda} population and
     * nothing else: a closer {@link RInlineFunction} is the standing binder legs' seat (the {@code named.} /
     * {@code implicit.} / {@code thenBody} faces), a closer {@link RSwitchExpr} the #523 arm-B
     * {@code switchCrossed} seat, and an {@link RFunction} boundary means a FUNCTION body, whose implicit
     * input is not the rule {@code input}. A from-type-less rule then declines - a fourth decline, not a
     * fourth stop: prove-or-decline, since the render needs the from-type to synthesize the receiver. That
     * from-type requirement is NOT {@code itemNavReceiverInputSlot}'s, which does not carry one; it comes from
     * the seat that synthesizes the same receiver for the bare-attr form ({@code ReferenceHandler
     * .synthesizeImplicitInputNavigation}) and from this adapter's own #514 arm.
     *
     * <p>Two of the three stops are belts over the caller's own guard chain, MEASURED as such (PR #638 round 1,
     * cq MF-3; lanes W3 / W4 delete them one at a time and the witness stays GREEN). The third is a belt over
     * the FRONT END, and its first description here was wrong (round 2, cq MF-1). The {@link RInlineFunction}
     * stop's residue is an {@code item} under a NAMED binder - a then/min/max/sort body with a parameter, or
     * any reduce body: {@link #newLegItemBindingSource} returns {@code null} for a named lambda, so
     * {@code itemBindingBound} is FALSE there and the caller's {@code !itemBindingBound(ruleIv)} conjunct does
     * NOT exclude it. The first cut said the conjunct covered this stop "twice over", on the evidence of lane
     * W2 - whose only carrier was a FILTER body, which the receiver gate and the conjunct both exclude, so W2
     * was green by construction and said nothing about the stop. What excludes the named-binder class today
     * is {@code TypeInferenceEngine.computeImplicitItemType}'s D41-LOCK: the literal {@code item} keyword is
     * shadowed by an explicit closure parameter and keeps the type MISSING, so no feature resolves off it and
     * the claim declines at {@code featureUnresolved} before any receiver gate - the same case split as the
     * switch stop's below, measured on a named sort and a reduce body (negative control 4; the named-then carrier
     * was retired at v3.3 seat 3 - it rode the fork-only {@code then l [ ... ]} shape); the
     * census's zero {@code named.} / {@code thenBody} faces in the class is why the corpus never reaches it.
     * Lanes T1 / T2 measure what the stop guards, by dropping that lock in the parser: with the lock dropped
     * and the stop present control 4's tripwire (a) goes RED - the #638 T1 print carried the fixed message alone, so
     * WHERE the claim declined was read from the code path (the residue block is entered, this walk returns
     * {@code null} at the {@link RInlineFunction}, the arm is not taken - the receiver gate); since v3.3 seat 3
     * the message carries the reason the adapter read, so the next lane print names it; with the
     * lock dropped AND the stop deleted the arm MINTS the rule input where legacy's
     * {@code nearestEnclosingInlineFunction(expr) == null} gate would not render it. A belt today, load-bearing
     * the day that lock moves - both measured, not argued. The {@link RFunction} stop cannot fire at all,
     * since no {@link RRule} can enclose an {@link RFunction} and the walk would run off the top and return
     * {@code null} anyway (the same defensive leg {@link #enclosingRule} carries). The {@link RSwitchExpr}
     * stop is the one round 1 predicted had blast radius, reasoning that
     * {@link #switchCaseNarrowedBinding} binds ONLY a case whose {@code resolvedGuard} is present and an
     * {@link RDataType}, so a case body under a default, enum or literal key would reach this arm with
     * {@code itemBindingBound} FALSE where legacy binds {@code item} to the switch SUBJECT (the #221
     * {@code switchChoiceHoist} arm, which runs BEFORE {@code itemNavReceiverInputSlot}). Cut as a fixture it
     * does not, and the case split is why: this arm mints for an {@link RFeatureCall} only; a case that is NOT
     * type-guarded narrows {@code item} to nothing, so the linker resolves no feature off it and the claim
     * declines at {@code featureUnresolved} before any receiver gate; and a case that IS type-guarded is
     * exactly what {@link #switchCaseNarrowedBinding} binds, so the conjunct excludes it. The witness pins
     * BOTH branches (negative control 3), because the stop becomes load-bearing the moment the first one
     * changes - a teach that narrows {@code item} on a non-type-guarded case.
     */
    private static RRule ruleInputBoundItem(RImplicitVariable iv) {
        RNode cur = iv == null ? null : iv.parent();
        int depth = 0;
        while (cur != null && depth++ < PARENT_WALK_LIMIT) {
            if (cur instanceof RInlineFunction) {
                return null; // a closer lambda owns `item` - the standing binder legs' seat
            }
            if (cur instanceof RSwitchExpr) {
                return null; // a case-narrowed subject - the #523 arm-B seat
            }
            if (cur instanceof RFunction) {
                return null; // a FUNCTION body: its implicit input is not the rule `input`
            }
            if (cur instanceof RRule rule) {
                return rule.fromType().isPresent() ? rule : null;
            }
            cur = cur.parent();
        }
        return null;
    }

    /**
     * #506: the L-029 meta split of the widened item admission, per leg. The filter/extract
     * legs keep the ORIGINAL heuristic verbatim (a direct meta-annotated feature-call source —
     * the dominant shape; byte-unchanged behavior on the standing admission). The NEW legs
     * take the STRONG allowlist proof ({@link #isProvablyNonMetaElementSource} on the binding
     * source, prove-or-decline): the #506 probe2 catch — a SORT comparator item over a
     * meta-OUTPUT function call rendered without legacy {@code NavigationHandler}'s null-safe
     * gm-aware {@code "Type coercion"} legs (the args-present/alias/deep source shapes are
     * outside any direct-feature heuristic), so an unprovable element form declines whole at
     * the widened seats — the honest residue.
     */
    private static boolean itemBindingMetaSourced(RImplicitVariable iv) {
        if (isFilterOrExtractBound(iv)) {
            RExpression source = filterExtractBindingSource(iv);
            return source instanceof RFeatureCall fc
                    && fc.resolvedFeature().isPresent()
                    && isMetaAnnotated(fc.resolvedFeature().get());
        }
        // #523 arm-B: the switch-case leg's own L-029 read — the narrowed case value is the
        // subject's choice OPTION, so the meta verdict is the option attribute's (the guard
        // type is meta-blind); prove-plain-or-decline via switchCaseOptionMetaSuspect.
        RDataType switchNarrowed = switchCaseNarrowedBinding(iv);
        if (switchNarrowed != null) {
            return switchCaseOptionMetaSuspect(iv, switchNarrowed);
        }
        RExpression source = newLegItemBindingSource(iv);
        return source == null || !isProvablyNonMetaElementSource(source);
    }

    /**
     * #506: the binding SOURCE of the WIDENED receiver-seat legs — the nearest inline
     * function when it is an implicit/paramless {@code then}/{@code min}/{@code max}/
     * {@code sort} body (the value the item takes: a min/max/sort item ranges the argument's
     * elements, a then item the whole pipe value — either way the op's ARGUMENT is the source
     * whose element form governs the L-029 split). {@code null} for the filter/extract pair
     * (the original legs keep their own helpers), a named body, or no lambda.
     */
    private static RExpression newLegItemBindingSource(RNode start) {
        RNode cur = start == null ? null : start.parent();
        int depth = 0;
        while (cur != null && depth++ < PARENT_WALK_LIMIT) {
            if (cur instanceof RInlineFunction inline) {
                if (!(inline.isImplicit() || inline.paramNames().isEmpty())) {
                    return null;
                }
                RNode p = inline.parent();
                if (p instanceof RThenExpr then && then.body().orElse(null) == inline) {
                    return then.argument();
                }
                if (p instanceof RMinExpr min && min.body().orElse(null) == inline) {
                    return min.argument();
                }
                if (p instanceof RMaxExpr max && max.body().orElse(null) == inline) {
                    return max.argument();
                }
                if (p instanceof RSortExpr sort && sort.body().orElse(null) == inline) {
                    return sort.argument();
                }
                return null;
            }
            cur = cur.parent();
        }
        return null;
    }

    /**
     * #523 arm-B: the SWITCH-CASE binding of a lambda-less implicit item — legacy
     * {@code ReferenceHandler.nearestEnclosingSwitchSubject}'s own boundary precedence mirrored
     * (an {@link RInlineFunction} FIRST owns the item, decline; an
     * {@link com.regnosys.rosetta.ast.expressions.supporting.RSwitchCase} FIRST binds it to the
     * #221 case-narrowed subject — legacy {@code handle(RImplicitVariable)}'s switchChoiceHoist
     * arm resolves it to the cast case var the rendering instanceof ladder bound on the scope,
     * and the emitter's {@code ImplicitItemRenderer} reuses that handler VERBATIM with the live
     * scope, so the binding decision is legacy's own at emit time — the #506 admission-lag law
     * at the SWITCH binder). Returns the case's narrowed {@link RDataType} read off the
     * linker's {@code resolvedGuard} channel (the SAME resolution the ladder's cast uses — the
     * PR #460 channel legacy {@code caseNarrowedImplicitType} reads first), or {@code null}
     * when the boundary is not a TYPE-guarded switch case: an enum/literal-keyed case never
     * ladder-renders a case subject, a default case narrows nothing, and an unresolved guard
     * proves nothing (prove-or-decline).
     */
    private static RDataType switchCaseNarrowedBinding(RImplicitVariable iv) {
        RNode cur = iv == null ? null : iv.parent();
        int depth = 0;
        while (cur != null && depth++ < PARENT_WALK_LIMIT) {
            if (cur instanceof RInlineFunction) {
                return null; // a closer lambda owns `item` — the standing binder legs' seat
            }
            if (cur instanceof com.regnosys.rosetta.ast.expressions.supporting.RSwitchCase scase) {
                return scase.guard()
                        .flatMap(com.regnosys.rosetta.ast.expressions.supporting
                                .RSwitchCaseGuard::resolvedGuard)
                        .filter(RDataType.class::isInstance)
                        .map(RDataType.class::cast)
                        .orElse(null);
            }
            cur = cur.parent();
        }
        return null;
    }

    /**
     * #523 arm-B: the L-029 meta belt of the switch-case binding leg — the narrowed case value
     * is the switch SUBJECT's own choice OPTION, so the item's Java value is a wrapper exactly
     * when that option attribute is meta-annotated (the engine's narrowing types the item from
     * the plain GUARD type and cannot see the option's {@code [metadata …]} annotation — the
     * blind spot this belt closes). Prove-or-decline: the subject must resolve to its declared
     * {@link RChoice} ({@link #declaredDirectChoiceOf} — the linker-bound channels), the
     * narrowed type must name one of its options, and the option's projected attribute
     * ({@link #choiceOptionAsAttribute} — annotationRefs carried) must be meta-free; any
     * resolution failure declines (returns {@code true} = meta-suspect).
     */
    private static boolean switchCaseOptionMetaSuspect(RImplicitVariable iv, RDataType narrowed) {
        com.regnosys.rosetta.ast.expressions.unary.RSwitchExpr sw = null;
        RNode cur = iv == null ? null : iv.parent();
        int depth = 0;
        while (cur != null && depth++ < PARENT_WALK_LIMIT) {
            if (cur instanceof com.regnosys.rosetta.ast.expressions.unary.RSwitchExpr s) {
                sw = s;
                break;
            }
            cur = cur.parent();
        }
        if (sw == null || sw.argument() == null || narrowed == null || narrowed.name() == null) {
            return true;
        }
        RChoice subjectChoice = declaredDirectChoiceOf(sw.argument());
        if (subjectChoice == null) {
            return true;
        }
        for (RChoiceOption opt : subjectChoice.options()) {
            RTypeCall tc = opt.typeCall();
            if (tc != null && narrowed.name().equals(tc.typeName())) {
                return isMetaAnnotated(choiceOptionAsAttribute(opt));
            }
        }
        return true; // the narrowed type is not one of the subject's options — decline
    }

    /**
     * Whether a lowered operand is the bare filter/extract-bound implicit {@code item} — the OPERAND-side analogue
     * of {@link #isFilterExtractBoundItemReceiver} (the bare-item-operand slice). Admitted as a comparison /
     * equality / existence operand when, and ONLY when, the lowered operand is a NON-synthetic
     * {@link IRVariable.VariableKind#USER_ITEM} of <strong>SINGLE</strong> cardinality carrying a resolved
     * NON-meta type, AND its raw {@link RImplicitVariable}'s nearest binding lambda is a {@link RFilterExpr} /
     * {@link RExtractExpr} body. The emitter renders it as the bare lambda parameter {@code item} (the same L-080
     * {@code emitVariable} arm the receiver slice uses — no {@code MapperS.of} wrap), so a fixed idiom composes it
     * byte-identically to legacy's bare lambda param ({@code exists(item)} / {@code areEqual(item, …)} /
     * {@code greaterThan(item, …)}).
     *
     * <p>Unlike {@link #isFilterExtractBoundItemReceiver} this REQUIRES SINGLE cardinality: a navigation receiver
     * admits any cardinality, but a comparison/equality operand requires a SINGLE result (the same constraint
     * {@link #isScalarOperand} enforces via {@link #isScalarParam}). A filter/extract {@code item} is structurally
     * SINGLE (the per-element binding — {@code CardinalityComputer} returns SINGLE for an {@code RImplicitVariable}
     * unconditionally), so the guard is a no-op today; it is belt-and-suspenders that fails CLOSED (declines) if
     * that invariant ever regresses, never silently mis-rendering a multi operand. Existence is itself
     * cardinality-agnostic, so the shared SINGLE guard only narrows it harmlessly (a multi item is unrepresentable).
     *
     * <p>Public since #528 as the opResGate census's own BY-CALL vocabulary (the #524 census
     * BY-CALL law — the {@code operandItem} face's admission read, paired with
     * {@link #isTypedUserItemArg}'s binder-unbounded ARG-seat gate so the census can price the
     * operand-seat widening directly); read-only.
     */
    public static boolean isFilterExtractBoundItemOperand(IRExpr operand, RExpression rawOperand) {
        return operand instanceof IRVariable var
                && var.variableKind() == IRVariable.VariableKind.USER_ITEM
                && operand.cardinality() == ExpressionCardinality.SINGLE
                && operand.type() != null && !operand.type().isMissing() && !operand.type().hasMeta()
                && rawOperand instanceof RImplicitVariable
                && isFilterOrExtractBound((RImplicitVariable) rawOperand);
    }

    /**
     * #528 arm-3 — the COMPARISON seat's item admission, binder-BLIND: the #502 ARG-seat gate
     * ({@link #isTypedUserItemArg}) applied at the comparison operand seat, replacing the
     * filter/extract-bound narrow gate there. The opResGate census read the residue at one
     * DECODE across the position-split pair of bucket tokens (at-root vs interior — the
     * Seat-1 MF-3 recut; {@code operandItem} 2 sole at the #527 SOT — a {@code number}-typed
     * SINGLE USER item against a literal sibling, bound by a #506-widened {@code then}/
     * {@code min}/{@code max}/{@code sort} body, {@code func:StandardizedScheduleVariance-
     * SwapNotionalAmount}) and reported {@code argGate:yes} for both carriers: the binder-unbounded gate
     * already admits this exact operand at the ARG seat, so the residue was pure admission
     * lag (the #502 law — the emitter's installed {@code implicitItemRenderer} reuses legacy
     * {@code handle(RImplicitVariable)} VERBATIM on the range-correlated node, so the binding
     * decision is legacy's own at emit time whatever the binder).
     *
     * <p>The route is the #518 EQUALITY precedent applied at its sibling seat: the compiler's
     * comparison shape leg takes the item operand KIND-WIDE, so the containing root renders
     * WHOLE through the oracle-root comparison serve (the literal {@code super.visitComparison}
     * line — legacy's own operand render, numeric-type threading and literal coercion inside
     * it) and the previously filter/extract-bound NATIVE composes at this seat reroute to that
     * serve BY IDENTITY. The kind-wide leg is forced, not chosen: the bound/unbound split is a
     * RAW-side fact the lowered tree cannot key (the #518 finding verbatim), so a carve-out
     * keeping the standing composes native is unexpressible — the moved mass lands on the
     * oracleRootLowered receipt.
     *
     * <p>Public since the #528 fix commit as the opResGate census's comparison-seat BY-CALL
     * read (the Copilot #528 R1 catch — the census's failing-side classifier had kept the
     * pre-arm bound gate; it now composes exactly this predicate with
     * {@link #isComparisonOperand}, mirroring {@code adaptComparison}'s own gate); read-only.
     */
    public static boolean isBinderBlindItemOperand(IRExpr operand, RExpression rawOperand) {
        return isTypedUserItemArg(operand, rawOperand);
    }

    /**
     * #502 — the ARG-seat item admission, binder-UNBOUNDED: a typed, non-meta, SINGLE explicit
     * {@code item} argument in ANY binding context (then-body, rule-top, filter/extract alike —
     * the #502 argItem census read the face ~100% {@code user.userUnbound.fn}, typed
     * everywhere). The #491 teach made the emitter's USER_ITEM render binding-general — the
     * installed {@code implicitItemRenderer} reuses legacy {@code handle(RImplicitVariable)}
     * VERBATIM on the range-correlated node, so the binding decision (lambda param / thenArg /
     * {@code MapperS.of(input)}) is legacy's own at emit time, and the historical
     * filter/extract bound was admission lag at the composed seats. The EQUALITY and EXISTENCE
     * operand seats keep the bound gate {@link #isFilterExtractBoundItemOperand}; the
     * COMPARISON operand seat went binder-blind at #528 ({@link #isBinderBlindItemOperand} —
     * arm 3).
     *
     * <p>Public since #528 as the opResGate census's own BY-CALL vocabulary (the #524 census
     * BY-CALL law — the {@code argItem} face's admission read, and the OPERAND-seat comparison
     * that prices this gate's binder-unbounded shape against the still-bound operand seat);
     * read-only.
     */
    public static boolean isTypedUserItemArg(IRExpr arg, RExpression rawArg) {
        return arg instanceof IRVariable var
                && var.variableKind() == IRVariable.VariableKind.USER_ITEM
                && arg.cardinality() == ExpressionCardinality.SINGLE
                && arg.type() != null && !arg.type().isMissing() && !arg.type().hasMeta()
                && rawArg instanceof RImplicitVariable;
    }

    /**
     * #529 — the ARG-seat item admission's DECLARATION channel (the #528 argItem residue's
     * teach: the opResGate row read {@code t:tMiss.b:unbound} — an UNRESOLVED switch-case
     * guard leaves the item untyped, so {@link #isTypedUserItemArg}'s type-channel meta
     * proof can never fire — while the #529 itemArgSrcGate census read the SUBJECT channel
     * {@code op:input.metaFree}). THE MECHANISM (the Seat-1 #529 MF-1 recut — the witness
     * golden {@code CheckCriteria} is the proof): for a CHOICE-declared subject legacy
     * renders the case value through its own switchArgument OPTION-NAV ladder ({@code
     * switchArgument.<T>map("getX", …)} — the item's Java value is the OPTION FIELD's
     * value, never an {@code instanceof} cast of the subject; the cast render exists only
     * for TYPE-keyed switches), so the item's meta shape belongs to the OPTION attribute —
     * exactly the #523 {@code switchCaseOptionMetaSuspect} doctrine at the receiver seat.
     * The proof therefore pairs TWO declaration reads: the SUBJECT input meta-free
     * ({@link #enclosingSwitchSubjectInput} + {@link #isMetaAnnotated}) AND the OPTION
     * channel meta-free ({@link #switchCaseOptionMetaFreeByDecl} — the guard's own resolved
     * option when it resolves, EVERY option of the subject's declared choice when it does
     * not [the census's {@code g:unres} class — the matched option is unknowable, so the
     * whole option set must prove plain]; a non-choice declared subject keeps the
     * subject-only proof — its case value is the subject's own narrowing). The gates: a
     * SINGLE explicit USER item whose nearest enclosing switch case (an
     * {@link RInlineFunction} bounds the walk first — the
     * {@link #switchCaseNarrowedBinding} precedence) passes both reads. The render stays
     * legacy's own {@code handle(RImplicitVariable)} at emit time (the #502
     * binding-general law), and the routing safety is the standing apply-arg SHAPE leg
     * (containsOracleLeaf carries the apply-with-an-item-arg shape), so every containing
     * claim root renders WHOLE through the oracle-root callArgs serve.
     */
    public static boolean isDeclProvenMetaFreeItemArg(IRExpr arg, RExpression rawArg) {
        if (!(arg instanceof IRVariable var)
                || var.variableKind() != IRVariable.VariableKind.USER_ITEM
                || arg.cardinality() != ExpressionCardinality.SINGLE
                || !(rawArg instanceof RImplicitVariable)) {
            return false;
        }
        RAttribute subjectInput = enclosingSwitchSubjectInput(rawArg);
        return subjectInput != null && !isMetaAnnotated(subjectInput)
                && switchCaseOptionMetaFreeByDecl(rawArg, subjectInput);
    }

    /**
     * #530 arm-D: the DEREF-PROVEN meta-param call argument (the metaArgGate census's
     * whole population — {@code dir:deref} 100%): a bare reference to the enclosing
     * function's {@code [metadata]}-annotated input, passed where the callee declares the
     * SAME value type, meta-free and single-cardinality — exactly legacy
     * {@code ReferenceHandler.tryMetaDerefArg}'s firing preconditions (the
     * {@code RJavaWithMetaValue} arg at a plain scalar param: the null-guarded
     * {@code getValue()} deref through the TypeCoercionService). The lowered
     * {@link IRMetaParamRef} IS the faithful arg; the deref render stays legacy's own —
     * the routing safety is the KIND leg ({@code IRMetaParamRef} is a standing
     * {@code containsOracleLeaf} member since #514), so every claim root containing this
     * call renders WHOLE through the oracle-root callArgs serve, byte-identical BY
     * IDENTITY. The value-type gate is INSTANCE identity on the referenced types (both
     * resolve in the same workspace — stronger than written-name equality); a meta,
     * multi-card or differently-typed param keeps the standing decline ({@code dir:other}
     * read ZERO at the #530 census — census-narrow). Public for the #530 metaArgGate
     * census's BY-CALL {@code dir} verdict (the Copilot #530 R1-C1 alignment — the census
     * reads THIS predicate, so the token and the gate cannot drift).
     */
    public static boolean isDerefProvenMetaParamArg(IRExpr lowered, RExpression rawArg,
            RAttribute param) {
        if (!(lowered instanceof IRMetaParamRef) || param == null
                || !(rawArg instanceof RSymbolReference argRef)
                || !(argRef.symbol().orElse(null) instanceof RAttribute callerParam)) {
            return false;
        }
        if (isMetaAnnotated(param) || isMultiCardinality(param)) {
            return false;
        }
        RNode paramType = param.typeCall() == null ? null
                : param.typeCall().referencedType().orElse(null);
        RNode valueType = callerParam.typeCall() == null ? null
                : callerParam.typeCall().referencedType().orElse(null);
        return paramType != null && paramType == valueType;
    }

    /**
     * #529 (the Seat-1 MF-1 belt) — the OPTION channel's declaration meta proof for a
     * switch-case item: a CHOICE-declared subject's case value is the matched OPTION
     * field's value (legacy's option-nav ladder — see {@link #isDeclProvenMetaFreeItemArg}),
     * so the meta verdict is the OPTION attribute's. A RESOLVED NAME guard proves through
     * its own option (matched by the option {@code typeCall}'s referenced type, the
     * {@link #caseNarrowedElementType} channel's identity); an unresolved/absent guard
     * proves only when EVERY option of the declared choice is meta-free (the conservative
     * direction — the matched option is unknowable); a non-choice declared subject returns
     * {@code true} (the subject-only proof carries — its case value is the subject's own
     * narrowing, no option field intervenes). Prove-or-decline; the residue keeps the
     * honest {@code argItem} face.
     */
    private static boolean switchCaseOptionMetaFreeByDecl(RExpression rawItem,
            RAttribute subjectInput) {
        RNode declared = subjectInput.typeCall() == null ? null
                : subjectInput.typeCall().referencedType().orElse(null);
        if (!(declared instanceof RChoice subjectChoice)) {
            return true; // a non-choice subject — the case value is the subject's narrowing
        }
        RNode cur = rawItem == null ? null : rawItem.parent();
        int depth = 0;
        RSwitchCase scase = null;
        while (cur != null && depth++ < PARENT_WALK_LIMIT) {
            if (cur instanceof RInlineFunction) {
                return false;
            }
            if (cur instanceof RSwitchCase c) {
                scase = c;
                break;
            }
            cur = cur.parent();
        }
        if (scase == null) {
            return false;
        }
        RSwitchCaseGuard guard = scase.guard().orElse(null);
        RNode resolvedGuard = guard == null || guard.kind() != SwitchGuardKind.NAME ? null
                : guard.resolvedGuard().orElse(null);
        if (resolvedGuard != null) {
            for (RChoiceOption option : subjectChoice.options()) {
                RTypeCall optionCall = option.typeCall();
                if (optionCall != null
                        && optionCall.referencedType().orElse(null) == resolvedGuard) {
                    RAttribute optAttr = choiceOptionAsAttribute(option);
                    return optAttr != null && !isMetaAnnotated(optAttr);
                }
            }
            // the resolved guard names no option of the declared choice — fall through to
            // the conservative whole-set read (the guard may be a supertype projection)
        }
        for (RChoiceOption option : subjectChoice.options()) {
            RAttribute optAttr = choiceOptionAsAttribute(option);
            if (optAttr == null || isMetaAnnotated(optAttr)) {
                return false; // an unprovable or meta-carrying option — prove-or-decline
            }
        }
        return true;
    }

    /**
     * #529 — the nearest enclosing switch case's SUBJECT, when it is a bare read of an
     * enclosing-function INPUT: the {@link #caseNarrowedElementType} walk's own boundary
     * precedence (an {@link RInlineFunction} FIRST owns the item — null; an
     * {@link RSwitchCase} FIRST binds it), then the case's parent {@link RSwitchExpr}'s
     * argument checked as an un-called {@link RSymbolReference} bound to one of the
     * enclosing function's inputs. Shared by {@link #isDeclProvenMetaFreeItemArg} and the
     * #529 itemArgSrcGate census token ({@link #itemArgSrcCensusToken} — the two reads
     * cannot drift). Null off-shape.
     */
    private static RAttribute enclosingSwitchSubjectInput(RNode start) {
        RNode cur = start == null ? null : start.parent();
        int depth = 0;
        RSwitchCase scase = null;
        while (cur != null && depth++ < PARENT_WALK_LIMIT) {
            if (cur instanceof RInlineFunction) {
                return null;
            }
            if (cur instanceof RSwitchCase c) {
                scase = c;
                break;
            }
            cur = cur.parent();
        }
        if (scase == null || !(scase.parent() instanceof RSwitchExpr sw)
                || !(sw.argument() instanceof RSymbolReference subjRef)
                || !subjRef.args().isEmpty()
                || !(subjRef.symbol().orElse(null) instanceof RAttribute subjAttr)) {
            return null;
        }
        RFunction enclosing = enclosingFunction(start);
        if (enclosing != null) {
            for (RAttribute in : enclosing.inputs()) {
                if (in == subjAttr) {
                    return subjAttr;
                }
            }
        }
        return null;
    }

    /**
     * #528 — the opResGate census's BINDING-class read of an item-shaped raw operand/argument,
     * composed from the adapter's OWN binder predicates BY CALL (the #524 census BY-CALL law):
     * {@code rawNotImplicit} (not an {@link RImplicitVariable} — contract-impossible for a lowered
     * item, the stable-token belt) / {@code feBound} (the ORIGINAL filter/extract pair —
     * {@link #isFilterOrExtractBound}: the one class both the operand and the arg seat admit) /
     * {@code opBound} (a #506 widened leg — an implicit/paramless {@code then}/{@code min}/
     * {@code max}/{@code sort} body, {@link #newLegItemBindingSource}) / {@code switchBound} (the
     * #523 arm-B lambda-less TYPE-guarded switch case, {@link #switchCaseNarrowedBinding}) /
     * {@code unbound} (no binder the walks recognise — the #502 {@code user.userUnbound} class the
     * ARG seat admits binder-blind). Each bound class appends {@code .metaSrc} when the seat's own
     * L-029 meta belt ({@link #itemBindingMetaSourced}) reads the binding source meta-suspect —
     * the fact that decides whether a widened admission would need legacy's gm-aware
     * {@code "Type coercion"} legs. Read-only; probe/census-only (no standing render consults it).
     */
    public static String itemBindingCensusToken(RExpression rawItem) {
        if (!(rawItem instanceof RImplicitVariable iv)) {
            return "rawNotImplicit";
        }
        String base;
        if (isFilterOrExtractBound(iv)) {
            base = "feBound";
        } else if (switchCaseNarrowedBinding(iv) != null) {
            base = "switchBound";
        } else if (newLegItemBindingSource(iv) != null) {
            base = "opBound";
        } else {
            return "unbound"; // no binder — itemBindingMetaSourced's residue path, not a class
        }
        return itemBindingMetaSourced(iv) ? base + ".metaSrc" : base;
    }

    /**
     * #529 — the itemHeadGate census's BY-CALL read of a {@code noResolutionChannel.headMiss
     * .other} head against the IMPLICIT-ITEM channel (the #524 census BY-CALL law): the
     * {@link #bareItemMemberResolution} ladder's own gates re-run READ-ONLY from the
     * {@link REnumValueRef}'s tree position with the node's HEAD name — the #507 record arm's
     * ladder searches function scope, dispatch base and closure params only, and the standing
     * noChanGate census restates exactly that ladder, so every live face reads
     * {@code headMiss.leafMiss} while the head actually lives on the enclosing BINDER's
     * element form (the goldens render the implicit-item read). Early-decline tokens in the
     * ladder's own order ({@code nameNull} / {@code oe} an only-exists element /
     * {@code cp} a closure-param name / {@code fnScope} a function-scope name /
     * {@code noBinder} outside the filter/extract binder slice), else the walked state:
     * {@code src:<class>} the binder source's form, {@code elem:} the element derivation
     * ({@code data} via {@link #sourceElementDataType} / {@code choiceDirect} via
     * {@link #declaredDirectChoiceOf} / {@code null}), {@code head:} the head name's by-name
     * resolution on the element form ({@code attrHit.plain}/{@code attrHit.meta} via
     * {@link #findAttributeOnDataTypeByName} + {@link #isMetaAnnotated} / {@code optHit} the
     * choice-super-option leg / {@code miss}), and {@code leaf:} the LEAF name's ladder on
     * the resolved head attribute in legacy's member-first order (the #525 law — type
     * features ahead of metafields): {@code recHit:<record>} a record feature by name (the
     * goldens render the record ladder, e.g. {@code zonedDateTime -> date}) /
     * {@code attrHit.plain}/{@code attrHit.meta} a data-type member / {@code qual:<name>} a
     * #512-pattern meta qualifier the head itself carries (the goldens render the qualifier
     * deref) / {@code miss}; {@code na} when the head never resolved. Read-only;
     * probe/census-only (no standing render consults it).
     */
    public static String itemHeadCensusToken(REnumValueRef enr) {
        String name = enr.enumName();
        String leafName = enr.valueName();
        if (name == null || name.isEmpty()) {
            return "nameNull";
        }
        if (hasOnlyExistsElementAncestor(enr)) {
            return "oe";
        }
        if (isEnclosingClosureParamName(enr, name)) {
            return "cp";
        }
        RFunction enclosing = enclosingFunction(enr);
        if (enclosing != null && nameResolvesInFunctionScope(enclosing, name)) {
            return "fnScope";
        }
        RExpression source = filterExtractSourceOfArmBinder(enr);
        if (source == null) {
            return "noBinder";
        }
        String src = "src:" + source.getClass().getSimpleName();
        RDataType elem = sourceElementDataType(source);
        RAttribute head = null;
        String headTok = "head:miss";
        String elemTok;
        if (elem != null) {
            elemTok = "elem:data";
            head = findAttributeOnDataTypeByName(elem, name);
            if (head != null) {
                headTok = "head:attrHit." + (isMetaAnnotated(head) ? "meta" : "plain");
            } else {
                head = findChoiceSuperOption(elem, name);
                if (head != null) {
                    headTok = "head:optHit";
                }
            }
        } else if (declaredDirectChoiceOf(source) != null) {
            elemTok = "elem:choiceDirect";
        } else {
            elemTok = "elem:null";
        }
        String leafTok = "leaf:na";
        if (head != null && leafName != null && !leafName.isEmpty()) {
            leafTok = "leaf:miss";
            RRecordType rec = declaredRecordTypeOf(head);
            RDataType headData = rec == null ? declaredDataTypeOf(head) : null;
            if (rec != null) {
                for (RRecordFeature f : rec.features()) {
                    if (leafName.equals(f.name())) {
                        leafTok = "leaf:recHit:" + rec.name();
                        break;
                    }
                }
            }
            if ("leaf:miss".equals(leafTok) && headData != null) {
                RAttribute leafAttr = findAttributeOnDataTypeByName(headData, leafName);
                if (leafAttr != null) {
                    leafTok = "leaf:attrHit." + (isMetaAnnotated(leafAttr) ? "meta" : "plain");
                }
            }
            if ("leaf:miss".equals(leafTok) && isMetaAnnotated(head)
                    && metaQualifierNames(head).contains(leafName)) {
                leafTok = "leaf:qual:" + leafName;
            }
        }
        return src + "." + elemTok + "." + headTok + "." + leafTok;
    }

    /**
     * #529 — the headAttrGate census's BY-CALL read of a {@code featureUnresolved.headAttr
     * .declMiss} head (the #492 {@link #featureUnresolvedFacet} resolution restated — the
     * facet's own ladder already excluded the record classes and the derivable data types, so
     * this token names WHICH remaining type class the resolved head attribute declares):
     * {@code ht:noCall} no type call at all / {@code ht:unres:<writtenName>} a type call
     * whose referenced type never resolved (the ingest wall's own type names) /
     * {@code ht:enumT:<name>} / {@code ht:choice:<name>} / {@code ht:basic:<name>} the
     * basic-type classes / {@code ht:offLadder:<class>} the contract-impossible record/data
     * forms (the facet would have read {@code recHit}/{@code recMiss}/{@code memberMiss} —
     * the defensive belt). Paired with the #512 qualifier verdict on the FEATURE name
     * ({@code qual:hit:<name>} — the head is meta-annotated AND carries the feature as one
     * of its own qualifiers, the class legacy renders through the metafield deref ladder;
     * {@code qual:offList} meta-annotated but the name is not carried; {@code qual:noMeta}).
     * Off-contract shapes read {@code offContract} (impossible under the reason self-gate).
     * Read-only; probe/census-only.
     */
    public static String headAttrCensusToken(RFeatureCall fc) {
        if (!(fc.receiver() instanceof RSymbolReference head)
                || !(head.symbol().orElse(null) instanceof RAttribute attr)) {
            return "offContract";
        }
        RTypeCall tc = attr.typeCall();
        String ht;
        if (tc == null) {
            ht = "ht:noCall";
        } else {
            RNode t = tc.referencedType().orElse(null);
            String written = tc.typeName() == null || tc.typeName().isEmpty()
                    ? "?" : tc.typeName();
            if (t == null) {
                ht = "ht:unres:" + written;
            } else if (t instanceof RRecordType || t instanceof RDataType) {
                ht = "ht:offLadder:" + t.getClass().getSimpleName();
            } else if (t instanceof REnumeration) {
                ht = "ht:enumT:" + written;
            } else if (t instanceof RChoice) {
                ht = "ht:choice:" + written;
            } else {
                ht = "ht:basic:" + written;
            }
        }
        String featureName = fc.featureName();
        String qual;
        if (!isMetaAnnotated(attr)) {
            qual = "qual:noMeta";
        } else if (featureName != null && metaQualifierNames(attr).contains(featureName)) {
            qual = "qual:hit:" + featureName;
        } else {
            qual = "qual:offList";
        }
        return ht + "." + qual;
    }

    /**
     * #529 — the itemArgSrcGate census's BY-CALL read of an un-admitted {@code argItem}
     * item's BINDER SOURCE (the #528 opResGate row read {@code b:unbound.t:tMiss} — no
     * binder the standing walks recognise types the item, so this token asks the PROVABLE
     * channels a teach could read instead): the nearest {@link RSwitchCase} ancestor's guard
     * class by the {@link #caseNarrowedElementType} walk's own reads ({@code sw:none} when
     * an inline function bounds the walk first or no case encloses the item — the
     * {@link #switchCaseNarrowedBinding} precedence; {@code g:default} a guard-less case /
     * {@code g:<kind>} a non-NAME guard / NAME guards by their {@code resolvedGuard} class:
     * {@code g:data:<name>} the #523-bindable class, {@code g:choice:<name>},
     * {@code g:enumT:<name>}, {@code g:enumV} an enum-VALUE guard, {@code g:unres}), and the
     * SWITCH SUBJECT's own declaration channel ({@code op:input.metaFree}/{@code op:input
     * .meta} — a bare read of an enclosing-function input, its meta verdict via
     * {@link #isMetaAnnotated} on the DECLARATION, the meta-freedom fact a missing item type
     * cannot carry; {@code op:<class>} other subject forms; {@code op:none} a without-left
     * switch). Read-only; probe/census-only.
     */
    public static String itemArgSrcCensusToken(RExpression rawItem) {
        RNode cur = rawItem == null ? null : rawItem.parent();
        int depth = 0;
        RSwitchCase scase = null;
        while (cur != null && depth++ < PARENT_WALK_LIMIT) {
            if (cur instanceof RInlineFunction) {
                return "sw:none";
            }
            if (cur instanceof RSwitchCase c) {
                scase = c;
                break;
            }
            cur = cur.parent();
        }
        if (scase == null) {
            return "sw:none";
        }
        RSwitchCaseGuard guard = scase.guard().orElse(null);
        String guardTok;
        if (guard == null) {
            guardTok = "g:default";
        } else if (guard.kind() != SwitchGuardKind.NAME) {
            guardTok = "g:" + guard.kind();
        } else {
            RNode resolved = guard.resolvedGuard().orElse(null);
            if (resolved instanceof RDataType dt) {
                guardTok = "g:data:" + dt.name();
            } else if (resolved instanceof RChoice ch) {
                guardTok = "g:choice:" + ch.name();
            } else if (resolved instanceof REnumeration en) {
                guardTok = "g:enumT:" + en.name();
            } else if (resolved != null) {
                guardTok = "g:" + resolved.getClass().getSimpleName();
            } else {
                guardTok = "g:unres";
            }
        }
        RNode swNode = scase.parent();
        String opTok = "op:none";
        if (swNode instanceof RSwitchExpr sw && sw.argument() != null) {
            RExpression subject = sw.argument();
            opTok = "op:" + subject.getClass().getSimpleName();
            // The shared #529 subject read ({@link #enclosingSwitchSubjectInput} — the
            // SAME walk the isDeclProvenMetaFreeItemArg admission gate runs; the census
            // and the arm cannot drift).
            RAttribute subjInput = enclosingSwitchSubjectInput(rawItem);
            if (subjInput != null) {
                opTok = "op:input." + (isMetaAnnotated(subjInput) ? "meta" : "metaFree");
            }
        }
        return guardTok + "." + opTok;
    }

    /**
     * #530 — the pipeCondGate census's BY-CALL read of an {@code attrOutsideFunction
     * .sourceElementUnresolved} face (the elided-pipe deep bottoms — the drr-r
     * {@code deep:RThenExpr.b:RExtractExpr.b:RConditionalExpr} residue): re-runs the arm's
     * OWN source derivation ({@link #filterExtractSourceOfArmBinder} +
     * {@link #resolveElidedPipedSource}) and descends the then/extract BODY chain with the
     * SAME steps {@link #sourceElementDataType} walks, naming WHERE the walk actually
     * bottoms — the #521 structural qualifier showed the SHAPE but not the bottom. Tokens:
     * {@code attr:<name>} the sought attribute ({@code attrGone}/{@code srcGone} the
     * defensive belts), then either {@code bottom:<class>} (a non-conditional bottom) or
     * the conditional's own reads — {@code then:<class>:<hit:<type>|miss>} /
     * {@code else:<class>:<hit:<type>|miss|null>} (each branch via
     * {@link #sourceElementDataType}'s own arms), {@code join:<hit:<type>|miss>} (the #487
     * conditional arm itself, run on the conditional — the by-call join), and the
     * identity-guard pre-read on a hit join ({@code idHit}/{@code idMiss} —
     * {@link #findAttributeOnDataTypeByName} against the sought attribute, the arm's own
     * guard; {@code idNa} when the join missed). Read-only; probe/census-only (no standing
     * render consults it).
     */
    public static String pipeDeepCensusToken(RSymbolReference ref) {
        RNode symbol = ref.symbol().orElse(null);
        RAttribute attr = symbol instanceof RAttribute a ? a : null;
        String attrTok = attr == null ? "attrGone" : "attr:" + attr.name();
        RExpression source = filterExtractSourceOfArmBinder(ref);
        if (source == null) {
            return attrTok + ".srcGone";
        }
        RExpression cur = resolveElidedPipedSource(source);
        int depth = 0;
        while (depth++ < SOURCE_WALK_DEPTH_LIMIT) {
            if (cur instanceof RThenExpr then) {
                cur = then.body().map(RInlineFunction::body).orElse(null);
            } else if (cur instanceof RExtractExpr extract) {
                RInlineFunction bodyFn = extract.body();
                cur = bodyFn == null ? null : bodyFn.body();
            } else {
                break;
            }
        }
        if (!(cur instanceof RConditionalExpr cond)) {
            return attrTok + ".bottom:"
                    + (cur == null ? "null" : cur.getClass().getSimpleName());
        }
        String thenTok = pipeCondBranchToken("then", cond.thenBranch());
        String elseTok = pipeCondBranchToken("else", cond.elseBranch().orElse(null));
        RDataType joined = sourceElementDataType(cond);
        String joinTok = joined == null ? "join:miss" : "join:hit:" + joined.name();
        String idTok;
        if (joined == null || attr == null) {
            idTok = "idNa";
        } else {
            idTok = findAttributeOnDataTypeByName(joined, attr.name()) == attr
                    ? "idHit" : "idMiss";
        }
        return attrTok + "." + thenTok + "." + elseTok + "." + joinTok + "." + idTok;
    }

    /** The per-branch read for {@link #pipeDeepCensusToken} — class + derivation verdict. */
    private static String pipeCondBranchToken(String seat, RExpression branch) {
        if (branch == null) {
            return seat + ":null";
        }
        RDataType t = sourceElementDataType(branch);
        return seat + ":" + branch.getClass().getSimpleName()
                + (t == null ? ":miss" : ":hit:" + t.name());
    }

    /**
     * #530 — the nsrCtGate census's ITEM-receiver refinement (probe4): the #512-B2
     * qualifier gate's OWN sub-verdicts BY CALL for an {@code item -> <feature>} face —
     * {@code notItem} / {@code unbound} ({@link #isFilterOrExtractBound} declines) /
     * {@code bound.<SrcClass>.termNull} ({@link #terminalBindingSourceFeature} bottoms —
     * the walk's own decline) / {@code bound.<SrcClass>.term:<name>.<meta|plain>
     * .<qHit|qMiss>} (the terminal's meta verdict + the feature name's qualifier
     * membership — {@link #metaQualifierNames}). Read-only; probe/census-only.
     */
    public static String qualItemCensusToken(RFeatureCall fc) {
        if (!(fc.receiver() instanceof RImplicitVariable iv)) {
            return "notItem";
        }
        if (!isFilterOrExtractBound(iv)) {
            return "unbound";
        }
        RExpression src = filterExtractBindingSource(iv);
        String srcTok = src == null ? "srcNull" : src.getClass().getSimpleName();
        RAttribute terminal = terminalBindingSourceFeature(src, 0);
        if (terminal == null) {
            return "bound." + srcTok + ".termNull";
        }
        String meta = isMetaAnnotated(terminal) ? "meta" : "plain";
        String qual = fc.featureName() != null
                && metaQualifierNames(terminal).contains(fc.featureName()) ? "qHit" : "qMiss";
        return "bound." + srcTok + ".term:" + terminal.name() + "." + meta + "." + qual;
    }

    /**
     * #530 — the kvpGate census's BY-CALL read of the {@code symbolNotAttribute} survivor
     * seats (the #522 arm's residue: {@code sym:RDataType.p:RKeyValuePair} /
     * {@code .p:RConditionalExpr} + {@code sym:RBody.p:RConditionalExpr}): the arm's OWN
     * decline ladder ({@link #adaptNonAttrBareItemNav}'s symbol-class gate +
     * {@link #bareItemMemberResolution}'s gates in legacy's ladder order) re-run
     * read-only, naming the EXACT leg that declines plus the seat detail the flat parent
     * facet hides. Tokens: {@code sym:<TypeName>} (the bound data type's own name;
     * {@code sym:<Class>} for non-data-type symbols) + the seat
     * ({@code seat:kvp:<key>[:asKey]} / {@code seat:cond:<if|then|else>} /
     * {@code seat:<class>}) + the leg — {@code classGate} (the admitted-symbol-list
     * decline: legacy's variable path, the RBody face) / {@code nameGate}/{@code oe}/
     * {@code cp}/{@code fnScope}/{@code noBinder} (the ladder's early declines in order) /
     * {@code elem:<data|choiceDirect|null>.member:<hit|miss>} (the element-channel
     * verdicts) / {@code armDecline} (member hit but the equivalent/typing legs declined) /
     * {@code claims} (the arm would claim — a stale-face tripwire) / {@code ladderNull}
     * (defensive — the restated gates above are the ladder's own). Read-only;
     * probe/census-only.
     */
    public String kvpCensusToken(RSymbolReference ref, RWorkspace ws) {
        RNode symbol = ref.symbol().orElse(null);
        String symTok;
        if (symbol instanceof RDataType dt) {
            symTok = "sym:" + dt.name();
        } else if (symbol == null) {
            symTok = "sym:null";
        } else {
            symTok = "sym:" + symbol.getClass().getSimpleName();
        }
        RNode p = ref.parent();
        String seatTok;
        if (p instanceof RKeyValuePair kvp) {
            seatTok = "seat:kvp:" + (kvp.key() == null ? "?" : kvp.key())
                    + (kvp.isAsKey() ? ":asKey" : "");
        } else if (p instanceof RConditionalExpr cond) {
            seatTok = "seat:cond:" + (cond.condition() == ref ? "if"
                    : cond.thenBranch() == ref ? "then" : "else");
        } else {
            seatTok = "seat:" + (p == null ? "orphan" : p.getClass().getSimpleName());
        }
        return symTok + "." + seatTok + "." + kvpDeclineLeg(ref, symbol, ws);
    }

    /** The decline-leg read for {@link #kvpCensusToken} — the arm's gates in ladder order. */
    private String kvpDeclineLeg(RSymbolReference ref, RNode symbol, RWorkspace ws) {
        if (!(symbol == null || symbol instanceof RDataType || symbol instanceof RChoice
                || symbol instanceof RRecordType || symbol instanceof REnumeration
                || symbol instanceof RBasicType || symbol instanceof RSegmentDef
                || symbol instanceof RAnnotation)) {
            return "classGate";
        }
        String name = ref.name();
        if (name == null || name.isEmpty()) {
            return "nameGate";
        }
        if (hasOnlyExistsElementAncestor(ref)) {
            return "oe";
        }
        if (isEnclosingClosureParamName(ref, name)) {
            return "cp";
        }
        RFunction enclosing = enclosingFunction(ref);
        if (enclosing != null && nameResolvesInFunctionScope(enclosing, name)) {
            return "fnScope";
        }
        if (filterExtractSourceOfArmBinder(ref) == null) {
            return "noBinder";
        }
        BareItemResolution res = bareItemMemberResolution(ref);
        if (res == null) {
            return "ladderNull";
        }
        String elemTok = res.elementType() != null ? "elem:data"
                : res.directChoice() != null ? "elem:choiceDirect" : "elem:null";
        if (res.attr() == null) {
            return elemTok + ".member:miss";
        }
        return adaptNonAttrBareItemNav(ref, symbol, NodeId.ROOT, ws).isPresent()
                ? elemTok + ".member:hit.claims"
                : elemTok + ".member:hit.armDecline";
    }

    /**
     * Whether {@code start}'s NEAREST enclosing inline-function lambda is the BODY of a {@link RFilterExpr} /
     * {@link RExtractExpr}. Mirrors legacy {@code NavigationHandler.implicitItemArgument}'s stop-at-the-first-
     * lambda contract: an {@code item} is bound by its nearest enclosing lambda, so the walk must NOT skip past
     * an inner non-filter/extract lambda to an outer filter/extract (which would mis-attribute a
     * sort/reduce/then-bound {@code item}). Returns false (decline) the moment the nearest lambda is not a
     * filter/extract body, or when no enclosing lambda is found.
     */
    private static boolean isFilterOrExtractBound(RNode start) {
        RNode cur = start == null ? null : start.parent();
        int depth = 0;
        while (cur != null && depth++ < PARENT_WALK_LIMIT) {
            if (cur instanceof RInlineFunction inline) {
                RNode p = inline.parent();
                return (p instanceof RFilterExpr filter && filter.body() == inline)
                        || (p instanceof RExtractExpr extract && extract.body() == inline);
            }
            cur = cur.parent();
        }
        return false;
    }

    /**
     * The SOURCE (argument) expression of the nearest enclosing {@link RFilterExpr} / {@link RExtractExpr} whose body
     * binds {@code start} — or {@code null} when {@code start} is not filter/extract-bound. The companion walk to
     * {@link #isFilterOrExtractBound} (same nearest-lambda contract), used to inspect what the {@code item} ranges over.
     */
    private static RExpression filterExtractBindingSource(RNode start) {
        RNode cur = start == null ? null : start.parent();
        int depth = 0;
        while (cur != null && depth++ < PARENT_WALK_LIMIT) {
            if (cur instanceof RInlineFunction inline) {
                RNode p = inline.parent();
                if (p instanceof RFilterExpr filter && filter.body() == inline) {
                    return filter.argument();
                }
                if (p instanceof RExtractExpr extract && extract.body() == inline) {
                    return extract.argument();
                }
                return null;
            }
            cur = cur.parent();
        }
        return null;
    }

    /**
     * #479 — the SYNTHETIC filter/extract item as a navigation base, RETYPED at the adapter
     * boundary. A synthetic elided-operand implicit ({@code extract a -> b}'s chain root, a
     * predicate's elided operand, …) lowers to a {@link IRVariable.VariableKind#SYNTHETIC_ITEM}
     * whose {@code type} ALWAYS reads MISSING: the expression-type cache is NODE-keyed and the
     * engine's implicit-item inference never types the parser-synthesized node (the #478
     * cache-boundary class — the #479 witness read {@code typeMissing} on every synthetic bucket
     * at population). The emitter needs the receiver's element type for the hop's
     * {@code <Witness>}-lambda naming, so the admissible slice rebuilds the lowered variable with
     * the binder SOURCE's element type — the engine's own Category-8 law ({@code item} ranges over
     * the source's elements) applied at the boundary, from the source expression's OWN cached type.
     * Admitted when, and ONLY when (the #479 witness's context split + the first ring's
     * three-class decode, in the method's own order):
     * <ul>
     *   <li>the lowered receiver is a {@code SYNTHETIC_ITEM} from a synthetic
     *       {@link RImplicitVariable} (defensive — the only lowering source);</li>
     *   <li>the item's NEAREST binding lambda — walked with SWITCH precedence
     *       ({@link #nearestInlineFunctionBeforeSwitch}: a switch between the item and the
     *       lambda routes legacy through the #221 case-narrowed subject machinery, decline) —
     *       is an IMPLICIT-or-paramless {@link RFilterExpr}/{@link RExtractExpr} body (the
     *       L-029-proven render family: legacy resolves the binding to the lambda's own
     *       Mapper-typed parameter, and the emitter's oracle renderer reuses that resolution
     *       verbatim; a NAMED extract carries the #357/#364/#367/#375 scope-LIVE walk-out
     *       re-roots — decline) — or, since #502 (5d), an IMPLICIT-or-paramless
     *       {@link RThenExpr} BODY: the then-item takes the WHOLE pipe-argument value (the
     *       #481 value identity), so the leg stamps the ARGUMENT's own cached type,
     *       SINGLE-only (value form ≡ element form on a single argument — the same allowlist
     *       + non-meta proofs apply verbatim; the MULTI residue declines); sort/other binders
     *       never pass either test;</li>
     *   <li>the source's ELEMENT FORM is PROVABLY plain
     *       ({@link #isProvablyNonMetaElementSource} — the allowlist; subsumes the direct
     *       meta-sourced class and declines every unprovable shape, the L-029 split; since #481 a
     *       grammar-elided PIPED source resolves to the enclosing then's argument first —
     *       {@link #resolveElidedPipedSource}, the value identity);</li>
     *   <li>the source's element type resolves non-meta (the retype's channel, read on the
     *       RESOLVED source — the elided node itself is cache-invisible).</li>
     * </ul>
     * The rebuild keeps the node identity facts (name/kind/id/range), stamps the element type and
     * SINGLE cardinality (an {@code item} is one element by the Cat-8 law — the per-hop map/mapC
     * choice stays feature-keyed, and the accumulated overlay reads this receiver as scalar,
     * matching legacy's chain-off-item semantics). Returns {@code null} outside the slice — the
     * caller defers, byte-identically.
     */
    private IRExpr retypedSyntheticFilterExtractItem(IRExpr lowered, RExpression rawReceiver, RWorkspace ws) {
        if (!(lowered instanceof IRVariable var)
                || var.variableKind() != IRVariable.VariableKind.SYNTHETIC_ITEM) {
            return null;
        }
        if (!(rawReceiver instanceof RImplicitVariable iv) || !iv.isSynthetic()) {
            return null;
        }
        // The binder gates (the first #479 ring's three mismatch classes, each excluded at its
        // own predicate): the nearest inline fn must be an IMPLICIT-or-paramless filter/extract
        // body — a NAMED extract binds its param and legacy's #357/#364/#367/#375 walk-out arms
        // can re-root the implicit through scope-live bindings with their own deferred-escape
        // timing (the DTCC λ-escape class); a SWITCH between the item and the lambda routes the
        // render through the #221 case-narrowed subject machinery (a bare cast var + the
        // narrowed-type λ derivation — the ExtractNotionalAdjustmentByLeg class). Both stay
        // legacy's; the witness's context split names them.
        RInlineFunction binder = nearestInlineFunctionBeforeSwitch(iv);
        if (binder == null || !(binder.isImplicit() || binder.paramNames().isEmpty())) {
            return null;
        }
        RNode binderParent = binder.parent();
        if (binderParent instanceof RThenExpr then && then.body().orElse(null) == binder) {
            // #502 (5d): the THEN-body binder leg — the census's 265-event thenBody context. A
            // then-body implicit takes the WHOLE pipe-argument value (the #481 value identity —
            // legacy handle(RImplicitVariable)'s thenArg render), so the retype stamps the
            // ARGUMENT's OWN cached type. Census-narrow SINGLE-only (the card1-dominant rows):
            // a single argument's value form ≡ its element form, so the same two proofs apply —
            // the allowlist on the argument (the FieldWithMeta-value protection: the census's
            // thenBody.metaFeature rows read aw0 and must keep declining) and the non-meta
            // cached type; the cardN residue (15 census events) declines with the MULTI stamp
            // question unanswered (a future leg's own decode). The card gate reads the
            // CARDINALITY COMPUTER's channel — the same channel the census's card leg read: a
            // disguised REnumValueRef chain argument reads the computer's conservative SINGLE
            // (a bare/FeatureCall MULTI reads MULTI), so the gate admits exactly what the
            // census called card1, and the corpus rings byte-prove the admitted set (55/55 at
            // every probe with the leg live).
            RExpression thenArg = then.argument();
            if (thenArg == null || ws == null
                    || card(thenArg, ws) != ExpressionCardinality.SINGLE
                    || !isProvablyNonMetaElementSource(thenArg)) {
                return null;
            }
            RMetaAnnotatedType argType = type(thenArg, ws);
            if (argType == null || argType.isMissing() || argType.hasMeta()) {
                return null;
            }
            return new IRVariable(var.name(), var.variableKind(), var.nodeId(), argType,
                    ExpressionCardinality.SINGLE, var.optionality(), var.sourceRange());
        }
        boolean filterExtractBody = (binderParent instanceof RFilterExpr filter && filter.body() == binder)
                || (binderParent instanceof RExtractExpr extract && extract.body() == binder);
        if (!filterExtractBody) {
            return null;
        }
        RExpression source = resolveElidedPipedSource(filterExtractBindingSource(iv));
        if (source == null || !isProvablyNonMetaElementSource(source)) {
            // The element-form allowlist (the GetIsin class): the item's Java VALUE is a
            // FieldWithMeta wrapper whenever the source's ELEMENTS come from a meta-annotated
            // feature — through ANY source shape (a direct nav, a LIST LITERAL of navs, a
            // disguised chain, an alias …) — and legacy inserts the gm-aware "Type coercion"
            // deref the neutral IR cannot reproduce (the L-029 split). Only sources whose
            // element form is PROVABLY plain are admitted; everything unprovable declines.
            // #481: a grammar-elided PIPED source resolves to the enclosing then's argument
            // FIRST (the value identity), so both this proof and the type read below see the
            // pipe input — the synthetic elided node itself is cache-invisible.
            return null;
        }
        RMetaAnnotatedType elementType;
        if (source instanceof RImplicitVariable topIv) {
            // #485: the rule-input face — the source IS a still-unresolved implicit (provable
            // above via the allowlist's implicit arms; the synthetic node itself is
            // cache-invisible like every parser-synthesized implicit, so the cached-type
            // channel below can never serve it). #502: the type read goes through the SHARED
            // structural walk (sourceElementDataType's own RImplicitVariable case), which
            // subsumes the #485 rule-top declaration read AND adds the #501 nested-binder
            // recursion — the census's aw1tw1-yet-declining nonThenBinder rows were exactly
            // this branch reading provableRuleInputElementType alone (NULL off rule-top) while
            // both proofs already passed; the walk derives the element type the same way the
            // allowlist just proved it, wrapped exactly as the checker wraps these reads
            // (withNoMeta).
            RDataType structural = sourceElementDataType(topIv, 0);
            elementType = structural == null
                    ? null : RMetaAnnotatedType.withNoMeta(new RDataTypeRef(structural));
        } else {
            elementType = type(source, ws);
        }
        if (elementType == null || elementType.isMissing() || elementType.hasMeta()) {
            return null;
        }
        return new IRVariable(var.name(), var.variableKind(), var.nodeId(), elementType,
                ExpressionCardinality.SINGLE, var.optionality(), var.sourceRange());
    }

    /**
     * The nearest enclosing {@link RInlineFunction}, or {@code null} if an {@link RSwitchExpr}
     * intervenes FIRST — the #221 switch-case boundary (a case-narrowed implicit resolves through
     * the scope-live subject binding, never the lambda's own item; mirrors legacy
     * {@code ReferenceHandler.nearestEnclosingSwitchSubject}'s precedence).
     */
    private static RInlineFunction nearestInlineFunctionBeforeSwitch(RNode start) {
        RNode cur = start == null ? null : start.parent();
        int depth = 0;
        while (cur != null && depth++ < PARENT_WALK_LIMIT) {
            if (cur instanceof RSwitchExpr) {
                return null;
            }
            if (cur instanceof RInlineFunction inline) {
                return inline;
            }
            cur = cur.parent();
        }
        return null;
    }

    /**
     * Whether a filter/extract SOURCE expression's element form is PROVABLY plain (non-meta) — the
     * #479 admission allowlist. The item's Java value is the source's ELEMENT form, decided by the
     * LAST hop / defining feature of each contributing shape:
     * <ul>
     *   <li>a feature call — the resolved LAST-hop feature, non-meta (a mid-chain meta hop is
     *       derefed inside the source's own render, which stays outside this claim);</li>
     *   <li>a bare argument-less symbol reference — a resolved non-meta {@link RAttribute}
     *       (an alias/shortcut symbol is NOT provable: its body's element form is its own);</li>
     *   <li>a disguised {@link REnumValueRef} — the bound chain/input-feature LEAF, non-meta;</li>
     *   <li>a list literal — every element provable;</li>
     *   <li>a filter — element-form-preserving, recurse into its argument;</li>
     *   <li>the #481 widening: a grammar-elided PIPED implicit — resolve to the enclosing then's
     *       ARGUMENT ({@link #resolveElidedPipedSource}, the value identity through legacy's
     *       thenBody face) and recurse; an unresolvable face (switch / named / non-then binder,
     *       top-level) stays unprovable;</li>
     *   <li>the #482 widening: a NESTED {@link RThenExpr} pipe — the element form of
     *       {@code A then B} is B's BODY result (the pipe's value IS the body's value on the
     *       piped input — the #481 value identity one structural seat deeper; legacy renders the
     *       then as its body's own chain, so the result's element form is the body
     *       expression's), recurse into the body. Termination rides the #481 argument: an
     *       elided implicit only ever resolves to an ANCESTOR then's argument subtree, which
     *       wholly precedes the implicit in post-order, and a then-body descent stays inside
     *       that subtree — the measure strictly decreases through every combination of the two
     *       arms;</li>
     *   <li>the #483 widening: an {@link RExtractExpr} with an IMPLICIT-or-paramless body — the
     *       element form of {@code A extract B} is B's OWN result per element (legacy
     *       {@code CollectionHandler.handle(RExtractExpr)}: EVERY receiver/body-cardinality
     *       map route — the single/item routes {@code mapSingleToItem}/{@code mapItem}/
     *       {@code mapSingleToList}/{@code mapItemToList} and the LoL routes
     *       {@code mapListToItem}/{@code mapListToList} — produces result elements that ARE
     *       the body's values, the MULTI/LoL-body routes flattening to the body's own
     *       elements/lists; the switch-block lambda routes are unreachable under this arm
     *       (an {@link RSwitchExpr} body never proves — prove-or-decline); the checker types
     *       the extract off its BODY — {@code ExpressionTypeComputer}'s
     *       {@code RExtractExpr} case), recurse
     *       into the body. A NAMED extract declines whole (the #357/#364/#367/#375 scope-live
     *       walk-out class); the identity {@code extract item} face declines through the
     *       resolver arm. Termination: the body descent stays inside the extract's own subtree
     *       ({@code children()} order [argument, body]) — the #482 post-order measure strictly
     *       decreases through every combination of the three arms.</li>
     *   <li>the #484 widening: a bare (no-arg) {@link RSymbolReference} resolving to an
     *       {@link RFunction} — the L-109 point-free application: the applied value IS the
     *       callee's OUTPUT (legacy {@code ReferenceHandler.renderImplicitFunctionInvocation}
     *       renders {@code <callee>.evaluate(<binding>.get())} and the invocation carries the
     *       function's output type; the checker types the bare reference off
     *       {@code fn.output()}), so the element form is the OUTPUT attribute's own — the
     *       CALLABLE-OUTPUT identity. The aliasShadow precedence declines first (legacy's
     *       {@code isAliasReference}: a shortcut-colliding name renders as an ALIAS
     *       invocation); the output must exist and be non-meta. No recursion — the #482
     *       termination measure is untouched.</li>
     *   <li>the #485 widening: an UNRESOLVED elided implicit whose binder walk stops
     *       {@code noBinder} at RULE top level — the RULE-INPUT identity: the implicit's
     *       value IS the rule input (legacy's {@code isElidedOperandTopLevel} route renders
     *       {@code MapperS.of(input)}; the checker types it off the rule's from-type,
     *       {@code withNoMeta} by construction), so the element form is the rule's declared
     *       from-type ({@link #provableRuleInputElementType} — the declaration read; a
     *       lambda/switch/function/condition crossing declines, prove-or-decline). No
     *       recursion — the termination measure is untouched.</li>
     *   <li>the #487 widening: an {@link RConditionalExpr} — the conditional JOIN law: the
     *       element form of {@code if c then A else B} is the arms' COMMON form (the runtime
     *       elements are the EXECUTED arm's — legacy {@code ControlFlowHandler}'s every route
     *       yields one arm's value — and the checker types the node
     *       {@code withNoMeta(typeJoin.join(thenT, elseT))}, the wrap STRIPPING arm meta from
     *       the cache channel, so the per-arm proofs are the ONLY meta protection): the THEN
     *       arm must prove; an EMPTY else ({@code hasGenuineElse}'s structural-emptiness
     *       discriminator — the {@code DefaultElseRule} synthetic empty list and a user-written
     *       {@code else empty} read identically) contributes no elements and admits on the
     *       then proof alone; a genuine else must prove too — the list-literal all-provable
     *       law lifted to the two-armed choice. The arms are strict subtrees — the #482
     *       termination measure decreases.</li>
     * </ul>
     * Every other shape (only-element collapses, calls, aliases,
     * named extracts, unresolved …) is NOT
     * provable and the caller declines — the honest-default direction: an unprovable source keeps
     * legacy's render, and the #479 witness's {@code unprovableSource} bucket (+ the #481 pipe
     * facet's residue map, since #482/#483 with the {@code thenPipe.*}/{@code extractPipe.*} body
     * sub-decodes, since #484 with the {@code symbol:RFunction.<facet>} callee-output refinement,
     * since #487 with the {@code conditional.<facet>} JOIN-law refinement) sizes the residue.
     *
     * <p>Public (not private) for the #502 census's walk-unification cross-read (the probe-only
     * {@code sourceWalkVerdicts} consumer in the compiler — the #501 probe-visibility
     * precedent); no standing-render caller outside this class.
     */
    public static boolean isProvablyNonMetaElementSource(RExpression source) {
        return isProvablyNonMetaElementSource(source, 0);
    }

    /**
     * The bounded internal form (#502): the alias leg descends into a shortcut BODY — not a
     * subtree of the reference — so the #482 structural termination measure does not cover it;
     * the depth bound (8, matching {@link #sourceElementDataType}'s) is the termination guard
     * on every leg alike (the structural legs strictly decrease anyway and never approach it).
     */
    private static boolean isProvablyNonMetaElementSource(RExpression source, int depth) {
        if (depth >= SOURCE_WALK_DEPTH_LIMIT) {
            return false;
        }
        if (source instanceof RImplicitVariable) {
            // #481: the grammar-elided PIPED implicit takes the enclosing then's ARGUMENT's
            // value (legacy handle(RImplicitVariable)'s thenBody face — both render channels
            // serve the pipe input), so its element form recurses to that argument.
            RExpression resolved = resolveElidedPipedSource(source);
            if (resolved != null && !(resolved instanceof RImplicitVariable)) {
                return isProvablyNonMetaElementSource(resolved, depth + 1);
            }
            if (resolved instanceof RImplicitVariable iv) {
                // #502 (5a — the walk-unification leg, the census's aw0tw1 rows: the typing
                // walk's #501 nested-binder recursion had no allowlist mirror): an implicit
                // bound by an ENCLOSING implicit/paramless filter/extract binder IS that
                // binder's element (the checker's computeImplicitItemType rule — a filter
                // preserves its source's elements, an extract-BODIED implicit is the extract
                // source's element), so the meta proof recurses on the binder's OWN source —
                // the value identity one binder out, exactly the sourceElementDataType leg.
                RInlineFunction outerBinder = nearestInlineFunctionBeforeSwitch(iv);
                if (outerBinder != null
                        && (outerBinder.isImplicit() || outerBinder.paramNames().isEmpty())) {
                    RNode binderParent = outerBinder.parent();
                    if (binderParent instanceof RFilterExpr filter
                            && filter.body() == outerBinder) {
                        return isProvablyNonMetaElementSource(filter.argument(), depth + 1);
                    }
                    if (binderParent instanceof RExtractExpr extract
                            && extract.body() == outerBinder) {
                        return isProvablyNonMetaElementSource(extract.argument(), depth + 1);
                    }
                }
                // #485: the rule-input widening — an UNRESOLVED implicit whose walk stops
                // noBinder at RULE top level takes the RULE INPUT's value (legacy's
                // isElidedOperandTopLevel → MapperS.of(input); the checker types it off the
                // rule's from-type, withNoMeta by construction — the fromRule-synthesized
                // input carries no annotations, so the declaration read IS the non-meta
                // proof).
                return provableRuleInputElementType(iv) != null;
            }
            return false;
        }
        if (source instanceof RThenExpr then) {
            // #482: the nested-pipe widening — the element form of `A then B` is B's BODY
            // result (the value identity one seat deeper), so the proof recurses into the
            // body expression; a bodyless then stays unprovable (prove-or-decline).
            RExpression body = then.body().map(RInlineFunction::body).orElse(null);
            return body != null && isProvablyNonMetaElementSource(body, depth + 1);
        }
        if (source instanceof RExtractExpr extract) {
            // #483: the extract-bodied widening — the element form of `A extract B` is B's
            // OWN result per element (every legacy map route produces result elements
            // that ARE the body's values, the MULTI/LoL-body routes flattening to the
            // body's own; the checker types the extract off its BODY), so the proof recurses
            // into the body expression. An IMPLICIT-or-paramless body lambda ONLY — a NAMED
            // extract carries the #357/#364/#367/#375 scope-live walk-out class (the #389
            // self-shadow face included: references inside the body can resolve through
            // scope-live bindings the structural walk cannot see) — decline whole, the safe
            // direction. The identity face (the body IS the written `item`) declines through
            // the recursion's resolver arm (a non-synthetic implicit never resolves).
            RInlineFunction bodyFn = extract.body();
            if (bodyFn == null || !(bodyFn.isImplicit() || bodyFn.paramNames().isEmpty())) {
                return false;
            }
            RExpression body = bodyFn.body();
            return body != null && isProvablyNonMetaElementSource(body, depth + 1);
        }
        if (source instanceof RConditionalExpr cond) {
            // #487: the conditional JOIN widening — the element form of `if c then A else B`
            // is the arms' COMMON form: the runtime elements are the EXECUTED arm's (legacy
            // ControlFlowHandler.handle(RConditionalExpr) — every route, the hoisted locals
            // and the inline ternary alike, yields ONE arm's value), so each arm's own proof
            // is the ONLY meta protection — the checker types the node
            // withNoMeta(typeJoin.join(thenT, elseT)) (ExpressionTypeComputer
            // .computeConditional), and the withNoMeta wrap STRIPS an arm's meta from the
            // cache channel (the L-029 law demands the per-arm allowlist proofs). The shape
            // admission is the list-literal all-provable law lifted to the two-armed choice:
            // the THEN arm must prove; an EMPTY else (absent, `empty`, or the DefaultElseRule
            // synthetic empty list — hasGenuineElse's structural-emptiness discriminator)
            // contributes NO elements and admits on the then proof alone (the checker's own
            // join(thenT, NOTHING) = thenT bottom rule keeps the cached type the then form);
            // a genuine else must prove too. Recursion: the arms are strict subtrees — the
            // #482 post-order termination measure decreases (nested else-if cascades prove
            // arm-by-arm). The #487 decode read the claimable slice at 201 of 507 pipe
            // events (sameType 135 + elseEmpty 52 + formGap 14) with typeDiffers ZERO at
            // population — no mixed-form conditional exists in the corpus.
            RExpression thenB = cond.thenBranch();
            if (thenB == null || !isProvablyNonMetaElementSource(thenB, depth + 1)) {
                // #506: the EMPTY-THEN mirror (the typing walk's rule in lockstep) — a
                // structurally empty then contributes no elements; the genuine else's own
                // proof is the admission.
                if (isStructurallyEmptyBranch(thenB) && hasGenuineElse(cond)) {
                    RExpression emptyThenElse = cond.elseBranch().orElse(null);
                    return emptyThenElse != null
                            && isProvablyNonMetaElementSource(emptyThenElse, depth + 1);
                }
                return false;
            }
            if (!hasGenuineElse(cond)) {
                return true;
            }
            RExpression elseB = cond.elseBranch().orElse(null);
            return elseB != null && isProvablyNonMetaElementSource(elseB, depth + 1);
        }
        if (source instanceof RFeatureCall fc) {
            return fc.resolvedFeature().isPresent() && !isMetaAnnotated(fc.resolvedFeature().get());
        }
        if (source instanceof RSymbolReference sym) {
            if (!sym.args().isEmpty()) {
                // #501: the ARGS-PRESENT call leg — the same callable-output law as the typing
                // walk's (the element IS the callee's output regardless of the argument list),
                // so the meta proof reads the output attribute; the alias-shadow precedence
                // cannot apply (an alias invocation carries no argument list). A non-function
                // callee declines, prove-or-decline.
                RNode callSymbol = sym.symbol().orElse(null);
                if (callSymbol instanceof RFunction callFn) {
                    RAttribute callOut = callFn.output().orElse(null);
                    return callOut != null && !isMetaAnnotated(callOut);
                }
                return false;
            }
            RNode symbol = sym.symbol().orElse(null);
            if (symbol instanceof RAttribute attr) {
                return !isMetaAnnotated(attr);
            }
            if (symbol instanceof RFunction calleeFn) {
                // #484: the callable-output widening — a bare (no-arg) function reference in
                // element-source position is the L-109 point-free application: legacy renders
                // it `<callee>.evaluate(<binding>.get())` (ReferenceHandler.
                // renderImplicitFunctionInvocation — the applied value IS the callee's OUTPUT,
                // and per its contract the invocation carries the function's output type), and
                // the checker types the bare reference off fn.output() (inferTypeOfNode's
                // RFunction leg) — so the element form is the OUTPUT attribute's own: prove it
                // non-meta, cardinality-agnostic like the bare-attribute leg (a MULTI output's
                // applied values are the output's own elements — the #483 route-grid flatten
                // faces). The aliasShadow precedence declines FIRST: legacy's isAliasReference
                // renders a shortcut-colliding name as an ALIAS invocation, not the function
                // (the #480 MF-1 law — mirror legacy's PRECEDENCE; collidesWithShortcut is the
                // coarse form without the #453 self-shortcut exemption, declining a SUPERSET —
                // the safe direction). No recursion: the output attribute is a declaration
                // read, so the #482 termination measure is untouched.
                RFunction enclosing = enclosingFunction(sym);
                if (enclosing != null && collidesWithShortcut(enclosing, sym.name())) {
                    return false;
                }
                RAttribute out = calleeFn.output().orElse(null);
                return out != null && !isMetaAnnotated(out);
            }
            if (symbol instanceof RShortcut shortcut) {
                // #502 (5c): the ALIAS leg — a linker-BOUND shortcut in element-source
                // position: the alias's value IS its body's (legacy renders aliasName(inputs),
                // the helper returning the body's Mapper — the value identity through the
                // helper), so the meta proof recurses on the shortcut's OWN body. The depth
                // bound is the termination guard (the body is NOT a subtree of the reference —
                // the #482 structural measure does not cover this leg; alias-to-alias chains
                // bottom at the bound), mirroring the typing walk's leg exactly.
                return shortcut.expression() != null
                        && isProvablyNonMetaElementSource(shortcut.expression(), depth + 1);
            }
            if (RInlineFunction.declaringLambdaOf(symbol, sym.name()).isPresent()) {
                // #506: the cpSym leg (the typing walk's rule in lockstep) — a named closure
                // param's value is its binder source's element, so the meta proof recurses on
                // that source; the depth bound guards the non-structural descent.
                RExpression cpSource = closureParamBinderSource(sym, sym.name());
                return cpSource != null && isProvablyNonMetaElementSource(cpSource, depth + 1);
            }
            return false;
        }
        if (source instanceof REnumValueRef evr) {
            if (evr.resolvedAttributeChain().isPresent()) {
                RAttribute leaf = evr.resolvedAttributeChain().get().feature();
                return leaf != null && !isMetaAnnotated(leaf);
            }
            return evr.resolvedInputFeature().isPresent()
                    && !isMetaAnnotated(evr.resolvedInputFeature().get());
        }
        if (source instanceof RListLiteral list) {
            if (list.elements().isEmpty()) {
                return false;
            }
            for (RExpression element : list.elements()) {
                if (!isProvablyNonMetaElementSource(element, depth + 1)) {
                    return false;
                }
            }
            return true;
        }
        if (source instanceof RFilterExpr filter) {
            return filter.argument() != null
                    && isProvablyNonMetaElementSource(filter.argument(), depth + 1);
        }
        if (source instanceof RListOpExpr listOp) {
            // #502 (5b): the element-form-PRESERVING collapse legs — only-element/first pick
            // ONE element of the argument and flatten concatenates inner lists of the same
            // declared terminal, so the meta proof recurses on the argument (the element form
            // is the argument's own; the listness lives on the cardinality axis). Census-narrow:
            // the three #502 census-live ops; #506 adds DISTINCT and LAST in lockstep with the
            // typing walk (dedup keeps the argument's own elements; last picks one — the
            // FIRST law at the other end); the value-collapsing and remaining order ops decline.
            ListOp op = listOp.op();
            if (op == ListOp.ONLY_ELEMENT || op == ListOp.FIRST || op == ListOp.FLATTEN
                    || op == ListOp.DISTINCT || op == ListOp.LAST) {
                return listOp.argument() != null
                        && isProvablyNonMetaElementSource(listOp.argument(), depth + 1);
            }
            return false;
        }
        return false;
    }

    /**
     * Whether a lowered receiver is a function-PARAMETER navigation base — an
     * {@link IRVariable.VariableKind#PARAM} of <em>either</em> cardinality. A SINGLE parameter renders
     * the {@code MapperS.of(name)} leaf, a MULTI parameter the witnessed {@code MapperC.<Item>of(name)}
     * leaf (both byte-proven), and a navigation hop composes identically off each: the step's lambda
     * variable derives from the parameter's <em>element</em> type name — the same source as legacy
     * {@code NavigationHandler.resolveSymbolTypeName}, which reads the declared {@code typeCall().typeName()}
     * regardless of cardinality — and {@code map}/{@code mapC} is keyed per hop on the feature's OWN
     * cardinality. So {@code legs -> rate} off a multi parameter {@code legs} composes byte-identically to
     * the chain-through-multi case ({@code trade -> legs -> rate}); the only difference is the receiver leaf
     * ({@code MapperC.<Leg>of(legs)} vs the {@code trade -> legs} hop), each itself byte-proven. A MULTI
     * parameter whose own {@code MapperC} witness simple name collides with the function output renders
     * FQN-inline — a Java-name decision the emitter makes against the output type (L-029), transparent to
     * this neutral receiver gate.
     *
     * <p>Broader than {@link #isScalarParam} (which a navigation OPERAND still requires): a multi parameter
     * is a valid receiver but not a scalar operand.
     */
    private static boolean isParameterReceiver(IRExpr expr) {
        return expr instanceof IRVariable var
                && var.variableKind() == IRVariable.VariableKind.PARAM;
    }

    /**
     * The absorbing-monoid optionality of a navigation hop (design §6): OPTIONAL if EITHER the resolved
     * feature is optional ({@code (0..n)}) OR the receiver chain is already optional — any absent hop
     * makes the whole chain absent ("any input absent ⇒ result absent"). For a single hop off a present
     * scalar-parameter receiver this is just the feature's own optionality; for a chain (and for a hop
     * off an optional parameter) it propagates an earlier OPTIONAL through every later hop — which the
     * feature's own optionality alone would miss.
     */
    private static Optionality chainOptionality(IRExpr receiver, RAttribute feature) {
        boolean optional = receiver.optionality() == Optionality.OPTIONAL
                || optionalityOf(feature) == Optionality.OPTIONAL;
        return optional ? Optionality.OPTIONAL : Optionality.PRESENT;
    }

    /** Maps the AST comparison operator to the neutral {@link BinaryOp.BinOp} (keeps the IR free of the rune-AST enum). */
    private static BinaryOp.BinOp toBinOp(CompOp op) {
        return switch (op) {
            case LT -> BinaryOp.BinOp.LT;
            case GT -> BinaryOp.BinOp.GT;
            case LTE -> BinaryOp.BinOp.LTE;
            case GTE -> BinaryOp.BinOp.GTE;
        };
    }

    /** Maps the AST equality operator to the neutral {@link BinaryOp.BinOp} (keeps the IR free of the rune-AST enum). */
    private static BinaryOp.BinOp toBinOp(EqOp op) {
        return switch (op) {
            case EQ -> BinaryOp.BinOp.EQ;
            case NEQ -> BinaryOp.BinOp.NEQ;
        };
    }

    /** Maps the AST arithmetic operator to the neutral {@link BinaryOp.BinOp} (keeps the IR free of the rune-AST enum). */
    private static BinaryOp.BinOp toBinOp(ArithOp op) {
        return switch (op) {
            case PLUS -> BinaryOp.BinOp.ADD;
            case MINUS -> BinaryOp.BinOp.SUB;
            case MULTIPLY -> BinaryOp.BinOp.MUL;
            case DIVIDE -> BinaryOp.BinOp.DIV;
        };
    }

    /** Maps the AST logical operator to the neutral {@link BinaryOp.BinOp} (keeps the IR free of the rune-AST enum). */
    private static BinaryOp.BinOp toBinOp(LogOp op) {
        return switch (op) {
            case AND -> BinaryOp.BinOp.AND;
            case OR -> BinaryOp.BinOp.OR;
        };
    }

    /** Maps the AST existence operator to the neutral {@link Existence.ExistOp} (keeps the IR free of the rune-AST enum). */
    private static Existence.ExistOp toExistOp(ExistenceOp op) {
        return switch (op) {
            case EXISTS -> Existence.ExistOp.EXISTS;
            case ABSENT -> Existence.ExistOp.ABSENT;
        };
    }

    /** Maps the optional AST exists modifier to the neutral {@link Existence.ExistMod} ({@code null} → {@code null}). */
    private static Existence.ExistMod toExistMod(ExistsModifier mod) {
        if (mod == null) {
            return null;
        }
        return switch (mod) {
            case SINGLE -> Existence.ExistMod.SINGLE;
            case MULTIPLE -> Existence.ExistMod.MULTIPLE;
        };
    }

    /** Membership by reference identity (the resolved symbol IS the declared parameter node). */
    private static boolean containsByIdentity(List<RAttribute> attributes, RAttribute target) {
        for (RAttribute attribute : attributes) {
            if (attribute == target) {
                return true;
            }
        }
        return false;
    }

    /** The nearest enclosing {@link RFunction} via the parent chain, or {@code null} (e.g. a rule/data-type seam). */
    private static RFunction enclosingFunction(RNode node) {
        for (RNode n = node.parent(); n != null; n = n.parent()) {
            if (n instanceof RFunction func) {
                return func;
            }
        }
        return null;
    }

    /** Whether {@code name} matches an enclosing shortcut — {@code isAliasReference}'s fallback. */
    private static boolean collidesWithShortcut(RFunction func, String name) {
        if (name == null) {
            return false;
        }
        for (RShortcut shortcut : func.shortcuts()) {
            if (name.equals(shortcut.name())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether the attribute carries a {@code [metadata …]} annotation (mirrors
     * {@code mapperCOfWrapWitness}). Public since #528 as the opResGate census's own BY-CALL
     * vocabulary (the {@code calleeMetaParam} face's declining predicate — the census re-runs
     * the mirror's own per-input scan to name WHICH input carries the annotation); read-only.
     */
    public static boolean isMetaAnnotated(RAttribute attr) {
        for (RAnnotationRef annotationRef : attr.annotationRefs()) {
            if ("metadata".equals(annotationRef.annotationName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * The sorted {@code metadata} annotation qualifier names for the #499 {@link IRMetaAccess}
     * payload ({@code scheme}/{@code reference}/{@code address}/{@code location}/{@code id};
     * {@code bare} for a qualifier-less {@code [metadata]} — never empty for a
     * {@link #isMetaAnnotated} feature). The render-class discriminant a functional target reads;
     * the Java emitter never consults it (the oracle render reads the raw AST). Public since #528
     * as the opResGate census's own BY-CALL vocabulary (the {@code calleeMetaParam} face's
     * per-input qualifier read — the qualifier class is what legacy's own meta-arg machinery
     * keys on); read-only.
     */
    public static List<String> metaQualifierNames(RAttribute attr) {
        java.util.TreeSet<String> quals = new java.util.TreeSet<>();
        for (RAnnotationRef annotationRef : attr.annotationRefs()) {
            if ("metadata".equals(annotationRef.annotationName())) {
                quals.add(annotationRef.qualifierName().orElse("bare"));
            }
        }
        return List.copyOf(quals);
    }

    /**
     * Declared multi-cardinality — unbounded ({@code *}) or an upper bound &gt; 1. An absent
     * cardinality is the implicit {@code (1..1)} (V1) ⇒ single. This is the same declared-card
     * signal {@code gm.isMulti(attr)} reads for a bare parameter.
     */
    private static boolean isMultiCardinality(RAttribute attr) {
        return attr.cardinality()
                .map(c -> c.isUnbounded() || (c.sup() != null && c.sup().compareTo(BigInteger.ONE) > 0))
                .orElse(false);
    }

    /** {@code (0..n)} ⇒ OPTIONAL; {@code (1..n)} or absent ⇒ PRESENT (design §6 / V1). */
    private static Optionality optionalityOf(RAttribute attr) {
        boolean optional = attr.cardinality()
                .map(c -> c.inf() != null && c.inf().signum() == 0)
                .orElse(false);
        return optional ? Optionality.OPTIONAL : Optionality.PRESENT;
    }

    // ---------------------------------------------------------------------------------
    // The decline-reason channel (#477 — the per-ARM refinement decode)
    // ---------------------------------------------------------------------------------

    /**
     * The mirror sentinel: a declining node for which the owning arm's {@code reasonFor*} mirror
     * finds NO failing gate. Expected never — a nonzero {@code unattributed} count in the probe's
     * ranked reason lines is a mirror-currency defect (an arm edit whose mirror twin was missed),
     * self-signalling rather than silently misclassified. PUBLIC on purpose: the probe side
     * ({@code IRExpressionCompiler}) shares this exact token for its {@code Family:reason} pair
     * fallback and its unattributed meter, so the two modules cannot drift if the token is ever
     * renamed (single-source-of-truth for the sentinel).
     */
    public static final String UNATTRIBUTED = "unattributed";

    /**
     * Names the FIRST declining gate for an expression this adapter does NOT lower — the per-ARM
     * refinement decode channel (#477). The #476 blocker probe's minimal-blocker attribution names
     * WHICH family blocks an {@code adapterGap} decline; this names WHY that family's own arm
     * declined the node, so an arm-refinement teach is sized by its exact gate before it is built
     * (the decode-first law).
     *
     * <p><strong>{@code empty ⟺ lowers}, by construction.</strong> The method calls the real
     * {@link #adapt(RExpression, RWorkspace)} first and returns {@link Optional#empty()} exactly
     * when it lowers — the channel can never claim a reason for a node the adapter serves. For a
     * declining node it returns a stable reason TOKEN naming the first gate that fails, in the
     * owning arm's own exit order, computed with the arm's own predicate helpers
     * ({@link #isGenuineEnumValueRef}, {@link #isSimpleCallArg}, {@link #isNavigableReceiver},
     * {@link #isFilterOrExtractBound}, …) — the classification cannot use a different definition
     * of a gate than the arm itself does. A mirror that finds no failing gate returns
     * {@link #UNATTRIBUTED} (the honest residue); a family with no {@code adapt} arm at all
     * returns {@code "noAdaptArm"}.
     *
     * <p>Read-only and stateless exactly like {@code adapt} (the child probes re-run {@code adapt},
     * never a visitor). A census-run cost by design: the caller is the compiler's
     * {@code -Drosetta.generator.ir.blockerProbe} channel, which buckets each minimal blocker as a
     * {@code Family:reason} pair and ranks the pairs — the arm-refinement worklist the family
     * ranking could never resolve (#476 read 87.97% of the adapterGap mass into three REFERENCE
     * arms; this splits each arm's mass by its declining gate).
     *
     * @param expr the expression whose decline is to be attributed; must be non-null
     * @param ws   the resolved workspace providing the same memoized facts {@code adapt} reads
     * @return empty when {@code expr} lowers; else the first-failing-gate token
     */
    public Optional<String> declineReason(RExpression expr, RWorkspace ws) {
        if (adapt(expr, ws).isPresent()) {
            return Optional.empty();
        }
        return Optional.of(reasonFor(expr, ws));
    }

    /**
     * The dispatch mirror of {@link #adapt(RExpression, NodeId, RWorkspace)}: one {@code reasonFor*}
     * twin per arm, in the dispatch's own order. The always-lowering leaves (the four scalar
     * literals, {@code empty}, the implicit {@code item}, {@code super}) cannot decline, so
     * reaching their branches contradicts the {@code adapt} call that routed us here and yields
     * {@link #UNATTRIBUTED}; a list literal CAN decline — but only through its elements — so its
     * branch names {@code elementNotExpressible} when an element subtree does not lower; every
     * family outside the dispatch is {@code "noAdaptArm"} (the un-armed compound families —
     * reduce/cardinality-check; extract/filter joined the dispatch at #496,
     * conversion/then/only-exists at #500, switch/default/contains/disjoint/max/min/sort
     * at the #513 noAdaptArm sweep, and with-meta/join at the #515 untargeted close).
     */
    private String reasonFor(RExpression expr, RWorkspace ws) {
        if (expr instanceof RConstructorExpr ctor) {
            // #494: the ctor arm lowers every ROOT-position constructor carrying a type call, so a
            // probed ROOT decline can only be the missing-type-call face; a CHILD-position decline
            // (the root-only law) attributes at the PARENT's own gate tokens, never here.
            return ctor.typeCall() == null ? "ctorNoTypeCall" : UNATTRIBUTED;
        }
        if (expr instanceof RListLiteral list) {
            for (RExpression element : list.elements()) {
                // #494 (the Copilot R2 suppressed-note fix): mirror the ARM's child semantics, not
                // the re-rooted probe read — a constructor element lowers when re-adapted as a
                // probe ROOT but declines at the list arm's child NodeId (the root-only law), so
                // for attribution it IS the non-expressible element. Without this arm-mirror a
                // list-of-constructors would read UNATTRIBUTED (zero such claims in the #494
                // corpus receipts — the fix is receipt-inert today, correctness for the future).
                // #495 extended the class (a ctor-slot conditional element), and the first #495
                // ring read that leak live (RListLiteral:unattributed=2, drr FUNCTION). #496
                // (Seat-1 MF-1): the check routes through the SHARED slotNotExpressibleAtChild —
                // the single source of truth for the position-divergent classes, so every future
                // claim-root admission extends this seat automatically (an inline twin of the
                // predicate is exactly how the #496 ctor-body-lambda class was missed here).
                if (slotNotExpressibleAtChild(element, ws)) {
                    return "elementNotExpressible";
                }
            }
            return UNATTRIBUTED;
        }
        if (expr instanceof RIntLiteral || expr instanceof RNumberLiteral
                || expr instanceof RStringLiteral || expr instanceof RBooleanLiteral
                || expr instanceof REmptyLiteral || expr instanceof RImplicitVariable
                || expr instanceof RSuperCall) {
            return UNATTRIBUTED; // these leaves lower unconditionally — a decline is a contradiction
        }
        if (expr instanceof RSymbolReference ref) {
            return reasonForSymbolReference(ref, ws);
        }
        if (expr instanceof REnumValueRef enr) {
            return reasonForEnumValueRef(enr, ws);
        }
        if (expr instanceof RComparisonExpr cmp) {
            return reasonForComparison(cmp, ws);
        }
        if (expr instanceof REqualityExpr eq) {
            return reasonForEquality(eq, ws);
        }
        if (expr instanceof RLogicalExpr log) {
            return reasonForLogical(log, ws);
        }
        if (expr instanceof RExistenceExpr exist) {
            return reasonForExistence(exist, ws);
        }
        if (expr instanceof RFeatureCall fc) {
            return reasonForFeatureCall(fc, ws);
        }
        if (expr instanceof RArithmeticExpr arith) {
            return reasonForArithmetic(arith, ws);
        }
        if (expr instanceof RListOpExpr listOp) {
            return reasonForListOp(listOp, ws);
        }
        if (expr instanceof RCountExpr count) {
            // #496 (Seat-1 OBS-1): the receiver check routes through slotNotExpressibleAtChild —
            // the arm adapts the argument at a child NodeId, so a position-divergent receiver
            // (the claim-root admission classes) attributes here instead of leaking UNATTRIBUTED.
            return slotNotExpressibleAtChild(count.argument(), ws)
                    ? "receiverNotExpressible" : UNATTRIBUTED;
        }
        if (expr instanceof RToStringExpr toString) {
            // #496 (Seat-1 OBS-1): same child-semantics routing as the count mirror above.
            return slotNotExpressibleAtChild(toString.argument(), ws)
                    ? "receiverNotExpressible" : UNATTRIBUTED;
        }
        if (expr instanceof RConditionalExpr cond) {
            return reasonForConditional(cond, ws);
        }
        if (expr instanceof RExtractExpr extract) {
            return reasonForLambdaOp(extract.argument(), extract.body(), ws);
        }
        if (expr instanceof RFilterExpr filter) {
            return reasonForLambdaOp(filter.argument(), filter.body(), ws);
        }
        if (expr instanceof RConversionExpr conv) {
            // #500: the conversion arm's exits in order — the parse-robustness gate, then the
            // all-or-nothing argument (the child-semantics routing per the #496 Seat-1 MF-1 law).
            if (conv.argument() == null || conv.kind() == null) {
                return "conversionShape";
            }
            return slotNotExpressibleAtChild(conv.argument(), ws)
                    ? "argNotExpressible" : UNATTRIBUTED;
        }
        if (expr instanceof RThenExpr then) {
            // #500: the pipe arm's exits in order — the parse-robustness argument gate (zero
            // live, the Copilot #500-R1 class: the census's own arg:nullSlot shape must decline
            // at the arm too), then the census-narrow binder-form gate.
            if (then.argument() == null) {
                return "pipeShape";
            }
            RInlineFunction thenBody = then.body().orElse(null);
            return thenBody == null || !thenBody.isImplicit() ? "pipeBinderForm" : UNATTRIBUTED;
        }
        if (expr instanceof ROnlyExistsExpr onlyExists) {
            // #500: the only-exists arm's single gate (the shallow node has no child gates).
            return onlyExists.elements().isEmpty() ? "onlyExistsNoPaths" : UNATTRIBUTED;
        }
        if (expr instanceof RDeepFeatureCall deep) {
            // #507: the deep arm's twin — the arm's exits restated in order.
            return reasonForDeepFeatureCall(deep, ws);
        }
        if (expr instanceof RSwitchExpr sw) {
            // #513: the switch arm's twin — the shape gates restated in order (the arm
            // claims every case-carrying node incl. the without-left argument-null form,
            // so a reason here is parse-robustness only).
            if (sw.cases().isEmpty()) {
                return "switchShape";
            }
            for (RSwitchCase switchCase : sw.cases()) {
                if (switchCase.expression() == null) {
                    return "switchShape";
                }
            }
            return UNATTRIBUTED; // the arm claims — reachable from the mirror only on twin drift
        }
        if (expr instanceof RDefaultExpr def) {
            // #513: the default arm's twin (the without-left left-null form claims; the
            // fallback side stays the gate).
            return def.rawRight() == null ? "defaultShape" : UNATTRIBUTED;
        }
        if (expr instanceof RContainsExpr || expr instanceof RDisjointExpr) {
            // #513: the membership arm's twin (one gate, both operator families).
            RBinaryExpression test = (RBinaryExpression) expr;
            return test.rawLeft() == null || test.rawRight() == null
                    ? "membershipShape" : UNATTRIBUTED;
        }
        if (expr instanceof RMaxExpr max) {
            // #513: the collect arm's twin (the three operator families below).
            return max.argument() == null ? "collectShape" : UNATTRIBUTED;
        }
        if (expr instanceof RMinExpr min) {
            return min.argument() == null ? "collectShape" : UNATTRIBUTED;
        }
        if (expr instanceof RSortExpr sort) {
            return sort.argument() == null ? "collectShape" : UNATTRIBUTED;
        }
        if (expr instanceof RWithMetaExpr) {
            // #515: the with-meta arm's twin — the arm is UNCONDITIONAL (the without-left
            // argument-null form and the entry-less form both claim), so the twin has no
            // reason to give; reachable from the mirror only on twin drift.
            return UNATTRIBUTED;
        }
        if (expr instanceof RJoinExpr) {
            // #515: the join arm's twin — UNCONDITIONAL (the without-left left-null form
            // claims; the separator is optional by grammar).
            return UNATTRIBUTED;
        }
        return "noAdaptArm";
    }

    /**
     * Mirror of {@link #adaptDeepFeatureCall}'s exits, in order (the twin discipline):
     * {@code deepFeatureUnresolved} (the linker's DEEP_FEATURE_NOT_FOUND channel empty or the
     * written name missing), {@code deepFeatureMeta} (the L-029 conservative belt — census
     * zero), {@code receiverNotExpressible} (child semantics via
     * {@link #slotNotExpressibleAtChild} — the #496 MF-1 law), {@code navTypeMissing} (the
     * cache-boundary belt — census 100% typed). A node passing every gate reads
     * {@link #UNATTRIBUTED} (the arm claims — reachable from the mirror only on twin drift).
     */
    private String reasonForDeepFeatureCall(RDeepFeatureCall deep, RWorkspace ws) {
        RAttribute feature = deep.resolvedFeature().orElse(null);
        if (feature == null || deep.featureName() == null || deep.featureName().isEmpty()) {
            return "deepFeatureUnresolved";
        }
        if (isMetaAnnotated(feature)) {
            return "deepFeatureMeta";
        }
        if (deep.receiver() == null || slotNotExpressibleAtChild(deep.receiver(), ws)) {
            return "receiverNotExpressible";
        }
        RMetaAnnotatedType navType = type(deep, ws);
        if (navType == null || navType.isMissing()) {
            return "navTypeMissing";
        }
        return UNATTRIBUTED;
    }

    /**
     * Mirror of {@link #adaptSymbolReference}'s exits, in order. The {@code !(symbol instanceof
     * RAttribute)} exit is sub-classified ({@code symbolUnresolved} — since #486 refined IN PLACE
     * to {@code symbolUnresolved.<facet>} ({@link #symbolUnresolvedFacet} — WHY the linker left
     * the symbol EMPTY, decoded against the resolution machinery's own precedence) — /
     * {@code bareFunctionReference}
     * — the L-109 point-free shape, deliberately declined so a NESTED bare function is never
     * mis-rendered — / {@code symbolNotAttribute}); the not-a-plain-function-input exit by its
     * three legs ({@code attrOutsideFunction} / {@code ruleReportScopedAttr} /
     * {@code notAnInputParam}); the claiming branches (alias, genuine bare enum, rule delegation)
     * fall through to {@link #UNATTRIBUTED}.
     */
    private String reasonForSymbolReference(RSymbolReference ref, RWorkspace ws) {
        if (!ref.args().isEmpty()) {
            return reasonForApply(ref, ws);
        }
        RNode symbol = ref.symbol().orElse(null);
        if (symbol instanceof RShortcut) {
            return UNATTRIBUTED; // the alias arm claims unconditionally
        }
        if (symbol instanceof REnumValue ev && ev.parent() instanceof REnumeration) {
            RFunction enclosing = enclosingFunction(ref);
            if (enclosing != null && collidesWithShortcut(enclosing, ref.name())) {
                return "bareEnumAliasCollision";
            }
            return UNATTRIBUTED; // the genuine bare enum-value arm claims
        }
        if (symbol instanceof RRule) {
            return UNATTRIBUTED; // the bare rule-delegation arm claims
        }
        if (symbol == null) {
            // #502: the twin restates arms 1+2's null-symbol claims IN ORDER (the arm order —
            // the alias name-match first, mirroring legacy isAliasReference's precedence with
            // the #453 self-shortcut exclusion; then the synthetic closure-param resolve; both
            // behind the only-exists exclusion). A claimed node reads UNATTRIBUTED — reachable
            // from the mirror only on twin drift, surfaced by the unattributed meter.
            if (!hasOnlyExistsElementAncestor(ref)) {
                RFunction aliasEnclosing = aliasMatchEnclosingFunction(ref);
                if (aliasEnclosing != null && ref.name() != null) {
                    RShortcut matched = shortcutByName(aliasEnclosing, ref.name());
                    if (matched != null && matched != enclosingShortcut(ref)) {
                        return UNATTRIBUTED; // the #502 alias name-match arm claims
                    }
                }
                if (ref.name() != null && isEnclosingClosureParamName(ref, ref.name())) {
                    return UNATTRIBUTED; // the #502 synthetic closure-param arm claims
                }
            }
            // #508 arm-C twin (the null-symbol leg — legacy's #137 unresolved shape, kept for
            // ladder fidelity with zero live rows): the seat-scoped requalify claims.
            if (wouldRequalifyAtParentSeat(ref, ws)) {
                return UNATTRIBUTED;
            }
            // #524 twin: the EMPTY-symbol bare-item arm claims (the #522 arm-B twin's idiom
            // at the null-symbol leg — the arm itself re-run with a throwaway id; a claimed
            // node reads UNATTRIBUTED, reachable only on twin drift).
            if (adaptNonAttrBareItemNav(ref, null, NodeId.ROOT, ws).isPresent()) {
                return UNATTRIBUTED;
            }
            // #486: the flat token refined IN PLACE — every channel composing the reason
            // (the blocker-reason pairs, the receiverBase. descents) carries the facets at
            // once, Σ facets ≡ the flat count per channel BY CONSTRUCTION (probe-only:
            // reasons compute on the probe/lock paths, never on a standing render).
            return "symbolUnresolved." + symbolUnresolvedFacet(ref, ws);
        }
        if (symbol instanceof RFunction) {
            // #492: the point-free arm claims position-independently (the position residue
            // re-spells at the CONSUMING gates' tokens once the parents adapt); the ONLY decline
            // here is the shortcut-collision face (legacy isAliasReference wins over the
            // bare-function arm, so a colliding name renders as an ALIAS CALL). The pre-arm
            // position × arity decode lives in the #492 probe receipts.
            RFunction enclosing = enclosingFunction(ref);
            if (enclosing != null && collidesWithShortcut(enclosing, ref.name())) {
                return "bareFunctionReference.aliasCollision";
            }
            return UNATTRIBUTED;
        }
        if (!(symbol instanceof RAttribute attr)) {
            // #502: the twin restates the non-attribute claims IN ORDER — the alias name-match
            // (legacy isAliasReference's precedence applies to ANY non-attribute bind) then the
            // BOUND closure-param (the parameter's own RClosureParameter node since v3.2 seat 8,
            // or the declaring RInlineFunction for a name-only parameter — the two shapes
            // RInlineFunction.declaringLambdaOf reads).
            if (!hasOnlyExistsElementAncestor(ref)) {
                RFunction aliasEnclosing = aliasMatchEnclosingFunction(ref);
                if (aliasEnclosing != null && ref.name() != null) {
                    RShortcut matched = shortcutByName(aliasEnclosing, ref.name());
                    if (matched != null && matched != enclosingShortcut(ref)) {
                        return UNATTRIBUTED; // the #502 alias name-match arm claims
                    }
                }
                if (RInlineFunction.declaringLambdaOf(symbol, ref.name()).isPresent()) {
                    return UNATTRIBUTED; // the #502 bound closure-param arm claims
                }
            }
            // #508 arm-C twin: the seat-scoped requalify claims (the arg-seat tryBareEnumArg
            // mirror against the callee's positional enum param + the equality-seat
            // tryBareEnumComparand mirror against the sibling's cached enumeration — the
            // arm's own gates restated via the SAME #504 helpers); the residue keeps the
            // probed spelling.
            if (wouldRequalifyAtParentSeat(ref, ws)) {
                return UNATTRIBUTED;
            }
            // #522 arm-B twin: the BY-NAME implicit-item bare-nav arm claims (the mirror
            // re-runs the arm itself with a throwaway id — the wouldRequalifyAtParentSeat
            // idiom; a claimed node reads UNATTRIBUTED, reachable only on twin drift).
            if (adaptNonAttrBareItemNav(ref, symbol, NodeId.ROOT, ws).isPresent()) {
                return UNATTRIBUTED;
            }
            // #514: the flat token refined IN PLACE (the #491 pattern): the symbol's own
            // class names the family the linker actually bound (the design must know WHAT
            // non-attribute classes live here) and the parent family names the consumer seat.
            return "symbolNotAttribute.sym:" + symbol.getClass().getSimpleName()
                    + ".p:" + probeParentFacet(ref);
        }
        if (isSyntheticRuleInputRef(attr)) {
            return UNATTRIBUTED; // the #497 rule-input arm claims unconditionally — twin drift only
        }
        if (!(attr.parent() instanceof RFunction func)) {
            // #480: the bare-attr arm's twin — the arm runs on this leg (a data-type field), so a
            // decliner names the arm's own first failing gate dot-suffixed onto the channel token
            // (the #478 pattern); the bare sentinel surfaces UNWRAPPED so the unattributed
            // meter's ":unattributed" suffix key still fires (reached only if the twins drift).
            String gate = bareAttrItemNavDeclineGate(ref, attr, ws);
            // #528 arm-5: the three census families now MINT IRImplicitAttrNav for a non-meta,
            // attribute-channel-TYPED attribute (the L-113 conversion — the mint site's own
            // guards restated BY CALL, this leg being the mint's census-narrow leg), so the
            // mirror drops exactly that slice; a matching arrival here is twin drift. The META
            // slice kept the standing faces until PR #640's arm E admitted it to the mint (the
            // mirror reads the arm's own reader below, so it drops that slice too - round 1, cq
            // SF-2: this note re-cut); the UNTYPEABLE residue keeps a face (the mint's
            // #514-pattern type guard declines it, prove-or-decline — the Copilot #528 R1
            // class), dot-refined in place, as does every other gate verdict.
            if (syntheticConditionInstanceOwner(ref, attr) != null) {
                return UNATTRIBUTED; // #640 arm A: the condition-instance mint claims it - twin drift only
            }
            if (isImplicitAttrMintFamily(gate)) {
                RMetaAnnotatedType mintType = implicitAttrMintType(ref, attr, ws); // #640 arm E - the arm's own reader
                if (mintType != null && !mintType.isMissing()) {
                    return UNATTRIBUTED;
                }
                return "attrOutsideFunction." + gate + ".untyped";
            }
            return UNATTRIBUTED.equals(gate) ? UNATTRIBUTED : "attrOutsideFunction." + gate;
        }
        if (func.origin() != RFunction.Origin.FUNCTION) {
            // #480: the arm also runs on this leg but can never claim it (the identity guard
            // requires the element type's OWN feature — a function-parented attr is never a data
            // type's member), so the coarse token stays exit-faithful.
            return "ruleReportScopedAttr"; // a rule/report synthetic input
        }
        if (!containsByIdentity(func.inputs(), attr)) {
            // #514: the flat token refined IN PLACE (the #491 pattern): the output-by-identity
            // split names the class (the function OUTPUT referenced by name vs any other
            // function-parented non-input bind) and the parent family names the consumer seat.
            // ARM-1 twin: the bare-attr arm runs first (never claims this class — the identity
            // guard), then the OUTPUT mint claims exactly the non-meta attribute-channel-typed
            // slice; the residue names the arm's own failing gate.
            boolean isOutput = func.output().orElse(null) == attr;
            if (isOutput) {
                if (isMetaAnnotated(attr)) {
                    return "notAnInputParam.out.meta";
                }
                RMetaAnnotatedType outType = ws == null ? null : ws.getInferredAttributeType(attr);
                if (outType == null || outType.isMissing()) {
                    return "notAnInputParam.out.untyped";
                }
                return UNATTRIBUTED; // the #514 ARM-1 OUTPUT mint claims — twin drift only
            }
            return "notAnInputParam.shadow.p:" + probeParentFacet(ref);
        }
        if (collidesWithShortcut(func, ref.name())) {
            return "paramAliasCollision";
        }
        if (isMetaAnnotated(attr)) {
            // #513: the flat token refined IN PLACE (the #491 pattern — Σ facets ≡ the flat
            // count BY CONSTRUCTION): the raw node's PARENT family names the consumer seat.
            // #514 ARM-3 twin: the IRMetaParamRef mint claims the attribute-channel-typed
            // slice UNCONDITIONALLY at this seat (the consumer gates own the per-seat
            // claims — the un-admitted seats' residue re-spells at THEIR tokens, e.g. the
            // callArgs gate's arg:IRMetaParamRef); only the untyped residue keeps a face
            // here, callee-facet-suffixed for the call-arg slice.
            RMetaAnnotatedType mpType = ws == null ? null : ws.getInferredAttributeType(attr);
            if (mpType == null || mpType.isMissing()) {
                return "metaParam.untyped." + probeParentFacet(ref) + metaParamCalleeFacet(ref);
            }
            return UNATTRIBUTED; // the #514 ARM-3 mint claims — twin drift only
        }
        return UNATTRIBUTED;
    }

    /**
     * #513: the parent-family facet (probe-only, the twin channel) — the raw node's parent
     * class simple name, {@code orphan} for a parentless node (the walk-out belt).
     */
    private static String probeParentFacet(RNode raw) {
        RNode parent = raw.parent();
        return parent == null ? "orphan" : parent.getClass().getSimpleName();
    }

    /**
     * #514: the first two dot-segments of a composed reason token (probe-only) — the
     * receiverNotExpressible descent caps the recursive reason at the informative head so
     * the token space stays bounded.
     */
    private static String reasonHead(String reason) {
        int first = reason.indexOf('.');
        if (first < 0) {
            return reason;
        }
        int second = reason.indexOf('.', first + 1);
        return second < 0 ? reason : reason.substring(0, second);
    }

    /**
     * #514: the metaParam call-arg slice's CALLEE-param meta agreement (probe-only) — for a
     * ref sitting in an args-present call's argument list, the positional callee input's
     * meta state ({@code .pMeta} the wrapper-to-wrapper candidates / {@code .pPlain} the
     * unwrap-coercion class / {@code .argSeatMiss} no positional input); empty for every
     * non-call-arg parent (those seats keep the #513 parent facet alone).
     */
    private static String metaParamCalleeFacet(RSymbolReference ref) {
        if (!(ref.parent() instanceof RSymbolReference call) || call.args().isEmpty()
                || !(call.symbol().orElse(null) instanceof RFunction callee)) {
            return "";
        }
        int idx = -1;
        for (int i = 0; i < call.args().size(); i++) {
            if (call.args().get(i) == ref) {
                idx = i;
                break;
            }
        }
        if (idx < 0 || idx >= callee.inputs().size()) {
            return ".argSeatMiss";
        }
        return isMetaAnnotated(callee.inputs().get(idx)) ? ".pMeta" : ".pPlain";
    }

    /**
     * #486: the symbolUnresolved DECODE — WHY a bare reference's resolved symbol is EMPTY,
     * classified against the resolution machinery's own precedence (probe-only; the flat
     * reason token refined IN PLACE at its single seat, {@code synthetic.}-prefixed by the
     * mechanism split above). The corpus parses diagnostic-clean, so a PARSE-TIME occurrence
     * sits in a deliberately-empty class: the linker's silent walks
     * ({@code AstBuilder.buildOnlyExistsPath}'s synthesized path — audit-exempt), the
     * pass-6 CLEARING-ONLY arms ({@code TypeInferenceEngine.runTypeDirectedResolution}'s #451
     * choice-option / #458 meta-feature + sibling-enum-value arms — upstream resolves these
     * silently through scope channels the lexical chain cannot see; the fork clears the stale
     * SYMBOL_NOT_FOUND and deliberately binds NOTHING, because binding would flip
     * bind-state-keyed render arms), the #453 alias-self-scope filter, or a name legacy
     * renders through its own name-match oracles without any cross-ref
     * ({@code isAliasReference}'s name-match fallback — the {@link #adaptSymbolReference}
     * arm-1 deferred edge). A SYNTHETIC occurrence (the decode-proven dominant population)
     * is a render-time equivalent's piece the linker never saw — the same head-name ladder
     * classifies WHAT its head names. The facets, most-specific first (each mirrors the
     * machinery it decodes):
     * <ul>
     *   <li>{@code nameMissing} / {@code dottedName} — contract-impossible drift detectors (the
     *       grammar always captures a simple name; qualified references parse as their own node
     *       classes);</li>
     *   <li>{@code onlyExists} — an {@link ROnlyExistsElement} ancestor: the synthesized
     *       only-exists receiver class ({@code LexicalResolutionPass} suppresses the diagnostic
     *       and {@code ResolutionAudit} exempts the subtree symmetrically; legacy renders the
     *       only-exists navigation from the element's own strings, so an unresolvable root
     *       stays silently EMPTY);</li>
     *   <li>{@code aliasName.<bodyFace>} — the name matches an enclosing function's shortcut:
     *       legacy {@code isAliasReference}'s NAME-MATCH fallback renders the alias invocation
     *       with no resolution at all (the partial-resolution edge arm 1 defers — a claim-shaped
     *       class); since #492 the shared {@link #aliasNavBodyFacet} walk sub-classifies the
     *       shortcut body ({@code bodyTyped} the claim-shaped face · {@code usesOutput} /
     *       {@code bodyMissing} / {@code bodyMeta} the honest declines);</li>
     *   <li>{@code closureParam} / {@code fnScopeName} — drift detectors (pass 5 registers
     *       closure params and function inputs/output; an unresolved name matching either means
     *       linker drift, not a standing class);</li>
     *   <li>{@code itemAttr} / {@code itemAttrMeta} — the enclosing filter/extract binder's
     *       source element type carries a same-named attribute (the arms' own PROVABLE slice:
     *       {@link #filterExtractSourceOfArmBinder} + {@link #sourceElementDataType} +
     *       {@link #findAttributeOnDataTypeByName}): the Cat-9-SHAPED residue the pass-6
     *       implicit-item resolution did not bind (the #453 alias-self filter carriers and the
     *       type-engine misses land here);</li>
     *   <li>the in-lambda prefixes — {@code lambda.} (an enclosing inline function exists and
     *       the element type PROVES, but the name is NOT a member — the #451 choice-option
     *       pattern reads {@code lambda.global.type}: the bare option name IS a global data
     *       type's name) / {@code lambdaSrcUnprovable.} (an enclosing inline function whose
     *       element form the arms cannot prove — then/named/switch-crossed binders and
     *       unprovable sources; membership is UNKNOWABLE, disclosed rather than absorbed);
     *       no prefix = no enclosing inline function (bounded walk, stops at rule/function —
     *       {@code nearestEnclosingInlineFunction});</li>
     *   <li>{@code siblingEnumValue} — the parent is an {@link REqualityExpr} whose OTHER
     *       operand's cached type is an {@link REnumeration} carrying a value of this name
     *       (the #458 Arm B mirror: upstream's expected-type scope contribution; EQUALITY
     *       only, the arm's own boundary — comparison seats stay undecoded here exactly as
     *       the clearing arm leaves them);</li>
     *   <li>{@code global.<kind>} — a root element with EXACTLY this name exists somewhere in
     *       the workspace ({@code RWorkspace.findByName} filtered to true name-equality — the
     *       index's fuzzy fallback returns near-misses, so emptiness alone is not the test):
     *       a VISIBILITY gap (pass 4 binds visible globals, so a hit here means the name lives
     *       in a namespace the reference's file does not see) or the in-lambda choice-option
     *       pattern; the import-level split (same-namespace vs not-imported) is deliberately
     *       NOT decoded — a follow-up sharpens it if the face carries the mass;</li>
     *   <li>{@code absent} — no root element anywhere carries the name: the true
     *       corpus/model-gap face (record-feature members and other builtin-shaped names land
     *       here too — builtins are not indexed root elements).</li>
     * </ul>
     * The facet's OWN walks are bounded ({@link #PARENT_WALK_LIMIT}) and the index lookup is
     * map-backed; the shared {@link #enclosingFunction} helper keeps its standing unbounded
     * form (the pre-existing adapt-path walk — a Seat-1 #486 OBS-1 hygiene note, not widened
     * here). The facet runs ONLY where reasons compute (the blocker probe + the unit locks) —
     * zero standing-render cost.
     */
    private String symbolUnresolvedFacet(RSymbolReference ref, RWorkspace ws) {
        // The MECHANISM split, first-position (run-first proven): a parse-time tree member is
        // child-linked at its own seat; a reference whose parent does NOT list it in children()
        // is an UP-only parented node — the legacy RE-ENTRANT SYNTHESIS shape (a fresh receiver
        // built at render time and parented for the scope walks only: ReferenceHandler
        // .synthesizeFeatureCall's disguised-nav receiver + the rule/report input-synthesis
        // seats build exactly this, resolving heads via resolveNameInFunction — inputs/output
        // ONLY — so an alias/closure-param/other head stays EMPTY by construction; pass 5
        // can never see these nodes, so this is NOT a linker gap). The corpus decode read the
        // near-whole population on this face: synthetic.aliasName 3,132 · synthetic
        // .closureParam 510 · synthetic.absent 232 — the parse-time complement is the ~25-event
        // unprefixed residue.
        return isChildLinkedAtOwnSeat(ref)
                ? symbolUnresolvedFacetBase(ref, ws)
                : "synthetic." + symbolUnresolvedFacetBase(ref, ws);
    }

    /**
     * #486: whether the node is a member of its own parent's {@code children()} (identity). A
     * parentless node counts as linked (fixture shapes; corpus references always carry
     * parents). Single-hop by design — the decode proved every orphan breaks at its OWN seat
     * (the synthesized RECEIVER itself), and the single hop is the mechanism-precise check.
     */
    private static boolean isChildLinkedAtOwnSeat(RNode node) {
        RNode parent = node.parent();
        if (parent == null) {
            return true;
        }
        for (RNode child : parent.children()) {
            if (child == node) {
                return true;
            }
        }
        return false;
    }

    /** #486: the head-name ladder — see {@link #symbolUnresolvedFacet}'s facet list. */
    private String symbolUnresolvedFacetBase(RSymbolReference ref, RWorkspace ws) {
        String name = ref.name();
        if (name == null || name.isEmpty()) {
            return "nameMissing";
        }
        if (name.indexOf('.') >= 0) {
            return "dottedName";
        }
        if (hasOnlyExistsElementAncestor(ref)) {
            return "onlyExists";
        }
        RFunction enclosing = enclosingFunction(ref);
        if (enclosing != null && collidesWithShortcut(enclosing, name)) {
            // #492: the flat face refined IN PLACE by the shared alias-nav body walk (one walk,
            // three seats — aliasName/featureUnresolved.aliasHead/receiverAlias spell the same
            // faces, so the receiver-seat and nav-seat distributions stay comparable).
            String bodyFace = aliasNavBodyFacet(shortcutByName(enclosing, name), enclosing, ws);
            return "aliasName." + (bodyFace == null ? "bodyTyped" : bodyFace);
        }
        if (isEnclosingClosureParamName(ref, name)) {
            return "closureParam";
        }
        if (enclosing != null && resolveNameInFunction(enclosing, name) != null) {
            return "fnScopeName";
        }
        String prefix = "";
        if (nearestEnclosingInlineFunction(ref) != null) {
            RExpression source = filterExtractSourceOfArmBinder(ref);
            RDataType elementType = source == null ? null : sourceElementDataType(source, 0);
            if (elementType != null) {
                RAttribute member = findAttributeOnDataTypeByName(elementType, name);
                if (member != null) {
                    return isMetaAnnotated(member) ? "itemAttrMeta" : "itemAttr";
                }
                prefix = "lambda.";
            } else {
                prefix = "lambdaSrcUnprovable.";
            }
        }
        if (equalitySiblingEnumValueMatches(ref, name, ws)) {
            return prefix + "siblingEnumValue";
        }
        String globalKind = globalRootElementKind(name, ws);
        if (globalKind != null) {
            return prefix + "global." + globalKind;
        }
        return prefix + "absent";
    }

    /**
     * #486: the ancestor walk to the synthesized only-exists element — since v3.2 seat 8 the
     * ONE enclosure read every consumer consults ({@link ROnlyExistsElement#encloses}, LAW 69).
     * The swap is an IDENTITY on this seat since round 1: the declaration carries the same
     * {@code PARENT_WALK_LIMIT} bound (64) this walk carried before it (the seat-8 code had
     * swapped a bounded walk for an unbounded one and called the bound "a termination guard"
     * without a measurement — the round-1 spec review's SF-2); the eight callers — three LIVE
     * adapter arms (the alias-match lowering, {@code bareItemMemberResolution},
     * {@code isClosureParamHead}) and five census/twin reads — are covered by the chain's
     * both-route 26-cell identity and the route content compare at zero.
     */
    private static boolean hasOnlyExistsElementAncestor(RNode start) {
        return ROnlyExistsElement.encloses(start);
    }

    /**
     * #486: the DECODE's in-lambda detector — the nearest enclosing {@link RInlineFunction}
     * with NO switch conservatism (that belongs to the claim arms' walks; a census classifies
     * position, it does not admit), stopping at rule/function roots like every enclosing walk.
     */
    private static RInlineFunction nearestEnclosingInlineFunction(RNode start) {
        RNode cur = start == null ? null : start.parent();
        int depth = 0;
        while (cur != null && depth++ < PARENT_WALK_LIMIT) {
            if (cur instanceof RInlineFunction inline) {
                return inline;
            }
            if (cur instanceof RRule || cur instanceof RFunction) {
                return null;
            }
            cur = cur.parent();
        }
        return null;
    }

    /**
     * #486: the #458 Arm B mirror — the reference's DIRECT parent is an equality whose other
     * operand's cached type is an enumeration carrying a value of this name (upstream's
     * expected-type scope contribution, the clearing-only class). The sibling reads the
     * node-keyed type cache — a parsed sibling carries its fixed-point type; a cache-invisible
     * sibling reads MISSING and the facet falls through (prove-or-decline, census-grade). The
     * value walk reads the enum's OWN values only — the #458 arm's {@code allEnumValueNames}
     * additionally walks the super-enum chain; a super-declared value here falls through to
     * the global/absent faces (the Seat-1 #486 OBS-2 boundary, zero corpus mass either way —
     * the facet read ZERO events on every cell).
     */
    private static boolean equalitySiblingEnumValueMatches(
            RSymbolReference ref, String name, RWorkspace ws) {
        if (!(ref.parent() instanceof REqualityExpr eq)) {
            return false;
        }
        RExpression sibling = eq.rawLeft() == ref ? eq.rawRight() : eq.rawLeft();
        if (sibling == null) {
            return false;
        }
        RMetaAnnotatedType siblingType = type(sibling, ws);
        if (siblingType == null || siblingType.isMissing()
                || !(siblingType.type() instanceof REnumTypeRef enumRef)) {
            return false;
        }
        for (REnumValue value : enumRef.astNode().values()) {
            if (name.equals(value.name())) {
                return true;
            }
        }
        return false;
    }

    /**
     * #486: does a root element with EXACTLY this name exist anywhere in the workspace, and as
     * WHAT kind — {@code RWorkspace.findByName}'s exact hits filtered to true name-equality
     * (the index's fuzzy fallback returns trigram near-misses whose names differ, so a
     * non-empty result alone proves nothing), the kind picked by a fixed precedence so the
     * token is deterministic when one simple name is declared as different kinds across
     * namespaces. Returns null when nothing matches exactly (the {@code absent} face).
     */
    private static String globalRootElementKind(String name, RWorkspace ws) {
        List<RNode> hits = ws.findByName(name);
        boolean function = false, rule = false, dataType = false, choice = false,
                enumeration = false, typeAlias = false, libraryFunction = false;
        for (RNode hit : hits) {
            if (hit instanceof RFunction fn && name.equals(fn.name())) {
                function = true;
            } else if (hit instanceof RRule r && name.equals(r.name())) {
                rule = true;
            } else if (hit instanceof RDataType dt && name.equals(dt.name())) {
                dataType = true;
            } else if (hit instanceof RChoice ch && name.equals(ch.name())) {
                choice = true;
            } else if (hit instanceof REnumeration en && name.equals(en.name())) {
                enumeration = true;
            } else if (hit instanceof RTypeAlias ta && name.equals(ta.name())) {
                typeAlias = true;
            } else if (hit instanceof RLibraryFunction lf && name.equals(lf.name())) {
                libraryFunction = true;
            }
        }
        if (function) {
            return "function";
        }
        if (rule) {
            return "rule";
        }
        if (dataType) {
            return "type";
        }
        if (choice) {
            return "choice";
        }
        if (enumeration) {
            return "enum";
        }
        if (typeAlias) {
            return "typeAlias";
        }
        if (libraryFunction) {
            return "libraryFunction";
        }
        return null;
    }

    /**
     * Mirror of {@link #adaptApply}'s exits, in order — the callee resolution first ({@code
     * calleeUnresolved}; {@code calleeRuleReportBridge} for a non-FUNCTION-origin {@link RFunction};
     * {@code calleeNotFunction} for the residual symbol class that is neither an {@link RFunction},
     * an {@link RRule} nor an {@link RLibraryFunction} — the #498 rule leg claims RRule callees
     * through the same {@link RFunction#fromRule} bridge the arm adapts with, and the #520
     * library-apply mint claims the RLibraryFunction class UNCONDITIONALLY [that slice returns
     * the UNATTRIBUTED belt here], so the shared gates below attribute the surviving legs'
     * residues under their own tokens); the per-argument loop names the FIRST
     * failing argument's gate — {@code argNotExpressible} when the argument subtree itself does
     * not lower, else the lowered form that failed the call-arg admission or-composition
     * ({@link #isSimpleCallArg} / {@link #isNavCallArg} / {@link #isFilterExtractBoundItemOperand}):
     * {@code argNav.<facet>} (since #488 — {@link #argNavFacet}; the CLEAN faces lower since #489,
     * so only the decline faces mint) / {@code argEmpty} / {@code argItem} / generic {@code arg:<IRClass>}.
     */
    private String reasonForApply(RSymbolReference ref, RWorkspace ws) {
        RNode symbol = ref.symbol().orElse(null);
        if (symbol == null) {
            return "calleeUnresolved";
        }
        RFunction callee;
        if (symbol instanceof RFunction fn) {
            if (fn.origin() != RFunction.Origin.FUNCTION) {
                return "calleeRuleReportBridge";
            }
            callee = fn;
        } else if (symbol instanceof RRule rule) {
            // #498: the mirror reads the same fromRule bridge the arm adapts through.
            callee = RFunction.fromRule(rule);
        } else if (symbol instanceof RLibraryFunction) {
            // #520: the library-apply mint is UNCONDITIONAL for the symbol class, so a
            // decline reaching this mirror with an RLibraryFunction callee is
            // contract-impossible (the stable-token belt).
            return UNATTRIBUTED;
        } else {
            return "calleeNotFunction";
        }
        RAttribute output = callee.output().orElse(null);
        if (output == null) {
            return "calleeNullOutput";
        }
        if (isMetaAnnotated(output)) {
            // #502: the meta-output arm claims the FUNCTION-callee slice (census-narrow — 100%
            // FUNCTION callees at the census; the bridged RULE callee keeps the standing
            // decline and this face).
            return symbol instanceof RRule ? "calleeMetaOutput" : UNATTRIBUTED;
        }
        // #528 arm-4: the meta-param pre-gate is GONE from the arm (the arms-dead recut — see
        // adaptApply), so the mirror drops its calleeMetaParam leg in lockstep; a call with a
        // meta-annotated input now attributes to whatever its per-argument seats say, exactly
        // like any other call (the token retires with the face).
        List<RExpression> rawArgList = ref.args();
        for (int i = 0; i < rawArgList.size(); i++) {
            RExpression rawArg = rawArgList.get(i);
            Optional<IRExpr> arg = adapt(rawArg, ws);
            RAttribute argParam = i < callee.inputs().size() ? callee.inputs().get(i) : null;
            if (arg.isEmpty()) {
                // #504 arm-C1a: the requalification fallback restated — mirrors the adapt arm.
                arg = requalifiedEnumArg(rawArg, argParam, NodeId.ROOT, ws);
            }
            if (arg.isEmpty()) {
                return "argNotExpressible";
            }
            if (!(isSimpleCallArg(arg.get())
                    || isNavCallArg(arg.get(), rawArg, argParam, ws)
                    || isFilterExtractBoundItemOperand(arg.get(), rawArg)
                    // #502 arm-3: the binder-UNBOUNDED item arg — the mirror restates the
                    // arm's admission (the argItem face now mints only for the residue).
                    || isTypedUserItemArg(arg.get(), rawArg)
                    // #529: the DECLARATION-channel item arg — the mirror restates the
                    // arm's admission BY the same call (the argItem face's last live
                    // carrier taught; the token stays for the residue).
                    || isDeclProvenMetaFreeItemArg(arg.get(), rawArg)
                    // #530 arm-D: the deref-proven meta-param argument — the mirror
                    // restates the arm's admission BY the same call (the
                    // arg:IRMetaParamRef face's live carriers taught; the generic token
                    // stays for any residue).
                    || isDerefProvenMetaParamArg(arg.get(), rawArg, argParam)
                    // #504 arm-C2: the explicit `empty` argument admission — mirrors the
                    // adapt arm (the argEmpty token is now a drift face at its seat).
                    || arg.get() instanceof IRImplicitAttrNav   // #640 arm D - mirrors the arm
                    || arg.get() instanceof IREmptyLiteral
                    // #505 arm-B: the list-op argument admission — mirrors the adapt arm
                    // (the arg:IRListOp token is now a drift face at its seat).
                    || arg.get() instanceof IRListOp
                    // #511: the BinaryOp argument admission — mirrors the adapt arm (a
                    // #512 lockstep backfill: the #511 teach admitted the kind at the live
                    // gate without this mirror leg — zero live carriers, latent-only, since
                    // the mirror runs on DECLINED calls and every BinaryOp-arg call now
                    // lowers unless ANOTHER argument declines first).
                    || arg.get() instanceof BinaryOp
                    // #512 — the ARG-KIND SWEEP admissions — mirror the adapt arm (the
                    // five arg:<kind> tokens are now drift faces at their seat).
                    || arg.get() instanceof IRToString
                    || arg.get() instanceof IRListConstruct
                    || arg.get() instanceof IRLambdaOp
                    || arg.get() instanceof IRConstruct
                    || arg.get() instanceof IRConditional)) {
                return argToken(arg.get(), rawArg, argParam, ws);
            }
        }
        return UNATTRIBUTED;
    }

    /**
     * The lowered form that failed the seat's admission: nav (since #488 refined IN PLACE to
     * {@code argNav.<facet>} — {@link #argNavFacet}; since #489 only the DECLINE faces can mint —
     * the admitted chains lower) / empty (since #504 a DRIFT face at this seat — the arm-C2
     * admission claims every {@code IREmptyLiteral} arg unconditionally, so the token fires
     * only on twin divergence) / item / generic.
     */
    private static String argToken(IRExpr lowered, RExpression rawArg, RAttribute param, RWorkspace ws) {
        if (lowered instanceof FieldAccess nav) {
            // #488: the flat token refined IN PLACE — every channel composing the reason
            // (both blocker instruments) carries the facets at once, Σ facets ≡ the flat
            // count per channel BY CONSTRUCTION (probe-only: reasons compute on the
            // probe/lock paths, never on a standing render). #489: a null face here is
            // contract-impossible (the seat declines only non-admitted args) — the stable
            // token is the seats-drift detector, the #486 nameMissing precedent.
            String face = argNavFacet(nav, rawArg, param, ws);
            return face == null ? "argNav.admitted" : "argNav." + face;
        }
        if (lowered instanceof IREmptyLiteral) {
            return "argEmpty";
        }
        if (lowered instanceof IRVariable var
                && (var.variableKind() == IRVariable.VariableKind.USER_ITEM
                        || var.variableKind() == IRVariable.VariableKind.SYNTHETIC_ITEM)) {
            return "argItem";
        }
        return "arg:" + lowered.getClass().getSimpleName();
    }

    /**
     * #488: the argNav DECODE / #489: the ADMISSION CLASSIFIER — the single walk that both admits
     * a navigation call argument (returns {@code null} — the L-042 revival, {@link #isNavCallArg})
     * and names the decline face for the witness channel. The #488 {@code argNav.<facet>} grammar
     * is refined IN PLACE a second time: every occurrence either LOWERS or mints EXACTLY ONE face
     * at the same seat, so claims + Σ faces ≡ the flat #488 population BY CONSTRUCTION (the
     * conservation-friendly witness shape), and the one-walk-two-consumers structure makes
     * admission/witness twin-drift impossible. The spelling:
     * {@code [meta.|typeMissing.|itemRootMeta.|itemRawHead.|typeGap.]<root>.<depth>[.multi]} —
     * <ul>
     *   <li>{@code meta.} / {@code typeMissing.} — some hop's RESULT type carries meta
     *       ({@code RMetaAnnotatedType.hasMeta()}) or reads the type engine's MISSING
     *       sentinel ({@code isMissing()} — {@code getInferredType} returns
     *       {@code RMetaAnnotatedType.MISSING}, never null, for any node the fixed point
     *       never saw; the #488 Seat-1 MF-1 recut made this face LIVE — the null check stays as
     *       a belt): drift detectors, expected ZERO at population — the nav arms decline
     *       meta features ({@code metaFeature} / {@code itemMetaSourced}) and retype
     *       synthetic bases before lowering, so neither face should survive a real
     *       lowering; since #491 an otherwise-ADMITTING root's OWN type joins the same
     *       flags (the #489 Seat-1 OBS-2 root-type belt — the hop walk reads hop RESULT
     *       types only, and a meta-typed param root was excluded only INDIRECTLY by
     *       {@code adaptSymbolReference}'s meta-input decline; gated on admission so no
     *       decline face re-spells); {@code typeMissing.} wins the prefix when both flags
     *       accumulate across different hops or the root (a MISSING reading is the
     *       stronger drift signal); since #527 a PURE missing-typed read on a PARAM root
     *       ADMITS instead — the compiler's {@code argNavMissingHop} belt serves every
     *       claim whose lowered tree carries such a chain whole-legacy at the claim root
     *       (the missing sentinel makes the native witness render unprovable, and the
     *       #527 argGapGate census read legacy's own raw-pass verdicts on the whole
     *       population) — while a meta flag anywhere and every other root keep the
     *       standing decline faces;</li>
     *   <li>{@code <root>} — the receiver chain's base: {@code param} (a function input — the
     *       {@code MapperS.of(name)} root; ADMITS), {@code item} / {@code synItem} (a filter/
     *       extract-bound explicit/elided implicit — the lambda-binder renders; ADMIT when
     *       un-chained-raw and meta-free-rooted, else the residue faces below),
     *       {@code root:IRApply} (a nav off a lowered explicit call — the #484 composition;
     *       ADMITS — {@code adaptApply} is reachable only with explicit args present, so this
     *       root never hides a point-free bare reference whose implicit input the render would
     *       drop), {@code closureParam} / {@code aliasVar} / {@code letBinder} (zero-population
     *       roots — decline), {@code alias} (an {@link IRReference.ReferenceKind#ALIAS} root —
     *       since #492 a POPULATED explicit-decline face, no longer a zero-population detector:
     *       the alias-nav teach lowers alias-rooted chains, so one can now reach an argument
     *       position; the arg-side admission — legacy's #360 arg neutralization over an alias
     *       chain — is the NAMED FOLLOW-UP, declined here until its own decode),
     *       the generic {@code root:<IRClass>}, or {@code rootMissing} (a NULL chain root —
     *       contract-impossible for adapter-built lowerings, the stable-token belt: the #486
     *       {@code nameMissing} precedent);</li>
     *   <li>the #489 ADMISSION-RESIDUE faces (the ring's own decode; RE-CUT at #523 — the
     *       guard co-design): {@code itemRootMeta.} — an item/synItem root whose OWN type is
     *       meta/untyped (the {@code metaItemReceiver} guarded family's STAMPED-TYPE channel —
     *       legacy threads a {@code .map("Type coercion", …getValue())} deref the hop-by-hop
     *       render cannot); {@code itemRawHead.} — the #523 residue drift face: an item-rooted
     *       arg whose raw HEAD class is outside the proven-route set (see the admission
     *       comment in the method body — the former {@code itemChain.}/{@code itemSrcUnprovable.}
     *       decline populations ADMIT since #523: the guarded shapes serve whole-legacy at the
     *       {@code RSymbolReference} claim root via the compiler's #469-law seat, the disguised
     *       heads trip legacy's own {@code synthesizeImplicitItemChain} predicate at the
     *       guard's call-branch probe, and the hop1 natives are the #469/#479/#480 proven
     *       composes); {@code typeGap.} — the chain admits but its
     *       result R-type differs from the callee parameter's declared R-type
     *       ({@code RWorkspace.getInferredAttributeType} vs the chain's stamped type): legacy's
     *       guarded arg-coercion trigger read at source — {@code ReferenceHandler}'s
     *       {@code paramJavaType.equals(actualItemType)} precondition passes an arg RAW only on
     *       Java-type equality, else the meta-deref / BigInteger / Integer→BigDecimal null-safe
     *       HOIST fires (a statement hoist the recursion-free arm cannot express — the drr POJO
     *       ring decode); R-type equality is the adapter-side SUFFICIENT mirror (R-equal ⇒
     *       Java-equal). Since #527 the residue is the ARMS-DEAD split (the argGapGate census
     *       BY CALL): an ORACLE-LEAF chain root skips the gate pair-blind (the containing
     *       claim serves whole-legacy — any coercion is legacy's own inside the serve), a
     *       MODEL-classed result and an int-family result into a non-BigDecimal-mapping param
     *       admit natively (no coercion arm can fire — legacy passes RAW and the flat render
     *       is param-type-blind), and the int→non-integer-number pairs admit census-narrow at
     *       the {@code synItem.deep} seat only (the ≥3-hop item chain trips the standing
     *       post-pin scan, so the claim serves whole-legacy with the hoist inside); the face
     *       survives as the honest drift residue everywhere else (e.g. a param-rooted
     *       {@code g:int_number}, an unreadable-param numeric chain);</li>
     *   <li>{@code <depth>} — {@code hop1} / {@code hop2} / {@code deep} (≥3 hops): the render
     *       is the same chain machinery at any depth, the split sizes the shapes;</li>
     *   <li>{@code .multi} — the ACCUMULATED result cardinality is MULTI (absent when single):
     *       the evaluate-slot accessor is the per-param {@code asMulti} decision either way
     *       (facet {@code tailMulti}, served per-call by the range-correlated resolver), the
     *       marker sizes the {@code MapperC}-chain sub-population.</li>
     * </ul>
     *
     * <p>Package-private (not private) for the #491 root-type-belt pin
     * ({@code ExpressionToIRAdapterTest.argNavRootTypeBeltDeclinesMetaAndMissingRoots}): the
     * belt's distinct state — clean-typed hops over a meta/missing-typed admitting root — is
     * structurally unreachable through {@code adapt} (the upstream gates decline it before any
     * lowering) and a hand-built RAW chain stamps the MISSING sentinel on every hop, masking the
     * root's contribution; the pin therefore hand-constructs the lowered triple and calls the
     * classifier directly.
     */
    static String argNavFacet(FieldAccess nav, RExpression rawArg, RAttribute param, RWorkspace ws) {
        int hops = 0;
        boolean meta = false;
        boolean typeMissing = false;
        IRExpr walk = nav;
        while (walk instanceof FieldAccess hop) {
            hops++;
            if (hop.type() == null || hop.type().isMissing()) {
                typeMissing = true;
            } else if (hop.type().hasMeta()) {
                meta = true;
            }
            walk = hop.receiver();
        }
        String root;
        boolean rootAdmits;
        boolean itemRootMeta = false;
        boolean itemHeadUnproven = false;
        if (walk == null) {
            // rootMissing — a contract-impossible drift detector (the #486 nameMissing
            // precedent): every adapter-built FieldAccess carries a lowered receiver, so a
            // null chain root can only mean a partially-built node reached the probe — a
            // stable token, never an NPE (the declineReason robustness convention).
            root = "rootMissing";
            rootAdmits = false;
        } else if (walk instanceof IRVariable var) {
            root = switch (var.variableKind()) {
                case PARAM -> "param";
                case USER_ITEM -> "item";
                case SYNTHETIC_ITEM -> "synItem";
                case CLOSURE_PARAM -> "closureParam";
                case ALIAS -> "aliasVar";
                case LET_BINDER -> "letBinder";
            };
            boolean itemRoot = var.variableKind() == IRVariable.VariableKind.USER_ITEM
                    || var.variableKind() == IRVariable.VariableKind.SYNTHETIC_ITEM;
            if (itemRoot) {
                itemRootMeta = var.type() == null || var.type().isMissing() || var.type().hasMeta();
                if (!itemRootMeta) {
                    // #523 — the itemChain./itemSrcUnprovable. decline gates RETIRED (the guard
                    // co-design): the compiler's post-pin guard now SERVES every guarded
                    // item-rooted arg shape whole-legacy at the RSymbolReference claim root (the
                    // #469 claim-root law — super.visitSymbolReference is the decline's own
                    // render), so the mirror admits exactly the raw HEAD classes whose route is
                    // proven: an RImplicitVariable head at any depth (the explicit chain — the
                    // standing raw-side trip recognizer catches multi-hop, legacy's own
                    // implicitItemArgMeta oracle catches the meta single-hop, and a clean
                    // single-hop is the #469-taught native compose); an REnumValueRef head at
                    // any depth (the single-arrow disguise — the guard's #523 call-branch probe
                    // trips it via legacy's OWN synthesizeImplicitItemChain predicate BY CALL);
                    // a bare RSymbolReference head at hop1 (the #479/#480 proven FieldAccess
                    // native over the walk-typed synthetic item — the parser's single-arrow law
                    // makes every multi-name head an REnumValueRef, so a sym head is
                    // structurally hop1; deeper sym-headed lowerings keep the decline as a
                    // drift face). The former itemSrcUnprovable re-walk was a STALE MIRROR on
                    // synthetic roots (a typed non-meta synItem is typed ONLY by the
                    // prove-or-decline walks — the node is cache-invisible, the #479
                    // cache-boundary law) and is superseded on USER roots by the guard's
                    // legacy-oracle meta read at the serve seat.
                    RExpression head = rawArg;
                    while (head instanceof RFeatureCall fcHop) {
                        head = fcHop.receiver();
                    }
                    itemHeadUnproven = !(head instanceof RImplicitVariable
                            || head instanceof REnumValueRef
                            || (head instanceof RSymbolReference && hops == 1));
                }
            }
            rootAdmits = var.variableKind() == IRVariable.VariableKind.PARAM
                    || (itemRoot && !itemRootMeta && !itemHeadUnproven);
        } else if (walk instanceof IRReference ref
                && ref.referenceKind() == IRReference.ReferenceKind.ALIAS) {
            // #492: RE-SCOPED from a zero-population drift detector to an EXPLICIT decline;
            // #505 arm-B: ADMITTED off its own decode (the #492-named follow-up landed) — the
            // argResidueGate census read the faces (hop1 211 · hop2 108 sole at the #504 SOT,
            // fn-callee dominant, atRoot-heavy) and the render question the #492 comment
            // parked is answered by ROUTING, not composition: an apply carrying an
            // alias-rooted nav arg is a containsOracleLeaf SHAPE leg, so every claim root
            // containing the call renders WHOLE through the oracle-root callArgs serve —
            // legacy's own #360 neutral-compile + evaluate-arg unwrap runs inside that
            // literal line, byte-identical BY IDENTITY (never the emitter's own arg
            // pipeline). The shape was a 100%-decline face pre-teach, so no standing native
            // compose reroutes (the #504 conservation argument). The deeper gates (the
            // hop-type drift flags + the #489 typeGap agreement) stay live below.
            root = "alias";
            rootAdmits = true;
        } else if (walk instanceof IRApply) {
            root = "root:IRApply"; // the #488 generic spelling kept; the root ADMITS (the #484 composition)
            rootAdmits = true;
        } else if (walk instanceof IRSymbolNav || walk instanceof IRMetaAccess) {
            // #510 arm-D1: the one-hop navs over a disguised symbol-receiver head
            // (argNav.root:IRSymbolNav.hop1 — 58 sole at the #509 SOT) or a meta-access
            // head (argNav.root:IRMetaAccess.hop1[.multi] — 52+14 sole), ADMITTED off the
            // #505 alias-root precedent one kind further: both root kinds are standing
            // ORACLE LEAVES (containsOracleLeaf carries them kind-wide), so an apply
            // carrying such an arg ALWAYS renders WHOLE through the oracle-root callArgs
            // serve — legacy's own arg neutral-compile + evaluate-slot unwrap runs inside
            // that literal line, byte-identical BY IDENTITY (never the emitter's own arg
            // pipeline; no new compiler leg is needed, the standing kind walk routes).
            // #522: the #510 hop1-only census gate WIDENED to EVERY depth (the live
            // deeper faces at the #521 SOT: root:IRMetaAccess.hop2 9 + .hop2.multi 4 +
            // .deep 1 drr-f · root:IRSymbolNav.deep 7 drr-r) — the serve is DEPTH-BLIND
            // (the same whole-legacy callArgs line at any hop count) and the #521 probe
            // loop proved the depth landscape TEACH-SENSITIVE (a co-landing typing fix
            // deepens the lowered args, so a census-narrow depth gate chases its own
            // teach — the #511 IRListOp every-depth pattern). The deeper gates (the
            // hop-type drift flags + the #489 typeGap agreement) stay live below.
            root = "root:" + walk.getClass().getSimpleName();
            rootAdmits = true;
        } else if (walk instanceof IROutputRef || walk instanceof IRMetaParamRef) {
            // #514 probe4 teach: the one-hop navs over the wave's own leaf mints
            // (argNav.root:IROutputRef.hop1[.multi] + root:IRMetaParamRef.hop1 — the arms'
            // exposed frontier) ADMIT off the #510 arm-D1 precedent verbatim: both root
            // kinds are ORACLE LEAVES (containsOracleLeaf carries them kind-wide), so an
            // apply carrying such an arg ALWAYS renders WHOLE through the oracle-root
            // callArgs serve — legacy's own arg neutral-compile + evaluate-slot unwrap
            // inside that literal line, byte-identical BY IDENTITY (no new compiler leg —
            // the standing kind walk routes). Census-narrow hop1 only; the deeper
            // spellings stay decline faces, and the deeper gates (the hop-type drift
            // flags + the #489 typeGap agreement) stay live below.
            root = "root:" + walk.getClass().getSimpleName();
            rootAdmits = hops == 1;
        } else if (walk instanceof IRListOp) {
            // #511: the list-op chain root ADMITS at every depth (argNav.root:IRListOp —
            // hop1 31 · hop2 38 · deep 13 · hop1.multi 2 sole at the #510 SOT: navs off a
            // lowered collapse/collection op passed as call arguments — ALL FOUR spellings
            // live, unlike the #510 hop1-only symbol/meta roots). The render is the oracle
            // serve BY IDENTITY at the containing apply: the compiler's callArgs arg-root
            // walk carries the list-op root (the #505 alias-root leg's sibling), and the
            // nav-over-list-op receiver leg independently carries every non-collapse
            // receiver anywhere in the tree — so every depth is the same whole-legacy
            // line and no per-depth render proof is owed. The deeper gates (the hop-type
            // drift flags + the #489 typeGap agreement) stay live below — typeGap residue
            // is the designed remainder, disclosed at the wave.
            root = "root:IRListOp";
            rootAdmits = true;
        } else if (walk instanceof IRDispatchInputRef) {
            // #520: the dispatch-base-input chain root (argNav.root:IRDispatchInputRef —
            // hop1 12 · hop2 6 sole at the #519 SOT: `<baseInput> -> a [-> b]` navs inside
            // dispatch VARIANT bodies passed as call arguments) ADMITS off the #510
            // arm-D1 / #514 probe4 precedent verbatim: the root kind is a standing ORACLE
            // LEAF (containsOracleLeaf carries it kind-wide since #505), so an apply
            // carrying such an arg ALWAYS renders WHOLE through the oracle-root callArgs
            // serve — legacy's own arg neutral-compile + evaluate-slot unwrap inside that
            // literal line, byte-identical BY IDENTITY (no new compiler leg — the
            // standing kind walk routes). Census-narrow to the LIVE depths (hop1 + hop2;
            // the deep spelling stays a decline face — the #510 conservatism), and the
            // deeper gates (the hop-type drift flags + the #489 typeGap agreement) stay
            // live below.
            root = "root:IRDispatchInputRef";
            rootAdmits = hops <= 2;
        } else if (walk instanceof IRClosureParam) {
            // #521: the closure-param chain root (argNav.root:IRClosureParam — hop1 20 ·
            // deep.multi 10 sole at the #520 SOT: navs off a lowered named-lambda closure
            // param passed as call arguments) ADMITS off the #510 arm-D1 / #514 probe4 /
            // #520 dispatch-root precedent: the root kind is a standing ORACLE LEAF
            // (containsOracleLeaf carries it kind-wide since #502), so an apply carrying
            // such an arg ALWAYS renders WHOLE through the oracle-root callArgs serve —
            // legacy's own arg neutral-compile + evaluate-slot unwrap inside that literal
            // line, byte-identical BY IDENTITY (no new compiler leg — the standing kind
            // walk routes). EVERY depth admits (the #511 IRListOp pattern): the serve is
            // depth-blind (the same whole-legacy line at any hop count), and the depth
            // landscape proved TEACH-SENSITIVE at the #521 probe loop — the base typing
            // deepened the lowered args (the #520-SOT hop1 faces re-read as hop2 once the
            // outer hops could lower), so a census-narrow depth gate would chase its own
            // teach. The deeper gates (the hop-type drift flags + the #489 typeGap
            // agreement) stay live below.
            root = "root:IRClosureParam";
            rootAdmits = true;
        } else if (walk instanceof IRChoiceOptionNav || walk instanceof IRSynItemNav
                || walk instanceof IRMetaItemNav || walk instanceof IRDeepFeatureNav) {
            // #522: the remaining standing-oracle-leaf chain roots ADMIT — the
            // #510-D1/#514/#520/#521 oracle-leaf-root pattern's FIFTH application, at
            // the SEVENTH through TENTH root kinds (the live faces at the #521 SOT:
            // root:IRDeepFeatureNav.hop1.multi 12 · root:IRChoiceOptionNav.hop1 3 ·
            // root:IRSynItemNav.hop1.multi 3 + .hop2 1 + .hop2.multi 1 ·
            // root:IRMetaItemNav.hop2 1 — all cdm6/drr-f). EVERY root kind here is a
            // standing kind-wide containsOracleLeaf entry (IRSynItemNav #504 ·
            // IRChoiceOptionNav #505 · IRDeepFeatureNav #507 · IRMetaItemNav #508), so
            // an apply carrying such an arg ALWAYS renders WHOLE through the
            // oracle-root callArgs serve — legacy's own arg neutral-compile +
            // evaluate-slot unwrap inside that literal line, byte-identical BY
            // IDENTITY (no new compiler leg — the standing kind walk routes). EVERY
            // depth admits (the #511/#521 pattern): the serve is depth-blind and the
            // #522 wave co-lands the by-name bare-item teach, whose new lowerings can
            // root exactly these kinds — a census-narrow depth gate would chase its
            // own teach (the #521 teach-sensitive-depth law). The deeper gates (the
            // hop-type drift flags + the #489 typeGap agreement) stay live below.
            root = "root:" + walk.getClass().getSimpleName();
            rootAdmits = true;
        } else {
            root = "root:" + walk.getClass().getSimpleName();
            rootAdmits = false;
        }
        // #491 — the #489 Seat-1 OBS-2 ROOT-TYPE BELT: the hop walk above reads hop RESULT types
        // only, so a meta/missing-typed ROOT under an otherwise-ADMITTING root kind (param /
        // root:IRApply) was excluded only INDIRECTLY (adaptSymbolReference declines meta inputs;
        // calleeMetaOutput declines meta-output callees). The belt makes the exclusion DIRECT at
        // the classifier: the admitting root's OWN type joins the #488 drift flags (population
        // ZERO by the upstream gates — the ring is the backstop). Gated on rootAdmits so no
        // decline face re-spells (an admitting item root is already proven clean-typed by
        // !itemRootMeta, where the belt is vacuous by construction).
        if (rootAdmits && walk != null) {
            if (walk.type() == null || walk.type().isMissing()) {
                typeMissing = true;
            } else if (walk.type().hasMeta()) {
                meta = true;
            }
        }
        String depth = hops == 1 ? "hop1" : hops == 2 ? "hop2" : "deep";
        String tail = root + "." + depth
                + (nav.cardinality() == ExpressionCardinality.MULTI ? ".multi" : "");
        if (typeMissing || meta) {
            // #527: a PURE missing-typed read on a PARAM-rooted chain ADMITS — the missing-hop
            // serve class (typeMissing.param.deep[.multi] — 10 sole at the #526 SOT, the drr
            // RULE seam entire). The MISSING sentinel makes both the agreement gate below and
            // the native witness render unreadable BY CONSTRUCTION, so the compiler's
            // argNavMissingHop belt serves every claim whose lowered tree carries such a chain
            // whole-legacy at the claim root (super.visitSymbolReference — byte-identical BY
            // IDENTITY; the #527 argGapGate census read legacy's OWN channel javaEq/multiParam
            // on the whole population, so legacy renders the flat raw pass inside the serve).
            // A meta flag anywhere keeps the decline (the #326 Type-coercion deref class), and
            // every other root keeps the standing drift faces — census-narrow, the admit
            // bypasses the unreadable agreement gate directly.
            if (typeMissing && !meta && "param".equals(root)) {
                return null; // ADMITTED — the compiler-side missing-hop serve claims the chain
            }
            return (typeMissing ? "typeMissing." : "meta.") + tail; // the #488 drift faces + the #491 root belt
        }
        if (!rootAdmits) {
            if (itemRootMeta) {
                return "itemRootMeta." + tail; // the metaItemReceiver family's stamped-type channel
            }
            if (itemHeadUnproven) {
                // #523: the residue drift face — an item-rooted arg whose raw HEAD class is
                // outside the proven-route set (not an implicit var, not the single-arrow
                // disguise, not a hop1 bare symbol). Expected ZERO at population: the parser's
                // single-arrow law makes every multi-name head an REnumValueRef, so this token
                // firing means a NEW head class reached the arg seat — the #486 stable-token
                // convention. The former itemChain./itemSrcUnprovable. spellings retired with
                // the #523 admission (the guard co-design serves their whole populations).
                return "itemRawHead." + tail;
            }
            return tail; // the #488 root decline spellings, unchanged
        }
        // #527: the agreement gate is SKIPPED for ORACLE-LEAF chain roots — such a chain makes
        // every containing claim root an oracle-root shape (containsOracleLeaf carries the root
        // kind), so the claim renders WHOLE through the standing oracle-root serves: legacy's
        // own evaluate-arg machinery runs inside that literal line, and any coercion is
        // legacy's own — byte-identical BY IDENTITY, the type pair moot (the #526-SOT faces
        // typeGap.root:IRSymbolNav.hop1.g:int_number 8 + typeGap.alias.hop1.g:DateOffset_Offset
        // 1, the #510-D1/#505 comments' "typeGap agreement kept live" conservatism flipped off
        // the #527 argGapGate census — every carrier's route is a standing whole-legacy serve).
        if (isOracleLeafRootKind(walk)) {
            return null; // ADMITTED pair-blind — the oracle-root serve carries any legacy coercion
        }
        // #489: the type-agreement gate — legacy passes a nav arg RAW only on Java-type equality
        // (the guarded-coercion precondition); R-type equality is the sufficient adapter mirror.
        RMetaAnnotatedType paramType = param == null ? null : ws.getInferredAttributeType(param);
        boolean rTypeEqual = paramType != null && !paramType.isMissing() && nav.type() != null
                && nav.type().type() != null && nav.type().type().equals(paramType.type());
        // #514 ARM-4: the JAVA-equality widening of the #489 mirror — the decode read the
        // dominant gap class 100% `g:number_number` (constraint-differing NON-INTEGER number
        // pairs: RNumberType.equals compares digits/fractionalDigits/min/max, but every
        // non-integer number maps to BigDecimal, so legacy's `paramJavaType.equals(
        // actualItemType)` precondition is TRUE and the arg passes RAW — no hoist fires).
        // Census-narrow: BOTH sides non-integer RNumberType only (an int side maps to
        // int/long/BigInteger BY DIGITS — JavaTypeTranslator.caseNumberType — so the
        // `g:int_number` faces are the genuine coercion class at a NATIVE root and keep to
        // the #527 serve/decline split below).
        boolean numberJavaEqual = !rTypeEqual && paramType != null && !paramType.isMissing()
                && nav.type() != null && nav.type().type() instanceof RNumberType navNum
                && !navNum.isInteger()
                && paramType.type() instanceof RNumberType paramNum && !paramNum.isInteger();
        // #527: the ARMS-DEAD widening — the argGapGate census (probe0, the #526 SOT) read
        // legacy's OWN evaluate-arg preconditions BY CALL on the whole 52-claim pool: a
        // coercion fires ONLY on a numeric/meta-wrapper ACTUAL into a BigDecimal/meta-free
        // param (ReferenceHandler.tryMetaDerefArg's three arms), so
        //   (1) a MODEL-classed chain result (RDataTypeRef) can NEVER coerce — the arms need
        //       numeric/meta actuals, the chain is meta-free by declaration (the hop flags
        //       above), and the census read rawNoArm/paramUnread on every model-pair carrier
        //       (StrikeSchedule/SpreadSchedule/FxSwapLeg/Stub/DateOffset/TradableProduct/
        //       OptionPayout — legacy raw-passes at its own gates, param side irrelevant);
        //   (2) an INT-family chain into a param that is NOT a non-integer number has no live
        //       arm either — only a BigDecimal param fires the #277/#128 hoists, an int-family
        //       param maps to Integer/Long/BigInteger BY DIGITS, never BigDecimal (the
        //       g:int_int carriers' javaEq census verdicts; the #514 MF-3 Java-equality wall
        //       dissolves because the raw-pass verdict needs no equality proof).
        // Both are R-read-provable raw-passes, and the flat native arg render is
        // param-type-BLIND (the #489 neutral-compile ctx + chain-derived witnesses), so the
        // native render is byte-identical BY the source read — admit natively.
        boolean navInt = nav.type() != null && nav.type().type() instanceof RNumberType n
                && n.isInteger();
        boolean modelActualRawPass = nav.type() != null
                && nav.type().type() instanceof RDataTypeRef;
        boolean paramNonIntNumber = paramType != null && !paramType.isMissing()
                && paramType.type() instanceof RNumberType pNum && !pNum.isInteger();
        boolean intoNonDecimalParam = navInt && paramType != null && !paramType.isMissing()
                && paramType.type() != null && !paramNonIntNumber;
        // #527: the SERVE class — an int-family chain into a non-integer-number param IS
        // legacy's Integer→BigDecimal null-guarded hoist (a statement hoist the recursion-free
        // native arm cannot express), admitted census-narrow at the synItem.deep seat ONLY:
        // a ≥3-hop item-rooted chain trips the standing post-pin scan's MULTI_HOP_ITEM_NAV
        // arm, so every containing claim serves whole-legacy at the #523 claim-root leg /
        // the kind-gated root-site renderers (the census's scan:multiHopItemNav column on all
        // 15 carriers) — the coercion is legacy's own inside the serve. Other native roots
        // (param/item/hop1/hop2 synItem) keep the typeGap face — the honest drift residue: no
        // standing serve catches them, and a native render would drop the hoist.
        boolean synItemDeepServe = navInt && paramNonIntNumber
                && "synItem".equals(root) && hops >= 3;
        if (!rTypeEqual && !numberJavaEqual && !modelActualRawPass && !intoNonDecimalParam
                && !synItemDeepServe) {
            // #514: the face refined IN PLACE (the #491 pattern): the gap CLASS rides as a
            // suffix — the chain's result R-type name vs the callee param's declared R-type
            // name (absent sides spelled `null`) — the coercion-family split the residue
            // sweep's design needs (numeric-widening vs model-subtype vs unreadable).
            String navT = nav.type() == null || nav.type().type() == null ? "null"
                    : nav.type().type().name();
            String paramT = paramType == null || paramType.isMissing()
                    || paramType.type() == null ? "null" : paramType.type().name();
            return "typeGap." + tail + ".g:" + navT + "_" + paramT;
        }
        return null; // ADMITTED — the L-042 revival claims the chain
    }

    /**
     * #527 — the ORACLE-LEAF chain-root screen for {@link #argNavFacet}'s agreement-gate skip:
     * exactly the classifier's ADMITTING root arms whose kinds are standing oracle leaves in
     * the compiler's {@code containsOracleLeaf} walk (the #505 alias root · the #510-D1
     * symbol/meta-access roots · the #514 output/meta-param roots · the #511 list-op root ·
     * the #520 dispatch-input root · the #521 closure-param root · the #522 four nav roots).
     * A chain over such a root makes every containing claim an oracle-root shape, so the claim
     * renders whole-legacy BY IDENTITY and the #489 type pair cannot matter. Deliberately a
     * SUBSET of the compiler's serve coverage AT THE ARG SEAT (the drift-safe direction: a
     * kind missing here keeps its typeGap face — an honest un-taught decline, never a byte
     * risk; a kind listed here but uncovered compiler-side would be the unsafe direction).
     * Ten of the twelve kinds are kind-wide {@code containsOracleLeaf} entries (#500–#525);
     * {@code IRListOp} (carried SUM-scoped in the kind walk) and the ALIAS reference ride the
     * compiler's IRApply ARG-CHAIN-ROOT leg instead — the #505/#511 legs that carry BOTH
     * root-wide at exactly the call-argument seat this classifier gates (the Seat-1 #527
     * OBS-2 scope note: this screen is ARG-SEAT-specific, not a general leaf predicate), with
     * the ON ring as the standing oracle.
     */
    private static boolean isOracleLeafRootKind(IRExpr walk) {
        return walk instanceof IRSymbolNav || walk instanceof IRMetaAccess
                || walk instanceof IROutputRef || walk instanceof IRMetaParamRef
                || walk instanceof IRListOp || walk instanceof IRDispatchInputRef
                || walk instanceof IRClosureParam || walk instanceof IRChoiceOptionNav
                || walk instanceof IRSynItemNav || walk instanceof IRMetaItemNav
                || walk instanceof IRDeepFeatureNav
                || (walk instanceof IRReference ref
                        && ref.referenceKind() == IRReference.ReferenceKind.ALIAS);
    }

    /**
     * Mirror of {@link #adaptEnumValueRef}'s compound gate ({@link #isGenuineEnumValueRef}),
     * naming the dominant channel in {@code ExpressionTypeComputer.computeEnumValueRef}'s own
     * resolution precedence: {@code attributeChain} → the {@code inputFeatureNav.*} family (the
     * L-111 typing-only channel — since the #478 arm the channel LOWERS through its
     * legacy-equivalent feature call, so a decliner names the equivalent's own first failing gate
     * dot-suffixed onto the channel token, e.g. {@code inputFeatureNav.featureUnresolved} for an
     * alias/unresolvable head or {@code inputFeatureNav.metaFeature}) → {@code choiceOption} →
     * {@code typeRestriction} → then the enumeration-absent shapes ({@code headSymbolNav} when the
     * head resolved to a callable symbol — the record-field navigation, plus the rare meta read
     * through a called symbol's meta output — else {@code noResolutionChannel}, which covers the
     * DIRECT disguised meta-feature read [no channel binds, #433] and the genuinely unresolved) →
     * {@code valueNameFallback}
     * (enumeration present, {@code enumValue()} absent — legacy renders from the raw
     * {@code valueName()} string, a render this adapter deliberately does not claim).
     */
    private String reasonForEnumValueRef(REnumValueRef enr, RWorkspace ws) {
        if (enr.resolvedAttributeChain().isPresent()) {
            // #480: the chain arm's twin — a still-declining bound chain names the arm's own
            // first failing gate dot-suffixed onto the channel token (the #478 pattern); the bare
            // sentinel surfaces UNWRAPPED for the unattributed meter (twin-drift only).
            String gate = implicitItemChainDeclineGate(enr, ws);
            return UNATTRIBUTED.equals(gate) ? UNATTRIBUTED : "attributeChain." + gate;
        }
        if (enr.resolvedInputFeature().isPresent()) {
            // #478: the arm's twin — the SAME equivalent the arm adapts (the shared builder + the
            // shared head resolution), its first failing gate named by the feature-call mirror;
            // when the equivalent itself lowers, the arm's remaining exits are the RETYPE gates
            // (the cache boundary — see adaptDisguisedInputNav), restated here in the arm's own
            // order. The bare sentinel surfaces UNWRAPPED so the unattributed meter's
            // ":unattributed" suffix key still fires (reached only if the twins ever drift).
            if (enr.enumName() == null || enr.enumName().isEmpty()
                    || enr.valueName() == null || enr.valueName().isEmpty()) {
                // The arm's own entry gate, restated FIRST (the Seat-1 OBS-1 / Copilot R2 class):
                // resolveNameInFunction name-matches with equals, so a partially-built node with a
                // null name must exit here with a named token, never an NPE out of declineReason.
                return "inputFeatureNav.nameMissing";
            }
            RFunction enclosing = enclosingFunction(enr);
            RAttribute head = enclosing == null ? null : resolveNameInFunction(enclosing, enr.enumName());
            if (enclosing == null) {
                head = conditionOwnerAttribute(enr, enr.enumName()); // #640 arm C - mirrors the arm
            }
            String sub = reasonForFeatureCall(disguisedInputNavEquivalent(enr, head), ws);
            if (!UNATTRIBUTED.equals(sub)) {
                return "inputFeatureNav." + sub;
            }
            // #507 — the arm's SHAPE exit restated (the l111Residue census's unattributed-119
            // decode: the mirror had NO twin for adaptDisguisedInputNav's `!(FieldAccess over
            // IRVariable)` defensive exit, so the whole class read unattributed). The arm's own
            // predicate re-run: a META single-hop equivalent LOWERS via the #499 IRMetaAccess
            // arm — the documented metaFeature token minted at this seat at #507; any other
            // off-shape lowering names its form (the defensive belt, zero live rows).
            Optional<IRExpr> shapeProbe = adaptFeatureCall(disguisedInputNavEquivalent(enr, head),
                    NodeId.ROOT, ws);
            if (shapeProbe.isPresent() && shapeProbe.get() instanceof FieldAccess condProbe
                    && condProbe.receiver() instanceof IRImplicitAttrNav) {
                return UNATTRIBUTED; // #640 arm C: the condition-attribute head arm claims it - mirrors the arm
            }
            if (shapeProbe.isPresent()
                    && !(shapeProbe.get() instanceof FieldAccess sfa
                            && sfa.receiver() instanceof IRVariable)) {
                if (shapeProbe.get() instanceof IRMetaAccess probeMeta) {
                    // #508 arm-A1: the arm ACCEPTS the IRVariable-receiver meta single-hop
                    // (the whole live class — the metaHopGate census read 100%
                    // recv:IRVariable), so the accepted shape reads UNATTRIBUTED (twin drift
                    // only); the token stays for the OFF-receiver meta residue (the
                    // defensive belt — census-zero, the #486 stable-token precedent).
                    // #517 arm-B2 twin: the DISPATCH-INPUT receiver joins the acceptance
                    // (the metaFeature.q:scheme residue's whole class), so the token is
                    // now a two-kind defensive belt.
                    if (probeMeta.receiver() instanceof IRVariable
                            || probeMeta.receiver() instanceof IRDispatchInputRef
                            || probeMeta.receiver() instanceof IRImplicitAttrNav) {
                        return UNATTRIBUTED; // the #508 arm-A1 / #517 arm-B2 / #640 arm-F acceptances claim
                    }
                    return "inputFeatureNav.metaFeature";
                }
                // #514 ARM-1b/3b twin: the OUTPUT-head and META-PARAM-head single hops now
                // CLAIM through the arm's oracle-leaf acceptance leg (typed — the arm's
                // whole gate); the untyped residue keeps a named face. #517 arm-B1 twin:
                // the DISPATCH-INPUT head joins the acceptance — the
                // offShape.fa:IRDispatchInputRef spelling becomes a DEFENSIVE BELT
                // (census-zero; a row reappearing means the acceptance regressed), and the
                // offShape token keeps naming any future un-accepted receiver kind.
                if (shapeProbe.get() instanceof FieldAccess leafFa
                        && (leafFa.receiver() instanceof IROutputRef
                                || leafFa.receiver() instanceof IRMetaParamRef
                                || leafFa.receiver() instanceof IRDispatchInputRef)) {
                    if (leafFa.receiver() instanceof IRDispatchInputRef) {
                        // #517 arm-B1 twin: the dispatch-head acceptance carries the
                        // FEATURE-channel type fallback — the arm declines only when BOTH
                        // channels are blind (node untyped AND hop untyped; the hop's own
                        // type is present by adaptFeatureCall's gates, so the accepted
                        // class reads UNATTRIBUTED whole).
                        RMetaAnnotatedType dispatchNodeT = type(enr, ws);
                        boolean dispatchNodeTyped = dispatchNodeT != null
                                && !dispatchNodeT.isMissing();
                        boolean dispatchHopTyped = leafFa.type() != null
                                && !leafFa.type().isMissing();
                        return dispatchNodeTyped || dispatchHopTyped ? UNATTRIBUTED
                                : "inputFeatureNav.leafHop.untyped";
                    }
                    RMetaAnnotatedType lhType = type(enr, ws);
                    if (lhType == null || lhType.isMissing()) {
                        return "inputFeatureNav.leafHop.untyped";
                    }
                    return UNATTRIBUTED; // the #514 arm-1b/3b acceptance claims — twin drift only
                }
                // A FieldAccess off-shape names its RECEIVER kind (the fact the shape gate
                // reads — the live class is fa:IRDispatchInputRef, the dispatch-scope
                // heads whose equivalents lower over the #505 kind; the Seat-1 MF-1 recut
                // of the hypothetical example that leaked into the #507 narration).
                String form = shapeProbe.get() instanceof FieldAccess offFa
                        ? "fa:" + offFa.receiver().getClass().getSimpleName()
                        : shapeProbe.get().getClass().getSimpleName();
                return "inputFeatureNav.offShape." + form;
            }
            RMetaAnnotatedType headType = head == null ? null : ws.getInferredAttributeType(head);
            if (headType == null || headType.isMissing()) {
                return "inputFeatureNav.headTypeMissing";
            }
            RMetaAnnotatedType navType = type(enr, ws);
            if (navType == null || navType.isMissing()) {
                return "inputFeatureNav.navTypeMissing";
            }
            return UNATTRIBUTED;
        }
        if (enr.resolvedChoiceOption().isPresent()) {
            // #505 arm-A2: the arm's exits restated (the twin discipline) — names + typing
            // claim through the IRChoiceOptionNav mint; the residue faces name the arm's own
            // gates.
            if (enr.enumName() == null || enr.enumName().isEmpty()
                    || enr.valueName() == null || enr.valueName().isEmpty()) {
                return "choiceOption.nameMissing";
            }
            RMetaAnnotatedType optType = type(enr, ws);
            if (optType == null || optType.isMissing()) {
                return "choiceOption.untyped";
            }
            return UNATTRIBUTED; // the arm claims — reachable from the mirror only on twin drift
        }
        if (enr.resolvedTypeRestriction().isPresent()) {
            return "typeRestriction";
        }
        if (enr.enumeration().isEmpty()) {
            if (enr.resolvedSymbol().isPresent()) {
                return "headSymbolNav";
            }
            // #507 arm-B: the token refined IN PLACE (the #486 symbolUnresolved precedent) —
            // the record arm claims the record-read shapes, so the residue names the arm's own
            // first failing gate dot-suffixed onto the channel token; the bare sentinel
            // surfaces UNWRAPPED for the unattributed meter (twin-drift only).
            String gate = recordArmDeclineGate(enr, ws);
            return UNATTRIBUTED.equals(gate) ? UNATTRIBUTED : "noResolutionChannel." + gate;
        }
        if (enr.enumValue().isEmpty()) {
            return "valueNameFallback";
        }
        return UNATTRIBUTED;
    }

    /**
     * The #507 record arm's decline mirror ({@link #adaptRecordFeatureNav}'s exits restated in
     * order with the arm's OWN helpers — the twin discipline): {@code shape} (a written segment
     * missing), {@code headMiss} (nothing in the enclosing function's scope nor the
     * dispatch-base join names the head — the honest residue: the census's lambda-scope meta
     * reads), {@code headNotRecord} (the head's declared type is not a record), {@code
     * leafMiss} (the leaf is not one of the record's own features). A node passing every gate
     * reads {@link #UNATTRIBUTED} (the arm claims — reachable from the mirror only on twin
     * drift). The channel guards are the caller's own branch conditions (every channel already
     * checked empty before the {@code noResolutionChannel} exit), so this mirror restates only
     * the arm's downstream ladder.
     */
    private String recordArmDeclineGate(REnumValueRef enr, RWorkspace ws) {
        if (enr.enumName() == null || enr.enumName().isEmpty()
                || enr.valueName() == null || enr.valueName().isEmpty()) {
            return "shape";
        }
        RFunction enclosing = enclosingFunction(enr);
        RAttribute head = enclosing == null ? null : resolveNameInFunction(enclosing, enr.enumName());
        if (head == null) {
            head = dispatchBaseInputNamed(enr, enr.enumName());
        }
        if (head == null) {
            // #514: the flat token refined IN PLACE (the #491 pattern): the census's
            // lambda-scope reads split by the CLOSURE-PARAM channel — a cp-named head
            // walks the #505 element derivation and the leaf ladder sizes the teach
            // directly (elemNull / leafMiss / leafMeta / leafPlain.<typed|untyped>);
            // the non-cp residue stays flat under `.other`.
            if (isEnclosingClosureParamName(enr, enr.enumName())) {
                RDataType hmElem = closureParamElementType(enr, enr.enumName());
                if (hmElem == null) {
                    return "headMiss.cp.elemNull";
                }
                RAttribute hmLeaf = findAttributeOnDataTypeByName(hmElem, enr.valueName());
                if (hmLeaf == null) {
                    return "headMiss.cp.leafMiss";
                }
                if (isMetaAnnotated(hmLeaf)) {
                    return "headMiss.cp.leafMeta";
                }
                RMetaAnnotatedType hmType = type(enr, ws);
                return hmType == null || hmType.isMissing()
                        ? "headMiss.cp.leafPlain.untyped" : "headMiss.cp.leafPlain.typed";
            }
            // #529: the implicit-item head arm's twin — the SAME equivalent re-adapt the
            // arm runs (adaptEnrItemHeadNav's census-narrow accept BY CALL; the #478
            // witness convention — the equivalent is a fresh throwaway construction). A
            // claimed shape reads UNATTRIBUTED (the arm claims — reachable from the
            // mirror only on twin drift); the residue keeps the honest spelling. The
            // re-adapt leg lives in THIS headMiss branch only while the arm runs on
            // every record-arm decline (the Seat-1 #529 OBS-4 asymmetry): the twins
            // agree on every reachable shape TODAY because the #524 ladder's
            // function-scope gate declines exactly the heads that reach the
            // headNotRecord/leafMiss exits below — if that gate ever changes, this
            // leg must move ahead of the whole ladder.
            Optional<IRExpr> itemHeadLowered =
                    adaptFeatureCall(enrBareItemNavEquivalent(enr), NodeId.ROOT, ws);
            if (itemHeadLowered.isPresent()
                    && (itemHeadLowered.get() instanceof IRQualifierReceiverNav
                            || itemHeadLowered.get() instanceof IRRecordReceiverNav)) {
                return UNATTRIBUTED;
            }
            return "headMiss.other";
        }
        RRecordType record = declaredRecordTypeOf(head);
        if (record == null) {
            return "headNotRecord";
        }
        for (RRecordFeature f : record.features()) {
            if (enr.valueName().equals(f.name())) {
                return UNATTRIBUTED; // the arm claims this shape — twin drift only
            }
        }
        return "leafMiss";
    }

    /**
     * Mirror of {@link #adaptFeatureCall}'s exits, in order; a receiver that lowers but fails the
     * navigability gate is named by its lowered form via {@link #receiverToken} (the
     * {@code receiverAlias.*} faces are the alias-receiver navigation — the L-032
     * shortcut-type-inference gap's live face, body-classified since #492), with the
     * filter/extract {@code item} sub-gates mirrored individually; an unresolved feature is
     * head-decoded via {@link #featureUnresolvedFacet} (#492).
     */
    private String reasonForFeatureCall(RFeatureCall fc, RWorkspace ws) {
        RExpression rawReceiver = fc.receiver();
        if (rawReceiver == null) {
            return "elidedReceiver";
        }
        RAttribute feature = fc.resolvedFeature().orElse(null);
        if (feature == null) {
            // #503 arm-B2: the twin restates the closure-param-head by-name resolution — a
            // head passing the arm's identity + both source proofs + the member lookup reads
            // UNATTRIBUTED (the arm claims; reachable from the mirror only on twin drift).
            // #509 arm-B2: the element derivation widened IN LOCKSTEP with the arm — the
            // UNPROVEN-source structural walk resolves the element when the strong allowlist
            // declines (the oracle-leaf routing makes the render byte-safe regardless).
            if (fc.receiver() instanceof RSymbolReference head && head.name() != null
                    && fc.featureName() != null && isClosureParamHead(head)) {
                RDataType elem = closureParamElementType(head, head.name());
                if (elem == null) {
                    RExpression unprovenSrc = closureParamBinderSource(head, head.name());
                    elem = unprovenSrc == null ? null : sourceElementDataType(unprovenSrc);
                }
                if (elem != null
                        && findAttributeOnDataTypeByName(elem, fc.featureName()) != null) {
                    return UNATTRIBUTED;
                }
            }
            // #503 arm-B1: the twin restates the choice-option resolution — a receiver whose
            // cached type is a choice with a matching projected option reads UNATTRIBUTED.
            // #509 arm-B4: the CHOICE-SUPERTYPE twin — a DATA-typed receiver whose ancestor
            // walk carries the type-named option reads UNATTRIBUTED (the arm claims via the
            // findChoiceSuperOption mirror, the PR #207 legacy lever).
            if (fc.receiver() != null && fc.featureName() != null && ws != null) {
                RMetaAnnotatedType recvType = type(fc.receiver(), ws);
                if (recvType != null && !recvType.isMissing()
                        && recvType.type() instanceof RChoiceTypeRef choiceRef) {
                    RDataType projected = choiceRef.asRDataType();
                    if (projected != null
                            && findAttributeOnDataTypeByName(projected, fc.featureName()) != null) {
                        return UNATTRIBUTED;
                    }
                }
                if (recvType != null && !recvType.isMissing()
                        && recvType.type() instanceof RDataTypeRef recvDataRef
                        && findChoiceSuperOption(recvDataRef.astNode(), fc.featureName())
                                != null) {
                    return UNATTRIBUTED;
                }
            }
            // #505 arm-C: the twin restates the dispatch-base-input head recovery — a
            // variant-body nav whose head names a data-typed base input with the feature
            // resolving by name reads UNATTRIBUTED (the arm claims; the record-typed heads
            // — the baseInput:date slice — CLAIMED at #509 by the record arm below).
            if (fc.receiver() instanceof RSymbolReference dHead && dHead.symbol().isEmpty()
                    && fc.featureName() != null) {
                RAttribute baseInput = dispatchBaseInputOf(dHead);
                RDataType headData = baseInput == null ? null : declaredDataTypeOf(baseInput);
                if (headData != null
                        && findAttributeOnDataTypeByName(headData, fc.featureName()) != null) {
                    return UNATTRIBUTED;
                }
            }
            // #509 arm-B1: the twin restates the parsed-seat record-feature arm — the head
            // through the #507 core (bound attribute / resolveNameInFunction /
            // dispatchBaseInputNamed), a RECORD declared type and the leaf on the record's
            // OWN feature list reads UNATTRIBUTED (the arm mints IRRecordFeatureNav).
            if (fc.receiver() instanceof RSymbolReference recHead && recHead.name() != null
                    && fc.featureName() != null) {
                RAttribute recHeadAttr =
                        recHead.symbol().orElse(null) instanceof RAttribute bound ? bound
                                : null;
                if (recHeadAttr == null && recHead.symbol().isEmpty()) {
                    RFunction recEnclosing = enclosingFunction(fc);
                    recHeadAttr = recEnclosing == null ? null
                            : resolveNameInFunction(recEnclosing, recHead.name());
                    if (recHeadAttr == null) {
                        recHeadAttr = dispatchBaseInputNamed(recHead, recHead.name());
                    }
                }
                RRecordType record = declaredRecordTypeOf(recHeadAttr);
                if (record != null) {
                    for (RRecordFeature f : record.features()) {
                        if (fc.featureName().equals(f.name())) {
                            return UNATTRIBUTED;
                        }
                    }
                }
            }
            // #508 arm-B1/arm-A2 twin: the ALIAS-head recovery claims at this seat (the shared
            // #492 core lowers the plain AND meta legs over the body-retyped alias receiver;
            // the PLAIN leg additionally carries the strong body allowlist — the arm's own
            // probe2-catch guard restated) — the arm's own gates re-run via the SAME dual
            // head path; a claiming shape reads UNATTRIBUTED (twin drift only) and the
            // residue keeps the composed spellings.
            if (fc.receiver() instanceof RSymbolReference aliasHead && fc.featureName() != null) {
                RShortcut recShortcut =
                        aliasHead.symbol().orElse(null) instanceof RShortcut bound ? bound
                                : aliasHead.symbol().isEmpty()
                                        ? shortcutByName(enclosingFunction(fc), aliasHead.name())
                                        : null;
                if (recShortcut != null) {
                    Optional<IRExpr> twinNav = aliasHeadNavLowering(recShortcut,
                            enclosingFunction(fc), aliasHead.name(), fc.featureName(), fc,
                            NodeId.ROOT, ws);
                    if (twinNav.isPresent() && twinNav.get() instanceof FieldAccess
                            && !isProvablyNonMetaElementSource(recShortcut.expression())) {
                        twinNav = Optional.empty();
                    }
                    if (twinNav.isPresent()) {
                        return UNATTRIBUTED;
                    }
                }
            }
            // #492: the flat token refined IN PLACE — the head class first (alias heads are the
            // L-032 population), then the shared body/feature walk sizes the claim-shaped slice
            // (aliasHead.bodyTyped.featureOnBody) apart from the honest residue faces.
            return "featureUnresolved." + featureUnresolvedFacet(fc, ws);
        }
        if (isMetaAnnotated(feature)) {
            // #499: the meta arm's exits restated in order (the twin discipline) — the arm
            // lowers whenever the receiver lowers, so the flat token is refined IN PLACE to the
            // residue face `metaFeature.recvBlocked` (the belt-kept class; the #480 composed-
            // token convention) and a lowering receiver reads UNATTRIBUTED (the arm claims).
            // The child-semantics gate mirrors the arm's child-NodeId adapt (the #496 OBS-1
            // position-divergence class).
            return slotNotExpressibleAtChild(rawReceiver, ws) || adapt(rawReceiver, ws).isEmpty()
                    ? "metaFeature.recvBlocked" : UNATTRIBUTED;
        }
        // #496 (Seat-1 OBS-1): the child-semantics gate FIRST — the arm adapts the receiver at a
        // child NodeId, so a position-divergent receiver (the claim-root admission classes)
        // attributes here; past it, the root re-adapt below is present by the same verdict and
        // feeds the navigability gates their lowered form as before.
        // #514: the flat token refined IN PLACE at BOTH exits (the #491 pattern): the
        // receiver's OWN first failing reason (capped at two segments — reasonHead) names
        // the class a receiver widening must serve; the child-semantics exit keeps its own
        // position marker (a root-lowerable receiver reads `child.recv:unattributed` — the
        // position-divergence class BY CONSTRUCTION).
        if (slotNotExpressibleAtChild(rawReceiver, ws)) {
            return "receiverNotExpressible.child.recv:" + reasonHead(reasonFor(rawReceiver, ws));
        }
        Optional<IRExpr> receiver = adapt(rawReceiver, ws);
        if (receiver.isEmpty()) {
            return "receiverNotExpressible.recv:" + reasonHead(reasonFor(rawReceiver, ws));
        }
        if (!(isNavigableReceiver(receiver.get())
                || isFilterExtractBoundItemReceiver(receiver.get(), rawReceiver)
                // #479: the arm's retype admission, restated with the arm's OWN helper — a
                // synthetic item inside the taught slice lowers, so it must not reach the token.
                || retypedSyntheticFilterExtractItem(receiver.get(), rawReceiver, ws) != null
                // #492: the alias-receiver retype admission, restated with the arm's own helper —
                // an admitted alias receiver lowers, so only the decline faces reach the token.
                || retypedAliasNavReceiver(receiver.get(), rawReceiver, ws) != null
                // #504 arm-B: the mint's admission restated — an un-retypeable SYNTHETIC-item
                // receiver claims UNCONDITIONALLY (the IRSynItemNav mint, same featureName
                // gate), so the receiverSyntheticItem token is now a drift face at its seat.
                || (receiver.get() instanceof IRVariable synVar
                        && synVar.variableKind() == IRVariable.VariableKind.SYNTHETIC_ITEM
                        && fc.featureName() != null)
                // #505: the shallow oracle-leaf receiver admissions restated — a lowered
                // dispatch-input or choice-option base composes (the containing root
                // oracle-serves), so the generic receiver:<Kind> tokens are drift faces at
                // this seat.
                || receiver.get() instanceof IRDispatchInputRef
                || receiver.get() instanceof IRChoiceOptionNav
                // #507: the deep-path nav and record-feature read admissions restated —
                // the same drift-face law at this seat. (#514: the IROutputRef/IRMetaParamRef
                // bases need NO restatement here — isNavigableReceiver itself carries both
                // kinds, so the first disjunct covers arm and twin through ONE predicate.)
                || receiver.get() instanceof IRDeepFeatureNav
                || receiver.get() instanceof IRRecordFeatureNav)) {
            return receiverToken(receiver.get(), rawReceiver, ws);
        }
        return UNATTRIBUTED;
    }

    /**
     * #528 arm-5 — the L-113 conversion's CENSUS-NARROW gate: whether a
     * {@link #bareAttrItemNavDeclineGate} verdict names one of the three walk families the
     * #497 {@code implicitAttrRoot} census reads on the belt's live population — the classes
     * where the arm cannot BUILD an equivalent navigation at all ({@code shortcutCollision}:
     * the name collides with an enclosing shortcut, so legacy renders an alias invocation;
     * {@code noFilterExtractBinder}: no binder walk reaches a source; {@code
     * sourceElementUnresolved}: the binder source's element type does not resolve). Exactly
     * these mint {@link IRImplicitAttrNav} — at the attr-outside-function leg, typed (the
     * mint site's paired guards); every other verdict keeps its standing defer (the
     * identity-guard residue, the #514 rule-input ladder's own faces and any equivalent-nav
     * sub-reason — see the mint site). Prefix-matched because two of the three carry decode
     * facets ({@code sourceElementUnresolved.<src>} — the #513/#514 deepenings).
     */
    private static boolean isImplicitAttrMintFamily(String gate) {
        return gate != null
                && (gate.equals("shortcutCollision")
                        || gate.equals("noFilterExtractBinder")
                        || gate.startsWith("sourceElementUnresolved"));
    }

    /**
     * #480: {@link #adaptBareAttrItemNav}'s twin — the arm's exits restated in order with the
     * arm's OWN predicates and the SAME equivalent builder; the delegation exit names the
     * equivalent's first failing gate via {@link #reasonForFeatureCall} (the #478 composition).
     * Returns {@link #UNATTRIBUTED} when every gate passes (the arm claims — reachable from the
     * mirror only on twin drift, surfaced by the unattributed meter).
     */
    private String bareAttrItemNavDeclineGate(RSymbolReference ref, RAttribute attr, RWorkspace ws) {
        if (wouldSynthesizeRuleInputNav(ref, attr) && !hasEnclosingRuleLambda(ref)) {
            // #514 ARM-2 twin: the rule-input top-nav arm claims through the SAME throwaway
            // `input`-rooted equivalent (the shared builder — the #497 PARAM leaf + the
            // standing hop compose); the residue names the arm's own first failing gate
            // dot-suffixed onto the channel token (the #478 composition), the parent facet
            // kept on the untyped/defensive faces.
            RRule riRule = enclosingRule(ref);
            if (riRule == null || riRule.fromType().isEmpty()) {
                return "ruleInputNav.ruleGone"; // defensive — the caller's guard just held
            }
            RMetaAnnotatedType riType = type(ref, ws);
            if (riType == null || riType.isMissing()) {
                return "ruleInputNav.untyped.p:" + probeParentFacet(ref);
            }
            RFeatureCall riEq = ruleInputTopNavEquivalent(ref, attr, riRule);
            String riSub = reasonForFeatureCall(riEq, ws);
            if (!UNATTRIBUTED.equals(riSub)) {
                return "ruleInputNav." + riSub;
            }
            Optional<IRExpr> riLowered = adapt(riEq, ws);
            boolean riPlain = riLowered.isPresent()
                    && riLowered.get() instanceof FieldAccess riFa
                    && riFa.receiver() instanceof IRVariable riRecv
                    && riRecv.variableKind() == IRVariable.VariableKind.PARAM
                    && "input".equals(riRecv.name());
            boolean riMeta = riLowered.isPresent()
                    && riLowered.get() instanceof IRMetaAccess riMa
                    && riMa.receiver() instanceof IRVariable riMetaRecv
                    && riMetaRecv.variableKind() == IRVariable.VariableKind.PARAM
                    && "input".equals(riMetaRecv.name());
            if (!riPlain && !riMeta) {
                return "ruleInputNav.offShape." + (riLowered.isEmpty() ? "empty"
                        : riLowered.get().getClass().getSimpleName());
            }
            return UNATTRIBUTED; // the #514 ARM-2 mint claims BOTH hop shapes — twin drift only
        }
        RFunction enclosing = enclosingFunction(ref);
        if (enclosing != null && collidesWithShortcut(enclosing, ref.name())) {
            return "shortcutCollision";
        }
        RExpression source = filterExtractSourceOfArmBinder(ref);
        if (source == null) {
            return "noFilterExtractBinder";
        }
        RDataType elementType = sourceElementDataType(source, 0);
        if (elementType == null) {
            // #513: the flat token refined IN PLACE (the #491 pattern): the binder SOURCE's
            // family names the walk leg that bottomed — the widening must know which source
            // shapes the 31 sole events actually carry.
            // #514: the facet DEEPENED one level (the #491 pattern, both twin seats) — see
            // sourceElementUnresolvedFacet.
            return "sourceElementUnresolved." + sourceElementUnresolvedFacet(source);
        }
        if (findAttributeOnDataTypeByName(elementType, attr.name()) != attr) {
            return "identityGuard";
        }
        String sub = reasonForFeatureCall(implicitItemNavEquivalent(ref, attr), ws);
        if (!UNATTRIBUTED.equals(sub)) {
            return sub;
        }
        // #501 C4: the #499 equivalentMetaAccess class CLAIMED — the arm's bareMeta leg
        // re-ranges the IRMetaAccess-over-synthetic-item equivalent exactly like the plain hop,
        // so the twin restates the arm's acceptance: a meta-annotated attr whose lowered
        // equivalent fits the accepted shape and types now reads UNATTRIBUTED (the arm claims);
        // the residue keeps the face. The annotation test routes (adaptFeatureCall sends every
        // meta-annotated feature through the meta arm); the shape test is the arm's own.
        if (isMetaAnnotated(attr)) {
            Optional<IRExpr> metaLowered = adapt(implicitItemNavEquivalent(ref, attr), ws);
            if (metaLowered.isEmpty()
                    || !(metaLowered.get() instanceof IRMetaAccess bareMeta
                            && bareMeta.receiver() instanceof IRVariable bareRecv
                            && bareRecv.variableKind() == IRVariable.VariableKind.SYNTHETIC_ITEM)) {
                return "equivalentMetaAccess";
            }
            RMetaAnnotatedType metaNavType = type(ref, ws);
            if (metaNavType == null || metaNavType.isMissing()) {
                return "equivalentMetaAccess.untyped";
            }
            return UNATTRIBUTED; // the arm's bareMeta leg claims — twin drift only
        }
        RMetaAnnotatedType navType = type(ref, ws);
        if (navType == null || navType.isMissing()) {
            return "navTypeMissing";
        }
        return UNATTRIBUTED;
    }

    /**
     * #480: {@link #adaptImplicitItemChain}'s twin — the arm's exits restated in order with the
     * arm's OWN predicates and the SAME equivalent builder; the delegation exit names the
     * equivalent's first failing gate via {@link #reasonForFeatureCall}. Returns
     * {@link #UNATTRIBUTED} when every gate passes (twin drift only).
     */
    private String implicitItemChainDeclineGate(REnumValueRef enr, RWorkspace ws) {
        if (enr.resolvedSymbol().isPresent()) {
            // #501 C2: the arm's exits restated in order (the twin discipline) — the class
            // lowers to IRSymbolNav whenever the symbol is a rule/function, both names are
            // present and the node types; the residue faces name the arm's own gates.
            RNode headSymbol = enr.resolvedSymbol().orElse(null);
            if (!(headSymbol instanceof RRule || headSymbol instanceof RFunction)) {
                return "headSymbolNav.otherSymbol";
            }
            if (enr.enumName() == null || enr.enumName().isEmpty()
                    || enr.valueName() == null || enr.valueName().isEmpty()) {
                return "headSymbolNav.nameMissing";
            }
            RMetaAnnotatedType symNavType = type(enr, ws);
            if (symNavType == null || symNavType.isMissing()) {
                return "headSymbolNav.untyped";
            }
            return UNATTRIBUTED; // the arm claims — reachable from the mirror only on twin drift
        }
        String headName = enr.enumName();
        String leafName = enr.valueName();
        if (headName == null || headName.isEmpty() || leafName == null || leafName.isEmpty()) {
            return "nameMissing";
        }
        if (isEnclosingClosureParamName(enr, headName)) {
            // #505 arm-A1: the arm's exits restated in order (the twin discipline) — the
            // class claims through adaptClosureParamHeadChain whenever the binder-source
            // walk proves the element, the VALUE segment resolves plain and the node types;
            // the residue faces name the arm's own gates (the composed-token convention).
            RDataType cpElem = closureParamElementType(enr, headName);
            if (cpElem == null) {
                // #508 arm-A4 twin: the typed-node leg claims the meta-sourced residue (the
                // node's own engine type is the leg's whole gate — the metaSrcGate census
                // read every live row typed); the UNTYPED residue keeps the probed spelling.
                RMetaAnnotatedType cpOwnType = type(enr, ws);
                if (cpOwnType != null && !cpOwnType.isMissing()) {
                    return UNATTRIBUTED; // the #508 arm-A4 typed-node leg claims
                }
                return "closureParamHead.twNull";
            }
            RAttribute cpLeaf = findAttributeOnDataTypeByName(cpElem, leafName);
            if (cpLeaf == null) {
                return "closureParamHead.leafMiss";
            }
            if (isMetaAnnotated(cpLeaf)) {
                // #514 ARM-5 twin: the meta-LEAF leg claims exactly the OWN-TYPED slice (the
                // arm's whole gate — the #508 arm-A4 pattern at the leaf-meta seat); the
                // untyped residue keeps the probed spelling.
                RMetaAnnotatedType mlType = type(enr, ws);
                if (mlType == null || mlType.isMissing()) {
                    return "closureParamHead.metaLeaf.untyped";
                }
                return UNATTRIBUTED; // the #514 ARM-5 meta-leaf claim — twin drift only
            }
            RMetaAnnotatedType cpNavType = type(enr, ws);
            if ((cpNavType == null || cpNavType.isMissing()) && ws != null) {
                cpNavType = ws.getInferredAttributeType(cpLeaf);
            }
            if (cpNavType == null || cpNavType.isMissing()) {
                return "closureParamHead.untyped";
            }
            return UNATTRIBUTED; // the arm claims — reachable from the mirror only on twin drift
        }
        RFunction enclosing = enclosingFunction(enr);
        RShortcut selfShadow = enclosingShortcut(enr);
        boolean headIsSelfShortcut = selfShadow != null && headName.equals(selfShadow.name());
        if (enclosing != null && !headIsSelfShortcut
                && (resolveNameInFunction(enclosing, headName) != null
                        || collidesWithShortcut(enclosing, headName))) {
            return "scopeHead";
        }
        if (wouldSynthesizeRuleInputChain(enr)) {
            // #501 C1: the arm's exits restated with the SAME equivalent builder — a
            // still-declining input chain names the equivalent's own first failing gate
            // dot-suffixed onto the channel token (the #478 composition), then the arm's
            // shape + typing gates in order. The mirror re-adapts at the public ROOT position
            // while the arm adapts at its child NodeId, so a position-gated sub-admission can
            // read lowerable here where the arm's recursion declined — the documented
            // optimistic-root asymmetry (the reasonForConditional boundary class); the
            // unattributed meter carries that residue honestly (+10 events at the #501 ring,
            // on the standing pre-#501 sentinel class).
            REnumValueRef.AttributeChain ruleChain = enr.resolvedAttributeChain().orElse(null);
            RAttribute ruleHead = ruleChain == null ? null : ruleChain.attributeOpt().orElse(null);
            RAttribute ruleLeaf = ruleChain == null ? null : ruleChain.feature();
            RRule enclosingR = enclosingRule(enr);
            RTypeCall fromTypeCall = enclosingR == null ? null
                    : enclosingR.fromType().orElse(null);
            if (ruleHead == null || ruleLeaf == null || fromTypeCall == null) {
                return "ruleInputChain.chainUnbound";
            }
            RFeatureCall inputEquivalent =
                    disguisedInputChainEquivalent(enr, ruleHead, ruleLeaf, fromTypeCall);
            String inputSub = reasonForFeatureCall(inputEquivalent, ws);
            if (!UNATTRIBUTED.equals(inputSub)) {
                return "ruleInputChain." + inputSub;
            }
            Optional<IRExpr> inputLowered = adapt(inputEquivalent, ws);
            // #513: the arm's IRSynItemNav-spine acceptance restated (the twin discipline)
            // — the two-link item-base form OR the #505-pattern FieldAccess{IRSynItemNav}
            // form OR (probe3) the #508-A3-pattern IRMetaAccess{IRSynItemNav} meta-leaf
            // form claims; the residue keeps the #513 in-place shape facet (the decode
            // axis that read the 28-event class 100% inner:IRSynItemNav — now claimed).
            boolean inputSpineAccepted = inputLowered.isPresent()
                    && (isTwoLinkSpineOverItemBase(inputLowered.get(), true)
                            || (inputLowered.get() instanceof FieldAccess synTop
                                    && synTop.receiver() instanceof IRSynItemNav)
                            || (inputLowered.get() instanceof IRMetaAccess metaTop
                                    && metaTop.receiver() instanceof IRSynItemNav));
            if (!inputSpineAccepted) {
                return "ruleInputChain.shapeGate."
                        + (inputLowered.isEmpty() ? "unlowered"
                                : spineShapeFacet(inputLowered.get()));
            }
            RMetaAnnotatedType inputNavType = type(enr, ws);
            if (inputNavType == null || inputNavType.isMissing()) {
                return "ruleInputChain.navTypeMissing";
            }
            return UNATTRIBUTED; // the arm claims — twin drift only
        }
        RExpression source = filterExtractSourceOfArmBinder(enr);
        if (source == null) {
            return "noFilterExtractBinder";
        }
        RDataType elementType = sourceElementDataType(source, 0);
        if (elementType == null) {
            // #513: the same in-place refinement as the bare-attr twin above — the source
            // family is the decode axis.
            // #514: the facet DEEPENED one level (the #491 pattern, both twin seats) — see
            // sourceElementUnresolvedFacet.
            return "sourceElementUnresolved." + sourceElementUnresolvedFacet(source);
        }
        RAttribute headAttr = findAttributeOnDataTypeByName(elementType, headName);
        if (headAttr == null) {
            return "headNotOnElement";
        }
        REnumValueRef.AttributeChain chain = enr.resolvedAttributeChain().orElse(null);
        RAttribute boundHead = chain == null ? null : chain.attributeOpt().orElse(null);
        RAttribute leafAttr;
        if (boundHead != null) {
            if (boundHead != headAttr) {
                return "boundHeadMismatch";
            }
            leafAttr = chain.feature();
            if (leafAttr == null) {
                return "leafUnresolved";
            }
        } else {
            RDataType headType = declaredDataTypeOf(headAttr);
            if (headType == null) {
                return "headTypeUnresolved";
            }
            leafAttr = findAttributeOnDataTypeByName(headType, leafName);
            if (leafAttr == null) {
                return "leafUnresolved";
            }
        }
        String sub = reasonForFeatureCall(implicitItemChainEquivalent(enr, headAttr, leafAttr), ws);
        if (!UNATTRIBUTED.equals(sub)) {
            return sub;
        }
        // #501 C4: the #500 equivalentMetaAccess class CLAIMED — the arm's meta legs re-range
        // the meta-bearing 2-link spines (IRMetaAccess{FieldAccess{item}} metaLeaf ·
        // FieldAccess{IRMetaAccess{item}} metaHead · IRMetaAccess{IRMetaAccess{item}} metaBoth)
        // exactly like the plain spine, so the twin restates the arm's acceptance: a
        // meta-annotated hop whose lowered equivalent fits the accepted spine and types now
        // reads UNATTRIBUTED (the arm claims); the residue keeps the face (a meta equivalent
        // outside the accepted spine — e.g. a deeper composition the 2-link gate excludes).
        // The annotation test routes (adaptFeatureCall sends every meta-annotated feature
        // through the meta arm); the spine test is the arm's own.
        if (isMetaAnnotated(leafAttr) || isMetaAnnotated(headAttr)) {
            Optional<IRExpr> metaLowered =
                    adapt(implicitItemChainEquivalent(enr, headAttr, leafAttr), ws);
            // #508 arm-A3 twin: the IRMetaAccess{IRSynItemNav} spine joins the accepted set
            // (the meta twin of the #505 synTop acceptance — the metaHopGate census read the
            // face 100% this one shape); the residue outside every accepted form keeps the
            // probed spelling.
            if (metaLowered.isEmpty()
                    || !(isTwoLinkSpineOverItemBase(metaLowered.get(), false)
                            || (metaLowered.get() instanceof IRMetaAccess metaSynTop
                                    && metaSynTop.receiver() instanceof IRSynItemNav))) {
                return "equivalentMetaAccess";
            }
            RMetaAnnotatedType metaNavType = type(enr, ws);
            if (metaNavType == null || metaNavType.isMissing()) {
                return "equivalentMetaAccess.untyped";
            }
            return UNATTRIBUTED; // the arm's meta legs claim — twin drift only
        }
        // #504: the arm-B mint broke the plain-spine IMPLICATION — a non-meta equivalent whose
        // sub-reason claims can now lower to the IRSynItemNav-based spine
        // (FieldAccess{IRSynItemNav} — the un-retypeable head hop minted, the leaf hop composed
        // over it). #505: the arm ACCEPTS that spine (the seat conversion installed the enum
        // family's oracle dispatch leg — the enumChain serve — so the #504 exclusion's premise
        // dissolved; the accepted spine re-ranges exactly like the plain form and every
        // containing root oracle-serves). The twin restates the widened spine gate; the face
        // names only the residue OUTSIDE every accepted form now.
        Optional<IRExpr> chainLowered = adapt(implicitItemChainEquivalent(enr, headAttr, leafAttr), ws);
        if (chainLowered.isPresent()
                && !((chainLowered.get() instanceof FieldAccess top
                        && top.receiver() instanceof FieldAccess headHop
                        && headHop.receiver() instanceof IRVariable spineRecv
                        && spineRecv.variableKind() == IRVariable.VariableKind.SYNTHETIC_ITEM)
                        || (chainLowered.get() instanceof FieldAccess synTop
                                && synTop.receiver() instanceof IRSynItemNav)
                        || isTwoLinkSpineOverItemBase(chainLowered.get(), false))) {
            return "equivalentSynItemSpine";
        }
        RMetaAnnotatedType navType = type(enr, ws);
        if (navType == null || navType.isMissing()) {
            return "navTypeMissing";
        }
        return UNATTRIBUTED;
    }

    /**
     * The lowered receiver form that failed {@link #isNavigableReceiver} +
     * {@link #isFilterExtractBoundItemReceiver} + (since #479) the
     * {@link #retypedSyntheticFilterExtractItem} admission: the reference kinds
     * ({@code receiverAlias.<bodyFace>} — body-classified since #492 by the shared
     * {@link #aliasNavBodyFacet} walk — / {@code receiverEnumValue}), the implicit-item sub-gates in
     * {@link #isFilterExtractBoundItemReceiver}'s own order ({@code itemTypeMissing} /
     * {@code itemTypeMeta} / {@code itemNotFilterExtractBound} / {@code itemMetaSourced}), the
     * synthetic item ({@code receiverSyntheticItem} — since #504 a DRIFT face at this seat: the
     * IRSynItemNav mint claims the whole residue unconditionally, so the token fires only if
     * the mint and this twin diverge — the #486 {@code nameMissing} stable-token precedent; the
     * pre-#504 residue decode lives in the #502 synItemGate census receipts), or the generic
     * {@code receiver:<IRClass>}.
     */
    private static String receiverToken(IRExpr lowered, RExpression rawReceiver, RWorkspace ws) {
        if (lowered instanceof IRReference ref) {
            if (ref.referenceKind() == IRReference.ReferenceKind.ALIAS) {
                // #492: refined IN PLACE by the shared alias-nav body walk (the shortcut is
                // linker-BOUND on this face — the raw receiver names it directly; the feature
                // gate already passed, so bodyTyped here is the claim-shaped state outright).
                RShortcut shortcut = rawReceiver instanceof RSymbolReference sr
                        && sr.symbol().orElse(null) instanceof RShortcut bound ? bound : null;
                String bodyFace = aliasNavBodyFacet(shortcut, enclosingFunction(rawReceiver), ws);
                return "receiverAlias." + (bodyFace == null ? "bodyTyped" : bodyFace);
            }
            if (ref.referenceKind() == IRReference.ReferenceKind.ENUM_VALUE) {
                return "receiverEnumValue";
            }
            return "receiver:IRReference";
        }
        if (lowered instanceof IRVariable var) {
            if (var.variableKind() == IRVariable.VariableKind.SYNTHETIC_ITEM) {
                return "receiverSyntheticItem";
            }
            if (var.variableKind() == IRVariable.VariableKind.USER_ITEM) {
                if (lowered.type() == null || lowered.type().isMissing()) {
                    return "itemTypeMissing";
                }
                if (lowered.type().hasMeta()) {
                    return "itemTypeMeta";
                }
                if (!(rawReceiver instanceof RImplicitVariable implicit)) {
                    return UNATTRIBUTED; // a USER_ITEM lowers only from an RImplicitVariable
                }
                // #506: the admission's widened binding gates restated (then/min/max/sort
                // implicit bodies join the filter/extract pair; the new legs carry the
                // STRONG allowlist meta proof — the probe2 sort-coercion catch); #523 arm-B
                // adds the lambda-less TYPE-guarded switch-case leg (the #221 re-root lives
                // inside the verbatim-reused handle(RImplicitVariable)) — the token
                // spellings KEPT (the #486 stable-token precedent): the bound face now names
                // the walk's whole residue (named binders / other ops / guard-less or
                // enum-keyed cases / no lambda), the meta face the legs' L-029 split.
                if (!itemBindingBound(implicit)) {
                    return "itemNotFilterExtractBound";
                }
                if (itemBindingMetaSourced(implicit)) {
                    // #508 arm-A5: a DRIFT face at this seat — the IRMetaItemNav mint claims
                    // the whole bound meta-sourced slice unconditionally (same predicates,
                    // same feature-name fact), so the token fires only if the mint and this
                    // twin diverge (the #504 receiverSyntheticItem stable-token precedent).
                    return "itemMetaSourced";
                }
                return UNATTRIBUTED;
            }
        }
        return "receiver:" + lowered.getClass().getSimpleName();
    }

    /** Mirror of {@link #adaptComparison}'s exits, in order; left operand checked before right. */
    private String reasonForComparison(RComparisonExpr cmp, RWorkspace ws) {
        if (cmp.mod().isPresent()) {
            return UNATTRIBUTED; // the #503 arm-A1 claims the modified comparison unconditionally
        }
        RExpression rawLeft = cmp.rawLeft();
        RExpression rawRight = cmp.rawRight();
        if (rawLeft == null || rawRight == null) {
            return "elidedOperand";
        }
        Optional<IRExpr> left = adapt(rawLeft, ws);
        Optional<IRExpr> right = adapt(rawRight, ws);
        if (left.isEmpty() || right.isEmpty()) {
            return "operandNotExpressible";
        }
        if (!(isComparisonOperand(left.get(), right.get())
                || isBinderBlindItemOperand(left.get(), rawLeft))) {
            return operandToken(left.get(), rawLeft, right.get(), ws);
        }
        if (!(isComparisonOperand(right.get(), left.get())
                || isBinderBlindItemOperand(right.get(), rawRight))) {
            return operandToken(right.get(), rawRight, left.get(), ws);
        }
        return UNATTRIBUTED;
    }

    /** Mirror of {@link #adaptEquality}'s exits, in order; left operand checked before right. */
    private String reasonForEquality(REqualityExpr eq, RWorkspace ws) {
        if (eq.mod().isPresent()) {
            return UNATTRIBUTED; // the #503 arm-A1 claims the modified equality unconditionally
        }
        RExpression rawLeft = eq.rawLeft();
        RExpression rawRight = eq.rawRight();
        if (rawLeft == null || rawRight == null) {
            return "elidedOperand";
        }
        Optional<IRExpr> left = adapt(rawLeft, ws);
        Optional<IRExpr> right = adapt(rawRight, ws);
        // #504 arm-C1b: the requalification fallback restated — mirrors the adapt arm.
        if (left.isEmpty()) {
            left = requalifiedEnumComparand(rawLeft, rawRight, NodeId.ROOT, ws);
        }
        if (right.isEmpty()) {
            right = requalifiedEnumComparand(rawRight, rawLeft, NodeId.ROOT, ws);
        }
        if (left.isEmpty() || right.isEmpty()) {
            return "operandNotExpressible";
        }
        if (!(isEqualityOperand(left.get(), right.get())
                || isFilterExtractBoundItemOperand(left.get(), rawLeft)
                || isAdmittedAliasOperand(left.get(), rawLeft, right.get(), ws))) {
            return operandToken(left.get(), rawLeft, right.get(), ws);
        }
        if (!(isEqualityOperand(right.get(), left.get())
                || isFilterExtractBoundItemOperand(right.get(), rawRight)
                || isAdmittedAliasOperand(right.get(), rawRight, left.get(), ws))) {
            return operandToken(right.get(), rawRight, left.get(), ws);
        }
        return UNATTRIBUTED;
    }

    /**
     * The lowered operand form that failed a comparison / equality / existence operand gate:
     * alias (since #491 faceted IN PLACE — see below), implicit item, MULTI param/nav/call (the
     * SINGLE-result requirement), a numeric literal without a resolved scalar sibling
     * ({@code operandNumericLiteral} — the two-literal {@code 0 = 0} shape and the
     * literal-vs-unresolvable-sibling shape), or the generic {@code operand:<IRClass>}.
     *
     * <p>#491 — the operandAlias DECODE: the flat token refined IN PLACE to
     * {@code operandAlias.<clean|bodyMeta|bodyMissing|rawUnresolved>[.enumSibling]} at this single
     * seat (all three consuming mirrors — equality, comparison, existence — compose it, so
     * Σ facets ≡ each flat count per gate BY CONSTRUCTION; the logical gate's
     * {@link #booleanOperandToken} alias face stays FLAT — the {@code ComparisonResult.ofNullSafe}
     * coercion family, a different story). The facets decode the PLANNED equality/existence
     * admission against legacy's own levers, each read at source:
     * <ul>
     *   <li>{@code clean} — the shortcut BODY's engine type is present and meta-free
     *       ({@code getInferredType(shortcut.expression())} — the engine types a shortcut's
     *       DEFINING expression even though the shortcut REFERENCE reads MISSING, legacy
     *       {@code resolveOperandJavaType}'s own note): legacy's equality render for such an
     *       operand is the bare context-free {@code aliasName(inputs)} ({@code inferNumericType}
     *       leaves shortcut refs unclassified so the join comes from the SIBLING alone and
     *       threads no coercion into a resolved sibling, the {@code literalSiblingNumericType}
     *       walk stays declined via the numeric-literal sibling gate, and
     *       {@code coerceNavigationReceiver} is a no-op on a null-typed builder) — the
     *       ADMITTING face, EXCEPT the {@code FunctionAliasHelper}-retype subset (the #326
     *       META / #334 basic-scalar classes — this engine-side body read is the WEAKER oracle,
     *       the #491 ring's own decode): {@code retypeNullTypedAliasOperand} defeats the
     *       null-typed premise there, so the Java-side guard trips that subset on the exact
     *       legacy lever and delegates the claim whole ({@code aliasEqualityOperandMeta});
     *       unconditional at EXISTENCE (that seat carries no retype lever);</li>
     *   <li>{@code bodyMeta} — the body element type carries meta: the #326
     *       {@code aliasOperandMetaCoerce} mirror — at the EQUALITY/comparison seats legacy
     *       retypes the null-typed alias operand from the {@code FunctionAliasHelper} walk so the
     *       meta-strip deref fires ({@code .map("Type coercion", … .getValue())} — the
     *       {@code Qualify_Transaction_OIS} golden), a render the neutral leaf cannot reproduce —
     *       declines at equality; the EXISTENCE seat carries NO such lever (verified at source —
     *       {@code ExistenceHandler} compiles the operand bare, no retype/strip), so the planned
     *       existence admission takes this face too;</li>
     *   <li>{@code bodyMissing} — the engine leaves the body untyped (filter/extract-bodied
     *       shortcuts etc.): prove-or-decline;</li>
     *   <li>{@code rawUnresolved} — the raw operand is not a shortcut-resolved
     *       {@link RSymbolReference}: contract-impossible for a lowered ALIAS (the stable-token
     *       belt, the #486 nameMissing precedent);</li>
     *   <li>{@code .enumSibling} — a decode DECORATION (the sibling operand is a bare
     *       enum-value reference): the #503 arm-A3 RETIRED the equality-seat carve-out this
     *       suffix once drove — the alias node now carries the BODY-channel type, so the
     *       emitter's {@code enumOperandRequalifies} owns the residual gate (same-enum pairs
     *       drive, divergent/unresolvable pairs decline byte-safely) and the
     *       {@code operandAlias.clean.enumSibling} face reads ZERO; the suffix still decorates
     *       the non-clean equality faces and every comparison-seat face.</li>
     * </ul>
     * The comparison gate keeps declining EVERY alias face (the #372 {@code comparisonIntWiden}
     * lever classifies aliases through the body walk at the INEQUALITY seat — a numeric-alias
     * inequality can fire a widen hop on its sibling that the neutral render does not reproduce);
     * the facets there are honest sizing, not an admission plan.
     */
    private String operandToken(IRExpr lowered, RExpression raw, IRExpr sibling, RWorkspace ws) {
        if (lowered instanceof IRReference ref
                && ref.referenceKind() == IRReference.ReferenceKind.ALIAS) {
            String base = aliasOperandBodyFacet(raw, ws);
            return "operandAlias." + (base == null ? "clean" : base)
                    + (sibling != null && isEnumValueReference(sibling) ? ".enumSibling" : "");
        }
        if (lowered instanceof IRVariable var) {
            if (var.variableKind() == IRVariable.VariableKind.USER_ITEM
                    || var.variableKind() == IRVariable.VariableKind.SYNTHETIC_ITEM) {
                return "operandItem";
            }
            if (var.variableKind() == IRVariable.VariableKind.PARAM
                    && var.cardinality() == ExpressionCardinality.MULTI) {
                return "operandMultiParam";
            }
        }
        if (lowered instanceof FieldAccess fa && fa.cardinality() == ExpressionCardinality.MULTI) {
            return "operandMultiNav";
        }
        if (lowered instanceof IRApply apply && apply.cardinality() == ExpressionCardinality.MULTI) {
            return "operandMultiCall";
        }
        if (isNumericLiteral(lowered)) {
            // #513: the flat token refined IN PLACE (the #491 operandAlias pattern — Σ
            // facets ≡ the flat count per gate BY CONSTRUCTION; all three consuming
            // mirrors compose it): the SIBLING's lowered class is the decode axis — the
            // licensing sets are sibling-keyed, so the facet names the exact missing
            // licensing leg per event (sib:BinaryOp = the #511 equality leg the comparison
            // seat lacks; sib:item = the bound-item leg neither licensing set carries).
            return "operandNumericLiteral." + numericLiteralSiblingFacet(sibling);
        }
        return "operand:" + lowered.getClass().getSimpleName();
    }

    /**
     * #513: the numeric-literal sibling facet ({@link #operandToken}'s in-place refinement)
     * — names the lowered SIBLING's licensing class, most-specific first: {@code sibNull}
     * (an existence seat — no sibling), {@code sib:literal} (the two-literal {@code 0 = 0}
     * shape), {@code sib:item} (a bound implicit item), {@code sib:alias}, or the generic
     * {@code sib:<Class>}. Probe-only (the twin channel).
     */
    private static String numericLiteralSiblingFacet(IRExpr sibling) {
        if (sibling == null) {
            return "sibNull";
        }
        if (isNumericLiteral(sibling)) {
            return "sib:literal";
        }
        if (sibling instanceof IRVariable var
                && (var.variableKind() == IRVariable.VariableKind.USER_ITEM
                        || var.variableKind() == IRVariable.VariableKind.SYNTHETIC_ITEM)) {
            return "sib:item";
        }
        if (isAliasReference(sibling)) {
            return "sib:alias";
        }
        return "sib:" + sibling.getClass().getSimpleName();
    }

    /**
     * #491 — the alias-operand BODY classifier (the single walk the {@link #operandToken} facets
     * and the planned equality/existence admission both consume — the #489 one-walk law, so
     * admission/witness twin-drift is structurally impossible): resolves the raw operand's
     * {@link RShortcut} and reads the engine's type for its DEFINING expression
     * ({@code getInferredType(shortcut.expression())} — populated even though the shortcut
     * REFERENCE itself reads MISSING, the L-032 gap legacy's {@code resolveOperandJavaType}
     * documents on its own side), falling back — since the #518 arm-B1 teach — to the
     * SYMBOL channel for an {@link RSymbolReference}-shaped body the expression channel
     * leaves untyped ({@link #symbolChannelType}: the referenced attribute's own type, or a
     * resolved callee's OUTPUT type — the byte-story is the standing clean-admission render,
     * see the inline note). Returns {@code null} when the body is typed and meta-free (the
     * admitting state), else the facet base: {@code bodyMeta} (the #326 equality-seat deref
     * lever's trigger) / {@code bodyMissing} (prove-or-decline — since #518 the
     * symbol-unresolvable residue) / {@code rawUnresolved} (the contract-impossible belt).
     */
    private static String aliasOperandBodyFacet(RExpression raw, RWorkspace ws) {
        RShortcut shortcut = raw instanceof RSymbolReference ref
                && ref.symbol().orElse(null) instanceof RShortcut sc ? sc : null;
        if (shortcut == null) {
            return "rawUnresolved";
        }
        RExpression body = shortcut.expression();
        RMetaAnnotatedType bodyType = body == null ? null : ws.getInferredType(body);
        if (bodyType == null || bodyType.isMissing()) {
            // #518 arm-B1 — the SYMBOL-channel fallback (the #514 re-decode armed): the
            // dominant untyped-body class is an RSymbolReference body — a bare input
            // reference (`alias periodEndPrice: observation`) or a resolved CALL
            // (`alias startDateIsInLeapYear: IsLeapYear(...)`) — whose EXPRESSION-channel
            // type the engine leaves missing while the referenced SYMBOL's own channel
            // types it (an attribute's declared type; a callee's OUTPUT type — the
            // adaptPointFree idiom). The byte-story is the STANDING #491 clean-admission
            // render, unmodified: the alias operand renders the null-typed context-free
            // aliasName(inputs) whatever the body channel (emitAlias stamps NO expression
            // type), the #326 META-signature subset still trips the Java-side guard on the
            // exact legacy lever (tryAliasReceiverMapperType — the postPin
            // ALIAS_EQUALITY_OPERAND_META serve), and the engine-side read here is only
            // the admission ORACLE. An unresolvable body symbol keeps the honest
            // bodyMissing face.
            // #518: the RAW operand ref anchors the enclosing walk (a parse-time child-linked
            // node — the probe's noEnclosing decode read the LINKER-bound RShortcut instances
            // parent-BARE across the corpus, so a shortcut-anchored walk finds nothing); the
            // shortcut anchor stays as the fixture belt (hand-built refs parent the shortcut).
            RFunction operandEnclosing = enclosingFunction(raw);
            if (operandEnclosing == null) {
                operandEnclosing = enclosingFunction(shortcut);
            }
            bodyType = body == null ? null : symbolChannelType(body, operandEnclosing, ws);
            if (bodyType == null || bodyType.isMissing()) {
                // #514: the designed-residue face refined IN PLACE (the #491 pattern): the
                // alias BODY's class names the untyped-body family — WHICH body shapes the
                // engine actually leaves untyped here. #518: the RSymbolReference class
                // refined ONE key deeper — the symbol-channel walk's own stop reason
                // ({@link #symbolChannelDeclineFacet}), so the residue names the exact
                // missing leg per event (the instrument-first response at the teach's own
                // residue).
                if (body instanceof RSymbolReference) {
                    return "bodyMissing.body:RSymbolReference."
                            + symbolChannelDeclineFacet(body, operandEnclosing, ws);
                }
                return body == null ? "bodyMissing.bodyNull"
                        : "bodyMissing.body:" + body.getClass().getSimpleName();
            }
        }
        if (bodyType.hasMeta()) {
            return "bodyMeta";
        }
        return null;
    }

    /**
     * #518 arm-B1 — the SYMBOL-channel type of an {@link RSymbolReference}-shaped shortcut
     * body the expression channel leaves untyped: the referenced symbol's OWN type — an
     * {@link RAttribute}'s inferred attribute type (the bare-input body,
     * {@code alias periodEndPrice: observation}), or an {@link RFunction} callee's OUTPUT
     * attribute type (the call-shaped body, {@code alias startDateIsInLeapYear:
     * IsLeapYear(...)} — the {@link #adaptPointFree} output-channel idiom). The corpus
     * bodies are SYMBOL-EMPTY (the linker never binds inside shortcut bodies — the #486
     * synthetic decode's mechanism at this seat), so both legs carry a NAME-KEYED
     * resolution mirroring legacy's own posture: a bare name resolves against the
     * enclosing function's INPUTS (deliberately never the output), a call-shaped name
     * against the workspace's exact-name-unique {@link RFunction}
     * ({@code RWorkspace.findByName} filtered to true name-equality — an ambiguous simple
     * name declines, the honest residue).
     *
     * <p><strong>The transitive-output guard (byte-critical):</strong> an OUTPUT-consuming
     * body — bare or transitive through the call's arguments — renders the
     * BUILDER-threaded {@code aliasName(result, inputs…)} invocation (legacy
     * {@code FunctionAliasHelper}: the output builder is the FIRST param), never the bare
     * {@code aliasName(inputs)} line {@code emitAlias} produces, so the walk declines the
     * whole class up front ({@link #aliasBodyUsesOutput} — the same mirror the nav-family
     * seats key on). Null for every other symbol class (an unresolved name, a nested
     * shortcut) and for non-reference bodies — the honest {@code bodyMissing} residue.
     * ONE walk (the #489 law): the {@link #aliasOperandBodyFacet} facets and the alias
     * mint arms' node-type sourcing both consume it, so admission/witness/type twin-drift
     * is structurally impossible.
     */
    private static RMetaAnnotatedType symbolChannelType(RExpression body, RFunction enclosing,
                                                        RWorkspace ws) {
        if (!(body instanceof RSymbolReference bodyRef) || ws == null) {
            return null;
        }
        // #518: the DISPATCH-variant scope join (the #505 dispatchBaseInputNamed law at this
        // walk — the live faces sit in `func YearFraction(dayCountFractionEnum: ... ->
        // _30E_360_ISDA)`-class VARIANT bodies, which declare no inputs/output of their own;
        // the base function carries both, exactly the scope legacy's own alias machinery
        // threads).
        RFunction scope = enclosing;
        if (scope != null && scope.dispatch().isPresent()) {
            scope = scope.dispatchBase().orElse(null);
        }
        String outputName = scope != null && scope.output().isPresent()
                ? scope.output().get().name()
                : null;
        if (outputName == null
                || aliasBodyUsesOutput(body, outputName, scope, new HashSet<>())) {
            return null; // unprovable enclosing state, or the BUILDER-threaded render class
        }
        RNode symbol = bodyRef.symbol().orElse(null);
        if (symbol instanceof RAttribute attr) {
            return ws.getInferredAttributeType(attr);
        }
        if (symbol instanceof RFunction callee) {
            RAttribute output = callee.output().orElse(null);
            return output == null ? null : ws.getInferredAttributeType(output);
        }
        if (symbol instanceof RLibraryFunction libraryCallee) {
            // The LIBRARY-callee leg (the live cdm class — IsLeapYear/Min are
            // `[calculation]` builtins the linker binds as RLibraryFunction): the declared
            // returnType() read through a synthetic attribute (the #509 findChoiceSuperOption
            // projection pattern — a fresh node carrying the deep-copied RTypeCall, read on
            // the same getInferredAttributeType channel).
            return libraryReturnType(libraryCallee, ws);
        }
        if (symbol != null || bodyRef.name() == null || bodyRef.name().isEmpty()) {
            return null; // a bound non-attribute/function symbol — the honest residue
        }
        if (bodyRef.args().isEmpty()) {
            // The bare-name leg: the effective scope's INPUTS only (the output is
            // excluded twice over — the guard above and this explicit input scan).
            for (RAttribute input : scope.inputs()) {
                if (bodyRef.name().equals(input.name())) {
                    return ws.getInferredAttributeType(input);
                }
            }
            return null;
        }
        // The call-shaped leg: the exact-name-unique global function's OUTPUT type.
        RFunction callee = null;
        for (RNode hit : ws.findByName(bodyRef.name())) {
            if (hit instanceof RFunction fn && bodyRef.name().equals(fn.name())) {
                if (callee != null && callee != fn) {
                    return null; // ambiguous simple name across namespaces — decline
                }
                callee = fn;
            }
        }
        RAttribute calleeOutput = callee == null ? null : callee.output().orElse(null);
        return calleeOutput == null ? null : ws.getInferredAttributeType(calleeOutput);
    }

    /**
     * #518 arm-B1 — a library callee's declared return type read through a synthetic
     * attribute carrying the deep-copied {@link RTypeCall} (the #509
     * {@code findChoiceSuperOption} projection pattern: fresh nodes, never the library
     * declaration's own subtree; the {@code getInferredAttributeType} channel resolves the
     * copied type call exactly as it does the #509 option projections).
     */
    private static RMetaAnnotatedType libraryReturnType(RLibraryFunction libraryCallee,
                                                        RWorkspace ws) {
        RTypeCall returnType = libraryCallee.returnType();
        if (returnType == null) {
            return null;
        }
        RAttribute synthetic = new RAttribute();
        synthetic.setName(libraryCallee.name());
        synthetic.setTypeCall(RTypeCall.deepCopy(returnType));
        return ws.getInferredAttributeType(synthetic);
    }

    /**
     * #518 arm-B1 — the {@link #symbolChannelType} decline decode (the #514 refine-in-place
     * law at the teach's own residue): names the walk's FIRST failing leg for an
     * {@link RSymbolReference}-shaped body, in the walk's own order — {@code noEnclosing}
     * (no provable enclosing function) / {@code noDispatchBase} (a variant whose same-file
     * base does not resolve) / {@code noOutput} (an output-less effective scope — the
     * guard has nothing to prove against) / {@code guardOutput} (the transitive-output
     * guard — the BUILDER-threaded render class) / {@code symBoundOther} (a bound symbol
     * outside the attribute/function/library classes) / {@code attrTypeMissing} /
     * {@code libraryReturnTypeMissing} / {@code nameMissing} / {@code bareNotInput} (a bare
     * name off the enclosing inputs) / {@code callAmbiguous} (the simple name declared as
     * functions across namespaces) / {@code callAbsent} (no exact-name function) /
     * {@code callOutputTypeMissing}. Probe-only (the twin channel); {@code typed} is the
     * drift belt (reached only if this decode and the walk diverge).
     */
    private static String symbolChannelDeclineFacet(RExpression body, RFunction enclosing,
                                                    RWorkspace ws) {
        RSymbolReference bodyRef = (RSymbolReference) body;
        if (enclosing == null) {
            return "noEnclosing";
        }
        // The DISPATCH-variant scope join — the walk's own (see symbolChannelType).
        RFunction scope = enclosing;
        if (scope.dispatch().isPresent()) {
            scope = scope.dispatchBase().orElse(null);
            if (scope == null) {
                return "noDispatchBase";
            }
        }
        String outputName = scope.output().isPresent()
                ? scope.output().get().name()
                : null;
        if (outputName == null) {
            return "noOutput";
        }
        if (aliasBodyUsesOutput(body, outputName, scope, new HashSet<>())) {
            return "guardOutput";
        }
        RNode symbol = bodyRef.symbol().orElse(null);
        if (symbol instanceof RAttribute attr) {
            RMetaAnnotatedType t = ws.getInferredAttributeType(attr);
            return t == null || t.isMissing() ? "attrTypeMissing" : "typed";
        }
        if (symbol instanceof RFunction callee) {
            RAttribute output = callee.output().orElse(null);
            RMetaAnnotatedType t = output == null ? null : ws.getInferredAttributeType(output);
            return t == null || t.isMissing() ? "callOutputTypeMissing" : "typed";
        }
        if (symbol instanceof RLibraryFunction libraryCallee) {
            RMetaAnnotatedType t = libraryReturnType(libraryCallee, ws);
            return t == null || t.isMissing() ? "libraryReturnTypeMissing" : "typed";
        }
        if (symbol != null) {
            return "symBoundOther." + symbol.getClass().getSimpleName();
        }
        if (bodyRef.name() == null || bodyRef.name().isEmpty()) {
            return "nameMissing";
        }
        if (bodyRef.args().isEmpty()) {
            for (RAttribute input : scope.inputs()) {
                if (bodyRef.name().equals(input.name())) {
                    RMetaAnnotatedType t = ws.getInferredAttributeType(input);
                    return t == null || t.isMissing() ? "attrTypeMissing" : "typed";
                }
            }
            return "bareNotInput";
        }
        RFunction callee = null;
        boolean ambiguous = false;
        for (RNode hit : ws.findByName(bodyRef.name())) {
            if (hit instanceof RFunction fn && bodyRef.name().equals(fn.name())) {
                if (callee != null && callee != fn) {
                    ambiguous = true;
                }
                callee = fn;
            }
        }
        if (ambiguous) {
            return "callAmbiguous";
        }
        if (callee == null) {
            return "callAbsent";
        }
        RAttribute calleeOutput = callee.output().orElse(null);
        RMetaAnnotatedType t = calleeOutput == null ? null
                : ws.getInferredAttributeType(calleeOutput);
        return t == null || t.isMissing() ? "callOutputTypeMissing" : "typed";
    }

    /**
     * #492 — the point-free lowering (the L-109 deferral resolved): a bare no-arg reference whose
     * symbol is an {@link RFunction} lowers to {@link IRPointFreeApply} carrying the callee's
     * simple name + the callee OUTPUT's engine channels (the node's own type reads MISSING for
     * these shapes — the cache-boundary law; the nav-receiver lambda naming and the operand gates
     * read the output type/cardinality as the honest facts). Declines ONLY the
     * {@code aliasCollision} face (legacy {@code isAliasReference} wins over the bare-function
     * arm at {@code ReferenceHandler.handle}, so a colliding name renders as an ALIAS CALL — the
     * probed face read ZERO in this corpus, kept as the honest gate). Position admission lives at
     * the CONSUMING gates ({@link #isSimpleCallArg} + {@link #isNavigableReceiver} admit the kind
     * since #492; the operand gates decline it pending their own wrap-lever decode — the residue
     * re-spells at those gates' generic tokens).
     */
    private Optional<IRExpr> adaptPointFree(RSymbolReference ref, RFunction callee, NodeId id,
                                            RWorkspace ws) {
        RFunction enclosing = enclosingFunction(ref);
        if (enclosing != null && collidesWithShortcut(enclosing, ref.name())) {
            return Optional.empty(); // the aliasCollision face — legacy renders an ALIAS CALL
        }
        RAttribute output = callee.output().orElse(null);
        RMetaAnnotatedType type = type(ref, ws);
        if ((type == null || type.isMissing()) && output != null) {
            type = ws.getInferredAttributeType(output);
        }
        ExpressionCardinality outputCard = output != null && isMultiCardinality(output)
                ? ExpressionCardinality.MULTI : ExpressionCardinality.SINGLE;
        return Optional.of(new IRPointFreeApply(callee.name(), id, type, outputCard,
                Optionality.PRESENT, ref.sourceRange()));
    }

    /**
     * #494 — the constructor lowering (the biggest untargeted family claimed): a
     * {@code Type { field: value, ... }} expression lowers to the deliberately-SHALLOW
     * {@link IRConstruct} carrying the constructed type's name, the attribute names in source order
     * and the {@code ...} spread flag (see the node javadoc for the shallow/deep split and the
     * emission story — the compiler's range-correlated {@code ConstructRenderer} reuses the literal
     * {@code super.visitConstructor} fallback, byte-identical by the strongest argument).
     *
     * <p><strong>ROOT-ONLY this wave</strong> (the #492 explicit-position law taken to its extreme):
     * a constructor in ANY child position ({@code id} non-root) declines, so every child-position
     * admission allow-list stays byte-frozen by construction — no parent lowering can observe the
     * new kind. Family coverage is still whole: the seam walk roots every constructor at its own
     * {@code visitConstructor} visit (the oracle's per-pair value compiles re-enter the compiler,
     * so nested constructors claim at their own roots). NOTE the probe-channel asymmetry this
     * implies: a census tool re-adapting a nested constructor AS a root (the blocker probe's
     * bottom-up re-adapt, the #494 {@code ctorVisitValues} walk) reads it lowerable — the
     * optimistic root view, documented here rather than special-cased there.
     *
     * <p><strong>The named exceptions (#495, #496; RECURSIVE + the list-element seat since
     * #526):</strong> a claim-ROOT conditional's direct slots admit the shallow node through
     * {@link #adaptConditionalSlot} → {@link #adaptAdmittedSlot} (the #495 condVisit probe read
     * 320 {@code if <cond> then Type{...}} conditionals blocked SOLELY by this gate), a
     * claim-ROOT lambda's direct body admits it through {@link #adaptLambdaBodySlot} (the #496
     * lambdaVisit probe read 194 extract bodies), a claim-ROOT list literal's elements admit it
     * through {@link #adaptListLiteral} (the #526 neGate census read 17 element-blocked lists),
     * and — the #526 recursion — every slot REACHED through an unbroken chain of those admitted
     * GLUE seats admits at any depth (the census read the whole 38-claim pool 100%
     * glue-reachable). Each admission is itself root-gated at the CHAIN's head — a
     * chain-external parent's slots stay declined — so no OTHER parent lowering can observe the
     * kind, and the admitted node is render-inert (the parent claim renders WHOLESALE via the
     * literal legacy fallback; the emitter never walks the slot).
     *
     * <p>The only structural decline is a missing type call ({@code ctorNoTypeCall} — no
     * constructed-type fact to carry; the #494 probe read ZERO such nodes, the face is the
     * robustness belt). The typing gate is the EMITTER's ({@code type().isMissing()} declines — the
     * #491 typeMissing convention; 2 of 3,501 at the probe), so the adapter stays unconditional on
     * typing exactly like the #491 implicit-variable lowering (the cache type is stamped as read).
     */
    private Optional<IRExpr> adaptConstructor(RConstructorExpr ctor, NodeId id, RWorkspace ws) {
        if (!id.path().isEmpty()) {
            // root-only at the PLAIN dispatch — the named admissions route around this gate
            // (adaptAdmittedSlot since #526); every other child position stays byte-frozen
            return Optional.empty();
        }
        return adaptConstructorShallow(ctor, id, ws);
    }

    /**
     * The post-gate body of {@link #adaptConstructor} — the shallow build itself, shared with
     * the root-only law's named admissions (the #526 {@link #adaptAdmittedSlot} recursive
     * dispatch — carrying the #495 conditional-slot, #496 lambda-body and #526 list-element
     * seats — and the #512 {@link #adaptApply} call-ARG slot + its
     * {@link BodySeat#ARG_CTOR_ONLY} lambda-body leg), each of which admits a constructor at
     * its parent's slot and must skip the position gate while keeping every other fact
     * identical.
     */
    private Optional<IRExpr> adaptConstructorShallow(RConstructorExpr ctor, NodeId id, RWorkspace ws) {
        if (ctor.typeCall() == null) {
            return Optional.empty(); // the ctorNoTypeCall face — no constructed-type fact to carry
        }
        List<String> attributeNames = new ArrayList<>(ctor.pairs().size());
        for (RKeyValuePair pair : ctor.pairs()) {
            attributeNames.add(pair.key());
        }
        return Optional.of(new IRConstruct(ctor.typeCall().typeName(), attributeNames,
                ctor.isSpread(), id, type(ctor, ws), card(ctor, ws), Optionality.PRESENT,
                ctor.sourceRange()));
    }

    /** #492: the enclosing function's shortcut carrying {@code name} — {@link #collidesWithShortcut}'s resolving twin. */
    private static RShortcut shortcutByName(RFunction func, String name) {
        if (func == null || name == null) {
            return null;
        }
        for (RShortcut shortcut : func.shortcuts()) {
            if (name.equals(shortcut.name())) {
                return shortcut;
            }
        }
        return null;
    }

    /**
     * #492 — the alias-NAV body classifier (the {@link #aliasOperandBodyFacet} sibling; ONE walk
     * for the three nav-family seats — the {@code aliasName} receiver face, the
     * {@code featureUnresolved.aliasHead} face and the {@code receiverAlias} face — so the three
     * seats' distributions stay comparable by spelling): {@code usesOutput} FIRST (legacy's
     * {@code FunctionAliasHelper.inferShortcutMapperJavaType} hard-declines the Mapper signature
     * for an output-referencing body, and the W-class output-builder nav renders a builder, never
     * a Mapper — the retype lever the #491 guard keys on declines this class too, so an arm must
     * never admit it), then the engine channels ({@code bodyMissing} / {@code bodyMeta} / null =
     * typed meta-free, the claim-shaped state). {@code shortcutUnresolvable} is the
     * contract-impossible belt (every caller proves the shortcut by name-collision or linker
     * binding first — the #486 {@code nameMissing} precedent).
     */
    private static String aliasNavBodyFacet(RShortcut shortcut, RFunction enclosing, RWorkspace ws) {
        if (shortcut == null) {
            return "shortcutUnresolvable";
        }
        RExpression body = shortcut.expression();
        String outputName = enclosing != null && enclosing.output().isPresent()
                ? enclosing.output().get().name()
                : null;
        if (body != null && outputName != null
                && aliasBodyUsesOutput(body, outputName, enclosing, new HashSet<>())) {
            return "usesOutput";
        }
        RMetaAnnotatedType bodyType = body == null ? null : ws.getInferredType(body);
        if (bodyType == null || bodyType.isMissing()) {
            return "bodyMissing";
        }
        if (bodyType.hasMeta()) {
            return "bodyMeta";
        }
        return null;
    }

    /**
     * #492 — mirror of legacy {@code FunctionAliasHelper.walkForOutput}: whether the shortcut body
     * references the enclosing function's OUTPUT by name — a bare symbol reference OR a
     * disguised-nav head ({@code REnumValueRef.enumName()} — the W-class output-builder nav roots
     * at a fully-unresolved disguise), transparent through nested shortcuts (linker-bound or
     * name-collision-resolved; the seen set guards re-entry — AST nodes carry identity equals).
     */
    private static boolean aliasBodyUsesOutput(RNode node, String outputName, RFunction enclosing,
                                               Set<RShortcut> seen) {
        if (node == null) {
            return false;
        }
        if (node instanceof RSymbolReference sr) {
            if (outputName.equals(sr.name())) {
                return true;
            }
            RShortcut nested = sr.symbol().orElse(null) instanceof RShortcut bound
                    ? bound
                    : sr.symbol().isEmpty() ? shortcutByName(enclosing, sr.name()) : null;
            if (nested != null && seen.add(nested)
                    && aliasBodyUsesOutput(nested.expression(), outputName, enclosing, seen)) {
                return true;
            }
        }
        if (node instanceof REnumValueRef evr && outputName.equals(evr.enumName())) {
            return true;
        }
        for (RNode child : node.children()) {
            if (aliasBodyUsesOutput(child, outputName, enclosing, seen)) {
                return true;
            }
        }
        return false;
    }

    /**
     * #492 — the feature-resolution axis over a TYPED alias body (the {@code bodyTyped} face's
     * second axis at the nav seats): whether the nav's feature name resolves on the body's engine
     * type — {@code featureOnBody} (the claim-shaped state — {@link #adaptAliasHeadNav}'s
     * admission, so this spelling is a DRIFT face on any post-teach probe read) /
     * {@code featureOnBodyMeta} (resolves but meta-annotated — the arm's feature gate declines) /
     * {@code featureTypeMissing} (resolves non-meta but the attribute channel cannot type the hop
     * — the arm's last gate, restated so the mirror stays the arm's exact twin) /
     * {@code featureOffBody} (no such member, or a non-data body type — record features and model
     * gaps).
     */
    private static String aliasNavFeatureFacet(RShortcut shortcut, String featureName, RWorkspace ws) {
        RExpression body = shortcut.expression();
        RMetaAnnotatedType bodyType = body == null ? null : ws.getInferredType(body);
        RDataType dataType = bodyType != null && bodyType.type() instanceof RDataTypeRef dt
                ? dt.astNode()
                : null;
        RAttribute member = dataType == null || featureName == null
                ? null
                : findAttributeOnDataTypeByName(dataType, featureName);
        if (member == null) {
            return "featureOffBody";
        }
        if (isMetaAnnotated(member)) {
            return "featureOnBodyMeta";
        }
        RMetaAnnotatedType hopType = ws.getInferredAttributeType(member);
        if (hopType == null || hopType.isMissing()) {
            return "featureTypeMissing";
        }
        return "featureOnBody";
    }

    /**
     * #492 — the disguised alias-head nav arm ({@code <shortcut> -> <feature>} parsed as
     * {@link REnumValueRef}, the L-032 teach): the head resolves to an enclosing SHORTCUT by name
     * (legacy {@code isAliasReference}'s name-match fallback — {@code resolveNameInFunction} nulls
     * a shortcut match by legacy's own contract, so the #478 equivalent can never carry a
     * feature), the shared {@link #aliasNavBodyFacet} walk admits the body (typed, meta-free, not
     * output-referencing) and the feature resolves NON-META with a typeable hop on the body's
     * engine data type (the probed {@code aliasHead.bodyTyped.featureOnBody} face — the arm's
     * gates ≡ the facet's spellings, twin-exact). Lowers to a single-hop {@link FieldAccess} over
     * the body-retyped {@code IRReference{ALIAS}}: the render is legacy's own interior form
     * ({@code aliasName(inputs).<Leaf>map("getLeaf", bodyTypeVar -> bodyTypeVar.getLeaf())}),
     * served by the emitter's alias-operand + field-access seats; the hop type comes from the
     * FEATURE's attribute channel and the receiver from the BODY's engine channels (the #478/#479
     * cache-boundary law — the disguise's own node type is engine-MISSING through the alias
     * head). The {@code FunctionAliasHelper}-retype subset the ENGINE reads clean (the #326 META
     * wrapper class — the engine body read is the WEAKER oracle, the #491 ring's own decode)
     * delegates WHOLE at the Java-side guard's {@code ALIAS_NAV_RECEIVER_META} arm on the exact
     * legacy lever ({@code tryAliasReceiverMapperType}). Empty off-slice: the caller falls
     * through to the equivalent path, whose mirror names the decline face.
     */
    private Optional<IRExpr> adaptAliasHeadNav(REnumValueRef enr, RFunction enclosing, NodeId id,
                                               RWorkspace ws) {
        RShortcut shortcut = enclosing == null ? null : shortcutByName(enclosing, enr.enumName());
        if (shortcut == null) {
            return Optional.empty();
        }
        return aliasHeadNavLowering(shortcut, enclosing, enr.enumName(), enr.valueName(),
                enr, id, ws);
    }

    /**
     * #508 — the alias-head nav CORE, extracted from {@link #adaptAliasHeadNav} (the #505
     * name-keyed-core precedent) and shared VERBATIM by the two seats that spell the same
     * navigation: the disguised {@link REnumValueRef} seat (the #492 arm — {@code head ->
     * feature} with the head resolving to an enclosing shortcut by name) and the PARSED
     * {@link RFeatureCall} seat (the #508 arm-B1/arm-A2 teach — the same navigation the parser
     * kept as a feature call, 143 plain + 67 meta sole at the #507 SOT, the metaHopGate census
     * reading BOTH seats' heads 100% name-collision with typed bodies and typed hops). The
     * shared {@link #aliasNavBodyFacet} walk admits the body (typed, meta-free, not
     * output-referencing), then the feature resolves on the body's engine data type and the
     * nav lowers over the body-retyped {@code IRReference{ALIAS}} receiver:
     * <ul>
     *   <li><strong>the PLAIN leg</strong> (the #492 render, byte-unchanged): a NON-meta
     *       feature with a typeable hop lowers to the single-hop {@link FieldAccess} —
     *       legacy's own interior form ({@code aliasName(inputs).<Leaf>map(...)}), the
     *       emitter's ring-proven alias-operand + field-access seats;</li>
     *   <li><strong>the META leg</strong> (the #508 arm-A2 teach of the {@code
     *       featureOnBodyMeta} residue): a META-annotated feature lowers to the DISTINCT
     *       {@link IRMetaAccess} over the SAME retyped receiver (the #499 meta arm's build —
     *       qualifiers from the feature's annotations, NO hop-type gate: the #497 type-blind
     *       delegation posture). The render is the standing meta routing law's — a
     *       meta-bearing claim ROOT renders WHOLE through the compiler's oracle-root serve
     *       (the literal legacy line, byte-identical BY IDENTITY; at the L-111 seat the #507
     *       identity-serve leg carries the root), an interior hop rides its containing
     *       root's serve — so legacy's FieldWithMetaX projection machinery stays legacy's
     *       own (the L-029 split).</li>
     * </ul>
     * Empty off-slice (body facet non-null / feature off-body / plain-leg hop untypeable):
     * the callers fall through and the mirrors keep the probed decline spellings.
     */
    private Optional<IRExpr> aliasHeadNavLowering(RShortcut shortcut, RFunction enclosing,
            String headName, String leafName, RExpression node, NodeId id, RWorkspace ws) {
        String navBodyFace = aliasNavBodyFacet(shortcut, enclosing, ws);
        if (navBodyFace != null) {
            // #518 arm-B2 — the usesOutput teach: an OUTPUT-consuming alias head lowers to
            // the DISTINCT shallow IROutputAliasNav oracle leaf (the face every
            // Mapper-shaped arm must never admit — legacy renders the BUILDER walk, so the
            // FieldAccess-over-ALIAS shape and its native Mapper compose stay byte-frozen
            // by construction; the #499 distinct-kind law). No body/feature resolution: the
            // node is name-only (the IRRuleInputNav shallow pattern) with the type/card
            // channels stamped as read from the node's own engine channels (the #491
            // convention — nothing reads them: at the L-111 relabel seat the #507
            // identity-serve leg renders the LITERAL legacy line, at the probed roots the
            // standing enumChain/navChain oracle serves render whole-legacy, and an
            // interior occurrence routes its containing root through containsOracleLeaf).
            // bodyMissing / bodyMeta / shortcutUnresolvable keep the honest decline.
            if ("usesOutput".equals(navBodyFace) && leafName != null) {
                return Optional.of(new IROutputAliasNav(headName, leafName, id,
                        type(node, ws), card(node, ws), Optionality.PRESENT,
                        node.sourceRange()));
            }
            return Optional.empty();
        }
        RExpression body = shortcut.expression();
        RMetaAnnotatedType bodyType = ws.getInferredType(body);
        // #509 arm-A2: the CHOICE-typed alias body (the #508 aliasIdGate census read the
        // offBody face 100% choice-typed with the leaf resolving on the choice's own options —
        // Underlier 38 · Payout 18, cdm6). The option resolves by TYPE NAME against the
        // choice's OWN options (the option's typeCall IS its identity — the #503 arm-B1 /
        // legacy findChoiceSuperOption law), and BOTH legs serve BY IDENTITY at both seats —
        // the identity-serve-first law at a new render position (an alias-receiver option nav
        // is NOT the #503 ring-proven resolved-receiver compose):
        //   - a META-annotated option lowers to IRMetaAccess over the retyped alias receiver
        //     (the standing meta routing law — a meta-bearing claim root renders WHOLE through
        //     the oracle-root serve);
        //   - a PLAIN option lowers to the DISTINCT shallow IRChoiceOptionNav (the #505 kind
        //     at the alias-head flavor — an ORACLE LEAF: no native emitter arm exists, so the
        //     claim root and every containing root render the literal legacy line BY IDENTITY,
        //     legacy's own alias + choice-projection machinery inside it).
        if (bodyType != null && bodyType.type() instanceof RChoiceTypeRef bodyChoiceRef
                && bodyChoiceRef.astNode() != null && leafName != null) {
            RChoiceOption option = null;
            for (RChoiceOption opt : bodyChoiceRef.astNode().options()) {
                if (opt.typeCall() != null && leafName.equals(opt.typeCall().typeName())) {
                    option = opt;
                    break;
                }
            }
            if (option == null) {
                return Optional.empty(); // featureOffBody — the honest residue
            }
            ExpressionCardinality choiceBodyCard = ws.getRuleBodyCardinality(body);
            IRReference choiceAliasRecv = new IRReference(headName,
                    IRReference.ReferenceKind.ALIAS, id.child(0), bodyType, choiceBodyCard,
                    Optionality.PRESENT, node.sourceRange());
            java.util.TreeSet<String> optQuals = new java.util.TreeSet<>();
            for (RAnnotationRef ar : option.annotationRefs()) {
                if ("metadata".equals(ar.annotationName())) {
                    optQuals.add(ar.qualifierName().orElse("bare"));
                }
            }
            if (!optQuals.isEmpty()) {
                return Optional.of(new IRMetaAccess(choiceAliasRecv, leafName,
                        List.copyOf(optQuals), id, type(node, ws), choiceBodyCard,
                        ExpressionCardinality.SINGLE,
                        Optionality.OPTIONAL, node.sourceRange()));
            }
            return Optional.of(new IRChoiceOptionNav(headName, leafName, id, type(node, ws),
                    choiceBodyCard, Optionality.PRESENT, node.sourceRange()));
        }
        RDataType bodyDataType = bodyType != null && bodyType.type() instanceof RDataTypeRef dt
                ? dt.astNode()
                : null;
        RAttribute feature = bodyDataType == null || leafName == null ? null
                : findAttributeOnDataTypeByName(bodyDataType, leafName);
        if (feature == null) {
            return Optional.empty(); // featureOffBody — the honest residue
        }
        // The FAITHFUL cardinality channel: the global compute() reads every disguised
        // REnumValueRef chain as conservative SINGLE (its thenAware=false arm), and alias bodies
        // ARE overwhelmingly disguised chains (`trade -> legs`) — an under-read MULTI would let a
        // multi alias chain pass the scalar operand gates. getRuleBodyCardinality is the
        // thenAware computer that resolves the disguised walk (its javadoc names this consumer).
        ExpressionCardinality bodyCard = ws.getRuleBodyCardinality(body);
        IRReference aliasRecv = new IRReference(headName, IRReference.ReferenceKind.ALIAS,
                id.child(0), bodyType, bodyCard, Optionality.PRESENT, node.sourceRange());
        ExpressionCardinality featureCard = isMultiCardinality(feature)
                ? ExpressionCardinality.MULTI : ExpressionCardinality.SINGLE;
        ExpressionCardinality accumulatedCard = (featureCard == ExpressionCardinality.MULTI
                || bodyCard == ExpressionCardinality.MULTI)
                ? ExpressionCardinality.MULTI : ExpressionCardinality.SINGLE;
        if (isMetaAnnotated(feature)) {
            // The #508 arm-A2 META leg — see the javadoc. Type from the node's own engine
            // channel (the census read every live row typed; the #499 meta arm's own
            // unconditional sourcing — no gate, the type-blind posture).
            return Optional.of(new IRMetaAccess(aliasRecv, leafName,
                    metaQualifierNames(feature), id, type(node, ws), accumulatedCard,
                    featureCard, chainOptionality(aliasRecv, feature), node.sourceRange()));
        }
        RMetaAnnotatedType hopType = ws.getInferredAttributeType(feature);
        if (hopType == null || hopType.isMissing()) {
            return Optional.empty(); // featureTypeMissing — the emitter could not name the witness
        }
        return Optional.of(new FieldAccess(aliasRecv, leafName, id, hopType,
                accumulatedCard, featureCard, chainOptionality(aliasRecv, feature),
                node.sourceRange()));
    }

    /**
     * #509 arm-A1 — the parsed-seat alias-nav GUARD-HELD query (the identity-serve leg's own
     * gate, twin-exact with the #508 claim site's null-out): TRUE exactly when the
     * {@link #adaptFeatureCall} alias recovery WOULD lower a plain {@link FieldAccess} over
     * the alias body (the shared {@link #aliasHeadNavLowering} core admits) but the STRONG
     * body allowlist ({@link #isProvablyNonMetaElementSource}) cannot prove the body's
     * element form — the #326 weaker-oracle class the claim site declines to render natively
     * (the #508 probe2 catch). The compiler's belt leg serves this slice by rendering the
     * LITERAL legacy fallback line ({@code super.visitFeatureCall} — the same call the
     * decline takes, byte-identical BY IDENTITY) and counts it LOWERED on its own receipt
     * (the #507 L-111 identity-serve pattern at the RFeatureCall seat). Read-only.
     */
    public boolean isGuardHeldParsedAliasNav(RFeatureCall fc, RWorkspace ws) {
        if (fc.resolvedFeature().isPresent() || fc.featureName() == null || ws == null
                || !(fc.receiver() instanceof RSymbolReference head)) {
            return false;
        }
        RShortcut shortcut = head.symbol().orElse(null) instanceof RShortcut bound ? bound
                : head.symbol().isEmpty()
                        ? shortcutByName(enclosingFunction(fc), head.name())
                        : null;
        if (shortcut == null) {
            return false;
        }
        Optional<IRExpr> nav = aliasHeadNavLowering(shortcut, enclosingFunction(fc),
                head.name(), fc.featureName(), fc, NodeId.ROOT, ws);
        return nav.isPresent() && nav.get() instanceof FieldAccess
                && !isProvablyNonMetaElementSource(shortcut.expression());
    }

    /**
     * #492 — the alias-receiver RETYPE at the adapter boundary (the #479 cache-boundary pattern at
     * the L-032 seat): a lowered {@code IRReference{ALIAS}} receiver always reads MISSING through
     * the expression-node-keyed type cache (the engine types the shortcut BODY, never the
     * REFERENCE), which the emitter declines ("cannot name the lambda var"). The admissible slice
     * — the shared {@link #aliasNavBodyFacet} walk reads the body typed, meta-free and not
     * output-referencing — rebuilds the reference with the BODY's engine type and cardinality
     * (exactly the channels legacy's {@code FunctionAliasHelper} signature walk and
     * {@code NavigationHandler.resolveReceiverDataType} lambda-naming derive from on the render
     * side). Null off-slice: the caller declines and {@link #receiverToken} names the face.
     */
    private static IRExpr retypedAliasNavReceiver(IRExpr lowered, RExpression rawReceiver,
                                                  RWorkspace ws) {
        if (!(lowered instanceof IRReference ref)
                || ref.referenceKind() != IRReference.ReferenceKind.ALIAS
                || !(rawReceiver instanceof RSymbolReference sr)
                || !(sr.symbol().orElse(null) instanceof RShortcut shortcut)) {
            return null;
        }
        RFunction enclosing = enclosingFunction(sr);
        if (aliasNavBodyFacet(shortcut, enclosing, ws) != null) {
            return null; // usesOutput / bodyMissing / bodyMeta — the honest decline faces
        }
        RExpression body = shortcut.expression();
        // The faithful cardinality channel — see adaptAliasHeadNav's bodyCard note.
        return new IRReference(ref.target(), IRReference.ReferenceKind.ALIAS, ref.nodeId(),
                ws.getInferredType(body), ws.getRuleBodyCardinality(body), ref.optionality(),
                sr.sourceRange());
    }

    /**
     * #492 — the {@code featureUnresolved} head decode (ONE seat, both composing channels: the
     * disguised {@code inputFeatureNav} equivalent AND the parsed {@code RFeatureCall} mirror
     * compose it): the head class first — {@code aliasHead} (linker-bound shortcut OR the
     * name-collision fallback — legacy {@code isAliasReference}'s own dual path) with the shared
     * body/feature walk sizing the claim-shaped slice; {@code headAttr} (a resolved attribute
     * head whose declared type carries no such member — record features and unresolvable declared
     * types); {@code headOther} (a non-attribute, non-shortcut symbol); {@code headUnresolved}
     * (no symbol, no collision); {@code nonSymbolReceiver} (a compound receiver — the feature
     * gate fires before the receiver ever adapts).
     */
    private static String featureUnresolvedFacet(RFeatureCall fc, RWorkspace ws) {
        if (!(fc.receiver() instanceof RSymbolReference head)) {
            return "nonSymbolReceiver";
        }
        RNode headSymbol = head.symbol().orElse(null);
        RShortcut shortcut = headSymbol instanceof RShortcut bound ? bound : null;
        if (shortcut == null && headSymbol == null) {
            RFunction enclosing = enclosingFunction(head);
            shortcut = enclosing == null ? null : shortcutByName(enclosing, head.name());
        }
        if (shortcut == null) {
            if (headSymbol == null) {
                return "headUnresolved";
            }
            if (headSymbol instanceof RAttribute headAttrSym) {
                // #514: the flat token refined IN PLACE (the #491 pattern): the head's
                // declared-type ladder — a RECORD head whose record carries the leaf (the
                // #507 record-read class at the PARSED seat), a record without it, an
                // underivable declared type, or a data type without such a member.
                RRecordType headRec = declaredRecordTypeOf(headAttrSym);
                if (headRec != null) {
                    for (RRecordFeature f : headRec.features()) {
                        if (f.name().equals(fc.featureName())) {
                            return "headAttr.recHit";
                        }
                    }
                    return "headAttr.recMiss";
                }
                if (declaredDataTypeOf(headAttrSym) == null) {
                    // #529: the choice-head twin — the arm's own SYMBOL-head channel
                    // restated BY the same reads (a declared CHOICE whose options carry
                    // the feature BY TYPE NAME with a resolvable referenced type). The
                    // claimed shape reads UNATTRIBUTED (the arm claims; the receiver-
                    // lowering leg is the arm's only residual gate, and a bound head that
                    // fails to lower lands on the unattributed meter — the designed
                    // tripwire). The residue keeps the honest declMiss spelling.
                    if (headAttrSym.typeCall() != null
                            && headAttrSym.typeCall().referencedType()
                                    .orElse(null) instanceof RChoice headChoiceT
                            && fc.featureName() != null) {
                        for (RChoiceOption option : headChoiceT.options()) {
                            RTypeCall optionCall = option.typeCall();
                            if (optionCall != null
                                    && fc.featureName().equals(optionCall.typeName())
                                    && (optionCall.referencedType()
                                            .orElse(null) instanceof RDataType
                                            || optionCall.referencedType()
                                                    .orElse(null) instanceof RChoice)) {
                                return UNATTRIBUTED;
                            }
                        }
                    }
                    return "headAttr.declMiss";
                }
                return "headAttr.memberMiss";
            }
            return "headOther";
        }
        String bodyFace = aliasNavBodyFacet(shortcut, enclosingFunction(head), ws);
        if (bodyFace != null) {
            return "aliasHead." + bodyFace;
        }
        return "aliasHead.bodyTyped." + aliasNavFeatureFacet(shortcut, fc.featureName(), ws);
    }


    /**
     * #491 — the alias-operand ADMISSION (the L-035 arithmetic-operand law extended to the
     * EQUALITY and EXISTENCE gates, decoded face-by-face at {@link #operandToken}): a lowered
     * {@code ALIAS} operand admits when the {@link #aliasOperandBodyFacet} walk proves the
     * shortcut body typed and meta-free ({@code null}). The #503 arm-A3 RETIRED the
     * equality-seat enumSibling carve-out that once rode here: the alias node now carries the
     * BODY-channel type (sourced at both mint arms), so the emitter's
     * {@code enumOperandRequalifies} owns the residual gate — same-enum pairs drive,
     * divergent/unresolvable pairs decline byte-safely. The render is then
     * legacy's bare context-free {@code aliasName(inputs)}, which the emitter's
     * {@code emitAliasOperand} reproduces byte-identically via the per-emission resolver —
     * EXCEPT the {@code FunctionAliasHelper}-retype subset at EQUALITY (the #326 META / #334
     * basic-scalar classes: this engine-side body read is the WEAKER oracle, the #491 ring's
     * own decode), which the Java-side guard trips on the exact legacy lever and delegates
     * whole; unconditional at existence (no retype lever there). The #491 probe read the
     * admitted slice at existence clean 402 + equality clean 228 sole, with comparison
     * (the #372 {@code comparisonIntWiden} body-walk lever) and logical (the
     * {@code ComparisonResult.ofNullSafe} coercion family) declining EVERY alias face by design.
     */
    private static boolean isAdmittedAliasOperand(IRExpr lowered, RExpression raw, IRExpr sibling,
                                                  RWorkspace ws) {
        // #503 arm-A3: the enumSibling exclusion RETIRED — the alias node now carries the
        // BODY-channel type (the L-032 gap's sourcing, taught at both alias mint arms), so the
        // emitter's enumOperandRequalifies compares the REAL enum classes: the same-enum pairs
        // drive through the standing native equality render (areEqual + the wrapped constant),
        // and a divergent/unresolvable pair still DECLINES at the emitter's requalify gate
        // (byte-safe — the L-029 Java-name decision stays the emitter's).
        return lowered instanceof IRReference ref
                && ref.referenceKind() == IRReference.ReferenceKind.ALIAS
                && aliasOperandBodyFacet(raw, ws) == null;
    }

    /** Mirror of {@link #adaptLogical}'s exits, in order; left operand checked before right. */
    private String reasonForLogical(RLogicalExpr log, RWorkspace ws) {
        RExpression rawLeft = log.rawLeft();
        RExpression rawRight = log.rawRight();
        if (rawLeft == null || rawRight == null) {
            return "elidedOperand";
        }
        Optional<IRExpr> left = adapt(rawLeft, ws);
        Optional<IRExpr> right = adapt(rawRight, ws);
        if (left.isEmpty() || right.isEmpty()) {
            return "operandNotExpressible";
        }
        if (!producesComparisonResult(left.get())) {
            return booleanOperandToken(left.get());
        }
        if (!producesComparisonResult(right.get())) {
            return booleanOperandToken(right.get());
        }
        return UNATTRIBUTED;
    }

    /**
     * The lowered operand form that failed {@link #producesComparisonResult} — the bare-boolean
     * shapes legacy coerces via {@code ComparisonResult.ofNullSafe} (param / navigation; the
     * CALL face is a #504 DRIFT face — the arm-A admission claims every IRApply operand, so
     * that branch fires only on twin divergence), the boolean literal, alias, item, or the
     * generic {@code operand:<IRClass>}.
     */
    private static String booleanOperandToken(IRExpr lowered) {
        if (lowered instanceof IRVariable var) {
            if (var.variableKind() == IRVariable.VariableKind.PARAM) {
                return "operandBareBooleanParam";
            }
            if (var.variableKind() == IRVariable.VariableKind.USER_ITEM
                    || var.variableKind() == IRVariable.VariableKind.SYNTHETIC_ITEM) {
                return "operandItem";
            }
        }
        if (lowered instanceof FieldAccess) {
            return "operandBareBooleanNav";
        }
        if (lowered instanceof IRApply) {
            // #504: a DRIFT face — producesComparisonResult admits IRApply (arm A), so this
            // fires only if the gate and this token ever diverge (the stable-token belt).
            return "operandBareBooleanCall";
        }
        if (lowered instanceof IRLiteral) {
            return "operandBooleanLiteral";
        }
        if (lowered instanceof IRReference ref
                && ref.referenceKind() == IRReference.ReferenceKind.ALIAS) {
            return "operandAlias";
        }
        return "operand:" + lowered.getClass().getSimpleName();
    }

    /** Mirror of {@link #adaptExistence}'s exits, in order. */
    private String reasonForExistence(RExistenceExpr exist, RWorkspace ws) {
        RExpression rawArg = exist.argument();
        if (rawArg == null) {
            // #519: the elided branch mirrors the synthesis — the facets name the walk's own
            // honest stops (the facet-first law); the bare token is the drift belt (the arm
            // synthesizes exactly the facet-free slice, so a bare-token row means the arm and
            // this mirror diverged).
            ElidedImplicitBinding binding = elidedImplicitBinding(exist);
            if (binding.declineFacet() != null) {
                return binding.declineFacet();
            }
            RMetaAnnotatedType sourceType = type(binding.source(), ws);
            if (sourceType == null || sourceType.isMissing()) {
                return "elidedOperand.srcTypeMissing";
            }
            return "elidedOperand";
        }
        Optional<IRExpr> arg = adapt(rawArg, ws);
        if (arg.isEmpty()) {
            return "operandNotExpressible";
        }
        if (!(isExistenceOperand(arg.get())
                || isFilterExtractBoundItemOperand(arg.get(), rawArg)
                || isAdmittedAliasOperand(arg.get(), rawArg, null, ws)
                // #504 arm-A: the point-free + syn-item operand admissions — mirror the arm.
                || arg.get() instanceof IRPointFreeApply
                || arg.get() instanceof IRSynItemNav
                // #508 arm-A5: the meta-item operand admission — mirror the arm.
                || arg.get() instanceof IRMetaItemNav
                // #640: RE-SYNCED with the arm (four kinds behind it since #505 / #507 - D54 item 3's first act)
                || arg.get() instanceof IRChoiceOptionNav
                || arg.get() instanceof IRDispatchInputRef
                || arg.get() instanceof IRDeepFeatureNav
                || arg.get() instanceof IRRecordFeatureNav)) {
            return operandToken(arg.get(), rawArg, null, ws);
        }
        return UNATTRIBUTED;
    }

    /** Mirror of {@link #adaptArithmetic}'s exits, in order; left operand checked before right. */
    private String reasonForArithmetic(RArithmeticExpr arith, RWorkspace ws) {
        RExpression rawLeft = arith.rawLeft();
        RExpression rawRight = arith.rawRight();
        if (rawLeft == null || rawRight == null) {
            return "unaryPrefix";
        }
        Optional<IRExpr> left = adapt(rawLeft, ws);
        Optional<IRExpr> right = adapt(rawRight, ws);
        if (left.isEmpty() || right.isEmpty()) {
            return "operandNotExpressible";
        }
        if (!isArithmeticOperand(left.get())) {
            return arithmeticOperandToken(left.get());
        }
        if (!isArithmeticOperand(right.get())) {
            return arithmeticOperandToken(right.get());
        }
        return UNATTRIBUTED;
    }

    /**
     * The lowered operand form that failed {@link #isArithmeticOperand}: since the #517
     * operand-cluster admissions the call / count / item / non-numeric-literal / to-string /
     * synthetic-item-nav / record-read tokens are DEFENSIVE BELTS (census-zero — the #486
     * stable-token precedent; a row reappearing means an admission regressed); the live
     * residue faces are {@code operandMultiParam} (a MULTI-cardinality param — the honest
     * decline) and the generic {@code operand:<IRClass>} for any future un-admitted kind.
     */
    private static String arithmeticOperandToken(IRExpr lowered) {
        if (lowered instanceof FieldAccess) {
            return "operandNav";
        }
        if (lowered instanceof IRApply) {
            return "operandCall";
        }
        if (lowered instanceof IRListOp listOp && listOp.op() == IRListOp.Kind.COUNT) {
            return "operandCount";
        }
        if (lowered instanceof IRVariable var) {
            if (var.variableKind() == IRVariable.VariableKind.USER_ITEM
                    || var.variableKind() == IRVariable.VariableKind.SYNTHETIC_ITEM) {
                return "operandItem";
            }
            if (var.variableKind() == IRVariable.VariableKind.PARAM
                    && var.cardinality() == ExpressionCardinality.MULTI) {
                return "operandMultiParam";
            }
        }
        return "operand:" + lowered.getClass().getSimpleName();
    }

    /** Mirror of {@link #adaptListOp}'s exits: every operator is taught since the #519 sum
     * admission (only-element joined at #498, sum at #519 — the opSum token retired with the
     * face), so the receiver gate is the only exit. */
    private String reasonForListOp(RListOpExpr expr, RWorkspace ws) {
        // #496 (Seat-1 OBS-1): the receiver check routes through slotNotExpressibleAtChild — the
        // arm adapts the argument at a child NodeId, so a position-divergent receiver (the
        // claim-root admission classes) attributes here instead of leaking UNATTRIBUTED.
        if (slotNotExpressibleAtChild(expr.argument(), ws)) {
            return "receiverNotExpressible";
        }
        return UNATTRIBUTED;
    }

    /**
     * Mirror of {@link #adaptConditional}'s exits, in slot order (condition → then → genuine
     * else). The mirror re-adapts each slot at the public entry's ROOT position while the real
     * recursion runs child positions — and the #494 ctor arm is position-gated (a root re-adapt
     * lowers what the child position declines), so a slot that IS a constructor hard-attributes
     * to its slot exit (the #494 list-arm convention): a conditional this mirror is queried FOR
     * declined in the real recursion, where a ctor slot lowers only via the #495 claim-root
     * admission — the queried population's ctor slots are the UN-admitted child-position class.
     * Two documented boundaries: (1) an admitted ROOT whose LATER slot blocked would
     * hard-attribute the ctor slot first — unreachable on the frozen corpus (every
     * {@code blocked.ctorSlot} probe face is else-less with a lowerable condition); (2) a NESTED
     * conditional slot re-adapted at root re-enters the admission (the optimistic-root asymmetry
     * documented at {@link #adaptConstructor}) and can read lowerable, falling through to
     * {@code UNATTRIBUTED} — the unattributed meter carries that residue honestly.
     */
    private String reasonForConditional(RConditionalExpr cond, RWorkspace ws) {
        if (slotNotExpressibleAtChild(cond.condition(), ws)) {
            return "conditionNotExpressible";
        }
        if (slotNotExpressibleAtChild(cond.thenBranch(), ws)) {
            return "thenNotExpressible";
        }
        if (hasGenuineElse(cond) && slotNotExpressibleAtChild(cond.elseBranch().get(), ws)) {
            return "elseNotExpressible";
        }
        return UNATTRIBUTED;
    }

    /**
     * #496 — mirror of {@link #adaptLambdaOp}'s exits, in order (belts → receiver → body). BOTH
     * child slots take {@link #slotNotExpressibleAtChild} (the #495 mirror-slot verdict, the
     * Seat-1 MF-1 sweep — the receiver adapts at {@code id.child(0)} in the arm, so a
     * position-divergent receiver is exactly as attributable as a divergent body): a direct-ctor
     * body lowers at a probe re-root — and at a claim ROOT via the #496 body-slot admission —
     * but declines at the real recursion's child NodeId, so for attribution it IS the
     * non-expressible slot (a lambda this mirror is queried FOR declined in the real recursion,
     * where the admission fires at claim roots only — the queried population's ctor bodies are
     * the un-admitted child-position class, the same boundary argument as
     * {@link #reasonForConditional}'s). No typing exit — the arm is unconditional on typing (the
     * emitter's gate, the #491 convention).
     */
    private String reasonForLambdaOp(RExpression argument, RInlineFunction body, RWorkspace ws) {
        if (argument == null || body == null || body.body() == null) {
            return "lambdaNullSlot";
        }
        if (body.paramNames().size() > 1) {
            return "multiParamBinder";
        }
        if (slotNotExpressibleAtChild(argument, ws)) {
            return "receiverNotExpressible";
        }
        if (slotNotExpressibleAtChild(body.body(), ws)) {
            return "bodyNotExpressible";
        }
        return UNATTRIBUTED;
    }

    /**
     * #495 — one mirror slot's child-semantics verdict: non-expressible when the root re-adapt
     * declines OR the slot is position-divergent (it lowers at the probe's ROOT position via
     * the claim-root admissions yet declines at the real recursion's child NodeId). Since #526
     * BOTH legs read BY CALL — the root re-adapt and a child-seat re-adapt ({@code
     * NodeId.ROOT.child(0)} — any non-empty path engages the #494 position gate and disengages
     * the admissions) — replacing the #495/#496-era structural class list ({@code ctor} /
     * {@link #isCtorSlotConditional} / {@link #isCtorBodyLambda}, equivalent one-level reads
     * pre-#526): the admitted-slot recursion made the divergence ARBITRARY-depth, and a
     * structural mirror would desync at depth (the census's {@code then.ctor.atRoot} rows were
     * that desync's one-level preview). Any admission the arm gains is reflected here
     * automatically — the mirror cannot drift. Public since #526 for the neGate census's
     * per-element BY-CALL read (the #524 census BY-CALL law); read-only, probe-only callers.
     */
    public boolean slotNotExpressibleAtChild(RExpression slot, RWorkspace ws) {
        return adapt(slot, ws).isEmpty()
                || adapt(slot, NodeId.ROOT.child(0), ws).isEmpty();
    }

    /**
     * #495 — the arm-mirrors' second position-divergent class (the #494 R2 fix's sibling): a
     * conditional whose DIRECT slot is a constructor lowers when re-adapted as a probe ROOT (the
     * claim-root ctor-slot admission fires there) but declines at a child NodeId (the admission
     * is root-gated), so for attribution it IS the non-expressible child — without this the
     * first #495 ring read the mirror's unattributed meter live ({@code RListLiteral:unattributed=2},
     * drr FUNCTION: lists whose conditional element carries a ctor then-slot). Deeper nestings
     * need no walk — a conditional whose slot is itself a ctor-slot conditional declines at the
     * probe root too (the nested conditional sits at a child position there), so the divergence
     * is exactly ONE level. Since #526 the live mirror ({@link #slotNotExpressibleAtChild})
     * routes BY CALL through the seat re-adapts and no longer consults this predicate — it
     * stays public as the neGate census's SHAPE-classification vocabulary (the one-level
     * {@code condCtorSlot} class read; the #524 census BY-CALL law); read-only.
     */
    public static boolean isCtorSlotConditional(RExpression element) {
        if (!(element instanceof RConditionalExpr cond)) {
            return false;
        }
        return cond.condition() instanceof RConstructorExpr
                || cond.thenBranch() instanceof RConstructorExpr
                || (hasGenuineElse(cond) && cond.elseBranch().get() instanceof RConstructorExpr);
    }

    /**
     * #496 — the arm-mirrors' third position-divergent class (the {@link #isCtorSlotConditional}
     * sibling at the lambda families): an extract/filter whose DIRECT body is a constructor
     * lowers when re-adapted as a probe ROOT (the #496 claim-root body-slot admission fires
     * there) but declines at a child NodeId (the admission is root-gated), so for attribution it
     * IS the non-expressible child. One level exactly, by the same argument. Since #526 the
     * live mirror ({@link #slotNotExpressibleAtChild}) routes BY CALL through the seat
     * re-adapts and no longer consults this predicate — it stays public as the neGate census's
     * SHAPE-classification vocabulary (the one-level {@code lamCtorBody} class read; the #524
     * census BY-CALL law); read-only.
     */
    public static boolean isCtorBodyLambda(RExpression element) {
        RInlineFunction body = element instanceof RExtractExpr extract ? extract.body()
                : element instanceof RFilterExpr filter ? filter.body() : null;
        return body != null && body.body() instanceof RConstructorExpr;
    }

    private static RMetaAnnotatedType type(RExpression expr, RWorkspace ws) {
        return ws.getInferredType(expr);
    }

    private static ExpressionCardinality card(RExpression expr, RWorkspace ws) {
        return ws.getCardinality(expr);
    }
}
