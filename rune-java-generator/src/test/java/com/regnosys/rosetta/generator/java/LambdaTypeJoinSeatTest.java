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
 * v3.2 seat 2 — the chaos census's families F4 "lambda-type-join" (52 declared D11 rows, the four
 * s05 classes) and F17 (12 rows, s20 {@code C20Spread}), plus the silent {@code java.util.Objects}
 * import defect the seat-2 measurement census found. The witness suite for the fix: every
 * sub-shape the census measured is a reduced fixture here - a fork-authored model whose expected
 * strings follow the forms the chaos goldens pin for the same shapes (the fixtures are the seat's
 * mutation witnesses, NOT golden compares) - and every declared carrier class is byte-locked whole
 * against its goldens over every placement variant ENUMERATED from the golden tree (the corpus
 * locks and control3 are the golden compares).
 *
 * <p>THE MEASURED SHAPES (the both-side dumps at the #622 merge head, one diff shape per class over
 * all placement variants, default and IR routes byte-identical for the four s05 classes;
 * {@code C20Spread} differs between the routes by one token pair):
 * <ul>
 *   <li><b>Law 1 — the alias signature may not fall back to the function OUTPUT type.</b>
 *       {@code FunctionAliasHelper.computeReturnType}'s fallback emitted the enclosing function's
 *       RAW Rosetta output type name in a Mapper generic — {@code MapperS<? extends number>}
 *       ({@code C20Spread.backFirst}: {@code ordered then reverse then first}, REVERSE was outside
 *       the #252 op set), {@code MapperS<? extends boolean>} ({@code C5Preds.joined}: a
 *       {@code then join} body had no arm), {@code MapperS<? extends string>}
 *       ({@code C5Forms.graded}: a literal-guarded value switch body had no arm). Golden:
 *       {@code MapperS<? extends C20Leaf>}, {@code MapperS<String>}, {@code MapperS<Integer>}.</li>
 *   <li><b>Law 2 — a count is consumed by its TYPE, not by its node.</b> The count render carries
 *       the primitive {@code int} stamp since seat 1, but the comparison seat still keyed its wrap
 *       on the RAW node being an {@code RCountExpr} (a {@code then count} chain slipped through bare
 *       — {@code C5Preds}) and the to-string seat had no count arm at all ({@code C5Deep}: {@code
 *       .resultCount().map("to-string", …)} on a bare {@code int}). Golden lifts both into
 *       {@code MapperS.of(…)}.</li>
 *   <li><b>Law 3 — an alias then-chain hoists its {@code thenArg} per METHOD.</b> Every clean
 *       then-alias of {@code C5Forms} rendered the non-compiling inline {@code .then(item -> …)}
 *       form because the deep hoist's function-level all-or-nothing guard counted a control-flow
 *       then-chain in a SIBLING alias; an alias is its own Java method with its own
 *       {@code thenArg} numbering, so a sibling cannot renumber it.</li>
 *   <li><b>Law 4 — the type join must see the piped item.</b> {@code C5Forms.fallback}
 *       ({@code vals then default [0]}) and {@code lambdaIte} ({@code items extract x [ if x -> opt
 *       exists then x -> opt else 0 ]}) joined only the literal arm ({@code MapperC<Integer>}) where
 *       golden joins the receiver element ({@code MapperC<BigDecimal>}).</li>
 *   <li><b>Law 5 — the conditional's arms take the seat's expected form.</b> {@code C5Seats}: the
 *       same {@code if it -> opt exists then it -> opt else 0} at a condition and at an arithmetic
 *       operand compiled its arms to the ITEM form ({@code .get()} / bare literal) where golden keeps
 *       the Mapper-typed {@code final MapperS<BigDecimal> ifThenElseResult} ladder.</li>
 *   <li><b>The silent import.</b> The chained-ternary switch lowering emits {@code Objects.equals(…)}
 *       and never registered {@code java.util.Objects} — a non-compiling emission on every route,
 *       invisible to the byte gate (no golden carries the ternary form).</li>
 * </ul>
 */
class LambdaTypeJoinSeatTest {

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

