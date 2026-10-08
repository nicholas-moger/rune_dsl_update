package com.regnosys.rosetta.validation.validators;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.model.RImport;
import com.regnosys.rosetta.validation.ValidationCollector;
import com.regnosys.rosetta.validation.ValidationIssueCode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ImportValidatorTest {

    private final ImportValidator validator = new ImportValidator(Map.of());

    @Test void no_imports_passes() {
        var c = new ValidationCollector();
        validator.validateModel(makeModel("namespace test\ntype Foo:\n"), c);
        assertTrue(c.isEmpty());
    }

    // PR #455: unreferenced imports now warn (upstream checkImport/findUnused —
    // the released `Unused import <ns>` bytes, star included for wildcards).
    @Test void unreferenced_imports_warn_unused() {
        var c = new ValidationCollector();
        validator.validateModel(makeModel(
            "namespace test\nimport foo.*\nimport bar.*\ntype Foo:\n"), c);
        assertEquals(2, c.toList().stream()
            .filter(d -> d.issueCode() == ValidationIssueCode.UNUSED_IMPORT).count());
        assertTrue(c.toList().stream().anyMatch(d ->
            d.message().equals("Unused import foo.*")));
        assertTrue(c.toList().stream().anyMatch(d ->
            d.message().equals("Unused import bar.*")));
    }

    @Test void duplicate_imports_warn() {
        var c = new ValidationCollector();
        validator.validateModel(makeModel(
            "namespace test\nimport foo.*\nimport foo.*\ntype Foo:\n"), c);
        assertTrue(c.toList().stream().anyMatch(d ->
            d.issueCode() == ValidationIssueCode.DUPLICATE_IMPORT
                && d.message().equals("Duplicate import foo.*")));
    }

    @Test void duplicate_non_wildcard_imports_warn() {
        var c = new ValidationCollector();
        var model = new RModel();
        model.setNamespace("test");
        var imp1 = new RImport(); imp1.setQualifiedName("foo.Bar"); imp1.setWildcard(false);
        var imp2 = new RImport(); imp2.setQualifiedName("foo.Bar"); imp2.setWildcard(false);
        model.imports().add(imp1);
        model.imports().add(imp2);
        validator.validateModel(model, c);
        assertTrue(c.toList().stream().anyMatch(d ->
            d.issueCode() == ValidationIssueCode.DUPLICATE_IMPORT));
    }

    @Test void wildcard_and_specific_not_duplicate() {
        var c = new ValidationCollector();
        var model = new RModel();
        model.setNamespace("test");
        var imp1 = new RImport(); imp1.setQualifiedName("foo"); imp1.setWildcard(true);
        var imp2 = new RImport(); imp2.setQualifiedName("foo.Bar"); imp2.setWildcard(false);
        model.imports().add(imp1);
        model.imports().add(imp2);
        validator.validateModel(model, c);
        assertTrue(c.toList().stream().noneMatch(d ->
            d.issueCode() == ValidationIssueCode.DUPLICATE_IMPORT));
    }

    private RModel makeModel(String source) {
        return AstBuilder.buildFromString(source, "import-test.rosetta");
    }
}
