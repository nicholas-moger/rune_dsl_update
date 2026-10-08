package com.regnosys.rosetta.validation.validators;

import com.regnosys.rosetta.ast.supporting.REnumValue;
import com.regnosys.rosetta.ast.types.REnumeration;
import com.regnosys.rosetta.symbols.diagnostics.Severity;
import com.regnosys.rosetta.symbols.symbolid.TestSymbolResolver;
import com.regnosys.rosetta.validation.ValidationCollector;
import com.regnosys.rosetta.validation.ValidationIssueCode;
import java.lang.ref.Reference;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EnumValidatorTest {

    private final EnumValidator validator = new EnumValidator();

    // === Cyclic extension =====================================================

    @Test void no_cycle_passes() {
        var resolver = new TestSymbolResolver();
        var parent = new REnumeration(); parent.setName("Parent");
        var child = new REnumeration(); child.setName("Child");
        resolver.wireSuperType(child, "test", "Parent", parent, child::setSuperTypeId);
        var c = new ValidationCollector();
        validator.validate(child, c);
        assertTrue(c.isEmpty());
        Reference.reachabilityFence(resolver);
    }

    @Test void self_cycle_errors() {
        var resolver = new TestSymbolResolver();
        var en = new REnumeration(); en.setName("Loop");
        // localName matches the enum's actual name so the SymbolId reflects a
        // realistic `extends Loop` source — consistent with TypeValidatorTest.self_cycle_errors.
        resolver.wireSuperType(en, "test", "Loop", en, en::setSuperTypeId);
        var c = new ValidationCollector();
        validator.validate(en, c);
        assertFalse(c.isEmpty());
        assertEquals(Severity.ERROR, c.toList().get(0).severity());
        Reference.reachabilityFence(resolver);
    }

    @Test void two_step_cycle_errors() {
        var resolver = new TestSymbolResolver();
        var a = new REnumeration(); a.setName("A");
        var b = new REnumeration(); b.setName("B");
        // Wire A -> B and B -> A (two-step cycle)
        resolver.wireSuperType(a, "test", "B", b, a::setSuperTypeId);
        resolver.wireSuperType(b, "test", "A", a, b::setSuperTypeId);
        var c = new ValidationCollector();
        validator.validate(a, c);
        assertFalse(c.isEmpty());
        Reference.reachabilityFence(resolver);
    }

    // === Value uniqueness across inheritance ==================================

    @Test void inherited_value_conflict_errors() {
        var resolver = new TestSymbolResolver();
        var parent = new REnumeration(); parent.setName("Parent");
        addValue(parent, "Red");

        var child = new REnumeration(); child.setName("Child");
        addValue(child, "Red"); // conflicts with parent
        resolver.wireSuperType(child, "test", "Parent", parent, child::setSuperTypeId);

        var c = new ValidationCollector();
        validator.validate(child, c);
        assertFalse(c.isEmpty());
        assertEquals(ValidationIssueCode.DUPLICATE_ENUM_VALUE, c.toList().get(0).issueCode());
        Reference.reachabilityFence(resolver);
    }

    @Test void no_inherited_conflict_passes() {
        var resolver = new TestSymbolResolver();
        var parent = new REnumeration(); parent.setName("Parent");
        addValue(parent, "Red");

        var child = new REnumeration(); child.setName("Child");
        addValue(child, "Blue"); // no conflict
        resolver.wireSuperType(child, "test", "Parent", parent, child::setSuperTypeId);

        var c = new ValidationCollector();
        validator.validate(child, c);
        assertTrue(c.isEmpty());
        Reference.reachabilityFence(resolver);
    }

    // === Non-enum elements ignored ===========================================

    @Test void non_enum_ignored() {
        var dt = new com.regnosys.rosetta.ast.types.RDataType();
        dt.setName("Foo");
        var c = new ValidationCollector();
        validator.validate(dt, c);
        assertTrue(c.isEmpty());
    }

    private void addValue(REnumeration en, String name) {
        var val = new REnumValue();
        val.setName(name);
        en.values().add(val);
    }
}
