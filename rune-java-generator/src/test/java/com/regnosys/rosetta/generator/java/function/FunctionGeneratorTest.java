package com.regnosys.rosetta.generator.java.function;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.scoping.JavaClassScope;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.generator.java.types.RGeneratedJavaClass;
import com.regnosys.rosetta.symbols.RLinkingResult;
import com.regnosys.rosetta.symbols.RWorkspace;
import com.rosetta.model.lib.annotations.RosettaReport;
import com.rosetta.model.lib.annotations.RuneLabelProvider;
import com.rosetta.util.types.JavaClass;
import com.rosetta.util.types.JavaGenericTypeDeclaration;
import com.rosetta.util.types.JavaParameterizedType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic tests for FunctionGenerator: verifies the orchestrator can iterate
 * all functions in the CDM corpus and produce non-empty output.
 */
class FunctionGeneratorTest {

    private static final Path CDM_ROSETTA_DIR = Path.of("../common-domain-model/rosetta-source/src/main/rosetta");
    private static final Path BUILTINS_DIR = Path.of("../test-corpus/rune-dsl-builtins");
    private static final JavaTypeUtil TYPE_UTIL = new JavaTypeUtil();
    private static final JavaTypeTranslator TYPE_TRANSLATOR = new JavaTypeTranslator(TYPE_UTIL);

    static boolean cdmAvailable() {
        return Files.isDirectory(CDM_ROSETTA_DIR);
    }

    @Test
    @EnabledIf("cdmAvailable")
    void generates_functions_from_corpus() throws IOException {
        var corpus = loadFullCorpus();
        var gm = new GeneratorModel(corpus.workspace());
        var gen = new FunctionGenerator(gm, TYPE_TRANSLATOR, TYPE_UTIL);

        Map<String, String> output = new LinkedHashMap<>();
        gen.generate(output);

        // Should generate at least some functions
        assertTrue(output.size() > 0, "Expected at least one function generated");

        // Report counts
        long functionFiles = output.keySet().stream()
                .filter(k -> k.contains("/functions/"))
                .count();
        System.out.println("FunctionGenerator: " + output.size() + " total files, "
                + functionFiles + " in /functions/ packages");

        // Sanity: expect a significant number of functions from CDM
        assertTrue(functionFiles >= 100,
                "Expected at least 100 function files, got " + functionFiles);
    }

    @Test
    @EnabledIf("cdmAvailable")
    void generates_max_function() throws IOException {
        var corpus = loadFullCorpus();
        var gm = new GeneratorModel(corpus.workspace());
        var gen = new FunctionGenerator(gm, TYPE_TRANSLATOR, TYPE_UTIL);

        Map<String, String> output = new LinkedHashMap<>();
        gen.generate(output);

        // Check Max.java exists
        String maxPath = "cdm/base/math/functions/Max.java";
        assertTrue(output.containsKey(maxPath),
                "Expected Max.java in output. Keys sample: "
                + output.keySet().stream().limit(10).toList());
        String maxCode = output.get(maxPath);
        assertFalse(maxCode.isEmpty(), "Max.java should not be empty");

        // Basic structural checks
        assertTrue(maxCode.contains("package cdm.base.math.functions;"),
                "Max.java should have correct package");
        assertTrue(maxCode.contains("class Max"),
                "Max.java should contain class declaration");
        assertTrue(maxCode.contains("RosettaFunction"),
                "Max.java should implement RosettaFunction");
    }

    @Test
    @EnabledIf("cdmAvailable")
    void function_output_contains_expected_elements() throws IOException {
        var corpus = loadFullCorpus();
        var gm = new GeneratorModel(corpus.workspace());
        var gen = new FunctionGenerator(gm, TYPE_TRANSLATOR, TYPE_UTIL);

        Map<String, String> output = new LinkedHashMap<>();
        gen.generate(output);

        // Check a function with known structure: Create_AcceptedWorkflowStep
        // This function has inputs, output, and operations
        String targetPath = "cdm/event/workflow/functions/Create_AcceptedWorkflowStep.java";
        if (output.containsKey(targetPath)) {
            String code = output.get(targetPath);
            assertTrue(code.contains("evaluate("), "Should have evaluate method");
            assertTrue(code.contains("doEvaluate("), "Should have doEvaluate method");
            assertTrue(code.contains("Default extends"), "Should have Default inner class");
        }
    }

