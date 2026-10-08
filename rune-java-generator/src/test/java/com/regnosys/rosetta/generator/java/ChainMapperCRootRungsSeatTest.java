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
 * SEAT 28, law 8 — facet {@code chainMapperCRootRungs}: <b>three missing rungs in
 * {@code NavigationHandler.chainRendersMapperC} (pristine {@code :4581-4777})</b>, each of which
 * mis-reads a genuinely-{@code MapperC} receiver chain as {@code MapperS} and so emits a spurious
 * null-guard on the following {@code "Type coercion"} lambda — and, because the guarded form
 * REGISTERS the plain lexeme, forces every same-named sibling coercion in the method to escape or
 * number.
 *
 * <p><b>The channel, end to end.</b> {@code handle(RFeatureCall)} {@code :628-645} ORs
 * {@code chainRendersMapperC} into {@code chainMapperC}; that flag picks the wrapper in
 * {@code metaNavResultType}; and {@code WrappedItemCoercer.coerce} branches on the wrapper:
 * <pre>
 *   MapperS arm : registerDeferredCoercionParam(name)  ->  "Type coercion", p -&gt; p == null ? null : p.getValue()
 *   MapperC arm : registerDeferredLambdaParam(name)    ->  "Type coercion", p -&gt; p.getValue()
 * </pre>
 * so ONE verdict flip changes BOTH the guard at that site AND the whole method's coercion-name
 * arithmetic. That is why each carrier heals as a unit.
 *
 * <p><b>The three rungs (disjoint arms, disjoint carriers, disjoint blast radii).</b>
 * <ol>
 *   <li><b>R1 — {@code RDeepFeatureCall}</b>: a {@code -&gt;&gt;} step is a shape the walk had no
 *       arm for at all, so a chain crossing a deep hop fell to the conservative-single tail.
 *       Carrier drr 7.x {@code GetUnderlierIDForIndex} (3 hunks: 1 guard + 2 name-escape
 *       consequences). MEASURED population: <b>4 lines corpus-wide, all 4 band, ZERO green</b>
 *       ({@code PROBE28-F14a rootKind=RDeepFeatureCall}).</li>
 *   <li><b>R2 — a FUNCTION/RULE-call-rooted DISGUISE with a MULTI leaf</b>: every existing
 *       disguised sub-arm resolves an ATTRIBUTE head, so
 *       {@code EconomicTermsForProduct -&gt; payout} (leaf {@code 0..*}) read SINGLE. Carrier drr
 *       7.x {@code UnderlyingIdentificationTypeRule} (1 hunk, three coercion sites). Keyed on the
 *       LEAF's cardinality, NOT on "callable root": the SAME file's
 *       {@code underlier.UnderlierForProduct -&gt; Observable} head is callable-rooted with a
 *       SINGLE leaf and golden GUARDS it.</li>
 *   <li><b>R3 — a LIST-consuming lambda's implicit item</b>: {@code filterListNullSafe} /
 *       {@code mapListToItem} / {@code mapListToList} bind the closure param as the inner
 *       {@code MapperC&lt;T&gt;}, so hops off it ride MapperC.
 *       {@code CollectionHandler.lambdaParamBindsListItem} is the SAME method-selection SOT the
 *       render halves consult (LAW 69) — it just needs the scope threaded through the recursion.
 *       Carrier drr 7.x {@code Extract_TradingCapacity} (1 hunk, two coercion sites).</li>
 * </ol>
 *
 * <p><b>The settling bytes</b> (drr 7.0.0; the 7.1/7.2/7.3 rows carry the same source):
 * <pre>
 * GetUnderlierIDForIndex hunk1  golden … .&lt;AssetIdentifier&gt;mapC("chooseIdentifier", …).&lt;FieldWithMetaString&gt;map("getIdentifier", …)
 *                                        .&lt;String&gt;map("Type coercion", fieldWithMetaString -&gt; fieldWithMetaString.getValue())
 *                               fork  … .&lt;String&gt;map("Type coercion", fieldWithMetaString -&gt; fieldWithMetaString == null ? null : …)
 * GetUnderlierIDForIndex hunk2/3 golden  "Type coercion",  fieldWithMetaString -&gt; …      (already BARE — NAME only)
 *                                fork   "Type coercion", _fieldWithMetaString -&gt; …      (escaped because hunk1 CLAIMED the lexeme)
 * UnderlyingIdentificationType  site1  golden GUARDED plain / fork GUARDED numbered0    (SINGLE leaf — must NOT move)
 *                               site2/3 golden BARE _referenceWithMetaObservable / fork GUARDED numbered1,2
 * Extract_TradingCapacity       site1  golden BARE _referenceWithMetaParty / fork GUARDED referenceWithMetaParty0
 *                               site2  golden GUARDED referenceWithMetaParty / fork GUARDED referenceWithMetaParty1
 * </pre>
 *
 * <p><b>LAW 74</b>: both forms compile — this law is expected to be javac PRE 0 / POST 0. Record
 * the zero; do not skip the probe.
 *
 * <p><b>NOT a carrier — stated loudly (LAW 80).</b> {@code NameOfTheUnderlyingIndexRule} ×4 carries
 * the F14(a) delta AND a {@code MapperC.of(MapperS.of(…))} re-wrap defect in its ONE hunk, so law 8
 * alone heals it <b>0</b>. R2 <i>will</i> fire there (its coercions hang off the same
 * {@code EconomicTermsForProduct -&gt; payout} disguise): the expected LAW-80 line for this seat is
 * <b>12 healed / 0 entered / 4 improved / 0 worsened</b>, the 4 improved being that file.
 * {@code UnderlyingIndexIndicatorRule} ×4 (5 hunks, 2 F14(a)) and
 * {@code CountryOfCounterparty2Rule} / {@code SmallScaleBuySideEntityIndicatorRule} ×4 (residual
 * hunk is the F11 cardinality family) are likewise NOT claimed.
 *
 * <p><b>MEASURED at the law-8 head</b> (the RED/GREEN loop, both routes): the RED set was
 * a1/a1n/a2/a3 + corpus_c1 + the two loud domain placeholders; three fixture expectations were
 * corrected against the ACTUAL renders during that loop — (i) the model's {@code single}
 * attribute renamed ({@code single} is a rune lexer keyword), (ii) b1's pin re-shaped to the
 * STATEMENT-form deref this whole-output function shape really renders (numbered
 * {@code fieldWithMetaString0/1} + if/else guards — not in-chain coercion lambdas), (iii) a3's
 * expected lexeme corrected to the ESCAPED {@code _referenceWithMetaPty} the registered sibling
 * forces — golden's own shape at the corpus carrier, byte-locked by corpus_c1. The union
 * domains are MEASURED (drr 7.0.0 = 331, drr 5.61.0 = 413 emitted token-bearing files) and the
 * control residues are NAMED per file — every entry an OTHER family's delta. The mutation
 * failing-sets in {@code law8-12-notes.md} §5 remain CLAIMS except the ONE the chain measured
 * (see MEASURED MUTATIONS below); the rest are NOT RUN — banked to S29 (LAW 82).
 * <p><b>MEASURED MUTATIONS (LAW 82 - the seat-28 mut28 suite-lane loop; each
 * mutation = the named apply-script reverted, the suite run, the script re-applied;
 * every set below is the RECORDED failing set from that run, never a claim):</b>
 * <ul>
 *   <li>the whole law reverted (law8-apply --revert: all three rungs) -> a1, a1n, a2, a3, corpus_c1, corpus_control1 (6F)</li>
 * </ul>
 */
