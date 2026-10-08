package com.regnosys.rosetta.generator.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

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
import com.regnosys.rosetta.symbols.RWorkspace;

/**
 * SEAT 32, law F.1 — facet {@code ruleArmBareOutputEvidence} (census F25): <b>the EFFECTIVE-ELSE
 * single-conditional block renderer joins the #295/#334/#338 mixed-arm meta-join law its two
 * sibling block renderers already carry, and the bare-side evidence it joins on is the sibling
 * RULE's own bare output.</b>
 *
 * <p><b>THE CARRIER.</b> Golden drr 5.61.0 asic
 * {@code drr/regulation/asic/rewrite/trade/reports/EffectiveDateRule.java}, from
 * {@code regulation-asic-rewrite-trade-rule.rosetta:355-363}:
 * <pre>
 *     filter IsAllowableActionForASIC
 *     then extract
 *         if IsFRA(ProductForEvent)
 *         then TradeForEvent -&gt; tradeDate
 *         else CDEEffectiveDate
 * </pre>
 * golden: {@code MapperS.of(tradeForEvent.evaluate(item.get())).<FieldWithMetaDate>map(
 * "getTradeDate", trade -> trade.getTradeDate()).<Date>map("Type coercion", fieldWithMetaDate ->
 * fieldWithMetaDate == null ? null : fieldWithMetaDate.getValue())} + the import
 * {@code com.rosetta.model.metafields.FieldWithMetaDate} (sigs B089 + B088);
 * fork: {@code MapperS.of(TradeForEvent).map("getTradeDate", _tradeForEvent ->
 * _tradeForEvent.getTradeDate())} — the CLASS name as a value, no type witness, the lambda named
 * off the SYMBOL not the callee's output type. ONE whole file; the file carries nothing else.
 *
 * <p><b>THE SEAT, AND THE RE-SITING.</b> The close census (F25) and the seat-32 scout both site
 * this on {@code ReferenceHandler.nestedTreeArmLeafMetaKind} /
 * {@code CollectionHandler.compileNestedConditionalBlock}. That is REFUTED BY READING at this
 * head: {@code isNestedThenConditional} requires {@code cond.thenBranch() instanceof
 * RConditionalExpr}, and the asic tree's then-branch is a plain nav — so the tree is served by
 * {@code compileEffectiveElseConditionalBlock}, whose {@code if (…) { return <then>; } return
 * <else>;} shape is exactly what BOTH golden and fork render. Two things were missing there:
 * <ol>
 *   <li><b>rung 1</b> — the #295 scope flag. {@code compileLadderConditionalBlock} pushes
 *       {@code pushMetaLeafBaresymAllowed()} on a mixed ladder and
 *       {@code compileNestedConditionalBlock} pushes it on a mixed tree; the effective-else
 *       renderer never did, so {@code ReferenceHandler}'s #280 conditional-arm meta-leaf gate
 *       declined the {@code TradeForEvent -> tradeDate} baresym nav (the other three doors are
 *       shut for it: {@code terminalMetaLeaf} is killed by {@code inConditionalArm},
 *       {@code conditionalJoinAgrees} is false on a bare join, {@code midChainMetaLeaf} needs a
 *       further hop) and the non-compiling bare type literal was emitted.</li>
 *   <li><b>rung 2</b> — the in-arm deref gate. The #334 THEN-arm deref probes its else sibling
 *       with the NARROW {@code isProvablyBareArm}; its own #339 ELSE-direction twin twenty lines
 *       below probes with the WIDE {@code isProvablyBareJoinArm} (=
 *       {@code isProvablyBareArm || isProvablyBareInvokableArm}). PR #339 widened one direction
 *       and never mirrored it, so a bare-output INVOKABLE else arm was not bare-join evidence.
 *       LAW 69 — the two halves of one law must agree.</li>
 * </ol>
 * {@code isProvablyBareInvokableArm} is where the facet name lives: {@code CDEEffectiveDate} is a
 * no-arg {@code RSymbolReference} on an {@code RRule} for which the #270 transitive walker
 * recovers NO wrapper (its body is a mixed else-if ladder, so {@code recoverMetaFromExpr} case (d)
 * returns null) — the rule's own bare output IS the evidence. LAW 69 again: the law consults that
 * EXISTING helper rather than minting a third read of a rule's output type.
 *
 * <p><b>THE CORPUS DISCRIMINATOR.</b> The GREEN esma / mas / fca {@code EffectiveDateRule} twins
 * nest a THIRD record-date arm ({@code PositionForEvent -> openDateTime -> date},
 * {@code regulation-mas-rewrite-trade-rule.rosetta:631-642}) INSIDE the then-branch, so they are
 * NESTED-THEN trees served by the #362 seat and already mixed through that arm's bare evidence.
 * They are byte-untouched here — {@code corpus_c2} and {@code e2} are their locks.
 *
 * <p><b>GREEN-SAFE BY THE #280 ARGUMENT</b>, restated: the declined form renders a bare Java TYPE
 * name as a value expression and never compiled, so no byte-identical golden can carry it. Rung 2
 * stays COMPILED-TYPE-gated (the arm's item type must actually BE an {@code RJavaWithMetaValue}),
 * so the widening cannot re-deref a bare or type-less arm.
 *
 * <p><b>CLAIMED RED at the seat's base (both routes)</b>: {@code a1}, {@code a2},
 * {@code corpus_c1}, {@code corpus_control1} (+ {@code corpus_control2} under {@code -Pir-on}).
 * <b>CLAIMED GREEN at the head</b>: 8/0F/1skip default, 8/0F/0skip {@code -Pir-on}.
 * {@code e1}, {@code e2}, {@code corpus_c2} are GREEN AT RED — they are the decline and
 * regression locks and must never move. Every figure here is CLAIMED — measured by the chain.
 *
 * <p><b>MUTATIONS (LAW 66/76) — MEASURED (LAW 82) by the seat-32 chain, run 1 at
 * {@code d99ded920} ({@code f32-mut-m-lawF1-*.log}):</b>
 * <ul>
 *   <li><b>m-lawF1-flag</b> (rung 1 severed — {@code effBaresymFlagPushed} forced false):
 *       MEASURED <b>8/4F/1S</b> = {@code a1}, {@code a2}, {@code corpus_c1},
 *       {@code corpus_control1} — the claim exactly; {@code e1}, {@code e2}, {@code corpus_c2}
 *       GREEN.</li>
 *   <li><b>m-lawF1-rulearm</b> (rung 2 severed — {@code isProvablyBareJoinArm} reverted to
 *       {@code isProvablyBareArm}): MEASURED <b>8/4F/1S</b> = {@code a1}, {@code a2},
 *       {@code corpus_c1}, {@code corpus_control1} — {@code a2}/{@code c1}/{@code control1}
 *       as claimed, and {@code a1} failing on its DEREF token while its first (fires) token
 *       passes, exactly the commit note's reading. The two rungs are independent, MEASURED.</li>
 *   <li><b>m-lawF1-metadecline</b> (the {@code isProvablyBareInvokableArm} rule branch forced
 *       true): MEASURED <b>8/1F/1S = {@code e1}</b> — the claim exactly: the discriminator is
 *       the sibling rule's OUTPUT meta-ness, not its shape. The predicate is SHARED with the #339
 *       else-direction gate and {@code FunctionExpressionRenderer.appendIteHoistChainCore}; the
 *       lane ran this suite alone (8 tests), so the collateral set under the mutation is NOT
 *       MEASURED — recorded as such, not narrowed away.</li>
 * </ul>
 * RED at the chain's base {@code ddcdd151b}: {@code a1}, {@code a2}, {@code corpus_c1},
 * {@code corpus_control1} (+ {@code corpus_control2} on {@code -Pir-on}); GREEN at the head
 * 8/0F/1skip default, 8/0F/0skip {@code -Pir-on}.
 *
 * <p><b>LAW 77.</b> {@code compileEffectiveElseConditionalBlock} is {@code private} on
 * {@code CollectionHandler} and {@code isProvablyBareJoinArm} /
 * {@code ReferenceHandler.baresymFunctionLeafMetaKind} are {@code public static} — neither is
 * virtual, and {@code IRCollectionHandler} overrides only {@code thenArgBaseName}, so the IR route
 * cannot substitute a different implementation even in principle. Grepping {@code rune-ir-java}
 * for {@code isMetaLeafBaresymAllowed} / {@code mixedArmJoin} / {@code isProvablyBare*} /
 * {@code compileEffectiveElseConditionalBlock} returns nothing. INHERITS;
 * {@code corpus_control2} measures it anyway.
 *
 * <p><b>LAW 74.</b> The fork's current asic text is NON-COMPILING: {@code MapperS.of(TradeForEvent)}
 * uses an imported CLASS as a value expression. MEASURED ({@code javac32-report.md} section 7.3
 * row C19): PRE 1 error -> POST 0.
 */
