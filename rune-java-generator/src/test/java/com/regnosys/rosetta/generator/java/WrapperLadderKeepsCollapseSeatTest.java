package com.regnosys.rosetta.generator.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import com.regnosys.rosetta.generator.java.enums.EnumGenerator;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.function.LabelProviderGenerator;
import com.regnosys.rosetta.generator.java.function.LabelProviderGeneratorUtil;
import com.regnosys.rosetta.generator.java.object.ChoiceObjectGenerator;
import com.regnosys.rosetta.generator.java.object.ModelObjectGenerator;
import com.regnosys.rosetta.generator.java.object.datarule.DataRuleGenerator;
import com.regnosys.rosetta.generator.java.reports.ReportGenerator;
import com.regnosys.rosetta.generator.java.reports.RuleGenerator;
import com.regnosys.rosetta.generator.java.spi.IRGeneration;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.symbols.RLinkingResult;
import com.regnosys.rosetta.symbols.RWorkspace;
import com.regnosys.rosetta.utils.DeepFeatureCallUtil;

/**
 * SEAT 24, law F28 — facet {@code wrapperLadderKeepsCollapse}: <b>a ladder whose ITEM is the meta wrapper keeps a
 * list-literal collapse WRAPPED (no in-rung deref) — lifted to a singleton list at the MapperC seat, wrapped
 * {@code MapperS.of(…)} at the MapperS seat — and a nested-then tree reads the SAME wrapper join the flat ladder
 * reads (LAW 69)</b>. Upstream types the lambda's return element from the ladder's join (the wrapper when every
 * arm is the wrapper) and coerces the collapsed item to that Mapper; the rule's output deref happens ONCE at the
 * output assign (seat 22's F1 {@code wholeOutputMetaDeref}), never in the arm. Golden drr 5.61.0 esma/fca/jfsa
 * {@code UnderlyingIdentificationRule}: {@code return MapperC.of(Collections.singletonList(MapperC.<FieldWithMetaString>
 * of(….first(), ….first()).get()));} under {@code return MapperC.<FieldWithMetaString>ofNull();}; mas
 * {@code UnderlyingRule}: {@code return MapperS.of(MapperC.<FieldWithMetaString>of(…).get());} under {@code MapperS.<
 * FieldWithMetaString>ofNull()} with the output deref {@code final FieldWithMetaString fieldWithMetaString = thenArg1
 * …get(); if (… == null) { output = null; } else { output = ….getValue(); }}; asic/jfsa {@code SingleBarrierPriceCurrencyRule}
 * (the nested tree): {@code final MapperS<FieldWithMetaString> thenArg1} + {@code MapperS.<FieldWithMetaString>ofNull()}.
 *
 * <p><b>The defect — and the producer the probe REFUTED (LAW 72).</b> The close census charted
 * {@code ControlFlowHandler:449} (the deep-then ladder's meta-blind snapshot). The seat-24 runtime probes over all
 * 275 matrix rows on BOTH routes showed that seat's carrier lines to be the OUTER {@code Product}/{@code TradableProduct}
 * conditionals; the identifier ladders are {@code CollectionHandler.compileLadderConditionalBlock}'s and are ALREADY
 * wrapper-typed there ({@code armJoinBare=false}, {@code first=META<String>}, {@code typedEmpty=MapperC/MapperS.<
 * FieldWithMetaString>ofNull()}). The defect is narrower: (a) the #337 {@code listLiteralNavMetaDeref} rung arm
 * ({@code renderLadderLevel}) fires at a WRAPPER-typed ladder seat — 4 of its 22 corpus firings (the other 18, the
 * DTCC_Leg1/2FloatingRateIndex rules in drr 6.34–7.3, are at a VALUE-typed seat and are GREEN); at the MapperC seat
 * the #373 decline then stood in for a form golden DOES witness (the three {@code UnderlyingIdentificationRule}s);
 * (b) {@code compileNestedConditionalBlock} types its empties from the meta-blind inferred type with NO wrapper
 * recovery ({@code item=java.lang.String} for the SingleBarrierPriceCurrency trees) while the flat ladder's #333
 * {@code metaTypedOfNull} arm recovers the wrapper join.
 *
 * <p><b>The seat (ONE law, three consumers).</b> {@code CollectionHandler.typedEmptyWrapperJoinOrNull} — the #333
 * recovery EXTRACTED from the flat ladder (every arm agrees on ONE wrapper whose value type is the meta-blind join)
 * — is consulted by the flat ladder (as before) AND by the nested tree; a wrapper-typed ladder's list-literal collapse
 * rung declines the #337 deref (the seat is not value-returning) and takes the #334 wrap / the MapperC singleton-list
 * lift; the nested tree threads its wrapper join out as the #387 chain-type stamp so the then-arg declaration types
 * {@code MapperS<FieldWithMetaString>} (FER's decl seat — rule 1 of {@code NavigationHandler.levelMetaWrapper}, the
 * level's own compiled stamp, since PR #613 retired the refs scan that rule replaced; it still only skips when the
 * block's terminal is the BARE {@code ofNull()}).
 *
 * <p><b>RED at the pre-seat blob</b> (the head before this law's commit, this suite kept — MEASURED by the receipts
 * chain at the final head, LAW 78/82): a1, a2, a3, {@code corpus_c1}, {@code corpus_c2}, {@code corpus_control1} — 6F
 * (under {@code -Pir-on} also a4 — 7F); b1, b2, {@code control0}, {@code control3} GREEN in both states
 * ({@code control2} compares routes — GREEN in both). <b>LAW 66/76 mutations</b> (each applied → run → reverted at
 * the FINAL head; the failing SETS are the chain's MEASUREMENT, not the charter's prediction): (i) the wrapper-seat
 * decline of the #337 arm deleted → a1, a2, c1, c2, control1 — 5F; (ii) the decline applied at EVERY seat (the
 * over-reach: the value-typed seat loses its deref) → b1, control3 — 2F; (iii) the nested tree's wrapper recovery
 * deleted → a3 — 1F; (iv) the ladder's MapperC singleton lift reverted to the #373 decline → a1, c1, control1 + law
 * F4's {@code BareFnCondHoistEveryContextSeatTest.corpus_control1} (the esma/fca/jfsa {@code UnderlyingIdentificationRule}
 * ladders fall to the decline and lose their hoists — fork {@code [0, 0, 0]} against golden {@code [2, 2, 0]}, beyond
 * that control's named residue) — 4F; (v) the wrapper walker's list-literal collapse case deleted
 * ({@code NavigationHandler} no longer resolves a {@code [meta-nav first, …] only-element} arm through
 * {@code listLiteralNavMetaWrapper}) → a2, c2 — 2F (the single-ladder output deref: the mas {@code UnderlyingRule}
 * carrier and its fixture twin; a1, c1 and control1 do NOT move).
 */
