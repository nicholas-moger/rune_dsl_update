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
 * SEAT 15 — the ARGUMENT-POSITION rule-wrap arm (facet {@code argPosRuleRefWrapThenArg}) and its
 * SINGLE-receiver element-preserve half (facet {@code singleFilterElementPreserve}).
 *
 * <p><b>The law.</b> Two decl seats emit the same {@code final Mapper*<X> thenArgN = …;}
 * declaration for a then-chain whose {@code k == 0} BASE is a bare no-arg RULE reference:
 * {@code FunctionExpressionRenderer.renderThenExtractSetImpl} (the rule's own body) and
 * {@code CollectionHandler}'s deep-then hoist (a chain reached through a FUNCTION-CALL ARGUMENT).
 * When the invoked rule's rosetta OUTPUT is a META wrapper over exactly this element, the value
 * the decl holds is the WRAPPER — its {@code evaluate()} returns the bare type, and the interior
 * consumers deref {@code .getValue()} off the declared local. The FER seat has re-presented the
 * model type since PR #360 (facet {@code ruleRefWrapThenArg}); this one never did. THE TWO HALVES
 * MUST AGREE (LAW 69).
 *
 * <p><b>The producer was CONFIRMED BY RUNTIME PROBE (LAW 72), not by attribution.</b>
 * Instrumenting the seat printed exactly ONE site in the drr 7.0.0 cell reaching the carrier —
 * {@code [PROBE-CH-SINK] k=0 type=MapperS<NonNegativeQuantitySchedule>
 * val=MapperS.of(quantityScheduleRule.evaluate(input)) baseClass=RSymbolReference} — on the
 * STATEMENT-SINK path, never the pending-lambda path. (Seats 13 and 14 each had their charted
 * producer refuted by exactly this instrument, so it is run before any code, every time.)
 *
 * <p><b>The second half.</b> Re-typing level 0 to the wrapper exposed that PR #362's
 * element-preserving FILTER arm was gated {@code isMapperC} only, so a SINGLE
 * {@code filterSingleNullSafe} level recomputed its element from the meta-blind snapshot and
 * declared the BARE type — reproducing, one level up, the very disagreement this seat removes.
 * {@code filterItemNullSafe} ({@code MapperC<T> → MapperC<T>}) and {@code filterSingleNullSafe}
 * ({@code MapperS<T> → MapperS<T>}) are equally element-preserving; the widening is confined to
 * the filter shape and keeps #362's own value-type-equality fire gates.
 *
 * <p><b>Green-safety is a property of the PREDICATE, never of the decl token</b> — the #585
 * REFUTATION, which found a control asserting "no golden carries the pre-fix form" while scanning
 * one cell, when the token was in 66 goldens across 20 cells. {@code final MapperS<Bare> thenArg}
 * is an ordinary declaration. What makes the arm safe is that where the gate fires — a recovered
 * meta wrapper whose VALUE type equals the element already computed — the interior consumers
 * deref {@code .getValue()} off that local, so the bare-typed decl is a generics mismatch that
 * never compiled. See {@code control1}.
 *
 * <p><b>What this seat FIXES, and what it deliberately does NOT.</b> The carrier
 * {@code NotionalQuantityScheduleRule} goes 12 → 2 diff-lines and is pinned SUB-FILE, not as a
 * whole-file heal. Its last two lines are an INDEPENDENT, PRE-EXISTING terminal-cardinality
 * divergence ({@code }).get()} where golden has {@code }).getMulti()}) whose producer four probe
 * runs failed to locate; it is banked with that evidence rather than guessed at. That terminal
 * divergence DOES predate #585 (measured: the diff-line count was 12 both before and after it).
 *
 * <p><b>But it is a byte divergence, NOT the compile break — and this seat REPAIRS the compile
 * break.</b> An earlier revision of this javadoc asserted the terminal was "what keeps the file
 * from compiling" on a type-level derivation. The independent review refuted that and a real
 * compiler settled it (three probes against the shipped {@code rune-runtime}, recorded in the
 * PR body): the terminal assignment COMPILES — {@code R} infers to
 * {@code glb(RosettaModelObjectBuilder, List<…Builder>)}, well-formed because both bounds are
 * interfaces (JLS 18.4 / 5.1.10). The actual compile break was the {@code "Type coercion"} hop
 * #585 introduced over a receiver still declared at the BARE value, where the explicitly-supplied
 * witness {@code F} met a {@code BigDecimal}-returning {@code getValue()}. Re-typing that receiver
 * to the wrapper is exactly what this seat does, so <b>#585's own disclosure was right and seat 15
 * closes it</b>. See {@code target/seat15-charter.md} §1.2 and §4, and
 * {@code DeclaredThenArgTypeSeatTest.corpus_control3}.
 */
