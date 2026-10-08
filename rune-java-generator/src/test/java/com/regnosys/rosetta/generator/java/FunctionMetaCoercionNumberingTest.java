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
 * Facet {@code meta_coercion_numbering} — upstream coerces every navigation
 * receiver and every assignment value META-FREE at its consumption site, and
 * registers each null-GUARDED coercion lambda param into the generator scope so
 * same-name groups number {@code 0..n-1} (wf169 mechanism (a)).
 *
 * <p>One upstream law, three cooperating pieces
 * ({@code ExpressionGenerator.xtend} L385 receiver coercion + L224 universal
 * expected-type coercion; {@code TypeCoercionService.xtend} L618-640 guarded
 * iff MapperS, bare iff MapperC; {@code convertNullSafe} L356
 * {@code declareAsVariable} scope registration whose
 * {@code computeActualNames} numbers same-scope same-name groups), four fork
 * arms:
 *
 * <ol>
 *   <li><b>A1 — alias-call receiver typing.</b> The alias-call rendering
 *       ({@code ReferenceHandler}) carries a null expression type, so the
 *       EXISTING {@code ExpressionCompiler.coerceNavigationReceiver} gate —
 *       already byte-faithful for in-chain meta steps — declines at the
 *       alias-head. The arm types the receiver from the SAME
 *       {@code FunctionAliasHelper} walk that produced the alias method
 *       signature (facet alias_method_signature_typing), so signature and
 *       coercion cannot disagree; the dormant gate then fires unchanged.
 *       Pre-fix render is NON-COMPILING: the next step's getter is invoked on
 *       the meta wrapper type ({@code ReferenceWithMetaTradeState.getTrade()}
 *       does not exist).</li>
 *   <li><b>A2 — guarded-param registration + deferred numbering.</b>
 *       {@code WrappedItemCoercer}'s guarded MapperS arm registers its lambda
 *       param into the scope threaded at the coercion site (upstream
 *       {@code convertNullSafe} → {@code declareAsVariable} →
 *       {@code createSynonym}) and emits a sentinel resolved to the actual
 *       name — bare when unique, {@code 0..n-1} when ≥2 share one scope,
 *       underscore-escaped when a lambda-scope param hits a parent-taken name
 *       — at statement finalization via the fork's EXISTING
 *       {@code GeneratorScope.computeActualNames}. Bare MapperC params never
 *       register (upstream applies the MapperC conversion bare, no
 *       {@code convertNullSafe}) and repeat unnumbered forever. Lambda bodies
 *       compile in child scopes ({@code JavaStatementScope.lambdaScope}) so
 *       in-lambda params group within their own scope and escape against
 *       parent-registered names.</li>
 *   <li><b>A4 — terminal meta coercion at the assignment unwrap ladder.</b> A
 *       compiled chain whose result item type is a meta wrapper, assigned to a
 *       value-typed target, appends the wrapper-kind coercion step BEFORE the
 *       {@code .get()}/{@code .getMulti()} suffix (upstream's universal L224
 *       coercion at the output-assignment site). Pre-fix render is
 *       NON-COMPILING ({@code List&lt;ReferenceWithMetaParty&gt;} into
 *       {@code toBuilder(List&lt;? extends Party&gt;)}). Meta-typed targets
 *       decline (the wrapper is wanted).</li>
 *   <li><b>Guard-kind sub-arm.</b> The carrier chain
 *       {@code interestRatePayouts -> priceQuantity} parses as a DISGUISED
 *       {@code REnumValueRef}, and {@code NavigationHandler.chainRendersMapperC}'s
 *       disguised arm read only the LEAF feature's own cardinality — ignoring
 *       the plural ROOT input that renders the
 *       {@code MapperC.&lt;InterestRatePayout&gt;of(interestRatePayouts)} wrap —
 *       so the chain mis-classified MapperS and emitted the GUARDED form where
 *       golden has bare. (Trace-pinned: the wf169 a.md hypothesis blamed the
 *       bare-SYMBOL arm's unpopulated {@code RAttribute::cardinality}; the
 *       trace showed that arm is never reached for this file.) The disguised
 *       arm now also reads the ROOT attribute through
 *       {@code ReferenceHandler.mapperCOfWrapWitness} — the SAME gm-aware
 *       predicate {@code tryMultiValueWrap} renders the wrap with — so the
 *       reading and the rendered chain cannot disagree.</li>
 * </ol>
 *
 * <p>Green-safety: A1/A4 firing shapes render non-compiling Java pre-fix, so no
 * byte-matching file carries one; A2 numbering fires only when ≥2 same-base
 * GUARDED params land in one scope — golden ALWAYS numbers such groups and the
 * fork NEVER does (0 of 1,674 waivered gen files contain a digit-suffixed
 * Type-coercion param vs 102 goldens that do), so every firing site is
 * currently divergent; single params keep their exact bytes
 * ({@code ids.size()==1}).
 *
 * <p>Anchors are WHOLE-FILE byte comparisons against the frozen goldens
 * (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}:
 * <ul>
 *   <li>cdm/5.38.0 + cdm/6.20.6 {@code Qualify_Novation} — A1+A2 (three
 *       missing alias-head guarded steps numbered {@code 0/1/2} across one
 *       statement; the two in-chain MapperC {@code &lt;Party&gt;} coercions
 *       stay bare + unnumbered);</li>
 *   <li>cdm/5.38.0 {@code Qualify_Allocation} — A1+A2 underscore-escape (the
 *       second guarded step sits inside a {@code .mapItem(item -&gt; …)}
 *       lambda whose param escapes to {@code _referenceWithMetaTradeState}
 *       against the statement-level registration);</li>
 *   <li>cdm/6.20.6 {@code Qualify_Reset} — A1+A2 ({@code 0/1} pair);</li>
 *   <li>cdm/5.38.0 {@code InterestRatePayoutCurrency} — guard-kind flip
 *       (guarded → bare on the plural-input-rooted chain) + A4 terminal
 *       {@code &lt;String&gt;} coercion on both ADD statements;</li>
 *   <li>drr/6.34.1 {@code Counterparties} — A4 alone (terminal bare MapperC
 *       {@code &lt;Party&gt;} coercion inside {@code toBuilder(...)});</li>
 *   <li>drr/6.34.1 {@code ResetFrequencyPeriodToDays} +
 *       {@code CommodityQuantity} — A2 alone (the guarded coercions already
 *       render; only the {@code 0/1} numbering is missing).</li>
 * </ul>
 */
