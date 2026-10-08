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
 * SEAT 26, law C — facet {@code lambdaCondBlockEveryShape} (mechanisms 1 + 2): <b>the in-lambda
 * conditional BLOCK forms admit the #315/#265 meta-wrap hoists that made their carriers fall to
 * the inline ternary</b>:
 *
 * <ul>
 *   <li><b>(1) the nested tree's rung-CONDITION block splice</b> — a condition that compiled to a
 *       multi-statement block (the #315 exists-meta-wrap hoist: {@code final PriceSchedule
 *       priceSchedule = <rule>.evaluate(item.get());} ahead of the null-guarded wrapped exists)
 *       splices its statements before the {@code if (} at the frame's depth
 *       ({@code appendNestedConditional} — the LAW-75 probe's {@code site=nested block=false}
 *       class; golden jfsa {@code SpreadOfLeg2Basis/DecimalRule}).</li>
 *   <li><b>(2) the effective-else block's arm-hoist kinds admit
 *       {@code ReferenceHandler.MetaWrapValueHoist}</b> — golden places an ARM's rule-call meta
 *       wrap hoist INSIDE the owning branch before the wrap-ternary return (cftc
 *       {@code PriorUTIRule} / jfsa {@code PriorUtiRule} — whose ELSE arm's law-B/law-A chain
 *       hoists ride the SAME drains).</li>
 * </ul>
 *
 * <p><b>RED at the pre-seat blob (the law commit's parent, this suite kept)</b>: a1, a2, c1–c4;
 * b1 GREEN in both states. The law-A suite's {@code corpus_control1} fired AGAIN on this law
 * (PriorUTI/PriorUti left the runtime-`.then(` set — 9 → 5) and is re-pinned in THIS commit —
 * the second within-seat LAW-81 hand-off.
 *
 * <p><b>LAW 66/76 mutations</b> (each applied → run → reverted; the failing sets MEASURED by the
 * seat's run-2 chain at {@code 124499fa} — LAW 82): (i) the rung-CONDITION wrap-ternary
 * acceptance deleted — 3F: a1, corpus_c3, corpus_c4 (the Spread pair); (ii) the effective-else
 * arm-hoist MetaWrapValueHoist admits deleted — 4F: a2, corpus_c1, corpus_c2 (the PriorU pair) +
 * the cross-suite {@code InLambdaThenChainCtlAdmitSeatTest.corpus_control1}; (iii) the wrap
 * ternary's per-arm {@code getOrDefault} distribution dropped — 3F: THE SAME SET AS (i) (a1,
 * corpus_c3, corpus_c4): the acceptance and its render distribution are two links on ONE
 * mechanism at the same carriers — the coincidence is the measurement, stated per the seat-25
 * b-ii/iii/iv precedent.
 */
class LambdaCondBlockWrapHoistSeatTest {

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

    /**
     * A1 = the Spread shape: a nested-THEN tree whose OUTER condition is a meta-output rule-call
     * exists (the #315 wrap hoists into the block, before the `if (`). A2 = the PriorU shape: an
     * effective-else conditional whose THEN arm is a SINGLE+meta rule call (the #265 wrap hoist
     * lands inside the branch). B1 = the decline pin: a multi-statement condition at a seat with
     * NO end expression keeps the ternary (never constructible here — pinned by the two-chain
     * arm decline of law B instead; b1 here pins the elseless-single sibling's unchanged bytes).
     */
    private static final String MODEL = """
            namespace census.seat26f12c
            version "1.0.0"

            type Leg:
                code string (0..1)
                    [metadata scheme]
                plain string (0..1)

            type Instr:
                lone Leg (0..1)
                allowed boolean (0..1)

            reporting rule LegCode from Leg: <"a meta-output callee">
                extract code
                    as "code"

            reporting rule A1WrapCond from Instr: <"a1 - the nested tree's wrap-hoisting condition splices">
                extract i [
                    if LegCode( i -> lone ) exists
                    then (if i -> allowed = True then "y" else empty)
                    else empty
                ]
                    as "a1"

            reporting rule A2WrapArm from Instr: <"a2 - the effective-else arm's meta wrap hoists in-branch">
                extract i [
                    if i -> allowed = True
                    then LegCode( i -> lone )
                    else i -> lone -> code
                ]
                    as "a2"

            reporting rule B1Plain from Instr: <"b1 - a plain elseless conditional keeps its block form bytes">
                extract i [
                    if i -> allowed = True
                    then i -> lone -> plain
                ]
                    as "b1"
            """;

    /** a1 — the wrap hoist splices before the `if (`; no inline ternary. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_nestedTreeWrapCondSplices() throws IOException {
        String out = rule("A1WrapCondRule.java");
        assertTrue(!codeOnly(out).contains(".getOrDefault(false) ? "),
                "a1 must not fall to the inline ternary:\n" + out);
        assertContains(out, "final String string = legCodeRule.evaluate(");
        assertContains(out, "if ((string == null ? exists(MapperS.<FieldWithMetaString>ofNull()).getOrDefault(false) : exists(MapperS.of(FieldWithMetaString.builder().setValue(string).build())).getOrDefault(false))) {");
    }

    /** a2 — the arm's meta wrap hoist lands INSIDE the owning branch. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_effectiveElseArmWrapHoistsInBranch() throws IOException {
        String out = rule("A2WrapArmRule.java");
        assertTrue(!codeOnly(out).contains(".getOrDefault(false) ? "),
                "a2 must not fall to the inline ternary:\n" + out);
        assertContains(out, "final String string = legCodeRule.evaluate(");
        assertContains(out, "return string == null ? MapperS.<FieldWithMetaString>ofNull() : MapperS.of(FieldWithMetaString.builder().setValue(string).build());");
    }

    /** b1 — the plain elseless block form is byte-frozen by the seat (no wrap, no splice). */
    @Test
    @EnabledIf("builtinsAvailable")
    void b1_plainElselessBlockUnchanged() throws IOException {
        String out = rule("B1PlainRule.java");
        assertContains(out, "if (areEqual(i.<Boolean>map(\"getAllowed\"");
        assertTrue(!codeOnly(out).contains(".getOrDefault(false) ? "),
                "b1 keeps the block form:\n" + out);
    }

    // =========================================================================
    // Part C — the corpus carriers (drr 5.61.0)
    // =========================================================================

    private static final Path DRR561_CELL_ROOT = Path.of("../test-corpus/drr/drr-5.61.0");
    private static final Path DRR561_GOLDEN = DRR561_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    static boolean drr561Available() {
        return Files.isDirectory(DRR561_GOLDEN);
    }

    static boolean drr561AndIrProviderAvailable() {
        return drr561Available() && irProviderOnClasspath();
    }

    private static final String PRIOR_UTI = "drr/regulation/cftc/rewrite/reports/PriorUTIRule.java";
    private static final String PRIOR_UTI_JFSA = "drr/regulation/jfsa/rewrite/trade/reports/PriorUtiRule.java";
    private static final String SPREAD_BASIS = "drr/regulation/jfsa/rewrite/trade/reports/SpreadOfLeg2BasisRule.java";
    private static final String SPREAD_DECIMAL = "drr/regulation/jfsa/rewrite/trade/reports/SpreadOfLeg2DecimalRule.java";

    @Test
    @EnabledIf("drr561Available")
    void corpus_c1_priorUTIByteIdentical() throws IOException {
        lock561(PRIOR_UTI);
    }

    @Test
    @EnabledIf("drr561Available")
    void corpus_c2_priorUtiJfsaByteIdentical() throws IOException {
        lock561(PRIOR_UTI_JFSA);
    }

    @Test
    @EnabledIf("drr561Available")
    void corpus_c3_spreadOfLeg2BasisByteIdentical() throws IOException {
        lock561(SPREAD_BASIS);
    }

    @Test
    @EnabledIf("drr561Available")
    void corpus_c4_spreadOfLeg2DecimalByteIdentical() throws IOException {
        lock561(SPREAD_DECIMAL);
    }

    /** control0 — golden is the oracle: the four carriers' goldens carry the in-block wrap hoists. */
    @Test
    @EnabledIf("drr561Available")
    void corpus_control0_goldenCarriesInBlockWrapHoists() throws IOException {
        assertTrue(Files.readString(DRR561_GOLDEN.resolve(SPREAD_DECIMAL))
                        .contains("final PriceSchedule priceSchedule = cDESpreadLeg2Rule.evaluate(item.get());"),
                "golden SpreadOfLeg2Decimal must hoist the wrap value at the block top");
        assertTrue(Files.readString(DRR561_GOLDEN.resolve(PRIOR_UTI))
                        .contains("final String string = cDEPriorUTIRule.evaluate(item.get());"),
                "golden PriorUTI must hoist the arm wrap value in-branch");
    }

    /** control2 — LAW 77 route parity: the IR-route render of the four carriers byte-matches the legacy route. */
    @Test
    @EnabledIf("drr561AndIrProviderAvailable")
    void corpus_control2_irRouteMatchesLegacyForCarriers() throws IOException {
        assertNotNull(drr561Output, "drr 5.61.0 legacy-route generation did not run");
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "5.61.0", DRR561_CELL_ROOT), new ArrayList<>());
        for (String p : List.of(PRIOR_UTI, PRIOR_UTI_JFSA, SPREAD_BASIS, SPREAD_DECIMAL)) {
            assertEquals(drr561Output.get(p), irOut.get(p), "route divergence: " + p);
        }
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

    // =========================================================================
    // Cell generation (drr 5.61.0 rules/reports/functions — the carriers' kinds)
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

    private static void collect(List<String> sink, List<GenerationException> errors) {
        if (errors != null) {
            errors.forEach(e -> sink.add(e.getTargetPath() + " — " + e));
        }
    }

    private static void lock561(String path) throws IOException {
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
                + path + " — seat 26 law C: the in-lambda conditional block admits the wrap hoists.");
    }

    // =========================================================================
    // Fixture harness
    // =========================================================================

    private static RLinkingResult linking;
    private static RModel mainModel;
    private static Map<String, String> fixtureOut;

    private static void link() throws IOException {
        if (linking == null) {
            RModel main = AstBuilder.buildFromString(MODEL, "seat26f12c.rosetta");
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
            fixtureOut = render(m -> "census.seat26f12c".equals(m.namespace()));
        }
        return fixtureOut;
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
            throw new AssertionError("[LambdaCondBlockWrapHoistSeatTest] builtins parse"
                    + " failures: " + String.join("; ", failures));
        }
        return models;
    }
}