class ChainMapperCRootRungsSeatTest {

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

    /** drr 7.0.0 — the cell that carries ALL TWELVE rows. */
    private static final Path CELL_B_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path GOLDEN_B = CELL_B_ROOT.resolve("rosetta-source/src/generated/java");

    /** drr 5.61.0 — a cell with NO law-8 row: the over-fire tripwire for R2 and R3. */
    private static final Path CELL_A_ROOT = Path.of("../test-corpus/drr/drr-5.61.0");
    private static final Path GOLDEN_A = CELL_A_ROOT.resolve("rosetta-source/src/generated/java");

    static boolean cellAAvailable() {
        return Files.isDirectory(GOLDEN_A);
    }

    static boolean cellBAvailable() {
        return Files.isDirectory(GOLDEN_B);
    }

    static boolean cellBAndIrProviderAvailable() {
        return cellBAvailable() && irProviderOnClasspath();
    }

    // =========================================================================
    // The fixture — one shape per rung, plus the three decline pins
    // =========================================================================

    /**
     * {@code note} everywhere a "tag" would read naturally: {@code tag} is a rune lexer keyword.
     *
     * <p>a1/a1n mirror {@code GetUnderlierIDForIndex}'s source
     * ({@code … -&gt; Index -&gt;&gt; identifier -&gt; identifier}); a2/b2 mirror
     * {@code UnderlyingIdentificationTypeRule}'s two disguised heads
     * ({@code EconomicTermsForProduct -&gt; payout} MULTI leaf vs
     * {@code underlier.UnderlierForProduct -&gt; Observable} SINGLE leaf); a3/b3 mirror
     * {@code Extract_TradingCapacity}'s {@code then extract} → {@code then filter} pipe, with b3
     * one {@code flatten} away (list filter vs item filter).
     */
    private static final String MODEL = """
            namespace census.seat28law8
            version "1.0.0"

            type Leg:
                code string (0..1)
                    [metadata scheme]
                amt number (0..1)

            type Sub:
                legs Leg (0..*)

            type Idx:
                subA Sub (0..1)
                subB Sub (0..1)

            type Carrier:
                legs Leg (0..*)

            type Holder:
                idx Idx (0..1)
                direct Carrier (0..1)
                one Carrier (0..1)
                two Carrier (0..1)

            type Obs:
                note string (0..1)

            type Under:
                obs Obs (0..1)
                    [metadata reference]

            type Payout:
                under Under (0..1)

            type Econ:
                payouts Payout (0..*)

            type Evt:
                econ Econ (0..1)
                soleUnder Under (0..1)

            type Pty:
                note string (0..1)

            type Member:
                pty Pty (0..1)
                    [metadata reference]

            type Grp:
                members Member (0..*)

            type Side:
                pty Pty (0..1)
                    [metadata reference]

            type Root:
                grps Grp (0..*)
                side Side (0..1)

            func A1DeepHopCoercion: <"R1 - a coercion whose receiver chain CROSSES a ->> mapC hop">
                inputs:
                    h Holder (1..1)
                output:
                    out string (0..*)
                add out:
                    h -> idx ->> legs -> code

            func A1nDeepHopNameRelease: <"R1 - the NAME half: a deep-rooted site claims the lexeme its mapC-rooted sibling wants">
                inputs:
                    h Holder (1..1)
                output:
                    out string (0..*)
                add out:
                    if h -> idx exists
                    then h -> idx ->> legs -> code
                    else if h -> direct exists
                    then h -> direct -> legs -> code
                    else empty

            func EconForEvent: <"the callable head a2's disguise is rooted on - SINGLE output, MULTI leaf">
                inputs:
                    e Evt (1..1)
                output:
                    econ Econ (0..1)
                set econ:
                    e -> econ

            func SingleUnderForEvent: <"the callable head b2's disguise is rooted on - SINGLE output, SINGLE leaf">
                inputs:
                    e Evt (1..1)
                output:
                    u Under (0..1)
                set u:
                    e -> soleUnder

            reporting rule A2CallableRootMultiLeaf from Evt: <"R2 - a FUNCTION-rooted disguise whose LEAF is 0..*">
                extract EconForEvent -> payouts -> under -> obs -> note

            reporting rule B2CallableRootSingleLeaf from Evt: <"b2 - the SAME root shape with a SINGLE leaf: must NOT move">
                extract SingleUnderForEvent -> obs -> note

            func A3ListFilterItemCoercion: <"R3 - a coercion off the implicit item of a LIST filter (filterListNullSafe)">
                inputs:
                    r Root (1..1)
                output:
                    result Member (0..*)
                set result:
                    r -> grps
                        then extract members
                        then filter pty any = r -> side -> pty
                        then flatten

            func B3ItemFilterCoercionUnmoved: <"b3 - the same predicate one FLATTEN earlier: filterItemNullSafe, must NOT move">
                inputs:
                    r Root (1..1)
                output:
                    result Member (0..*)
                set result:
                    r -> grps
                        then extract members
                        then flatten
                        then filter pty any = r -> side -> pty

            func B1SingleChainCoercionsUnmoved: <"b1 - two SINGLE-chain coercions: the numbering the scope-escape law owns must NOT move">
                inputs:
                    h Holder (1..1)
                output:
                    out string (0..1)
                set out:
                    if h -> one -> legs -> code exists
                    then h -> one -> legs first -> code
                    else h -> two -> legs first -> code
            """;

