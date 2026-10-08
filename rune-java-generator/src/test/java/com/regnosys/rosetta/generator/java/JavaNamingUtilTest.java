package com.regnosys.rosetta.generator.java;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit tests for {@link JavaNamingUtil} — the shared first-letter case-flip
 * utility that must produce identical results across POJO getters/setters,
 * function-body builder chains, validator method names, and alias
 * invocations. Drift between generator layers is a byte-identical gate
 * failure.
 */
class JavaNamingUtilTest {

    // =========================================================================
    // toFirstUpper
    // =========================================================================

    @Test
    void toFirstUpper_capitalises_lowerCamelCase_attribute() {
        assertEquals("AdjustedDate", JavaNamingUtil.toFirstUpper("adjustedDate"));
    }

    @Test
    void toFirstUpper_leaves_interior_chars_unchanged() {
        // Multi-word name — only first char flips; "usinessDayConvention" stays intact.
        assertEquals("BusinessDayConvention",
                JavaNamingUtil.toFirstUpper("businessDayConvention"));
    }

    @Test
    void toFirstUpper_idempotent_when_already_upper() {
        assertEquals("AlreadyUpper", JavaNamingUtil.toFirstUpper("AlreadyUpper"));
    }

    @Test
    void toFirstUpper_single_char() {
        assertEquals("A", JavaNamingUtil.toFirstUpper("a"));
    }

    @Test
    void toFirstUpper_non_letter_first_char_passes_through() {
        // A digit first char has no upper-case equivalent — toUpperCase returns it unchanged.
        assertEquals("1foo", JavaNamingUtil.toFirstUpper("1foo"));
    }

    @Test
    void toFirstUpper_null_returns_null() {
        assertNull(JavaNamingUtil.toFirstUpper(null));
    }

    @Test
    void toFirstUpper_empty_returns_empty() {
        assertEquals("", JavaNamingUtil.toFirstUpper(""));
    }

    // =========================================================================
    // toFirstLower
    // =========================================================================

    @Test
    void toFirstLower_lowers_pascalCase_type() {
        assertEquals("businessEvent", JavaNamingUtil.toFirstLower("BusinessEvent"));
    }

    @Test
    void toFirstLower_idempotent_when_already_lower() {
        assertEquals("alreadyLower", JavaNamingUtil.toFirstLower("alreadyLower"));
    }

    @Test
    void toFirstLower_single_char() {
        assertEquals("a", JavaNamingUtil.toFirstLower("A"));
    }

    @Test
    void toFirstLower_null_returns_null() {
        assertNull(JavaNamingUtil.toFirstLower(null));
    }

    @Test
    void toFirstLower_empty_returns_empty() {
        assertEquals("", JavaNamingUtil.toFirstLower(""));
    }
}
