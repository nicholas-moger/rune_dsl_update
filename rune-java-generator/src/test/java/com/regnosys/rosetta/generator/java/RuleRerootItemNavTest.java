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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static com.regnosys.rosetta.testutil.GenerationErrors.assertNoGenerationErrors;

/**
 * PR #282 — facet rerootItemNav: ONE green-safe, RULE-SCOPED (FUNCTION-byte-neutral) GENERATOR
 * mechanism, 22 drr POJO Rule byte flips. The user-chosen reroot pivot after the lambda-interior
 * then-hoist spike (the original #282 lever) came up a dry hole (it flipped 0 of 172 block-rendered
 * files — every then-carrier is co-occupied; diffing the residual surfaced this clean replacement).
 *
 * <p><b>The bug (the #279 head→feature cascade at the rule-body map-lambda implicit-item seat).</b>
 * A disguised navigation HEAD inside a drr rule-body {@code mapSingleToItem} lambda — a 2-name
 * {@code head -> feature} chain ({@code generalTerms -> indexReferenceInformation}) or a 1-name bare
 * item feature ({@code cashSettlementTerms} / {@code partyId}) — whose implicit ITEM type is
 * THEN-piped (the rule-body then-chain stage shape) rendered the witness-less, non-compiling bare
 * {@code MapperS.of(<head>)} / {@code MapperC.of(<head>)} (the variable-path fallthrough). The cause:
 * {@code NavigationHandler.implicitItemDataType}'s structural walk does not resolve a THEN-piped
 * lambda argument, so {@code ReferenceHandler.synthesizeImplicitItemChain} /
 * {@code synthesizeImplicitItemBareNav} declined and the head fell to the bare local.
 *
 * <p><b>The fix (GENERATOR-only, 2 source files).</b> New
 * {@code NavigationHandler.implicitItemDataTypeOrInferred} adds an INFERRED-type fallback
 * ({@code gm.workspace().getInferredType(arg)} — EXACTLY the type
 * {@code FunctionExpressionRenderer.renderThenExtractSet} declares the upstream {@code thenArg}
 * from), so both reroot seats recover the item type and re-root
 * {@code item.<T>map(C)("getX", elem -> elem.getX())}; a widened {@code handle(RSymbolReference)}
 * gate routes a RULE-scoped {@code RAttribute}-bound bare item feature through
 * {@code synthesizeImplicitItemBareNav}; and the #255 {@code resolveLambdaVarName} branch names the
 * re-rooted nav's first lambda param from the item element type ({@code creditDefaultPayout}), not
 * the rule from-type ({@code transactionReportInstruction}). ALL RULE-SCOPED
 * ({@code findEnclosingRule}) → the FUNCTION tail + cdm/iso/fpml POJO stay byte-IDENTICAL.
 *
 * <p><b>Green-safe by construction.</b> The bare {@code MapperS.of(<head>)} / {@code MapperC.of(<head>)}
 * references a local that does not exist (the item feature has no such variable), so it never
 * compiled — every carrier was an already-waivered NON_COMPILING mismatch; re-rooting can only flip a
 * waivered file. byte-oracle / stash-baseline measured exactly 22 flips (drr POJO 34 now-matching =
 * 22 mine + 12 stale waivers separated out by the same-session clean-main re-dump), 0 within-waiver
 * regressions, 134 toward-golden churn (the reroot pre-stages the co-occupied meta-deref / cardinality
 * residual); all FUNCTION cells (cdm5 81 / cdm6 236 / drr 207) + cdm/iso/fpml POJO byte-IDENTICAL.
 *
 * <p>Anchors are WHOLE-FILE byte comparisons of the generated drr output against the frozen goldens
 * (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}. REVERT-VERIFIED RED.
 */
class RuleRerootItemNavTest {

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

    // ==== Flip locks (revert-RED): rerootItemNav carriers now byte-match golden. ====

