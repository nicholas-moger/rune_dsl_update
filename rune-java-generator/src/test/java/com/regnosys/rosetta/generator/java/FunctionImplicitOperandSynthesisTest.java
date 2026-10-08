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
 * PR #218 — the generator-crash lever. Three composed GENERATOR mechanisms that
 * together flip a family of functions whose body was a {@code /* TODO: expression
 * compilation error … RExpression.accept … "expr" is null *}{@code /} crash:
 *
 * <ol>
 *   <li><b>implicit_operand_synthesis</b> — a bare implicit operand (the WHOLE piped
 *       value of a {@code then}-body or lambda consumed directly, with no feature
 *       navigation: {@code … then all = True}, {@code … then exists}, {@code … then
 *       default False}, {@code … then join ";"}, an extract-lambda {@code switch})
 *       parses with a {@code null} operand RExpression. The fork compiled the raw
 *       {@code null} → NPE → the TODO. {@code HandlerHelper.orSyntheticImplicit}
 *       substitutes a synthetic {@code RImplicitVariable} (parented at the operation)
 *       at the five operand-compile seats (Comparison/Existence/Join/Switch/Default),
 *       which resolves to the bound {@code thenArg} (then-body) / {@code item} (lambda)
 *       via {@code ReferenceHandler.handle(RImplicitVariable)}. Removes the NPE
 *       (24→0 of these crashes) — a 0-flip foundation.</li>
 *   <li><b>asMapper</b> — a {@code ComparisonResult} (equality / existence / contains /
 *       logical) consumed in a {@code then}-chain MapperS sink coerces with
 *       {@code .asMapper()}: a {@code thenArg} declaration ({@code final MapperS<Boolean>
 *       thenArg1 = areEqual(...).asMapper()}), a then-OUTPUT ({@code out = areEqual(thenArg,
 *       ...).asMapper().get()} / {@code exists(thenArg).asMapper().get()}), and a
 *       {@code contains} map-body ({@code item -> contains(...).asMapper()}). Scoped to the
 *       then-chain path: a DIRECT comparison output ({@code set out: a = b}) keeps the bare
 *       {@code areEqual(...).get()} form (252 green carriers).</li>
 *   <li><b>defaultForm</b> — a scalar-literal {@code default} value ({@code … default False})
 *       is the plain-T {@code getOrDefault} overload: the arg renders BARE and the whole
 *       result wraps in {@code MapperS.of(...)} — golden {@code MapperS.of(<X>.getOrDefault(
 *       false))} (a then-output appends {@code .get()}; a comparison operand uses it
 *       as-is). The fork's pre-fix {@code getOrDefault(MapperS.of(false))} (wrapped arg, no
 *       result wrap) is always waivered.</li>
 * </ol>
 *
 * <p>GREEN-SAFE BY CONSTRUCTION: every mechanism activates only on a shape that did NOT
 * compile / byte-match before (a null operand crashed; a {@code ComparisonResult} assigned
 * to {@code MapperS<Boolean>} or passed to {@code mapItem} does not compile without
 * {@code .asMapper()}; a literal default in {@code MapperS.of(...)} never byte-matched the
 * bare golden arg). Whole-file byte anchors through the REAL D11 loader over all 10 clean
 * carriers (cdm 5.38.0 ×2, cdm 6.20.6 ×8). REVERT-VERIFIED RED: reverting any of the three
 * mechanisms reverts the corresponding anchors to the crash TODO / un-coerced form.
 *
 * <p>The crash-clean residual that needs FURTHER rendering facets (deferred to a follow-up):
 * {@code IsAcceptedEicCode} (a {@code contains} operand-order fix), and the inlineThen-hoist
 * family ({@code GetEventDate}, {@code CheckCriteria}, {@code CriteriaMatchesAssetType},
 * {@code Qualify_Transaction_ZeroCoupon_KnownAmount}, {@code Create_SubmissionHarmonizedData}).
 */
class FunctionImplicitOperandSynthesisTest {

    private static final Path CDM5_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-5.38.0");
    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM5_GOLDEN_DIR = CDM5_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM6_GOLDEN_DIR = CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static Map<String, String> cdm5FunctionOutput;
    private static Map<String, String> cdm6FunctionOutput;

    static boolean cdmCellsAvailable() {
        return Files.isDirectory(CDM5_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM5_GOLDEN_DIR)
                && Files.isDirectory(CDM6_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM6_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (cdmCellsAvailable()) {
            cdm5FunctionOutput = generateCell(new D11CorpusRegressionTest.CellSpec("cdm", "5.38.0", CDM5_CELL_ROOT));
            cdm6FunctionOutput = generateCell(new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CDM6_CELL_ROOT));
        }
    }

    private static Map<String, String> generateCell(D11CorpusRegressionTest.CellSpec cell) throws IOException {
        var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(), D11CorpusRegressionTest.emissionFilter(cell));
        var typeUtil = new JavaTypeUtil();
        var gen = new FunctionGenerator(gm, new JavaTypeTranslator(typeUtil), typeUtil);
        Map<String, String> output = new LinkedHashMap<>();
        List<GenerationException> genErrors = gen.generateWithErrors(output);
        assertTrue(genErrors.isEmpty(), cell + " FUNCTION generation reported errors: " + genErrors);
        return output;
    }

    // ---- asMapper-only carriers (then-output ComparisonResult coercion) ----

    /** cdm5 `extract (validRoles contains role) then all = True` — contains map-body + then-output areEqual asMapper. */
    @Test
    @EnabledIf("cdmCellsAvailable")
    void cdm5_isValidPartyRole_asMapper_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5FunctionOutput, CDM5_GOLDEN_DIR,
                "cdm/event/position/functions/IsValidPartyRole.java");
    }

    @Test
    @EnabledIf("cdmCellsAvailable")
    void cdm6_isValidPartyRole_asMapper_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/event/position/functions/IsValidPartyRole.java");
    }

    /** `… filter … then exists` — then-output existence asMapper. */
    @Test
    @EnabledIf("cdmCellsAvailable")
    void cdm5_qualifyTransactionZeroCoupon_asMapper_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm5FunctionOutput, CDM5_GOLDEN_DIR,
                "cdm/product/qualification/functions/Qualify_Transaction_ZeroCoupon.java");
    }

    @Test
    @EnabledIf("cdmCellsAvailable")
    void cdm6_qualifyTransactionZeroCoupon_asMapper_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/product/qualification/functions/Qualify_Transaction_ZeroCoupon.java");
    }

    // ---- PayoutOnlyExists carriers (asMapper thenArg-decl/then-output + defaultForm) ----

    @Test
    @EnabledIf("cdmCellsAvailable")
    void cdm6_commodityPayoutOnlyExists_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/product/qualification/functions/CommodityPayoutOnlyExists.java");
    }

    @Test
    @EnabledIf("cdmCellsAvailable")
    void cdm6_creditDefaultPayoutOnlyExists_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/product/qualification/functions/CreditDefaultPayoutOnlyExists.java");
    }

    @Test
    @EnabledIf("cdmCellsAvailable")
    void cdm6_interestRatePayoutOnlyExists_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/product/qualification/functions/InterestRatePayoutOnlyExists.java");
    }

    @Test
    @EnabledIf("cdmCellsAvailable")
    void cdm6_optionPayoutOnlyExists_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/product/qualification/functions/OptionPayoutOnlyExists.java");
    }

    @Test
    @EnabledIf("cdmCellsAvailable")
    void cdm6_performancePayoutOnlyExists_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/product/qualification/functions/PerformancePayoutOnlyExists.java");
    }

    @Test
    @EnabledIf("cdmCellsAvailable")
    void cdm6_settlementPayoutOnlyExists_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/product/qualification/functions/SettlementPayoutOnlyExists.java");
    }

    private static void assertByteMatchesGolden(Map<String, String> cellOutput, Path goldenDir, String path)
            throws IOException {
        assertNotNull(cellOutput, "Function generation did not run — corpus unavailable?");
        String generated = cellOutput.get(path);
        assertNotNull(generated, "Function not generated: " + path);
        Path goldenPath = goldenDir.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated output must byte-match the golden (newline-normalized) for " + path
                + " — a bare implicit then/lambda operand must synthesize the bound thenArg/item "
                + "(implicit_operand_synthesis), a then-chain ComparisonResult must coerce with "
                + ".asMapper() (asMapper), and a scalar-literal `default` must render "
                + "MapperS.of(<X>.getOrDefault(<bare-literal>)) (defaultForm). Reverting any of the "
                + "three reverts this anchor to the crash TODO / un-coerced form (RED).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
