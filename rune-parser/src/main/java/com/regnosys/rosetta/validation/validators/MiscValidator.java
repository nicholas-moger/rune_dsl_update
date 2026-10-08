package com.regnosys.rosetta.validation.validators;

import com.regnosys.rosetta.ast.RRootElement;
import com.regnosys.rosetta.validation.ValidationCollector;
import com.regnosys.rosetta.validation.Validator;

/**
 * Miscellaneous validations:
 * - Experimental feature scope gates
 * - Any remaining checks not covered by other validators
 *
 * <p>Upstream: ExperimentalFeatureValidator.
 */
public final class MiscValidator implements Validator {

    @Override
    public void validate(RRootElement element, ValidationCollector collector) {
        // Experimental feature checks will be added when the scope system
        // is fully implemented. Currently a placeholder.
    }
}
