package com.regnosys.rosetta.validation.validators;

import com.regnosys.rosetta.ast.RRootElement;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.supporting.REnumValue;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.ast.types.REnumeration;
import com.regnosys.rosetta.validation.ValidationCollector;
import com.regnosys.rosetta.validation.ValidationIssueCode;
import com.regnosys.rosetta.validation.Validator;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Validates local name uniqueness within declarations:
 * - Attribute names unique within a data type
 * - Enum value names unique within an enumeration
 *
 * <p>Global uniqueness (duplicate type/function names across files) is
 * handled by M3's symbol registration pass.
 *
 * <p>Upstream: NamesAreUniqueValidator + TypeValidator.checkAttributeNamesAreUnique,
 * EnumValidator.checkEnumValuesAreUnique.
 */
public final class UniquenessValidator implements Validator {

    @Override
    public void validate(RRootElement element, ValidationCollector collector) {
        if (element instanceof RDataType dt) {
            checkAttributeUniqueness(dt.attributes(), collector);
        }
        if (element instanceof REnumeration en) {
            checkEnumValueUniqueness(en.values(), collector);
        }
    }

    private void checkAttributeUniqueness(List<RAttribute> attrs, ValidationCollector collector) {
        Set<String> seen = new HashSet<>();
        for (RAttribute attr : attrs) {
            if (attr.name() != null && !seen.add(attr.name())) {
                collector.error(
                    attr.sourceRange(),
                    "Duplicate attribute '" + attr.name() + "'",
                    ValidationIssueCode.DUPLICATE_ATTRIBUTE);
            }
        }
    }

    private void checkEnumValueUniqueness(List<REnumValue> values, ValidationCollector collector) {
        Set<String> seen = new HashSet<>();
        for (REnumValue val : values) {
            if (val.name() != null && !seen.add(val.name())) {
                collector.error(
                    val.sourceRange(),
                    "Duplicate enum value '" + val.name() + "'",
                    ValidationIssueCode.DUPLICATE_ENUM_VALUE);
            }
        }
    }
}
