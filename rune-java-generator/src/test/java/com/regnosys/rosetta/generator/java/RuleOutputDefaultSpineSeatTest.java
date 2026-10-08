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
import com.regnosys.rosetta.generator.GenerationException;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.reports.ReportGenerator;
import com.regnosys.rosetta.generator.java.reports.RuleGenerator;
import com.regnosys.rosetta.generator.java.spi.IRGeneration;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.symbols.RLinkingResult;
import com.regnosys.rosetta.symbols.RWorkspace;

/**
 * SEAT 29, law 2 — facet {@code ruleOutputDefaultSpineMulti} (family F10a): <b>a reporting rule
 * whose body SPINE carries a {@code default} whose LEFT operand proves MULTI has a MULTI output</b>.
 * ONE new disjunct in {@code NavigationHandler.ruleOutputProvesMulti}, the ONE predicate all seven
 * rule-output consumer seats read.
 *
 * <p><b>The golden vs fork shape</b> (drr 7.0.0
 * {@code drr/regulation/esma/emir/refit/trade/reports/IndicatorOfTheUnderlyingIndexRule.java} — the
 * complete 24-line delta, identical in the fca twin and in all four drr 7.x cells):
 * <pre>
 * golden: public abstract class … implements ReportFunction&lt;TransactionReportInstruction, List&lt;IndexEnum&gt;&gt; {
 *         public List&lt;IndexEnum&gt; evaluate(…) / protected abstract List&lt;IndexEnum&gt; doEvaluate(…)
 *         List&lt;IndexEnum&gt; output = new ArrayList&lt;&gt;();
 *         protected List&lt;IndexEnum&gt; assignOutput(List&lt;IndexEnum&gt; output, …)
 *         final MapperC&lt;IndexEnum&gt; thenArg1 = thenArg0
 *             .mapSingleToList(item -&gt; MapperC.&lt;IndexEnum&gt;of(indicatorOfTheUnderlyingIndexRule.evaluate(item.get())));
 *         output = thenArg1.mapItem(item -&gt; { … }).getMulti();
 * fork  : public abstract class … implements ReportFunction&lt;TransactionReportInstruction, IndexEnum&gt; {
 *         public IndexEnum evaluate(…)      / protected abstract IndexEnum doEvaluate(…)
 *         IndexEnum output = null;
 *         protected IndexEnum assignOutput(IndexEnum output, …)
 *         final MapperS&lt;IndexEnum&gt; thenArg1 = thenArg0
 *             .mapSingleToItem(item -&gt; MapperS.of(indicatorOfTheUnderlyingIndexRule.evaluate(item.get())));
 *         output = thenArg1.mapSingleToItem(item -&gt; { … }).get();
 * </pre>
 *
 * <p><b>The mechanism.</b> The producer rule (emir {@code IndicatorOfTheUnderlyingIndex}) carries
 * {@code … -&gt; identifier default &lt;hoisted conditional&gt;} in its SECOND-TO-LAST extract body and a
 * SINGLE-output function call as its tail, so {@code getRuleBodyCardinality} reads the whole body
 * SINGLE while {@code chainProvesMulti(left)} proves the default's left MULTI. The new disjunct
 * descends the cardinality-composing edges ({@code RThenExpr} argument+body, {@code RExtractExpr}
 * argument+body, {@code RFilterExpr} argument), declines on a collapsing list-op leaf, admits on the
 * FIRST {@code default} whose left proves MULTI, and otherwise recurses into an
 * {@code RSymbolReference} bound to an {@code RRule} — the step that carries the two CONSUMER rules,
 * whose own spines hold no {@code default} at all.
 *
 * <p><b>The probe verdict this law answers (LAW 75 — {@code PROBE29-F10sig/call/decl/term},
 * {@code target/seat29-instruments/verdicts29-f10.md}).</b> Both routes, one instrumented D11 round
 * each, 25 cells:
 * <ul>
 *   <li>{@code declaredMulti=false && cfMulti=true} at the SIGNATURE seat is <b>4 rows / 1 distinct
 *       rule / ALL BAND / ZERO green</b> over 21,341 rows per route. There is no green member to
 *       exclude — the gate as probed is already exact at that seat.</li>
 *   <li>The {@code chainProvesMulti(left)} conjunct is <b>LOAD-BEARING</b>: gate G3 (a
 *       {@code default} on the spine with no left test) admits 95 rows of which <b>87 are GREEN
 *       across 16 green rules</b>. Its in-BAND decline witness is jfsa {@code UnderlyingIndexIndicator}
 *       at drr 7.0–7.3 ({@code defaultSeen=true leftKind=RListOpExpr leftProvesMulti=false} — a
 *       {@code first}-collapsed left), which this law must NOT move.</li>
 *   <li>Every caller of the flipped callee is BAND, closed three ways: the probe's flip-set scan
 *       (72 rows, all drr 7.x POJO, all band), the sig-seat depth-2 join (8 rows = the esma + fca
 *       consumers), and a {@code .rosetta} grep finding exactly 5 sources that name the rule.
 *       {@code cfCalleeMulti=true} is <b>56 rows, zero green</b>.</li>
 *   <li>The sibling seat {@code FER-bareInvokableThen} is <b>REFUTED-AS-SITED</b>: 1,799 rows, ZERO
 *       with {@code cfCalleeMulti=true}, both routes. Its expected failing set is empty BY
 *       MEASUREMENT — an adjudicated zero, not a declared one.</li>
 * </ul>
 *
 * <p><b>The collapse guard is MEASURED, not proposed.</b> The verdict's §8 gap 3 recorded the
 * unguarded walk as the measured spec and a collapse guard as an unmeasured hardening. It is
 * measurable from the same dump, and it was measured: at {@code FER-thenExtractTerminal} the 12
 * candidate rows ({@code defaultSeen=true && leftProvesMulti=true && declaredMulti!=true}) split
 * <b>4 BAND</b> (the emir producer — no {@code RListOpExpr} on its leaf trail, {@code ruleScoped=true})
 * and <b>8 GREEN</b> (drr 7.x FUNCTION {@code GetUniqueTransactionIdentifier}
 * {@code leaves=RDefaultExpr,RListOpExpr} and {@code UniqueSwapIdentifierForValuation}
 * {@code leaves=RListOpExpr,RDefaultExpr,RListOpExpr}, both {@code (… default …) then distinct then
 * only-element} with {@code output (1..1)}), route-identical. <b>The left test does NOT decline the
 * 8</b> — all carry {@code leftProvesMulti=true} — so the guard is a second, independent decline
 * channel, not a duplicate of the conjunct. At {@code RG-outputCardBackfill} the guard removes ZERO
 * rows (the 4-row admit set carries no {@code RListOpExpr} leaf on either route). Its practical
 * effect: green-safety for those two functions stops resting on the rule-scoping of the seven
 * {@code ruleOutputProvesMulti} call sites the probe did not instrument. b3 is its only witness —
 * both corpus witnesses are FUNCTIONS, which no rule-output law can reach.
 *
 * <p><b>LAW 74 — the repair the byte count cannot see.</b> The esma/fca report types declare
 * {@code override indicatorOfTheUnderlyingIndex IndexEnum (0..*)}, so
 * {@code ESMAEMIRTransactionReport}'s ONLY setter overload is
 * {@code setIndicatorOfTheUnderlyingIndex(List<IndexEnum>)} while today's fork rule returns
 * {@code IndexEnum}: {@code ESMAEMIRTradeReportFunction} and {@code FCAUKEMIRTradeReportFunction}
 * <b>do not compile</b> in any of the four drr 7.x cells. Their call site is
 * {@code .setIndicatorOfTheUnderlyingIndex(indicatorOfTheUnderlyingIndexRule.evaluate(input));}
 * before and after, so those 8 GREEN files move ZERO bytes and start compiling. The seat's javac
 * PRE/POST must show 8 → 0; {@code corpus_control0} pins the golden bytes the argument rests on.
 *
 * <p><b>What this law does NOT heal (LAW 80).</b> The 8 esma/fca consumer cells go WHOLE (band
 * 152 → 144). The 4 emir PRODUCER cells do NOT: of their 31-line delta, the {@code thenArg1}
 * element read, the ite-hoist decl form and the multi-{@code default} render arm belong to three
 * other families, so the producer stays a mismatch and contributes ZERO file heals. It is
 * deliberately NOT byte-locked here; {@code corpus_control1} is where its residual movement shows.
 *
 * <p><b>RED at the pre-law head</b>: a1, a2, corpus_c1, corpus_c1b, corpus_control1.
 * b1, b2, b3, corpus_control0, corpus_control3, corpus_control4 GREEN in BOTH states — b1 is the
 * jfsa decline witness reduced, b3 the collapse-guard witness, and control0 is golden-only.
 *
 * <p><b>MEASURED MUTATIONS (LAW 82 — the seat-29 chain's suite-lane loop at {@code 687c8feb}; the
 * set below is the RECORDED failing set from the f29-mut-m-law2 log):</b>
 * <ul>
 *   <li><b>m-law2</b> the whole law reverted ({@code law2-apply.py --revert}) → MEASURED a1, a2,
 *       corpus_c1, corpus_c1b, corpus_control1 (5F — exactly the drafted claim); b1, b2, b3,
 *       corpus_control0, corpus_control3, corpus_control4 unmoved as designed</li>
 * </ul>
 * <p><b>Designed manual severs — stated, NOT run in the seat-29 chain</b> (the chain ran the
 * scripted lanes only; these have no apply-script flag and their failing sets remain claims):
 * <ul>
 *   <li><b>m-law2-callee</b> the callee descent alone severed (delete the {@code RSymbolReference}
 *       → {@code RRule} recursion loop in {@code defaultSpineProvesMulti}) → CLAIMED a2, corpus_c1,
 *       corpus_c1b, corpus_control1; a1 unmoved (the producer's own spine carries the
 *       {@code default}). This is the cleanest single-half sever available: it must lose exactly
 *       the 8 consumer heals and keep the producer's partial.</li>
 *   <li><b>m-law2-guard</b> the collapse guard alone severed (delete the collapsing-{@code
 *       RListOpExpr} loop) → CLAIMED b3 EXACTLY, witness-unique; a1, a2, and every corpus control
 *       unmoved (the guard removes zero rows at the signature seat by measurement).</li>
 *   <li><b>m-law2-left</b> the {@code chainProvesMulti(left)} conjunct dropped (gate G3) → CLAIMED
 *       b1, b2 and a large corpus_control1/control3/control4 failure — the probe measured 87 green
 *       rows / 16 green rules entering. Run it only if the seat wants the G3 refutation reproduced
 *       at suite scale; the probe already carries it.</li>
 * </ul>
 */
