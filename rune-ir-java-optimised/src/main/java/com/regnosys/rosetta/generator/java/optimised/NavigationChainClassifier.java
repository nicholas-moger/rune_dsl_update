package com.regnosys.rosetta.generator.java.optimised;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.expressions.references.RDeepFeatureCall;
import com.regnosys.rosetta.ast.expressions.references.REnumValueRef;
import com.regnosys.rosetta.ast.expressions.binary.RThenExpr;
import com.regnosys.rosetta.ast.expressions.references.RFeatureCall;
import com.regnosys.rosetta.ast.expressions.references.RImplicitVariable;
import com.regnosys.rosetta.ast.expressions.supporting.RInlineFunction;
import com.regnosys.rosetta.ast.expressions.supporting.ROnlyExistsElement;
import com.regnosys.rosetta.ast.expressions.unary.RConversionExpr;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.generator.java.JavaNamingUtil;
import com.regnosys.rosetta.generator.java.expression.handlers.HandlerHelper;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.types.JavaPojoProperty;
import com.regnosys.rosetta.ir.expr.FieldAccess;
import com.regnosys.rosetta.ir.expr.IRExpr;
import com.regnosys.rosetta.ir.expr.IRMetaAccess;
import com.regnosys.rosetta.ir.expr.IRReference;
import com.regnosys.rosetta.ir.expr.IRVariable;
import com.regnosys.rosetta.ir.expr.adapter.ExpressionToIRAdapter;
import com.regnosys.rosetta.symbols.RWorkspace;
import com.regnosys.rosetta.types.ExpressionCardinality;
import com.regnosys.rosetta.types.RMetaAnnotatedType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * THE NAVIGATION-FAMILY ELIGIBILITY CLASSIFIER (tranches 1 + 2 + the family-2 meta
 * widening) — the single predicate implementation shared by the emitter
 * ({@link OptimisedExpressionCompiler}) and the census instrument
 * ({@code NavigationChainCensusTest}), so the priced scope and the converted scope
 * can never drift apart on the predicate itself (the reconciliation gate then checks
 * TRAVERSAL completeness: the render path must meet every AST site the census walk
 * meets, exactly once).
 *
 * <p><b>The predicate</b> (the census `research/p3-navigation-family-census.md` § 4 +
 * § 9 + § 10): the node is the MAXIMAL top of a navigation chain that adapts to pure
 * {@code FieldAccess}/{@link IRMetaAccess} hops over an {@code IRVariable(PARAM)}
 * root naming an input of the enclosing function. Tranche 1 (PR #539) admitted only
 * all-SINGLE {@code FieldAccess} steps over a SINGLE root; tranche 2 (PR-5) WIDENS
 * the same predicate to multi-cardinality steps and multi-cardinality roots — the
 * {@link LadderPlan} carries the per-hop cardinality facts and the derived
 * {@link LadderPlan.Shape} so consumers pick the emission form from ONE decode;
 * family 2 (PR-6) admits META hops ({@code [metadata …]}-annotated features — the
 * {@code FieldWithMetaX}/{@code ReferenceWithMetaX} wrapper families): an INTERIOR
 * meta hop contributes its wrapper getter PLUS the {@code getValue} deref the
 * reference render's own interior coercion emits
 * ({@code ExpressionCompiler.coerceNavigationReceiver} — the
 * {@code map("Type coercion", x -> … x.getValue())} step), and a TOP meta hop
 * contributes ONLY its wrapper getter — the chain's value IS the wrapper there, and
 * any terminal coercion is appended by the CONSUMER side onto the harvested
 * reference type, riding the boundary wrap unchanged. The IR is the NORMALIZER: the
 * parser spells multi-hop chains in several AST forms (nested {@link RFeatureCall}
 * spines, linker-bound {@link REnumValueRef} qualified chains, mixes), and the
 * adapter resolves them all to the same {@code FieldAccess}/{@code IRMetaAccess}
 * spine — classifying on the IR covers every spelling with one check (the probe that
 * forced this design: cdm5's multi-hop param chains arrive overwhelmingly as
 * {@code REnumValueRef}).
 *
 * <p><b>The meta-qualifier vocabulary lock.</b> The wrapper KIND mirrors the render's
 * own discriminant ({@code MetaFieldGenerator.detectMetaKind}): {@code reference}/
 * {@code address} → {@code ReferenceWithMetaX} ({@link LadderPlan.TopMeta#REF});
 * else {@code scheme}/{@code id}/{@code location} → {@code FieldWithMetaX}
 * ({@link LadderPlan.TopMeta#FIELD}). A hop whose qualifier set maps to NEITHER kind
 * — a bare qualifier-less {@code [metadata]} (which the render treats as a PLAIN
 * getter: {@code detectMetaKind} = NONE, so no wrapper and no coercion) or any
 * unknown token — REFUSES the whole chain conservatively: the bare class is
 * unwitnessed on the priced pool and the unknown class is a render family this
 * classifier has not proven.
 *
 * <p><b>The plan facts:</b> the escaped root name reuses
 * {@link FunctionGenerator#escapedFunctionInputName} (the signature/body single
 * source); each accessor is {@code get} + FirstUpper(feature) through
 * {@link JavaPojoProperty#escapeOperationName} (the inherited-collision escape —
 * golden {@code telephoneNumber._getType()}).
 */
public final class NavigationChainClassifier {

    private final ExpressionToIRAdapter adapter = new ExpressionToIRAdapter();

    /**
     * One eligible chain: the escaped root name + the escaped accessor sequence
     * (root→top) + the cardinality facts (tranche 2): whether the PARAM root is a
     * list input and, per ACCESSOR STEP, whether the step is a MULTI feature
     * ({@code hopMulti} aligns index-for-index with {@code accessors}), plus the TOP
     * hop's IR type ({@code topType} — the chain's ELEMENT type: the plain feature
     * type for a plain top, the meta hop's VALUE type for a meta top — possibly
     * MISSING there, the #497 fragmented-typing census). The multi boundary witness
     * ({@code MapperC.<X>of}) translates it through the SHARED
     * {@code JavaTypeTranslator} — the same fact source the reference render's own
     * {@code <X>mapC}/{@code <X>map} witnesses come from, and a fact measured
     * COMPLETE on the tranche-1/2 pool (census § 2: hopTypeMissing onPure = 0 on
     * every FUNCTION face; the META pool's completeness is census § 10's own
     * measurement).
     *
     * <p><b>The family-2 meta facts.</b> {@code accessors} BAKES interior meta
     * derefs: an interior meta hop contributes its wrapper getter AND a
     * {@code getValue} step (both SINGLE unless the getter step itself is MULTI), so
     * {@code accessors.size()} is the LADDER length (getter count), not the DSL hop
     * count. {@code topMeta} names the TOP hop's wrapper kind ({@code NONE} for a
     * plain top — a plan can still bear meta through interior hops);
     * {@code fieldMetaHops}/{@code refMetaHops} count the admitted meta hops by
     * wrapper kind (top included — the census § 10 analytics split).
     *
     * <p><b>The tranche-3 loop-form facts (census § 11).</b> {@code rootType} is the
     * PARAM root's DECLARATION-channel type — {@code ws.getInferredAttributeType}
     * on the owner's input attribute, the same fact the generated signature is
     * typed from, with the adapter's node-channel {@code IRVariable.type()} as the
     * fallback (that channel is MISSING on a measured slice of bare param
     * references — the § 11 shortfall forensics); it is the loop helper's
     * parameter spelling. {@code stepTypes}/{@code stepMetaKinds} align
     * index-for-index with
     * {@code accessors} and carry each step's IR VALUE type + wrapper kind
     * ({@code NONE} for plain getter steps AND baked {@code getValue} deref steps —
     * a deref RESULT is the value; {@code FIELD}/{@code REF} on a meta GETTER step,
     * whose runtime element is the WRAPPER class derived from kind + value type).
     * The loop emission spells element types ONLY at multi seats (the root and the
     * multi steps' loop locals) — every fact read from the SAME IR channels the § 5a
     * boundary witness rides.
     *
     * <p><b>The family-3 root facts (census § 12 — the rule-face census stage).</b>
     * {@code rootKind} names the admitted root class: {@code PARAM} (a function-input
     * root — every landed family) or {@code ITEM} (a {@code SYNTHETIC_ITEM} root —
     * the implicit item under extract/filter/then stages, the drr RULE face's
     * dominant root and a minority FUNCTION-face root). ITEM plans are
     * CENSUS-STAGE ONLY: the emitter declines every one behind its pending pins
     * (no proven emission form yet — the § 12 price decides). For an ITEM root,
     * {@code rootName} is the IR-level item name (informational — an emission
     * form must derive the seat's Java spelling from the render, never from
     * here), {@code rootType} rides the node channel only (no declaration
     * exists for an implicit item; possibly MISSING — a measured § 12 gap), and
     * {@code rootMeta} names the wrapper kind of a wrapper-TYPED item root (the
     * reference render derefs such a root before navigating — the leading
     * {@code getValue} class; {@code NONE} for plain roots and every PARAM
     * plan, whose meta-annotated roots refuse outright).
     */
    public record LadderPlan(String rootName, List<String> accessors, boolean rootMulti,
            List<Boolean> hopMulti, RMetaAnnotatedType topType, TopMeta topMeta,
            int fieldMetaHops, int refMetaHops, RMetaAnnotatedType rootType,
            List<RMetaAnnotatedType> stepTypes, List<TopMeta> stepMetaKinds,
            RootKind rootKind, TopMeta rootMeta) {

        /** The TOP hop's wrapper kind — {@code NONE} when the top is a plain hop. */
        public enum TopMeta { NONE, FIELD, REF }

        /**
         * The admitted root class (census § 12; {@code ALIAS_CALL} = the § 6.3
         * T1 extension): {@code PARAM} / {@code ITEM} — the landed families;
         * {@code ALIAS_CALL} — a chain rooted at a VALUE-TYPED alias
         * invocation (an {@code IRReference&#123;ALIAS&#125;} residue admitted
         * ONLY through the opt-in overload's {@link AliasRootAdmission} — the
         * 3-arg {@code classify} the census/map instruments call refuses it
         * unchanged, so every standing census row stays byte-comparable). For
         * an {@code ALIAS_CALL} root, {@code rootName} is the ALIAS NAME
         * (informational — the emitter derives the seat's invocation spelling
         * from its own reference render, the § 12b.3 harvest law), and
         * {@code rootMulti} is the SEAM's S/C bit supplied by the admission
         * (the flip policy's render-channel authority).
         */
        public enum RootKind { PARAM, ITEM, ALIAS_CALL }

        public LadderPlan {
            accessors = List.copyOf(accessors);
            hopMulti = List.copyOf(hopMulti);
            stepTypes = List.copyOf(stepTypes);
            stepMetaKinds = List.copyOf(stepMetaKinds);
            if (hopMulti.size() != accessors.size()) {
                throw new IllegalArgumentException("hopMulti must align with accessors: "
                        + hopMulti.size() + " != " + accessors.size());
            }
            if (stepTypes.size() != accessors.size()
                    || stepMetaKinds.size() != accessors.size()) {
                throw new IllegalArgumentException(
                        "stepTypes/stepMetaKinds must align with accessors: "
                                + stepTypes.size() + "/" + stepMetaKinds.size()
                                + " != " + accessors.size());
            }
            if (topMeta != TopMeta.NONE && fieldMetaHops + refMetaHops == 0) {
                throw new IllegalArgumentException(
                        "a meta top must be counted in the per-kind meta-hop tallies");
            }
            if (rootMeta != TopMeta.NONE && rootKind != RootKind.ITEM) {
                throw new IllegalArgumentException(
                        "a wrapper-typed root is an ITEM-only admission — PARAM plans"
                                + " refuse meta-annotated roots outright");
            }
        }

        /** True when any admitted hop is a meta (wrapper-family) hop — the family-2 pool. */
        public boolean bearsMeta() {
            return fieldMetaHops + refMetaHops > 0;
        }

        /**
         * The emission-form decode (census § 9): {@code SINGLE} = tranche 1 (all
         * steps + root SINGLE — the {@code MapperS} ladder); {@code TERMINAL_MULTI} =
         * a SINGLE root whose ONLY multi point is the last hop (the same ladder, list
         * boundary — {@code MapperC}); {@code STREAM} = every other multi shape (a
         * multi root and/or an interior multi hop — the value-semantics stream
         * pipeline, {@code MapperC} boundary).
         */
        public enum Shape { SINGLE, TERMINAL_MULTI, STREAM }

        /** True when no hop and not the root is multi-cardinality (the tranche-1 pool). */
        public boolean allSingle() {
            return !rootMulti && !hopMulti.contains(true);
        }

        /** The number of multi-cardinality points (the root counts as one when multi). */
        public int multiPoints() {
            int points = rootMulti ? 1 : 0;
            for (Boolean multi : hopMulti) {
                if (multi) {
                    points++;
                }
            }
            return points;
        }

        public Shape shape() {
            if (allSingle()) {
                return Shape.SINGLE;
            }
            if (!rootMulti && multiPoints() == 1 && hopMulti.get(hopMulti.size() - 1)) {
                return Shape.TERMINAL_MULTI;
            }
            return Shape.STREAM;
        }

        /**
         * The null-guarded direct ladder ({@code p == null ? null : p.getA() == null ?
         * null : p.getA().getB()}) — nested ternaries right-associate, so no interior
         * parens are needed; callers parenthesize the whole ladder once for
         * position-independence after a structural unwrap. For {@code TERMINAL_MULTI}
         * the same ladder applies unchanged — the last accessor returns the List the
         * boundary {@code MapperC.of} wraps.
         */
        public String ladderCode() {
            return ladder(rootName, accessors);
        }

        /**
         * The null-guarded ladder over a SLICE of the accessor sequence, rooted at an
         * arbitrary variable — the loop-form helper's mini-ladder channel (census
         * § 11: each stage of the {@code STREAM}-shape loop emission is a
         * consecutive-single run rendered as one ladder over the stage's element
         * local). An EMPTY slice renders the variable itself (the
         * flattened-element-is-the-value stage). The § 9a stream pipeline this
         * replaces was the census § 9d measured decline — its design of record
         * stays in the census; no live path renders stream machinery.
         *
         * @param rootVar the stage's element variable
         * @param fromInclusive the first accessor index of the run
         * @param toExclusive one past the last accessor index of the run
         */
        public String ladderOver(String rootVar, int fromInclusive, int toExclusive) {
            return ladder(rootVar, accessors.subList(fromInclusive, toExclusive));
        }

        private static String ladder(String rootVar, List<String> accessorRun) {
            StringBuilder sb = new StringBuilder();
            String prefix = rootVar;
            for (String accessor : accessorRun) {
                sb.append(prefix).append(" == null ? null : ");
                prefix = prefix + "." + accessor + "()";
            }
            return sb.append(prefix).toString();
        }
    }

    /**
     * True when this node can be a chain TOP by AST kind ({@link RFeatureCall} or
     * {@link REnumValueRef} — the two arrival spellings) and no parent navigates off
     * it (a parent feature/deep call consuming it as receiver makes it an interior).
     *
     * <p><b>The onlyExists path guard (load-bearing).</b> A chain that sits ANYWHERE
     * inside an {@code only exists} path element ({@link ROnlyExistsElement#encloses}
     * — the ONE declaration, LAW 69) is NEVER a candidate. The exclusion is a CONTRACT
     * exclusion: the deprecated list form of {@code ExpressionOperators.onlyExists}
     * consumes the mappers' PARENT-ITEM and PATH structure semantically (which sibling
     * attributes are set is derived from them), so there the Mapper path metadata is
     * behaviour, not message text, and a ladder rewrite would change results; the
     * 3-arg form every generator renders today reads {@code getMulti} alone, so the
     * exclusion is VALUE-identical today (the ten drr cells that carry the 34 twins the
     * pre-seat parent test leaked are in the pair gate's population, which ran at zero
     * divergence at #628 — a per-CELL receipt, {@code s7e.status:154-163}; no per-site
     * execution receipt exists) and load-bearing the moment a consumer renders the list
     * form — it is kept on the contract, not on today's render. The reference render
     * routes these elements through its own onlyExists machinery rather than visitor
     * dispatch, but the classifier refuses
     * them REGARDLESS (defense in depth — never rely on routing for a semantic
     * exclusion). Until v3.2 seat 8 this read the chain's PARENT alone — the receiver
     * chain was the element's direct child; the seat's leaf node ({@code t -> p} as a
     * real {@link RFeatureCall}) put a call between them, and the generator's
     * synthesized item-rooted receiver twin (parented where the root reference sits,
     * under the leaf now) stopped matching: eight cdm 5.38.0 receivers became SINGLE
     * item candidates and the conversion-event pin caught it (782 → 790 on
     * {@code cdm/5.38.0}, the FIRST pin to fail; re-taken with a preserved print at the
     * round-1 head under the parent test by text —
     * {@code target/v32-seat8-instruments/scratch/plus8-r1.log}, local — which measured
     * the class's reach too: 94 twins under the leaf over 20 of the 21 cells, every cell
     * but chaos, beside the 34 root twins). The walk reads the structure at any depth
     * (bounded at 64 like the IR adapter's ancestor walks).
     */
    public static boolean isMaximalChainTopCandidate(RExpression expr) {
        if (!(expr instanceof RFeatureCall) && !(expr instanceof REnumValueRef)) {
            return false;
        }
        if (ROnlyExistsElement.encloses(expr)) {
            return false;
        }
        RNode parent = expr.parent();
        if (parent instanceof RConversionExpr) {
            // The conversion consumers (to-enum / to-string / to-number / temporal)
            // read the RECEIVER's rendered shape to make render decisions — the
            // to-enum target's import-vs-FQN-inline collision choice keyed off the
            // reference chain's witness text, so a witness-less ladder flipped a
            // collision-suppressed import back on and produced a duplicate
            // single-type import (the drr gate catch). Chains feeding a conversion
            // stay reference-emitted; the conversion family owns them later.
            return false;
        }
        if (parent instanceof RFeatureCall fc && fc.receiver() == expr) {
            return false;
        }
        return !(parent instanceof RDeepFeatureCall dfc && dfc.receiver() == expr);
    }

    /**
     * THE § 6.3 T1 ALIAS-ROOT ADMISSION — the opt-in channel for chains rooted
     * at a VALUE-TYPED alias invocation: non-null facts admit an
     * {@code IRReference&#123;ALIAS&#125;} chain residue as an
     * {@link LadderPlan.RootKind#ALIAS_CALL} root. ONLY the emitter's
     * conversion seat supplies one (window-gated, policy-backed); the census
     * and map instruments call the 3-arg {@link #classify} whose admission is
     * null, so their walk-visible populations — and every standing NAVCENSUS /
     * ALIASMAP row — stay byte-comparable by construction.
     */
    @FunctionalInterface
    public interface AliasRootAdmission {
        /** The seam facts for {@code aliasName} of the owner, or null to refuse. */
        AliasRootFacts admitOrNull(String aliasName);
    }

    /** The admitted alias seam's facts: the render-channel S/C bit. */
    public record AliasRootFacts(boolean seamMulti) {
    }

    /**
     * Classifies a maximal-top candidate; {@link Optional#empty()} unless the WHOLE
     * predicate holds. {@code owner} may be null (no enclosing function — e.g. a rule
     * body walked without the synthetic-owner bridge): a PARAM root then has no input
     * declaration to resolve against and refuses; an ITEM root needs no owner (census
     * § 12 — the emitter declines every ITEM plan behind its pending pins until the
     * family-3 price lands, so the admission is census-visible and emission-inert).
     */
    public Optional<LadderPlan> classify(RExpression expr, RWorkspace workspace, RFunction owner) {
        return classify(expr, workspace, owner, null);
    }

    /**
     * The 4-arg overload with the § 6.3 {@link AliasRootAdmission} channel —
     * see {@link #classify(RExpression, RWorkspace, RFunction)} (whose behaviour
     * this reproduces EXACTLY when {@code aliasRootAdmission} is null).
     */
    public Optional<LadderPlan> classify(RExpression expr, RWorkspace workspace, RFunction owner,
            AliasRootAdmission aliasRootAdmission) {
        if (!isMaximalChainTopCandidate(expr)) {
            return Optional.empty();
        }
        Optional<IRExpr> adapted = adapter.adapt(expr, workspace);
        if (adapted.isEmpty()) {
            return Optional.empty();
        }
        IRExpr ir = adapted.get();
        RMetaAnnotatedType topType = null;
        LadderPlan.TopMeta topMeta = LadderPlan.TopMeta.NONE;
        int fieldMetaHops = 0;
        int refMetaHops = 0;
        // Top-down step lists (exact REVERSE of execution order): a feature name, or
        // null for an interior meta hop's baked getValue deref; per-step VALUE types
        // + wrapper kinds ride in parallel (the § 11 loop-form spelling facts).
        List<String> featuresTopDown = new ArrayList<>();
        List<Boolean> multiTopDown = new ArrayList<>();
        List<RMetaAnnotatedType> typesTopDown = new ArrayList<>();
        List<LadderPlan.TopMeta> kindsTopDown = new ArrayList<>();
        while (true) {
            if (ir instanceof FieldAccess hop) {
                if (topType == null) {
                    topType = hop.type();
                }
                if (hop.type().hasMeta()) {
                    // A meta-annotated type on a PLAIN FieldAccess hop is the
                    // twin-respell hazard class (the render synthesizes value-unwrap
                    // twins whose spellings re-type wrapper values — a
                    // wrapper-signature input's synthesized twin inferred
                    // MapperS<Object> and broke the cdm6 compile). Real meta hops
                    // arrive as the DISTINCT IRMetaAccess kind (the #499 adapter
                    // arm); this belt keeps the ambiguous FieldAccess spelling on
                    // the reference emission.
                    return Optional.empty();
                }
                featuresTopDown.add(hop.feature());
                multiTopDown.add(hop.featureCardinality() == ExpressionCardinality.MULTI);
                typesTopDown.add(hop.type());
                kindsTopDown.add(LadderPlan.TopMeta.NONE);
                ir = hop.receiver();
            } else if (ir instanceof IRMetaAccess metaHop) {
                // The family-2 admission (census § 10): the wrapper kind mirrors the
                // render's own discriminant (the class javadoc's vocabulary lock);
                // bare/unknown qualifier sets refuse the WHOLE chain.
                LadderPlan.TopMeta kind = metaKindOf(metaHop.metaQualifiers());
                if (kind == LadderPlan.TopMeta.NONE) {
                    return Optional.empty();
                }
                if (kind == LadderPlan.TopMeta.REF) {
                    refMetaHops++;
                } else {
                    fieldMetaHops++;
                }
                boolean top = featuresTopDown.isEmpty();
                if (top) {
                    // The chain's value IS the wrapper at the top — the terminal
                    // coercion (if the consumer wants the value) is appended by the
                    // CONSUMER side onto the harvested reference type, riding the
                    // boundary wrap unchanged. topType records the meta hop's VALUE
                    // type (possibly MISSING — the #497 fragmented-typing census);
                    // the multi boundary witness derives the WRAPPER from it plus
                    // this kind.
                    topType = metaHop.type();
                    topMeta = kind;
                } else {
                    // INTERIOR: the reference render derefs the wrapper before
                    // navigating further (coerceNavigationReceiver's
                    // `map("Type coercion", x -> … x.getValue())`) — the ladder
                    // bakes the SAME deref as a getValue step. Top-down (reverse
                    // execution) order: the deref precedes the getter. The deref
                    // RESULT is the value — step kind NONE.
                    featuresTopDown.add(null);
                    multiTopDown.add(false);
                    typesTopDown.add(metaHop.type());
                    kindsTopDown.add(LadderPlan.TopMeta.NONE);
                }
                featuresTopDown.add(metaHop.feature());
                multiTopDown.add(metaHop.featureCardinality() == ExpressionCardinality.MULTI);
                // The GETTER step's runtime element is the WRAPPER class — carried
                // as VALUE type + kind; the § 11 spelling derives wrapper(kind,
                // value) through the same RJavaWithMetaValue factory as the § 10a
                // boundary witness.
                typesTopDown.add(metaHop.type());
                kindsTopDown.add(kind);
                ir = metaHop.receiver();
            } else {
                break;
            }
        }
        if (featuresTopDown.isEmpty()) {
            return Optional.empty();
        }
        LadderPlan.RootKind rootKind;
        LadderPlan.TopMeta rootMeta = LadderPlan.TopMeta.NONE;
        RMetaAnnotatedType rootType;
        String rootName;
        boolean rootMulti;
        IRVariable root = ir instanceof IRVariable v ? v : null;
        if (root == null) {
            // THE § 6.3 ALIAS_CALL RESIDUE (T1): a chain whose root is an
            // IRReference{ALIAS} — admitted ONLY through the opt-in channel
            // (the emitter's window-gated, policy-backed admission for
            // VALUE-TYPED aliases); a null admission (the census/map
            // instruments, every pre-T1 caller) refuses exactly as before.
            // The seam's S/C bit is the admission's fact (the render-channel
            // authority — the flip policy already proved render ≡ IR on every
            // flipped member, the ¬D5 leg).
            if (aliasRootAdmission == null || !(ir instanceof IRReference aliasRef)
                    || aliasRef.referenceKind() != IRReference.ReferenceKind.ALIAS) {
                return Optional.empty();
            }
            AliasRootFacts facts = aliasRootAdmission.admitOrNull(aliasRef.target());
            if (facts == null) {
                return Optional.empty();
            }
            rootType = aliasRef.type();
            rootName = aliasRef.target();
            rootKind = LadderPlan.RootKind.ALIAS_CALL;
            rootMulti = facts.seamMulti();
        } else if (root.variableKind() == IRVariable.VariableKind.PARAM) {
            if (owner == null || root.type().hasMeta()) {
                return Optional.empty();
            }
            RAttribute rootInput = findOwnInput(owner, root.name());
            if (rootInput == null) {
                return Optional.empty();
            }
            // The root's spelling type rides the DECLARATION channel
            // (ws.getInferredAttributeType — the same fact the generated signature is
            // typed from, and the adapter's own meta-param precedent): the adapter's
            // node-channel type is MISSING on a measured slice of bare param references
            // (the § 11 shortfall forensics — four cdm5 STREAM walk sites), while the
            // declaration always types a real input. Node channel = the fallback (twins
            // may carry neither — the emitter's fallback belt owns those). A
            // meta-annotated DECLARATION refuses the chain — the § 10 wrapper-signature
            // exclusion (the root's Java name itself derefs) enforced on the reliable
            // channel, beltwise beside the node-channel hasMeta() check above.
            // ATTACHMENT GATE (census § 12): the channel exists only for a
            // workspace-ATTACHED declaration — a synthetic fromRule/fromReport owner
            // is a parentless in-memory bridge whose deep-copied typeCall has no
            // workspace (RNode.workspace() throws), so the rule-face bridge rides
            // the node channel; rootType is form-IRRELEVANT for the landed
            // SINGLE/TERMINAL_MULTI swaps (the ladder spells rootName + accessors,
            // the witness spells topType), carried for analytics there.
            rootType = (workspace == null || owner.parent() == null) ? null
                    : workspace.getInferredAttributeType(rootInput);
            if (rootType == null || rootType.isMissing()) {
                rootType = root.type();
            } else if (rootType.hasMeta()) {
                return Optional.empty();
            }
            rootName = FunctionGenerator.escapedFunctionInputName(owner, root.name(),
                    owner.name());
            rootKind = LadderPlan.RootKind.PARAM;
            rootMulti = root.cardinality() == ExpressionCardinality.MULTI;
        } else if (root.variableKind() == IRVariable.VariableKind.SYNTHETIC_ITEM) {
            // THE FAMILY-3 ADMISSION (census § 12 — the rule-face census stage): the
            // implicit item under extract/filter/then stages — the drr RULE face's
            // dominant root (284/418 chains) and a minority FUNCTION-face root. No
            // owner is consulted (an item is not an input declaration). The root
            // type rides the NODE channel only (no declaration exists — possibly
            // MISSING, a measured § 12 gap; never a refusal here, the census prices
            // the gap). A wrapper-TYPED item root is the leading-deref class (the
            // reference render derefs it before navigating): its wrapper kind maps
            // through the SAME detectMetaKind-mirroring vocabulary lock as the hop
            // channel — a bare/unknown qualifier set refuses the whole chain (the
            // hop law applied at the root seat). EMISSION-INERT this stage: the
            // emitter declines every ITEM plan behind its pending pins.
            rootType = root.type();
            if (rootType.hasMeta()) {
                rootMeta = metaKindOf(rootType.metaAttributes());
                if (rootMeta == LadderPlan.TopMeta.NONE) {
                    return Optional.empty();
                }
            }
            rootName = root.name();
            rootKind = LadderPlan.RootKind.ITEM;
            rootMulti = root.cardinality() == ExpressionCardinality.MULTI;
            if (!rootMulti && thenBodyArgumentIsMulti(expr)) {
                rootMulti = true;
                // The decode receipt: which seats the then-body root law re-shaped
                // (a bare counter movement cannot be decoded — the #607 instrument).
                thenBodyRootDeclines.add((owner == null ? "?" : owner.name()) + " "
                        + root.name() + " -> " + featuresTopDown);
            }
        } else {
            // ALIAS / USER_ITEM / CLOSURE_PARAM / LET_BINDER variable roots —
            // unpriced classes (census § 2 byRoot rows carry their sizes; the
            // § 6.3 ALIAS_CALL admission above covers only the
            // IRReference{ALIAS} residue, never an IRVariable kind).
            return Optional.empty();
        }
        List<String> accessors = new ArrayList<>(featuresTopDown.size());
        Collections.reverse(featuresTopDown);
        Collections.reverse(multiTopDown);
        Collections.reverse(typesTopDown);
        Collections.reverse(kindsTopDown);
        for (String feature : featuresTopDown) {
            // A null step is a baked interior meta deref — the runtime wrapper
            // interface's own accessor, never property-escaped.
            accessors.add(feature == null ? "getValue"
                    : JavaPojoProperty.escapeOperationName(
                            "get" + JavaNamingUtil.toFirstUpper(feature)));
        }
        return Optional.of(new LadderPlan(rootName, accessors,
                rootMulti, multiTopDown, topType,
                topMeta, fieldMetaHops, refMetaHops, rootType, typesTopDown,
                kindsTopDown, rootKind, rootMeta));
    }

    /**
     * THE THEN-BODY ROOT LAW (PR #607 — the first band-cell miscompile of the
     * ITEM pool): an implicit item that is the DIRECT body of a {@code then}
     * ({@code x then a -> b}, the implicit inline function) stands for the WHOLE
     * then-argument, not for one element of it. The IR variable types that item
     * per element (SINGLE), but the reference render hoists the argument as a
     * {@code MapperC} local when the argument is multi and maps over it
     * ({@code thenArgN.<A>map("getA", …)}) — so a SINGLE ladder over
     * {@code thenArgN.get()} would unwrap a list as one value (drr 7.x
     * {@code ClearingExceptionsAndExemptionsCounterparty}: {@code MapperS.of((thenArg3
     * .get() == null ? null : thenArg3.get().getClearingException()))} assigned to a
     * {@code MapperC<ClearingException>} local — javac's catch on the widened pair
     * gate). The then-ARGUMENT's cardinality is the render's own decision base, so it
     * is the root's: a multi argument makes the root multi and the shape STREAM (the
     * standing decline). An item under a list operation's own lambda ({@code then
     * filter …}, {@code then extract …}) is per element and keeps the IR reading.
     * The cardinality is read from the parser's FAITHFUL channel —
     * {@code CardinalityComputer.computeRuleBody}, upstream's own
     * {@code CardinalityProvider} rule: an implicit defined by a {@code then}'s
     * inline function IS the whole piped value, an extract/filter item is a
     * per-element view, a piped chain computed through its body end to end (the
     * channel the rule face and the validator already read) — never from the
     * adapter nor from the global {@code compute}, which both type a piped
     * {@code then} chain by its per-item body (SINGLE, the historical conservative
     * default the function tail's rendering is byte-frozen on): the first two cuts
     * of this law read those channels and each missed the carrier, whose argument
     * is a pipe rooted at an OUTER then-body's implicit item. The implicit variable
     * is found by descending the chain's receivers; a chain whose head is a
     * disguised spelling (no {@code RImplicitVariable} node) falls back to its
     * nearest enclosing inline function's {@code then} argument — the SAME walk
     * {@code CardinalityComputer.implicitItemCardinalityFrom} makes (LAW 69, the
     * two halves agree: an inline function under a {@code then} is the whole piped
     * value whether its parameter is implicit or spelt; the review dropped an
     * implicit-only conjunct the vendored walk does not have). Anything but SINGLE
     * reads as multi (the conservative direction).
     */
    private boolean thenBodyArgumentIsMulti(RExpression chainTop) {
        RExpression cur = chainTop;
        while (true) {
            if (cur instanceof RFeatureCall fc && fc.receiver() != null) {
                cur = fc.receiver();
            } else if (cur instanceof RDeepFeatureCall dfc && dfc.receiver() != null) {
                cur = dfc.receiver();
            } else {
                break;
            }
        }
        if (cur instanceof RImplicitVariable iv) {
            return CARDINALITY.computeRuleBody(iv) != ExpressionCardinality.SINGLE;
        }
        RNode n = chainTop.parent();
        while (n != null && !(n instanceof RInlineFunction)) {
            if (n instanceof RFunction) {
                return false;
            }
            n = n.parent();
        }
        if (!(n instanceof RInlineFunction fn)
                || !(fn.parent() instanceof RThenExpr then) || then.argument() == null) {
            return false;
        }
        return CARDINALITY.computeRuleBody(then.argument()) != ExpressionCardinality.SINGLE;
    }

    /** The parser's cardinality computer — its faithful ({@code computeRuleBody}) path is the channel read here. */
    private static final com.regnosys.rosetta.types.inference.CardinalityComputer CARDINALITY =
            new com.regnosys.rosetta.types.inference.CardinalityComputer();

    private final List<String> thenBodyRootDeclines = new ArrayList<>();

    /** The seats the then-body root law re-shaped to STREAM ({@code owner item -> [hops]}), in classify order. */
    public List<String> thenBodyRootDeclines() {
        return Collections.unmodifiableList(thenBodyRootDeclines);
    }

    /**
     * The wrapper kind of a meta hop's qualifier set — mirrors
     * {@code MetaFieldGenerator.detectMetaKind} exactly ({@code reference}/{@code
     * address} win over the field-meta qualifiers): {@code REF} → {@code
     * ReferenceWithMetaX}, {@code FIELD} → {@code FieldWithMetaX}, {@code NONE} →
     * refuse (a bare-only set renders PLAIN — unwitnessed here — and an unknown
     * token is an unproven render family).
     *
     * <p><b>Channel note (Seat-1 PR-6 OBS).</b> The qualifiers arrive on the
     * adapter's OWN-refs channel ({@code ExpressionToIRAdapter.metaQualifierNames}
     * — the attribute's own annotation refs), while the render's
     * {@code detectMetaKind} reads the parent-UNION
     * ({@code MetaFieldGenerator.allMetaAnnotationRefs} — override inheritance).
     * The two agree on the frozen 9.83.0 corpus (zero inherit-without-restate
     * overrides — the union javadoc's own pin), and every constructible divergence
     * fails safe here (bare/unknown → refuse; interior hops are kind-agnostic —
     * both wrapper families expose {@code getValue}; the one kind-sensitive seat,
     * the TM wrapper witness, is compile-loud in the differential gate). RE-CHECK
     * this agreement at every corpus bump, beside the witness-collision belt.
     *
     * <p>Package-visible since T2 (PR-23): {@link AliasValueSeamPolicy}'s
     * VALUE-BODY boundary witness derives the meta-topped wrapper through this
     * SAME discriminant (never reimplemented — the shared-channel law).
     */
    static LadderPlan.TopMeta metaKindOf(List<String> qualifiers) {
        boolean ref = false;
        boolean field = false;
        for (String qualifier : qualifiers) {
            switch (qualifier) {
                case "reference", "address" -> ref = true;
                case "scheme", "id", "location" -> field = true;
                case "bare" -> {
                    // detectMetaKind ignores the bare annotation — plain render.
                }
                default -> {
                    // An unknown token refuses REGARDLESS of the other qualifiers
                    // (never order-dependent — the sorted set must not decide).
                    return LadderPlan.TopMeta.NONE;
                }
            }
        }
        return ref ? LadderPlan.TopMeta.REF
                : field ? LadderPlan.TopMeta.FIELD : LadderPlan.TopMeta.NONE;
    }

    /** Convenience: classify with the owner resolved from the node's AST ancestry. */
    public Optional<LadderPlan> classify(RExpression expr, RWorkspace workspace) {
        return classify(expr, workspace, HandlerHelper.findEnclosingFunction(expr));
    }

    private static RAttribute findOwnInput(RFunction owner, String name) {
        for (RAttribute input : owner.inputs()) {
            if (input.name().equals(name)) {
                return input;
            }
        }
        return null;
    }
}
