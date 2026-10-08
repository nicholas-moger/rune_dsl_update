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
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.function.LabelProviderGenerator;
import com.regnosys.rosetta.generator.java.function.LabelProviderGeneratorUtil;
import com.regnosys.rosetta.generator.java.object.ChoiceObjectGenerator;
import com.regnosys.rosetta.generator.java.object.ModelObjectGenerator;
import com.regnosys.rosetta.generator.java.object.datarule.DataRuleGenerator;
import com.regnosys.rosetta.generator.java.reports.ReportGenerator;
import com.regnosys.rosetta.generator.java.reports.RuleGenerator;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.symbols.RLinkingResult;
import com.regnosys.rosetta.symbols.RWorkspace;
import com.regnosys.rosetta.utils.DeepFeatureCallUtil;

/**
 * SEAT 21, lever H — facet {@code ruleReceiverRecordFeature}: <b>the {@code date} record feature of a
 * {@code zonedDateTime}/{@code dateTime} value renders upstream's record form
 * {@code .<Date>map("Date", zdt -> Date.of(zdt.toLocalDate()))} whatever the RECEIVER is — a RULE
 * call with args ({@code ExecutionTimestamp(transaction) -> date}), a bare RULE reference
 * ({@code extract ValuationTimestamp -> date}) or a rule-body disguised 2-name chain
 * ({@code collateralDetails -> collateralTimestamp -> date}) — because the receiver's type is what
 * admits the arm, and the fork's ONE receiver-typing walk ({@code NavigationHandler.resolveReceiverRType})
 * now types those three receiver shapes.</b> Upstream picks the arm by the LINKED feature's class
 * ({@code RosettaRecordFeature} → {@code recordCall}, {@code ExpressionGenerator.xtend:769-782, 352-357};
 * {@code RecordJavaUtil.xtend:51-68}) and never by context; the fork re-derives the receiver type, and
 * its walk had an {@code RFunction} arm (#377 {@code fnCallRecordReceiver}) but no {@code RRule}
 * sibling, and its {@code REnumValueRef} arm was function-scoped while
 * {@code resolveLambdaVarName} already read the same {@code resolvedAttributeChain} binding to name
 * the lambda var {@code zonedDateTime} (LAW 69 — the namer knew, the admission did not).
 *
 * <p><b>The defect.</b> The three carrier receivers fell to the POJO-getter path and emitted
 * {@code .map("getDate", x -> x.getDate())} on a {@code java.time.ZonedDateTime} — which does NOT
 * compile (javac: cannot find symbol getDate()). 16 band rows (drr 7.0–7.3 FUNCTION
 * {@code ExecutionISIN}/{@code ExecutionVenue} + POJO margin/valuation {@code EventDateRule}), 16
 * non-compiling files repaired (LAW 74).
 *
 * <p><b>The seat (two monotone arms in the shared walk + ONE rule-output read, LAW 69/67):</b> H1 —
 * an {@code RRule} symbol receiver types as {@code HandlerHelper.ruleInferredOutputRType} (the
 * workspace inference over the rule body, the read {@code RuleGenerator} types the generated
 * {@code evaluate()} from; previously written EIGHT times with eight local filters — four re-pointed at the
 * seat, three more ({@code CollectionHandler.enclosingOutputEnumeration},
 * {@code NavigationHandler.callableChoiceOutputBridge}, {@code FunctionExpressionRenderer.
 * renderReportRuleSetOrNull}) at the seat-21 review (MF-3), {@code RuleGenerator} the authority,
 * deliberately not);
 * H2 — a bound disguised chain ({@code resolvedAttributeChain}) supplies its leaf when
 * {@code resolveDisguisedFeature} declined (gated {@code compiler != null}, the 2-arg overload's
 * contract). The {@code instanceof RRecordType + isDateRecordFeature} gate in
 * {@code tryRecordFeatureNav} is UNTOUCHED — a data-type receiver with a real {@code date} getter
 * keeps the getter path ({@code b1}, {@code b2}). LAW 67, the THIRD receiver-typing walk:
 * {@code FunctionAliasHelper.inferReceiverRType} (the alias SIGNATURE) gets the {@code RRule} arm —
 * pinned by {@code a6} (a rule call as the receiver INSIDE a function alias, the ExecutionISIN venueMic
 * shape) and mutation (vii) after the seat-21 review found it pinned by nothing (SF-6); the fixture also
 * exposed and closed a PRE-EXISTING defect in that walk — an alias whose RESULT is a record feature fell
 * to the output-type fallback and rendered the RAW Rune name ({@code MapperS<? extends date>}, never
 * compiled; zero corpus carriers) — the {@code Date} witness rung in {@code inferFeatureCallType}, the
 * DISTINCT walk-through's own substitution.
 *
 * <p><b>LAW 75 — measured before the seat over all 275 matrix rows (the seat-21 runtime probe):</b>
 * 575 rule-symbol receivers and 6,506 bound-chain receivers reach the walk, ALL from
 * {@code tryRecordFeatureNav} (ZERO at the other nine consumers); of the rule receivers exactly 12
 * infer {@code zonedDateTime} (= the carriers), the other 563 infer data types the record gate
 * declines; exactly 4 bound-chain {@code -> date} hops are record-typed (= the margin carriers). The
 * static golden census: {@code .<Date>map("Date"} in 1,422 files / 2,099 sites across 18 cells, 1,406
 * files outside the band — the already-right population the arms never reach (their receivers are
 * attributes / feature calls / functions, not rules or rule-body chains).
 *
 * <p><b>RED at the pre-seat blob</b> ({@code rune-java-generator/src/main} at {@code 9c1bd8ef3}, this suite
 * kept — the COMBINED pre-seat blob, re-measured at the final head after the seat-21 review, MF-1):
 * <b>16 run / 11 F</b> = exactly {@code a1, a2, a3, a4, a5, a6, corpus_c1, corpus_c2, corpus_c3,
 * corpus_c4, corpus_control1}; every {@code b*} + {@code control0} GREEN in both states; GREEN 16/16
 * (default and {@code -Pir-on}). <b>LAW 66/76 mutations</b> (each applied → run → reverted AT THE FINAL
 * HEAD; {@code artefacts/review/rv-mut-h-*.log}): (i) H1 deleted → <b>16/8F</b> {a1, a2, a5, a6, c1, c3,
 * c4, control1}; (ii) H2 deleted → <b>16/5F</b> {a3, a4, c2, c4, control1}; (iii) the helper reads
 * {@code rule.fromType()} instead of the body inference → <b>16/8F</b>, the SAME set as (i): it confirms
 * the ONE read is the oracle, it pins no arm (i) leaves unpinned; (iv) the record gate relaxed (a
 * non-record receiver treated as zonedDateTime) → <b>16/3F</b> {b1, b2, control1} (b2's data-type rule
 * output AND b1's first `-> date` hop — a real getter — both flip to the record form; the control's zdt
 * count moves); (v) the RecordKind discriminator dropped (always {@code zdt}) → <b>16/3F</b> {a4, a5,
 * control1} (the separately-pinned dt count); (vi) the #377 RFunction arm deleted → <b>16/2F</b> {b3,
 * control1} ONLY (H1 is a distinct arm, not a re-implementation); (vii) the FunctionAliasHelper
 * {@code RRule} arm deleted → <b>16/1F</b> {a6} — the alias signature falls back to the raw-name form;
 * ZERO corpus carriers (no corpus alias returns a rule-call record feature — the ring was EXACT with
 * and without the arm), so the whole-cell control cannot move: a6 is the declared fixture pin. Every
 * other mutation moved its named pins AND {@code control1}.
 *
 * <p><b>Disclosed evidence caps / declared declines.</b> {@code a5} (a {@code dateTime}-output rule
 * call as the receiver) has ZERO corpus carriers — a fixture pin for the RecordKind discriminator.
 * A record-feature hop used as the RECEIVER of a date ACCESSOR hop ({@code Stamp(t) -> date -> year})
 * is typed by neither receiver walk (the {@code -> date} {@code RFeatureCall} resolves no attribute)
 * and renders an EMPTY assignment today; the shape has ZERO occurrences in every cell's sources
 * ({@code grep -E "-> (date|time) -> (year|month|day)"} = 0 in cdm 6.23.0 and drr 7.0.0) and is
 * declared OUT of this seat (banked: a record-leaf receiver typing arm). {@code corpus_c4} locks the
 * 4 carrier files of the generated drr 7.0.0 cell; the other 12 rows are the same files in 7.1–7.3,
 * locked by the 275-row matrix digest (not the ring, which is the five uniform 9.83.0 cells).
 */
