package com.regnosys.rosetta.harness.jqwik;

import org.antlr.v4.runtime.Vocabulary;
import org.antlr.v4.runtime.atn.ATN;
import org.antlr.v4.runtime.atn.ATNState;
import org.antlr.v4.runtime.atn.AtomTransition;
import org.antlr.v4.runtime.atn.RuleStopState;
import org.antlr.v4.runtime.atn.RuleTransition;
import org.antlr.v4.runtime.atn.SetTransition;
import org.antlr.v4.runtime.atn.Transition;

import java.util.Random;

/**
 * Walks an ANTLR4 parser ATN to generate syntactically plausible source
 * text for a given rule.
 *
 * <p>P1.2 audit hooks H18 (bootstrap) + H18.2 (weights + expression
 * coverage). Given a parser's {@link ATN}, {@link Vocabulary}, and a
 * target rule index, this generator traces transitions from
 * {@link ATN#ruleToStartState ruleToStartState} to a
 * {@link RuleStopState}, emitting lexemes for each consumed token.
 * Choice points (decision states with multiple outgoing transitions)
 * are resolved by weighted-random selection: non-{@link RuleTransition}
 * alternatives carry {@link RuleWeights#DEFAULT_WEIGHT} and
 * {@link RuleTransition} alternatives carry the weight of their target
 * rule from the supplied {@link RuleWeights}. With the default
 * {@link RuleWeights#uniform() uniform} weights every outgoing
 * transition carries the same weight — i.e. the pick is distributionally
 * equivalent to the H18-bootstrap uniform selection. Output is not
 * bit-identical to the bootstrap under the same seed because the
 * weighted path consumes the PRNG via {@code nextDouble()} rather than
 * {@code nextInt()}.
 *
 * <h2>Scope</h2>
 * <ul>
 *   <li><b>Handles:</b> {@link AtomTransition}, {@link SetTransition},
 *       {@link RuleTransition} (recurses), epsilon-class transitions
 *       (pass-through — includes {@code PrecedencePredicateTransition},
 *       {@code PredicateTransition}, {@code ActionTransition} in
 *       parser ATNs). Arbitrary depth / token budget.
 *   <li><b>Expression rule:</b> walkable. ANTLR4 rewrites left-recursive
 *       rules into a non-LR loop form gated by precedence predicates;
 *       this walker treats those predicates as pass-through, so the
 *       rewritten loop is explored the same way any other star-loop
 *       would be. Budget caps prevent runaway; {@link RuleWeights}
 *       biases which child rules the loop drops into.
 *   <li><b>No regex on structured content</b> — the generator consumes
 *       ATN structure, never string-scans the grammar.
 * </ul>
 *
 * <p>Budget: {@link #MAX_DEPTH} caps rule-recursion depth;
 * {@link #MAX_TOKENS} caps total emitted tokens. When either is
 * reached the walker stops emitting and returns whatever was built —
 * callers can detect short strings and widen the budget if needed.
 */
final class AtnStringGenerator {

    static final int MAX_DEPTH = 32;
    static final int MAX_TOKENS = 128;

    private final ATN atn;
    private final TokenSynthesiser synthesiser;
    private final Random random;
    private final RuleWeights weights;

    AtnStringGenerator(ATN atn, Vocabulary vocabulary, Random random) {
        this(atn, vocabulary, random, RuleWeights.uniform());
    }

    AtnStringGenerator(ATN atn, Vocabulary vocabulary, Random random, RuleWeights weights) {
        this.atn = atn;
        this.synthesiser = new TokenSynthesiser(vocabulary);
        this.random = random;
        this.weights = weights;
    }

    /** Generate a source fragment for {@code ruleIndex}. */
    String generate(int ruleIndex) {
        if (ruleIndex < 0 || ruleIndex >= atn.ruleToStartState.length) {
            throw new IllegalArgumentException("rule index out of range: " + ruleIndex);
        }
        StringBuilder out = new StringBuilder();
        TokenBudget budget = new TokenBudget();
        walk(atn.ruleToStartState[ruleIndex], 0, out, budget);
        return out.toString().trim();
    }

