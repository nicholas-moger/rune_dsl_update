package org.finos.rune.equivalence;

import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.functions.ConditionValidator;

import java.util.function.Supplier;

/**
 * The pair-execution COVERAGE arm's condition validator: EVALUATES every condition
 * (so condition expressions execute on both sides exactly as under the default
 * binding) but never throws — pre/post-condition failures no longer abort the
 * function body, so OPERATION bodies execute even where deterministic synthesized
 * inputs violate business preconditions.
 *
 * <p>NOT an oracle (the lenient-loading-is-never-the-oracle law): the STRICT
 * default-binding arm stays primary and compares thrown envelopes byte-for-byte;
 * this arm exists to push pair execution THROUGH the bodies, and its own outcomes
 * are compared just as strictly between the two sides.
 */
public final class LenientConditionValidator implements ConditionValidator {

    @Override
    public void validate(Supplier<ComparisonResult> condition, String description) {
        try {
            condition.get();
        } catch (RuntimeException e) {
            // Evaluated for coverage; the outcome (incl. a throwing condition
            // expression) is deliberately ignored — this arm never aborts.
        }
    }
}
