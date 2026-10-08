package org.finos.rune.equivalence;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * O1's structural comparator, cross-classloader safe: two generated-model
 * object graphs loaded by DIFFERENT classloaders are never {@code equals()}
 * (distinct {@code Class} objects defeat every generated
 * {@code instanceof}/{@code getClass()} check), so equality is re-derived
 * structurally — twin classes are matched by FQCN and walked getter-by-getter
 * in deterministic order, scalars compared by value, enums by declaring-class
 * name + constant name, lists elementwise.
 *
 * <p>Returns the FIRST divergence as a path string ({@code null} = equal) so
 * a mismatch is actionable, not boolean. A depth guard fails loudly on
 * runaway recursion (built model objects are trees; hitting the guard is a
 * bug, not a comparison verdict).
 *
 * <p>Self-sufficiency note (the gate's no-miss argument): O1 must stand on
 * its own — the O5 API-delta report is REPORT-ONLY (revised requirement 1)
 * and never feeds the divergence total. The twin walk therefore compares the
 * UNION of both sides' getter surfaces (a getter present on exactly one side
 * is an O1 divergence in its own right), twin-typed arrays get an explicit
 * elementwise branch, and a cross-loader Map lands in the type-differs
 * branch (a loud false positive, never a silent miss).
 */
public final class ReflectiveDeepCompare {

    private static final int MAX_DEPTH = 64;

    private ReflectiveDeepCompare() {}

    /** @return {@code null} when structurally equal, else the first divergence path. */
    public static String firstDivergence(Object a, Object b) {
        return compare(a, b, "$", 0);
    }

    private static String compare(Object a, Object b, String path, int depth) {
        if (depth > MAX_DEPTH) {
            throw new IllegalStateException("deep-compare depth guard tripped at " + path
                    + " — cyclic or pathological graph; this is a harness bug, not a verdict");
        }
        if (a == null && b == null) return null;
        if (a == null || b == null) {
            return path + " one-sided null (left=" + present(a) + ", right=" + present(b) + ")";
        }
        if (a instanceof Class<?> ca && b instanceof Class<?> cb) {
            return ca.getName().equals(cb.getName()) ? null
                    : path + " class-valued getter differs: " + ca.getName() + " vs " + cb.getName();
        }
        if (a.getClass().isEnum() && b.getClass().isEnum()) {
            String an = a.getClass().getName() + "." + ((Enum<?>) a).name();
            String bn = b.getClass().getName() + "." + ((Enum<?>) b).name();
            return an.equals(bn) ? null : path + " enum differs: " + an + " vs " + bn;
        }
        if (a instanceof List<?> la && b instanceof List<?> lb) {
            if (la.size() != lb.size()) {
                return path + " list size differs: " + la.size() + " vs " + lb.size();
            }
            for (int i = 0; i < la.size(); i++) {
                String d = compare(la.get(i), lb.get(i), path + "[" + i + "]", depth + 1);
                if (d != null) return d;
            }
            return null;
        }
        if (a.getClass().isArray() && b.getClass().isArray()) {
            if (!a.getClass().getComponentType().getName()
                    .equals(b.getClass().getComponentType().getName())) {
                return path + " array component type differs: "
                        + a.getClass().getComponentType().getName() + " vs "
                        + b.getClass().getComponentType().getName();
            }
            int la = java.lang.reflect.Array.getLength(a);
            int lb = java.lang.reflect.Array.getLength(b);
            if (la != lb) {
                return path + " array length differs: " + la + " vs " + lb;
            }
            for (int i = 0; i < la; i++) {
                String d = compare(java.lang.reflect.Array.get(a, i),
                        java.lang.reflect.Array.get(b, i), path + "[" + i + "]", depth + 1);
                if (d != null) return d;
            }
            return null;
        }
        if (a.getClass() == b.getClass()) {
            // Same class object (JDK or shared-runtime type on the common parent
            // loader): value equality is authoritative.
            return a.equals(b) ? null : path + " value differs: " + a + " vs " + b;
        }
        if (a.getClass().getName().equals(b.getClass().getName())) {
            // Cross-loader twins: walk the getter surface.
            return compareTwins(a, b, path, depth);
        }
        return path + " type differs: " + a.getClass().getName() + " vs " + b.getClass().getName();
    }

    private static String compareTwins(Object a, Object b, String path, int depth) {
        // The UNION of both sides' getter names: a one-sided getter is an O1
        // divergence in its own right (O5 reports shape deltas but never gates).
        java.util.TreeSet<String> names = new java.util.TreeSet<>();
        for (Method m : getters(a.getClass())) names.add(m.getName());
        for (Method m : getters(b.getClass())) names.add(m.getName());
        for (String name : names) {
            Method aGetter;
            Method bGetter;
            try {
                aGetter = a.getClass().getMethod(name);
            } catch (NoSuchMethodException e) {
                return path + " twin getter missing on left: " + name;
            }
            try {
                bGetter = b.getClass().getMethod(name);
            } catch (NoSuchMethodException e) {
                return path + " twin getter missing on right: " + name;
            }
            Object av;
            Object bv;
            try {
                av = aGetter.invoke(a);
            } catch (Throwable t) {
                return path + "." + name + " left getter threw: " + t;
            }
            try {
                bv = bGetter.invoke(b);
            } catch (Throwable t) {
                return path + "." + name + " right getter threw: " + t;
            }
            String d = compare(av, bv, path + "." + name, depth + 1);
            if (d != null) return d;
        }
        return null;
    }

    /** Deterministic zero-arg {@code get*}/{@code is*} surface (never {@code getClass}). */
    private static List<Method> getters(Class<?> type) {
        Method[] methods = type.getMethods();
        Arrays.sort(methods, Comparator.comparing(Method::getName));
        return Arrays.stream(methods)
                .filter(m -> m.getParameterCount() == 0)
                .filter(m -> !Modifier.isStatic(m.getModifiers()))
                .filter(m -> m.getName().startsWith("get") || m.getName().startsWith("is"))
                .filter(m -> !m.getName().equals("getClass"))
                .filter(m -> m.getReturnType() != void.class)
                .toList();
    }

    private static String present(Object o) {
        return o == null ? "null" : "non-null";
    }
}
