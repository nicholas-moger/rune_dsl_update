package com.regnosys.rosetta.generator.java;

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.java.object.deeppath.DeepPathUtilGenerator;
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
 * PR #408 — coverage burn-down wave C anchors: the deep-path util family
 * ({@code <pkg>.util.<T>DeepPathUtil}) locked byte-identical against the frozen
 * 9.83.0 goldens, generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached} route. One representative
 * whole-file lock per cell carrying the family's hardest shapes, plus NEGATIVE
 * WITNESSES — tokens with COUNT 0 IN THE GOLDEN that exactly a broken law
 * re-introduces.
 *
 * <p>The laws these anchors hold shut (audit: {@code target-408-audit1..2.log} —
 * cdm6 31/31 + iso 231/237 → 237/237 after ONE drift class):
 * <ol>
 *   <li><b>the HashMap method-order law</b> — {@code choose*} methods render in
 *       {@code DeepPathScan.findDeepFeatureMap(type).values()} iteration order,
 *       the operation-by-operation port of upstream's HashMap sequence;</li>
 *   <li><b>the HashSet constructor-order law</b> — {@code ObservableDeepPathUtil}'s
 *       2-dependency order ({@code indexDeepPathUtil} BEFORE
 *       {@code assetDeepPathUtil} — buckets 7/15 of the 16-bucket table; neither
 *       alphabetical nor declaration order; the ONLY ≥2-dep golden corpus-wide);</li>
 *   <li><b>the meta unwrap law</b> — a meta-wrapped alternative inserts the
 *       {@code "Type coercion"} null-ternary {@code getValue()} step before
 *       descending, and a meta-wrapped feature returns its wrapper with the
 *       {@code FieldWithMetaString.builder().build()} terminal
 *       ({@code IndexDeepPathUtil});</li>
 *   <li><b>the keyword-escape law</b> — an attribute named {@code new} declares
 *       guard variable {@code _new} (the audit-1 drift class, 6 iso witnesses);</li>
 *   <li><b>the NullSafe static-import context</b> — the family imports
 *       {@code ExpressionOperatorsNullSafe.*}, never the function-body
 *       {@code ExpressionOperators.*}; no javadoc/version stamp anywhere;</li>
 *   <li><b>the empty-class law</b> — an eligible type with zero deep features
 *       renders the import-less empty class ({@code UnitTypeDeepPathUtil}).</li>
 * </ol>
 *
 * <p><b>REVERT-RED (the wave-C form).</b> Unlike waves A/B — whose generators were
 * rebuilds of pre-existing dormant classes, revertible by checking out the
 * pre-wave bundle — the deep-path family is ENTIRELY NEW: the pre-wave tree has no
 * generator to run, and checking it out removes the classes this anchor imports
 * (the full-revert RED is an unconditional anchor-compilation failure, verified as
 * such). The per-law discrimination is therefore revert-verified by LAW REVERSION
 * instead: (a) {@code escapeReserved} → identity (the audit-caught defect) — the
 * iso lock + the keyword witness go RED; (b) the dependency {@code HashSet} → an
 * alphabetical sort — the Observable lock + the ctor-order witness go RED
 * ({@code target-408-anchor-red.log}).
 */
class WaveCFamilyAnchorTest {

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

