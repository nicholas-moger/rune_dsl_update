package com.regnosys.rosetta.harness.matrix;

import java.util.Objects;

/**
 * Byte-level canonicalisation used to derive stable cache keys across
 * operating systems.
 *
 * <p>P1.2 audit hook H23. Policy resolved by audit Q11:
 * <ol>
 *   <li>Strip a leading UTF-8 BOM (U+FEFF) if present.</li>
 *   <li>Convert every CRLF sequence to LF.</li>
 *   <li><b>Do not</b> trim trailing whitespace — would corrupt indented
 *       string literals, and byte-identity is already achievable without it.</li>
 * </ol>
 *
 * <p>The canonical form is idempotent by construction:
 * {@code canonicalise(canonicalise(x)).equals(canonicalise(x))} for every
 * input {@code x}. The companion Jqwik property pins this invariant.
 */
public final class Canonicaliser {

    /** Leading UTF-8 byte-order mark as a {@code char} (Java String unit). */
    private static final char BOM = '﻿';

    private Canonicaliser() {}

    /**
     * Canonicalise {@code source} per audit Q11. Never returns {@code null}.
     * Throws {@link NullPointerException} if {@code source} is {@code null}.
     */
    public static String canonicalise(String source) {
        Objects.requireNonNull(source, "source");
        String s = source;
        // Strip ALL leading BOMs — stripping a single BOM is not idempotent
        // when the input has two or more leading BOMs (caught by Jqwik).
        while (!s.isEmpty() && s.charAt(0) == BOM) {
            s = s.substring(1);
        }
        // Replace CRLF -> LF until stable. One pass is not idempotent for
        // inputs like "\r\r\n": single pass yields "\r\n", a CRLF that a
        // second pass would still rewrite.
        while (s.contains("\r\n")) {
            s = s.replace("\r\n", "\n");
        }
        return s;
    }
}