class RuleOutputDefaultSpineSeatTest {

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

    /**
     * cell A = drr 7.0.0 — the carrier cell. It holds ALL FOUR populations at once: the two
     * consumer heals, the producer partial, the jfsa {@code first}-collapsed-left decline, and the
     * two GREEN drr 7.x FUNCTION rules that are the corpus witnesses of the collapse shape.
     */
    private static final Path CELL_A_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path GOLDEN_A = CELL_A_ROOT.resolve("rosetta-source/src/generated/java");
    /**
     * cell B = drr 5.61.0 — the reach cell. The SAME rule names live here in their already-correct
     * form: emir {@code IndicatorOfTheUnderlyingIndex} reads {@code engineRuleBody=MULTI
     * declaredMulti=true} (the engine channel already answers, so a monotone law must not disturb
     * it) and jfsa {@code UnderlyingIndexIndicator} carries {@code defaultSeen=false} with a
     * collapsing list-op on its trail — declined twice over.
     */
    private static final Path CELL_B_ROOT = Path.of("../test-corpus/drr/drr-5.61.0");
    private static final Path GOLDEN_B = CELL_B_ROOT.resolve("rosetta-source/src/generated/java");
    /**
     * cell C = cdm 6.21.0 — the NON-drr over-fire cell. This law's carriers and every control above
     * are drr-scoped, and the seat-28 law-6 lesson is that a cdm/iosco over-fire is invisible to any
     * drr-scoped control. This does NOT replace the mid-seat whole-matrix D11 checkpoint, which the
     * verdict makes mandatory for this law; it is the cheap in-suite tripwire.
     */
    private static final Path CELL_C_ROOT = Path.of("../test-corpus/cdm/cdm-6.21.0");
    private static final Path GOLDEN_C = CELL_C_ROOT.resolve("rosetta-source/src/generated/java");

