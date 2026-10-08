package com.regnosys.rosetta.ast.metadata;

import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.SourceRange;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link RNode#metadata()} introduced in P1.4.1a (H6 / U003).
 *
 * <p>Reserves the metadata slot for D21 IR augmentation hooks. Empty by
 * default; defensively copied at construction; unmodifiable on read.
 */
class RNodeMetadataImmutabilityTest {

    private static final class TestNode extends RNode {
        TestNode() { super(); }
        TestNode(SourceRange r) { super(r); }
        TestNode(SourceRange r, Map<String, Object> m) { super(r, m); }
        @Override public List<? extends RNode> children() { return List.of(); }
    }

    @Test
    void noArgConstructor_yieldsEmptyMetadata() {
        TestNode n = new TestNode();
        assertNotNull(n.metadata());
        assertTrue(n.metadata().isEmpty());
    }

    @Test
    void sourceRangeOnlyConstructor_yieldsEmptyMetadata() {
        TestNode n = new TestNode(SourceRange.NONE);
        assertTrue(n.metadata().isEmpty());
        assertEquals(SourceRange.NONE, n.sourceRange());
    }

    @Test
    void fullConstructor_populatesMetadata() {
        TestNode n = new TestNode(SourceRange.NONE, Map.of("k", "v"));
        assertEquals("v", n.metadata().get("k"));
    }

    @Test
    void metadata_isUnmodifiableOnRead() {
        TestNode n = new TestNode(SourceRange.NONE, Map.of("k", "v"));
        assertThrows(UnsupportedOperationException.class,
                () -> n.metadata().put("x", "y"));
    }

    @Test
    void metadata_defensiveCopyAtConstruction() {
        Map<String, Object> input = new HashMap<>();
        input.put("a", 1);
        TestNode n = new TestNode(SourceRange.NONE, input);
        input.put("b", 2);   // mutate after construction
        assertEquals(1, n.metadata().size());
        assertEquals(1, n.metadata().get("a"));
    }

    @Test
    void metadata_nullInputCoercedToEmpty() {
        TestNode n = new TestNode(SourceRange.NONE, null);
        assertTrue(n.metadata().isEmpty());
    }
}
