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
 * Anchor for PR #325's FUNCTION-tail re-opening (facets functionTailCardinality +
 * functionTailMetaCoerce, GENERATOR-only, parser UNTOUCHED) — the first dedicated
 * FUNCTION-path facet since ~#252: four rule-proven laws widened to the function path,
 * flipping 10 waivered FUNCTION files (cdm5 1 + cdm6 6 + drr 3):
 * <ul>
 *   <li><b>C1 — MapperC.of wrap at *ToList lambda bodies</b> (the #278 arm un-gated,
 *       path-independent green-safety: a bare {@code List}-returning
 *       {@code <fn>.evaluate(...)} at a MapperC-expecting body never compiles) —
 *       MapPartyRoleList / MapTradeIdentifierList / MapNovationToPrimitiveInstruction.</li>
 *   <li><b>B1 — receiver/body cardinality overlays</b>: a MULTI ALIAS-call receiver (the
 *       #178 aliasIsMulti walk at the {@code chainProvesMulti} predicate — a MapperC
 *       receiver has no {@code mapSingleToItem}, non-compiling either path) selects
 *       {@code mapItem} (MapNaturalPersonRoleList); the #293 RFunction-receiver arm
 *       un-gated (the declared-output mirror argument); a provably-multi
 *       {@code RFeatureCall} extract body reads multi path-blind
 *       ({@code isBodyMulti}).</li>
 *   <li><b>B2 — whole-output terminal {@code .getMulti()}</b> via the
 *       {@code chainProvesMulti} overlay at {@code isMultiToMultiSet}'s function branch
 *       (multi MID-chain steps the leaf-only workspace cardinality misses) + the new
 *       flatten/distinct list-op arms — ExtractCommodityCalculationPeriods.</li>
 *   <li><b>A — meta-KEEP + "Type coercion" deref at function-path lambda seats</b>: the
 *       #284/#286 {@code implicitItemMetaMapperType} fallback widened path-blind,
 *       BODY-SHAPE-ANCHORED on the function path (nav/comparison/predicate lambda bodies
 *       only; a CONSTRUCTOR or CONDITIONAL body declines — golden restructures those
 *       enclosing forms, the drr NotionalLeg regscan catch) — ExtractUpi /
 *       FrequencyPeriod / QuantityFrequencyOrCalculationPeriod (drr) /
 *       InterestRateObservableCondition (cdm6) / RateOptionObservableCondition (cdm5).</li>
 * </ul>
 *
 * <p>Anchors are WHOLE-FILE byte comparisons of generated output against the frozen
 * 9.83.0 goldens (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}.
 *
 * <p><b>Deliberate coverage gap (Seat-1 S1):</b> the 10th flip —
 * {@code RateOptionObservableCondition} (cdm/5.38.0) — has no byte lock here: locking it
 * would load a THIRD cell corpus for one file. It is verified by the PR #325 byte-oracle +
 * probe dumps, and the cdm6 SAME-FAMILY lock ({@code InterestRateObservableCondition},
 * the identical mapItem-areEqual body-shape) covers the mechanism.
 */
class FunctionTailCardinalityMetaTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    // C1 flip carriers (cdm6): bare multi-fn *ToList bodies now MapperC.<X>of-wrapped.
    private static final String MAP_PARTY_ROLE_LIST =
            "cdm/ingest/fpml/confirmation/party/functions/MapPartyRoleList.java";
    private static final String MAP_NOVATION =
            "cdm/ingest/fpml/confirmation/workflowstep/functions/MapNovationToPrimitiveInstruction.java";
    private static final String MAP_TRADE_IDENTIFIER_LIST =
            "cdm/ingest/fpml/confirmation/header/functions/MapTradeIdentifierList.java";
    // B1 flip carrier (cdm6): multi ALIAS receiver -> mapItem.
    private static final String MAP_NATURAL_PERSON_ROLE_LIST =
            "cdm/ingest/fpml/confirmation/party/functions/MapNaturalPersonRoleList.java";
    // B2 flip carrier (cdm6): whole-output terminal .getMulti() via mid-chain mapC.
    private static final String EXTRACT_COMMODITY_CALCULATION_PERIODS =
            "cdm/ingest/fpml/confirmation/product/commodityoption/functions/ExtractCommodityCalculationPeriods.java";
    // A flip carrier (cdm6): mapItem areEqual body deref (the body-shape anchor admits it).
    private static final String INTEREST_RATE_OBSERVABLE_CONDITION =
            "cdm/observable/asset/functions/InterestRateObservableCondition.java";
    // A flip carriers (drr): filter-predicate + map-nav-body item derefs.
    private static final String EXTRACT_UPI = "drr/enrichment/upi/functions/ExtractUpi.java";
    private static final String FREQUENCY_PERIOD =
            "drr/regulation/common/functions/FrequencyPeriod.java";
    private static final String QUANTITY_FREQUENCY =
            "drr/regulation/common/functions/QuantityFrequencyOrCalculationPeriod.java";
    // Green-safety pin (cdm6): a GREEN function exercising mapItem + filterItemNullSafe +
    // Type-coercion seats — must stay byte-identical under all four widened laws.
    private static final String PRICE_QUANTITY_TRIANGULATION_GREEN =
            "cdm/product/template/functions/PriceQuantityTriangulation.java";
    // Body-shape-anchor pin (drr): NotionalLeg's mapSingleToItem CONSTRUCTOR/CONDITIONAL
    // bodies DECLINE the function-path meta recovery (golden restructures the enclosing
    // form; an in-lambda per-use deref moved the file AWAY from golden — the regscan
    // catch that drove the anchor). The numbered coercion param is the away-signature.
    private static final String NOTIONAL_LEG =
            "drr/standards/iosco/cde/base/quantity/functions/NotionalLeg.java";

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
    @EnabledIf("cdm6CellAvailable")
    void mapPartyRoleList_mapperCWrap_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6Output, CDM6_GOLDEN_DIR, MAP_PARTY_ROLE_LIST);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapNovation_mapperCWrap_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6Output, CDM6_GOLDEN_DIR, MAP_NOVATION);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapTradeIdentifierList_mapperCWrap_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6Output, CDM6_GOLDEN_DIR, MAP_TRADE_IDENTIFIER_LIST);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapNaturalPersonRoleList_aliasReceiverMulti_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6Output, CDM6_GOLDEN_DIR, MAP_NATURAL_PERSON_ROLE_LIST);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void extractCommodityCalculationPeriods_terminalGetMulti_byteMatchesGolden()
            throws IOException {
        assertByteMatchesGolden(cdm6Output, CDM6_GOLDEN_DIR,
                EXTRACT_COMMODITY_CALCULATION_PERIODS);
    }

    @Test
    @EnabledIf("cdm6CellAvailable")
    void interestRateObservableCondition_mapItemDeref_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(cdm6Output, CDM6_GOLDEN_DIR, INTEREST_RATE_OBSERVABLE_CONDITION);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void extractUpi_filterAndMapDeref_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrOutput, DRR_GOLDEN_DIR, EXTRACT_UPI);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void frequencyPeriod_filterDeref_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrOutput, DRR_GOLDEN_DIR, FREQUENCY_PERIOD);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void quantityFrequency_filterDeref_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(drrOutput, DRR_GOLDEN_DIR, QUANTITY_FREQUENCY);
    }

    // ==== Positive-content locks (revert-RED). ====

    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapPartyRoleList_wrapsBareMultiInvocationInMapperC() {
        String gen = gen(cdm6Output, MAP_PARTY_ROLE_LIST);
        assertTrue(gen.contains(
                ".mapItemToList(item -> MapperC.<PartyRole>of(mapRelatedPartyToPartyRole.evaluate(item.get())))"),
                "the *ToList body must wrap the bare multi-fn invocation in MapperC.<PartyRole>of");
    }

    // ==== Green-safety locks (pass with AND without the facets). ====

    @Test
    @EnabledIf("cdm6CellAvailable")
    void priceQuantityTriangulation_greenStaysByteIdentical() throws IOException {
        assertByteMatchesGolden(cdm6Output, CDM6_GOLDEN_DIR, PRICE_QUANTITY_TRIANGULATION_GREEN);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void notionalLeg_byteMatchesGolden() throws IOException {
        // REPOINTED (PR #398, the #284/#295 lock convention this note prescribed): facet
        // fnNotionalTogetherRestructure flipped NotionalLeg byte-identical — the pre-#398
        // premise (the fork's divergent per-use-deref hazard inside the still-waivered
        // creditDefaultNotional ctor body) is GONE, and golden's OWN wrapper-preserving
        // consumer now carries the two "Type coercion" derefs the old region-scoped
        // assertFalse guarded against. The whole-file byte lock subsumes it.
        assertByteMatchesGolden(drrOutput, DRR_GOLDEN_DIR, NOTIONAL_LEG);
    }

    private static String gen(Map<String, String> output, String path) {
        assertNotNull(output, "generation did not run for the cell of " + path);
        String gen = output.get(path);
        assertNotNull(gen, "missing generated file: " + path);
        return gen;
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