    // ---- walk ----

    /**
     * Walk from {@code state} until a {@link RuleStopState} for the
     * current rule is reached, or the budget is exhausted.
     */
    private void walk(ATNState state, int depth, StringBuilder out, TokenBudget budget) {
        ATNState current = state;
        int ruleBound = current.ruleIndex;
        int guard = 0;
        // Hard guard against pathological loops — the budget already
        // caps emissions, but epsilon cycles can still spin.
        int maxSteps = MAX_TOKENS * 8;
        while (guard++ < maxSteps && !budget.exhausted()) {
            if (current instanceof RuleStopState stop && stop.ruleIndex == ruleBound) {
                return;
            }
            int n = current.getNumberOfTransitions();
            if (n == 0) return;
            Transition t = pickTransition(current, n);
            current = step(t, depth, out, budget);
            if (current == null) return;
        }
    }

    /**
     * Pick one of {@code state}'s outgoing transitions, weighted by the
     * target-rule weight for {@link RuleTransition} alternatives. Other
     * transition types carry {@link RuleWeights#DEFAULT_WEIGHT}. With
     * uniform weights this reduces to the bootstrap's uniform pick.
     *
     * <p>The walker evaluates weights at every decision state — it does
     * not precompute a distribution, because the same state can be
     * re-entered at different depths where the budget / depth state
     * varies but the available transitions do not. Per-call cost is
     * O(n) for the small n of a decision state (single digits for this
     * grammar), which is negligible beside the test's parse cost.
     */
    private Transition pickTransition(ATNState state, int n) {
        if (n == 1) return state.transition(0);

        double total = 0.0;
        double[] cumulative = new double[n];
        for (int i = 0; i < n; i++) {
            Transition t = state.transition(i);
            double w = (t instanceof RuleTransition rt)
                    ? weights.weightFor(rt.ruleIndex)
                    : RuleWeights.DEFAULT_WEIGHT;
            total += w;
            cumulative[i] = total;
        }

        // Defensive: if every weight is zero (shouldn't happen given
        // RuleWeights floors at MIN_WEIGHT, but floats can round) fall
        // back to uniform so the walker never stalls.
        if (total <= 0.0) return state.transition(random.nextInt(n));

        double pick = random.nextDouble() * total;
        for (int i = 0; i < n; i++) {
            if (pick < cumulative[i]) return state.transition(i);
        }
        return state.transition(n - 1);
    }

    /**
     * Follow one transition. Returns the next state, or {@code null} if
     * the walker should stop (budget or depth exhausted).
     */
    private ATNState step(Transition t, int depth, StringBuilder out, TokenBudget budget) {
        if (t instanceof AtomTransition atom) {
            emit(atom.label, out, budget);
            return budget.exhausted() ? null : t.target;
        }
        if (t instanceof SetTransition set) {
            int[] members = set.set.toArray();
            if (members.length == 0) return t.target;
            emit(members[random.nextInt(members.length)], out, budget);
            return budget.exhausted() ? null : t.target;
        }
        if (t instanceof RuleTransition rule) {
            if (depth >= MAX_DEPTH) return rule.followState;
            walk(atn.ruleToStartState[rule.ruleIndex], depth + 1, out, budget);
            return budget.exhausted() ? null : rule.followState;
        }
        // Epsilon, action, predicate, precedence — pass through.
        return t.target;
    }

    /**
     * Emit one token. Separates tokens with a single space — the lexer
     * is whitespace-tolerant between every token we synthesise here.
     */
    private void emit(int tokenType, StringBuilder out, TokenBudget budget) {
        String lexeme = synthesiser.synthesise(tokenType, random);
        if (lexeme == null || lexeme.isEmpty()) return;
        if (out.length() > 0) out.append(' ');
        out.append(lexeme);
        budget.spend();
    }

    private static final class TokenBudget {
        int remaining = MAX_TOKENS;
        void spend() { remaining--; }
        boolean exhausted() { return remaining <= 0; }
    }
}
