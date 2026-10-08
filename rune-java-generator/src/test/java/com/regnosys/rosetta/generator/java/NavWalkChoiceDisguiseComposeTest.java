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
 * PR #344 anchor — the three-facet compose (13 FUNCTION flips: 12 cdm6 + 1 cdm5).
 * All three facets align the shared receiver-type WALK ({@code NavigationHandler}) with
 * the resolution the RENDER half already performs — the P344 System.err probe decoded
 * every gap before design ({@code evr=underlier->Product attr=NULL},
 * {@code evr=asset->Instrument attr=NULL}, {@code RRDT0 NO-ARM recvClass=RDeepFeatureCall}).
 *
 * <p><b>F1 — {@code navWalkChoiceDisguise}</b> (8 cdm6 flips): the walk learns the three
 * receiver shapes it was blind to. Arm 1: a disguised 2-name chain whose input/output ROOT
 * is CHOICE-typed ({@code underlier -> Product}) narrows the root gm-aware
 * ({@code resolveTypeCall} + {@code asRDataType} projection) and resolves the leaf directly
 * plus through the #207 choice-supertype options. Arm 2: a disguised chain whose ROOT is a
 * feature on the enclosing lambda's implicit ITEM ({@code asset -> Instrument} inside
 * {@code transfers filter [...]}) resolves through the SAME {@code implicitItemDataType}
 * walk the naming/witness consumers read — the #225 conversion-scoped shape folded into the
 * SHARED resolver. Arm 3: an {@link com.regnosys.rosetta.ast.expressions.RDeepFeatureCall}
 * receiver resolves through the SAME {@code resolveDeepFeature} the deep-call render reads
 * (the walk previously had NO arm for it). Arm 5: the chained-RFeatureCall lambda-NAMING
 * branch recovers gm-aware when the static walk declines, so the chained var derives from
 * the receiver's resolved type (golden {@code transferableProduct ->}) instead of the
 * capitalized feature-name fallback ({@code TransferableProduct ->}). The {@code <T>}
 * witness, map/mapC arity, meta Type-coercion insertion, DeepPathUtil wiring and lambda
 * naming all cascade from the ONE resolution.
 *
 * <p><b>F2 — {@code deepPathParamEscape}</b> (3 cdm6 flips): the deep-path step's lambda
 * var scope-disambiguates via {@code registerDeferredLambdaParam} exactly like every other
 * type-derived lambda var — golden escapes {@code _instrument ->
 * instrumentDeepPathUtil.chooseInstrumentType(_instrument)} where the desired name collides
 * with the enclosing method's {@code instrument} param (a bare colliding form is an illegal
 * Java lambda shadow, so no green file carries one — the 38 bare {@code product} golden
 * deep-path lambdas stay bare). The DeepPathUtil FIELD keeps the BARE type-derived name.
 *
 * <p><b>F3 — {@code itemRootDisguiseMapperC}</b> (2 flips — the cdm5↔cdm6 twin):
 * {@code chainRendersMapperC}'s disguised-evr arm gains the FUNCTION-lambda siblings of the
 * #310 rule arm — the compiler-carrying leaf-cardinality read plus the MULTI implicit-item
 * HEAD proof ({@code quantity -> unit -> currency} over PriceQuantity items renders
 * {@code item.<FWM..>mapC(...)}, so its meta coercions are BARE-unguarded like golden's
 * plain {@code fieldWithMetaString -> fieldWithMetaString.getValue()}; the single-headed
 * observable coercions in the SAME carriers keep the guard golden also carries).
 *
 * <p>Whole-file byte comparisons run through the REAL D11 FUNCTION generation path and
 * revert RED without the facets (pre-fix verdicts: FilterSecurityTransfers, MapTradeLotList,
 * UnderlierQualification, FindMatching-cdm5 COMPILES-divergent; the other 9 NON_COMPILING).
 */
class NavWalkChoiceDisguiseComposeTest {

