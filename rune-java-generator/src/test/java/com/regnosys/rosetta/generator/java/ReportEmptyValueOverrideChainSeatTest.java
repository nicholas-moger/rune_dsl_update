package com.regnosys.rosetta.generator.java;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.function.LabelProviderGenerator;
import com.regnosys.rosetta.generator.java.function.LabelProviderGeneratorUtil;
import com.regnosys.rosetta.generator.java.object.ChoiceObjectGenerator;
import com.regnosys.rosetta.generator.java.object.ModelObjectGenerator;
import com.regnosys.rosetta.generator.java.reports.ReportGenerator;
import com.regnosys.rosetta.generator.java.reports.RuleGenerator;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.symbols.RLinkingResult;
import com.regnosys.rosetta.symbols.RWorkspace;
import com.regnosys.rosetta.utils.DeepFeatureCallUtil;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.lang.ref.Reference.reachabilityFence;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static com.regnosys.rosetta.testutil.GenerationErrors.assertNoGenerationErrors;

/**
 * v3.1 LADDER RETIREMENT — flip seat 10: the report-seat pair (the LAW-65
 * charter {@code target/seat10-charter.md}, written before any emitter edit;
 * the line-grain trace {@code target/seat10-mechanism-trace.md}). Two
 * DISJOINT rungs in {@code FunctionExpressionRenderer}'s report seat, 4
 * whole-file drr 7.x carriers, drr-only (report functions exist only in drr):
 *
 * <p><b>F1 — the report empty-value form</b> (facet reportEmptyValueForm; csa
 * CSAPPDReportFunction 17 + sec SECPPDReportFunction 14 lines):
 * {@code renderReportRuleSetOrNull} Arm 1 emitted the bare {@code null} for a
 * Void-typed ({@code empty}-bodied) report rule REGARDLESS of the leaf's
 * cardinality; upstream {@code TypeCoercionService} (Void ⇒ expected.empty ⇒
 * {@code isList ? Collections.<X>emptyList() : JavaLiteral.NULL}) renders the
 * MULTI leaf's empty as {@code Collections.<Item>emptyList()}. The
 * discriminator is the leaf's own declared cardinality — witnessed inside the
 * carrier goldens themselves (CSAPPD: 22 {@code (0..1)} emptyRule setter sites
 * stay {@code null} vs exactly 4 {@code (0..*)} sites take {@code emptyList()}).
 *
 * <p><b>F2 — the override-chain setter name</b> (facet overrideChainSetterName;
 * esma ESMAEMIRTradeReportFunction + fca FCAUKEMIRTradeReportFunction, 7 sites
 * each): GOLDEN calls {@code .setEffectiveDateOverriddenAsDate(…)} etc. where
 * the fork emitted the PLAIN name. {@code overrideSetterRenameOrNull} was a
 * ONE-STEP comparison against the nearest declaring supertype and returned
 * null (= plain) at Case 0 / different-erasure — exactly where the POJO law
 * ({@code RJavaPojoInterface.addProperty}, "a faithful port of upstream
 * addPropertyIfNecessary") REUSES the parent property's
 * {@code setterCompatibilityName}: Case 0 keeps the parent PROPERTY in place
 * (its possibly-renamed setter continues to represent the attribute) and a
 * different-erasure specialization INHERITS the parent's name. The fork's own
 * #322 note predicted "a future corpus carrying a doubly-specialized override
 * chain would surface as a NEW divergence" — drr 7.x IS that corpus (the
 * ESMA/FCA depth-3 chains: {@code CriticalDataElement (0..1)} →
 * {@code EMIRTransactionReport override … (1..1)} → the leaf's Case-0
 * re-declaration). The method now RECURSES up the parent chain — the two
 * halves (POJO declaration + call-site selection) cannot disagree; the
 * function-path deep-SET consumer heals with it (a4). Depth-≤2 chains answer
 * exactly as before (the 4 in-file matchers; the 23 #322 carriers; the drr
 * 6.36.0 depth-2 control).
 *
 * <p><b>Test geometry (the seat-1..9 pattern):</b> same-workspace controls
 * (RED pre-seat) + inert pins (GREEN before AND after) + drr 7.0.0 corpus
 * locks (ALL FOUR whole-file byte locks, two-signature-closed
 * single-mechanism). Fixture-truth honoured: every PRE form was probed and
 * recorded before any assertion was frozen (the temporary Seat10ProbeTest,
 * deleted with the seat landing). The fixture harness ADDS ReportGenerator
 * (and the POJO generator, for the two-halves-agree witness) to the seat-9
 * rule+function harness.
 */
