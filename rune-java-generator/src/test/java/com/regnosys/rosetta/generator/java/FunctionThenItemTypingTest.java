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
 * Facet {@code then_maxmin_item_typing} (the then-body ITEM-TYPING facet,
 * deferred at PR #172) — upstream types the implicit item inside {@code then}
 * bodies and {@code max}/{@code min} comparator-key lambdas from the owning
 * operation's ARGUMENT, exactly like filter/extract lambdas. The fork's
 * {@code NavigationHandler.implicitItemArgument} walk (PR #161) recognized only
 * filter/extract owners, so every consumer starved inside then/max/min
 * interiors. Three arms:
 *
 * <ol>
 *   <li><b>S1+S3 — owner-walk widening</b> (S1 = the then-body owner arm, S3 =
 *       the max/min comparator-lambda owner arm + the element-type-transparent
 *       receiver arms). {@code implicitItemArgument} gains
 *       {@code RMaxExpr}/{@code RMinExpr}/{@code RThenExpr} owner arms and
 *       {@code resolveReceiverDataType} gains element-type-transparent receiver
 *       arms (max/min/filter collapse-transparent, then = its body's value,
 *       first/last like only-element). Cascades — through the EXISTING
 *       consumers, no per-consumer changes — the bare-feature re-root
 *       ({@code then resetDate} previously rendered the unresolved,
 *       non-compiling {@code MapperS.of(resetDate)}), the {@code <T>map}
 *       witnesses, and the item-type-derived lambda var names
 *       ({@code reset -> reset.getResetDate()} instead of the
 *       {@code _resetDate} feature fallback) — drr
 *       {@code GetLastFloatingReferenceValue}, cdm6 {@code FXFarLeg}.</li>
 *   <li><b>S2 — rebound-receiver cardinality.</b> A then-body's implicit item
 *       is the WHOLE piped list (upstream {@code CardinalityProvider}:
 *       then-item cardinality = the then-argument's), not a per-element item;
 *       the parser-side {@code CardinalityComputer} reads SINGLE for it. The
 *       gm-aware {@code chainProvesMulti} MONOTONE overlay gains the
 *       then-owner implicit-item arm (+ filter/extract/then transparency and
 *       the synthesized bare-feature recursion), consumed by the hoisted
 *       {@code thenArgN} declaration ({@code MapperC} vs {@code MapperS}), the
 *       filter method ({@code filterItemNullSafe} vs {@code filterSingleNullSafe})
 *       and the map method ({@code mapItem} vs {@code mapSingleToItem}) — drr
 *       {@code GetLastFloatingReferenceResetDate}, drr+iosco
 *       {@code GetMarginValue}.</li>
 *   <li><b>S4 — disguised-receiver lambda-var disambiguation.</b> A navigation
 *       step whose receiver is a disguised {@link
 *       com.regnosys.rosetta.ast.expressions.references.REnumValueRef} chain
 *       derives its type-named lambda var WITHOUT the scope disambiguation
 *       every other type-derived branch applies (upstream
 *       {@code createUniqueIdentifier} ALWAYS escapes): golden
 *       {@code _product} where the var collides with the in-scope input
 *       {@code product}, gen emitted the non-compiling shadow {@code product}
 *       — drr {@code FXFarLeg}/{@code FXNearLeg}.</li>
 * </ol>
 *
 * <p>Deliberately OUT of scope at this facet (mechanism-distinct), each since
 * recovered by its own facet: the max/min lambda BODY expected-type coercions
 * (terminal meta-unwrap, list-literal receiver join, record-feature leaf — drr
 * {@code FXLeg1/2}, {@code InterestRateLeg*CrossCurrency}, {@code GetValuation})
 * by facet maxmin_body_coercion ({@link FunctionMaxMinBodyCoercionTest}); the
 * {@code .first()}-receiver lambda naming + its continuation-indent sibling
 * (drr {@code Extract_UnderlyingAssetTradingPlatformIdentifier}) by facets
 * first_receiver_lambda_naming ({@link FunctionFirstReceiverNamingTest}) +
 * condition_continuation_indent
 * ({@link FunctionConditionContinuationIndentTest}).
 *
 * <p>Green-safety: every pre-facet decline rendered either a NON-COMPILING form
 * (the unresolved {@code MapperS.of(<feature>)} bare re-root, the shadowing
 * lambda var) or a witness-starved form at a site whose RESOLUTION previously
 * failed — the mechanism-level claim: a green file's navigation steps off
 * then/max/min receivers already resolved (path unchanged by construction) or
 * the file was waivered; the full 5-cell D11 matrix (20/20, rule emission
 * included) is the empirical arbiter. A misjudged carrier stays waivered
 * rather than regressing.
 *
 * <p>Anchors are WHOLE-FILE byte comparisons against the frozen goldens
 * (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}.
 */
class FunctionThenItemTypingTest {

    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static Map<String, String> cdm6FunctionOutput;
    private static Map<String, String> drrFunctionOutput;

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

    /**
     * S1+S2: a bare feature as the ENTIRE then-body ({@code then resetDate})
     * re-roots off the rebound {@code thenArg0} with the {@code <Date>} witness
     * and the item-type-derived var ({@code reset}), and the hoisted local
     * declares {@code MapperC<Date>} (the then-item is the whole MULTI list) (drr).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void getLastFloatingReferenceResetDateDrr_bareFeatureThenBodyRerootMapperC_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/regulation/common/functions/GetLastFloatingReferenceResetDate.java");
    }

    /**
     * S3: inside a {@code then max [item -> resetDate]} comparator lambda the
     * navigation recovers its {@code <Date>} witness, and a chain directly off
     * the max-rebound {@code thenArg1} names its lambda vars from the item type
     * ({@code reset}), not the {@code _resetValue} feature fallback (drr).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void getLastFloatingReferenceValueDrr_maxLambdaWitnessAndReboundNaming_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/regulation/common/functions/GetLastFloatingReferenceValue.java");
    }

    /**
     * S1+S2: bare features inside a then-FILTER predicate and a then-EXTRACT
     * body re-root off the rebound item, and the rebound MULTI cardinality
     * selects {@code filterItemNullSafe}/{@code mapItem} + the
     * {@code MapperC} declarations for both REBOUND-receiver hoisted locals
     * ({@code thenArg1}/{@code thenArg2}; {@code thenArg0} roots on the input
     * list, provably multi pre-facet) (drr).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void getMarginValueDrr_thenFilterExtractCardinalityAndReroot_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/regulation/common/functions/GetMarginValue.java");
    }

    /**
     * S1+S2: the verbatim iosco copy of {@code GetMarginValue} pins the same
     * shape through the second drr namespace (drr standards/iosco).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void getMarginValueIoscoDrr_thenFilterExtractCardinalityAndReroot_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/standards/iosco/cde/base/collateral/functions/GetMarginValue.java");
    }

    /**
     * S3: a pure max-lambda carrier — both {@code .max(item -> item.<…>map(…))}
     * keys recover witnesses + type-derived vars with zero other divergence (cdm6).
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void fxFarLegCdm6_maxLambdaItemTyping_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/margin/schedule/functions/FXFarLeg.java");
    }

    /**
     * S4: a step chained after a disguised {@code forwardPayout -> underlier}
     * receiver scope-escapes its type-derived lambda var against the in-scope
     * input {@code product} (golden {@code _product}, previously the
     * non-compiling shadow {@code product}) (drr).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void fxFarLegDrr_disguisedReceiverVarDisambiguation_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/regulation/common/functions/FXFarLeg.java");
    }

    /** S4: the {@code .min}-side sibling pins the same escape (drr). */
    @Test
    @EnabledIf("drrCellAvailable")
    void fxNearLegDrr_disguisedReceiverVarDisambiguation_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/regulation/common/functions/FXNearLeg.java");
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
                + path + " — the then/max/min implicit-item owner walk (S1/S3), the "
                + "rebound-receiver cardinality overlay (S2), or the disguised-receiver "
                + "lambda-var disambiguation (S4) is missing or regressed, if the "
                + "then_maxmin_item_typing recovery is reverted.");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n");
    }
}
