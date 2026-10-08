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
 * PR #295 — facet conditionalMetaJoin: ONE green-safe GENERATOR mechanism, 2 drr POJO Rule byte
 * flips (the #280 {@code bareSymInvoke} meta-leaf residual — the user-chosen aim-bigger foundation).
 *
 * <p><b>The mechanism.</b> #280 made a disguised bare-FUNCTION navigation receiver
 * ({@code func -> feature}, an {@link com.regnosys.rosetta.ast.expressions.references.REnumValueRef}
 * whose {@code resolvedSymbol} is an {@code RFunction}) render
 * {@code MapperS.of(<fn>.evaluate(item.get())).<T>map("getFeature", …)}, but DECLINED when the
 * navigated leaf is META-annotated (FieldWithMetaX / ReferenceWithMetaX) — golden derefs such a leaf
 * {@code .<X>map("Type coercion", w -> w == null ? null : w.getValue())} and the un-derefed wrapper
 * leaked onto the enclosing {@code thenArg} declaration. The deref decision is NON-LOCAL: golden
 * derefs the meta arm ONLY in a MIXED conditional — where a SIBLING arm forces the bare common type
 * (the upstream conditional meta-type-join) — and KEEPS the wrapper in a HOMOGENEOUS-meta conditional
 * (DTCC_Leg1). This PR resolves it at the conditional-ladder seat where ALL arms are visible.
 *
 * <p><b>Where it fires.</b> {@code CollectionHandler.compileLadderConditionalBlock} pre-scans the
 * rung then-branches (AST): a MIXED-BARESYM ladder has some baresym arm navigating a BARE leaf AND
 * some a META leaf (leaf meta-kind via {@code ReferenceHandler.baresymFunctionLeafMetaKind} — the
 * SAME leaf resolution as the #280 gate). When mixed it (a) sets a scope flag
 * ({@code JavaStatementScope.pushMetaLeafBaresymAllowed}) that un-declines the #280 meta-leaf gate
 * while the arms compile (so the meta arm fires its wrapper), and (b) derefs every wrapper arm via
 * {@code ExpressionCompiler.coerceNavigationReceiver} (a no-op on a bare arm) so all arms agree on
 * the bare value. The thenArg DECLARATION follows via a matching #144-gate in
 * {@code FunctionExpressionRenderer} (skip the meta-wrapper recovery when the rendered block carries
 * the return-terminal deref marker).
 *
 * <p><b>Green-safety (regscan: 2 flipped-out / 0 within-waiver regressions / 0 churn).</b> The
 * mechanism is confined to the mixed-baresym LADDER seat — a HOMOGENEOUS-meta ladder, a non-baresym
 * (RFeatureCall-chain) ladder, a getOrDefault operand, and a single conditional ALL keep the #280
 * decline (no flag, no deref). The pre-fix bare type literal never compiled, so only already-waivered
 * files are touched. byte-oracle / stash-baseline measured exactly 2 flips (drr POJO 425 → 423; clean
 * source = 12 stale waivers, comm -23 = the 2 carriers); ALL FUNCTION cells (cdm5 81 / cdm6 236 /
 * drr 207) + cdm/iso/fpml POJO byte-IDENTICAL.
 *
 * <p>Anchors are WHOLE-FILE byte comparisons of the generated drr Rule output against the frozen
 * goldens (newline-normalized), generated through the REAL
 * {@link D11CorpusRegressionTest#loadCellCorpusCached}. REVERT-VERIFIED RED for the 2 flip locks.
 */
class RuleConditionalMetaJoinTest {

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

    // ==== Flip locks (revert-RED): mixed-baresym conditional-meta-join carriers byte-match golden. ====

    /**
     * Flip: a MIXED-baresym ladder. {@code MasterAgreementType}'s first then is
     * {@code if exists(tradeForEvent) then tradeForEvent -> contractDetails else if
     * exists(positionForEvent) then positionForEvent -> contractDetails else <empty>}. Arm 1
     * navigates {@code Trade.contractDetails} (NON-meta, bare {@code ContractDetails}); arm 2 navigates
     * {@code CounterpartyPosition.contractDetails} ({@code ReferenceWithMetaContractDetails}, meta) —
     * so the ladder is mixed-baresym. The meta arm fires its wrapper + is deref'd
     * {@code .<ContractDetails>map("Type coercion", referenceWithMetaContractDetails -> … getValue())},
     * and {@code thenArg0} declares the bare {@code MapperS<ContractDetails>} (NOT the wrapper).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void masterAgreementType_mixedBaresymLadder_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/common/trade/contract/reports/MasterAgreementTypeRule.java");
    }

    /** Flip: the sibling carrier, identical {@code contractDetails} mixed-baresym ladder shape. */
    @Test
    @EnabledIf("drrCellAvailable")
    void otherMasterAgreementType_mixedBaresymLadder_byteMatchesGolden() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/common/trade/contract/reports/OtherMasterAgreementTypeRule.java");
    }

    // ==== Green-safety / gate locks: the mechanism does NOT over-fire outside the mixed-baresym
    //      ladder seat (homogeneous ladder / getOrDefault operand). ====

    /**
     * Green-safety lock — a HOMOGENEOUS, NON-baresym ladder is NOT deref'd. {@code UnderlyingIdOther}
     * (asic) is a ladder whose arms are {@code RFeatureCall} chains
     * ({@code economicTermsForProduct -> … -> indexName}, a {@code FieldWithMetaString} leaf) — NOT
     * 2-name baresym {@code func -> feature} navs, so {@code baresymFunctionLeafMetaKind} returns null
     * for them and the ladder is not mixed-baresym. The terminal {@code getIndexName} meta arm must
     * therefore KEEP its wrapper (golden does too — homogeneous): the spurious
     * {@code .getIndexName())…<String>map("Type coercion", …)} deref that an un-scoped meta-join would
     * add must be ABSENT (it would be a within-waiver regression — measured +4 diff-lines in an
     * earlier un-scoped attempt).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void underlyingIdOther_homogeneousNonBaresymLadder_notDerefed() {
        assertNotNull(drrOutput, "drr generation did not run — corpus unavailable?");
        String gen = drrOutput.get(
                "drr/regulation/asic/rewrite/trade/reports/UnderlyingIdOtherRule.java");
        assertNotNull(gen, "UnderlyingIdOtherRule (asic) not generated");
        assertFalse(gen.contains(".getIndexName()).<String>map(\"Type coercion\""),
                "a homogeneous, non-baresym (RFeatureCall-chain) ladder must NOT be deref'd — the "
                + "conditional meta-join is confined to the mixed-BARESYM ladder seat");
    }

    /**
     * FLIPPED at PR #371 (facet midChainCallableMetaDeref) — the deferred-carrier law: this lock's
     * carrier WAS the flip candidate. History: originally asserted {@code CollateralPortfolioIndicator}'s
     * getOrDefault operand kept the #280-declined bare form; REPOINTED at PR #296 to
     * {@code CollateralPortfolioCode}'s plain-nav meta baresym ({@code PositionForEvent -> collateral
     * -> …} — a MID-CHAIN meta leaf), which kept the bare type literal
     * {@code MapperS.of(PositionForEvent)} while the #295/#296 fixes stayed confined to their seats.
     * PR #371 IS the plain-nav follow-on the #296 javadoc anticipated: a MID-CHAIN meta leaf (the EVR
     * is the receiver of a further {@link com.regnosys.rosetta.ast.expressions.references.RFeatureCall}
     * hop) now fires the callable synthesis WITH the wrapper witness, and the continuation hop's
     * receiver coercion derefs inline ({@code .<Collateral>map("Type coercion",
     * referenceWithMetaCollateral0 -> referenceWithMetaCollateral0 == null ? null :
     * referenceWithMetaCollateral0.getValue())} — the method-numbered params). The file is byte-golden
     * post-#371; the lock now asserts the FIRED form and the bare literal's ABSENCE. TERMINAL meta
     * leaves keep the #280/#370-cp4 decline (a separate concern this widening does not touch).
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void collateralPortfolioCode_midChainMetaLeaf_firesWithInlineDeref() {
        assertNotNull(drrOutput, "drr generation did not run — corpus unavailable?");
        String gen = drrOutput.get(
                "drr/standards/iosco/cde/version1/collateral/reports/CollateralPortfolioCodeRule.java");
        assertNotNull(gen, "CollateralPortfolioCodeRule not generated");
        assertFalse(gen.contains("MapperS.of(PositionForEvent)"),
                "the #280-declined bare type literal must be GONE — the PR #371 mid-chain "
                + "callable-meta admission fires the synthesis (FLIPPED-at-#371)");
        assertTrue(gen.contains("MapperS.of(positionForEvent.evaluate(item.get()))"),
                "the disguised PositionForEvent head must render the callable invocation "
                + "(PR #371 midChainCallableMetaDeref)");
        assertTrue(gen.contains("referenceWithMetaCollateral0 == null ? null : "
                        + "referenceWithMetaCollateral0.getValue()"),
                "the mid-chain meta hop must deref inline through the numbered "
                + "WrappedItemCoercer params (PR #371 midChainCallableMetaDeref)");
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
                + path + " (PR #295 conditionalMetaJoin).");
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
