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
 * SEAT 25, law C — facet {@code ruleCallMapperCWitness}: <b>the MapperC element witness of a MULTI
 * rule invocation reads the CALLEE's inferred output when the reference node's own inference is
 * MISSING</b> — golden drr 7.x iosco {@code UnderlierIDOtherRule}/{@code UnderlierIDOtherSourceRule}
 * write {@code MapperC.<AssetIdentifier>of(underlierProductIdentifierOtherRule.evaluate(input))}
 * (with the {@code cdm.base.staticdata.asset.common.AssetIdentifier} import) where the fork wrote
 * {@code MapperC.<Object>of(…)} and dropped the import: the reference node's workspace inference is
 * MISSING at these seats (the drr 7.x refactor's point-free callee heads), and MISSING translated to
 * {@code Object}.
 *
 * <p><b>The law (ONE read — LAW 69).</b> The #274 MapperC arm's witness
 * ({@code ReferenceHandler.renderImplicitRuleInvocation}) consults
 * {@code HandlerHelper.ruleInferredOutputRType} — the ONE rule-output read (#593) — when the node
 * inference is MISSING; a null callee read (its body MISSING too) falls back to the node read
 * (today's bytes, the decline polarity). A PRESENT node inference keeps the node read byte-for-byte.
 *
 * <p><b>The golden-domain law (the seat-25 census, {@code golden-scan25.txt}):</b>
 * {@code MapperC.<Object>of(} and {@code MapperS.<Object>of(} appear ZERO times over the 24 of 25
 * cells that census reaches — iso20022 1.38.0 is UNSCANNED there (the artefact's own
 * {@code NO GOLDEN TREE} line: its goldens live under {@code rosetta-source/target/classes/…}, not
 * the scanned {@code src/generated/java}) — the review's A-2, the control-domain law applied to the
 * CLAIM, never asserted as absence across an unscanned cell. An {@code Object}-witnessed wrap is
 * never a correct render where measured; control0 pins the two carrier cells' zeros DIRECTLY (this
 * suite's own scans, not the census) and control1 holds the fork to the same zero.
 *
 * <p><b>RED at the pre-seat blob — MEASURED</b> (the law commit's parent, this suite kept): c1, c2,
 * corpus_control1 (the fork's two {@code MapperC.<Object>of} sites); b1 and corpus_control0 GREEN in
 * both states. Under {@code -Pir-on} also corpus_control2 stays GREEN in both states (the seat is
 * the shared renderer — the Object count is equal-by-route in both states).
 *
 * <p><b>LAW 66/76 mutations</b> (each applied → run → reverted; the failing sets MEASURED by the
 * seat's receipts chain — LAW 82): (i) the MISSING consult reverted to the node read — 3F: c1, c2,
 * corpus_control1; (ii) the consult widened to EVERY reference (the callee read unconditionally) —
 * <b>0F MEASURED</b>: at every corpus reference the five seat suites witness in the two cells, a
 * PRESENT node inference and the callee's inferred output translate to the SAME witness type, so
 * the always-consult variant is byte-indistinguishable there — the MISSING-only gate is
 * conservative hardening (the node read stays the authority wherever it exists), kept as the
 * measured-zero record (the seat-24 {@code f27-ii} precedent), not a load-bearing gate.
 */
class RuleCallMapperCWitnessSeatTest {

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

    /** A plain (meta-less) MULTI inner rule behind a then-stage: the node-read fallback pin. */
    private static final String MODEL = """
            namespace census.seat25f6c
            version "1.0.0"

            type Leg:
                name string (0..1)

            type Instr:
                legs Leg (0..*)
                allowed boolean (0..1)

            func IsAllowable:
                inputs:
                    i Instr (0..1)
                output:
                    result boolean (1..1)
                set result:
                    i -> allowed = True

            reporting rule InnerNames from Instr: <"a MULTI rule with a resolvable plain output">
                extract legs -> name
                    as "names"

            reporting rule B1NodeRead from Instr: <"b1 - a PRESENT node inference keeps the node-read witness">
                filter IsAllowable
                then InnerNames
                    as "b1"
            """;

    /** b1 — a present node inference keeps the node-read witness byte-for-byte (no Object, no callee read change). */
    @Test
    @EnabledIf("builtinsAvailable")
    void b1_presentNodeInferenceKeepsNodeRead() throws IOException {
        String out = rule("B1NodeReadRule.java");
        assertContains(out, "MapperC.<String>of(innerNamesRule.evaluate(");
        assertNotContains(out, "MapperC.<Object>of(");
    }

    // =========================================================================
    // Part C — the corpus (drr 7.0.0: the carriers + the zero-Object controls)
    // =========================================================================

    private static final Path DRR561_CELL_ROOT = Path.of("../test-corpus/drr/drr-5.61.0");
    private static final Path DRR561_GOLDEN = DRR561_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path DRR7_CELL_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path DRR7_GOLDEN = DRR7_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    static boolean drr7Available() {
        return Drr7Corpus.gate(Files.isDirectory(DRR7_GOLDEN), RuleCallMapperCWitnessSeatTest.class);
    }

    static boolean drr561Available() {
        return Files.isDirectory(DRR561_GOLDEN);
    }

    static boolean drr7AndIrProviderAvailable() {
        return drr7Available() && irProviderOnClasspath();
    }

    private static final String UNDERLIER_ID_OTHER =
            "drr/standards/iosco/cde/version3/underlier/reports/UnderlierIDOtherRule.java";
    private static final String UNDERLIER_ID_OTHER_SOURCE =
            "drr/standards/iosco/cde/version3/underlier/reports/UnderlierIDOtherSourceRule.java";

    @Test
    @EnabledIf("drr7Available")
    void corpus_c1_underlierIdOtherByteIdentical() throws IOException {
        lock7(UNDERLIER_ID_OTHER);
    }

    @Test
    @EnabledIf("drr7Available")
    void corpus_c2_underlierIdOtherSourceByteIdentical() throws IOException {
        lock7(UNDERLIER_ID_OTHER_SOURCE);
    }

    /** control0 — golden is the oracle: ZERO Object-witnessed wraps in BOTH carrier cells' frozen goldens. */
    @Test
    @EnabledIf("drr7Available")
    void corpus_control0_goldenCarriesZeroObjectWitnessWraps() throws IOException {
        assertEquals(0, objectWraps(readGoldenTree(DRR7_GOLDEN)), "golden drr 7.0.0 Object-witness wraps");
        if (drr561Available()) {
            assertEquals(0, objectWraps(readGoldenTree(DRR561_GOLDEN)), "golden drr 5.61.0 Object-witness wraps");
        }
    }

    /** control1 — the FORK's WHOLE generated drr 7.0.0 cell carries ZERO Object-witnessed wraps (every kind). */
    @Test
    @EnabledIf("drr7Available")
    void corpus_control1_forkDrr7CarriesZeroObjectWitnessWraps() throws IOException {
        assertNotNull(drr7Output, "drr 7.0.0 generation did not run");
        assertEquals(List.of(), drr7GenErrors, "drr 7.0.0 reported a generation error — the scan is incomplete");
        assertEquals(0, objectWraps(drr7Output), "fork drr 7.0.0 Object-witness wraps");
    }

    /** control2 — LAW 77 route parity: the IR-route drr 7.0.0 cell carries the same zero. */
    @Test
    @EnabledIf("drr7AndIrProviderAvailable")
    void corpus_control2_irRouteDrr7CarriesZeroObjectWitnessWraps() throws IOException {
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", DRR7_CELL_ROOT), new ArrayList<>());
        assertEquals(0, objectWraps(irOut), "ir-route drr 7.0.0 Object-witness wraps");
    }

    private static int objectWraps(Map<String, String> tree) {
        int n = 0;
        for (String java : tree.values()) {
            if (!java.contains(".<Object>of(")) {
                continue;
            }
            String code = codeOnly(java);
            int i = 0;
            while ((i = code.indexOf("MapperC.<Object>of(", i)) >= 0) {
                n++;
                i++;
            }
            i = 0;
            while ((i = code.indexOf("MapperS.<Object>of(", i)) >= 0) {
                n++;
                i++;
            }
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
                int e = java.indexOf("*/", i + 2);
                i = e < 0 ? n : e + 2;
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
                + path + " — seat 25 law C: the element witness reads the callee's inferred output.");
    }

    // =========================================================================
    // Fixture harness
    // =========================================================================

    private static RLinkingResult linking;
    private static RModel mainModel;
    private static Map<String, String> fixtureOut;

    private static void link() throws IOException {
        if (linking == null) {
            RModel main = AstBuilder.buildFromString(MODEL, "seat25f6c.rosetta");
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
            fixtureOut = render(m -> "census.seat25f6c".equals(m.namespace()));
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
            throw new AssertionError("[RuleCallMapperCWitnessSeatTest] builtins parse"
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