    /**
     * The placement FAMILIES of the two seats (the a3 axis numbers its part directory per seat and
     * per class, so the carriers are enumerated from the golden tree and checked by family): s05 is
     * the expander's 13-variant seat (the twelve + {@code x13half}); s20 carries the twelve.
     */
    private static final List<String> FAMILIES_12 = List.of(
            "a1o1", "a1o2", "a1o3", "a1o4", "a2alias", "a2dangle", "a2qual", "a2wild",
            "a3half", "a3hub", "a3third", "base");
    private static final List<String> FAMILIES_13 = List.of(
            "a1o1", "a1o2", "a1o3", "a1o4", "a2alias", "a2dangle", "a2qual", "a2wild",
            "a3half", "a3hub", "a3third", "base", "x13half");

    // =========================================================================
    // The reduced fixtures — one construct per measured shape
    // =========================================================================

    private static final String MODEL = """
            namespace census.seat2f4
            version "0.0.0.test"

            metaType key string
            metaType reference string

            type F4Sub: <"List-of-list rung (the s05 helper).">
                [metadata key]
                vals number (0..*)
                name string (1..1)

            type F4Item: <"The s05 shape battery.">
                one string (1..1)
                opt number (0..1)
                many number (0..*)
                sub F4Sub (0..*)

            type F4Leaf: <"The s20 leaf.">
                v number (1..1)

            func F4Back: <"Law 1 (REVERSE): C20Spread's backFirst - `then reverse` inside the chain; the function output (number) must not leak into the alias signature.">
                inputs:
                    leaves F4Leaf (0..*)
                output:
                    spread number (1..1)
                alias ordered: leaves sort l [ l -> v ]
                alias backFirst: ordered then reverse then first
                alias revAll: leaves then reverse
                set spread: (backFirst -> v default 0) + ((ordered last -> v default 0) * 0) + ((revAll first -> v default 0) * 0)

            func F4Join: <"Law 1 (JOIN): C5Preds' joined - the function output (boolean) must not leak.">
                inputs:
                    items F4Item (0..*)
                output:
                    ok boolean (1..1)
                alias ones: items extract one
                alias joined: ones then join ", "
                set ok: joined exists

            func F4Graded: <"Law 1 (SWITCH): C5Forms' graded - literal guards, int results; the function output (string) must not leak.">
                inputs:
                    items F4Item (0..*)
                output:
                    verdict string (1..1)
                alias graded: items then only-element then extract one then switch "a" then 1, default 0
                set verdict: if graded >= 0 then "covered" else "sparse"

            func F4Deep: <"Law 2 (to-string): C5Deep's count consumed by to-string inside nested explicit lambdas.">
                inputs:
                    items F4Item (0..*)
                output:
                    names string (0..*)
                add names:
                    items
                        then extract x [
                            x -> sub extract y [
                                y -> name + "-" + (y -> vals count to-string)
                            ]
                        ]
                        then flatten

            func F4Count: <"Law 2 (comparison): a then-count operand behind a filter.">
                inputs:
                    items F4Item (0..*)
                output:
                    ok boolean (1..1)
                set ok: (items then filter opt exists then count) >= 0

            func F4CountBare: <"Law 2 control: the bare count operand keeps its single wrap (no double wrap).">
                inputs:
                    items F4Item (0..*)
                output:
                    ok boolean (1..1)
                set ok: items count >= 0

            func F4Forms: <"Law 3 (per-method hoist beside a ctl then-chain, graded) + Law 4 (fallback / lambdaIte joins).">
                inputs:
                    items F4Item (0..*)
                output:
                    verdict string (1..1)
                alias vals: items extract opt
                alias soloThen: items then only-element
                alias lastV: vals then last
                alias uniqOnes: (items extract one) then distinct
                alias fallback: vals then default [0]
                alias strsThen: vals then extract to-string
                alias graded: items then only-element then extract one then switch "a" then 1, default 0
                alias lambdaIte: items extract x [ if x -> opt exists then x -> opt else 0 ]
                alias asInt: "7" to-int
                set verdict:
                    if soloThen exists and lastV exists and uniqOnes count >= 0 and (fallback then sum) >= asInt
                        and graded >= 0 and strsThen count >= 0 and (lambdaIte sum) >= 0
                    then "covered"
                    else "sparse"

            func F4Lambda: <"C5Forms' lambdaThen in isolation: a nested then-chain inside an explicit-parameter extract.">
                inputs:
                    items F4Item (0..*)
                output:
                    n number (1..1)
                alias lambdaThen: items extract x [ x -> sub then filter item -> vals exists then extract name ] then flatten
                set n: lambdaThen count

            func F4Seats: <"Law 5: C5Seats - the same if-exists-then-else-0 at the alias, condition and set seats.">
                inputs:
                    it F4Item (1..1)
                output:
                    r number (1..1)
                alias seatAlias: if it -> opt exists then it -> opt else 0
                condition SeatCond:
                    (if it -> opt exists then it -> opt else 0) >= 0
                set r: (if it -> opt exists then it -> opt else 0) + seatAlias

            func F4Obj: <"the silent import: an inline chained-ternary switch at an arithmetic-operand seat.">
                inputs:
                    s string (1..1)
                output:
                    n number (1..1)
                set n: (s switch "a" then 1, default 0) + 1
            """;

