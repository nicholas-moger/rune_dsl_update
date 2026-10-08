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
 * SEAT 23, law A1 — facet {@code aliasSwitchBareCaseNav}: <b>a BARE 1-name case result of an alias-body
 * choice-type {@code switch} is the NARROWED type's own attribute — on both halves of the alias</b>: its
 * SIGNATURE types as that attribute at the attribute's own cardinality, and its RENDER is the #365/#369
 * instanceof RETURN ladder whose arm re-roots the name on the bound cast local. Golden cdm 6.21.0
 * {@code MapFloatingRateMultiplerScheduleToPriceWithLocation}: {@code protected MapperS<? extends Schedule>
 * floatingRateMultiplierSchedule(…)} with {@code return MapperS.of(floatingRateCalculation).<Schedule>map(
 * "getFloatingRateMultiplierSchedule", _floatingRateCalculation -> _floatingRateCalculation
 * .getFloatingRateMultiplierSchedule());} — and the multi-leaf twin {@code MapperC<? extends SpreadSchedule>
 * spreadSchedule(…)} ({@code .mapC}, {@code MapperC.<SpreadSchedule>ofNull()}).
 *
 * <p><b>The defect — a VERSION DIFF.</b> cdm 6.20.6 reads {@code floatingRateModel -> floatingRateMultiplierSchedule}
 * (the 2-name disguised chain the #369 {@code caseNarrowedNavTypeOrNull} admits, green today); CDM 6.21.0
 * flattened the intermediate out of the FpML model and the SAME source navigation became a bare symbol the
 * linker binds to {@code FloatingRateCalculation}'s own attribute. The join declined it, the ladder declined,
 * the alias fell to the type-keyed ternary stub and REFUSED ({@code TYPE_SWITCH_TERNARY_STUB}): 14 missing
 * outputs — the two functions × cdm 6.21.0/6.22.0/6.23.0 + drr 7.0–7.3 (the drr cells build the transitive CDM).
 *
 * <p><b>The seat — three reads, one law.</b> (1) {@code FunctionAliasHelper.caseNarrowedNavTypeOrNull} admits
 * the bare {@code RSymbolReference} (no args, never a function): the case type's attribute BY NAME through the
 * same guard resolution as the 2-name arm, IDENTITY-guarded against the linker's binding (the #590 law), typed
 * by {@code resolveAttributeTypeInfo} at the leaf's own cardinality. (2) The parser's alias self-scope filter
 * (facet {@code aliasSelfScopeFilter} #453 — a ShortcutDeclaration removes its OWN NAME from its parent scope,
 * so inside {@code alias x: … x …} the bare {@code x} falls through to the implicit item's feature) is consulted
 * by the generator's scope test: {@code ReferenceHandler.nameResolvesInFunctionScope(site, func, name)} answers
 * false for the enclosing alias's own name at every bare-name / chain-head synthesis arm (LAW 69 — the render's
 * decline mirrors the linker's binding; the 2-name item-chain arm had carried the exemption locally). Without it
 * the carriers — aliases named AS the attribute they read — rendered the bare name as a non-existent variable.
 * (3) The alias ladder's {@code compileSwitchLadderValueOrNull} splices the bare-nav arm RAW through the ONE
 * shape predicate the #221 SET twin consults ({@code ChoiceSwitchSupport.isCaseNarrowedNavShapeArm} — LAW 69);
 * a {@code MapperS.of} re-wrap would double-wrap the chain.
 *
 * <p><b>LAW 75 — measured before the seat over all 275 matrix rows, BOTH routes (the seat-23 runtime probe):</b>
 * of 1,549 probe LINES at the alias-switch join seat, 28 are bare non-function symbols — 14 of the carrier
 * class's 21 members (the Multipler's case and the Spread's FloatingRateCalculation case in each of 7 cells, two
 * lines each; sym=RAttribute, identity=true at every recorded line; the Spread's InflationRateCalculation case
 * never surfaced at the instrumented line and is covered by c2/c3 + control0's two-arm pin). The alias-own-name
 * scope class fired at exactly those 14 sites at the THREE probed callers of
 * {@code ReferenceHandler#nameResolvesInFunctionScope(RNode, RFunction, String)}; the other two callers are
 * covered by construction and by the full-matrix receipt (see that javadoc). Green-safe by construction besides:
 * every pre-seat carrier was a refusal.
 *
 * <p><b>RED at the pre-seat blob</b> ({@code rune-java-generator/src/main} at {@code fd8093b2} — this suite kept):
 * a1–a3, {@code corpus_c1}/{@code c2}/{@code c3}, {@code corpus_control1}/{@code control3} (the carriers are
 * refusals, so the fork's tree lacks them — named by the controls, never skipped); under {@code -Pir-on} also
 * {@code a4}; b1–b4 + {@code control0} + {@code control2} (the route twin) GREEN in both states.
 * <b>LAW 66/76 mutations</b> (each applied → run → reverted at the FINAL head, LAW 78): (i) the signature arm
 * deleted → a1–a3 + c1–c3 + control1/control3 (the refusal returns; MEASURED 8F); (ii) the scope exemption
 * reverted → a1, a2 + c1–c3, a3 stays GREEN (the isolating witness: its alias is NOT named as the attribute, so
 * only the signature half carries it; MEASURED 5F); (iii) the raw-splice admission reverted → a1–a3 + c1–c3 (the
 * double wrap; MEASURED 6F). Under (ii) and (iii) the whole-cell controls do NOT move, by construction: they
 * count the ladders' {@code instanceof} ARMS, which the render half and the splice neither add nor remove —
 * the arm's TEXT is what moves, and the c-locks (whole-file byte identity) are its witnesses;
 * (iv) the identity guard dropped → b4 (the bare case name the linker binds to an INPUT that merely SHARES the
 * case attribute's name: the guard declines, the alias keeps refusing; without it the join would type the arm as
 * the case attribute while the render half resolves the input — the two halves disagreeing; MEASURED 1F {b4}). No
 * CORPUS case moves
 * under (iv) (MEASURED: every reached carrier is identity=true), so b4 is a declared fixture pin — the first
 * draft of this javadoc wrongly called the guard un-witnessable; the parser's alias self-scope filter empties
 * ONLY the enclosing alias's own name, every other binding survives, which is exactly what b4 exercises.
 */
class AliasSwitchBareCaseNavSeatTest {

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

    static boolean cdm621AndIrProviderAvailable() {
        return cdm621Available() && irProviderOnClasspath();
    }

    /**
     * An extends-based subject type with two subtypes, a single-leaf and a multi-leaf attribute on the narrowed
     * type, and one function per shape: the Multipler shape (alias named AS the single leaf), the Spread shape
     * (two cases, a multi leaf), the signature half alone (alias named differently), the #369 2-name case, the
     * #365 bare-fn case.
     */
    private static final String MODEL = """
            namespace census.seat23a1
            version "1.0.0"

            type Sched:
                value number (0..1)

            type Spread:
                value number (0..1)

            type FloatModel:
                sched Sched (0..1)

            type Rate:
                name string (0..1)

            type FloatCalc extends Rate:
                multiplierSchedule Sched (0..1)
                spreadSchedule Spread (0..*)

            type InflCalc extends Rate:
                spreadSchedule Spread (0..*)

            type ModelCalc extends Rate:
                model FloatModel (0..1)

            type Price:
                amount number (0..1)

            func MapSched: <"MapScheduleToInterestRatePriceSchedule twin">
                inputs:
                    s Sched (0..1)
                output:
                    p Price (0..1)
                set p:
                    Price { amount: s -> value }

            func MapSpread: <"the multi-leaf consumer">
                inputs:
                    s Spread (0..1)
                output:
                    p Price (0..1)
                set p:
                    Price { amount: s -> value }

            func SchedOf: <"b2 - a bare fn reference as a case result (the #365/#382 class)">
                inputs:
                    c FloatCalc (0..1)
                output:
                    s Sched (0..1)
                set s:
                    c -> multiplierSchedule

            func A1Multipler: <"a1 - THE MapFloatingRateMultiplerScheduleToPriceWithLocation SHAPE: an alias named AS the single-leaf attribute it reads in a bare 1-name case">
                inputs:
                    rate Rate (0..1)
                output:
                    price Price (0..*)
                alias multiplierSchedule:
                    rate switch
                        FloatCalc then multiplierSchedule,
                        default empty
                add price:
                    multiplierSchedule
                        extract MapSched(item)

            func A2Spread: <"a2 - THE MapSpreadScheduleToPriceWithLocation SHAPE: two cases reading a MULTI leaf, the alias named as the attribute">
                inputs:
                    rate Rate (0..1)
                output:
                    price Price (0..*)
                alias spreadSchedule:
                    rate switch
                        FloatCalc then spreadSchedule,
                        InflCalc then spreadSchedule,
                        default empty
                add price:
                    spreadSchedule
                        extract MapSpread(item)

            func A3AliasNamedDifferently: <"a3 - the signature half alone: the alias is NOT named as the attribute, so only the join's bare-case admission carries it">
                inputs:
                    rate Rate (0..1)
                output:
                    price Price (0..*)
                alias sched:
                    rate switch
                        FloatCalc then multiplierSchedule,
                        default empty
                add price:
                    sched
                        extract MapSched(item)

            func B1TwoNameCase: <"b1 - the #369 2-name disguised-chain case keeps its bytes">
                inputs:
                    rate Rate (0..1)
                output:
                    price Price (0..*)
                alias sched2:
                    rate switch
                        ModelCalc then model -> sched,
                        default empty
                add price:
                    sched2
                        extract MapSched(item)

            func B2FnRefCase: <"b2 - the #365 bare-fn case keeps its bytes">
                inputs:
                    rate Rate (0..1)
                output:
                    price Price (0..*)
                alias viaFn:
                    rate switch
                        FloatCalc then SchedOf,
                        default empty
                add price:
                    viaFn
                        extract MapSched(item)
            """;

    /**
     * b3 — a bare name that is NOT an attribute of the case type (here the subject input itself): the join
     * declines, the ladder declines, and the alias still REFUSES (today's bytes) — so it lives in its own
     * namespace, rendered with the errors collected rather than thrown.
     */
    private static final String MODEL_NEG = """
            namespace census.seat23a1neg
            version "1.0.0"

            import census.seat23a1.*

            func B3NotACaseAttribute: <"b3 - a bare name that is an INPUT, not the case type's attribute, still refuses">
                inputs:
                    rate Rate (0..1)
                output:
                    price Price (0..*)
                alias bad:
                    rate switch
                        FloatCalc then rate,
                        default empty
                add price:
                    bad
                        extract Price { amount: 1 }
            """;

    /**
     * b4 — the IDENTITY guard: a bare case name the linker binds to an INPUT that merely shares the case
     * attribute's name (the alias is NOT named as the attribute, so the #453 self-scope filter leaves the
     * binding alone) declines at the join — the alias still REFUSES (today's bytes). Its own namespace, so its
     * error list is its own.
     */
    private static final String MODEL_NEG2 = """
            namespace census.seat23a1neg2
            version "1.0.0"

            import census.seat23a1.*

            func B4InputNamedAsTheCaseAttribute: <"b4 - an INPUT named as the case attribute: the linker binds the bare case name to the input, the identity guard declines">
                inputs:
                    rate Rate (0..1)
                    multiplierSchedule Sched (0..1)
                output:
                    price Price (0..*)
                alias sched:
                    rate switch
                        FloatCalc then multiplierSchedule,
                        default empty
                add price:
                    sched
                        extract Price { amount: 1 }
            """;

    // =========================================================================
    // Part A — the seat (RED at the pre-seat blob)
    // =========================================================================

    /** a1 — the Multipler shape: signature MapperS<? extends Sched>, the arm re-rooted on the cast local. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_aliasNamedAsSingleLeafAttribute() throws IOException {
        String out = function("A1Multipler.java");
        assertContains(out, "protected abstract MapperS<? extends Sched> multiplierSchedule(Rate rate);");
        assertContains(out, "if (rate == null) {\n\t\t\t\treturn MapperS.<Sched>ofNull();");
        assertContains(out, "if (rate instanceof FloatCalc) {\n\t\t\t\tfinal FloatCalc floatCalc = (FloatCalc) rate;");
        assertContains(out, "return MapperS.of(floatCalc).<Sched>map(\"getMultiplierSchedule\", _floatCalc -> _floatCalc.getMultiplierSchedule());");
        assertNotContains(out, "MapperS.of(MapperS.of(floatCalc)");
        assertNotContains(out, "MapperS.of(multiplierSchedule)");
    }

    /** a2 — the Spread shape: two cases, a MULTI leaf — MapperC signature, .mapC arms, MapperC.<Spread>ofNull() terminals. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_aliasNamedAsMultiLeafAttributeTwoCases() throws IOException {
        String out = function("A2Spread.java");
        assertContains(out, "protected abstract MapperC<? extends Spread> spreadSchedule(Rate rate);");
        assertContains(out, "return MapperC.<Spread>ofNull();");
        assertContains(out, "return MapperS.of(floatCalc).<Spread>mapC(\"getSpreadSchedule\", _floatCalc -> _floatCalc.getSpreadSchedule());");
        assertContains(out, "return MapperS.of(inflCalc).<Spread>mapC(\"getSpreadSchedule\", _inflCalc -> _inflCalc.getSpreadSchedule());");
        assertNotContains(out, "MapperS.of(spreadSchedule)");
    }

    /** a3 — the signature half alone: an alias NOT named as the attribute (the isolating witness for mutation ii). */
    @Test
    @EnabledIf("builtinsAvailable")
    void a3_aliasNamedDifferentlyFromTheAttribute() throws IOException {
        String out = function("A3AliasNamedDifferently.java");
        assertContains(out, "protected abstract MapperS<? extends Sched> sched(Rate rate);");
        assertContains(out, "return MapperS.of(floatCalc).<Sched>map(\"getMultiplierSchedule\", _floatCalc -> _floatCalc.getMultiplierSchedule());");
        assertNotContains(out, "MapperS.of(multiplierSchedule)");
    }

    /**
     * a4 — LAW 77: the a1 shape rendered through the REAL {@code IRGeneration.functionGenerator} seam
     * ({@code -Pir-on}); the alias ladder and the join are the shared legacy seats, so the bytes agree.
     */
    @Test
    @EnabledIf("builtinsAndIrProviderAvailable")
    void a4_aliasNamedAsAttributeOnIrRoute() throws IOException {
        String out = lookup(fixtureOnIrRoute(), "functions/A1Multipler.java");
        assertContains(out, "protected abstract MapperS<? extends Sched> multiplierSchedule(Rate rate);");
        assertContains(out, "return MapperS.of(floatCalc).<Sched>map(\"getMultiplierSchedule\", _floatCalc -> _floatCalc.getMultiplierSchedule());");
    }

    // =========================================================================
    // Part B — placement pins (GREEN in BOTH states)
    // =========================================================================

    /** b1 — the #369 2-name disguised-chain case keeps its bytes. */
    @Test
    @EnabledIf("builtinsAvailable")
    void b1_twoNameCaseUnchanged() throws IOException {
        String out = function("B1TwoNameCase.java");
        assertContains(out, "protected abstract MapperS<? extends Sched> sched2(Rate rate);");
        assertContains(out, "return MapperS.of(modelCalc).<FloatModel>map(\"getModel\", _modelCalc -> _modelCalc.getModel()).<Sched>map(\"getSched\", floatModel -> floatModel.getSched());");
    }

    /** b2 — the #365 bare-fn case keeps its bytes: the implicit invocation on the cast local. */
    @Test
    @EnabledIf("builtinsAvailable")
    void b2_bareFnRefCaseUnchanged() throws IOException {
        String out = function("B2FnRefCase.java");
        assertContains(out, "protected abstract MapperS<? extends Sched> viaFn(Rate rate);");
        assertContains(out, "return MapperS.of(schedOf.evaluate(floatCalc));");
    }

    /** b3 — a bare name that is NOT the case type's attribute (an input) declines: the alias still refuses (today's bytes). */
    @Test
    @EnabledIf("builtinsAvailable")
    void b3_bareNameThatIsNotACaseAttributeStillRefuses() throws IOException {
        List<String> errors = negativeFixtureErrors();
        assertFalse(errors.isEmpty(), "B3NotACaseAttribute must still refuse (the join declines a non-attribute)");
        // The negative namespace holds exactly one function, so its error list is B3's alone; the
        // refusal's text names the seat (the C0 TYPE_SWITCH_TERNARY_STUB message).
        assertTrue(errors.stream().anyMatch(e -> e.contains("type-keyed switch case")),
                "the refusal must be the type-keyed switch stub for B3NotACaseAttribute: " + errors);
    }

    /**
     * b4 — the identity guard declines a bare case name bound to a same-named INPUT: the alias still refuses.
     * The isolating witness of mutation (iv): with the guard dropped the join admits the case attribute and the
     * alias RENDERS (no refusal) against a render half that resolves the input.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b4_inputNamedAsTheCaseAttributeDeclinesAtTheIdentityGuard() throws IOException {
        List<String> errors = negativeFixture2Errors();
        assertFalse(errors.isEmpty(),
                "B4InputNamedAsTheCaseAttribute must still refuse (the identity guard declines a foreign binding)");
        assertTrue(errors.stream().anyMatch(e -> e.contains("type-keyed switch case")),
                "the refusal must be the type-keyed switch stub for B4InputNamedAsTheCaseAttribute: " + errors);
    }

    // =========================================================================
    // Part C — the corpus (cdm 6.21.0 the charter cell; drr 7.0.0 the transitive twin — LAW 79)
    // =========================================================================

    private static final Path CDM621_CELL_ROOT = Path.of("../test-corpus/cdm/cdm-6.21.0");
    private static final Path CDM621_GOLDEN = CDM621_CELL_ROOT.resolve("rosetta-source/src/generated/java");
    private static final Path DRR7_CELL_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path DRR7_GOLDEN = DRR7_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    static boolean cdm621Available() {
        return Files.isDirectory(CDM621_GOLDEN);
    }

    static boolean drr7Available() {
        return Drr7Corpus.gate(Files.isDirectory(DRR7_GOLDEN), AliasSwitchBareCaseNavSeatTest.class);
    }

    private static final String MULTIPLER =
            "cdm/ingest/fpml/confirmation/pricequantity/functions/MapFloatingRateMultiplerScheduleToPriceWithLocation.java";
    private static final String SPREAD =
            "cdm/ingest/fpml/confirmation/pricequantity/functions/MapSpreadScheduleToPriceWithLocation.java";

    /** c1 — cdm 6.21.0 MapFloatingRateMultiplerScheduleToPriceWithLocation whole-file lock. */
    @Test
    @EnabledIf("cdm621Available")
    void corpus_c1_cdm621MultiplerByteIdentical() throws IOException {
        lock(cdm621Output, cdm621GenErrors, CDM621_GOLDEN, "cdm 6.21.0", MULTIPLER);
    }

    /** c2 — cdm 6.21.0 MapSpreadScheduleToPriceWithLocation whole-file lock (the multi leaf, two cases). */
    @Test
    @EnabledIf("cdm621Available")
    void corpus_c2_cdm621SpreadByteIdentical() throws IOException {
        lock(cdm621Output, cdm621GenErrors, CDM621_GOLDEN, "cdm 6.21.0", SPREAD);
    }

    /**
     * c3 — the drr 7.0.0 transitive-CDM twins byte-identical (LAW 79: the mechanism reaches every cell that
     * builds this CDM), and the sibling cells' goldens (cdm 6.22.0/6.23.0, drr 7.1.0–7.3.0) are identical to
     * these — a PRECONDITION for the other ten heals (the #589 R2 correction: it never RUNS those cells; the
     * full-matrix ring attests them). An absent sibling FAILS rather than skipping (the #588 MF-3 rule).
     */
    @Test
    @EnabledIf("drr7Available")
    void corpus_c3_drr7TwinsByteIdenticalAndSiblingGoldensIdentical() throws IOException {
        lock(drr7Output, drr7GenErrors, DRR7_GOLDEN, "drr 7.0.0", MULTIPLER);
        lock(drr7Output, drr7GenErrors, DRR7_GOLDEN, "drr 7.0.0", SPREAD);
        List<Path> siblings = List.of(
                Path.of("../test-corpus/cdm/cdm-6.22.0"),
                Path.of("../test-corpus/cdm/cdm-6.23.0"),
                Path.of("../test-corpus/drr/drr-7.1.0"),
                Path.of("../test-corpus/drr/drr-7.2.0"),
                Path.of("../test-corpus/drr/drr-7.3.0"));
        for (String rel : List.of(MULTIPLER, SPREAD)) {
            String expected = normalize(Files.readString(DRR7_GOLDEN.resolve(rel)));
            for (Path cell : siblings) {
                Path sib = cell.resolve("rosetta-source/src/generated/java").resolve(rel);
                assertTrue(Files.isRegularFile(sib), "sibling cell golden missing: " + sib
                        + " — that cell is in the frozen 9.83.0 manifest, so this checkout is incomplete");
                assertEquals(expected, normalize(Files.readString(sib)), "sibling golden diverged: " + sib);
            }
        }
    }

    /**
     * control0 — golden is the oracle (the frozen cdm 6.21.0 tree, every kind): the alias RETURN ladder's
     * token — a {@code if (<subject> instanceof <Type>) {} line (the early-return arm the alias ladder emits;
     * the assignment ladders emit {@code } else if (…)} and never match) — at its exact census: 19 files /
     * 79 sites (censused by the seat-23 charter's python walk); the carriers carry 1 and 2.
     */
    @Test
    @EnabledIf("cdm621Available")
    void corpus_control0_goldenCdm621IsTheOracle() throws IOException {
        Map<String, Integer> g = scanReturnLadderArms(readGoldenTree(CDM621_GOLDEN));
        assertEquals(19, g.size(), "golden cdm 6.21.0 alias-ladder files (the whole-cell control's domain)");
        assertEquals(79, g.values().stream().mapToInt(Integer::intValue).sum(), "golden cdm 6.21.0 ladder arms");
        assertEquals(1, g.get(MULTIPLER), "the Multipler carrier's arms");
        assertEquals(2, g.get(SPREAD), "the Spread carrier's arms");
    }

    /**
     * control1 — the FORK's WHOLE generated cdm 6.21.0 cell (every kind, LAW 72): over the UNION of the files
     * either tree carries the token in (a missing side counts as zero, LAW 79), the per-file arm counts agree
     * FILE BY FILE; files golden carries that the fork does not emit at all — the pre-seat refusals — are named,
     * never silently skipped; the domain equals golden's 19.
     */
    @Test
    @EnabledIf("cdm621Available")
    void corpus_control1_forkCdm621WholeCellLadderArmsEqualGoldenFileByFile() throws IOException {
        assertNotNull(cdm621Output, "cdm 6.21.0 generation did not run");
        // FAIL-CLOSED: the cell is error-free since this seat, so ANY generation error means a file
        // is missing from the scan for an unknown reason and the whole-cell contract cannot be asserted.
        assertEquals(List.of(), cdm621GenErrors, "cdm 6.21.0 reported a generation error — the scan is incomplete");
        assertUnionEqual(scanReturnLadderArms(cdm621Output), scanReturnLadderArms(readGoldenTree(CDM621_GOLDEN)),
                cdm621Output.keySet(), "fork", "golden", 19, List.of());
    }

    /**
     * control2 — LAW 77 route parity: the whole cdm 6.21.0 cell generated through the REAL {@code IRGeneration}
     * seams ({@code -Pir-on}) carries the SAME per-file arm counts as the legacy-route render (the domain is the
     * legacy render's own).
     */
    @Test
    @EnabledIf("cdm621AndIrProviderAvailable")
    void corpus_control2_irRouteCdm621LadderArmsEqualLegacyRouteFileByFile() throws IOException {
        assertNotNull(cdm621Output, "cdm 6.21.0 legacy-route generation did not run");
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("cdm", "6.21.0", CDM621_CELL_ROOT), new ArrayList<>());
        assertUnionEqual(scanReturnLadderArms(irOut), scanReturnLadderArms(cdm621Output), irOut.keySet(),
                "ir", "legacy", scanReturnLadderArms(cdm621Output).size(), List.of());
    }

    /**
     * control3 — LAW 79: the mechanism reaches every cell that builds this CDM — the fork's WHOLE generated
     * drr 7.0.0 cell vs its golden, the same token, the same UNION discipline; golden's domain there is the
     * same 19 files / 79 sites (the transitive CDM functions).
     */
    @Test
    @EnabledIf("drr7Available")
    void corpus_control3_forkDrr7WholeCellLadderArmsEqualGoldenFileByFile() throws IOException {
        assertNotNull(drr7Output, "drr 7.0.0 generation did not run");
        assertEquals(List.of(), drr7GenErrors, "drr 7.0.0 reported a generation error — the scan is incomplete");
        Map<String, Integer> g = scanReturnLadderArms(readGoldenTree(DRR7_GOLDEN));
        assertEquals(19, g.size(), "golden drr 7.0.0 alias-ladder files");
        assertEquals(79, g.values().stream().mapToInt(Integer::intValue).sum(), "golden drr 7.0.0 ladder arms");
        // The union domain is golden's 19 plus the named residue file the fork alone carries (golden=0).
        assertUnionEqual(scanReturnLadderArms(drr7Output), g, drr7Output.keySet(), "fork", "golden",
                19 + KNOWN_RESIDUE_DRR7.size(), KNOWN_RESIDUE_DRR7);
    }

    /**
     * The named PRE-EXISTING residue of ANOTHER family in drr 7.0.0 — the per-file differences between
     * the fork's whole cell and golden on this token AFTER the seat, pinned EXACTLY (LAW 73). <b>EMPTY
     * since seat 31</b>: iosco {@code CustomBasketCodeRule} (was {@code fork=4 golden=0}) was close-census
     * family F15(a) — the fork rendered its CHOICE switch as an {@code instanceof} ladder (four
     * {@code if (… instanceof …) {} arms) where golden probes the per-option getters and carries none,
     * the COMPILES-BUT-WRONG shape the census named as the most dangerous item in the residue — and
     * seat-31 law 4b ({@code choiceSwitchLambdaOptionGetter}) healed it WHOLE in drr 7.0.0, so the
     * fork's four arms are gone and the union domain is golden's 19 alone. RE-MEASURED at the seat-31
     * chain head {@code f2a4d5c0}: transcribed from control3's own failing print (LAW 81),
     * {@code but was: <[]>}. Any entry that ENTERS this list is a regression; a carrier may NEVER
     * appear here.
     */
    private static final List<String> KNOWN_RESIDUE_DRR7 = List.of();

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
    // The scan — the alias RETURN ladder's arms per file over CODE only (a line walk)
    // =========================================================================

    private static Map<String, Integer> scanReturnLadderArms(Map<String, String> tree) {
        Map<String, Integer> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : tree.entrySet()) {
            if (!e.getValue().contains(" instanceof ")) {
                continue;
            }
            int n = 0;
            for (String raw : codeOnly(e.getValue()).split("\n")) {
                String s = raw.strip();
                if (s.startsWith("if (") && s.contains(" instanceof ") && s.endsWith(") {")) {
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
    // Cell generation (cdm 6.21.0 + drr 7.0.0, the legacy route; the IR route for control2)
    // =========================================================================

    private static Map<String, String> cdm621Output;
    private static List<String> cdm621GenErrors;
    private static Map<String, String> drr7Output;
    private static List<String> drr7GenErrors;

    @BeforeAll
    static void generateCells() throws IOException {
        if (cdm621Available()) {
            List<String> errs = new ArrayList<>();
            cdm621Output = generateCell(
                    new D11CorpusRegressionTest.CellSpec("cdm", "6.21.0", CDM621_CELL_ROOT), errs);
            cdm621GenErrors = errs;
        }
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

    private static void lock(Map<String, String> output, List<String> genErrors, Path golden, String cellName,
            String path) throws IOException {
        assertNotNull(output, cellName + " generation did not run — corpus unavailable?");
        List<String> lockedErrors = genErrors.stream().filter(e -> e.contains(path)).toList();
        assertTrue(lockedErrors.isEmpty(),
                "the generator reported errors for the locked file " + path + ": " + lockedErrors);
        String generated = output.get(path);
        assertNotNull(generated, "not generated in " + cellName + ": " + path);
        Path goldenPath = golden.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "golden missing: " + goldenPath);
        assertEquals(normalize(Files.readString(goldenPath)), normalize(generated),
                "generated " + cellName + " output must byte-match golden (newline-normalized) for "
                + path + " — seat 23 A1: the bare 1-name alias-switch case.");
    }

    // =========================================================================
    // Fixture harness
    // =========================================================================

    private static RLinkingResult linking;
    private static RModel mainModel;
    private static RModel negModel;
    private static RModel negModel2;
    private static Map<String, String> fixtureOut;

    private static void link() throws IOException {
        if (linking == null) {
            RModel main = AstBuilder.buildFromString(MODEL, "seat23a1.rosetta");
            main.setVersion("0.0.0.test");
            RModel neg = AstBuilder.buildFromString(MODEL_NEG, "seat23a1neg.rosetta");
            neg.setVersion("0.0.0.test");
            RModel neg2 = AstBuilder.buildFromString(MODEL_NEG2, "seat23a1neg2.rosetta");
            neg2.setVersion("0.0.0.test");
            List<RModel> models = new ArrayList<>();
            models.add(main);
            models.add(neg);
            models.add(neg2);
            models.addAll(loadBuiltinsOnly());
            linking = RWorkspace.build(models);
            mainModel = main;
            negModel = neg;
            negModel2 = neg2;
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

    private static List<String> fixtureErrors;

    /**
     * The fixture's functions. A generation error is KEPT, not thrown: pre-seat the a-functions REFUSE
     * ({@code TYPE_SWITCH_TERNARY_STUB}) — that refusal IS the RED — so only the test that asks for a
     * refused file fails, naming the refusal; the b-tests' files render in both states.
     */
    private static Map<String, String> fixture() throws IOException {
        if (fixtureOut == null) {
            link();
            List<String> errors = new ArrayList<>();
            fixtureOut = render(m -> "census.seat23a1".equals(m.namespace()), mainModel, errors);
            fixtureErrors = errors;
        }
        return fixtureOut;
    }

    private static List<String> negativeFixtureErrors() throws IOException {
        link();
        List<String> errors = new ArrayList<>();
        render(m -> "census.seat23a1neg".equals(m.namespace()), negModel, errors);
        return errors;
    }

    private static List<String> negativeFixture2Errors() throws IOException {
        link();
        List<String> errors = new ArrayList<>();
        render(m -> "census.seat23a1neg2".equals(m.namespace()), negModel2, errors);
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
                        m -> "census.seat23a1".equals(m.namespace()));
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
            throw new AssertionError("[AliasSwitchBareCaseNavSeatTest] builtins parse"
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
