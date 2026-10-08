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

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.GenerationException;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.reports.ReportGenerator;
import com.regnosys.rosetta.generator.java.reports.RuleGenerator;
import com.regnosys.rosetta.generator.java.spi.IRGeneration;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.symbols.RWorkspace;

/**
 * SEAT 31, law 4 rung (i) -- facet {@code choiceOptionNavLadderDeepHop} (F15): the SET-seat
 * option-nav ladder ({@code FunctionExpressionRenderer.renderChoiceOptionNavLadderOrNull},
 * PR #394) loses its SINGLE-HOP limitation. Each case type now resolves through the
 * NESTED-choice option tree via {@code ChoiceSwitchSupport.findChoiceOptionPath} -- the SAME
 * walk the two ControlFlowHandler ladder forms consult (the #396 conditional-arm ladder and
 * the datarule RETURN ladder), PROMOTED to the shared support class this seat so the three
 * seats can never disagree on which case types are reachable (the two-halves-agree law).
 *
 * <p><b>The mechanism, from the seat-31 LAW-75 probe ({@code verdicts31.md} §law 4).</b>
 * drr {@code UnderlierBasketIdentifier}: {@code [P31-OPTGATE] itemKind=RChoiceTypeRef
 * gate=true} x4 -- the option-nav gate ADMITS and the ladder RUNS, then declines mid-render
 * because {@code Basket}/{@code NonTransferableProduct} are not DIRECT options of
 * {@code Underlier} (they sit one hop down, under {@code Observable} [metadata address, the
 * ReferenceWithMeta wrapper] and {@code Product}); the fork falls through to the instanceof
 * branch ({@code [P31-SWHOIST] site=bareSubject}), which cannot compile against a model
 * choice (an option is NOT a subtype -- LAW 74). Golden renders the DEEP option nav with the
 * meta coercion. Three rungs land here:
 * <ul>
 *   <li><b>path membership</b> -- {@code findChoiceOptionPath} replaces the direct-option
 *       set; the TERMINAL hop must still resolve to the guard's own JavaClass (canonical
 *       identity -- the #396 cp1 namespace catch carried over);</li>
 *   <li><b>the deep/meta nav</b> -- the ControlFlowHandler #396 hop loop verbatim:
 *       registered lambda params (ancestor collisions escape -- golden {@code _underlier}
 *       against the fn input) and the guarded {@code Type coercion} deref for META options,
 *       its param registered on the STATEMENT scope;</li>
 *   <li><b>the wrapper-deref assign arm</b> -- a case VALUE whose Mapper item is a meta
 *       wrapper assigned into the fn's bare single value-typed output renders upstream's
 *       deref assign (golden UBI {@code final FieldWithMetaString fieldWithMetaString =
 *       ...\n\t.first().get(); if (...) ... .getValue();}); its rhs tolerates chain-link
 *       continuations, re-indented at emission.</li>
 * </ul>
 * The direct single-hop BARE-option rungs keep the #394 raw-{@code subjLC} idiom
 * byte-for-byte (corpus_c2 locks golden cdm6 {@code CriteriaMatchesAssetType} whole).
 *
 * <p><b>Charter:</b> {@code UnderlierBasketIdentifier} x4 whole (drr 7.0-7.3 FUNCTION) --
 * corpus_c1 locks the 7.0.0 cell whole-golden; the other three cells ride the seat chain's
 * matrix digest + LAW-80 per-file accounting.
 *
 * <p><b>LAW 77.</b> The seat is FER-side, inherited by the IR route (renders flow
 * identically; no IR re-implementation of the ladder). corpus_control2 measures
 * BOTH-ROUTES-vs-GOLDEN on the healed carrier under {@code -Pir-on}.
 *
 * <p><b>Whole-cell LAW-79 note, disclosed:</b> the UNION whole-cell instrument for this law
 * is the seat chain's matrix digest + the whole-matrix checkpoint's per-file accounting
 * (both routes), not a per-suite token scan; corpus_c2's whole-golden CMAT lock is the
 * frozen-population control in the other direction.
 *
 * <p><b>MUTATIONS (LAW 66/76) -- MEASURED (LAW 82) at the seat-31 chain head {@code f2a4d5c0}:</b>
 * <ul>
 *   <li><b>m-law4a</b> ({@code lanes31.py --law4a}, the deep-hop branch severed -- every
 *       {@code deepOrMeta} path declines): MEASURED 6/3F/1S = a1, a2, corpus_c1; corpus_c2 PASSED
 *       -- exactly the claim (MATCH): the frozen single-hop idiom is untouched, the two branches
 *       are separable.</li>
 *   <li><b>m-law4a-deref</b> ({@code lanes31.py --law4a-deref}, the wrapper-deref arm severed --
 *       {@code armMetaDeref} forced false): MEASURED 6/3F/1S = a1, a2, corpus_c1; corpus_c2 PASSED
 *       -- exactly the claim (MATCH; a2's arm also derefs, so it moves with the sever).</li>
 * </ul>
 * RED at the seat's base {@code 0c8fc7dc4} (both routes): a1, a2, corpus_c1 (+ corpus_control2 on
 * {@code -Pir-on}); e1 and corpus_c2 GREEN at RED, the decline direction. GREEN at the head
 * 6/0F/1skip default, 6/0F/0skip {@code -Pir-on}.
 */
