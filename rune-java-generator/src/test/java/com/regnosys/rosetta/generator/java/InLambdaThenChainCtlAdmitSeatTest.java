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
import java.util.TreeSet;
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
 * SEAT 26, law A — facet {@code inLambdaThenChainCtlAdmit}: <b>an IN-LAMBDA then chain on the rule
 * path admits nested-then control flow that hoists in its OWN inner lambda scope, and a
 * MULTI-STATEMENT consumer body SPLICES its statements into the block lambda</b> — the runtime
 * {@code .then(} inline fallback (no {@code Mapper.then(Function)} method exists; ZERO of the
 * 34,686 goldens carry the form — the whole-corpus law this suite's controls pin) retires for the
 * class.
 *
 * <p><b>The probe verdicts this law answers (LAW 75/72, {@code probe26-off-carriers.txt}):</b> the
 * carriers print {@code hoistable=true inLambda=true hasSink=false suppressed=false} at the
 * outermost chain — the census's "no reachable statement-hoist sink" line named a symptom; the
 * DECLINES are (1) the ctl-scan ({@code thenChainHasUnhandledControlFlow}) reading a nested then at
 * an evaluate-ARG position inside a then-body as unhandled (the FloatingRate* carriers), and (2)
 * the consumer body compiling to a multi-statement block (the #350-era single-line decline —
 * CDEPaymentFrequencyPeriodMultiplierAdjusted, whose fork bytes carry BOTH the hoisted level decl
 * AND the inline {@code .then(} on a RE-NAVIGATED receiver).
 *
 * <p><b>The law (two mechanisms, ONE channel).</b> (i) The scan's with-args invocation-body walk
 * admits an ARG whose ONLY control flow is a nested hoistable+ctl-clean then chain (rule-path,
 * the #392 {@code inLambdaArgSeatCondAdmit} pattern at the SAME walk) — the inner chain hoists
 * through the #350-F5 lambda channel INSIDE the level's own lambda scope (per-scope numbering:
 * the outer levels {@code thenArg0..N}, the inner chain's bare {@code thenArg} — golden esma
 * FloatingRateReferencePeriodOfLeg1Multiplier). (ii) The lambda-channel CONSUMER whose compiled
 * body is a multi-statement block SPLICES the block's statements ahead of the return instead of
 * declining (golden esma CDEPaymentFrequencyPeriodMultiplierAdjusted:
 * {@code final Integer integer = _thenArg.get(); return MapperS.of(adjustPeriodMultiplier.evaluate(…));}).
 *
 * <p><b>RED at the pre-seat blob (the law commit's parent, this suite kept)</b>: a1, a2, c1–c4,
 * corpus_control1 and corpus_control3 (the pinned carrier sets shrink); b1 and corpus_control0
 * GREEN in both states.
 *
 * <p><b>LAW 66/76 mutations</b> (each applied → run → reverted; the failing sets MEASURED by the
 * seat's run-2 chain at {@code 124499fa} — LAW 82): (i) the LAMBDA-channel LEVEL multi-statement
 * splice disabled — 2F: c4, corpus_control1; (ii) the LAMBDA-channel CONSUMER multi-statement
 * splice disabled — 3F: a2, c1, corpus_control1; (iii) the #392 invocation-ARG nested-chain admit
 * deleted — 4F: a1, c2, c3, corpus_control1. The law's FOURTH landed link — the ctl-scan
 * BODIES-loop tail admit — was REMOVED at the seat's zero-adjudication close: its Boolean.FALSE
 * toggle measured 0F with the 174,141-file matrix moving +22 EXACTLY and 207/207 residue groups
 * hunk-stable (contribution-free everywhere; the run-1 receipt), its FRR* carrier claim owned by
 * the (iii) arg admit, and its CustomBasketCode switch-body class banked to the S31 F15 seat.
 */
class InLambdaThenChainCtlAdmitSeatTest {

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
     * The distilled carrier shapes. A1 = the FloatingRate* class (a mid-chain
     * {@code then extract Fn(<arg with a nested then>, <int arg>)} level + a bare-invokable
     * terminal); A2 = the CDEPaymentFrequencyPeriodMultiplierAdjusted class (a single in-lambda
     * then whose consumer body needs the {@code integer} coercion hoist — a multi-statement
     * consumer); B1 = the decline pin (an ELSEFUL conditional at the invocation-arg seat stays
     * on today's inline bytes — corpus-unwitnessed, the admit is confined to nested THEN args).
     */
    private static final String MODEL = """
            namespace census.seat26f7a
            version "1.0.0"

            type Leg:
                name string (0..1)
                num int (0..1)

            type Instr:
                legs Leg (0..*)
                lone Leg (0..1)
                allowed boolean (0..1)

            func IsOk:
                inputs:
                    i Instr (0..1)
                output:
                    result boolean (1..1)
                set result:
                    i -> allowed = True

            func LegName:
                inputs:
                    l Leg (0..1)
                output:
                    result string (1..1)
                set result:
                    l -> name

            func Adjust:
                inputs:
                    a string (0..1)
                    b number (0..1)
                output:
                    result number (1..1)
                set result:
                    b

            func Fmt:
                inputs:
                    n number (0..1)
                output:
                    result string (1..1)
                set result:
                    "x"

            reporting rule A1Frr from Instr: <"the FloatingRate* shape - a nested then at an evaluate-arg position">
                extract i [
                    filter IsOk
                    then extract Adjust(i -> lone then extract LegName, i -> lone -> num)
                    then Fmt
                ]
                    as "a1"

            reporting rule A2Adj from Instr: <"the CDEPFPMA shape - a multi-statement consumer body">
                extract i [
                    i -> lone -> num
                        then Adjust(LegName(i -> lone), item)
                ]
                    as "a2"

            reporting rule B1Decline from Instr: <"b1 - an ELSEFUL conditional at the invocation-arg seat keeps today's bytes">
                extract i [
                    i -> lone -> name
                        then Adjust(if IsOk(i) then "a" else "b", i -> lone -> num)
                ]
                    as "b1"
            """;

    // =========================================================================
    // Part A — the fixtures (failing-first)
    // =========================================================================

    /** a1 — the FloatingRate* shape: every level hoists, the inner then hoists in ITS own lambda scope, no runtime `.then(`. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_nestedThenAtArgSeatHoistsWholeChain() throws IOException {
        String out = rule("A1FrrRule.java");
        assertNoRuntimeThen(out, "a1");
        assertContains(out, "final MapperS<Instr> thenArg0 = ");
        assertContains(out, "final MapperS<BigDecimal> thenArg1 = thenArg0");
        assertContains(out, "final MapperS<Leg> thenArg = ");
        assertContains(out, "return MapperS.of(fmt.evaluate(thenArg1.get()));");
    }

    /** a2 — the CDEPFPMA shape: the multi-statement consumer splices; the level decl is consumed, not re-navigated. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_multiStatementConsumerSplices() throws IOException {
        String out = rule("A2AdjRule.java");
        assertNoRuntimeThen(out, "a2");
        assertContains(out, "final Integer integer = thenArg.get();");
        assertContains(out, "return MapperS.of(adjust.evaluate(");
    }

    /** b1 — the ELSEFUL invocation-arg conditional keeps the inline decline (corpus-unwitnessed; the admit is THEN-args only). */
    @Test
    @EnabledIf("builtinsAvailable")
    void b1_elsefulArgConditionalKeepsDecline() throws IOException {
        String out = rule("B1DeclineRule.java");
        assertTrue(codeOnly(out).contains(".then("),
                "b1 must keep today's inline decline bytes — the admit is confined to nested-THEN args;"
                + " an elseful arg conditional stays on the fallback:\n" + out);
    }

    // =========================================================================
    // Part C — the corpus (drr 5.61.0 carriers + the whole-cell `.then(` set controls)
    // =========================================================================

    private static final Path DRR561_CELL_ROOT = Path.of("../test-corpus/drr/drr-5.61.0");
    private static final Path DRR561_GOLDEN = DRR561_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path DRR7_CELL_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path DRR7_GOLDEN = DRR7_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    static boolean drr561Available() {
        return Files.isDirectory(DRR561_GOLDEN);
    }

    static boolean drr7Available() {
        return Drr7Corpus.gate(Files.isDirectory(DRR7_GOLDEN), InLambdaThenChainCtlAdmitSeatTest.class);
    }

    static boolean drr561AndIrProviderAvailable() {
        return drr561Available() && irProviderOnClasspath();
    }

    private static final String CDEPFPMA =
            "drr/regulation/esma/emir/refit/trade/reports/CDEPaymentFrequencyPeriodMultiplierAdjustedRule.java";
    private static final String FRR_LEG1 =
            "drr/regulation/esma/emir/refit/trade/reports/FloatingRateReferencePeriodOfLeg1MultiplierRule.java";
    private static final String FRR_LEG2 =
            "drr/regulation/esma/emir/refit/trade/reports/FloatingRateReferencePeriodOfLeg2MultiplierRule.java";
    private static final String FRRF =
            "drr/regulation/esma/emir/refit/trade/reports/FloatingRateResetFrequencyMultiplierOfLeg1Rule.java";

    @Test
    @EnabledIf("drr561Available")
    void corpus_c1_cdePaymentFrequencyPeriodMultiplierAdjustedByteIdentical() throws IOException {
        lock561(CDEPFPMA);
    }

    @Test
    @EnabledIf("drr561Available")
    void corpus_c2_floatingRateReferencePeriodOfLeg1MultiplierByteIdentical() throws IOException {
        lock561(FRR_LEG1);
    }

    @Test
    @EnabledIf("drr561Available")
    void corpus_c3_floatingRateReferencePeriodOfLeg2MultiplierByteIdentical() throws IOException {
        lock561(FRR_LEG2);
    }

    @Test
    @EnabledIf("drr561Available")
    void corpus_c4_floatingRateResetFrequencyMultiplierOfLeg1ByteIdentical() throws IOException {
        lock561(FRRF);
    }

    /** control0 — golden is the oracle: ZERO runtime `.then(` in BOTH carrier cells' frozen goldens (the whole-corpus law). */
    @Test
    @EnabledIf("drr561Available")
    void corpus_control0_goldenCarriesZeroRuntimeThen() throws IOException {
        assertEquals(List.of(), thenCarriers(readGoldenTree(DRR561_GOLDEN)), "golden drr 5.61.0 runtime `.then(` carriers");
        if (drr7Available()) {
            assertEquals(List.of(), thenCarriers(readGoldenTree(DRR7_GOLDEN)), "golden drr 7.0.0 runtime `.then(` carriers");
        }
    }

    /**
     * control1 — LAW 73/79: the fork's WHOLE drr 5.61.0 cell's runtime-`.then(`-carrying file SET, pinned EXACTLY.
     * Every entry is a named residue of ANOTHER law of this seat or a later seat — law B re-pins the two
     * QuantityFrequency* rows in ITS commit, law C the PriorUTI/mega-ladder rows in ITS — the within-seat LAW-81
     * hand-off. An entry may only LEAVE in the commit that heals it; a NEW carrier may never appear.
     */
    @Test
    @EnabledIf("drr561Available")
    void corpus_control1_forkDrr561ThenCarrierSetPinned() throws IOException {
        assertNotNull(drr561Output, "drr 5.61.0 generation did not run");
        assertEquals(List.of(), drr561GenErrors, "drr 5.61.0 reported a generation error — the scan is incomplete");
        assertEquals(KNOWN_THEN_CARRIERS_561, thenCarriers(drr561Output), "fork drr 5.61.0 runtime `.then(` carriers");
    }

    private static final List<String> KNOWN_THEN_CARRIERS_561 = List.of(
            // Sorted (the scan's order). Per entry, the law/seat that heals it:
            // NotionalAmount/CurrencyLeg1 = the mega-ladder cascade (this seat's law E — the
            // rung-interior chains render once the cascade block lands; re-pinned in ITS commit);
            // NotionalCurrencyLeg2 + jfsa NotionalLeg2 = F8 (seat S27, NOT this seat's). The two
            // QuantityFrequencyOf* entries HEALED at law B and the two PriorUTI/PriorUti entries
            // at law C (the wrap-hoisting block forms carried their chains with them) — each left
            // this pin in the commit that healed it, the control going RED first every time (the
            // within-seat LAW-81 hand-off, fired twice).
            // the four cftc Notional* paths LEFT this set at seat 32: law C.3 (ruleThenArmLadderNestedTreeAdmit) hoisted
            // their rule-then ladders into the block form (NotionalCurrencyLeg1 IMPROVED-not-whole, but its then-chains
            // all hoisted); transcribed from this control's own print (C3-trip1a.log).
            );  // LAW 81 (seat 33, law F.B): NotionalLeg2Rule LEFT the drr 5.61.0 `.then(` carrier set - the file is WHOLE, no `.then(` remains (FB-trip1.log print: expected [NotionalLeg2Rule] but was [])

    /** control2 — LAW 77 route parity: the IR-route drr 5.61.0 cell carries the SAME carrier set. */
    @Test
    @EnabledIf("drr561AndIrProviderAvailable")
    void corpus_control2_irRouteDrr561ThenCarrierSetMatchesLegacy() throws IOException {
        assertNotNull(drr561Output, "drr 5.61.0 legacy-route generation did not run");
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "5.61.0", DRR561_CELL_ROOT), new ArrayList<>());
        assertEquals(thenCarriers(drr561Output), thenCarriers(irOut), "ir-route vs legacy-route runtime `.then(` carriers");
    }

    /** control3 — LAW 79: the predicate reaches the other carrier cell; the fork drr 7.0.0 set pinned the same way. */
    @Test
    @EnabledIf("drr7Available")
    void corpus_control3_forkDrr7ThenCarrierSetPinned() throws IOException {
        List<String> errs = new ArrayList<>();
        Map<String, String> out = generateCell(
                new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", DRR7_CELL_ROOT), errs);
        assertEquals(List.of(), errs, "drr 7.0.0 reported a generation error — the scan is incomplete");
        assertEquals(KNOWN_THEN_CARRIERS_7, thenCarriers(out), "fork drr 7.0.0 runtime `.then(` carriers");
    }

    private static final List<String> KNOWN_THEN_CARRIERS_7 = List.of(
            // MapNonpublicExecutionReportToWorkflowStep.java (the census's F10-residue row B103) LEFT this
            // pin at seat 32: law C.1 (deepBareFunctionThenChainAdmit - the deep seat's bodies loop admits
            // a bare-function then-body off the rule path) healed it WHOLE in all four drr 7.x cells;
            // transcribed from corpus_control3's own print (C1-trip1.log).
            // CommodityQuantityWithFrequency.java — was the un-keyed close-census tail — LEFT this
            // pin at seat 30: law 4 (the d=4 function-level all-or-nothing decline at the RThenExpr
            // inline fallback, CollectionHandler.tryDeepThenHoist) healed it WHOLE in all four
            // drr 7.x cells, and the heal hoists its runtime `.then(` away, so the file no longer
            // carries the scanned token at all. Re-pinned VERBATIM from corpus_control3's own
            // failing print at the seat-30 chain head e223ce19 (LAW 81): 4 carriers -> 3.
            // The ClearingExceptionsAndExemptionsCounterparty entry HEALED at law F (the
            // fn-path in-lambda cond-chain admit) and left this pin in law F's commit —
            // the FOURTH within-seat LAW-81 firing (this control went RED first).
            // F7 ∩ F13/F22 and F7 ∩ F8 partial rows (their `.then(` halves are rung-interior
            // chains — re-measured as later laws land):
            // the QuantityUnitOfMeasure row (the bare-path carrier-set entry) LEFT this list: law B.1 (setSeatNestedValueThenTogetherHoist,
            // the k>0 restructure window) healed this scan's token set to golden's in all four drr 7.x cells -
            // the file itself stays BANDED (its close is B.3 + B.24); transcribed from this control's own print
            // (B1-trip1.log), a pure row removal (was == expected minus it).
            // the TotalNotionalQuantity row (the bare-path entry) LEFT this list: law D.3 (fnDeepCondBaseConfinedArmChainAdmit,
            // seven rungs) healed the file WHOLE in all four drr 7.x cells; transcribed from this control's own
            // print (D3-trip1.log), a pure row removal (was == expected minus it).
            );

    // =========================================================================
    // The runtime-`.then(` scan (code only — comments and strings never count)
    // =========================================================================

    private static List<String> thenCarriers(Map<String, String> tree) {
        TreeSet<String> hits = new TreeSet<>();
        for (Map.Entry<String, String> e : tree.entrySet()) {
            if (!e.getValue().contains(".then(")) {
                continue;
            }
            if (codeOnly(e.getValue()).contains(".then(")) {
                hits.add(e.getKey());
            }
        }
        return new ArrayList<>(hits);
    }

    private static void assertNoRuntimeThen(String out, String which) {
        assertFalse(codeOnly(out).contains(".then("),
                which + " must not emit the runtime `.then(` form (no Mapper.then(Function) exists):\n" + out);
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

    private static Map<String, String> readGoldenTree(Path root) throws IOException {
        Map<String, String> out = new LinkedHashMap<>();
        try (var stream = Files.walk(root)) {
            for (Path p : stream.filter(q -> q.toString().endsWith(".java")).sorted().toList()) {
                out.put(root.relativize(p).toString().replace('\\', '/'), Files.readString(p));
            }
        }
        return out;
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n");
    }

    // =========================================================================
    // Cell generation (drr 5.61.0 legacy; the IR route for control2)
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
                + path + " — seat 26 law A: the in-lambda then chain hoists.");
    }

    // =========================================================================
    // Fixture harness
    // =========================================================================

    private static RLinkingResult linking;
    private static RModel mainModel;
    private static Map<String, String> fixtureOut;

    private static void link() throws IOException {
        if (linking == null) {
            RModel main = AstBuilder.buildFromString(MODEL, "seat26f7a.rosetta");
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
            fixtureOut = render(m -> "census.seat26f7a".equals(m.namespace()));
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
            throw new AssertionError("[InLambdaThenChainCtlAdmitSeatTest] builtins parse"
                    + " failures: " + String.join("; ", failures));
        }
        return models;
    }
}
