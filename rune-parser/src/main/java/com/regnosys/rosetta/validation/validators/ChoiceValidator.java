package com.regnosys.rosetta.validation.validators;

import com.regnosys.rosetta.ast.RRootElement;
import com.regnosys.rosetta.ast.supporting.RChoiceOption;
import com.regnosys.rosetta.ast.types.RChoice;
import com.regnosys.rosetta.validation.ValidationCollector;
import com.regnosys.rosetta.validation.ValidationIssueCode;
import com.regnosys.rosetta.validation.Validator;

import java.util.HashSet;
import java.util.Set;

/**
 * Validates choice type declarations:
 * - No duplicate option types (same type name referenced twice)
 *
 * <p>Upstream: ChoiceValidator.checkCyclicOptions,
 * ChoiceValidator.checkChoiceOptionsDoNotOverlap.
 */
public final class ChoiceValidator implements Validator {

    @Override
    public void validate(RRootElement element, ValidationCollector collector) {
        if (!(element instanceof RChoice choice)) return;

        checkDuplicateOptions(choice, collector);
    }

    private void checkDuplicateOptions(RChoice choice, ValidationCollector collector) {
        Set<String> seen = new HashSet<>();
        for (RChoiceOption opt : choice.options()) {
            if (opt.typeCall() == null) continue;
            String typeName = opt.typeCall().typeName();
            if (typeName != null && !seen.add(typeName)) {
                collector.error(
                    opt.sourceRange(),
                    "Duplicate choice option '" + typeName + "' in choice '" + choice.name() + "'",
                    ValidationIssueCode.DUPLICATE_CHOICE_RULE_ATTRIBUTE);
            }
        }
    }
}
