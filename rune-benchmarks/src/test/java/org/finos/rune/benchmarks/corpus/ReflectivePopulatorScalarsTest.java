package org.finos.rune.benchmarks.corpus;

import com.rosetta.model.lib.records.Date;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.DayOfWeek;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** The deterministic value table the workload inputs are built from. */
class ReflectivePopulatorScalarsTest {

    private final ReflectivePopulator populator = new ReflectivePopulator();

    @Test
    void scalarTableIsDeterministic() {
        assertEquals("BMRK1", populator.valueFor(String.class, 0));
        assertEquals(1, populator.valueFor(Integer.class, 0));
        assertEquals(Boolean.TRUE, populator.valueFor(Boolean.class, 0));
        assertEquals(BigDecimal.ONE, populator.valueFor(BigDecimal.class, 0));
        assertEquals(Date.of(2026, 1, 15), populator.valueFor(Date.class, 0));
    }

    @Test
    void enumsResolveToFirstConstant() {
        assertEquals(DayOfWeek.MONDAY, populator.valueFor(DayOfWeek.class, 0));
    }

    @Test
    void unsupplyableTypesReturnNullNotThrow() {
        assertNull(populator.valueFor(Thread.class, 0));
        assertNull(populator.valueFor(java.util.List.class, 3));
    }
}
