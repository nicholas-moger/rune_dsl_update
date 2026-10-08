package com.regnosys.rosetta.validation.validators;

import com.regnosys.rosetta.ast.RRootElement;
import com.regnosys.rosetta.ast.synonyms.RSynonymSource;
import com.regnosys.rosetta.validation.ValidationCollector;
import com.regnosys.rosetta.validation.Validator;

/**
 * Validates synonym source declarations.
 * Placeholder for synonym-specific checks (path validation, format).
 *
 * <p>Upstream: RosettaSimpleValidator synonym checks.
 */
public final class SynonymValidator implements Validator {

    @Override
    public void validate(RRootElement element, ValidationCollector collector) {
        if (!(element instanceof RSynonymSource)) return;
        // Synonym validation checks will be added as the upstream checks are ported.
        // Currently a placeholder to complete the 15-validator registration.
    }
}