class RuleReceiverRecordFeatureSeatTest {

    private static final Path REPO_ROOT =
            Path.of(System.getProperty("user.dir")).resolve("..").normalize();

    private static final List<Path> BUILTINS_SEARCH_ROOTS = List.of(
            REPO_ROOT.resolve("test-corpus/rune-dsl-builtins/rune-runtime/src/main/resources/model"),
            REPO_ROOT.resolve("rune-dsl/rune-runtime/src/main/resources/model"),
            REPO_ROOT.resolve("rune-runtime/src/main/resources/model"));

    static boolean builtinsAvailable() {
        return BUILTINS_SEARCH_ROOTS.stream().anyMatch(Files::isDirectory);
    }

    /** One input type, the rules/functions that produce zonedDateTime/dateTime/data values, one shape per pin. */
    private static final String MODEL = """
            namespace census.seat21h
            version "1.0.0"

            type In:
                ts zonedDateTime (1..1)
                dts dateTime (1..1)
                d date (1..1)
                agreement Agreement (1..1)
                inner Inner (1..1)

            type Agreement: <"b1/b2 - a DATA type with a real attribute NAMED date (the MapOtherAgreements shape)">
                date zonedDateTime (0..1)

            type Inner:
                ts zonedDateTime (1..1)

            type InnerDt:
                dts dateTime (1..1)

            type Outer:
                inner Inner (1..1)

            type OuterDt:
                inner InnerDt (1..1)

            reporting rule Stamp from In: <"a zonedDateTime-output RULE (ExecutionTimestamp / ValuationTimestamp)">
                ts

            reporting rule StampDt from In: <"a5 - a dateTime-output RULE">
                dts

            reporting rule Agr from In: <"b2 - a DATA-type-output RULE">
                agreement

            func GetStamp: <"b3 - the FUNCTION-call receiver the #377 arm already serves">
                inputs:
                    t In (1..1)
                output:
                    out zonedDateTime (1..1)
                set out:
                    t -> ts

            func A1RuleCallArgDate: <"a1 - CARRIER A: a rule call WITH ARGS as the `-> date` receiver (ExecutionISIN venueMic)">
                inputs:
                    t In (1..1)
                output:
                    out boolean (1..1)
                set out:
                    t -> d <= Stamp(t) -> date

            func A5RuleCallDateTime: <"a5 - a dateTime-output rule call as the receiver: the `dt` lambda var">
                inputs:
                    t In (1..1)
                output:
                    out date (1..1)
                set out:
                    StampDt(t) -> date

            func A6AliasRuleCallDate: <"a6 - the rule-call receiver inside a function ALIAS (the ExecutionISIN venueMic shape): the alias SIGNATURE types through FunctionAliasHelper's walk, the third receiver-typing walk (LAW 67)">
                inputs:
                    t In (1..1)
                output:
                    out date (0..1)
                alias stampDate: Stamp(t) -> date
                set out:
                    stampDate

            func B1AttrNamedDate: <"b1 - a real attribute named `date` declared zonedDateTime, then the record feature: getter hop + record hop on one line">
                inputs:
                    a Agreement (1..1)
                output:
                    out date (0..1)
                set out:
                    a -> date -> date

            func B2RuleDataOutputDateAttr: <"b2 - a DATA-type-output rule navigated to its REAL `date` attribute keeps the getter">
                inputs:
                    t In (1..1)
                output:
                    out zonedDateTime (0..1)
                set out:
                    Agr(t) -> date

            func B3FnCallDate: <"b3 - the #377 function-call receiver (GetExecutionTimestamp(x) -> date), untouched">
                inputs:
                    t In (1..1)
                output:
                    out date (1..1)
                set out:
                    GetStamp(t) -> date

            func B4FnDisguisedChainDate: <"b4 - the FUNCTION-context disguised 2-name chain (cdm fpmlFxEuropeanExercise -> expiryDate -> date), untouched">
                inputs:
                    inner Inner (1..1)
                output:
                    out date (1..1)
                set out:
                    inner -> ts -> date

            reporting rule A2BareRuleDate from In: <"a2 - CARRIER C: a bare rule reference as the receiver (extract ValuationTimestamp -> date)">
                extract Stamp -> date

            reporting rule A3RuleBodyChainDate from Outer: <"a3 - CARRIER B: the rule-body disguised 2-name chain then `-> date` (collateralDetails -> collateralTimestamp -> date)">
                inner -> ts -> date

            reporting rule A4RuleBodyChainDateTime from OuterDt: <"a4 - the dateTime twin of a3: the `dt` lambda var">
                inner -> dts -> date
            """;