class RuleArmBareOutputEvidenceSeatTest {

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

    /** The carrier — the ONE whole file this law heals. */
    private static final String ASIC =
            "drr/regulation/asic/rewrite/trade/reports/EffectiveDateRule.java";

    /** The GREEN twin — the #362 nested-tree seat's carrier, the regression lock. */
    private static final String MAS =
            "drr/regulation/mas/rewrite/trade/reports/EffectiveDateRule.java";

    private static final Path CELL_ROOT = Path.of("../test-corpus/drr/drr-5.61.0");
    private static final Path GOLDEN = CELL_ROOT.resolve("rosetta-source/src/generated/java");

    static boolean cellAvailable() {
        return Files.isDirectory(GOLDEN);
    }

    static boolean cellAndIrProviderAvailable() {
        return cellAvailable() && irProviderOnClasspath();
    }

    // =========================================================================
    // Fixture — reduced from regulation-asic-rewrite-trade-rule.rosetta:355-363
    // (a1/a2/e1) and regulation-mas-rewrite-trade-rule.rosetta:631-642 (e2).
    // Same construct kinds, same cardinalities, same metadata annotation.
    // =========================================================================

    private static final String MODEL = """
            namespace census.seat32f1
            version "1.0.0"

            type Trade: <"the cdm Trade twin - its tradeDate leaf is the FieldWithMetaDate the baresym nav reaches">
                tradeDate date (0..1)
                    [metadata scheme]

            type Prod:
                isFra boolean (0..1)

            type Pos: <"the CounterpartyPosition twin - e2's record-date receiver">
                openDateTime dateTime (0..1)

            type Instr: <"the TransactionReportInstruction twin">
                ok boolean (0..1)
                trade Trade (0..1)
                position Pos (0..1)
                prod Prod (0..1)
                plainDate date (0..1)

            func IsAllowed: <"IsAllowableActionForASIC twin - the filter predicate">
                inputs:
                    i Instr (1..1)
                output:
                    result boolean (1..1)
                set result:
                    i -> ok = True

            func ProductForEvent: <"ProductForEvent twin - the bare-fn ARGUMENT of the condition">
                inputs:
                    i Instr (1..1)
                output:
                    product Prod (0..1)
                set product:
                    i -> prod

            func IsFra: <"IsFRA twin - the bare boolean fn call that hoists final Boolean _boolean">
                inputs:
                    p Prod (1..1)
                output:
                    result boolean (1..1)
                set result:
                    p -> isFra = True

            func TradeForEvent: <"TradeForEvent twin - its OUTPUT type carries the meta leaf">
                inputs:
                    i Instr (1..1)
                output:
                    reportableTrade Trade (0..1)
                set reportableTrade:
                    i -> trade

            func PositionForEvent: <"PositionForEvent twin - e2's record-date callee">
                inputs:
                    i Instr (1..1)
                output:
                    reportablePosition Pos (0..1)
                set reportablePosition:
                    i -> position

            reporting rule CdeEff from Instr: <"CDEEffectiveDate twin - a BARE-output rule: a plain date body, so the #270 walker recovers no wrapper">
                plainDate

            reporting rule CdeEffMeta from Instr: <"e1's sibling - a META-output rule: its body navigates the [metadata scheme] leaf, so the walker DOES recover FieldWithMetaDate">
                trade -> tradeDate

            reporting rule AsicEff from Instr: <"a1/a2 - THE asic EffectiveDate SHAPE: effective-else, a baresym META then arm beside a bare-output RULE else arm">
                filter IsAllowed
                then extract
                    if IsFra(ProductForEvent)
                    then TradeForEvent -> tradeDate
                    else CdeEff
                    as "a1"

            reporting rule EffMetaSibling from Instr: <"e1 - THE DECLINE LOCK: the same tree whose else arm is a META-output rule - no bare evidence, today's bytes kept">
                filter IsAllowed
                then extract
                    if IsFra(ProductForEvent)
                    then TradeForEvent -> tradeDate
                    else CdeEffMeta
                    as "e1"

            reporting rule EffNested from Instr: <"e2 - THE GREEN CONTROL: the mas/esma/fca twin - a NESTED-THEN tree served by the #362 seat, already mixed through its record-date arm">
                filter IsAllowed
                then extract
                    if IsFra(ProductForEvent)
                    then (if TradeForEvent exists
                        then TradeForEvent -> tradeDate
                        else if PositionForEvent exists
                        then PositionForEvent -> openDateTime -> date)
                    else CdeEff
                    as "e2"
            """;

