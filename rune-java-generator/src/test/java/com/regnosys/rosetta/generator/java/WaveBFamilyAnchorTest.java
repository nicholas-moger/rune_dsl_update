package com.regnosys.rosetta.generator.java;

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.java.object.ModelMetaGenerator;
import com.regnosys.rosetta.generator.java.object.validators.TypeFormatValidatorGenerator;
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
 * PR #407 — coverage burn-down wave B anchors: the two new families (type-format
 * validators / XMeta registries) locked byte-identical against the frozen 9.83.0
 * goldens, generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached} route. One representative
 * whole-file lock per (family, cell) carrying the family's hardest shapes, plus
 * NEGATIVE WITNESSES — tokens with COUNT 0 IN THE GOLDEN that exactly the reverted
 * pre-#407 defects re-introduce. REVERT-VERIFIED RED.
 *
 * <p>The audit-measured drift classes these anchors hold shut
 * (fix bundle: the #407 wave-B commits; audit: {@code target-407-audit1..3.log}):
 * <ol>
 *   <li>alias-carried and inline constraint envelopes dropped (the shared
 *       resolution path returns {@code RAliasType} wrappers the dormant
 *       {@code instanceof} filter never matched, and deliberately discards
 *       string-alias/inline args) — the {@code TypeFormatConstraintScan} rebuild;
 *       the {@code int} alias renders {@code of(0)} fractional digits;</li>
 *   <li>{@code choice} types not streamed by either family (10 missing per family
 *       in cdm6);</li>
 *   <li>the XMeta 5-method shape (the 9.83 goldens carry SEVEN — the
 *       {@code @Deprecated validator()}/{@code typeFormatValidator()} pair);</li>
 *   <li>the XMeta qualify wing absent ({@code BusinessEventMeta}'s 37 entries in
 *       the full-path-sorted walk × declaration order — ordering law 2);</li>
 *   <li>unnamed-condition naming unported (the count-all-named law —
 *       {@code PhysicalSettlementPeriodOneOf2});</li>
 *   <li>the XMeta blank-line bytes (tab/tab/EMPTY/EMPTY/EMPTY/tab separators; an
 *       extra blank line before the version javadoc) + missing declaring-type
 *       imports for inherited condition refs;</li>
 *   <li>the #306 collision law on the subject type (iso {@code Error}: FQN at every
 *       TYPE position incl. {@code @RosettaMeta(model=…)}, no data-class import).</li>
 * </ol>
 */
class WaveBFamilyAnchorTest {

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

