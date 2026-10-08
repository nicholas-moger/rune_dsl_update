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
 * PR #286 — facet disguisedChainMetaRecovery: ONE green-safe, RULE-SCOPED (FUNCTION-byte-neutral)
 * GENERATOR mechanism, 10 drr POJO Rule byte flips + compounding. The user-chosen aim-bigger
 * ceiling — the disguised-chain meta-recovery FOUNDATION the #285 close-out deferred (the dominant
 * receiver-deref residual).
 *
 * <p><b>The bug (the #284 receiver-deref family, blocked on the disguised chain).</b> A drr rule
 * body {@code <head> -> <feature> then extract [item -> … item.<X>mapC("getY", …) …]} whose
 * then-ARGUMENT is a DISGUISED 2-name navigation chain ({@code reportingSide -> reportingCounterparty},
 * parsed as an {@code REnumValueRef} rooted on the implicit input) makes the {@code .mapSingleToItem}
 * item a {@code ReferenceWithMetaParty} wrapper; golden inserts
 * {@code item.<Party>map("Type coercion", w -> w == null ? null : w.getValue())} before navigating,
 * while the fork navigated the wrapper-typed receiver directly ({@code item.<X>mapC("getY", …)} —
 * NON_COMPILING). #284's {@code NavigationHandler.implicitItemMetaMapperType} reaches its
 * {@code recoverMetaFromExpr} fallback but that DECLINES, because its {@code terminalNavAttr} →
 * {@code resolveDisguisedFeature} resolves a disguised head only against the enclosing function's
 * INPUT/OUTPUT/shortcut NAMES, never as the FIRST navigation FEATURE of the implicit input.
 *
 * <p><b>The fix (GENERATOR-only, 1 source file).</b> A new {@code recoverDisguisedChainMeta} fallback
 * (after {@code recoverMetaFromExpr} declines) re-roots the disguised chain onto its own
 * extract-lambda item type — the #282 {@code implicitItemDataTypeOrInferred} route: resolve
 * {@code head} as a feature of the item type, {@code leaf} as a feature of {@code head}'s type, and
 * return the leaf's {@code metaNavResultType} wrapper. Gated by {@code metaValueHasNavFeature}: the
 * recovered wrapper's VALUE type must carry the feature THIS receiver navigates next (passed from the
 * enclosing feature call), so a mis-resolution DECLINES rather than inserting a spurious deref.
 * RULE-scoped ({@code findEnclosingRule}) → the FUNCTION tail + cdm/iso/fpml POJO stay byte-IDENTICAL
 * (the #232 shared-seat lesson).
 *
 * <p><b>Green-safe by construction.</b> The fork's wrapper-direct-nav on a {@code ReferenceWithMetaX}
 * does not compile, so every carrier was an already-waivered NON_COMPILING mismatch; inserting the
 * golden deref can only flip a waivered file or move a co-occupied one toward golden. byte-oracle /
 * stash-baseline measured exactly 10 flips (drr POJO 468 → 458; 22 now-matching = 10 mine + 12 stale,
 * clean-main re-dump = exactly the 12 stale), 0 within-waiver regressions / 0 new mismatches / 17
 * toward-golden + 11 neutral churn (the recovery pre-stages the co-occupied disguised-chain meta-deref
 * residual); all FUNCTION cells (cdm5 81 / cdm6 236 / drr 207) + cdm/iso/fpml POJO byte-IDENTICAL.
 *
 * <p>Anchors are WHOLE-FILE byte comparisons of the generated drr output against the frozen goldens
 * (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}. REVERT-VERIFIED RED.
 */
class RuleDisguisedChainMetaRecoveryTest {

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

    // ==== Flip locks (revert-RED): disguisedChainMetaRecovery carriers now byte-match golden. ====

    /**
     * Flip — Counterparty2IdentifierType (esma margin): the {@code reportingSide ->
     * reportingCounterparty} disguised then-argument re-roots to recover {@code ReferenceWithMetaParty},
     * inserting the numbered {@code item.<Party>map("Type coercion", referenceWithMetaParty0/1/2 -> …)}
     * before each {@code getPersonRole}/{@code getPartyId} nav.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void counterparty2IdentifierType_esma_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/esma/emir/refit/margin/reports/Counterparty2IdentifierTypeRule.java");
    }

    /** Flip — Counterparty2IdentifierTypeIndicator (asic valuation): the TypeIndicator sibling. */
    @Test
    @EnabledIf("drrCellAvailable")
    void counterparty2IdentifierTypeIndicator_asic_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/asic/rewrite/valuation/reports/Counterparty2IdentifierTypeIndicatorRule.java");
    }

    /** Flip — Counterparty2IdentifierTypeIndicator (hkma valuation): a different regime, same shape. */
    @Test
    @EnabledIf("drrCellAvailable")
    void counterparty2IdentifierTypeIndicator_hkma_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/hkma/rewrite/valuation/reports/Counterparty2IdentifierTypeIndicatorRule.java");
    }

    /** Flip — Counterparty2IdentifierSource (common): the Source sibling carrier. */
    @Test
    @EnabledIf("drrCellAvailable")
    void counterparty2IdentifierSource_common_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/common/trade/party/reports/Counterparty2IdentifierSourceRule.java");
    }

    // ==== Facet-boundary locks (revert-RED): compounding + scoped decline. ====

    /**
     * CONVERTED at PR #358 (the #330/#357 lock-predicts-its-own-conversion precedent): this
     * was the compounding lock — the deref FIRED but the carrier stayed divergent on "a
     * SEPARATE co-occupied mechanism". That residual mechanism was the in-rung value
     * then-hoist: the #358 F-E ladder-arm blessing (the #339 handshake widened to ctl-free
     * hoistable VALUE chains) lands golden's {@code final MapperC<PersonIdentifierTypeEnum>
     * thenArg0/1/2} rung decls, so the carrier now BYTE-MATCHES golden. The deref witness
     * stays — it is part of the byte-identical file.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void counterparty2IdentifierSource_cftc_metaDerefFires_byteMatchesGolden() throws IOException {
        assertNotNull(drrOutput, "drr generation did not run — corpus unavailable?");
        String path = "drr/regulation/cftc/rewrite/margin/reports/Counterparty2IdentifierSourceRule.java";
        String gen = drrOutput.get(path);
        assertNotNull(gen, "cftc-margin Counterparty2IdentifierSourceRule not generated");
        assertTrue(gen.contains("item.<Party>map(\"Type coercion\""),
                "the disguised-chain meta-deref must fire on this carrier (part of the golden bytes)");
        assertByteMatchesGolden(path);
    }

    /**
     * CONVERTED at PR #330 (the #295/#296 cross-PR-anchor-break precedent): this was the
     * #286 scoped-DECLINE lock — the iosco cde v1 {@code Counterparty2IdentifierTypeRule}
     * deref was "unreachable until the block-lambda + then-hoist facets land first" (its
     * own words: golden DOES want the deref). The #330 recovery extension (the
     * {@code recoverMetaFromExpr} extract-descent + disguised-chain leaf read) IS part of
     * that fix: the {@code item.<Party>map("Type coercion")} deref now FIRES and the
     * carrier moved 0.8507 → 0.9046 toward golden, staying divergent on the residual
     * block-lambda mechanisms — the lock now pins the FIRING + the still-divergent
     * boundary.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void counterparty2IdentifierType_iosco_recoveryFires_byteMatchesGolden() throws IOException {
        assertNotNull(drrOutput, "drr generation did not run — corpus unavailable?");
        String path =
                "drr/standards/iosco/cde/version1/party/reports/Counterparty2IdentifierTypeRule.java";
        String gen = drrOutput.get(path);
        assertNotNull(gen, "iosco cde v1 Counterparty2IdentifierTypeRule not generated");
        assertTrue(gen.contains("item.<Party>map(\"Type coercion\""),
                "the #330-extended recovery must FIRE the deref golden wants here "
                + "(the #286-era decline was lifted by the extract-descent + chain-leaf arms)");
        // CONVERTED at PR #380 (facet seqThenChainDecomp, D4 — the #329 lock-graduation
        // law): the "residual block-lambda mechanisms" this lock pinned as still-divergent
        // LANDED (the F5 base admit for the extract-wrapped elseless ladder + the
        // restructure-window isCleanLadderContext transparency + the filter-predicate
        // bare-item operand deref), so the carrier is now byte-identical.
        String golden = Files.readString(DRR_GOLDEN_DIR.resolve(path));
        assertTrue(normalize(golden).equals(normalize(gen)),
                "the #380 D4 restructure landed — the carrier must byte-match golden");
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
                + path + " (PR #286 disguisedChainMetaRecovery).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
