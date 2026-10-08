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
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.util.AstWalker;
import com.regnosys.rosetta.generator.GenerationException;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.object.datarule.DataRuleGenerator;
import com.regnosys.rosetta.generator.java.reports.ReportGenerator;
import com.regnosys.rosetta.generator.java.reports.RuleGenerator;
import com.regnosys.rosetta.generator.java.spi.IRGeneration;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.symbols.RLinkingResult;
import com.regnosys.rosetta.symbols.RWorkspace;

/**
 * v3.2 seat 7: the seat suite of F11 — a CHOICE-keyed {@code switch} INSIDE a list-op lambda whose arms are
 * case-NARROWED navigations, a to-string over one and a non-empty literal default (the chaos {@code C18ToKind} shape,
 * {@code alias pulled: eths extract (item switch C18OptA then item -> av, C18OptB then item -> bv to-string, default
 * "none")}), 12 declared D11 rows (the {@code TYPE_SWITCH_TERNARY_STUB} refusals — every s18 placement variant) healed
 * on BOTH routes under two oracle groups pinned from the released 9.83.0 plugin BEFORE the code, and the TWO control
 * findings the oracle turned up beside them (the enum-keyed and the literal-keyed switch in a lambda, both non-compiling
 * emissions before the seat).
 *
 * <ul>
 *   <li><b>(A) the arm law</b> — the block-lambda switch seats admitted a function-call arm, the bare item and a
 *       {@code <bare attr> to-string} only, and an EMPTY default only; every other shape fell past the seat-31
 *       option-getter block and the #226 instanceof block to the ControlFlowHandler ternary, which REFUSES a type-keyed
 *       guard (the LOUD register site). The seats now share ONE enumerated admission
 *       ({@code CollectionHandler.admissibleBlockSwitchCaseBody} — a function call, the bare item, a case-narrowed
 *       navigation rooted at the item, a to-string over one, a literal) and ONE default-arm law
 *       ({@code compileBlockSwitchDefault}: a non-empty default compiles last in the lambda's own scope and becomes the
 *       terminal {@code return}); the option getter splices a nav / to-string / literal arm VERBATIM off the bound
 *       MapperS-typed local (the pre-seat wrap was the bare invocation's alone); the int-literal coercion of a
 *       number-typed switch threads as the expected type ({@code HandlerHelper.intLiteralArmExpectedType} — the SET seat's
 *       conditional-arm law, LAW 69).</li>
 *   <li><b>(B) the subject</b> — the lambda item's element type resolves through the receiver's OWN binding
 *       ({@code CollectionHandler.implicitItemRType} over {@code NavigationHandler.resolveReceiverRType}), never a
 *       simple-name lookup: the chaos s18 seed expands into twelve namespaces each declaring a {@code C18Either}, and the
 *       pre-seat {@code gm.resolveTypeByName} bound the FIRST (a1o1's) for every variant — the option getter's path walk
 *       failed its identity check and the instanceof form leaked with a foreign import (the fix1 measurement). A
 *       MODEL-CHOICE subject whose option form declines DECLINES the block (never the instanceof form).</li>
 *   <li><b>(C) the deep hop</b> — a case type reachable only THROUGH a nested choice option resolves by the SAME walk the
 *       SET seat's ladder consults ({@code ChoiceSwitchSupport.findChoiceOptionPath}); the nav is the synthesized
 *       feature-call chain along the path.</li>
 *   <li><b>(D) the literal-guard block</b> — a LITERAL-keyed switch in a lambda had NO block renderer (the
 *       {@code Objects.equals("r", item)} ternary compiles and is always false); it renders the {@code areEqual} ladder
 *       through the SET seat's own guard render ({@code ChoiceSwitchSupport.renderGuardMapper}, extracted from
 *       {@code FunctionExpressionRenderer}), and the alias SIGNATURE reads the same class through
 *       {@code FunctionAliasHelper.switchValueCaseJoin}'s lambda-item arm ({@code MapperC<String>} where the output-type
 *       fallback had leaked the raw rune name {@code string}).</li>
 *   <li><b>(E) the filter block, the list form, the then-hoist</b> — a choice switch as a FILTER predicate renders the
 *       Boolean block; a MULTI nav arm takes the list twin over a MULTI receiver ({@code mapItemToList} into a
 *       {@code MapperListOfLists} then-arg — the map-method chooser's body read sees the MULTI arm); the then-hoist's
 *       compiler-free switch admission mirrors the widened arm law, so the datarule's
 *       {@code (eths extract [ item switch … ] then count)} hoists like the function condition.</li>
 * </ul>
 *
 * <p>The fixtures below mirror the seat's ORACLE groups ({@code holdout/choice-switch-in-lambda}, {@code -edge} —
 * byte-locked whole by {@code HoldOutByteCompareTest}) under {@code census.seat7*} namespaces; each expected string is
 * the golden's own text (the namespace never appears in a function body), so every assertion is a mutation witness, not
 * a guess. The controls: a11 is the SET-seat option-nav ladder (the #394 form the seat must NOT move — byte-identical
 * before and after); control2a pins the default route's populations; control2b is LAW 77 on the IR route (runs under
 * {@code -Pir-on} — the chain's ON-route seat step; the datarule generator has no IR twin); the chaos carriers are
 * byte-locked whole over every placement variant ({@code C18ToKind} x12, the twelve s18 families that carry a
 * {@code functions/} directory); control3 / control4 prove the locks able to fail.
 *
 * <p><b>THE LANE SET</b> (v3.2 seat 7 commit 14 — {@code target/v32-seat7-instruments/lanes-s7.py}, local; the receipt
 * {@code scratch/lanes-s7.status} at the round-2 code head {@code 55464ce0a}, 2026-09-08 06:23–06:33 local, the tree clean at
 * both ends; the round-1 run at {@code 7399d3c76} preserved as {@code scratch/lanes-s7-r1-7399d3c76.status} —
 * SIXTEEN lanes now, I1 the eleventh register site's refusal removed (the literal-keyed lambda switch's fall-through
 * back to the silent ternary) → control5 ALONE; fourteen of the fifteen shared lanes measuring the SAME sets and C1
 * — the literal block's dispatch gated off — grown by control2a control2b control2c: with the block gone the
 * literal-keyed switches reach the round-2 LOUD refusal instead of the silent ternary, so the population pin, the
 * doctored-pin precondition and the route identity fail as the refusal's CONSEQUENCE (the A1 / A2 / E1 / F1 class),
 * a measured and explained movement, not a re-prediction; the round-1 run's
 * own header follows — at the round-1 code head {@code 7399d3c76}, 2026-09-08 04:21–04:33 local, the tree clean at
 * both ends; the commit-7 run at {@code b21c39fcf} preserved as {@code scratch/lanes-s7-c7-b21c39fcf.status} and the
 * commit-5 run of the first fourteen lanes at {@code 229223f89} as {@code scratch/lanes-s7-r0-229223f89.status}, the
 * same sets at all three heads — three lanes re-anchored on the round-1 code (A2 the default law without its dead
 * {@code listForm} parameter, B2 the decline's comment, E1 the bracketed filter attempt), the other twelve verbatim; a
 * seventeenth mutation, E4 the static gate's LITERAL-guard widening reverted, was measured by TEXT SWAP before commit 10
 * over both suites AND the whole hold-out byte bar ({@code scratch/lane-E4-r1.log}, 126/0F GREEN) and its target
 * WITHDRAWN under the #614 law, so it carries no lane; the suite AND the seat-31 option-getter suite run under
 * {@code -Pir-on} so control2b RUNS on every lane and e1 is a witness): sixteen exact-string mutations of the SHIPPED code,
 * one per fix arm, each applied, run against both suites and restored from git — the runner refuses a dirty tree and
 * dry-checks every anchor for its expected count first (fifteen unique; A3's line is shared verbatim by the option getter
 * and the list twin, both mutated); the one cross-module lane (G1 on rune-ir-java) installs the mutated module before
 * its run and re-installs the restored one after. THE MEASURED RED SETS, as SETS (each kept lane red on EXACTLY these and
 * green on the rest; a set that CONTAINS its prediction is recorded as measured, never re-predicted): A1 the nav-arm
 * admission gated off → a1–a8, b1 b2 b3 b5 b6 b7 b8, corpus_c1, control3 AND control2a control2b control2c (20: every
 * fixture with a case-narrowed nav arm declines to the ternary REFUSAL, which also shrinks the emitted population — the
 * population pin, the doctored-pin precondition and the route identity fail as its CONSEQUENCE; the predicted 17 plus
 * those three) · A2 the default-arm law gated off → a1 a2 a3 a4 a6 a7 a8 a9 a10, b1 b3 b5 b6 b7 b8, corpus_c1, control3
 * AND the same three controls (20; a5 NoDefault, b2 {@code default empty} and b4 the filter block's boolean default stay
 * green) · A3 the option getter's bare-item admission gated off → {@code ChoiceSwitchLambdaOptionGetterSeatTest.e1} ALONE
 * (the seat-31 fixture PickItems, re-pinned at commit 7 to the released plugin's option-getter form — this suite carries
 * no bare-item arm over a model choice; a8's EXTENDS subject takes the instanceof form by construction) · B1 the
 * namespace-correct item type reverted → corpus_c1 control3 ALONE (the chaos cell's twelve {@code C18Either} namespaces;
 * every single-namespace fixture keeps the right binding) · C1 the literal block's dispatch gated off → a1 a10 corpus_c1
 * control3 · C2 the shared guard render's string quoting dropped → a1 a10 corpus_c1 control3 (predicted a1 a10; the chaos
 * {@code switched} alias carries the literal ladder too) · D1 the lambda-item subject gated off → a1 a2 a10 corpus_c1
 * control3 · D2 the list-literal receiver rule reverted to the #348 one-element clause → a1 a10 corpus_c1 control3 · E1
 * the filter-predicate block gated off → b4 AND control2a control2b (the refusal consequence again) · E2 the MULTI
 * switch-arm read of {@code isBodyMulti} removed → b2 ALONE · E3 the then-hoist's default admission reverted → b8 ALONE ·
 * F1 the option path walk replaced by single-hop membership → b1 AND control2a control2b · H1 the int-literal expected
 * type returning null → b6 ALONE · I1 the {@code LITERAL_SWITCH_TERNARY_STUB} refusal removed → control5 ALONE (round 2)
 * · G1 the IR leaf emitter's bound-local pre-escape removed (the commit-7 structural gate,
 * IDENTITY alone since round 1 — its unwitnessed text leg deleted; the anchor followed the code) → control2b ALONE, the
 * default route unmoved (the IR-ONLY lane, LAW 77). ONE LANE
 * MEASURED GREEN AND WITHDRAWN under the #614 law, as predicted: B2 the model-choice instanceof decline removed — the
 * fall-through is never entered, every model-choice carrier taking the option form; the decline stands as the guard,
 * not as a measured arm. Nine of the fifteen kept sets measured EXACTLY their prediction (A3 B1 D1 D2 E2 E3 H1 I1 G1);
 * six measured a SUPERSET (A1 A2 C1 E1 F1 by the three population / identity controls a refusal takes down — C1
 * since round 2 — and C2 by the chaos locks); no kept lane missed a predicted witness (the round-1 record's own
 * closing sentence, which named five supersets, is superseded by this one).
 */
