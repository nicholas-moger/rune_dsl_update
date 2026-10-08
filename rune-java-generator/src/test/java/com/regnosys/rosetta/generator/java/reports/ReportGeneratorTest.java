package com.regnosys.rosetta.generator.java.reports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static java.lang.ref.Reference.reachabilityFence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.generator.java.types.RGeneratedJavaClass;
import com.regnosys.rosetta.symbols.RLinkingResult;
import com.regnosys.rosetta.symbols.RWorkspace;
import com.rosetta.model.lib.functions.RosettaFunction;

/**
 * Phase X T6 unit tests for {@link ReportGenerator}.
 *
 * <p>Mirrors {@code RuleGeneratorTest}'s {@link AstBuilder#buildFromString} +
 * {@link RWorkspace#build(List)} + direct-lifecycle-call pattern. Each test
 * exercises the generator's three lifecycle methods ({@code streamObjects} /
 * {@code createTypeRepresentation} / {@code generate}) on a synthetic
 * {@code .rosetta} corpus that declares a {@code body} + one or more
 * {@code corpus} entries + a {@code report} root element.
 *
 * <p>Grammar paste-quote (verified against
 * {@code rune-parser/src/main/antlr4/.../RosettaParser.g4} lines 798-806):
 * <ul>
 *   <li>{@code report <body> <corpus>... in T+1 from <Type> when <Cond>
 *       with type <OutType>} — produces an
 *       {@link com.regnosys.rosetta.ast.regulatory.RReport RReport} root
 *       element.</li>
 *   <li>The {@code body} is the first {@code qualifiedName} of the
 *       {@code regulatoryDocumentReference}, the {@code corpus...} list is
 *       the remaining names per
 *       {@code regulatoryDocumentReference: qualifiedName qualifiedName+
 *       rosettaSegmentRef*} at line 722-724.</li>
 *   <li>{@code body Authority <Name>} (top-level declaration at line 676-678)
 *       and {@code corpus <Kind> <Name>} (line 680-683) are the bindings the
 *       {@code report} references.</li>
 * </ul>
 *
 * <p>Per spec § 4.1 this class covers ≥ 6 named scenarios:
 * <ol>
 *   <li>{@code @RosettaReport} annotation emission with namespace + body +
 *       single-element corpusList</li>
 *   <li>{@code @RuneLabelProvider} annotation emission with labelProvider
 *       class reference</li>
 *   <li>{@code streamObjects} filters RReport only (skips RFunction +
 *       RRule + RAnnotation)</li>
 *   <li>{@code ReportFunction<I,O>} base interface emission</li>
 *   <li>Multi-corpus corpusList (e.g. {@code corpusList={"X", "Y"}})</li>
 *   <li>Multiple reports in one model emit independently with distinct
 *       class names</li>
 * </ol>
 *
 * <p>Plus a cardinality edge-case test for the
 * {@code generate}-side fail-fast contract on synthetic-bypass mutation
 * (mirrors RuleGeneratorTest's pattern).
 */
class ReportGeneratorTest {

    /**
     * Repository-root-anchored test-corpus search root. Surefire forks each
     * test JVM with {@code user.dir} pinned to the test module
     * ({@code rune-java-generator}); {@code "../test-corpus/..."} resolves
     * to the repo root via the module's parent. Mirrors the convention used
     * by {@code RuleGeneratorTest} + {@code LabelProviderGeneratorTest} +
     * {@code ChoiceObjectGeneratorTest}.
     */
    private static final Path REPO_ROOT =
            Path.of(System.getProperty("user.dir")).resolve("..").normalize();

    /**
     * Two-root builtins search so synthetic-source tests can resolve
     * {@code string} / {@code int} / annotation declarations. Mirrors
     * {@code RuleGeneratorTest.BUILTINS_SEARCH_ROOTS}.
     */
    private static final List<Path> BUILTINS_SEARCH_ROOTS = List.of(
            REPO_ROOT.resolve("test-corpus/rune-dsl-builtins/rune-runtime/src/main/resources/model"),
            REPO_ROOT.resolve("rune-dsl/rune-runtime/src/main/resources/model")
    );

    private final JavaTypeUtil typeUtil = new JavaTypeUtil();
    private final JavaTypeTranslator typeTranslator = new JavaTypeTranslator(typeUtil);

    // === Test 1: @RosettaReport annotation emission ==========================

