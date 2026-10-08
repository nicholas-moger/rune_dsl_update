package com.regnosys.rosetta.generator.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.GenerationException;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.reports.ReportGenerator;
import com.regnosys.rosetta.generator.java.reports.RuleGenerator;
import com.regnosys.rosetta.generator.java.spi.IRGeneration;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.symbols.RLinkingResult;
import com.regnosys.rosetta.symbols.RWorkspace;

/**
 * SEAT 29, law 3 -- facet {@code rulePathFilterLolBinding}: <b>a then-step {@code filter} whose
 * implicit receiver is thenArg-bound to a {@code MapperListOfLists} takes upstream's
 * {@code filterListNullSafe} on the RULE path too, not only on the FUNCTION path</b>. The #430
 * binding walk declines wholesale when {@code findEnclosingRule(node) != null}; law 3 admits it
 * back for the FILTER overload alone, keyed on the bound WRAPPER.
 *
 * <p><b>THREE RUNGS, ONE LAW.</b> The carriers are single-hunk files whose hunk carries FOUR
 * deltas. Each rung closes a disjoint subset, and a rung left out means the file heals ZERO
 * (LAW 80 -- a count digest cannot see a partial repair):
 * <pre>
 * rung A  CollectionHandler boundImplicitReceiverType(RFilterExpr, scope, tu)
 *         -&gt; delta 1  .filterItemNullSafe(  =&gt;  .filterListNullSafe(       [the render seat]
 *         -&gt; delta 3  referenceWithMetaParty0 -&gt; referenceWithMetaParty0 == null ? null : ...
 *                     =&gt;  _referenceWithMetaParty -&gt; _referenceWithMetaParty.getValue()
 *                     [DERIVED: lambdaParamBindsListItem -&gt; NavigationHandler
 *                      .chainRendersMapperC rung R3 -&gt; metaNavResultType -&gt; the
 *                      WrappedItemCoercer MapperC arm, which uses registerDeferredLambdaParam
 *                      (a fresh child scope, hence the bare `_`-escaped name) instead of
 *                      registerDeferredCoercionParam (this scope, hence the `0`/`1` suffixes)]
 * rung B  CollectionHandler tryDeepThenHoist's listOfLists gains the
 *         filterPreservesListOfLists arm -- the LAW-69 port of FER:7002-7012
 *         -&gt; delta 2  final MapperC&lt;X&gt; thenArg3  =&gt;  final MapperListOfLists&lt;X&gt; thenArg3
 * rung C  CollectionHandler's MapperS.of re-wrap gate gains isFlattenCollapseOnlyElementLocal
 *         -- the LAW-69 twin of FER:8482
 *         -&gt; delta 4  thenArg3.flattenList().get()  =&gt;  MapperS.of(thenArg3.flattenList().get())
 * </pre>
 * Delta 4 exists ONLY in {@code CountryOfCounterparty2Rule}; {@code SmallScaleBuySide}'s next
 * level is MULTI ({@code final MapperC<X> thenArg4 = thenArg3.flattenList();}) and is a context
 * line in both. So rungs A+B heal 4 files and A+B+C heal 8.
 *
 * <p><b>Why rungs B and C exist at all (measured at source, not inferred).</b> The carriers'
 * {@code thenArg0..5} are NOT emitted by {@code FunctionExpressionRenderer}: they sit inside the
 * rule's {@code .mapSingleToItem(reportInstruction -> { ... })} block lambda, so they come from
 * the IN-LAMBDA twin {@code CollectionHandler.tryDeepThenHoist}, whose admitted-shape lists are a
 * strict SUBSET of the SET seat's. The positive control is in the corpus: drr 7.x
 * {@code Extract_TradingCapacity} is the SAME rosetta chain at STATEMENT level and the fork emits
 * all four golden forms there today, byte-green. Both rungs are therefore LAW-69 completions of
 * arms the sibling seat already carries, not new mechanisms.
 *
 * <p><b>The golden vs fork shape</b> (drr 7.0.0 asic
 * {@code trade/reports/CountryOfCounterparty2Rule}, the file's only hunk):
 * <pre>
 * golden:  final MapperListOfLists&lt;JurisdictionPartyInformation&gt; thenArg3 = thenArg2
 *              .filterListNullSafe(item -&gt; areEqual(item.&lt;ReferenceWithMetaParty&gt;map("getPartyReference", ...)
 *                  .&lt;Party&gt;map("Type coercion", _referenceWithMetaParty -&gt; _referenceWithMetaParty.getValue()), ...);
 *          final MapperS&lt;JurisdictionPartyInformation&gt; thenArg4 = MapperS.of(thenArg3
 *              .flattenList().get());
 * fork:    final MapperC&lt;JurisdictionPartyInformation&gt; thenArg3 = thenArg2
 *              .filterItemNullSafe(item -&gt; areEqual(item.&lt;ReferenceWithMetaParty&gt;map("getPartyReference", ...)
 *                  .&lt;Party&gt;map("Type coercion", referenceWithMetaParty0 -&gt; referenceWithMetaParty0 == null ? null
 *                      : referenceWithMetaParty0.getValue()), ...);
 *          final MapperS&lt;JurisdictionPartyInformation&gt; thenArg4 = thenArg3
 *              .flattenList().get();
 * </pre>
 *
 * <p><b>The probe verdict this law answers (LAW 75).</b> {@code PROBE29-F11cf} cloned the binding
 * walk with the rule guard promoted to a parameter and scored every candidate gate over the whole
 * 46,882-row matrix, both routes. The clone's own positive control ({@code curAgrees}, the
 * guard-ON clone vs the LIVE wrapper verdict) was true on <b>46,882/46,882 rows on each route</b>.
 * <ul>
 *   <li><b>G3 (this law) -- {@code rule-path + cf=LoL + site=filter}: 8 admitted, 8 BAND, 8 moved,
 *       ZERO green rows admitted at all.</b> The carriers are
 *       {@code CountryOfCounterparty2Rule} and {@code SmallScaleBuySideEntityIndicatorRule} over
 *       drr 7.0.0/7.1.0/7.2.0/7.3.0.</li>
 *   <li>G0 bare guard-drop: 181 rows moved, <b>130 GREEN groups</b> -- refuted.</li>
 *   <li>G4 {@code cf=C}: refuted TWICE -- by those 130 green groups, and because
 *       {@code SmallScaleBuySideEntityIndicatorRule}'s OWN extract site already emits golden's
 *       {@code mapItem}, so a C arm opens a NEW divergence in the file this law heals.</li>
 *   <li>G2 ({@code cf=LoL}, BOTH overloads): byte-equal at the render seats but admits five extra
 *       GREEN rows ({@code BasketConstituentUnitOfMeasureRule}'s rule-path extract, already
 *       answered identically by the #397 arm) and so exposes a green file to the decl-half
 *       consumers this round did not instrument. G3 confines the blast radius to the carriers.</li>
 * </ul>
 * The whole {@code MapperListOfLists} population is 69 rows / 10 shapes; every LoL row that is not
 * a carrier is either a FUNCTION-path row (already live, a guard change cannot reach it) or the
 * single rule-path EXTRACT row the #397 {@code ruleThenPipedLoLReceiver} arm already answers. So
 * this law is the LAW-69 two-halves completion of #397 -- the extract half has honoured a piped
 * LoL receiver on the rule path since #397; the filter half had not.
 *
 * <p><b>The #430 AWAY class, reproduced and excluded STRUCTURALLY.</b>
 * {@code CollectionHandler}'s own guard comment records that the pre-#430 overlay "moved 6 drr
 * POJO rule renders (NotionalQuantityLeg2Rule AWAY class)". The probe reproduces that regression
 * file-for-file -- {@code NotionalQuantityLeg2Rule} over exactly six drr POJO cells (5.61.0,
 * 6.34.1, 6.35.0, 6.36.0, 6.37.0, 6.38.0), flip vector {@code site=extract, cur=null -> cf=C,
 * cfBodyMulti=true -> mapItemToList}. It is {@code cf == C} at the {@code extract} site, so it is
 * not in G3's admitted set at all: excluded by the wrapper key, never by enumeration.
 * {@code corpus_control3} scans drr 6.34.1 whole -- one of those six cells -- so the AWAY class is
 * under a real whole-cell control and not merely under a fixture.
 *
 * <p><b>LAW 74 -- this law is a compile REPAIR.</b> {@code MapperListOfLists} declares
 * {@code filterListNullSafe(Function<MapperC<T>,Boolean>)} and {@code flattenList()} but NO
 * {@code filterItemNullSafe}; {@code MapperC} declares {@code filterItemNullSafe} but NO
 * {@code flattenList} (the only {@code flattenList} in the runtime is
 * {@code MapperListOfLists:113}). Both carriers declare {@code thenArg2} as
 * {@code MapperListOfLists} (an unchanged CONTEXT line in both diffs) and then call
 * {@code .filterItemNullSafe} on it, and declare {@code thenArg3} as {@code MapperC} and call
 * {@code .flattenList()} on it: two {@code cannot find symbol} errors per file over 8 files.
 * Expect javac29 PRE &gt;= 8 -&gt; POST 0. Every carrier is an already-waivered, non-compiling
 * mismatch, so no green file can carry the pre-fix form at a site this gate accepts.
 *
 * <p><b>CROSS-SUITE PIN (LAW 81).</b> {@code FilterPredicateMetaDerefSeatTest.KNOWN_RESIDUE_7}
 * listed both carriers with a T3 (guarded-deref) count one ABOVE golden's --
 * {@code CountryOfCounterparty2Rule fork=[0, 3, 7] golden=[0, 3, 6]} and
 * {@code SmallScaleBuySideEntityIndicatorRule fork=[0, 2, 3] golden=[0, 2, 2]} (both counts
 * re-measured on the seat-29 dump: 7/6 and 3/2). Delta 3 removes exactly that one ternary per
 * file, so both rows reach golden's triple and LEAVE that suite's mismatch set. {@code
 * law3-apply29.py} deletes them in the SAME commit as the law; its {@code --revert} lane restores
 * them, so the mutation run stays honest.
 *
 * <p><b>RED at the pre-law head -- MEASURED at the seat-29 chain's RED leg ({@code fa277393},
 * identical on BOTH routes)</b>: a1, a2, a3, corpus_c1, corpus_control1. b1..b3 GREEN in both
 * states; corpus_control0 GREEN in both states (it reads golden only).
 *
 * <p><b>MEASURED MUTATIONS (LAW 82 -- the seat-29 chain's suite-lane loop at {@code 687c8feb};
 * the set below is the RECORDED failing set from the f29-mut-m-law3 log):</b>
 * <ul>
 *   <li><b>m-law3</b> ({@code law3-apply29.py --revert}: all three rungs and the cross-suite
 *       re-pin) -- MEASURED a1, a2, a3, corpus_c1, corpus_control1 (5F -- exactly the drafted
 *       claim) <b>plus the cross-suite catch</b>: {@code FilterPredicateMetaDerefSeatTest}'s
 *       {@code corpus_control1} fired in the same lane. The lane reverts law AND pin together
 *       (the 14-row pre-seat pin restored), so the assert's failure delta is exactly the rows
 *       OTHER laws moved since that pin was measured -- the review's re-derivation: TWO rows,
 *       {@code Enrich_TransactionReportInstructionTestPackDefault} and
 *       {@code FirstExerciseDateRule}, both law-1-family movements (laws 9/9b). Law 3's own
 *       carriers restore to the pin exactly when reverted, which is the LAW-81 point measured:
 *       the pin travels with the law, and what fires under the lane is the surrounding seat's
 *       movement against the restored pin, not this law's own carriers.</li>
 * </ul>
 * <p><b>Designed manual severs -- stated, NOT run in the seat-29 chain</b> (no apply-script flag;
 * their failing sets remain claims):
 * <ul>
 *   <li><b>m-law3-A</b> (rung A alone severed: {@code boundImplicitReceiverType(RFilterExpr,
 *       scope, tu)} returns {@code boundImplicitReceiverType0(...)} verbatim) -- CLAIMED: a1, a2,
 *       a3, corpus_c1, corpus_control1. Rung A is the head of the chain, so severing it must sink
 *       everything; a SMALLER measured set means a rung is reachable without it and the law's
 *       dependency story is wrong.</li>
 *   <li><b>m-law3-B</b> (rung B alone severed: the {@code filterPreservesListOfLists} arm dropped
 *       from {@code tryDeepThenHoist}) -- CLAIMED: a2, corpus_c1, corpus_control1; a1 and a3 hold
 *       (they pin the render and the wrap, not the decl). This is the mutation that proves the
 *       decl half is NOT free-riding on rung A.</li>
 *   <li><b>m-law3-C</b> (rung C alone severed: {@code isFlattenCollapseOnlyElementLocal} dropped
 *       from the re-wrap disjunct) -- CLAIMED: a3 and the {@code CountryOfCounterparty2Rule} half
 *       of corpus_c1 ONLY; a1, a2 and the {@code SmallScaleBuySide} half hold. Witness-unique to
 *       delta 4, whose whole population is one basename.</li>
 * </ul>
 */
