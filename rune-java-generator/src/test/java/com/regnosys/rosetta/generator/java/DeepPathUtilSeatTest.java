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
 * v3.2 seat 6 (PR #627): the seat suite of F5 — a deep feature call ({@code ->>}) whose RECEIVER is a list-op
 * LAMBDA'S ITEM (the chaos {@code C3Commons} shape, {@code outers extract item ->> text}), 11 declared D11 rows
 * healed on BOTH routes under two oracle groups pinned from the released 9.83.0 plugin BEFORE the code (the census
 * {@code target/v32-seat6-instruments/f5-census.md}, local).
 *
 * <ul>
 *   <li><b>(A) the injection</b> — the released plugin injects the receiver type's {@code <Type>DeepPathUtil}
 *       ({@code @Inject protected OuterDeepPathUtil outerDeepPathUtil;} + its import + {@code javax.inject.Inject})
 *       and routes the hop through it ({@code outer -> outerDeepPathUtil.chooseText(outer)},
 *       {@code ExpressionGenerator.attributeCall} / {@code JavaDependencyProvider}); the fork rendered the
 *       {@code TODO(M7b-4): wire DeepPathUtil} comment placeholder with no field. The WITNESS half already typed the
 *       item ({@code resolveDeepFeature} over {@code resolveReceiverDataType}'s implicit-variable arm — the render
 *       carried {@code <String>} and {@code map}); the ADMISSION half ({@code resolveDeepReceiverJavaClass}, the ONE
 *       resolver the render's field / lambda var AND {@code FunctionDependencyCollector}'s {@code @Inject} consult)
 *       walked {@code resolveReceiverRType}, which has no lambda-item arm. ONE rung heals both halves:
 *       {@code NavigationHandler.lambdaItemReceiverRType} — the implicit item or the explicit closure parameter
 *       ({@code ReferenceHandler.enclosingClosureParamOwner}) resolved to the owning argument's ELEMENT
 *       ({@code lambdaOwnerArgument}, the SAME owner walk the naming reads, through {@code recoverThenArgItemRType}).</li>
 *   <li><b>(B) the map method</b> — the extract chooser's body read ({@code CollectionHandler.isBodyMulti}) took the
 *       parser computer's GLOBAL blanket MULTI for any deep call first ({@code CardinalityComputer:155}, the #443
 *       split, DO-NOT-TOUCH), so a single deep feature selected {@code mapItemToList} / {@code mapSingleToList} where
 *       the plugin selects {@code mapItem} / {@code mapSingleToItem} (and {@code mapItemToList} + {@code mapC} only
 *       for a MULTI feature). {@code NavigationHandler.deepBodyProvesMulti} CONSULTS the deep arm of
 *       {@code chainProvesMulti} (the render's own map-method chooser, {@code resolveDeepMapMethod} over
 *       {@code resolveDeepFeature}; ONE declaration since round 1) — upstream's {@code isFeatureMulti(feature)} term;
 *       the {@code isMulti(receiver)} term is NOT mirrored (round 1: measured empty by lane B2, the seat-30
 *       precedent, BANKED with its oracle group named in the seat plan).</li>
 * </ul>
 *
 * <p>The fixtures below mirror the seat's ORACLE groups ({@code holdout/deep-path-util-injection}, {@code -edge} — byte-locked
 * whole by {@code HoldOutByteCompareTest}) under {@code census.seat6*} namespaces; each expected string is the golden's own
 * line (the namespace never appears in a function body except through the util import, which the test swaps), so every
 * assertion is a mutation witness, not a guess. The controls: a9 / a10 are the DIRECT-receiver shapes the fix must NOT
 * move (byte-identical before and after); control2a pins the default route's populations; control2b is LAW 77 on the IR
 * route (runs under {@code -Pir-on} — the chain's ON-route seat step; the datarule generator has no IR twin, so its files
 * are the same generator on both "routes" — the functions and the rule are the measured half); the chaos carriers are
 * byte-locked whole over every placement variant ({@code C3Commons} x11 — the eleven s03 families that carry a
 * {@code functions/} directory; the seat's five {@code a4*} / {@code a5*} families expand the type subset alone);
 * control3 / control4 prove the locks able to fail.
 *
 * <p><b>THE LANE SET</b> (v3.2 seat 6 commit 6 — {@code target/v32-seat6-instruments/lanes-s6.py}, local; the receipt
 * {@code scratch/lanes-s6-r0-829abc152.status} — the fix-head run, preserved under that name when the round-1 re-run
 * overwrote {@code lanes-s6.status} — at the committed seat-suite head {@code 829abc152}, 2026-09-07 11:05–11:13 local, the
 * tree clean at both ends; the suite run under {@code -Pir-on} so control2b RUNS on every lane): nine exact-string mutations
 * of the SHIPPED code, each applied, run against this whole suite and restored from git — the runner refuses a dirty tree
 * and dry-checks every anchor for uniqueness first (all nine unique); the two cross-module lanes (Z and Z2, on rune-ir-java)
 * install the mutated module before their run and re-install the restored one after. THE MEASURED RED SETS, as SETS (each
 * kept lane red on EXACTLY these and green on the rest): A1 the lambda-item rung returning null → a1–a8, b1–b8, corpus_c1
 * AND control3 (18: every lambda-item fixture keeps the placeholder; control3 fails at its PRECONDITION, its carrier
 * chaos/s03/base C3Commons being a healed row; the direct-receiver controls a9 / a10 stay green — the rung is never
 * reached) · A2 the explicit-parameter arm gated off → b1 ALONE · A3 the then-piped owner recursion returning null → a4
 * b7 · B1 the deep-body read gated off (the parser computer's blanket MULTI again) → a1 a4 a5 a6 a7 a8 b1 b2 b3 b6 b7 b8
 * corpus_c1 control3 (14: every single-feature extract selects {@code *ToList} again; a2's MULTI feature keeps
 * {@code mapItemToList}, a3 / b4 / b5 have no map method) · C1 the hoist block's CR coercion gated off → b3 ALONE (the
 * return line) · D1 the primitive identity reverted to the pre-seat decline → b3 ALONE (the count line) · Z2 the IR leaf
 * emitter's plain feature-hop render spaced (the reporting rule's {@code extract picks} on the IR route) → control2b
 * ALONE, the default route unmoved (the IR-ONLY lane). TWO LANES MEASURED GREEN AND WITHDRAWN under the #614 law (a lane
 * green on the seat suite is never kept): B2 the receiver-chain disjunct of {@code deepBodyProvesMulti} deleted — no
 * fixture carries a deep call on a MULTI lambda item (a list of lists; the disjunct stands on upstream's
 * {@code caseDeepFeatureCall} table alone, BANKED with its oracle group named in the seat plan); Z the IR leaf emitter's
 * existence render ({@code exists} → {@code notExists}) — the IR route emits none of this seat's shapes itself (the
 * filter predicate and the condition hoist block are rendered by the shared handlers on both routes: LAW 77 by identity),
 * which is why Z2 is the positive control of control2b. Every prediction in the runner's header was measured as a SET;
 * A1, A2, A3, B1, C1 and D1 measured EXACTLY their prediction, B2 its predicted green; Z's green was NOT predicted and is
 * recorded as measured.
 *
 * <p><b>ROUND 1</b> (commit 9 — the sweep re-run WHOLE at the round-1 code head {@code 51d740fbd}, the fix-head receipt
 * preserved as {@code scratch/lanes-s6-r0-829abc152.status}): EIGHT lanes — B2's term was DELETED by the round-1 code
 * (the code-quality review's MF-1, the seat-30 precedent), so its lane went with it. The seven kept sets measured
 * IDENTICAL to the fix-head run (A1 18, A2 b1, A3 a4 b7, B1 14, C1 b3, D1 b3, Z2 control2b ALONE) and Z measured
 * GREEN again (withdrawn, as before). B1's mutation now gates the arm that CONSULTS {@code chainProvesMulti}'s deep
 * arm — the same 14 red, so the consulted declaration serves the seat exactly as the restated one did.
 */
class DeepPathUtilSeatTest {

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
     * The eleven placement families of s03 that carry a {@code C3Commons.java} — the chaos tree holds SIXTEEN s03
     * directories, of which {@code a4none} / {@code a4snap} / {@code a5crlf} / {@code a5mixed} / {@code a5uni} expand the
     * type subset alone and emit no {@code functions/} directory ({@code a1o4} and {@code x13half} are other seats'
     * families and do not occur in s03 at all — round 1's SF-6 corrected the first cut's reason; {@link #lockChaos}
     * enumerates the carriers from the golden tree and asserts the count and the per-family bijection).
     */
    private static final List<String> FAMILIES_11 = List.of(
            "a1o1", "a1o2", "a1o3", "a2alias", "a2dangle", "a2qual", "a2wild",
            "a3half", "a3hub", "a3third", "base");

    // =========================================================================
    // The fixtures — the oracle groups under census.seat6* namespaces
    // =========================================================================

    private static final String CHOICES = """

            type Note: <"Choice option 1 - a text and tags (the chaos C3Note, widened by tags).">
                text string (1..1)
                tags string (0..*)

            type CashLeg: <"Inner option 1 - shares 'common' with its sibling (the chaos C3CashLeg).">
                ccy string (1..1)
                common string (0..1)

            type StockLeg: <"Inner option 2 (the chaos C3StockLeg).">
                ticker string (1..1)
                common string (0..1)

            choice Inner: <"The inner choice (the chaos C3Inner).">
                CashLeg
                StockLeg

            type Wrap: <"Choice option 2 - wraps the inner choice; shares 'text' and 'tags' with Note (the chaos C3Wrap).">
                inner Inner (1..1)
                text string (0..1)
                tags string (0..*)

            choice Outer: <"The nested choice (the chaos C3Outer).">
                Wrap
                Note
            """;

    /** deep-path-util-injection: the C3Commons shape and its sub-shapes, two direct-receiver controls. */
    private static final String DP = "namespace census.seat6dp\n" + CHOICES + """

            func Commons: <"The chaos C3Commons shape: a deep feature on the IMPLICIT item inside an extract lambda.">
                inputs:
                    outers Outer (0..*)
                output:
                    texts string (0..*)
                add texts:
                    outers extract item ->> text

            func MultiFeature: <"The deep feature is MULTI (tags 0..*): the body is multi, the list of lists flattened.">
                inputs:
                    outers Outer (0..*)
                output:
                    tags string (0..*)
                add tags:
                    outers extract item ->> tags then flatten

            func Filtered: <"The deep feature inside a FILTER lambda.">
                inputs:
                    outers Outer (0..*)
                output:
                    kept Outer (0..*)
                add kept:
                    outers filter [ item ->> text exists ]

            func ThenChain: <"A then chain: the extract's argument is the filter.">
                inputs:
                    outers Outer (0..*)
                output:
                    texts string (0..*)
                add texts:
                    outers filter [ item ->> text exists ] then extract item ->> text

            func SingleReceiver: <"A single receiver - the single-to-item form.">
                inputs:
                    outer Outer (0..1)
                output:
                    text string (0..1)
                set text:
                    outer extract item ->> text

            func NestedChoice: <"The inner choice's util.">
                inputs:
                    inners Inner (0..*)
                output:
                    commons string (0..*)
                add commons:
                    inners extract item ->> common

            func TwoUtils: <"Two deep calls on two choice types - two injected utils.">
                inputs:
                    outers Outer (0..*)
                    inners Inner (0..*)
                output:
                    texts string (0..*)
                add texts:
                    outers extract item ->> text
                add texts:
                    inners extract item ->> common

            func WithFnDep: <"A function dependency beside the util.">
                inputs:
                    outers Outer (0..*)
                output:
                    texts string (0..*)
                add texts:
                    Commons(outers)
                add texts:
                    outers extract item ->> text

            func Direct: <"CONTROL - the direct deep call on a single input (the pre-seat arm; byte-identical before and after).">
                inputs:
                    outer Outer (0..1)
                output:
                    text string (0..1)
                set text:
                    outer ->> text

            func DirectMulti: <"CONTROL - the direct deep call on a multi input.">
                inputs:
                    outers Outer (0..*)
                output:
                    texts string (0..*)
                add texts:
                    outers ->> text
            """;

    /** deep-path-util-injection-edge: the explicit parameter, the alias body, the condition, the keys, the remote choice, the rule. */
    private static final String DPE = "namespace census.seat6dpe\n\nimport census.seat6dpet.*\n" + CHOICES + """

            type Deep: <"The chaos C3Deep shape with a LAMBDA deep call in a type condition (the datarule path).">
                pick Outer (0..1)
                picks Inner (0..*)
                condition LambdaNav:
                    if picks exists
                    then (picks extract [ item ->> common ] then count) <= picks count

            func ExplicitParam: <"An explicit lambda parameter as the deep receiver.">
                inputs:
                    outers Outer (0..*)
                output:
                    texts string (0..*)
                add texts:
                    outers extract o [ o ->> text ]

            func AliasItem: <"The lambda deep call inside an ALIAS body.">
                inputs:
                    outers Outer (0..*)
                output:
                    texts string (0..*)
                alias picked: outers extract item ->> text
                add texts: picked

            func InCondition: <"The lambda deep call inside a function CONDITION.">
                inputs:
                    outers Outer (0..*)
                output:
                    n int (1..1)
                condition HasText:
                    outers extract [ item ->> text ] then exists
                set n: outers count

            func MaxKey: <"The lambda deep call as a max comparator key.">
                inputs:
                    outers Outer (0..*)
                output:
                    top Outer (0..1)
                set top:
                    outers max [ item ->> text ]

            func Sorted: <"The lambda deep call as a sort key.">
                inputs:
                    outers Outer (0..*)
                output:
                    sorted Outer (0..*)
                add sorted:
                    outers sort [ item ->> text ]

            func Remote: <"A cross-namespace choice - the util lives in the TYPE's namespace, imported.">
                inputs:
                    rs ROuter (0..*)
                output:
                    texts string (0..*)
                add texts:
                    rs extract item ->> text

            reporting rule PickTexts from Deep: <"The lambda deep call in a REPORTING RULE body.">
                extract picks then extract item ->> common
            """;

    /** The edge group's second file: the remote choice's namespace (loaded, never emitted). */
    private static final String DPET = """
            namespace census.seat6dpet

            type RNote: <"Remote option 1.">
                text string (1..1)

            type RWrap: <"Remote option 2.">
                text string (0..1)

            choice ROuter: <"The remote nested choice - its util is generated in THIS namespace.">
                RWrap
                RNote
            """;

    private static final String DEPS_HEADER = "\t// RosettaFunction dependencies\n\t//\n";
    private static final String OUTER_UTIL_FIELD = "\t@Inject protected OuterDeepPathUtil outerDeepPathUtil;\n";
    private static final String INNER_UTIL_FIELD = "\t@Inject protected InnerDeepPathUtil innerDeepPathUtil;\n";
    private static final String OUTER_HOP = "item.<String>map(\"chooseText\", outer -> outerDeepPathUtil.chooseText(outer))";
    private static final String INNER_HOP = "item.<String>map(\"chooseCommon\", inner -> innerDeepPathUtil.chooseCommon(inner))";
    private static final String TODO = "TODO(M7b-4)";

    // =========================================================================
    // (A) + (B) — the core group
    // =========================================================================

    /** a1: the chaos shape — the util injected, imported, the hop routed through it, the single feature's mapItem. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_implicitItemDeepCall_injectsTheUtilAndMapsItem() throws IOException {
        String code = fn("dp", "Commons.java");
        assertContains(code, "import census.seat6dp.util.OuterDeepPathUtil;\n");
        assertContains(code, "import javax.inject.Inject;\n");
        assertContains(code, DEPS_HEADER + OUTER_UTIL_FIELD);
        assertContains(code, "\t\t\ttexts.addAll(MapperC.<Outer>of(outers)\n\t\t\t\t.mapItem(item -> " + OUTER_HOP + ").getMulti());\n");
        assertAbsent(code, TODO);
        assertAbsent(code, "mapItemToList");
    }

    /** a2: a MULTI deep feature — the body IS multi: mapItemToList + mapC, the list of lists hoisted and flattened. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_multiDeepFeature_keepsMapItemToListWithMapC() throws IOException {
        String code = fn("dp", "MultiFeature.java");
        assertContains(code, "import com.rosetta.model.lib.mapper.MapperListOfLists;\n");
        assertContains(code, DEPS_HEADER + OUTER_UTIL_FIELD);
        assertContains(code, "\t\t\tfinal MapperListOfLists<String> thenArg = MapperC.<Outer>of(outers)\n"
                + "\t\t\t\t.mapItemToList(item -> item.<String>mapC(\"chooseTags\", outer -> outerDeepPathUtil.chooseTags(outer)));\n");
        assertContains(code, "\t\t\ttags.addAll(thenArg\n\t\t\t\t.flattenList().getMulti());\n");
        assertAbsent(code, TODO);
    }

    /** a3: the FILTER lambda's item — the injection alone (no map method at a filter). */
    @Test
    @EnabledIf("builtinsAvailable")
    void a3_filterLambdaItem_injectsTheUtil() throws IOException {
        String code = fn("dp", "Filtered.java");
        assertContains(code, DEPS_HEADER + OUTER_UTIL_FIELD);
        assertContains(code, "\t\t\t\t.filterItemNullSafe(item -> exists(" + OUTER_HOP + ").get()).getMulti()));\n");
        assertAbsent(code, TODO);
    }

    /** a4: a then chain — the extract's argument is the filter; the hoisted thenArg is a MapperC, the step mapItem. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a4_thenChain_theExtractArgumentIsTheFilter() throws IOException {
        String code = fn("dp", "ThenChain.java");
        assertContains(code, "\t\t\tfinal MapperC<Outer> thenArg = MapperC.<Outer>of(outers)\n"
                + "\t\t\t\t.filterItemNullSafe(item -> exists(" + OUTER_HOP + ").get());\n");
        assertContains(code, "\t\t\ttexts.addAll(thenArg\n\t\t\t\t.mapItem(item -> " + OUTER_HOP + ").getMulti());\n");
        assertAbsent(code, TODO);
        assertAbsent(code, "mapItemToList");
    }

    /** a5: a single receiver — mapSingleToItem, the lambda var escaped by the input's name. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a5_singleReceiver_takesMapSingleToItem() throws IOException {
        String code = fn("dp", "SingleReceiver.java");
        assertContains(code, "\t\t\ttext = MapperS.of(outer)\n"
                + "\t\t\t\t.mapSingleToItem(item -> item.<String>map(\"chooseText\", _outer -> outerDeepPathUtil.chooseText(_outer))).get();\n");
        assertAbsent(code, "mapSingleToList");
        assertAbsent(code, TODO);
    }

    /** a6: the inner choice's util — the receiver type decides the util, the namespace its package. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a6_innerChoice_injectsItsOwnUtil() throws IOException {
        String code = fn("dp", "NestedChoice.java");
        assertContains(code, "import census.seat6dp.util.InnerDeepPathUtil;\n");
        assertContains(code, DEPS_HEADER + INNER_UTIL_FIELD);
        assertContains(code, "\t\t\t\t.mapItem(item -> " + INNER_HOP + ").getMulti());\n");
        assertAbsent(code, TODO);
    }

    /** a7: two utils — both injected, sorted by simple name (Inner before Outer), each hop through its own. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a7_twoUtils_bothInjectedInNameOrder() throws IOException {
        String code = fn("dp", "TwoUtils.java");
        assertContains(code, DEPS_HEADER + INNER_UTIL_FIELD + OUTER_UTIL_FIELD);
        assertContains(code, "\t\t\t\t.mapItem(item -> " + OUTER_HOP + ").getMulti());\n");
        assertContains(code, "\t\t\t\t.mapItem(item -> " + INNER_HOP + ").getMulti());\n");
        assertAbsent(code, TODO);
    }

    /** a8: a function dependency beside the util — the collector's name order (commons before outerDeepPathUtil). */
    @Test
    @EnabledIf("builtinsAvailable")
    void a8_functionDependencyBesideTheUtil_nameOrdered() throws IOException {
        String code = fn("dp", "WithFnDep.java");
        assertContains(code, DEPS_HEADER + "\t@Inject protected Commons commons;\n" + OUTER_UTIL_FIELD);
        assertContains(code, "\t\t\ttexts.addAll(commons.evaluate(outers));\n");
        assertContains(code, "\t\t\t\t.mapItem(item -> " + OUTER_HOP + ").getMulti());\n");
        assertAbsent(code, TODO);
    }

    /** a9 CONTROL: the direct deep call on a single input — the pre-seat arm, unmoved. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a9_control_directSingleReceiver_keepsThePreSeatForm() throws IOException {
        String code = fn("dp", "Direct.java");
        assertContains(code, DEPS_HEADER + OUTER_UTIL_FIELD);
        assertContains(code, "\t\t\ttext = MapperS.of(outer).<String>map(\"chooseText\", _outer -> outerDeepPathUtil.chooseText(_outer)).get();\n");
        assertAbsent(code, TODO);
    }

    /** a10 CONTROL: the direct deep call on a multi input — unmoved. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a10_control_directMultiReceiver_keepsThePreSeatForm() throws IOException {
        String code = fn("dp", "DirectMulti.java");
        assertContains(code, DEPS_HEADER + OUTER_UTIL_FIELD);
        assertContains(code, "\t\t\ttexts.addAll(MapperC.<Outer>of(outers).<String>map(\"chooseText\", outer -> outerDeepPathUtil.chooseText(outer)).getMulti());\n");
        assertAbsent(code, TODO);
    }

    // =========================================================================
    // The edge group
    // =========================================================================

    /** b1: an EXPLICIT lambda parameter as the receiver — the closure-param owner walk types it. */
    @Test
    @EnabledIf("builtinsAvailable")
    void b1_explicitLambdaParameter_injectsTheUtil() throws IOException {
        String code = fn("dpe", "ExplicitParam.java");
        assertContains(code, DEPS_HEADER + OUTER_UTIL_FIELD);
        assertContains(code, "\t\t\t\t.mapItem(o -> o.<String>map(\"chooseText\", outer -> outerDeepPathUtil.chooseText(outer))).getMulti());\n");
        assertAbsent(code, TODO);
    }

    /** b2: the ALIAS body — the alias method carries the hop, the class the field. */
    @Test
    @EnabledIf("builtinsAvailable")
    void b2_aliasBody_theAliasMethodCarriesTheHop() throws IOException {
        String code = fn("dpe", "AliasItem.java");
        assertContains(code, DEPS_HEADER + OUTER_UTIL_FIELD);
        assertContains(code, "\t\tprotected MapperC<String> picked(List<? extends Outer> outers) {\n"
                + "\t\t\treturn MapperC.<Outer>of(outers)\n\t\t\t\t.mapItem(item -> " + OUTER_HOP + ");\n");
        assertAbsent(code, TODO);
    }

    /**
     * b3: a function CONDITION — the condition lambda hoists a MapperC thenArg and returns the ComparisonResult form (the
     * fix's cut 3, arm (i): the hoist block's return coerced through the datarule seat's own {@code isDataRuleCrRoot} /
     * {@code crCoerce}); the count-into-int SET beside it assigns the primitive INLINE, no {@code .get()} (cut 3, arm (ii):
     * the numeric-output coercion's primitive identity). Two needles, one per arm — lane C reverts the coercion and must
     * go red on the return line alone, lane D the identity arm on the count line alone.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b3_functionCondition_hoistsAMapperCThenArg() throws IOException {
        String code = fn("dpe", "InCondition.java");
        assertContains(code, "import com.rosetta.model.lib.expression.ComparisonResult;\n");
        assertContains(code, DEPS_HEADER + OUTER_UTIL_FIELD);
        assertContains(code, "\t\t\tfinal MapperC<String> thenArg = MapperC.<Outer>of(outers)\n"
                + "\t\t\t\t.mapItem(item -> " + OUTER_HOP + ");\n"
                + "\t\t\treturn ComparisonResult.ofNullSafe(exists(thenArg).asMapper());\n");
        assertContains(code, "\t\t\tn = MapperC.<Outer>of(outers).resultCount();\n");
        assertAbsent(code, "resultCount().get()");
        assertAbsent(code, TODO);
        assertAbsent(code, "MapperListOfLists");
    }

    /** b4: a max comparator key. */
    @Test
    @EnabledIf("builtinsAvailable")
    void b4_maxKey_injectsTheUtil() throws IOException {
        String code = fn("dpe", "MaxKey.java");
        assertContains(code, DEPS_HEADER + OUTER_UTIL_FIELD);
        assertContains(code, "\t\t\t\t.max(item -> " + OUTER_HOP + ").get());\n");
        assertAbsent(code, TODO);
    }

    /** b5: a sort key. */
    @Test
    @EnabledIf("builtinsAvailable")
    void b5_sortKey_injectsTheUtil() throws IOException {
        String code = fn("dpe", "Sorted.java");
        assertContains(code, DEPS_HEADER + OUTER_UTIL_FIELD);
        assertContains(code, "\t\t\t\t.sort(item -> " + OUTER_HOP + ").getMulti()));\n");
        assertAbsent(code, TODO);
    }

    /** b6: a cross-namespace choice — the util's package follows the TYPE's namespace, the field its name. */
    @Test
    @EnabledIf("builtinsAvailable")
    void b6_remoteChoice_theUtilLivesInTheTypesNamespace() throws IOException {
        String code = fn("dpe", "Remote.java");
        assertContains(code, "import census.seat6dpet.util.ROuterDeepPathUtil;\n");
        assertContains(code, DEPS_HEADER + "\t@Inject protected ROuterDeepPathUtil rOuterDeepPathUtil;\n");
        assertContains(code, "\t\t\t\t.mapItem(item -> item.<String>map(\"chooseText\", rOuter -> rOuterDeepPathUtil.chooseText(rOuter))).getMulti());\n");
        assertAbsent(code, TODO);
    }

    /** b7: a REPORTING RULE body — the rule class carries the field, the step mapItem. */
    @Test
    @EnabledIf("builtinsAvailable")
    void b7_reportingRule_injectsTheUtil() throws IOException {
        String code = pick(render("dpe"), "reports/PickTextsRule.java", "PickTextsRule.java");
        assertContains(code, DEPS_HEADER + INNER_UTIL_FIELD);
        assertContains(code, "\t\t\t\t.mapItem(item -> " + INNER_HOP + ").getMulti();\n");
        assertAbsent(code, TODO);
        assertAbsent(code, "mapItemToList");
    }

    /** b8: a TYPE condition (the datarule path) — the Default class carries the field, the thenArg is a MapperC. */
    @Test
    @EnabledIf("builtinsAvailable")
    void b8_typeCondition_theDataRuleInjectsTheUtil() throws IOException {
        String code = pick(render("dpe"), "validation/datarule/DeepLambdaNav.java", "DeepLambdaNav.java");
        assertContains(code, "\t\t@Inject protected InnerDeepPathUtil innerDeepPathUtil;\n");
        assertContains(code, "\t\t\t\t\tfinal MapperC<String> thenArg = MapperS.of(deep).<Inner>mapC(\"getPicks\", _deep -> _deep.getPicks())\n"
                + "\t\t\t\t\t\t.mapItem(item -> " + INNER_HOP + ");\n");
        assertAbsent(code, TODO);
        assertAbsent(code, "MapperListOfLists");
    }

    // =========================================================================
    // The controls
    // =========================================================================

    /**
     * control2a — the default route's population pins: every fixture set emits EXACTLY its declared functions (the
     * declared names read off the PARSED model, never a regex over source), and the edge set its rule and its datarule.
     */
    private static final Map<String, Integer> POPULATION = Map.of("dp", 10, "dpe", 6);
    /** Every fixture set this class renders — the ONE list {@link #source} dispatches over. */
    private static final List<String> FIXTURE_SETS = List.of("dp", "dpe");

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
        Render edge = render("dpe");
        assertEquals(1, edge.output().keySet().stream().filter(k -> k.endsWith("/reports/PickTextsRule.java")).count(), "the rule renders");
        assertEquals(1, edge.output().keySet().stream().filter(k -> k.endsWith("/validation/datarule/DeepLambdaNav.java")).count(), "the datarule renders");
        assertTrue(edge.output().keySet().stream().noneMatch(k -> k.contains("seat6dpet")), "the remote namespace is loaded, never emitted");
    }

    /** control2c — control2a's pins proven able to FAIL on a doctored INPUT (the last function deleted). */
    @Test
    @EnabledIf("builtinsAvailable")
    void control2c_populationPin_failsOnADoctoredFixture() throws IOException {
        String full = source("dp");
        int cut = full.lastIndexOf("func DirectMulti:");
        assertTrue(cut > 0, "the dp fixture ends with DirectMulti");
        String doctored = full.substring(0, cut);
        java.util.Set<String> declared = declaredFunctionNames(doctored, "dp-doctored");
        assertEquals(POPULATION.get("dp") - 1, declared.size(), "the doctored fixture declares one function fewer");
        assertTrue(!declared.contains("DirectMulti"), "DirectMulti is the deleted one");
        Render r = renderSource(doctored, "dp", "dp-doctored", false);
        assertEquals(POPULATION.get("dp") - 1, fixtureFunctionKeys(r).size(),
                "the rendered population moves with the input - the pin can fail: " + fixtureFunctionKeys(r));
        // round 1 (cq NIT-5): the SET pin proven INPUT-SENSITIVE where the count is not - a RENAME keeps the count and
        // moves the set, and the emitted set follows it. assertEquals(declared, emitted) itself cannot be broken from
        // the input side (both halves read the same source); the mutation that falsifies it is a GENERATOR one - lane
        // A1, which reddens control3 / corpus_c1 and every lambda-item fixture (round 2's cq SF-3 corrected the claim).
        String renamed = full.replace("func DirectMulti:", "func DirectMultiRenamed:");
        assertTrue(!renamed.equals(full), "the rename must change a byte");
        java.util.Set<String> renamedDeclared = declaredFunctionNames(renamed, "dp-renamed");
        assertEquals(POPULATION.get("dp").intValue(), renamedDeclared.size(), "the renamed fixture keeps the count");
        assertTrue(!renamedDeclared.equals(declaredFunctionNames(full, "dp")), "the renamed fixture moves the SET");
        Render rr = renderSource(renamed, "dp", "dp-renamed", false);
        java.util.Set<String> renamedEmitted = new java.util.TreeSet<>();
        for (String k : fixtureFunctionKeys(rr)) {
            String simple = k.substring(k.lastIndexOf('/') + 1);
            renamedEmitted.add(simple.endsWith(".java") ? simple.substring(0, simple.length() - 5) : simple);
        }
        assertEquals(renamedDeclared, renamedEmitted, "the emitted SET follows the declared one under the rename");
        assertTrue(renamedEmitted.contains("DirectMultiRenamed") && !renamedEmitted.contains("DirectMulti"),
                "the SET assertion sees the rename where the count cannot: " + renamedEmitted);
    }

    private static java.util.Set<String> declaredFunctionNames(String source, String label) {
        RModel model = AstBuilder.buildFromString(source, "seat6" + label + ".rosetta");
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

    /** corpus_c1: the eleven s03 C3Commons variants byte-identical to the chaos goldens. */
    @Test
    @EnabledIf("chaosAvailable")
    void corpus_c1_C3Commons_allElevenVariants() throws IOException {
        lockChaos("s03", "C3Commons", FAMILIES_11);
    }

    /** control3: the whole-file lock is proven able to fail — a doctored golden is reported by name. */
    @Test
    @EnabledIf("chaosAvailable")
    void control3_doctoredGolden_isReportedByTheLock() throws IOException {
        assertNotNull(chaosOutput, "chaos cell generation did not run — corpus unavailable?");
        String carrier = "chaos/s03/base/functions/C3Commons.java";
        String generated = chaosOutput.get(carrier);
        assertNotNull(generated, "not generated: " + carrier);
        String golden = normalize(Files.readString(CHAOS_GOLDEN.resolve(carrier)));
        assertTrue(compareCarrier(carrier, golden, generated) == null,
                "the undoctored carrier must lock green before the doctor is applied");
        String doctored = golden.replace("c3OuterDeepPathUtil.chooseText(c3Outer)", "c3OuterDeepPathUtil.chooseTexts(c3Outer)");
        assertTrue(!doctored.equals(golden), "the doctor must change a byte");
        String verdict = compareCarrier(carrier, doctored, generated);
        assertNotNull(verdict, "a doctored golden MUST be reported by the lock");
        assertContains(verdict, "chooseTexts");
    }

    /** control4: the placement-family identity pin proven able to fail (over the eleven-family s03 set). */
    @Test
    void control4_placementFamilyPin_canFail() {
        assertEquals("a3half", placementFamily("a3half/p2"));
        assertEquals("a3third", placementFamily("a3third/p3"));
        assertEquals("base", placementFamily("base"));
        List<String> good = List.of("a1o1", "a1o2", "a1o3", "a2alias", "a2dangle", "a2qual", "a2wild",
                "a3half/p2", "a3hub/p2", "a3third/p3", "base").stream()
                .map(v -> "chaos/s99/" + v + "/functions/X.java").toList();
        assertTrue(placementFamilyVerdict("s99", good, FAMILIES_11) == null, "the enumerated eleven must pass");
        List<String> doctored = new ArrayList<>(good);
        doctored.set(doctored.indexOf("chaos/s99/a3third/p3/functions/X.java"), "chaos/s99/a3half/p1/functions/X.java");
        String verdict = placementFamilyVerdict("s99", doctored, FAMILIES_11);
        assertNotNull(verdict, "a doctored population MUST be reported");
        String found = verdict.substring(verdict.indexOf("found "));
        assertTrue(!found.contains("a3third"), "the missing family must be absent from the found list: " + found);
        assertTrue(found.indexOf("a3half") != found.lastIndexOf("a3half"), "the doubled family must appear twice: " + found);
    }

    // =========================================================================
    // Fixture harness — functions, rules and datarules, both routes, the remote namespace loaded for the edge set
    // =========================================================================

    private record Render(Map<String, String> output, List<String> errors) { }

    private static final Map<String, Render> RENDERED = new LinkedHashMap<>();
    private static final Map<String, Render> RENDERED_IR = new LinkedHashMap<>();

    private static String source(String set) {
        assertTrue(FIXTURE_SETS.contains(set), "an undeclared fixture set: " + set + " (add it to FIXTURE_SETS and POPULATION)");
        return switch (set) {
            case "dp" -> DP;
            case "dpe" -> DPE;
            default -> throw new IllegalArgumentException(set);
        };
    }

    /** The sibling models a set loads beside its main file (the edge set's remote namespace). */
    private static List<String> siblingSources(String set) {
        return "dpe".equals(set) ? List.of(DPET) : List.of();
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
        RModel main = AstBuilder.buildFromString(source, "seat6" + label + ".rosetta");
        main.setVersion("0.0.0.test");
        List<RModel> models = new ArrayList<>();
        models.add(main);
        int n = 0;
        for (String sibling : siblingSources(set)) {
            RModel sib = AstBuilder.buildFromString(sibling, "seat6" + label + "-sibling" + (n++) + ".rosetta");
            sib.setVersion("0.0.0.test");
            models.add(sib);
        }
        models.addAll(loadBuiltinsOnly());
        RLinkingResult linked = RWorkspace.build(models);
        RWorkspace workspace = linked.workspace();
        String namespace = "census.seat6" + set;
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
            throw new AssertionError("[DeepPathUtilSeatTest] builtins parse failures: "
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
                .map(DeepPathUtilSeatTest::placementFamily)
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
