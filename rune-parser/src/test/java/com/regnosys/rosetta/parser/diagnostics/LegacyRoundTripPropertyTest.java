package com.regnosys.rosetta.parser.diagnostics;

import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.symbols.diagnostics.DiagnosticCategory;
import com.regnosys.rosetta.symbols.diagnostics.ParserDiagnostic;
import com.regnosys.rosetta.symbols.diagnostics.Severity;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Property-based test for {@link ParserDiagnostic#toLegacyString()} round-trip.
 *
 * <p>Format contract: {@code line:col message} where col is 0-based (subtract 1 from
 * SourceRange.startCol which is 1-based). Two properties:
 * <ul>
 *   <li>Format always equals {@code line:col-1 message} for any line/col/message</li>
 *   <li>Format is parseable back via prefix regex (line/col/message recoverable)</li>
 * </ul>
 */
class LegacyRoundTripPropertyTest {

    @Property
    void toLegacyStringFormatIsLineColonColSpaceMessage(
            @ForAll @IntRange(min = 1, max = 100000) int line,
            @ForAll @IntRange(min = 1, max = 1000) int startCol,
            @ForAll("safeMessages") String message) {
        SourceRange range = new SourceRange("test.rosetta",
            line, startCol, line, startCol + 1,
            SourceRange.OFFSETS_UNKNOWN, SourceRange.OFFSETS_UNKNOWN);

        ParserDiagnostic d = new ParserDiagnostic(
            new RuneErrorCode.UnknownErrorPattern(message),
            Severity.ERROR, DiagnosticCategory.PARSER,
            range, message, Optional.empty());

        String legacy = d.toLegacyString();
        String expected = line + ":" + (startCol - 1) + " " + message;
        assertTrue(legacy.equals(expected),
            "toLegacyString must produce 'line:col-1 message'; got: '" + legacy
                + "', expected: '" + expected + "'");
    }

    @Property
    void formattedStringIsParsableBackByPrefixRegex(
            @ForAll @IntRange(min = 1, max = 9999) int line,
            @ForAll @IntRange(min = 1, max = 999) int startCol,
            @ForAll("safeMessages") String message) {
        SourceRange range = new SourceRange("t.rosetta",
            line, startCol, line, startCol + 1,
            SourceRange.OFFSETS_UNKNOWN, SourceRange.OFFSETS_UNKNOWN);

        ParserDiagnostic d = new ParserDiagnostic(
            new RuneErrorCode.UnknownErrorPattern(message),
            Severity.ERROR, DiagnosticCategory.PARSER,
            range, message, Optional.empty());

        String legacy = d.toLegacyString();
        int colon = legacy.indexOf(':');
        int space = legacy.indexOf(' ', colon);
        assertTrue(colon > 0 && space > colon, "Format must contain ':' and ' '");

        int parsedLine = Integer.parseInt(legacy.substring(0, colon));
        int parsedCol = Integer.parseInt(legacy.substring(colon + 1, space));
        String parsedMsg = legacy.substring(space + 1);

        assertTrue(parsedLine == line, "line round-trip");
        assertTrue(parsedCol == startCol - 1, "col round-trip (0-based)");
        assertTrue(parsedMsg.equals(message), "message round-trip");
    }

    @Provide
    Arbitrary<String> safeMessages() {
        // Messages without newlines / leading whitespace — would break the
        // single-line legacy format.
        return Arbitraries.strings()
            .ofMinLength(1).ofMaxLength(80)
            .filter(s -> !s.contains("\n") && !s.contains("\r") && !s.startsWith(" "));
    }
}
