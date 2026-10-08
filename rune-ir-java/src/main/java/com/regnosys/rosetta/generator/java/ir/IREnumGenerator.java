package com.regnosys.rosetta.generator.java.ir;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.types.REnumeration;
import com.regnosys.rosetta.generator.GenerationException;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.enums.EnumGenerator;
import com.regnosys.rosetta.ir.adapter.IREnumNode;

/**
 * Path-2 (IR-routed) variant of {@link EnumGenerator} for {@code enum} declarations. For each {@link REnumeration}
 * it builds the ENUM IR through the workspace-wide {@link IREnumIndex} and reconciles EVERY fact it carries against
 * the source ({@link IRDeclarationReconciler}, decision D55), then - since v3.3 seat 6 (PR #642, ruling R3 stage 1) -
 * WRITES THE FILE FROM THE IR ALONE through {@link IREnumEmitter}: the inherited Path-1 pipeline is NOT called for
 * any enumeration on this route, and {@link #filesWrittenByIrEmitter()} names every file the emitter wrote, so the
 * D11 host's fallback gate reads {@code newEmitter=N oldGenerator=0} on every ENUM line and the register holds no
 * ENUM row.
 *
 * <p>All or nothing: a declaration whose IR DISAGREES with its source (a reconcile mismatch) is REFUSED - its
 * mismatches are the generation errors and no file is written for it (a file written from an IR known to be wrong
 * is what R1 forbids); there is no per-file fallback to the old generator. A parent an enum extends is reconciled
 * too, on first resolution, through a second reconciler whose counters are kept apart from this pass's population
 * (the D11 host holds the pass's {@code declarations} equal to ITS count of the cell's emitted enums, LAW 84, and a
 * parent may live outside the cell's emission filter).
 *
 * <p>Used only when {@link IRFlag} is enabled; Path-1 is otherwise untouched. Lab-authored Phase-1 (not present
 * upstream).
 */
public final class IREnumGenerator extends EnumGenerator implements IREmittableGenerator {

    private final IRDeclarationReconciler reconciler;
    private final IRDeclarationReconciler parentReconciler;
    private final IREnumIndex index;
    private final IREnumEmitter emitter;
    private final Set<REnumeration> refused = new HashSet<>();
    private final Set<REnumeration> parentsReconciled = new HashSet<>();
    private final Set<REnumeration> parentsRefused = new HashSet<>();

    public IREnumGenerator(GeneratorModel generatorModel) {
        super(generatorModel);
        this.reconciler = new IRDeclarationReconciler(generatorModel);
        this.parentReconciler = new IRDeclarationReconciler(generatorModel);
        this.index = new IREnumIndex(generatorModel.workspace(), reconciler.adapter());
        this.emitter = new IREnumEmitter(this::resolveParent);
    }

    /**
     * {@code {declarations attempted, facts asserted, mismatches}} of this generator's passes - the D11 host
     * prints the three per cell and asserts the last at zero (v3.3 seat 5, decision D55).
     */
    public int[] declarationReconcileStats() {
        return reconciler.stats();
    }

    /**
     * The same three counters for the PARENTS the emitter resolved through the index (each reconciled once, a throw a
     * mismatch) - the D11 host prints them beside the pass's own on the ENUM line and asserts the last at zero.
     */
    public int[] parentReconcileStats() {
        return parentReconciler.stats();
    }

    @Override
    public Set<String> filesWrittenByIrEmitter() {
        return emitter.written();
    }

    /** TEST SEAM: the index, for {@code IREnumEmitterTest} to plant a lying node ({@link IREnumIndex#plant}). */
    IREnumIndex indexForTests() {
        return index;
    }

    @Override
    public List<GenerationException> generateClassesAsIR(RModel model, String version,
                                                         Map<String, String> output) {
        List<GenerationException> errors = new ArrayList<>(reconcile(model));
        errors.addAll(emit(model, version, output));
        return errors;
    }

    /**
     * Build the ENUM IR for each enumeration in the model (ONE node per declaration - the emitter reads the same
     * instance) and assert EVERY fact it carries against the source; a mismatched declaration is booked as REFUSED.
     */
    List<GenerationException> reconcile(RModel model) {   // package-private: the throw witness calls the reconcile alone
        List<GenerationException> errors = new ArrayList<>();
        String namespace = model.namespace();
        for (var element : model.rootElements()) {
            if (element instanceof REnumeration enumeration) {
                reconciler.attempt();   // counted BEFORE the adapter: a throw below is a mismatch, never a silent drop
                try {
                    IREnumNode ir = index.node(namespace, enumeration);
                    List<String> mismatches = reconciler.reconcileEnum(namespace, enumeration, ir);
                    for (String mismatch : mismatches) {
                        errors.add(new GenerationException(mismatch, null, enumeration));
                    }
                    if (!mismatches.isEmpty()) {
                        refused.add(enumeration);
                    }
                } catch (RuntimeException e) {
                    reconciler.threw();
                    refused.add(enumeration);
                    errors.add(new GenerationException(
                            "AST->IR adapter failed for enum " + enumeration.name(),
                            null, enumeration, e));
                }
            }
        }
        return errors;
    }

    /** Write every reconciled enumeration of the model from its IR node alone; a refused one is skipped (its errors stand). */
    List<GenerationException> emit(RModel model, String version, Map<String, String> output) {
        List<GenerationException> errors = new ArrayList<>();
        String namespace = model.namespace();
        for (var element : model.rootElements()) {
            if (element instanceof REnumeration enumeration && !refused.contains(enumeration)) {
                try {
                    IREnumNode ir = index.node(namespace, enumeration);
                    output.put(IREnumEmitter.outputKey(ir), emitter.emit(ir, version));
                } catch (GenerationException e) {
                    errors.add(e);
                }
            }
        }
        return errors;
    }

    /**
     * The emitter's parent lookup: the parent's node through the workspace-wide index, reconciled against its own
     * source on first sight (a parent that disagrees with its source refuses EVERY child that extends it).
     */
    private Optional<IREnumNode> resolveParent(String qualifiedName) {
        Optional<REnumeration> declaration = index.declaration(qualifiedName);
        if (declaration.isEmpty()) {
            return Optional.empty();
        }
        REnumeration parent = declaration.get();
        if (parentsReconciled.add(parent)) {
            // booked BEFORE the adapter and the reconcile run, as the pass does: a throw on the way is a mismatch that
            // REFUSES the parent for every child - never a parent read clean by the next child (round 1, cq SF-1)
            parentReconciler.attempt();
            try {
                String namespace = IREnumIndex.namespaceOf(parent);
                if (!parentReconciler.reconcileEnum(namespace, parent, index.node(namespace, parent)).isEmpty()) {
                    parentsRefused.add(parent);
                }
            } catch (RuntimeException e) {
                parentReconciler.threw();
                parentsRefused.add(parent);
                throw new GenerationException("IR enum emitter: the parent " + qualifiedName
                        + " could not be adapted or reconciled - the child is refused", null, parent, e);
            }
        }
        if (parentsRefused.contains(parent)) {
            throw new GenerationException("IR enum emitter: the parent " + qualifiedName
                    + " disagrees with its source (its reconcile mismatched or threw) - the child is refused", null, parent);
        }
        return Optional.of(index.node(IREnumIndex.namespaceOf(parent), parent));
    }
}