    /** Golden's wrapper-witnessed baresym nav — the token the #280 un-decline (rung 1) produces. */
    private static final String WITNESSED_NAV =
            ".<FieldWithMetaDate>map(\"getTradeDate\", trade -> trade.getTradeDate())";

    /** Golden's in-arm deref — the token the widened bare-join evidence (rung 2) produces. */
    private static final String DEREF = ".<Date>map(\"Type coercion\", ";

    /** The fork's non-compiling bare type literal — the token both rungs together remove. */
    private static final String BARE_TYPE_LITERAL = "MapperS.of(TradeForEvent)";

    private static final String META_IMPORT =
            "import com.rosetta.model.metafields.FieldWithMetaDate;";

    /** e2's record-date hop — the #362 seat's own bare evidence, which must not move. */
    private static final String DT_RECORD = ".<Date>map(\"Date\", dt -> Date.of(dt.toLocalDate()))";

    // =========================================================================
    // Part A — the seat
    // =========================================================================

    /**
     * a1 — the four tokens of the heal: the wrapper-witnessed baresym nav (rung 1), the in-arm
     * deref (rung 2), the metafield import that rides the nav's refs, and the ABSENCE of the
     * non-compiling bare type literal.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_effectiveElseBaresymMetaArmFiresAndDerefsBesideABareOutputRuleArm() throws IOException {
        String out = fixtureRule("AsicEffRule");
        assertContains(out, WITNESSED_NAV);
        assertContains(out, DEREF);
        assertContains(out, META_IMPORT);
        assertTrue(!out.contains(BARE_TYPE_LITERAL),
                "the non-compiling bare type literal must be gone:\n" + out);
    }

    /**
     * a2 — the WHOLE golden expression, verbatim from
     * {@code test-corpus/drr/drr-5.61.0/rosetta-source/src/generated/java/drr/regulation/asic/
     * rewrite/trade/reports/EffectiveDateRule.java}, re-spelled against the fixture's names
     * (which are chosen so it is the SAME string): the receiver is the callee's evaluate, the
     * nav lambda is named off the callee's OUTPUT TYPE ({@code trade}, not {@code _tradeForEvent}),
     * and the deferred coercion parameter is the un-indexed {@code fieldWithMetaDate}.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_theWholeGoldenArmExpressionRenders() throws IOException {
        String out = fixtureRule("AsicEffRule");
        assertContains(out,
                "MapperS.of(tradeForEvent.evaluate(item.get()))"
                + ".<FieldWithMetaDate>map(\"getTradeDate\", trade -> trade.getTradeDate())"
                + ".<Date>map(\"Type coercion\", fieldWithMetaDate -> fieldWithMetaDate == null"
                + " ? null : fieldWithMetaDate.getValue())");
    }

    /**
     * e1 — THE DECLINE LOCK. The identical tree whose else arm is a META-output rule
     * ({@code CdeEffMeta}'s body navigates the {@code [metadata scheme]} leaf, so the #270 walker
     * recovers {@code FieldWithMetaDate} and {@code isProvablyBareInvokableArm} returns false):
     * no bare-join evidence, no flag, no deref. MEASURED at RED (F1-red1.log): the #265
     * ruleThenValueMetaWrap block already serves this tree today (the typed then-arm nav, the
     * wrapped else, no junk literal, no deref) — the lock pins those bytes. GREEN AT RED and at
     * the head.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void e1_aMetaOutputRuleArmIsNotBareEvidenceAndTheDeclineStands() throws IOException {
        // MEASURED AT RED (F1-red1.log): with a META-output rule else arm the #265
        // ruleThenValueMetaWrap block ALREADY renders this tree - the typed then-arm nav, the
        // else arm wrapped `MapperS.of(FieldWithMetaDate.builder().setValue(date).build())`, no
        // junk literal and NO in-arm deref. The drafter's "today's bytes carry the junk literal"
        // was a claim by reasoning; the lock is these measured bytes, which the law must keep.
        String out = fixtureRule("EffMetaSiblingRule");
        assertContains(out, WITNESSED_NAV);
        assertContains(out, "MapperS.of(FieldWithMetaDate.builder().setValue(date).build())");
        assertTrue(!out.contains(BARE_TYPE_LITERAL),
                "the META-output sibling never carried the junk literal:\n" + out);
        assertTrue(!out.contains("map(\"Type coercion\", "),
                "a META-output rule arm must not force an in-arm deref:\n" + out);
    }

    /**
     * e2 — THE GREEN CONTROL. The mas/esma/fca shape: a NESTED-THEN tree whose inner ladder
     * carries the record-date arm, so the #362 seat has ALREADY mixed it and already fires the
     * nav + deref. This law touches a different renderer and must leave every one of these three
     * tokens exactly where they are. GREEN AT RED and at the head.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void e2_theNestedTreeTwinIsAlreadyMixedAndIsUntouched() throws IOException {
        String out = fixtureRule("EffNestedRule");
        assertContains(out, WITNESSED_NAV);
        assertContains(out, DEREF);
        assertContains(out, DT_RECORD);
        assertTrue(!out.contains(BARE_TYPE_LITERAL),
                "the #362 seat already un-declines this tree:\n" + out);
    }

    // =========================================================================
    // Part B — the corpus
    // =========================================================================

    /** corpus_c1 — the carrier's whole-file heal (drr 5.61.0 asic). */
    @Test
    @EnabledIf("cellAvailable")
    void corpus_c1_drr5610AsicEffectiveDateRuleMatchesGolden() throws IOException {
        assertNotNull(drrOutput, "drr 5.61.0 generation did not run");
        List<String> own = drrGenErrors.stream().filter(e -> e.contains(ASIC)).toList();
        assertTrue(own.isEmpty(), "generation errors for " + ASIC + ": " + own);
        String gen = drrOutput.get(ASIC);
        assertNotNull(gen, "not generated: " + ASIC);
        assertEquals(normalize(Files.readString(GOLDEN.resolve(ASIC))), normalize(gen),
                "asic EffectiveDateRule must byte-match golden — seat 32 law F.1: the"
                + " effective-else block joins the mixed-arm meta join on the sibling rule's"
                + " bare output");
    }

