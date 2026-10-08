package com.regnosys.rosetta.harness.jqwik;

import com.regnosys.rosetta.parser.RosettaLexer;
import com.regnosys.rosetta.parser.RosettaParser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class RuleWeightsTest {

    private static String[] parserRuleNames() {
        RosettaLexer lexer = new RosettaLexer(CharStreams.fromString(""));
        RosettaParser parser = new RosettaParser(new CommonTokenStream(lexer));
        return parser.getRuleNames();
    }

    @Test
    void uniform_returns_default_for_every_rule() {
        RuleWeights w = RuleWeights.uniform();
        assertEquals(0, w.size(),
                "uniform weights should declare zero overrides");
        assertEquals(RuleWeights.DEFAULT_WEIGHT, w.weightFor(0));
        assertEquals(RuleWeights.DEFAULT_WEIGHT, w.weightFor(42));
    }

    @Test
    void unknown_rule_index_falls_back_to_default() {
        Properties p = new Properties();
        p.setProperty("enumeration", "10");
        RuleWeights w = RuleWeights.fromProperties(p, parserRuleNames());
        // -1 is never a valid rule index; the map must return the default.
        assertEquals(RuleWeights.DEFAULT_WEIGHT, w.weightFor(-1));
    }

    @Test
    void known_rule_weight_is_parsed_and_applied() {
        Properties p = new Properties();
        p.setProperty("enumeration", "5.5");
        RuleWeights w = RuleWeights.fromProperties(p, parserRuleNames());

        int enumerationIndex = indexOf("enumeration");
        assertEquals(5.5, w.weightFor(enumerationIndex), 1e-9);
        assertEquals(1, w.size());
    }

    @Test
    void unknown_rule_name_is_silently_ignored() {
        Properties p = new Properties();
        p.setProperty("notARule_xyz_123", "99");
        p.setProperty("enumeration", "7");
        RuleWeights w = RuleWeights.fromProperties(p, parserRuleNames());
        // Only the valid key survives; unknown keys must not throw.
        assertEquals(1, w.size());
        assertEquals(7.0, w.weightFor(indexOf("enumeration")), 1e-9);
    }

    @Test
    void non_numeric_value_is_silently_ignored() {
        Properties p = new Properties();
        p.setProperty("enumeration", "not-a-number");
        RuleWeights w = RuleWeights.fromProperties(p, parserRuleNames());
        // Garbage values degrade to "no entry" → rule falls back to default.
        assertEquals(RuleWeights.DEFAULT_WEIGHT,
                w.weightFor(indexOf("enumeration")));
    }

    @Test
    void non_finite_values_are_silently_rejected() {
        // Double.parseDouble accepts "Infinity", "-Infinity", and "NaN"
        // as literal tokens. Without the isFinite guard, an Infinity
        // weight would propagate into AtnStringGenerator's cumulative
        // distribution and collapse the pick to always-last-transition
        // (total becomes +Inf, pick < cumulative[i] never holds).
        // Pinning the contract: non-finite values degrade to "no entry".
        Properties p = new Properties();
        p.setProperty("enumeration", "Infinity");
        p.setProperty("typeCall", "-Infinity");
        p.setProperty("attribute", "NaN");
        RuleWeights w = RuleWeights.fromProperties(p, parserRuleNames());
        assertEquals(0, w.size(),
                "non-finite weights must be silently dropped, not stored");
        assertEquals(RuleWeights.DEFAULT_WEIGHT, w.weightFor(indexOf("enumeration")));
        assertEquals(RuleWeights.DEFAULT_WEIGHT, w.weightFor(indexOf("typeCall")));
        assertEquals(RuleWeights.DEFAULT_WEIGHT, w.weightFor(indexOf("attribute")));
    }

    @Test
    void zero_or_negative_weight_is_floored_to_min() {
        Properties p = new Properties();
        p.setProperty("enumeration", "0");
        p.setProperty("typeCall", "-3.0");
        RuleWeights w = RuleWeights.fromProperties(p, parserRuleNames());
        // Floor prevents a rule from being silently skipped when the
        // grammar REQUIRES it — a zero weight at a mandatory choice
        // point would make the generator stall. The floor keeps the
        // walker's progress guarantee.
        assertEquals(RuleWeights.MIN_WEIGHT,
                w.weightFor(indexOf("enumeration")), 1e-9);
        assertEquals(RuleWeights.MIN_WEIGHT,
                w.weightFor(indexOf("typeCall")), 1e-9);
    }

    @Test
    void loadProperties_decodes_utf8_keys_and_values() throws IOException {
        // `Properties.load(InputStream)` decodes as ISO-8859-1 per the
        // properties-file spec, but the dumper (CorpusRuleCounter) writes
        // this file as UTF-8 — the committed header contains em-dashes.
        // A non-ASCII key read under ISO-8859-1 is bit-mangled: UTF-8
        // `café` (0x63 0x61 0x66 0xC3 0xA9) is decoded char-by-char to
        // `cafÃ©`, and the original key becomes unreachable. This test
        // pins the UTF-8 Reader behaviour so a future refactor can't
        // silently regress back to ISO-8859-1. Mirrors the fix applied
        // to StructuralBaselines in H26b rd-1 F2.
        String content = "# em-dash header — \n"
                + "enumeration=42\n"
                + "café=7\n";
        byte[] utf8 = content.getBytes(StandardCharsets.UTF_8);
        Properties p;
        try (ByteArrayInputStream in = new ByteArrayInputStream(utf8)) {
            p = RuleWeights.loadProperties(in);
        }
        assertEquals("42", p.getProperty("enumeration"));
        assertEquals("7", p.getProperty("café"),
                "non-ASCII key must round-trip through the UTF-8 decoder "
                        + "— ISO-8859-1 would turn `café` into `cafÃ©`");
    }

    @Test
    void fromDefaultResource_returns_a_valid_instance() {
        // The committed property-weights.properties ships as part of
        // test resources. Regardless of its current content (may be
        // placeholder or a full regeneration), loading must not throw
        // and must return something usable.
        RuleWeights w = RuleWeights.fromDefaultResource(parserRuleNames());
        assertNotNull(w);
        // Every rule must yield a finite positive weight.
        assertTrue(w.weightFor(0) >= RuleWeights.MIN_WEIGHT);
    }

    private static int indexOf(String ruleName) {
        String[] names = parserRuleNames();
        for (int i = 0; i < names.length; i++) {
            if (ruleName.equals(names[i])) return i;
        }
        throw new IllegalStateException("no such rule: " + ruleName);
    }
}
