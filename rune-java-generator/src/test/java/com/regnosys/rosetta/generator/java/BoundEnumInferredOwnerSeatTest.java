package com.regnosys.rosetta.generator.java;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.function.LabelProviderGenerator;
import com.regnosys.rosetta.generator.java.function.LabelProviderGeneratorUtil;
import com.regnosys.rosetta.generator.java.object.ChoiceObjectGenerator;
import com.regnosys.rosetta.generator.java.object.ModelObjectGenerator;
import com.regnosys.rosetta.generator.java.object.datarule.DataRuleGenerator;
import com.regnosys.rosetta.generator.java.reports.ReportGenerator;
import com.regnosys.rosetta.generator.java.reports.RuleGenerator;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.symbols.RLinkingResult;
import com.regnosys.rosetta.symbols.RWorkspace;
import com.regnosys.rosetta.utils.DeepFeatureCallUtil;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.lang.ref.Reference.reachabilityFence;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static com.regnosys.rosetta.testutil.GenerationErrors.assertNoGenerationErrors;

/**
 * v3.1 LADDER RETIREMENT — flip seat 12: THE ROOT FIX of the enum expected-owner
 * family (the trace {@code target/seat12-root-trace.md} + charter
 * {@code target/seat12-charter.md}; Rung B of the seat-11 census's lever H). The
 * OFF-route ROOT, {@code ReferenceHandler.handle(RSymbolReference)}'s bare-enum
 * arm, rendered a BOUND bare enum value through the value's DECLARING enum
 * ({@code ev.parent()}) — {@code MapperS.of(PutCallEnum.PUT)} against an
 * {@code OptionTypeEnum}-typed operand, {@code ProductIdTypeEnum.ISIN} against an
 * {@code AssetIdTypeEnum} navigation — where golden qualifies by the EXPECTED enum
 * (upstream {@code enumCall(feature, expectedType.getItemValueType)},
 * ExpressionGenerator.xtend:340-343; the generated Java flattens inherited values
 * under the child's name — the #211/#358 flatten law), NON-COMPILING at every
 * carrier. The parser ALREADY types that node by the expected enum
 * ({@code ExpressionTypeComputer} — a bare {@code REnumValue}-symbol reference types
 * as {@code engine.expectedEnumAt(ref)}; the declaring enum is only its fallback),
 * and every bind of a bare enum walks UP from the very enum the position walker
 * returns, so the node's inferred type IS the expected enum. The IR route's
 * {@code IRJavaLeafEmitter.emitEnumValue} already qualifies by the node's type —
 * the ON dump was this seat's movement oracle.
 *
 * <p><b>The seat (facet {@code boundEnumInferredOwner}, ONE arm):</b> the root arm
 * reads {@code gm.workspace().getInferredType(expr)} (the SetOperationHandler
 * {@code default}-RHS precedent) and, when it is an enum {@code expected != declaring}
 * whose hierarchy flattens the EXACT bound value
 * ({@code HandlerHelper.findEnumValueInHierarchy(expected, name) == ev} — the #215
 * SAME-INSTANCE descend-only gate), renders {@code Expected.VALUE} + the expected
 * enum's import; otherwise today's bytes. The arm owns a FRESH single-element ref
 * set (no ladder-wide strip — LAW 70 by construction). {@code handle(REnumValueRef)}
 * (an EXPLICIT {@code Enum -> Value}) is UNTOUCHED — the golden two-sided witness
 * {@code UnderlierIDOtherSourceLeg1Rule.java:69} (bare → child) vs {@code :74}
 * (explicit → the author's enum). The seat-11 {@code CollectionHandler} ladder arm
 * MUST stay (a rule-root ladder has no container expectation — the walker's answer
 * there is typeof(then), one level too shallow on the depth-2 chain; the seat-11
 * suite's a2 + its 6 hkma whole-file locks are the shallowing detectors, and its
 * b6 single-rung pin FLIPS with this seat, as its javadoc pre-registered).
 *
 * <p><b>Over-fire, discharged by construction (fixture-truth probe, recorded in the
 * charter):</b> a WIDENING shape ({@code if … then ProductId -> Name else CUSIP} at
 * a parent-typed seat) never BINDS — the linker reports SYMBOL_NOT_FOUND on the
 * child-only value and the generator REFUSES the echo ({@code UNRESOLVED_SYMBOL_ECHO})
 * — so the root arm, which requires a bound symbol, can never see it (and
 * {@code findEnumValueInHierarchy} walks UP only, so it would decline anyway); an
 * UNRELATED same-name enum can only enter through a bind, and every bind walks the
 * expected enum's own hierarchy. Green-safety: an own-value seat has
 * {@code expected == declaring} and is a literal no-op; the LAW-66 golden instrument
 * fires on both sides (99 child-qualified / 170 parent-qualified golden files, drr
 * 7.0.0) and the over-fire probe (a parent-qualified constant beside a child-typed
 * sibling) is 0 golden lines corpus-wide.
 */
