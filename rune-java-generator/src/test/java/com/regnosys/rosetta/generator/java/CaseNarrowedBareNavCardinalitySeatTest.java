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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.GenerationException;
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

/**
 * SEAT 18 — facet {@code caseNarrowedBareNavCardinality}: <b>a BARE one-name navigation off a
 * choice-switch's case-narrowed subject carries that attribute's own cardinality.</b>
 *
 * <p><b>The defect, and why it hid.</b> {@code FunctionExpressionRenderer}'s ADD-arm gate (facet
 * {@code caseNarrowedDisguisedNav}, PR #368, F-B) appends {@code .getMulti()} when a switch arm is
 * an {@code extract} over a case-narrowed MULTI nav — its javadoc names
 * {@code MapCap/MapFloorRateSchedule} as the carriers by name. It only ever REACHED them where the
 * nav is a DISGUISED two-name chain: in cdm 6.20.x the source
 * {@code capRateSchedule} resolves through {@code floatingRateModel}, arriving as an
 * {@code REnumValueRef} that {@code NavigationHandler.chainProvesMulti} proves MULTI. From
 * cdm 6.21.0 the model moved the attribute onto the case type itself, so the SAME source
 * navigation arrives as a bare {@code RSymbolReference} the linker resolves straight to the
 * narrowed type's own {@code RAttribute}. {@code chainProvesMulti}'s {@code RSymbolReference} arm
 * re-roots only through {@code synthesizeImplicitItem*Navigation} — and a case-narrowed subject is
 * not an item — so it answered false and the drain went missing.
 *
 * <p>Meanwhile the RENDER of that same bare name had already re-rooted it on the narrowed subject
 * and chosen its step from the same cardinality: golden and fork alike emit
 * {@code MapperS.of(floatingRateCalculation).<StrikeSchedule>mapC("getCapRateSchedule", …)} — a
 * {@code .mapC} step, i.e. MULTI. <b>LAW 69</b>, two halves of one law with only one of them
 * recorded.
 *
 * <p><b>What makes the halves agree is an IDENTITY GUARD, not a shared function — the #590
 * independent review's REFUTATION, and it was right.</b> An earlier revision of this seat argued
 * that reading the linker's {@code RSymbolReference.symbol()} binding through the same
 * {@code isMultiValued} the {@code RFeatureCall} arm uses meant the two halves "cannot disagree".
 * <b>False:</b> the render never consults {@code symbol()}. It resolves through
 * {@code ReferenceHandler.synthesizeCaseNarrowedBareNav}, a FRESH by-name lookup on
 * {@code NavigationHandler.caseNarrowedImplicitType}'s result, and THAT attribute's cardinality is
 * what picks {@code .mapC} vs {@code .map}. Two independently resolved {@code RAttribute} objects
 * sharing a comparison function agree only by luck — and this corpus declares the carrier name
 * twice with different cardinality (cdm 6.21.0 {@code product-asset-type.rosetta}:
 * {@code FloatingRateBase.capRateSchedule (0..1)} and
 * {@code StubFloatingRate.capRateSchedule (0..*)}). So the predicate now performs the render's OWN
 * resolution and requires the two to be the SAME object, which is exactly the guard
 * {@code synthesizeImplicitItemNavigation} already applies for the item-rooted arm.
 *
 * <p>{@code b6} pins that the cardinality is read PER ARM against that arm's narrowed type. It does
 * <b>not</b> pin the identity guard itself — see its own note, which records the mutation that
 * measured the difference rather than assuming it.
 *
 * <p><b>LAW 74 — the compiler is the oracle, and it was actually run.</b> Against the shipped
 * {@code rune-runtime} 9.83.0, the fork's pre-seat form is
 * {@code error: no suitable method found for toBuilder(MapperC<…>)} —
 * {@code RosettaFunction.toBuilder} has exactly three overloads
 * ({@code RosettaModelObject}, {@code (RosettaModelObject, Supplier)},
 * {@code List<? extends RosettaModelObject>}) and a {@code MapperC} is none of them. The
 * non-{@code RosettaModelObject} variant ({@code a2}) has no {@code toBuilder} wrapper at all and
 * fails as {@code error: incompatible types: MapperC<BigDecimal> cannot be converted to
 * Collection<? extends BigDecimal>}. Both drained forms compile. This seat repairs <b>14 currently
 * non-compiling corpus files</b>.
 *
 * <p><b>THE PRODUCER WAS FOUND BY RUNTIME PROBE AFTER THE CHARTED ONE WAS REFUTED BY MEASUREMENT
 * — this seat's own first implementation was the refutation.</b> The charter named
 * {@code FunctionExpressionRenderer.unwrapSwitchArmValue}'s {@code return body;} fall-through,
 * whose javadoc promises a drain via {@code unwrapForAddAssignment} it never calls. Draining there
 * healed cdm 6.21/6.22/6.23 and drr 7.0–7.3 exactly as predicted — and ENTERED
 * {@code .getMulti().getMulti()} in cdm 6.20.2–6.20.6, five ring cells the band dump could not
 * show because they were already byte-identical there. <b>A band-derived producer is a hypothesis
 * about the cells that are WRONG; it says nothing about the cells that are already right.</b>
 * Probing the real gate then split the population cleanly: cdm 6.20.x arrives
 * {@code arg=REnumValueRef provesMulti=true}, cdm 6.21+/drr 7.x arrives
 * {@code arg=RSymbolReference provesMulti=false}.
 *
 * <p><b>The blast radius was measured over the WHOLE 275-row matrix (LAW 72), not the carriers'
 * cells:</b> across every corpus, project, version and kind there are <b>exactly 14 switch-arm ADD
 * extract sites whose argument is a bare {@code RSymbolReference}</b>, and all 14 are
 * {@code capRateSchedule} / {@code floorRateSchedule} with {@code multi=true}. There is no other
 * site in the corpus for this arm to move.
 *
 * <p><b>The fixtures mirror the CORPUS shapes</b> (the #583 fixture-truth law) and cover both
 * halves of the law plus the near-miss: {@code b2} reproduces the DISGUISED two-name form that
 * cdm 6.20.x carries and that the first implementation broke, and it is GREEN in both states —
 * it exists to fail if this seat ever double-drains that half again.
 */