    static boolean cellAAvailable() {
        return Files.isDirectory(GOLDEN_A);
    }

    static boolean cellBAvailable() {
        return Files.isDirectory(GOLDEN_B);
    }

    static boolean cellCAvailable() {
        return Files.isDirectory(GOLDEN_C);
    }

    static boolean cellAAndIrProviderAvailable() {
        return cellAAvailable() && irProviderOnClasspath();
    }

    /**
     * A1 = the emir PRODUCER shape reduced — {@code extract head then extract (&lt;multi chain&gt;
     * default &lt;conditional&gt;) then extract &lt;single-output fn&gt;}. The trailing single-output
     * function is what makes the ENGINE read the whole body SINGLE (the carrier's own
     * {@code then extract GetIndexIndicatorFromFloatingRate} tail), so the fixture reproduces the
     * carrier's measured field profile: {@code engineRuleBody=SINGLE defaultSeen=true
     * leftKind=RFeatureCall leftProvesMulti=true}, and the leaf trail
     * {@code RImplicitVariable,RSymbolReference,RImplicitVariable,&lt;first-extract body&gt;,
     * RImplicitVariable,RDefaultExpr} — the emir row's trail with the first extract's conditional
     * replaced by a navigation.
     *
     * <p>A2 = the esma/fca CONSUMER shape reduced — {@code filter &lt;rule&gt; then extract
     * &lt;a1's rule&gt; then extract &lt;conditional&gt;}. Its own spine holds no {@code default}, so
     * ONLY the callee descent can move it, and it pins all four consumer decisions at once: the
     * {@code List} signature, the {@code MapperC} decl, the {@code mapSingleToList} +
     * {@code MapperC.&lt;X&gt;of} wrap, the derived next-step {@code mapItem} and the
     * {@code .getMulti()} terminal.
     *
     * <p>B1 = the jfsa decline witness reduced — the same spine with the default's LEFT
     * {@code first}-collapsed. B2 = a plain single-valued default. B3 = the collapse-guard witness:
     * a1's spine with a collapsing {@code only-element} tail; the default still proves its left
     * MULTI, so ONLY the guard can decline it.
     *
     * <p>{@code note} and {@code mark} are used instead of {@code tag} / {@code label}, and no
     * identifier is named {@code single} — all three are lexer keywords.
     */
    private static final String MODEL = """
            namespace census.seat29d
            version "1.0.0"

            type Ident:
                identifier string (0..1)
                note string (0..1)

            type Leg:
                idents Ident (0..*)
                code string (0..1)

            type Holder:
                head Leg (0..1)
                legs Leg (0..*)
                flag boolean (0..1)
                mark string (0..1)

            func GradeFrom: <"the SINGLE-output tail that makes the ENGINE read the whole body SINGLE">
                inputs:
                    s string (0..1)
                output:
                    g string (0..1)
                set g:
                    s

            reporting rule IsOkay from Holder: <"the filter control (the consumer's leading filter)">
                extract flag

            reporting rule A1DefaultSpine from Holder: <"a1 - the emir root shape: a spine default whose LEFT proves MULTI">
                extract head
                then extract
                    (item -> idents -> identifier default (if item -> code exists then item -> code else "none"))
                then extract GradeFrom(item)

            reporting rule A2Consumer from Holder: <"a2 - the esma/fca consumer: only the callee descent moves it">
                filter IsOkay
                then extract A1DefaultSpine
                then extract
                    if item = "skip"
                    then empty
                    else item

            reporting rule B1CollapsedLeftDefault from Holder: <"b1 - the jfsa witness: a first-collapsed LEFT declines">
                extract head
                then extract
                    (item -> idents -> identifier first default (if item -> code exists then item -> code else "none"))
                then extract GradeFrom(item)

            reporting rule B2PlainSingleDefault from Holder: <"b2 - a plain single default: nothing to prove">
                extract head
                then extract (item -> code default "none")
                then extract GradeFrom(item)

            reporting rule B3CollapsingTailAfterDefault from Holder: <"b3 - the collapse guard: a collapsing tail kills the proof">
                extract head
                then extract
                    (item -> idents -> identifier default (if item -> code exists then item -> code else "none"))
                then only-element
            """;