class BoundEnumInferredOwnerSeatTest {

    private static final Path REPO_ROOT =
            Path.of(System.getProperty("user.dir")).resolve("..").normalize();

    private static final List<Path> BUILTINS_SEARCH_ROOTS = List.of(
            REPO_ROOT.resolve("test-corpus/rune-dsl-builtins/rune-runtime/src/main/resources/model"),
            REPO_ROOT.resolve("rune-dsl/rune-runtime/src/main/resources/model")
    );

    static boolean builtinsAvailable() {
        return BUILTINS_SEARCH_ROOTS.stream().anyMatch(Files::isDirectory);
    }

    /**
     * {@code AssetId extends ProductId} (ISIN/Name/Other/UPI on the PARENT — the
     * AssetIdTypeEnum/ProductIdTypeEnum pair); {@code OptionType extends PutCall}
     * (Put/Call on the PARENT — the OptionTypeEnum/PutCallEnum pair). The shapes
     * mirror the drr 7.0.0 carriers 1:1: A1 the UnderlierIDOtherSourceLeg1 comparison
     * over a child-typed navigation; A2 the DTCC_OptionType ladder over a bound
     * lambda {@code item} (inherited Put/Call + the own Receiver = the in-file
     * control); A3 the MAS_BR_0052 DATA-RULE condition ({@code any <>}); A4 the hkma
     * ReferenceEntityFormat elided-subject equality ({@code then if = ISIN}) with the
     * explicit-{@code item} own-value sibling; A5 a rule whose OUTPUT is NOT an enum
     * (the seat-11 consumer declines, the root fires, the merged arm refs carry the
     * child); A6 the ctor-pair ITE hoist (the local's declared type must agree with
     * the constant — LAW 69); B1 the explicit {@code ProductId -> Name} control; B3
     * the function ARGUMENT seat; B4 the switch GUARD; B5 the FUNCTION {@code set out:}
     * ladder (the render-truth-gated FER consumer); B8 the {@code Unrelated} same-name
     * enum that must never enter.
     */
    private static final String MODEL = """
            namespace census.seat12
            version "1.0.0"

            enum ProductId:
                ISIN
                Name
                Other
                UPI

            enum AssetId extends ProductId:
                CUSIP
                ISDACRP

            enum PutCall:
                Put
                Call

            enum OptionType extends PutCall:
                Receiver
                Payer

            enum Unrelated:
                Other
                ISIN

            type Identifier:
                identifierType AssetId (0..1)
                kind PutCall (0..1)

            type BasketReport:
                source AssetId (0..1)

            type Holder:
                idType AssetId (0..1)
                note string (0..1)

            type Trade:
                identifier Identifier (0..*)
                optionType OptionType (0..1)
                flag boolean (0..1)
                basketConstituents BasketReport (0..*)

                condition A3SourceCondition: <"a3 - the DATA-RULE condition seat">
                    if basketConstituents exists and basketConstituents -> source any <> ISIN
                    then basketConstituents -> source exists

            reporting rule A1Compare from Trade: <"a1 - the comparison operand over a child-typed navigation">
                extract
                    if identifier -> identifierType all <> ISIN
                    then "not-isin"
                    else "isin"

            reporting rule A2Ladder from Trade: <"a2 - the DTCC_OptionType shape">
                extract optionType
                then extract
                    if item = Put
                    then "Put"
                    else if item = Call
                    then "Call"
                    else if item = Receiver
                    then "Receiver"
                    else "Other"

            reporting rule A4Elided from Trade: <"a4 - the elided-subject equality + b2 the explicit-item LHS">
                extract identifier -> identifierType first
                then if = ISIN
                    then "isin"
                    else if item = CUSIP
                    then "cusip"

            reporting rule A5NonEnumOutput from Trade: <"a5 - the seat-11 consumer's DECLINE path">
                extract identifier first
                then extract
                    if kind = Put
                    then AssetId -> CUSIP
                    else if identifierType exists
                    then ISIN
                    else Other
                then extract item to-string

            reporting rule B1Explicit from Trade: <"b1 - the EXPLICIT ref control">
                extract
                    if identifier -> identifierType all <> ISIN
                    then identifier first -> identifierType to-string
                    else if flag = True
                    then ProductId -> Name to-string
                    else "CCY"

            func Takes: <"b3 helper">
                inputs:
                    ids Identifier (0..*)
                    t AssetId (1..1)
                output:
                    n int (1..1)
                set n:
                    ids count

            func B3Arg: <"b3 - the function-ARGUMENT bare enum">
                inputs:
                    tr Trade (1..1)
                output:
                    n int (1..1)
                set n:
                    Takes(tr -> identifier, ISIN)

            func B4Guard: <"b4 - the switch-case GUARD">
                inputs:
                    ot OptionType (1..1)
                output:
                    s string (1..1)
                set s:
                    ot switch
                        Put then "put",
                        Call then "call",
                        Receiver then "receiver",
                        default "other"

            func A6CtorPairIte: <"a6 - the ctor-pair ITE hoist: the local's declared type agrees with the constant">
                inputs:
                    tr Trade (1..1)
                output:
                    h Holder (1..1)
                set h:
                    Holder {
                        idType: if tr -> flag = True then ISIN,
                        note: "x"
                    }

            func B5SetOut: <"b5 - the FER render-truth-gated consumer at unit grain">
                inputs:
                    tr Trade (1..1)
                output:
                    out AssetId (0..1)
                set out:
                    if tr -> flag = True
                    then ISIN
                    else if tr -> identifier exists
                    then CUSIP
                    else Other
            """;

