package demo.harness.exec.jmh;

import demo.harness.exec.corpus.ModelClosure;
import demo.harness.exec.workload.Workload;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

/**
 * JMH headline 2 - the validator sweep on this jar's runtime leg. One op runs every usable
 * XMeta registry's cardinality validator, type-format validator and data rules over
 * deterministically populated instances - the same unit the {@code run --workload validate}
 * verb times.
 *
 * <p>Requires {@code setup --leg <leg>} first. Run it as:
 * <pre>
 *   java -cp target/legacy-rt/demo-harness-exec-1.0.0-legacy-rt.jar org.openjdk.jmh.Main \
 *        demo.harness.exec.jmh.ValidationSweepBenchmark -prof gc
 * </pre>
 * The full sweep is large; {@code -p limit=500} takes a deterministic stratified subset when
 * a shorter trial is wanted, and the census then records both figures so the subset is never
 * mistaken for the whole.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 2)
@Fork(2)
// SINGLE-THREADED BY CONSTRUCTION: the @State(Scope.Benchmark) workload sweeps one shared
// list of populated instances, so -t N is pinned out rather than left to a flag.
@Threads(1)
@State(Scope.Benchmark)
public class ValidationSweepBenchmark {

    /** 0 = every XMeta registry in the closure (the default and the honest setting). */
    @Param({"0"})
    public int limit;

    /** Builder-population depth for the synthetic instances under validation. */
    @Param({"2"})
    public int depth;

    private ModelClosure closure;
    private Workload workload;

    @Setup(Level.Trial)
    public void setup() {
        closure = JmhSupport.openClosure();
        workload = JmhSupport.prepare(closure, "validate", limit, depth);
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        if (workload != null) {
            workload.close();
        }
        if (closure != null) {
            closure.close();
        }
    }

    @Benchmark
    public void validationSweep(Blackhole bh) throws Exception {
        bh.consume(workload.runOnce());
    }
}
