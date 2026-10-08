package com.regnosys.rosetta.validation;

import com.regnosys.rosetta.ast.RRootElement;
import com.regnosys.rosetta.ast.annotations.RAnnotationRef;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.types.RChoice;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.ast.types.REnumeration;
import com.regnosys.rosetta.ast.types.RTypeAlias;

import java.util.Collections;
import java.util.List;

/**
 * Checks whether a warning should be suppressed via a
 * {@code [suppressWarnings <code>]} annotation on the enclosing declaration.
 * Only checks direct annotations on the root element — NOT nested annotations
 * on attributes, enum values, etc.
 *
 * <p>Spec: D5 in {@code docs/specs/2026-04-09-m6-validation-design.md}.
 */
public final class WarningSuppressionHelper {

    private static final String SUPPRESS_WARNINGS = "suppressWarnings";

    /**
     * Returns true if the root element has a direct
     * {@code [suppressWarnings <code>]} annotation matching the given code.
     */
    public static boolean isSuppressed(RRootElement element, String warningCode) {
        return isSuppressedRefs(getDirectAnnotations(element), warningCode);
    }

    /**
     * Ref-list form for non-root annotated nodes (conditions, attributes —
     * upstream's {@code isCapitalisationSuppressed(EObject)} accepts any
     * {@code Annotated} object; upstream {@code Condition} and
     * {@code Attribute} both are).
     */
    public static boolean isSuppressedRefs(List<RAnnotationRef> refs, String warningCode) {
        for (RAnnotationRef ref : refs) {
            if (SUPPRESS_WARNINGS.equals(ref.annotationName())) {
                if (ref.qualifierName().isEmpty()) return true; // suppress all
                if (warningCode.equals(ref.qualifierName().orElse(null))) return true;
            }
        }
        return false;
    }

    /** Gets only the direct annotation refs on the root element (not nested). */
    private static List<RAnnotationRef> getDirectAnnotations(RRootElement element) {
        if (element instanceof RDataType dt) return dt.annotationRefs();
        if (element instanceof REnumeration en) return en.annotationRefs();
        if (element instanceof RChoice ch) return ch.annotationRefs();
        if (element instanceof RFunction fn) return fn.annotationRefs();
        return Collections.emptyList();
    }
}
