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
 * PR #284 — facet metaDerefReceiverRule: ONE green-safe, RULE-SCOPED (FUNCTION-byte-neutral)
 * GENERATOR mechanism, 4 drr POJO Rule byte flips + compounding. The user-chosen "ship the clean
 * 4 + compounding foundation" after the byte-oracle refuted the census's ~17 estimate (it counted
 * deref INSERTIONS, not files; several "SOLE" carriers are co-occupied or hit the conservative
 * meta-recovery stop).
 *
 * <p><b>The bug (the #265/#267/#178 meta-deref-receiver family, at the rule-body second-extract
 * seat).</b> A drr rule body {@code <navTerminal> then [item -> … item.<X>mapC("getY", …) …]} whose
 * implicit {@code item} is a {@code ReferenceWithMetaX}/{@code FieldWithMetaX} wrapper: golden
 * inserts {@code item.<Value>map("Type coercion", w -> w == null ? null : w.getValue())} BEFORE
 * navigating the wrapper, while the fork navigated the wrapper-typed receiver directly
 * ({@code item.<X>mapC("getY", …)} — NON_COMPILING, since the wrapper has no {@code getY}). The
 * coercion machinery EXISTS ({@code NavigationHandler.implicitItemMetaMapperType} +
 * {@code ExpressionCompiler.coerceNavigationReceiver}) but DECLINED because the second extract's
 * owning argument is the then-PIPE {@code RImplicitVariable} (not a directly-resolvable feature), so
 * the terminal meta resolution returned null.
 *
 * <p><b>The fix (GENERATOR-only, 1 source file).</b> {@code implicitItemMetaMapperType} gains a
 * RULE-scoped ({@code findEnclosingRule}) fallback after the direct terminal resolution: it follows
 * the then-PIPE {@code RImplicitVariable} to the owning then's ARGUMENT via {@code thenOwnerArgument}
 * and recovers the terminal meta wrapper from that chain with the #264/#270 then-aware walker
 * {@code recoverMetaFromExpr} (whose {@code terminalNavAttr} descends extract bodies +
 * element-preserving list-ops and whose then-chain walk STOPS conservatively at a non-meta
 * terminal). The {@code MapperS} (single) wrap matches the existing arm — the {@code .mapSingleToItem}
 * item is single. RULE-scoped → the FUNCTION tail + cdm/iso/fpml POJO stay byte-IDENTICAL (the #232
 * shared-seat lesson; a THEN-BODY lambda keeps declining at the unchanged gate above — its item is
 * the whole piped value and can be a MapperC, the #180 guard-kind hazard).
 *
 * <p><b>Green-safe by construction.</b> The fork's wrapper-direct-nav ({@code item.<X>mapC("getY", …)}
 * on a {@code ReferenceWithMetaX}) does not compile, so every carrier was an already-waivered
 * NON_COMPILING mismatch; inserting the golden deref can only flip a waivered file or move a
 * co-occupied one toward golden. byte-oracle / regscan measured exactly 4 flips (drr POJO 476 → 472),
 * 0 within-waiver regressions, 6 toward-golden + 33 neutral churn (the deref pre-stages co-occupied
 * carriers like DTCC_Leg / Counterparty2IdentifierType); all FUNCTION cells (cdm5 81 / cdm6 236 /
 * drr 207) + cdm/iso/fpml POJO byte-IDENTICAL (verified by the full gensuite + the multi-cell
 * regscan — the {@code findEnclosingRule} gate keeps the FUNCTION tail frozen).
 *
 * <p>Anchors are WHOLE-FILE byte comparisons of the generated drr output against the frozen goldens
 * (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}. REVERT-VERIFIED RED.
 */
class RuleMetaDerefReceiverTest {

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

    // ==== Flip locks (revert-RED): metaDerefReceiverRule carriers now byte-match golden. ====