class WrapperLadderKeepsCollapseSeatTest {

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

    static boolean builtinsAndIrProviderAvailable() {
        return builtinsAvailable() && irProviderOnClasspath();
    }

    static boolean drr561AndIrProviderAvailable() {
        return drr561Available() && irProviderOnClasspath();
    }

    /**
     * {@code [metadata scheme]} string leaves (FieldWithMetaString) on a single and a multi holder, a boolean
     * function (the IsSwap / IsOption twins), a navigation function, and one reporting rule per shape.
     */
    private static final String MODEL = """
            namespace census.seat24f28
            version "1.0.0"

            type Ident:
                code string (0..1)
                    [metadata scheme]
                plain string (0..1)

            type Leg:
                ident Ident (0..1)
                idents Ident (0..*)
                name string (0..1)

            type Trig:
                ccy string (0..*)
                    [metadata scheme]

            type Knock:
                knockIn Trig (0..1)
                knockOut Trig (0..1)

            type Instr:
                legA Leg (0..1)
                legB Leg (0..1)
                legs Leg (0..*)
                knock Knock (0..1)
                isOpt boolean (0..1)
                allowed boolean (0..1)

            func IsAllowable: <"IsAllowableAction twin - the function filter">
                inputs:
                    i Instr (0..1)
                output:
                    result boolean (1..1)
                set result:
                    i -> allowed = True

            func IsOpt: <"IsOption / Qualify_BaseProduct_IRSwap twin - a bare fn-call condition">
                inputs:
                    i Instr (0..1)
                output:
                    result boolean (1..1)
                set result:
                    i -> isOpt = True

            func KnockFor: <"EconomicTermsForProduct twin - the chain head the nested tree navigates from">
                inputs:
                    i Instr (0..1)
                output:
                    k Knock (0..1)
                set k:
                    i -> knock

            func FormatCcy: <"ConvertNonISOToISOCurrency twin - the trailing then step that needs the typed then-arg decl">
                inputs:
                    s string (0..1)
                output:
                    r string (1..1)
                set r:
                    s

            reporting rule A1MultiLadder from Instr: <"a1 - THE esma UnderlyingIdentification SHAPE: a MULTI (MapperC) ladder of wrapper arms whose last rung is a list-literal collapse, then last (the carrier's own tail)">
                filter IsAllowable
                then extract
                    if legA -> ident -> code exists
                    then legA -> ident -> code
                    else if legs -> ident -> code exists
                    then legs -> ident -> code
                    else if IsOpt(item)
                    then [legA -> idents -> code first, legB -> idents -> code first] only-element
                then last
                    as "a1"

            reporting rule A2SingleLadder from Instr: <"a2 - THE mas Underlying SHAPE: a single (MapperS) ladder of wrapper arms whose last rung is a list-literal collapse; the output derefs ONCE">
                filter IsAllowable
                then extract
                    if legA -> ident -> code exists
                    then legA -> ident -> code
                    else if IsOpt(item)
                    then [legA -> idents -> code first, legB -> idents -> code first] only-element
                    as "a2"

            reporting rule A3NestedTree from Instr: <"a3 - THE SingleBarrierPriceCurrency SHAPE: a nested-then tree whose collapse arms are wrapper chains, followed by a then step">
                filter IsAllowable
                then extract
                    if IsOpt(item)
                    then if KnockFor(item) -> knockIn exists and KnockFor(item) -> knockOut is absent
                        then KnockFor(item) -> knockIn -> ccy only-element
                        else if KnockFor(item) -> knockIn is absent and KnockFor(item) -> knockOut exists
                        then KnockFor(item) -> knockOut -> ccy only-element
                then FormatCcy
                    as "a3"

            reporting rule B1ValueLadder from Instr: <"b1 - THE DTCC_Leg1FloatingRateIndex SHAPE: a list-literal collapse rung beside a BARE (plain string) arm joins bare, so the #337 deref stays">
                filter IsAllowable
                then extract
                    if legA -> ident -> code exists
                    then [legA -> idents -> code first, legB -> idents -> code first] only-element
                    else if legA -> name exists
                    then legA -> name
                    as "b1"

            reporting rule B2WrapperLadderNoLiteral from Instr: <"b2 - a wrapper ladder with NO list-literal rung keeps its bytes">
                filter IsAllowable
                then extract
                    if legA -> ident -> code exists
                    then legA -> ident -> code
                    else if legB -> ident -> code exists
                    then legB -> ident -> code
                    as "b2"
            """;

