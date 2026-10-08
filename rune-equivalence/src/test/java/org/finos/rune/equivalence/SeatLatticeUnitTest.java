package org.finos.rune.equivalence;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Corpus-free locks on the leg-G structural layer: slot discovery order and
 * naming, the bounded seat enumeration (MIN + FULL + capped flips), and the
 * structural reference-wrapper detection — all on local fixtures so the suite
 * stays green on checkouts without the corpus.
 */
class SeatLatticeUnitTest {

    /** Fixture builder: three settable slots + noise the discovery must ignore. */
    public static final class FixtureBuilder {
        public FixtureBuilder setBeta(String v) { return this; }
        public FixtureBuilder setAlpha(Integer v) { return this; }
        public FixtureBuilder addGamma(String v) { return this; }
        public FixtureBuilder setTwoArgs(String a, String b) { return this; }
        public static FixtureBuilder setStaticNoise(String v) { return null; }
        public String getAlpha() { return null; }
    }

    /** Fixture reference wrapper: builder() with BOTH channels. */
    public static final class FixtureRef {
        public static final class Builder {
            public Builder setValue(String v) { return this; }
            public Builder setGlobalReference(String v) { return this; }
            public FixtureRef build() { return new FixtureRef(); }
        }
        public static Builder builder() { return new Builder(); }
    }

    /** Fixture non-wrapper: builder() with a value channel only. */
    public static final class FixturePlain {
        public static final class Builder {
            public Builder setValue(String v) { return this; }
            public FixturePlain build() { return new FixturePlain(); }
        }
        public static Builder builder() { return new Builder(); }
    }

    @Test
    void slotDiscoveryIsDeterministicAndFiltered() {
        SeatLattice lattice = new SeatLattice(0);
        List<SeatLattice.Slot> slots = lattice.slots(FixtureBuilder.class);
        assertEquals(List.of("gamma", "alpha", "beta"),
                slots.stream().map(SeatLattice.Slot::attributeName).toList(),
                "name-sorted method order (addGamma < setAlpha < setBeta), two-arg and"
                        + " static methods excluded, attribute names decapitalised");
        assertEquals(List.of("addGamma", "setAlpha", "setBeta"),
                slots.stream().map(SeatLattice.Slot::methodName).toList());
    }

    @Test
    void seatEnumerationIsBoundedAndLabelled() {
        SeatLattice lattice = new SeatLattice(2);
        List<SeatLattice.Slot> slots = lattice.slots(FixtureBuilder.class);
        List<SeatLattice.Seat> seats = lattice.seats(slots);
        assertEquals(4, seats.size(), "MIN + FULL + flipWidthCap(2) flips");
        assertEquals("MIN", seats.get(0).label());
        assertEquals(3, seats.get(0).absentAttributes().size(), "MIN leaves every slot absent");
        assertEquals("FULL", seats.get(1).label());
        assertTrue(seats.get(1).absentAttributes().isEmpty(), "FULL leaves nothing absent");
        assertEquals("FLIP:gamma", seats.get(2).label());
        assertEquals(List.of("gamma"), seats.get(2).absentAttributes());
        assertEquals("FLIP:alpha", seats.get(3).label());
    }

    @Test
    void flipCapLargerThanWidthEnumeratesEverySlot() {
        SeatLattice lattice = new SeatLattice(99);
        List<SeatLattice.Seat> seats = lattice.seats(lattice.slots(FixtureBuilder.class));
        assertEquals(2 + 3, seats.size(), "the cap clamps to the slot width");
    }

    @Test
    void referenceWrapperDetectionIsStructural() {
        assertTrue(SeatLattice.isReferenceWrapper(FixtureRef.class),
                "builder() with setValue + setGlobalReference(String) ⇒ wrapper");
        assertFalse(SeatLattice.isReferenceWrapper(FixturePlain.class),
                "a value channel alone is not a reference wrapper");
        assertFalse(SeatLattice.isReferenceWrapper(String.class),
                "no builder() ⇒ not a wrapper");
    }
}
