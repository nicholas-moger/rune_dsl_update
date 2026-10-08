package com.regnosys.rosetta.validation.validators;

import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.supporting.REnumValue;
import com.regnosys.rosetta.ast.supporting.RTypeCall;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.ast.types.REnumeration;
import com.regnosys.rosetta.validation.ValidationCollector;
import com.regnosys.rosetta.validation.ValidationIssueCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UniquenessValidatorTest {

    private final UniquenessValidator validator = new UniquenessValidator();

    // === Attribute uniqueness within data type ================================

    @Test void unique_attributes_pass() {
        var dt = new RDataType(); dt.setName("Foo");
        addAttr(dt, "bar");
        addAttr(dt, "baz");
        var c = new ValidationCollector();
        validator.validate(dt, c);
        assertTrue(c.isEmpty());
    }

    @Test void duplicate_attributes_error() {
        var dt = new RDataType(); dt.setName("Foo");
        addAttr(dt, "bar");
        addAttr(dt, "bar");
        var c = new ValidationCollector();
        validator.validate(dt, c);
        assertFalse(c.isEmpty());
        assertEquals(ValidationIssueCode.DUPLICATE_ATTRIBUTE, c.toList().get(0).issueCode());
    }

    @Test void three_duplicates_two_errors() {
        var dt = new RDataType(); dt.setName("Foo");
        addAttr(dt, "bar");
        addAttr(dt, "bar");
        addAttr(dt, "bar");
        var c = new ValidationCollector();
        validator.validate(dt, c);
        assertEquals(2, c.size()); // second and third are duplicates
    }

    // === Enum value uniqueness ================================================

    @Test void unique_enum_values_pass() {
        var en = new REnumeration(); en.setName("Color");
        addEnumVal(en, "Red");
        addEnumVal(en, "Blue");
        var c = new ValidationCollector();
        validator.validate(en, c);
        assertTrue(c.isEmpty());
    }

    @Test void duplicate_enum_values_error() {
        var en = new REnumeration(); en.setName("Color");
        addEnumVal(en, "Red");
        addEnumVal(en, "Red");
        var c = new ValidationCollector();
        validator.validate(en, c);
        assertFalse(c.isEmpty());
        assertEquals(ValidationIssueCode.DUPLICATE_ENUM_VALUE, c.toList().get(0).issueCode());
    }

    // === No elements — no errors =============================================

    @Test void empty_type_passes() {
        var dt = new RDataType(); dt.setName("Empty");
        var c = new ValidationCollector();
        validator.validate(dt, c);
        assertTrue(c.isEmpty());
    }

    @Test void empty_enum_passes() {
        var en = new REnumeration(); en.setName("Empty");
        var c = new ValidationCollector();
        validator.validate(en, c);
        assertTrue(c.isEmpty());
    }

    private void addAttr(RDataType dt, String name) {
        var attr = new RAttribute();
        attr.setName(name);
        var tc = new RTypeCall(); tc.setTypeName("int");
        attr.setTypeCall(tc);
        dt.attributes().add(attr);
    }

    private void addEnumVal(REnumeration en, String name) {
        var val = new REnumValue();
        val.setName(name);
        en.values().add(val);
    }
}
