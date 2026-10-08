package com.regnosys.rosetta.harness.jqwik;

import com.regnosys.rosetta.parser.RosettaParseResult;
import com.regnosys.rosetta.parser.RosettaParserFacade;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Property tests for generated reporting/eligibility {@code rule}
 * declarations.
 *
 * <p>P1.2 audit hook H17.4 — fourth hot production.
 */
class RuleDeclProperties {

    @Provide
    Arbitrary<String> ruleModel() {
        return RuneArbitraries.modelWith(RuneArbitraries.ruleDecl());
    }

    @Property(tries = 200)
    void generated_rule_parses_without_errors(@ForAll("ruleModel") String source) {
        RosettaParseResult r = RosettaParserFacade.parseString(source);
        assertTrue(r.errors().isEmpty(),
                "Generated rule failed to parse:\n---\n" + source + "---\n" + String.join("\n", r.errors()));
    }

    @Property(tries = 200)
    void parse_is_idempotent_across_two_invocations(@ForAll("ruleModel") String source) {
        RosettaParseResult first = RosettaParserFacade.parseString(source);
        RosettaParseResult second = RosettaParserFacade.parseString(source);
        assertEquals(first.errors().size(), second.errors().size(),
                "Error count differs between two parses of identical source:\n"
                        + "source:\n" + source
                        + "\nfirst: " + first.errors()
                        + "\nsecond: " + second.errors());
    }
}
