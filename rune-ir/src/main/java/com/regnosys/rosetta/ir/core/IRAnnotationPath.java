package com.regnosys.rosetta.ir.core;

import java.util.List;
import java.util.Objects;

/**
 * The attribute path an annotation is scoped to - the {@code for a -> b} of a doc reference, a label or a
 * rule reference, and the {@code a -> b as} of a label: a root (or the {@code item} keyword) and its steps.
 *
 * @param rootItem true when the path starts at the {@code item} keyword ({@link #root()} is then empty)
 * @param root     the root attribute's name
 * @param steps    the {@code ->} / {@code ->>} steps in order
 */
public record IRAnnotationPath(boolean rootItem, String root, List<Step> steps) {

    public IRAnnotationPath {
        root = root == null ? "" : root;
        steps = List.copyOf(steps);
    }

    /** One step: {@code -> name} or, when {@link #deep()}, {@code ->> name}. */
    public record Step(String name, boolean deep) {
        public Step {
            Objects.requireNonNull(name, "name");
        }
    }

    /** The model's own spelling: {@code item -> a ->> b}. */
    public String display() {
        StringBuilder sb = new StringBuilder(rootItem ? "item" : root);
        for (Step step : steps) {
            sb.append(step.deep() ? " ->> " : " -> ").append(step.name());
        }
        return sb.toString();
    }
}