    // ---- a: the measured shapes (the chaos goldens' exact text, on the reduced fixtures) ----

    /** Law 1 (F17): the alias signature is the ELEMENT the chain pipes, not the function output. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_reverseThenFirstAlias_typesAsTheElementNotTheFunctionOutput() throws IOException {
        String code = fn("F4Back.java");
        assertContains(code, "protected abstract MapperS<? extends F4Leaf> backFirst(List<? extends F4Leaf> leaves);");
        // PR #623 (round-1 cq review, SF-8): the REVERSE cardinality sub-arm's witness - a bare
        // `then reverse` alias keeps the receiver's cardinality (multi in -> multi out); with the sub-arm
        // off the walk types it SINGLE (lane AG goes red here).
        assertContains(code, "protected abstract MapperC<? extends F4Leaf> revAll(List<? extends F4Leaf> leaves);");
        assertContains(code, "\t\t\tfinal MapperC<? extends F4Leaf> thenArg0 = ordered(leaves);\n"
                + "\t\t\tfinal MapperC<F4Leaf> thenArg1 = thenArg0\n"
                + "\t\t\t\t.reverse();\n"
                + "\t\t\treturn thenArg1\n"
                + "\t\t\t\t.first();");
        assertAbsent(code, "? extends number");
    }

    /** Law 1 (the default route's token pair): the navigation off the alias call names its lambda from the element. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1b_navigationOffTheSortAliasCall_namesTheLambdaFromTheElementWithItsWitness() throws IOException {
        String code = fn("F4Back.java");
        assertContains(code, "backFirst(leaves).<BigDecimal>map(\"getV\", f4Leaf -> f4Leaf.getV())");
        assertContains(code, ".last().<BigDecimal>map(\"getV\", f4Leaf -> f4Leaf.getV())");
        assertAbsent(code, "_backFirst");
        assertAbsent(code, "_v -> _v");
    }

    /** Law 1 (JOIN): a `then join` alias is MapperS<String>, whatever the function output. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_joinAlias_typesString() throws IOException {
        String code = fn("F4Join.java");
        assertContains(code, "protected abstract MapperS<String> joined(List<? extends F4Item> items);");
        assertContains(code, "\t\t\tfinal MapperC<String> thenArg = ones(items);\n"
                + "\t\t\treturn thenArg.join(MapperS.of(\", \"));");
        assertAbsent(code, "? extends boolean");
    }

    /** Laws 1 + 3: a then-chain ending in a literal-guarded value switch types Integer and hoists golden's ladder. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a3_literalSwitchThenChainAlias_typesIntegerAndRendersTheHoistedLadder() throws IOException {
        String code = fn("F4Graded.java");
        assertContains(code, "protected abstract MapperS<Integer> graded(List<? extends F4Item> items);");
        assertContains(code, GRADED_BODY);
        assertAbsent(code, "Objects.equals(");
        assertAbsent(code, ".then(");
        assertAbsent(code, "? extends string");
    }

    private static final String GRADED_BODY =
            "\t\t\tfinal MapperC<F4Item> thenArg0 = MapperC.<F4Item>of(items);\n"
            + "\t\t\tfinal MapperS<F4Item> thenArg1 = MapperS.of(thenArg0.get());\n"
            + "\t\t\tfinal MapperS<String> thenArg2 = thenArg1\n"
            + "\t\t\t\t.mapSingleToItem(item -> item.<String>map(\"getOne\", f4Item -> f4Item.getOne()));\n"
            + "\t\t\tfinal MapperS<Integer> ifThenElseResult;\n"
            + "\t\t\tif (thenArg2.get() == null) {\n"
            + "\t\t\t\tifThenElseResult = MapperS.<Integer>ofNull();\n"
            + "\t\t\t} else if (areEqual(thenArg2, MapperS.of(\"a\"), CardinalityOperator.All).get()) {\n"
            + "\t\t\t\tifThenElseResult = MapperS.of(1);\n"
            + "\t\t\t} else {\n"
            + "\t\t\t\tifThenElseResult = MapperS.of(0);\n"
            + "\t\t\t}\n"
            + "\t\t\treturn ifThenElseResult;";

    /** Law 2 (to-string): the count lifts into MapperS before the to-string map. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a4_countConsumedByToString_liftsIntoMapperS() throws IOException {
        String code = fn("F4Deep.java");
        assertContains(code, "MapperS.of(y.<BigDecimal>mapC(\"getVals\", f4Sub -> f4Sub.getVals()).resultCount())"
                + ".map(\"to-string\", Object::toString)");
        assertAbsent(code, ".resultCount().map(");
    }

    /** Law 2 (comparison): a `then count` chain's count lifts by TYPE — the node read never saw it. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a5_thenCountComparisonOperand_liftsIntoMapperS() throws IOException {
        String code = fn("F4Count.java");
        assertContains(code, "greaterThanEquals(MapperS.of(thenArg1.resultCount()), MapperS.of(0), CardinalityOperator.All)");
    }

    /** Laws 3 + 4 on the C5Forms battery: every clean then-alias hoists per method; the two joins see the piped item. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a6_thenAliasesBesideTheSwitchChain_hoistPerMethod_andTheJoinsSeeThePipedItem() throws IOException {
        String code = fn("F4Forms.java");
        assertContains(code, "protected abstract MapperC<BigDecimal> fallback(List<? extends F4Item> items);");
        assertContains(code, "protected abstract MapperC<BigDecimal> lambdaIte(List<? extends F4Item> items);");
        assertContains(code, "protected abstract MapperS<Integer> graded(List<? extends F4Item> items);");
        assertContains(code, "\t\t\tfinal MapperC<F4Item> thenArg = MapperC.<F4Item>of(items);\n"
                + "\t\t\treturn MapperS.of(thenArg.get());");
        assertContains(code, "\t\t\tfinal MapperC<BigDecimal> thenArg = vals(items);\n"
                + "\t\t\treturn thenArg\n"
                + "\t\t\t\t.last();");
        assertContains(code, "\t\t\t\t.mapItem(item -> item.<String>map(\"getOne\", f4Item -> f4Item.getOne()));\n"
                + "\t\t\treturn distinct(thenArg);");
        assertContains(code, "\t\t\tfinal MapperC<BigDecimal> thenArg = vals(items);\n"
                + "\t\t\treturn thenArg\n"
                + "\t\t\t\t.mapItem(item -> item.map(\"to-string\", Object::toString));");
        assertContains(code, GRADED_BODY);
        assertAbsent(code, ".then(");
        assertAbsent(code, "MapperC<Integer> ");
        assertAbsent(code, "import java.util.Objects;");
    }

    /** Law 4 + the multi default: the elided left is the whole pipe, the literal right widens to its element. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a7_thenDefaultOverAMultiPipe_takesTheTernaryWithTheWideningRung() throws IOException {
        String code = fn("F4Forms.java");
        assertContains(code, "\t\t\tfinal MapperC<BigDecimal> thenArg = vals(items);\n"
                + "\t\t\treturn (thenArg.getMulti().isEmpty() ? MapperC.<Integer>of(MapperS.of(0))"
                + ".<BigDecimal>map(\"Type coercion\", integer -> BigDecimal.valueOf(integer)) : thenArg);");
        assertAbsent(code, ".getOrDefault(MapperC.");
    }

    /** The typed numeric widening at the comparison seat: an Integer alias operand against a BigDecimal sum. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a8_integerAliasOperandAgainstABigDecimalSum_takesTheGuardedWideningRung() throws IOException {
        String code = fn("F4Forms.java");
        assertContains(code, "\t\t\tfinal MapperC<BigDecimal> thenArg = fallback(items);\n");
        assertContains(code, "greaterThanEquals(thenArg\n\t\t\t\t.sumBigDecimal(), asInt(items).<BigDecimal>map(\"Type coercion\", "
                + "integer -> integer == null ? null : BigDecimal.valueOf(integer)), CardinalityOperator.All)");
    }

    /** Law 5: the conditional at a comparison operand and at an arithmetic operand keeps the Mapper form with Mapper arms. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a9_conditionalAtMapperOperandSeats_takesTheMapperFormWithVerbatimChainArms() throws IOException {
        String code = fn("F4Seats.java");
        String ladder = "\t\t\tfinal MapperS<BigDecimal> ifThenElseResult;\n"
                + "\t\t\tif (exists(MapperS.of(it).<BigDecimal>map(\"getOpt\", f4Item -> f4Item.getOpt())).getOrDefault(false)) {\n"
                + "\t\t\t\tifThenElseResult = MapperS.of(it).<BigDecimal>map(\"getOpt\", f4Item -> f4Item.getOpt());\n"
                + "\t\t\t} else {\n"
                + "\t\t\t\tifThenElseResult = MapperS.of(BigDecimal.valueOf(0));\n"
                + "\t\t\t}\n";
        assertContains(code, ladder + "\t\t\treturn greaterThanEquals(ifThenElseResult, MapperS.of(BigDecimal.valueOf(0)), CardinalityOperator.All);");
        assertContains(code, ladder + "\t\t\tr = MapperMaths.<BigDecimal, BigDecimal, BigDecimal>add(ifThenElseResult, seatAlias(it)).get();");
        assertAbsent(code, "final BigDecimal ifThenElseResult;");
        assertAbsent(code, ".get());");
    }

    /**
     * The silent import, CLOSED BY CONSTRUCTION (v3.2 seat 12, D52): this test pinned that an emitted
     * {@code Objects.equals(…)} chained ternary registers {@code java.util.Objects} (the seat-2 fix for the F16
     * javac census's missing import). The fixture - a switch at an arithmetic-operand seat - is EXACTLY the gen-2
     * census's M2 class (a switch at a seat with no ladder renderer), and since seat 12 the residual ternary
     * fall-through REFUSES at {@code SilentDegradation.Site.SWITCH_TERNARY_STUB} instead of rendering: no file
     * can carry an {@code Objects.equals} render without its import because no file carries the render. The
     * fixture is kept (tests are cumulative) and the assertion re-cut to the refusal; the heal by hoist channel
     * (an {@code ifThenElseResult} ladder at the operand seat) is a later seat's, where this test re-pins the
     * ladder.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a10_chainedTernarySwitch_refusedAtSwitchTernaryStub_neverEmitsTheImportlessRender() throws IOException {
        Render r = render();
        List<String> own = r.errors().stream().filter(e -> e.contains("F4Obj.java")).toList();
        assertTrue(own.size() == 1 && own.get(0).contains("[SWITCH_TERNARY_STUB]"),
                "F4Obj refuses at the residual switch site: " + own);
        assertTrue(r.output().keySet().stream().noneMatch(k -> k.endsWith("/functions/F4Obj.java")),
                "the refused function does not emit");
    }

    // ---- c: the decline polarities and the cross-family watches ----

    /** The bare count operand keeps its single wrap (LAW 81 control: the typed lift cannot double-wrap). */
    @Test
    @EnabledIf("builtinsAvailable")
    void c1_bareCountComparisonOperand_keepsItsSingleWrap() throws IOException {
        String code = fn("F4CountBare.java");
        assertContains(code, "greaterThanEquals(MapperS.of(MapperC.<F4Item>of(items).resultCount()), MapperS.of(0), CardinalityOperator.All)");
        assertAbsent(code, "MapperS.of(MapperS.of(");
    }

