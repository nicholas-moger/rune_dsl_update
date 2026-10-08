package com.regnosys.rosetta.generator.java.optimised;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.RRootElement;
import com.regnosys.rosetta.ast.expressions.references.RDeepFeatureCall;
import com.regnosys.rosetta.ast.expressions.references.REnumValueRef;
import com.regnosys.rosetta.ast.expressions.references.RFeatureCall;
import com.regnosys.rosetta.ast.expressions.references.RImplicitVariable;
import com.regnosys.rosetta.ast.expressions.references.RSymbolReference;
import com.regnosys.rosetta.ast.expressions.supporting.ROnlyExistsElement;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.functions.ROperation;
import com.regnosys.rosetta.ast.functions.RRule;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.GenerationException;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.reports.ReportGenerator;
import com.regnosys.rosetta.generator.java.reports.RuleGenerator;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.symbols.RWorkspace;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE PR-4 EMISSION RECONCILIATION GATE — three conservation laws over
 * {@link OptimisedFunctionGenerator} on the FUNCTION-bearing cells:
 *
 * <ol>
 *   <li><b>COMPLETENESS (shortfall == 0):</b> every operation-body site the
 *       shared-classifier AST walk finds (recomputed live here — the same walk the
 *       census instrument prints) is IN the emitter's converted-site set. No eligible
 *       source-visible site is ever silently left on the Mapper idiom.</li>
 *   <li><b>NO SILENT BAILS (bailed == 0):</b> a classify-passed site whose reference
 *       render is not a plain expression would bail conservatively — the counter
 *       proves that path never fires on this corpus.</li>
 *   <li><b>THE PINNED EVENT COUNTS:</b> total conversions per cell are pinned to the
 *       census receipt (`research/p3-navigation-family-census.md` § 4). Conversions
 *       EXCEED the walk-visible population by design: the legacy render synthesizes
 *       parent-wired TWIN nav nodes (call-argument/constructor re-spellings —
 *       `ReferenceHandler.synthesize*`; unreachable from any {@code children()} walk)
 *       and dispatch-style renders emit the SAME AST body more than once
 *       (converted &gt; distinct) — every such event still passes the classifier at
 *       its own render, so the excess is the same predicate applied to the render's
 *       own re-spellings, never out-of-predicate conversion.</li>
 * </ol>
 *
 * <p>Also pinned: the emission WITNESS — generated bodies carry the ladder +
 * boundary wrap idiom (`MapperS.of((…`) — and zero generation errors cell-wide.
 */
class OptimisedNavigationEmissionTest {

    private static final JavaTypeUtil TYPE_UTIL = new JavaTypeUtil();
    private static final JavaTypeTranslator TYPE_TRANSLATOR = new JavaTypeTranslator(TYPE_UTIL);

    /**
     * THE PER-CELL PIN TABLE (PR #607: one row per function-bearing cell of the corpus SOT,
     * the three ring cells' values carried over UNCHANGED from the fourteen per-value maps this
     * record replaced; every band cell's row was measured by a {@code -Doptnav.reportOnly=true}
     * run and transcribed by an instrument, never typed by hand — the instrument-first
     * discipline). A cell whose OWN models carry functions or rules and has NO row here fails
     * loudly (below); iso20022 and rune-fpml carry neither and are skipped by that census, not
     * by a missing row. Each component keeps the receipt its map carried:
     *
     * <p><b>{@code conversions}</b> (was {@code PINNED_CONVERSIONS}) — The pinned conversion-EVENT counts per FUNCTION-bearing cell (the census § 4 +
     * § 9 + § 10 + § 12 receipts — tranches 1 + 2 + the family-2 meta emission +
     * the family-3 rule-face/item emission; deterministic on the frozen corpus —
     * any drift means the predicate, the corpus, or the render traversal changed,
     * and the census must be re-run and reconciled BEFORE touching these). The
     * family split is receipt-visible in the byShape breakdown ({@code :meta} /
     * {@code :item} keys disjoint from the § 9b values — cdm5/cdm6's param+meta
     * rows are BYTE-IDENTICAL to the landed § 9b/§ 10b receipts; drr's param rows
     * additionally carry the § 12a rule-face param pool, which is the SAME
     * PARAM-plan predicate emitting inside rule bodies). § 12 decomposition:
     * 782 = 746 landed + 36 item · 3,499 = 3,341 + 158 item · 9,140 = 6,825
     * landed + 1,968 item + 347 rule-face param/meta events (the § 12
     * rootTypeMissing refusal — the unprovable-wrapper class — bails 9/274
     * item events on cdm6/drr). The § 11 loop-form
     * re-price briefly measured 878/3,578/7,061 (+605 STREAM events) on the
     * loop-bearing intermediate — measured OUT (+494.7 MB/op).
     *
     * <p><b>{@code twinTypeMissingFallbacks}</b> (was {@code PINNED_TWIN_TYPE_MISSING_FALLBACKS}) — The pinned multi-twin type-missing fallback counts (census § 9, META-FREE
     * plans — receipt-comparable with the § 9b tuple): multi-shape conversion
     * attempts on render-synthesized twin nodes with MISSING adapted top types —
     * those seats keep the reference emission; walk-invisible by construction
     * (shortfall == 0 proves every walk site converted).
     *
     * <p><b>{@code metaTwinTypeMissingFallbacks}</b> (was {@code PINNED_META_TWIN_TYPE_MISSING_FALLBACKS}) — The pinned META twin type-missing fallback counts (census § 10b) — the meta
     * pool's twin of the tuple above (the § 10 receipt proves the walk-visible meta
     * pool carries ZERO missing top types, so every one of these is a
     * render-synthesized twin).
     *
     * <p><b>{@code streamDeclines}</b> (was {@code PINNED_STREAM_DECLINES}) — The pinned STREAM-shape decline counts (census § 9d + § 11 — TWICE measured
     * out; META-FREE plans, receipt-comparable with the § 9d tuple):
     * classify-passed STREAM sites deliberately kept on the reference emission.
     * The § 11 re-price DECOMPOSED these pins exactly (target-542-optdiag3.log):
     * 97 = 93 walk + 4 missing-fact twins · 215 = 202 (200 walk + 2 typed twins)
     * + 13 · 406 = 228 (200 walk + 28 typed twins) + 178 — the § 9b totals
     * (30 typed / 195 missing) confirmed per cell before the loop form measured
     * out (+494.7 MB/op, the fixed-cost-vs-N law's second red).
     *
     * <p><b>{@code metaStreamDeclines}</b> (was {@code PINNED_META_STREAM_DECLINES}) — The pinned META STREAM-shape decline counts (census § 10a) — the § 9d/§ 11
     * declines CARRIED to the family-2 pool, pinned apart so the § 9d tuple stays
     * receipt-comparable. The § 11 decomposition: 41 = 39 walk + 2 twins ·
     * 38 = 35 + 3 · 8 = 8 + 0.
     *
     * <p><b>{@code itemStreamDeclines}</b> (was {@code PINNED_ITEM_STREAM_DECLINES}) — The pinned ITEM-rooted META-FREE STREAM-shape decline counts (census § 12
     * — the § 9d/§ 11 verdict carried to the family-3 item pools; the drr value
     * is rule-face-dominated).
     *
     * <p><b>{@code itemMetaStreamDeclines}</b> (was {@code PINNED_ITEM_META_STREAM_DECLINES}) — The pinned ITEM-rooted META-bearing STREAM-shape decline counts (census
     * § 12 — the meta twin of the tuple above).
     *
     * <p><b>{@code itemInteriorSuppressed}</b> (was {@code PINNED_ITEM_INTERIOR_SUPPRESSED}) — The pinned ITEM-interior suppression counts (census § 12b.3 — interior
     * twin re-spells inside an outer ITEM plan's reference render, kept on the
     * reference emission so the outer harvest reads clean text; a twin class,
     * never a walk site).
     *
     * <p><b>{@code itemHarvestBails}</b> (was {@code PINNED_ITEM_HARVEST_BAILS}) — The pinned ITEM harvest-bail TOTALS (census § 12b.3 belts — every one
     * walk-invisible: {@code rootTypeMissing} = the unprovable-wrapper refusal
     * (a MISSING node-channel item type cannot prove the root is not
     * wrapper-typed — the first § 12 gate run's drr compile catch; mirrored in
     * the walk filter), {@code multiTopTypeMissing} = the § 9b twin class on
     * the item pool (drr 300 = 274 + 26).
     *
     * <p><b>{@code aliasSeams}</b> (was {@code PINNED_ALIAS_SEAMS}) — THE § 6.3 T1∪T2∪T3∪T4 VALUE-SEAM PINS — the tranche membership
     * MECHANISED (the T0 map's T1 partition 52/136/43 = 231 plus the T2
     * partition 173/269/145 = 587 plus the T3 partition 182/318/220 = 720
     * plus the T4 careful classes 73/127/20 = 220 since PR-25, totalling
     * 480/850/428 = 1,758 — the FULL flip population, the ¬EXCLUDED residue
     * of the 1,820 declarations; the map test's own belt asserts the flip
     * policy ≡ the landed-tranche column per row, and the equivalence gate's
     * O5-protected channel reconciles the emitted seam pairs to the same
     * union receipt). A drift here means the policy, the corpus or the map
     * moved — re-run {@code AliasDispositionMapTest} and reconcile BEFORE
     * touching.
     *
     * <p><b>{@code aliasSeamForms}</b> (was {@code PINNED_ALIAS_SEAM_FORMS}) — The emitted value seams' body-form breakdown per cell (the § 4 honest-shape
     * receipt): the T1 keys — SINGLE/TERMINAL_MULTI = the pure value ladders,
     * STREAM = the DISCLOSED Mapper-bodied fallback behind the value seam (no
     * landed stream ladder) — plus the {@code VALUE:<topKind>:<form>} keys:
     * {@code DIRECT} = the body IS the value (the outer structural Mapper-of
     * wrap stripped, a bare ctor, an item-typed only-element/count collapse, a
     * zero-arg call — the § 4 direct composition), {@code BOUNDARY} = the
     * disclosed Mapper-bodied fallback value-unwrapped once at the seam
     * boundary, {@code :hoist} = the decl-led lifted/then/sink bodies (decls
     * verbatim + the trailing return re-shaped), {@code LADDER} /
     * {@code LADDER:switch} (T3, PR-24) = the multi-return conditional /
     * instanceof-switch ladders with per-rung value decoration and § 2 value
     * empties — EVERY conditional member landed the ladder (75/107/74 = 256
     * at T3; 81/113/78 = 272 with the 16 T4 conditional rows riding the SAME
     * machinery, PR-25) and every switch member the switch ladder (0/11/0 at
     * T3; 0/13/0 with T4); the PIPE class rides the decorated then-hoist;
     * the T4 careful classes (PR-25) add the LIBRARY_APPLY /
     * RECORD_FEATURE_NAV / DEEP_FEATURE_NAV keys (dispatch-unit bodies
     * dominantly) on the standing DIRECT/BOUNDARY forms; ZERO
     * {@code BOUNDARY:block} (the last resort never fired, any tranche).
     * Derived from the T0 map's tranche profiles + the PR-23/PR-24/PR-25
     * measure passes ({@code -Doptnav.reportOnly=true}).
     *
     * <p><b>{@code aliasRootConversions}</b> (was {@code PINNED_ALIAS_ROOT_CONVERSIONS}) — The pinned ALIAS_CALL-rooted ladder conversion events per cell (the § 5
     * CHAIN_ROOT deliverable at T1 grain — chains rooted at a flipped alias's
     * invocation re-emitted as the alias-rooted § 9a ladder at operation
     * seats; alias-body and STREAM-shaped seats ride the leaf bridge instead,
     * counted apart).
     *
     * <p>A VALUE-BODY member is never {@code aliasRootLadderEligible} — the
     * ladder re-invokes its root k+1 times per guard, the T1 getter-repeat
     * class, but a value body invokes DEPENDENCY functions / full Mapper
     * pipelines / whole conditional ladders; those members' chain-root seats
     * ride the ONE-invocation leaf bridge, preserving today's per-reference
     * re-invocation count identically (plan § 2). The pins held UNCHANGED at
     * the T1 values through T2 AND T3 (PR-23/PR-24) — the mechanised proof of
     * that decision.
     *
     * <p>GREW AT T4 (PR-25, cdm6 +195): the T4 WHOLE-CHAIN careful rows are
     * T1-SHAPED bodies (pure getter ladders — the cdm6 {@code cdm/ingest/**}
     * D4-live class dominantly), so they are ladder-eligible under the SAME
     * T1 law, and their consumer chains — DISGUISED {@code REnumValueRef}
     * spellings the counting channels cannot see but the walk's
     * disguised-head resolution CAN — convert at their operation seats
     * exactly like visible T1 roots (the getter-repeat class, the accepted
     * economics). cdm5/drr T4 rows carry no such convertible seats — their
     * cells hold at the T1 values.
     *
     * <p><b>{@code aliasRootStreamDeclines}</b> (was {@code PINNED_ALIAS_ROOT_STREAM_DECLINES}) — The pinned STREAM-shaped ALIAS_CALL declines per cell (a multi-seamed
     * alias root and/or interior multi hops — kept on the bridge composition,
     * the § 9d/§ 11 economics carried to the alias-root pool). The T4 rows
     * add 2/3/0 (PR-25 — STREAM-shaped careful roots decline to the bridge
     * like every STREAM root before them).
     *
     * <p><b>{@code aliasLeafWraps}</b> (was {@code PINNED_ALIAS_LEAF_WRAPS}) — The pinned leaf bridge-wrap emissions per cell ({@code S=n C=m} — every
     * alias invocation seat of a flipped member: the structural
     * {@code MapperS.of(...)} / {@code MapperC.<X>of(...)} whose unwrap channel
     * restores the bare value at evaluate-arg/assignment strip seats).
     */
    record CellPins(int conversions, int twinTypeMissingFallbacks,
                    int metaTwinTypeMissingFallbacks, int streamDeclines, int metaStreamDeclines,
                    int itemStreamDeclines, int itemMetaStreamDeclines, int itemInteriorSuppressed,
                    int itemHarvestBails, int aliasSeams, String aliasSeamForms,
                    int aliasRootConversions, int aliasRootStreamDeclines, String aliasLeafWraps) {
    }

    private static Map.Entry<String, CellPins> pin(String cell, int conversions,
            int twinTypeMissingFallbacks, int metaTwinTypeMissingFallbacks, int streamDeclines,
            int metaStreamDeclines, int itemStreamDeclines, int itemMetaStreamDeclines,
            int itemInteriorSuppressed, int itemHarvestBails, int aliasSeams, String aliasSeamForms,
            int aliasRootConversions, int aliasRootStreamDeclines, String aliasLeafWraps) {
        return Map.entry(cell, new CellPins(conversions, twinTypeMissingFallbacks,
                metaTwinTypeMissingFallbacks, streamDeclines, metaStreamDeclines,
                itemStreamDeclines, itemMetaStreamDeclines, itemInteriorSuppressed,
                itemHarvestBails, aliasSeams, aliasSeamForms, aliasRootConversions,
                aliasRootStreamDeclines, aliasLeafWraps));
    }