class ReportEmptyValueOverrideChainSeatTest {

    private static final Path REPO_ROOT =
            Path.of(System.getProperty("user.dir")).resolve("..").normalize();

    private static final List<Path> BUILTINS_SEARCH_ROOTS = List.of(
            REPO_ROOT.resolve("test-corpus/rune-dsl-builtins/rune-runtime/src/main/resources/model"),
            REPO_ROOT.resolve("rune-dsl/rune-runtime/src/main/resources/model")
    );

    static boolean builtinsAvailable() {
        return BUILTINS_SEARCH_ROOTS.stream().anyMatch(Files::isDirectory);
    }

    // =========================================================================
    // F1 control model — the empty-value form at unit grain
    // =========================================================================

    /**
     * {@code Rep.extra} carries the CSAPPD shape: an {@code empty}-bodied rule
     * bound to MULTI leaves ({@code dests} a String list, {@code grades} a
     * data-type list, {@code codes} a BOUNDED string list — the
     * {@code (0..18)} otherPaymentPayerIDType class) and to a SINGLE leaf
     * ({@code venue} — stays {@code null}); {@code venues} a MULTI leaf with a
     * NON-empty rule (unchanged); {@code note} the bare
     * {@code [ruleReference empty]} (no setter at all).
     */
    private static final String F1_MODEL = """
            namespace census.seat10a
            version "1.0.0"

            type Instr:
                names string (0..*)
                flag string (0..1)

            type Grade:
                g string (0..1)

            type Extra:
                dests string (0..*)
                grades Grade (0..*)
                codes string (0..18)
                venue string (0..1)
                venues string (0..*)
                note string (0..1)

            type Rep:
                extra Extra (0..1)
                    [ruleReference for dests EmptyRule]
                    [ruleReference for grades EmptyRule]
                    [ruleReference for codes EmptyRule]
                    [ruleReference for venue EmptyRule]
                    [ruleReference for venues NamesRule]
                    [ruleReference for note empty]

            reporting rule EmptyRule from Instr:
                empty

            reporting rule NamesRule from Instr: <"a non-empty multi rule">
                extract names

            eligibility rule IsRep from Instr: <"eligibility">
                flag exists

            report Test Seat10 in T+1
                from Instr
                when IsRep
                with type Rep
            """;

    // =========================================================================
    // F2 control model — the override chain at unit grain
    // =========================================================================

