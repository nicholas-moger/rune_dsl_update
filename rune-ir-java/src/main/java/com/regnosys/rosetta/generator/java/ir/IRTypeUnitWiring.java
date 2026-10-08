package com.regnosys.rosetta.generator.java.ir;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;

/**
 * THE WIRING OF THE TYPE UNIT - ONE DECLARATION, SIX CALLERS (v3.3 seat 9, PR #645 commit 12).
 *
 * <p>{@link #READY_MEMBERS} is the single place the fork states which of the six {@link IRTypeUnit.Member}s write
 * from the IR alone today. Six generators build a unit - the POJO pass ({@link IRModelObjectGenerator}) and the five
 * per-kind derived passes - and every one of them reads THIS set and THIS construction, so the all-or-nothing law
 * cannot be true of one pass and false of another because someone edited five copies of a literal.
 *
 * <p>The construction is also one declaration: every member's emitter is built HERE, from the index, the generator
 * model's {@code doNotPrune} set and the version stamp, so the file the D11 SHADOW measures is the file the unit
 * would write - the commit-11 law ({@code emitterFor} serving both), generalised to six members.
 */
final class IRTypeUnitWiring {

    /**
     * THE MEMBERS THE WIRING DECLARES READY. The POJO member is READY since v3.3 seat 9, PR #645 commit 11: its
     * emitter renders the whole file for every data type of the corpus and the D11 POJO SHADOW line holds it
     * byte-identical to the golden (or to the old generator's own file where the golden is contested) on 19,300 of
     * 19,316 types (READ: the s9c10 chain and the offload box s9c10b run), the 16 others having no golden.
     *
     * <p>THE THREE VALIDATOR MEMBERS ARE READY since v3.3 seat 9, PR #645 commit 12: each writes its whole file
     * for every validated data type from {@link IRDerivedFacts} and {@link IRPropertyModel} alone, and each D11
     * {@code UNIT SHADOW[<MEMBER>]} line is its GATE - {@code identical + noGolden + refusedExpected == types},
     * with {@code refused} carrying none but the NAMED sites the old generator refuses at and
     * {@code renderedWhereLegacyRefused} at zero.
     *
     * <p>THE {@code META} MEMBER IS READY since v3.3 seat 9, PR #645 commit 13: {@link IRModelMetaEmitter} writes
     * the whole {@code <Simple>Meta} for every validated data type from {@link IRDerivedFacts} and
     * {@link IRModelIndex} alone - the condition refs of the supertype chain and, for the workspace's first-wins
     * qualifiable roots, the qualify wing - and the D11 {@code UNIT SHADOW[META]} line is its GATE.
     *
     * <p>THE {@code DEEP_PATH_UTIL} MEMBER IS READY since v3.3 seat 9, PR #645 commit 14:
     * {@link IRDeepPathUtilEmitter} writes the whole {@code <Simple>DeepPathUtil} for every ELIGIBLE data type
     * from {@link IRDerivedFacts} and {@link IRPropertyModel} alone - the deep-feature map in its own iteration
     * order, the alternatives' guard-return chain, the injected sibling utils in the {@code HashSet}'s own order -
     * and answers NO FILE BY LAW for a type the eligibility fact refuses, which is the one lawful empty answer of
     * the six. The D11 {@code UNIT SHADOW[DEEP_PATH_UTIL]} line is its GATE.
     *
     * <p>SO ALL SIX MEMBERS ARE READY - AND SINCE v3.3 seat 9, PR #645 commit 15 THE WIRING'S SWITCH IS ON
     * TOO, so the unit WRITES every data type whole on the reference IR route. See {@link #AVAILABLE}.
     */
    static final Set<IRTypeUnit.Member> READY_MEMBERS = Set.of(
            IRTypeUnit.Member.POJO,
            IRTypeUnit.Member.TYPE_FORMAT_VALIDATOR,
            IRTypeUnit.Member.CARDINALITY_VALIDATOR,
            IRTypeUnit.Member.ONLY_EXISTS_VALIDATOR,
            IRTypeUnit.Member.META,
            IRTypeUnit.Member.DEEP_PATH_UTIL);

