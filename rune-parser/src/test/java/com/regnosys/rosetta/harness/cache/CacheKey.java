package com.regnosys.rosetta.harness.cache;

import java.util.Objects;

/**
 * Three-component content-addressable cache key.
 *
 * <p>Resolved by audit Q10 as: <em>input-SHA × test-bytecode-SHA ×
 * tool-version-SHA</em>. A cached result is reusable only when the same
 * {@code .rosetta} input is run by identical test bytecode under an
 * identical toolchain (ANTLR, parser jar, JVM).
 *
 * <p>P1.2 audit hook H24 foundation. Each component is assumed to be a
 * lowercase hex SHA-256 digest; pass raw bytes through {@link #ofRawComponents}
 * if you want the helper to compute the digests.
 *
 * <p>The fingerprint is SHA-256 of the literal concatenation
 * {@code inputSha + "/" + testBytecodeSha + "/" + toolVersionSha} — stable
 * across JVMs, deterministic, safe for embedding in file names.
 */
public record CacheKey(String inputSha, String testBytecodeSha, String toolVersionSha) {

    public CacheKey {
        Objects.requireNonNull(inputSha, "inputSha");
        Objects.requireNonNull(testBytecodeSha, "testBytecodeSha");
        Objects.requireNonNull(toolVersionSha, "toolVersionSha");
    }

    /**
     * Compose a key from raw component bytes (as strings). Each component is
     * SHA-256 hashed before binding into the key. Convenience for call sites
     * that don't already have the digests.
     */
    public static CacheKey ofRawComponents(String rawInput, String rawBytecode, String rawTool) {
        return new CacheKey(
                Digest.sha256String(rawInput),
                Digest.sha256String(rawBytecode),
                Digest.sha256String(rawTool));
    }

    /** Stable 64-char lowercase-hex fingerprint combining all three components. */
    public String fingerprint() {
        return Digest.sha256String(inputSha + "/" + testBytecodeSha + "/" + toolVersionSha);
    }
}
