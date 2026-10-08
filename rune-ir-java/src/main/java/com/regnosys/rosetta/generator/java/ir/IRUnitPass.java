package com.regnosys.rosetta.generator.java.ir;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import com.regnosys.rosetta.ast.RRootElement;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.types.RChoice;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.generator.GenerationException;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.generator.java.types.RGeneratedJavaClass;
import com.regnosys.rosetta.ir.adapter.IRTypeNode;

/**
 * ONE KIND'S HALF OF THE TYPE UNIT (v3.3 seat 9, PR #645 commit 12).
 *
 * <p>The five per-type DERIVED files are written by FIVE separate per-kind generators, each a
 * {@code JavaClassGenerator} driven per model, and each extending a DIFFERENT legacy class - so the IR-routed
 * variants cannot share a superclass. They share this COLLABORATOR instead: the workspace-wide {@link IRTypeIndex},
 * the unit construction ({@link IRTypeUnitWiring}), the per-element routing law, the shadow, and the four seams the
 * D11 host reads reflectively ({@code filesWrittenByIrEmitter}, {@code unitShadowRenders}, {@code unitAttemptedTypes},
 * {@code unitRefusedTypes}). One declaration, six callers - the POJO pass included (LAW 69).
 *
 * <p><b>THE ROUTING LAW</b> (the planning review's Q5), per element of the pass's own {@code streamObjects} - since
 * v3.3 seat 10, PR #646 commit 5 it is stated per KIND rather than per class, because the unit is available for the
 * CHOICE kind now:
 * <ul>
 *   <li>an element that is NOT a VALIDATED declaration ({@link #validated}), and EVERY element while the unit is
 *       UNAVAILABLE FOR ITS KIND ({@link IRTypeUnit#available(com.regnosys.rosetta.ir.core.IRKind)}), takes the
 *       INHERITED generator's path for that element - its file and its own errors exactly as on the OFF route;</li>
 *   <li>a validated element whose verdict has THIS member writes ONLY {@code outputKey(node, member)};</li>
 *   <li>a validated element for which this member is NOT APPLICABLE writes nothing and raises no error;</li>
 *   <li>a validated element the unit REFUSES takes the INHERITED path for that element - never a converted refusal,
 *       never a swallowed one: the old generator's own verdict for the element is the one that reaches
 *       {@code errors}.</li>
 * </ul>
 * The decision is always WHOLE-ELEMENT: the inherited generator writes the element's one file or the unit writes it,
 * and there is no arm in which both do.
 *
 * <p><b>ONE ELEMENT LOOP FOR SEVEN CALLERS</b> (v3.3 seat 10, PR #646 commit 5 - LAW 69, the commit-3 lift
 * generalised). {@link #routeElements} is that loop: the five derived passes reach it through {@link #routeWith},
 * and the two POJO-writing generators - {@link IRModelObjectGenerator} over the data types and
 * {@link IRChoiceObjectGenerator} over the choices - call it directly with their own
 * {@code createTypeRepresentation} / {@code generate} and their own populations. There is ONE routing law in the
 * module, and a kind's admission is written once.
 *
 * <p><b>THE SHADOW'S POPULATION AND THE ROUTE'S ARE ONE AGAIN</b> (v3.3 seat 10, PR #646 commit 5). {@link #shadow}
 * has walked the cell's VALIDATED ELEMENTS - the data types AND the choices - since commit 4, while the route sent
 * every choice down the inherited path; the kind switch's flip closes that gap, and what the shadow measures on a
 * choice is now what the route writes for it. The D11 {@code ... CHOICES} lines are what reads it.
 */
final class IRUnitPass {

    /** The pass's own {@code createTypeRepresentation} - the legacy generator's, called on the subclass. */
    interface TypeRepresentation {
        RGeneratedJavaClass<?> of(RRootElement element);
    }

    /** The pass's own {@code generate} - the legacy generator's, called on the subclass. */
    interface ElementRenderer {
        String render(RRootElement element, RGeneratedJavaClass<?> representation, String version);
    }

    private final GeneratorModel generatorModel;
    private final JavaTypeUtil typeUtil;
    private final IRTypeUnit.Member member;
    private final IRTypeIndex index;
    /**
     * The pass's OWN workspace-wide model index, on the SAME adapter as {@link #index} (v3.3 seat 9, PR #645
     * commit 13). The {@code META} member's qualify wing reads it; it is built ONCE per pass and never shared
     * across passes, because each pass adapts through its own adapter and a node from another pass's adapter is
     * not the instance this pass's halves compare.
     */
    private final IRModelIndex modelIndex;
    /** The index's parents ride their own reconciler, whose counters are this pass's to keep apart (LAW 84). */
    private final IRDeclarationReconciler parentReconciler;