    @Test
    @EnabledIf("cdmAvailable")
    void condition_rendering_produces_validate_lambda_pattern() throws IOException {
        var corpus = loadFullCorpus();
        var gm = new GeneratorModel(corpus.workspace());
        var gen = new FunctionGenerator(gm, TYPE_TRANSLATOR, TYPE_UTIL);

        Map<String, String> output = new LinkedHashMap<>();
        gen.generate(output);

        // DeliveryAmount.java has pre-conditions
        String deliveryPath = "cdm/legaldocumentation/csa/functions/DeliveryAmount.java";
        assertTrue(output.containsKey(deliveryPath), "DeliveryAmount.java should be generated");
        String deliveryCode = output.get(deliveryPath);
        assertTrue(deliveryCode.contains("// pre-conditions"),
                "DeliveryAmount should have pre-conditions comment");
        assertTrue(deliveryCode.contains("conditionValidator.validate(() ->"),
                "DeliveryAmount should have conditionValidator.validate(() -> pattern");
        assertTrue(deliveryCode.contains("\"\");"),
                "DeliveryAmount condition should end with empty definition");

        // StandardizedScheduleNotionalCurrency.java has post-conditions with non-empty definition
        String ssnPath = "cdm/margin/schedule/functions/StandardizedScheduleNotionalCurrency.java";
        assertTrue(output.containsKey(ssnPath), "StandardizedScheduleNotionalCurrency.java should be generated");
        String ssnCode = output.get(ssnPath);
        assertTrue(ssnCode.contains("// post-conditions"),
                "StandardizedScheduleNotionalCurrency should have post-conditions comment");
        assertTrue(ssnCode.contains("conditionValidator.validate(() ->"),
                "StandardizedScheduleNotionalCurrency should have conditionValidator.validate(() -> pattern");
        assertTrue(ssnCode.contains("\"Ensure Currency is an ISO 3-Letter Currency Code \");"),
                "StandardizedScheduleNotionalCurrency condition should have definition text");

        // FxMarkToMarket.java has pre-conditions with a definition
        String fxPath = "cdm/event/position/functions/FxMarkToMarket.java";
        assertTrue(output.containsKey(fxPath), "FxMarkToMarket.java should be generated");
        String fxCode = output.get(fxPath);
        assertTrue(fxCode.contains("// pre-conditions"),
                "FxMarkToMarket should have pre-conditions comment");
        assertTrue(fxCode.contains("conditionValidator.validate(() ->"),
                "FxMarkToMarket should have conditionValidator.validate(() -> pattern");
        assertTrue(fxCode.contains("\"The settlementPayout on the contract must exist.\");"),
                "FxMarkToMarket condition should have definition text");

        // Verify condition validator field is injected
        assertTrue(deliveryCode.contains("@Inject protected ConditionValidator conditionValidator;"),
                "DeliveryAmount should inject ConditionValidator");
    }

    // =========================================================================
    // Phase X T4: buildClassWithBaseInterface tests
    // =========================================================================

