package com.regnosys.rosetta.validation.validators;

import com.regnosys.rosetta.ast.RRootElement;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.validation.ValidationCollector;
import com.regnosys.rosetta.validation.ValidationIssueCode;
import com.regnosys.rosetta.validation.Validator;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * Validates data type declarations:
 * - No cyclic type extension chains (A extends B extends A)
 *
 * <p>Upstream: TypeValidator.checkCyclicExtensions,
 * TypeValidator.checkDoNotExtendChoice.
 */
public final class TypeValidator implements Validator {

    @Override
    public void validate(RRootElement element, ValidationCollector collector) {
        if (!(element instanceof RDataType dt)) return;

        checkCyclicExtension(dt, collector);
        checkDoNotExtendChoice(dt, collector);
    }

    /**
     * Upstream {@code TypeValidator.checkDoNotExtendChoice} (vendored
     * {@code :39-46}, released-jar-verified {@code warning}, facet
     * warningFamilyWaves PR #455): a data type whose supertype is a CHOICE
     * fires the deprecation WARNING {@code Extending a choice type is
     * deprecated}, anchored at the supertype reference (upstream
     * {@code DATA__SUPER_TYPE}; the fork anchors the {@code superType}
     * token range, falling back to the type node). The fork's linker
     * already discriminates the choice-parent case
     * ({@link RDataType#choiceSuperType()} — the #451 extends-choice view).
     * The cdm bank carries 3 lines of this class.
     */
    private void checkDoNotExtendChoice(RDataType dt, ValidationCollector collector) {
        if (dt.choiceSuperType().isEmpty()) return;
        var anchor = dt.tokenRanges().get("superType");
        collector.warning(
            anchor != null ? anchor : dt.sourceRange(),
            "Extending a choice type is deprecated",
            ValidationIssueCode.DEPRECATION);
    }

    private void checkCyclicExtension(RDataType dt, ValidationCollector collector) {
        if (dt.superType().isEmpty()) return;

        Set<RDataType> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        visited.add(dt);
        RDataType current = dt.superType().orElse(null);
        int safety = 0;
        while (current != null && safety++ < 1000) {
            if (!visited.add(current)) {
                collector.error(
                    dt.sourceRange(),
                    "Cyclic type extension: '" + dt.name() + "' has a circular inheritance chain",
                    ValidationIssueCode.TYPE_ERROR);
                return;
            }
            current = current.superType().orElse(null);
        }
    }
}
