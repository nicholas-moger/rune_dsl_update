package com.regnosys.rosetta.ir.expr.anf;

import com.regnosys.rosetta.ir.expr.NodeId;
import com.regnosys.rosetta.types.RMetaAnnotatedType;

/**
 * The neutral <em>facts</em> of a renderer-direct {@code boolean} condition hoist — a bare-function-call
 * condition hoisted to a {@code final Boolean} temporary BEFORE an <strong>inline</strong> {@code if/else}
 * (the whole-output SET-conditional and alias return-ladder seats, where the conditional itself is NOT hoisted
 * to an {@code ifThenElseResult}). It is a fact bundle, deliberately <strong>not</strong> an
 * {@link com.regnosys.rosetta.ir.expr.IRExpr} and not a member of any emitter-dispatch enum.
 *
 * <p><strong>Why a record distinct from {@link ConditionalHoist}.</strong> {@code ConditionalHoist} is an
 * {@code ifThenElseResult} fact — its {@code key} and {@code resultType} describe the hoisted conditional-result
 * local. At the renderer-direct boolean seats there is NO {@code ifThenElseResult} (the conditional renders as an
 * inline {@code if (<boolGuard>) { output = then; } else { output = else; }}), so reusing {@code ConditionalHoist}
 * would carry an {@code ifThenElseResult} identity the seat does not have (and waste the then-branch type
 * inference). This record carries ONLY the condition facts the {@code boolean} hoist needs — the structural
 * decision to hoist, the temporary's {@link NodeId} key, and its declared {@code Boolean} type. (Wave-6 Phase-C
 * slice-3; the §6 cross-group {@code boolean}-before-{@code ifThenElseResult} reorder — where the two co-occur —
 * is a separate, deferred expression-compiler concern.)
 *
 * <p>Lab-authored Phase-2; not present upstream.
 *
 * @param conditionKey    the {@link NodeId} of the condition sub-expression — the {@link TempName} key of the
 *                        hoisted {@code boolean} temporary (its numeric suffix / keyword escape comes from the
 *                        per-scope render-walk replay, not from this key)
 * @param conditionType   the condition's inferred type (a {@code boolean}), supplying the {@code Boolean} decl of
 *                        the hoisted temporary; meaningful only when {@code conditionHoists}
 * @param conditionHoists whether the condition is a <em>bare function call</em> that hoists a {@code final Boolean}
 *                        temporary (design §5; dump §6). A comparison/equality/existence condition renders inline
 *                        as a {@code ComparisonResult} and is NOT hoisted ({@code false}). Detected structurally (a
 *                        function-call AST node), independent of whether the condition lowers to IR.
 */
public record BooleanConditionHoist(NodeId conditionKey, RMetaAnnotatedType conditionType,
                                    boolean conditionHoists) {
}
