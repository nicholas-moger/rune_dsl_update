package com.regnosys.rosetta.ir.core;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * One {@code [synonym …]} written on an enum value: the external value it stands for and the synonym
 * sources it belongs to. The enum kind renders one {@code @RosettaSynonym} per (value, source).
 *
 * @param sources        the synonym sources, in source order
 * @param value          the external value, as written (never escaped)
 * @param definition     the synonym's own {@code definition} text
 * @param patternMatch   the {@code pattern} match half
 * @param patternReplace the {@code pattern} replace half
 * @param removeHtml     true when the synonym is marked {@code removeHtml}
 */
public record IREnumSynonym(List<String> sources, String value, Optional<String> definition,
                            Optional<String> patternMatch, Optional<String> patternReplace, boolean removeHtml) {
    public IREnumSynonym {
        sources = List.copyOf(sources);
        value = value == null ? "" : value;
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(patternMatch, "patternMatch");
        Objects.requireNonNull(patternReplace, "patternReplace");
    }
}
