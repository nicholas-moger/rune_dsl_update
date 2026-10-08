package com.regnosys.rosetta.symbols.symbolid;

import com.regnosys.rosetta.symbols.SymbolId;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SymbolIdTest {

    @Test
    void of_buildsRecordWithGivenValues() {
        SymbolId id = SymbolId.of("com.foo", "Bar", 42L);
        assertEquals("com.foo", id.namespace());
        assertEquals("Bar", id.localName());
        assertEquals(42L, id.generation());
    }

    @Test
    void fqn_concatenatesNamespaceAndLocalNameWithDot() {
        assertEquals("com.foo.Bar", SymbolId.of("com.foo", "Bar", 1L).fqn());
    }

    @Test
    void fqn_emptyNamespace_omitsLeadingDot() {
        // Root namespace (no qualifier) — fqn is just the local name.
        assertEquals("Bar", SymbolId.of("", "Bar", 1L).fqn());
    }

    @Test
    void of_nullNamespace_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> SymbolId.of(null, "Bar", 1L));
    }

    @Test
    void of_nullLocalName_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> SymbolId.of("com.foo", null, 1L));
    }

    @Test
    void of_emptyOrWhitespaceLocalName_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> SymbolId.of("com.foo", "", 1L));
        assertThrows(IllegalArgumentException.class, () -> SymbolId.of("com.foo", "   ", 1L));
    }

    @Test
    void of_whitespaceNamespace_throwsIllegalArgumentException() {
        // Whitespace-only namespace would produce a nonsensical fqn() like
        // "   .Foo" and silently break RWorkspace.resolve lookups. Empty
        // string (root) is allowed; sentinel "<builtin>" is non-blank.
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> SymbolId.of("   ", "Foo", 1L));
        assertTrue(ex.getMessage().contains("namespace"));
        // Empty namespace (root) remains valid.
        assertEquals("", SymbolId.of("", "Foo", 1L).namespace());
    }

    @Test
    void equalsAndHashCode_byValue() {
        SymbolId a = SymbolId.of("com.foo", "Bar", 1L);
        SymbolId b = SymbolId.of("com.foo", "Bar", 1L);
        SymbolId differentGen = SymbolId.of("com.foo", "Bar", 2L);
        SymbolId differentNs = SymbolId.of("com.bar", "Bar", 1L);
        SymbolId differentName = SymbolId.of("com.foo", "Baz", 1L);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, differentGen);
        assertNotEquals(a, differentNs);
        assertNotEquals(a, differentName);
    }
}