    @Test
    void generate_emitsRosettaReportAnnotation_withSingleCorpus() throws IOException {
        // A single-corpus report — exercises namespace, body, single-element
        // corpusList all in one emission.
        FixtureResult fx = loadFixture(asicMarginCorpus());
        try {
            String code = generateFor(fx);

            assertTrue(code.contains("import com.rosetta.model.lib.annotations.RosettaReport;"),
                    "imports must contain RosettaReport FQN; got:\n" + code);
            assertTrue(code.contains("@RosettaReport("),
                    "class must carry @RosettaReport annotation; got:\n" + code);
            assertTrue(code.contains("namespace=\"drr.regulation.asic.rewrite.margin\""),
                    "namespace argument must be the report's namespace; got:\n" + code);
            assertTrue(code.contains("body=\"ASIC\""),
                    "body argument must match the regulatoryBody reference; got:\n" + code);
            assertTrue(code.contains("corpusList={\"Margin\"}"),
                    "single-corpus corpusList must render as {\"Margin\"}; got:\n" + code);
        } finally {
            reachabilityFence(fx.linkingResult);
        }
    }

    // === Test 2: @RuneLabelProvider annotation emission ======================

    @Test
    void generate_emitsRuneLabelProviderAnnotation_pointingAtLabelClass() throws IOException {
        FixtureResult fx = loadFixture(asicMarginCorpus());
        try {
            String code = generateFor(fx);

            assertTrue(code.contains("import com.rosetta.model.lib.annotations.RuneLabelProvider;"),
                    "imports must contain RuneLabelProvider FQN; got:\n" + code);
            assertTrue(code.contains("@RuneLabelProvider(labelProvider=ASICMarginLabelProvider.class)"),
                    "class must carry @RuneLabelProvider with simple-name labelProvider .class "
                    + "reference; got:\n" + code);
            // The label-provider FQN must also be importable so the .class
            // literal resolves — derived name is body+corpus alphanumeric
            // (ASICMargin) under <ns>.labels.
            assertTrue(code.contains(
                    "import drr.regulation.asic.rewrite.margin.labels.ASICMarginLabelProvider;"),
                    "imports must contain the LabelProvider canonical name so the "
                    + ".class literal resolves; got:\n" + code);
        } finally {
            reachabilityFence(fx.linkingResult);
        }
    }

    // === Test 3: streamObjects filters RReport ==============================

    @Test
    void streamObjects_filtersRReport_skipsRFunctionAndRRule() throws IOException {
        // Corpus: 1 report + 1 plain func + 1 reporting rule + 1 plain type.
        // streamObjects must emit ONLY the report (mapped via fromReport).
        String source = String.join("\n",
                "namespace com.example.reports",
                "",
                "body Authority TestBody",
                "corpus Specifications TestCorpus",
                "",
                "type Trade:",
                "    id string (1..1)",
                "",
                "type ReportOut:",
                "    value string (1..1)",
                "",
                "func Plain:",
                "    inputs: x string (1..1)",
                "    output: result string (1..1)",
                "",
                "reporting rule TradeIdRule from Trade:",
                "    extract id",
                "",
                "report TestBody TestCorpus in T+1",
                "    from Trade",
                "    when IsEligible",
                "    with type ReportOut",
                "",
                "eligibility rule IsEligible from Trade:",
                "    filter id exists"
        );
        FixtureResult fx = loadFixture(source);
        try {
            ReportGenerator gen = newGenerator(fx);
            List<? extends RFunction> emitted = gen.streamObjects(fx.model).toList();

            assertEquals(1, emitted.size(),
                    "streamObjects must emit exactly one RFunction (the report); got names: "
                    + emitted.stream().map(RFunction::name).toList());
            // The synthetic name is body+corpus alphanumeric (TestBody+TestCorpus).
            assertEquals("TestBodyTestCorpus", emitted.get(0).name(),
                    "bridged RFunction's name must be body+corpus alphanumeric");
            // The bridge uses RFunction.fromReport — guard that originReport
            // is populated (the back-pointer is what createTypeRepresentation
            // relies on for namespace resolution).
            assertTrue(emitted.get(0).originReport().isPresent(),
                    "bridged RFunction must carry originReport back-pointer");
        } finally {
            reachabilityFence(fx.linkingResult);
        }
    }

    // === Test 4: ReportFunction<I,O> base interface emission =================