    // =========================================================================
    // Part A — controls (RED pre-seat)
    // =========================================================================

    /** a1 — the comparison operand: a bare value declared on the PARENT compared
     *  against a CHILD-typed navigation qualifies by the child; the parent's import
     *  is NOT emitted. PRE (probed): {@code MapperS.of(ProductId.ISIN)} + the
     *  ProductId import. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_comparisonOperand_childTypedNavigation_qualifiesByExpectedEnum() throws IOException {
        String out = rule("A1CompareRule.java");
        assertContains(out, "MapperS.of(AssetId.ISIN), CardinalityOperator.All)");
        assertNotContains(out, "ProductId.ISIN");
        assertNotContains(out, "import census.seat12.ProductId;");
    }

    /** a2 — the DTCC_OptionType shape: a ladder of equalities over a bound lambda
     *  {@code item} — the inherited Put/Call qualify by the item's (child) enum,
     *  the own Receiver is unchanged (the in-file control), the parent import
     *  goes. PRE (probed): {@code PutCall.PUT} / {@code PutCall.CALL} + the PutCall
     *  import beside {@code OptionType.RECEIVER}. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_boundLambdaItemLadder_inheritedAndOwnValues() throws IOException {
        String out = rule("A2LadderRule.java");
        assertContains(out, "areEqual(item, MapperS.of(OptionType.PUT), CardinalityOperator.All)");
        assertContains(out, "areEqual(item, MapperS.of(OptionType.CALL), CardinalityOperator.All)");
        assertContains(out, "areEqual(item, MapperS.of(OptionType.RECEIVER), CardinalityOperator.All)");
        assertNotContains(out, "PutCall.");
        assertNotContains(out, "import census.seat12.PutCall;");
    }

    /** a3 — the DATA-RULE condition seat ({@code any <>} inside a type condition —
     *  the MAS_BR_0052 shape, the whole DATA_RULE residue): the DataRuleGenerator
     *  path renders through the same root arm. PRE (probed):
     *  {@code MapperS.of(ProductId.ISIN), CardinalityOperator.Any)} + the import. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a3_dataRuleCondition_anyNotEqual_qualifiesByExpectedEnum() throws IOException {
        String out = lookup(output(), "TradeA3SourceCondition.java");
        assertContains(out, "MapperS.of(AssetId.ISIN), CardinalityOperator.Any)");
        assertNotContains(out, "ProductId.ISIN");
        assertNotContains(out, "import census.seat12.ProductId;");
    }

    /** a4 — the elided-subject equality ({@code then if = ISIN} — the hkma
     *  ReferenceEntityFormat {@code = LEI} shape): the expected enum is read off
     *  the enclosing item type. PRE (probed): {@code areEqual(thenArg,
     *  MapperS.of(ProductId.ISIN), …)} + the ProductId import. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a4_elidedSubjectEquality_byEnclosingItemType() throws IOException {
        String out = rule("A4ElidedRule.java");
        assertContains(out, "areEqual(thenArg, MapperS.of(AssetId.ISIN), CardinalityOperator.All)");
        assertNotContains(out, "ProductId.ISIN");
        assertNotContains(out, "import census.seat12.ProductId;");
    }

    /** a5 — the seat-11 consumer's DECLINE path (the one live import channel): a
     *  rule whose OUTPUT is NOT an enum ({@code … then extract item to-string}) has
     *  no rule-output enum, so {@code CollectionHandler}'s ladder arm declines and
     *  MERGES the arm's own compiled refs — post-seat those refs are the CHILD's
     *  (typeof(then) = the explicit {@code AssetId -> CUSIP} anchor), and the
     *  ProductId import goes. PRE (probed): {@code MapperS<AssetId>} lambda returning
     *  {@code MapperS.of(ProductId.ISIN)} / {@code ProductId.OTHER} (non-compiling)
     *  + the ProductId import. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a5_ruleOutputNotEnum_seat11ConsumerDeclines_rootFires_importFollows() throws IOException {
        String out = rule("A5NonEnumOutputRule.java");
        assertContains(out, "return MapperS.of(AssetId.CUSIP);");
        assertContains(out, "return MapperS.of(AssetId.ISIN);");
        assertContains(out, "return MapperS.of(AssetId.OTHER);");
        assertContains(out, "import census.seat12.AssetId;");
        assertNotContains(out, "ProductId.");
        assertNotContains(out, "import census.seat12.ProductId;");
    }

    /** a6 — the ctor-pair ITE HOIST (the indep review's NIT-1, a LAW-69 sibling):
     *  {@code ControlFlowHandler.thenItemJavaClass} types the hoisted local for a
     *  no-else ctor-setter conditional; it CONSULTS the same owner computation the
     *  constant renders with ({@code HandlerHelper.boundEnumInferredOwner}), so the
     *  local's declared type and the assigned constant agree — {@code AssetId
     *  ifThenElseResult = null; … ifThenElseResult = AssetId.ISIN; …
     *  .setIdType(ifThenElseResult)}. PRE (probed at the seat-12 2/n head): the
     *  local typed by the DECLARING enum ({@code ProductId ifThenElseResult}) beside
     *  the root's {@code AssetId.ISIN} — and before seat 12 the pair {@code ProductId
     *  ifThenElseResult = ProductId.ISIN} handed to {@code setIdType(AssetId)} did not
     *  compile either: zero corpus carriers, byte-neutral corpus-wide (both rings
     *  EXACT with the fix). */
    @Test
    @EnabledIf("builtinsAvailable")
    void a6_ctorPairIteHoist_localTypeAgreesWithConstant() throws IOException {
        String out = lookup(output(), "A6CtorPairIte.java");
        assertContains(out, "AssetId ifThenElseResult = null;");
        assertContains(out, "ifThenElseResult = AssetId.ISIN;");
        assertContains(out, ".setIdType(ifThenElseResult)");
        assertNotContains(out, "ProductId");
    }

