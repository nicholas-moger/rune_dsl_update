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
 * PR #354 — the block-arm interior ladder + the null-typed else meta-deref + the
 * mis-bound sibling enum: 9 byte flips (ALL drr POJO Rules) + 0 movers / 0 AWAY
 * (regscan354: the flips are the only byte movement corpus-wide).
 *
 * <p><b>F-block — {@code blockArmInteriorLadder}</b> (CollectionHandler +
 * JavaStatementScope): {@code compileLadderConditionalBlock} registers its rung chain
 * around each rung-arm / terminal-else compile ({@code pushBlockArmSeatConditionals}),
 * and {@code isCleanLadderContext} treats a REGISTERED conditional ancestor as
 * TRANSPARENT — the outer is mid-BLOCK-render, so its arm is a statement seat where
 * golden block-converts the interior ladder too. Render truth, never AST shape: an
 * inline-ternary outer is unregistered and keeps the #281 decline (the P352A suppress
 * class). Scoped by the then-free path (an interior behind a runtime {@code .then(}
 * lambda stays declined — the LoadType class) + the then/switch-free ladder subtree
 * (the #350 handshake law) + {@code extractCount <= 1} below the registered seat (the
 * ancestry above it was validated by the outer's own admission). OptionStyleRule
 * common + esma + fca flip.
 *
 * <p><b>F-m1 — {@code nullTypedElseMetaDeref}</b> (CollectionHandler): the #339
 * effective-else mixed-join else-deref declined its own javadoc carriers because the
 * else compiles NULL-typed — (a) a {@code first}-collapsed nav chain (common
 * Counterparty2Rule: {@code reportingSide -> … -> identifier first}), (b) a
 * deep-hoisted then-chain whose consumer is untyped (cftc UniqueSwapIdentifierRule
 * margin + valuation, csa DTCC_USIIDRule margin + valuation). Recover the wrapper from
 * the arm's AST terminal ({@code recoverExprMetaWrapper} case (a) fused collapse /
 * case (b) then-walk), re-stamp with the SAME render + refs, and take the standard
 * coercion route (null-guarded MapperS deref / plain MapperC element map). The outer
 * {@code thenArg0} decl element follows FREE through the #294
 * {@code blockArmDerefsToBareLeaf} return-terminal marker gate.
 *
 * <p><b>F-m2 — {@code annotationShadowedSiblingEnum}</b> (ComparisonHandler +
 * NavigationHandler): {@code filter qualification = confirmationDateTime} — the
 * sibling {@code qualification} mis-binds to the same-named ANNOTATION (the #204
 * class; the P354M2 probe: {@code sibSym=RAnnotation}), so the #299 attribute rung
 * declined. The new rung recovers the enum through the SAME re-root synthesis the
 * render uses ({@code ReferenceHandler.synthesizeImplicitItemBareNav} — same-walk
 * invariant) + the #299 typeCall tail; the value-name match stays the load-bearing
 * gate. Common ConfirmationTimestampRule flips.
 *
 * <p>Whole-file byte comparisons run through the REAL D11 generation path and revert
 * RED without the facets. The negative witnesses are load-bearing per the
 * witness-uniqueness law (OCCURRENCE counts, never line counts): every token below
 * counted EXACTLY 1 in its PRE gen and 0 in its golden — the flips REMOVE them.
 */
class BlockArmMetaDerefComposeTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    // F-block flip carriers.
    private static final String OPTION_STYLE_COMMON =
            "drr/regulation/common/trade/contract/reports/OptionStyleRule.java";
    private static final String OPTION_STYLE_ESMA =
            "drr/regulation/esma/emir/refit/trade/reports/OptionStyleRule.java";
    private static final String OPTION_STYLE_FCA =
            "drr/regulation/fca/ukemir/refit/trade/reports/OptionStyleRule.java";
    // F-m1a flip carrier.
    private static final String COUNTERPARTY2_COMMON =
            "drr/regulation/common/trade/party/reports/Counterparty2Rule.java";
    // F-m1b flip carriers.
    private static final String USI_CFTC_MARGIN =
            "drr/regulation/cftc/rewrite/margin/reports/UniqueSwapIdentifierRule.java";
    private static final String USI_CFTC_VALUATION =
            "drr/regulation/cftc/rewrite/valuation/reports/UniqueSwapIdentifierRule.java";
    private static final String USIID_CSA_MARGIN =
            "drr/regulation/csa/rewrite/margin/reports/DTCC_USIIDRule.java";
    private static final String USIID_CSA_VALUATION =
            "drr/regulation/csa/rewrite/valuation/reports/DTCC_USIIDRule.java";
    // F-m2 flip carrier.
    private static final String CONFIRMATION_TIMESTAMP_COMMON =
            "drr/regulation/common/trade/datetime/reports/ConfirmationTimestampRule.java";

    private static Map<String, String> drrCellOutput;

    static boolean drrCellAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (drrCellAvailable()) {
            drrCellOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT));
        }
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

    // ------------------------------------------------------------- F-block byte locks

    @Test
    @EnabledIf("drrCellAvailable")
    void optionStyleCommon_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(OPTION_STYLE_COMMON);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void optionStyleEsma_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(OPTION_STYLE_ESMA);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void optionStyleFca_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(OPTION_STYLE_FCA);
    }

    /**
     * The F-block negative witness: the inner ladder's inline-ternary token
     * {@code .getOrDefault(false) ? OptionStyleEnum.AMER} counted EXACTLY 1 occurrence
     * in each PRE gen and 0 in each golden — the block conversion REMOVES it (the arm
     * becomes {@code return MapperS.of(OptionStyleEnum.AMER);}).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void optionStyleCarriers_innerInlineTernaryGone() {
        for (String path : new String[] {OPTION_STYLE_COMMON, OPTION_STYLE_ESMA,
                OPTION_STYLE_FCA}) {
            assertEquals(0, count(gen(path), ".getOrDefault(false) ? OptionStyleEnum.AMER"),
                    "The inner inline ternary is gone (count 0 in golden): " + path);
        }
    }

    // ------------------------------------------------------------- F-m1a byte lock

    @Test
    @EnabledIf("drrCellAvailable")
    void counterparty2Common_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(COUNTERPARTY2_COMMON);
    }

    /**
     * The F-m1a negative witness: the un-deref'd terminal {@code .first();} counted
     * EXACTLY 1 occurrence in the PRE gen and 0 in the golden (golden continues
     * {@code .first().<String>map("Type coercion", …)}) — the deref REMOVES the bare
     * statement-terminal form.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void counterparty2Common_bareFirstTerminalGone() {
        assertEquals(0, count(gen(COUNTERPARTY2_COMMON), ".first();"),
                "The un-deref'd .first(); terminal is gone (count 0 in golden)");
    }

    // ------------------------------------------------------------- F-m1b byte locks

    @Test
    @EnabledIf("drrCellAvailable")
    void uniqueSwapIdentifierCftcMargin_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(USI_CFTC_MARGIN);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void uniqueSwapIdentifierCftcValuation_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(USI_CFTC_VALUATION);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void dtccUsiidCsaMargin_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(USIID_CSA_MARGIN);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void dtccUsiidCsaValuation_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(USIID_CSA_VALUATION);
    }

    /**
     * The F-m1b negative witness: the wrapper-typed decl token
     * {@code MapperC<FieldWithMetaString> thenArg0} counted EXACTLY 1 occurrence in
     * each PRE gen and 0 in each golden — the in-block terminal deref retypes the decl
     * element to {@code String} through the #294 marker gate.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void deepHoistCarriers_wrapperTypedDeclGone() {
        for (String path : new String[] {USI_CFTC_MARGIN, USI_CFTC_VALUATION,
                USIID_CSA_MARGIN, USIID_CSA_VALUATION}) {
            assertEquals(0, count(gen(path), "MapperC<FieldWithMetaString> thenArg0"),
                    "The wrapper-typed thenArg0 decl is gone (count 0 in golden): " + path);
        }
    }

    // ------------------------------------------------------------- F-m2 byte lock

    @Test
    @EnabledIf("drrCellAvailable")
    void confirmationTimestampCommon_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(CONFIRMATION_TIMESTAMP_COMMON);
    }

    /**
     * The F-m2 negative witness: the unresolved bare-symbol echo
     * {@code MapperS.of(confirmationDateTime)} counted EXACTLY 1 occurrence in the PRE
     * gen and 0 in the golden — the sibling-enum recovery qualifies it to
     * {@code MapperS.of(EventTimestampQualificationEnum.CONFIRMATION_DATE_TIME)}.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void confirmationTimestampCommon_bareSymbolEchoGone() {
        assertEquals(0, count(gen(CONFIRMATION_TIMESTAMP_COMMON),
                        "MapperS.of(confirmationDateTime)"),
                "The unresolved bare-symbol echo is gone (count 0 in golden)");
    }

    // ---------------------------------------------------------------- helpers

    private static int count(String haystack, String needle) {
        int n = 0;
        for (int i = haystack.indexOf(needle); i >= 0; i = haystack.indexOf(needle, i + 1)) {
            n++;
        }
        return n;
    }

    private static String gen(String path) {
        assertNotNull(drrCellOutput, "generation did not run — corpus unavailable?");
        String g = drrCellOutput.get(path);
        assertNotNull(g, "Class not generated: " + path);
        return g;
    }

    private static void assertByteMatchesGolden(String path) throws IOException {
        String gen = gen(path);
        Path goldenPath = DRR_GOLDEN_DIR.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath).replace("\r", "");
        assertEquals(golden, gen.replace("\r", ""),
                "Generated bytes must match the golden for " + path);
    }
}
