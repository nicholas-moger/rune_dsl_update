package com.regnosys.rosetta.validation.validators;

import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.symbols.diagnostics.Severity;
import com.regnosys.rosetta.symbols.symbolid.TestSymbolResolver;
import com.regnosys.rosetta.validation.ValidationCollector;
import com.regnosys.rosetta.validation.ValidationIssueCode;
import java.lang.ref.Reference;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TypeValidatorTest {

    private final TypeValidator validator = new TypeValidator();

    // === Cyclic extension =====================================================

    @Test void no_cycle_passes() {
        var resolver = new TestSymbolResolver();
        var parent = new RDataType(); parent.setName("Parent");
        var child = new RDataType(); child.setName("Child");
        resolver.wireSuperType(child, "test", "Parent", parent, child::setSuperTypeId);
        var c = new ValidationCollector();
        validator.validate(child, c);
        assertTrue(c.isEmpty());
        Reference.reachabilityFence(resolver);
    }

    @Test void self_cycle_errors() {
        var resolver = new TestSymbolResolver();
        var dt = new RDataType(); dt.setName("Loop");
        resolver.wireSuperType(dt, "test", "Loop", dt, dt::setSuperTypeId);
        var c = new ValidationCollector();
        validator.validate(dt, c);
        assertFalse(c.isEmpty());
        assertEquals(Severity.ERROR, c.toList().get(0).severity());
        assertEquals(ValidationIssueCode.TYPE_ERROR, c.toList().get(0).issueCode());
        Reference.reachabilityFence(resolver);
    }

    @Test void two_step_cycle_errors() {
        var resolver = new TestSymbolResolver();
        var a = new RDataType(); a.setName("A");
        var b = new RDataType(); b.setName("B");
        resolver.wireSuperType(a, "test", "B", b, a::setSuperTypeId);
        resolver.wireSuperType(b, "test", "A", a, b::setSuperTypeId);
        var c = new ValidationCollector();
        validator.validate(a, c);
        assertFalse(c.isEmpty());
        Reference.reachabilityFence(resolver);
    }

    @Test void deep_chain_no_cycle_passes() {
        var resolver = new TestSymbolResolver();
        var gp = new RDataType(); gp.setName("GrandParent");
        var p = new RDataType(); p.setName("Parent");
        resolver.wireSuperType(p, "test", "GrandParent", gp, p::setSuperTypeId);
        var c2 = new RDataType(); c2.setName("Child");
        resolver.wireSuperType(c2, "test", "Parent", p, c2::setSuperTypeId);
        var c = new ValidationCollector();
        validator.validate(c2, c);
        assertTrue(c.isEmpty());
        Reference.reachabilityFence(resolver);
    }

    // === No super type — no cycle check needed ================================

    @Test void no_super_type_passes() {
        var dt = new RDataType(); dt.setName("Standalone");
        var c = new ValidationCollector();
        validator.validate(dt, c);
        assertTrue(c.isEmpty());
    }

    // === Non-data-type ignored ================================================

    @Test void non_data_type_ignored() {
        var en = new com.regnosys.rosetta.ast.types.REnumeration();
        en.setName("Color");
        var c = new ValidationCollector();
        validator.validate(en, c);
        assertTrue(c.isEmpty());
    }
}
