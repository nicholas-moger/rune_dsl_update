package com.regnosys.rosetta.ast.sourcerange;

import com.regnosys.rosetta.ast.CharToByteOffsets;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CharToByteOffsetsTest {

    @Test
    void asciiOnly_charIndexEqualsByteOffset() {
        int[] table = CharToByteOffsets.table("hello");
        assertArrayEquals(new int[] {0, 1, 2, 3, 4, 5}, table);
    }

    @Test
    void emDash_isThreeBytes() {
        // U+2014 EM DASH = 0xE2 0x80 0x94 in UTF-8 (3 bytes)
        int[] table = CharToByteOffsets.table("a—b");
        assertArrayEquals(new int[] {0, 1, 4, 5}, table);
    }

    @Test
    void emoji_isFourBytes() {
        // U+1F600 GRINNING FACE = 0xF0 0x9F 0x98 0x80 in UTF-8 (4 bytes).
        // Java String stores as 2-char surrogate pair. The high surrogate at
        // char index 1 maps to the byte offset before the 4-byte sequence
        // (1 = start), and the low surrogate at char index 2 maps to the
        // byte offset after the 4-byte sequence (5 = end). This aligns with
        // LSP exclusive-end semantics for Token.getStopIndex() + 1.
        int[] table = CharToByteOffsets.table("a😀b");
        assertArrayEquals(new int[] {0, 1, 5, 5, 6}, table);
    }

    @Test
    void emptyString_returnsLengthOneTable() {
        int[] table = CharToByteOffsets.table("");
        assertArrayEquals(new int[] {0}, table);
    }

    @Test
    void copyrightSign_isTwoBytes() {
        // U+00A9 COPYRIGHT SIGN = 0xC2 0xA9 in UTF-8 (2 bytes)
        int[] table = CharToByteOffsets.table("©2026");
        assertArrayEquals(new int[] {0, 2, 3, 4, 5, 6}, table);
    }

    @Test
    void nullContent_throwsNpeWithMessage() {
        // Self-audit: Objects.requireNonNull guards against raw NPE
        // on .length() with no diagnostic context.
        NullPointerException ex = assertThrows(NullPointerException.class,
                () -> CharToByteOffsets.table(null));
        org.junit.jupiter.api.Assertions.assertTrue(
                ex.getMessage() != null && ex.getMessage().contains("content must not be null"),
                "expected named NPE, got: " + ex.getMessage());
    }
}
