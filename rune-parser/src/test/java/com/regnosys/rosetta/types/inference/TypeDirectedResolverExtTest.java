package com.regnosys.rosetta.types.inference;

import com.regnosys.rosetta.ast.annotations.RAnnotationPathSegment;
import com.regnosys.rosetta.ast.expressions.supporting.RSwitchCaseGuard;
import com.regnosys.rosetta.ast.enums.SwitchGuardKind;
import com.regnosys.rosetta.ast.external.RExternalRegularAttribute;
import com.regnosys.rosetta.ast.external.RExternalEnumValue;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.supporting.REnumValue;
import com.regnosys.rosetta.ast.supporting.RTypeCall;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.ast.types.REnumeration;
import com.regnosys.rosetta.types.*;
import com.regnosys.rosetta.types.builtin.BuiltinTypeRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TypeDirectedResolverExtTest {

    private final BuiltinTypeRegistry builtins = BuiltinTypeRegistry.createDefault();
    private final TypeDirectedResolver resolver = new TypeDirectedResolver(builtins);

    // === External regular attribute resolution ================================

    @Test void external_regular_attribute_resolved() {
        var dt = new RDataType(); dt.setName("Trade");
        var priceTc = new RTypeCall(); priceTc.setTypeName("number");
        var priceAttr = new RAttribute();
        priceAttr.setName("price"); priceAttr.setTypeCall(priceTc);
        dt.attributes().add(priceAttr);

        var era = new RExternalRegularAttribute();
        era.setName("price");
        era.setAddition(false);

        RMetaAnnotatedType contextType = RMetaAnnotatedType.withNoMeta(new RDataTypeRef(dt));
        resolver.resolveExternalAttribute(era, contextType);

        assertTrue(era.resolvedAttribute().isPresent());
        assertEquals("price", era.resolvedAttribute().get().name());
    }

    @Test void external_regular_attribute_not_found() {
        var dt = new RDataType(); dt.setName("Trade");

        var era = new RExternalRegularAttribute();
        era.setName("nonexistent");

        resolver.resolveExternalAttribute(era, RMetaAnnotatedType.withNoMeta(new RDataTypeRef(dt)));
        assertTrue(era.resolvedAttribute().isEmpty());
    }

    // === External enum value resolution ======================================

    @Test void external_enum_value_resolved() {
        var en = new REnumeration(); en.setName("Color");
        var redVal = new REnumValue(); redVal.setName("Red");
        en.values().add(redVal);

        var eev = new RExternalEnumValue();
        eev.setName("Red");

        RMetaAnnotatedType contextType = RMetaAnnotatedType.withNoMeta(new REnumTypeRef(en));
        resolver.resolveExternalEnumValue(eev, contextType);

        assertTrue(eev.resolvedEnumValue().isPresent());
        assertEquals("Red", eev.resolvedEnumValue().get().name());
    }

    @Test void external_enum_value_not_found() {
        var en = new REnumeration(); en.setName("Color");

        var eev = new RExternalEnumValue();
        eev.setName("Purple");

        resolver.resolveExternalEnumValue(eev, RMetaAnnotatedType.withNoMeta(new REnumTypeRef(en)));
        assertTrue(eev.resolvedEnumValue().isEmpty());
    }

    // === Annotation path segment resolution ==================================

    @Test void annotation_path_segment_resolved() {
        var dt = new RDataType(); dt.setName("Trade");
        var priceTc = new RTypeCall(); priceTc.setTypeName("number");
        var priceAttr = new RAttribute();
        priceAttr.setName("price"); priceAttr.setTypeCall(priceTc);
        dt.attributes().add(priceAttr);

        var seg = new RAnnotationPathSegment();
        seg.setName("price");
        seg.setDeep(false);

        RMetaAnnotatedType contextType = RMetaAnnotatedType.withNoMeta(new RDataTypeRef(dt));
        resolver.resolveAnnotationPathSegment(seg, contextType);

        assertTrue(seg.resolvedAttribute().isPresent());
        assertEquals("price", seg.resolvedAttribute().get().name());
    }

    // === Switch case guard resolution ========================================

    @Test void switch_guard_name_resolved_to_enum_value() {
        var en = new REnumeration(); en.setName("Color");
        var redVal = new REnumValue(); redVal.setName("Red");
        en.values().add(redVal);

        var guard = new RSwitchCaseGuard();
        guard.setKind(SwitchGuardKind.NAME);
        guard.setQualifiedName("Red");

        RMetaAnnotatedType switchType = RMetaAnnotatedType.withNoMeta(new REnumTypeRef(en));
        resolver.resolveSwitchGuard(guard, switchType);

        assertTrue(guard.resolvedGuard().isPresent());
    }

    @Test void switch_guard_literal_no_resolution_needed() {
        var guard = new RSwitchCaseGuard();
        guard.setKind(SwitchGuardKind.LITERAL);
        guard.setLiteralValue("42");

        resolver.resolveSwitchGuard(guard, RMetaAnnotatedType.withNoMeta(RNumberType.intType()));
        // Literal guards don't need type-directed resolution
        assertTrue(guard.resolvedGuard().isEmpty());
    }
}
