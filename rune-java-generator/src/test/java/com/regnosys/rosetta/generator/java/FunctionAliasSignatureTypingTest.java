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
 * Facet {@code alias_method_signature_typing} — five RENDERER-ONLY recoveries in
 * {@link com.regnosys.rosetta.generator.java.function.FunctionAliasHelper}'s
 * signature walk, sharing one upstream law: the protected alias method's return
 * type derives from the ALIAS EXPRESSION'S OWN typed result (upstream 9.83.0
 * {@code AliasUtil.getReturnType}: {@code MapperC} iff
 * {@code CardinalityProvider.isMulti(alias.getExpression())}, witness =
 * {@code typeProvider.getRMetaAnnotatedType(expression)}, {@code ? extends} iff
 * the witness is a generated POJO interface per {@code wrapExtendsIfNotFinal}'s
 * {@code instanceof JavaPojoInterface} test) — never from the FUNCTION's output
 * type. The fork's walker dropped to that output-type fallback (raw rosetta name
 * + unconditional {@code ? extends}, emitting even non-compiling
 * {@code MapperS<? extends boolean>}) or mis-derived cardinality/witness:
 *
 * <ol>
 *   <li><b>Function-call receiver cardinality.</b>
 *       {@code FilterOpenTradeStates(businessEvent -> after) -> trade} typed
 *       correctly ({@code Trade}) but lost the callee output's {@code (0..*)} —
 *       {@code isReceiverMultiInner}'s symbol arm handled {@code RAttribute}
 *       only, so the signature stayed {@code MapperS} where golden carries
 *       {@code MapperC<? extends Trade>} (upstream
 *       {@code CardinalityProvider}: a symbol call to a Function carries the
 *       callee output's cardinality; an only-element receiver is a hard single
 *       barrier — the explicit {@code RListOpExpr} barrier lands in the same
 *       change so {@code instruction (0..*)} cannot leak multi through
 *       {@code only-element}).</li>
 *   <li><b>Type walk through only-element.</b>
 *       {@code businessEvent -> instruction only-element -> before -> … ->
 *       economicTerms} nulled the whole walk ({@code RListOpExpr} had no arm in
 *       {@code inferExpressionType}/{@code inferReceiverRType}) and fell back to
 *       the output type — the non-compiling {@code MapperS<? extends boolean>}
 *       where golden carries {@code MapperS<? extends EconomicTerms>}.
 *       ONLY_ELEMENT/FIRST/LAST recurse into the operand for the item type and
 *       force single; other list ops decline to the legacy fallback.</li>
 *   <li><b>Choice-option navigation.</b>
 *       {@code interestRatePayout -> rateSpecification -> FloatingRateSpecification
 *       -> rounding} died at the choice step ({@code findAttribute}'s
 *       {@code RChoiceTypeRef} arm searched attributes ON each option's type,
 *       never the options THEMSELVES) → output-type fallback
 *       {@code MapperS<? extends FloatingRateProcessingParameters>} ×3 aliases
 *       where golden carries {@code MapperS<? extends Rounding>} /
 *       {@code MapperS<NegativeInterestRateTreatmentEnum>} /
 *       {@code MapperS<RateTreatmentEnum>}. The arm now tries the
 *       {@code RChoiceTypeRef.asRDataType()} projection FIRST (options become
 *       attributes named after their type — the PR #160 body machinery's
 *       narrowing), keeping the option loop as secondary. The cdm/5.38.0
 *       sibling is GREEN ({@code RateSpecification} became a choice only in
 *       cdm 6) — a free version-pair regression pin.</li>
 *   <li><b>Meta witness.</b> {@code NaturalPersonBuyerOrSeller(party) ->
 *       personId -> identifier} (leaf {@code string (1..1) [metadata scheme]})
 *       translated the BARE type → {@code MapperC<String>} where golden carries
 *       {@code MapperC<? extends FieldWithMetaString>} — the meta annotation is
 *       now consulted via {@code MetaFieldGenerator.detectMetaKind} +
 *       {@code RJavaWithMetaValue.create} (the {@code NavigationHandler.metaNavResultType}
 *       pattern; NOT {@code toMetaJavaType}, which emits the generic runtime
 *       {@code FieldWithMeta<T>} form), witness wildcarded as a generated POJO
 *       interface and recorded as a ref.</li>
 *   <li><b>To-enum conversion.</b> {@code periodExtended to-enum PeriodEnum}
 *       had no {@code RConversionExpr} arm → the non-compiling output-type
 *       fallback {@code MapperS<? extends int>} where golden carries the
 *       exact-wrapped {@code MapperS<PeriodEnum>} (bare enums NEVER take
 *       {@code ? extends}). Other conversion kinds decline to the legacy
 *       fallback.</li>
 * </ol>
 *
 * <p>Corpus law (frozen 9.83.0 baseline, all 5 cells): of 1,819 golden
 * {@code protected abstract Mapper} lines, {@code ? extends} appears iff the
 * witness is a generated POJO interface (1,119 wildcard — all model types or
 * Field/ReferenceWithMeta wrappers, meta wrappers 156/0; 700 non-wildcard — all
 * builtins or bare enums); the fork fallback shapes
 * {@code Mapper*<? extends boolean|int>} appear in ZERO goldens. The legacy
 * fallback branch itself stays byte-frozen as the decline path.
 *
 * <p>Anchors are WHOLE-FILE byte comparisons against the frozen goldens
 * (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}, one sole-mechanism
 * waivered file per mechanism:
 * <ul>
 *   <li>cdm/5.38.0 {@code Qualify_Reprice} — mechanism 1 ALONE (the cdm/6.20.6
 *       sibling is byte-identical in shape);</li>
 *   <li>cdm/5.38.0 {@code Qualify_Roll} — mechanism 2 ALONE (its other alias
 *       {@code openEconomicTerms}, a fn-call-receiver only-element chain, is
 *       ALREADY correct {@code MapperS} — pinning that the new multi arms do
 *       not leak through the barrier);</li>
 *   <li>cdm/6.20.6 {@code GetFloatingRateProcessingParameters} — mechanism 3
 *       with all three witness shapes (wildcarded model type + two exact-wrapped
 *       bare enums) in one file;</li>
 *   <li>drr/6.34.1 {@code PartyPersonIdExists} — mechanism 4 ALONE;</li>
 *   <li>drr/6.34.1 {@code PeriodExtendedCalculation} — mechanism 5 ALONE.</li>
 * </ul>
 */