    private static final String DEREF_PAIR_RETURN =
            "return fieldWithMetaString == null ? MapperS.<String>ofNull() : MapperS.of(fieldWithMetaString.getValue());";

    // =========================================================================
    // Part A — the seat (RED at the pre-seat blob)
    // =========================================================================

    /**
     * a1 — the MapperC ladder: the list-literal collapse lifts to a singleton list, the terminal stays the wrapper,
     * no in-rung deref; the rule's output (after {@code then last}) derefs ONCE at the output assign (F1).
     * (A MULTI-output ladder WITHOUT the trailing collapse is NOT pinned: the fork's output assign there carries a
     * PRE-EXISTING scalar deref over a {@code MapperC} — measured identical before and after this law, another
     * family's, outside the corpus shape.)
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_multiLadderCollapseLiftsToSingletonList() throws IOException {
        String out = rule("A1MultiLadderRule.java");
        assertContains(out, "return MapperC.of(Collections.singletonList(MapperC.<FieldWithMetaString>of(");
        assertContains(out, "return MapperC.<FieldWithMetaString>ofNull();");
        assertNotContains(out, "== null ? MapperS.<String>ofNull()");
        assertNotContains(out, "final FieldWithMetaString _fieldWithMetaString");
        assertContains(out, "import java.util.Collections;");
        assertContains(out, "output = fieldWithMetaString.getValue();");
    }

    /** a2 — the MapperS ladder: the collapse wraps MapperS.of(…), the terminal stays the wrapper, the output derefs ONCE. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_singleLadderCollapseWrapsAndOutputDerefsOnce() throws IOException {
        String out = rule("A2SingleLadderRule.java");
        assertContains(out, "return MapperS.of(MapperC.<FieldWithMetaString>of(");
        assertContains(out, "return MapperS.<FieldWithMetaString>ofNull();");
        assertNotContains(out, "== null ? MapperS.<String>ofNull()");
        assertContains(out, "final FieldWithMetaString fieldWithMetaString = thenArg");
        assertContains(out, "output = fieldWithMetaString.getValue();");
    }

    /** a3 — the nested tree reads the wrapper join: the typed empty and the then-arg declaration are the wrapper. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a3_nestedTreeReadsTheWrapperJoin() throws IOException {
        String out = rule("A3NestedTreeRule.java");
        assertContains(out, "return MapperS.<FieldWithMetaString>ofNull();");
        assertNotContains(out, "return MapperS.<String>ofNull();");
        assertContains(out, "final MapperS<FieldWithMetaString> thenArg1 = thenArg0");
        assertContains(out, ".<FieldWithMetaString>mapC(\"getCcy\", trig -> trig.getCcy()).get());");
    }

    /** a4 — LAW 77: the a1 shape through the REAL {@code IRGeneration.functionGenerator} seam ({@code -Pir-on}). */
    @Test
    @EnabledIf("builtinsAndIrProviderAvailable")
    void a4_multiLadderCollapseLiftsOnIrRoute() throws IOException {
        String out = lookup(fixtureOnIrRoute(), "reports/A1MultiLadderRule.java");
        assertContains(out, "return MapperC.of(Collections.singletonList(MapperC.<FieldWithMetaString>of(");
        assertContains(out, "return MapperC.<FieldWithMetaString>ofNull();");
    }

