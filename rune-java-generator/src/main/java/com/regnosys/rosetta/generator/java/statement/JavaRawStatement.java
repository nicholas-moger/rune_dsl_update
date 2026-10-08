package com.regnosys.rosetta.generator.java.statement;

import java.util.Objects;
import java.util.Set;

import com.rosetta.util.types.JavaClass;

/**
 * facet inLambdaArgSeatIteHoist (PR #355): a pre-rendered (possibly multi-line)
 * statement block registered on the pending-lambda-hoist channel
 * ({@code JavaStatementScope.registerPendingLambdaHoist}) — the in-lambda
 * {@code ifThenElseResult} if/else blocks {@code ControlFlowHandler.hoistAsItemLocalOrNull}
 * builds for a conditional consumed at an arg seat INSIDE a map/extract lambda.
 *
 * <p>The text is emitted VERBATIM (plus a trailing newline when absent):
 * <ul>
 *   <li>local names are embedded as OPAQUE {@code StatementHoistSession} sentinel
 *       tokens, substituted at the per-method {@code session.resolve(…)} pass —
 *       the #346-safe late-binding pattern: no {@code GeneratedIdentifier} renders
 *       at drain time, so the drain closes no naming scope;</li>
 *   <li>interior lines carry RELATIVE indent only (depth-tabs from the block's own
 *       base, the #179 condition-channel law) — every draining renderer re-anchors
 *       each line at its own statement indent
 *       ({@code CollectionHandler.renderBlockLambdaBody} / the arm-interior
 *       relocation window in {@code ControlFlowHandler.appendConditionalChain}).</li>
 * </ul>
 */
public class JavaRawStatement extends JavaStatement {
    private final String text;
    private final Set<JavaClass<?>> refs;
    private final Set<JavaClass<?>> staticWildcardImports;

    public JavaRawStatement(String text, Set<JavaClass<?>> refs,
            Set<JavaClass<?>> staticWildcardImports) {
        // Defensive copies + fail-fast nulls, consistent with JavaExpression /
        // JavaStatementList — a later caller-side mutation of the passed sets
        // must not change this statement's import contribution.
        this.text = Objects.requireNonNull(text, "text");
        this.refs = Set.copyOf(Objects.requireNonNull(refs, "refs"));
        this.staticWildcardImports = Set.copyOf(
                Objects.requireNonNull(staticWildcardImports, "staticWildcardImports"));
    }

    @Override
    public void render(StringBuilder sb) {
        sb.append(text);
        if (!text.endsWith("\n")) {
            sb.append('\n');
        }
    }

    @Override
    public Set<JavaClass<?>> getRefs() {
        return refs;
    }

    @Override
    public Set<JavaClass<?>> getStaticWildcardImports() {
        return staticWildcardImports;
    }
}
