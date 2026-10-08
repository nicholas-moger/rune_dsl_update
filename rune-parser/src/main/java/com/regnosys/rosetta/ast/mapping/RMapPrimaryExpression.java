package com.regnosys.rosetta.ast.mapping;

import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.enums.MapPrimaryKind;

import java.math.BigInteger;
import java.util.Optional;

/**
 * Primary expression in mapping logic, corresponding to the
 * {@code rosettaMapPrimaryExpression} grammar rule.
 *
 * <p>Can be one of: enum value reference, string literal, boolean,
 * integer, or decimal.
 *
 * <p>Grammar:
 * <pre>
 * rosettaMapPrimaryExpression:
 *     enumValueReference                (ENUM_VALUE)
 *   | STRING                            (STRING)
 *   | TRUE | FALSE                      (BOOLEAN)
 *   | INT_LITERAL                       (INT)
 *   | BIG_DECIMAL                       (DECIMAL)
 * ;
 * </pre>
 *
 * <p>{@code intValue} is parsed from the grammar's {@code INT_LITERAL}
 * token, which has no upper bound. Stored as {@link BigInteger} to handle
 * arbitrary-precision integers — the
 * {@link com.regnosys.rosetta.ast.expressions.literals.RIntLiteral} class
 * uses the same approach for the same reason.
 */
public class RMapPrimaryExpression extends RNode {

    private MapPrimaryKind kind;
    private String enumRef;
    private String stringValue;
    private Boolean boolValue;
    private BigInteger intValue;
    private String decimalValue;

    // -- kind -----------------------------------------------------------------

    public MapPrimaryKind kind() {
        return kind;
    }

    public void setKind(MapPrimaryKind kind) {
        checkMutable();
        this.kind = kind;
    }

    // -- enumRef (ENUM_VALUE kind) --------------------------------------------

    public Optional<String> enumRef() {
        return Optional.ofNullable(enumRef);
    }

    public void setEnumRef(String enumRef) {
        checkMutable();
        this.enumRef = enumRef;
    }

    // -- stringValue (STRING kind) --------------------------------------------

    public Optional<String> stringValue() {
        return Optional.ofNullable(stringValue);
    }

    public void setStringValue(String stringValue) {
        checkMutable();
        this.stringValue = stringValue;
    }

    // -- boolValue (BOOLEAN kind) ---------------------------------------------

    public Optional<Boolean> boolValue() {
        return Optional.ofNullable(boolValue);
    }

    public void setBoolValue(Boolean boolValue) {
        checkMutable();
        this.boolValue = boolValue;
    }

    // -- intValue (INT kind) --------------------------------------------------

    public Optional<BigInteger> intValue() {
        return Optional.ofNullable(intValue);
    }

    public void setIntValue(BigInteger intValue) {
        checkMutable();
        this.intValue = intValue;
    }

    /** Convenience overload for callers that have a plain {@code int}. */
    public void setIntValue(int intValue) {
        checkMutable();
        this.intValue = BigInteger.valueOf(intValue);
    }

    // -- decimalValue (DECIMAL kind) ------------------------------------------

    public Optional<String> decimalValue() {
        return Optional.ofNullable(decimalValue);
    }

    public void setDecimalValue(String decimalValue) {
        checkMutable();
        this.decimalValue = decimalValue;
    }
}
