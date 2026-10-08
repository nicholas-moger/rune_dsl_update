package com.regnosys.rosetta.generator.java;

import com.regnosys.rosetta.utils.DeepFeatureCallUtil;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.function.LabelProviderGenerator;
import com.regnosys.rosetta.generator.java.function.LabelProviderGeneratorUtil;
import com.regnosys.rosetta.generator.java.object.ChoiceObjectGenerator;
import com.regnosys.rosetta.generator.java.object.ModelObjectGenerator;
import com.regnosys.rosetta.generator.java.reports.ReportGenerator;
import com.regnosys.rosetta.generator.java.reports.RuleGenerator;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.ast.model.RModel;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static com.regnosys.rosetta.testutil.GenerationErrors.assertNoGenerationErrors;

/**
 * PR #381 — the in-lambda decomposition/output-builder trio (3 byte flips:
 * UnderlyingAssetTradingPlatformIdentifierLeg1Rule + Leg2Rule iosco cde v3 [drr POJO
 * Rules] + NewEquitySwapProduct [cdm5 FUNCTION]).
 *
 * <p><b>D4b-1 inLambdaCondBaseArmChainDecomp</b> (CollectionHandler +
 * ControlFlowHandler + StatementHoistSession): the conditional-ROOT base arm of
 * {@code thenChainHasUnhandledControlFlow} widens to ARM-ROOT nested then-chains that
 * are themselves fully admissible (the #352-i1 recursion at the ARM position,
 * rule-path-scoped); the handshake arm drains the arm chain's
 * {@code DeepThenArgHoist} decls INTO the owning branch, wraps a MapperS-typed
 * non-factory arm {@code MapperC.of(…)} at a MULTI seat, and keeps the WRAPPER join
 * when every arm carries the same meta element (the #144/#269/#334 law). The
 * two-channel in-lambda naming law: a cond-base chain at the LAMBDA-BODY ROOT with
 * {@code n >= 2} in-lambda hoists and a NUMBERED ({@code >= 2}) method session group
 * JOINS the method session ({@code thenArg3/thenArg4} continue the method's
 * {@code thenArg0..2}) while the in-BRANCH chain keeps the per-scope deferred channel
 * ({@code _thenArg0.._thenArg2} escape on ancestor collision). The four green
 * carriers pin every other cell of the law: asic UnderlyingIdOtherRule (no cond base
 * — {@code _thenArg0/1}), esma/fca UnderlyingIdentificationRule (bare-singleton
 * session — {@code _thenArg}), DTCC + csa CountryAndProvinceOrTerritoryOfIndividual
 * ({@code n == 1} singletons stay bare), drr GetReportableSchedulePeriod (session
 * size 1 — per-scope {@code thenArg0/1} unescaped).
 *
 * <p><b>W aliasOutputBuilderNav</b> (FunctionAliasHelper + FunctionGenerator +
 * FunctionTemplateModel + FunctionExpressionRenderer + ReferenceHandler + the
 * function template): an alias navigating FROM the function OUTPUT (the chain root
 * parses as a FULLY-unresolved {@code REnumValueRef} — the parser cannot root a nav
 * at the output) joins the DORMANT usesOutput class: the output builder threads as
 * the alias's FIRST param, the return type is the NAV ELEMENT's builder
 * ({@code Payout.PayoutBuilder}), the body wraps
 * {@code return toBuilder(<nav>.get());} with the {@code _product} scope-seed
 * escape, and every call site wraps
 * {@code MapperS.of(payout(product.toBuilder(), …).build())}. Single corpus carrier
 * (the corpus rosetta scan finds exactly one output-rooted alias).
 *
 * <p>Whole-file byte locks run through the REAL D11 routes (the full POJO/rule cell
 * route for drr; the FUNCTION route for cdm5) and revert RED without the facets;
 * every witness token is occurrence-counted (python {@code str.count} semantics —
 * the #352 law) and PRE-counted against f-probe-380post (each removal token
 * PRE 1 / golden 0; each golden token PRE 0 / golden &ge; 1).
 */
