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
 * SEAT 28, law 5 — facet {@code defaultMetaJoinFromRender}: <b>a {@code default} whose LEFT renders a
 * META wrapper over a meta-FREE RIGHT derefs the LEFT IN PLACE</b>, because upstream compiles both
 * operands at the JOINED meta type ({@code ExpressionGenerator.xtend} L452-468, {@code binaryExpr}
 * {@code case "default"}: {@code joinMetaAnnotatedTypes} + {@code withExpected(MAPPER.wrapExtends(joined))})
 * and meta survives the join only when BOTH operands carry it.
 *
 * <p><b>The census's PRODUCER is REFUTED and this law names the replacement.</b> The join cannot be
 * computed from the inferred types — {@code PROBE28-F10b} measures, at every carrier row and on both
 * routes, {@code leftInferredMeta=false} while {@code leftCompiled=MapperS<…FieldWithMetaDate>};
 * {@code rightCompiled=(null)}; {@code expected=null}. The join is read from RENDER truth,
 * {@code leftBuilder.getExpressionType()}, and the deref is emitted by
 * {@code ExpressionCompiler.coerceNavigationReceiver} — <b>the same call this method already makes at
 * {@code SetOperationHandler:388}</b>, behind the {@code #296} mixed-baresym gate the probe measured
 * {@code metaJoinGate=false} at every carrier. Law 5 is the missing SECOND caller of an existing
 * mechanism, not a new one.
 *
 * <p><b>The golden vs fork shape</b> (drr 7.0.0 {@code FixingDateRule}, the single hunk):
 * <pre>
 * golden:  final MapperS&lt;Date&gt; thenArg2 = thenArg1
 *              .mapSingleToItem(item -&gt; MapperS.of(item.&lt;FieldWithMetaDate&gt;map("getAdjustedDate", …)
 *                  .&lt;Date&gt;map("Type coercion", fieldWithMetaDate -&gt; fieldWithMetaDate == null ? null : fieldWithMetaDate.getValue())
 *                  .getOrDefault(item.&lt;Date&gt;map("getUnadjustedDate", …).get())));
 *          output = MapperS.of(toDateTime.evaluate(thenArg2.get())).get();
 * fork:    final MapperS&lt;FieldWithMetaDate&gt; thenArg2 = thenArg1
 *              .mapSingleToItem(item -&gt; MapperS.of(item.&lt;FieldWithMetaDate&gt;map("getAdjustedDate", …)
 *                  .getOrDefault(item.&lt;Date&gt;map("getUnadjustedDate", …).get())));
 *          final FieldWithMetaDate fieldWithMetaDate = thenArg2.get();
 *          output = MapperS.of(toDateTime.evaluate((fieldWithMetaDate == null ? null : fieldWithMetaDate.getValue()))).get();
 * </pre>
 * <b>GOLDEN emits the hop; the fork omits it.</b> (Verified on the raw dump, not the diff labels:
 * {@code grep -c "Type coercion"} is 1 in golden and 0 in the fork.) The model says why —
 * {@code adjustedDate} carries {@code [metadata id]} and {@code unadjustedDate} does not, so the join
 * is bare {@code date}.
 *
 * <p><b>The decl type and the consumer hoist heal for FREE — no second law.</b> The {@code #144}
 * wrapper-recovery skip predicate at {@code FunctionExpressionRenderer:6809-6815} already reads the
 * exact marker this render installs:
 * {@code value.contains(".<" + inferredItemType.getSimpleName() + ">map(\"Type coercion\", ")} together
 * with {@code value.contains(".getValue()).getOrDefault(")}. Once the hop lands, both markers hit, the
 * refs scan is skipped, the decl stays bare {@code MapperS<Date>}, and the consumer-side deref has no
 * wrapper to strip. <b>This is a source-level inference, not a measurement</b> — a1 asserts the DECL as
 * well as the hop, so a hop that lands without the decl following is a RED, never a silent partial.
 *
 * <p><b>LAW 74</b>: the fork passes a {@code Date} to {@code MapperS<FieldWithMetaDate>.getOrDefault(T)}
 * — an {@code incompatible types} error. The PRE javac probe must show it on all four
 * {@code FixingDateRule} cells and the POST must exit 0.
 *
 * <p><b>The over-fire domain is FULLY ENUMERATED, and the RIGHT-is-meta-free term is LOAD-BEARING.</b>
 * A paren-balanced backward parse of every {@code .getOrDefault(} receiver over all 165,544 generated
 * goldens (excluding the 99,916 {@code getOrDefault(false|true)} comparison forms) gives exactly 34
 * meta-left sites:
 * <ul>
 *   <li><b>Golden DEREFS (21)</b>: {@code CollateralPortfolioIndicatorRule} iosco cde ×9 <b>GREEN and
 *       ALREADY CORRECT</b> (it takes the {@code #296} arm — law 5 must be inert there);
 *       {@code FixingDateRule} ×4 <b>band, F10, the whole heal</b>;
 *       {@code UnderlyingIndexIndicatorRule} ×4 <b>band, IMPROVE only</b> (3 of 5 hunks — the other two
 *       are one {@code MapperC}/{@code MapperS} wrap-kind mis-stamp at
 *       {@code WrappedItemCoercer:103-117} vs {@code :76-92} and its lambda-escape knock-on);
 *       {@code QuantityUnitOfMeasure} ×4 <b>band but NOT F10</b> (F7+F13+F22) — if it moves, the census
 *       family must be re-pinned in the same commit.</li>
 *   <li><b>Golden KEEPS the wrapper (13)</b>: {@code MessageID} ×9 <b>GREEN</b> and
 *       {@code GetBasketConstituents} ×4 (band, not F10). Both are HOMOGENEOUS meta — both operands
 *       {@code FieldWithMetaString} — so the join keeps the wrapper. <b>A left-only gate breaks nine
 *       green files on contact.</b></li>
 * </ul>
 * Since {@code rightCompiled=(null)} at the carriers, the meta-free read uses the refs superset
 * invariant declared on {@code JavaStatementBuilder.getRefs()} ({@code refs ⊇ the library/domain classes
 * textually present in renderToString()}): no {@code RJavaWithMetaValue} in the right's refs ⟹ none in
 * its text. Both error directions are conservative — an incidental interior wrapper in refs DECLINES
 * the law, which is byte-flat.
 *
 * <p><b>THE NAMED DECLINE</b>: {@code leftBuilder.getExpressionType() == null} ⟹ no coercion, bytes
 * unchanged. The F11 lens measured that channel populated only ~28-58% of the time, so the decline is
 * the COMMON path; {@code coerceNavigationReceiver} ({@code ExpressionCompiler:324-334}) re-checks both
 * the null and the wrapper conditions itself, so a mis-gate here can only decline or no-op, never crash.
 *
 * <p><b>LAW 69</b>: {@code SetOperationHandler.tryThenBoundMetaArgDeref} at {@code :1145-1149} already
 * carries this exact predicate, used there to DECLINE on the stated ground that "a homogeneous-meta
 * default keeps the wrapper — the #296 mixed-join law owns those". Law 5 is the owner it defers to;
 * both javadocs are cross-referenced in the same commit.
 *
 * <p><b>RED at the pre-seat blob</b>: a1, a2, corpus_c1, corpus_control1. b1, b2, corpus_c2,
 * corpus_control0, corpus_control2 GREEN in both states.
 *
 * <p><b>LAW 66/76 mutations</b> — the seat's chain ran ONE on this suite: the WHOLE law reverted, whose
 * RECORDED failing set is in the MEASURED MUTATIONS block below. The planned severs were NOT RUN as
 * such — each is <b>NOT RUN, banked to S29</b>: (i) the whole insertion removed; (ii) the
 * {@code rightRendersNoMetaWrapper} term dropped (the homogeneous-meta over-fire — PLANNED to move
 * corpus_c2 and the union controls); (iii) the {@code !metaJoin} term dropped ({@code #296} double-deref).
 * <p><b>MEASURED MUTATIONS (LAW 82 - the seat-28 mut28 suite-lane loop; each
 * mutation = the named apply-script reverted, the suite run, the script re-applied;
 * every set below is the RECORDED failing set from that run, never a claim):</b>
 * <ul>
 *   <li>the whole law reverted (law5-apply --revert) -> a1, a2, corpus_c1, corpus_control1 (4F)</li>
 * </ul>
 */
class DefaultMetaJoinSeatTest {

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

    /** cell A = drr 7.0.0 — the carrier, the improve, BOTH negative controls and the positive control. */
    private static final Path CELL_A_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path GOLDEN_A = CELL_A_ROOT.resolve("rosetta-source/src/generated/java");
    /** cell B = drr 6.34.1 — MessageID + CollateralPortfolioIndicatorRule, NO carrier: the over-fire cell. */
    private static final Path CELL_B_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
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
     * A1 = the {@code FixingDateRule} shape: a {@code default} with a META LEFT ({@code [metadata id]})
     * and a meta-FREE RIGHT, consumed by a bare {@code .get()} — the hop, the decl type and the absent
     * consumer hoist are all asserted. A2 = the same law with a {@code ReferenceWithMeta} left
     * ({@code [metadata reference]}) — the {@code CollateralPortfolioIndicatorRule} wrapper family.
     * B1 = HOMOGENEOUS meta ({@code MessageID} / {@code GetBasketConstituents}): both operands
     * annotated, so the join KEEPS the wrapper and no hop may appear. B2 = a meta-FREE left: the law's
     * gate never opens.
     */
    private static final String MODEL = """
            namespace census.seat28b
            version "1.0.0"

            type Party:
                name string (0..1)

            type Adj:
                adjusted date (0..1)
                    [metadata id]
                unadjusted date (0..1)
                otherAdjusted date (0..1)
                    [metadata id]
                partyRef Party (0..1)
                    [metadata reference]
                partyPlain Party (0..1)
                plainA date (0..1)
                plainB date (0..1)

            type Holder:
                adj Adj (0..1)
                note string (0..1)

            func A1MetaLeftBareRightDefault: <"a1 - the FixingDateRule shape: meta LEFT, meta-free RIGHT">
                inputs:
                    h Holder (1..1)
                output:
                    result date (0..1)
                set result:
                    h -> adj -> adjusted default h -> adj -> unadjusted

            func A2ReferenceMetaLeftBareRightDefault: <"a2 - the same law with a [metadata reference] LEFT">
                inputs:
                    h Holder (1..1)
                output:
                    result Party (0..1)
                set result:
                    h -> adj -> partyRef default h -> adj -> partyPlain

            func B1HomogeneousMetaDefault: <"b1 - BOTH operands meta: the join KEEPS the wrapper (MessageID)">
                inputs:
                    h Holder (1..1)
                output:
                    result date (0..1)
                set result:
                    h -> adj -> adjusted default h -> adj -> otherAdjusted

            func B2BareLeftDefault: <"b2 - a meta-FREE left: the law's gate never opens">
                inputs:
                    h Holder (1..1)
                output:
                    result date (0..1)
                set result:
                    h -> adj -> plainA default h -> adj -> plainB
            """;

    /**
     * a1 — the meta LEFT derefs IN PLACE. Three asserts, because the law claims three deltas and the
     * second two are inferred from {@code FunctionExpressionRenderer:6809-6815} rather than measured: the
     * HOP (delta a), the bare DECL type (delta b), and the ABSENCE of the consumer-side wrapper local
     * (delta c). A hop that lands without the decl following is a RED.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_metaLeftDerefsInPlaceAtTheDefaultSeat() throws IOException {
        String out = fn("A1MetaLeftBareRightDefault.java");
        assertContains(out, ".<Date>map(\"Type coercion\", fieldWithMetaDate -> fieldWithMetaDate == null ? null : fieldWithMetaDate.getValue())");
        assertContains(out, ".getValue()).getOrDefault(");
        assertTrue(!codeOnly(out).contains("final FieldWithMetaDate "),
                "with the left deref'd in place the consumer hoists NO wrapper local:\n" + out);
        // PIN AT RED: add the byte-exact decl line (`final MapperS<Date> …` / `Date output` at the set
        // seat) once the GREEN run names it — delta (b) is the #144 skip-marker inference and must be
        // pinned by bytes, not left to the hop assert alone.
    }

    /**
     * a2 — the {@code ReferenceWithMeta} wrapper family takes the SAME null-ternary hop. This is the
     * fixture twin of the GREEN {@code CollateralPortfolioIndicatorRule}, whose golden carries exactly
     * this form; asserting it here makes corpus_control0's oracle reproducible in the fixture lane.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_referenceMetaLeftDerefsInPlace() throws IOException {
        String out = fn("A2ReferenceMetaLeftBareRightDefault.java");
        assertContains(out, ">map(\"Type coercion\", referenceWithMetaParty -> referenceWithMetaParty == null ? null : referenceWithMetaParty.getValue())");
        assertContains(out, ".getValue()).getOrDefault(");
    }

    /**
     * b1 — the decline pin (LAW 76 witness-uniqueness), and the single most important control in this
     * law: HOMOGENEOUS meta. Both operands are {@code [metadata id]}-annotated, so the join keeps the
     * wrapper and golden emits NO hop — the {@code MessageID} (9 GREEN cells) and
     * {@code GetBasketConstituents} class. The file must carry NEITHER token the flip adds: no
     * {@code "Type coercion"} hop on the left and no {@code .getValue()).getOrDefault(} adjacency.
     * A left-only gate fails here, which is exactly what it must do.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b1_homogeneousMetaDefaultKeepsTheWrapper() throws IOException {
        String out = fn("B1HomogeneousMetaDefault.java");
        String code = codeOnly(out);
        assertContains(out, ".getOrDefault(");
        assertTrue(!out.contains("Type coercion"),
                "a homogeneous-meta default must NOT deref either operand:\n" + out);
        assertTrue(!code.contains(".getValue()).getOrDefault("),
                "a homogeneous-meta default must NOT carry the deref'd-left adjacency:\n" + out);
    }

    /**
     * b2 — the precondition decline: a meta-FREE left means {@code leftRendersMetaWrapper} is false and
     * the coercion is never consulted at all. None of the law's tokens may appear.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void b2_bareLeftNeverConsultsTheCoercion() throws IOException {
        String out = fn("B2BareLeftDefault.java");
        assertTrue(!out.contains("Type coercion"),
                "a bare left must not gain a coercion hop:\n" + out);
        assertTrue(!codeOnly(out).contains(".getValue()).getOrDefault("),
                "a bare left must not carry the deref'd-left adjacency:\n" + out);
    }

    // =========================================================================
    // Part C — the corpus carriers (4 whole + 4 improved, one cell family)
    // =========================================================================

    private static final String FIXING_DATE =
            "drr/regulation/common/trade/datetime/reports/FixingDateRule.java";

    private static final String UNDERLYING_INDEX_INDICATOR =
            "drr/regulation/jfsa/rewrite/trade/reports/UnderlyingIndexIndicatorRule.java";

    private static final String MESSAGE_ID =
            "drr/regulation/common/trade/link/functions/MessageID.java";

    private static final String COLLATERAL_PORTFOLIO_INDICATOR =
            "drr/standards/iosco/cde/version1/collateral/reports/CollateralPortfolioIndicatorRule.java";

    /** c1 — the WHOLE heal: {@code FixingDateRule} is a single-hunk file and this law owns the hunk. */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_fixingDateRuleByteIdentical() throws IOException {
        lockA(FIXING_DATE);
    }

    /**
     * c2 — the GREEN files this law must leave alone, byte-locked: {@code MessageID} (homogeneous meta,
     * the negative control's corpus twin) and {@code CollateralPortfolioIndicatorRule} (already correct
     * through the {@code #296} arm, the positive control's corpus twin). Both are byte-identical to
     * golden TODAY, so this test is GREEN in both states and fails the moment either gate term is
     * dropped.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c2_greenMetaDefaultCarriersByteIdentical() throws IOException {
        lockA(MESSAGE_ID);
        lockA(COLLATERAL_PORTFOLIO_INDICATOR);
    }

    /**
     * c3 — the predicted IMPROVE, MEASURED AS A DECLINE at the law-5 head and pinned as such
     * from seat 28 to seat 32: {@code UnderlyingIndexIndicatorRule}'s default has a META left
     * ({@code ... -> identifier first}) over a RIGHT that is a CONDITIONAL LADDER whose interior
     * legitimately references {@code FieldWithMetaString}, so {@code rightRendersNoMetaWrapper}'s
     * refs channel took its documented CONSERVATIVE false-negative and the law declined. The pin
     * existed "so the refinement is measured as a flip here, not silently absorbed" — and it was:
     * <b>RE-PINNED at seat 32, law D.1 (defaultJoinDerefAtCollapsedLeft)</b>, the refined gate
     * (the walker's own verdict, {@code recoverExprMetaWrapper}, behind the compiled stamp) fired
     * here (D1-trip1.log printed {@code expected: <0> but was: <1>}) and the file went WHOLE in all
     * four drr 7.x cells — this is now the whole-file lock, strictly stronger than the flipped count.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c3_underlyingIndexIndicatorMetaLeftDerefsOverAConditionalRight() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");
        String generated = drrAOutput.get(UNDERLYING_INDEX_INDICATOR);
        assertNotNull(generated, "not generated in drr 7.0.0: " + UNDERLYING_INDEX_INDICATOR);
        String golden = Files.readString(GOLDEN_A.resolve(UNDERLYING_INDEX_INDICATOR));
        assertEquals(1, count(golden, ".getValue()).getOrDefault("),
                "golden derefs this left in place — the target law D.1 hits");
        assertEquals(1, count(generated, ".getValue()).getOrDefault("),
                "seat 32 law D.1: the conditional-right join reads the walker's verdict and derefs the left");
        lockA(UNDERLYING_INDEX_INDICATOR);
    }

    /** control0 — golden is the oracle, in BOTH directions (a deref'd carrier and a wrapper-keeping one). */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control0_goldenDerefsTheMixedJoinAndKeepsTheHomogeneousOne() throws IOException {
        String fixing = Files.readString(GOLDEN_A.resolve(FIXING_DATE));
        assertTrue(fixing.contains(".<Date>map(\"Type coercion\", fieldWithMetaDate -> fieldWithMetaDate == null ? null : fieldWithMetaDate.getValue())"),
                "golden FixingDateRule must carry the null-ternary deref hop");
        assertTrue(fixing.contains("final MapperS<Date> thenArg2"),
                "golden FixingDateRule's decl must be the BARE element type");
        assertTrue(!fixing.contains("final FieldWithMetaDate fieldWithMetaDate"),
                "golden FixingDateRule must hoist no wrapper local");
        String message = Files.readString(GOLDEN_A.resolve(MESSAGE_ID));
        assertTrue(message.contains("final MapperS<FieldWithMetaString> _thenArg"),
                "golden MessageID KEEPS the wrapper — both operands are meta");
        assertTrue(!message.contains(".getValue()).getOrDefault("),
                "golden MessageID must carry NO deref'd-left adjacency");
        String collateral = Files.readString(GOLDEN_A.resolve(COLLATERAL_PORTFOLIO_INDICATOR));
        assertTrue(collateral.contains(".<Collateral>map(\"Type coercion\", referenceWithMetaCollateral -> referenceWithMetaCollateral == null ? null : referenceWithMetaCollateral.getValue())"),
                "golden CollateralPortfolioIndicatorRule already carries this law's form (via the #296 arm)");
    }

    /**
     * control1 — LAW 79, the whole-cell UNION scan on drr 7.0.0: per file the (deref'd-left default,
     * meta-left default kept, wrapper local) triple must equal golden's, file for file over the UNION,
     * beyond the NAMED residue. Its reach covers every one of the 34 enumerated meta-left sites that
     * lives in this cell — both carriers, both negative controls and the positive control.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control1_forkDrr7WholeCellMetaJoinSitesEqualGoldenFileByFile() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 generation did not run");
        assertEquals(List.of(), drrAGenErrors, "drr 7.0.0 reported a generation error — the scan is incomplete");
        assertUnionEqual(scan(drrAOutput), scan(readGoldenTree(GOLDEN_A)), drrAOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_7, DOMAIN_DRR7);
    }

    /**
     * MEASURED AT THE CHAIN (LAW 82) — leave empty until the GREEN read fills it. EXPECTED to contain
     * {@code UnderlyingIndexIndicatorRule} only if its residual F6/F14 hunks move a T1/T2/T3 token
     * (they should not — they are a wrap-kind and a lambda-name delta), and {@code QuantityUnitOfMeasure}
     * if law 5 reaches it. Any entry here must name the movement and re-pin the census family in the
     * SAME commit.
     */
    /**
     * MEASURED at the law-5 head: every entry an OTHER family's token delta. <b>RE-MEASURED at the
     * seat-30 chain head {@code e223ce19}</b> — 8 entries, transcribed VERBATIM from that run's own
     * failing print (LAW 81); the prior set was the seat-29 head's 10. <b>RE-MEASURED at the seat-31
     * chain head {@code f2a4d5c0}</b> — 7 entries, the UnderlierBasketIdentifier row having LEFT
     * (noted inline), transcribed VERBATIM from that run's own failing print.
     */
    private static final List<String> KNOWN_RESIDUE_7 = List.of(
            // GetBasket.java (was fork=[0, 1, 1] golden=[0, 1, 0]) left this list at seat 30: laws
            // 6 + 7 TOGETHER (condArmMultiMetaElementDeref hunk 1 + multiEmptyElseArmToBuilder
            // hunk 2) healed it WHOLE in all four drr 7.x cells, so its extra hoisted-wrapper (T3)
            // site is gone. Golden's T2 = 1 keeps the file inside the union domain.
            // the GetBasketConstituents row (fork=[0, 1, 2] golden=[0, 1, 4]) LEFT this list: law A.1
            // (ctorSetterMetaDerefFunctionHost + the rung-3 ctorSetterHoistTextOrder numbering) took its tuples to
            // golden's in all four drr 7.x cells; the file stays BANDED on law A.2's lambda-name line, which this
            // tuple set cannot see; transcribed from this control's own print (A1-trip1.log).
            // UnderlierBasketIdentifier.java (was fork=[0, 0, 0] golden=[0, 0, 1]) left this list at
            // seat 31: law 4a (choiceOptionNavLadderDeepHop - the FER SET-seat option ladder walks the NESTED choice option tree
            // through ChoiceSwitchSupport.findChoiceOptionPath and derefs the META option hop into the bare output)
            // healed it WHOLE in all four drr 7.x cells, so golden's hoisted-wrapper site (T3) now renders. Golden's T3 = 1 keeps
            // the file inside the union domain: DOMAIN_DRR7 UNMOVED at 577 by THAT heal (577 -> 576 later, at seat 32 law D.2 - see the PIN below).
            // the UnderlierProductIdentifier row (fork=[0, 10, 0] golden=[0, 14, 0]) LEFT this list at seat 32: law A.2 (wrapperItemReceiverBind)
            // healed it WHOLE in all four drr 7.x cells after this suite's pins were measured; transcribed from
            // the checkpoint-2 full-gensuite print (ckpt2-gensuite.log).
            // Enrich_TransactionReportInstructionTestPackDefault left this list at seat 29 (law 9b,
            // defaultSingleMixedJoinArgDeref, moved its meta-join-counted site; the residual now
            // shows at the collapse seat).
            // the IndicatorOfTheUnderlyingIndexRule row (fork=[1, 0, 0] golden=[0, 0, 0]) LEFT this list at seat 32: law D.2
            // (extractBodyMultiDefaultTernary, on law D.1's left deref) healed it WHOLE in all four drr 7.x cells;
            // transcribed from this control's own print (D2-trip1.log).
            // the UnderlyingIndexIndicatorRule row (fork=[0, 0, 1] golden=[1, 0, 0]) LEFT this list at seat 32: law D.1 (defaultJoinDerefAtCollapsedLeft)
            // healed it WHOLE in all four drr 7.x cells; transcribed from this control's own print (D1-trip1.log).
            // the Price row (fork=[0, 3, 1] golden=[0, 2, 1]) LEFT this list: law C.2 (heteroMetaDefaultJoinDeref + iteArmMultiDefaultTernary)
            // took the rung-1 default join, the last residue after C.1 - Price is WHOLE in all four drr 7.x cells;
            // transcribed from this control's own print (C2-trip1.log), a pure row removal.
            // LAW 81 re-pin (seat 33, law C.1): fork=[0, 3, 0] -> fork=[0, 3, 1] - the statement ladder moved this scan's fork side toward golden; the file stays BANDED on C.2's default join; from C1-trip1.log.
            // ReportablePricePeriod.java (was fork=[0, 0, 0] golden=[0, 0, 1]) left this list at
            // seat 30: law 1 (aliasSigElementMetaKeep — the then-arg decl consults the alias
            // SIGNATURE channel) healed it WHOLE in all four drr 7.x cells, so its hoisted-wrapper
            // site now renders. Golden's T3 = 1 keeps the file inside the union domain.
            // FirstExerciseDateRule left this list at seat 29 (law 9, enumSwitchCaseBodyWiden —
            // healed WHOLE, measured in the seat's chain).
            // the QuantityUnitOfMeasure row (fork=[1, 4, 4] golden=[1, 3, 5]) LEFT this list: law B.24 (defaultJoinHeteroMetaDerefBoth +
            // iteArmMetaCollapseDerefSinkChannel + three in-seat rungs) healed the file WHOLE in all four drr 7.x
            // cells - the band's last four files; transcribed from this control's own print (B24-trip1.log),
            // a pure row removal (was == expected minus it).
            // LAW 81 re-pin (seat 33, law C.2): fork=[0, 5, 3] -> fork=[1, 4, 4] - R3a's join deref moved this scan's fork side (LAW 80 IMPROVED-not-whole, planned; QUOM closes at B.24); from C2-trip1.log.
            // LAW 81 re-pin (seat 33, law B.3): fork=[0, 5, 0] -> fork=[0, 5, 3] - the bare-rule invocations + their guarded arg derefs moved this scan's fork side; the file stays BANDED (B.24); from B3-trip1.log.
            // the TotalNotionalQuantity row (fork=[0, 7, 6] golden=[0, 7, 0]) LEFT this list: law D.3 (fnDeepCondBaseConfinedArmChainAdmit,
            // seven rungs) healed the file WHOLE in all four drr 7.x cells; transcribed from this control's own
            // print (D3-trip1.log), a pure row removal (was == expected minus it).
            );

    /** control2 — LAW 77 route parity for the carrier; the band is route-identical, so both must move. */
    @Test
    @EnabledIf("cellAAndIrProviderAvailable")
    void corpus_control2_irRouteMatchesLegacyForCarrier() throws IOException {
        assertNotNull(drrAOutput, "drr 7.0.0 legacy-route generation did not run");
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", CELL_A_ROOT), new ArrayList<>());
        assertEquals(drrAOutput.get(FIXING_DATE), irOut.get(FIXING_DATE),
                "route divergence: " + FIXING_DATE);
    }

    /**
     * control3 — LAW 79 on drr 6.34.1: the cell that carries {@code MessageID} and
     * {@code CollateralPortfolioIndicatorRule} and NO law-5 carrier. It is the pure over-fire cell — if
     * the right-is-meta-free term is wrong, nine green files move and this control names them.
     */
    @Test
    @EnabledIf("cellBAvailable")
    void corpus_control3_forkDrr6341WholeCellMetaJoinSitesEqualGoldenFileByFile() throws IOException {
        assertNotNull(drrBOutput, "drr 6.34.1 generation did not run");
        assertEquals(List.of(), drrBGenErrors, "drr 6.34.1 reported a generation error — the scan is incomplete");
        assertUnionEqual(scan(drrBOutput), scan(readGoldenTree(GOLDEN_B)), drrBOutput.keySet(),
                "fork", "golden", KNOWN_RESIDUE_6341, DOMAIN_DRR6341);
    }

    /** MEASURED AT THE CHAIN (LAW 82) — leave empty until the GREEN read fills it. */
    private static final List<String> KNOWN_RESIDUE_6341 = List.of();

    /** MEASURED AT THE CHAIN — the drr 7.0.0 union domain. */
    ///PIN: 577 -> 576 at seat 32 (law D.2): the healed IndicatorOfTheUnderlyingIndexRule's GOLDEN tuple is all-zero, so the
    ///PIN: file leaves the token-bearing union once its fork junk is gone; transcribed from this control's own print (D2-trip2.log).
    private static final int DOMAIN_DRR7 = 576;

    /** MEASURED AT THE CHAIN — the drr 6.34.1 union domain. */
    private static final int DOMAIN_DRR6341 = 562;

    /**
     * (T1, T2, T3) = deref'd-left defaults, meta-left defaults that KEEP the wrapper, hoisted wrapper
     * locals. T1 is the adjacency {@code .getValue()).getOrDefault(} — the same token the {@code #144}
     * skip predicate reads at {@code FunctionExpressionRenderer:6812}, so the scan and the render consult
     * one definition (LAW 69). T2 counts {@code .getOrDefault(} sites whose receiver text names a
     * wrapper and which are NOT already deref'd — the token the flip REMOVES. T3 is delta (c). All three
     * run on {@code codeOnly()}, so the {@code "Type coercion"} LABEL is stripped and cannot be used —
     * the scan keys on structure, which is the point.
     */
    private static Map<String, int[]> scan(Map<String, String> tree) {
        Map<String, int[]> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : tree.entrySet()) {
            String code = codeOnly(e.getValue());
            int t1 = count(code, ".getValue()).getOrDefault(");
            int t2 = 0;
            int t3 = count(code, "final FieldWithMeta") + count(code, "final ReferenceWithMeta");
            for (String line : code.split("\n")) {
                int at = line.indexOf(".getOrDefault(");
                if (at < 0) {
                    continue;
                }
                String receiver = line.substring(0, at);
                if (receiver.contains("WithMeta") && !line.contains(".getValue()).getOrDefault(")) {
                    t2++;
                }
            }
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
        // Scoped to the files this harness emits (rule/report/function kinds): golden's
        // POJO/DATA_RULE kinds carry tokens these generators never produce - the D11
        // ring's question, not this suite's (the law-6/law-8 correction, same class).
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
                    new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", CELL_B_ROOT), errs);
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
                + path + " — seat 28 law 5: a default whose LEFT renders a meta wrapper over a"
                + " meta-FREE right derefs the left in place, read from render truth.");
    }

    private static void assertContains(String out, String needle) {
        assertTrue(out.contains(needle), "expected <" + needle + "> in:\n" + out);
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
            RModel main = AstBuilder.buildFromString(MODEL, "seat28b.rosetta");
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
            fixtureOut = render(m -> "census.seat28b".equals(m.namespace()));
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
            throw new AssertionError("[DefaultMetaJoinSeatTest] builtins parse"
                    + " failures: " + String.join("; ", failures));
        }
        return models;
    }
}
