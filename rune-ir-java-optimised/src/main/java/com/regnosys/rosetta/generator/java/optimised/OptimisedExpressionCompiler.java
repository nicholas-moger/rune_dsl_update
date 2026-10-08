package com.regnosys.rosetta.generator.java.optimised;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.expressions.references.REnumValueRef;
import com.regnosys.rosetta.ast.expressions.references.RFeatureCall;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.functions.RShortcut;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.expression.ExpressionCompiler;
import com.regnosys.rosetta.generator.java.expression.ExpressionContext;
import com.regnosys.rosetta.generator.java.expression.handlers.HandlerHelper;
import com.regnosys.rosetta.generator.java.statement.builder.JavaExpression;
import com.regnosys.rosetta.generator.java.statement.builder.JavaStatementBuilder;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.generator.java.types.RJavaWithMetaValue;
import com.rosetta.util.types.JavaClass;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Supplier;

/**
 * The v3 optimised expression compiler — the NAVIGATION-CHAIN family, tranches 1 + 2
 * plus the family-2 META widening (the census
 * `research/p3-navigation-family-census.md` § 4/§ 5 + § 9/§ 9a + § 10/§ 10a; the
 * STREAM shape is TWICE measured out — § 9d the stream pipeline, § 11 the
 * loop-form re-price): pure and meta-bearing navigation chains, input-PARAM-rooted,
 * in FUNCTION OPERATION bodies, re-emitted as a null-guarded direct form wrapped
 * ONCE at the boundary — the {@code Shape} decode picks the form:
 *
 * <pre>
 *   SINGLE          reference: MapperS.of(p).&lt;A&gt;map("getA", _p -&gt; _p.getA()).&lt;B&gt;map("getB", a -&gt; a.getB())
 *                   optimised: MapperS.of((p == null ? null : p.getA() == null ? null : p.getA().getB())).filterSingle(sameNav -&gt; true)
 *   TERMINAL_MULTI  reference: MapperS.of(p).&lt;B&gt;mapC("getBs", _p -&gt; _p.getBs())
 *                   optimised: MapperC.&lt;B&gt;of((p == null ? null : p.getBs())).unionSame(MapperC.&lt;B&gt;ofNull())
 *   STREAM          reference: MapperC.&lt;A&gt;of(as).&lt;B&gt;map("getB", a -&gt; a.getB())
 *                   optimised: NONE — TWICE measured out (§ 9d the stream pipeline, § 11 the loop-form helpers via {@link NavigationHelperForm}); STREAM sites stay reference-emitted behind the per-family decline pins
 * </pre>
 *
 * <p>META hops (§ 10a — the {@code FieldWithMetaX}/{@code ReferenceWithMetaX}
 * wrapper families) ride the SAME forms: an interior meta hop's ladder run is
 * {@code .getM() == null ? null : … .getM().getValue()} (the reference render's
 * {@code map("Type coercion", …)} deref baked as a getter step, single-run), a
 * meta TOP leaves the wrapper as the chain value (the consumer-side terminal
 * coercion composes onto the boundary wrap exactly as onto the reference chain),
 * and a meta-top multi boundary is witnessed with the WRAPPER class
 * ({@code MapperC.&lt;FieldWithMetaX&gt;of((…}) — derived through the same
 * {@code RJavaWithMetaValue.create} factory the reference witness rides.
 *
 * <p><b>Equivalence argument.</b> Operation bodies extract VALUES from the chain
 * (`.get()`/`.getMulti()`/arg unwrap) and discard ComparisonResult text, so the Mapper
 * path metadata — the ONLY observable the wrap drops — is unobservable at eligible sites
 * (the census § 4 text-safety cut; conditions/postConditions/aliases/DATA_RULE keep the
 * reference emission untouched). Value semantics are identical: a mid-chain null yields
 * the same null value / 0 resultCount / same truth outcomes as the Mapper chain's error
 * item. The differential gate (rune-equivalence, O1–O7) is the merge oracle.
 *
 * <p><b>Mechanism.</b> Every visit falls through to {@code super} (byte-identical to the
 * reference emission) unless ALL of: (1) the optimised window is open —
 * {@link OptimisedFunctionGenerator#generateWithErrors} opens it for the FUNCTION kind
 * and (family 3, census § 12) {@code OptimisedFunctionGenerator}'s
 * {@code buildClassWithBaseInterface} override opens it for the rule/report renders
 * (which also stamp the § 12b.2 owner bridge — rule bodies have no ancestry owner);
 * the dependency collector's compiler never has a window; (2) an OPERATION body is
 * being rendered (only {@link OptimisedFunctionExpressionRenderer} toggles it around
 * the canonical {@code renderOperation} — every path, functions AND rules, renders
 * operations through it); (3) the {@link NavigationChainClassifier} accepts the node —
 * the maximal top of a chain whose ADAPTED form is pure {@code FieldAccess}/meta hops
 * over a function-input PARAM root (owner-resolved via ancestry or the bridge) or a
 * {@code SYNTHETIC_ITEM} root (family 3 — the § 12b.3 harvested-root ladder). Both AST
 * spellings of a chain are hooked
 * ({@link #visitFeatureCall} for feature-call spines, {@link #visitEnumValueRef} for
 * linker-bound qualified chains — the classifier's IR normalization covers both with
 * one predicate).
 *
 * <p>At an eligible site the reference emission is STILL rendered first and harvested:
 * its expression type + refs + static wildcards carry to the replacement (consumers see
 * the exact reference type; imports stay reference-shaped), and its scope side effects
 * (lambda-name registrations — the #170 same-name numbering) keep SIBLING lambdas
 * numbered exactly as the reference file numbers them. The replacement is a PLAIN
 * {@link JavaExpression} — deliberately NOT the structural-unwrap wrap factory — so
 * every consumer treats it exactly as it treats the reference chain expression (see
 * the in-method note).
 *
 * <p>The per-cell conversion counters are the reconciliation receipt: converted ==
 * the census's site-walk count per cell (the census is the priced scope; the emitter
 * must hit it exactly — a shortfall is a bailed eligible site, an excess is
 * out-of-scope conversion, both loud in the module's reconciliation gate).
 */
public class OptimisedExpressionCompiler extends ExpressionCompiler {

    private final GeneratorModel generatorModel;
    private final JavaTypeTranslator typeTranslator;
    private final NavigationChainClassifier classifier = new NavigationChainClassifier();

