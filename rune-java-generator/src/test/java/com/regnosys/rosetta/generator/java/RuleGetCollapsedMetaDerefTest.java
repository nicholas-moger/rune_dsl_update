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
 * Anchor for facet {@code getCollapsedMetaDeref} (PR #314): a {@code to-enum} whose FIELD_WITH_META
 * -{@code string} source is a {@code .get()}-COLLAPSED bare wrapper cannot take the inline C5
 * {@code .<String>map("Type coercion", …)} meta-deref step ({@code .map} is a {@code Mapper} method,
 * invalid on the bare {@code FieldWithMetaString} the {@code .get()} yields). golden block-converts the
 * enclosing {@code mapSingleToItem} lambda: hoist {@code final FieldWithMetaString <name> = <chain>.get();}
 * through the LAMBDA_CHANNEL + null-guard-reconstructs {@code (<name> == null ? MapperS.<String>ofNull() :
 * MapperS.of(<name>.getValue())).checkedMap("to-enum", …)}. See
 * {@code ConversionHandler.getCollapsedMetaDerefOrNull}.
 *
 * <p>Carrier (1): {@code BookingLocationRule} (common trade execution) — {@code getCountry} on a MULTI
 * {@code getAddress} ({@code .mapC(…)}) chain, {@code to-enum ISOCountryCodeEnum}. The census SOLE-2 was
 * refuted to 1 clean: {@code TraderLocationRule} shares the shape (my arm block-converts its country deref
 * TOWARD golden — 14→9 diff lines) but is co-occupied with a block-lambda then-hoist + {@code _thenArg},
 * so it stays divergent.
 *
 * <p>Green-safe by construction: the fork's bare-wrapper {@code .map} never compiled, so every carrier was
 * an already-waivered mismatch (a green file cannot carry the firing shape). RULE-scoped
 * ({@code findEnclosingRule}) → FUNCTION-byte-neutral (#232, cdm5 79 / cdm6 232 / drr 206 FUNCTION mismatch
 * UNCHANGED, cdm/iso/fpml POJO byte-IDENTICAL). A plain (non-collapsed / non-FIELD_WITH_META) to-enum source
 * keeps the compiling inline form: the GREEN {@code CollateralisationCategoryRule} (asic) — an enum-source
 * {@code .checkedMap("to-enum", e -> …valueOf(e.name()), …)} — stays byte-matching.
 *
 * <p>Anchors are WHOLE-FILE byte comparisons of the generated drr output against the frozen goldens
 * (newline-normalized), generated through the REAL {@link D11CorpusRegressionTest#loadCellCorpusCached}.
 * REVERT-VERIFIED RED 3/4 — the flip lock + the positive-content lock + the TraderLocation
 * fires-toward-golden lock fail on clean source; the green-safety decline lock passes either way.
 */
class RuleGetCollapsedMetaDerefTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static final String BOOKING_LOCATION =
            "drr/regulation/common/trade/execution/reports/BookingLocationRule.java";
    private static final String TRADER_LOCATION =
            "drr/regulation/common/trade/execution/reports/TraderLocationRule.java";
    private static final String COLLAT_CATEGORY_ASIC =
            "drr/regulation/asic/rewrite/margin/reports/CollateralisationCategoryRule.java";

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

    // ==== Flip lock (revert-RED): the carrier now byte-matches golden. ====

    /** Flip — BookingLocationRule: the getCountry to-enum block-conversion. */
    @Test
    @EnabledIf("drrCellAvailable")
    void bookingLocation_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(BOOKING_LOCATION);
    }

    // ==== Positive-content lock (revert-RED): the block-converted hoist + reconstruct. ====

    /**
     * BookingLocation's {@code mapSingleToItem} lambda is BLOCK-converted: the collapsed bare wrapper is
     * hoisted {@code final FieldWithMetaString fieldWithMetaString = <chain>.get();} and the country deref
     * is null-guard-reconstructed {@code (fieldWithMetaString == null ? MapperS.<String>ofNull() :
     * MapperS.of(fieldWithMetaString.getValue())).checkedMap("to-enum", …)}, NOT the fork's non-compiling
     * {@code <chain>.get().<String>map("Type coercion", …)} (a {@code Mapper} method on a bare wrapper).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void bookingLocation_blockConvertsMetaDerefHoist() {
        String gen = gen(BOOKING_LOCATION);
        assertTrue(gen.contains(
                "final FieldWithMetaString fieldWithMetaString = item.<ReportingSide>map(\"getReportingSide\""),
                "Expected the collapsed bare wrapper hoisted to a final FieldWithMetaString local");
        assertTrue(gen.contains(
                ".<FieldWithMetaString>map(\"getCountry\", address -> address.getCountry()).get();"),
                "Expected the hoist value to be the .get()-collapsed getCountry chain");
        assertTrue(gen.contains(
                "return (fieldWithMetaString == null ? MapperS.<String>ofNull() : "
                + "MapperS.of(fieldWithMetaString.getValue())).checkedMap(\"to-enum\", "
                + "ISOCountryCodeEnum::fromDisplayName, IllegalArgumentException.class);"),
                "Expected the null-guard-reconstructed MapperS<String> before .checkedMap");
        assertTrue(!gen.contains(".get().<String>map(\"Type coercion\""),
                "The fork's non-compiling .get().<String>map(\"Type coercion\", …) on a bare wrapper must be gone");
    }

    // ==== Fires-toward-golden lock (revert-RED): the shared checkedMap-wrap shape. ====

    /**
     * TraderLocationRule shares the {@code .get()}-collapsed FIELD_WITH_META-string to-enum shape (the
     * SAME getCountry deref), so my arm block-converts its country deref — the null-guard-reconstruct is
     * present, the fork's {@code .get().<String>map("Type coercion", …)} on the bare wrapper is gone. The
     * file STAYS divergent (co-occupied with a block-lambda then-hoist + {@code _thenArg}), so this is a
     * content lock, not a byte-match. Fails on clean source (the reconstruct is not there).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void traderLocation_countryDeref_firesTowardGolden() {
        String gen = gen(TRADER_LOCATION);
        assertTrue(gen.contains(
                "MapperS.of(fieldWithMetaString.getValue())).checkedMap(\"to-enum\", "
                + "ISOCountryCodeEnum::fromDisplayName"),
                "Expected TraderLocation's country deref block-reconstructed MapperS.of(...getValue()).checkedMap");
        assertTrue(gen.contains(
                ".<FieldWithMetaString>map(\"getCountry\", address -> address.getCountry()).get();"),
                "Expected the collapsed getCountry chain hoisted to a local");
        assertTrue(!gen.contains(
                "address -> address.getCountry()).get().<String>map(\"Type coercion\""),
                "The fork's non-compiling country-deref .get().<String>map(\"Type coercion\", …) must be gone");
    }

    // ==== Green-safety lock (passes on clean source too): a plain to-enum declines. ====

    /**
     * Green-safety — {@code CollateralisationCategoryRule} (asic) is a GREEN plain ENUM-source to-enum
     * ({@code .checkedMap("to-enum", e -> CollateralisationType3Code__1.valueOf(e.name()), …)}, no
     * FIELD_WITH_META string leaf, no {@code .get()}-collapsed meta wrapper), so the arm declines (its
     * {@code conversionLeafAttribute} is not a FIELD_WITH_META string) and the file stays byte-matching
     * golden. Proves the fix does not disturb the broad plain to-enum population. Passes on clean source.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void collateralisationCategory_plainToEnum_staysGreenAndDeclines() throws IOException {
        String gen = gen(COLLAT_CATEGORY_ASIC);
        assertTrue(gen.contains(
                ".checkedMap(\"to-enum\", e -> CollateralisationType3Code__1.valueOf(e.name()), "
                + "IllegalArgumentException.class)"),
                "CollateralisationCategory must keep the plain enum-source .checkedMap(\"to-enum\", …)");
        assertTrue(!gen.contains("MapperS.<String>ofNull()"),
                "A plain to-enum must NOT be block-converted into the MapperS.<String>ofNull() reconstruct");
        assertByteMatchesGolden(COLLAT_CATEGORY_ASIC);
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
                + path + " (PR #314 getCollapsedMetaDeref).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
