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
 * Facet {@code then_statement_hoisting} (arm TA) — upstream compiles EVERY
 * {@code then}-operation as a hoisted statement, never a runtime chain: each
 * then-argument becomes a {@code final MapperS/MapperC<X> thenArg}/{@code thenArgN}
 * local and each body compiles re-rooted on its preceding local (upstream
 * {@code caseThenOperation} + the statement-floating builder composition). The
 * goldens carry ZERO runtime {@code .then(} calls — there is no
 * {@code Mapper.then(Function)} runtime method, so the generic inline form never
 * compiles. The fork's statement form ({@code renderThenExtractSet} /
 * {@code renderBareInvokableThenSet}, PR #95–#99) was gated rule-body-only
 * (BC-PRIMARY caution at introduction); this facet extends it to FUNCTION bodies.
 *
 * <p>Three fork arms:
 *
 * <ol>
 *   <li><b>S1 — function-body gate.</b> The two {@code renderOperationInner}
 *       then-statement branches drop their blanket {@code findEnclosingRule != null}
 *       guard: a function {@code assignOutput} SET whose RHS is an implicit
 *       then-chain or a bare-FUNCTION then takes the hoisted {@code thenArg} form
 *       (drr {@code MinAdjustableDateResolution}, the iosco cde
 *       {@code Initial/VariationMargin*Haircut} family). The bare-RULE sub-case
 *       alone stays rule-body-gated — the dependency collector injects the rule
 *       receiver field only on the rule-emission path.</li>
 *   <li><b>S2 — conditional-branch routing.</b> A then-chain in the THEN/ELSE
 *       arm of a conditional SET ({@code renderConditionalAssignment}) routes
 *       through the same statement renderer at the branch's indent — the
 *       hoisted locals live INSIDE the if/else block (drr
 *       {@code FormatToMax3Number}, {@code GetProductIdentifierFilteringISIN}).</li>
 *   <li><b>S3 — only-element last body.</b> A bare {@code only-element} then-body
 *       over the rebound implicit item renders upstream's Mapper-valued
 *       {@code MapperS.of(thenArg.get())} (+ the assignment {@code .get()}),
 *       not the self-unwrapping chain collapse (cdm
 *       {@code ExtractCounterpartyByRole}/{@code ExtractAncillaryPartyByRole}).</li>
 * </ol>
 *
 * <p>Deliberately OUT of scope at THIS facet (the then-body ITEM-TYPING family,
 * deferred to its own facet alongside maxmin_lambda_item_typing — landed at
 * facet then_maxmin_item_typing, {@link FunctionThenItemTypingTest}): bare-feature
 * then-bodies that must synthesize navigation off the rebound {@code thenArg} (drr
 * {@code GetLastFloatingReferenceResetDate}) and rebound-receiver cardinality for
 * filter/extract inside then-bodies ({@code filterItemNullSafe}/{@code mapItem}
 * + the {@code MapperC} declaration wrapper, drr {@code GetMarginValue}) — those
 * carriers FLIPPED there. Likewise out of scope: a function body with MULTIPLE
 * statement-form then operations would reuse the per-statement {@code thenArg}
 * base name across sibling statements (golden numbers such groups scope-wide);
 * no flip carrier has more than one, and any such file stays waivered.
 *
 * <p>Green-safety: the pre-fix inline {@code .then(lambda)} form never compiles
 * (no runtime method), so no byte-matching FUNCTION file carries any shape these
 * arms change; a misjudged carrier stays waivered rather than regressing.
 *
 * <p>Anchors are WHOLE-FILE byte comparisons against the frozen goldens
 * (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}.
 */
class FunctionThenStatementHoistTest {

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

    /** S1 alone: a top-level function SET then-chain takes the hoisted thenArg form (drr). */
    @Test
    @EnabledIf("drrCellAvailable")
    void minAdjustableDateResolutionDrr_functionThenArgHoist_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/regulation/common/functions/MinAdjustableDateResolution.java");
    }

    /** S1 alone: the iosco cde margin family pins the same shape (drr). */
    @Test
    @EnabledIf("drrCellAvailable")
    void initialMarginCollectedPostHaircutDrr_functionThenArgHoist_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/standards/iosco/cde/version1/collateral/functions/"
                + "InitialMarginCollectedByReportingCounterpartyPostHaircut.java");
    }

    /** S1+S3: bare only-element last body renders MapperS.of(thenArg.get()).get() (cdm5). */
    @Test
    @EnabledIf("cdm5CellAvailable")
    void extractCounterpartyByRoleCdm5_onlyElementLastBody_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(cdm5FunctionOutput, CDM5_GOLDEN_DIR,
                "cdm/base/staticdata/party/functions/ExtractCounterpartyByRole.java");
    }

    /** S1+S3: the same shape pinned in the cdm6 cell via the ancillary-party sibling. */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void extractAncillaryPartyByRoleCdm6_onlyElementLastBody_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/base/staticdata/party/functions/ExtractAncillaryPartyByRole.java");
    }

    /** S2: a then-chain in a conditional SET branch hoists inside the if-block (drr). */
    @Test
    @EnabledIf("drrCellAvailable")
    void formatToMax3NumberDrr_conditionalBranchThenArg_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/regulation/common/functions/FormatToMax3Number.java");
    }

    /** S2: branch-hoisted thenArg with a {@code first} last body (drr). */
    @Test
    @EnabledIf("drrCellAvailable")
    void getProductIdentifierFilteringIsinDrr_branchThenArgFirstBody_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/regulation/common/functions/GetProductIdentifierFilteringISIN.java");
    }

    /** S2 (oracle-discovered): a branch then-chain whose extract body carries the meta-deref block lambda (drr). */
    @Test
    @EnabledIf("drrCellAvailable")
    void getMarginCurrencyDrr_branchThenWithMetaDerefBody_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/regulation/common/functions/GetMarginCurrency.java");
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
                + path + " — the function-body then-statement gate (S1), the conditional-"
                + "branch thenArg routing (S2), or the only-element last-body Mapper form "
                + "(S3) is missing or regressed, if the then_statement_hoisting recovery "
                + "is reverted.");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n");
    }
}