class ArgumentPositionRuleWrapSeatTest {

    private static final Path REPO_ROOT =
            Path.of(System.getProperty("user.dir")).resolve("..").normalize();

    private static final List<Path> BUILTINS_SEARCH_ROOTS = List.of(
            REPO_ROOT.resolve("test-corpus/rune-dsl-builtins/rune-runtime/src/main/resources/model"),
            REPO_ROOT.resolve("rune-dsl/rune-runtime/src/main/resources/model"),
            // THIS repo's own builtins, LAST so the two roots above keep their precedence
            // (loadBuiltinsOnly is putIfAbsent keyed on file name, so first root wins) but a
            // checkout without the external corpus RUNS these tests instead of silently
            // skipping them. A test that skips is not a lock - Copilot's suppressed finding on
            // PR #586 and the independent review's NIT 7, same class as #579 R1-1.
            REPO_ROOT.resolve("rune-runtime/src/main/resources/model")
    );

    static boolean builtinsAvailable() {
        return BUILTINS_SEARCH_ROOTS.stream().anyMatch(Files::isDirectory);
    }

    /**
     * The DECOY namespace — {@code Sched} under a SECOND namespace, registered FIRST.
     *
     * <p><b>Caveat, measured at PR #586:</b> this fixture does NOT in fact separate a canonical
     * value-type comparison from a simple-name one — {@code DecoyHolder.sch} names THIS namespace,
     * which is also the one the rule returns, so both answers coincide. See {@code b5}, which
     * records the mutation proof and what a real pin would need.
     */
    private static final String MODEL_OTHER = """
            namespace census.seat15.other
            version "1.0.0"

            type Sched: <"the DECOY - the same simple name as the dep type, another namespace">
                amt string (0..1)
            """;

    /** The dependency namespace — LOADED but NOT GENERATED, exactly as every drr cell navigates. */
    private static final String MODEL_DEP = """
            namespace census.seat15.dep
            version "1.0.0"

            type Sched: <"the carrier element - shares its simple name with the decoy (b5)">
                amt string (0..1)

            type Other: <"b2 - a DIFFERENT value type behind the same meta kind">
                otag string (0..1)

            type Holder:
                sch Sched (0..1)
                    [metadata reference]
                bare Sched (0..1)

            type OtherHolder:
                oth Other (0..1)
                    [metadata reference]

            type DecoyHolder: <"b5 - the same SIMPLE name, another namespace">
                sch census.seat15.other.Sched (0..1)
                    [metadata reference]

            type Inner:
                a Holder (0..1)
                b Holder (0..1)
                o OtherHolder (0..1)
                d DecoyHolder (0..1)
            """;

