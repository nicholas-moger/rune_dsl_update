package org.finos.rune.equivalence;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Leg G's structural layer: per generated type, discover the builder's
 * settable SLOTS and enumerate a bounded attribute-presence SEAT lattice
 * (the plan § 4.1 — "enumerate attribute-presence seat lattices (bounded
 * depth/width, census-printed)").
 *
 * <p>Slot discovery mirrors the {@code ReflectivePopulator} discipline
 * (single-argument {@code set*}/{@code add*} instance methods, name+param-type
 * sorted so enumeration order is deterministic and cross-classloader stable —
 * two loaders' copies of one builder class yield the same slot list in the
 * same order) with one strengthening: covariant-return BRIDGE methods are
 * excluded at discovery ({@code isBridge()}), so a bridge pair can never
 * contribute a duplicate (name, param-type) slot — the remaining sort keys
 * are strictly distinct and the order is total, never tie-dependent.
 *
 * <p>The full presence lattice is 2^n; the enumerated subset is linear and
 * census-printed against the full width so the bound is never silent:
 * <ul>
 *   <li>{@code MIN} — nothing set (the cardinality-violating arm wherever
 *   required attributes exist: the validator-targeting INVALID baseline);</li>
 *   <li>{@code FULL} — every suppliable slot set (the VALID-candidate arm);</li>
 *   <li>{@code FLIP:<slot>} — FULL minus exactly one slot, for the first
 *   {@code flipWidthCap} slots (single-seat violations where the flipped
 *   slot is required).</li>
 * </ul>
 * Reference-state arms (resolved/unresolved — the plan's
 * {@code [metadata reference]} contract) are enumerated per REFERENCE slot by
 * the synthesizer over the FULL seat; {@link #referenceSlots} reports which
 * slots qualify so the census can count them.
 */
public final class SeatLattice {

    /** One settable builder slot: {@code set*}/{@code add*} + its parameter type. */
    public record Slot(String attributeName, String methodName, Class<?> paramType) {}

    /** One enumerated seat: a label + the slots deliberately left ABSENT. */
    public record Seat(String label, List<String> absentAttributes) {}

    private final int flipWidthCap;

    public SeatLattice(int flipWidthCap) {
        if (flipWidthCap < 0) {
            throw new IllegalArgumentException("flipWidthCap must be >= 0: " + flipWidthCap);
        }
        this.flipWidthCap = flipWidthCap;
    }

    public int flipWidthCap() {
        return flipWidthCap;
    }

    /** Discover the deterministic slot list of {@code builderClass}. */
    public List<Slot> slots(Class<?> builderClass) {
        Method[] methods = builderClass.getMethods();
        Arrays.sort(methods, Comparator.comparing(Method::getName)
                .thenComparing(m -> m.getParameterCount() == 1
                        ? m.getParameterTypes()[0].getName() : ""));
        List<Slot> slots = new ArrayList<>();
        for (Method m : methods) {
            String n = m.getName();
            if (!(n.startsWith("set") || n.startsWith("add"))) continue;
            if (m.getParameterCount() != 1 || Modifier.isStatic(m.getModifiers())) continue;
            if (m.isBridge()) continue; // covariant-return duplicates never become slots
            String attribute = n.length() > 3
                    ? Character.toLowerCase(n.charAt(3)) + n.substring(4) : n;
            slots.add(new Slot(attribute, n, m.getParameterTypes()[0]));
        }
        return slots;
    }

    /**
     * The slots whose parameter type is a reference-carrying wrapper
     * ({@code ReferenceWithMeta*} shape: the type itself exposes both a
     * {@code setValue} and a {@code setGlobalReference} channel on its
     * builder) — the seats the resolved/unresolved arms quantify over.
     */
    public List<Slot> referenceSlots(List<Slot> slots) {
        List<Slot> refs = new ArrayList<>();
        for (Slot s : slots) {
            if (isReferenceWrapper(s.paramType())) {
                refs.add(s);
            }
        }
        return refs;
    }

    /**
     * A type is a reference wrapper when its own builder carries BOTH a value
     * channel and a global-reference channel — detected structurally (never by
     * name matching) off the generated {@code builder()} surface.
     */
    public static boolean isReferenceWrapper(Class<?> type) {
        Method builderMethod;
        try {
            builderMethod = type.getMethod("builder");
        } catch (NoSuchMethodException e) {
            return false;
        }
        Class<?> builderType = builderMethod.getReturnType();
        boolean hasValue = false;
        boolean hasGlobalReference = false;
        for (Method m : builderType.getMethods()) {
            if (m.getParameterCount() != 1) continue;
            if (m.getName().equals("setValue")) hasValue = true;
            if (m.getName().equals("setGlobalReference")
                    && m.getParameterTypes()[0] == String.class) {
                hasGlobalReference = true;
            }
        }
        return hasValue && hasGlobalReference;
    }

    /** Enumerate the bounded seat list for {@code slots} (census-stable order). */
    public List<Seat> seats(List<Slot> slots) {
        List<Seat> seats = new ArrayList<>();
        seats.add(new Seat("MIN", slots.stream().map(Slot::attributeName).toList()));
        seats.add(new Seat("FULL", List.of()));
        int flips = Math.min(flipWidthCap, slots.size());
        for (int i = 0; i < flips; i++) {
            Slot s = slots.get(i);
            seats.add(new Seat("FLIP:" + s.attributeName(), List.of(s.attributeName())));
        }
        return seats;
    }
}