class ChoiceSwitchLambdaSeatTest {

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
     * The twelve placement families of s18 that carry a {@code C18ToKind.java} — the declared rows' set
     * ({@code chaos-expected-divergence.txt}'s twelve FUNCTION rows of s18 before the seat; {@link #lockChaos}
     * enumerates the carriers from the golden tree and asserts the count and the per-family bijection).
     */
    private static final List<String> FAMILIES_12 = List.of(
            "a1o1", "a1o2", "a1o3", "a1o4", "a2alias", "a2dangle", "a2qual", "a2wild",
            "a3half", "a3hub", "a3third", "base");

    // =========================================================================
    // The fixtures — the oracle groups under census.seat7* namespaces
    // =========================================================================

    private static final String TYPES = """

            enum KindEnum: <"The chaos C18KindEnum.">
                Red
                Green
                Blue

            type OptA: <"Choice option A (the chaos C18OptA, widened by tags).">
                av string (1..1)
                tags string (0..*)

            type OptB: <"Choice option B (the chaos C18OptB).">
                bv number (1..1)

            choice Either: <"The choice subject (the chaos C18Either).">
                OptA
                OptB

            type Base: <"The EXTENDS-typed twin's base.">
                id string (1..1)

            type Sub1 extends Base: <"Subtype 1 - the nav arm's target.">
                a string (1..1)

            type Sub2 extends Base: <"Subtype 2 - the to-string arm's target.">
                b number (1..1)
            """;

    private static final String CS = "namespace census.seat7cs\n" + TYPES + """

            func Chaos: <"The chaos C18ToKind shape whole.">
                inputs:
                    raw string (1..1)
                    eths Either (0..*)
                output:
                    k KindEnum (0..1)
                alias switched: [raw] extract (item switch "r" then "Red", "g" then "Green", default "Blue")
                alias pulled: eths extract (item switch OptA then item -> av, OptB then item -> bv to-string, default "none")
                set k:
                    if (pulled count = switched count) or pulled is absent
                    then raw to-enum KindEnum

            func Pulled: <"The choice switch inside the extract lambda of an ALIAS.">
                inputs:
                    eths Either (0..*)
                output:
                    texts string (0..*)
                alias pulled: eths extract (item switch OptA then item -> av, OptB then item -> bv to-string, default "none")
                add texts: pulled

            func DirectAdd: <"The same switch lambda at a direct ADD seat.">
                inputs:
                    eths Either (0..*)
                output:
                    texts string (0..*)
                add texts:
                    eths extract (item switch OptA then item -> av, OptB then item -> bv to-string, default "none")

            func ExplicitParam: <"An explicit lambda parameter as the switch SUBJECT; the case arms read the narrowed ITEM.">
                inputs:
                    eths Either (0..*)
                output:
                    texts string (0..*)
                add texts:
                    eths extract eth [ eth switch OptA then item -> av, OptB then item -> bv to-string, default "none" ]

            func NoDefault: <"No default arm - the null terminal.">
                inputs:
                    eths Either (0..*)
                output:
                    texts string (0..*)
                add texts:
                    eths extract (item switch OptA then item -> av, OptB then item -> bv to-string)

            func SingleReceiver: <"A single receiver - the single-to-item form.">
                inputs:
                    eth Either (0..1)
                output:
                    text string (0..1)
                set text:
                    eth extract (item switch OptA then item -> av, OptB then item -> bv to-string, default "none")

            func Fmt: <"A helper the FnArm shape calls.">
                inputs:
                    s string (1..1)
                output:
                    out string (1..1)
                set out: s

            func FnArm: <"A function-call arm with a narrowed-nav argument beside a nav arm.">
                inputs:
                    eths Either (0..*)
                output:
                    texts string (0..*)
                add texts:
                    eths extract (item switch OptA then Fmt(item -> av), OptB then item -> bv to-string, default "none")

            func ExtendsSubject: <"The EXTENDS-typed twin - the same arms over an instanceof subject.">
                inputs:
                    bases Base (0..*)
                output:
                    texts string (0..*)
                add texts:
                    bases extract (item switch Sub1 then item -> a, Sub2 then item -> b to-string, default "none")

            func ControlEnum: <"The enum-keyed switch in a lambda (the #379 block with a non-empty default).">
                inputs:
                    kinds KindEnum (0..*)
                output:
                    hexes string (0..*)
                add hexes:
                    kinds extract (item switch Red then "ff0000", Green then "00ff00", default "0000ff")

            func ControlLiteral: <"The literal-keyed switch in a lambda over a list literal (the chaos switched alias alone).">
                inputs:
                    raw string (1..1)
                output:
                    texts string (0..*)
                alias switched: [raw] extract (item switch "r" then "Red", "g" then "Green", default "Blue")
                add texts: switched

            func ControlSetSeat: <"CONTROL - the choice switch at the SET seat, no lambda (the #394 ladder).">
                inputs:
                    eth Either (1..1)
                output:
                    out string (1..1)
                set out:
                    eth switch
                        OptA then item -> av,
                        OptB then item -> bv to-string,
                        default "none"
            """;

