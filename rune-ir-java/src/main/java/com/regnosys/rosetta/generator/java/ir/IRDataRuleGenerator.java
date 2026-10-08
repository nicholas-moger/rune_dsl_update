package com.regnosys.rosetta.generator.java.ir;

import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.expression.ExpressionCompiler;
import com.regnosys.rosetta.generator.java.function.FunctionExpressionRenderer;
import com.regnosys.rosetta.generator.java.object.datarule.DataRuleGenerator;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;

/**
 * Path-2 (IR-routed) variant of {@link DataRuleGenerator} — THE DATA_RULE SEAM (v3.3 seat 3, D54). It
 * reuses the entire Path-1 condition-class pipeline (the template, the {@code NAME} / {@code DEFINITION}
 * constants, the {@code Default} / {@code NoOp} classes, the dependency collection, the import resolution, the
 * cardinality twins) unchanged, swapping in only an {@link IRExpressionCompiler} and an
 * {@link IRFunctionExpressionRenderer} via the {@link DataRuleGenerator#createExpressionCompiler()} /
 * {@link DataRuleGenerator#createFunctionExpressionRenderer} seams — the {@link IRFunctionGenerator} file, one
 * kind over — so every condition BODY is compiled through the IR compiler's claim seats and the D11's DATA_RULE
 * seam reads the same counters the FUNCTION and RULE seams read.
 *
 * <p><b>Byte identity, in three classes (D54 item 1).</b> A DECLINED claim renders through the legacy handler
 * the IR compiler overrides ({@code super.visitX}) — identical BY IDENTITY; an ORACLE-LEAF-bearing root (the
 * bare attribute reference of a condition body is an {@code IRImplicitAttrNav}, a {@code containsOracleLeaf}
 * member since #528) renders whole through the same legacy call — BY IDENTITY, the at-root bare attribute
 * served by {@code emitterGapServe}'s {@code RSymbolReference} leg; a FULLY-LOWERED non-oracle root (a boolean
 * literal, a navigation-free equality) renders NATIVELY through the leaf emitter, where identity is the emitter's,
 * ring-proven at the FUNCTION and RULE seams for the same shapes and proven for this seam by the rings. The IR
 * compiler's handler overrides on the data-rule path — {@link IRCollectionHandler#thenArgBaseName} on a
 * then-chain rung, {@link IRLiteralHandler#bigIntegerBaseName} on a bigInteger literal,
 * {@link IRControlFlowHandler#ifThenElseResultBaseName} on the seat-agnostic item-local conditional arm — each
 * return the legacy constant or throw on divergence (R5) under an R3 catch. The cardinality TWIN's root is an
 * untargeted visit ({@code visitCardinalityCheck} records and delegates); its synthesized receiver re-enters the
 * share as one claim per twin.
 *
 * <p>Fork-authored (v3.3 seat 3); not present upstream.
 */
public class IRDataRuleGenerator extends DataRuleGenerator {

    public IRDataRuleGenerator(GeneratorModel generatorModel, JavaTypeTranslator typeTranslator,
                               JavaTypeUtil typeUtil) {
        super(generatorModel, typeTranslator, typeUtil);
    }

    /**
     * Substitutes the IR-aware compiler. Invoked from the superclass constructor, so it reads only the
     * inherited {@code generatorModel} / {@code typeTranslator} / {@code typeUtil} (assigned before the call)
     * and no field of this subclass — see {@link DataRuleGenerator#createExpressionCompiler()} for the invariant.
     */
    @Override
    protected ExpressionCompiler createExpressionCompiler() {
        return new IRExpressionCompiler(generatorModel, typeTranslator, typeUtil);
    }

    /**
     * Substitutes the IR-driven renderer (the {@link IRFunctionGenerator} shape). Its four overrides sit on the
     * SET-position / alias paths a condition body never reaches (the data-rule ladder registers its boolean
     * hoist directly), so on this kind the renderer is behaviour-transparent by construction; it is substituted
     * so the two IR-routed generators carry ONE renderer class and a later condition-side drive has its seat.
     */
    @Override
    protected FunctionExpressionRenderer createFunctionExpressionRenderer(ExpressionCompiler compiler) {
        return new IRFunctionExpressionRenderer(compiler);
    }

    /**
     * The IR-aware compiler that renders this generator's condition BODIES — the one whose counters (IR-driven /
     * declined claims, the site and family breakdowns, the untargeted visits) the D11's DATA_RULE seam reads and
     * asserts against the register. Accumulated across every condition class generated in this run.
     */
    public IRExpressionCompiler irExpressionCompiler() {
        return (IRExpressionCompiler) renderingExpressionCompiler();
    }
}