class ChoiceOptionLadderDeepHopSeatTest {

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

    private static final String UBI =
            "drr/base/trade/basket/functions/UnderlierBasketIdentifier.java";
    private static final String CMAT =
            "cdm/product/collateral/functions/CriteriaMatchesAssetType.java";

    /** Cell A = drr 7.0.0 (the UBI carrier); cell B = cdm 6.20.6 (the frozen CMAT lock). */
    private static final Path CELL_A_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path GOLDEN_A = CELL_A_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CELL_B_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.6");
    private static final Path GOLDEN_B = CELL_B_ROOT.resolve("rosetta-source/src/generated/java");

    static boolean cellAAvailable() {
        return Files.isDirectory(GOLDEN_A);
    }

    static boolean cellBAvailable() {
        return Files.isDirectory(GOLDEN_B);
    }

    static boolean cellAAndIrProviderAvailable() {
        return cellAAvailable() && irProviderOnClasspath();
    }

    // =========================================================================
    // Fixtures
    // =========================================================================

    private static final String MODEL = """
            namespace census.seat31l4
            version "1.0.0"

            type Basket2:
                ids string (0..*)
                    [metadata scheme]

            type Index2:
                nm string (0..1)

            type Prod2:
                code string (0..1)

            choice Observable2:
                Basket2
                Index2

            choice Outer:
                Observable2
                    [metadata reference]
                Prod2

            type Stray:
                code2 string (0..1)

            func DeepPick: <"a1 - the UBI shape: a DEEP case (Basket2 under Observable2, the META hop) + a direct bare case">
                inputs:
                    u Outer (0..1)
                output:
                    out string (0..1)
                set out:
                    u switch
                        Basket2 then item -> ids first,
                        Prod2 then item -> code,
                        default empty

            func SingleMetaPick: <"a2 - the SINGLE-hop META option case">
                inputs:
                    u Outer (0..1)
                output:
                    out string (0..1)
                set out:
                    u switch
                        Observable2 then item -> Basket2 -> ids first,
                        default empty

            func StrayPick: <"e1 - a case type NOT reachable in the option tree: the ladder must DECLINE">
                inputs:
                    u Outer (0..1)
                output:
                    out string (0..1)
                set out:
                    u switch
                        Stray then item -> code2,
                        default empty
            """;