    /**
     * corpus_c2 — THE REGRESSION LOCK. The mas twin is GREEN at the base: it is a nested-then
     * tree served by the #362 seat and already mixed through its {@code positionForEvent ->
     * openDateTime -> date} arm. It must stay byte-identical.
     */
    @Test
    @EnabledIf("cellAvailable")
    void corpus_c2_drr5610MasEffectiveDateRuleStaysGolden() throws IOException {
        assertNotNull(drrOutput, "drr 5.61.0 generation did not run");
        String gen = drrOutput.get(MAS);
        assertNotNull(gen, "not generated: " + MAS);
        assertEquals(normalize(Files.readString(GOLDEN.resolve(MAS))), normalize(gen),
                "mas EffectiveDateRule is the GREEN twin — seat 32 law F.1 must not move it");
    }

    /**
     * corpus_control1 — LAW 79, the UNION whole-cell control over drr 5.61.0. Four tokens,
     * counted per file over the WHOLE golden tree and the WHOLE fork emission, unioned and
     * scoped to the files this harness emits (the seat-28 correction class):
     * <ul>
     *   <li><b>T1</b> {@code MapperS.of(TradeForEvent)} — the fork's non-compiling defect token.
     *       Golden 0 everywhere; fork 1, in the carrier alone. UNDER-FIRE sentinel.</li>
     *   <li><b>T2</b> {@code <FieldWithMetaDate>map("getTradeDate"} — the wrapper witness rung 1
     *       produces.</li>
     *   <li><b>T3</b> {@code map("Type coercion", fieldWithMetaDate} — the in-arm deref rung 2
     *       produces.</li>
     *   <li><b>T4</b> {@code MapperS.<FieldWithMeta} + {@code MapperS.<ReferenceWithMeta} — the
     *       wrapper-typed empty terminal. This is the seat-24 F28 wrapper-join-stamp sentinel:
     *       any law that flips a tree's mixed verdict SUPPRESSES that stamp and this count
     *       DROPS on the fork side. OVER-FIRE sentinel.</li>
     * </ul>
     *
     * <p><b>DERIVED BY A READ-ONLY WALK</b> at head {@code ddcdd151b}: python {@code os.walk} over
     * {@code test-corpus/drr/drr-5.61.0/rosetta-source/src/generated/java} (5,249 golden
     * {@code .java} files) with the fork side reconstructed as golden-for-every-non-band-file plus
     * the 13 drr 5.61.0 band files taken from the seat-31 final OFF dump. Union domain (any token
     * non-zero on either side) = <b>113</b> files — 109 under {@code reports/}, 4 under
     * {@code functions/}, so every one of them is emitted by this harness and
     * {@code retainAll(emitted)} does not shrink it. Whole-cell totals AT THE BASE HEAD
     * {@code ddcdd151b}: T1 golden 0 / fork 1; T2 golden 6 / fork 5; T3 golden 7 / fork 6;
     * T4 golden 119 / fork 118 (after law C.3, T4 fork == golden == 119).
     *
     * <p><b>THE EXPECTED RESIDUE AT THIS HEAD IS EMPTY.</b> At the base head there are TWO rows:
     * the carrier (this law's) and {@code NotionalCurrencyLeg2Rule} (sig B084, the pre-existing
     * 40-line drr 5.61.0 band file whose fork dropped one wrapper-typed empty) -- the carrier's
     * row is removed by this law, the B084 row by law C.3 later in the seat, so the control is RED
     * at the base and GREEN at the seat's head with {@code KNOWN_RESIDUE_DRR561 = List.of()}.
     * LAW 73: the SET is pinned, not the count.
     */
    @Test
    @EnabledIf("cellAvailable")
    void corpus_control1_forkDrr561WholeCellMetaJoinTokensEqualGoldenFileByFile()
            throws IOException {
        assertNotNull(drrOutput, "drr 5.61.0 generation did not run");
        assertEquals(List.of(), drrGenErrors,
                "drr 5.61.0 reported a generation error — the scan is incomplete");
        assertUnionEqual(scan(drrOutput), scan(readGoldenTree(GOLDEN)), drrOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_DRR561, DOMAIN_DRR561);
    }