class CaseNarrowedBareNavCardinalitySeatTest {

    private static final Path REPO_ROOT =
            Path.of(System.getProperty("user.dir")).resolve("..").normalize();

    private static final List<Path> BUILTINS_SEARCH_ROOTS = List.of(
            REPO_ROOT.resolve("test-corpus/rune-dsl-builtins/rune-runtime/src/main/resources/model"),
            REPO_ROOT.resolve("rune-dsl/rune-runtime/src/main/resources/model"),
            // THIS repo's own builtins, LAST so the roots above keep their precedence — a checkout
            // without the external corpus RUNS these tests instead of skipping them (#586: a test
            // that skips is not a lock).
            REPO_ROOT.resolve("rune-runtime/src/main/resources/model"));

    static boolean builtinsAvailable() {
        return BUILTINS_SEARCH_ROOTS.stream().anyMatch(Files::isDirectory);
    }

    /**
     * {@code Boxed} carries all three navigation shapes the seat must tell apart: a MULTI attribute
     * reached by a bare one-name nav ({@code leaves} — the carrier), a SINGLE one ({@code sole} —
     * the monotonicity decline), and a MULTI one reached through a two-name chain
     * ({@code inner -> deep} — the disguised half that already worked).
     */
    private static final String MODEL_DEP = """
            namespace census.seat18.dep
            version "1.0.0"

            type Leaf: <"the MULTI arm's element">
                amt number (0..1)

            type Res: <"a RosettaModelObject output element - the toBuilder consumption">
                v number (0..1)

            type Inner:
                deep Leaf (0..*)

            type Boxed:
                leaves Leaf (0..*)
                sole Leaf (0..1)
                inner Inner (0..1)

            type Plain: <"b6 - declares `leaves` too, but SINGLE: the identity-guard hazard">
                note string (0..1)
                leaves Leaf (0..1)

            choice Shape:
                Boxed
                Plain
            """;

    private static final String MODEL_MAIN = """
            namespace census.seat18
            version "1.0.0"

            import census.seat18.dep.*

            func MakeRes:
                inputs:
                    leaf Leaf (0..1)
                output:
                    res Res (1..1)
                set res:
                    Res { v: leaf -> amt }

            func MakeAll:
                inputs:
                    boxed Boxed (0..1)
                output:
                    res Res (0..*)
                add res:
                    boxed -> leaves
                        extract MakeRes(item)

            func A1BareMultiNav: <"a1 - THE CARRIER SHAPE: a bare MULTI nav off the narrowed case">
                inputs:
                    shape Shape (0..1)
                output:
                    res Res (0..*)
                add res:
                    shape switch
                        Boxed then
                            leaves
                                extract MakeRes(item),
                        default empty

            func A2BareMultiNavNonModelObject: <"a2 - the same shape with no toBuilder wrapper">
                inputs:
                    shape Shape (0..1)
                output:
                    out number (0..*)
                add out:
                    shape switch
                        Boxed then
                            leaves
                                extract amt,
                        default empty

            func B1BareSingleNav: <"b1 - the SINGLE decline: monotone, add-only">
                inputs:
                    shape Shape (0..1)
                output:
                    res Res (0..*)
                add res:
                    shape switch
                        Boxed then
                            sole
                                extract MakeRes(item),
                        default empty

            func B2DisguisedTwoNameNav: <"b2 - the DISGUISED half that already worked (cdm 6.20.x)">
                inputs:
                    shape Shape (0..1)
                output:
                    res Res (0..*)
                add res:
                    shape switch
                        Boxed then
                            inner -> deep
                                extract MakeRes(item),
                        default empty

            func B3BareFunctionCallArm: <"b3 - a bare fn-call arm never reaches the gate">
                inputs:
                    shape Shape (0..1)
                output:
                    res Res (0..*)
                add res:
                    shape switch
                        Boxed then MakeAll(item),
                        default empty

            func B6SameNameOnTheOtherCaseType: <"b6 - resolves on the NARROWED type, not by name">
                inputs:
                    shape Shape (0..1)
                output:
                    res Res (0..*)
                add res:
                    shape switch
                        Boxed then
                            leaves
                                extract MakeRes(item),
                        Plain then
                            leaves
                                extract MakeRes(item),
                        default empty

            func B4SetSeatBareMultiNav: <"b4 - the SET seat, gated out by isAdd">
                inputs:
                    shape Shape (0..1)
                output:
                    out number (0..1)
                set out:
                    shape switch
                        Boxed then
                            leaves first -> amt,
                        default empty
            """;

