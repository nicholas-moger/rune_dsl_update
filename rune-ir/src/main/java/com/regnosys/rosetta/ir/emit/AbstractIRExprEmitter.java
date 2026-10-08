package com.regnosys.rosetta.ir.emit;

import com.regnosys.rosetta.ir.expr.BinaryOp;
import com.regnosys.rosetta.ir.expr.Existence;
import com.regnosys.rosetta.ir.expr.FieldAccess;
import com.regnosys.rosetta.ir.expr.IRApply;
import com.regnosys.rosetta.ir.expr.IRConditional;
import com.regnosys.rosetta.ir.expr.IRConstruct;
import com.regnosys.rosetta.ir.expr.IRConversion;
import com.regnosys.rosetta.ir.expr.IREmptyLiteral;
import com.regnosys.rosetta.ir.expr.IRLambdaOp;
import com.regnosys.rosetta.ir.expr.IRExpr;
import com.regnosys.rosetta.ir.expr.IRExprKind;
import com.regnosys.rosetta.ir.expr.IRListConstruct;
import com.regnosys.rosetta.ir.expr.IRListOp;
import com.regnosys.rosetta.ir.expr.IRLiteral;
import com.regnosys.rosetta.ir.expr.IRMetaAccess;
import com.regnosys.rosetta.ir.expr.IROnlyExists;
import com.regnosys.rosetta.ir.expr.IRPipe;
import com.regnosys.rosetta.ir.expr.IRReference;
import com.regnosys.rosetta.ir.expr.IRPointFreeApply;
import com.regnosys.rosetta.ir.expr.IRAllAnyCompare;
import com.regnosys.rosetta.ir.expr.IRChoiceOptionNav;
import com.regnosys.rosetta.ir.expr.IRDeepFeatureNav;
import com.regnosys.rosetta.ir.expr.IRDispatchInputRef;
import com.regnosys.rosetta.ir.expr.IRRecordFeatureNav;
import com.regnosys.rosetta.ir.expr.IRRecordReceiverNav;
import com.regnosys.rosetta.ir.expr.IRQualifierItemNav;
import com.regnosys.rosetta.ir.expr.IRChoiceReceiverNav;
import com.regnosys.rosetta.ir.expr.IRQualifierReceiverNav;
import com.regnosys.rosetta.ir.expr.IRSwitchOp;
import com.regnosys.rosetta.ir.expr.IRDefaultOp;
import com.regnosys.rosetta.ir.expr.IRMembershipOp;
import com.regnosys.rosetta.ir.expr.IRCollectOp;
import com.regnosys.rosetta.ir.expr.IROutputRef;
import com.regnosys.rosetta.ir.expr.IRMetaParamRef;
import com.regnosys.rosetta.ir.expr.IRImplicitAttrNav;
import com.regnosys.rosetta.ir.expr.IRConditionInstance;
import com.regnosys.rosetta.ir.expr.IRRuleInputNav;
import com.regnosys.rosetta.ir.expr.IRWithMetaOp;
import com.regnosys.rosetta.ir.expr.IRJoinOp;
import com.regnosys.rosetta.ir.expr.IRLibraryApply;
import com.regnosys.rosetta.ir.expr.IROutputAliasNav;
import com.regnosys.rosetta.ir.expr.IRMetaItemNav;
import com.regnosys.rosetta.ir.expr.IRSynItemNav;
import com.regnosys.rosetta.ir.expr.IRClosureParam;
import com.regnosys.rosetta.ir.expr.IRMetaOutputApply;
import com.regnosys.rosetta.ir.expr.IRSymbolNav;
import com.regnosys.rosetta.ir.expr.IRToString;
import com.regnosys.rosetta.ir.expr.IRVariable;
import com.regnosys.rosetta.ir.expr.Let;

/**
 * Base for a neutral expression emitter. Owns the per-node <strong>dispatch</strong> (a no-{@code default}
 * {@code switch} over every {@link IRExprKind}, casting to the concrete record) and a single
 * {@code unsupported} <strong>choke point</strong>. Subclasses fill per-kind hooks; a backend overrides
 * only the kinds it supports, and every other kind declines through {@link #unsupported(IRExprKind)}.
 *
 * <p><strong>Recursion is the subclass's job.</strong> The base does single-level dispatch only; a hook
 * recurses by calling {@link #emit(IRExpr)} on its children (e.g. a binary-op hook calls
 * {@code emit(node.left())}). The post-order walk thus <em>emerges</em> from hooks re-entering
 * {@code emit}; the base owns neither child traversal nor result-combination.
 *
 * <p><strong>No {@code default} arm</strong> (a deliberate divergence from the printer/serializer, which
 * keep a graceful {@code default}, and from {@code IRExprKind}'s strangler javadoc). A new
 * {@link IRExprKind} makes the {@code switch} non-exhaustive — a <em>compile</em> error here, the single
 * point a maintainer wires a new kind. That is correct for a neutral target, which has no legacy to
 * delegate to; "decline a kind I don't support" is the separate, runtime per-kind default below.
 *
 * @param <T> the target output form
 */
