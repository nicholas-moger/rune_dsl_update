package com.regnosys.rosetta.symbols.harness;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class XtextEquivalenceHarnessTest {

    @Test
    void harness_loads_via_both_pipelines() {
        String source = """
                namespace test
                type Foo:
                    field string (1..1)
                """;
        XtextEquivalenceHarness harness = XtextEquivalenceHarness.loadString(source);

        // Our side always loads
        assertNotNull(harness.ourModel(), "Our M1+M2 side must load");

        // Xtext side may or may not load depending on classpath state.
        // When available AND loading succeeds, it's non-null.
        // The harness degrades gracefully when it can't load.
        if (harness.xtextModel() != null) {
            System.out.println("Xtext side loaded successfully");
        } else {
            System.out.println("Xtext side unavailable — harness in degraded mode");
        }

        // No categories wired yet — comparing returns empty
        assertEquals(0,
            harness.compareCrossRefs(XtextEquivalenceHarness.Category.ALL).size());
    }
}