    // =========================================================================
    // Part B — placement pins (GREEN in BOTH states)
    // =========================================================================

    /** b1 — the VALUE-typed seat (a bare arm joins the ladder bare) keeps the #337 deref pair. */
    @Test
    @EnabledIf("builtinsAvailable")
    void b1_valueTypedSeatKeepsTheDeref() throws IOException {
        String out = rule("B1ValueLadderRule.java");
        assertContains(out, "final FieldWithMetaString fieldWithMetaString = MapperC.<FieldWithMetaString>of(");
        assertContains(out, DEREF_PAIR_RETURN);
        assertContains(out, "return MapperS.<String>ofNull();");
    }

    /** b2 — a wrapper ladder with no list-literal rung keeps its bytes. */
    @Test
    @EnabledIf("builtinsAvailable")
    void b2_wrapperLadderWithoutLiteralRungUnchanged() throws IOException {
        String out = rule("B2WrapperLadderNoLiteralRule.java");
        assertContains(out, "return MapperS.<FieldWithMetaString>ofNull();");
        assertNotContains(out, "Collections.singletonList(");
        assertNotContains(out, "== null ? MapperS.<String>ofNull()");
    }

    // =========================================================================
    // Part C — the corpus (drr 5.61.0: the carriers + the whole-cell control; drr 7.0.0: LAW 79)
    // =========================================================================

    private static final Path DRR561_CELL_ROOT = Path.of("../test-corpus/drr/drr-5.61.0");
    private static final Path DRR561_GOLDEN = DRR561_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path DRR7_CELL_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path DRR7_GOLDEN = DRR7_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    static boolean drr561Available() {
        return Files.isDirectory(DRR561_GOLDEN);
    }

    static boolean drr7Available() {
        return Drr7Corpus.gate(Files.isDirectory(DRR7_GOLDEN), WrapperLadderKeepsCollapseSeatTest.class);
    }

    private static final String ESMA_UNDERLYING_ID = "drr/regulation/esma/emir/refit/trade/reports/UnderlyingIdentificationRule.java";
    private static final String MAS_UNDERLYING = "drr/regulation/mas/rewrite/trade/reports/UnderlyingRule.java";

    /** c1 — drr 5.61.0 esma UnderlyingIdentificationRule whole-file lock (the MapperC-seat carrier). */
    @Test
    @EnabledIf("drr561Available")
    void corpus_c1_esmaUnderlyingIdentificationByteIdentical() throws IOException {
        lock(ESMA_UNDERLYING_ID);
    }