public abstract class AbstractIRExprEmitter<T> implements IRExprEmitter<T> {

    @Override
    public final T emit(IRExpr node) {
        return switch (node.kind()) {
            case LITERAL        -> emitLiteral((IRLiteral) node);
            case LIST_CONSTRUCT -> emitListConstruct((IRListConstruct) node);
            case EMPTY_LITERAL  -> emitEmptyLiteral((IREmptyLiteral) node);
            case VARIABLE       -> emitVariable((IRVariable) node);
            case REFERENCE      -> emitReference((IRReference) node);
            case APPLY          -> emitApply((IRApply) node);
            case BINARY_OP      -> emitBinaryOp((BinaryOp) node);
            case EXISTENCE      -> emitExistence((Existence) node);
            case FIELD_ACCESS   -> emitFieldAccess((FieldAccess) node);
            case META_ACCESS    -> emitMetaAccess((IRMetaAccess) node);
            case LIST_OP        -> emitListOp((IRListOp) node);
            case CONDITIONAL    -> emitConditional((IRConditional) node);
            case LET            -> emitLet((Let) node);
            case TO_STRING      -> emitToString((IRToString) node);
            case POINT_FREE_APPLY -> emitPointFreeApply((IRPointFreeApply) node);
            case CONSTRUCT      -> emitConstruct((IRConstruct) node);
            case LAMBDA_OP      -> emitLambdaOp((IRLambdaOp) node);
            case CONVERSION     -> emitConversion((IRConversion) node);
            case PIPE           -> emitPipe((IRPipe) node);
            case ONLY_EXISTS    -> emitOnlyExists((IROnlyExists) node);
            case SYMBOL_NAV     -> emitSymbolNav((IRSymbolNav) node);
            case CLOSURE_PARAM  -> emitClosureParam((IRClosureParam) node);
            case META_OUTPUT_APPLY -> emitMetaOutputApply((IRMetaOutputApply) node);
            case ALL_ANY_COMPARE -> emitAllAnyCompare((IRAllAnyCompare) node);
            case SYN_ITEM_NAV   -> emitSynItemNav((IRSynItemNav) node);
            case CHOICE_OPTION_NAV -> emitChoiceOptionNav((IRChoiceOptionNav) node);
            case DISPATCH_INPUT_REF -> emitDispatchInputRef((IRDispatchInputRef) node);
            case DEEP_FEATURE_NAV -> emitDeepFeatureNav((IRDeepFeatureNav) node);
            case RECORD_FEATURE_NAV -> emitRecordFeatureNav((IRRecordFeatureNav) node);
            case META_ITEM_NAV  -> emitMetaItemNav((IRMetaItemNav) node);
            case RECORD_RECEIVER_NAV -> emitRecordReceiverNav((IRRecordReceiverNav) node);
            case QUALIFIER_ITEM_NAV -> emitQualifierItemNav((IRQualifierItemNav) node);
            case SWITCH_OP      -> emitSwitchOp((IRSwitchOp) node);
            case DEFAULT_OP     -> emitDefaultOp((IRDefaultOp) node);
            case MEMBERSHIP_OP  -> emitMembershipOp((IRMembershipOp) node);
            case COLLECT_OP     -> emitCollectOp((IRCollectOp) node);
            case OUTPUT_REF     -> emitOutputRef((IROutputRef) node);
            case META_PARAM_REF -> emitMetaParamRef((IRMetaParamRef) node);
            case RULE_INPUT_NAV -> emitRuleInputNav((IRRuleInputNav) node);
            case WITH_META_OP   -> emitWithMetaOp((IRWithMetaOp) node);
            case JOIN_OP        -> emitJoinOp((IRJoinOp) node);
            case OUTPUT_ALIAS_NAV -> emitOutputAliasNav((IROutputAliasNav) node);
            case LIBRARY_APPLY  -> emitLibraryApply((IRLibraryApply) node);
            case QUALIFIER_RECEIVER_NAV -> emitQualifierReceiverNav((IRQualifierReceiverNav) node);
            case IMPLICIT_ATTR_NAV -> emitImplicitAttrNav((IRImplicitAttrNav) node);
            case CHOICE_RECEIVER_NAV -> emitChoiceReceiverNav((IRChoiceReceiverNav) node);
            case CONDITION_INSTANCE -> emitConditionInstance((IRConditionInstance) node);
            // No default: exhaustive over IRExprKind. A new kind breaks compilation here (intended).
        };
    }