    // =========================================================================
    // Part A — the fixtures (RED before the flip)
    // =========================================================================

    /**
     * a1 — R1. The coercion sits after {@code .&lt;Leg&gt;mapC("chooseLegs", idx -&gt;
     * idxDeepPathUtil.chooseLegs(idx)).&lt;FieldWithMetaString&gt;map("getCode", …)}: the deep hop
     * renders {@code mapC} (leaf {@code legs} is {@code 0..*}), so the chain rides MapperC and the
     * coercion must be BARE.
     *
     * <p>DRAFT: confirm the exact rendered prefix at the RED head before shipping.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_deepHopReceiverRendersBareCoercion() throws IOException {
        String out = fn("A1DeepHopCoercion.java");
        assertContains(out, ".<Leg>mapC(\"chooseLegs\", idx -> idxDeepPathUtil.chooseLegs(idx))");
        assertContains(out, ".<String>map(\"Type coercion\","
                + " fieldWithMetaString -> fieldWithMetaString.getValue())");
        assertTrue(!codeOnly(out).contains("fieldWithMetaString == null ? null :"),
                "a coercion on a deep-hop (mapC) chain must be the BARE MapperC form:\n" + out);
    }

    /**
     * a1n — R1's NAME half. TWO coercions of the same item type: the first rides the {@code ->>}
     * hop, the second a plain {@code mapC}. Today the first is guarded and REGISTERS
     * {@code fieldWithMetaString}, forcing the second to escape to {@code _fieldWithMetaString} —
     * exactly {@code GetUnderlierIDForIndex}'s hunks 2/3. After the flip both are bare and both
     * carry the plain lexeme.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1n_deepHopSiteReleasesTheLexemeForItsSibling() throws IOException {
        String out = fn("A1nDeepHopNameRelease.java");
        String code = codeOnly(out);
        assertEquals(2, count(out, ".<String>map(\"Type coercion\","
                + " fieldWithMetaString -> fieldWithMetaString.getValue())"),
                "both coercion sites must be BARE and carry the PLAIN lexeme:\n" + out);
        assertTrue(!code.contains("_fieldWithMetaString"),
                "no site may escape once the deep-rooted site stops registering the lexeme:\n" + out);
        assertTrue(!code.contains("fieldWithMetaString0") && !code.contains("fieldWithMetaString1"),
                "no site may number once both are on the MapperC (non-registering) arm:\n" + out);
    }

    /**
     * a2 — R2. {@code EconForEvent -> payouts} is a disguised 2-name head whose
     * {@code resolvedSymbol} is the {@code RFunction} and whose LEAF ({@code payouts}) is
     * {@code 0..*}: the render synthesises {@code .<Payout>mapC("getPayouts", …)}, so the chain
     * rides MapperC and the {@code obs} coercion must be BARE.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_callableRootedDisguiseWithMultiLeafRendersBareCoercion() throws IOException {
        String out = rule("A2CallableRootMultiLeafRule.java");
        assertContains(out, ".<Payout>mapC(\"getPayouts\",");
        assertContains(out, ".<Obs>map(\"Type coercion\","
                + " referenceWithMetaObs -> referenceWithMetaObs.getValue())");
        assertTrue(!codeOnly(out).contains("referenceWithMetaObs == null ? null :"),
                "a MULTI-leaf callable-rooted disguise rides MapperC — the coercion bares:\n" + out);
    }

    /**
     * a3 — R3. {@code then extract members} over a MULTI item yields a
     * {@code MapperListOfLists<Member>}, so the following {@code then filter} renders
     * {@code filterListNullSafe} and binds its param as {@code MapperC<Member>}: the
     * {@code pty} coercion INSIDE the predicate must be BARE, while the SINGLE
     * {@code r -> side -> pty} comparand in the SAME predicate must stay GUARDED.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a3_listFilterPredicateItemRendersBareCoercion() throws IOException {
        String out = fn("A3ListFilterItemCoercion.java");
        assertContains(out, ".filterListNullSafe(item -> ");
        // MEASURED at the law-8 head: the bare form escapes to the underscored lexeme
        // (`_referenceWithMetaPty`) because the guarded REGISTERED sibling in the same
        // areEqual claims the plain name - exactly the corpus carrier's golden shape
        // (Extract_TradingCapacity: `_referenceWithMetaParty`), byte-locked by corpus_c1.
        assertContains(out, "item.<ReferenceWithMetaPty>map(\"getPty\", member -> member.getPty())"
                + ".<Pty>map(\"Type coercion\", _referenceWithMetaPty -> _referenceWithMetaPty.getValue())");
        assertContains(out, ".<Pty>map(\"Type coercion\","
                + " referenceWithMetaPty -> referenceWithMetaPty == null ? null"
                + " : referenceWithMetaPty.getValue())");
        assertEquals(1, count(out, "== null ? null :"),
                "exactly the SINGLE comparand keeps its guard:\n" + out);
    }

    // =========================================================================
    // Part B — the decline pins (GREEN in both states; LAW 76 witness-uniqueness)
    // =========================================================================

    /**
     * b1 — the scope-escape sibling class named in the seat brief. Two SINGLE-chain coercions of
     * the same item type: both take the MapperS arm, both REGISTER, so they number {@code 0}/{@code 1}.
     * The law changes no naming code, so this file must be byte-stable — and it must carry BOTH the
     * guard token and the numbering token, or it is not a witness for the class it pins.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b1_singleChainCoercionNumberingUnmoved() throws IOException {
        String out = fn("B1SingleChainCoercionsUnmoved.java");
        // MEASURED at the RED head: this whole-output function shape renders the
        // STATEMENT-form deref (hoisted `final FieldWithMetaString fieldWithMetaStringN =
        // ...get();` + an if/else null guard), not the in-chain coercion lambda the draft
        // assumed. The pin keeps its intent - both SINGLE chains stay GUARDED and their
        // locals stay NUMBERED 0/1 - against the render that actually exists.
        assertEquals(2, count(out, "== null) {"),
                "both SINGLE-chain derefs keep their statement-form guard:\n" + out);
        assertContains(out, "final FieldWithMetaString fieldWithMetaString0 =");
        assertContains(out, "final FieldWithMetaString fieldWithMetaString1 =");
        assertTrue(!codeOnly(out).contains("-> fieldWithMetaString.getValue()"),
                "no bare unguarded coercion lambda may appear:\n" + out);
    }

    /**
     * b2 — R2's discriminator, and the reason the rung is keyed on the LEAF rather than on the
     * root. {@code SingleUnderForEvent -> obs} is the SAME callable-rooted disguise shape as a2's
     * with a SINGLE leaf; golden guards its sibling in the corpus carrier
     * ({@code underlier.UnderlierForProduct -> Observable}) and this file must not move.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b2_callableRootedDisguiseWithSingleLeafKeepsItsGuard() throws IOException {
        String out = rule("B2CallableRootSingleLeafRule.java");
        assertContains(out, ".<Obs>map(\"Type coercion\", referenceWithMetaObs"
                + " -> referenceWithMetaObs == null ? null : referenceWithMetaObs.getValue())");
        assertTrue(!codeOnly(out).contains("mapC("),
                "a SINGLE-leaf callable-rooted disguise renders no mapC step at all:\n" + out);
    }

    /**
     * b3 — R3's discriminator: the SAME predicate one {@code flatten} earlier. The receiver is a
     * {@code MapperC}, so the filter renders {@code filterItemNullSafe} and binds its param as a
     * {@code MapperS} — {@code lambdaParamBindsListItem} answers FALSE and the coercion keeps its
     * guard. If this file moves, R3 is keyed on "inside a lambda" instead of on the binding.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b3_itemFilterPredicateItemKeepsItsGuard() throws IOException {
        String out = fn("B3ItemFilterCoercionUnmoved.java");
        assertContains(out, ".filterItemNullSafe(item -> ");
        assertEquals(2, count(out, "== null ? null :"),
                "an item-filter predicate binds a MapperS — BOTH coercions keep their guard:\n" + out);
        assertTrue(!codeOnly(out).contains("filterListNullSafe"),
                "the flattened receiver must NOT select the list-level member:\n" + out);
    }

    // =========================================================================
    // Part C — the corpus carriers (12 whole-file rows; drr 7.0.0 is the locked cell)
    // =========================================================================

    private static final String CARRIER_TRADING_CAPACITY =
            "drr/regulation/common/trade/execution/functions/Extract_TradingCapacity.java";
    private static final String CARRIER_UNDERLIER_ID =
            "drr/regulation/common/trade/underlier/functions/GetUnderlierIDForIndex.java";
    private static final String CARRIER_UID_TYPE_RULE =
            "drr/regulation/asic/rewrite/trade/reports/UnderlyingIdentificationTypeRule.java";

    private static final List<String> CARRIERS_B = List.of(
            CARRIER_TRADING_CAPACITY, CARRIER_UNDERLIER_ID, CARRIER_UID_TYPE_RULE);

    /** c1 — the three drr 7.0.0 carriers byte-match golden; drr 7.1/7.2/7.3 carry the same rows. */
    @Test
    @EnabledIf("cellBAvailable")
    void corpus_c1_drr7CarriersByteIdentical() throws IOException {
        for (String p : CARRIERS_B) {
            lockB(p);
        }
    }

