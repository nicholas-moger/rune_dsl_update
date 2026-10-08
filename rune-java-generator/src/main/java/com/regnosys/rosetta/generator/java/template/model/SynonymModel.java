package com.regnosys.rosetta.generator.java.template.model;

import java.util.List;

/**
 * Template model for a single enum value synonym with one or more sources.
 * CDM is the only corpus that uses synonyms on enum values.
 *
 * <p>Rendered as {@code @RosettaSynonym(value = "...", source = "...")} per source.
 *
 * <p>v3.2 seat 9 (D46, F1): the value and the sources are the RAW model text — the released 9.83.0 plugin
 * splices them into the annotation unescaped ({@code @RosettaSynonym(value = "µ–syn", source = "EdgeSrc")},
 * the oracle group {@code enum-unicode-display-edge}'s {@code SynonymEnum}); the generator used to
 * Java-escape both. A source is a synonym-source NAME (an identifier), so its escape was always a no-op —
 * the raw carry keeps the annotation seat under ONE law.
 */
public class SynonymModel {

    private final String value;
    private final List<String> sources;

    public SynonymModel(String value, List<String> sources) {
        this.value = value;
        this.sources = List.copyOf(sources);
    }

    public String getValue() { return value; }
    public List<String> getSources() { return sources; }
}