    // =========================================================================
    // Part A — the seat (RED at the pre-seat blob)
    // =========================================================================

    private static final String ZDT_RECORD = ".<Date>map(\"Date\", zdt -> Date.of(zdt.toLocalDate()))";
    private static final String DT_RECORD = ".<Date>map(\"Date\", dt -> Date.of(dt.toLocalDate()))";
    /** The pre-seat getter fall-through on a java.time value — never compiles (no getDate()). */
    private static final String GETTER_ON_RECORD_VAR = "map(\"getDate\",";

    /** a1 — carrier A: a rule call with args as the receiver renders the record form (and the receiver stays the rule evaluate). */
    @Test
    @EnabledIf("builtinsAvailable")
    void a1_ruleCallWithArgsReceiverRendersTheRecordForm() throws IOException {
        String out = function("A1RuleCallArgDate.java");
        assertContains(out, "MapperS.of(stampRule.evaluate(t))" + ZDT_RECORD);
        assertNotContains(out, GETTER_ON_RECORD_VAR);
    }

    /** a2 — carrier C: a bare rule reference as the receiver inside the rule's extract. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a2_bareRuleReferenceReceiverRendersTheRecordForm() throws IOException {
        String out = rule("A2BareRuleDateRule.java");
        assertContains(out, "stampRule.evaluate(");
        assertContains(out, ZDT_RECORD);
        assertNotContains(out, GETTER_ON_RECORD_VAR);
    }

    /** a3 — carrier B: the rule-body disguised 2-name chain; the getter hops stay, the `date` hop is the record form. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a3_ruleBodyDisguisedChainRendersTheRecordForm() throws IOException {
        String out = rule("A3RuleBodyChainDateRule.java");
        assertContains(out, ".<ZonedDateTime>map(\"getTs\", inner -> inner.getTs())" + ZDT_RECORD);
        assertNotContains(out, GETTER_ON_RECORD_VAR);
    }

    /** a4 — the dateTime twin of a3 pins the RecordKind discriminator (`dt`, not `zdt`). */
    @Test
    @EnabledIf("builtinsAvailable")
    void a4_ruleBodyDisguisedChainDateTimeUsesTheDtVar() throws IOException {
        String out = rule("A4RuleBodyChainDateTimeRule.java");
        assertContains(out, ".<LocalDateTime>map(\"getDts\", innerDt -> innerDt.getDts())" + DT_RECORD);
        assertNotContains(out, "zdt");
        assertNotContains(out, GETTER_ON_RECORD_VAR);
    }