    /** The carrier's arm, drained, exactly as golden writes the corpus twin. */
    private static final String DRAINED_ARM =
            "res.addAll(toBuilder(MapperS.of(boxed).<Leaf>mapC(\"getLeaves\","
            + " _boxed -> _boxed.getLeaves())"
            + "\n\t\t\t\t\t.mapItem(item -> MapperS.of(makeRes.evaluate(item.get())))"
            + ".getMulti()));";

    /** The pre-seat form: the identical chain with the drain missing. Does not compile. */
    private static final String UNDRAINED_ARM =
            "res.addAll(toBuilder(MapperS.of(boxed).<Leaf>mapC(\"getLeaves\","
            + " _boxed -> _boxed.getLeaves())"
            + "\n\t\t\t\t\t.mapItem(item -> MapperS.of(makeRes.evaluate(item.get())))));";

    // =========================================================================
    // Part A — the seat (RED at the pre-seat blob)
    // =========================================================================

    /**
     * a1 — a bare MULTI nav off the narrowed case drains, in golden's exact form: the whole
     * statement byte-for-byte including the continuation indent, and the undrained form absent.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_theBareMultiNavArmIsDrained() throws IOException {
        String out = filtered("A1BareMultiNav.java");
        assertContains(out, DRAINED_ARM);
        assertNotContains(out, UNDRAINED_ARM);
    }

    /**
     * a2 — the same defect where the output element is not a {@code RosettaModelObject}: no
     * {@code toBuilder(…)} wrapper at all, so the raw {@code MapperC} went straight to
     * {@code List.addAll(Collection)}. javac calls that
     * {@code incompatible types: MapperC<BigDecimal> cannot be converted to
     * Collection<? extends BigDecimal>}; the drain is the same and equally required.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_theNonModelObjectAddSeatIsDrainedToo() throws IOException {
        assertContains(filtered("A2BareMultiNavNonModelObject.java"),
                "out.addAll(MapperS.of(boxed).<Leaf>mapC(\"getLeaves\","
                + " _boxed -> _boxed.getLeaves())"
                + "\n\t\t\t\t\t.mapItem(item -> item.<BigDecimal>map(\"getAmt\","
                + " leaf -> leaf.getAmt()))"
                + ".getMulti());");
    }

    /**
     * a3 — the drain adds no type name, so the import set must be untouched: it is appended to the
     * already-rendered arm string, downstream of every ref decision.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a3_theDrainLeavesTheImportSetUntouched() throws IOException {
        String out = filtered("A1BareMultiNav.java");
        assertContains(out, "import com.rosetta.model.lib.mapper.MapperS;");
        assertNotContains(out, "import com.rosetta.model.lib.mapper.MapperC;");
    }

    // =========================================================================
    // Part B — placement pins (GREEN in BOTH states: they pin what must NOT move)
    // =========================================================================

    /**
     * b1 — THE ADVERSARIAL PLACEMENT PIN, and the predicate's monotonicity stated as a test: a
     * bare nav to a SINGLE attribute at the very same seat must NOT drain. It renders
     * {@code .map("getSole", …).mapSingleToItem(…)} — a single-valued chain — and a
     * {@code .getMulti()} there would be wrong in the other direction. The new predicate reads the
     * attribute's OWN cardinality, so this is exactly the case it must decline.
     *
     * <p>(That this fixture's own {@code toBuilder(<MapperS chain>)} consumption is itself
     * uncompilable is a SEPARATE, pre-existing question. It has no carrier in the corpus — the
     * whole-cell {@code control1} finds zero undrained Mapper-headed ADD arguments — so it is
     * banked in {@code target/seat18-charter.md}, not fixed here.)
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b1_theBareSingleNavArmIsNotDrained() throws IOException {
        String out = filtered("B1BareSingleNav.java");
        assertContains(out,
                "res.addAll(toBuilder(MapperS.of(boxed).<Leaf>map(\"getSole\","
                + " _boxed -> _boxed.getSole())"
                + "\n\t\t\t\t\t.mapSingleToItem(item -> MapperS.of(makeRes.evaluate(item.get())))));");
        assertNotContains(out, ".getMulti()");
    }

    /**
     * b2 — <b>THE NEAR-MISS PIN.</b> The DISGUISED two-name nav is the half that already worked
     * (PR #368, and the shape cdm 6.20.x carries); it must drain EXACTLY ONCE. This seat's first
     * implementation sat one layer down, in {@code unwrapSwitchArmValue}'s fall-through, and
     * appended a SECOND {@code .getMulti()} here — five ring cells entered mismatches that the
     * band dump could not have predicted, because they were already byte-identical. This test is
     * what makes that regression impossible to reintroduce silently.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b2_theDisguisedTwoNameNavDrainsExactlyOnce() throws IOException {
        String out = filtered("B2DisguisedTwoNameNav.java");
        assertContains(out,
                "res.addAll(toBuilder(MapperS.of(boxed).<Inner>map(\"getInner\","
                + " _boxed -> _boxed.getInner()).<Leaf>mapC(\"getDeep\", inner -> inner.getDeep())"
                + "\n\t\t\t\t\t.mapItem(item -> MapperS.of(makeRes.evaluate(item.get())))"
                + ".getMulti()));");
        assertNotContains(out, ".getMulti().getMulti()");
        assertEquals(1, countOccurrences(out, ".getMulti()"),
                "the disguised half must drain exactly once");
    }

    /**
     * b3 — a bare function-call arm is not an {@code extract} at all, so it never reaches the
     * gate. Its value already IS the {@code List} and it must be spliced verbatim. Over the whole
     * matrix this is the dominant arm shape: 97 of the 99 switch-arm ADD sites per cell present as
     * {@code RSymbolReference} expressions the gate never sees.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b3_theBareFunctionCallArmStaysVerbatim() throws IOException {
        String out = filtered("B3BareFunctionCallArm.java");
        assertContains(out, "res.addAll(toBuilder(makeAll.evaluate(boxed)));");
        assertNotContains(out, "makeAll.evaluate(boxed).getMulti()");
    }

    /**
     * b4 — the SET seat is gated out by {@code isAdd}. A single-valued seat that drained with
     * {@code .getMulti()} would be wrong in the other direction.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b4_theSetSeatIsUntouched() throws IOException {
        String out = filtered("B4SetSeatBareMultiNav.java");
        assertContains(out,
                "out = MapperS.of(boxed).<Leaf>mapC(\"getLeaves\", _boxed -> _boxed.getLeaves())"
                + "\n\t\t\t\t\t.first().<BigDecimal>map(\"getAmt\", leaf -> leaf.getAmt());");
        assertNotContains(out, ".getMulti()");
    }

    /**
     * b6 — <b>THE IDENTITY-GUARD PIN (the #590 independent review's REFUTATION).</b> Both case
     * types declare {@code leaves}, with DIFFERENT cardinality — {@code Boxed.leaves (0..*)} and
     * {@code Plain.leaves (0..1)}. That is not a contrived shape: cdm 6.21.0's
     * {@code product-asset-type.rosetta} declares this seat's own carrier name twice the same way
     * ({@code FloatingRateBase.capRateSchedule (0..1)},
     * {@code StubFloatingRate.capRateSchedule (0..*)}).
     *
     * <p>What this pins, stated exactly: in ONE generated file the {@code Boxed} arm drains and the
     * {@code Plain} arm does not, so the cardinality is read PER ARM against that arm's narrowed
     * type — never by name across the switch. Draining the {@code Plain} arm would put
     * {@code .getMulti()} on a {@code MapperS}-headed chain: non-compiling Java.
     *
     * <p><b>What this does NOT pin, disclosed rather than glossed.</b> It does not exercise the
     * predicate's IDENTITY GUARD. <b>Measured by mutation:</b> deleting the guard leaves this whole
     * suite 17/17 GREEN, because through this shape the LINKER narrows exactly as the render does
     * — {@code symbol()} already binds {@code Plain.leaves} in the {@code Plain} arm. The guard is
     * defence against the two halves resolving DIFFERENTLY, which needs
     * {@code caseNarrowedImplicitType} to decline where the linker did not (its lambda-boundary
     * decline), and at this gate the switch is always the statement-level assignment's own switch,
     * so that shape does not arise here today. The guard is kept because it costs nothing measured
     * (the full matrix reads the same 471/173,640/30 with it in place) and because without it the
     * predicate would be {@code chainProvesMulti}'s {@code RAttribute} arm minus the identity check
     * that arm's own delegate applies. A fixture that reaches it is BANKED in
     * {@code target/seat18-charter.md}.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b6_theSameNameOnTheOtherCaseTypeResolvesOnItsOwnType() throws IOException {
        String out = filtered("B6SameNameOnTheOtherCaseType.java");
        assertContains(out, DRAINED_ARM);
        assertEquals(1, countOccurrences(out, ".getMulti()"),
                "exactly the MULTI arm drains; the SINGLE arm of the same name must not");
    }

    /**
     * b5 — the absent arms (null subject, empty default) are the two siblings of the carrier in
     * every corpus file this seat heals, and they were already byte-identical to golden.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b5_theEmptyArmsAreUntouched() throws IOException {
        String out = filtered("A1BareMultiNav.java");
        assertEquals(2,
                countOccurrences(out, "res.addAll(toBuilder(Collections.<Res>emptyList()));"),
                "both absent arms (null subject + empty default) must stay exactly as they were");
        assertNotContains(out, "Collections.<Res>emptyList().getMulti()");
    }

    // =========================================================================
    // Part C — corpus locks + the LAW-72 whole-cell controls
    // =========================================================================

    private static final Path DRR7_CELL_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path DRR7_GOLDEN_DIR =
            DRR7_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path CDM6202_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.20.2");
    private static final Path CDM6202_GOLDEN_DIR =
            CDM6202_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    static boolean drr7Available() {
        return Drr7Corpus.gate(Files.isDirectory(DRR7_CELL_ROOT) && Files.isDirectory(DRR7_GOLDEN_DIR), CaseNarrowedBareNavCardinalitySeatTest.class);
    }

    static boolean cdm6202Available() {
        return Files.isDirectory(CDM6202_CELL_ROOT) && Files.isDirectory(CDM6202_GOLDEN_DIR);
    }

    private static final String CAPRATE =
            "cdm/ingest/fpml/confirmation/pricequantity/functions/"
            + "MapCapRateScheduleToPriceWithLocation.java";
    private static final String FLOORRATE =
            "cdm/ingest/fpml/confirmation/pricequantity/functions/"
            + "MapFloorRateScheduleToPriceWithLocation.java";

    /** c1 — a WHOLE-FILE heal. Both of its diff-lines were this seat's. */
    @Test
    @EnabledIf("drr7Available")
    void corpus_c1_mapCapRateSchedule() throws IOException {
        lockDrr7(CAPRATE);
    }

