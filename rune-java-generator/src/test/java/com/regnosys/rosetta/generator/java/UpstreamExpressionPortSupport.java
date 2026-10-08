package com.regnosys.rosetta.generator.java;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.symbols.RLinkingResult;
import com.regnosys.rosetta.symbols.RWorkspace;
import com.regnosys.rosetta.symbols.diagnostics.LinkingDiagnostic;
import com.regnosys.rosetta.symbols.diagnostics.ValidationDiagnostic;
import com.regnosys.rosetta.types.ExpressionCardinality;
import com.regnosys.rosetta.types.RMetaAnnotatedType;
import org.junit.jupiter.api.Assertions;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * The leg-C EXPRESSION-CONTEXT parse seat (PR #423, slice 2) — the fork mirror
 * of upstream {@code rune-testing}'s {@code ExpressionParser}
 * ({@code parseExpression(expr, context, attributes)}).
 *
 * <p>Upstream partial-parses the bare expression at the grammar's
 * ExpressionRule, wraps it in a synthetic resource and statically links the
 * declared attributes into scope. The fork has no partial-parse entry — the
 * SAME contract is reached through a WRAPPER MODEL: the expression becomes an
 * {@code alias} body inside a synthetic func whose {@code inputs:} are the
 * declared attributes verbatim (an alias body types WITHOUT an expected type,
 * so nothing constrains the expression — the alias law), and the context
 * models join the same workspace with {@code import <ns>.*} lines mirroring
 * upstream's {@code RosettaContextBasedScopeProvider} implicit imports. Every
 * parse loads the builtins + the {@code commonTestTypes} companion exactly
 * like {@link UpstreamPortHarness#generate}.
 *
 * <p>Diagnostics are WRAPPER-FILE-SCOPED (linking + validation filtered on
 * {@code SourceRange.file()}), mirroring upstream's expression-resource-scoped
 * {@code assertNoIssues}/{@code assertError}.
 */
final class UpstreamExpressionPortSupport {

    private UpstreamExpressionPortSupport() { }

    static final String WRAPPER_NS = "com.rosetta.test.model";
    static final String WRAPPER_FILE = "expression-port-wrapper.rosetta";
    static final String WRAPPER_FUNC = "PortExprWrapper";

    /** One parsed expression-in-context: the workspace, the node, the scoped diagnostics. */
    record ParsedExpression(
            RWorkspace ws,
            RExpression expr,
            List<LinkingDiagnostic> allLinking,
            List<LinkingDiagnostic> wrapperLinking,
            List<ValidationDiagnostic> wrapperValidation) {

        RMetaAnnotatedType type() {
            return ws.getInferredType(expr);
        }

        boolean isMulti() {
            return ws.getCardinality(expr) == ExpressionCardinality.MULTI;
        }

        /** Upstream {@code assertNoIssues} — no linking errors, no validator issues. */
        void assertNoIssues() {
            Assertions.assertTrue(wrapperLinking.isEmpty() && wrapperValidation.isEmpty(),
                    "expected no issues, got linking=" + wrapperLinking
                            + " validation=" + wrapperValidation);
        }

        /** Upstream {@code assertError(…, message)} — some wrapper-scoped ERROR carries the message. */
        void assertHasValidationError(String expectedMessage) {
            boolean found = wrapperValidation.stream()
                    .filter(d -> d.severity() == com.regnosys.rosetta.symbols.diagnostics.Severity.ERROR)
                    .anyMatch(d -> d.message().equals(expectedMessage));
            Assertions.assertTrue(found,
                    "expected a validation ERROR [" + expectedMessage + "] but got: "
                            + wrapperValidation);
        }

        /**
         * The released-9.83.0 WARNING assert (PR #454 — the Annex-A un-pins):
         * some wrapper-scoped WARNING-severity diagnostic carries exactly the
         * message. The severity oracle is the RELEASED jar's bytecode
         * ({@code isSingleCheck}/{@code isMultiCheck} emit warnings there —
         * see the fork validator's {@code annexAReleasedSeverities} facet).
         */
        void assertHasValidationWarning(String expectedMessage) {
            boolean found = wrapperValidation.stream()
                    .filter(d -> d.severity() == com.regnosys.rosetta.symbols.diagnostics.Severity.WARNING)
                    .anyMatch(d -> d.message().equals(expectedMessage));
            Assertions.assertTrue(found,
                    "expected a validation WARNING [" + expectedMessage + "] but got: "
                            + wrapperValidation);
        }

        long validationErrorCount() {
            return wrapperValidation.stream()
                    .filter(d -> d.severity() == com.regnosys.rosetta.symbols.diagnostics.Severity.ERROR)
                    .count();
        }

        long validationWarningCount() {
            return wrapperValidation.stream()
                    .filter(d -> d.severity() == com.regnosys.rosetta.symbols.diagnostics.Severity.WARNING)
                    .count();
        }
    }

    static ParsedExpression parse(String expr) {
        return parse(List.of(), List.of(), expr);
    }

    static ParsedExpression parse(List<String> attributes, String expr) {
        return parse(List.of(), attributes, expr);
    }

    /**
     * Parse {@code expr} with {@code contextSources} (full models, upstream's
     * {@code List<RosettaModel>} context) and {@code attributes} (upstream's
     * declared-attribute strings, verbatim — e.g. {@code "a int (2..4) [metadata scheme]"}).
     */
    static ParsedExpression parse(List<String> contextSources, List<String> attributes, String expr) {
        return parse(contextSources, attributes, expr, false);
    }

    /**
     * The SET-BODY channel: the expression becomes {@code set wrapperResult: <expr>}
     * instead of an alias body. HISTORICAL origin: at the #423 port the #279
     * input-nav typing arm gated out alias (RShortcut) bodies (leg-C finding
     * #9), so navigation expressions only typed through an operation body. The
     * #429 typing-channel wave LIFTED that gate and the #438 probe measured
     * both channels typing identically (finding #9 CLOSED — see the
     * disposition brief, the development audit "2026-07-19-recorded-findings-disposition-brief");
     * the channel is KEPT because the projection pins ride it and an
     * operation body is the natural context for nav pins. The wrapper output
     * stays {@code boolean (0..*)}: the fork has no set-coercion/cardinality
     * validator to object, and inference is bottom-up so the expression's own
     * type is unaffected by the output type.
     */
    static ParsedExpression parseInSetBody(List<String> contextSources, List<String> attributes,
            String expr) {
        return parse(contextSources, attributes, expr, true);
    }

    private static ParsedExpression parse(List<String> contextSources, List<String> attributes,
            String expr, boolean setBodyChannel) {
        try {
            List<RModel> models = new ArrayList<>();
            for (Path p : HoldOutByteCompareTest.resolveBuiltinFiles()) {
                models.add(AstBuilder.buildFromFile(p));
            }
            models.add(AstBuilder.buildFromString(
                    UpstreamPortHarness.COMMON_TEST_TYPES, "common-test-types.rosetta"));

            LinkedHashSet<String> contextNamespaces = new LinkedHashSet<>();
            int i = 0;
            for (String ctx : contextSources) {
                String src = ctx.trim().startsWith("namespace")
                        ? ctx
                        : UpstreamPortHarness.TEST_NS_HEADER + "\n" + ctx;
                RModel m = AstBuilder.buildFromString(src, "expression-port-context-" + (i++) + ".rosetta");
                contextNamespaces.add(m.namespace());
                models.add(m);
            }

            StringBuilder wrapper = new StringBuilder(UpstreamPortHarness.TEST_NS_HEADER);
            for (String ns : contextNamespaces) {
                if (!WRAPPER_NS.equals(ns)) {
                    wrapper.append("import ").append(ns).append(".*\n");
                }
            }
            wrapper.append("\nfunc ").append(WRAPPER_FUNC).append(":\n");
            if (!attributes.isEmpty()) {
                wrapper.append("\tinputs:\n");
                for (String attr : attributes) {
                    wrapper.append("\t\t").append(attr).append("\n");
                }
            }
            if (setBodyChannel) {
                wrapper.append("\toutput:\n\t\twrapperResult boolean (0..*)\n");
                wrapper.append("\tset wrapperResult: ").append(expr).append("\n");
            } else {
                wrapper.append("\toutput:\n\t\twrapperResult boolean (0..1)\n");
                wrapper.append("\talias portExpr: ").append(expr).append("\n");
                wrapper.append("\tset wrapperResult: True\n");
            }

            RModel wrapperModel = AstBuilder.buildFromString(wrapper.toString(), WRAPPER_FILE);
            models.add(wrapperModel);

            RLinkingResult result = RWorkspace.build(models);
            RWorkspace ws = result.workspace();

            RFunction fn = null;
            for (var el : wrapperModel.rootElements()) {
                if (el instanceof RFunction f && WRAPPER_FUNC.equals(f.name())) {
                    fn = f;
                }
            }
            Assertions.assertNotNull(fn, "wrapper func did not parse:\n" + wrapper);
            RExpression parsed;
            if (setBodyChannel) {
                Assertions.assertFalse(fn.operations().isEmpty(),
                        "wrapper set operation did not parse:\n" + wrapper);
                parsed = fn.operations().get(0).expression();
            } else {
                Assertions.assertFalse(fn.shortcuts().isEmpty(),
                        "wrapper alias did not parse:\n" + wrapper);
                parsed = fn.shortcuts().get(0).expression();
            }
            Assertions.assertNotNull(parsed, "wrapper expression missing:\n" + wrapper);

            List<LinkingDiagnostic> allLinking = result.linkingDiagnostics();
            List<LinkingDiagnostic> linking = allLinking.stream()
                    .filter(d -> WRAPPER_FILE.equals(d.range().file()))
                    .toList();
            List<ValidationDiagnostic> validation = ws.validationDiagnostics().stream()
                    .filter(d -> WRAPPER_FILE.equals(d.range().file()))
                    .toList();
            return new ParsedExpression(ws, parsed, allLinking, linking, validation);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
