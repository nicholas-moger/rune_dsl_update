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
 * Property tests for generated {@code func} declarations.
 *
 * <p>P1.2 audit hook H17.3 — third hot production.
 */
class FuncDeclProperties {

    @Provide
    Arbitrary<String> funcModel() {
        return RuneArbitraries.modelWith(RuneArbitraries.funcDecl());
    }

    @Property(tries = 200)
    void generated_func_parses_without_errors(@ForAll("funcModel") String source) {
        RosettaParseResult r = RosettaParserFacade.parseString(source);
        assertTrue(r.errors().isEmpty(),
                "Generated func failed to parse:\n---\n" + source + "---\n" + String.join("\n", r.errors()));
    }

    @Property(tries = 200)
    void parse_is_idempotent_across_two_invocations(@ForAll("funcModel") String source) {
        RosettaParseResult first = RosettaParserFacade.parseString(source);
        RosettaParseResult second = RosettaParserFacade.parseString(source);
        assertEquals(first.errors().size(), second.errors().size(),
                "Error count differs between two parses of identical source:\n"
                        + "source:\n" + source
                        + "\nfirst: " + first.errors()
                        + "\nsecond: " + second.errors());
    }
}
