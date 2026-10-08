package com.regnosys.rosetta.ir.json;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonReaderTest {

    @Test
    void parsesObjectWithStringMember() {
        Json parsed = JsonReader.parse("{\n  \"kind\": \"ENUM\"\n}\n");
        assertEquals(new Json.Obj(List.of(new Json.Member("kind", Json.str("ENUM")))), parsed);
    }

    @Test
    void parsesNestedArraysBooleansNullAndNumbers() {
        String text = "{\n  \"irFormatVersion\": 1,\n  \"nodes\": [],\n"
                + "  \"flag\": true,\n  \"base\": null\n}\n";
        Json parsed = JsonReader.parse(text);
        Json.Obj obj = (Json.Obj) parsed;
        assertEquals(Json.number("1"), obj.members().get(0).value());
        assertEquals(new Json.Arr(List.of()), obj.members().get(1).value());
        assertEquals(Json.bool(true), obj.members().get(2).value());
        assertEquals(Json.NULL, obj.members().get(3).value());
    }

    @Test
    void roundTripsNamedEscapes() {
        // The reader is the inverse of JsonWriter.escape: a control char <0x20 is written as \\u00xx.
        String raw = "a\"b\\c\nd\tef";
        Json written = new Json.Str(raw);
        String text = JsonWriter.write(written);
        assertEquals(written, JsonReader.parse(text), "read(write(str)) must round-trip the raw value");
    }

    @Test
    void toleratesTrailingNewlineAndWhitespace() {
        assertEquals(new Json.Obj(List.of()), JsonReader.parse("{}\n"));
        assertEquals(new Json.Arr(List.of()), JsonReader.parse("  [] \t\n"));
    }

    @Test
    void unescapesUnicodeControlCharAndNonAscii() {
        // U+001F is <0x20 but NOT a named escape, so JsonWriter emits a unicode (\\u00xx) escape for it
        // — the only way to exercise the reader's unicode-escape decode in a fixed-point round-trip
        // (a printable char passes through literally). U+00E9 (e-acute) is a printable non-ASCII BMP
        // char that must round-trip verbatim. Built via (char) casts to keep zero backslashes here.
        String raw = "a" + (char) 0x1F + "b" + (char) 0xE9 + "c";
        Json written = new Json.Str(raw);
        String text = JsonWriter.write(written);
        assertTrue(text.contains("u001f"), "precondition: writer escapes U+001F as a unicode escape");
        assertEquals(written, JsonReader.parse(text), "read(write(str)) must round-trip the raw value");
    }

    @Test
    void rejectsTrailingGarbage() {
        IRJsonException ex = assertThrows(IRJsonException.class, () -> JsonReader.parse("{}x"));
        assertTrue(ex.getMessage().contains("trailing"), ex.getMessage());
    }

    @Test
    void rejectsNearNumberLiterals() {
        // RFC 8259 numbers only: the whole token must match, not just its first character.
        for (String bad : List.of("1x", "-x", "1.2.3", "1e", "1e+", "01", "-", ".5", "1.", "--1", "1..2")) {
            IRJsonException ex = assertThrows(IRJsonException.class,
                    () -> JsonReader.parse("{\"n\": " + bad + "}"),
                    "should reject invalid literal: " + bad);
            assertTrue(ex.getMessage().contains("invalid literal") || ex.getMessage().contains("expected"),
                    bad + " -> " + ex.getMessage());
        }
    }

    @Test
    void acceptsStrictNumberForms() {
        for (String ok : List.of("0", "-0", "10", "1.5", "-0.25", "1e10", "1E+5", "2.5e-3", "0.0")) {
            Json parsed = JsonReader.parse("{\"n\": " + ok + "}");
            assertEquals(Json.number(ok), ((Json.Obj) parsed).members().get(0).value(), ok);
        }
    }

    @Test
    void rejectsUnterminatedStringAndInvalidEscape() {
        assertThrows(IRJsonException.class, () -> JsonReader.parse("\"abc"));
        assertThrows(IRJsonException.class, () -> JsonReader.parse("\"a\\xb\""));
    }
}
