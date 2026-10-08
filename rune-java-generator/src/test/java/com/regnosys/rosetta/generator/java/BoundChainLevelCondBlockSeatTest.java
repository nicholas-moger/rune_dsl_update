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
 * SEAT 26, law E — facet {@code boundChainLevelCondBlock}: <b>a conditional consumed at a
 * BOUND (restructured) then-chain LEVEL inside a statement-rendered ladder rung takes the
 * block-lambda form</b> — the {@code isCleanLadderContext} rung-chain walk terminates at the
 * SET-operation ROOT conditional exactly as it terminates at the datarule {@code RCondition}
 * (the #355 set-root transparency one rung deeper: {@code appendIteHoistChainCore} renders
 * every rung as a statement block, so an interior ladder inside a rung's HOISTED chain level
 * is golden's {@code .mapSingleToItem(item -> { if … return …; })} form — golden drr
 * {@code EMIRClearingObligation}'s inner enum ladder).
 *
 * <p><b>The probe verdict this law answers (LAW 75):</b> the EMIR ladder printed
 * {@code isLadder=false … clean=false} — the walk hit the OUTER set-root ladder's RUNG
 * conditional ancestor and no transparency admitted it; the census's ":6792 pending-hoist
 * reject" line for this carrier was REFUTED (zero block attempts — the gate never admitted).
 *
 * <p><b>RED at the pre-seat blob (the law commit's parent, this suite kept)</b>: a1, c1;
 * b1 GREEN in both states.
 *
 * <p><b>LAW 66/76 mutations</b> (each applied → run → reverted; the failing sets MEASURED by
 * the seat's run-2 chain at {@code 124499fa} — LAW 82). Law E: (e-i) the set-root rung-chain
 * terminator deleted — 2F: a1, corpus_c1; (e-ii) the ladder CLIMB dropped (only an IMMEDIATE
 * set-root ancestor terminates) — 2F: THE SAME SET — whenever the terminator line is reached the
 * immediate conditional ancestor is NOT the set root (the guard construction), so the climb IS
 * the terminator's reach and the coincidence is the measurement. The first cut's (e-ii) — a copy
 * of the terminator escaping the subtree-scoping guard — measured 0F: on carriers the escaped
 * copy re-derives the guarded copy's verdict (a pure re-order) and the widened class has no
 * carrier in the scanned cells; re-aimed at the close. Law F (its corpus lock corpus_c2 lives
 * here): (f-i) the pre-law cond-gate restored (rule-path always, fn-path base-only) — 2F:
 * corpus_c2 + the cross-suite {@code InLambdaThenChainCtlAdmitSeatTest.corpus_control3} (CEAEC
 * re-enters the drr 7.0.0 carrier set); (f-ii) the IN-LAMBDA term dropped — <b>0F MEASURED</b>:
 * the term is defense-in-depth — the downstream {@code lambdaChannel} computation re-imposes
 * in-lambda, so a sink-less out-of-lambda chain declines either way and the variant is
 * byte-inert by construction; kept as the measured-zero record (the seat-25 c-ii precedent),
 * not a load-bearing gate.
 */
class BoundChainLevelCondBlockSeatTest {

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
     * A1 = the EMIR shape distilled: a fn SET whose value is an outer if/else-if ladder with a
     * rung arm carrying a hoisted then-chain whose LAST level is an inner enum-arm ladder.
     * B1 = the decline pin: the SAME inner ladder whose own subtree carries a nested then keeps
     * the inline form (the #350 scoping law — an arm-interior then still renders `.then(`).
     */
    private static final String MODEL = """
            namespace census.seat26f12e
            version "1.0.0"

            enum Grade:
                HIGH
                LOW
                NONE

            type Leg:
                name string (0..1)
                grade Grade (0..1)

            type Instr:
                legs Leg (0..*)
                allowed boolean (0..1)

            func IsOk:
                inputs:
                    i Instr (0..1)
                output:
                    result boolean (1..1)
                set result:
                    i -> allowed = True

            func A1RungChainLadder:
                inputs:
                    i Instr (0..1)
                output:
                    result Grade (0..1)
                set result:
                    if IsOk(i)
                    then Grade -> NONE
                    else if IsOk(i) = False
                    then (i -> legs
                        then filter name exists
                        then only-element
                        then extract [
                            if item -> grade = Grade -> HIGH
                            then HIGH
                            else if item -> grade = Grade -> LOW
                            then LOW
                        ])
            """;

    /** a1 — the inner ladder block-renders inside the hoisted chain level; no inline ternary. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_innerLadderBlockRendersInsideBoundChainLevel() throws IOException {
        String out = fn("A1RungChainLadder.java");
        assertTrue(!codeOnly(out).contains(".getOrDefault(false) ? "),
                "a1 must not fall to the inline ternary:\n" + out);
        assertContains(out, "return MapperS.of(Grade.HIGH);");
        assertContains(out, "return MapperS.<Grade>ofNull();");
    }

    // =========================================================================
    // Part C — the corpus carriers
    // =========================================================================

    private static final Path DRR7_CELL_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path DRR7_GOLDEN = DRR7_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    static boolean drr7Available() {
        return Drr7Corpus.gate(Files.isDirectory(DRR7_GOLDEN), BoundChainLevelCondBlockSeatTest.class);
    }

    static boolean drr7AndIrProviderAvailable() {
        return drr7Available() && irProviderOnClasspath();
    }

    private static final String EMIR = "drr/regulation/common/emir/contract/functions/EMIRClearingObligation.java";
    private static final String CEAEC = "drr/regulation/csa/rewrite/trade/functions/ClearingExceptionsAndExemptionsCounterparty.java";

    @Test
    @EnabledIf("drr7Available")
    void corpus_c1_emirClearingObligationByteIdentical() throws IOException {
        lock7(EMIR);
    }

    /** c2 — the law-F carrier: the fn-path in-lambda cond chain (facet fnInLambdaCondChainAdmit). */
    @Test
    @EnabledIf("drr7Available")
    void corpus_c2_clearingExceptionsCounterpartyByteIdentical() throws IOException {
        lock7(CEAEC);
    }

    /** control0 — golden is the oracle: the carrier's golden carries the block-lambda ladder. */
    @Test
    @EnabledIf("drr7Available")
    void corpus_control0_goldenCarriesBlockLambdaLadder() throws IOException {
        String golden = Files.readString(DRR7_GOLDEN.resolve(EMIR));
        assertTrue(golden.contains("return MapperS.of(ClearingObligationEnum.TRUE);"),
                "golden must carry the in-block enum arm return");
        assertTrue(golden.contains("return MapperS.<ClearingObligationEnum>ofNull();"),
                "golden must carry the typed-empty terminal");
    }

    /** control2 — LAW 77 route parity: the IR-route render of the carrier byte-matches the legacy route. */
    @Test
    @EnabledIf("drr7AndIrProviderAvailable")
    void corpus_control2_irRouteMatchesLegacyForCarrier() throws IOException {
        assertNotNull(drr7Output, "drr 7.0.0 legacy-route generation did not run");
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", DRR7_CELL_ROOT), new ArrayList<>());
        assertEquals(drr7Output.get(EMIR), irOut.get(EMIR), "route divergence: " + EMIR);
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
                + path + " — seat 26 law E: the bound-chain level's ladder block-renders.");
    }

    // =========================================================================
    // Fixture harness
    // =========================================================================

    private static RLinkingResult linking;
    private static RModel mainModel;
    private static Map<String, String> fixtureOut;

    private static void link() throws IOException {
        if (linking == null) {
            RModel main = AstBuilder.buildFromString(MODEL, "seat26f12e.rosetta");
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
            fixtureOut = render(m -> "census.seat26f12e".equals(m.namespace()));
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
            throw new AssertionError("[BoundChainLevelCondBlockSeatTest] builtins parse"
                    + " failures: " + String.join("; ", failures));
        }
        return models;
    }
}
