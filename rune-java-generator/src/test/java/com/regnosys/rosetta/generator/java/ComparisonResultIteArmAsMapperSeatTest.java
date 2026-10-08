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
 * SEAT 23, law F21 — facet {@code comparisonResultIteArmAsMapper}: <b>a ComparisonResult arm assigned into a
 * SINGLE {@code MapperS<Boolean>} if-then-else local coerces {@code .asMapper()} — in EVERY context</b>. Upstream's
 * {@code addCoercions} converts the arm to the local's type at the assignment ({@code ComparisonResult}
 * implements {@code Mapper<Boolean>}, never {@code MapperS}), so golden drr 7.x emir {@code PTRRRule} writes
 * {@code ifThenElseResult = ComparisonResult.ofNullSafe(MapperS.of(isCompressed.evaluate(…))).orNullSafe(
 * ComparisonResult.ofNullSafe(MapperS.of(isPortfolioRebalancing.evaluate(…)))).asMapper();} against
 * {@code final MapperS<Boolean> ifThenElseResult;}.
 *
 * <p><b>The defect — and the producer the probe REFUTED (LAW 72).</b> The close census charted the seat as
 * {@code ControlFlowHandler.wrapDeepThenIteArm} (whose {@code .asMapper()} arm is gated to TYPE conditions by
 * the wave-D datarule work). The seat-23 runtime probe over all 275 matrix rows on BOTH routes showed every one
 * of that seat's 31 ComparisonResult arms to be a type-condition carrier (drr 6.34–6.38 × 3 + drr 7.0–7.3 × 4
 * validation files, already coerced) — {@code PTRRRule} never reaches it. The carrier's seat is the rule
 * then-chain {@code ifThenElseResult} hoist, {@code FunctionExpressionRenderer.appendIteHoistChainCore}, whose
 * single-form arm pipeline ({@code wrapEnumRungInMapperSOf} → {@code wrapBareInvocationOperand} →
 * {@code coerceBigIntegerLiteralIteArm} → {@code wrapDistinctCollapseIteArm} →
 * {@code coerceLiteralIteArmToSingletonList}) had no ComparisonResult step at all: 4 whole-file drr 7.0–7.3
 * POJO carriers ({@code PTRRRule} emir), every one NON-COMPILING (LAW 74).
 *
 * <p><b>The seat (ONE predicate, TWO consumers — LAW 69).</b>
 * {@code HandlerHelper.comparisonResultIteArmNeedsAsMapper(armNode, multi)} — a single-seat
 * {@link HandlerHelper#isComparisonResultExpr} arm — consulted by {@code appendIteHoistChainCore} for the then
 * AND the else arm (the carrier's seat; after the existing wraps, which cannot co-occur with a boolean operator
 * arm) and by {@code wrapDeepThenIteArm}, whose type-condition gate it RETIRES: the law is context-free, and the
 * retirement moves nothing by measurement (the probe's 31 arms are all type conditions — the wave-D carriers keep
 * their bytes and the whole-cell control below pins them). A MULTI seat is excluded by typing (a boolean operator
 * is single), so no carrier exists there.
 *
 * <p><b>RED at the pre-seat blob</b> ({@code rune-java-generator/src/main} at {@code d966c6ce} — this suite
 * kept): a1–a3, {@code corpus_c1}, {@code corpus_control1} (and under {@code -Pir-on} also {@code a4};
 * {@code control2} compares the two routes and is GREEN in both states — the seat is shared); b2, b3 +
 * {@code control0} GREEN in both states. <b>LAW 66/76 mutations</b> (each applied → run → reverted at the FINAL
 * head, LAW 78; receipts in the PR body): (i) the predicate deleted (returns false) → a1–a3, b3 + c1 + control1
 * (b3 moves because the deep-then seat consults the same predicate — the LAW-69 receipt); (ii) the then-arm
 * consult deleted only → a1, a2 + c1 + control1, a3 stays (isolating); (iii) the else-arm consult deleted only →
 * a3 only (the corpus carries no else-arm carrier — a declared fixture pin); (iv) the deep-then seat reverted to
 * the wave-D type-condition gate → NOTHING moves (declared, MEASURED: 0 corpus carriers outside type conditions;
 * b3 keeps passing since it IS a type condition) — information-free, so it is NOT in the chain.
 */
class ComparisonResultIteArmAsMapperSeatTest {

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
     * One {@code from} type with a function filter (the {@code IsAllowableAction} twin), two boolean functions
     * (the {@code IsCompressed} / {@code IsPortfolioRebalancing} twins), and one reporting rule per arm shape: the
     * PTRRRule shape (an {@code or} of two fn calls as the then-arm), an {@code exists} then-arm, a
     * ComparisonResult ELSE arm, a non-ComparisonResult ladder; plus the wave-D type-condition shape at the
     * deep-then seat.
     */
    private static final String MODEL = """
            namespace census.seat23f21
            version "1.0.0"

            type Step:
                kind string (0..1)

            type Party:
                clearable boolean (0..1)

            type Jur:
                authority string (0..1)
                parties Party (1..*)

            type Instr:
                step Step (0..1)
                allowed boolean (0..1)
                name string (0..1)
                jur Jur (0..*)

                condition B3Clearable: <"b3 - THE wave-D ReportableInformationMandatorilyClearableConditionCFTC SHAPE: a then-chain conditional with a ComparisonResult arm inside a TYPE condition - the deep-then seat, already .asMapper() through the wave-D gate (byte-frozen)">
                    jur
                        then filter authority = "CFTC"
                        then if parties -> clearable exists
                            then parties -> clearable distinct count = 1

            func IsAllowable: <"IsAllowableAction twin - the function filter">
                inputs:
                    i Instr (0..1)
                output:
                    result boolean (1..1)
                set result:
                    i -> allowed = True

            func IsCompressed: <"IsCompressed twin">
                inputs:
                    s Step (0..1)
                output:
                    result boolean (1..1)
                set result:
                    s -> kind = "COMPRESSION"

            func IsRebalancing: <"IsPortfolioRebalancing twin">
                inputs:
                    s Step (0..1)
                output:
                    result boolean (1..1)
                set result:
                    s -> kind = "REBALANCE"

            reporting rule StepRule from Instr: <"a Step rule (TradeForEvent twin)">
                extract step

            reporting rule A1Ptrr from Instr: <"a1 - THE PTRRRule SHAPE: a function filter, then a conditional whose then-arm is an `or` of two boolean fn calls, at a MapperS<Boolean> local">
                filter IsAllowable
                then if StepRule exists
                    then IsCompressed(step) or IsRebalancing(step)
                    as "a1"

            reporting rule A2ExistsArm from Instr: <"a2 - an `exists` then-arm">
                filter IsAllowable
                then if StepRule exists
                    then step -> kind exists
                    as "a2"

            reporting rule A3ElseArm from Instr: <"a3 - a ComparisonResult ELSE arm (the then-arm a bare fn call, which keeps its MapperS.of wrap)">
                filter IsAllowable
                then if name exists
                    then IsCompressed(step)
                    else step -> kind = "REBALANCE"
                    as "a3"

            reporting rule B2NonCrArm from Instr: <"b2 - a non-ComparisonResult ladder: string nav arms at a MapperS<String> local keep their bytes">
                filter IsAllowable
                then if StepRule exists
                    then step -> kind
                    else name
                    as "b2"
            """;

    // =========================================================================
    // Part A — the seat (RED at the pre-seat blob)
    // =========================================================================

    /** a1 — the PTRRRule shape: the `or` then-arm coerces .asMapper() against the MapperS<Boolean> local. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_ptrrShapeOrArmCoercesAsMapper() throws IOException {
        String out = rule("A1PtrrRule.java");
        assertContains(out, "final MapperS<Boolean> ifThenElseResult;");
        assertContains(out, "ifThenElseResult = ComparisonResult.ofNullSafe(MapperS.of(isCompressed.evaluate(");
        assertContains(out, ".orNullSafe(ComparisonResult.ofNullSafe(MapperS.of(isRebalancing.evaluate(");
        assertContains(out, ")))).asMapper();");
        assertContains(out, "ifThenElseResult = MapperS.<Boolean>ofNull();");
        assertContains(out, "output = ifThenElseResult.get();");
    }

    /** a2 — an `exists` then-arm coerces the same way. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_existsArmCoercesAsMapper() throws IOException {
        String out = rule("A2ExistsArmRule.java");
        assertContains(out, "final MapperS<Boolean> ifThenElseResult;");
        assertContains(out, "ifThenElseResult = exists(");
        assertContains(out, ").asMapper();");
    }

    /** a3 — a ComparisonResult ELSE arm coerces; the fn-call then-arm keeps its MapperS.of wrap (not a boolean operator). */
    @Test
    @EnabledIf("builtinsAvailable")
    void a3_elseArmCoercesAsMapper() throws IOException {
        String out = rule("A3ElseArmRule.java");
        assertContains(out, "ifThenElseResult = MapperS.of(isCompressed.evaluate(");
        assertContains(out, "ifThenElseResult = areEqual(");
        assertContains(out, "MapperS.of(\"REBALANCE\"), CardinalityOperator.All).asMapper();");
        assertNotContains(out, "evaluate(thenArg.<Step>map(\"getStep\", instr -> instr.getStep()).get())).asMapper()");
    }

    /**
     * a4 — LAW 77: the a1 shape rendered through the REAL {@code IRGeneration.functionGenerator} seam
     * ({@code -Pir-on}); the rule then-chain hoist is the shared legacy renderer, so the bytes agree.
     */
    @Test
    @EnabledIf("builtinsAndIrProviderAvailable")
    void a4_ptrrShapeOnIrRoute() throws IOException {
        String out = lookup(fixtureOnIrRoute(), "reports/A1PtrrRule.java");
        assertContains(out, ")))).asMapper();");
        assertContains(out, "final MapperS<Boolean> ifThenElseResult;");
    }

    // =========================================================================
    // Part B — placement pins (GREEN in BOTH states)
    // =========================================================================

    /** b2 — a non-ComparisonResult ladder (string nav arms at a MapperS<String> local) keeps its bytes: no .asMapper(). */
    @Test
    @EnabledIf("builtinsAvailable")
    void b2_nonComparisonResultArmsUnchanged() throws IOException {
        String out = rule("B2NonCrArmRule.java");
        assertContains(out, "final MapperS<String> ifThenElseResult;");
        assertNotContains(out, ".asMapper()");
    }

    /**
     * b3 — the wave-D type-condition shape at the DEEP-THEN seat keeps {@code .asMapper()} (it was coerced
     * through the type-condition gate before the seat; the gate's retirement keeps it through the shared
     * predicate — the LAW-69 receipt, and mutation (i) moves it).
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b3_typeConditionDeepThenArmKeepsAsMapper() throws IOException {
        String out = lookup(fixture(), "validation/datarule/InstrB3Clearable.java");
        assertContains(out, "final MapperS<Boolean> ifThenElseResult;");
        assertContains(out, "MapperS.of(1), CardinalityOperator.All).asMapper();");
    }

    // =========================================================================
    // Part C — the corpus (drr 7.0.0: the carrier + the whole-cell control)
    // =========================================================================

    private static final Path DRR7_CELL_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path DRR7_GOLDEN = DRR7_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    static boolean drr7Available() {
        return Drr7Corpus.gate(Files.isDirectory(DRR7_GOLDEN), ComparisonResultIteArmAsMapperSeatTest.class);
    }

    private static final String PTRR_RULE = "drr/regulation/common/emir/reports/PTRRRule.java";

    /** c1 — drr 7.0.0 emir PTRRRule whole-file lock (the carrier). */
    @Test
    @EnabledIf("drr7Available")
    void corpus_c1_ptrrRuleByteIdentical() throws IOException {
        lock(PTRR_RULE);
    }

    /**
     * control0 — golden is the oracle (the frozen drr 7.0.0 tree, every kind): the two token populations at
     * their exact census counts — T1 an {@code ifThenElseResult… = ….asMapper();} arm, 5 sites (the 4 wave-D
     * validation files + PTRRRule), and T2 a {@code final MapperS<Boolean> ifThenElseResult…;} declaration,
     * 16 sites — over 13 token-bearing files; the carrier carries (1, 1).
     */
    @Test
    @EnabledIf("drr7Available")
    void corpus_control0_goldenDrr7IsTheOracle() throws IOException {
        Map<String, int[]> g = scan(readGoldenTree(DRR7_GOLDEN));
        assertEquals(13, g.size(), "golden token-bearing files (the whole-cell control's domain)");
        assertEquals(5, sites(g, 0), "golden T1 ifThenElseResult .asMapper() arms");
        assertEquals(16, sites(g, 1), "golden T2 MapperS<Boolean> ifThenElseResult decls");
        int[] c = g.get(PTRR_RULE);
        assertNotNull(c, "golden carrier carries a token: " + PTRR_RULE);
        assertEquals(List.of(1, 1), List.of(c[0], c[1]), "golden carrier (T1, T2)");
    }

    /**
     * control1 — the FORK's WHOLE generated drr 7.0.0 cell (every kind, LAW 72): over the UNION of the files
     * either tree carries a token in (a missing side counts as all-zero, LAW 79), the per-file (T1, T2) pairs
     * agree FILE BY FILE — an over-fire (a non-boolean arm gaining {@code .asMapper()}) and an under-fire (the
     * carrier still bare) both fail here; the domain equals golden's 13; the wave-D validation files — the
     * deep-then seat's carriers — are pinned here too, so the gate retirement cannot move them unseen.
     */
    @Test
    @EnabledIf("drr7Available")
    void corpus_control1_forkDrr7WholeCellAsMapperSitesEqualGoldenFileByFile() throws IOException {
        assertNotNull(drr7Output, "drr 7.0.0 generation did not run");
        // FAIL-CLOSED: the cell is error-free since this seat, so ANY generation error means a file
        // is missing from the scan for an unknown reason and the whole-cell contract cannot be asserted.
        assertEquals(List.of(), drr7GenErrors, "drr 7.0.0 reported a generation error — the scan is incomplete");
        assertUnionEqual(scan(drr7Output), scan(readGoldenTree(DRR7_GOLDEN)), drr7Output.keySet(),
                "fork", "golden", KNOWN_RESIDUE_DRR7, 13);
    }

    /** The named pre-existing residue of other families in this cell (LAW 73: the set, not the count). */
    private static final List<String> KNOWN_RESIDUE_DRR7 = List.of();

    /**
     * control2 — LAW 77 route parity: the whole drr 7.0.0 cell generated through the REAL
     * {@code IRGeneration} seams ({@code -Pir-on}) carries the SAME per-file (T1, T2) pairs as the legacy-route
     * render, file by file over the UNION (the domain is the legacy render's own).
     */
    @Test
    @EnabledIf("drr7AndIrProviderAvailable")
    void corpus_control2_irRouteDrr7AsMapperSitesEqualLegacyRouteFileByFile() throws IOException {
        assertNotNull(drr7Output, "drr 7.0.0 legacy-route generation did not run");
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", DRR7_CELL_ROOT), new ArrayList<>());
        assertUnionEqual(scan(irOut), scan(drr7Output), irOut.keySet(), "ir", "legacy", List.of(),
                scan(drr7Output).size());
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

    private static Map<String, int[]> scan(Map<String, String> tree) {
        Map<String, int[]> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : tree.entrySet()) {
            if (!e.getValue().contains("ifThenElseResult")) {
                continue;
            }
            int[] c = new int[2];
            for (String raw : codeOnly(e.getValue()).split("\n")) {
                String s = raw.strip();
                if (s.startsWith("ifThenElseResult") && s.endsWith(".asMapper();")) {
                    c[0]++;
                }
                if (s.startsWith("final MapperS<Boolean> ifThenElseResult")) {
                    c[1]++;
                }
            }
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
                + path + " — seat 23 F21: the ComparisonResult ITE arm coerces .asMapper().");
    }

    // =========================================================================
    // Fixture harness
    // =========================================================================

    private static RLinkingResult linking;
    private static RModel mainModel;
    private static Map<String, String> fixtureOut;

    private static void link() throws IOException {
        if (linking == null) {
            RModel main = AstBuilder.buildFromString(MODEL, "seat23f21.rosetta");
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
        DataRuleGenerator dataRuleGen = new DataRuleGenerator(gm, tt, typeUtil);
        Map<String, String> out = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        ruleGen.generateClasses(mainModel, "1.0", out)
                .forEach(e -> errors.add(e.getTargetPath() + " — " + e));
        dataRuleGen.generateClasses(mainModel, "1.0", out)
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
            fixtureOut = render(m -> "census.seat23f21".equals(m.namespace()));
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
                        m -> "census.seat23f21".equals(m.namespace()));
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
            throw new AssertionError("[ComparisonResultIteArmAsMapperSeatTest] builtins parse"
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
