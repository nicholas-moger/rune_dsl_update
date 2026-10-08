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
import java.util.function.Predicate;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.function.LabelProviderGenerator;
import com.regnosys.rosetta.generator.java.function.LabelProviderGeneratorUtil;
import com.regnosys.rosetta.generator.java.object.ChoiceObjectGenerator;
import com.regnosys.rosetta.generator.java.object.ModelObjectGenerator;
import com.regnosys.rosetta.generator.java.object.datarule.DataRuleGenerator;
import com.regnosys.rosetta.generator.java.reports.ReportGenerator;
import com.regnosys.rosetta.generator.java.reports.RuleGenerator;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.symbols.RLinkingResult;
import com.regnosys.rosetta.symbols.RWorkspace;
import com.regnosys.rosetta.utils.DeepFeatureCallUtil;
import static com.regnosys.rosetta.testutil.GenerationErrors.assertNoGenerationErrors;

/**
 * SEAT 16 — facet {@code nullTypedIteLiteralArmLift}: <b>a conditional arm with NO compiled
 * expression type must still be lifted to the seat's cardinality.</b>
 *
 * <p><b>The law.</b> {@code ControlFlowHandler.wrapDeepThenIteArm}'s first arm (facet
 * {@code mapperCIteLift}, PR #364) coerces a SINGLE wrap-factory arm to the MULTI seat's
 * {@code MapperC.of(Collections.singletonList(…))}. Its own javadoc names a STRING-LITERAL
 * carrier — golden {@code GetReportTrackingNumber}'s {@code "RTNNotProvided"} — but it gated on
 * {@code getExpressionType() != null && isMapperS(getExpressionType())}, and <b>a literal has no
 * compiled expression type</b>. So the gate could never fire for the very shape it was written
 * for, and the fork emitted {@code ifThenElseResult = MapperS.of("NON-CA");} against a local it
 * had already correctly declared {@code MapperC<String>}.
 *
 * <p><b>That does not compile</b>, and this is asserted on a COMPILER, not a derivation (LAW 74,
 * born at #586 when a signature-level argument was used to retract a true disclosure and was
 * itself refuted). javac on the two shapes against the shipped {@code rune-runtime}:
 * <pre>
 *   PRE : final MapperC&lt;String&gt; r; … r = MapperS.of("NON-CA");
 *         error: incompatible types: no instance(s) of type variable(s) T exist so that
 *                MapperS&lt;T&gt; conforms to MapperC&lt;String&gt;
 *   POST: r = MapperC.of(Collections.singletonList("NON-CA"));           compiles
 * </pre>
 *
 * <p><b>The producer was CONFIRMED BY RUNTIME PROBE (LAW 72), and the seat's BANKED producer line
 * was WRONG.</b> The menu banked this lever at {@code ReferenceHandler:3839-3857}; that site's own
 * javadoc names csa <i>UATPI</i> Leg1/2 — the META-wrapper family, a different carrier set with a
 * different defect. Instrumenting {@code wrapDeepThenIteArm} over drr 7.0.0 printed <b>117 arm
 * sites</b>, of which the real carrier appears twice, verbatim:
 * {@code multi=true type=null isMapperS=false unwrap=true node=RStringLiteral
 * render=MapperS.of("NON-CA")} — reached, correctly MULTI, recognisably a single wrap, failing
 * ONLY the null check. Fourth seat running whose charted producer needed a probe.
 * See {@code target/seat16-charter.md}.
 *
 * <p><b>Why the fix is an EXACT render-truth identity, and why the FIRST answer to that was
 * WRONG.</b> An earlier revision argued the identity was needed to stop a {@code startsWith} test
 * "stealing" the eight {@code RConstructorExpr} arms measured in the same cell. <b>The #588
 * independent review REFUTED that:</b> a constructor arm renders through
 * {@code ImportCollisionResolver.typeRef}, so it begins with the private-use sentinel
 * {@code U+E000} and {@code startsWith("MapperS.of(")} is false for it exactly as {@code equals}
 * is. The claim was recorded here and in {@code src/main}; both are corrected, because a safety
 * argument that is wrong about WHY it is safe is worth no more than luck.
 *
 * <p>The real argument is a structural ENUMERATION, and it is cell-independent — so unlike the
 * 117-site probe it cannot be undone by a corpus this seat never dumped. Only FOUR factories ever
 * set {@code unwrapToBuilder}: {@code JavaExpression:177} {@code wrappedInMapperSOf} (renders
 * {@code MapperS.of(}), {@code :349} {@code MapperCOfSingleWrap} ({@code MapperC.<}), {@code :377}
 * {@code witnesslessForm} ({@code MapperC.of(}) and {@code :401} {@code selfUnwrapping} (renders
 * its inner VERBATIM). Only {@code selfUnwrapping} can produce an arbitrary prefix, so it is the
 * only shape a prefix test could wrongly admit — and {@code equals} structurally cannot admit it,
 * since that would require {@code render(inner) == "MapperS.of(" + render(inner) + ")"}. The
 * identity therefore holds <b>iff</b> the arm is a {@code wrappedInMapperSOf} wrap.
 *
 * <p><b>The sibling arm is deliberately NOT mirrored (LAW 69 considered and DECLINED, on
 * measurement).</b> Among null-typed MULTI arms whose {@code unwrapToBuilder} is EMPTY, four
 * render {@code MapperS.of(x).mapC(…)} — chains whose RESULT is already {@code MapperC}. A
 * render-prefix test there would wrap an already-multi value a second time. They are excluded
 * from this arm by the pre-existing {@code unwrapToBuilder().isPresent()} condition.
 *
 * <p><b>Green-safety is the PREDICATE, never a token</b> (the #585 REFUTATION): where this gate
 * fires, a {@code MapperS} single wrap is being assigned to a {@code MapperC}-typed local, which
 * never compiled — so no byte-identical file can carry the pre-fix form at an accepted site. The
 * #588 review verified that over the FULL population — all 174,141 goldens across 25 cells, with
 * a paren-balanced scan — and found ZERO green files carrying the pre-fix form at an accepted
 * site.
 *
 * <p><b>A FIXTURE LESSON, recorded because this suite nearly shipped without one.</b> The first
 * draft of the fixture would not type-infer, and the draft's own diagnosis — "an extract BLOCK
 * does not resolve in an isolated workspace" — was WRONG, as the #588 review showed by pointing at
 * two green in-tree counterexamples ({@code DeclaredThenArgTypeSeatTest:237},
 * {@code ArgumentPositionRuleWrapSeatTest:190}). Two real causes, both cheap once named:
 * {@code tag} and {@code root} are RESERVED words in this grammar (the parse error names the
 * offending token, not the construct), and an extract block must navigate from its DECLARED
 * PARAMETER ({@code extract inn [ inn -> … ]}), not from a bare feature name. The suite had been
 * about to ship corpus-only with that false diagnosis written into its javadoc as a disclosure.
 * <b>A disclosure is not a substitute for a diagnosis.</b>
 */
