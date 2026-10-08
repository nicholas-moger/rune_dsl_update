package com.regnosys.rosetta.generator.java;

import com.regnosys.rosetta.generator.java.enums.EnumHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Leg-C port (PR #414) of upstream
 * {@code rune-integration-tests/.../object/EnumGeneratorTest.xtend} — 4/4 methods.
 * Ledger: the development audit "2026-07-17-leg-c-integration-port-ledger".
 */
class UpstreamEnumGeneratorPortTest {

    @BeforeAll
    static void requireReleasedRuntime() {
        UpstreamPortHarness.assumeReleasedRuntime();
    }

    /** Upstream {@code shouldGenerateAnnotationForEnumSynonyms}. */
    @Test
    void shouldGenerateAnnotationForEnumSynonyms() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                synonym source FpML
                enum TestEnum:
                	one <"Some description"> [synonym FpML value "oneSynonym"]
                	two <"Some other description"> [synonym FpML value "twoSynonym"]
                """);

        String testEnumCode = code.get(UpstreamPortHarness.ROOT_PACKAGE + ".TestEnum");
        assertTrue(testEnumCode.contains("RosettaSynonym(value = \"oneSynonym\", source = \"FpML\")"),
                "missing enum-synonym annotation in:\n" + testEnumCode);

        UpstreamPortHarness.compileToClasses(code);
    }

    /** Upstream {@code shouldGenerateBasicReferenceForEnum}. */
    @Test
    void shouldGenerateBasicReferenceForEnum() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                namespace "com.rosetta.test.model"
                version "test"

                enum TestEnum:
                	one
                	two

                type TestObj:
                	attr TestEnum (1..1)
                """);

        UpstreamPortHarness.compileToClasses(code);
    }

    /** Upstream {@code shouldGenerateAllDisplayNameAndConstructors}. */
    @Test
    void shouldGenerateAllDisplayNameAndConstructors() {
        Map<String, String> code = UpstreamPortHarness.generateCode("""
                synonym source FpML
                enum TestEnumWithDisplay:
                	one displayName "uno" <"Some description"> [synonym FpML value "oneSynonym"]
                	two <"Some other description"> [synonym FpML value "twoSynonym"]
                	three displayName "tria" <"Some description"> [synonym FpML value "threeSynonym"]
                	four  displayName "tessera" <"Some description"> [synonym FpML value "fourSynonym"]
                """);

        String testEnumCode = code.get(UpstreamPortHarness.ROOT_PACKAGE + ".TestEnumWithDisplay");
        assertTrue(testEnumCode.contains("TestEnumWithDisplay(String rosettaName, String displayName)"),
                "missing displayName constructor in:\n" + testEnumCode);
        assertTrue(testEnumCode.contains("public String toDisplayString()"),
                "missing toDisplayString() in:\n" + testEnumCode);

        UpstreamPortHarness.compileToClasses(code);
    }

    /** Upstream {@code shouldGenerateUppercaseUnderscoreFormattedEnumNames} — the
     * {@code EnumHelper.formatEnumName} unit matrix, byte-for-byte. */
    @Test
    void shouldGenerateUppercaseUnderscoreFormattedEnumNames() {
        assertEquals("ISDA_1993_COMMODITY", EnumHelper.formatEnumName("ISDA1993Commodity"));
        assertEquals("ISDA1998FX", EnumHelper.formatEnumName("ISDA1998FX"));
        assertEquals("I_TRAXX_EUROPE_DEALER", EnumHelper.formatEnumName("iTraxxEuropeDealer"));
        assertEquals("STANDARD_LCDS", EnumHelper.formatEnumName("StandardLCDS"));
        assertEquals("_1_1", EnumHelper.formatEnumName("_1_1"));
        assertEquals("_30E_360_ISDA", EnumHelper.formatEnumName("_30E_360_ISDA"));
        assertEquals("ACT_365L", EnumHelper.formatEnumName("ACT_365L"));
        assertEquals("OSP_PRICE", EnumHelper.formatEnumName("OSPPrice"));
        assertEquals("FRA_YIELD", EnumHelper.formatEnumName("FRAYield"));
        assertEquals("AED_EBOR_REUTERS", EnumHelper.formatEnumName("AED-EBOR-Reuters"));
        assertEquals("EUR_EURIBOR_REUTERS", EnumHelper.formatEnumName("EUR-EURIBOR-Reuters"));
        assertEquals("DJ_I_TRAXX_EUROPE", EnumHelper.formatEnumName("DJ.iTraxx.Europe"));
        assertEquals("IVS_1_OPEN_MARKETS", EnumHelper.formatEnumName("IVS1OpenMarkets"));
        assertEquals("D", EnumHelper.formatEnumName("D"));
        assertEquals("_1", EnumHelper.formatEnumName("_1"));
        assertEquals("DJ_CDX_NA", EnumHelper.formatEnumName("DJ.CDX.NA"));
        assertEquals("NOVATION", EnumHelper.formatEnumName("novation"));
        assertEquals("PARTIAL_NOVATION", EnumHelper.formatEnumName("partialNovation"));
        assertEquals("ALUMINIUM_ALLOY_LME_15_MONTH", EnumHelper.formatEnumName("ALUMINIUM_ALLOY_LME_15_MONTH"));
        assertEquals("AGGREGATE_CLIENT", EnumHelper.formatEnumName("AggregateClient"));
        assertEquals("CURRENCY_1_PER_CURRENCY_2", EnumHelper.formatEnumName("Currency1PerCurrency2"));
    }
}
