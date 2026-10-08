package com.regnosys.rosetta.symbols.diagnostics;

import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.parser.diagnostics.RuneErrorCode;
import com.regnosys.rosetta.validation.ValidationIssueCode;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RDiagnosticDefaultCodeTest {

    @Test
    void linkingDiagnosticInheritsEmptyCodeByDefault() {
        RDiagnostic d = new LinkingDiagnostic(
            Severity.ERROR, DiagnosticCategory.TYPE_NOT_FOUND, SourceRange.NONE,
            "Foo", "Type Foo not found", java.util.List.of());
        Optional<RuneErrorCode> code = d.code();
        assertTrue(code.isEmpty(),
            "LinkingDiagnostic must inherit Optional.empty() from RDiagnostic.code() default");
    }

    @Test
    void validationDiagnosticInheritsEmptyCodeByDefault() {
        RDiagnostic d = new ValidationDiagnostic(
            Severity.WARNING, SourceRange.NONE, "msg",
            ValidationIssueCode.TYPE_ERROR);
        assertTrue(d.code().isEmpty(),
            "ValidationDiagnostic must inherit Optional.empty() from RDiagnostic.code() default");
    }
}
