package com.regnosys.rosetta.generator.java;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Leg-C port (PR #423, slice 2) of upstream
 * {@code rune-integration-tests/.../qualify/RosettaQualifyEventTest.xtend} —
 * 3/3 methods. The qualify RUNTIME stack (upstream {@code QualifyTestHelper}:
 * {@code QualifyFunctionFactory.Default} → {@code RosettaMetaDataBuilder} →
 * {@code QualifyResultsExtractor}) runs REAL Guice inside the isolated loader
 * via {@link UpstreamPortHarness#qualifyAllResults} — a behaviour dimension no
 * byte bar exercises (the XMeta registry bytes are corpus-locked; THIS proves
 * the registered qualify functions evaluate). Ledger:
 * the development audit "2026-07-17-leg-c-integration-port-ledger".
 */
class UpstreamQualifyEventPortTest {

    @BeforeAll
    static void requireReleasedRuntime() {
        UpstreamPortHarness.assumeReleasedRuntime();
    }

    private Map<String, Class<?>> classes;

    /** Upstream {@code setUp} — the shared isEvent model, verbatim. */
    @BeforeEach
    void setUp() {
        classes = UpstreamPortHarness.compileToClasses(UpstreamPortHarness.generateCode("""
                isEvent root Foo;

                type Foo:
                	bar Bar (0..*)
                	baz Baz (0..1)

                type Bar:
                	before number (0..1)
                	after number (0..1)

                type Baz:
                	bazValue number (0..1)
                	other number (0..1)

                func Qualify_Event:
                	[qualification BusinessEvent]
                	inputs: foo Foo(1..1)
                	output: is_event boolean (1..1)
                	set is_event:
                		(foo -> baz -> bazValue is absent or foo -> baz -> bazValue = 15)
                """));
    }

    private Object fooWithBazValue(BigDecimal bazValue, String bazAttr) {
        Object baz = UpstreamPortHarness.createInstanceUsingBuilder(classes, "Baz",
                Map.of(bazAttr, bazValue));
        return UpstreamPortHarness.createInstanceUsingBuilder(classes, "Foo",
                Map.of("baz", baz));
    }

    /** Upstream {@code whenPresentExpr_isPresent_and_matches_should_qualify}. */
    @Test
    void whenPresentExpr_isPresent_and_matches_should_qualify() {
        Object foo = fooWithBazValue(BigDecimal.valueOf(15), "bazValue");

        List<Object> results = UpstreamPortHarness.qualifyAllResults(classes, foo);
        Object result = UpstreamPortHarness.qualifyResult(results, "Event");
        assertTrue((Boolean) UpstreamPortHarness.call(result, "isSuccess"),
                "Unexpected success result");
        assertEquals(1, ((Collection<?>) UpstreamPortHarness.call(
                result, "getExpressionDataRuleResults")).size(),
                "Unexpected number of expressionDataRule results");
    }

    /** Upstream {@code whenPresentExpr_isPresent_and_doesNotMatch_should_not_qualify}. */
    @Test
    void whenPresentExpr_isPresent_and_doesNotMatch_should_not_qualify() {
        Object foo = fooWithBazValue(BigDecimal.valueOf(20), "bazValue");

        List<Object> results = UpstreamPortHarness.qualifyAllResults(classes, foo);
        Object result = UpstreamPortHarness.qualifyResult(results, "Event");
        assertFalse((Boolean) UpstreamPortHarness.call(result, "isSuccess"));
        assertEquals(1, ((Collection<?>) UpstreamPortHarness.call(
                result, "getExpressionDataRuleResults")).size(),
                "Unexpected number of expressionDataRule results");
    }

    /** Upstream {@code whenPresentExpr_isNotPresent_should_qualify}. */
    @Test
    void whenPresentExpr_isNotPresent_should_qualify() {
        Object foo = fooWithBazValue(BigDecimal.valueOf(20), "other");

        List<Object> results = UpstreamPortHarness.qualifyAllResults(classes, foo);
        Object result = UpstreamPortHarness.qualifyResult(results, "Event");
        assertTrue((Boolean) UpstreamPortHarness.call(result, "isSuccess"),
                "Unexpected success result");
        assertEquals(1, ((Collection<?>) UpstreamPortHarness.call(
                result, "getExpressionDataRuleResults")).size(),
                "Unexpected number of expressionDataRule results");
    }
}
