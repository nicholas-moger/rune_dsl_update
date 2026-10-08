package com.regnosys.rosetta.symbols.symbolid;

import com.regnosys.rosetta.symbols.SymbolId;
import net.jqwik.api.*;
import net.jqwik.api.constraints.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for {@link SymbolId} invariants.
 *
 * <p>3 Jqwik {@code @Property} tests; 1000 random cases each (controlled by
 * {@code jqwik.tries.default} in the maven-surefire config). Covers:
 * <ol>
 *   <li>fqn() round-trip via last-dot split</li>
 *   <li>equals / hashCode by value, independent of whether {@code of()} or
 *       the canonical constructor was used</li>
 *   <li>Different generation → not equal (generation is part of identity)</li>
 * </ol>
 *
 * <p>Part of P1.4.1b Task 6.1 (plan §6.1).
 */
class SymbolIdPropertyTest {

    @Property
    void fqn_reversibleViaLastDot(
            @ForAll("namespaceArb") String namespace,
            @ForAll @AlphaChars @StringLength(min = 1, max = 20) String localName,
            @ForAll long generation) {
        SymbolId id = SymbolId.of(namespace, localName, generation);
        String fqn = id.fqn();
        if (namespace.isEmpty()) {
            // Root namespace — fqn is just the localName, no dot prefix.
            assertEquals(localName, fqn);
        } else {
            int lastDot = fqn.lastIndexOf('.');
            assertEquals(namespace, fqn.substring(0, lastDot));
            assertEquals(localName, fqn.substring(lastDot + 1));
        }
    }

    /**
     * Generates namespace strings covering both edge cases ({@code ""} for the
     * root namespace) and dotted multi-segment namespaces (so the last-dot
     * split path is exercised meaningfully). Per R13-1: prior {@code @AlphaChars
     * @StringLength(min = 1, max = 20)} made the root-namespace branch
     * unreachable AND only generated single-segment names that didn't exercise
     * the dot-split.
     */
    @Provide
    Arbitrary<String> namespaceArb() {
        Arbitrary<String> empty = Arbitraries.just("");
        Arbitrary<String> singleSegment = Arbitraries.strings()
                .alpha()
                .ofMinLength(1)
                .ofMaxLength(20);
        Arbitrary<String> dottedSegments = Arbitraries.strings()
                .alpha()
                .ofMinLength(1)
                .ofMaxLength(8)
                .list().ofMinSize(2).ofMaxSize(4)
                .map(parts -> String.join(".", parts));
        return Arbitraries.oneOf(empty, singleSegment, dottedSegments);
    }

    @Property
    void equalsByValue_independentOfConstruction(
            @ForAll @AlphaChars @StringLength(min = 1, max = 20) String namespace,
            @ForAll @AlphaChars @StringLength(min = 1, max = 20) String localName,
            @ForAll long generation) {
        SymbolId a = SymbolId.of(namespace, localName, generation);
        SymbolId b = new SymbolId(namespace, localName, generation);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Property
    void differentGeneration_notEqual(
            @ForAll @AlphaChars @StringLength(min = 1, max = 20) String namespace,
            @ForAll @AlphaChars @StringLength(min = 1, max = 20) String localName,
            @ForAll long g1,
            @ForAll long g2) {
        Assume.that(g1 != g2);
        assertNotEquals(SymbolId.of(namespace, localName, g1),
                        SymbolId.of(namespace, localName, g2));
    }
}