    /** c2 — the twin, same shape, same single diff-line. */
    @Test
    @EnabledIf("drr7Available")
    void corpus_c2_mapFloorRateSchedule() throws IOException {
        lockDrr7(FLOORRATE);
    }

    /**
     * c3 — THE CROSS-CELL PRECONDITION for the other twelve heals: the six sibling cells carry a
     * byte-identical golden for both carriers, so the fork faces the same expectation there.
     *
     * <p>This is a PRECONDITION, not proof (the #589 R2 correction): it never RUNS the generator
     * for those cells. What attests the other twelve heals is the FULL-MATRIX receipt, measured
     * per cell. An absent sibling FAILS rather than skipping (the #588 MF-3 rule) — all six are in
     * the frozen 9.83.0 manifest, so absence is a broken checkout.
     */
    @Test
    @EnabledIf("drr7Available")
    void corpus_c3_theSixSiblingCellsAreIdentical() throws IOException {
        List<Path> siblings = List.of(
                Path.of("../test-corpus/drr/drr-7.1.0"),
                Path.of("../test-corpus/drr/drr-7.2.0"),
                Path.of("../test-corpus/drr/drr-7.3.0"),
                Path.of("../test-corpus/cdm/cdm-6.21.0"),
                Path.of("../test-corpus/cdm/cdm-6.22.0"),
                Path.of("../test-corpus/cdm/cdm-6.23.0"));
        int compared = 0;
        for (String rel : List.of(CAPRATE, FLOORRATE)) {
            Path base = DRR7_GOLDEN_DIR.resolve(rel);
            assertTrue(Files.isRegularFile(base), "golden missing: " + base);
            String expected = normalize(Files.readString(base));
            for (Path cell : siblings) {
                Path sib = cell.resolve("rosetta-source/src/generated/java").resolve(rel);
                assertTrue(Files.isRegularFile(sib),
                        "sibling cell golden missing: " + sib + " — that cell is in the frozen"
                        + " 9.83.0 manifest, so this checkout is incomplete and drr 7.0.0 can no"
                        + " longer stand in for it.");
                assertEquals(expected, normalize(Files.readString(sib)),
                        "sibling golden diverged: " + sib);
                compared++;
            }
        }
        assertEquals(12, compared, "both carriers must be compared against all six sibling cells");
    }