    /**
     * Flip — multi-condition meta-deref reroot: the payer-format check navigates
     * {@code item.<NaturalPersonRole>mapC("getPersonRole", …)} and {@code item.<PartyIdentifier>mapC(
     * "getPartyId", …)} off a {@code ReferenceWithMetaParty} item; golden inserts a numbered
     * {@code item.<Party>map("Type coercion", referenceWithMetaParty0/1/2 -> … getValue())} before each.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void otherPaymentPayerFormat_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/hkma/rewrite/trade/reports/OtherPaymentPayerFormatRule.java");
    }

    /** Flip — the receiver-side sibling of {@code OtherPaymentPayerFormatRule}. */
    @Test
    @EnabledIf("drrCellAvailable")
    void otherPaymentReceiverFormat_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/hkma/rewrite/trade/reports/OtherPaymentReceiverFormatRule.java");
    }

    /** Flip — esma SpreadCurrencyOfLeg2: a meta-wrapper item deref before the spread-currency nav. */
    @Test
    @EnabledIf("drrCellAvailable")
    void spreadCurrencyOfLeg2_esma_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/esma/emir/refit/trade/reports/SpreadCurrencyOfLeg2Rule.java");
    }

    /** Flip — the fca ukemir sibling of {@code SpreadCurrencyOfLeg2Rule}. */
    @Test
    @EnabledIf("drrCellAvailable")
    void spreadCurrencyOfLeg2_fca_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/fca/ukemir/refit/trade/reports/SpreadCurrencyOfLeg2Rule.java");
    }

    // ==== Facet-boundary locks (revert-RED): compounding + conservative green-safety. ====

    /**
     * Compounding lock — co-occupied: {@code DTCC_Leg1CommodityInstrumentIDRule} re-roots its meta
     * item correctly (the fix recovers {@code ReferenceWithMetaProductIdentifier} from the then-chain
     * and inserts {@code item.<ProductIdentifier>map("Type coercion", …)}).
     *
     * <p>CONVERTED to a full byte-match at PR #333 (the same #284/#295/#296/#299
     * cross-PR-anchor-break precedent as the {@code DTCC_Leg2} sibling below at #330): the
     * "separate co-occupied mechanism" that held this carrier divergent was the #280
     * meta-leaf baresym decline, lifted by #333's {@code baresymConditionalMetaJoin} (the
     * all-meta conditional join fires the wrapper nav), so the carrier flipped byte-identical.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void dtccLeg1CommodityInstrumentId_metaDerefFires_nowByteMatchesGolden() throws IOException {
        assertNotNull(drrOutput, "drr generation did not run — corpus unavailable?");
        String path = "drr/regulation/common/dtcc/reports/DTCC_Leg1CommodityInstrumentIDRule.java";
        String gen = drrOutput.get(path);
        assertNotNull(gen, "DTCC_Leg1CommodityInstrumentIDRule not generated");
        assertTrue(gen.contains("item.<ProductIdentifier>map(\"Type coercion\""),
                "the meta-deref-receiver must fire on this carrier (compounding toward golden)");
        String golden = Files.readString(DRR_GOLDEN_DIR.resolve(path));
        assertTrue(normalize(golden).equals(normalize(gen)),
                "the carrier byte-matches golden since PR #333 (baresymConditionalMetaJoin)");
    }

    /**
     * Compounding lock #2 (revert-RED) — the {@code DTCC_Leg2CommodityInstrumentIDRule} sibling of the
     * {@code DTCC_Leg1} lock above: #284's {@code recoverMetaFromExpr} fires (inserts
     * {@code item.<ProductIdentifier>map("Type coercion", …)}). CONVERTED to a full byte-match at
     * PR #330 (the #284/#295/#296/#299 cross-PR-anchor-break precedent): the "separate co-occupied
     * mechanism" this lock held the boundary against WAS the recovery walker's disguised-chain
     * blindness — the #330 extract-descent + chain-leaf arms healed it and the carrier FLIPPED.
     * (This lock formerly pinned the asic-margin {@code Counterparty2IdentifierTypeRule}
     * conservative decline, repointed at #286 to this DTCC sibling.)
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void dtccLeg2CommodityInstrumentId_metaDerefFires_byteMatchesGolden() throws IOException {
        assertNotNull(drrOutput, "drr generation did not run — corpus unavailable?");
        String path = "drr/regulation/common/dtcc/reports/DTCC_Leg2CommodityInstrumentIDRule.java";
        String gen = drrOutput.get(path);
        assertNotNull(gen, "DTCC_Leg2CommodityInstrumentIDRule not generated");
        assertTrue(gen.contains("item.<ProductIdentifier>map(\"Type coercion\""),
                "the meta-deref-receiver must fire on this carrier");
        String golden = Files.readString(DRR_GOLDEN_DIR.resolve(path));
        assertEquals(normalize(golden), normalize(gen),
                "the carrier flipped byte-identical at PR #330 (the recovery-extension compounding)");
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
                + path + " (PR #284 metaDerefReceiverRule).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
