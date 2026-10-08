package com.regnosys.rosetta.generator.java.ir;

import java.util.Map;

import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.object.MetaFieldGenerator;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;

/**
 * Path-2 (IR-routed) variant of the workspace-wide {@link MetaFieldGenerator}.
 *
 * <p><b>Why this is a pass-through (decision L-007).</b> Metafield wrappers
 * ({@code FieldWithMeta*} / {@code ReferenceWithMeta*}) are <em>derived,
 * deduplicated</em> types synthesised from {@code [metadata ...]} annotations on
 * attributes / choice options — they have no {@code RRootElement} and therefore
 * no declaration-IR node. The closest {@link com.regnosys.rosetta.ir.core.IRKind}
 * is {@code META_TYPE}, which is reserved for built-in meta types, not these
 * generated wrappers. Modelling derived wrapper types in the IR is deferred to a
 * later phase (it needs an IR representation of the metafield <em>spec</em>, not a
 * declaration node). So in Phase-1 this generator routes through the flag seam for
 * pipeline completeness but emits via the unchanged Path-1
 * {@link MetaFieldGenerator#generate(Map)} logic.
 *
 * <p>What this proves: that enabling the IR flag and routing the metafield
 * generator through the Path-2 seam leaves the emitted wrappers byte-identical
 * (the {@code Path2ByteIdentityTest} METAFIELD pass gates this). It does NOT yet
 * exercise an IR for metafields — that is honest, deferred scope.
 *
 * <p>Used only when {@link IRFlag} is enabled; Path-1 is otherwise untouched.
 * Lab-authored Phase-1 (not present upstream).
 */
public final class IRMetaFieldGenerator extends MetaFieldGenerator
        implements IREmittableMetaGenerator {

    public IRMetaFieldGenerator(GeneratorModel generatorModel, JavaTypeTranslator typeTranslator) {
        super(generatorModel, typeTranslator);
    }

    @Override
    public void generateAsIR(Map<String, String> output) {
        // Deliberate pass-through to the unchanged Path-1 emission — see class javadoc
        // (decision L-007). No metafield IR exists to build or reconcile in Phase-1.
        generate(output);
    }
}