    private static final Path CDM5_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-5.38.0");
    private static final Path CDM5_GOLDEN_DIR =
            CDM5_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    // F1 flip carriers (walk arms 1+2+3+5 — witness/arity/coercion/naming cascade).
    private static final String FILTER_SECURITY_TRANSFERS =
            "cdm/event/common/functions/FilterSecurityTransfers.java";
    private static final String INTEREST_CASH_SETTLEMENT_AMOUNT =
            "cdm/event/common/functions/InterestCashSettlementAmount.java";
    private static final String MAP_RELATED_PARTY_TO_PARTY_ROLE =
            "cdm/ingest/fpml/confirmation/party/functions/MapRelatedPartyToPartyRole.java";
    private static final String MAP_OPTION_STRIKE_REFERENCE_SWAP_CURVE =
            "cdm/ingest/fpml/confirmation/pricequantity/functions/MapOptionStrikeReferenceSwapCurve.java";
    private static final String MAP_CALCULATION_PERIOD_LIST =
            "cdm/ingest/fpml/confirmation/product/swap/functions/MapCalculationPeriodList.java";
    private static final String MAP_TRADE_LOT_LIST =
            "cdm/ingest/fpml/confirmation/tradestate/functions/MapTradeLotList.java";
    private static final String QUALIFY_ASSET_CLASS_FOREIGN_EXCHANGE =
            "cdm/product/qualification/functions/Qualify_AssetClass_ForeignExchange.java";
    private static final String UNDERLIER_QUALIFICATION =
            "cdm/product/qualification/functions/UnderlierQualification.java";
    // F2 flip carriers (deep-path lambda-var scope escape).
    private static final String AUXILIAR_EFFECTIVE_DATE =
            "cdm/margin/schedule/functions/AuxiliarEffectiveDate.java";
    private static final String AUXILIAR_TERMINATION_DATE =
            "cdm/margin/schedule/functions/AuxiliarTerminationDate.java";
    private static final String QUALIFY_INSTRUMENT_TYPE_EQUITY =
            "cdm/product/qualification/functions/Qualify_InstrumentTypeEquity.java";
    // F3 flip carrier (item-root disguise MapperC guard-kind) — the cdm5↔cdm6 twin.
    private static final String FIND_MATCHING_INDEX_TRANSITION_INSTRUCTION =
            "cdm/event/common/functions/FindMatchingIndexTransitionInstruction.java";

    private static Map<String, String> cdm5FnOutput;
    private static Map<String, String> cdm6FnOutput;

    static boolean cdm5CellAvailable() {
        return Files.isDirectory(CDM5_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM5_GOLDEN_DIR);
    }

    static boolean cdm6CellAvailable() {
        return Files.isDirectory(CDM6_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM6_GOLDEN_DIR);
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
        // Surface any per-function emission failures directly (the #343 Copilot R1
        // convention): the flip carriers all emit (D11 shows emitted==expected,
        // missingOutput=0 for both cells), so this list is empty today; a future emission
        // regression fails here with the underlying GenerationException(s).
        var errors = funcGen.generateWithErrors(output);
        assertTrue(errors.isEmpty(),
                () -> "Function generation reported " + errors.size() + " error(s): " + errors);
        return output;
    }

    // ==== F1 navWalkChoiceDisguise flip locks (revert-RED) ====

