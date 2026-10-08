package com.regnosys.rosetta.ast.synonyms;

import com.regnosys.rosetta.ast.RNode;

import java.util.Optional;

/**
 * Merge synonym value node, corresponding to the {@code rosettaMergeSynonymValue}
 * grammar rule.
 *
 * <p>Represents a merge synonym such as {@code merge "trade" when path <> "excluded"}.
 *
 * <p>Grammar:
 * <pre>
 * rosettaMergeSynonymValue:
 *     name=STRING (WHEN PATH NEQ excludePath=STRING)?
 * ;
 * </pre>
 */
public class RMergeSynonymValue extends RNode {

    private String name;
    private String excludePath;

    // -- name -----------------------------------------------------------------

    public String name() {
        return name;
    }

    public void setName(String name) {
        checkMutable();
        this.name = name;
    }

    // -- excludePath ----------------------------------------------------------

    public Optional<String> excludePath() {
        return Optional.ofNullable(excludePath);
    }

    public void setExcludePath(String excludePath) {
        checkMutable();
        this.excludePath = excludePath;
    }
}