    /**
     * c4 — <b>THE NEAR-MISS CORPUS LOCK.</b> cdm 6.20.2 is where the DISGUISED half lives: its
     * golden for both carriers already carries {@code .getMulti()}, and the fork already matched
     * it byte-for-byte before this seat. The first implementation of this seat double-drained
     * exactly these two files across five cells. Locking them here means the regression cannot
     * return without a named, failing test — and unlike {@code b2}, this one is measured against
     * the real golden rather than a fixture.
     */
    @Test
    @EnabledIf("cdm6202Available")
    void corpus_c4_theDisguisedHalfInCdm6202IsUnchanged() throws IOException {
        lockCdm6202(CAPRATE);
        lockCdm6202(FLOORRATE);
    }

    /**
     * control 0 — <b>THE ORACLE, read off GOLDEN, so the two controls below assert a MEASURED law
     * rather than this seat's opinion of one.</b> Scanning the frozen drr 7.0.0 golden tree with
     * the very same scan the fork is held to:
     * <ul>
     *   <li><b>116 MAPPER-HEADED {@code .addAll(…)} arguments in 71 files, ZERO undrained</b> — at
     *       the ADD seat golden drains a Mapper without exception.</li>
     *   <li><b>163 BARE {@code <ident>.evaluate(…)} arguments in 57 files, ZERO drained</b> —
     *       golden never drains one, because its value already IS the {@code List}.</li>
     * </ul>
     * Both figures come from the frozen 9.83.0 corpus, so they are stable pins, not fork counts
     * that drift as other seats land. This is also the scan's own POSITIVE CONTROL (LAW 66): if
     * the character walk ever stopped finding sites, these counts would collapse and say so here,
     * before the emitter controls below could pass vacuously.
     */
    @Test
    @EnabledIf("drr7Available")
    void corpus_control0_goldenIsTheOracleForBothDirections() throws IOException {
        AddSeatScan golden = scan(readGoldenCell());
        assertEquals(List.of(), golden.undrainedMapperHeaded,
                "golden was expected to drain EVERY Mapper-headed .addAll argument");
        assertEquals(List.of(), golden.drainedBareFnCall,
                "golden was expected to drain NO bare function-call .addAll argument");
        assertEquals(116, golden.mapperHeaded,
                "the frozen drr 7.0.0 golden tree has 116 Mapper-headed .addAll sites");
        assertEquals(163, golden.bareFnCall,
                "the frozen drr 7.0.0 golden tree has 163 bare function-call .addAll sites");
    }

