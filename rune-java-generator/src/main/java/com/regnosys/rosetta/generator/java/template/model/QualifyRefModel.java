package com.regnosys.rosetta.generator.java.template.model;

/**
 * One {@code getQualifyFunctions} entry in an XMeta registry:
 * {@code factory.<Root>create(Qualify_X.class)} — the qualification function's
 * simple class name; the typed root is the meta's own data class
 * ({@code MetaTemplateModel.dataClassJavaType}), identical for every entry.
 * Only the qualifiable roots ({@code isEvent root} / {@code isProduct root}
 * configurations) carry a non-empty list — 4 files corpus-wide at 9.83
 * (BusinessEventMeta + EconomicTermsMeta in each cdm cell).
 * <p>v3.2 seat 11 (D50): the simple name carries the function class's first-claim SENTINEL
 * ({@code ImportCollisionResolver.typeRefOrBare}), resolved bare or canonical with the whole meta class text.
 */
public class QualifyRefModel {

    private final String simpleName;
    private final boolean isLast;

    public QualifyRefModel(String simpleName, boolean isLast) {
        this.simpleName = simpleName;
        this.isLast = isLast;
    }

    public String getSimpleName() { return simpleName; }
    public boolean getIsLast() { return isLast; }
}
