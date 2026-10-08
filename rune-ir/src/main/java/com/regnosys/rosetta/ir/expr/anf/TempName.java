package com.regnosys.rosetta.ir.expr.anf;

import com.regnosys.rosetta.ir.expr.NodeId;

/**
 * The neutral identity of a hoisted temporary — a base lexeme plus the {@link NodeId} structural key
 * of the node that produced it. It deliberately does <strong>not</strong> carry the final numeric
 * suffix: the maintainer's generator numbers temps by <em>per-scope render-walk registration order</em>
 * (reset per method body and per lambda body), which deliberately reorders versus source pre-order
 * (design §5 / §2.6 Q3, confirmed by the Q3 hoist-trace dump). The numeric suffix is therefore assigned
 * by the ANF→Java render-walk replay, NOT stored here — keeping {@code TempName} neutral and the number
 * a render-time decision (other targets reset per-scope the same way with their own base lexemes).
 *
 * <h2>Base lexemes</h2>
 * The hoisted-statement families use fixed base names — {@code ifThenElseResult}, {@code thenArg},
 * {@code boolean}, {@code bigInteger} — plus the fixed literals {@code switchArgument} and {@code item};
 * a type-derived local (switch case var, meta-wrapper, coercion param) uses the lowerCamelCase of its
 * type. At render the group is numbered {@code base, base0, base1, …} by registration order (a single
 * occurrence stays bare), with a leading {@code _} escape when the base is a Java keyword (e.g.
 * {@code _boolean}, never {@code boolean0}) or collides with an in-scope name (design §5; dump §1).
 *
 * <p>Lab-authored Phase-2; not present upstream.
 *
 * @param base the base lexeme (e.g. {@code "ifThenElseResult"}, {@code "thenArg"}, or a lowerCamelCase
 *             type name) — never the numbered/escaped form, which the render-walk replay produces
 * @param key  the {@link NodeId} of the node this temporary names — the stable identity used to map a
 *             temp back to its node (NOT the source of the numeric suffix)
 */
public record TempName(String base, NodeId key) {
}