    /**
     * The MAIN namespace. Every carrier mirrors the CORPUS shape (the #583/#585 fixture-truth
     * lesson): a RULE head whose body is a FUNCTION CALL with the then-chain in an ARGUMENT —
     * {@code EnrichDatedValueWithEndDate(QuantitySchedule then filter …, CustomSchedule)}.
     * A fixture that put the chain at the rule's own top level would exercise the FER seat that
     * ALREADY has this arm, and every decline control would pass vacuously.
     */
    private static final String MODEL_MAIN = """
            namespace census.seat15
            version "1.0.0"

            import census.seat15.dep.*

            func Consume: <"the argument-position host - the EnrichDatedValueWithEndDate analogue">
                inputs:
                    s string (0..1)
                    t Sched (0..1)
                output:
                    r string (0..1)
                set r: s

            func ConsumeOther: <"b2 - the host for the disagreeing-value-type control">
                inputs:
                    s string (0..1)
                output:
                    r string (0..1)
                set r: s

            reporting rule MetaInner from Inner: <"the inner rule whose OUTPUT recovers a meta wrapper">
                extract inn [
                    inn -> a -> sch
                        then default inn -> b -> sch
                ]

            reporting rule BareInner from Inner: <"b1 - the same shape over a NON-META leaf">
                extract inn [
                    inn -> a -> bare
                        then default inn -> b -> bare
                ]

            reporting rule OtherInner from Inner: <"b2 - a meta wrapper over a DIFFERENT value type">
                extract inn [
                    inn -> o -> oth
                        then default inn -> o -> oth
                ]

            reporting rule DecoyInner from Inner: <"b5 - the same meta KIND over the same SIMPLE name, another namespace">
                extract inn [
                    inn -> d -> sch
                        then default inn -> d -> sch
                ]

            reporting rule A1Carrier from Inner: <"a1/a2/a3/a4 - THE CARRIER: a meta inner rule at the k==0 base of an ARGUMENT chain">
                Consume(
                    MetaInner
                        then filter amt exists
                        then extract amt,
                    MetaInner
                )

            reporting rule B1BareInner from Inner: <"b1 - GREEN control: a non-meta inner rule keeps the bare decl">
                Consume(
                    BareInner
                        then filter amt exists
                        then extract amt,
                    BareInner
                )

            reporting rule B2OtherValueType from Inner: <"b2 - GREEN control: the recovered wrapper's VALUE type is not this element">
                ConsumeOther(
                    OtherInner
                        then filter otag exists
                        then extract otag
                )

            reporting rule B3FunctionBase from Inner: <"b3 - GREEN control: the base is a FUNCTION, not a RULE">
                Consume(
                    MakeSched
                        then filter amt exists
                        then extract amt,
                    MetaInner
                )

            func MakeSched: <"b3 - the bare-FUNCTION base the RRule gate must exclude">
                inputs:
                    i Inner (0..1)
                output:
                    o Sched (0..1)
                set o: empty

            reporting rule B5DecoyNamespace from Inner: <"b5 - the multi-namespace lift pin (NOT a canonicality pin - see b5)">
                Consume(
                    DecoyInner
                        then filter amt exists
                        then extract amt,
                    MetaInner
                )
            """;

    // ---- the tokens ---------------------------------------------------------
    private static final String CARRIER_DECL_PRE = "final MapperS<Sched> thenArg";
    private static final String CARRIER_DECL_POST = "final MapperS<ReferenceWithMetaSched> thenArg";
    private static final String LIFT_PREDECL = "final Sched sched = metaInnerRule.evaluate(input);";
    private static final String BARE_IMPORT = "import census.seat15.dep.Sched;";

    // =========================================================================
    // Part A — the seat fires
    // =========================================================================

