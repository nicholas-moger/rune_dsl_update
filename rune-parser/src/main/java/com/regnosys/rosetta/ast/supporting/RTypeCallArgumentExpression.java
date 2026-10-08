package com.regnosys.rosetta.ast.supporting;

import com.regnosys.rosetta.ast.RNode;

import java.util.Optional;

/**
 * Type call argument expression node, corresponding to the
 * {@code typeCallArgumentExpression} grammar rule.
 *
 * <p>Represents the value side of a type call argument. Can be a named reference
 * (typeParameterValidID) or a literal value. The MINUS negation prefix applies
 * only to the literal alternative.
 *
 * <p>Grammar:
 * <pre>
 * typeCallArgumentExpression:
 *     typeParameterValidID
 *   | MINUS? literal
 * ;
 * </pre>
 */
public class RTypeCallArgumentExpression extends RNode {

    private boolean negated;
    private String nameValue;
    private String literalValue;

    // -- negated --------------------------------------------------------------

    public boolean isNegated() {
        return negated;
    }

    public void setNegated(boolean negated) {
        checkMutable();
        this.negated = negated;
    }

    // -- nameValue ------------------------------------------------------------

    public Optional<String> nameValue() {
        return Optional.ofNullable(nameValue);
    }

    public void setNameValue(String nameValue) {
        checkMutable();
        this.nameValue = nameValue;
    }

    // -- literalValue ---------------------------------------------------------

    public Optional<String> literalValue() {
        return Optional.ofNullable(literalValue);
    }

    public void setLiteralValue(String literalValue) {
        checkMutable();
        this.literalValue = literalValue;
    }
}