    /**
     * Phase X T4 — BC-pass-through guard. When {@code buildClassWithBaseInterface}
     * is called with empty {@code baseInterfaces} + empty {@code annotations}
     * + {@code renderAsReportFunction=false}, the rendered output MUST be
     * byte-identical to the standard function emission path. This locks the
     * AT-RISK invariant that adding the 3 new {@code FunctionTemplateModel}
     * fields and conditional {@code templates/java-function.stg} branches
     * does NOT perturb standard function byte output. Critical for D11
     * byte-parity (881 unit
     * tests + 14-cell corpus regression).
     */
    @Test
    @EnabledIf("cdmAvailable")
    void buildClassWithBaseInterface_unchangedWhenBaseAndAnnotationsEmpty() throws IOException {
        var corpus = loadFullCorpus();
        var gm = new GeneratorModel(corpus.workspace());
        var gen = new FunctionGenerator(gm, TYPE_TRANSLATOR, TYPE_UTIL);

        // Render the entire CDM corpus via the standard path
        Map<String, String> standardOutput = new LinkedHashMap<>();
        gen.generate(standardOutput);

        // Pick a known-standard function (non-dispatch, present in CDM)
        String maxPath = "cdm/base/math/functions/Max.java";
        assertTrue(standardOutput.containsKey(maxPath),
                "Expected Max.java in standard output for BC-guard test");
        String standardMax = standardOutput.get(maxPath);

        // Find the matching RFunction by class FQN match
        RFunction maxFunc = findFunctionByClassFqn(gm, "cdm.base.math.functions.Max");
        assertNotNull(maxFunc, "Max function not found in corpus");

        // Render via the new path with empty/empty/false (BC pass-through)
        RGeneratedJavaClass<?> clazz = TYPE_TRANSLATOR.toFunctionJavaClass(gm.symbolId(maxFunc));
        JavaClassScope scope = JavaClassScope.createAndRegisterIdentifier(clazz);
        String extendedMax = gen.buildClassWithBaseInterface(
                maxFunc, clazz, /* isAbstract */ true,
                List.of(), Map.of(), /* renderAsReportFunction */ false, scope);

        // BC INVARIANT: byte-identical
        assertEquals(standardMax, extendedMax,
                "BC-guard violation: buildClassWithBaseInterface with empty extension "
                + "fields must produce byte-identical output to standard emission. "
                + "AT-RISK: any diff means the new templates/java-function.stg template branches or "
                + "FunctionTemplateModel ctor delegation has perturbed standard "
                + "function byte output, breaking D11 byte-parity.");
    }

    /**
     * Phase X T4 — base interface emission. When {@code renderAsReportFunction}
     * is true and {@code baseInterfaces} is non-empty, the {@code implements}
     * clause is REPLACED with the supplied base interfaces (used by Rule/Report
     * generators to emit {@code implements ReportFunction<I, O>}).
     *
     * <p>T4.0.5 C1 update: the implements clause MUST render every base
     * interface (raw type + type arguments) as simple names — see
     * {@link #buildClassWithBaseInterface_pinsSimpleNameAndImportCollection()}
     * for the byte-parity invariant. This test asserts the substitution
     * mechanic (RosettaFunction replaced) + the simple-name rendering.
     */
    @Test
    @EnabledIf("cdmAvailable")
    void buildClassWithBaseInterface_emitsBaseInterfaceInClassDecl() throws IOException {
        var corpus = loadFullCorpus();
        var gm = new GeneratorModel(corpus.workspace());
        var gen = new FunctionGenerator(gm, TYPE_TRANSLATOR, TYPE_UTIL);

        RFunction func = firstStandardFunction(gm);
        assertNotNull(func, "No standard functions found in corpus");

        RGeneratedJavaClass<?> clazz = TYPE_TRANSLATOR.toFunctionJavaClass(gm.symbolId(func));
        JavaClassScope scope = JavaClassScope.createAndRegisterIdentifier(clazz);

        // Build a synthetic 2-arg parameterized base interface (stand-in for
        // ReportFunction<I,O> until T5/T6 wires the real type from rune-runtime).
        // java.util.function.Function<T,R> is a convenient 2-arg generic on
        // the test classpath.
        JavaGenericTypeDeclaration<java.util.function.Function> fnDecl =
                JavaGenericTypeDeclaration.from(java.util.function.Function.class);
        JavaParameterizedType base = JavaParameterizedType.from(
                fnDecl, JavaClass.from(String.class), JavaClass.from(Integer.class));

        String code = gen.buildClassWithBaseInterface(
                func, clazz, /* isAbstract */ false,
                List.of(base), Map.of(),
                /* renderAsReportFunction */ true, scope);

        // T4.0.5 C1: implements clause uses simple names for both raw type
        // AND type arguments — java.lang.String/Integer must render as
        // String/Integer (java.lang import-filtered).
        String classLine = extractClassDeclLine(code);
        assertTrue(classLine.startsWith("public abstract class ")
                && classLine.contains(" implements Function<String, Integer>"),
                "Expected '<class decl> implements Function<String, Integer>' in rendered output; got:\n"
                + classLine);
        assertFalse(code.contains("implements RosettaFunction"),
                "Standard 'implements RosettaFunction' must be REPLACED, not appended; got:\n"
                + classLine);
        assertFalse(code.contains("java.lang.String") || code.contains("java.lang.Integer"),
                "FQN leak: implements clause / imports must not contain java.lang.* "
                + "(legacy plugin always import-filters java.lang). Code:\n" + code);
    }

