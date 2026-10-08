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
import static com.regnosys.rosetta.testutil.GenerationErrors.assertNoGenerationErrors;

/**
 * PR #341 anchor — the five-facet compose (11 FUNCTION flips: 4 drr + 4 cdm6 + 3 cdm5).
 *
 * <p><b>Facet 1 — {@code aliasInvocationThenHoist}</b> (2 drr flips:
 * ChangeInNotionalAmountLeg1/2): the #251-M2 alias VALUE-then deferral CLOSED for the
 * invocation-extract shape — a with-args FUNCTION invocation projection
 * ({@code … then extract PayoutLeg1(…)}) admits at
 * {@code FunctionExpressionRenderer.renderAliasThenHoistOrNull} (its alias signature
 * already agrees via the generic then-body fall-through), rendering golden's
 * {@code final MapperS<TradableProduct> thenArg = <base>; return
 * thenArg.mapSingleToItem(…);}. The NEW arm alone carries the #250 no-control-flow walk
 * over the WHOLE chain ({@code CollectionHandler.thenChainHasControlFlow}, now public):
 * a control-flow base compiled in the sink-marked scope registers its ifThenElseResult
 * hoist even though {@code tryDeepThenHoist} declines the then, leaving a PARTIAL
 * restructure — the cp1 MapGenericProductPriceQuantityList catch, locked below.
 *
 * <p><b>Facet 2 — {@code builderSetMetaRefGetOrCreateValue}</b> (1 drr flip:
 * Enrich_ReportableEventWithCfiCode): {@code renderSetBuilderChain} derefs
 * {@code .getOrCreateValue()} after an INTERMEDIATE single-card {@code [metadata
 * reference]} segment (ReferenceWithMetaX) — the bare hop into a VALUE-type attribute
 * never compiled. The next-segment-must-RESOLVE belt is the cp2 catch: a wrapper-level
 * meta-field target ({@code setExternalReference}, never a model RAttribute) sets
 * directly on the wrapper builder and is GREEN today — the MapSwapResetDates green pin
 * below locks the belt.
 *
 * <p><b>Facet 3 — {@code aliasParamImportCollisionFqn}</b> (1 cdm6 flip:
 * MapProtectionTerms): a #195 collision-nulled INPUT param ({@code typeFqn == null},
 * {@code typeName} = the dotted FQN) renders FULLY-QUALIFIED at every seat, but the
 * alias-walk refs channel ({@code AliasModel.inferredRefs}) still imported its JavaClass
 * — the duplicate same-simple-name import never compiled. Dropped from the refs channel
 * exactly like the #227 sentinel-resolved losers.
 *
 * <p><b>Facet 4 — {@code filterPredicateOperandNavReRoot}</b> (1 drr flip:
 * Extract_UTIPropietary): the #282 bare-name item re-root un-rule-scoped at THREE
 * coordinated seats — the RAttribute-symbol admission (ReferenceHandler), the inferred
 * item-type fallback ({@code synthesizeImplicitItemBareNav}), and the first-step
 * lambda-param naming arm (NavigationHandler's no-from-type
 * {@code filter_predicate_item_typing} arm). The green-safety discriminator was always
 * the decline ladder (closure param / FUNCTION-SCOPE name / non-implicit lambda / no
 * exact-name attribute). Extract_BondConnect stays declined ({@code itemTypeNull} — the
 * 2-hop meta-nav case), locked below as the near-flip-pool breadcrumb.
 *
 * <p><b>Facet 5 — {@code flattenLoLThenArgElement}</b> (6 cdm flips:
 * ExtractTradeCollateralPrice + ExtractTradeCollateralQuantity + ExtractTradePurchasePrice
 * × cdm5+cdm6): the FIFTH anchor disjunct at the #297 element-preserving thenArg recovery
 * — a bare-item FLATTEN over a MapperListOfLists propagates the LoL decl element
 * ({@code flattenList()} is {@code MapperListOfLists<T> -> MapperC<T>}, invariant: the
 * TYPE SYSTEM is the anchor; a green file's fire is a byte-neutral no-op by the same
 * law).
 *
 * <p>Every pre-fix form was NON_COMPILING (the inline runtime {@code .then(} — no
 * {@code Mapper.then(Function)} exists; a value-type hop on a reference-wrapper builder;
 * a duplicate same-simple-name import; an undefined bare symbol operand; a bare-element
 * decl over a wrapper LoL — a generics mismatch) — no green file carries any pre-fix
 * form. Whole-file byte comparisons run through the REAL D11 FUNCTION generation path
 * and revert RED without the facets.
 */
class AliasThenHoistNavRerootComposeTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM5_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-5.38.0");
    private static final Path CDM5_GOLDEN_DIR =
            CDM5_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    // Facet 1 flip carriers (drr FUNCTION — alias-override then-hoist).
    private static final String CHANGE_IN_NOTIONAL_LEG1 =
            "drr/regulation/cftc/rewrite/functions/ChangeInNotionalAmountLeg1.java";
    private static final String CHANGE_IN_NOTIONAL_LEG2 =
            "drr/regulation/cftc/rewrite/functions/ChangeInNotionalAmountLeg2.java";
    // Facet 2 flip carrier (drr FUNCTION — builder-SET meta-reference deref).
    private static final String ENRICH_REPORTABLE_EVENT_CFI =
            "drr/enrichment/upi/functions/Enrich_ReportableEventWithCfiCode.java";
    // Facet 3 flip carrier (cdm6 FUNCTION — the fpml/cdm ProtectionTerms import collision).
    private static final String MAP_PROTECTION_TERMS_CDM6 =
            "cdm/ingest/fpml/confirmation/product/creditdefaultswap/functions/MapProtectionTerms.java";
    // Facet 4 flip carrier (drr FUNCTION — the filter-operand bare-name re-root).
    private static final String EXTRACT_UTI_PROPIETARY =
            "drr/regulation/hkma/rewrite/valuation/functions/Extract_UTIPropietary.java";
    // Facet 5 flip carriers (cdm5 + cdm6 FUNCTION — the LoL-flatten element propagation).
    private static final String EXTRACT_TRADE_COLLATERAL_PRICE =
            "cdm/event/common/functions/ExtractTradeCollateralPrice.java";
    private static final String EXTRACT_TRADE_COLLATERAL_QUANTITY =
            "cdm/event/common/functions/ExtractTradeCollateralQuantity.java";
    private static final String EXTRACT_TRADE_PURCHASE_PRICE =
            "cdm/event/common/functions/ExtractTradePurchasePrice.java";
    // Facet 1 decline lock — the cp1 catch carrier: a control-flow BASE (inline-ternary
    // conditional) feeding an invocation-extract then. The chain must keep the inline
    // `.then(` (witness: counts 0 in every golden) and must NOT gain the partial
    // base-restructure (the leak marker counts 0 in golden AND in correct gen).
    private static final String MAP_GENERIC_PRODUCT_PQ_LIST_CDM6 =
            "cdm/ingest/fpml/confirmation/product/genericproduct/functions/MapGenericProductPriceQuantityList.java";
    // Facet 2 green pin — the cp2 catch carrier: the path targets a WRAPPER-level meta
    // field (setExternalReference), whose segment never resolves as a model RAttribute,
    // so the deref must NOT fire and the file stays byte-identical to golden.
    private static final String MAP_SWAP_RESET_DATES_CDM6 =
            "cdm/ingest/fpml/confirmation/product/swap/functions/MapSwapResetDates.java";
    // Facet 4 decline breadcrumb — the 2-hop meta-nav sibling (`item -> meta -> scheme`):
    // the filter item type does not resolve (itemTypeNull), so the operand keeps the bare
    // non-compiling `MapperS.of(scheme)` (witness: counts 0 in golden — golden re-roots
    // through getMeta().getScheme()). Returned to the near-flip pool at #341 scope.
    private static final String EXTRACT_BOND_CONNECT =
            "drr/regulation/hkma/rewrite/trade/functions/Extract_BondConnect.java";

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
        if (drrCellAvailable()) {
            drrFnOutput = generateFunctions(
                    new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT));
        }
        if (cdm6CellAvailable()) {
            cdm6FnOutput = generateFunctions(
                    new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CDM6_CELL_ROOT));
        }
        if (cdm5CellAvailable()) {
            cdm5FnOutput = generateFunctions(
                    new D11CorpusRegressionTest.CellSpec("cdm", "5.38.0", CDM5_CELL_ROOT));
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
        assertNoGenerationErrors(funcGen.generateWithErrors(output));
        return output;
    }

    // ==== facet 1 flip locks (revert-RED) ====

    @Test
    @EnabledIf("drrCellAvailable")
    void changeInNotionalAmountLeg1_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFnOutput, DRR_GOLDEN_DIR, CHANGE_IN_NOTIONAL_LEG1);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void changeInNotionalAmountLeg2_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFnOutput, DRR_GOLDEN_DIR, CHANGE_IN_NOTIONAL_LEG2);
    }

    /**
     * Positive-content lock (revert-RED): the payoutBefore alias-override hoists the chain
     * base as golden's statement local and re-roots the invocation-extract consumer on it —
     * and the inline runtime {@code .then(} (which counts 0 in every golden, corpus-wide)
     * is GONE from the file.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void changeInNotionalAmountLeg1_aliasHoistLandsAndInlineThenGone() {
        String gen = gen(drrFnOutput, CHANGE_IN_NOTIONAL_LEG1);
        assertTrue(gen.contains(
                "final MapperS<TradableProduct> thenArg = MapperS.of(beforeTradeForEvent"
                        + ".evaluate(transactionReportInstruction))"),
                "Expected golden's hoisted thenArg decl for the payoutBefore alias base");
        assertTrue(gen.contains("return thenArg"),
                "Expected the consumer re-rooted on the hoisted thenArg");
        assertFalse(gen.contains(".then("),
                "The inline runtime .then( must be gone (counts 0 in every golden)");
    }

    // ==== facet 2 flip lock + positive content (revert-RED) ====

    @Test
    @EnabledIf("drrCellAvailable")
    void enrichReportableEventWithCfiCode_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFnOutput, DRR_GOLDEN_DIR, ENRICH_REPORTABLE_EVENT_CFI);
    }

    /**
     * Positive-content lock (revert-RED): BOTH builder chains deref the reference-wrapped
     * {@code before} intermediate through {@code .getOrCreateValue()} on the same line —
     * golden's exact form (the bare {@code .getOrCreateBefore()} hop into
     * {@code .getOrCreateTrade()} never compiled: the reference builder carries no
     * value-type accessors).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void enrichReportableEventWithCfiCode_derefsBothBeforeHops() {
        String gen = gen(drrFnOutput, ENRICH_REPORTABLE_EVENT_CFI);
        int first = gen.indexOf(".getOrCreateBefore().getOrCreateValue()");
        assertTrue(first >= 0, "Expected the first getOrCreateValue deref");
        assertTrue(gen.indexOf(".getOrCreateBefore().getOrCreateValue()", first + 1) > first,
                "Expected the second getOrCreateValue deref (two builder chains)");
    }

    // ==== facet 3 flip lock + positive content (revert-RED) ====

    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapProtectionTerms_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, MAP_PROTECTION_TERMS_CDM6);
    }

    /**
     * Positive-content lock (revert-RED): the collision-nulled fpml INPUT type renders
     * FULLY-QUALIFIED in the signatures (unchanged), the OUTPUT type keeps the sole
     * import, and the spurious fpml import — the duplicate same-simple-name compile
     * error, the ONE diff line pre-facet — is GONE.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapProtectionTerms_fpmlImportSuppressedFqnKept() {
        String gen = gen(cdm6FnOutput, MAP_PROTECTION_TERMS_CDM6);
        assertFalse(gen.contains("import fpml.consolidated.cd.ProtectionTerms;"),
                "The colliding fpml input import must be suppressed (golden carries none)");
        assertTrue(gen.contains("import cdm.product.asset.ProtectionTerms;"),
                "The output type keeps the sole ProtectionTerms import");
        assertTrue(gen.contains("evaluate(fpml.consolidated.cd.ProtectionTerms fpmlProtectionTerms"),
                "The collision-nulled input renders FQN in the evaluate signature");
    }

    // ==== facet 4 flip lock + positive content (revert-RED) ====

    @Test
    @EnabledIf("drrCellAvailable")
    void extractUtiPropietary_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFnOutput, DRR_GOLDEN_DIR, EXTRACT_UTI_PROPIETARY);
    }

    /**
     * Positive-content lock (revert-RED): the filter-predicate operand's bare
     * {@code identifierType} re-roots as golden's item navigation with the lambda param
     * named from the INFERRED item type ({@code _tradeIdentifier}, the receiver-type law —
     * not the feature-name fallback {@code _identifierType}).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void extractUtiPropietary_operandRerootsWithReceiverTypedParam() {
        String gen = gen(drrFnOutput, EXTRACT_UTI_PROPIETARY);
        assertTrue(gen.contains("item.<TradeIdentifierTypeEnum>map(\"getIdentifierType\","
                        + " _tradeIdentifier -> _tradeIdentifier.getIdentifierType())"),
                "Expected golden's re-rooted operand nav with the receiver-typed param");
        assertFalse(gen.contains("MapperS.of(identifierType)"),
                "The bare undefined operand must be gone (counts 0 in golden)");
    }

    // ==== facet 5 flip locks + positive content (revert-RED) ====

    @Test
    @EnabledIf("cdm5CellAvailable")
    void extractTradeCollateralPrice_cdm5_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5FnOutput, CDM5_GOLDEN_DIR, EXTRACT_TRADE_COLLATERAL_PRICE);
    }

    @Test
    @EnabledIf("cdm5CellAvailable")
    void extractTradeCollateralQuantity_cdm5_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5FnOutput, CDM5_GOLDEN_DIR, EXTRACT_TRADE_COLLATERAL_QUANTITY);
    }

    @Test
    @EnabledIf("cdm5CellAvailable")
    void extractTradePurchasePrice_cdm5_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5FnOutput, CDM5_GOLDEN_DIR, EXTRACT_TRADE_PURCHASE_PRICE);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void extractTradeCollateralPrice_cdm6_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, EXTRACT_TRADE_COLLATERAL_PRICE);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void extractTradeCollateralQuantity_cdm6_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, EXTRACT_TRADE_COLLATERAL_QUANTITY);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void extractTradePurchasePrice_cdm6_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, EXTRACT_TRADE_PURCHASE_PRICE);
    }

    /**
     * Positive-content lock (revert-RED): the flatten thenArg preserves the LoL decl's
     * meta-wrapper element ({@code MapperC<FieldWithMetaNonNegativeQuantitySchedule>}, not
     * the meta-blind bare {@code MapperC<NonNegativeQuantitySchedule>} — a generics
     * mismatch that never compiled against {@code MapperListOfLists
     * <FieldWithMetaNonNegativeQuantitySchedule>.flattenList()}).
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void extractTradeCollateralQuantity_flattenKeepsWrapperElement() {
        String gen = gen(cdm6FnOutput, EXTRACT_TRADE_COLLATERAL_QUANTITY);
        assertTrue(gen.contains("final MapperC<FieldWithMetaNonNegativeQuantitySchedule> thenArg1 = thenArg0"),
                "Expected the LoL element propagated into the flatten thenArg decl");
        assertFalse(gen.contains("final MapperC<NonNegativeQuantitySchedule> thenArg1"),
                "The meta-blind bare element decl must be gone (never compiled)");
    }

    // ==== decline / green-preservation locks ====

    /**
     * GRADUATED at PR #392 (facets inLambdaArgSeatCondAdmit + fnCondBaseLadder*): the
     * decline this lock pinned was exactly the fn-arg-conditional class the #392
     * bodies-loop arm made HANDLED — {@code unitFromQuotation}'s chain now decomposes
     * (the blank-final {@code final MapperS<QuotationCharacteristicsModel> thenArg;}
     * elseless base + the in-lambda {@code Currency ifThenElseResult = null;}
     * mutable-null ite at the {@code MapCurrency} arg seat via the #355 lambda channel)
     * and the whole file is byte-identical to golden. The old positive witness (the
     * inline runtime {@code .then(}) inverts to the flip's negative; the cp1
     * partial-leak marker stays count-0 on both sides (the over-fire guard survives).
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapGenericProductPriceQuantityList_fnArgCondDecomposed_byteIdentical()
            throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, MAP_GENERIC_PRODUCT_PQ_LIST_CDM6);
        String gen = gen(cdm6FnOutput, MAP_GENERIC_PRODUCT_PQ_LIST_CDM6);
        assertFalse(gen.contains(".then(item -> item"),
                "The flipped render must carry NO inline runtime .then( (the #392 decomposition)");
        assertFalse(gen.contains("QuotationCharacteristicsModel ifThenElseResult = null;"),
                "The partial base-restructure (the cp1 leak) must not appear");
    }

    /**
     * Facet 2 green pin (holds on BOTH sides of the facet, by design): MapSwapResetDates
     * targets the WRAPPER-level meta field ({@code … -> calculationPeriodDatesReference ->
     * externalReference}) — the next segment never resolves as a model RAttribute, so the
     * getOrCreateValue deref must NOT fire and the whole file stays byte-identical to
     * golden (it was GREEN before the facet; the un-belted first cut regressed it, the cp2
     * catch).
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapSwapResetDates_wrapperFieldSetterStaysGreen() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, MAP_SWAP_RESET_DATES_CDM6);
        String gen = gen(cdm6FnOutput, MAP_SWAP_RESET_DATES_CDM6);
        assertTrue(gen.contains(".getOrCreateCalculationPeriodDatesReference()"),
                "The wrapper hop must render");
        assertFalse(gen.contains(".getOrCreateCalculationPeriodDatesReference().getOrCreateValue()"),
                "The deref must NOT fire on a wrapper-field-targeting path (the cp2 belt)");
    }

    /**
     * CONVERTED at PR #348 (facet metaPathShortForm) — the breadcrumb's own javadoc
     * promise: Extract_BondConnect's {@code scheme} operand now renders upstream
     * buildMapFunc's a->a short form ({@code item.map("getMeta",
     * a->a.getMeta()).map("getScheme", a->a.getScheme())}) via the #348 bare-name
     * meta rung (implicitItemArgMeta proves the {@code FieldWithMetaString} item at
     * the itemTypeNull decline — the P348-M1 probe), and the whole file is
     * byte-identical to golden. The old negative witness ({@code MapperS.of(scheme)},
     * count 0 in golden) inverts to the flip's negative.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void extractBondConnect_metaShortFormFlips() throws IOException {
        assertByteMatchesGolden(drrFnOutput, DRR_GOLDEN_DIR, EXTRACT_BOND_CONNECT);
        String gen = gen(drrFnOutput, EXTRACT_BOND_CONNECT);
        assertFalse(gen.contains("MapperS.of(scheme)"),
                "The bare mis-bound form must be gone (the #348 meta short-form facet)");
        assertTrue(gen.contains(".map(\"getMeta\", a->a.getMeta()).map(\"getScheme\", a->a.getScheme())"),
                "The a->a meta short form must render on the wrapper item");
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
                + path + " (PR #341 the five-facet compose).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
