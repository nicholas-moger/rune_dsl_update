package com.regnosys.rosetta.generator.java.template.model;

/**
 * Template model for a single attribute cardinality or type format check
 * within a validator generator.
 *
 * <p>For cardinality validators: {@code checkCardinality("name", cast, min, max)}.
 * For type format validators: {@code checkString()} or {@code checkNumber()} with constraints.
 * For only-exists validators: {@code ExistenceChecker.isSet(cast)}.
 */
public class ValidatorCheckModel {

    private final boolean isLast;
    private final String checkExpr;

    /**
     * A single pre-computed check expression (checkCardinality, checkString,
     * checkNumber, ExistenceChecker.isSet-put, …). Pre-computing in the generator
     * avoids angle-bracket escaping in ST4 and keeps the cast-type law
     * ({@code ValidatorScan.castType}) in ONE place; {@code isLast} drives the
     * cardinality template's {@code ", "} separator (comma + trailing space on
     * every entry but the last).
     *
     * <p>The former cardinality-specific constructor (attrName/typeName/getter/
     * min/max/isMulti) was removed at PR #405: it re-derived the list cast as
     * {@code List<X>} where the 9.83 goldens require {@code List<? extends X>}
     * for model types — the cast now comes from the POJO property surface.
     */
    public ValidatorCheckModel(String checkExpr, boolean isLast) {
        this.isLast = isLast;
        this.checkExpr = checkExpr;
    }

    public boolean getIsLast() { return isLast; }
    /** Pre-computed check expression (checkCardinality, checkString, checkNumber, etc.). */
    public String getCheckExpr() { return checkExpr; }
}