class RulePathFilterLolSeatTest {

    private static final Path REPO_ROOT =
            Path.of(System.getProperty("user.dir")).resolve("..").normalize();

    private static final List<Path> BUILTINS_SEARCH_ROOTS = List.of(
            REPO_ROOT.resolve("test-corpus/rune-dsl-builtins/rune-runtime/src/main/resources/model"),
            REPO_ROOT.resolve("rune-dsl/rune-runtime/src/main/resources/model"),
            REPO_ROOT.resolve("rune-runtime/src/main/resources/model"));

    static boolean builtinsAvailable() {
        return BUILTINS_SEARCH_ROOTS.stream().anyMatch(Files::isDirectory);
    }

    static boolean irProviderOnClasspath() {
        try {
            Class.forName("com.regnosys.rosetta.generator.java.ir.IRGenerationProviderImpl");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /** Cell A = drr 7.0.0 -- the carrier cell (7.1/7.2/7.3 carry the same two rows). */
    private static final Path CELL_A_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path GOLDEN_A = CELL_A_ROOT.resolve("rosetta-source/src/generated/java");
    /**
     * Cell B = drr 6.34.1 -- the mechanism's OTHER reach cell, chosen deliberately. All 8 flip
     * rows are drr 7.x, so any cell outside 7.x is an EMPTY-DOMAIN guard for the law; 6.34.1 is
     * additionally one of the SIX cells carrying the #430 {@code NotionalQuantityLeg2Rule} AWAY
     * class, so the same scan that proves the law is inert here also proves the recorded
     * regression stays excluded (the seat-28 empty-domain-guard convention, with a carrier).
     */
    private static final Path CELL_B_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path GOLDEN_B = CELL_B_ROOT.resolve("rosetta-source/src/generated/java");

    static boolean cellAAvailable() {
        return Files.isDirectory(GOLDEN_A);
    }

    static boolean cellBAvailable() {
        return Files.isDirectory(GOLDEN_B);
    }

    static boolean cellAAndIrProviderAvailable() {
        return cellAAvailable() && irProviderOnClasspath();
    }

    /**
     * a1 = the {@code CountryOfCounterparty2} shape reduced: a rule-path then-chain hoisted inside
     * a NAMED-param extract lambda, whose third step filters a piped {@code MapperListOfLists}.
     *     rung A -- the member name and the item-form coercion.
     * a2 = the same chain read at the DECL: the filtered level keeps the LoL wrapper (rung B).
     * a3 = the {@code flatten only-element} tail immediately after that filter (rung C).
     *      (a1/a2/a3 read three disjoint deltas out of ONE rendered rule, exactly as the carrier
     *      carries them in one hunk -- three assertions, one shape.)
     * b1 = the #430 AWAY-class witness: a rule-path EXTRACT over a bound receiver must NOT gain
     *      the binding -- witness-unique on the tokens a widened extract admission would ADD.
     * b2 = the FUNCTION-path LoL filter -- live since #430 and unchanged by this law.
     * b3 = a rule-path filter over a plain {@code MapperC} -- the WRAPPER key is load-bearing.
     *
     * <p>Lexer-safe identifiers: no {@code tag}, {@code single} or {@code label} anywhere; the
     * meta-annotated attributes are {@code owner} and {@code counterparty}.
     */
    private static final String MODEL = """
            namespace census.seat29f11a
            version "1.0.0"

            enum Seat29KindEnum:
                PRIMARY
                SECONDARY

            type Seat29Owner:
                ident string (0..1)

            type Seat29Member:
                owner Seat29Owner (0..1)
                    [metadata reference]
                code string (0..1)

            type Seat29Group:
                kind Seat29KindEnum (0..1)
                members Seat29Member (0..*)

            type Seat29Side:
                counterparty Seat29Owner (0..1)
                    [metadata reference]

            type Seat29Root:
                groups Seat29Group (0..*)
                directMembers Seat29Member (0..*)
                side Seat29Side (0..1)

            func Seat29Allowed: <"IsAllowableActionForASIC twin - the chain-head filter">
                inputs:
                    r Seat29Root (1..1)
                output:
                    result boolean (1..1)
                set result:
                    r -> side exists

            reporting rule A1RulePathLolFilter from Seat29Root: <"a1/a2/a3 - THE CountryOfCounterparty2 SHAPE: a rule-path then-chain inside a named-param extract lambda whose third step filters a piped MapperListOfLists, followed by the flatten only-element tail">
                filter Seat29Allowed
                then extract rootItem [
                    extract groups
                    then filter kind = Seat29KindEnum -> PRIMARY
                    then extract members
                    then filter owner any = rootItem -> side -> counterparty
                    then flatten only-element
                    then extract code
                ]
                as "a1"

            reporting rule B1RulePathExtractAway from Seat29Root: <"b1 - the #430 AWAY-class witness: a rule-path EXTRACT step over a thenArg-bound receiver keeps its item-level method - the extract overload is NOT widened (G3, not G2)">
                filter Seat29Allowed
                then extract rootItem [
                    extract groups
                    then filter kind = Seat29KindEnum -> PRIMARY
                    then extract kind
                ]
                as "b1"

            func B2FunctionPathLolFilter: <"b2 - the FUNCTION-path LoL filter (the Extract_TradingCapacity class): live since #430, and this law must not disturb it">
                inputs:
                    r Seat29Root (1..1)
                output:
                    picked Seat29Member (0..1)
                set picked:
                    r -> groups
                        then extract members
                        then filter owner -> ident exists
                        then flatten only-element

            reporting rule B3RulePathPlainFilter from Seat29Root: <"b3 - the WRAPPER key is load-bearing: the SAME rule path, the SAME predicate as a1, but the receiver is a plain MapperC (NOT a list-of-lists) - it must keep filterItemNullSafe">
                filter Seat29Allowed
                then extract rootItem [
                    extract directMembers
                    then filter owner any = rootItem -> side -> counterparty
                    then only-element
                    then extract code
                ]
                as "b3"
            """;

    // =========================================================================
    // Part A -- the positive fixtures (the law's three rungs, one rendered rule)
    // =========================================================================

    /**
     * a1 -- RUNG A, the render seat. A rule-path filter whose implicit receiver is bound to a
     * {@code MapperListOfLists} selects upstream's list-level member, and the predicate's inner
     * meta deref takes the {@code MapperC} ITEM form (the bare {@code _x -> _x.getValue()}), not
     * the {@code MapperS} null-safe ternary. Both tokens are added by rung A and by nothing else.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_rulePathFilterOverListOfListsSelectsFilterListNullSafe() throws IOException {
        String out = rule("A1RulePathLolFilterRule.java");
        assertContains(out, ".filterListNullSafe(");
        assertTrue(!codeOnly(out).contains(
                        ".filterItemNullSafe(item -> areEqual(item.<ReferenceWithMetaSeat29Owner>"),
                "the item-level filter over the piped list-of-lists must be gone:\n" + out);
        // The item-form coercion, counted rather than name-matched. The predicate carries TWO
        // "Type coercion" hops -- the item-rooted one (which flips to the MapperC item form) and
        // the reportInstruction-rooted one (which keeps the MapperS null-safe ternary in golden
        // too). MEASURED at the pre-law head: 2 guarded derefs; the law removes exactly ONE.
        // This is the same arithmetic as the cross-suite re-pin of
        // FilterPredicateMetaDerefSeatTest.KNOWN_RESIDUE_7 (fork T3 7 -> 6 and 3 -> 2).
        assertEquals(1, count(codeOnly(out), "== null ? null :"),
                "exactly ONE guarded deref must remain - the item-rooted coercion takes the"
                + " MapperC item form and the caller-rooted one keeps its ternary:\n" + out);
        assertContains(out, "referenceWithMetaSeat29Owner.getValue()");
    }

    /**
     * a2 -- RUNG B, the decl half. The filtered level KEEPS the list-of-lists wrapper: a filter is
     * element-AND-shape preserving ({@code filterListNullSafe: MapperListOfLists<T> ->
     * MapperListOfLists<T>}), so the {@code thenArg} the in-lambda hoist declares must be a
     * {@code MapperListOfLists}, matching the member rung A selected (the #274 two-halves law).
     * Without rung B this assertion fails while a1 passes -- which is exactly the state in which
     * the corpus carriers heal ZERO whole files.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_theFilteredLevelKeepsTheListOfListsDecl() throws IOException {
        String out = rule("A1RulePathLolFilterRule.java");
        String code = codeOnly(out);
        int filterIdx = code.indexOf(".filterListNullSafe(");
        assertTrue(filterIdx > 0, "rung A must have fired before rung B can be read:\n" + out);
        String beforeFilter = code.substring(0, filterIdx);
        int declIdx = beforeFilter.lastIndexOf("final ");
        assertTrue(declIdx >= 0, "the filtered level must have a thenArg decl:\n" + out);
        String decl = beforeFilter.substring(declIdx);
        assertTrue(decl.startsWith("final MapperListOfLists<"),
                "the level whose value is filterListNullSafe must be declared MapperListOfLists,"
                + " got <" + decl.trim() + "> in:\n" + out);
    }

    /**
     * a3 -- RUNG C, the {@code flatten only-element} consumer. The collapse renders
     * {@code <lol>.flattenList().get()}, a bare {@code List} against the {@code MapperS}
     * declaration; golden re-wraps it. Witness-uniqueness: the assertion keys on the wrapped
     * TERMINATOR {@code .flattenList().get());} -- the unwrapped form ends {@code .get();} -- so
     * it cannot be satisfied by the presence of {@code flattenList} alone, which is there in both
     * states once rungs A and B have fired.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a3_theFlattenOnlyElementCollapseIsReWrapped() throws IOException {
        String out = rule("A1RulePathLolFilterRule.java");
        String flat = collapse(codeOnly(out));
        assertTrue(flat.contains(".flattenList().get());"),
                "the flatten only-element collapse must be re-wrapped MapperS.of(...):\n" + out);
        assertTrue(!flat.contains(".flattenList().get();"),
                "the bare .get() collapse must be gone:\n" + out);
    }

    // =========================================================================
    // Part B -- the decline pins (witness-unique on tokens the flip ADDS)
    // =========================================================================

    /**
     * b1 -- the #430 AWAY-class pin. Law 3 widens the FILTER overload ONLY (the probe's G3). A
     * rule-path EXTRACT step must not gain the binding, because the recorded AWAY class
     * ({@code NotionalQuantityLeg2Rule} over six drr POJO cells) flips {@code mapItem ->
     * mapItemToList} exactly there off a {@code cf=C} binding.
     *
     * <p>Witness-uniqueness: the assertions key on the three tokens a widened EXTRACT admission
     * would ADD -- {@code mapItemToList}, {@code mapListToItem} and the {@code MapperListOfLists}
     * wrapper -- never on the presence of {@code mapItem}, which is there in both states. The real
     * six-cell lock is {@code corpus_control3}, which scans drr 6.34.1 whole; this fixture pins
     * the SHAPE so a re-siting to G2 fails here before it reaches a corpus run.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b1_rulePathExtractKeepsItsItemLevelMethod() throws IOException {
        String code = codeOnly(rule("B1RulePathExtractAwayRule.java"));
        assertTrue(!code.contains("mapItemToList"),
                "law 3 must NOT widen the EXTRACT overload (the #430 AWAY class):\n" + code);
        assertTrue(!code.contains("mapListToItem"),
                "law 3 must NOT widen the EXTRACT overload (the #430 AWAY class):\n" + code);
        assertTrue(!code.contains("MapperListOfLists"),
                "a rule-path extract chain must not acquire a list-of-lists wrapper:\n" + code);
    }

    /**
     * b2 -- the FUNCTION path is untouched. The #430 law has selected {@code filterListNullSafe}
     * off a piped {@code MapperListOfLists} inside a FUNCTION since #430; rung A adds a rule-path
     * arm and must leave that verdict exactly where it was. This is the pin that fails if rung A
     * is implemented by weakening {@code boundImplicitReceiverType0} for everyone instead of
     * adding a FILTER-only overload.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b2_functionPathListOfListsFilterIsUnchanged() throws IOException {
        String out = fn("B2FunctionPathLolFilter.java");
        assertContains(out, ".filterListNullSafe(");
        assertContains(out, "MapperListOfLists");
    }

    /**
     * b3 -- the WRAPPER key is load-bearing. A rule-path filter whose bound receiver is a plain
     * {@code MapperC} must keep {@code filterItemNullSafe}: this is the whole difference between
     * G3 (8 rows, all band) and G5 ({@code rule-path + site=filter}, any wrapper -- 14,549 rows
     * admitted). Witness-unique on {@code filterListNullSafe}, the token the flip adds.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b3_rulePathFilterOverPlainMapperCKeepsItemForm() throws IOException {
        String code = codeOnly(rule("B3RulePathPlainFilterRule.java"));
        assertTrue(!code.contains("filterListNullSafe"),
                "a rule-path filter over a plain MapperC must NOT take the list-level member"
                + " - the wrapper is the gate, not the path:\n" + code);
        assertTrue(!code.contains("MapperListOfLists"),
                "a MapperC-receiver chain must not acquire the list-of-lists wrapper:\n" + code);
        assertContains(code, "filterItemNullSafe");
        // b3's predicate is a1's predicate verbatim; only the RECEIVER differs. So the two
        // guarded derefs must BOTH survive here -- if the item-form coercion appeared without a
        // list-of-lists receiver, rung A would be keyed on the path and not on the wrapper.
        assertEquals(2, count(code, "== null ? null :"),
                "both guarded derefs must survive over a MapperC receiver:\n" + code);
    }

    // =========================================================================
    // Part C -- the corpus carriers (8 whole-file rows; drr 7.0.0 shown, 7.1/7.2/7.3 identical)
    // =========================================================================

    private static final String CC2 =
            "drr/regulation/asic/rewrite/trade/reports/CountryOfCounterparty2Rule.java";
    private static final String SSBS =
            "drr/regulation/asic/rewrite/valuation/reports/SmallScaleBuySideEntityIndicatorRule.java";

    /** The two drr 7.0.0 carriers, both single-hunk, both byte-whole under the three rungs. */
    private static final List<String> CARRIERS_A = List.of(CC2, SSBS);

    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_drr7CarriersByteIdentical() throws IOException {
        for (String p : CARRIERS_A) {
            lockA(p);
        }
    }

    /**
     * control0 -- golden is the oracle (prove the instrument can fail). Quotes the REAL golden
     * bytes of both carriers: golden takes the list-level member, declares the LoL wrapper, uses
     * the item-form coercion, and -- for {@code CountryOfCounterparty2Rule} ONLY -- re-wraps the
     * flatten collapse. The last two assertions are the delta-4 asymmetry stated as an oracle
     * fact: {@code SmallScaleBuySide}'s next level is MULTI and carries NO wrap in golden either,
     * which is why rung C heals four files and not eight.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control0_goldenCarriesEveryRungAtTheCarriers() throws IOException {
        String cc2 = collapse(Files.readString(GOLDEN_A.resolve(CC2)));
        assertTrue(cc2.contains(".filterListNullSafe("), "golden must take the list-level member");
        assertTrue(cc2.contains("final MapperListOfLists<JurisdictionPartyInformation> thenArg3"),
                "golden must declare the filtered level MapperListOfLists");
        assertTrue(cc2.contains("_referenceWithMetaParty -> _referenceWithMetaParty.getValue()"),
                "golden must use the MapperC item-form coercion inside the filter predicate");
        assertTrue(cc2.contains(".flattenList().get());"),
                "golden must re-wrap the flatten only-element collapse (delta 4)");
        assertTrue(!cc2.contains(".filterItemNullSafe(item -> areEqual(item.<ReferenceWithMetaParty>"
                + "map(\"getPartyReference\""),
                "golden must NOT carry the fork's item-level filter at the carrier site");

        String ssbs = collapse(Files.readString(GOLDEN_A.resolve(SSBS)));
        assertTrue(ssbs.contains(".filterListNullSafe("), "golden must take the list-level member");
        assertTrue(ssbs.contains("final MapperListOfLists<JurisdictionPartyInformation> thenArg3"),
                "golden must declare the filtered level MapperListOfLists");
        assertTrue(ssbs.contains("_referenceWithMetaParty -> _referenceWithMetaParty.getValue()"),
                "golden must use the MapperC item-form coercion inside the filter predicate");
        assertTrue(!ssbs.contains(".flattenList().get());"),
                "delta 4 must be ABSENT here: SmallScaleBuySide's next level is MULTI, so golden"
                + " carries the bare flattenList() -- this asymmetry is why rung C heals 4, not 8");
    }

    /**
     * control1 -- LAW 79, the whole-cell UNION scan on drr 7.0.0: per file the (T1, T2, T3) triple
     * must equal golden's, file for file over the UNION, beyond the NAMED residue. A cell-scoped
     * carrier lock cannot see an over-fire elsewhere in the cell; this can.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control1_forkDrr7WholeCellListOfListsFiltersEqualGoldenFileByFile()
            throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");
        assertEquals(List.of(), drrAGenErrors,
                "drr 7.0.0 reported a generation error - the scan is incomplete");
        assertUnionEqual(scan(drrAOutput), scan(readGoldenTree(GOLDEN_A)), drrAOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_DRR7, DOMAIN_DRR7);
    }

    /**
     * control2 -- LAW 77 route parity. Law 3 sits in {@code CollectionHandler}, which BOTH routes
     * walk, and the probe measured the two routes byte-identical over all 46,882 rows; this
     * re-proves it per FILE at the carriers on the real IR seams.
     */
    @Test
    @EnabledIf("cellAAndIrProviderAvailable")
    void corpus_control2_irRouteMatchesLegacyForCarriers() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 legacy-route generation did not run");
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", CELL_A_ROOT), new ArrayList<>());
        for (String p : CARRIERS_A) {
            assertEquals(drrAOutput.get(p), irOut.get(p), "route divergence: " + p);
        }
    }

    /**
     * control3 -- LAW 79 on the OTHER reach cell, drr 6.34.1. Two jobs in one scan:
     * <ol>
     *   <li>the EMPTY-DOMAIN guard -- all 8 flip rows are drr 7.x, so this cell must be
     *       byte-inert under the law; and</li>
     *   <li>the #430 AWAY-class lock -- 6.34.1 is one of the six drr POJO cells carrying
     *       {@code NotionalQuantityLeg2Rule}, so if the wrapper key ever slips to {@code cf=C}
     *       the recorded regression reappears HERE, in a scan whose domain includes it.</li>
     * </ol>
     */
    @Test
    @EnabledIf("cellBAvailable")
    void corpus_control3_forkDrr6341WholeCellIsInertAndTheAwayClassHolds() throws IOException {
        assertNotNull(drrBOutput, "drr 6.34.1 generation did not run");
        assertEquals(List.of(), drrBGenErrors,
                "drr 6.34.1 reported a generation error - the scan is incomplete");
        assertTrue(drrBOutput.keySet().stream()
                        .anyMatch(k -> k.endsWith("/NotionalQuantityLeg2Rule.java")),
                "the AWAY class must be INSIDE this scan's domain, else control3 proves nothing"
                + " about it (LAW: a control scans the domain it claims)");
        assertUnionEqual(scan(drrBOutput), scan(readGoldenTree(GOLDEN_B)), drrBOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_DRR634, DOMAIN_DRR634);
    }

    /**
     * The NAMED residue of drr 7.0.0 (LAW 73: pin the SET, not the count) -- MEASURED at the
     * seat-29 chain ({@code 687c8feb}: suite GREEN with this set on both routes).
     *
     * <p><b>RE-MEASURED at the seat-30 chain head {@code e223ce19}</b> — 1 entry, transcribed
     * VERBATIM from this control's own failing print (LAW 81), down from 2.
     * {@code GetUnderlierProductIdentifierLeg1.java} (was {@code fork=[0, 0, 0] golden=[0, 1, 0]})
     * left the list: <b>law 5</b> ({@code thenArgDeclKindFromCompiled} — the then-arg DECL reads
     * the compiled stamp instead of re-reading S) healed it WHOLE in all four drr 7.x cells, so
     * its missing {@code MapperListOfLists} decl (T2) now renders. Golden's T2 = 1 keeps the file
     * inside the union domain, so {@code DOMAIN_DRR7} is UNMOVED at 37.
     */
    // LAW 81 re-pin (v3.1 flip seat 33, law A.4): the GetBasketConstituents row (fork=[0, 3, 0] golden=[0, 4, 0]) LEFT this list -
    // facet lolDefaultBodyMulti moved this scan's tuple to golden's (the file stays BANDED on its A.3/A.5/A.1/A.2
    // residue; LolDefaultBodyMultiSeatTest pins that residue by name); transcribed from the control print (A4-trip1.log).
    private static final List<String> KNOWN_RESIDUE_DRR7 = List.of();

    /**
     * The NAMED residue of drr 6.34.1 (LAW 73) -- MEASURED EMPTY at the seat-29 chain
     * ({@code 687c8feb}). The law's own contribution here must be EMPTY (the empty-domain guard);
     * any entry naming {@code NotionalQuantityLeg2Rule} is the #430 regression returning and is a
     * STOP, not a pin.
     */
    private static final List<String> KNOWN_RESIDUE_DRR634 = List.of();

    /**
     * The union domain is MEASURED and pinned (LAW 73) - a negative value means an unpinned call
     * site. Set from the chain's control1 output at the law-3 head.
     */
    private static final int DOMAIN_DRR7 = 37;

    /**
     * The union domain is MEASURED and pinned (LAW 73) - a negative value means an unpinned call
     * site. Set from the chain's control3 output at the law-3 head.
     */
    private static final int DOMAIN_DRR634 = 61;

    /**
     * (T1, T2, T3) per file, one token per RUNG so a partial repair cannot hide (LAW 80):
     * <ul>
     *   <li><b>T1</b> -- rung A's ADDED member: {@code filterListNullSafe(} occurrences.</li>
     *   <li><b>T2</b> -- rung B's ADDED decl: {@code final MapperListOfLists&lt;} occurrences.</li>
     *   <li><b>T3</b> -- rung C's ADDED wrap: occurrences of the WRAPPED terminator
     *       {@code .flattenList().get());} in the whitespace-collapsed code. The unwrapped form
     *       ends {@code .flattenList().get();}, so this counts the wrap and not the flatten --
     *       deliberately global, so a spurious re-wrap anywhere in the cell fails the control.</li>
     * </ul>
     * Each {@code CountryOfCounterparty2Rule} cell moves T1 +1, T2 +1, T3 +1; each
     * {@code SmallScaleBuySideEntityIndicatorRule} cell moves T1 +1, T2 +1, T3 +0.
     * <p>{@code filterListNullSafe} / {@code filterItemNullSafe} / {@code filterSingleNullSafe} are
     * the complete set {@code CollectionHandler.filterMethod} can emit; re-verify with a corpus
     * grep before pinning the domain (the LAW-79 domain law).
     */
    private static Map<String, int[]> scan(Map<String, String> tree) {
        Map<String, int[]> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : tree.entrySet()) {
            String code = codeOnly(e.getValue());
            String flat = collapse(code);
            int t1 = count(code, "filterListNullSafe(");
            int t2 = count(code, "final MapperListOfLists<");
            int t3 = count(flat, ".flattenList().get());");
            if (t1 + t2 + t3 > 0) {
                out.put(e.getKey(), new int[] {t1, t2, t3});
            }
        }
        return out;
    }