    /**
     * {@code Base → Middle → Leaf} replicates the ESMA chain: {@code effDate}
     * is {@code (0..1)} at the root, specialized {@code (1..1)} at the middle
     * (WITH the rule binding — the EMIR shape) and re-declared Case-0 at the
     * leaf (no rule — the ESMA shape) → depth 3, the setter name is the
     * middle's rename. {@code lvl} is the depth-2 leaf-only specialization
     * (the 4 in-file matchers — its OWN rename fires). {@code sub} is a
     * DIFFERENT-erasure specialization at the middle ({@code Sub → SubX})
     * re-declared Case-0 at the leaf → the plain overload, inherited (b5).
     * {@code bag} nests a Middle so the report ALSO renders a depth-2 setter on
     * a nested path (b8). {@code SetLeaf} is the FUNCTION-path deep-SET twin
     * over the same depth-3 chain (a4 — the second consumer of the shared
     * rename). {@code wid} is the a5 shape: root {@code Wide (0..1)} → middle
     * {@code Wide (1..1)} (renamed) → leaf {@code Narrow (1..1)} (different
     * erasure — INHERITS the middle's rename; the indep review's NIT-3).
     */
    private static final String F2_MODEL = """
            namespace census.seat10b
            version "1.0.0"

            type Instr:
                d date (0..1)
                n string (0..1)
                sub Sub (0..1)

            type Sub:
                sval string (0..1)

            type Base:
                effDate date (0..1)
                lvl string (0..1)
                sub Sub (0..1)
                bag Base (0..1)
                wid Wide (0..1)

            type Wide:
                w string (0..1)

            type Narrow extends Wide:
                nn string (0..1)

            type SubX extends Sub:
                extra string (0..1)

            type Middle extends Base:
                override effDate date (1..1)
                    [ruleReference EffRule]
                override sub SubX (0..1)
                override bag Middle (0..1)
                override wid Wide (1..1)

            type Leaf extends Middle:
                override effDate date (1..1)
                override lvl string (1..1)
                    [ruleReference LvlRule]
                override sub SubX (0..1)
                    [ruleReference SubRule]
                override wid Narrow (1..1)
                    [ruleReference WidRule]

            reporting rule EffRule from Instr: <"the middle-bound rule">
                extract d

            reporting rule LvlRule from Instr: <"the leaf-bound rule">
                extract n

            reporting rule SubRule from Instr: <"the sub rule">
                extract sub
                    then extract SubX {
                        sval: item -> sval,
                        extra: "x"
                    }

            reporting rule WidRule from Instr: <"the a5 rule">
                extract Narrow {
                    w: n,
                    nn: n
                }

            eligibility rule IsLeaf from Instr: <"eligibility">
                n exists

            report Test Seat10b in T+1
                from Instr
                when IsLeaf
                with type Leaf

            func SetLeaf: <"the FUNCTION deep-SET twin over the depth-3 chain">
                inputs:
                    d date (0..1)
                output:
                    out Leaf (1..1)
                set out -> effDate:
                    d
            """;

    // =========================================================================
    // Part A — controls (RED pre-seat)
    // =========================================================================

    /** F1 a1 — a MULTI String leaf with the empty rule takes
     *  {@code Collections.<String>emptyList()}; the Collections import returns.
     *  PRE (probed): {@code .setDests(null);}, no Collections import. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_emptyRule_multiStringLeaf_emptyList() throws IOException {
        String out = report(f1Output(), "TestSeat10ReportFunction.java");
        assertContains(out, ".setDests(Collections.<String>emptyList());");
        assertContains(out, "import java.util.Collections;");
        assertNotContains(out, ".setDests(null);");
    }

    /** F1 a2 — a MULTI data-type leaf takes {@code Collections.<Grade>emptyList()};
     *  the element-type import returns (the CSAPPD ProductGradeReport class);
     *  and the BOUNDED {@code (0..18)} string list is MULTI too (the
     *  otherPaymentPayerIDType class). PRE (probed): both {@code null}. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_emptyRule_dataTypeAndBoundedLeaves_emptyList() throws IOException {
        String out = report(f1Output(), "TestSeat10ReportFunction.java");
        assertContains(out, ".setGrades(Collections.<Grade>emptyList());");
        assertContains(out, "import census.seat10a.Grade;");
        assertContains(out, ".setCodes(Collections.<String>emptyList());");
    }

    /** F2 a3 — the depth-3 Case-0 leaf's REPORT setter takes the middle's
     *  rename ({@code setEffDateOverriddenAsDate}) — the two-halves-agree
     *  law. PRE (probed): {@code .setEffDate(effRuleRule.evaluate(input));}. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a3_depth3CaseZeroLeaf_reportSetterTakesMiddleRename() throws IOException {
        String out = report(f2Output(), "TestSeat10bReportFunction.java");
        assertContains(out, ".setEffDateOverriddenAsDate(effRuleRule.evaluate(input));");
        assertNotContains(out, "\t.setEffDate(effRuleRule.evaluate(input));");
    }

    /** F2 a4 — the FUNCTION-path deep-SET twin over the same depth-3 chain
     *  heals with the shared rename (the second consumer — LAW 67). PRE
     *  (probed): {@code .setEffDate(d);}. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a4_depth3CaseZeroLeaf_functionDeepSetTakesMiddleRename() throws IOException {
        String out = lookup(f2Output(), "SetLeaf.java");
        assertContains(out, ".setEffDateOverriddenAsDate(d);");
        assertNotContains(out, ".setEffDate(d);");
    }

    /** F2 a5 — the different-erasure INHERIT branch (indep review NIT-3): root
     *  {@code wid Wide (0..1)} → middle {@code override wid Wide (1..1)} (RENAMED,
     *  equal erasure) → leaf {@code override wid Narrow (1..1)} ({@code Narrow
     *  extends Wide} — a different-erasure specialization). The POJO law INHERITS
     *  the parent's compatibility name for a different-erasure specialization
     *  (upstream RJavaPojoInterface:154), so the leaf's report setter is the
     *  middle's {@code setWidOverriddenAsWide}; PRE: the plain
     *  {@code .setWid(…)} (the pre-seat one-step arm). Zero corpus carriers —
     *  divergence-grade, RED-capable for the inherit branch specifically. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a5_depth3DifferentErasureLeafOverRenamedMiddle_inheritsRename() throws IOException {
        String out = report(f2Output(), "TestSeat10bReportFunction.java");
        assertContains(out, ".setWidOverriddenAsWide(widRuleRule.evaluate(input));");
        assertNotContains(out, "	.setWid(widRuleRule.evaluate(input));");
        // the two-halves witness for this branch: the Leaf POJO declares it
        assertContains(lookup(f2Output(), "Leaf.java"), "setWidOverriddenAsWide(");
    }

    // =========================================================================
    // Part B — inert pins (GREEN pre-seat AND post-seat)
    // =========================================================================

    /** F1 b1 — a SINGLE {@code (0..1)} leaf with the empty rule stays
     *  {@code null} (the discriminator; the 22 in-file CSAPPD setter sites). */
    @Test
    @EnabledIf("builtinsAvailable")
    void b1_emptyRule_singleLeaf_staysNull() throws IOException {
        String out = report(f1Output(), "TestSeat10ReportFunction.java");
        assertContains(out, ".setVenue(null);");
    }