    /**
     * THE WIRING'S SECOND DECLARATION (v3.3 seat 9, PR #645 commit 14): whether the type unit may WRITE. It is a
     * fact of its own, apart from {@link #READY_MEMBERS}, because turning the route on is not the same event as
     * finishing the last member: the switch also turns the fallback register's DATA_TYPE rows to zero and moves
     * the file meter, and the strict path holds every member's bytes against the golden on the FULL corpus before
     * any of that happens.
     *
     * <p><b>THE ROUTE IS ON since v3.3 seat 9, PR #645 commit 15.</b> All six members are ready and this switch is
     * TRUE, so on the reference IR route every data type of every cell is written WHOLE by the unit - its six files
     * from {@link com.regnosys.rosetta.ir.adapter.IRTypeNode} alone - and only a type the unit REFUSES BY NAME
     * falls back, whole, to the old generator. Each of the six D11 {@code UNIT SHADOW} lines remains its member's
     * gate; the register's DATA_TYPE rows are re-cut to the run's own dump in the SAME commit, and the file meter
     * is COMPUTED from the register there.
     *
     * <p><b>THIS IS THE WHOLE-ROUTE KILL SWITCH.</b> Setting it back to {@code false} sends EVERY data type back to
     * the old generator, byte-identical (the rings hold both routes at {@code 9062fc14}), because the unit's
     * refusal arm is the inherited generator's own path per element. It is not a free undo, though: the fallback
     * register is SHRINK-ONLY, so the DATA_TYPE rows this commit deletes would be OWED BACK before an ON-route run
     * could go green again, and a register cannot grow. In practice the switch is one-way; a revert of this commit,
     * register and all, is the way back.
     *
     * <p>{@link IRTypeUnit#available()} is {@code READY_MEMBERS} complete AND this - ONE predicate over two facts,
     * read by {@link #unitFor} alone.
     */
    static final boolean AVAILABLE = true;

    /**
     * THE WIRING'S THIRD DECLARATION - THE KIND-SCOPED SWITCH (v3.3 seat 10, PR #646 commit 4): whether the type
     * unit may write a node of kind {@code CHOICE}. It is a SECOND declaration of exactly the same sort as
     * {@link #AVAILABLE}, for exactly the same reason, one kind over: the commit that lands the POJO emitter's
     * CHOICE arm and the six choice SHADOW lines is NOT the commit that turns the choice route on, because
     * switching it on also re-cuts the fallback register's {@code CHOICE} rows and moves the file meter, and the
     * strict path holds every member's choice bytes against the LEGACY generator's own render on the full corpus
     * before any of that happens (PR #645's commit 14 -&gt; 15 shape, kept).
     *
     * <p><b>THE CHOICE ROUTE IS ON since v3.3 seat 10, PR #646 commit 5.</b> {@link IRUnitPass#routeElements}'s
     * routing law admits a CHOICE node beside a STRUCT one, so on the reference IR route every choice of every
     * cell is written WHOLE by the unit - its six files from {@link com.regnosys.rosetta.ir.adapter.IRTypeNode}
     * alone, the POJO through {@link IRChoiceObjectGenerator} and the five derived files through the five
     * per-kind passes - and only a choice the unit REFUSES BY NAME falls back, whole, to the old generator. The
     * commit-4 SHADOW is what earned the flip: every choice of every cell rendered byte-identically to the legacy
     * render on the five derived members, and on the POJO member identical + no-golden == choices against the legacy
     * {@code ChoiceObjectGenerator}'s own render (235 identical + 2 no-golden of 237 over the 26 cells; 155 + 2 of 157 on the
     * chaos cell alone - the two chaos choices the corpus holds no file for), before this switch moved. The register's
     * {@code CHOICE} rows are re-cut to the flip run's own dump in the SAME commit, and the file meter is
     * COMPUTED from the register there.
     *
     * <p>Like {@link #AVAILABLE} it is the CHOICE route's kill switch with the same one-way caveat: setting it
     * back to {@code false} sends every choice back to the old generator, byte-identical, because the unit's
     * refusal arm IS the inherited generator's own path per element - but the fallback register is SHRINK-ONLY,
     * so the {@code CHOICE} rows this commit deletes would be OWED BACK before an ON-route run could go green
     * again, and a register cannot grow. In practice the switch is one-way; a revert of this commit, register and
     * all, is the way back.
     *
     * <p>{@link IRTypeUnit#available(com.regnosys.rosetta.ir.core.IRKind)} is {@link IRTypeUnit#available()}
     * AND, for the CHOICE kind alone, this - ONE predicate over three facts, read by {@link #unitFor(Map)} alone.
     */
    static final boolean CHOICE_AVAILABLE = true;

    private IRTypeUnitWiring() {
    }

