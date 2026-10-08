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
 * SEAT 32, law C.2 -- facet {@code switchBodyThenChainAdmit} (F7): <b>the #353
 * {@code switchBaseThenHoist} exemption exists at the chain-BASE arm and nowhere else, so a
 * then-BODY {@code extract} whose inline-body ROOT is a statically-renderable choice switch
 * reads UNHANDLED and the whole chain declines to the runtime {@code .then(} inline
 * fallback.</b> This law adds the missing bodies-loop arm, reading the IDENTICAL predicate.
 *
 * <p><b>The carrier</b> ({@code standards-iosco-cde-version1-basket-rule.rosetta:10-28},
 * {@code CustomBasketCodeRule}, drr 7.1.0 / 7.2.0 / 7.3.0 POJO, {@code 49 lines | 4 hunks}
 * each, composition class C017 = sigs {@code B073} + {@code B075} plus the two IMPORT sigs
 * {@code B001} ({@code cdm.product.template.NonTransferableProduct}) and {@code B008}
 * ({@code com.rosetta.model.lib.mapper.MapperC}), which are pure CONSEQUENCES of the hoisted
 * decl types):
 * <pre>
 * extract
 *     if reportableInformation -&gt; customBasket -&gt; customBasketCode exists
 *     then reportableInformation -&gt; customBasket -&gt; customBasketCode
 *     else (ProductForEvent
 *         then economicTerms -&gt; payout
 *         then extract (switch SettlementPayout then …, OptionPayout then …,
 *                              PerformancePayout then …, CreditDefaultPayout then …,
 *                              default empty)
 *         then only-element)
 * </pre>
 *
 * <p><b>Golden vs fork</b> (inside the {@code mapSingleToItem} block lambda):
 * <pre>
 * golden: final MapperS&lt;NonTransferableProduct&gt; thenArg0 = MapperS.of(productForEvent.evaluate(item.get()));
 *         final MapperC&lt;Payout&gt; thenArg1 = thenArg0.&lt;EconomicTerms&gt;map(…).&lt;Payout&gt;mapC(…);
 *         final MapperC&lt;String&gt; thenArg2 = thenArg1
 *             .mapItem(_item -&gt; { …the 4-case choice ladder… });
 *         return MapperS.of(thenArg2.get());
 * fork:   return productForEvent.evaluate(item.get()).then(_item -&gt; _item.&lt;EconomicTerms&gt;map(…)
 *             .&lt;Payout&gt;mapC(…)).then(_item -&gt; _item
 *             .mapItem(_item -&gt; { …the SAME 4-case ladder, byte-identical… })).then(_item -&gt; _item.get());
 * </pre>
 * The ladder text INSIDE the lambda is byte-identical on both sides -- the switch RENDERER
 * was never the defect. The defect is the ctl SCAN that decided the chain could not hoist.
 *
 * <p><b>The producer, and the GREEN twin that settles it (LAW 69).</b>
 * {@code CollectionHandler.thenChainHasUnhandledControlFlow}'s bodies loop falls to its tail
 * {@code if (subtreeHasControlFlow(b))}, which counts the {@code RSwitchExpr} and returns
 * true, so {@code tryDeepThenHoist} declines. The exemption that should apply already exists
 * -- at {@code subtreeHasUnhandledControlFlow}, for the chain BASE only, and the committed
 * comment at that seat said so outright: <i>"Bodies keep the plain walk (a then-BODY
 * extract-switch has no per-level machinery -- no carrier)."</i> CBC 7.1-7.3 <b>is</b> that
 * carrier now. And the two halves are witnessed disagreeing by a GREEN GOLDEN FILE: drr
 * 7.0.0's {@code CustomBasketCodeRule} is the IDENTICAL chain without the conditional
 * wrapper, reaches the SET seat ({@code isHoistableThenChain}, which never consults this
 * walk), and already renders {@code thenArg0/1/2} byte-identically to golden. At drr 7.1+
 * the chain moved into the conditional's block-lambda interior and reached the DEEP seat.
 * {@code corpus_control0} asserts BOTH goldens so the two-halves claim is measured here.
 *
 * <p><b>Blast radius, measured not assumed.</b> An {@code os.walk} over all 4,062 corpus
 * {@code .rosetta} files finds {@code then … extract … (switch} at exactly FOUR sites, all
 * four the same basket rule in drr 7.0/7.1/7.2/7.3; 7.0.0's is the SET-seat green twin this
 * arm never sees. The 41-file / 89-site {@code extract (switch} SUPERSET is the #353
 * BASE-position population (MapReturnSwap* / MapEquitySwap*, the mas rewrite rule), untouched
 * by a BODY arm. The one near-miss is the iosco cde v1 price rule's {@code FirstExerciseDate}
 * ({@code then extract trade [ … extract [ style switch … ] ]}) whose body ROOT is a nested
 * extract, not the switch -- {@code e1} locks that direction at fixture scale.
 *
 * <p><b>LAW 74 (compile).</b> The fork's current text is NON-COMPILING: there is no
 * {@code Mapper.then(Function)} anywhere in {@code rune-runtime}, and
 * {@code productForEvent.evaluate(...)} is a bare {@code NonTransferableProduct} used as a
 * Mapper receiver. Zero of the 174,141 goldens (the whole frozen baseline, 25 cells) carry
 * {@code .then(}, so every site this arm reaches was already a divergent, non-compiling file.
 * <b>LAW 74 MEASURED</b> ({@code javac32-report.md} section 7.3 row C17): PRE <b>one</b> error,
 * not three -- the first {@code .then} is fatal and javac stops the chain there -- POST 0.
 *
 * <p><b>LAW 77.</b> INHERITED, no IR twin. {@code IRCollectionHandler extends CollectionHandler}
 * with exactly ONE {@code @Override} ({@code thenArgBaseName}) and there is no
 * re-implementation of this walk in {@code rune-ir-java}. Render truth agrees: the seat-31
 * final dumps have CBC 7.1/7.2/7.3 {@code cmp}-identical between {@code d31final-off} and
 * {@code d31final-on}. {@code corpus_control2} re-proves it on the real IR seams.
 *
 * <p><b>CLAIMED sets (measured by the chain, not by this javadoc).</b>
 * <ul>
 *   <li><b>RED at the seat-32 base</b> (both routes): {@code a1}, {@code corpus_c1},
 *       {@code corpus_c2}, {@code corpus_control1} (residue 5 rows, CBC present as
 *       {@code fork=[3, 0, 0] golden=[0, 6, 2]}), and {@code corpus_control2} under
 *       {@code -Pir-on}. {@code e1}, {@code e2} and {@code corpus_control0} are GREEN at RED
 *       -- they are decline/no-move/oracle locks, not law witnesses.</li>
 *   <li><b>GREEN at the law's head</b>: 9/0F/1skip default, 9/0F/0skip {@code -Pir-on}.</li>
 * </ul>
 *
 * <p><b>MUTATIONS (LAW 66/76) -- MEASURED (LAW 82) by the seat-32 chain, run 1 at
 * {@code d99ded920} ({@code f32-mut-m-lawC2-*.log}):</b>
 * <ul>
 *   <li><b>m-lawC2-switch</b> -- {@code isStaticallyRenderableChoiceSwitch(swBodySwitch)}
 *       forced {@code false} in the new arm: MEASURED <b>9/4F/1S</b> = {@code a1},
 *       {@code corpus_c1}, {@code corpus_c2}, {@code corpus_control1} -- the claimed set
 *       exactly (the claim's "8/4F/1S" miscounted the suite, which has nine tests);
 *       {@code e1}/{@code e1b}/{@code e2}/{@code corpus_control0} GREEN.</li>
 *   <li><b>m-lawC2-recv</b> -- the receiver-walk conjunct deleted: MEASURED <b>9/0F/1S --
 *       EMPTY</b>, as DECLARED: at all four corpus sites the extract's argument is the implicit
 *       item, so the conjunct is defence-in-depth mirroring the #353 base arm's own receiver
 *       walk. A measured zero against a declared zero is an adjudication, recorded here, not a
 *       pass.</li>
 * </ul>
 * RED at the chain's base {@code ddcdd151b}: {@code a1}, {@code corpus_c1}, {@code corpus_c2},
 * {@code corpus_control1} (+ {@code corpus_control2} on {@code -Pir-on}); GREEN at the head
 * 9/0F/1skip default, 9/0F/0skip {@code -Pir-on}.
 *
 * <p><b>LAW-81 tripwires expected: NONE mechanical.</b> No other suite in the tree GENERATES
 * drr 7.1.0/7.2.0/7.3.0 (verified by grep over {@code src/test}: only
 * {@code AliasSwitchBareCaseNavSeatTest} and {@code CaseNarrowedBareNavCardinalitySeatTest}
 * name those cells, and both only read SIBLING GOLDENS for a cross-cell precondition). The
 * drr 7.0.0 green twin's own whole-file lock lives in
 * {@code ChoiceSwitchLambdaOptionGetterSeatTest.corpus_c1} and its whole-cell scans in
 * {@code DefaultRightNestedThenHoistSeatTest.corpus_control1} /
 * {@code AliasSwitchBareCaseNavSeatTest.corpus_control3} -- if this arm ever reached the SET
 * seat those three fire, which is exactly the alarm one wants.
 */
class SwitchBodyThenChainAdmitSeatTest {

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

    private static final String CBC =
            "drr/standards/iosco/cde/version1/basket/reports/CustomBasketCodeRule.java";

    /** Cell A = drr 7.1.0 -- the carrier, and the whole-cell control's domain. */
    private static final Path CELL_A_ROOT = Path.of("../test-corpus/drr/drr-7.1.0");
    private static final Path GOLDEN_A = CELL_A_ROOT.resolve("rosetta-source/src/generated/java");

    /**
     * Cell B = drr 7.3.0 -- the far end of the carrier's range. 7.1.0/7.2.0/7.3.0 carry a
     * BYTE-IDENTICAL golden and a BYTE-IDENTICAL fork render for this file (verified by
     * {@code cmp} over the seat-31 final dumps), so locking the two ends locks the middle.
     */
    private static final Path CELL_B_ROOT = Path.of("../test-corpus/drr/drr-7.3.0");
    private static final Path GOLDEN_B = CELL_B_ROOT.resolve("rosetta-source/src/generated/java");

    /**
     * The GREEN TWIN's golden tree -- drr 7.0.0, READ ONLY. This cell is never generated
     * here: its {@code CustomBasketCodeRule} is already byte-identical to golden and its
     * whole-file lock lives in {@code ChoiceSwitchLambdaOptionGetterSeatTest.corpus_c1}.
     * {@code corpus_control0} reads it to prove the two-halves-disagree claim from GOLDEN.
     */
    private static final Path GOLDEN_TWIN =
            Path.of("../test-corpus/drr/drr-7.0.0/rosetta-source/src/generated/java");

    static boolean cellAAvailable() {
        return Files.isDirectory(GOLDEN_A);
    }

    static boolean cellBAvailable() {
        return Files.isDirectory(GOLDEN_B);
    }

    static boolean cellAAndTwinAvailable() {
        return cellAAvailable() && Files.isDirectory(GOLDEN_TWIN);
    }

    static boolean cellAAndIrProviderAvailable() {
        return cellAAvailable() && irProviderOnClasspath();
    }

    // =========================================================================
    // Fixtures -- reduced from the REAL carrier source,
    // test-corpus/drr/drr-7.1.0/rosetta-source/src/main/rosetta/
    //     standards-iosco-cde-version1-basket-rule.rosetta:10-28
    // =========================================================================

    /**
     * The reduction, construct for construct:
     * <ul>
     *   <li>{@code Payout} (a model CHOICE whose options are navigated by getter, per seat-31
     *       law 4b) becomes {@code Leaf} with two options instead of four -- the switch's
     *       admissibility does not depend on the arity, and two cases keep the assertions
     *       readable.</li>
     *   <li>{@code ProductForEvent} (bare implicit-input call, output {@code (0..1)}) becomes
     *       {@code MakeWrap} -- same shape, same cardinality, so the base wrap is the same
     *       {@code MapperS.of(fn.evaluate(item.get()))}.</li>
     *   <li>{@code economicTerms -> payout} (a {@code (1..1)} hop then a {@code (0..*)} hop,
     *       i.e. {@code map} then {@code mapC}) becomes {@code holder -> leaves}.</li>
     *   <li>{@code basket.UnderlierBasketIdentifier(underlier)} case bodies become
     *       {@code TagOfA(item)} / {@code TagOfB(item)} -- fn-call case bodies over a
     *       model-choice item, the shape {@code ChoiceSwitchLambdaOptionGetterSeatTest}
     *       already proves renders.</li>
     *   <li>{@code default empty} and {@code then only-element} are kept verbatim.</li>
     * </ul>
     */
    private static final String MODEL = """
            namespace census.seat32c2
            version "1.0.0"

            type OptA:
                tagA string (0..1)

            type OptB:
                tagB string (0..1)

            choice Leaf:
                OptA
                OptB

            enum KindEnum:
                KindA
                KindB

            type Holder:
                leaves Leaf (0..*)
                kinds KindEnum (0..*)

            type Wrap:
                holder Holder (0..1)
                code string (0..1)

            type Root:
                wrap Wrap (0..1)

            func MakeWrap:
                inputs:
                    r Root (1..1)
                output:
                    w Wrap (0..1)
                set w:
                    r -> wrap

            func TagOfA:
                inputs:
                    a OptA (0..1)
                output:
                    s string (0..1)
                set s:
                    a -> tagA

            func TagOfB:
                inputs:
                    b OptB (0..1)
                output:
                    s string (0..1)
                set s:
                    b -> tagB

            reporting rule C2Cond from Root: <"a1 - THE CARRIER SHAPE: a conditional whose ELSE arm is a bare-function-rooted then-chain whose SECOND body is an extract over a statically-renderable CHOICE switch. The chain sits inside the extract's block lambda, so it reaches the DEEP seat whose bodies loop had no switch exemption.">
                extract
                    if wrap -> code exists
                    then wrap -> code
                    else (MakeWrap
                        then holder -> leaves
                        then extract
                            (switch
                                OptA then TagOfA(item),
                                OptB then TagOfB(item),
                                default empty)
                        then only-element)

            reporting rule C2CondCtlCase from Root: <"e1 - THE DECLINE LOCK: the same chain whose switch has a CONTROL-FLOW case body, so isStaticallyRenderableChoiceSwitch rejects it and the new arm must leave the decline exactly where it was.">
                extract
                    if wrap -> code exists
                    then wrap -> code
                    else (MakeWrap
                        then holder -> leaves
                        then extract
                            (switch
                                OptA then (if item -> tagA exists then TagOfA(item)),
                                OptB then TagOfB(item),
                                default empty)
                        then only-element)

            reporting rule C2CondEnumCtlCase from Root: <"e1b - THE DECLINE LOCK at a RENDERABLE seat: the same chain over an ENUM-keyed switch whose case body carries control flow. isStaticallyRenderableChoiceSwitch rejects it twice over (the guard is not a NAME kind; the body is ctl-carrying), so the runtime .then( fallback stays - and unlike the choice-keyed twin, the inline renderer CAN emit an enum ternary, so the decline is observable as text rather than as a refusal.">
                extract
                    if wrap -> code exists
                    then wrap -> code
                    else (MakeWrap
                        then holder -> kinds
                        then extract
                            (switch
                                KindA then (if item = KindEnum -> KindA then "a"),
                                KindB then "b",
                                default empty)
                        then only-element)

            reporting rule C2Set from Root: <"e2 - THE GREEN TWIN, drr 7.0.0's shape: the identical chain as the rule body ROOT, no conditional wrapper. It reaches the SET seat, which never consults this walk, and must be BYTE-UNCHANGED by the law.">
                extract MakeWrap
                then holder -> leaves
                then extract
                    (switch
                        OptA then TagOfA(item),
                        OptB then TagOfB(item),
                        default empty)
                then only-element
            """;

    /**
     * a1 -- the law. The chain hoists: the runtime {@code .then(} disappears and the three
     * golden decls appear in the block lambda.
     *
     * <p>Witness-uniqueness (the decline-lock law): {@code .then(} is a token the flip
     * REMOVES and that NO golden can carry -- the runtime has no {@code Mapper.then(Function)}
     * at all, so the whole {@code .then(}-carrying corpus population is divergent by
     * construction.
     *
     * <p>Every asserted token is transcribed from the carrier's own golden
     * ({@code test-corpus/drr/drr-7.1.0/…/CustomBasketCodeRule.java:60-62,85}) with the
     * fixture's names substituted. <b>PIN AT RED</b>: if the measured RED shape differs (e.g.
     * the declared element type of {@code thenArg1} is not the choice), reshape the FIXTURE
     * to the carrier -- never weaken the assert.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_switchBodyChainHoistsInsideTheBlockLambda() throws IOException {
        String out = fixtureRule("C2CondRule");
        assertTrue(!out.contains(".then("),
                "the runtime .then( inline fallback must be gone -- the runtime has no"
                + " Mapper.then(Function) and no golden carries it:\n" + out);
        assertContains(out, "thenArg0 = MapperS.of(makeWrap.evaluate(item.get()));");
        assertContains(out, "final MapperS<Wrap> thenArg0");
        assertContains(out, "final MapperC<Leaf> thenArg1 = thenArg0");
        assertContains(out, "final MapperC<String> thenArg2 = thenArg1");
        assertContains(out, ".mapItem(");
        assertContains(out, "return MapperS.of(thenArg2.get());");
    }

    /**
     * e1 -- the decline lock. {@code isStaticallyRenderableChoiceSwitch} rejects a switch
     * whose case body carries control flow ({@code body == null || subtreeHasControlFlow(body)}
     * at the committed {@code :6312-6339}), so the new arm must NOT fire and the pre-law
     * shape must stand.
     *
     * <p><b>MEASURED AT RED</b> ({@code C2-red2.log}, the pre-law render of this fixture): the
     * declined chain falls to the runtime {@code .then(} path whose inline ternary renderer
     * REFUSES a type-keyed case, so the pre-law shape is a {@code TYPE_SWITCH_TERNARY_STUB}
     * refusal with NO output -- not a {@code .then(}-bearing render. The lock asserts the refusal
     * survives: exactly one refusal for {@code C2CondCtlCaseRule.java}, its text still the
     * static-gate refusal, and no output. The law-relevant assert is the no-output one; the
     * {@code .then(}-text witness lives at a RENDERABLE seat in {@code e1b}.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void e1_ctlCarryingCaseBodyKeepsTheDecline() throws IOException {
        // MEASURED AT RED (C2-red2.log): the declined chain falls to the runtime `.then(` path whose
        // inline ternary renderer REFUSES a type-keyed case (TYPE_SWITCH_TERNARY_STUB, the v3.1 C0
        // refusal contract) - so the pre-law shape of this fixture is a refusal and NO output, and
        // that is exactly what the new arm must leave alone. The `.then(`-text witness lives in e1b.
        Render r = render();
        String path = "C2CondCtlCaseRule.java";
        List<String> own = r.errors().stream().filter(e -> e.contains(path)).toList();
        assertEquals(1, own.size(), "exactly one refusal expected for " + path + ": " + own);
        assertTrue(own.get(0).contains("TYPE_SWITCH_TERNARY_STUB"),
                "the refusal must stay the static-gate refusal: " + own.get(0));
        assertTrue(r.output().keySet().stream().noneMatch(k -> k.endsWith(path)),
                "a refused rule must produce no output: " + r.output().keySet());
    }

    /**
     * e1b -- the decline lock at a RENDERABLE seat. The same chain over an ENUM-keyed switch
     * with a control-flow case body: {@code isStaticallyRenderableChoiceSwitch} rejects it
     * (non-NAME guard, ctl-carrying body), the bodies-loop tail keeps declining, and the runtime
     * {@code .then(} fallback is what the inline renderer emits for an enum ternary. GREEN in
     * BOTH states; fails if the new arm ever admits a switch the gate rejects.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void e1b_enumCtlCarryingCaseBodyKeepsTheThenFallback() throws IOException {
        // v3.2 seat 12 (D52, COUNTERS FIRST - R1): the decline is observable as a REFUSAL now, not as text. The
        // static-renderability gate still rejects the chain (the guard is not a NAME kind; the body is ctl-carrying),
        // so no hoist happens - and the inline enum ternary the fallback used to write is refused at the residual
        // switch seat (SWITCH_TERNARY_STUB: `guards NAME/NAME/default; subject elided`), so the rule file is not
        // emitted at all. The test keeps its name as the seat-32c2 record of the decline lock.
        Render r = render();
        List<String> own = r.errors().stream().filter(e -> e.contains("C2CondEnumCtlCaseRule.java")).toList();
        assertEquals(1, own.size(), "exactly one refusal for the decline-lock rule: " + own);
        assertTrue(own.get(0).contains("[SWITCH_TERNARY_STUB]"), own.get(0));
        assertTrue(own.get(0).contains("guards NAME/NAME/default; subject elided"), own.get(0));
        assertTrue(r.output().keySet().stream().noneMatch(k -> k.endsWith("C2CondEnumCtlCaseRule.java")),
                "no file for a refused rule - and so no hoist either");
    }

    /**
     * e2 -- the GREEN TWIN at fixture scale, and the no-move witness for the SET seat. The
     * same chain as the rule body ROOT hoists TODAY through {@code isHoistableThenChain},
     * which never consults {@code thenChainHasUnhandledControlFlow}; this law must leave it
     * byte-for-byte alone. Tokens transcribed from drr 7.0.0's golden
     * {@code CustomBasketCodeRule.java:52-53,78}.
     *
     * <p>GREEN in BOTH states -- that is the point: it is the control that fails if the arm
     * is ever wired at the wrong seat.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void e2_theSetSeatTwinIsUnchanged() throws IOException {
        String out = fixtureRule("C2SetRule");
        assertTrue(!out.contains(".then("),
                "the SET seat already hoists this chain and must keep doing so:\n" + out);
        assertContains(out, "thenArg0 = MapperS.of(input)");
        assertContains(out, ".mapSingleToItem(item -> MapperS.of(makeWrap.evaluate(item.get())));");
        assertContains(out, "output = MapperS.of(thenArg2.get()).get();");
    }

    /** corpus_c1 -- the CustomBasketCodeRule whole-file heal, drr 7.1.0 iosco cde v1. */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_drr710CustomBasketCodeRuleMatchesGolden() throws IOException {
        assertNotNull(drrAOutput, "drr 7.1.0 generation did not run");
        List<String> own = drrAGenErrors.stream().filter(e -> e.contains(CBC)).toList();
        assertTrue(own.isEmpty(), "generation errors for " + CBC + ": " + own);
        String gen = drrAOutput.get(CBC);
        assertNotNull(gen, "not generated: " + CBC);
        assertEquals(normalize(Files.readString(GOLDEN_A.resolve(CBC))), normalize(gen),
                "CustomBasketCodeRule must byte-match golden - seat 32 law C.2: a then-BODY"
                + " extract over a statically-renderable choice switch is not unhandled"
                + " control flow");
    }

    /** corpus_c2 -- the same heal at the far end of the carrier's range, drr 7.3.0. */
    @Test
    @EnabledIf("cellBAvailable")
    void corpus_c2_drr730CustomBasketCodeRuleMatchesGolden() throws IOException {
        assertNotNull(drrBOutput, "drr 7.3.0 generation did not run");
        List<String> own = drrBGenErrors.stream().filter(e -> e.contains(CBC)).toList();
        assertTrue(own.isEmpty(), "generation errors for " + CBC + ": " + own);
        String gen = drrBOutput.get(CBC);
        assertNotNull(gen, "not generated: " + CBC);
        assertEquals(normalize(Files.readString(GOLDEN_B.resolve(CBC))), normalize(gen),
                "CustomBasketCodeRule must byte-match golden in drr 7.3.0 too - seat 32 law C.2");
    }

    /**
     * control0 -- GOLDEN is the oracle, and it must DISCRIMINATE (prove the instrument can
     * fail). Three facts, all read off frozen goldens, no generation:
     * <ol>
     *   <li>drr 7.1.0's golden carries {@code .then(} in ZERO files. The token this law
     *       removes cannot appear on the oracle side anywhere in the cell, which is what
     *       makes {@code control1}'s first column a corpus-wide hoist census rather than a
     *       spot check. Derived by a read-only walk of the golden tree; asserted live here
     *       so a corpus change cannot silently invalidate it.</li>
     *   <li>the carrier's golden carries the three hoisted decls and the wrapped return.</li>
     *   <li><b>the LAW-69 two-halves claim, measured:</b> drr 7.0.0's golden for the SAME
     *       basename carries the SET-seat form of the SAME chain -- so the fork's two halves
     *       disagreed about one shape that golden renders one way.</li>
     * </ol>
     */
    @Test
    @EnabledIf("cellAAndTwinAvailable")
    void corpus_control0_goldenDiscriminatesTheCarrierFromItsGreenTwin() throws IOException {
        Map<String, String> goldenTree = readGoldenTree(GOLDEN_A);
        List<String> thenBearing = new ArrayList<>();
        for (Map.Entry<String, String> e : goldenTree.entrySet()) {
            if (collapse(codeOnly(e.getValue())).contains(".then(")) {
                thenBearing.add(e.getKey());
            }
        }
        assertEquals(List.of(), thenBearing,
                "NO drr 7.1.0 golden may carry the runtime .then( form - the standing corpus"
                + " law, and the premise of control1's first column");

        String carrierRaw = goldenTree.get(CBC);
        assertNotNull(carrierRaw, "golden missing: " + GOLDEN_A.resolve(CBC));
        String carrier = collapse(carrierRaw);
        assertTrue(carrier.contains("final MapperS<NonTransferableProduct> thenArg0 ="
                + " MapperS.of(productForEvent.evaluate(item.get()));"),
                "golden must carry the hoisted base decl at the carrier");
        assertTrue(carrier.contains("final MapperC<Payout> thenArg1 = thenArg0"),
                "golden must carry the MapperC nav decl at the carrier");
        assertTrue(carrier.contains("final MapperC<String> thenArg2 = thenArg1 .mapItem(_item -> {"),
                "golden must carry the switch-body decl at the carrier");
        assertTrue(carrier.contains("return MapperS.of(thenArg2.get());"),
                "golden must carry the only-element consumer wrap at the carrier");

        Path twin = GOLDEN_TWIN.resolve(CBC);
        assertTrue(Files.isRegularFile(twin),
                "the GREEN TWIN oracle must exist (else this control is vacuous): " + twin);
        String twinText = collapse(Files.readString(twin));
        assertTrue(twinText.contains("final MapperS<NonTransferableProduct> thenArg0 ="
                + " MapperS.of(input) .mapSingleToItem(item ->"
                + " MapperS.of(productForEvent.evaluate(item.get())));"),
                "the drr 7.0.0 twin must carry the SET-seat hoist of the SAME chain - that is"
                + " the two-halves-disagree witness");
        assertTrue(twinText.contains("output = MapperS.of(thenArg2.get()).get();"),
                "the drr 7.0.0 twin must carry the SET-seat consumer wrap");
        assertTrue(!twinText.contains(".then("),
                "the green twin never carried the inline form either");
    }

    /**
     * control1 -- LAW 79, the UNION whole-cell scan on drr 7.1.0. The new arm sits in a walk
     * reached from EVERY value-then in the cell, so an enumerable 4-site source census is not
     * enough: this scan is the only instrument that can see an over-fire at a site no census
     * named, and an under-fire (the carrier not moving) shows up as the CBC row staying in
     * the residue.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control1_forkDrr710WholeCellHoistShapesEqualGoldenFileByFile() throws IOException {
        assertNotNull(drrAOutput, "drr 7.1.0 generation did not run");
        assertEquals(List.of(), drrAGenErrors,
                "drr 7.1.0 reported a generation error - the scan is incomplete");
        assertTrue(drrAOutput.containsKey(CBC),
                "the carrier must be INSIDE this scan's domain, else control1 proves nothing"
                + " about it (LAW: a control scans the domain it claims)");
        for (String residueFile : ROUTE_PARITY) {
            assertTrue(drrAOutput.containsKey(residueFile),
                    "the named residue file must be INSIDE this scan's domain: " + residueFile);
        }
        assertUnionEqual(scan(drrAOutput), scan(readGoldenTree(GOLDEN_A)), drrAOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_DRR71, DOMAIN_DRR71);
    }

    /**
     * control2 -- LAW 77 route parity. The law is a {@code CollectionHandler} arm the IR route
     * INHERITS, so the carrier must reach golden on the IR route too, and the cell's other
     * {@code .then(}-bearing files must render route-identically.
     *
     * <p><b>{@code drr/base/util/party/functions/CounterpartyRoleFromLEI.java} is
     * DELIBERATELY EXCLUDED</b>: it is the standing F18 OFF/ON divergence (pre-existing,
     * measured {@code OFF != ON} in the seat-31 final dumps, chartered separately). Asserting
     * whole-cell route equality here would fail on a defect this law does not own. Every file
     * in {@code ROUTE_PARITY} was verified {@code cmp}-identical between {@code d31final-off}
     * and {@code d31final-on} at the seat-31 head.
     */
    @Test
    @EnabledIf("cellAAndIrProviderAvailable")
    void corpus_control2_irRouteMatchesGoldenForTheCarrierAndLegacyForTheResidue()
            throws IOException {
        assertNotNull(drrAOutput, "drr 7.1.0 legacy-route generation did not run");
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "7.1.0", CELL_A_ROOT),
                new ArrayList<>());
        assertEquals(normalize(Files.readString(GOLDEN_A.resolve(CBC))),
                normalize(irOut.get(CBC)), "IR route vs GOLDEN: " + CBC);
        for (String path : ROUTE_PARITY) {
            assertEquals(drrAOutput.get(path), irOut.get(path), "route divergence: " + path);
        }
    }

    // =========================================================================
    // The pins
    // =========================================================================

    /**
     * The cell's OTHER {@code .then(}-bearing files -- every one a pre-existing band file of a
     * DIFFERENT family, none of them this law's. Route-parity checked in {@code control2};
     * {@code CounterpartyRoleFromLEI} is excluded there and here (F18).
     */
    private static final List<String> ROUTE_PARITY = List.of(
            "drr/ingest/fpml/recordkeeping/message/functions/MapNonpublicExecutionReportToWorkflowStep.java",
            "drr/standards/iosco/cde/version1/price/functions/Price.java",
            // the QuantityUnitOfMeasure ROUTE_PARITY `.then(`-carrier entry LEFT this list: law B.1 (setSeatNestedValueThenTogetherHoist,
            // the k>0 restructure window) healed this scan's token set to golden's in all four drr 7.x cells -
            // the file itself stays BANDED (its close is B.3 + B.24); transcribed from this control's own print
            // (the .then( token left the file - measured on the default route by DeepBareFunctionThenChainAdmit's and
            // InLambdaThenChainCtlAdmit's own prints in B1-trip1.log; the ON-route parity test re-measures at the chain).
            "drr/standards/iosco/cde/version1/quantity/functions/TotalNotionalQuantity.java");

    /**
     * The named PRE-EXISTING residue AFTER this law, pinned EXACTLY (LAW 73). DERIVED, not
     * invented: the fork differs from golden ONLY at drr 7.1.0's 19 band files (the ring is
     * EXACT everywhere else), so the residue is computable from the seat-31 final OFF dump
     * plus the frozen golden tree. The same derivation was CALIBRATED against two MEASURED
     * pins before being trusted -- it reproduces
     * {@code DefaultRightNestedThenHoistSeatTest}'s {@code KNOWN_RESIDUE_DRR7} (5 rows,
     * drr 7.0.0) and {@code KNOWN_RESIDUE_DRR637} (empty, drr 6.37.0) row for row.
     *
     * <p><b>At the seat-32 BASE this list has a FIFTH row</b> --
     * {@code drr/standards/iosco/cde/version1/basket/reports/CustomBasketCodeRule.java
     * fork=[3, 0, 0] golden=[0, 6, 2]} -- which is exactly how {@code control1} reads RED.
     *
     * <p><b>ORDER DEPENDENCY inside seat 32</b>: row 1 is scout C's law C.1 carrier
     * ({@code deepBareFunctionThenChainAdmit}, {@code MapNonpublicExecutionReportToWorkflowStep}
     * x4). If C.1 lands BEFORE C.2 that row moves or disappears and this pin must be
     * re-transcribed from the control's own failing print in the SAME commit.
     */
    private static final List<String> KNOWN_RESIDUE_DRR71 = List.of(
            // the MapNonpublicExecutionReportToWorkflowStep row (fork=[1, 16, 0] golden=[0, 18, 0]) LEFT this list at
            // seat 32: law C.1 (deepBareFunctionThenChainAdmit) healed that file WHOLE after this suite landed - a
            // cross-law tripwire caught by the checkpoint-1 full gensuite (ckpt1-gensuite.log), transcribed from its print.
            // the Price row (fork=[0, 2, 1] golden=[0, 13, 1]) LEFT this list: law C.1 (iteChainNestedThenLadderAdmit + R2a/R2b/R4)
            // rendered the seven-rung ladder as statements and took this scan's token set to golden's in
            // all four drr 7.x cells - the file stays BANDED on C.2's default join; transcribed from this
            // control's own print (C1-trip1.log), a pure row removal (was == expected minus it).
            // the QuantityUnitOfMeasure row (fork=[3, 14, 1] golden=[0, 20, 4]) LEFT this list: law B.1 (setSeatNestedValueThenTogetherHoist,
            // the k>0 restructure window) healed this scan's token set to golden's in all four drr 7.x cells -
            // the file itself stays BANDED (its close is B.3 + B.24); transcribed from this control's own print
            // (B1-trip1.log), a pure row removal (was == expected minus it).
            // the TotalNotionalQuantity row (fork=[3, 4, 0] golden=[0, 18, 2]) LEFT this list: law D.3 (fnDeepCondBaseConfinedArmChainAdmit,
            // seven rungs) healed the file WHOLE in all four drr 7.x cells; transcribed from this control's own
            // print (D3-trip1.log), a pure row removal (was == expected minus it).
            );

    /**
     * The union domain, DERIVED and pinned (LAW 73) -- a negative value is the SENTINEL that
     * makes {@code assertUnionEqual} print the measured value instead of passing vacuously.
     *
     * <p><b>Derivation, and its calibration.</b> {@code universe} is the token-bearing files
     * of either tree RESTRICTED to what the fork actually emits, and {@code generateCell}
     * here runs the RULE, REPORT and FUNCTION generators only -- so the emitted key set is
     * the cell's {@code /functions/} and {@code /reports/} files. A read-only walk of the
     * frozen golden tree under that restriction, with the band files' fork text substituted
     * from the seat-31 OFF dump, gives 1585 for drr 7.1.0 on this token triple. The SAME walk
     * reproduces {@code DefaultRightNestedThenHoistSeatTest}'s two MEASURED pins EXACTLY
     * (1261 for drr 7.0.0, 1125 for drr 6.37.0, on that suite's token triple), which is why
     * the number is pinned rather than sentinelled. The domain does NOT move across this law:
     * the carrier is token-bearing on both sides before and after.
     *
     * <p>Falsifier: the domain assert itself failing on a later run. Re-derive from the
     * control's own scan; do NOT wait for the "MEASURED domain=" print, which only surfaces
     * while the value is the sentinel.
     */
    private static final int DOMAIN_DRR71 = 1585;

    /**
     * (T1, T2, T3) per file -- the law's three directions:
     * <ul>
     *   <li><b>T1</b> {@code .then(} -- the REMOVED inline fallback. Golden's count is ZERO in
     *       every file of the cell ({@code control0} asserts it), so this column alone makes
     *       the scan a corpus-wide hoist census: any occurrence is a declined hoist.</li>
     *   <li><b>T2</b> {@code thenArg} -- the ADDED hoist bindings (decls AND uses). The
     *       carrier moves 0 -&gt; 6. A moved T2 anywhere else is an over-fire.</li>
     *   <li><b>T3</b> {@code final MapperC&lt;} -- the ADDED decl KIND. The carrier moves
     *       0 -&gt; 2; this is the column that would catch a hoist landing with the wrong
     *       cardinality even if T1/T2 agreed.</li>
     * </ul>
     * Counted over CODE only ({@code codeOnly} strips comments and string literals, so a
     * javadoc or a generated {@code map("getX", …)} name can never score).
     */
    private static Map<String, int[]> scan(Map<String, String> tree) {
        Map<String, int[]> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : tree.entrySet()) {
            String flat = collapse(codeOnly(e.getValue()));
            int t1 = count(flat, ".then(");
            int t2 = count(flat, "thenArg");
            int t3 = count(flat, "final MapperC<");
            if (t1 + t2 + t3 > 0) {
                out.put(e.getKey(), new int[] {t1, t2, t3});
            }
        }
        return out;
    }

    private static int count(String haystack, String needle) {
        int n = 0;
        int from = 0;
        while ((from = haystack.indexOf(needle, from)) >= 0) {
            n++;
            from += needle.length();
        }
        return n;
    }

    // =========================================================================
    // The union assert (LAW 73: pin the SET, not the count) - the seat-30 shape verbatim
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
        // The domain-pin flow (LAW 73): the sentinel failure PRINTS the measured values so
        // the pin is transcribed from this assert's own output, never invented.
        assertTrue(expectedDomain >= 0,
                "the union domain is MEASURED and pinned (LAW 73) - transcribe from this"
                        + " print: MEASURED domain=" + universe.size()
                        + " residue=" + mismatched);
        assertEquals(knownResidue, mismatched,
                "(T1, T2, T3) differ beyond the named residue in " + mismatched.size() + " file(s)");
        assertEquals(expectedDomain, universe.size(),
                "the union domain must equal the emitted token-bearing files (" + expectedDomain + ")");
    }

    // =========================================================================
    // Corpus harness (the seat-30 DefaultRightNestedThenHoistSeatTest shape verbatim)
    // =========================================================================

    private static Map<String, String> drrAOutput;
    private static List<String> drrAGenErrors;
    private static Map<String, String> drrBOutput;
    private static List<String> drrBGenErrors;

    @BeforeAll
    static void generateCells() throws IOException {
        if (cellAAvailable()) {
            List<String> errs = new ArrayList<>();
            drrAOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "7.1.0", CELL_A_ROOT), errs);
            drrAGenErrors = errs;
        }
        if (cellBAvailable()) {
            List<String> errs = new ArrayList<>();
            drrBOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "7.3.0", CELL_B_ROOT), errs);
            drrBGenErrors = errs;
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

    private static String collapse(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        boolean inWs = false;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n') {
                inWs = true;
                continue;
            }
            if (inWs && sb.length() > 0) {
                sb.append(' ');
            }
            inWs = false;
            sb.append(ch);
        }
        return sb.toString();
    }

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

    // =========================================================================
    // Fixture harness (the InLambdaBoolHoistShadowSeatTest rule renderer, verbatim)
    // =========================================================================

    private record Render(Map<String, String> output, List<String> errors) {}

    private static Render rendered;

    private static Render render() throws IOException {
        if (rendered != null) {
            return rendered;
        }
        RModel main = AstBuilder.buildFromString(MODEL, "seat32c2.rosetta");
        main.setVersion("0.0.0.test");
        List<RModel> models = new ArrayList<>();
        models.add(main);
        models.addAll(loadBuiltinsOnly());
        RWorkspace workspace = RWorkspace.build(models).workspace();
        GeneratorModel gm = new GeneratorModel(workspace,
                m -> "census.seat32c2".equals(m.namespace()));
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
        rendered = new Render(out, errors);
        return rendered;
    }

    private static String fixtureRule(String ruleName) throws IOException {
        Render r = render();
        String path = ruleName + ".java";
        List<String> own = r.errors().stream().filter(e -> e.contains(path)).toList();
        assertTrue(own.isEmpty(), "the generator reported errors for " + path + ": " + own);
        String out = r.output().entrySet().stream()
                .filter(e -> e.getKey().endsWith(path))
                .map(Map.Entry::getValue)
                .findFirst().orElse(null);
        assertNotNull(out, "not generated: " + path + " (have: " + r.output().keySet() + ")");
        return out;
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
            throw new AssertionError("[SwitchBodyThenChainAdmitSeatTest] builtins parse"
                    + " failures: " + String.join("; ", failures));
        }
        return models;
    }

}
