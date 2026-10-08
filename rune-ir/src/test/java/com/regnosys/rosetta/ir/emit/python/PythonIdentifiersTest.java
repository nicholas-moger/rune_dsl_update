package com.regnosys.rosetta.ir.emit.python;

import com.regnosys.rosetta.ir.emit.EmitterException;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PythonIdentifiersTest {

    @Test void sanitizesHardKeyword() {
        assertEquals("global_", PythonIdentifiers.sanitize("global"));
        assertEquals("None_", PythonIdentifiers.sanitize("None"));
        assertEquals("return_", PythonIdentifiers.sanitize("return"));
    }

    @Test void passesNonKeywordThrough() {
        assertEquals("foo", PythonIdentifiers.sanitize("foo"));
        assertEquals("global_", PythonIdentifiers.sanitize("global_")); // a literal keyword_ is not a keyword
    }

    @Test void declinesUnsafe() {
        assertThrows(EmitterException.class, () -> PythonIdentifiers.sanitize(null));
        assertThrows(EmitterException.class, () -> PythonIdentifiers.sanitize(""));
        assertThrows(EmitterException.class, () -> PythonIdentifiers.sanitize("café"));
        assertThrows(EmitterException.class, () -> PythonIdentifiers.sanitize("a.b")); // dotted declines (no namespace handling)
    }

    @Test void safeNameStripsNamespaceThenSanitizes() {
        assertEquals("Trade", PythonIdentifiers.safeName("ns.Trade"));
        assertEquals("global_", PythonIdentifiers.safeName("ns.global"));
        assertEquals("Trade", PythonIdentifiers.safeName("Trade"));
    }

    @Test void snakeNameLowersCamelAndPascal() {
        assertEquals("day_count_fraction", PythonIdentifiers.snakeName("dayCountFraction"));
        assertEquals("trade_date", PythonIdentifiers.snakeName("TradeDate"));
        assertEquals("payer_receiver", PythonIdentifiers.snakeName("payerReceiver"));
    }

    @Test void snakeNameHandlesAcronymRuns() {
        assertEquals("isda_fix_number", PythonIdentifiers.snakeName("ISDAFixNumber"));
        assertEquals("http_server", PythonIdentifiers.snakeName("HTTPServer"));
        assertEquals("lei_value", PythonIdentifiers.snakeName("LEIValue"));
        assertEquals("notional_usd_value", PythonIdentifiers.snakeName("notionalUSDValue")); // embedded run -> pass 1 fires
        assertEquals("ur_ls", PythonIdentifiers.snakeName("URLs"));                          // greedy-backtrack, expected
    }

    @Test void snakeNameHandlesDigitBoundariesAndAlreadySnake() {
        assertEquals("party1_a", PythonIdentifiers.snakeName("party1A"));
        assertEquals("iso20022", PythonIdentifiers.snakeName("iso20022"));
        assertEquals("day_count", PythonIdentifiers.snakeName("day_count"));   // already snake -> unchanged
    }

    @Test void snakeNameIsIdempotent() {
        for (String s : java.util.List.of("dayCountFraction", "ISDAFixNumber", "notionalUSDValue",
                "URLs", "party1A", "Return", "_x", "day_count")) {
            String once = PythonIdentifiers.snakeName(s);
            assertEquals(once, PythonIdentifiers.snakeName(once), "snakeName not idempotent on " + s);
        }
    }

    @Test void snakeNameSanitizesKeywordAfterSnaking() {
        assertEquals("return_", PythonIdentifiers.snakeName("Return")); // -> "return" (keyword) -> "return_"
        assertEquals("class_", PythonIdentifiers.snakeName("Class"));
        assertEquals("global_", PythonIdentifiers.snakeName("global")); // snake no-op, keyword
        assertEquals("none", PythonIdentifiers.snakeName("None"));       // -> "none" is NOT a keyword -> legal, unsuffixed
    }

    @Test void snakeNamePreservesLeadingUnderscore() {
        assertEquals("_x", PythonIdentifiers.snakeName("_x"));
        assertEquals("_x", PythonIdentifiers.snakeName("_X"));
    }

    @Test void snakeNameDeclinesNullAndNonAscii() {
        assertThrows(EmitterException.class, () -> PythonIdentifiers.snakeName(null));
        assertThrows(EmitterException.class, () -> PythonIdentifiers.snakeName("naïve")); // non-ASCII -> decline
        assertThrows(EmitterException.class, () -> PythonIdentifiers.snakeName(""));           // blank -> decline
    }

    @Test void snakeNameLowercasingIsLocaleIndependent() {
        // A tr/az default locale would map capital 'I' -> dotless 'ı' (U+0131, non-ASCII) under a bare
        // toLowerCase(); Locale.ROOT prevents that. Force the hostile locale to lock the regression.
        Locale prior = Locale.getDefault();
        try {
            Locale.setDefault(Locale.of("tr", "TR"));
            assertEquals("isda", PythonIdentifiers.snakeName("ISDA"));
            assertEquals("id", PythonIdentifiers.snakeName("ID"));
        } finally {
            Locale.setDefault(prior);
        }
    }
}