    /** a5 — a dateTime-output rule call as the receiver: H1 feeds the KIND, not just the record-ness. */
    @Test
    @EnabledIf("builtinsAvailable")
    void a5_dateTimeOutputRuleCallReceiverUsesTheDtVar() throws IOException {
        String out = function("A5RuleCallDateTime.java");
        assertContains(out, "MapperS.of(stampDtRule.evaluate(t))" + DT_RECORD);
        assertNotContains(out, "zdt");
        assertNotContains(out, GETTER_ON_RECORD_VAR);
    }

    // =========================================================================
    // Part B — placement pins (GREEN in BOTH states)
    // =========================================================================

    /** b1 — a real attribute NAMED date (declared zonedDateTime) then the record feature: the getter hop AND the record hop, in order. */
    /**
     * a6 — LAW 67, the THIRD receiver-typing walk: a rule call as the receiver INSIDE a function alias
     * ({@code alias stampDate: Stamp(t) -> date}). {@code FunctionAliasHelper.inferReceiverRType} types
     * the alias signature — it had the RFunction arm and no RRule sibling; the arm added at this seat is
     * pinned by this fixture and by mutation (vii) (seat-21 review SF-6: "pinned by no test or mutation").
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a6_aliasWithRuleCallReceiverTypesTheRecordForm() throws IOException {
        String out = function("A6AliasRuleCallDate.java");
        assertContains(out, "MapperS<Date> stampDate(In t)");
        assertContains(out, "MapperS.of(stampRule.evaluate(t)).<Date>map(\"Date\", zdt -> Date.of(zdt.toLocalDate()))");
        assertNotContains(out, "map(\"getDate\"");
    }

    @Test
    @EnabledIf("builtinsAvailable")
    void b1_attributeNamedDateThenRecordDateKeepsBothArms() throws IOException {
        String out = function("B1AttrNamedDate.java");
        assertContains(out, "MapperS.of(a).<ZonedDateTime>map(\"getDate\", agreement -> agreement.getDate())" + ZDT_RECORD);
    }

    /** b2 — a DATA-type-output rule navigated to its REAL `date` attribute keeps the getter: H1 types the receiver, the record gate declines. */
    @Test
    @EnabledIf("builtinsAvailable")
    void b2_dataTypeOutputRuleKeepsTheRealDateGetter() throws IOException {
        String out = function("B2RuleDataOutputDateAttr.java");
        assertContains(out, "MapperS.of(agrRule.evaluate(t)).<ZonedDateTime>map(\"getDate\", agreement -> agreement.getDate())");
        assertNotContains(out, ZDT_RECORD);
    }