    /**
     * control 1 — <b>LAW 72: the control's domain is the MECHANISM's reach, not the carrier
     * subset.</b> Over the WHOLE generated drr 7.0.0 cell, every {@code .addAll(<argument>)} whose
     * argument is MAPPER-HEADED (directly, or inside a {@code toBuilder(…)} wrapper) must end
     * {@code .getMulti()} — golden's law, measured in {@code control0}.
     *
     * <p>The fork's TOTAL is deliberately not pinned against golden's 116: this cell still carries
     * unrelated band mismatches, so a shape difference elsewhere legitimately changes how many
     * arguments present a Mapper head. What is pinned is the direction — none undrained — plus the
     * two carriers being reached, so the control cannot pass by finding nothing.
     *
     * <p>The scan runs over a {@link #codeOnly} view (string and char literals and both comment
     * forms blanked), so a shape appearing inside a generated literal cannot be counted as an
     * emission site.
     */
    @Test
    @EnabledIf("drr7Available")
    void corpus_control1_everyMapperHeadedAddIsDrainedAcrossTheWholeCell() {
        assertNotNull(drr7Output, "drr 7.0.0 generation did not run — corpus unavailable?");
        AddSeatScan fork = scan(drr7Output);
        assertEquals(List.of(), fork.undrainedMapperHeaded,
                "a Mapper-headed .addAll(...) argument was emitted UNDRAINED. addAll takes a"
                + " Collection and toBuilder takes a RosettaModelObject or a List — a Mapper is"
                + " neither, so this does not compile. Golden drains every one of its 116.");
        assertTrue(fork.mapperHeadedFiles.contains(CAPRATE)
                        && fork.mapperHeadedFiles.contains(FLOORRATE),
                "the scan must reach both seat carriers, else 'none undrained' is vacuous; files"
                + " seen with a Mapper-headed .addAll: " + fork.mapperHeadedFiles);
    }

    /**
     * control 2 — the OTHER direction, over the same whole-cell domain: a BARE
     * {@code <ident>.evaluate(…)} argument must never be drained. This is the dominant arm
     * population's protection measured at corpus scale rather than in the fixture, and it is what
     * fails if the gate is ever widened to fire on a non-extract arm. Golden has 163 such sites
     * and drains none.
     */
    @Test
    @EnabledIf("drr7Available")
    void corpus_control2_noBareFunctionCallAddIsDrained() {
        assertNotNull(drr7Output, "drr 7.0.0 generation did not run — corpus unavailable?");
        AddSeatScan fork = scan(drr7Output);
        assertEquals(List.of(), fork.drainedBareFnCall,
                "a BARE function-call .addAll(...) argument was drained. Its value already IS the"
                + " List, so .getMulti() does not resolve.");
        assertTrue(fork.bareFnCall >= 100,
                "expected the bare function-call population to be present in the cell (golden has"
                + " 163); found " + fork.bareFnCall + " — the scan stopped seeing them, so 'none"
                + " drained' would be vacuous.");
    }

    /**
     * control 3 — the neighbouring seats' heals stay healed. {@code IndexFactorRule} (seat 17
     * rung 1) and {@code JurisdictionOfCounterparty1Rule} (seat 16) are byte-identical at the
     * merged head; both live on the arm-rendering path this seat edits.
     */
    @Test
    @EnabledIf("drr7Available")
    void corpus_control3_theNeighbouringSeatHealsStayHealed() throws IOException {
        lockDrr7("drr/regulation/common/trade/index/reports/IndexFactorRule.java");
        lockDrr7("drr/regulation/csa/rewrite/trade/reports/JurisdictionOfCounterparty1Rule.java");
    }

    // =========================================================================
    // The whole-cell scan (a character walk over a code-only view — no regex)
    // =========================================================================

    /**
     * The ADD-seat census of one tree: how many {@code .addAll(…)} arguments are MAPPER-HEADED and
     * how many are BARE {@code <ident>.evaluate(…)} calls, plus the two violation lists (a Mapper
     * left undrained; a bare call wrongly drained).
     */
    private static final class AddSeatScan {
        private int mapperHeaded;
        private int bareFnCall;
        private final Set<String> mapperHeadedFiles = new LinkedHashSet<>();
        private final List<String> undrainedMapperHeaded = new ArrayList<>();
        private final List<String> drainedBareFnCall = new ArrayList<>();
    }

