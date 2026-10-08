package com.regnosys.rosetta.ir.contract;

import com.regnosys.rosetta.ir.core.IRKind;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IRKindCoverageTest {

    /**
     * Serialisation-stability pin per spec Section 7 reviewer fix S1.
     * M9 Python target may serialise {@link IRKind} names; reordering or
     * renaming breaks wire compat. This test pins the exact string emitted
     * by {@link Arrays#toString(Object[])} so any drift fails loudly.
     *
     * <p>Modifying this expected string is a deliberate breaking change
     * requiring a D-entry per D21.
     */
    @Test
    void irKindValuesPinnedOrder() {
        String expected = "[STRUCT, CHOICE, TYPE_ALIAS, BASIC_TYPE, RECORD_TYPE, META_TYPE, " +
                "ENUM, FUNCTION, LIBRARY_FUNCTION, RULE, REPORT, ANNOTATION_DECL, " +
                "SYNONYM_SOURCE, EXTERNAL_SYNONYM_SOURCE, EXTERNAL_RULE_SOURCE, " +
                "REGULATORY_BODY, REGULATORY_CORPUS, REGULATORY_SEGMENT_DEF, " +
                "MODEL, " +
                "FIELD, PARAMETER, ENUM_VALUE]";
        assertEquals(expected, Arrays.toString(IRKind.values()));
    }

    /**
     * MODEL joined at v3.3 seat 8 (PR #644 — the property gate), inserted AFTER the 18 RRootElement-mapped
     * kinds and BEFORE the 3 sub-element kinds: it is a top-level node (a {@code namespace} declaration's own
     * facts) but it maps from no {@code RRootElement} subclass — see
     * {@code RRootElementToIRKindMappingTest#theModelKindMapsFromNoRRootElementSubclass}.
     */
    @Test
    void exactly22KindsDeclared() {
        assertEquals(22, IRKind.values().length,
                "22 IRKind values expected — 18 RRootElement-mapped + 1 model-level container "
                        + "+ 3 sub-element kinds");
    }
}
