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
 * PR #611, C2d retirement family 4 {@code dotted-enum-constant} — every consumer that must know
 * whether a compiled expression is a BARE enum constant ({@code EnumName.CONSTANT}, an item-typed
 * constant that has no {@code .get()} and must be {@code MapperS.of}-wrapped where a Mapper is
 * expected) moves off the RENDERED TEXT and onto the PRODUCER's witness.
 *
 * <p><b>The family, verbatim from the triage</b>
 * ({@code scripts/ci/evidence-api-triage.tsv}, family {@code dotted-enum-constant}): SIX ledger rows /
 * SIX occurrences, all verdict RETIRE-AFTER-CENSUS —
 * <ol>
 *   <li><b>four rows in {@code HandlerHelper.isDottedEnumConstant}</b>: the paren conjunct
 *       ({@code indexOf('(') >= 0 || indexOf(')') >= 0}), the space conjunct, the dot locator
 *       ({@code int dot = expr.indexOf('.')}) and the exactly-one-dot conjunct — a shape test on the
 *       compiled text, shared by SEVEN consumer seats on the default route and mirrored by the IR-java
 *       operand wrap;</li>
 *   <li><b>two rows in {@code ControlFlowHandler}</b> (the Mapper-typed ite-arm assign seat, facet
 *       inLambdaCondConsumerDecomp PR #384): the inline twin's {@code armRendered.indexOf('(') < 0}
 *       and {@code armRendered.indexOf('.') > 0} conjuncts, recognising "a #204-class bare-symbol arm
 *       the enum recovery QUALIFIED" by its render. The triage named these NONE-YET: "the
 *       recovery-QUALIFIED RSymbolReference carries no resolved enum value — a front-end addition,
 *       then a mechanical swap".</li>
 * </ol>
 *
 * <p><b>The retirement channel — the producer's witness.</b> Every emitter of a bare enum-constant
 * render builds it through {@code JavaExpression.enumConstant} (the resolved {@code Enum -> Value}
 * reference and the parser-bound bare value in {@code ReferenceHandler}; the fn-arg, ctor-setter and
 * switch-arm re-qualifications; the comparison seat's sibling-enum re-qualification arms; the IR
 * route's {@code IRJavaLeafEmitter.emitEnumValue}), and every consumer asks ONE predicate,
 * {@code HandlerHelper.isBareEnumConstant(builder)} — the {@code .get()}-suppression seats
 * ({@code unwrapForEvaluateArg}, {@code unwrapForAssignment}, the ite-arm and cond-list item ladders,
 * the ctor-arg and with-meta item forms), the {@code MapperS.of} operand wraps (comparison / contains
 * / disjoint / to-string, the Mapper-typed ite-arm slot), the IR-java operand wrap, and the two
 * alias-retype hops that pass a witness through by identity
 * ({@code ComparisonHandler.retypeNullTypedAliasOperand}, {@code ConversionHandler}'s to-string
 * retype). The producer's decision and the consumer's read are the same object (LAW 69), on both
 * routes (LAW 77). The ite-arm
 * seat's two hand-kept classes ({@code enumConstArm} — a resolved {@code REnumValueRef}; the inline
 * twin) collapse onto that one read.
 *
 * <p><b>Why no front-end addition.</b> The C2c census (PR #611; {@code target/seat611-instruments/}
 * {@code c2c-f4-patch.py} + {@code c2c-f4-verdicts.md}, local — env-gated probes at every consumer
 * seat, a producer-identity registry beside the text answer, two D11 walks over every cell of the
 * 25-cell matrix, REVERTED, never committed) read 391,168 consumer arrivals on the default route and
 * 351,404 on the IR route: the text answer and the producer witness (or the comparison seat's own
 * re-qualification) agreed at EVERY arrival, no non-enum {@code X.Y} spelling reached any seat, and
 * the inline twin fired on exactly NINE arrivals per route — all in
 * {@code regulation-mas-rewrite-trade-rule.rosetta}, every one a PARSER-BOUND bare value (the node's
 * {@code RSymbolReference.symbol()} IS the {@code REnumValue}, rendered by {@code ReferenceHandler}'s
 * bare-bound arm). The verdict the triage wanted recorded "on the node" is already there since the
 * v3.1 C1 resolution rebuild; the generator-side re-qualifications (a recovery the PARSER never
 * performs, so it has nothing to record) are witnessed where they are made. Recording a generator
 * recovery back into the parser's AST would be a back-channel, not a front-end fact.
 *
 * <p><b>The fixtures (Part A)</b> put one enum constant at each consumer seat the fork's own
 * emitters can reach from a reduced model, so the swap is pinned at unit grain per seat: a1 the
 * explicit {@code Enum -> Value} comparison operand, a2 the parser-BOUND bare comparand (the Cat-13
 * expected-type binding — the class the inline twin's nine corpus arrivals belong to), a3 the
 * evaluate-arg passthrough, a4 the SET assignment, a5 the to-string source wrap, a6 the
 * {@code contains} operand wrap, a7 the ctor-setter argument, a9 the digit-leading
 * {@code _30_360}-escaped value (the PR #267 A3 class, which the witness needs no escape clause for).
 * Each asserts the wrapped / bare form POSITIVELY and the wrong form ABSENT (LAW 76 witness
 * uniqueness). There is no a8: the inline twin's own seat is not reachable from a reduced model
 * (see the note in Part A) and is witnessed by the corpus carrier alone.
 *
 * <p><b>The corpus carrier (Part B).</b> drr 6.34.1
 * {@code drr/regulation/mas/rewrite/trade/reports/UnderlyingIdOtherSourceDTCCRule.java} — the file
 * every one of the twin's nine census arrivals lives in (its sibling {@code UnderlyingIdOtherDTCCRule}
 * carries the rest); golden {@code ifThenElseResult = MapperS.of(ProductIdTypeEnum.OTHER);} at the
 * Mapper-typed slot (control0), byte-identical on the default route (corpus_c1) and identical on the
 * IR route (control2, LAW 77; skips without the IR provider as its siblings' do). This is the inline
 * twin's witness: it fails under BOTH mutation lanes.
 *
 * <p><b>MUTATIONS (LAW 66/76) — MEASURED (LAW 82)</b> by {@code target/seat611-instruments/lanes-f4.py}
 * (local): the swap is an IDENTITY swap on the corpus, so it cannot fail first on its own; each lane
 * makes the one predicate LIE and the witnesses must fail. Lane A ({@code isBareEnumConstant} answers
 * {@code false} everywhere): the constants stay bare at the wrap seats and take {@code .get()} at the
 * suppression seats. Lane B ({@code true} everywhere): every operand double-wraps and no argument or
 * assignment is ever unwrapped. The measured failing sets are transcribed into the seat's CHANGELOG
 * entry from the lane logs, never quoted from this comment.
 */
class EnumConstantWitnessSeatTest {

    private static final Path REPO_ROOT =
            Path.of(System.getProperty("user.dir")).resolve("..").normalize();

    private static final List<Path> BUILTINS_SEARCH_ROOTS = List.of(
            REPO_ROOT.resolve("test-corpus/rune-dsl-builtins/rune-runtime/src/main/resources/model"),
            REPO_ROOT.resolve("rune-dsl/rune-runtime/src/main/resources/model"),
            REPO_ROOT.resolve("rune-runtime/src/main/resources/model"));

    static boolean builtinsAvailable() {
        return BUILTINS_SEARCH_ROOTS.stream().anyMatch(Files::isDirectory);
    }

    /** The carrier cell — drr 6.34.1 (the cell ListOfListsCondConsumerOctetComposeTest locks the same file in). */
    private static final Path CELL_A_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path GOLDEN_A = CELL_A_ROOT.resolve("rosetta-source/src/generated/java");

    static boolean cellAAvailable() {
        return Files.isDirectory(GOLDEN_A)
                && Files.isDirectory(CELL_A_ROOT.resolve("rosetta-source/src/main/rosetta"));
    }

    static boolean irProviderOnClasspath() {
        try {
            Class.forName("com.regnosys.rosetta.generator.java.ir.IRGenerationProviderImpl");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    static boolean cellAAndIrProviderAvailable() {
        return cellAAvailable() && irProviderOnClasspath();
    }

    /** The whole-file carrier of the inline twin's nine census arrivals. */
    private static final String CARRIER_A =
            "drr/regulation/mas/rewrite/trade/reports/UnderlyingIdOtherSourceDTCCRule.java";

    // =========================================================================
    // Part A — the fixtures, one per consumer seat
    // =========================================================================

    private static final String MODEL = """
            namespace census.seat611f4
            version "1.0.0"

            enum KindEnum:
                Cash
                Physical

            enum DccEnum:
                _30_360
                Act360

            type Leg:
                kind KindEnum (0..1)
                kinds KindEnum (0..*)
                dcc DccEnum (0..1)
                amount string (0..1)

            func Fn1: <"a callee taking the enum">
                inputs:
                    k KindEnum (0..1)
                output:
                    result string (0..1)
                set result:
                    "x"

            func A1ExplicitComparand: <"a1 - an explicit Enum -> Value comparison operand wraps MapperS.of">
                inputs:
                    leg Leg (1..1)
                output:
                    result boolean (0..1)
                set result:
                    leg -> kind = KindEnum -> Cash

            func A2BoundComparand: <"a2 - the bare value the parser BINDS from the sibling's enum type">
                inputs:
                    leg Leg (1..1)
                output:
                    result boolean (0..1)
                set result:
                    leg -> kind = Cash

            func A3EvaluateArg: <"a3 - an enum constant passed to a function stays bare (no .get())">
                inputs:
                    leg Leg (1..1)
                output:
                    result string (0..1)
                set result:
                    Fn1(KindEnum -> Cash)

            func A4SetAssignment: <"a4 - an enum constant assigned to the output stays bare">
                output:
                    result KindEnum (0..1)
                set result:
                    KindEnum -> Cash

            func A5ToString: <"a5 - to-string of an enum constant wraps its source">
                output:
                    result string (0..1)
                set result:
                    KindEnum -> Cash to-string

            func A6Contains: <"a6 - a contains operand wraps">
                inputs:
                    leg Leg (1..1)
                output:
                    result boolean (0..1)
                set result:
                    leg -> kinds contains KindEnum -> Cash

            func A7CtorArg: <"a7 - a ctor-setter argument stays bare">
                output:
                    result Leg (0..1)
                set result:
                    Leg {
                        kind: KindEnum -> Cash,
                        ...
                    }

            func A9EscapedValue: <"a9 - the digit-leading escaped value wraps like any other constant">
                inputs:
                    leg Leg (1..1)
                output:
                    result boolean (0..1)
                set result:
                    leg -> dcc = DccEnum -> _30_360
            """;

    /** a1 — the explicit reference: {@code areEqual(<nav>, MapperS.of(KindEnum.CASH), …)}. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_explicitReferenceComparandWraps() throws IOException {
        String code = codeOnly(fn("A1ExplicitComparand.java"));
        assertContains(code, "MapperS.of(KindEnum.CASH)");
        assertAbsent(code, ", KindEnum.CASH,");
        assertAbsent(code, "KindEnum.CASH.get()");
    }

    /**
     * a2 — the parser-BOUND bare value (TypeInferenceEngine Cat 13: the sibling's enum type makes
     * {@code Cash} unambiguous; {@code symbol()} is the REnumValue) renders through
     * {@code ReferenceHandler}'s bare-bound arm and wraps exactly like a1 — the class the inline
     * twin's nine corpus arrivals belong to.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_parserBoundBareComparandWraps() throws IOException {
        String code = codeOnly(fn("A2BoundComparand.java"));
        assertContains(code, "MapperS.of(KindEnum.CASH)");
        assertAbsent(code, "MapperS.of(Cash)");
        assertAbsent(code, ", KindEnum.CASH,");
    }

    /** a3 — the evaluate-arg passthrough: {@code fn1.evaluate(KindEnum.CASH)}, never {@code .get()}. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a3_evaluateArgStaysBare() throws IOException {
        String code = codeOnly(fn("A3EvaluateArg.java"));
        assertContains(code, "fn1.evaluate(KindEnum.CASH)");
        assertAbsent(code, "KindEnum.CASH.get()");
    }

    /** a4 — the SET assignment: {@code result = KindEnum.CASH;}, never {@code .get()}. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a4_setAssignmentStaysBare() throws IOException {
        String code = codeOnly(fn("A4SetAssignment.java"));
        assertContains(code, "= KindEnum.CASH;");
        assertAbsent(code, "KindEnum.CASH.get()");
    }

    /** a5 — the to-string source wrap: {@code MapperS.of(KindEnum.CASH).<String>map("to-string", …)}. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a5_toStringSourceWraps() throws IOException {
        String code = codeOnly(fn("A5ToString.java"));
        assertContains(code, "MapperS.of(KindEnum.CASH).");
        assertAbsent(code, "KindEnum.CASH.map(");
        assertAbsent(code, "KindEnum.CASH.<");
    }

    /** a6 — the contains operand wrap: {@code contains(<chain>, MapperS.of(KindEnum.CASH))}. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a6_containsOperandWraps() throws IOException {
        String code = codeOnly(fn("A6Contains.java"));
        assertContains(code, "MapperS.of(KindEnum.CASH)");
        assertAbsent(code, ", KindEnum.CASH)");
    }

    /** a7 — the ctor-setter argument: {@code .setKind(KindEnum.CASH)}, never {@code .get()}. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a7_ctorSetterArgStaysBare() throws IOException {
        String code = codeOnly(fn("A7CtorArg.java"));
        assertContains(code, "setKind(KindEnum.CASH)");
        assertAbsent(code, "KindEnum.CASH.get()");
    }

    /*
     * There is deliberately NO a8: the inline twin's seat (ControlFlowHandler's Mapper-typed ite-arm
     * assign inside a lambda, facet inLambdaCondConsumerDecomp) has no unit-grain fixture in this
     * suite. Three reductions of the mas carrier were tried and none reaches it — a plain
     * extract-body conditional renders the if/return block lambda (a wrap that is not the
     * predicate's; it failed under neither lane); a top-level then-step ladder renders
     * FunctionExpressionRenderer's then-arg hoist (a different seat; it failed under neither lane);
     * the mas-shaped extract-arm then-ladder declines the block form altogether and renders the
     * ternary stub. The seat is admitted only inside the full mas ladder context, so its witness is
     * the CORPUS carrier: corpus_c1 below and ListOfListsCondConsumerOctetComposeTest's mas locks,
     * which fail under BOTH lanes. Disclosed here rather than papered over with a fixture that
     * passes for another reason (LAW 72: a control must scan the domain it claims).
     */

    /** a9 — the PR #267 A3 class: a digit-leading escaped value wraps with no escape clause needed. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a9_escapedDigitLeadingValueWraps() throws IOException {
        String code = codeOnly(fn("A9EscapedValue.java"));
        assertContains(code, "MapperS.of(DccEnum._30_360)");
        assertAbsent(code, ", DccEnum._30_360,");
    }

    // =========================================================================
    // Part B — the corpus carrier (drr 6.34.1; the D11 ring locks the cell, this suite locks the
    // one whole file the inline twin's census arrivals live in)
    // =========================================================================

    /** corpus_c1 — the whole-file byte lock on the twin's carrier. */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_drr6341UnderlyingIdOtherSourceDTCCRuleByteIdentical() throws IOException {
        assertNotNull(drrAOutput, "drr 6.34.1 generation did not run — corpus unavailable?");
        List<String> own = drrAGenErrors.stream().filter(e -> e.contains(CARRIER_A)).toList();
        assertTrue(own.isEmpty(),
                "the generator reported errors for the locked file " + CARRIER_A + ": " + own);
        String generated = drrAOutput.get(CARRIER_A);
        assertNotNull(generated, "not generated in drr 6.34.1: " + CARRIER_A);
        Path goldenPath = GOLDEN_A.resolve(CARRIER_A);
        assertTrue(Files.isRegularFile(goldenPath), "golden missing: " + goldenPath);
        assertEquals(normalize(Files.readString(goldenPath)), normalize(generated),
                "generated drr 6.34.1 output must byte-match golden (newline-normalized) for "
                + CARRIER_A + " — PR #611 family 4: the ite-arm seat's inline twin moves onto the"
                + " producer's enum-constant witness with no byte change.");
    }

    /**
     * control0 — golden is the oracle: it carries the Mapper-typed slot's wrapped constant. If this
     * ever stops holding, the carrier stopped being this family's carrier and every claim above is
     * restated, not patched.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control0_goldenCarriesTheWrappedConstantAtTheIteSlot() throws IOException {
        String golden = Files.readString(GOLDEN_A.resolve(CARRIER_A));
        assertTrue(golden.contains("ifThenElseResult = MapperS.of(ProductIdTypeEnum.OTHER);"),
                "golden must carry the MapperS.of-wrapped enum constant at the ifThenElseResult slot");
    }

    /**
     * control2 — LAW 77 route parity for the drr 6.34.1 carrier: the seat is ControlFlowHandler-side
     * and the IR route inherits it, so the carrier must render identically under {@code -Pir-on}.
     * Skips (recorded) without the IR provider.
     */
    @Test
    @EnabledIf("cellAAndIrProviderAvailable")
    void corpus_control2_irRouteMatchesLegacyForCarrier() throws IOException {
        assertNotNull(drrAOutput, "drr 6.34.1 legacy-route generation did not run");
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", CELL_A_ROOT), new ArrayList<>());
        assertEquals(drrAOutput.get(CARRIER_A), irOut.get(CARRIER_A),
                "route divergence: " + CARRIER_A);
    }

    // =========================================================================
    // Fixture harness (the SwitchCaseCardinalitySeatTest renderer)
    // =========================================================================

    private record Render(Map<String, String> output, List<String> errors) {}

    private static Render rendered;

    private static Render render() throws IOException {
        if (rendered != null) {
            return rendered;
        }
        RModel main = AstBuilder.buildFromString(MODEL, "seat611f4.rosetta");
        main.setVersion("0.0.0.test");
        List<RModel> models = new ArrayList<>();
        models.add(main);
        models.addAll(loadBuiltinsOnly());
        RWorkspace workspace = RWorkspace.build(models).workspace();
        GeneratorModel gm = new GeneratorModel(workspace,
                m -> "census.seat611f4".equals(m.namespace()));
        JavaTypeUtil typeUtil = new JavaTypeUtil();
        JavaTypeTranslator tt = new JavaTypeTranslator(typeUtil);
        FunctionGenerator fg = new FunctionGenerator(gm, tt, typeUtil);
        Map<String, String> out = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        fg.generateWithErrors(out)
                .forEach(e -> errors.add(e.getTargetPath() + " — " + e));
        rendered = new Render(out, errors);
        return rendered;
    }

    /** Functions land under {@code .../functions/}. */
    private static String fn(String fileName) throws IOException {
        Render r = render();
        List<String> own = r.errors().stream().filter(e -> e.contains(fileName)).toList();
        assertTrue(own.isEmpty(),
                "the generator reported errors for " + fileName + " (a broken fixture must fail"
                + " loudly, not skip): " + own);
        String out = lookupOrNull(r.output(), "functions/" + fileName);
        assertNotNull(out, "not generated: " + fileName + " (have: " + r.output().keySet() + ")");
        return out;
    }

    private static String lookupOrNull(Map<String, String> output, String suffix) {
        return output.entrySet().stream()
                .filter(e -> e.getKey().endsWith("/" + suffix))
                .map(Map.Entry::getValue)
                .findFirst().orElse(null);
    }

    private static void assertContains(String code, String needle) {
        assertTrue(code.contains(needle), "expected <" + needle + "> in:\n" + code);
    }

    private static void assertAbsent(String code, String needle) {
        assertTrue(!code.contains(needle), "did NOT expect <" + needle + "> in:\n" + code);
    }

    /**
     * Strip line and block comments plus string literals so a javadoc, a label or a
     * {@code "getKind"} literal never counts as code. Every assertion runs on this.
     */
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
            throw new AssertionError("[EnumConstantWitnessSeatTest] builtins parse failures: "
                    + String.join("; ", failures));
        }
        return models;
    }

    // =========================================================================
    // Corpus harness (the sibling seat suites' cell generator, verbatim)
    // =========================================================================

    private static Map<String, String> drrAOutput;
    private static List<String> drrAGenErrors;

    @BeforeAll
    static void generateCells() throws IOException {
        if (cellAAvailable()) {
            List<String> errs = new ArrayList<>();
            drrAOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", CELL_A_ROOT), errs);
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
}