    private int optimisedWindowDepth;
    private int operationDepth;
    private int itemRenderDepth;
    private int itemInteriorSuppressed;
    private int convertedChains;
    private int eligibleBailedNonExpression;
    private int multiTwinTypeMissingFallbacks;
    private int metaTwinTypeMissingFallbacks;
    private int declinedStreamShape;
    private int declinedMetaStream;
    private int declinedItemStream;
    private int declinedItemMetaStream;
    private final Map<String, Integer> itemHarvestBails = new TreeMap<>();
    private final Map<Integer, Integer> convertedByHops = new TreeMap<>();
    private final Map<Integer, Integer> convertedMetaBySteps = new TreeMap<>();
    private final Map<String, Integer> convertedItemBySteps = new TreeMap<>();
    private final Map<String, Integer> convertedByShape = new TreeMap<>();
    private final java.util.Set<RExpression> convertedSites =
            java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());

    // -------------------------------------------- the § 6.3 T1 alias-seam state --
    // (ALL rows disjoint from the landed family counters above — the § 9b/§ 10b/
    // § 12 receipts stay byte-comparable; the tranche gate pins these separately)
    /** The T1 flip policy (lazy — one instance per rendering compiler). */
    private AliasValueSeamPolicy aliasValueSeamPolicy;
    /** Leaf bridge wraps emitted at alias invocation seats ({@code S=n C=m}). */
    private final Map<String, Integer> aliasLeafWraps = new TreeMap<>();
    /** Converted ALIAS_CALL-rooted ladder events ({@code SINGLE=n TERMINAL_MULTI=k …}). */
    private final Map<String, Integer> convertedAliasRootByShape = new TreeMap<>();
    private int convertedAliasRootChains;
    /** STREAM-shaped ALIAS_CALL classify-passes kept on the bridge composition. */
    private int declinedAliasRootStream;
    /** ALIAS_CALL conversion bails ({@code reason=n}; every bail keeps the reference emission). */
    private final Map<String, Integer> aliasRootBails = new TreeMap<>();
    /** ALIAS_CALL classify-passes suppressed inside an outer ITEM plan's reference render. */
    private int aliasRootInteriorSuppressed;
    private final java.util.Set<RExpression> convertedAliasRootSites =
            java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());

    /**
     * THE FAMILY-3 OWNER BRIDGE (census § 12b.2): the current render function,
     * stamped by {@link OptimisedFunctionGenerator#buildClassWithBaseInterface}
     * around the rule/report render (the synthetic {@code fromRule}/{@code
     * fromReport} bridge is DETACHED — {@code ROperation.setExpression} never
     * re-parents, so {@code HandlerHelper.findEnclosingFunction} returns null
     * inside rule bodies). Read ONLY as the ancestry fallback — attached
     * functions keep resolving through the AST walk.
     */
    private RFunction bridgeOwner;


    public OptimisedExpressionCompiler(GeneratorModel generatorModel,
            JavaTypeTranslator typeTranslator, JavaTypeUtil typeUtil) {
        super(generatorModel, typeTranslator, typeUtil);
        this.generatorModel = generatorModel;
        this.typeTranslator = typeTranslator;
    }

    void enterOptimisedWindow() {
        optimisedWindowDepth++;
    }

    void exitOptimisedWindow() {
        optimisedWindowDepth--;
    }

    void enterOperationBody() {
        operationDepth++;
    }

    void exitOperationBody() {
        operationDepth--;
    }

    /** The conversions this compiler instance performed (the reconciliation counter). */
    public int convertedChains() {
        return convertedChains;
    }

    /** DISTINCT AST sites converted (identity) — the re-render duplication discriminator. */
    public int convertedDistinctSites() {
        return convertedSites.size();
    }

    /** The converted sites themselves (identity set, unmodifiable view) — diagnostics. */
    public java.util.Set<RExpression> convertedSiteSet() {
        return java.util.Collections.unmodifiableSet(convertedSites);
    }

    /**
     * Classify-passed sites whose reference render was NOT a plain expression
     * (bailed conservative) — the completeness pin expects ZERO.
     */
    public int eligibleBailedNonExpression() {
        return eligibleBailedNonExpression;
    }

    /**
     * Multi-shape conversion attempts on RENDER-SYNTHESIZED twin nodes whose
     * adapted top type is MISSING (no linker typing on re-spelled nodes) — those
     * seats keep the reference emission. Walk-invisible by construction (the gate's
     * shortfall == 0 proves it); pinned exactly per cell.
     */
    public int multiTwinTypeMissingFallbacks() {
        return multiTwinTypeMissingFallbacks;
    }

    /**
     * META-shape conversion attempts on RENDER-SYNTHESIZED twin nodes whose adapted
     * top type is MISSING — the meta twin of {@link #multiTwinTypeMissingFallbacks}
     * (census § 10b), counted separately so the § 9b tuple stays
     * receipt-comparable. Walk-invisible by construction (the census § 10 receipt:
     * {@code siteOperationMetaTopTypeMissingByShape: none} — zero missing top types
     * on the walk-visible pool).
     */
    public int metaTwinTypeMissingFallbacks() {
        return metaTwinTypeMissingFallbacks;
    }

    /**
     * STREAM-shape classify-passes deliberately kept on the reference emission —
     * TWICE measured out (the census § 9d stream pipeline AND the § 11 loop-form
     * re-price: the same-day 6-fork A/B priced the loop-bearing tree +494.7 MB/op
     * ABOVE this tree on DRR, fully disjoint — the flatten-to-materialized-list
     * boundary structure ESCAPES where the reference chain's intermediate items
     * die young under escape analysis). A priced scope cut, pinned exactly per
     * cell. META-free plans only — the meta pool's stream declines ride
     * {@link #declinedMetaStream} so this tuple stays receipt-comparable with § 9d.
     */
    public int declinedStreamShape() {
        return declinedStreamShape;
    }

    /**
     * META-bearing STREAM-shape classify-passes kept on the reference emission —
     * the § 9d/§ 11 measured declines CARRIED to the family-2 pool (the same
     * escaping-materialization economics on an even smaller-N pool). Pinned per
     * cell, separately from the § 9d tuple.
     */
    public int declinedMetaStream() {
        return declinedMetaStream;
    }

    /**
     * ITEM-rooted META-FREE STREAM-shape classify-passes kept on the reference
     * emission (census § 12): the § 9d/§ 11 twice-measured decline CARRIED to
     * the family-3 item pool, on DISJOINT counters so the § 9d/§ 10a tuples
     * stay receipt-comparable. Pinned per cell.
     */
    public int declinedItemStream() {
        return declinedItemStream;
    }

    /** The seats the classifier's then-body root law re-shaped to STREAM (PR #607) — the decode receipt. */
    public java.util.List<String> thenBodyRootDeclines() {
        return classifier.thenBodyRootDeclines();
    }

    /**
     * ITEM-rooted META-bearing STREAM-shape classify-passes kept on the
     * reference emission — the meta twin of {@link #declinedItemStream}
     * (census § 12). Pinned per cell.
     */
    public int declinedItemMetaStream() {
        return declinedItemMetaStream;
    }

    /**
     * ITEM-rooted SINGLE/TERMINAL_MULTI classify-passes that bailed
     * conservatively at a harvest belt ({@code reason=n} breakdown; {@code none}
     * when zero) — the census § 12b.3 belts: {@code rootScan} (the reference
     * render text does not open with the render-idiom root shape
     * {@code <ident>.<}/{@code <ident>.map(} — the harvest channel's whole
     * contract), {@code nonExpression} (a statement-composite reference
     * render), {@code wrappedRoot} (a wrapper-typed item root — measured ZERO
     * on the frozen corpus; its leading deref is not in the accessors, so it
     * must never mis-emit), {@code multiTopTypeMissing} (a multi boundary
     * witness without a type — the render-synthesized-twin class, counted here
     * so the § 9b/§ 10b twin pins stay receipt-comparable). Every bail keeps
     * the reference emission — value-identical by definition. The walk-visible
     * pool must never bail (the completeness law); pinned per cell.
     */
    public String itemHarvestBailBreakdown() {
        return renderBreakdown(itemHarvestBails);
    }

    /** The total ITEM-plan harvest bails (the sum over the reason breakdown). */
    public int itemHarvestBailTotal() {
        int total = 0;
        for (int n : itemHarvestBails.values()) {
            total += n;
        }
        return total;
    }

    /**
     * ITEM-rooted classify-passes suppressed INSIDE an outer ITEM plan's
     * reference render (census § 12b.3 — interior twin re-spells kept on the
     * reference emission so the outer harvest sees clean text). A twin class,
     * never a walk site; pinned per cell.
     */
    public int itemInteriorSuppressed() {
        return itemInteriorSuppressed;
    }

    /**
     * Per-accessor-step conversion breakdown for ITEM-rooted plans
     * ({@code steps[:meta]=n} — disjoint from {@link #convertedByHopsBreakdown}
     * and {@link #convertedMetaByStepsBreakdown} so the § 9b/§ 10b receipts
     * stay comparable; {@code none} when zero).
     */
    public String convertedItemByStepsBreakdown() {
        return renderBreakdown(convertedItemBySteps);
    }

    /** Stamp the owner bridge for a rule/report render (census § 12b.2). */
    void setBridgeOwner(RFunction owner) {
        this.bridgeOwner = owner;
    }

    /** The current owner bridge (save/restore channel for nested renders). */
    RFunction bridgeOwner() {
        return bridgeOwner;
    }

    // ------------------------------------------------ the § 6.3 T1 alias seam --

    /** The T1 flip policy (lazy; shared by the leaf, the chain admission and the generator). */
    public AliasValueSeamPolicy aliasValueSeamPolicy() {
        if (aliasValueSeamPolicy == null) {
            aliasValueSeamPolicy = new AliasValueSeamPolicy(generatorModel, typeTranslator,
                    getTypeUtil(), classifier);
        }
        return aliasValueSeamPolicy;
    }

    /** Whether the optimised window is open (the generator's flip-seam gate). */
    boolean optimisedWindowOpen() {
        return optimisedWindowDepth > 0;
    }

    /**
     * THE § 6.3 VALUE-SEAM LEAF (the {@code ReferenceHandler} alias-arm query):
     * WINDOW-GATED — the dependency collector's compiler instance never opens
     * the window, so its walks keep reading reference shapes; inside the window
     * every seat of a unit (operations, alias bodies, the disguised synthesized
     * navs — all routes reach the single alias arm) answers from ONE policy
     * instance, so the seam emission and every invocation seat can never
     * disagree.
     */
    @Override
    public AliasValueSeam aliasValueSeamOrNull(RFunction enclosingFunc, RShortcut alias) {
        if (optimisedWindowDepth == 0 || enclosingFunc == null || alias == null) {
            return null;
        }
        AliasValueSeamPolicy.FlipFacts facts =
                aliasValueSeamPolicy().flipFactsOrNull(enclosingFunc, alias);
        if (facts == null) {
            return null;
        }
        aliasLeafWraps.merge(facts.isMulti() ? "C" : "S", 1, Integer::sum);
        // A sentinel-spelt seam element rides to the bridge as the witness render
        // (PR #607): the bridge then resolves bare-vs-FQN with the seam declaration
        // instead of adding a bare import of a colliding simple name.
        String witnessRender = facts.isMulti()
                && com.regnosys.rosetta.generator.java.template.ImportCollisionResolver
                        .hasSentinel(facts.multiElementText())
                ? facts.multiElementText() : null;
        return new AliasValueSeam(facts.isMulti(), facts.multiWitness(), witnessRender);
    }

    /**
     * The NON-COUNTING cardinality twin (T3, PR-24): the same window-gated
     * policy read as {@link #aliasValueSeamOrNull} WITHOUT the leaf-wrap
     * counter merge — the guard-seat predicate channel (the ctor
     * bridge-cardinality guard reads it per candidate value, so a counting
     * read would inflate the emission tallies the gate pins).
     */
    @Override
    public Boolean aliasValueSeamIsMultiOrNull(RFunction enclosingFunc, RShortcut alias) {
        if (optimisedWindowDepth == 0 || enclosingFunc == null || alias == null) {
            return null;
        }
        AliasValueSeamPolicy.FlipFacts facts =
                aliasValueSeamPolicy().flipFactsOrNull(enclosingFunc, alias);
        return facts == null ? null : facts.isMulti();
    }

    /** Leaf bridge-wrap emissions at alias invocation seats ({@code S=n C=m}; {@code none} when zero). */
    public String aliasLeafWrapBreakdown() {
        return renderBreakdown(aliasLeafWraps);
    }

    /** Converted ALIAS_CALL-rooted ladder events (the § 5 CHAIN_ROOT deliverable). */
    public int convertedAliasRootChains() {
        return convertedAliasRootChains;
    }

    /** DISTINCT ALIAS_CALL-rooted AST sites converted (identity). */
    public int convertedAliasRootDistinctSites() {
        return convertedAliasRootSites.size();
    }

    /** The converted ALIAS_CALL site set (identity, unmodifiable) — the completeness law's read. */
    public java.util.Set<RExpression> convertedAliasRootSiteSet() {
        return java.util.Collections.unmodifiableSet(convertedAliasRootSites);
    }

    /** Per-shape ALIAS_CALL conversion breakdown ({@code SINGLE=n …}; {@code none} when zero). */
    public String convertedAliasRootByShapeBreakdown() {
        return renderBreakdown(convertedAliasRootByShape);
    }

    /** STREAM-shaped ALIAS_CALL classify-passes kept on the bridge composition (deliberate). */
    public int declinedAliasRootStream() {
        return declinedAliasRootStream;
    }

    /** ALIAS_CALL conversion bails ({@code reason=n}; {@code none} when zero) — every bail keeps the reference emission. */
    public String aliasRootBailBreakdown() {
        return renderBreakdown(aliasRootBails);
    }

    /** The total ALIAS_CALL bails. */
    public int aliasRootBailTotal() {
        int total = 0;
        for (int n : aliasRootBails.values()) {
            total += n;
        }
        return total;
    }

    /** ALIAS_CALL classify-passes suppressed inside an outer ITEM plan's reference render. */
    public int aliasRootInteriorSuppressed() {
        return aliasRootInteriorSuppressed;
    }

    /**
     * Per-hop-count conversion breakdown for META-FREE plans, {@code 1=n 2=m …}
     * ({@code none} when zero) — receipt-comparable with the § 4/§ 9 receipts; the
     * meta pool prints its own {@link #convertedMetaByStepsBreakdown}.
     */
    public String convertedByHopsBreakdown() {
        return renderBreakdown(convertedByHops);
    }

    /**
     * Per-ACCESSOR-STEP conversion breakdown for META-bearing plans (census § 10 —
     * ladder steps, interior {@code getValue} derefs baked in).
     */
    public String convertedMetaByStepsBreakdown() {
        return renderBreakdown(convertedMetaBySteps);
    }

    /**
     * Per-shape conversion breakdown ({@code SINGLE=n TERMINAL_MULTI=k
     * SINGLE:meta=… TERMINAL_MULTI:meta=…} — meta-bearing plans ride disjoint
     * {@code :meta} keys so the § 9b values stay receipt-comparable).
     */
    public String convertedByShapeBreakdown() {
        return renderBreakdown(convertedByShape);
    }

    private static String renderBreakdown(Map<?, Integer> map) {
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

    @Override
    public JavaStatementBuilder visitFeatureCall(RFeatureCall expr, ExpressionContext ctx) {
        return convertOrDelegate(expr, () -> super.visitFeatureCall(expr, ctx));
    }

    @Override
    public JavaStatementBuilder visitEnumValueRef(REnumValueRef expr, ExpressionContext ctx) {
        return convertOrDelegate(expr, () -> super.visitEnumValueRef(expr, ctx));
    }

    /** The classify owner: AST ancestry first, the § 12b.2 bridge as fallback. */
    private RFunction ownerFor(RExpression expr) {
        RFunction ancestry = HandlerHelper.findEnclosingFunction(expr);
        return ancestry != null ? ancestry : bridgeOwner;
    }

    /**
     * THE ITEM-ROOT HARVEST (census § 12b.3 — the render-idiom channel): the
     * reference render of an ITEM-rooted chain spells the seat's own root
     * variable followed by a Mapper step —
     * {@code <rootIdent>.<X>map("…", …)} / {@code <rootIdent>.map(…)} — the
     * SAME textual render-idiom contract the § 5a clause-3 prefix guards stand
     * on. The AST is NOT the channel: item chains arrive in BOTH AST spellings
     * (feature-call spines AND linker-bound {@code REnumValueRef} re-spellings
     * — the first § 12 emission run's debug receipts), elided receivers have
     * no root node at all, and the render's implicit-receiver machinery
     * re-spells interiors as parent-unwired twins — only the RENDERED TEXT is
     * seat-truth. A lexical prefix read of the compiler's OWN just-rendered
     * expression, belted: the prefix must be a bare Java identifier,
     * immediately followed by {@code .} and a Mapper step ({@code <}
     * type-witness or {@code map(}) — anything else returns null and the
     * caller bails conservatively (counted + pinned). Only a Mapper receiver
     * can spell {@code .map} in the render, and the plan's
     * {@code rootMulti=false} (every converting pool) makes {@code .get()} the
     * exact single-value unwrap.
     *
     * <p>v3.1 C2d family 10, PR #617 — the c10 census MEASURED legs (1) and
     * (2) of the refutation above and did NOT measure leg (3): the probe read
     * no {@code .parent()} link, so the parent-unwired-twins clause stands as
     * this javadoc's design rationale, not as a census fact. The adjudication
     * and its figures are at the ONE call site — the
     * {@code facet itemRootRenderHarvest} block in {@code convertOrDelegate}'s
     * item-rooted arm.
     */
    private static String harvestedItemRootIdent(String referenceText) {
        int i = 0;
        int n = referenceText.length();
        if (n == 0 || !Character.isJavaIdentifierStart(referenceText.charAt(0))) {
            return null;
        }
        while (i < n && Character.isJavaIdentifierPart(referenceText.charAt(i))) {
            i++;
        }
        if (i >= n - 1 || referenceText.charAt(i) != '.') {
            return null;
        }
        char step = referenceText.charAt(i + 1);
        boolean mapperStep = step == '<'
                || referenceText.startsWith("map(", i + 1);
        return mapperStep ? referenceText.substring(0, i) : null;
    }

    /**
     * The single conversion seat both chain spellings route through: window + context
     * gates, the shared classifier, the reference render (harvested, side effects
     * preserved), the ladder swap. Anything ineligible delegates untouched.
     */
    private JavaStatementBuilder convertOrDelegate(RExpression expr,
            Supplier<JavaStatementBuilder> reference) {
        if (optimisedWindowDepth == 0 || operationDepth == 0) {
            return reference.get();
        }
        RFunction owner = ownerFor(expr);
        // THE § 6.3 ALIAS-ROOT ADMISSION (T1): chains rooted at a VALUE-TYPED
        // alias invocation classify through the opt-in channel — admitted
        // exactly when the flip policy says the owner's alias is value-seamed
        // (the same policy instance the leaf and the seam emission answer
        // from). A null owner supplies no admission (nothing to flip against).
        NavigationChainClassifier.AliasRootAdmission aliasAdmission = owner == null ? null
                : name -> {
                    AliasValueSeamPolicy.FlipFacts f =
                            aliasValueSeamPolicy().flipFactsByNameOrNull(owner, name);
                    return f == null || !f.aliasRootLadderEligible() ? null
                            : new NavigationChainClassifier.AliasRootFacts(f.isMulti());
                };
        NavigationChainClassifier.LadderPlan plan =
                classifier.classify(expr, generatorModel.workspace(), owner, aliasAdmission)
                        .orElse(null);
        if (plan == null) {
            return reference.get();
        }
        boolean itemRooted =
                plan.rootKind() == NavigationChainClassifier.LadderPlan.RootKind.ITEM;
        boolean aliasRooted =
                plan.rootKind() == NavigationChainClassifier.LadderPlan.RootKind.ALIAS_CALL;
        if (aliasRooted && itemRenderDepth > 0) {
            // The § 12b.3 interior-suppression law applied to the ALIAS_CALL
            // pool: inside an outer ITEM plan's reference render, an interior
            // re-spell converting would pollute the outer harvest text.
            // Expected ZERO on this corpus (pinned); the reference emission —
            // the bridge composition — is value-identical either way.
            aliasRootInteriorSuppressed++;
            return reference.get();
        }
        if (itemRooted && itemRenderDepth > 0) {
            // THE ITEM-INTERIOR SUPPRESSION (census § 12b.3): inside an
            // ITEM-rooted plan's OWN reference render, the render machinery
            // re-spells INTERIOR segments as parent-unwired twins
            // (candidate-true by construction) and re-dispatches them through
            // the visitor — an interior twin converting would pollute the outer
            // site's reference text and forfeit the whole-chain harvest
            // (measured: the first § 12 emission run's 15-site shortfall —
            // every polluted text embedded an interior
            // `MapperS.of((item.get()…` swap). Interior ITEM re-spells stay
            // reference (counted — a twin class, never a walk site); the outer
            // then harvests clean text and converts the WHOLE chain. PARAM
            // plans are NOT gated: their interiors are param-rooted, a param
            // swap never reads its reference text (ladderCode), and the § 9b
            // twin-conversion behaviour is landed coverage.
            itemInteriorSuppressed++;
            return reference.get();
        }
        JavaStatementBuilder referenceBuilder;
        if (itemRooted) {
            itemRenderDepth++;
        }
        try {
            referenceBuilder = reference.get();
        } finally {
            if (itemRooted) {
                itemRenderDepth--;
            }
        }
        if (!(referenceBuilder instanceof JavaExpression referenceExpr)) {
            // A statement-composite reference shape is out of the ladder's drop-in
            // contract — keep the reference emission; the PARAM-pool pin
            // (eligibleBailedNonExpression == 0) surfaces this loudly if it ever
            // fires on an eligible site; ITEM plans ride their own bail row so
            // the pin stays receipt-comparable.
            if (itemRooted) {
                bailItem("nonExpression");
            } else {
                eligibleBailedNonExpression++;
            }
            return referenceBuilder;
        }
        NavigationChainClassifier.LadderPlan.Shape shape = plan.shape();
        boolean meta = plan.bearsMeta();
        if (shape == NavigationChainClassifier.LadderPlan.Shape.STREAM) {
            // THE MEASURED STREAM DECLINE — TWICE priced, twice red (the
            // fixed-cost-vs-N law): (1) census § 9d — the § 9a stream pipeline
            // measured ~405 MB/op ABOVE the TERMINAL_MULTI-only tree on DRR;
            // (2) census § 11 — the loop-form re-price ({@link NavigationHelperForm},
            // zero stream machinery) measured +494.7 MB/op ABOVE the same tree
            // (6-fork same-day A/B, fully disjoint). The shared mechanism: the
            // boundary contract needs a MATERIALIZED flattened List, and that list
            // ESCAPES into the long-lived MapperC where the reference chain's
            // intermediate MapperItems die young and are escape-analysis-eliminated
            // — the cost is the structure, not the stream-vs-loop spelling.
            // STREAM-shape sites stay reference-emitted DELIBERATELY (a priced
            // scope cut, not a bail); a re-price re-enters ONLY with a changed
            // boundary contract (e.g. a lazy MapperC surface — a runtime change,
            // out of this family's scope). The decline CARRIES to the family-2
            // meta pool (census § 10a) and to the family-3 item pools (census
            // § 12 — the same economics), each counted apart so the § 9d tuple
            // stays receipt-comparable.
            if (aliasRooted) {
                // THE ALIAS_CALL STREAM DECLINE (§ 6.3 T1): a multi-seamed
                // alias root and/or an interior multi hop — the reference
                // emission here IS the bridge composition (the leaf wrap plus
                // Mapper hops), value-identical by the same § 9d/§ 11
                // economics that decline every other STREAM pool. Disjoint
                // counter; pinned per cell.
                declinedAliasRootStream++;
            } else if (itemRooted) {
                if (meta || plan.rootMeta() != NavigationChainClassifier.LadderPlan.TopMeta.NONE) {
                    declinedItemMetaStream++;
                } else {
                    declinedItemStream++;
                }
            } else if (meta) {
                declinedMetaStream++;
            } else {
                declinedStreamShape++;
            }
            return referenceBuilder;
        }
        String witness = null;
        RJavaWithMetaValue metaWitnessClass = null;
        JavaClass<?> plainWitnessClass = null;
        if (shape != NavigationChainClassifier.LadderPlan.Shape.SINGLE) {
            // The multi boundary witness (MapperC.<X>of — § 9a clause 4): X is the
            // chain's ELEMENT type, translated from the TOP hop's IR type through
            // the SHARED JavaTypeTranslator — the same fact source the reference
            // render's own <X>mapC/<X>map witnesses come from (the render pipeline
            // does not reliably carry harvested JavaTypes on chain expressions, and
            // a receiver position is a standalone context, so bare `MapperC.of`
            // inference could pick a narrower element than the reference produced
            // and break invariant seats). The fact is measured COMPLETE on the
            // priced pool (census § 2: hopTypeMissing onPure = 0 on every FUNCTION
            // face; census § 10: siteOperationMetaTopTypeMissingByShape none) — the
            // isMissing bail below is a zero-pinned belt for walk sites. THE
            // WRAP-CLASS CLAUSE (§ 9a clause 5 — areEqual dispatches on the
            // operands' RUNTIME class) holds by the render's own cardinality rules:
            // the same IR facts that make this plan a multi shape make the
            // reference render a MapperC; the differential gate is the oracle.
            if (plan.topType().isMissing()) {
                // RENDER-SYNTHESIZED TWINS ONLY: the legacy render's re-spelled
                // call-argument/constructor nav nodes (`ReferenceHandler.synthesize*`)
                // carry no linker typing, so their adapted top types are MISSING and
                // the witness cannot be built. Those seats KEEP the reference
                // emission (value-identical by definition — it IS the reference
                // text); the walk-visible priced pool is untouched (the gate's
                // shortfall == 0 proves every walk site converted, so a fallback
                // here is never a walk site — census § 12: topTypeMissingMulti=0
                // on both family-3 walk pools too). Counted separately per family
                // and pinned exactly — never folded into the render-shape bail pin;
                // ITEM plans ride their own bail row so the § 9b/§ 10b twin pins
                // stay receipt-comparable.
                if (aliasRooted) {
                    bailAliasRoot("multiTopTypeMissing");
                } else if (itemRooted) {
                    bailItem("multiTopTypeMissing");
                } else if (meta) {
                    metaTwinTypeMissingFallbacks++;
                } else {
                    multiTwinTypeMissingFallbacks++;
                }
                return referenceBuilder;
            }
            if (plan.topMeta() != NavigationChainClassifier.LadderPlan.TopMeta.NONE) {
                // THE META-TOP WITNESS (census § 10a): the chain's element is the
                // WRAPPER class (`<FieldWithMetaX>mapC` in the reference render) —
                // derived through the SAME factory the reference witness rides
                // (NavigationHandler.metaNavResultType → RJavaWithMetaValue.create):
                // the REF/FIELD kind from the classifier's detectMetaKind-mirroring
                // vocabulary lock, the value type from the TOP hop's IR type via
                // the SHARED JavaTypeTranslator. The wrapper class joins the refs
                // below (the MAPPER_C-belt precedent — idempotent when the
                // reference render already registered it via metaNavResultType).
                metaWitnessClass = RJavaWithMetaValue.create(
                        plan.topMeta() == NavigationChainClassifier.LadderPlan.TopMeta.REF,
                        typeTranslator.toJavaReferenceType(plan.topType().type()),
                        getTypeUtil());
                witness = metaWitnessClass.getSimpleName();
            } else {
                // THE FQN-WITNESS LAW (#227, the reference render's own rule for
                // every non-meta witness position — ControlFlowHandler /
                // ConstructionHandler's typeRef seats): the witness renders as the
                // import-collision SENTINEL carrying the canonical name, and the
                // file's first-claim-wins resolver decides bare-vs-FQN, dropping
                // the import of a loser. A bare simple name here re-imported the
                // loser: cdm 6.x's ingest functions navigate fpml types whose simple
                // names the CDM output already claims (AdjustableOrRelativeDate,
                // ExerciseNotice, …), and the reference `<X>map` witnesses were the
                // sentinels that kept those imports suppressed — javac's catch on
                // the widened pair gate (PR #607: 24 files per cdm 6.20.x cell). A
                // meta wrapper never collides (the plain simple name above).
                plainWitnessClass = typeTranslator.toJavaReferenceType(plan.topType().type());
                if (plainWitnessClass == null) {
                    // A type the translator cannot name (the isMissing belt above
                    // guards the IR channel, this guards the translation) keeps the
                    // reference emission through the same twin-type-missing rows.
                    if (aliasRooted) {
                        bailAliasRoot("multiTopTypeMissing");
                    } else if (itemRooted) {
                        bailItem("multiTopTypeMissing");
                    } else {
                        multiTwinTypeMissingFallbacks++;
                    }
                    return referenceBuilder;
                }
                witness = com.regnosys.rosetta.generator.java.template.ImportCollisionResolver
                        .typeRef(plainWitnessClass.getCanonicalName().withDots());
            }
        }
        // THE ITEM-ROOT LADDER (census § 12b.3): the reference render's OWN
        // spelling of the root — a bare identifier naming the seat's
        // single-cardinality Mapper wrap (`item` in structural lambdas,
        // `thenArgN` at then-stage seats), harvested from the just-rendered
        // reference text through the render-idiom channel — value-unwrapped
        // ONCE via `.get()` (a pure allocation-free read, repeated per
        // null-guard level exactly as the landed ladder repeats getters — the
        // same JIT-CSE law; the § 12c clause-6 ledger: the then-stage boundary
        // already value-unwraps in the reference render itself:
        // `MapperS.of(thenArgN.get())`). Every belt bails CONSERVATIVELY to
        // the reference emission and is counted + pinned; a wrapper-typed root
        // (measured ZERO on the frozen corpus) would need a leading deref the
        // accessors do not carry — belted, never mis-emitted.
        String ladder;
        if (aliasRooted) {
            // THE ALIAS-ROOT LADDER (§ 6.3 T1 — the § 5 CHAIN_ROOT form): the
            // ladder roots at the alias INVOCATION and descends by the landed
            // hops — no NamedFunctionImpl, no Mapper hops (deliverable 3). The
            // invocation spelling comes from the policy's FlipFacts — the SAME
            // public authorities the invocation seat itself consults (the
            // FlipFacts javadoc's laws), NOT a text harvest: an EnumValueRef-
            // spelled chain's reference render synthesizes a twin FeatureCall
            // that CONVERTS FIRST (the § 9b twin-excess pattern), so the outer
            // text is already a swap — the built invocation replaces the whole
            // text at the OUTER (walk-visible) seat, exactly like the PARAM
            // pool's ladderCode. The re-invocation per null-guard level is the
            // ratified § 5 row's own form (call-by-name preserved per
            // REFERENCE; a T1 alias body is a pure getter ladder). Belts bail
            // CONSERVATIVELY to the reference emission — the bridge
            // composition, value-identical.
            AliasValueSeamPolicy.FlipFacts rootFacts = owner == null ? null
                    : aliasValueSeamPolicy().flipFactsByNameOrNull(owner, plan.rootName());
            if (rootFacts == null) {
                bailAliasRoot("factsMissing");
                return referenceBuilder;
            }
            ladder = plan.ladderOver(rootFacts.invocationText(), 0, plan.accessors().size());
        } else if (itemRooted) {
            if (plan.rootType().isMissing()) {
                // A MISSING node-channel root type cannot PROVE the item is not
                // wrapper-typed (the § 10 wrapper-signature exclusion needs a
                // reliable channel; an implicit item has no declaration channel
                // at all) — and an undetected wrapper root emits a ladder
                // missing its leading deref (the first § 12 gate run's drr
                // compile catch: `thenArg4.get().getIdentifier()` on a
                // ReferenceWithMetaProductIdentifier item — the census § 12a
                // rootTypeMissing=44 subset was exactly the unprovable class).
                // Refuse conservatively; the walk filter mirrors this, so the
                // completeness law stays identity-exact.
                bailItem("rootTypeMissing");
                return referenceBuilder;
            }
            if (plan.rootMeta() != NavigationChainClassifier.LadderPlan.TopMeta.NONE) {
                bailItem("wrappedRoot");
                return referenceBuilder;
            }
            // facet itemRootRenderHarvest (v3.1 C2d retirement family 10, PR #617) —
            // JUSTIFIED-KEPT at the seat-30 bar. The c10 census (round c10 at 671a1f7c7,
            // target/seat617-instruments/c2c-c10-verdicts.md (local)): 18,494 arrivals, ALL
            // on the optimised walk (0 on either D11 route — the seat exists on one route,
            // so LAW 77 holds by construction), harvest non-null 18,494/18,494 — the
            // rootScan belt NEVER FIRES on this corpus (0/18,494); its liveness is lane
            // L10's receipt, MEASURED at the #617 lane run at 318fd30ba: the harvest result
            // nulled -> optimised 12/1F (the per-cell COMPLETENESS law fires at the first
            // cell under the forced bails) with the generator leg GREEN (207/0F), as the
            // optimised-only seat map requires. (The two commits since are comment-only in
            // this file — the mutated anchor line is byte-identical — so the figures stand
            // at this head with no re-run owed.)
            // The javadoc's three-way AST refutation, stated to what the probe READ (the
            // spec review's MF-3): (1) dual spellings MEASURED — exprKind RFeatureCall
            // 11,920 / REnumValueRef 6,574 (35.5% arrive linker-re-spelled; the spine
            // terminal REnumValueRef@0 6,574 is BY CONSTRUCTION this same population, one
            // finding, not two); (2) unnamed roots — 9,353 spines end at a nameless
            // RImplicitVariable (@1 9,203 / @2 150) and only RSymbolReference@1 24 (0.13%)
            // names a root VARIABLE (the 9,117 REnumValueRef terminals name enum values —
            // leg-1 re-spellings, not roots); the probe's null-receiver sentinel fired
            // 0/18,494 (disclosed — no literally-elided RFeatureCall receiver at this
            // corpus); (3) parent-unwired twins NOT measured (no .parent() read; the
            // REnumValueRef@1..@7 interior terminals, 2,543, are consistent with the twin
            // mechanism but do not prove it — the disposition does not rest on this leg).
            // THE DECISIVE FINDING: the harvested identifier's VALUE is frequently a name
            // no AST or model channel can hold — item 16,819, _item 979 (the #292
            // depth-escape convention) and EXACTLY 696 minted locals (__STMT_HOIST_* 592 +
            // __COERCION_PARAM_* 104, OTHER 0 and the four buckets summing to 18,494 — the
            // analyser's identClass census prints the split and the total), minted by the
            // hoist session and the coercion machinery at render time. A typed channel
            // needs the render pipeline to carry the root ident on the JavaExpression
            // itself (a producer migration, NONE-YET).
            String rootIdent = harvestedItemRootIdent(referenceExpr.toString());
            if (rootIdent == null) {
                bailItem("rootScan");
                return referenceBuilder;
            }
            ladder = plan.ladderOver(rootIdent + ".get()", 0, plan.accessors().size());
        } else {
            ladder = plan.ladderCode();
        }
        if (aliasRooted) {
            // DISJOINT counters (the § 9b/§ 10b/§ 12 receipts stay
            // byte-comparable): alias-root events never bump the landed
            // family tuples.
            convertedAliasRootChains++;
            convertedAliasRootSites.add(expr);
            convertedAliasRootByShape.merge(shape.name() + (meta ? ":meta" : ""),
                    1, Integer::sum);
        } else {
            convertedChains++;
            convertedSites.add(expr);
            if (itemRooted) {
                convertedItemBySteps.merge(plan.accessors().size() + (meta ? ":meta" : ""),
                        1, Integer::sum);
            } else if (meta) {
                convertedMetaBySteps.merge(plan.accessors().size(), 1, Integer::sum);
            } else {
                convertedByHops.merge(plan.accessors().size(), 1, Integer::sum);
            }
            convertedByShape.merge(shape.name() + (meta ? ":meta" : "")
                    + (itemRooted ? ":item" : ""), 1, Integer::sum);
        }
        // THE CONSUMPTION-SHAPE CONTRACT (census § 5a + § 9a — the differential-gate
        // catches distilled): the swap text must be consumed EXACTLY like the
        // reference chain it replaces — (1) NOT structurally unwrappable
        // (wrappedInMapperSOf's unwrapToBuilder channel let an ADD seat substitute
        // the bare single-value ladder into addAll(...)); (2) NOT a textual
        // whole-wrap either (HandlerHelper.unwrapMapperSOf and its both-kinds
        // generalisation strip bare Mapper{S,C} wraps at SET/ADD statement seats —
        // the filter suffix closes the balanced scan short); (3) still STARTING with
        // "MapperS.of(" / "MapperC." (the wrap-avoidance guards key on those
        // prefixes to recognize an already-Mapper operand — a non-matching prefix
        // would double-wrap); (4) INFERENCE-TRANSPARENT — filterSingle/filterItem
        // return the NON-generic Mapper{S,C}<T> (T receiver-bound), and the multi
        // boundary `of` is WITNESSED with the harvested element type because a
        // receiver position is a standalone context (bare inference could pick a
        // narrower element than the reference produced and break invariant seats);
        // and (5) THE WRAP-CLASS CLAUSE above. filterSingle is a runtime identity
        // (true-predicate → this; error item → this; zero allocation);
        // filterItem(sameNav -> true) keeps every non-error item — the error-item
        // drop is value-invisible (census § 9 null/error ledger).
        // The multi suffix is unionSame(MapperC.<X>ofNull()) — the C-side twin of
        // filterSingle's textual roles (§ 9a clauses 2-3) chosen for COST and
        // FIDELITY over filterItem: unionSame appends an empty item list via one
        // arraycopy (no per-element allocation — filterItem allocated a MapperS
        // wrapper per element and measurably gave back the tranche-1 DRR allocation
        // win), and it preserves the boundary's error items VERBATIM (reference
        // chains keep error items too — the § 9 ledger's value surfaces are
        // identical either way).
        String swap = switch (shape) {
            case SINGLE -> "MapperS.of((" + ladder
                    + ")).filterSingle(sameNav -> true)";
            case TERMINAL_MULTI -> "MapperC.<" + witness + ">of((" + ladder
                    + ")).unionSame(MapperC.<" + witness + ">ofNull())";
            // Unreachable while the decline stands (both families return above) —
            // throw rather than silently activate a measured-OUT form if the
            // decline block is ever removed without a GREEN re-price (Seat-1 PR-6
            // N3). BOTH built forms are red on this pool: the § 9a stream pipeline
            // (§ 9d, ~+405 MB/op) and the § 11 loop-form helpers
            // ({@link NavigationHelperForm} — the design of record, gate-proven
            // equivalent, +494.7 MB/op on the 6-fork A/B).
            case STREAM -> throw new IllegalStateException(
                    "STREAM swaps are declined (census § 9d + § 11 — both re-forms"
                            + " measured out) — a re-price must land its own emission"
                            + " on a changed boundary contract, never re-enter this"
                            + " switch");
        };
        // THE SPELLED-REFS LAW (PR #607): the swap's refs are EXACTLY the classes
        // its text spells — the wrapper class token (MapperS for the SINGLE
        // ladder, MapperC for the multi boundary — the refs channel is the ONLY
        // correct route for those: § 9a clause 3's wrap-avoidance guards key on
        // the `MapperS.of(` / `MapperC.` prefixes, so an FQN-inline spelling would
        // break them; the equivalence gate's compile leg caught the missing
        // MapperS import loud on the first § 12 gate run — cannot find symbol
        // ×3/×4/×100 per cell) plus the multi witness class (the meta wrapper by
        // its plain name, the plain class through the sentinel above so the
        // resolver can still drop it if it loses). Until #607 the swap inherited
        // the REFERENCE chain's refs — the `<X>map` witness classes of every hop —
        // while spelling none of them: the reference resolved a colliding hop
        // witness by sentinel and dropped the loser's import, the ladder kept the
        // ref with no sentinel to resolve, and the file gained a second import of
        // the same simple name (cdm 6.x `cdm/ingest/**`, 24 files per 6.20.x cell,
        // javac's duplicate-import error — invisible on the ring, whose cdm 6.20.6
        // gate excluded exactly that directory). A ladder is pure getters: a PLAIN
        // hop import it does not spell is one it must not carry.
        //
        // THE TYPE-FACT CHANNEL (PR #607, the widened gate's second catch on this
        // law — LAW 69, the two halves agree): the refs set is not only an import
        // list. The then-stage decl typing READ it as a fact channel until PR #613 —
        // FunctionExpressionRenderer.renderThenExtractSetImpl recovered the hoisted
        // local's element wrapper from the compiled value's refs
        // (NavigationHandler.uniqueMetaWrapperForValueType: the meta-blind inferred
        // `Party` became `ReferenceWithMetaParty` iff exactly one wrapper of that
        // value type was registered). PR #613 (v3.1 C2d retirement family 6) retired
        // that scan and its siblings for NavigationHandler.levelMetaWrapper, whose
        // rule 1 reads the level's COMPILED item — this swap's own type stamp — so the
        // wrapper refs kept below are now the import list's business only; the
        // finding that follows is the history of why they are kept. The first cut
        // of this law dropped EVERY
        // inherited ref, so a chain whose value IS a wrapper re-typed its then
        // local bare — `final MapperS<Party> thenArg = MapperS.of((… .getReporting
        // Party()))` — javac's catch on the widened pair gate (drr Counterparty1 /
        // Counterparty2 / Counterparty2IdentifierType, in EVERY drr cell incl. the
        // ring's 6.34.1; the ring's own emission gate cannot see a decl type). The
        // reference chain's META WRAPPER refs are therefore KEPT: they are the
        // reference render's own registrations (metaNavResultType), the golden's
        // decl spells the same wrapper so the import is the golden's own, a
        // wrapper never collides, and the recovery then reads exactly the wrapper
        // set the reference route reads (the same unique-scan verdict).
        Set<JavaClass<?>> refs = new LinkedHashSet<>();
        refs.add(shape == NavigationChainClassifier.LadderPlan.Shape.SINGLE
                ? HandlerHelper.MAPPER_S : HandlerHelper.MAPPER_C);
        for (JavaClass<?> inherited : referenceExpr.getRefs()) {
            if (inherited instanceof RJavaWithMetaValue) {
                refs.add(inherited);
            }
        }
        if (metaWitnessClass != null) {
            refs.add(metaWitnessClass);
        }
        if (plainWitnessClass != null) {
            refs.add(plainWitnessClass);
        }
        return JavaExpression.from(swap,
                referenceExpr.getExpressionType(), refs,
                referenceExpr.getStaticWildcardImports());
    }

    private void bailItem(String reason) {
        itemHarvestBails.merge(reason, 1, Integer::sum);
    }

    private void bailAliasRoot(String reason) {
        aliasRootBails.merge(reason, 1, Integer::sum);
    }

}
