package com.regnosys.rosetta.generator.java;

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.ast.supporting.REnumValue;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.ast.types.REnumeration;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.supporting.RTypeCall;
import com.regnosys.rosetta.generator.AggregateGenerationException;
import com.regnosys.rosetta.generator.GenerationException;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.generator.java.util.ModelGeneratorUtil;
import com.regnosys.rosetta.symbols.RWorkspace;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JavaCodeGeneratorTest {

    // === Orchestrator basics ==================================================

    @Test void empty_workspace_produces_no_output() {
        var model = makeModel("com.example");
        var result = RWorkspace.build(List.of(model));
        var codegen = new JavaCodeGenerator(result.workspace());
        var output = codegen.generate();
        assertTrue(output.isEmpty());
    }

    @Test void ignored_file_skipped() {
        var model = makeModel("com.rosetta.model");
        model.setSourceRange(SourceRange.of5Arg("basictypes.rosetta", 1, 1, 10, 1));
        var result = RWorkspace.build(List.of(model));
        var codegen = new JavaCodeGenerator(result.workspace());
        assertTrue(codegen.getGeneratorModel().isIgnoredFile(model));
    }

    @Test void generator_model_accessible() {
        var model = makeModel("com.example");
        var result = RWorkspace.build(List.of(model));
        var codegen = new JavaCodeGenerator(result.workspace());
        assertNotNull(codegen.getGeneratorModel());
        assertEquals(1, codegen.getGeneratorModel().files().size());
    }

    // P2.1.1 R4 F15 — Lock the {@code shouldGenerate} composition contract:
    // {@code shouldGenerate = !isIgnoredFile && emissionFilter.test}. Both
    // gates must be consulted; either returning the rejecting value must
    // suppress emission. The default constructor's accept-all predicate
    // must NOT override the IGNORED_FILES gate.
    @Test void should_generate_composes_ignored_files_with_emission_filter() {
        var ignored = makeModel("com.rosetta.model");
        ignored.setSourceRange(SourceRange.of5Arg("basictypes.rosetta", 1, 1, 10, 1));
        var regular = makeModel("com.example");
        var result = RWorkspace.build(List.of(ignored, regular));
        var workspace = result.workspace();

        // Default (accept-all) ctor: IGNORED suppresses, regular emits.
        var gmDefault = new GeneratorModel(workspace);
        assertFalse(gmDefault.shouldGenerate(ignored),
                "Default-ctor accept-all filter must NOT override IGNORED_FILES gate; "
                        + "basictypes.rosetta-shape models stay non-emitting");
        assertTrue(gmDefault.shouldGenerate(regular),
                "Default-ctor accept-all filter must let regular models emit");

        // Custom filter rejects the regular model.
        var gmReject = new GeneratorModel(workspace, m -> false);
        assertFalse(gmReject.shouldGenerate(ignored),
                "Reject-all filter AND ignored-file together still false");
        assertFalse(gmReject.shouldGenerate(regular),
                "Reject-all filter suppresses non-ignored models — emissionFilter gate must apply");

        // Custom filter rejects only com.example. IGNORED still suppressed.
        var gmNamespaceFilter = new GeneratorModel(workspace,
                m -> !"com.example".equals(m.namespace()));
        assertFalse(gmNamespaceFilter.shouldGenerate(ignored),
                "Per-namespace filter that accepts ignored-namespace-model is still gated by IGNORED_FILES");
        assertFalse(gmNamespaceFilter.shouldGenerate(regular),
                "Per-namespace filter rejects com.example — regular model suppressed");
    }

    // === GeneratorModel integration ==========================================

    @Test void symbol_id_computation() {
        var model = makeModel("com.example.model");
        var dt = new RDataType(); dt.setName("Trade");
        model.rootElements().add(dt); dt.setParent(model);

        var result = RWorkspace.build(List.of(model));
        var gm = new GeneratorModel(result.workspace());
        var id = gm.symbolId(dt);
        assertEquals("com.example.model.Trade", id.getQualifiedName().withDots());
    }

    @Test void all_attributes_across_inheritance() {
        var model = makeModel("com.example");
        var parent = new RDataType(); parent.setName("Parent");
        addAttribute(parent, "id", "string");
        model.rootElements().add(parent); parent.setParent(model);

        var child = new RDataType(); child.setName("Child");
        // Use superTypeName so GlobalResolutionPass resolves it via RWorkspace.build()
        child.setSuperTypeName("Parent");
        addAttribute(child, "value", "number");
        model.rootElements().add(child); child.setParent(model);

        var result = RWorkspace.build(List.of(model));
        var gm = new GeneratorModel(result.workspace());
        // Find the resolved child from the workspace (nodes are frozen + attached post-build)
        var resolvedChild = result.workspace().namespace("com.example")
                .flatMap(ns -> ns.lookup("Child"))
                .filter(RDataType.class::isInstance)
                .map(RDataType.class::cast)
                .orElseThrow(() -> new AssertionError("Child not found in workspace"));
        var attrs = new java.util.ArrayList<>(gm.allAttributes(resolvedChild));
        assertEquals(2, attrs.size());
        assertEquals("id", attrs.get(0).name());
        assertEquals("value", attrs.get(1).name());
    }

    // === ModelGeneratorUtil ===================================================

    @Test void javadoc_with_definition_and_version() {
        var util = new ModelGeneratorUtil();
        String jd = util.javadoc("A trade between parties.", List.of(), "1.0.0");
        assertNotNull(jd);
        assertTrue(jd.contains("A trade between parties."));
        assertTrue(jd.contains("@version 1.0.0"));
        assertTrue(jd.startsWith("/**"));
        assertTrue(jd.endsWith("*/"));
    }

    @Test void javadoc_null_when_all_empty() {
        var util = new ModelGeneratorUtil();
        assertNull(util.javadoc(null, List.of(), null));
    }

    // P2.1.1 R4 F13 — Lock the empty-string-vs-null distinction.
    // A null definition + no docRefs + no version returns null (no block emitted);
    // an empty-string definition still emits an empty /** */ block to preserve
    // upstream-parity (rune-dsl Xtend emitted the empty block in that case;
    // collapsing absent + explicit-empty caused Cluster A.4 — see P2.1.1 T3 fix
    // at the development audit "cluster-a-fix-design" § 9.3 + § 9.5).
    @Test void javadoc_empty_definition_emits_empty_block() {
        var util = new ModelGeneratorUtil();
        String jd = util.javadoc("", List.of(), null);
        assertNotNull(jd, "empty-string definition must emit a /** */ block, not null");
        assertEquals("/**\n */", jd,
                "empty-string definition must emit a minimal empty javadoc block; "
                        + "collapsing this with null-definition recreates Cluster A.4");
    }

    @Test void javadoc_version_only() {
        var util = new ModelGeneratorUtil();
        String jd = util.javadoc(null, List.of(), "2.0");
        assertNotNull(jd);
        assertTrue(jd.contains("@version 2.0"));
    }

    @Test void empty_javadoc_with_version() {
        var util = new ModelGeneratorUtil();
        String jd = util.emptyJavadocWithVersion("1.0.0");
        assertTrue(jd.contains("@version 1.0.0"));
    }

    @Test void html_escaping() {
        assertEquals("&amp;", ModelGeneratorUtil.escapeHtml("&"));
        assertEquals("&lt;", ModelGeneratorUtil.escapeHtml("<"));
        assertEquals("&gt;", ModelGeneratorUtil.escapeHtml(">"));
        assertEquals("&quot;", ModelGeneratorUtil.escapeHtml("\""));
        assertEquals("&#39;", ModelGeneratorUtil.escapeHtml("'"));
        assertEquals("hello", ModelGeneratorUtil.escapeHtml("hello"));
        assertEquals("Cote d&#39;Ivoire", ModelGeneratorUtil.escapeHtml("Cote d'Ivoire"));
    }

    // === RosettaJavaPackages ==================================================

    @Test void default_namespace() {
        var packages = new RosettaJavaPackages();
        assertEquals("com.rosetta.model", packages.defaultNamespace().withDots());
    }

    @Test void default_lib() {
        var packages = new RosettaJavaPackages();
        assertEquals("com.rosetta.model.lib", packages.defaultLib().withDots());
    }

    @Test void default_lib_functions() {
        var packages = new RosettaJavaPackages();
        assertEquals("com.rosetta.model.lib.functions",
                packages.defaultLibFunctions().withDots());
    }

    // === rune-parser regression ===============================================

    @Test void rune_parser_types_accessible() {
        // Verify that rune-parser types are on the classpath
        assertNotNull(RDataType.class);
        assertNotNull(REnumeration.class);
        assertNotNull(RFunction.class);
        assertNotNull(RWorkspace.class);
    }

    // === Error-propagation regression (Copilot R10 F2 2026-05-04) =============
    //
    // Locks the production wiring at JavaCodeGenerator.java:128 — the
    // `allErrors.addAll(functionGenerator.generateWithErrors(output))`
    // aggregation path added at R9. Without these tests the path can regress
    // silently (e.g. a future refactor reverts to discard-return) while the
    // existing matrix tests still pass against converged corpora.

    @Test void function_generator_no_errors_succeeds() {
        var model = makeModel("com.example");
        var result = RWorkspace.build(List.of(model));
        var workspace = result.workspace();
        var gm = new GeneratorModel(workspace);

        var stubFuncGen = new FunctionGenerator(gm, TYPE_TRANSLATOR, TYPE_UTIL) {
            @Override
            public List<GenerationException> generateWithErrors(Map<String, String> output) {
                return List.of();
            }
        };

        var codegen = new JavaCodeGenerator(workspace, List.of(), null, stubFuncGen);
        // Should not throw on empty error list.
        var output = codegen.generate();
        assertNotNull(output);
    }

    @Test void function_generator_single_error_throws_directly() {
        var model = makeModel("com.example");
        var result = RWorkspace.build(List.of(model));
        var workspace = result.workspace();
        var gm = new GeneratorModel(workspace);

        var sentinel = new GenerationException("synthetic R10-F2 single failure",
                "test://Synthetic.rosetta", null);

        var stubFuncGen = new FunctionGenerator(gm, TYPE_TRANSLATOR, TYPE_UTIL) {
            @Override
            public List<GenerationException> generateWithErrors(Map<String, String> output) {
                return List.of(sentinel);
            }
        };

        var codegen = new JavaCodeGenerator(workspace, List.of(), null, stubFuncGen);
        var thrown = assertThrows(GenerationException.class, codegen::generate);
        // Single-error path at JavaCodeGenerator.java:130 throws the exception directly
        // (not wrapped) — verify identity, not just message equality.
        assertSame(sentinel, thrown);
    }

    @Test void function_generator_multiple_errors_aggregate() {
        var model = makeModel("com.example");
        var result = RWorkspace.build(List.of(model));
        var workspace = result.workspace();
        var gm = new GeneratorModel(workspace);

        var err1 = new GenerationException("synthetic R10-F2 failure 1", null, null);
        var err2 = new GenerationException("synthetic R10-F2 failure 2", null, null);

        var stubFuncGen = new FunctionGenerator(gm, TYPE_TRANSLATOR, TYPE_UTIL) {
            @Override
            public List<GenerationException> generateWithErrors(Map<String, String> output) {
                return List.of(err1, err2);
            }
        };

        var codegen = new JavaCodeGenerator(workspace, List.of(), null, stubFuncGen);
        var thrown = assertThrows(AggregateGenerationException.class, codegen::generate);
        assertTrue(thrown.getMessage().contains("Multiple errors during Java code generation"),
                "AggregateGenerationException message should announce multi-error wrap; got: "
                        + thrown.getMessage());
    }

    // === Production-routes-through-canonical-method regression (PR-1.5 Copilot R5 F3+F5) ====
    //
    // Locks the @apiNote contract on both FunctionGenerator.generate(Map) and
    // FunctionGenerator.generateWithErrors(Map): "overriding the deprecated void
    // generate(Map) overload does NOT influence production code generation;
    // production calls generateWithErrors(Map)". Without this test, a future
    // refactor that accidentally routed JavaCodeGenerator.generate() back through
    // the deprecated overload would leave the existing R10 F2 tests green (they
    // stub generateWithErrors) AND would silently break the @apiNote claim.

    @Test void production_calls_generateWithErrors_not_deprecated_generate_overload() {
        var model = makeModel("com.example");
        var result = RWorkspace.build(List.of(model));
        var workspace = result.workspace();
        var gm = new GeneratorModel(workspace);

        var generateOverloadInvoked = new boolean[]{false};
        var generateWithErrorsInvoked = new boolean[]{false};

        var stubFuncGen = new FunctionGenerator(gm, TYPE_TRANSLATOR, TYPE_UTIL) {
            @Override
            @SuppressWarnings("deprecation")
            public void generate(Map<String, String> output) {
                generateOverloadInvoked[0] = true;
                // Intentionally do NOT delegate to super — production must NOT
                // route through this method, so the test must detect any future
                // refactor that does (which would change generateOverloadInvoked
                // to true).
            }

            @Override
            public List<GenerationException> generateWithErrors(Map<String, String> output) {
                generateWithErrorsInvoked[0] = true;
                return List.of();
            }
        };

        var codegen = new JavaCodeGenerator(workspace, List.of(), null, stubFuncGen);
        codegen.generate();

        assertFalse(generateOverloadInvoked[0],
                "Production must NOT call the deprecated generate(Map) overload — "
                        + "overriding it must have no effect on production codegen "
                        + "(@apiNote contract on generate(Map) + generateWithErrors(Map); "
                        + "PR-1.5 Copilot R5 F3+F5 lock 2026-05-04).");
        assertTrue(generateWithErrorsInvoked[0],
                "Production must call generateWithErrors(Map) — the canonical override point.");
    }

    private static final JavaTypeUtil TYPE_UTIL = new JavaTypeUtil();
    private static final JavaTypeTranslator TYPE_TRANSLATOR = new JavaTypeTranslator(TYPE_UTIL);

    // === Helpers ==============================================================

    private RModel makeModel(String namespace) {
        var model = new RModel();
        model.setNamespace(namespace);
        return model;
    }

    private void addAttribute(RDataType dt, String name, String typeName) {
        var attr = new RAttribute();
        attr.setName(name);
        var tc = new RTypeCall(); tc.setTypeName(typeName);
        attr.setTypeCall(tc);
        dt.attributes().add(attr);
    }
}