    /** F1 b2 — a MULTI leaf with a NON-empty rule keeps its evaluate render. */
    @Test
    @EnabledIf("builtinsAvailable")
    void b2_nonEmptyRule_multiLeaf_unchanged() throws IOException {
        String out = report(f1Output(), "TestSeat10ReportFunction.java");
        assertContains(out, ".setVenues(namesRuleRule.evaluate(input));");
    }

    /** F1 b3 — the bare {@code [ruleReference empty]} form emits NO setter
     *  (ReportGenerator declines the operation; untouched). */
    @Test
    @EnabledIf("builtinsAvailable")
    void b3_bareEmptyRuleReference_noSetter() throws IOException {
        String out = report(f1Output(), "TestSeat10ReportFunction.java");
        assertNotContains(out, ".setNote(");
    }

    /** F2 b4 — the depth-2 leaf-only requiredness specialization keeps its
     *  OWN rename (the 4 in-file ESMA/FCA matchers). */
    @Test
    @EnabledIf("builtinsAvailable")
    void b4_depth2LeafOnlyOverride_ownRenameUnchanged() throws IOException {
        String out = report(f2Output(), "TestSeat10bReportFunction.java");
        assertContains(out, ".setLvlOverriddenAsString(lvlRuleRule.evaluate(input));");
    }

    /** F2 b5 — THE adversarial placement pin: a depth-3 chain whose MIDDLE is
     *  a DIFFERENT-erasure specialization ({@code Sub → SubX}, the plain
     *  overload) and whose leaf is Case 0 stays PLAIN — the recursion returns
     *  the parent's inherited plain name, never invents a rename. */
    @Test
    @EnabledIf("builtinsAvailable")
    void b5_depth3OverDifferentErasureMiddle_staysPlain() throws IOException {
        String out = report(f2Output(), "TestSeat10bReportFunction.java");
        assertContains(out, ".setSub(subRuleRule.evaluate(input));");
        assertNotContains(out, ".setSubOverriddenAs");
    }

    /** F2 b8 — a depth-2 rename on a NESTED report path (getOrCreateBag ->
     *  the Middle's own effDate) keeps firing (an accidental extra control the
     *  probe surfaced — kept as a pin). */
    @Test
    @EnabledIf("builtinsAvailable")
    void b8_nestedPathDepth2Rename_unchanged() throws IOException {
        String out = report(f2Output(), "TestSeat10bReportFunction.java");
        assertContains(out, ".getOrCreateBag()\n\t\t\t\t.setEffDateOverriddenAsDate(effRuleRule.evaluate(input));");
    }

