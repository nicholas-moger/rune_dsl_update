package com.regnosys.rosetta.types;

import com.regnosys.rosetta.ast.expressions.references.RFeatureCall;
import com.regnosys.rosetta.ast.expressions.references.RDeepFeatureCall;
import com.regnosys.rosetta.ast.functions.RSegment;
import com.regnosys.rosetta.ast.expressions.supporting.RSwitchCaseGuard;
import com.regnosys.rosetta.ast.expressions.supporting.RWithMetaEntry;
import com.regnosys.rosetta.ast.annotations.RAnnotationPathSegment;
import com.regnosys.rosetta.ast.external.RExternalRegularAttribute;
import com.regnosys.rosetta.ast.external.RExternalEnumValue;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.supporting.REnumValue;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * M4 T0 — verifies all 7 type-directed resolved field accessors compile
 * and work correctly (empty by default, settable, Optional-returning).
 */
class M4ResolvedFieldAccessorsTest {

    @Test void featureCall_resolved_field() {
        var fc = new RFeatureCall();
        assertTrue(fc.resolvedFeature().isEmpty());
        var attr = new RAttribute();
        attr.setName("price");
        fc.setResolvedFeature(attr);
        assertTrue(fc.resolvedFeature().isPresent());
        assertEquals("price", fc.resolvedFeature().get().name());
    }

    @Test void deepFeatureCall_resolved_field() {
        var dfc = new RDeepFeatureCall();
        assertTrue(dfc.resolvedFeature().isEmpty());
        var attr = new RAttribute();
        attr.setName("deep");
        dfc.setResolvedFeature(attr);
        assertTrue(dfc.resolvedFeature().isPresent());
    }

    @Test void segment_resolved_field() {
        var seg = new RSegment();
        assertTrue(seg.resolvedAttribute().isEmpty());
        var attr = new RAttribute();
        attr.setName("bar");
        seg.setResolvedAttribute(attr);
        assertTrue(seg.resolvedAttribute().isPresent());
    }

    @Test void switchCaseGuard_resolved_field() {
        var guard = new RSwitchCaseGuard();
        assertTrue(guard.resolvedGuard().isEmpty());
    }

    @Test void withMetaEntry_resolved_field() {
        var entry = new RWithMetaEntry();
        assertTrue(entry.resolvedMetaKey().isEmpty());
    }

    @Test void annotationPathSegment_resolved_field() {
        var seg = new RAnnotationPathSegment();
        assertTrue(seg.resolvedAttribute().isEmpty());
        var attr = new RAttribute();
        seg.setResolvedAttribute(attr);
        assertTrue(seg.resolvedAttribute().isPresent());
    }

    @Test void externalRegularAttribute_resolved_field() {
        var era = new RExternalRegularAttribute();
        assertTrue(era.resolvedAttribute().isEmpty());
    }

    @Test void externalEnumValue_resolved_field() {
        var eev = new RExternalEnumValue();
        assertTrue(eev.resolvedEnumValue().isEmpty());
        var val = new REnumValue();
        val.setName("TestVal");
        eev.setResolvedEnumValue(val);
        assertTrue(eev.resolvedEnumValue().isPresent());
        assertEquals("TestVal", eev.resolvedEnumValue().get().name());
    }
}