    /** @param node the literal to lower */
    protected T emitLiteral(IRLiteral node)             { return unsupported(node.kind()); }
    /** @param node the list-literal to lower */
    protected T emitListConstruct(IRListConstruct node) { return unsupported(node.kind()); }
    /** @param node the {@code empty} leaf to lower */
    protected T emitEmptyLiteral(IREmptyLiteral node)   { return unsupported(node.kind()); }
    /** @param node the variable reference to lower */
    protected T emitVariable(IRVariable node)           { return unsupported(node.kind()); }
    /** @param node the by-name reference to lower */
    protected T emitReference(IRReference node)         { return unsupported(node.kind()); }
    /** @param node the application to lower */
    protected T emitApply(IRApply node)                 { return unsupported(node.kind()); }
    /** @param node the binary operation to lower */
    protected T emitBinaryOp(BinaryOp node)             { return unsupported(node.kind()); }
    /** @param node the existence check to lower */
    protected T emitExistence(Existence node)           { return unsupported(node.kind()); }
    /** @param node the field access to lower */
    protected T emitFieldAccess(FieldAccess node)       { return unsupported(node.kind()); }
    /** @param node the meta-feature access (#499) to lower — a functional target renders the wrapper family from {@code metaQualifiers()} */
    protected T emitMetaAccess(IRMetaAccess node)       { return unsupported(node.kind()); }
    /** @param node the list operation to lower */
    protected T emitListOp(IRListOp node)               { return unsupported(node.kind()); }
    /** @param node the conditional to lower */
    protected T emitConditional(IRConditional node)     { return unsupported(node.kind()); }
    /** @param node the let-binding to lower */
    protected T emitLet(Let node)                       { return unsupported(node.kind()); }
    /** @param node the to-string conversion to lower */
    protected T emitToString(IRToString node)           { return unsupported(node.kind()); }
    /** @param node the point-free function application to lower */
    protected T emitPointFreeApply(IRPointFreeApply node) { return unsupported(node.kind()); }
    /** @param node the type construction to lower */
    protected T emitConstruct(IRConstruct node)         { return unsupported(node.kind()); }
    /** @param node the lambda-bodied collection operation (extract/filter) to lower */
    protected T emitLambdaOp(IRLambdaOp node)           { return unsupported(node.kind()); }
    /** @param node the typed conversion (#500) to lower — a functional target renders from the child + kind/target facts */
    protected T emitConversion(IRConversion node)       { return unsupported(node.kind()); }
    /** @param node the {@code then}-pipe chain root (#500, shallow) to lower */
    protected T emitPipe(IRPipe node)                   { return unsupported(node.kind()); }
    /** @param node the {@code only exists} check root (#500, shallow) to lower */
    protected T emitOnlyExists(IROnlyExists node)       { return unsupported(node.kind()); }
    /** @param node the disguised symbol-receiver navigation (#501, shallow) to lower */
    protected T emitSymbolNav(IRSymbolNav node)         { return unsupported(node.kind()); }
    /** @param node the closure-param reference (#502, shallow) to lower */
    protected T emitClosureParam(IRClosureParam node)   { return unsupported(node.kind()); }
    /** @param node the meta-output function call (#502, shallow) to lower */
    protected T emitMetaOutputApply(IRMetaOutputApply node) { return unsupported(node.kind()); }
    /** @param node the all/any-modified comparison (#503, shallow) to lower */
    protected T emitAllAnyCompare(IRAllAnyCompare node)     { return unsupported(node.kind()); }
    /** @param node the un-retypeable synthetic-item navigation (#504, shallow) to lower */
    protected T emitSynItemNav(IRSynItemNav node)           { return unsupported(node.kind()); }
    /** @param node the qualified-name choice-option selection (#505, shallow) to lower */
    protected T emitChoiceOptionNav(IRChoiceOptionNav node) { return unsupported(node.kind()); }
    /** @param node the dispatch-base input reference (#505, shallow) to lower */
    protected T emitDispatchInputRef(IRDispatchInputRef node) { return unsupported(node.kind()); }
    /** @param node the deep-path feature navigation (#507) to lower — the receiver is a real IR child */
    protected T emitDeepFeatureNav(IRDeepFeatureNav node)     { return unsupported(node.kind()); }
    /** @param node the record-feature read (#507, shallow) to lower */
    protected T emitRecordFeatureNav(IRRecordFeatureNav node) { return unsupported(node.kind()); }
    /** @param node the meta-sourced USER-item navigation to lower (#508 — no native arm by design) */
    protected T emitMetaItemNav(IRMetaItemNav node)         { return unsupported(node.kind()); }
    /** @param node the record-feature read over a lowered receiver (#512) — the receiver is a real IR child */
    protected T emitRecordReceiverNav(IRRecordReceiverNav node) { return unsupported(node.kind()); }
    /** @param node the metadata-qualifier read off a bound item (#512, shallow) to lower */
    protected T emitQualifierItemNav(IRQualifierItemNav node)   { return unsupported(node.kind()); }
    /** @param node the {@code switch} expression root (#513, shallow) to lower */
    protected T emitSwitchOp(IRSwitchOp node)                   { return unsupported(node.kind()); }
    /** @param node the {@code default} fallback (#513, shallow) to lower */
    protected T emitDefaultOp(IRDefaultOp node)                 { return unsupported(node.kind()); }
    /** @param node the list-membership test (contains/disjoint, #513, shallow) to lower */
    protected T emitMembershipOp(IRMembershipOp node)           { return unsupported(node.kind()); }
    /** @param node the element-collecting postfix op (max/min/sort, #513, shallow) to lower */
    protected T emitCollectOp(IRCollectOp node)                 { return unsupported(node.kind()); }
    /** @param node the bare function-output reference (#514, shallow) to lower */
    protected T emitOutputRef(IROutputRef node)                 { return unsupported(node.kind()); }
    /** @param node the bare meta-annotated-input reference (#514, shallow) to lower */
    protected T emitMetaParamRef(IRMetaParamRef node)           { return unsupported(node.kind()); }
    /** @param node the top-level bare rule-input navigation (#514, shallow) to lower */
    protected T emitRuleInputNav(IRRuleInputNav node)           { return unsupported(node.kind()); }
    /** @param node the synthesized-receiver bare attribute reference (#528, shallow) to lower */
    protected T emitImplicitAttrNav(IRImplicitAttrNav node)     { return unsupported(node.kind()); }
    /** @param node the data-rule condition's implicit instance (#640, shallow) to lower */
    protected T emitConditionInstance(IRConditionInstance node) { return unsupported(node.kind()); }
    /** @param node the with-metadata annotation (#515, shallow) to lower */
    protected T emitWithMetaOp(IRWithMetaOp node)               { return unsupported(node.kind()); }
    /** @param node the list-join (#515, shallow) to lower */
    protected T emitJoinOp(IRJoinOp node)                       { return unsupported(node.kind()); }
    /** @param node the output-consuming alias-head navigation (#518, shallow) to lower */
    protected T emitOutputAliasNav(IROutputAliasNav node)       { return unsupported(node.kind()); }
    /** @param node the library-function application (#520, childless shallow) to lower */
    protected T emitLibraryApply(IRLibraryApply node)           { return unsupported(node.kind()); }
    /** @param node the metadata-qualifier read over a lowered head (#525) — the receiver is a real IR child */
    protected T emitQualifierReceiverNav(IRQualifierReceiverNav node) { return unsupported(node.kind()); }
    /** @param node the choice-option selection over a lowered receiver (#529) — the receiver is a real IR child */
    protected T emitChoiceReceiverNav(IRChoiceReceiverNav node) { return unsupported(node.kind()); }

    /**
     * The single decline choke point: a backend that does not override a kind's hook declines here.
     * Overridable (e.g. a future strangler-style target that delegates instead of failing).
     *
     * @param kind the kind this backend does not lower
     * @return never returns normally (the default throws)
     * @throws EmitterException always, in the default implementation
     */
    protected T unsupported(IRExprKind kind) {
        throw new EmitterException(getClass().getSimpleName()
                + " does not lower IR expression kind " + kind);
    }
}
