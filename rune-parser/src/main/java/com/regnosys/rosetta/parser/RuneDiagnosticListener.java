package com.regnosys.rosetta.parser;

import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.parser.diagnostics.RuneErrorClassifier;
import com.regnosys.rosetta.parser.diagnostics.RuneErrorCode;
import com.regnosys.rosetta.symbols.diagnostics.DiagnosticCategory;
import com.regnosys.rosetta.symbols.diagnostics.ParserDiagnostic;
import com.regnosys.rosetta.symbols.diagnostics.Severity;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * ANTLR error listener producing structured {@link ParserDiagnostic} records
 * routed through the sealed {@code RDiagnostic} hierarchy.
 *
 * <p>Replaces {@link RosettaErrorListener} (kept {@code @Deprecated} for
 * backward compatibility). New consumers should use
 * {@link com.regnosys.rosetta.parser.RosettaParseResult#diagnostics()} —
 * legacy {@code errors(): List<String>} continues to populate via
 * {@link ParserDiagnostic#toLegacyString()}.
 *
 * <p>Wires three ANTLR callbacks:
 * <ul>
 *   <li>{@link #syntaxError} → ERROR-severity diagnostic</li>
 *   <li>{@link #reportAmbiguity} → INFO-severity diagnostic (RUNE-006)</li>
 *   <li>{@link #reportContextSensitivity} → INFO-severity diagnostic (RUNE-007)</li>
 * </ul>
 *
 * <p>Severity mapping per spec §15.A drift resolution: ANTLR ambiguity callbacks
 * are reported at {@code Severity.INFO} (no {@code HINT} value exists in the
 * landed Severity enum). Spec §6.4 originally proposed HINT for
 * reportContextSensitivity; lowered to INFO to preserve §7.2 invariant.
 *
 * <p>Spec: §6.4 in {@code docs/superpowers/specs/2026-04-26-p1.4.1-internal-hardening-design.md}.
 */
public class RuneDiagnosticListener extends BaseErrorListener {

    private final List<ParserDiagnostic> diagnostics = new ArrayList<>();
    private final String filePath;

    public RuneDiagnosticListener(String filePath) {
        this.filePath = Objects.requireNonNull(filePath, "filePath");
    }

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                            int line, int charPositionInLine, String msg,
                            RecognitionException e) {
        String offendingText = offendingSymbol instanceof Token t ? t.getText() : null;
        RuneErrorCode code = RuneErrorClassifier.classify(offendingText, msg, e);
        SourceRange range = rangeFor(offendingSymbol, line, charPositionInLine);

        diagnostics.add(new ParserDiagnostic(
            code, Severity.ERROR, DiagnosticCategory.PARSER,
            range, msg != null ? msg : "(no message)", Optional.empty()));
    }

    @Override
    public void reportAmbiguity(Parser recognizer, DFA dfa, int startIndex, int stopIndex,
                                 boolean exact, BitSet ambigAlts, ATNConfigSet configs) {
        SourceRange range = rangeForIndices(recognizer, startIndex, stopIndex);
        String rule = recognizer.getRuleNames()[recognizer.getContext().getRuleIndex()];

        diagnostics.add(new ParserDiagnostic(
            new RuneErrorCode.GrammarAmbiguity(rule, startIndex, stopIndex),
            Severity.INFO, DiagnosticCategory.PARSER,
            range,
            "Grammar ambiguity in rule " + rule + " at tokens [" + startIndex + ".." + stopIndex + "]",
            Optional.empty()));
    }

    @Override
    public void reportContextSensitivity(Parser recognizer, DFA dfa, int startIndex, int stopIndex,
                                          int prediction, ATNConfigSet configs) {
        SourceRange range = rangeForIndices(recognizer, startIndex, stopIndex);
        String rule = recognizer.getRuleNames()[recognizer.getContext().getRuleIndex()];

        diagnostics.add(new ParserDiagnostic(
            new RuneErrorCode.ContextSensitiveParse(rule, startIndex, stopIndex),
            Severity.INFO, DiagnosticCategory.PARSER,
            range,
            "Context-sensitive parse in rule " + rule + " at tokens [" + startIndex + ".." + stopIndex + "]",
            Optional.empty()));
    }

    public List<ParserDiagnostic> diagnostics() {
        return List.copyOf(diagnostics);
    }

    private SourceRange rangeFor(Object offendingSymbol, int line, int charPositionInLine) {
        if (offendingSymbol instanceof Token t) {
            int startLine = t.getLine();
            int startCol = t.getCharPositionInLine() + 1;
            String text = t.getText() != null ? t.getText() : "";
            // endCol is 1-indexed inclusive end of the last character (per SourceRange
            // canonical contract — see SourceRange.of() factory). Formula: 0-based
            // pos + length = 1-based inclusive end of last char. -1 token.length-clamp
            // ensures even zero-length tokens get endCol == startCol (collapsed range).
            int endCol = t.getCharPositionInLine() + Math.max(text.length(), 1);
            return new SourceRange(filePath, startLine, startCol, startLine, endCol,
                SourceRange.OFFSETS_UNKNOWN, SourceRange.OFFSETS_UNKNOWN);
        }
        return new SourceRange(filePath, line, charPositionInLine + 1,
            line, charPositionInLine + 2,
            SourceRange.OFFSETS_UNKNOWN, SourceRange.OFFSETS_UNKNOWN);
    }

    private SourceRange rangeForIndices(Parser recognizer, int startIndex, int stopIndex) {
        var tokens = recognizer.getTokenStream();
        if (tokens == null || startIndex < 0 || startIndex >= tokens.size()) {
            return SourceRange.NONE;
        }
        Token start = tokens.get(startIndex);
        Token stop = stopIndex >= 0 && stopIndex < tokens.size()
            ? tokens.get(stopIndex) : start;
        return new SourceRange(filePath,
            start.getLine(), start.getCharPositionInLine() + 1,
            stop.getLine(),
            stop.getCharPositionInLine() + (stop.getText() != null ? stop.getText().length() : 1),
            SourceRange.OFFSETS_UNKNOWN, SourceRange.OFFSETS_UNKNOWN);
    }
}
