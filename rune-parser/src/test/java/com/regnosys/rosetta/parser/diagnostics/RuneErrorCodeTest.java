package com.regnosys.rosetta.parser.diagnostics;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RuneErrorCodeTest {

    @Test
    void rune001UnexpectedToken() {
        RuneErrorCode code = new RuneErrorCode.UnexpectedToken(
            Optional.of("'='"), Optional.empty(), "raw");
        assertEquals("RUNE-001", code.code());
        assertEquals("docs/diagnostics/RUNE-001.md", code.docPath());
    }

    @Test
    void rune002MissingToken() {
        RuneErrorCode code = new RuneErrorCode.MissingToken("ID");
        assertEquals("RUNE-002", code.code());
        assertEquals("docs/diagnostics/RUNE-002.md", code.docPath());
    }

    @Test
    void rune003ExtraneousInput() {
        RuneErrorCode code = new RuneErrorCode.ExtraneousInput(":");
        assertEquals("RUNE-003", code.code());
    }

    @Test
    void rune004NoViableAlternative() {
        RuneErrorCode code = new RuneErrorCode.NoViableAlternative("expression");
        assertEquals("RUNE-004", code.code());
    }

    @Test
    void rune005FailedPredicate() {
        RuneErrorCode code = new RuneErrorCode.FailedPredicate("isLegalIdentifier");
        assertEquals("RUNE-005", code.code());
    }

    @Test
    void rune006GrammarAmbiguity() {
        RuneErrorCode code = new RuneErrorCode.GrammarAmbiguity("rule X", 0, 5);
        assertEquals("RUNE-006", code.code());
    }

    @Test
    void rune007ContextSensitiveParse() {
        RuneErrorCode code = new RuneErrorCode.ContextSensitiveParse("rule Y", 0, 5);
        assertEquals("RUNE-007", code.code());
    }

    @Test
    void rune008LexerNoViableAlternative() {
        RuneErrorCode code = new RuneErrorCode.LexerNoViableAlternative("@");
        assertEquals("RUNE-008", code.code());
    }

    @Test
    void rune009MismatchedInput() {
        RuneErrorCode code = new RuneErrorCode.MismatchedInput("'type'", "'func'");
        assertEquals("RUNE-009", code.code());
    }

    @Test
    void rune010UnexpectedEof() {
        RuneErrorCode code = new RuneErrorCode.UnexpectedEof("type body");
        assertEquals("RUNE-010", code.code());
    }

    @Test
    void rune011InvalidStringLiteral() {
        RuneErrorCode code = new RuneErrorCode.InvalidStringLiteral("\\q");
        assertEquals("RUNE-011", code.code());
    }

    @Test
    void rune012InvalidNumericLiteral() {
        RuneErrorCode code = new RuneErrorCode.InvalidNumericLiteral("0x");
        assertEquals("RUNE-012", code.code());
    }

    @Test
    void rune013UnclosedComment() {
        RuneErrorCode code = new RuneErrorCode.UnclosedComment(42);
        assertEquals("RUNE-013", code.code());
    }

    @Test
    void rune014DuplicateModifier() {
        RuneErrorCode code = new RuneErrorCode.DuplicateModifier("optional");
        assertEquals("RUNE-014", code.code());
    }

    @Test
    void rune015UnknownErrorPattern() {
        RuneErrorCode code = new RuneErrorCode.UnknownErrorPattern("raw msg");
        assertEquals("RUNE-015", code.code());
    }

    @Test
    void allRecordsAreSealedPermitted() {
        // Compile-time check: switch must be exhaustive on the sealed interface.
        // If a new variant is added without the switch updated, this won't compile.
        RuneErrorCode code = new RuneErrorCode.UnknownErrorPattern("test");
        String summary = switch (code) {
            case RuneErrorCode.UnexpectedToken u -> "001";
            case RuneErrorCode.MissingToken m -> "002";
            case RuneErrorCode.ExtraneousInput e -> "003";
            case RuneErrorCode.NoViableAlternative n -> "004";
            case RuneErrorCode.FailedPredicate f -> "005";
            case RuneErrorCode.GrammarAmbiguity g -> "006";
            case RuneErrorCode.ContextSensitiveParse c -> "007";
            case RuneErrorCode.LexerNoViableAlternative l -> "008";
            case RuneErrorCode.MismatchedInput mi -> "009";
            case RuneErrorCode.UnexpectedEof eof -> "010";
            case RuneErrorCode.InvalidStringLiteral i -> "011";
            case RuneErrorCode.InvalidNumericLiteral inl -> "012";
            case RuneErrorCode.UnclosedComment uc -> "013";
            case RuneErrorCode.DuplicateModifier dm -> "014";
            case RuneErrorCode.UnknownErrorPattern uk -> "015";
            case RuneErrorCode.InvalidRuneAnnotationArg ira -> "016";
            case RuneErrorCode.InvalidFileHeaderField ifh -> "017";
            case RuneErrorCode.InvalidRegulatoryReferenceArg irra -> "018";
            case RuneErrorCode.InvalidRuneAnnotationName iran -> "019";
        };
        assertEquals("015", summary);
    }

    @Test
    void rune016InvalidRuneAnnotationArgCode() {
        RuneErrorCode code = new RuneErrorCode.InvalidRuneAnnotationArg("missing equals");
        assertEquals("RUNE-016", code.code());
        assertEquals("docs/diagnostics/RUNE-016.md", code.docPath());
    }

    @Test
    void rune017InvalidFileHeaderFieldCode() {
        RuneErrorCode code = new RuneErrorCode.InvalidFileHeaderField("duplicate version");
        assertEquals("RUNE-017", code.code());
    }

    @Test
    void rune018InvalidRegulatoryReferenceArgCode() {
        RuneErrorCode code = new RuneErrorCode.InvalidRegulatoryReferenceArg("non-string value");
        assertEquals("RUNE-018", code.code());
    }

    @Test
    void rune019InvalidRuneAnnotationNameCode() {
        RuneErrorCode code = new RuneErrorCode.InvalidRuneAnnotationName("numeric prefix");
        assertEquals("RUNE-019", code.code());
    }
}
