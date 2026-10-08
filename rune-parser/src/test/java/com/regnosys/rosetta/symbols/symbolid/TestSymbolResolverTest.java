package com.regnosys.rosetta.symbols.symbolid;

import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.symbols.StaleSymbolIdException;
import com.regnosys.rosetta.symbols.SymbolId;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class TestSymbolResolverTest {

    private static final class Leaf extends RNode {
        Leaf() { super(); }
        @Override public List<RNode> children() { return List.of(); }
    }

    @Test
    void unknownId_returnsNull() {
        TestSymbolResolver r = new TestSymbolResolver();
        assertNull(r.resolve(r.idFor("ns", "Missing"), Leaf.class));
    }

    @Test
    void boundId_returnsTarget() {
        TestSymbolResolver r = new TestSymbolResolver();
        Leaf target = new Leaf();
        r.bind("ns", "Foo", target);
        assertSame(target, r.resolve(r.idFor("ns", "Foo"), Leaf.class));
    }

    @Test
    void staleGeneration_throws() {
        TestSymbolResolver r = new TestSymbolResolver();
        SymbolId stale = SymbolId.of("ns", "Foo", r.generation() + 1);
        assertThrows(StaleSymbolIdException.class, () -> r.resolve(stale, Leaf.class));
    }

    @Test
    void typeMismatch_throws() {
        TestSymbolResolver r = new TestSymbolResolver();
        Leaf target = new Leaf();
        r.bind("ns", "Foo", target);
        assertThrows(IllegalArgumentException.class,
                () -> r.resolve(r.idFor("ns", "Foo"), RDataType.class));
    }

    @Test
    void bind_overwritesExistingEntry() {
        TestSymbolResolver r = new TestSymbolResolver();
        Leaf first = new Leaf();
        Leaf second = new Leaf();
        TestSymbolResolver returned = r.bind("ns", "Foo", first).bind("ns", "Foo", second);
        assertSame(r, returned);
        assertSame(second, r.resolve(r.idFor("ns", "Foo"), Leaf.class));
    }

    @Test
    void rootNamespaceRoundTrips() {
        TestSymbolResolver r = new TestSymbolResolver();
        Leaf target = new Leaf();
        r.bind("", "Foo", target);
        assertSame(target, r.resolve(r.idFor("", "Foo"), Leaf.class));
    }

    @Test
    void wireSuperType_bindsTargetAndInvokesSetter() {
        TestSymbolResolver r = new TestSymbolResolver();
        Leaf source = new Leaf();
        Leaf parent = new Leaf();
        AtomicReference<SymbolId> captured = new AtomicReference<>();
        TestSymbolResolver returned =
                r.wireSuperType(source, "ns", "Parent", parent, captured::set);
        assertSame(r, returned);
        assertEquals(r.idFor("ns", "Parent"), captured.get());
        assertSame(parent, r.resolve(captured.get(), Leaf.class));
    }

    @Test
    void resolve_nullExpected_throwsNullPointerException() {
        TestSymbolResolver r = new TestSymbolResolver();
        // Per SymbolResolver contract: expected null wins over null id
        assertThrows(NullPointerException.class, () -> r.resolve(null, null));
    }

    @Test
    void bind_nullTarget_throwsNullPointerException() {
        // Fail-fast on null target — would otherwise be indistinguishable
        // from an unbound SymbolId at resolve-time, silently masking miswired
        // test fixtures.
        TestSymbolResolver r = new TestSymbolResolver();
        NullPointerException ex = assertThrows(NullPointerException.class,
                () -> r.bind("ns", "Foo", null));
        assertTrue(ex.getMessage().contains("target"));
    }

    @Test
    void resolveTypeLike_typeLikeRRootElement_returnsTarget() {
        // R18 F1 — the default impl resolves via resolve(id, RNode.class) +
        // filters by isTypeLike. RDataType satisfies isTypeLike, so the type
        // is returned.
        TestSymbolResolver r = new TestSymbolResolver();
        RDataType type = new RDataType();
        type.setName("Trade");
        r.bind("ns", "Trade", type);
        assertSame(type, r.resolveTypeLike(r.idFor("ns", "Trade")));
    }

    @Test
    void resolveTypeLike_nonTypeLikeRNode_returnsNull() {
        // R18 F1 — the strict default contract: when the target is not a
        // type-like RRootElement (here a bare RNode subclass), return null
        // instead of silently binding the wrong kind to a type-position
        // lookup. Pre-R18 the default delegated to resolve(id, RNode.class)
        // unconditionally, which would have returned `leaf` and reintroduced
        // the rule-vs-type collision class for test resolvers.
        TestSymbolResolver r = new TestSymbolResolver();
        Leaf leaf = new Leaf();
        r.bind("ns", "NotAType", leaf);
        assertNull(r.resolveTypeLike(r.idFor("ns", "NotAType")));
    }

    @Test
    void resolveTypeLike_unknownId_returnsNull() {
        // R18 F1 — unknown id resolves to null; the default's null-safe
        // pattern-match (`target instanceof RRootElement re`) returns false
        // on null target, so no NPE and the final return null fires.
        TestSymbolResolver r = new TestSymbolResolver();
        assertNull(r.resolveTypeLike(r.idFor("ns", "Missing")));
    }
}
