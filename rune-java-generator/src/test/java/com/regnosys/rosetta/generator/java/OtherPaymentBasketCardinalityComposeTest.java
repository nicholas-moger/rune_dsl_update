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
 * PR #370 — the OtherPayment/basket cardinality composite: 7 byte flips (1 cdm6
 * FUNCTION + 1 drr FUNCTION + 5 drr POJO Rules) + 0 new / 0 away (regscan370:
 * 7/0/0; 16 common movers all TOWARD — the LoadType family 0.88→0.98,
 * CollateralPortfolioCode 0.91→0.96).
 *
 * <p><b>F-B caseSwitchAliasSubjectStmtArms</b> (FunctionAliasHelper +
 * NavigationHandler + FunctionExpressionRenderer): the #221 choice-type ladder
 * admits MapTransferStateList's whole 4-ladder method — the ALIAS-call subject
 * recovers its MODEL item through the NEW
 * {@code FunctionAliasHelper.inferShortcutModelItemJavaClass} walk (the SAME walk
 * the {@code MapperS<? extends Product>} alias signature renders from — the #178
 * same-walk law; {@code final Product switchArgument0 = fpmlProduct(fpmlTrade)
 * .get();}); the hoisted subject rides the per-method StatementHoistSession
 * ({@code switchArgument0..3} across 4 ladders — green singletons stay bare,
 * #170); cast locals number method-wide by AST precount
 * ({@code genericProduct0/1}, {@code capFloor0/1}) and the #368 hop-1 param
 * pre-escape keys on the LIVE #221 binding text (escape iff the cast local
 * carries exactly the type-derived name); ADD-seat STATEMENT-form case bodies
 * render the nested if/else-if ladder (each branch consumed independently,
 * absent/empty else = the emptyList arm) and a SINGLE-output fn call into the
 * MULTI output hoists the null-guarded emptyList/singletonList pair over a
 * method-spanning {@code transferState0..2} local (the #198 dynamic-base
 * pattern): MapTransferStateList cdm6.
 *
 * <p><b>F-D listOfListsCardinality</b> (NavigationHandler + ReferenceHandler +
 * ComparisonHandler + FunctionExpressionRenderer): resolveDisguisedFeature's
 * callable-head arm 4 — a disguised head bound to a RULE/FUNCTION
 * ({@code evr.resolvedSymbol}; a converted rule arrives as its fromRule
 * RFunction) resolves the leaf on the callable's OUTPUT type, sitting ABOVE the
 * enclosing-function gate (the #363 parent-walk detachment class) with META
 * leaves declined (the cp4 green catch) — the then-chain decl wrappers and
 * filter/map arities cascade ({@code MapperC<TransferState> thenArg0} +
 * filterItemNullSafe + mapItem + mapItemToList + the MapperListOfLists decl):
 * DTCC_OtherPaymentPayer/ReceiverIDTypeRule. The k&gt;0 intermediate ITE
 * recovers the #144/#333 meta ELEMENT by probing the then-arm on a throwaway
 * lambda-child scope ({@code final MapperS<ReferenceWithMetaParty>
 * ifThenElseResult;} + the ofNull twin + the downstream #144 deref hoist), the
 * rule-output enum rung requalifies the comparison operand by the sibling
 * rule's inferred output enum ({@code PartyIdentifierFormatEnum.OTHER} over the
 * Format2 super — the #215 same-instance descend-only gate), and the stmtDirect
 * meta-deref BLOCK wrap goes cardinality-aware ({@code return
 * MapperC.<String>of(partyIdentifierType.evaluate(…))}):
 * OtherPaymentPayer/ReceiverSchemeNameRule hkma.
 *
 * <p><b>F-E conditionalLadderMulti</b> (NavigationHandler + CollectionHandler):
 * chainProvesMulti's conditional arm generalizes — PATH-BLIND, LADDER-AWARE,
 * EMPTY arms NEUTRAL (upstream's join; the golden {@code MapperC.<T>ofNull()}
 * ladder terminal is the byte evidence) — and the #350-F2b MapperC ladder
 * admission widens to the POSITION so an UNBOUND multi-alias receiver renders
 * the mapItemToList ladder block with MapperC-form terminals:
 * GetBasketConstituentsProductIdentifier drr FN +
 * BasketConstituentNumberOfUnitsRule v1.
 *
 * <p>Whole-file byte comparisons run through the REAL D11 generation paths and
 * revert RED without the facets. The negative witnesses are load-bearing per the
 * witness-uniqueness law (OCCURRENCE counts, never line counts): every token
 * below was occurrence-counted in its PRE gen (f-probe-369post — counts stated
 * per witness) and 0 in its golden — the flips REMOVE them.
 */
class OtherPaymentBasketCardinalityComposeTest {

    private static final Path CDM6_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path CDM6_GOLDEN_DIR =
            CDM6_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path DRR_CELL_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path DRR_GOLDEN_DIR =
            DRR_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    /** The cdm6 FUNCTION flip carrier (F-B). */
    private static final String MAP_TRANSFER_STATE_LIST =
            "cdm/ingest/fpml/confirmation/payment/functions/MapTransferStateList.java";

    /** The drr FUNCTION flip carrier (F-E). */
    private static final String GET_BASKET_CONSTITUENTS =
            "drr/standards/iosco/cde/base/basket/functions/GetBasketConstituentsProductIdentifier.java";

    /** The 5 drr POJO Rule flip carriers (F-D ×4 + the F-E family member). */
    private static final String[] DRR_RULES = {
            "drr/regulation/common/dtcc/reports/DTCC_OtherPaymentPayerIDTypeRule.java",
            "drr/regulation/common/dtcc/reports/DTCC_OtherPaymentReceiverIDTypeRule.java",
            "drr/regulation/hkma/rewrite/trade/reports/OtherPaymentPayerSchemeNameRule.java",
            "drr/regulation/hkma/rewrite/trade/reports/OtherPaymentReceiverSchemeNameRule.java",
            "drr/standards/iosco/cde/version1/basket/reports/BasketConstituentNumberOfUnitsRule.java",
    };

    private static Map<String, String> cdm6FnOutput;
    private static Map<String, String> drrFnOutput;
    private static Map<String, String> drrRuleOutput;

    static boolean cdm6CellAvailable() {
        return Files.isDirectory(CDM6_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(CDM6_GOLDEN_DIR);
    }

    static boolean drrCellAvailable() {
        return Files.isDirectory(DRR_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR_GOLDEN_DIR);
    }

    @BeforeAll
    static void generate() throws IOException {
        if (cdm6CellAvailable()) {
            cdm6FnOutput = generateFunctions(
                    new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CDM6_CELL_ROOT));
        }
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

    // ----------------------------------------------------------- byte locks (7)

    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapTransferStateList_byteMatchesGolden() throws IOException {
        assertBytes(MAP_TRANSFER_STATE_LIST, gen(cdm6FnOutput, MAP_TRANSFER_STATE_LIST),
                CDM6_GOLDEN_DIR);
    }

    @Test
    @EnabledIf("drrCellAvailable")
    void getBasketConstituentsProductIdentifier_byteMatchesGolden() throws IOException {
        assertBytes(GET_BASKET_CONSTITUENTS, gen(drrFnOutput, GET_BASKET_CONSTITUENTS),
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
     * The F-B legacy-ternary witness (MapTransferStateList cdm6): the non-compiling
     * trailing {@code null.getMulti()} counted EXACTLY 4 occurrences in the PRE gen
     * (f-probe-369post — one per legacy switch ternary) and 0 in the golden — the
     * instanceof ladders replace all four.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapTransferStateList_legacyTernaryGone() {
        assertEquals(0, count(gen(cdm6FnOutput, MAP_TRANSFER_STATE_LIST), "null.getMulti()"),
                "The legacy switch-ternary null.getMulti() must be gone (PRE count 4)");
    }

    /**
     * The F-B subject witness (MapTransferStateList cdm6): the type-literal equality
     * {@code Objects.equals(fpml.FxVolatilitySwap, fpmlProduct(fpmlTrade))} counted
     * EXACTLY 1 occurrence in the PRE gen and 0 in the golden — the recovered alias
     * subject hoists {@code final Product switchArgument0 = fpmlProduct(fpmlTrade)
     * .get();} instead.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapTransferStateList_typeLiteralEqualityGone() {
        assertEquals(0, count(gen(cdm6FnOutput, MAP_TRANSFER_STATE_LIST),
                        "Objects.equals(fpml.FxVolatilitySwap, fpmlProduct(fpmlTrade))"),
                "The type-literal Objects.equals subject must be gone (PRE count 1)");
    }

    /**
     * The F-B statement-form witness (MapTransferStateList cdm6): the hoisted
     * bare-item conditional {@code final TransferState ifThenElseResult;} counted
     * EXACTLY 1 occurrence in the PRE gen and 0 in the golden — the CreditDefaultSwap
     * case renders the nested in-arm ladder with the {@code transferState0} hoist.
     */
    @Test
    @EnabledIf("cdm6CellAvailable")
    void mapTransferStateList_itemConditionalHoistGone() {
        assertEquals(0, count(gen(cdm6FnOutput, MAP_TRANSFER_STATE_LIST),
                        "final TransferState ifThenElseResult;"),
                "The statement-level item conditional hoist must be gone (PRE count 1)");
    }

    /**
     * The F-E terminal witness (GetBasketConstituentsProductIdentifier drr): the
     * MapperS-form ladder terminal {@code return
     * MapperS.<ReferenceWithMetaProductIdentifier>ofNull();} counted EXACTLY 2
     * occurrences in the PRE gen (one per then-chain) and 0 in the golden — the
     * MAPPER_C_EXPECTING ladder renders the MapperC-form terminals.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void getBasket_mapperSLadderTerminalGone() {
        assertEquals(0, count(gen(drrFnOutput, GET_BASKET_CONSTITUENTS),
                        "return MapperS.<ReferenceWithMetaProductIdentifier>ofNull();"),
                "The MapperS-form ladder terminal must be gone (PRE count 2)");
    }

    /**
     * The F-E consumption witness (GetBasketConstituentsProductIdentifier drr): the
     * item-form next step {@code .mapItem(item -> MapperS.of(
     * getProductIdentifierFilteringISIN.evaluate(} counted EXACTLY 2 occurrences in
     * the PRE gen and 0 in the golden — the MapperListOfLists pipe consumes via
     * {@code mapListToItem}.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void getBasket_mapItemConsumptionGone() {
        assertEquals(0, count(gen(drrFnOutput, GET_BASKET_CONSTITUENTS),
                        ".mapItem(item -> MapperS.of(getProductIdentifierFilteringISIN.evaluate("),
                "The mapItem LoL consumption must be gone (PRE count 2)");
    }

    /**
     * The F-D arity witness (DTCC_OtherPaymentPayer/ReceiverIDTypeRule drr): the
     * single-form filter {@code .filterSingleNullSafe(item -> isOtherPayment
     * .evaluate(item.get()))} counted EXACTLY 1 occurrence in EACH PRE gen and 0 in
     * the goldens — the callable-head walk proves the base MULTI and the filter
     * takes {@code filterItemNullSafe}.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void dtccIdType_singleFilterGone() {
        for (String path : new String[] {DRR_RULES[0], DRR_RULES[1]}) {
            assertEquals(0, count(gen(drrRuleOutput, path),
                            ".filterSingleNullSafe(item -> isOtherPayment.evaluate(item.get()))"),
                    () -> "The single-form filter must be gone in " + path + " (PRE count 1)");
        }
    }

    /**
     * The F-D wrap witness (DTCC_OtherPaymentPayerIDTypeRule drr): the MapperS-form
     * in-lambda return {@code return MapperS.of(partyIdentifierType.evaluate(}
     * counted EXACTLY 1 occurrence in the PRE gen and 0 in the golden — the
     * MULTI-output callee re-presents {@code MapperC.<String>of(…)} at the
     * stmtDirect block seat.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void dtccIdType_mapperSCallWrapGone() {
        assertEquals(0, count(gen(drrRuleOutput, DRR_RULES[0]),
                        "return MapperS.of(partyIdentifierType.evaluate("),
                "The MapperS.of multi-callee wrap must be gone (PRE count 1)");
    }

    /**
     * The F-D enum witness (OtherPaymentPayer/ReceiverSchemeNameRule hkma): the
     * super-qualified constant {@code MapperS.of(PartyIdentifierFormat2Enum.OTHER)}
     * counted EXACTLY 1 occurrence in EACH PRE gen and 0 in the goldens — the
     * rule-output rung requalifies by the child {@code PartyIdentifierFormatEnum}.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void schemeName_superQualifiedEnumGone() {
        for (String path : new String[] {DRR_RULES[2], DRR_RULES[3]}) {
            assertEquals(0, count(gen(drrRuleOutput, path),
                            "MapperS.of(PartyIdentifierFormat2Enum.OTHER)"),
                    () -> "The super-qualified enum constant must be gone in " + path
                            + " (PRE count 1)");
        }
    }

    /**
     * The F-D meta-element witness (OtherPaymentPayerSchemeNameRule hkma): the
     * bare-element decl {@code final MapperS<Party> ifThenElseResult;} counted
     * EXACTLY 1 occurrence in the PRE gen and 0 in the golden — the then-arm probe
     * recovers {@code ReferenceWithMetaParty}, and the un-derefed consumption
     * {@code evaluate(ifThenElseResult.get())} (PRE count 1, golden 0) gives way to
     * the #144 wrapper deref hoist.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void schemeName_bareElementDeclGone() {
        String gen = gen(drrRuleOutput, DRR_RULES[2]);
        assertEquals(0, count(gen, "final MapperS<Party> ifThenElseResult;"),
                "The bare-element ITE decl must be gone (PRE count 1)");
        assertEquals(0, count(gen, "evaluate(ifThenElseResult.get())"),
                "The un-derefed wrapper consumption must be gone (PRE count 1)");
    }

    /**
     * The F-E family witness (BasketConstituentNumberOfUnitsRule v1): the
     * single-form step {@code .mapSingleToItem(item -> {} counted EXACTLY 1
     * occurrence in the PRE gen (with its {@code return MapperS.<Basket>ofNull();}
     * terminal, PRE count 1) and 0 in the golden — the generalized conditional
     * cardinality selects {@code mapSingleToList} with the MapperC terminal.
     */
    @Test
    @EnabledIf("drrCellAvailable")
    void basketConstituentNumberOfUnits_singleFormStepGone() {
        String gen = gen(drrRuleOutput, DRR_RULES[4]);
        assertEquals(0, count(gen, ".mapSingleToItem(item -> {"),
                "The single-form conditional step must be gone (PRE count 1)");
        assertEquals(0, count(gen, "return MapperS.<Basket>ofNull();"),
                "The MapperS-form conditional terminal must be gone (PRE count 1)");
    }

    // ------------------------------------------------------------------ helpers

    private static String gen(Map<String, String> output, String path) {
        assertNotNull(output, "Generation did not run — corpus unavailable?");
        String gen = output.get(path);
        assertNotNull(gen, () -> "Generated output missing for " + path);
        return gen;
    }

    private static void assertBytes(String path, String gen, Path goldenDir) throws IOException {
        Path goldenPath = goldenDir.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), () -> "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath).replace("\r\n", "\n");
        assertEquals(golden, gen.replace("\r\n", "\n"),
                () -> "Generated bytes must match golden for " + path);
    }

    /** OCCURRENCE count (the witness-uniqueness law — never line counts). */
    private static int count(String text, String token) {
        int n = 0;
        int i = text.indexOf(token);
        while (i >= 0) {
            n++;
            i = text.indexOf(token, i + 1);
        }
        return n;
    }
}