    // =========================================================================
    // Part B — inert pins (GREEN pre-seat AND post-seat)
    // =========================================================================

    /** b1 — the EXPLICIT {@code Enum -> Value} control (the golden two-sided witness
     *  UnderlierIDOtherSourceLeg1Rule:74): an explicitly-written parent reference
     *  keeps the author's enum AND its import — in a file whose bare sibling
     *  ({@code identifierType all <> ISIN}, the a1 shape) heals to the child, so
     *  post-seat the ProductId import SURVIVES on the explicit ref's account alone
     *  (green PRE and POST; the sibling's heal is a1's assertion, not this pin's). */
    @Test
    @EnabledIf("builtinsAvailable")
    void b1_explicitEnumValueRef_keepsAuthorsEnumAndImport() throws IOException {
        String out = rule("B1ExplicitRule.java");
        assertContains(out, "MapperS.of(ProductId.NAME).map(\"to-string\", ProductId::toDisplayString)");
        assertContains(out, "import census.seat12.ProductId;");
    }

    /** b2 — the explicit-{@code item} LHS with an OWN value of the child
     *  ({@code else if item = CUSIP}): binds and stays own-qualified (expected ==
     *  declaring — a literal no-op). Note the corpus {@code ReferenceEntityFormatRule}
     *  {@code item = CountryCode} echo does NOT reproduce here (the probe bound the
     *  explicit-item LHS through a plain navigation, a rule-call receiver and a
     *  rule-call + navigation receiver alike) — that residue is a rule-chain typing
     *  gap on the hkma {@code ExtractReferenceEntity} chain, the seat-13 candidate,
     *  not a qualification defect. */
    @Test
    @EnabledIf("builtinsAvailable")
    void b2_explicitItemLhs_ownValue_unchanged() throws IOException {
        String out = rule("A4ElidedRule.java");
        assertContains(out, "areEqual(thenArg, MapperS.of(AssetId.CUSIP), CardinalityOperator.All)");
    }

