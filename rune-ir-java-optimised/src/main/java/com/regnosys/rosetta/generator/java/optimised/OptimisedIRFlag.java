package com.regnosys.rosetta.generator.java.optimised;

/**
 * Single, centralised read of the v3 optimised route's opt-in flag
 * {@code -Drosetta.generator.ir.optimised=true} (the phase plan § 2 — the
 * flag name ratified at PR-3). When disabled (the default), no
 * optimised-routed path is taken and both standing routes' output is
 * byte-identical by construction.
 *
 * <p>The module-local mirror of the generator seam's
 * {@code IRGeneration.OPTIMISED_PROPERTY} — the same two-module constant
 * pattern the reference route's {@code IRFlag} carries, with the seam test
 * locking property-name agreement (the Rule-3 lock).
 */
public final class OptimisedIRFlag {

    /** JVM system property name: {@code rosetta.generator.ir.optimised}. */
    public static final String PROPERTY = "rosetta.generator.ir.optimised";

    private OptimisedIRFlag() {
    }

    /** @return {@code true} iff {@code -Drosetta.generator.ir.optimised=true} is set. */
    public static boolean enabled() {
        return Boolean.getBoolean(PROPERTY);
    }
}
