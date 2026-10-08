package com.regnosys.rosetta.generator.java.optimised;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.RRootElement;
import com.regnosys.rosetta.ast.expressions.references.RDeepFeatureCall;
import com.regnosys.rosetta.ast.expressions.references.RFeatureCall;
import com.regnosys.rosetta.ast.expressions.references.RSymbolReference;
import com.regnosys.rosetta.ast.expressions.supporting.ROnlyExistsElement;
import com.regnosys.rosetta.ast.functions.RCondition;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.functions.ROperation;
import com.regnosys.rosetta.ast.functions.RPostCondition;
import com.regnosys.rosetta.ast.functions.RRule;
import com.regnosys.rosetta.ast.functions.RShortcut;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.types.RChoice;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.generator.java.function.FunctionAliasHelper;
import com.regnosys.rosetta.generator.java.function.FunctionTemplateModel;
import com.regnosys.rosetta.ir.expr.FieldAccess;
import com.regnosys.rosetta.ir.expr.IRExpr;
import com.regnosys.rosetta.ir.expr.IRExprKind;
import com.regnosys.rosetta.ir.expr.IRMetaAccess;
import com.regnosys.rosetta.ir.expr.IRReference;
import com.regnosys.rosetta.ir.expr.IRVariable;
import com.regnosys.rosetta.ir.expr.adapter.ExpressionToIRAdapter;
import com.regnosys.rosetta.symbols.RLinkingResult;
import com.regnosys.rosetta.symbols.RWorkspace;
import com.regnosys.rosetta.types.ExpressionCardinality;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE PR-4 FAMILY CENSUS INSTRUMENT (the plan § 7 step 1 — census-then-price): the
 * navigation-chain population over the five frozen cells, measured DIRECTLY on the IR the
 * optimised backend consumes (not via the reference backend's dispatch-leg counters, which
 * label render routing, not semantic shape — the sharpen-the-instrument move).
 *
 * <p><b>What a site is.</b> A <em>maximal navigation chain</em>: a receiver-carrying nav node
 * ({@code FIELD_ACCESS} / {@code META_ACCESS} / {@code DEEP_FEATURE_NAV} /
 * {@code RECORD_RECEIVER_NAV} / {@code QUALIFIER_RECEIVER_NAV} / {@code CHOICE_RECEIVER_NAV})
 * whose consumer is not itself a nav node navigating off it, walked down its receiver spine to
 * the first non-nav root. Childless nav-shaped kinds (RULE_INPUT_NAV, IMPLICIT_ATTR_NAV,
 * SYN_ITEM_NAV, …) carry no receiver child by design and are classified as chain ROOTS when a
 * chain stands on them.
 *
 * <p><b>What the census answers</b> (the emission-design pricing inputs):
 * <ol>
 *   <li><b>Face × context:</b> which of the 11 kinds carry the family (FUNCTION operation /
 *       alias / condition / postCondition; RULE body; DATA_RULE condition body) — the contexts
 *       differ in TEXT-OBSERVABILITY (DATA_RULE failure text is O2-compared byte-for-byte;
 *       function condition text surfaces through thrown behaviour O3; operation/alias values
 *       discard ComparisonResult text).</li>
 *   <li><b>Shape:</b> pure-FieldAccess chains (every hop carries full IR facts — the
 *       direct-emittable core) vs meta-bearing (FieldWithMeta vs ReferenceWithMeta wrapper
 *       hops — the O7-sensitive class) vs deep/record/qualifier/choice-link-bearing vs
 *       shallow-rooted (a childless nav kind at the root, whose render is legacy-identity).</li>
 *   <li><b>Cardinality:</b> all-single chains (a direct null-ladder + one boundary wrap
 *       reproduces value semantics; failure paths static per break position) vs multi-bearing
 *       (list semantics + index-bearing Mapper paths).</li>
 *   <li><b>Fact completeness</b> (the step-B adapter-carry question): per-hop
 *       {@code type().isMissing()} counts — a missing hop type is exactly an additive-enrichment
 *       candidate.</li>
 *   <li><b>The alias population and its consumers</b> (census § 19 — the § 6.3 alias re-typing
 *       family's census stage): per-cell {@code RShortcut} declarations with the render's own
 *       {@code usesOutput} split ({@code FunctionAliasHelper.analyze}), the non-usesOutput
 *       seam-kind split on the IR cardinality channel, alias-body shape rows, and the
 *       cross-method CONSUMER census on TWO COMPLEMENTARY channels — the AST channel
 *       ({@code aliasBoundRef*}: the linker-complete consumer taxonomy; carries the
 *       {@code ONLY_EXISTS_ELEMENT} path-semantic blocker class, invisible at the IR by the
 *       adapter's own exclusion) and the IR channel ({@code aliasIrRef*}: the
 *       spelling-normalized walk-visible view — the IR normalizes the bound, name-match and
 *       qualified-chain spellings to {@code IRReference&#123;ALIAS&#125;}; its
 *       {@code CHAIN_ROOT} row is the § 2 byRoot REFERENCE dual, cross-pinned). All on rows
 *       DISJOINT from the standing censuses.</li>
 * </ol>
 *
 * <p>The instrument RECORDS (census-then-price): assertions pin structural sanity and
 * conservation, never distributions. Receipt channel: the {@code NAVCENSUS} stdout lines,
 * captured to {@code target-<pr>-navcensus<n>.log}.
 */
class NavigationChainCensusTest {

    /**
     * The receiver-carrying nav kinds — each contributes exactly one hop and one
     * receiver child. Package-visible (not private) so the § 6.3 disposition-map
     * instrument ({@code AliasDispositionMapTest}) descends chains with the SAME
     * kind set — one definition, no drift; unmodifiable so a sharing test can
     * never mutate it (Copilot #557 R1).
     */
    static final Set<IRExprKind> CHAIN_LINK_KINDS = java.util.Collections.unmodifiableSet(
            EnumSet.of(
                    IRExprKind.FIELD_ACCESS,
                    IRExprKind.META_ACCESS,
                    IRExprKind.DEEP_FEATURE_NAV,
                    IRExprKind.RECORD_RECEIVER_NAV,
                    IRExprKind.QUALIFIER_RECEIVER_NAV,
                    IRExprKind.CHOICE_RECEIVER_NAV));

    /** The childless nav-shaped kinds — legal chain ROOTS whose own render is legacy-identity. */
    private static final Set<IRExprKind> SHALLOW_NAV_ROOT_KINDS = EnumSet.of(
            IRExprKind.SYMBOL_NAV,
            IRExprKind.SYN_ITEM_NAV,
            IRExprKind.CHOICE_OPTION_NAV,
            IRExprKind.RECORD_FEATURE_NAV,
            IRExprKind.META_ITEM_NAV,
            IRExprKind.QUALIFIER_ITEM_NAV,
            IRExprKind.RULE_INPUT_NAV,
            IRExprKind.IMPLICIT_ATTR_NAV,
            IRExprKind.OUTPUT_ALIAS_NAV,
            IRExprKind.DISPATCH_INPUT_REF,
            IRExprKind.META_PARAM_REF,
            IRExprKind.OUTPUT_REF);

    // ------------------------------------------------------------------ the census aggregate --

    /** One (cell, face) aggregation bucket — every counter the census prints. */
    private static final class Bucket {
        int bodies;
        int adapted;
        int adaptEmpty;
        int chains;
        int pureAllSingle;
        int pureMulti;
        int metaBearing;
        int fieldMetaHops;
        int refMetaHops;
        int otherLinkBearing;
        int shallowRooted;
        int hopTypeMissing;
        int hopTypeMissingOnPure;
        int rootTypeMissing;
        int rootTypeMissingOnPure;
        final Map<String, Integer> byContext = new TreeMap<>();
        final Map<Integer, Integer> byHopCount = new TreeMap<>();
        final Map<String, Integer> byRoot = new TreeMap<>();
        final Map<String, Integer> byConsumer = new TreeMap<>();
        // THE SITE CENSUS — the emitter-altitude count: every AST maximal-top candidate
        // accepted by the SHARED NavigationChainClassifier, per body context. Since
        // the tranche-2 widening the classifier admits multi-cardinality shapes too:
        // the operation-context SINGLE shape row IS the tranche-1 priced scope
        // (receipt-comparable with the PR #539 census), and the TERMINAL_MULTI /
        // STREAM rows price tranche 2. The emission reconciliation gate enforces the
        // converting pool by IDENTITY CONTAINMENT (shortfall == 0 — every walk site
        // converted) while its pinned EVENT counts sit HIGHER by design
        // (render-synthesized twins + dispatch re-renders + excluded-top prefixes —
        // the census doc § 4 decode). (The IR-walk counters above measure the
        // population VISIBLE in top-level adapted trees — shape/fact analytics; sites
        // inside shallow-node interiors are visible only to this AST walk, which is
        // why those two counts differ as well.)
        final Map<String, Integer> siteEligibleByContext = new TreeMap<>();
        final Map<String, Integer> siteShapeByContext = new TreeMap<>();
        final Map<Integer, Integer> siteEligibleOperationByHops = new TreeMap<>();
        final Map<Integer, Integer> siteOperationHopsTerminalMulti = new TreeMap<>();
        final Map<Integer, Integer> siteOperationHopsStream = new TreeMap<>();
        final Map<Integer, Integer> siteOperationStreamMultiPoints = new TreeMap<>();
        int siteOperationRootMulti;
        // THE FAMILY-2 META POOL (census § 10 — the PR-6 widening): meta-BEARING
        // plans tally into their OWN rows (the meta-free rows above stay
        // receipt-comparable with the § 4/§ 9 receipts). Step maps key on the
        // ACCESSOR-STEP count (the ladder length — interior getValue derefs baked),
        // not the DSL hop count.
        final Map<String, Integer> siteOperationMetaByShape = new TreeMap<>();
        final Map<Integer, Integer> siteOperationMetaStepsSingle = new TreeMap<>();
        final Map<Integer, Integer> siteOperationMetaStepsTerminalMulti = new TreeMap<>();
        final Map<Integer, Integer> siteOperationMetaStepsStream = new TreeMap<>();
        final Map<Integer, Integer> siteOperationMetaStreamMultiPoints = new TreeMap<>();
        int siteOperationMetaRootMulti;
        int siteOperationMetaFieldHops;
        int siteOperationMetaRefHops;
        final Map<String, Integer> siteOperationMetaTopKind = new TreeMap<>();
        final Map<String, Integer> siteOperationMetaTopTypeMissingByShape = new TreeMap<>();
        // THE FAMILY-3 POOLS (census § 12 — the rule-face census stage), on DISJOINT
        // rows so every § 4/§ 9/§ 10 site row stays receipt-comparable:
        //   (a) the BODY-context PARAM pool — the rule face's input-rooted chains,
        //       classifiable only through the synthetic-owner bridge
        //       (RFunction.fromRule — the same bridge RuleGenerator emits through);
        //   (b) the ITEM-root pool (ANY context) — SYNTHETIC_ITEM-rooted plans, the
        //       classifier's family-3 admission (the emitter declines them all
        //       behind its pending pins this stage). Keys compose
        //       context:SHAPE[:steps][:meta][:wroot] — :meta = meta involvement
        //       (hops and/or root), :wroot = the wrapper-typed-root subset (the
        //       leading-deref class).
        final Map<String, Integer> siteBodyParamByShape = new TreeMap<>();
        final Map<String, Integer> siteBodyParamBySteps = new TreeMap<>();
        int siteBodyParamRootMulti;
        int siteBodyParamRootTypeMissing;
        int siteBodyParamTopTypeMissingMulti;
        final Map<String, Integer> siteItemByContext = new TreeMap<>();
        final Map<String, Integer> siteItemByShape = new TreeMap<>();
        final Map<String, Integer> siteItemBySteps = new TreeMap<>();
        int siteItemWrappedRef;
        int siteItemWrappedField;
        int siteItemRootTypeMissing;
        int siteItemRootMulti;
        int siteItemTopTypeMissingMulti;
        // THE § 6.3 ALIAS CENSUS (census § 19 — the alias re-typing family, the
        // census stage; NO emission rides this leg): the alias POPULATION + the
        // cross-method CONSUMER census, on DISJOINT rows — the alias leg is a
        // SEPARATE walk over the same bodies that bumps ONLY alias* rows, so every
        // standing § 4/§ 9/§ 10/§ 12 row stays receipt-comparable byte-for-byte.
        //   aliasDecls/aliasUsesOutput — the population and the render's OWN
        //     usesOutput split (FunctionAliasHelper.analyze — the builder-seam
        //     class is OUT of the Mapper-seam family);
        //   aliasSeamByKind — the non-usesOutput seam split on the IR CARDINALITY
        //     channel (MULTI → the MapperC seam, SINGLE → MapperS; the render's own
        //     split is FunctionAliasHelper's typeInfo.isMulti at
        //     buildMapperReturnType — same fact family, reconciled at any edit
        //     PR's differential gate); adaptEmpty = its own row (unclassifiable
        //     at IR altitude, counted never dropped);
        //   aliasBodyTopKind — the body's top-level IR kind (which alias bodies
        //     are nav chains vs applies vs composites);
        //   aliasBodyWholeChain — the body IS one shared-classifier-admitted
        //     chain (rootKind:Shape[:meta] keys — the cleanest body-re-emission
        //     class; ALIAS-rooted bodies refuse at the classifier and ride the
        //     consumer rows instead);
        //   aliasBoundRef* — the AST channel, THE LINKER-COMPLETE CONSUMER
        //     TAXONOMY (primary for consumer shapes and blocker classes): every
        //     LINKER-BOUND RSymbolReference resolving to an RShortcut, per body
        //     context and per consumer parent shape (ONLY_EXISTS_ELEMENT = the
        //     path-semantic blocker class, § 4 guard 1 — countable ONLY on this
        //     channel: the adapter deliberately never mints an alias reference
        //     inside an only-exists subtree);
        //   aliasIrRef* — the IR channel, THE SPELLING-NORMALIZED WALK-VISIBLE
        //     VIEW (primary for chain facts): the IR is the normalizer (§ 4), so
        //     the linker-bound reference, the resolution-free name-match (the
        //     adapter's #502 arm-1, mirroring legacy isAliasReference) and the
        //     qualified-chain head all arrive as one IRReference{ALIAS} node;
        //     rows key the IR consumer kind — CHAIN_ROOT = a chain navigates off
        //     the alias, the § 2 byRoot REFERENCE dual across ALL spellings,
        //     cross-pinned below. The channels count DIFFERENT populations by
        //     construction (neither subsumes the other): the IR side ADDS the
        //     name-match/qualified spellings the linker cannot see and DROPS the
        //     seats inside interiors the IR walk cannot see (only-exists
        //     subtrees — the adapter's own exclusion — and composite-kind
        //     interiors like constructor field values: the standing
        //     walk-visibility caveat above);
        //   alias*RefsPerAlias — the per-alias consumer-count distributions
        //     (re-evaluation-per-reference is today's call-by-name semantics —
        //     the multiplicity fact any re-typing must preserve). A 0-count
        //     alias is ZERO-CHANNEL-VISIBLE, NOT proven dead: the disguised
        //     qualified-name spelling (an REnumValueRef the adapter routes to
        //     adaptDisguisedInputNav — never an alias mint) is visible to
        //     NEITHER channel, and the corpus witness renders SIX alias
        //     invocations from it (census § 19c). Liveness per member needs
        //     the golden-invocation oracle at any edit PR;
        //   aliasAssignTargets — the THIRD possible consumption channel
        //     (`set <aliasName> -> seg:` — the assign-target facet): a raw
        //     name string invisible to both walks; corpus-EMPTY today and
        //     PINNED zero so a corpus bump reopens census § 19c before the
        //     consumer rows are trusted.
        //   aliasBearingFuncs/aliasBearingClassNames — the O5 scope grains: the
        //     RFunction units declaring ≥1 non-usesOutput alias, and the DISTINCT
        //     namespace-qualified function names among them (dispatch cases share
        //     one name+namespace = one generated top-level class file, while
        //     same-named functions in DIFFERENT namespaces — the drr per-regime
        //     packages — are distinct files, so the qualified-name grain is the
        //     generated-class-file proxy the API-delta report counts in).
        int aliasDecls;
        int aliasUsesOutput;
        int aliasRefOnlyExists;
        int aliasBearingFuncs;
        int aliasAssignTargets;
        final Set<String> aliasBearingClassNames = new HashSet<>();
        final Map<String, Integer> aliasSeamByKind = new TreeMap<>();
        final Map<String, Integer> aliasBodyTopKind = new TreeMap<>();
        final Map<String, Integer> aliasBodyWholeChain = new TreeMap<>();
        final Map<String, Integer> aliasBoundRefByContext = new TreeMap<>();
        final Map<String, Integer> aliasBoundRefConsumer = new TreeMap<>();
        final Map<Integer, Integer> aliasBoundRefsPerAlias = new TreeMap<>();
        final Map<String, Integer> aliasIrRefByContext = new TreeMap<>();
        final Map<String, Integer> aliasIrRefConsumer = new TreeMap<>();
        final Map<Integer, Integer> aliasIrRefsPerAlias = new TreeMap<>();

        static void bump(Map<String, Integer> map, String key) {
            map.merge(key, 1, Integer::sum);
        }
    }

    /** The SHARED family classifier (tranches 1 + 2) — the same instance kind the emitter runs. */
    private final NavigationChainClassifier siteClassifier = new NavigationChainClassifier();

    /**
     * The render's OWN alias analyser (census § 19) — the zero-arg construction is
     * safe by that class's design (every typing path null-guards on the missing
     * type infrastructure; the {@code usesOutput} decision — including the
     * outputBuilderNav clause — is a pure AST walk), so the census's
     * usesOutput split IS {@code FunctionGenerator}'s emission decision, never a
     * mirror. The degraded returnType strings the zero-arg fallback produces are
     * NEVER read here (the seam-kind split rides the IR cardinality channel — see
     * the Bucket rows). Sequential use only (the helper is stateful per function,
     * its own thread-safety note) — this census walks cells sequentially.
     */
    private final FunctionAliasHelper aliasHelper = new FunctionAliasHelper();

    @Test
    void navigationChainCensusOverTheFiveCells() throws IOException {
        // The name predates PR #607: the walk now covers every active catalogue
        // cell of the corpus SOT (25), the census rows recorded per cell as before.
        Assumptions.assumeTrue(CorpusCells.corpusStaged(),
                "test-corpus not staged — skipped");
        Assumptions.assumeTrue(!CorpusCells.resolveBuiltinFiles().isEmpty(),
                "rune-dsl builtins absent (searched " + CorpusCells.BUILTINS_SEARCH_ROOTS
                        + ") — skipped");

        for (CorpusCells.CellSpec cell : CorpusCells.cellsUnderTest()) {
            Assumptions.assumeTrue(CorpusCells.staged(cell),
                    "test-corpus " + cell.ownDir() + " not staged — skipped");
            censusCell(cell);
        }
    }

    private void censusCell(CorpusCells.CellSpec cell) throws IOException {
        // The cell's OWN models are walked exclusively — never the deps (the D11 per-cell
        // accounting law; dep expressions are counted in their own cell's row).
        CorpusCells.LoadedCell loaded = CorpusCells.load(cell);
        RWorkspace ws = loaded.workspace();
        List<RModel> ownModels = loaded.ownModels();

        ExpressionToIRAdapter adapter = new ExpressionToIRAdapter();
        Map<String, Bucket> buckets = new LinkedHashMap<>();
        buckets.put("FUNCTION", new Bucket());
        buckets.put("RULE", new Bucket());
        buckets.put("DATA_RULE", new Bucket());
        int choiceOneOfSynthetics = 0;

        for (RModel model : ownModels) {
            for (RRootElement element : model.rootElements()) {
                if (element instanceof RFunction func) {
                    Bucket b = buckets.get("FUNCTION");
                    for (ROperation op : func.operations()) {
                        censusBody(adapter, ws, b, "operation", op.expression(), func);
                    }
                    for (RShortcut alias : func.shortcuts()) {
                        censusBody(adapter, ws, b, "alias", alias.expression(), func);
                    }
                    for (RCondition cond : func.conditions()) {
                        censusBody(adapter, ws, b, "condition", cond.expression(), func);
                    }
                    for (RPostCondition post : func.postConditions()) {
                        censusBody(adapter, ws, b, "postCondition", post.expression(), func);
                    }
                    censusAliasLeg(adapter, ws, b, func, model.namespace());
                } else if (element instanceof RRule rule) {
                    Bucket b = buckets.get("RULE");
                    Optional<RExpression> body = rule.expression();
                    if (body.isPresent()) {
                        // THE SYNTHETIC-OWNER BRIDGE (census § 12): the SAME bridge the
                        // rule-face emission renders through (RuleGenerator.streamObjects
                        // → RFunction.fromRule — one synthetic input named "input", the
                        // bare rule-input reads' PARAM root name). The bridge is
                        // PARENTLESS by construction, so the classifier's
                        // declaration-type channel is unavailable (its attachment
                        // gate) and rule-face root typing rides the node channel —
                        // measured by the siteBodyParamFacts row; the landed
                        // SINGLE/TERMINAL_MULTI swap forms never consume the root
                        // type. (The REPORT face carries no census bucket: its
                        // operation expressions are SYNTHESIZED rule-invocation refs
                        // — ReportGenerator.synthesizeReportOperations builds
                        // RSymbolReference(rule, input) per reported field, no
                        // navigation nodes by construction; the § 12 emission stage
                        // proves the zero empirically on its own OPTNAV counters.)
                        censusBody(adapter, ws, b, "body", body.get(),
                                RFunction.fromRule(rule));
                    }
                } else if (element instanceof RDataType data) {
                    Bucket b = buckets.get("DATA_RULE");
                    for (RCondition cond : data.conditions()) {
                        censusBody(adapter, ws, b, "condition", cond.expression(), null);
                    }
                } else if (element instanceof RChoice) {
                    // The choice one-of DATA_RULE twin is generator-SYNTHESIZED (no source
                    // expression; the render is the nav-free choice(MapperS.of(x), …) form) —
                    // counted for an honest denominator, out of the navigation family.
                    choiceOneOfSynthetics++;
                }
            }
        }

        for (Map.Entry<String, Bucket> e : buckets.entrySet()) {
            printBucket(cell.label(), e.getKey(), e.getValue());
        }
        System.out.println("NAVCENSUS " + cell.label() + " choiceOneOfSynthetics=" + choiceOneOfSynthetics);

        // Structural pins only (census-then-price: distributions are RECORDED, never gated).
        for (Map.Entry<String, Bucket> e : buckets.entrySet()) {
            Bucket b = e.getValue();
            assertEquals(b.chains,
                    b.pureAllSingle + b.pureMulti + b.metaBearing + b.otherLinkBearing + b.shallowRooted,
                    cell.label() + " " + e.getKey() + ": the shape partition must conserve to the chain total");
            assertEquals(b.bodies, b.adapted + b.adaptEmpty,
                    cell.label() + " " + e.getKey() + ": adapted + adaptEmpty must conserve to bodies");
        }
        // THE § 19 ALIAS-LEG CONSERVATION PINS (structural only — distributions
        // stay RECORDED per census-then-price): every alias reference is
        // classified exactly once on both consumer channels; the per-alias
        // distribution conserves to both the declaration and the reference
        // totals; the leg is FUNCTION-face-only by grammar (shortcuts are
        // function-scoped — the other faces' alias rows must stay empty).
        Bucket functionBucket = buckets.get("FUNCTION");
        assertAliasChannelConserves(cell, "aliasBoundRef (AST channel)",
                functionBucket.aliasBoundRefByContext, functionBucket.aliasBoundRefConsumer,
                functionBucket.aliasBoundRefsPerAlias, functionBucket.aliasDecls);
        assertAliasChannelConserves(cell, "aliasIrRef (IR channel)",
                functionBucket.aliasIrRefByContext, functionBucket.aliasIrRefConsumer,
                functionBucket.aliasIrRefsPerAlias, functionBucket.aliasDecls);
        int aliasSeams = functionBucket.aliasSeamByKind.values().stream()
                .mapToInt(Integer::intValue).sum();
        assertEquals(functionBucket.aliasDecls - functionBucket.aliasUsesOutput, aliasSeams,
                cell.label() + " FUNCTION: every non-usesOutput alias lands exactly one"
                        + " seam-kind row");
        // The assign-target zero-pin (census § 19c blind spot (b) — a CORPUS-fact
        // pin whose firing means a corpus bump introduced `set <alias> -> seg:`;
        // RECUT § 19c before trusting the consumer rows).
        assertEquals(0, functionBucket.aliasAssignTargets, cell.label()
                + " FUNCTION: the alias assign-target channel is corpus-empty today"
                + " — a nonzero count reopens census § 19c blind spot (b)");
        // The cross-channel chain pin: an IRReference{ALIAS} at a chain root IS a
        // REFERENCE-rooted chain in the standing IR walk (same adapted trees, same
        // CHAIN_LINK descent), so CHAIN_ROOT can never exceed byRoot's REFERENCE
        // row. Equality is a CORPUS fact, recorded in census § 19, not pinned —
        // a future corpus could root a chain at a non-alias reference kind.
        int aliasChainRoots = functionBucket.aliasIrRefConsumer.entrySet().stream()
                .filter(e -> e.getKey().startsWith("CHAIN_ROOT"))
                .mapToInt(Map.Entry::getValue).sum();
        assertTrue(aliasChainRoots <= functionBucket.byRoot.getOrDefault("REFERENCE", 0),
                cell.label() + " FUNCTION: alias-rooted chains (CHAIN_ROOT) must be a subset"
                        + " of the REFERENCE-rooted chain population, got " + aliasChainRoots
                        + " > " + functionBucket.byRoot.getOrDefault("REFERENCE", 0));
        for (Map.Entry<String, Bucket> e : buckets.entrySet()) {
            if (!"FUNCTION".equals(e.getKey())) {
                assertEquals(0, e.getValue().aliasDecls, cell.label() + " " + e.getKey()
                        + ": shortcuts are function-scoped — no alias declarations here");
                assertEquals(0, e.getValue().aliasBoundRefByContext.size()
                                + e.getValue().aliasIrRefByContext.size(),
                        cell.label() + " " + e.getKey()
                                + ": no alias references outside the FUNCTION face");
            }
        }
        if (cell.label().startsWith("cdm") || cell.label().startsWith("drr")) {
            assertTrue(buckets.get("FUNCTION").chains > 0,
                    cell.label() + ": the FUNCTION face is known nav-bearing");
            assertTrue(functionBucket.aliasDecls > 0,
                    cell.label() + ": the FUNCTION face is known alias-bearing (census § 19)");
        }
        if (cell.label().startsWith("drr")) {
            assertTrue(buckets.get("RULE").chains > 0, "drr: the RULE face is known nav-bearing");
        }
        assertTrue(buckets.get("DATA_RULE").bodies > 0,
                cell.label() + ": every cell carries data-type conditions (the 3,853-file kind)");
    }

    private void censusBody(ExpressionToIRAdapter adapter, RWorkspace ws, Bucket b, String context,
            RExpression body, RFunction owner) {
        b.bodies++;
        Optional<IRExpr> ir = adapter.adapt(body, ws);
        if (ir.isEmpty()) {
            b.adaptEmpty++;
        } else {
            b.adapted++;
            walk(ir.get(), "BODY_ROOT:" + context, b, context);
        }
        // The emitter-altitude SITE census (the shared-classifier leg): every AST
        // maximal-top candidate in this body, classified exactly as the emitter
        // classifies it. RULE bodies classify through the synthetic-owner bridge
        // (census § 12); DATA_RULE bodies stay owner-less — their PARAM roots
        // refuse (no input declaration), and any ITEM-rooted plans land on the
        // family-3 rows (the kind has no funcGen emission path at all —
        // DataRuleGenerator constructs the legacy compiler directly — so the rows
        // are population facts, never an emission scope).
        siteWalk(body, ws, b, context, owner);
    }

    /**
     * THE § 6.3 ALIAS LEG (census § 19): the alias population + the cross-method
     * consumer census for ONE function. A SEPARATE walk from {@link #censusBody}/
     * {@link #siteWalk} by design — it bumps ONLY the alias* rows, so every
     * standing row keeps its § 4/§ 9/§ 10/§ 12 value byte-for-byte. Aliases are
     * function-scoped ({@code RShortcut} lives under {@code RFunction} only —
     * rules and data-rule conditions cannot declare or reference one), so this
     * function's four body contexts bound the consumer population — with TWO
     * disclosed blind spots that ride OUTSIDE both counting channels (census
     * § 19c): (a) the DISGUISED qualified-name spelling — an
     * {@code REnumValueRef} head the adapter routes to
     * {@code adaptDisguisedInputNav} and legacy renders as an alias invocation
     * without either channel seeing a reference (the corpus witness: the cdm5
     * usesOutput alias {@code payout} counts ZERO on both channels while its
     * golden invokes the generated method six times); (b) the assign-target
     * channel ({@code set <aliasName> -> seg:}) — a raw name string, counted
     * by {@code aliasAssignTargets} and PINNED corpus-zero.
     */
    private void censusAliasLeg(ExpressionToIRAdapter adapter, RWorkspace ws, Bucket b,
            RFunction func, String namespace) {
        List<RShortcut> shortcuts = func.shortcuts();
        if (shortcuts.isEmpty()) {
            return;
        }
        // The render's own usesOutput decision per alias, keyed by name (alias
        // names are unique within a function's scope — the generated method names).
        Set<String> usesOutputNames = new HashSet<>();
        for (FunctionTemplateModel.AliasModel model : aliasHelper.analyze(func)) {
            if (model.getUsesOutput()) {
                usesOutputNames.add(model.getName());
            }
        }
        if (shortcuts.size() > usesOutputNames.size()) {
            b.aliasBearingFuncs++;
            b.aliasBearingClassNames.add(namespace + ":" + func.name());
        }
        // The assign-target channel (blind spot (b) — the leg javadoc): a raw
        // targetName naming a same-function shortcut would be a consumption seat
        // neither walk can see. Corpus-empty today; pinned zero below.
        Set<String> shortcutNames = new HashSet<>();
        for (RShortcut alias : shortcuts) {
            shortcutNames.add(alias.name());
        }
        for (ROperation op : func.operations()) {
            if (op.targetName() != null && shortcutNames.contains(op.targetName())) {
                b.aliasAssignTargets++;
            }
        }
        // Identity-keyed so the conservation pin catches any ref resolving outside
        // this function's own shortcut set (semantically impossible — loud if not).
        Map<RShortcut, Integer> refCounts = new IdentityHashMap<>();
        for (RShortcut alias : shortcuts) {
            b.aliasDecls++;
            refCounts.put(alias, 0);
            boolean usesOutput = usesOutputNames.contains(alias.name());
            if (usesOutput) {
                b.aliasUsesOutput++;
            }
            Optional<IRExpr> ir = alias.expression() == null ? Optional.empty()
                    : adapter.adapt(alias.expression(), ws);
            Bucket.bump(b.aliasBodyTopKind, (usesOutput ? "usesOutput:" : "")
                    + ir.map(e -> e.kind().name()).orElse("adaptEmpty"));
            if (!usesOutput) {
                Bucket.bump(b.aliasSeamByKind, ir.isEmpty() ? "adaptEmpty"
                        : ir.get().cardinality() == ExpressionCardinality.MULTI
                                ? "MapperC" : "MapperS");
                if (alias.expression() != null) {
                    siteClassifier.classify(alias.expression(), ws, func).ifPresent(p ->
                            Bucket.bump(b.aliasBodyWholeChain, p.rootKind() + ":" + p.shape()
                                    + (p.bearsMeta() ? ":meta" : "")));
                }
            }
        }
        // The IR-channel per-alias tallies ride the alias NAME — the adapter's own
        // key ("NAME-keyed against the enclosing function ... resolution-free by
        // construction"); names are unique within a function's scope.
        Map<String, Integer> irRefCounts = new TreeMap<>();
        for (RShortcut alias : shortcuts) {
            irRefCounts.put(alias.name(), 0);
        }
        for (ROperation op : func.operations()) {
            aliasRefWalk(op.expression(), op.expression(), "operation", b, refCounts,
                    usesOutputNames);
            aliasIrWalk(adapter, ws, op.expression(), "operation", b, irRefCounts,
                    usesOutputNames);
        }
        for (RShortcut alias : shortcuts) {
            aliasRefWalk(alias.expression(), alias.expression(), "alias", b, refCounts,
                    usesOutputNames);
            aliasIrWalk(adapter, ws, alias.expression(), "alias", b, irRefCounts,
                    usesOutputNames);
        }
        for (RCondition cond : func.conditions()) {
            aliasRefWalk(cond.expression(), cond.expression(), "condition", b, refCounts,
                    usesOutputNames);
            aliasIrWalk(adapter, ws, cond.expression(), "condition", b, irRefCounts,
                    usesOutputNames);
        }
        for (RPostCondition post : func.postConditions()) {
            aliasRefWalk(post.expression(), post.expression(), "postCondition", b, refCounts,
                    usesOutputNames);
            aliasIrWalk(adapter, ws, post.expression(), "postCondition", b, irRefCounts,
                    usesOutputNames);
        }
        for (Map.Entry<RShortcut, Integer> entry : refCounts.entrySet()) {
            b.aliasBoundRefsPerAlias.merge(entry.getValue(), 1, Integer::sum);
        }
        for (Map.Entry<String, Integer> entry : irRefCounts.entrySet()) {
            b.aliasIrRefsPerAlias.merge(entry.getValue(), 1, Integer::sum);
        }
    }

    /**
     * The IR-channel consumer walk (census § 19): adapts the body and records
     * every {@code IRReference&#123;ALIAS&#125;} under its IR consumer kind. The IR
     * is the normalizer (§ 4) — the linker-bound reference, the resolution-free
     * name-match and the RESOLVED qualified-chain head all arrive as the SAME node
     * kind — so this channel counts the walk-visible SPELLING-NORMALIZED
     * population (NOT the full render-emitted invocation set: the
     * disguised-spelling class renders alias invocations without ever minting
     * {@code IRReference&#123;ALIAS&#125;} — the leg javadoc's blind-spot (a)).
     * Chain links descend to their root with the {@code CHAIN_ROOT} label (a
     * chain link's only child is its receiver — the same invariant
     * {@link #recordChain} asserts), so an alias-rooted chain of any normalized
     * spelling lands exactly one {@code CHAIN_ROOT} row.
     */
    private void aliasIrWalk(ExpressionToIRAdapter adapter, RWorkspace ws, RExpression body,
            String context, Bucket b, Map<String, Integer> irRefCounts,
            Set<String> usesOutputNames) {
        if (body == null) {
            return;
        }
        adapter.adapt(body, ws).ifPresent(ir ->
                aliasIrNodeWalk(ir, "BODY_ROOT", b, context, irRefCounts, usesOutputNames));
    }

    private void aliasIrNodeWalk(IRExpr node, String consumerLabel, Bucket b, String context,
            Map<String, Integer> irRefCounts, Set<String> usesOutputNames) {
        if (node instanceof IRReference ref
                && ref.referenceKind() == IRReference.ReferenceKind.ALIAS) {
            irRefCounts.merge(ref.target(), 1, Integer::sum);
            String flag = usesOutputNames.contains(ref.target()) ? ":usesOutput" : "";
            Bucket.bump(b.aliasIrRefByContext, context + flag);
            Bucket.bump(b.aliasIrRefConsumer, consumerLabel + flag);
        }
        if (CHAIN_LINK_KINDS.contains(node.kind())) {
            IRExpr cursor = node;
            while (CHAIN_LINK_KINDS.contains(cursor.kind())) {
                // The same 1-child invariant recordChain asserts — a future kind
                // violating it fails diagnosably here too (Copilot #554 R1).
                assertEquals(1, cursor.children().size(),
                        cursor.kind() + ": a chain-link kind must carry exactly one"
                                + " (receiver) child — the alias IR walk cannot"
                                + " descend this shape");
                cursor = cursor.children().get(0);
            }
            aliasIrNodeWalk(cursor, "CHAIN_ROOT", b, context, irRefCounts, usesOutputNames);
            return;
        }
        for (IRExpr child : node.children()) {
            aliasIrNodeWalk(child, node.kind().name(), b, context, irRefCounts, usesOutputNames);
        }
    }

    /** AST pre-order walk recording every alias reference (census § 19 consumer leg). */
    private void aliasRefWalk(RNode node, RExpression bodyRoot, String context, Bucket b,
            Map<RShortcut, Integer> refCounts, Set<String> usesOutputNames) {
        if (node == null) {
            return;
        }
        if (node instanceof RSymbolReference ref) {
            RShortcut target = ref.symbol().filter(RShortcut.class::isInstance)
                    .map(RShortcut.class::cast).orElse(null);
            if (target != null) {
                refCounts.merge(target, 1, Integer::sum);
                String flag = usesOutputNames.contains(target.name()) ? ":usesOutput" : "";
                String consumer = classifyAliasConsumer(ref, bodyRoot);
                Bucket.bump(b.aliasBoundRefByContext, context + flag);
                Bucket.bump(b.aliasBoundRefConsumer, consumer + flag);
                // Derived from the computed label so the counter can never drift
                // from the taxonomy row (Copilot #554 R1).
                if ("ONLY_EXISTS_ELEMENT".equals(consumer)) {
                    b.aliasRefOnlyExists++;
                }
            }
        }
        for (RNode child : node.children()) {
            aliasRefWalk(child, bodyRoot, context, b, refCounts, usesOutputNames);
        }
    }

    /**
     * The consumer shape of one alias reference — the parent-node classification
     * (the exact dual of the chain walk's {@code byConsumer}):
     * {@code ONLY_EXISTS_ELEMENT} = the path-semantic blocker (the § 4 guard-1
     * seat — {@code onlyExists} reads parent/path structure SEMANTICALLY, so a
     * re-typed alias could not feed it through a bare re-wrap; ANCESTOR-classified
     * and checked FIRST — it dominates every local parent shape);
     * {@code NAV_RECEIVER} = a chain navigates off the alias at the BOUND spelling
     * (corpus-EMPTY on all three cells post-ancestor-classification — every bound
     * nav-off-alias seat sits inside an only-exists element, census § 19c; kept as
     * a live label for future corpora); {@code CALL_ARG} = an argument of a symbol
     * call; {@code BODY_ROOT} = the reference IS the whole body; everything else
     * keys the parent's AST simple name (grouped in the census doc, never
     * load-bearing individually). Package-visible (not private) so the § 6.3
     * disposition-map instrument ({@code AliasDispositionMapTest}) classifies
     * consumer seats with the SAME code — its per-member multisets must
     * reconcile to this census's global rows by construction, never by parallel
     * reimplementation.
     */
    static String classifyAliasConsumer(RSymbolReference ref, RExpression bodyRoot) {
        if (ref == bodyRoot) {
            return "BODY_ROOT";
        }
        // The only-exists BLOCKER class dominates the local parent shape: an
        // ROnlyExistsElement exposes its element content as a synthesized
        // receiverExpression subtree, so an alias reference HEADING an element's
        // chain has an RFeatureCall parent, not the element — an ANCESTOR check,
        // never a direct-parent check (Copilot #554 R1; § 4 guard 1 — every seat
        // inside the element subtree is path-load-bearing).
        for (RNode up = ref.parent(); up != null; up = up.parent()) {
            if (up instanceof ROnlyExistsElement) {
                return "ONLY_EXISTS_ELEMENT";
            }
        }
        RNode parent = ref.parent();
        if (parent instanceof RFeatureCall fc && fc.receiver() == ref) {
            return "NAV_RECEIVER";
        }
        if (parent instanceof RDeepFeatureCall dfc && dfc.receiver() == ref) {
            return "DEEP_NAV_RECEIVER";
        }
        if (parent instanceof RSymbolReference) {
            return "CALL_ARG";
        }
        return parent == null ? "PARENTLESS" : parent.getClass().getSimpleName();
    }

    /** AST pre-order site walk over a body — the shared-classifier eligibility census. */
    private void siteWalk(RNode node, RWorkspace ws, Bucket b, String context, RFunction owner) {
        if (node instanceof RExpression expr
                && NavigationChainClassifier.isMaximalChainTopCandidate(expr)) {
            Optional<NavigationChainClassifier.LadderPlan> plan =
                    siteClassifier.classify(expr, ws, owner);
            if (plan.isPresent()
                    && plan.get().rootKind() == NavigationChainClassifier.LadderPlan.RootKind.ITEM) {
                // THE FAMILY-3 ITEM POOL (census § 12) — entirely DISJOINT rows: the
                // standing siteEligible/siteShape/… rows keep their § 4/§ 9/§ 10
                // values byte-for-byte (an ITEM plan never bumps them), and the
                // emitter's pending pins mirror this pool's totals.
                NavigationChainClassifier.LadderPlan p = plan.get();
                boolean itemMeta = p.bearsMeta()
                        || p.rootMeta() != NavigationChainClassifier.LadderPlan.TopMeta.NONE;
                boolean wroot = p.rootMeta() != NavigationChainClassifier.LadderPlan.TopMeta.NONE;
                String suffix = (itemMeta ? ":meta" : "") + (wroot ? ":wroot" : "");
                Bucket.bump(b.siteItemByContext, context + (itemMeta ? ":meta" : ""));
                Bucket.bump(b.siteItemByShape, context + ":" + p.shape() + suffix);
                Bucket.bump(b.siteItemBySteps,
                        context + ":" + p.shape() + ":" + p.accessors().size() + suffix);
                if (wroot) {
                    if (p.rootMeta() == NavigationChainClassifier.LadderPlan.TopMeta.REF) {
                        b.siteItemWrappedRef++;
                    } else {
                        b.siteItemWrappedField++;
                    }
                }
                if (p.rootType().isMissing()) {
                    b.siteItemRootTypeMissing++;
                }
                if (p.rootMulti()) {
                    b.siteItemRootMulti++;
                }
                if (p.shape() != NavigationChainClassifier.LadderPlan.Shape.SINGLE
                        && p.topType().isMissing()) {
                    b.siteItemTopTypeMissingMulti++;
                }
            } else if (plan.isPresent()) {
                NavigationChainClassifier.LadderPlan p = plan.get();
                boolean meta = p.bearsMeta();
                // Meta-bearing plans ride DISJOINT keys/rows so every pre-widening
                // row stays receipt-comparable with the § 4/§ 9 receipts.
                Bucket.bump(b.siteEligibleByContext, meta ? context + ":meta" : context);
                Bucket.bump(b.siteShapeByContext,
                        context + ":" + p.shape() + (meta ? ":meta" : ""));
                if ("body".equals(context)) {
                    // THE BODY-CONTEXT PARAM POOL (census § 12a) — the rule face's
                    // input-rooted chains, visible only through the synthetic-owner
                    // bridge; new rows (the RULE face's site rows were measured
                    // "none" before the bridge), so nothing pre-existing moves.
                    Bucket.bump(b.siteBodyParamByShape, p.shape() + (meta ? ":meta" : ""));
                    Bucket.bump(b.siteBodyParamBySteps,
                            p.shape() + ":" + p.accessors().size() + (meta ? ":meta" : ""));
                    if (p.rootMulti()) {
                        b.siteBodyParamRootMulti++;
                    }
                    if (p.rootType().isMissing()) {
                        b.siteBodyParamRootTypeMissing++;
                    }
                    if (p.shape() != NavigationChainClassifier.LadderPlan.Shape.SINGLE
                            && p.topType().isMissing()) {
                        b.siteBodyParamTopTypeMissingMulti++;
                    }
                }
                if ("operation".equals(context) && !meta) {
                    switch (p.shape()) {
                        case SINGLE -> b.siteEligibleOperationByHops
                                .merge(p.accessors().size(), 1, Integer::sum);
                        case TERMINAL_MULTI -> b.siteOperationHopsTerminalMulti
                                .merge(p.accessors().size(), 1, Integer::sum);
                        case STREAM -> {
                            b.siteOperationHopsStream
                                    .merge(p.accessors().size(), 1, Integer::sum);
                            b.siteOperationStreamMultiPoints
                                    .merge(p.multiPoints(), 1, Integer::sum);
                        }
                        // A statement switch does not enforce exhaustiveness — keep
                        // a future Shape value from silently skipping the per-hops
                        // tally (Seat-1 PR-5 N3).
                        default -> throw new IllegalStateException(
                                "unpriced Shape in the census tally: " + p.shape());
                    }
                    if (p.rootMulti()) {
                        b.siteOperationRootMulti++;
                    }
                } else if ("operation".equals(context)) {
                    // THE FAMILY-2 META POOL (census § 10).
                    Bucket.bump(b.siteOperationMetaByShape, p.shape().name());
                    switch (p.shape()) {
                        case SINGLE -> b.siteOperationMetaStepsSingle
                                .merge(p.accessors().size(), 1, Integer::sum);
                        case TERMINAL_MULTI -> b.siteOperationMetaStepsTerminalMulti
                                .merge(p.accessors().size(), 1, Integer::sum);
                        case STREAM -> {
                            b.siteOperationMetaStepsStream
                                    .merge(p.accessors().size(), 1, Integer::sum);
                            b.siteOperationMetaStreamMultiPoints
                                    .merge(p.multiPoints(), 1, Integer::sum);
                        }
                        default -> throw new IllegalStateException(
                                "unpriced Shape in the meta census tally: " + p.shape());
                    }
                    if (p.rootMulti()) {
                        b.siteOperationMetaRootMulti++;
                    }
                    b.siteOperationMetaFieldHops += p.fieldMetaHops();
                    b.siteOperationMetaRefHops += p.refMetaHops();
                    Bucket.bump(b.siteOperationMetaTopKind, p.topMeta().name());
                    if (p.topType().isMissing()) {
                        // The witness-fact gap (the § 9a boundary witness needs the
                        // TOP type for the multi shapes; the #497 census read meta
                        // typing heavily fragmented — measure it on THIS pool).
                        Bucket.bump(b.siteOperationMetaTopTypeMissingByShape,
                                p.shape().name());
                    }
                }
            }
        }
        for (RNode child : node.children()) {
            siteWalk(child, ws, b, context, owner);
        }
    }

    /** Pre-order walk; a chain-top link records ONE chain and recursion resumes at its root. */
    private void walk(IRExpr node, String consumerLabel, Bucket b, String context) {
        if (CHAIN_LINK_KINDS.contains(node.kind())) {
            IRExpr root = recordChain(node, consumerLabel, b, context);
            walk(root, "CHAIN_ROOT", b, context);
            return;
        }
        for (IRExpr child : node.children()) {
            walk(child, node.kind().name(), b, context);
        }
    }

    /** Classify one maximal chain; returns the (non-link) chain root for continued walking. */
    private IRExpr recordChain(IRExpr top, String consumerLabel, Bucket b, String context) {
        b.chains++;
        Bucket.bump(b.byContext, context);
        Bucket.bump(b.byConsumer, consumerLabel);

        int hops = 0;
        boolean pure = true;
        boolean meta = false;
        boolean otherLink = false;
        boolean anyHopTypeMissing = false;
        boolean allHopsSingleStep = true;
        IRExpr cursor = top;
        while (CHAIN_LINK_KINDS.contains(cursor.kind())) {
            hops++;
            if (cursor.type().isMissing()) {
                anyHopTypeMissing = true;
            }
            switch (cursor.kind()) {
                case FIELD_ACCESS -> {
                    if (((FieldAccess) cursor).featureCardinality() == ExpressionCardinality.MULTI) {
                        allHopsSingleStep = false;
                    }
                }
                case META_ACCESS -> {
                    pure = false;
                    meta = true;
                    IRMetaAccess metaHop = (IRMetaAccess) cursor;
                    if (metaHop.metaQualifiers().contains("reference")
                            || metaHop.metaQualifiers().contains("address")) {
                        b.refMetaHops++;
                    } else {
                        b.fieldMetaHops++;
                    }
                    if (metaHop.featureCardinality() == ExpressionCardinality.MULTI) {
                        allHopsSingleStep = false;
                    }
                }
                default -> {
                    pure = false;
                    otherLink = true;
                    // Step cardinality is not modelled on the deep/record/qualifier/choice
                    // links; classify conservatively as not-all-single.
                    allHopsSingleStep = false;
                }
            }
            // Every CHAIN_LINK kind carries exactly its receiver as the single child
            // (the record shapes); assert loudly so a future kind violating the
            // invariant can never mis-price the census (Copilot #539 R1).
            assertEquals(1, cursor.children().size(),
                    cursor.kind() + ": a chain-link kind must carry exactly one (receiver)"
                            + " child — the census walk cannot price this shape");
            cursor = cursor.children().get(0);
        }
        IRExpr root = cursor;

        b.byHopCount.merge(hops, 1, Integer::sum);
        String rootClass = classifyRoot(root);
        Bucket.bump(b.byRoot, rootClass);
        if (anyHopTypeMissing) {
            b.hopTypeMissing++;
            if (pure) {
                b.hopTypeMissingOnPure++;
            }
        }
        if (root.type().isMissing()) {
            b.rootTypeMissing++;
            if (pure) {
                b.rootTypeMissingOnPure++;
            }
        }

        boolean shallowRooted = SHALLOW_NAV_ROOT_KINDS.contains(root.kind());
        boolean rootSingle = root.cardinality() == ExpressionCardinality.SINGLE;
        if (shallowRooted) {
            b.shallowRooted++;
        } else if (otherLink) {
            b.otherLinkBearing++;
        } else if (meta) {
            b.metaBearing++;
        } else if (pure && rootSingle && allHopsSingleStep) {
            b.pureAllSingle++;
        } else {
            b.pureMulti++;
        }
        return root;
    }

    /**
     * The § 19 conservation pins, one consumer channel at a time (structural only —
     * distributions stay RECORDED): every reference classifies exactly once on both
     * the context and the consumer rows; the per-alias distribution conserves to the
     * reference total AND to the declaration population (a reference resolving
     * outside the function's own shortcut set would inflate the alias count — loud).
     */
    private static void assertAliasChannelConserves(CorpusCells.CellSpec cell, String channel,
            Map<String, Integer> byContext, Map<String, Integer> byConsumer,
            Map<Integer, Integer> perAlias, int aliasDecls) {
        int refsByContext = byContext.values().stream().mapToInt(Integer::intValue).sum();
        int refsByConsumer = byConsumer.values().stream().mapToInt(Integer::intValue).sum();
        assertEquals(refsByContext, refsByConsumer, cell.label() + " FUNCTION " + channel
                + ": every alias reference must classify exactly once per row family");
        int refsByDistribution = perAlias.entrySet().stream()
                .mapToInt(e -> e.getKey() * e.getValue()).sum();
        assertEquals(refsByContext, refsByDistribution, cell.label() + " FUNCTION " + channel
                + ": the per-alias distribution must conserve to the reference total");
        int aliasesInDistribution = perAlias.values().stream().mapToInt(Integer::intValue).sum();
        assertEquals(aliasDecls, aliasesInDistribution, cell.label() + " FUNCTION " + channel
                + ": every alias declaration must land in the consumer distribution");
    }

    private static String classifyRoot(IRExpr root) {
        if (root instanceof IRVariable variable) {
            return "var:" + variable.variableKind();
        }
        if (SHALLOW_NAV_ROOT_KINDS.contains(root.kind())) {
            return "shallowNav:" + root.kind();
        }
        return root.kind().name();
    }

    private static void printBucket(String cell, String face, Bucket b) {
        String head = "NAVCENSUS " + cell + " " + face + " ";
        System.out.println(head + "bodies=" + b.bodies + " adapted=" + b.adapted
                + " adaptEmpty=" + b.adaptEmpty + " chains=" + b.chains);
        System.out.println(head + "shape: pureAllSingle=" + b.pureAllSingle
                + " pureMulti=" + b.pureMulti + " metaBearing=" + b.metaBearing
                + " otherLinkBearing=" + b.otherLinkBearing + " shallowRooted=" + b.shallowRooted);
        System.out.println(head + "metaHops: fieldMeta=" + b.fieldMetaHops
                + " refMeta=" + b.refMetaHops);
        System.out.println(head + "factGaps: hopTypeMissing=" + b.hopTypeMissing
                + " (onPure=" + b.hopTypeMissingOnPure + ") rootTypeMissing=" + b.rootTypeMissing
                + " (onPure=" + b.rootTypeMissingOnPure + ")");
        System.out.println(head + "siteEligibleByContext: " + render(b.siteEligibleByContext));
        System.out.println(head + "siteShapeByContext: " + render(b.siteShapeByContext));
        System.out.println(head + "siteOperationByHopsSINGLE: "
                + render(b.siteEligibleOperationByHops));
        System.out.println(head + "siteOperationByHopsTERMINAL_MULTI: "
                + render(b.siteOperationHopsTerminalMulti));
        System.out.println(head + "siteOperationByHopsSTREAM: "
                + render(b.siteOperationHopsStream));
        System.out.println(head + "siteOperationStreamMultiPoints: "
                + render(b.siteOperationStreamMultiPoints));
        System.out.println(head + "siteOperationRootMulti=" + b.siteOperationRootMulti);
        System.out.println(head + "siteOperationMetaByShape: "
                + render(b.siteOperationMetaByShape));
        System.out.println(head + "siteOperationMetaByStepsSINGLE: "
                + render(b.siteOperationMetaStepsSingle));
        System.out.println(head + "siteOperationMetaByStepsTERMINAL_MULTI: "
                + render(b.siteOperationMetaStepsTerminalMulti));
        System.out.println(head + "siteOperationMetaByStepsSTREAM: "
                + render(b.siteOperationMetaStepsStream));
        System.out.println(head + "siteOperationMetaStreamMultiPoints: "
                + render(b.siteOperationMetaStreamMultiPoints));
        System.out.println(head + "siteOperationMetaRootMulti=" + b.siteOperationMetaRootMulti
                + " metaHopKinds: field=" + b.siteOperationMetaFieldHops
                + " ref=" + b.siteOperationMetaRefHops);
        System.out.println(head + "siteOperationMetaTopKind: "
                + render(b.siteOperationMetaTopKind));
        System.out.println(head + "siteOperationMetaTopTypeMissingByShape: "
                + render(b.siteOperationMetaTopTypeMissingByShape));
        System.out.println(head + "siteBodyParamByShape: " + render(b.siteBodyParamByShape));
        System.out.println(head + "siteBodyParamBySteps: " + render(b.siteBodyParamBySteps));
        System.out.println(head + "siteBodyParamFacts: rootMulti=" + b.siteBodyParamRootMulti
                + " rootTypeMissing=" + b.siteBodyParamRootTypeMissing
                + " topTypeMissingMulti=" + b.siteBodyParamTopTypeMissingMulti);
        System.out.println(head + "siteItemByContext: " + render(b.siteItemByContext));
        System.out.println(head + "siteItemByShape: " + render(b.siteItemByShape));
        System.out.println(head + "siteItemBySteps: " + render(b.siteItemBySteps));
        System.out.println(head + "siteItemFacts: wrappedREF=" + b.siteItemWrappedRef
                + " wrappedFIELD=" + b.siteItemWrappedField
                + " rootTypeMissing=" + b.siteItemRootTypeMissing
                + " rootMulti=" + b.siteItemRootMulti
                + " topTypeMissingMulti=" + b.siteItemTopTypeMissingMulti);
        System.out.println(head + "aliasPopulation: decls=" + b.aliasDecls
                + " usesOutput=" + b.aliasUsesOutput
                + " onlyExistsRefs=" + b.aliasRefOnlyExists
                + " bearingFuncs=" + b.aliasBearingFuncs
                + " bearingClassNames=" + b.aliasBearingClassNames.size()
                + " assignTargets=" + b.aliasAssignTargets);
        System.out.println(head + "aliasSeamByKindIR: " + render(b.aliasSeamByKind));
        System.out.println(head + "aliasBodyTopKind: " + render(b.aliasBodyTopKind));
        System.out.println(head + "aliasBodyWholeChain: " + render(b.aliasBodyWholeChain));
        System.out.println(head + "aliasBoundRefByContext: " + render(b.aliasBoundRefByContext));
        System.out.println(head + "aliasBoundRefConsumer: " + render(b.aliasBoundRefConsumer));
        System.out.println(head + "aliasBoundRefsPerAlias: " + render(b.aliasBoundRefsPerAlias));
        System.out.println(head + "aliasIrRefByContext: " + render(b.aliasIrRefByContext));
        System.out.println(head + "aliasIrRefConsumer: " + render(b.aliasIrRefConsumer));
        System.out.println(head + "aliasIrRefsPerAlias: " + render(b.aliasIrRefsPerAlias));
        System.out.println(head + "byContext: " + render(b.byContext));
        System.out.println(head + "byHops: " + render(b.byHopCount));
        System.out.println(head + "byRoot: " + render(b.byRoot));
        System.out.println(head + "byConsumer: " + render(b.byConsumer));
    }

    private static String render(Map<?, Integer> map) {
        if (map.isEmpty()) {
            return "none";
        }
        StringBuilder sb = new StringBuilder();
        map.forEach((k, v) -> {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(k).append('=').append(v);
        });
        return sb.toString();
    }

}
