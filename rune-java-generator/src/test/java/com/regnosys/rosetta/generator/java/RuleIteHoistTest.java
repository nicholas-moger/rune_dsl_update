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
 * PR #257 — facet {@code ruleIteHoist} (rule-body emission, M7b-3; the bigger restructure lever
 * deferred at #256).
 *
 * <p>A conditional EXPRESSION consumed at a rule-body then-chain seat is rendered by the fork as an
 * INLINE TERNARY ending in the non-compiling {@code : MapperC.of().get()} (whole-output) or as
 * {@code final MapperS<T> thenArgN = <ternary>} (intermediate then-body). The golden (upstream
 * rune-dsl 9.83.0) instead HOISTS it to a MapperS-typed statement-form ladder:
 * <pre>
 * final MapperS&lt;X&gt; ifThenElseResult;
 * if (&lt;cond&gt;.getOrDefault(false)) { ifThenElseResult = MapperS.of(&lt;then&gt;); }
 * else if (&lt;cond2&gt;.getOrDefault(false)) { ifThenElseResult = MapperS.of(&lt;then2&gt;); }
 * else { ifThenElseResult = MapperS.&lt;X&gt;ofNull(); }
 * output = ifThenElseResult.get();
 * </pre>
 * This is the RULE-PATH analogue of the #173/#223 ifThenElseResult statement-hoist, applied at the
 * top-level then-chain in the rule output assignment. TWO seats:
 * <ul>
 *   <li><b>Shape A</b> — the LAST then-body is a conditional (possibly a NESTED {@code else if}
 *       chain): {@code FunctionExpressionRenderer.appendThenConditionalBlock} only handled the FLAT
 *       2-branch case (#135/#142); extended to the nested else-if ladder. Carriers (4):
 *       OptionPremiumCurrencyRule (esma/fca), PriceUnitOfMeasureRule (hkma),
 *       UnderlyingIdentificationTypeRule (hkma — a bare-fn + enum-arm nested else-if).</li>
 *   <li><b>Shape B</b> — an INTERMEDIATE then-body is a (flat) conditional, consumed by the next
 *       then ({@code ... then (if cond then A else B) then nextRule}): the
 *       {@code renderThenExtractSet} loop emitted {@code thenArgN = <ternary>}; now it hoists the
 *       conditional to a separate {@code ifThenElseResult} block (re-rooting the next then on it),
 *       which collapses the surviving thenArg group to the bare {@code thenArg}. Carriers: the 14
 *       csa FixedRate / FloatingRate Leg1/2 rules.</li>
 * </ul>
 *
 * <p><b>Green-safe by construction</b> (corpus-verified, frozen 9.83.0 baseline): ZERO of the 34,686
 * goldens carry an inline {@code getOrDefault(false) ? ... : ...} ternary (the #173 invariant), and
 * ZERO green goldens carry the nested-else-if-from-the-fork or the intermediate-{@code ifThenElseResult}
 * form (all 13 nested-else-if + 18 intermediate-conditional goldens are currently waivered
 * mismatches). So the rewrite only ever touches currently-waivered (non-compiling / byte-divergent)
 * output. The FLAT 2-branch then-body conditional (121 green files, e.g. {@code SpreadLeg1Rule}) and
 * the non-conditional multi-thenArg numbering (e.g. asic {@code FixedRateDayCountConventionLeg1Rule})
 * stay byte-identical — see the green-safety locks below.
 *
 * <p>Anchors are WHOLE-FILE byte comparisons of the generated drr POJO output (Rule kind) against
 * the frozen goldens (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}. REVERT-VERIFIED RED.
 */
class RuleIteHoistTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR = DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static Map<String, String> drrPojoOutput;

    static boolean drrCellAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR);
    }

    @BeforeAll
    static void generatePojo() throws IOException {
        if (drrCellAvailable()) {
            drrPojoOutput = generatePojoCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT));
        }
    }

    /**
     * Generate the drr POJO cell (Rule/Report/LabelProvider included), mirroring
     * {@link D11CorpusRegressionTest#pojo_comparison}'s generator wiring — the rule-body Rule
     * classes this facet touches are emitted by {@link RuleGenerator} (which delegates to
     * {@link FunctionGenerator}, the shared expression compiler).
     */
    private static Map<String, String> generatePojoCell(D11CorpusRegressionTest.CellSpec cell)
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
                // Rule-family generators are still converging on body emission (M7b-3); a failure
                // for an already-waivered element is tolerated debt. The anchored carriers below
                // MUST emit + byte-match, so a swallowed failure surfaces as a null lookup in
                // assertByteMatchesGolden.
                assertNoGenerationErrors(ruleGen.generateClasses(model, version, output));
                assertNoGenerationErrors(reportGen.generateClasses(model, version, output));
                assertNoGenerationErrors(labelProviderGen.generateClasses(model, version, output));
            }
        }
        return output;
    }

    // ---- Shape A flip locks (revert-RED): a NESTED else-if last-then-body conditional.

    /** Shape A nested else-if (whole-output): {@code if exists {…} else if areEqual {…} else {ofNull}}. */
    @Test
    @EnabledIf("drrCellAvailable")
    void optionPremiumCurrencyRuleEsma_nestedElseIfHoist_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/esma/emir/refit/trade/reports/OptionPremiumCurrencyRule.java");
    }

    /** Shape A nested else-if (whole-output), cross-region: fca. */
    @Test
    @EnabledIf("drrCellAvailable")
    void optionPremiumCurrencyRuleFca_nestedElseIfHoist_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/fca/ukemir/refit/trade/reports/OptionPremiumCurrencyRule.java");
    }

    /**
     * Shape A nested else-if (whole-output) with a {@code ComparisonResult.ofNullSafe} condition;
     * also drops the now-unused {@code import …MapperC} (the {@code MapperC.of().get()} empty else is
     * replaced by {@code MapperS.<String>ofNull()}).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void priceUnitOfMeasureRuleHkma_nestedElseIfHoistAndMapperCImportDrop_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden("drr/regulation/hkma/rewrite/trade/reports/PriceUnitOfMeasureRule.java");
    }

    /**
     * Shape A nested else-if (whole-output) exercising the #256 arm wraps INSIDE the nested
     * recursion: a bare-FUNCTION first arm ({@code wrapBareInvocationOperand}) + a bare-ENUM arm
     * ({@code wrapEnumRungInMapperSOf}), both wrapped {@code MapperS.of(...)} per rung. Confirms the
     * nested-else chain applies the per-arm wraps at every level, not just the flat case.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void underlyingIdentificationTypeRuleHkma_nestedElseIfBareFnAndEnumArms_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/hkma/rewrite/trade/reports/UnderlyingIdentificationTypeRule.java");
    }

    // ---- Shape B flip locks (revert-RED): an INTERMEDIATE then-body conditional.

    /**
     * Shape B intermediate conditional, NON_COMPILING carrier:
     * {@code filter then (if IsCSAAligned then InterestRateLeg1 else InterestRateLeg2) then InterestRateFixedRate}
     * — the conditional hoists to {@code ifThenElseResult}; the filter thenArg collapses bare.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void fixedRateLeg1RuleCsa_intermediateConditionalHoist_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/csa/rewrite/trade/reports/FixedRateLeg1Rule.java");
    }

    /** Shape B intermediate conditional, COMPILES carrier (byte-only realignment). */
    @Test
    @EnabledIf("drrCellAvailable")
    void fixedRateDayCountConventionLeg1RuleCsa_intermediateConditionalHoist_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/csa/rewrite/trade/reports/FixedRateDayCountConventionLeg1Rule.java");
    }

    /** Shape B intermediate conditional, COMPILES carrier (Floating leg variant). */
    @Test
    @EnabledIf("drrCellAvailable")
    void floatingRatePaymentFrequencyPeriodLeg1RuleCsa_intermediateConditionalHoist_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/csa/rewrite/trade/reports/FloatingRatePaymentFrequencyPeriodLeg1Rule.java");
    }

    // ---- Green-safety locks: forms the fix must NOT perturb.

    /**
     * Green-safety lock (FLAT then-body conditional, present else + toBuilder): {@code SpreadLeg1Rule}
     * (csa) is a GREEN drr Rule whose last then-body is a FLAT 2-branch conditional
     * ({@code if IsCSAAligned then … else …}) already rendered correctly by the pre-existing
     * {@code appendThenConditionalBlock} flat path (decl + {@code if/else} + {@code output =
     * toBuilder(ifThenElseResult.get())}). The nested-else-if extension must keep the FLAT path
     * byte-identical (the 121 green flat carriers).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void spreadLeg1RuleCsa_flatConditionalToBuilder_staysByteIdentical() throws IOException {
        assertByteMatchesGolden("drr/regulation/csa/rewrite/trade/reports/SpreadLeg1Rule.java");
    }

    /**
     * Green-safety lock (FLAT elseless then-body conditional, {@code notExists} condition):
     * {@code Direction2Leg1Rule} (asic) is a GREEN drr Rule with a flat {@code if notExists {…} else
     * {ofNull}} hoist — confirms the existing flat-hoist + empty-else path is untouched.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void direction2Leg1RuleAsic_flatElselessHoist_staysByteIdentical() throws IOException {
        assertByteMatchesGolden("drr/regulation/asic/rewrite/trade/reports/Direction2Leg1Rule.java");
    }

    /**
     * Green-safety lock (NON-conditional multi-thenArg numbering): asic
     * {@code FixedRateDayCountConventionLeg1Rule} is a GREEN drr Rule whose then-chain has TWO
     * NON-conditional then-bodies ({@code filter then InterestRateLeg1 then periodicPaymentRule}),
     * so golden numbers them {@code thenArg0}/{@code thenArg1}. The Shape B numbering change (collapse
     * to bare {@code thenArg} when a conditional leaves the group) must NOT perturb a chain with no
     * conditional body — the effective thenArg count equals {@code n}, so the numbering is unchanged.
     * (Note the SAME rule name in the csa cell is a Shape B FLIP carrier above — the contrast
     * is exactly the presence of the {@code if IsCSAAligned} conditional.)
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void fixedRateDayCountConventionLeg1RuleAsic_nonConditionalThenArgNumbering_staysByteIdentical()
            throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/asic/rewrite/trade/reports/FixedRateDayCountConventionLeg1Rule.java");
    }

    /**
     * Cascade-fallback lock (the {@code containsUnhoistedThen} whole-chain render-and-fallback):
     * {@code UnderlyingIdOtherRule} (asic) is a CO-OCCUPIED carrier whose in-lambda conditional arm
     * carries a runtime {@code .then(} chain ({@code … ? getUnderlierProductIdentifier.evaluate(…)
     * .then(item -> …).then(item -> …) : …}) — an un-hoisted then-chain golden hoists into the SAME
     * thenArg group, so its thenArg group is larger than the fork's and applying the ite-hoist would
     * mis-collapse the numbering (the #223 cascade). The fallback must detect the {@code .then(} and
     * re-render with the ite-hoist DISABLED, keeping the pre-#257 inline ternary. This locks that
     * behavior: the generated output must still carry the inline {@code .getOrDefault(false) ? }
     * ternary (NOT a hoisted {@code final MapperS<…> ifThenElseResult;} block) and the triggering
     * {@code .then(}. If a future change broke the fallback, the ite-hoist would fire here and
     * replace the ternary with a (mis-numbered) {@code ifThenElseResult} block — caught by this lock.
     * (The PRE/POST regscan additionally proves the file is byte-identical to clean {@code main}.)
     *
     * <p>NOTE (PR #267): this lock originally pinned {@code PriorUtiRule} (esma). PR #267 opened the
     * {@code renderThenExtractSet} statement-hoist sink for rules (the meta-deref STATEMENT_SINK
     * hoist), which legitimately hoists PriorUti's else-branch then-chain into {@code thenArg} decls —
     * so its iteHoist render no longer carries a residual {@code .then(}, the fallback no longer fires
     * for it, and the ite-hoist correctly moves it CLOSER to golden (45 → 37 diff-lines,
     * improvement-churn, 0 regression). Repointed (PR #267) to {@code UnderlyingIdOtherRule}.
     *
     * <p>NOTE (PR #281): PR #281's {@code ladderConditionalBlock} mechanism (CollectionHandler) is
     * ORTHOGONAL to this ite-hoist fallback — it block-converts a nested else-if ladder
     * {@code mapSingleToItem} body. UnderlyingIdOtherRule's OUTER body is exactly such a ladder, so
     * #281 legitimately block-converts it (the cascade fallback STILL suppresses the ite-hoist — no
     * {@code ifThenElseResult} — but the inline {@code getOrDefault(false) ?} ternary is now a block,
     * moving the file CLOSER to golden, 27 → 19 diff-lines, improvement-churn, 0 regression). The
     * "keeps inline ternary" assertion is therefore no longer observable for it. Repointed to
     * {@code CountryOfCounterparty2Rule} (asic), whose then-seat inline ternary is a SINGLE conditional
     * ({@code … ? … : MapperC.of()}, NOT a nested ladder), so #281's conversion declines it (byte-
     * identical PRE/POST) while the un-hoisted {@code .then(} STILL triggers the ite-hoist fallback —
     * the mechanism is intact, exercised by a different (stable) carrier.
     *
     * <p>NOTE (PR #361): #361's {@code ruleRuntimeThenHoist} (the RULE-path single-conditional
     * then-extract consumer admit) legitimately hoists CountryOfCounterparty2Rule's runtime
     * {@code .then(} into thenArg decls (a content-verified TOWARD mover at cp1, 0.848 → 0.930),
     * so the fallback no longer fires for it — the third repoint of this lock, same law as
     * #267/#281.
     *
     * <p>NOTE (PR #392): #392's {@code ruleMidChainCondLambdaAdmit} legitimately restructures
     * ClearingExceptionsAndExemptionsCounterparty1Rule (csa) WHOLE — byte-identical to golden
     * (the in-lambda mid-chain cond rungs + the bound-pipe MapperC deref) — the FOURTH repoint,
     * same law. Repointed to {@code PTRRIDRule} (common), whose gen still carries the
     * un-hoisted {@code .then(} + the inline {@code .getOrDefault(false) ? } ternary + no
     * {@code ifThenElseResult} post-#392 (verified against f-probe-392post): its chain is the
     * NESTED-mapItem if-return block-lambda class (the EA-trio family), which converts with
     * the in-lambda mapItem-return facet, not the mid-chain rung admit.
     *
     * <p>NOTE (PR #394): #394's {@code ruleFilterPredicateElselessNestedAdmit} +
     * {@code filterPredArmComparandCollapseHoist} legitimately restructure PTRRIDRule WHOLE —
     * byte-identical to golden (the #365 elseless filter-predicate block + the in-branch
     * comparand collapse-hoists + the mapItemToList/flattenList block decomposition; the
     * actual converting facets were the #365 widenings + the named-param extract block,
     * not the #393 mapItem admit the fourth-repoint note anticipated) — the FIFTH repoint,
     * same law. Repointed to {@code DTCC_UnderlyingAssetReportRule} (csa dtcc), the LAST
     * remaining carrier of the exact triple (un-hoisted {@code .then(} ×7 + the inline
     * {@code .getOrDefault(false) ? } ternary ×7 + zero {@code ifThenElseResult}, verified
     * against f-probe-394post): its body is the multi-mechanism block-mapItem family
     * (partial-pays with the PTRRID mechanisms), so this pin GRADUATES with that
     * conversion; if no carrier remains at that point the fallback keeps its unit-level
     * exercise through the co-resident locks and this whole-file lock retires.
     */
    /**
     * <p>NOTE (PR #397): the SIXTH repoint — the graduation-in-progress form pinned the
     * post-cascade content state (zero {@code .then(}, zero ternaries, the ofNull
     * terminal, {@code __thenArg0}).
     *
     * <p>NOTE (PR #398): GRADUATED — the whole-file byte lock (the #340/#383 pattern).
     * facet {@code effElseCtorSetterMetaDeref} (the #354 blockArmSeatConditionals
     * registration around the effective-else ARM compiles + the CtorNavMetaDerefHoist
     * SENTINEL render + the ctor-arm {@code MapperS.of} wraps + the #362-binding-channel
     * consumer recovery) and facet {@code elselessLadderWrapperJoin} (the #381
     * wrapper-preserving join at the ELSELESS DeepThenCondArg ladder) landed the
     * in-lambda META residual: DTCC_UnderlyingAssetReportRule is byte-identical — the
     * LAST drr POJO (the cell EMPTIES; CODEGEN_BODY_GAP ELIMINATES). The #257
     * cascade-fallback MECHANISM keeps its unit-level exercise through the co-resident
     * locks in this class.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void dtccUnderlyingAssetReportRule_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/csa/rewrite/dtcc/reports/DTCC_UnderlyingAssetReportRule.java");
    }

    private static void assertByteMatchesGolden(String path) throws IOException {
        assertNotNull(drrPojoOutput, "drr POJO generation did not run — corpus unavailable?");
        String generated = drrPojoOutput.get(path);
        assertNotNull(generated, "Rule class not generated: " + path
                + " (RuleGenerator emission failed or the path differs)");
        Path goldenPath = DRR_GOLDEN_DIR.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated drr Rule output must byte-match the golden (newline-normalized) for "
                + path + " — a conditional expression at a rule-body then-chain seat (whole-output "
                + "last-then-body or intermediate then-body) must hoist to the MapperS-typed "
                + "ifThenElseResult statement form (facet ruleIteHoist, PR #257).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
