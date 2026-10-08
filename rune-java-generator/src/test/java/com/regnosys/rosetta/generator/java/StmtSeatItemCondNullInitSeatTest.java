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
 * SEAT 26, law D — facet {@code stmtSeatItemCondNullInit}: <b>a no-else SINGLE conditional at a
 * {@code default}-operand ARG seat on a statement path hoists the null-initialized item local</b>
 * — {@code hoistAsItemLocalOrNull} gains the {@link
 * com.regnosys.rosetta.ast.expressions.binary.RDefaultExpr}-parent seat, typing the local from
 * the then-arm's LEAF attribute through the SAME {@code onlyElementLeafAttribute} walk the #365
 * ctor-field arm uses (the deep-path {@code ->>} leaf resolves where the workspace snapshot is
 * blind): golden drr {@code EconomicTermsForProduct} renders {@code final Boolean _boolean =
 * isProductETD.evaluate(product); EconomicTerms ifThenElseResult = null; if ((_boolean == null ?
 * false : _boolean)) { ifThenElseResult = <nav>.get(); } economicTerms = toBuilder(<nav>
 * .getOrDefault(ifThenElseResult));} — the fork's inline ternary handed {@code MapperC} poles to
 * {@code getOrDefault(EconomicTerms)}, never compiling (the LAW-74 PRE class).
 *
 * <p><b>The probe verdict this law answers (LAW 75, {@code probe26-off-carriers.txt}):</b>
 * EconomicTermsForProduct prints {@code inLambda=false} with a sink — a STATEMENT-seat carrier,
 * not a lambda interior; the census's F12 row named the lambda-interior class, and this carrier
 * is its statement-path sibling (the LAW-72 correction recorded in the charter).
 *
 * <p><b>RED at the pre-seat blob (the law commit's parent, this suite kept)</b>: a1, c1; b1
 * GREEN in both states. {@code BareFnCondHoistEveryContextSeatTest.KNOWN_RESIDUE_DRR7} pinned
 * EconomicTermsForProduct as {@code fork=[0,0,0] golden=[1,1,0]} — this law lands the
 * {@code _boolean} hoist, the control FIRES, and the pin empties in THIS commit (LAW 81, the
 * third within-seat firing).
 *
 * <p><b>LAW 66/76 mutations</b> (each applied → run → reverted; the failing sets MEASURED by
 * the seat's run-2 chain at {@code 124499fa} — LAW 82): (i) the default-operand ARG-seat leaf
 * typing deleted — 2F: corpus_c1 + the cross-suite
 * {@code BareFnCondHoistEveryContextSeatTest.corpus_control3} (the LAW-81 tripwire: its
 * KNOWN_RESIDUE_DRR7 was re-pinned EMPTY when this law healed EconomicTermsForProduct, and the
 * un-heal fires it); (ii) the DEFAULT LEFT-operand leaf fallback deleted — 2F: THE SAME SET —
 * the coincidence IS the measurement: the carrier's deep-path {@code ->>} then-leaf resolves
 * through NEITHER primary walk, so the left fallback is the sole type source and severing either
 * link kills the same heal. The law's landed workspace-cardinality MULTI-gate exception was
 * REMOVED at the seat's zero-adjudication close: the global-path computer is hardwired
 * {@code case RConditionalExpr c -> SINGLE}, so the enclosing gate never ran (the original (i)
 * toggle measured 0F against a RED corpus_c1 — the refutation) and the commit-message
 * resolution-blindness premise is withdrawn; the live half is the leaf-typing arm these
 * mutations sever.
 */
class StmtSeatItemCondNullInitSeatTest {

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
     * A1 = the EconomicTermsForProduct shape: a fn-path SET whose value is {@code <nav> default
     * if <bare fn cond> then <nav>} — the conditional at the default's RIGHT operand. B1 = the
     * decline pin: the same conditional WITH a real else keeps today's bytes (the effective-else
     * default-arg form is corpus-unwitnessed).
     */
    private static final String MODEL = """
            namespace census.seat26f12d
            version "1.0.0"

            type Leg:
                plain string (0..1)
                backupVal string (0..1)

            type Instr:
                lone Leg (0..1)
                allowed boolean (0..1)

            func IsOk:
                inputs:
                    i Instr (0..1)
                output:
                    result boolean (1..1)
                set result:
                    i -> allowed = True

            func A1DefaultCond:
                inputs:
                    i Instr (1..1)
                output:
                    result string (0..1)
                set result:
                    i -> lone -> plain default if IsOk(i)
                        then i -> lone -> backupVal

            func B1DefaultCondElse:
                inputs:
                    i Instr (1..1)
                output:
                    result string (0..1)
                set result:
                    i -> lone -> plain default if IsOk(i)
                        then i -> lone -> backupVal
                        else "z"
            """;

    /** a1 — the null-init item local + the bare-fn condition hoist + the spliced getOrDefault consumer. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_defaultArgCondHoistsNullInitItemLocal() throws IOException {
        String out = fn("A1DefaultCond.java");
        assertTrue(!codeOnly(out).contains(".getOrDefault(false) ? "),
                "a1 must not fall to the inline ternary:\n" + out);
        assertContains(out, "final Boolean _boolean = isOk.evaluate(i);");
        assertContains(out, "String ifThenElseResult = null;");
        assertContains(out, "if ((_boolean == null ? false : _boolean)) {");
        assertContains(out, ".getOrDefault(ifThenElseResult)");
    }

    /**
     * b1 — the SIBLING form's byte-freeze: a REAL else at the default-arg seat already hoists
     * through the #203 effective-else ladder (`final String ifThenElseResult; if … else …`) —
     * pinned so law D's seat arm provably leaves it untouched.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b1_defaultArgCondWithRealElseKeepsEffectiveElseLadder() throws IOException {
        String out = fn("B1DefaultCondElse.java");
        assertContains(out, "final String ifThenElseResult;");
        assertContains(out, "} else {");
        assertContains(out, "ifThenElseResult = \"z\";");
        assertContains(out, ".getOrDefault(ifThenElseResult)");
    }

    // =========================================================================
    // Part C — the corpus carrier (drr 7.0.0)
    // =========================================================================

    private static final Path DRR7_CELL_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path DRR7_GOLDEN = DRR7_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    static boolean drr7Available() {
        return Drr7Corpus.gate(Files.isDirectory(DRR7_GOLDEN), StmtSeatItemCondNullInitSeatTest.class);
    }

    static boolean drr7AndIrProviderAvailable() {
        return drr7Available() && irProviderOnClasspath();
    }

    private static final String ETFP = "drr/base/trade/functions/EconomicTermsForProduct.java";

    @Test
    @EnabledIf("drr7Available")
    void corpus_c1_economicTermsForProductByteIdentical() throws IOException {
        lock7(ETFP);
    }

    /** control0 — golden is the oracle: the carrier's golden carries the null-init local + the boolean hoist. */
    @Test
    @EnabledIf("drr7Available")
    void corpus_control0_goldenCarriesNullInitLocalAndBoolHoist() throws IOException {
        String golden = Files.readString(DRR7_GOLDEN.resolve(ETFP));
        assertTrue(golden.contains("EconomicTerms ifThenElseResult = null;"),
                "golden must carry the null-init item local");
        assertTrue(golden.contains("final Boolean _boolean = isProductETD.evaluate(product);"),
                "golden must carry the bare-fn condition hoist");
    }

    /** control2 — LAW 77 route parity: the IR-route render of the carrier byte-matches the legacy route. */
    @Test
    @EnabledIf("drr7AndIrProviderAvailable")
    void corpus_control2_irRouteMatchesLegacyForCarrier() throws IOException {
        assertNotNull(drr7Output, "drr 7.0.0 legacy-route generation did not run");
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", DRR7_CELL_ROOT), new ArrayList<>());
        assertEquals(drr7Output.get(ETFP), irOut.get(ETFP), "route divergence: " + ETFP);
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
    // Cell generation (drr 7.0.0 rules/reports/functions — the carrier's kinds)
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

    private static void lock7(String path) throws IOException {
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
                + path + " — seat 26 law D: the default-arg conditional hoists the null-init item local.");
    }

    // =========================================================================
    // Fixture harness
    // =========================================================================

    private static RLinkingResult linking;
    private static RModel mainModel;
    private static Map<String, String> fixtureOut;

    private static void link() throws IOException {
        if (linking == null) {
            RModel main = AstBuilder.buildFromString(MODEL, "seat26f12d.rosetta");
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
            fixtureOut = render(m -> "census.seat26f12d".equals(m.namespace()));
        }
        return fixtureOut;
    }

    private static String fn(String fileName) throws IOException {
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
            throw new AssertionError("[StmtSeatItemCondNullInitSeatTest] builtins parse"
                    + " failures: " + String.join("; ", failures));
        }
        return models;
    }
}