    /** b3 — the function-ARGUMENT bare enum (site 2, {@code tryBareEnumArg} — the
     *  callee's declared input already IS the expected type): unchanged, no double
     *  processing, the argument stays {@code AssetId.ISIN}. */
    @Test
    @EnabledIf("builtinsAvailable")
    void b3_functionArgumentBareEnum_unchanged() throws IOException {
        String out = lookup(output(), "B3Arg.java");
        assertContains(out, ", AssetId.ISIN);");
        assertNotContains(out, "ProductId.");
    }

    /** b4 — the switch-case GUARD ({@code FunctionExpressionRenderer
     *  .renderEnumGuardConstant}, an {@code RSwitchCaseGuard}, NOT an
     *  {@code RSymbolReference}) is out of this seat's scope: it still qualifies by
     *  the DECLARING enum ({@code PutCall.PUT} against an {@code OptionType} argument
     *  — a latent divergence with ZERO corpus carriers, recorded in the trace as
     *  shape (e); the own value {@code OptionType.RECEIVER} is right either way).
     *  This pin records the boundary; it flips deliberately when a guard seat lands. */
    @Test
    @EnabledIf("builtinsAvailable")
    void b4_switchCaseGuard_outOfScope_declaringQualified_documented() throws IOException {
        String out = lookup(output(), "B4Guard.java");
        assertContains(out, "ot == PutCall.PUT");
        assertContains(out, "ot == PutCall.CALL");
        assertContains(out, "ot == OptionType.RECEIVER");
    }

