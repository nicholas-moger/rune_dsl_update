package com.regnosys.rosetta.parser;

import org.antlr.v4.runtime.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Legacy ANTLR error listener producing {@code line:col message} strings.
 *
 * @deprecated since 0.1.0. New code should use {@link RuneDiagnosticListener},
 * which produces structured {@link com.regnosys.rosetta.symbols.diagnostics.ParserDiagnostic}
 * records with typed {@link com.regnosys.rosetta.parser.diagnostics.RuneErrorCode}
 * payloads. This class is no longer used by {@link RosettaParserFacade} and is
 * preserved for back-compat (e.g. external consumers that built directly against it).
 * Removal: not scheduled. See {@code docs/upgrades/U006-structured-diagnostics.md}.
 */
@Deprecated(since = "0.1.0", forRemoval = false)
public class RosettaErrorListener extends BaseErrorListener {
    private final List<String> errors = new ArrayList<>();

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                            int line, int charPositionInLine, String msg,
                            RecognitionException e) {
        errors.add(line + ":" + charPositionInLine + " " + msg);
    }

    public List<String> getErrors() {
        return List.copyOf(errors);
    }
}