class InLambdaDecompOutputBuilderTrioComposeTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM5_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-5.38.0");
    private static final Path CDM5_GOLDEN_DIR =
            CDM5_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static final String UATPI_LEG1_RULE =
            "drr/standards/iosco/cde/version3/underlier/reports/"
                    + "UnderlyingAssetTradingPlatformIdentifierLeg1Rule.java";
    private static final String UATPI_LEG2_RULE =
            "drr/standards/iosco/cde/version3/underlier/reports/"
                    + "UnderlyingAssetTradingPlatformIdentifierLeg2Rule.java";
    private static final String NEW_EQUITY_SWAP_PRODUCT =
            "cdm/event/common/functions/NewEquitySwapProduct.java";

    private static Map<String, String> drrOutput;
    private static Map<String, String> cdm5FnOutput;

    static boolean drrCellAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR);
    }

    static boolean cdm5CellAvailable() {
        return Files.isDirectory(CDM5_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM5_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (drrCellAvailable()) {
            drrOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT));
        }
        if (cdm5CellAvailable()) {
            cdm5FnOutput = generateFunctions(
                    new D11CorpusRegressionTest.CellSpec("cdm", "5.38.0", CDM5_CELL_ROOT));
        }
    }

    /** The REAL D11 full-cell generation path (POJO/choice/rule/report/label + functions). */
    private static Map<String, String> generateCell(D11CorpusRegressionTest.CellSpec cell)
            throws IOException {
        var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(),
                D11CorpusRegressionTest.emissionFilter(cell));
        var typeUtil = new JavaTypeUtil();
        var typeTranslator = new JavaTypeTranslator(typeUtil);
        var pojoGen = new ModelObjectGenerator(gm, typeTranslator, typeUtil);
        var choiceGen = new ChoiceObjectGenerator(gm, typeTranslator, typeUtil, pojoGen);
        var funcGen = new FunctionGenerator(gm, typeTranslator, typeUtil);
        var ruleGen = new RuleGenerator(gm, typeTranslator, funcGen);
        var reportGen = new ReportGenerator(gm, typeTranslator, funcGen);
        var labelProviderGen = new LabelProviderGenerator(
                gm, typeTranslator, new DeepFeatureCallUtil(gm::getType),
                new LabelProviderGeneratorUtil());
        Map<String, String> output = new LinkedHashMap<>();
        for (RModel model : corpus.workspace().files()) {
            if (gm.shouldGenerate(model)) {
                String version = gm.version(model);
                assertNoGenerationErrors(pojoGen.generateClasses(model, version, output));
                assertNoGenerationErrors(choiceGen.generateClasses(model, version, output));
                assertNoGenerationErrors(ruleGen.generateClasses(model, version, output));
                assertNoGenerationErrors(reportGen.generateClasses(model, version, output));
                assertNoGenerationErrors(labelProviderGen.generateClasses(model, version, output));
            }
        }
        funcGen.generate(output);
        return output;
    }

    /** The REAL D11 FUNCTION-cell generation path. */
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

    // ==== byte locks (all 3 flips through the REAL D11 routes) ====

    /** D4b-1: UATPI Leg1 — the cond-base arm-chain decomposition + two-channel naming. */
    @Test
    @EnabledIf("drrCellAvailable")
    void uatpiLeg1_byteMatchesGolden() throws IOException {
        assertDrrByteMatchesGolden(UATPI_LEG1_RULE);
    }

    /** D4b-1: UATPI Leg2 — the identical sibling. */
    @Test
    @EnabledIf("drrCellAvailable")
    void uatpiLeg2_byteMatchesGolden() throws IOException {
        assertDrrByteMatchesGolden(UATPI_LEG2_RULE);
    }

    /** W: NewEquitySwapProduct cdm5 — the alias-over-output-builder class. */
    @Test
    @EnabledIf("cdm5CellAvailable")
    void newEquitySwapProduct_byteMatchesGolden() throws IOException {
        assertNotNull(cdm5FnOutput, "cdm5 generation did not run — corpus unavailable?");
        String generated = cdm5FnOutput.get(NEW_EQUITY_SWAP_PRODUCT);
        assertNotNull(generated, "Class not generated: " + NEW_EQUITY_SWAP_PRODUCT);
        Path goldenPath = CDM5_GOLDEN_DIR.resolve(NEW_EQUITY_SWAP_PRODUCT);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated cdm5 output must byte-match the golden (newline-normalized) for "
                        + NEW_EQUITY_SWAP_PRODUCT);
    }

    // ==== occurrence-counted witnesses (python str.count semantics — the #352 law) ====

    /**
     * D4b-1: the session-continued statement-seat hoists — the blank-final join
     * {@code thenArg3} (PRE 0 / golden 1), the tail step {@code thenArg4} re-rooted on
     * it (PRE 0 / golden 1) and the {@code MapperC.of(…)} single-arm wrap of the
     * then-arm's {@code _thenArg2.first()} (PRE 0 / golden 1).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void uatpiLeg1_sessionContinueAndArmWrap_witness() {
        String gen = drrOutput.get(UATPI_LEG1_RULE);
        assertNotNull(gen, "UATPI Leg1 not generated");
        assertEquals(1, count(gen, "final MapperC<FieldWithMetaString> thenArg3;"),
                "the blank-final cond join continues the method session (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "final MapperC<FieldWithMetaString> thenArg4 = thenArg3"),
                "the tail filter step continues the session (PRE 0 / golden 1)");
        assertEquals(1, count(gen, "thenArg3 = MapperC.of(_thenArg2"),
                "the MapperS-typed then-arm wraps MapperC.of (PRE 0 / golden 1)");
    }

    /**
     * D4b-1: the in-BRANCH escaped chain decls PRESENT ({@code _thenArg0} decl —
     * PRE 0 / golden 1) and the inline ternary GONE (the
     * {@code ).getOrDefault(false) ? item.<EntityIdentifier>mapC(} token — PRE 1 /
     * golden 0, the negative witness the flip removes).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void uatpiLeg1_inBranchEscapeAndTernaryGone_witness() {
        String gen = drrOutput.get(UATPI_LEG1_RULE);
        assertNotNull(gen, "UATPI Leg1 not generated");
        assertEquals(1, count(gen,
                "final MapperC<EntityIdentifier> _thenArg0 = item.<EntityIdentifier>mapC("),
                "the in-branch chain escapes + renumbers per-scope (PRE 0 / golden 1)");
        assertEquals(0, count(gen, ").getOrDefault(false) ? item.<EntityIdentifier>mapC("),
                "the inline ternary must be gone (PRE 1 / golden 0)");
    }

    /**
     * W: the builder signature (abstract + impl — PRE 0 / golden 2) and the SIX
     * call-site wraps (PRE 0 / golden 6).
     */
    @Test
    @EnabledIf("cdm5CellAvailable")
    void newEquitySwapProduct_builderSignatureAndCallSites_witness() {
        String gen = cdm5FnOutput.get(NEW_EQUITY_SWAP_PRODUCT);
        assertNotNull(gen, "NewEquitySwapProduct not generated");
        assertEquals(2, count(gen, "Payout.PayoutBuilder payout(Product.ProductBuilder product,"
                + " Security security, EquitySwapMasterConfirmation2018 masterConfirmation)"),
                "the output builder threads as the FIRST param on both decls (PRE 0 / golden 2)");
        assertEquals(6, count(gen,
                "MapperS.of(payout(product.toBuilder(), security, masterConfirmation).build())"),
                "every call site wraps the Builder-returning call (PRE 0 / golden 6)");
    }

    /**
     * W: the toBuilder body wrap with the {@code _product} scope-seed escape PRESENT
     * (PRE 0 / golden 1) and the old Mapper signature GONE (PRE 1 / golden 0 — the
     * negative witness the flip removes).
     */
    @Test
    @EnabledIf("cdm5CellAvailable")
    void newEquitySwapProduct_toBuilderBodyAndOldSigGone_witness() {
        String gen = cdm5FnOutput.get(NEW_EQUITY_SWAP_PRODUCT);
        assertNotNull(gen, "NewEquitySwapProduct not generated");
        assertEquals(1, count(gen, "return toBuilder(MapperS.of(product).<ContractualProduct>"
                + "map(\"getContractualProduct\", _product -> _product.getContractualProduct())"),
                "the body wraps toBuilder(<nav>.get()) with the _product escape (PRE 0 / golden 1)");
        assertEquals(0, count(gen, "protected abstract MapperS<? extends Payout> payout(Security security"),
                "the old Mapper signature must be gone (PRE 1 / golden 0)");
    }

    private static int count(String haystack, String needle) {
        int n = 0;
        int i = haystack.indexOf(needle);
        while (i >= 0) {
            n++;
            i = haystack.indexOf(needle, i + needle.length());
        }
        return n;
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }

    private static void assertDrrByteMatchesGolden(String path) throws IOException {
        assertNotNull(drrOutput, "drr generation did not run — corpus unavailable?");
        String generated = drrOutput.get(path);
        assertNotNull(generated, "Class not generated: " + path);
        Path goldenPath = DRR_GOLDEN_DIR.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated drr output must byte-match the golden (newline-normalized) for " + path);
    }
}
