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
 * Property tests for generated {@code type} declarations.
 *
 * <p>P1.2 audit hook H17.2 — second hot production after enum. Follows
 * the H17-bootstrap pattern: parse stability + idempotent re-parse.
 */
class TypeDeclProperties {

    @Provide
    Arbitrary<String> typeModel() {
        return RuneArbitraries.modelWith(RuneArbitraries.typeDecl());
    }

    @Property(tries = 200)
    void generated_type_parses_without_errors(@ForAll("typeModel") String source) {
        RosettaParseResult r = RosettaParserFacade.parseString(source);
        assertTrue(r.errors().isEmpty(),
                "Generated type failed to parse:\n---\n" + source + "---\n" + String.join("\n", r.errors()));
    }

    @Property(tries = 200)
    void parse_is_idempotent_across_two_invocations(@ForAll("typeModel") String source) {
        RosettaParseResult first = RosettaParserFacade.parseString(source);
        RosettaParseResult second = RosettaParserFacade.parseString(source);
        assertEquals(first.errors().size(), second.errors().size(),
                "Error count differs between two parses of identical source — hidden state?\n"
                        + "source:\n" + source
                        + "\nfirst errors: " + first.errors()
                        + "\nsecond errors: " + second.errors());
    }
}
