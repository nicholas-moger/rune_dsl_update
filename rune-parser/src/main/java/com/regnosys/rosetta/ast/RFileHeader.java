package com.regnosys.rosetta.ast;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * File-level header block (P1.4.2 H3).
 *
 * <p>Grammar:
 * <pre>
 * fileHeader:
 *     FILE_HEADER COLON
 *     versionField?
 *     dependsOnField?
 *     experimentalField?
 * ;
 * </pre>
 *
 * <p>Distinct from {@code versionDecl} (namespace-level version).
 * {@link #version()} carries file-level metadata;
 * {@link com.regnosys.rosetta.ast.model.RModel#version()} carries the
 * namespace-level version. Both co-exist.
 *
 * <p>Layer-1 only (P1.4.2). Fields are parsed and stored but carry no runtime
 * semantics until W13 feature-registry lands in a future sub-phase.
 */
public class RFileHeader extends RNode {

    private String version;
    private final List<String> dependsOn = new ArrayList<>();
    private final List<String> experimental = new ArrayList<>();

    // -- version --------------------------------------------------------------

    public Optional<String> version() {
        return Optional.ofNullable(version);
    }

    public void setVersion(String v) {
        checkMutable();
        this.version = v;
    }

    // -- dependsOn ------------------------------------------------------------

    public List<String> dependsOn() {
        return dependsOn;
    }

    // -- experimental ---------------------------------------------------------

    public List<String> experimental() {
        return experimental;
    }

    // -- children (for traversal) ---------------------------------------------

    @Override
    public List<? extends RNode> children() {
        return List.of();
    }
}
