package com.regnosys.rosetta.ir.core;

import java.util.Objects;
import java.util.Optional;

/**
 * One {@code [label …]} annotation on a field or a choice option: the label text and the path it is
 * scoped to, in either of the grammar's two spellings ({@code for <path>} and the legacy {@code <path> as}).
 * The label-provider kind reads these.
 *
 * @param label   the label text
 * @param forPath the {@code for <path>} scope, when written
 * @param asPath  the legacy {@code <path> as} scope, when written
 */
public record IRLabel(String label, Optional<IRAnnotationPath> forPath, Optional<IRAnnotationPath> asPath) {
    public IRLabel {
        label = label == null ? "" : label;
        Objects.requireNonNull(forPath, "forPath");
        Objects.requireNonNull(asPath, "asPath");
    }
}
