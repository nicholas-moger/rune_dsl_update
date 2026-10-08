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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static com.regnosys.rosetta.testutil.GenerationErrors.assertNoGenerationErrors;

/**
 * PR #288 — facet interiorThenArgCardinality: ONE green-safe, RULE-SCOPED (FUNCTION-byte-neutral)
 * GENERATOR mechanism, 4 drr POJO Rule byte flips + compounding. The interior-thenArg seat of the
 * report-output cardinality lineage (#272/#273/#274/#275), via the #282/#286/#287 disguised-chain
 * descent.
 *
 * <p><b>The bug.</b> A drr rule body {@code … then extract [head -> leaf]} (or {@code then extract
 * X} delegating to it) whose disguised 2-name navigation chain ({@code head -> leaf}, an
 * {@code REnumValueRef}) terminates on a MULTI feature — {@code originatingWorkflowStep -> timestamp}
 * ({@code timestamp} MULTI), {@code contractDetails -> documentation} ({@code documentation} MULTI) —
 * produces a MULTI thenArg: golden renders {@code final MapperC<X> thenArgN = … .mapSingleToList(…)}
 * and the downstream {@code … .filterItemNullSafe(…)}. The fork mis-computes it SINGLE
 * ({@code MapperS} + {@code mapSingleToItem} + {@code filterSingleNullSafe}), a {@code MapperS<X> =
 * MapperC.of(…)}-shaped NON_COMPILING mismatch. The cardinality reads SINGLE because BOTH seats are
 * blind to the disguised chain: {@code CollectionHandler.isBodyMulti}'s parser-side
 * {@code CardinalityComputer} cannot descend it (the extract-method seat), and
 * {@code NavigationHandler.chainProvesMulti}'s {@code REnumValueRef} arm uses the 1-arg
 * {@code resolveDisguisedFeature} that resolves a disguised head only against the enclosing
 * function's INPUT/OUTPUT/shortcut NAMES, never as the FIRST nav feature off the implicit item (the
 * thenArg-decl + filter seats) — the SAME blindness #286/#287 fixed for the meta-deref recoveries.
 *
 * <p><b>The fix (GENERATOR-only, 2 source files).</b> {@code NavigationHandler}'s new
 * {@code disguisedChainProvesMulti} re-roots a disguised {@code head -> leaf} onto its own item type
 * (the #282 {@code implicitItemDataTypeOrInferred} descent, shared with
 * {@code resolveDisguisedChainLeafAttr}) and reads head/leaf cardinality; {@code chainProvesMulti}'s
 * {@code REnumValueRef} arm falls back to it (RULE-scoped) when the 1-arg resolver declines — fixing
 * the thenArg decl ({@code renderThenExtractSet}'s {@code single} flag) and the downstream
 * {@code filterMethod}. {@code CollectionHandler.isBodyMulti} gains a compiler-aware overload whose
 * rule-scoped overlay additionally consults the now-disguised-chain-aware {@code chainProvesMulti} —
 * fixing the {@code mapSingleToItem}/{@code mapSingleToList} method. Both rule-scoped + monotone
 * (only ADDs multi) → the FUNCTION tail + cdm/iso/fpml POJO stay byte-IDENTICAL (the #232 lesson),
 * and the no-compiler {@code isBodyMulti} (the #275 whole-output terminal seat) keeps its prior bytes.
 *
 * <p><b>Green-safe by construction.</b> The fork's single form ({@code MapperS<X> thenArgN =
 * <multi>.mapSingleToItem(…)}) does not compile when the body navigates a MULTI feature, so every
 * carrier was an already-waivered NON_COMPILING mismatch; recovering the correct MULTI cardinality
 * only flips a waivered file or moves a co-occupied one toward golden. byte-oracle / stash-baseline
 * measured exactly 4 flips (drr POJO 448 → 444; 16 now-matching = 4 mine + 12 stale, clean-main
 * re-dump = exactly the 12 stale), 0 within-waiver regressions / 0 new mismatches / 29 toward-golden
 * + 33 neutral churn; all FUNCTION cells (cdm5 81 / cdm6 236 / drr 207) + cdm/iso/fpml POJO
 * byte-IDENTICAL.
 *
 * <p>Anchors are WHOLE-FILE byte comparisons of the generated drr output against the frozen goldens
 * (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}. REVERT-VERIFIED RED.
 */
class RuleInteriorThenArgCardinalityTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static Map<String, String> drrOutput;

    static boolean drrCellAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (drrCellAvailable()) {
            drrOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT));
        }
    }

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

    // ==== Flip locks (revert-RED): interiorThenArgCardinality carriers now byte-match golden. ====

    /**
     * Flip — EventTimestampRule (iosco cde v3): {@code … then extract originatingWorkflowStep ->
     * timestamp …}, {@code timestamp} MULTI. The disguised chain now proves MULTI, so the thenArg
     * declares {@code MapperC} + {@code mapSingleToList} + {@code filterItemNullSafe}.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void eventTimestamp_iosco_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/standards/iosco/cde/version3/datetime/reports/EventTimestampRule.java");
    }

    /** Flip — ClearingTimestampRule (common datetime): the {@code originatingWorkflowStep -> timestamp} sibling. */
    @Test
    @EnabledIf("drrCellAvailable")
    void clearingTimestamp_common_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/common/trade/datetime/reports/ClearingTimestampRule.java");
    }

    /** Flip — ClearingTimestampRule (mas trade): a different regime, same disguised-chain shape (thenArg1/2). */
    @Test
    @EnabledIf("drrCellAvailable")
    void clearingTimestamp_mas_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/mas/rewrite/trade/reports/ClearingTimestampRule.java");
    }

    /**
     * Flip — MasterAgreementVersionRule (common contract): {@code … then extract contractDetails ->
     * documentation …}, {@code documentation} MULTI — a DISTINCT disguised chain locking that the
     * mechanism is not specific to the {@code originatingWorkflowStep -> timestamp} shape.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void masterAgreementVersion_common_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/common/trade/contract/reports/MasterAgreementVersionRule.java");
    }

    // ==== Facet-boundary locks (revert-RED): the decline path + the compounding boundary. ====

    /**
     * Green-safety decline lock — BasketStructurerRule (asic trade) is GREEN: its
     * {@code then extract reportableInformation -> customBasket -> basketStructurerLei} chain
     * navigates only SINGLE features (terminal {@code PartyIdentifier}). The new
     * {@code disguisedChainProvesMulti} IS reached (the inner disguised {@code reportableInformation
     * -> customBasket} is re-rooted) but returns false (head AND leaf single), so the chain stays
     * SINGLE — {@code mapSingleToItem} + {@code MapperS} kept, byte-IDENTICAL to golden. Locks that
     * the monotone overlay does not false-fire on a green single-cardinality disguised then-extract.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void basketStructurer_singleDisguisedChain_declines_staysGreen() throws IOException {
        assertNotNull(drrOutput, "drr generation did not run — corpus unavailable?");
        String path = "drr/regulation/asic/rewrite/trade/reports/BasketStructurerRule.java";
        String gen = drrOutput.get(path);
        assertNotNull(gen, "BasketStructurerRule not generated");
        assertTrue(gen.contains(".mapSingleToItem(item -> item.<ReportableInformation>map("
                        + "\"getReportableInformation\""),
                "the single-terminal disguised chain must DECLINE (keep mapSingleToItem / MapperS)");
        assertFalse(gen.contains("MapperListOfLists"),
                "a single disguised then-extract must not be lifted to a list");
        String golden = Files.readString(DRR_GOLDEN_DIR.resolve(path));
        assertEquals(normalize(golden), normalize(gen),
                "BasketStructurerRule must STAY byte-IDENTICAL to golden (green-safety decline)");
    }

    /**
     * Compounding lock — the common-dtcc {@code DTCC_ProductIDRule} is a carrier where the
     * interior-thenArg cardinality recovery DOES fire — its {@code … then extract contractualProduct ->
     * productTaxonomy} body ({@code productTaxonomy} MULTI) lifts thenArg2 to
     * {@code final MapperC<ProductTaxonomy> thenArg2 = … .mapSingleToList(…)} + the downstream
     * {@code thenArg3 … .filterItemNullSafe(…)}. This carrier was co-occupied with a bare-enum
     * qualification residual ({@code MapperS.of(ISDA)} vs golden's {@code MapperS.of(
     * TaxonomySourceEnum.ISDA)}) at #288, so it stayed divergent; <b>PR #299
     * (bareEnumQualifyRBodyShadow) lifted that residual</b> ({@code ISDA} binds to a
     * {@code body Authority ISDA} RBody → now qualified {@code TaxonomySourceEnum.ISDA}), so the carrier
     * is now byte-identical to golden — the #288 cardinality + #299 bare-enum compounding completes it.
     * Locks that the #288 cardinality recovery remains present in the now-clean output.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void dtccProductId_cardinalityFires_nowCleanAfterBareEnum() throws IOException {
        assertNotNull(drrOutput, "drr generation did not run — corpus unavailable?");
        String path = "drr/regulation/common/dtcc/reports/DTCC_ProductIDRule.java";
        String gen = drrOutput.get(path);
        assertNotNull(gen, "common-dtcc DTCC_ProductIDRule not generated");
        assertTrue(gen.contains("final MapperC<ProductTaxonomy> thenArg2"),
                "the #288 interior-thenArg cardinality recovery must remain present in the now-clean "
                + "output — the contractualProduct -> productTaxonomy disguised chain (productTaxonomy "
                + "MULTI) lifts thenArg2 to MapperC + mapSingleToList");
        assertTrue(gen.contains("MapperS.of(TaxonomySourceEnum.ISDA)"),
                "the #299 bare-enum qualification must have lifted MapperS.of(ISDA) -> "
                + "MapperS.of(TaxonomySourceEnum.ISDA)");
        assertByteMatchesGolden(path);
    }

    private static void assertByteMatchesGolden(String path) throws IOException {
        assertNotNull(drrOutput, "drr generation did not run — corpus unavailable?");
        String generated = drrOutput.get(path);
        assertNotNull(generated, "Class not generated: " + path
                + " (emission failed or the path differs)");
        Path goldenPath = DRR_GOLDEN_DIR.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated drr output must byte-match the golden (newline-normalized) for "
                + path + " (PR #288 interiorThenArgCardinality).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
