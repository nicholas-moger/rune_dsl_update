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
 * PR #262 — {@code ruleBodyHoistSession} (M7b-3, the first slice of the user-chosen big rule-body
 * cluster-fix): open the {@code StatementHoistSession} on the rule/report emission path.
 *
 * <p><b>The change (3 source files).</b> {@code FunctionGenerator.compileOperations} opened the
 * per-method {@code StatementHoistSession} on the FUNCTION path ONLY
 * ({@code hostClassSimpleNameOverride == null}); the rule/report path passed {@code false} and kept
 * the inline (un-hoisted, frozen) forms — the conservative #172/#173 "rule bytes frozen" gate. But
 * upstream rune-dsl 9.83.0 resolves a rule's hoisted locals through the SAME
 * {@code assignOutputBodyScope} a function uses, so golden rule bodies carry the hoisted forms the
 * fork could not emit while the session stayed closed. Opening the session
 * ({@code hoistSessionEligible = true}) lets golden's rule-body hoists emit across THREE mechanisms
 * at once (each previously frozen for rules):
 * <ul>
 *   <li><b>meta-deref evaluate-arg CSE</b> (the convertNullSafe meta→item deref, #143/#170/#237 at
 *       the rule seat): a meta-wrapper chain passed as a function-call ARG hoists
 *       {@code final ReferenceWithMetaParty referenceWithMetaParty0 = <chain>.get();} (numbered
 *       function-spanning via the session) + passes
 *       {@code (referenceWithMetaParty0 == null ? null : referenceWithMetaParty0.getValue())} — the
 *       STATEMENT_SINK route ({@code ReferenceHandler}) needs a reachable
 *       {@code findStatementHoistSink}, which only a marked session provides. Carriers:
 *       DTCC_TradeParty1IDType, PayerIdentifierLeg1/Leg2, ReceiverIdentifierLeg1/Leg2 (csa).</li>
 *   <li><b>whole-output meta-deref numbering</b> ({@code renderMetaValueDerefOrNull}, #189): a rule
 *       output that is a meta wrapper assigned to a bare-value output hoists
 *       {@code final FieldWithMetaString fieldWithMetaString0 = <chain>; if (… == null) { output =
 *       null; } else { output = ….getValue(); }} — the hoist already fired for rules, but the local
 *       NUMBERING (two derefs in one rule → {@code fieldWithMetaString0}/{@code 1}) needs the
 *       session. Carriers: FXNotionalCurrency, InterestRateNotionalCurrency (iosco/cde).</li>
 *   <li><b>top-level boolean-condition hoist</b> (#179/#260/#261 at a rule-body TOP-LEVEL conditional,
 *       not a lambda): a bare boolean function-call condition hoists
 *       {@code final Boolean _boolean = <call>; if ((_boolean == null ? false : _boolean)) { … }}
 *       — the statement-sink hoist the closed session blocked. Carriers: Cleared, CentralCounterparty,
 *       ClearingMember, SwapLinkID.</li>
 * </ul>
 *
 * <p><b>The leak fix.</b> Opening the session for rules made one latent hazard load-bearing: the
 * {@code renderThenExtractSet} cascade fallback (#257) renders a then-chain twice (once with the
 * ite-hoist, discarded if it carries a runtime {@code .then(} chain), and the per-method session is a FIELD
 * — the discarded first render's {@code thenArg}/{@code ifThenElseResult} registrations would inflate
 * the group counts and renumber the kept render's locals AWAY from golden (every drr Rule cascade
 * carrier double-renders). {@code StatementHoistSession.snapshot()}/{@code restore(Map)} now rolls the
 * discarded registrations back before the re-render — the snapshot/restore the #257 caveat prescribed.
 * Without it the change cascaded 81 within-waiver files (+ numbering desyncs); with it the change is
 * byte-clean (PRE/POST regscan: 0 within-waiver regression).
 *
 * <p><b>Green-safe by construction</b> (corpus-verified, frozen 9.83.0 baseline): the inline
 * (un-hoisted) form a closed-session rule emitted never byte-matched golden (golden always hoists),
 * so every would-be-hoisted rule was already a waivered mismatch — opening the session can only move
 * toward golden, never regress a green rule (full all-kinds gensuite: 0 green regression across all 5
 * cells, FUNCTION cells byte-neutral). The green-safety locks below pin green rules that exercise the
 * now-open session (function-spanning {@code thenArg} numbering + a singleton {@code ifThenElseResult})
 * and must stay byte-identical.
 *
 * <p>Anchors are WHOLE-FILE byte comparisons of the generated drr POJO output (Rule kind) against the
 * frozen goldens (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}. REVERT-VERIFIED RED.
 */
class RuleBodyHoistSessionTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

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
     * {@link D11CorpusRegressionTest#pojo_comparison}'s generator wiring — the rule-body Rule classes
     * this facet touches are emitted by {@link RuleGenerator} (which delegates to
     * {@link FunctionGenerator}, the shared expression compiler whose hoist session this PR opens).
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
                assertNoGenerationErrors(ruleGen.generateClasses(model, version, output));
                assertNoGenerationErrors(reportGen.generateClasses(model, version, output));
                assertNoGenerationErrors(labelProviderGen.generateClasses(model, version, output));
            }
        }
        return output;
    }

    // ==== Flip locks (revert-RED): meta-deref evaluate-arg CSE (STATEMENT_SINK route). ====

    /** if-condition meta-deref arg: {@code naturalPersonBuyerOrSeller.evaluate((referenceWithMetaParty == null ? … ))}. */
    @Test
    @EnabledIf("drrCellAvailable")
    void dtccTradeParty1IdType_metaDerefArg_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/common/dtcc/reports/DTCC_TradeParty1IDTypeRule.java");
    }

    /** conditional-branch SET, two derefs numbered {@code referenceWithMetaParty0}/{@code 1}: csa. */
    @Test
    @EnabledIf("drrCellAvailable")
    void payerIdentifierLeg1_metaDerefArg_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/csa/rewrite/trade/reports/PayerIdentifierLeg1Rule.java");
    }

    /** Leg2 sibling. */
    @Test
    @EnabledIf("drrCellAvailable")
    void payerIdentifierLeg2_metaDerefArg_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/csa/rewrite/trade/reports/PayerIdentifierLeg2Rule.java");
    }

    /** Receiver Leg1. */
    @Test
    @EnabledIf("drrCellAvailable")
    void receiverIdentifierLeg1_metaDerefArg_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/csa/rewrite/trade/reports/ReceiverIdentifierLeg1Rule.java");
    }

    /** Receiver Leg2. */
    @Test
    @EnabledIf("drrCellAvailable")
    void receiverIdentifierLeg2_metaDerefArg_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/csa/rewrite/trade/reports/ReceiverIdentifierLeg2Rule.java");
    }

    // ==== Flip locks (revert-RED): whole-output meta-deref numbering (renderMetaValueDerefOrNull). ====

    /** Two whole-output meta-derefs numbered {@code fieldWithMetaString0}/{@code 1}: iosco/cde. */
    @Test
    @EnabledIf("drrCellAvailable")
    void fxNotionalCurrency_wholeOutputMetaDeref_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/standards/iosco/cde/base/quantity/reports/FXNotionalCurrencyRule.java");
    }

    /** Interest-rate sibling. */
    @Test
    @EnabledIf("drrCellAvailable")
    void interestRateNotionalCurrency_wholeOutputMetaDeref_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/standards/iosco/cde/base/quantity/reports/InterestRateNotionalCurrencyRule.java");
    }

    // ==== Flip locks (revert-RED): top-level boolean-condition hoist (statement-sink). ====

    /** Top-level {@code final Boolean _boolean = isCleared.evaluate(…); if ((_boolean == null ? false : _boolean))}: iosco/cde. */
    @Test
    @EnabledIf("drrCellAvailable")
    void cleared_topLevelBoolHoist_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/standards/iosco/cde/version1/execution/reports/ClearedRule.java");
    }

    /** central-counterparty sibling (isCleared condition). */
    @Test
    @EnabledIf("drrCellAvailable")
    void centralCounterparty_topLevelBoolHoist_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/standards/iosco/cde/version1/execution/reports/CentralCounterpartyRule.java");
    }

    /** clearing-member sibling. */
    @Test
    @EnabledIf("drrCellAvailable")
    void clearingMember_topLevelBoolHoist_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/standards/iosco/cde/version1/execution/reports/ClearingMemberRule.java");
    }

    /** {@code isFXSwap.evaluate(productForEvent.evaluate(input))} + a MapperS import drop: common. */
    @Test
    @EnabledIf("drrCellAvailable")
    void swapLinkId_topLevelBoolHoist_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/common/trade/link/reports/SwapLinkIDRule.java");
    }

    // ==== Green-safety locks: green rules the now-open session must NOT perturb. ====

    /**
     * Green-safety lock (function-spanning {@code thenArg} numbering): {@code CallAmountRule} is a
     * GREEN drr Rule whose single then-chain declares {@code thenArg0}/{@code thenArg1}. Local
     * per-chain naming (closed session) and function-spanning session naming both produce
     * {@code thenArg0}/{@code thenArg1} for a single chain, so opening the session leaves it
     * byte-identical (the flip carriers number ACROSS separate conditional branches, where local
     * naming restarts but the session spans).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void callAmountRule_thenArgSpanning_staysGreen() throws IOException {
        assertByteMatchesGolden("drr/regulation/asic/rewrite/trade/reports/CallAmountRule.java");
    }

    /**
     * Green-safety lock (singleton {@code ifThenElseResult}): {@code Direction2Leg1Rule} is a GREEN
     * drr Rule with a single hoisted {@code ifThenElseResult} conditional. The session resolves a
     * singleton group to the bare base name, identical to the closed-session literal name — byte-
     * identical.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void direction2Leg1Rule_singletonIfThenElseResult_staysGreen() throws IOException {
        assertByteMatchesGolden("drr/regulation/asic/rewrite/trade/reports/Direction2Leg1Rule.java");
    }

    /**
     * Green-safety lock (cftc {@code ifThenElseResult} ladder): {@code EventTypeRule} — a second,
     * cross-regulation green carrier confirming the session-open is byte-neutral on a hoisted
     * conditional rule.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void eventTypeRule_ifThenElseResult_staysGreen() throws IOException {
        assertByteMatchesGolden("drr/regulation/cftc/rewrite/trade/reports/EventTypeRule.java");
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
                + path + " (PR #262 ruleBodyHoistSession).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