    private static final String CSE = "namespace census.seat7cse\n" + TYPES + """

            type OptC: <"A third option beside the nested choice.">
                cv string (1..1)

            choice Both: <"A nested choice - OptA is reachable through Either (the deep option hop).">
                Either
                OptC

            type Bag: <"The rule and datarule carrier.">
                eths Either (0..*)
                condition Texts:
                    if eths exists
                    then (eths extract [ item switch OptA then item -> av, OptB then item -> bv to-string, default "none" ] then count) <= eths count

            func NestedChoice: <"A case type reachable only through a nested choice option.">
                inputs:
                    boths Both (0..*)
                output:
                    texts string (0..*)
                add texts:
                    boths extract (item switch OptA then item -> av, OptC then item -> cv, default "none")

            func MultiArm: <"A MULTI nav arm (tags 0..*) - the list form, flattened.">
                inputs:
                    eths Either (0..*)
                output:
                    tags string (0..*)
                add tags:
                    eths extract (item switch OptA then item -> tags, default empty) then flatten

            func ThenChain: <"The switch extract then-chained into a filter.">
                inputs:
                    eths Either (0..*)
                output:
                    texts string (0..*)
                add texts:
                    eths extract (item switch OptA then item -> av, OptB then item -> bv to-string, default "none") then filter [ item <> "none" ]

            func Filtered: <"The choice switch as a FILTER predicate - boolean arms.">
                inputs:
                    eths Either (0..*)
                output:
                    kept Either (0..*)
                add kept:
                    eths filter [ item switch OptA then True, OptB then False, default False ]

            func InCondition: <"The switch lambda inside a function CONDITION.">
                inputs:
                    eths Either (0..*)
                output:
                    n int (1..1)
                condition HasText:
                    eths extract [ item switch OptA then item -> av, OptB then item -> bv to-string, default "none" ] then exists
                set n: eths count

            func NumberArms: <"Number-typed arms with an int literal and a literal default.">
                inputs:
                    eths Either (0..*)
                output:
                    nums number (0..*)
                add nums:
                    eths extract (item switch OptA then 1, OptB then item -> bv, default 0)

            reporting rule PickAv from Bag: <"The switch lambda in a REPORTING RULE body.">
                extract eths then extract (item switch OptA then item -> av, OptB then item -> bv to-string, default "none")
            """;

    // The goldens' own lines (the released 9.83.0 plugin's forms) — tabs count.
    private static final String OPTA_GUARD = "<OptA>map(\"getOptA\", either -> either.getOptA())";
    private static final String OPTB_GUARD = "<OptB>map(\"getOptB\", either -> either.getOptB())";
    private static final String OPTA_ARM = "optA.<String>map(\"getAv\", _optA -> _optA.getAv())";
    private static final String OPTB_ARM =
            "optB.<BigDecimal>map(\"getBv\", _optB -> _optB.getBv()).map(\"to-string\", Object::toString)";
    private static final String OF_NULL = "MapperS.<String>ofNull()";

    private static String tabs(int n) {
        return "\t".repeat(n);
    }

    /**
     * The option-nav block of the chaos shape as the golden renders it — the two option rungs with their case locals,
     * the null guard and the terminal — at the given depth (the tab count of the {@code .mapItem(} line).
     */
    private static String choiceBlock(String method, String param, int t, String armA, String armB, String terminal) {
        return tabs(t) + "." + method + "(" + param + " -> {\n"
                + tabs(t + 1) + "if (" + param + ".get() == null) {\n"
                + tabs(t + 2) + "return " + OF_NULL + ";\n"
                + tabs(t + 1) + "}\n"
                + tabs(t + 1) + "if (" + param + "." + OPTA_GUARD + ".get() != null) {\n"
                + tabs(t + 2) + "final MapperS<OptA> optA = " + param + "." + OPTA_GUARD + ";\n"
                + tabs(t + 2) + "return " + armA + ";\n"
                + tabs(t + 1) + "}\n"
                + tabs(t + 1) + "if (" + param + "." + OPTB_GUARD + ".get() != null) {\n"
                + tabs(t + 2) + "final MapperS<OptB> optB = " + param + "." + OPTB_GUARD + ";\n"
                + tabs(t + 2) + "return " + armB + ";\n"
                + tabs(t + 1) + "}\n"
                + tabs(t + 1) + "return " + terminal + ";\n"
                + tabs(t) + "})";
    }

    private static final String CHAOS_BLOCK = choiceBlock("mapItem", "item", 4, OPTA_ARM, OPTB_ARM, "MapperS.of(\"none\")");

    /** The literal-guard block of the chaos {@code switched} alias (the areEqual ladder). */
    private static final String LITERAL_BLOCK = tabs(4) + ".mapItem(item -> {\n"
            + tabs(5) + "if (item.get() == null) {\n"
            + tabs(6) + "return " + OF_NULL + ";\n"
            + tabs(5) + "}\n"
            + tabs(5) + "if (areEqual(item, MapperS.of(\"r\"), CardinalityOperator.All).get()) {\n"
            + tabs(6) + "return MapperS.of(\"Red\");\n"
            + tabs(5) + "}\n"
            + tabs(5) + "if (areEqual(item, MapperS.of(\"g\"), CardinalityOperator.All).get()) {\n"
            + tabs(6) + "return MapperS.of(\"Green\");\n"
            + tabs(5) + "}\n"
            + tabs(5) + "return MapperS.of(\"Blue\");\n"
            + tabs(4) + "})";

    // =========================================================================
    // (A) + (B) + (D) — the core group
    // =========================================================================