class FunctionMetaCoercionNumberingTest {

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

    /** A1+A2: alias-head guarded steps numbered 0/1/2 across one statement (cdm5). */
    @Test
    @EnabledIf("cdm5CellAvailable")
    void qualifyNovationCdm5_aliasHeadCoercionNumbered_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5FunctionOutput, CDM5_GOLDEN_DIR,
                "cdm/event/qualification/functions/Qualify_Novation.java");
    }

    /** A1+A2: the identical shape in the cdm6 cell (free version-pair pin). */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void qualifyNovationCdm6_aliasHeadCoercionNumbered_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/event/qualification/functions/Qualify_Novation.java");
    }

    /** A1+A2: the in-lambda second occurrence escapes to {@code _referenceWithMetaTradeState}. */
    @Test
    @EnabledIf("cdm5CellAvailable")
    void qualifyAllocationCdm5_lambdaScopeUnderscoreEscape_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5FunctionOutput, CDM5_GOLDEN_DIR,
                "cdm/event/qualification/functions/Qualify_Allocation.java");
    }

    /** A1+A2: the 0/1 numbered pair (cdm6). */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void qualifyResetCdm6_aliasHeadCoercionPair_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/event/qualification/functions/Qualify_Reset.java");
    }

    /** Guard-kind + A4: bare MapperC coercion on a plural-input-rooted chain + terminal unwrap. */
    @Test
    @EnabledIf("cdm5CellAvailable")
    void interestRatePayoutCurrencyCdm5_guardKindAndTerminalCoercion_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(cdm5FunctionOutput, CDM5_GOLDEN_DIR,
                "cdm/product/common/functions/InterestRatePayoutCurrency.java");
    }

    /** A4 alone: terminal bare MapperC coercion inside {@code toBuilder(...)} (drr). */
    @Test
    @EnabledIf("drrCellAvailable")
    void counterpartiesDrr_terminalBareCoercion_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/regulation/common/functions/Counterparties.java");
    }

    /** A2 alone: existing guarded pair gains 0/1 numbering (drr). */
    @Test
    @EnabledIf("drrCellAvailable")
    void resetFrequencyPeriodToDaysDrr_numberedPair_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/regulation/common/functions/ResetFrequencyPeriodToDays.java");
    }

    /** A2 alone: the second drr numbering carrier. */
    @Test
    @EnabledIf("drrCellAvailable")
    void commodityQuantityDrr_numberedPair_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/regulation/common/functions/CommodityQuantity.java");
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
                + path + " — the alias-head/terminal meta Type-coercion step is missing, "
                + "the guarded coercion param lost its scope registration (numbering / "
                + "underscore escape), or the wrapper-kind classification regressed to "
                + "the guarded MapperS form, if the meta_coercion_numbering recovery is "
                + "reverted.");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