    /** Cross-family watch: the nested in-lambda then-chain (C5Forms' lambdaThen) renders its block form unchanged. */
    @Test
    @EnabledIf("builtinsAvailable")
    void c2_nestedLambdaThenChain_keepsTheBlockLambdaForm() throws IOException {
        String code = fn("F4Lambda.java");
        assertContains(code, "\t\t\tfinal MapperListOfLists<String> thenArg = MapperC.<F4Item>of(items)\n"
                + "\t\t\t\t.mapItemToList(x -> {\n"
                + "\t\t\t\t\tfinal MapperC<F4Sub> thenArg0 = x.<F4Sub>mapC(\"getSub\", f4Item -> f4Item.getSub());\n");
        assertContains(code, "\t\t\treturn thenArg\n\t\t\t\t.flattenList();");
    }

    // ---- corpus: every declared carrier class byte-locked whole, all placement variants ----

    @Test
    @EnabledIf("chaosAvailable")
    void corpus_c1_C5Deep_allThirteenVariants() throws IOException {
        lockChaos("s05", "C5Deep", FAMILIES_13);
    }

    @Test
    @EnabledIf("chaosAvailable")
    void corpus_c2_C5Preds_allThirteenVariants() throws IOException {
        lockChaos("s05", "C5Preds", FAMILIES_13);
    }

