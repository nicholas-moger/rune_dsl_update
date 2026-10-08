package com.regnosys.rosetta.harness.matrix;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class VersionTest {

    @ParameterizedTest
    @CsvSource({
            "'6.16.0',        6, 16, 0, ''",
            "'7.0.0',         7,  0, 0, ''",
            "'1.36.0',        1, 36, 0, ''",
            "'7.0.0-asc.96',  7,  0, 0, 'asc.96'",
            "'7.0.0-dev.111', 7,  0, 0, 'dev.111'",
            "'6.28.0',        6, 28, 0, ''",
            "'5.59.0',        5, 59, 0, ''",
    })
    void parses_canonical_and_qualified_forms(
            String raw, int major, int minor, int patch, String qualifier) {
        Version v = Version.parse(raw);
        assertEquals(major, v.major());
        assertEquals(minor, v.minor());
        assertEquals(patch, v.patch());
        assertEquals(qualifier, v.qualifier());
    }

    @Test
    void display_round_trips_canonical_form() {
        assertEquals("6.16.0", Version.parse("6.16.0").display());
        assertEquals("7.0.0-asc.96", Version.parse("7.0.0-asc.96").display());
    }

    @Test
    void ordering_is_major_then_minor_then_patch_then_qualifier() {
        Version v610 = Version.parse("6.10.0");
        Version v612 = Version.parse("6.12.0");
        Version v616 = Version.parse("6.16.0");
        Version v700 = Version.parse("7.0.0");
        Version v700asc = Version.parse("7.0.0-asc.96");
        Version v700dev = Version.parse("7.0.0-dev.111");

        assertTrue(v610.compareTo(v612) < 0);
        assertTrue(v612.compareTo(v616) < 0);
        assertTrue(v616.compareTo(v700) < 0);
        // qualifier "asc.96" < "dev.111" by plain String.compareTo
        assertTrue(v700.compareTo(v700asc) < 0, "no-qualifier sorts before qualified");
        assertTrue(v700asc.compareTo(v700dev) < 0);
    }

    @Test
    void equality_is_component_wise() {
        assertEquals(Version.parse("6.16.0"), Version.parse("6.16.0"));
        assertNotEquals(Version.parse("6.16.0"), Version.parse("6.16.1"));
        assertNotEquals(Version.parse("7.0.0"), Version.parse("7.0.0-asc.96"));
    }

    @Test
    void parse_rejects_malformed_input() {
        assertThrows(IllegalArgumentException.class, () -> Version.parse("6.16"));
        assertThrows(IllegalArgumentException.class, () -> Version.parse("six.sixteen.zero"));
        assertThrows(IllegalArgumentException.class, () -> Version.parse(""));
        assertThrows(NullPointerException.class, () -> Version.parse(null));
    }

    @Test
    void parse_rejects_trailing_dash_with_empty_qualifier() {
        // `parse(x).display()` must round-trip; "6.16.0-" would lose the dash on
        // display and break that invariant. Reject as malformed instead.
        assertThrows(IllegalArgumentException.class, () -> Version.parse("6.16.0-"));
    }

    @Test
    void constructor_rejects_negative_components() {
        assertThrows(IllegalArgumentException.class, () -> new Version(-1, 0, 0, ""));
        assertThrows(IllegalArgumentException.class, () -> new Version(0, -1, 0, ""));
        assertThrows(IllegalArgumentException.class, () -> new Version(0, 0, -1, ""));
    }

    @Test
    void constructor_rejects_null_qualifier() {
        assertThrows(NullPointerException.class, () -> new Version(1, 0, 0, null));
    }
}