    /** Run the ADD-seat census over a path &rarr; Java-source map (fork output or golden tree). */
    private static AddSeatScan scan(Map<String, String> tree) {
        AddSeatScan r = new AddSeatScan();
        for (Map.Entry<String, String> e : tree.entrySet()) {
            for (String arg : addAllArguments(e.getValue())) {
                String inner = stripToBuilder(arg);
                boolean drained = inner.endsWith(".getMulti()");
                if (inner.startsWith("MapperS.") || inner.startsWith("MapperC.")) {
                    r.mapperHeaded++;
                    r.mapperHeadedFiles.add(e.getKey());
                    if (!drained) {
                        r.undrainedMapperHeaded.add(e.getKey() + " — " + inner);
                    }
                } else if (isBareFunctionCall(inner)) {
                    r.bareFnCall++;
                    if (drained) {
                        r.drainedBareFnCall.add(e.getKey() + " — " + inner);
                    }
                }
            }
        }
        r.undrainedMapperHeaded.sort(String::compareTo);
        r.drainedBareFnCall.sort(String::compareTo);
        return r;
    }

    /**
     * The bare-arm population, stated exactly: the argument is ONE
     * {@code <javaIdentifier>.evaluate( … )} call and nothing else — the closing paren of that
     * call is the end of the argument. A chain that merely CONTAINS an {@code .evaluate(} deeper
     * inside a lambda is a different shape and is not counted here.
     */
    private static boolean isBareFunctionCall(String arg) {
        int i = 0;
        while (i < arg.length() && isIdentifierChar(arg.charAt(i), i == 0)) {
            i++;
        }
        if (i == 0 || !arg.startsWith(".evaluate(", i)) {
            return false;
        }
        return matchParen(arg, i + ".evaluate".length()) == arg.length() - 1;
    }

    private static boolean isIdentifierChar(char c, boolean first) {
        return first ? Character.isJavaIdentifierStart(c) : Character.isJavaIdentifierPart(c);
    }

    /** The frozen drr 7.0.0 golden tree as a cell-relative path &rarr; source map. */
    private static Map<String, String> readGoldenCell() throws IOException {
        Map<String, String> tree = new LinkedHashMap<>();
        try (var stream = Files.walk(DRR7_GOLDEN_DIR)) {
            for (Path p : stream.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java")).toList()) {
                tree.put(DRR7_GOLDEN_DIR.relativize(p).toString().replace('\\', '/'),
                        Files.readString(p));
            }
        }
        assertTrue(tree.size() > 1000,
                "the frozen drr 7.0.0 golden tree looks truncated: " + tree.size() + " files");
        return tree;
    }

    /** Every balanced {@code .addAll( … )} argument in one file, whitespace-collapsed. */
    private static List<String> addAllArguments(String source) {
        String code = codeOnly(normalize(source));
        List<String> args = new ArrayList<>();
        int i = 0;
        while (true) {
            i = code.indexOf(".addAll(", i);
            if (i < 0) {
                return args;
            }
            int open = i + ".addAll".length();
            int close = matchParen(code, open);
            if (close < 0) {
                return args;
            }
            args.add(collapse(code.substring(open + 1, close)));
            i = open + 1;
        }
    }

    /** Peel one {@code toBuilder( … )} wrapper, when the argument is exactly that call. */
    private static String stripToBuilder(String arg) {
        if (!arg.startsWith("toBuilder(") || !arg.endsWith(")")) {
            return arg;
        }
        int close = matchParen(arg, "toBuilder".length());
        return close == arg.length() - 1
                ? arg.substring("toBuilder(".length(), close).trim()
                : arg;
    }

    /** Collapse every run of whitespace to one space — a character walk, not a pattern match. */
    private static String collapse(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        boolean pending = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isWhitespace(c)) {
                pending = sb.length() > 0;
            } else {
                if (pending) {
                    sb.append(' ');
                    pending = false;
                }
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * A CODE-ONLY view of generated Java: string literals, char literals and both comment forms
     * blanked to spaces, newlines preserved. A character walk, not a pattern match, and it performs
     * no structural analysis of the language content — only a literal-token scan of the fork's own
     * just-emitted output.
     */
    private static String codeOnly(String s) {
        char[] out = s.toCharArray();
        for (int i = 0; i < out.length; i++) {
            char c = out[i];
            if (c == '"' || c == '\'') {
                char quote = c;
                int j = i + 1;
                while (j < out.length && out[j] != quote) {
                    j += out[j] == '\\' ? 2 : 1;
                }
                for (int k = i; k <= Math.min(j, out.length - 1); k++) {
                    if (out[k] != '\n') {
                        out[k] = ' ';
                    }
                }
                i = j;
            } else if (c == '/' && i + 1 < out.length && out[i + 1] == '/') {
                int j = i;
                while (j < out.length && out[j] != '\n') {
                    out[j++] = ' ';
                }
                i = j;
            } else if (c == '/' && i + 1 < out.length && out[i + 1] == '*') {
                int j = i;
                while (j + 1 < out.length && !(out[j] == '*' && out[j + 1] == '/')) {
                    if (out[j] != '\n') {
                        out[j] = ' ';
                    }
                    j++;
                }
                if (j < out.length) {
                    out[j] = ' ';
                }
                if (j + 1 < out.length) {
                    out[j + 1] = ' ';
                }
                i = j + 1;
            }
        }
        return new String(out);
    }

    /** Index of the {@code ')'} matching the {@code '('} at {@code open}, or -1. */
    private static int matchParen(String s, int open) {
        int depth = 0;
        for (int i = open; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')' && --depth == 0) {
                return i;
            }
        }
        return -1;
    }

