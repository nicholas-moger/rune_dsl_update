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
 * PR #294 — facet blockLambdaNestedThen: ONE green-safe GENERATOR mechanism, 2 drr POJO Rule byte
 * flips (the user-chosen aim-bigger block_lambda play — the NESTED-THEN shape #281 explicitly
 * deferred).
 *
 * <p><b>The mechanism.</b> A NESTED-THEN conditional map body — a conditional whose THEN branch is
 * itself an {@code RConditionalExpr} ({@code extract [item -> if c1 then (if c2 then a else b) [else …]]})
 * in a MapperS-expecting {@code mapSingleToItem} lambda — renders as the golden recursive if/return
 * BLOCK lambda
 * ({@code item -> { if (<c1>) { if (<c2>) { return <a>; } return <b>; } return MapperS.<T>ofNull(); }})
 * instead of the non-compiling inline nested ternary
 * ({@code c1.getOrDefault(false) ? (c2.getOrDefault(false) ? a : b) : MapperC.of()}). It is the
 * then-branch-nesting extension of the three sibling block forms ({@code compileElselessConditionalBlock}
 * / {@code compileEffectiveElseConditionalBlock} / {@code compileLadderConditionalBlock}), ALL of which
 * DECLINE a then-branch that is itself a conditional (and the #281 ladder walk returns null on a
 * nested-THEN rung). A general recursive renderer over the whole conditional tree reuses the sibling
 * per-arm logic: a bare boolean FUNCTION-call condition HOISTS {@code final Boolean <name> = <call>;}
 * + the {@code (<name> == null ? false : <name>)} guard — numbered {@code _boolean} for a single
 * bare-fn condition in the whole tree or {@code boolean0..n-1} across multiple (pre-order: condition,
 * then-subtree, else-subtree); a non-bare-fn condition keeps the inline {@code <cond>.getOrDefault(false)};
 * a bare-enum / bare-FUNCTION arm wraps {@code MapperS.of(…)}; an empty arm re-presents as the typed
 * {@code MapperS.<T>ofNull()}.
 *
 * <p><b>The clean-context gate ({@code isCleanLadderContext}, shared with #281).</b> The conversion
 * fires ONLY where golden renders the chain to the rule identically (so the only divergence is the
 * body's inline→block restructure). It DECLINES every context golden RESTRUCTURES — rule-scoped (the
 * M7b-3 rule-body seat, FUNCTION-byte-neutral by construction), at most ONE enclosing
 * {@code RExtractExpr}, no enclosing conditional unless directly under the rule. The deep-nested
 * ladders (DTCC_OptionType etc.) the byte-clean reverser flagged "single-region" are co-occupied on
 * OTHER mechanisms — a SPIKE relaxing the gate block-converted them but flipped 0 + only added
 * regression risk, confirming the #281 gate.
 *
 * <p><b>Green-safe by construction.</b> 0 of the 34,686 goldens carry the inline
 * {@code getOrDefault(false) ?} ternary in a {@code mapSingleToItem} body, so every carrier is an
 * already-waivered NON_COMPILING mismatch. byte-oracle / stash-baseline measured exactly 2 flips
 * (drr POJO 427 → 425; clean source = 12 stale waivers, comm -23 = the 2 carriers), 0 within-waiver
 * regressions, 3 toward-golden (incl. EffectiveDate, whose block fires correctly but stays divergent
 * on a co-occupied baresym/meta/date-record facet) + 4 neutral; all FUNCTION cells
 * (cdm5 81 / cdm6 236 / drr 207) + cdm/iso/fpml POJO byte-IDENTICAL.
 *
 * <p>Anchors are WHOLE-FILE byte comparisons of the generated drr Rule output against the frozen
 * goldens (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}. REVERT-VERIFIED RED for the 2 flip locks +
 * the EffectiveDate block-fired lock.
 */
class RuleBlockLambdaNestedThenTest {

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

    // ==== Flip locks (revert-RED): blockLambdaNestedThen carriers now byte-match golden. ====

    /**
     * Flip: the PURE nested-THEN case — an outer {@code areEqual} conditional (no boolHoist) whose
     * THEN branch is a nested {@code areEqual} conditional and whose outer else is EMPTY, rendered
     * {@code item -> { if (<c1>) { if (<c2>) { return MapperS.of(…HOUS); } return MapperS.of(…CLIE); }
     * return MapperS.<ClearingAccountOriginEnum>ofNull(); }} — bare-enum arms wrapped
     * {@code MapperS.of(…)}, the outer empty else as the typed-empty terminal.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void clearingAccountOrigin_pureNestedThen_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/common/trade/execution/reports/ClearingAccountOriginRule.java");
    }

    /**
     * Flip: a LADDER whose first rung's THEN is a nested conditional + boolHoist numbering. The top
     * conditional has a bare-fn condition ({@code isCompressed} → {@code boolean0}) whose THEN is the
     * nested {@code if areEqual then PWAS else PWOS} effective-else, and its else continues the ladder
     * with a second bare-fn rung ({@code isPortfolioRebalancing} → {@code boolean1}); two bare-fn
     * conditions in the tree ⇒ {@code boolean0..1}, the {@code areEqual} rung inline. Exercises the
     * recursion's nested-THEN-in-rung + ladder-continuation + boolHoist + bare-enum-arm composition.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void typeOfPtrrTechnique_boolHoistNestedThenLadder_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/common/trade/event/reports/TypeOfPTRRTechniqueRule.java");
    }

    // ==== Toward-golden lock CONVERTED at PR #362: the co-occupied facet landed. ====

    /**
     * Toward-golden lock CONVERTED at PR #362 (the deferred-carrier law — the pre-#362
     * lock's own javadoc named the exact residual: "a {@code TradeForEvent} baresym_resid + a
     * missing FieldWithMetaDate meta-deref + a date-record nav in the arm contents"). The facet
     * nestedTreeMixedMetaJoin lands precisely that: the nested-tree pre-scan reads the
     * positionForEvent date-record arm as BARE evidence beside the {@code tradeForEvent ->
     * tradeDate} baresym META arm (mixed join), the scope flag un-declines the #280/#359
     * conditional-arm meta-leaf gate so the arm fires
     * {@code MapperS.of(tradeForEvent.evaluate(item.get())).<FieldWithMetaDate>map("getTradeDate",
     * trade -> trade.getTradeDate())}, and appendNestedArm derefs it in-arm
     * {@code .<Date>map("Type coercion", …getValue())} — the bare join golden carries. The file
     * is now byte-identical (cp1-362: 1 GONE / 0 NEW / 0 AWAY). Byte-equality here locks the
     * whole composition (the #294 block + the singleton {@code _boolean} boolHoist + the
     * exists-rung ladder + the typed-empty {@code MapperS.<Date>ofNull()} terminal + the
     * mixed-join deref) revert-RED.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void effectiveDate_nestedTreeMixedMetaJoin_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/common/trade/datetime/reports/EffectiveDateRule.java");
    }

    // ==== Gate lock CONVERTED at PR #357: the deep-nested ladder now converts through the
    //      RULE-path bound-then seat (render truth — the chain provably restructures). ====

    /**
     * Gate lock CONVERTED at PR #357 (the #354 pin-conversion pattern — the pre-#357
     * decline lock's own javadoc predicted "Removing the gate … converts it → this
     * inline marker disappears"): the facet inputFormThenHoist's LOCKSTEP predicate
     * widening restructures {@code DTCC_OptionType}'s chains, and the RULE-path
     * bound-then arm of {@code isCleanLadderContext} (RETURN {@code extractCount <= 1}
     * at the thenArg-BOUND seat — render truth, unlike the pre-#357 SPIKE's blanket
     * gate relax, which is why the old "flipped 0 + only regression risk" reasoning no
     * longer applies) admits the ladder at the bound seat — the inline ternary marker
     * is GONE (occurrence count 0, matching golden's 0) and the {@code if (}/{@code
     * return MapperS.of(} rung counts match golden's exactly (6/6). The file moved
     * TOWARD (0.7901 → 0.9462, dl 34 → 10, regscan357 — 0 AWAY corpus-wide); the
     * residual is the co-occupied multi-machinery content the #356 defer catalogued.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void dtccOptionType_deepLadder_converts() {
        assertNotNull(drrOutput, "drr generation did not run — corpus unavailable?");
        String gen = drrOutput.get("drr/regulation/common/dtcc/reports/DTCC_OptionTypeRule.java");
        assertNotNull(gen, "DTCC_OptionTypeRule not generated");
        assertEquals(0, countOccurrences(gen, ".getOrDefault(false) ? MapperS.of(\"Put\")"),
                "the inline-ternary marker is GONE (count 0 in golden too — the #357 "
                + "bound-seat ladder conversion)");
        assertTrue(gen.contains("return MapperS.of(\"Put\");"),
                "the Put arm renders as golden's if/return rung");
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
                + path + " (PR #294 blockLambdaNestedThen).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
