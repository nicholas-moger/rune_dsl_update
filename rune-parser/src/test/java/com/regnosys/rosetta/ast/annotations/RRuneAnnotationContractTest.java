package com.regnosys.rosetta.ast.annotations;

import com.regnosys.rosetta.ast.SourceRange;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Contract tests for {@link RRuneAnnotation} and {@link RRuneAnnotationArg}
 * P1.4.2 H2 / H11.
 */
class RRuneAnnotationContractTest {

    @Test
    void newAnnotationHasEmptyArgsAndNullName() {
        RRuneAnnotation a = new RRuneAnnotation();
        assertNull(a.annotationName());
        assertTrue(a.arguments().isEmpty());
    }

    @Test
    void setAnnotationNameRoundTrips() {
        RRuneAnnotation a = new RRuneAnnotation();
        a.setAnnotationName("experimental");
        assertEquals("experimental", a.annotationName());
    }

    @Test
    void argumentsListIsAppendable() {
        RRuneAnnotation a = new RRuneAnnotation();
        RRuneAnnotationArg arg = new RRuneAnnotationArg();
        arg.setName("feature");
        arg.setValueAsString("strict");
        a.arguments().add(arg);
        assertEquals(1, a.arguments().size());
    }

    @Test
    void freezePreventsFurtherSetters() {
        RRuneAnnotation a = new RRuneAnnotation();
        a.setSourceRange(SourceRange.NONE);
        a.setAnnotationName("foo");
        a.freeze();
        assertThrows(IllegalStateException.class, () -> a.setAnnotationName("bar"));
    }

    @Test
    void childrenIncludesAllArguments() {
        RRuneAnnotation a = new RRuneAnnotation();
        RRuneAnnotationArg arg1 = new RRuneAnnotationArg();
        arg1.setName("k1");
        RRuneAnnotationArg arg2 = new RRuneAnnotationArg();
        arg2.setName("k2");
        a.arguments().add(arg1);
        a.arguments().add(arg2);
        assertEquals(2, a.children().size());
    }

    @Test
    void argNewInstanceHasNullFields() {
        RRuneAnnotationArg arg = new RRuneAnnotationArg();
        assertNull(arg.name());
        assertNull(arg.valueAsString());
    }

    @Test
    void argFreezePreventsFurtherSetters() {
        RRuneAnnotationArg arg = new RRuneAnnotationArg();
        arg.setSourceRange(SourceRange.NONE);
        arg.setName("k");
        arg.setValueAsString("v");
        arg.freeze();
        assertThrows(IllegalStateException.class, () -> arg.setName("k2"));
        assertThrows(IllegalStateException.class, () -> arg.setValueAsString("v2"));
    }
}
