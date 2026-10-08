package com.regnosys.rosetta.ast.external;

import com.regnosys.rosetta.ast.RNode;

import java.util.Optional;

/**
 * External enum synonym node, corresponding to the
 * {@code rosettaExternalEnumSynonym} grammar rule.
 *
 * <p>Represents a synonym mapping for an individual enum value within
 * an external enum, with optional definition and pattern fields.
 *
 * <p>Grammar:
 * <pre>
 * rosettaExternalEnumSynonym:
 *     LBRACK VALUE STRING
 *         (DEFINITION STRING)?
 *         (PATTERN STRING STRING)?
 *     RBRACK
 * ;
 * </pre>
 *
 * <p>This is a leaf node — it has no child AST nodes.
 */
public class RExternalEnumSynonym extends RNode {

    private String value;
    private String definitionText;
    private String patternMatch;
    private String patternReplace;

    // -- value ----------------------------------------------------------------

    public String value() {
        return value;
    }

    public void setValue(String value) {
        checkMutable();
        this.value = value;
    }

    // -- definitionText -------------------------------------------------------

    public Optional<String> definitionText() {
        return Optional.ofNullable(definitionText);
    }

    public void setDefinitionText(String definitionText) {
        checkMutable();
        this.definitionText = definitionText;
    }

    // -- patternMatch ---------------------------------------------------------

    public Optional<String> patternMatch() {
        return Optional.ofNullable(patternMatch);
    }

    public void setPatternMatch(String patternMatch) {
        checkMutable();
        this.patternMatch = patternMatch;
    }

    // -- patternReplace -------------------------------------------------------

    public Optional<String> patternReplace() {
        return Optional.ofNullable(patternReplace);
    }

    public void setPatternReplace(String patternReplace) {
        checkMutable();
        this.patternReplace = patternReplace;
    }
}
