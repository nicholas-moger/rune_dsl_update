package org.finos.rune.equivalence;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Corpus-free locks on the O1 comparator's verdict semantics. The
 * cross-classloader twin walk is exercised for real by the corpus-gated pair
 * smoke (two loaders over one compiled tree); these fixtures lock the
 * shared-branch behaviour the walk composes from.
 */
class ReflectiveDeepCompareUnitTest {

    enum Colour { RED, GREEN }

    record Node(String name, List<String> tags) {}

    @Test
    void equalScalarsAndNullsCompareEqual() {
        assertNull(ReflectiveDeepCompare.firstDivergence(null, null));
        assertNull(ReflectiveDeepCompare.firstDivergence("x", "x"));
        assertNull(ReflectiveDeepCompare.firstDivergence(Colour.RED, Colour.RED));
        assertNull(ReflectiveDeepCompare.firstDivergence(List.of("a", "b"), List.of("a", "b")));
    }

    @Test
    void oneSidedNullReportsThePath() {
        String d = ReflectiveDeepCompare.firstDivergence("x", null);
        assertNotNull(d);
        assertTrue(d.contains("one-sided null"), d);
    }

    @Test
    void valueAndEnumDifferencesReport() {
        assertNotNull(ReflectiveDeepCompare.firstDivergence("x", "y"));
        String d = ReflectiveDeepCompare.firstDivergence(Colour.RED, Colour.GREEN);
        assertNotNull(d);
        assertTrue(d.contains("enum differs"), d);
    }

    @Test
    void listDifferencesReportSizeThenElementPath() {
        String size = ReflectiveDeepCompare.firstDivergence(List.of("a"), List.of("a", "b"));
        assertNotNull(size);
        assertTrue(size.contains("list size differs"), size);
        String elem = ReflectiveDeepCompare.firstDivergence(List.of("a", "b"), List.of("a", "c"));
        assertNotNull(elem);
        assertTrue(elem.contains("[1]"), elem);
    }

    @Test
    void sameClassObjectsUseValueEquality() {
        assertNull(ReflectiveDeepCompare.firstDivergence(
                new Node("n", List.of("t")), new Node("n", List.of("t"))));
        assertNotNull(ReflectiveDeepCompare.firstDivergence(
                new Node("n", List.of("t")), new Node("m", List.of("t"))));
    }

    @Test
    void unrelatedTypesReportTypeDivergence() {
        String d = ReflectiveDeepCompare.firstDivergence("x", 1);
        assertNotNull(d);
        assertTrue(d.contains("type differs"), d);
    }
}
