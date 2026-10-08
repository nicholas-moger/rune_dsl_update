package com.regnosys.rosetta.ir.contract;

import com.regnosys.rosetta.ir.core.IRKind;
import com.regnosys.rosetta.ir.core.IRNode;
import com.regnosys.rosetta.ir.core.Metadata;
import com.regnosys.rosetta.ir.core.MetadataKey;
import com.regnosys.rosetta.ir.core.SourceRange;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Locks the {@link Metadata} contract via a test-scope {@code RecordIRNode}
 * fixture (per spec Section 7 reviewer fix I4).
 *
 * <p>Exercises the {@link IRNode} default contract for {@link IRNode#children()}
 * (unmodifiable, non-null), {@link IRNode#sourceRange()} (non-null
 * {@code Optional}), and {@link IRNode#metadata()} (non-null
 * {@link Metadata}).
 *
 * <p><b>MetadataKey equality:</b> the test-scope {@code MetadataKey}
 * instances below are anonymous inner classes (so reference-equality on the
 * key wrapper itself does NOT hold across declarations). This is fine
 * because {@link Metadata#getAs(MetadataKey)} contract is implemented via
 * {@code key.name()} as the lookup key — the wrapper is just a typed envelope
 * carrying the string name and the value class. Production
 * {@code MetadataKey} implementations should be effectively-immutable
 * singletons ({@code public static final ...}) to keep call sites readable
 * and to keep the wrapper cheap.
 */
class MetadataContractTest {

    private static final MetadataKey<String> STRING_KEY = new MetadataKey<>() {
        @Override public String name() { return "test.stringKey"; }
        @Override public Class<String> type() { return String.class; }
    };
    private static final MetadataKey<Integer> INT_KEY = new MetadataKey<>() {
        @Override public String name() { return "test.intKey"; }
        @Override public Class<Integer> type() { return Integer.class; }
    };

    /**
     * Test-scope {@link Metadata} implementation. Stores values keyed by
     * {@code MetadataKey.name()}; type-checks via {@code key.type().isInstance(value)}
     * before unsafe-cast in {@link #getAs(MetadataKey)}.
     */
    private static final class RecordMetadata implements Metadata {
        private final Map<String, Object> store;

        RecordMetadata(Map<String, Object> store) {
            this.store = Map.copyOf(store);
        }

        @Override
        public Optional<Object> get(String key) {
            return Optional.ofNullable(store.get(key));
        }

        @Override
        public Set<String> keys() {
            return store.keySet();
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> Optional<T> getAs(MetadataKey<T> key) {
            Object v = store.get(key.name());
            if (v == null) return Optional.empty();
            if (!key.type().isInstance(v)) return Optional.empty();
            return Optional.of((T) v);
        }
    }

    /**
     * Test-scope {@link IRNode} record. Exercises the IRNode default contract.
     */
    private record RecordIRNode(
            String name,
            IRKind kind,
            Optional<SourceRange> sourceRange,
            List<? extends IRNode> children,
            Metadata metadata
    ) implements IRNode {}

    private static IRNode fixture(Map<String, Object> meta) {
        return new RecordIRNode(
                "test.Foo",
                IRKind.STRUCT,
                Optional.of(new SourceRange(0, 10, URI.create("test:///foo"))),
                List.of(),
                new RecordMetadata(meta));
    }

    @Test
    void metadataNonNullOnIRNode() {
        IRNode node = fixture(Map.of());
        assertNotNull(node.metadata(), "IRNode.metadata() must be non-null");
    }

    @Test
    void metadataGetUnknownKeyReturnsEmpty() {
        Metadata m = fixture(Map.of("a", 1)).metadata();
        assertTrue(m.get("nonexistent").isEmpty());
    }

    @Test
    void metadataGetAsTypedExtractsCorrectly() {
        Metadata m = fixture(Map.of("test.stringKey", "hello", "test.intKey", 42)).metadata();
        assertEquals(Optional.of("hello"), m.getAs(STRING_KEY));
        assertEquals(Optional.of(42), m.getAs(INT_KEY));
    }

    @Test
    void metadataGetAsTypeMismatchReturnsEmpty() {
        // Wrong-typed value stored against a typed key → must NOT cast unsafely.
        // 42 here is autoboxed to Integer; this is the common silent-bug shape
        // (a generator stores the wrong primitive type) and the type-guard in
        // RecordMetadata.getAs() uses isInstance() which correctly rejects it.
        Metadata m = fixture(Map.of("test.stringKey", 42)).metadata();
        assertTrue(m.getAs(STRING_KEY).isEmpty(),
                "getAs() must return empty when stored value is not an instance of key.type()");
    }

    @Test
    void metadataKeysNonNullReflectsContents() {
        Metadata m = fixture(Map.of("a", 1, "b", 2)).metadata();
        assertNotNull(m.keys());
        assertEquals(Set.of("a", "b"), m.keys());
    }

    @Test
    void irNodeChildrenUnmodifiableNonNull() {
        // NOTE: this exercises the immutability of List.of() rather than an
        // IRNode-contract guarantee — the IRNode interface itself does not
        // enforce unmodifiable children. Production records carry the
        // unmodifiable invariant by convention via List.copyOf in their
        // canonical constructors (per the IRNode Javadoc "Tree walk;
        // unmodifiable" doc-contract).
        IRNode node = fixture(Map.of());
        assertNotNull(node.children());
        @SuppressWarnings({"unchecked", "rawtypes"})
        List rawChildren = node.children();
        assertThrows(UnsupportedOperationException.class,
                () -> rawChildren.add(node),
                "IRNode.children() must be unmodifiable");
    }

    @Test
    void irNodeSourceRangeNonNullOptional() {
        IRNode node = fixture(Map.of());
        assertNotNull(node.sourceRange());
        assertTrue(node.sourceRange().isPresent());
    }

    @Test
    void irNodeSourceRangeEmptyAllowed() {
        IRNode node = new RecordIRNode(
                "x", IRKind.STRUCT, Optional.empty(),
                List.of(), new RecordMetadata(Map.of()));
        assertTrue(node.sourceRange().isEmpty(),
                "IRNode.sourceRange() may legitimately be empty (an importer without source offsets)");
    }
}
