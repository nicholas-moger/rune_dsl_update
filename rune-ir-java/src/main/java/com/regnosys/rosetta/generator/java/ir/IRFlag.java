package com.regnosys.rosetta.generator.java.ir;

/**
 * Single, centralised read of the lab-created Path-2 flag
 * {@code -Drosetta.generator.ir=true} (decision L-002). When disabled (the
 * default), no IR-routed path is taken and Path-1 output is byte-identical by
 * construction.
 *
 * <p>Lab-authored Phase-1 (not present upstream). The flag is read from a JVM
 * system property so it can be toggled per generation run without changing the
 * generator wiring.
 */
public final class IRFlag {

    /** JVM system property name: {@code rosetta.generator.ir}. */
    public static final String PROPERTY = "rosetta.generator.ir";

    private IRFlag() {
    }

    /** @return {@code true} iff {@code -Drosetta.generator.ir=true} is set. */
    public static boolean enabled() {
        return Boolean.getBoolean(PROPERTY);
    }
}
