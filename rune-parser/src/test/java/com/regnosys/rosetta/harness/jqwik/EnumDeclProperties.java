package com.regnosys.rosetta.harness.jqwik;

import com.regnosys.rosetta.parser.RosettaParseResult;
import com.regnosys.rosetta.parser.RosettaParserFacade;
import net.jqwik.api.ForAll;
import net.jqwik.api.From;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Arbitrary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Property tests for generated {@code enum} declarations.
 *
 * <p>P1.2 audit hook H17 (bootstrap — enum production). Establishes the
 * property-test harness shape the remaining hot productions (H17.2–H17.6)
 * will follow:
 * <ul>
 *   <li><b>Parse stability</b> — any generated enum parses without errors.
 *   <li><b>Idempotent re-parse</b> — parsing the same source twice produces
 *       identical error counts. Pins out hidden static state.
 * </ul>
 */
class EnumDeclProperties {

    @Provide
    Arbitrary<String> enumModel() {
        return RuneArbitraries.modelWith(RuneArbitraries.enumDecl());
    }

    @Property(tries = 200)
    void generated_enum_parses_without_errors(@ForAll("enumModel") String source) {
        RosettaParseResult r = RosettaParserFacade.parseString(source);
        assertTrue(r.errors().isEmpty(),
                "Generated enum failed to parse:\n---\n" + source + "---\n" + String.join("\n", r.errors()));
    }

    @Property(tries = 200)
    void parse_is_idempotent_across_two_invocations(@ForAll("enumModel") String source) {
        RosettaParseResult first = RosettaParserFacade.parseString(source);
        RosettaParseResult second = RosettaParserFacade.parseString(source);
        assertEquals(first.errors().size(), second.errors().size(),
                "Error count differs between two parses of identical source — hidden state?\n"
                        + "source:\n" + source
                        + "\nfirst errors: " + first.errors()
                        + "\nsecond errors: " + second.errors());
    }
}
