package com.regnosys.rosetta.harness.cache;

import com.regnosys.rosetta.harness.matrix.Corpus;
import com.regnosys.rosetta.harness.matrix.ElementKind;
import com.regnosys.rosetta.harness.matrix.MatrixCoordinate;
import com.regnosys.rosetta.harness.matrix.Version;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.regnosys.rosetta.harness.cache.CachedMatrixExtension.Mode;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests that drive {@link CachedMatrixExtension} end-to-end
 * with a real {@link LocalDiskCache} backed by {@code @TempDir} and a
 * fake {@link FakeInvocation} as the test-method proxy.
 *
 * <p>These tests pin the core caching contract: PASSED hits skip the
 * invocation; misses / failures / non-PASSED hits proceed normally;
 * mode flags (READ_ONLY / WRITE_ONLY / DISABLED) behave per audit Q12.
 */
class CachedMatrixExtensionIntegrationTest {

    private static final Version V = Version.parse("6.16.0");

    private static MatrixCoordinate coord(Path rosettaFile) {
        return new MatrixCoordinate(
                Corpus.CDM, "", V, ElementKind.TYPE, rosettaFile);
    }

    private static Path writeRosetta(Path dir, String name, String body) throws IOException {
        Path p = dir.resolve(name);
        Files.writeString(p, body, StandardCharsets.UTF_8);
        return p;
    }

    // ---- READ_WRITE (default) ----

    @Test
    void miss_then_hit_second_invocation_is_skipped(@TempDir Path cacheRoot, @TempDir Path srcDir) throws Throwable {
        CachedMatrixExtension ext = new CachedMatrixExtension(
                LocalDiskCache.at(cacheRoot), Mode.READ_WRITE);
        Path src = writeRosetta(srcDir, "a.rosetta", "namespace x\ntype Foo:\n");
        MatrixCoordinate mc = coord(src);
        AtomicInteger runs = new AtomicInteger();

        FakeReflectiveContext ctx = FakeReflectiveContext.withArgsAndTargetClass(
                List.of(mc), CachedMatrixExtensionIntegrationTest.class);

        FakeInvocation first = new FakeInvocation(runs::incrementAndGet);
        ext.interceptTestTemplateMethod(first, ctx, null);
        assertEquals(1, runs.get(), "first call should execute the test body");
        assertTrue(first.proceeded);
        assertFalse(first.skipped);

        FakeInvocation second = new FakeInvocation(runs::incrementAndGet);
        ext.interceptTestTemplateMethod(second, ctx, null);
        assertEquals(1, runs.get(), "second call should be cache-skipped (body not re-run)");
        assertFalse(second.proceeded);
        assertTrue(second.skipped);
    }

    @Test
    void failure_is_cached_and_rethrown_but_not_skipped(@TempDir Path cacheRoot, @TempDir Path srcDir) throws Throwable {
        CachedMatrixExtension ext = new CachedMatrixExtension(
                LocalDiskCache.at(cacheRoot), Mode.READ_WRITE);
        Path src = writeRosetta(srcDir, "a.rosetta", "broken");
        MatrixCoordinate mc = coord(src);

        FakeReflectiveContext ctx = FakeReflectiveContext.withArgsAndTargetClass(
                List.of(mc), CachedMatrixExtensionIntegrationTest.class);

        AssertionError boom = assertThrows(AssertionError.class, () ->
                ext.interceptTestTemplateMethod(
                        new FakeInvocation(() -> { throw new AssertionError("boom"); }),
                        ctx, null));
        assertEquals("boom", boom.getMessage());

        // Second invocation: failed entry is stored, but non-PASSED hits
        // re-run the test — the extension only short-circuits PASSED.
        AtomicInteger runs = new AtomicInteger();
        FakeInvocation second = new FakeInvocation(runs::incrementAndGet);
        ext.interceptTestTemplateMethod(second, ctx, null);
        assertEquals(1, runs.get(),
                "non-PASSED cache hit must not short-circuit — re-run");
        assertTrue(second.proceeded);
    }

