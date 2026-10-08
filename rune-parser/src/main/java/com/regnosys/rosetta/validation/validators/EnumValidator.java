package com.regnosys.rosetta.validation.validators;

import com.regnosys.rosetta.ast.RRootElement;
import com.regnosys.rosetta.ast.supporting.REnumValue;
import com.regnosys.rosetta.ast.types.REnumeration;
import com.regnosys.rosetta.validation.ValidationCollector;
import com.regnosys.rosetta.validation.ValidationIssueCode;
import com.regnosys.rosetta.validation.Validator;

import java.util.*;

/**
 * Validates enumeration declarations:
 * - No cyclic extension chains (A extends B extends A)
 * - Value names unique across inheritance hierarchy
 *
 * <p>Upstream: EnumValidator.checkCyclicExtensions,
 * EnumValidator.checkEnumValuesAreUnique.
 */
public final class EnumValidator implements Validator {

    @Override
    public void validate(RRootElement element, ValidationCollector collector) {
        if (!(element instanceof REnumeration en)) return;

        checkCyclicExtension(en, collector);
        checkInheritedValueUniqueness(en, collector);
    }

    private void checkCyclicExtension(REnumeration en, ValidationCollector collector) {
        Set<REnumeration> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        visited.add(en);
        REnumeration current = en.superType().orElse(null);
        int safety = 0;
        while (current != null && safety++ < 1000) {
            if (!visited.add(current)) {
                collector.error(
                    en.sourceRange(),
                    "Cyclic enum extension: '" + en.name() + "' has a circular inheritance chain",
                    ValidationIssueCode.TYPE_ERROR);
                return;
            }
            current = current.superType().orElse(null);
        }
    }

    private void checkInheritedValueUniqueness(REnumeration en, ValidationCollector collector) {
        if (en.superType().isEmpty()) return;

        // Collect all inherited value names
        Set<String> inheritedNames = new HashSet<>();
        REnumeration ancestor = en.superType().orElse(null);
        Set<REnumeration> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        int safety = 0;
        while (ancestor != null && safety++ < 1000) {
            if (!visited.add(ancestor)) break; // cycle — already reported
            for (REnumValue val : ancestor.values()) {
                if (val.name() != null) inheritedNames.add(val.name());
            }
            ancestor = ancestor.superType().orElse(null);
        }

        // Check own values against inherited
        for (REnumValue val : en.values()) {
            if (val.name() != null && inheritedNames.contains(val.name())) {
                collector.error(
                    val.sourceRange(),
                    "Enum value '" + val.name() + "' conflicts with inherited value from parent enum",
                    ValidationIssueCode.DUPLICATE_ENUM_VALUE);
            }
        }
    }
}
