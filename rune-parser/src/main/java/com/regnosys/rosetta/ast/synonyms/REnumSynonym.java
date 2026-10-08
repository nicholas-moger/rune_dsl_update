package com.regnosys.rosetta.ast.synonyms;

import com.regnosys.rosetta.ast.RNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Enum-level synonym declaration node, corresponding to the
 * enum synonym grammar rule.
 *
 * <p>Represents an enum value synonym such as
 * {@code [synonym FpML value "ActiveTrade" definition "Active" pattern "Active.*" "Active"]}.
 *
 * <p>Grammar:
 * <pre>
 * enumSynonymDecl:
 *     LBRACKET SYNONYM sources+=qualifiedName (COMMA sources+=qualifiedName)*
 *     VALUE value=STRING
 *     (DEFINITION definitionText=STRING)?
 *     (PATTERN patternMatch=STRING patternReplace=STRING)?
 *     removeHtml?=REMOVE_HTML
 *     RBRACKET
 * ;
 * </pre>
 */
public class REnumSynonym extends RNode {

    private final List<String> sources = new ArrayList<>();
    private String value;
    private String definitionText;
    private String patternMatch;
    private String patternReplace;
    private boolean removeHtml;

    // -- sources --------------------------------------------------------------

    public List<String> sources() {
        return sources;
    }

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

    // -- removeHtml -----------------------------------------------------------

    public boolean isRemoveHtml() {
        return removeHtml;
    }

    public void setRemoveHtml(boolean removeHtml) {
        checkMutable();
        this.removeHtml = removeHtml;
    }
}
