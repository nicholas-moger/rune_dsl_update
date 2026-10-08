package org.finos.rune.equivalence;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * O5 — the API-delta REPORT (revised requirement 1: measured + reported per
 * family PR, NOT a blocking gate): the signature-set diff between the
 * reference-emitted and optimised-emitted copies of each paired class, on TWO
 * channels reported beside — never merged into — each other:
 *
 * <ol>
 *   <li><b>The PUBLIC channel</b> ({@link #compare}) — {@code getMethods()} +
 *       {@code getFields()}, the standing channel every banked "O5 published:
 *       zero API delta" row was measured on. Its collection, string form and
 *       {@code Summary} shape are frozen so those rows stay byte-comparable.</li>
 *   <li><b>The PROTECTED channel</b> ({@link #compareDeclaredProtected}) — the
 *       § 6.3 alias re-typing program's instrument (the program plan § 7): the
 *       family's delta is protected-surface (the alias seams are
 *       protected-abstract, their overrides protected), which the public
 *       channel CANNOT see. Declared-protected methods are collected over each
 *       paired class AND its declared nested classes transitively (dispatch
 *       case units and {@code ...Default} overrides live in the nest), each
 *       signature prefixed with its declaring class's binary name so a nested
 *       seam attributes exactly. Fields are deliberately out of this channel:
 *       the program re-types method seams only, and the generated protected
 *       fields ({@code @Inject} dependencies) are body-side plumbing whose
 *       drift the differential gate already catches behaviourally.</li>
 * </ol>
 *
 * <p>Signatures are compared as strings (method name + parameter type names +
 * return type name; public fields as name + type name) so the comparison is
 * classloader-neutral. When both sides load the same compiled tree, either
 * channel MUST read zero — the machinery's own correctness check (the public
 * channel's at PR-3; the protected channel's pre-edit zero-proof at the § 6.3
 * program's T0); across an emitter change each is the compatibility report the
 * family PR publishes (the protected channel reconciling to the tranche
 * membership receipt — a scope-integrity belt, never a gate term).
 */
public final class ApiDeltaReport {

    /** Per-class delta: signatures present on exactly one side. */
    public record ClassDelta(String className, List<String> referenceOnly, List<String> optimisedOnly) {}

    /** The aggregated report. */
    public record Summary(int classesCompared, int classesWithDelta,
                          int referenceOnlySignatures, int optimisedOnlySignatures,
                          List<ClassDelta> deltas) {}

    private ApiDeltaReport() {}

    /**
     * Diff the paired classes' public surfaces. {@code classNames} is the
     * paired population (same names on both loaders — one-sided CLASSES are a
     * tree-level question owned by O6, not this report).
     */
    public static Summary compare(List<String> classNames, ClassLoader reference,
                                  ClassLoader optimised) {
        return diff(classNames, reference, optimised, ApiDeltaReport::signaturesOf);
    }

    /**
     * Diff the paired classes' DECLARED-PROTECTED method surfaces (the § 6.3
     * protected channel — see the class javadoc). Same paired population as
     * {@link #compare}; the two summaries are published side by side.
     */
    public static Summary compareDeclaredProtected(List<String> classNames, ClassLoader reference,
                                                   ClassLoader optimised) {
        return diff(classNames, reference, optimised, ApiDeltaReport::declaredProtectedSignaturesOf);
    }

    private interface SignatureCollector {
        Set<String> collect(String className, ClassLoader loader);
    }

    private static Summary diff(List<String> classNames, ClassLoader reference,
                                ClassLoader optimised, SignatureCollector collector) {
        List<ClassDelta> deltas = new ArrayList<>();
        int refOnlyTotal = 0;
        int optOnlyTotal = 0;
        for (String name : classNames) {
            Set<String> ref = collector.collect(name, reference);
            Set<String> opt = collector.collect(name, optimised);
            Set<String> refOnly = new TreeSet<>(ref);
            refOnly.removeAll(opt);
            Set<String> optOnly = new TreeSet<>(opt);
            optOnly.removeAll(ref);
            if (!refOnly.isEmpty() || !optOnly.isEmpty()) {
                deltas.add(new ClassDelta(name, List.copyOf(refOnly), List.copyOf(optOnly)));
                refOnlyTotal += refOnly.size();
                optOnlyTotal += optOnly.size();
            }
        }
        return new Summary(classNames.size(), deltas.size(), refOnlyTotal, optOnlyTotal, deltas);
    }

    private static Set<String> signaturesOf(String className, ClassLoader loader) {
        Class<?> c;
        try {
            c = Class.forName(className, false, loader);
        } catch (ClassNotFoundException e) {
            return Set.of("<class not loadable: " + e.getMessage() + ">");
        }
        Set<String> signatures = new LinkedHashSet<>();
        for (Method m : c.getMethods()) {
            signatures.add("method " + m.getReturnType().getName() + " " + m.getName() + "("
                    + Arrays.stream(m.getParameterTypes()).map(Class::getName)
                            .collect(Collectors.joining(",")) + ")");
        }
        for (Field f : c.getFields()) {
            signatures.add("field " + f.getType().getName() + " " + f.getName());
        }
        return signatures;
    }

    private static Set<String> declaredProtectedSignaturesOf(String className, ClassLoader loader) {
        Class<?> c;
        try {
            c = Class.forName(className, false, loader);
        } catch (ClassNotFoundException e) {
            return Set.of("<class not loadable: " + e.getMessage() + ">");
        }
        Set<String> signatures = new TreeSet<>();
        collectDeclaredProtected(c, signatures);
        return signatures;
    }

    /** Declared-protected methods of {@code c} and (recursively) its declared nested classes. */
    private static void collectDeclaredProtected(Class<?> c, Set<String> signatures) {
        for (Method m : c.getDeclaredMethods()) {
            // Synthetic/bridge methods are compiler artifacts, not the declared
            // source surface a subclass implementer sees — a re-typed seam's own
            // row is the real delta; its erasure bridge would only double-count it.
            if (Modifier.isProtected(m.getModifiers()) && !m.isSynthetic()) {
                signatures.add("method " + m.getDeclaringClass().getName() + " "
                        + m.getReturnType().getName() + " " + m.getName() + "("
                        + Arrays.stream(m.getParameterTypes()).map(Class::getName)
                                .collect(Collectors.joining(",")) + ")");
            }
        }
        for (Class<?> nested : c.getDeclaredClasses()) {
            collectDeclaredProtected(nested, signatures);
        }
    }
}
