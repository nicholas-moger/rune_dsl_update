package com.regnosys.rosetta.harness.cache;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CacheKeyTest {

    @Test
    void components_must_be_non_null() {
        assertThrows(NullPointerException.class,
                () -> new CacheKey(null, "b", "c"));
        assertThrows(NullPointerException.class,
                () -> new CacheKey("a", null, "c"));
        assertThrows(NullPointerException.class,
                () -> new CacheKey("a", "b", null));
    }

    @Test
    void fingerprint_is_deterministic() {
        CacheKey a = new CacheKey("input", "bytecode", "tool");
        CacheKey b = new CacheKey("input", "bytecode", "tool");
        assertEquals(a.fingerprint(), b.fingerprint());
    }

    @Test
    void different_components_yield_different_fingerprints() {
        CacheKey a = new CacheKey("input", "bytecode", "tool");
        CacheKey b = new CacheKey("InpuT", "bytecode", "tool");
        CacheKey c = new CacheKey("input", "BytEcodE", "tool");
        CacheKey d = new CacheKey("input", "bytecode", "Tool");
        assertNotEquals(a.fingerprint(), b.fingerprint());
        assertNotEquals(a.fingerprint(), c.fingerprint());
        assertNotEquals(a.fingerprint(), d.fingerprint());
    }

    @Test
    void fingerprint_is_lowercase_hex_64_chars() {
        CacheKey k = new CacheKey("a", "b", "c");
        String fp = k.fingerprint();
        assertEquals(64, fp.length());
        assertTrue(fp.chars().allMatch(ch -> (ch >= '0' && ch <= '9') || (ch >= 'a' && ch <= 'f')));
    }

    @Test
    void of_string_inputs_hashes_all_three_then_combines() {
        // Helper constructor: compute component digests first, then bind.
        CacheKey k = CacheKey.ofRawComponents("input-text", "bytecode-bytes", "tool-version");
        assertEquals(Digest.sha256String("input-text"), k.inputSha());
        assertEquals(Digest.sha256String("bytecode-bytes"), k.testBytecodeSha());
        assertEquals(Digest.sha256String("tool-version"), k.toolVersionSha());
    }
}
