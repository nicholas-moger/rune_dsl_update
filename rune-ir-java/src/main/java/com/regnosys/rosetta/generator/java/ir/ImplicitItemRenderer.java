package com.regnosys.rosetta.generator.java.ir;

import com.regnosys.rosetta.generator.java.statement.builder.JavaStatementBuilder;
import com.regnosys.rosetta.ir.expr.IRVariable;

/**
 * Renders a lambda-bound implicit {@code item} ({@link IRVariable.VariableKind#USER_ITEM}) with the LIVE
 * scope binding legacy resolves — the #469 itemCallArg teach. The pin-era emitter rendered the bare
 * {@code variable.name()} (always {@code "item"}, the adapter's hardcoded lowering name), but post-pin
 * legacy resolves the BOUND name through {@code ReferenceHandler.handle(RImplicitVariable)}'s scope
 * cascade: a NAMED extract parameter renders its declared name ({@code floatingLeg}), a collision-escaped
 * binding renders the escaped form ({@code _item}), and the plain case renders {@code item}. That binding
 * is a live-scope decision the neutral IR cannot carry (the L-029 split), so the compiler
 * ({@link IRExpressionCompiler}) supplies this renderer per top-level emission — correlating the
 * {@link IRVariable} back to its AST {@code RImplicitVariable} by the SOURCE RANGE the adapter stamps on
 * the node, then calling the legacy oracle VERBATIM on its own {@code ReferenceHandler} with the live
 * {@code ExpressionContext} (the established oracle-reuse closure: the render is byte-identical to the
 * legacy fallback by the strongest argument).
 *
 * <p>A {@code null} renderer (a standalone-emitter context, e.g. unit tests) keeps the historical bare
 * render; an installed renderer returning {@code null} (an uncorrelated variable) declines the claim to
 * legacy — never guess a binding.
 *
 * <p>Fork-authored (PR #469); not present in the lab tree.
 */
@FunctionalInterface
public interface ImplicitItemRenderer {

    /**
     * The scope-bound render of this implicit {@code item} — exactly what legacy
     * {@code ReferenceHandler.handle(RImplicitVariable)} produces for the correlated AST node, or
     * {@code null} when the node cannot be correlated (→ the claim declines to legacy).
     */
    JavaStatementBuilder render(IRVariable item);
}
