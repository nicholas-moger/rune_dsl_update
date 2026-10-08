package com.regnosys.rosetta.harness.cache;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class DigestTest {

    @Test
    void sha256_of_empty_is_well_known_value() {
        // Canonical SHA-256 of zero bytes.
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                Digest.sha256(new byte[0]));
    }

    @Test
    void sha256_of_hello_is_well_known_value() {
        assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
                Digest.sha256("hello".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void sha256_is_lowercase_hex_64_chars() {
        String d = Digest.sha256("anything".getBytes(StandardCharsets.UTF_8));
        assertEquals(64, d.length());
        assertTrue(d.chars().allMatch(c -> (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f')),
                "SHA-256 hex should be lowercase a-f + 0-9: " + d);
    }

    @Test
    void sha256_of_string_equals_sha256_of_utf8_bytes() {
        String s = "type Foo:";
        assertEquals(Digest.sha256(s.getBytes(StandardCharsets.UTF_8)), Digest.sha256String(s));
    }

    @Test
    void sha256_null_throws() {
        assertThrows(NullPointerException.class, () -> Digest.sha256((byte[]) null));
        assertThrows(NullPointerException.class, () -> Digest.sha256String(null));
    }
}