    /**
     * corpus_control2 — LAW 77, BOTH-ROUTES-vs-GOLDEN. The seat is a {@code private} method on
     * {@code CollectionHandler} reached through the shared handler stack, so the IR route
     * inherits it; this measures that rather than asserting it.
     */
    @Test
    @EnabledIf("cellAndIrProviderAvailable")
    void corpus_control2_irRouteMatchesGoldenForBothEffectiveDateRules() throws IOException {
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "5.61.0", CELL_ROOT),
                new ArrayList<>());
        assertEquals(normalize(Files.readString(GOLDEN.resolve(ASIC))),
                normalize(irOut.get(ASIC)), "IR route vs GOLDEN: " + ASIC);
        assertEquals(normalize(Files.readString(GOLDEN.resolve(MAS))),
                normalize(irOut.get(MAS)), "IR route vs GOLDEN: " + MAS);
    }

    // =========================================================================
    // The union instrument (LAW 73/79) — the seat-27/28 shape
    // =========================================================================

    /**
     * MEASURED by the read-only walk described on {@code corpus_control1}: EXACTLY ZERO entries
     * at the seat-32 head; TWO at the base head {@code ddcdd151b} — the carrier's row
     * {@code drr/regulation/asic/rewrite/trade/reports/EffectiveDateRule.java fork=[1, 0, 0, 0]
     * golden=[0, 1, 1, 0]}, removed by this law, and the B084 row, removed by law C.3.
     */
    private static final List<String> KNOWN_RESIDUE_DRR561 = List.of();
            // the NotionalCurrencyLeg2Rule row (fork=[0, 0, 0, 4] golden=[0, 0, 0, 5]) LEFT this list at seat 32: law C.3 (ruleThenArmLadderNestedTreeAdmit)
            // healed it WHOLE in drr 5.61.0 after this suite's pins were measured; transcribed from the checkpoint-2
            // full-gensuite print (ckpt2-gensuite.log).
            // The T4 over-fire sentinel is now UNWITNESSED at this cell (whole-cell T4 fork ==
            // golden == 119 after C.3) and fires on ANY non-zero divergence in the 113-file union
            // domain.

    /** MEASURED at head {@code ddcdd151b}: the drr 5.61.0 union domain (see corpus_control1). */
    private static final int DOMAIN_DRR561 = 113;

    private static final String T1 = "MapperS.of(TradeForEvent)";
    private static final String T2 = "<FieldWithMetaDate>map(\"getTradeDate\"";
    private static final String T3 = "map(\"Type coercion\", fieldWithMetaDate";
    private static final String T4A = "MapperS.<FieldWithMeta";
    private static final String T4B = "MapperS.<ReferenceWithMeta";

    private static int count(String haystack, String needle) {
        int n = 0;
        for (int i = haystack.indexOf(needle); i >= 0; i = haystack.indexOf(needle, i + 1)) {
            n++;
        }
        return n;
    }

    private static Map<String, int[]> scan(Map<String, String> tree) {
        Map<String, int[]> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : tree.entrySet()) {
            String code = e.getValue();
            int[] t = new int[] {
                count(code, T1),
                count(code, T2),
                count(code, T3),
                count(code, T4A) + count(code, T4B),
            };
            if (t[0] + t[1] + t[2] + t[3] > 0) {
                out.put(e.getKey(), t);
            }
        }
        return out;
    }

    private static void assertUnionEqual(Map<String, int[]> a, Map<String, int[]> b,
            Set<String> emittedA, String aName, String bName, List<String> knownResidue,
            int expectedDomain) {
        List<String> mismatched = new ArrayList<>();
        Set<String> universe = new TreeSet<>(a.keySet());
        universe.addAll(b.keySet());
        universe.retainAll(emittedA);
        int[] zero = new int[4];
        for (String key : universe) {
            int[] ac = a.getOrDefault(key, zero);
            int[] bc = b.getOrDefault(key, zero);
            if (!Arrays.equals(ac, bc)) {
                mismatched.add(key + " " + aName + "=" + Arrays.toString(ac)
                        + " " + bName + "=" + Arrays.toString(bc));
            }
        }
        assertEquals(knownResidue, mismatched,
                "(T1, T2, T3, T4) differ beyond the named residue in " + mismatched.size()
                + " file(s)");
        assertEquals(expectedDomain, universe.size(),
                "the union domain must equal the oracle's token-bearing files ("
                + expectedDomain + ")");
    }

    // =========================================================================
    // Fixture harness (the InLambdaBoolHoistShadowSeatTest rule renderer, verbatim)
    // =========================================================================

    private record Render(Map<String, String> output, List<String> errors) {}

    private static Render rendered;

    private static Render render() throws IOException {
        if (rendered != null) {
            return rendered;
        }
        RModel main = AstBuilder.buildFromString(MODEL, "seat32f1.rosetta");
        main.setVersion("0.0.0.test");
        List<RModel> models = new ArrayList<>();
        models.add(main);
        models.addAll(loadBuiltinsOnly());
        RWorkspace workspace = RWorkspace.build(models).workspace();
        GeneratorModel gm = new GeneratorModel(workspace,
                m -> "census.seat32f1".equals(m.namespace()));
        JavaTypeUtil typeUtil = new JavaTypeUtil();
        JavaTypeTranslator tt = new JavaTypeTranslator(typeUtil);
        FunctionGenerator fg = new FunctionGenerator(gm, tt, typeUtil);
        RuleGenerator ruleGen = new RuleGenerator(gm, tt, fg);
        Map<String, String> out = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        ruleGen.generateClasses(main, "1.0", out)
                .forEach(e -> errors.add(e.getTargetPath() + " - " + e));
        fg.generateWithErrors(out)
                .forEach(e -> errors.add(e.getTargetPath() + " - " + e));
        rendered = new Render(out, errors);
        return rendered;
    }

    private static String fixtureRule(String ruleName) throws IOException {
        Render r = render();
        String path = ruleName + ".java";
        List<String> own = r.errors().stream().filter(e -> e.contains(path)).toList();
        assertTrue(own.isEmpty(), "the generator reported errors for " + path + ": " + own);
        String out = r.output().entrySet().stream()
                .filter(e -> e.getKey().substring(e.getKey().lastIndexOf('/') + 1).equals(path))
                .map(Map.Entry::getValue)
                .findFirst().orElse(null);
        assertNotNull(out, "not generated: " + path + " (have: " + r.output().keySet() + ")");
        return out;
    }

    private static void assertContains(String out, String token) {
        assertTrue(out.contains(token), "expected token missing:\n  " + token + "\nin:\n" + out);
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
            throw new AssertionError("[RuleArmBareOutputEvidenceSeatTest] builtins parse"
                    + " failures: " + String.join("; ", failures));
        }
        return models;
    }

    // =========================================================================
    // Corpus harness (the ReceiverRenderTypingSeatTest cell generator, verbatim)
    // =========================================================================

    private static Map<String, String> drrOutput;
    private static List<String> drrGenErrors;

    @BeforeAll
    static void generateCells() throws IOException {
        if (cellAvailable()) {
            List<String> errs = new ArrayList<>();
            drrOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "5.61.0", CELL_ROOT), errs);
            drrGenErrors = errs;
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

    private static Map<String, String> generateCellOnIrRoute(D11CorpusRegressionTest.CellSpec cell,
            List<String> errors) throws IOException {
        String previous = System.getProperty(IRGeneration.PROPERTY);
        System.setProperty(IRGeneration.PROPERTY, "true");
        try {
            assertNotNull(IRGeneration.providerOrNull(),
                    "the IR provider must be resolvable under -Pir-on");
            var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
            var gm = new GeneratorModel(corpus.workspace(),
                    D11CorpusRegressionTest.emissionFilter(cell));
            var typeUtil = new JavaTypeUtil();
            var typeTranslator = new JavaTypeTranslator(typeUtil);
            FunctionGenerator funcGen = IRGeneration.functionGenerator(gm, typeTranslator, typeUtil);
            assertTrue(!funcGen.getClass().equals(FunctionGenerator.class),
                    "the seam must hand back the IR-route FunctionGenerator");
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

    private static String normalize(String s) {
        return s.replace("\r\n", "\n");
    }

}