    /** a1: the chaos shape whole — both alias signatures MapperC<String>, the areEqual block, the option-nav block. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_chaosShapeWhole_bothAliasesAndBlocks() throws IOException {
        String code = fn("cs", "Chaos.java");
        assertContains(code, "\tprotected abstract MapperC<String> switched(String raw, List<? extends Either> eths);\n");
        assertContains(code, "\tprotected abstract MapperC<String> pulled(String raw, List<? extends Either> eths);\n");
        assertContains(code, "\t\tprotected MapperC<String> switched(String raw, List<? extends Either> eths) {\n"
                + "\t\t\treturn MapperC.<String>of(MapperS.of(raw))\n" + LITERAL_BLOCK + ";\n");
        assertContains(code, "\t\tprotected MapperC<String> pulled(String raw, List<? extends Either> eths) {\n"
                + "\t\t\treturn MapperC.<Either>of(eths)\n" + CHAOS_BLOCK + ";\n");
        assertContains(code, "k = MapperS.of(raw).checkedMap(\"to-enum\", KindEnum::fromDisplayName, IllegalArgumentException.class).get();");
        assertAbsent(code, "Objects.equals");
        assertAbsent(code, "instanceof");
        assertAbsent(code, "? extends string");
    }

    /** a2: the alias alone — the option-nav block, the MapperC<String> signature. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_aliasPulled_optionNavBlock() throws IOException {
        String code = fn("cs", "Pulled.java");
        assertContains(code, "\tprotected abstract MapperC<String> pulled(List<? extends Either> eths);\n");
        assertContains(code, "\t\t\treturn MapperC.<Either>of(eths)\n" + CHAOS_BLOCK + ";\n");
        assertContains(code, "\t\t\ttexts.addAll(pulled(eths).getMulti());\n");
        assertAbsent(code, "? extends string");
    }

    /** a3: the direct ADD seat. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a3_directAdd_optionNavBlock() throws IOException {
        String code = fn("cs", "DirectAdd.java");
        assertContains(code, "\t\t\ttexts.addAll(MapperC.<Either>of(eths)\n" + CHAOS_BLOCK + ".getMulti());\n");
    }

    /** a4: an explicit lambda parameter replaces `item` throughout; the arms read the narrowed item off the local. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a4_explicitParameter_replacesItemThroughout() throws IOException {
        String code = fn("cs", "ExplicitParam.java");
        assertContains(code, "\t\t\ttexts.addAll(MapperC.<Either>of(eths)\n"
                + choiceBlock("mapItem", "eth", 4, OPTA_ARM, OPTB_ARM, "MapperS.of(\"none\")") + ".getMulti());\n");
        assertAbsent(code, "item ->");
    }

    /** a5: no default — the ofNull terminal. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a5_noDefault_nullTerminal() throws IOException {
        String code = fn("cs", "NoDefault.java");
        assertContains(code, choiceBlock("mapItem", "item", 4, OPTA_ARM, OPTB_ARM, OF_NULL) + ".getMulti());\n");
    }

    /** a6: a single receiver — mapSingleToItem and the `.get()` collapse. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a6_singleReceiver_mapSingleToItem() throws IOException {
        String code = fn("cs", "SingleReceiver.java");
        assertContains(code, "\t\t\ttext = MapperS.of(eth)\n"
                + choiceBlock("mapSingleToItem", "item", 4, OPTA_ARM, OPTB_ARM, "MapperS.of(\"none\")") + ".get();\n");
        assertAbsent(code, "mapItem(");
    }

    /** a7: a function-call arm with a narrowed-nav argument — the explicit-args call's own MapperS.of wrap. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a7_functionCallArm_narrowedNavArgument() throws IOException {
        String code = fn("cs", "FnArm.java");
        assertContains(code, choiceBlock("mapItem", "item", 4,
                "MapperS.of(fmt.evaluate(" + OPTA_ARM + ".get()))", OPTB_ARM, "MapperS.of(\"none\")") + ".getMulti());\n");
        assertContains(code, "\t@Inject protected Fmt fmt;\n");
    }

    /** a8: the EXTENDS-typed twin — the instanceof form with the same arms off the wrapped cast var. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a8_extendsSubject_instanceofFormSameArms() throws IOException {
        String code = fn("cs", "ExtendsSubject.java");
        assertContains(code, "\t\t\ttexts.addAll(MapperC.<Base>of(bases)\n"
                + "\t\t\t\t.mapItem(item -> {\n"
                + "\t\t\t\t\tfinal Base switchArgument = item.get();\n"
                + "\t\t\t\t\tif (switchArgument == null) {\n"
                + "\t\t\t\t\t\treturn " + OF_NULL + ";\n"
                + "\t\t\t\t\t}\n"
                + "\t\t\t\t\tif (switchArgument instanceof Sub1) {\n"
                + "\t\t\t\t\t\tfinal Sub1 sub1 = (Sub1) switchArgument;\n"
                + "\t\t\t\t\t\treturn MapperS.of(sub1).<String>map(\"getA\", _sub1 -> _sub1.getA());\n"
                + "\t\t\t\t\t}\n"
                + "\t\t\t\t\tif (switchArgument instanceof Sub2) {\n"
                + "\t\t\t\t\t\tfinal Sub2 sub2 = (Sub2) switchArgument;\n"
                + "\t\t\t\t\t\treturn MapperS.of(sub2).<BigDecimal>map(\"getB\", _sub2 -> _sub2.getB()).map(\"to-string\", Object::toString);\n"
                + "\t\t\t\t\t}\n"
                + "\t\t\t\t\treturn MapperS.of(\"none\");\n"
                + "\t\t\t\t}).getMulti());\n");
    }

    /** a9: the ENUM-keyed switch in a lambda — the #379 block with the non-empty default (the oracle's control finding). */
    @Test
    @EnabledIf("builtinsAvailable")
    void a9_enumSwitchInLambda_blockWithLiteralDefault() throws IOException {
        String code = fn("cs", "ControlEnum.java");
        assertContains(code, "\t\t\thexes.addAll(MapperC.<KindEnum>of(kinds)\n"
                + "\t\t\t\t.mapItem(item -> {\n"
                + "\t\t\t\t\tfinal KindEnum switchArgument = item.get();\n"
                + "\t\t\t\t\tif (switchArgument == null) {\n"
                + "\t\t\t\t\t\treturn " + OF_NULL + ";\n"
                + "\t\t\t\t\t}\n"
                + "\t\t\t\t\tif (switchArgument == KindEnum.RED) {\n"
                + "\t\t\t\t\t\treturn MapperS.of(\"ff0000\");\n"
                + "\t\t\t\t\t}\n"
                + "\t\t\t\t\tif (switchArgument == KindEnum.GREEN) {\n"
                + "\t\t\t\t\t\treturn MapperS.of(\"00ff00\");\n"
                + "\t\t\t\t\t}\n"
                + "\t\t\t\t\treturn MapperS.of(\"0000ff\");\n"
                + "\t\t\t\t}).getMulti());\n");
        assertAbsent(code, "Objects.equals");
    }

    /** a10: the LITERAL-keyed switch in a lambda — the areEqual block and the MapperC<String> signature (the control finding). */
    @Test
    @EnabledIf("builtinsAvailable")
    void a10_literalSwitchInLambda_areEqualBlockAndSignature() throws IOException {
        String code = fn("cs", "ControlLiteral.java");
        assertContains(code, "\tprotected abstract MapperC<String> switched(String raw);\n");
        assertContains(code, "\t\t\treturn MapperC.<String>of(MapperS.of(raw))\n" + LITERAL_BLOCK + ";\n");
        assertContains(code, "import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;\n");
        assertContains(code, "import com.rosetta.model.lib.expression.CardinalityOperator;\n");
        assertAbsent(code, "Objects.equals");
        assertAbsent(code, "? extends string");
    }

