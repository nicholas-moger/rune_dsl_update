package com.regnosys.rosetta.generator.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
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
 * PR #350 — the runtime-then restructure compose: 5 byte flips (4 drr FUNCTION + 1 cdm6
 * FUNCTION) + 37 TOWARD movers / 0 AWAY. The GetNextFloatingReferenceResetDate flip lock
 * lives in {@link DerefHoistLadderJoinComposeTest} — its #349 decline lock CONVERTED per
 * its own javadoc promise; this class locks the other 4 + the family content locks + the
 * #351 conversion-ready decline locks.
 *
 * <p><b>F1 — {@code ladderAdmitHandshake}</b>: {@code isCleanLadderContext}'s
 * FUNCTION-path then-ancestor decline becomes TRANSPARENT when that then's body is
 * thenArg-BOUND in the scope chain ({@code bindThenArg} = the chain provably renders
 * hoisted — render-truth, not AST shape; the whole left-nested chain skips together, an
 * outer then binds only at the consumer stage). Scoped to ladders with no nested
 * then/switch in their own subtree (the cp1 Create_AnnaDsbUpiRequest AWAY catch).
 * NotionalAmountLeg1/2 (iosco cde v3) — the last step's ternary renders as golden's
 * block if/return ladder with the typed-empty terminal.
 *
 * <p><b>F2 — {@code multiCondBaseThenArg}</b>: a MULTI conditional BASE feeding a
 * then-chain hoists as golden's MAPPER-TYPED if/else thenArg block
 * ({@code appendIteHoistChainCore} mapperFormArms mode: arms compile INTERIOR and keep
 * the Mapper chain; a SINGLE mapper arm lifts {@code MapperC.of(<chain>)}; a raw single
 * invocable {@code MapperC.of(Collections.singletonList(...))}; a multi fn-call
 * {@code MapperC.<T>of(...)}; mixed meta-join arms deref elementwise; elseless takes
 * {@code MapperC.<T>ofNull()}), admitted at the FUNCTION-path ADD seat via
 * {@code isFunctionMultiCondThenBase}. The render-truth receiver overlay
 * ({@code boundImplicitReceiverType}: a bound-MapperC receiver is MULTI, a bound-LoL
 * receiver takes {@code mapListToItem}) threads through
 * {@code mapMethod}/{@code producesListOfLists}/the lambda position — FUNCTION-path
 * only (the cp4b catch: rule seats moved 6 POJO renders) with the conditional-body
 * descent scoped to the bound seat (the cp4 catch: a global {@code isBodyMulti} descent
 * regressed 150 POJO renders). The implicit evaluate-arg derefs elementwise via the
 * bound-LoL lambda-item channel (the P350B probe pinned the seat:
 * {@code renderImplicitFunctionInvocation}, not the explicit-args loop).
 * GetUnderlierProductIdentifierLeg2 flips; the GetUnderlier family moves deep TOWARD.
 *
 * <p><b>F4 — {@code aliasGeneralValueThenHoist}</b>: the alias seat's #251-M2-deferred
 * GENERAL VALUE then-body admission (any implicit value chain with NO control flow),
 * with the SIGNATURE-keyed collapse ({@code Boolean.FALSE.equals(inferShortcutIsMulti)}
 * — the same walk that renders the signature; a NULLABLE Boolean, the cp5b NPE catch)
 * wrapping a MapperC consumer {@code MapperS.of(<consumer>.get())} / a raw collapse
 * {@code MapperS.of(<render>)}. A NEW-arm render emitting a {@code Mapper*<Object>}
 * decl is DISCARDED whole via the session objectDecl flag on discardable-owner sessions
 * (the cp5c catch: a mid-loop decline stranded partial Franken-renders on legacy
 * carriers). MapPartyChangePayerReceiverModelToCounterparty flips.
 *
 * <p><b>F5 — {@code inLambdaMultiThenHoist}</b>: the #309 n==1 lambda-channel scope
 * widens to EVERY value chain (the #339 per-scope collision-group numbering). The
 * per-level identifiers + deferred tokens are PRE-CREATED before any value compiles —
 * the cp6 catch: an inner-lambda drain during a level's value compile closes the scope
 * (the #346 eager-render law) and the next level's createUniqueIdentifier threw,
 * degrading Beneficiary1/2Rule to the TODO form; pre-creation renders golden's
 * in-lambda thenArg0..3 + {@code return MapperS.of(thenArg3.get())}. 27 drr POJO
 * movers ALL TOWARD. (An F5b all-or-nothing lambda bypass was tried and REVERTED —
 * cp7 moved MapFxOptionToSettlementTerms 23→58 AWAY; the #351 deep-seat ctl
 * restructure owns that class.)
 *
 * <p>Whole-file byte comparisons run through the REAL D11 generation paths and revert
 * RED without the facets. Compile-split (the FRESH 2026-07-06 gate, per-carrier): all 5
 * NON_COMPILING (the runtime {@code .then(} / type-mismatch forms never compiled).
 */
class RuntimeThenRestructureComposeTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    // F1 flip carriers (drr — the iosco cde v3 pair; GetNextFloatingReferenceResetDate's
    // converted lock lives in DerefHoistLadderJoinComposeTest).
    private static final String NOTIONAL_AMOUNT_LEG_1_V3 =
            "drr/standards/iosco/cde/version3/quantity/functions/NotionalAmountLeg1.java";
    private static final String NOTIONAL_AMOUNT_LEG_2_V3 =
            "drr/standards/iosco/cde/version3/quantity/functions/NotionalAmountLeg2.java";
    // F2 flip carrier + the family TOWARD content lock.
    private static final String GET_UNDERLIER_PRODUCT_IDENTIFIER_LEG_2 =
            "drr/regulation/common/functions/GetUnderlierProductIdentifierLeg2.java";
    private static final String GET_UNDERLIER_PRODUCT_IDENTIFIER =
            "drr/regulation/common/functions/GetUnderlierProductIdentifier.java";
    // F4 flip carrier + the TOWARD content lock.
    private static final String MAP_PARTY_CHANGE_PAYER_RECEIVER =
            "cdm/ingest/fpml/confirmation/workflowstep/functions/"
                    + "MapPartyChangePayerReceiverModelToCounterparty.java";
    private static final String MAP_BUYER_SELLER_TO_ACCOUNT_PARTY_REFERENCE =
            "cdm/ingest/fpml/confirmation/party/functions/"
                    + "MapBuyerSellerToAccountPartyReference.java";
    // F5 TOWARD content lock (drr POJO — the in-lambda multi-then form).
    private static final String BENEFICIARY_2_RULE =
            "drr/standards/iosco/cde/version1/party/reports/Beneficiary2Rule.java";
    // #351 DEFERRED carriers (decline locks — the deep-seat ctl restructure converts).
    private static final String MAP_FX_OPTION_TO_SETTLEMENT_TERMS =
            "cdm/ingest/fpml/confirmation/settlement/functions/"
                    + "MapFxOptionToSettlementTerms.java";
    private static final String MAP_ADJUSTABLE_OR_RELATIVE_DATES =
            "cdm/ingest/fpml/confirmation/datetime/functions/"
                    + "MapAdjustableOrRelativeDates.java";

    private static Map<String, String> cdm6FnOutput;
    private static Map<String, String> drrOutput;

    static boolean drrCellAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR);
    }

    static boolean cdm6CellAvailable() {
        return Files.isDirectory(CDM6_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM6_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (cdm6CellAvailable()) {
            cdm6FnOutput = generateFunctions(
                    new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CDM6_CELL_ROOT));
        }
        if (drrCellAvailable()) {
            drrOutput = generateFunctionsAndRules(
                    new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT));
        }
    }

    /** The REAL D11 FUNCTION-kind generation path (function_comparison). */
    private static Map<String, String> generateFunctions(D11CorpusRegressionTest.CellSpec cell)
            throws IOException {
        var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(),
                D11CorpusRegressionTest.emissionFilter(cell));
        var typeUtil = new JavaTypeUtil();
        var funcGen = new FunctionGenerator(gm, new JavaTypeTranslator(typeUtil), typeUtil);
        Map<String, String> output = new LinkedHashMap<>();
        var errors = funcGen.generateWithErrors(output);
        assertTrue(errors.isEmpty(),
                () -> "Function generation reported " + errors.size() + " error(s): " + errors);
        return output;
    }

    /** FUNCTION + Rule generation for the drr cell (the F5 lock is a POJO Rule). */
    private static Map<String, String> generateFunctionsAndRules(
            D11CorpusRegressionTest.CellSpec cell) throws IOException {
        var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(),
                D11CorpusRegressionTest.emissionFilter(cell));
        var typeUtil = new JavaTypeUtil();
        var typeTranslator = new JavaTypeTranslator(typeUtil);
        var funcGen = new FunctionGenerator(gm, typeTranslator, typeUtil);
        var ruleGen = new RuleGenerator(gm, typeTranslator, funcGen);
        var reportGen = new ReportGenerator(gm, typeTranslator, funcGen);
        Map<String, String> output = new LinkedHashMap<>();
        for (RModel model : corpus.workspace().files()) {
            if (gm.shouldGenerate(model)) {
                String version = gm.version(model);
                assertNoGenerationErrors(ruleGen.generateClasses(model, version, output));
                assertNoGenerationErrors(reportGen.generateClasses(model, version, output));
            }
        }
        var errors = funcGen.generateWithErrors(output);
        assertTrue(errors.isEmpty(),
                () -> "Function generation reported " + errors.size() + " error(s): " + errors);
        return output;
    }

    // ------------------------------------------------------------------ F1 byte locks

    /**
     * F1 flip lock: the invocation-based then-chain's last-step ternary renders as the
     * block if/return ladder (the hoisted chain's binding admits the ladder).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void f1_notionalAmountLeg1V3_flips() throws IOException {
        assertByteMatchesGolden(drrOutput, DRR_GOLDEN_DIR, NOTIONAL_AMOUNT_LEG_1_V3);
        String gen = gen(drrOutput, NOTIONAL_AMOUNT_LEG_1_V3);
        assertFalse(gen.contains(".getOrDefault(false) ?"),
                "The inline ternary must be gone (the #350-F1 negative witness)");
    }

    /** F1 flip lock: the Leg2 twin. */
    @Test
    @EnabledIf("drrCellAvailable")
    void f1_notionalAmountLeg2V3_flips() throws IOException {
        assertByteMatchesGolden(drrOutput, DRR_GOLDEN_DIR, NOTIONAL_AMOUNT_LEG_2_V3);
    }

    // ------------------------------------------------------------------ F2 locks

    /**
     * F2 flip lock: the MULTI conditional base hoists mapper-typed, the step ladder
     * renders mapItemToList with the MapperC typed-empty, and the mapListToItem
     * consumption derefs the wrapper elementwise.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void f2_getUnderlierProductIdentifierLeg2_flips() throws IOException {
        assertByteMatchesGolden(drrOutput, DRR_GOLDEN_DIR,
                GET_UNDERLIER_PRODUCT_IDENTIFIER_LEG_2);
        String gen = gen(drrOutput, GET_UNDERLIER_PRODUCT_IDENTIFIER_LEG_2);
        assertTrue(gen.contains(".mapItemToList(item -> {"),
                "The step ladder renders the mapItemToList block (the #350-F2 witness)");
        assertFalse(gen.contains(".then(item -> "),
                "The runtime .then( form must be gone (count 0 in every golden)");
    }

    /**
     * F2 TOWARD content lock: GetUnderlierProductIdentifier (still divergent — the
     * remaining hunks are co-mechanisms) carries the mapper-typed conditional-base
     * hoist verbatim-golden: the MapperC-typed decl, the typed-empty else arm, and the
     * mapItemToList step block.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void f2_getUnderlierProductIdentifier_condBaseHoistsMapperTyped() {
        String gen = gen(drrOutput, GET_UNDERLIER_PRODUCT_IDENTIFIER);
        assertTrue(gen.contains("final MapperC<ReferenceObligation> thenArg0;"),
                "The MULTI conditional base declares the MAPPER-TYPED thenArg (F2)");
        assertTrue(gen.contains("thenArg0 = MapperC.<ReferenceObligation>ofNull();"),
                "The elseless arm takes the MapperC typed-empty (F2)");
        assertTrue(gen.contains(".mapItemToList(item -> {"),
                "The step ladder renders the mapItemToList block (F2 + F1 handshake)");
    }

    // ------------------------------------------------------------------ F4 locks

    /**
     * F4 flip lock: the alias seat's general-value admission hoists thenArg0/thenArg1
     * and the signature-keyed collapse wraps the consumer.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void f4_mapPartyChangePayerReceiver_flips() throws IOException {
        assertByteMatchesGolden(cdm6FnOutput, CDM6_GOLDEN_DIR, MAP_PARTY_CHANGE_PAYER_RECEIVER);
        String gen = gen(cdm6FnOutput, MAP_PARTY_CHANGE_PAYER_RECEIVER);
        assertTrue(gen.contains("final MapperC<Counterparty> thenArg0"),
                "The alias-body chain hoists thenArg0 (the #350-F4 witness)");
        assertFalse(gen.contains(".then(item -> "),
                "The runtime .then( form must be gone (count 0 in every golden)");
    }

    /**
     * F4 content lock: MapBuyerSellerToAccountPartyReference hoists both alias bodies'
     * thenArg0/thenArg1 pairs. (The file FLIPPED at PR #352 — F-e2 defaultAliasArgDeref
     * reduced the last residual, the getOrDefault Mapper arg; these fragments are now
     * golden-verbatim and additionally byte-locked whole-file by
     * MicroResidualNestedThenComposeTest.)
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void f4_mapBuyerSeller_aliasBodiesHoist() {
        String gen = gen(cdm6FnOutput, MAP_BUYER_SELLER_TO_ACCOUNT_PARTY_REFERENCE);
        assertEquals(2, count(gen, "final MapperC<BuyerSellerModel> thenArg0"),
                "Both alias bodies hoist thenArg0 (F4)");
        assertEquals(2, count(gen, "final MapperC<String> thenArg1"),
                "Both alias bodies hoist thenArg1 (F4)");
    }

    // ------------------------------------------------------------------ F5 lock

    /**
     * F5 TOWARD content lock: the rule-body lambda hoists the 4-level chain in-lambda
     * (thenArg0..3, the pre-created identifiers surviving the #346 scope-close law)
     * and collapses the consumer — verbatim-golden fragments; the file stays divergent
     * on co-mechanisms.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void f5_beneficiary2Rule_inLambdaChainHoists() {
        String gen = gen(drrOutput, BENEFICIARY_2_RULE);
        assertTrue(gen.contains("final MapperC<PartyInformation> thenArg0"),
                "The in-lambda chain hoists thenArg0 (the #350-F5 witness)");
        assertTrue(gen.contains("return MapperS.of(thenArg3.get());"),
                "The consumer collapses on the last thenArg (F5)");
        assertFalse(gen.contains("expression compilation error"),
                "The cp6 closed-scope degradation must not recur (F5)");
    }

    // -------------------------------------------------- #351 conversion-ready declines

    /**
     * Restructure lock (pin MOVED at PR #368 — was the #351 deferred-carrier decline
     * lock): MapFxOptionToSettlementTerms' nested in-lambda chains restructure TOGETHER
     * with their method-level control-flow chains in golden. The #350-F5b PARTIAL
     * in-lambda hoist moved it AWAY and was reverted; the PR #368 ctorFieldNestedThenAdmit
     * arm admits the ctor-field nested then-chains so the WHOLE chain restructures in one
     * pass — the file FLIPPED byte-identical (the byte lock lives in
     * {@code CaseNarrowReportFamilyComposeTest}); the runtime {@code .then(} re-appearing
     * means the admission regressed.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void x351_mapFxOptionToSettlementTerms_restructures() {
        // Pin MOVED with the facet (PR #368 ctorFieldNestedThenAdmit): the
        // ctor-field nested-then admission RESTRUCTURES the chain (the file
        // FLIPPED; the byte lock lives in CaseNarrowReportFamilyComposeTest
        // .cdm6Functions_byteMatchGolden).
        String gen = gen(cdm6FnOutput, MAP_FX_OPTION_TO_SETTLEMENT_TERMS);
        assertTrue(!gen.contains(".then(item -> "),
                "The nested chain now restructures (PR #368) — the runtime .then( re-appearing "
                + "means the ctor-field admission regressed");
    }

    /**
     * Restructure lock (pin MOVED at PR #368 — was the #351 deferred-carrier decline
     * lock; the file FLIPPED byte-identical at PR #369): MapAdjustableOrRelativeDates
     * carries a control-flow then-body (a ctor with a conditional setter value) plus a
     * clean sibling behind the all-or-nothing guard. The PR #368 ctor-field admissions
     * restructured BOTH chains, and the PR #369 innerCtorMapperSRoundTrip facet closed
     * the last residual (the inner-ctor {@code MapperS.of(<built>).get()} wrap — the
     * whole-file byte-lock lives in DispatchVariantResolutionComposeTest). This lock
     * stays as the focused restructure witness — {@code .then(} re-appearing means the
     * admissions regressed, independently of the byte anchor.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void x351_mapAdjustableOrRelativeDates_restructures() {
        // Pin MOVED at PR #368 (ctorFieldNestedThenAdmit + ctorCondFieldAdmit
        // restructured the chain); PR #369 (innerCtorMapperSRoundTrip) flipped the
        // file byte-identical — this substring witness stays as the focused
        // restructure lock.
        String gen = gen(cdm6FnOutput, MAP_ADJUSTABLE_OR_RELATIVE_DATES);
        assertTrue(!gen.contains(".then(item -> "),
                "The ctl-body chain now restructures (PR #368) — the runtime .then( re-appearing "
                + "means the ctor-field admissions regressed");
    }

    // ---------------------------------------------------------------- helpers

    private static int count(String haystack, String needle) {
        int n = 0;
        for (int i = haystack.indexOf(needle); i >= 0; i = haystack.indexOf(needle, i + 1)) {
            n++;
        }
        return n;
    }

    private static String gen(Map<String, String> output, String path) {
        assertNotNull(output, "generation did not run — corpus unavailable?");
        String g = output.get(path);
        assertNotNull(g, "Class not generated: " + path);
        return g;
    }

    private static void assertByteMatchesGolden(Map<String, String> output, Path goldenDir,
            String path) throws IOException {
        String gen = gen(output, path);
        Path golden = goldenDir.resolve(path);
        assertTrue(Files.isRegularFile(golden), "Golden missing: " + golden);
        String goldenText = Files.readString(golden).replace("\r\n", "\n");
        assertEquals(goldenText, gen.replace("\r\n", "\n"),
                "Generated bytes must equal the frozen golden: " + path);
    }
}