    private static int count(String haystack, String needle) {
        int n = 0;
        int from = 0;
        while ((from = haystack.indexOf(needle, from)) >= 0) {
            n++;
            from += needle.length();
        }
        return n;
    }

    // =========================================================================
    // The union assert (LAW 73: pin the SET, not the count)
    // =========================================================================

    /**
     * The scan universe is the token-bearing union INTERSECTED with the files this harness
     * actually emits (rule/report/function kinds): whether the fork emits every expected FILE is
     * the D11 ring's question (missingOutput), not this suite's, and golden's POJO/metafield
     * kinds -- which these generators never produce -- can legitimately carry the tokens.
     */
    private static void assertUnionEqual(Map<String, int[]> a, Map<String, int[]> b,
            java.util.Set<String> emittedA, String aName, String bName, List<String> knownResidue,
            int expectedDomain) {
        assertTrue(expectedDomain >= 0,
                "the union domain is MEASURED and pinned (LAW 73) - a negative value means an"
                + " unpinned call site");
        List<String> mismatched = new ArrayList<>();
        java.util.Set<String> universe = new java.util.TreeSet<>(a.keySet());
        universe.addAll(b.keySet());
        universe.retainAll(emittedA);
        int[] zero = new int[3];
        for (String key : universe) {
            int[] ac = a.getOrDefault(key, zero);
            int[] bc = b.getOrDefault(key, zero);
            if (!java.util.Arrays.equals(ac, bc)) {
                mismatched.add(key + " " + aName + "=" + java.util.Arrays.toString(ac)
                        + " " + bName + "=" + java.util.Arrays.toString(bc));
            }
        }
        assertEquals(knownResidue, mismatched,
                "(T1, T2, T3) differ beyond the named residue in " + mismatched.size() + " file(s)");
        assertEquals(expectedDomain, universe.size(),
                "the union domain must equal the emitted token-bearing files (" + expectedDomain + ")");
    }

