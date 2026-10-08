package com.regnosys.rosetta.parser;

import com.regnosys.rosetta.symbols.diagnostics.ParserDiagnostic;
import com.regnosys.rosetta.symbols.diagnostics.RDiagnostic;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.antlr.v4.runtime.misc.ParseCancellationException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Convenience API for parsing .rosetta files.
 *
 * <p>Uses two-stage SLL/LL parsing for performance:
 * <ul>
 *   <li>Stage 1: SLL mode with BailErrorStrategy (8x faster for well-formed input)</li>
 *   <li>Stage 2: LL mode with DefaultErrorStrategy (full error recovery, only on SLL failure)</li>
 * </ul>
 *
 * <p>Diagnostic surfaces (P1.4.1c, see U006):
 * <ul>
 *   <li>{@link RosettaParseResult#errors()} — legacy {@code List<String>} (deprecated)</li>
 *   <li>{@link RosettaParseResult#diagnostics()} — structured {@code List<RDiagnostic>}</li>
 * </ul>
 * Both are populated in lockstep from the same {@link RuneDiagnosticListener}.
 */
public final class RosettaParserFacade {

    private RosettaParserFacade() {}

    public static RosettaParseResult parseFile(Path path) {
        try {
            return parse(CharStreams.fromPath(path), path.toString());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read " + path, e);
        }
    }

    public static RosettaParseResult parseString(String source) {
        return parse(CharStreams.fromString(source), "<string>");
    }

    /**
     * Backward-compat overload — preserves pre-P1.4.1c API for tests / external callers
     * that called the 1-arg form. File path defaults to the {@link CharStream}'s source name.
     */
    public static RosettaParseResult parse(CharStream input) {
        return parse(input, input.getSourceName());
    }

    public static RosettaParseResult parse(CharStream input, String filePath) {
        RosettaLexer lexer = new RosettaIdLexer(input);
        RuneDiagnosticListener lexerListener = new RuneDiagnosticListener(filePath);
        lexer.removeErrorListeners();
        lexer.addErrorListener(lexerListener);

        CommonTokenStream tokens = new CommonTokenStream(lexer);
        RosettaParser parser = new RosettaParser(tokens);
        parser.removeErrorListeners();

        // Stage 1: SLL mode — fast path for well-formed input.
        // Attach a listener so ANTLR's reportAmbiguity / reportContextSensitivity
        // INFO callbacks are captured even when SLL succeeds (BailErrorStrategy
        // suppresses errors but leaves ambiguity callbacks active). Without this,
        // INFO diagnostics would be silently dropped on the fast path.
        RuneDiagnosticListener stage1Listener = new RuneDiagnosticListener(filePath);
        parser.addErrorListener(stage1Listener);
        parser.getInterpreter().setPredictionMode(PredictionMode.SLL);
        parser.setErrorHandler(new BailErrorStrategy());
        try {
            RosettaParser.RosettaModelContext tree = parser.rosettaModel();
            return buildResult(tree, lexerListener.diagnostics(), stage1Listener.diagnostics());
        } catch (ParseCancellationException ex) {
            // Stage 2: LL mode — full error recovery on SLL failure.
            // Discard stage-1 listener (its diagnostics were against a partial parse
            // that is being abandoned); use a fresh stage-2 listener instead.
            tokens.seek(0);
            parser.reset();
            parser.removeErrorListeners();
            RuneDiagnosticListener parserListener = new RuneDiagnosticListener(filePath);
            parser.addErrorListener(parserListener);
            parser.getInterpreter().setPredictionMode(PredictionMode.LL);
            parser.setErrorHandler(new DefaultErrorStrategy());
            RosettaParser.RosettaModelContext tree = parser.rosettaModel();
            return buildResult(tree, lexerListener.diagnostics(), parserListener.diagnostics());
        }
    }

    private static RosettaParseResult buildResult(
            RosettaParser.RosettaModelContext tree,
            List<ParserDiagnostic> lexerDiags,
            List<ParserDiagnostic> parserDiags) {
        List<ParserDiagnostic> all = new ArrayList<>(lexerDiags.size() + parserDiags.size());
        all.addAll(lexerDiags);
        all.addAll(parserDiags);

        // Legacy `errors()` semantic: FATAL errors only (Severity.ERROR). INFO-level
        // diagnostics (ANTLR reportAmbiguity / reportContextSensitivity → RUNE-006/007)
        // appear ONLY in `diagnostics()`. AstBuilder treats `errors()` non-empty as
        // a hard parse-failure; bleeding INFO into errors() would break legacy callers.
        List<String> legacyErrors = new ArrayList<>(all.size());
        List<RDiagnostic> structured = new ArrayList<>(all.size());
        for (ParserDiagnostic d : all) {
            if (d.severity() == com.regnosys.rosetta.symbols.diagnostics.Severity.ERROR) {
                legacyErrors.add(d.toLegacyString());
            }
            structured.add(d);
        }
        return new RosettaParseResult(tree, List.copyOf(legacyErrors), List.copyOf(structured));
    }
}