    /** Every key a NEW IR emitter wrote on this pass - the D11 fallback gate's {@code newEmitter} set. */
    private final Set<String> written = new LinkedHashSet<>();
    /** The shadow renders of this pass's member, keyed by the member's own output key. */
    private final Map<String, String[]> shadow = new LinkedHashMap<>();
    private final Set<String> attempted = new TreeSet<>();
    private final Set<String> refused = new TreeSet<>();

    /**
     * THE PER-PASS EMITTER MEMO, KEYED BY THE VERSION STAMP (v3.3 seat 10, PR #646 commit 3 - PR #645 round 1
     * cq NIT-1). The wiring used to build six emitters, and so six {@code loadGroupFromClasspath} calls, for EVERY
     * model of EVERY pass; an emitter is a pure function of this pass's index, its model index, its generator
     * model's pruning set, the type util and the stamp, and of those only the stamp varies within a pass - so the
     * six are built ONCE PER STAMP here and handed to every unit of that stamp. See
     * {@link IRTypeUnitWiring#membersFor} for the law, the shared {@link IRDerivedFacts} and why the memo may not
     * be static: it lives BESIDE the index it was built over, and dies with the pass.
     *
     * <p>THE UNIT ITSELF IS NOT MEMOISED - its per-node memo and its attempted / refused sets are per-unit state,
     * and {@link #unitFor} still returns a FRESH unit per {@code generateClassesAsIR} call.
     */
    private final Map<String, Map<IRTypeUnit.Member, IRTypeUnit.MemberEmitter>> membersByStamp = new HashMap<>();

    IRUnitPass(GeneratorModel generatorModel, JavaTypeUtil typeUtil, IRTypeUnit.Member member) {
        this.generatorModel = Objects.requireNonNull(generatorModel, "generatorModel");
        this.typeUtil = Objects.requireNonNull(typeUtil, "typeUtil");
        this.member = Objects.requireNonNull(member, "member");
        IRDeclarationReconciler adapterHolder = new IRDeclarationReconciler(generatorModel);
        this.parentReconciler = new IRDeclarationReconciler(generatorModel, adapterHolder.adapter());
        this.index = new IRTypeIndex(generatorModel.workspace(), adapterHolder.adapter(), parentReconciler);
        this.modelIndex = new IRModelIndex(generatorModel.workspace(), adapterHolder.adapter());
    }

    /** The member this pass writes. */
    IRTypeUnit.Member member() {
        return member;
    }

    /** The workspace-wide type index of this pass - ONE node per declaration. */
    IRTypeIndex index() {
        return index;
    }

    /**
     * The type unit for one model's version, with exactly {@link IRTypeUnitWiring#READY_MEMBERS} ready - constructed
     * PER {@code generateClassesAsIR} CALL, so the memo lives for one model of one pass and no state crosses a model
     * or a pass (the planning review's Q1(c)).
     *
     * <p>Its six EMITTERS come from {@link #membersByStamp}, built once per version stamp on this pass (v3.3 seat
     * 10, PR #646 commit 3). The stamp is normalised by {@link IRTypeUnitWiring#stamp} - the ONE spelling both
     * roads take - and {@link IRTypeUnitWiring#AVAILABLE} is still read on the wiring alone.
     */
    IRTypeUnit unitFor(String version) {
        String stamp = IRTypeUnitWiring.stamp(version);
        return IRTypeUnitWiring.unitFor(membersByStamp.computeIfAbsent(stamp,
                s -> IRTypeUnitWiring.membersFor(index, modelIndex, generatorModel, typeUtil, s)));
    }

    /** {@code {declarations attempted, facts asserted, mismatches}} of the PARENTS the index resolved. */
    int[] parentReconcileStats() {
        return parentReconciler.stats();
    }

    /** The output keys this pass's NEW IR emitter wrote. */
    Set<String> filesWrittenByIrEmitter() {
        return Set.copyOf(written);
    }

    /** Every type name the production path was asked for, across this pass's models. */
    Set<String> attemptedTypes() {
        return Set.copyOf(attempted);
    }

    /** Every type name the production path refused, across this pass's models. */
    Set<String> refusedTypes() {
        return Set.copyOf(refused);
    }