    // =========================================================================
    // Harness (the seat-28 FilterPredicateMetaDerefSeatTest shape verbatim)
    // =========================================================================

    private static Map<String, String> drrAOutput;
    private static List<String> drrAGenErrors;
    private static Map<String, String> drrBOutput;
    private static List<String> drrBGenErrors;

    @BeforeAll
    static void generateCells() throws IOException {
        if (cellAAvailable()) {
            List<String> errs = new ArrayList<>();
            drrAOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", CELL_A_ROOT), errs);
            drrAGenErrors = errs;
        }
        if (cellBAvailable()) {
            List<String> errs = new ArrayList<>();
            drrBOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", CELL_B_ROOT), errs);
            drrBGenErrors = errs;
        }
    }

    private static Map<String, String> generateCell(D11CorpusRegressionTest.CellSpec cell,
            List<String> errors) throws IOException {
        var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(),
                D11CorpusRegressionTest.emissionFilter(cell));
        var typeUtil = new JavaTypeUtil();
        var typeTranslator = new JavaTypeTranslator(typeUtil);
        var funcGen = new FunctionGenerator(gm, typeTranslator, typeUtil);
        var ruleGen = new RuleGenerator(gm, typeTranslator, funcGen);
        var reportGen = new ReportGenerator(gm, typeTranslator, funcGen);
        Map<String, String> output = new LinkedHashMap<>();
        for (RModel model : corpus.workspace().files()) {
            if (gm.shouldGenerate(model)) {
                String version = gm.version(model);
                collect(errors, ruleGen.generateClasses(model, version, output));
                collect(errors, reportGen.generateClasses(model, version, output));
            }
        }
        collect(errors, funcGen.generateWithErrors(output));
        return output;
    }

    /** The cell through the REAL {@code IRGeneration} seams (the D11 ON ring's wiring). */
    private static Map<String, String> generateCellOnIrRoute(D11CorpusRegressionTest.CellSpec cell,
            List<String> errors) throws IOException {
        String previous = System.getProperty(IRGeneration.PROPERTY);
        System.setProperty(IRGeneration.PROPERTY, "true");
        try {
            assertNotNull(IRGeneration.providerOrNull(),
                    "the IR provider must be resolvable under -Pir-on, else this is not an ON-route render");
            var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
            var gm = new GeneratorModel(corpus.workspace(),
                    D11CorpusRegressionTest.emissionFilter(cell));
            var typeUtil = new JavaTypeUtil();
            var typeTranslator = new JavaTypeTranslator(typeUtil);
            FunctionGenerator funcGen = IRGeneration.functionGenerator(gm, typeTranslator, typeUtil);
            assertTrue(!funcGen.getClass().equals(FunctionGenerator.class),
                    "the seam must hand back the IR-route FunctionGenerator, got " + funcGen.getClass());
            var ruleGen = new RuleGenerator(gm, typeTranslator, funcGen);
            var reportGen = new ReportGenerator(gm, typeTranslator, funcGen);
            Map<String, String> output = new LinkedHashMap<>();
            for (RModel model : corpus.workspace().files()) {
                if (gm.shouldGenerate(model)) {
                    String version = gm.version(model);
                    collect(errors, ruleGen.generateClasses(model, version, output));
                    collect(errors, reportGen.generateClasses(model, version, output));
                }
            }
            collect(errors, funcGen.generateWithErrors(output));
            return output;
        } finally {
            if (previous == null) {
                System.clearProperty(IRGeneration.PROPERTY);
            } else {
                System.setProperty(IRGeneration.PROPERTY, previous);
            }
        }
    }

    private static Map<String, String> readGoldenTree(Path root) throws IOException {
        Map<String, String> out = new LinkedHashMap<>();
        try (var stream = Files.walk(root)) {
            stream.filter(p -> p.toString().endsWith(".java")).sorted().forEach(p -> {
                try {
                    out.put(root.relativize(p).toString().replace('\\', '/'), Files.readString(p));
                } catch (IOException e) {
                    throw new AssertionError("golden read failed: " + p, e);
                }
            });
        }
        return out;
    }

    private static void collect(List<String> sink, List<GenerationException> errors) {
        if (errors != null) {
            errors.forEach(e -> sink.add(e.getTargetPath() + " - " + e));
        }
    }

    private static void lockA(String path) throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run - corpus unavailable?");
        List<String> lockedErrors = drrAGenErrors.stream().filter(e -> e.contains(path)).toList();
        assertTrue(lockedErrors.isEmpty(),
                "the generator reported errors for the locked file " + path + ": " + lockedErrors);
        String generated = drrAOutput.get(path);
        assertNotNull(generated, "not generated in drr 7.0.0: " + path);
        Path goldenPath = GOLDEN_A.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "golden missing: " + goldenPath);
        assertEquals(normalize(Files.readString(goldenPath)), normalize(generated),
                "generated drr 7.0.0 output must byte-match golden (newline-normalized) for "
                + path + " - seat 29 law 3: a rule-path filter over a piped MapperListOfLists"
                + " takes filterListNullSafe, keeps the LoL decl, and re-wraps the flatten"
                + " collapse.");
    }

    private static void assertContains(String out, String needle) {
        assertTrue(out.contains(needle), "expected <" + needle + "> in:\n" + out);
    }

    /** Collapse every run of whitespace to one space, so a token split across lines still reads. */
    private static String collapse(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        boolean inWs = false;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n') {
                inWs = true;
                continue;
            }
            if (inWs && sb.length() > 0) {
                sb.append(' ');
            }
            inWs = false;
            sb.append(ch);
        }
        return sb.toString();
    }

    /** Strip line and block comments plus string literals so a javadoc or label never counts as code. */
    private static String codeOnly(String java) {
        StringBuilder sb = new StringBuilder(java.length());
        int i = 0;
        int n = java.length();
        while (i < n) {
            char ch = java.charAt(i);
            if (ch == '"') {
                int j = i + 1;
                while (j < n && java.charAt(j) != '"') {
                    if (java.charAt(j) == '\\') {
                        j++;
                    }
                    j++;
                }
                i = j + 1;
            } else if (ch == '/' && i + 1 < n && java.charAt(i + 1) == '/') {
                while (i < n && java.charAt(i) != '\n') {
                    i++;
                }
            } else if (ch == '/' && i + 1 < n && java.charAt(i + 1) == '*') {
                int e = java.indexOf("*/", i + 2);
                i = e < 0 ? n : e + 2;
            } else {
                sb.append(ch);
                i++;
            }
        }
        return sb.toString();
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n");
    }

    private static RLinkingResult linking;
    private static RModel mainModel;
    private static Map<String, String> fixtureOut;

    private static void link() throws IOException {
        if (linking == null) {
            RModel main = AstBuilder.buildFromString(MODEL, "seat29f11a.rosetta");
            main.setVersion("0.0.0.test");
            List<RModel> models = new ArrayList<>();
            models.add(main);
            models.addAll(loadBuiltinsOnly());
            linking = RWorkspace.build(models);
            mainModel = main;
        }
    }

    private static Map<String, String> render(Predicate<RModel> filter) throws IOException {
        link();
        GeneratorModel gm = new GeneratorModel(linking.workspace(), filter);
        JavaTypeUtil typeUtil = new JavaTypeUtil();
        JavaTypeTranslator tt = new JavaTypeTranslator(typeUtil);
        FunctionGenerator fg = new FunctionGenerator(gm, tt, typeUtil);
        RuleGenerator ruleGen = new RuleGenerator(gm, tt, fg);
        Map<String, String> out = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        ruleGen.generateClasses(mainModel, "1.0", out)
                .forEach(e -> errors.add(e.getTargetPath() + " - " + e));
        fg.generateWithErrors(out)
                .forEach(e -> errors.add(e.getTargetPath() + " - " + e));
        if (!errors.isEmpty()) {
            throw new AssertionError("fixture generation errors (a broken fixture"
                    + " must fail loudly, not skip): " + errors);
        }
        return out;
    }

    private static Map<String, String> fixture() throws IOException {
        if (fixtureOut == null) {
            fixtureOut = render(m -> "census.seat29f11a".equals(m.namespace()));
        }
        return fixtureOut;
    }

    private static String fn(String fileName) throws IOException {
        return lookup(fixture(), "functions/" + fileName);
    }

    private static String rule(String fileName) throws IOException {
        return lookup(fixture(), "reports/" + fileName);
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
            if (!Files.isDirectory(root)) {
                continue;
            }
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
                    try {
                        models.add(AstBuilder.buildFromFile(p));
                    } catch (Exception e) {
                        failures.add(p + " - " + e);
                    }
                });
        if (!failures.isEmpty()) {
            throw new AssertionError("[RulePathFilterLolSeatTest] builtins parse"
                    + " failures: " + String.join("; ", failures));
        }
        return models;
    }
}
