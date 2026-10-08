package com.regnosys.rosetta.generator.java;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The positive control for {@link Drr7Corpus#gate}: both modes measured, so the
 * instrument is proven able to fail (LAW 60 applied to the gate itself). Runs without
 * the corpus — the verdict is passed in, never derived here.
 */
class Drr7CorpusGateTest {

    @Test
    void a_present_corpus_passes_through_unchanged() {
        assertTrue(Drr7Corpus.gate(true, Drr7CorpusGateTest.class));
    }

    @Test
    void an_absent_corpus_skips_by_default() {
        withProperty(null, () -> assertFalse(Drr7Corpus.gate(false, Drr7CorpusGateTest.class)));
    }

    @Test
    void an_absent_corpus_fails_loudly_when_required() {
        withProperty("true", () -> {
            AssertionError e = assertThrows(AssertionError.class,
                    () -> Drr7Corpus.gate(false, Drr7CorpusGateTest.class));
            assertTrue(e.getMessage().startsWith("[DRR7-CORPUS ABSENT] Drr7CorpusGateTest"),
                    "the failure names the tag and the suite: " + e.getMessage());
            assertTrue(e.getMessage().contains("-D" + Drr7Corpus.REQUIRED_PROPERTY + "=true"),
                    "the failure names the property that armed it: " + e.getMessage());
        });
    }

    @Test
    void a_present_corpus_is_never_failed_by_the_property() {
        withProperty("true", () -> assertTrue(Drr7Corpus.gate(true, Drr7CorpusGateTest.class)));
    }

    /**
     * Sets (or clears, for {@code null}) the property around {@code body}, restoring the prior
     * value — and re-arms the once-per-JVM report afterwards, so this control never consumes
     * the one report a genuinely absent corpus would print later in the same fork (the #606
     * review's SF9).
     */
    private static void withProperty(String value, Runnable body) {
        String prior = System.getProperty(Drr7Corpus.REQUIRED_PROPERTY);
        try {
            if (value == null) {
                System.clearProperty(Drr7Corpus.REQUIRED_PROPERTY);
            } else {
                System.setProperty(Drr7Corpus.REQUIRED_PROPERTY, value);
            }
            body.run();
        } finally {
            if (prior == null) {
                System.clearProperty(Drr7Corpus.REQUIRED_PROPERTY);
            } else {
                System.setProperty(Drr7Corpus.REQUIRED_PROPERTY, prior);
            }
            Drr7Corpus.resetReportForTest();
        }
    }
}
