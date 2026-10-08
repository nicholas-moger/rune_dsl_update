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
 * SEAT 23, law F20 — facet {@code switchConversionArmGet}: <b>a CONVERSION arm of a basic-type {@code switch}
 * at the SET seat collapses {@code .get()}</b> — upstream compiles every case result against the output's item
 * type ({@code FunctionGenerator.assign} → {@code TypeCoercionService}: a {@code Mapper}-valued arm takes the
 * item {@code .get()}), so golden drr 7.x {@code MapRegimeNameEnum} writes
 * {@code regimeNameEnum = MapperS.of(reportingRegimeName).<String>map("getValue", …).checkedMap("to-enum",
 * RegimeNameEnum::fromDisplayName, IllegalArgumentException.class).get();} at its {@code default} arm.
 *
 * <p><b>The defect.</b> {@code FunctionExpressionRenderer.renderSwitchAssignment} splices every non-literal
 * arm VERBATIM (the #149 enum-value family's bytes), and a conversion ({@code ConversionHandler}) compiles to a
 * type-less, wrap-factory-less {@code JavaExpression} — so the {@code checkedMap} chain was assigned to the bare
 * enum output without {@code .get()}: 4 whole-file drr 7.0–7.3 FUNCTION carriers, every one NON-COMPILING
 * (a {@code MapperS<RegimeNameEnum>} into a {@code RegimeNameEnum} — LAW 74).
 *
 * <p><b>The seat (ONE predicate, BOTH arms — LAW 69 within the seat).</b>
 * {@code HandlerHelper.switchArmTakesItemCoercion(arm, rendered)} — a render-truth arbiter (the #371 law): an
 * arm whose rendered form is a NAME (an enum constant {@code RegimeEnum.CSA}, the #149 family's 26,059 green
 * arms) is already the item and splices verbatim; an int literal keeps mechanism 3's bare unwrap; an empty
 * default keeps {@code null}; EVERY OTHER arm is Mapper-valued by construction (a conversion chain, a
 * wrap-factory {@code MapperS.of(<fn>.evaluate(…))} invocation, a navigation, a wrapped literal) and takes the
 * standard {@code unwrapForAssignment} — a whole {@code MapperS.of(…)} wrap strips to its item, a chain
 * appends {@code .get()}. Consulted for the {@code default} arm AND every case arm.
 *
 * <p><b>LAW 75 — measured before the seat over all 275 matrix rows, BOTH routes (the seat-23 runtime probe):</b>
 * 27,380 arms reach this seat corpus-wide — 26,059 enum-value cases (bare names), 1,241 {@code default empty}
 * ({@code null}), 72 int literals (cdm {@code MapCommodityClassificationOrdinal}, Integer output — green),
 * 4 resolved enum refs (green) and exactly 4 {@code RConversionExpr} default arms: the carriers. No other
 * non-name / non-literal arm exists at this seat, so the predicate moves nothing green BY MEASUREMENT; the
 * whole-cell control below is the receipt, not the probe.
 *
 * <p><b>RED at the pre-seat blob</b> ({@code rune-java-generator/src/main} at {@code 6caba807} — this suite
 * kept): a1–a3, a5, a6, b4, {@code corpus_c1}, {@code corpus_control1} (and under {@code -Pir-on} also
 * {@code a4}; {@code control2} compares the two routes and is GREEN in both states — the seat is shared);
 * b1, b3 + {@code control0} GREEN in both states. <b>LAW 66/76 mutations</b> (each applied → run → reverted
 * at the FINAL head, LAW 78; receipts in the PR body): (i) the consult deleted at both arms → a1–a3, a5, a6,
 * b4 + c1 + control1 (MEASURED 8F); (ii) the name-shape exclusion dropped (every non-literal arm takes the
 * unwrap) → NOTHING moves (MEASURED 0F over all four seat suites incl. control1/control2, at the seat's
 * receipts chain): {@code unwrapForAssignment}'s own Phase-X1 guard (the producer's enum-constant witness,
 * {@code HandlerHelper.isBareEnumConstant} since PR #611) already returns a bare enum constant UNCHANGED, and every NAME-rendered arm in the
 * corpus is one (the 26,059 enum-value cases), so the exclusion is a stated decline for the corpus-unwitnessed
 * UNDOTTED name (which that guard would NOT protect), not a load-bearing gate — information-free, RETIRED from
 * the chain after its one measured run; (iii) the default-arm consult deleted only → a1, a3, a5, a6, b4 + c1 +
 * control1 move, a2 stays (isolating: the case-arm consult alone is not the law; MEASURED 7F). The seat is the
 * shared legacy renderer on BOTH routes (probe: 27,380 = 27,380), so {@code control2} is the LAW-77 receipt and
 * there is no IR-route mutation (stated).
 */
class SwitchConversionArmGetSeatTest {

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

    static boolean drr7AndIrProviderAvailable() {
        return drr7Available() && irProviderOnClasspath();
    }

    /**
     * One enum, one input type, and one function per arm shape the seat must tell apart: the
     * {@code MapRegimeNameEnum} shape (a to-enum DEFAULT arm), a to-enum CASE arm, a to-int default, and the
     * enum-value / empty-default / int-literal / fn-call negatives.
     */
    private static final String MODEL = """
            namespace census.seat23f20
            version "1.0.0"

            enum RegimeEnum:
                CSA
                HKMA
                OTHER

            type Probe:
                value string (0..1)
                code string (0..1)

            func PickRegime: <"a fn whose call is a DEFAULT arm (a5)">
                inputs:
                    p Probe (0..1)
                output:
                    result RegimeEnum (0..1)
                set result:
                    RegimeEnum -> OTHER

            func A1ToEnumDefault: <"a1 - THE MapRegimeNameEnum SHAPE: a string-keyed switch whose default arm is a to-enum conversion">
                inputs:
                    p Probe (0..1)
                output:
                    regime RegimeEnum (0..1)
                set regime:
                    p -> value switch
                        "CA.Rule.91-507" then CSA,
                        "HKTR" then HKMA,
                        default p -> value to-enum RegimeEnum

            func A2ToEnumCaseArm: <"a2 - a to-enum conversion at a CASE arm">
                inputs:
                    p Probe (0..1)
                output:
                    regime RegimeEnum (0..1)
                set regime:
                    p -> value switch
                        "CA" then p -> code to-enum RegimeEnum,
                        default OTHER

            func A3ToIntDefault: <"a3 - a to-int conversion default on an int output">
                inputs:
                    p Probe (0..1)
                output:
                    n int (0..1)
                set n:
                    p -> value switch
                        "ONE" then 1,
                        "TWO" then 2,
                        default p -> code to-int

            func B1EnumArmsStayBare: <"b1/b2 - enum-value case arms stay bare names; default empty stays null">
                inputs:
                    p Probe (0..1)
                output:
                    regime RegimeEnum (0..1)
                set regime:
                    p -> value switch
                        "CA" then CSA,
                        "HK" then HKMA,
                        default empty

            func B3IntLiteralArmsStayBare: <"b3 - MapCommodityClassificationOrdinal: int-literal arms on an Integer output stay bare">
                inputs:
                    p Probe (0..1)
                output:
                    n int (0..1)
                set n:
                    p -> value switch
                        "ONE" then 1,
                        "TWO" then 2,
                        default empty

            func A5FnCallDefaultStripsWrap: <"a5 - a fn-call default arm is the bare invocation item: the wrap-factory MapperS.of(<fn>.evaluate(..)) strips (never .get())">
                inputs:
                    p Probe (0..1)
                output:
                    regime RegimeEnum (0..1)
                set regime:
                    p -> value switch
                        "CA" then CSA,
                        default PickRegime(p)

            func A6StringLiteralArmStripsWrap: <"a6 - a string-literal arm on a string output is the bare literal item (the wrap strips)">
                inputs:
                    p Probe (0..1)
                output:
                    s string (0..1)
                set s:
                    p -> value switch
                        "CA" then "canada",
                        default "elsewhere"

            func B4BareInputArmStripsWrap: <"b4 - a bare INPUT as an arm (not an enum constant) is an item: its wrap strips to the bare name">
                inputs:
                    p Probe (0..1)
                    fallback RegimeEnum (0..1)
                output:
                    regime RegimeEnum (0..1)
                set regime:
                    p -> value switch
                        "CA" then CSA,
                        default fallback
            """;

    private static final String TO_ENUM_TAIL =
            ".checkedMap(\"to-enum\", RegimeEnum::fromDisplayName, IllegalArgumentException.class)";

    // =========================================================================
    // Part A — the seat (RED at the pre-seat blob)
    // =========================================================================

    /** a1 — the MapRegimeNameEnum shape: the to-enum DEFAULT arm collapses .get(). */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_toEnumDefaultArmCollapsesGet() throws IOException {
        String out = function("A1ToEnumDefault.java");
        assertContains(out, "regime = RegimeEnum.CSA;");
        assertContains(out, "regime = RegimeEnum.HKMA;");
        assertContains(out, TO_ENUM_TAIL + ".get();");
        assertNotContains(out, TO_ENUM_TAIL + ";");
    }

    /** a2 — a to-enum conversion at a CASE arm collapses .get() too (the same predicate at both arms). */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_toEnumCaseArmCollapsesGet() throws IOException {
        String out = function("A2ToEnumCaseArm.java");
        assertContains(out, "} else if (areEqual(switchArgument, MapperS.of(\"CA\"), CardinalityOperator.All).get()) {");
        assertContains(out, "probe -> probe.getCode())" + TO_ENUM_TAIL + ".get();");
        assertContains(out, "regime = RegimeEnum.OTHER;");
        assertNotContains(out, TO_ENUM_TAIL + ";");
    }

    /** a3 — a to-int default on an int output collapses .get() (the conversion kind, not the enum target). */
    @Test
    @EnabledIf("builtinsAvailable")
    void a3_toIntDefaultArmCollapsesGet() throws IOException {
        String out = function("A3ToIntDefault.java");
        assertContains(out, "n = 1;");
        assertContains(out, "n = 2;");
        assertContains(out, ".checkedMap(\"to-int\", Integer::parseInt, NumberFormatException.class).get();");
        assertNotContains(out, "NumberFormatException.class);");
    }

    /** a5 — a fn-call default arm: the wrap-factory {@code MapperS.of(<fn>.evaluate(p))} strips to the bare invocation item. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a5_fnCallDefaultArmStripsItsWrap() throws IOException {
        String out = function("A5FnCallDefaultStripsWrap.java");
        assertContains(out, "regime = pickRegime.evaluate(p);");
        assertNotContains(out, "MapperS.of(pickRegime.evaluate(p))");
        assertNotContains(out, "evaluate(p).get()");
    }

    /** a6 — a string-literal arm on a string output: the wrap strips to the bare literal item. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a6_stringLiteralArmStripsItsWrap() throws IOException {
        String out = function("A6StringLiteralArmStripsWrap.java");
        assertContains(out, "s = \"canada\";");
        assertContains(out, "s = \"elsewhere\";");
        assertNotContains(out, "MapperS.of(\"canada\")");
        assertNotContains(out, "MapperS.of(\"elsewhere\")");
    }

    /**
     * a4 — LAW 77: the a1 shape rendered through the REAL {@code IRGeneration.functionGenerator} seam
     * ({@code -Pir-on}); the SET seat is the shared legacy renderer, so the bytes agree by construction.
     */
    @Test
    @EnabledIf("builtinsAndIrProviderAvailable")
    void a4_toEnumDefaultArmOnIrRoute() throws IOException {
        String out = lookup(fixtureOnIrRoute(), "functions/A1ToEnumDefault.java");
        assertContains(out, TO_ENUM_TAIL + ".get();");
        assertNotContains(out, TO_ENUM_TAIL + ";");
    }

    // =========================================================================
    // Part B — placement pins b1/b3 (GREEN in BOTH states) + b4, the wrap-strip witness (RED pre-seat)
    // =========================================================================

    /** b1/b2 — enum-value case arms stay bare names; `default empty` stays `= null;`. */
    @Test
    @EnabledIf("builtinsAvailable")
    void b1_enumArmsStayBareAndEmptyDefaultStaysNull() throws IOException {
        String out = function("B1EnumArmsStayBare.java");
        assertContains(out, "regime = RegimeEnum.CSA;");
        assertContains(out, "regime = RegimeEnum.HKMA;");
        assertContains(out, "} else {\n\t\t\t\tregime = null;");
        assertNotContains(out, "RegimeEnum.CSA.get()");
        assertNotContains(out, "RegimeEnum.HKMA.get()");
    }

    /** b3 — the MapCommodityClassificationOrdinal shape: int-literal arms on an Integer output stay bare. */
    @Test
    @EnabledIf("builtinsAvailable")
    void b3_intLiteralArmsStayBare() throws IOException {
        String out = function("B3IntLiteralArmsStayBare.java");
        assertContains(out, "n = 1;");
        assertContains(out, "n = 2;");
        assertContains(out, "} else {\n\t\t\t\tn = null;");
        assertNotContains(out, ".get();\n\t\t\t} else");
    }

    /**
     * b4 — a bare INPUT as an arm (not an enum constant) is an item too: its wrap-factory
     * {@code MapperS.of(fallback)} strips to the bare name — the render-truth arbiter sees a non-name render,
     * the unwrap sees a whole wrap. RED pre-seat (the wrap spliced verbatim), listed with the a-tests.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b4_bareInputArmStripsItsWrap() throws IOException {
        String out = function("B4BareInputArmStripsWrap.java");
        assertContains(out, "regime = fallback;");
        assertNotContains(out, "MapperS.of(fallback)");
        assertNotContains(out, "fallback.get()");
    }

    // =========================================================================
    // Part C — the corpus (drr 7.0.0: the carrier + the whole-cell control)
    // =========================================================================

    private static final Path DRR7_CELL_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path DRR7_GOLDEN = DRR7_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    static boolean drr7Available() {
        return Drr7Corpus.gate(Files.isDirectory(DRR7_GOLDEN), SwitchConversionArmGetSeatTest.class);
    }

    private static final String MAP_REGIME_NAME_ENUM =
            "drr/ingest/fpml/recordkeeping/reportableinfo/functions/MapRegimeNameEnum.java";

    /** c1 — drr 7.0.0 MapRegimeNameEnum whole-file lock (the carrier). */
    @Test
    @EnabledIf("drr7Available")
    void corpus_c1_mapRegimeNameEnumByteIdentical() throws IOException {
        lock(MAP_REGIME_NAME_ENUM);
    }

    /**
     * control0 — golden is the oracle (the frozen drr 7.0.0 tree, every kind): the basic-type switch ladder
     * arm shapes at their exact census counts — 107 ladder-bearing files, arms enum 2,173 / lit 6 / null 212
     * / get 1 / other 2 (censused by {@code target/seat23-instruments/f20-golden-census.py}); the carrier
     * carries the ONE {@code .get()} arm.
     */
    @Test
    @EnabledIf("drr7Available")
    void corpus_control0_goldenDrr7IsTheOracle() throws IOException {
        Map<String, int[]> g = scanLadderArms(readGoldenTree(DRR7_GOLDEN));
        assertEquals(107, g.size(), "golden ladder-bearing files (the whole-cell control's domain)");
        int[] tot = totals(g);
        assertEquals(List.of(2173, 6, 212, 1, 2), List.of(tot[0], tot[1], tot[2], tot[3], tot[4]),
                "golden drr 7.0.0 arm shapes (enum, lit, null, get, other)");
        int[] c = g.get(MAP_REGIME_NAME_ENUM);
        assertNotNull(c, "golden carrier carries a ladder: " + MAP_REGIME_NAME_ENUM);
        assertEquals(1, c[3], "golden carrier carries the .get() arm");
    }

    /**
     * control1 — the FORK's WHOLE generated drr 7.0.0 cell (every kind, LAW 72): over the UNION of the files
     * either tree carries a ladder in (a missing side counts as all-zero, LAW 79), the per-file
     * (enum, lit, null, get, other) tuples agree FILE BY FILE — an over-fire (an enum arm turned
     * {@code .get()}) and an under-fire (the carrier still bare) both fail here; the domain equals golden's
     * 107; files golden carries that the fork does not emit at all are named, never silently skipped.
     */
    @Test
    @EnabledIf("drr7Available")
    void corpus_control1_forkDrr7WholeCellLadderArmShapesEqualGoldenFileByFile() throws IOException {
        assertNotNull(drr7Output, "drr 7.0.0 generation did not run");
        // FAIL-CLOSED: the cell is error-free since this seat, so ANY generation error means a file
        // is missing from the scan for an unknown reason and the whole-cell contract cannot be asserted.
        assertEquals(List.of(), drr7GenErrors, "drr 7.0.0 reported a generation error — the scan is incomplete");
        assertUnionEqual(scanLadderArms(drr7Output), scanLadderArms(readGoldenTree(DRR7_GOLDEN)),
                drr7Output.keySet(), "fork", "golden", KNOWN_RESIDUE_DRR7, 107);
    }

    /**
     * The named PRE-EXISTING residue of ANOTHER family in this cell — the per-file tuple differences between
     * the fork's whole drr 7.0.0 cell and golden AFTER the seat, pinned EXACTLY (LAW 73: the set, not the count).
     * <b>EMPTY since seat 31</b>: {@code UnderlierBasketIdentifier} (was {@code fork=[0, 0, 0, 0, 0]
     * golden=[0, 0, 1, 0, 2]}) was close-census family F15(a) — golden renders its CHOICE switch as the #394
     * option-nav ladder under a {@code switchArgument} local ({@code null} 1 + two {@code .map(…)} arms), the
     * fork the {@code instanceof} form with no such local, so the fork's tuple was all-zero — and seat-31 law
     * 4a ({@code choiceOptionNavLadderDeepHop}) healed it WHOLE in all four drr 7.x cells, so the fork's tuple
     * now equals golden's. Golden's tuple is non-zero, so the file stays inside the union domain (107 UNMOVED).
     * RE-MEASURED at the seat-31 chain head {@code f2a4d5c0}: transcribed from control1's own failing print
     * (LAW 81), {@code but was: <[]>}. Any entry that ENTERS this list is a regression; the carrier may NEVER
     * appear here.
     */
    private static final List<String> KNOWN_RESIDUE_DRR7 = List.of();

    /**
     * control2 — LAW 77 route parity: the whole drr 7.0.0 cell generated through the REAL
     * {@code IRGeneration} seams ({@code -Pir-on}) carries the SAME per-file tuples as the legacy-route
     * render, file by file over the UNION.
     */
    @Test
    @EnabledIf("drr7AndIrProviderAvailable")
    void corpus_control2_irRouteDrr7LadderArmShapesEqualLegacyRouteFileByFile() throws IOException {
        assertNotNull(drr7Output, "drr 7.0.0 legacy-route generation did not run");
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", DRR7_CELL_ROOT), new ArrayList<>());
        // The route twin's domain is the LEGACY render's own (106 = golden's 107 minus the one named F15
        // residue neither route emits a ladder for); the fork-vs-golden control above pins golden's 107.
        assertUnionEqual(scanLadderArms(irOut), scanLadderArms(drr7Output), irOut.keySet(), "ir", "legacy",
                List.of(), scanLadderArms(drr7Output).size());
    }

    private static void assertUnionEqual(Map<String, int[]> a, Map<String, int[]> b,
            java.util.Set<String> emittedA, String aName, String bName, List<String> knownResidue,
            int expectedDomain) {
        List<String> mismatched = new ArrayList<>();
        List<String> notEmitted = new ArrayList<>();
        java.util.Set<String> universe = new java.util.TreeSet<>(a.keySet());
        universe.addAll(b.keySet());
        int[] zero = new int[5];
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
                "(enum, lit, null, get, other) tuples differ beyond the named residue in "
                + mismatched.size() + " file(s)");
        assertEquals(List.of(), notEmitted,
                "ladder-bearing files " + bName + " carries that " + aName + " does not emit at all");
        assertEquals(expectedDomain, universe.size(),
                "the union domain must equal the oracle's ladder-bearing files (" + expectedDomain + ")");
    }

    // =========================================================================
    // The scan — the basic-type switch ladder's arm shapes per file (a line walk; comments stripped)
    // =========================================================================

    private static Map<String, int[]> scanLadderArms(Map<String, String> tree) {
        Map<String, int[]> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : tree.entrySet()) {
            if (!e.getValue().contains("switchArgument")) {
                continue;
            }
            int[] c = ladderArms(codeOnly(e.getValue()));
            if (c[0] + c[1] + c[2] + c[3] + c[4] > 0) {
                out.put(e.getKey(), c);
            }
        }
        return out;
    }

    /** (enum, lit, null, get, other) over every arm of every {@code switchArgument} ladder in {@code code}. */
    private static int[] ladderArms(String code) {
        int[] c = new int[5];
        boolean inLadder = false;
        boolean expectArm = false;
        for (String raw : code.split("\n")) {
            String s = raw.strip();
            if (!inLadder) {
                if (s.startsWith("if (switchArgument.get() == null) {")) {
                    inLadder = true;
                    expectArm = true;
                }
                continue;
            }
            if (expectArm) {
                expectArm = false;
                int eq = s.indexOf(" = ");
                if (eq > 0 && s.endsWith(";")) {
                    c[classifyArm(s.substring(eq + 3, s.length() - 1))]++;
                } else {
                    c[4]++;
                }
                continue;
            }
            if (s.startsWith("} else")) {
                expectArm = true;
                continue;
            }
            if (s.equals("}")) {
                inLadder = false;
            }
        }
        return c;
    }

    private static int classifyArm(String rhs) {
        if (rhs.equals("null")) {
            return 2;
        }
        if (rhs.endsWith(".get()")) {
            return 3;
        }
        if (rhs.equals("true") || rhs.equals("false")
                || (rhs.startsWith("\"") && rhs.endsWith("\""))
                || (rhs.startsWith("BigDecimal.valueOf(") && rhs.endsWith(")"))) {
            return 1;
        }
        boolean nameChars = true;
        for (int i = 0; i < rhs.length(); i++) {
            char ch = rhs.charAt(i);
            if (!(Character.isLetterOrDigit(ch) || ch == '_' || ch == '.' || (i == 0 && ch == '-'))) {
                nameChars = false;
                break;
            }
        }
        if (nameChars) {
            return Character.isDigit(rhs.charAt(0)) || rhs.charAt(0) == '-' ? 1 : 0;
        }
        return 4;
    }

    private static int[] totals(Map<String, int[]> per) {
        int[] t = new int[5];
        for (int[] c : per.values()) {
            for (int i = 0; i < 5; i++) {
                t[i] += c[i];
            }
        }
        return t;
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
    // Cell generation (drr 7.0.0, the legacy route; the IR route for control2)
    // =========================================================================

    private static Map<String, String> drr7Output;
    private static List<String> drr7GenErrors;

    @BeforeAll
    static void generateCells() throws IOException {
        if (drr7Available()) {
            List<String> errs = new ArrayList<>();
            drr7Output = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", DRR7_CELL_ROOT), errs);
            drr7GenErrors = errs;
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
     * The cell through the REAL {@code IRGeneration} seams — the D11 ON ring's wiring (the flag set for
     * the render and restored after; the provider asserted present; the function seam asserted to hand
     * back the IR-route generator, so this can never silently be an OFF-route render).
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
        assertNotNull(drr7Output, "drr 7.0.0 generation did not run — corpus unavailable?");
        List<String> lockedErrors = drr7GenErrors.stream().filter(e -> e.contains(path)).toList();
        assertTrue(lockedErrors.isEmpty(),
                "the generator reported errors for the locked file " + path + ": " + lockedErrors);
        String generated = drr7Output.get(path);
        assertNotNull(generated, "not generated in drr 7.0.0: " + path);
        Path goldenPath = DRR7_GOLDEN.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "golden missing: " + goldenPath);
        assertEquals(normalize(Files.readString(goldenPath)), normalize(generated),
                "generated drr 7.0.0 output must byte-match golden (newline-normalized) for "
                + path + " — seat 23 F20: the conversion switch arm collapses .get().");
    }

    // =========================================================================
    // Fixture harness
    // =========================================================================

    private static RLinkingResult linking;
    private static RModel mainModel;
    private static Map<String, String> fixtureOut;

    private static void link() throws IOException {
        if (linking == null) {
            RModel main = AstBuilder.buildFromString(MODEL, "seat23f20.rosetta");
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
        Map<String, String> out = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        new EnumGenerator(gm).generateClasses(mainModel, "1.0", out)
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
            fixtureOut = render(m -> "census.seat23f20".equals(m.namespace()));
        }
        return fixtureOut;
    }

    private static Map<String, String> fixtureOutIr;

    /** The fixture's functions through the REAL {@code IRGeneration.functionGenerator} seam (the ON route). */
    private static Map<String, String> fixtureOnIrRoute() throws IOException {
        if (fixtureOutIr == null) {
            link();
            String previous = System.getProperty(IRGeneration.PROPERTY);
            System.setProperty(IRGeneration.PROPERTY, "true");
            try {
                assertNotNull(IRGeneration.providerOrNull(),
                        "the IR provider must be resolvable under -Pir-on, else this is not an ON-route render");
                GeneratorModel gm = new GeneratorModel(linking.workspace(),
                        m -> "census.seat23f20".equals(m.namespace()));
                JavaTypeUtil typeUtil = new JavaTypeUtil();
                JavaTypeTranslator tt = new JavaTypeTranslator(typeUtil);
                FunctionGenerator fg = IRGeneration.functionGenerator(gm, tt, typeUtil);
                assertTrue(!fg.getClass().equals(FunctionGenerator.class),
                        "the seam must hand back the IR-route FunctionGenerator, got " + fg.getClass());
                Map<String, String> out = new LinkedHashMap<>();
                List<String> errors = new ArrayList<>();
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

    private static String function(String fileName) throws IOException {
        return lookup(fixture(), "functions/" + fileName);
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
            throw new AssertionError("[SwitchConversionArmGetSeatTest] builtins parse"
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