    /**
     * The shadow renders of this pass's member - the D11 {@code UNIT SHADOW[&lt;MEMBER&gt;]} line's input, IN THE
     * ORDER THE PASS BOOKED THEM.
     *
     * <p><b>NOT {@code Map.copyOf} (v3.3 seat 9, PR #645 commit 14, MEASURED).</b> That factory returns an
     * immutable map whose iteration order is UNSPECIFIED and salted per JVM run, and the host's line draws its
     * first FIVE refusal samples in that order - so two runs of the SAME head printed two different five-line
     * samples under identical counts (c13-scoped-chaos.log against c14-scoped-chaos.log: the same twelve
     * BOILERPLATE_NAME_COLLISION refusals, a different five named). A print that moves without the code moving
     * is a print no one can diff, and the log diff of every later commit would carry the churn. The copy keeps
     * insertion order instead; no count and no byte depends on it.
     */
    Map<String, String[]> unitShadowRenders() {
        return java.util.Collections.unmodifiableMap(new LinkedHashMap<>(shadow));
    }

    // ------------------------------------------------------------------------------------------ the routing

    /**
     * Route one model's elements: the unit's key for a data type this member has, the inherited path for everything
     * else. See the class javadoc for the four arms.
     */
    List<GenerationException> route(Stream<? extends RRootElement> elements, RModel model, String version,
            Map<String, String> output, TypeRepresentation representation, ElementRenderer renderer) {
        return routeWith(unitFor(version), elements, model, version, output, representation, renderer);
    }

    /**
     * The routing law over a unit HANDED IN. {@link #route} is the production entry and builds the unit itself;
     * this overload exists so lane X3 can drive the REFUSAL arm with a unit that refuses - the arm is unreachable
     * through {@link #route} while {@link IRTypeUnitWiring#READY_MEMBERS} is short of a member, and an arm no
     * witness can reach is an arm nobody has proved (v3.3 seat 9, PR #645 commit 12). No production caller.
     */
    List<GenerationException> routeWith(IRTypeUnit unit, Stream<? extends RRootElement> elements, RModel model,
            String version, Map<String, String> output, TypeRepresentation representation,
            ElementRenderer renderer) {
        List<GenerationException> errors = routeElements(unit, elements.map(e -> (RRootElement) e).toList(),
                model.namespace(), version, index, member, output, written, new ArrayList<>(),
                representation, renderer);
        attempted.addAll(unit.attemptedTypes());
        refused.addAll(unit.refusedTypes());
        shadow(unit, model, version);
        return errors;
    }

    /**
     * THE ROUTING LAW ITSELF, over a population and an index HANDED IN - the class javadoc's four arms, and the ONE
     * element loop of the module (v3.3 seat 10, PR #646 commit 5; LAW 69, the commit-3 lift of {@link #inherited}
     * generalised from a helper to the whole loop). Seven callers: {@link #routeWith} for the five derived passes,
     * {@link IRModelObjectGenerator#generateClassesAsIR} for the data types' POJO and
     * {@link IRChoiceObjectGenerator#generateClassesAsIR} for the choices'.
     *
     * <p>It uses no instance state, so it is static: the file the unit writes, the fallback arm's bytes and its
     * errors all belong to the CALLER's generator and its own key sets, never to a pass.
     *
     * <p>THE KIND IS READ OFF THE NODE THE INDEX MINTED, never guessed from the element's class: the adapter is
     * what decides a declaration's {@link com.regnosys.rosetta.ir.core.IRKind}
     * ({@code AstToIRAdapter:220} STRUCT for a {@code data}, {@code :240} CHOICE for a {@code choice}), and
     * availability is a fact about the kind. The gate stands BEFORE {@link IRTypeUnit#verdict} rather than inside
     * it so an unavailable kind never enters the unit's {@code attempted} set - the D11 {@code TYPE UNIT VERDICTS}
     * line's population is the PRODUCTION path's, and a kind the route does not write has no business in it.
     *
     * @param ordered the elements to route, IN THE ORDER the caller's own {@code streamObjects} yields them
     * @param index the pass's own workspace-wide index - ONE node per declaration
     * @param written the caller's own {@code newEmitter} key set, added to for every file the unit writes here
     * @param walked filled with EVERY element this loop walked, in loop order - the input to the caller's
     *     population assertion ({@link IRModelObjectGenerator#assertUnitLoopCoversTheInheritedPopulation})
     */
    static List<GenerationException> routeElements(IRTypeUnit unit, List<? extends RRootElement> ordered,
            String namespace, String version, IRTypeIndex index, IRTypeUnit.Member member,
            Map<String, String> output, Set<String> written, List<RRootElement> walked,
            TypeRepresentation representation, ElementRenderer renderer) {
        List<GenerationException> errors = new ArrayList<>();
        for (RRootElement element : ordered) {
            walked.add(element);
            IRTypeNode node = null;
            IRTypeUnit.Verdict verdict = null;
            boolean inherit = !unit.available() || !validated(element);
            if (!inherit) {
                try {
                    node = nodeOf(index, namespace, element);
                    inherit = !unit.available(node.kind());
                    if (!inherit) {
                        verdict = unit.verdict(node);
                    }
                } catch (RuntimeException refusedWhole) {
                    // A REFUSAL IS A WHOLE-ELEMENT DECISION: the inherited generator writes this element's file
                    // and raises its own errors, exactly as on the OFF route. The refusal itself never lands in
                    // `errors` - it is this route's reason for taking the other path, not a generation failure of
                    // its own.
                    inherit = true;
                }
            }
            if (inherit) {
                inherited(element, version, output, errors, representation, renderer);
                continue;
            }
            Optional<String> text = verdict.textOf(member);
            if (text.isEmpty()) {
                continue;   // NOT APPLICABLE by law: no file, no error, no fallback
            }
            String key = IRTypeUnit.outputKey(node, member);
            output.put(key, text.get());
            written.add(key);
        }
        return errors;
    }