    /**
     * a1 -- the deep META hop + the wrapper-deref arm, the UBI shape end-to-end: the Basket2
     * case probes {@code <ReferenceWithMetaObservable2>map("getObservable2", ...)} + the
     * guarded {@code Type coercion} + {@code <Basket2>map("getBasket2", ...)}, and its arm
     * derefs the {@code FieldWithMetaString} into the bare String output. The Prod2 case
     * stays the #394 single-hop idiom in the SAME switch (the two branches co-exist).
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_deepMetaHopLadderRendersWithDerefArm() throws IOException {
        String out = fixtureFunction("DeepPick");
        assertContains(out, ".<ReferenceWithMetaObservable2>map(\"getObservable2\", ");
        assertContains(out, ">map(\"Type coercion\", ");
        assertContains(out, ".<Basket2>map(\"getBasket2\", ");
        assertContains(out, "final FieldWithMetaString fieldWithMetaString = ");
        assertContains(out, "out = fieldWithMetaString.getValue();");
        // the direct bare case keeps the #394 raw-subjLC idiom
        assertContains(out, ".<Prod2>map(\"getProd2\", outer -> outer.getProd2())");
        assertTrue(!out.contains("instanceof"),
                "the ladder must serve every case - no instanceof fall-through:\n" + out);
    }

    /**
     * a2 -- the SINGLE-hop META option case takes the wrapper witness + coercion (the old
     * gate ADMITTED it and rendered the bare witness against the wrapper-returning getter --
     * non-compiling, so no green file can carry the old form; LAW 74).
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_singleMetaHopTakesTheWrapperWitness() throws IOException {
        String out = fixtureFunction("SingleMetaPick");
        assertContains(out, ".<ReferenceWithMetaObservable2>map(\"getObservable2\", ");
        assertContains(out, ">map(\"Type coercion\", ");
        assertContains(out, "final MapperS<Observable2> observable2 = ");
        assertTrue(!out.contains("switchArgument.<Observable2>map(\"getObservable2\","
                        + " outer -> outer.getObservable2())"),
                "the META option must NOT render the bare witness (the pre-law non-compiling"
                + " form):\n" + out);
    }

    /**
     * e1 -- the decline-lock: a case type NOT reachable as an option path must keep the
     * pre-law fall-through (never the option ladder). A decline OBSERVED, not a uniquely
     * discriminating control: no lane moves it (both law-4a lanes SEVER, and e1 is green at
     * RED), so an over-widening lane -- {@code findChoiceOptionPath} forced to admit any case
     * type -- is BANKED to S32 (the #603 review, SF-9). The draft's escape hatch (accept ANY
     * generation error when the fixture did not render) is gone: the fixture must render,
     * error-free, and carry neither ladder token.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void e1_unreachableCaseTypeDeclinesTheLadder() throws IOException {
        String out = fixtureFunction("StrayPick");
        assertTrue(!out.contains("map(\"Type coercion\""),
                "an unreachable case type must not render the option ladder:\n" + out);
        assertTrue(!out.contains(".<Stray>map(\"getStray\""),
                "an unreachable case type must not render an option probe:\n" + out);
    }

    /** corpus_c1 -- the UnderlierBasketIdentifier whole-file heal (drr 7.0.0 FUNCTION). */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_drr700UnderlierBasketIdentifierMatchesGolden() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");
        List<String> own = drrAGenErrors.stream().filter(e -> e.contains(UBI)).toList();
        assertTrue(own.isEmpty(), "generation errors for " + UBI + ": " + own);
        String gen = drrAOutput.get(UBI);
        assertNotNull(gen, "not generated: " + UBI);
        assertEquals(normalize(Files.readString(GOLDEN_A.resolve(UBI))), normalize(gen),
                "UnderlierBasketIdentifier must byte-match golden - seat 31 law 4 rung (i):"
                + " the FER option-nav ladder gains the deep-hop loop");
    }

    /**
     * corpus_c2 -- the FROZEN single-hop population: cdm6 CriteriaMatchesAssetType keeps its
     * #394 bytes whole (the raw-subjLC idiom untouched by the deep extension).
     */
    @Test
    @EnabledIf("cellBAvailable")
    void corpus_c2_cdm6206CriteriaMatchesAssetTypeStaysGolden() throws IOException {
        assertNotNull(cdmBOutput, "cdm 6.20.6 generation did not run");
        String gen = cdmBOutput.get(CMAT);
        assertNotNull(gen, "not generated: " + CMAT);
        assertEquals(normalize(Files.readString(GOLDEN_B.resolve(CMAT))), normalize(gen),
                "CriteriaMatchesAssetType must STAY byte-golden - the #394 single-hop idiom"
                + " is byte-frozen under the seat-31 deep extension");
    }

    /**
     * corpus_control2 -- LAW 77, BOTH-ROUTES-vs-GOLDEN: the IR route (which inherits the
     * FER seat) must render the healed carrier golden-identical too.
     */
    @Test
    @EnabledIf("cellAAndIrProviderAvailable")
    void corpus_control2_irRouteMatchesGoldenForUbi() throws IOException {
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", CELL_A_ROOT),
                new ArrayList<>());
        assertEquals(normalize(Files.readString(GOLDEN_A.resolve(UBI))),
                normalize(irOut.get(UBI)), "IR route vs GOLDEN: " + UBI);
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
        RModel main = AstBuilder.buildFromString(MODEL, "seat31l4.rosetta");
        main.setVersion("0.0.0.test");
        List<RModel> models = new ArrayList<>();
        models.add(main);
        models.addAll(loadBuiltinsOnly());
        RWorkspace workspace = RWorkspace.build(models).workspace();
        GeneratorModel gm = new GeneratorModel(workspace,
                m -> "census.seat31l4".equals(m.namespace()));
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
            throw new AssertionError("[ChoiceOptionLadderDeepHopSeatTest] builtins parse"
                    + " failures: " + String.join("; ", failures));
        }
        return models;
    }

    // =========================================================================
    // Corpus harness (the ReceiverRenderTypingSeatTest cell generator, verbatim)
    // =========================================================================

    private static Map<String, String> drrAOutput;
    private static List<String> drrAGenErrors;
    private static Map<String, String> cdmBOutput;

    @BeforeAll
    static void generateCells() throws IOException {
        if (cellAAvailable()) {
            List<String> errs = new ArrayList<>();
            drrAOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", CELL_A_ROOT), errs);
            drrAGenErrors = errs;
        }
        if (cellBAvailable()) {
            cdmBOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6", CELL_B_ROOT),
                    new ArrayList<>());
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
