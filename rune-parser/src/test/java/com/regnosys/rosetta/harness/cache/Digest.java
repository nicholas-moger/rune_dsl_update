package com.regnosys.rosetta.harness.cache;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

/**
 * SHA-256 digest helper. Audit Q10 picked SHA-256 as the content-addressable
 * cache-key primitive (BLAKE3 deferred pending benchmark).
 *
 * <p>P1.2 audit hook H24 foundation. Pure-Java, no native deps; output is
 * lowercase hex so cache-key strings are safe to embed in file names.
 */
public final class Digest {

    private Digest() {}

    /** SHA-256 of {@code bytes}, returned as 64-character lowercase hex. */
    public static String sha256(byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes");
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 must be available on every JVM", e);
        }
        byte[] hash = md.digest(bytes);
        StringBuilder hex = new StringBuilder(hash.length * 2);
        for (byte b : hash) {
            hex.append(Character.forDigit((b >>> 4) & 0xF, 16));
            hex.append(Character.forDigit(b & 0xF, 16));
        }
        return hex.toString();
    }

    /** SHA-256 of {@code s} under its UTF-8 encoding. */
    public static String sha256String(String s) {
        Objects.requireNonNull(s, "s");
        return sha256(s.getBytes(StandardCharsets.UTF_8));
    }
}
