package demo.harness.exec;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Per-op stopwatch for the plain (non-JMH) {@code run} verb: records one elapsed time per
 * measured op and reports mean / p95 in milliseconds, per the CONTRACTS section 4 execution
 * metrics.
 *
 * <p>Allocation is measured with HotSpot's {@code com.sun.management.ThreadMXBean
 * .getCurrentThreadAllocatedBytes()}, reached REFLECTIVELY so the harness still compiles and
 * runs on a JVM that does not expose it. When it is unavailable {@link #allocBytesPerOp()}
 * returns -1 and the caller omits {@code allocMbPerOp} from the receipt entirely rather than
 * writing a zero - CONTRACTS section 2: never fabricate a field.
 *
 * <p>These are wall-clock loop timings, not JMH scores: they include JIT state that a single
 * process happens to be in. The JMH benchmarks in {@code demo.harness.exec.jmh} are the
 * statistically defensible half; {@code run} is the fast, streamable half the dashboard uses.
 */
public final class Timing {

    private static final Method ALLOC_METHOD = findAllocMethod();
    private static final Object THREAD_MX = ManagementFactory.getThreadMXBean();

    private final long[] elapsedNanos;
    private final long[] allocBytes;
    private int count;

    private long opStartNanos;
    private long opStartAlloc;

    public Timing(int ops) {
        this.elapsedNanos = new long[Math.max(ops, 1)];
        this.allocBytes = new long[Math.max(ops, 1)];
    }

    public void beginOp() {
        opStartAlloc = threadAllocatedBytes();
        opStartNanos = System.nanoTime();
    }

    public void endOp() {
        long elapsed = System.nanoTime() - opStartNanos;
        long alloc = threadAllocatedBytes();
        if (count < elapsedNanos.length) {
            elapsedNanos[count] = elapsed;
            allocBytes[count] = (alloc < 0 || opStartAlloc < 0) ? -1 : alloc - opStartAlloc;
            count++;
        }
    }

    public int ops() {
        return count;
    }

    public double meanMs() {
        if (count == 0) {
            return 0d;
        }
        long total = 0;
        for (int i = 0; i < count; i++) {
            total += elapsedNanos[i];
        }
        return round(total / (double) count / 1_000_000d);
    }

    /** Nearest-rank p95: the smallest sample at or above 95% of the ordered set. */
    public double p95Ms() {
        if (count == 0) {
            return 0d;
        }
        long[] sorted = Arrays.copyOf(elapsedNanos, count);
        Arrays.sort(sorted);
        int idx = (int) Math.ceil(0.95d * count) - 1;
        if (idx < 0) {
            idx = 0;
        }
        if (idx >= count) {
            idx = count - 1;
        }
        return round(sorted[idx] / 1_000_000d);
    }

    public double minMs() {
        if (count == 0) {
            return 0d;
        }
        long min = Long.MAX_VALUE;
        for (int i = 0; i < count; i++) {
            min = Math.min(min, elapsedNanos[i]);
        }
        return round(min / 1_000_000d);
    }

    public double maxMs() {
        if (count == 0) {
            return 0d;
        }
        long max = Long.MIN_VALUE;
        for (int i = 0; i < count; i++) {
            max = Math.max(max, elapsedNanos[i]);
        }
        return round(max / 1_000_000d);
    }

    /** Mean bytes allocated per op, or -1 when the JVM does not expose the counter. */
    public long allocBytesPerOp() {
        if (count == 0) {
            return -1L;
        }
        long total = 0;
        for (int i = 0; i < count; i++) {
            if (allocBytes[i] < 0) {
                return -1L;
            }
            total += allocBytes[i];
        }
        return total / count;
    }

    /** Mean MB allocated per op, or -1 when unavailable. */
    public double allocMbPerOp() {
        long bytes = allocBytesPerOp();
        return bytes < 0 ? -1d : round(bytes / (1024d * 1024d));
    }

    public static boolean allocAvailable() {
        return ALLOC_METHOD != null;
    }

    private static long threadAllocatedBytes() {
        if (ALLOC_METHOD == null) {
            return -1L;
        }
        try {
            Object v = ALLOC_METHOD.invoke(THREAD_MX);
            return v instanceof Long ? (Long) v : -1L;
        } catch (Throwable t) {
            return -1L;
        }
    }

    /**
     * Resolve the method on the EXPORTED interface {@code com.sun.management.ThreadMXBean},
     * never on the implementation class: the impl lives in the non-exported {@code
     * sun.management} package, so a lookup there would need setAccessible and fail under
     * JPMS. Probing once here means the run loop never pays for a failing call.
     */
    private static Method findAllocMethod() {
        try {
            Object mx = ManagementFactory.getThreadMXBean();
            Class<?> iface = Class.forName("com.sun.management.ThreadMXBean");
            if (!iface.isInstance(mx)) {
                return null;
            }
            Method m = iface.getMethod("getCurrentThreadAllocatedBytes");
            Object probe = m.invoke(mx);
            return probe instanceof Long && (Long) probe >= 0 ? m : null;
        } catch (Throwable t) {
            return null;
        }
    }

    static double round(double v) {
        return Math.round(v * 10000d) / 10000d;
    }
}
