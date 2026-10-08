package com.regnosys.rosetta.types.relation;

import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.ast.types.REnumeration;
import com.regnosys.rosetta.symbols.symbolid.TestSymbolResolver;
import com.regnosys.rosetta.types.*;
import java.lang.ref.Reference;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class SubtypeRelationTest {

    private final SubtypeRelation sub = new SubtypeRelation();

    // === Rule 1: Identity ====================================================

    @Test void identity_basic() {
        assertTrue(sub.isSubtypeOf(RBasicType.BOOLEAN, RBasicType.BOOLEAN));
    }

    @Test void identity_number() {
        var n = RNumberType.unconstrained();
        assertTrue(sub.isSubtypeOf(n, n));
    }

    @Test void identity_missing() {
        assertTrue(sub.isSubtypeOf(RMissingType.INSTANCE, RMissingType.INSTANCE));
    }

    // === Rule 2: Bottom (nothing <= T) =======================================

    @Test void nothing_subtype_of_boolean() {
        assertTrue(sub.isSubtypeOf(RBasicType.NOTHING, RBasicType.BOOLEAN));
    }

    @Test void nothing_subtype_of_any() {
        assertTrue(sub.isSubtypeOf(RBasicType.NOTHING, RBasicType.ANY));
    }

    @Test void nothing_subtype_of_number() {
        assertTrue(sub.isSubtypeOf(RBasicType.NOTHING, RNumberType.unconstrained()));
    }

    @Test void nothing_subtype_of_data() {
        var dt = new RDataType(); dt.setName("Foo");
        assertTrue(sub.isSubtypeOf(RBasicType.NOTHING, new RDataTypeRef(dt)));
    }

    // === Rule 3: Top (T <= any) ==============================================

    @Test void boolean_subtype_of_any() {
        assertTrue(sub.isSubtypeOf(RBasicType.BOOLEAN, RBasicType.ANY));
    }

    @Test void number_subtype_of_any() {
        assertTrue(sub.isSubtypeOf(RNumberType.unconstrained(), RBasicType.ANY));
    }

    @Test void data_subtype_of_any() {
        var dt = new RDataType(); dt.setName("Foo");
        assertTrue(sub.isSubtypeOf(new RDataTypeRef(dt), RBasicType.ANY));
    }

    // === Rule 4: Number ======================================================

    @Test void constrained_number_subtype_of_unconstrained() {
        var constrained = new RNumberType(OptionalInt.of(5), OptionalInt.of(2),
            Optional.empty(), Optional.empty());
        assertTrue(sub.isSubtypeOf(constrained, RNumberType.unconstrained()));
    }

    @Test void int_subtype_of_number() {
        assertTrue(sub.isSubtypeOf(RNumberType.intType(), RNumberType.unconstrained()));
    }

    // === Rule 5: String ======================================================

    @Test void constrained_string_subtype_of_unconstrained() {
        var constrained = new RStringType(OptionalInt.of(1), OptionalInt.of(100), Optional.empty());
        assertTrue(sub.isSubtypeOf(constrained, RStringType.unconstrained()));
    }

    // === Rule 6: Data (transitive inheritance) ===============================

    @Test void child_subtype_of_parent() {
        var resolver = new TestSymbolResolver();
        var parent = new RDataType(); parent.setName("Parent");
        var child = new RDataType(); child.setName("Child");
        resolver.wireSuperType(child, "test", "Parent", parent, child::setSuperTypeId);
        assertTrue(sub.isSubtypeOf(new RDataTypeRef(child), new RDataTypeRef(parent)));
        Reference.reachabilityFence(resolver);
    }

    @Test void grandchild_subtype_of_grandparent() {
        var resolver = new TestSymbolResolver();
        var gp = new RDataType(); gp.setName("GrandParent");
        var p = new RDataType(); p.setName("Parent");
        resolver.wireSuperType(p, "test", "GrandParent", gp, p::setSuperTypeId);
        var c = new RDataType(); c.setName("Child");
        resolver.wireSuperType(c, "test", "Parent", p, c::setSuperTypeId);
        assertTrue(sub.isSubtypeOf(new RDataTypeRef(c), new RDataTypeRef(gp)));
        Reference.reachabilityFence(resolver);
    }

    @Test void parent_not_subtype_of_child() {
        var resolver = new TestSymbolResolver();
        var parent = new RDataType(); parent.setName("Parent");
        var child = new RDataType(); child.setName("Child");
        resolver.wireSuperType(child, "test", "Parent", parent, child::setSuperTypeId);
        assertFalse(sub.isSubtypeOf(new RDataTypeRef(parent), new RDataTypeRef(child)));
        Reference.reachabilityFence(resolver);
    }

    @Test void unrelated_data_types_not_subtypes() {
        var a = new RDataType(); a.setName("A");
        var b = new RDataType(); b.setName("B");
        assertFalse(sub.isSubtypeOf(new RDataTypeRef(a), new RDataTypeRef(b)));
    }

    // === Rule 7: Choice LHS (all options subtype) ============================

    @Test void choice_subtype_if_all_options_subtype() {
        var resolver = new TestSymbolResolver();
        var parent = new RDataType(); parent.setName("Parent");
        var a = new RDataType(); a.setName("A");
        resolver.wireSuperType(a, "test", "Parent", parent, a::setSuperTypeId);
        var b = new RDataType(); b.setName("B");
        resolver.wireSuperType(b, "test", "Parent", parent, b::setSuperTypeId);
        var choice = new RChoiceTypeRef("AB", List.of(new RDataTypeRef(a), new RDataTypeRef(b)));
        assertTrue(sub.isSubtypeOf(choice, new RDataTypeRef(parent)));
        Reference.reachabilityFence(resolver);
    }

    @Test void choice_not_subtype_if_some_option_not_subtype() {
        var resolver = new TestSymbolResolver();
        var parent = new RDataType(); parent.setName("Parent");
        var a = new RDataType(); a.setName("A");
        resolver.wireSuperType(a, "test", "Parent", parent, a::setSuperTypeId);
        var c = new RDataType(); c.setName("C"); // no super
        var choice = new RChoiceTypeRef("AC", List.of(new RDataTypeRef(a), new RDataTypeRef(c)));
        assertFalse(sub.isSubtypeOf(choice, new RDataTypeRef(parent)));
        Reference.reachabilityFence(resolver);
    }

    // === Rule 8: Choice RHS (any option supertype) ===========================

    @Test void type_subtype_of_choice_if_any_option_matches() {
        var a = new RDataType(); a.setName("A");
        var b = new RDataType(); b.setName("B");
        var choice = new RChoiceTypeRef("AB", List.of(new RDataTypeRef(a), new RDataTypeRef(b)));
        assertTrue(sub.isSubtypeOf(new RDataTypeRef(a), choice));
    }

    @Test void type_not_subtype_of_choice_if_no_option_matches() {
        var a = new RDataType(); a.setName("A");
        var b = new RDataType(); b.setName("B");
        var c = new RDataType(); c.setName("C");
        var choice = new RChoiceTypeRef("AB", List.of(new RDataTypeRef(a), new RDataTypeRef(b)));
        assertFalse(sub.isSubtypeOf(new RDataTypeRef(c), choice));
    }

    // === Alias transparency ==================================================

    @Test void alias_unwrapped_for_subtype_check() {
        var alias = new RAliasType("MyBool", Map.of(), RBasicType.BOOLEAN);
        assertTrue(sub.isSubtypeOf(alias, RBasicType.BOOLEAN));
        assertTrue(sub.isSubtypeOf(RBasicType.BOOLEAN, alias));
    }

    // === Negative cases ======================================================

    @Test void boolean_not_subtype_of_number() {
        assertFalse(sub.isSubtypeOf(RBasicType.BOOLEAN, RNumberType.unconstrained()));
    }

    @Test void string_not_subtype_of_number() {
        assertFalse(sub.isSubtypeOf(RStringType.unconstrained(), RNumberType.unconstrained()));
    }

    @Test void any_not_subtype_of_boolean() {
        assertFalse(sub.isSubtypeOf(RBasicType.ANY, RBasicType.BOOLEAN));
    }

    // === Cycle safety ========================================================

    @Test void circular_inheritance_does_not_loop() {
        var resolver = new TestSymbolResolver();
        var a = new RDataType(); a.setName("A");
        var b = new RDataType(); b.setName("B");
        resolver.wireSuperType(a, "test", "B", b, a::setSuperTypeId);
        resolver.wireSuperType(b, "test", "A", a, b::setSuperTypeId);
        // Should terminate without stack overflow
        assertFalse(sub.isSubtypeOf(new RDataTypeRef(a), new RDataTypeRef(new RDataType())));
        Reference.reachabilityFence(resolver);
    }

    // === Meta-annotated ======================================================

    @Test void meta_annotated_delegates_to_underlying() {
        var a = RMetaAnnotatedType.withNoMeta(RBasicType.BOOLEAN);
        var b = RMetaAnnotatedType.withNoMeta(RBasicType.ANY);
        assertTrue(sub.isSubtypeOf(a, b));
    }
}
