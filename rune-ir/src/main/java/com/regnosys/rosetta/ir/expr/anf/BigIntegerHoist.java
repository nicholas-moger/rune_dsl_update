package com.regnosys.rosetta.ir.expr.anf;

import com.regnosys.rosetta.ir.expr.NodeId;

/**
 * The fact bundle for the {@code bigInteger} literal-hoist family (facet {@code biginteger_literal_hoist}) — a
 * beyond-{@code long} integer literal ({@code value.bitLength() > 63}) consumed in a {@code BigDecimal} context,
 * which the Java target hoists as {@code final BigInteger bigInteger = new BigInteger("…");} at the nearest
 * statement-hoist sink (the 4th + last Wave-6 hoist family).
 *
 * <p><strong>This is the THINNEST fact bundle — identity-only.</strong> Unlike {@link ConditionalHoist}
 * ({@code hasElse}/{@code conditionHoists}), {@link BooleanConditionHoist} ({@code conditionHoists}) and
 * {@link ThenChainHoist} (the binder count) — whose lowerings BRANCH on a behavioural field — the
 * {@code bigInteger} hoist's base lexeme is <em>value-independent</em> (always {@code "bigInteger"}). So this
 * record carries only the hoist's {@link NodeId} key (its render-walk numbering slot), and
 * {@link Normalize#bigIntegerToBind} branches on nothing. It exists so the ANF substrate genuinely <em>models</em>
 * the bigInteger family — the name flows from the ANF rather than being a re-stated constant; the family-completion
 * and the L-029 name-driving split need an ANF node for that to be literally true — NOT because it carries a
 * per-node decision. It is honestly thinner than the boolean/conditional/then precedents (decision-log L-068).
 *
 * <p>Like the rest of {@code ir.expr.anf} this is the <strong>Java-target</strong> lowering: the bigInteger hoist
 * is a Java numeric-tower artifact (a literal beyond Java {@code long}); a Python/Rust target with unbounded
 * integers has NO such family and would never call {@link Normalize#bigIntegerToBind}.
 *
 * <p>Lab-authored Phase-2; not present upstream.
 */
public record BigIntegerHoist(NodeId key) {
}
