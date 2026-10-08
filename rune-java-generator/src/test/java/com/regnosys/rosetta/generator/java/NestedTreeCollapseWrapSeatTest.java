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
 * SEAT 24, law F27 — facet {@code nestedTreeCollapseWrap}: <b>a bare {@code .get()}-collapsed ONLY_ELEMENT arm of a
 * NESTED-THEN tree re-wraps {@code MapperS.of(…)} at the Mapper-returning return seat — the SAME predicate the three
 * sibling block forms consult (LAW 69)</b>. Upstream compiles every conditional arm against the lambda's Mapper return
 * type, so an item-collapsed arm takes the {@code MapperS.of(<collapse>)} wrap (golden drr 5.61.0 asic/jfsa
 * {@code SingleBarrierPriceDecimal/Monetary/CurrencyRule}: {@code return MapperS.of(MapperS.of(economicTermsForProduct
 * .evaluate(…)).<Payout>map(…)….<BigDecimal>mapC("getLevelPercentage", …).get());} inside the {@code if IsOption(…) then
 * if … then … only-element else if …} tree).
 *
 * <p><b>The defect — and the producer the probe REFUTED (LAW 72).</b> The close census charted
 * {@code CollectionHandler.isBareGetCollapse} declining TYPED arms. The seat-24 runtime probes over all 275 matrix
 * rows on BOTH routes showed that predicate never sees a typed arm (206/206 consults {@code typed=null}, every one
 * firing) and that the carriers never reach it at all: their arms are rendered by {@code appendNestedArm} (the #294
 * nested-then tree), whose {@code bareArm} test is {@code isBareEnumReference || isBareFunctionInvocation} — no
 * collapse consult, unlike its three siblings ({@code compileElselessConditionalBlock}, {@code
 * compileEffectiveElseConditionalBlock}, {@code renderLadderLevel}). The twelve nested-tree ONLY_ELEMENT arms in the
 * whole corpus are exactly the carriers ({@code typed=null endsGet=true startsMapperS=true mapperC=false}).
 *
 * <p><b>The seat.</b> {@code appendNestedArm}'s {@code bareArm} gains {@code || isBareGetCollapse(arm, armExpr, armStr)}.
 * The existing wrap, the MapperC-seat decline ({@code nb.mapperC && bareArm → decline the whole tree}, the #373
 * conservative law — mirrored from the ladder seat; no corpus carrier) and the multi-line decline apply unchanged.
 * Green-safe BY THE PROBE and by construction (a bare item return never compiled against the Mapper lambda, LAW 74).
 *
 * <p><b>RED at the pre-seat blob</b> (the head before this law's commit, this suite kept — MEASURED by the receipts
 * chain at the final head, LAW 78/82): a1, a2, a4, {@code corpus_c1}, {@code corpus_c2}, {@code corpus_control1} — 6F
 * (under {@code -Pir-on} also a3 — 7F); b1, b2, {@code control0} and {@code corpus_control3} (the whole drr 7.0.0
 * cell — UNMOVED by this law and by every mutation below) GREEN in both states; {@code control2} compares the two
 * routes and is GREEN in both states — the seat is shared. <b>LAW 66/76 mutations</b> (each applied → run → reverted
 * at the FINAL head; the failing SETS are the chain's MEASUREMENT, not the charter's prediction — receipts in the PR
 * body): (i) the collapse consult deleted → a1, a2, c1, c2, control1 + law F28's
 * {@code WrapperLadderKeepsCollapseSeatTest.a3} (the nested tree's wrapper-join arm is rendered through this same
 * consult) — 6F; a4 does NOT move (its lift reads its OWN consult, mutation iii's) and control3 does not (no carrier
 * in that cell); (ii) the consult widened to EVERY list-op collapse ({@code FIRST}/{@code LAST}, not just
 * ONLY_ELEMENT) → 0F MEASURED — the corpus has no nested-tree FIRST/LAST {@code .get()} arm, so the ONLY_ELEMENT
 * restriction is a stated decline, not a load-bearing exclusion (declared, information-free); (iii) the MapperC-seat
 * collapse lift reverted to the #373 decline (law F28's mirror at this seat) → a4 — 1F.
 */
