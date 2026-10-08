package com.regnosys.rosetta.parser.diagnostics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link RuneErrorClassifier} covering the message-based
 * classification paths. Typed-exception paths (InputMismatchException,
 * NoViableAltException, FailedPredicateException, LexerNoViableAltException)
 * are exercised by {@code RuneDiagnosticListenerTest} via real ANTLR parsing
 * of broken fixtures (rune-parser does not have Mockito on its test classpath
 * and constructing ANTLR exceptions directly requires a full Parser instance).
 */
class RuneErrorClassifierTest {

    @Test
    void missingTokenMessageMapsToMissingToken() {
        // ANTLR's DefaultErrorStrategy reports missing tokens via syntaxError msg
        // without a typed exception; classify by message prefix.
        RuneErrorCode code = RuneErrorClassifier.classify(null, "missing ID at 'foo'", null);
        assertInstanceOf(RuneErrorCode.MissingToken.class, code);
        assertEquals("RUNE-002", code.code());
    }

    @Test
    void extraneousInputMessageMapsToExtraneousInput() {
        RuneErrorCode code = RuneErrorClassifier.classify(null, "extraneous input ':'", null);
        assertInstanceOf(RuneErrorCode.ExtraneousInput.class, code);
        assertEquals("RUNE-003", code.code());
    }

    @Test
    void mismatchedInputMessageMapsToMismatchedInput() {
        RuneErrorCode code = RuneErrorClassifier.classify(null,
            "mismatched input 'foo' expecting ID", null);
        assertInstanceOf(RuneErrorCode.MismatchedInput.class, code);
        assertEquals("RUNE-009", code.code());
    }

    @Test
    void mismatchedInputExtractsExpectedFromMessage() {
        // S-3 (independent reviewer): MismatchedInput.expected used to be hard-coded "?"
        // which discarded the structured payload. Now extracts via expectedFromMessage().
        RuneErrorCode code = RuneErrorClassifier.classify(null,
            "mismatched input 'foo' expecting ID", null);
        RuneErrorCode.MismatchedInput mi = (RuneErrorCode.MismatchedInput) code;
        assertEquals("ID", mi.expected());
        assertEquals("foo", mi.actual());
    }

    @Test
    void unrecognizedExceptionMapsToUnknownErrorPattern() {
        RuneErrorCode code = RuneErrorClassifier.classify(null, "totally novel error", null);
        assertInstanceOf(RuneErrorCode.UnknownErrorPattern.class, code);
        assertEquals("RUNE-015", code.code());
    }

    @Test
    void nullMessageMapsToUnknownWithSentinel() {
        RuneErrorCode code = RuneErrorClassifier.classify(null, null, null);
        assertInstanceOf(RuneErrorCode.UnknownErrorPattern.class, code);
        assertEquals("RUNE-015", code.code());
        // Sentinel message must not be null
        assertNotNull(((RuneErrorCode.UnknownErrorPattern) code).rawMessage());
    }

    @Test
    void missingTokenExtractsTokenName() {
        RuneErrorCode code = RuneErrorClassifier.classify(null, "missing ID at 'foo'", null);
        RuneErrorCode.MissingToken mt = (RuneErrorCode.MissingToken) code;
        assertEquals("ID", mt.tokenName());
    }

    @Test
    void extraneousInputExtractsText() {
        RuneErrorCode code = RuneErrorClassifier.classify(null, "extraneous input ':' expecting ID", null);
        RuneErrorCode.ExtraneousInput ei = (RuneErrorCode.ExtraneousInput) code;
        assertEquals(":", ei.text());
    }
}