    /**
     * Phase X T4.0.5 C1 — byte-parity hazard guard. When emitting a non-default
     * implements clause via {@code renderAsReportFunction=true}, the base
     * interface raw-type FQN MUST appear in the {@code import} section AND
     * the implements clause MUST render the simple name. This locks the
     * upstream legacy plugin's emission shape (see
     * {@code test-corpus/drr/drr-6.34.1/.../CollateralEnrichmentDataRule.java}
     * lines 6 + 15 — {@code import com.rosetta.model.lib.reports.ReportFunction;}
     * + {@code public abstract class ... implements ReportFunction<CollateralReportInstruction, EnrichmentData>}).
     *
     * <p>Without this invariant, T5/T6 D11 byte-parity at T7/T8 would fail
     * across 4,784 entries because the fork would emit {@code implements
     * com.rosetta.model.lib.reports.ReportFunction<...>} inline (FQN) while
     * the legacy plugin emits a simple-name + import.
     */
    @Test
    @EnabledIf("cdmAvailable")
    void buildClassWithBaseInterface_pinsSimpleNameAndImportCollection() throws IOException {
        var corpus = loadFullCorpus();
        var gm = new GeneratorModel(corpus.workspace());
        var gen = new FunctionGenerator(gm, TYPE_TRANSLATOR, TYPE_UTIL);

        RFunction func = firstStandardFunction(gm);
        assertNotNull(func, "No standard functions found in corpus");

        RGeneratedJavaClass<?> clazz = TYPE_TRANSLATOR.toFunctionJavaClass(gm.symbolId(func));
        JavaClassScope scope = JavaClassScope.createAndRegisterIdentifier(clazz);

        // Use a non-java.lang type argument so we can verify both the raw type
        // AND the type-argument package are added to imports. java.util.Map
        // is a 2-arg generic; we parameterize Function<Map<K,V>,Integer> to
        // exercise nested-arg traversal too. For simplicity we use a flat
        // 2-arg pair: Function<java.util.UUID, java.math.BigDecimal>.
        JavaGenericTypeDeclaration<java.util.function.Function> fnDecl =
                JavaGenericTypeDeclaration.from(java.util.function.Function.class);
        JavaParameterizedType base = JavaParameterizedType.from(
                fnDecl,
                JavaClass.from(java.util.UUID.class),
                JavaClass.from(java.math.BigDecimal.class));

        String code = gen.buildClassWithBaseInterface(
                func, clazz, /* isAbstract */ false,
                List.of(base), Map.of(),
                /* renderAsReportFunction */ true, scope);

        // Invariant #1: imports list contains the base interface raw-type FQN
        assertTrue(code.contains("import java.util.function.Function;"),
                "C1 invariant violated: imports list must contain the base "
                + "interface raw-type FQN 'java.util.function.Function'. Code:\n"
                + code);
        // Invariant #2: imports list contains each non-java.lang type-arg FQN
        assertTrue(code.contains("import java.util.UUID;"),
                "C1 invariant violated: imports list must contain the type-arg "
                + "FQN 'java.util.UUID'. Code:\n" + code);
        assertTrue(code.contains("import java.math.BigDecimal;"),
                "C1 invariant violated: imports list must contain the type-arg "
                + "FQN 'java.math.BigDecimal'. Code:\n" + code);
        // Invariant #3: implements clause uses simple names (no FQN leak)
        String classLine = extractClassDeclLine(code);
        assertTrue(classLine.contains(" implements Function<UUID, BigDecimal>"),
                "C1 invariant violated: implements clause must use simple names "
                + "for raw type AND every type argument. Got:\n" + classLine);
        assertFalse(classLine.contains("java.util.function.Function")
                || classLine.contains("java.util.UUID")
                || classLine.contains("java.math.BigDecimal"),
                "C1 invariant violated: implements clause must NOT leak FQN. Got:\n"
                + classLine);
    }

