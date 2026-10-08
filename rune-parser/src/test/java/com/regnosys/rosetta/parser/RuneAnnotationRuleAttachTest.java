package com.regnosys.rosetta.parser;

import com.regnosys.rosetta.ast.functions.RRule;
import com.regnosys.rosetta.ast.model.RModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end attach tests for runeAnnotations? on rosettaRule (P1.4.2 H11 / T6).
 * Free reuse of H2 infrastructure on rule declarations.
 */
class RuneAnnotationRuleAttachTest {

    @Test
    void reportingRuleWithRuneAnnotation() {
        String src = """
                namespace foo

                type Trade:
                  tradeId string (1..1)

                @experimental
                reporting rule TradeIdRule from Trade:
                  extract item -> tradeId
                """;
        RModel model = parse(src);
        RRule rule = (RRule) model.rootElements().stream()
            .filter(r -> r instanceof RRule).findFirst().orElseThrow();
        assertEquals(1, rule.runeAnnotations().size());
        assertEquals("experimental", rule.runeAnnotations().get(0).annotationName());
    }

    @Test
    void eligibilityRuleWithRuneAnnotation() {
        String src = """
                namespace foo

                type Trade:
                  isActive boolean (1..1)

                @experimental
                eligibility rule IsReportable from Trade:
                  extract item -> isActive
                """;
        RModel model = parse(src);
        RRule rule = (RRule) model.rootElements().stream()
            .filter(r -> r instanceof RRule).findFirst().orElseThrow();
        assertEquals(1, rule.runeAnnotations().size());
        assertEquals("experimental", rule.runeAnnotations().get(0).annotationName());
    }

    @Test
    void ruleWithoutAnnotationsParsesUnchanged() {
        String src = """
                namespace foo

                type Trade:
                  tradeId string (1..1)

                reporting rule TradeIdRule from Trade:
                  extract item -> tradeId
                """;
        RModel model = parse(src);
        RRule rule = (RRule) model.rootElements().stream()
            .filter(r -> r instanceof RRule).findFirst().orElseThrow();
        assertTrue(rule.runeAnnotations().isEmpty());
    }

    @Test
    void ruleWithMultiArgRuneAnnotation() {
        String src = """
                namespace foo

                type Trade:
                  tradeId string (1..1)

                @feature(name = "lineage", since = "0.3")
                reporting rule TradeIdRule from Trade:
                  extract item -> tradeId
                """;
        RModel model = parse(src);
        RRule rule = (RRule) model.rootElements().stream()
            .filter(r -> r instanceof RRule).findFirst().orElseThrow();
        var ann = rule.runeAnnotations().get(0);
        assertEquals("feature", ann.annotationName());
        assertEquals(2, ann.arguments().size());
        assertEquals("name", ann.arguments().get(0).name());
        assertEquals("lineage", ann.arguments().get(0).valueAsString());
    }

    private static RModel parse(String src) {
        return TestParseHelper.parseModel(src);
    }
}
