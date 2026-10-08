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
 * Anchor for facet {@code inlineTernaryBaseThenArgIteHoist} (PR #311): a bare-invokable then whose
 * BASE (k==0) thenArg ARGUMENT is a hoistable SINGLE-cardinality conditional
 * ({@code if <cond> then <value> [else empty]}) — at the {@code renderBareInvokableThenSet} seat —
 * emits the golden {@code ifThenElseResult} block named {@code thenArg} (the base thenArg IS the
 * hoist local) rather than the type-incompatible inline ternary
 * {@code <cond>.getOrDefault(false) ? <value> : MapperC.of()} assigned to a {@code MapperS<X> thenArg}
 * (never compiled — already waivered). Reuses the #257/#289/#301 {@code appendIteHoistChainCore}
 * (arms take the #256 MapperS.of bare-invocation wrap + typed-empty else + MapperC-import drop) via
 * a new {@code forcedName="thenArg"} overload.
 *
 * <p>RULE-scoped ({@code findEnclosingRule}) → FUNCTION-byte-neutral (#232): cdm5 79 / cdm6 232 /
 * drr 206 FUNCTION mismatch UNCHANGED, cdm/iso/fpml POJO byte-IDENTICAL. Green-safe by construction:
 * the inline ternary's type-incompatible arms never compiled, so every carrier was already a
 * waivered mismatch (a green file cannot carry the firing shape).
 *
 * <p>The census predicted ~3 (§5(a) inline_ternary base-thenArg ite-hoist); the byte-oracle refuted
 * it to 2 CLEAN at THIS seat. The 3rd (EventIdentifierType, iosco cde v3) routes through the
 * DISTINCT {@code renderThenExtractSet} base-k==0 seat (a mapSingleToItem-extract then-target, not a
 * bare rule/function) — the cascade-prone path with a session-naming/double-register hazard, DEFERRED
 * to a follow-on PR; PR #316 ({@code schemeNameBaseThenArgIteHoist}) LIFTED it (the
 * {@link #eventIdentifierType_renderThenExtractSetSeat_liftedByPr316} cross-PR lift lock documents it).
 *
 * <p>Anchors are WHOLE-FILE byte comparisons of the generated drr output against the frozen goldens
 * (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}. REVERT-VERIFIED RED 3/5 (w.r.t. the #311 fix) —
 * the 2 UTI flip locks + the positive-content lock fail on a #311-revert; the CentralCounterparty
 * green-safety lock + the EventIdentifierType cross-PR-lift lock (now owned by PR #316) pass either way.
 */
class RuleInlineTernaryBaseThenArgIteHoistTest {

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

    // ==== Flip locks (revert-RED): the 2 carriers now byte-match golden. ====

    /** Flip — UniqueTransactionIdentifierProprietaryRule hkma valuation (base thenArg conditional). */
    @Test
    @EnabledIf("drrCellAvailable")
    void utiProprietary_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/hkma/rewrite/valuation/reports/UniqueTransactionIdentifierProprietaryRule.java");
    }

    /** Flip — UniqueTransactionIdentifierProprietarySchemeNameRule hkma valuation. */
    @Test
    @EnabledIf("drrCellAvailable")
    void utiProprietarySchemeName_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/hkma/rewrite/valuation/reports/UniqueTransactionIdentifierProprietarySchemeNameRule.java");
    }

    // ==== Positive-content lock (revert-RED): the rendered ite-block form, not just byte-match. ====

    /**
     * The base thenArg conditional renders the golden {@code ifThenElseResult} block named
     * {@code thenArg} (typed-empty {@code MapperS.<TradeIdentifier>ofNull()} else), NOT the
     * type-incompatible inline ternary ({@code MapperC.of()} else).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void utiProprietary_rendersIteHoistBlock() {
        String gen = gen(
                "drr/regulation/hkma/rewrite/valuation/reports/UniqueTransactionIdentifierProprietaryRule.java");
        assertTrue(gen.contains("final MapperS<TradeIdentifier> thenArg;\n")
                && gen.contains(
                        "if (notExists(MapperS.of(uniqueTransactionIdentifierRule.evaluate(input))).getOrDefault(false)) {")
                && gen.contains("thenArg = MapperS.of(extract_UTIPropietary.evaluate(input));")
                && gen.contains("thenArg = MapperS.<TradeIdentifier>ofNull();"),
                "Expected the base thenArg conditional to render the ifThenElseResult block named thenArg");
        assertTrue(!gen.contains("getOrDefault(false) ? extract_UTIPropietary.evaluate(input) : MapperC.of()"),
                "The base thenArg must NOT keep the type-incompatible inline ternary");
        assertTrue(!gen.contains("import com.rosetta.model.lib.mapper.MapperC;"),
                "The spurious MapperC import (from the inline ternary's MapperC.of() else) must drop");
    }

    // ==== Green-safety / decline locks. ====

    /**
     * Green-safety (regression-prevention) — CentralCounterpartyRule (asic, GREEN) routes through
     * {@code renderBareInvokableThenSet} with a NON-conditional (plain nav) base thenArg, so the
     * ite-hoist declines ({@code thenArgExpr} is not an {@code RConditionalExpr}) and it STAYS
     * byte-matching golden (the inline {@code final MapperS<X> thenArg = MapperS.of(input)…} decl).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void centralCounterparty_nonConditionalBase_staysGreen() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/asic/rewrite/trade/reports/CentralCounterpartyRule.java");
    }

    /**
     * Cross-PR lift lock — EventIdentifierType (iosco cde v3) reaches the base thenArg conditional
     * through the DISTINCT {@code renderThenExtractSet} seat (its then-target is a
     * {@code mapSingleToItem} extract, not a bare rule/function). PR #311 DEFERRED it (it kept the
     * inline ternary {@code getOrDefault(false) ? MapperS.of(input)… : MapperC.of()}); PR #316
     * ({@code schemeNameBaseThenArgIteHoist}) is the fix that LIFTS it — the base conditional now
     * hoists to the {@code final MapperS<BusinessEvent> thenArg; if/else} block and byte-matches
     * golden (converted from a decline lock, the cross-PR-anchor-break signal). See
     * {@code RuleSchemeNameBaseThenArgIteHoistTest} for the full #316 family.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void eventIdentifierType_renderThenExtractSetSeat_liftedByPr316() throws IOException {
        assertByteMatchesGolden(
                "drr/standards/iosco/cde/version3/event/reports/EventIdentifierTypeRule.java");
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
                + path + " (PR #311 inlineTernaryBaseThenArgIteHoist).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
