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
 * PR #281 — facet ladderConditionalBlock: ONE green-safe GENERATOR mechanism, 20 drr POJO Rule byte
 * flips (the user-chosen aim-bigger continuation of #280's bareSymInvoke — the co-occupied residual).
 *
 * <p><b>The mechanism.</b> A nested else-if LADDER map body
 * ({@code extract [item -> if c1 then a1 else if c2 then a2 else …]}) in a MapperS-expecting
 * {@code mapSingleToItem} lambda renders as the golden if/return BLOCK lambda
 * ({@code item -> { if (<c1>) { return <a1>; } … return MapperS.<T>ofNull(); }}) instead of the
 * non-compiling inline nested ternary
 * ({@code c1.getOrDefault(false) ? a1 : c2.getOrDefault(false) ? a2 : … : MapperC.of()}). It is the
 * multi-rung extension of the two single-conditional block forms ({@code compileEffectiveElse-} /
 * {@code compileElselessConditionalBlock}), which both DECLINE the ladder. Each rung reuses the
 * sibling per-arm logic: a bare boolean FUNCTION-call condition HOISTS {@code final Boolean <name> =
 * <call>;} + the {@code (<name> == null ? false : <name>)} guard — numbered {@code _boolean} for a
 * single bare-fn rung or {@code boolean0..n-1} across multiple (ActionType: 8 → boolean0..7); a
 * non-bare-fn condition (ComparisonResult / {@code areEqual} / {@code exists}) keeps the inline
 * {@code <cond>.getOrDefault(false)}; a bare-enum / bare-FUNCTION arm wraps {@code MapperS.of(…)};
 * the terminal empty else re-presents as the typed {@code MapperS.<T>ofNull()} (the synthetic empty
 * {@code MapperC.of()} is never compiled, so its import drops).
 *
 * <p><b>The clean-context gate ({@code isCleanLadderContext}).</b> The conversion fires ONLY where
 * golden renders the chain to the rule identically (so the only divergence is the body's
 * inline→block restructure). It DECLINES every context golden RESTRUCTURES — a co-occupied runtime
 * {@code .then(} chain, a single→multi cardinality lift, or a nested conditional — where the block
 * expansion adds lines that do not align with golden's (different) structure, a within-waiver
 * regression (the #223/#250 line-diff cascade). The gate: rule-scoped (the M7b-3 rule-body seat,
 * FUNCTION-byte-neutral by construction), at most ONE enclosing {@code RExtractExpr} (the ladder's
 * own map), and no enclosing conditional unless it is directly under the rule.
 *
 * <p><b>Green-safe by construction.</b> 0 of the 34,686 goldens carry the inline
 * {@code getOrDefault(false) ?} conditional in a {@code mapSingleToItem} body (563 carry the block
 * form), so every carrier is an already-waivered NON_COMPILING mismatch. byte-oracle / stash-baseline
 * measured exactly 20 flips (drr POJO 2003 → 2023; 12 stale waivers separated out), 0 within-waiver
 * regressions, 43 toward-golden churn; all FUNCTION cells (cdm5 81 / cdm6 236 / drr 207) UNCHANGED.
 *
 * <p>Anchors are WHOLE-FILE byte comparisons of the generated drr Rule output against the frozen
 * goldens (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}. REVERT-VERIFIED RED.
 */
class RuleLadderConditionalBlockTest {

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

    // ==== Flip locks (revert-RED): ladderConditionalBlock carriers now byte-match golden. ====

    /**
     * Flip: the {@code boolean0..7} numbering case — 8 bare boolean FUNCTION-call rungs hoist
     * {@code final Boolean boolean0 = isActionTypeCORR.evaluate(item.get());} … {@code boolean7}
     * (with one inline ComparisonResult MODI rung between, NOT consuming a number) + bare-enum arms
     * wrapped {@code MapperS.of(ActionTypeEnum.CORR)} + terminal {@code MapperS.<ActionTypeEnum>ofNull()}.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void actionType_boolHoistNumbering_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/standards/iosco/cde/version3/event/reports/ActionTypeRule.java");
    }

    /**
     * Flip: an {@code areEqual} (ComparisonResult) condition ladder (no boolHoist) with a conditional
     * ancestor DIRECTLY under the rule (the {@code isCleanLadderContext} conditional-under-rule case).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void confirmed_areEqualLadder_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/standards/iosco/cde/version1/execution/reports/ConfirmedRule.java");
    }

    /** Flip: an {@code areEqual().andNullSafe(areEqual())} multi-arm condition ladder. */
    @Test
    @EnabledIf("drrCellAvailable")
    void jurisdiction_andNullSafeLadder_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/cftc/rewrite/trade/reports/JurisdictionRule.java");
    }

    /** Flip: a simple bare-enum ladder rule body. */
    @Test
    @EnabledIf("drrCellAvailable")
    void assetClass_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/common/trade/contract/reports/AssetClassRule.java");
    }

    /** Flip: a DTCC-wrapped enum ladder rule. */
    @Test
    @EnabledIf("drrCellAvailable")
    void dtccSettlementType_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/common/dtcc/reports/DTCC_SettlementTypeRule.java");
    }

    /** Flip: an iosco cde Spread ladder rule. */
    @Test
    @EnabledIf("drrCellAvailable")
    void spreadLeg1_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/standards/iosco/cde/version1/price/reports/SpreadLeg1Rule.java");
    }

    // ==== Clean-context gate locks (revert-RED for isCleanLadderContext): cascades stay DECLINED. ====

    /**
     * CONVERTED at PR #380 (facet seqThenChainDecomp, D4 — the #329 lock-graduation law;
     * the pre-#380 decline lock's own javadoc named the conversion condition verbatim:
     * "golden hoists the enclosing map to a {@code final MapperC<…> thenArg0 =
     * MapperS.of(input).mapSingleToList(…)}"). The F5 deep-then hoist now ADMITS the
     * rule-path extract-wrapped elseless-ladder BASE ({@code
     * thenChainHasUnhandledControlFlow}'s D4 base arm) and the ladder renders golden's
     * mapSingleToList if/return block INSIDE the hoisted {@code thenArg0} decl — the
     * restructure-window transparency in {@code isCleanLadderContext} (the RULE-path
     * argument-side chain-top read) replaces the {@code ≤ 1 enclosing RExtractExpr}
     * decline this lock pinned. The inline-ternary marker is GONE; the carrier
     * byte-matches golden.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void counterparty2IdentifierType_multiExtract_byteMatchesGolden() throws IOException {
        assertNotNull(drrOutput, "drr generation did not run — corpus unavailable?");
        String gen = drrOutput.get(
                "drr/standards/iosco/cde/version1/party/reports/Counterparty2IdentifierTypeRule.java");
        assertNotNull(gen, "Counterparty2IdentifierTypeRule not generated");
        assertFalse(gen.contains("getOrDefault(false) ? MapperS.of(tradeForEvent.evaluate"),
                "the pre-#380 inline-ternary decline marker must be GONE (the D4 restructure fired)");
        assertByteMatchesGolden(
                "drr/standards/iosco/cde/version1/party/reports/Counterparty2IdentifierTypeRule.java");
    }

    /**
     * CONVERTED at PR #354 (facet blockArmInteriorLadder) — the pre-#354 decline lock's own
     * promise cashed: {@code OptionStyle} (esma)'s inner ladder is an arm of an OUTER
     * conditional that ITSELF renders block-form (the #281 ladder renderer), so the outer's
     * rung arm is a statement seat — {@code compileLadderConditionalBlock} registers its rung
     * chain around each arm compile ({@code pushBlockArmSeatConditionals}) and
     * {@code isCleanLadderContext} treats the REGISTERED conditional ancestor as TRANSPARENT
     * (render truth: an inline-ternary outer is unregistered and keeps the decline — the
     * P352A suppress class; the then-free-path + then/switch-free-subtree conditions scope
     * the admit below the registered seat). The inner {@code MapperC.of()}-terminated inline
     * ternary became golden's if/return block with {@code MapperS.of}-wrapped enum arms +
     * the typed {@code MapperS.<OptionStyleEnum>ofNull()} terminal.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void optionStyle_nestedConditional_blockConverts_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/esma/emir/refit/trade/reports/OptionStyleRule.java");
    }

    /**
     * Gate lock CONVERTED at PR #357 (the #354 pin-conversion pattern — the pre-#357
     * decline lock's own javadoc predicted: "Removing the rule-scope gate converts it →
     * the inline ternary arm becomes a {@code return …;} and this disappears"): the facet
     * inputFormThenHoist's LOCKSTEP predicate widening
     * ({@code thenChainHasUnhandledControlFlow} admits the extract-wrapped ctl-free
     * ladder consumer) lets {@code GetUnderlierProductIdentifier}'s chains hoist, and the
     * #350 bound-then handshake admits the FUNCTION-tail ladders at the bound seats — the
     * inline {@code getOrDefault(false) ? } ternary is GONE (occurrence count 0, matching
     * golden's 0) and the #341/#348 re-rooted arm ({@code item.<Loan>map("getLoan",
     * _product → …)} with the {@code <ReferenceWithMetaProductIdentifier>mapC} trailing
     * witness) now renders as golden's if/return rung. The file moved TOWARD
     * (0.8299 → 0.9515, regscan357); the residual is the co-occupied cardinality family.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void getUnderlierProductIdentifier_function_ladderConverts() {
        assertNotNull(drrOutput, "drr generation did not run — corpus unavailable?");
        String gen = drrOutput.get("drr/regulation/common/functions/GetUnderlierProductIdentifier.java");
        assertNotNull(gen, "GetUnderlierProductIdentifier not generated");
        assertEquals(0, countOccurrences(gen, "getOrDefault(false) ? "),
                "the inline-ternary ladder form is GONE (count 0 in golden too — the "
                + "#357 inputFormThenHoist conversion)");
        assertTrue(gen.contains("return item.<Loan>map(\"getLoan\", _product -> "
                        + "_product.getLoan()).<ReferenceWithMetaProductIdentifier>mapC(\"getProductIdentif"),
                "the re-rooted #341/#348 arm renders as golden's if/return rung");
    }

    private static int countOccurrences(String haystack, String needle) {
        int n = 0;
        for (int i = haystack.indexOf(needle); i >= 0; i = haystack.indexOf(needle, i + 1)) {
            n++;
        }
        return n;
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
                + path + " (PR #281 ladderConditionalBlock).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