    // =========================================================================
    // Part A — the fixture carriers (RED pre-law)
    // =========================================================================

    /**
     * a1 — the PRODUCER half. The {@code default} sits in the second-to-last extract body and its
     * left is a MULTI navigation, so the rule's declared output flips SINGLE → MULTI and every
     * signature surface that reads it follows: the {@code ReportFunction} type argument, the
     * {@code output} local's initialiser and the whole-output terminal.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_spineDefaultWithMultiLeftMakesTheRuleOutputMulti() throws IOException {
        String out = rule("A1DefaultSpine");
        assertContains(out, "implements ReportFunction<Holder, List<");
        assertNotContains(out, "implements ReportFunction<Holder, String>");
        assertContains(out, "output = new ArrayList<>();");
        assertTrue(codeOnly(out).contains(".getMulti();"),
                "the whole-output terminal must collapse to the MULTI form:\n" + out);
        assertContains(out, "import java.util.List;");
        // PIN AT RED: replace the three structural asserts above with the byte-exact
        // `implements ReportFunction<Holder, List<String>>`, `List<String> output = new ArrayList<>();`
        // and the exact terminal line, captured from the post-law run. The element spelling
        // (`List<String>` vs `List<? extends String>`) is RuleGenerator:404's RDataType-vs-primitive
        // polymorphism, not this law's question — read it, do not guess it.
    }

    /**
     * a2 — the CONSUMER half, and the only fixture the callee descent can move. Four decisions ride
     * one predicate here (LAW 69): the {@code List} signature, the {@code MapperC} thenArg decl, the
     * {@code mapSingleToList} + {@code MapperC.<X>of} wrap, and the next-step {@code mapItem} +
     * {@code .getMulti()} terminal. The consumer's OWN spine carries no {@code default}, so if this
     * test moves without a1 the walk is admitting on something other than the callee's body.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_consumerDelegatingToAMultiProvingRuleTakesTheListSurface() throws IOException {
        String out = rule("A2Consumer");
        String code = codeOnly(out);
        assertContains(out, "implements ReportFunction<Holder, List<");
        assertNotContains(out, "implements ReportFunction<Holder, String>");
        assertContains(out, "output = new ArrayList<>();");
        assertTrue(code.contains(".mapSingleToList(item -> MapperC."),
                "the rule-call wrap and method must both take the MULTI form:\n" + out);
        assertTrue(!code.contains(".mapSingleToItem(item -> MapperS.of(a1DefaultSpineRule"),
                "no single-form rule-call wrap may survive:\n" + out);
        assertTrue(code.contains(".mapItem(item -> {"),
                "the derived next step must read the MapperC-bound receiver:\n" + out);
        assertTrue(code.contains(".getMulti();"),
                "the whole-output terminal must collapse to the MULTI form:\n" + out);
        // PIN AT RED: replace with the byte-exact thenArg1 declaration + assignOutput lines once
        // the post-law render is captured (the local names are stable: thenArg0/thenArg1).
    }

    // =========================================================================
    // Part B — the decline locks (GREEN in BOTH states)
    // =========================================================================

    /**
     * b1 — the jfsa {@code UnderlyingIndexIndicator} witness reduced (drr 7.0–7.3, BAND, measured
     * {@code defaultSeen=true leftKind=RListOpExpr leftProvesMulti=false}): a {@code first}-collapsed
     * LEFT is not a multi proof, and {@code chainProvesMulti}'s own {@code RListOpExpr} arm declines
     * every collapsing op. Witness-unique: dropping the {@code chainProvesMulti(left)} conjunct is
     * the ONE edit that moves this file, and the probe measured what else it would move — 87 green
     * rows across 16 green rules.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b1_firstCollapsedLeftDefaultKeepsTheSingleSurface() throws IOException {
        String out = rule("B1CollapsedLeftDefault");
        assertContains(out, "implements ReportFunction<Holder, String>");
        assertNotContains(out, "implements ReportFunction<Holder, List<");
        assertTrue(!codeOnly(out).contains(".getMulti();"),
                "a collapsed left proves nothing — the terminal must stay single:\n" + out);
        // PIN AT RED: add the byte-exact `set`/assignOutput line captured from the pre-law run;
        // this file must be byte-identical before and after the law.
    }

    /**
     * b2 — a plain single-valued {@code default}. The spine carries a {@code default} but its left
     * is a {@code (0..1)} navigation, so the gate declines on the left test alone. This is the
     * shape 87 green rows corpus-wide take.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b2_plainSingleDefaultKeepsTheSingleSurface() throws IOException {
        String out = rule("B2PlainSingleDefault");
        assertContains(out, "implements ReportFunction<Holder, String>");
        assertNotContains(out, "implements ReportFunction<Holder, List<");
        assertTrue(!codeOnly(out).contains(".getMulti();"),
                "a single left proves nothing — the terminal must stay single:\n" + out);
    }

    /**
     * b3 — the COLLAPSE GUARD's only witness. a1's spine with a collapsing {@code only-element}
     * tail: the {@code default}'s left still proves MULTI ({@code leftProvesMulti=true}), so neither
     * the engine channel nor the {@code chainProvesMulti(left)} conjunct declines it — ONLY the
     * guard does. The corpus witnesses of this shape are the two drr 7.x FUNCTION rules
     * {@code GetUniqueTransactionIdentifier} and {@code UniqueSwapIdentifierForValuation}
     * ({@code (… default …) then distinct then only-element}, both {@code output (1..1)}, both
     * measured {@code leftProvesMulti=true} at {@code FER-thenExtractTerminal}); no rule-output law
     * can reach a function, so this fixture is the only place the guard can be pinned.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b3_collapsingTailAfterTheDefaultKeepsTheSingleSurface() throws IOException {
        String out = rule("B3CollapsingTailAfterDefault");
        assertContains(out, "implements ReportFunction<Holder, String>");
        assertNotContains(out, "implements ReportFunction<Holder, List<");
        assertTrue(!codeOnly(out).contains(".getMulti();"),
                "a collapsing tail makes the output single whatever the default proves:\n" + out);
    }

    // =========================================================================
    // Part C — the corpus carriers (8 whole-file heals across four drr 7.x
    // cells; this suite locks the 7.0.0 pair, the D11 ring locks all eight)
    // =========================================================================

    private static final String ESMA_CONSUMER =
            "drr/regulation/esma/emir/refit/trade/reports/IndicatorOfTheUnderlyingIndexRule.java";

    private static final String FCA_CONSUMER =
            "drr/regulation/fca/ukemir/refit/trade/reports/IndicatorOfTheUnderlyingIndexRule.java";

    /** NOT locked — a LAW-80 partial this seat cannot take whole (three other families own the rest). */
    private static final String EMIR_PRODUCER =
            "drr/regulation/common/emir/reports/IndicatorOfTheUnderlyingIndexRule.java";