    /** b3 — the #377 FUNCTION-call receiver is untouched (H1 is a sibling arm, not a rewrite). */
    @Test
    @EnabledIf("builtinsAvailable")
    void b3_functionCallReceiverStillRendersTheRecordForm() throws IOException {
        String out = function("B3FnCallDate.java");
        assertContains(out, "MapperS.of(getStamp.evaluate(t))" + ZDT_RECORD);
    }

    /** b4 — the FUNCTION-context disguised chain resolves through resolveDisguisedFeature's input arm, not H2. */
    @Test
    @EnabledIf("builtinsAvailable")
    void b4_functionContextDisguisedChainStillRendersTheRecordForm() throws IOException {
        String out = function("B4FnDisguisedChainDate.java");
        assertContains(out, "MapperS.of(inner).<ZonedDateTime>map(\"getTs\", _inner -> _inner.getTs())" + ZDT_RECORD);
    }

    // =========================================================================
    // Part C — the corpus (drr 7.0.0: the carriers + the whole-cell control)
    // =========================================================================

    private static final Path DRR7_CELL_ROOT = Path.of("../test-corpus/drr/drr-7.0.0");
    private static final Path DRR7_GOLDEN = DRR7_CELL_ROOT.resolve("rosetta-source/src/generated/java");

    static boolean drr7Available() {
        return Drr7Corpus.gate(Files.isDirectory(DRR7_GOLDEN), RuleReceiverRecordFeatureSeatTest.class);
    }

