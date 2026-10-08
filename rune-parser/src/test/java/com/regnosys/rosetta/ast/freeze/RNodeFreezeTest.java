package com.regnosys.rosetta.ast.freeze;

import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.SourceRange;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link RNode#freeze()} and post-freeze setter guards
 * introduced in P1.4.1a (D12 / U002).
 */
class RNodeFreezeTest {

    private static final class Leaf extends RNode {
        @Override public List<? extends RNode> children() { return List.of(); }
    }

    private static final class Branch extends RNode {
        private final List<? extends RNode> kids;
        Branch(List<? extends RNode> kids) { this.kids = kids; }
        @Override public List<? extends RNode> children() { return kids; }
    }

    @Test
    void newNode_isNotFrozen() {
        Leaf n = new Leaf();
        assertFalse(n.isFrozen());
    }

    @Test
    void freeze_setsFrozenTrue() {
        Leaf n = new Leaf();
        n.freeze();
        assertTrue(n.isFrozen());
    }

    @Test
    void freeze_isIdempotent() {
        Leaf n = new Leaf();
        n.freeze();
        n.freeze();   // second call must not throw
        assertTrue(n.isFrozen());
    }

    @Test
    void freeze_recursesIntoChildren() {
        Leaf c1 = new Leaf();
        Leaf c2 = new Leaf();
        Branch parent = new Branch(List.of(c1, c2));
        parent.freeze();
        assertTrue(parent.isFrozen());
        assertTrue(c1.isFrozen());
        assertTrue(c2.isFrozen());
    }

    @Test
    void setSourceRangeAfterFreeze_throwsIllegalStateException() {
        Leaf n = new Leaf();
        n.freeze();
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> n.setSourceRange(SourceRange.NONE));
        assertTrue(ex.getMessage().contains("post-freeze"),
                "expected 'post-freeze' in message, got: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("Leaf"),
                "expected 'Leaf' (class name) in message, got: " + ex.getMessage());
    }

    @Test
    void setTokenRangesAfterFreeze_throws() {
        Leaf n = new Leaf();
        n.freeze();
        assertThrows(IllegalStateException.class,
                () -> n.setTokenRanges(java.util.Map.of()));
    }

    @Test
    void putTokenRangeAfterFreeze_throws() {
        Leaf n = new Leaf();
        n.freeze();
        assertThrows(IllegalStateException.class,
                () -> n.putTokenRange("k", SourceRange.NONE));
    }

    @Test
    void setParentAfterFreeze_throws() {
        Leaf n = new Leaf();
        n.freeze();
        assertThrows(IllegalStateException.class,
                () -> n.setParent(new Leaf()));
    }
}
