package com.regnosys.rosetta.parser.diagnostics;

import org.antlr.v4.runtime.*;

import java.util.Optional;

/**
 * Maps ANTLR error reports (typed exceptions + raw messages) to typed
 * {@link RuneErrorCode} variants.
 *
 * <p>Strategy:
 * <ol>
 *   <li>Inspect typed {@link RecognitionException} (most precise classification).</li>
 *   <li>Fall back to {@link String#startsWith}/{@link String#contains} on the raw
 *       message for ANTLR's untyped reports (missing-token, extraneous-input).</li>
 *   <li>Default to {@link RuneErrorCode.UnknownErrorPattern} carrying the raw text
 *       — never throws; the parser always gets a typed code.</li>
 * </ol>
 *
 * <p>Spec: §6.4 in {@code docs/superpowers/specs/2026-04-26-p1.4.1-internal-hardening-design.md}.
 */
public final class RuneErrorClassifier {

    private RuneErrorClassifier() {}

    /**
     * @param offendingText  text of the offending token/lexeme, or {@code null} if not applicable.
     * @param rawMessage     the raw message ANTLR passed to the listener.
     * @param exception      typed exception, or {@code null} if ANTLR produced none.
     */
    public static RuneErrorCode classify(
            String offendingText, String rawMessage, RecognitionException exception) {

        if (exception instanceof InputMismatchException) {
            return new RuneErrorCode.UnexpectedToken(
                expectedFromMessage(rawMessage),
                Optional.ofNullable(exception.getOffendingToken()),
                rawMessage != null ? rawMessage : "(no message)");
        }
        if (exception instanceof NoViableAltException) {
            return new RuneErrorCode.NoViableAlternative(
                contextFromMessage(rawMessage));
        }
        if (exception instanceof FailedPredicateException) {
            return new RuneErrorCode.FailedPredicate(
                exception.getMessage() != null ? exception.getMessage()
                    : (rawMessage != null ? rawMessage : "(unknown predicate)"));
        }
        if (exception instanceof LexerNoViableAltException) {
            return new RuneErrorCode.LexerNoViableAlternative(
                offendingText != null ? offendingText
                    : (rawMessage != null ? rawMessage : "(unknown lexeme)"));
        }

        // Untyped ANTLR reports — classify by message prefix.
        if (rawMessage != null) {
            if (rawMessage.startsWith("missing ")) {
                String tokenName = extractAfter(rawMessage, "missing ", " at ");
                return new RuneErrorCode.MissingToken(
                    tokenName != null ? tokenName : rawMessage);
            }
            if (rawMessage.startsWith("extraneous input")) {
                String text = extractBetween(rawMessage, "'", "'");
                return new RuneErrorCode.ExtraneousInput(
                    text != null ? text : rawMessage);
            }
            if (rawMessage.startsWith("mismatched input")) {
                String actual = extractBetween(rawMessage, "'", "'");
                String expected = expectedFromMessage(rawMessage).orElse("?");
                return new RuneErrorCode.MismatchedInput(expected,
                    actual != null ? actual : rawMessage);
            }
        }

        return new RuneErrorCode.UnknownErrorPattern(
            rawMessage != null ? rawMessage : "(no message)");
    }

    private static Optional<String> expectedFromMessage(String msg) {
        if (msg == null) return Optional.empty();
        int idx = msg.indexOf("expecting ");
        if (idx < 0) return Optional.empty();
        return Optional.of(msg.substring(idx + "expecting ".length()).trim());
    }

    private static String contextFromMessage(String msg) {
        if (msg == null) return "<unknown>";
        return msg;
    }

    private static String extractAfter(String s, String from, String to) {
        int a = s.indexOf(from);
        if (a < 0) return null;
        int b = s.indexOf(to, a + from.length());
        if (b < 0) return s.substring(a + from.length()).trim();
        return s.substring(a + from.length(), b).trim();
    }

    private static String extractBetween(String s, String from, String to) {
        int a = s.indexOf(from);
        if (a < 0) return null;
        int b = s.indexOf(to, a + from.length());
        if (b < 0) return null;
        return s.substring(a + from.length(), b);
    }
}
