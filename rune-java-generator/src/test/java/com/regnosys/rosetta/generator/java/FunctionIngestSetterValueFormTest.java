package com.regnosys.rosetta.generator.java;

import com.regnosys.rosetta.generator.GenerationException;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Facet {@code ingest_setter_value_form} — six small setter-name / value-form
 * recoveries, mostly in the cdm 6.20.6 ingest-fpml {@code Map*} function
 * family (the CLAUDE.md § NEXT Lead #2 singletons). Each arm mirrors a
 * distinct upstream law:
 *
 * <ol>
 *   <li><b>C1 — ctor setter name respects the VALUE expression's meta.</b>
 *       Upstream picks {@code set<X>Value} only when the target attribute has
 *       meta and the value expression does NOT
 *       ({@code requiresValueAssignment}, ExpressionGenerator.xtend:1233-1240);
 *       a direct fn-call value whose callee output is itself meta-annotated
 *       keeps the PLAIN setter ({@code .setContractualParty(...)}, not
 *       {@code .setContractualPartyValue(...)}). The fork keyed only on the
 *       target attribute's annotation.</li>
 *   <li><b>C2r — {@code new ArrayList<>(...)} wrap only for wildcard (pojo)
 *       item types.</b> Upstream inserts the defensive-copy wrap only when the
 *       actual type carries a wildcard argument (TypeCoercionService.xtend:342-344),
 *       and only generated-pojo item types are ever wildcard; a
 *       {@code List<Date>} / {@code List<DayOfWeekEnum>} fn-call value splices
 *       bare. The fork wrapped every multi ctor value unconditionally.</li>
 *   <li><b>C3 — deep-path meta-attribute leaf.</b> A SET path ending in the
 *       {@code key}/{@code id} meta pseudo-attribute renders
 *       {@code .getOrCreateMeta().setExternalKey(...)}
 *       (FunctionGenerator.xtend:478-550 + PojoPropertyUtil key/id→externalKey);
 *       the fork string-built the non-compiling {@code .setKey(...)}.</li>
 *   <li><b>C4 — OverriddenAs leaf setter name.</b> A deep-path leaf attribute
 *       that OVERRIDES a parent attribute uses the pojo's compatibility name
 *       ({@code setCollateralCriteriaOverriddenAsCollateralCriteria}) exactly
 *       as the fork's own ModelObjectGenerator emits it; the fork rendered the
 *       plain delegating name (byte-only divergence).</li>
 *   <li><b>C5 — to-enum meta unwrap.</b> Upstream compiles the conversion
 *       argument against the meta-STRIPPED expected type, materialising the
 *       {@code .<String>map("Type coercion", fieldWithMetaString -> ...
 *       .getValue())} step before {@code .checkedMap("to-enum", ...)}; the
 *       fork fed the raw {@code FieldWithMetaString} into
 *       {@code CurrencyCodeEnum::fromDisplayName} — non-compiling.</li>
 *   <li><b>C6 — multi-output implicit-else empty list.</b> The absent else of
 *       a conditional MULTI assignment renders
 *       {@code Collections.<String>emptyList()}
 *       (TypeCoercionService.xtend:377-380), not {@code null}; single targets
 *       keep {@code null} untouched.</li>
 * </ol>
 *
 * <p>Green-safety: C3/C5 and the MapAncillaryParty C1 shape rendered
 * NON-COMPILING Java pre-fix; C1-call/C2r/C4/C6 are law-based (upstream never
 * emits the fork's form for the gated shape, so a byte-identical green file
 * cannot contain it) with every gate declining to today's render on resolution
 * doubt.
 *
 * <p>Anchors are WHOLE-FILE byte comparisons against the frozen goldens
 * (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}, one sole-mechanism
 * waivered file per arm (two for the two C1/C2r/C3 shape variants):
 * cdm/6.20.6 {@code MapLegalAgreement} + {@code MapAncillaryParty} (C1),
 * cdm/6.20.6 {@code MapDateListToAdjustableDates} + drr/6.34.1
 * {@code Create_DeliveryBlock} (C2r), cdm/6.20.6
 * {@code MapAdjustedDateToAdjustableOrRelativeDate} +
 * {@code MapUnadjustedDateToAdjustableOrRelativeDate} (C3), cdm/6.20.6
 * {@code CloneEligibleCollateralWithChangedTreatment} (C4), cdm/6.20.6
 * {@code GetCashCurrency} (C5), drr/6.34.1 {@code ExtractPartySector} (C6).
 */
class FunctionIngestSetterValueFormTest {

    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static Map<String, String> cdm6FunctionOutput;
    private static Map<String, String> drrFunctionOutput;

    static boolean cdm6CellAvailable() {
        return Files.isDirectory(CDM6_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM6_GOLDEN_DIR);
    }

    static boolean drrCellAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR);
    }

    @BeforeAll
    static void generateFunctions() throws IOException {
        if (cdm6CellAvailable()) {
            cdm6FunctionOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CDM6_CELL_ROOT));
        }
        if (drrCellAvailable()) {
            drrFunctionOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT));
        }
    }

    private static Map<String, String> generateCell(D11CorpusRegressionTest.CellSpec cell)
            throws IOException {
        var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(),
                D11CorpusRegressionTest.emissionFilter(cell));
        var typeUtil = new JavaTypeUtil();
        var gen = new FunctionGenerator(gm, new JavaTypeTranslator(typeUtil), typeUtil);
        Map<String, String> output = new LinkedHashMap<>();
        List<GenerationException> genErrors = gen.generateWithErrors(output);
        assertTrue(genErrors.isEmpty(),
                cell + " FUNCTION generation reported errors: " + genErrors);
        return output;
    }

    /** C1: meta-carrying fn-call ctor value keeps the PLAIN setter (multi shape). */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapLegalAgreement_plainSetterForMetaValue_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/legal/functions/MapLegalAgreement.java");
    }

    /** C1: the extract-bodied variant of the meta-carrying value shape. */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapAncillaryParty_plainSetterForMetaValue_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/party/functions/MapAncillaryParty.java");
    }

    /** C2r: a List&lt;Date&gt; ctor value splices bare — no ArrayList wrap. */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapDateListToAdjustableDates_noArrayListWrap_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/datetime/functions/MapDateListToAdjustableDates.java");
    }

    /** C2r: the enum-item variant (List&lt;DayOfWeekEnum&gt; fn-call value). */
    @Test
    @EnabledIf("drrCellAvailable")
    void createDeliveryBlock_noArrayListWrap_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/regulation/common/functions/Create_DeliveryBlock.java");
    }

    /** C3: deep-path key leaf renders getOrCreateMeta().setExternalKey (adjusted shape). */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapAdjustedDate_metaKeyLeaf_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/datetime/functions/MapAdjustedDateToAdjustableOrRelativeDate.java");
    }

    /** C3: the unadjusted sibling — same key-leaf law, second shape pin. */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapUnadjustedDate_metaKeyLeaf_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/ingest/fpml/confirmation/datetime/functions/MapUnadjustedDateToAdjustableOrRelativeDate.java");
    }

    /** C4: overridden leaf attribute takes the OverriddenAs compatibility setter. */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void cloneEligibleCollateral_overriddenAsSetter_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/product/collateral/functions/CloneEligibleCollateralWithChangedTreatment.java");
    }

    /** C5: to-enum over a FieldWithMetaString takes the Type-coercion unwrap step. */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void getCashCurrency_toEnumMetaUnwrap_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6FunctionOutput, CDM6_GOLDEN_DIR,
                "cdm/base/staticdata/asset/common/functions/GetCashCurrency.java");
    }

    /** C6: multi-output implicit else renders Collections.&lt;String&gt;emptyList(). */
    @Test
    @EnabledIf("drrCellAvailable")
    void extractPartySector_emptyListElse_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrFunctionOutput, DRR_GOLDEN_DIR,
                "drr/regulation/common/trade/party/functions/ExtractPartySector.java");
    }

    private static void assertByteMatchesGolden(Map<String, String> functionOutput,
            Path goldenDir, String path) throws IOException {
        assertNotNull(functionOutput, "Function generation did not run — corpus unavailable?");
        String generated = functionOutput.get(path);
        assertNotNull(generated, "Function not generated: " + path);
        Path goldenPath = goldenDir.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated output must byte-match the golden (newline-normalized) for "
                + path + " — the ctor setter name ignores the value expression's "
                + "meta, the ArrayList wrap fires on a non-wildcard item type, the "
                + "deep-path key/overridden leaf renders the wrong setter, the "
                + "to-enum argument misses the meta unwrap, or the multi-output "
                + "implicit else renders null, if the ingest_setter_value_form "
                + "recovery is reverted.");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
