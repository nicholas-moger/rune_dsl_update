package com.regnosys.rosetta.generator.java;

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.java.object.JavaPackageInfoGenerator;
import com.regnosys.rosetta.generator.java.object.validators.CardinalityValidatorGenerator;
import com.regnosys.rosetta.generator.java.object.validators.OnlyExistsValidatorGenerator;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;

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
 * PR #405 — coverage burn-down wave A anchors: the three new families (only-exists
 * validators / cardinality validators / package-info) locked byte-identical against the
 * frozen 9.83.0 goldens, generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached} route. One representative
 * whole-file lock per (family, cell) carrying the family's hardest shapes, plus
 * NEGATIVE WITNESSES — tokens with COUNT 0 IN THE GOLDEN that exactly the reverted
 * pre-#405 defects re-introduce. REVERT-VERIFIED RED.
 *
 * <p>The five audit-measured drift classes these anchors hold shut
 * (fix bundle: the #405 wave-A commit; audit: {@code target-405-audit1..5.log}):
 * <ol>
 *   <li>meta-wrapped attribute casts unwrapped ({@code (Date)} where golden has
 *       {@code (FieldWithMetaDate)}) — the POJO-property-surface rebuild;</li>
 *   <li>{@code choice} types not streamed (10 missing per validator family in cdm6);</li>
 *   <li>model-type list casts missing the wildcard ({@code List<X>} vs
 *       {@code List<? extends X>});</li>
 *   <li>the only-exists in-method blank lines dropped ({@code \t\t} lines — the ST4
 *       {@code <\t>} escape fix; AutoIndentWriter drops whitespace-only literal
 *       indentation);</li>
 *   <li>package-info duplicate (namespace, definition) pairs + missing emission
 *       gating (upstream's LinkedHashMultimap dedupes exact pairs; dependency
 *       closures must not mint files).</li>
 * </ol>
 * Plus the used-only import laws (checkCardinality dropped when the check list is
 * empty; ExistenceChecker dropped for zero-attribute types) and the #306 collision
 * law on the subject type (iso {@code Error}: no data-class import, FQN at every
 * TYPE position, simple name in the success/failure string literals).
 */
class WaveAFamilyAnchorTest {

    private static final JavaTypeUtil TYPE_UTIL = new JavaTypeUtil();
    private static final JavaTypeTranslator TYPE_TRANSLATOR = new JavaTypeTranslator(TYPE_UTIL);

    private static final Map<String, D11CorpusRegressionTest.CellSpec> CELLS = Map.of(
            "cdm5", new D11CorpusRegressionTest.CellSpec("cdm", "5.38.0",
                    Path.of("../test-corpus/cdm/cdm-5.38.0")),
            "cdm6", new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6",
                    Path.of("../test-corpus/cdm/cdm-6.20.6")),
            "drr", new D11CorpusRegressionTest.CellSpec("drr", "6.34.1",
                    Path.of("../test-corpus/drr/drr-6.34.1")),
            "iso", new D11CorpusRegressionTest.CellSpec("iso20022", "1.38.0",
                    Path.of("../test-corpus/iso20022/iso20022-1.38.0")),
            "fpml", new D11CorpusRegressionTest.CellSpec("rune-fpml", "2.0.0",
                    Path.of("../test-corpus/rune-fpml/rune-fpml-2.0.0")));

    /** Per cell key: {family key → output map}. Families: "exists", "card", "pkg". */
    private static final Map<String, Map<String, Map<String, String>>> OUTPUTS = new LinkedHashMap<>();

    static boolean allCellsAvailable() {
        return CELLS.values().stream().allMatch(D11CorpusRegressionTest::cellGoldensExist);
    }

    @BeforeAll
    static void generateAll() throws IOException {
        if (!allCellsAvailable()) {
            return;
        }
        var d11 = new D11CorpusRegressionTest();
        for (var e : CELLS.entrySet()) {
            var cell = e.getValue();
            var corpus = d11.loadCellCorpusCached(cell);
            var gm = new GeneratorModel(corpus.workspace(),
                    D11CorpusRegressionTest.emissionFilter(cell));
            Map<String, String> exists = new LinkedHashMap<>();
            Map<String, String> card = new LinkedHashMap<>();
            var existsGen = new OnlyExistsValidatorGenerator(gm, TYPE_TRANSLATOR, TYPE_UTIL);
            var cardGen = new CardinalityValidatorGenerator(gm, TYPE_TRANSLATOR, TYPE_UTIL);
            for (RModel model : corpus.workspace().files()) {
                if (gm.shouldGenerate(model)) {
                    assertNoGenerationErrors(existsGen.generateClasses(model, gm.version(model), exists));
                    assertNoGenerationErrors(cardGen.generateClasses(model, gm.version(model), card));
                }
            }
            Map<String, String> pkg = new LinkedHashMap<>();
            new JavaPackageInfoGenerator(gm).generatePackageInfoClasses(pkg);
            Map<String, Map<String, String>> families = new LinkedHashMap<>();
            families.put("exists", exists);
            families.put("card", card);
            families.put("pkg", pkg);
            OUTPUTS.put(e.getKey(), families);
        }
    }

    // ==== ONLY-EXISTS byte locks (one per cell; drift classes 1/2/4 + the used-only law) ====

    /** cdm6, meta-wrapped attrs: {@code (FieldWithMetaDate)} + {@code (ReferenceWithMetaBusinessDayAdjustments)} casts. */
    @Test
    @EnabledIf("allCellsAvailable")
    void onlyExists_cdm6_adjustableDate_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("cdm6", "exists",
                "cdm/base/datetime/validation/exists/AdjustableDateOnlyExistsValidator.java");
    }

    /** cdm6, a {@code choice} type: PascalCase option keys ({@code "Cash"} …) — drift class 2. */
    @Test
    @EnabledIf("allCellsAvailable")
    void onlyExists_cdm6_assetChoice_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("cdm6", "exists",
                "cdm/base/staticdata/asset/common/validation/exists/AssetOnlyExistsValidator.java");
    }

    /** cdm6, a zero-attribute type: the ExistenceChecker import DROPS (the used-only law). */
    @Test
    @EnabledIf("allCellsAvailable")
    void onlyExists_cdm6_masterAgreementBase_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("cdm6", "exists",
                "cdm/legaldocumentation/master/validation/exists/MasterAgreementBaseOnlyExistsValidator.java");
    }

    /** cdm5, list + meta + 0..* attrs ALL present (only-exists includes unbounded). */
    @Test
    @EnabledIf("allCellsAvailable")
    void onlyExists_cdm5_party_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("cdm5", "exists",
                "cdm/base/staticdata/party/validation/exists/PartyOnlyExistsValidator.java");
    }

    /** drr. */
    @Test
    @EnabledIf("allCellsAvailable")
    void onlyExists_drr_enrichmentData_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr", "exists",
                "drr/enrichment/common/validation/exists/EnrichmentDataOnlyExistsValidator.java");
    }

    /** iso, the #306 collision subject ({@code Error}): FQN type positions, no data-class import. */
    @Test
    @EnabledIf("allCellsAvailable")
    void onlyExists_iso_error_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("iso", "exists",
                "iso20022/dtcc/rds/harmonized/validation/exists/ErrorOnlyExistsValidator.java");
    }

    /** fpml. */
    @Test
    @EnabledIf("allCellsAvailable")
    void onlyExists_fpml_accumulatorKnockOut_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("fpml", "exists",
                "fpml/consolidated/accumulator/validation/exists/AccumulatorKnockOutOnlyExistsValidator.java");
    }

    // ==== CARDINALITY byte locks (drift classes 1/2/3 + the empty-list import law) ====

    /** cdm6, the wildcard-list + meta + unbounded-skip carrier ({@code partyId} 1..*, {@code person} 0..* skipped). */
    @Test
    @EnabledIf("allCellsAvailable")
    void cardinality_cdm6_party_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("cdm6", "card",
                "cdm/base/staticdata/party/validation/PartyValidator.java");
    }

    /** cdm6, a {@code choice} type: options check {@code 0, 1}. */
    @Test
    @EnabledIf("allCellsAvailable")
    void cardinality_cdm6_assetChoice_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("cdm6", "card",
                "cdm/base/staticdata/asset/common/validation/AssetValidator.java");
    }

    /** cdm6, data-extends-choice: the child validates the choice's OPTIONS (SpecificAsset extends Asset). */
    @Test
    @EnabledIf("allCellsAvailable")
    void cardinality_cdm6_specificAsset_choiceSuper_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("cdm6", "card",
                "cdm/product/collateral/validation/SpecificAssetValidator.java");
    }

    /** cdm6, an EMPTY check list: {@code checkCardinality} static import DROPS (the used-only law). */
    @Test
    @EnabledIf("allCellsAvailable")
    void cardinality_cdm6_collateralTaxonomyValue_empty_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("cdm6", "card",
                "cdm/base/staticdata/asset/common/validation/CollateralTaxonomyValueValidator.java");
    }

    /** cdm5. */
    @Test
    @EnabledIf("allCellsAvailable")
    void cardinality_cdm5_party_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("cdm5", "card",
                "cdm/base/staticdata/party/validation/PartyValidator.java");
    }

    /** drr. */
    @Test
    @EnabledIf("allCellsAvailable")
    void cardinality_drr_enrichmentData_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr", "card",
                "drr/enrichment/common/validation/EnrichmentDataValidator.java");
    }

    /** iso, the wildcard-list carrier ({@code (List<? extends OrganisationIdentification15Choice__3>)}). */
    @Test
    @EnabledIf("allCellsAvailable")
    void cardinality_iso_tradeCounterpartyReport_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("iso", "card",
                "iso20022/auth030/asic/validation/TradeCounterpartyReport20__1Validator.java");
    }

    /** iso, the #306 collision subject. */
    @Test
    @EnabledIf("allCellsAvailable")
    void cardinality_iso_error_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("iso", "card",
                "iso20022/dtcc/rds/harmonized/validation/ErrorValidator.java");
    }

    /** fpml. */
    @Test
    @EnabledIf("allCellsAvailable")
    void cardinality_fpml_accumulatorKnockOut_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("fpml", "card",
                "fpml/consolidated/accumulator/validation/AccumulatorKnockOutValidator.java");
    }

    // ==== PACKAGE-INFO byte locks (drift class 5; drr emits none — expected=0) ====

    /** cdm5 floatingrate — the ALPHABETICAL-order golden (contrast: cdm6's same namespace is a waived order artifact). */
    @Test
    @EnabledIf("allCellsAvailable")
    void packageInfo_cdm5_floatingrate_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("cdm5", "pkg", "cdm/product/asset/floatingrate/package-info.java");
    }

    /** cdm6. */
    @Test
    @EnabledIf("allCellsAvailable")
    void packageInfo_cdm6_baseDatetime_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("cdm6", "pkg", "cdm/base/datetime/package-info.java");
    }

    /** iso — N same-namespace files sharing one verbatim definition dedupe to ONE description. */
    @Test
    @EnabledIf("allCellsAvailable")
    void packageInfo_iso_auth030Asic_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("iso", "pkg", "iso20022/auth030/asic/package-info.java");
    }

    /** fpml. */
    @Test
    @EnabledIf("allCellsAvailable")
    void packageInfo_fpml_accumulator_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("fpml", "pkg", "fpml/consolidated/accumulator/package-info.java");
    }

    // ==== NEGATIVE WITNESSES (count 0 in golden; a revert re-introduces each) ====

    /**
     * Drift-class-1 witness: the unwrapped meta cast {@code (Date) o.getAdjustedDate()}
     * appears NOWHERE in the golden (count 0 — golden wraps {@code (FieldWithMetaDate)});
     * the pre-#405 generator emitted it.
     */
    @Test
    @EnabledIf("allCellsAvailable")
    void witness_unwrappedMetaCast_absent() {
        String gen = generated("cdm6", "exists",
                "cdm/base/datetime/validation/exists/AdjustableDateOnlyExistsValidator.java");
        assertFalse(gen.contains("(Date) o.getAdjustedDate()"),
                "REVERTED DEFECT: meta-wrapped attribute cast to the bare value type");
        assertTrue(gen.contains("(FieldWithMetaDate) o.getAdjustedDate()"),
                "golden shape: the meta wrapper stays in the cast");
    }

    /**
     * Drift-class-3 witness: the non-wildcard model list cast {@code (List<PartyIdentifier>)}
     * has count 0 in the golden ({@code List<? extends PartyIdentifier>}); the pre-#405
     * ValidatorCheckModel constructor built it.
     */
    @Test
    @EnabledIf("allCellsAvailable")
    void witness_nonWildcardModelListCast_absent() {
        String gen = generated("cdm6", "card",
                "cdm/base/staticdata/party/validation/PartyValidator.java");
        assertFalse(gen.contains("(List<PartyIdentifier>)"),
                "REVERTED DEFECT: model-type list cast without the wildcard");
        assertTrue(gen.contains("(List<? extends PartyIdentifier>)"),
                "golden shape: List<? extends X> for model types");
    }

    /**
     * Drift-class-4 witness: a truly-EMPTY line after {@code .build();} has count 0 in the
     * golden (the line carries {@code \t\t}); AutoIndentWriter dropped it pre-fix.
     */
    @Test
    @EnabledIf("allCellsAvailable")
    void witness_emptyBlankLineAfterBuild_absent() {
        String gen = normalize(generated("cdm6", "exists",
                "cdm/base/datetime/validation/exists/AdjustableDateOnlyExistsValidator.java"));
        assertFalse(gen.contains(".build();\n\n"),
                "REVERTED DEFECT: the in-method blank line lost its \\t\\t");
        assertTrue(gen.contains(".build();\n\t\t\n"),
                "golden shape: the blank line carries two tabs");
    }

    /**
     * Used-only-import witnesses: {@code ExistenceChecker} in a zero-attribute only-exists
     * validator / the {@code checkCardinality} static import in an empty-check cardinality
     * validator — count 0 in the respective goldens.
     */
    @Test
    @EnabledIf("allCellsAvailable")
    void witness_usedOnlyImports_absent() {
        String exists = generated("cdm6", "exists",
                "cdm/legaldocumentation/master/validation/exists/MasterAgreementBaseOnlyExistsValidator.java");
        assertFalse(exists.contains("import com.rosetta.model.lib.validation.ExistenceChecker;"),
                "REVERTED DEFECT: ExistenceChecker imported with zero checks");
        String card = generated("cdm6", "card",
                "cdm/base/staticdata/asset/common/validation/CollateralTaxonomyValueValidator.java");
        assertFalse(card.contains("checkCardinality;"),
                "REVERTED DEFECT: checkCardinality static-imported with zero checks");
    }

    /**
     * Drift-class-5 witness: the duplicated adjacent description block has count 0 in the
     * iso golden (upstream dedupes exact (namespace, definition) pairs — N files share one
     * verbatim definition; ONE description renders).
     */
    @Test
    @EnabledIf("allCellsAvailable")
    void witness_duplicateDescription_absent() {
        String gen = normalize(generated("iso", "pkg", "iso20022/auth030/asic/package-info.java"));
        String desc = "The DerivativesTradeReport message is sent by the report submitting entity";
        int first = gen.indexOf(desc);
        assertTrue(first >= 0, "the shared iso definition renders once");
        assertEquals(-1, gen.indexOf(desc, first + 1),
                "REVERTED DEFECT: the shared (namespace, definition) pair rendered more than once");
    }

    /**
     * Emission-gating witness: the transitive-fpml dependency closure (cdm6 loads
     * rune-fpml 1.5.3 for RESOLUTION only) must not mint package-info files — count 0
     * {@code fpml/**}{@code /package-info.java} in the cdm6 golden tree; the ungated
     * pre-#405 generator emitted 39.
     */
    @Test
    @EnabledIf("allCellsAvailable")
    void witness_dependencyClosurePackageInfo_absent() {
        Map<String, String> pkg = OUTPUTS.get("cdm6").get("pkg");
        assertNotNull(pkg);
        assertTrue(pkg.keySet().stream().noneMatch(p -> p.startsWith("fpml/")),
                "REVERTED DEFECT: resolution-only dependency namespaces minted package-info files");
    }

    // ==== plumbing ====

    private static String generated(String cellKey, String family, String path) {
        Map<String, Map<String, String>> families = OUTPUTS.get(cellKey);
        assertNotNull(families, "generation did not run — corpus unavailable?");
        String gen = families.get(family).get(path);
        assertNotNull(gen, "not generated: " + path + " (" + cellKey + "/" + family + ")");
        return gen;
    }

    private static void assertByteMatchesGolden(String cellKey, String family, String path)
            throws IOException {
        String generated = generated(cellKey, family, path);
        Path goldenPath = D11CorpusRegressionTest.resolveGoldensDir(CELLS.get(cellKey)).resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated output must byte-match the golden (newline-normalized) for "
                + path + " (PR #405 wave A).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