    /**
     * a1 — the argument-position decl at the k==0 BASE takes the inner rule's meta WRAPPER, not
     * the bare value. Scoped to {@code thenArg0} so its domain is disjoint from a4's (the
     * element-preserving filter level): the two arms are independent and their pins should fail
     * independently.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_metaInnerRuleInArgumentPosition_typesTheThenArgAtTheWrapper() throws IOException {
        String out = filtered("A1CarrierRule.java");
        assertContains(out, CARRIER_DECL_POST + "0");
        assertNotContains(out, CARRIER_DECL_PRE + "0");
    }

    /** a2 — the bare {@code evaluate()} value is hoisted into its own local and lifted. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_theBareValueIsHoistedAndLiftedThroughTheNullGuardTernary() throws IOException {
        String out = filtered("A1CarrierRule.java");
        assertContains(out, LIFT_PREDECL);
        assertContains(out, "sched == null ? MapperS.<ReferenceWithMetaSched>ofNull()"
                + " : MapperS.of(ReferenceWithMetaSched.builder().setValue(sched).build())");
    }

    /**
     * a3 — the LINE ORDER: the hoisted bare-value local must precede the decl that reads it
     * (golden's order; the FER twin appends {@code ruleWrapPreDecl} ahead of the decl at
     * {@code FER:7097-7099}). Registration order is drain order, so this pins the registration.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a3_theHoistedLocalPrecedesTheDeclThatReadsIt() throws IOException {
        String out = normalize(filtered("A1CarrierRule.java"));
        int hoist = out.indexOf(LIFT_PREDECL);
        int decl = out.indexOf(CARRIER_DECL_POST);
        assertTrue(hoist >= 0, "the hoisted bare-value local is missing:\n" + out);
        assertTrue(decl >= 0, "the wrapper-typed decl is missing:\n" + out);
        assertTrue(hoist < decl,
                "the hoisted local must be declared BEFORE the decl that reads it — a Java local"
                + " cannot be read above its declaration. hoist@" + hoist + " decl@" + decl
                + "\n" + out);
    }

    /**
     * a4 — the SINGLE-receiver element-preserve half (facet {@code singleFilterElementPreserve}).
     * The {@code then filter} level is element-preserving, so its decl must inherit the wrapper
     * the base level was re-typed to. Without this the two levels disagree — level 0 wrapper,
     * level 1 bare — which is the same defect one level up.
     *
     * <p>RED-CAPABLE PROVEN BY MUTATION: restoring #362's {@code isMapperC}-only gate fails this
     * test and only this test.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a4_theSingleFilterLevelInheritsTheReTypedElement() throws IOException {
        String out = normalize(filtered("A1CarrierRule.java"));
        long wrapperDecls = out.lines().filter(l -> l.contains(CARRIER_DECL_POST)).count();
        assertEquals(2, wrapperDecls,
                "BOTH the base level and the element-preserving filter level must declare the"
                + " wrapper; a single hit means the filter level recomputed from the meta-blind"
                + " snapshot.\n" + out);
        assertNotContains(out, CARRIER_DECL_PRE);
    }

    /** a5 — the BARE value type keeps its import: the pre-decl renders {@code final Sched …}. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a5_theBareValueTypeImportSurvivesTheReTyping() throws IOException {
        String out = filtered("A1CarrierRule.java");
        assertContains(out, BARE_IMPORT);
    }

    // =========================================================================
    // Part B — GREEN controls (the seat must NOT fire)
    // =========================================================================

    /** b1 — a NON-meta inner rule keeps the bare decl (the meta-recovery gate discriminates). */
    @Test
    @EnabledIf("builtinsAvailable")
    void b1_nonMetaInnerRule_keepsTheBareDecl() throws IOException {
        String out = filtered("B1BareInnerRule.java");
        assertContains(out, CARRIER_DECL_PRE);
        assertNotContains(out, "ReferenceWithMetaSched> thenArg");
    }

