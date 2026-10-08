package com.regnosys.rosetta.symbols;

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.RRootElement;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.symbols.diagnostics.DiagnosticCategory;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ImportResolutionTest extends BaseSymbolsTest {

    @Test
    void wildcard_import_makes_other_namespace_visible() {
        RLinkingResult result = parseAndLink(
            "namespace cdm.foo\ntype Foo:\n    a string (1..1)\n",
            "namespace test\nimport cdm.foo.*\ntype Bar:\n    a string (1..1)\n");

        RModel testFile = result.workspace().files().stream()
            .filter(m -> "test".equals(m.namespace()))
            .findFirst().orElseThrow();
        RFileScope scope = result.workspace().fileScope(testFile).orElseThrow();

        Optional<RRootElement> foo = scope.lookup("Foo");
        assertTrue(foo.isPresent(), "wildcard import should make Foo visible");
    }

    @Test
    void own_namespace_is_visible_without_import() {
        RLinkingResult result = parseAndLink("""
                namespace test
                type Foo:
                    a string (1..1)
                type Bar:
                    b string (1..1)
                """);
        RModel m = result.workspace().files().get(0);
        RFileScope scope = result.workspace().fileScope(m).orElseThrow();

        assertTrue(scope.lookup("Foo").isPresent());
        assertTrue(scope.lookup("Bar").isPresent());
    }

    @Test
    void unresolved_import_emits_diagnostic() {
        RLinkingResult result = parseAndLink("""
                namespace test
                import cdm.nonexistent.*
                type Foo:
                    a string (1..1)
                """);
        assertTrue(result.linkingDiagnostics().stream()
            .anyMatch(d -> d.category() == DiagnosticCategory.IMPORT_UNRESOLVED
                        && d.unresolvedName().equals("cdm.nonexistent")),
            "should emit IMPORT_UNRESOLVED for cdm.nonexistent");
    }

    @Test
    void wildcard_import_to_parentOnly_namespace_does_not_error() {
        // Phase X1 Gap G1 — `a.b.c` carries content; `a.b` is parent-only
        // (no direct declarations, only the registered sub-namespace `a.b.c`).
        // `import a.b.*` is legitimate (it brings the sub-namespace into scope
        // for qualified refs like `c.Foo`), so it must NOT raise
        // IMPORT_UNRESOLVED. Mirrors the real DRR shape `import
        // drr.base.qualification.*` where only `.event` / `.product` exist.
        RLinkingResult result = parseAndLink(
            "namespace a.b.c\ntype Foo:\n    x string (1..1)\n",
            "namespace test\nimport a.b.*\ntype Bar:\n    f c.Foo (1..1)\n");

        assertFalse(result.linkingDiagnostics().stream()
            .anyMatch(d -> d.category() == DiagnosticCategory.IMPORT_UNRESOLVED
                        && d.unresolvedName().equals("a.b")),
            "parent-only wildcard import a.b.* should NOT raise IMPORT_UNRESOLVED");

        RModel testFile = result.workspace().files().stream()
            .filter(m -> "test".equals(m.namespace()))
            .findFirst().orElseThrow();
        RFileScope scope = result.workspace().fileScope(testFile).orElseThrow();
        assertTrue(scope.imports().stream()
                .anyMatch(i -> i.importedNamespace().equals("a.b") && i.isWildcard()),
            "parent-only wildcard import a.b.* should be registered in the file scope");

        // End-to-end: the qualified sub-namespace reference c.Foo must resolve
        // to the sub-namespace's type (the actual point of Gap G1).
        RAttribute f = findType(testFile, "Bar").attributes().get(0);
        assertTrue(f.typeCall().referencedType().isPresent(),
            "Bar.f -> c.Foo should resolve via the parent-only wildcard import");
        assertInstanceOf(RDataType.class, f.typeCall().referencedType().get());
    }

    @Test
    void parentOnly_wildcard_provider_change_surfaces_importer_as_dependent() {
        // Gap G1 reverse-dependency lock: a change to the contributing
        // sub-namespace file (a.b.c) must surface the importer (test) via
        // getDependents, even though no file declares the parent `a.b`. The
        // dependency edge is keyed by the real declaring sub-namespace.
        RLinkingResult result = parseAndLink(
            "namespace a.b.c\ntype Foo:\n    x string (1..1)\n",
            "namespace test\nimport a.b.*\ntype Bar:\n    f c.Foo (1..1)\n");

        RModel providerFile = result.workspace().files().stream()
            .filter(m -> "a.b.c".equals(m.namespace()))
            .findFirst().orElseThrow();
        java.util.List<RModel> dependents = result.workspace().getDependents(providerFile);
        assertTrue(dependents.stream().anyMatch(m -> "test".equals(m.namespace())),
            "importer of parent-only wildcard a.b.* should be a dependent of provider a.b.c");
    }

    @Test
    void multilevel_parentOnly_wildcard_import_resolves() {
        // The relaxation is depth-agnostic: `import a.*` where only the
        // grandchild `a.b.c` carries content (no `a` and no `a.b`) must still
        // resolve `b.c.Foo`.
        RLinkingResult result = parseAndLink(
            "namespace a.b.c\ntype Foo:\n    x string (1..1)\n",
            "namespace test\nimport a.*\ntype Bar:\n    f b.c.Foo (1..1)\n");

        assertFalse(result.linkingDiagnostics().stream()
            .anyMatch(d -> d.category() == DiagnosticCategory.IMPORT_UNRESOLVED
                        && d.unresolvedName().equals("a")),
            "multi-level parent-only wildcard import a.* should NOT raise IMPORT_UNRESOLVED");
        RModel testFile = result.workspace().files().stream()
            .filter(m -> "test".equals(m.namespace()))
            .findFirst().orElseThrow();
        RAttribute f = findType(testFile, "Bar").attributes().get(0);
        assertTrue(f.typeCall().referencedType().isPresent(),
            "Bar.f -> b.c.Foo should resolve via the multi-level parent-only wildcard import");
    }

    @Test
    void wildcard_import_to_genuinely_missing_namespace_still_errors() {
        // The Gap G1 relaxation applies ONLY to parent-only namespaces (a
        // prefix of a registered namespace). A wildcard import to a namespace
        // that is neither registered nor a parent of one must still error.
        RLinkingResult result = parseAndLink("""
                namespace test
                import totally.bogus.ns.*
                type Foo:
                    a string (1..1)
                """);
        assertTrue(result.linkingDiagnostics().stream()
            .anyMatch(d -> d.category() == DiagnosticCategory.IMPORT_UNRESOLVED
                        && d.unresolvedName().equals("totally.bogus.ns")),
            "non-parent missing wildcard import should still emit IMPORT_UNRESOLVED");
    }

    @Test
    void dependency_index_powers_getDependents() {
        RLinkingResult result = parseAndLink(
            "namespace cdm.foo\ntype Foo:\n    a string (1..1)\n",
            "namespace test1\nimport cdm.foo.*\ntype A:\n",
            "namespace test2\nimport cdm.foo.*\ntype B:\n");

        RModel cdmFoo = result.workspace().files().stream()
            .filter(m -> "cdm.foo".equals(m.namespace()))
            .findFirst().orElseThrow();

        java.util.List<RModel> dependents = result.workspace().getDependents(cdmFoo);
        assertEquals(2, dependents.size(),
            "both test1 and test2 should be dependents of cdm.foo");
    }
}
