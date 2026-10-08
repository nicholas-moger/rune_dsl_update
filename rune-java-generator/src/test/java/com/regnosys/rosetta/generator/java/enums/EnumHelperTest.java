package com.regnosys.rosetta.generator.java.enums;

import com.regnosys.rosetta.ast.supporting.REnumValue;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EnumHelperTest {

    // === Already uppercase — no formatting needed ============================

    @Test void all_caps_unchanged() {
        assertEquals("ACTIVE", EnumHelper.formatEnumName("ACTIVE"));
    }

    @Test void all_caps_with_underscore_unchanged() {
        assertEquals("ACTIVE_TRADE", EnumHelper.formatEnumName("ACTIVE_TRADE"));
    }

    @Test void all_caps_with_number_unchanged() {
        assertEquals("ISO20022", EnumHelper.formatEnumName("ISO20022"));
    }

    // === CamelCase conversion ================================================

    @Test void simple_camel_case() {
        assertEquals("ACTIVE_TRADE", EnumHelper.formatEnumName("ActiveTrade"));
    }

    @Test void lower_camel_case() {
        assertEquals("ACTIVE_TRADE", EnumHelper.formatEnumName("activeTrade"));
    }

    // === Numbers =============================================================

    @Test void starts_with_number_gets_underscore_prefix() {
        assertEquals("_3_MONTH_RATE", EnumHelper.formatEnumName("3MonthRate"));
    }

    @Test void all_caps_with_number_no_change() {
        // Already all-caps: no formatting needed
        assertEquals("ISO20022", EnumHelper.formatEnumName("ISO20022"));
    }

    @Test void camel_case_with_number() {
        // "Iso" is camelCase, "20022" is a number segment
        assertEquals("ISO_20022", EnumHelper.formatEnumName("Iso20022"));
    }

    // === Separators ==========================================================

    @Test void dots_replaced() {
        assertEquals("COM_EXAMPLE", EnumHelper.formatEnumName("com.example"));
    }

    @Test void dashes_replaced() {
        assertEquals("SOME_VALUE", EnumHelper.formatEnumName("some-value"));
    }

    @Test void spaces_replaced() {
        assertEquals("SOME_VALUE", EnumHelper.formatEnumName("some value"));
    }

    // === Real-world enum values from CDM =====================================

    @Test void currency_code_unchanged() {
        assertEquals("AED", EnumHelper.formatEnumName("AED"));
    }

    @Test void active_enum() {
        assertEquals("ACTIVE", EnumHelper.formatEnumName("Active"));
    }

    // === Caret-escape stripping (P2.1.1 R4 F14; Cluster A.3 lock) ============
    //
    // The caret prefix ({@code ^}) on a Rosetta identifier is a parser-escape
    // marker telling the parser "treat as identifier, not keyword" (e.g. DRR
    // {@code NonFinancialSectorEnum.^E}). It is NOT part of the identifier
    // itself; codegen must drop it before any case-conversion. Without the
    // strip, {@code formatEnumName("^E")} splits at camelCase boundaries to
    // produce {@code "^_E"}, which is invalid Java. See cluster-a-fix-design
    // § 9.2 + § 9.4 for the architectural rationale.

    @Test void strip_escape_null_returns_null() {
        assertNull(EnumHelper.stripEscape(null));
    }

    @Test void strip_escape_caret_prefix_removed() {
        assertEquals("E", EnumHelper.stripEscape("^E"));
    }

    @Test void strip_escape_no_caret_returns_unchanged() {
        assertEquals("Active", EnumHelper.stripEscape("Active"));
    }

    @Test void strip_escape_caret_in_middle_returns_unchanged() {
        // Only LEADING caret is the parser-escape marker; embedded caret stays.
        assertEquals("foo^bar", EnumHelper.stripEscape("foo^bar"));
    }

    @Test void strip_escape_empty_string_returns_empty() {
        assertEquals("", EnumHelper.stripEscape(""));
    }

    @Test void convert_value_caret_escaped_identifier() {
        // Cluster A.3 regression lock: ^E → E (not ^_E).
        var v = new REnumValue();
        v.setName("^E");
        assertEquals("E", EnumHelper.convertValue(v),
                "caret-escaped enum value name must be stripped before formatEnumName; "
                        + "without the strip the result is ^_E (invalid Java) — see Cluster A.3");
    }

    @Test void convert_value_unescaped_identifier_unchanged_path() {
        // Bare identifier flows through stripEscape unchanged + formatEnumName as usual.
        var v = new REnumValue();
        v.setName("ActiveTrade");
        assertEquals("ACTIVE_TRADE", EnumHelper.convertValue(v));
    }
}
