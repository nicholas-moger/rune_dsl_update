/**
 * Scope-matrix test harness primitives for rune-dsl-plus P1.2 test expansion.
 *
 * <h2>Contents</h2>
 *
 * Typed coordinate axes (H13):
 * <ul>
 *   <li>{@link com.regnosys.rosetta.harness.matrix.Corpus}
 *   <li>{@link com.regnosys.rosetta.harness.matrix.Version}
 *   <li>{@link com.regnosys.rosetta.harness.matrix.ElementKind}
 *   <li>{@link com.regnosys.rosetta.harness.matrix.MatrixCoordinate}
 * </ul>
 *
 * Runner + reporter infrastructure (H14):
 * <ul>
 *   <li>{@link com.regnosys.rosetta.harness.matrix.MatrixCellsProvider} —
 *       JUnit {@code @ArgumentsSource} walking {@code test-corpus/**}
 *   <li>{@link com.regnosys.rosetta.harness.matrix.Reporter} — NDJSON sink,
 *       one record per matrix cell result
 * </ul>
 *
 * <h2>Future additions</h2>
 *
 * Deferred to subsequent audit hooks:
 * <ul>
 *   <li>Migration of existing corpus-aware tests to {@code @ParameterizedTest}
 *       with {@code MatrixCellsProvider} (H15)
 *   <li>Per-fork tmpdir + matrix {@code argLine} + NDJSON shard merge (H16)
 *   <li>Content-addressable cache {@code InvocationInterceptor} (H24)
 * </ul>
 *
 * <p>Design references (external master-skills repository, separately
 * versioned): {@code rune-ecosystem/rune-dsl-plus/reference/matrix-runner.md}
 * and {@code content-addressable-cache.md}.
 *
 * <p>No regex on structured content; no thread-parallel execution;
 * fork-parallel only per the Surefire configuration documented in
 * the development plan "2026-04-19-pr-speed-surefire-parallelism" and
 * {@code rune-parser/src/test/resources/junit-platform.properties}.
 */
package com.regnosys.rosetta.harness.matrix;
