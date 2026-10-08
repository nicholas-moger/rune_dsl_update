package demo.harness.exec.jmh;

import demo.harness.exec.LegPins;
import demo.harness.exec.corpus.ModelClosure;
import demo.harness.exec.workload.Workload;
import demo.harness.exec.workload.Workloads;

import java.util.Map;

/**
 * Shared trial setup for the two JMH benchmarks.
 *
 * <h2>How the leg is chosen</h2>
 * The leg is a CLASSPATH property, not a parameter: each shaded jar contains exactly one
 * runtime, so the jar you launch IS the leg. {@code -Dexec.leg=<leg>} is therefore optional
 * and acts as an ASSERTION - if it is present and disagrees with the jar's own pins, the
 * trial fails rather than producing a mislabelled score. Defaulting to the jar's leg also
 * side-steps the usual forked-JVM property-propagation trap: a {@code @Fork(2)} child that
 * did not inherit {@code -Dexec.leg} still measures the right thing.
 *
 * <h2>Why setup never compiles</h2>
 * {@link ModelClosure#open} requires a completed {@code setup --leg <leg>}. Compiling inside
 * {@code @Setup(Level.Trial)} would put a multi-minute javac inside each fork's trial and
 * charge part of it to the first iteration. Run setup once, then benchmark.
 */
public final class JmhSupport {

    public static final String LEG_PROPERTY = "exec.leg";

    private JmhSupport() {}

    /** The jar's own leg, cross-checked against {@code -Dexec.leg} when that is supplied. */
    public static String leg() {
        String requested = System.getProperty(LEG_PROPERTY);
        if (requested == null || requested.isBlank()) {
            return LegPins.LEG;
        }
        LegPins.requireLeg(requested.trim());
        return requested.trim();
    }

    /** Open the pre-compiled closure for this jar's leg. */
    public static ModelClosure openClosure() {
        String leg = leg();
        ModelClosure closure = ModelClosure.open(leg);
        System.out.println("[jmh] leg=" + leg + " closure=" + closure.describe());
        System.out.println("[jmh] " + LegPins.notes());
        return closure;
    }

    /**
     * Build and set up a workload, printing its census. The census lines are part of the JMH
     * receipt: a score whose denominator is unknown is not a measurement.
     */
    public static Workload prepare(ModelClosure closure, String workload, int limit, int depth) {
        Workload w = Workloads.create(workload, closure, limit, depth);
        try {
            Map<String, Object> census = w.setUp();
            System.out.println("[jmh] " + workload + " census " + census);
        } catch (Exception e) {
            throw new IllegalStateException("workload setup failed for " + workload, e);
        }
        System.out.println("[jmh] " + workload + " unitsPerOp=" + w.unitsPerOp()
                + " (" + w.unitLabel() + ")");
        return w;
    }
}
