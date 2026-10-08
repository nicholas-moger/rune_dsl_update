package com.regnosys.rosetta.types.relation;

import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.symbols.symbolid.TestSymbolResolver;
import com.regnosys.rosetta.types.*;
import java.lang.ref.Reference;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class TypeJoinTest {

    private final TypeJoin join = new TypeJoin(new SubtypeRelation());

    // === Identity =============================================================

    @Test void join_same_type_is_itself() {
        assertEquals(RBasicType.BOOLEAN, join.join(RBasicType.BOOLEAN, RBasicType.BOOLEAN));
    }

    // === Nothing (bottom) =====================================================

    @Test void join_nothing_with_type_returns_type() {
        assertEquals(RBasicType.BOOLEAN, join.join(RBasicType.NOTHING, RBasicType.BOOLEAN));
        assertEquals(RBasicType.BOOLEAN, join.join(RBasicType.BOOLEAN, RBasicType.NOTHING));
    }

    // === Any (top) ============================================================

    @Test void join_any_with_type_returns_any() {
        assertEquals(RBasicType.ANY, join.join(RBasicType.ANY, RBasicType.BOOLEAN));
        assertEquals(RBasicType.ANY, join.join(RBasicType.BOOLEAN, RBasicType.ANY));
    }

    // === Numbers ==============================================================

    @Test void join_numbers_widens_digits() {
        var n5 = new RNumberType(OptionalInt.of(5), OptionalInt.of(2),
            Optional.empty(), Optional.empty());
        var n3 = new RNumberType(OptionalInt.of(3), OptionalInt.of(1),
            Optional.empty(), Optional.empty());
        RType result = join.join(n5, n3);
        assertInstanceOf(RNumberType.class, result);
        var r = (RNumberType) result;
        assertEquals(5, r.digits().orElseThrow());
        assertEquals(2, r.fractionalDigits().orElseThrow());
    }

    @Test void join_unconstrained_numbers() {
        RType result = join.join(RNumberType.unconstrained(), RNumberType.intType());
        assertInstanceOf(RNumberType.class, result);
    }

    // === Strings ==============================================================

    @Test void join_strings_widens_length() {
        var s1 = new RStringType(OptionalInt.of(1), OptionalInt.of(10), Optional.empty());
        var s2 = new RStringType(OptionalInt.of(5), OptionalInt.of(20), Optional.empty());
        RType result = join.join(s1, s2);
        assertInstanceOf(RStringType.class, result);
        var r = (RStringType) result;
        assertEquals(1, r.minLength().orElseThrow()); // min of mins
        assertEquals(20, r.maxLength().orElseThrow()); // max of maxes
    }

    // === Data types (common ancestor) =========================================

    @Test void join_child_and_parent_returns_parent() {
        var resolver = new TestSymbolResolver();
        var parent = new RDataType(); parent.setName("Parent");
        var child = new RDataType(); child.setName("Child");
        resolver.wireSuperType(child, "test", "Parent", parent, child::setSuperTypeId);
        RType result = join.join(new RDataTypeRef(child), new RDataTypeRef(parent));
        assertInstanceOf(RDataTypeRef.class, result);
        assertEquals("Parent", result.name());
        Reference.reachabilityFence(resolver);
    }

    @Test void join_siblings_returns_common_ancestor() {
        var resolver = new TestSymbolResolver();
        var gp = new RDataType(); gp.setName("GrandParent");
        var a = new RDataType(); a.setName("A");
        resolver.wireSuperType(a, "test", "GrandParent", gp, a::setSuperTypeId);
        var b = new RDataType(); b.setName("B");
        resolver.wireSuperType(b, "test", "GrandParent", gp, b::setSuperTypeId);
        RType result = join.join(new RDataTypeRef(a), new RDataTypeRef(b));
        assertInstanceOf(RDataTypeRef.class, result);
        assertEquals("GrandParent", result.name());
        Reference.reachabilityFence(resolver);
    }

    @Test void join_unrelated_data_types_returns_any() {
        var a = new RDataType(); a.setName("A");
        var b = new RDataType(); b.setName("B");
        assertEquals(RBasicType.ANY, join.join(new RDataTypeRef(a), new RDataTypeRef(b)));
    }

    // === Unrelated types ======================================================

    @Test void join_boolean_and_number_returns_any() {
        assertEquals(RBasicType.ANY, join.join(RBasicType.BOOLEAN, RNumberType.unconstrained()));
    }

    @Test void join_string_and_number_returns_any() {
        assertEquals(RBasicType.ANY, join.join(RStringType.unconstrained(), RNumberType.unconstrained()));
    }

    // === Alias transparency ===================================================

    @Test void join_alias_unwraps() {
        var alias = new RAliasType("MyBool", Map.of(), RBasicType.BOOLEAN);
        assertEquals(RBasicType.BOOLEAN, join.join(alias, RBasicType.BOOLEAN));
    }

    // === Missing ==============================================================

    @Test void join_missing_with_type_returns_type() {
        assertEquals(RBasicType.BOOLEAN, join.join(RMissingType.INSTANCE, RBasicType.BOOLEAN));
        assertEquals(RBasicType.BOOLEAN, join.join(RBasicType.BOOLEAN, RMissingType.INSTANCE));
    }

    // === Meta-annotated =======================================================

    @Test void join_meta_annotated() {
        var a = RMetaAnnotatedType.withNoMeta(RBasicType.BOOLEAN);
        var b = RMetaAnnotatedType.withNoMeta(RBasicType.BOOLEAN);
        var result = join.join(a, b);
        assertEquals(RBasicType.BOOLEAN, result.type());
    }
}
