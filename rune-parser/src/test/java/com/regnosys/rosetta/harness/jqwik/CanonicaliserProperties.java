package com.regnosys.rosetta.harness.jqwik;

import com.regnosys.rosetta.harness.matrix.Canonicaliser;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Jqwik properties pinning the canonicaliser invariants.
 *
 * <p>P1.2 audit hook H23 — the {@code canon(canon(x)) == canon(x)}
 * idempotence property called out in audit Q11.
 */
class CanonicaliserProperties {

    @Provide
    Arbitrary<String> anyString() {
        // Printable ASCII plus the canonicalisation-relevant specials:
        // BOM (U+FEFF), CR, LF, tab.
        Arbitrary<Character> chars = Arbitraries.oneOf(
                Arbitraries.chars().range(' ', '~'),
                Arbitraries.of('\r', '\n', '\t', '﻿'));
        return chars.list().ofMinSize(0).ofMaxSize(80)
                .map(cs -> {
                    StringBuilder sb = new StringBuilder(cs.size());
                    for (Character c : cs) sb.append(c);
                    return sb.toString();
                });
    }

    @Property(tries = 500)
    void canonicalise_is_idempotent(@ForAll("anyString") String input) {
        String once = Canonicaliser.canonicalise(input);
        String twice = Canonicaliser.canonicalise(once);
        assertEquals(once, twice,
                "canonicalise should be idempotent. input=" + debug(input)
                        + " once=" + debug(once) + " twice=" + debug(twice));
    }

    @Property(tries = 500)
    void canonical_form_contains_no_crlf(@ForAll("anyString") String input) {
        String canon = Canonicaliser.canonicalise(input);
        assertFalse(canon.contains("\r\n"),
                "CRLF should be rewritten to LF. input=" + debug(input) + " canon=" + debug(canon));
    }

    @Property(tries = 500)
    void canonical_form_never_starts_with_bom(@ForAll("anyString") String input) {
        String canon = Canonicaliser.canonicalise(input);
        assertFalse(!canon.isEmpty() && canon.charAt(0) == '﻿',
                "Canonical form must not start with U+FEFF (BOM). input=" + debug(input));
    }

    private static String debug(String s) {
        // Escape unprintables so property-test output is readable.
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\r') sb.append("\\r");
            else if (c == '\n') sb.append("\\n");
            else if (c == '\t') sb.append("\\t");
            else if (c == '﻿') sb.append("<BOM>");
            else sb.append(c);
        }
        return sb.append("\"").toString();
    }
}
