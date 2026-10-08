package com.regnosys.rosetta.validation;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.symbols.RWorkspace;
import com.regnosys.rosetta.symbols.diagnostics.RDiagnostic;
import com.regnosys.rosetta.symbols.diagnostics.Severity;
import com.regnosys.rosetta.symbols.diagnostics.ValidationDiagnostic;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * M6 T21 — Negative corpus: .rosetta inputs with deliberate errors.
 * Each test verifies the validation pipeline detects the expected error.
 */
class NegativeCorpusValidationTest {

    // === Naming violations ====================================================

    @Test void lowercase_type_produces_warning() {
        var ws = build("namespace test\ntype foo:\n");
        var vDiags = ws.validationDiagnostics();
        assertTrue(vDiags.stream().anyMatch(d ->
            d.severity() == Severity.WARNING &&
            d.issueCode() == ValidationIssueCode.INVALID_CASE));
    }

    @Test void uppercase_attribute_produces_warning() {
        var ws = build("namespace test\ntype Foo:\n  Bar int (1..1)\n");
        var vDiags = ws.validationDiagnostics();
        assertTrue(vDiags.stream().anyMatch(d ->
            d.issueCode() == ValidationIssueCode.INVALID_CASE));
    }

    // === Duplicate attributes ================================================

    @Test void duplicate_attribute_produces_error() {
        var ws = build("namespace test\ntype Foo:\n  bar int (1..1)\n  bar string (0..1)\n");
        var vDiags = ws.validationDiagnostics();
        assertTrue(vDiags.stream().anyMatch(d ->
            d.severity() == Severity.ERROR &&
            d.issueCode() == ValidationIssueCode.DUPLICATE_ATTRIBUTE));
    }

    // === Duplicate enum values ===============================================

    @Test void duplicate_enum_value_produces_error() {
        var ws = build("namespace test\nenum Color:\n  Red\n  Red\n");
        var vDiags = ws.validationDiagnostics();
        assertTrue(vDiags.stream().anyMatch(d ->
            d.issueCode() == ValidationIssueCode.DUPLICATE_ENUM_VALUE));
    }

    // === Duplicate imports ===================================================

    @Test void duplicate_import_produces_warning() {
        var ws = build("namespace test\nimport foo.*\nimport foo.*\ntype Bar:\n");
        var vDiags = ws.validationDiagnostics();
        assertTrue(vDiags.stream().anyMatch(d ->
            d.issueCode() == ValidationIssueCode.DUPLICATE_IMPORT));
    }

    // === Valid input produces no validation errors ============================

    @Test void valid_type_no_validation_errors() {
        var ws = build("namespace test\ntype Foo:\n  bar int (1..1)\n");
        var vDiags = ws.validationDiagnostics();
        long errors = vDiags.stream().filter(d -> d.severity() == Severity.ERROR).count();
        assertEquals(0, errors, "Valid input should produce no validation errors");
    }

    @Test void valid_enum_no_validation_errors() {
        var ws = build("namespace test\nenum Color:\n  Red\n  Blue\n");
        var vDiags = ws.validationDiagnostics();
        long errors = vDiags.stream().filter(d -> d.severity() == Severity.ERROR).count();
        assertEquals(0, errors);
    }

    private RWorkspace build(String source) {
        RModel model = AstBuilder.buildFromString(source, "negative.rosetta");
        return RWorkspace.build(List.of(model)).workspace();
    }
}