    @Test
    @EnabledIf("chaosAvailable")
    void corpus_c3_C5Seats_allThirteenVariants() throws IOException {
        lockChaos("s05", "C5Seats", FAMILIES_13);
    }

    @Test
    @EnabledIf("chaosAvailable")
    void corpus_c4_C5Forms_allThirteenVariants() throws IOException {
        lockChaos("s05", "C5Forms", FAMILIES_13);
    }

    @Test
    @EnabledIf("chaosAvailable")
    void corpus_c5_C20Spread_allTwelveVariants() throws IOException {
        lockChaos("s20", "C20Spread", FAMILIES_12);
    }

    // ---- controls (LAW 81): the lock can fail, the family pin can fail, the IR route agrees ----

    /** L3 positive control: a doctored golden IS reported by the lock (a byte moved in the REVERSE step). */
    @Test
    @EnabledIf("chaosAvailable")
    void control3_doctoredGolden_isReportedByTheLock() throws IOException {
        assertNotNull(chaosOutput, "chaos cell generation did not run — corpus unavailable?");
        String carrier = "chaos/s20/base/functions/C20Spread.java";
        String generated = chaosOutput.get(carrier);
        assertNotNull(generated, "not generated: " + carrier);
        String golden = normalize(Files.readString(CHAOS_GOLDEN.resolve(carrier)));
        assertTrue(compareCarrier(carrier, golden, generated) == null,
                "the undoctored carrier must lock green before the doctor is applied");
        String doctored = golden.replace(".reverse();", ".reverse ();");
        assertTrue(!doctored.equals(golden), "the doctor must change a byte");
        String verdict = compareCarrier(carrier, doctored, generated);
        assertNotNull(verdict, "a doctored golden MUST be reported by the lock");
        assertContains(verdict, ".reverse ();");
    }