    /**
     * control0 — golden is the oracle, so the instrument can fail. MEASURED on the drr 7.0.0
     * golden tree: {@code GetUnderlierIDForIndex} carries FIVE {@code "Type coercion"} sites — two
     * {@code referenceWithMetaObservable{0,1}} (guarded, genuinely single, unrelated to this law)
     * and <b>THREE {@code fieldWithMetaString} sites that are all BARE and all carry the PLAIN
     * lexeme</b> (zero escapes). The fork today has one of those three guarded and the other two
     * escaped — the exact delta law 8's R1 closes. {@code Extract_TradingCapacity} golden carries
     * exactly one escaped-and-bare and one plain-and-guarded coercion.
     */
    @Test
    @EnabledIf("cellBAvailable")
    void corpus_control0_goldenCarriesTheTargetShapes() throws IOException {
        String uid = Files.readString(GOLDEN_B.resolve(CARRIER_UNDERLIER_ID));
        assertEquals(5, count(uid, "\"Type coercion\""), "golden coercion-site count");
        assertEquals(3, count(uid, "\"Type coercion\", fieldWithMetaString ->"),
                "golden carries THREE bare plain-lexeme fieldWithMetaString coercions");
        assertTrue(!uid.contains("_fieldWithMetaString"),
                "golden never escapes fieldWithMetaString in this file");
        String etc = Files.readString(GOLDEN_B.resolve(CARRIER_TRADING_CAPACITY));
        assertTrue(etc.contains("\"Type coercion\", _referenceWithMetaParty"
                + " -> _referenceWithMetaParty.getValue()"),
                "golden's list-filter coercion is BARE and ESCAPED");
        assertTrue(etc.contains("\"Type coercion\", referenceWithMetaParty"
                + " -> referenceWithMetaParty == null ? null"),
                "golden's SINGLE comparand coercion stays GUARDED with the plain lexeme");
    }