    /**
     * a6 — the recovery is driven by the INNER RULE's own output type, not by a fixed one: the
     * same shape over a wrapper on a DIFFERENT value type lifts to THAT wrapper.
     *
     * <p>(This began life as a decline control and was wrong: the recovered wrapper's value type
     * and the element the decl computed AGREE here, so the gate correctly fires. Stated as what
     * it actually proves rather than left as a control that passes for the wrong reason — the
     * #585 vacuous-control lesson.)
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a6_recoveryFollowsTheInnerRulesOwnOutputType() throws IOException {
        String out = filtered("B2OtherValueTypeRule.java");
        assertContains(out, "final Other other = otherInnerRule.evaluate(input);");
        assertContains(out, "final MapperS<ReferenceWithMetaOther> thenArg0");
        assertNotContains(out, "final MapperS<Other> thenArg0");
    }

    /**
     * b3 — a bare FUNCTION at the base is excluded by the {@code RRule} gate.
     *
     * <p><b>The first two assertions are the decline; the ANTI-VACUITY assertions are what make
     * them mean anything.</b> As originally written this test was purely negative, so a fixture
     * that never reached the deep-then decl seat at all — a rename, a parse change, a gate moved
     * upstream — would have passed it identically. The positive residue below pins that the
     * fixture really does render the shape the arm declines: the function IS invoked, and the
     * decl seat DID run and produced the BARE-typed local. (The #585 vacuous-control lesson,
     * applied to my own suite for the third time in this PR — see {@code a6} and {@code b5}.)
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b3_bareFunctionBase_declines() throws IOException {
        String out = filtered("B3FunctionBaseRule.java");
        // anti-vacuity: the fixture reaches the seat
        assertContains(out, "makeSched.evaluate(");
        assertContains(out, CARRIER_DECL_PRE);
        // the decline itself
        assertNotContains(out, "final Sched sched = makeSched.evaluate(input);");
        assertNotContains(out, "MapperS.of(ReferenceWithMetaSched.builder().setValue(sched)");
    }

    /**
     * b5 — the multi-namespace lift pin. <b>It is NOT the namespace-exactness pin an earlier
     * revision of this javadoc claimed it was; that claim was FALSE and is withdrawn.</b>
     *
     * <p>What it does pin, and what it does not:
     *
     * <p><b>PINS (real).</b> The workspace declares {@code Sched} TWICE —
     * {@code census.seat15.dep.Sched} and {@code census.seat15.other.Sched} — with the second
     * namespace registered FIRST. In a workspace with a duplicate simple name the lift still
     * binds, and imports, the type the rule ACTUALLY returns
     * ({@code census.seat15.other.Sched}), asserted on the emitted import.
     *
     * <p><b>DOES NOT PIN, PROVEN BY MUTATION (PR #586, the independent review's NIT 2).</b>
     * Replacing the canonical value-type comparison at the arm's gate —
     * {@code argRuleMeta.getValueType().equals(itemType)} — with a SIMPLE-NAME comparison leaves
     * this suite at <b>16/16 green</b>. So the gate's canonicality is unpinned, and b5 does not
     * catch it. The reason is a defect in this fixture, stated so the next seat does not repeat
     * it: {@code DecoyHolder.sch} is declared as {@code census.seat15.other.Sched}, which is BOTH
     * the namespace registered first AND the one the rule genuinely returns — so the correct
     * (canonical) answer and the degraded (simple-name, first-registered) answer are the SAME
     * STRING, and no assertion over this fixture can separate them. The earlier revision's own
     * reasoning contained that contradiction in one sentence.
     *
     * <p><b>What a real pin needs</b> — banked as a seat-16 item rather than guessed at, because
     * two controls in this very suite were already shipped mis-designed: a level whose computed
     * element and whose recovered wrapper VALUE type share a simple name but differ in namespace,
     * so the canonical comparison DECLINES where a simple-name comparison would FIRE. The
     * observable is then the bare decl surviving. That is a decline control, which is what b5
     * originally tried to be; the rewrite dropped the discriminating case instead of repairing
     * the fixture, and this note records that rather than leaving the claim standing.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b5_duplicateSimpleNameBindsTheTypeTheRuleActuallyReturns() throws IOException {
        String out = filtered("B5DecoyNamespaceRule.java");
        assertContains(out, "final Sched sched = decoyInnerRule.evaluate(input);");
        assertContains(out, "import census.seat15.other.Sched;");
        assertNotContains(out, "import census.seat15.dep.Sched;");
    }

    // =========================================================================
    // Part C — corpus locks (drr 7.0.0)
    // =========================================================================

    private static final Path DRR7_CELL_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path DRR7_GOLDEN_DIR =
            DRR7_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    private static final Path DRR561_CELL_ROOT = Path.of("../test-corpus/drr/drr-5.61.0");
    private static final Path DRR561_GOLDEN_DIR =
            DRR561_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    static boolean drr561Available() {
        return Files.isDirectory(DRR561_CELL_ROOT) && Files.isDirectory(DRR561_GOLDEN_DIR);
    }

    static boolean drr7Available() {
        return Drr7Corpus.gate(Files.isDirectory(DRR7_CELL_ROOT) && Files.isDirectory(DRR7_GOLDEN_DIR), ArgumentPositionRuleWrapSeatTest.class);
    }

    private static final String CARRIER =
            "drr/standards/iosco/cde/version1/quantity/reports/NotionalQuantityScheduleRule.java";

    /**
     * Control 1 — GREEN-SAFETY, asserted as the PREDICATE rather than as a token's absence.
     *
     * <p>The #585 REFUTATION: that seat's Control 1 claimed "no golden carries the pre-fix form"
     * after scanning eight files in ONE cell, when the token was carried by 66 goldens across 20
     * cells. A bare {@code final MapperS<X> thenArg} declaration is ORDINARY. What this seat
     * relies on is narrower and checkable: at the ONE site in this cell where the gate fires, the
     * interior consumers deref {@code .getValue()} off the declared local, so the bare-typed decl
     * could never have compiled. This asserts exactly that, on the golden.
     */
    @Test
    @EnabledIf("drr7Available")
    void corpus_control1_theGateFiresOnlyWhereGoldenDerefsTheWrapper() throws IOException {
        String golden = Files.readString(DRR7_GOLDEN_DIR.resolve(CARRIER));
        assertTrue(golden.contains(
                        "final MapperS<ReferenceWithMetaNonNegativeQuantitySchedule> thenArg0"),
                "golden no longer declares the wrapper at the carrier's base level");
        assertTrue(golden.contains("\"Type coercion\", referenceWithMetaNonNegativeQuantitySchedule"
                        + " -> referenceWithMetaNonNegativeQuantitySchedule == null ? null"
                        + " : referenceWithMetaNonNegativeQuantitySchedule.getValue()"),
                "golden no longer derefs .getValue() off the declared local — the property that"
                + " makes the bare-typed decl non-compiling, and therefore makes this arm"
                + " green-safe, no longer holds. Re-derive the safety argument.");
    }

