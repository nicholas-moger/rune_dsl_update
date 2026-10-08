package com.regnosys.rosetta.generator.java.ir;

import com.regnosys.rosetta.generator.java.statement.builder.JavaStatementBuilder;
import com.regnosys.rosetta.ir.expr.FieldAccess;

/**
 * Renders an item-rooted navigation the emitter's own hop-by-hop {@code FieldAccess} render cannot
 * reproduce byte-faithfully — the #469 multiHopItemNav + metaItemReceiver teach. Two families route here:
 *
 * <ul>
 *   <li><strong>the chained item-rooted nav</strong> (the #467 A/B face R-E): legacy names each chained
 *       hop's lambda var through {@code JavaStatementScope.registerDeferredLambdaParam} — a DEFERRED
 *       sentinel whose escape decision resolves at method finalization, when the whole registration chain
 *       is known ({@code _trade -> _trade.getTradeLot()}, Create_ExposureFromTrades). A node-local
 *       immediate {@code disambiguate} cannot reproduce that (the L-029 split's "irreproducible without
 *       the scope machinery" class);</li>
 *   <li><strong>the meta-typed item receiver</strong>: legacy inserts the {@code .map("Type coercion",
 *       …getValue())} deref step ahead of the feature hop when the extracted elements are meta wrappers
 *       (ExtractTradePurchasePrice).</li>
 * </ul>
 *
 * The compiler ({@link IRExpressionCompiler}) serves ONLY a guarded nav that IS the whole claim (the
 * ROOT position, matched by the SOURCE RANGE the adapter stamps on the {@link FieldAccess}), by calling
 * {@code super.visitFeatureCall} — the EXACT legacy fallback render, sentinels, registrations and
 * coercions all legacy's own — so the bytes are identical to the decline path by the strongest argument
 * (it is literally the decline's own call). A NESTED guarded nav is never served: its whole claim
 * declines at the guard instead, because a delegated chain inside IR-rendered parents is a composition
 * legacy never produced (the #469 cpON4 drift receipt — scope-group-numbered coercion params — is the
 * witness). A {@code null} renderer, or a {@code null} return (a non-root node — the green hop-by-hop
 * population), keeps the emitter's own {@code FieldAccess} render.
 *
 * <p>Fork-authored (PR #469); not present in the lab tree.
 */
@FunctionalInterface
public interface ItemNavRenderer {

    /**
     * The complete legacy render of this navigation when it belongs to the indexed guarded family, or
     * {@code null} when it does not (→ the emitter's own hop-by-hop render proceeds).
     */
    JavaStatementBuilder render(FieldAccess nav);
}
