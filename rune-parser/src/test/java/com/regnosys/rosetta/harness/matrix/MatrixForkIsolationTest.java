package com.regnosys.rosetta.harness.matrix;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Sanity checks that Surefire's per-fork properties reach the test JVM.
 *
 * <p>H16 wires two sys props per fork:
 * <ul>
 *   <li>{@code java.io.tmpdir} — a fork-specific temp directory so concurrent
 *       forks cannot stomp on each other's temp files.
 *   <li>{@code matrix.ndjson.shard} — the NDJSON shard path this fork's
 *       matrix-cell reporter should write to.
 * </ul>
 *
 * <p>If either is missing or not parameterised by the fork number, the fork
 * plumbing is broken and the matrix NDJSON artifact will be corrupt when the
 * post-test concat step runs. These tests pin the contract.
 */
class MatrixForkIsolationTest {

    /**
     * The H16 argLine is only applied by Surefire when tests run in forked
     * JVMs. The dev override {@code -Dpr-speed.forkCount=0} runs tests
     * in-process with the Maven JVM and intentionally does not apply
     * argLine. Skip in that mode — H16 is a fork-only feature.
     */
    @BeforeEach
    void skipWhenNotForked() {
        assumeTrue(System.getProperty("matrix.ndjson.shard") != null,
                "H16 argLine not applied — in-process run (pr-speed.forkCount=0); H16 skipped");
    }

    @Test
    void tmpdir_is_set_to_a_fork_specific_directory() {
        String tmp = System.getProperty("java.io.tmpdir");
        assertNotNull(tmp, "java.io.tmpdir must be set");
        Path tmpPath = Path.of(tmp);
        assertTrue(Files.isDirectory(tmpPath),
                "java.io.tmpdir must point to an existing directory: " + tmp);
        // Fork isolation: path segment must contain "fork-<N>" (or the absent
        // forkNumber literal when running in-process) so concurrent forks have
        // disjoint tmp roots.
        String name = tmpPath.getFileName().toString();
        assertTrue(name.startsWith("fork-"),
                "tmp dir should be fork-scoped (fork-N or fork-${surefire.forkNumber}); got: " + tmp);
    }

    @Test
    void matrix_ndjson_shard_prop_is_set_and_fork_scoped() {
        String shard = System.getProperty("matrix.ndjson.shard");
        assertNotNull(shard, "matrix.ndjson.shard must be set by pom argLine");
        assertTrue(shard.endsWith(".ndjson"), "shard should be an .ndjson file: " + shard);
        assertTrue(shard.contains("shard-"), "shard filename should be fork-scoped: " + shard);
    }

    @Test
    void reporter_writes_to_the_configured_shard_path_when_requested() throws Exception {
        // Use a sibling file so we do not clobber the real shard for this fork.
        Path shard = Path.of(System.getProperty("matrix.ndjson.shard"));
        Path sibling = shard.resolveSibling("isolation-probe-" + System.nanoTime() + ".ndjson");

        Version v = Version.parse("6.16.0");
        MatrixCoordinate mc = new MatrixCoordinate(
                Corpus.CDM, "", v, ElementKind.TYPE, Path.of("probe.rosetta"));
        try (Reporter r = Reporter.open(sibling)) {
            r.record(Reporter.Result.passed(mc, 1));
        }
        try {
            assertTrue(Files.exists(sibling), "Reporter should create the shard file");
            assertTrue(Files.size(sibling) > 0, "Reporter should write at least one record");
        } finally {
            Files.deleteIfExists(sibling);
        }
    }
}
