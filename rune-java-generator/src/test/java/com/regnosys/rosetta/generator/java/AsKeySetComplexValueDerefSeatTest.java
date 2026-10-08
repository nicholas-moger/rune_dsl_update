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
 * SEAT 32, law F.2 -- facet {@code asKeySetComplexValueWrapperDeref} (census family F26).
 * An {@code as-key} SET whose value is a COMPLEX expression (a nav chain, not a bare
 * identifier) and whose COMPILED ITEM TYPE is the leaf attribute's OWN reference wrapper
 * must hoist that WRAPPER into a local of its own and deref it into the target-path local.
 *
 * <p><b>GOLDEN &larr; FORK</b> (drr 5.61.0
 * {@code drr/enrichment/common/trade/functions/Create_CancelledWorkflowStepFromInstruction.java},
 * sig B081 -- 3 diff lines, ONE hunk, the file's only hunk):
 * <pre>
 * GOLDEN  final ReferenceWithMetaWorkflowStep referenceWithMetaWorkflowStep = MapperS.of(originatingWorkflowStep)
 *             .&lt;ReferenceWithMetaWorkflowStep&gt;map("getPreviousWorkflowStep", workflowStep -&gt; workflowStep.getPreviousWorkflowStep()).get();
 * GOLDEN  final WorkflowStep cancelledWorkflowStepPreviousWorkflowStep =
 *             referenceWithMetaWorkflowStep == null ? null : referenceWithMetaWorkflowStep.getValue();
 * FORK    final WorkflowStep cancelledWorkflowStepPreviousWorkflowStep = MapperS.of(originatingWorkflowStep)
 *             .&lt;ReferenceWithMetaWorkflowStep&gt;map(...).get();
 * </pre>
 * Source {@code test-corpus/drr/drr-5.61.0/rosetta-source/src/main/rosetta/enrichment-common-trade-func.rosetta:54-55}
 * -- {@code set cancelledWorkflowStep -> previousWorkflowStep: originatingWorkflowStep ->
 * previousWorkflowStep as-key} (attribute {@code WorkflowStep.previousWorkflowStep
 * WorkflowStep (0..1) [metadata reference]}, cdm {@code event-workflow-type.rosetta:46-47}).
 *
 * <p><b>THE SEAT.</b> {@code FunctionExpressionRenderer.renderAsKeySetOrNull}'s final
 * {@code else} -- the COMPLEX-value arm -- spliced the compiled chain into a local declared
 * with the leaf's BARE type and NO coercion. The deref this law needs already lives six
 * lines above it (facet {@code fnIoMetaAsKeyValueHoist}, PR #434 finding #21) behind an
 * {@code SourceVersion.isIdentifier(bare)} gate, i.e. the BARE-INPUT case only. LAW 69: the
 * two-consumer sibling is {@code ConstructionHandler.tryCtorAsKeyReference} (facet
 * {@code ctorAsKeyReference}, PR #390), which already renders exactly this (wrapper, value)
 * pair at the CONSTRUCTOR seat off the SAME discriminator (the compiled item IS the
 * attribute's own wrapper) and the SAME lower-cased-wrapper name base.
 *
 * <p><b>THE NAMER, and why the literal would be wrong.</b> The wrapper local rides
 * {@code JavaStatementScope.registerDeferredCoercionParam}, whose group law numbers a
 * same-desired-name group {@code 0..n-1} METHOD-wide and leaves a SINGLETON bare. Golden
 * proves both directions inside this one cell: {@code Create_ReportingSideFromReportableEvent}
 * (GREEN, the ctor seat) numbers its FOUR sibling hoists
 * {@code referenceWithMetaParty0..3}, while this carrier's lone hoist reads
 * {@code referenceWithMetaWorkflowStep} bare. {@code corpus_c3} byte-locks the numbered half.
 *
 * <p><b>GREEN BLAST RADIUS -- MEASURED, not argued.</b> A plain walk over every
 * {@code .rosetta} in all 29 corpus cells classifies the COMPLETE {@code as-key} population:
 * <b>85</b> SET-seat IDENTIFIER values (the two branches above this arm), <b>14</b> SET-seat
 * NO-REAL-ELSE CONDITIONALs (the {@code RConditionalExpr} branch -- the cdm5
 * {@code transfer -> settlementOrigin -> assetPayout} complex value among them, which never
 * reaches this arm), <b>78</b> CONSTRUCTOR values (ConstructionHandler's own seat), and
 * <b>EXACTLY ONE</b> SET-seat COMPLEX value: this carrier. The repo's only other SET-seat
 * COMPLEX {@code as-key} is the hold-out fixture {@code holdout/func-bulk-as-key}
 * ({@code set out -> attrSingle: withMeta only-element as-key}), whose compiled item is the
 * BARE model type {@code test.bulkaskey.WithMeta} -- not an {@code RJavaWithMetaValue} -- so
 * it declines on the {@code instanceof} half and its 12/12 byte-identical oracle group is
 * untouched. On top of that the form this arm replaces NEVER COMPILED (a
 * {@code MapperS<ReferenceWithMetaX>.get()} assigned to an {@code X}-typed local), so no
 * green byte is reachable at all.
 *
 * <p><b>CARRIER / WHOLE CEILING.</b> One file, one cell: drr 5.61.0
 * {@code Create_CancelledWorkflowStepFromInstruction} goes WHOLE (its only sig is B081).
 * drr 6.x/7.x do not carry the function; cdm carries no SET-seat complex as-key.
 *
 * <p><b>LAW 77 -- route.</b> {@code renderAsKeySetOrNull} is private to
 * {@code FunctionExpressionRenderer} and reached from {@code renderOperationInner}, which is
 * also private; {@code IRFunctionExpressionRenderer extends FunctionExpressionRenderer} and
 * overrides only {@code ifThenElseResultBaseName}, {@code thenArgBaseName},
 * {@code booleanHoistBaseName} and {@code renderSetAssignmentStatement} -- none of them this
 * seat. {@code grep -rn "AsKey\|asKey" rune-ir-java/src/main/java} returns NOTHING. The fix
 * therefore INHERITS on the IR route. It is NOT free of route risk, however: the law reads
 * {@code compiled.getExpressionType()} and {@code IRExpressionCompiler extends
 * ExpressionCompiler}, so the IR route could type the chain differently -- which is exactly
 * what {@code corpus_control2} measures (BOTH-ROUTES-vs-GOLDEN on the carrier).
 *
 * <p><b>LAW 74 -- compile.</b> The fork's CURRENT text at the carrier is NON-COMPILING:
 * {@code final WorkflowStep x = MapperS.of(...).<ReferenceWithMetaWorkflowStep>map(...).get();}
 * assigns a {@code ReferenceWithMetaWorkflowStep} to a {@code WorkflowStep} -- incompatible
 * types, and {@code ReferenceWithMetaWorkflowStep} does not implement {@code WorkflowStep}
 * (it is the generated wrapper interface). LAW 74 MEASURED ({@code javac32-report.md} section
 * 7.3 row C23): PRE 1 error -> POST 0, the file leaving the PRE set at the head.
 *
 * <p><b>RED (CLAIMED -- measured by the chain) at the seat's base head:</b> default profile
 * <b>10/3F/1skip</b> = {@code a1}, {@code corpus_c1}, {@code corpus_control1} (control2 skips
 * without the IR provider); {@code -Pir-on} <b>10/4F/0skip</b> = the same three plus
 * {@code corpus_control2}. {@code e1}, {@code e2}, {@code e3}, {@code corpus_c2},
 * {@code corpus_c3} and {@code corpus_control3} are GREEN at RED -- they are decline and
 * no-regression locks, and a decline lock that fails at RED is a broken fixture, not a find.
 * <b>GREEN (CLAIMED):</b> <b>10/0F/1skip</b> default, <b>10/0F/0skip</b> under
 * {@code -Pir-on}.
 *
 * <p><b>MUTATIONS (LAW 66/76) -- MEASURED (LAW 82) by the seat-32 chain, run 1 at
 * {@code d99ded920} ({@code f32-mut-m-lawF2-*.log}):</b>
 * <ul>
 *   <li><b>m-lawF2-hoist</b> -- the WRAPPER-typed hoist severed (the first local declared with
 *       {@code valueType}, the deref kept): MEASURED <b>10/3F/1S</b> = {@code a1},
 *       {@code corpus_c1}, {@code corpus_control1} -- the claim exactly on the default route
 *       ({@code corpus_control2} is the {@code -Pir-on} member); the whole-cell control moves
 *       (LAW 76); {@code e1}..{@code e3}, {@code corpus_c2}, {@code corpus_c3},
 *       {@code corpus_control3} GREEN.</li>
 *   <li><b>m-lawF2-wrapperkey</b> -- the SAME-WRAPPER equality conjunct severed, the
 *       {@code instanceof} half kept: MEASURED <b>10/0F/1S -- EMPTY</b>; {@code e3} did NOT
 *       fail. The adjudication {@code e3}'s own javadoc wrote for this outcome is the record:
 *       the fixture's nav renders a {@code "Type coercion"} hop and hands the arm a BARE
 *       compiled item, so {@code e3} collapses onto {@code e2}'s mechanism and never reaches the
 *       equality; the conjunct ships as sibling-consistent defence-in-depth
 *       ({@code ConstructionHandler.tryCtorAsKeyReference} carries the identical compare) --
 *       the seat-30 {@code m-law8-typed} precedent -- UNWITNESSED at this corpus, where the
 *       measured population says no file can move. A foreign-wrapper fixture witness is
 *       BANKED; the conjunct and the assert are not weakened.</li>
 * </ul>
 * RED at the chain's base {@code ddcdd151b}: {@code a1}, {@code corpus_c1},
 * {@code corpus_control1} (+ {@code corpus_control2} on {@code -Pir-on}); GREEN at the head
 * 10/0F/1skip default, 10/0F/0skip {@code -Pir-on}.
 *
 * <p><b>LAW 81 -- tripwires this heal FIRES in OTHER suites (do not edit them here; the lead
 * re-pins from their own prints).</b> The carrier is named in exactly two committed residue
 * lists, both with the same {@code (T1,T2,T3)} scan whose T3 counts
 * {@code == null ? null :} occurrences file-wide:
 * {@code DispatchBaseInputDeclineSeatTest.KNOWN_RESIDUE_561} and
 * {@code FilterPredicateMetaDerefSeatTest.KNOWN_RESIDUE_561}, each carrying
 * {@code "drr/enrichment/common/trade/functions/Create_CancelledWorkflowStepFromInstruction.java
 * fork=[0, 0, 0] golden=[0, 0, 1]"}. The heal adds the file's one guarded deref, so the row
 * EQUALS golden and LEAVES both lists; {@code DOMAIN_DRR561 = 1174} does NOT move in either
 * (golden already put the file in the token-bearing union). Both suites go green after the
 * one-row removal.
 */
class AsKeySetComplexValueDerefSeatTest {

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

    /** THE CARRIER -- the only file this law moves. */
    private static final String CARRIER =
            "drr/enrichment/common/trade/functions/Create_CancelledWorkflowStepFromInstruction.java";

    /**
     * The GREEN namer sibling, same cell: FOUR ctor-seat as-key pairs whose wrapper locals
     * number {@code referenceWithMetaParty0..3}. Byte-identical today; must stay so.
     */
    private static final String NAMER_SIBLING =
            "drr/enrichment/common/trade/functions/Create_ReportingSideFromReportableEvent.java";

    /** The GREEN identifier-branch as-key SET carrier (the PR #328 golden anchor). */
    private static final String IDENT_GREEN =
            "cdm/event/workflow/functions/Create_AcceptedWorkflowStep.java";

    /** Cell A = drr 5.61.0 -- the carrier cell (the ONLY cell carrying the function). */
    private static final Path CELL_A_ROOT = Path.of("../test-corpus/drr/drr-5.61.0");
    private static final Path GOLDEN_A = CELL_A_ROOT.resolve("rosetta-source/src/generated/java");

    /**
     * Cell B = cdm 5.38.0 -- the OVER-FIRE cell, chosen BY NAME: the as-key SET seat's
     * IDENTIFIER branches live almost entirely in cdm ({@code event-common-func.rosetta} +
     * {@code event-workflow-func.rosetta} carry every one of cdm5's, and cdm5 emits TEN
     * as-key golden files against drr 5.61.0's TWO). drr 5.61.0 contains exactly ONE
     * SET-seat as-key of any shape -- the carrier -- so a control on the carrier cell alone
     * would leave the branch this law sits next to completely unscanned.
     */
    private static final Path CELL_B_ROOT = Path.of("../test-corpus/cdm/cdm-5.38.0");
    private static final Path GOLDEN_B = CELL_B_ROOT.resolve("rosetta-source/src/generated/java");

    static boolean cellAAvailable() {
        return Files.isDirectory(GOLDEN_A);
    }

    static boolean cellBAvailable() {
        return Files.isDirectory(GOLDEN_B);
    }

    static boolean cellAAndIrProviderAvailable() {
        return cellAAvailable() && irProviderOnClasspath();
    }

    // =========================================================================
    // The fixture -- a FAITHFUL reduction of the carrier's real source
    // =========================================================================

    /**
     * Reduced from {@code enrichment-common-trade-func.rosetta:33-55} (the carrier function)
     * plus its attribute declaration in cdm {@code event-workflow-type.rosetta:37-47}:
     * a {@code [metadata key]} type with a SELF-referencing {@code (0..1) [metadata reference]}
     * attribute, and a function whose single {@code (1..1)} input of that type feeds a pathed
     * {@code as-key} SET on the {@code (1..1)} output.
     *
     * <ul>
     *   <li>{@code A1AsKeySetComplexWrapperValue} -- THE CARRIER shape: the as-key value is a
     *       one-hop NAV into the leaf's own {@code [metadata reference]} attribute, so the
     *       compiled item IS {@code ReferenceWithMetaStep}.</li>
     *   <li>{@code E1AsKeySetBareIdentifierValue} -- the {@code #328} IDENTIFIER branch (cdm
     *       {@code Create_AcceptedWorkflowStep}'s shape); consumes inline, no hoist at all.</li>
     *   <li>{@code E2AsKeySetComplexBareValue} -- a COMPLEX value whose compiled item is the
     *       BARE type; the {@code instanceof} half declines.</li>
     *   <li>{@code E3AsKeySetComplexForeignWrapperValue} -- a COMPLEX value whose compiled
     *       item is a DIFFERENT wrapper ({@code FieldWithMetaStep}, from a {@code [metadata id]}
     *       attribute -- the cdm {@code ExecutionInstruction.tradeTime TimeZone (0..1)
     *       [metadata id]} shape); the EQUALITY half declines.</li>
     * </ul>
     */
    private static final String MODEL = """
            namespace census.seat32lawf2
            version "1.0.0"

            type Step:
                [metadata key]

                previousStep Step (0..1)
                    [metadata reference]

            type Box:
                inner Step (0..1)
                tagged Step (0..1)
                    [metadata id]

            func A1AsKeySetComplexWrapperValue: <"a1 - THE CARRIER: an as-key SET whose COMPLEX value compiles to the SET leaf own reference wrapper">
                inputs:
                    originating Step (1..1)
                output:
                    cancelled Step (1..1)
                set cancelled -> previousStep:
                    originating -> previousStep as-key

            func E1AsKeySetBareIdentifierValue: <"e1 - DECLINE: the #328 identifier branch consumes inline">
                inputs:
                    key Step (1..1)
                output:
                    holder Step (1..1)
                set holder -> previousStep:
                    key as-key

            func E2AsKeySetComplexBareValue: <"e2 - DECLINE: a COMPLEX value whose compiled item is the BARE type">
                inputs:
                    b Box (1..1)
                output:
                    holder Step (1..1)
                set holder -> previousStep:
                    b -> inner as-key

            func E3AsKeySetComplexForeignWrapperValue: <"e3 - DECLINE: a COMPLEX value whose compiled item is a DIFFERENT wrapper">
                inputs:
                    b Box (1..1)
                output:
                    holder Step (1..1)
                set holder -> previousStep:
                    b -> tagged as-key
            """;

    // =========================================================================
    // Part A -- the failing-first pin (RED pre-law)
    // =========================================================================

    /**
     * a1 -- THE LAW. The wrapper gets a local of its own and the path local derefs it.
     * Fails pre-law: the fork declared the path local with the BARE type and spliced the
     * wrapper-typed chain straight into it.
     *
     * <p>PIN AT RED: the wrapper local's name is the GROUP namer's output for a SINGLETON
     * group, i.e. bare {@code referenceWithMetaStep} (no seed in this function's scope
     * collides with it and the function renders no {@code "Type coercion"} param, so the
     * group has exactly one member -- the same arithmetic that makes golden's carrier read
     * {@code referenceWithMetaWorkflowStep}). If the measured name is escaped or numbered,
     * transcribe THE MEASURED NAME and say why -- do not weaken the assert to a substring.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_complexWrapperValueHoistsTheWrapperThenDerefsIntoThePathLocal() throws IOException {
        String out = fn("A1AsKeySetComplexWrapperValue.java");
        assertContains(out, "final ReferenceWithMetaStep referenceWithMetaStep = ");
        assertContains(out, "final Step cancelledPreviousStep = referenceWithMetaStep == null"
                + " ? null : referenceWithMetaStep.getValue();");
        assertTrue(!out.contains("final Step cancelledPreviousStep = MapperS.of("),
                "the wrapper must NOT be spliced into the bare-typed path local:\n" + out);
    }

    // =========================================================================
    // Part B -- the decline locks (each witness-unique on a token the flip moves)
    // =========================================================================

    /**
     * e1 -- the IDENTIFIER branch is untouched. A bare identifier value consumes INLINE:
     * no wrapper hoist, no path local at all, just the reference-copy builder reading the
     * identifier. This is the #328/#434 green form and 85 of the corpus' 100 SET-seat as-key
     * values take it, so it is the single most important byte-neutrality lock in the suite.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void e1_bareIdentifierValueStillConsumesInline() throws IOException {
        String out = fn("E1AsKeySetBareIdentifierValue.java");
        assertContains(out, ".setGlobalReference(Optional.ofNullable(key)");
        assertTrue(!out.contains("final ReferenceWithMetaStep "),
                "the identifier branch must not hoist a wrapper local:\n" + out);
        assertTrue(!out.contains("final Step holderPreviousStep = "),
                "the identifier branch must not declare a path local:\n" + out);
    }

    /**
     * e2 -- the {@code instanceof} half. A COMPLEX value whose compiled item is the BARE
     * leaf type keeps today's single splice. WITNESS-UNIQUE on
     * {@code final Step holderPreviousStep = MapperS.of(} -- the exact token a1 asserts
     * ABSENT at the carrier.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void e2_complexBareValueKeepsTodaysSingleSplice() throws IOException {
        String out = fn("E2AsKeySetComplexBareValue.java");
        assertContains(out, "final Step holderPreviousStep = MapperS.of(");
        assertTrue(!out.contains("final ReferenceWithMetaStep "),
                "a bare-item value must not hoist a wrapper local:\n" + out);
    }

    /**
     * e3 -- the SAME-WRAPPER EQUALITY half, and the {@code m-lawF2-wrapperkey} witness. The
     * value navigates to a {@code [metadata id]} attribute, so its compiled item is
     * {@code FieldWithMetaStep} while the leaf attribute's own wrapper is
     * {@code ReferenceWithMetaStep}: the law must LEAVE IT ALONE.
     *
     * <p>Asserted mechanism-AGNOSTICALLY on purpose. If the nav renders a {@code "Type
     * coercion"} hop and hands this arm a BARE compiled item, e3 collapses onto e2's
     * mechanism and {@code m-lawF2-wrapperkey} measures EMPTY. That outcome is an
     * ADJUDICATION of the conjunct (sibling-consistent defence-in-depth --
     * {@code ConstructionHandler.tryCtorAsKeyReference} carries the identical compare), not a
     * reason to weaken the conjunct or this assert. RESHAPE the fixture if a real
     * foreign-wrapper witness is wanted; record the re-scoring in the class javadoc.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void e3_complexForeignWrapperValueKeepsTodaysSingleSplice() throws IOException {
        String out = fn("E3AsKeySetComplexForeignWrapperValue.java");
        assertTrue(!out.contains("final FieldWithMetaStep "),
                "a FOREIGN wrapper must never be hoisted by this law:\n" + out);
        assertContains(out, "final Step holderPreviousStep = ");
    }

    // =========================================================================
    // Part C -- the whole-file byte compares
    // =========================================================================

    /** corpus_c1 -- THE HEAL: the carrier goes WHOLE in drr 5.61.0. */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_drr5610CancelledWorkflowStepMatchesGolden() throws IOException {
        assertNotNull(drrAOutput, "drr 5.61.0 generation did not run");
        List<String> own = drrAGenErrors.stream().filter(e -> e.contains(CARRIER)).toList();
        assertTrue(own.isEmpty(), "generation errors for " + CARRIER + ": " + own);
        String gen = drrAOutput.get(CARRIER);
        assertNotNull(gen, "not generated: " + CARRIER);
        assertEquals(normalize(Files.readString(GOLDEN_A.resolve(CARRIER))), normalize(gen),
                "Create_CancelledWorkflowStepFromInstruction must byte-match golden - seat 32"
                        + " law F.2: the complex as-key value's own wrapper is hoisted and"
                        + " dereffed into the path local");
    }

    /**
     * corpus_c2 -- the GREEN identifier-branch as-key SET carrier (cdm 5.38.0
     * {@code Create_AcceptedWorkflowStep}, the PR #328 javadoc's own golden anchor). Already
     * byte-identical; a byte-lock, not a heal. If the law reaches the identifier branch this
     * fails first and by name.
     */
    @Test
    @EnabledIf("cellBAvailable")
    void corpus_c2_cdm5380AcceptedWorkflowStepStaysByteIdentical() throws IOException {
        assertNotNull(cdmBOutput, "cdm 5.38.0 generation did not run");
        String gen = cdmBOutput.get(IDENT_GREEN);
        assertNotNull(gen, "not generated: " + IDENT_GREEN);
        assertEquals(normalize(Files.readString(GOLDEN_B.resolve(IDENT_GREEN))), normalize(gen),
                "the GREEN as-key identifier branch must stay byte-identical: " + IDENT_GREEN);
    }

    /**
     * corpus_c3 -- the GREEN NAMER sibling in the CARRIER's own cell
     * ({@code Create_ReportingSideFromReportableEvent}, four ctor-seat as-key pairs whose
     * wrapper locals number {@code referenceWithMetaParty0..3}). Two things ride on it: the
     * ctor seat this law CONSULTS but must not touch, and the numbered half of the group
     * namer whose singleton half a1 asserts.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c3_drr5610ReportingSideStaysByteIdentical() throws IOException {
        assertNotNull(drrAOutput, "drr 5.61.0 generation did not run");
        String gen = drrAOutput.get(NAMER_SIBLING);
        assertNotNull(gen, "not generated: " + NAMER_SIBLING);
        assertEquals(normalize(Files.readString(GOLDEN_A.resolve(NAMER_SIBLING))), normalize(gen),
                "the ctor-seat as-key sibling (and the numbered namer group) must stay"
                        + " byte-identical: " + NAMER_SIBLING);
    }

    // =========================================================================
    // Part D -- the controls (LAW 79 UNION whole-cell + LAW 77 route)
    // =========================================================================

    /**
     * control1 -- LAW 79, the UNION whole-cell scan on drr 5.61.0 (the carrier's cell). Domain
     * = every file whose GOLDEN <b>or</b> FORK text carries one of the three tokens below,
     * intersected with what the fork actually emits. Per file the triple must equal golden's
     * beyond the NAMED residue, so the control fails when the law OVER-fires (a green file
     * gains a hoist/deref pair, or loses a legitimate bare-typed chain local) AND when it
     * UNDER-fires (the carrier's row stays).
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control1_forkDrr561WholeCellAsKeyDerefFormsEqualGoldenFileByFile()
            throws IOException {
        assertNotNull(drrAOutput, "drr 5.61.0 generation did not run");
        assertEquals(List.of(), drrAGenErrors,
                "drr 5.61.0 reported a generation error - the scan is incomplete");
        assertUnionEqual(scan(drrAOutput), scan(readGoldenTree(GOLDEN_A)), drrAOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_DRR561, GOLDEN_DOMAIN_DRR561);
    }

    /**
     * control2 -- LAW 77, BOTH-ROUTES-vs-GOLDEN. The seat is inherited (rune-ir-java names no
     * as-key seat at all), but the law reads the COMPILED expression type and
     * {@code IRExpressionCompiler} re-implements parts of the compile, so route-safety is
     * MEASURED here rather than argued. Skips without the IR provider on the classpath.
     */
    @Test
    @EnabledIf("cellAAndIrProviderAvailable")
    void corpus_control2_irRouteMatchesGoldenForTheCarrier() throws IOException {
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "5.61.0", CELL_A_ROOT),
                new ArrayList<>());
        assertEquals(normalize(Files.readString(GOLDEN_A.resolve(CARRIER))),
                normalize(irOut.get(CARRIER)), "IR route vs GOLDEN: " + CARRIER);
        assertEquals(normalize(Files.readString(GOLDEN_A.resolve(NAMER_SIBLING))),
                normalize(irOut.get(NAMER_SIBLING)), "IR route vs GOLDEN: " + NAMER_SIBLING);
    }

    /**
     * control3 -- LAW 79 in the OVER-FIRE cell (cdm 5.38.0), the same UNION triple. This is
     * where the as-key SET seat's identifier branches and the ctor as-key seat's hoists both
     * live in bulk; a control on the carrier cell alone would claim a domain it never scanned.
     */
    @Test
    @EnabledIf("cellBAvailable")
    void corpus_control3_forkCdm538WholeCellAsKeyDerefFormsEqualGoldenFileByFile()
            throws IOException {
        assertNotNull(cdmBOutput, "cdm 5.38.0 generation did not run");
        assertEquals(List.of(), cdmBGenErrors,
                "cdm 5.38.0 reported a generation error - the scan is incomplete");
        assertUnionEqual(scan(cdmBOutput), scan(readGoldenTree(GOLDEN_B)), cdmBOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_CDM538, GOLDEN_DOMAIN_CDM538);
    }

    // =========================================================================
    // The pins
    // =========================================================================

    /**
     * DERIVED (not yet chain-measured) at head {@code ddcdd151b} by a read-only walk of the
     * GOLDEN tree -- the fork side can only differ at files the law moves, and after the law
     * the carrier is byte-identical to golden, so the union equals the golden token-bearing
     * set:
     * <pre>
     * python: os.walk("test-corpus/drr/drr-5.61.0/rosetta-source/src/generated/java")
     *         -> 5,249 .java goldens, 248 carry a non-zero (T1,T2,T3) triple,
     *            all 248 FUNCTION-kind and all under drr/ (the cell's rosetta-config.yml
     *            declares generators.namespaces = [drr.*, com.rosetta.model]).
     *            Golden-side sums: T1 349, T2 5, T3 0.
     * </pre>
     * FALSIFIER: this suite's own domain assert on the next chain (its message prints the
     * measured size). Re-pin from THAT print, not from this javadoc.
     */
    private static final int GOLDEN_DOMAIN_DRR561 = 248;

    /**
     * DERIVED the same way over cdm 5.38.0: 3,950 .java goldens, 21 token-bearing, all
     * FUNCTION-kind and all under cdm/. Golden-side sums: T1 35, T2 2, T3 0.
     */
    private static final int GOLDEN_DOMAIN_CDM538 = 21;

    /**
     * DERIVED at head {@code ddcdd151b} from the seat-31 final OFF-route dump: the drr 5.61.0
     * band is 13 files and exactly TWO of them differ on this triple for reasons that are NOT
     * this law's -- both are the F13 / B080-B083 meta-deref family (golden hoists a
     * {@code ReferenceWithMetaPriceSchedule} the fork inlines), owned by seat 32's laws F.5 /
     * F.6a. The CARRIER's own row ({@code fork=[0, 0, 1] golden=[1, 1, 0]}) is present at RED
     * and must be GONE at the law's head -- that is the control's failing-first half.
     * LAW 81: if either row below heals or a new row appears, re-pin from the control's own
     * print in the same commit and say which law reached the file.
     */
    // LAW 81 re-pin (v3.1 flip seat 33, law F.A): the cftc NotionalCurrencyLeg1Rule + jfsa
    // NotionalCurrencyOfLeg1Rule rows LEFT this list - both files healed WHOLE by facet
    // blockArmWrapperHopDeref (byte-identical to golden, locked by BlockArmWrapperHopDerefSeatTest
    // corpus_c1/c2); the list is EMPTY, transcribed from this control's own print (FA-trip1.log).
    private static final List<String> KNOWN_RESIDUE_DRR561 = List.of();

    /**
     * DERIVED EMPTY: cdm 5.38.0 FUNCTION is at TRUE 100% byte parity (the single PERMANENT
     * D11 waiver is a cdm6 METAFIELD), so fork and golden agree on every file in the domain.
     * A NON-EMPTY result here is the ENTERING signal for the over-fire cell.
     */
    private static final List<String> KNOWN_RESIDUE_CDM538 = List.of();

    /**
     * (T1, T2, T3) per file -- the three forms this law trades between:
     * <ul>
     *   <li><b>T1</b> -- the law's ADDED first line: a {@code final} local whose DECLARED type
     *       names a meta wrapper, initialised from a mapper chain ({@code ... .get();}).</li>
     *   <li><b>T2</b> -- the law's ADDED second line: a {@code final} local initialised by the
     *       guarded value deref ({@code ... == null ? null : ... .getValue();}).</li>
     *   <li><b>T3</b> -- the DEFECT SIGNATURE the law removes, and the over-fire net in the
     *       other direction: a {@code final} local whose DECLARED type is NOT a wrapper, whose
     *       initialiser names a wrapper as a map witness and ends {@code .get();}. Golden's
     *       T3 is ZERO across BOTH cells (measured), so any non-zero fork T3 is a wrapper
     *       spliced into a bare-typed local -- exactly this defect.</li>
     * </ul>
     * The carrier moves {@code [0,0,1] -> [1,1,0]}; every other file in both cells must equal
     * golden.
     */
    private static Map<String, int[]> scan(Map<String, String> tree) {
        Map<String, int[]> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : tree.entrySet()) {
            int[] t = new int[3];
            for (String raw : normalize(e.getValue()).split("\n")) {
                String s = raw.trim();
                if (!s.startsWith("final ")) {
                    continue;
                }
                int eq = s.indexOf(" = ");
                if (eq < "final ".length()) {
                    continue;
                }
                String lhs = s.substring("final ".length(), eq);
                int sp = lhs.lastIndexOf(' ');
                if (sp < 0) {
                    continue;
                }
                String decl = lhs.substring(0, sp);
                boolean chainGet = s.endsWith(".get();");
                boolean declIsWrapper = decl.contains("WithMeta");
                boolean namesWrapper = s.contains(".<ReferenceWithMeta")
                        || s.contains(".<FieldWithMeta")
                        || s.contains(".<BasicReferenceWithMeta");
                if (chainGet && declIsWrapper) {
                    t[0]++;
                }
                if (s.contains(" == null ? null : ") && s.endsWith(".getValue();")) {
                    t[1]++;
                }
                if (chainGet && !declIsWrapper && namesWrapper) {
                    t[2]++;
                }
            }
            if (t[0] + t[1] + t[2] > 0) {
                out.put(e.getKey(), t);
            }
        }
        return out;
    }

    // =========================================================================
    // The union assert (LAW 73: pin the SET, not the count)
    // =========================================================================

    private static void assertUnionEqual(Map<String, int[]> a, Map<String, int[]> b,
            java.util.Set<String> emittedA, String aName, String bName, List<String> knownResidue,
            int expectedDomain) {
        List<String> mismatched = new ArrayList<>();
        java.util.Set<String> universe = new java.util.TreeSet<>(a.keySet());
        universe.addAll(b.keySet());
        universe.retainAll(emittedA);
        int[] zero = new int[3];
        for (String key : universe) {
            int[] ac = a.getOrDefault(key, zero);
            int[] bc = b.getOrDefault(key, zero);
            if (!java.util.Arrays.equals(ac, bc)) {
                mismatched.add(key + " " + aName + "=" + java.util.Arrays.toString(ac)
                        + " " + bName + "=" + java.util.Arrays.toString(bc));
            }
        }
        // The domain-pin flow (LAW 73): the sentinel failure PRINTS the measured values.
        assertTrue(expectedDomain >= 0,
                "the union domain is MEASURED and pinned (LAW 73) - transcribe from this"
                        + " print: MEASURED domain=" + universe.size()
                        + " residue=" + mismatched);
        assertEquals(knownResidue, mismatched,
                "the triple differs beyond the named residue in " + mismatched.size()
                        + " file(s)");
        assertEquals(expectedDomain, universe.size(),
                "the union domain must equal the emitted token-bearing files - MEASURED"
                        + " domain=" + universe.size());
    }

    // =========================================================================
    // Fixture harness
    // =========================================================================

    private static Map<String, String> fixtureOut;

    private static Map<String, String> fixture() throws IOException {
        if (fixtureOut == null) {
            RModel main = AstBuilder.buildFromString(MODEL, "seat32lawf2.rosetta");
            main.setVersion("0.0.0.test");
            List<RModel> models = new ArrayList<>();
            models.add(main);
            models.addAll(loadBuiltinsOnly());
            RWorkspace workspace = RWorkspace.build(models).workspace();
            GeneratorModel gm = new GeneratorModel(workspace,
                    m -> "census.seat32lawf2".equals(m.namespace()));
            JavaTypeUtil typeUtil = new JavaTypeUtil();
            JavaTypeTranslator tt = new JavaTypeTranslator(typeUtil);
            FunctionGenerator fg = new FunctionGenerator(gm, tt, typeUtil);
            RuleGenerator ruleGen = new RuleGenerator(gm, tt, fg);
            Map<String, String> out = new LinkedHashMap<>();
            List<String> errors = new ArrayList<>();
            ruleGen.generateClasses(main, "1.0", out)
                    .forEach(e -> errors.add(e.getTargetPath() + " - " + e));
            fg.generateWithErrors(out)
                    .forEach(e -> errors.add(e.getTargetPath() + " - " + e));
            if (!errors.isEmpty()) {
                throw new AssertionError("fixture generation errors (a broken fixture must"
                        + " fail loudly, not skip): " + errors);
            }
            fixtureOut = out;
        }
        return fixtureOut;
    }

    private static String fn(String fileName) throws IOException {
        return normalize(lookup(fixture(), "functions/" + fileName));
    }

    private static String lookup(Map<String, String> output, String suffix) {
        return output.entrySet().stream()
                .filter(e -> e.getKey().endsWith("/" + suffix))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "'" + suffix + "' was not generated; keys=" + output.keySet()));
    }

    private static void assertContains(String out, String token) {
        assertTrue(out.contains(token), "expected token missing:\n  " + token + "\nin:\n" + out);
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
                        failures.add(p + " - " + e);
                    }
                });
        if (!failures.isEmpty()) {
            throw new AssertionError("[AsKeySetComplexValueDerefSeatTest] builtins parse"
                    + " failures: " + String.join("; ", failures));
        }
        return models;
    }

    // =========================================================================
    // Corpus harness (the seat-30/31 suite shape, verbatim)
    // =========================================================================

    private static Map<String, String> drrAOutput;
    private static List<String> drrAGenErrors;
    private static Map<String, String> cdmBOutput;
    private static List<String> cdmBGenErrors;

    @BeforeAll
    static void generateCells() throws IOException {
        if (cellAAvailable()) {
            List<String> errs = new ArrayList<>();
            drrAOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "5.61.0", CELL_A_ROOT), errs);
            drrAGenErrors = errs;
        }
        if (cellBAvailable()) {
            List<String> errs = new ArrayList<>();
            cdmBOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("cdm", "5.38.0", CELL_B_ROOT), errs);
            cdmBGenErrors = errs;
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
                    "the IR provider must be resolvable under -Pir-on, else this is not an"
                            + " ON-route render");
            var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
            var gm = new GeneratorModel(corpus.workspace(),
                    D11CorpusRegressionTest.emissionFilter(cell));
            var typeUtil = new JavaTypeUtil();
            var typeTranslator = new JavaTypeTranslator(typeUtil);
            FunctionGenerator funcGen = IRGeneration.functionGenerator(gm, typeTranslator, typeUtil);
            assertTrue(!funcGen.getClass().equals(FunctionGenerator.class),
                    "the seam must hand back the IR-route FunctionGenerator, got "
                            + funcGen.getClass());
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

    private static Map<String, String> readGoldenTree(Path root) throws IOException {
        Map<String, String> out = new LinkedHashMap<>();
        try (var stream = Files.walk(root)) {
            stream.filter(p -> p.toString().endsWith(".java")).sorted().forEach(p -> {
                try {
                    out.put(root.relativize(p).toString().replace('\\', '/'), Files.readString(p));
                } catch (IOException e) {
                    throw new AssertionError("golden read failed: " + p, e);
                }
            });
        }
        return out;
    }

    private static void collect(List<String> sink, List<GenerationException> errors) {
        if (errors != null) {
            errors.forEach(e -> sink.add(e.getTargetPath() + " - " + e));
        }
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n");
    }

}