    /** Per cell key: the deep-path util output map (path → source). */
    private static final Map<String, Map<String, String>> OUTPUTS = new LinkedHashMap<>();

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
            var gen = new DeepPathUtilGenerator(gm, TYPE_TRANSLATOR, TYPE_UTIL);
            Map<String, String> output = new LinkedHashMap<>();
            for (RModel model : corpus.workspace().files()) {
                if (gm.shouldGenerate(model)) {
                    assertNoGenerationErrors(gen.generateClasses(model, gm.version(model), output));
                }
            }
            OUTPUTS.put(e.getKey(), output);
        }
    }

    // ==== whole-file byte locks (one per cell + the hard cdm6 shapes) ====

    /**
     * cdm6, THE ordering pin: the ONLY ≥2-dependency golden corpus-wide —
     * {@code indexDeepPathUtil} before {@code assetDeepPathUtil} (the HashSet
     * iteration-order law), cross-package dep import (AssetDeepPathUtil) beside the
     * same-package no-import sibling (IndexDeepPathUtil), delegated multi
     * ({@code mapC …chooseIdentifier… getMulti()}) and the
     * {@code Collections.<X>emptyList()} terminal.
     */
    @Test
    @EnabledIf("allCellsAvailable")
    void deeppath_cdm6_observable_ctorOrder_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("cdm6", "cdm/observable/asset/util/ObservableDeepPathUtil.java");
    }

    /**
     * cdm6, the meta carrier: the {@code FieldWithMetaInterestRateIndex} guard, the
     * {@code "Type coercion"} null-ternary unwrap before delegation, the
     * meta-wrapped feature return ({@code FieldWithMetaString chooseName}) and the
     * {@code FieldWithMetaString.builder().build()} terminal.
     */
    @Test
    @EnabledIf("allCellsAvailable")
    void deeppath_cdm6_index_metaCoercion_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("cdm6", "cdm/observable/asset/util/IndexDeepPathUtil.java");
    }

    /**
     * cdm6, a {@code choice} subject: options as capitalized-name attributes
     * ({@code getCash}/{@code cash} guard), instrument delegation via the
     * same-package sibling (no import), the 5-method HashMap order.
     */
    @Test
    @EnabledIf("allCellsAvailable")
    void deeppath_cdm6_assetChoice_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("cdm6", "cdm/base/staticdata/asset/common/util/AssetDeepPathUtil.java");
    }

    /** cdm6, the EMPTY-class law: an eligible type with zero deep features — package + three blank lines + empty body, no imports at all. */
    @Test
    @EnabledIf("allCellsAvailable")
    void deeppath_cdm6_unitType_empty_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("cdm6", "cdm/base/math/util/UnitTypeDeepPathUtil.java");
    }

    /** cdm5, the OTHER cdm cell's dependency carrier ({@code bondChoiceModelDeepPathUtil} delegation). */
    @Test
    @EnabledIf("allCellsAvailable")
    void deeppath_cdm5_bondEquityModel_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("cdm5", "cdm/observable/asset/util/BondEquityModelDeepPathUtil.java");
    }

    /** drr, one of the cell's exactly-two utils: CROSS-NAMESPACE imports (cdm.* types into a drr util — the transitive-CDM closure). */
    @Test
    @EnabledIf("allCellsAvailable")
    void deeppath_drr_payoutLeg_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr", "drr/regulation/common/util/PayoutLegDeepPathUtil.java");
    }

    /** iso, the keyword-escape carrier: the attribute named {@code new} → guard variable {@code _new} (the audit-1 drift class). */
    @Test
    @EnabledIf("allCellsAvailable")
    void deeppath_iso_tradeReport33_keywordEscape_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("iso", "iso20022/auth030/asic/util/TradeReport33Choice__1DeepPathUtil.java");
    }

    /** fpml, the BigDecimal-feature carrier ({@code chooseOpenUnits}). */
    @Test
    @EnabledIf("allCellsAvailable")
    void deeppath_fpml_underlyer_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("fpml", "fpml/consolidated/asset/util/UnderlyerDeepPathUtil.java");
    }

    // ==== NEGATIVE WITNESSES (count 0 in golden; a broken law re-introduces each) ====

    /**
     * Keyword-escape witness: the UNESCAPED guard declaration
     * {@code > new = MapperS} has count 0 in the golden (the audit-1 defect rendered
     * it verbatim — invalid Java); the escaped {@code _new} declaration is the
     * golden shape.
     */
    @Test
    @EnabledIf("allCellsAvailable")
    void witness_keywordEscape() {
        String gen = generated("iso", "iso20022/auth030/asic/util/TradeReport33Choice__1DeepPathUtil.java");
        assertFalse(gen.contains("> new = MapperS"),
                "BROKEN LAW: the Java-keyword attribute rendered an unescaped guard variable");
        assertTrue(gen.contains("> _new = MapperS.of(tradeReport33Choice__1)"),
                "golden shape: the keyword-escaped guard variable _new");
    }

    /**
     * Ctor-order witness: the alphabetical/declaration order
     * {@code (AssetDeepPathUtil assetDeepPathUtil, IndexDeepPathUtil indexDeepPathUtil)}
     * has count 0 in the golden — the HashSet law puts index first (neither
     * alphabetical nor the asset/basket/index declaration order).
     */
    @Test
    @EnabledIf("allCellsAvailable")
    void witness_ctorOrder_hashSetLaw() {
        String gen = generated("cdm6", "cdm/observable/asset/util/ObservableDeepPathUtil.java");
        assertFalse(gen.contains("(AssetDeepPathUtil assetDeepPathUtil, IndexDeepPathUtil indexDeepPathUtil)"),
                "BROKEN LAW: dependencies ordered alphabetically/declaration-first");
        assertTrue(gen.contains("(IndexDeepPathUtil indexDeepPathUtil, AssetDeepPathUtil assetDeepPathUtil)"),
                "golden shape: the HashSet iteration order (index bucket 7 < asset bucket 15)");
        assertTrue(gen.indexOf("private final IndexDeepPathUtil indexDeepPathUtil;")
                        < gen.indexOf("private final AssetDeepPathUtil assetDeepPathUtil;"),
                "golden shape: the field order matches the ctor order");
    }

    /**
     * Meta-unwrap witness: the coercion-less delegation
     * {@code interestRateIndex.<Boolean>map("chooseIsExchangeListed"} has count 0 in
     * the golden (the receiver is {@code MapperS<FieldWithMetaInterestRateIndex>} —
     * the {@code "Type coercion"} step must interpose); the builder terminal
     * replaces {@code null} for the meta-wrapped feature.
     */
    @Test
    @EnabledIf("allCellsAvailable")
    void witness_metaUnwrapAndBuilderTerminal() {
        String gen = generated("cdm6", "cdm/observable/asset/util/IndexDeepPathUtil.java");
        assertFalse(gen.contains("interestRateIndex.<Boolean>map(\"chooseIsExchangeListed\""),
                "BROKEN LAW: delegation on the meta-wrapped receiver without the Type-coercion unwrap");
        assertTrue(gen.contains(
                "interestRateIndex.<InterestRateIndex>map(\"Type coercion\", fieldWithMetaInterestRateIndex -> "
                + "fieldWithMetaInterestRateIndex == null ? null : fieldWithMetaInterestRateIndex.getValue())"),
                "golden shape: the null-ternary getValue() unwrap named after the wrapper type");
        assertTrue(gen.contains("return FieldWithMetaString.builder().build();"),
                "golden shape: the meta-wrapped single feature's builder terminal (never `return null;`)");
    }

    /**
     * Static-import-context witness: the function-body context
     * {@code import static …ExpressionOperators.*;} has count 0 in the family — the
     * util classes import the {@code ExpressionOperatorsNullSafe} wildcard; and the
     * family carries NO version javadoc anywhere ({@code @version} count 0 — even in
     * the iso cell, whose XMeta files stamp the {@code ${project.version}} literal).
     */
    @Test
    @EnabledIf("allCellsAvailable")
    void witness_nullSafeImportContext_noVersionJavadoc() {
        for (String path : new String[]{
                "cdm/observable/asset/util/ObservableDeepPathUtil.java",
                "cdm/base/staticdata/asset/common/util/AssetDeepPathUtil.java"}) {
            String gen = generated("cdm6", path);
            assertFalse(gen.contains("import static com.rosetta.model.lib.expression.ExpressionOperators.*;"),
                    "BROKEN LAW: the function-body import context leaked into a util class");
            assertTrue(gen.contains("import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;"),
                    "golden shape: the NullSafe wildcard static import");
            assertFalse(gen.contains("@version"),
                    "BROKEN LAW: a version javadoc in the deep-path family");
        }
        String iso = generated("iso", "iso20022/auth030/asic/util/TradeReport33Choice__1DeepPathUtil.java");
        assertFalse(iso.contains("@version"),
                "BROKEN LAW: the iso ${project.version} stamp leaked into the deep-path family");
    }

    /**
     * Empty-class witness: {@code UnitTypeDeepPathUtil} (eligible, zero deep
     * features) carries NO import statement at all — a generator that emits the
     * frame imports (MapperS / the static wildcard) unconditionally re-introduces
     * them.
     */
    @Test
    @EnabledIf("allCellsAvailable")
    void witness_emptyClass_importless() {
        String gen = generated("cdm6", "cdm/base/math/util/UnitTypeDeepPathUtil.java");
        assertFalse(gen.contains("import"),
                "BROKEN LAW: an import in the empty deep-path class");
        assertFalse(gen.contains("MapperS"),
                "BROKEN LAW: frame machinery leaked into the empty deep-path class");
        assertTrue(normalize(gen).endsWith("public class UnitTypeDeepPathUtil {\n}\n"),
                "golden shape: the empty class body");
    }

    // ==== plumbing ====

    private static String generated(String cellKey, String path) {
        Map<String, String> output = OUTPUTS.get(cellKey);
        assertNotNull(output, "generation did not run — corpus unavailable?");
        String gen = output.get(path);
        assertNotNull(gen, "not generated: " + path + " (" + cellKey + ")");
        return gen;
    }

    private static void assertByteMatchesGolden(String cellKey, String path) throws IOException {
        String generated = generated(cellKey, path);
        Path goldenPath = D11CorpusRegressionTest.resolveGoldensDir(CELLS.get(cellKey)).resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated output must byte-match the golden (newline-normalized) for "
                + path + " (PR #408 wave C).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
