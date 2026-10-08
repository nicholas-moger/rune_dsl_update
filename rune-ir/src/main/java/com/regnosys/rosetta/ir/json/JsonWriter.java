package com.regnosys.rosetta.ir.json;

/**
 * Renders a {@link Json} tree to a deterministic, 2-space pretty-printed document ending in a
 * single newline. Empty objects/arrays render inline ({@code {}} / {@code []}); non-empty ones
 * place each member/element on its own indented line. Strings are JSON-escaped
 * (RFC 8259: {@code " \ \b \f \n \r \t} and {@code \\u00XX} for other control chars below 0x20).
 *
 * <p>Example: {@code JsonWriter.write(new Json.Obj(List.of(new Json.Member("kind", Json.str("ENUM")))))}
 * → {@code "{\n  \"kind\": \"ENUM\"\n}\n"}.
 */
final class JsonWriter {

    private static final String INDENT = "  ";

    private JsonWriter() {}

    static String write(Json value) {
        StringBuilder sb = new StringBuilder();
        writeValue(value, 0, sb);
        sb.append('\n');
        return sb.toString();
    }

    private static void writeValue(Json value, int depth, StringBuilder sb) {
        switch (value) {
            case Json.Str s -> { sb.append('"'); escape(s.value(), sb); sb.append('"'); }
            case Json.Raw r -> sb.append(r.token());
            case Json.Arr a -> writeArray(a, depth, sb);
            case Json.Obj o -> writeObject(o, depth, sb);
        }
    }

    private static void writeObject(Json.Obj o, int depth, StringBuilder sb) {
        if (o.members().isEmpty()) { sb.append("{}"); return; }
        sb.append("{\n");
        String itemIndent = INDENT.repeat(depth + 1);
        int last = o.members().size() - 1;
        for (int i = 0; i <= last; i++) {
            Json.Member m = o.members().get(i);
            sb.append(itemIndent).append('"');
            escape(m.name(), sb);
            sb.append("\": ");
            writeValue(m.value(), depth + 1, sb);
            sb.append(i < last ? ",\n" : "\n");
        }
        sb.append(INDENT.repeat(depth)).append('}');
    }

    private static void writeArray(Json.Arr a, int depth, StringBuilder sb) {
        if (a.elements().isEmpty()) { sb.append("[]"); return; }
        sb.append("[\n");
        String itemIndent = INDENT.repeat(depth + 1);
        int last = a.elements().size() - 1;
        for (int i = 0; i <= last; i++) {
            sb.append(itemIndent);
            writeValue(a.elements().get(i), depth + 1, sb);
            sb.append(i < last ? ",\n" : "\n");
        }
        sb.append(INDENT.repeat(depth)).append(']');
    }

    // note: supplementary characters (code points > U+FFFF) are passed through char-by-char via
    // surrogate pairs; they are not combined. ASCII/BMP data is the intended scope.
    private static void escape(String s, StringBuilder sb) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"'  -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default   -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
    }
}
