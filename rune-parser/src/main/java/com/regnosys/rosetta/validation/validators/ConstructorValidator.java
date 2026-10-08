package com.regnosys.rosetta.validation.validators;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.RRootElement;
import com.regnosys.rosetta.ast.expressions.constructors.RConstructorExpr;
import com.regnosys.rosetta.ast.expressions.supporting.RKeyValuePair;
import com.regnosys.rosetta.ast.util.AstWalker;
import com.regnosys.rosetta.validation.ValidationCollector;
import com.regnosys.rosetta.validation.ValidationIssueCode;
import com.regnosys.rosetta.validation.Validator;

import java.util.HashSet;
import java.util.Set;

/**
 * Validates constructor expressions:
 * - No duplicate keys in constructor key-value pairs
 *
 * <p>Upstream: ConstructorValidator.checkMandatoryConstructorArguments,
 * SwitchValidator.checkSwitchExhaustiveness.
 */
public final class ConstructorValidator implements Validator {

    @Override
    public void validate(RRootElement element, ValidationCollector collector) {
        for (var ctor : AstWalker.findAll(element, RConstructorExpr.class)) {
            checkDuplicateKeys(ctor, collector);
        }
    }

    private void checkDuplicateKeys(RConstructorExpr ctor, ValidationCollector collector) {
        Set<String> seen = new HashSet<>();
        for (RKeyValuePair pair : ctor.pairs()) {
            if (pair.key() != null && !seen.add(pair.key())) {
                collector.error(
                    pair.sourceRange(),
                    "Duplicate key '" + pair.key() + "' in constructor",
                    ValidationIssueCode.DUPLICATE_ATTRIBUTE);
            }
        }
    }
}
