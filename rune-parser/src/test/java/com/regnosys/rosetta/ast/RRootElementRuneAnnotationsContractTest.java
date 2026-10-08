package com.regnosys.rosetta.ast;

import com.regnosys.rosetta.ast.annotations.RRuneAnnotation;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.functions.RRule;
import com.regnosys.rosetta.ast.types.RChoice;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.ast.types.REnumeration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Contract tests for the hoisted {@code runeAnnotations()} slot on
 * {@link RRootElement}. Verifies all 5 attach-site subclasses inherit
 * the field with default-empty list and that {@code addRuneAnnotation}
 * respects the freeze contract. P1.4.2 T3.
 */
class RRootElementRuneAnnotationsContractTest {

    @Test
    void rDataTypeInheritsEmptyRuneAnnotations() {
        RDataType d = new RDataType();
        assertNotNull(d.runeAnnotations());
        assertTrue(d.runeAnnotations().isEmpty());
    }

    @Test
    void rChoiceInheritsEmptyRuneAnnotations() {
        RChoice c = new RChoice();
        assertTrue(c.runeAnnotations().isEmpty());
    }

    @Test
    void rEnumerationInheritsEmptyRuneAnnotations() {
        REnumeration e = new REnumeration();
        assertTrue(e.runeAnnotations().isEmpty());
    }

    @Test
    void rFunctionInheritsEmptyRuneAnnotations() {
        RFunction f = new RFunction();
        assertTrue(f.runeAnnotations().isEmpty());
    }

    @Test
    void rRuleInheritsEmptyRuneAnnotations() {
        RRule r = new RRule();
        assertTrue(r.runeAnnotations().isEmpty());
    }

    @Test
    void addRuneAnnotationAppendsAndChecksMutable() {
        RDataType d = new RDataType();
        RRuneAnnotation a = new RRuneAnnotation();
        a.setAnnotationName("experimental");
        d.addRuneAnnotation(a);
        assertEquals(1, d.runeAnnotations().size());
        assertEquals("experimental", d.runeAnnotations().get(0).annotationName());
    }

    @Test
    void freezePreventsFurtherAddRuneAnnotation() {
        RDataType d = new RDataType();
        d.setSourceRange(SourceRange.NONE);
        d.freeze();
        RRuneAnnotation a = new RRuneAnnotation();
        a.setAnnotationName("foo");
        assertThrows(IllegalStateException.class, () -> d.addRuneAnnotation(a));
    }

    @Test
    void childrenIncludesRuneAnnotationsFromHoist() {
        RDataType d = new RDataType();
        RRuneAnnotation a = new RRuneAnnotation();
        a.setAnnotationName("foo");
        d.addRuneAnnotation(a);
        // children() of the subclass must surface hoisted runeAnnotations from RRootElement
        assertTrue(d.children().contains(a),
            "RDataType.children() must surface hoisted runeAnnotations");
    }

    @Test
    void childrenIncludesRuneAnnotationsOnRRule() {
        RRule r = new RRule();
        RRuneAnnotation a = new RRuneAnnotation();
        a.setAnnotationName("experimental");
        r.addRuneAnnotation(a);
        assertTrue(r.children().contains(a),
            "RRule.children() must surface hoisted runeAnnotations");
    }
}
