package com.regnosys.rosetta.generator.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.java.enums.EnumGenerator;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.object.ChoiceObjectGenerator;
import com.regnosys.rosetta.generator.java.object.ModelObjectGenerator;
import com.regnosys.rosetta.generator.java.reports.ReportGenerator;
import com.regnosys.rosetta.generator.java.reports.RuleGenerator;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import static com.regnosys.rosetta.testutil.GenerationErrors.assertNoGenerationErrors;

/**
 * PR #371 — the meta-deref/collapse-rewrap quintet: 12 byte flips (1 drr FUNCTION
 * + 11 drr POJO Rules) + 0 new / 0 away (regscan371: 12/0/0; 6 common movers all
 * TOWARD — SingleOrUpperAndLowerBarrierRule 22→14 dl, the
 * GetUnderlierProductIdentifier trio typed-empty, the 2 UnderlyingIdentification
 * F-D rides).
 *
 * <p><b>F-A navOnlyElementDeclRewrap + collapsedOnlyElementConversionRewrap</b>
 * (CollectionHandler + ConversionHandler): the #260/#359 deep-then decl collapse
 * re-wrap widens to the k==0 BASE level and NAV-rooted ONLY_ELEMENT collapse
 * bodies — the 2-name nav parses as a DISGUISED REnumValueRef (the P371A probe:
 * all 8 trio seats), so the predicate admits RFeatureCall + REnumValueRef with
 * the rendered-shape guards as arbiter ({@code final MapperS<OptionPayout>
 * _thenArg2 = MapperS.of(item.<Payout>map(…).<OptionPayout>mapC(…).get());});
 * a to-enum whose argument is the bare-item (distinct) only-element collapse
 * wraps the receiver before the chain ({@code MapperS.of(distinct(thenArg2)
 * .get()).checkedMap("to-enum", …)}): LoadTypeRule esma/fca +
 * DTCC_LoadTypeRule csa.
 *
 * <p><b>F-B filterTerminalMetaTypeStamp</b> (FunctionExpressionRenderer): the
 * #362 collapse stamp's FILTER sibling — a TYPE-LESS bare-item FILTER terminal
 * is element-AND-kind preserving, so the previous level's typed prevRef (the ITE
 * hoist's own {@code MapperS<FieldWithMetaString>} decl) is render truth; the
 * whole-output deref fires through the COMPILED-type route (the #361-cp6-REFUTED
 * AST walk stays dead): {@code final FieldWithMetaString fieldWithMetaString =
 * ifThenElseResult\n\t.filterSingleNullSafe(…).get();} + the if-null block, and
 * the in-lambda {@code _fieldWithMetaString} escapes ride the #329 finalization:
 * PriorUtiRule + PriorUtiProprietaryRule esma, PriorUTIRule +
 * PriorUTIProprietaryRule fca.
 *
 * <p><b>F-D midChainCallableMetaDeref</b> (ReferenceHandler): the #359 terminal
 * admission's MID-CHAIN sibling — a meta leaf whose EVR is the RECEIVER of a
 * further hop fires the callable synthesis WITH the wrapper witness; the
 * continuation hop's receiver coercion derefs inline through the method-numbered
 * WrappedItemCoercer params ({@code .<Collateral>map("Type coercion",
 * referenceWithMetaCollateral0 -> … .getValue())}); terminal meta leaves keep
 * the #280/#370-cp4 decline: CollateralPortfolioCodeRule iosco.
 *
 * <p><b>F-C thenChainLadderArmMetaJoin + ladderEnumSingletonArg</b>
 * (CollectionHandler): a THEN-CHAIN or item-rooted NAV arm's terminal wrapper
 * joins META evidence via the #331 walker (null walk = NO evidence; the deref
 * pass stays COMPILED-TYPE-gated) so the mixed ladder derefs meta arms in-arm
 * ({@code .<String>map("Type coercion", fieldWithMetaString ->
 * fieldWithMetaString.getValue())}) and the block decl retypes
 * {@code MapperC<String>}; the #340 EnumConstArgHoist channel extends to the
 * LADDER seat (per-rung bless; cond-position hoists render before the
 * {@code if (}, arm-position hoists in-branch — {@code supervisoryBodyEnum0/1}):
 * EsmaUtiRule + UTIFCAValueRule + HKMAUniqueTransactionIdentifierRule (the
 * family bonus).
 *
 * <p><b>F-E deepThenIteArmContinuation + deepThenIteEmptyElse +
 * condListMetaElemDeref</b> (ControlFlowHandler): the #351 deep-seat ITE's
 * multi-line arm re-anchors at assignment+1 (golden's uniform convention); the
 * P358I materialized-empty else takes the typed {@code Mapper*.<T>ofNull()};
 * a Mapper-chain arm whose element is a META wrapper of the list element derefs
 * elementwise before {@code .getMulti()}:
 * Create_ValuationDetailsFromReportableEvent drr FN.
 *
 * <p>Whole-file byte comparisons run through the REAL D11 generation paths and
 * revert RED without the facets. The negative witnesses are load-bearing per the
 * witness-uniqueness law (OCCURRENCE counts, never line counts): every token
 * below was occurrence-counted in its PRE gen (f-probe-370post — counts stated
 * per witness) and 0 in its golden — the flips REMOVE them.
 */