    /**
     * The rows, in corpus-SOT order. Column order = the record: conversions ·
     * twinTypeMissingFallbacks · metaTwinTypeMissingFallbacks · streamDeclines · metaStreamDeclines ·
     * itemStreamDeclines · itemMetaStreamDeclines · itemInteriorSuppressed · itemHarvestBails ·
     * aliasSeams · aliasSeamForms · aliasRootConversions · aliasRootStreamDeclines · aliasLeafWraps.
     */
    private static final Map<String, CellPins> PINS = Map.ofEntries(
            pin("cdm/5.38.0",
                    782, 89, 9, 97, 41, 2, 3, 40, 0, 480,
                    "SINGLE=41 SINGLE:meta=4 STREAM=4 STREAM:meta=4 TERMINAL_MULTI=1"
                            + " VALUE:APPLY:DIRECT=132 VALUE:APPLY:DIRECT:hoist=4 VALUE:BINARY_OP:BOUNDARY=26"
                            + " VALUE:BINARY_OP:BOUNDARY:hoist=1 VALUE:CONDITIONAL:LADDER=81"
                            + " VALUE:CONSTRUCT:DIRECT=14 VALUE:EXISTENCE:BOUNDARY=2"
                            + " VALUE:FIELD_ACCESS:BOUNDARY=50 VALUE:FIELD_ACCESS:BOUNDARY:hoist=2"
                            + " VALUE:LAMBDA_OP:BOUNDARY=8 VALUE:LIBRARY_APPLY:BOUNDARY:hoist=3"
                            + " VALUE:LIBRARY_APPLY:DIRECT=3 VALUE:LIST_OP:BOUNDARY=8 VALUE:LIST_OP:DIRECT=41"
                            + " VALUE:META_ACCESS:BOUNDARY=18 VALUE:PIPE:BOUNDARY:hoist=19"
                            + " VALUE:RECORD_FEATURE_NAV:BOUNDARY=12 VALUE:VARIABLE:DIRECT=2",
                    38, 4,
                    "C=260 S=844"),
            pin("cdm/5.39.0",
                    782, 89, 9, 97, 41, 2, 3, 40, 0, 480,
                    "SINGLE=41 SINGLE:meta=4 STREAM=4 STREAM:meta=4 TERMINAL_MULTI=1"
                            + " VALUE:APPLY:DIRECT=132 VALUE:APPLY:DIRECT:hoist=4 VALUE:BINARY_OP:BOUNDARY=26"
                            + " VALUE:BINARY_OP:BOUNDARY:hoist=1 VALUE:CONDITIONAL:LADDER=81"
                            + " VALUE:CONSTRUCT:DIRECT=14 VALUE:EXISTENCE:BOUNDARY=2"
                            + " VALUE:FIELD_ACCESS:BOUNDARY=50 VALUE:FIELD_ACCESS:BOUNDARY:hoist=2"
                            + " VALUE:LAMBDA_OP:BOUNDARY=8 VALUE:LIBRARY_APPLY:BOUNDARY:hoist=3"
                            + " VALUE:LIBRARY_APPLY:DIRECT=3 VALUE:LIST_OP:BOUNDARY=8 VALUE:LIST_OP:DIRECT=41"
                            + " VALUE:META_ACCESS:BOUNDARY=18 VALUE:PIPE:BOUNDARY:hoist=19"
                            + " VALUE:RECORD_FEATURE_NAV:BOUNDARY=12 VALUE:VARIABLE:DIRECT=2",
                    38, 4,
                    "C=260 S=844"),
            pin("cdm/6.20.2",
                    3_496, 420, 11, 221, 38, 4, 2, 96, 9, 849,
                    "SINGLE=134 SINGLE:meta=4 STREAM=13 STREAM:meta=4 TERMINAL_MULTI=11"
                            + " TERMINAL_MULTI:meta=1 VALUE:APPLY:DIRECT=214 VALUE:APPLY:DIRECT:hoist=5"
                            + " VALUE:BINARY_OP:BOUNDARY=30 VALUE:BINARY_OP:BOUNDARY:hoist=2"
                            + " VALUE:CHOICE_OPTION_NAV:BOUNDARY=4 VALUE:COLLECT_OP:BOUNDARY=1"
                            + " VALUE:CONDITIONAL:LADDER=113 VALUE:CONSTRUCT:DIRECT=37"
                            + " VALUE:CONVERSION:BOUNDARY=1 VALUE:DEEP_FEATURE_NAV:BOUNDARY=1"
                            + " VALUE:DEFAULT_OP:BOUNDARY=1 VALUE:DEFAULT_OP:DIRECT=8"
                            + " VALUE:EXISTENCE:BOUNDARY=3 VALUE:FIELD_ACCESS:BOUNDARY=73"
                            + " VALUE:FIELD_ACCESS:BOUNDARY:hoist=3 VALUE:LAMBDA_OP:BOUNDARY=28"
                            + " VALUE:LIBRARY_APPLY:BOUNDARY:hoist=3 VALUE:LIBRARY_APPLY:DIRECT=3"
                            + " VALUE:LIST_OP:BOUNDARY=13 VALUE:LIST_OP:DIRECT=37"
                            + " VALUE:MEMBERSHIP_OP:BOUNDARY=1 VALUE:META_ACCESS:BOUNDARY=19"
                            + " VALUE:META_ACCESS:BOUNDARY:hoist=1 VALUE:META_OUTPUT_APPLY:DIRECT=3"
                            + " VALUE:PIPE:BOUNDARY:hoist=51 VALUE:RECORD_FEATURE_NAV:BOUNDARY=12"
                            + " VALUE:SWITCH_OP:LADDER:switch=13 VALUE:VARIABLE:DIRECT=2",
                    313, 32,
                    "C=396 S=1680"),
            pin("cdm/6.20.3",
                    3_496, 420, 11, 221, 38, 4, 2, 96, 9, 849,
                    "SINGLE=134 SINGLE:meta=4 STREAM=13 STREAM:meta=4 TERMINAL_MULTI=11"
                            + " TERMINAL_MULTI:meta=1 VALUE:APPLY:DIRECT=214 VALUE:APPLY:DIRECT:hoist=5"
                            + " VALUE:BINARY_OP:BOUNDARY=30 VALUE:BINARY_OP:BOUNDARY:hoist=2"
                            + " VALUE:CHOICE_OPTION_NAV:BOUNDARY=4 VALUE:COLLECT_OP:BOUNDARY=1"
                            + " VALUE:CONDITIONAL:LADDER=113 VALUE:CONSTRUCT:DIRECT=37"
                            + " VALUE:CONVERSION:BOUNDARY=1 VALUE:DEEP_FEATURE_NAV:BOUNDARY=1"
                            + " VALUE:DEFAULT_OP:BOUNDARY=1 VALUE:DEFAULT_OP:DIRECT=8"
                            + " VALUE:EXISTENCE:BOUNDARY=3 VALUE:FIELD_ACCESS:BOUNDARY=73"
                            + " VALUE:FIELD_ACCESS:BOUNDARY:hoist=3 VALUE:LAMBDA_OP:BOUNDARY=28"
                            + " VALUE:LIBRARY_APPLY:BOUNDARY:hoist=3 VALUE:LIBRARY_APPLY:DIRECT=3"
                            + " VALUE:LIST_OP:BOUNDARY=13 VALUE:LIST_OP:DIRECT=37"
                            + " VALUE:MEMBERSHIP_OP:BOUNDARY=1 VALUE:META_ACCESS:BOUNDARY=19"
                            + " VALUE:META_ACCESS:BOUNDARY:hoist=1 VALUE:META_OUTPUT_APPLY:DIRECT=3"
                            + " VALUE:PIPE:BOUNDARY:hoist=51 VALUE:RECORD_FEATURE_NAV:BOUNDARY=12"
                            + " VALUE:SWITCH_OP:LADDER:switch=13 VALUE:VARIABLE:DIRECT=2",
                    313, 32,
                    "C=396 S=1680"),
            pin("cdm/6.20.4",
                    3_496, 420, 11, 221, 38, 4, 2, 96, 9, 849,
                    "SINGLE=134 SINGLE:meta=4 STREAM=13 STREAM:meta=4 TERMINAL_MULTI=11"
                            + " TERMINAL_MULTI:meta=1 VALUE:APPLY:DIRECT=214 VALUE:APPLY:DIRECT:hoist=5"
                            + " VALUE:BINARY_OP:BOUNDARY=30 VALUE:BINARY_OP:BOUNDARY:hoist=2"
                            + " VALUE:CHOICE_OPTION_NAV:BOUNDARY=4 VALUE:COLLECT_OP:BOUNDARY=1"
                            + " VALUE:CONDITIONAL:LADDER=113 VALUE:CONSTRUCT:DIRECT=37"
                            + " VALUE:CONVERSION:BOUNDARY=1 VALUE:DEEP_FEATURE_NAV:BOUNDARY=1"
                            + " VALUE:DEFAULT_OP:BOUNDARY=1 VALUE:DEFAULT_OP:DIRECT=8"
                            + " VALUE:EXISTENCE:BOUNDARY=3 VALUE:FIELD_ACCESS:BOUNDARY=73"
                            + " VALUE:FIELD_ACCESS:BOUNDARY:hoist=3 VALUE:LAMBDA_OP:BOUNDARY=28"
                            + " VALUE:LIBRARY_APPLY:BOUNDARY:hoist=3 VALUE:LIBRARY_APPLY:DIRECT=3"
                            + " VALUE:LIST_OP:BOUNDARY=13 VALUE:LIST_OP:DIRECT=37"
                            + " VALUE:MEMBERSHIP_OP:BOUNDARY=1 VALUE:META_ACCESS:BOUNDARY=19"
                            + " VALUE:META_ACCESS:BOUNDARY:hoist=1 VALUE:META_OUTPUT_APPLY:DIRECT=3"
                            + " VALUE:PIPE:BOUNDARY:hoist=51 VALUE:RECORD_FEATURE_NAV:BOUNDARY=12"
                            + " VALUE:SWITCH_OP:LADDER:switch=13 VALUE:VARIABLE:DIRECT=2",
                    313, 32,
                    "C=396 S=1680"),
            pin("cdm/6.20.5",
                    3_496, 420, 11, 221, 38, 4, 2, 96, 9, 849,
                    "SINGLE=134 SINGLE:meta=4 STREAM=13 STREAM:meta=4 TERMINAL_MULTI=11"
                            + " TERMINAL_MULTI:meta=1 VALUE:APPLY:DIRECT=214 VALUE:APPLY:DIRECT:hoist=5"
                            + " VALUE:BINARY_OP:BOUNDARY=30 VALUE:BINARY_OP:BOUNDARY:hoist=2"
                            + " VALUE:CHOICE_OPTION_NAV:BOUNDARY=4 VALUE:COLLECT_OP:BOUNDARY=1"
                            + " VALUE:CONDITIONAL:LADDER=113 VALUE:CONSTRUCT:DIRECT=37"
                            + " VALUE:CONVERSION:BOUNDARY=1 VALUE:DEEP_FEATURE_NAV:BOUNDARY=1"
                            + " VALUE:DEFAULT_OP:BOUNDARY=1 VALUE:DEFAULT_OP:DIRECT=8"
                            + " VALUE:EXISTENCE:BOUNDARY=3 VALUE:FIELD_ACCESS:BOUNDARY=73"
                            + " VALUE:FIELD_ACCESS:BOUNDARY:hoist=3 VALUE:LAMBDA_OP:BOUNDARY=28"
                            + " VALUE:LIBRARY_APPLY:BOUNDARY:hoist=3 VALUE:LIBRARY_APPLY:DIRECT=3"
                            + " VALUE:LIST_OP:BOUNDARY=13 VALUE:LIST_OP:DIRECT=37"
                            + " VALUE:MEMBERSHIP_OP:BOUNDARY=1 VALUE:META_ACCESS:BOUNDARY=19"
                            + " VALUE:META_ACCESS:BOUNDARY:hoist=1 VALUE:META_OUTPUT_APPLY:DIRECT=3"
                            + " VALUE:PIPE:BOUNDARY:hoist=51 VALUE:RECORD_FEATURE_NAV:BOUNDARY=12"
                            + " VALUE:SWITCH_OP:LADDER:switch=13 VALUE:VARIABLE:DIRECT=2",
                    313, 32,
                    "C=396 S=1680"),
            pin("cdm/6.20.6",
                    3_499, 423, 11, 215, 38, 4, 2, 96, 9, 850,
                    "SINGLE=134 SINGLE:meta=4 STREAM=11 STREAM:meta=4 TERMINAL_MULTI=11"
                            + " TERMINAL_MULTI:meta=1 VALUE:APPLY:DIRECT=214 VALUE:APPLY:DIRECT:hoist=5"
                            + " VALUE:BINARY_OP:BOUNDARY=30 VALUE:BINARY_OP:BOUNDARY:hoist=2"
                            + " VALUE:CHOICE_OPTION_NAV:BOUNDARY=4 VALUE:COLLECT_OP:BOUNDARY=1"
                            + " VALUE:CONDITIONAL:LADDER=113 VALUE:CONSTRUCT:DIRECT=37"
                            + " VALUE:CONVERSION:BOUNDARY=1 VALUE:DEEP_FEATURE_NAV:BOUNDARY=1"
                            + " VALUE:DEFAULT_OP:BOUNDARY=1 VALUE:DEFAULT_OP:DIRECT=8"
                            + " VALUE:EXISTENCE:BOUNDARY=3 VALUE:FIELD_ACCESS:BOUNDARY=73"
                            + " VALUE:FIELD_ACCESS:BOUNDARY:hoist=3 VALUE:LAMBDA_OP:BOUNDARY=28"
                            + " VALUE:LIBRARY_APPLY:BOUNDARY:hoist=3 VALUE:LIBRARY_APPLY:DIRECT=3"
                            + " VALUE:LIST_OP:BOUNDARY=15 VALUE:LIST_OP:DIRECT=38"
                            + " VALUE:MEMBERSHIP_OP:BOUNDARY=1 VALUE:META_ACCESS:BOUNDARY=19"
                            + " VALUE:META_ACCESS:BOUNDARY:hoist=1 VALUE:META_OUTPUT_APPLY:DIRECT=3"
                            + " VALUE:PIPE:BOUNDARY:hoist=51 VALUE:RECORD_FEATURE_NAV:BOUNDARY=12"
                            + " VALUE:SWITCH_OP:LADDER:switch=13 VALUE:VARIABLE:DIRECT=2",
                    313, 8,
                    "C=384 S=1694"),
            pin("cdm/6.21.0",
                    3_947, 567, 11, 210, 38, 4, 2, 84, 9, 826,
                    "SINGLE=117 SINGLE:meta=4 STREAM=11 STREAM:meta=4 TERMINAL_MULTI=13"
                            + " TERMINAL_MULTI:meta=1 VALUE:APPLY:DIRECT=215 VALUE:APPLY:DIRECT:hoist=5"
                            + " VALUE:BINARY_OP:BOUNDARY=30 VALUE:BINARY_OP:BOUNDARY:hoist=2"
                            + " VALUE:CHOICE_OPTION_NAV:BOUNDARY=4 VALUE:COLLECT_OP:BOUNDARY=1"
                            + " VALUE:CONDITIONAL:LADDER=116 VALUE:CONSTRUCT:DIRECT=37"
                            + " VALUE:CONVERSION:BOUNDARY=1 VALUE:DEEP_FEATURE_NAV:BOUNDARY=1"
                            + " VALUE:DEFAULT_OP:BOUNDARY=1 VALUE:DEFAULT_OP:DIRECT=5"
                            + " VALUE:EXISTENCE:BOUNDARY=3 VALUE:FIELD_ACCESS:BOUNDARY=69"
                            + " VALUE:FIELD_ACCESS:BOUNDARY:hoist=3 VALUE:LAMBDA_OP:BOUNDARY=24"
                            + " VALUE:LIBRARY_APPLY:BOUNDARY:hoist=3 VALUE:LIBRARY_APPLY:DIRECT=3"
                            + " VALUE:LIST_OP:BOUNDARY=16 VALUE:LIST_OP:DIRECT=37"
                            + " VALUE:MEMBERSHIP_OP:BOUNDARY=1 VALUE:META_ACCESS:BOUNDARY=19"
                            + " VALUE:META_ACCESS:BOUNDARY:hoist=1 VALUE:META_OUTPUT_APPLY:DIRECT=3"
                            + " VALUE:PIPE:BOUNDARY:hoist=49 VALUE:RECORD_FEATURE_NAV:BOUNDARY=12"
                            + " VALUE:SWITCH_OP:LADDER:switch=13 VALUE:VARIABLE:DIRECT=2",
                    113, 10,
                    "C=381 S=1556"),
            pin("cdm/6.22.0",
                    3_950, 564, 11, 210, 38, 4, 2, 92, 9, 826,
                    "SINGLE=117 SINGLE:meta=4 STREAM=11 STREAM:meta=4 TERMINAL_MULTI=13"
                            + " TERMINAL_MULTI:meta=1 VALUE:APPLY:DIRECT=215 VALUE:APPLY:DIRECT:hoist=5"
                            + " VALUE:BINARY_OP:BOUNDARY=30 VALUE:BINARY_OP:BOUNDARY:hoist=2"
                            + " VALUE:CHOICE_OPTION_NAV:BOUNDARY=4 VALUE:COLLECT_OP:BOUNDARY=1"
                            + " VALUE:CONDITIONAL:LADDER=116 VALUE:CONSTRUCT:DIRECT=37"
                            + " VALUE:CONVERSION:BOUNDARY=1 VALUE:DEEP_FEATURE_NAV:BOUNDARY=1"
                            + " VALUE:DEFAULT_OP:BOUNDARY=1 VALUE:DEFAULT_OP:DIRECT=5"
                            + " VALUE:EXISTENCE:BOUNDARY=3 VALUE:FIELD_ACCESS:BOUNDARY=69"
                            + " VALUE:FIELD_ACCESS:BOUNDARY:hoist=3 VALUE:LAMBDA_OP:BOUNDARY=24"
                            + " VALUE:LIBRARY_APPLY:BOUNDARY:hoist=3 VALUE:LIBRARY_APPLY:DIRECT=3"
                            + " VALUE:LIST_OP:BOUNDARY=16 VALUE:LIST_OP:DIRECT=37"
                            + " VALUE:MEMBERSHIP_OP:BOUNDARY=1 VALUE:META_ACCESS:BOUNDARY=19"
                            + " VALUE:META_ACCESS:BOUNDARY:hoist=1 VALUE:META_OUTPUT_APPLY:DIRECT=3"
                            + " VALUE:PIPE:BOUNDARY:hoist=49 VALUE:RECORD_FEATURE_NAV:BOUNDARY=12"
                            + " VALUE:SWITCH_OP:LADDER:switch=13 VALUE:VARIABLE:DIRECT=2",
                    113, 10,
                    "C=381 S=1556"),
            pin("cdm/6.23.0",
                    3_950, 564, 11, 210, 38, 4, 2, 92, 9, 826,
                    "SINGLE=117 SINGLE:meta=4 STREAM=11 STREAM:meta=4 TERMINAL_MULTI=13"
                            + " TERMINAL_MULTI:meta=1 VALUE:APPLY:DIRECT=215 VALUE:APPLY:DIRECT:hoist=5"
                            + " VALUE:BINARY_OP:BOUNDARY=30 VALUE:BINARY_OP:BOUNDARY:hoist=2"
                            + " VALUE:CHOICE_OPTION_NAV:BOUNDARY=4 VALUE:COLLECT_OP:BOUNDARY=1"
                            + " VALUE:CONDITIONAL:LADDER=116 VALUE:CONSTRUCT:DIRECT=37"
                            + " VALUE:CONVERSION:BOUNDARY=1 VALUE:DEEP_FEATURE_NAV:BOUNDARY=1"
                            + " VALUE:DEFAULT_OP:BOUNDARY=1 VALUE:DEFAULT_OP:DIRECT=5"
                            + " VALUE:EXISTENCE:BOUNDARY=3 VALUE:FIELD_ACCESS:BOUNDARY=69"
                            + " VALUE:FIELD_ACCESS:BOUNDARY:hoist=3 VALUE:LAMBDA_OP:BOUNDARY=24"
                            + " VALUE:LIBRARY_APPLY:BOUNDARY:hoist=3 VALUE:LIBRARY_APPLY:DIRECT=3"
                            + " VALUE:LIST_OP:BOUNDARY=16 VALUE:LIST_OP:DIRECT=37"
                            + " VALUE:MEMBERSHIP_OP:BOUNDARY=1 VALUE:META_ACCESS:BOUNDARY=19"
                            + " VALUE:META_ACCESS:BOUNDARY:hoist=1 VALUE:META_OUTPUT_APPLY:DIRECT=3"
                            + " VALUE:PIPE:BOUNDARY:hoist=49 VALUE:RECORD_FEATURE_NAV:BOUNDARY=12"
                            + " VALUE:SWITCH_OP:LADDER:switch=13 VALUE:VARIABLE:DIRECT=2",
                    113, 10,
                    "C=381 S=1556"),
            pin("drr/5.61.0",
                    // v3.2 seat 8: 6_218 -> 6_211 = -7 - the only-exists enclosure read (ROnlyExistsElement.encloses): the pre-seat
                    // PARENT test admitted the generator's item-rooted ROOT twin of a two-or-more-hop `only exists` path inside a
                    // lambda (the twin hangs off the hop ABOVE the root, one level below the element - `terminationProvision ->
                    // earlyTerminationProvision -> optionalEarlyTermination only exists` and kin), a leak of the classifier's own law
                    // that the ancestor walk refuses. Value-identical under the 3-arg ExpressionOperators.onlyExists (it reads
                    // getMulti alone); transcribed from run B of sites-s8.ps1 (target/v32-seat8-instruments/scratch/
                    // sites-B-optimised.log, local); the per-site diff (sites-diff.txt) names every moved site: 34 over the ten
                    // drr cells, one class, none on any cdm cell or the chaos cell.
                    6_211, 70, 1, 33, 26, 265, 183, 2_914, 342, 283,
                    "SINGLE=35 SINGLE:meta=1 STREAM=1 VALUE:APPLY:DIRECT=78"
                            + " VALUE:BINARY_OP:BOUNDARY=4 VALUE:CONDITIONAL:LADDER=61"
                            + " VALUE:CONSTRUCT:DIRECT=1 VALUE:DEFAULT_OP:BOUNDARY=7"
                            + " VALUE:EXISTENCE:BOUNDARY=4 VALUE:FIELD_ACCESS:BOUNDARY=18"
                            + " VALUE:LAMBDA_OP:BOUNDARY=7 VALUE:LIST_OP:BOUNDARY=2 VALUE:LIST_OP:DIRECT=9"
                            + " VALUE:LIST_OP:DIRECT:hoist=1 VALUE:MEMBERSHIP_OP:BOUNDARY=7"
                            + " VALUE:META_ACCESS:BOUNDARY=5 VALUE:PIPE:BOUNDARY:hoist=42",
                    137, 5,
                    "C=113 S=1386"),
            pin("drr/6.34.1",
                    // v3.2 seat 8: 9_125 -> 9_122 = -3 - the only-exists enclosure read (ROnlyExistsElement.encloses): the pre-seat
                    // PARENT test admitted the generator's item-rooted ROOT twin of a two-or-more-hop `only exists` path inside a
                    // lambda (the twin hangs off the hop ABOVE the root, one level below the element - `terminationProvision ->
                    // earlyTerminationProvision -> optionalEarlyTermination only exists` and kin), a leak of the classifier's own law
                    // that the ancestor walk refuses. Value-identical under the 3-arg ExpressionOperators.onlyExists (it reads
                    // getMulti alone); transcribed from run B of sites-s8.ps1 (target/v32-seat8-instruments/scratch/
                    // sites-B-optimised.log, local); the per-site diff (sites-diff.txt) names every moved site: 34 over the ten
                    // drr cells, one class, none on any cdm cell or the chaos cell.
                    9_122, 117, 18, 414, 14, 167, 182, 2_946, 291, 428,
                    "SINGLE=40 SINGLE:meta=1 STREAM=1 TERMINAL_MULTI=1 VALUE:APPLY:DIRECT=99"
                            + " VALUE:BINARY_OP:BOUNDARY=9 VALUE:COLLECT_OP:BOUNDARY=2"
                            + " VALUE:CONDITIONAL:LADDER=78 VALUE:CONSTRUCT:DIRECT=2"
                            + " VALUE:CONVERSION:BOUNDARY=1 VALUE:DEFAULT_OP:BOUNDARY=7"
                            + " VALUE:EXISTENCE:BOUNDARY=5 VALUE:FIELD_ACCESS:BOUNDARY=41"
                            + " VALUE:LAMBDA_OP:BOUNDARY=35 VALUE:LIST_CONSTRUCT:BOUNDARY=1"
                            + " VALUE:LIST_CONSTRUCT:BOUNDARY:hoist=3 VALUE:LIST_OP:BOUNDARY=3"
                            + " VALUE:LIST_OP:DIRECT=16 VALUE:LIST_OP:DIRECT:hoist=3"
                            + " VALUE:MEMBERSHIP_OP:BOUNDARY=7 VALUE:META_ACCESS:BOUNDARY=15"
                            + " VALUE:PIPE:BOUNDARY:hoist=58",
                    127, 5,
                    "C=266 S=1811"),
            pin("drr/6.35.0",
                    // v3.2 seat 8: 9_121 -> 9_118 = -3 - the only-exists enclosure read (ROnlyExistsElement.encloses): the pre-seat
                    // PARENT test admitted the generator's item-rooted ROOT twin of a two-or-more-hop `only exists` path inside a
                    // lambda (the twin hangs off the hop ABOVE the root, one level below the element - `terminationProvision ->
                    // earlyTerminationProvision -> optionalEarlyTermination only exists` and kin), a leak of the classifier's own law
                    // that the ancestor walk refuses. Value-identical under the 3-arg ExpressionOperators.onlyExists (it reads
                    // getMulti alone); transcribed from run B of sites-s8.ps1 (target/v32-seat8-instruments/scratch/
                    // sites-B-optimised.log, local); the per-site diff (sites-diff.txt) names every moved site: 34 over the ten
                    // drr cells, one class, none on any cdm cell or the chaos cell.
                    9_118, 117, 18, 414, 14, 167, 180, 2_934, 291, 428,
                    "SINGLE=40 SINGLE:meta=1 STREAM=1 TERMINAL_MULTI=1 VALUE:APPLY:DIRECT=99"
                            + " VALUE:BINARY_OP:BOUNDARY=9 VALUE:COLLECT_OP:BOUNDARY=2"
                            + " VALUE:CONDITIONAL:LADDER=78 VALUE:CONSTRUCT:DIRECT=2"
                            + " VALUE:CONVERSION:BOUNDARY=1 VALUE:DEFAULT_OP:BOUNDARY=7"
                            + " VALUE:EXISTENCE:BOUNDARY=5 VALUE:FIELD_ACCESS:BOUNDARY=41"
                            + " VALUE:LAMBDA_OP:BOUNDARY=35 VALUE:LIST_CONSTRUCT:BOUNDARY=1"
                            + " VALUE:LIST_CONSTRUCT:BOUNDARY:hoist=3 VALUE:LIST_OP:BOUNDARY=3"
                            + " VALUE:LIST_OP:DIRECT=16 VALUE:LIST_OP:DIRECT:hoist=3"
                            + " VALUE:MEMBERSHIP_OP:BOUNDARY=7 VALUE:META_ACCESS:BOUNDARY=15"
                            + " VALUE:PIPE:BOUNDARY:hoist=58",
                    127, 5,
                    "C=266 S=1811"),
            pin("drr/6.36.0",
                    // v3.2 seat 8: 9_121 -> 9_118 = -3 - the only-exists enclosure read (ROnlyExistsElement.encloses): the pre-seat
                    // PARENT test admitted the generator's item-rooted ROOT twin of a two-or-more-hop `only exists` path inside a
                    // lambda (the twin hangs off the hop ABOVE the root, one level below the element - `terminationProvision ->
                    // earlyTerminationProvision -> optionalEarlyTermination only exists` and kin), a leak of the classifier's own law
                    // that the ancestor walk refuses. Value-identical under the 3-arg ExpressionOperators.onlyExists (it reads
                    // getMulti alone); transcribed from run B of sites-s8.ps1 (target/v32-seat8-instruments/scratch/
                    // sites-B-optimised.log, local); the per-site diff (sites-diff.txt) names every moved site: 34 over the ten
                    // drr cells, one class, none on any cdm cell or the chaos cell.
                    9_118, 117, 18, 414, 14, 167, 180, 2_934, 291, 428,
                    "SINGLE=40 SINGLE:meta=1 STREAM=1 TERMINAL_MULTI=1 VALUE:APPLY:DIRECT=99"
                            + " VALUE:BINARY_OP:BOUNDARY=9 VALUE:COLLECT_OP:BOUNDARY=2"
                            + " VALUE:CONDITIONAL:LADDER=78 VALUE:CONSTRUCT:DIRECT=2"
                            + " VALUE:CONVERSION:BOUNDARY=1 VALUE:DEFAULT_OP:BOUNDARY=7"
                            + " VALUE:EXISTENCE:BOUNDARY=5 VALUE:FIELD_ACCESS:BOUNDARY=41"
                            + " VALUE:LAMBDA_OP:BOUNDARY=35 VALUE:LIST_CONSTRUCT:BOUNDARY=1"
                            + " VALUE:LIST_CONSTRUCT:BOUNDARY:hoist=3 VALUE:LIST_OP:BOUNDARY=3"
                            + " VALUE:LIST_OP:DIRECT=16 VALUE:LIST_OP:DIRECT:hoist=3"
                            + " VALUE:MEMBERSHIP_OP:BOUNDARY=7 VALUE:META_ACCESS:BOUNDARY=15"
                            + " VALUE:PIPE:BOUNDARY:hoist=58",
                    127, 5,
                    "C=266 S=1811"),
            pin("drr/6.37.0",
                    // v3.2 seat 8: 9_122 -> 9_119 = -3 - the only-exists enclosure read (ROnlyExistsElement.encloses): the pre-seat
                    // PARENT test admitted the generator's item-rooted ROOT twin of a two-or-more-hop `only exists` path inside a
                    // lambda (the twin hangs off the hop ABOVE the root, one level below the element - `terminationProvision ->
                    // earlyTerminationProvision -> optionalEarlyTermination only exists` and kin), a leak of the classifier's own law
                    // that the ancestor walk refuses. Value-identical under the 3-arg ExpressionOperators.onlyExists (it reads
                    // getMulti alone); transcribed from run B of sites-s8.ps1 (target/v32-seat8-instruments/scratch/
                    // sites-B-optimised.log, local); the per-site diff (sites-diff.txt) names every moved site: 34 over the ten
                    // drr cells, one class, none on any cdm cell or the chaos cell.
                    9_119, 117, 18, 414, 14, 167, 180, 2_936, 291, 428,
                    "SINGLE=40 SINGLE:meta=1 STREAM=1 TERMINAL_MULTI=1 VALUE:APPLY:DIRECT=99"
                            + " VALUE:BINARY_OP:BOUNDARY=9 VALUE:COLLECT_OP:BOUNDARY=2"
                            + " VALUE:CONDITIONAL:LADDER=78 VALUE:CONSTRUCT:DIRECT=2"
                            + " VALUE:CONVERSION:BOUNDARY=1 VALUE:DEFAULT_OP:BOUNDARY=7"
                            + " VALUE:EXISTENCE:BOUNDARY=5 VALUE:FIELD_ACCESS:BOUNDARY=41"
                            + " VALUE:LAMBDA_OP:BOUNDARY=35 VALUE:LIST_CONSTRUCT:BOUNDARY=1"
                            + " VALUE:LIST_CONSTRUCT:BOUNDARY:hoist=3 VALUE:LIST_OP:BOUNDARY=3"
                            + " VALUE:LIST_OP:DIRECT=16 VALUE:LIST_OP:DIRECT:hoist=3"
                            + " VALUE:MEMBERSHIP_OP:BOUNDARY=7 VALUE:META_ACCESS:BOUNDARY=15"
                            + " VALUE:PIPE:BOUNDARY:hoist=58",
                    127, 5,
                    "C=266 S=1811"),
            pin("drr/6.38.0",
                    // v3.2 seat 8: 9_122 -> 9_119 = -3 - the only-exists enclosure read (ROnlyExistsElement.encloses): the pre-seat
                    // PARENT test admitted the generator's item-rooted ROOT twin of a two-or-more-hop `only exists` path inside a
                    // lambda (the twin hangs off the hop ABOVE the root, one level below the element - `terminationProvision ->
                    // earlyTerminationProvision -> optionalEarlyTermination only exists` and kin), a leak of the classifier's own law
                    // that the ancestor walk refuses. Value-identical under the 3-arg ExpressionOperators.onlyExists (it reads
                    // getMulti alone); transcribed from run B of sites-s8.ps1 (target/v32-seat8-instruments/scratch/
                    // sites-B-optimised.log, local); the per-site diff (sites-diff.txt) names every moved site: 34 over the ten
                    // drr cells, one class, none on any cdm cell or the chaos cell.
                    9_119, 117, 18, 414, 14, 167, 180, 2_936, 291, 428,
                    "SINGLE=40 SINGLE:meta=1 STREAM=1 TERMINAL_MULTI=1 VALUE:APPLY:DIRECT=99"
                            + " VALUE:BINARY_OP:BOUNDARY=9 VALUE:COLLECT_OP:BOUNDARY=2"
                            + " VALUE:CONDITIONAL:LADDER=78 VALUE:CONSTRUCT:DIRECT=2"
                            + " VALUE:CONVERSION:BOUNDARY=1 VALUE:DEFAULT_OP:BOUNDARY=7"
                            + " VALUE:EXISTENCE:BOUNDARY=5 VALUE:FIELD_ACCESS:BOUNDARY=41"
                            + " VALUE:LAMBDA_OP:BOUNDARY=35 VALUE:LIST_CONSTRUCT:BOUNDARY=1"
                            + " VALUE:LIST_CONSTRUCT:BOUNDARY:hoist=3 VALUE:LIST_OP:BOUNDARY=3"
                            + " VALUE:LIST_OP:DIRECT=16 VALUE:LIST_OP:DIRECT:hoist=3"
                            + " VALUE:MEMBERSHIP_OP:BOUNDARY=7 VALUE:META_ACCESS:BOUNDARY=15"
                            + " VALUE:PIPE:BOUNDARY:hoist=58",
                    127, 5,
                    "C=266 S=1811"),
            pin("drr/7.0.0",
                    // v3.2 seat 8: 9_001 -> 8_998 = -3 - the only-exists enclosure read (ROnlyExistsElement.encloses): the pre-seat
                    // PARENT test admitted the generator's item-rooted ROOT twin of a two-or-more-hop `only exists` path inside a
                    // lambda (the twin hangs off the hop ABOVE the root, one level below the element - `terminationProvision ->
                    // earlyTerminationProvision -> optionalEarlyTermination only exists` and kin), a leak of the classifier's own law
                    // that the ancestor walk refuses. Value-identical under the 3-arg ExpressionOperators.onlyExists (it reads
                    // getMulti alone); transcribed from run B of sites-s8.ps1 (target/v32-seat8-instruments/scratch/
                    // sites-B-optimised.log, local); the per-site diff (sites-diff.txt) names every moved site: 34 over the ten
                    // drr cells, one class, none on any cdm cell or the chaos cell.
                    8_998, 166, 3, 424, 15, 107, 117, 1_567, 236, 511,
                    "SINGLE=54 SINGLE:meta=1 STREAM:meta=1 TERMINAL_MULTI=5 VALUE:APPLY:DIRECT=120"
                            + " VALUE:BINARY_OP:BOUNDARY=10 VALUE:COLLECT_OP:BOUNDARY=2"
                            + " VALUE:CONDITIONAL:LADDER=73 VALUE:CONSTRUCT:DIRECT=4"
                            + " VALUE:CONVERSION:BOUNDARY=7 VALUE:DEFAULT_OP:BOUNDARY=2"
                            + " VALUE:DEFAULT_OP:BOUNDARY:hoist=1 VALUE:DEFAULT_OP:DIRECT=3"
                            + " VALUE:EXISTENCE:BOUNDARY=3 VALUE:FIELD_ACCESS:BOUNDARY=63"
                            + " VALUE:LAMBDA_OP:BOUNDARY=28 VALUE:LIST_CONSTRUCT:BOUNDARY=2"
                            + " VALUE:LIST_CONSTRUCT:BOUNDARY:hoist=2 VALUE:LIST_OP:BOUNDARY=4"
                            + " VALUE:LIST_OP:DIRECT=18 VALUE:LIST_OP:DIRECT:hoist=3 VALUE:LITERAL:DIRECT=4"
                            + " VALUE:MEMBERSHIP_OP:BOUNDARY=7 VALUE:META_ACCESS:BOUNDARY=11"
                            + " VALUE:PIPE:BOUNDARY:hoist=83",
                    133, 7,
                    "C=407 S=1850"),
            pin("drr/7.1.0",
                    // v3.2 seat 8: 9_002 -> 8_999 = -3 - the only-exists enclosure read (ROnlyExistsElement.encloses): the pre-seat
                    // PARENT test admitted the generator's item-rooted ROOT twin of a two-or-more-hop `only exists` path inside a
                    // lambda (the twin hangs off the hop ABOVE the root, one level below the element - `terminationProvision ->
                    // earlyTerminationProvision -> optionalEarlyTermination only exists` and kin), a leak of the classifier's own law
                    // that the ancestor walk refuses. Value-identical under the 3-arg ExpressionOperators.onlyExists (it reads
                    // getMulti alone); transcribed from run B of sites-s8.ps1 (target/v32-seat8-instruments/scratch/
                    // sites-B-optimised.log, local); the per-site diff (sites-diff.txt) names every moved site: 34 over the ten
                    // drr cells, one class, none on any cdm cell or the chaos cell.
                    8_999, 166, 3, 424, 15, 107, 117, 1_569, 236, 511,
                    "SINGLE=54 SINGLE:meta=1 STREAM:meta=1 TERMINAL_MULTI=5 VALUE:APPLY:DIRECT=120"
                            + " VALUE:BINARY_OP:BOUNDARY=10 VALUE:COLLECT_OP:BOUNDARY=2"
                            + " VALUE:CONDITIONAL:LADDER=73 VALUE:CONSTRUCT:DIRECT=4"
                            + " VALUE:CONVERSION:BOUNDARY=7 VALUE:DEFAULT_OP:BOUNDARY=2"
                            + " VALUE:DEFAULT_OP:BOUNDARY:hoist=1 VALUE:DEFAULT_OP:DIRECT=3"
                            + " VALUE:EXISTENCE:BOUNDARY=3 VALUE:FIELD_ACCESS:BOUNDARY=63"
                            + " VALUE:LAMBDA_OP:BOUNDARY=28 VALUE:LIST_CONSTRUCT:BOUNDARY=2"
                            + " VALUE:LIST_CONSTRUCT:BOUNDARY:hoist=2 VALUE:LIST_OP:BOUNDARY=4"
                            + " VALUE:LIST_OP:DIRECT=18 VALUE:LIST_OP:DIRECT:hoist=3 VALUE:LITERAL:DIRECT=4"
                            + " VALUE:MEMBERSHIP_OP:BOUNDARY=7 VALUE:META_ACCESS:BOUNDARY=11"
                            + " VALUE:PIPE:BOUNDARY:hoist=83",
                    133, 7,
                    "C=407 S=1850"),
            pin("drr/7.2.0",
                    // v3.2 seat 8: 9_002 -> 8_999 = -3 - the only-exists enclosure read (ROnlyExistsElement.encloses): the pre-seat
                    // PARENT test admitted the generator's item-rooted ROOT twin of a two-or-more-hop `only exists` path inside a
                    // lambda (the twin hangs off the hop ABOVE the root, one level below the element - `terminationProvision ->
                    // earlyTerminationProvision -> optionalEarlyTermination only exists` and kin), a leak of the classifier's own law
                    // that the ancestor walk refuses. Value-identical under the 3-arg ExpressionOperators.onlyExists (it reads
                    // getMulti alone); transcribed from run B of sites-s8.ps1 (target/v32-seat8-instruments/scratch/
                    // sites-B-optimised.log, local); the per-site diff (sites-diff.txt) names every moved site: 34 over the ten
                    // drr cells, one class, none on any cdm cell or the chaos cell.
                    8_999, 166, 3, 424, 15, 107, 117, 1_569, 236, 511,
                    "SINGLE=54 SINGLE:meta=1 STREAM:meta=1 TERMINAL_MULTI=5 VALUE:APPLY:DIRECT=120"
                            + " VALUE:BINARY_OP:BOUNDARY=10 VALUE:COLLECT_OP:BOUNDARY=2"
                            + " VALUE:CONDITIONAL:LADDER=73 VALUE:CONSTRUCT:DIRECT=4"
                            + " VALUE:CONVERSION:BOUNDARY=7 VALUE:DEFAULT_OP:BOUNDARY=2"
                            + " VALUE:DEFAULT_OP:BOUNDARY:hoist=1 VALUE:DEFAULT_OP:DIRECT=3"
                            + " VALUE:EXISTENCE:BOUNDARY=3 VALUE:FIELD_ACCESS:BOUNDARY=63"
                            + " VALUE:LAMBDA_OP:BOUNDARY=28 VALUE:LIST_CONSTRUCT:BOUNDARY=2"
                            + " VALUE:LIST_CONSTRUCT:BOUNDARY:hoist=2 VALUE:LIST_OP:BOUNDARY=4"
                            + " VALUE:LIST_OP:DIRECT=18 VALUE:LIST_OP:DIRECT:hoist=3 VALUE:LITERAL:DIRECT=4"
                            + " VALUE:MEMBERSHIP_OP:BOUNDARY=7 VALUE:META_ACCESS:BOUNDARY=11"
                            + " VALUE:PIPE:BOUNDARY:hoist=83",
                    133, 7,
                    "C=407 S=1850"),
            pin("drr/7.3.0",
                    // v3.2 seat 8: 9_002 -> 8_999 = -3 - the only-exists enclosure read (ROnlyExistsElement.encloses): the pre-seat
                    // PARENT test admitted the generator's item-rooted ROOT twin of a two-or-more-hop `only exists` path inside a
                    // lambda (the twin hangs off the hop ABOVE the root, one level below the element - `terminationProvision ->
                    // earlyTerminationProvision -> optionalEarlyTermination only exists` and kin), a leak of the classifier's own law
                    // that the ancestor walk refuses. Value-identical under the 3-arg ExpressionOperators.onlyExists (it reads
                    // getMulti alone); transcribed from run B of sites-s8.ps1 (target/v32-seat8-instruments/scratch/
                    // sites-B-optimised.log, local); the per-site diff (sites-diff.txt) names every moved site: 34 over the ten
                    // drr cells, one class, none on any cdm cell or the chaos cell.
                    8_999, 166, 3, 424, 15, 107, 117, 1_569, 236, 511,
                    "SINGLE=54 SINGLE:meta=1 STREAM:meta=1 TERMINAL_MULTI=5 VALUE:APPLY:DIRECT=120"
                            + " VALUE:BINARY_OP:BOUNDARY=10 VALUE:COLLECT_OP:BOUNDARY=2"
                            + " VALUE:CONDITIONAL:LADDER=73 VALUE:CONSTRUCT:DIRECT=4"
                            + " VALUE:CONVERSION:BOUNDARY=7 VALUE:DEFAULT_OP:BOUNDARY=2"
                            + " VALUE:DEFAULT_OP:BOUNDARY:hoist=1 VALUE:DEFAULT_OP:DIRECT=3"
                            + " VALUE:EXISTENCE:BOUNDARY=3 VALUE:FIELD_ACCESS:BOUNDARY=63"
                            + " VALUE:LAMBDA_OP:BOUNDARY=28 VALUE:LIST_CONSTRUCT:BOUNDARY=2"
                            + " VALUE:LIST_CONSTRUCT:BOUNDARY:hoist=2 VALUE:LIST_OP:BOUNDARY=4"
                            + " VALUE:LIST_OP:DIRECT=18 VALUE:LIST_OP:DIRECT:hoist=3 VALUE:LITERAL:DIRECT=4"
                            + " VALUE:MEMBERSHIP_OP:BOUNDARY=7 VALUE:META_ACCESS:BOUNDARY=11"
                            + " VALUE:PIPE:BOUNDARY:hoist=83",
                    133, 7,
                    "C=407 S=1850"),
            // v3.2 PR-2: the chaos cell's row, transcribed from the 2026-09-02
            // -Doptnav.reportOnly measure at the PR-2 head (never typed by hand).
            // itemHarvestBails=12 = rootTypeMissing ×12: the s15 Void-mapped carrier
            // (census F8) denies the harvest its root type on every variant.
            // v3.2 seat 10 (chaos-1.1.0, D49): the cell REPLACED whole (charter § 9) - the row re-pinned
            // from the -Doptnav.reportOnly=true measure over the gen-2 cell at the swap (fills-f4,
            // target/v32-seat10-instruments/scratch/optnav-report-f4.log, local; never typed): 1,460
            // conversions over 1,241 function files + 308 rule files, 53 + 1 twin-type-missing fallbacks
            // (the fresh families' Void-typed and alias-typed twins), 12 item-interior suppressions and
            // 12 harvest bails (the s15 carrier, unmoved), 688 alias seams flipped over 15 forms, leaf
            // wraps C=353 S=526.
            // --- chaos-1.0.0 HISTORY (the retired cell; the seat-2 / seat-7 narration of the 1.0.0 tuple, fenced
            // --- here at round 1 - cq SF-5 - so the 1.1.0 figures below carry no 1.0.0 narration beside them) ---
            // | v3.2 seat 2: the chaos cell 435 -> 461 = +26, exactly the C5Forms
            // | fallback/lambdaIte members whose witness refusals fell (the map's
            // | tranche partition T3=347 / T4=126 UNMOVED - those 26 were always in T3/T4;
            // | only the emission's refusal kept them out of the flipped count).
            // | v3.2 seat 7 (F11): the chaos cell 461 -> 473 = +12 - OPTNAV-ALIAS seamsFlipped, the MOVING
            // | figure, with VALUE:LAMBDA_OP:BOUNDARY 64 -> 76 = +12 - transcribed from the seat's fix print
            // | (target/v32-seat7-instruments/scratch/ov-optimised.log, local). The twelve C18ToKind `pulled`
            // | aliases (a lambda-op seam, one per placement variant) were ALREADY map rows: ALIASMAP rows is
            // | UNMOVED at 473 since #627 (target/v32-seat6-instruments/scratch/s6b-t-optimised.log) and the
            // | 24 `sc|` alias-law exception rows law-checked both aliases while the function file was
            // | missing; what moved is their entry into the FLIPPED population as the F11 refusals fell (the
            // | seat-2 form above; the tranche partition T3=347 / T4=126 UNMOVED). Round 1: the first cut
            // | named `rows` as the moving figure and the aliases as "entering the map" - the spec review's
            // | MF-1, refuted by the seat-6 print.
            // | (the leaf-wrap channel's 1.0.0 narration:)
            // | v3.2 seat 2: C 320 -> 281 = -65 + 26, DECODED per member by the instrumented
            // | seam query at the seat-1 head and at this head: C5Forms.vals 130 -> 65 (the five
            // | `vals then ...` bases were compiled TWICE per variant pre-seat - the alias then-
            // | hoist attempt's sink compile, then the inline re-render after the guard declined
            // | it - and once now that the hoist lands) and C5Forms.fallback / lambdaIte +13 each
            // | (the two formerly-refused members now flip and count at their invocation seats).
            // | Every other member unmoved; S unmoved at 347.
            // | v3.2 seat 7 (F11): C 281 -> 317 = +36 at the same print, S unmoved at 347. The per-member split
            // | is NOT decoded at this seat (round 1, the spec review's SF-1): the seam-form histogram moves at
            // | VALUE:LAMBDA_OP:BOUNDARY alone (64 -> 76), so `switched` did not join the flipped seam set and
            // | the +36's composition is a HYPOTHESIS (the twelve C18ToKind functions' C-channel seams counting
            // | at their invocation seats now that the functions emit) until the seat-2 instrument decodes it
            // | (target/v32-seat2-instruments/probe-leafwraps.py, the [S2LEAF] print per member).
            // --- end of the chaos-1.0.0 history ---
            pin("chaos/1.1.0",
                    // (chaos-1.0.0's row at #630: 569, 0, 0, 0, 0, 0, 0, 0, 12, 473 and the ten seam forms
                    // COLLECT_OP 36 / CONDITIONAL 39 / CONVERSION 26 / DEFAULT_OP 26 / LAMBDA_OP 76 /
                    // LIST_OP 13 + 13 / LITERAL 13 / MEMBERSHIP_OP 13 / PIPE:hoist 218, leaf wraps C=317 S=347.)
                    // v3.2 seat 12 (D52, COUNTERS FIRST - read from the third run at the counter tree,
                    // target/v32-seat12-instruments/scratch/optnav-chaos-pins-c3pre3.txt, local): the navigation
                    // channel UNMOVED (conversions 1460, fallbacks 53 / 1, declines 0, item 12 / 12); the alias-seam
                    // channel 688 -> 601 (-87: BINARY_OP:BOUNDARY 23 -> 11, COLLECT_OP:BOUNDARY 63 -> 36,
                    // LAMBDA_OP:BOUNDARY 103 -> 76, PIPE:BOUNDARY:hoist 264 -> 255, SWITCH_OP:BOUNDARY 12 -> 0)
                    // and the leaf wraps C 353 -> 317 / S 526 -> 475 - the seams of the FUNCTION files the three register
                    // sites now REFUSE on this route as on the default one (the D11's FUNCTION LOUD line at the same
                    // tree: SWITCH_TERNARY_STUB 42, DEEP_PATH_UTIL_UNRESOLVED 12, ALIAS_SIGNATURE_RAW_TYPE 52 - one
                    // refusal per file at its first site) leave the walk; the per-site split of the -87 is NOT decoded
                    // here (the seat-2 instrument's [S2LEAF] print would name it).
                    // v3.2 seat 13 (D53, commit 4 - read from the overlay run's own chaos lines,
                    // target/v32-seat13-instruments/scratch/optnav-chaos-pins-c4k.txt, local): the navigation
                    // channel MOVED for the first time since #630 - conversions 1460 -> 1330 (-130); twin-type fallbacks 53 -> 27 (-26) -
                    // the walk sites and fallbacks of the s24 FUNCTION files the four register sites now REFUSE on
                    // this route as on the default one (the D11's FUNCTION LOUD line at the commit-4 tree: WITH_META_UNTYPED_STUB 26, VOID_INTO_META_OUTPUT 13, INLINE_CONDITIONAL_TERNARY 12, HOIST_ARM_COERCION_DROPPED 25 -
                    // one refusal per file at its first site; the shortfall map 56 -> 134 names the same files),
                    // leave the walk; the alias-seam channel 601 -> 601 (+0: APPLY:DIRECT 13 -> 26, APPLY:DIRECT:hoist 13 -> 0) and the leaf wraps C 317 -> 317 /
                    // S 475 -> 475. Item 12 / 12.
                    1330, 27, 1, 0, 0, 0, 0, 12, 12, 601,
                    "VALUE:APPLY:DIRECT=26 VALUE:BINARY_OP:BOUNDARY=11 VALUE:COLLECT_OP:BOUNDARY=36"
                            + " VALUE:CONDITIONAL:LADDER=39 VALUE:CONVERSION:BOUNDARY=26"
                            + " VALUE:DEFAULT_OP:BOUNDARY=28 VALUE:DEFAULT_OP:DIRECT=13 VALUE:LAMBDA_OP:BOUNDARY=76"
                            + " VALUE:LIST_OP:BOUNDARY=26 VALUE:LIST_OP:DIRECT=13 VALUE:LITERAL:DIRECT=39"
                            + " VALUE:MEMBERSHIP_OP:BOUNDARY=13 VALUE:PIPE:BOUNDARY:hoist=255",
                    0, 0,
                    "C=317 S=475"));

