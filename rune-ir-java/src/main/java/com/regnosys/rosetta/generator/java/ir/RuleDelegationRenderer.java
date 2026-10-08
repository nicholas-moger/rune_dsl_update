package com.regnosys.rosetta.generator.java.ir;

import com.regnosys.rosetta.generator.java.statement.builder.JavaStatementBuilder;

import java.util.Optional;

/**
 * Produces the complete Java render of an <em>in-lambda</em> bare-RULE delegation
 * ({@link com.regnosys.rosetta.ir.expr.IRApply} with an
 * {@link com.regnosys.rosetta.ir.expr.IRReference.ReferenceKind#RULE} callee reached inside an
 * extract/filter/then lambda body) — {@code MapperS.of(<Name>Rule.evaluate(<binding>))}, where the
 * implicit-input {@code <binding>} is the lambda's own {@code item.get()} / {@code thenArg.get()}.
 *
 * <p>The binding is <em>polymorphic</em> and AST-dependent (it follows the
 * thenArg-scope&rarr;enclosing-lambda-binding precedence the legacy
 * {@code ReferenceHandler.handle(RImplicitVariable)} encodes), so it cannot be derived from the
 * neutral {@link com.regnosys.rosetta.ir.expr.IRApply} alone. Rather than re-derive that precedence
 * lab-side (a parity risk), {@link IRExpressionCompiler} captures the AST node + the live
 * {@code ExpressionContext} and renders through legacy
 * {@code ReferenceHandler.renderImplicitRuleInvocation} VERBATIM — so the in-lambda delegation is
 * byte-identical to Path-1 by construction. The emitter therefore stays free of any direct dependence
 * on the AST / {@code ExpressionCompiler}, depending only on this focused interface (the L-029 split,
 * the same boundary {@link CallReceiverResolver} and {@link AliasOperandResolver} use).
 *
 * <p>Installed by the compiler only for the in-lambda case; the rule-body <em>top-level</em>
 * delegation ({@code output = SomeRule}, binding {@code input}) is rendered by the L-045
 * {@link CallReceiverResolver} path instead (no oracle re-entry, so its &sect;4.2 firing count is
 * undisturbed). A {@code null} renderer makes the emitter fall to that top-level path.
 *
 * <p>Lab-authored Phase-2; not present upstream.
 */
@FunctionalInterface
public interface RuleDelegationRenderer {

    /**
     * The full {@code MapperS.of(<Name>Rule.evaluate(<binding>))} render for the in-lambda delegation,
     * or {@link Optional#empty()} when it cannot be produced (&rarr; the delegation declines to legacy).
     */
    Optional<JavaStatementBuilder> render();
}