class NullTypedIteArmLiftSeatTest {

    private static final Path REPO_ROOT =
            Path.of(System.getProperty("user.dir")).resolve("..").normalize();

    private static final List<Path> BUILTINS_SEARCH_ROOTS = List.of(
            REPO_ROOT.resolve("test-corpus/rune-dsl-builtins/rune-runtime/src/main/resources/model"),
            REPO_ROOT.resolve("rune-dsl/rune-runtime/src/main/resources/model"),
            // THIS repo's own builtins, LAST so the roots above keep their precedence — a
            // checkout without the external corpus RUNS these tests instead of skipping them
            // (#586: a test that skips is not a lock).
            REPO_ROOT.resolve("rune-runtime/src/main/resources/model"));

    static boolean builtinsAvailable() {
        return BUILTINS_SEARCH_ROOTS.stream().anyMatch(Files::isDirectory);
    }

    /**
     * The fixture mirrors the CORPUS shape (the #583 fixture-truth law), not a convenient one:
     * csa {@code JurisdictionOfCounterparty1} is {@code extract <field> [ <MULTI nav> then …
     * then extract <leaf> then if item exists then item else "NON-CA" ]}. A fixture whose ite sat
     * at the rule's top level would exercise a different seat entirely.
     */
    private static final String MODEL_DEP = """
            namespace census.seat16.dep
            version "1.0.0"

            type Leaf: <"the leaf whose tag the extract reads">
                amt string (0..1)

            type Holder: <"carries a MULTI arm and a SINGLE arm of the same leaf type">
                leaves Leaf (0..*)
                one Leaf (0..1)

            type Root: <"the rule input">
                holder Holder (0..1)
            """;