    /** F2 b9 — the two-halves-agree WITNESS: the Leaf POJO builder declares
     *  the middle-renamed setter the report/deep-SET seats now call (the POJO
     *  half was already right; the seat only re-pointed the call-site half). */
    @Test
    @EnabledIf("builtinsAvailable")
    void b9_leafPojoDeclaresTheRenamedSetter() throws IOException {
        String pojo = lookup(f2Output(), "Leaf.java");
        assertContains(pojo, "setEffDateOverriddenAsDate(");
    }

    // =========================================================================
    // Part C — drr 7.0.0 corpus locks (RED pre-seat; ALL FOUR WHOLE-FILE)
    // =========================================================================

    private static final Path DRR7_CELL_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path DRR7_GOLDEN_DIR =
            DRR7_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    /** The cell gate ALSO requires the builtins (the #579 R1-1 law): the D11
     *  loader fails LOUD by design when the corpus is present but the builtins
     *  are absent, so a corpus-only gate would run these locks into that
     *  AssertionError instead of skipping. */
    static boolean drr7Available() {
        return Drr7Corpus.gate(builtinsAvailable()
                && Files.isDirectory(DRR7_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR7_GOLDEN_DIR), ReportEmptyValueOverrideChainSeatTest.class);
    }

    private static Map<String, String> drr7Output;
    private static List<String> drr7GenErrors;

    @BeforeAll
    static void generateDrr7() throws IOException {
        if (drr7Available()) {
            drr7Output = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", DRR7_CELL_ROOT));
        }
    }