    /** a11: CONTROL — the SET-seat option-nav ladder (no lambda; the #394 form the seat must not move). */
    @Test
    @EnabledIf("builtinsAvailable")
    void a11_control_setSeatLadder_keepsThePreSeatForm() throws IOException {
        String code = fn("cs", "ControlSetSeat.java");
        assertContains(code, "\t\t\tfinal MapperS<Either> switchArgument = MapperS.of(eth);\n"
                + "\t\t\tif (switchArgument.get() == null) {\n"
                + "\t\t\t\tout = null;\n"
                + "\t\t\t} else if (switchArgument." + OPTA_GUARD + ".get() != null) {\n"
                + "\t\t\t\tfinal MapperS<OptA> optA = switchArgument." + OPTA_GUARD + ";\n"
                + "\t\t\t\tout = " + OPTA_ARM + ".get();\n"
                + "\t\t\t} else if (switchArgument." + OPTB_GUARD + ".get() != null) {\n"
                + "\t\t\t\tfinal MapperS<OptB> optB = switchArgument." + OPTB_GUARD + ";\n"
                + "\t\t\t\tout = " + OPTB_ARM + ".get();\n"
                + "\t\t\t} else {\n"
                + "\t\t\t\tout = \"none\";\n"
                + "\t\t\t}\n");
    }

    // =========================================================================
    // (C) + (E) — the edge group
    // =========================================================================

    /** b1: a case type reachable only through the nested choice — the deep option hop. */
    @Test
    @EnabledIf("builtinsAvailable")
    void b1_nestedChoice_deepOptionHop() throws IOException {
        String code = fn("cse", "NestedChoice.java");
        String deepGuard = "item.<Either>map(\"getEither\", both -> both.getEither())." + OPTA_GUARD;
        String optcGuard = "item.<OptC>map(\"getOptC\", both -> both.getOptC())";
        assertContains(code, "\t\t\ttexts.addAll(MapperC.<Both>of(boths)\n"
                + "\t\t\t\t.mapItem(item -> {\n"
                + "\t\t\t\t\tif (item.get() == null) {\n"
                + "\t\t\t\t\t\treturn " + OF_NULL + ";\n"
                + "\t\t\t\t\t}\n"
                + "\t\t\t\t\tif (" + deepGuard + ".get() != null) {\n"
                + "\t\t\t\t\t\tfinal MapperS<OptA> optA = " + deepGuard + ";\n"
                + "\t\t\t\t\t\treturn " + OPTA_ARM + ";\n"
                + "\t\t\t\t\t}\n"
                + "\t\t\t\t\tif (" + optcGuard + ".get() != null) {\n"
                + "\t\t\t\t\t\tfinal MapperS<OptC> optC = " + optcGuard + ";\n"
                + "\t\t\t\t\t\treturn optC.<String>map(\"getCv\", _optC -> _optC.getCv());\n"
                + "\t\t\t\t\t}\n"
                + "\t\t\t\t\treturn MapperS.of(\"none\");\n"
                + "\t\t\t\t}).getMulti());\n");
    }

    /** b2: a MULTI nav arm — the list form over a MULTI receiver, the MapperListOfLists then-arg, flattened. */
    @Test
    @EnabledIf("builtinsAvailable")
    void b2_multiArm_listFormIntoAListOfLists() throws IOException {
        String code = fn("cse", "MultiArm.java");
        assertContains(code, "import com.rosetta.model.lib.mapper.MapperListOfLists;\n");
        assertContains(code, "\t\t\tfinal MapperListOfLists<String> thenArg = MapperC.<Either>of(eths)\n"
                + "\t\t\t\t.mapItemToList(item -> {\n"
                + "\t\t\t\t\tif (item.get() == null) {\n"
                + "\t\t\t\t\t\treturn MapperC.<String>ofNull();\n"
                + "\t\t\t\t\t}\n"
                + "\t\t\t\t\tif (item." + OPTA_GUARD + ".get() != null) {\n"
                + "\t\t\t\t\t\tfinal MapperS<OptA> optA = item." + OPTA_GUARD + ";\n"
                + "\t\t\t\t\t\treturn optA.<String>mapC(\"getTags\", _optA -> _optA.getTags());\n"
                + "\t\t\t\t\t}\n"
                + "\t\t\t\t\treturn MapperC.<String>ofNull();\n"
                + "\t\t\t\t});\n");
        assertContains(code, "\t\t\ttags.addAll(thenArg\n\t\t\t\t.flattenList().getMulti());\n");
    }

    /** b3: the switch extract then-piped into a filter — the MapperC then-arg hoist. */
    @Test
    @EnabledIf("builtinsAvailable")
    void b3_thenChain_thenArgHoist() throws IOException {
        String code = fn("cse", "ThenChain.java");
        assertContains(code, "\t\t\tfinal MapperC<String> thenArg = MapperC.<Either>of(eths)\n" + CHAOS_BLOCK + ";\n");
        assertContains(code, "\t\t\ttexts.addAll(thenArg\n"
                + "\t\t\t\t.filterItemNullSafe(item -> notEqual(item, MapperS.of(\"none\"), CardinalityOperator.Any).get()).getMulti());\n");
    }

    /** b4: the choice switch as a FILTER predicate — the Boolean block, the unread case locals, the bare null guard. */
    @Test
    @EnabledIf("builtinsAvailable")
    void b4_filterPredicate_booleanBlock() throws IOException {
        String code = fn("cse", "Filtered.java");
        assertContains(code, "\t\t\tkept.addAll(toBuilder(MapperC.<Either>of(eths)\n"
                + "\t\t\t\t.filterItemNullSafe(item -> {\n"
                + "\t\t\t\t\tif (item.get() == null) {\n"
                + "\t\t\t\t\t\treturn null;\n"
                + "\t\t\t\t\t}\n"
                + "\t\t\t\t\tif (item." + OPTA_GUARD + ".get() != null) {\n"
                + "\t\t\t\t\t\tfinal MapperS<OptA> optA = item." + OPTA_GUARD + ";\n"
                + "\t\t\t\t\t\treturn true;\n"
                + "\t\t\t\t\t}\n"
                + "\t\t\t\t\tif (item." + OPTB_GUARD + ".get() != null) {\n"
                + "\t\t\t\t\t\tfinal MapperS<OptB> optB = item." + OPTB_GUARD + ";\n"
                + "\t\t\t\t\t\treturn false;\n"
                + "\t\t\t\t\t}\n"
                + "\t\t\t\t\treturn false;\n"
                + "\t\t\t\t}).getMulti()));\n");
    }

    /** b5: the switch lambda inside a function CONDITION — the then-arg hoist and the CR coercion. */
    @Test
    @EnabledIf("builtinsAvailable")
    void b5_functionCondition_thenArgHoistAndCoercion() throws IOException {
        String code = fn("cse", "InCondition.java");
        assertContains(code, "\t\tconditionValidator.validate(() -> {\n"
                + "\t\t\tfinal MapperC<String> thenArg = MapperC.<Either>of(eths)\n" + CHAOS_BLOCK + ";\n"
                + "\t\t\treturn ComparisonResult.ofNullSafe(exists(thenArg).asMapper());\n"
                + "\t\t},\n");
        assertContains(code, "\t\t\tn = MapperC.<Either>of(eths).resultCount();\n");
    }

