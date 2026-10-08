package com.regnosys.rosetta.ir.json;

import java.util.List;

/**
 * An immutable JSON value tree. Building a tree then rendering it (via {@link JsonWriter})
 * keeps "what to emit" (the serializer) separate from "how to format" (the writer) and makes
 * both independently testable.
 *
 * <p>{@link Str} carries a raw Java string (escaped at write time); {@link Raw} carries a
 * verbatim JSON token (a number, {@code true}/{@code false}, or {@code null}) the caller has
 * already validated. Use the factories ({@link #str}, {@link #bool}, {@link #number},
 * {@link #NULL}) rather than {@code Raw} directly for the common cases.
 *
 * <p>Example — building a two-member object:
 * <pre>{@code
 * Json obj = new Json.Obj(List.of(
 *     new Json.Member("kind", Json.str("ENUM")),
 *     new Json.Member("name", Json.str("MyEnum"))));
 * // JsonWriter.write(obj) → "{\n  \"kind\": \"ENUM\",\n  \"name\": \"MyEnum\"\n}\n"
 * }</pre>
 */
public sealed interface Json permits Json.Obj, Json.Arr, Json.Str, Json.Raw {

    /** An object: ordered members (key order is the caller's deliberate, fixed choice). */
    record Obj(List<Member> members) implements Json {
        public Obj { members = List.copyOf(members); }
    }

    /** One ordered object member. */
    record Member(String name, Json value) {}

    /** An array: ordered elements (source order preserved by the caller). */
    record Arr(List<Json> elements) implements Json {
        public Arr { elements = List.copyOf(elements); }
    }

    /** A JSON string; {@code value} is the raw Java string, escaped at write time. */
    record Str(String value) implements Json {}

    /** A verbatim JSON token (number / {@code true} / {@code false} / {@code null}). */
    record Raw(String token) implements Json {}

    /** A JSON string node. */
    static Json str(String value) { return new Str(value); }
    /** A JSON boolean token. */
    static Json bool(boolean b) { return new Raw(Boolean.toString(b)); }
    /** A JSON number token from an already-formatted decimal/integer string (never a host double). */
    static Json number(String token) { return new Raw(token); }
    /** The JSON null token. */
    Json NULL = new Raw("null");
}