    /**
     * The type unit for one model's version, with exactly {@link #READY_MEMBERS} declared ready and the wiring's own
     * {@link #AVAILABLE} switch. Since PR #645 commit 15 all six members are ready AND the switch is on, so
     * {@link IRTypeUnit#available()} is TRUE and {@link IRTypeUnit#verdict} writes every data type whole - or
     * refuses it whole BY NAME, and the caller takes the inherited generator's path for that element.
     *
     * <p>THIS OVERLOAD BUILDS THE SIX EMITTERS EVERY TIME. The two production passes take the MEMOISED seam
     * instead - {@link #membersFor} through their own per-stamp map, then {@link #unitFor(Map)} (v3.3 seat 10,
     * PR #646 commit 3). This one stays for the callers that hold no memo, and is the one declaration both roads
     * end at.
     *
     * @param index      the pass's own workspace-wide index - ONE node per declaration
     * @param modelIndex the pass's own workspace-wide MODEL index, built beside {@code index} on the SAME adapter -
     *     the {@code META} member's qualify wing is the one member fact that is not a function of the node's own
     *     chain (a first-wins root over the workspace's models IN LOAD ORDER, dependency models included). The
     *     CONSTRUCTION is shared here; an INSTANCE is never shared across passes, because each pass adapts on its
     *     own adapter (the commit-12 law - see {@code IRUnitPass})
     * @param gm         the pass's generator model, read for its {@code doNotPrune} set alone
     * @param typeUtil   the PURE structural type table the compat algebra is parameterised by
     * @param version    the host's version stamp for this model, empty when it passes none
     */
    static IRTypeUnit unitFor(IRTypeIndex index, IRModelIndex modelIndex, GeneratorModel gm,
            JavaTypeUtil typeUtil, String version) {
        return unitFor(membersFor(index, modelIndex, gm, typeUtil, stamp(version)));
    }

    /**
     * THE VERSION STAMP, IN ONE SPELLING (v3.3 seat 10, PR #646 commit 3 - round 1 cq NIT-1). The host passes a
     * version PER MODEL and passes {@code null} where it has none; the emitters that write it ({@link
     * IRModelMetaEmitter}, the POJO's javadoc) take the normalised form. It is the MEMO'S KEY, so it has to be
     * derived in ONE place: a pass that normalised it differently from this class would memo under one key and
     * construct under another.
     */
    static String stamp(String version) {
        return version == null ? "" : version;
    }

    /**
     * The type unit over a member map built by {@link #membersFor} - the seam the PER-PASS MEMO takes (v3.3 seat 10,
     * PR #646 commit 3, round 1 cq NIT-1). {@link #AVAILABLE} is read HERE and nowhere else, so the switch stays one
     * declaration however the members were obtained. The UNIT ITSELF IS NEVER MEMOISED: its per-node memo and its
     * attempted / refused sets are per-unit state ({@link IRTypeUnit}'s javadoc, "THE MEMO ... lives on the unit
     * INSTANCE"), and one unit per {@code generateClassesAsIR} call is what keeps them per model.
     */
    static IRTypeUnit unitFor(Map<IRTypeUnit.Member, IRTypeUnit.MemberEmitter> members) {
        return new IRTypeUnit(members, AVAILABLE, CHOICE_AVAILABLE);
    }

