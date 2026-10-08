package com.regnosys.rosetta.ast.regulatory;

import com.regnosys.rosetta.ast.RDefinable;
import com.regnosys.rosetta.ast.RRootElement;

import java.util.Optional;

/**
 * Regulatory body declaration node, corresponding to the {@code rosettaBody}
 * grammar rule.
 *
 * <p>Represents a body declaration such as
 * {@code body Authority CFTC <"Commodity Futures Trading Commission">}.
 *
 * <p>Grammar:
 * <pre>
 * rosettaBody:
 *     BODY bodyType=ID validID definable?
 * ;
 * </pre>
 */
public class RBody extends RRootElement implements RDefinable {

    private String bodyTypeKeyword;
    private String name;
    private String definition;

    // -- bodyTypeKeyword -------------------------------------------------------

    /**
     * Returns the body type keyword (e.g., "Authority", "Standard").
     */
    public String bodyTypeKeyword() {
        return bodyTypeKeyword;
    }

    public void setBodyTypeKeyword(String bodyTypeKeyword) {
        checkMutable();
        this.bodyTypeKeyword = bodyTypeKeyword;
    }

    // -- name -----------------------------------------------------------------

    public String name() {
        return name;
    }

    public void setName(String name) {
        checkMutable();
        this.name = name;
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
}
