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
 * v3.1 LADDER RETIREMENT — flip seat 11: the ladder-arm enum expected-owner
 * family (the LAW-65 census {@code target/seat11-census-trace.md} + charter
 * {@code target/seat11-charter.md}). A RULE-path ladder arm naming a bare enum
 * value that is DECLARED ON A SUPER-ENUM ({@code then Lei} where {@code Lei}
 * lives on {@code LeiIdentifierFormatEnum} and the rule's output is
 * {@code PartyIdentifierFormat2Enum extends LeiIdentifierFormatEnum}) rendered
 * {@code MapperS.of(LeiIdentifierFormatEnum.LEI)} — the value's DECLARING enum,
 * NON-COMPILING against the {@code MapperS<PartyIdentifierFormat2Enum>} seat —
 * where golden qualifies by the EXPECTED enum ({@code PartyIdentifierFormat2Enum
 * .LEI}; upstream {@code enumCall(feature, expectedType.getItemValueType)},
 * ExpressionGenerator.xtend:340-343 — generated Java flattens inherited values
 * under the child's name, the #211/#358 flatten law). The depth-2 shape
 * (OtherPaymentPayer/ReceiverFormat: rule output {@code PartyIdentifierFormatEnum
 * extends PartyIdentifierFormat2Enum extends LeiIdentifierFormatEnum}) qualifies
 * EVERY arm — including the middle enum's OWN values — by the leaf output enum.
 *
 * <p><b>The two defects in the #355 fnOutputLadderArmEnum arm:</b> (1)
 * {@code enclosingFunctionOutputEnumeration} read only a FUNCTION's declared
 * output typeCall — a RULE's output type is inferred-only (the synthetic
 * RFunction.fromRule output carries no declared typeCall when the body compiles;
 * RuleGenerator back-fills a name+id typeCall for the SIGNATURE only), so on the
 * rule path the arm never fired; the fix reads the enclosing RULE's inferred
 * output enum from the workspace inference over its expression — EXACTLY the
 * #370 {@code ComparisonHandler.ruleOutputEnumeration} read (LAW 67: the same
 * read, another consumer). (2) {@code functionOutputEnumConstantOrNull} iterated
 * {@code outEnum.values()} only (own values) and admitted only UNRESOLVED bare
 * symbols; the engine's Category-16 bind resolves the bare arm to the SUPER's
 * REnumValue, so the arm declined and the bound ref rendered through its
 * declaring enum. The fix: the #452 {@code boundBareEnumValue} third rung + the
 * hierarchy walk {@code findEnumValueInHierarchy} (the identical in-house law at
 * the 7 sibling qualifier sites) under the #215 SAME-INSTANCE descend-only gate,
 * qualifying by the EXPECTED enum, with the #391 parent-import removal.
 *
 * <p><b>Green-safety:</b> an own-value match finds the same value first, so
 * every pre-seat firing seat is byte-identical (the #391/#452 argument, banked
 * in-tree); the replaced render was NON-COMPILING at every carrier; the LAW-66
 * scan: golden super-qualified constants where the enclosing rule's output is
 * a strict sub-enum = 0 (positive control: the PRE gen dump enumerates the
 * carriers). FUNCTION-path ladders (the #355 home seat) already qualified
 * correctly (own + inherited) and are pinned unchanged (b1).
 */
class LadderArmEnumExpectedOwnerSeatTest {

    private static final Path REPO_ROOT =
            Path.of(System.getProperty("user.dir")).resolve("..").normalize();

    private static final List<Path> BUILTINS_SEARCH_ROOTS = List.of(
            REPO_ROOT.resolve("test-corpus/rune-dsl-builtins/rune-runtime/src/main/resources/model"),
            REPO_ROOT.resolve("rune-dsl/rune-runtime/src/main/resources/model")
    );

    static boolean builtinsAvailable() {
        return BUILTINS_SEARCH_ROOTS.stream().anyMatch(Files::isDirectory);
    }

    /**
     * {@code SubFmt extends BaseFmt} (Lei on the SUPER); {@code LeafFmt extends
     * SubFmt} (the depth-2 chain). A1 the hkma BrokerIdentifierFormat shape (a
     * rule whose output is SubFmt: bare {@code Lei} inherited + own {@code Swift}
     * + the qualified {@code SubFmt -> NaturalPerson} + the else {@code Other});
     * A2 the OtherPaymentPayerFormat depth-2 shape (a rule whose output is
     * LeafFmt: EVERY arm — the middle's own {@code Swift}/{@code Other} AND the
     * root's {@code Lei} — qualifies by LeafFmt); B1 the FUNCTION ladder control
     * (already correct); B2 an unrelated same-name value ({@code Other} on
     * {@code Unrelated}) in a rule whose output is Unrelated — qualifies by its
     * own enum (the same-instance gate); B3 a rule ladder whose arms are all
     * OWN values (the pre-seat firing seat — byte-identical).
     */
    private static final String MODEL = """
            namespace census.seat11
            version "1.0.0"

            enum BaseFmt:
                Lei
                LeiAndPerson

            enum SubFmt extends BaseFmt:
                NaturalPerson
                Swift
                Other

            enum LeafFmt extends SubFmt:
                Extra

            enum Unrelated:
                Other
                Some

            enum IdType:
                LEI
                BIC

            type Party:
                idType IdType (0..1)
                isPerson boolean (0..1)
                fmt BaseFmt (0..1)

            type Instr:
                party Party (0..1)
                flag boolean (0..1)

            reporting rule A1Ladder from Instr: <"a1 — the ladder-arm inherited value (depth 1)">
                extract party
                then extract
                    if isPerson = True
                    then SubFmt -> NaturalPerson
                    else if idType = LEI
                    then Lei
                    else if idType = BIC
                    then Swift
                    else Other

            reporting rule A2Depth2 from Instr: <"a2 — the depth-2 chain: every arm by the leaf output">
                extract party
                then extract
                    if isPerson = True
                    then LeafFmt -> Extra
                    else if idType = LEI
                    then Lei
                    else if idType = BIC
                    then Swift
                    else Other

            reporting rule B2Unrelated from Instr: <"b2 — a same-name value on an UNRELATED enum">
                extract party
                then extract
                    if isPerson = True
                    then Unrelated -> Some
                    else Other

            reporting rule B3Own from Instr: <"b3 — all-own-value arms (the pre-seat firing seat)">
                extract party
                then extract
                    if isPerson = True
                    then SubFmt -> NaturalPerson
                    else if idType = BIC
                    then Swift
                    else Other

            reporting rule A3CondSuper from Instr: <"a3 (MF-1 pin) — a rung CONDITION references the SUPER enum; a LATER inherited arm fires">
                extract party
                then extract
                    if isPerson = True
                    then SubFmt -> NaturalPerson
                    else if fmt = BaseFmt -> Lei
                    then Lei
                    else Other

            func B1Fn: <"b1 — the FUNCTION set-out ladder (the renderer seat; already correct)">
                inputs:
                    p Party (1..1)
                output:
                    out SubFmt (0..1)
                set out:
                    if p -> idType = LEI
                    then Lei
                    else if p -> idType = BIC
                    then Swift
                    else Other

            func B4MapBody: <"b4 — the FUNCTION map-body LADDER (the CollectionHandler arm's home seat)">
                inputs:
                    ps Party (0..*)
                output:
                    out SubFmt (0..*)
                set out:
                    ps extract [
                        if idType = LEI
                        then Lei
                        else if idType = BIC
                        then Swift
                        else Other
                    ]

            func B6SingleRung: <"b6 — a SINGLE-rung if/else is not a ladder: the arm is unreachable; the seat-12 ROOT covers it">
                inputs:
                    ps Party (0..*)
                output:
                    out SubFmt (0..*)
                set out:
                    ps extract [
                        if idType = LEI
                        then Lei
                        else Other
                    ]

            func TakesUnrelated: <"b5 helper">
                inputs:
                    us Unrelated (0..*)
                output:
                    n int (1..1)
                set n:
                    us count

            func B5Gate: <"b5 (THE gate pin) — a lambda whose POSITION enum (Unrelated) differs from the OUTPUT enum (SubFmt)">
                inputs:
                    ps Party (0..*)
                output:
                    out SubFmt (0..1)
                alias probe:
                    TakesUnrelated(ps extract [ if isPerson = True then Some else if idType = BIC then Other else Some ])
                set out:
                    if probe > 0
                    then Swift
                    else Other
            """;

    // =========================================================================
    // Part A — controls (RED pre-seat)
    // =========================================================================

    /** a1 — the inherited value qualifies by the rule's OUTPUT enum (the child);
     *  the own values and the qualified arm unchanged; the SUPER's import is
     *  NOT emitted. PRE (probed): {@code MapperS.of(BaseFmt.LEI)} + the BaseFmt
     *  import. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_ruleLadderInheritedValue_qualifiesByExpectedEnum() throws IOException {
        String out = rule("A1LadderRule.java");
        assertContains(out, "return MapperS.of(SubFmt.LEI);");
        assertContains(out, "return MapperS.of(SubFmt.SWIFT);");
        assertContains(out, "return MapperS.of(SubFmt.NATURAL_PERSON);");
        assertContains(out, "return MapperS.of(SubFmt.OTHER);");
        assertNotContains(out, "BaseFmt.LEI");
        assertNotContains(out, "import census.seat11.BaseFmt;");
    }

    /** a2 — the depth-2 chain (the OtherPaymentPayerFormat shape: a qualified
     *  leaf arm anchors the ladder; then EVERY bare arm — the middle's own
     *  Swift/Other AND the root's Lei — qualifies by the LEAF output enum, and
     *  neither ancestor's import is emitted). PRE (probed at corpus grain):
     *  {@code LeiIdentifierFormatEnum.LEI} / {@code PartyIdentifierFormat2Enum
     *  .SWIFTBIC} / {@code .OTHER} + both ancestor imports. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_depth2Chain_everyArmByLeafOutput() throws IOException {
        String out = rule("A2Depth2Rule.java");
        assertContains(out, "return MapperS.of(LeafFmt.EXTRA);");
        assertContains(out, "return MapperS.of(LeafFmt.LEI);");
        assertContains(out, "return MapperS.of(LeafFmt.SWIFT);");
        assertContains(out, "return MapperS.of(LeafFmt.OTHER);");
        assertNotContains(out, "SubFmt.");
        assertNotContains(out, "BaseFmt.");
        assertNotContains(out, "import census.seat11.SubFmt;");
        assertNotContains(out, "import census.seat11.BaseFmt;");
    }

    /** a3 — the indep review's MF-1 pin: a rung CONDITION that references the
     *  SUPER enum ({@code fmt = BaseFmt -> Lei}) keeps its {@code BaseFmt} import
     *  even though a LATER-processed inherited arm fires (a ladder-WIDE
     *  {@code refs.remove(declaring)} would have stripped it — non-compiling);
     *  the arm itself still qualifies by the expected enum. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a3_superEnumInRungCondition_keepsItsImport() throws IOException {
        String out = rule("A3CondSuperRule.java");
        assertContains(out, "MapperS.of(BaseFmt.LEI)");
        assertContains(out, "import census.seat11.BaseFmt;");
        assertContains(out, "return MapperS.of(SubFmt.LEI);");
        assertNotContains(out, "return MapperS.of(BaseFmt.LEI);");
    }

    // =========================================================================
    // Part B — inert pins (GREEN pre-seat AND post-seat)
    // =========================================================================

    /** b1 — the FUNCTION {@code set out:} ladder (the FunctionExpressionRenderer
     *  seat, NOT this arm) already qualified the inherited value by the declared
     *  output enum — unchanged (an unrelated-seat control; the indep review's NIT-2). */
    @Test
    @EnabledIf("builtinsAvailable")
    void b1_functionSetOutLadder_unchanged() throws IOException {
        String out = lookup(output(), "B1Fn.java");
        assertContains(out, "out = SubFmt.LEI;");
        assertContains(out, "out = SubFmt.SWIFT;");
        assertContains(out, "out = SubFmt.OTHER;");
    }

    /** b4 — the FUNCTION MAP-BODY LADDER (this arm's #355 home seat; a ladder =
     *  at least one {@code else if} — the {@code isLadderConditional} gate) with an
     *  inherited value: qualifies by the declared output enum via the hierarchy
     *  walk + the bound rung (RED pre-seat: {@code MapperS.of(BaseFmt.LEI)} + the
     *  BaseFmt import — the indep review's P4 probe; zero corpus FUNCTION carriers,
     *  the FUNCTION residue byte-identical). */
    @Test
    @EnabledIf("builtinsAvailable")
    void b4_functionMapBodyLadder_inheritedValueByOutputEnum() throws IOException {
        String out = lookup(output(), "B4MapBody.java");
        assertContains(out, "return MapperS.of(SubFmt.LEI);");
        assertContains(out, "return MapperS.of(SubFmt.SWIFT);");
        assertNotContains(out, "BaseFmt.LEI");
        assertNotContains(out, "import census.seat11.BaseFmt;");
    }

    /** b6 — the seat-12 ROOT-coverage pin (FLIPPED deliberately with seat 12, as
     *  this pin's seat-11 javadoc pre-registered): a SINGLE-rung {@code if … then
     *  Lei else Other} is NOT a ladder (no {@code else if}), so it routes to the
     *  #281 single-rung block sibling which never consults this arm — at seat 11
     *  the bare inherited value still rendered through its DECLARING enum
     *  ({@code BaseFmt.LEI}, the documented under-fire, zero corpus carriers). The
     *  seat-12 ROOT fix (ReferenceHandler's bare-enum arm qualifying by the node's
     *  INFERRED = expected type — the extract-body arm's expected type is the
     *  function output {@code SubFmt}, {@code TypeInferenceEngine}'s inline-body arm)
     *  covers this shape at the root: {@code SubFmt.LEI}, the BaseFmt import gone,
     *  the same answer this arm gives the ladder shapes. */
    @Test
    @EnabledIf("builtinsAvailable")
    void b6_singleRungIfElse_notALadder_rootCoversIt() throws IOException {
        String out = lookup(output(), "B6SingleRung.java");
        assertContains(out, "return MapperS.of(SubFmt.LEI);");
        assertContains(out, "return MapperS.of(SubFmt.OTHER);");
        assertNotContains(out, "BaseFmt.LEI");
        assertNotContains(out, "import census.seat11.BaseFmt;");
    }

    /** b5 — THE gate pin (the indep review's P3, RED-capable): a map-body lambda
     *  whose POSITION enum ({@code Unrelated} — the callee's input) differs from
     *  the enclosing function's OUTPUT enum ({@code SubFmt}) keeps its own
     *  qualification ({@code Unrelated.OTHER}); dropping the #215 same-instance
     *  gate would re-qualify it {@code SubFmt.OTHER} (wrong). */
    @Test
    @EnabledIf("builtinsAvailable")
    void b5_positionEnumDiffersFromOutputEnum_sameInstanceGateHolds() throws IOException {
        String out = lookup(output(), "B5Gate.java");
        // the probe lambda's arms keep the POSITION enum
        assertContains(out, "return MapperS.of(Unrelated.OTHER);");
        assertContains(out, "return MapperS.of(Unrelated.SOME);");
        assertNotContains(out, "return MapperS.of(SubFmt.OTHER);");
        // the function's OWN set-out else legitimately renders its output enum
        assertContains(out, "out = SubFmt.OTHER;");
    }

    /** b2 — the own-enum control (the indep review's NIT-1: NOT gate-discriminating —
     *  the rule's output IS Unrelated, so the hierarchy walk finds its own value;
     *  the gate-RED-capable pin is b5): a same-name value on an UNRELATED enum in
     *  a rule whose output is that enum keeps its own qualification. */
    @Test
    @EnabledIf("builtinsAvailable")
    void b2_unrelatedSameNameValue_ownEnum() throws IOException {
        String out = rule("B2UnrelatedRule.java");
        assertContains(out, "return MapperS.of(Unrelated.OTHER);");
        assertNotContains(out, "SubFmt.OTHER");
    }

    /** b3 — an all-own-value rule ladder renders byte-identically (the own-value
     *  match finds the same value first — the pre-seat firing seat). */
    @Test
    @EnabledIf("builtinsAvailable")
    void b3_allOwnValueLadder_unchanged() throws IOException {
        String out = rule("B3OwnRule.java");
        assertContains(out, "return MapperS.of(SubFmt.NATURAL_PERSON);");
        assertContains(out, "return MapperS.of(SubFmt.SWIFT);");
        assertContains(out, "return MapperS.of(SubFmt.OTHER);");
    }

    // =========================================================================
    // Part C — drr 7.0.0 corpus locks (RED pre-seat; WHOLE-FILE)
    // =========================================================================

    private static final Path DRR7_CELL_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path DRR7_GOLDEN_DIR =
            DRR7_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    static boolean drr7Available() {
        return Drr7Corpus.gate(builtinsAvailable()
                && Files.isDirectory(DRR7_CELL_ROOT.resolve("rosetta-source/src/main/rosetta"))
                && Files.isDirectory(DRR7_GOLDEN_DIR), LadderArmEnumExpectedOwnerSeatTest.class);
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

    /** The depth-1 3-line carrier (Lei on the super, output PartyIdentifierFormat2Enum). */
    @Test
    @EnabledIf("drr7Available")
    void corpus_brokerIdentifierFormat_hkma_wholeFile() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/hkma/rewrite/trade/reports/BrokerIdentifierFormatRule.java");
    }

    /** The depth-1 twin. */
    @Test
    @EnabledIf("drr7Available")
    void corpus_centralCounterpartyIdentifierFormat_hkma_wholeFile() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/hkma/rewrite/trade/reports/CentralCounterpartyIdentifierFormatRule.java");
    }

    /** The depth-2 8-line carrier (output PartyIdentifierFormatEnum: EVERY arm
     *  — the middle's own SWIFTBIC/OTHER AND the root's LEI — by the leaf). */
    @Test
    @EnabledIf("drr7Available")
    void corpus_otherPaymentPayerFormat_hkma_wholeFile() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/hkma/rewrite/trade/reports/OtherPaymentPayerFormatRule.java");
    }

    /** The depth-2 twin. */
    @Test
    @EnabledIf("drr7Available")
    void corpus_otherPaymentReceiverFormat_hkma_wholeFile() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/hkma/rewrite/trade/reports/OtherPaymentReceiverFormatRule.java");
    }

    /** The remaining depth-1 pair (Clearing member / Counterparty 2). */
    @Test
    @EnabledIf("drr7Available")
    void corpus_clearingMemberIdentifierFormat_hkma_wholeFile() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/hkma/rewrite/trade/reports/ClearingMemberIdentifierFormatRule.java");
    }

    @Test
    @EnabledIf("drr7Available")
    void corpus_counterparty2IdentifierFormat_hkma_wholeFile() throws IOException {
        assertByteMatchesGolden(
                "drr/regulation/hkma/rewrite/trade/reports/Counterparty2IdentifierFormatRule.java");
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
                + path + " (seat 11 — the ladder-arm enum expected-owner family).");
    }

    // =========================================================================
    // Harness
    // =========================================================================

    private static Map<String, String> outputMap;

    private static Map<String, String> output() throws IOException {
        if (outputMap == null) {
            RModel model = AstBuilder.buildFromString(MODEL, "seat11.rosetta");
            model.setVersion("0.0.0.test");
            List<RModel> models = new ArrayList<>();
            models.add(model);
            models.addAll(loadBuiltinsOnly());
            RLinkingResult linkingResult = RWorkspace.build(models);
            GeneratorModel gm = new GeneratorModel(linkingResult.workspace());
            JavaTypeUtil typeUtil = new JavaTypeUtil();
            JavaTypeTranslator tt = new JavaTypeTranslator(typeUtil);
            FunctionGenerator fg = new FunctionGenerator(gm, tt, typeUtil);
            RuleGenerator ruleGen = new RuleGenerator(gm, tt, fg);
            Map<String, String> out = new LinkedHashMap<>();
            List<String> errors = new ArrayList<>();
            ruleGen.generateClasses(model, "1.0", out)
                    .forEach(e -> errors.add(e.getTargetPath() + " — " + e));
            fg.generateWithErrors(out)
                    .forEach(e -> errors.add(e.getTargetPath() + " — " + e));
            reachabilityFence(linkingResult);
            if (!errors.isEmpty()) {
                throw new AssertionError("fixture generation errors (a broken fixture"
                        + " must fail loudly, not skip): " + errors);
            }
            outputMap = out;
        }
        return outputMap;
    }

    private static String rule(String fileName) throws IOException {
        return lookup(output(), fileName);
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
            throw new AssertionError("[LadderArmEnumExpectedOwnerSeatTest] builtins parse failures: "
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