    private static final List<String> DRR7_CARRIERS = List.of(
            "drr/regulation/common/emir/contract/functions/ExecutionISIN.java",
            "drr/regulation/common/emir/contract/functions/ExecutionVenue.java",
            "drr/regulation/common/margin/datetime/reports/EventDateRule.java",
            "drr/regulation/common/valuation/datetime/reports/EventDateRule.java");

    /** c1 — carrier A whole-file lock (FUNCTION, the rule-call-with-args receiver inside the venueMic alias). */
    @Test
    @EnabledIf("drr7Available")
    void corpus_c1_executionIsinByteIdentical() throws IOException {
        lock(drr7Output, drr7GenErrors, DRR7_GOLDEN, DRR7_CARRIERS.get(0), "drr 7.0.0");
    }

    /** c2 — carrier B whole-file lock (POJO, the rule-body disguised chain). */
    @Test
    @EnabledIf("drr7Available")
    void corpus_c2_marginEventDateRuleByteIdentical() throws IOException {
        lock(drr7Output, drr7GenErrors, DRR7_GOLDEN, DRR7_CARRIERS.get(2), "drr 7.0.0");
    }

    /** c3 — carrier C whole-file lock (POJO, the bare rule reference receiver). */
    @Test
    @EnabledIf("drr7Available")
    void corpus_c3_valuationEventDateRuleByteIdentical() throws IOException {
        lock(drr7Output, drr7GenErrors, DRR7_GOLDEN, DRR7_CARRIERS.get(3), "drr 7.0.0");
    }

    /** c4 — ALL four carrier files of the generated drr 7.0.0 cell byte-identical. */
    @Test
    @EnabledIf("drr7Available")
    void corpus_c4_allFourCarrierFilesByteIdentical() throws IOException {
        for (String path : DRR7_CARRIERS) {
            lock(drr7Output, drr7GenErrors, DRR7_GOLDEN, path, "drr 7.0.0");
        }
    }

    /**
     * control0 — golden is the oracle (the frozen drr 7.0.0 tree, the whole FUNCTION+POJO population):
     * the record form's population is SEEN (the scan can see the token), the getter-on-record form
     * is ZERO, the getter form off a rule-evaluate receiver is ZERO (every rule-receiver `-> date`
     * golden is the record form), and the four carrier goldens each carry the record form. The
     * exact counts are the seat-21 static census (196 sites / 131 files; 49 `getDate` sites / 34
     * files — every one a data-type receiver with a real getter).
     */
    @Test
    @EnabledIf("drr7Available")
    void corpus_control0_goldenDrr7IsTheOracle() throws IOException {
        Map<String, String> golden = readGoldenTree(DRR7_GOLDEN);
        RecordScan g = scan(golden);
        assertEquals(196, g.recordSites, "golden drr 7.0.0 `.<Date>map(\"Date\", zdt -> …)` sites");
        // the RecordKind split (the seat-21 census: 193 zonedDateTime `zdt` sites + 3 dateTime `dt`
        // sites) — pinned separately so a mutation that drops the discriminator (always `zdt`) cannot
        // hide behind an unchanged total (LAW 76: a control must be able to fail under EACH mutation)
        assertEquals(193, g.zdtSites, "golden drr 7.0.0 zonedDateTime record-form sites (`zdt`)");
        assertEquals(3, g.dtSites, "golden drr 7.0.0 dateTime record-form sites (`dt`)");
        assertEquals(131, g.recordFiles.size(), "golden drr 7.0.0 record-form files");
        assertEquals(0, g.getterOnRecordReceiverFiles.size(),
                "golden carries NO getDate getter off a rule-evaluate / record-witnessed receiver: " + g.getterOnRecordReceiverFiles);
        assertEquals(49, g.getterDateSites, "golden drr 7.0.0 `map(\"getDate\",` sites (data-type receivers)");
        for (String carrier : DRR7_CARRIERS) {
            assertTrue(g.recordFiles.contains(carrier), "golden carrier carries the record form: " + carrier);
        }
    }

