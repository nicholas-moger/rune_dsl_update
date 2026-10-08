package com.regnosys.rosetta.parser.diagnostics;

import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.symbols.diagnostics.DiagnosticCategory;
import com.regnosys.rosetta.symbols.diagnostics.ParserDiagnostic;
import com.regnosys.rosetta.symbols.diagnostics.RDiagnostic;
import com.regnosys.rosetta.symbols.diagnostics.Severity;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ParserDiagnosticTest {

    private static final SourceRange RANGE_LINE5_COL12 = new SourceRange(
            "test.rosetta", 5, 12, 5, 18, 100, 106);

    @Test
    void recordHoldsAllSixFields() {
        RuneErrorCode code = new RuneErrorCode.UnexpectedToken(
            Optional.of("'='"), Optional.empty(), "mismatched input");
        ParserDiagnostic d = new ParserDiagnostic(
            code, Severity.ERROR, DiagnosticCategory.PARSER,
            RANGE_LINE5_COL12, "mismatched input 'foo' expecting '='", Optional.empty());

        assertEquals(code, d.errorCode());
        assertEquals(code, d.code().orElseThrow());
        assertEquals(Severity.ERROR, d.severity());
        assertEquals(DiagnosticCategory.PARSER, d.category());
        assertEquals(RANGE_LINE5_COL12, d.range());
        assertEquals("mismatched input 'foo' expecting '='", d.message());
        assertTrue(d.hint().isEmpty());
    }

    @Test
    void implementsRDiagnosticSealedInterface() {
        RDiagnostic d = new ParserDiagnostic(
            new RuneErrorCode.MissingToken("ID"),
            Severity.ERROR, DiagnosticCategory.PARSER,
            RANGE_LINE5_COL12, "missing ID", Optional.empty());
        assertNotNull(d);
        assertTrue(d.code().isPresent());
    }

    @Test
    void toLegacyStringMatchesRosettaErrorListenerFormat() {
        // RosettaErrorListener emits "line:col message" where col is 0-indexed
        // (ANTLR getCharPositionInLine convention) and line is 1-indexed.
        // SourceRange holds startCol as 1-indexed (per CharToByteOffsets convention).
        // toLegacyString() must subtract 1 from startCol for round-trip parity.
        ParserDiagnostic d = new ParserDiagnostic(
            new RuneErrorCode.UnknownErrorPattern("mismatched input 'foo' expecting ID"),
            Severity.ERROR, DiagnosticCategory.PARSER,
            RANGE_LINE5_COL12, "mismatched input 'foo' expecting ID", Optional.empty());
        assertEquals("5:11 mismatched input 'foo' expecting ID", d.toLegacyString());
    }

    @Test
    void toLegacyStringHandlesCol0Boundary() {
        // SourceRange startCol = 1 (line start, 1-indexed) → legacy col 0.
        SourceRange range = new SourceRange("t.rosetta", 1, 1, 1, 5, 0, 4);
        ParserDiagnostic d = new ParserDiagnostic(
            new RuneErrorCode.UnknownErrorPattern("err"),
            Severity.ERROR, DiagnosticCategory.PARSER,
            range, "err", Optional.empty());
        assertEquals("1:0 err", d.toLegacyString());
    }

    @Test
    void nullArgsRejected() {
        assertThrows(NullPointerException.class,
            () -> new ParserDiagnostic(null, Severity.ERROR, DiagnosticCategory.PARSER,
                RANGE_LINE5_COL12, "msg", Optional.empty()));
        assertThrows(NullPointerException.class,
            () -> new ParserDiagnostic(new RuneErrorCode.MissingToken("X"),
                null, DiagnosticCategory.PARSER,
                RANGE_LINE5_COL12, "msg", Optional.empty()));
        assertThrows(NullPointerException.class,
            () -> new ParserDiagnostic(new RuneErrorCode.MissingToken("X"),
                Severity.ERROR, null,
                RANGE_LINE5_COL12, "msg", Optional.empty()));
        assertThrows(NullPointerException.class,
            () -> new ParserDiagnostic(new RuneErrorCode.MissingToken("X"),
                Severity.ERROR, DiagnosticCategory.PARSER,
                null, "msg", Optional.empty()));
        assertThrows(NullPointerException.class,
            () -> new ParserDiagnostic(new RuneErrorCode.MissingToken("X"),
                Severity.ERROR, DiagnosticCategory.PARSER,
                RANGE_LINE5_COL12, null, Optional.empty()));
        assertThrows(NullPointerException.class,
            () -> new ParserDiagnostic(new RuneErrorCode.MissingToken("X"),
                Severity.ERROR, DiagnosticCategory.PARSER,
                RANGE_LINE5_COL12, "msg", null));
    }
}