class FunctionAliasSignatureTypingTest {

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

    /** Mechanism 1: multi-output fn-call receiver makes the alias MapperC. */
    @Test
    @EnabledIf("cdm5CellAvailable")
    void qualifyReprice_fnCallReceiverCardinality_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5FunctionOutput, CDM5_GOLDEN_DIR,
                "cdm/event/common/functions/Qualify_Reprice.java");
    }

    /** Mechanism 2: only-element chain types from the walked item, not the output fallback. */
    @Test
    @EnabledIf("cdm5CellAvailable")
    void qualifyRoll_onlyElementChainTyping_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5FunctionOutput, CDM5_GOLDEN_DIR,
                "cdm/event/common/functions/Qualify_Roll.java");
    }

    /** Mechanism 3: choice-option step resolves via the asRDataType projection. */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void getFloatingRateProcessingParameters_choiceOptionNav_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/product/asset/floatingrate/functions/GetFloatingRateProcessingParameters.java");
    }

    /** Mechanism 4: [metadata scheme] leaf surfaces the concrete FieldWithMeta witness. */
    @Test
    @EnabledIf("drrCellAvailable")
    void partyPersonIdExists_metaWitness_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/regulation/cftc/rewrite/functions/PartyPersonIdExists.java");
    }

    /** Mechanism 5: to-enum alias takes the exact-wrapped bare enum witness. */
    @Test
    @EnabledIf("drrCellAvailable")
    void periodExtendedCalculation_toEnumWitness_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/regulation/common/functions/PeriodExtendedCalculation.java");
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
                + path + " — the protected alias method signature derives from the "
                + "function's OUTPUT type (raw rosetta name + unconditional '? extends') "
                + "or mis-derives cardinality/witness (fn-call receiver multi dropped, "
                + "only-element/choice-option/to-enum walks nulled, meta annotation "
                + "ignored) if the alias_method_signature_typing recovery is reverted.");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
