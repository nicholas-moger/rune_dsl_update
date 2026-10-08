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
 * PR #613, C2d retirement family 6 {@code meta-wrapper-recovery} — every consumer that had to know
 * which METADATA WRAPPER a then-level, a block lambda or a {@code default} operand carried moves off
 * the compiled value's emitted-import set ({@code JavaExpression.getRefs()}) and off the rendered
 * {@code Type coercion} / {@code ofNull} / {@code .getMulti()} markers, and onto typed facts.
 *
 * <p><b>The family, verbatim from the triage</b>
 * ({@code scripts/ci/evidence-api-triage.tsv}, family {@code meta-wrapper-recovery}): EIGHTEEN ledger
 * rows / eighteen occurrences over four files, all verdict RETIRE-AFTER-CENSUS — the shared scan
 * {@code NavigationHandler.uniqueMetaWrapperForValueType} and its five callers, the
 * {@code SetOperationHandler} then-base refs loop, the {@code FunctionExpressionRenderer}
 * switch-argument witness loop, the four block-arm render markers and the four ADD-rung render tails.
 *
 * <p><b>The retirement channels.</b>
 * <ul>
 *   <li><b>The two then-arg decl seats</b> ({@code CollectionHandler.tryDeepThenHoist}'s deep-then
 *       decl and {@code FunctionExpressionRenderer.renderThenExtractSet}'s then-arg decl) call ONE
 *       shared helper, {@code NavigationHandler.levelMetaWrapper} (LAW 69), which reads the level's
 *       own compiled stamp, declines an invocation level, joins a standalone {@code default} level's
 *       two faces, walks the level's own body with a fused-filter descent (scoped to the retired
 *       scan's reach by the s613a chain's e1 catch: a bare {@code then filter <Fn>} level answers
 *       prevRef's wrapper — rule 4a — and a collapse-op body declines to the #297 prevRef rung —
 *       rule 4b; {@code CollapseCarryPrevMetaElementSeatTest} is that scoping's decline lock and
 *       joined the mutation lanes' selection), and finally reads the block producer's recorded
 *       typed-empty element.</li>
 *   <li><b>The block-arm markers</b> read {@code ExpressionCompiler.lambdaJoinFactsFor(node)} — the
 *       {@code LambdaJoinFacts} the ladder / elseless / effective-else / nested / cascade / switch
 *       block compilers and the {@code default} seat record beside the text they render (LAW 69),
 *       keyed by the AST node they compiled.</li>
 *   <li><b>The in-arm deref marker</b> at the k&gt;0 ITE arm probe reads the coercion service's own
 *       deref ledger ({@code TypeCoercionService.derefOfValueSince}), written by
 *       {@code WrappedItemCoercer} beside the {@code .<X>map("Type coercion", …)} it emits.</li>
 *   <li><b>The ADD rungs</b> read their producers: {@code JavaExpression.MultiExtracted} (the
 *       {@code unwrapForAddAssignment} fall-through's own {@code .getMulti()} append), the flatten
 *       rung's AST twin, and the flatten / #347-rewrap rungs' own fired flags.</li>
 *   <li><b>Two rows were DEAD at the corpus</b> and are deleted rather than swapped: the #338
 *       arm-(iii) re-key (entered at 0 of 22,729 then-arg arrivals per walk) and the
 *       switch-argument witness loop (its Object gate entered at 0 of 207 switch-argument arrivals
 *       per D11 walk, 0 of 143 on the optimised walk).</li>
 * </ul>
 *
 * <p><b>The census</b> ({@code target/seat613-instruments/c2c-f6-verdicts.md}, local — six rounds of
 * env-gated probes over three walks each: the default-route D11 275/0F, the IR-route D11 275/0F and
 * the optimised route's own suite 12/0F; applied, never committed, reverted before any commit).
 * The two D11 routes are IDENTICAL probe multisets at every seat. The decl composite reproduces the
 * DECL at every arrival: S1 10,046 arrivals / 1,333 scan fires — the ARRIVALS classify SAME 9,354
 * + MASKED 663 + 25 {@code default} levels + 4 invocation levels, RISK 0 (round 2's print, summing
 * to the 10,046) — and S7 22,729 / 1,241 (SAME 22,127 + MASKED 602, OVERRIDE 0, RISK 0 — the
 * analyser's own print); round 6's post-swap receipt is a DIFFERENT measurement, reaching 9,352 of
 * the S1 arrivals past the seat's three gates and reading identity there. The block markers are
 * the producers' own verdicts at 702 fires per walk. The census's own finding: the
 * {@code return MapperS.<X>ofNull();} marker has FOUR more producers than the triage named — the
 * four switch block-lambda compilers, which now record too. The {@code default} seat's LEFT can
 * also arrive ALREADY deref'd from its own compile (fn:QuantityUnitOfMeasure k = 4, four marker
 * fires per D11 walk); that class is deliberately NOT captured — its deref renders at a PREVIOUS
 * rung, outside any window the seat can hold — and is byte-neutral (census § 3d: the consuming
 * composite returns null there either way; the code-quality review, SF-6).
 *
 * <p><b>The fixtures (Part A)</b> put one shape at each seat a REDUCED model reaches AND can move:
 * a1 the S1 deep-then decl, a2 the S2 elseless {@code ofNull} element, a4 the S5 ITE-arm probe, a8
 * the S7 fused-filter base, a9 and a10 the two S8 ADD rungs. Their lane sensitivity is NOT asserted
 * here — the MEASURED failing set per lane is transcribed into the seat's CHANGELOG entry from the
 * lane logs (LAW 82).
 *
 * <p><b>Four seats have no fixture</b>, each disclosed at the point it would have sat rather than
 * papered over with one that passes for another reason (LAW 72): S4's then-base {@code default}
 * recovery (see the note where a3 would be), the S6 marker's three fact kinds (the note where a5–a7
 * would be — with the MEASURED finding that the marker is corpus-inert after the swap) and the decl
 * composite's rule 5 (the note above corpus_c1_06). Each names the witness that took its place.
 *
 * <p><b>The corpus carriers (Part B)</b> — cell A is drr 6.34.1, the cell the sibling seat suites
 * lock: the two {@code PTRRServiceProviderRule} files (esma + fca — S2's {@code metaTypedOfNull}
 * carriers), {@code GetAllUnderlierProductIdentifier} (the S8 flatten rung),
 * {@code OtherPaymentPayerSchemeNameRule} (the S5 ITE-arm probe), {@code MessageID} (the S1
 * standalone-{@code default} level, one of the twenty-five the composite's rule 3 closes) and
 * {@code CountryOfCounterparty2Rule} (the composite's rule 5). Cell B is drr 7.0.0 — the two
 * charter-named carriers that do not exist in cell A (the spec review's SF-11, adjudicated by the
 * code-quality review's SF-8): {@code GetUnderlierLEIForCredit} (the S8 ADD-terminal rung's own
 * [P31-STAMP] carrier) and {@code QuantityScheduleRule} (16 of the 25 rule-3 arrivals, the
 * {@code ReferenceWithMeta…} wrapper kind rule 3 closes most often — the locked cell-A rule-3
 * carrier {@code MessageID} is a {@code FieldWithMetaString}). Whole-file byte locks (corpus_c1),
 * golden-token controls (control0 for cell A, control0b for cell B) and the LAW 77 IR-route parity
 * control (control2, skipping without the IR provider as its siblings do; control2 runs cell A
 * alone — cell B's route parity is witnessed corpus-wide by the D11 matrix's ROUTE ROW DIFF NONE
 * over every drr 7.x cell, and a second in-suite IR-route cell generation would duplicate that
 * gate at real cost).
 */
class MetaWrapperRecoverySeatTest {

    private static final Path REPO_ROOT =
            Path.of(System.getProperty("user.dir")).resolve("..").normalize();

    private static final List<Path> BUILTINS_SEARCH_ROOTS = List.of(
            REPO_ROOT.resolve("test-corpus/rune-dsl-builtins/rune-runtime/src/main/resources/model"),
            REPO_ROOT.resolve("rune-dsl/rune-runtime/src/main/resources/model"),
            REPO_ROOT.resolve("rune-runtime/src/main/resources/model"));

    static boolean builtinsAvailable() {
        return BUILTINS_SEARCH_ROOTS.stream().anyMatch(Files::isDirectory);
    }

    /** The carrier cell — drr 6.34.1 (the cell the sibling family-6 suites lock the same files in). */
    private static final Path CELL_A_ROOT = Path.of("../test-corpus/drr/drr-6.34.1");
    private static final Path GOLDEN_A = CELL_A_ROOT.resolve("rosetta-source/src/generated/java");

    static boolean cellAAvailable() {
        return Files.isDirectory(GOLDEN_A)
                && Files.isDirectory(CELL_A_ROOT.resolve("rosetta-source/src/main/rosetta"));
    }

    /** The second carrier cell — drr 7.0.0, home of the two charter carriers cell A lacks (SF-8). */
    private static final Path CELL_B_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path GOLDEN_B = CELL_B_ROOT.resolve("rosetta-source/src/generated/java");

    static boolean cellBAvailable() {
        return Files.isDirectory(GOLDEN_B)
                && Files.isDirectory(CELL_B_ROOT.resolve("rosetta-source/src/main/rosetta"));
    }

    static boolean irProviderOnClasspath() {
        try {
            Class.forName("com.regnosys.rosetta.generator.java.ir.IRGenerationProviderImpl");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    static boolean cellAAndIrProviderAvailable() {
        return cellAAvailable() && irProviderOnClasspath();
    }

    /** S2 — the #333 metaTypedOfNull carriers: the elseless block's ofNull element. */
    private static final String CARRIER_PTRR_ESMA =
            "drr/regulation/esma/emir/refit/trade/reports/PTRRServiceProviderRule.java";
    private static final String CARRIER_PTRR_FCA =
            "drr/regulation/fca/ukemir/refit/trade/reports/PTRRServiceProviderRule.java";
    /** S8 — the #374 ADD flatten rung. */
    private static final String CARRIER_FLATTEN_ADD =
            "drr/regulation/common/functions/GetAllUnderlierProductIdentifier.java";
    /** S5 — the k&gt;0 ITE arm probe (the #370 F-D carrier). */
    private static final String CARRIER_ITE_ARM =
            "drr/regulation/hkma/rewrite/trade/reports/OtherPaymentPayerSchemeNameRule.java";
    /** S1 — a standalone {@code default} level, closed by the composite's rule 3. */
    private static final String CARRIER_DEFAULT_LEVEL =
            "drr/regulation/common/trade/link/functions/MessageID.java";

    /**
     * The decl composite's rule 5 — the block producer's recorded typed-empty element. This file is
     * its corpus witness: it is the one lock in this suite that fails when
     * {@code ExpressionCompiler.lambdaJoinFactsFor} is stubbed to {@code null} (the seat's mutation
     * lane B — `target/seat613-instruments/logs/lane3.status`, the run at the e1-fix head's
     * content: the generator selection 368 run / 3F, of which this suite's 1F is this lock).
     */
    private static final String CARRIER_BLOCK_TYPED_EMPTY =
            "drr/regulation/asic/rewrite/trade/reports/CountryOfCounterparty2Rule.java";

    private static final List<String> CARRIERS = List.of(
            CARRIER_PTRR_ESMA, CARRIER_PTRR_FCA, CARRIER_FLATTEN_ADD, CARRIER_ITE_ARM,
            CARRIER_DEFAULT_LEVEL, CARRIER_BLOCK_TYPED_EMPTY);

    /**
     * S8 — the ADD-terminal rung's own charter carrier ([P31-STAMP]; cell B — this file does not
     * exist in drr 6.34.1): golden's trailing {@code .<String>map("Type coercion",
     * fieldWithMetaString -> fieldWithMetaString.getValue()).getMulti())}.
     */
    private static final String CARRIER_ADD_TERMINAL_B =
            "drr/regulation/common/trade/underlier/functions/GetUnderlierLEIForCredit.java";

    /**
     * S1 — sixteen of the twenty-five rule-3 arrivals (cell B): the standalone {@code default}
     * levels declaring {@code final MapperS<ReferenceWithMetaNonNegativeQuantitySchedule>
     * thenArg0..4} — the {@code ReferenceWithMeta…} wrapper kind, where the cell-A rule-3 lock
     * ({@code MessageID}) is a {@code FieldWithMetaString}.
     */
    private static final String CARRIER_RULE3_QS_B =
            "drr/base/trade/quantity/reports/QuantityScheduleRule.java";

    private static final List<String> CARRIERS_B = List.of(
            CARRIER_ADD_TERMINAL_B, CARRIER_RULE3_QS_B);

    // =========================================================================
    // Part A — the fixtures, one per seat a reduced model reaches
    // =========================================================================

    private static final String MODEL = """
            namespace census.seat613f6
            version "1.0.0"

            type UT:
                ccy string (0..1)
                    [metadata scheme]

            type QS:
                unit UT (0..1)

            type Leg:
                mark string (0..1)
                id string (0..1)
                    [metadata scheme]
                ids string (0..*)
                    [metadata scheme]
                plainId string (0..1)
                plainIds string (0..*)
                qs QS (0..1)

            type Root:
                leg Leg (0..1)
                legs Leg (0..*)

            func A1DeepThenFusedFilter: <"a1 - S1: the deep-route (in-lambda) then hoist whose level is a fused `<nav> filter [...]` over a meta leaf">
                inputs:
                    rt Root (0..1)
                output:
                    r string (0..*)
                set r:
                    rt -> legs
                        extract [ item -> ids filter [ item exists ] then distinct ]
                        then flatten

            func A2ElselessOfNull: <"a2 - S2: an elseless map body whose then-arm is a null-typed collapse of a meta leaf">
                inputs:
                    rt Root (0..1)
                output:
                    r string (0..*)
                set r:
                    rt -> legs
                        extract [ if item -> mark exists then item -> ids only-element ]


            reporting rule A4IteArmMeta from Root: <"a4 - S5: the k>0 rule ITE (the hkma OtherPaymentPayerSchemeName shape) whose then-arm terminates at a meta wrapper">
                filter leg exists
                then if leg -> mark exists
                    then extract leg -> id
                then extract item


            func A8FusedFilterBase: <"a8 - S7: a k == 0 fused-filter base over a meta leaf">
                inputs:
                    rt Root (0..1)
                output:
                    r string (0..*)
                set r:
                    rt -> leg -> ids filter [ item exists ]
                        then distinct

            func A9AddFlattenRung: <"a9 - S8: the ADD flatten rung over a list-of-lists whose element is a meta wrapper">
                inputs:
                    rt Root (0..1)
                output:
                    r string (0..*)
                add r:
                    rt -> legs
                        extract [ item -> ids ]
                        then flatten

            func A10AddTerminalRung: <"a10 - S8: the ADD-terminal rung - the last then-body chain ends META and derefs before .getMulti()">
                inputs:
                    rt Root (0..1)
                output:
                    r string (0..*)
                add r:
                    rt -> legs
                        then extract [ item -> id ]

            """;

    /** a1 — S1: the deep-then decl keeps the fused filter's meta element. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_deepThenFusedFilterLevelKeepsTheWrapper() throws IOException {
        String code = codeOnly(fn("A1DeepThenFusedFilter.java"));
        assertContains(code, "MapperC<FieldWithMetaString>");
    }

    /** a2 — S2: the elseless block's typed empty spells the WRAPPER the collapse erased. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_elselessBlockTypedEmptyKeepsTheWrapper() throws IOException {
        String code = codeOnly(fn("A2ElselessOfNull.java"));
        assertContains(code, "MapperS.<FieldWithMetaString>ofNull()");
        assertAbsent(code, "MapperS.<String>ofNull()");
    }

    /*
     * There is deliberately NO a3 for S4 (the then-base `default` join's RIGHT-wrapper recovery).
     * Two reductions were tried; both reach the seat — the then-base ternary renders — but in both
     * the RIGHT keeps its compiled stamp, so the recovery this row retired (reached only when the
     * right's stamp is LOST in the re-rooting) never fires and the fixture failed under no lane
     * (LAW 72). Its charter carrier, drr UniqueSwapIdentifierForValuation, is not in this corpus.
     * The swap's witnesses are the D11 ring and the existing then-base default suites in the seat's
     * run list (ThenSeatMultiDefaultTernarySeatTest, ExtractBodyMultiDefaultTernarySeatTest,
     * RuleGetOrDefaultMetaJoinTest), which lock the seat's ternary and its coerced arm on the corpus.
     */

    /** a4 — S5: the k&gt;0 ITE arm probe types the hoisted result at the arm's wrapper. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a4_iteArmProbeTypesTheResultAtTheWrapper() throws IOException {
        String code = codeOnly(rule("A4IteArmMetaRule.java"));
        assertContains(code, "final MapperS<FieldWithMetaString> ifThenElseResult");
        assertAbsent(code, "final MapperS<String> ifThenElseResult");
    }

    /*
     * There are deliberately NO a5 / a6 / a7 for the S6 block-arm marker's three fact kinds. All
     * three were written and all three RENDER the producer form they were meant to witness (a mixed
     * ladder's arms, a `default` level's LEFT deref, an elseless block's bare typed empty), but none
     * changes a byte under any lane, because the recovery the marker GATES declines on its own at
     * every one of those levels: the #331 walker has no case for a standalone `default` arm and
     * recovers nothing from a bare arm, so skipping it or not skipping it renders the same decl.
     * Keeping them would be keeping fixtures that pass for another reason (LAW 72), so they were
     * dropped.
     *
     * That inertness is MEASURED, not assumed, and it is the seat's own finding. The round-6
     * post-swap census (`target/seat613-instruments/c2c-f6-verdicts.md` § 3d) recomputes the retired
     * markers beside the recorded verdict at all 22,729 arrivals of both D11 routes: they differ at
     * FOUR (fn:QuantityUnitOfMeasure k = 4), and the byte oracle at this commit is 275/0F on both
     * routes — so this gate moves no bytes at this corpus. What the block facts DO move is the decl
     * composite's RULE 5: mutation lane B (that read stubbed to {@code null}) fails exactly one lock
     * in this suite, corpus_c1_06's CountryOfCounterparty2Rule, and two locks elsewhere
     * (`logs/lane3.status`, 368 run / 3F).
     */

    /** a8 — S7: the k == 0 fused-filter base declares the receiver's meta element. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a8_fusedFilterBaseDeclaresTheWrapper() throws IOException {
        String code = codeOnly(fn("A8FusedFilterBase.java"));
        assertContains(code, "MapperC<FieldWithMetaString> thenArg");
        assertAbsent(code, "MapperC<String> thenArg");
    }

    /**
     * a9 — S8: the ADD flatten rung appends its element-wise deref before the List expansion.
     * Asserted on the RAW render, not on {@link #codeOnly}: the coercion's {@code "Type coercion"}
     * label is a STRING LITERAL of the emitted code, which the comment/literal stripper removes.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a9_addFlattenRungAppendsTheElementwiseDeref() throws IOException {
        String code = fn("A9AddFlattenRung.java");
        assertContains(code, ".flattenList().<String>map(\"Type coercion\", "
                + "fieldWithMetaString -> fieldWithMetaString.getValue()).getMulti()");
        assertAbsent(code, ".flattenList().getMulti()");
    }

    /**
     * a10 — S8: the ADD-terminal rung appends its element-wise deref before {@code .getMulti()}.
     * Raw render for the same reason as a9.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a10_addTerminalRungAppendsTheElementwiseDeref() throws IOException {
        String code = fn("A10AddTerminalRung.java");
        assertContains(code, ".<String>map(\"Type coercion\", "
                + "fieldWithMetaString -> fieldWithMetaString.getValue()).getMulti()");
        assertAbsent(code, "leg.getId())).getMulti()");
    }

    /*
     * There is deliberately NO a11 for the decl composite's RULE 5 (the block producer's recorded
     * typed-empty element, read through lambdaJoinFactsFor). Three reductions were tried and none
     * reaches it: the identity-arm extract level (`then extract [if … then item]`) is decided by the
     * #362 pipe-meta rung before the composite runs; the MULTI form of the carrier's shape renders
     * the ternary, not a block lambda; and the SINGLE form types its own typed empty BARE, because a
     * reduced `default` arm compiles with a stamp the carrier's collapse-terminated arms do not have.
     * Each failed under NO lane, which is information-free, so all three were dropped rather than
     * kept for passing for another reason (LAW 72).
     *
     * The seat's witness is therefore the CORPUS carrier, and it is MEASURED, not asserted: under
     * mutation lane B ({@code ExpressionCompiler.lambdaJoinFactsFor} stubbed to {@code null}) the
     * lock below is the one failure this suite reports — corpus_c1_06's
     * drr/regulation/asic/rewrite/trade/reports/CountryOfCounterparty2Rule.java, whose decl falls
     * from `final MapperS<FieldWithMetaString> thenArg3` to the bare element
     * (`target/seat613-instruments/logs/lane3.status`, the run at the e1-fix head's content).
     * control0 pins the golden form it locks.
     */

    // =========================================================================
    // Part B — the corpus carriers (cell A drr 6.34.1; cell B drr 7.0.0, the SF-8 locks)
    // =========================================================================

    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_01_ptrrServiceProviderEsmaByteIdentical() throws IOException {
        lock(CARRIER_PTRR_ESMA);
    }

    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_02_ptrrServiceProviderFcaByteIdentical() throws IOException {
        lock(CARRIER_PTRR_FCA);
    }

    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_03_getAllUnderlierProductIdentifierByteIdentical() throws IOException {
        lock(CARRIER_FLATTEN_ADD);
    }

    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_04_otherPaymentPayerSchemeNameByteIdentical() throws IOException {
        lock(CARRIER_ITE_ARM);
    }

    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_05_messageIdByteIdentical() throws IOException {
        lock(CARRIER_DEFAULT_LEVEL);
    }

    @Test
    @EnabledIf("cellAAvailable")
    void corpus_c1_06_countryOfCounterparty2ByteIdentical() throws IOException {
        lock(CARRIER_BLOCK_TYPED_EMPTY);
    }

    @Test
    @EnabledIf("cellBAvailable")
    void corpus_c1_07_getUnderlierLeiForCreditByteIdentical() throws IOException {
        lockB(CARRIER_ADD_TERMINAL_B);
    }

    @Test
    @EnabledIf("cellBAvailable")
    void corpus_c1_08_quantityScheduleRuleByteIdentical() throws IOException {
        lockB(CARRIER_RULE3_QS_B);
    }

    /**
     * control0 — golden is the oracle: each carrier still carries the form this family's seat
     * decides. If one of these ever stops holding, the carrier stopped being this family's carrier
     * and every claim above is restated, not patched.
     */
    @Test
    @EnabledIf("cellAAvailable")
    void corpus_control0_goldensCarryTheFourDecidedForms() throws IOException {
        assertTrue(Files.readString(GOLDEN_A.resolve(CARRIER_PTRR_ESMA))
                        .contains("MapperS.<ReferenceWithMetaParty>ofNull()"),
                "S2: the golden's elseless block must spell its typed empty at the WRAPPER");
        assertTrue(Files.readString(GOLDEN_A.resolve(CARRIER_FLATTEN_ADD))
                        .contains(".flattenList().<ProductIdentifier>map(\"Type coercion\""),
                "S8: the golden's ADD flatten rung must carry the element-wise deref");
        assertTrue(Files.readString(GOLDEN_A.resolve(CARRIER_ITE_ARM))
                        .contains("final MapperS<ReferenceWithMetaParty> ifThenElseResult"),
                "S5: the golden's hoisted ITE result must be typed at the arm's wrapper");
        assertTrue(Files.readString(GOLDEN_A.resolve(CARRIER_DEFAULT_LEVEL))
                        .contains("final MapperS<FieldWithMetaString> _thenArg"),
                "S1: the golden's standalone `default` level must declare the joined wrapper");
        assertTrue(Files.readString(GOLDEN_A.resolve(CARRIER_BLOCK_TYPED_EMPTY))
                        .contains("final MapperS<FieldWithMetaString> thenArg3")
                && Files.readString(GOLDEN_A.resolve(CARRIER_BLOCK_TYPED_EMPTY))
                        .contains("return MapperS.<FieldWithMetaString>ofNull();"),
                "the composite's rule 5: the golden's block level must declare the element its own"
                + " typed-empty terminal spells");
    }

    /** control0b — the cell-B carriers still carry the forms their seats decide (SF-8). */
    @Test
    @EnabledIf("cellBAvailable")
    void corpus_control0b_cellBGoldensCarryTheDecidedForms() throws IOException {
        assertTrue(Files.readString(GOLDEN_B.resolve(CARRIER_ADD_TERMINAL_B))
                        .contains(".<String>map(\"Type coercion\", fieldWithMetaString ->"
                                + " fieldWithMetaString.getValue()).getMulti())"),
                "S8: the golden's ADD-terminal rung must carry the element-wise deref before"
                + " .getMulti()");
        assertTrue(Files.readString(GOLDEN_B.resolve(CARRIER_RULE3_QS_B))
                        .contains("final MapperS<ReferenceWithMetaNonNegativeQuantitySchedule>"
                                + " thenArg0"),
                "S1 rule 3: the golden's standalone `default` levels must declare the joined"
                + " ReferenceWithMeta wrapper");
    }

    /**
     * control2 — LAW 77 route parity: every seat in this family lives in the SHARED generator, which
     * the IR route runs too, so each carrier must render identically under {@code -Pir-on}. Skips
     * (recorded) without the IR provider on the classpath. Cell A's carriers only: the cell-B
     * locks' route parity is witnessed corpus-wide by the D11 matrix's ROUTE ROW DIFF NONE over
     * every drr 7.x cell (see the class javadoc's Part B paragraph).
     */
    @Test
    @EnabledIf("cellAAndIrProviderAvailable")
    void corpus_control2_irRouteMatchesLegacyForEveryCarrier() throws IOException {
        assertNotNull(drrAOutput, "drr 6.34.1 legacy-route generation did not run");
        Map<String, String> irOut = generateCellOnIrRoute(
                new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", CELL_A_ROOT), new ArrayList<>());
        for (String carrier : CARRIERS) {
            assertEquals(drrAOutput.get(carrier), irOut.get(carrier), "route divergence: " + carrier);
        }
    }

    private static void lock(String carrier) throws IOException {
        assertNotNull(drrAOutput, "drr 6.34.1 generation did not run — corpus unavailable?");
        List<String> own = drrAGenErrors.stream().filter(e -> e.contains(carrier)).toList();
        assertTrue(own.isEmpty(),
                "the generator reported errors for the locked file " + carrier + ": " + own);
        String generated = drrAOutput.get(carrier);
        assertNotNull(generated, "not generated in drr 6.34.1: " + carrier);
        Path goldenPath = GOLDEN_A.resolve(carrier);
        assertTrue(Files.isRegularFile(goldenPath), "golden missing: " + goldenPath);
        assertEquals(normalize(Files.readString(goldenPath)), normalize(generated),
                "generated drr 6.34.1 output must byte-match golden (newline-normalized) for "
                + carrier + " — PR #613 family 6: the refs scans and the render markers move onto"
                + " typed facts with no byte change.");
    }

    /** The cell-B (drr 7.0.0) twin of {@link #lock} — the SF-8 charter-carrier locks. */
    private static void lockB(String carrier) throws IOException {
        assertNotNull(drrBOutput, "drr 7.0.0 generation did not run — corpus unavailable?");
        List<String> own = drrBGenErrors.stream().filter(e -> e.contains(carrier)).toList();
        assertTrue(own.isEmpty(),
                "the generator reported errors for the locked file " + carrier + ": " + own);
        String generated = drrBOutput.get(carrier);
        assertNotNull(generated, "not generated in drr 7.0.0: " + carrier);
        Path goldenPath = GOLDEN_B.resolve(carrier);
        assertTrue(Files.isRegularFile(goldenPath), "golden missing: " + goldenPath);
        assertEquals(normalize(Files.readString(goldenPath)), normalize(generated),
                "generated drr 7.0.0 output must byte-match golden (newline-normalized) for "
                + carrier + " — PR #613 family 6: the refs scans and the render markers move onto"
                + " typed facts with no byte change.");
    }

    // =========================================================================
    // Fixture harness (the BlockArmWrapperHopDerefSeatTest renderer)
    // =========================================================================

    private record Render(Map<String, String> output, List<String> errors) {}

    private static Render rendered;

    private static Render render() throws IOException {
        if (rendered != null) {
            return rendered;
        }
        RModel main = AstBuilder.buildFromString(MODEL, "seat613f6.rosetta");
        main.setVersion("0.0.0.test");
        List<RModel> models = new ArrayList<>();
        models.add(main);
        models.addAll(loadBuiltinsOnly());
        RWorkspace workspace = RWorkspace.build(models).workspace();
        GeneratorModel gm = new GeneratorModel(workspace,
                m -> "census.seat613f6".equals(m.namespace()));
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

    /** Functions land under {@code .../functions/}. */
    private static String fn(String fileName) throws IOException {
        return generated("functions/" + fileName, fileName);
    }

    /** Reporting rules land under {@code .../reports/}. */
    private static String rule(String fileName) throws IOException {
        return generated("reports/" + fileName, fileName);
    }

    private static String generated(String suffix, String fileName) throws IOException {
        Render r = render();
        List<String> own = r.errors().stream().filter(e -> e.contains(fileName)).toList();
        assertTrue(own.isEmpty(),
                "the generator reported errors for " + fileName + " (a broken fixture must fail"
                + " loudly, not skip): " + own);
        String out = r.output().entrySet().stream()
                .filter(e -> e.getKey().endsWith("/" + suffix))
                .map(Map.Entry::getValue)
                .findFirst().orElse(null);
        assertNotNull(out, "not generated: " + fileName + " (have: " + r.output().keySet() + ")");
        return out;
    }

    private static void assertContains(String code, String needle) {
        assertTrue(code.contains(needle), "expected <" + needle + "> in:\n" + code);
    }

    private static void assertAbsent(String code, String needle) {
        assertTrue(!code.contains(needle), "did NOT expect <" + needle + "> in:\n" + code);
    }

    /**
     * Strip line and block comments plus string literals so a javadoc, a label or a
     * {@code "getId"} literal never counts as code. Every Part-A assertion runs on this.
     */
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
            throw new AssertionError("[MetaWrapperRecoverySeatTest] builtins parse failures: "
                    + String.join("; ", failures));
        }
        return models;
    }

    // =========================================================================
    // Corpus harness (the sibling seat suites' cell generator, verbatim)
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
                    new D11CorpusRegressionTest.CellSpec("drr", "6.34.1", CELL_A_ROOT), errs);
            drrAGenErrors = errs;
        }
        if (cellBAvailable()) {
            List<String> errs = new ArrayList<>();
            drrBOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("drr", "7.0.0", CELL_B_ROOT), errs);
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

    private static void collect(List<String> sink, List<GenerationException> errors) {
        if (errors != null) {
            errors.forEach(e -> sink.add(e.getTargetPath() + " - " + e));
        }
    }
}