    /** The placement-family identity pin proven able to fail (over the 13-family seat). */
    @Test
    void control4_placementFamilyPin_canFail() {
        assertEquals("a3half", placementFamily("a3half/p2"));
        assertEquals("x13half", placementFamily("x13half/p1"));
        List<String> good = FAMILIES_13.stream().map(v -> "chaos/s99/" + v + "/p1/functions/X.java").toList();
        assertTrue(placementFamilyVerdict("s99", good, FAMILIES_13) == null, "the enumerated thirteen must pass");
        List<String> doctored = new ArrayList<>(good);
        doctored.set(doctored.indexOf("chaos/s99/a3hub/p1/functions/X.java"), "chaos/s99/a3half/p2/functions/X.java");
        String verdict = placementFamilyVerdict("s99", doctored, FAMILIES_13);
        assertNotNull(verdict, "a doctored population MUST be reported");
        String found = verdict.substring(verdict.indexOf("found "));
        assertTrue(!found.contains("a3hub"), "the missing family must be absent from the found list: " + found);
        assertTrue(found.indexOf("a3half") != found.lastIndexOf("a3half"), "the doubled family must appear twice: " + found);
    }

    /**
     * LAW 77: the IR route renders every fixture byte-identically to the default route. The population is
     * the render map's OWN key set, pinned at the MODEL's function count LESS the declared refusal set; runs
     * only with the IR provider on the classpath ({@code -Pir-on} — the chain's ON-route seat step).
     * <p>v3.2 seat 12 (PR #633, D52 — R1): {@code F4Obj} refuses at {@code SWITCH_TERNARY_STUB} on BOTH routes
     * since the counter commit (a10 pins the default route's refusal; the IR route serves the switch through
     * the same handler as an oracle root — LAW 77 by identity, witnessed here by print). This control admits
     * EXACTLY that declared set (LAW 73, the commit-8 shape of {@code EnumSwitchBlockWidenSeatTest}): one
     * error per route naming the file and the site, no other error, and the SAME nine-function set rendered
     * byte-identically. The chain of record {@code s12a} at the commit-8 head caught the stale pin — this test
     * runs on the ON route alone, which no targeted OFF-route run and no verify-gate half reaches.
     */
    @Test
    @EnabledIf("builtinsAndIrProviderAvailable")
    void control2_irRoute_rendersEveryFixtureIdenticallyToTheDefaultRoute() throws IOException {
        Render def = render();
        Render ir = renderIr();
        assertDeclaredRefusalsOnly(def.errors(), "default-route");
        assertDeclaredRefusalsOnly(ir.errors(), "IR-route");
        List<String> defFns = fixtureFunctionKeys(def);
        List<String> irFns = fixtureFunctionKeys(ir);
        assertEquals(MODEL_FUNCTIONS - DECLARED_REFUSALS.size(), defFns.size(),
                "the MODEL declares " + MODEL_FUNCTIONS + " functions and " + DECLARED_REFUSALS.size()
                        + " refuse by declaration; the default route rendered " + defFns);
        for (String refused : DECLARED_REFUSALS.keySet()) {
            assertTrue(defFns.stream().noneMatch(k -> k.endsWith("/functions/" + refused)),
                    "the refused function does not emit on the default route: " + refused);
        }
        assertEquals(defFns, irFns, "the two routes must emit the SAME function set");
        for (String key : defFns) {
            assertEquals(normalize(def.output().get(key)), normalize(ir.output().get(key)),
                    "the IR route must agree with the default route for " + key);
        }
    }

