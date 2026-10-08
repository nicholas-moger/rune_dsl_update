package com.regnosys.rosetta.types;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RMetaAnnotatedTypeTest {

    @Test void withNoMeta_wraps_type() {
        var t = RMetaAnnotatedType.withNoMeta(RBasicType.BOOLEAN);
        assertEquals(RBasicType.BOOLEAN, t.type());
        assertTrue(t.metaAttributes().isEmpty());
        assertFalse(t.hasMeta());
    }

    @Test void withMeta_stores_attributes() {
        var t = RMetaAnnotatedType.withMeta(RBasicType.BOOLEAN, List.of("key", "template"));
        assertEquals(RBasicType.BOOLEAN, t.type());
        assertEquals(List.of("key", "template"), t.metaAttributes());
        assertTrue(t.hasMeta());
    }

    @Test void equals_based_on_type_and_meta() {
        var a = RMetaAnnotatedType.withNoMeta(RBasicType.BOOLEAN);
        var b = RMetaAnnotatedType.withNoMeta(RBasicType.BOOLEAN);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test void not_equal_if_different_meta() {
        var a = RMetaAnnotatedType.withNoMeta(RBasicType.BOOLEAN);
        var b = RMetaAnnotatedType.withMeta(RBasicType.BOOLEAN, List.of("key"));
        assertNotEquals(a, b);
    }

    @Test void missing_constant() {
        var m = RMetaAnnotatedType.MISSING;
        assertInstanceOf(RMissingType.class, m.type());
        assertFalse(m.hasMeta());
    }

    @Test void toString_without_meta() {
        assertEquals("boolean", RMetaAnnotatedType.withNoMeta(RBasicType.BOOLEAN).toString());
    }

    @Test void toString_with_meta() {
        var t = RMetaAnnotatedType.withMeta(RBasicType.BOOLEAN, List.of("key"));
        assertTrue(t.toString().contains("meta"));
    }

    @Test void isMissing_convenience() {
        assertTrue(RMetaAnnotatedType.MISSING.isMissing());
        assertFalse(RMetaAnnotatedType.withNoMeta(RBasicType.BOOLEAN).isMissing());
    }
}