    private static int countOccurrences(String haystack, String needle) {
        int n = 0;
        int i = haystack.indexOf(needle);
        while (i >= 0) {
            n++;
            i = haystack.indexOf(needle, i + needle.length());
        }
        return n;
    }

    // =========================================================================
    // Corpus harness
    // =========================================================================

    private static Map<String, String> drr7Output;
    private static List<String> drr7GenErrors = new ArrayList<>();
    private static Map<String, String> cdm6202Output;
    private static List<String> cdm6202GenErrors = new ArrayList<>();

    @BeforeAll
    static void generateCells() throws IOException {
        if (drr7Available()) {
            List<String> errs = new ArrayList<>();
            drr7Output = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", DRR7_CELL_ROOT), errs);
            drr7GenErrors = errs;
        }
        if (cdm6202Available()) {
            List<String> errs = new ArrayList<>();
            cdm6202Output = generateCell(
                    new D11CorpusRegressionTest.CellSpec("cdm", "6.20.2", CDM6202_CELL_ROOT), errs);
            cdm6202GenErrors = errs;
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
        var funcGen = new FunctionGenerator(gm, typeTranslator, typeUtil);
        var ruleGen = new RuleGenerator(gm, typeTranslator, funcGen);
        var reportGen = new ReportGenerator(gm, typeTranslator, funcGen);
        var dataRuleGen = new DataRuleGenerator(gm, typeTranslator, typeUtil);
        var labelProviderGen = new LabelProviderGenerator(
                gm, typeTranslator, new DeepFeatureCallUtil(gm::getType),
                new LabelProviderGeneratorUtil());
        Map<String, String> output = new LinkedHashMap<>();
        // #588/#589 (Copilot): every generator leg's per-model errors are aggregated, not
        // discarded — dropping them lets a byte-lock pass while unrelated work in the same cell
        // failed to generate, which is green for the wrong reason.
        for (RModel model : corpus.workspace().files()) {
            if (gm.shouldGenerate(model)) {
                String version = gm.version(model);
                collect(errors, pojoGen.generateClasses(model, version, output));
                collect(errors, choiceGen.generateClasses(model, version, output));
                collect(errors, ruleGen.generateClasses(model, version, output));
                collect(errors, reportGen.generateClasses(model, version, output));
                collect(errors, dataRuleGen.generateClasses(model, version, output));
                collect(errors, labelProviderGen.generateClasses(model, version, output));
            }
        }
        collect(errors, funcGen.generateWithErrors(output));
        return output;
    }

    /** Aggregate one generator leg's reported errors; surfaced per locked file by {@code lock}. */
    private static void collect(List<String> sink, List<GenerationException> errors) {
        if (errors != null) {
            errors.forEach(e -> sink.add(e.getTargetPath() + " — " + e));
        }
    }

    private static void lockDrr7(String path) throws IOException {
        lock(drr7Output, drr7GenErrors, DRR7_GOLDEN_DIR, "drr 7.0.0", path);
    }

    private static void lockCdm6202(String path) throws IOException {
        lock(cdm6202Output, cdm6202GenErrors, CDM6202_GOLDEN_DIR, "cdm 6.20.2", path);
    }

    private static void lock(Map<String, String> output, List<String> genErrors, Path goldenDir,
            String cell, String path) throws IOException {
        assertNotNull(output, cell + " generation did not run — corpus unavailable?");
        List<String> lockedErrors = genErrors.stream().filter(e -> e.contains(path)).toList();
        assertTrue(lockedErrors.isEmpty(),
                "the generator reported errors for the locked file " + path + ": " + lockedErrors);
        String generated = output.get(path);
        assertNotNull(generated, "not generated in " + cell + ": " + path);
        Path goldenPath = goldenDir.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "golden missing: " + goldenPath);
        assertEquals(normalize(Files.readString(goldenPath)), normalize(generated),
                "generated " + cell + " output must byte-match golden (newline-normalized) for "
                + path + " — seat 18: a bare one-name navigation off a choice-switch's"
                + " case-narrowed subject carries that attribute's own cardinality.");
    }

    // =========================================================================
    // Fixture harness
    // =========================================================================

    private static RLinkingResult linking;
    private static RModel mainModel;
    private static Map<String, String> filteredOut;

    private static void link() throws IOException {
        if (linking == null) {
            RModel dep = AstBuilder.buildFromString(MODEL_DEP, "seat18-dep.rosetta");
            RModel main = AstBuilder.buildFromString(MODEL_MAIN, "seat18.rosetta");
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
            filteredOut = render(m -> "census.seat18".equals(m.namespace()));
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
            throw new AssertionError("[CaseNarrowedBareNavCardinalitySeatTest] builtins parse"
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
        assertTrue(!out.contains(token),
                "forbidden token present: " + token + "\n--- in output:\n" + out);
    }
}