    /**
     * Phase X T4 — class-level annotation emission. The {@code annotations}
     * map values are emitted as {@code @<fragment>} lines immediately above
     * the {@code @ImplementedBy} line.
     */
    @Test
    @EnabledIf("cdmAvailable")
    void buildClassWithBaseInterface_emitsAnnotationsOnClass() throws IOException {
        var corpus = loadFullCorpus();
        var gm = new GeneratorModel(corpus.workspace());
        var gen = new FunctionGenerator(gm, TYPE_TRANSLATOR, TYPE_UTIL);

        RFunction func = firstStandardFunction(gm);
        assertNotNull(func, "No standard functions found in corpus");

        RGeneratedJavaClass<?> clazz = TYPE_TRANSLATOR.toFunctionJavaClass(gm.symbolId(func));
        JavaClassScope scope = JavaClassScope.createAndRegisterIdentifier(clazz);

        // PR #72 R9 F22 — the annotation MAP KEYS are load-bearing: they flow
        // into FunctionGenerator.mergeBaseInterfaceImports (via the
        // annotationKeys parameter) which calls
        // addReflectiveCanonicalIfImportable on each Class<?> to add the
        // annotation's canonical FQN to the emitted imports. Using
        // placeholder keys (Override.class / Deprecated.class) would emit
        // @RosettaReport / @RuneLabelProvider in the class body WITHOUT
        // matching imports — the rendered source would not compile. The
        // test contract is: keys MUST match the rendered annotation simple
        // names so import collection and rendering stay consistent. Using
        // the real annotation classes both exercises the import-emission
        // contract and produces a result that would compile if written to
        // disk.
        Map<Class<?>, String> annotations = new LinkedHashMap<>();
        annotations.put(RosettaReport.class,
                "RosettaReport(namespace=\"test.ns\", body=\"BodyA\", corpusList={\"C1\", \"C2\"})");
        annotations.put(RuneLabelProvider.class,
                "RuneLabelProvider(labelProvider=FooLabels.class)");

        String code = gen.buildClassWithBaseInterface(
                func, clazz, /* isAbstract */ false,
                List.of(), annotations,
                /* renderAsReportFunction */ false, scope);

        assertTrue(code.contains(
                "@RosettaReport(namespace=\"test.ns\", body=\"BodyA\", corpusList={\"C1\", \"C2\"})"),
                "Expected @RosettaReport annotation rendered verbatim; got:\n"
                + extractClassDeclBlock(code));
        assertTrue(code.contains("@RuneLabelProvider(labelProvider=FooLabels.class)"),
                "Expected @RuneLabelProvider annotation rendered verbatim; got:\n"
                + extractClassDeclBlock(code));

        // R9 F22 — verify the imports list also contains the annotation FQNs.
        // This is the contract the keys are supposed to exercise.
        assertTrue(code.contains("import " + RosettaReport.class.getCanonicalName() + ";"),
                "Expected @RosettaReport import emitted via annotation-keys contract; "
                + "got code:\n" + code);
        assertTrue(code.contains("import " + RuneLabelProvider.class.getCanonicalName() + ";"),
                "Expected @RuneLabelProvider import emitted via annotation-keys contract; "
                + "got code:\n" + code);

        // T4.0.5 F1 — no-blank-line whitespace contract between the last
        // @<annotation> line and @ImplementedBy. Pin the exact adjacency so a
        // future template edit cannot silently insert a blank line that would
        // break byte-parity at T7/T8 D11.
        assertTrue(code.contains(
                "@RuneLabelProvider(labelProvider=FooLabels.class)\n"
                + "@ImplementedBy("),
                "F1 invariant violated: last @<annotation> line must be "
                + "immediately followed by @ImplementedBy (no blank line). "
                + "Got:\n" + extractClassDeclBlock(code));
    }

