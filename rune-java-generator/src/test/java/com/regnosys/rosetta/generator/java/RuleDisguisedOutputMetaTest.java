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
 * PR #287 — facet disguisedOutputMeta: ONE green-safe, RULE-SCOPED (FUNCTION-byte-neutral) GENERATOR
 * mechanism, 10 drr POJO Rule byte flips + compounding. The whole-output deref analogue of #286's
 * receiver-coercion disguised-chain recovery — the #286 compounding follow-on (the user-chosen lead a).
 *
 * <p><b>The bug (the #286 carriers' 2nd mechanism).</b> A drr rule body
 * {@code <head> -> <feature> then extract [partyId -> identifier only-element]} whose SECOND extract
 * body is a DISGUISED 2-name navigation chain ({@code partyId -> identifier}, an {@code REnumValueRef})
 * collapsed by an {@code only-element}: after #286 inserts the RECEIVER deref
 * {@code item.<Party>map("Type coercion", w -> w == null ? null : w.getValue())}, golden ALSO hoists
 * the terminal meta wrapper local and null-guards {@code output = <wrapper>.getValue();}, but the fork
 * assigns the {@code FieldWithMetaString} wrapper bare to the {@code String} output (NON_COMPILING).
 * {@code renderThenExtractSet}'s then-output meta recovery declined because the extract erases its
 * compiled type to null AND {@code NavigationHandler.tryTerminalMetaMapperType} cannot descend the
 * disguised chain ({@code terminalNavAttr}'s {@code resolveDisguisedFeature} resolves a disguised head
 * only against the enclosing function's INPUT/OUTPUT/shortcut NAMES, never as a FEATURE of the item) —
 * the SAME blindness #286 fixed for the receiver seat, now at the output terminal.
 *
 * <p><b>The fix (GENERATOR-only, 2 source files).</b> {@code NavigationHandler}'s new
 * {@code recoverDisguisedTerminalMetaWrapper} re-roots the disguised output chain onto its own item
 * type (descending the {@code only-element} list-op, the #282 {@code implicitItemDataTypeOrInferred}
 * route; sharing the descend/re-root core {@code resolveDisguisedChainLeafAttr} with #286) and returns
 * the leaf's meta wrapper. {@code FunctionExpressionRenderer.renderThenExtractSet} passes it as the
 * {@code renderMetaValueDerefOrNull} precomputed meta (the #265 lambdaSeatMeta channel), RULE-scoped
 * ({@code findEnclosingRule}) + SINGLE-output. The single-output gate is load-bearing: a disguised
 * output chain with NO single-collapse whose head/leaf is MULTI is a List output golden derefs
 * element-wise ({@code .getMulti()}) — declined (OriginalSwapUTIRule), which drove the lone
 * within-waiver regression to 0. Rule-scoped + single-output → the FUNCTION tail + cdm/iso/fpml POJO
 * stay byte-IDENTICAL (the #232 shared-seat lesson).
 *
 * <p><b>Green-safe by construction.</b> The fork's bare {@code output = <wrapper>.get()} (a meta
 * wrapper into a bare-value output) does not compile, so every carrier was an already-waivered
 * NON_COMPILING mismatch; recovering the wrapper only flips a waivered file or moves a co-occupied one
 * toward golden. byte-oracle / stash-baseline measured exactly 10 flips (drr POJO 458 → 448; 22
 * now-matching = 10 mine + 12 stale, clean-main re-dump = exactly the 12 stale), 0 within-waiver
 * regressions / 0 new mismatches / 1 toward-golden + 3 neutral churn; all FUNCTION cells
 * (cdm5 81 / cdm6 236 / drr 207) + cdm/iso/fpml POJO byte-IDENTICAL.
 *
 * <p>Anchors are WHOLE-FILE byte comparisons of the generated drr output against the frozen goldens
 * (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}. REVERT-VERIFIED RED.
 */
class RuleDisguisedOutputMetaTest {

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

    // ==== Flip locks (revert-RED): disguisedOutputMeta carriers now byte-match golden. ====

    /**
     * Flip — Counterparty1Rule (asic margin): {@code extract reportingSide -> reportingParty then
     * extract partyId -> identifier only-element}. After #286's receiver deref, the whole-output
     * disguised chain {@code partyId -> identifier only-element} re-roots to recover
     * {@code FieldWithMetaString}, hoisting {@code final FieldWithMetaString … = …;} +
     * {@code output = ….getValue();}.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void counterparty1_asic_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/asic/rewrite/margin/reports/Counterparty1Rule.java");
    }

    /** Flip — Counterparty2Rule (asic margin): the reportingCounterparty sibling carrier. */
    @Test
    @EnabledIf("drrCellAvailable")
    void counterparty2_asic_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/asic/rewrite/margin/reports/Counterparty2Rule.java");
    }

    /** Flip — Counterparty2Rule (esma margin): a different regime, same disguised-output shape. */
    @Test
    @EnabledIf("drrCellAvailable")
    void counterparty2_esma_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/esma/emir/refit/margin/reports/Counterparty2Rule.java");
    }

    /** Flip — Counterparty1Rule (mas margin): a third regime, locking the cross-regime breadth. */
    @Test
    @EnabledIf("drrCellAvailable")
    void counterparty1_mas_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/mas/rewrite/margin/reports/Counterparty1Rule.java");
    }

    // ==== Facet-boundary locks (revert-RED): the single-output gate + the compounding. ====

    /**
     * Multi-output lock — OriginalSwapUTIRule (common trade/link) is a MULTI output
     * ({@code List<String>}: golden's terminal is {@code .mapSingleToList(…)
     * .<String>map("Type coercion", w -> w.getValue()).getMulti()}). Its last then-body
     * {@code assignedIdentifier -> identifier} is a disguised chain WITHOUT a single-collapse and
     * {@code assignedIdentifier} is MULTI — so the single-output wou deref is WRONG and the #287
     * single-output gate DECLINES it (no {@code output = <wrapper>.getValue()} hoist; the
     * {@code output = thenArg3} whole-output assignment is kept). Locks the gate.
     *
     * <p>CONVERTED at PR #331 (the anchor-break-is-signal precedent, #284/#295/#296/#299): the
     * facet existsMetaSeats CP4b {@code multiElemMetaDeref} is EXACTLY the "residual
     * element-wise-deref mechanism" this lock's stays-divergent half documented — the MULTI
     * terminal now derefs ELEMENT-WISE ({@code .<String>map("Type coercion", …).getMulti()}),
     * so the carrier byte-matches golden while the single-output DECLINE half still holds.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void originalSwapUti_multiOutput_singleDeclines_elementWiseDerefByteMatches() throws IOException {
        assertNotNull(drrOutput, "drr generation did not run — corpus unavailable?");
        String path = "drr/regulation/common/trade/link/reports/OriginalSwapUTIRule.java";
        String gen = drrOutput.get(path);
        assertNotNull(gen, "OriginalSwapUTIRule not generated");
        assertTrue(gen.contains("output = thenArg3"),
                "the single-output gate must DECLINE the MULTI-output disguised chain "
                + "(keeping the `output = thenArg3` whole-output assignment — no single wou hoist)");
        String golden = Files.readString(DRR_GOLDEN_DIR.resolve(path));
        assertEquals(normalize(golden), normalize(gen),
                "the MULTI-output carrier byte-matches under the #331 element-wise deref");
    }

    /**
     * Compounding lock — the common-dtcc {@code DTCC_ProductIDRule} is a SINGLE output whose disguised
     * whole-output chain this facet's recovery DOES fire on (the deref inserts
     * {@code final FieldWithMetaString … = thenArg4; … output = ….getValue();}). This carrier was
     * co-occupied with a bare-enum residual ({@code MapperS.of(ISDA)}) at #287, so it stayed divergent;
     * <b>PR #299 (bareEnumQualifyRBodyShadow) lifted that residual</b> ({@code ISDA} binds to a
     * {@code body Authority ISDA} RBody → now qualified {@code TaxonomySourceEnum.ISDA}), so the carrier
     * is now byte-identical to golden — the #287 meta-deref + #299 bare-enum compounding completes it.
     * Locks that the #287 disguised-output deref remains present in the now-clean output.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void dtccProductId_metaDerefFires_nowCleanAfterBareEnum() throws IOException {
        assertNotNull(drrOutput, "drr generation did not run — corpus unavailable?");
        String path = "drr/regulation/common/dtcc/reports/DTCC_ProductIDRule.java";
        String gen = drrOutput.get(path);
        assertNotNull(gen, "common-dtcc DTCC_ProductIDRule not generated");
        assertTrue(gen.contains("= fieldWithMetaString.getValue();"),
                "the #287 disguised-output deref must remain present in the now-clean output");
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
                + path + " (PR #287 disguisedOutputMeta).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