    /** c2 — drr 5.61.0 mas UnderlyingRule whole-file lock (the MapperS-seat carrier, with the F1 output deref). */
    @Test
    @EnabledIf("drr561Available")
    void corpus_c2_masUnderlyingByteIdentical() throws IOException {
        lock(MAS_UNDERLYING);
    }

    /**
     * control0 — golden is the oracle (the frozen drr 5.61.0 tree, every kind): the three token populations at their
     * exact census counts — T1 the MapperC-seat lifted collapse (3 sites), T2 the MapperS-seat wrapped collapse (1),
     * T3 the value-seat deref pair's return (2) — over 6 token-bearing files; the carriers carry (1,0,0) / (0,1,0).
     */
    @Test
    @EnabledIf("drr561Available")
    void corpus_control0_goldenDrr561IsTheOracle() throws IOException {
        Map<String, int[]> g = scan(readGoldenTree(DRR561_GOLDEN));
        assertEquals(6, g.size(), "golden token-bearing files (the whole-cell control's domain)");
        assertEquals(List.of(3, 1, 2), List.of(sites(g, 0), sites(g, 1), sites(g, 2)), "golden (T1, T2, T3) sites");
        assertEquals(List.of(1, 0, 0), counts(g.get(ESMA_UNDERLYING_ID)), "golden esma carrier (T1, T2, T3)");
        assertEquals(List.of(0, 1, 0), counts(g.get(MAS_UNDERLYING)), "golden mas carrier (T1, T2, T3)");
    }

    /**
     * control1 — the FORK's WHOLE generated drr 5.61.0 cell (every kind, LAW 72): over the UNION of the files either
     * tree carries a token in (a missing side counts as all-zero, LAW 79), the per-file (T1, T2, T3) triples agree
     * FILE BY FILE — an over-fire (a value-typed seat losing its deref, a lift where golden has none) and an under-fire
     * (a carrier still deref'd in-rung) both fail here; the domain equals golden's 6.
     */
    @Test
    @EnabledIf("drr561Available")
    void corpus_control1_forkDrr561WholeCellTokensEqualGoldenFileByFile() throws IOException {
        assertNotNull(drr561Output, "drr 5.61.0 generation did not run");
        assertEquals(List.of(), drr561GenErrors, "drr 5.61.0 reported a generation error — the scan is incomplete");
        assertUnionEqual(scan(drr561Output), scan(readGoldenTree(DRR561_GOLDEN)), drr561Output.keySet(),
                "fork", "golden", KNOWN_RESIDUE_DRR561, 6);
    }

    /**
     * The named pre-existing residue of other families in this cell (LAW 73: the set, not the count): two
     * F13 (meta-deref seat) carriers whose golden carries a T3 deref-pair return the fork does not render —
     * identical before and after this law (the RED and GREEN reads both list exactly these two).
     */
    // LAW 81 re-pin (v3.1 flip seat 33, law F.A): the cftc NotionalCurrencyLeg1Rule + jfsa
    // NotionalCurrencyOfLeg1Rule rows LEFT this list - both files healed WHOLE by facet
    // blockArmWrapperHopDeref (byte-identical to golden, locked by BlockArmWrapperHopDerefSeatTest
    // corpus_c1/c2); the list is EMPTY, transcribed from this control's own print (FA-trip1.log).
    private static final List<String> KNOWN_RESIDUE_DRR561 = List.of();

