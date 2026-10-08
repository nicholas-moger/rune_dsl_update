package com.regnosys.rosetta.ast;

import java.util.Objects;

/**
 * Builds a per-file {@code int[]} mapping char index to UTF-8 byte offset.
 *
 * <p>ANTLR's {@code Token.getStartIndex()} returns CHAR index into the
 * {@code CharStream}, not byte index. For ASCII-only input these are
 * identical, but for any non-ASCII codepoint (Unicode identifiers,
 * em-dashes in comments, copyright signs in headers) char index and
 * UTF-8 byte offset diverge.
 *
 * <p>Build the table once per file at parse time, pass to
 * {@link SourceRange#of(org.antlr.v4.runtime.Token, org.antlr.v4.runtime.Token, String, int[])}.
 *
 * <p>Per D13 in the development decision log.
 */
public final class CharToByteOffsets {

    private CharToByteOffsets() {}

    /**
     * Returns an int array of length {@code content.length() + 1}.
     *
     * <p>{@code table[i]} is the UTF-8 byte offset corresponding to char
     * index {@code i} — specifically, the number of UTF-8 bytes that
     * encode the substring {@code content.substring(0, i)}. The final
     * entry {@code table[content.length()]} is the total UTF-8 byte length
     * of the string.
     *
     * <p>Surrogate pair handling: when the codepoint at char {@code i} is
     * supplementary (4-byte UTF-8), the high surrogate at {@code i} maps
     * to the byte position before the 4-byte sequence, and the low
     * surrogate at {@code i + 1} maps to the byte position after it
     * (equivalent to the start of the next codepoint). This matches LSP
     * exclusive-end semantics: {@code Token.getStopIndex() + 1} indexes
     * past the last char of the token, and the table at that index
     * yields the byte after the last UTF-8 byte of the token.
     */
    public static int[] table(String content) {
        Objects.requireNonNull(content, "content must not be null");
        int[] t = new int[content.length() + 1];
        int b = 0;
        int i = 0;
        while (i < content.length()) {
            t[i] = b;
            int cp = content.codePointAt(i);
            b += utf8ByteCount(cp);
            if (Character.isSupplementaryCodePoint(cp)) {
                i += 1;
                t[i] = b;
                i += 1;
            } else {
                i += 1;
            }
        }
        t[content.length()] = b;
        return t;
    }

    private static int utf8ByteCount(int cp) {
        if (cp < 0x80) return 1;
        if (cp < 0x800) return 2;
        if (cp < 0x10000) return 3;
        return 4;
    }
}