    @Test
    @EnabledIf("cdm6CellAvailable")
    void filterSecurityTransfers_cdm6_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, FILTER_SECURITY_TRANSFERS);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void interestCashSettlementAmount_cdm6_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, INTEREST_CASH_SETTLEMENT_AMOUNT);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapRelatedPartyToPartyRole_cdm6_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, MAP_RELATED_PARTY_TO_PARTY_ROLE);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapOptionStrikeReferenceSwapCurve_cdm6_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR,
                MAP_OPTION_STRIKE_REFERENCE_SWAP_CURVE);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapCalculationPeriodList_cdm6_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, MAP_CALCULATION_PERIOD_LIST);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapTradeLotList_cdm6_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, MAP_TRADE_LOT_LIST);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void qualifyAssetClassForeignExchange_cdm6_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR,
                QUALIFY_ASSET_CLASS_FOREIGN_EXCHANGE);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void underlierQualification_cdm6_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, UNDERLIER_QUALIFICATION);
    }

    /**
     * Positive-content lock (F1 arms 1+5): the choice-option chain resolves — the
     * TransferableProduct step carries its witness AND names its lambda var from the
     * receiver's resolved type ({@code product}), never the capitalized feature-name
     * fallback. The negative witness counts 0 in golden (verified against
     * corpus-baseline-9.83) — a token the flip REMOVES.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void underlierQualification_cdm6_choiceChainResolvesTypedAndLowercased() {
        assertNotNull(cdm6FnOutput);
        String gen = cdm6FnOutput.get(UNDERLIER_QUALIFICATION);
        assertNotNull(gen, "UnderlierQualification not generated");
        assertTrue(gen.contains(
                "<TransferableProduct>map(\"getTransferableProduct\", product -> product.getTransferableProduct())"),
                "the choice-option step must carry its witness + the type-derived lambda var");
        assertFalse(gen.contains("Product -> Product.getTransferableProduct()"),
                "the capitalized feature-name fallback must be gone");
    }

    // ==== F2 deepPathParamEscape flip locks (revert-RED) ====

    @Test
    @EnabledIf("cdm6CellAvailable")
    void auxiliarEffectiveDate_cdm6_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, AUXILIAR_EFFECTIVE_DATE);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void auxiliarTerminationDate_cdm6_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, AUXILIAR_TERMINATION_DATE);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void qualifyInstrumentTypeEquity_cdm6_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, QUALIFY_INSTRUMENT_TYPE_EQUITY);
    }

    /**
     * Positive-content lock (F2): the deep-path step's lambda var escapes the enclosing
     * method-param collision ({@code _instrument}) while the DeepPathUtil FIELD keeps the
     * bare type-derived name. The unescaped shadow counts 0 in golden (verified) — an
     * illegal Java lambda shadow the flip REMOVES.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void qualifyInstrumentTypeEquity_cdm6_deepPathVarEscapesFieldStaysBare() {
        assertNotNull(cdm6FnOutput);
        String gen = cdm6FnOutput.get(QUALIFY_INSTRUMENT_TYPE_EQUITY);
        assertNotNull(gen, "Qualify_InstrumentTypeEquity not generated");
        assertTrue(gen.contains(
                "_instrument -> instrumentDeepPathUtil.chooseInstrumentType(_instrument)"),
                "the deep-path lambda var must scope-escape; the field must stay bare");
        assertFalse(gen.contains("chooseInstrumentType\", instrument ->"),
                "the unescaped method-param shadow must be gone");
    }

    // ==== F3 itemRootDisguiseMapperC flip locks (revert-RED) — the cdm5↔cdm6 twin ====

    @Test
    @EnabledIf("cdm6CellAvailable")
    void findMatchingIndexTransitionInstruction_cdm6_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR,
                FIND_MATCHING_INDEX_TRANSITION_INSTRUCTION);
    }

    @Test
    @EnabledIf("cdm5CellAvailable")
    void findMatchingIndexTransitionInstruction_cdm5_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5FnOutput, CDM5_GOLDEN_DIR,
                FIND_MATCHING_INDEX_TRANSITION_INSTRUCTION);
    }

    /**
     * Positive-content lock (F3): the multi-headed item-root disguise
     * ({@code quantity -> unit -> currency}) rides MapperC, so its meta Type-coercion is
     * the BARE-unguarded plain form; the numbered null-guarded token
     * ({@code fieldWithMetaString0}) counts 0 in golden for BOTH cells (verified) — a
     * token the flip REMOVES. The single-headed observable coercions in the same file
     * keep the numbered guard golden also carries (asserted via the whole-file locks).
     */
    @Test
    @EnabledIf("cdm5CellAvailable")
    void findMatchingIndexTransitionInstruction_cdm5_multiHeadCoercionUnguarded() {
        assertNotNull(cdm5FnOutput);
        String gen = cdm5FnOutput.get(FIND_MATCHING_INDEX_TRANSITION_INSTRUCTION);
        assertNotNull(gen, "FindMatchingIndexTransitionInstruction not generated");
        assertTrue(gen.contains("fieldWithMetaString -> fieldWithMetaString.getValue()"),
                "the multi-chain meta coercion must be the bare-unguarded plain form");
        assertFalse(gen.contains("fieldWithMetaString0"),
                "the numbered null-guarded single-kind coercion must be gone");
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
                + path + " (PR #344 the three-facet compose).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
