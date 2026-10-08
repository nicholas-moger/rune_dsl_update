package com.regnosys.rosetta.generator.java.ir;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.regnosys.rosetta.ast.RRootElement;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.types.RChoice;
import com.regnosys.rosetta.generator.GenerationException;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.object.ChoiceObjectGenerator;
import com.regnosys.rosetta.generator.java.object.ModelObjectGenerator;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.generator.java.types.RJavaPojoInterface;
import com.regnosys.rosetta.ir.adapter.AstToIRAdapter;
import com.regnosys.rosetta.ir.adapter.IRTypeNode;

/**
 * Path-2 (IR-routed) variant of {@link ChoiceObjectGenerator} for {@code choice}
 * declarations. For each {@link RChoice} it builds the CHOICE IR via
 * {@link AstToIRAdapter} and reconciles EVERY fact it carries against the source
 * ({@link IRDeclarationReconciler}, decision D55).
 *
 * <p><b>SINCE v3.3 seat 10, PR #646 commit 5 IT WRITES THE CHOICE POJO FROM THE IR ALONE.</b>
 * {@link IRTypeUnitWiring#CHOICE_AVAILABLE} is on, so this pass routes its own population - the
 * model's choices, in {@code streamObjects} order - through {@link IRUnitPass#routeElements}, the
 * module's ONE routing law: the unit writes the choice's POJO at the POJO member's key, or refuses
 * the choice WHOLE by name and the INHERITED Path-1 pipeline (decision L-001) writes its file and
 * raises its own errors exactly as on the OFF route. The five derived files of the same choice are
 * written by the five per-kind passes of the same unit, or fall back with it.
 *
 * <p><b>THE CHOICE POJO SHADOW RIDES THIS PASS since v3.3 seat 10, PR #646 commit 4.</b> A choice owns the SAME six
 * files as a data type, and {@link IRDataTypeEmitter} admits a CHOICE node since that commit - so after its own
 * reconcile and its own (legacy) generation this class asks the emitter what it WOULD write for every choice of the
 * model and books the answer into {@link #pojoShadowRenders()}, writing into no output map. The D11 host's
 * {@code POJO SHADOW CHOICES} line holds each render against the LEGACY {@link ChoiceObjectGenerator}'s own render
 * of the same key on the same run - a second real producer, the two-producer gate of PR #645 commit 18.
 *
 * <p><b>TWO GENERATORS, TWO MAPS, ONE LAW.</b> {@link IRModelObjectGenerator#pojoShadowRenders()} keeps the DATA
 * TYPES' renders; this map keeps the CHOICES'. They are never merged: the POJO pass's own population assertion
 * (PR #645 commit 15) holds its walk equal to {@code ModelObjectGenerator.streamObjects}, which yields data types
 * alone, and a merged map would make one line's population two populations.
 *
 * <p><b>ONE INDEX, ONE NODE PER DECLARATION.</b> The shadow runs on the DELEGATE's index and the delegate's
 * memoised shadow emitter, so the node this class measures is the very node the POJO pass's unit would be handed
 * and the emitter is the same construction, never a seventh one. That is why the delegate must be the IR-routed
 * {@link IRModelObjectGenerator}: a plain {@link ModelObjectGenerator} carries neither, and the constructor
 * REFUSES it by name rather than quietly shadowing nothing.
 *
 * <p>Used only when {@link IRFlag} is enabled; Path-1 is otherwise untouched.
 * Lab-authored Phase-1 (not present upstream).
 */