    /**
     * DIAGNOSTIC MODE ({@code -Doptnav.reportOnly=true}): the conservation LAWS
     * (shortfall/bailed/distinct) still assert, but the pinned-VALUE asserts are
     * skipped so one run prints every cell's actuals — the census-recut instrument
     * for an emission-scope change (a re-price run measures first, then pins).
     * Default OFF: the pins enforce.
     */
    private static final boolean REPORT_ONLY = Boolean.getBoolean("optnav.reportOnly");

    /**
     * THE PER-SITE INSTRUMENT (v3.2 seat 8): under {@code -Doptnav.dump-sites=<dir>} every
     * converted site of every cell — the landed pools AND the alias-root pool — is written to
     * {@code <dir>/<cell leaf>.tsv} as a sorted MULTISET (a count per distinct row): the
     * nearest RANGED ancestor's file and byte offsets (a render-time twin carries no range of
     * its own — the depth column says how far up the range was found), the site's spelling
     * (its receiver chain down to the root), the plan's shape / root kind / step count / meta
     * bit, the parent's kind, whether an only-exists element ENCLOSES the site (the ancestor
     * walk written out in {@link #enclosedByOnlyExists}, so the instrument reads the same at a
     * content without the parser's own {@code ROnlyExistsElement.encloses} — the two reads are
     * asserted equal at every node of the guard suite's fixture,
     * {@code OnlyExistsGuardSeatTest}) and whether the parent IS the element, and the
     * enclosing function or rule. Two runs at two contents, diffed row for row, NAME the sites
     * whose conversion verdict moved — the fact the conversion-event pin counts but cannot
     * name (the seat's drr/5.61.0 6,218 → 6,211). Default OFF: nothing is written, no print.
     */
    private static final String DUMP_SITES_DIR = System.getProperty("optnav.dump-sites");