    /**
     * control1 — LAW 79, the whole-cell UNION scan on drr 7.0.0: per file the
     * (guarded, bare, escaped, numbered) coercion quadruple must equal golden's, file for file over
     * the UNION, beyond the NAMED residue.
     */
    @Test
    @EnabledIf("cellBAvailable")
    void corpus_control1_forkDrr7WholeCellCoercionSitesEqualGoldenFileByFile() throws IOException {
        assertNotNull(drrBOutput, "drr 7.0.0 generation did not run");
        assertEquals(List.of(), drrBGenErrors,
                "drr 7.0.0 reported a generation error — the scan is incomplete");
        assertUnionEqual(scan(drrBOutput), scan(readGoldenTree(GOLDEN_B)), drrBOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_7, DOMAIN_DRR7);
    }

    /**
     * control3 — LAW 79 on drr 5.61.0, a cell with NO law-8 row. R2's and R3's flip sets are
     * UNMEASURED at this head (the probe carries only a ceiling), so this scan is the over-fire
     * tripwire: any movement here is a finding, not a heal.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control3_forkDrr561WholeCellCoercionSitesEqualGoldenFileByFile() throws IOException {
        assertNotNull(drrAOutput, "drr 5.61.0 generation did not run");
        assertEquals(List.of(), drrAGenErrors,
                "drr 5.61.0 reported a generation error — the scan is incomplete");
        assertUnionEqual(scan(drrAOutput), scan(readGoldenTree(GOLDEN_A)), drrAOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_561, DOMAIN_DRR561);
    }

    /** control2 — LAW 77 route parity for the three carriers. */
    @Test
    @EnabledIf("cellBAndIrProviderAvailable")
    void corpus_control2_irRouteMatchesLegacyForCarriers() throws IOException {
        assertNotNull(drrBOutput, "drr 7.0.0 legacy-route generation did not run");
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", CELL_B_ROOT), new ArrayList<>());
        for (String p : CARRIERS_B) {
            assertEquals(drrBOutput.get(p), irOut.get(p), "route divergence: " + p);
        }
    }

