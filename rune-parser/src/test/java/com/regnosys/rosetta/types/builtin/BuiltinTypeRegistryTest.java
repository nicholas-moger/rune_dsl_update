package com.regnosys.rosetta.types.builtin;

import com.regnosys.rosetta.types.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BuiltinTypeRegistryTest {

    private final BuiltinTypeRegistry reg = BuiltinTypeRegistry.createDefault();

    @Test void boolean_registered() {
        var t = reg.lookup("boolean").orElseThrow();
        assertSame(RBasicType.BOOLEAN, t);
    }

    @Test void time_registered() {
        assertSame(RBasicType.TIME, reg.lookup("time").orElseThrow());
    }

    @Test void pattern_registered() {
        assertSame(RBasicType.PATTERN, reg.lookup("pattern").orElseThrow());
    }

    @Test void nothing_registered() {
        assertSame(RBasicType.NOTHING, reg.lookup("nothing").orElseThrow());
    }

    @Test void any_registered() {
        assertSame(RBasicType.ANY, reg.lookup("any").orElseThrow());
    }

    @Test void number_registered() {
        var t = reg.lookup("number").orElseThrow();
        assertInstanceOf(RNumberType.class, t);
        assertEquals("number", t.name());
    }

    @Test void int_is_number_with_zero_fractional() {
        var t = reg.lookup("int").orElseThrow();
        assertInstanceOf(RNumberType.class, t);
        assertTrue(((RNumberType) t).isInteger());
    }

    // v3.2 seat 9 (PR #630, F8 / D47) round 1 (cq SF-6): the ONE law's three cases, inside the module that declares it
    @Test void basicOrRecordNodeType_builtinsModelDeclaration_isItsRegisteredTwin() {
        var node = new com.regnosys.rosetta.ast.types.RBasicType();
        node.setName("string");
        assertSame(reg.lookup("string").orElseThrow(), reg.basicOrRecordNodeType(node).orElseThrow());
    }

    @Test void basicOrRecordNodeType_modelDeclaredBasicOrRecord_isNothing() {
        var basic = new com.regnosys.rosetta.ast.types.RBasicType();
        basic.setName("c15token");
        assertSame(RBasicType.NOTHING, reg.basicOrRecordNodeType(basic).orElseThrow());
        var record = new com.regnosys.rosetta.ast.types.RRecordType();
        record.setName("espan");
        assertSame(RBasicType.NOTHING, reg.basicOrRecordNodeType(record).orElseThrow());
    }

    @Test void basicOrRecordNodeType_otherNode_isEmpty() {
        assertTrue(reg.basicOrRecordNodeType(new com.regnosys.rosetta.ast.supporting.REnumValue()).isEmpty());
    }

    @Test void string_registered() {
        var t = reg.lookup("string").orElseThrow();
        assertInstanceOf(RStringType.class, t);
    }

    @Test void date_registered() {
        var t = reg.lookup("date").orElseThrow();
        assertSame(RRecordType.DATE, t);
    }

    @Test void dateTime_registered() {
        assertSame(RRecordType.DATE_TIME, reg.lookup("dateTime").orElseThrow());
    }

    @Test void zonedDateTime_registered() {
        assertSame(RRecordType.ZONED_DATE_TIME, reg.lookup("zonedDateTime").orElseThrow());
    }

    @Test void unknown_returns_empty() {
        assertTrue(reg.lookup("nonexistent").isEmpty());
    }

    @Test void allTypes_returns_all_registered() {
        assertTrue(reg.allTypes().size() >= 11);
    }

    @Test void meta_annotated_convenience() {
        var t = reg.lookupAnnotated("boolean").orElseThrow();
        assertEquals(RBasicType.BOOLEAN, t.type());
        assertFalse(t.hasMeta());
    }

    @Test void meta_annotated_missing_for_unknown() {
        assertTrue(reg.lookupAnnotated("nonexistent").isEmpty());
    }
}
