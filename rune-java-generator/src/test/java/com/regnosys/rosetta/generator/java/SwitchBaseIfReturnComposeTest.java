package com.regnosys.rosetta.generator.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.java.enums.EnumGenerator;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.object.ChoiceObjectGenerator;
import com.regnosys.rosetta.generator.java.object.ModelObjectGenerator;
import com.regnosys.rosetta.generator.java.reports.ReportGenerator;
import com.regnosys.rosetta.generator.java.reports.RuleGenerator;
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
 * PR #353 — the switch-BASE desugar + the GetIsin close + the multiline if/return
 * arms: 7 byte flips (1 drr FUNCTION + 4 cdm6 FUNCTION + 2 drr POJO Rules) + 18
 * TOWARD movers / 0 AWAY.
 *
 * <p><b>F-b — {@code listLiteralElementMeta} + {@code functionThenOutputMetaDeref}</b>
 * (NavigationHandler + FunctionExpressionRenderer): a list-op item over a LIST-LITERAL
 * receiver whose elements share a META-annotated terminal (incl. DISGUISED 2-name
 * elements via the #286 resolver) classifies through a per-element terminal resolution
 * with a canonical-name join, so GetIsin's filter item derefs the
 * ReferenceWithMetaProductIdentifier element before navigating {@code getSource}; and
 * the FUNCTION-path whole-output deref channel recovers the extract-wrapped nested
 * chain's terminal wrapper ({@code final FieldWithMetaString fieldWithMetaString =
 * <chain>.get();} + the null-guarded {@code getValue} block). GetIsin drr flips.
 *
 * <p><b>F-d — {@code switchBaseItemCase} + {@code switchBaseThenHoist}</b>
 * (CollectionHandler + FunctionAliasHelper): the #226 choice-switch block lambda
 * admits the bare implicit-ITEM case body (returning the CAST case var,
 * {@code MapperS.of(_returnLeg)}) with the RESULT type joined from the case GUARD
 * types; {@code thenChainHasUnhandledControlFlow}'s BASE walk exempts an extract
 * whose inline-body ROOT is a statically-renderable choice switch (the render is
 * self-contained, so the {@code (extract switch …) then only-element} alias chains
 * hoist like ctl-free pipes); the thenArg decl element and the alias signature
 * ({@code MapperS<? extends ReturnLeg>}) read the SAME guard-type join (render
 * truth). MapReturnSwapPriceQuantityList + MapReturnSwapLegListToPayoutList ×2 +
 * MapEquitySwapTransactionSupplementPriceQuantityList (all cdm6) flip.
 *
 * <p><b>F-a — {@code inLambdaIfReturnMultiline}</b> (CollectionHandler): the elseless
 * / effective-else / nested-tree in-lambda conditional block renderers admit
 * MULTI-LINE arms (continuations re-anchored at the owning return's depth — the #333
 * ladder law generalized), SUPPRESSION-gated so a conditional inside an OUTER
 * inline-ternary arm (the P352A suppress class) keeps its bytes. asic
 * UnderlyingIdentificationRule + mas UnderlyingRule (drr POJO) flip.
 *
 * <p>Whole-file byte comparisons run through the REAL D11 generation paths and revert
 * RED without the facets. The switch witness is load-bearing per the
 * witness-uniqueness law: the misparsed-guard ternary token
 * {@code Objects.equals(fpml.} counted ≥1 in every PRE switch-carrier gen and 0 in
 * every golden — the flip REMOVES it.
 */
class SwitchBaseIfReturnComposeTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    // F-b flip carrier (drr FUNCTION).
    private static final String GET_ISIN =
            "drr/regulation/common/functions/GetIsin.java";
    // F-d flip carriers (cdm6 FUNCTION).
    private static final String MAP_RETURN_SWAP_PRICE_QUANTITY_LIST =
            "cdm/ingest/fpml/confirmation/product/returnswap/functions/"
                    + "MapReturnSwapPriceQuantityList.java";
    private static final String MAP_RETURN_SWAP_LEG_LIST_RETURNSWAP =
            "cdm/ingest/fpml/confirmation/product/returnswap/functions/"
                    + "MapReturnSwapLegListToPayoutList.java";
    private static final String MAP_RETURN_SWAP_LEG_LIST_EQUITYSWAP =
            "cdm/ingest/fpml/confirmation/product/equityswaptransactionsupplement/functions/"
                    + "MapReturnSwapLegListToPayoutList.java";
    private static final String MAP_EQUITY_SWAP_PRICE_QUANTITY_LIST =
            "cdm/ingest/fpml/confirmation/product/equityswaptransactionsupplement/functions/"
                    + "MapEquitySwapTransactionSupplementPriceQuantityList.java";
    // F-a flip carriers (drr POJO Rules).
    private static final String ASIC_UNDERLYING_IDENTIFICATION_RULE =
            "drr/regulation/asic/rewrite/trade/reports/UnderlyingIdentificationRule.java";
    private static final String MAS_UNDERLYING_RULE =
            "drr/regulation/mas/rewrite/trade/reports/UnderlyingRule.java";

    private static Map<String, String> cdm6FnOutput;
    private static Map<String, String> drrFnOutput;
    private static Map<String, String> drrCellOutput;

    static boolean drrCellAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR);
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
        if (drrCellAvailable()) {
            var drrCell = new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT);
            drrFnOutput = generateFunctions(drrCell);
            drrCellOutput = generateCell(drrCell);
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

    /** The REAL D11 full-cell path (the POJO/Rule kinds ride RuleGenerator/ReportGenerator). */
    private static Map<String, String> generateCell(D11CorpusRegressionTest.CellSpec cell)
            throws IOException {
        var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(),
                D11CorpusRegressionTest.emissionFilter(cell),
                D11CorpusRegressionTest.readDoNotPrune(cell));
        var typeUtil = new JavaTypeUtil();
        var typeTranslator = new JavaTypeTranslator(typeUtil);
        var enumGen = new EnumGenerator(gm);
        var pojoGen = new ModelObjectGenerator(gm, typeTranslator, typeUtil);
        var choiceGen = new ChoiceObjectGenerator(gm, typeTranslator, typeUtil, pojoGen);
        var funcGen = new FunctionGenerator(gm, typeTranslator, typeUtil);
        var ruleGen = new RuleGenerator(gm, typeTranslator, funcGen);
        var reportGen = new ReportGenerator(gm, typeTranslator, funcGen);
        Map<String, String> output = new LinkedHashMap<>();
        for (RModel model : corpus.workspace().files()) {
            if (gm.shouldGenerate(model)) {
                String version = gm.version(model);
                assertNoGenerationErrors(enumGen.generateClasses(model, version, output));
                assertNoGenerationErrors(pojoGen.generateClasses(model, version, output));
                assertNoGenerationErrors(choiceGen.generateClasses(model, version, output));
                assertNoGenerationErrors(ruleGen.generateClasses(model, version, output));
                assertNoGenerationErrors(reportGen.generateClasses(model, version, output));
            }
        }
        return output;
    }

    // ------------------------------------------------------------- F-b byte lock

    @Test
    @EnabledIf("drrCellAvailable")
    void getIsin_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFnOutput, DRR_GOLDEN_DIR, GET_ISIN);
    }

    // ------------------------------------------------------------- F-d byte locks

    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapReturnSwapPriceQuantityList_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR,
                MAP_RETURN_SWAP_PRICE_QUANTITY_LIST);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapReturnSwapLegListToPayoutList_returnswap_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR,
                MAP_RETURN_SWAP_LEG_LIST_RETURNSWAP);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapReturnSwapLegListToPayoutList_equityswap_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR,
                MAP_RETURN_SWAP_LEG_LIST_EQUITYSWAP);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapEquitySwapTransactionSupplementPriceQuantityList_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR,
                MAP_EQUITY_SWAP_PRICE_QUANTITY_LIST);
    }

    /**
     * The load-bearing negative witness (the witness-uniqueness law): the
     * misparsed-NAME-guard ternary token {@code Objects.equals(fpml.} counted
     * 1/2/2/1 OCCURRENCES in the four PRE gens (PriceQuantityList 1, each LegList 2,
     * EquitySwap 1 — occurrence counts, never line counts) and 0 in every golden —
     * the F-d flip REMOVES it, so this assertion is RED on the pre-facet generator.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void switchCarriers_misparsedGuardTernaryGone() {
        for (String path : new String[] {MAP_RETURN_SWAP_PRICE_QUANTITY_LIST,
                MAP_RETURN_SWAP_LEG_LIST_RETURNSWAP, MAP_RETURN_SWAP_LEG_LIST_EQUITYSWAP,
                MAP_EQUITY_SWAP_PRICE_QUANTITY_LIST}) {
            String gen = gen(cdm6FnOutput, path);
            assertEquals(0, count(gen, "Objects.equals(fpml."),
                    "The misparsed-guard ternary is gone (count 0 in golden): " + path);
        }
    }

    // ------------------------------------------------------------- F-a byte locks

    @Test
    @EnabledIf("drrCellAvailable")
    void asicUnderlyingIdentificationRule_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrCellOutput, DRR_GOLDEN_DIR,
                ASIC_UNDERLYING_IDENTIFICATION_RULE);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void masUnderlyingRule_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrCellOutput, DRR_GOLDEN_DIR, MAS_UNDERLYING_RULE);
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
        String gen = gen(output, path);
        Path goldenPath = goldenDir.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath).replace("\r", "");
        assertEquals(golden, gen.replace("\r", ""),
                "Generated bytes must match the golden for " + path);
    }
}
