package org.finos.rune.equivalence;

import org.finos.rune.benchmarks.corpus.ReflectivePopulator;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

/**
 * Leg G's value layer: build a deterministic instance of a generated type at
 * a given SEAT (the lattice's absence set), plus the reference-state arms
 * (resolved / unresolved) over reference-wrapper slots.
 *
 * <p>Value supply delegates to the PR-2 {@link ReflectivePopulator} table
 * (fixed scalars, first enum constant, bounded-depth recursion) so leg-G
 * instances stay byte-deterministic and cross-classloader consistent: the
 * SAME recipe applied to two loaders' copies of one class yields structurally
 * identical graphs. Accounting is split across two seams: NESTED populates
 * surface through the populator's own counters (ok/failed/setterRejections),
 * while TOP-LEVEL outcomes — a builder()/build() failure or a top-level
 * setter rejection — surface at the harness level (the null-pair /
 * one-sided-build counters inside the pair summary's divergence total), not
 * in the populator counters. Both seams print; neither swallows a cross-side
 * asymmetry.
 */
public final class InstanceSynthesizer {

    /** The two reference-state arms of the plan § 4.1. */
    public enum ReferenceArm {
        /** Wrapper carries its value (and a global key reference). */
        RESOLVED,
        /** Wrapper carries ONLY the address (global reference), no value. */
        UNRESOLVED
    }

    private final ReflectivePopulator populator = new ReflectivePopulator();
    private final int depth;

    public InstanceSynthesizer(int depth) {
        this.depth = depth;
    }

    public ReflectivePopulator populator() {
        return populator;
    }

    /**
     * Build {@code type} at the given seat: every discoverable slot is
     * populated EXCEPT the seat's absent set. Returns {@code null} when the
     * instance cannot be built — accounted by the CALLER's pair summary
     * (null-pair / one-sided counters), since a top-level builder failure
     * never reaches the populator's own counters.
     */
    public Object buildAtSeat(Class<?> type, Set<String> absentAttributes) {
        try {
            Method builderM = type.getMethod("builder");
            Object builder = builderM.invoke(null);
            SeatLattice lattice = new SeatLattice(0);
            for (SeatLattice.Slot slot : lattice.slots(builder.getClass())) {
                if (absentAttributes.contains(slot.attributeName())) continue;
                Object v = populator.valueFor(slot.paramType(), depth);
                if (v == null) continue;
                try {
                    builder.getClass().getMethod(slot.methodName(), slot.paramType())
                            .invoke(builder, v);
                } catch (Throwable t) {
                    // setter rejection: tolerated, the instance stays sparser —
                    // the populator's own counter convention
                }
            }
            return builder.getClass().getMethod("build").invoke(builder);
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Build a reference-wrapper instance in the given arm:
     * {@link ReferenceArm#RESOLVED} carries a populated value + a global
     * reference; {@link ReferenceArm#UNRESOLVED} carries ONLY the global
     * reference (address-only — navigation through it must yield empty).
     * Returns {@code null} when the wrapper cannot be built.
     */
    public Object buildReferenceArm(Class<?> wrapperType, ReferenceArm arm) {
        try {
            Object builder = wrapperType.getMethod("builder").invoke(null);
            Method setGlobal = null;
            Method setValue = null;
            // Sorted + bridge-excluded selection: getMethods() order is
            // JVM-unspecified, and the covariant-return BRIDGE setValue(Object)
            // must never out-select the real setter — an unsorted last-match
            // scan here made the reference-arm value population per-JVM
            // bimodal (caught by the pair census's populate-count drift; the
            // value table returns null for Object, so the bridge pick built
            // address-only arms and the count moved by whole subtrees).
            Method[] methods = builder.getClass().getMethods();
            java.util.Arrays.sort(methods, java.util.Comparator.comparing(Method::getName)
                    .thenComparing(m -> m.getParameterCount() == 1
                            ? m.getParameterTypes()[0].getName() : ""));
            for (Method m : methods) {
                if (m.getParameterCount() != 1 || m.isBridge()) continue;
                if (m.getName().equals("setGlobalReference")
                        && m.getParameterTypes()[0] == String.class) {
                    setGlobal = m;
                }
                if (m.getName().equals("setValue") && setValue == null) setValue = m;
            }
            if (setGlobal == null || setValue == null) {
                return null;
            }
            setGlobal.invoke(builder, "EQ-REF-1");
            if (arm == ReferenceArm.RESOLVED) {
                Object value = populator.valueFor(setValue.getParameterTypes()[0], depth);
                if (value != null) {
                    try {
                        setValue.invoke(builder, value);
                    } catch (Throwable t) {
                        // tolerated — the wrapper stays address-only and the
                        // pair still compares identically on both sides
                    }
                }
            }
            return builder.getClass().getMethod("build").invoke(builder);
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Convenience: build the FULL seat then, for each reference slot, the two
     * reference arms as SEPARATE wrapper instances (returned for pair
     * comparison; graft-into-parent synthesis deepens at the family PRs).
     */
    public List<Object> referenceArmInstances(Class<?> wrapperType) {
        Object resolved = buildReferenceArm(wrapperType, ReferenceArm.RESOLVED);
        Object unresolved = buildReferenceArm(wrapperType, ReferenceArm.UNRESOLVED);
        return java.util.Arrays.asList(resolved, unresolved);
    }
}
