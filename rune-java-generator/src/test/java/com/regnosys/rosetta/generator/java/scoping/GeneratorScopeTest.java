package com.regnosys.rosetta.generator.java.scoping;

import com.regnosys.rosetta.generator.GeneratedIdentifier;
import com.rosetta.util.DottedPath;
import com.rosetta.util.types.JavaClass;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GeneratorScopeTest {

    // === Basic identifier creation ==========================================

    @Test void create_and_resolve_identifier() {
        var scope = new JavaStatementScope("test", null);
        var id = scope.createIdentifier("key", "foo");
        assertEquals("foo", id.getActualName());
    }

    @Test void unique_identifier_avoids_clash() {
        var scope = new JavaStatementScope("test", null);
        scope.createIdentifier("key1", "x");
        var id2 = scope.createUniqueIdentifier("x");
        // Second "x" gets a numeric suffix
        assertNotEquals("x", id2.getActualName());
        assertTrue(id2.getActualName().startsWith("x"));
    }

    @Test void duplicate_key_throws() {
        var scope = new JavaStatementScope("test", null);
        scope.createIdentifier("key", "foo");
        assertThrows(IllegalStateException.class,
                () -> scope.createIdentifier("key", "bar"));
    }

    // === Scope hierarchy ====================================================

    @Test void child_scope_sees_parent_identifiers() {
        var parent = new JavaStatementScope("parent", null);
        parent.createIdentifier("pKey", "parentVar");
        var child = new JavaStatementScope("child", parent);
        var id = child.getIdentifierOrThrow("pKey");
        assertEquals("parentVar", id.getActualName());
    }

    @Test void child_scope_unique_avoids_parent_names() {
        var parent = new JavaStatementScope("parent", null);
        parent.createIdentifier("k1", "x");
        var child = new JavaStatementScope("child", parent);
        var id = child.createUniqueIdentifier("x");
        // "x" is taken by parent, so child must escape
        assertNotEquals("x", id.getActualName());
    }

    // === Java class scope ===================================================

    @Test void class_scope_creates_method_scope() {
        var classScope = JavaClassScope.createAndRegisterIdentifier(
                JavaClass.from(java.util.List.class));
        var methodScope = classScope.createMethodScope("doSomething");
        assertNotNull(methodScope);
        var bodyScope = methodScope.getBodyScope();
        assertNotNull(bodyScope);
    }

    // === File scope: java.lang types auto-recognized ========================

    @Test void file_scope_recognizes_java_lang_types() {
        var fileScope = new JavaFileScope("Test.java", DottedPath.of("com", "example"));
        // String is in java.lang — should be recognized without explicit registration
        var id = fileScope.getIdentifier(JavaClass.from(String.class));
        assertTrue(id.isPresent());
        assertEquals("String", id.get().getActualName());
    }

    @Test void file_scope_java_lang_names_are_taken() {
        var fileScope = new JavaFileScope("Test.java", DottedPath.of("com", "example"));
        // "String" should be considered taken (it's in java.lang)
        assertTrue(fileScope.isNameTaken("String"));
    }

    // === Escaping reserved words =============================================

    @Test void reserved_word_escaped() {
        var scope = new JavaStatementScope("test", null);
        var id = scope.createIdentifier("k", "class");
        // "class" is not a valid identifier, should be escaped to "_class"
        assertEquals("_class", id.getActualName());
    }

    // === Key synonyms =======================================================

    @Test void key_synonym_resolves_to_same_identifier() {
        var scope = new JavaStatementScope("test", null);
        var id = scope.createIdentifier("original", "myVar");
        scope.createKeySynonym("alias", "original");
        var resolved = scope.getIdentifierOrThrow("alias");
        assertSame(id, resolved);
    }

    // === Package name escaping ==============================================

    @Test void package_name_escapes_keywords() {
        var pkg = JavaPackageName.escape(DottedPath.of("com", "default", "test"));
        // "default" is a keyword, should be escaped
        assertEquals("com._default.test", pkg.toString());
    }

    @Test void package_name_no_escaping_needed() {
        var pkg = JavaPackageName.escape(DottedPath.of("com", "example", "model"));
        assertEquals("com.example.model", pkg.toString());
    }

    // === Lambda scope =======================================================

    @Test void lambda_scope_isolates_variables() {
        var parent = new JavaStatementScope("body", null);
        parent.createIdentifier("k1", "item");
        var lambda = parent.lambdaScope();
        // Lambda can define its own "item" — different scope
        // But the parent name is taken, so lambda's "item" will be escaped
        var id = lambda.createUniqueIdentifier("item");
        assertNotEquals("item", id.getActualName());
    }

    // === disambiguate (Phase X1 rendering pivot: scope-faithful lambda names) ==

    @Test void disambiguate_untaken_name_unchanged() {
        var scope = new JavaStatementScope("test", null);
        // Nothing registered — a fresh name passes through verbatim.
        assertEquals("settlementTerms", scope.disambiguate("settlementTerms"));
    }

    @Test void disambiguate_taken_name_escaped() {
        var scope = new JavaStatementScope("test", null);
        scope.createIdentifier("input1", "businessEvent");
        // The lambda var clashes with the registered name -> escapeName prefix.
        assertEquals("_businessEvent", scope.disambiguate("businessEvent"));
    }

    @Test void disambiguate_child_scope_distinguishes_parent_collision() {
        // Mirrors the function-vs-rule lambda-naming divergence: the body scope
        // holds the input names; a fresh lambda sub-scope disambiguates against
        // them. A name that does NOT collide stays un-escaped (rule case:
        // implicit `input` vs lambda `trade`); a name that DOES collide is
        // escaped (function case: input `businessEvent` vs lambda `businessEvent`).
        var body = new JavaStatementScope("body", null);
        body.createIdentifier("ruleInput", "input");
        var lambda = body.lambdaScope();
        assertEquals("trade", lambda.disambiguate("trade"));
        assertEquals("_input", lambda.disambiguate("input"));
    }

    @Test void disambiguate_is_non_mutating() {
        var scope = new JavaStatementScope("test", null);
        scope.createIdentifier("k", "x");
        // Calling disambiguate must neither register a name nor close the scope.
        assertEquals("y", scope.disambiguate("y"));
        assertEquals("y", scope.disambiguate("y"), "repeat call must be stable");
        assertFalse(scope.isClosed(), "disambiguate must not close the scope");
        // The scope is still open for further registration.
        var id = scope.createIdentifier("k2", "z");
        assertEquals("z", id.getActualName());
    }

    @Test void disambiguate_invalid_identifier_escaped() {
        var scope = new JavaStatementScope("test", null);
        // "class" is a Java keyword -> not a valid identifier -> escaped.
        assertEquals("_class", scope.disambiguate("class"));
    }

    // === Closed scope throws on modification ================================

    @Test void closed_scope_rejects_new_identifiers() {
        var scope = new JavaStatementScope("test", null);
        scope.createIdentifier("k", "x");
        // Force close by resolving an actual name
        scope.getActualName(scope.getIdentifierOrThrow("k"));
        assertTrue(scope.isClosed());
        assertThrows(IllegalStateException.class,
                () -> scope.createIdentifier("k2", "y"));
    }
}
