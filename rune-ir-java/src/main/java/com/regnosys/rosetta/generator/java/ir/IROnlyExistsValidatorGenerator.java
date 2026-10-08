package com.regnosys.rosetta.generator.java.ir;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.GenerationException;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.object.validators.OnlyExistsValidatorGenerator;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;

/**
 * Path-2 (IR-routed) variant of {@link OnlyExistsValidatorGenerator} - the {@code ONLY_EXISTS_VALIDATOR} member of
 * the TYPE UNIT (v3.3 seat 9, PR #645 commit 12). The file is a data type's {@code <Simple>OnlyExistsValidator}: one
 * {@code .put("name", ExistenceChecker.isSet((cast) getter))} per effective member, the synthetic {@code meta}
 * excluded, ALL members including the fully unbounded ones.
 *
 * <p><b>WHY THIS CLASS EXISTS.</b> The five per-type DERIVED files are written by five separate per-kind generators,
 * each constructed directly by the plugin runner and by the D11 host; the IR route had construction seams for six
 * OTHER kinds and none for these, so on the reference route every derived file was Path-1 and no derived file could
 * ever be booked {@code newEmitter} - the unit's all-or-nothing could not switch on at all. This is the seam's other
 * end. It routes ONE member and leaves every other decision to {@link IRUnitPass}, which holds the index, the unit
 * construction, the routing law and the shadow for all six passes (ONE declaration, LAW 69).
 *
 * <p><b>SINCE COMMIT 15 THE UNIT IS AVAILABLE</b> ({@link IRTypeUnitWiring#READY_MEMBERS} names all six members
 * and {@link IRTypeUnitWiring#AVAILABLE} is true), so on the reference route every data type of every model is
 * written WHOLE by the unit - this class writes this member's file from the IR alone - or REFUSED BY NAME, in
 * which case all six of that type's files come from the INHERITED generator. Through commit 14 the unit was
 * unavailable and this class wrote not one byte of its own (PR #645 round 1 cq SF-1).
 *
 * <p>Used only when {@link IRFlag} is enabled; Path-1 is otherwise untouched.
 */
public final class IROnlyExistsValidatorGenerator extends OnlyExistsValidatorGenerator
        implements IREmittableGenerator {

    private final IRUnitPass pass;

    public IROnlyExistsValidatorGenerator(GeneratorModel generatorModel, JavaTypeTranslator typeTranslator,
            JavaTypeUtil typeUtil) {
        super(generatorModel, typeTranslator, typeUtil);
        this.pass = new IRUnitPass(generatorModel, typeUtil, IRTypeUnit.Member.ONLY_EXISTS_VALIDATOR);
    }

    @Override
    public Set<String> filesWrittenByIrEmitter() {
        return pass.filesWrittenByIrEmitter();
    }

    /** SEAM for the D11 host (read reflectively): the members the wiring declares ready, as their names. */
    public Set<String> unitReady() {
        return IRTypeUnitWiring.readyNames();
    }

    /**
     * SEAM for the D11 host (read reflectively, beside {@link #unitReady()}): the WIRING'S OWN {@code AVAILABLE}
     * switch (v3.3 seat 9, PR #645 commit 14). Ready and available are TWO facts, and the host's
     * {@code UNIT READY} line prints both - a line that said only "6 of 6" would read as a route that is on.
     */
    public boolean unitAvailable() {
        return IRTypeUnitWiring.available();
    }

    /**
     * SEAM for the D11 host (read reflectively, beside {@link #unitAvailable()}): the WIRING'S KIND-SCOPED switch
     * for the CHOICE kind (v3.3 seat 10, PR #646 commit 4). READY, AVAILABLE and CHOICE_AVAILABLE are THREE facts
     * and the host's {@code UNIT READY} line prints all three - a line that said only "6 of 6 ; AVAILABLE=true"
     * would read as a route that writes the choices - which it does since PR #646 commit 5, the kind switch ON
     * (this sentence read "and at this commit it does not" from commit 4 to commit 8; round 1 cq SF-1).
     */
    public boolean unitChoiceAvailable() {
        return IRTypeUnitWiring.choiceAvailable();
    }

    /** SEAM for the D11 host: which member's shadow {@link #unitShadowRenders()} holds. */
    public String unitShadowMember() {
        return pass.member().name();
    }

    /** SEAM for the D11 host: this member's SHADOW renders, keyed by the member's own output key. */
    public Map<String, String[]> unitShadowRenders() {
        return pass.unitShadowRenders();
    }

    /** SEAM for the D11 host: every type NAME the production path was asked for on this pass. */
    public Set<String> unitAttemptedTypes() {
        return pass.attemptedTypes();
    }

    /** SEAM for the D11 host: every type NAME the production path REFUSED on this pass. */
    public Set<String> unitRefusedTypes() {
        return pass.refusedTypes();
    }

    /** {@code {declarations attempted, facts asserted, mismatches}} of the PARENTS this pass's index resolved. */
    public int[] parentReconcileStats() {
        return pass.parentReconcileStats();
    }

    @Override
    public List<GenerationException> generateClassesAsIR(RModel model, String version,
                                                         Map<String, String> output) {
        return pass.route(streamObjects(model), model, version, output,
                this::createTypeRepresentation, this::generate);
    }
}