class NestedTreeCollapseWrapSeatTest {

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
     * One {@code from} type with a function filter, a boolean function (the {@code IsOption} twin), a navigation
     * function (the {@code EconomicTermsForProduct} twin — its output's multi {@code level} leaf is what
     * {@code only-element} collapses), and one reporting rule per shape.
     */
    private static final String MODEL = """
            namespace census.seat24f27
            version "1.0.0"

            type Trig:
                level number (0..*)
                mark string (0..*)

            type Knock:
                knockIn Trig (0..1)
                knockOut Trig (0..1)

            type Instr:
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

            func IsOpt: <"IsOption twin - the bare fn-call top condition of the tree">
                inputs:
                    i Instr (0..1)
                output:
                    result boolean (1..1)
                set result:
                    i -> isOpt = True

            func KnockFor: <"EconomicTermsForProduct twin - the chain head every rung navigates from">
                inputs:
                    i Instr (0..1)
                output:
                    k Knock (0..1)
                set k:
                    i -> knock

            reporting rule A1Decimal from Instr: <"a1 - THE SingleBarrierPriceDecimal SHAPE: a nested-then tree whose inner else-if rungs return only-element collapses of multi chains">
                filter IsAllowable
                then extract
                    if IsOpt(item)
                    then if KnockFor(item) -> knockIn exists and KnockFor(item) -> knockOut is absent
                        then KnockFor(item) -> knockIn -> level only-element
                        else if KnockFor(item) -> knockIn is absent and KnockFor(item) -> knockOut exists
                        then KnockFor(item) -> knockOut -> level only-element
                    as "a1"

            reporting rule A2InnerElse from Instr: <"a2 - a nested-then tree whose inner ELSE arm (the fall-through, not an else-if) is the collapse">
                filter IsAllowable
                then extract
                    if IsOpt(item)
                    then (if KnockFor(item) -> knockIn exists
                        then KnockFor(item) -> knockIn -> level only-element
                        else KnockFor(item) -> knockOut -> level only-element)
                    as "a2"

            reporting rule A4MultiTree from Instr: <"a4 - a MULTI nested-then tree (the else arm is the multi mark leaf) with a collapse arm: the MapperC seat LIFTS the collapse to a singleton list, exactly like the ladder sibling (law F28)">
                filter IsAllowable
                then extract
                    if IsOpt(item)
                    then (if KnockFor(item) -> knockIn exists
                        then KnockFor(item) -> knockIn -> mark only-element
                        else KnockFor(item) -> knockOut -> mark)
                    as "a4"

            reporting rule B1FlatLadder from Instr: <"b1 - the FLAT ladder with collapse rungs keeps the #334 bytes (the sibling seat already wraps)">
                filter IsAllowable
                then extract
                    if KnockFor(item) -> knockIn exists
                    then KnockFor(item) -> knockIn -> level only-element
                    else if KnockFor(item) -> knockOut exists
                    then KnockFor(item) -> knockOut -> level only-element
                    as "b1"

            reporting rule B2BareFnArms from Instr: <"b2 - a nested-then tree with bare fn arms keeps its bytes (the existing bareArm wrap)">
                filter IsAllowable
                then extract
                    if IsOpt(item)
                    then (if KnockFor(item) -> knockIn exists
                        then IsAllowable
                        else IsOpt)
                    as "b2"
            """;

    private static final String KNOCK_IN_WRAPPED =
            "return MapperS.of(MapperS.of(knockFor.evaluate(item.get())).<Trig>map(\"getKnockIn\", knock -> knock.getKnockIn())"
            + ".<BigDecimal>mapC(\"getLevel\", trig -> trig.getLevel()).get());";
    private static final String KNOCK_OUT_WRAPPED =
            "return MapperS.of(MapperS.of(knockFor.evaluate(item.get())).<Trig>map(\"getKnockOut\", knock -> knock.getKnockOut())"
            + ".<BigDecimal>mapC(\"getLevel\", trig -> trig.getLevel()).get());";

    // =========================================================================
    // Part A — the seat (RED at the pre-seat blob)
    // =========================================================================