    /** F1 — the 4-setter carrier (incl. productGrade, the data-type element). */
    @Test
    @EnabledIf("drr7Available")
    void corpus_csappdReportFunction_wholeFile() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/csa/rewrite/trade/reports/CSAPPDReportFunction.java");
    }

    /** F1 — the 3-setter twin. */
    @Test
    @EnabledIf("drr7Available")
    void corpus_secppdReportFunction_wholeFile() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/sec/rewrite/trade/reports/SECPPDReportFunction.java");
    }

    /** F2 — the 7 depth-3 sites + the 4 depth-2 matchers in ONE file. */
    @Test
    @EnabledIf("drr7Available")
    void corpus_esmaEmirTradeReportFunction_wholeFile() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/esma/emir/refit/trade/reports/ESMAEMIRTradeReportFunction.java");
    }

    /** F2 — the FCA twin. */
    @Test
    @EnabledIf("drr7Available")
    void corpus_fcaUkemirTradeReportFunction_wholeFile() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/fca/ukemir/refit/trade/reports/FCAUKEMIRTradeReportFunction.java");
    }

    private static Map<String, String> generateCell(D11CorpusRegressionTest.CellSpec cell)
            throws IOException {
        var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(),
                D11CorpusRegressionTest.emissionFilter(cell));
        var typeUtil = new JavaTypeUtil();
        var typeTranslator = new JavaTypeTranslator(typeUtil);
        var pojoGen = new ModelObjectGenerator(gm, typeTranslator, typeUtil);
        var choiceGen = new ChoiceObjectGenerator(gm, typeTranslator, typeUtil, pojoGen);
        var funcGen = new FunctionGenerator(gm, typeTranslator, typeUtil);
        var ruleGen = new RuleGenerator(gm, typeTranslator, funcGen);
        var reportGen = new ReportGenerator(gm, typeTranslator, funcGen);
        var labelProviderGen = new LabelProviderGenerator(
                gm, typeTranslator, new DeepFeatureCallUtil(gm::getType),
                new LabelProviderGeneratorUtil());
        Map<String, String> output = new LinkedHashMap<>();
        drr7GenErrors = new ArrayList<>();
        for (RModel model : corpus.workspace().files()) {
            if (gm.shouldGenerate(model)) {
                String version = gm.version(model);
                assertNoGenerationErrors(pojoGen.generateClasses(model, version, output));
                assertNoGenerationErrors(choiceGen.generateClasses(model, version, output));
                ruleGen.generateClasses(model, version, output)
                        .forEach(e -> drr7GenErrors.add(e.getTargetPath() + " — " + e));
                assertNoGenerationErrors(reportGen.generateClasses(model, version, output));
                assertNoGenerationErrors(labelProviderGen.generateClasses(model, version, output));
            }
        }
        funcGen.generateWithErrors(output)
                .forEach(e -> drr7GenErrors.add(e.getTargetPath() + " — " + e));
        return output;
    }

    private static void assertByteMatchesGolden(String path) throws IOException {
        assertNotNull(drr7Output, "drr 7.0.0 generation did not run — corpus unavailable?");
        // Full-slash-path match (the #577 R2 law).
        List<String> lockedErrors = drr7GenErrors.stream()
                .filter(e -> e.contains(path)).toList();
        assertTrue(lockedErrors.isEmpty(),
                "generator reported errors for the locked file " + path + ": " + lockedErrors);
        String generated = drr7Output.get(path);
        assertNotNull(generated, "not generated: " + path);
        Path goldenPath = DRR7_GOLDEN_DIR.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "Golden missing: " + goldenPath);
        String golden = Files.readString(goldenPath);
        assertEquals(normalize(golden), normalize(generated),
                "Generated drr 7.0.0 output must byte-match the golden (newline-normalized) for "
                + path + " (seat 10 — the report-seat empty-value/override-chain pair).");
    }

    // =========================================================================
    // Harness (the seat-9 pattern + ReportGenerator + the POJO generator)
    // =========================================================================

    private static Map<String, String> f1OutputMap;
    private static Map<String, String> f2OutputMap;

    private static Map<String, String> f1Output() throws IOException {
        if (f1OutputMap == null) {
            f1OutputMap = buildOutput(F1_MODEL, "seat10a.rosetta");
        }
        return f1OutputMap;
    }

    private static Map<String, String> f2Output() throws IOException {
        if (f2OutputMap == null) {
            f2OutputMap = buildOutput(F2_MODEL, "seat10b.rosetta");
        }
        return f2OutputMap;
    }

    private static Map<String, String> buildOutput(String source, String fileName) throws IOException {
        RModel model = AstBuilder.buildFromString(source, fileName);
        model.setVersion("0.0.0.test");
        List<RModel> models = new ArrayList<>();
        models.add(model);
        models.addAll(loadBuiltinsOnly());
        RLinkingResult linkingResult = RWorkspace.build(models);
        GeneratorModel gm = new GeneratorModel(linkingResult.workspace());
        JavaTypeUtil typeUtil = new JavaTypeUtil();
        JavaTypeTranslator tt = new JavaTypeTranslator(typeUtil);
        FunctionGenerator fg = new FunctionGenerator(gm, tt, typeUtil);
        ModelObjectGenerator pojoGen = new ModelObjectGenerator(gm, tt, typeUtil);
        RuleGenerator ruleGen = new RuleGenerator(gm, tt, fg);
        ReportGenerator reportGen = new ReportGenerator(gm, tt, fg);
        Map<String, String> output = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        assertNoGenerationErrors(pojoGen.generateClasses(model, "1.0", output));
        ruleGen.generateClasses(model, "1.0", output)
                .forEach(e -> errors.add(e.getTargetPath() + " — " + e));
        assertNoGenerationErrors(reportGen.generateClasses(model, "1.0", output));
        fg.generateWithErrors(output)
                .forEach(e -> errors.add(e.getTargetPath() + " — " + e));
        reachabilityFence(linkingResult);
        if (!errors.isEmpty()) {
            throw new AssertionError("fixture generation errors (a broken fixture"
                    + " must fail loudly, not skip): " + errors);
        }
        return output;
    }

    private static String report(Map<String, String> output, String fileName) {
        return lookup(output, fileName);
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
            if (!Files.isDirectory(root)) continue;
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
                    try { models.add(AstBuilder.buildFromFile(p)); }
                    catch (Exception e) { failures.add(p + " — " + e); }
                });
        if (!failures.isEmpty()) {
            throw new AssertionError("[ReportEmptyValueOverrideChainSeatTest] builtins parse failures: "
                    + String.join("; ", failures));
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
        assertTrue(!out.contains(token),
                "forbidden token present: " + token + "\n--- in output:\n" + out);
    }
}
