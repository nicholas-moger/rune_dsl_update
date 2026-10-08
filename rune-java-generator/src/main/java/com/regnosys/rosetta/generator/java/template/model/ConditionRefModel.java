package com.regnosys.rosetta.generator.java.template.model;

/**
 * Template model for a single condition (data rule) reference within a Meta class.
 * Used by {@code java-meta.stg} to render {@code factory.<instanceType>create(simpleName.class)}.
 * <p>v3.2 seat 11 (D50): {@code simpleName} carries the condition class's first-claim SENTINEL
 * ({@code ImportCollisionResolver.typeRefOrBare}), resolved bare or canonical with the whole meta class text.
 */
public class ConditionRefModel {

    private final String simpleName;
    private final String instanceType;
    private final String fqn;
    private final boolean isLast;

    public ConditionRefModel(String simpleName, String instanceType, String fqn, boolean isLast) {
        this.simpleName = simpleName;
        this.instanceType = instanceType;
        this.fqn = fqn;
        this.isLast = isLast;
    }

    public String getSimpleName() { return simpleName; }
    public String getInstanceType() { return instanceType; }
    public String getFqn() { return fqn; }
    public boolean getIsLast() { return isLast; }
}