    private static final String MODEL_MAIN = """
            namespace census.seat16
            version "1.0.0"

            import census.seat16.dep.*

            reporting rule MultiLiteralArm from Root: <"THE CARRIER - a MULTI ite whose ELSE arm is a bare string literal">
                extract inn [
                    inn -> holder -> leaves -> amt
                        then if item exists then item else "FALLBACK"
                ]

            reporting rule SingleLiteralArm from Root: <"b1 - the SINGLE seat: the same literal arm must NOT be list-lifted">
                extract inn [
                    inn -> holder -> one -> amt
                        then if item exists then item else "FALLBACK"
                ]
            """;

    // ---- the tokens ---------------------------------------------------------
    private static final String MULTI_LIFT =
            "MapperC.of(Collections.singletonList(\"FALLBACK\"))";
    private static final String SINGLE_WRAP = "MapperS.of(\"FALLBACK\")";

    // =========================================================================
    // Part A — the carrier
    // =========================================================================

    /**
     * a1 — the MULTI seat lifts the null-typed literal arm to a singleton list. Failing-first:
     * at the pre-seat emitter this is {@code MapperS.of("FALLBACK")} against a
     * {@code MapperC}-typed local, which does not compile.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_multiSeatLiftsTheNullTypedLiteralArm() throws IOException {
        String out = filtered("MultiLiteralArmRule.java");
        // anti-vacuity: the fixture really does render the ite at a MULTI seat
        assertContains(out, "final MapperC<String> ifThenElseResult");
        assertContains(out, MULTI_LIFT);
        assertNotContains(out, SINGLE_WRAP);
    }

    /** a2 — the lift pulls in {@code java.util.Collections}, exactly as golden does. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_theLiftRegistersTheCollectionsImport() throws IOException {
        String out = filtered("MultiLiteralArmRule.java");
        assertContains(out, "import java.util.Collections;");
    }

    // =========================================================================
    // Part B — decline controls (each asserts its fixture REACHED the seat)
    // =========================================================================

    /**
     * b1 — the SINGLE seat must keep the bare {@code MapperS.of(…)}. The lift is a
     * cardinality coercion, not a rewrite: at a single seat there is nothing to coerce.
     *
     * <p>The first assertion is the ANTI-VACUITY pin — without it a fixture that never rendered
     * an ite at all would pass. Three controls in the seat-15 suite shipped mis-designed for
     * exactly this reason.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b1_singleSeatKeepsTheBareWrap() throws IOException {
        String out = filtered("SingleLiteralArmRule.java");
        assertContains(out, "final MapperS<String> ifThenElseResult");
        assertContains(out, SINGLE_WRAP);
        assertNotContains(out, MULTI_LIFT);
    }

    // =========================================================================
    // Part C — corpus locks (drr 7.0.0) + the cross-cell identity
    // =========================================================================

    private static final Path DRR7_CELL_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path DRR7_GOLDEN_DIR =
            DRR7_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    static boolean drr7Available() {
        return Drr7Corpus.gate(Files.isDirectory(DRR7_CELL_ROOT) && Files.isDirectory(DRR7_GOLDEN_DIR), NullTypedIteArmLiftSeatTest.class);
    }

    private static final String JURIS1 =
            "drr/regulation/csa/rewrite/trade/reports/JurisdictionOfCounterparty1Rule.java";
    private static final String JURIS2 =
            "drr/regulation/csa/rewrite/trade/reports/JurisdictionOfCounterparty2Rule.java";

    /** c1 — the whole-file heal. Every one of its 3 diff-lines was this seat's. */
    @Test
    @EnabledIf("drr7Available")
    void corpus_c1_jurisdictionOfCounterparty1() throws IOException { lock(JURIS1); }