    /**
     * control1 — the FORK's WHOLE generated drr 7.0.0 cell (every kind the suite generates, LAW 72):
     * ZERO getDate getters off a rule-evaluate / record-witnessed receiver, the record-form site
     * count equals golden's, and the four carriers are REACHED (each carries the record form).
     */
    @Test
    @EnabledIf("drr7Available")
    void corpus_control1_forkDrr7WholeCellHasNoGetterOnRecordReceiver() throws IOException {
        assertNotNull(drr7Output, "drr 7.0.0 generation did not run");
        RecordScan f = scan(drr7Output);
        assertEquals(List.of(), f.getterOnRecordReceiverFiles,
                "fork emits a getDate getter off a rule-evaluate / record-witnessed receiver (non-compiling)");
        assertEquals(196, f.recordSites, "fork drr 7.0.0 record-form sites == golden");
        assertEquals(193, f.zdtSites, "fork drr 7.0.0 zonedDateTime record-form sites (`zdt`) == golden");
        assertEquals(3, f.dtSites, "fork drr 7.0.0 dateTime record-form sites (`dt`) == golden");
        for (String carrier : DRR7_CARRIERS) {
            assertTrue(f.recordFiles.contains(carrier), "carrier REACHED by the record arm: " + carrier);
        }
    }

    // =========================================================================
    // The scan (positive AND negative controls on the instrument)
    // =========================================================================

    private static final class RecordScan {
        int recordSites;
        int zdtSites;
        int dtSites;
        final List<String> recordFiles = new ArrayList<>();
        int getterDateSites;
        final List<String> getterOnRecordReceiverFiles = new ArrayList<>();
    }

    /**
     * The exact golden record form {@code .<Date>map("Date", zdt -> Date.of(zdt.toLocalDate()))} /
     * the {@code dt} twin counted per site; a {@code map("getDate", <v> -> <v>.getDate())} whose
     * receiver ends in {@code .evaluate(…))} (a rule/function call) or is preceded by a
     * {@code <ZonedDateTime>}/{@code <LocalDateTime>} witness is the non-compiling getter-on-record
     * shape this seat removes.
     */
    private static RecordScan scan(Map<String, String> tree) {
        RecordScan r = new RecordScan();
        for (Map.Entry<String, String> e : tree.entrySet()) {
            String code = codeOnly(e.getValue());
            int zdt = countOccurrences(code, ZDT_RECORD);
            int dt = countOccurrences(code, DT_RECORD);
            int sites = zdt + dt;
            if (sites > 0) {
                r.recordSites += sites;
                r.zdtSites += zdt;
                r.dtSites += dt;
                r.recordFiles.add(e.getKey());
            }
            r.getterDateSites += countOccurrences(code, "map(\"getDate\",");
            if (hasGetterOnRecordReceiver(code)) {
                r.getterOnRecordReceiverFiles.add(e.getKey());
            }
        }
        return r;
    }

    private static boolean hasGetterOnRecordReceiver(String code) {
        // the search token is the WITNESS-LESS getter form `.map("getDate",` — a real getter on a data
        // type renders `.<T>map("getDate",` (a witness between the dot and `map`) and never matches it;
        // the record fall-through has none (seat-21 review N-1: a second, dead `>map(` check was removed)
        int i = code.indexOf(".map(\"getDate\",");
        while (i >= 0) {
            String before = code.substring(Math.max(0, i - 80), i);
            // the receiver just rendered a call: `….evaluate(…))` immediately before `.map("getDate"`
            boolean afterEvaluate = before.endsWith(")") && before.contains(".evaluate(");
            // the previous hop witnessed a java.time type
            boolean afterRecordWitness = before.contains("<ZonedDateTime>map(") || before.contains("<LocalDateTime>map(");
            if (afterEvaluate || afterRecordWitness) {
                return true;
            }
            i = code.indexOf(".map(\"getDate\",", i + 1);
        }
        return false;
    }

