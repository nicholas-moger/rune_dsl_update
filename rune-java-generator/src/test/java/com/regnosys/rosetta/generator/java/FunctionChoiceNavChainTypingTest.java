package com.regnosys.rosetta.generator.java;

import com.regnosys.rosetta.generator.GenerationException;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Facet {@code choice_nav_chain_typing} — three navigation-chain typing recoveries
 * that the cdm/6.20.6 {@code Qualify_*} family exposes (the "qualify trio"):
 *
 * <ol>
 *   <li><b>Choice-narrowed cardinality selection.</b> A feature step DOWNSTREAM of a
 *       choice-option step ({@code economicTerms -> payout -> SettlementPayout ->
 *       settlementTerms -> cashSettlementTerms}) lost its multi-cardinality: the
 *       {@code <CashSettlementTerms>} witness (gm-aware since the
 *       {@code choice_option_nav_witness} facet) rendered correctly but the map-method
 *       chose {@code map} where the golden has {@code mapC}, because
 *       {@code NavigationHandler.resolveMapMethod} / {@code navLeafFeatureMulti} still
 *       used the STATIC (choice-blind) receiver-chain fallback — falsifying that facet's
 *       "cardinality selection needs no choice narrowing" assumption. Both now route
 *       through the gm-aware {@code fallbackResolveFeature}, so the witness and the
 *       cardinality derive from one resolved {@link com.regnosys.rosetta.ast.supporting.RAttribute}.</li>
 *   <li><b>Chain-aware coercion wrapper kind.</b> A meta-attribute step on a chain that
 *       already passed through {@code mapC} ({@code businessEvent -> instruction ->
 *       before}) renders a {@code MapperC} at runtime, so upstream's Type-coercion
 *       meta-deref is the BARE {@code w -> w.getValue()} ({@code MapperC} items are
 *       non-null); the fork tracked the wrapper kind from the CURRENT step's cardinality
 *       only ({@code map} → {@code MapperS}) and emitted the spurious null-guarded
 *       {@code w -> w == null ? null : w.getValue()}. {@code metaNavResultType}'s multi
 *       flag is now chain-aware ({@code mapC} at this step OR an uncollapsed {@code mapC}
 *       anywhere upstream of it).</li>
 *   <li><b>Meta-annotated choice option.</b> A choice option carrying a
 *       {@code [metadata reference|address]} annotation ({@code choice Underlier:
 *       Observable [metadata address ...]}) navigates through the POJO's
 *       {@code ReferenceWithMetaObservable} wrapper, so the golden renders the wrapper
 *       witness + the Type-coercion deref pair (and imports the wrapper); the fork's
 *       {@code RChoiceTypeRef.asRDataType()} projection dropped the option's annotation
 *       refs, so {@code detectMetaKind} saw NONE and the step rendered the direct —
 *       non-compiling — {@code <Observable>map("getObservable", ...)} form. The
 *       projection now copies each option's annotation name + qualifier onto the
 *       projected attribute.</li>
 * </ol>
 *
 * <p>This test is REVERT-VERIFIED RED: reverting any one mechanism reverts its anchors
 * to the divergent shape. Anchors are WHOLE-FILE byte comparisons against the frozen
 * goldens (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}, chosen from the 16
 * sole-mechanism waivered files (census over f-probe-160):
 * <ul>
 *   <li>{@code Qualify_ForeignExchange_NDF} — mechanism 1: {@code mapC} recovered for
 *       {@code cashSettlementTerms} downstream of the {@code -> SettlementPayout}
 *       choice-option step;</li>
 *   <li>{@code Qualify_CorporateActionDetermined} — mechanism 2: bare {@code getValue()}
 *       on the {@code instruction (mapC) -> before [metadata reference]} chain;</li>
 *   <li>{@code Qualify_EquityForward_PriceReturnBasicPerformance_Basket} — mechanism 3:
 *       {@code <ReferenceWithMetaObservable>} witness + Type-coercion pair + the
 *       golden-only wrapper import;</li>
 *   <li>{@code Qualify_EquitySwap_TotalReturnBasicPerformance_Index} — mechanism 3 on
 *       the swap family (different base-product qualifier + Index option shape).</li>
 * </ul>
 */
class FunctionChoiceNavChainTypingTest {

    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static Map<String, String> cdm6FunctionOutput;

    static boolean cdm6CellAvailable() {
        return Files.isDirectory(CDM6_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM6_GOLDEN_DIR);
    }

    @BeforeAll
    static void generateFunctions() throws IOException {
        if (cdm6CellAvailable()) {
            var cell = new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CDM6_CELL_ROOT);
            var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
            var gm = new GeneratorModel(corpus.workspace(),
                    D11CorpusRegressionTest.emissionFilter(cell));
            var typeUtil = new JavaTypeUtil();
            var gen = new FunctionGenerator(gm, new JavaTypeTranslator(typeUtil), typeUtil);
            Map<String, String> output = new LinkedHashMap<>();
            List<GenerationException> genErrors = gen.generateWithErrors(output);
            assertTrue(genErrors.isEmpty(),
                    cell + " FUNCTION generation reported errors: " + genErrors);
            cdm6FunctionOutput = output;
        }
    }

    /** Mechanism 1: mapC recovered downstream of a choice-option step. */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void qualifyForeignExchangeNdf_mapCAfterChoiceOptionStep_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "cdm/product/qualification/functions/Qualify_ForeignExchange_NDF.java");
    }

    /** Mechanism 2: bare meta-deref (no null-guard) on a chain that passed through mapC. */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void qualifyCorporateActionDetermined_bareGetValueOnMapperCChain_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "cdm/event/qualification/functions/Qualify_CorporateActionDetermined.java");
    }

    /** Mechanism 3: meta choice option — wrapper witness + coercion pair + wrapper import. */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void qualifyEquityForwardBasket_metaChoiceOptionWrapper_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "cdm/product/qualification/functions/Qualify_EquityForward_PriceReturnBasicPerformance_Basket.java");
    }

    /** Mechanism 3 on the swap family (different base-product qualifier + Index shape). */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void qualifyEquitySwapIndex_metaChoiceOptionWrapper_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "cdm/product/qualification/functions/Qualify_EquitySwap_TotalReturnBasicPerformance_Index.java");
    }

    private static void assertByteMatchesGolden(String path) throws IOException {
        assertNotNull(cdm6FunctionOutput, "Function generation did not run — corpus unavailable?");
        String generated = cdm6FunctionOutput.get(path);
        assertNotNull(generated, "Function not generated: " + path);
        Path goldenPath = CDM6_GOLDEN_DIR.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated output must byte-match the golden (newline-normalized) for "
                + path + " — a choice-blind cardinality selection drops mapC, a "
                + "step-local wrapper kind adds a spurious null-guard to the meta-deref, "
                + "and an annotation-less choice projection loses the ReferenceWithMetaX "
                + "witness/coercion/import if the respective mechanism is reverted.");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
