package demo.harness.exec.workload;

import demo.harness.exec.corpus.ModelClosure;
import demo.harness.exec.corpus.ReflectivePopulator;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Workloads 4 and 5 - {@code report} and {@code projection}: evaluate every generated
 * function that survives a probe, once per op. Adapted from
 * {@code rune-benchmarks/.../DrrRuleEvaluationBenchmark.java}, whose discipline is copied
 * exactly: the DEFAULT Guice injector (so {@code DefaultConditionValidator} enforces
 * conditions and throws, and {@code NoOpModelObjectValidator} is the object validator), a
 * deterministic {@code evaluate} overload pick, builder-populated inputs, a one-shot probe
 * evaluation, and a PRINTED SURVIVOR CENSUS that is part of the receipt.
 *
 * <p>The two instances differ only in which classes they consider:
 * <ul>
 *   <li>{@code report} - every {@code *ReportFunction} in the closure. These are the DRR
 *       regulatory report entry points: one call runs the whole rule tree beneath it, so
 *       this is the heaviest and most representative workload in the demo.</li>
 *   <li>{@code projection} - every function under a {@code drr.projection..functions}
 *       package: the ISO 20022 / DTCC projection layer that turns a DRR report into the
 *       submission message. Unlike report functions, many of these take ZERO arguments
 *       ({@code Create_TradeReportHeader}), which the argument builder handles as a valid
 *       call rather than a failure.</li>
 * </ul>
 *
 * <p>Probe failures are EXPECTED and counted, not hidden: synthetic inputs cannot satisfy
 * every condition in a regulatory rule tree, so a function whose conditions throw drops out
 * of the measured set. What must never happen is the two legs surviving DIFFERENT sets - so
 * the survivor count and every drop-out reason are printed, and the integrator should compare
 * them across legs before comparing any timing.
 */
public final class FunctionWorkload implements Workload {

    private final String name;
    private final ModelClosure closure;
    private final Predicate<String> classFilter;
    private final String subject;
    private final int limit;
    private final int depth;

    private List<Runner> runners;

    record Runner(String name, Object fn, Method evaluate, Object[] args) {}

    private FunctionWorkload(String name, ModelClosure closure, Predicate<String> classFilter,
                             String subject, int limit, int depth) {
        this.name = name;
        this.closure = closure;
        this.classFilter = classFilter;
        this.subject = subject;
        this.limit = limit;
        this.depth = depth;
    }

    /** Every {@code *ReportFunction} in the closure. */
    public static FunctionWorkload reports(ModelClosure closure, int limit, int depth) {
        return new FunctionWorkload("report", closure,
                n -> n.endsWith("ReportFunction"), "report functions", limit, depth);
    }

    /** Every function under a {@code drr.projection..functions} package. */
    public static FunctionWorkload projections(ModelClosure closure, int limit, int depth) {
        return new FunctionWorkload("projection", closure,
                n -> n.startsWith("drr.projection.") && n.contains(".functions."),
                "projection functions", limit, depth);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Map<String, Object> setUp() {
        List<String> allNames = closure.classNames(classFilter);
        List<String> names = Workloads.stratify(allNames, limit);
        ReflectivePopulator populator = new ReflectivePopulator();
        runners = new ArrayList<>();
        int loadFailed = 0;
        int injectFailed = 0;
        int noEvaluate = 0;
        int inputFailed = 0;
        int probeFailed = 0;
        int zeroArg = 0;

        for (String className : names) {
            Class<?> cls;
            try {
                cls = closure.load(className);
            } catch (Throwable t) {
                loadFailed++;
                continue;
            }
            Object fn;
            try {
                fn = closure.injector().getInstance(cls);
            } catch (Throwable t) {
                injectFailed++;
                continue;
            }
            Method evaluate = Workloads.evaluateMethod(cls);
            if (evaluate == null) {
                noEvaluate++;
                continue;
            }
            Object[] args = Workloads.evaluateArgs(evaluate, populator, depth);
            if (args == null) {
                inputFailed++;
                continue;
            }
            if (args.length == 0) {
                zeroArg++;
            }
            try {
                evaluate.invoke(fn, args);
            } catch (Throwable t) {
                probeFailed++;
                continue;
            }
            runners.add(new Runner(className, fn, evaluate, args));
        }

        Map<String, Object> census = new LinkedHashMap<>();
        census.put("subject", subject);
        census.put("candidatesFound", allNames.size());
        census.put("candidatesConsidered", names.size());
        census.put("survivors", runners.size());
        census.put("loadFailed", loadFailed);
        census.put("injectFailed", injectFailed);
        census.put("noEvaluate", noEvaluate);
        census.put("inputFailed", inputFailed);
        census.put("probeFailed", probeFailed);
        census.put("zeroArgSurvivorsAndProbes", zeroArg);
        // ...AllLevels: the populator recurses into nested model objects with the same
        // instance, so these span every depth and do not reconcile against the columns above
        census.put("populatorBuiltAllLevels", populator.populatedCount());
        census.put("populatorFailedAllLevels", populator.failedCount());
        census.put("populatorSetterRejectionsAllLevels", populator.setterRejectionCount());
        System.out.printf("[%s census] subject=%s candidates=%d considered=%d survivors=%d "
                        + "loadFailed=%d injectFailed=%d noEvaluate=%d inputFailed=%d "
                        + "probeFailed=%d zeroArg=%d%n",
                name, subject.replace(' ', '-'), allNames.size(), names.size(), runners.size(),
                loadFailed, injectFailed, noEvaluate, inputFailed, probeFailed, zeroArg);
        if (runners.isEmpty()) {
            throw new IllegalStateException("zero probe-surviving " + subject + " - census above");
        }
        return census;
    }

    @Override
    public int unitsPerOp() {
        return runners == null ? 0 : runners.size();
    }

    @Override
    public String unitLabel() {
        return subject + " evaluated";
    }

    @Override
    public long runOnce() throws Exception {
        long checksum = 0L;
        for (Runner r : runners) {
            Object out = r.evaluate().invoke(r.fn(), r.args());
            checksum += System.identityHashCode(out);
        }
        return checksum;
    }

    /** Survivor names, so {@code live} can show which functions actually ran. */
    public List<String> survivorNames() {
        List<String> out = new ArrayList<>();
        if (runners != null) {
            for (Runner r : runners) {
                out.add(r.name());
            }
        }
        return out;
    }
}
