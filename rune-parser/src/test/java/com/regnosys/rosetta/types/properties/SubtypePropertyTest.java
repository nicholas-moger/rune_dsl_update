package com.regnosys.rosetta.types.properties;

import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.types.*;
import com.regnosys.rosetta.types.relation.SubtypeRelation;
import com.regnosys.rosetta.types.relation.TypeJoin;
import net.jqwik.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * M4 T21 — Property-based tests for subtype relation and type join.
 * Validates algebraic invariants hold for all generated types.
 */
class SubtypePropertyTest {

    private final SubtypeRelation sub = new SubtypeRelation();
    private final TypeJoin join = new TypeJoin(sub);

    @Provide
    Arbitrary<RType> types() {
        // MISSING excluded — it's a sentinel, not a proper type in the lattice
        return Arbitraries.oneOf(
            Arbitraries.of(
                RBasicType.BOOLEAN, RBasicType.TIME, RBasicType.PATTERN,
                RBasicType.NOTHING, RBasicType.ANY,
                RNumberType.unconstrained(), RNumberType.intType(),
                RStringType.unconstrained(),
                RRecordType.DATE, RRecordType.DATE_TIME, RRecordType.ZONED_DATE_TIME
            ),
            Arbitraries.integers().between(1, 20).map(d ->
                new RNumberType(OptionalInt.of(d), OptionalInt.of(0),
                    Optional.empty(), Optional.empty())),
            Arbitraries.integers().between(0, 100).map(len ->
                new RStringType(OptionalInt.of(len), OptionalInt.of(len), Optional.empty()))
        );
    }

    @Property(tries = 200)
    void subtype_is_reflexive(@ForAll("types") RType t) {
        assertTrue(sub.isSubtypeOf(t, t), t + " should be subtype of itself");
    }

    @Property(tries = 200)
    void nothing_is_subtype_of_everything(@ForAll("types") RType t) {
        assertTrue(sub.isSubtypeOf(RBasicType.NOTHING, t));
    }

    @Property(tries = 200)
    void everything_is_subtype_of_any(@ForAll("types") RType t) {
        assertTrue(sub.isSubtypeOf(t, RBasicType.ANY));
    }

    @Property(tries = 200)
    void join_is_commutative(@ForAll("types") RType a, @ForAll("types") RType b) {
        RType ab = join.join(a, b);
        RType ba = join.join(b, a);
        assertEquals(ab, ba, "join(" + a + ", " + b + ") should be commutative");
    }

    @Property(tries = 200)
    void join_with_nothing_is_identity(@ForAll("types") RType t) {
        assertEquals(t, join.join(t, RBasicType.NOTHING));
        assertEquals(t, join.join(RBasicType.NOTHING, t));
    }

    @Property(tries = 200)
    void join_with_any_is_any(@ForAll("types") RType t) {
        assertEquals(RBasicType.ANY, join.join(t, RBasicType.ANY));
        assertEquals(RBasicType.ANY, join.join(RBasicType.ANY, t));
    }

    @Property(tries = 200)
    void join_is_idempotent(@ForAll("types") RType t) {
        assertEquals(t, join.join(t, t));
    }

    @Property(tries = 200)
    void join_result_is_supertype_of_both(@ForAll("types") RType a, @ForAll("types") RType b) {
        RType joined = join.join(a, b);
        assertTrue(sub.isSubtypeOf(a, joined),
            a + " should be subtype of join(" + a + ", " + b + ") = " + joined);
        assertTrue(sub.isSubtypeOf(b, joined),
            b + " should be subtype of join(" + a + ", " + b + ") = " + joined);
    }
}