    /** b6: number-typed arms — the int literals coerced to the switch's BigDecimal join. */
    @Test
    @EnabledIf("builtinsAvailable")
    void b6_numberArms_intLiteralsCoerced() throws IOException {
        String code = fn("cse", "NumberArms.java");
        assertContains(code, "\t\t\tnums.addAll(MapperC.<Either>of(eths)\n"
                + "\t\t\t\t.mapItem(item -> {\n"
                + "\t\t\t\t\tif (item.get() == null) {\n"
                + "\t\t\t\t\t\treturn MapperS.<BigDecimal>ofNull();\n"
                + "\t\t\t\t\t}\n"
                + "\t\t\t\t\tif (item." + OPTA_GUARD + ".get() != null) {\n"
                + "\t\t\t\t\t\tfinal MapperS<OptA> optA = item." + OPTA_GUARD + ";\n"
                + "\t\t\t\t\t\treturn MapperS.of(BigDecimal.valueOf(1));\n"
                + "\t\t\t\t\t}\n"
                + "\t\t\t\t\tif (item." + OPTB_GUARD + ".get() != null) {\n"
                + "\t\t\t\t\t\tfinal MapperS<OptB> optB = item." + OPTB_GUARD + ";\n"
                + "\t\t\t\t\t\treturn optB.<BigDecimal>map(\"getBv\", _optB -> _optB.getBv());\n"
                + "\t\t\t\t\t}\n"
                + "\t\t\t\t\treturn MapperS.of(BigDecimal.valueOf(0));\n"
                + "\t\t\t\t}).getMulti());\n");
    }

    /** b7: the switch lambda in a REPORTING RULE body. */
    @Test
    @EnabledIf("builtinsAvailable")
    void b7_reportingRule_optionNavBlock() throws IOException {
        String code = pick(render("cse"), "reports/PickAvRule.java", "PickAvRule.java");
        assertContains(code, "\t\t\tfinal MapperC<Either> thenArg = MapperS.of(input)\n"
                + "\t\t\t\t.mapSingleToList(item -> item.<Either>mapC(\"getEths\", bag -> bag.getEths()));\n");
        assertContains(code, "\t\t\toutput = thenArg\n" + CHAOS_BLOCK + ".getMulti();\n");
    }

    /** b8: the type condition (the datarule path) — the then-arg hoist through the widened base admission. */
    @Test
    @EnabledIf("builtinsAvailable")
    void b8_typeCondition_theDataRuleHoistsTheThenArg() throws IOException {
        String code = pick(render("cse"), "validation/datarule/BagTexts.java", "BagTexts.java");
        assertContains(code, "\t\t\t\t\tfinal MapperC<String> thenArg = MapperS.of(bag).<Either>mapC(\"getEths\", _bag -> _bag.getEths())\n"
                + choiceBlock("mapItem", "item", 6, OPTA_ARM, OPTB_ARM, "MapperS.of(\"none\")") + ";\n"
                + "\t\t\t\t\treturn lessThanEquals(MapperS.of(thenArg.resultCount()), MapperS.of(MapperS.of(bag).<Either>mapC(\"getEths\", _bag -> _bag.getEths()).resultCount()), CardinalityOperator.All);\n");
        assertAbsent(code, ".then(item ->");
    }

    // =========================================================================
    // The controls
    // =========================================================================

    /** control2a — the default route's population pins (the declared names read off the PARSED model). */
    private static final Map<String, Integer> POPULATION = Map.of("cs", 12, "cse", 6);
    /** Every fixture set this class renders — the ONE list {@link #source} dispatches over. */
    private static final List<String> FIXTURE_SETS = List.of("cs", "cse");

    @Test
    @EnabledIf("builtinsAvailable")
    void control2a_defaultRoute_populationPins() throws IOException {
        assertEquals(new java.util.TreeSet<>(FIXTURE_SETS), new java.util.TreeSet<>(POPULATION.keySet()),
                "every fixture set the class dispatches over must carry a population pin");
        for (Map.Entry<String, Integer> pin : POPULATION.entrySet()) {
            Render r = render(pin.getKey());
            List<String> keys = fixtureFunctionKeys(r);
            assertEquals(pin.getValue().intValue(), keys.size(),
                    "fixture set " + pin.getKey() + " emits " + pin.getValue() + " functions on the default route; rendered " + keys);
            java.util.Set<String> declared = declaredFunctionNames(source(pin.getKey()), pin.getKey());
            java.util.Set<String> emitted = new java.util.TreeSet<>();
            for (String k : keys) {
                String simple = k.substring(k.lastIndexOf('/') + 1);
                emitted.add(simple.endsWith(".java") ? simple.substring(0, simple.length() - 5) : simple);
            }
            assertEquals(declared, emitted, "fixture set " + pin.getKey() + ": the emitted function SET must be the declared one");
            assertTrue(r.errors().isEmpty(), "fixture set " + pin.getKey() + " generation errors: " + r.errors());
        }
        Render edge = render("cse");
        assertEquals(1, edge.output().keySet().stream().filter(k -> k.endsWith("/reports/PickAvRule.java")).count(), "the rule renders");
        assertEquals(1, edge.output().keySet().stream().filter(k -> k.endsWith("/validation/datarule/BagTexts.java")).count(), "the datarule renders");
    }

    /** control2c — control2a's pins proven INPUT-SENSITIVE on a doctored fixture (a deletion and a rename). */
    @Test
    @EnabledIf("builtinsAvailable")
    void control2c_populationPin_failsOnADoctoredFixture() throws IOException {
        String full = source("cs");
        int cut = full.lastIndexOf("func ControlSetSeat:");
        assertTrue(cut > 0, "the cs fixture ends with ControlSetSeat");
        String doctored = full.substring(0, cut);
        java.util.Set<String> declared = declaredFunctionNames(doctored, "cs-doctored");
        assertEquals(POPULATION.get("cs") - 1, declared.size(), "the doctored fixture declares one function fewer");
        assertTrue(!declared.contains("ControlSetSeat"), "ControlSetSeat is the deleted one");
        Render r = renderSource(doctored, "cs", "cs-doctored", false);
        assertEquals(POPULATION.get("cs") - 1, fixtureFunctionKeys(r).size(),
                "the rendered population moves with the input - the pin can fail: " + fixtureFunctionKeys(r));
        String renamed = full.replace("func ControlSetSeat:", "func ControlSetSeatRenamed:");
        assertTrue(!renamed.equals(full), "the rename must change a byte");
        java.util.Set<String> renamedDeclared = declaredFunctionNames(renamed, "cs-renamed");
        assertEquals(POPULATION.get("cs").intValue(), renamedDeclared.size(), "the renamed fixture keeps the count");
        assertTrue(!renamedDeclared.equals(declaredFunctionNames(full, "cs")), "the renamed fixture moves the SET");
        Render rr = renderSource(renamed, "cs", "cs-renamed", false);
        java.util.Set<String> renamedEmitted = new java.util.TreeSet<>();
        for (String k : fixtureFunctionKeys(rr)) {
            String simple = k.substring(k.lastIndexOf('/') + 1);
            renamedEmitted.add(simple.endsWith(".java") ? simple.substring(0, simple.length() - 5) : simple);
        }
        assertEquals(renamedDeclared, renamedEmitted, "the emitted SET follows the declared one under the rename");
        assertTrue(renamedEmitted.contains("ControlSetSeatRenamed") && !renamedEmitted.contains("ControlSetSeat"),
                "the SET assertion sees the rename where the count cannot: " + renamedEmitted);
    }