    @Test
    void source_edit_invalidates_cached_pass(@TempDir Path cacheRoot, @TempDir Path srcDir) throws Throwable {
        CachedMatrixExtension ext = new CachedMatrixExtension(
                LocalDiskCache.at(cacheRoot), Mode.READ_WRITE);
        Path src = writeRosetta(srcDir, "a.rosetta", "type Foo:\n");
        MatrixCoordinate mc = coord(src);
        AtomicInteger runs = new AtomicInteger();
        FakeReflectiveContext ctx = FakeReflectiveContext.withArgsAndTargetClass(
                List.of(mc), CachedMatrixExtensionIntegrationTest.class);

        ext.interceptTestTemplateMethod(new FakeInvocation(runs::incrementAndGet), ctx, null);
        assertEquals(1, runs.get());

        // Edit source — cache key changes (inputSha changes) so the entry
        // no longer matches and the body runs again.
        Files.writeString(src, "type Bar:\n", StandardCharsets.UTF_8);
        FakeInvocation second = new FakeInvocation(runs::incrementAndGet);
        ext.interceptTestTemplateMethod(second, ctx, null);
        assertEquals(2, runs.get(), "edited source should re-run");
        assertTrue(second.proceeded);
    }

    // ---- DISABLED ----

    @Test
    void disabled_mode_always_proceeds_and_never_reads_or_writes_cache(@TempDir Path cacheRoot, @TempDir Path srcDir) throws Throwable {
        CachedMatrixExtension ext = new CachedMatrixExtension(
                LocalDiskCache.at(cacheRoot), Mode.DISABLED);
        Path src = writeRosetta(srcDir, "a.rosetta", "type Foo:\n");
        MatrixCoordinate mc = coord(src);
        AtomicInteger runs = new AtomicInteger();
        FakeReflectiveContext ctx = FakeReflectiveContext.withArgsAndTargetClass(
                List.of(mc), CachedMatrixExtensionIntegrationTest.class);

        ext.interceptTestTemplateMethod(new FakeInvocation(runs::incrementAndGet), ctx, null);
        ext.interceptTestTemplateMethod(new FakeInvocation(runs::incrementAndGet), ctx, null);
        assertEquals(2, runs.get(), "DISABLED must always proceed");
        // Cache directory should be empty. Files.list returns a Stream that
        // must be closed to release the OS directory handle (Windows will
        // refuse to delete the TempDir otherwise).
        try (var listing = Files.list(cacheRoot)) {
            assertTrue(listing.findFirst().isEmpty(),
                    "DISABLED must not write any cache entries");
        }
    }

    // ---- READ_ONLY ----

    @Test
    void read_only_reads_existing_hits_but_does_not_write_new_ones(@TempDir Path cacheRoot, @TempDir Path srcDir) throws Throwable {
        // Priming pass in READ_WRITE to seed a PASSED entry.
        CachedMatrixExtension primer = new CachedMatrixExtension(
                LocalDiskCache.at(cacheRoot), Mode.READ_WRITE);
        Path src = writeRosetta(srcDir, "a.rosetta", "type Foo:\n");
        MatrixCoordinate mc = coord(src);
        FakeReflectiveContext ctx = FakeReflectiveContext.withArgsAndTargetClass(
                List.of(mc), CachedMatrixExtensionIntegrationTest.class);
        primer.interceptTestTemplateMethod(new FakeInvocation(() -> {}), ctx, null);

        // Read-only pass on the seeded cache — should skip.
        CachedMatrixExtension ro = new CachedMatrixExtension(
                LocalDiskCache.at(cacheRoot), Mode.READ_ONLY);
        AtomicInteger runs = new AtomicInteger();
        FakeInvocation second = new FakeInvocation(runs::incrementAndGet);
        ro.interceptTestTemplateMethod(second, ctx, null);
        assertEquals(0, runs.get());
        assertTrue(second.skipped);

        // Read-only on a DIFFERENT coord (no seed) should proceed without
        // writing anything back.
        Path src2 = writeRosetta(srcDir, "b.rosetta", "type Bar:\n");
        FakeReflectiveContext ctx2 = FakeReflectiveContext.withArgsAndTargetClass(
                List.of(coord(src2)), CachedMatrixExtensionIntegrationTest.class);
        FakeInvocation third = new FakeInvocation(runs::incrementAndGet);
        ro.interceptTestTemplateMethod(third, ctx2, null);
        assertEquals(1, runs.get(), "cold read-only should still run the test");
        assertTrue(third.proceeded);

        // Verify no new file was added for src2.
        CacheKey key2 = CachedMatrixExtension.computeKey(coord(src2), ctx2);
        assertTrue(LocalDiskCache.at(cacheRoot).get(key2).isEmpty(),
                "READ_ONLY must not write cache entries");
    }