    private static void dumpConvertedSites(CorpusCells.CellSpec cell,
            OptimisedExpressionCompiler compiler, CorpusCells.LoadedCell loaded)
            throws IOException {
        java.util.TreeMap<String, Integer> rows = new java.util.TreeMap<>();
        NavigationChainClassifier classifier = new NavigationChainClassifier();
        for (RExpression site : compiler.convertedSiteSet()) {
            rows.merge(siteRow("landed", site, classifier, loaded), 1, Integer::sum);
        }
        for (RExpression site : compiler.convertedAliasRootSiteSet()) {
            rows.merge(siteRow("aliasRoot", site, classifier, loaded), 1, Integer::sum);
        }
        String leaf = cell.ownDir().substring(cell.ownDir().lastIndexOf('/') + 1);
        java.nio.file.Path dir = java.nio.file.Path.of(DUMP_SITES_DIR);
        java.nio.file.Files.createDirectories(dir);
        StringBuilder sb = new StringBuilder();
        sb.append("count\tpool\tfile\tstart\tend\trangedKind\tdepth\texprKind\tspelling"
                + "\tshape\trootKind\tsteps\tmeta\tparentKind\tenclosedByOnlyExists"
                + "\tparentIsOnlyExists\towner\n");
        int events = 0;
        for (Map.Entry<String, Integer> e : rows.entrySet()) {
            sb.append(e.getValue()).append('\t').append(e.getKey()).append('\n');
            events += e.getValue();
        }
        java.nio.file.Path out = dir.resolve(leaf + ".tsv");
        java.nio.file.Files.writeString(out, sb.toString(),
                java.nio.charset.StandardCharsets.UTF_8);
        System.out.println("OPTNAV-SITES " + cell.label() + " rows=" + rows.size()
                + " events=" + events + " -> " + out.toAbsolutePath());
    }

    /**
     * The instrument's OWN ancestor walk to an only-exists element — deliberately not a call
     * to {@code ROnlyExistsElement.encloses}, so the per-site dump reads the same at a content
     * that predates the declaration (run A of the seat's two-content measurement did). Unbounded
     * by design (the parser's read is bounded at 64); the guard suite asserts the two agree at
     * every node of its fixture (v3.2 seat 8 round 1, the code-quality NIT-9).
     */
    static boolean enclosedByOnlyExists(RNode site) {
        for (RNode up = site == null ? null : site.parent(); up != null; up = up.parent()) {
            if (up instanceof ROnlyExistsElement) {
                return true;
            }
        }
        return false;
    }

    private static String siteRow(String pool, RExpression site,
            NavigationChainClassifier classifier, CorpusCells.LoadedCell loaded) {
        RNode ranged = site;
        int depth = 0;
        while (ranged != null && !ranged.sourceRange().hasByteOffsets()) {
            ranged = ranged.parent();
            depth++;
        }
        String file = ranged == null ? "<none>" : ranged.sourceRange().file();
        String start = ranged == null ? "-" : String.valueOf(ranged.sourceRange().startOffset());
        String end = ranged == null ? "-" : String.valueOf(ranged.sourceRange().endOffset());
        String rangedKind = ranged == null ? "<none>" : ranged.getClass().getSimpleName();
        // the plan re-taken on the site (the 2-arg classify finds the enclosing function
        // itself); the alias-root pool needs the emitter's admission channel, so its plan
        // is n/a here — the pool column carries the fact instead
        NavigationChainClassifier.LadderPlan plan = "landed".equals(pool)
                ? classifier.classify(site, loaded.workspace()).orElse(null) : null;
        String shape = plan == null ? "n/a" : plan.shape().name();
        String rootKind = plan == null ? "n/a" : plan.rootKind().name();
        String steps = plan == null ? "n/a" : String.valueOf(plan.accessors().size());
        String meta = plan == null ? "n/a" : String.valueOf(plan.bearsMeta());
        RNode parent = site.parent();
        String parentKind = parent == null ? "<none>" : parent.getClass().getSimpleName();
        boolean enclosed = enclosedByOnlyExists(site);
        boolean parentIsElement = parent instanceof ROnlyExistsElement;
        String owner = "<none>";
        for (RNode up = site; up != null; up = up.parent()) {
            if (up instanceof RFunction f) {
                owner = "fn:" + f.name();
                break;
            }
            if (up instanceof RRule r) {
                owner = "rule:" + r.name();
                break;
            }
        }
        return String.join("\t", pool, file, start, end, rangedKind, String.valueOf(depth),
                site.getClass().getSimpleName(), spell(site), shape, rootKind, steps, meta,
                parentKind, String.valueOf(enclosed), String.valueOf(parentIsElement), owner);
    }

    private static String spell(RExpression e) {
        if (e instanceof RFeatureCall fc) {
            return spell(fc.receiver()) + " -> " + fc.featureName();
        }
        if (e instanceof RDeepFeatureCall dfc) {
            return spell(dfc.receiver()) + " ->> " + dfc.featureName();
        }
        if (e instanceof RSymbolReference sr) {
            return "sym:" + sr.name();
        }
        if (e instanceof RImplicitVariable iv) {
            return iv.isSynthetic() ? "item(synthetic)" : "item";
        }
        if (e instanceof REnumValueRef evr) {
            return "evr:" + evr.enumName() + " -> " + evr.valueName();
        }
        return e == null ? "<null>" : e.getClass().getSimpleName();
    }

