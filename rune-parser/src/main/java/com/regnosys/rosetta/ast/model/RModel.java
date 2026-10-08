package com.regnosys.rosetta.ast.model;

import com.regnosys.rosetta.ast.RDefinable;
import com.regnosys.rosetta.ast.RFileHeader;
import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.RRootElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Root node of the Rune DSL typed AST, corresponding to the {@code rosettaModel}
 * grammar rule.
 *
 * <p>Every Rune DSL source file produces exactly one {@code RModel}. It carries
 * the namespace declaration, optional override flag, optional version,
 * and contains imports, qualifiable configurations, and root-level declarations.
 *
 * <p>Grammar:
 * <pre>
 * rosettaModel:
 *     fileHeader?                               // P1.4.2 H3 — file-level metadata, see U009
 *     OVERRIDE? NAMESPACE (qualifiedName | STRING) definable?
 *     rosettaScope?
 *     versionDecl?
 *     imports+=importDecl*
 *     configurations+=rosettaQualifiableConfiguration*
 *     rootElements+=rootElement*
 * ;
 * </pre>
 */
public class RModel extends RNode implements RDefinable {

    private boolean override;
    private String namespace;
    private String definition;
    private RScope scope;
    private String version;
    private final List<RImport> imports = new ArrayList<>();
    private final List<RQualifiableConfig> configurations = new ArrayList<>();
    private final List<RRootElement> rootElements = new ArrayList<>();

    // -- override -------------------------------------------------------------

    public boolean isOverride() {
        return override;
    }

    public void setOverride(boolean override) {
        checkMutable();
        this.override = override;
    }

    // -- namespace ------------------------------------------------------------

    public String namespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        checkMutable();
        this.namespace = namespace;
    }

    // -- definition (RDefinable) ----------------------------------------------

    @Override
    public Optional<String> definition() {
        return Optional.ofNullable(definition);
    }

    public void setDefinition(String definition) {
        checkMutable();
        this.definition = definition;
    }

    // -- scope ----------------------------------------------------------------

    public Optional<RScope> scope() {
        return Optional.ofNullable(scope);
    }

    public void setScope(RScope scope) {
        checkMutable();
        this.scope = scope;
    }

    // -- version --------------------------------------------------------------

    public Optional<String> version() {
        return Optional.ofNullable(version);
    }

    public void setVersion(String version) {
        checkMutable();
        this.version = version;
    }

    // -- fileHeader (P1.4.2 H3) -----------------------------------------------
    // File-level metadata block (separate from namespace-level versionDecl).

    private RFileHeader fileHeader;

    public Optional<RFileHeader> fileHeader() {
        return Optional.ofNullable(fileHeader);
    }

    public void setFileHeader(RFileHeader h) {
        checkMutable();
        this.fileHeader = h;
    }

    // -- imports --------------------------------------------------------------

    public List<RImport> imports() {
        return imports;
    }

    // -- configurations -------------------------------------------------------

    public List<RQualifiableConfig> configurations() {
        return configurations;
    }

    // -- rootElements ---------------------------------------------------------

    public List<RRootElement> rootElements() {
        return rootElements;
    }

    // -- children (for traversal) ---------------------------------------------

    @Override
    public List<? extends RNode> children() {
        List<RNode> result = new ArrayList<>();
        if (fileHeader != null) {           // P1.4.2 H3 — appears first in source
            result.add(fileHeader);
        }
        if (scope != null) {
            result.add(scope);
        }
        result.addAll(imports);
        result.addAll(configurations);
        result.addAll(rootElements);
        return List.copyOf(result);
    }
}
