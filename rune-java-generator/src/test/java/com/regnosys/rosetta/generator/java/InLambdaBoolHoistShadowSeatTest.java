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
 * SEAT 31, law 3 rung (b) -- facet {@code inLambdaBoolHoistShadowEscape} (F14): the
 * IN-LAMBDA bool-hoist local ({@code CollectionHandler}'s two block-conditional lambda
 * seats, #260) rides the per-METHOD session's per-LAMBDA {@code boolean} sub-group
 * ({@code StatementHoistSession.registerLambdaScoped} -- the #355 F-1 naming law) instead
 * of a hard {@code _boolean} literal, so it ESCAPES a method-level {@code _boolean}
 * sibling exactly as upstream's ancestor-taken walk does.
 *
 * <p><b>The mechanism, from the seat-31 law-3 bank ({@code law3-bank.md} rung (b)).</b>
 * Golden drr 5.61.0 jfsa {@code CallAmountRule}/{@code PutAmountRule}: the {@code mapItem}
 * lambda's hoist is {@code __boolean} (line 57) because the METHOD carries its own
 * {@code final Boolean _boolean = isFXOption.evaluate(...)} sibling (line 63, the FER
 * #179/#217 method-level hoist that rides {@code hoistSession.register}); the fork
 * emitted the literal {@code _boolean} at both, diverging on exactly the two lambda
 * lines. The escalation is ORDER-BLOCKED for any mark-then-read flag (the lambda renders
 * BEFORE the method-level hoist registers), which is precisely what the SESSION's
 * end-of-method {@code resolve} law already solves: the lambda sub-group singleton
 * resolves {@code escapeWhileUnusable("boolean", method-group-names)} -- method group
 * EMPTY {@code -> _boolean} (the keyword escape; today's bytes for every green carrier),
 * method group present {@code -> __boolean} (the heal). Green-safe by construction: a
 * green file carrying BOTH would already carry {@code __boolean} (upstream always
 * escapes), so the flip population is divergent-only.
 *
 * <p><b>The bank's caution is honoured:</b> the ControlFlowHandler per-rung method-level
 * {@code _boolean}s (the CryptoAsset sibling-branch REUSE family) are NOT routed through
 * any group -- only the two in-lambda literal seats move.
 *
 * <p><b>Charter:</b> {@code CallAmountRule} + {@code PutAmountRule} drr 5.61.0 x2 whole.
 * LAW 79/80: the whole-cell instrument is the seat chain's matrix digest + the
 * whole-matrix checkpoint's per-file accounting, disclosed here.
 *
 * <p><b>LAW 77.</b> CollectionHandler seats are IR-inherited; corpus_control2 measures
 * BOTH-ROUTES-vs-GOLDEN under {@code -Pir-on}.
 *
 * <p><b>MUTATIONS (LAW 66/76) -- MEASURED (LAW 82) at the seat-31 chain head {@code f2a4d5c0}:</b>
 * <ul>
 *   <li><b>m-law3b</b> ({@code lanes31.py --law3b}, the registration severed -- the literal
 *       fallback forced at both seats): MEASURED 5/3F/1S = a1, corpus_c1, corpus_c2; e1 PASSED --
 *       exactly the claim (MATCH; the no-sibling direction resolves {@code _boolean} either way).</li>
 * </ul>
 * RED at the seat's base {@code 0c8fc7dc4} (both routes): a1, corpus_c1, corpus_c2 (+
 * corpus_control2 on {@code -Pir-on}); e1 GREEN at RED. GREEN at the head 5/0F/1skip default,
 * 5/0F/0skip {@code -Pir-on}.
 */
class InLambdaBoolHoistShadowSeatTest {

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

    private static final String CALL =
            "drr/regulation/jfsa/rewrite/trade/reports/CallAmountRule.java";
    private static final String PUT =
            "drr/regulation/jfsa/rewrite/trade/reports/PutAmountRule.java";

    /** Cell A = drr 5.61.0 -- both carriers. */
    private static final Path CELL_A_ROOT = Path.of("../test-corpus/drr/drr-5.61.0");
    private static final Path GOLDEN_A = CELL_A_ROOT.resolve("rosetta-source/src/generated/java");

    static boolean cellAAvailable() {
        return Files.isDirectory(GOLDEN_A);
    }

    static boolean cellAAndIrProviderAvailable() {
        return cellAAvailable() && irProviderOnClasspath();
    }

    // =========================================================================
    // Fixtures
    // =========================================================================

    private static final String MODEL = """
            namespace census.seat31l3b
            version "1.0.0"

            type Leg3:
                amt number (0..1)

            type Holder3:
                legs Leg3 (0..*)

            func IsBig:
                inputs:
                    l Leg3 (0..1)
                output:
                    r boolean (1..1)
                set r:
                    l -> amt exists

            func IsHot:
                inputs:
                    n number (0..1)
                output:
                    r boolean (1..1)
                set r:
                    n exists

            reporting rule B1Both from Holder3: <"a1 - an IN-LAMBDA bool hoist under a METHOD-level bool-hoist sibling: the lambda one escalates">
                extract legs
                then extract (if IsBig(item) then item -> amt)
                then only-element
                then (if IsHot(item) then item)

            reporting rule B2LambdaOnly from Holder3: <"e1 - the in-lambda hoist with NO method sibling keeps _boolean (today's bytes)">
                extract legs
                then extract (if IsBig(item) then item -> amt)
            """;

    /**
     * a1 -- the shadow-escape: the in-lambda hoist renders {@code __boolean} under the
     * method-level {@code _boolean} sibling (both present in one method). PIN AT RED: if
     * the fixture's method-level stage does not produce a session-registered
     * {@code _boolean}, reshape the fixture -- do not weaken the assert.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_inLambdaHoistEscapesTheMethodSibling() throws IOException {
        String out = fixtureRule("B1BothRule");
        assertContains(out, "final Boolean _boolean = ");
        assertContains(out, "final Boolean __boolean = ");
        assertContains(out, "(__boolean == null ? false : __boolean)");
    }

    /**
     * e1 -- the no-sibling direction: a lone in-lambda hoist resolves the keyword-escaped
     * {@code _boolean} -- byte-identical to the pre-law literal (the green population's
     * form). This is the lock that severing the registration cannot invert.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void e1_loneInLambdaHoistKeepsTheKeywordEscape() throws IOException {
        String out = fixtureRule("B2LambdaOnlyRule");
        assertContains(out, "final Boolean _boolean = ");
        assertTrue(!out.contains("__boolean"),
                "a lone in-lambda hoist must NOT escalate:\n" + out);
    }

    /** corpus_c1 -- the CallAmountRule whole-file heal (drr 5.61.0 jfsa). */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_drr5610CallAmountRuleMatchesGolden() throws IOException {
        assertNotNull(drrAOutput, "drr 5.61.0 generation did not run");
        List<String> own = drrAGenErrors.stream().filter(e -> e.contains(CALL)).toList();
        assertTrue(own.isEmpty(), "generation errors for " + CALL + ": " + own);
        String gen = drrAOutput.get(CALL);
        assertNotNull(gen, "not generated: " + CALL);
        assertEquals(normalize(Files.readString(GOLDEN_A.resolve(CALL))), normalize(gen),
                "CallAmountRule must byte-match golden - seat 31 law 3 rung (b): the in-lambda"
                + " bool hoist escapes its method-level sibling");
    }

    /** corpus_c2 -- the PutAmountRule whole-file heal (drr 5.61.0 jfsa). */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c2_drr5610PutAmountRuleMatchesGolden() throws IOException {
        assertNotNull(drrAOutput, "drr 5.61.0 generation did not run");
        String gen = drrAOutput.get(PUT);
        assertNotNull(gen, "not generated: " + PUT);
        assertEquals(normalize(Files.readString(GOLDEN_A.resolve(PUT))), normalize(gen),
                "PutAmountRule must byte-match golden - seat 31 law 3 rung (b)");
    }

    /**
     * corpus_control2 -- LAW 77, BOTH-ROUTES-vs-GOLDEN: the IR route (which inherits the
     * CollectionHandler seats) must render the healed carriers golden-identical too.
     */
    @Test
    @EnabledIf("cellAAndIrProviderAvailable")
    void corpus_control2_irRouteMatchesGoldenForCallPut() throws IOException {
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "5.61.0", CELL_A_ROOT),
                new ArrayList<>());
        assertEquals(normalize(Files.readString(GOLDEN_A.resolve(CALL))),
                normalize(irOut.get(CALL)), "IR route vs GOLDEN: " + CALL);
        assertEquals(normalize(Files.readString(GOLDEN_A.resolve(PUT))),
                normalize(irOut.get(PUT)), "IR route vs GOLDEN: " + PUT);
    }

    // =========================================================================
    // Fixture harness (rule renderer -- the Law2GuardsSeatTest pattern, trimmed)
    // =========================================================================

    private record Render(Map<String, String> output, List<String> errors) {}

    private static Render rendered;

    private static Render render() throws IOException {
        if (rendered != null) {
            return rendered;
        }
        RModel main = AstBuilder.buildFromString(MODEL, "seat31l3b.rosetta");
        main.setVersion("0.0.0.test");
        List<RModel> models = new ArrayList<>();
        models.add(main);
        models.addAll(loadBuiltinsOnly());
        RWorkspace workspace = RWorkspace.build(models).workspace();
        GeneratorModel gm = new GeneratorModel(workspace,
                m -> "census.seat31l3b".equals(m.namespace()));
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
                .filter(e -> e.getKey().endsWith(path))
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
            throw new AssertionError("[InLambdaBoolHoistShadowSeatTest] builtins parse"
                    + " failures: " + String.join("; ", failures));
        }
        return models;
    }

    // =========================================================================
    // Corpus harness (the ReceiverRenderTypingSeatTest cell generator, verbatim)
    // =========================================================================

    private static Map<String, String> drrAOutput;
    private static List<String> drrAGenErrors;

    @BeforeAll
    static void generateCells() throws IOException {
        if (cellAAvailable()) {
            List<String> errs = new ArrayList<>();
            drrAOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "5.61.0", CELL_A_ROOT), errs);
            drrAGenErrors = errs;
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

    private static void collect(List<String> sink, List<GenerationException> errors) {
        if (errors != null) {
            errors.forEach(e -> sink.add(e.getTargetPath() + " - " + e));
        }
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n");
    }

}