    /**
     * THE SIX MEMBERS' EMITTERS, BUILT ONCE PER (PASS, STAMP) (v3.3 seat 10, PR #646 commit 3 - PR #645 round 1
     * cq NIT-1: the wiring built six fresh emitters, and so six {@code TemplateRenderer.loadGroupFromClasspath}
     * calls, PER MODEL PER PASS).
     *
     * <p><b>THE LAW OF THE MEMO.</b> An emitter is a PURE FUNCTION of the pass's {@code index}, the pass's
     * {@code modelIndex}, the version {@code stamp}, {@code gm.doNotPrune()} and {@code typeUtil} - the same claim
     * this class's own javadoc makes and the D11 host's {@code TYPE UNIT VERDICTS} line asserts across the six
     * passes. Within ONE pass the index, the model index, the pruning set and the type util are FIXED; only the
     * stamp varies, because the host passes a version per model. So a memo KEYED BY THE STAMP and living on the
     * PASS - beside the index it was built over - hands the same six emitters to every unit of that stamp without
     * changing what any of them writes.
     *
     * <p><b>WHY THE SHARED {@link IRDerivedFacts} IS SOUND.</b> The facts instance carries per-node caches
     * ({@code Descend.attributeCache}, an {@code IdentityHashMap} keyed on {@link
     * com.regnosys.rosetta.ir.adapter.IRTypeNode}). The index hands out ONE node per declaration for the whole
     * workspace, so a node's identity is stable across the pass's models and a cache entry booked while rendering
     * one model answers the same for the next. The memo therefore keeps ONE facts instance per stamp PER PASS -
     * never static, never on a provider - so an {@code IRDerivedFacts} can never outlive the index it was built
     * over. {@link IRUnitPass#unitFor} and {@link IRModelObjectGenerator#unitFor} are the two callers that hold it.
     *
     * @param stamp the version stamp ALREADY normalised by {@link #stamp(String)} - the memo's key
     */
    static Map<IRTypeUnit.Member, IRTypeUnit.MemberEmitter> membersFor(IRTypeIndex index, IRModelIndex modelIndex,
            GeneratorModel gm, JavaTypeUtil typeUtil, String stamp) {
        EnumMap<IRTypeUnit.Member, IRTypeUnit.MemberEmitter> ready =
                new EnumMap<>(IRTypeUnit.Member.class);
        if (READY_MEMBERS.contains(IRTypeUnit.Member.POJO)) {
            IRDataTypeEmitter pojo = new IRDataTypeEmitter(index,
                    new IRDataTypeEmitter.Config(stamp, gm.doNotPrune()), typeUtil);
            // the POJO is written for EVERY data type - it never answers "no file by law"
            ready.put(IRTypeUnit.Member.POJO, node -> Optional.of(pojo.render(node)));
        }
        // THE THREE VALIDATOR MEMBERS (v3.3 seat 9, PR #645 commit 12) share ONE IRDerivedFacts over this pass's
        // index - the very class the derived-facts reconcile holds its IR half against, built with
        // IRDerivedLie.NONE so no test that lies to a reconciler can ever lie to an emitter.
        IRDerivedFacts facts = new IRDerivedFacts(index, IRDerivedLie.NONE);
        if (READY_MEMBERS.contains(IRTypeUnit.Member.TYPE_FORMAT_VALIDATOR)) {
            ready.put(IRTypeUnit.Member.TYPE_FORMAT_VALIDATOR,
                    new IRTypeFormatValidatorEmitter(index, facts));
        }
        if (READY_MEMBERS.contains(IRTypeUnit.Member.CARDINALITY_VALIDATOR)) {
            ready.put(IRTypeUnit.Member.CARDINALITY_VALIDATOR,
                    new IRCardinalityValidatorEmitter(index, facts));
        }
        if (READY_MEMBERS.contains(IRTypeUnit.Member.ONLY_EXISTS_VALIDATOR)) {
            ready.put(IRTypeUnit.Member.ONLY_EXISTS_VALIDATOR,
                    new IROnlyExistsValidatorEmitter(index, facts));
        }
        // THE META MEMBER (v3.3 seat 9, PR #645 commit 13) takes the SAME facts and, alone among the five, the
        // pass's MODEL index - the qualify wing's root is a load-order fact over the whole workspace - plus the
        // version stamp, which the meta file writes into its javadoc and the validators never read.
        if (READY_MEMBERS.contains(IRTypeUnit.Member.META)) {
            ready.put(IRTypeUnit.Member.META, new IRModelMetaEmitter(facts, modelIndex, stamp));
        }
        // THE DEEP-PATH MEMBER (v3.3 seat 9, PR #645 commit 14) takes the SAME facts and the pass's own index -
        // its descend targets and their property models are workspace reads - and no version stamp: the family
        // writes none (no javadoc, no stamp, anywhere in the file).
        if (READY_MEMBERS.contains(IRTypeUnit.Member.DEEP_PATH_UTIL)) {
            ready.put(IRTypeUnit.Member.DEEP_PATH_UTIL, new IRDeepPathUtilEmitter(index, facts));
        }
        return ready;
    }

    /** The wiring's AVAILABLE switch - the seam the D11 host reads reflectively beside {@link #readyNames}. */
    static boolean available() {
        return AVAILABLE;
    }

    /**
     * The wiring's KIND-SCOPED switch (v3.3 seat 10, PR #646 commit 4) - the seam the D11 host reads reflectively
     * beside {@link #available()}, and prints on its own {@code UNIT READY} line. THREE facts, three seams: a
     * line that named only "6 of 6 ; AVAILABLE=true" would read as a route that writes choices, and it does not.
     */
    static boolean choiceAvailable() {
        return CHOICE_AVAILABLE;
    }

    /** The ready members' NAMES - the seam the D11 host reads reflectively off every one of the six generators. */
    static Set<String> readyNames() {
        Set<String> names = new TreeSet<>();
        for (IRTypeUnit.Member member : READY_MEMBERS) {
            names.add(member.name());
        }
        return names;
    }
}