    /**
     * IS THIS ELEMENT A VALIDATED DECLARATION - a {@code data} type or a {@code choice}? ONE declaration of the
     * question for the route and the shadow alike (v3.3 seat 10, PR #646 commit 5, LAW 69). It is the OLD
     * generators' own population law, read from them rather than restated: {@code ValidatorScan.isValidatedType}
     * ({@code :80-82}) is {@code RDataType || RChoice}, and {@code ModelMetaGenerator.streamObjects}
     * ({@code :109-113}) and {@code DeepPathUtilGenerator.streamObjects} ({@code :113-118}) take the same pair.
     */
    static boolean validated(RRootElement element) {
        return element instanceof RDataType || element instanceof RChoice;
    }

    /** The index's node for a VALIDATED element, through the overload its own kind takes. */
    private static IRTypeNode nodeOf(IRTypeIndex index, String namespace, RRootElement element) {
        if (element instanceof RDataType dataType) {
            return index.node(namespace, dataType);
        }
        if (element instanceof RChoice choice) {
            return index.node(namespace, choice);
        }
        throw new IllegalStateException("the IR type unit was handed a " + element.getClass().getSimpleName()
                + ", which is not a VALIDATED declaration - only a data type or a choice has a node, and every"
                + " caller gates on IRUnitPass.validated before asking for one");
    }

    /** A validated element's DECLARED name - the one spelling the shadow's {@code (no output key)} entries take. */
    static String declaredName(RRootElement element) {
        if (element instanceof RDataType dataType) {
            return dataType.name();
        }
        if (element instanceof RChoice choice) {
            return choice.name();
        }
        throw new IllegalStateException("the IR type unit was handed a " + element.getClass().getSimpleName()
                + ", which is not a VALIDATED declaration");
    }

    /**
     * The INHERITED per-element path of {@code JavaClassGenerator.generateClasses} ({@code :59-75}), for ONE element
     * - the path resolved BEFORE the body so a failure names a file, a {@code GenerationException} kept as it is and
     * anything else wrapped, both with the target path attached. Copied statement for statement because the whole
     * point of the fallback arm is that the element's bytes AND its errors are the OFF route's.
     *
     * <p><b>ONE DECLARATION, SEVEN CALLERS</b> (v3.3 seat 10, PR #646 commit 3 - PR #645 round 1 cq NIT-2, LAW 69).
     * The five per-kind derived passes reach it through {@link #routeWith}; the POJO pass
     * ({@link IRModelObjectGenerator#generateClassesAsIR}) is the sixth caller of the six that does NOT route
     * through this class, and called it through a statement-for-statement COPY of this body until this commit. It
     * now calls THIS method, with its own {@code createTypeRepresentation} / {@code generate} handed in as the two
     * functional interfaces. It uses no instance state, so it is static: the fallback arm's bytes and errors
     * belong to the caller's generator, never to a pass.
     */
    static void inherited(RRootElement element, String version, Map<String, String> output,
            List<GenerationException> errors, TypeRepresentation representation, ElementRenderer renderer) {
        String filePath = null;
        try {
            RGeneratedJavaClass<?> typeRepresentation = representation.of(element);
            filePath = typeRepresentation.getCanonicalName().withForwardSlashes() + ".java";
            output.put(filePath, renderer.render(element, typeRepresentation, version));
        } catch (GenerationException e) {
            e.setTargetPath(filePath);
            errors.add(e);
        } catch (Exception e) {
            GenerationException wrapped = new GenerationException(e.getMessage(), null, null, e);
            wrapped.setTargetPath(filePath);
            errors.add(wrapped);
        }
    }

