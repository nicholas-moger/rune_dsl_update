package com.regnosys.rosetta.generator.java;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Leg-C port (PR #423, slice 2) of upstream
 * {@code rune-integration-tests/.../qualify/RosettaQualifyProductTest.xtend} —
 * 2/2 methods (the isProduct twin of {@link UpstreamQualifyEventPortTest};
 * upstream's second method name says "BranchAndLeafNodeCount…" but its body
 * asserts {@code BranchNodeCountComparisonToLiteral} is FALSE — ported
 * body-verbatim). Ledger:
 * the development audit "2026-07-17-leg-c-integration-port-ledger".
 */
class UpstreamQualifyProductPortTest {

    @BeforeAll
    static void requireReleasedRuntime() {
        UpstreamPortHarness.assumeReleasedRuntime();
    }

    private Map<String, Class<?>> classes;

    /** Upstream {@code setUp} — the shared isProduct model, verbatim. */
    @BeforeEach
    void setUp() {
        classes = UpstreamPortHarness.compileToClasses(UpstreamPortHarness.generateCode("""
                isProduct root Foo;

                type Foo:
                	bar Bar (0..*)
                	corge number (0..1)

                type Bar:
                	baz Baz (0..1)
                	qux number (0..1)

                type Baz:
                	quux number (0..1)

                func Qualify_BranchNodeCountComparisonToLiteral:
                	[qualification Product]
                	inputs: foo Foo (1..1)
                	output: is_product boolean (1..1)
                	set is_product:
                		foo -> bar -> baz count = 2
                """));
    }

    private Object createFoo(List<?> bars, int corge) {
        return UpstreamPortHarness.createInstanceUsingBuilder(classes, "Foo",
                Map.of("corge", BigDecimal.valueOf(corge)), Map.of("bar", bars));
    }

    private void assertResult(List<Object> results, String isProductName, boolean expectedSuccess) {
        Object result = results.stream()
                .filter(r -> isProductName.equals(UpstreamPortHarness.call(r, "getName")))
                .findFirst().orElse(null);
        assertNotNull(result, "no qualify result named " + isProductName);
        assertEquals(expectedSuccess, UpstreamPortHarness.call(result, "isSuccess"));
    }

    /** Upstream {@code should_match_BranchNodeCountComparisonToLiteral_only}. */
    @Test
    void should_match_BranchNodeCountComparisonToLiteral_only() {
        Object baz = UpstreamPortHarness.createInstanceUsingBuilder(classes, "Baz",
                Map.of("quux", BigDecimal.valueOf(1.1)));
        Object bar1 = UpstreamPortHarness.createInstanceUsingBuilder(classes, "Bar",
                Map.of("baz", baz));
        Object bar2 = UpstreamPortHarness.createInstanceUsingBuilder(classes, "Bar",
                Map.of("baz", baz));
        Object foo = createFoo(List.of(bar1, bar2), 5);

        List<Object> results = UpstreamPortHarness.qualifyAllResults(classes, foo);

        assertResult(results, "BranchNodeCountComparisonToLiteral", true);
    }

    /** Upstream {@code should_match_BranchAndLeafNodeCountComparisonToLiterals_only}. */
    @Test
    void should_match_BranchAndLeafNodeCountComparisonToLiterals_only() {
        Object baz = UpstreamPortHarness.createInstanceUsingBuilder(classes, "Baz",
                Map.of("quux", BigDecimal.valueOf(1.1)));
        Object bar1 = UpstreamPortHarness.createInstanceUsingBuilder(classes, "Bar",
                Map.of("baz", baz));
        Object bar2 = UpstreamPortHarness.createInstanceUsingBuilder(classes, "Bar",
                Map.of("qux", BigDecimal.valueOf(1.2)));
        Object foo = createFoo(List.of(bar1, bar2), 5);

        List<Object> results = UpstreamPortHarness.qualifyAllResults(classes, foo);

        assertResult(results, "BranchNodeCountComparisonToLiteral", false);
    }
}