    // ---- WRITE_ONLY ----

    @Test
    void write_only_always_proceeds_and_writes_back(@TempDir Path cacheRoot, @TempDir Path srcDir) throws Throwable {
        // Seed a PASSED entry first via READ_WRITE.
        CachedMatrixExtension primer = new CachedMatrixExtension(
                LocalDiskCache.at(cacheRoot), Mode.READ_WRITE);
        Path src = writeRosetta(srcDir, "a.rosetta", "type Foo:\n");
        MatrixCoordinate mc = coord(src);
        FakeReflectiveContext ctx = FakeReflectiveContext.withArgsAndTargetClass(
                List.of(mc), CachedMatrixExtensionIntegrationTest.class);
        primer.interceptTestTemplateMethod(new FakeInvocation(() -> {}), ctx, null);

        // WRITE_ONLY should IGNORE the hit and re-run (so it can overwrite).
        CachedMatrixExtension wo = new CachedMatrixExtension(
                LocalDiskCache.at(cacheRoot), Mode.WRITE_ONLY);
        AtomicInteger runs = new AtomicInteger();
        FakeInvocation second = new FakeInvocation(runs::incrementAndGet);
        wo.interceptTestTemplateMethod(second, ctx, null);
        assertEquals(1, runs.get(),
                "WRITE_ONLY must never use cache hits — always re-run for ground-truth");
        assertTrue(second.proceeded);
    }

    // ---- non-matrix tests ----

    @Test
    void argument_without_matrix_coordinate_passes_through_unchanged(@TempDir Path cacheRoot) throws Throwable {
        CachedMatrixExtension ext = new CachedMatrixExtension(
                LocalDiskCache.at(cacheRoot), Mode.READ_WRITE);
        AtomicInteger runs = new AtomicInteger();
        FakeReflectiveContext ctx = FakeReflectiveContext.withArgsAndTargetClass(
                List.of("non-matrix", 42), CachedMatrixExtensionIntegrationTest.class);
        FakeInvocation inv = new FakeInvocation(runs::incrementAndGet);
        ext.interceptTestTemplateMethod(inv, ctx, null);
        assertEquals(1, runs.get());
        assertTrue(inv.proceeded);
        assertFalse(inv.skipped);
    }

    // ---- intercept both @Test and @ParameterizedTest (sanity) ----

    @Test
    void interceptTestMethod_delegates_same_path_as_template(@TempDir Path cacheRoot, @TempDir Path srcDir) throws Throwable {
        CachedMatrixExtension ext = new CachedMatrixExtension(
                LocalDiskCache.at(cacheRoot), Mode.READ_WRITE);
        Path src = writeRosetta(srcDir, "a.rosetta", "type Foo:\n");
        MatrixCoordinate mc = coord(src);
        AtomicInteger runs = new AtomicInteger();
        FakeReflectiveContext ctx = FakeReflectiveContext.withArgsAndTargetClass(
                List.of(mc), CachedMatrixExtensionIntegrationTest.class);

        ext.interceptTestMethod(new FakeInvocation(runs::incrementAndGet), ctx, null);
        assertEquals(1, runs.get());

        FakeInvocation second = new FakeInvocation(runs::incrementAndGet);
        ext.interceptTestMethod(second, ctx, null);
        assertEquals(1, runs.get());
        assertTrue(second.skipped);
    }
}