    /**
     * control2 — LAW 77 route parity: the whole drr 5.61.0 cell generated through the REAL {@code IRGeneration} seams
     * ({@code -Pir-on}) carries the SAME per-file triples as the legacy-route render, file by file over the UNION.
     */
    @Test
    @EnabledIf("drr561AndIrProviderAvailable")
    void corpus_control2_irRouteDrr561TokensEqualLegacyRouteFileByFile() throws IOException {
        assertNotNull(drr561Output, "drr 5.61.0 legacy-route generation did not run");
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "5.61.0", DRR561_CELL_ROOT), new ArrayList<>());
        assertUnionEqual(scan(irOut), scan(drr561Output), irOut.keySet(), "ir", "legacy", List.of(),
                scan(drr561Output).size());
    }

    /**
     * control3 — LAW 79: the drr 7.0.0 cell, where the VALUE-typed #337 carriers live (DTCC_Leg1/2FloatingRateIndex —
     * golden T3 4, T2 2, T1 0 over 6 files): the seat must move NOTHING there, file by file over the UNION.
     */
    @Test
    @EnabledIf("drr7Available")
    void corpus_control3_forkDrr7WholeCellTokensEqualGoldenFileByFile() throws IOException {
        List<String> errs = new ArrayList<>();
        Map<String, String> out = generateCell(
                new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", DRR7_CELL_ROOT), errs);
        assertEquals(List.of(), errs, "drr 7.0.0 reported a generation error — the scan is incomplete");
        Map<String, int[]> g = scan(readGoldenTree(DRR7_GOLDEN));
        assertEquals(List.of(0, 2, 4), List.of(sites(g, 0), sites(g, 1), sites(g, 2)), "golden drr 7.0.0 (T1, T2, T3)");
        assertUnionEqual(scan(out), g, out.keySet(), "fork", "golden", List.of(), g.size());
    }

    private static List<Integer> counts(int[] c) {
        assertNotNull(c, "the file carries no token");
        return List.of(c[0], c[1], c[2]);
    }

    private static void assertUnionEqual(Map<String, int[]> a, Map<String, int[]> b,
            java.util.Set<String> emittedA, String aName, String bName, List<String> knownResidue,
            int expectedDomain) {
        List<String> mismatched = new ArrayList<>();
        List<String> notEmitted = new ArrayList<>();
        java.util.Set<String> universe = new java.util.TreeSet<>(a.keySet());
        universe.addAll(b.keySet());
        int[] zero = new int[3];
        for (String key : universe) {
            if (!emittedA.contains(key)) {
                notEmitted.add(key);
                continue;
            }
            int[] ac = a.getOrDefault(key, zero);
            int[] bc = b.getOrDefault(key, zero);
            if (!java.util.Arrays.equals(ac, bc)) {
                mismatched.add(key + " " + aName + "=" + java.util.Arrays.toString(ac)
                        + " " + bName + "=" + java.util.Arrays.toString(bc));
            }
        }
        assertEquals(knownResidue, mismatched,
                "(T1, T2, T3) differ beyond the named residue in " + mismatched.size() + " file(s)");
        assertEquals(List.of(), notEmitted,
                "token-bearing files " + bName + " carries that " + aName + " does not emit at all");
        assertEquals(expectedDomain, universe.size(),
                "the union domain must equal the oracle's token-bearing files (" + expectedDomain + ")");
    }

    // =========================================================================
    // The scan — (T1, T2, T3) per file over CODE only (comments stripped; a line walk)
    // =========================================================================

    static int[] countT28(String java) {
        int[] c = new int[3];
        for (String raw : codeOnly(java).split("\n")) {
            String s = raw.strip();
            if (s.startsWith("return MapperC.of(Collections.singletonList(MapperC.<FieldWithMeta")
                    || s.startsWith("return MapperC.of(Collections.singletonList(MapperC.<ReferenceWithMeta")) {
                c[0]++;
            }
            if (s.startsWith("return MapperS.of(MapperC.<FieldWithMeta")
                    || s.startsWith("return MapperS.of(MapperC.<ReferenceWithMeta")) {
                c[1]++;
            }
            if (s.startsWith("return ") && s.contains(" == null ? MapperS.<") && s.endsWith(".getValue());")) {
                c[2]++;
            }
        }
        return c;
    }

    private static Map<String, int[]> scan(Map<String, String> tree) {
        Map<String, int[]> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : tree.entrySet()) {
            if (!e.getValue().contains("MapperC.<") && !e.getValue().contains("== null ? MapperS.<")) {
                continue;
            }
            int[] c = countT28(e.getValue());
            if (c[0] + c[1] + c[2] > 0) {
                out.put(e.getKey(), c);
            }
        }
        return out;
    }

    private static int sites(Map<String, int[]> per, int idx) {
        int n = 0;
        for (int[] c : per.values()) {
            n += c[idx];
        }
        return n;
    }

    /** Strip line and block comments so a javadoc never counts as code. */
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
                sb.append(java, i, Math.min(j + 1, n));
                i = j + 1;
            } else if (ch == '/' && i + 1 < n && java.charAt(i + 1) == '/') {
                while (i < n && java.charAt(i) != '\n') {
                    i++;
                }
            } else if (ch == '/' && i + 1 < n && java.charAt(i + 1) == '*') {
                int end = java.indexOf("*/", i + 2);
                i = end < 0 ? n : end + 2;
            } else {
                sb.append(ch);
                i++;
            }
        }
        return sb.toString();
    }

    private static Map<String, String> readGoldenTree(Path root) throws IOException {
        Map<String, String> out = new LinkedHashMap<>();
        try (var stream = Files.walk(root)) {
            for (Path p : stream.filter(q -> q.toString().endsWith(".java")).sorted().toList()) {
                out.put(root.relativize(p).toString().replace('\\', '/'), Files.readString(p));
            }
        }
        return out;
    }

    // =========================================================================
    // Cell generation (drr 5.61.0 / 7.0.0, the legacy route; the IR route for control2)
    // =========================================================================

    private static Map<String, String> drr561Output;
    private static List<String> drr561GenErrors;

    @BeforeAll
    static void generateCells() throws IOException {
        if (drr561Available()) {
            List<String> errs = new ArrayList<>();
            drr561Output = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "5.61.0", DRR561_CELL_ROOT), errs);
            drr561GenErrors = errs;
        }
    }

    private static Map<String, String> generateCell(D11CorpusRegressionTest.CellSpec cell,
            List<String> errors) throws IOException {
        var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(),
                D11CorpusRegressionTest.emissionFilter(cell));
        var typeUtil = new JavaTypeUtil();
        var typeTranslator = new JavaTypeTranslator(typeUtil);
        var pojoGen = new ModelObjectGenerator(gm, typeTranslator, typeUtil);
        var choiceGen = new ChoiceObjectGenerator(gm, typeTranslator, typeUtil, pojoGen);
        var enumGen = new EnumGenerator(gm);
        var funcGen = new FunctionGenerator(gm, typeTranslator, typeUtil);
        var ruleGen = new RuleGenerator(gm, typeTranslator, funcGen);
        var reportGen = new ReportGenerator(gm, typeTranslator, funcGen);
        var dataRuleGen = new DataRuleGenerator(gm, typeTranslator, typeUtil);
        var labelProviderGen = new LabelProviderGenerator(
                gm, typeTranslator, new DeepFeatureCallUtil(gm::getType),
                new LabelProviderGeneratorUtil());
        Map<String, String> output = new LinkedHashMap<>();
        for (RModel model : corpus.workspace().files()) {
            if (gm.shouldGenerate(model)) {
                String version = gm.version(model);
                collect(errors, pojoGen.generateClasses(model, version, output));
                collect(errors, choiceGen.generateClasses(model, version, output));
                collect(errors, enumGen.generateClasses(model, version, output));
                collect(errors, ruleGen.generateClasses(model, version, output));
                collect(errors, reportGen.generateClasses(model, version, output));
                collect(errors, dataRuleGen.generateClasses(model, version, output));
                collect(errors, labelProviderGen.generateClasses(model, version, output));
            }
        }
        collect(errors, funcGen.generateWithErrors(output));
        return output;
    }

    /**
     * The cell through the REAL {@code IRGeneration} seams — the D11 ON ring's wiring (the flag set for the render
     * and restored after; the provider asserted present; the function seam asserted to hand back the IR-route
     * generator, so this can never silently be an OFF-route render).
     */
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
            var pojoGen = IRGeneration.modelObjectGenerator(gm, typeTranslator, typeUtil);
            var choiceGen = IRGeneration.choiceObjectGenerator(gm, typeTranslator, typeUtil, pojoGen);
            var enumGen = IRGeneration.enumGenerator(gm);
            FunctionGenerator funcGen = IRGeneration.functionGenerator(gm, typeTranslator, typeUtil);
            assertTrue(!funcGen.getClass().equals(FunctionGenerator.class),
                    "the seam must hand back the IR-route FunctionGenerator, got " + funcGen.getClass());
            var ruleGen = new RuleGenerator(gm, typeTranslator, funcGen);
            var reportGen = new ReportGenerator(gm, typeTranslator, funcGen);
            var dataRuleGen = new DataRuleGenerator(gm, typeTranslator, typeUtil);
            var labelProviderGen = new LabelProviderGenerator(
                    gm, typeTranslator, new DeepFeatureCallUtil(gm::getType),
                    new LabelProviderGeneratorUtil());
            Map<String, String> output = new LinkedHashMap<>();
            for (RModel model : corpus.workspace().files()) {
                if (gm.shouldGenerate(model)) {
                    String version = gm.version(model);
                    collect(errors, IRGeneration.generateClasses(pojoGen, model, version, output));
                    collect(errors, IRGeneration.generateClasses(choiceGen, model, version, output));
                    collect(errors, IRGeneration.generateClasses(enumGen, model, version, output));
                    collect(errors, ruleGen.generateClasses(model, version, output));
                    collect(errors, reportGen.generateClasses(model, version, output));
                    collect(errors, dataRuleGen.generateClasses(model, version, output));
                    collect(errors, labelProviderGen.generateClasses(model, version, output));
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

    private static void collect(List<String> sink, List<GenerationException> errors) {
        if (errors != null) {
            errors.forEach(e -> sink.add(e.getTargetPath() + " — " + e));
        }
    }

    private static void lock(String path) throws IOException {
        assertNotNull(drr561Output, "drr 5.61.0 generation did not run — corpus unavailable?");
        List<String> lockedErrors = drr561GenErrors.stream().filter(e -> e.contains(path)).toList();
        assertTrue(lockedErrors.isEmpty(),
                "the generator reported errors for the locked file " + path + ": " + lockedErrors);
        String generated = drr561Output.get(path);
        assertNotNull(generated, "not generated in drr 5.61.0: " + path);
        Path goldenPath = DRR561_GOLDEN.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "golden missing: " + goldenPath);
        assertEquals(normalize(Files.readString(goldenPath)), normalize(generated),
                "generated drr 5.61.0 output must byte-match golden (newline-normalized) for "
                + path + " — seat 24 F28: a wrapper-typed ladder keeps its list-literal collapse.");
    }

    // =========================================================================
    // Fixture harness
    // =========================================================================

    private static RLinkingResult linking;
    private static RModel mainModel;
    private static Map<String, String> fixtureOut;

    private static void link() throws IOException {
        if (linking == null) {
            RModel main = AstBuilder.buildFromString(MODEL, "seat24f28.rosetta");
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
            fixtureOut = render(m -> "census.seat24f28".equals(m.namespace()));
        }
        return fixtureOut;
    }

    private static Map<String, String> fixtureOutIr;

    /** The fixture's rules through the REAL {@code IRGeneration.functionGenerator} seam (the ON route). */
    private static Map<String, String> fixtureOnIrRoute() throws IOException {
        if (fixtureOutIr == null) {
            link();
            String previous = System.getProperty(IRGeneration.PROPERTY);
            System.setProperty(IRGeneration.PROPERTY, "true");
            try {
                assertNotNull(IRGeneration.providerOrNull(),
                        "the IR provider must be resolvable under -Pir-on, else this is not an ON-route render");
                GeneratorModel gm = new GeneratorModel(linking.workspace(),
                        m -> "census.seat24f28".equals(m.namespace()));
                JavaTypeUtil typeUtil = new JavaTypeUtil();
                JavaTypeTranslator tt = new JavaTypeTranslator(typeUtil);
                FunctionGenerator fg = IRGeneration.functionGenerator(gm, tt, typeUtil);
                assertTrue(!fg.getClass().equals(FunctionGenerator.class),
                        "the seam must hand back the IR-route FunctionGenerator, got " + fg.getClass());
                RuleGenerator ruleGen = new RuleGenerator(gm, tt, fg);
                Map<String, String> out = new LinkedHashMap<>();
                List<String> errors = new ArrayList<>();
                ruleGen.generateClasses(mainModel, "1.0", out)
                        .forEach(e -> errors.add(e.getTargetPath() + " — " + e));
                fg.generateWithErrors(out)
                        .forEach(e -> errors.add(e.getTargetPath() + " — " + e));
                if (!errors.isEmpty()) {
                    throw new AssertionError("fixture generation errors on the IR route: " + errors);
                }
                fixtureOutIr = out;
            } finally {
                if (previous == null) {
                    System.clearProperty(IRGeneration.PROPERTY);
                } else {
                    System.setProperty(IRGeneration.PROPERTY, previous);
                }
            }
        }
        return fixtureOutIr;
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
            throw new AssertionError("[WrapperLadderKeepsCollapseSeatTest] builtins parse"
                    + " failures: " + String.join("; ", failures));
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
        assertFalse(out.contains(token),
                "forbidden token present: " + token + "\n--- in output:\n" + out);
    }
}