    /** Helper: extract the class declaration line for assertion diagnostics. */
    private static String extractClassDeclLine(String code) {
        return code.lines()
                .filter(line -> line.contains("public abstract class") || line.contains("public class"))
                .findFirst()
                .orElse("<no class declaration line found>");
    }

    /** Helper: extract the annotation block + class declaration for diagnostics. */
    private static String extractClassDeclBlock(String code) {
        return code.lines()
                .filter(line -> line.startsWith("@") || line.contains("public abstract class")
                        || line.contains("public class"))
                .reduce("", (a, b) -> a + "\n" + b);
    }

    /**
     * Locate the first standard (non-dispatch) RFunction in the GeneratorModel,
     * iterating the same way {@code FunctionGenerator.generateWithErrors} does.
     * Returns {@code null} if none found.
     */
    private static RFunction firstStandardFunction(GeneratorModel gm) {
        for (RModel model : gm.files()) {
            if (!gm.shouldGenerate(model)) continue;
            for (var element : model.rootElements()) {
                if (element instanceof RFunction func && func.dispatch().isEmpty()) {
                    return func;
                }
            }
        }
        return null;
    }

    /**
     * Locate an RFunction by its emitted Java class FQN (e.g.
     * {@code "cdm.base.math.functions.Max"}). Returns {@code null} if not found.
     */
    private static RFunction findFunctionByClassFqn(GeneratorModel gm, String fqn) {
        for (RModel model : gm.files()) {
            if (!gm.shouldGenerate(model)) continue;
            for (var element : model.rootElements()) {
                if (element instanceof RFunction func) {
                    var jc = TYPE_TRANSLATOR.toFunctionJavaClass(gm.symbolId(func));
                    if (fqn.equals(jc.getCanonicalName().withDots())) {
                        return func;
                    }
                }
            }
        }
        return null;
    }

    private RLinkingResult loadFullCorpus() throws IOException {
        List<RModel> models = new ArrayList<>();

        // Load builtins first
        if (Files.isDirectory(BUILTINS_DIR)) {
            try (var stream = Files.list(BUILTINS_DIR)) {
                stream.filter(p -> p.toString().endsWith(".rosetta"))
                      .sorted()
                      .forEach(p -> {
                          try { models.add(AstBuilder.buildFromFile(p)); }
                          catch (Exception e) {
                              System.err.println("Builtin parse error: " + p.getFileName()
                                      + " — " + e.getMessage());
                          }
                      });
            }
        }

        // Load all CDM rosetta files
        try (var stream = Files.list(CDM_ROSETTA_DIR)) {
            stream.filter(p -> p.toString().endsWith(".rosetta"))
                  .sorted()
                  .forEach(p -> {
                      try {
                          RModel model = AstBuilder.buildFromFile(p);
                          model.setVersion("0.0.0.master-SNAPSHOT");
                          models.add(model);
                      } catch (Exception e) {
                          System.err.println("Parse error: " + p.getFileName()
                                  + " — " + e.getMessage());
                      }
                  });
        }

        return RWorkspace.build(models);
    }
}
