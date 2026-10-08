package demo.harness.exec.workload;

import com.rosetta.model.lib.RosettaModelObject;
import demo.harness.exec.corpus.ModelClosure;
import demo.harness.exec.corpus.ReflectivePopulator;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Selection and reflection helpers shared by the five workloads. Everything here is
 * DETERMINISTIC by construction: the same jar, the same tree and the same arguments select
 * the same members in the same order on both legs and on every repeat run. Without that the
 * two legs would not be doing the same work, and a receipt would not be reproducible.
 */
public final class Workloads {

    /** The five workload ids accepted by {@code --workload}. */
    public static final List<String> NAMES =
            List.of("populate", "serialise", "validate", "report", "projection");

    private Workloads() {}

    /** Build the named workload over an open closure. */
    public static Workload create(String name, ModelClosure closure, int limit, int depth) {
        switch (name) {
            case "populate":
                return new PopulateWorkload(closure, limit, depth);
            case "serialise":
                return new SerialiseWorkload(closure, limit, depth);
            case "validate":
                return new ValidateWorkload(closure, limit, depth);
            case "report":
                return FunctionWorkload.reports(closure, limit, depth);
            case "projection":
                return FunctionWorkload.projections(closure, limit, depth);
            default:
                throw new IllegalArgumentException("unknown --workload '" + name
                        + "' (expected one of " + NAMES + ")");
        }
    }

    /**
     * Load without running static initialisers: the probe touches thousands of classes and
     * only needs their shape, and deferring initialisation avoids an initialiser storm on
     * classes the workload will never use.
     */
    public static Class<?> loadShape(ModelClosure closure, String name) throws ClassNotFoundException {
        return Class.forName(name, false, closure.loader());
    }

    /** A generated model type: an interface extending RosettaModelObject with a static builder(). */
    public static boolean isModelType(Class<?> c) {
        if (!c.isInterface() || !RosettaModelObject.class.isAssignableFrom(c)) {
            return false;
        }
        try {
            Method b = c.getMethod("builder");
            return Modifier.isStatic(b.getModifiers()) && b.getParameterCount() == 0;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    /**
     * A STRATIFIED, deterministic sample of model types. Class names are sorted, so they are
     * grouped by package; walking them with a stride therefore spreads the sample across the
     * whole closure instead of taking the first N (which would be one corner of cdm). A
     * second in-order pass tops the sample up if the strided pass came up short, so the
     * requested count is met whenever the closure can meet it - and the census records both
     * the requested and the achieved figure either way.
     *
     * @param limit target sample size; {@code <= 0} means every model type in the closure
     */
    public static List<Class<?>> selectModelTypes(ModelClosure closure, int limit) {
        List<String> names = closure.allClassNames();
        List<Class<?>> out = new ArrayList<>();
        Set<String> taken = new LinkedHashSet<>();
        int n = names.size();
        if (n == 0) {
            return out;
        }
        if (limit <= 0) {
            for (String name : names) {
                Class<?> c = tryModelType(closure, name);
                if (c != null) {
                    out.add(c);
                }
            }
            return out;
        }
        int stride = Math.max(1, n / Math.max(1, limit * 8));
        for (int i = 0; i < n && out.size() < limit; i += stride) {
            String name = names.get(i);
            Class<?> c = tryModelType(closure, name);
            if (c != null && taken.add(name)) {
                out.add(c);
            }
        }
        for (int i = 0; i < n && out.size() < limit; i++) {
            String name = names.get(i);
            if (taken.contains(name)) {
                continue;
            }
            Class<?> c = tryModelType(closure, name);
            if (c != null && taken.add(name)) {
                out.add(c);
            }
        }
        return out;
    }

    private static Class<?> tryModelType(ModelClosure closure, String name) {
        try {
            Class<?> c = loadShape(closure, name);
            return isModelType(c) ? c : null;
        } catch (Throwable t) {
            return null; // an unloadable class is simply not a candidate
        }
    }

    /** Deterministic stratified sample of an already-filtered name list. */
    public static List<String> stratify(List<String> sorted, int limit) {
        int n = sorted.size();
        if (limit <= 0 || limit >= n) {
            return sorted;
        }
        List<String> out = new ArrayList<>(limit);
        // even spacing across the sorted set, first element always included
        for (int i = 0; i < limit; i++) {
            int idx = (int) ((long) i * n / limit);
            if (idx >= n) {
                idx = n - 1;
            }
            out.add(sorted.get(idx));
        }
        return out;
    }

    /** Build one instance per type, dropping the ones the populator could not build. */
    public static List<Object> buildInstances(List<Class<?>> types, int depth,
                                              ReflectivePopulator populator) {
        List<Object> out = new ArrayList<>(types.size());
        for (Class<?> t : types) {
            Object o = populator.populate(t, depth);
            if (o != null) {
                out.add(o);
            }
        }
        return out;
    }

    /**
     * The generated function's {@code evaluate} entry point, chosen DETERMINISTICALLY:
     * synthetic and bridge methods are dropped (a {@code ReportFunction<In,Out>} carries an
     * {@code evaluate(Object)} bridge), then the remaining overloads are ordered by parameter
     * count and then by parameter type names, and the first is taken. {@code getMethods()}
     * order is JVM-unspecified, so without this the survivor census could differ run to run.
     */
    public static Method evaluateMethod(Class<?> cls) {
        List<Method> candidates = new ArrayList<>();
        for (Method m : cls.getMethods()) {
            if (!"evaluate".equals(m.getName()) || m.isBridge() || m.isSynthetic()) {
                continue;
            }
            if (m.getParameterCount() == 1 && m.getParameterTypes()[0] == Object.class) {
                continue; // erased bridge that escaped isBridge on some compilers
            }
            candidates.add(m);
        }
        if (candidates.isEmpty()) {
            return null;
        }
        candidates.sort(Comparator.<Method>comparingInt(Method::getParameterCount)
                .thenComparing(Workloads::paramSignature));
        return candidates.get(0);
    }

    private static String paramSignature(Method m) {
        StringBuilder sb = new StringBuilder();
        for (Class<?> p : m.getParameterTypes()) {
            sb.append(p.getName()).append(',');
        }
        return sb.toString();
    }

    /**
     * Synthetic arguments for {@code m}, or null when any parameter type cannot be supplied
     * (the caller counts that as an {@code inputFailed}, never as a success). A zero-argument
     * {@code evaluate} - common among the projection {@code Create_*} functions - yields an
     * empty array, which is a valid call, not a failure.
     */
    public static Object[] evaluateArgs(Method m, ReflectivePopulator populator, int depth) {
        Class<?>[] params = m.getParameterTypes();
        Object[] args = new Object[params.length];
        for (int i = 0; i < params.length; i++) {
            // Model-object parameters are populated AT depth, matching the reference
            // benchmark's populate(paramType, 2); valueFor would consume one level itself.
            Object v = RosettaModelObject.class.isAssignableFrom(params[i])
                    ? populator.populate(params[i], depth)
                    : populator.valueFor(params[i], depth);
            if (v == null) {
                return null;
            }
            args[i] = v;
        }
        return args;
    }
}
