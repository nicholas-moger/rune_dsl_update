package com.regnosys.rosetta.generator.java.scoping;

import com.regnosys.rosetta.ast.RExpression;
import com.rosetta.util.types.JavaClass;

/**
 * facet ctorCondSingleCoerce (seat 28, law A) - the consumer->conditional handshake for the
 * SINGLE-cardinality conditional hoist, the twin of {@link CondListCoerce}. A ctor key-value
 * seat about to compile a value that is an
 * {@link com.regnosys.rosetta.ast.expressions.constructors.RConditionalExpr} consumed at a
 * SINGLE, meta-NONE attribute pushes one of these on the compile scope
 * ({@link JavaStatementScope#pushCondSingleCoerce}); {@code ControlFlowHandler}'s
 * {@code hoistAsItemLocalOrNull} arm consumes it (node-identity-keyed) and - ONLY when its own
 * already-computed walk type DIFFERS from {@link #targetType} by a witnessed numeric conversion
 * - declares the local at {@code targetType} and converts EACH ARM in place:
 *
 * <pre>
 * &lt;Target&gt; ifThenElseResultN = null;
 * if (&lt;cond&gt;.getOrDefault(false)) {
 *     final &lt;Walk&gt; &lt;name&gt; = &lt;then value&gt;;          // only when the arm is not a bare identifier
 *     ifThenElseResultN = &lt;name&gt; == null ? null : &lt;name&gt;.intValueExact();
 * }
 * </pre>
 *
 * after which the consumer splices the local BARE ({@code fired} set by the arm; the consumer's
 * own setter-side narrow - {@code ConstructionHandler.tryCtorNumericNarrow} / {@code
 * hoistNumericCoerceCtorChainOrNull} - must not re-coerce, or the post-fix bytes would call
 * {@code intValueExact()} on the already-narrowed local). This class carries NO type walk of its
 * own: the producer supplies the TARGET only, and the single walk that decides fire-vs-decline is
 * the consumer's own {@code declWork} (LAW 69 - one walk, two halves, no possible disagreement).
 * See {@code ConstructionHandler.condSingleCoerceFor} and
 * {@code ControlFlowHandler.condSingleCoerceOrNull}.
 */
public final class CondSingleCoerce {

    /** The exact conditional node this coercion belongs to (identity-compared). */
    public final RExpression node;
    /** The consumer's expected ITEM type - the attribute's Java type. */
    public final JavaClass<?> targetType;
    /** Set by the hoist arm when it fired - the consumer then splices the local bare. */
    public boolean fired;

    public CondSingleCoerce(RExpression node, JavaClass<?> targetType) {
        this.node = node;
        this.targetType = targetType;
    }
}