    @Test
    void conversionLawsHoldPerCell() throws IOException {
        Assumptions.assumeTrue(CorpusCells.corpusStaged(),
                "test-corpus not staged — skipped");
        Assumptions.assumeTrue(!CorpusCells.resolveBuiltinFiles().isEmpty(),
                "rune-dsl builtins absent — skipped");

        // Every pin row must name a cell of the SOT (a row outliving its cell would
        // enforce nothing); checked after the walk, which visits every cell.
        List<String> pinnedButAbsent = new ArrayList<>(PINS.keySet());
        List<String> lawFindings = new ArrayList<>();
        // THE STAGED-CELL LAW (the review's catch on the 5 -> 25 blast radius): an
        // un-staged cell is COLLECTED and failed after the walk — never a mid-loop
        // assume, which would abort the method as SKIPPED and drop every later cell
        // and the pinned-but-absent check with it (the silent narrowing the loud
        // pin law below exists to forbid).
        List<String> unstaged = new ArrayList<>();
        for (CorpusCells.CellSpec cell : CorpusCells.cellsUnderTest()) {
            if (!CorpusCells.staged(cell)) {
                unstaged.add(cell.label() + " (" + cell.ownDir() + ")");
                continue;
            }
            pinnedButAbsent.remove(cell.label());

            CorpusCells.LoadedCell loaded = CorpusCells.load(cell);
            // THE LOUD PIN LAW (PR #607): a cell is skipped ONLY on the measured fact
            // that its own models carry neither functions nor rules (iso20022 /
            // rune-fpml — the census's empty FUNCTION face); a function-bearing cell
            // without a pin row FAILS rather than dropping out of the gate silently
            // (the pre-#607 `continue` on a missing pin covered the three ring cells
            // and would have let every band cell pass unmeasured).
            int ownFunctions = 0;
            int ownRules = 0;
            for (RModel model : loaded.ownModels()) {
                for (RRootElement element : model.rootElements()) {
                    if (element instanceof RFunction) {
                        ownFunctions++;
                    } else if (element instanceof RRule) {
                        ownRules++;
                    }
                }
            }
            CellPins pins = PINS.get(cell.label());
            if (ownFunctions == 0 && ownRules == 0) {
                assertTrue(pins == null, cell.label()
                        + ": a cell with no functions and no rules must carry no pin row");
                System.out.println("OPTNAV " + cell.label()
                        + " skipped: the cell's own models carry no functions and no rules");
                continue;
            }
            if (!REPORT_ONLY) {
                final int fnCount = ownFunctions;
                final int ruleCount = ownRules;
                assertTrue(pins != null, () -> cell.label() + ": a function-bearing cell ("
                        + fnCount + " functions, " + ruleCount + " rules) has NO pin row —"
                        + " measure it with -Doptnav.reportOnly=true and transcribe its"
                        + " OPTNAV / OPTNAV-ALIAS lines into PINS (never type a pin by hand)");
            }

            GeneratorModel gm = new GeneratorModel(loaded.workspace(),
                    loaded.ownModelSet()::contains);
            OptimisedFunctionGenerator funcGen =
                    new OptimisedFunctionGenerator(gm, TYPE_TRANSLATOR, TYPE_UTIL);

            Map<String, String> output = new LinkedHashMap<>();
            List<GenerationException> errors = funcGen.generateWithErrors(output);
            // The chaos cell (v3.2 PR-2): a refusal whose attributed target path is a
            // DECLARED row of the D11 chaos expected-divergence baseline (the ONE
            // declared-red set, read cross-module by path — this module cannot see
            // rune-java-generator's test scope) is tolerated-but-counted on BOTH
            // routes (LAW 77: the optimised route refuses the same s18 seats the
            // default route refuses); an undeclared refusal fails exactly as before.
            List<GenerationException> undeclared = errors;
            if (cell.label().startsWith("chaos/")) {
                java.util.Set<String> declared = chaosDeclaredDivergenceRows();
                undeclared = errors.stream()
                        .filter(e -> e.getTargetPath() == null
                                || !declared.contains(e.getTargetPath()))
                        .toList();
                int tolerated = errors.size() - undeclared.size();
                // EQUALITY-pinned (the code-quality review's MF-1: the baseline's 354
                // paths licence far more than the refusal class, and this PR's own
                // target-path attribution would let a NEW route-only refusal at any
                // declared-mismatch path slip into "tolerated"): the chaos cell
                // refuses EXACTLY the 12 s18 TYPE_SWITCH_TERNARY_STUB seats (census
                // F11) on this route as on the default one (LAW 77). Movement EITHER
                // way is loud — down when the F11 seat lands (then delete this pin),
                // up is a new optimised-route refusal to census.
                // v3.2 seat 3 (F12): 12 -> 24 at the COUNTER commit (the 12 F11 refusals + the
                // 12 FUNCTION_NOT_EMITTED refusals of the twelve C4Speed dispatch groups whose
                // file the bare-name grouping dropped — FunctionGenerator's accounting pass,
                // inherited by this route's generator, LAW 77) and back to 12 at the FIX commit:
                // the per-file grouping emits the twelve files, the accounting pass reads zero.
                // v3.2 seat 7 (F11): 12 -> 0 at the FIX commit - the twelve C18ToKind functions EMIT on this route as
                // on the default one (LAW 77; the seat's chaos D11 both routes at the fix content:
                // TYPE_SWITCH_TERNARY_STUB=0). The pin stays at ZERO as the belt: a rise is a NEW refusal to census.
                // v3.2 seat 9 (F13 / D48): 0 -> 1 at the D48 commit - the ONE function-kind element refusal of the
                // chaos s17 rival-enum split, p2's C17Sift, refused at ENUM_VALUE_NAME_ECHO on this route as on the
                // default one (LAW 77: the IR adapter declines the shape to the default render, the ONE site); the
                // split's other refusal, the data rule C17BooksC17Agree, is not a function and never reaches this
                // generator. Both are the baseline's two remaining rows (the REFUSAL rows; the noGolden class moved
                // to the golden-free register). A rise is a new refusal to census, a fall a heal to re-pin.
                // v3.2 seat 10 (chaos-1.1.0, D49): 1 -> 25 at the gen-2 swap - the function-kind refusals of the FIRST
                // gen-2 census on this route as on the default one (LAW 77), every one a DECLARED row of the re-derived
                // baseline: the s26 LITERAL_SWITCH_TERNARY_STUB seats, the rival-enum splits' ENUM_VALUE_NAME_ECHO
                // functions (s17 x36 + its A1xA6 x16 twin, s31's pair), the band's two generator-refused functions
                // (s93 TYPE_COLLAPSED_TO_OBJECT, s94 TYPE_SWITCH_TERNARY_STUB); the paths are PRINTED below at every
                // run (the [OPTNAV-REFUSED] lines), so the composition is read from the log, never typed. SHRINK-ONLY
                // from here: the second round's heals re-pin it down, a rise is a NEW optimised-route refusal.
                // Round 1 (cq SF-1): the SET, not the count (LAW 73) — a count-equality pin over a 611-path
                // licence would let one declared refusal heal while a NEW refusal at another declared path
                // appeared, same count, green; the tolerated paths are asserted against the declared set now.
                java.util.TreeSet<String> toleratedPaths = new java.util.TreeSet<>();
                for (GenerationException e : errors) {
                    if (e.getTargetPath() != null && declared.contains(e.getTargetPath())) {
                        System.out.println("[OPTNAV-REFUSED] " + cell.label() + " " + e.getTargetPath());
                        toleratedPaths.add(e.getTargetPath());
                    }
                }
                assertEquals(CHAOS_DECLARED_FUNCTION_FACE_REFUSALS, toleratedPaths, cell.label()
                        + ": the optimised route's tolerated function-face refusals must equal the"
                        + " declared SET exactly (a missing member is a heal to re-pin, an extra one"
                        + " a NEW refusal to census — never to absorb)");
                assertEquals(CHAOS_DECLARED_FUNCTION_FACE_REFUSALS.size(), tolerated, cell.label()
                        + ": one tolerated refusal per declared path");
                System.out.println("OPTNAV " + cell.label() + ": " + tolerated
                        + " generation refusal(s) tolerated as DECLARED rows of the"
                        + " chaos expected-divergence baseline (equality-pinned, shrink-only)");
            }
            final List<GenerationException> failing = undeclared;
            assertTrue(failing.isEmpty(), () -> cell.label() + ": generation errors — "
                    + failing.stream().map(Throwable::getMessage).toList());

            OptimisedExpressionCompiler compiler = funcGen.optimisedExpressionCompiler();

            // THE FAMILY-3 RULE-FACE RENDER (census § 12b): rules + reports render
            // through the SAME funcGen (the buildClassWithBaseInterface window
            // bracket + owner bridge) — rule conversions accumulate on the shared
            // compiler; the REPORT face must convert ZERO sites BY CONSTRUCTION
            // (synthesized rule-invocation refs only), asserted on the full
            // counter tuple around its run.
            Map<String, String> ruleOutput = new LinkedHashMap<>();
            Map<String, String> reportOutput = new LinkedHashMap<>();
            List<GenerationException> ruleFaceErrors = new ArrayList<>();
            RuleGenerator ruleGen = new RuleGenerator(gm, TYPE_TRANSLATOR, funcGen);
            ReportGenerator reportGen = new ReportGenerator(gm, TYPE_TRANSLATOR, funcGen);
            for (RModel model : loaded.ownModels()) {
                ruleFaceErrors.addAll(
                        ruleGen.generateClasses(model, gm.version(model), ruleOutput));
            }
            long tupleBeforeReports = conversionTuple(compiler);
            for (RModel model : loaded.ownModels()) {
                ruleFaceErrors.addAll(
                        reportGen.generateClasses(model, gm.version(model), reportOutput));
            }
            long tupleAfterReports = conversionTuple(compiler);
            System.out.println("OPTNAV " + cell.label() + " reportFace: files="
                    + reportOutput.size() + " conversionTupleDelta="
                    + (tupleAfterReports - tupleBeforeReports));
            assertEquals(tupleBeforeReports, tupleAfterReports,
                    cell.label() + ": the REPORT face must convert/decline/bail ZERO"
                            + " sites — its operations are synthesized rule-invocation"
                            + " refs (census § 12a), so any movement here is a"
                            + " predicate or synthesis change");
            // v3.2 seat 10 (chaos-1.1.0, D49): the RULE face refuses on this route exactly what the default
            // route refuses under the POJO kind (LAW 77) - 23 = the s26 type-keyed switch inside a reporting
            // rule (C26PickAvRule, TYPE_SWITCH_TERNARY_STUB - the v3.1 class at a NEW seat, 11) + the s28
            // report `with type` naming a CHOICE (C28ALTC28RegReportFunction, REPORT_REFERENCE_UNRESOLVED -
            // the seat-4 banked group, 12; its label providers are not a face of this route), every one a
            // row of the re-derived baseline: a declared row is tolerated-but-counted here as on the
            // function face, EQUALITY-pinned and printed ([OPTNAV-REFUSED-RULEFACE], the fills-f3 print);
            // an undeclared rule-face refusal fails exactly as before. SHRINK-ONLY from here.
            List<GenerationException> undeclaredRuleFace = ruleFaceErrors;
            if (cell.label().startsWith("chaos/")) {
                java.util.Set<String> declared = chaosDeclaredDivergenceRows();
                undeclaredRuleFace = ruleFaceErrors.stream()
                        .filter(e -> e.getTargetPath() == null || !declared.contains(e.getTargetPath()))
                        .toList();
                int toleratedRuleFace = ruleFaceErrors.size() - undeclaredRuleFace.size();
                java.util.TreeSet<String> toleratedRuleFacePaths = new java.util.TreeSet<>();
                for (GenerationException e : ruleFaceErrors) {
                    if (e.getTargetPath() != null && declared.contains(e.getTargetPath())) {
                        System.out.println("[OPTNAV-REFUSED-RULEFACE] " + cell.label() + " " + e.getTargetPath());
                        toleratedRuleFacePaths.add(e.getTargetPath());
                    }
                }
                // Round 1 (cq SF-1): the SET (LAW 73), seeded from the fills-f5 print.
                assertEquals(CHAOS_DECLARED_RULE_FACE_REFUSALS, toleratedRuleFacePaths, cell.label()
                        + ": the optimised route's tolerated rule-face refusals must equal the declared"
                        + " SET exactly (a missing member is a heal to re-pin, an extra one a NEW"
                        + " refusal to census — never to absorb)");
                assertEquals(CHAOS_DECLARED_RULE_FACE_REFUSALS.size(), toleratedRuleFace, cell.label()
                        + ": one tolerated rule-face refusal per declared path");
            }
            List<GenerationException> undeclaredRuleFaceFinal = undeclaredRuleFace;
            assertTrue(undeclaredRuleFaceFinal.isEmpty(), () -> cell.label()
                    + ": rule-face generation errors — "
                    + undeclaredRuleFaceFinal.stream().map(Throwable::getMessage).toList());

            Set<RExpression> walkSites = operationSiteWalk(loaded);
            List<RExpression> shortfall = new ArrayList<>();
            for (RExpression site : walkSites) {
                if (!compiler.convertedSiteSet().contains(site)) {
                    shortfall.add(site);
                }
            }

            // THE § 6.3 T1 ALIAS-ROOT COMPLETENESS LAW: every walk-visible
            // ALIAS_CALL-rooted SINGLE/TERMINAL_MULTI chain of a FLIPPED alias
            // at an operation seat converts to the alias-rooted ladder — the
            // same identity-set law as the landed pools, over the admission
            // walk (the policy instance IS the emitter's own).
            AliasValueSeamPolicy seamPolicy = compiler.aliasValueSeamPolicy();
            Set<RExpression> aliasRootWalkSites = aliasRootSiteWalk(loaded, seamPolicy);
            List<RExpression> aliasRootShortfall = new ArrayList<>();
            for (RExpression site : aliasRootWalkSites) {
                if (!compiler.convertedAliasRootSiteSet().contains(site)) {
                    aliasRootShortfall.add(site);
                }
            }
            System.out.println("OPTNAV-ALIAS " + cell.label()
                    + " seamsFlipped=" + seamPolicy.seamsFlipped()
                    + " seamForms: " + seamPolicy.seamsByBodyFormBreakdown()
                    + " leafWraps: " + compiler.aliasLeafWrapBreakdown()
                    + " aliasRootConverted=" + compiler.convertedAliasRootChains()
                    + " aliasRootDistinct=" + compiler.convertedAliasRootDistinctSites()
                    + " aliasRootWalkSites=" + aliasRootWalkSites.size()
                    + " aliasRootShortfall=" + aliasRootShortfall.size()
                    + " aliasRootByShape: " + compiler.convertedAliasRootByShapeBreakdown()
                    + " aliasRootStreamDeclines=" + compiler.declinedAliasRootStream()
                    + " aliasRootBails: " + compiler.aliasRootBailBreakdown()
                    + " aliasRootInteriorSuppressed=" + compiler.aliasRootInteriorSuppressed()
                    + " witnessRefusals=" + seamPolicy.witnessRefusals()
                    + (seamPolicy.witnessRefusals() == 0 ? ""
                            : " witnessRefusalMembers=" + seamPolicy.witnessRefusalMembers()));

            System.out.println("OPTNAV " + cell.label() + " converted="
                    + compiler.convertedChains() + " distinct=" + compiler.convertedDistinctSites()
                    + " walkSites=" + walkSites.size() + " shortfall=" + shortfall.size()
                    + " bailed=" + compiler.eligibleBailedNonExpression()
                    + " twinTypeMissingFallbacks=" + compiler.multiTwinTypeMissingFallbacks()
                    + " metaTwinTypeMissingFallbacks=" + compiler.metaTwinTypeMissingFallbacks()
                    + " declinedStreamShape=" + compiler.declinedStreamShape()
                    + " declinedMetaStream=" + compiler.declinedMetaStream()
                    + " declinedItemStream=" + compiler.declinedItemStream()
                    + " declinedItemMetaStream=" + compiler.declinedItemMetaStream()
                    + " itemInteriorSuppressed=" + compiler.itemInteriorSuppressed()
                    + " itemHarvestBailTotal=" + compiler.itemHarvestBailTotal()
                    + " itemHarvestBails: " + compiler.itemHarvestBailBreakdown()
                    + " byHops: " + compiler.convertedByHopsBreakdown()
                    + " metaBySteps: " + compiler.convertedMetaByStepsBreakdown()
                    + " itemBySteps: " + compiler.convertedItemByStepsBreakdown()
                    + " byShape: " + compiler.convertedByShapeBreakdown()
                    + " files=" + output.size() + " ruleFiles=" + ruleOutput.size()
                    + " thenBodyRootDeclines=" + compiler.thenBodyRootDeclines().size()
                    + (compiler.thenBodyRootDeclines().isEmpty() ? ""
                            : " thenBodyRootDeclineSeats=" + compiler.thenBodyRootDeclines()));
            if (DUMP_SITES_DIR != null) {
                dumpConvertedSites(cell, compiler, loaded);
            }

            java.util.TreeMap<String, Integer> shortfallByFile = new java.util.TreeMap<>();
            for (RExpression missed : shortfall) {
                // Shortfall forensics (prints BEFORE the law asserts — the census
                // step-B instrument): which plan fact is missing on the site.
                NavigationChainClassifier.LadderPlan p = new NavigationChainClassifier()
                        .classify(missed, loaded.workspace()).orElse(null);
                StringBuilder facts = new StringBuilder();
                if (p != null) {
                    facts.append("shape=").append(p.shape())
                            .append(" accessors=").append(p.accessors())
                            .append(" topMissing=").append(p.topType().isMissing())
                            .append(" rootMissing=").append(p.rootType().isMissing())
                            .append(" missingMultiSteps=");
                    for (int i = 0; i < p.accessors().size(); i++) {
                        if (p.hopMulti().get(i) && p.stepTypes().get(i).isMissing()) {
                            facts.append(i).append(':').append(p.accessors().get(i))
                                    .append(' ');
                        }
                    }
                } else {
                    facts.append("classify=EMPTY (predicate drift)");
                }
                String at = missed.sourceRange() == null || missed.sourceRange().file() == null ? ""
                        : missed.sourceRange().file().replace('\\', '/');
                at = at.substring(at.lastIndexOf('/') + 1);
                System.out.println("OPTNAV-SHORTFALL " + cell.label() + " " + facts + " at=" + at);
                shortfallByFile.merge(at, 1, Integer::sum);
            }
            try {
                // v3.2 seat 10 (chaos-1.1.0, D49): the chaos cell DECLARES 56 shortfall sites at the gen-2
                // swap - 52 `getTok` SINGLE sites (s24's Void-typed attribute navigations: the emitter's twin
                // of the #630 Void render law declines them, so they stay on the Mapper idiom - the census's
                // M3 class on this route) + 2 `getTop` + 2 `getMove` (s31's rival-enum split p2 halves, x16 /
                // x36: the enum-typed sites beside the unresolved rival value, D48's class on this route) - the
                // OPTNAV-SHORTFALL lines above name each with its file (the fills-f5 print). EQUALITY-pinned,
                // shrink-only: a fall is a heal to re-pin, a rise a NEW optimised-route class to census.
                // Every other cell keeps the hard zero.
                // Round 1 (cq SF-1): the declared shortfall as a SET at the file grain with counts (the
                // OPTNAV-SHORTFALL `at=` keys of the fills-f5 print), not a bare count of 56.
                java.util.Map<String, Integer> expectedShortfall = cell.label().startsWith("chaos/")
                        ? CHAOS_DECLARED_SHORTFALL_BY_FILE : java.util.Map.of();
                assertEquals(expectedShortfall, shortfallByFile,
                        cell.label() + ": COMPLETENESS — every walk-visible eligible site must"
                                + " convert (a shortfall is an eligible site left on the Mapper"
                                + " idiom; the chaos cell declares its measured shortfall per file"
                                + " until the second round heals it)");
                assertEquals(expectedShortfall.values().stream().mapToInt(Integer::intValue).sum(), shortfall.size(),
                        cell.label() + ": the shortfall count is the declared map's total");
                assertEquals(0, compiler.eligibleBailedNonExpression(),
                        cell.label() + ": NO SILENT BAILS — no classify-passed site may bail on"
                                + " reference-render shape");
                assertTrue(compiler.convertedDistinctSites() <= compiler.convertedChains(),
                        cell.label() + ": distinct sites can never exceed conversion events");
                // ------------------------------ the § 6.3 T1 alias-root LAWS --
                assertEquals(0, aliasRootShortfall.size(), cell.label()
                        + ": ALIAS-ROOT COMPLETENESS — every walk-visible eligible"
                        + " alias-rooted chain of a flipped alias must convert to the"
                        + " alias-rooted ladder");
                assertEquals(0, compiler.aliasRootBailTotal(), cell.label()
                        + ": ALIAS-ROOT NO SILENT BAILS — the harvest/witness belts"
                        + " must never fire on this corpus (a firing is a real class"
                        + " to decode, never a pin-and-forget): "
                        + compiler.aliasRootBailBreakdown());
                assertEquals(0, compiler.aliasRootInteriorSuppressed(), cell.label()
                        + ": no ALIAS_CALL classify-pass may sit interior to an ITEM"
                        + " plan's reference render on this corpus");
                // v3.2 PR-2: the chaos cell DECLARES 26 witness refusals — all
                // C5Forms.fallback/lambdaIte (13 variants × 2), the exact members
                // whose signature types the fork mis-joins (census family F4,
                // lambda-type-join; the D11 default route byte-mismatches the same
                // members). EQUALITY-pinned, not tolerated: the count moving EITHER
                // way is loud — down when F4's seat lands (then delete this pin to
                // restore the zero law), up is a new regression. Every other cell
                // keeps the hard zero.
                // v3.2 seat 2 (F4 healed): the 26 fell with the C5Forms.fallback/lambdaIte signature
                // join (the seat's Law 4) - the zero law is restored on EVERY cell, chaos included.
                // v3.2 seat 10 (chaos-1.1.0, D49): the gen-2 cell DECLARES 72 again - 60 = s26's five
                // C26AliasArms switch aliases over 12 variants, 12 = s29's C29Piped.piped: every one a seam
                // typed `MapperC<? extends string>` (the ROSETTA type name as the Java type - the F4 family
                // at the switch-alias and piped-lambda seats, the census's M2 / M7c classes; the D11 default
                // route byte-mismatches the same members). EQUALITY-pinned as at PR-2, shrink-only.
                // Round 1 (cq SF-1): the declared witness refusals as a MULTISET of member ids (the
                // witnessRefusalMembers print of fills-f5: six members x twelve variants), not a bare 72.
                java.util.TreeMap<String, Integer> witnessRefusalsByMember = new java.util.TreeMap<>();
                for (String member : seamPolicy.witnessRefusalMembers()) {
                    witnessRefusalsByMember.merge(member, 1, Integer::sum);
                }
                java.util.Map<String, Integer> expectedWitnessRefusals = cell.label().startsWith("chaos/")
                        ? CHAOS_DECLARED_WITNESS_REFUSALS : java.util.Map.of();
                assertEquals(expectedWitnessRefusals, witnessRefusalsByMember, cell.label()
                        + ": no multi member may lose its boundary witness (the"
                        + " same-walk law — a refusal diverges the policy from the"
                        + " T0 map and the map belt fires; chaos declares its F4"
                        + " members until the seat heals them)");
                assertEquals(expectedWitnessRefusals.values().stream().mapToInt(Integer::intValue).sum(),
                        seamPolicy.witnessRefusals(), cell.label() + ": the witness-refusal count is the declared multiset's total");
                // (The escape belt of Seat-1 #558 SF1 — "no flip candidate's input
                // may escape" — is retired at PR #607: the alias-root ladder now
                // spells the escaped input name like every other seat, and the
                // drr 7.x witness below proves it on the carrier the belt refused.)
            } catch (AssertionError lawBroken) {
                if (!REPORT_ONLY) {
                    throw lawBroken;
                }
                // Report-only mode is the whole-matrix census: a broken law is
                // RECORDED per cell and the walk continues so one run measures
                // every cell; the run still fails at the end on any finding.
                lawFindings.add(lawBroken.getMessage());
                System.out.println("OPTNAV-LAW-BROKEN " + cell.label() + " " + lawBroken.getMessage());
            }
            // THE OVERLAY UNION (census § 12b.5): the FUNCTION files + the re-emitted
            // RULE files (paths disjoint — functions/ vs reports/); report files are
            // EXCLUDED — zero conversions proven above means the render IS the
            // reference render, so substitution would be a no-op.
            Map<String, String> overlay = new LinkedHashMap<>(output);
            overlay.putAll(ruleOutput);

            if (REPORT_ONLY) {
                materializeOverlay(cell, overlay);
                continue;
            }
            assertEquals(pins.conversions(), compiler.convertedChains(),
                    cell.label() + ": the pinned conversion-event count (census § 4/§ 9) —"
                            + " re-run the census instrument and reconcile the doc BEFORE"
                            + " touching this pin");
            assertEquals(pins.twinTypeMissingFallbacks(),
                    compiler.multiTwinTypeMissingFallbacks(),
                    cell.label() + ": the pinned twin type-missing fallback count"
                            + " (census § 9) — these seats keep the reference emission;"
                            + " reconcile the census BEFORE touching this pin");
            assertEquals(pins.metaTwinTypeMissingFallbacks(),
                    compiler.metaTwinTypeMissingFallbacks(),
                    cell.label() + ": the pinned META twin type-missing fallback count"
                            + " (census § 10b) — walk-invisible twins only (the § 10"
                            + " receipt pins zero missing top types on the walk pool);"
                            + " reconcile the census BEFORE touching this pin");
            assertEquals(pins.streamDeclines(),
                    compiler.declinedStreamShape(),
                    cell.label() + ": the pinned STREAM-shape decline count (census"
                            + " § 9d + § 11 — twice measured out); reconcile the census"
                            + " BEFORE touching this pin");
            assertEquals(pins.metaStreamDeclines(),
                    compiler.declinedMetaStream(),
                    cell.label() + ": the pinned META STREAM-shape decline count (census"
                            + " § 10a — the § 9d/§ 11 declines carried to the family-2"
                            + " pool); reconcile the census BEFORE touching this pin");
            assertEquals(pins.itemStreamDeclines(),
                    compiler.declinedItemStream(),
                    cell.label() + ": the pinned ITEM STREAM-shape decline count"
                            + " (census § 12); reconcile the census BEFORE touching"
                            + " this pin");
            assertEquals(pins.itemMetaStreamDeclines(),
                    compiler.declinedItemMetaStream(),
                    cell.label() + ": the pinned ITEM META STREAM-shape decline count"
                            + " (census § 12); reconcile the census BEFORE touching"
                            + " this pin");
            assertEquals(pins.itemInteriorSuppressed(),
                    compiler.itemInteriorSuppressed(),
                    cell.label() + ": the pinned ITEM-interior suppression count"
                            + " (census § 12b.3 — interior twin re-spells); reconcile"
                            + " the census BEFORE touching this pin");
            assertEquals(pins.itemHarvestBails(),
                    compiler.itemHarvestBailTotal(),
                    cell.label() + ": the pinned ITEM harvest-bail total (census"
                            + " § 12b.3 belts — walk-invisible twins only; the"
                            + " breakdown row names the reason); reconcile the census"
                            + " BEFORE touching this pin");

            // ------------------- the § 6.3 T1∪T2∪T3∪T4 value-seam pins --
            assertEquals(pins.aliasSeams(),
                    seamPolicy.seamsFlipped(),
                    cell.label() + ": the pinned T1∪T2∪T3∪T4 value-seam count (the"
                            + " T0 map's tranche partition, 480/850/428 on the ring"
                            + " cells) — re-run AliasDispositionMapTest and reconcile"
                            + " BEFORE touching this pin");
            assertEquals(pins.aliasSeamForms(),
                    seamPolicy.seamsByBodyFormBreakdown(),
                    cell.label() + ": the pinned value-seam body-form breakdown"
                            + " (the § 4 honest-shape receipt — STREAM = the"
                            + " disclosed Mapper-bodied fallback)");
            assertEquals(pins.aliasRootConversions(),
                    compiler.convertedAliasRootChains(),
                    cell.label() + ": the pinned ALIAS_CALL ladder conversion count"
                            + " (the § 5 CHAIN_ROOT deliverable under the T1 ladder"
                            + " law — T1 members plus, since T4, the whole-chain"
                            + " careful members' disguised chains)");
            assertEquals(pins.aliasRootStreamDeclines(),
                    compiler.declinedAliasRootStream(),
                    cell.label() + ": the pinned ALIAS_CALL STREAM decline count"
                            + " (kept on the bridge composition)");
            assertEquals(pins.aliasLeafWraps(),
                    compiler.aliasLeafWrapBreakdown(),
                    cell.label() + ": the pinned leaf bridge-wrap breakdown (every"
                            + " invocation seat of a flipped member)");

            if ("cdm/5.38.0".equals(cell.label())) {
                assertEmissionWitness(output);
                assertAliasSeamWitness(output);
            }
            if ("drr/6.34.1".equals(cell.label())) {
                assertRuleFaceEmissionWitness(ruleOutput);
                assertThenLocalWrapperWitness(ruleOutput, "drr 6.34.1",
                        "drr/regulation/asic/rewrite/margin/reports/Counterparty1Rule.java",
                        "getReportingParty");
            }
            if ("drr/7.3.0".equals(cell.label())) {
                assertEscapedInputWitness(output);
                assertThenBodyRootWitness(output);
                assertThenLocalWrapperWitness(ruleOutput, "drr 7.3.0",
                        "drr/regulation/common/margin/party/reports"
                                + "/Counterparty2IdentifierTypeRule.java",
                        "getReportingCounterparty");
                assertAsKeyFlippedAliasWitness(output);
            }
            if ("cdm/6.20.2".equals(cell.label())) {
                assertSentinelElementWitness(output);
                assertSpelledRefsWitness(output);
            }

            materializeOverlay(cell, overlay);
        }
        assertTrue(lawFindings.isEmpty(), "conservation laws broken (report-only census): "
                + lawFindings);
        assertTrue(unstaged.isEmpty(), "active catalogue cells of the SOT NOT staged under"
                + " test-corpus/ — the gate walked the others and FAILS here rather than"
                + " skipping them silently: " + unstaged);
        assertTrue(CorpusCells.cellFilterActive() || pinnedButAbsent.isEmpty(),
                "pin rows whose cell is no longer an active catalogue cell of the corpus SOT"
                        + " (a row enforcing nothing): " + pinnedButAbsent);
    }

    /**
     * The full increment-only counter tuple (every conversion, decline, twin
     * fallback and bail is a non-negative increment, so a stable SUM proves ZERO
     * movement) — the REPORT-face zero-conversion oracle.
     */
    private static long conversionTuple(OptimisedExpressionCompiler c) {
        return (long) c.convertedChains() + c.eligibleBailedNonExpression()
                + c.multiTwinTypeMissingFallbacks() + c.metaTwinTypeMissingFallbacks()
                + c.declinedStreamShape() + c.declinedMetaStream()
                + c.declinedItemStream() + c.declinedItemMetaStream()
                + c.itemInteriorSuppressed() + c.itemHarvestBailTotal()
                // the § 6.3 T1 alias rows (rules/reports carry no aliases —
                // any movement here is a synthesis or predicate change):
                + c.convertedAliasRootChains() + c.declinedAliasRootStream()
                + c.aliasRootBailTotal() + c.aliasRootInteriorSuppressed();
    }

    /**
     * THE BAND'S ESCAPED-INPUT WITNESS (PR #607, drr 7.x; witness-before-teach):
     * {@code CounterpartyRoleFromLEI} declares an input {@code partyLei} that
     * collides with its dependency field of the same name (the {@code PartyLei}
     * function), so the reference golden spells {@code _partyLei} at every seat.
     * Its two aliases {@code party1}/{@code party2} (APPLY bodies, T2 DIRECT) were
     * REFUSED by the pre-#607 escape belt; flipped, their value seams re-type to
     * the bare {@code Counterparty} and every alias-root ladder / bridge
     * invocation spells the ESCAPED arg — a raw {@code partyLei} there would hand
     * the {@code @Inject}ed {@code PartyLei} field to a {@code String} parameter.
     */
    private static void assertEscapedInputWitness(Map<String, String> output) {
        String body = output.get("drr/base/util/party/functions/CounterpartyRoleFromLEI.java");
        assertTrue(body != null,
                "drr 7.3.0: the escaped-input witness file CounterpartyRoleFromLEI.java must generate");
        assertTrue(body.contains("protected abstract Counterparty party1(List<? extends Counterparty>"
                        + " counterparties, String _partyLei);"),
                "drr 7.3.0 escape witness: party1 must re-type to the bare value seam with the"
                        + " ESCAPED parameter spelling (the reference AliasModel's own params)");
        assertTrue(body.contains("party1(counterparties, _partyLei)"),
                "drr 7.3.0 escape witness: every party1 invocation must spell the escaped arg");
        assertFalse(body.contains("party1(counterparties, partyLei)")
                        || body.contains("party2(counterparties, partyLei)"),
                "drr 7.3.0 escape witness: a RAW partyLei arg would bind the @Inject'd PartyLei"
                        + " field, not the String input — the miscompile the retired belt refused");
    }

    /**
     * THE BAND'S THEN-BODY ROOT WITNESS (PR #607, drr 7.x; witness-before-teach):
     * in {@code ClearingExceptionsAndExemptionsCounterparty} the chain {@code then
     * clearingException} is the DIRECT body of a {@code then} whose argument is a
     * filtered list, so the reference hoists the argument as a {@code MapperC}
     * local and maps over it; the pre-#607 classifier read the implicit item's IR
     * cardinality (per element, SINGLE) and laddered the whole list through
     * {@code thenArg3.get()} into a {@code MapperS} — javac's catch on the widened
     * pair gate. The seat now classifies STREAM on the then-argument's
     * cardinality and keeps the reference emission; the same law declined six
     * seats per drr 6.3x cell (the ring's drr 6.34.1 included) whose consumers
     * accept either Mapper kind, so they compiled and were never exercised with
     * a list of more than one element.
     */
    private static void assertThenBodyRootWitness(Map<String, String> output) {
        String body = output.get("drr/regulation/csa/rewrite/trade/functions"
                + "/ClearingExceptionsAndExemptionsCounterparty.java");
        assertTrue(body != null, "drr 7.3.0: the then-body root witness file"
                + " ClearingExceptionsAndExemptionsCounterparty.java must generate");
        assertTrue(body.contains(".<ClearingException>map(\"getClearingException\""),
                "drr 7.3.0 then-body witness: the multi then-argument's chain must keep the"
                        + " reference's element-wise map over the MapperC local");
        // The MECHANISM, not a render-order name (the review's catch: a renumbered
        // then-local would let the miscompile back in past a `thenArg3` literal):
        // NO then-local anywhere in the file may be value-unwrapped into a SINGLE
        // ladder — `MapperS.of((thenArgN.get()` for any N.
        for (int at = body.indexOf("MapperS.of((thenArg"); at >= 0;
                at = body.indexOf("MapperS.of((thenArg", at + 1)) {
            int end = body.indexOf('\n', at);
            String line = body.substring(at, end < 0 ? body.length() : end);
            assertFalse(line.contains(".get()"),
                    "drr 7.3.0 then-body witness: a SINGLE ladder over a then-local unwraps a"
                            + " list as one value — the miscompile the widened gate caught: " + line);
        }
    }

