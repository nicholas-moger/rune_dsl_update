package org.finos.rune.equivalence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

/**
 * v3.2 seat 10, round 1 (cq SF-7 / spec SF-4): the corpus-free witness of the pair harness's function-leg
 * classification — a class under a functions namespace with a canonical {@code evaluate} on both sides is a FUNCTION,
 * with none on either side a NON-FUNCTION (the A8 axis's rival-TYPE validators, recorded by name and pinned as a set
 * by {@code OptimisedNavigationPairGateTest}), with exactly one an ASYMMETRIC skip — the hard-zero regression signal.
 */
class PairHarnessFunctionLegClassTest {

    private static Method some() throws NoSuchMethodException {
        return Object.class.getMethod("toString");
    }

    @Test
    void bothSides_isAFunction() throws NoSuchMethodException {
        assertEquals(PairHarness.FunctionLegClass.FUNCTION, PairHarness.classifyEvaluate(some(), some()));
    }

    @Test
    void neitherSide_isANonFunction_neverASkip() {
        assertEquals(PairHarness.FunctionLegClass.NON_FUNCTION_BOTH_SIDES, PairHarness.classifyEvaluate(null, null));
    }

    @Test
    void referenceOnly_isAsymmetric() throws NoSuchMethodException {
        assertEquals(PairHarness.FunctionLegClass.ASYMMETRIC, PairHarness.classifyEvaluate(some(), null));
    }

    @Test
    void optimisedOnly_isAsymmetric() throws NoSuchMethodException {
        assertEquals(PairHarness.FunctionLegClass.ASYMMETRIC, PairHarness.classifyEvaluate(null, some()));
    }
}
