package com.regnosys.rosetta.harness.fuzz;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.junit.FuzzTest;
import com.regnosys.rosetta.parser.RosettaParseResult;
import com.regnosys.rosetta.parser.RosettaParserFacade;

/**
 * Coverage-guided fuzz harness for the Rosetta parser.
 *
 * <p>P1.2 audit hook H20. Targets
 * {@link RosettaParserFacade#parseString(String)} — the public convenience
 * wrapper over {@code parse(CharStream)}. Jazzer treats any uncaught
 * {@link Throwable} from the harness as a finding; the facade is documented
 * as returning a {@link RosettaParseResult} with a (possibly non-empty)
 * errors list rather than throwing, so an uncaught throw here is a real
 * parser bug.
 *
 * <p>Two entry points:
 * <ul>
 *   <li>{@link #parseString_does_not_crash(FuzzedDataProvider)} — normal
 *       mode, default lenient UTF-8 (audit Q7). Runs under the standard
 *       Jqwik/Jazzer test lifecycle.
 *   <li>{@link #parseString_with_tokens_does_not_crash(FuzzedDataProvider)} —
 *       biased toward real Rosetta lexemes, so the fuzzer spends coverage
 *       budget on shapes the grammar actually accepts instead of purely
 *       random UTF-8.
 * </ul>
 *
 * <p>Seed corpus — audit Q5 — is bootstrapped from CDM 6.16.0 files at the
 * CI configuration layer (H22). The harness itself is agnostic to the seed
 * source: Jazzer feeds it {@link FuzzedDataProvider} bytes.
 */
class ParserFuzzTest {

    /**
     * Pool of realistic Rosetta keywords the token-biased harness draws
     * from. Covers the hot productions exercised by H17.1–H17.5 plus a
     * few common leaves so the fuzzer can build plausible near-programs.
     */
    private static final String[] KEYWORDS = {
            "namespace", "import", "type", "enum", "func", "rule", "report",
            "reporting", "eligibility", "condition", "inputs", "output",
            "from", "when", "with", "in", "as", "if", "then", "else",
            "true", "false", "and", "or",
            "string", "int", "number", "boolean", "date",
            ":", "(", ")", "[", "]", ",", ".", "->", "=", "+", "-", "*", "/",
            "\n", "    ", " "
    };

    @FuzzTest(maxDuration = "10s")
    void parseString_does_not_crash(FuzzedDataProvider data) {
        String source = data.consumeRemainingAsString();
        // The facade is contracted never to throw: it returns a result
        // with an `errors` list (stage-1 BailErrorStrategy is caught and
        // retried under LL). Any uncaught throw from this call is a real
        // finding, so we deliberately do NOT swallow Throwable here.
        RosettaParseResult result = RosettaParserFacade.parseString(source);
        // Touch the result so the JIT cannot elide the call.
        result.errors().size();
    }

    @FuzzTest(maxDuration = "10s")
    void parseString_with_tokens_does_not_crash(FuzzedDataProvider data) {
        int tokenCount = data.consumeInt(0, 200);
        StringBuilder sb = new StringBuilder(tokenCount * 6);
        for (int i = 0; i < tokenCount; i++) {
            if (data.remainingBytes() == 0) break;
            // 80% chance of a real keyword; 20% chance of arbitrary bytes
            // — keeps the fuzzer anchored to Rosetta shapes without losing
            // the randomness that catches parser edge cases.
            if (data.consumeBoolean() || data.consumeBoolean() || data.consumeBoolean()) {
                sb.append(KEYWORDS[data.consumeInt(0, KEYWORDS.length - 1)]);
            } else {
                int len = data.consumeInt(1, 16);
                sb.append(data.consumeAsciiString(len));
            }
            sb.append(' ');
        }
        RosettaParseResult result = RosettaParserFacade.parseString(sb.toString());
        result.errors().size();
    }
}