class MetaDerefCollapseRewrapComposeTest {

    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    /** The drr FUNCTION flip carrier (F-E). */
    private static final String CREATE_VALUATION_DETAILS =
            "drr/enrichment/common/valuation/functions/Create_ValuationDetailsFromReportableEvent.java";

    /** The 11 drr POJO Rule flip carriers (F-A ×3 + F-B ×4 + F-C ×3 + F-D). */
    private static final String[] DRR_RULES = {
            "drr/regulation/esma/emir/refit/trade/reports/LoadTypeRule.java",
            "drr/regulation/fca/ukemir/refit/trade/reports/LoadTypeRule.java",
            "drr/regulation/csa/rewrite/dtcc/reports/DTCC_LoadTypeRule.java",
            "drr/regulation/esma/emir/refit/trade/reports/PriorUtiRule.java",
            "drr/regulation/esma/emir/refit/trade/reports/PriorUtiProprietaryRule.java",
            "drr/regulation/fca/ukemir/refit/trade/reports/PriorUTIRule.java",
            "drr/regulation/fca/ukemir/refit/trade/reports/PriorUTIProprietaryRule.java",
            "drr/regulation/esma/emir/refit/trade/reports/EsmaUtiRule.java",
            "drr/regulation/fca/ukemir/refit/trade/reports/UTIFCAValueRule.java",
            "drr/regulation/hkma/rewrite/valuation/reports/HKMAUniqueTransactionIdentifierRule.java",
            "drr/standards/iosco/cde/version1/collateral/reports/CollateralPortfolioCodeRule.java",
    };

    private static Map<String, String> drrFnOutput;
    private static Map<String, String> drrRuleOutput;

