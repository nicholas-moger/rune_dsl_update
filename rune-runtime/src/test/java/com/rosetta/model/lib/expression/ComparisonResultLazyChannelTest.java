/*
 * Copyright 2024 REGnosys
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.rosetta.model.lib.expression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

/**
 * Locks for the U020 lazy error channel's own contract (census § 14e):
 * deferral until read, materialize-once memoization, eager-twin text
 * equivalence for every lazy factory, lazy combinator composition, and the
 * two behavioural quirks the design reproduces verbatim (the passing-`and`
 * stored-"" and the passing-`or` null channel). The corpus-scale lock is
 * the rune-equivalence O2 channel + the § 14f text-capture instrument;
 * these pins keep the carrier's contract enforced without the corpus.
 */
class ComparisonResultLazyChannelTest {

    private static ComparisonResult countingFailure(AtomicInteger count, String text) {
        return ComparisonResult.failureLazy(() -> {
            count.incrementAndGet();
            return text;
        });
    }

    @Test
    void failureLazyDefersUntilRead() {
        AtomicInteger count = new AtomicInteger();
        ComparisonResult r = countingFailure(count, "deferred");
        assertEquals(0, count.get(), "supplier must not run at construction");
        assertEquals(false, r.get());
        assertEquals(0, count.get(), "reading the boolean channel must not force the text");
    }

    @Test
    void getErrorMaterializesOnceAndMemoizes() {
        AtomicInteger count = new AtomicInteger();
        ComparisonResult r = countingFailure(count, "once");
        String first = r.getError();
        String second = r.getError();
        assertEquals("once", first);
        assertSame(first, second, "the memoized read must return the materialized instance");
        assertEquals(1, count.get(), "the supplier must run exactly once across reads");
    }

    @Test
    void lazyFactoriesMatchTheirEagerTwins() {
        assertEquals(ComparisonResult.failure("t").getError(),
                ComparisonResult.failureLazy(() -> "t").getError());
        ComparisonResult eagerSuccessEmpty = ComparisonResult.successEmptyOperand("e");
        ComparisonResult lazySuccessEmpty = ComparisonResult.successEmptyOperandLazy(() -> "e");
        assertEquals(eagerSuccessEmpty.getError(), lazySuccessEmpty.getError());
        assertEquals(eagerSuccessEmpty.get(), lazySuccessEmpty.get());
        assertEquals(eagerSuccessEmpty.isEmptyOperand(), lazySuccessEmpty.isEmptyOperand());
        ComparisonResult eagerFailureEmpty = ComparisonResult.failureEmptyOperand("f");
        ComparisonResult lazyFailureEmpty = ComparisonResult.failureEmptyOperandLazy(() -> "f");
        assertEquals(eagerFailureEmpty.getError(), lazyFailureEmpty.getError());
        assertEquals(eagerFailureEmpty.get(), lazyFailureEmpty.get());
        assertEquals(eagerFailureEmpty.isEmptyOperand(), lazyFailureEmpty.isEmptyOperand());
    }

    @Test
    void combinatorsComposeWithoutForcingChildren() {
        AtomicInteger left = new AtomicInteger();
        AtomicInteger right = new AtomicInteger();
        ComparisonResult combined = countingFailure(left, "L")
                .andNullSafe(countingFailure(right, "R"));
        assertEquals(0, left.get() + right.get(),
                "combining must not force either child's text");
        assertEquals("L and R", combined.getError());
        assertEquals(1, left.get());
        assertEquals(1, right.get());
        assertEquals(ComparisonResult.failure("L").andNullSafe(ComparisonResult.failure("R")).getError(),
                combined.getError(), "the lazy composition must equal the eager twin's text");
    }

    @Test
    void passingAndCarriesTheStoredEmptyStringQuirk() {
        assertEquals("", ComparisonResult.success().andNullSafe(ComparisonResult.success()).getError(),
                "a passing `and` carries \"\", not null — the eager quirk preserved");
    }

    @Test
    void passingOrHasNoErrorChannelAndNeverForcesTheChild() {
        AtomicInteger count = new AtomicInteger();
        ComparisonResult r = ComparisonResult.success().orNullSafe(countingFailure(count, "never"));
        assertTrue(r.get());
        assertNull(r.getError(), "a passing `or` has NO error channel — exactly as the eager path stored");
        assertEquals(0, count.get(), "the failing child's text must never materialize on a passing or");
    }

    @Test
    void getErrorsRoutesThroughMaterialization() {
        AtomicInteger count = new AtomicInteger();
        ComparisonResult r = countingFailure(count, "routed");
        assertEquals(Collections.singletonList("routed"), r.getErrors());
        assertEquals(1, count.get());
    }
}