    private static java.util.Set<String> declaredFunctionNames(String source, String label) {
        RModel model = AstBuilder.buildFromString(source, "seat7" + label + ".rosetta");
        java.util.Set<String> names = new java.util.TreeSet<>();
        for (RFunction f : AstWalker.findAll(model, RFunction.class)) {
            names.add(f.name());
        }
        return names;
    }

    /**
     * control2b — LAW 77: the IR route renders every fixture set byte-identically to the default route (the same file
     * set, the same bytes per file — functions, the rule and the datarule). Runs only with the IR provider on the
     * classpath ({@code -Pir-on} — the chain's ON-route seat step); the OFF gensuite reports it skipped.
     */
    @Test
    @EnabledIf("builtinsAndIrProviderAvailable")
    void control2b_irRoute_rendersEveryFixtureIdenticallyToTheDefaultRoute() throws IOException {
        for (String set : POPULATION.keySet()) {
            Render def = render(set);
            Render ir = renderIr(set);
            assertTrue(ir.errors().isEmpty(), set + ": IR-route generation errors: " + ir.errors());
            List<String> defKeys = def.output().keySet().stream().sorted().toList();
            assertEquals(defKeys, ir.output().keySet().stream().sorted().toList(), set + ": the two routes must emit the SAME file set");
            for (String key : defKeys) {
                assertEquals(normalize(def.output().get(key)), normalize(ir.output().get(key)),
                        set + ": the IR route must agree with the default route for " + key);
            }
        }
    }

    /** corpus_c1: the twelve s18 C18ToKind variants byte-identical to the chaos goldens. */
    @Test
    @EnabledIf("chaosAvailable")
    void corpus_c1_C18ToKind_allTwelveVariants() throws IOException {
        lockChaos("s18", "C18ToKind", FAMILIES_12);
    }

    /** control3: the whole-file lock is proven able to fail — a doctored golden is reported by name. */
    @Test
    @EnabledIf("chaosAvailable")
    void control3_doctoredGolden_isReportedByTheLock() throws IOException {
        assertNotNull(chaosOutput, "chaos cell generation did not run — corpus unavailable?");
        String carrier = "chaos/s18/base/functions/C18ToKind.java";
        String generated = chaosOutput.get(carrier);
        assertNotNull(generated, "not generated: " + carrier);
        String golden = normalize(Files.readString(CHAOS_GOLDEN.resolve(carrier)));
        assertTrue(compareCarrier(carrier, golden, generated) == null,
                "the undoctored carrier must lock green before the doctor is applied");
        String doctored = golden.replace("_c18OptA -> _c18OptA.getAv()", "_c18OptA -> _c18OptA.getAvv()");
        assertTrue(!doctored.equals(golden), "the doctor must change a byte");
        String verdict = compareCarrier(carrier, doctored, generated);
        assertNotNull(verdict, "a doctored golden MUST be reported by the lock");
        assertContains(verdict, "getAvv");
    }

    /**
     * control5 (round 2, the code-quality review's MF-1): the LOUD register's literal twin proven able to fire — a
     * literal-keyed switch over the lambda item whose arm the literal-guard block declines (a NUMBER literal under
     * literal guards, the round-1 SF-6 narrowing) reaches {@code ControlFlowHandler}'s ternary and is REFUSED at
     * {@code LITERAL_SWITCH_TERNARY_STUB} instead of emitting the always-false {@code Objects.equals} guard the seat's
     * literal-keyed control (a10) measured before the block existed. The register is reset before and after (it is
     * JVM-global and the module reuses forks).
     * v3.2 seat 12 (D52): the switch is written DIRECTLY at the ADD seat - the round-2 fixture carried it in an alias
     * ({@code alias graded: [raw] extract (…)}) whose SIGNATURE now refuses first at
     * {@code ALIAS_SIGNATURE_RAW_TYPE} (the signature walk declines the literal-arm switch and the number output's
     * raw name is a builtin; the alias signature renders before the alias body, so that site wins the file -
     * measured at the seat-12 counter tree, scratch/seat-c3-suites-run1.log). Without the alias there is no
     * signature walk and the SAME switch shape reaches the SAME body seat: the control keeps proving THIS site.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void control5_literalSwitchInLambda_declinedArm_refusedLoud() throws IOException {
        SilentDegradation.reset();
        try {
            List<RModel> models = new ArrayList<>();
            models.add(model(String.join("\n",
                    "namespace census.seat7ctl5",
                    "version \"0.0.0\"",
                    "",
                    "func Grade:",
                    "    inputs:",
                    "        raw string (1..1)",
                    "    output:",
                    "        out number (0..*)",
                    "    add out: [raw] extract (item switch \"r\" then 1.5, default 2.5)"), "seat7ctl5.rosetta"));
            models.addAll(loadBuiltinsOnly());
            RWorkspace workspace = RWorkspace.build(models).workspace();
            GeneratorModel gm = new GeneratorModel(workspace, m -> m.namespace().startsWith("census.seat7ctl5"));
            JavaTypeUtil typeUtil = new JavaTypeUtil();
            JavaTypeTranslator tt = new JavaTypeTranslator(typeUtil);
            FunctionGenerator funcGen = new FunctionGenerator(gm, tt, typeUtil);
            Map<String, String> out = new LinkedHashMap<>();
            List<GenerationException> errors = funcGen.generateWithErrors(out);
            assertEquals(1, errors.size(), "exactly one refusal: " + errors);
            assertTrue(errors.get(0) instanceof SilentDegradation.Refusal r
                    && r.site() == SilentDegradation.Site.LITERAL_SWITCH_TERNARY_STUB, errors.toString());
            assertEquals("census/seat7ctl5/functions/Grade.java", errors.get(0).getTargetPath());
            assertTrue(!out.containsKey("census/seat7ctl5/functions/Grade.java"), "the refused function does not emit");
            assertEquals(1, SilentDegradation.counts().get(SilentDegradation.Site.LITERAL_SWITCH_TERNARY_STUB));
            assertTrue(SilentDegradation.witnesses(SilentDegradation.Site.LITERAL_SWITCH_TERNARY_STUB).get(0)
                    .contains("literal-keyed switch over a lambda-bound subject"));
        } finally {
            SilentDegradation.reset();
        }
    }

    private static RModel model(String source, String fileName) {
        RModel m = AstBuilder.buildFromString(source, fileName);
        m.setVersion("0.0.0.test");
        return m;
    }

    /** control4: the placement-family identity pin proven able to fail (over the twelve-family s18 set). */
    @Test
    void control4_placementFamilyPin_canFail() {
        assertEquals("a3half", placementFamily("a3half/p2"));
        assertEquals("a3third", placementFamily("a3third/p3"));
        assertEquals("base", placementFamily("base"));
        List<String> good = List.of("a1o1", "a1o2", "a1o3", "a1o4", "a2alias", "a2dangle", "a2qual", "a2wild",
                "a3half/p2", "a3hub/p2", "a3third/p3", "base").stream()
                .map(v -> "chaos/s99/" + v + "/functions/X.java").toList();
        assertTrue(placementFamilyVerdict("s99", good, FAMILIES_12) == null, "the enumerated twelve must pass");
        List<String> doctored = new ArrayList<>(good);
        doctored.set(doctored.indexOf("chaos/s99/a3third/p3/functions/X.java"), "chaos/s99/a3half/p1/functions/X.java");
        String verdict = placementFamilyVerdict("s99", doctored, FAMILIES_12);
        assertNotNull(verdict, "a doctored population MUST be reported");
        String found = verdict.substring(verdict.indexOf("found "));
        assertTrue(!found.contains("a3third"), "the missing family must be absent from the found list: " + found);
        assertTrue(found.indexOf("a3half") != found.lastIndexOf("a3half"), "the doubled family must appear twice: " + found);
    }

