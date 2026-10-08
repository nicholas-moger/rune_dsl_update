package com.regnosys.rosetta.validation;

import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.symbols.diagnostics.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * M6 T0 — tests for validation infrastructure: RDiagnostic sealed interface,
 * ValidationDiagnostic, ValidationCollector, Severity, ValidationIssueCode.
 */
class ValidationInfraTest {

    private static final SourceRange DUMMY = SourceRange.of5Arg("test.rosetta", 1, 0, 1, 10);

    // === RDiagnostic sealed interface ========================================

    @Test void linking_diagnostic_implements_rdiagnostic() {
        RDiagnostic d = new LinkingDiagnostic(
            Severity.ERROR, DiagnosticCategory.TYPE_NOT_FOUND,
            DUMMY, "Foo", "Type not found", List.of());
        assertInstanceOf(LinkingDiagnostic.class, d);
        assertEquals(Severity.ERROR, d.severity());
        assertEquals(DUMMY, d.range());
        assertEquals("Type not found", d.message());
    }

    @Test void validation_diagnostic_implements_rdiagnostic() {
        RDiagnostic d = new ValidationDiagnostic(
            Severity.WARNING, DUMMY, "Name should start with uppercase",
            ValidationIssueCode.INVALID_CASE);
        assertInstanceOf(ValidationDiagnostic.class, d);
        assertEquals(Severity.WARNING, d.severity());
        assertEquals(DUMMY, d.range());
    }

    @Test void sealed_switch_is_exhaustive() {
        RDiagnostic d = new ValidationDiagnostic(
            Severity.INFO, DUMMY, "info", ValidationIssueCode.INVALID_NAME);
        String result = switch (d) {
            case LinkingDiagnostic ld -> "linking:" + ld.category();
            case ValidationDiagnostic vd -> "validation:" + vd.issueCode();
            case com.regnosys.rosetta.symbols.diagnostics.ParserDiagnostic pd ->
                "parser:" + pd.errorCode().code();
        };
        assertEquals("validation:INVALID_NAME", result);
    }

    // === Severity ============================================================

    @Test void severity_has_three_values() {
        assertEquals(3, Severity.values().length);
        assertNotNull(Severity.ERROR);
        assertNotNull(Severity.WARNING);
        assertNotNull(Severity.INFO);
    }

    // === ValidationDiagnostic ================================================

    @Test void validation_diagnostic_rejects_nulls() {
        assertThrows(NullPointerException.class, () ->
            new ValidationDiagnostic(null, DUMMY, "msg", ValidationIssueCode.TYPE_ERROR));
        assertThrows(NullPointerException.class, () ->
            new ValidationDiagnostic(Severity.ERROR, null, "msg", ValidationIssueCode.TYPE_ERROR));
    }

    @Test void validation_diagnostic_equality() {
        var a = new ValidationDiagnostic(Severity.ERROR, DUMMY, "msg", ValidationIssueCode.TYPE_ERROR);
        var b = new ValidationDiagnostic(Severity.ERROR, DUMMY, "msg", ValidationIssueCode.TYPE_ERROR);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    // === ValidationCollector ==================================================

    @Test void collector_starts_empty() {
        var c = new ValidationCollector();
        assertTrue(c.isEmpty());
        assertEquals(0, c.size());
    }

    @Test void collector_collects_errors() {
        var c = new ValidationCollector();
        c.error(DUMMY, "type error", ValidationIssueCode.TYPE_ERROR);
        assertFalse(c.isEmpty());
        assertEquals(1, c.size());
        assertEquals(Severity.ERROR, c.toList().get(0).severity());
    }

    @Test void collector_collects_warnings() {
        var c = new ValidationCollector();
        c.warning(DUMMY, "naming", ValidationIssueCode.INVALID_CASE);
        assertEquals(Severity.WARNING, c.toList().get(0).severity());
    }

    @Test void collector_collects_info() {
        var c = new ValidationCollector();
        c.info(DUMMY, "hint", ValidationIssueCode.INVALID_NAME);
        assertEquals(Severity.INFO, c.toList().get(0).severity());
    }

    @Test void collector_returns_immutable_list() {
        var c = new ValidationCollector();
        c.error(DUMMY, "msg", ValidationIssueCode.TYPE_ERROR);
        var list = c.toList();
        assertThrows(UnsupportedOperationException.class, () -> list.add(null));
    }

    // === ValidationPass ======================================================

    @Test void validation_pass_runs_all_validators() {
        var model = com.regnosys.rosetta.ast.builder.AstBuilder.buildFromString(
            "namespace \"test\"\ntype Foo:\n", "vp-test.rosetta");
        var collector = new ValidationCollector();

        // Dummy validator that counts invocations
        var count = new int[]{0};
        Validator counter = (elem, c) -> count[0]++;

        var pass = new ValidationPass(java.util.List.of(counter), null);
        pass.run(java.util.List.of(model), collector);
        assertEquals(1, count[0]); // one root element (Foo)
    }

    @Test void validation_pass_empty_model() {
        var model = com.regnosys.rosetta.ast.builder.AstBuilder.buildFromString(
            "namespace \"test\"\n", "vp-empty.rosetta");
        var collector = new ValidationCollector();
        var pass = new ValidationPass(java.util.List.of(), null);
        pass.run(java.util.List.of(model), collector);
        assertTrue(collector.isEmpty());
    }

    // === WarningSuppressionHelper ============================================

    @Test void suppression_not_present_returns_false() {
        var dt = new com.regnosys.rosetta.ast.types.RDataType();
        dt.setName("Foo");
        assertFalse(WarningSuppressionHelper.isSuppressed(dt, "capitalisation"));
    }

    // === ValidationIssueCode =================================================

    @Test void issue_codes_exist() {
        assertTrue(ValidationIssueCode.values().length >= 19);
        assertNotNull(ValidationIssueCode.UNUSED_IMPORT);
        assertNotNull(ValidationIssueCode.MANDATORY_THEN);
        assertNotNull(ValidationIssueCode.MISSING_MANDATORY_CONSTRUCTOR_ARGUMENT);
    }
}
