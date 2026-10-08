package com.regnosys.rosetta.ir.json;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses a JSON document into the {@link Json} value tree — the exact inverse of
 * {@link JsonWriter}. Hand-rolled recursive descent, zero third-party dependency.
 *
 * <p><b>Bounded-depth assumption.</b> This reader is recursive descent and assumes
 * serializer-canonical input of bounded nesting depth. A pathologically deep document can exhaust
 * the Java call stack and throw a {@link StackOverflowError} — an {@code Error}, not an
 * {@link IRJsonException}. A depth guard is deferred until this API is pointed at untrusted
 * external JSON outside the serializer's own output.
 *
 * <p>Scalars (numbers, {@code true}/{@code false}/{@code null}) are captured verbatim as
 * {@link Json.Raw} tokens <em>without interpretation</em> — their meaning is decoded later by
 * the consumer ({@link IRJsonDeserializer}), which keeps the reader minimal and total over any
 * scalar. Strings are unescaped (the inverse of {@link JsonWriter}'s escaping). Insignificant
 * whitespace — including the single trailing newline {@code JsonWriter} always appends — is
 * skipped; trailing non-whitespace is rejected.
 *
 * <p>Example:
 * <pre>{@code
 * JsonReader.parse("{\n  \"kind\": \"ENUM\"\n}\n")
 *   == new Json.Obj(List.of(new Json.Member("kind", Json.str("ENUM"))))
 * }</pre>
 */
final class JsonReader {

    private final String s;
    private int pos;

    private JsonReader(String s) {
        this.s = s;
    }

    /** Parses {@code text} as a single JSON document. Throws {@link IRJsonException} on malformed input. */
    static Json parse(String text) {
        JsonReader r = new JsonReader(text);
        Json value = r.readValue();
        r.skipWs();
        if (r.pos != r.s.length()) {
            throw r.error("trailing content after JSON value");
        }
        return value;
    }

    private Json readValue() {
        skipWs();
        char c = peek();
        return switch (c) {
            case '{' -> readObject();
            case '[' -> readArray();
            case '"' -> new Json.Str(readString());
            default -> readLiteral();
        };
    }

    private Json readObject() {
        expect('{');
        List<Json.Member> members = new ArrayList<>();
        skipWs();
        if (peek() == '}') {
            pos++;
            return new Json.Obj(members);
        }
        while (true) {
            skipWs();
            if (peek() != '"') {
                throw error("expected a member name");
            }
            String name = readString();
            skipWs();
            expect(':');
            members.add(new Json.Member(name, readValue()));
            skipWs();
            char c = next();
            if (c == ',') {
                continue;
            }
            if (c == '}') {
                break;
            }
            throw error("expected ',' or '}'");
        }
        return new Json.Obj(members);
    }

    private Json readArray() {
        expect('[');
        List<Json> elements = new ArrayList<>();
        skipWs();
        if (peek() == ']') {
            pos++;
            return new Json.Arr(elements);
        }
        while (true) {
            elements.add(readValue());
            skipWs();
            char c = next();
            if (c == ',') {
                continue;
            }
            if (c == ']') {
                break;
            }
            throw error("expected ',' or ']'");
        }
        return new Json.Arr(elements);
    }

    private String readString() {
        expect('"');
        StringBuilder sb = new StringBuilder();
        while (true) {
            if (pos >= s.length()) {
                throw error("unterminated string");
            }
            char c = s.charAt(pos++);
            if (c == '"') {
                break;
            }
            if (c == '\\') {
                sb.append(readEscape());
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private char readEscape() {
        if (pos >= s.length()) {
            throw error("unterminated escape");
        }
        char e = s.charAt(pos++);
        return switch (e) {
            case '"' -> '"';
            case '\\' -> '\\';
            case '/' -> '/';
            case 'b' -> '\b';
            case 'f' -> '\f';
            case 'n' -> '\n';
            case 'r' -> '\r';
            case 't' -> '\t';
            case 'u' -> readUnicodeEscape();
            default -> throw error("invalid escape '\\" + e + "'");
        };
    }

    private char readUnicodeEscape() {
        if (pos + 4 > s.length()) {
            throw error("incomplete \\u escape");
        }
        String hex = s.substring(pos, pos + 4);
        pos += 4;
        try {
            return (char) Integer.parseInt(hex, 16);
        } catch (NumberFormatException ex) {
            throw error("invalid \\u escape '" + hex + "'");
        }
    }

    // A bare literal: number, true, false, or null — captured verbatim as Json.Raw and NOT
    // interpreted here (the consumer decodes the value per-field). Read up to the next structural
    // delimiter or whitespace. Strict numeric-grammar validation is deferred to the field decode.
    private Json readLiteral() {
        int start = pos;
        while (pos < s.length() && !isDelimiter(s.charAt(pos))) {
            pos++;
        }
        String token = s.substring(start, pos);
        if (token.isEmpty()) {
            throw error("expected a value");
        }
        if (!isLiteral(token)) {
            throw error("invalid literal '" + token + "'");
        }
        return new Json.Raw(token);
    }

    private static boolean isLiteral(String t) {
        if (t.equals("true") || t.equals("false") || t.equals("null")) {
            return true;
        }
        return isNumberLiteral(t);
    }

    // Strict RFC 8259 number grammar: -?(0|[1-9][0-9]*)(.[0-9]+)?([eE][+-]?[0-9]+)?
    // — the whole token must match, so near-numbers like "1x", "01" or "1.2.3"
    // are rejected here rather than surfacing later when a Raw token is decoded.
    private static boolean isNumberLiteral(String t) {
        int i = 0;
        int n = t.length();
        if (i < n && t.charAt(i) == '-') {
            i++;
        }
        if (i >= n) {
            return false;
        }
        char c = t.charAt(i);
        if (c == '0') {
            i++;
        } else if (c >= '1' && c <= '9') {
            do {
                i++;
            } while (i < n && isDigit(t.charAt(i)));
        } else {
            return false;
        }
        if (i < n && t.charAt(i) == '.') {
            i++;
            if (i >= n || !isDigit(t.charAt(i))) {
                return false;
            }
            do {
                i++;
            } while (i < n && isDigit(t.charAt(i)));
        }
        if (i < n && (t.charAt(i) == 'e' || t.charAt(i) == 'E')) {
            i++;
            if (i < n && (t.charAt(i) == '+' || t.charAt(i) == '-')) {
                i++;
            }
            if (i >= n || !isDigit(t.charAt(i))) {
                return false;
            }
            do {
                i++;
            } while (i < n && isDigit(t.charAt(i)));
        }
        return i == n;
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isDelimiter(char c) {
        return c == ',' || c == '}' || c == ']' || isWs(c);
    }

    private void skipWs() {
        while (pos < s.length() && isWs(s.charAt(pos))) {
            pos++;
        }
    }

    private static boolean isWs(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r';
    }

    private char peek() {
        if (pos >= s.length()) {
            throw error("unexpected end of input");
        }
        return s.charAt(pos);
    }

    private char next() {
        if (pos >= s.length()) {
            throw error("unexpected end of input");
        }
        return s.charAt(pos++);
    }

    private void expect(char c) {
        if (pos >= s.length() || s.charAt(pos) != c) {
            throw error("expected '" + c + "'");
        }
        pos++;
    }

    private IRJsonException error(String message) {
        return new IRJsonException(message + " at position " + pos);
    }
}