    /** Per cell key: {family key → output map}. Families: "typeformat", "xmeta". */
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
            Map<String, String> typeFormat = new LinkedHashMap<>();
            Map<String, String> xmeta = new LinkedHashMap<>();
            // The two-arg surface keeps this class compilable across the
            // revert-RED verification (the pre-#407 generators' constructor shape).
            var typeFormatGen = new TypeFormatValidatorGenerator(gm, TYPE_TRANSLATOR);
            var xmetaGen = new ModelMetaGenerator(gm, TYPE_TRANSLATOR);
            for (RModel model : corpus.workspace().files()) {
                if (gm.shouldGenerate(model)) {
                    assertNoGenerationErrors(typeFormatGen.generateClasses(model, gm.version(model), typeFormat));
                    assertNoGenerationErrors(xmetaGen.generateClasses(model, gm.version(model), xmeta));
                }
            }
            Map<String, Map<String, String>> families = new LinkedHashMap<>();
            families.put("typeformat", typeFormat);
            families.put("xmeta", xmeta);
            OUTPUTS.put(e.getKey(), families);
        }
    }

    // ==== TYPE-FORMAT byte locks (one per cell; drift classes 1/2/7) ====

    /** cdm6, the {@code int} alias carrier: {@code checkNumber(…, empty(), of(0), empty(), empty())}. */
    @Test
    @EnabledIf("allCellsAvailable")
    void typeFormat_cdm6_frequency_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("cdm6", "typeformat",
                "cdm/base/datetime/validation/FrequencyTypeFormatValidator.java");
    }

    /** cdm6, the EMPTY body + the per-symbol used-only static-import law (no checkString/checkNumber/of/empty). */
    @Test
    @EnabledIf("allCellsAvailable")
    void typeFormat_cdm6_party_empty_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("cdm6", "typeformat",
                "cdm/base/staticdata/party/validation/PartyTypeFormatValidator.java");
    }

    /** cdm6, a {@code choice} type — drift class 2 (the dormant generator skipped choices). */
    @Test
    @EnabledIf("allCellsAvailable")
    void typeFormat_cdm6_assetChoice_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("cdm6", "typeformat",
                "cdm/base/staticdata/asset/common/validation/AssetTypeFormatValidator.java");
    }

    /** cdm5, the int carrier again (cross-cell). */
    @Test
    @EnabledIf("allCellsAvailable")
    void typeFormat_cdm5_frequency_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("cdm5", "typeformat",
                "cdm/base/datetime/validation/FrequencyTypeFormatValidator.java");
    }

    /** drr, the INLINE-constraint + pattern-escape carrier (14 checkString/checkNumber entries). */
    @Test
    @EnabledIf("allCellsAvailable")
    void typeFormat_drr_cftcMarginReport_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr", "typeformat",
                "drr/regulation/cftc/rewrite/margin/validation/CFTCMarginReportTypeFormatValidator.java");
    }

    /** iso, the ALIAS-chain envelope carrier: number digits/fractional + BigDecimal min + string pattern. */
    @Test
    @EnabledIf("allCellsAvailable")
    void typeFormat_iso_activeOrHistoric13_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("iso", "typeformat",
                "iso20022/auth030/asic/validation/ActiveOrHistoricCurrencyAnd13DecimalAmountTypeFormatValidator.java");
    }

    /** iso, the #306 collision subject ({@code Error}): FQN type positions, no data-class import. */
    @Test
    @EnabledIf("allCellsAvailable")
    void typeFormat_iso_error_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("iso", "typeformat",
                "iso20022/dtcc/rds/harmonized/validation/ErrorTypeFormatValidator.java");
    }

    /** fpml, the minLength-raw-int + pattern carrier. */
    @Test
    @EnabledIf("allCellsAvailable")
    void typeFormat_fpml_countryCode_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("fpml", "typeformat",
                "fpml/consolidated/shared/validation/CountryCodeTypeFormatValidator.java");
    }

    // ==== XMETA byte locks (drift classes 3/4/5/6/7) ====

    /** cdm6, the qualify ROOT: 37 entries, the walk × declaration order (ordering law 2). */
    @Test
    @EnabledIf("allCellsAvailable")
    void xmeta_cdm6_businessEvent_qualifyRoot_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("cdm6", "xmeta", "cdm/event/common/meta/BusinessEventMeta.java");
    }

    /** cdm6, the SECOND qualify root ({@code isProduct root EconomicTerms}). */
    @Test
    @EnabledIf("allCellsAvailable")
    void xmeta_cdm6_economicTerms_qualifyRoot_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("cdm6", "xmeta", "cdm/product/template/meta/EconomicTermsMeta.java");
    }

    /** cdm6, the count-all-named unnamed-condition witness ({@code OneOf2} at declaration position 0). */
    @Test
    @EnabledIf("allCellsAvailable")
    void xmeta_cdm6_physicalSettlementPeriod_unnamed_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("cdm6", "xmeta",
                "cdm/product/common/settlement/meta/PhysicalSettlementPeriodMeta.java");
    }

    /** cdm6, a {@code choice} type: the implicit {@code AssetChoice} ref. */
    @Test
    @EnabledIf("allCellsAvailable")
    void xmeta_cdm6_assetChoice_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("cdm6", "xmeta", "cdm/base/staticdata/asset/common/meta/AssetMeta.java");
    }

    /** cdm6, data-extends-choice: the child inherits {@code factory.<Asset>create(AssetChoice.class)}. */
    @Test
    @EnabledIf("allCellsAvailable")
    void xmeta_cdm6_specificAsset_choiceSuper_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("cdm6", "xmeta", "cdm/product/collateral/meta/SpecificAssetMeta.java");
    }

    /** cdm6, inherited-condition refs typed to the DECLARING supertype (its import present). */
    @Test
    @EnabledIf("allCellsAvailable")
    void xmeta_cdm6_adjustedRelativeDateOffset_declaringImport_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("cdm6", "xmeta",
                "cdm/base/datetime/meta/AdjustedRelativeDateOffsetMeta.java");
    }

    /** cdm5, the qualify root in the OTHER cdm cell. */
    @Test
    @EnabledIf("allCellsAvailable")
    void xmeta_cdm5_businessEvent_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("cdm5", "xmeta", "cdm/event/common/meta/BusinessEventMeta.java");
    }

    /** drr, a condition-bearing meta (datarule refs present). */
    @Test
    @EnabledIf("allCellsAvailable")
    void xmeta_drr_leiData_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr", "xmeta", "drr/enrichment/lei/meta/LeiDataMeta.java");
    }

    /** iso, the #306 collision subject + the {@code ${project.version}} literal stamp. */
    @Test
    @EnabledIf("allCellsAvailable")
    void xmeta_iso_error_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("iso", "xmeta", "iso20022/dtcc/rds/harmonized/meta/ErrorMeta.java");
    }

    /** fpml. */
    @Test
    @EnabledIf("allCellsAvailable")
    void xmeta_fpml_accumulatorKnockOut_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("fpml", "xmeta",
                "fpml/consolidated/accumulator/meta/AccumulatorKnockOutMeta.java");
    }

    // ==== NEGATIVE WITNESSES (count 0 in golden; a revert re-introduces each) ====

    /**
     * Drift-class-1 witness: the EMPTY check body in {@code FrequencyTypeFormatValidator}
     * has count 0 in the golden (the {@code int} attribute renders a checkNumber with the
     * {@code of(0)} fractional-digits law); the dormant generator — whose
     * {@code instanceof RNumberType} filter never matched the alias-wrapped envelope —
     * emitted the empty body.
     */
    @Test
    @EnabledIf("allCellsAvailable")
    void witness_typeFormat_intEnvelopeDropped_absent() {
        String gen = normalize(generated("cdm6", "typeformat",
                "cdm/base/datetime/validation/FrequencyTypeFormatValidator.java"));
        assertFalse(gen.contains("newArrayList(\n\t\t\t);"),
                "REVERTED DEFECT: the int attribute's constraint envelope dropped (empty check body)");
        assertTrue(gen.contains(
                "checkNumber(\"periodMultiplier\", o.getPeriodMultiplier(), empty(), of(0), empty(), empty())"),
                "golden shape: the int alias renders of(0) fractional digits");
    }

    /**
     * Drift-class-1 witness (string aliases): the constraint-stripped ccy check
     * {@code checkString("ccy", o.getCcy(), 0, empty(), empty())} has count 0 in the iso
     * golden (the alias carries a pattern — {@code of(Pattern.compile("[A-Z]{3,3}"))});
     * the shared resolution path deliberately drops string-alias constraints, so a revert
     * to it re-introduces the stripped shape.
     */
    @Test
    @EnabledIf("allCellsAvailable")
    void witness_typeFormat_stringAliasConstraintDropped_absent() {
        String gen = generated("iso", "typeformat",
                "iso20022/auth030/asic/validation/ActiveOrHistoricCurrencyAnd13DecimalAmountTypeFormatValidator.java");
        assertFalse(gen.contains("checkString(\"ccy\", o.getCcy(), 0, empty(), empty())"),
                "REVERTED DEFECT: the string-alias pattern constraint dropped");
        assertTrue(gen.contains("checkString(\"ccy\", o.getCcy(), 0, empty(), of(Pattern.compile(\"[A-Z]{3,3}\")))"),
                "golden shape: the alias-carried pattern renders");
        assertTrue(gen.contains("of(new BigDecimal(\"0\"))"),
                "golden shape: the alias-carried min bound renders as a BigDecimal");
    }

    /**
     * Drift-class-3 witness: the 5-method XMeta shape — {@code @Deprecated} has count 2
     * in every golden (the no-arg validator()/typeFormatValidator() pair); the vendored
     * generator's 5-method shape emits ZERO.
     */
    @Test
    @EnabledIf("allCellsAvailable")
    void witness_xmeta_deprecatedPair_present() {
        String gen = generated("cdm6", "xmeta", "cdm/base/staticdata/asset/common/meta/AssetMeta.java");
        long deprecatedCount = gen.lines().filter(l -> l.trim().equals("@Deprecated")).count();
        assertEquals(2, deprecatedCount,
                "golden shape: EXACTLY the @Deprecated validator()/typeFormatValidator() no-arg pair");
        assertTrue(gen.contains("public Validator<? super Asset> validator() {"),
                "golden shape: the no-arg validator() overload");
        assertTrue(gen.contains("public Validator<? super Asset> typeFormatValidator() {"),
                "golden shape: the no-arg typeFormatValidator() overload");
    }

    /**
     * Drift-class-4 witness: {@code Collections.emptyList()} has count 0 in the
     * BusinessEventMeta golden (the qualify ROOT lists 37 entries — and drops the
     * {@code java.util.Collections} import entirely); the wing-less dormant generator
     * emitted the empty list for every type.
     */
    @Test
    @EnabledIf("allCellsAvailable")
    void witness_xmeta_qualifyWing_present() {
        String gen = generated("cdm6", "xmeta", "cdm/event/common/meta/BusinessEventMeta.java");
        assertFalse(gen.contains("Collections.emptyList()"),
                "REVERTED DEFECT: the qualify root rendered the empty list (no qualify wing)");
        assertFalse(gen.contains("import java.util.Collections;"),
                "REVERTED DEFECT: the unused Collections import on a qualify root");
        assertTrue(gen.contains("factory.<BusinessEvent>create(Qualify_Repurchase.class),"),
                "golden shape: the FIRST qualify entry (the walk-order pin — event-common-func declaration order)");
        assertTrue(gen.contains("factory.<BusinessEvent>create(Qualify_ValuationUpdate.class)\n"),
                "golden shape: the LAST qualify entry (comma-less)");
    }

    /**
     * Drift-class-5 witness: the dormant index-based fallback name
     * {@code PhysicalSettlementPeriodDataRule0} has count 0 in the golden — the
     * count-all-named law names the unnamed one-of {@code …OneOf2} (2 named conditions
     * on the type; the expression root selects the {@code OneOf} kind).
     */
    @Test
    @EnabledIf("allCellsAvailable")
    void witness_xmeta_unnamedNaming_countAllNamed() {
        String gen = generated("cdm6", "xmeta",
                "cdm/product/common/settlement/meta/PhysicalSettlementPeriodMeta.java");
        assertFalse(gen.contains("PhysicalSettlementPeriodDataRule0"),
                "REVERTED DEFECT: the dormant index-based unnamed-condition fallback name");
        assertTrue(gen.contains("PhysicalSettlementPeriodOneOf2.class"),
                "golden shape: count-all-named + the OneOf expression-root kind");
    }

    /**
     * Drift-class-6 witnesses: (a) the EMPTY separator line between dataRules and
     * getQualifyFunctions has count 0 in the golden (the line carries {@code \t});
     * (b) the THREE-blank-line import gap has count 0 (the golden carries TWO);
     * (c) the declaring-supertype import ({@code cdm.base.datetime.Offset}) is present
     * for an inherited condition ref.
     */
    @Test
    @EnabledIf("allCellsAvailable")
    void witness_xmeta_blankLineAndDeclaringImport() {
        String gen = normalize(generated("cdm6", "xmeta", "cdm/base/staticdata/asset/common/meta/AssetMeta.java"));
        assertFalse(gen.contains("\t}\n\n\t@Override\n\tpublic List<Function"),
                "REVERTED DEFECT: the dataRules→qualify separator lost its \\t");
        assertTrue(gen.contains("\t}\n\t\n\t@Override\n\tpublic List<Function"),
                "golden shape: the tab-bearing separator line");
        assertFalse(gen.contains(";\n\n\n\n/**"),
                "REVERTED DEFECT: three blank lines before the version javadoc");
        assertTrue(gen.contains(";\n\n\n/**"),
                "golden shape: two blank lines between the imports and the javadoc");
        String inherited = generated("cdm6", "xmeta", "cdm/base/datetime/meta/AdjustedRelativeDateOffsetMeta.java");
        assertTrue(inherited.contains("import cdm.base.datetime.Offset;"),
                "golden shape: the inherited condition ref imports its DECLARING type");
    }

    /**
     * Drift-class-7 witness: the bare {@code @RosettaMeta(model=Error.class)} and the
     * blocked {@code import iso20022.dtcc.rds.harmonized.Error;} both have count 0 in
     * the iso golden — the collision subject renders FQN at every TYPE position.
     */
    @Test
    @EnabledIf("allCellsAvailable")
    void witness_xmeta_collisionSubject_fqn() {
        String gen = generated("iso", "xmeta", "iso20022/dtcc/rds/harmonized/meta/ErrorMeta.java");
        assertFalse(gen.contains("@RosettaMeta(model=Error.class)"),
                "REVERTED DEFECT: the bare colliding simple name at the @RosettaMeta position");
        assertFalse(gen.contains("import iso20022.dtcc.rds.harmonized.Error;"),
                "REVERTED DEFECT: the collision-blocked data-class import");
        assertTrue(gen.contains("@RosettaMeta(model=iso20022.dtcc.rds.harmonized.Error.class)"),
                "golden shape: the FQN at the @RosettaMeta position");
        assertTrue(normalize(gen).contains("/**\n * @version ${project.version}\n */"),
                "golden shape: the iso cell's unresolved ${project.version} literal stamp");
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
                + path + " (PR #407 wave B).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
