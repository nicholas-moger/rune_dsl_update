package com.regnosys.rosetta.ir.adapter;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.regnosys.rosetta.ir.core.IRFunctionSignature;
import com.regnosys.rosetta.ir.core.IRKind;
import com.regnosys.rosetta.ir.core.IRModel;
import com.regnosys.rosetta.ir.core.IRNode;
import com.regnosys.rosetta.ir.core.IRQualifiableConfig;
import com.regnosys.rosetta.ir.core.IRQualificationFunction;
import com.regnosys.rosetta.ir.core.IRWithMetaUse;
import com.regnosys.rosetta.ir.core.Metadata;
import com.regnosys.rosetta.ir.core.SourceRange;

/**
 * The adapter's {@link IRModel} (v3.3 seat 8, PR #644 - the property gate): the facts of ONE {@code namespace}
 * declaration that the data-type emitter's derived files read and no type node carries. {@link #name()} IS the
 * namespace. {@link #children()} surfaces nothing: the model's declarations are the workspace's own nodes, adapted and
 * reconciled each through its own pass - this node states the MODEL-level facts only.
 *
 * <p>Every list is copied defensively; every {@code Optional} is required non-null (the record's law, as on every
 * declaration node).
 */
public record IRModelNode(String name, Optional<String> definition, Optional<String> version,
                          List<IRQualifiableConfig> qualifiableConfigs,
                          List<IRQualificationFunction> qualificationFunctions,
                          List<IRFunctionSignature> functionSignatures,
                          List<IRWithMetaUse> withMetaUses,
                          Optional<SourceRange> sourceRange, Metadata metadata)
        implements IRModel {

    public IRModelNode {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(version, "version");
        qualifiableConfigs = List.copyOf(qualifiableConfigs);
        qualificationFunctions = List.copyOf(qualificationFunctions);
        functionSignatures = List.copyOf(functionSignatures);
        withMetaUses = List.copyOf(withMetaUses);
        Objects.requireNonNull(sourceRange, "sourceRange");
        Objects.requireNonNull(metadata, "metadata");
        if (name.isBlank()) {
            throw new IllegalArgumentException("a model node needs its namespace as its name");
        }
    }

    /** A model node with every optional fact at its empty default. */
    public IRModelNode(String namespace) {
        this(namespace, Optional.empty(), Optional.empty(), List.of(), List.of(), List.of(), List.of(),
                Optional.empty(), IRMetadata.EMPTY);
    }

    @Override
    public IRKind kind() {
        return IRKind.MODEL;
    }

    @Override
    public String namespace() {
        return name;
    }

    @Override
    public List<? extends IRNode> children() {
        return List.of();
    }
}
