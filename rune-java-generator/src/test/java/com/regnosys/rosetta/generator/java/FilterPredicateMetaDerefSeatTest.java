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
 * SEAT 28, law 6 -- facet {@code filterPredicateMetaDeref}: <b>a meta-wrapper value consumed by
 * a function/rule call inside a FILTER predicate lambda dereferences null-safely, in EVERY
 * predicate position -- the NESTED operand (already admitted at PR #363) AND the call that IS
 * the predicate body</b>. {@code ReferenceHandler.metaDerefHoistRoute}'s filter arm carried an
 * {@code && inline.body() != expr} term; the term is deleted, so the gate reads
 * {@code inline.parent() instanceof RFilterExpr} alone.
 *
 * <p><b>The golden vs fork shape</b> -- two carrier forms, one gate:
 * <pre>
 * (A) the bare `filter &lt;Fn&gt;` form, drr 5.61.0 asic SpreadCurrencyLeg2Rule (single hunk):
 * fork:    .filterSingleNullSafe(item -&gt; isPriceMonetary.evaluate(item.get()));
 * golden:  .filterSingleNullSafe(item -&gt; {
 *              final ReferenceWithMetaPriceSchedule referenceWithMetaPriceSchedule = item.get();
 *              return isPriceMonetary.evaluate((referenceWithMetaPriceSchedule == null ? null
 *                      : referenceWithMetaPriceSchedule.getValue()));
 *          });
 *
 * (B) the EXPLICIT-args form, drr 7.x ExtractPartyByNameContains (single hunk):
 * fork:    .filterItemNullSafe(item -&gt; stringContains.evaluate(item.&lt;FieldWithMetaString&gt;map("getName", ...).get(), &lt;arg1&gt;));
 * golden:  .filterItemNullSafe(item -&gt; {
 *              final FieldWithMetaString fieldWithMetaString = item.&lt;FieldWithMetaString&gt;map("getName", ...).get();
 *              return stringContains.evaluate((fieldWithMetaString == null ? null
 *                      : fieldWithMetaString.getValue()), &lt;arg1&gt;);
 *          });
 * </pre>
 * In (B) the hoisted expression is a <b>NAV CHAIN</b>, not the bare piped item, and it sits at
 * <b>argument index 0 of a two-argument call</b> -- a design keyed on {@code item.get()} alone
 * misses that carrier entirely.
 *
 * <p><b>Why LAMBDA_CHANNEL and not BLOCK</b> (the sub-question the F13 lens left open, settled
 * at source rather than by test): {@code CollectionHandler.compileLambda}'s FILTER_PREDICATE
 * structural {@code MapperS.of} strip is guarded by
 * {@code if (bodyBuilder instanceof JavaExpression bodyExpr)}. The BLOCK route returns a
 * {@code JavaBlockBuilder}, which SKIPS the strip, so its {@code return} would keep the
 * spurious wrap. The LAMBDA_CHANNEL route returns the same {@code JavaExpression} the flat
 * path returns today and merely registers the decl; the strip runs unchanged and
 * {@code compileLambda}'s drain converts the stripped body to the block.
 *
 * <p><b>The machinery is proven ONE SEAT OVER, in the SAME FILE</b>: SpreadCurrencyLeg2Rule's
 * final statement is an EXTRACT-lambda meta-deref hoist that is byte-green in the fork today
 * (route BLOCK, the #144/#171 arm). Only the FILTER seat diverges. b3 pins that.
 *
 * <p><b>LAW 74</b>: a raw {@code ReferenceWithMetaPriceSchedule} passed to a
 * {@code PriceSchedule} parameter is an {@code incompatible types} error -- the PRE javac probe
 * must show it at the six drr 5.61.0 rows and the POST must exit 0.
 *
 * <p><b>The probe verdict this law answers (LAW 75).</b> The decline was measured from BOTH
 * sides and the two sides are ONE predicate:
 * <ul>
 *   <li>{@code PROBE28-F13r2} (the inline-body gate):
 *       {@code lambdaParent=RFilterExpr isFilterPred=true bodyIsExpr=true admitted=false} --
 *       x60 SpreadCurrencyLeg2, x2 FixedFloatRateLeg1, x2 InterestRatePrice, identical on
 *       BOTH routes;</li>
 *   <li>{@code PROBE28-F13r} (the route table): {@code where=fn:ExtractPartyByNameContains
 *       position=RFilterExpr:direct route=NONE admitted=false} x4, identical on BOTH routes.</li>
 * </ul>
 * <b>Blast radius, measured.</b> The widened gate is ridden by 3,677 distinct (cell,kind,file)
 * sites of which <b>3,611 are currently GREEN</b> (the F13r2 aggregation). What holds them flat
 * is the DOWNSTREAM {@code tryMetaDerefArg} chain -- param-type equality, the meta gate, the
 * #347 meta-PARAM decline and the coercion-service probe -- not the route. The golden-side
 * ceiling of the shape those guards can produce is <b>64 sites / 60 (cell,file) pairs
 * corpus-wide</b> (the F13 lens's paren-matched census over 165,544 goldens, whose first
 * regex-lookahead cut reported 4,653 and was RETRACTED after its positive control); 10 of those
 * are this law's carriers and 12 of the drr 5.61.0 remainder are ALREADY GREEN through the #363
 * nested arm -- i.e. the drain mechanism is green at twelve carriers on this very gate.
 *
 * <p><b>RED at the pre-seat blob</b>: a1, a2, a3, corpus_c1, corpus_c2, corpus_control1,
 * corpus_control3. b1..b4 GREEN in both states.
 *
 * <p><b>LAW 66/76 mutations</b> -- the seat's chain ran TWO on this suite (law6 and law6b; their
 * RECORDED failing sets are in the MEASURED MUTATIONS block below). The three severs planned here
 * are PLANS, not receipts (LAW 82):
 * (i) restore {@code && inline.body() != expr} -- the whole law reverted; this is the chain's law6
 * mutation, MEASURED below;
 * (ii) route the DIRECT body to BLOCK instead of LAMBDA_CHANNEL (the FINDING-1 discrimination) --
 * NOT RUN, banked to S29;
 * (iii) sever {@code filterPredicateSeat} at ReferenceHandler ~:3495 -- isolates the
 * IMPLICIT-item half (a1/a2) from the EXPLICIT-args half (a3) -- NOT RUN, banked to S29.
 * <p><b>MEASURED MUTATIONS (LAW 82 - the seat-28 mut28 suite-lane loop; each
 * mutation = the named apply-script reverted, the suite run, the script re-applied;
 * every set below is the RECORDED failing set from that run, never a claim):</b>
 * <ul>
 *   <li>the whole law reverted (law6-apply --revert: the route term restored) -> a1, a2, a3, corpus_c1, corpus_c1b, corpus_c2, corpus_control1, corpus_control3 (8F)</li>
 *   <li>the AMENDMENT alone reverted (law6b-apply --revert: the caller-input decline removed) -> b5 (1F, witness-unique - the decline-lock pin)</li>
 * </ul>
 */
class FilterPredicateMetaDerefSeatTest {

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

    /** Cell A = drr 5.61.0 -- the six bare-`filter &lt;Fn&gt;` rows across four namespaces. */
    private static final Path CELL_A_ROOT = Path.of("../test-corpus/drr/drr-5.61.0");
    private static final Path GOLDEN_A = CELL_A_ROOT.resolve("rosetta-source/src/generated/java");
    /** Cell B = drr 7.0.0 -- the EXPLICIT-args row (7.1/7.2/7.3 carry the same row). */
    private static final Path CELL_B_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
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
     * a1 = the drr 5.61.0 SpreadCurrencyLeg2 shape (SINGLE receiver, filterSingleNullSafe);
     * a2 = the mas FixedFloatRateLeg1 shape (MULTI receiver, filterItemNullSafe);
     * a3 = the drr 7.x ExtractPartyByNameContains shape (EXPLICIT args, a NAV CHAIN at index 0).
     * b1 = a NON-meta piped item (the param-type-equality decline);
     * b2 = a META-annotated callee param (the #347 identity-coercion decline);
     * b3 = the ADJACENT EXTRACT seat, green today -- it must not move;
     * b4 = a comparison predicate -- the other FILTER_PREDICATE arms must not move.
     *
     * <p>{@code tag} is a rune lexer keyword; the meta-annotated string list is {@code notes}.
     */
    private static final String MODEL = """
            namespace census.seat28f13a
            version "1.0.0"

            type Sched:
                ccy string (0..1)
                    [metadata scheme]
                amt number (0..1)

            type Party:
                name string (0..1)
                    [metadata scheme]

            type Holder:
                price Sched (0..1)
                    [metadata reference]
                prices Sched (0..*)
                    [metadata reference]
                plain Sched (0..1)
                notes string (0..*)
                    [metadata scheme]
                allowed boolean (0..1)

            func IsAllowable: <"IsAllowableActionForASIC twin - the chain-head filter">
                inputs:
                    h Holder (0..1)
                output:
                    result boolean (1..1)
                set result:
                    h -> allowed = True

            func IsMonetary: <"IsPriceMonetary twin - the predicate callee takes the BARE value type">
                inputs:
                    p Sched (1..1)
                output:
                    result boolean (1..1)
                set result:
                    p -> ccy exists

            func WantsMetaNote: <"b2 - the callee param CARRIES the meta, so the coercion is IDENTITY (#347)">
                inputs:
                    n string (1..1)
                        [metadata scheme]
                output:
                    result boolean (1..1)
                set result:
                    n exists

            func ToCode: <"b3 - the EXTRACT-seat twin of this law; green TODAY">
                inputs:
                    p Sched (1..1)
                output:
                    code string (1..1)
                set code:
                    p -> ccy

            func NameMatches: <"StringContains twin - a3's predicate callee; arg 0 is the meta nav">
                inputs:
                    candidate string (1..1)
                    wanted string (1..1)
                output:
                    result boolean (1..1)
                set result:
                    candidate = wanted

            reporting rule A1FilterBareFnSingle from Holder: <"a1 - THE drr 5.61.0 SpreadCurrencyLeg2 SHAPE: a bare-fn filter over a SINGLE [metadata reference] item">
                filter IsAllowable
                then extract price
                then filter IsMonetary
                then extract ccy
                as "a1"

            reporting rule A2FilterBareFnMulti from Holder: <"a2 - THE mas FixedFloatRateLeg1 SHAPE: the MULTI receiver form (filterItemNullSafe)">
                filter IsAllowable
                then extract prices
                then filter IsMonetary
                then extract ccy
                as "a2"

            func A3FilterExplicitArgs: <"a3 - THE drr 7.x ExtractPartyByNameContains SHAPE: an EXPLICIT-args predicate whose FIRST arg is a nav to a [metadata scheme] leaf">
                inputs:
                    parties Party (0..*)
                    wanted string (1..1)
                output:
                    picked Party (0..1)
                set picked:
                    parties
                        filter NameMatches(name, wanted)
                        then only-element

            reporting rule B1FilterNonMeta from Holder: <"b1 - the DECLINE pin: the SAME bare-fn filter over a NON-meta item keeps the flat one-liner">
                filter IsAllowable
                then extract plain
                then filter IsMonetary
                then extract ccy
                as "b1"

            reporting rule B2FilterMetaParam from Holder: <"b2 - the #347 DECLINE: the callee param carries the meta, so golden passes the wrapper bare">
                filter IsAllowable
                then extract notes
                then filter WantsMetaNote
                as "b2"

            reporting rule B3ExtractBareFn from Holder: <"b3 - the ADJACENT-SEAT pin: the EXTRACT-lambda hoist is green TODAY and must not move">
                filter IsAllowable
                then extract price
                then extract ToCode
                as "b3"

            reporting rule B4FilterComparison from Holder: <"b4 - the OTHER FILTER_PREDICATE arms must not move: a comparison predicate keeps its .get() form">
                filter IsAllowable
                then extract plain
                then filter amt exists
                as "b4"

            type PriceHolder:
                sched Sched (0..1)
                    [metadata reference]
                items Sched (0..*)

            func TakesPlain: <"a plain (meta-free) param callee">
                inputs:
                    s Sched (0..1)
                output:
                    result boolean (1..1)
                set result:
                    s exists

            func B5CallerInputArgFilter: <"b5 AMENDMENT - the arg is the CALLER'S bare meta-annotated input (the UpdatePriceAmountForEachMatchingQuantity class): golden derefs it INLINE (the input's own ternary form) and hoists NOTHING">
                inputs:
                    ph PriceHolder (1..1)
                    ref Sched (0..1)
                        [metadata reference]
                output:
                    result Sched (0..*)
                add result:
                    ph -> items
                        then filter TakesPlain(ref)
            """;

    // =========================================================================
    // Part A -- the positive fixtures (the law's two carrier shapes)
    // =========================================================================

    /**
     * a1 -- the SINGLE-receiver bare-fn filter over a {@code [metadata reference]} item hoists
     * the wrapper at lambda top and passes the null-guarded deref. Asserted on RAW text: the
     * decl and the guard are code, but the hoisted name is also the wrapper's lower-camel
     * lexeme, which {@code codeOnly} leaves alone.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_singleReceiverBareFnFilterHoistsAndDerefs() throws IOException {
        String out = rule("A1FilterBareFnSingleRule.java");
        assertContains(out, ".filterSingleNullSafe(item -> {");
        assertContains(out, "final ReferenceWithMetaSched referenceWithMetaSched = item.get();");
        assertContains(out, "return isMonetary.evaluate((referenceWithMetaSched == null ? null"
                + " : referenceWithMetaSched.getValue()));");
        assertTrue(!codeOnly(out).contains(".filterSingleNullSafe(item -> isMonetary.evaluate("),
                "the flat one-liner predicate must be gone:\n" + out);
    }

    /** a2 -- the MULTI-receiver twin selects {@code filterItemNullSafe} and hoists identically. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_multiReceiverBareFnFilterHoistsAndDerefs() throws IOException {
        String out = rule("A2FilterBareFnMultiRule.java");
        assertContains(out, ".filterItemNullSafe(item -> {");
        assertContains(out, "final ReferenceWithMetaSched referenceWithMetaSched = item.get();");
        assertContains(out, "return isMonetary.evaluate((referenceWithMetaSched == null ? null"
                + " : referenceWithMetaSched.getValue()));");
        assertTrue(!codeOnly(out).contains(".filterItemNullSafe(item -> isMonetary.evaluate("),
                "the flat one-liner predicate must be gone:\n" + out);
    }

    /**
     * a3 -- the EXPLICIT-args carrier: the hoisted expression is a NAV CHAIN, the deref sits at
     * ARGUMENT INDEX 0 of a two-argument call, and the second argument renders unchanged in the
     * same {@code return}. This is the shape a design keyed on {@code item.get()} alone misses.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a3_explicitArgsFilterPredicateHoistsTheNavChainAtIndexZero() throws IOException {
        String out = fn("A3FilterExplicitArgs.java");
        assertContains(out, ".filterItemNullSafe(item -> {");
        assertContains(out, "final FieldWithMetaString fieldWithMetaString = item"
                + ".<FieldWithMetaString>map(\"getName\", ");
        assertContains(out, "return nameMatches.evaluate((fieldWithMetaString == null ? null"
                + " : fieldWithMetaString.getValue()), wanted);");
        assertTrue(!codeOnly(out).contains(".filterItemNullSafe(item -> nameMatches.evaluate("),
                "the flat one-liner predicate must be gone:\n" + out);
    }

    // =========================================================================
    // Part B -- the decline pins (witness-unique on tokens the flip ADDS)
    // =========================================================================

    /**
     * b1 -- a NON-meta piped item declines inside {@code tryMetaDerefArg}
     * ({@code paramJavaType.equals(actualItemType)}), so the predicate stays a flat one-liner.
     * Witness-uniqueness: the assertions key on the two tokens the flip adds -- the block form
     * and the guarded deref -- never on the presence of {@code filterSingleNullSafe}, which is
     * there in BOTH states.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b1_nonMetaFilterItemKeepsTodaysFlatBytes() throws IOException {
        String out = rule("B1FilterNonMetaRule.java");
        String code = codeOnly(out);
        assertContains(out, ".filterSingleNullSafe(item -> isMonetary.evaluate(item.get()));");
        assertTrue(!code.contains("NullSafe(item -> {"),
                "law 6 must NOT block-convert a non-meta filter predicate:\n" + out);
        assertTrue(!code.contains("== null ? null :"),
                "law 6 must NOT emit a guarded deref for a non-meta item:\n" + out);
    }

    /**
     * b2 -- a META-annotated callee parameter EXPECTS the wrapper (the #347
     * inverseN7CalleeParamMeta law and the implicit-item seat's own
     * {@code detectMetaKind(callee.inputs().get(0)) == NONE} gate), so golden passes the
     * wrapper bare. The newly-opened route must not reach past that gate.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b2_metaAnnotatedCalleeParamKeepsTheWrapperBare() throws IOException {
        String out = rule("B2FilterMetaParamRule.java");
        String code = codeOnly(out);
        assertContains(out, "wantsMetaNote.evaluate(item.get())");
        assertTrue(!code.contains("NullSafe(item -> {"),
                "a meta-PARAM callee must keep the flat predicate:\n" + out);
        assertTrue(!code.contains("== null ? null :"),
                "a meta-PARAM callee takes the wrapper bare - no deref:\n" + out);
    }

    /**
     * b3 -- the ADJACENT seat. An EXTRACT lambda whose direct body is a bare fn call over the
     * SAME meta item is already green today through route BLOCK (the #144/#171 arm). Its bytes
     * must be identical before and after: law 6 ports a ROUTE SELECTION to the filter seat, it
     * does not touch the extract arm.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b3_extractSeatHoistIsUnchanged() throws IOException {
        String out = rule("B3ExtractBareFnRule.java");
        assertContains(out, ".mapSingleToItem(item -> {");
        assertContains(out, "final ReferenceWithMetaSched referenceWithMetaSched = item.get();");
        assertContains(out, "return MapperS.of(toCode.evaluate((referenceWithMetaSched == null"
                + " ? null : referenceWithMetaSched.getValue())));");
    }

    /**
     * b4 -- the other FILTER_PREDICATE arms. A comparison/existence predicate takes the #339
     * {@code .get()} coercion and never enters the meta-deref route at all.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b4_comparisonPredicateKeepsItsGetForm() throws IOException {
        String out = rule("B4FilterComparisonRule.java");
        String code = codeOnly(out);
        assertContains(out, ".filterSingleNullSafe(item -> exists(item.");
        assertTrue(!code.contains("NullSafe(item -> {"),
                "a comparison predicate must not block-convert:\n" + out);
    }

    /**
     * b5 — the AMENDMENT's decline pin (measured by the mid-seat whole-matrix D11: cdm
     * UpdatePriceAmountForEachMatchingQuantity ×10 cells ENTERED under the un-amended law).
     * The predicate's arg is the CALLER'S meta-annotated input, not an item-rooted chain —
     * golden derefs such an arg with the input's own inline ternary form and hoists NOTHING.
     * The LAMBDA_CHANNEL route now declines any arg whose root is not the lambda's implicit
     * item; the block form appearing here would be the over-fire recurring.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b5_callerInputMetaArgIsNotHoisted() throws IOException {
        String out = fn("B5CallerInputArgFilter.java");
        assertTrue(!out.contains("NullSafe(item -> {"),
                "a caller-input meta arg must not block-convert the predicate:\n" + out);
    }

    // =========================================================================
    // Part C -- the corpus carriers (10 whole-file rows across two cells)
    // =========================================================================

    /**
     * The Spread class: BYTE-WHOLE under law 6 alone (MEASURED at the law-6 head: zero diff
     * lines on disk, and every green sibling in the family — SpreadCurrencyLeg1/OfLeg1,
     * PackageTransactionSpreadCurrencyRule ×5 cells, CDEPackageTransactionSpreadCurrencyRule —
     * stayed at zero too).
     */
    private static final List<String> CARRIERS_A = List.of(
            "drr/regulation/asic/rewrite/trade/reports/SpreadCurrencyLeg2Rule.java",
            "drr/regulation/esma/emir/refit/trade/reports/SpreadCurrencyOfLeg2Rule.java",
            "drr/regulation/fca/ukemir/refit/trade/reports/SpreadCurrencyOfLeg2Rule.java");

    /**
     * The mas trio: law 6 healed the filter-predicate deref block, and EXACTLY ONE line remained
     * per file — the {@code MapperS.of(<mapperC>.get())} then-arg decl re-derived its element
     * with the meta wrapper DROPPED ({@code MapperS<PriceSchedule>} where golden keeps
     * {@code MapperS<FieldWithMetaPriceSchedule>}): the decl-ELEMENT read class the mega-probe
     * filed under law D (the census F13 producer-7 twin). MEASURED at the law-6 head: 2 diff
     * lines each; c1b pinned that residue exactly from seat 28 to seat 32. HEALED WHOLE at seat 33
     * by law E.1 ({@code collapseCarryPrevMetaElement}: the k=8 only-element collapse carries the
     * previous level's meta element through a seventh disjunct of the #297 anchor fold) — c1b's
     * residue arithmetic fired at the E.1 head exactly as this javadoc promised ("exactly ONE
     * fork-only line expected … but got []") and was upgraded to the byte lock below.
     */
    private static final List<String> CARRIERS_A_PARTIAL = List.of(
            "drr/regulation/techsprint/g20/mas/reports/FixedFloatRateLeg1Rule.java",
            "drr/regulation/techsprint/g20/mas/reports/FixedFloatRateLeg2Rule.java",
            "drr/regulation/techsprint/g20/mas/reports/InterestRatePriceRule.java");

    private static final String EPBNC =
            "drr/enrichment/common/test/functions/ExtractPartyByNameContains.java";

    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_drr561SpreadCarriersByteIdentical() throws IOException {
        for (String p : CARRIERS_A) {
            lockA(p);
        }
    }

    /**
     * c1b — the mas trio, BYTE-IDENTICAL to golden since seat 33 law E.1. From seat 28 to seat 32
     * this was the residue pin (LAW 80: a partial repair is accounted, never absorbed): each file
     * differed from golden ONLY on the meta-dropped decl element, and the pin promised to fire
     * "and upgrade to a byte lock" when the decl-element law landed. It fired at the E.1 head
     * (the RED print: exactly ONE fork-only line expected, got []), and this is the upgrade.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1b_masTrioByteIdenticalSinceSeat33LawE1() throws IOException {
        for (String p : CARRIERS_A_PARTIAL) {
            lockA(p);
        }
    }

    /** c2 -- the drr 7.0.0 carrier; cells 7.1/7.2/7.3 carry the same row. */
    @Test
    @EnabledIf("cellBAvailable")
    void corpus_c2_extractPartyByNameContainsByteIdentical() throws IOException {
        assertNotNull(drrBOutput, "drr 7.0.0 generation did not run");
        assertEquals(normalize(Files.readString(GOLDEN_B.resolve(EPBNC))),
                normalize(drrBOutput.get(EPBNC)),
                "generated drr 7.0.0 output must byte-match golden for " + EPBNC);
    }

    /**
     * control0 -- golden is the oracle (prove the instrument can fail): golden BLOCK-converts
     * the predicate and hoists the wrapper at BOTH carrier shapes, and carries no flat form.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control0_goldenHoistsAtEveryCarrier() throws IOException {
        String g = Files.readString(
                GOLDEN_A.resolve("drr/regulation/asic/rewrite/trade/reports/SpreadCurrencyLeg2Rule.java"));
        assertTrue(g.contains(".filterSingleNullSafe(item -> {"),
                "golden must block-convert the filter predicate");
        assertTrue(g.contains(
                "final ReferenceWithMetaPriceSchedule referenceWithMetaPriceSchedule = item.get();"),
                "golden must hoist the wrapper at lambda top");
        assertTrue(!g.contains(".filterSingleNullSafe(item -> isPriceMonetary.evaluate(item.get()))"),
                "golden must NOT carry the flat one-liner");
        String gExtract = Files.readString(
                GOLDEN_A.resolve("drr/regulation/asic/rewrite/trade/reports/SpreadCurrencyLeg2Rule.java"));
        assertTrue(gExtract.contains("final FieldWithMetaString fieldWithMetaString = item.get();"),
                "the SAME file's EXTRACT-seat hoist is the machinery-exists witness");
    }

    /**
     * control1 -- LAW 79, the whole-cell UNION scan on drr 7.0.0: per file the
     * (meta-hoisting filter block, flat filter predicate, guarded-deref census) triple must
     * equal golden's, file for file over the UNION, beyond the NAMED residue.
     */
    @Test
    @EnabledIf("cellBAvailable")
    void corpus_control1_forkDrr7WholeCellFilterPredicatesEqualGoldenFileByFile() throws IOException {
        assertNotNull(drrBOutput, "drr 7.0.0 generation did not run");
        assertEquals(List.of(), drrBGenErrors,
                "drr 7.0.0 reported a generation error - the scan is incomplete");
        assertUnionEqual(scan(drrBOutput), scan(readGoldenTree(GOLDEN_B)), drrBOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_7, DOMAIN_DRR7);
    }

    /**
     * The NAMED residue of drr 7.0.0 (LAW 73: pin the SET, not the count). <b>MEASURED at the
     * chain head c6871ed5</b>; RE-PINNED at seat 29 (FOUR rows left the set - see below).
     * The {@code QuantityUnitOfMeasure} prediction resolved the SECOND way: it did NOT drop out of
     * the list, it is named below with its measured triple {@code fork=[0, 2, 1] golden=[1, 1, 7]}
     * -- an unhealed multi-family row (7 hunks, >=3 families, one of them a non-compiling bare
     * Rosetta name) whose filter hunk this law DOES move.
     *
     * <p><b>The seat-29 re-pin (LAW 81 — a control pinned to a residue set fires when the
     * residue heals). FOUR rows left the set, measured at the law-3 head:</b>
     * (1)+(2) {@code CountryOfCounterparty2Rule} ({@code fork=[0, 3, 7] golden=[0, 3, 6]}) and
     * {@code SmallScaleBuySideEntityIndicatorRule} ({@code fork=[0, 2, 3] golden=[0, 2, 2]})
     * carried ONE guarded deref (T3) more than golden: the filter predicate's first
     * {@code Type coercion} rendered the {@code MapperS} null-safe ternary where golden renders
     * the {@code MapperC} item form. Law 3's rung A flips that lambda param to the item binding,
     * the extra ternary goes, both triples reach golden's. (3) {@code FirstExerciseDateRule}
     * ({@code fork=[0, 0, 0] golden=[0, 0, 1]}): seat-29 law 1's block render emits the AMERICAN
     * arm's guarded deref golden carries — the fork's triple reaches golden's. (4)
     * {@code Enrich_TransactionReportInstructionTestPackDefault} ({@code fork=[0, 0, 0]}
     * {@code golden=[0, 0, 1]}): law 1's companion rung (defaultSingleMixedJoinArgDeref) fires on
     * its single mixed-join default and emits the golden-side guarded deref — a golden-ward
     * partial on a still-multi-family band file.
     *
     * <p><b>The seat-30 re-pin (LAW 81 again). ONE row left the set, measured at the seat-30 chain
     * head {@code e223ce19}</b> and transcribed VERBATIM from that run's own failing print: 10
     * entries -> 9. See the inline note below.
     *
     * <p><b>The seat-31 re-pin (LAW 81 again). ONE row left the set, measured at the seat-31 chain
     * head {@code f2a4d5c0}</b> and transcribed VERBATIM from that run's own failing print: 9
     * entries -> 8 — the UnderlierBasketIdentifier row, noted inline below.
     */
    private static final List<String> KNOWN_RESIDUE_7 = List.of(
            // the GetBasketConstituents row (fork=[0, 0, 2] golden=[0, 0, 4]) LEFT this list: law A.1
            // (ctorSetterMetaDerefFunctionHost + the rung-3 ctorSetterHoistTextOrder numbering) took its tuples to
            // golden's in all four drr 7.x cells; the file stays BANDED on law A.2's lambda-name line, which this
            // tuple set cannot see; transcribed from this control's own print (A1-trip1.log).
            // UnderlierBasketIdentifier.java (was fork=[0, 0, 0] golden=[0, 0, 2]) left this list at
            // seat 31: law 4a (choiceOptionNavLadderDeepHop - the FER SET-seat option ladder walks the NESTED choice option tree
            // through ChoiceSwitchSupport.findChoiceOptionPath and derefs the META option hop into the bare output)
            // healed it WHOLE in all four drr 7.x cells, so golden's two guarded derefs (T3) now render. Golden's T3 = 2 keeps the
            // file inside the union domain: DOMAIN_DRR7 UNMOVED at 1653.
            // the UnderlierProductIdentifier row (fork=[0, 2, 6] golden=[0, 2, 16]) LEFT this list at seat 32: law A.2 (wrapperItemReceiverBind)
            // healed it WHOLE in all four drr 7.x cells after this suite's pins were measured; transcribed from
            // the checkpoint-2 full-gensuite print (ckpt2-gensuite.log).
            // the IndicatorOfTheUnderlyingIndexRule row (fork=[0, 0, 2] golden=[0, 0, 1]) LEFT this list at seat 32:
            // law D.1's left deref made this scan's tuple equal golden's (the file stays BANDED on its D.2 sigs);
            // transcribed from the print (D1-trip1.log).
            // the Price row (fork=[0, 4, 10] golden=[0, 4, 12]) LEFT this list: law C.1 (iteChainNestedThenLadderAdmit + R2a/R2b/R4)
            // rendered the seven-rung ladder as statements and took this scan's token set to golden's in
            // all four drr 7.x cells - the file stays BANDED on C.2's default join; transcribed from this
            // control's own print (C1-trip1.log), a pure row removal (was == expected minus it).
            // ReportablePricePeriod.java (was fork=[0, 2, 2] golden=[0, 2, 3]) left this list at
            // seat 30: law 1 (aliasSigElementMetaKeep — the then-arg decl consults the alias
            // SIGNATURE channel) healed it WHOLE in all four drr 7.x cells, so the missing guarded
            // deref (T3) now renders. Golden's T2/T3 stay non-zero, so the file remains inside the
            // union domain and DOMAIN_DRR7 is UNMOVED at 1653.
            // the QuantityUnitOfMeasure row (fork=[1, 1, 5] golden=[1, 1, 7]) LEFT this list: law B.24 (defaultJoinHeteroMetaDerefBoth +
            // iteArmMetaCollapseDerefSinkChannel + three in-seat rungs) healed the file WHOLE in all four drr 7.x
            // cells - the band's last four files; transcribed from this control's own print (B24-trip1.log),
            // a pure row removal (was == expected minus it).
            // LAW 81 re-pin (seat 33, law C.2): fork=[1, 1, 4] -> fork=[1, 1, 5] - R3a's join deref moved this scan's fork side (LAW 80 IMPROVED-not-whole, planned; QUOM closes at B.24); from C2-trip1.log.
            // LAW 81 re-pin (seat 33, law B.3): fork=[0, 2, 1] -> fork=[1, 1, 4] - the bare-rule invocations + their guarded arg derefs moved this scan's fork side; the file stays BANDED (B.24); from B3-trip1.log.
            // the TotalNotionalQuantity row (fork=[0, 1, 10] golden=[0, 1, 11]) LEFT this list: law D.3 (fnDeepCondBaseConfinedArmChainAdmit,
            // seven rungs) healed the file WHOLE in all four drr 7.x cells; transcribed from this control's own
            // print (D3-trip1.log), a pure row removal (was == expected minus it).
            );
            // the UnderlyingAssetTradingPlatformIdentifierLeg1/Leg2 rows LEFT this list at seat 32: law A.1
            // (choiceOptionProjectionTypeId) healed both WHOLE in all four drr 7.x cells; transcribed from this
            // control's own print (A1-trip1.log).

    /** control2 -- LAW 77 route parity for the drr 5.61.0 carriers (a ReferenceHandler seat). */
    @Test
    @EnabledIf("cellAAndIrProviderAvailable")
    void corpus_control2_irRouteMatchesLegacyForCarriers() throws IOException {
        assertNotNull(drrAOutput, "drr 5.61.0 legacy-route generation did not run");
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "5.61.0", CELL_A_ROOT), new ArrayList<>());
        for (String p : CARRIERS_A) {
            assertEquals(drrAOutput.get(p), irOut.get(p), "route divergence: " + p);
        }
        for (String p : CARRIERS_A_PARTIAL) {
            assertEquals(drrAOutput.get(p), irOut.get(p), "route divergence: " + p);
        }
    }

    /** control3 -- LAW 79: the same scan on drr 5.61.0, the mechanism's OTHER carrier cell. */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control3_forkDrr561WholeCellFilterPredicatesEqualGoldenFileByFile() throws IOException {
        assertNotNull(drrAOutput, "drr 5.61.0 generation did not run");
        assertEquals(List.of(), drrAGenErrors,
                "drr 5.61.0 reported a generation error - the scan is incomplete");
        assertUnionEqual(scan(drrAOutput), scan(readGoldenTree(GOLDEN_A)), drrAOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_561, DOMAIN_DRR561);
    }

    /**
     * The NAMED residue of drr 5.61.0, MEASURED at the law-6 head. The pre-law scan carried 13
     * residue files; the law removed the six law-6 carriers (three Spread byte-whole + the mas
     * trio whose remaining delta carries no scan token — c1b pins it). Every entry below is an
     * OTHER family's delta (F12 block lambdas, F13 siblings, F19, the EffectiveDateRule class).
     */
    // LAW 81 re-pin (v3.1 flip seat 31, law 1): the asic CustomBasketCodeIdentifier +
    // PlatformIdentifier rows LEFT this list - both files healed WHOLE (byte-identical
    // to golden, locked by IteElseArmRecoveredMetaDerefSeatTest corpus_c1/c2). The
    // heal-tripwire fired exactly as prescribed and the list is re-pinned from the
    // control's own measured print (delta = the two rows REMOVED, nothing else).
    // LAW 81 re-pin (v3.1 flip seat 33, law F.A): the cftc NotionalCurrencyLeg1Rule + jfsa
    // NotionalCurrencyOfLeg1Rule rows LEFT this list - both files healed WHOLE by facet
    // blockArmWrapperHopDeref (byte-identical to golden, locked by BlockArmWrapperHopDerefSeatTest
    // corpus_c1/c2); the list is EMPTY, transcribed from this control's own print (FA-trip1.log).
    private static final List<String> KNOWN_RESIDUE_561 = List.of();
    // LAW 81 re-pin (v3.1 flip seat 31, law 1b): the mas PlatformIdentifierRule row LEFT
    // this list - the file healed WHOLE (byte-identical to golden, locked by
    // MapperFormRuleRootArmSeatTest corpus_c1). Re-pinned from the control's own measured
    // print (delta = the one row REMOVED, nothing else).

    /** MEASURED at the law-6 head (set below after the residue pass). */
    private static final int DOMAIN_DRR7 = 1653;

    /** MEASURED at the law-6 head (set below after the residue pass). */
    private static final int DOMAIN_DRR561 = 1174;

    /**
     * (T1, T2, T3) per file:
     * <ul>
     *   <li><b>T1</b> -- the law's ADDED shape: a filter-predicate BLOCK whose FIRST inner line
     *       declares a meta wrapper ({@code final <X>WithMeta<Y> <name> = ...;}). Two-line state
     *       machine over the code-only text.</li>
     *   <li><b>T2</b> -- the law's REMOVED shape: a filter-predicate lambda rendered FLAT.</li>
     *   <li><b>T3</b> -- the OVER-FIRE NET: the file's TOTAL guarded-deref count
     *       ({@code == null ? null :}). Deliberately global, not filter-scoped, so a spurious
     *       deref anywhere in the cell fails the control.</li>
     * </ul>
     * Each carrier moves T1 +1, T2 -1, T3 +1; every other file must match golden exactly.
     * <p>The three filter member names are the complete set {@code CollectionHandler.filterMethod}
     * can emit; re-verify with a corpus grep before pinning the domain (the LAW-79 domain law).
     */
    private static Map<String, int[]> scan(Map<String, String> tree) {
        Map<String, int[]> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : tree.entrySet()) {
            String code = codeOnly(e.getValue());
            String[] lines = code.split("\n");
            int t1 = 0;
            int t2 = 0;
            int t3 = 0;
            for (int i = 0; i < lines.length; i++) {
                String s = lines[i].trim();
                boolean isFilterLambda = s.contains("filterSingleNullSafe(")
                        || s.contains("filterItemNullSafe(")
                        || s.contains("filterListNullSafe(");
                if (isFilterLambda && s.endsWith("-> {")) {
                    String next = i + 1 < lines.length ? lines[i + 1].trim() : "";
                    if (next.startsWith("final ") && next.contains("WithMeta")
                            && next.contains(" = ")) {
                        t1++;
                    }
                } else if (isFilterLambda) {
                    t2++;
                }
                int from = 0;
                while ((from = s.indexOf("== null ? null :", from)) >= 0) {
                    t3++;
                    from += "== null ? null :".length();
                }
            }
            if (t1 + t2 + t3 > 0) {
                out.put(e.getKey(), new int[] {t1, t2, t3});
            }
        }
        return out;
    }

    // =========================================================================
    // The union assert (LAW 73: pin the SET, not the count)
    // =========================================================================

    /**
     * The scan universe is the token-bearing union INTERSECTED with the files this harness
     * actually emits (rule/report/function kinds): T3 is a GLOBAL guarded-deref count and
     * golden's POJO/metafields/report-shell files — kinds these generators never produce —
     * legitimately carry it. Whether the fork emits every expected FILE is the D11 ring's
     * question (missingOutput), not this suite's; scoping to the emitted set keeps the control
     * honest about what it can see (the pre-fix draft asserted the un-emitted list empty and
     * tripped on ~230 out-of-kind golden files).
     */
    private static void assertUnionEqual(Map<String, int[]> a, Map<String, int[]> b,
            java.util.Set<String> emittedA, String aName, String bName, List<String> knownResidue,
            int expectedDomain) {
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
    // Harness (the seat-27 AddDistributionSeatTest shape verbatim)
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
                    new D11CorpusRegressionTest.CellSpec("drr", "5.61.0", CELL_A_ROOT), errs);
            drrAGenErrors = errs;
        }
        if (cellBAvailable()) {
            List<String> errs = new ArrayList<>();
            drrBOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", CELL_B_ROOT), errs);
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
        assertNotNull(drrAOutput, "drr 5.61.0 generation did not run - corpus unavailable?");
        List<String> lockedErrors = drrAGenErrors.stream().filter(e -> e.contains(path)).toList();
        assertTrue(lockedErrors.isEmpty(),
                "the generator reported errors for the locked file " + path + ": " + lockedErrors);
        String generated = drrAOutput.get(path);
        assertNotNull(generated, "not generated in drr 5.61.0: " + path);
        Path goldenPath = GOLDEN_A.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "golden missing: " + goldenPath);
        assertEquals(normalize(Files.readString(goldenPath)), normalize(generated),
                "generated drr 5.61.0 output must byte-match golden (newline-normalized) for "
                + path + " - seat 28 law 6: a meta-wrapper arg in a FILTER predicate hoists and derefs.");
    }

    private static void assertContains(String out, String needle) {
        assertTrue(out.contains(needle), "expected <" + needle + "> in:\n" + out);
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
            RModel main = AstBuilder.buildFromString(MODEL, "seat28f13a.rosetta");
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
            fixtureOut = render(m -> "census.seat28f13a".equals(m.namespace()));
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
            throw new AssertionError("[FilterPredicateMetaDerefSeatTest] builtins parse"
                    + " failures: " + String.join("; ", failures));
        }
        return models;
    }
}