    /**
     * The NAMED residue of drr 7.0.0 (LAW 73: pin the SET, not the count). <b>RE-MEASURED at the
     * seat-30 chain head {@code e223ce19}</b> — the 10 entries below are transcribed VERBATIM from
     * that run's own failing print (LAW 81). The prior set was the seat-29 head's 11 entries.
     * <b>RE-MEASURED at the seat-31 chain head {@code f2a4d5c0}</b> — 8 entries (the print:
     * "in 8 file(s)"), two rows having LEFT this seat, both noted inline: GetUnderlierLEIForCredit
     * via law 2 rung 2 (in the law-2 commit) and UnderlierBasketIdentifier via law 4a (at the
     * chain); transcribed VERBATIM from that run's own failing print.
     */
    private static final List<String> KNOWN_RESIDUE_7 = List.of(
            // GetBasket.java (was fork=[0, 0, 0, 0] golden=[0, 1, 0, 0]) left this list at seat 30:
            // laws 6 + 7 TOGETHER (condArmMultiMetaElementDeref hunk 1 + multiEmptyElseArmToBuilder
            // hunk 2) healed the file WHOLE in all four drr 7.x cells, so its coercion tuple now
            // equals golden's. It stays inside the union domain (golden T2 = 1), so DOMAIN_DRR7 is
            // UNMOVED at 331.
            // UnderlierBasketIdentifier.java (was fork=[0, 0, 0, 0] golden=[2, 0, 0, 0]) left this list
            // at seat 31: law 4a (choiceOptionNavLadderDeepHop - the FER SET-seat option ladder walks the NESTED choice option tree
            // through ChoiceSwitchSupport.findChoiceOptionPath and derefs the META option hop into the bare output)
            // healed it WHOLE in all four drr 7.x cells, so golden's two guarded coercion sites (T1) now render. Golden's T1 = 2
            // keeps the file inside the union domain: DOMAIN_DRR7 UNMOVED at 331.
            // the UnderlierProductIdentifier row (fork=[6, 4, 0, 6] golden=[16, 4, 0, 16]) LEFT this list at seat 32: law A.2 (wrapperItemReceiverBind)
            // healed it WHOLE in all four drr 7.x cells after this suite's pins were measured; transcribed from
            // the checkpoint-2 full-gensuite print (ckpt2-gensuite.log).
            // CountryOfCounterparty2Rule and SmallScaleBuySideEntityIndicatorRule left this list at
            // seat 29 (law 3, rulePathFilterLolBinding) — healed WHOLE, measured in the seat's chain.
            // the IndicatorOfTheUnderlyingIndexRule row (fork=[1, 2, 0, 0] golden=[1, 3, 0, 0]) LEFT this list at seat 32: law D.2
            // (extractBodyMultiDefaultTernary, on law D.1's left deref) healed it WHOLE in all four drr 7.x cells;
            // transcribed from this control's own print (D2-trip1.log).
            // GetUnderlierLEIForCredit.java (was fork=[0, 0, 0, 0] golden=[0, 1, 0, 0]) left this list at
            // seat 31: law 2 rung 2 (lambdaItemReceiverType - the ADD-terminal deref) healed it WHOLE
            // in all four drr 7.x cells (locked whole by ReceiverRenderTypingSeatTest corpus_c1 at 7.0.0).
            // Re-pinned from the control's own measured print (delta = the one row REMOVED, nothing else).
            // (The law-2 commit's own note called this "the drr 5.61.0 row" - there is no 5.61.0 sibling:
            // the file exists only under drr 7.0-7.3; corrected at the review of #603, MF-4.)
            // the UnderlyingIndexIndicatorRule row (fork=[2, 2, 0, 2] golden=[3, 2, 0, 2]) LEFT this list at seat 32: law D.1 (defaultJoinDerefAtCollapsedLeft)
            // healed it WHOLE in all four drr 7.x cells; transcribed from this control's own print (D1-trip1.log).
            // the Price row (fork=[12, 5, 0, 11] golden=[12, 8, 0, 11]) LEFT this list: law C.2 (heteroMetaDefaultJoinDeref + iteArmMultiDefaultTernary)
            // took the rung-1 default join, the last residue after C.1 - Price is WHOLE in all four drr 7.x cells;
            // transcribed from this control's own print (C2-trip1.log), a pure row removal.
            // LAW 81 re-pin (seat 33, law C.1): fork=[10, 0, 0, 9] -> fork=[12, 5, 0, 11] - the statement ladder moved this scan's fork side toward golden; the file stays BANDED on C.2's default join; from C1-trip1.log.
            // the QuantityUnitOfMeasure row (fork=[1, 2, 0, 0] golden=[2, 0, 0, 0]) LEFT this list: law B.24 (defaultJoinHeteroMetaDerefBoth +
            // iteArmMetaCollapseDerefSinkChannel + three in-seat rungs) healed the file WHOLE in all four drr 7.x
            // cells - the band's last four files; transcribed from this control's own print (B24-trip1.log),
            // a pure row removal (was == expected minus it).
            // LAW 81 re-pin (seat 33, law C.2): fork=[1, 0, 0, 0] -> fork=[1, 2, 0, 0] - R3a's join deref moved this scan's fork side (LAW 80 IMPROVED-not-whole, planned; QUOM closes at B.24); from C2-trip1.log.
            // the TotalNotionalQuantity row (fork=[4, 1, 0, 3] golden=[11, 1, 2, 9]) LEFT this list: law D.3 (fnDeepCondBaseConfinedArmChainAdmit,
            // seven rungs) healed the file WHOLE in all four drr 7.x cells; transcribed from this control's own
            // print (D3-trip1.log), a pure row removal (was == expected minus it).
            );
            // the UnderlyingAssetTradingPlatformIdentifierLeg1/Leg2 rows LEFT this list at seat 32: law A.1
            // (choiceOptionProjectionTypeId) healed both WHOLE in all four drr 7.x cells; transcribed from this
            // control's own print (A1-trip1.log).

