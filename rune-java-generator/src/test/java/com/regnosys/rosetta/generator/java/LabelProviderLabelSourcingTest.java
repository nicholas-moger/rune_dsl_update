package com.regnosys.rosetta.generator.java;

import com.regnosys.rosetta.utils.DeepFeatureCallUtil;
import com.regnosys.rosetta.generator.java.enums.EnumGenerator;
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
 * Anchor for PR #321's LabelProvider label-sourcing family (GENERATOR-only, parser UNTOUCHED) —
 * FOUR coordinated mechanisms in {@code LabelProviderGenerator} that together flip ALL 21
 * waivered drr LabelProviders (every label-content divergence in the family; zero structural):
 *
 * <p><b>M1 — as-clause text</b> ({@code registerLegacyRuleAsLabel}): the legacy rule-as label is
 * the rule's {@code as "..."} clause text ({@code RRule.alias()}; upstream
 * {@code rule.identifier}), NOT the rule name; a rule without an {@code as} clause contributes
 * NO label (the ESMA/FCA margin {@code MICCollateral} drop). <b>M1b — separator blanks</b>
 * ({@code emitClass}): the blank line before each non-start {@code LabelNode} declaration and
 * before each edge block carries the template's two-tab indentation (upstream's Xtend blank
 * template lines) — every multi-node provider is a waivered drr file, every green provider is
 * single-block. <b>M2a — named-root label paths</b> ({@code evaluateAnnotationPathExpression}):
 * a {@code [label for periodicPayment -> x "..."]} named root is a FIRST PATH STEP
 * ({@code root.child(expr.root())}), not the annotation host. <b>M2b — parent-attribute label
 * inheritance</b> ({@code allLabelAnnotations}): an {@code override} attribute inherits the
 * overridden parent attribute's label annotations (parent's first — upstream
 * {@code RAttribute.getAllLabelAnnotations}). <b>M3 — external rule-source traversal</b>
 * (faithful port of upstream {@code RuleReferenceService.traverse} + {@code RulePathMap}): a
 * report's {@code with source X} attaches rule references through the external
 * {@code rule source} declaration ({@code +}/{@code -} attributions, super sources, the
 * supertype-in-source layer), and the traversal semantics gate the as-label map (an associated
 * rule reference or MULTI cardinality terminates descent; pathed references associate at the
 * pointed-to attribute but never contribute as-labels — the pathless-origin guard).
 *
 * <p>Anchors are WHOLE-FILE byte comparisons of the generated drr output against the frozen
 * goldens (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}. REVERT-VERIFIED RED 9/10 — the 6 flip
 * locks + 3 positive-content locks fail on clean source; the green-safety lock
 * (CFTCMarginLabelProvider byte-identity) passes either way.
 */
class LabelProviderLabelSourcingTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    // M1 flip carrier: every label is a rule-as text swap (single-block provider).
    private static final String MAS_VALUATION =
            "drr/regulation/mas/rewrite/valuation/labels/MASValuationLabelProvider.java";
    // M1b flip carrier: multi-node provider — the node/edge separator blank lines carry \t\t.
    private static final String JFSA_MARGIN =
            "drr/regulation/jfsa/rewrite/margin/labels/JFSAMarginLabelProvider.java";
    // M2a+M2b flip carrier: named-root label paths + labels inherited by the
    // `override nonReportable` attribute from the common parent attribute.
    private static final String CFTC_PART43 =
            "drr/regulation/cftc/rewrite/trade/labels/CFTCPart43LabelProvider.java";
    // M3 flip carrier: `report JFSA Trade ... with source JFSARules` — as-labels attached via
    // the external rule source's `+` attributions (e.g. priorUTIProprietary → PriorUtiProprietary).
    private static final String JFSA_TRADE =
            "drr/regulation/jfsa/rewrite/trade/labels/JFSATradeLabelProvider.java";
    // M3 flip carrier: the SECOND rule source on the same report type (CSARulesPPD) — the
    // source-resolution + layering must keep the two CSA trade reports' maps distinct.
    private static final String CSA_PPD =
            "drr/regulation/csa/rewrite/trade/labels/CSAPPDLabelProvider.java";
    // M2a-heavy + M3 flip carrier (ASICRules source + the leg1/leg2 override label batteries).
    private static final String ASIC_TRADE =
            "drr/regulation/asic/rewrite/trade/labels/ASICTradeLabelProvider.java";
    // Green-safety pin: a GREEN provider whose type carries BOTH [ruleReference X] and a plain
    // [label "..."] per attribute — the label annotation overwrites the as-label at the same
    // path (step-3-over-step-2), so the file is byte-identical before AND after M1's text swap.
    private static final String CFTC_MARGIN_GREEN =
            "drr/regulation/cftc/rewrite/margin/labels/CFTCMarginLabelProvider.java";

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
        var enumGen = new EnumGenerator(gm);
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
                assertNoGenerationErrors(enumGen.generateClasses(model, version, output));
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

    // ==== Flip locks (revert-RED): the carriers now byte-match golden. ====

    @Test
    @EnabledIf("drrCellAvailable")
    void masValuationLabelProvider_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(MAS_VALUATION);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void jfsaMarginLabelProvider_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(JFSA_MARGIN);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void cftcPart43LabelProvider_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(CFTC_PART43);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void jfsaTradeLabelProvider_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(JFSA_TRADE);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void csaPpdLabelProvider_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(CSA_PPD);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void asicTradeLabelProvider_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(ASIC_TRADE);
    }

    // ==== Positive-content locks (revert-RED). ====

    /**
     * M1: the as-label carries the rule's {@code as "..."} clause text, never the rule NAME —
     * {@code UniqueTransactionIdentifier}'s
     * {@code as "10 Unique Transaction Identifier (UTI)"} is the label; the fork's pre-#321
     * name-label form ({@code , "UniqueTransactionIdentifier")}) is gone.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void masValuation_asClauseTextNotRuleName() {
        String gen = gen(MAS_VALUATION);
        assertTrue(gen.contains(
                "startNode.addLabel(Arrays.asList(\"uniqueTransactionIdentifier\"), \"10 Unique Transaction Identifier (UTI)\");"),
                "Expected the rule's `as` clause text as the label (M1)");
        assertTrue(!gen.contains(", \"UniqueTransactionIdentifier\");"),
                "The fork's rule-NAME label form must be gone (M1)");
    }

    /**
     * M2a + M2b on one line: the {@code [label for postUpiData "Upi Post-Enrichment Data"]}
     * annotation lives on the COMMON {@code CommonTransactionReport.nonReportable} attribute
     * (inherited by CFTC's {@code override nonReportable} — M2b) and its named root
     * {@code postUpiData} is a path STEP under the attribute (M2a). Plus the ASIC named-root
     * chain {@code [label for periodicPayment -> fixedRateDayCountConvention ...]} on the
     * {@code leg1} override registers the full three-segment path.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void namedRootAndInheritedLabelPaths() {
        String part43 = gen(CFTC_PART43);
        assertTrue(part43.contains(
                "startNode.addLabel(Arrays.asList(\"nonReportable\", \"postUpiData\"), \"Upi Post-Enrichment Data\");"),
                "Expected the parent-inherited named-root label path (M2a + M2b)");
        String asicTrade = gen(ASIC_TRADE);
        assertTrue(asicTrade.contains(
                "startNode.addLabel(Arrays.asList(\"leg1\", \"periodicPayment\", \"fixedRateDayCountConvention\"), \"1.66 Day count convention - Leg 1\");"),
                "Expected the named root as a path step under the annotated attribute (M2a)");
    }

    /**
     * M3: the JFSA Trade report's {@code with source JFSARules} attaches
     * {@code [ruleReference PriorUtiProprietary]} to {@code priorUTIProprietary} via the
     * external rule source's {@code +} attribution — the rule's
     * {@code as "26 Prior UTI (Proprietary)"} becomes the label (no inline annotation exists
     * on the attribute).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void jfsaTrade_ruleSourceAsLabel() {
        String gen = gen(JFSA_TRADE);
        assertTrue(gen.contains(
                "startNode.addLabel(Arrays.asList(\"priorUTIProprietary\"), \"26 Prior UTI (Proprietary)\");"),
                "Expected the rule-source-attached rule's `as` text as the label (M3)");
    }

    // ==== Green-safety lock (passes on clean source too). ====

    /**
     * A GREEN provider stays byte-identical: the CFTC margin type carries BOTH
     * {@code [ruleReference X]} and a plain {@code [label "..."]} on its attributes, so the
     * label annotation overwrites the as-label at the same path (registration step 3 over
     * step 2) both before and after the M1 text swap; the provider is single-block, so M1b's
     * separator blanks never fire; it has no named-root labels (M2a) and the report has no
     * {@code with source} (M3's source is null → inline-only layering).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void cftcMarginLabelProvider_greenStaysByteIdentical() throws IOException {
        assertByteMatchesGolden(CFTC_MARGIN_GREEN);
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
                + path + " (PR #321 LabelProvider label sourcing).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
