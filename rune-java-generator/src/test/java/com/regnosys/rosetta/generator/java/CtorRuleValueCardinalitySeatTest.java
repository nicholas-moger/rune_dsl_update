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
 * SEAT 25, law D — facet {@code ctorRuleValueCardinality}: <b>a bare RULE-reference ctor value
 * consults the callee rule's cardinality</b> — golden drr 5.61.0 esma/fca {@code DeliveryRule}
 * passes the MULTI {@code DaysOfTheWeek} rule's List straight through the MULTI setter
 * ({@code .setDaysOfTheWeek(daysOfTheWeekRule.evaluate(item.get()))}) where the fork read the
 * workspace's cardinality-BLIND SINGLE, hoisted {@code final String string = …;} and coerced
 * {@code Collections.singletonList(string)} (non-compiling — a List into a String local, LAW 74).
 *
 * <p><b>The law (two coordinated reads at ONE seat — LAW 69).</b> At the ctor-value seat
 * ({@code ConstructionHandler}): (1) the cardinality authority for a bare rule-reference value is
 * the shared {@code NavigationHandler.ruleOutputProvesMulti} verdict (the #367 law — the same
 * predicate every rule-call seat consults; the T3 alias-seam override at the same seat is the
 * precedent); (2) the arm-C2r bare-splice gate ({@code ctorValueItemIsBareSpliced}) classifies a
 * RULE-valued item by the callee's inferred output ({@code HandlerHelper.ruleInferredOutputRType},
 * the ONE rule-output read) — its own javadoc's golden witness
 * ({@code .setDaysOfTheWeek(getDaysOfTheWeek.evaluate(…))}) is the rune-fpml FUNCTION twin of
 * exactly this drr RULE shape. A recoverable META output declines to the ArrayList wrap (the
 * regression-free direction; corpus-unwitnessed), as does every resolution failure.
 *
 * <p><b>LAW 75 — the seat-25 P25C probe measured the whole population over all 275 matrix rows,
 * BOTH routes:</b> {@code attrMulti=true ∧ provesMulti=true ∧ valueCard=SINGLE} at exactly the two
 * {@code daysOfTheWeek} sites (esma + fca) corpus-wide; every other bare-rule ctor value reads
 * {@code provesMulti=false} and keeps today's bytes.
 *
 * <p><b>RED at the pre-seat blob — MEASURED</b> (the law commit's parent, this suite kept): 6F =
 * a1, a3, b1, b2, c1, c2 — b1/b2 are red PRE-seat too because law A's {@code ruleBodyNonRootSeat}
 * admission reached CTOR-value references until THIS commit's decline (the third coordinated edit:
 * a ctor-value rule reference belongs to the ctor seat; the stream wrap at a setter is
 * corpus-unwitnessed — found by this suite's a3 fixture, fixed by declining, never by weakening the
 * fixture). corpus_control0 GREEN in both states (golden is frozen). Under {@code -Pir-on} also a2.
 * The within-seat LAW-81 hand-off: law B's {@code RuleCallArmMetaWrapSeatTest.KNOWN_RESIDUE_561}
 * pinned the two DeliveryRule fork-extra hoists — re-pinned in THIS commit to the healed set (only
 * the cftc NotionalCurrencyLeg2Rule ladder row remains).
 *
 * <p><b>LAW 66/76 mutations</b> (each applied → run → reverted; the failing sets MEASURED by the
 * seat's receipts chain — LAW 82): (i) the provesMulti cardinality override deleted — 7F: a1, a3,
 * b1, b2, c1, c2 + the cross-suite {@code RuleCallArmMetaWrapSeatTest.corpus_control1} (the
 * Delivery {@code final String string = …} hoists reappear in law B's residue census — the LAW-81
 * hand-off's own receipt); b1/b2 move because without the override the fixture's every rule value
 * reads SINGLE and the whole ctor re-shapes; (ii) the rule arm of the bare-splice gate deleted
 * (the ArrayList wrap for every plain rule value) — 3F: a1, c1, c2 (b1 stays — the single path
 * never reaches the gate); (iii) the meta decline dropped (a meta-output rule bare-splices) — 1F:
 * a3 (isolating).
 */
class CtorRuleValueCardinalitySeatTest {

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

    static boolean builtinsAndIrProviderAvailable() {
        return builtinsAvailable() && irProviderOnClasspath();
    }

    /**
     * One MULTI plain rule, one SINGLE plain rule, one MULTI meta rule, one MULTI pojo rule, and a
     * ctor consuming each — the DeliveryRule shape and its decline classes.
     */
    private static final String MODEL = """
            namespace census.seat25f6d
            version "1.0.0"

            type Ident:
                code string (0..1)
                    [metadata scheme]

            type Leg:
                name string (0..1)
                ident Ident (0..1)

            type Instr:
                legs Leg (0..*)
                allowed boolean (0..1)

            type OutReport:
                days string (0..*)
                extra string (0..*)
                codes string (0..*)
                parts Leg (0..*)

            func IsAllowable:
                inputs:
                    i Instr (0..1)
                output:
                    result boolean (1..1)
                set result:
                    i -> allowed = True

            reporting rule InnerDays from Instr: <"DaysOfTheWeek twin - a MULTI plain rule">
                extract legs -> name
                    as "days"

            reporting rule InnerFirst from Instr: <"a SINGLE plain rule">
                extract legs -> name first
                    as "first"

            reporting rule InnerCodes from Instr: <"a MULTI META rule - the unwitnessed decline">
                extract legs -> ident -> code
                    as "codes"

            reporting rule InnerLegs from Instr: <"a MULTI POJO rule - the arm-C2r pojo decline">
                extract legs
                    as "legs"

            reporting rule A1Ctor from Instr: <"THE DeliveryRule SHAPE: bare rule values in a ctor">
                filter IsAllowable
                then extract OutReport {
                    days: InnerDays,
                    extra: InnerFirst,
                    codes: InnerCodes,
                    parts: InnerLegs
                }
                    as "a1"
            """;

    // =========================================================================
    // Part A — the seat (RED at the pre-seat blob)
    // =========================================================================

    /** a1 — the MULTI plain rule value passes straight through the MULTI setter (no hoist, no singletonList). */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_multiPlainRuleValuePassesThrough() throws IOException {
        String out = rule("A1CtorRule.java");
        assertContains(out, ".setDays(innerDaysRule.evaluate(item.get()))");
        assertNotContains(out, "new ArrayList(innerDaysRule");
    }

    /** a2 — LAW 77: the a1 shape through the REAL {@code IRGeneration.functionGenerator} seam ({@code -Pir-on}). */
    @Test
    @EnabledIf("builtinsAndIrProviderAvailable")
    void a2_multiPlainRuleValuePassesThroughOnIrRoute() throws IOException {
        String out = lookup(fixtureOnIrRoute(), "reports/A1CtorRule.java");
        assertContains(out, ".setDays(innerDaysRule.evaluate(item.get()))");
        assertNotContains(out, "new ArrayList(innerDaysRule");
    }

    /** a3 — a MULTI META-output rule value DECLINES to the ArrayList wrap (unwitnessed; type-correct). */
    @Test
    @EnabledIf("builtinsAvailable")
    void a3_multiMetaRuleValueDeclinesToWrap() throws IOException {
        String out = rule("A1CtorRule.java");
        assertContains(out, ".setCodes(new ArrayList(innerCodesRule.evaluate(item.get())))");
    }

    // =========================================================================
    // Part B — placement pins (GREEN in BOTH states)
    // =========================================================================

    /** b1 — a SINGLE rule value at the MULTI attribute keeps the #198 singleton hoist. */
    @Test
    @EnabledIf("builtinsAvailable")
    void b1_singleRuleValueKeepsSingletonHoist() throws IOException {
        String out = rule("A1CtorRule.java");
        assertContains(out, "final String string = innerFirstRule.evaluate(item.get());");
        assertContains(out, ".setExtra((string == null ? Collections.<String>emptyList() : Collections.singletonList(string)))");
    }

    /** b2 — a MULTI POJO-item rule value keeps the arm-C2r ArrayList wrap. */
    @Test
    @EnabledIf("builtinsAvailable")
    void b2_multiPojoRuleValueKeepsArrayListWrap() throws IOException {
        String out = rule("A1CtorRule.java");
        assertContains(out, ".setParts(new ArrayList(innerLegsRule.evaluate(item.get())))");
    }

    // =========================================================================
    // Part C — the corpus (drr 5.61.0: the two DeliveryRule carriers)
    // =========================================================================

    private static final Path DRR561_CELL_ROOT = Path.of("../test-corpus/drr/drr-5.61.0");
    private static final Path DRR561_GOLDEN = DRR561_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    static boolean drr561Available() {
        return Files.isDirectory(DRR561_GOLDEN);
    }

    private static final String ESMA_DELIVERY = "drr/regulation/esma/emir/refit/trade/reports/DeliveryRule.java";
    private static final String FCA_DELIVERY = "drr/regulation/fca/ukemir/refit/trade/reports/DeliveryRule.java";

    @Test
    @EnabledIf("drr561Available")
    void corpus_c1_esmaDeliveryByteIdentical() throws IOException {
        lock(ESMA_DELIVERY);
    }

    @Test
    @EnabledIf("drr561Available")
    void corpus_c2_fcaDeliveryByteIdentical() throws IOException {
        lock(FCA_DELIVERY);
    }

    /**
     * control0 — golden is the oracle: neither Delivery golden carries a {@code daysOfTheWeek}
     * singleton coercion or an ArrayList wrap at the setter (the straight pass-through), and the
     * {@code DaysOfTheWeek} callee itself is untouched by this seat.
     */
    @Test
    @EnabledIf("drr561Available")
    void corpus_control0_goldenDeliveryPassesStraightThrough() throws IOException {
        for (String path : List.of(ESMA_DELIVERY, FCA_DELIVERY)) {
            String golden = Files.readString(DRR561_GOLDEN.resolve(path));
            assertContains(golden, ".setDaysOfTheWeek(daysOfTheWeekRule.evaluate(item.get()))");
            assertNotContains(golden, "new ArrayList(daysOfTheWeekRule");
            assertNotContains(golden, "Collections.singletonList(string)");
        }
    }

    private static void lock(String path) throws IOException {
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
                + path + " — seat 25 law D: the ctor rule value consults the callee's cardinality.");
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
    // Cell generation (drr 5.61.0, the legacy route)
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

    private static void collect(List<String> sink, List<GenerationException> errors) {
        if (errors != null) {
            errors.forEach(e -> sink.add(e.getTargetPath() + " — " + e));
        }
    }

    // =========================================================================
    // Fixture harness
    // =========================================================================

    private static RLinkingResult linking;
    private static RModel mainModel;
    private static Map<String, String> fixtureOut;

    private static void link() throws IOException {
        if (linking == null) {
            RModel main = AstBuilder.buildFromString(MODEL, "seat25f6d.rosetta");
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
            fixtureOut = render(m -> "census.seat25f6d".equals(m.namespace()));
        }
        return fixtureOut;
    }

    private static Map<String, String> fixtureOutIr;

    /** The fixture's rules through the REAL {@code IRGeneration.functionGenerator} seam (the ON route). */
    private static Map<String, String> fixtureOnIrRoute() throws IOException {
        if (fixtureOutIr == null) {
            link();
            String previous = System.getProperty(IRGeneration.PROPERTY);
            System.setProperty(IRGeneration.PROPERTY, "true");
            try {
                assertNotNull(IRGeneration.providerOrNull(),
                        "the IR provider must be resolvable under -Pir-on, else this is not an ON-route render");
                GeneratorModel gm = new GeneratorModel(linking.workspace(),
                        m -> "census.seat25f6d".equals(m.namespace()));
                JavaTypeUtil typeUtil = new JavaTypeUtil();
                JavaTypeTranslator tt = new JavaTypeTranslator(typeUtil);
                FunctionGenerator fg = IRGeneration.functionGenerator(gm, tt, typeUtil);
                assertTrue(!fg.getClass().equals(FunctionGenerator.class),
                        "the seam must hand back the IR-route FunctionGenerator, got " + fg.getClass());
                RuleGenerator ruleGen = new RuleGenerator(gm, tt, fg);
                Map<String, String> out = new LinkedHashMap<>();
                List<String> errors = new ArrayList<>();
                ruleGen.generateClasses(mainModel, "1.0", out)
                        .forEach(e -> errors.add(e.getTargetPath() + " — " + e));
                fg.generateWithErrors(out)
                        .forEach(e -> errors.add(e.getTargetPath() + " — " + e));
                if (!errors.isEmpty()) {
                    throw new AssertionError("fixture generation errors on the IR route: " + errors);
                }
                fixtureOutIr = out;
            } finally {
                if (previous == null) {
                    System.clearProperty(IRGeneration.PROPERTY);
                } else {
                    System.setProperty(IRGeneration.PROPERTY, previous);
                }
            }
        }
        return fixtureOutIr;
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
            throw new AssertionError("[CtorRuleValueCardinalitySeatTest] builtins parse"
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
