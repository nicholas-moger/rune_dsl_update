package com.regnosys.rosetta.generator.java.ir;

import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.expression.ExpressionCompiler;
import com.regnosys.rosetta.generator.java.function.FunctionExpressionRenderer;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;

/**
 * Path-2 (IR-routed) variant of {@link FunctionGenerator}. It reuses the entire Path-1
 * function pipeline (templates, dependency collection, alias handling, statement
 * rendering) unchanged, swapping in only an {@link IRExpressionCompiler} via the
 * {@link FunctionGenerator#createExpressionCompiler()} seam — so expression emission can
 * migrate to the expression IR behaviour-by-behaviour while everything else stays
 * byte-identical (decision L-001/L-002).
 *
 * <p>In Wave 0 the substituted compiler overrides nothing, so this generator's output is
 * byte-for-byte identical to {@code FunctionGenerator}'s — the transparent-harness proof
 * that the IR routing is invisible before any behaviour is flipped.
 *
 * <p>Used by the lab's {@code FUNCTION} byte gate and (in later phases) by
 * {@code JavaCodeGenerator} when the IR flag is enabled; Path-1 is otherwise untouched.
 * Lab-authored Phase-2 (not present upstream).
 */
public class IRFunctionGenerator extends FunctionGenerator {

    public IRFunctionGenerator(GeneratorModel generatorModel, JavaTypeTranslator typeTranslator,
                               JavaTypeUtil typeUtil) {
        super(generatorModel, typeTranslator, typeUtil);
    }

    /**
     * Substitutes the IR-aware compiler. Invoked from the superclass constructor, so it
     * reads only the inherited {@code generatorModel}/{@code typeTranslator}/{@code typeUtil}
     * (assigned before the call) and no field of this subclass — see
     * {@link FunctionGenerator#createExpressionCompiler()} for the invariant.
     */
    @Override
    protected ExpressionCompiler createExpressionCompiler() {
        return new IRExpressionCompiler(generatorModel, typeTranslator, typeUtil);
    }

    /**
     * Substitutes the IR-driven renderer (the Wave-6 Phase C seam). Invoked from the superclass
     * constructor, so it uses only its {@code compiler} argument (per the
     * {@link FunctionGenerator#createFunctionExpressionRenderer} construction-time invariant).
     *
     * <p>Today the subclass is <strong>behaviour-transparent</strong> — it delegates every render to
     * {@code super}, so Path-2 output stays byte-for-byte identical to Path-1 and the byte gate is unmoved.
     * This is the byte-inert architectural unblock (Phase C "Step 0") that proves the IR generator can
     * supply its own renderer; the per-family SET-position hoist overrides land in later Phase-C slices
     * (see {@code notes/wave6-anf-phaseC-feasibility.md}).
     */
    @Override
    protected FunctionExpressionRenderer createFunctionExpressionRenderer(ExpressionCompiler compiler) {
        return new IRFunctionExpressionRenderer(compiler);
    }

    /**
     * The IR-aware compiler that renders this generator's function BODIES — the one whose §4.2
     * instrumentation counters (IR-driven / declined expression nodes, and witness/output collision
     * FQN-inline renders) reflect the bytes the FUNCTION byte gate verifies. Accumulated across every
     * function generated in this run; read by {@code Path2ByteIdentityTest} for the IR-driven-share metric.
     */
    public IRExpressionCompiler irExpressionCompiler() {
        return (IRExpressionCompiler) renderingExpressionCompiler();
    }

    /**
     * The IR-routed renderer ({@link IRFunctionExpressionRenderer}) that drives this generator's SET-position
     * statement hoists — exposed so the FUNCTION byte gate can read its Wave-6 Phase-C hoist-driving counters
     * (e.g. {@link IRFunctionExpressionRenderer#ifThenElseResultDrivenCount()}, the slice-1 firing breadth).
     */
    public IRFunctionExpressionRenderer irFunctionExpressionRenderer() {
        return (IRFunctionExpressionRenderer) functionExpressionRenderer();
    }
}
