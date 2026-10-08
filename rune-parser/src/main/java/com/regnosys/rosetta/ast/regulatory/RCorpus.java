package com.regnosys.rosetta.ast.regulatory;

import com.regnosys.rosetta.ast.RDefinable;
import com.regnosys.rosetta.ast.RRootElement;

import java.util.Optional;

/**
 * Regulatory corpus declaration node, corresponding to the {@code rosettaCorpus}
 * grammar rule.
 *
 * <p>Represents a corpus declaration such as
 * {@code corpus Regulation MiFIR_RTS "MiFIR RTS" ESMA <"...">}.
 *
 * <p>Grammar:
 * <pre>
 * rosettaCorpus:
 *     CORPUS corpusType=ID body=[RosettaBody|qualifiedName]?
 *     (displayName=STRING)? validID definable?
 * ;
 * </pre>
 */
public class RCorpus extends RRootElement implements RDefinable {

    private String corpusTypeKeyword;
    private String bodyRef;
    private String displayName;
    private String name;
    private String definition;

    // -- corpusTypeKeyword -----------------------------------------------------

    /**
     * Returns the corpus type keyword (e.g., "Regulation", "Directive").
     */
    public String corpusTypeKeyword() {
        return corpusTypeKeyword;
    }

    public void setCorpusTypeKeyword(String corpusTypeKeyword) {
        checkMutable();
        this.corpusTypeKeyword = corpusTypeKeyword;
    }

    // -- bodyRef --------------------------------------------------------------

    /**
     * Returns the optional reference to a regulatory body (qualifiedName).
     */
    public Optional<String> bodyRef() {
        return Optional.ofNullable(bodyRef);
    }

    public void setBodyRef(String bodyRef) {
        checkMutable();
        this.bodyRef = bodyRef;
    }

    // -- displayName ----------------------------------------------------------

    /**
     * Returns the optional display name (STRING literal).
     */
    public Optional<String> displayName() {
        return Optional.ofNullable(displayName);
    }

    public void setDisplayName(String displayName) {
        checkMutable();
        this.displayName = displayName;
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
