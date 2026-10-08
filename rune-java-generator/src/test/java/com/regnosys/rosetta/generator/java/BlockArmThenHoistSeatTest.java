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
 * SEAT 26, law B — facet {@code blockArmThenHoist}: <b>a then chain BURIED inside a
 * BLOCK-rendered ladder-rung ARM (an evaluate-arg of the arm's invocation) hoists its
 * {@code thenArg} decls INTO the owning rung</b> — the {@code compileLadderArmWithDeepThenDrain}
 * bless extends from arm-ROOT chains to the arm whose subtree carries exactly ONE buried
 * admissible chain (rule-path): the chain node blesses (the #339/#383 single-slot identity
 * handshake), hoists through the #350-F5 lambda channel during the arm compile, and the
 * existing rung drain pulls its decls in-rung, exactly like the #331/#371/#372 in-rung hoist
 * classes (golden mas {@code QuantityFrequencyOfLeg2Rule}: {@code final MapperC<PriceQuantity>
 * thenArg = …\n\t.filterItemNullSafe(…); return MapperS.of(frequencyPeriod.evaluate(
 * MapperS.of(thenArg.get()).get(), …));} INSIDE the {@code boolean1} rung).
 *
 * <p><b>The probe verdict this law answers (LAW 75, {@code probe26-off-carriers.txt}):</b> the
 * QuantityFrequencyOf* chains print {@code valueSupp=true} at the fallback — the ladder block's
 * P352A arm-compile suppression; the arm-root bless never fires because the chain sits one level
 * down, inside the invocation's ARG. The rung is a real braced block, so the #250
 * inline-ternary-cascade rationale behind the suppression does not apply to the buried chain.
 *
 * <p><b>RED at the pre-seat blob (the law commit's parent, this suite kept)</b>: a1, c1, c2 and
 * the law-A suite's {@code corpus_control1} hand-off entries (the two QuantityFrequency rows
 * leave that pin in THIS commit — the within-seat LAW-81 discipline); b1 GREEN in both states.
 *
 * <p><b>LAW 66/76 mutations</b> (each applied → run → reverted; the failing sets MEASURED by the
 * seat's run-2 chain at {@code 124499fa} — LAW 82): (i) the buried-chain arm bless deleted —
 * 4F: a1, corpus_c1, corpus_c2 + the cross-suite
 * {@code InLambdaThenChainCtlAdmitSeatTest.corpus_control1} (the QF pair re-enters the 5.61.0
 * {@code .then(} carrier set); (ii) the single-slot restriction widened to a first-of-many bless —
 * 1F: b1, FIXTURE-ONLY BY MEASUREMENT: the two-chain decline class is corpus-unwitnessed (no
 * banded arm carries two buried admissible chains), and b1's oracle fires only since the seat's
 * close made it witness-unique (the added no-{@code thenArg} assert — the first cut checked only
 * that {@code .then(} survives, which a first-of-many bless satisfies; the run-1 chain measured
 * that blindness as 0F).
 */
class BlockArmThenHoistSeatTest {

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
     * A1 = the QuantityFrequencyOf* shape: an else-if ladder in an extract lambda whose rung arm
     * is a fn CALL carrying ONE buried {@code filter … then only-element} chain in an arg.
     * B1 = the decline pin: an arm carrying TWO buried chains keeps today's inline bytes (the
     * bless is a single-slot identity handshake; corpus-unwitnessed shape).
     */
    private static final String MODEL = """
            namespace census.seat26f7b
            version "1.0.0"

            type Leg:
                name string (0..1)

            type Instr:
                legs Leg (0..*)
                allowed boolean (0..1)
                backup boolean (0..1)

            func IsOk:
                inputs:
                    i Instr (0..1)
                output:
                    result boolean (1..1)
                set result:
                    i -> allowed = True

            func IsBackup:
                inputs:
                    i Instr (0..1)
                output:
                    result boolean (1..1)
                set result:
                    i -> backup = True

            func TakeLeg:
                inputs:
                    l Leg (0..1)
                output:
                    result string (1..1)
                set result:
                    l -> name

            func TakeTwo:
                inputs:
                    a Leg (0..1)
                    b Leg (0..1)
                output:
                    result string (1..1)
                set result:
                    a -> name

            reporting rule A1RungChain from Instr: <"a1 - the buried arg chain hoists INTO the owning rung">
                extract i [
                    if IsOk(i)
                    then TakeLeg(i -> legs
                        filter item -> name exists
                        then only-element)
                    else if IsBackup(i)
                    then "z"
                ]
                    as "a1"

            reporting rule B1TwoChains from Instr: <"b1 - TWO buried chains keep today's bytes (single-slot bless)">
                extract i [
                    if IsOk(i)
                    then TakeTwo(
                        i -> legs filter item -> name exists then only-element,
                        i -> legs filter item -> name is absent then only-element)
                    else if IsBackup(i)
                    then "z"
                ]
                    as "b1"
            """;

    /** a1 — the buried chain's thenArg decl lands INSIDE the rung; the runtime `.then(` is gone. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_buriedArgChainHoistsIntoOwningRung() throws IOException {
        String out = rule("A1RungChainRule.java");
        assertTrue(!codeOnly(out).contains(".then("),
                "a1 must not emit the runtime `.then(` form:\n" + out);
        assertContains(out, "final MapperC<Leg> thenArg = ");
        assertContains(out, "return MapperS.of(takeLeg.evaluate(MapperS.of(thenArg.get()).get()));");
    }

    /**
     * b1 — TWO buried chains: the single-slot bless declines, today's inline bytes stay.
     *
     * <p>The oracle is witness-unique BOTH ways (the decline-lock law): the inline
     * {@code .then(} must survive AND no {@code thenArg} hoist may appear — a
     * first-of-many bless would hoist chain 1 (a {@code thenArg} decl) while chain 2
     * keeps its {@code .then(}, satisfying the presence check alone.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b1_twoBuriedChainsKeepDecline() throws IOException {
        String out = rule("B1TwoChainsRule.java");
        assertTrue(codeOnly(out).contains(".then("),
                "b1 must keep today's inline decline — the bless is single-slot"
                + " (two buried chains are corpus-unwitnessed):\n" + out);
        assertTrue(!codeOnly(out).contains("thenArg"),
                "b1 must hoist NEITHER buried chain — a thenArg decl means the"
                + " single-slot restriction leaked a first-of-many bless:\n" + out);
    }

    // =========================================================================
    // Part C — the corpus carriers (drr 5.61.0 mas)
    // =========================================================================

    private static final Path DRR561_CELL_ROOT = Path.of("../test-corpus/drr/drr-5.61.0");
    private static final Path DRR561_GOLDEN = DRR561_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    static boolean drr561Available() {
        return Files.isDirectory(DRR561_GOLDEN);
    }

    static boolean drr561AndIrProviderAvailable() {
        return drr561Available() && irProviderOnClasspath();
    }

    private static final String QFOL2 =
            "drr/regulation/mas/rewrite/trade/reports/QuantityFrequencyOfLeg2Rule.java";
    private static final String QFOTCOL1 =
            "drr/regulation/mas/rewrite/trade/reports/QuantityFrequencyOfTheContractOrLeg1Rule.java";

    @Test
    @EnabledIf("drr561Available")
    void corpus_c1_quantityFrequencyOfLeg2ByteIdentical() throws IOException {
        lock561(QFOL2);
    }

    @Test
    @EnabledIf("drr561Available")
    void corpus_c2_quantityFrequencyOfTheContractOrLeg1ByteIdentical() throws IOException {
        lock561(QFOTCOL1);
    }

    /** control0 — golden is the oracle: BOTH carriers' goldens place the thenArg decl INSIDE a rung. */
    @Test
    @EnabledIf("drr561Available")
    void corpus_control0_goldenCarriesInRungThenArgDecl() throws IOException {
        for (String p : List.of(QFOL2, QFOTCOL1)) {
            String golden = Files.readString(DRR561_GOLDEN.resolve(p));
            assertTrue(golden.contains("\t\t\t\t\t\tfinal MapperC<PriceQuantity> thenArg = "),
                    "golden must carry the in-rung thenArg decl: " + p);
        }
    }

    /** control2 — LAW 77 route parity: the IR-route render of BOTH carriers byte-matches the legacy route. */
    @Test
    @EnabledIf("drr561AndIrProviderAvailable")
    void corpus_control2_irRouteMatchesLegacyForCarriers() throws IOException {
        assertNotNull(drr561Output, "drr 5.61.0 legacy-route generation did not run");
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "5.61.0", DRR561_CELL_ROOT), new ArrayList<>());
        for (String p : List.of(QFOL2, QFOTCOL1)) {
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
                + path + " — seat 26 law B: the buried arg chain hoists into the owning rung.");
    }

    // =========================================================================
    // Fixture harness
    // =========================================================================

    private static RLinkingResult linking;
    private static RModel mainModel;
    private static Map<String, String> fixtureOut;

    private static void link() throws IOException {
        if (linking == null) {
            RModel main = AstBuilder.buildFromString(MODEL, "seat26f7b.rosetta");
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
            fixtureOut = render(m -> "census.seat26f7b".equals(m.namespace()));
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
            throw new AssertionError("[BlockArmThenHoistSeatTest] builtins parse"
                    + " failures: " + String.join("; ", failures));
        }
        return models;
    }
}
