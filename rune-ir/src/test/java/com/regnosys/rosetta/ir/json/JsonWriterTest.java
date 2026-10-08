package com.regnosys.rosetta.ir.json;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonWriterTest {

    @Test
    void escapesJsonStringSpecials() {
        // chars: a " b \ c <newline> <tab> <CR> <backspace> <formfeed> <unit-sep U+001F> d
        Json v = Json.str("a\"b\\c\n\t\r\b\f\u001fd");
        assertEquals("\"a\\\"b\\\\c\\n\\t\\r\\b\\f\\u001fd\"\n", JsonWriter.write(v));
    }

    @Test
    void writesObjectInKeyOrderWithTwoSpaceIndent() {
        Json v = new Json.Obj(List.of(
                new Json.Member("kind", Json.str("STRUCT")),
                new Json.Member("isAbstract", Json.bool(false)),
                new Json.Member("baseType", Json.NULL),
                new Json.Member("fields", new Json.Arr(List.of()))));
        assertEquals("""
                {
                  "kind": "STRUCT",
                  "isAbstract": false,
                  "baseType": null,
                  "fields": []
                }
                """, JsonWriter.write(v));
    }

    @Test
    void writesNestedArrayMultilineAndNumberAsRawToken() {
        Json v = new Json.Arr(List.of(
                new Json.Obj(List.of(new Json.Member("value", Json.number("0.050")))),
                new Json.Obj(List.of(new Json.Member("value", Json.number("42"))))));
        assertEquals("""
                [
                  {
                    "value": 0.050
                  },
                  {
                    "value": 42
                  }
                ]
                """, JsonWriter.write(v));
    }
}