    /**
     * Flip — 2-name CHAIN reroot + lambda-var naming: {@code generalTerms -> indexReferenceInformation
     * -> indexAnnexVersion} re-roots {@code item.<GeneralTerms>map("getGeneralTerms",
     * creditDefaultPayout -> creditDefaultPayout.getGeneralTerms())…}; the re-rooted first lambda param
     * is named from the THEN-piped item element type ({@code creditDefaultPayout}), not the rule
     * from-type ({@code transactionReportInstruction}) — both the chain seat and the #255 lambda-var
     * naming seat share the inferred-type fallback.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void version_chainReroot_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/common/trade/index/reports/VersionRule.java");
    }

    /** Flip — the sibling 2-name CHAIN reroot ({@code generalTerms -> indexReferenceInformation -> indexSeries}). */
    @Test
    @EnabledIf("drrCellAvailable")
    void series_chainReroot_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/common/trade/index/reports/SeriesRule.java");
    }

    /**
     * Flip — 1-name bare-RAttribute reroot, MULTI cardinality: a bare {@code cashSettlementTerms} item
     * feature (linker-bound to a MULTI {@code RAttribute}) re-roots {@code item.<CashSettlementTerms>
     * mapC("getCashSettlementTerms", settlementTerms -> settlementTerms.getCashSettlementTerms())}
     * instead of the bogus {@code MapperC.of(cashSettlementTerms)} — exercises the widened
     * {@code handle(RSymbolReference)} RAttribute gate.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void fixingDateLeg1_bareRAttributeReroot_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/common/trade/datetime/reports/FixingDateLeg1Rule.java");
    }

    /**
     * Flip — 1-name bare-RAttribute reroot at an EVALUATE-ARG seat: {@code partyLei.evaluate(partyId)}
     * re-roots its bare {@code partyId} arg {@code item.<PartyIdentifier>mapC("getPartyId", …).getMulti()}.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void newSdrIdentifier_bareRAttributeArg_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden("drr/regulation/common/trade/link/reports/NewSDRIdentifierRule.java");
    }

    /** Flip — an iosco cde version1 bare reroot carrier. */
    @Test
    @EnabledIf("drrCellAvailable")
    void packageTransactionPriceNoFormat_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/standards/iosco/cde/version1/price/reports/PackageTransactionPriceNoFormatRule.java");
    }

    /** Flip — an iosco cde base bare reroot carrier. */
    @Test
    @EnabledIf("drrCellAvailable")
    void quantityUnitOfMeasure_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/standards/iosco/cde/base/quantity/reports/QuantityUnitOfMeasureRule.java");
    }

    // ==== Gate locks (revert-RED): the rule-scope + facet boundary. ====

    /**
     * Gate lock — CONVERTED at PR #341 (facet filterPredicateOperandNavReRoot): the #282
     * "documented follow-on" LANDED — the reroot is un-rule-scoped, so the FUNCTION-body bare
     * item-feature navs ({@code security} / {@code loan}) now re-root onto the implicit item
     * exactly like the rule path ({@code item.<Security>map("getSecurity", _product ->
     * _product.getSecurity()).map("getProductIdentifier", …)} — golden's fragment), and the
     * pre-#341 bare non-compiling {@code MapperS.of(security).map(…)} form is GONE. The file
     * STAYS divergent on the co-occupied then-chain/control-flow tail (a partial heal,
     * regscan341 ratio-TOWARD 0.7234 → 0.7324); the whole-file flip converts this lock again
     * when that facet lands. WITNESS refreshed at PR #348 (facet
     * choiceOptionTrailingWitness): the re-rooted nav's trailing step now carries golden's
     * {@code <ReferenceWithMetaProductIdentifier>mapC} witness + typed lambda var (the
     * receiver-type walk resolves the re-rooted bare name; fragment golden-verified
     * content-TOWARD at the #348 cp6d checkpoint).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void getUnderlierProductIdentifier_function_rerootFires() {
        assertNotNull(drrOutput, "drr generation did not run — corpus unavailable?");
        String gen = drrOutput.get("drr/regulation/common/functions/GetUnderlierProductIdentifier.java");
        assertNotNull(gen, "GetUnderlierProductIdentifier not generated");
        assertTrue(gen.contains("item.<Security>map(\"getSecurity\", _product -> _product.getSecurity())"
                        + ".<ReferenceWithMetaProductIdentifier>mapC(\"getProductIdentifier\""),
                "the FUNCTION-body bare item-feature nav must re-root onto the implicit item "
                + "(the #341 reroot, with its #348 trailing witness — golden's fragment)");
        assertTrue(!gen.contains("MapperS.of(security).map(\"getProductIdentifier\""),
                "the pre-#341 bare non-compiling form must be gone");
    }

    /**
     * Facet-boundary lock — co-occupied META-DEREF residual: {@code Counterparty2IdentifierTypeRule}
     * (iosco cde version1) re-roots its bare {@code partyId} chain onto the implicit item (the reroot
     * fires correctly). UPDATED at PR #330: the "follow-on facet" this lock named — golden's
     * {@code item.<Party>map("Type coercion", referenceWithMetaParty -> … getValue())} deref BEFORE
     * the {@code getPartyId} — now FIRES too (the #330 recovery-walker extract-descent +
     * disguised-chain-leaf arms), so the {@code mapC("getPartyId"} hangs off the deref'd receiver
     * rather than {@code item} directly, and the carrier moved 0.8507 → 0.9046 toward golden.
     * CONVERTED at PR #380 (facet seqThenChainDecomp, D4a — the #329 lock-graduation law): the
     * "residual block-lambda mechanisms" LANDED (the F5 base admit + the restructure-window
     * ladder transparency + the filter-predicate operand deref), so the carrier is byte-identical.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void counterparty2IdentifierType_rerootAndDerefFire_byteMatchesGolden() throws IOException {
        assertNotNull(drrOutput, "drr generation did not run — corpus unavailable?");
        String path = "drr/standards/iosco/cde/version1/party/reports/Counterparty2IdentifierTypeRule.java";
        String gen = drrOutput.get(path);
        assertNotNull(gen, "Counterparty2IdentifierTypeRule not generated");
        assertTrue(gen.contains("item.<Party>map(\"Type coercion\""),
                "golden's meta-deref must fire before the partyId nav (the #330 recovery extension)");
        assertTrue(gen.contains(".<PartyIdentifier>mapC(\"getPartyId\""),
                "the re-rooted partyId chain must nav off the deref'd receiver");
        assertByteMatchesGolden(path);
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
                + path + " (PR #282 rerootItemNav).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
