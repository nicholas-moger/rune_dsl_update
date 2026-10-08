package com.regnosys.rosetta.ast.model;

import com.regnosys.rosetta.ast.RNode;

import java.util.Optional;

/**
 * Import declaration node, corresponding to the {@code importDecl} grammar rule.
 *
 * <p>Grammar:
 * <pre>
 * importDecl:
 *     IMPORT qualifiedName (DOT STAR)? (AS validID)?
 * ;
 * </pre>
 */
public class RImport extends RNode {

    private String qualifiedName;
    private boolean wildcard;
    private String alias;

    // -- qualifiedName --------------------------------------------------------

    public String qualifiedName() {
        return qualifiedName;
    }

    public void setQualifiedName(String qualifiedName) {
        checkMutable();
        this.qualifiedName = qualifiedName;
    }

    // -- wildcard -------------------------------------------------------------

    public boolean isWildcard() {
        return wildcard;
    }

    public void setWildcard(boolean wildcard) {
        checkMutable();
        this.wildcard = wildcard;
    }

    // -- alias ----------------------------------------------------------------

    public Optional<String> alias() {
        return Optional.ofNullable(alias);
    }

    public void setAlias(String alias) {
        checkMutable();
        this.alias = alias;
    }
}
