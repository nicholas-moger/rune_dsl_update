package com.regnosys.rosetta.generator.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
 * PR #352 — the three micro-residual facets + the nested-then lockstep admit: 4 byte
 * flips (cdm5 1 + cdm6 3) + 3 TOWARD movers / 0 AWAY.
 *
 * <p><b>F-e1 — {@code comparisonAliasSiblingType}</b> (ComparisonHandler): an ALIAS
 * sibling of an int-literal comparison operand whose body the
 * {@code numericOperandKind} AST walk cannot type (a then-chain body) classifies
 * through the SAME walk that renders the alias signature
 * ({@code tryAliasReceiverMapperType} → {@code inferShortcutMapperJavaType}, the #351
 * render-truth RThenExpr arm); a meta item derefs to its VALUE type per the #349 join
 * law. Qualify_StockSplit's {@code afterPrice > 0} literal now wraps
 * {@code BigDecimal.valueOf(0)} exactly like its beforeNoOfUnits sibling (both cdm
 * cells flip).
 *
 * <p><b>F-e2 — {@code defaultAliasArgDeref}</b> (SetOperationHandler): an ALIAS-call
 * {@code default} RIGHT operand reduces to the bare item via {@code .get()} when the
 * same-walk signature proves MapperS with a non-meta item (the #234 law: golden never
 * keeps a Mapper as a getOrDefault argument). MapBuyerSellerToAccountPartyReference
 * cdm6 flips ({@code getOrDefault(sellerPartyReference(…).get())}).
 *
 * <p><b>F-e3 — {@code sumTypedThenArgBinding}</b> (CollectionHandler): a sum that is
 * the ROOT of a then-STEP body is exempt from the #299 in-lambda decline — the
 * enclosing RInlineFunction is the then step's AST wrapper, not a render lambda (the
 * #350 restructure applies the last step at statement level) — with the item recovered
 * from the compiled argument's MapperC binding type (render truth); the exemption
 * route fires ONLY through the binding read so unhoisted broken-{@code .then(}
 * carriers keep their bytes. UndisputedAdjustedPostedCreditSupportAmount cdm6 flips
 * ({@code return thenArg\n\t.sumBigDecimal();}).
 *
 * <p><b>i1/i1b — {@code inLambdaNestedThenRestructure}</b> (CollectionHandler): the
 * lockstep predicate ({@code thenChainHasUnhandledControlFlow}) no longer counts a
 * then-body whose ROOT is a nested then-chain that is ITSELF fully admissible —
 * including the {@code then extract [ <chain> ]} form — so the inner chain hoists
 * through the #350-F5 lambda channel INSIDE the step lambda (golden's
 * together-restructure, the #350-F5b revert lesson). A bare-item ONLY_ELEMENT collapse
 * level's decl element anchors to the receiver's item via prevRef (the flatten arm's
 * type-system law one op over — {@code MapperC<X>.get()} yields X, so the meta-blind
 * snapshot's VALUE type yields to the wrapper). GetIsin drr moved 18→9 TOWARD here
 * with the in-lambda {@code thenArg0}/{@code thenArg1} ladder structurally golden; its
 * two residual seats (the filter-item meta-deref over a meta-element list-literal
 * receiver + the then-SET output deref-hoist) FLIPPED at PR #353 (F-b — the whole-file
 * byte lock lives in {@code SwitchBaseIfReturnComposeTest}).
 *
 * <p>Whole-file byte comparisons run through the REAL D11 generation paths and revert
 * RED without the facets. The GetIsin content lock's negative witness ({@code .then(})
 * is load-bearing: count 2 in the PRE gen (a two-step chain on one line), count 0
 * in the new gen AND in golden.
 */
class MicroResidualNestedThenComposeTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM5_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-5.38.0");
    private static final Path CDM5_GOLDEN_DIR =
            CDM5_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    // F-e1 flip carrier (both cdm cells).
    private static final String QUALIFY_STOCK_SPLIT =
            "cdm/event/qualification/functions/Qualify_StockSplit.java";
    // F-e2 flip carrier (cdm6).
    private static final String MAP_BUYER_SELLER_TO_ACCOUNT_PARTY_REFERENCE =
            "cdm/ingest/fpml/confirmation/party/functions/MapBuyerSellerToAccountPartyReference.java";
    // F-e3 flip carrier (cdm6).
    private static final String UNDISPUTED_ADJUSTED_POSTED_CREDIT_SUPPORT_AMOUNT =
            "cdm/legaldocumentation/csa/functions/UndisputedAdjustedPostedCreditSupportAmount.java";
    // i1/i1b TOWARD carrier (drr FUNCTION — the in-lambda ladder content lock).
    private static final String GET_ISIN =
            "drr/regulation/common/functions/GetIsin.java";

    private static Map<String, String> cdm6FnOutput;
    private static Map<String, String> cdm5FnOutput;
    private static Map<String, String> drrFnOutput;

    static boolean drrCellAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"));
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

    // ------------------------------------------------------------- F-e1 byte locks

    @Test
    @EnabledIf("cdm6CellAvailable")
    void qualifyStockSplit_cdm6_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, QUALIFY_STOCK_SPLIT);
    }

    @Test
    @EnabledIf("cdm5CellAvailable")
    void qualifyStockSplit_cdm5_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5FnOutput, CDM5_GOLDEN_DIR, QUALIFY_STOCK_SPLIT);
    }

    // ------------------------------------------------------------- F-e2 byte lock

    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapBuyerSellerToAccountPartyReference_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR,
                MAP_BUYER_SELLER_TO_ACCOUNT_PARTY_REFERENCE);
    }

    // ------------------------------------------------------------- F-e3 byte lock

    @Test
    @EnabledIf("cdm6CellAvailable")
    void undisputedAdjustedPostedCreditSupportAmount_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR,
                UNDISPUTED_ADJUSTED_POSTED_CREDIT_SUPPORT_AMOUNT);
    }

    // ------------------------------------------- i1/i1b TOWARD content lock (GetIsin)

    /**
     * The in-lambda nested-then ladder renders structurally golden: thenArg0 (the
     * filtered meta-element list-literal, declared with the WRAPPER element) +
     * thenArg1 (the bare-item only-element collapse, {@code MapperS.of(thenArg0.get())}
     * with the i1b render-truth wrapper element). The negative witness: the runtime
     * {@code .then(} link is GONE (count 2 in the PRE gen — the two-step
     * `.then(_item -> _item.get()).then(_item -> …)` chain; count 0 in golden — the
     * decline-lock witness-uniqueness law).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void getIsin_rendersInLambdaNestedThenLadder() {
        String gen = gen(drrFnOutput, GET_ISIN);
        assertTrue(gen.contains(
                "final MapperC<ReferenceWithMetaProductIdentifier> thenArg0 = MapperC."
                        + "<ReferenceWithMetaProductIdentifier>of("),
                "The inner chain's base hoists in-lambda with the wrapper element");
        assertTrue(gen.contains(
                "final MapperS<ReferenceWithMetaProductIdentifier> thenArg1 = "
                        + "MapperS.of(thenArg0.get());"),
                "The only-element collapse level hoists with the i1b render-truth element");
        assertEquals(0, count(gen, ".then("),
                "The runtime .then( link is gone (count 0 in golden)");
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
