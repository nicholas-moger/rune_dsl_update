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
 * SEAT 31, law 4b -- facet {@code choiceSwitchLambdaOptionGetter} (F15): the in-lambda
 * CHOICE-keyed {@code switch} ({@code CollectionHandler.compileChoiceSwitchBlockLambda},
 * PR #226) gains the MODEL-CHOICE OPTION-GETTER branch. A {@code choice} subject's case
 * guards lower to option-nav null-tests with a MAPPER-typed case local -- an option is NOT
 * a subtype. <b>Measured, not argued (LAW 74):</b> the #226 {@code instanceof} ladder COMPILES
 * -- every model type is a generated interface, and {@code javac31/Seat31Pre.java} measured 0
 * errors on the pre-law carrier -- but no implementation of the {@code Payout} choice interface
 * implements an option interface, so every arm is false at RUNTIME: the divergence is SEMANTIC.
 * Green-safety is measured, not constructed: golden never emits the instanceof form for a
 * model-choice subject, so every site the branch reaches was already a divergent mismatch, and
 * LAW 80 read 14/0/7/0/88 with ZERO entered on both routes. (The draft's "cannot compile"
 * wording was withdrawn at the review of #603, SF-4.)
 *
 * <p><b>The mechanism, from the seat-31 LAW-75 probe ({@code verdicts31.md} §law 4):</b>
 * {@code [P31-SWLAMBDA] choice=Payout choiceKind=RChoiceTypeRef gate=true} x4 -- the
 * in-lambda compile emits the instanceof ladder + the unconditional {@code switchArgument}
 * hoist on a subject the FER SET-seat gate (#394) would route to the option-getter form.
 * Golden probes {@code item.<SettlementPayout>map("getSettlementPayout", payout ->
 * payout.getSettlementPayout()).get() != null}, declares the Mapper-typed local, and the
 * case body renders off it with the escaped inner param
 * ({@code _settlementPayout -> _settlementPayout.getUnderlier()}) and the evaluate-arg
 * {@code <local>.get()} collapse. The option nav compiles through the SAME synthesized
 * implicit-item feature call the MULTI twin
 * ({@code compileModelChoiceSwitchListBlockLambda}, PR #365) uses -- the twin's MapperC
 * gate declines exactly this MapperS shape, so the two seats stay disjoint. A decline
 * falls through to the instanceof emission: today's bytes. <b>v3.2 seat 7 (F11, PR #628):
 * the decline direction e1 locked was a WRONG emission</b> -- the seat's widened arm law
 * ({@code CollectionHandler.admissibleBlockSwitchCaseBody}: the bare item admitted at the
 * option getter beside the fn call) renders the bare-item case body in the option-getter form,
 * and the released 9.83.0 plugin, put to e1's fixture verbatim as the hold-out group
 * {@code choice-switch-in-lambda-bare-item} (22 goldens, deterministic x2), renders EXACTLY that
 * form -- byte-identical on the fork's first run ({@code target/v32-seat7-instruments/scratch/v5.status}).
 * e1 pins the golden's lines now; the fall-through the ladder still takes for a declined option
 * form has NO oracle-witnessed carrier left in this class.
 *
 * <p><b>Charter:</b> {@code CustomBasketCodeRule} drr 7.0.0 iosco cde x1 whole -- corpus_c1
 * locks it whole-golden; LAW 79/80: the whole-cell instrument is the seat chain's matrix
 * digest + the whole-matrix checkpoint's per-file accounting, disclosed here.
 *
 * <p><b>LAW 77.</b> The seat is CollectionHandler + NavigationHandler side (the hoisted naming
 * rung), inherited by the IR route.
 * corpus_control2 measures BOTH-ROUTES-vs-GOLDEN on the healed carrier under
 * {@code -Pir-on}.
 *
 * <p><b>MUTATIONS (LAW 66/76) -- MEASURED (LAW 82) at the seat-31 chain head {@code f2a4d5c0}.</b>
 * The draft listed one lane; the lane table ({@code mut31.py}) carries two, the second severing the
 * naming rung this law hoisted, and both are transcribed here from their logs:
 * <ul>
 *   <li><b>m-law4b</b> ({@code lanes31.py --law4b}, the branch hook severed -- the RChoiceTypeRef
 *       subject falls straight to the instanceof emission): MEASURED 4/2F/1S = a1, corpus_c1; e1
 *       PASSED -- exactly the claim (MATCH; e1 asserts the decline direction, which severing
 *       cannot invert).</li>
 *   <li><b>m-law4b-name</b> ({@code lanes31.py --law4b-name}, the HOISTED live-bound
 *       case-narrowed naming rung severed -- the #396 rung falls back below the rule-from-type
 *       rung): MEASURED 4/1F/1S = corpus_c1 ONLY (the inner params regress to {@code payout});
 *       a1 PASSED -- exactly the claim (MATCH): the fixture is a FUNCTION, so no rule-from-type
 *       rung shadows it, which is why the naming hoist needed the corpus, not the fixture, as its
 *       witness.</li>
 * </ul>
 * RED at the seat's base {@code 0c8fc7dc4} (both routes): a1, corpus_c1 (+ corpus_control2 on
 * {@code -Pir-on}); e1 GREEN at RED. GREEN at the head 4/0F/1skip default, 4/0F/0skip
 * {@code -Pir-on}.
 */
class ChoiceSwitchLambdaOptionGetterSeatTest {

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

    private static final String CBC =
            "drr/standards/iosco/cde/version1/basket/reports/CustomBasketCodeRule.java";

    /** Cell A = drr 7.0.0 -- the CustomBasketCodeRule carrier. */
    private static final Path CELL_A_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
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
            namespace census.seat31l4b
            version "1.0.0"

            type OptA:
                fieldA string (0..1)

            type OptB:
                fieldB string (0..1)

            choice Outer2:
                OptA
                OptB

            func FnA:
                inputs:
                    a OptA (0..1)
                output:
                    r string (0..1)
                set r:
                    a -> fieldA

            func FnB:
                inputs:
                    b OptB (0..1)
                output:
                    r string (0..1)
                set r:
                    b -> fieldB

            func PickAll: <"a1 - fn-call case bodies over a MODEL-CHOICE lambda item: the option-getter form">
                inputs:
                    outers Outer2 (0..*)
                output:
                    outs string (0..*)
                set outs:
                    outers extract
                        (switch
                            OptA then FnA(item),
                            OptB then FnB(item),
                            default empty)

            func PickItems: <"e1 - bare implicit-ITEM case bodies: NOT the witnessed fn-call shape, the option branch DECLINES to the instanceof emission (today's bytes)">
                inputs:
                    outers Outer2 (0..*)
                output:
                    outs OptA (0..*)
                set outs:
                    outers extract
                        (switch
                            OptA then item,
                            default empty)
            """;

    /**
     * a1 -- the option-getter block lambda end-to-end: the null-guard probes the lambda
     * param directly (no {@code switchArgument} hoist), each case probes the option nav and
     * declares the Mapper-typed local, and the fn-call body renders the evaluate-arg
     * {@code <local>.get()} collapse.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_optionGetterLambdaRenders() throws IOException {
        String out = fixtureFunction("PickAll");
        assertContains(out, "if (item.get() == null) {");
        assertContains(out, ".<OptA>map(\"getOptA\", ");
        assertContains(out, "final MapperS<OptA> optA = item.<OptA>map(\"getOptA\", ");
        assertContains(out, "final MapperS<OptB> optB = item.<OptB>map(\"getOptB\", ");
        assertContains(out, "fnA.evaluate(optA.get())");
        assertContains(out, "fnB.evaluate(optB.get())");
        assertTrue(!out.contains("instanceof"),
                "a model-choice subject must take the option-getter form, never instanceof:\n"
                        + out);
        assertTrue(!out.contains("switchArgument"),
                "the option-getter form probes the lambda param directly - no switchArgument"
                        + " hoist:\n" + out);
    }

    /**
     * e1 -- WAS the decline-lock (v3.1 seat 31): a bare implicit-ITEM case body was NOT the
     * witnessed fn-call shape, so the option branch declined and the pre-law {@code instanceof}
     * emission stood ("today's bytes -- the fall-through direction"). <b>Re-pinned at v3.2 seat 7
     * (F11, PR #628) to the ORACLE'S form:</b> the chain of record {@code s7a} caught this pin red
     * under the seat's widened arm law, the released 9.83.0 plugin was put to this very fixture as
     * the hold-out group {@code choice-switch-in-lambda-bare-item} (PickItems = the model below,
     * re-namespaced), and it renders the OPTION-GETTER form -- the null-guard on the lambda param,
     * the option probe, the Mapper-typed local returned as the case value -- which the fork's
     * post-seat render matches byte for byte (golden
     * {@code holdout-goldens/choice-switch-in-lambda-bare-item/test/chswitchbare/functions/PickItems.java}).
     * Every expected string below is the golden's own line; the instanceof ladder this test used
     * to pin was a WRONG emission (every arm false at runtime -- the class javadoc's own diagnosis).
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void e1_bareItemCaseBodyTakesTheOptionGetterForm_oracleConfirmed() throws IOException {
        String out = fixtureFunction("PickItems");
        assertContains(out, "if (item.get() == null) {");
        assertContains(out, "if (item.<OptA>map(\"getOptA\", outer2 -> outer2.getOptA()).get() != null) {");
        assertContains(out, "final MapperS<OptA> optA = item.<OptA>map(\"getOptA\", outer2 -> outer2.getOptA());");
        assertContains(out, "return optA;");
        assertContains(out, "return MapperS.<OptA>ofNull();");
        assertTrue(!out.contains("instanceof"),
                "a model-choice subject takes the option-getter form, never instanceof (the oracle's form):\n" + out);
        assertTrue(!out.contains("switchArgument"),
                "the option-getter form probes the lambda param directly - no switchArgument hoist:\n" + out);
    }

    /** corpus_c1 -- the CustomBasketCodeRule whole-file heal (drr 7.0.0 iosco cde). */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_drr700CustomBasketCodeRuleMatchesGolden() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");
        List<String> own = drrAGenErrors.stream().filter(e -> e.contains(CBC)).toList();
        assertTrue(own.isEmpty(), "generation errors for " + CBC + ": " + own);
        String gen = drrAOutput.get(CBC);
        assertNotNull(gen, "not generated: " + CBC);
        assertEquals(normalize(Files.readString(GOLDEN_A.resolve(CBC))), normalize(gen),
                "CustomBasketCodeRule must byte-match golden - seat 31 law 4b: the in-lambda"
                + " choice switch takes the option-getter form");
    }

    /**
     * corpus_control2 -- LAW 77, BOTH-ROUTES-vs-GOLDEN: the IR route (which inherits the
     * CollectionHandler seat) must render the healed carrier golden-identical too.
     */
    @Test
    @EnabledIf("cellAAndIrProviderAvailable")
    void corpus_control2_irRouteMatchesGoldenForCbc() throws IOException {
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", CELL_A_ROOT),
                new ArrayList<>());
        assertEquals(normalize(Files.readString(GOLDEN_A.resolve(CBC))),
                normalize(irOut.get(CBC)), "IR route vs GOLDEN: " + CBC);
    }

    // =========================================================================
    // Fixture harness (the Law2GuardsSeatTest renderer, trimmed)
    // =========================================================================

    private record Render(Map<String, String> output, List<String> errors) {}

    private static Render rendered;

    private static Render render() throws IOException {
        if (rendered != null) {
            return rendered;
        }
        RModel main = AstBuilder.buildFromString(MODEL, "seat31l4b.rosetta");
        main.setVersion("0.0.0.test");
        List<RModel> models = new ArrayList<>();
        models.add(main);
        models.addAll(loadBuiltinsOnly());
        RWorkspace workspace = RWorkspace.build(models).workspace();
        GeneratorModel gm = new GeneratorModel(workspace,
                m -> "census.seat31l4b".equals(m.namespace()));
        JavaTypeUtil typeUtil = new JavaTypeUtil();
        JavaTypeTranslator tt = new JavaTypeTranslator(typeUtil);
        FunctionGenerator fg = new FunctionGenerator(gm, tt, typeUtil);
        Map<String, String> out = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        fg.generateWithErrors(out)
                .forEach(e -> errors.add(e.getTargetPath() + " - " + e));
        rendered = new Render(out, errors);
        return rendered;
    }

    private static String fixtureFunction(String fnName) throws IOException {
        Render r = render();
        String path = fnName + ".java";
        List<String> own = r.errors().stream().filter(e -> e.contains(path)).toList();
        assertTrue(own.isEmpty(), "the generator reported errors for " + path + ": " + own);
        String out = lookupOrNull(r.output(), path);
        assertNotNull(out, "not generated: " + path + " (have: " + r.output().keySet() + ")");
        return out;
    }

    private static String lookupOrNull(Map<String, String> output, String suffix) {
        return output.entrySet().stream()
                .filter(e -> e.getKey().endsWith(suffix))
                .map(Map.Entry::getValue)
                .findFirst().orElse(null);
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
            throw new AssertionError("[ChoiceSwitchLambdaOptionGetterSeatTest] builtins parse"
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
                    new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", CELL_A_ROOT), errs);
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
