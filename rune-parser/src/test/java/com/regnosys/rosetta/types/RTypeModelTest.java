package com.regnosys.rosetta.types;

import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.ast.types.REnumeration;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.*;

/**
 * M4 T1 — tests for the sealed interface RType and all 9 variants.
 */
class RTypeModelTest {

    // === RBasicType =========================================================

    @Test void basic_boolean() {
        RType t = RBasicType.BOOLEAN;
        assertEquals("boolean", t.name());
        assertTrue(t.hasNaturalOrder());
    }

    @Test void basic_nothing_is_bottom() {
        assertEquals("nothing", RBasicType.NOTHING.name());
        assertTrue(RBasicType.NOTHING.hasNaturalOrder());
    }

    @Test void basic_any_is_top() {
        assertEquals("any", RBasicType.ANY.name());
        assertFalse(RBasicType.ANY.hasNaturalOrder());
    }

    @Test void basic_types_are_singletons() {
        assertSame(RBasicType.BOOLEAN, RBasicType.BOOLEAN);
        assertSame(RBasicType.NOTHING, RBasicType.NOTHING);
    }

    // === RMissingType ========================================================

    @Test void missing_is_singleton() {
        assertSame(RMissingType.INSTANCE, RMissingType.INSTANCE);
        assertEquals("MISSING", RMissingType.INSTANCE.name());
        assertFalse(RMissingType.INSTANCE.hasNaturalOrder());
    }

    // === RNumberType =========================================================

    @Test void number_unconstrained() {
        var t = RNumberType.unconstrained();
        assertEquals("number", t.name());
        assertTrue(t.hasNaturalOrder());
        assertTrue(t.digits().isEmpty());
        assertTrue(t.fractionalDigits().isEmpty());
    }

    @Test void number_with_constraints() {
        var t = new RNumberType(OptionalInt.of(5), OptionalInt.of(2),
            Optional.empty(), Optional.empty());
        assertEquals(5, t.digits().orElseThrow());
        assertEquals(2, t.fractionalDigits().orElseThrow());
        assertFalse(t.isInteger());
    }

    @Test void int_is_number_with_zero_fractional() {
        var t = RNumberType.intType();
        assertTrue(t.isInteger());
        assertEquals(0, t.fractionalDigits().orElseThrow());
    }

    @Test void number_equality() {
        var a = new RNumberType(OptionalInt.of(5), OptionalInt.of(2),
            Optional.empty(), Optional.empty());
        var b = new RNumberType(OptionalInt.of(5), OptionalInt.of(2),
            Optional.empty(), Optional.empty());
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    // === RStringType =========================================================

    @Test void string_unconstrained() {
        var t = RStringType.unconstrained();
        assertEquals("string", t.name());
        assertTrue(t.minLength().isEmpty());
        assertTrue(t.maxLength().isEmpty());
        assertTrue(t.pattern().isEmpty());
    }

    @Test void string_with_constraints() {
        var t = new RStringType(OptionalInt.of(1), OptionalInt.of(100), Optional.empty());
        assertEquals(1, t.minLength().orElseThrow());
        assertEquals(100, t.maxLength().orElseThrow());
    }

    // === RRecordType =========================================================

    @Test void date_record_type() {
        var t = RRecordType.DATE;
        assertEquals("date", t.name());
        assertTrue(t.hasNaturalOrder());
        assertEquals(RecordKind.DATE, t.kind());
        assertEquals(3, t.features().size()); // day, month, year
    }

    @Test void dateTime_record_type() {
        var t = RRecordType.DATE_TIME;
        assertEquals("dateTime", t.name());
        assertEquals(RecordKind.DATE_TIME, t.kind());
        assertEquals(2, t.features().size()); // date, time
    }

    @Test void zonedDateTime_record_type() {
        var t = RRecordType.ZONED_DATE_TIME;
        assertEquals("zonedDateTime", t.name());
        assertEquals(RecordKind.ZONED_DATE_TIME, t.kind());
        assertEquals(3, t.features().size()); // date, time, timezone
    }

    // === RDataTypeRef ========================================================

    @Test void data_type_ref_holds_ast_node() {
        var ast = new RDataType();
        ast.setName("Trade");
        var t = new RDataTypeRef(ast);
        assertEquals("Trade", t.name());
        assertFalse(t.hasNaturalOrder());
        assertSame(ast, t.astNode());
    }

    // === REnumTypeRef ========================================================

    @Test void enum_type_ref_holds_ast_node() {
        var ast = new REnumeration();
        ast.setName("Color");
        var t = new REnumTypeRef(ast);
        assertEquals("Color", t.name());
        assertFalse(t.hasNaturalOrder());
        assertSame(ast, t.astNode());
    }

    // === RChoiceTypeRef ======================================================

    @Test void choice_type_ref() {
        var a = new RDataType(); a.setName("Bond");
        var b = new RDataType(); b.setName("Equity");
        var t = new RChoiceTypeRef("Instrument",
            List.of(new RDataTypeRef(a), new RDataTypeRef(b)));
        assertEquals("Instrument", t.name());
        assertEquals(2, t.options().size());
    }

    // === RAliasType ==========================================================

    @Test void alias_type_wraps_underlying() {
        var t = new RAliasType("Max", java.util.Map.of("n", 5),
            new RNumberType(OptionalInt.of(5), OptionalInt.of(0),
                Optional.empty(), Optional.empty()));
        assertEquals("Max", t.name());
        assertInstanceOf(RNumberType.class, t.refersTo());
    }

    // === Sealed interface exhaustive switch ==================================

    @Test void sealed_switch_is_exhaustive() {
        RType t = RBasicType.BOOLEAN;
        String result = switch (t) {
            case RDataTypeRef d -> "data";
            case REnumTypeRef e -> "enum";
            case RChoiceTypeRef c -> "choice";
            case RBasicType b -> "basic:" + b.name();
            case RNumberType n -> "number";
            case RStringType s -> "string";
            case RRecordType r -> "record:" + r.kind();
            case RAliasType a -> "alias";
            case RMissingType m -> "missing";
        };
        assertEquals("basic:boolean", result);
    }

    @Test void sealed_switch_on_missing() {
        RType t = RMissingType.INSTANCE;
        String result = switch (t) {
            case RDataTypeRef d -> "data";
            case REnumTypeRef e -> "enum";
            case RChoiceTypeRef c -> "choice";
            case RBasicType b -> "basic";
            case RNumberType n -> "number";
            case RStringType s -> "string";
            case RRecordType r -> "record";
            case RAliasType a -> "alias";
            case RMissingType m -> "missing";
        };
        assertEquals("missing", result);
    }
}
