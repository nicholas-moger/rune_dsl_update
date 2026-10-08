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
 * SEAT 23, law A2 — facet {@code pathedChoiceSwitchSet}: <b>a SEGMENT-PATHED {@code set out -> path:} whose value
 * is a CHOICE/TYPE-keyed {@code switch} hoists the leaf-typed instanceof ASSIGNMENT ladder and consumes the local
 * through the setter chain</b> — upstream's {@code assignValue} for a path-tail SET (the switch compiled as its
 * {@code JavaIfThenElseBuilder} block into a {@code final <Leaf> ifThenElseResultN;} local, then
 * {@code collapseToSingleExpression} into the builder chain). Golden drr 7.x
 * {@code Ingest_FpmlRecordKeepingToReportableEvent}: {@code final WorkflowStep ifThenElseResult0; if (fpmlDocument
 * == null) { ifThenElseResult0 = null; } else if (fpmlDocument instanceof NonpublicExecutionReport) { final
 * NonpublicExecutionReport nonpublicExecutionReport0 = (NonpublicExecutionReport) fpmlDocument; ifThenElseResult0 =
 * mapNonpublicExecutionReportToWorkflowStep.evaluate(nonpublicExecutionReport0); } else { ifThenElseResult0 = null; }
 * reportableEvent\n\t.setOriginatingWorkflowStep(ifThenElseResult0);} — twice, the second switch's arm a fn CALL
 * whose arguments are a bare fn reference and bare narrowed navs, the cast locals numbered {@code 0/1}.
 *
 * <p><b>The defect.</b> The SET dispatch's #221 choice-switch ladder is gated {@code operation.segment().isEmpty()}
 * ("segment-pathed switches keep the ControlFlowHandler fallback") and that fallback is the type-keyed ternary
 * stub, which REFUSES ({@code TYPE_SWITCH_TERNARY_STUB}): 4 missing outputs, drr 7.0–7.3 (one refusal per cell
 * aborts the element, so the second switch was unseen until the first healed).
 *
 * <p><b>The seat — assembled from three existing laws, one new renderer.</b>
 * {@code FunctionExpressionRenderer.renderPathedChoiceSwitchSetOrNull}: the #367 ctor-field hoist's block
 * ({@code ControlFlowHandler.hoistCtorSwitchInstanceofLadderOrNull} — the green cdm {@code MapRateOptionWithLocation}
 * shape) at the statement seat; the #221 whole-output ladder's case machinery ({@code resolveCaseType},
 * {@code bindSwitchSubject}, {@code unwrapSwitchArmValue}, and {@code choiceCaseVarName} — which now COUNTS pathed
 * switches, upstream's one-scope numbering); the pathed conditional twin's leaf-typed local and setter consumer
 * (the leaf read extracted to ONE {@code pathedLeafDeclOrNull} shared by both pathed hoists — LAW 69;
 * {@code renderSetBuilderChain}); the sentinel late-attached to the {@code ifThenElseResult} group (the #397 law).
 * The IR route shares the seat (the seat-23 probe: 1,454 = 1,454 top-level switch operations on both routes).
 *
 * <p><b>LAW 75 — measured before the seat over all 275 matrix rows, BOTH routes:</b> of 1,454 top-level switch
 * operations the probe OBSERVED 4 segment-pathed choice switches — the carrier's FIRST switch × drr 7.0–7.3 (leaf
 * single, subject a bare input, case a bare fn reference). The probe is blind past a refusal (the
 * TYPE_SWITCH_TERNARY_STUB refusal THROWS and aborts the element at its first refusing operation), so the
 * carrier's SECOND pathed switch was unseen: the true pre-seat population is 8, two per cell. The population is
 * closed POST-seat, by the receipt that can see past refusals: missingOutput is 12 on both routes and every one
 * is a METAFIELD row, so no FUNCTION element is refused any more, every element's full operation list is rendered
 * and byte-compared, and the LAW-80 accounting reads 8 healed / 0 entered / 310 unchanged hunk-for-hunk. A
 * GOLDEN-side census of pathed instanceof ladders beyond drr 7.0.0 (goldens are refusal-immune) is BANKED as the
 * durable cross-cell close. Green-safe by construction besides: the pre-seat render was a refusal. Declines stated
 * as corpus-unwitnessed: a MULTI or META leaf, a non-identifier subject, a model-CHOICE subject (the #394
 * option-nav form), a bare-identifier arm.
 *
 * <p><b>RED at the pre-seat blob</b> ({@code rune-java-generator/src/main} at {@code 7fde647e} — this suite kept):
 * a1–a3, a5, {@code corpus_c1}, {@code corpus_control1} (the carrier is a refusal, named as not emitted — and under
 * {@code -Pir-on} also {@code a4}); b1–b4 + {@code control0} + {@code control2} (the route twin) GREEN in both
 * states. <b>LAW 66/76 mutations</b> (each applied → run → reverted at the FINAL head, LAW 78; every set below
 * MEASURED at the seat's receipts chain): (i) the renderer's dispatch deleted → a1–a3, a5 + c1 + control1 (the
 * refusal returns); (ii) the case-var count extension reverted →
 * a3 + c1 (both ladders name the bare cast local — non-compiling Java; the carrier's two switches number
 * {@code nonpublicExecutionReport0/1}); (iii) the leaf-typed local replaced by the case fn's OUTPUT type → a5
 * only (MEASURED: the carrier's two switches and a1/a2 all have leaf == fn output — {@code WorkflowStep},
 * {@code ReportableInformation} — so only the declared subtype fixture witnesses the read; isolating); (iv) the
 * MULTI-leaf decline dropped → b4 (the List-typed local form has no byte evidence).
 */
class PathedChoiceSwitchSetSeatTest {

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

    static boolean drr7AndIrProviderAvailable() {
        return drr7Available() && irProviderOnClasspath();
    }

    /**
     * An extends-based subject type with one subtype, an output type with single and multi leaves, the
     * {@code MapNonpublicExecutionReportToWorkflowStep} / {@code GetTradeHeaderForNonpublicExecutionReport} /
     * {@code MapTradeHeaderToReportableInformation} twins, and one function per shape.
     */
    private static final String MODEL = """
            namespace census.seat23a2
            version "1.0.0"

            type Doc:
                id string (0..1)

            type NpeReport extends Doc:
                withdrawal string (0..1)
                party string (0..1)

            type OtherReport extends Doc:
                note string (0..1)

            type Step:
                ref string (0..1)

            type Info:
                caption string (0..1)

            type InfoSub extends Info:
                extra string (0..1)

            type Event:
                step Step (0..1)
                info Info (0..1)
                steps Step (0..*)

            func MapNpeToStep: <"MapNonpublicExecutionReportToWorkflowStep twin">
                inputs:
                    r NpeReport (1..1)
                output:
                    step Step (1..1)
                set step:
                    Step { ref: r -> withdrawal }

            func HeaderOf: <"GetTradeHeaderForNonpublicExecutionReport twin">
                inputs:
                    r NpeReport (0..1)
                output:
                    h string (0..1)
                set h:
                    r -> party

            func MapInfo: <"MapTradeHeaderToReportableInformation twin">
                inputs:
                    h string (0..1)
                    w string (0..1)
                output:
                    info Info (0..1)
                set info:
                    Info { caption: h }

            func MapInfoSub: <"a5 - a fn whose output is a SUBTYPE of the leaf it is assigned into">
                inputs:
                    r NpeReport (1..1)
                output:
                    info InfoSub (0..1)
                set info:
                    InfoSub { caption: r -> party, ... }

            func A1BareFnArm: <"a1 - THE Ingest_FpmlRecordKeepingToReportableEvent SHAPE: a pathed SET whose value is a choice switch with a bare fn-ref arm">
                inputs:
                    doc Doc (0..1)
                output:
                    event Event (0..1)
                set event -> step:
                    doc switch
                        NpeReport then MapNpeToStep,
                        default empty

            func A2FnCallArm: <"a2 - the carrier's SECOND switch: a fn CALL arm whose args are a bare fn ref and a bare narrowed nav">
                inputs:
                    doc Doc (0..1)
                output:
                    event Event (0..1)
                set event -> info:
                    doc switch
                        NpeReport then MapInfo(HeaderOf, withdrawal),
                        default empty

            func A3TwoLaddersSameCase: <"a3 - two pathed switches on the SAME case type number their cast locals 0/1 and their locals 0/1 (golden nonpublicExecutionReport0/1)">
                inputs:
                    doc Doc (0..1)
                output:
                    event Event (0..1)
                set event -> step:
                    doc switch
                        NpeReport then MapNpeToStep,
                        default empty
                set event -> info:
                    doc switch
                        NpeReport then MapInfo(HeaderOf, withdrawal),
                        default empty

            func A5SubtypeFnOutput: <"a5 - the LEAF types the local, not the arm's fn output: InfoSub (extends Info) into event -> info declares final Info">
                inputs:
                    doc Doc (0..1)
                output:
                    event Event (0..1)
                set event -> info:
                    doc switch
                        NpeReport then MapInfoSub,
                        default empty

            func B1WholeOutputSwitch: <"b1 - the #221 whole-output choice switch keeps its bytes">
                inputs:
                    doc Doc (0..1)
                output:
                    step Step (0..1)
                set step:
                    doc switch
                        NpeReport then MapNpeToStep,
                        default empty

            func B2CtorFieldSwitch: <"b2 - the #367 ctor-field switch keeps its bytes">
                inputs:
                    doc Doc (0..1)
                output:
                    event Event (0..1)
                set event:
                    Event {
                        step: (doc switch
                            NpeReport then MapNpeToStep,
                            default empty),
                        ...
                    }

            func B3PathedConditional: <"b3 - a pathed CONDITIONAL keeps the conditional twin's bytes (the shared leaf read)">
                inputs:
                    doc Doc (0..1)
                output:
                    event Event (0..1)
                set event -> info:
                    if doc -> id exists
                    then Info { caption: doc -> id }
            """;

    /**
     * b4 — a MULTI leaf declines (the List-typed local has no byte evidence): the pathed switch still REFUSES
     * (today's bytes) — its own namespace, rendered with the errors collected rather than thrown.
     */
    private static final String MODEL_NEG = """
            namespace census.seat23a2neg
            version "1.0.0"

            import census.seat23a2.*

            func B4MultiLeafDeclines: <"b4 - a pathed switch into a MULTI leaf declines and still refuses">
                inputs:
                    doc Doc (0..1)
                output:
                    event Event (0..1)
                set event -> steps:
                    doc switch
                        NpeReport then MapNpeToStep,
                        default empty
            """;

    // =========================================================================
    // Part A — the seat (RED at the pre-seat blob)
    // =========================================================================

    /** a1 — the carrier's first switch: the bare fn-ref arm, the leaf-typed local, the setter consumer. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_bareFnRefArmHoistsLeafTypedLadder() throws IOException {
        String out = function("A1BareFnArm.java");
        assertContains(out, "final Step ifThenElseResult;\n"
                + "\t\t\tif (doc == null) {\n"
                + "\t\t\t\tifThenElseResult = null;\n"
                + "\t\t\t} else if (doc instanceof NpeReport) {\n"
                + "\t\t\t\tfinal NpeReport npeReport = (NpeReport) doc;\n"
                + "\t\t\t\tifThenElseResult = mapNpeToStep.evaluate(npeReport);\n"
                + "\t\t\t} else {\n"
                + "\t\t\t\tifThenElseResult = null;\n"
                + "\t\t\t}\n"
                + "\t\t\tevent\n"
                + "\t\t\t\t.setStep(ifThenElseResult);");
        assertNotContains(out, "Objects.equals(");
    }

    /** a2 — the carrier's second switch: a fn CALL arm whose args are a bare fn ref and a bare narrowed nav. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_fnCallArmWithBareFnRefAndNarrowedNavArgs() throws IOException {
        String out = function("A2FnCallArm.java");
        assertContains(out, "final Info ifThenElseResult;");
        assertContains(out, "\t\t\t\tfinal NpeReport npeReport = (NpeReport) doc;\n"
                + "\t\t\t\tifThenElseResult = mapInfo.evaluate(headerOf.evaluate(npeReport), MapperS.of(npeReport).<String>map(\"getWithdrawal\", ");
        assertContains(out, ".getWithdrawal()).get());");
        assertContains(out, "\t\t\tevent\n\t\t\t\t.setInfo(ifThenElseResult);");
        assertNotContains(out, "Objects.equals(");
    }

    /** a3 — two pathed switches on the SAME case type: cast locals 0/1 and ifThenElseResult locals 0/1. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a3_twoLaddersOnTheSameCaseTypeNumberTheirLocals() throws IOException {
        String out = function("A3TwoLaddersSameCase.java");
        assertContains(out, "final Step ifThenElseResult0;");
        assertContains(out, "final NpeReport npeReport0 = (NpeReport) doc;");
        assertContains(out, "ifThenElseResult0 = mapNpeToStep.evaluate(npeReport0);");
        assertContains(out, "final Info ifThenElseResult1;");
        assertContains(out, "final NpeReport npeReport1 = (NpeReport) doc;");
        assertContains(out, "ifThenElseResult1 = mapInfo.evaluate(headerOf.evaluate(npeReport1), MapperS.of(npeReport1).<String>map(\"getWithdrawal\", npeReport -> npeReport.getWithdrawal()).get());");
        assertContains(out, ".setStep(ifThenElseResult0);");
        assertContains(out, ".setInfo(ifThenElseResult1);");
    }

    /**
     * a4 — LAW 77: the a1 shape rendered through the REAL {@code IRGeneration.functionGenerator} seam
     * ({@code -Pir-on}); the SET dispatch is the shared legacy renderer, so the bytes agree.
     */
    @Test
    @EnabledIf("builtinsAndIrProviderAvailable")
    void a4_bareFnRefArmOnIrRoute() throws IOException {
        String out = lookup(fixtureOnIrRoute(), "functions/A1BareFnArm.java");
        assertContains(out, "final Step ifThenElseResult;");
        assertContains(out, "ifThenElseResult = mapNpeToStep.evaluate(npeReport);");
        assertContains(out, "\t\t\tevent\n\t\t\t\t.setStep(ifThenElseResult);");
    }

    /**
     * a5 — the LEAF types the local, not the arm's function output (upstream's expected type at a path-tail
     * assignment is the target attribute's, and the if-then-else local collapses at that type): a case fn returning
     * {@code InfoSub extends Info} into {@code event -> info} declares {@code final Info ifThenElseResult;}. A
     * DECLARED fixture pin — the corpus carries no pathed choice switch whose fn output differs from its leaf (the
     * drr 7.x carrier's two switches agree with theirs) — and the isolating witness of mutation (iii).
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a5_leafTypesTheLocalNotTheArmsFnOutput() throws IOException {
        String out = function("A5SubtypeFnOutput.java");
        assertContains(out, "final Info ifThenElseResult;");
        assertContains(out, "ifThenElseResult = mapInfoSub.evaluate(npeReport);");
        assertContains(out, "\t\t\tevent\n\t\t\t\t.setInfo(ifThenElseResult);");
        assertNotContains(out, "final InfoSub ifThenElseResult;");
    }

    // =========================================================================
    // Part B — placement pins (GREEN in BOTH states)
    // =========================================================================

    /** b1 — the #221 whole-output choice switch keeps its bytes: the direct toBuilder assignment, no hoisted local. */
    @Test
    @EnabledIf("builtinsAvailable")
    void b1_wholeOutputChoiceSwitchUnchanged() throws IOException {
        String out = function("B1WholeOutputSwitch.java");
        assertContains(out, "} else if (doc instanceof NpeReport) {\n"
                + "\t\t\t\tfinal NpeReport npeReport = (NpeReport) doc;\n"
                + "\t\t\t\tstep = toBuilder(mapNpeToStep.evaluate(npeReport));");
        assertNotContains(out, "ifThenElseResult");
    }

    /** b2 — the #367 ctor-field switch keeps its bytes: the hoisted local feeds the builder's setter. */
    @Test
    @EnabledIf("builtinsAvailable")
    void b2_ctorFieldSwitchUnchanged() throws IOException {
        String out = function("B2CtorFieldSwitch.java");
        assertContains(out, "final Step ifThenElseResult;");
        assertContains(out, "ifThenElseResult = mapNpeToStep.evaluate(npeReport);");
        assertContains(out, ".setStep(ifThenElseResult)");
        assertNotContains(out, "event\n\t\t\t\t.setStep(");
    }

    /**
     * b3 — a pathed CONDITIONAL keeps the conditional twin's bytes (the shared leaf read): an ELSELESS
     * conditional takes the twin's INITIALIZER form ({@code Info ifThenElseResult = null;} + the guarded
     * assignment + the setter consumer), the leaf-typed local declared from the SAME read the switch uses.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b3_pathedConditionalUnchanged() throws IOException {
        String out = function("B3PathedConditional.java");
        assertContains(out, "\t\t\tInfo ifThenElseResult = null;\n"
                + "\t\t\tif (exists(MapperS.of(doc).<String>map(\"getId\", _doc -> _doc.getId())).getOrDefault(false)) {\n"
                + "\t\t\t\tifThenElseResult = Info.builder()\n");
        assertContains(out, "\t\t\tevent\n\t\t\t\t.setInfo(ifThenElseResult);");
        assertNotContains(out, "instanceof");
        assertNotContains(out, "final Info ifThenElseResult;");
    }

    /** b4 — a MULTI leaf declines: the pathed switch still refuses (today's bytes). */
    @Test
    @EnabledIf("builtinsAvailable")
    void b4_multiLeafDeclinesAndStillRefuses() throws IOException {
        List<String> errors = negativeFixtureErrors();
        assertFalse(errors.isEmpty(), "B4MultiLeafDeclines must still refuse (the List-typed local has no byte evidence)");
        assertTrue(errors.stream().anyMatch(e -> e.contains("type-keyed switch case")),
                "the refusal must be the type-keyed switch stub: " + errors);
    }

    // =========================================================================
    // Part C — the corpus (drr 7.0.0: the carrier + the whole-cell control)
    // =========================================================================

    private static final Path DRR7_CELL_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path DRR7_GOLDEN = DRR7_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    static boolean drr7Available() {
        return Drr7Corpus.gate(Files.isDirectory(DRR7_GOLDEN), PathedChoiceSwitchSetSeatTest.class);
    }

    private static final String INGEST =
            "drr/ingest/fpml/recordkeeping/message/functions/Ingest_FpmlRecordKeepingToReportableEvent.java";

    /** c1 — drr 7.0.0 Ingest_FpmlRecordKeepingToReportableEvent whole-file lock (both switches). */
    @Test
    @EnabledIf("drr7Available")
    void corpus_c1_ingestFpmlRecordKeepingByteIdentical() throws IOException {
        lock(INGEST);
    }

    /**
     * control0 — golden is the oracle (the frozen drr 7.0.0 tree, every kind): the assignment ladder's token —
     * a {@code } else if (<subject> instanceof <Type>) {} line (the #221 / #367 / pathed forms; the alias RETURN
     * ladder's {@code if (…)} arms never match) — at its exact census: 15 files / 180 sites; the carrier carries 2.
     */
    @Test
    @EnabledIf("drr7Available")
    void corpus_control0_goldenDrr7IsTheOracle() throws IOException {
        Map<String, Integer> g = scanAssignmentLadderArms(readGoldenTree(DRR7_GOLDEN));
        assertEquals(15, g.size(), "golden drr 7.0.0 assignment-ladder files (the whole-cell control's domain)");
        assertEquals(180, g.values().stream().mapToInt(Integer::intValue).sum(), "golden drr 7.0.0 ladder arms");
        assertEquals(2, g.get(INGEST), "the carrier's arms (its two switches)");
    }

    /**
     * control1 — the FORK's WHOLE generated drr 7.0.0 cell (every kind, LAW 72): over the UNION of the files
     * either tree carries the token in (a missing side counts as zero, LAW 79), the per-file arm counts agree
     * FILE BY FILE except the named pre-existing residue; files golden carries that the fork does not emit at all
     * (the pre-seat refusal) are named, never silently skipped.
     */
    @Test
    @EnabledIf("drr7Available")
    void corpus_control1_forkDrr7WholeCellAssignmentLadderArmsEqualGoldenFileByFile() throws IOException {
        assertNotNull(drr7Output, "drr 7.0.0 generation did not run");
        // FAIL-CLOSED: the cell is error-free since this seat, so ANY generation error means a file
        // is missing from the scan for an unknown reason and the whole-cell contract cannot be asserted.
        assertEquals(List.of(), drr7GenErrors, "drr 7.0.0 reported a generation error — the scan is incomplete");
        Map<String, Integer> g = scanAssignmentLadderArms(readGoldenTree(DRR7_GOLDEN));
        assertUnionEqual(scanAssignmentLadderArms(drr7Output), g, drr7Output.keySet(), "fork", "golden",
                15 + forkOnlyResidueFiles(), KNOWN_RESIDUE_DRR7);
    }

    /**
     * The named pre-existing residue of other families in this cell (LAW 73: the set, not the count) —
     * <b>EMPTY since seat 31</b>. {@code UnderlierBasketIdentifier} (was {@code fork=2 golden=0}) was
     * close-census family F15(a): the fork rendered its CHOICE switch as an {@code instanceof} ladder
     * (two {@code } else if (… instanceof …) {} arms) where golden probes the per-option getters under a
     * {@code switchArgument} local and carries none — and seat-31 law 4a
     * ({@code choiceOptionNavLadderDeepHop}) healed it WHOLE in all four drr 7.x cells, so the fork's
     * two arms are gone and the union domain is golden's 15 alone ({@code forkOnlyResidueFiles()} = 0).
     * RE-MEASURED at the seat-31 chain head {@code f2a4d5c0}: transcribed from control1's own failing
     * print (LAW 81), {@code but was: <[]>}. Any entry that ENTERS this list is a regression.
     */
    private static final List<String> KNOWN_RESIDUE_DRR7 = List.of();

    /** The residue entries golden has no token in at all (they widen the union domain by one each). */
    private static int forkOnlyResidueFiles() {
        return (int) KNOWN_RESIDUE_DRR7.stream().filter(r -> r.endsWith(" golden=0")).count();
    }

    /**
     * control2 — LAW 77 route parity: the whole drr 7.0.0 cell generated through the REAL {@code IRGeneration}
     * seams ({@code -Pir-on}) carries the SAME per-file arm counts as the legacy-route render (the domain is the
     * legacy render's own).
     */
    @Test
    @EnabledIf("drr7AndIrProviderAvailable")
    void corpus_control2_irRouteDrr7AssignmentLadderArmsEqualLegacyRouteFileByFile() throws IOException {
        assertNotNull(drr7Output, "drr 7.0.0 legacy-route generation did not run");
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", DRR7_CELL_ROOT), new ArrayList<>());
        assertUnionEqual(scanAssignmentLadderArms(irOut), scanAssignmentLadderArms(drr7Output), irOut.keySet(),
                "ir", "legacy", scanAssignmentLadderArms(drr7Output).size(), List.of());
    }

    private static void assertUnionEqual(Map<String, Integer> a, Map<String, Integer> b,
            java.util.Set<String> emittedA, String aName, String bName, int expectedDomain,
            List<String> knownResidue) {
        List<String> mismatched = new ArrayList<>();
        List<String> notEmitted = new ArrayList<>();
        java.util.Set<String> universe = new java.util.TreeSet<>(a.keySet());
        universe.addAll(b.keySet());
        for (String key : universe) {
            if (!emittedA.contains(key)) {
                notEmitted.add(key);
                continue;
            }
            int ac = a.getOrDefault(key, 0);
            int bc = b.getOrDefault(key, 0);
            if (ac != bc) {
                mismatched.add(key + " " + aName + "=" + ac + " " + bName + "=" + bc);
            }
        }
        assertEquals(knownResidue, mismatched,
                "ladder arm counts differ beyond the named residue in " + mismatched.size() + " file(s)");
        assertEquals(List.of(), notEmitted,
                "ladder files " + bName + " carries that " + aName + " does not emit at all (a refusal)");
        assertEquals(expectedDomain, universe.size(),
                "the union domain must equal the oracle's ladder files (" + expectedDomain + ")");
    }

    // =========================================================================
    // The scan — the assignment ladder's arms per file over CODE only (a line walk)
    // =========================================================================

    private static Map<String, Integer> scanAssignmentLadderArms(Map<String, String> tree) {
        Map<String, Integer> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : tree.entrySet()) {
            if (!e.getValue().contains(" instanceof ")) {
                continue;
            }
            int n = 0;
            for (String raw : codeOnly(e.getValue()).split("\n")) {
                String s = raw.strip();
                if (s.startsWith("} else if (") && s.contains(" instanceof ") && s.endsWith(") {")) {
                    n++;
                }
            }
            if (n > 0) {
                out.put(e.getKey(), n);
            }
        }
        return out;
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
                int end = java.indexOf("*/", i + 2);
                i = end < 0 ? n : end + 2;
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

    /**
     * The cell through the REAL {@code IRGeneration} seams — the D11 ON ring's wiring (the flag set for
     * the render and restored after; the provider asserted present; the function seam asserted to hand
     * back the IR-route generator, so this can never silently be an OFF-route render).
     */
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

    private static void lock(String path) throws IOException {
        assertNotNull(drr7Output, "drr 7.0.0 generation did not run — corpus unavailable?");
        List<String> lockedErrors = drr7GenErrors.stream().filter(e -> e.contains(path)).toList();
        assertTrue(lockedErrors.isEmpty(),
                "the generator reported errors for the locked file " + path + ": " + lockedErrors);
        String generated = drr7Output.get(path);
        assertNotNull(generated, "not generated in drr 7.0.0 (a refusal): " + path + " — errors: " + drr7GenErrors.stream()
                .filter(e -> e.contains("type-keyed switch")).toList());
        Path goldenPath = DRR7_GOLDEN.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "golden missing: " + goldenPath);
        assertEquals(normalize(Files.readString(goldenPath)), normalize(generated),
                "generated drr 7.0.0 output must byte-match golden (newline-normalized) for "
                + path + " — seat 23 A2: the pathed choice-switch SET.");
    }

    // =========================================================================
    // Fixture harness
    // =========================================================================

    private static RLinkingResult linking;
    private static RModel mainModel;
    private static RModel negModel;
    private static Map<String, String> fixtureOut;
    private static List<String> fixtureErrors;

    private static void link() throws IOException {
        if (linking == null) {
            RModel main = AstBuilder.buildFromString(MODEL, "seat23a2.rosetta");
            main.setVersion("0.0.0.test");
            RModel neg = AstBuilder.buildFromString(MODEL_NEG, "seat23a2neg.rosetta");
            neg.setVersion("0.0.0.test");
            List<RModel> models = new ArrayList<>();
            models.add(main);
            models.add(neg);
            models.addAll(loadBuiltinsOnly());
            linking = RWorkspace.build(models);
            mainModel = main;
            negModel = neg;
        }
    }

    private static Map<String, String> render(Predicate<RModel> filter, RModel model, List<String> errors)
            throws IOException {
        link();
        GeneratorModel gm = new GeneratorModel(linking.workspace(), filter);
        JavaTypeUtil typeUtil = new JavaTypeUtil();
        JavaTypeTranslator tt = new JavaTypeTranslator(typeUtil);
        FunctionGenerator fg = new FunctionGenerator(gm, tt, typeUtil);
        ModelObjectGenerator pojoGen = new ModelObjectGenerator(gm, tt, typeUtil);
        Map<String, String> out = new LinkedHashMap<>();
        pojoGen.generateClasses(model, "1.0", out)
                .forEach(e -> errors.add(e.getTargetPath() + " — " + e));
        fg.generateWithErrors(out)
                .forEach(e -> errors.add(e.getTargetPath() + " — " + e));
        return out;
    }

    /**
     * The fixture's functions. A generation error is KEPT, not thrown: pre-seat the a-functions REFUSE
     * ({@code TYPE_SWITCH_TERNARY_STUB}) — that refusal IS the RED — so only the test that asks for a refused
     * file fails, naming the refusal; the b-tests' files render in both states.
     */
    private static Map<String, String> fixture() throws IOException {
        if (fixtureOut == null) {
            link();
            List<String> errors = new ArrayList<>();
            fixtureOut = render(m -> "census.seat23a2".equals(m.namespace()), mainModel, errors);
            fixtureErrors = errors;
        }
        return fixtureOut;
    }

    private static List<String> negativeFixtureErrors() throws IOException {
        link();
        List<String> errors = new ArrayList<>();
        render(m -> "census.seat23a2neg".equals(m.namespace()), negModel, errors);
        return errors;
    }

    private static Map<String, String> fixtureOutIr;

    /** The fixture's functions through the REAL {@code IRGeneration.functionGenerator} seam (the ON route). */
    private static Map<String, String> fixtureOnIrRoute() throws IOException {
        if (fixtureOutIr == null) {
            link();
            String previous = System.getProperty(IRGeneration.PROPERTY);
            System.setProperty(IRGeneration.PROPERTY, "true");
            try {
                assertNotNull(IRGeneration.providerOrNull(),
                        "the IR provider must be resolvable under -Pir-on, else this is not an ON-route render");
                GeneratorModel gm = new GeneratorModel(linking.workspace(),
                        m -> "census.seat23a2".equals(m.namespace()));
                JavaTypeUtil typeUtil = new JavaTypeUtil();
                JavaTypeTranslator tt = new JavaTypeTranslator(typeUtil);
                FunctionGenerator fg = IRGeneration.functionGenerator(gm, tt, typeUtil);
                assertTrue(!fg.getClass().equals(FunctionGenerator.class),
                        "the seam must hand back the IR-route FunctionGenerator, got " + fg.getClass());
                Map<String, String> out = new LinkedHashMap<>();
                List<String> errors = new ArrayList<>();
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

    private static String function(String fileName) throws IOException {
        Map<String, String> out = fixture();
        boolean present = out.keySet().stream().anyMatch(k -> k.endsWith("/functions/" + fileName));
        assertTrue(present, "'" + fileName + "' was not generated — the fixture's generation errors: "
                + fixtureErrors + "; keys=" + out.keySet());
        return lookup(out, "functions/" + fileName);
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
            throw new AssertionError("[PathedChoiceSwitchSetSeatTest] builtins parse"
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