    /**
     * The NAMED residue of drr 5.61.0 — MEASURED at the chain head c6871ed5 (5 entries then);
     * RE-MEASURED at the seat-31 chain head f2a4d5c0: the 2 entries below — the asic
     * CustomBasketCodeIdentifier + asic PlatformIdentifier rows LEFT (law 1) and the mas
     * PlatformIdentifier row LEFT (law 1b), each re-pinned from the fired control's own print.
     */
    private static final List<String> KNOWN_RESIDUE_561 = List.of();
            // the NotionalCurrencyLeg1Rule row (fork=[11, 0, 0, 5] golden=[21, 0, 0, 15]) LEFT this list at seat 32: law C.3's
            // ladder block made this scan's tuple equal golden's (the file stays BANDED on its wrapper-hop residue);
            // transcribed from the print (C3-trip1b.log).
            // LAW 81 re-pin (v3.1 flip seat 31, law 1): the asic CustomBasketCodeIdentifier +
            // PlatformIdentifier rows LEFT this list - both files healed WHOLE (byte-identical
            // to golden, locked by IteElseArmRecoveredMetaDerefSeatTest corpus_c1/c2). The
            // heal-tripwire fired exactly as prescribed and the list is re-pinned from the
            // control's own measured print (delta = the two rows REMOVED, nothing else).
            // LAW 81 re-pin (v3.1 flip seat 31, law 1b): the mas PlatformIdentifierRule row LEFT
            // this list - the file healed WHOLE (byte-identical to golden, locked by
            // MapperFormRuleRootArmSeatTest corpus_c1). Re-pinned from the control's own measured
            // print (delta = the one row REMOVED, nothing else).

    /** MEASURED at the chain head c6871ed5: the drr 7.0.0 union domain. */
    private static final int DOMAIN_DRR7 = 331;

    /** MEASURED at the chain head c6871ed5: the drr 5.61.0 union domain. */
    private static final int DOMAIN_DRR561 = 413;