    @Test
    void generate_emitsReportFunctionBaseInterface() throws IOException {
        // The generate path delegates to FunctionGenerator.buildClassWithBaseInterface
        // with renderAsReportFunction=true. The emitted source must:
        //   - import com.rosetta.model.lib.reports.ReportFunction
        //   - "implements ReportFunction<...>" (simple-name; FQN goes to imports
        //      per T4.0.5 C1 invariant)
        //   - NOT contain "implements RosettaFunction" (replaced, not appended)
        FixtureResult fx = loadFixture(asicMarginCorpus());
        try {
            String code = generateFor(fx);

            assertTrue(code.contains("import com.rosetta.model.lib.reports.ReportFunction;"),
                    "imports must contain ReportFunction FQN; got:\n" + code);
            assertTrue(code.contains(" implements ReportFunction<"),
                    "implements clause must use ReportFunction simple name with type args; "
                    + "got:\n" + code);
            assertFalse(code.contains("implements RosettaFunction"),
                    "default 'implements RosettaFunction' must be REPLACED, not appended; "
                    + "got:\n" + code);
            // Sanity-check the class declaration is abstract + carries the
            // body+corpus name with ReportFunction suffix.
            assertTrue(code.contains("public abstract class ASICMarginReportFunction "),
                    "class decl must be 'public abstract class ASICMarginReportFunction '; "
                    + "got:\n" + code);
        } finally {
            reachabilityFence(fx.linkingResult);
        }
    }

    // === Test 5: FQN derivation via T6.0.5 origin-dispatched toFunctionJavaClass ===

    @Test
    void createTypeRepresentation_derivesFqnUnderReportsPackage() throws IOException {
        // Per T6.0.5 REPORT-origin dispatch, the class routes to
        // <namespace>.reports/<body+corpus>ReportFunction via the private
        // toJavaReportClass router (mirrors upstream lines 122-126).
        // Replaces the ad-hoc toReportFunctionJavaClass(DottedPath, RReport)
        // entry point that shipped at T6.
        FixtureResult fx = loadFixture(asicMarginCorpus());
        try {
            ReportGenerator gen = newGenerator(fx);
            RFunction f = gen.streamObjects(fx.model).findFirst().orElseThrow();

            RGeneratedJavaClass<? extends RosettaFunction> clazz =
                    gen.createTypeRepresentation(f);
            assertNotNull(clazz, "createTypeRepresentation must return non-null");

            assertEquals("ASICMarginReportFunction", clazz.getSimpleName(),
                    "simple name must be <body><corpus...>ReportFunction; got "
                    + clazz.getSimpleName());
            assertEquals("drr.regulation.asic.rewrite.margin.reports",
                    clazz.getPackageName().withDots(),
                    "package must be <namespace>.reports per T6.0.5 "
                    + "REPORT-origin dispatch; got " + clazz.getPackageName().withDots());
        } finally {
            reachabilityFence(fx.linkingResult);
        }
    }

    // === Test 6: Multi-corpus list ==========================================

    @Test
    void generate_emitsMultiCorpusList() throws IOException {
        // A two-corpus report. The corpusList rendering MUST be
        // {"EMIR_REFIT", "MIFIR"} with comma+space separator and double
        // quotes around each element (mirrors upstream Xtend's
        // `FOR corpus SEPARATOR ", "` rendering).
        String source = String.join("\n",
                "namespace com.example.reports",
                "",
                "body Authority ESMA",
                "corpus Specifications EMIR_REFIT",
                "corpus Specifications MIFIR",
                "",
                "type Trade:",
                "    id string (1..1)",
                "",
                "type RefitReport:",
                "    value string (1..1)",
                "",
                "report ESMA EMIR_REFIT MIFIR in T+1",
                "    from Trade",
                "    when IsEligible",
                "    with type RefitReport",
                "",
                "eligibility rule IsEligible from Trade:",
                "    filter id exists"
        );
        FixtureResult fx = loadFixture(source);
        try {
            String code = generateFor(fx);

            assertTrue(code.contains("body=\"ESMA\""),
                    "body argument must be 'ESMA'; got:\n" + code);
            assertTrue(code.contains("corpusList={\"EMIR_REFIT\", \"MIFIR\"}"),
                    "multi-corpus corpusList must render as "
                    + "{\"EMIR_REFIT\", \"MIFIR\"} with `, ` separator; got:\n" + code);
            // Class name should be body+corpus1+corpus2+ReportFunction.
            assertTrue(code.contains("public abstract class ESMAEMIR_REFITMIFIRReportFunction "),
                    "class name must be body+all-corpora+ReportFunction; got:\n" + code);
        } finally {
            reachabilityFence(fx.linkingResult);
        }
    }

    // === Test 7: Multiple reports in one model ==============================

