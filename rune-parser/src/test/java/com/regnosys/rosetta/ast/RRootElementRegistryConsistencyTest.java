package com.regnosys.rosetta.ast;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Registry-consistency guard (restored in the review export from the retired corpus audit test,
 * separated from that test's comparison against a development audit document).
 *
 * <p>The 18-FQN registry in {@link CorpusExpected#EXPECTED} and the 18-entry mapping in
 * {@link IRKindForClass} must enumerate the same {@code RRootElement} subclasses. If a future
 * change adds a subclass to one but forgets the other, the corpus walks would still pass while
 * the IR coverage matrix silently drifted; this catches that at the cheapest site (no corpus).
 */
class RRootElementRegistryConsistencyTest {

    @Test
    void corpusExpectedAndIrKindMapAgreeOnSubclassSet() {
        assertEquals(CorpusExpected.EXPECTED, IRKindForClass.knownFqns(),
                "CorpusExpected.EXPECTED and IRKindForClass.MAP disagree on the "
                        + "RRootElement subclass set - add the missing FQN to whichever is short");
    }
}