    /** c2 — its twin. */
    @Test
    @EnabledIf("drr7Available")
    void corpus_c2_jurisdictionOfCounterparty2() throws IOException { lock(JURIS2); }

    /**
     * c3 — THE CROSS-CELL IDENTITY that makes pinning drr 7.0.0 pin all FOUR cells. The state is
     * identical in drr 7.1.0/7.2.0/7.3.0; rather than generate three more cells (a 4x cost for
     * two assertions — the seat-14 precedent), pin that the goldens are byte-identical. If a
     * sibling cell's golden ever diverges, this stops standing in for it and fails loudly.
     */
    @Test
    @EnabledIf("drr7Available")
    void corpus_c3_theSiblingCellsGoldensAreIdentical() throws IOException {
        int compared = 0;
        for (String rel : List.of(JURIS1, JURIS2)) {
            Path base = DRR7_GOLDEN_DIR.resolve(rel);
            assertTrue(Files.isRegularFile(base), "golden missing: " + base);
            String expected = normalize(Files.readString(base));
            for (String v : List.of("7.1.0", "7.2.0", "7.3.0")) {
                Path sib = Path.of("../test-corpus/drr/drr-" + v)
                        .resolve("rosetta-source/src/generated/java").resolve(rel);
                // NOT a `continue` (the #588 review's MUST-FIX 3): skipping an absent sibling
                // would let this test PASS while asserting nothing — and it is the ONLY thing
                // extending a 2-file lock to the claimed 8 whole-file heals. All three siblings
                // are in the frozen contract (corpus-baseline-9.83.manifest), so absence is a
                // broken checkout and must fail loudly.
                assertTrue(Files.isRegularFile(sib),
                        "sibling cell golden missing: " + sib + " — drr " + v + " is in the frozen"
                        + " 9.83.0 manifest, so this checkout is incomplete and drr 7.0.0 can no"
                        + " longer stand in for it.");
                assertEquals(expected, normalize(Files.readString(sib)),
                        "drr " + v + "'s golden for " + rel + " has diverged from drr 7.0.0's —"
                        + " this cell no longer stands in for it, so pin " + v + " directly.");
                compared++;
            }
        }
        assertEquals(6, compared, "expected 2 files x 3 sibling cells = 6 comparisons");

        // FORK-SIDE identity too, not just golden-side: the stand-in argument needs the INPUT to
        // be the same as well as the expected output. The csa rule source is byte-identical
        // across all four cells, so the fork compiles the same AST in each.
        String src0 = normalize(Files.readString(Path.of("../test-corpus/drr/drr-7.0.0")
                .resolve("rosetta-source/src/main/rosetta/regulation-csa-rewrite-trade-rule.rosetta")));
        for (String v : List.of("7.1.0", "7.2.0", "7.3.0")) {
            Path src = Path.of("../test-corpus/drr/drr-" + v)
                    .resolve("rosetta-source/src/main/rosetta/regulation-csa-rewrite-trade-rule.rosetta");
            assertTrue(Files.isRegularFile(src), "sibling cell source missing: " + src);
            assertEquals(src0, normalize(Files.readString(src)),
                    "drr " + v + "'s csa rule SOURCE has diverged from drr 7.0.0's — the fork no"
                    + " longer compiles the same AST there, so pin " + v + " directly.");
        }
    }

