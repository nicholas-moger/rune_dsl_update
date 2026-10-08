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
 * Facet {@code lambda_item_body_coercion} — RENDERER-ONLY recoveries sharing one
 * upstream law: upstream compiles every expression against its EXPECTED TYPE and
 * with the IMPLICIT ITEM context in scope; the fork compiled several positions
 * with neither, splicing item-typed or unresolved renderings verbatim.
 *
 * <ol>
 *   <li><b>A — alias-body-to-signature coercion.</b> Upstream compiles the alias
 *       body with expected = the declared signature
 *       ({@code FunctionGenerator.xtend:319-321}), so an item-typed body — the
 *       only-element collapse {@code <chain>.get()} /
 *       {@code MapperC.of(<call>).get()} or count's
 *       {@code <chain>.resultCount()} — takes {@code TypeCoercionService}'s
 *       null-safe item→MapperS wrap. The fork spliced the raw item rendering as
 *       {@code return <src>;} under the (already-correct, PR #167)
 *       {@code MapperS<X>} signature — non-compiling. Corpus law: of 1,658
 *       golden alias methods, ZERO MapperS-signature bodies end bare
 *       {@code .get();}; all 332 MapperC-signature bodies are raw chains
 *       (never wrapped) — the wrap fires ONLY on top-level
 *       ONLY_ELEMENT/count under a {@code MapperS<} signature.</li>
 *   <li><b>B1 — functional-op method selection by receiver cardinality.</b>
 *       Upstream picks {@code mapItem} vs {@code mapSingleToItem} from
 *       {@code CardinalityProvider.isMulti(receiver)}, which propagates
 *       receiver-multi through single feature steps
 *       ({@code CardinalityProvider.java:353-358}). The fork's parser-side
 *       {@code CardinalityComputer} consults only the leaf feature's own
 *       cardinality and reads SINGLE when {@code resolvedFeature} is
 *       unpopulated, so a {@code ...only-element -> priceQuantity (1..*)
 *       extract ...} chain rendered {@code mapSingleToItem} + a {@code .get()}
 *       tail where golden carries {@code mapItem} + {@code .getMulti()}. A
 *       gm-aware MONOTONE overlay ({@code chainProvesMulti}) adds multi on top
 *       of the legacy answer for feature-call chains only; {@code RListOpExpr}
 *       stays a hard barrier. Corpus law: ZERO golden {@code .mapSingleToItem(}
 *       directly follows a {@code .mapC(...)} step.</li>
 *   <li><b>B2 — implicit-item navigation synthesis inside lambdas.</b> A
 *       map/filter lambda body navigating the implicit item must compile
 *       item-rooted ({@code item.<T>map(...)}). The fork covered only the
 *       Cat-9-bound bare {@code RSymbolReference→RAttribute} shape (PR #161);
 *       the disguised {@code REnumValueRef} chain ({@code state -> closedState})
 *       fell through to {@code synthesizeFeatureCall}'s unresolved-head
 *       rendering {@code MapperS.of(state).map(...)} (witness-less,
 *       non-compiling), and a choice option referenced by TYPE name
 *       ({@code PerformancePayout} on cdm6 {@code choice Payout}) was bound to
 *       the RData node, failed the {@code instanceof RAttribute} gate, and
 *       emitted the raw type name {@code MapperS.of(PerformancePayout)}. Both
 *       now synthesize the item navigation via the gm-aware
 *       {@code implicitItemDataType} walk, declining for closure params and
 *       function-scope names.</li>
 *   <li><b>C — onlyExists parent-type resolution.</b> The fork's
 *       upstream-shaped onlyExists arm declined to the legacy placeholder
 *       ({@code onlyExists(<root>, Arrays.asList("<root>.<leaf>"), ...)})
 *       whenever {@code HandlerHelper.resolveValueDataType} could not resolve
 *       the receiver — alias roots ({@code RShortcut} filtered out), alias-rooted
 *       chains ({@code resolvedFeature} empty), and implicit-item roots (symbol
 *       empty). The parent type now falls back to the gm-aware
 *       {@code NavigationHandler.resolveReceiverDataType} walk, and a bare
 *       symbol-empty root that names an item attribute synthesizes the
 *       item-rooted receiver. Corpus law: ZERO of 219 golden onlyExists calls
 *       carry a dotted path; lists are always parent-relative simple names.
 *       An onlyExists lambda body also takes {@code .asMapper()}
 *       (ComparisonResult→MapperS coercion) like the exists family.</li>
 *   <li><b>S1 — MapperMaths count-operand wrap.</b> Arithmetic operands compile
 *       against {@code MAPPER.wrapExtends(...)}; count's item-typed
 *       {@code .resultCount()} takes the item→MapperS wrap. Corpus law: 12/12
 *       golden MapperMaths count operands are {@code MapperS.of(...)}-wrapped.
 *       Mirrors {@code ComparisonHandler.wrapCountOperand}, RCountExpr-only.</li>
 *   <li><b>S2 — getOrDefault is item-typed.</b> {@code Mapper.getOrDefault(T)}
 *       returns the VALUE; upstream appends nothing. The fork's constructor
 *       value coercion appended {@code .get()}. Corpus law: ZERO of 12,484
 *       golden {@code .getOrDefault(} sites are followed by {@code .get()}.
 *       Gate: single-cardinality {@code RDefaultExpr} ctor values only.</li>
 * </ol>
 *
 * <p>Anchors are WHOLE-FILE byte comparisons against the frozen goldens
 * (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}, one sole-mechanism
 * waivered file per arm plus two interplay combos:
 * <ul>
 *   <li>cdm/5.38.0 {@code Qualify_EquityOption_ParameterReturnCorrelation_Basket}
 *       — A alone (only-element navigation chain; head of the 10-file cdm5
 *       EquityOption family);</li>
 *   <li>cdm/5.38.0 {@code Qualify_PartialDelivery} — A alone, fn-call variant
 *       ({@code MapperS.of(MapperC.of(evaluate(...)).get())}, two aliases);</li>
 *   <li>cdm/5.38.0 {@code UpdateSpreadAdjustmentAndRateOptions} — B1 alone
 *       (mapItem + the deep-path SET leaf {@code .getMulti()} tail);</li>
 *   <li>cdm/6.20.6 {@code PerformancePayoutAndFixedPricePayoutOnlyExists} — B2
 *       alone, bare-TYPE-name shape (choice options; gen emitted the
 *       non-compiling {@code MapperS.of(PerformancePayout)});</li>
 *   <li>cdm/5.38.0 {@code FilterOpenTradeStates} — B2 alone, disguised-chain
 *       shape in a FILTER predicate ({@code state -> closedState});</li>
 *   <li>drr/6.34.1 {@code IsSingleCommodityPayoutProduct} — C alone (alias-rooted
 *       chain receiver {@code economicTerms(product).<Payout>map(...)});</li>
 *   <li>cdm/5.38.0 {@code Qualify_Execution} — A+C interplay (alias wrap + the
 *       alias-root onlyExists with full sibling-attribute lists);</li>
 *   <li>cdm/5.38.0 {@code Qualify_CashTransfer} — B1+C+S1 interplay (mapItem +
 *       in-lambda onlyExists with {@code .asMapper()} + MapperMaths count
 *       wraps in one statement);</li>
 *   <li>drr/6.34.1 cftc rewrite trade {@code Create_SubmissionHeader} — S2
 *       alone (single {@code getOrDefault} line).</li>
 * </ul>
 */
class FunctionLambdaItemBodyCoercionTest {

    private static final Path CDM5_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-5.38.0");
    private static final Path CDM5_GOLDEN_DIR =
            CDM5_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static Map<String, String> cdm5FunctionOutput;
    private static Map<String, String> cdm6FunctionOutput;
    private static Map<String, String> drrFunctionOutput;

    static boolean cdm5CellAvailable() {
        return Files.isDirectory(CDM5_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM5_GOLDEN_DIR);
    }

    static boolean cdm6CellAvailable() {
        return Files.isDirectory(CDM6_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM6_GOLDEN_DIR);
    }

    static boolean drrCellAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR);
    }

    @BeforeAll
    static void generateFunctions() throws IOException {
        if (cdm5CellAvailable()) {
            cdm5FunctionOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("cdm", "5.38.0", CDM5_CELL_ROOT));
        }
        if (cdm6CellAvailable()) {
            cdm6FunctionOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CDM6_CELL_ROOT));
        }
        if (drrCellAvailable()) {
            drrFunctionOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT));
        }
    }

    private static Map<String, String> generateCell(D11CorpusRegressionTest.CellSpec cell)
            throws IOException {
        var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(),
                D11CorpusRegressionTest.emissionFilter(cell));
        var typeUtil = new JavaTypeUtil();
        var gen = new FunctionGenerator(gm, new JavaTypeTranslator(typeUtil), typeUtil);
        Map<String, String> output = new LinkedHashMap<>();
        List<GenerationException> genErrors = gen.generateWithErrors(output);
        assertTrue(genErrors.isEmpty(),
                cell + " FUNCTION generation reported errors: " + genErrors);
        return output;
    }

    /** Arm A: only-element navigation-chain alias body takes the MapperS.of wrap. */
    @Test
    @EnabledIf("cdm5CellAvailable")
    void basket_aliasBodyOnlyElementWrap_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5FunctionOutput, CDM5_GOLDEN_DIR,
                "cdm/product/qualification/functions/Qualify_EquityOption_ParameterReturnCorrelation_Basket.java");
    }

    /** Arm A: only-element fn-call alias body — the MapperC.of(evaluate(...)) variant. */
    @Test
    @EnabledIf("cdm5CellAvailable")
    void partialDelivery_aliasBodyFnCallWrap_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5FunctionOutput, CDM5_GOLDEN_DIR,
                "cdm/event/common/functions/Qualify_PartialDelivery.java");
    }

    /** Arm B1: multi leaf over only-element renders mapItem + the .getMulti() leaf tail. */
    @Test
    @EnabledIf("cdm5CellAvailable")
    void updateSpread_mapItemReceiverMulti_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5FunctionOutput, CDM5_GOLDEN_DIR,
                "cdm/event/common/functions/UpdateSpreadAdjustmentAndRateOptions.java");
    }

    /** Arm B2: choice option by TYPE name synthesizes the item navigation. */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void performancePayout_choiceOptionBareName_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/product/qualification/functions/PerformancePayoutAndFixedPricePayoutOnlyExists.java");
    }

    /** Arm B2: disguised REnumValueRef chain in a filter predicate synthesizes item nav. */
    @Test
    @EnabledIf("cdm5CellAvailable")
    void filterOpenTradeStates_disguisedChainItemSynthesis_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(cdm5FunctionOutput, CDM5_GOLDEN_DIR,
                "cdm/event/common/functions/FilterOpenTradeStates.java");
    }

    /** Arm C: alias-rooted onlyExists resolves the parent type + simple-name lists. */
    @Test
    @EnabledIf("drrCellAvailable")
    void isSingleCommodityPayoutProduct_onlyExistsAliasRoot_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/regulation/common/functions/IsSingleCommodityPayoutProduct.java");
    }

    /** Arms A+C interplay: alias wrap + alias-root onlyExists in one file. */
    @Test
    @EnabledIf("cdm5CellAvailable")
    void qualifyExecution_aliasWrapPlusOnlyExists_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5FunctionOutput, CDM5_GOLDEN_DIR,
                "cdm/event/qualification/functions/Qualify_Execution.java");
    }

    /** Arms B1+C+S1 interplay: mapItem + in-lambda onlyExists(.asMapper()) + count wraps. */
    @Test
    @EnabledIf("cdm5CellAvailable")
    void qualifyCashTransfer_lambdaOnlyExistsCountWrap_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(cdm5FunctionOutput, CDM5_GOLDEN_DIR,
                "cdm/event/qualification/functions/Qualify_CashTransfer.java");
    }

    /** Arm S2: a single-cardinality default ctor value keeps the item-typed getOrDefault. */
    @Test
    @EnabledIf("drrCellAvailable")
    void createSubmissionHeader_getOrDefaultNoGet_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/projection/dtcc/rds/harmonized/cftc/rewrite/trade/functions/Create_SubmissionHeader.java");
    }

    private static void assertByteMatchesGolden(Map<String, String> functionOutput,
            Path goldenDir, String path) throws IOException {
        assertNotNull(functionOutput, "Function generation did not run — corpus unavailable?");
        String generated = functionOutput.get(path);
        assertNotNull(generated, "Function not generated: " + path);
        Path goldenPath = goldenDir.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated output must byte-match the golden (newline-normalized) for "
                + path + " — the alias body splices the raw item-typed rendering "
                + "(bare .get()/.resultCount() under a MapperS signature), the map/filter "
                + "method choice loses receiver-multi, in-lambda implicit-item navigations "
                + "re-root at bare/unresolved symbols, onlyExists declines to the dotted "
                + "legacy placeholder, or the count/getOrDefault expected-type coercions "
                + "are missing if the lambda_item_body_coercion recovery is reverted.");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