    /** b5 — the ONE render-truth-gated consumer ({@code requalifyBareEnumAssignArmOrNull},
     *  #387 — fires only when the arm rendered EXACTLY {@code Declaring.VALUE}): the
     *  FUNCTION {@code set out:} ladder with inherited arms renders the same bytes
     *  AND the same imports PRE (FER requalifies the root's declaring render) and
     *  POST (the root already renders the expected enum; the FER gate goes false and
     *  the caller keeps the root's render) — the two computations agree. */
    @Test
    @EnabledIf("builtinsAvailable")
    void b5_functionSetOutLadder_renderTruthConsumerAgrees() throws IOException {
        String out = lookup(output(), "B5SetOut.java");
        assertContains(out, "out = AssetId.ISIN;");
        assertContains(out, "out = AssetId.CUSIP;");
        assertContains(out, "out = AssetId.OTHER;");
        assertContains(out, "import census.seat12.AssetId;");
        assertNotContains(out, "ProductId.");
        assertNotContains(out, "import census.seat12.ProductId;");
    }

    /** b8 — the UNRELATED same-name enum never enters (the indep review's NIT-6):
     *  the fixture declares {@code Unrelated {Other, ISIN}} — the same value names the
     *  a-series binds through {@code ProductId}/{@code AssetId} — so a name-based
     *  binder or a qualifier that searched enums by value name would surface it;
     *  every bind walks the EXPECTED enum's own hierarchy and the root's same-
     *  instance gate compares instances, so no generated file anywhere in the
     *  fixture references {@code Unrelated} at all. */
    @Test
    @EnabledIf("builtinsAvailable")
    void b8_unrelatedSameNameEnum_neverEnters() throws IOException {
        Map<String, String> all = output();
        // 5 reporting rules + 1 data rule + 5 functions (the harness emits no POJOs)
        assertEquals(11, all.size(), "unexpected fixture file count: " + all.keySet());
        for (Map.Entry<String, String> e : all.entrySet()) {
            assertTrue(!e.getValue().contains("Unrelated."),
                    "Unrelated. leaked into " + e.getKey() + ":" + System.lineSeparator() + e.getValue());
            assertTrue(!e.getValue().contains("import census.seat12.Unrelated;"),
                    "the Unrelated import leaked into " + e.getKey());
        }
    }

    // =========================================================================
    // Part C — drr 7.0.0 corpus locks (a7-a9 RED pre-seat, WHOLE-FILE; b6-b7 STAY)
    // =========================================================================

    private static final Path DRR7_CELL_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path DRR7_GOLDEN_DIR =
            DRR7_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    static boolean drr7Available() {
        return Drr7Corpus.gate(builtinsAvailable()
                && Files.isDirectory(DRR7_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR7_GOLDEN_DIR), BoundEnumInferredOwnerSeatTest.class);
    }

    private static Map<String, String> drr7Output;
    private static List<String> drr7GenErrors;

    @BeforeAll
    static void generateDrr7() throws IOException {
        if (drr7Available()) {
            drr7Output = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", DRR7_CELL_ROOT));
        }
    }

