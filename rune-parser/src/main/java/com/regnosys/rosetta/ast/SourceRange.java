package com.regnosys.rosetta.ast;

import org.antlr.v4.runtime.Token;

/**
 * Immutable source-location record for AST nodes and individual tokens.
 *
 * <p>Coordinate semantics:
 * <ul>
 *   <li>{@code startLine, startCol}: 1-based, inclusive (rune-dsl convention).</li>
 *   <li>{@code endLine, endCol}: 1-based, inclusive end of last token.</li>
 *   <li>{@code startOffset, endOffset}: 0-based UTF-8 byte indices, half-open
 *       {@code [start, end)} per LSP / ECMA-426 / LLVM DWARF convention.
 *       {@link #OFFSETS_UNKNOWN} sentinel for ranges constructed without
 *       byte-offset info (legacy {@link #of5Arg} factory or {@link #NONE}).</li>
 * </ul>
 *
 * <p>The {@link #NONE} sentinel indicates that no source location is
 * available (e.g., for synthetic nodes injected during desugaring or
 * error recovery).
 *
 * <p>Per D13 in the development decision log. Manifest
 * entry U001 in {@code docs/upgrades/U001-bytewise-source-range.md}.
 *
 * @param file        the source file name (or "&lt;unknown&gt;" for unknown sources)
 * @param startLine   1-based start line
 * @param startCol    1-based start column
 * @param endLine     1-based end line
 * @param endCol      1-based end column (inclusive)
 * @param startOffset 0-based UTF-8 byte offset of start, or {@link #OFFSETS_UNKNOWN}
 * @param endOffset   0-based UTF-8 byte offset just past end, or {@link #OFFSETS_UNKNOWN}
 */
public record SourceRange(
        String file,
        int startLine,
        int startCol,
        int endLine,
        int endCol,
        int startOffset,
        int endOffset
) {
    public SourceRange {
        java.util.Objects.requireNonNull(file, "file must not be null");
        // Offsets must be either both sentinel or both non-negative + ordered.
        // Mixed (one sentinel, one valid) is illegal; otherwise hasByteOffsets()
        // would be ambiguous.
        boolean offsetsUnknown = startOffset == OFFSETS_UNKNOWN && endOffset == OFFSETS_UNKNOWN;
        boolean offsetsValid = startOffset >= 0 && endOffset >= 0 && endOffset >= startOffset;
        if (!offsetsUnknown && !offsetsValid) {
            throw new IllegalArgumentException(
                    "Offsets must either both be OFFSETS_UNKNOWN or both be non-negative and ordered: "
                            + "startOffset=" + startOffset + ", endOffset=" + endOffset);
        }
    }

    /** Sentinel value indicating no byte-offset information is available. */
    public static final int OFFSETS_UNKNOWN = -1;

    /**
     * Sentinel value indicating no source location is available.
     * Used as the default for AST nodes that are constructed synthetically
     * or before source mapping has been applied.
     */
    public static final SourceRange NONE =
            new SourceRange("<unknown>", 0, 0, 0, 0, OFFSETS_UNKNOWN, OFFSETS_UNKNOWN);

    /**
     * Returns true iff both byte-offset fields are populated (not sentinel).
     * Use this predicate instead of raw {@code -1} checks; the sentinel
     * value may change in a future minor version (the predicate will not).
     */
    public boolean hasByteOffsets() {
        return startOffset != OFFSETS_UNKNOWN && endOffset != OFFSETS_UNKNOWN;
    }

    /**
     * Builds a {@code SourceRange} from two ANTLR tokens with byte-offset
     * population via the supplied char-to-byte projection table.
     *
     * <p><strong>Preconditions enforced at runtime:</strong> all four args
     * non-null; {@code start.getStartIndex()} and {@code stop.getStopIndex()+1}
     * both in {@code [0, charToByte.length)}; and
     * {@code stop.getStopIndex()+1 >= start.getStartIndex()} (i.e., {@code stop}
     * must be at or after {@code start} in the source). Violations throw
     * {@link IllegalArgumentException} with a diagnostic message identifying
     * the offending indices.
     *
     * @param start      non-null start token
     * @param stop       non-null stop token (may equal {@code start}; must
     *                   not precede {@code start})
     * @param file       non-null file path
     * @param charToByte non-null projection table from
     *                   {@link CharToByteOffsets#table(String)}
     */
    public static SourceRange of(Token start, Token stop, String file, int[] charToByte) {
        java.util.Objects.requireNonNull(start, "start token must not be null");
        java.util.Objects.requireNonNull(stop, "stop token must not be null");
        java.util.Objects.requireNonNull(file, "file must not be null");
        java.util.Objects.requireNonNull(charToByte, "charToByte must not be null");
        int startIdx = start.getStartIndex();
        int stopIdx = stop.getStopIndex() + 1;
        // charToByte has length content.length()+1; valid indices are
        // [0, charToByte.length-1] inclusive, so reject >= length.
        if (startIdx < 0 || stopIdx < 0
                || startIdx >= charToByte.length || stopIdx >= charToByte.length
                || startIdx > stopIdx) {
            throw new IllegalArgumentException(
                    "Token indices out of range for charToByte (length=" + charToByte.length + "): "
                            + "startIndex=" + startIdx + ", stopIndex+1=" + stopIdx);
        }
        return new SourceRange(
                file,
                start.getLine(),
                start.getCharPositionInLine() + 1,
                stop.getLine(),
                stop.getCharPositionInLine() + stop.getText().length(),
                charToByte[startIdx],
                charToByte[stopIdx]
        );
    }

    /**
     * Legacy 5-arg factory. Returns a range with {@link #OFFSETS_UNKNOWN}
     * sentinel offsets. New code should call
     * {@link #of(Token, Token, String, int[])} instead.
     *
     * @deprecated since 0.1.0. Migrating consumers should use the 4-arg
     *     {@link #of(Token, Token, String, int[])} factory for byte-precise
     *     positions. See {@code docs/upgrades/U001-bytewise-source-range.md}
     *     for migration. Removal: not scheduled.
     */
    @Deprecated(since = "0.1.0", forRemoval = false)
    public static SourceRange of5Arg(String file, int sl, int sc, int el, int ec) {
        return new SourceRange(file, sl, sc, el, ec, OFFSETS_UNKNOWN, OFFSETS_UNKNOWN);
    }
}
