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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static com.regnosys.rosetta.testutil.GenerationErrors.assertNoGenerationErrors;

/**
 * Anchor for PR #323's POJO property-specialization law (facet pojoOverrideNaming,
 * GENERATOR-only, parser UNTOUCHED) — the upstream {@code RJavaPojoInterface}
 * {@code addPropertyIfNecessary} law ported to the fork's property model and every
 * {@code ModelObjectGenerator} seat, verified against the 9.83.0 goldens. It flips 18
 * waivered drr POJO files (the override-naming family + LegV1):
 * <ul>
 *   <li><b>Case 0</b> — an annotations-only {@code override} (identical property type AND
 *       requiredness) creates NO property: zero members anywhere (the fork previously
 *       emitted a full phantom member set + {@code setXOverriddenAsX}).</li>
 *   <li><b>Equal-erasure specialization</b> (e.g. requiredness-only {@code (1..1)} over
 *       {@code (0..1)}) — the setter renames {@code setXOverriddenAs<ItemTypeSimpleName>}
 *       (the ITEM TYPE's simple name, never the attribute name) at every setter-name seat
 *       (builder-interface decl, BuilderImpl, setBuilderFields, merge — including
 *       mergeBasic); the getter keeps the plain name with {@code @Override}.</li>
 *   <li><b>Different-erasure specialization</b> — a plain-name overload plus one
 *       {@code @RosettaIgnore}/{@code @RuneIgnore} delegate per ancestor CHAIN node
 *       (nearest-first, instanceof/class.cast downcast), and the builder interface emits
 *       GENERATION-MAJOR segments (super-first recursion: {@code @Override setLeg1(LegV1)}
 *       in the v1 segment … bare {@code setLeg1(CommonLeg)} in the own segment).</li>
 * </ul>
 * Sub-facets in the same PR: the one-space javadoc continuation prefix + the pathed
 * doc-reference {@code  * a -> b} path line (POJO/function javadoc style), with the ENUM
 * javadoc style kept RAW (upstream's enum generator renders its own template — the green
 * cdm6 {@code RatingPriorityResolutionEnum} golden carries the raw form); the
 * {@code Consumer} import keyed on the merge property set (the #309 ListEquals precedent).
 *
 * <p>Anchors are WHOLE-FILE byte comparisons of generated output against the frozen
 * goldens (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}.
 */
class PojoOverrideNamingTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    // Case-0-SOLE flip carrier: all 13 LegV2 overrides are annotations-only — golden
    // carries NO own members (empty getter section, field-less extended Impl/BuilderImpl);
    // the fork previously emitted 13 phantom property sets (48 OverriddenAs tokens).
    private static final String LEGV2 = "drr/standards/iosco/cde/version2/LegV2.java";
    // Case-0 mass-drop flip carrier: 29 annotations-only overrides (comment1 et al.).
    private static final String DTCC_CSA = "drr/regulation/csa/rewrite/dtcc/DTCCAdditionalFields.java";
    // Rename-battery flip carrier: 5 requiredness-only specs → setXOverriddenAs<Type>
    // (String/ZonedDateTime/CommonContractType/CommonAssetClass/Boolean).
    private static final String ASIC = "drr/regulation/asic/rewrite/trade/ASICTransactionReport.java";
    // Chain flip carrier: leg1/leg2 specialize per generation (LegV1→LegV2→Leg→CommonLeg)
    // — 3 ancestor delegates nearest-first + generation-major interface segments + the
    // Case-0 expirationDate drop.
    private static final String COMMON = "drr/regulation/common/trade/CommonTransactionReport.java";
    // Inherited-rename flip carrier: re-implements the 8 renamed setters inherited from
    // CFTCTransactionReport + its own type-changing nonReportable + the Consumer import.
    private static final String PART43 = "drr/regulation/cftc/rewrite/trade/CFTCPart43TransactionReport.java";
    // Interface-segment flip carrier: golden re-declares TWO ancestor generations in the
    // builder interface (the CommonLeg-spec'd periodicPayment chain) + inherited-spec
    // delegates + the PeriodicPayment/RosettaIgnore imports.
    private static final String CSA_LEG = "drr/regulation/csa/rewrite/trade/CSALeg.java";
    // Green-safety pin (drr POJO): a GREEN delegate-carrying type — single-hop
    // different-erasure specs whose direct parent chain the law reproduces byte-exactly.
    private static final String COMMON_LEG_GREEN = "drr/regulation/common/trade/CommonLeg.java";
    // Green-safety pin (cdm6 POJO): the GREEN requiredness-only rename whose capitalized
    // attribute name COINCIDES with its item type's simple name
    // (setCollateralCriteriaOverriddenAsCollateralCriteria) — the law must produce the
    // identical bytes the old attr-name-suffix heuristic accidentally got right.
    private static final String ELIGIBLE_GREEN = "cdm/product/collateral/EligibleCollateralCriteria.java";
    // Green-safety pin (cdm6 ENUM): the enum javadoc style stays RAW (no one-space
    // continuation prefix, no path lines) — this golden's multi-line definition is the
    // carrier that discriminated the two upstream javadoc styles.
    private static final String RATING_ENUM_GREEN = "cdm/product/collateral/RatingPriorityResolutionEnum.java";

    private static Map<String, String> drrOutput;
    private static Map<String, String> cdm6Output;

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
        if (drrCellAvailable()) {
            drrOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT));
        }
        if (cdm6CellAvailable()) {
            cdm6Output = generateCell(
                    new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CDM6_CELL_ROOT));
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
    void legV2_caseZeroSole_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(LEGV2);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void dtccAdditionalFieldsCsa_caseZeroMassDrop_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(DTCC_CSA);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void asicTransactionReport_renameBattery_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(ASIC);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void commonTransactionReport_ancestorChain_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(COMMON);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void cftcPart43_inheritedRenames_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(PART43);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void csaLeg_generationSegments_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(CSA_LEG);
    }

    // ==== Positive-content locks (revert-RED). ====

    /**
     * The rename law: the suffix is the specialized ITEM TYPE's simple name (never the
     * attribute name), applied at the builder-interface decl, setBuilderFields, and BOTH
     * merge branches (mergeBasic included — the fork previously renamed only the model
     * branch); the getter keeps the plain name.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void asic_renamedSetterSeats_typeSimpleNameSuffix() {
        String gen = gen(drrOutput, ASIC);
        assertTrue(gen.contains(
                "ASICTransactionReport.ASICTransactionReportBuilder setTechnicalRecordIdOverriddenAsString(String technicalRecordId);"),
                "Expected the renamed builder-interface decl with the ITEM-TYPE suffix");
        assertTrue(gen.contains(
                "ofNullable(getTechnicalRecordId()).ifPresent(builder::setTechnicalRecordIdOverriddenAsString);"),
                "Expected setBuilderFields to use the renamed setter");
        assertTrue(gen.contains(
                "merger.mergeBasic(getTechnicalRecordId(), o.getTechnicalRecordId(), this::setTechnicalRecordIdOverriddenAsString);"),
                "Expected mergeBasic to use the renamed setter");
        assertFalse(gen.contains("OverriddenAsTechnicalRecordId"),
                "The attr-name-suffix heuristic must not fire");
        assertFalse(gen.contains("getTechnicalRecordIdOverriddenAs"),
                "Getters never rename for a subtype-compatible specialization");
    }

    /**
     * The ancestor chain: one {@code @RosettaIgnore} delegate per specializing ancestor,
     * nearest-first, each downcast-coercing to the MAIN type; the builder interface
     * re-declares each generation's own signature with {@code @Override}; the Case-0
     * {@code expirationDate} override contributes NO members.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void common_chainDelegatesAndCaseZeroDrop() {
        String gen = gen(drrOutput, COMMON);
        int legIdx = gen.indexOf("public CommonTransactionReport.CommonTransactionReportBuilder setLeg1(Leg _leg1) {");
        int legV2Idx = gen.indexOf("public CommonTransactionReport.CommonTransactionReportBuilder setLeg1(LegV2 _leg1) {");
        int legV1Idx = gen.indexOf("public CommonTransactionReport.CommonTransactionReportBuilder setLeg1(LegV1 _leg1) {");
        assertTrue(legIdx > 0 && legV2Idx > legIdx && legV1Idx > legV2Idx,
                "Expected the ancestor delegates nearest-first (Leg, LegV2, LegV1)");
        assertTrue(gen.contains("_leg1 instanceof CommonLeg ? CommonLeg.class.cast(_leg1) : null;"),
                "Expected the downcast coercion to the MAIN type");
        assertTrue(gen.contains("CommonTransactionReport.CommonTransactionReportBuilder setLeg1(LegV1 leg1);"),
                "Expected the v1-generation builder-interface re-declaration");
        assertFalse(gen.contains("setExpirationDateOverriddenAs"),
                "A Case-0 (annotations-only) override must contribute no renamed setter");
        assertFalse(gen.contains("private final Date expirationDate;"),
                "A Case-0 override must contribute no Impl field");
    }

    /**
     * The javadoc sub-facets on a flipped carrier: the pathed doc reference's
     * {@code  * <path>} line and the one-space continuation prefix are covered by the
     * whole-file locks; here pin the Consumer import (merge property set — the fork
     * previously keyed it on implProps and dropped it for Part43's non-extended builder).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void part43_consumerImportAndPathLine() {
        String gen = gen(drrOutput, PART43);
        assertTrue(gen.contains("import java.util.function.Consumer;"),
                "Expected the Consumer import for the non-extended builder's mergeBasic casts");
        assertTrue(gen.contains(" * notionalSchedule\n"),
                "Expected the pathed doc reference's path javadoc line");
    }

    // ==== Green-safety locks (pass on clean source too). ====

    /**
     * A GREEN drr delegate-carrier: CommonLeg's single-hop different-erasure specs (its
     * parent Leg has ONLY Case-0 overrides, so every chain stays one node) must keep the
     * exact bytes the old direct-parent emission produced.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void commonLeg_green_staysByteIdentical() throws IOException {
        assertByteMatchesGolden(drrOutput, DRR_GOLDEN_DIR, COMMON_LEG_GREEN);
    }

    /**
     * The GREEN cdm6 rename-coincidence carrier: attribute {@code collateralCriteria} of
     * item type {@code CollateralCriteria} — the law's type-suffix name equals the old
     * attr-suffix name, so the file must stay byte-identical.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void eligibleCollateralCriteria_green_staysByteIdentical() throws IOException {
        assertByteMatchesGolden(cdm6Output, CDM6_GOLDEN_DIR, ELIGIBLE_GREEN);
    }

    /**
     * The GREEN cdm6 enum-style carrier: enum javadocs keep RAW multi-line continuations
     * (no one-space prefix, no path lines) — prefixing regressed this file, which is how
     * the two upstream javadoc styles were discriminated empirically.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void ratingPriorityResolutionEnum_green_staysByteIdentical() throws IOException {
        assertByteMatchesGolden(cdm6Output, CDM6_GOLDEN_DIR, RATING_ENUM_GREEN);
    }

    // ==== Helpers. ====

    private static String gen(Map<String, String> output, String path) {
        assertNotNull(output, "generation did not run for the cell of " + path);
        String gen = output.get(path);
        assertNotNull(gen, "missing generated file: " + path);
        return gen;
    }

    private void assertByteMatchesGolden(String path) throws IOException {
        assertByteMatchesGolden(drrOutput, DRR_GOLDEN_DIR, path);
    }

    private void assertByteMatchesGolden(Map<String, String> output, Path goldenDir,
            String path) throws IOException {
        String gen = gen(output, path);
        Path goldenPath = goldenDir.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "missing golden: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(gen), "byte mismatch vs golden: " + path);
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