    static boolean drrCellAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (drrCellAvailable()) {
            D11CorpusRegressionTest.CellSpec drr =
                    new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", DRR_CELL_ROOT);
            drrFnOutput = generateFunctions(drr);
            drrRuleOutput = generateRuleKinds(drr);
        }
    }

    /** The REAL D11 FUNCTION-kind generation path (function_comparison). */
    private static Map<String, String> generateFunctions(D11CorpusRegressionTest.CellSpec cell)
            throws IOException {
        var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(),
                D11CorpusRegressionTest.emissionFilter(cell));
        var typeUtil = new JavaTypeUtil();
        var funcGen = new FunctionGenerator(gm, new JavaTypeTranslator(typeUtil), typeUtil);
        Map<String, String> output = new LinkedHashMap<>();
        var errors = funcGen.generateWithErrors(output);
        assertTrue(errors.isEmpty(),
                () -> "Function generation reported " + errors.size() + " error(s): " + errors);
        return output;
    }

    /** The REAL D11 rule-kind generation path (the drr POJO Rule carriers). */
    private static Map<String, String> generateRuleKinds(D11CorpusRegressionTest.CellSpec cell)
            throws IOException {
        var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(),
                D11CorpusRegressionTest.emissionFilter(cell),
                D11CorpusRegressionTest.readDoNotPrune(cell));
        var typeUtil = new JavaTypeUtil();
        var typeTranslator = new JavaTypeTranslator(typeUtil);
        var enumGen = new EnumGenerator(gm);
        var pojoGen = new ModelObjectGenerator(gm, typeTranslator, typeUtil);
        var choiceGen = new ChoiceObjectGenerator(gm, typeTranslator, typeUtil, pojoGen);
        var funcGen = new FunctionGenerator(gm, typeTranslator, typeUtil);
        var ruleGen = new RuleGenerator(gm, typeTranslator, funcGen);
        var reportGen = new ReportGenerator(gm, typeTranslator, funcGen);
        Map<String, String> output = new LinkedHashMap<>();
        for (RModel model : corpus.workspace().files()) {
            if (gm.shouldGenerate(model)) {
                String version = gm.version(model);
                assertNoGenerationErrors(enumGen.generateClasses(model, version, output));
                assertNoGenerationErrors(pojoGen.generateClasses(model, version, output));
                assertNoGenerationErrors(choiceGen.generateClasses(model, version, output));
                assertNoGenerationErrors(ruleGen.generateClasses(model, version, output));
                assertNoGenerationErrors(reportGen.generateClasses(model, version, output));
            }
        }
        return output;
    }

    // ---------------------------------------------------------- byte locks (12)

    @Test
    @EnabledIf("drrCellAvailable")
    void createValuationDetails_byteMatchesGolden() throws IOException {
        assertBytes(CREATE_VALUATION_DETAILS, gen(drrFnOutput, CREATE_VALUATION_DETAILS),
                DRR_GOLDEN_DIR);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void drrRuleFamily_byteMatchesGolden() throws IOException {
        for (String path : DRR_RULES) {
            assertBytes(path, gen(drrRuleOutput, path), DRR_GOLDEN_DIR);
        }
    }

    // ------------------------------------------------------- negative witnesses

    /**
     * The F-A decl-seat witnesses (LoadTypeRule esma): the UNWRAPPED collapse decl
     * values {@code _thenArg2 = item.<Payout>map(} and {@code thenArg4 =
     * item.<Payout>map(} counted EXACTLY 1 occurrence EACH in the PRE gen
     * (f-probe-370post) and 0 in the golden — the k==0 nav-only-element re-wrap
     * renders {@code = MapperS.of(item.<Payout>map(…).get())}.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void loadTypeEsma_unwrappedCollapseDeclGone() {
        String gen = gen(drrRuleOutput, DRR_RULES[0]);
        assertEquals(0, count(gen, "_thenArg2 = item.<Payout>map("),
                "The unwrapped _thenArg2 collapse decl must be gone (PRE count 1)");
        assertEquals(0, count(gen, "thenArg4 = item.<Payout>map("),
                "The unwrapped thenArg4 collapse decl must be gone (PRE count 1)");
    }

    /**
     * The F-A conversion-receiver witness (LoadTypeRule fca): the bare collapse
     * feeding checkedMap {@code output = distinct(thenArg2).get().checkedMap}
     * counted EXACTLY 1 occurrence in the PRE gen and 0 in the golden — the
     * conversion receiver re-wraps {@code MapperS.of(distinct(thenArg2).get())}.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void loadTypeFca_bareCheckedMapReceiverGone() {
        assertEquals(0, count(gen(drrRuleOutput, DRR_RULES[1]),
                        "output = distinct(thenArg2).get().checkedMap"),
                "The bare checkedMap receiver must be gone (PRE count 1)");
    }

    /**
     * The F-B output-assign witness (PriorUtiRule esma + PriorUTIRule fca): the
     * direct wrapper-into-output assignment {@code output = ifThenElseResult}
     * counted EXACTLY 1 occurrence in EACH PRE gen and 0 in the goldens — the
     * whole-output deref hoists {@code final FieldWithMetaString
     * fieldWithMetaString = ifThenElseResult…} + the if-null block.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void priorUti_directOutputAssignGone() {
        for (String path : new String[] {DRR_RULES[3], DRR_RULES[5]}) {
            assertEquals(0, count(gen(drrRuleOutput, path), "output = ifThenElseResult"),
                    () -> "The direct wrapper output assignment must be gone in " + path
                            + " (PRE count 1)");
        }
    }

    /**
     * The F-B naming-cascade witness (PriorUtiRule esma): the BARE in-lambda local
     * {@code final FieldWithMetaString fieldWithMetaString = item.get();} counted
     * EXACTLY 1 occurrence in the PRE gen and 0 in the golden — the method-level
     * hoist takes the bare name and the #329 finalization escapes the filter
     * block's local to {@code _fieldWithMetaString}.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void priorUtiEsma_bareFilterLocalEscapes() {
        assertEquals(0, count(gen(drrRuleOutput, DRR_RULES[3]),
                        "final FieldWithMetaString fieldWithMetaString = item.get();"),
                "The bare in-lambda filter local must escape to _fieldWithMetaString "
                + "(PRE count 1)");
    }

    /**
     * The F-D bare-literal witness (CollateralPortfolioCodeRule iosco): the
     * #280-declined bare type literal {@code MapperS.of(PositionForEvent)} counted
     * EXACTLY 2 occurrences in the PRE gen (the exists condition + the arm return)
     * and 0 in the golden — the mid-chain admission fires the callable synthesis.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void collateralPortfolioCode_bareTypeLiteralGone() {
        assertEquals(0, count(gen(drrRuleOutput, DRR_RULES[10]),
                        "MapperS.of(PositionForEvent)"),
                "The #280-declined bare type literal must be gone (PRE count 2)");
    }

    /**
     * The F-C2 bare-enum-arg witness (EsmaUtiRule esma): the RAW enum constant into
     * the MULTI callee param {@code , SupervisoryBodyEnum.ESMA,} counted EXACTLY 2
     * occurrences in the PRE gen (the condition + the arm) and 0 in the golden —
     * the ladder-seat EnumConstArgHoist channel hoists {@code supervisoryBodyEnum0/1}
     * and passes the null-guarded singletonList.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void esmaUti_bareEnumArgGone() {
        assertEquals(0, count(gen(drrRuleOutput, DRR_RULES[7]), ", SupervisoryBodyEnum.ESMA,"),
                "The bare enum arg into the multi param must be gone (PRE count 2)");
    }

    /**
     * The F-C2 fca sibling witness (UTIFCAValueRule): {@code , SupervisoryBodyEnum
     * .FCA,} counted EXACTLY 2 occurrences in the PRE gen and 0 in the golden.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void utiFcaValue_bareEnumArgGone() {
        assertEquals(0, count(gen(drrRuleOutput, DRR_RULES[8]), ", SupervisoryBodyEnum.FCA,"),
                "The bare enum arg into the multi param must be gone (PRE count 2)");
    }

    /**
     * The F-C1 decl-retype witness (EsmaUtiRule + HKMAUniqueTransactionIdentifierRule):
     * the wrapper-element block decl {@code final MapperC<FieldWithMetaString>
     * thenArg0} counted EXACTLY 1 occurrence in EACH PRE gen and 0 in the goldens —
     * the in-arm elementwise deref joins the ladder to the bare String and the #144
     * skip-gate types the decl {@code MapperC<String>}.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void mixedLadder_wrapperDeclRetyped() {
        for (String path : new String[] {DRR_RULES[7], DRR_RULES[9]}) {
            assertEquals(0, count(gen(drrRuleOutput, path),
                            "final MapperC<FieldWithMetaString> thenArg0"),
                    () -> "The wrapper-element block decl must retype to MapperC<String> in "
                            + path + " (PRE count 1)");
        }
    }

    /**
     * The F-E2 empty-else witness (Create_ValuationDetailsFromReportableEvent drr):
     * the witness-less compiled empty {@code thenArg = MapperC.of();} counted
     * EXACTLY 1 occurrence in the PRE gen and 0 in the golden — the materialized
     * empty else takes the typed {@code MapperC.<TradeIdentifier>ofNull()}.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void createValuationDetails_bareEmptyElseGone() {
        assertEquals(0, count(gen(drrFnOutput, CREATE_VALUATION_DETAILS),
                        "thenArg = MapperC.of();"),
                "The witness-less MapperC.of() empty else must be gone (PRE count 1)");
    }

    /**
     * The F-E3 elementwise-deref witness (Create_ValuationDetailsFromReportableEvent
     * drr): the underef'd meta collapse into the List
     * {@code getProductIdentifier()).getMulti()} counted EXACTLY 1 occurrence in the
     * PRE gen and 0 in the golden — the chain arm derefs elementwise
     * ({@code .<ProductIdentifier>map("Type coercion", …)}) before the coercion.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void createValuationDetails_underefGetMultiGone() {
        assertEquals(0, count(gen(drrFnOutput, CREATE_VALUATION_DETAILS),
                        "getProductIdentifier()).getMulti()"),
                "The underef'd wrapper .getMulti() must be gone (PRE count 1)");
    }

    // ------------------------------------------------------------------ helpers

    private static String gen(Map<String, String> output, String path) {
        assertNotNull(output, "generation did not run — corpus unavailable?");
        String gen = output.get(path);
        assertNotNull(gen, "Class not generated: " + path);
        return gen;
    }

    private static void assertBytes(String path, String generated, Path goldenDir)
            throws IOException {
        Path goldenPath = goldenDir.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated output must byte-match the golden (newline-normalized) for "
                + path + " (PR #371).");
    }

    /** OCCURRENCE count (the #352 law — grep -c counts lines, str.count counts hits). */
    private static int count(String haystack, String needle) {
        int n = 0;
        int idx = 0;
        while ((idx = haystack.indexOf(needle, idx)) >= 0) {
            n++;
            idx += needle.length();
        }
        return n;
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
