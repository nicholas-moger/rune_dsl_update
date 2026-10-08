package com.regnosys.rosetta.validation;

import com.regnosys.rosetta.ast.RRootElement;
import com.regnosys.rosetta.ast.model.RModel;

import java.util.List;

/**
 * Base interface for all M6 semantic validators. Each validator checks
 * a root element and its descendants for semantic correctness.
 *
 * <p>Implementations use {@link com.regnosys.rosetta.ast.util.AstWalker}
 * internally to find specific node types within the subtree.
 *
 * <p>Spec: D1 in {@code docs/specs/2026-04-09-m6-validation-design.md}.
 */
public interface Validator {
    void validate(RRootElement element, ValidationCollector collector);

    /**
     * v3.2 seat 4 (PR #625, F10): the pass hands every validator the models it runs over, in LOAD
     * ORDER, before any element is validated — a validator whose check spans models (the qualifiable
     * root is a first-wins over the whole set) reads them here; the default keeps nothing.
     */
    default void bindModels(List<RModel> modelsInLoadOrder) {
    }
}
