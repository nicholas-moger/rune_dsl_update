package com.regnosys.rosetta.smoke;

import com.regnosys.rosetta.parser.RosettaErrorListener;
import com.regnosys.rosetta.parser.RosettaParseResult;
import com.regnosys.rosetta.parser.RosettaParserFacade;
import org.antlr.v4.runtime.BaseErrorListener;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * External-consumer fixture exercising the LEGACY parser surface only.
 *
 * <p>If this test ever fails to compile or run after a rune-parser change, the
 * back-compat invariant is broken (per U006).
 *
 * <p>Compiles + runs unchanged across PR1c — verifies that:
 * <ul>
 *   <li>{@code RosettaErrorListener} is still instantiable (despite {@code @Deprecated})</li>
 *   <li>{@code RosettaParseResult.errors()} returns {@code List<String>} (despite {@code @Deprecated})</li>
 *   <li>The legacy {@code line:col message} format is preserved</li>
 * </ul>
 */
class LegacyParserSurfaceSmokeTest {

    @Test
    @SuppressWarnings("deprecation")
    void rosettaErrorListenerStillInstantiable() {
        RosettaErrorListener listener = new RosettaErrorListener();
        assertNotNull(listener);
        assertTrue(listener instanceof BaseErrorListener);
        assertTrue(listener.getErrors().isEmpty());
    }

    @Test
    @SuppressWarnings("deprecation")
    void rosettaParseResultErrorsReturnsListOfString() {
        RosettaParseResult result = RosettaParserFacade.parseString(
            "namespace com.test\ntype Foo:\n  a string (1..1)\n");
        List<String> errors = result.errors();
        assertNotNull(errors);
        assertTrue(errors.isEmpty(), "valid input → no errors");
    }

    @Test
    @SuppressWarnings("deprecation")
    void legacyFormatPreservedOnMalformedInput() {
        RosettaParseResult result = RosettaParserFacade.parseString(
            "namespace com.test\ntype Foo\n  a string (1..1)\n");
        List<String> errors = result.errors();
        assertFalse(errors.isEmpty());
        for (String err : errors) {
            assertTrue(err.matches("^\\d+:\\d+ .*"),
                "Legacy format must be 'line:col message', got: " + err);
        }
    }
}
