package com.regnosys.rosetta.ir.core;

import java.util.List;
import java.util.Objects;

/**
 * One {@code isEvent root X} / {@code isProduct root X} declaration of a model (v3.3 seat 8, PR #644): the kind
 * token and the root as a type REFERENCE (kind, namespace, resolved name - empty when the linker did not resolve it).
 * Which declaration WINS its kind over the workspace is the index's law, not this record's.
 *
 * @param kind     {@code IS_EVENT} or {@code IS_PRODUCT} - the parser's own token
 * @param rootType the declared root as a reference
 */
public record IRQualifiableConfig(String kind, IRType rootType) {

    /** The two tokens the grammar admits. */
    public static final List<String> KINDS = List.of("IS_EVENT", "IS_PRODUCT");

    public IRQualifiableConfig {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(rootType, "rootType");
        if (!KINDS.contains(kind)) {
            throw new IllegalArgumentException("a qualifiable configuration's kind is one of " + KINDS + ", not '" + kind + "'");
        }
    }
}