    @Test
    void streamObjects_multipleReports_emitsAllIndependently() throws IOException {
        // Two reports — both must be streamed; each must produce a distinct
        // generated class (different simple name).
        String source = String.join("\n",
                "namespace com.example.reports",
                "",
                "body Authority TestBody",
                "corpus Specifications CorpusA",
                "corpus Specifications CorpusB",
                "",
                "type Trade:",
                "    id string (1..1)",
                "",
                "type OutA:",
                "    value string (1..1)",
                "",
                "type OutB:",
                "    value string (1..1)",
                "",
                "report TestBody CorpusA in T+1",
                "    from Trade",
                "    when IsEligible",
                "    with type OutA",
                "",
                "report TestBody CorpusB in T+1",
                "    from Trade",
                "    when IsEligible",
                "    with type OutB",
                "",
                "eligibility rule IsEligible from Trade:",
                "    filter id exists"
        );
        FixtureResult fx = loadFixture(source);
        try {
            ReportGenerator gen = newGenerator(fx);
            List<? extends RFunction> emitted = gen.streamObjects(fx.model).toList();

            assertEquals(2, emitted.size(),
                    "both reports must be streamed; got names: "
                    + emitted.stream().map(RFunction::name).toList());

            List<String> classNames = new ArrayList<>();
            for (RFunction f : emitted) {
                classNames.add(gen.createTypeRepresentation(f).getSimpleName());
            }
            assertTrue(classNames.contains("TestBodyCorpusAReportFunction"),
                    "TestBodyCorpusAReportFunction class must be generated; got " + classNames);
            assertTrue(classNames.contains("TestBodyCorpusBReportFunction"),
                    "TestBodyCorpusBReportFunction class must be generated; got " + classNames);
        } finally {
            reachabilityFence(fx.linkingResult);
        }
    }

    // === Test 8: Cardinality edge case (spec § 4.1) =========================

    /**
     * Spec § 4.1 — "Rule with no inputs/outputs (edge case) — verify error
     * handling vs upstream". Mirrors {@code RuleGeneratorTest}'s test 7
     * verbatim — the synthetic {@link RFunction} produced by
     * {@link RFunction#fromReport(com.regnosys.rosetta.ast.regulatory.RReport)}
     * always sets one input + one output, so the error-handling branches in
     * {@link ReportGenerator#generate} are unreachable from normal grammar
     * input. This test exercises them directly by mutating the synthetic's
     * inputs / output after construction — locking the fail-fast diagnostic
     * contract so a future refactor that bypasses the factory invariant
     * produces a workspace-construction-time error rather than a downstream
     * {@code IndexOutOfBoundsException} / silent miscompilation.
     */
    @Test
    void generate_emptyInputsOrMissingOutput_throwsWithDiagnostic() throws IOException {
        FixtureResult fx = loadFixture(asicMarginCorpus());
        try {
            ReportGenerator gen = newGenerator(fx);

            // -- 8a: empty inputs -------------------------------------------
            RFunction emptyInputs = gen.streamObjects(fx.model).findFirst().orElseThrow();
            emptyInputs.inputs().clear();
            RGeneratedJavaClass<? extends RosettaFunction> clazz1 =
                    gen.createTypeRepresentation(emptyInputs);
            IllegalStateException isEmpty = assertThrows(IllegalStateException.class,
                    () -> gen.generate(emptyInputs, clazz1, "1.0"),
                    "generate() must fail-fast when synthetic RFunction has no inputs");
            assertTrue(isEmpty.getMessage().contains("no input attribute"),
                    "diagnostic must name the missing surface 'no input attribute'; got: "
                    + isEmpty.getMessage());
            assertTrue(isEmpty.getMessage().contains("ASICMargin"),
                    "diagnostic must include the function name for triage; got: "
                    + isEmpty.getMessage());

            // -- 8b: missing output ----------------------------------------
            RFunction missingOutput = gen.streamObjects(fx.model).findFirst().orElseThrow();
            missingOutput.setOutput(null);
            RGeneratedJavaClass<? extends RosettaFunction> clazz2 =
                    gen.createTypeRepresentation(missingOutput);
            IllegalStateException isMissing = assertThrows(IllegalStateException.class,
                    () -> gen.generate(missingOutput, clazz2, "1.0"),
                    "generate() must fail-fast when synthetic RFunction has no output");
            assertTrue(isMissing.getMessage().contains("no output attribute"),
                    "diagnostic must name the missing surface 'no output attribute'; got: "
                    + isMissing.getMessage());
            assertTrue(isMissing.getMessage().contains("ASICMargin"),
                    "diagnostic must include the function name for triage; got: "
                    + isMissing.getMessage());
        } finally {
            reachabilityFence(fx.linkingResult);
        }
    }

    // === Helpers ===============================================================

    /** Bundle of per-fixture state. */
    private static final class FixtureResult {
        final RModel model;
        final RLinkingResult linkingResult;
        final GeneratorModel generatorModel;
        final FunctionGenerator functionGenerator;

