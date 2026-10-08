package com.regnosys.rosetta.parser;

import org.antlr.v4.runtime.BaseErrorListener;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression tests for the {@code @Deprecated} annotations on the legacy
 * parser surface (RosettaErrorListener class + RosettaParseResult.errors()
 * accessor). Locks the back-compat policy: classes/accessors stay deprecated
 * with {@code forRemoval=false}; removal is not scheduled.
 *
 * <p>Spec §7.2 + §7.4 + §15.A in {@code docs/superpowers/specs/2026-04-26-p1.4.1-internal-hardening-design.md}.
 */
@SuppressWarnings("deprecation")
class RosettaErrorListenerDeprecatedTest {

    @Test
    void classCarriesDeprecatedAnnotation() {
        Deprecated dep = RosettaErrorListener.class.getAnnotation(Deprecated.class);
        assertNotNull(dep, "RosettaErrorListener must be @Deprecated");
        assertEquals("0.1.0", dep.since(), "since must be 0.1.0");
        assertFalse(dep.forRemoval(), "forRemoval must be false (no removal scheduled)");
    }

    @Test
    void classStillFunctional() {
        // Even though deprecated, must continue to work for existing consumers.
        RosettaErrorListener listener = new RosettaErrorListener();
        assertTrue(listener instanceof BaseErrorListener);
        assertTrue(listener.getErrors().isEmpty());
    }

    @Test
    void parseResultErrorsAccessorIsDeprecated() throws NoSuchMethodException {
        var method = RosettaParseResult.class.getMethod("errors");
        Deprecated dep = method.getAnnotation(Deprecated.class);
        assertNotNull(dep, "RosettaParseResult.errors() accessor must be @Deprecated");
        assertEquals("0.1.0", dep.since());
        assertFalse(dep.forRemoval());
    }
}
