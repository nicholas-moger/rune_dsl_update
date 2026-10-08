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
 * SEAT 28, law 4 — facet {@code thenWrappedDefaultRewrap}: <b>a ctor-setter value that is a THEN chain
 * whose BODY is a {@code default} takes upstream's identity round-trip
 * {@code MapperS.of(&lt;item&gt;).get()}</b>, and — on the LEGACY route only — <b>a {@code then default}
 * alias receiver keeps its {@code &lt;T&gt;map} type witness</b>.
 *
 * <p>The law has TWO rungs and the file is WHOLE only if both land:
 * <ol>
 *   <li><b>(A) BOTH routes</b> — {@code ConstructionHandler.coerceCtorArg}. The {@code default} body is
 *       item-typed ({@code Mapper.getOrDefault(T)} returns the VALUE — the {@code #S2} law at
 *       {@code :726}) but the enclosing {@code then} is Mapper-typed, so upstream re-wraps the item and
 *       the ctor-setter's own item extraction collapses it again. <b>The census's stated fix — "walk the
 *       {@code RThenExpr} to its body before the {@code instanceof RDefaultExpr} test" — produces the
 *       WRONG BYTES</b>: the {@code :726} arm's action is a BARE splice, so widening it emits
 *       {@code .setPeriod(thenArg.getOrDefault(FrequencyPeriodEnum.ADHO))} where golden has
 *       {@code MapperS.of(…).get()}. This is a NEW disjoint rung modelled on
 *       {@code innerCtorMapperSRoundTrip} ({@code :839-847}), and {@code :726} is left untouched so its
 *       80 live rows ({@code Create_SubmissionCore} 40 + {@code Create_SubmissionHeader} 40) stay
 *       byte-frozen.</li>
 *   <li><b>(B) OFF ROUTE ONLY — LAW 77 in reverse</b> — {@code NavigationHandler.resolveReceiverDataType0}.
 *       The grammar's {@code DefaultWithoutLeftExpr} alternative leaves {@code rawLeft()} NULL by parse
 *       ({@code AstBuilder.visitDefaultWithoutLeftExpr} sets only {@code right}; {@code default} is not
 *       among the ten elided-operand synthesis slots — the #513 IR-adapter law), so the {@code #362}
 *       {@code aliasDefaultJoin} arm at {@code :7912-7916} computes {@code ancestorJoin(null, X)}, which
 *       declines at {@code :8022}. The whole alias types null, {@code resolveTypeParam} returns
 *       {@code ""}, and the {@code &lt;T&gt;} witness AND its import vanish. The IR route never walks the
 *       receiver — {@code IRJavaLeafEmitter.emitFieldAccess} reads the node's own
 *       {@code ws.getInferredType(expr)} stamp — which is why the ON dump carries ONE hunk for this
 *       file where the OFF dump carries TWO.</li>
 * </ol>
 *
 * <p><b>The golden vs fork shape</b> (drr 7.0.0 {@code QuantityFrequency}; identical in 7.1/7.2/7.3):
 * <pre>
 * rung A, BOTH routes (golden line 69):
 * golden:  .setPeriod(MapperS.of(thenArg.getOrDefault(FrequencyPeriodEnum.ADHO)).get())
 * fork:    .setPeriod(          thenArg.getOrDefault(FrequencyPeriodEnum.ADHO) .get())
 *
 * rung B, OFF route only (golden lines 67 and 70):
 * golden:  calculationPeriodFrequency(payout).&lt;PeriodExtendedEnum&gt;map("getPeriod", …)
 *          calculationPeriodFrequency(payout).&lt;Integer&gt;map("getPeriodMultiplier", …)
 * fork:    calculationPeriodFrequency(payout).map("getPeriod", …)
 *          calculationPeriodFrequency(payout).map("getPeriodMultiplier", …)
 * </pre>
 * The discriminator for rung B is the ALIAS BODY, not the receiver kind: the same file's
 * {@code frequencyPeriod(payout)} hops (golden lines 66, 72, 74, 75) keep their witness in the fork
 * because that alias's body is a plain nav chain, while {@code calculationPeriodFrequency}'s body is
 * {@code … then default … then default …}. <b>Never assert on the lambda variable</b>: the
 * {@code _calculationPeriodFrequency} fallback name coincides with the type-derived name here because
 * the alias is named after its own type.
 *
 * <p><b>LAW 74</b>: rung A's fork form does not compile — {@code Mapper#getOrDefault} is
 * {@code T getOrDefault(T defaultValue)}, so {@code thenArg.getOrDefault(FrequencyPeriodEnum.ADHO)} is
 * a bare enum and {@code .get()} on an enum does not exist. Green-safe by construction: no green golden
 * can carry it. Rung B by contrast COMPILES ({@code public <F> MapperS<F> map(String, Function<T, F>)}
 * infers {@code F} from the lambda), so rung B needs the strictly-additive-walk argument instead — see
 * the probe note below.
 *
 * <p><b>The probe verdict this law answers (LAW 75 — {@code PROBE28-F10d}, 773 lines corpus-wide).</b>
 * <ul>
 *   <li>{@code thenBodyIsDefault=true} occurs <b>EXACTLY 4 times corpus-wide</b>, one per drr 7.x cell,
 *       all {@code fn:QuantityFrequency}, <b>all band, zero green</b>.</li>
 *   <li>{@code srcHasGetOrDefault=true} and {@code srcEndsGet=false} ⟹ {@code src} already holds the
 *       rendered {@code thenArg.getOrDefault(FrequencyPeriodEnum.ADHO)}; the fork's trailing
 *       {@code .get()} is appended by the {@code :897} cardinality fall-through.</li>
 *   <li>{@code valueCard=SINGLE} ⟹ {@code gm.workspace().getCardinality(pair.value())} on the
 *       {@code RThenExpr} NODE already answers SINGLE — <b>the cardinality read needs no redirect to the
 *       then body</b>.</li>
 *   <li>{@code armTaken=false} at all 4 rows; the {@code :726} arm's live population is 80 rows, all
 *       {@code valueKind=RDefaultExpr} ({@code isThen=false}) — disjoint from this rung by shape.</li>
 *   <li><b>PROBE GAP, stated (LAW 75):</b> no seat-28 tag instruments the {@code resolveReceiverDataType0}
 *       {@code RDefaultExpr} arm, so <b>rung B's walk reach is UNSIZED</b>. What is known: the arm returns
 *       {@code null} for EVERY left-null {@code default} today, so the change is strictly additive at the
 *       walk (a decline becomes a resolution; a resolution never becomes a different one) — and
 *       {@code corpus_control1}/{@code corpus_control3} are what turn that into a MEASURED claim at the
 *       bytes.</li>
 * </ul>
 *
 * <p><b>RED at the pre-seat blob</b>: a1, a2, corpus_c1, corpus_control1. b1, b2, b3,
 * corpus_control0, corpus_control2 GREEN in both states.
 *
 * <p><b>LAW 66/76 mutations</b> — the seat's chain ran ONE on this suite: the WHOLE law reverted
 * (both rungs), whose RECORDED failing set is in the MEASURED MUTATIONS block below. The planned
 * severs were NOT RUN as such — each is <b>NOT RUN, banked to S29</b>: (i) rung A's new ctor rung
 * deleted; (ii) rung A's whole-wrap guard ({@code unwrapMapperSOf}) removed — a then body already
 * rooted in {@code MapperS.of} would double-wrap; (iii) rung B's {@code rawLeft() == null} branch
 * reverted to the unconditional {@code resolveReceiverDataType(defaultRecv.rawLeft(), …)}.
 * <p><b>MEASURED MUTATIONS (LAW 82 - the seat-28 mut28 suite-lane loop; each
 * mutation = the named apply-script reverted, the suite run, the script re-applied;
 * every set below is the RECORDED failing set from that run, never a claim):</b>
 * <ul>
 *   <li>the whole law reverted (law4-apply --revert: both rungs) -> a1, a2, corpus_c1, corpus_control1 (4F)</li>
 * </ul>
 */
class ThenWrappedDefaultSeatTest {

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

    /** cell A = drr 7.0.0 — the QuantityFrequency carrier (the sibling cells 7.1/7.2/7.3 carry the same row). */
    private static final Path CELL_A_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path GOLDEN_A = CELL_A_ROOT.resolve("rosetta-source/src/generated/java");
    /** cell B = drr 5.61.0 — NO carrier: the pure over-fire cell for rung B's receiver-type walk. */
    private static final Path CELL_B_ROOT = Path.of("../test-corpus/drr/drr-5.61.0");
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

    /**
     * A1 = rung A: a ctor-setter whose value is {@code &lt;call&gt; then default &lt;enum&gt;} — the
     * {@code QuantityFrequency} {@code period:} pair verbatim. A2 = rung B: a nav hop off an alias whose
     * body is {@code … then default …} (the {@code calculationPeriodFrequency} shape). B1 = a BARE
     * {@code RDefaultExpr} ctor value — the {@code #S2} arm at {@code :726} owns it and splices bare.
     * B2 = an {@code RThenExpr} ctor value whose body is NOT a default — the cardinality fall-through
     * owns it. B3 = a nav hop off an alias whose body is a plain chain — the witness already resolves
     * and must not change.
     */
    private static final String MODEL = """
            namespace census.seat28d
            version "1.0.0"

            enum Period:
                ADHO
                DAIL

            type Freq:
                period Period (0..1)
                multiplier int (0..1)
                note string (0..1)

            type Wrapper:
                primary Freq (0..1)
                secondary Freq (0..1)

            type Out:
                period Period (0..1)
                multiplier int (0..1)
                mark string (0..1)

            func A1CtorThenWrappedDefault: <"a1 - rung A: a ctor setter whose value is `<x> then default <enum>`">
                inputs:
                    w Wrapper (1..1)
                output:
                    out Out (1..1)
                set out:
                    Out {
                        period: w -> primary -> period then default ADHO,
                        multiplier: w -> primary -> multiplier
                    }

            func A2AliasWithThenDefaultBody: <"a2 - rung B: a nav hop off a `then default` alias body">
                inputs:
                    w Wrapper (1..1)
                output:
                    result Period (0..1)
                alias joined:
                    w -> primary
                        then default w -> secondary
                set result:
                    joined -> period

            func B1BareDefaultCtorValue: <"b1 - a BARE default ctor value: the #S2 arm keeps its bare splice">
                inputs:
                    w Wrapper (1..1)
                output:
                    out Out (1..1)
                set out:
                    Out {
                        period: w -> primary -> period default w -> secondary -> period,
                        multiplier: w -> primary -> multiplier
                    }

            func B2ThenWrappedNonDefaultCtorValue: <"b2 - a then-wrapped value whose body is NOT a default">
                inputs:
                    w Wrapper (1..1)
                output:
                    out Out (1..1)
                set out:
                    Out {
                        period: w -> primary then period,
                        multiplier: w -> primary -> multiplier
                    }

            func B3PlainAliasBody: <"b3 - a plain-chain alias body: the witness already resolves">
                inputs:
                    w Wrapper (1..1)
                output:
                    result Period (0..1)
                alias plain:
                    w -> primary
                set result:
                    plain -> period
            """;

    /**
     * a1 — rung A: the then-wrapped {@code default} ctor value takes the identity round-trip. BOTH
     * directions are asserted (the decline-lock law applied to a positive): the wrap must appear AND the
     * bare {@code .getOrDefault(…).get()} form the flip removes must be gone.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_ctorThenWrappedDefaultTakesTheRoundTrip() throws IOException {
        String out = fn("A1CtorThenWrappedDefault.java");
        String code = codeOnly(out);
        assertContains(out, ".setPeriod(MapperS.of(");
        assertTrue(code.contains(".getOrDefault(Period.ADHO)).get())"),
                "the then-wrapped default must re-wrap MapperS.of and collapse .get() OUTSIDE it:\n" + out);
        assertTrue(!code.contains(".getOrDefault(Period.ADHO).get()"),
                "the un-wrapped form is the token the flip REMOVES — a .get() on a bare enum:\n" + out);
    }

    /**
     * a2 — rung B: the nav hop off a {@code then default} alias body recovers its {@code &lt;T&gt;}
     * witness. Assert the WITNESS, never the lambda variable (the corpus carrier's fallback lambda name
     * coincidentally equals the type-derived one). The import is the second half of the same rung —
     * {@code addWitnessTypeRef} declines on a null attribute, so a witness without its import means only
     * half the lockstep fired.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_thenDefaultAliasReceiverKeepsTheTypeWitness() throws IOException {
        String out = fn("A2AliasWithThenDefaultBody.java");
        // The witness lives next to a string literal, so the POSITIVE assert runs against the raw
        // output (codeOnly() strips literals); the NEGATIVE runs against codeOnly(), where the
        // receiver prefix survives and only the getter literal is gone.
        assertContains(out, ".<Period>map(\"getPeriod\"");
        assertTrue(!codeOnly(out).contains("joined(w).map("),
                "a bare .map( off the alias receiver means the receiver-type walk still declines:\n" + out);
        // PIN AT RED: add the WITNESS IMPORT assert here once the GREEN run names it. addWitnessTypeRef
        // (NavigationHandler:610) declines on a null attribute exactly as resolveTypeParam does, so the
        // witness and its import are ONE lockstep and both halves must be pinned — a witness without its
        // import is a half-fire, and the corpus carrier's witnesses are cross-namespace.
    }

    /**
     * b1 — the decline pin (LAW 76 witness-uniqueness): a BARE {@code RDefaultExpr} ctor value belongs to
     * the {@code #S2} arm at {@code ConstructionHandler:726}, whose 80 live corpus rows
     * ({@code Create_SubmissionCore} + {@code Create_SubmissionHeader}) splice BARE. This file must carry
     * NEITHER token rung A adds — no {@code MapperS.of(} around the {@code getOrDefault} and no trailing
     * {@code ).get()} — and must keep the bare splice.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b1_bareDefaultCtorValueKeepsTodaysBytes() throws IOException {
        String out = fn("B1BareDefaultCtorValue.java");
        String code = codeOnly(out);
        assertTrue(code.contains(".setPeriod(") && code.contains(".getOrDefault("),
                "the #S2 bare splice must survive:\n" + out);
        // MEASURED pins (witness uniqueness - the draft's substring negatives false-tripped
        // on the ORDINARY MapperS.of(w) input wraps present in both states): rung A firing
        // here would ADD one whole-wrap MapperS.of( and one round-trip .get()) - the counts
        // are the flip-sensitive witnesses. Measured identical PRE and POST law 4.
        assertEquals(3, count(code, "MapperS.of("),
                "rung A must not add a whole-wrap at the bare-default #S2 seat:\n" + out);
        assertEquals(2, count(code, ".get())"),
                "rung A's round-trip collapse must not reach the bare-default #S2 seat:\n" + out);
    }

    /**
     * b2 — the decline pin: a then-wrapped ctor value whose body is NOT a {@code default}. The new rung's
     * gate is {@code ctorSetterValueEndsInDefault}, so the cardinality fall-through at
     * {@code ConstructionHandler:897} keeps owning this shape and its bytes must not move.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b2_thenWrappedNonDefaultCtorValueKeepsTodaysBytes() throws IOException {
        String out = fn("B2ThenWrappedNonDefaultCtorValue.java");
        String code = codeOnly(out);
        assertTrue(!code.contains(".setPeriod(MapperS.of(MapperS.of("),
                "the new rung must not double-wrap a non-default then body:\n" + out);
        // PIN AT RED: replace with the byte-exact `.setPeriod(…)` line captured from the pre-seat run.
    }

    /**
     * b3 — the decline pin for rung B: an alias whose body is a PLAIN nav chain already resolves through
     * the untouched {@code :7509-7539} alias arm, so its witness must be byte-identical before and after.
     * This is the fixture analogue of the carrier's own internal control — in golden
     * {@code QuantityFrequency}, {@code frequencyPeriod(payout)} keeps its witness in the FORK today
     * while {@code calculationPeriodFrequency(payout)} loses it.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b3_plainAliasBodyWitnessUnchanged() throws IOException {
        String out = fn("B3PlainAliasBody.java");
        assertTrue(out.contains(".<Period>map(\"getPeriod\""),
                "a plain-chain alias body must already carry its witness, before and after:\n" + out);
    }

    // =========================================================================
    // Part C — the corpus carrier (4 whole-file rows, one cell family)
    // =========================================================================

    private static final String QUANTITY_FREQUENCY =
            "drr/regulation/common/trade/quantity/functions/QuantityFrequency.java";

    /** c1 — the WHOLE heal: byte-identical only when BOTH rungs land (rung A alone leaves lines 67 and 70). */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_quantityFrequencyByteIdentical() throws IOException {
        lockA(QUANTITY_FREQUENCY);
    }

    /** control0 — golden is the oracle for both rungs at the carrier. */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control0_goldenCarriesBothRungs() throws IOException {
        String golden = Files.readString(GOLDEN_A.resolve(QUANTITY_FREQUENCY));
        assertTrue(golden.contains(".setPeriod(MapperS.of(thenArg.getOrDefault(FrequencyPeriodEnum.ADHO)).get())"),
                "golden must carry rung A's identity round-trip");
        assertTrue(golden.contains("calculationPeriodFrequency(payout).<PeriodExtendedEnum>map(\"getPeriod\""),
                "golden must carry rung B's <PeriodExtendedEnum> witness");
        assertTrue(golden.contains("calculationPeriodFrequency(payout).<Integer>map(\"getPeriodMultiplier\""),
                "golden must carry rung B's <Integer> witness");
        assertTrue(!golden.contains("calculationPeriodFrequency(payout).map("),
                "golden must carry NO bare .map( off the then-default alias receiver");
    }

    /**
     * control1 — LAW 79, the whole-cell UNION scan on drr 7.0.0: per file the (witnessed hop, bare hop,
     * bare-arg round trip) triple must equal golden's, file for file over the UNION, beyond the NAMED
     * residue. This is the control that SIZES rung B, because no probe does — an over-fire of the
     * receiver-type walk shows up here as a witnessed hop golden does not have.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control1_forkDrr7WholeCellWitnessSitesEqualGoldenFileByFile() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");
        assertEquals(List.of(), drrAGenErrors, "drr 7.0.0 reported a generation error — the scan is incomplete");
        assertUnionEqual(scan(drrAOutput), scan(readGoldenTree(GOLDEN_A)), drrAOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_7, DOMAIN_DRR7);
    }

    /**
     * MEASURED AT THE CHAIN (LAW 82). <b>RE-MEASURED at the seat-30 chain head {@code e223ce19}</b>
     * — 13 entries, transcribed VERBATIM from this control's own failing print (LAW 81), down
     * from 14. (The seat-31 law-2 commit then re-pinned the drr 7.0.0 GetUnderlierLEIForCredit row
     * away — 12.) <b>RE-MEASURED at the seat-31 chain head {@code f2a4d5c0}</b> — 10 entries, two
     * rows having LEFT (both noted inline), transcribed VERBATIM from this control's own failing
     * print.
     */
    private static final List<String> KNOWN_RESIDUE_7 = List.of(
            // GetBasket.java (was fork=[2, 3, 2] golden=[3, 3, 2]) left this list at seat 30: laws
            // 6 + 7 TOGETHER (condArmMultiMetaElementDeref hunk 1 + multiEmptyElseArmToBuilder
            // hunk 2) healed it WHOLE in all four drr 7.x cells, so its third witnessed feature hop
            // (T1) now renders. Golden's tuple stays non-zero, so the file remains inside the union
            // domain and DOMAIN_DRR7 is UNMOVED at 3044.
            // UnderlierBasketIdentifier.java (was fork=[5, 0, 1] golden=[15, 0, 0]) left this list at
            // seat 31: law 4a (choiceOptionNavLadderDeepHop - the FER SET-seat option ladder walks the NESTED choice option tree
            // through ChoiceSwitchSupport.findChoiceOptionPath and derefs the META option hop into the bare output)
            // healed it WHOLE in all four drr 7.x cells, so golden's fifteen witnessed feature hops (T1) render and the fork's one
            // spurious hop (T3) is gone. Golden's T1 = 15 keeps the file inside the union domain:
            // DOMAIN_DRR7 UNMOVED at 3044.
            // the UnderlierProductIdentifier row (fork=[159, 3, 13] golden=[169, 3, 13]) LEFT this list at seat 32: law A.2 (wrapperItemReceiverBind)
            // healed it WHOLE in all four drr 7.x cells after this suite's pins were measured; transcribed from
            // the checkpoint-2 full-gensuite print (ckpt2-gensuite.log).
            // fork T3 2 -> 0 at seat 29 (law 9b, defaultSingleMixedJoinArgDeref — the mixed-join
            // getOrDefault deref reshapes this file's residual composition; still banded, measured
            // in the seat's chain — the m-law3 lane's FPMD delta proved law 3 is NOT the mover).
            // the Enrich_TransactionReportInstructionTestPackDefault row (fork=[16, 1, 0] golden=[16, 17, 1]) LEFT this list at seat 33:
            // law E.234 (rungs E.2 + E.3 + E.4) healed the file WHOLE in all four drr 7.x cells
            // (EnrichReportInstructionWholeSeatTest corpus_c1/c2); transcribed from the control print (E234-trip1.log).
            // the IndicatorOfTheUnderlyingIndexRule row (fork=[13, 1, 2] golden=[15, 1, 2]) LEFT this list at seat 32: law D.2
            // (extractBodyMultiDefaultTernary, on law D.1's left deref) healed it WHOLE in all four drr 7.x cells;
            // transcribed from this control's own print (D2-trip1.log).
            // GetUnderlierLEIForCredit.java (was fork=[30, 0, 4] golden=[31, 0, 4]) left this list at
            // seat 31: law 2 rung 2 (lambdaItemReceiverType - the ADD-terminal deref) healed it WHOLE
            // in all four drr 7.x cells (locked whole by ReceiverRenderTypingSeatTest corpus_c1 at 7.0.0).
            // Re-pinned from the control's own measured print (delta = the one row REMOVED, nothing else).
            // (The law-2 commit's own note called this "the drr 5.61.0 row" - there is no 5.61.0 sibling:
            // the file exists only under drr 7.0-7.3; corrected at the review of #603, MF-4.)
            // the UnderlyingIndexIndicatorRule row (fork=[22, 2, 2] golden=[23, 2, 2]) LEFT this list at seat 32: law D.1 (defaultJoinDerefAtCollapsedLeft)
            // healed it WHOLE in all four drr 7.x cells; transcribed from this control's own print (D1-trip1.log).
            // CustomBasketCodeRule.java (was fork=[5, 0, 3] golden=[13, 0, 0]) left this list at seat 31:
            // law 4b (choiceSwitchLambdaOptionGetter - the in-lambda CHOICE switch lowers its case guards to option-getter
            // null-tests with MAPPER-typed case locals, the live-bound naming rung hoisted) healed it WHOLE in drr 7.0.0
            // ONLY (7.1-7.3 IMPROVED 50 -> 49 lines and stay banded on a DIFFERENT mechanism - the in-lambda nested
            // then-chain hoist, S32's), so golden's thirteen witnessed hops (T1) render and the three instanceof-cast hops
            // (T3) are gone. Golden's T1 = 13 keeps the file inside the union domain.
            // the Price row (fork=[83, 1, 11] golden=[90, 1, 11]) LEFT this list: law C.2 (heteroMetaDefaultJoinDeref + iteArmMultiDefaultTernary)
            // took the rung-1 default join, the last residue after C.1 - Price is WHOLE in all four drr 7.x cells;
            // transcribed from this control's own print (C2-trip1.log), a pure row removal.
            // LAW 81 re-pin (seat 33, law C.1): fork=[76, 1, 13] -> fork=[83, 1, 11] - the statement ladder moved this scan's fork side toward golden; the file stays BANDED on C.2's default join; from C1-trip1.log.
            // FirstExerciseDateRule left this list at seat 29 (law 9, enumSwitchCaseBodyWiden —
            // healed WHOLE, measured in the seat's chain).
            // (FixedRateRule left this residue when seat-28 law 11 healed it whole -
            //  re-pinned here, the cross-suite verify's catch)
            // the QuantityUnitOfMeasure row (fork=[29, 0, 7] golden=[28, 0, 7]) LEFT this list: law B.24 (defaultJoinHeteroMetaDerefBoth +
            // iteArmMetaCollapseDerefSinkChannel + three in-seat rungs) healed the file WHOLE in all four drr 7.x
            // cells - the band's last four files; transcribed from this control's own print (B24-trip1.log),
            // a pure row removal (was == expected minus it).
            // LAW 81 re-pin (seat 33, law C.2): fork=[27, 0, 8] -> fork=[29, 0, 7] - R3a's join deref moved this scan's fork side (LAW 80 IMPROVED-not-whole, planned; QUOM closes at B.24); from C2-trip1.log.
            // the TotalNotionalQuantity row (fork=[65, 0, 0] golden=[72, 0, 0]) LEFT this list: law D.3 (fnDeepCondBaseConfinedArmChainAdmit,
            // seven rungs) healed the file WHOLE in all four drr 7.x cells; transcribed from this control's own
            // print (D3-trip1.log), a pure row removal (was == expected minus it).
            );
            // the UnderlyingAssetTradingPlatformIdentifierLeg1/Leg2 rows LEFT this list at seat 32: law A.1
            // (choiceOptionProjectionTypeId) healed both WHOLE in all four drr 7.x cells; transcribed from this
            // control's own print (A1-trip1.log).

    /**
     * control2 — LAW 77, and for THIS law it is load-bearing rather than decoration. Rung A must move
     * BOTH routes; rung B must move the OFF route ONLY, because the IR route already renders the witness
     * ({@code IRJavaLeafEmitter.emitFieldAccess} reads {@code ws.getInferredType}). After the seat the
     * two routes must CONVERGE on this file, and both must equal golden — which is exactly what
     * {@code corpus_c1} + this assert together pin. If the ON route MOVES on lines 67/70, rung B leaked
     * into a path the IR route was already answering.
     */
    @Test
    @EnabledIf("cellAAndIrProviderAvailable")
    void corpus_control2_irRouteMatchesLegacyForCarrier() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 legacy-route generation did not run");
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", CELL_A_ROOT), new ArrayList<>());
        assertEquals(drrAOutput.get(QUANTITY_FREQUENCY), irOut.get(QUANTITY_FREQUENCY),
                "route divergence: " + QUANTITY_FREQUENCY
                + " — rung A moves both routes and rung B moves OFF only, so the two must CONVERGE here");
        assertEquals(normalize(Files.readString(GOLDEN_A.resolve(QUANTITY_FREQUENCY))),
                normalize(irOut.get(QUANTITY_FREQUENCY)),
                "the IR route must also byte-match golden for " + QUANTITY_FREQUENCY);
    }

    /**
     * control3 — LAW 79 on drr 5.61.0: a cell with NO law-4 carrier. Rung B changes a walk consulted by
     * every feature-call hop in the generator, and this cell is where an unsized over-fire would surface.
     */
    @Test
    @EnabledIf("cellBAvailable")
    void corpus_control3_forkDrr561WholeCellWitnessSitesEqualGoldenFileByFile() throws IOException {
        assertNotNull(drrBOutput, "drr 5.61.0 generation did not run");
        assertEquals(List.of(), drrBGenErrors, "drr 5.61.0 reported a generation error — the scan is incomplete");
        assertUnionEqual(scan(drrBOutput), scan(readGoldenTree(GOLDEN_B)), drrBOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_561, DOMAIN_DRR561);
    }

    /** MEASURED AT THE CHAIN (LAW 82) — leave empty until the GREEN read fills it. */
    // LAW 81 re-pin (v3.1 flip seat 31, law 1): the asic CustomBasketCodeIdentifier +
    // PlatformIdentifier rows LEFT this list - both files healed WHOLE (byte-identical
    // to golden, locked by IteElseArmRecoveredMetaDerefSeatTest corpus_c1/c2). The
    // heal-tripwire fired exactly as prescribed and the list is re-pinned from the
    // control's own measured print (delta = the two rows REMOVED, nothing else).
    // the NotionalAmountLeg1/2 rows LEFT this list at seat 32: law C.3 (ruleThenArmLadderNestedTreeAdmit) healed
    // NotionalAmountLeg1/2 + NotionalCurrencyLeg2 WHOLE in drr 5.61.0; transcribed from this control's own print (C3-trip1*.log).
    // LAW 81 re-pin (v3.1 flip seat 33, law F.A): the cftc NotionalCurrencyLeg1Rule + jfsa
    // NotionalCurrencyOfLeg1Rule rows LEFT this list - both files healed WHOLE by facet
    // blockArmWrapperHopDeref (byte-identical to golden, locked by BlockArmWrapperHopDerefSeatTest
    // corpus_c1/c2); the list is EMPTY, transcribed from this control's own print (FA-trip1.log).
    private static final List<String> KNOWN_RESIDUE_561 = List.of();
            // LAW 81 (seat 33, law F.B): the NotionalLeg2Rule row fork=[18, 0, 6] golden=[18, 0, 5] LEFT - the file is WHOLE (FB-trip1.log print)
    // LAW 81 re-pin (v3.1 flip seat 31, law 1b): the mas PlatformIdentifierRule row LEFT
    // this list - the file healed WHOLE (byte-identical to golden, locked by
    // MapperFormRuleRootArmSeatTest corpus_c1). Re-pinned from the control's own measured
    // print (delta = the one row REMOVED, nothing else).

    /** MEASURED AT THE CHAIN — the drr 7.0.0 union domain. */
    private static final int DOMAIN_DRR7 = 3044;

    /** MEASURED AT THE CHAIN — the drr 5.61.0 union domain. */
    private static final int DOMAIN_DRR561 = 1902;

    /**
     * (T1, T2, T3) = witnessed feature hops, BARE feature hops, ctor-setter round trips. T1/T2 are the
     * rung-B pair: every {@code >map("} / {@code >mapC("} is necessarily preceded by a witness, and every
     * {@code .map("} / {@code .mapC("} is necessarily not. T3 is rung A's shape. All three are counted on
     * {@code codeOnly()}, so a getter name inside a string literal never contributes.
     */
    private static Map<String, int[]> scan(Map<String, String> tree) {
        Map<String, int[]> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : tree.entrySet()) {
            String code = codeOnly(e.getValue());
            int t1 = count(code, ">map(") + count(code, ">mapC(");
            int t2 = count(code, ".map(") + count(code, ".mapC(");
            int t3 = count(code, "(MapperS.of(") - count(code, "(MapperS.of(MapperS.of(");
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

    private static void assertUnionEqual(Map<String, int[]> a, Map<String, int[]> b,
            java.util.Set<String> emittedA, String aName, String bName, List<String> knownResidue,
            int expectedDomain) {
        // Scoped to the files this harness emits (the law-6/8/5/3 correction class).
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
                "the union domain must equal the oracle's token-bearing files (" + expectedDomain + ")");
    }

    // =========================================================================
    // Harness (the seat-26/27 suite shape verbatim)
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
                    new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", CELL_A_ROOT), errs);
            drrAGenErrors = errs;
        }
        if (cellBAvailable()) {
            List<String> errs = new ArrayList<>();
            drrBOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "5.61.0", CELL_B_ROOT), errs);
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
                + path + " — seat 28 law 4: a then-wrapped default ctor value takes the MapperS.of"
                + " identity round-trip, and a `then default` alias receiver keeps its <T> witness.");
    }

    private static void assertContains(String out, String needle) {
        assertTrue(out.contains(needle), "expected <" + needle + "> in:\n" + out);
    }

    /** Strip line and block comments plus string literals so a javadoc or mark never counts as code. */
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
            RModel main = AstBuilder.buildFromString(MODEL, "seat28d.rosetta");
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
            fixtureOut = render(m -> "census.seat28d".equals(m.namespace()));
        }
        return fixtureOut;
    }

    private static String fn(String fileName) throws IOException {
        return lookup(fixture(), "functions/" + fileName);
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
            throw new AssertionError("[ThenWrappedDefaultSeatTest] builtins parse"
                    + " failures: " + String.join("; ", failures));
        }
        return models;
    }
}
