package com.regnosys.rosetta.harness.matrix;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Canonicaliser} — byte-level canonicalisation used
 * by the content-addressable test cache (H24) to derive stable cache keys
 * across Windows dev machines and Linux CI.
 *
 * <p>P1.2 audit hook H23. Policy per audit Q11: strip UTF-8 BOM, convert
 * CRLF to LF, <em>do not</em> trim trailing whitespace (would corrupt
 * indented string literals). Idempotence invariant is pinned by the
 * companion Jqwik property.
 */
class CanonicaliserTest {

    @Test
    void strips_leading_utf8_bom() {
        String bom = "﻿";
        assertEquals("hello", Canonicaliser.canonicalise(bom + "hello"));
    }

    @Test
    void bom_only_at_start_is_stripped_not_internal() {
        // An internal U+FEFF (zero-width no-break space) is a valid literal
        // character inside a document and must be preserved.
        String internal = "hello﻿world";
        assertEquals("hello﻿world", Canonicaliser.canonicalise(internal));
    }

    @Test
    void converts_crlf_to_lf() {
        assertEquals("a\nb\nc", Canonicaliser.canonicalise("a\r\nb\r\nc"));
    }

    @Test
    void leaves_lone_cr_alone() {
        // Classic-Mac line endings are rare in the corpus; leave them so
        // canonicalisation remains precise about what it changes.
        assertEquals("a\rb", Canonicaliser.canonicalise("a\rb"));
    }

    @Test
    void does_not_trim_trailing_whitespace() {
        // Q11 — trailing-whitespace trim would corrupt indented string
        // literals. Preserve every trailing space.
        String src = "type Foo:\n    x string (1..1)   \n";
        assertEquals(src, Canonicaliser.canonicalise(src));
    }

    @Test
    void empty_input_is_empty_output() {
        assertEquals("", Canonicaliser.canonicalise(""));
    }

    @Test
    void null_input_throws() {
        assertThrows(NullPointerException.class,
                () -> Canonicaliser.canonicalise(null));
    }

    @Test
    void bom_and_crlf_compose() {
        String src = "﻿a\r\nb\r\n";
        assertEquals("a\nb\n", Canonicaliser.canonicalise(src));
    }

    @Test
    void canon_of_canon_is_canon() {
        // Spot-check of idempotence — the property test sweeps this
        // invariant across random inputs.
        String src = "﻿namespace x\r\ntype Foo:\r\n";
        String once = Canonicaliser.canonicalise(src);
        String twice = Canonicaliser.canonicalise(once);
        assertEquals(once, twice);
    }
}
