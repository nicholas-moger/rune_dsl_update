package com.regnosys.rosetta.harness.cache;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class LocalDiskCacheTest {

    private static CacheKey key(String id) {
        return new CacheKey(
                Digest.sha256String(id + "-input"),
                Digest.sha256String(id + "-bytecode"),
                Digest.sha256String(id + "-tool"));
    }

    @Test
    void get_returns_empty_on_miss(@TempDir Path dir) {
        LocalDiskCache cache = LocalDiskCache.at(dir);
        assertEquals(Optional.empty(), cache.get(key("miss")));
    }

    @Test
    void put_then_get_roundtrips_a_passed_entry(@TempDir Path dir) {
        LocalDiskCache cache = LocalDiskCache.at(dir);
        CacheKey k = key("pass");
        CacheEntry entry = CacheEntry.passed(k, 123L);
        cache.put(entry);
        assertEquals(Optional.of(entry), cache.get(k));
    }

    @Test
    void put_then_get_roundtrips_a_failed_entry(@TempDir Path dir) {
        LocalDiskCache cache = LocalDiskCache.at(dir);
        CacheKey k = key("fail");
        CacheEntry entry = CacheEntry.failed(k, 42L, "assertion X");
        cache.put(entry);
        assertEquals(Optional.of(entry), cache.get(k));
    }

    @Test
    void put_then_get_roundtrips_a_skipped_entry_with_special_chars(@TempDir Path dir) {
        LocalDiskCache cache = LocalDiskCache.at(dir);
        CacheKey k = key("skip");
        CacheEntry entry = CacheEntry.skipped(k, 0L, "line1\nline2 \"quoted\" \\slash\t tab");
        cache.put(entry);
        assertEquals(Optional.of(entry), cache.get(k));
    }

    @Test
    void put_overwrites_existing_entry(@TempDir Path dir) {
        LocalDiskCache cache = LocalDiskCache.at(dir);
        CacheKey k = key("overwrite");
        cache.put(CacheEntry.passed(k, 1L));
        cache.put(CacheEntry.failed(k, 2L, "now failed"));
        assertEquals(Optional.of(CacheEntry.failed(k, 2L, "now failed")), cache.get(k));
    }

    @Test
    void entries_with_different_keys_do_not_collide(@TempDir Path dir) {
        LocalDiskCache cache = LocalDiskCache.at(dir);
        cache.put(CacheEntry.passed(key("a"), 10));
        cache.put(CacheEntry.failed(key("b"), 20, "b failed"));
        assertEquals(Optional.of(CacheEntry.passed(key("a"), 10)), cache.get(key("a")));
        assertEquals(Optional.of(CacheEntry.failed(key("b"), 20, "b failed")), cache.get(key("b")));
    }

    @Test
    void cache_creates_root_directory_on_first_write(@TempDir Path parent) {
        Path root = parent.resolve("nested").resolve(".cache-p12");
        assertFalse(Files.exists(root));
        LocalDiskCache cache = LocalDiskCache.at(root);
        cache.put(CacheEntry.passed(key("first"), 1L));
        assertTrue(Files.isDirectory(root),
                "root must be created on first put");
    }

    @Test
    void second_cache_instance_sees_previously_written_entries(@TempDir Path dir) {
        CacheKey k = key("persistence");
        LocalDiskCache writer = LocalDiskCache.at(dir);
        writer.put(CacheEntry.passed(k, 5L));
        LocalDiskCache reader = LocalDiskCache.at(dir);
        assertEquals(Optional.of(CacheEntry.passed(k, 5L)), reader.get(k));
    }

    @Test
    void at_null_path_throws() {
        assertThrows(NullPointerException.class, () -> LocalDiskCache.at(null));
    }
}
