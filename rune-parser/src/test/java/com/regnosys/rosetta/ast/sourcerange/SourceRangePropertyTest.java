package com.regnosys.rosetta.ast.sourcerange;

import com.regnosys.rosetta.ast.CharToByteOffsets;
import com.regnosys.rosetta.ast.SourceRange;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.StringLength;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Jqwik property tests for {@link SourceRange} byte-offset semantics and
 * {@link CharToByteOffsets} table invariants.
 */
class SourceRangePropertyTest {

    @Property
    void hasByteOffsets_trueForValidNonNegativeOrderedOffsets(
            @ForAll @IntRange(min = 0, max = 1_000_000) int startOffset,
            @ForAll @IntRange(min = 0, max = 1_000_000) int delta) {
        // Construct a valid range: end >= start, both non-negative.
        int endOffset = startOffset + delta;
        SourceRange r = new SourceRange("f", 1, 1, 1, 1, startOffset, endOffset);
        assertTrue(r.hasByteOffsets(),
                "hasByteOffsets() must be true for non-negative ordered offsets");
    }

    @Property
    void hasByteOffsets_falseForBothSentinel(
            @ForAll @IntRange(min = 1, max = 100) int dummyLine) {
        // Only legal sentinel construction is BOTH offsets = OFFSETS_UNKNOWN
        // (mixed sentinel is rejected by the constructor — see
        // SourceRangeByteOffsetTest#mixedSentinelOffsets_throwsIllegalArgument).
        SourceRange r = new SourceRange(
                "f", dummyLine, 1, dummyLine, 1,
                SourceRange.OFFSETS_UNKNOWN,
                SourceRange.OFFSETS_UNKNOWN);
        assertFalse(r.hasByteOffsets());
    }

    @Property
    void of5Arg_alwaysReturnsSentinelOffsets(
            @ForAll int sl, @ForAll int sc,
            @ForAll int el, @ForAll int ec) {
        @SuppressWarnings("deprecation")
        SourceRange r = SourceRange.of5Arg("f", sl, sc, el, ec);
        assertEquals(SourceRange.OFFSETS_UNKNOWN, r.startOffset());
        assertEquals(SourceRange.OFFSETS_UNKNOWN, r.endOffset());
        assertFalse(r.hasByteOffsets());
        assertEquals(sl, r.startLine());
        assertEquals(ec, r.endCol());
    }

    @Property
    void charToByteTable_isMonotonicallyNonDecreasing(
            @ForAll @StringLength(min = 0, max = 200) String content) {
        int[] table = CharToByteOffsets.table(content);
        for (int i = 1; i < table.length; i++) {
            assertTrue(
                table[i] >= table[i - 1],
                "table[" + i + "]=" + table[i] + " < table[" + (i - 1) + "]=" + table[i - 1]
                    + " for input length " + content.length()
            );
        }
    }

    @Property
    void charToByteTable_finalEntryEqualsUtf8ByteLength(
            @ForAll @StringLength(min = 0, max = 200) String content) {
        int[] table = CharToByteOffsets.table(content);
        int expected = content.getBytes(StandardCharsets.UTF_8).length;
        assertEquals(expected, table[content.length()]);
    }

    @Property
    void charToByteTable_lengthEqualsContentLengthPlusOne(
            @ForAll @StringLength(min = 0, max = 200) String content) {
        int[] table = CharToByteOffsets.table(content);
        assertEquals(content.length() + 1, table.length);
    }
}