    /**
     * Control 2 — the carrier was pinned SUB-FILE at its POST-seat-15 form while the two remaining
     * diff-lines were the banked terminal-cardinality defect (seat-16 item #0). <b>That defect
     * LANDED at seat 29</b> (law 4, {@code setTerminalReparentedRuleMulti} — this control's residue
     * assert fired as a heal tripwire, the LAW-81 flow), so the carrier is now a WHOLE-FILE lock:
     * the four seat consequences stay pinned as mechanism witnesses, and the file must be
     * byte-identical to golden.
     */
    @Test
    @EnabledIf("drr7Available")
    void corpus_control2_carrierIsWholeFileHealed() throws IOException {
        assertNotNull(drr7Output, "drr 7.0.0 generation did not run — corpus unavailable?");
        String generated = drr7Output.get(CARRIER);
        assertNotNull(generated, "not generated: " + CARRIER);
        // the seat's four consequences
        assertTrue(generated.contains(
                        "final NonNegativeQuantitySchedule nonNegativeQuantitySchedule"
                        + " = quantityScheduleRule.evaluate(input);"),
                "the hoisted bare-value local is missing");
        assertTrue(generated.contains(
                        "final MapperS<ReferenceWithMetaNonNegativeQuantitySchedule> thenArg0"),
                "thenArg0 is not typed at the wrapper");
        assertTrue(generated.contains(
                        "final MapperS<ReferenceWithMetaNonNegativeQuantitySchedule> thenArg1"),
                "thenArg1 did not inherit the re-typed element through the filter"
                + " (facet singleFilterElementPreserve)");
        assertTrue(generated.contains(
                        "import cdm.base.math.metafields.ReferenceWithMetaNonNegativeQuantitySchedule;"),
                "the wrapper's import is missing");
        // the whole-file lock (seat 29: law 4 landed seat-16 item #0)
        String golden = Files.readString(DRR7_GOLDEN_DIR.resolve(CARRIER));
        assertEquals(normalize(golden), normalize(generated),
                "the carrier is a WHOLE-FILE lock since seat 29 (law 4,"
                + " setTerminalReparentedRuleMulti, landed seat-16 item #0) — any divergence from"
                + " golden is a regression.");
    }

