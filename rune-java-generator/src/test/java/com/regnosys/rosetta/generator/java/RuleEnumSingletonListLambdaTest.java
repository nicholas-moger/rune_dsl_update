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
 * Anchor for facet {@code enumSingletonListLambda} (PR #319): the #269 (B) {@code enumArgCollections}
 * law ({@code ReferenceHandler.tryEnumSingletonListArg}) extended from the STATEMENT_SINK to the
 * LAMBDA_CHANNEL seat. A PRESENT enum CONSTANT ({@code SupervisoryBodyEnum.CFTC}) passed to a MULTI
 * ({@code 0..*}) callee parameter INSIDE a {@code mapSingleToItem} lambda body is coerced item→list by
 * golden — it block-converts the lambda and hoists {@code final SupervisoryBodyEnum supervisoryBodyEnum
 * = SupervisoryBodyEnum.CFTC;} at the lambda top + null-guards the arg into
 * {@code (supervisoryBodyEnum == null ? Collections.<SupervisoryBodyEnum>emptyList() :
 * Collections.singletonList(supervisoryBodyEnum))} + the {@code java.util.Collections} import. The fork
 * passed the bare {@code SupervisoryBodyEnum.CFTC} (a single enum where a List is wanted —
 * NON_COMPILING). When no statement-hoist sink is reachable (a lambda interior stops the walk) but the
 * arg sits DIRECTLY in a drainable map/extract lambda body ({@code HandlerHelper.isInsideDrainableMapLambda}),
 * the enum decl now registers on the lambda-body scope ({@code registerPendingLambdaHoist}), which
 * {@code CollectionHandler.compileLambda} drains into the brace block — the #301/#312 LAMBDA_CHANNEL
 * pattern.
 *
 * <p>The enum-constant gate is also TIGHTENED (load-bearing for BOTH routes): it now requires a
 * GENUINE enum value (a bare value already resolved by {@code tryBareEnumArg}, OR an {@code REnumValueRef}
 * whose {@code enumeration()} is PRESENT). A disguised 2-name nav chain ({@code head -> feature}, e.g.
 * DTCC's {@code tradeForEvent -> tradeIdentifier} into a MULTI {@code TradeIdentifier} param) ALSO
 * parses as an {@code REnumValueRef} but with an EMPTY {@code enumeration()} (the #288/#291 lineage) —
 * golden passes such a multi nav chain directly via {@code .getMulti()}, never singletonList-coerced.
 * The STATEMENT_SINK route masked this over-fire (its disguised-chain carriers are inside lambdas,
 * {@code sink == null} → declined pre-#319); the LAMBDA_CHANNEL surfaced it (the regscan caught
 * {@code DTCC_TradeParty1TransactionIDRule} moving AWAY), so the {@code enumeration()} check is load-bearing.
 *
 * <p>RULE-scoped ({@code findEnclosingRule}) → FUNCTION-byte-neutral (#232; cdm5 79 / cdm6 232 / drr 206
 * FUNCTION mismatch UNCHANGED, cdm/iso/fpml POJO byte-IDENTICAL). Green-safe by construction: 0 goldens
 * carry the bare {@code evaluate(…, <Enum>.<CONST>)} form into a multi param (golden always coerces),
 * so the fork's bare form never byte-matched a green file; every carrier is an already-waivered
 * NON_COMPILING mismatch.
 *
 * <p>Anchors are WHOLE-FILE byte comparisons of the generated drr output against the frozen goldens
 * (newline-normalized), generated through the REAL {@link D11CorpusRegressionTest#loadCellCorpusCached}.
 * REVERT-VERIFIED RED: the flip lock + the positive-content lock fail against pre-#319
 * source; the CONVERTED ASIC lock fails against pre-#340 source (verified at the #340
 * stash-baseline gate); the {@code DTCC_TradeParty1TransactionID} disguised-chain
 * green-safety lock passes either way (it asserts the arm DECLINES, which holds on clean
 * source too). The #312 conditional-arm exclusion this class originally locked was CLOSED
 * at PR #340 (facet {@code enumSingletonListCondArm} — the effective-else per-arm drains +
 * the blessed-conditional handshake), flipping the ASIC/CFTC/HKMA/MAS margin+valuation
 * UniqueTransactionIdentifier family; see {@code EnumCondArmGetValueComposeTest}.
 */
class RuleEnumSingletonListLambdaTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    // Flip carrier: SupervisoryBodyEnum.CFTC into a MULTI callee param inside a mapSingleToItem lambda
    // (a simple-expression body, no conditional) — golden block-converts + hoists + singletonList-coerces.
    private static final String UTI_CFTC_TRADE =
            "drr/regulation/cftc/rewrite/trade/reports/UniqueTransactionIdentifierRule.java";
    // Green-safety (disguised-chain decline): the getInternalId first arg is a disguised
    // `tradeForEvent -> tradeIdentifier` chain (an REnumValueRef with empty enumeration()) into a MULTI
    // TradeIdentifier param — the tightened gate declines it (golden passes it directly via .getMulti()).
    private static final String DTCC_TRADE_PARTY1_CFTC =
            "drr/regulation/cftc/rewrite/dtcc/reports/DTCC_TradeParty1TransactionIDRule.java";
    // CONVERTED at PR #340: was the #312 conditional-arm decline lock (the enum inside an
    // `if (exists(...))` conditional stayed bare); the enumSingletonListCondArm facet opened
    // the seat and the variant now byte-matches golden — the FIFTH deferred-decline-lock flip.
    private static final String ASIC_UTI_MARGIN =
            "drr/regulation/asic/rewrite/margin/reports/ASICUniqueTransactionIdentifierRule.java";

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

    @Test
    @EnabledIf("drrCellAvailable")
    void uniqueTransactionIdentifierCftc_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(UTI_CFTC_TRADE);
    }

    // ==== Positive-content lock (revert-RED). ====

    /**
     * UniqueTransactionIdentifier cftc: the mapSingleToItem lambda is block-converted with the enum
     * hoisted {@code final SupervisoryBodyEnum supervisoryBodyEnum = SupervisoryBodyEnum.CFTC;} at the
     * top + the singletonList coercion + the {@code java.util.Collections} import — NOT the fork's bare
     * {@code evaluate(item.get(), SupervisoryBodyEnum.CFTC)}.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void uniqueTransactionIdentifierCftc_enumHoistedAndSingletonListCoerced() {
        String gen = gen(UTI_CFTC_TRADE);
        assertTrue(gen.contains("import java.util.Collections;"),
                "Expected the java.util.Collections import");
        assertTrue(gen.contains("final SupervisoryBodyEnum supervisoryBodyEnum = SupervisoryBodyEnum.CFTC;"),
                "Expected the enum constant hoisted to a final local at the lambda top (LAMBDA_CHANNEL)");
        assertTrue(gen.contains("(supervisoryBodyEnum == null ? Collections.<SupervisoryBodyEnum>emptyList()"
                        + " : Collections.singletonList(supervisoryBodyEnum))"),
                "Expected the enum arg null-guarded into the singletonList item→list coercion");
        assertTrue(!gen.contains("getUniqueTransactionIdentifier.evaluate(item.get(), SupervisoryBodyEnum.CFTC)"),
                "The fork's bare enum-into-multi arg must be gone");
    }

    // ==== Green-safety / decline locks (pass on clean source too). ====

    /**
     * Green-safety (disguised-chain decline): {@code DTCC_TradeParty1TransactionIDRule}'s first arg to
     * {@code getInternalId} is a disguised {@code tradeForEvent -> tradeIdentifier} chain (an
     * {@code REnumValueRef} with EMPTY {@code enumeration()}) producing a MULTI
     * {@code List<TradeIdentifier>}. The tightened enum-constant gate declines it — the arg is passed
     * directly via {@code .getMulti()}, NOT wrapped in a bogus {@code final TradeIdentifier tradeIdentifier
     * = …; Collections.singletonList(tradeIdentifier)}. The file itself stays divergent (the golden uses a
     * {@code _thenArg} scope escape the fork does not emit, #313), so only the decline is asserted, not a
     * byte-match. Passes on clean source too (which never had the bogus form).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void dtccTradeParty1_disguisedChainNotSingletonListCoerced() {
        String gen = gen(DTCC_TRADE_PARTY1_CFTC);
        assertTrue(!gen.contains("final TradeIdentifier tradeIdentifier ="),
                "The disguised nav chain must NOT be hoisted to a singletonList local (over-fire guard)");
        assertTrue(!gen.contains("Collections.singletonList(tradeIdentifier)"),
                "The disguised multi nav chain must NOT be singletonList-coerced");
        assertTrue(gen.contains(".<TradeIdentifier>mapC(\"getTradeIdentifier\","
                        + " trade -> trade.getTradeIdentifier()).getMulti(), item.<ReportingSide>"),
                "Expected the multi nav chain passed directly via .getMulti()");
    }

    /**
     * CONVERTED at PR #340 (facet {@code enumSingletonListCondArm} — the FIFTH
     * deferred-decline-lock flip): this lock previously asserted the #312 conditional-arm
     * DECLINE ("the enum stays bare — no per-arm drain can place the hoist"). PR #339 built
     * the per-arm drains and PR #340 opened the seat with the identity-keyed
     * blessed-conditional handshake, so the ASIC margin variant now byte-matches golden
     * (the numbered {@code supervisoryBodyEnum0/1} per-occurrence hoists + singletonList
     * coercions). The whole-file flip locks live in {@code EnumCondArmGetValueComposeTest};
     * this breadcrumb pins the byte equality at the original #319 seat.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void asicUniqueTransactionIdentifier_conditionalArmEnumNowCoerced() throws IOException {
        assertByteMatchesGolden(ASIC_UTI_MARGIN);
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
                + path + " (PR #319 enumSingletonListLambda).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
