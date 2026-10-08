package com.regnosys.rosetta.generator.java.optimised;

import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.expressions.references.REnumValueRef;
import com.regnosys.rosetta.ast.expressions.references.RSymbolReference;
import com.regnosys.rosetta.ast.expressions.supporting.ROnlyExistsElement;
import com.regnosys.rosetta.ast.functions.RCondition;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.functions.ROperation;
import com.regnosys.rosetta.ast.functions.RPostCondition;
import com.regnosys.rosetta.ast.functions.RShortcut;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.function.FunctionAliasHelper;
import com.regnosys.rosetta.generator.java.function.FunctionTemplateModel;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.generator.java.types.RJavaWithMetaValue;
import com.regnosys.rosetta.ir.expr.IRExpr;
import com.regnosys.rosetta.ir.expr.IRExprKind;
import com.regnosys.rosetta.ir.expr.IRMetaAccess;
import com.regnosys.rosetta.ir.expr.adapter.ExpressionToIRAdapter;
import com.rosetta.util.types.JavaClass;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * THE § 6.3 FLIP POLICY — the per-alias VALUE-SEAM eligibility predicate of the
 * alias re-typing program's landed emitter tranches (T1 at PR #558; T1∪T2 at
 * PR #559; T1∪T2∪T3 at PR-24; T1∪T2∪T3∪T4 since PR-25 — the program plan
 * the development plan "2026-08-12-p3-63-alias-retyping-program-plan" §§ 3–6, 8;
 * the T0 disposition map {@code AliasDispositionMapTest} is the membership
 * authority this predicate must reproduce EXACTLY — the map test asserts
 * {@code flips == (tranche ∈ {T1, T2, T3, T4})} over every alias row of every
 * cell with per-row form/S-C/top-kind cross-checks, and the equivalence gate's
 * O5-protected channel reconciles the emitted seam pairs to the same
 * landed-tranche receipt — 1,758 members at T4, 480/850/428 per cell).
 *
 * <p><b>The predicate</b> (the map's {@code assignTranche} precedence, restated
 * over generation-visible channels — every leg per-member, never in-head):
 * an alias flips to the value-typed seam exactly when ALL of:
 * <ol>
 *   <li><b>the body form admits</b> — EITHER whole-body-one-chain (the body
 *       classifies as a PARAM-rooted ladder plan via
 *       {@link NavigationChainClassifier} — the same shared predicate instance
 *       the emitter and census ride; the plan's shape picks the T1 § 4 body
 *       form) OR the body ADAPTS at all (T2 at PR #559 admitted the APPLY /
 *       FIELD_ACCESS / META_ACCESS tops; T3 at PR-24 admits EVERY remaining
 *       adapted top kind — the map's {@code assignTranche} fall-through leg,
 *       making the body-form leg total over the ¬D population; the § 4
 *       value-body forms — the direct call composition, the disclosed boundary
 *       fallback, the decorated ladder/hoist variants — are the emitter's
 *       per-shape choice at {@code OptimisedFunctionGenerator.valueBodyForm});</li>
 *   <li><b>¬D3</b> — not a {@code usesOutput} alias (the builder-typed class);</li>
 *   <li><b>¬D1/¬D1T</b> — no only-exists consumer seat (the census's
 *       ANCESTOR-classified rule: any {@link RSymbolReference} alias reference
 *       under an {@link ROnlyExistsElement} subtree) and not reachable from an
 *       excluded alias's verbatim Mapper body along alias→alias reference edges
 *       (the D1 transitive closure, computed to fixpoint per function);</li>
 *   <li><b>the boundary witness exists</b> — a MULTI seam needs the chain's
 *       element class for the bridge/boundary witness
 *       ({@code MapperC.<X>of(...)}); a missing top type refuses conservatively
 *       (measured ZERO on the priced pool — census § 2; the map belt fires if a
 *       T1 row ever hits this), and the seam string's element simple name must
 *       agree with the translated witness (the same-walk law, belted).</li>
 * </ol>
 *
 * <p><b>The T4 careful classes (PR-25 — the plan § 8 row T4, three former
 * refusal legs LIFTED with their disciplines):</b>
 * <ul>
 *   <li><b>dispatch case units</b> — a variant function's aliases flip like any
 *       other member's; the unit's atomicity (the abstract seam + the
 *       {@code Default} override re-type TOGETHER — the § 11 rule) holds BY
 *       CONSTRUCTION: both lines render from the ONE {@code AliasModel} this
 *       policy's form replaces, and javac enforces the signature pairing. The
 *       per-member facts thread the DISPATCH BASE exactly where the reference
 *       render does ({@code HandlerHelper.dispatchBaseOf} — the
 *       {@code renderEnclosingInputs} / {@code buildStandardModel}
 *       signatureSource law): the invocation args and the escape belt walk the
 *       BASE's declared inputs while the dependency/operator collision laws
 *       stay on the VARIANT (the emitted class's own surface).</li>
 *   <li><b>D5 rows (render-authority)</b> — where the RENDER-channel S/C bit
 *       (the seam {@code FunctionAliasHelper} records beside the signature it
 *       emits — MAPPER_SINGLE / MAPPER_MULTI, the shipped truth per plan § 3)
 *       disagrees with the IR cardinality channel (the adapted body's
 *       {@link com.regnosys.rosetta.types.ExpressionCardinality}), the RENDER bit governs the value seam
 *       — {@code isMulti} reads the RENDER seam alone, never the IR bit; the
 *       map's {@code d5} column + the O5p reconciliation carry the per-member
 *       receipt (which channel said what, which shape shipped). An un-adaptable
 *       body ({@code adaptEmpty} — corpus-zero) still refuses.</li>
 *   <li><b>D4-live rows (oracle-covered)</b> — zero references on BOTH counting
 *       channels with a DISGUISED head present ({@link REnumValueRef} naming
 *       the alias — the census § 19c channel-invisible spelling) no longer
 *       refuses: the golden-invocation oracle (the map's per-member seat
 *       enumeration — law 48's instrument) is the liveness receipt, and every
 *       enumerated seat renders through the SAME consumer machinery the landed
 *       tranches serve: a WHOLE-CHAIN careful member (the cdm6
 *       {@code cdm/ingest/**} class dominantly) is ladder-eligible under the
 *       T1 law, and its disguised chains — which the walk's disguised-head
 *       resolution CAN see — convert at operation seats like visible T1 roots
 *       (the emission gate re-pinned the alias-root conversions accordingly,
 *       cdm6 +195); every other seat takes {@code ReferenceHandler}'s
 *       structural bridge — the § 5 CHAIN_ROOT form.</li>
 * </ul>
 *
 * <p><b>Safety belt beyond the map:</b> a disguised head sitting INSIDE an
 * only-exists element ({@code disguisedOnlyExists}) refuses the flip regardless
 * of channel counts — the only-exists machinery consumes path structure
 * semantically (plan § 6 D1), and the bound-channel D1 rule cannot see a
 * disguised spelling. Corpus-expected ZERO; the map belt surfaces any firing as
 * a predicate/map divergence to decode BEFORE any emission ships.
 *
 * <p>One instance per rendering compiler; per-function analysis cached by
 * identity. The policy also carries the tranche's emission COUNTERS (seams
 * flipped per body form, leaf wraps) — the emission gate pins them per cell
 * against the landed-tranche receipt (480/850/428 members at T4 = the T1
 * 52/136/43 plus the T2 173/269/145 plus the T3 182/318/220 plus the T4
 * 73/127/20).
 */
public final class AliasValueSeamPolicy {

    /**
     * The flip facts of one value-typed alias seam. {@code invocationText} is
     * the seat-invocation spelling {@code renderedName(input1, input2, …)} —
     * derived from the SAME public authorities the invocation seat itself
     * consults ({@code FunctionDependencyCollector.hasFunctionDependencyNamed}
     * numbering + {@code HandlerHelper.staticOperatorMembersUsed} escape +
     * declaration-order ESCAPED input names ({@code escapedFunctionInputName},
     * PR #607) with the synthesized placeholder filtered —
     * {@code ReferenceHandler.disambiguateAliasInvocation} /
     * {@code renderEnclosingInputs}'s own laws, including the dispatch-base
     * thread for a variant's arg names — T4, PR-25), so the alias-rooted
     * ladder's root spelling and the leaf bridge's invocation cannot disagree.
     * {@code multiElementText} is the seam's own recorded element spelling verbatim —
     * an import-collision sentinel where the reference render emitted one — and
     * rides to the leaf bridge as the witness render so the bridge's
     * {@code MapperC.<X>of(…)} resolves bare-vs-FQN exactly as the seam
     * declaration does (PR #607).
     *
     * <p>{@code plan} is the whole-body-one-chain ladder plan (the T1 pool
     * plus the T4 whole-chain careful rows) and is {@code null} for a
     * VALUE-BODY member (the T2 chain/APPLY-headed class and the T3/T4
     * CONDITIONAL + tail kinds); {@code valueBodyTopKind} then carries the
     * adapted body's top kind (the map's {@code bodyTopKind} channel,
     * mirrored) and is {@code null} for plan-carrying members.
     */
    public record FlipFacts(boolean isMulti, JavaClass<?> multiWitness,
            NavigationChainClassifier.LadderPlan plan, String valueReturnType,
            String multiElementText, String invocationText,
            IRExprKind valueBodyTopKind) {

        /**
         * Whether chains ROOTED at this member's invocation may take the
         * alias-rooted ladder: a META-TOP member's seam element is the WRAPPER
         * class ({@code FieldWithMetaX}/{@code ReferenceWithMetaX}), and a
         * chain off it needs the render-side {@code getValue} coercion the
         * bare ladder cannot spell — the PARAM pool's wrapper-signature
         * exclusion (census § 10), applied at the alias-root seat. Refused
         * members' chain-root seats ride the leaf BRIDGE instead, whose
         * receiver-type channel composes the coercion correctly (the drr/cdm5
         * ResolveTransfer compile catch — {@code payout(instruction)
         * .getAssetPayout()} on a {@code ReferenceWithMetaPayout} value).
         *
         * <p>A VALUE-BODY member ({@code plan == null} — the T2 chain/APPLY
         * class AND the T3/T4 conditional/tail classes) is NEVER eligible: the
         * § 9a ladder re-invokes its root once per guard level (k+1 seam
         * calls per seat — the T1 getter-ladder repeat class, the JIT-CSE
         * law), but a value body invokes DEPENDENCY functions / full Mapper
         * pipelines / whole conditional ladders, so the repeat would multiply
         * real work per reference. The leaf bridge invokes the seam exactly
         * ONCE per seat — today's per-reference re-invocation count preserved
         * identically (plan § 2's call-by-name contract; the T2 landing
         * decision PR-23, extended verbatim at T3 PR-24 and T4 PR-25 — a T4
         * WHOLE-CHAIN member IS eligible like any T1-shaped body, and its
         * disguised consumer chains, which the walk's disguised-head
         * resolution resolves, convert at operation seats under the same
         * getter-repeat economics: the emission gate's alias-root pins grew
         * cdm6 +195 at PR-25, the mechanised receipt).
         */
        public boolean aliasRootLadderEligible() {
            return plan != null
                    && plan.topMeta() == NavigationChainClassifier.LadderPlan.TopMeta.NONE;
        }
    }

    private final GeneratorModel generatorModel;
    private final JavaTypeTranslator typeTranslator;
    private final JavaTypeUtil typeUtil;
    private final NavigationChainClassifier classifier;
    private final FunctionAliasHelper aliasHelper;
    private final ExpressionToIRAdapter adapter = new ExpressionToIRAdapter();

    private final Map<RFunction, Map<String, FlipFacts>> analysed = new IdentityHashMap<>();

    // ------------------------------------------------------------- counters --
    // (the tranche receipt's mechanised arm — read by the emission gate)
    private final Map<String, Integer> seamsByBodyForm = new TreeMap<>();
    private int seamsFlipped;
    private int witnessRefusals;
    private final List<String> witnessRefusalMembers = new ArrayList<>();

    public AliasValueSeamPolicy(GeneratorModel generatorModel, JavaTypeTranslator typeTranslator,
            JavaTypeUtil typeUtil, NavigationChainClassifier classifier) {
        this.generatorModel = generatorModel;
        this.typeTranslator = typeTranslator;
        this.typeUtil = typeUtil;
        this.classifier = classifier;
        this.aliasHelper = new FunctionAliasHelper(generatorModel, typeTranslator, typeUtil);
    }

    /** The flip facts for {@code alias} of {@code func}, or null (not flipped). */
    public FlipFacts flipFactsOrNull(RFunction func, RShortcut alias) {
        return func == null || alias == null ? null
                : analysis(func).get(alias.name());
    }

    /** The name-keyed twin (the chain-root admission's channel). */
    public FlipFacts flipFactsByNameOrNull(RFunction func, String aliasName) {
        return func == null || aliasName == null ? null
                : analysis(func).get(aliasName);
    }

    /** Record one emitted value seam (the generator's compileAliases seat). */
    public void recordSeamEmitted(FlipFacts facts) {
        seamsFlipped++;
        NavigationChainClassifier.LadderPlan.Shape shape = facts.plan().shape();
        seamsByBodyForm.merge(shape.name()
                + (facts.plan().bearsMeta() ? ":meta" : ""), 1, Integer::sum);
    }

    /**
     * Record one emitted VALUE-BODY seam (T2 PR-23; T3 PR-24; the T4
     * value-body classes PR-25 — dispatch/D5/D4-live members ride the same
     * per-shape forms) with the
     * emission-time form: {@code DIRECT} — the compiled body IS the value
     * already (the outer structural Mapper wrap stripped via
     * {@code unwrapToBuilder}; a zero-arg no-real-input call's bare value; a
     * bare constructor — the T3 coerce arm; an item-typed only-element/count
     * collapse — the T3 item-top arm); {@code BOUNDARY} — the disclosed
     * Mapper-bodied fallback, value-unwrapped once at the seam boundary
     * ({@code (…).get()} / {@code (…).getMulti()} — the § 4 tail clause, the
     * T1 STREAM precedent; ComparisonResult bodies included); {@code LADDER} /
     * {@code LADDER:switch} — the T3 decorated multi-return conditional /
     * instanceof-switch ladders (per-rung value decoration + the § 2 value
     * empties); a {@code :hoist} suffix marks the decl-led lifted/then/sink
     * bodies (decls verbatim + the trailing return re-shaped);
     * {@code BOUNDARY:block} — the corpus-zero last resort. Counter key
     * {@code VALUE:<topKind>:<form>} — the per-body-class honest-shape
     * receipt the plan § 4 mandates.
     */
    public void recordValueSeamEmitted(FlipFacts facts, String form) {
        seamsFlipped++;
        seamsByBodyForm.merge("VALUE:" + facts.valueBodyTopKind().name() + ":" + form,
                1, Integer::sum);
    }

    /** Distinct value seams emitted (the per-cell tranche membership count). */
    public int seamsFlipped() {
        return seamsFlipped;
    }

    /** The emitted seams' body-form breakdown ({@code SINGLE=n …}; {@code none} when zero). */
    public String seamsByBodyFormBreakdown() {
        if (seamsByBodyForm.isEmpty()) {
            return "none";
        }
        StringBuilder sb = new StringBuilder();
        seamsByBodyForm.forEach((k, v) -> {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(k).append('=').append(v);
        });
        return sb.toString();
    }

    /** Multi members refused for a missing/disagreeing boundary witness (expected 0). */
    public int witnessRefusals() {
        return witnessRefusals;
    }

    /**
     * The refused members behind {@link #witnessRefusals()}, one line each
     * ({@code function.alias seam=… element=… plan=…}) — the decode receipt the
     * emission gate prints beside the count.
     */
    public List<String> witnessRefusalMembers() {
        return List.copyOf(witnessRefusalMembers);
    }

    // ------------------------------------------------------------- analysis --

    private Map<String, FlipFacts> analysis(RFunction func) {
        Map<String, FlipFacts> cached = analysed.get(func);
        if (cached != null) {
            return cached;
        }
        Map<String, FlipFacts> computed = compute(func);
        analysed.put(func, computed);
        return computed;
    }

    /**
     * The per-alias unit facts the predicate reads. The T1–T3 eras also carried
     * both counting channels ({@code boundRefs}/{@code irRefs}) and the
     * disguised-head flag for the ¬D4-live refusal; T4 (PR-25) admits the
     * D4-live class on the map's oracle receipt, so the predicate keeps only
     * the legs it still reads — the counting channels live on in
     * {@code AliasDispositionMapTest} (the receipt authority the belt
     * arbitrates against).
     */
    private static final class AliasFacts {
        RShortcut shortcut;
        boolean usesOutput;
        boolean d1Direct;
        boolean excluded;      // D3/D1 seed ∪ D1T fixpoint
        boolean disguisedOnlyExists;
    }

    private Map<String, FlipFacts> compute(RFunction func) {
        Map<String, FlipFacts> result = new LinkedHashMap<>();
        if (func.shortcuts().isEmpty()) {
            return result;
        }

        // -- the structural walk (the render channel: the seam facts, PR #612) ----
        List<FunctionTemplateModel.AliasModel> structural = aliasHelper.analyze(func);
        List<RShortcut> shortcuts = func.shortcuts();
        if (structural.size() != shortcuts.size()) {
            return result; // mirrors compileAliases' own fallback — nothing flips
        }

        Map<RShortcut, AliasFacts> byShortcut = new IdentityHashMap<>();
        Map<String, AliasFacts> byName = new HashMap<>();
        for (int i = 0; i < shortcuts.size(); i++) {
            AliasFacts f = new AliasFacts();
            f.shortcut = shortcuts.get(i);
            f.usesOutput = structural.get(i).getUsesOutput();
            byShortcut.put(f.shortcut, f);
            byName.put(f.shortcut.name(), f);
        }

        // -- the unit walks (the D1 channel + the disguised-only-exists scan) --
        List<RShortcut[]> aliasEdges = new java.util.ArrayList<>();
        for (ROperation op : func.operations()) {
            boundScan(op.expression(), null, byShortcut, byName, aliasEdges);
        }
        // Each alias body adapts ONCE INTO THIS MAP (Copilot #558 R4's
        // suppressed note, mechanism-verified; the #561 R2 wording catch): the
        // predicate loop's witness/top-kind reads all share this one adapted
        // form — the classifier's classify() below performs its OWN internal
        // adapt for the ladder-plan walk, a separate read this map never
        // claimed to cover.
        Map<RShortcut, Optional<IRExpr>> adaptedBodies = new IdentityHashMap<>();
        for (RShortcut alias : shortcuts) {
            boundScan(alias.expression(), alias, byShortcut, byName, aliasEdges);
            adaptedBodies.put(alias, alias.expression() == null ? Optional.empty()
                    : adapter.adapt(alias.expression(), generatorModel.workspace()));
        }
        for (RCondition cond : func.conditions()) {
            boundScan(cond.expression(), null, byShortcut, byName, aliasEdges);
        }
        for (RPostCondition post : func.postConditions()) {
            boundScan(post.expression(), null, byShortcut, byName, aliasEdges);
        }

        // -- the D1 transitive closure (the map's fixpoint, mirrored) ----------
        for (AliasFacts f : byShortcut.values()) {
            f.excluded = f.usesOutput || f.d1Direct;
        }
        boolean grew = true;
        while (grew) {
            grew = false;
            for (RShortcut[] edge : aliasEdges) {
                AliasFacts from = byShortcut.get(edge[0]);
                AliasFacts to = byShortcut.get(edge[1]);
                if (from.excluded && !to.excluded) {
                    to.excluded = true;
                    grew = true;
                }
            }
        }

        // -- the per-member predicate ------------------------------------------
        // T4 (PR-25): a dispatch VARIANT's per-member facts thread the BASE
        // exactly where the reference render does (the renderEnclosingInputs /
        // buildStandardModel signatureSource law): the invocation args and the
        // escape belt walk the BASE's declared inputs; the dependency/operator
        // collision laws stay on the VARIANT (the emitted class's own surface).
        // Non-variant functions thread themselves (dispatchBaseOf null).
        RFunction dispatchBase = com.regnosys.rosetta.generator.java.expression.handlers
                .HandlerHelper.dispatchBaseOf(func);
        RFunction sig = dispatchBase != null ? dispatchBase : func;
        for (int i = 0; i < shortcuts.size(); i++) {
            RShortcut shortcut = shortcuts.get(i);
            AliasFacts f = byShortcut.get(shortcut);
            // PR #612 (the aliasSeamSignature retirement): the TYPED seam the analyze walk
            // recorded beside the rendered signature string (LAW 69 / LAW 77 — the optimised
            // route reads the same facts the shared generator does).
            FunctionTemplateModel.AliasSeam seam = structural.get(i).getSeam();
            if (f.excluded || f.disguisedOnlyExists) {
                continue;
            }
            // The RENDER-channel S/C bit — the seam's own recorded wrapper form (the
            // shipped truth, plan § 3). T4 (PR-25) makes it the SOLE cardinality
            // authority: a D5 row (the IR channel disagreeing) flips on THIS
            // bit, receipted per member in the map's d5 column + the O5p
            // reconciliation. A NON-Mapper seam (the usesOutput builder forms) needs no
            // decline of its own: none reached here over the census's 24,302 policy
            // arrivals (the f.excluded gate above removes every usesOutput alias, and no
            // other producer builds one — MEASURED, PR #612) and the
            // valueReturnTypeOrNull null below is the structural close if one ever does.
            boolean renderMulti = seam.isMapperMulti();
            if (shortcut.expression() == null) {
                continue;
            }
            NavigationChainClassifier.LadderPlan plan = classifier
                    .classify(shortcut.expression(), generatorModel.workspace(), func)
                    .orElse(null);
            Optional<IRExpr> ir = adaptedBodies.getOrDefault(shortcut, Optional.empty());
            // T3 (PR-24): the body-form leg is TOTAL over adapted bodies — a
            // non-whole-chain member admits under EVERY adapted TOP kind (the
            // map's assignTranche fall-through; since T4 the union T1∪T2∪T3∪T4
            // is exactly the ¬D population). An UN-ADAPTED body (the map's
            // adaptEmpty class — corpus-zero) refuses RIGHT HERE (the in-block
            // twin of the ir.isEmpty() refusal below); the map belt names any
            // future carrier loud (map tranche vs policy no-flip).
            IRExprKind valueBodyTopKind = null;
            if (plan == null) {
                valueBodyTopKind = ir.map(IRExpr::kind).orElse(null);
                if (valueBodyTopKind == null) {
                    continue; // adaptEmpty — never flips (the twin refusal)
                }
            }
            if (ir.isEmpty()) {
                continue; // adaptEmpty — the witness/top-kind reads need the form
            }
            String valueReturnType = valueReturnTypeOrNull(seam);
            if (valueReturnType == null) {
                continue;
            }
            JavaClass<?> witness = null;
            String elementText = null;
            if (renderMulti) {
                elementText = multiElementText(seam);
                witness = plan != null ? multiWitnessOrNull(plan, elementText)
                        : multiWitnessFromBodyTopOrNull(ir.get(), elementText);
                if (witness == null) {
                    witnessRefusals++;
                    // A refusal names its member (PR #607: the first band cell,
                    // cdm 6.20.2, fired this counter — a bare count cannot be decoded).
                    witnessRefusalMembers.add(func.name() + "." + shortcut.name()
                            + " seam=" + structural.get(i).getReturnType()
                            + " element=" + elementText
                            + " plan=" + (plan == null ? "none/top=" + ir.get().kind()
                                    : plan.shape() + "/" + plan.rootKind()));
                    continue;
                }
            }
            result.put(shortcut.name(),
                    new FlipFacts(renderMulti, witness, plan, valueReturnType, elementText,
                            invocationText(func, sig, shortcut.name()), valueBodyTopKind));
        }
        return result;
    }

    /**
     * The seat-invocation spelling (the FlipFacts record javadoc's laws): the
     * dep-collision numbering, then the static-operator {@code _} escape, then
     * the declaration-order ESCAPED input names (synthesized placeholder
     * filtered; {@code FunctionGenerator.escapedFunctionInputName} against
     * {@code func} — the emitted class's own dependency surface — exactly as
     * buildStandardModel spells the same inputs in the alias method's params).
     * Until PR #607 the arg names were the RAW input names and a separate
     * escape "belt" (Seat-1 #558 SF1) refused every member of a function with
     * an escaping input; the belt is retired with the disagreement it guarded.
     * {@code sig} is the signature-source function the arg names walk (the
     * dispatch BASE for a variant — T4's dispatch-base thread, mirroring
     * {@code ReferenceHandler.renderEnclosingInputs}' own
     * {@code dispatchBaseOf} consult; {@code func} itself otherwise); the
     * collision/operator laws read {@code func} — the variant's own rendered
     * surface, exactly as {@code disambiguateAliasInvocation} reads the
     * enclosing function.
     */
    private static String invocationText(RFunction func, RFunction sig, String aliasName) {
        String name = aliasName;
        if (com.regnosys.rosetta.generator.java.function.FunctionDependencyCollector
                .hasFunctionDependencyNamed(func, name)) {
            name = name + "1";
        }
        if (com.regnosys.rosetta.generator.java.expression.handlers.HandlerHelper
                .staticOperatorMembersUsed(func).contains(name)) {
            name = "_" + name;
        }
        StringBuilder sb = new StringBuilder(name).append('(');
        boolean first = true;
        for (com.regnosys.rosetta.ast.supporting.RAttribute input : sig.inputs()) {
            if (com.regnosys.rosetta.generator.java.function.FunctionGenerator
                    .isSynthesizedInput(input)) {
                continue;
            }
            if (!first) {
                sb.append(", ");
            }
            // THE ONE INPUT-SPELLING AUTHORITY (PR #607): the escaped Java name —
            // the same law the alias method's own parameter list (the reference
            // AliasModel's paramsDecl, reused verbatim by the value form) and the
            // reference bridge's renderEnclosingInputs spell. drr 7.x's
            // CounterpartyRoleFromLEI carries an input `partyLei` colliding with
            // its dependency field `partyLei` (the PartyLei function): the golden
            // spells `_partyLei` at every seat — params, invocations, the ladder
            // root — and a raw `partyLei` here would have handed the @Inject'd
            // PartyLei field to a String parameter at the alias-root ladder. The
            // pre-#607 escape "belt" refused such members outright instead
            // (invisible on the ring: no ring member's input escapes).
            sb.append(com.regnosys.rosetta.generator.java.function.FunctionGenerator
                    .escapedFunctionInputName(func, input.name(), func.name()));
            first = false;
        }
        return sb.append(')').toString();
    }

    /**
     * The bound (AST) channel + the disguised-only-exists scan, one pre-order
     * walk — the D1 flags mirror the map's {@code boundWalk} (the
     * symbol-resolved channel; the only-exists rule is the census classifier's
     * ANCESTOR check), the disguised scan flags unresolved-enumeration
     * {@link REnumValueRef} heads naming an alias INSIDE an only-exists element
     * (the § 19c channel-invisible spelling at the one seat class that must
     * refuse regardless of counts — the safety belt; the counting-channel
     * reads themselves retired with the T4 D4-live admission, PR-25).
     */
    private void boundScan(RNode node, RShortcut enclosingAlias,
            Map<RShortcut, AliasFacts> byShortcut, Map<String, AliasFacts> byName,
            List<RShortcut[]> aliasEdges) {
        if (node == null) {
            return;
        }
        if (node instanceof RSymbolReference ref) {
            RShortcut target = ref.symbol().filter(RShortcut.class::isInstance)
                    .map(RShortcut.class::cast).orElse(null);
            AliasFacts f = target == null ? null : byShortcut.get(target);
            if (f != null) {
                if (underOnlyExistsElement(ref)) {
                    f.d1Direct = true;
                }
                if (enclosingAlias != null) {
                    aliasEdges.add(new RShortcut[] {enclosingAlias, target});
                }
            }
        }
        if (node instanceof REnumValueRef evr && evr.enumeration().isEmpty()) {
            AliasFacts f = evr.enumName() == null ? null : byName.get(evr.enumName());
            if (f != null && underOnlyExistsElement(evr)) {
                f.disguisedOnlyExists = true;
            }
        }
        for (RNode child : node.children()) {
            boundScan(child, enclosingAlias, byShortcut, byName, aliasEdges);
        }
    }

    /** The ONE enclosure read (LAW 69, v3.2 seat 8): {@link ROnlyExistsElement#encloses}. */
    private static boolean underOnlyExistsElement(RNode node) {
        return ROnlyExistsElement.encloses(node);
    }

    // ------------------------------------------------------- the seam strings --

    /**
     * The value-typed seam string from the Mapper seam — the RENDER channel is the S/C
     * and element authority (plan § 3), and since the aliasSeamSignature retirement (#612) the
     * transform reads the FACTS {@code FunctionAliasHelper} recorded beside that text
     * rather than re-parsing it: a MAPPER_SINGLE seam → its element;
     * a MAPPER_MULTI seam → {@code List<}[{@code ? extends }]{@code element>}
     * (the wildcard bit carries over verbatim). A collision-sentinel element rides
     * VERBATIM (the {@code typedEmptyElseOrNull} ride-along precedent). Null for any
     * other seam form, and for a Mapper seam with NO element (the legacy
     * {@code MapperS<?>} fallback) — never flips.
     */
    static String valueReturnTypeOrNull(FunctionTemplateModel.AliasSeam seam) {
        if (seam.element() == null) {
            return null;
        }
        if (seam.isMapperSingle()) {
            return seam.element();
        }
        if (seam.isMapperMulti()) {
            return "List<" + (seam.wildcarded() ? "? extends " : "") + seam.element() + ">";
        }
        return null;
    }

    /** The multi seam's element (the {@code ? extends} never joined it) — the boundary witness spelling. */
    static String multiElementText(FunctionTemplateModel.AliasSeam seam) {
        return seam.element();
    }

    /**
     * The multi boundary witness class — the chain TOP hop's IR type through the
     * SHARED translator (the § 9a clause-4 fact source), the meta-top wrapper via
     * the same {@link RJavaWithMetaValue} factory the boundary witness rides.
     * Null (refuse) when the top type is missing or the translated simple name
     * disagrees with the seam string's element (the same-walk law, belted) —
     * both classes measured ZERO on the priced pool; the map belt fires loud if
     * a T1 row ever lands here.
     */
    private JavaClass<?> multiWitnessOrNull(NavigationChainClassifier.LadderPlan plan,
            String elementText) {
        if (plan.topType() == null || plan.topType().isMissing()) {
            return null;
        }
        JavaClass<?> witness;
        if (plan.topMeta() != NavigationChainClassifier.LadderPlan.TopMeta.NONE) {
            // An unmappable VALUE type refuses (null) rather than flowing into
            // the wrapper factory (Copilot #559 R2's suppressed NPE note,
            // mechanism-verified: create() would construct over the null and
            // fail downstream instead of declining toward NO-FLIP + the belt).
            var metaValue = typeTranslator.toJavaReferenceType(plan.topType().type());
            if (metaValue == null) {
                return null;
            }
            witness = RJavaWithMetaValue.create(
                    plan.topMeta() == NavigationChainClassifier.LadderPlan.TopMeta.REF,
                    metaValue, typeUtil);
        } else {
            witness = typeTranslator.toJavaReferenceType(plan.topType().type());
        }
        return witness != null && witness.getSimpleName().equals(bareSimpleName(elementText))
                ? witness : null;
    }

    /**
     * The T2 twin of {@link #multiWitnessOrNull} (PR-23): a VALUE-BODY member
     * has no ladder plan, so the boundary witness derives from the adapted
     * body's TOP node — {@link IRExpr#type()} through the SAME shared
     * translator, the meta-topped wrapper via the SAME
     * {@link NavigationChainClassifier#metaKindOf} discriminant +
     * {@link RJavaWithMetaValue} factory the classifier's § 10a boundary
     * witness rides (the type() contract: a meta node's type() is the VALUE
     * type; the witness wraps it per the qualifier kind). Null (refuse) on a
     * missing type, an unmappable qualifier set, or a simple-name
     * disagreement with the seam string's element (the same-walk law) — every
     * class corpus-zero on the T2 pool (the map belt fires loud otherwise).
     *
     * <p>T3 (PR-24): a NON-meta-node top whose TYPE carries meta qualifiers
     * (a meta-annotated ELEMENT under a LAMBDA_OP/LIST_OP/… top) derives the
     * wrapper witness from the TYPE-level qualifier channel
     * ({@code RMetaAnnotatedType.metaAttributes()} — the same channel the
     * classifier's ITEM-root arm reads) through the SAME {@code metaKindOf}
     * discriminant. And where the IR channel is BLIND to the element's meta
     * entirely (the inference types a lambda result at the bare VALUE — e.g.
     * a {@code mapItem} whose items render {@code FieldWithMetaString} while
     * {@code type()} says bare {@code string}, the MapLegalEntity class), the
     * witness RECONSTRUCTS: wrap the translated IR VALUE class through the
     * SAME {@link RJavaWithMetaValue} factory (FIELD and REF candidates) and
     * accept exactly the candidate whose simple name equals the seam string's
     * element — the same-walk agreement at the VALUE grain (the two wrapper
     * spellings are disjoint, so the seam element picks the kind; a
     * no-candidate member still refuses and the map belt names it).
     */
    private JavaClass<?> multiWitnessFromBodyTopOrNull(IRExpr top, String elementText) {
        if (top.type() == null || top.type().isMissing()) {
            return null;
        }
        JavaClass<?> witness;
        NavigationChainClassifier.LadderPlan.TopMeta kind =
                NavigationChainClassifier.LadderPlan.TopMeta.NONE;
        if (top instanceof IRMetaAccess metaTop) {
            kind = NavigationChainClassifier.metaKindOf(metaTop.metaQualifiers());
            if (kind == NavigationChainClassifier.LadderPlan.TopMeta.NONE) {
                return null;
            }
        } else if (top.type().hasMeta()) {
            kind = NavigationChainClassifier.metaKindOf(top.type().metaAttributes());
        }
        String bare = bareSimpleName(elementText);
        if (kind != NavigationChainClassifier.LadderPlan.TopMeta.NONE) {
            // An unmappable VALUE type refuses (null) rather than flowing into
            // the wrapper factory (Copilot #559 R2's suppressed NPE note,
            // mechanism-verified; the T1 twin above hardened identically —
            // the class-sweep law).
            var metaValue = typeTranslator.toJavaReferenceType(top.type().type());
            if (metaValue == null) {
                return null;
            }
            witness = RJavaWithMetaValue.create(
                    kind == NavigationChainClassifier.LadderPlan.TopMeta.REF,
                    metaValue, typeUtil);
        } else {
            witness = typeTranslator.toJavaReferenceType(top.type().type());
            if (witness != null && !witness.getSimpleName().equals(bare)) {
                // The IR-blind meta element (T3, PR-24): reconstruct the seam's
                // wrapper over the IR VALUE class; accept only an EXACT
                // seam-element name match (the constructive same-walk law).
                JavaClass<?> field = RJavaWithMetaValue.create(false, witness, typeUtil);
                JavaClass<?> ref = RJavaWithMetaValue.create(true, witness, typeUtil);
                witness = field != null && field.getSimpleName().equals(bare) ? field
                        : ref != null && ref.getSimpleName().equals(bare) ? ref
                        : witness;
            }
        }
        return witness != null && witness.getSimpleName().equals(bare)
                ? witness : null;
    }

    /**
     * The element's bare simple name: an import-collision sentinel
     * ({@code ImportCollisionResolver.typeRef} — the seam string's spelling of an
     * element whose simple name collides with another import of the file, e.g.
     * cdm 6.20.2–6.20.5's {@code MapCreditEventsReferenceWithReference.creditEvents},
     * {@code MapperC<? extends ⟨fpml.consolidated.option.shared.CreditEvents⟩>}
     * beside the CDM {@code CreditEvents} output) is stripped to its canonical
     * name by the resolver's OWN neutraliser first, then the last segment is taken.
     * Before PR #607 the trailing delimiter rode into the segment
     * ({@code CreditEvents⟩}), the same-walk comparison could never agree, and the
     * member refused — invisible on the ring (cdm 6.20.6's twin takes {@code first}
     * and renders SINGLE, so no multi witness is ever derived there).
     */
    private static String bareSimpleName(String elementText) {
        String canonical = com.regnosys.rosetta.generator.java.template.ImportCollisionResolver
                .stripToBare(elementText);
        int lastDot = canonical.lastIndexOf('.');
        return lastDot < 0 ? canonical : canonical.substring(lastDot + 1);
    }
}
