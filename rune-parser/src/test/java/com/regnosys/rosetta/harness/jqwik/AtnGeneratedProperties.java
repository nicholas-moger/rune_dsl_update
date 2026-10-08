package com.regnosys.rosetta.harness.jqwik;

import com.regnosys.rosetta.parser.RosettaParseResult;
import com.regnosys.rosetta.parser.RosettaParser;
import com.regnosys.rosetta.parser.RosettaParserFacade;
import com.regnosys.rosetta.parser.RosettaLexer;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Property tests that feed ATN-walked source text to the parser
 * facade and assert the no-throw contract.
 *
 * <p>P1.2 audit hooks H18 (bootstrap) + H18.2 (expression coverage,
 * corpus-weighted RuleTransition selection). Parser errors are expected
 * from an ATN walk that doesn't respect every semantic predicate — the
 * property is {@code parser.parseString(x)} returns a
 * {@link RosettaParseResult} (errors may be populated), never throws.
 *
 * <p>Target rules: {@code enumeration}, {@code rosettaEnumValue},
 * {@code typeCall}, and (H18.2) {@code expression}. The expression rule
 * is left-recursive with 91 labelled alternatives; the walker handles it
 * via ANTLR4's rewritten-LR ATN shape, with {@link AtnStringGenerator}'s
 * budget caps preventing runaway and {@link RuleWeights} biasing which
 * child rules the loop drops into.
 */
class AtnGeneratedProperties {

    // Weights loaded once per test class, not per @Provide call —
    // parser instantiation is cheap but the properties file load and
    // the rule-name lookup are wasted work if repeated per sample.
    private static final RuleWeights RULE_WEIGHTS = loadWeights();

    private static RuleWeights loadWeights() {
        RosettaParser parser = newParser();
        return RuleWeights.fromDefaultResource(parser.getRuleNames());
    }

    @Provide
    Arbitrary<String> enumerationFragment() {
        return atnWalkOf(RosettaParser.RULE_enumeration);
    }

    @Provide
    Arbitrary<String> rosettaEnumValueFragment() {
        return atnWalkOf(RosettaParser.RULE_rosettaEnumValue);
    }

    @Provide
    Arbitrary<String> typeCallFragment() {
        return atnWalkOf(RosettaParser.RULE_typeCall);
    }

    @Provide
    Arbitrary<String> expressionFragment() {
        // Expression can't stand alone — it needs a holder context. The
        // smallest holder per RosettaParser.g4 is `alias X: <expr>`
        // inside a `func`, matching RuneArbitraries.expressionInFunctionBody()
        // so the two hooks share a wrapping shape.
        return Arbitraries.longs().map(seed -> {
            RosettaParser parser = newParser();
            AtnStringGenerator gen = new AtnStringGenerator(
                    parser.getATN(), parser.getVocabulary(),
                    new Random(seed), RULE_WEIGHTS);
            String expr = gen.generate(RosettaParser.RULE_expression);
            return "namespace test.atn\n\nfunc TestExpr:\n    alias X: " + expr + "\n";
        });
    }

    @Property(tries = 200)
    void parser_does_not_throw_on_atn_generated_enumeration(
            @ForAll("enumerationFragment") String source) {
        assertParserDoesNotThrow(source);
    }

    @Property(tries = 200)
    void parser_does_not_throw_on_atn_generated_enum_value(
            @ForAll("rosettaEnumValueFragment") String source) {
        assertParserDoesNotThrow(source);
    }

    @Property(tries = 200)
    void parser_does_not_throw_on_atn_generated_type_call(
            @ForAll("typeCallFragment") String source) {
        assertParserDoesNotThrow(source);
    }

    @Property(tries = 200)
    void parser_does_not_throw_on_atn_generated_expression(
            @ForAll("expressionFragment") String source) {
        assertParserDoesNotThrow(source);
    }

    // ---- helpers ----

    /**
     * Build a Jqwik arbitrary that draws a long seed and walks the
     * parser ATN deterministically from that seed. Determinism under
     * a fixed seed is part of the reproducibility contract (`jqwik.md`
     * §7) — a failing sample replays bit-for-bit.
     */
    private Arbitrary<String> atnWalkOf(int ruleIndex) {
        return Arbitraries.longs().map(seed -> {
            RosettaParser parser = newParser();
            AtnStringGenerator gen = new AtnStringGenerator(
                    parser.getATN(), parser.getVocabulary(),
                    new Random(seed), RULE_WEIGHTS);
            // Wrap the generated fragment in a minimal namespace header
            // so the parser's root production (`rosettaModel`) has a
            // chance to consume it — otherwise even a perfect fragment
            // would fail at the namespace requirement.
            return "namespace test.atn\n\n" + gen.generate(ruleIndex) + "\n";
        });
    }

    private static RosettaParser newParser() {
        RosettaLexer lexer = new RosettaLexer(CharStreams.fromString(""));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        return new RosettaParser(tokens);
    }

    private static void assertParserDoesNotThrow(String source) {
        // Contract: RosettaParserFacade.parseString returns a result
        // object (errors list may be populated), never throws.
        RosettaParseResult r = RosettaParserFacade.parseString(source);
        assertNotNull(r, "parser must return a non-null result");
        assertNotNull(r.errors(), "parse result's errors list must be non-null");
    }
}
