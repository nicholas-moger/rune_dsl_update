package com.regnosys.rosetta.ast.sourcerange;

import com.regnosys.rosetta.ast.SourceRange;
import org.antlr.v4.runtime.CommonToken;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SourceRangeByteOffsetTest {

    @Test
    void newConstructor_acceptsOffsets() {
        SourceRange r = new SourceRange("foo.rosetta", 1, 1, 2, 5, 0, 50);
        assertEquals(0, r.startOffset());
        assertEquals(50, r.endOffset());
        assertTrue(r.hasByteOffsets());
    }

    @Test
    void none_hasUnknownOffsets() {
        assertEquals(SourceRange.OFFSETS_UNKNOWN, SourceRange.NONE.startOffset());
        assertEquals(SourceRange.OFFSETS_UNKNOWN, SourceRange.NONE.endOffset());
        assertFalse(SourceRange.NONE.hasByteOffsets());
    }

    @Test
    void of5Arg_returnsRangeWithSentinelOffsets() {
        SourceRange r = SourceRange.of5Arg("foo.rosetta", 1, 1, 2, 5);
        assertEquals(SourceRange.OFFSETS_UNKNOWN, r.startOffset());
        assertEquals(SourceRange.OFFSETS_UNKNOWN, r.endOffset());
        assertFalse(r.hasByteOffsets());
        assertEquals(1, r.startLine());
        assertEquals(5, r.endCol());
    }

    @Test
    void mixedSentinelOffsets_throwsIllegalArgument() {
        // Mixed sentinel/valid offsets are illegal — the canonical contract
        // is "both unknown OR both populated and ordered". Mixed would make
        // hasByteOffsets() ambiguous.
        IllegalArgumentException a = assertThrows(IllegalArgumentException.class,
                () -> new SourceRange("f", 1, 1, 1, 1, SourceRange.OFFSETS_UNKNOWN, 50));
        IllegalArgumentException b = assertThrows(IllegalArgumentException.class,
                () -> new SourceRange("f", 1, 1, 1, 1, 0, SourceRange.OFFSETS_UNKNOWN));
        assertTrue(a.getMessage().contains("OFFSETS_UNKNOWN"));
        assertTrue(b.getMessage().contains("OFFSETS_UNKNOWN"));
    }

    @Test
    void unorderedOffsets_throwsIllegalArgument() {
        // endOffset must be >= startOffset.
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new SourceRange("f", 1, 1, 1, 1, 50, 0));
        assertTrue(ex.getMessage().contains("startOffset=50"));
        assertTrue(ex.getMessage().contains("endOffset=0"));
    }

    @Test
    void negativeNonSentinelOffsets_throwsIllegalArgument() {
        // Only OFFSETS_UNKNOWN (-1) is an allowed negative; -2 is not.
        assertThrows(IllegalArgumentException.class,
                () -> new SourceRange("f", 1, 1, 1, 1, -2, 5));
        assertThrows(IllegalArgumentException.class,
                () -> new SourceRange("f", 1, 1, 1, 1, 5, -2));
    }

    @Test
    void of_stopIndexAtTableBoundary_throwsIllegalArgument() {
        // Regression guard: charToByte has length content.length()+1; the
        // largest valid index is charToByte.length-1. A bounds check using
        // ">" instead of ">=" would let stopIdx == charToByte.length slip
        // through and AIOOBE on the array access.
        int[] charToByte = {0, 1, 2, 3, 4};   // for content of length 4
        CommonToken start = makeToken(0, 0);
        // stop.getStopIndex() = 4 → stopIdx = 5 = charToByte.length (illegal)
        CommonToken stop = makeToken(0, 4);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> SourceRange.of(start, stop, "f", charToByte));
        assertTrue(ex.getMessage().contains("Token indices out of range"));
        assertTrue(ex.getMessage().contains("length=5"));
    }

    @Test
    void of_startIndexAtTableBoundary_throwsIllegalArgument() {
        int[] charToByte = {0, 1, 2, 3, 4};
        CommonToken start = makeToken(5, 5);   // startIdx = 5 = length (illegal)
        CommonToken stop = makeToken(5, 5);
        assertThrows(IllegalArgumentException.class,
                () -> SourceRange.of(start, stop, "f", charToByte));
    }

    @Test
    void of_validIndices_succeeds() {
        int[] charToByte = {0, 1, 2, 3, 4};
        CommonToken start = makeToken(0, 0);   // 'a'
        CommonToken stop = makeToken(3, 3);    // last char, stopIdx = 4 = length-1 (legal)
        SourceRange r = SourceRange.of(start, stop, "f", charToByte);
        assertEquals(0, r.startOffset());
        assertEquals(4, r.endOffset());
    }

    private static CommonToken makeToken(int startIndex, int stopIndex) {
        CommonToken t = new CommonToken(0, "x");
        t.setLine(1);
        t.setCharPositionInLine(0);
        t.setStartIndex(startIndex);
        t.setStopIndex(stopIndex);
        return t;
    }
}