    private static Map<String, String> readGoldenTree(Path dir) throws IOException {
        Map<String, String> out = new LinkedHashMap<>();
        try (var stream = Files.walk(dir)) {
            for (Path p : (Iterable<Path>) stream.filter(q -> q.toString().endsWith(".java"))::iterator) {
                out.put(dir.relativize(p).toString().replace('\\', '/'), Files.readString(p));
            }
        }
        return out;
    }

    /** Strip block and line comments so doc text cannot pollute a token count. */
    private static String codeOnly(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        int i = 0;
        while (i < s.length()) {
            if (s.startsWith("/*", i)) {
                int end = s.indexOf("*/", i + 2);
                i = end < 0 ? s.length() : end + 2;
            } else if (s.startsWith("//", i)) {
                int end = s.indexOf('\n', i);
                i = end < 0 ? s.length() : end;
            } else if (s.charAt(i) == '"') {
                int j = i + 1;
                while (j < s.length() && s.charAt(j) != '"') {
                    if (s.charAt(j) == '\\') j++;
                    j++;
                }
                sb.append(s, i, Math.min(j + 1, s.length()));
                i = j + 1;
            } else {
                sb.append(s.charAt(i));
                i++;
            }
        }
        return sb.toString();
    }

    private static int countOccurrences(String haystack, String needle) {
        int n = 0, i = haystack.indexOf(needle);
        while (i >= 0) {
            n++;
            i = haystack.indexOf(needle, i + needle.length());
        }
        return n;
    }

    // =========================================================================
    // Corpus generation (the same harness every seat suite uses)
    // =========================================================================

    private static Map<String, String> drr7Output;
    private static List<String> drr7GenErrors = new ArrayList<>();

    @BeforeAll
    static void generateCells() throws IOException {
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
                collect(errors, ruleGen.generateClasses(model, version, output));
                collect(errors, reportGen.generateClasses(model, version, output));
                collect(errors, dataRuleGen.generateClasses(model, version, output));
                collect(errors, labelProviderGen.generateClasses(model, version, output));
            }
        }
        collect(errors, funcGen.generateWithErrors(output));
        return output;
    }

    private static void collect(List<String> sink, List<GenerationException> errors) {
        if (errors != null) {
            errors.forEach(e -> sink.add(e.getTargetPath() + " — " + e));
        }
    }

    private static void lock(Map<String, String> output, List<String> genErrors, Path goldenDir,
            String path, String cell) throws IOException {
        assertNotNull(output, cell + " generation did not run — corpus unavailable?");
        List<String> lockedErrors = genErrors.stream().filter(e -> e.contains(path)).toList();
        assertTrue(lockedErrors.isEmpty(),
                "the generator reported errors for the locked file " + path + ": " + lockedErrors);
        String generated = output.get(path);
        assertNotNull(generated, "not generated in " + cell + ": " + path);
        Path goldenPath = goldenDir.resolve(path);
        assertTrue(Files.isRegularFile(goldenPath), "golden missing: " + goldenPath);
        assertEquals(normalize(Files.readString(goldenPath)), normalize(generated),
                "generated " + cell + " output must byte-match golden (newline-normalized) for "
                + path + " — seat 21 H: the date record feature off a rule-call / rule-body chain"
                + " receiver renders the record form.");
    }

    // =========================================================================
    // Fixture harness
    // =========================================================================

    private static RLinkingResult linking;
    private static RModel mainModel;
    private static Map<String, String> fixtureOut;

    private static void link() throws IOException {
        if (linking == null) {
            RModel main = AstBuilder.buildFromString(MODEL, "seat21h.rosetta");
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
            fixtureOut = render(m -> "census.seat21h".equals(m.namespace()));
        }
        return fixtureOut;
    }

    private static String rule(String fileName) throws IOException {
        return lookup(fixture(), "reports/" + fileName);
    }

    private static String function(String fileName) throws IOException {
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
            throw new AssertionError("[RuleReceiverRecordFeatureSeatTest] builtins parse"
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
