package com.regnosys.rosetta.generator.java;

import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Leg-C port (PR #423, slice 2) of upstream
 * {@code rune-integration-tests/.../tests/util/ExpressionParserTest.xtend} —
 * 3/3 methods: the acceptance bar for the expression-context parse seat itself
 * ({@link UpstreamExpressionPortSupport}, the fork mirror of upstream's
 * {@code ExpressionParser}). Bare expressions, declared-attribute contexts
 * (including a parameterized {@code number(max: 5)} attribute) and a
 * full-model context with a func call all parse, link and validate clean.
 * Ledger: the development audit "2026-07-17-leg-c-integration-port-ledger".
 */
class UpstreamExpressionParserPortTest {

    /** Upstream {@code simpleExpressionParseTest}. */
    @Test
    void simpleExpressionParseTest() {
        UpstreamExpressionPortSupport.parse("(1 + 1) / 2").assertNoIssues();
    }

    /** Upstream {@code expressionWithVariablesParseTest}. */
    @Test
    void expressionWithVariablesParseTest() {
        UpstreamExpressionPortSupport.parse(
                List.of("a int (1..1)", "b number(max: 5) (0..1)"),
                "(a + b) / 2").assertNoIssues();
    }

    /**
     * Upstream {@code expressionWithContextParseTest} — upstream parses
     * {@code foo -> attr - Bar(foo)} with no issues. The leg-C finding-#4 pin
     * (PR #423) that recorded the fork's un-retracted input-nav
     * {@code ENUM_NOT_FOUND} on the {@code foo -> attr} head HEALED at the
     * PR #443 class-(a) wave (the input-feature channel now clears the stale
     * diagnostic on bind) — un-pinned to upstream's clean-parse form, with
     * upstream's context-model half ({@code parseRosettaWithNoIssues} on the
     * Foo/Bar model, whose own {@code alias part: foo -> attr + 1} carried the
     * identical stale class) restored as the all-linking-clean assert.
     */
    @Test
    void expressionWithContextParseTest() {
        String model = """
                type Foo:
                	attr int (0..1)

                func Bar:
                	inputs:
                		foo Foo (1..1)
                	output:
                		result int (1..1)

                	alias part: foo -> attr + 1

                	set result:
                		part + 1
                """;
        var parsed = UpstreamExpressionPortSupport.parse(
                List.of(model),
                List.of("foo Foo (1..1)"),
                "foo -> attr - Bar(foo)");
        parsed.assertNoIssues();
        org.junit.jupiter.api.Assertions.assertTrue(parsed.allLinking().isEmpty(),
                "upstream's context-model half parses clean too "
                + "(parseRosettaWithNoIssues): " + parsed.allLinking());
    }
}