    /**
     * THE BAND'S FLIPPED-ALIAS AS-KEY WITNESS (PR #607, drr 7.0–7.3 — the widened
     * pair gate's one BEHAVIOURAL catch, defect 6; witness-before-teach): in
     * {@code Enrich_TransactionReportInstructionTestPackDefault} the alias
     * {@code reportingSide} builds a {@code ReportingSide} whose four fields are
     * {@code as-key} references to value-typed aliases ({@code reportingParty as-key}
     * …). The reference copies each party's global/external KEYS into a
     * reference-only wrapper (upstream assignAsKey); the optimised route's flipped
     * alias call rode the structural {@code MapperS.of(…)} bridge into the as-key
     * arm's identifier check, declined there, and fell to the {@code …Value(}
     * setter — the whole Party EMBEDDED where the reference carries only its keys,
     * so {@code $.getReportingSide} was non-null on one side only (the harness's
     * catch on all four cells). The arm now hoists the bare call into the key-copy
     * local exactly as the reference hoists the Mapper call's {@code .get()}.
     */
    private static void assertAsKeyFlippedAliasWitness(Map<String, String> output) {
        String body = output.get("drr/enrichment/common/test/functions"
                + "/Enrich_TransactionReportInstructionTestPackDefault.java");
        assertTrue(body != null, "drr 7.3.0: the flipped-alias as-key witness file"
                + " Enrich_TransactionReportInstructionTestPackDefault.java must generate");
        int keyCopies = 0;
        for (int at = body.indexOf(".setGlobalReference(Optional.ofNullable("); at >= 0;
                at = body.indexOf(".setGlobalReference(Optional.ofNullable(", at + 1)) {
            keyCopies++;
        }
        assertEquals(4, keyCopies, "drr 7.3.0 as-key witness: the four as-key fields of the"
                + " reportingSide alias must each copy the party's global key (upstream"
                + " assignAsKey — the reference form)");
        assertTrue(body.contains("final Party _reportingParty = reportingParty(reportableEvent);"),
                "drr 7.3.0 as-key witness: the flipped alias call hoists BARE into the key-copy"
                        + " local (no .get() — the value seam already yields the Party)");
        assertTrue(body.contains(".setReportingParty(ReferenceWithMetaParty.builder()"),
                "drr 7.3.0 as-key witness: the PLAIN setter over the reference-only wrapper");
        assertFalse(body.contains("setReportingPartyValue(")
                        || body.contains("setReportingCounterpartyValue("),
                "drr 7.3.0 as-key witness: a Value setter embeds the whole Party where the"
                        + " reference carries its keys only — the behavioural divergence the"
                        + " widened pair gate caught");
    }

    /**
     * THE BAND'S THEN-LOCAL WRAPPER WITNESS (PR #607, every drr cell incl. the
     * ring's 6.34.1; witness-before-teach): {@code extract reportingSide ->
     * reportingParty then extract …} hoists its then-argument as a local whose
     * element is the {@code [metadata reference]} WRAPPER — golden {@code final
     * MapperS<ReferenceWithMetaParty> thenArg = …}. The then-stage decl typing
     * recovered that wrapper from the compiled value's REFS until PR #613 (the
     * type-fact channel of the day, {@code NavigationHandler.uniqueMetaWrapperForValueType},
     * retired at v3.1 C2d retirement family 6 for {@code NavigationHandler.levelMetaWrapper}'s
     * compiled-item rule — the stamp this swap carries); the first cut of
     * the spelled-refs law dropped every inherited ref, the recovery found no
     * wrapper, and the local re-typed bare ({@code MapperS<Party>}) over a ladder
     * that yields the wrapper — javac's catch on the widened pair gate
     * ({@code Counterparty1Rule} / {@code Counterparty2Rule} /
     * {@code Counterparty2IdentifierTypeRule}, 3 files per drr cell). The swap
     * now keeps the reference chain's meta-wrapper refs, so the decl spells the
     * wrapper, the file imports it once (the golden's own import), and the chain
     * STILL converts to the getter ladder — fixed at its refs, not by declining.
     */
    private static void assertThenLocalWrapperWitness(Map<String, String> ruleOutput,
            String cellName, String ruleFile, String topGetter) {
        String body = ruleOutput.get(ruleFile);
        assertTrue(body != null, cellName + ": the then-local wrapper witness file " + ruleFile
                + " must generate (rule-face keys present: "
                + ruleOutput.keySet().stream().filter(k -> k.endsWith("Counterparty1Rule.java")
                        || k.endsWith("Counterparty2IdentifierTypeRule.java")).toList() + ")");
        assertTrue(body.contains("final MapperS<ReferenceWithMetaParty> thenArg = MapperS.of(input)"),
                cellName + " then-local witness: the hoisted then-argument must declare the"
                        + " [metadata reference] WRAPPER element the golden declares");
        assertFalse(body.contains("final MapperS<Party> thenArg"),
                cellName + " then-local witness: a bare-element decl over a wrapper-valued"
                        + " ladder is the miscompile the widened gate caught");
        assertTrue(body.contains("MapperS.of((item.get() == null ? null : item.get().getReportingSide()"
                        + " == null ? null : item.get().getReportingSide()." + topGetter + "()))"
                        + ".filterSingle(sameNav -> true)"),
                cellName + " then-local witness: the then-argument chain must still convert to the"
                        + " getter ladder (the class is fixed at its refs, not by declining)");
        int wrapperImports = body.split(
                "import cdm\\.base\\.staticdata\\.party\\.metafields\\.ReferenceWithMetaParty;", -1)
                .length - 1;
        assertEquals(1, wrapperImports, cellName + " then-local witness: exactly ONE import of"
                + " ReferenceWithMetaParty (the decl spells it; the reference render registered it)");
    }

    /**
     * THE BAND'S SPELLED-REFS WITNESS (PR #607, cdm 6.x {@code cdm/ingest/**};
     * witness-before-teach): {@code MapAmericanExerciseTerms} navigates fpml's
     * {@code AdjustableOrRelativeDate} while its CDM output claims the same simple
     * name; the reference spells the fpml hop witnesses as import-collision
     * sentinels the resolver FQNs (their import suppressed), and the pre-#607
     * ladder inherited those hop refs while spelling none of them — a second
     * {@code import …AdjustableOrRelativeDate;}, javac's duplicate-import error in
     * 24 files per cdm 6.20.x cell. The swap's refs are now exactly the classes it
     * spells, so the file carries ONE import of the colliding name (the CDM one,
     * the golden's own) and no import of the fpml twin.
     */
    private static void assertSpelledRefsWitness(Map<String, String> output) {
        String body = output.get("cdm/ingest/fpml/confirmation/common/functions"
                + "/MapAmericanExerciseTerms.java");
        assertTrue(body != null, "cdm 6.20.2: the spelled-refs witness file"
                + " MapAmericanExerciseTerms.java must generate");
        int cdmImports = body.split("import cdm\\.base\\.datetime\\.AdjustableOrRelativeDate;", -1)
                .length - 1;
        int fpmlImports = body.split("import fpml\\.consolidated\\.shared\\.AdjustableOrRelativeDate;",
                -1).length - 1;
        assertEquals(1, cdmImports, "cdm 6.20.2 spelled-refs witness: exactly one import of the"
                + " CDM AdjustableOrRelativeDate (the file's first claim)");
        assertEquals(0, fpmlImports, "cdm 6.20.2 spelled-refs witness: NO import of the fpml"
                + " AdjustableOrRelativeDate — a ladder spells no hop type, so it carries no"
                + " hop import (the duplicate-import miscompile the widened gate caught)");
        assertTrue(body.contains("fpmlAmericanExercise == null ? null : fpmlAmericanExercise"
                        + ".getExpirationDate()"),
                "cdm 6.20.2 spelled-refs witness: the ingest chain must still convert to the"
                        + " getter ladder (the class is fixed at its refs, not by declining)");
    }

    /**
     * THE BAND'S SENTINEL-ELEMENT WITNESS (PR #607, cdm 6.20.2–6.20.5;
     * witness-before-teach): {@code MapCreditEventsReferenceWithReference.creditEvents}
     * is a multi alias whose element, fpml's {@code CreditEvents}, collides on
     * simple name with the CDM {@code CreditEvents} output, so the seam string
     * spells the element as an import-collision sentinel. The policy strips the
     * sentinel before the same-walk comparison (the witness derives), the seam
     * re-types to {@code List<? extends CreditEvents>} (fpml's is the file's
     * first claim — its import stands once), the bridge's witness rides the
     * sentinel channel (bare here, no second import), and no sentinel delimiter
     * survives into the file. cdm 6.20.6's twin takes {@code first} (SINGLE), so
     * the ring never exercised this class.
     */
    private static void assertSentinelElementWitness(Map<String, String> output) {
        String body = output.get("cdm/ingest/fpml/confirmation/product/creditdefaultswapoption"
                + "/functions/MapCreditEventsReferenceWithReference.java");
        assertTrue(body != null, "cdm 6.20.2: the sentinel-element witness file"
                + " MapCreditEventsReferenceWithReference.java must generate");
        assertTrue(body.contains("protected abstract List<? extends CreditEvents> creditEvents("),
                "cdm 6.20.2 sentinel witness: creditEvents must re-type to the List value seam"
                        + " with the element resolved bare (fpml's CreditEvents is the file's"
                        + " first claim)");
        assertTrue(body.contains("MapperC.<CreditEvents>of(creditEvents("),
                "cdm 6.20.2 sentinel witness: the leaf bridge's witness must resolve bare like"
                        + " the seam declaration");
        int imports = body.split("import fpml\\.consolidated\\.option\\.shared\\.CreditEvents;", -1)
                .length - 1;
        assertEquals(1, imports, "cdm 6.20.2 sentinel witness: exactly ONE import of the fpml"
                + " CreditEvents (the bridge must not add a second import of a colliding name)");
        assertFalse(body.indexOf(com.regnosys.rosetta.generator.java.template
                        .ImportCollisionResolver.OPEN) >= 0,
                "cdm 6.20.2 sentinel witness: no collision sentinel may survive into the file");
    }

    /**
     * The drr rule-face emission witnesses (witness-before-teach, census § 12b.3):
     * real generated {@code *Rule.java} bodies carry (a) the ITEM-rooted ladder —
     * a harvested root value-unwrapped ONCE ({@code .get() == null ? null : }
     * inside a {@code MapperS.of((} boundary) — and (b) the PARAM-rooted rule
     * ladder over the synthetic {@code input}.
     */
    private static void assertRuleFaceEmissionWitness(Map<String, String> ruleOutput) {
        boolean anyItemLadder = false;
        boolean anyInputLadder = false;
        for (String body : ruleOutput.values()) {
            if (body.contains(".get() == null ? null : ")
                    && body.contains("MapperS.of((")) {
                anyItemLadder = true;
            }
            if (body.contains("MapperS.of((input == null ? null : ")) {
                anyInputLadder = true;
            }
            if (anyItemLadder && anyInputLadder) {
                break;
            }
        }
        assertTrue(anyItemLadder,
                "drr rule face: no ITEM-rooted ladder witness (.get() == null ladder"
                        + " inside a MapperS.of(( boundary) in any generated *Rule.java");
        assertTrue(anyInputLadder,
                "drr rule face: no PARAM-rooted input ladder witness"
                        + " (MapperS.of((input == null ? null : ) in any generated"
                        + " *Rule.java");
    }