    /**
     * Control 3 — a stays-identical witness. {@code NotionalAmountScheduleRule} consumes the SAME
     * inner rule but through the FER seat (its chain is the rule's own body, not an argument), so
     * PR #585 already healed it and this seat must leave it exactly where it is.
     */
    @Test
    @EnabledIf("drr7Available")
    void corpus_control3_theFerSeatSiblingStaysByteIdentical() throws IOException {
        assertNotNull(drr7Output, "drr 7.0.0 generation did not run — corpus unavailable?");
        String path = "drr/standards/iosco/cde/version1/quantity/reports/"
                + "NotionalAmountScheduleRule.java";
        String generated = drr7Output.get(path);
        assertNotNull(generated, "not generated: " + path);
        Path goldenPath = DRR7_GOLDEN_DIR.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "golden missing: " + goldenPath);
        assertEquals(normalize(Files.readString(goldenPath)), normalize(generated),
                "the FER-seat sibling moved — this seat reached a chain it does not own,"
                + " or #585's heal regressed: " + path);
    }

    // =========================================================================
    // Part D — the HEADLINE HEALS (drr 5.61.0), locked whole-file
    // =========================================================================

    /**
     * The four whole-file heals this seat's headline number is made of — arm 2 (the SINGLE half of
     * the element-preserving filter) taking {@code drr/5.61.0 FUNCTION} identical 802 → 806.
     *
     * <p><b>Why these locks exist, added at the independent review's insistence.</b> The seat
     * originally shipped its headline result with NO test lock at all, and nothing else in the
     * repo can see it: {@code drr/5.61.0} is <b>not a ring cell</b> (the five-cell parity ring is
     * cdm 5.38.0 / cdm 6.20.6 / drr 6.34.1 / iso20022 1.38.0 / rune-fpml 2.0.0), and it carries
     * <b>zero census rows</b> ({@code ladder-census/expected-summary.tsv} pins it
     * {@code 0 0 0 0 0 e3b0c442}, the empty-set digest), so {@code enforceBandGate} iterates
     * nothing there. The only instrument that could observe these four files was the 275-row
     * matrix digest — a COUNT, which cannot distinguish a heal from a substitution. These are
     * byte-identical to golden after the seat, so the standing whole-file pattern applies.
     */
    private static final List<String> HEAL_WHOLE_FILE_LOCKS = List.of(
            "drr/regulation/esma/emir/refit/trade/functions/EUEMIRNotionalAmountPeriodLeg1.java",
            "drr/regulation/esma/emir/refit/trade/functions/EUEMIRNotionalAmountPeriodLeg2.java",
            "drr/regulation/fca/ukemir/refit/trade/functions/UKEMIRNotionalAmountPeriodLeg1.java",
            "drr/regulation/fca/ukemir/refit/trade/functions/UKEMIRNotionalAmountPeriodLeg2.java");

    @Test @EnabledIf("drr561Available")
    void corpus_heal1_euEmirNotionalAmountPeriodLeg1() throws IOException { lock561(HEAL_WHOLE_FILE_LOCKS.get(0)); }

    @Test @EnabledIf("drr561Available")
    void corpus_heal2_euEmirNotionalAmountPeriodLeg2() throws IOException { lock561(HEAL_WHOLE_FILE_LOCKS.get(1)); }

    @Test @EnabledIf("drr561Available")
    void corpus_heal3_ukEmirNotionalAmountPeriodLeg1() throws IOException { lock561(HEAL_WHOLE_FILE_LOCKS.get(2)); }

    @Test @EnabledIf("drr561Available")
    void corpus_heal4_ukEmirNotionalAmountPeriodLeg2() throws IOException { lock561(HEAL_WHOLE_FILE_LOCKS.get(3)); }

    private static void lock561(String path) throws IOException {
        assertNotNull(drr561Output, "drr 5.61.0 generation did not run — corpus unavailable?");
        String generated = drr561Output.get(path);
        assertNotNull(generated, "not generated: " + path);
        Path goldenPath = DRR561_GOLDEN_DIR.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "golden missing: " + goldenPath);
        assertEquals(normalize(Files.readString(goldenPath)), normalize(generated),
                "generated drr 5.61.0 output must byte-match golden (newline-normalized) for "
                + path + " — seat 15 arm 2: a SINGLE filterSingleNullSafe level must inherit the"
                + " re-typed element rather than recompute it from the meta-blind snapshot.");
    }

    // =========================================================================
    // Harness
    // =========================================================================

    private static Map<String, String> drr7Output;
    private static Map<String, String> drr561Output;

    @BeforeAll
    static void generateDrr7() throws IOException {
        if (drr7Available()) {
            drr7Output = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", DRR7_CELL_ROOT));
        }
        if (drr561Available()) {
            drr561Output = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "5.61.0", DRR561_CELL_ROOT));
        }
    }

    /** The seat-14 cell harness, unchanged — one cell, generated under its own emission filter. */
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
        assertNoGenerationErrors(funcGen.generateWithErrors(output));
        return output;
    }

    private static RLinkingResult linking;
    private static RModel mainModel;
    private static Map<String, String> filteredOut;

    private static void link() throws IOException {
        if (linking == null) {
            RModel other = AstBuilder.buildFromString(MODEL_OTHER, "seat15-other.rosetta");
            RModel dep = AstBuilder.buildFromString(MODEL_DEP, "seat15-dep.rosetta");
            RModel main = AstBuilder.buildFromString(MODEL_MAIN, "seat15.rosetta");
            other.setVersion("0.0.0.test");
            dep.setVersion("0.0.0.test");
            main.setVersion("0.0.0.test");
            List<RModel> models = new ArrayList<>();
            models.add(other); // the decoy registered FIRST (b5)
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
            filteredOut = render(m -> "census.seat15".equals(m.namespace()));
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
            throw new AssertionError("[ArgumentPositionRuleWrapSeatTest] builtins parse failures: "
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