    /**
     * control — THE POSITIVE CONTROL FOR THE EXACT-IDENTITY GATE. {@code Notional.java} carries
     * the CONSTRUCTOR arm form ({@code MapperC.of(Collections.singletonList(Measure.builder()…))})
     * emitted by the LATER {@code fnNotionalTogetherRestructure} arm (PR #398). Eight such arms
     * are null-typed, MULTI and unwrappable in this cell — i.e. they satisfy every part of this
     * seat's widened predicate EXCEPT the exact render-truth identity. Had the gate been written
     * as a {@code startsWith} test it would have stolen them and rewritten their bytes.
     *
     * <p><b>What it does and does NOT prove — corrected after the #588 review.</b> It is NOT an
     * exact-vs-prefix discriminator: a constructor arm's render begins with the private-use
     * sentinel {@code U+E000} (via {@code ImportCollisionResolver.typeRef}), so a
     * {@code startsWith} gate would reject it too. What it DOES prove is that the widened
     * predicate is genuinely REACHED by a non-carrier: {@code ConstructionHandler:418} builds the
     * arm through {@code JavaExpression.selfUnwrapping} with a {@code null} inner type, so this
     * file's arms really do satisfy {@code multi && getExpressionType()==null && unwrap} and are
     * declined only by the identity. The exact-vs-prefix safety rests on the structural
     * enumeration in this class's javadoc, not on this file.
     *
     * <p>This file is byte-identical to golden BOTH before and after the seat, so it is a real
     * green-safety witness rather than a token scan.
     */
    @Test
    @EnabledIf("drr7Available")
    void corpus_control_ctorArmKeepsItsOwnForm() throws IOException {
        String path = "drr/standards/iosco/cde/version1/quantity/functions/Notional.java";
        assertNotNull(drr7Output, "drr 7.0.0 generation did not run — corpus unavailable?");
        String generated = drr7Output.get(path);
        assertNotNull(generated, "not generated: " + path);
        assertContains(generated,
                "MapperC.of(Collections.singletonList(Measure.builder()");
        Path goldenPath = DRR7_GOLDEN_DIR.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "golden missing: " + goldenPath);
        assertEquals(normalize(Files.readString(goldenPath)), normalize(generated),
                "the constructor-arm witness moved — this seat's exact render-truth identity has"
                + " widened into fnNotionalTogetherRestructure's territory (PR #398): " + path);
    }

    /**
     * control — the OTHER csa family's state, re-pinned when it moved. {@code UATPI Leg1/2}
     * carried a META-wrapper defect (8 diff-lines each) which this seat does NOT own; the
     * original pin was that the un-healed form SURVIVES, "so a future seat that heals it fails
     * here deliberately and re-pins" — and that is exactly what happened: seat 25 law B (facet
     * {@code ruleCallArmMetaWrap}, the {@code RuleCallArmMetaWrapSeatTest} byte locks) healed the
     * arms with the in-branch value hoist + null-safe builder wrap, and this control fired on the
     * seat's first full gensuite (LAW 81, the fourth firing). The pin now holds the HEALED form:
     * the bare {@code MapperS.of(<rule>.evaluate(item.get()))} arm is GONE and the wrap ternary
     * stands.
     */
    @Test
    @EnabledIf("drr7Available")
    void corpus_control_theMetaWrapperSiblingHealedAtSeat25() throws IOException {
        String path = "drr/regulation/csa/rewrite/trade/reports/"
                + "UnderlyingAssetTradingPlatformIdentifierLeg1Rule.java";
        assertNotNull(drr7Output, "drr 7.0.0 generation did not run — corpus unavailable?");
        String generated = drr7Output.get(path);
        assertNotNull(generated, "not generated: " + path);
        assertContains(generated,
                "final String string0 = underlyingAssetTradingPlatformIdentifierLeg1Rule"
                + ".evaluate(item.get());");
        assertContains(generated,
                "return string0 == null ? MapperS.<FieldWithMetaString>ofNull()"
                + " : MapperS.of(FieldWithMetaString.builder().setValue(string0).build());");
        assertNotContains(generated,
                "return MapperS.of(underlyingAssetTradingPlatformIdentifierLeg1Rule"
                + ".evaluate(item.get()));");
    }

    private static void lock(String path) throws IOException {
        assertNotNull(drr7Output, "drr 7.0.0 generation did not run — corpus unavailable?");
        List<String> lockedErrors = drr7GenErrors.stream()
                .filter(e -> e.contains(path)).toList();
        assertTrue(lockedErrors.isEmpty(),
                "the generator reported errors for the locked file " + path + ": " + lockedErrors);
        String generated = drr7Output.get(path);
        assertNotNull(generated, "not generated: " + path);
        Path goldenPath = DRR7_GOLDEN_DIR.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "golden missing: " + goldenPath);
        assertEquals(normalize(Files.readString(goldenPath)), normalize(generated),
                "generated drr 7.0.0 output must byte-match golden (newline-normalized) for "
                + path + " — seat 16: a null-typed literal ite arm must be lifted to the MULTI"
                + " seat's cardinality.");
    }

    // =========================================================================
    // Harness
    // =========================================================================

    private static Map<String, String> drr7Output;
    private static List<String> drr7GenErrors = new ArrayList<>();

    @BeforeAll
    static void generateDrr7() throws IOException {
        if (drr7Available()) {
            drr7Output = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", DRR7_CELL_ROOT));
        }
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
        var dataRuleGen = new DataRuleGenerator(gm, typeTranslator, typeUtil);
        var labelProviderGen = new LabelProviderGenerator(
                gm, typeTranslator, new DeepFeatureCallUtil(gm::getType),
                new LabelProviderGeneratorUtil());
        Map<String, String> output = new LinkedHashMap<>();
        for (RModel model : corpus.workspace().files()) {
            if (gm.shouldGenerate(model)) {
                String version = gm.version(model);
                assertNoGenerationErrors(pojoGen.generateClasses(model, version, output));
                assertNoGenerationErrors(choiceGen.generateClasses(model, version, output));
                assertNoGenerationErrors(ruleGen.generateClasses(model, version, output));
                assertNoGenerationErrors(reportGen.generateClasses(model, version, output));
                assertNoGenerationErrors(dataRuleGen.generateClasses(model, version, output));
                assertNoGenerationErrors(labelProviderGen.generateClasses(model, version, output));
            }
        }
        // Copilot #588: generateWithErrors returns per-function failures and its javadoc warns
        // against dropping them. Discarding it lets a byte-lock pass while unrelated functions in
        // the same cell failed to generate — green for the wrong reason. Captured and surfaced by
        // `lock()` below, which refuses to compare a file the generator reported an error for.
        drr7GenErrors = new ArrayList<>();
        funcGen.generateWithErrors(output)
                .forEach(e -> drr7GenErrors.add(e.getTargetPath() + " — " + e));
        return output;
    }

    private static RLinkingResult linking;
    private static RModel mainModel;
    private static Map<String, String> filteredOut;

    private static void link() throws IOException {
        if (linking == null) {
            RModel dep = AstBuilder.buildFromString(MODEL_DEP, "seat16-dep.rosetta");
            RModel main = AstBuilder.buildFromString(MODEL_MAIN, "seat16.rosetta");
            dep.setVersion("0.0.0.test");
            main.setVersion("0.0.0.test");
            List<RModel> models = new ArrayList<>();
            models.add(dep);
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

    private static String filtered(String fileName) throws IOException {
        if (filteredOut == null) {
            filteredOut = render(m -> "census.seat16".equals(m.namespace()));
        }
        return lookup(filteredOut, fileName);
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
            throw new AssertionError("[NullTypedIteArmLiftSeatTest] builtins parse failures: "
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