    // =========================================================================
    // Fixture harness — functions, rules and datarules, both routes (the seat-6 harness verbatim)
    // =========================================================================

    private record Render(Map<String, String> output, List<String> errors) { }

    private static final Map<String, Render> RENDERED = new LinkedHashMap<>();
    private static final Map<String, Render> RENDERED_IR = new LinkedHashMap<>();

    private static String source(String set) {
        assertTrue(FIXTURE_SETS.contains(set), "an undeclared fixture set: " + set + " (add it to FIXTURE_SETS and POPULATION)");
        return switch (set) {
            case "cs" -> CS;
            case "cse" -> CSE;
            default -> throw new IllegalArgumentException(set);
        };
    }

    private static Render render(String set) throws IOException {
        Render r = RENDERED.get(set);
        if (r == null) {
            r = renderModel(set, false);
            RENDERED.put(set, r);
        }
        return r;
    }

    private static Render renderIr(String set) throws IOException {
        Render r = RENDERED_IR.get(set);
        if (r != null) {
            return r;
        }
        String previous = System.getProperty(IRGeneration.PROPERTY);
        System.setProperty(IRGeneration.PROPERTY, "true");
        try {
            assertNotNull(IRGeneration.providerOrNull(),
                    "the IR provider must be resolvable under -Pir-on, else this is not an ON-route render");
            r = renderModel(set, true);
        } finally {
            if (previous == null) {
                System.clearProperty(IRGeneration.PROPERTY);
            } else {
                System.setProperty(IRGeneration.PROPERTY, previous);
            }
        }
        RENDERED_IR.put(set, r);
        return r;
    }

    private static Render renderModel(String set, boolean irRoute) throws IOException {
        return renderSource(source(set), set, set, irRoute);
    }

    /** Renders {@code source} under set {@code set}'s namespace filter; {@code label} names the model file (control2c). */
    private static Render renderSource(String source, String set, String label, boolean irRoute) throws IOException {
        RModel main = AstBuilder.buildFromString(source, "seat7" + label + ".rosetta");
        main.setVersion("0.0.0.test");
        List<RModel> models = new ArrayList<>();
        models.add(main);
        models.addAll(loadBuiltinsOnly());
        RLinkingResult linked = RWorkspace.build(models);
        RWorkspace workspace = linked.workspace();
        String namespace = "census.seat7" + set;
        GeneratorModel gm = new GeneratorModel(workspace, m -> namespace.equals(m.namespace()));
        JavaTypeUtil typeUtil = new JavaTypeUtil();
        JavaTypeTranslator tt = new JavaTypeTranslator(typeUtil);
        FunctionGenerator fg = irRoute
                ? IRGeneration.functionGenerator(gm, tt, typeUtil)
                : new FunctionGenerator(gm, tt, typeUtil);
        if (irRoute) {
            assertTrue(!fg.getClass().equals(FunctionGenerator.class),
                    "the seam must hand back the IR-route FunctionGenerator, got " + fg.getClass());
        }
        RuleGenerator ruleGen = new RuleGenerator(gm, tt, fg);
        ReportGenerator reportGen = new ReportGenerator(gm, tt, fg);
        DataRuleGenerator dataRuleGen = new DataRuleGenerator(gm, tt, typeUtil);
        Map<String, String> out = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        for (RModel model : workspace.files()) {
            if (gm.shouldGenerate(model)) {
                String version = gm.version(model);
                collect(errors, ruleGen.generateClasses(model, version, out));
                collect(errors, reportGen.generateClasses(model, version, out));
                collect(errors, dataRuleGen.generateClasses(model, version, out));
            }
        }
        fg.generateWithErrors(out).forEach(e -> errors.add(e.getTargetPath() + " - " + e));
        return new Render(out, errors);
    }

    private static List<String> fixtureFunctionKeys(Render r) {
        return r.output().keySet().stream()
                .filter(k -> k.contains("/functions/"))
                .sorted()
                .toList();
    }

    /** Functions land under {@code .../functions/}. */
    private static String fn(String set, String fileName) throws IOException {
        return pick(render(set), "functions/" + fileName, fileName);
    }

    private static String pick(Render r, String suffix, String fileName) {
        List<String> own = r.errors().stream().filter(e -> e.contains(fileName)).toList();
        assertTrue(own.isEmpty(),
                "the generator reported errors for " + fileName + " (a broken fixture must fail"
                + " loudly, not skip): " + own);
        List<String> matches = r.output().keySet().stream()
                .filter(k -> k.endsWith("/" + suffix))
                .sorted()
                .toList();
        assertTrue(!matches.isEmpty(), "not generated: " + fileName + " (have: " + r.output().keySet() + ")");
        assertEquals(1, matches.size(), "exactly ONE emitted file may match " + suffix + ": " + matches);
        return normalize(r.output().get(matches.get(0)));
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
     * named difference — the ONE comparison every whole-class lock and control3 go through.
     */
    private static String compareCarrier(String carrier, String golden, String generated) {
        String gen = normalize(generated);
        return golden.equals(gen) ? null
                : carrier + ": differs from its golden — " + firstDifference(golden, gen);
    }

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
            throw new AssertionError("[ChoiceSwitchLambdaSeatTest] builtins parse failures: "
                    + String.join("; ", failures));
        }
        return models;
    }

    // =========================================================================
    // The chaos cell — generated once (functions, rules, reports), every carrier locked whole
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
    private static String placementFamilyVerdict(String seat, List<String> carriers, List<String> expectedFamilies) {
        String prefix = "chaos/" + seat + "/";
        List<String> families = carriers.stream()
                .map(c -> c.substring(prefix.length(), c.indexOf("/functions/")))
                .map(ChoiceSwitchLambdaSeatTest::placementFamily)
                .sorted()
                .toList();
        List<String> expected = expectedFamilies.stream().sorted().toList();
        return expected.equals(families) ? null
                : "expected one carrier per placement family " + expected + ", found " + families;
    }

    /**
     * Byte-lock every placement variant of one declared function class against the chaos goldens. A
     * carrier the generator refused, did not emit, or emitted differently fails by NAME.
     */
    private static void lockChaos(String seat, String simpleName, List<String> families) throws IOException {
        assertNotNull(chaosOutput, "chaos cell generation did not run — corpus unavailable?");
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
        assertEquals(families.size(), carriers.size(),
                "expected the declared " + families.size() + " placement variants of " + simpleName + " under " + seatRoot
                + ", found " + carriers);
        String familyVerdict = placementFamilyVerdict(seat, carriers, families);
        assertTrue(familyVerdict == null,
                "the carriers of " + simpleName + " must be one per placement family: " + familyVerdict);
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
        assertTrue(failures.isEmpty(), "carriers of " + simpleName + " not byte-identical to the chaos"
                + " goldens (" + failures.size() + "/" + families.size() + "):\n  "
                + String.join("\n  ", failures));
    }
}