    /**
     * (T1, T2, T3, T4) = guarded coercion sites, bare coercion sites, escaped coercion params,
     * numbered coercion params.
     *
     * <p><b>Scanned on COMMENTS-STRIPPED text, not on {@code codeOnly()}</b>: {@code codeOnly()}
     * strips STRING LITERALS, which deletes the {@code "Type coercion"} token the whole scan keys
     * on. Comment stripping alone is sufficient — {@code "Type coercion"} is emitted only by
     * {@code WrappedItemCoercer}, and after comment stripping no javadoc occurrence survives.
     */
    private static Map<String, int[]> scan(Map<String, String> tree) {
        Map<String, int[]> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : tree.entrySet()) {
            String code = commentsStripped(e.getValue());
            int t1 = 0;
            int t2 = 0;
            int t3 = 0;
            int t4 = 0;
            int i = code.indexOf("\"Type coercion\", ");
            while (i >= 0) {
                int arrow = code.indexOf(" ->", i);
                String param = arrow < 0 ? "" : code.substring(i + 17, arrow);
                int end = code.indexOf(')', arrow < 0 ? i : arrow);
                String body = end < 0 ? "" : code.substring(arrow < 0 ? i : arrow, end);
                if (body.contains("== null ? null :")) {
                    t1++;
                } else {
                    t2++;
                }
                if (param.startsWith("_")) {
                    t3++;
                }
                if (!param.isEmpty() && Character.isDigit(param.charAt(param.length() - 1))) {
                    t4++;
                }
                i = code.indexOf("\"Type coercion\", ", i + 1);
            }
            if (t1 + t2 + t3 + t4 > 0) {
                out.put(e.getKey(), new int[] {t1, t2, t3, t4});
            }
        }
        return out;
    }

    // =========================================================================
    // The union assert (LAW 73: pin the SET, not the count)
    // =========================================================================

    /**
     * The scan universe is the token-bearing union INTERSECTED with the files this harness
     * emits (rule/report/function kinds): golden's DATA-RULE (validation/datarule) files carry
     * coercion tokens but these generators never produce that kind — whether the fork emits
     * every expected FILE is the D11 ring's question (missingOutput), not this suite's. The
     * same correction as the law-6 suite's, hit by the same draft assert.
     */
    private static void assertUnionEqual(Map<String, int[]> a, Map<String, int[]> b,
            java.util.Set<String> emittedA, String aName, String bName, List<String> knownResidue,
            int expectedDomain) {
        assertTrue(expectedDomain >= 0,
                "the union domain is MEASURED and pinned (LAW 73) - a negative value means "
                        + "an unpinned call site");
        List<String> mismatched = new ArrayList<>();
        java.util.Set<String> universe = new java.util.TreeSet<>(a.keySet());
        universe.addAll(b.keySet());
        universe.retainAll(emittedA);
        int[] zero = new int[4];
        for (String key : universe) {
            int[] ac = a.getOrDefault(key, zero);
            int[] bc = b.getOrDefault(key, zero);
            if (!java.util.Arrays.equals(ac, bc)) {
                mismatched.add(key + " " + aName + "=" + java.util.Arrays.toString(ac)
                        + " " + bName + "=" + java.util.Arrays.toString(bc));
            }
        }
        assertEquals(knownResidue, mismatched,
                "(T1, T2, T3, T4) differ beyond the named residue in " + mismatched.size() + " file(s)");
        assertEquals(expectedDomain, universe.size(),
                "the union domain must equal the emitted token-bearing files (" + expectedDomain + ")");
    }

    // =========================================================================
    // Harness (the seat-26/27 suite shape verbatim)
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
            errors.forEach(e -> sink.add(e.getTargetPath() + " — " + e));
        }
    }

    private static void lockB(String path) throws IOException {
        assertNotNull(drrBOutput, "drr 7.0.0 generation did not run — corpus unavailable?");
        List<String> lockedErrors = drrBGenErrors.stream().filter(e -> e.contains(path)).toList();
        assertTrue(lockedErrors.isEmpty(),
                "the generator reported errors for the locked file " + path + ": " + lockedErrors);
        String generated = drrBOutput.get(path);
        assertNotNull(generated, "not generated in drr 7.0.0: " + path);
        Path goldenPath = GOLDEN_B.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "golden missing: " + goldenPath);
        assertEquals(normalize(Files.readString(goldenPath)), normalize(generated),
                "generated drr 7.0.0 output must byte-match golden (newline-normalized) for "
                + path + " — seat 28 law 8: chainRendersMapperC's three missing root rungs.");
    }

    private static void assertContains(String out, String needle) {
        assertTrue(out.contains(needle), "expected <" + needle + "> in:\n" + out);
    }

    private static int count(String haystack, String needle) {
        int n = 0;
        int i = haystack.indexOf(needle);
        while (i >= 0) {
            n++;
            i = haystack.indexOf(needle, i + needle.length());
        }
        return n;
    }

    /** Strip line and block comments ONLY — string literals SURVIVE (the scan keys on one). */
    private static String commentsStripped(String java) {
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
                sb.append(java, i, Math.min(j + 1, n));
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
            RModel main = AstBuilder.buildFromString(MODEL, "seat28law8.rosetta");
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
                .forEach(e -> errors.add(e.getTargetPath() + " — " + e));
        fg.generateWithErrors(out)
                .forEach(e -> errors.add(e.getTargetPath() + " — " + e));
        if (!errors.isEmpty()) {
            throw new AssertionError("fixture generation errors (a broken fixture"
                    + " must fail loudly, not skip): " + errors);
        }
        return out;
    }

    private static Map<String, String> fixture() throws IOException {
        if (fixtureOut == null) {
            fixtureOut = render(m -> "census.seat28law8".equals(m.namespace()));
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
                        failures.add(p + " — " + e);
                    }
                });
        if (!failures.isEmpty()) {
            throw new AssertionError("[ChainMapperCRootRungsSeatTest] builtins parse"
                    + " failures: " + String.join("; ", failures));
        }
        return models;
    }
}
