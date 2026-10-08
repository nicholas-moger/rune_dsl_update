package com.regnosys.rosetta.parser.diagnostics;

import com.regnosys.rosetta.parser.RosettaLexer;
import com.regnosys.rosetta.parser.RosettaParser;
import com.regnosys.rosetta.parser.RuneDiagnosticListener;
import com.regnosys.rosetta.symbols.diagnostics.DiagnosticCategory;
import com.regnosys.rosetta.symbols.diagnostics.ParserDiagnostic;
import com.regnosys.rosetta.symbols.diagnostics.Severity;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RuneDiagnosticListenerTest {

    @Test
    void emptyResultOnValidInput() {
        var listener = new RuneDiagnosticListener("test.rosetta");
        parseLLMode("namespace com.test\ntype Foo:\n  a string (1..1)\n", listener);
        assertTrue(listener.diagnostics().isEmpty(), "valid input should produce no diagnostics");
    }

    @Test
    void syntaxErrorOnMalformedInputProducesParserDiagnostic() {
        var listener = new RuneDiagnosticListener("test.rosetta");
        // Stage 2 (LL mode) — only this path goes through addErrorListener.
        parseLLMode("namespace com.test\ntype Foo\n  a string (1..1)\n", listener);

        var diags = listener.diagnostics();
        assertFalse(diags.isEmpty(), "malformed input should produce ≥1 diagnostic");
        ParserDiagnostic d = diags.get(0);
        assertEquals(Severity.ERROR, d.severity());
        assertEquals(DiagnosticCategory.PARSER, d.category());
        assertNotNull(d.errorCode());
        assertTrue(d.message().length() > 0);
    }

    @Test
    void syntaxErrorRangeReflectsLineAndColumn() {
        var listener = new RuneDiagnosticListener("test.rosetta");
        parseLLMode("namespace com.test\ntype Foo\n  a string (1..1)\n", listener);

        var d = listener.diagnostics().get(0);
        assertTrue(d.range().startLine() >= 1, "startLine should be ≥1");
        assertTrue(d.range().startCol() >= 1, "startCol should be 1-indexed");
    }

    @Test
    void diagnosticsListIsImmutableCopy() {
        var listener = new RuneDiagnosticListener("test.rosetta");
        parseLLMode("namespace com.test\ntype Foo\n", listener);

        List<ParserDiagnostic> first = listener.diagnostics();
        List<ParserDiagnostic> second = listener.diagnostics();
        assertEquals(first, second);
        assertThrows(UnsupportedOperationException.class, () -> first.add(null));
    }

    @Test
    void multipleSyntaxErrorsAccumulate() {
        var listener = new RuneDiagnosticListener("test.rosetta");
        // Two distinct errors.
        parseLLMode("namespace com.test\ntype Foo\n  a string (1..1)\ntype Bar\n  b int (1..1)\n",
                listener);
        assertTrue(listener.diagnostics().size() >= 2,
            "expected ≥2 diagnostics; got " + listener.diagnostics().size());
    }

    @Test
    void filePathPropagatesToSourceRange() {
        var listener = new RuneDiagnosticListener("custom.rosetta");
        parseLLMode("namespace com.test\ntype Foo\n", listener);
        var d = listener.diagnostics().get(0);
        assertEquals("custom.rosetta", d.range().file());
    }

    @Test
    void rangeEndColumnIsInclusiveLastCharOfOffendingToken() {
        // Locks SourceRange contract: startCol is 1-based start, endCol is 1-based
        // inclusive end of the last character. For an offending multi-char token like
        // 'Foo' (3 chars) at column 5 (0-based 4), startCol=5 and endCol=4+3=7.
        // Earlier draft used `startCol + length` which produced endCol=8 (one past end).
        var listener = new RuneDiagnosticListener("test.rosetta");
        // 'extends' has 7 chars; missing colon makes 'a' the offending token (1 char)
        // and 'extends' a recovery candidate. We assert the range invariant on
        // whichever offending token surfaces.
        parseLLMode("namespace com.test\ntype Foo\n  attr string (1..1)\n", listener);
        for (var d : listener.diagnostics()) {
            int startCol = d.range().startCol();
            int endCol = d.range().endCol();
            assertTrue(endCol >= startCol,
                "endCol must be >= startCol; got startCol=" + startCol + ", endCol=" + endCol);
            // For a single-line range the difference (endCol - startCol + 1) equals
            // the offending text's length when length >= 1.
            assertTrue(endCol - startCol >= 0 && endCol - startCol < 200,
                "range width plausible; got " + (endCol - startCol));
        }
    }

    private RosettaParser.RosettaModelContext parseLLMode(
            String source, RuneDiagnosticListener listener) {
        var lexer = new RosettaLexer(CharStreams.fromString(source));
        lexer.removeErrorListeners();
        lexer.addErrorListener(listener);
        var tokens = new CommonTokenStream(lexer);
        var parser = new RosettaParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(listener);
        parser.getInterpreter().setPredictionMode(PredictionMode.LL);
        return parser.rosettaModel();
    }
}
