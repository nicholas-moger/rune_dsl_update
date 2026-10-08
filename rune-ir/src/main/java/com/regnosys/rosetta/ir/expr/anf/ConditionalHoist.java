package com.regnosys.rosetta.ir.expr.anf;

import com.regnosys.rosetta.ir.expr.NodeId;
import com.regnosys.rosetta.types.RMetaAnnotatedType;

/**
 * The neutral <em>facts</em> of a statement-position conditional hoist — exactly what
 * {@code normalize} needs to produce the {@code ifThenElseResult} {@link Bind}, with no dependence on the
 * branch interiors being lowered. It is a fact bundle, deliberately <strong>not</strong> an
 * {@link com.regnosys.rosetta.ir.expr.IRExpr} and not a member of any emitter-dispatch enum: it carries
 * no program semantics beyond identity, result type and {@code else}-presence.
 *
 * <p><strong>Why a facts bundle and not the full {@link com.regnosys.rosetta.ir.expr.IRConditional}.</strong>
 * At the vendored #219 pin the dominant Phase-A carriers have alias-receiver branch interiors whose
 * shortcut type is {@code MISSING} (L-032), so they cannot be lowered to a fully-childed
 * {@code IRConditional}. The offline harness validates the hoist <em>skeleton</em> (temp name/number/order
 * + declared type + the declare-then-assign shape), which needs only these three facts. The live "C" path
 * builds a real {@code IRConditional} (interiors lowered) and {@code normalize} reads the same three facts
 * off it via {@link Normalize#factsOf}, so the {@link Normalize#conditionalToBind} lowering is shared.
 *
 * <p>Lab-authored Phase-2; not present upstream.
 *
 * @param key        the conditional node's structural {@link NodeId} — the {@link TempName} key of the
 *                   hoisted {@code ifThenElseResult} temporary (its numeric suffix comes from the
 *                   per-scope render-walk replay, not from this key)
 * @param resultType the hoisted local's declared type — the then-branch's inferred type (for an
 *                   {@code else}-less conditional, equal to the conditional's own inferred type). The
 *                   ANF→Java replay names the local via {@code toJavaReferenceType(resultType.type())}.
 * @param hasElse    whether the conditional has an explicit {@code else} branch. An {@code else}-less
 *                   conditional lowers to the {@code <Type> t = null; if(<cond>){t = <then>;}} form (the
 *                   only form Phase-A slice 1 handles); the {@code else}-bearing form is a later slice.
 * @param conditionKey   the {@link NodeId} of the condition sub-expression — the {@link TempName} key of the
 *                       hoisted {@code boolean} temporary when {@code conditionHoists}. Distinct from
 *                       {@code key} (the conditional's own NodeId).
 * @param conditionHoists whether the condition is a <em>bare function call</em> that hoists a
 *                       {@code final Boolean} temporary BEFORE the {@code ifThenElseResult} it guards (design
 *                       §5; dump §6). A comparison/equality/existence condition renders inline as a
 *                       {@code ComparisonResult} and is NOT hoisted ({@code false}). The hoist is detected
 *                       structurally (a function-call AST node), independent of whether the condition lowers to
 *                       IR — so a non-lowering arg (e.g. a meta-nav, a post-#489 argNav decline face, or an
 *                       empty arg, the separate {@code argEmpty} gate) does not suppress it.
 * @param conditionType  the condition's inferred type (a {@code boolean}), supplying the offline-derivable
 *                       {@code Boolean} decl of the hoisted temporary; meaningful only when {@code conditionHoists}.
 */
public record ConditionalHoist(NodeId key, RMetaAnnotatedType resultType, boolean hasElse,
                               NodeId conditionKey, boolean conditionHoists, RMetaAnnotatedType conditionType) {
}
