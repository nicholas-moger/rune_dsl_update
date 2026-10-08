package demo.harness.analytics;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The demo contract section 3 runner stdout protocol, plus a dependency-free JSON writer.
 *
 * <p>Every machine-readable line is exactly {@code "##DEMO## "} followed by one JSON object
 * on one line. Everything else printed by this process is ordinary log text. Each protocol
 * line is flushed immediately so the demo server can stream it as SSE.
 *
 * <p>All emitted strings are ASCII: {@link #escape} renders every character outside
 * {@code 0x20..0x7E} as a {@code \\uXXXX} escape, which keeps the output readable on a
 * Windows console under any code page (demo-contract section 0 rule 8).
 *
 * <p>This class is deliberately duplicated (with only the package line changed) into each
 * demo harness module rather than shared through a fourth artifact: the fork and upstream
 * language jars cannot share a classpath, so a common module would have to be dependency-free
 * anyway and would add a build-ordering constraint the integrator does not need.
 */
public final class DemoOut {

    private DemoOut() {
    }

    private static final PrintStream OUT = System.out;
    private static final String PREFIX = "##DEMO## ";

    // -- protocol events ------------------------------------------------------

    /** {@code {"ev":"start","id":...,"detail":...}} */
    public static void start(String id, String detail) {
        emit(obj("ev", "start", "id", id, "detail", detail));
    }

    /** {@code {"ev":"progress","n":N,"of":M}} — callers must throttle to one per 250 units. */
    public static void progress(long n, long of) {
        emit(obj("ev", "progress", "n", n, "of", of));
    }

    /** {@code {"ev":"metric","key":...,"value":...}} */
    public static void metric(String key, Object value) {
        emit(obj("ev", "metric", "key", key, "value", value));
    }

    /** {@code {"ev":"done","metrics":{...}}} */
    public static void done(Map<String, Object> metrics) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ev", "done");
        m.put("metrics", metrics);
        emit(m);
    }

    /** {@code {"ev":"error","message":...}} */
    public static void error(String message) {
        emit(obj("ev", "error", "message", message));
    }

    /** An ordinary (non-protocol) log line. */
    public static void log(String message) {
        OUT.println(message);
        OUT.flush();
    }

    private static void emit(Map<String, Object> event) {
        OUT.print(PREFIX);
        OUT.print(json(event));
        OUT.print('\n');
        OUT.flush();
    }

    private static Map<String, Object> obj(Object... keyValuePairs) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keyValuePairs.length; i += 2) {
            m.put(String.valueOf(keyValuePairs[i]), keyValuePairs[i + 1]);
        }
        return m;
    }

    // -- JSON -----------------------------------------------------------------

    /**
     * Renders a value as compact single-line JSON. Supported: {@code null}, {@link String},
     * {@link Number}, {@link Boolean}, {@link Map} (keys stringified, insertion order kept)
     * and {@link Iterable}. Anything else is rendered as its {@code toString()} in quotes,
     * which is a visible fallback rather than a silent drop.
     */
    public static String json(Object value) {
        StringBuilder sb = new StringBuilder();
        write(value, sb);
        return sb.toString();
    }

    /**
     * Renders a value as indented multi-line JSON, for files a human will read.
     * Same supported types as {@link #json(Object)}.
     */
    public static String jsonPretty(Object value) {
        StringBuilder sb = new StringBuilder();
        writePretty(value, 0, sb);
        sb.append('\n');
        return sb.toString();
    }

    private static void write(Object value, StringBuilder sb) {
        if (value == null) {
            sb.append("null");
        } else if (value instanceof String s) {
            escape(s, sb);
        } else if (value instanceof Boolean b) {
            sb.append(b.booleanValue() ? "true" : "false");
        } else if (value instanceof Number n) {
            sb.append(number(n));
        } else if (value instanceof Map<?, ?> map) {
            sb.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> e : map.entrySet()) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                escape(String.valueOf(e.getKey()), sb);
                sb.append(':');
                write(e.getValue(), sb);
            }
            sb.append('}');
        } else if (value instanceof Iterable<?> it) {
            sb.append('[');
            boolean first = true;
            for (Object o : it) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                write(o, sb);
            }
            sb.append(']');
        } else {
            escape(String.valueOf(value), sb);
        }
    }

    private static void writePretty(Object value, int depth, StringBuilder sb) {
        if (value instanceof Map<?, ?> map) {
            if (map.isEmpty()) {
                sb.append("{}");
                return;
            }
            sb.append("{\n");
            int i = 0;
            for (Map.Entry<?, ?> e : map.entrySet()) {
                indent(depth + 1, sb);
                escape(String.valueOf(e.getKey()), sb);
                sb.append(": ");
                writePretty(e.getValue(), depth + 1, sb);
                if (++i < map.size()) {
                    sb.append(',');
                }
                sb.append('\n');
            }
            indent(depth, sb);
            sb.append('}');
            return;
        }
        if (value instanceof Iterable<?> it) {
            List<Object> items = new ArrayList<>();
            for (Object o : it) {
                items.add(o);
            }
            if (items.isEmpty()) {
                sb.append("[]");
                return;
            }
            sb.append("[\n");
            for (int i = 0; i < items.size(); i++) {
                indent(depth + 1, sb);
                writePretty(items.get(i), depth + 1, sb);
                if (i + 1 < items.size()) {
                    sb.append(',');
                }
                sb.append('\n');
            }
            indent(depth, sb);
            sb.append(']');
            return;
        }
        write(value, sb);
    }

    private static void indent(int depth, StringBuilder sb) {
        sb.append("  ".repeat(Math.max(0, depth)));
    }

    private static String number(Number n) {
        // Non-finite doubles have no JSON representation; emit null rather than
        // producing a document no parser will accept.
        if (n instanceof Double d && (d.isNaN() || d.isInfinite())) {
            return "null";
        }
        if (n instanceof Float f && (f.isNaN() || f.isInfinite())) {
            return "null";
        }
        return n.toString();
    }

    /** Quotes and escapes a string to strictly-ASCII JSON. */
    public static String escape(String s) {
        StringBuilder sb = new StringBuilder();
        escape(s, sb);
        return sb.toString();
    }

    private static void escape(String s, StringBuilder sb) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                default -> {
                    if (c < 0x20 || c > 0x7E) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
    }
}
