package com.regnosys.rosetta.generator.java;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.enums.RSynonymRef;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.supporting.REnumValue;
import com.regnosys.rosetta.ast.synonyms.REnumSynonym;
import com.regnosys.rosetta.ast.synonyms.RSynonym;
import com.regnosys.rosetta.ast.types.REnumeration;
import com.regnosys.rosetta.symbols.RLinkingResult;
import com.regnosys.rosetta.symbols.RWorkspace;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Leg-C port (PR #423, slice 2) of upstream
 * {@code rune-integration-tests/.../object/RosettaModelTest.xtend} — 1/1
 * method. Pure AST-shape asserts on enum synonyms through the fork synonym
 * model; the upstream↔fork accessor map: {@code synonyms.sources.head.name} →
 * {@code RSynonym.sources()} (plain strings in the fork),
 * {@code body.values[0].refType.name == "componentID"} →
 * {@code RSynonymValue.ref() == RSynonymRef.COMPONENT_ID}, {@code value == 24}
 * → {@code refValue() == BigInteger(24)}, enum-value synonym
 * {@code synonymValue}/{@code definition} → {@code REnumSynonym.value()} /
 * {@code definitionText()}. Ledger:
 * the development audit "2026-07-17-leg-c-integration-port-ledger".
 */
class UpstreamRosettaModelPortTest {

    /** Upstream {@code testEnumeration}. */
    @Test
    void testEnumeration() {
        String src = UpstreamPortHarness.TEST_NS_HEADER + "\n" + """
                enum QuoteRejectReasonEnum: <"The enumeration values.">
                [synonym ISO value "QuoteRejectReason" componentID 24]
                	UnknownSymbol <"unknown symbol">
                	[synonym ISO_20022 value "UK" definition "Unknown Symbol"]
                	KnownSymbol

                synonym source ISO
                synonym source ISO_20022
                """;
        RModel model = AstBuilder.buildFromString(src, "rosetta-model-port.rosetta");
        RLinkingResult result = RWorkspace.build(List.of(model));
        // Upstream parseRosettaWithNoErrors: the snippet parses and links clean.
        assertTrue(result.diagnostics().isEmpty(),
                "expected no diagnostics, got: " + result.diagnostics());

        REnumeration enumeration = (REnumeration) model.rootElements().get(0);
        assertEquals("QuoteRejectReasonEnum", enumeration.name());
        assertEquals("The enumeration values.", enumeration.definition().orElseThrow());

        RSynonym synonyms = enumeration.synonyms().get(0);
        assertEquals("ISO", synonyms.sources().get(0));
        assertEquals("QuoteRejectReason", synonyms.body().values().get(0).name());
        assertEquals(RSynonymRef.COMPONENT_ID, synonyms.body().values().get(0).ref().orElseThrow());
        assertEquals(BigInteger.valueOf(24), synonyms.body().values().get(0).refValue().orElseThrow());

        REnumValue enumValues1 = enumeration.values().get(0);
        assertEquals("UnknownSymbol", enumValues1.name());
        assertEquals("unknown symbol", enumValues1.definition().orElseThrow());

        REnumSynonym enumSynonyms = enumValues1.synonyms().get(0);
        assertEquals("ISO_20022", String.join("", enumSynonyms.sources()));
        assertEquals("UK", enumSynonyms.value());
        assertEquals("Unknown Symbol", enumSynonyms.definitionText().orElseThrow());

        REnumValue enumValues2 = enumeration.values().get(1);
        assertEquals("KnownSymbol", enumValues2.name());
    }
}
