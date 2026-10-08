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
 * v3.2 seat 1 — the chaos census's family F2 "dropped-coercion" (96 declared D11 rows over five
 * seats, PR #621's first gate run). The witness suite for the fix: every sub-shape the census
 * measured is a reduced fixture here (the chaos seed reduced to the ONE construct that carries the
 * row), asserted against the legacy-9.83.0 golden text the chaos cell pins — and every declared
 * carrier file is byte-locked whole against its golden (the corpus lock, one test per class over
 * all twelve placement variants, because the shapes were measured placement-invariant: one diff
 * shape per class across the 12 variants on BOTH routes).
 *
 * <p>THE MEASURED SHAPES (the both-side dumps at the #621 merge head {@code b9a539672}, default
 * and IR routes byte-identical for seven of the eight classes; {@code C20Spread} differs between
 * routes and is NOT a coercion — re-priced out of this family, see the seat plan):
 * <ul>
 *   <li>{@code C8Scale} (s08, 12): an Integer-typed INPUT parameter (a {@code number(fractionalDigits:
 *       0)} alias) in a BigDecimal arithmetic join — upstream's item→wrapper conversion at the
 *       variable seat: {@code (q == null ? MapperS.<BigDecimal>ofNull() : MapperS.of(BigDecimal.valueOf(q)))};
 *       the fork emitted the bare {@code MapperS.of(q)}.</li>
 *   <li>{@code C18FromCoded} (s18, 12): a {@code switch} over a scheme-carrying string — the hoisted
 *       {@code final MapperS<String> switchArgument} takes the meta-unwrap
 *       {@code .<String>map("Type coercion", …)} rung; the fork hoisted the raw
 *       {@code MapperS<FieldWithMetaString>} chain into a {@code MapperS<String>} local.</li>
 *   <li>{@code C18Maybe} (s18, 12): an enum {@code switch} in the THEN arm of an elseless
 *       conditional at the output — upstream's statement ladder ({@code if (k == null) … else if
 *       (k == E.RED) … else …}); the fork fell to a non-compiling ternary.</li>
 *   <li>{@code C19Make} (s19, 12): a list literal of int literals at a {@code number (0..*)}
 *       constructor field — the {@code MapperC<Integer>→MapperC<BigDecimal>} map rung, UNGUARDED
 *       (a MapperC's items are non-null); the fork spliced the Integer list.</li>
 *   <li>{@code C19Pick} (s19, 12): a conditional whose ELSE is a literal at a constructor field —
 *       upstream's initializer form {@code String ifThenElseResult0 = "neg"; if (…) { … = "pos"; }};
 *       the fork emitted the {@code final} if/else form it uses for a non-literal else.</li>
 *   <li>{@code C20Shade} (s20, 12): an Integer-joined arithmetic whose right operand is a
 *       {@code then count} chain, assigned to a {@code number} output — the operand takes the
 *       {@code MapperS.of(thenArg.resultCount())} wrap and the assignment the null-guarded
 *       statement form {@code final Integer integer = …get(); if (integer == null) {…} else {n =
 *       BigDecimal.valueOf(integer);}}; the fork spliced the bare int and skipped the conversion.</li>
 *   <li>{@code C21Void} (s21, 12): {@code set n: many count} at a {@code number} output —
 *       {@code n = BigDecimal.valueOf(MapperC.<String>of(many).resultCount());} (a primitive int
 *       needs no guard); the fork emitted {@code .resultCount().get()}.</li>
 * </ul>
 *
 * <p>The ONE root under five of the seven: the fork's {@code count} render carried NO expression
 * type and every bare input-parameter reference carries none either, so the type-directed coercion
 * service ({@code TypeCoercionService}, the quadrant dispatch upstream's {@code addCoercions}
 * performs at every consumption seat) never saw a mismatch at these boundaries. The fix gives those
 * producers their Java type (the typed channel) and lets the consuming seats coerce; the two render
 * forms (the then-arm enum ladder, the literal-else initializer) are the seats' own statement-form
 * routes. The corpus laws: ZERO vendored goldens carry any of the four failing forms (the grep census
 * at the seat: the param form, the count-to-number assignment, the literal initializer, the
 * {@code .resultCount()).get()} splice — all 0 files), so no green file can move; the chain's 25-cell
 * filtered digests are the belt.
 *
 * <p>THE s1a CHAIN'S CATCH (PR #622): the item→wrapper operand arm as first written admitted every
 * item-typed operand and double-wrapped the #361 Mapper-form if-then-else sentinel (an item-stamped,
 * self-unwrapping {@code MapperS<X>} local) in golden cdm5/cdm6 {@code GenerateObservationDates} —
 * ten CDM cells on BOTH routes, the 25-cell digests moved, the #361 lock red. The arm is gated on the
 * PRIMITIVE stamp (the count's own) since; {@code c4} is the reduced witness and lane J its mutation.
 * The primitive promotion the seat added also moved {@code UpstreamTypeCoercionPortTest}'s first
 * item→item case onto upstream's own text ({@code return (long) 42;}) — a divergence note closed,
 * not a regression.
 */
class DroppedCoercionSeatTest {

    private static final Path REPO_ROOT =
            Path.of(System.getProperty("user.dir")).resolve("..").normalize();

    private static final List<Path> BUILTINS_SEARCH_ROOTS = List.of(
            REPO_ROOT.resolve("test-corpus/rune-dsl-builtins/rune-runtime/src/main/resources/model"),
            REPO_ROOT.resolve("rune-dsl/rune-runtime/src/main/resources/model"),
            REPO_ROOT.resolve("rune-runtime/src/main/resources/model"));

    static boolean builtinsAvailable() {
        return BUILTINS_SEARCH_ROOTS.stream().anyMatch(Files::isDirectory);
    }

    private static final Path CHAOS_ROOT = com.regnosys.rosetta.testutil.ChaosCell.root();
    private static final Path CHAOS_GOLDEN = CHAOS_ROOT.resolve("rosetta-source/src/generated/java");

    static boolean chaosAvailable() {
        return Files.isDirectory(CHAOS_GOLDEN)
                && Files.isDirectory(CHAOS_ROOT.resolve("rosetta-source/src/main/rosetta"));
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

    /** The twelve placement variants every chaos seat is expanded over (the a1/a2/a3 axes + base). */
    private static final List<String> VARIANTS = List.of(
            "a1o1", "a1o2", "a1o3", "a1o4", "a2alias", "a2dangle", "a2qual", "a2wild",
            "a3half/p2", "a3hub/p2", "a3third/p3", "base");

    // =========================================================================
    // The reduced fixtures — one construct per measured shape
    // =========================================================================

    private static final String MODEL = """
            namespace census.seat1f2
            version "0.0.0.test"

            typeAlias F2Positive: <"Integer-grained number alias (the s08 shape).">
                number(fractionalDigits: 0, min: 0)
            typeAlias F2Pct: <"Bounded percentage."> number(min: 0, max: 100)

            func F2Scale: <"C8Scale twin: an Integer-typed alias input in a BigDecimal join.">
                inputs:
                    q F2Positive (1..1)
                    w F2Pct (1..1)
                output:
                    r number (1..1)
                set r: q * w / 100

            func F2ScalePlain: <"The plain-int twin of F2Scale (the alias-vs-int relation).">
                inputs:
                    q int (1..1)
                    w number (1..1)
                output:
                    r number (1..1)
                set r: q * w / 100

            func F2Sum: <"control: an Integer input in an INTEGER join needs no conversion.">
                inputs:
                    q int (1..1)
                output:
                    r int (1..1)
                set r: q + 1

            metaType scheme string

            type F2Coded: <"The scheme-carrying switch subject (the s18 shape).">
                kind string (0..1)
                    [metadata scheme]

            func F2FromCoded: <"C18FromCoded twin: a switch over a scheme-carrying scalar.">
                inputs:
                    c F2Coded (1..1)
                output:
                    n number (1..1)
                set n:
                    c -> kind switch
                        "spot" then 1,
                        "fwd" then 2,
                        default 0

            enum F2KindEnum: <"Enum-guard subject.">
                Red
                Green

            func F2Maybe: <"C18Maybe twin: the switch under an OPTIONAL subject in a then-arm.">
                inputs:
                    k F2KindEnum (0..1)
                output:
                    hx string (0..1)
                set hx:
                    if k exists
                    then k switch
                        Red then "ff0000",
                        default "xx"

            type F2Whole: <"Constructor target.">
                name string (1..1)
                opt number (0..1)
                scores number (0..*)

            func F2Make: <"C19Make twin: an int list literal at a number (0..*) field.">
                inputs:
                    nm string (1..1)
                output:
                    w F2Whole (1..1)
                set w:
                    F2Whole {
                        name: nm,
                        scores: [1, 2, 3],
                        ...
                    }

            func F2Pick: <"C19Pick twin: a literal-else conditional at a constructor field.">
                inputs:
                    n number (1..1)
                output:
                    w F2Whole (1..1)
                set w:
                    F2Whole {
                        name: if n > 0 then "pos" else "neg",
                        opt: if n > 100 then n else empty,
                        ...
                    }

            func F2PickComputed: <"control: a NON-literal else keeps the final if/else form.">
                inputs:
                    n number (1..1)
                    nm string (1..1)
                output:
                    w F2Whole (1..1)
                set w:
                    F2Whole {
                        name: if n > 0 then nm else nm + "x",
                        ...
                    }

            type F2Twig:
                t string (1..1)
            type F2Branch:
                twigs F2Twig (0..*)
            type F2Trunk:
                title string (1..1)
                branches F2Branch (0..*)

            func F2Shade: <"C20Shade twin: a then-count operand in an Integer join assigned to a number.">
                inputs:
                    trunks F2Trunk (0..*)
                output:
                    n number (1..1)
                set n:
                    (trunks extract t [
                        t -> branches extract tb [
                            tb -> twigs count
                        ]
                    ] then flatten then sum)
                        + (trunks extract t [ t -> title ] then count)

            func F2Void: <"C21Void twin: a count assigned to a number output.">
                inputs:
                    many string (0..*)
                output:
                    n number (1..1)
                set n: many count

            func F2Has: <"cross-family watch: a count operand in a comparison keeps its wrap.">
                inputs:
                    many string (0..*)
                output:
                    b boolean (1..1)
                set b: many count > 0

            func F2Days: <"the s1a chain's catch: a Mapper-form if-then-else sentinel as an arithmetic operand splices BARE (golden cdm GenerateObservationDates `days`).">
                inputs:
                    lockoutDays int (0..1)
                output:
                    r int (1..1)
                alias days: 1 + (if lockoutDays exists then lockoutDays else 0)
                set r: days

            func F2Half: <"the round-2 review's input (cq SF-1): a count beside a LITERAL-bodied alias - the kind resolver declines a literal alias, so the join takes the legacy heuristic's BigDecimal default; the entry coercion converts the INT-stamped count, and the text wrap must not convert it again.">
                inputs:
                    many string (0..*)
                output:
                    r number (1..1)
                alias half: 0.5
                set r: many count * half
            """;

    // ---- a: the seven measured shapes (the chaos goldens' exact text) ----

    @Test
    @EnabledIf("builtinsAvailable")
    void a1_integerAliasInputInBigDecimalJoin_takesTheGuardedParamForm() throws IOException {
        String code = fn("F2Scale.java");
        assertContains(code, "r = MapperMaths.<BigDecimal, BigDecimal, BigDecimal>divide("
                + "MapperMaths.<BigDecimal, BigDecimal, BigDecimal>multiply("
                + "(q == null ? MapperS.<BigDecimal>ofNull() : MapperS.of(BigDecimal.valueOf(q))), "
                + "MapperS.of(w)), MapperS.of(BigDecimal.valueOf(100))).get();");
    }

    /**
     * The alias-vs-int RELATION, not a second golden: {@code F2ScalePlain} declares {@code q int}
     * where {@code F2Scale} declares the {@code fractionalDigits: 0} alias; both are the Java
     * {@code Integer}, so the type-directed conversion must render the two bodies identically.
     * (Refutes, or confirms, the "the alias is the differentiator" hypothesis the seat opened with.)
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1b_plainIntInputRendersExactlyAsTheAliasInput() throws IOException {
        String alias = assignOutputBody(fn("F2Scale.java"));
        String plain = assignOutputBody(fn("F2ScalePlain.java"));
        assertEquals(alias, plain, "the alias input and the plain int input must coerce identically");
    }

    @Test
    @EnabledIf("builtinsAvailable")
    void a2_schemeCarryingSwitchSubject_hoistsWithTheMetaUnwrapRung() throws IOException {
        String code = fn("F2FromCoded.java");
        assertContains(code, "final MapperS<String> switchArgument = MapperS.of(c)"
                + ".<FieldWithMetaString>map(\"getKind\", f2Coded -> f2Coded.getKind())"
                + ".<String>map(\"Type coercion\", fieldWithMetaString -> "
                + "fieldWithMetaString == null ? null : fieldWithMetaString.getValue());");
    }

    @Test
    @EnabledIf("builtinsAvailable")
    void a3_enumSwitchInAThenArm_rendersTheStatementLadder() throws IOException {
        String code = fn("F2Maybe.java");
        assertContains(code, "if (exists(MapperS.of(k)).getOrDefault(false)) {\n"
                + "\t\t\t\tif (k == null) {\n"
                + "\t\t\t\t\thx = null;\n"
                + "\t\t\t\t} else if (k == F2KindEnum.RED) {\n"
                + "\t\t\t\t\thx = \"ff0000\";\n"
                + "\t\t\t\t} else {\n"
                + "\t\t\t\t\thx = \"xx\";\n"
                + "\t\t\t\t}\n"
                + "\t\t\t} else {\n"
                + "\t\t\t\thx = null;\n"
                + "\t\t\t}");
        assertAbsent(code, "Objects.equals(");
    }

    @Test
    @EnabledIf("builtinsAvailable")
    void a4_intListLiteralAtANumberListField_takesTheUnguardedMapRung() throws IOException {
        String code = fn("F2Make.java");
        assertContains(code, ".setScores(MapperC.<Integer>of(MapperS.of(1), MapperS.of(2), MapperS.of(3))"
                + ".<BigDecimal>map(\"Type coercion\", integer -> BigDecimal.valueOf(integer)).getMulti())");
    }

    @Test
    @EnabledIf("builtinsAvailable")
    void a5_literalElseConditionalAtACtorField_takesTheInitializerForm() throws IOException {
        String code = fn("F2Pick.java");
        assertContains(code, "\t\t\tString ifThenElseResult0 = \"neg\";\n"
                + "\t\t\tif (greaterThan(MapperS.of(n), MapperS.of(BigDecimal.valueOf(0)), "
                + "CardinalityOperator.All).getOrDefault(false)) {\n"
                + "\t\t\t\tifThenElseResult0 = \"pos\";\n"
                + "\t\t\t}\n"
                + "\t\t\tBigDecimal ifThenElseResult1 = null;\n");
    }

    @Test
    @EnabledIf("builtinsAvailable")
    void a6_thenCountOperandInAnIntegerJoinAssignedToANumber_wrapsAndConvertsInStatementForm()
            throws IOException {
        String code = fn("F2Shade.java");
        assertContains(code, "\t\t\tfinal Integer integer = MapperMaths.<Integer, Integer, Integer>add(thenArg1\n"
                + "\t\t\t\t.sumInteger(), MapperS.of(thenArg2.resultCount())).get();\n"
                + "\t\t\tif (integer == null) {\n"
                + "\t\t\t\tn = null;\n"
                + "\t\t\t} else {\n"
                + "\t\t\t\tn = BigDecimal.valueOf(integer);\n"
                + "\t\t\t}\n");
    }

    @Test
    @EnabledIf("builtinsAvailable")
    void a7_countAssignedToANumberOutput_convertsTheBareIntUnguarded() throws IOException {
        String code = fn("F2Void.java");
        assertContains(code, "n = BigDecimal.valueOf(MapperC.<String>of(many).resultCount());");
        assertAbsent(code, ".resultCount().get()");
    }

    /**
     * The round-2 review's proposed counter-input (cq SF-1): a count beside a LITERAL-bodied alias -
     * the one valid shape the review read as reaching the legacy (plain-compile) join, where the
     * seat's INT stamp would let the entry coercion convert the count before the text wrap and the
     * deleted BigDecimal-item guard would have been needed. MEASURED, not read: the probe at the wrap
     * ({@code c8-probe-a8.log}) prints {@code resolvedJoin=true alreadyWrapped=true} for this input -
     * the inference engine types the alias, the join RESOLVES, and the count arrives wrapped by the
     * typed coercion; the text wrap declines. The review's refutation of "unreachable" is itself
     * refuted at runtime; the deletion stands on the corpus zero AND this measurement. A cross-family
     * watch: the resolved-path form asserted, the double conversion asserted absent.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a8_countBesideALiteralAlias_resolvesTheJoin_theCountArrivesWrapped() throws IOException {
        String code = fn("F2Half.java");
        assertContains(code, "MapperS.of(BigDecimal.valueOf(MapperC.<String>of(many).resultCount()))");
        assertAbsent(code, "BigDecimal.valueOf(BigDecimal.valueOf(");
    }

    // ---- c: the decline polarities and the cross-family watches ----

    /** An Integer input in an INTEGER join is identity — no guard, no conversion (LAW 81 control). */
    @Test
    @EnabledIf("builtinsAvailable")
    void c1_integerInputInIntegerJoin_staysBare() throws IOException {
        String code = fn("F2Sum.java");
        assertContains(code, "r = MapperMaths.<Integer, Integer, Integer>add(MapperS.of(q), MapperS.of(1)).get();");
        assertAbsent(code, "ofNull()");
        assertAbsent(code, "BigDecimal");
    }

    /** The count-operand wrap at a COMPARISON seat (19 vendored goldens) must survive the retyping. */
    @Test
    @EnabledIf("builtinsAvailable")
    void c2_countOperandInAComparison_keepsItsWrap() throws IOException {
        String code = fn("F2Has.java");
        assertContains(code, "greaterThan(MapperS.of(MapperC.<String>of(many).resultCount()), "
                + "MapperS.of(0), CardinalityOperator.All)");
    }

    /** A NON-literal else keeps the {@code final} if/else form (the initializer rule is literal-only). */
    @Test
    @EnabledIf("builtinsAvailable")
    void c3_computedElseConditionalAtACtorField_keepsTheFinalForm() throws IOException {
        String code = fn("F2PickComputed.java");
        // ONE conditional in the method: the singleton local is the bare `ifThenElseResult`
        // (the #170 singleton naming law); F2Pick's two number 0..1.
        assertContains(code, "\t\t\tfinal String ifThenElseResult;\n");
        assertContains(code, "\t\t\t} else {\n\t\t\t\tifThenElseResult = ");
        assertAbsent(code, "String ifThenElseResult = ");
    }

    /**
     * THE s1a CHAIN'S CATCH (PR #622): the #361 Mapper-form if-then-else slot's sentinel — a
     * {@code final MapperS<Integer> ifThenElseResult;} local returned with its ITEM class as its
     * stamp and self-unwrapping ({@code ControlFlowHandler.hoistAsItemLocalOrNull}) — is an
     * arithmetic operand whose Java value is ALREADY the Mapper, so the seat's item→wrapper operand
     * arm (gated on the count's PRIMITIVE stamp since the catch) must leave it BARE: golden cdm5/cdm6
     * {@code GenerateObservationDates} {@code days}, ten CDM cells on both routes, the #361 lock
     * {@code FnIteHoistSeatsComposeTest}. Lane J disables the gate; this witness goes red.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void c4_mapperFormIteSentinelOperand_splicesBare() throws IOException {
        String code = fn("F2Days.java");
        assertContains(code, "\t\t\tfinal MapperS<Integer> ifThenElseResult;\n"
                + "\t\t\tif (exists(MapperS.of(lockoutDays)).getOrDefault(false)) {\n"
                + "\t\t\t\tifThenElseResult = MapperS.of(lockoutDays);\n"
                + "\t\t\t} else {\n"
                + "\t\t\t\tifThenElseResult = MapperS.of(0);\n"
                + "\t\t\t}\n"
                + "\t\t\treturn MapperMaths.<Integer, Integer, Integer>add(MapperS.of(1), ifThenElseResult);\n");
        assertAbsent(code, "MapperS.of(ifThenElseResult)");
    }

    // ---- corpus locks: every declared F2 carrier byte-identical to its chaos golden ----

    @Test
    @EnabledIf("chaosAvailable")
    void corpus_c1_C8Scale_allTwelveVariants() throws IOException {
        lockChaos("s08", "C8Scale");
    }

    @Test
    @EnabledIf("chaosAvailable")
    void corpus_c2_C18FromCoded_allTwelveVariants() throws IOException {
        lockChaos("s18", "C18FromCoded");
    }

    @Test
    @EnabledIf("chaosAvailable")
    void corpus_c3_C18Maybe_allTwelveVariants() throws IOException {
        lockChaos("s18", "C18Maybe");
    }

    @Test
    @EnabledIf("chaosAvailable")
    void corpus_c4_C19Make_allTwelveVariants() throws IOException {
        lockChaos("s19", "C19Make");
    }

    @Test
    @EnabledIf("chaosAvailable")
    void corpus_c5_C19Pick_allTwelveVariants() throws IOException {
        lockChaos("s19", "C19Pick");
    }

    @Test
    @EnabledIf("chaosAvailable")
    void corpus_c6_C20Shade_allTwelveVariants() throws IOException {
        lockChaos("s20", "C20Shade");
    }

    @Test
    @EnabledIf("chaosAvailable")
    void corpus_c7_C21Void_allTwelveVariants() throws IOException {
        lockChaos("s21", "C21Void");
    }

    // ---- L3: the doctored-golden positive control (banked at PR-2 for the first fix seat) ----

    /**
     * The chaos byte comparator proven able to fail: a healed carrier's golden is doctored by ONE
     * byte (a space inside the seat's own conversion call) and run through the SAME comparison the
     * whole-class locks use — the lock must REPORT it and name the line. A comparator that could
     * not fail would make every green lock above vacuous (LAW 73, the #614 instrument law); this is
     * the charter's § 4 L3 control at the seat suite's grain, the temp-copy comparator harness the
     * PR-2 census banked for the first fix seat.
     */
    @Test
    @EnabledIf("chaosAvailable")
    void control3_doctoredGolden_isReportedByTheLock() throws IOException {
        assertNotNull(chaosOutput, "chaos cell generation did not run — corpus unavailable?");
        String carrier = "chaos/s21/base/functions/C21Void.java";
        String generated = chaosOutput.get(carrier);
        assertNotNull(generated, "not generated: " + carrier);
        String golden = normalize(Files.readString(CHAOS_GOLDEN.resolve(carrier)));
        assertTrue(compareCarrier(carrier, golden, generated) == null,
                "the undoctored carrier must lock green before the doctor is applied");
        String doctored = golden.replace("BigDecimal.valueOf(", "BigDecimal.valueOf (");
        assertTrue(!doctored.equals(golden), "the doctor must change a byte");
        String verdict = compareCarrier(carrier, doctored, generated);
        assertNotNull(verdict, "a doctored golden MUST be reported by the lock");
        assertContains(verdict, "BigDecimal.valueOf (");
    }

    // ---- LAW 77: the IR route renders the fixtures byte-identically to the default route ----

    /**
     * The population is the render map's OWN key set - never a hand-kept list (the first list
     * omitted five of the twelve, the commit-4 catch fixture {@code F2Days} among them; the reviews'
     * SF-1) - pinned at the MODEL's function count and required to be the SAME set on both routes.
     * Runs only with the IR provider on the classpath ({@code -Pir-on}): the chain's ON-route seat
     * step executes it; the OFF gensuite reports it skipped.
     */
    @Test
    @EnabledIf("builtinsAndIrProviderAvailable")
    void control2_irRoute_rendersEveryFixtureIdenticallyToTheDefaultRoute() throws IOException {
        Render def = render();
        Render ir = renderIr();
        assertTrue(def.errors().isEmpty(), "default-route generation errors: " + def.errors());
        assertTrue(ir.errors().isEmpty(), "IR-route generation errors: " + ir.errors());
        List<String> defFns = fixtureFunctionKeys(def);
        List<String> irFns = fixtureFunctionKeys(ir);
        assertEquals(MODEL_FUNCTIONS, defFns.size(),
                "the MODEL declares " + MODEL_FUNCTIONS + " functions; the default route rendered " + defFns);
        assertEquals(defFns, irFns, "the two routes must emit the SAME function set");
        assertTrue(defFns.stream().anyMatch(k -> k.endsWith("/F2Days.java")),
                "the commit-4 catch fixture must be in the population: " + defFns);
        for (String key : defFns) {
            assertEquals(normalize(def.output().get(key)), normalize(ir.output().get(key)),
                    "the IR route must agree with the default route for " + key);
        }
    }

    /** The MODEL's function count - the control's population pin (a fixture added without bumping it fails). */
    private static final int MODEL_FUNCTIONS = 13;

    private static List<String> fixtureFunctionKeys(Render r) {
        return r.output().keySet().stream()
                .filter(k -> k.contains("/functions/"))
                .sorted()
                .toList();
    }

    // =========================================================================
    // Fixture harness (the sibling seat suites' renderer)
    // =========================================================================

    private record Render(Map<String, String> output, List<String> errors) {}

    private static Render rendered;
    private static Render renderedIr;

    private static Render render() throws IOException {
        if (rendered != null) {
            return rendered;
        }
        rendered = renderModel(false);
        return rendered;
    }

    private static Render renderIr() throws IOException {
        if (renderedIr != null) {
            return renderedIr;
        }
        String previous = System.getProperty(IRGeneration.PROPERTY);
        System.setProperty(IRGeneration.PROPERTY, "true");
        try {
            assertNotNull(IRGeneration.providerOrNull(),
                    "the IR provider must be resolvable under -Pir-on, else this is not an ON-route render");
            renderedIr = renderModel(true);
        } finally {
            if (previous == null) {
                System.clearProperty(IRGeneration.PROPERTY);
            } else {
                System.setProperty(IRGeneration.PROPERTY, previous);
            }
        }
        return renderedIr;
    }

    private static Render renderModel(boolean irRoute) throws IOException {
        RModel main = AstBuilder.buildFromString(MODEL, "seat1f2.rosetta");
        main.setVersion("0.0.0.test");
        List<RModel> models = new ArrayList<>();
        models.add(main);
        models.addAll(loadBuiltinsOnly());
        RWorkspace workspace = RWorkspace.build(models).workspace();
        GeneratorModel gm = new GeneratorModel(workspace,
                m -> "census.seat1f2".equals(m.namespace()));
        JavaTypeUtil typeUtil = new JavaTypeUtil();
        JavaTypeTranslator tt = new JavaTypeTranslator(typeUtil);
        FunctionGenerator fg = irRoute
                ? IRGeneration.functionGenerator(gm, tt, typeUtil)
                : new FunctionGenerator(gm, tt, typeUtil);
        if (irRoute) {
            assertTrue(!fg.getClass().equals(FunctionGenerator.class),
                    "the seam must hand back the IR-route FunctionGenerator, got " + fg.getClass());
        }
        Map<String, String> out = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        fg.generateWithErrors(out)
                .forEach(e -> errors.add(e.getTargetPath() + " - " + e));
        return new Render(out, errors);
    }

    /** Functions land under {@code .../functions/}. */
    private static String fn(String fileName) throws IOException {
        return pick(render(), "functions/" + fileName, fileName);
    }

    private static String pick(Render r, String suffix, String fileName) {
        List<String> own = r.errors().stream().filter(e -> e.contains(fileName)).toList();
        assertTrue(own.isEmpty(),
                "the generator reported errors for " + fileName + " (a broken fixture must fail"
                + " loudly, not skip): " + own);
        String out = r.output().entrySet().stream()
                .filter(e -> e.getKey().endsWith("/" + suffix))
                .map(Map.Entry::getValue)
                .findFirst().orElse(null);
        assertNotNull(out, "not generated: " + fileName + " (have: " + r.output().keySet() + ")");
        return normalize(out);
    }

    /** The {@code assignOutput} method body — the part of a function class the shapes live in. */
    private static String assignOutputBody(String code) {
        int i = code.indexOf("assignOutput(");
        assertTrue(i >= 0, "no assignOutput in:\n" + code);
        return code.substring(i);
    }

    private static void assertContains(String code, String needle) {
        assertTrue(code.contains(needle), "expected <" + needle + "> in:\n" + code);
    }

    private static void assertAbsent(String code, String needle) {
        assertTrue(!code.contains(needle), "did NOT expect <" + needle + "> in:\n" + code);
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n");
    }

    /**
     * {@code null} when the generated text byte-matches the golden (newline-normalised); else the
     * named difference — the ONE comparison every whole-class lock and the L3 control go through.
     */
    private static String compareCarrier(String carrier, String golden, String generated) {
        String gen = normalize(generated);
        return golden.equals(gen) ? null
                : carrier + ": differs from its golden — " + firstDifference(golden, gen);
    }

    /** The first line pair that differs (golden vs generated), for a failure message that names it. */
    private static String firstDifference(String golden, String generated) {
        String[] g = golden.split("\n");
        String[] r = generated.split("\n");
        int n = Math.min(g.length, r.length);
        for (int i = 0; i < n; i++) {
            if (!g[i].equals(r[i])) {
                return "line " + (i + 1) + " golden <" + g[i].strip() + "> vs generated <" + r[i].strip() + ">";
            }
        }
        return "lengths differ: golden " + g.length + " lines vs generated " + r.length + " lines";
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
            throw new AssertionError("[DroppedCoercionSeatTest] builtins parse failures: "
                    + String.join("; ", failures));
        }
        return models;
    }

    // =========================================================================
    // The chaos cell — generated once, every declared F2 carrier locked whole
    // =========================================================================

    private static Map<String, String> chaosOutput;
    private static List<String> chaosGenErrors;

    @BeforeAll
    static void generateChaosCell() throws IOException {
        if (chaosAvailable()) {
            List<String> errs = new ArrayList<>();
            chaosOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("chaos", "1.0.0", CHAOS_ROOT), errs);
            chaosGenErrors = errs;
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

    private static void collect(List<String> errors, List<GenerationException> found) {
        for (GenerationException e : found) {
            errors.add(e.getTargetPath() + " - " + e);
        }
    }

    /** {@code a3half/p2} -> {@code a3half}; {@code base} -> {@code base}. */
    private static String placementFamily(String variant) {
        int slash = variant.indexOf('/');
        return slash < 0 ? variant : variant.substring(0, slash);
    }

    /**
     * {@code null} when {@code carriers} (golden-relative paths under {@code chaos/<seat>/}) hold every
     * placement FAMILY exactly once; else the named difference. The family is the variant path with its
     * {@code /pN} part stripped, because the a3 axis numbers the part directory per seat AND per class
     * (s18: C18FromCoded a3half/p1, C18Maybe a3half/p2) - the reason the carriers are enumerated, not listed.
     */
    private static String placementFamilyVerdict(String seat, List<String> carriers) {
        String prefix = "chaos/" + seat + "/";
        List<String> families = carriers.stream()
                .map(c -> c.substring(prefix.length(), c.indexOf("/functions/")))
                .map(DroppedCoercionSeatTest::placementFamily)
                .sorted()
                .toList();
        List<String> expected = VARIANTS.stream()
                .map(DroppedCoercionSeatTest::placementFamily)
                .sorted()
                .toList();
        return expected.equals(families) ? null
                : "expected one carrier per placement family " + expected + ", found " + families;
    }

    /** The placement-family identity pin proven able to fail (the round-2 review's NIT-4). */
    @Test
    void control4_placementFamilyPin_canFail() {
        assertEquals("a3half", placementFamily("a3half/p2"));
        assertEquals("base", placementFamily("base"));
        List<String> twelveGood = VARIANTS.stream().map(v -> "chaos/s99/" + v + "/functions/X.java").toList();
        assertTrue(placementFamilyVerdict("s99", twelveGood) == null, "the enumerated twelve must pass");
        // two carriers of one family and none of another: the count is still twelve, the identity is not
        List<String> doctored = new ArrayList<>(twelveGood);
        doctored.set(doctored.indexOf("chaos/s99/a3hub/p2/functions/X.java"), "chaos/s99/a3half/p1/functions/X.java");
        String verdict = placementFamilyVerdict("s99", doctored);
        assertNotNull(verdict, "a doctored population MUST be reported");
        // the verdict names the FOUND families (the expected list is a constant, so it cannot discriminate):
        // a3hub absent, a3half twice
        String found = verdict.substring(verdict.indexOf("found "));
        assertTrue(!found.contains("a3hub"), "the missing family must be absent from the found list: " + found);
        assertTrue(found.indexOf("a3half") != found.lastIndexOf("a3half"), "the doubled family must appear twice: " + found);
    }

    /**
     * Byte-lock all twelve placement variants of one declared F2 class against the chaos goldens.
     * A carrier the generator refused, did not emit, or emitted differently fails by NAME.
     */
    private static void lockChaos(String seat, String simpleName) throws IOException {
        assertNotNull(chaosOutput, "chaos cell generation did not run — corpus unavailable?");
        // The carriers are ENUMERATED from the golden tree, not hand-listed: the a3 placement
        // axis numbers its part directories per seat (`a3half/p1` for s18/s19, `a3half/p2` for
        // s08), so a fixed variant list mis-names a third of them. The population is then PINNED
        // at the declared twelve so a missing golden fails loudly instead of shrinking the lock.
        List<String> carriers;
        Path seatRoot = CHAOS_GOLDEN.resolve("chaos").resolve(seat);
        try (var stream = Files.walk(seatRoot)) {
            carriers = stream
                    .filter(p -> p.getFileName().toString().equals(simpleName + ".java"))
                    .filter(p -> p.getParent().getFileName().toString().equals("functions"))
                    .map(p -> CHAOS_GOLDEN.relativize(p).toString().replace('\\', '/'))
                    .sorted()
                    .toList();
        }
        assertEquals(VARIANTS.size(), carriers.size(),
                "expected the declared twelve placement variants of " + simpleName + " under " + seatRoot
                + ", found " + carriers);
        // ... and by IDENTITY, not count alone (the reviews' NIT-5), through the ONE helper the
        // control4 positive control proves able to fail
        String familyVerdict = placementFamilyVerdict(seat, carriers);
        assertTrue(familyVerdict == null,
                "the twelve carriers of " + simpleName + " must be one per placement family: " + familyVerdict);
        List<String> failures = new ArrayList<>();
        for (String carrier : carriers) {
            List<String> own = chaosGenErrors.stream().filter(e -> e.contains(carrier)).toList();
            if (!own.isEmpty()) {
                failures.add(carrier + ": generator errors " + own);
                continue;
            }
            String generated = chaosOutput.get(carrier);
            if (generated == null) {
                failures.add(carrier + ": not generated (have "
                        + chaosOutput.keySet().stream().filter(k -> k.endsWith(simpleName + ".java")).toList() + ")");
                continue;
            }
            Path goldenPath = CHAOS_GOLDEN.resolve(carrier);
            if (!Files.isRegularFile(goldenPath)) {
                failures.add(carrier + ": golden missing at " + goldenPath);
                continue;
            }
            String verdict = compareCarrier(carrier, normalize(Files.readString(goldenPath)), generated);
            if (verdict != null) {
                failures.add(verdict);
            }
        }
        assertTrue(failures.isEmpty(), "F2 carriers of " + simpleName + " not byte-identical to the chaos"
                + " goldens (" + failures.size() + "/" + VARIANTS.size() + "):\n  "
                + String.join("\n  ", failures));
    }
}