    // ------------------------------------------------------------------------------------------- the shadow

    /**
     * ASK THE MEMBER'S EMITTER for every VALIDATED ELEMENT of the model - since v3.3 seat 10, PR #646 commit 4 the
     * data types AND the choices - and BOOK what it answered, into no output map. The
     * shadow runs AFTER the pass's own generation and a throw of any kind is BOOKED rather than raised, so a shadow
     * can never break a generation the route would otherwise complete.
     *
     * <p>The entry shapes the D11 host reads reflectively:
     * <ul>
     *   <li>{@code {"RENDERED", <the file text>}} - the member wrote the whole file;</li>
     *   <li>{@code {"NOFILE"}} - the member answered NO FILE BY LAW (the deep-path util's eligibility fact);</li>
     *   <li>{@code {"REFUSED", <reason>}} - the member, the property model or the index refused the type by name.
     *       A member the wiring has NOT declared ready refuses every type with {@code member-not-ready}, which is
     *       the progress read the not-yet-landed members' lines print.</li>
     * </ul>
     */
    private void shadow(IRTypeUnit unit, RModel model, String version) {
        boolean ready = unit.ready().contains(member);
        String namespace = model.namespace();
        for (RRootElement element : model.rootElements()) {
            // THE SHADOW WALKS BOTH VALIDATED KINDS since v3.3 seat 10, PR #646 commit 4: the data types AND the
            // choices - through {@link #validated}, the SAME declaration the route above gates on since commit 5
            // (LAW 69). A choice owns exactly the same six files as a data type and the five legacy derived
            // generators already stream it, so the member has something to say about it and the D11 line holds
            // that against the old generator's own render.
            //
            // THE SHADOW STILL WRITES NOTHING: it books into `shadow`, which no output map reads. Since commit 5
            // the route writes what this walk measures rather than standing ahead of it, and the two read the
            // same node of the same index either way.
            if (!validated(element)) {
                continue;
            }
            String declaredName = declaredName(element);
            String key;
            IRTypeNode node;
            try {
                node = nodeOf(index, namespace, element);
                key = IRTypeUnit.outputKey(node, member);
            } catch (RuntimeException e) {
                shadow.put(namespace + "." + declaredName + " (no output key)",
                        new String[] {"REFUSED", reason(e)});
                continue;
            }
            if (!ready) {
                shadow.put(key, new String[] {"REFUSED", "member-not-ready"});
                continue;
            }
            try {
                Optional<String> text = unit.inspect(node, member);
                shadow.put(key, text.map(t -> new String[] {"RENDERED", t})
                        .orElseGet(() -> new String[] {"NOFILE"}));
            } catch (RuntimeException e) {
                shadow.put(key, new String[] {"REFUSED", reason(e)});
            }
        }
    }

    /**
     * A refusal's reason, short enough for a print: a NAMED missing fact by its name, everything else by its class.
     *
     * <p><b>ONE DECLARATION, SEVEN CALLERS</b> (v3.3 seat 10, PR #646 commit 3 - PR #645 round 1 cq NIT-2, LAW 69).
     * {@link #shadow} above and, since this commit, {@link IRModelObjectGenerator#shadowPojos} and its
     * {@code (no output key)} site - the POJO pass carried its own {@code shadowReason}, which named
     * {@link IRDataTypeEmitter.MissingIRFact} alone. This body is the UNION: it names
     * {@link IRValidatorScan.NamedRefusal} first and the missing fact second, and a POJO render can raise no
     * {@code NamedRefusal} (the validator scan is the three validator members' own), so the reasons the POJO
     * shadow prints are UNMOVED by the lift.
     */
    static String reason(RuntimeException e) {
        // A NAMED SITE first (v3.3 seat 9, PR #645 commit 12): a validator member refuses at one of the sites the
        // OLD generator refuses at, and the D11 UNIT SHADOW line ASSERTS that every refusal of a READY member
        // carries one - counting refusals would let an unnamed decline pass for an agreed one.
        if (e instanceof IRValidatorScan.NamedRefusal named) {
            return "refused-site:" + named.site();
        }
        if (e instanceof IRDataTypeEmitter.MissingIRFact missing) {
            return "missing-ir-fact:" + missing.fact();
        }
        String message = e.getMessage() == null ? "" : e.getMessage();
        int cut = message.indexOf(" - ");
        return e.getClass().getSimpleName() + ":" + (cut > 0 ? message.substring(0, cut) : message);
    }
}
