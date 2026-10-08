package com.regnosys.rosetta.symbols.derived;

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.functions.RRule;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.util.AstWalker;
import com.regnosys.rosetta.symbols.diagnostics.Diagnostics;

/**
 * Generated input parameter rule — for any function with no explicit
 * {@code inputs} block, synthesizes a single input parameter. The
 * parameter's TYPE is left unresolved; M4 type inference fills it in.
 */
public final class GeneratedInputRule implements DerivedStateRule {

    private static final String SYNTHESIZED_NAME = "__synthesized_input__";

    /**
     * Whether {@code attr} is this rule's synthesized placeholder input —
     * fork-internal scaffolding invisible to upstream's declared-signature
     * reads. PR #454: the call-site validator filters it out of the arity
     * population (upstream {@code numberOfParameters} counts DECLARED inputs
     * only; a zero-input function must arity-check as zero).
     */
    public static boolean isSynthesized(RAttribute attr) {
        return SYNTHESIZED_NAME.equals(attr.name());
    }

    @Override
    public void apply(RModel model, Diagnostics collector) {
        for (RFunction fn : AstWalker.findAll(model, RFunction.class)) {
            if (fn.inputs().isEmpty()) {
                RAttribute synth = new RAttribute();
                synth.setName(SYNTHESIZED_NAME);
                fn.inputs().add(synth);
            }
        }
        // RRule has its own input shape via fromType()/setFromType() rather
        // than an inputs() list. When fromType() is empty, the rule body has
        // no implicit input. No structural mutation needed — M4 handles rule
        // input typing.
    }
}
