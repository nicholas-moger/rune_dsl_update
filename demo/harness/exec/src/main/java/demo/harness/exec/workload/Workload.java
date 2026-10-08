package demo.harness.exec.workload;

import java.util.Map;

/**
 * One measurable unit of execution work over the compiled generated closure.
 *
 * <p>The shape is what keeps the measurement honest:
 * <ul>
 *   <li>{@link #setUp()} does everything that is NOT being measured - class loading, Guice
 *       instantiation, building synthetic inputs, probing which members actually work - and
 *       returns the CENSUS: how many candidates were found and, individually, why each one
 *       that dropped out dropped out. A census that only reported survivors would let the
 *       denominator shrink silently between legs.</li>
 *   <li>{@link #unitsPerOp()} states what one op actually contains, so "meanMs" is never
 *       read without knowing what it is the mean of.</li>
 *   <li>{@link #runOnce()} returns a checksum the caller consumes. The plain {@code run}
 *       loop has no Blackhole, so without a consumed result the JIT is free to delete the
 *       work being timed.</li>
 * </ul>
 */
public interface Workload extends AutoCloseable {

    /** Workload id as it appears in {@code --workload} and in the receipt metrics. */
    String name();

    /** Non-measured preparation; returns the census (recorded in the receipt). */
    Map<String, Object> setUp() throws Exception;

    /** How many units one op processes - the honest denominator. */
    int unitsPerOp();

    /** What a unit IS, e.g. "model types populated". */
    String unitLabel();

    /** One measured op. The returned checksum must depend on the work done. */
    long runOnce() throws Exception;

    @Override
    default void close() {
        // most workloads hold nothing that needs releasing; the closure owns the loader
    }
}