public final class IRChoiceObjectGenerator extends ChoiceObjectGenerator
        implements IREmittableGenerator {

    private final IRDeclarationReconciler reconciler;
    /**
     * THE IR POJO PASS THIS GENERATOR SHADOWS THROUGH (v3.3 seat 10, PR #646 commit 4) - the delegate, typed. The
     * provider hands the IR-routed generator here ({@code IRGenerationProviderImpl:49-51}); any other delegate is
     * refused at construction, because a shadow over a different index would compare a node the POJO pass never
     * minted.
     */
    private final IRModelObjectGenerator irDelegate;
    /**
     * The choice POJO shadow renders, keyed by {@link IRDataTypeEmitter#outputKey}, IN THE ORDER THE PASS BOOKED
     * THEM - not {@code Map.copyOf}'s, for the MEASURED reason {@link IRUnitPass#unitShadowRenders()} states: the
     * host draws its five-key samples in this map's iteration order, and that factory salts it per JVM run.
     */
    private final Map<String, String[]> pojoShadow = new LinkedHashMap<>();
    /**
     * Every choice POJO key the NEW IR emitter wrote on this generator's passes (v3.3 seat 10, PR #646 commit 5) -
     * the D11 {@code CHOICE IR file writers} line's {@code newEmitter} set, read off this class exactly as the
     * data types' is read off {@link IRModelObjectGenerator}. It is THIS generator's own set and never the
     * delegate's: a file has one writer, and the fallback gate judges the two sub-kinds apart.
     */
    private final Set<String> unitWritten = new java.util.LinkedHashSet<>();

    public IRChoiceObjectGenerator(GeneratorModel generatorModel, JavaTypeTranslator typeTranslator,
                                   JavaTypeUtil typeUtil, ModelObjectGenerator delegate) {
        super(generatorModel, typeTranslator, typeUtil, delegate);
        // THE DELEGATE IS THE IR POJO PASS OR THE CONSTRUCTION IS REFUSED BY NAME (v3.3 seat 10, PR #646
        // commit 4). Written as a test-and-cast rather than a pattern variable ON PURPOSE: a lane has to be
        // able to drop the check and see what happens next, and a pattern variable would take the assignment
        // with it and stop compiling instead of failing.
        if (!(delegate instanceof IRModelObjectGenerator)) {
            throw new IllegalArgumentException("the IR choice pass needs the IR POJO pass as its delegate - one"
                    + " index, one node per declaration - and was handed a "
                    + (delegate == null ? "null" : delegate.getClass().getName()));
        }
        this.irDelegate = (IRModelObjectGenerator) delegate;
        this.reconciler = new IRDeclarationReconciler(generatorModel);
    }

    /**
     * {@code {declarations attempted, facts asserted, mismatches}} of this generator's passes - the D11 host
     * prints the three per cell and asserts the last at zero (v3.3 seat 5, decision D55).
     */
    public int[] declarationReconcileStats() {
        return reconciler.stats();
    }

    /**
     * SEAM for the D11 host (read reflectively, like {@link IRModelObjectGenerator#pojoShadowRenders()}): what the
     * IR POJO emitter WOULD write for every choice of this cell, keyed by the choice's own POJO output key. The
     * host's {@code POJO SHADOW CHOICES} line is its gate - {@code differing == 0} against the legacy
     * {@link ChoiceObjectGenerator}'s own render, and {@code identical + noGolden == choices}.
     */
    public Map<String, String[]> pojoShadowRenders() {
        return java.util.Collections.unmodifiableMap(new LinkedHashMap<>(pojoShadow));
    }

    /**
     * SEAM for the D11 host (read reflectively, beside {@link #pojoShadowRenders()}): the members the wiring
     * declares ready, as their names - the SAME one declaration the six passes read
     * ({@link IRTypeUnitWiring#readyNames()}). The host's {@code POJO SHADOW CHOICES} line reads it to decide
     * whether that line is a GATE or a progress print, and the POJO member has been ready since PR #645 commit 11,
     * so it is a gate. Without this seam the line would print its columns and judge nothing.
     *
     * <p>This generator joins no {@code UNIT READY} or {@code TYPE UNIT VERDICTS} row of its own: since v3.3 seat
     * 10, PR #646 commit 5 it routes the choices through the DELEGATE's unit and books that unit's verdict sets
     * into the delegate ({@link IRModelObjectGenerator#bookUnitVerdicts}), because the POJO member is ONE member
     * however many generators route it - the six passes' members are what those lines are about.
     */
    public Set<String> unitReady() {
        return IRTypeUnitWiring.readyNames();
    }

    /**
     * SEAM for the D11 host: the choice POJO files the NEW IR emitter wrote (v3.3 seat 10, PR #646 commit 5). The
     * {@code CHOICE} sub-kind's fallback gate books every OTHER emitted choice file to the old generator and holds
     * that set EQUAL to the committed register, whose {@code CHOICE} rows this commit re-cuts to the flip run's
     * own dump.
     */
    @Override
    public Set<String> filesWrittenByIrEmitter() {
        return Set.copyOf(unitWritten);
    }

    @Override
    public List<GenerationException> generateClassesAsIR(RModel model, String version,
                                                         Map<String, String> output) {
        List<GenerationException> errors = new ArrayList<>(reconcile(model));
        // THE CHOICE ROUTE, since v3.3 seat 10, PR #646 commit 5. The unit comes from the DELEGATE - one index,
        // one node per declaration, one memoised emitter set per version stamp - and never from a second
        // IRUnitPass of this class's own, which would mint a second node for every choice and compare a render
        // the POJO pass never made. The loop is IRUnitPass.routeElements, the ONE routing law of the module
        // (LAW 69): a choice whose verdict has the POJO member is written at the member's key, a choice the unit
        // REFUSES takes the INHERITED legacy path WHOLE - its file and its own errors exactly as on the OFF
        // route, through this generator's own createTypeRepresentation and generate.
        //
        // The population handed in is this class's own re-derivation of the choices; the assertion below holds
        // the walk against ChoiceObjectGenerator.streamObjects's own yield: the two filters are the same filter, and
        // what the assertion witnesses is the LOOP'S WALK against that population - every element walked once, in
        // order, none skipped (lane P1's hole).
        IRTypeUnit unit = irDelegate.unitFor(version);
        if (unit.available()) {
            List<RRootElement> mine = new ArrayList<>();
            for (RRootElement element : model.rootElements()) {
                if (element instanceof RChoice) {
                    mine.add(element);
                }
            }
            List<RRootElement> walked = new ArrayList<>();
            errors.addAll(IRUnitPass.routeElements(unit, mine, model.namespace(), version, irDelegate.index(),
                    IRTypeUnit.Member.POJO, output, unitWritten, walked,
                    e -> createTypeRepresentation((RChoice) e),
                    (e, r, v) -> generate((RChoice) e, (RJavaPojoInterface) r, v)));
            // THE MEMBER'S VERDICT SETS ARE THE MEMBER'S: the POJO pass is routed by two generators since this
            // commit, and the D11 TYPE UNIT VERDICTS line reads one set off the delegate (PR #646 commit 5)
            irDelegate.bookUnitVerdicts(unit);
            IRModelObjectGenerator.assertUnitLoopCoversTheInheritedPopulation(model.namespace(),
                    streamObjects(model).toList(), walked);
        } else {
            errors.addAll(generateClasses(model, version, output));
        }
        // THE SHADOW RUNS LAST and writes into no output map (v3.3 seat 10, PR #646 commit 4) - a measurement,
        // never an emission, and a throw of any kind is BOOKED rather than raised, so it can never break a
        // generation the route would otherwise complete. Since commit 5 it measures the very render the route
        // above wrote, against the legacy generator's own on the same run.
        shadowChoicePojos(model, version);
        return errors;
    }

    /** Build and structurally check the CHOICE IR for each choice in the model. */
    List<GenerationException> reconcile(RModel model) {   // package-private: the throw witness calls the reconcile alone
        List<GenerationException> errors = new ArrayList<>();
        String namespace = model.namespace();
        for (var element : model.rootElements()) {
            if (element instanceof RChoice choice) {
                reconciler.attempt();   // counted BEFORE the adapter: a throw below is a mismatch, never a silent drop
                try {
                    IRTypeNode ir = reconciler.adapter().adaptChoice(namespace, choice);
                    for (String mismatch : reconciler.reconcileChoice(namespace, choice, ir)) {
                        errors.add(new GenerationException(mismatch, null, choice));
                    }
                } catch (RuntimeException e) {
                    reconciler.threw();
                    errors.add(new GenerationException(
                            "AST->IR adapter failed for choice " + choice.name(),
                            null, choice, e));
                }
            }
        }
        return errors;
    }

    /**
     * Ask {@link IRDataTypeEmitter} for a SHADOW render of every {@code choice} declaration of this model and book
     * what it answered - the SAME three entry shapes {@link IRModelObjectGenerator}'s own {@code shadowPojos}
     * books, so the host reads one channel with one reader:
     * <ul>
     *   <li>{@code {"RENDERED", <the file text>, <the section line starts>}};</li>
     *   <li>{@code {"PARTIAL", <section>[, <the prefix>, <its section line starts>]}};</li>
     *   <li>{@code {"REFUSED", <reason>}} - by {@link IRUnitPass#reason}, the ONE declaration all the shadow
     *       sites take.</li>
     * </ul>
     * The node and the emitter both come from the DELEGATE: {@link IRModelObjectGenerator#index()} hands out ONE
     * node per declaration for the whole workspace and {@link IRModelObjectGenerator#emitterFor(String)} is the
     * pass's memoised shadow emitter, which is a SEPARATE construction from the unit's (PR #645 round 1 cq SF-1)
     * and is not rebuilt per model (PR #645 round 1 cq NIT-1).
     */
    private void shadowChoicePojos(RModel model, String version) {
        IRDataTypeEmitter emitter = irDelegate.emitterFor(version);
        String namespace = model.namespace();
        for (var element : model.rootElements()) {
            if (!(element instanceof RChoice choice)) {
                continue;
            }
            String key;
            IRTypeNode node;
            try {
                node = irDelegate.index().node(namespace, choice);
                key = IRDataTypeEmitter.outputKey(node);
            } catch (RuntimeException e) {
                // the node or its key could not even be formed: book it under the DECLARATION's qualified name, so
                // the choice is still counted and named rather than dropped from the shadow's population
                pojoShadow.put(namespace + "." + choice.name() + " (no output key)",
                        new String[] {"REFUSED", IRUnitPass.reason(e)});
                continue;
            }
            try {
                IRDataTypeEmitter.Rendered rendered = emitter.renderWithSections(node);
                pojoShadow.put(key, new String[] {"RENDERED", rendered.text(),
                        IRModelObjectGenerator.renderSectionLineStarts(rendered.sectionLineStarts())});
            } catch (IRDataTypeEmitter.NotYetRendered partial) {
                pojoShadow.put(key, partial.prefix()
                        .map(prefix -> new String[] {"PARTIAL", partial.section().name(), prefix.text(),
                                IRModelObjectGenerator.renderSectionLineStarts(prefix.sectionLineStarts())})
                        .orElseGet(() -> new String[] {"PARTIAL", partial.section().name()}));
            } catch (RuntimeException e) {
                pojoShadow.put(key, new String[] {"REFUSED", IRUnitPass.reason(e)});
            }
        }
    }
}
