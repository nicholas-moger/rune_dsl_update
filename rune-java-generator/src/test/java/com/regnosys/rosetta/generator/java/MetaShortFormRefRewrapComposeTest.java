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
 * PR #348 — the six-facet compose: 11 byte flips (2 cdm5 + 8 cdm6 + 1 drr FUNCTION).
 *
 * <p><b>F1 — {@code metaPathShortForm}</b>: a META-FEATURE read ({@code scheme} /
 * {@code reference}) renders upstream buildMapFunc's a->a short form DIRECTLY on the
 * wrapper (ExpressionGenerator.xtend:515-527): {@code reference} →
 * {@code .map("getReference", a->a.getExternalReference())} with NO deref;
 * {@code scheme} → {@code .map("getMeta", a->a.getMeta()).map("getScheme",
 * a->a.getScheme())}. The fork has no RosettaMetaType node — the linker leaves the
 * feature UNRESOLVED (P348-M1 probe: resolved=EMPTY at every corpus seat). Two seats,
 * one render arm: the nav-step arm before {@code coerceNavigationReceiver} (the
 * wrapper proof: typed MapperS item, else the #285 {@code implicitItemArgMeta} walk)
 * and the bare-symbol rung at {@code synthesizeImplicitItemBareNav}'s itemTypeNull
 * decline (drr Extract_BondConnect's {@code filter scheme = '…'}).
 *
 * <p><b>F2 — {@code mapPriceQuantityPair}</b>: (i) {@code isReceiverMultiInner} gains
 * the {@code RListLiteral} arm (>1 element = MULTI, upstream's element join) so the
 * {@code q} alias ({@code [<6 aliases>] extract CreateQuantityWithLocation(...)})
 * types MapperC; (ii) {@code valueCarriesAttributeMeta} gains the
 * {@code RImplicitVariable} arm ({@code implicitItemArgMeta} + the SCOPED alias
 * consultation via {@code tryAliasReceiverMapperType}) so the ctor {@code quantity:
 * item} keeps the PLAIN setter — golden {@code .setQuantity(item.getMulti())}.
 *
 * <p><b>F3 — {@code ctorWithMetaReference}</b>: {@code tryTypedWithMeta}'s expected
 * wrapper derives from the ctor TARGET attribute's {@code [metadata …]} annotation at
 * the ctor-pair seat ({@code notionaReference: empty with-meta { reference: <href
 * chain> first }} → ReferenceWithMetaMoney), and a with-meta ctor value keeps the
 * PLAIN setter (it IS the wrapper by construction). Golden {@code
 * .setNotionaReference(ReferenceWithMetaMoney.builder().setValue(null)
 * .setExternalReference(<chain>.first().get()).build())}.
 *
 * <p><b>F4 — {@code refToFieldRewrap}</b>: upstream convertNullSafe's
 * Reference→Field kind-to-kind meta conversion where a ReferenceWithMeta&lt;T&gt;-item
 * value lands on a FieldWithMeta&lt;T&gt; leaf (same T). LIST form at the ADD seat
 * (the elementwise {@code .<FieldWithMetaProductIdentifier>map("Type coercion",
 * ref -> {…}).getMulti()} block); SINGLE form at the SET seat (the hoisted reference
 * local + the null-distributed {@code ifThenElseResult} if/else). The cdm6
 * ResolveInterestRateObservationIdentifiers twin stays DEFERRED — its
 * {@code set identifiers -> observable -> Index -> InterestRateIndex} is a
 * NESTED-choice-option segment path needing a multi-hop projection (P348-F4b probe).
 *
 * <p><b>F5 — {@code toEnumClaimsSentinel}</b>: the to-enum TARGET renders via the
 * #227 first-claim-wins claims table (an ImportCollisionResolver sentinel) instead of
 * the #196 eager source-wins FQN — golden MapReturnSwapLegToSettlementTerms claims
 * the cdm SettlementTypeEnum first (the ite-hoist decl), so the target renders BARE
 * while the fpml witness FQNs; the 126 green-FQN goldens all carry a source-first
 * claim and resolve byte-identically (over-fire ZERO, Agent-A-verified).
 *
 * <p><b>F6 — {@code enumRequalifyComparison} + {@code choiceOptionTrailingWitness}</b>:
 * (d) {@code tryBareEnumComparand}'s FIFTH sibling rung
 * ({@code deepSiblingEnumeration}) types a DEEP-feature-call sibling ({@code
 * observable -> Index ->> assetClass} → AssetClassEnum) so the type-shadowed bare
 * {@code Commodity} requalifies to {@code MapperS.of(AssetClassEnum.COMMODITY)};
 * (e) {@code resolveReceiverDataType0}'s symbol arm gains the bare-choice-option
 * re-root (the SAME {@code synthesizeImplicitItemBareNav} ladder the render uses), so
 * the TRAILING step after a #347-F8 re-rooted nav recovers its {@code <Type>} witness
 * + import ({@code .<ForeignExchangeRateIndex>map(…)} / {@code .<Commodity>map(…)}).
 * The 4 green #344-F1 carriers' receivers are RAttribute-bound — the attribute arm
 * wins first, so the #347 cp11 naive-carry over-fire class is structurally
 * unreachable.
 *
 * <p>Whole-file byte comparisons run through the REAL D11 FUNCTION generation path and
 * revert RED without the facets. Compile-split: 9 NON_COMPILING + 2 COMPILES-by-shape —
 * the #348 Seat-1 catch (MapReturnSwapLegToSettlementTerms: the sole divergence was the
 * FQN-vs-bare enum token; Qualify_ForeignExchange_ParameterReturnCorrelation: the
 * inference-optional witness); their June-27 compile-gate.json verdicts were STALE (the
 * documented silent NC→CD drift), both bookkept NON_COMPILING, so the post-flip split
 * is frame-invariant (COMPILES_DIVERGENT 24 / non-compiling 452).
 */
class MetaShortFormRefRewrapComposeTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM5_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-5.38.0");
    private static final Path CDM5_GOLDEN_DIR =
            CDM5_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    // F1 flip carriers: the bare-symbol scheme seat (drr) + the reference-kind
    // feature-call seats (cdm6, 2 + 4 short-form occurrences + the Party import drop).
    private static final String EXTRACT_BOND_CONNECT =
            "drr/regulation/hkma/rewrite/trade/functions/Extract_BondConnect.java";
    private static final String MAP_COUNTERPARTY_ROLE_ENUM =
            "cdm/ingest/fpml/confirmation/party/functions/MapCounterpartyRoleEnum.java";
    private static final String MAP_MULTIPLE_COUNTERPARTY_ROLE_ENUM =
            "cdm/ingest/fpml/confirmation/party/functions/MapMultipleCounterpartyRoleEnum.java";
    // F1 heal carrier (still divergent — the block-ladder class): 5 scheme-kind seats.
    private static final String EXTRACT_HKMA_SCHEME_NAME =
            "drr/regulation/hkma/rewrite/trade/functions/Extract_HKMASchemeName.java";
    // F2 flip carrier (cdm6; both mechanisms needed).
    private static final String MAP_PRICE_QUANTITY =
            "cdm/ingest/fpml/confirmation/workflowstep/functions/MapPriceQuantity.java";
    // F3 flip carriers (cdm6; the ctor-seat with-meta reference construction).
    private static final String MAP_MULTIPLE_EXERCISE =
            "cdm/ingest/fpml/confirmation/common/functions/MapMultipleExercise.java";
    private static final String MAP_EUROPEAN_EXERCISE_TERMS =
            "cdm/ingest/fpml/confirmation/common/functions/MapEuropeanExerciseTerms.java";
    // F4 flip carriers (cdm5; LIST + SINGLE forms) + the DEFERRED cdm6 twin.
    private static final String RESOLVE_PERF_OBS_IDENTIFIERS_CDM5 =
            "cdm/event/common/functions/ResolvePerformanceObservationIdentifiers.java";
    private static final String RESOLVE_IR_OBS_IDENTIFIERS =
            "cdm/event/common/functions/ResolveInterestRateObservationIdentifiers.java";
    // F5 flip carrier (cdm6).
    private static final String MAP_RETURN_SWAP_LEG_TO_SETTLEMENT_TERMS =
            "cdm/ingest/fpml/confirmation/settlement/functions/MapReturnSwapLegToSettlementTerms.java";
    // F6 flip carriers (cdm6; (e) solo + (d)+(e) composed).
    private static final String QUALIFY_FX_PARAMETER_RETURN_CORRELATION =
            "cdm/product/qualification/functions/Qualify_ForeignExchange_ParameterReturnCorrelation.java";
    private static final String OBSERVABLE_IS_COMMODITY =
            "cdm/observable/asset/functions/ObservableIsCommodity.java";

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

    // ---------------------------------------------------------------- F1 locks

    @Test
    @EnabledIf("drrCellAvailable")
    void f1_extractBondConnect_bareSchemeShortForm_byteLock() throws IOException {
        assertByteMatchesGolden(drrFnOutput, DRR_GOLDEN_DIR, EXTRACT_BOND_CONNECT);
        String gen = gen(drrFnOutput, EXTRACT_BOND_CONNECT);
        // Negative witness (count 0 in golden, present pre-facet): the bare mis-bound form.
        assertFalse(gen.contains("MapperS.of(scheme)"),
                "The bare mis-bound scheme must re-root through the meta short form");
        assertTrue(gen.contains(
                ".map(\"getMeta\", a->a.getMeta()).map(\"getScheme\", a->a.getScheme())"),
                "The scheme-kind paired short form must render on the wrapper item");
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void f1_mapCounterpartyRoleEnum_referenceShortForm_byteLock() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, MAP_COUNTERPARTY_ROLE_ENUM);
        String gen = gen(cdm6FnOutput, MAP_COUNTERPARTY_ROLE_ENUM);
        // Negative witness: the pre-facet wrapper deref into the by-name getter
        // (count 0 in golden — golden keeps the wrapper, and the Party import drops).
        assertFalse(gen.contains(".<Party>map(\"Type coercion\""),
                "The Reference wrapper must NOT deref before the meta-feature read");
        assertFalse(gen.contains("import cdm.base.staticdata.party.Party;"),
                "The deref witness gone -> the Party import drops (golden)");
        assertEquals(2, count(gen, ".map(\"getReference\", a->a.getExternalReference())"),
                "Both filter seats render the reference-kind short form");
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void f1_mapMultipleCounterpartyRoleEnum_fourSeats_byteLock() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR,
                MAP_MULTIPLE_COUNTERPARTY_ROLE_ENUM);
        assertEquals(4, count(gen(cdm6FnOutput, MAP_MULTIPLE_COUNTERPARTY_ROLE_ENUM),
                ".map(\"getReference\", a->a.getExternalReference())"),
                "All four filter seats render the reference-kind short form");
    }

    /**
     * F1 heal lock (still divergent — the mapSingleToItem block-ladder class is a
     * separate deferred mechanism): all FIVE scheme-kind seats render the paired
     * short form, exactly golden's count.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void f1_extractHkmaSchemeName_fiveSeats_contentLock() {
        assertEquals(5, count(gen(drrFnOutput, EXTRACT_HKMA_SCHEME_NAME),
                ".map(\"getMeta\", a->a.getMeta()).map(\"getScheme\", a->a.getScheme())"),
                "All five if-arm seats carry the scheme-kind short form (golden count 5)");
    }

    // ---------------------------------------------------------------- F2 lock

    @Test
    @EnabledIf("cdm6CellAvailable")
    void f2_mapPriceQuantity_aliasSigAndPlainSetter_byteLock() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, MAP_PRICE_QUANTITY);
        String gen = gen(cdm6FnOutput, MAP_PRICE_QUANTITY);
        // Negative witness: the Value-setter variant (count 0 in golden).
        assertFalse(gen.contains(".setQuantityValue("),
                "The wrapper-element value takes the PLAIN setter (upstream "
                + "requiresValueAssignment)");
        assertEquals(2, count(gen,
                "MapperC<? extends FieldWithMetaNonNegativeQuantitySchedule> q("),
                "The q alias signature types MapperC in BOTH the abstract and impl decls");
    }

    // ---------------------------------------------------------------- F3 locks

    @Test
    @EnabledIf("cdm6CellAvailable")
    void f3_mapMultipleExercise_withMetaReference_byteLock() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, MAP_MULTIPLE_EXERCISE);
        String gen = gen(cdm6FnOutput, MAP_MULTIPLE_EXERCISE);
        // Negative witness: the M7b-1 stub (count 0 in golden — it never compiled).
        assertFalse(gen.contains("null.toBuilder()"),
                "The with-meta stub must be gone at the ctor seat");
        assertTrue(gen.contains(
                "ReferenceWithMetaMoney.builder().setValue(null).setExternalReference("),
                "The typed Reference construction renders with the null value + external ref");
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void f3_mapEuropeanExerciseTerms_byteLock() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, MAP_EUROPEAN_EXERCISE_TERMS);
    }

    // ---------------------------------------------------------------- F4 locks

    @Test
    @EnabledIf("cdm5CellAvailable")
    void f4_resolvePerfObsIdentifiers_listRewrap_byteLock() throws IOException {
        assertByteMatchesGolden(cdm5FnOutput, CDM5_GOLDEN_DIR, RESOLVE_PERF_OBS_IDENTIFIERS_CDM5);
        assertTrue(gen(cdm5FnOutput, RESOLVE_PERF_OBS_IDENTIFIERS_CDM5).contains(
                ".<FieldWithMetaProductIdentifier>map(\"Type coercion\", "
                + "referenceWithMetaProductIdentifier -> {"),
                "The elementwise Reference->Field block renders at the ADD seat");
    }

    @Test
    @EnabledIf("cdm5CellAvailable")
    void f4_resolveIrObsIdentifiers_singleRewrap_byteLock() throws IOException {
        assertByteMatchesGolden(cdm5FnOutput, CDM5_GOLDEN_DIR, RESOLVE_IR_OBS_IDENTIFIERS);
        String gen = gen(cdm5FnOutput, RESOLVE_IR_OBS_IDENTIFIERS);
        assertTrue(gen.contains("final FieldWithMetaFloatingRateOption ifThenElseResult;"),
                "The single-form if/else hoist declares the field-wrapper local");
        assertTrue(gen.contains(".setRateOption(ifThenElseResult);"),
                "The leaf setter takes the converted local verbatim");
    }

    /**
     * F4 cdm6-twin FLIP lock (CONVERTED at PR #349 per this lock's own #348 promise —
     * facet nestedChoiceLeafProjection): the twin's
     * {@code set identifiers -> observable -> Index -> InterestRateIndex} is a
     * NESTED-choice-option segment path (every segment past {@code observable} is an
     * unresolved capitalized option — the P348-F4b probe); the multi-hop choice
     * projection ({@code projectNestedChoiceLeafOrNull} — per-hop
     * {@code RChoiceTypeRef.asRDataType} with by-name typeCall resolution) recovers the
     * leaf option ({@code [metadata location]} → FIELD_WITH_META), and the #348
     * SINGLE-form rewrap renders golden verbatim.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void f4_resolveIrObsIdentifiersCdm6_nestedChoiceTwin_byteLock() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, RESOLVE_IR_OBS_IDENTIFIERS);
        String gen = gen(cdm6FnOutput, RESOLVE_IR_OBS_IDENTIFIERS);
        assertTrue(gen.contains("final FieldWithMetaInterestRateIndex ifThenElseResult;"),
                "The single-form if/else hoist declares the field-wrapper local "
                + "(the multi-hop projection resolved the nested-option leaf)");
        assertTrue(gen.contains(".setInterestRateIndex(ifThenElseResult);"),
                "The leaf setter takes the converted local verbatim");
    }

    // ---------------------------------------------------------------- F5 lock

    @Test
    @EnabledIf("cdm6CellAvailable")
    void f5_mapReturnSwapLeg_toEnumClaimsBare_byteLock() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR,
                MAP_RETURN_SWAP_LEG_TO_SETTLEMENT_TERMS);
        String gen = gen(cdm6FnOutput, MAP_RETURN_SWAP_LEG_TO_SETTLEMENT_TERMS);
        // Negative witness: the eager source-wins FQN (count 0 in golden).
        assertFalse(gen.contains("cdm.product.common.settlement.SettlementTypeEnum.valueOf"),
                "The claim-winning target must render BARE at the to-enum seat");
        assertTrue(gen.contains("e -> SettlementTypeEnum.valueOf(e.name())"),
                "The bare target valueOf renders (the cdm enum owns the file's claim)");
        assertTrue(gen.contains(".<fpml.consolidated.fpmlenum.SettlementTypeEnum>map("),
                "The claim-losing fpml witness stays FQN (first-claim-wins)");
    }

    // ---------------------------------------------------------------- F6 locks

    @Test
    @EnabledIf("cdm6CellAvailable")
    void f6_qualifyFxParameterReturnCorrelation_trailingWitness_byteLock() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR,
                QUALIFY_FX_PARAMETER_RETURN_CORRELATION);
        String gen = gen(cdm6FnOutput, QUALIFY_FX_PARAMETER_RETURN_CORRELATION);
        assertTrue(gen.contains(".<ForeignExchangeRateIndex>map(\"getForeignExchangeRateIndex\""),
                "The trailing step after the re-rooted choice-option nav carries its witness");
        // Negative witness: the witness-less form `.map("getForeignExchangeRateIndex"`
        // (dot directly before map — count 0 in golden, whose only occurrence is
        // witness-prefixed `>map(`).
        assertFalse(gen.contains(".map(\"getForeignExchangeRateIndex\""),
                "The witness-less trailing step must be gone");
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void f6_observableIsCommodity_requalifyAndWitness_byteLock() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, OBSERVABLE_IS_COMMODITY);
        String gen = gen(cdm6FnOutput, OBSERVABLE_IS_COMMODITY);
        assertTrue(gen.contains("MapperS.of(AssetClassEnum.COMMODITY)"),
                "The type-shadowed bare Commodity requalifies to the enum literal "
                + "(the deep-sibling rung types the LHS)");
        // Negative witness: the bare mis-bound TYPE wrap (count 0 in ALL goldens —
        // the corpus law re-verified at #348 sizing).
        assertFalse(gen.contains("MapperS.of(Commodity)"),
                "The bare mis-bound type name must be gone from the comparison seat");
        assertTrue(gen.contains(".<Commodity>map(\"getCommodity\""),
                "The trailing step after the re-rooted Asset nav carries its witness");
    }

    // ---------------------------------------------------------------- helpers

    private static int count(String haystack, String needle) {
        int n = 0;
        for (int i = haystack.indexOf(needle); i >= 0; i = haystack.indexOf(needle, i + 1)) {
            n++;
        }
        return n;
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
                + path + " (PR #348 the six-facet compose).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