    /** a1 — the SingleBarrierPriceDecimal shape: both inner rungs' collapses re-wrap MapperS.of(…). */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_nestedTreeElseIfRungCollapsesWrap() throws IOException {
        String out = rule("A1DecimalRule.java");
        assertContains(out, "final Boolean _boolean = isOpt.evaluate(item.get());");
        assertContains(out, KNOCK_IN_WRAPPED);
        assertContains(out, KNOCK_OUT_WRAPPED);
        assertContains(out, "return MapperS.<BigDecimal>ofNull();");
        assertEquals(List.of(2, 0), List.of(countT27(out)[0], countT27(out)[1]), "(T27wrap, T27bare) in a1");
    }

    /** a2 — the inner ELSE fall-through collapse wraps too. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_nestedTreeInnerElseCollapseWraps() throws IOException {
        String out = rule("A2InnerElseRule.java");
        assertContains(out, KNOCK_IN_WRAPPED);
        assertContains(out, KNOCK_OUT_WRAPPED);
        assertEquals(List.of(2, 0), List.of(countT27(out)[0], countT27(out)[1]), "(T27wrap, T27bare) in a2");
    }

    /** a3 — LAW 77: the a1 shape through the REAL {@code IRGeneration.functionGenerator} seam ({@code -Pir-on}). */
    @Test
    @EnabledIf("builtinsAndIrProviderAvailable")
    void a3_nestedTreeCollapseWrapsOnIrRoute() throws IOException {
        String out = lookup(fixtureOnIrRoute(), "reports/A1DecimalRule.java");
        assertContains(out, KNOCK_IN_WRAPPED);
        assertContains(out, KNOCK_OUT_WRAPPED);
        assertEquals(List.of(2, 0), List.of(countT27(out)[0], countT27(out)[1]), "(T27wrap, T27bare) in a3");
    }

    /**
     * a4 — the MapperC seat: a MULTI nested-then tree's collapse arm LIFTS to upstream's item→list coercion
     * {@code MapperC.of(Collections.singletonList(<collapse>))} — the form golden witnesses at the ladder seat
     * (drr 5.61.0 esma/fca/jfsa UnderlyingIdentificationRule, law F28), mirrored here (LAW 69); the multi else arm
     * stays bare. Pre-seat the tree rendered the un-witnessed {@code MapperC.of(<collapse>)}.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a4_multiNestedTreeCollapseLiftsLikeTheLadder() throws IOException {
        String out = rule("A4MultiTreeRule.java");
        assertContains(out, "return MapperC.of(Collections.singletonList(MapperS.of(knockFor.evaluate(item.get())).<Trig>map(\"getKnockIn\", knock -> knock.getKnockIn()).<String>mapC(\"getMark\", trig -> trig.getMark()).get()));");
        assertContains(out, "return MapperS.of(knockFor.evaluate(item.get())).<Trig>map(\"getKnockOut\", knock -> knock.getKnockOut()).<String>mapC(\"getMark\", trig -> trig.getMark());");
        assertNotContains(out, "return MapperC.of(MapperS.of(");
        assertEquals(List.of(0, 0), List.of(countT27(out)[0], countT27(out)[1]), "(T27wrap, T27bare) in a4");
    }

    // =========================================================================
    // Part B — placement pins (GREEN in BOTH states)
    // =========================================================================

    /** b1 — the FLAT ladder's collapse rungs keep the #334 bytes (the sibling seat already wraps). */
    @Test
    @EnabledIf("builtinsAvailable")
    void b1_flatLadderCollapseRungsKeepSiblingBytes() throws IOException {
        String out = rule("B1FlatLadderRule.java");
        assertContains(out, KNOCK_IN_WRAPPED);
        assertContains(out, KNOCK_OUT_WRAPPED);
        assertEquals(List.of(2, 0), List.of(countT27(out)[0], countT27(out)[1]), "(T27wrap, T27bare) in b1");
    }

