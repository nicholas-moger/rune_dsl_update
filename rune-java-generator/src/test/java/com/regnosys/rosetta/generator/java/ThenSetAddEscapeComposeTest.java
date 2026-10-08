package com.regnosys.rosetta.generator.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

/**
 * PR #347 — the eight-facet compose: 7 byte flips (1 cdm5 + 4 cdm6 + 2 drr FUNCTION)
 * + the F7/F8 heal-only facets.
 *
 * <p><b>F1 — {@code inverseN7CalleeParamMeta}</b>: the meta-deref arg hoist
 * ({@code ReferenceHandler.tryMetaDerefArg}) DECLINES when the callee param itself
 * carries meta — golden passes the whole nav chain's wrapper BARE (upstream's
 * type-directed coercion is identity when the expected type IS the wrapper;
 * {@code toJavaReferenceType} strips meta from the param, so the type-equality gate
 * alone could not decline — the #346 cp1 catch generalized from the W1/W2 call sites
 * to the core, covering the #237 statement-sink route).
 *
 * <p><b>F2 — {@code thenSetSegmentPath}</b>: a SEGMENT-path SET whose value is a
 * hoistable then-chain honors its path — the final assignment renders the leaf setter
 * ({@code cdmTradeIdentifier\n\t.setIdentifierType(thenArg\n\t\t.first().get());})
 * instead of the whole-output {@code <out> = toBuilder(<lastBody>);}, evidence-scoped
 * to a SINGLE resolved single-cardinality meta-free leaf.
 *
 * <p><b>F3 — {@code setMetaDerefHoist}</b>: the DEREF direction of the #328 wrap hoist
 * at the segment-SET seat — a META-FREE single leaf whose value terminal is a meta
 * WRAPPER hoists {@code final <Wrapper> <name> = <chain>.get();} and passes the
 * null-guarded {@code .getValue()} deref to the leaf setter.
 *
 * <p><b>F4 — {@code addHoistLocalParamEscape}</b>: the {@code StatementHoistSession}
 * replay gains upstream's escape-iff-taken law — number FIRST (a multi-member group's
 * {@code base0..n-1} never escapes: golden CalculateTransfer's {@code transfer0/1}
 * under the colliding {@code transfer} output param pins the arm ORDER), then
 * {@code _}-escape a resolved name against the method seeds (inputs/output/shortcuts
 * + dep fields). A bare local shadowing a method param is illegal Java, so every
 * colliding singleton carrier was an already-waivered non-compiling mismatch.
 *
 * <p><b>F5 — {@code arithStringJoinAlias}</b>: ALIAS ({@code RShortcut}) {@code +}
 * operands resolve through the SAME walk that renders the alias method signature
 * ({@code tryAliasReceiverMapperType} → {@code inferShortcutMapperJavaType}) — String
 * item or String-valued meta wrapper — so the #334 {@code stringJoin} fires
 * ({@code MapperMaths.<String, String, String>add}) and the wrapper operand derefs
 * via the wrapper-level coercion (unguarded for a MapperC receiver, exactly golden).
 *
 * <p><b>F6 — {@code disguisedHeadCardinality} + {@code thenAddSingleMetaRewrap}</b>:
 * (i) a DISGUISED 2-name extract body ({@code extract tradeLot -> priceQuantity}, an
 * unresolved REnumValueRef — NOT an RFeatureCall, the P347-D1 probe) whose feature
 * HEAD is MULTI on the implicit item selects {@code mapSingleToList}; (ii) a collapsed
 * SINGLE meta then-tail ({@code then flatten then only-element}) added to a MULTI
 * meta-free model output re-wraps {@code MapperS.of(<v>).<Bare>map("Type coercion",
 * <guarded deref>).getMulti()} (the only-element type erasure recovered from the prior
 * thenArg's element — the #290 law).
 *
 * <p><b>F7 — {@code addSingleMetaSingletonWrap}</b> (heal-only): the ADD-segment seat
 * gains {@code wrapMetaSetterValueOrNull}'s MULTI-leaf singleton meta-builder ternary
 * for a SINGLE bare-identifier value into a MULTI value-level-META leaf — closing the
 * {@code FunctionConvertNullSafeMultiWrapTest} deferred ADD-position gap. The four
 * {@code Resolve{InterestRate,Performance}Reset} carriers heal to their dl=2
 * double-underscore naming-only residual (the #170 lambdaParamUS law stays deferred).
 *
 * <p><b>F8 — {@code itemNavChoiceSuperOption}</b> (heal-only): the bare-symbol item
 * re-root's attribute lookup gains the #207 {@code findChoiceSuperOption} fallback —
 * a bare capitalized name that is an option of the item type's CHOICE supertype
 * ({@code type BasketConstituent extends Observable}, a choice) re-roots
 * {@code item.<BasketConstituent>map("Type coercion", <deref>).<Asset>map("getAsset",
 * …)} instead of the non-compiling {@code MapperS.of(Asset)} (zero goldens carry a
 * bare {@code MapperS.of(<CapitalizedName>)}).
 *
 * <p>Whole-file byte comparisons run through the REAL D11 FUNCTION generation path and
 * revert RED without the facets (compile-split MEASURED per the per-carrier
 * compile-gate.json verdicts: 7 NON_COMPILING).
 */
class ThenSetAddEscapeComposeTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM5_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-5.38.0");
    private static final Path CDM5_GOLDEN_DIR =
            CDM5_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    // F1 inverseN7CalleeParamMeta flip carrier (cdm5; cdm5 Create_Cashflow's
    // `currency string [metadata scheme]` param takes the wrapper bare).
    private static final String CREATE_ON_DEMAND_INTEREST_PAYMENT_CDM5 =
            "cdm/event/common/functions/Create_OnDemandInterestPaymentPrimitiveInstruction.java";
    // F2+F3 flip carrier (cdm6; the F2 setter-path + the F3 setObservable deref hoist).
    private static final String RESOLVE_PERF_OBS_IDENTIFIERS_CDM6 =
            "cdm/event/common/functions/ResolvePerformanceObservationIdentifiers.java";
    // F2 flip carrier (cdm6; the single-segment `set … -> identifierType` then-chain).
    private static final String MAP_TRADE_IDENTIFIER_SEQUENCE =
            "cdm/ingest/fpml/confirmation/header/functions/MapTradeIdentifierSequenceToTradeIdentifier.java";
    // F4 flip carriers (cdm6; the conditional-arm seat + the whole-output seat).
    private static final String MAP_OPTIONAL_EARLY_TERMINATION =
            "cdm/ingest/fpml/confirmation/party/functions/MapOptionalEarlyTerminationToAncillaryParty.java";
    private static final String MAP_AVERAGING_OBSERVATIONS =
            "cdm/ingest/fpml/confirmation/product/equityoption/functions/MapAveragingObservations.java";
    // F5 flip carrier (drr).
    private static final String PARTY_LEI_AND_PERSON_BY_ROLES =
            "drr/regulation/common/functions/PartyLeiAndPersonByRoles.java";
    // F6 flip carrier (drr; both halves needed).
    private static final String PRICE_OF_ZERO_COUPON_SWAPS =
            "drr/standards/iosco/cde/base/price/functions/PriceOfZeroCouponSwaps.java";
    // F7 heal carrier (cdm5; the dl=2 naming-only residual stays waivered).
    private static final String RESOLVE_INTEREST_RATE_RESET_CDM5 =
            "cdm/event/common/functions/ResolveInterestRateReset.java";
    // F8 heal carrier (cdm6; the trailing choice-option witness stays deferred).
    private static final String QUALIFY_FX_PARAMETER_RETURN_CORRELATION_CDM6 =
            "cdm/product/qualification/functions/Qualify_ForeignExchange_ParameterReturnCorrelation.java";

    private static Map<String, String> drrFnOutput;
    private static Map<String, String> cdm6FnOutput;
    private static Map<String, String> cdm5FnOutput;

    static boolean drrCellAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR);
    }

    static boolean cdm6CellAvailable() {
        return Files.isDirectory(CDM6_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM6_GOLDEN_DIR);
    }

    static boolean cdm5CellAvailable() {
        return Files.isDirectory(CDM5_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM5_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (cdm6CellAvailable()) {
            cdm6FnOutput = generateFunctions(
                    new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CDM6_CELL_ROOT));
        }
        if (cdm5CellAvailable()) {
            cdm5FnOutput = generateFunctions(
                    new D11CorpusRegressionTest.CellSpec("cdm", "5.38.0", CDM5_CELL_ROOT));
        }
        if (drrCellAvailable()) {
            drrFnOutput = generateFunctions(
                    new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT));
        }
    }

    /** The REAL D11 FUNCTION-kind generation path (function_comparison). */
    private static Map<String, String> generateFunctions(D11CorpusRegressionTest.CellSpec cell)
            throws IOException {
        var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(),
                D11CorpusRegressionTest.emissionFilter(cell));
        var typeUtil = new JavaTypeUtil();
        var funcGen = new FunctionGenerator(gm, new JavaTypeTranslator(typeUtil), typeUtil);
        Map<String, String> output = new LinkedHashMap<>();
        var errors = funcGen.generateWithErrors(output);
        assertTrue(errors.isEmpty(),
                () -> "Function generation reported " + errors.size() + " error(s): " + errors);
        return output;
    }

    // ==== F1–F6 flip locks (revert-RED) ====

    @Test
    @EnabledIf("cdm5CellAvailable")
    void createOnDemandInterestPayment_cdm5_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5FnOutput, CDM5_GOLDEN_DIR,
                CREATE_ON_DEMAND_INTEREST_PAYMENT_CDM5);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void resolvePerformanceObservationIdentifiers_cdm6_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR,
                RESOLVE_PERF_OBS_IDENTIFIERS_CDM6);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapTradeIdentifierSequence_cdm6_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, MAP_TRADE_IDENTIFIER_SEQUENCE);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapOptionalEarlyTermination_cdm6_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, MAP_OPTIONAL_EARLY_TERMINATION);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapAveragingObservations_cdm6_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, MAP_AVERAGING_OBSERVATIONS);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void partyLeiAndPersonByRoles_drr_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFnOutput, DRR_GOLDEN_DIR, PARTY_LEI_AND_PERSON_BY_ROLES);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void priceOfZeroCouponSwaps_drr_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFnOutput, DRR_GOLDEN_DIR, PRICE_OF_ZERO_COUPON_SWAPS);
    }

    // ==== F7 content lock (heal — the ADD-seat singleton meta wrap) ====

    /**
     * Positive + negative content lock (F7): the addObservations value wraps into the
     * null-guarded singleton meta-builder list; the bare {@code .addObservations(observation);}
     * splice is a token the heal REMOVES (count 0 in the frozen goldens — it never compiled;
     * positive control: the wrap fragment appears in 4 golden files, the 4 carriers).
     */
    @Test
    @EnabledIf("cdm5CellAvailable")
    void resolveInterestRateReset_cdm5_addObservationsSingletonMetaWrap() {
        String gen = gen(cdm5FnOutput, RESOLVE_INTEREST_RATE_RESET_CDM5);
        assertTrue(gen.contains(
                ".addObservations((observation == null ? Collections.<ReferenceWithMetaObservation>"
                + "emptyList() : Collections.singletonList(ReferenceWithMetaObservation.builder()"
                + ".setValue(observation).build())));"),
                "The ADD-seat singleton meta wrap must fire for the single bare param into the "
                + "MULTI [metadata reference] observations leaf");
        assertFalse(gen.contains(".addObservations(observation);"),
                "The bare non-compiling add splice is the pre-facet form the heal removes");
    }

    // ==== F8 content lock (heal — the choice-super-option item re-root) ====

    /**
     * Positive + negative content lock (F8): the bare {@code Index} (an option of the
     * {@code Observable} CHOICE that {@code BasketConstituent} extends) re-roots on the
     * implicit item with the meta deref; {@code MapperS.of(Index)} is a token the heal
     * REMOVES (count 0 in the frozen goldens — the bare type reference never compiled).
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void qualifyFxParameterReturnCorrelation_cdm6_choiceSuperOptionReRoot() {
        String gen = gen(cdm6FnOutput, QUALIFY_FX_PARAMETER_RETURN_CORRELATION_CDM6);
        assertTrue(gen.contains(
                "item.<BasketConstituent>map(\"Type coercion\", fieldWithMetaBasketConstituent -> "
                + "fieldWithMetaBasketConstituent == null ? null : fieldWithMetaBasketConstituent"
                + ".getValue()).<Index>map(\"getIndex\", basketConstituent -> "
                + "basketConstituent.getIndex())"),
                "The choice-super-option re-root must deref the meta item and navigate getIndex");
        assertFalse(gen.contains("MapperS.of(Index)"),
                "The bare mis-bound type reference is the pre-facet form the heal removes");
    }

    private static String gen(Map<String, String> output, String path) {
        assertNotNull(output, "generation did not run — corpus unavailable?");
        String g = output.get(path);
        assertNotNull(g, "Class not generated: " + path);
        return g;
    }

    private static void assertByteMatchesGolden(Map<String, String> output, Path goldenDir,
            String path) throws IOException {
        assertNotNull(output, "generation did not run — corpus unavailable?");
        String generated = output.get(path);
        assertNotNull(generated, "Class not generated: " + path
                + " (emission failed or the path differs)");
        Path goldenPath = goldenDir.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated output must byte-match the golden (newline-normalized) for "
                + path + " (PR #347 the eight-facet compose).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
