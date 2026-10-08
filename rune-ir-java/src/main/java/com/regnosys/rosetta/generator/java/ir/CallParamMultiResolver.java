package com.regnosys.rosetta.generator.java.ir;

import com.regnosys.rosetta.ir.expr.IRApply;
import com.regnosys.rosetta.ir.expr.IRReference;

/**
 * Resolves, for a function CALL ({@link IRApply}) being lowered, whether the callee parameter at a given
 * argument position is declared MULTI (a {@code 0..*} {@code List}-typed input) — the one fact the
 * {@link IRJavaLeafEmitter} needs to choose the evaluate-arg unwrap accessor: {@code .getMulti()} (→ {@code List<T>})
 * for a {@code Mapper}-chain argument into a multi parameter, versus the scalar {@code .get()} (→ {@code T}).
 *
 * <p>This mirrors legacy {@code ReferenceHandler}'s facet {@code tailMulti} (PR #191): the accessor follows the
 * CALLEE PARAMETER's declared cardinality, not the argument's, because {@code MapperS.getMulti()} coerces a single
 * value into a 0-or-1 element list. An alias argument ({@code aliasName(inputs)}, a chained {@code Mapper}) into a
 * multi parameter therefore unwraps as {@code aliasName(inputs).getMulti()}; a navigation argument (the #489 L-042
 * revival — the other chained-{@code Mapper} class) follows the same law; a scalar param / literal / enum-value
 * argument strips STRUCTURALLY before the accessor and is unaffected by the flag.
 *
 * <p>The parameter cardinality is derivable from the callee's signature, so — like the {@link CallReceiverResolver}
 * receiver name — it is a Java-emission decision computed compiler-side and kept OUT of the language-neutral
 * {@link IRApply} (the L-029 split). Since #489 the resolver is PER-CALL: the {@link IRExpressionCompiler} indexes
 * every args-present resolved call site in the claimed subtree by its {@link com.regnosys.rosetta.ast.SourceRange}
 * (the same correlation key the adapter stamps on the callee {@link IRReference} — the {@code CallReceiverResolver}
 * mechanism) and reuses legacy {@code ReferenceHandler.evaluateParamIsMulti} as the oracle. The pre-#489 form
 * captured the callee from the claim's TOP-LEVEL expression only — an invariant the nav-arg admission broke twice
 * over (a claimed call nested under an operator read a NULL resolver and defaulted every accessor to {@code .get()};
 * a NESTED call's args resolved against the OUTER callee's positions — both byte-visible on the #489 ring, the drr
 * decode). A {@code null} resolver (or an uncorrelated callee) makes every argument unwrap with the scalar accessor
 * — a RENDER default (the historical single-parameter behaviour), structurally unreachable for a claimed call — so
 * existing single-parameter calls are unaffected.
 *
 * <p>Lab-authored Phase-2; not present upstream.
 */
@FunctionalInterface
public interface CallParamMultiResolver {

    /**
     * Whether {@code callee}'s parameter positionally matching argument {@code argIndex} is declared MULTI —
     * byte-identical to legacy {@code ReferenceHandler.evaluateParamIsMulti} ({@code GeneratorModel.isMulti} of the
     * callee's {@code inputs().get(argIndex)}), returning {@code false} for an out-of-range index, an uncorrelated
     * callee (its {@code sourceRange()} matches no indexed call site) or an unresolved generator model — the
     * scalar RENDER default ({@code .get()}), not a claim decline: for a claimed call an uncorrelated callee is
     * structurally unreachable (the adapter stamps every claimed callee with its call node's own range, and
     * {@code IRExpressionCompiler#indexFunctionCallSites} walks the whole claimed subtree with a superset of the
     * claim gate); the byte ring is the empirical backstop.
     *
     * @param callee the {@link IRApply}'s callee reference — its {@code sourceRange()} is the call-site
     *               correlation key (the adapter stamps the CALL node's range on it)
     */
    boolean paramAcceptsMulti(IRReference callee, int argIndex);
}
