package demo.harness.exec.workload;

import demo.harness.exec.corpus.ModelClosure;
import demo.harness.exec.corpus.ReflectivePopulator;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Workload 1 - {@code populate}: build one instance of each of a stratified sample of the
 * closure's generated model types, through their generated builders, per op.
 *
 * <p>What it actually measures: the generated builder surface itself - {@code builder()},
 * every {@code set*}/{@code add*} the value table can feed, and {@code build()}'s copy into
 * the immutable implementation. This is the cheapest workload and the one most sensitive to
 * the runtime's object-construction path, so it is the natural first bar in the demo's
 * create -> serialise -> validate -> report -> projection story.
 *
 * <p>Reflection cost is INSIDE the measurement, identically on both legs. That is a real
 * caveat and the receipt says so: the number compares two runtimes doing the same reflective
 * construction, not the cost of hand-written construction code.
 */
public final class PopulateWorkload implements Workload {

    /** Default sample size when {@code --limit} is not given. */
    public static final int DEFAULT_LIMIT = 200;

    private final ModelClosure closure;
    private final int limit;
    private final int depth;

    private List<Class<?>> types;

    public PopulateWorkload(ModelClosure closure, int limit, int depth) {
        this.closure = closure;
        this.limit = limit <= 0 ? DEFAULT_LIMIT : limit;
        this.depth = depth;
    }

    @Override
    public String name() {
        return "populate";
    }

    @Override
    public Map<String, Object> setUp() {
        types = Workloads.selectModelTypes(closure, limit);
        if (types.isEmpty()) {
            throw new IllegalStateException("no generated model types found in "
                    + closure.classesDir() + " - census above");
        }
        // A dedicated probe populator so the census reports ONE pass, not an accumulating
        // count across every measured op.
        ReflectivePopulator probe = new ReflectivePopulator();
        int built = Workloads.buildInstances(types, depth, probe).size();

        // probeBuiltTopLevel reconciles against selectedTypes; the ...AllLevels counters do
        // NOT - the populator recurses into nested model objects with the same instance, so
        // its own counters span every depth. Naming them apart stops the two being read as
        // one split.
        Map<String, Object> census = new LinkedHashMap<>();
        census.put("compiledClasses", closure.allClassNames().size());
        census.put("requestedTypes", limit);
        census.put("selectedTypes", types.size());
        census.put("probeBuiltTopLevel", built);
        census.put("probeNotBuiltTopLevel", types.size() - built);
        census.put("probeFailedAllLevels", probe.failedCount());
        census.put("probeSetterRejectionsAllLevels", probe.setterRejectionCount());
        census.put("populateDepth", depth);
        System.out.printf("[populate census] compiledClasses=%d requested=%d selected=%d "
                        + "builtTopLevel=%d notBuiltTopLevel=%d (all levels: failed=%d "
                        + "setterRejections=%d) depth=%d%n",
                closure.allClassNames().size(), limit, types.size(), built,
                types.size() - built, probe.failedCount(), probe.setterRejectionCount(), depth);
        return census;
    }

    @Override
    public int unitsPerOp() {
        return types == null ? 0 : types.size();
    }

    @Override
    public String unitLabel() {
        return "model types built";
    }

    @Override
    public long runOnce() {
        // fresh populator per op: its counters are census state, not measured state, and a
        // shared one would keep growing across ops
        ReflectivePopulator populator = new ReflectivePopulator();
        long checksum = 0L;
        for (Class<?> t : types) {
            Object o = populator.populate(t, depth);
            // identity hash: cheap, and it makes the result unconditionally consumed so the
            // JIT cannot delete the construction we are timing
            checksum += System.identityHashCode(o);
        }
        return checksum;
    }

    /** The selected types, so {@code serialise} can build over exactly the same sample. */
    public List<Class<?>> types() {
        return types;
    }
}