    /** b2 — a nested-then tree with bare fn arms keeps the existing bareArm wrap bytes. */
    @Test
    @EnabledIf("builtinsAvailable")
    void b2_nestedTreeBareFnArmsUnchanged() throws IOException {
        String out = rule("B2BareFnArmsRule.java");
        assertContains(out, "return MapperS.of(isAllowable.evaluate(item.get()));");
        assertContains(out, "return MapperS.of(isOpt.evaluate(item.get()));");
        assertEquals(List.of(0, 0), List.of(countT27(out)[0], countT27(out)[1]), "(T27wrap, T27bare) in b2");
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
        return Drr7Corpus.gate(Files.isDirectory(DRR7_GOLDEN), NestedTreeCollapseWrapSeatTest.class);
    }

    private static final String ASIC_DECIMAL = "drr/regulation/asic/rewrite/trade/reports/SingleBarrierPriceDecimalRule.java";
    private static final String ASIC_MONETARY = "drr/regulation/asic/rewrite/trade/reports/SingleBarrierPriceMonetaryRule.java";

    /** c1 — drr 5.61.0 asic SingleBarrierPriceDecimalRule whole-file lock (the carrier). */
    @Test
    @EnabledIf("drr561Available")
    void corpus_c1_asicSingleBarrierPriceDecimalByteIdentical() throws IOException {
        lock(ASIC_DECIMAL);
    }

    /** c2 — drr 5.61.0 asic SingleBarrierPriceMonetaryRule whole-file lock (the second carrier basename). */
    @Test
    @EnabledIf("drr561Available")
    void corpus_c2_asicSingleBarrierPriceMonetaryByteIdentical() throws IOException {
        lock(ASIC_MONETARY);
    }

    /**
     * control0 — golden is the oracle (the frozen drr 5.61.0 tree, every kind): the two token populations at their
     * exact census counts — T1 a wrapped collapse return {@code return MapperS.of(MapperS.of(….get());}, 30 sites, and
     * T2 a BARE collapse return {@code return MapperS.of(…).get();}, ZERO sites — over 24 token-bearing files; the two
     * carriers carry (2, 0) each.
     */
    @Test
    @EnabledIf("drr561Available")
    void corpus_control0_goldenDrr561IsTheOracle() throws IOException {
        Map<String, int[]> g = scan(readGoldenTree(DRR561_GOLDEN));
        assertEquals(24, g.size(), "golden token-bearing files (the whole-cell control's domain)");
        assertEquals(30, sites(g, 0), "golden T1 wrapped collapse returns");
        assertEquals(0, sites(g, 1), "golden T2 bare collapse returns");
        for (String carrier : List.of(ASIC_DECIMAL, ASIC_MONETARY)) {
            int[] c = g.get(carrier);
            assertNotNull(c, "golden carrier carries a token: " + carrier);
            assertEquals(List.of(2, 0), List.of(c[0], c[1]), "golden carrier (T1, T2): " + carrier);
        }
    }

    /**
     * control1 — the FORK's WHOLE generated drr 5.61.0 cell (every kind, LAW 72): over the UNION of the files either
     * tree carries a token in (a missing side counts as all-zero, LAW 79), the per-file (T1, T2) pairs agree FILE BY
     * FILE — an over-fire (a wrap where golden has none) and an under-fire (a carrier still bare) both fail here; the
     * domain equals golden's 24.
     */
    @Test
    @EnabledIf("drr561Available")
    void corpus_control1_forkDrr561WholeCellCollapseReturnsEqualGoldenFileByFile() throws IOException {
        assertNotNull(drr561Output, "drr 5.61.0 generation did not run");
        assertEquals(List.of(), drr561GenErrors, "drr 5.61.0 reported a generation error — the scan is incomplete");
        assertUnionEqual(scan(drr561Output), scan(readGoldenTree(DRR561_GOLDEN)), drr561Output.keySet(),
                "fork", "golden", KNOWN_RESIDUE_DRR561, 24);
    }

    /** The named pre-existing residue of other families in this cell (LAW 73: the set, not the count). */
    private static final List<String> KNOWN_RESIDUE_DRR561 = List.of();

