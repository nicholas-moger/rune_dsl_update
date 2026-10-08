package com.regnosys.rosetta.ast.synonyms;

import com.regnosys.rosetta.ast.RRootElement;

/**
 * Synonym source declaration node, corresponding to the {@code rosettaSynonymSource}
 * grammar rule.
 *
 * <p>Declares a named synonym source (e.g., {@code synonym source FpML}).
 * This is a top-level (root) element in the model.
 *
 * <p>Grammar:
 * <pre>
 * rosettaSynonymSource:
 *     SYNONYM SOURCE qualifiedName
 * ;
 * </pre>
 */
public class RSynonymSource extends RRootElement {

    private String name;

    // -- name -----------------------------------------------------------------

    public String name() {
        return name;
    }

    public void setName(String name) {
        checkMutable();
        this.name = name;
    }
}
