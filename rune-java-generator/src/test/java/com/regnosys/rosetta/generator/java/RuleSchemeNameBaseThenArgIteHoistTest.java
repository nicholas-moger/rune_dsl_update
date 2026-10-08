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
 * Anchor for facet {@code schemeNameBaseThenArgIteHoist} (PR #316): the k==0 BASE of a
 * {@code renderThenExtractSet} then-chain ({@code <conditional> then filter then extract}) that is a
 * hoistable elseless SINGLE conditional. The fork compiled the base via
 * {@code ControlFlowHandler.hoistAsItemLocalOrNull} (#181) to the ITEM-typed local
 * {@code <Party> ifThenElseResult = null; if (…) { ifThenElseResult = <value>.get(); }} and then
 * declared {@code final MapperS<Party> thenArg0 = ifThenElseResult;} — a bare {@code Party} assigned to
 * a {@code MapperS<Party>} (TYPE-INCOMPATIBLE, never compiled → already waivered) — OR
 * ({@code EventIdentifierType}) the inline ternary
 * {@code final MapperS<X> thenArg = <cond>.getOrDefault(false) ? <value> : MapperC.of();} (also
 * non-compiling: the then-arm a MapperS value, the else a MapperC). Golden renders the base thenArg AS
 * the MapperS-typed ite-hoist block (the #257/#289/#311 lineage; #311 did the analogue at
 * {@code renderBareInvokableThenSet}):
 * <pre>
 *   final MapperS&lt;Party&gt; thenArg0;
 *   if (…) { thenArg0 = MapperS.of(&lt;value&gt;); } else { thenArg0 = MapperS.&lt;Party&gt;ofNull(); }
 * </pre>
 * {@code renderThenExtractSetImpl}'s new k==0 arm reuses {@code appendIteHoistChainCore} with the
 * thenArg name as {@code forcedName} (the base thenArg IS the hoist local) + a probe-then-render that
 * protects the surviving-thenArg numbering on a decline, recovering the meta itemType from the probe's
 * refs (the #144 wrapper — {@code appendIteHoistChainCore} gains an {@code itemTypeOverride} param).
 *
 * <p>Carriers (6, all NON_COMPILING): {@code BrokerSchemeNameRule} / {@code CentralCounterpartySchemeNameRule}
 * / {@code ClearingMemberSchemeNameRule} / {@code Counterparty2SchemeNameRule} (hkma trade) +
 * {@code Counterparty2SchemeNameRule} (hkma valuation) [the item-hoist PRE-form; Counterparty2 recovers
 * the {@code ReferenceWithMetaParty} meta itemType] + {@code EventIdentifierTypeRule} (iosco cde version3)
 * [the inline-ternary PRE-form, a 1-thenArg {@code thenArg} chain — the byte-oracle surfaced it as the 6th].
 *
 * <p>Green-safe by construction: the fork's {@code MapperS<X> thenArg0 = <bare item>} (or the mixed-type
 * inline ternary) never compiled, so no green file carries the pre-#316 form. RULE-scoped
 * ({@code findEnclosingRule}) → FUNCTION-byte-neutral (#232; cdm5 79 / cdm6 232 / drr 206 FUNCTION mismatch
 * UNCHANGED, cdm/iso/fpml POJO byte-IDENTICAL). A green rule whose base thenArg is a NON-conditional nav
 * ({@code Counterparty1Rule} asic margin) is NOT an {@code RConditionalExpr}, so the arm declines and it
 * stays byte-matching.
 *
 * <p>Anchors are WHOLE-FILE byte comparisons of the generated drr output against the frozen goldens
 * (newline-normalized), generated through the REAL {@link D11CorpusRegressionTest#loadCellCorpusCached}.
 * REVERT-VERIFIED RED 8/10 — the 6 flip locks + the 2 positive-content locks fail on clean source; the
 * non-conditional-base decline lock + the MULTI-conditional-base decline lock (FIX-1) pass either way.
 */
class RuleSchemeNameBaseThenArgIteHoistTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static final String BROKER =
            "drr/regulation/hkma/rewrite/trade/reports/BrokerSchemeNameRule.java";
    private static final String CENTRAL_CCP =
            "drr/regulation/hkma/rewrite/trade/reports/CentralCounterpartySchemeNameRule.java";
    private static final String CLEARING_MEMBER =
            "drr/regulation/hkma/rewrite/trade/reports/ClearingMemberSchemeNameRule.java";
    private static final String COUNTERPARTY2_TRADE =
            "drr/regulation/hkma/rewrite/trade/reports/Counterparty2SchemeNameRule.java";
    private static final String COUNTERPARTY2_VALUATION =
            "drr/regulation/hkma/rewrite/valuation/reports/Counterparty2SchemeNameRule.java";
    private static final String EVENT_IDENTIFIER_TYPE =
            "drr/standards/iosco/cde/version3/event/reports/EventIdentifierTypeRule.java";
    // A GREEN rule whose base thenArg is a NON-conditional nav — the arm declines (base is not an
    // RConditionalExpr), so it stays byte-matching (green-safety / decline lock).
    private static final String COUNTERPARTY1_ASIC_MARGIN =
            "drr/regulation/asic/rewrite/margin/reports/Counterparty1Rule.java";
    // A divergent rule whose base thenArg is a MULTI conditional (golden `MapperC<PriceSchedule>`);
    // the SINGLE gate (`computeRuleBody` thenAware) DECLINES it, so the arm never emits the wrong
    // MapperS block (FIX-1 lock — Seat-1 #316 SHOULD-FIX).
    private static final String STRIKE_PRICE_CURRENCY =
            "drr/standards/iosco/cde/version1/price/reports/StrikePriceCurrencyRule.java";

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

    // ==== Flip locks (revert-RED): the 6 carriers now byte-match golden. ====

    @Test
    @EnabledIf("drrCellAvailable")
    void brokerSchemeName_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(BROKER);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void centralCounterpartySchemeName_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(CENTRAL_CCP);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void clearingMemberSchemeName_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(CLEARING_MEMBER);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void counterparty2SchemeName_trade_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(COUNTERPARTY2_TRADE);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void counterparty2SchemeName_valuation_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(COUNTERPARTY2_VALUATION);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void eventIdentifierType_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(EVENT_IDENTIFIER_TYPE);
    }

    // ==== Positive-content locks (revert-RED): the base-thenArg MapperS-block form. ====

    /**
     * BrokerSchemeName (the item-hoist PRE-form): the base thenArg0 is declared as the MapperS-typed
     * ite-hoist block with a typed-empty {@code MapperS.<Party>ofNull()} else — NOT the fork's
     * type-incompatible {@code Party ifThenElseResult = null; … final MapperS<Party> thenArg0 =
     * ifThenElseResult;}.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void brokerSchemeName_declaresMapperSBlockNotItemHoist() {
        String gen = gen(BROKER);
        assertTrue(gen.contains("final MapperS<Party> thenArg0;"),
                "Expected the base thenArg0 declared as an uninitialized MapperS<Party> ite-hoist block");
        assertTrue(gen.contains("thenArg0 = MapperS.of(extract_BrokerId.evaluate(input));"),
                "Expected the then-arm to wrap the bare invocation MapperS.of(extract_BrokerId.evaluate(input))");
        assertTrue(gen.contains("thenArg0 = MapperS.<Party>ofNull();"),
                "Expected the typed-empty else MapperS.<Party>ofNull()");
        assertTrue(!gen.contains("Party ifThenElseResult = null;"),
                "The fork's item-typed hoist (Party ifThenElseResult = null;) must be gone");
        assertTrue(!gen.contains("final MapperS<Party> thenArg0 = ifThenElseResult;"),
                "The fork's type-incompatible thenArg0 = ifThenElseResult wrap must be gone");
    }

    /**
     * EventIdentifierType (the inline-ternary PRE-form, a 1-thenArg chain named {@code thenArg}): the
     * base thenArg is the MapperS-typed ite-hoist block with the typed-empty
     * {@code MapperS.<BusinessEvent>ofNull()} else — NOT the fork's mixed-type inline ternary ending
     * {@code : MapperC.of();} (which also drops the spurious {@code MapperC} import).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void eventIdentifierType_declaresMapperSBlockNotInlineTernary() {
        String gen = gen(EVENT_IDENTIFIER_TYPE);
        assertTrue(gen.contains("final MapperS<BusinessEvent> thenArg;"),
                "Expected the base thenArg declared as an uninitialized MapperS<BusinessEvent> ite-hoist block");
        assertTrue(gen.contains("thenArg = MapperS.<BusinessEvent>ofNull();"),
                "Expected the typed-empty else MapperS.<BusinessEvent>ofNull()");
        assertTrue(!gen.contains(": MapperC.of();"),
                "The fork's inline-ternary else (: MapperC.of();) must be gone");
        assertTrue(!gen.contains("import com.rosetta.model.lib.mapper.MapperC;"),
                "The spurious MapperC import must drop (the inline-ternary else was its only use)");
    }

    // ==== Green-safety / decline lock (passes on clean source too): a non-conditional base. ====

    /**
     * Green-safety — {@code Counterparty1Rule} (asic margin) has a base thenArg that is a PLAIN nav
     * ({@code final MapperS<ReferenceWithMetaParty> thenArg = MapperS.of(input)…}), NOT an
     * {@code RConditionalExpr}. The #316 arm's {@code base instanceof RConditionalExpr} gate declines
     * it, so the base thenArg is byte-unchanged and the file stays byte-matching golden. Passes on clean
     * source (identical bytes).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void counterparty1_nonConditionalBase_declinesStaysGreen() throws IOException {
        assertByteMatchesGolden(COUNTERPARTY1_ASIC_MARGIN);
    }

    /**
     * FIX-1 lock (Seat-1 #316 SHOULD-FIX; decl pin moved at PR #339, moved AGAIN at PR #365
     * F-D2) — {@code StrikePriceCurrencyRule} (iosco cde v1) has a MULTI conditional base.
     * The #365 ruleMultiCondBaseThenArg arm now renders golden's MAPPER-TYPED if/else block
     * ({@code final MapperC<PriceSchedule> thenArg0; if (…) { thenArg0 = …mapSingleToList(…);
     * } else { thenArg0 = MapperC.<PriceSchedule>ofNull(); }}) — the rule-path MULTI sibling
     * of the #350-F2 FUNCTION-ADD arm; the file byte-matches golden (the whole-file lock
     * lives in the #365 anchor class). The #316 MapperS-block arm still DECLINES the multi
     * base (the SINGLE gate via {@code computeRuleBody} is unchanged), so the
     * wrong-cardinality {@code MapperS} block can never appear — the negative half of the
     * original pin holds verbatim.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void strikePriceCurrency_multiConditionalBase_rendersMapperCBlock() {
        String gen = gen(STRIKE_PRICE_CURRENCY);
        assertTrue(gen.contains("final MapperC<PriceSchedule> thenArg0;"),
                "Multi conditional base must render the #365 MapperC if/else thenArg block"
                + " (the rule-path sibling of the #350-F2 arm)");
        assertTrue(gen.contains("thenArg0 = MapperC.<PriceSchedule>ofNull();"),
                "The block's else arm must carry golden's typed empty"
                + " (MapperC.<PriceSchedule>ofNull())");
        assertTrue(!gen.contains("thenArg0 = MapperS.<PriceSchedule>ofNull();"),
                "The arm must NOT emit the wrong-cardinality MapperS block for a MULTI base");
    }

    private static String gen(String path) {
        assertNotNull(drrOutput, "drr generation did not run — corpus unavailable?");
        String g = drrOutput.get(path);
        assertNotNull(g, "Class not generated: " + path);
        return g;
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
                + path + " (PR #316 schemeNameBaseThenArgIteHoist).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