        FixtureResult(RModel model, RLinkingResult linkingResult,
                      GeneratorModel generatorModel,
                      FunctionGenerator functionGenerator) {
            this.model = model;
            this.linkingResult = linkingResult;
            this.generatorModel = generatorModel;
            this.functionGenerator = functionGenerator;
        }
    }

    /**
     * Construct a {@link ReportGenerator} wired to the fixture's per-call
     * dependencies. Each test calls this fresh — the generator is stateless
     * across calls.
     */
    private ReportGenerator newGenerator(FixtureResult fx) {
        return new ReportGenerator(fx.generatorModel, typeTranslator, fx.functionGenerator);
    }

    /**
     * The {@code drr/regulation/asic/rewrite/margin} ASIC-Margin golden
     * report shape — reused across the 4 tests that exercise full-emission
     * paths so failures localize easily and the corpus stays minimal.
     */
    private static String asicMarginCorpus() {
        return String.join("\n",
                "namespace drr.regulation.asic.rewrite.margin",
                "",
                "body Authority ASIC",
                "corpus Dissemination Margin",
                "",
                "type CollateralReportInstruction:",
                "    id string (1..1)",
                "",
                "type ASICMarginReport:",
                "    value string (1..1)",
                "",
                "report ASIC Margin in T+1",
                "    from CollateralReportInstruction",
                "    when IsCollateralReportable",
                "    with type ASICMarginReport",
                "",
                "eligibility rule IsCollateralReportable from CollateralReportInstruction:",
                "    filter id exists"
        );
    }

    /**
     * Parse the synthetic source + load builtins + build the workspace +
     * construct GeneratorModel + FunctionGenerator. Mirrors
     * {@code RuleGeneratorTest.loadFixture} verbatim.
     */
    private FixtureResult loadFixture(String source) throws IOException {
        RModel model = AstBuilder.buildFromString(source, "test.rosetta");
        model.setVersion("0.0.0.test");
        List<RModel> models = new ArrayList<>();
        models.add(model);
        models.addAll(loadBuiltinsOnly());
        RLinkingResult linkingResult = RWorkspace.build(models);
        GeneratorModel gm = new GeneratorModel(linkingResult.workspace());
        FunctionGenerator fg = new FunctionGenerator(gm, typeTranslator, typeUtil);
        return new FixtureResult(model, linkingResult, gm, fg);
    }

    /**
     * Locate the first report streamed by the generator + run the
     * {@code createTypeRepresentation} + {@code generate} pipeline.
     */
    private String generateFor(FixtureResult fx) {
        ReportGenerator gen = newGenerator(fx);
        RFunction f = gen.streamObjects(fx.model)
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "no report was streamed; either the corpus has no RReport "
                        + "root element or the bridge is mis-wired. Verify the "
                        + "test source."));
        return gen.generate(f, gen.createTypeRepresentation(f), "1.0");
    }

    /**
     * Mirrors {@code RuleGeneratorTest.loadBuiltinsOnly} verbatim — union
     * over {@link #BUILTINS_SEARCH_ROOTS}, dedup by filename, aggregate
     * parse failures into an {@link AssertionError} (no silent swallowing
     * per PR #68 R1 F12).
     */
    private List<RModel> loadBuiltinsOnly() throws IOException {
        List<String> failures = new ArrayList<>();
        List<RModel> models = loadBuiltinsOnly(failures);
        if (!failures.isEmpty()) {
            throw new AssertionError(
                    "[ReportGeneratorTest] loadBuiltinsOnly: "
                    + failures.size() + " parse failure(s) — first: "
                    + failures.get(0)
                    + (failures.size() > 1
                        ? " (and " + (failures.size() - 1) + " more — full list: "
                          + String.join("; ", failures.subList(1, failures.size())) + ")"
                        : ""));
        }
        return models;
    }

    private List<RModel> loadBuiltinsOnly(List<String> failures) throws IOException {
        Map<String, Path> resolved = new LinkedHashMap<>();
        for (Path root : BUILTINS_SEARCH_ROOTS) {
            if (!Files.isDirectory(root)) continue;
            try (var stream = Files.walk(root)) {
                stream.filter(p -> p.toString().endsWith(".rosetta"))
                      .forEach(p -> resolved.putIfAbsent(p.getFileName().toString(), p));
            }
        }
        List<RModel> models = new ArrayList<>();
        resolved.values().stream()
                .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                .forEach(p -> {
                    try { models.add(AstBuilder.buildFromFile(p)); }
                    catch (Exception e) {
                        failures.add(p + " — " + e);
                    }
                });
        return models;
    }
}
