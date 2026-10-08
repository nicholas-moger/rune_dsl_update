package com.regnosys.rosetta.generator.java.function;

import com.rosetta.util.types.JavaClass;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link RenderedStatement}. Guards the three invariants
 * the record relies on — non-null construction, unmodifiable refs view,
 * and pre-construction snapshot of the caller-supplied set. Full
 * rationale in PR-A plan Step 1.1 (the development plan "2026-04-20-pr-a-import-scan-rewrite").
 */
class RenderedStatementTest {

    @Test
    void empty_factory_yields_empty_refs() {
        RenderedStatement rs = RenderedStatement.empty("x = 1;");
        assertEquals("x = 1;", rs.source());
        assertTrue(rs.refs().isEmpty());
    }

    @Test
    void constructor_requires_non_null_source_and_refs() {
        assertThrows(NullPointerException.class,
            () -> new RenderedStatement(null, Set.of()));
        assertThrows(NullPointerException.class,
            () -> new RenderedStatement("x;", null));
    }

    @Test
    void refs_view_is_unmodifiable_after_construction() {
        // Construction takes a defensive copy so callers can't mutate
        // the contents after wrapping. This is load-bearing: multiple
        // RenderedStatement instances share the FunctionGenerator
        // expressionRefs collection pattern, and accidental mutation
        // would leak across functions.
        Set<JavaClass<?>> caller = new HashSet<>();
        caller.add(JavaClass.from(BigDecimal.class));
        RenderedStatement rs = new RenderedStatement("x;", caller);
        caller.add(JavaClass.from(String.class));   // mutate AFTER construction
        assertEquals(1, rs.refs().size(),
            "RenderedStatement must snapshot refs at construction");
        assertThrows(UnsupportedOperationException.class,
            () -> rs.refs().add(JavaClass.from(Integer.class)),
            "refs() must return an unmodifiable view");
    }
}