    /** a7 — the 5-line POJO carrier: {@code if item = Put/Call} over an
     *  OptionTypeEnum item ({@code PutCallEnum.{PUT,CALL}} → {@code OptionTypeEnum.*}
     *  ×2 + the PutCallEnum import drops; RECEIVER/PAYER/STRADDLE = the in-file
     *  own-value control). */
    @Test
    @EnabledIf("drr7Available")
    void corpus_dtccOptionType_common_wholeFile() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/common/dtcc/trade/reports/DTCC_OptionTypeRule.java");
    }

    /** a8 — the golden TWO-SIDED control: line 69 bare {@code identifierType all <>
     *  ISIN} → {@code AssetIdTypeEnum.ISIN} while line 74's explicit
     *  {@code ProductIdTypeEnum -> Name} keeps {@code ProductIdTypeEnum.NAME} (and its
     *  import) — the whole file byte-matches. */
    @Test
    @EnabledIf("drr7Available")
    void corpus_underlierIdOtherSourceLeg1_iosco_wholeFile() throws IOException {
        assertByteMatchesGolden(
                "drr/standards/iosco/cde/version3/underlier/reports/UnderlierIDOtherSourceLeg1Rule.java");
    }

    /** a9 — the DATA_RULE carrier ({@code basketConstituents -> source any <> ISIN}
     *  in a type condition) — the whole drr 7.0.0 DATA_RULE residue (1 → 0). */
    @Test
    @EnabledIf("drr7Available")
    void corpus_masBr0052_dataRule_wholeFile() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/mas/rewrite/trade/validation/datarule/MASTransactionReportDTCC_MAS_BR_0052_01.java");
    }

    /** b6 — STAYS-IDENTICAL: an EXPLICIT {@code ProductIdTypeEnum -> ISIN} in a
     *  comparison seat whose sibling is ProductIdTypeEnum-typed (expected == declaring)
     *  — byte-identical before and after (the explicit-ref + own-enum control at
     *  corpus grain). */
    @Test
    @EnabledIf("drr7Available")
    void corpus_getIsin_common_staysIdentical() throws IOException {
        assertByteMatchesGolden("drr/regulation/common/functions/GetIsin.java");
    }

    /** b7 — STAYS-IDENTICAL: the seat-11 depth-2 chain (rule output
     *  PartyIdentifierFormatEnum, first arm explicit PartyIdentifierFormat2Enum): the
     *  {@code CollectionHandler} ladder arm still wins with the LEAF; the root's
     *  typeof(then) answer (the MIDDLE) never lands — the corpus-grain shallowing
     *  detector (the unit-grain twin is the seat-11 suite's a2). */
    @Test
    @EnabledIf("drr7Available")
    void corpus_otherPaymentPayerFormat_hkma_staysIdentical() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/hkma/rewrite/trade/reports/OtherPaymentPayerFormatRule.java");
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
        var dataRuleGen = new DataRuleGenerator(gm, typeTranslator, typeUtil);
        var labelProviderGen = new LabelProviderGenerator(
                gm, typeTranslator, new DeepFeatureCallUtil(gm::getType),
                new LabelProviderGeneratorUtil());
        Map<String, String> output = new LinkedHashMap<>();
        drr7GenErrors = new ArrayList<>();
        for (RModel model : corpus.workspace().files()) {
            if (gm.shouldGenerate(model)) {
                String version = gm.version(model);
                assertNoGenerationErrors(pojoGen.generateClasses(model, version, output));
                assertNoGenerationErrors(choiceGen.generateClasses(model, version, output));
                ruleGen.generateClasses(model, version, output)
                        .forEach(e -> drr7GenErrors.add(e.getTargetPath() + " — " + e));
                assertNoGenerationErrors(reportGen.generateClasses(model, version, output));
                dataRuleGen.generateClasses(model, version, output)
                        .forEach(e -> drr7GenErrors.add(e.getTargetPath() + " — " + e));
                assertNoGenerationErrors(labelProviderGen.generateClasses(model, version, output));
            }
        }
        funcGen.generateWithErrors(output)
                .forEach(e -> drr7GenErrors.add(e.getTargetPath() + " — " + e));
        return output;
    }

    private static void assertByteMatchesGolden(String path) throws IOException {
        assertNotNull(drr7Output, "drr 7.0.0 generation did not run — corpus unavailable?");
        List<String> lockedErrors = drr7GenErrors.stream()
                .filter(e -> e.contains(path)).toList();
        assertTrue(lockedErrors.isEmpty(),
                "generator reported errors for the locked file " + path + ": " + lockedErrors);
        String generated = drr7Output.get(path);
        assertNotNull(generated, "not generated: " + path);
        Path goldenPath = DRR7_GOLDEN_DIR.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated drr 7.0.0 output must byte-match the golden (newline-normalized) for "
                + path + " (seat 12 — the bound-bare-enum inferred-owner root fix).");
    }

    // =========================================================================
    // Harness
    // =========================================================================

    private static Map<String, String> outputMap;

    private static Map<String, String> output() throws IOException {
        if (outputMap == null) {
            RModel model = AstBuilder.buildFromString(MODEL, "seat12.rosetta");
            model.setVersion("0.0.0.test");
            List<RModel> models = new ArrayList<>();
            models.add(model);
            models.addAll(loadBuiltinsOnly());
            RLinkingResult linkingResult = RWorkspace.build(models);
            GeneratorModel gm = new GeneratorModel(linkingResult.workspace());
            JavaTypeUtil typeUtil = new JavaTypeUtil();
            JavaTypeTranslator tt = new JavaTypeTranslator(typeUtil);
            FunctionGenerator fg = new FunctionGenerator(gm, tt, typeUtil);
            RuleGenerator ruleGen = new RuleGenerator(gm, tt, fg);
            DataRuleGenerator dataRuleGen = new DataRuleGenerator(gm, tt, typeUtil);
            Map<String, String> out = new LinkedHashMap<>();
            List<String> errors = new ArrayList<>();
            ruleGen.generateClasses(model, "1.0", out)
                    .forEach(e -> errors.add(e.getTargetPath() + " — " + e));
            dataRuleGen.generateClasses(model, "1.0", out)
                    .forEach(e -> errors.add(e.getTargetPath() + " — " + e));
            fg.generateWithErrors(out)
                    .forEach(e -> errors.add(e.getTargetPath() + " — " + e));
            reachabilityFence(linkingResult);
            if (!errors.isEmpty()) {
                throw new AssertionError("fixture generation errors (a broken fixture"
                        + " must fail loudly, not skip): " + errors);
            }
            outputMap = out;
        }
        return outputMap;
    }

    private static String rule(String fileName) throws IOException {
        return lookup(output(), fileName);
    }

    private static String lookup(Map<String, String> output, String suffix) {
        return output.entrySet().stream()
                .filter(e -> e.getKey().endsWith("/" + suffix))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "'" + suffix + "' was not generated; keys=" + output.keySet()));
    }

    private static List<RModel> loadBuiltinsOnly() throws IOException {
        Map<String, Path> resolved = new LinkedHashMap<>();
        for (Path root : BUILTINS_SEARCH_ROOTS) {
            if (!Files.isDirectory(root)) continue;
            try (var stream = Files.walk(root)) {
                stream.filter(p -> p.toString().endsWith(".rosetta"))
                      .forEach(p -> resolved.putIfAbsent(p.getFileName().toString(), p));
            }
        }
        List<String> failures = new ArrayList<>();
        List<RModel> models = new ArrayList<>();
        resolved.values().stream()
                .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                .forEach(p -> {
                    try { models.add(AstBuilder.buildFromFile(p)); }
                    catch (Exception e) { failures.add(p + " — " + e); }
                });
        if (!failures.isEmpty()) {
            throw new AssertionError("[BoundEnumInferredOwnerSeatTest] builtins parse failures: "
                    + String.join("; ", failures));
        }
        return models;
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }

    private static void assertContains(String out, String needle) {
        assertTrue(out.contains(needle),
                "expected needle missing:\n" + needle + "\n--- in output:\n" + out);
    }

    private static void assertNotContains(String out, String token) {
        assertTrue(!out.contains(token),
                "forbidden token present: " + token + "\n--- in output:\n" + out);
    }
}