    /** The BAND decline witness in the carrier cell — it must keep its current bytes. */
    private static final String JFSA_DECLINE =
            "drr/regulation/jfsa/rewrite/trade/reports/UnderlyingIndexIndicatorRule.java";

    /** The two GREEN report functions the flip repairs without moving a byte (LAW 74). */
    private static final String ESMA_REPORT_FN =
            "drr/regulation/esma/emir/refit/trade/reports/ESMAEMIRTradeReportFunction.java";

    private static final String FCA_REPORT_FN =
            "drr/regulation/fca/ukemir/refit/trade/reports/FCAUKEMIRTradeReportFunction.java";

    /** c1 — the WHOLE heal, esma. Its complete gen→golden delta is 24 lines, all owned by this seat. */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_esmaConsumerByteIdentical() throws IOException {
        lockA(ESMA_CONSUMER);
    }

    /** c1b — the WHOLE heal, fca. The same 24-line delta; the two consumers are a matched pair. */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1b_fcaConsumerByteIdentical() throws IOException {
        lockA(FCA_CONSUMER);
    }

    /**
     * control0 — GOLDEN IS THE ORACLE, quoted from real bytes. Three independent facts:
     * (i) golden's consumer carries the List signature, the MapperC decl + {@code mapSingleToList}
     * wrap and the {@code .getMulti()} terminal; (ii) golden's report FUNCTION call site is
     * {@code set…(rule.evaluate(input))} — unchanged by this law, so the file moves zero bytes; and
     * (iii) the generated report TYPE declares exactly ONE setter overload and it takes
     * {@code List<IndexEnum>} — which is what makes today's scalar-returning fork rule a compile
     * error in those two green files (LAW 74), and what makes golden's List signature the correct
     * one rather than merely the different one.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control0_goldenCarriesTheListSurfaceAndTheListOnlySetter() throws IOException {
        String esma = Files.readString(GOLDEN_A.resolve(ESMA_CONSUMER));
        assertTrue(esma.contains(
                "implements ReportFunction<TransactionReportInstruction, List<IndexEnum>>"),
                "golden's consumer must carry the List-signatured ReportFunction");
        assertTrue(esma.contains("List<IndexEnum> output = new ArrayList<>();"),
                "golden's consumer must initialise the output as a list");
        assertTrue(esma.contains("final MapperC<IndexEnum> thenArg1 = thenArg0"),
                "golden's consumer must declare the thenArg as a MapperC");
        assertTrue(esma.contains(".mapSingleToList(item -> MapperC.<IndexEnum>of("
                + "indicatorOfTheUnderlyingIndexRule.evaluate(item.get())));"),
                "golden's consumer must take the MULTI wrap AND the MULTI method");
        assertTrue(esma.contains("}).getMulti();"),
                "golden's consumer must take the MULTI terminal");
        assertTrue(!esma.contains(".mapSingleToItem(item -> MapperS.of("
                + "indicatorOfTheUnderlyingIndexRule"),
                "golden must NOT carry the single-form rule-call wrap");

        String reportFn = Files.readString(GOLDEN_A.resolve(ESMA_REPORT_FN));
        assertTrue(reportFn.contains(
                ".setIndicatorOfTheUnderlyingIndex(indicatorOfTheUnderlyingIndexRule.evaluate(input));"),
                "the report function's call site is unchanged by this law — it must be the bare"
                + " evaluate(input) form in golden, which is why those 8 green files move zero bytes");

        String reportType = Files.readString(GOLDEN_A.resolve(
                "drr/regulation/esma/emir/refit/trade/ESMAEMIRTransactionReport.java"));
        assertEquals(0, count(reportType, "setIndicatorOfTheUnderlyingIndex(IndexEnum "),
                "there must be NO scalar setter overload — the fork's scalar rule cannot type-check");
        assertTrue(count(reportType, "setIndicatorOfTheUnderlyingIndex(List<IndexEnum>") >= 1,
                "the ONLY setter overload takes List<IndexEnum> (the report type declares the field"
                + " `override indicatorOfTheUnderlyingIndex IndexEnum (0..*)`)");
    }

    /**
     * control1 — LAW 79, the whole-cell UNION scan on drr 7.0.0: per file the (MULTI rule signature,
     * {@code mapSingleToList} site, MULTI terminal) triple must equal golden's, file for file over
     * the UNION, beyond the NAMED residue. Its reach covers every rule in the carrier cell — the
     * jfsa decline witness, the emir producer's residual movement, and the two GREEN drr 7.x
     * FUNCTION rules that carry the collapse shape — so an over-fire into any of them fails here.
     * Under-fire fails too: a consumer that keeps the single form is a mismatch against golden.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control1_forkDrr7WholeCellOutputCardSitesEqualGoldenFileByFile() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");
        assertEquals(List.of(), drrAGenErrors,
                "drr 7.0.0 reported a generation error — the scan is incomplete");
        assertUnionEqual(scan(drrAOutput), scan(readGoldenTree(GOLDEN_A)), drrAOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_DRR7, DOMAIN_DRR7);
    }

    /** control2 — LAW 77 route parity on both carriers; the band is route-identical, so both must move. */
    @Test
    @EnabledIf("cellAAndIrProviderAvailable")
    void corpus_control2_irRouteMatchesLegacyForCarriers() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 legacy-route generation did not run");
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", CELL_A_ROOT), new ArrayList<>());
        for (String p : List.of(ESMA_CONSUMER, FCA_CONSUMER, EMIR_PRODUCER, JFSA_DECLINE,
                ESMA_REPORT_FN, FCA_REPORT_FN)) {
            assertEquals(drrAOutput.get(p), irOut.get(p), "route divergence: " + p);
        }
    }

    /**
     * control3 — LAW 79 on drr 5.61.0, the reach cell. The SAME rule names live here in their
     * already-correct form: emir {@code IndicatorOfTheUnderlyingIndex} is measured
     * {@code engineRuleBody=MULTI declaredMulti=true} (the engine channel already answers, and a
     * monotone add-only law must leave it alone), and jfsa {@code UnderlyingIndexIndicator} carries
     * {@code defaultSeen=false} plus a collapsing list-op on its leaf trail. If the new disjunct
     * disturbs an already-MULTI row or reaches a no-default spine, it fires here rather than only in
     * the carrier cell.
     */
    @Test
    @EnabledIf("cellBAvailable")
    void corpus_control3_forkDrr561WholeCellOutputCardSitesEqualGoldenFileByFile() throws IOException {
        assertNotNull(drrBOutput, "drr 5.61.0 generation did not run");
        assertEquals(List.of(), drrBGenErrors,
                "drr 5.61.0 reported a generation error — the scan is incomplete");
        assertUnionEqual(scan(drrBOutput), scan(readGoldenTree(GOLDEN_B)), drrBOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_DRR561, DOMAIN_DRR561);
    }

    /**
     * control4 — LAW 79 on cdm 6.21.0, the NON-drr tripwire. Every other control in this suite is
     * drr-scoped and this law's admit set is measured drr-only, so a cdm over-fire would be
     * invisible to all of them (the seat-28 law-6 lesson: a cell-scoped control cannot see a
     * cross-corpus over-fire). This is the cheap in-suite version; the mid-seat whole-matrix D11
     * checkpoint the verdict makes mandatory for this law still runs.
     */
    @Test
    @EnabledIf("cellCAvailable")
    void corpus_control4_forkCdm621WholeCellOutputCardSitesEqualGoldenFileByFile() throws IOException {
        assertNotNull(cdmCOutput, "cdm 6.21.0 generation did not run");
        assertEquals(List.of(), cdmCGenErrors,
                "cdm 6.21.0 reported a generation error — the scan is incomplete");
        assertUnionEqual(scan(cdmCOutput), scan(readGoldenTree(GOLDEN_C)), cdmCOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_CDM621, DOMAIN_CDM621);
    }

    /**
     * MEASURED at the law's head (LAW 73: the SET, not the count) - TWO rows:
     * (1) the emir ROOT IndicatorOfTheUnderlyingIndexRule - the law's OWN partial (its sig +
     * terminal move; the T3 token appears fork-side while golden renders the multi-default
     * TERNARY form still owned by the S30 families - the root is a 3-hunk multi-family band
     * file and heals ZERO whole this seat, LAW 80 said in advance); (2)
     * GetUnderlierProductIdentifierLeg1's fork-side T2, PRE-EXISTING in the pre-law dump
     * (gen T2=1, golden 0 - the F16w-family wrap divergence, this seat's law 5). Either row
     * healing OR a new row appearing fails this control (LAW 81).
     *
     * <p><b>RE-MEASURED at the SEAT-30 chain head {@code e223ce19}</b> — 1 entry, transcribed
     * VERBATIM from this control's own failing print (LAW 81), down from 2. Row (2) healed exactly
     * as row (2)'s own note predicted: <b>seat-30 law 5</b>
     * ({@code thenArgDeclKindFromCompiled} — the then-arg DECL reads the compiled stamp instead of
     * re-reading S) closed the F16w wrap divergence and healed
     * {@code GetUnderlierProductIdentifierLeg1.java} WHOLE in all four drr 7.x cells, so its
     * fork-side {@code .mapSingleToList(} site (T2) is gone. Row (1) is unchanged.
     */
    private static final List<String> KNOWN_RESIDUE_DRR7 = List.of();
            // the IndicatorOfTheUnderlyingIndexRule row (fork=[1, 1, 1] golden=[1, 1, 0]) LEFT this list at seat 32: law D.2
            // (extractBodyMultiDefaultTernary, on law D.1's left deref) healed it WHOLE in all four drr 7.x cells;
            // transcribed from this control's own print (D2-trip1.log).

    /** MEASURE at the law's head. Expected EMPTY — this cell carries no law-2 carrier at all. */
    private static final List<String> KNOWN_RESIDUE_DRR561 = List.of();

    /** MEASURE at the law's head. Expected EMPTY — a NON-empty set here is an over-fire, not residue. */
    private static final List<String> KNOWN_RESIDUE_CDM621 = List.of();

    /**
     * MEASURED at the seat: fill from the first corpus run — the drr 7.0.0 union domain
     * (golden ∪ fork token-bearing files, intersected with what this harness emits).
     *
     * <p><b>244 -> 243 at seat 30.</b> DERIVED, not printed: {@code assertUnionEqual} fires the
     * residue assert BEFORE the domain assert, so the seat-30 run never reached this line. Law 5's
     * whole heal of {@code GetUnderlierProductIdentifierLeg1} takes its fork triple to
     * {@code [0, 0, 0]} and golden's is already {@code [0, 0, 0]}, so {@code scan()}'s
     * {@code t1 + t2 + t3 > 0} predicate drops the file from BOTH sides of the union.
     *
     * <p><b>MEASURED-CONFIRMED at {@code 71a92e826}</b> (chain run 2). The run-2 GREEN leg is 0F
     * for this suite on both routes, so the residue assert PASSED and execution reached the domain
     * assert — which also passed against this value. 243 is no longer derived; the adjudication
     * the paragraph above asked the next chain for has happened, and it agreed.
     */
    private static final int DOMAIN_DRR7 = 243;

    /** MEASURED at the seat: fill from the first corpus run — the drr 5.61.0 union domain. */
    private static final int DOMAIN_DRR561 = 229;

    /** MEASURED at the seat: fill from the first corpus run — the cdm 6.21.0 union domain. */
    private static final int DOMAIN_CDM621 = 3;

    /**
     * (T1, T2, T3) = MULTI-output rule signatures ({@code implements ReportFunction<…, List<…>>}),
     * MULTI rule-call method sites ({@code .mapSingleToList(}) and MULTI whole-output terminals
     * (the lambda's closing brace followed by {@code ).getMulti();}). T1 is counted per LINE
     * because the signature's {@code , List<} must
     * belong to the {@code implements} clause and not to some unrelated declaration; T2 and T3 are
     * whole-file counts. No regex — plain literal matching on generated text, per the engineering
     * standard.
     */
    private static Map<String, int[]> scan(Map<String, String> tree) {
        Map<String, int[]> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : tree.entrySet()) {
            String code = codeOnly(e.getValue());
            int t1 = 0;
            for (String line : code.split("\n", -1)) {
                if (line.contains("implements ReportFunction<") && line.contains(", List<")) {
                    t1++;
                }
            }
            int t2 = count(code, ".mapSingleToList(");
            int t3 = count(code, "}).getMulti();");
            if (t1 + t2 + t3 > 0) {
                out.put(e.getKey(), new int[] {t1, t2, t3});
            }
        }
        return out;
    }

    private static int count(String haystack, String needle) {
        int n = 0;
        int i = haystack.indexOf(needle);
        while (i >= 0) {
            n++;
            i = haystack.indexOf(needle, i + needle.length());
        }
        return n;
    }

    // =========================================================================
    // The union assert (LAW 73: pin the SET, not the count)
    // =========================================================================

    /**
     * The scan universe is the token-bearing union INTERSECTED with the files this harness emits
     * (rule / report / function kinds): golden's DATA-RULE and POJO kinds can bear tokens these
     * generators never produce, and whether the fork emits every expected FILE is the D11 ring's
     * question (missingOutput), not this suite's.
     */
    private static void assertUnionEqual(Map<String, int[]> a, Map<String, int[]> b,
            java.util.Set<String> emittedA, String aName, String bName, List<String> knownResidue,
            int expectedDomain) {
        assertTrue(expectedDomain >= 0,
                "the union domain is MEASURED and pinned (LAW 73) - a negative value means an"
                + " unpinned call site");
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
        assertEquals(knownResidue, mismatched,
                "(T1, T2, T3) differ beyond the named residue in " + mismatched.size() + " file(s)");
        assertEquals(expectedDomain, universe.size(),
                "the union domain must equal the emitted token-bearing files (" + expectedDomain + ")");
    }

    // =========================================================================
    // Harness (the seat-27/28 suite shape verbatim)
    // =========================================================================

    private static Map<String, String> drrAOutput;
    private static List<String> drrAGenErrors;
    private static Map<String, String> drrBOutput;
    private static List<String> drrBGenErrors;
    private static Map<String, String> cdmCOutput;
    private static List<String> cdmCGenErrors;

    @BeforeAll
    static void generateCells() throws IOException {
        if (cellAAvailable()) {
            List<String> errs = new ArrayList<>();
            drrAOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", CELL_A_ROOT), errs);
            drrAGenErrors = errs;
        }
        if (cellBAvailable()) {
            List<String> errs = new ArrayList<>();
            drrBOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "5.61.0", CELL_B_ROOT), errs);
            drrBGenErrors = errs;
        }
        if (cellCAvailable()) {
            List<String> errs = new ArrayList<>();
            cdmCOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("cdm", "6.21.0", CELL_C_ROOT), errs);
            cdmCGenErrors = errs;
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

    /** The cell through the REAL {@code IRGeneration} seams (the D11 ON ring's wiring). */
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
            FunctionGenerator funcGen = IRGeneration.functionGenerator(gm, typeTranslator, typeUtil);
            assertTrue(!funcGen.getClass().equals(FunctionGenerator.class),
                    "the seam must hand back the IR-route FunctionGenerator, got " + funcGen.getClass());
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
            errors.forEach(e -> sink.add(e.getTargetPath() + " — " + e));
        }
    }

    private static void lockA(String path) throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run — corpus unavailable?");
        List<String> lockedErrors = drrAGenErrors.stream().filter(e -> e.contains(path)).toList();
        assertTrue(lockedErrors.isEmpty(),
                "the generator reported errors for the locked file " + path + ": " + lockedErrors);
        String generated = drrAOutput.get(path);
        assertNotNull(generated, "not generated in drr 7.0.0: " + path);
        Path goldenPath = GOLDEN_A.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "golden missing: " + goldenPath);
        assertEquals(normalize(Files.readString(goldenPath)), normalize(generated),
                "generated drr 7.0.0 output must byte-match golden (newline-normalized) for "
                + path + " — seat 29 law 2: a reporting rule whose body spine carries a default"
                + " whose LEFT proves MULTI has a MULTI output, and a rule delegating to such a"
                + " rule inherits it through the callee descent.");
    }

    private static void assertContains(String out, String needle) {
        assertTrue(out.contains(needle), "expected <" + needle + "> in:\n" + out);
    }

    private static void assertNotContains(String out, String needle) {
        assertTrue(!out.contains(needle), "did NOT expect <" + needle + "> in:\n" + out);
    }

    /** Strip line and block comments plus string literals so a javadoc or label never counts as code. */
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
                i = j + 1;
            } else if (ch == '/' && i + 1 < n && java.charAt(i + 1) == '/') {
                while (i < n && java.charAt(i) != '\n') {
                    i++;
                }
            } else if (ch == '/' && i + 1 < n && java.charAt(i + 1) == '*') {
                int e = java.indexOf("*/", i + 2);
                i = e < 0 ? n : e + 2;
            } else {
                sb.append(ch);
                i++;
            }
        }
        return sb.toString();
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n");
    }

    private static RLinkingResult linking;
    private static RModel mainModel;
    private static Map<String, String> fixtureOut;

    private static void link() throws IOException {
        if (linking == null) {
            RModel main = AstBuilder.buildFromString(MODEL, "seat29d.rosetta");
            main.setVersion("0.0.0.test");
            List<RModel> models = new ArrayList<>();
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

    private static Map<String, String> fixture() throws IOException {
        if (fixtureOut == null) {
            fixtureOut = render(m -> "census.seat29d".equals(m.namespace()));
        }
        return fixtureOut;
    }

    private static String rule(String ruleName) throws IOException {
        return lookup(fixture(), ruleName + "Rule.java");
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
            throw new AssertionError("[RuleOutputDefaultSpineSeatTest] builtins parse"
                    + " failures: " + String.join("; ", failures));
        }
        return models;
    }
}