    /**
     * THE OVERLAY MATERIALIZER: writes the cell's optimised FUNCTION files to
     * {@code target/optimised-tree/<cellLeaf>/<relativePath>} + a content stamp
     * ({@code .overlay-marker} — file count + SHA-256 over sorted path+content), the
     * tree the differential gate's overlay compile substitutes into the goldens
     * (rune-equivalence {@code OptimisedNavigationPairGateTest} via
     * {@code CorpusClasses.ensureCompiledWithOverlay}; a regenerated overlay changes
     * the stamp and forces recompilation there).
     */
    private static void materializeOverlay(CorpusCells.CellSpec cell, Map<String, String> output)
            throws IOException {
        String leaf = cell.ownDir().substring(cell.ownDir().lastIndexOf('/') + 1);
        java.nio.file.Path root = java.nio.file.Path.of("target", "optimised-tree", leaf);
        if (java.nio.file.Files.isDirectory(root)) {
            try (java.util.stream.Stream<java.nio.file.Path> s = java.nio.file.Files.walk(root)) {
                s.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
                    try {
                        java.nio.file.Files.delete(p);
                    } catch (IOException e) {
                        throw new java.io.UncheckedIOException(e);
                    }
                });
            }
        }
        java.nio.file.Files.createDirectories(root);
        java.security.MessageDigest digest;
        try {
            digest = java.security.MessageDigest.getInstance("SHA-256");
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
        List<String> sortedPaths = new ArrayList<>(output.keySet());
        Collections.sort(sortedPaths);
        for (String rel : sortedPaths) {
            java.nio.file.Path file = root.resolve(rel.replace('/', java.io.File.separatorChar));
            java.nio.file.Files.createDirectories(file.getParent());
            String content = output.get(rel);
            java.nio.file.Files.writeString(file, content, java.nio.charset.StandardCharsets.UTF_8);
            digest.update(rel.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            digest.update((byte) '\n');
            digest.update(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            digest.update((byte) '\n');
        }
        StringBuilder hex = new StringBuilder();
        for (byte b : digest.digest()) {
            hex.append(String.format("%02x", b));
        }
        java.nio.file.Files.writeString(root.resolve(".overlay-marker"),
                "files=" + sortedPaths.size() + " sha256=" + hex,
                java.nio.charset.StandardCharsets.UTF_8);
        System.out.println("OPTNAV " + cell.label() + " overlay: files=" + sortedPaths.size()
                + " sha256=" + hex + " -> " + root.toAbsolutePath());
    }

    /**
     * The shared-classifier walk over the cell's OWN functions' OPERATION bodies —
     * the same site census the instrument prints (`siteEligibleByContext:
     * operation=…`), recomputed here so the completeness law compares identity sets,
     * not just counts.
     */
    private static Set<RExpression> operationSiteWalk(CorpusCells.LoadedCell loaded) {
        Set<RExpression> sites = Collections.newSetFromMap(new IdentityHashMap<>());
        NavigationChainClassifier classifier = new NavigationChainClassifier();
        for (RModel model : loaded.ownModels()) {
            for (RRootElement element : model.rootElements()) {
                if (element instanceof RFunction func) {
                    for (ROperation op : func.operations()) {
                        collectSites(op.expression(), loaded.workspace(), func, classifier, sites);
                    }
                } else if (element instanceof RRule rule && rule.expression().isPresent()) {
                    // THE FAMILY-3 RULE FACE (census § 12): the same
                    // synthetic-owner bridge the emission renders through
                    // (fromRule shares the rule's expression node — no copy — so
                    // site identity is stable across the walk and the render).
                    collectSites(rule.expression().get(), loaded.workspace(),
                            RFunction.fromRule(rule), classifier, sites);
                }
            }
        }
        return sites;
    }

    /**
     * The § 6.3 T1 alias-root site walk — the ALIAS_CALL twin of
     * {@link #operationSiteWalk}: OPERATION-body chains whose classified root is
     * a FLIPPED alias's invocation (the admission reads the SAME policy instance
     * the emitter converted with), SINGLE/TERMINAL_MULTI shapes only (STREAM =
     * the pinned decline, kept on the bridge composition).
     */
    private static Set<RExpression> aliasRootSiteWalk(CorpusCells.LoadedCell loaded,
            AliasValueSeamPolicy policy) {
        Set<RExpression> sites = Collections.newSetFromMap(new IdentityHashMap<>());
        NavigationChainClassifier classifier = new NavigationChainClassifier();
        for (RModel model : loaded.ownModels()) {
            for (RRootElement element : model.rootElements()) {
                if (element instanceof RFunction func && !func.shortcuts().isEmpty()) {
                    NavigationChainClassifier.AliasRootAdmission admission = name -> {
                        AliasValueSeamPolicy.FlipFacts f =
                                policy.flipFactsByNameOrNull(func, name);
                        return f == null || !f.aliasRootLadderEligible() ? null
                                : new NavigationChainClassifier.AliasRootFacts(f.isMulti());
                    };
                    for (ROperation op : func.operations()) {
                        collectAliasRootSites(op.expression(), loaded.workspace(), func,
                                classifier, admission, sites);
                    }
                }
            }
        }
        return sites;
    }

    private static void collectAliasRootSites(RNode node, RWorkspace ws, RFunction owner,
            NavigationChainClassifier classifier,
            NavigationChainClassifier.AliasRootAdmission admission, Set<RExpression> out) {
        if (node instanceof RExpression expr
                && NavigationChainClassifier.isMaximalChainTopCandidate(expr)
                && classifier.classify(expr, ws, owner, admission)
                        .filter(p -> p.rootKind()
                                == NavigationChainClassifier.LadderPlan.RootKind.ALIAS_CALL
                                && p.shape() != NavigationChainClassifier.LadderPlan.Shape.STREAM)
                        .isPresent()) {
            out.add(expr);
        }
        for (RNode child : node.children()) {
            collectAliasRootSites(child, ws, owner, classifier, admission, out);
        }
    }

    private static void collectSites(RNode node, RWorkspace ws, RFunction owner,
            NavigationChainClassifier classifier, Set<RExpression> out) {
        if (node instanceof RExpression expr
                && NavigationChainClassifier.isMaximalChainTopCandidate(expr)
                // The CONVERTING pool = SINGLE + TERMINAL_MULTI over PARAM and
                // ITEM roots, meta-bearing plans included (the family-2 § 10a +
                // family-3 § 12 emissions); STREAM-shape sites are the census
                // § 9d + § 11 measured declines (carried to the item pools) and
                // ITEM plans with a MISSING node-channel root type are the § 12
                // unprovable-wrapper refusal (the emitter's rootTypeMissing
                // bail, mirrored here so the completeness law stays
                // identity-exact) — all deliberately reference-emitted and
                // pinned by their per-family counters, not the completeness law.
                && classifier.classify(expr, ws, owner)
                        .filter(p -> p.shape() != NavigationChainClassifier.LadderPlan.Shape.STREAM
                                && !(p.rootKind() == NavigationChainClassifier.LadderPlan.RootKind.ITEM
                                        && p.rootType().isMissing()))
                        .isPresent()) {
            out.add(expr);
        }
        for (RNode child : node.children()) {
            collectSites(child, ws, owner, classifier, out);
        }
    }

    /**
     * The cdm5 emission witnesses (witness-before-teach): real generated bodies
     * carry the converting § 9a forms — the SINGLE ladder ({@code MapperS.of((… ==
     * null ? null : …}) and the TERMINAL_MULTI list ladder ({@code MapperC.<X>of((…})
     * with its {@code unionSame(MapperC.<X>ofNull())} suffix — plus the § 10a
     * family-2 forms: a ladder carrying a BAKED interior meta deref
     * ({@code .getValue() == null ? null : } — the reference render's
     * {@code map("Type coercion", …)} step as a getter run) and the meta-top
     * TERMINAL_MULTI boundary witnessed with BOTH wrapper kinds
     * ({@code MapperC.<FieldWithMetaX>of((} and {@code MapperC.<ReferenceWithMetaX>of((}
     * — cdm5's § 10 pool carries both) — and carry ZERO stream-decline machinery
     * (the § 9d + § 11 measured declines: {@code _nav} local/lambda names, the
     * FQN-inline collect, and the § 11 loop-helper spellings
     * ({@code _navStream}) are tokens UNIQUE to the two declined forms — the
     * decline-lock negative witness covers BOTH).
     */
    private static void assertEmissionWitness(Map<String, String> output) {
        boolean anyLadder = false;
        boolean anyMapperC = false;
        boolean anyMetaDerefLadder = false;
        boolean anyFieldMetaBoundary = false;
        boolean anyRefMetaBoundary = false;
        for (Map.Entry<String, String> file : output.entrySet()) {
            String body = file.getValue();
            if (body.contains("MapperS.of((") && body.contains(" == null ? null : ")) {
                anyLadder = true;
            }
            if (body.contains("MapperC.<") && body.contains(">of((")
                    && body.contains(".unionSame(MapperC.<")) {
                anyMapperC = true;
            }
            if (body.contains(".getValue() == null ? null : ")) {
                anyMetaDerefLadder = true;
            }
            if (body.contains("MapperC.<FieldWithMeta")
                    && body.contains(".unionSame(MapperC.<FieldWithMeta")) {
                anyFieldMetaBoundary = true;
            }
            if (body.contains("MapperC.<ReferenceWithMeta")
                    && body.contains(".unionSame(MapperC.<ReferenceWithMeta")) {
                anyRefMetaBoundary = true;
            }
            assertFalse(body.contains("_nav0")
                    || body.contains(".collect(java.util.stream.Collectors.toList())")
                    || body.contains("_navStream"),
                    () -> file.getKey() + ": the STREAM shape is declined (§ 9d + § 11"
                            + " — both re-forms measured out) — neither form's unique"
                            + " tokens may appear in any generated body");
        }
        assertTrue(anyLadder,
                "cdm5 converts SINGLE sites — at least one generated body must carry the"
                        + " boundary-wrapped ladder idiom MapperS.of((… == null ? null : …");
        assertTrue(anyMapperC,
                "cdm5 converts TERMINAL_MULTI sites — at least one generated body must"
                        + " carry the witnessed MapperC.<X>of((… boundary + unionSame suffix");
        assertTrue(anyMetaDerefLadder,
                "cdm5 converts interior-meta chains (§ 10a) — at least one ladder must"
                        + " carry the baked .getValue() interior deref");
        assertTrue(anyFieldMetaBoundary,
                "cdm5 converts FIELD-meta-top TERMINAL_MULTI sites (§ 10a) — the"
                        + " MapperC.<FieldWithMetaX>of(( boundary witness must appear");
        assertTrue(anyRefMetaBoundary,
                "cdm5 converts REF-meta-top TERMINAL_MULTI sites (§ 10a) — the"
                        + " MapperC.<ReferenceWithMetaX>of(( boundary witness must appear");
        assertFalse(output.isEmpty(), "the cdm5 FUNCTION emission produced no files");
    }

    /**
     * The § 6.3 T1–T4 value-seam witnesses (witness-before-teach, cdm5
     * grain) — the T0 map's own member rows as carriers, ONE BLOCK PER
     * TRANCHE: the in-method banners delimit the T2 set (PR-23), the T3 set
     * (PR-24) and the T4 careful-class set (the PR-25 emission, folded in at
     * T5 per the #561 Rule-6 N1 follow-up — a dispatch seam+override pair, a
     * D5 render-authority row, a D4-live disguised-seat row). This list
     * enumerates the T1 set:
     * <ol>
     *   <li><b>the re-typed seam + ladder body</b> —
     *       {@code ConvertToAdjustableOrRelativeDate.relativeDate} (T1 row:
     *       PARAM:SINGLE, 7 existence + 7 chain-root seats): the abstract seam
     *       line carries NO Mapper wrapper, and the impl returns the
     *       null-guarded getter ladder;</li>
     *   <li><b>the leaf bridge</b> — the same member's existence seats consume
     *       {@code MapperS.of(relativeDate(…))} (the § 5 disclosed bridge);</li>
     *   <li><b>the alias-rooted ladder</b> — the same member's chain-root seats
     *       re-root the § 9a ladder at the invocation
     *       ({@code MapperS.of((relativeDate(…) == null ? null : …});</li>
     *   <li><b>the exclusion negative</b> — the D1-excluded
     *       {@code Qualify_Shaping.instruction} keeps its Mapper-typed seam
     *       VERBATIM (plan § 6 D1: only-exists participants never re-type).</li>
     * </ol>
     */
    private static void assertAliasSeamWitness(Map<String, String> output) {
        String convert = output.get(
                "cdm/base/datetime/functions/ConvertToAdjustableOrRelativeDate.java");
        assertTrue(convert != null,
                "cdm5: the T1 witness file ConvertToAdjustableOrRelativeDate.java must generate");
        assertTrue(convert.contains("protected abstract RelativeDateOffset relativeDate("),
                "cdm5 T1 seam: relativeDate must re-type to the bare value seam"
                        + " (protected abstract RelativeDateOffset relativeDate()");
        assertFalse(convert.contains("MapperS<? extends RelativeDateOffset> relativeDate("),
                "cdm5 T1 seam: the Mapper-typed relativeDate seam must be GONE");
        assertTrue(convert.contains("return (adjustableOrAdjustedOrRelativeDate == null ? null : "),
                "cdm5 T1 body: relativeDate's impl must return the null-guarded ladder value");
        assertTrue(convert.contains("MapperS.of(relativeDate(adjustableOrAdjustedOrRelativeDate))"),
                "cdm5 T1 leaf: an invocation seat must carry the structural MapperS.of bridge");
        assertTrue(convert.contains("MapperS.of((relativeDate(adjustableOrAdjustedOrRelativeDate)"
                        + " == null ? null : "),
                "cdm5 T1 chain-root: an alias-rooted § 9a ladder must land at a"
                        + " CHAIN_ROOT seat (the invocation as the ladder root)");
        String excluded = output.get("cdm/event/common/functions/Qualify_Shaping.java");
        assertTrue(excluded != null,
                "cdm5: the D1-excluded witness file Qualify_Shaping.java must generate");
        assertTrue(excluded.contains("MapperS<? extends Instruction> instruction("),
                "cdm5 D1 exclusion: Qualify_Shaping.instruction must KEEP its"
                        + " Mapper-typed seam verbatim (only-exists participant)");

        // ---------------- the T2 VALUE-BODY witnesses (PR-23; plan § 4) ----------------
        String isWeekend = output.get("cdm/base/datetime/functions/IsWeekend.java");
        assertTrue(isWeekend != null,
                "cdm5: the T2 DIRECT witness file IsWeekend.java must generate");
        assertTrue(isWeekend.contains(
                "protected abstract DayOfWeekEnum dayOfWeek1(Date date"),
                "cdm5 T2 seam: the APPLY-headed dayOfWeek1 must re-type to the bare"
                        + " value seam (the collision-numbered rendered name kept)");
        assertTrue(isWeekend.contains("return dayOfWeek0.evaluate(date);"),
                "cdm5 T2 DIRECT: the bare-call APPLY body must land the § 4"
                        + " direct-call composition — the Mapper-of wrap stripped");
        String isHoliday = output.get("cdm/base/datetime/functions/IsHoliday.java");
        assertTrue(isHoliday != null,
                "cdm5: the T2 multi DIRECT witness file IsHoliday.java must generate");
        assertTrue(isHoliday.contains(
                "protected abstract List<Date> holidays(Date checkDate"),
                "cdm5 T2 multi seam: the bare-MapperC<Date> seam must re-type to"
                        + " List<Date> (the § 3 canonical clause's bare form — no"
                        + " wildcard, the seam string's inner text verbatim)");
        assertTrue(isHoliday.contains(
                "return businessCenterHolidaysMultiple.evaluate(businessCenters);"),
                "cdm5 T2 multi DIRECT: the bare-call multi body must land the"
                        + " direct-call composition (the callee's List returned bare)");
        String exercise = output.get("cdm/event/common/functions/Create_Exercise.java");
        assertTrue(exercise != null,
                "cdm5: the T2 BOUNDARY witness file Create_Exercise.java must generate");
        assertTrue(exercise.contains("return (MapperS.of(optionPayout(exerciseInstruction,"
                        + " originalTrade)).<Product>map(\"getUnderlier\","),
                "cdm5 T2 BOUNDARY: the chain-headed underlier body must keep the"
                        + " Mapper pipeline behind the value seam (the § 4 tail"
                        + " clause's disclosed fallback) — since T3 its alias root"
                        + " optionPayout is ITSELF flipped, so the chain composes"
                        + " the structural bridge over the value invocation");
        assertTrue(exercise.contains(".getUnderlier())).get();"),
                "cdm5 T2 BOUNDARY: the underlier body must value-unwrap ONCE at"
                        + " the seam boundary ((…).get())");
        assertTrue(exercise.contains("exercise.addAll(toBuilder(MapperS.of("
                        + "execution(exerciseInstruction, originalTrade)).getMulti()));"),
                "cdm5 T2 ADD-seat guard: a SINGLE-seamed flipped member at the"
                        + " whole-output ADD seat must keep the bridge + the"
                        + " single→list getMulti() lift (null → EMPTY list — the"
                        + " Create_Exercise ClassCastException catch; a bare strip"
                        + " hands the single value to addAll/toBuilder)");
        String fxMtm = output.get("cdm/event/position/functions/FxMarkToMarket.java");
        assertTrue(fxMtm != null,
                "cdm5: the T2 hoist witness file FxMarkToMarket.java must generate");
        assertTrue(fxMtm.contains("final FieldWithMetaString fieldWithMetaString ="
                        + " quotedCurrency(trade);"),
                "cdm5 T2 hoist: quotedQuantity's lifted decl must render VERBATIM"
                        + " ahead of the re-shaped trailing return — since T3 the"
                        + " referenced quotedCurrency is ITSELF flipped (a LIST_OP"
                        + " T3 row whose seam is the bare FieldWithMetaString), so"
                        + " the hoist seat consumes the BARE wrapper value (the"
                        + " strip channel; no .get() round-trip)");
        assertTrue(fxMtm.contains("filterQuantityByCurrency.evaluate("
                        + "MapperC.<FieldWithMetaNonNegativeQuantitySchedule>of("
                        + "quantities(trade)).<QuantitySchedule>map(\"Type coercion\","),
                "cdm5 T2 wrapper-arg: the flipped multi META-topped member"
                        + " quantities must keep the BRIDGE at its evaluate-arg seat"
                        + " and compose the #347-F5 wrapper→value Type-coercion map"
                        + " (the strip guard — a bare pass would hand List<Wrapper>"
                        + " to a List<? extends Value> param, non-compiling)");
        assertTrue(fxMtm.contains(".getValue()).getMulti(), (fieldWithMetaString == null"),
                "cdm5 T2 wrapper-arg: the coerced bridge arg must value-unwrap via"
                        + " getMulti() at the callee boundary");

        // ------------- the T3 VALUE-BODY witnesses (PR-24; plan § 4) -------------
        assertTrue(exercise.contains(
                "protected abstract OptionPayout optionPayout(ExerciseInstruction"),
                "cdm5 T3 seam: the D2 CONDITIONAL member optionPayout must re-type"
                        + " to the bare value seam (the § 6 D2 clause arms the § 14f"
                        + " capture for this tranche)");
        assertTrue(exercise.contains("if (exists(MapperS.of(exerciseInstruction)"
                        + ".<ReferenceWithMetaOptionPayout>map(\"getExerciseOption\","),
                "cdm5 T3 LADDER: optionPayout's multi-return if ladder must keep"
                        + " today's Mapper-composed condition (the § 4 disclosed"
                        + " non-goal at T3)");
        assertTrue(exercise.contains(".getValue())).get();"),
                "cdm5 T3 LADDER: the then rung must take the paren value boundary"
                        + " ((…).get() — the § 4 tail clause at the rung grain)");
        assertTrue(exercise.contains(".<OptionPayout>mapC(\"getOptionPayout\","
                        + " payout -> payout.getOptionPayout()).get();"),
                "cdm5 T3 LADDER: the terminal else rung's MapperS.of lift must"
                        + " INVERT to the bare only-element collapse (the § 4 direct"
                        + " rung — the construction-equality strip)");
        String triangulation = output.get(
                "cdm/observable/common/functions/CashPriceQuantityNoOfUnitsTriangulation.java");
        assertTrue(triangulation != null,
                "cdm5: the T3 PIPE witness file CashPriceQuantityNoOfUnits"
                        + "Triangulation.java must generate");
        assertTrue(triangulation.contains(
                "protected abstract BigDecimal notional(List<? extends"
                        + " NonNegativeQuantitySchedule> quantity,"),
                "cdm5 T3 seam: the PIPE member notional must re-type to the bare"
                        + " value seam");
        assertTrue(triangulation.contains("final MapperC<BigDecimal> thenArg0 ="),
                "cdm5 T3 PIPE: the then-hoist decls must render VERBATIM (the"
                        + " decorated then-hoist — decls untouched, only the trailing"
                        + " return re-shapes)");
        assertTrue(triangulation.contains("return (MapperS.of(thenArg1.get())).get();"),
                "cdm5 T3 PIPE: the trailing return must take the value boundary"
                        + " over the F4-collapsed consumer");

        // ---- the T4 CAREFUL-CLASS witnesses (v3 PR-25 emission; the witness
        // ---- block folded in at T5 per the #561 Rule-6 N1 follow-up) ----
        String yearFraction = output.get(
                "cdm/base/datetime/daycount/functions/YearFraction.java");
        assertTrue(yearFraction != null,
                "cdm5: the T4 dispatch witness file YearFraction.java must generate");
        assertTrue(yearFraction.contains(
                "public static abstract class YearFractionACT_360 implements RosettaFunction {"),
                "cdm5 T4 dispatch: the ACT_360 case unit must render (the O5p"
                        + " nest-attribution row $YearFractionACT_360)");
        assertTrue(yearFraction.contains(
                "protected abstract Integer daysInPeriod(DayCountFractionEnum"
                        + " dayCountFractionEnum, Date startDate, Date endDate,"
                        + " Date terminationDate, Integer periodsInYear);"),
                "cdm5 T4 dispatch seam: the case unit's abstract alias seam must"
                        + " re-type to the bare value form");
        assertFalse(yearFraction.contains("MapperS<Integer> daysInPeriod("),
                "cdm5 T4 dispatch: the Mapper-typed daysInPeriod seam must be GONE"
                        + " in every case unit (14 reference-golden occurrences,"
                        + " zero here)");
        assertTrue(yearFraction.contains(
                "protected Integer daysInPeriod(DayCountFractionEnum"
                        + " dayCountFractionEnum, Date startDate, Date endDate,"
                        + " Date terminationDate, Integer periodsInYear) {"),
                "cdm5 T4 dispatch: the Default override must re-type WITH its"
                        + " abstract seam (unit atomicity BY CONSTRUCTION — both"
                        + " render from the ONE AliasModel; javac enforces)");
        assertTrue(yearFraction.contains("return dateDifference.evaluate(startDate, endDate);"),
                "cdm5 T4 dispatch body: the daysInPeriod overrides (7 case units"
                        + " share this APPLY body, ACT_360 among them) must land"
                        + " the § 4 DIRECT call composition");
        assertTrue(yearFraction.contains(
                "MapperMaths.<BigDecimal, Integer, Integer>divide(MapperS.of("
                        + "daysInPeriod(dayCountFractionEnum, startDate, endDate,"
                        + " terminationDate, periodsInYear)), MapperS.of(360)).get()"),
                "cdm5 T4 dispatch seat: the arithmetic consumer must bridge the"
                        + " value invocation whose arg names read the DISPATCH"
                        + " BASE's declared inputs (the renderEnclosingInputs/"
                        + "dispatchBaseOf law)");
        assertTrue(yearFraction.contains(
                "MapperMaths.<Integer, Integer, Integer>multiply(MapperS.of("
                        + "daysInPeriod(dayCountFractionEnum, startDate, endDate,"
                        + " terminationDate, periodsInYear)), MapperS.of(periodsInYear))"),
                "cdm5 T4 dispatch: the ACT_ACT_ICMA golden's SECOND daysInPeriod"
                        + " invocation must survive (per-reference re-invocation —"
                        + " the § 2 call-by-name witness, preserved identically)");
        String observation = output.get(
                "cdm/observable/asset/calculatedrate/functions/DetermineObservationPeriod.java");
        assertTrue(observation != null,
                "cdm5: the T4 D5 witness file DetermineObservationPeriod.java must generate");
        assertTrue(observation.contains(
                "protected abstract List<BusinessCenterEnum> allBusinessDays("
                        + "CalculationPeriodBase adjustedCalculationPeriod,"
                        + " FloatingRateCalculationParameters calculationParams);"),
                "cdm5 T4 D5 seam: allBusinessDays (IR bit SINGLE, render channel"
                        + " MapperC) must re-type on the RENDER channel to the bare"
                        + " List (§ 3 render-authority; the map's d5 column is the"
                        + " per-member receipt)");
        assertFalse(observation.contains("MapperC<BusinessCenterEnum> allBusinessDays("),
                "cdm5 T4 D5: the Mapper-typed allBusinessDays seam must be GONE");
        assertTrue(observation.contains(
                "generateObservationPeriod.evaluate(adjustedCalculationPeriod,"
                        + " allBusinessDays(adjustedCalculationPeriod, calculationParams),"
                        + " shiftDefaulted"),
                "cdm5 T4 D5 seat: the evaluate-arg must consume the bare List"
                        + " straight (the CALL_ARG strip — a MULTI-seamed value IS"
                        + " the List the param wants; the token runs through the"
                        + " argument boundary so the reference's .getMulti() spelling"
                        + " cannot satisfy it)");
        String debtOption = output.get(
                "cdm/product/qualification/functions/Qualify_InterestRate_Option_DebtOption.java");
        assertTrue(debtOption != null,
                "cdm5: the T4 D4-live witness file Qualify_InterestRate_Option_"
                        + "DebtOption.java must generate");
        assertTrue(debtOption.contains(
                "protected abstract List<? extends OptionPayout> optionPayout("
                        + "EconomicTerms economicTerms);"),
                "cdm5 T4 D4-live seam: the zero-channel-visible optionPayout"
                        + " (oracle 1/1/1 EXACT) must re-type — law 48's"
                        + " channel-zero-is-not-dead, executed at member grain");
        assertTrue(debtOption.contains(
                "return economicTerms == null ? Collections.<OptionPayout>emptyList() : "),
                "cdm5 T4 D4-live body: the whole-chain careful row must take the"
                        + " T1 TERMINAL_MULTI empty-coalescing ladder (the first"
                        + " cdm5 TERMINAL_MULTI landing — the § 8 T4 row's"
                        + " whole-chain receipt)");
        assertTrue(debtOption.contains(
                "MapperC.<OptionPayout>of(optionPayout(economicTerms))"
                        + ".<Product>map(\"getUnderlier\""),
                "cdm5 T4 D4-live seat: the oracle-enumerated DISGUISED invocation"
                        + " must ride the structural MapperC bridge (the seat the"
                        + " counting channels cannot see — the law-48 liveness"
                        + " witness consumed)");
    }

    /**
     * The D11 chaos expected-divergence rows as a PATH set (v3.2 PR-2, charter § 5):
     * {@code chaos/<version>/<KIND>:<path>} lines from the committed baseline at
     * {@code rune-java-generator/src/test/resources/chaos-expected-divergence.txt},
     * read cross-module by path (rune-java-generator publishes no test-jar — the
     * #464 law; the file stays the ONE declared-red set, this a deliberate further
     * READER). Used to tolerate the declared s18 refusals on the OPTIMISED route —
     * the same rows the default route tolerates (LAW 77).
     */
    private static volatile java.util.Set<String> chaosDeclaredRowsCache;

    // ---- v3.2 seat 10, round 1 (cq SF-1): the four chaos-1.1.0 tolerances as SETS / multisets (LAW 73), every
    // member from the fills-f5 print (target/v32-seat10-instruments/scratch/optnav-report-f5.log, local), never typed.
    /**
     * The 200 function-face refusals the baseline declares on this route (the [OPTNAV-REFUSED] lines). v3.2 seat 12
     * (D52, COUNTERS FIRST): 25 -> 112 at the counter tree - the three register sites (SWITCH_TERNARY_STUB,
     * DEEP_PATH_UTIL_UNRESOLVED, ALIAS_SIGNATURE_RAW_TYPE) refuse on this route EXACTLY the function files they refuse on the
     * default route (LAW 77 by identity - this compiler inherits the handlers; the set below EQUALS the D11's refused FUNCTION
     * paths at the same tree, target/v32-seat12-instruments/scratch/optnav-refused-c3pre.txt vs d11-refused-functions-a-off.txt,
     * local), the 25 of seat 10 all still present (shrink-only from here: a heal re-pins the set down).
     * v3.2 seat 13 (D53, commit 3): 112 -> 124 FROM THE PRINT (target/v32-seat13-instruments/scratch/optnav-fnface-
     * actual-c3a.txt, local) - 12 joined: C29Piped.java x 12 (R4 ALIAS_SIGNATURE_CLOSURE_PARAM_TYPE refuses C29Piped
     * at its alias signature on this route as on the default one, LAW 77 by identity; R6 ALIAS_BODY_ITEM_UNWRAPPED
     * fires on NO file here - the value-form re-seam of this route returns the count's int against an Integer value
     * seam, which compiles, so C23Scale stays an emitted, register-declared twin); the 112 of the previous tree all
     * still present (shrink-only from here).
     * v3.2 seat 13 (D53, commit commit 4): 124 -> 200 FROM THE PRINT (target/v32-seat13-instruments/scratch/optnav-
     * fnface-actual-c4i.txt, local) - 76 joined: C24IntoMeta.java x 13, C24Segments.java x 13, C24WithMetaExists.java
     * x 13, C24WithMetaPlain.java x 13, C30MultiThenAdd.java x 12, C30TwoOp.java x 12 (the four commit-4 register
     * sites fire on the optimised route by identity (LAW 77) - R5 C24WithMetaExists / C24WithMetaPlain 26, R13
     * C24IntoMeta 13, R14 C30MultiThenAdd 12, R10 C24Segments / C30TwoOp 25: the function-face set 124 -> 200 FROM THE
     * c4i PRINT (scratch/optnav-fnface-actual-c4i.txt, the optimised suite at the c4i overlay run); the alias-law set
     * UNMOVED (its assertion passed at the same run)); the 124 of the previous tree all still present (shrink-only
     * from here).
     */
    private static final java.util.Set<String> CHAOS_DECLARED_FUNCTION_FACE_REFUSALS = chaosPaths(
            "chaos/s17/x16enum/p2/functions/C17Sift.java", "chaos/s17/x36enum/p2/functions/C17Sift.java",
            "chaos/s24/a1o1/functions/C24IntoMeta.java", "chaos/s24/a1o1/functions/C24Segments.java",
            "chaos/s24/a1o1/functions/C24WithMetaExists.java", "chaos/s24/a1o1/functions/C24WithMetaPlain.java",
            "chaos/s24/a1o2/functions/C24IntoMeta.java", "chaos/s24/a1o2/functions/C24Segments.java",
            "chaos/s24/a1o2/functions/C24WithMetaExists.java", "chaos/s24/a1o2/functions/C24WithMetaPlain.java",
            "chaos/s24/a1o3/functions/C24IntoMeta.java", "chaos/s24/a1o3/functions/C24Segments.java",
            "chaos/s24/a1o3/functions/C24WithMetaExists.java", "chaos/s24/a1o3/functions/C24WithMetaPlain.java",
            "chaos/s24/a1o4/functions/C24IntoMeta.java", "chaos/s24/a1o4/functions/C24Segments.java",
            "chaos/s24/a1o4/functions/C24WithMetaExists.java", "chaos/s24/a1o4/functions/C24WithMetaPlain.java",
            "chaos/s24/a2alias/functions/C24IntoMeta.java", "chaos/s24/a2alias/functions/C24Segments.java",
            "chaos/s24/a2alias/functions/C24WithMetaExists.java", "chaos/s24/a2alias/functions/C24WithMetaPlain.java",
            "chaos/s24/a2dangle/functions/C24IntoMeta.java", "chaos/s24/a2dangle/functions/C24Segments.java",
            "chaos/s24/a2dangle/functions/C24WithMetaExists.java", "chaos/s24/a2dangle/functions/C24WithMetaPlain.java",
            "chaos/s24/a2qual/functions/C24IntoMeta.java", "chaos/s24/a2qual/functions/C24Segments.java",
            "chaos/s24/a2qual/functions/C24WithMetaExists.java", "chaos/s24/a2qual/functions/C24WithMetaPlain.java",
            "chaos/s24/a2wild/functions/C24IntoMeta.java", "chaos/s24/a2wild/functions/C24Segments.java",
            "chaos/s24/a2wild/functions/C24WithMetaExists.java", "chaos/s24/a2wild/functions/C24WithMetaPlain.java",
            "chaos/s24/a3half/p1/functions/C24IntoMeta.java", "chaos/s24/a3half/p1/functions/C24WithMetaExists.java",
            "chaos/s24/a3half/p1/functions/C24WithMetaPlain.java", "chaos/s24/a3half/p2/functions/C24Segments.java",
            "chaos/s24/a3hub/p2/functions/C24IntoMeta.java", "chaos/s24/a3hub/p2/functions/C24Segments.java",
            "chaos/s24/a3hub/p2/functions/C24WithMetaExists.java", "chaos/s24/a3hub/p2/functions/C24WithMetaPlain.java",
            "chaos/s24/a3third/p2/functions/C24IntoMeta.java", "chaos/s24/a3third/p2/functions/C24WithMetaExists.java",
            "chaos/s24/a3third/p2/functions/C24WithMetaPlain.java", "chaos/s24/a3third/p3/functions/C24Segments.java",
            "chaos/s24/base/functions/C24IntoMeta.java", "chaos/s24/base/functions/C24Segments.java",
            "chaos/s24/base/functions/C24WithMetaExists.java", "chaos/s24/base/functions/C24WithMetaPlain.java",
            "chaos/s24/x13half/p1/functions/C24Segments.java", "chaos/s24/x13half/p2/functions/C24IntoMeta.java",
            "chaos/s24/x13half/p2/functions/C24WithMetaExists.java", "chaos/s24/x13half/p2/functions/C24WithMetaPlain.java",
            "chaos/s25/a1o1/functions/C25Shadow.java", "chaos/s25/a2alias/functions/C25Shadow.java",
            "chaos/s25/a2dangle/functions/C25Shadow.java", "chaos/s25/a2qual/functions/C25Shadow.java",
            "chaos/s25/a2wild/functions/C25Shadow.java", "chaos/s25/a3half/p2/functions/C25Shadow.java",
            "chaos/s25/a3hub/p2/functions/C25Shadow.java", "chaos/s25/a3third/p3/functions/C25Shadow.java",
            "chaos/s25/base/functions/C25Shadow.java", "chaos/s26/a1o1/functions/C26AliasArms.java",
            "chaos/s26/a1o1/functions/C26Hoist.java", "chaos/s26/a1o1/functions/C26Lit.java",
            "chaos/s26/a1o1/functions/C26Nested.java", "chaos/s26/a1o2/functions/C26AliasArms.java",
            "chaos/s26/a1o2/functions/C26Hoist.java", "chaos/s26/a1o2/functions/C26Lit.java",
            "chaos/s26/a1o2/functions/C26Nested.java", "chaos/s26/a1o3/functions/C26AliasArms.java",
            "chaos/s26/a1o3/functions/C26Hoist.java", "chaos/s26/a1o3/functions/C26Lit.java",
            "chaos/s26/a1o3/functions/C26Nested.java", "chaos/s26/a1o4/functions/C26AliasArms.java",
            "chaos/s26/a1o4/functions/C26Hoist.java", "chaos/s26/a1o4/functions/C26Lit.java",
            "chaos/s26/a1o4/functions/C26Nested.java", "chaos/s26/a2alias/functions/C26AliasArms.java",
            "chaos/s26/a2alias/functions/C26Hoist.java", "chaos/s26/a2alias/functions/C26Lit.java",
            "chaos/s26/a2alias/functions/C26Nested.java", "chaos/s26/a2dangle/functions/C26AliasArms.java",
            "chaos/s26/a2dangle/functions/C26Hoist.java", "chaos/s26/a2dangle/functions/C26Lit.java",
            "chaos/s26/a2dangle/functions/C26Nested.java", "chaos/s26/a2qual/functions/C26AliasArms.java",
            "chaos/s26/a2qual/functions/C26Hoist.java", "chaos/s26/a2qual/functions/C26Lit.java",
            "chaos/s26/a2qual/functions/C26Nested.java", "chaos/s26/a2wild/functions/C26AliasArms.java",
            "chaos/s26/a2wild/functions/C26Hoist.java", "chaos/s26/a2wild/functions/C26Lit.java",
            "chaos/s26/a2wild/functions/C26Nested.java", "chaos/s26/a3half/p1/functions/C26AliasArms.java",
            "chaos/s26/a3half/p1/functions/C26Lit.java", "chaos/s26/a3half/p2/functions/C26Hoist.java",
            "chaos/s26/a3half/p2/functions/C26Nested.java", "chaos/s26/a3hub/p2/functions/C26AliasArms.java",
            "chaos/s26/a3hub/p2/functions/C26Hoist.java", "chaos/s26/a3hub/p2/functions/C26Lit.java",
            "chaos/s26/a3hub/p2/functions/C26Nested.java", "chaos/s26/a3third/p1/functions/C26Lit.java",
            "chaos/s26/a3third/p2/functions/C26AliasArms.java", "chaos/s26/a3third/p2/functions/C26Nested.java",
            "chaos/s26/a3third/p3/functions/C26Hoist.java", "chaos/s26/a4none/functions/C26Lit.java",
            "chaos/s26/a4snap/functions/C26Lit.java", "chaos/s26/a5crlf/functions/C26Lit.java",
            "chaos/s26/a5mixed/functions/C26Lit.java", "chaos/s26/a5uni/functions/C26Lit.java",
            "chaos/s26/a9comment/functions/C26Lit.java", "chaos/s26/a9tabs/functions/C26Lit.java",
            "chaos/s26/base/functions/C26AliasArms.java", "chaos/s26/base/functions/C26Hoist.java",
            "chaos/s26/base/functions/C26Lit.java", "chaos/s26/base/functions/C26Nested.java",
            "chaos/s29/a1o1/functions/C29LoL.java", "chaos/s29/a1o1/functions/C29Piped.java",
            "chaos/s29/a1o2/functions/C29LoL.java", "chaos/s29/a1o2/functions/C29Piped.java",
            "chaos/s29/a1o3/functions/C29LoL.java", "chaos/s29/a1o3/functions/C29Piped.java",
            "chaos/s29/a2alias/functions/C29LoL.java", "chaos/s29/a2alias/functions/C29Piped.java",
            "chaos/s29/a2dangle/functions/C29LoL.java", "chaos/s29/a2dangle/functions/C29Piped.java",
            "chaos/s29/a2qual/functions/C29LoL.java", "chaos/s29/a2qual/functions/C29Piped.java",
            "chaos/s29/a2wild/functions/C29LoL.java", "chaos/s29/a2wild/functions/C29Piped.java",
            "chaos/s29/a3half/p2/functions/C29LoL.java", "chaos/s29/a3half/p2/functions/C29Piped.java",
            "chaos/s29/a3hub/p2/functions/C29LoL.java", "chaos/s29/a3hub/p2/functions/C29Piped.java",
            "chaos/s29/a3third/p2/functions/C29LoL.java", "chaos/s29/a3third/p2/functions/C29Piped.java",
            "chaos/s29/base/functions/C29LoL.java", "chaos/s29/base/functions/C29Piped.java",
            "chaos/s29/x13half/p2/functions/C29LoL.java", "chaos/s29/x13half/p2/functions/C29Piped.java",
            "chaos/s30/a1o1/functions/C30MultiThenAdd.java", "chaos/s30/a1o1/functions/C30TwoOp.java",
            "chaos/s30/a1o2/functions/C30MultiThenAdd.java", "chaos/s30/a1o2/functions/C30TwoOp.java",
            "chaos/s30/a1o3/functions/C30MultiThenAdd.java", "chaos/s30/a1o3/functions/C30TwoOp.java",
            "chaos/s30/a2alias/functions/C30MultiThenAdd.java", "chaos/s30/a2alias/functions/C30TwoOp.java",
            "chaos/s30/a2dangle/functions/C30MultiThenAdd.java", "chaos/s30/a2dangle/functions/C30TwoOp.java",
            "chaos/s30/a2qual/functions/C30MultiThenAdd.java", "chaos/s30/a2qual/functions/C30TwoOp.java",
            "chaos/s30/a2wild/functions/C30MultiThenAdd.java", "chaos/s30/a2wild/functions/C30TwoOp.java",
            "chaos/s30/a3half/p2/functions/C30MultiThenAdd.java", "chaos/s30/a3half/p2/functions/C30TwoOp.java",
            "chaos/s30/a3hub/p2/functions/C30MultiThenAdd.java", "chaos/s30/a3hub/p2/functions/C30TwoOp.java",
            "chaos/s30/a3third/p2/functions/C30MultiThenAdd.java", "chaos/s30/a3third/p2/functions/C30TwoOp.java",
            "chaos/s30/base/functions/C30MultiThenAdd.java", "chaos/s30/base/functions/C30TwoOp.java",
            "chaos/s30/x13half/p2/functions/C30MultiThenAdd.java", "chaos/s30/x13half/p2/functions/C30TwoOp.java",
            "chaos/s31/a1o1/functions/C31Keys.java", "chaos/s31/a1o1/functions/C31Which.java",
            "chaos/s31/a1o2/functions/C31Keys.java", "chaos/s31/a1o2/functions/C31Which.java",
            "chaos/s31/a1o3/functions/C31Keys.java", "chaos/s31/a1o3/functions/C31Which.java",
            "chaos/s31/a2alias/functions/C31Keys.java", "chaos/s31/a2alias/functions/C31Which.java",
            "chaos/s31/a2dangle/functions/C31Keys.java", "chaos/s31/a2dangle/functions/C31Which.java",
            "chaos/s31/a2qual/functions/C31Keys.java", "chaos/s31/a2qual/functions/C31Which.java",
            "chaos/s31/a2wild/functions/C31Keys.java", "chaos/s31/a2wild/functions/C31Which.java",
            "chaos/s31/a3half/p2/functions/C31Keys.java", "chaos/s31/a3half/p2/functions/C31Which.java",
            "chaos/s31/a3hub/p2/functions/C31Keys.java", "chaos/s31/a3hub/p2/functions/C31Which.java",
            "chaos/s31/a3third/p3/functions/C31Keys.java", "chaos/s31/a3third/p3/functions/C31Which.java",
            "chaos/s31/a6enum/functions/C31Keys.java", "chaos/s31/a6enum/functions/C31Which.java",
            "chaos/s31/base/functions/C31Keys.java", "chaos/s31/base/functions/C31Which.java",
            "chaos/s31/x16enum/p2/functions/C31Defaults.java", "chaos/s31/x16enum/p2/functions/C31Keys.java",
            "chaos/s31/x16enum/p2/functions/C31Which.java", "chaos/s31/x26enum/functions/C31Keys.java",
            "chaos/s31/x26enum/functions/C31Which.java", "chaos/s31/x36enum/p2/functions/C31Defaults.java",
            "chaos/s31/x36enum/p2/functions/C31Keys.java", "chaos/s31/x36enum/p2/functions/C31Which.java",
            "chaos/s93/base/functions/C93Nav.java", "chaos/s94/base/functions/C94PickBoth.java");
    /**
     * The 35 rule-face refusals the baseline declares on this route (the [OPTNAV-REFUSED-RULEFACE] lines). v3.2 seat 12
     * (D52, COUNTERS FIRST): 23 -> 35 at the counter tree - the seat-10 23 (s26 / s28) all still present and the 12
     * C29InnerRule report files joined, refused at DEEP_PATH_UTIL_UNRESOLVED (R2: the deep-path call whose util the
     * writer never emitted - M7b, the `TODO` stub the rule face used to render silently). Read from the second run's
     * print (target/v32-seat12-instruments/scratch/optnav-refused-ruleface-c3pre2.txt, local); shrink-only from here.
     */
    private static final java.util.Set<String> CHAOS_DECLARED_RULE_FACE_REFUSALS = chaosPaths(
            "chaos/s26/a1o2/reports/C26PickAvRule.java", "chaos/s26/a1o3/reports/C26PickAvRule.java",
            "chaos/s26/a1o4/reports/C26PickAvRule.java", "chaos/s26/a2alias/reports/C26PickAvRule.java",
            "chaos/s26/a2dangle/reports/C26PickAvRule.java", "chaos/s26/a2qual/reports/C26PickAvRule.java",
            "chaos/s26/a2wild/reports/C26PickAvRule.java", "chaos/s26/a3half/p2/reports/C26PickAvRule.java",
            "chaos/s26/a3hub/p2/reports/C26PickAvRule.java", "chaos/s26/a3third/p3/reports/C26PickAvRule.java",
            "chaos/s26/base/reports/C26PickAvRule.java", "chaos/s28/a1o1/reports/C28ALTC28RegReportFunction.java",
            "chaos/s28/a1o2/reports/C28ALTC28RegReportFunction.java", "chaos/s28/a1o3/reports/C28ALTC28RegReportFunction.java",
            "chaos/s28/a1o4/reports/C28ALTC28RegReportFunction.java", "chaos/s28/a2alias/reports/C28ALTC28RegReportFunction.java",
            "chaos/s28/a2dangle/reports/C28ALTC28RegReportFunction.java", "chaos/s28/a2qual/reports/C28ALTC28RegReportFunction.java",
            "chaos/s28/a2wild/reports/C28ALTC28RegReportFunction.java", "chaos/s28/a3half/p2/reports/C28ALTC28RegReportFunction.java",
            "chaos/s28/a3hub/p2/reports/C28ALTC28RegReportFunction.java", "chaos/s28/a3third/p3/reports/C28ALTC28RegReportFunction.java",
            "chaos/s28/base/reports/C28ALTC28RegReportFunction.java", "chaos/s29/a1o1/reports/C29InnerRule.java",
            "chaos/s29/a1o2/reports/C29InnerRule.java", "chaos/s29/a1o3/reports/C29InnerRule.java",
            "chaos/s29/a2alias/reports/C29InnerRule.java", "chaos/s29/a2dangle/reports/C29InnerRule.java",
            "chaos/s29/a2qual/reports/C29InnerRule.java", "chaos/s29/a2wild/reports/C29InnerRule.java",
            "chaos/s29/a3half/p2/reports/C29InnerRule.java", "chaos/s29/a3hub/p2/reports/C29InnerRule.java",
            "chaos/s29/a3third/p3/reports/C29InnerRule.java", "chaos/s29/base/reports/C29InnerRule.java",
            "chaos/s29/x13half/p1/reports/C29InnerRule.java");
    /**
     * The 134 COMPLETENESS shortfall sites by source file (the OPTNAV-SHORTFALL `at=` keys): s24's Void-typed
     * `getTok` SINGLE sites (M3 on this route) and s31's split-p2 `getTop` / `getMove` (D48's class) - the seat-10
     * 56 - and, since v3.2 seat 13 commit 4 (D53), the walk-visible sites of the s24 FUNCTIONS this route now REFUSES
     * by identity with the default route (LAW 77): C24WithMetaExists / C24WithMetaPlain at R5 WITH_META_UNTYPED_STUB,
     * C24IntoMeta at R13 VOID_INTO_META_OUTPUT, C24Segments at R10 HOIST_ARM_COERCION_DROPPED - a refused function
     * emits nothing, so its eligible sites stay on the Mapper idiom by construction: 56 -> 134, the rise {chaos-s24-a1o1.rosetta +6, chaos-s24-a1o2.rosetta +6, chaos-s24-a1o3.rosetta +6, chaos-s24-a1o4.rosetta +6, chaos-s24-a2alias.rosetta +6, chaos-s24-a2dangle.rosetta +6, chaos-s24-a2qual.rosetta +6, chaos-s24-a2wild.rosetta +6, chaos-s24-a3half-p2.rosetta +6, chaos-s24-a3hub-p2.rosetta +6, chaos-s24-a3third-p3.rosetta +6, chaos-s24-base.rosetta +6, chaos-s24-x13half-p1.rosetta +6} per file
     * READ from the optimised suite's own print at the commit-4 tree (target/v32-seat13-instruments/scratch/
     * ov-optimised-c4j.log, local; written by patch-optimised-shortfall-s13.py). EQUALITY-pinned, shrink-only from
     * here: a fall is a heal (v3.3's) to re-pin, a rise a NEW optimised-route class to census.
     */
    private static final java.util.Map<String, Integer> CHAOS_DECLARED_SHORTFALL_BY_FILE = chaosCounts(
            "chaos-s24-a1o1.rosetta", 10, "chaos-s24-a1o2.rosetta", 10, "chaos-s24-a1o3.rosetta", 10,
            "chaos-s24-a1o4.rosetta", 10, "chaos-s24-a2alias.rosetta", 10, "chaos-s24-a2dangle.rosetta", 10,
            "chaos-s24-a2qual.rosetta", 10, "chaos-s24-a2wild.rosetta", 10, "chaos-s24-a3half-p2.rosetta", 10,
            "chaos-s24-a3hub-p2.rosetta", 10, "chaos-s24-a3third-p2.rosetta", 2, "chaos-s24-a3third-p3.rosetta", 8,
            "chaos-s24-base.rosetta", 10, "chaos-s24-x13half-p1.rosetta", 8, "chaos-s24-x13half-p2.rosetta", 2,
            "chaos-s31-x16enum-p2.rosetta", 2, "chaos-s31-x36enum-p2.rosetta", 2);
    /**
     * The 12 witness refusals by member id (the witnessRefusalMembers print): s29's piped lambda, twelve variants -
     * the seam the nested closure parameter's NAME as the Java type (M7c, seat 13's). v3.2 seat 12 (D52, COUNTERS
     * FIRST): 72 -> 12 at the counter tree - s26's five C26AliasArms switch aliases (5 x 12 = 60) are GONE from this
     * print because the FILE refuses at the alias SIGNATURE (ALIAS_SIGNATURE_RAW_TYPE, the R3 site) before the seam
     * policy walks any member; read from the third run's print (target/v32-seat12-instruments/scratch/
     * witness-refusals-c3pre3.txt, local); shrink-only from here (the M7c heal re-pins it to zero).
     * v3.2 seat 13 (D53, commit 3): 12 -> 0 FROM THE PRINT (target/v32-seat13-instruments/scratch/ov-
     * optimised-c3b.log, the assertion's `but was` map) - C29Piped.piped seam=MapperC<l2> element=l2
     * plan=none/top=PIPE x 12 GONE from this print because the FILE refuses at a seat-13 site before the seam policy
     * walks any member (R4 ALIAS_SIGNATURE_CLOSURE_PARAM_TYPE refuses C29Piped at its alias signature on this route as
     * on the default one, LAW 77 by identity; R6 ALIAS_BODY_ITEM_UNWRAPPED fires on NO file here - the value-form re-
     * seam of this route returns the count's int against an Integer value seam, which compiles, so C23Scale stays an
     * emitted, register-declared twin); shrink-only from here.
     */
    private static final java.util.Map<String, Integer> CHAOS_DECLARED_WITNESS_REFUSALS = chaosCounts();

    private static java.util.Set<String> chaosPaths(String... paths) {
        java.util.TreeSet<String> out = new java.util.TreeSet<>();
        for (String p : paths) {
            if (!out.add(p)) {
                throw new IllegalStateException("duplicate declared path: " + p);
            }
        }
        return java.util.Collections.unmodifiableSet(out);
    }

    private static java.util.Map<String, Integer> chaosCounts(Object... keyCountPairs) {
        java.util.TreeMap<String, Integer> out = new java.util.TreeMap<>();
        for (int i = 0; i < keyCountPairs.length; i += 2) {
            if (out.put((String) keyCountPairs[i], (Integer) keyCountPairs[i + 1]) != null) {
                throw new IllegalStateException("duplicate declared key: " + keyCountPairs[i]);
            }
        }
        return java.util.Collections.unmodifiableMap(out);
    }

    private static java.util.Set<String> chaosDeclaredDivergenceRows() {
        java.util.Set<String> cached = chaosDeclaredRowsCache;
        if (cached != null) {
            return cached;
        }
        java.nio.file.Path baseline = java.nio.file.Path.of(
                "../rune-java-generator/src/test/resources/chaos-expected-divergence.txt");
        if (!java.nio.file.Files.isRegularFile(baseline)) {
            throw new IllegalStateException("chaos expected-divergence baseline missing: "
                    + baseline + " — broken checkout (committed, never generated)");
        }
        java.util.Set<String> rows = new java.util.TreeSet<>();
        try {
            int lineNo = 0;
            for (String raw : java.nio.file.Files.readAllLines(baseline,
                    java.nio.charset.StandardCharsets.UTF_8)) {
                lineNo++;
                String line = raw.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                // ONE strict law across every reader of this file (the cq review's
                // MF-2): a malformed row fails loud, never narrows the set silently.
                int colon = line.indexOf(':');
                if (colon <= 0 || colon >= line.length() - 1) {
                    throw new IllegalStateException(baseline + ":" + lineNo
                            + " — malformed baseline row (expected '<key>:<path>'): " + line);
                }
                rows.add(line.substring(colon + 1));
            }
        } catch (IOException e) {
            throw new java.io.UncheckedIOException("cannot read " + baseline, e);
        }
        if (rows.isEmpty()) {
            throw new IllegalStateException(baseline + " carries no rows — at v3.2 close the"
                    + " baseline is DELETED, and this tolerance leg goes with it");
        }
        chaosDeclaredRowsCache = java.util.Set.copyOf(rows);
        return chaosDeclaredRowsCache;
    }
}