    /** The MODEL's function count — the control's population pin (a fixture added without bumping it fails). */
    private static final int MODEL_FUNCTIONS = 10;

    /**
     * The MODEL's declared refusal set (LAW 73): a fixture file → the register site it refuses at, on BOTH
     * routes. {@code F4Obj} — the silent import fixture, an inline chained-ternary switch at an arithmetic-operand
     * seat — refuses at R1 since v3.2 seat 12's counter commit (a10 is its default-route pin). A refusal that
     * does not fire fails the control as loudly as an undeclared one.
     */
    private static final Map<String, String> DECLARED_REFUSALS = Map.of("F4Obj.java", "[SWITCH_TERNARY_STUB]");

    private static void assertDeclaredRefusalsOnly(List<String> errors, String route) {
        assertEquals(DECLARED_REFUSALS.size(), errors.size(),
                route + " generation errors must be EXACTLY the declared refusal set " + DECLARED_REFUSALS + ": " + errors);
        for (Map.Entry<String, String> declared : DECLARED_REFUSALS.entrySet()) {
            assertTrue(errors.stream().anyMatch(e -> e.contains(declared.getKey()) && e.contains(declared.getValue())),
                    route + ": the declared refusal " + declared.getKey() + " at " + declared.getValue() + " did not fire: " + errors);
        }
    }

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
        RModel main = AstBuilder.buildFromString(MODEL, "seat2f4.rosetta");
        main.setVersion("0.0.0.test");
        List<RModel> models = new ArrayList<>();
        models.add(main);
        models.addAll(loadBuiltinsOnly());
        RWorkspace workspace = RWorkspace.build(models).workspace();
        GeneratorModel gm = new GeneratorModel(workspace,
                m -> "census.seat2f4".equals(m.namespace()));
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
            throw new AssertionError("[LambdaTypeJoinSeatTest] builtins parse failures: "
                    + String.join("; ", failures));
        }
        return models;
    }

    // =========================================================================
    // The chaos cell — generated once, every declared carrier locked whole
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
     * placement FAMILY of {@code expectedFamilies} exactly once; else the named difference.
     */
    private static String placementFamilyVerdict(String seat, List<String> carriers,
            List<String> expectedFamilies) {
        String prefix = "chaos/" + seat + "/";
        List<String> families = carriers.stream()
                .map(c -> c.substring(prefix.length(), c.indexOf("/functions/")))
                .map(LambdaTypeJoinSeatTest::placementFamily)
                .sorted()
                .toList();
        List<String> expected = expectedFamilies.stream().sorted().toList();
        return expected.equals(families) ? null
                : "expected one carrier per placement family " + expected + ", found " + families;
    }

    /**
     * Byte-lock every placement variant of one declared class against the chaos goldens. A carrier
     * the generator refused, did not emit, or emitted differently fails by NAME.
     */
    private static void lockChaos(String seat, String simpleName, List<String> families) throws IOException {
        assertNotNull(chaosOutput, "chaos cell generation did not run — corpus unavailable?");
        List<String> carriers = carriersOf(seat, simpleName);
        assertEquals(families.size(), carriers.size(),
                "expected the declared " + families.size() + " placement variants of " + simpleName
                + " under chaos/" + seat + ", found " + carriers);
        String familyVerdict = placementFamilyVerdict(seat, carriers, families);
        assertTrue(familyVerdict == null,
                "the carriers of " + simpleName + " must be one per placement family: " + familyVerdict);
        List<String> failures = new ArrayList<>();
        for (String carrier : carriers) {
            String verdict = lockOne(carrier);
            if (verdict != null) {
                failures.add(verdict);
            }
        }
        assertTrue(failures.isEmpty(), "carriers of " + simpleName + " not byte-identical to the chaos"
                + " goldens (" + failures.size() + "/" + families.size() + "):\n  "
                + String.join("\n  ", failures));
    }

    private static List<String> carriersOf(String seat, String simpleName) throws IOException {
        Path seatRoot = CHAOS_GOLDEN.resolve("chaos").resolve(seat);
        try (var stream = Files.walk(seatRoot)) {
            return stream
                    .filter(p -> p.getFileName().toString().equals(simpleName + ".java"))
                    .filter(p -> p.getParent().getFileName().toString().equals("functions"))
                    .map(p -> CHAOS_GOLDEN.relativize(p).toString().replace('\\', '/'))
                    .sorted()
                    .toList();
        }
    }

    /** {@code null} when one carrier byte-matches its golden; else the named failure. */
    private static String lockOne(String carrier) throws IOException {
        List<String> own = chaosGenErrors.stream().filter(e -> e.contains(carrier)).toList();
        if (!own.isEmpty()) {
            return carrier + ": generator errors " + own;
        }
        String generated = chaosOutput.get(carrier);
        if (generated == null) {
            return carrier + ": not generated";
        }
        Path goldenPath = CHAOS_GOLDEN.resolve(carrier);
        if (!Files.isRegularFile(goldenPath)) {
            return carrier + ": golden missing at " + goldenPath;
        }
        return compareCarrier(carrier, normalize(Files.readString(goldenPath)), generated);
    }
}
