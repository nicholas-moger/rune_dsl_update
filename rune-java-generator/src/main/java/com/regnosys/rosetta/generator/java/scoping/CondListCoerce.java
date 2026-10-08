package com.regnosys.rosetta.generator.java.scoping;

import com.regnosys.rosetta.ast.RExpression;
import com.rosetta.util.types.JavaClass;

/**
 * facet condListCoerce (PR #327, arm A1) — the consumer→conditional handshake for the
 * LIST-typed conditional hoist. A consumer seat about to compile a value that is a
 * no-real-else SINGLE-cardinality {@link com.regnosys.rosetta.ast.expressions.constructors.RConditionalExpr}
 * consumed at a MULTI (List) position (a multi ctor-setter pair, a multi segment-ADD
 * leaf) pushes one of these on the compile scope
 * ({@link JavaStatementScope#pushCondListCoerce}); {@code ControlFlowHandler}'s
 * {@code hoistAsListLocalOrNull} arm consumes it (node-identity-keyed) and emits the
 * upstream per-branch coercion form instead of the ITEM-typed hoist:
 *
 * <pre>
 * final List&lt;Elem&gt; ifThenElseResultN;
 * if (&lt;cond&gt;.getOrDefault(false)) {
 *     final &lt;Local&gt; &lt;name&gt; = &lt;then value&gt;;
 *     ifThenElseResultN = &lt;name&gt; == null ? Collections.&lt;Elem&gt;emptyList()
 *             : Collections.singletonList(&lt;name or meta-wrap&gt;);
 * } else {
 *     ifThenElseResultN = Collections.&lt;Elem&gt;emptyList();
 * }
 * </pre>
 *
 * after which the consumer splices the local BARE ({@code fired} set by the arm; the
 * consumer's own single→list coercion — #198 {@code hoistSingleValueIntoMultiOrNull} /
 * the segment-ADD leaf splice — must not re-coerce). The type split mirrors
 * {@code hoistSingleValueIntoMultiOrNull}'s meta law: a META-annotated attribute whose
 * value already carries attribute-meta lists the WRAPPER with a wrapper-typed local; a
 * meta attribute with a meta-FREE value splits on the SEAT (facet ctorCondValueSetterList,
 * PR #383 — the ctor key-value seat lists the BARE item end-to-end, consumed by the
 * {@code set<Name>Value} setter; the segment-ADD leaf keeps the #190 wrap law:
 * the wrapper list with a BARE local and {@code Wrapper.builder().setValue(local).build()}
 * INSIDE the singletonList, consumed by the PLAIN adder); a non-meta attribute lists the
 * bare item. See {@code ConstructionHandler.condListCoerceFor}.
 */
public final class CondListCoerce {

    /** The exact conditional node this coercion belongs to (identity-compared). */
    public final RExpression node;
    /** The list ELEMENT type — the (possibly meta-wrapped) attribute item type. */
    public final JavaClass<?> listElemType;
    /** The hoisted value local's type — bare when {@link #wrapValueInMeta}. */
    public final JavaClass<?> localType;
    /** Compose {@code Wrapper.builder().setValue(local).build()} inside the singletonList. */
    public final boolean wrapValueInMeta;
    /** Set by the hoist arm when it fired — the consumer then splices the local bare. */
    public boolean fired;

    public CondListCoerce(RExpression node, JavaClass<?> listElemType, JavaClass<?> localType,
            boolean wrapValueInMeta) {
        this.node = node;
        this.listElemType = listElemType;
        this.localType = localType;
        this.wrapValueInMeta = wrapValueInMeta;
    }
}