    /**
     * control2 — LAW 77 route parity: the whole drr 5.61.0 cell generated through the REAL {@code IRGeneration} seams
     * ({@code -Pir-on}) carries the SAME per-file (T1, T2) pairs as the legacy-route render, file by file over the UNION.
     */
    @Test
    @EnabledIf("drr561AndIrProviderAvailable")
    void corpus_control2_irRouteDrr561CollapseReturnsEqualLegacyRouteFileByFile() throws IOException {
        assertNotNull(drr561Output, "drr 5.61.0 legacy-route generation did not run");
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "5.61.0", DRR561_CELL_ROOT), new ArrayList<>());
        assertUnionEqual(scan(irOut), scan(drr561Output), irOut.keySet(), "ir", "legacy", List.of(),
                scan(drr561Output).size());
    }

    /**
     * control3 — LAW 79: the predicate reaches every cell, so the fork's whole drr 7.0.0 cell is compared with its golden
     * the same way (golden: 8 wrapped / 0 bare over 6 files) — the seat must move NOTHING there.
     */
    @Test
    @EnabledIf("drr7Available")
    void corpus_control3_forkDrr7WholeCellCollapseReturnsEqualGoldenFileByFile() throws IOException {
        List<String> errs = new ArrayList<>();
        Map<String, String> out = generateCell(
                new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", DRR7_CELL_ROOT), errs);
        assertEquals(List.of(), errs, "drr 7.0.0 reported a generation error — the scan is incomplete");
        Map<String, int[]> g = scan(readGoldenTree(DRR7_GOLDEN));
        assertEquals(8, sites(g, 0), "golden drr 7.0.0 T1 wrapped collapse returns");
        assertEquals(0, sites(g, 1), "golden drr 7.0.0 T2 bare collapse returns");
        assertUnionEqual(scan(out), g, out.keySet(), "fork", "golden", List.of(), g.size());
    }

    private static void assertUnionEqual(Map<String, int[]> a, Map<String, int[]> b,
            java.util.Set<String> emittedA, String aName, String bName, List<String> knownResidue,
            int expectedDomain) {
        List<String> mismatched = new ArrayList<>();
        List<String> notEmitted = new ArrayList<>();
        java.util.Set<String> universe = new java.util.TreeSet<>(a.keySet());
        universe.addAll(b.keySet());
        int[] zero = new int[2];
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
                "(T1, T2) differ beyond the named residue in " + mismatched.size() + " file(s)");
        assertEquals(List.of(), notEmitted,
                "token-bearing files " + bName + " carries that " + aName + " does not emit at all");
        assertEquals(expectedDomain, universe.size(),
                "the union domain must equal the oracle's token-bearing files (" + expectedDomain + ")");
    }

    // =========================================================================
    // The scan — (T1, T2) per file over CODE only (comments stripped; a line walk)
    // =========================================================================

    /** (T1 wrapped collapse returns, T2 bare collapse returns) over the code lines of one file. */
    static int[] countT27(String java) {
        int[] c = new int[2];
        for (String raw : codeOnly(java).split("\n")) {
            String s = raw.strip();
            if (s.startsWith("return MapperS.of(MapperS.of(") && s.endsWith(".get());")) {
                c[0]++;
            } else if (s.startsWith("return MapperS.of(") && s.endsWith(".get();")) {
                c[1]++;
            }
        }
        return c;
    }

    private static Map<String, int[]> scan(Map<String, String> tree) {
        Map<String, int[]> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : tree.entrySet()) {
            if (!e.getValue().contains(".get()")) {
                continue;
            }
            int[] c = countT27(e.getValue());
            if (c[0] + c[1] > 0) {
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
                + path + " — seat 24 F27: the nested-tree collapse arm re-wraps MapperS.of(…).");
    }

    // =========================================================================
    // Fixture harness
    // =========================================================================

    private static RLinkingResult linking;
    private static RModel mainModel;
    private static Map<String, String> fixtureOut;

    private static void link() throws IOException {
        if (linking == null) {
            RModel main = AstBuilder.buildFromString(MODEL, "seat24f27.rosetta");
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
            fixtureOut = render(m -> "census.seat24f27".equals(m.namespace()));
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
                        m -> "census.seat24f27".equals(m.namespace()));
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
            throw new AssertionError("[NestedTreeCollapseWrapSeatTest] builtins parse"
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
