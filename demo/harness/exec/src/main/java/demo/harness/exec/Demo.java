package demo.harness.exec;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The runner stdout protocol of CONTRACTS section 3: one JSON object per line, prefixed
 * exactly {@code "##DEMO## "}, for the site's SSE stream. Everything else printed is
 * ordinary log.
 *
 * <p>{@code progress} is throttled to at most one line per 250 units (plus a final line at
 * completion) as the contract requires. The emitter is deliberately dumb - it never
 * computes, only reports - so a metric can never be produced by the protocol layer.
 */
public final class Demo {

    public static final String PREFIX = "##DEMO## ";
    private static final int PROGRESS_EVERY = 250;

    private static final ObjectMapper JSON = new ObjectMapper();
    /**
     * Unit index of the last emitted progress line. It must be 0, NOT Long.MIN_VALUE: the
     * throttle below computes {@code n - lastProgressAt}, and a sentinel of Long.MIN_VALUE
     * overflows that subtraction to a large NEGATIVE value, which passes the "too soon"
     * test forever and suppresses every intermediate line.
     */
    private static long lastProgressAt = 0L;

    private Demo() {}

    public static void start(String id, String detail) {
        lastProgressAt = 0L;
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ev", "start");
        m.put("id", id);
        m.put("detail", detail);
        emit(m);
    }

    /** Throttled to one line per {@value #PROGRESS_EVERY} units; {@code n == of} always emits. */
    public static void progress(long n, long of) {
        if (n != of && n - lastProgressAt < PROGRESS_EVERY) {
            return;
        }
        lastProgressAt = n;
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ev", "progress");
        m.put("n", n);
        m.put("of", of);
        emit(m);
    }

    public static void metric(String key, Object value) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ev", "metric");
        m.put("key", key);
        m.put("value", value);
        emit(m);
    }

    public static void done(Map<String, Object> metrics) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ev", "done");
        m.put("metrics", metrics);
        emit(m);
    }

    public static void error(String message) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ev", "error");
        m.put("message", message);
        emit(m);
    }

    private static void emit(Map<String, Object> payload) {
        String line;
        try {
            line = JSON.writeValueAsString(payload);
        } catch (Exception e) {
            // last resort: never let the protocol layer throw out of a measured run
            line = "{\"ev\":\"error\",\"message\":\"protocol serialisation failed: "
                    + e.getClass().getSimpleName() + "\"}";
        }
        System.out.println(PREFIX + line);
        System.out.flush();
    }
}
