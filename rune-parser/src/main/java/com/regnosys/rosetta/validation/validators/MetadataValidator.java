package com.regnosys.rosetta.validation.validators;

import com.regnosys.rosetta.ast.RRootElement;
import com.regnosys.rosetta.ast.annotations.RAnnotationRef;
import com.regnosys.rosetta.ast.functions.RCondition;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.supporting.RChoiceOption;
import com.regnosys.rosetta.ast.supporting.REnumValue;
import com.regnosys.rosetta.ast.types.RChoice;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.ast.types.REnumeration;
import com.regnosys.rosetta.validation.ValidationCollector;
import com.regnosys.rosetta.validation.ValidationIssueCode;
import com.regnosys.rosetta.validation.Validator;

import java.util.List;

/**
 * Validates metadata annotation PLACEMENT — upstream
 * {@code RosettaSimpleValidator.checkMetadataAnnotation(Annotated)} (vendored
 * {@code :795-886}), message bytes identical:
 * <ul>
 *   <li>{@code [metadata key]} and {@code [metadata template]} only on a TYPE
 *       (upstream {@code Data} — a choice is a {@code Data} upstream, so the fork's
 *       {@link RChoice} counts as a type here); a template type must also carry
 *       {@code key};</li>
 *   <li>{@code [metadata id]}, {@code [metadata reference]}, {@code [metadata location]}
 *       and {@code [metadata address]} only on an ATTRIBUTE (upstream {@code Attribute};
 *       a choice OPTION is an attribute upstream, and a function's inputs and output
 *       are attributes);</li>
 *   <li>{@code [metadata scheme]} on an attribute or a type.</li>
 * </ul>
 * The hosts are the eight grammar rules upstream's {@code Annotated} fragment reaches
 * ({@code Rosetta.xtext}: Data, Choice, ChoiceOption, Attribute, Enumeration, Function,
 * Condition, RosettaEnumValue), walked STRUCTURALLY from the root element — a type's
 * own refs are the type's, its attributes' refs are attributes', its conditions' and
 * an enumeration's values' refs are neither — never through the parent link, so a
 * bare {@link RDataType} assembled without tree wiring (the unit-test shape) reads the
 * same as a parsed one. Root kinds that carry no {@code [metadata …]} grammatically
 * (rules, reports, type aliases, synonym and rule sources, annotations, bodies,
 * corpora, segments, library functions) are not walked. Everything else upstream
 * checks at this seat ({@code location}'s {@code pointsTo} qualifier, {@code
 * address}'s target type) is qualifier-path validation the fork's {@code
 * RAnnotationRef} does not yet model; it is not reached by any corpus model (the V0
 * oracle streams are error-free) and is the recorded follow-on.
 *
 * <p>v3.1 CLOSE-OUT (the parser's standing conformance red, list (a)): the
 * attribute-level {@code [metadata key]} of {@code a2-choice-option-other-seats:45}
 * resolves upstream — the resolver admits the face — but upstream's VALIDATOR
 * rejects it, and the fork reported nothing. {@link com.regnosys.rosetta.types.inference.TypeDirectedResolver#isMetaFaceOnAttribute}
 * records the same split from the resolution side.
 *
 * <p>The fork-native missing-qualifier WARNING is kept, over the same eight hosts.
 */
public final class MetadataValidator implements Validator {

    private enum Host { TYPE, ATTRIBUTE, OTHER }

    @Override
    public void validate(RRootElement element, ValidationCollector collector) {
        if (element instanceof RDataType dt) {
            check(dt.annotationRefs(), Host.TYPE, collector);
            for (RAttribute attribute : dt.attributes()) {
                check(attribute.annotationRefs(), Host.ATTRIBUTE, collector);
            }
            for (RCondition condition : dt.conditions()) {
                check(condition.annotationRefs(), Host.OTHER, collector);
            }
        } else if (element instanceof RChoice ch) {
            check(ch.annotationRefs(), Host.TYPE, collector);
            for (RChoiceOption option : ch.options()) {
                check(option.annotationRefs(), Host.ATTRIBUTE, collector);
            }
        } else if (element instanceof RFunction fn) {
            check(fn.annotationRefs(), Host.OTHER, collector);
            for (RAttribute input : fn.inputs()) {
                check(input.annotationRefs(), Host.ATTRIBUTE, collector);
            }
            fn.output().ifPresent(out -> check(out.annotationRefs(), Host.ATTRIBUTE, collector));
            for (RCondition condition : fn.conditions()) {
                check(condition.annotationRefs(), Host.OTHER, collector);
            }
        } else if (element instanceof REnumeration en) {
            check(en.annotationRefs(), Host.OTHER, collector);
            for (REnumValue value : en.values()) {
                check(value.annotationRefs(), Host.OTHER, collector);
            }
        }
    }

    private void check(List<RAnnotationRef> refs, Host host, ValidationCollector collector) {
        for (RAnnotationRef ref : refs) {
            checkMetadataAnnotation(ref, host, collector);
        }
        if (host == Host.TYPE) {
            checkTemplateHasKey(refs, collector);
        }
    }

    private void checkMetadataAnnotation(RAnnotationRef ref, Host host, ValidationCollector collector) {
        if (!"metadata".equals(ref.annotationName())) return;

        String qualifier = ref.qualifierName().orElse(null);
        if (qualifier == null) {
            collector.warning(
                ref.sourceRange(),
                "[metadata] annotation missing qualifier (expected key, scheme, reference, etc.)",
                ValidationIssueCode.INVALID_TYPE);
            return;
        }
        boolean onType = host == Host.TYPE;
        boolean onAttribute = host == Host.ATTRIBUTE;
        switch (qualifier) {
            case "key", "template" -> {
                if (!onType) {
                    placementError(ref, "[metadata " + qualifier + "] annotation only allowed on a type.", collector);
                }
            }
            case "id", "reference", "location", "address" -> {
                if (!onAttribute) {
                    placementError(ref, "[metadata " + qualifier + "] annotation only allowed on an attribute.", collector);
                }
            }
            case "scheme" -> {
                if (!onAttribute && !onType) {
                    placementError(ref, "[metadata scheme] annotation only allowed on an attribute or a type.", collector);
                }
            }
            default -> {
                // No-op — upstream's default arm.
            }
        }
    }

    /** Upstream: a type carrying {@code [metadata template]} must also carry {@code [metadata key]}. */
    private void checkTemplateHasKey(List<RAnnotationRef> refs, ValidationCollector collector) {
        boolean hasKey = refs.stream().anyMatch(r -> isMetadata(r, "key"));
        for (RAnnotationRef ref : refs) {
            if (isMetadata(ref, "template") && !hasKey) {
                placementError(ref,
                        "Types with [metadata template] annotation must also specify the [metadata key] annotation.",
                        collector);
            }
        }
    }

    private static boolean isMetadata(RAnnotationRef ref, String qualifier) {
        return "metadata".equals(ref.annotationName())
                && ref.qualifierName().map(qualifier::equals).orElse(false);
    }

    private static void placementError(RAnnotationRef ref, String message, ValidationCollector collector) {
        collector.error(ref.sourceRange(), message, ValidationIssueCode.INVALID_TYPE);
    }
}
