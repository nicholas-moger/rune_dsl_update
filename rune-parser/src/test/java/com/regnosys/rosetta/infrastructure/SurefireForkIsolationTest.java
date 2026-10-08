package com.regnosys.rosetta.infrastructure;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the fork-isolation invariant documented in Decision 40.
 *
 * <p>When surefire runs with {@code forkCount>1 reuseForks=true}, each fork
 * is an independent JVM. Tests MUST NOT assume any shared in-process state
 * across classes, and MUST NOT write to hard-coded non-fork-unique paths
 * that another concurrent fork could clobber.
 *
 * <p>This class asserts the most common failure modes are NOT introduced
 * going forward. It is intentionally defensive: a passing run proves the
 * fork-safe invariant holds in the narrow ways checked here; it does NOT
 * prove the invariant holds universally (that requires a full corpus-
 * sweep under parallelism, which IS the D38-F regression gate).
 */
class SurefireForkIsolationTest {

    /**
     * Asserts {@code surefire.forkNumber} is observable + numeric when
     * running under any surefire invocation that produces a fork
     * (which is {@code forkCount>=1} — Surefire sets the property in
     * every forked JVM, even when there is only one fork). Only runs
     * the assertion when the property is present; a truly
     * non-surefire run (e.g. an IDE launching the test in-process,
     * or {@code forkCount=0}) is a legitimate skip rather than a
     * failure.
     *
     * <p>Guards the contract relied on by Decision 40-C's escape
     * hatch — a {@code @DisabledIf} that pins a test to a single
     * chosen fork number. The specific number is the test author's
     * choice; any value in the Surefire-documented range works. Per
     * the maven-surefire-plugin documentation, {@code ${surefire.forkNumber}}
     * is replaced with values from 1 to ({@code forkCount} × parallel
     * builds), so the numbering is 1-based (never 0). {@code #1} is
     * the canonical recommendation because it is guaranteed to exist
     * whenever {@code forkCount>=1}; the corresponding annotation form
     * is {@code @DisabledIf("systemProperty.get('surefire.forkNumber') != '1'")}.
     * Also underpins Task 4's jqwik-database fork-local trick
     * ({@code target/jqwik-fork-${surefire.forkNumber}.bin}).
     */
    @Test
    void fork_identity_contract_holds() {
        String forkNumber = System.getProperty("surefire.forkNumber");
        if (forkNumber != null) {
            assertTrue(forkNumber.matches("\\d+"),
                "surefire.forkNumber should be numeric when set, got: "
                + forkNumber);
        }
    }

    /**
     * Asserts that the fork-unique path pattern used across the test base
     * (JUnit {@code @TempDir} + optional {@code surefire.forkNumber}
     * suffix) actually produces a writeable, fork-isolated path. If the
     * JUnit {@code @TempDir} contract regresses or permissions fail, this
     * test fails loudly instead of silently swallowing an IOException.
     *
     * <p>The literal {@code "0"} fallback is a sentinel for in-process
     * execution (when {@code forkCount=0} and surefire does not set the
     * property). Since Surefire numbers real forks from 1, "0" can never
     * collide with a real fork's value.
     */
    @Test
    void test_targets_use_fork_unique_paths(@TempDir Path tempDir) throws IOException {
        String forkNum = System.getProperty("surefire.forkNumber", "0");
        Path marker = tempDir.resolve("fork-" + forkNum + "-marker.txt");
        Files.writeString(marker, "ok");
        assertTrue(Files.exists(marker),
            "fork-unique marker not written: " + marker);
    }

    /**
     * Sanity-check that the generated parser package loads — a canary that
     * fails loudly if the ANTLR parser is renamed and existing tests
     * accidentally load the wrong class. Does NOT assert per-JVM DFA
     * cache isolation: isolation is guaranteed by surefire's fork model
     * itself (each fork is an independent JVM with its own static
     * initialisers, so the DFA cache cannot leak across forks by
     * construction).
     */
    @Test
    void parser_package_canary() {
        String expectedPkg = "com.regnosys.rosetta.parser";
        try {
            Class<?> lexer = Class.forName(expectedPkg + ".RosettaLexer");
            assertFalse(lexer.isInterface(),
                "RosettaLexer should be a class, not an interface.");
        } catch (ClassNotFoundException e) {
            throw new AssertionError(
                "Expected " + expectedPkg + ".RosettaLexer to exist. "
                + "If the parser was renamed, update this test.", e);
        }
    }
}
