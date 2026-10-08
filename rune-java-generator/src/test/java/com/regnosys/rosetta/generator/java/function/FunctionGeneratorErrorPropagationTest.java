package com.regnosys.rosetta.generator.java.function;

import com.regnosys.rosetta.ast.functions.RDispatch;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.GenerationException;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.symbols.RWorkspace;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Locks the FunctionGenerator catch-block identity-preservation contract added at
 * Copilot R11 F1+F3 2026-05-04 (PR-1.5) and refined at PR-1.5 Copilot R1 F1+F3
 * 2026-05-04 with a payload-aware guard.
 *
 * <p>Production semantic: when {@code generateStandardFunction} (Phase 2) or
 * {@code generateDispatchFunction} (Phase 3) throws a {@link GenerationException},
 * the catch block preserves the original instance (identity intact) <em>only
 * when</em> it carries diagnostic payload — i.e. {@code resourceUri} or
 * {@code context} is non-null. Bare {@code GenerationException}s with both
 * payload fields null (e.g. from {@code TemplateRenderer}) get wrapped in a
 * fresh {@code GenerationException} that adds this generator's per-function
 * attribution prefix; the original is preserved as cause via the 4-arg ctor.
 * Generic (non-{@code GenerationException}) exceptions are always wrapped with
 * cause.
 *
 * <p>Eight tests cover the (Phase 2 × {preserve-resourceUri, preserve-context-only,
 * wrap-bare, wrap-runtime}) × (Phase 3 × same) matrix, where the four
 * R11/R1/R2 scenarios per phase are:
 * <ul>
 *   <li><b>preserve-identity (resourceUri set):</b> identity-preserve when
 *       {@code resourceUri != null}, asserted via {@code assertSame}.</li>
 *   <li><b>preserve-identity (context set, resourceUri null):</b> identity-preserve
 *       when {@code context != null} alone — locks the OR guard's right-hand
 *       side (PR-1.5 Copilot R2 F3 sweep 2026-05-04).</li>
 *   <li><b>wrap-bare (no payload):</b> wrap with attribution + cause chain when
 *       both payload fields null — locks PR-1.5 Copilot R1 F1+F3 fix.</li>
 *   <li><b>wrap-runtime (non-GE):</b> wrap generic exceptions with cause —
 *       unchanged from R11 F1+F3.</li>
 * </ul>
 *
 * <p>Diverges from {@code JavaClassGenerator#generateClasses}, which preserves
 * identity for all already-typed {@code GenerationException}s unconditionally —
 * including bare GEs without payload. Both paths receive bare GEs from
 * {@code TemplateRenderer}; only this (function) path adds a payload-aware
 * attribution-preserving wrap. The per-model path's behaviour for bare GEs is a
 * latent attribution-loss case (out of this PR's scope) (PR-1.5 Copilot R3 F2
 * divergence framing correction 2026-05-04).
 */
class FunctionGeneratorErrorPropagationTest {

    private static final JavaTypeUtil TYPE_UTIL = new JavaTypeUtil();
    private static final JavaTypeTranslator TYPE_TRANSLATOR = new JavaTypeTranslator(TYPE_UTIL);

    @Test
    void phase2_catch_preserves_generation_exception_identity() {
        var sentinel = new GenerationException(
                "synthetic Phase 2 R11-F3 failure",
                "test://Phase2.rosetta",
                null);

        var fg = standardThrowingGenerator(sentinel);
        Map<String, String> output = new LinkedHashMap<>();
        List<GenerationException> errors = fg.generateWithErrors(output);

        assertEquals(1, errors.size(),
                "Expected one error from Phase 2 catch; got: " + errors);
        assertSame(sentinel, errors.get(0),
                "Phase 2 catch must preserve GenerationException identity (R11 F3)");
        assertEquals("test://Phase2.rosetta", errors.get(0).getResourceUri(),
                "resourceUri must survive through the Phase 2 catch block");
    }

    @Test
    void phase2_catch_preserves_identity_when_only_context_is_set() {
        // Locks the OR guard's right-hand side: identity preserved when context
        // is non-null even if resourceUri is null (PR-1.5 Copilot R2 F3
        // 2026-05-04). Without this test, a regression that drops the
        // `getContext()` branch of the guard would leave the suite green.
        var contextNode = new RFunction();
        contextNode.setName("ContextNode");

        var sentinel = new GenerationException(
                "synthetic Phase 2 R2-F3 context-only failure",
                /* resourceUri */ null,
                contextNode);

        var fg = standardThrowingGenerator(sentinel);
        Map<String, String> output = new LinkedHashMap<>();
        List<GenerationException> errors = fg.generateWithErrors(output);

        assertEquals(1, errors.size());
        assertSame(sentinel, errors.get(0),
                "Phase 2 catch must preserve identity when context is non-null and resourceUri is null (R2 F3)");
        assertSame(contextNode, errors.get(0).getContext(),
                "context must survive through the Phase 2 catch block");
        assertNull(errors.get(0).getResourceUri(),
                "resourceUri should remain null on the preserved instance");
    }

    @Test
    void phase2_catch_wraps_runtime_exception_with_cause() {
        var cause = new RuntimeException("synthetic Phase 2 RuntimeException");

        var fg = standardThrowingGenerator(cause);
        Map<String, String> output = new LinkedHashMap<>();
        List<GenerationException> errors = fg.generateWithErrors(output);

        assertEquals(1, errors.size());
        assertNotSame(cause, errors.get(0),
                "Generic exceptions must be wrapped (not unwrapped) in Phase 2");
        assertSame(cause, errors.get(0).getCause(),
                "Phase 2 wrap must carry the original exception as cause");
        assertTrue(errors.get(0).getMessage().contains("FunctionGenerator: error generating "),
                "Phase 2 wrap message should announce wrap; got: "
                        + errors.get(0).getMessage());
    }

    @Test
    void phase3_catch_preserves_generation_exception_identity() {
        var sentinel = new GenerationException(
                "synthetic Phase 3 R11-F1 failure",
                "test://Phase3.rosetta",
                null);

        var fg = dispatchThrowingGenerator(sentinel);
        Map<String, String> output = new LinkedHashMap<>();
        List<GenerationException> errors = fg.generateWithErrors(output);

        assertEquals(1, errors.size());
        assertSame(sentinel, errors.get(0),
                "Phase 3 catch must preserve GenerationException identity (R11 F1)");
        assertEquals("test://Phase3.rosetta", errors.get(0).getResourceUri(),
                "resourceUri must survive through the Phase 3 catch block");
    }

    @Test
    void phase3_catch_preserves_identity_when_only_context_is_set() {
        // Same OR-guard right-hand-side coverage as the Phase 2 test, on the
        // dispatch path (PR-1.5 Copilot R2 F3 2026-05-04, class-of-issue sweep).
        var contextNode = new RFunction();
        contextNode.setName("ContextNode");

        var sentinel = new GenerationException(
                "synthetic Phase 3 R2-F3 context-only failure",
                /* resourceUri */ null,
                contextNode);

        var fg = dispatchThrowingGenerator(sentinel);
        Map<String, String> output = new LinkedHashMap<>();
        List<GenerationException> errors = fg.generateWithErrors(output);

        assertEquals(1, errors.size());
        assertSame(sentinel, errors.get(0),
                "Phase 3 catch must preserve identity when context is non-null and resourceUri is null (R2 F3)");
        assertSame(contextNode, errors.get(0).getContext(),
                "context must survive through the Phase 3 catch block");
        assertNull(errors.get(0).getResourceUri(),
                "resourceUri should remain null on the preserved instance");
    }

    @Test
    void phase3_catch_wraps_runtime_exception_with_cause() {
        var cause = new RuntimeException("synthetic Phase 3 RuntimeException");

        var fg = dispatchThrowingGenerator(cause);
        Map<String, String> output = new LinkedHashMap<>();
        List<GenerationException> errors = fg.generateWithErrors(output);

        assertEquals(1, errors.size());
        assertNotSame(cause, errors.get(0));
        assertSame(cause, errors.get(0).getCause());
        assertTrue(errors.get(0).getMessage().contains("FunctionGenerator: error generating dispatch "),
                "Phase 3 wrap message should announce dispatch wrap; got: "
                        + errors.get(0).getMessage());
    }

    // === payload-free GenerationException wrap-with-attribution ==============
    //
    // R1 F1+F3 sweep on top of R11 F1+F3: when an inner GenerationException
    // carries no diagnostic payload (resourceUri == null AND context == null),
    // identity preservation has no value — the catch block falls through to the
    // wrap branch so this generator's per-function attribution survives.
    // Producers like TemplateRenderer throw bare GEs with null payload; without
    // the payload guard those bare GEs would surface as plain "ST4 ..." messages
    // with no indication which function or dispatch broke.

    @Test
    void phase2_catch_wraps_payload_free_generation_exception_with_func_name() {
        var bare = new GenerationException("ST4 runtime error: missing arg",
                /* resourceUri */ null, /* context */ null);

        var fg = standardThrowingGenerator(bare);
        Map<String, String> output = new LinkedHashMap<>();
        List<GenerationException> errors = fg.generateWithErrors(output);

        assertEquals(1, errors.size());
        assertNotSame(bare, errors.get(0),
                "Payload-free GenerationException must be wrapped to add func-name attribution (R1 F1)");
        assertSame(bare, errors.get(0).getCause(),
                "Wrap must preserve the original GenerationException as cause");
        assertTrue(errors.get(0).getMessage().contains("StandardFunc"),
                "Wrap message must include func name; got: " + errors.get(0).getMessage());
        assertTrue(errors.get(0).getMessage().contains("ST4 runtime error"),
                "Wrap message must preserve inner exception message; got: "
                        + errors.get(0).getMessage());
    }

    @Test
    void phase3_catch_wraps_payload_free_generation_exception_with_dispatch_name() {
        var bare = new GenerationException("ST4 runtime error: missing arg",
                /* resourceUri */ null, /* context */ null);

        var fg = dispatchThrowingGenerator(bare);
        Map<String, String> output = new LinkedHashMap<>();
        List<GenerationException> errors = fg.generateWithErrors(output);

        assertEquals(1, errors.size());
        assertNotSame(bare, errors.get(0),
                "Payload-free GenerationException must be wrapped to add dispatch-name attribution (R1 F3)");
        assertSame(bare, errors.get(0).getCause(),
                "Wrap must preserve the original GenerationException as cause");
        assertTrue(errors.get(0).getMessage().contains("DispatchFunc"),
                "Wrap message must include dispatch name; got: " + errors.get(0).getMessage());
        assertTrue(errors.get(0).getMessage().contains("ST4 runtime error"),
                "Wrap message must preserve inner exception message; got: "
                        + errors.get(0).getMessage());
    }

    // === helpers ==============================================================

    private FunctionGenerator standardThrowingGenerator(RuntimeException toThrow) {
        var model = new RModel();
        model.setNamespace("com.example");
        var func = new RFunction();
        func.setName("StandardFunc");
        model.rootElements().add(func);
        func.setParent(model);

        var workspace = RWorkspace.build(List.of(model)).workspace();
        var gm = new GeneratorModel(workspace);

        return new FunctionGenerator(gm, TYPE_TRANSLATOR, TYPE_UTIL) {
            @Override
            void generateStandardFunction(RFunction f, Map<String, String> output) {
                throw toThrow;
            }
        };
    }

    private FunctionGenerator dispatchThrowingGenerator(RuntimeException toThrow) {
        var model = new RModel();
        model.setNamespace("com.example");

        var dispatch = new RDispatch();
        dispatch.setParamName("mode");
        dispatch.setEnumRef("ModeEnum");
        dispatch.setValueName("Active");

        var func = new RFunction();
        func.setName("DispatchFunc");
        func.setDispatch(dispatch);
        model.rootElements().add(func);
        func.setParent(model);

        var workspace = RWorkspace.build(List.of(model)).workspace();
        var gm = new GeneratorModel(workspace);

        return new FunctionGenerator(gm, TYPE_TRANSLATOR, TYPE_UTIL) {
            @Override
            void generateDispatchFunction(String baseName, List<RFunction> variants,
                                          RFunction base, Map<String, String> output) {
                throw toThrow;
            }
        };
    }
}
