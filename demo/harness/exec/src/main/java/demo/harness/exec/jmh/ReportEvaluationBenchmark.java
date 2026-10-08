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
 * JMH headline 1 - DRR report evaluation on this jar's runtime leg. One op evaluates every
 * probe-surviving {@code *ReportFunction} in the compiled closure once, the same unit the
 * {@code run --workload report} verb times; JMH's forks, warmup schedule and error bars are
 * what this class adds.
 *
 * <p>Requires {@code setup --leg <leg>} first. Run it as:
 * <pre>
 *   java -cp target/plus-rt/demo-harness-exec-1.0.0-plus-rt.jar org.openjdk.jmh.Main \
 *        demo.harness.exec.jmh.ReportEvaluationBenchmark -prof gc
 * </pre>
 * and again with the legacy-rt jar. Compare the two scores only after checking that the
 * printed survivor censuses match - different survivor sets are different work.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 2)
@Fork(2)
// SINGLE-THREADED BY CONSTRUCTION. The @State(Scope.Benchmark) workload re-invokes each
// generated function with the SAME populated argument instances; sharing those across
// benchmark threads would be a data race, so -t N is pinned out rather than left to a flag.
@Threads(1)
@State(Scope.Benchmark)
public class ReportEvaluationBenchmark {

    /** 0 = every report function in the closure (the default and the honest setting). */
    @Param({"0"})
    public int limit;

    /** Builder-population depth for the synthetic report inputs. */
    @Param({"2"})
    public int depth;

    private ModelClosure closure;
    private Workload workload;

    @Setup(Level.Trial)
    public void setup() {
        closure = JmhSupport.openClosure();
        workload = JmhSupport.prepare(closure, "report", limit, depth);
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        if (workload != null) {
            workload.close();
        }
        if (closure != null) {
            closure.close(); // Windows: release the exec-classes file locks
        }
    }

    @Benchmark
    public void reportEvaluation(Blackhole bh) throws Exception {
        bh.consume(workload.runOnce());
    }
}
