package com.regnosys.rosetta.ast.synonyms;

import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.enums.RSynonymRef;

import java.math.BigInteger;
import java.util.Optional;

/**
 * Synonym value node, corresponding to the {@code rosettaSynonymValue}
 * grammar rule.
 *
 * <p>Represents a single value entry in a VALUE synonym body, such as
 * {@code "trade" tag 1 path "header" maps 2}.
 *
 * <p>Grammar:
 * <pre>
 * rosettaSynonymValue:
 *     name=STRING (rosettaSynonymRef refValue=INT_LITERAL)?
 *     (PATH path=STRING)?
 *     (MAPS maps=INT_LITERAL)?
 * ;
 * </pre>
 *
 * <p>{@code refValue} and {@code maps} are parsed from the grammar's
 * {@code INT_LITERAL} token, which has no upper bound. They are stored as
 * {@link BigInteger} to handle arbitrary-precision integers — the
 * {@link com.regnosys.rosetta.ast.expressions.literals.RIntLiteral} class
 * uses the same approach for the same reason.
 */
public class RSynonymValue extends RNode {

    private String name;
    private RSynonymRef ref;
    private BigInteger refValue;
    private String path;
    private BigInteger maps;

    // -- name -----------------------------------------------------------------

    public String name() {
        return name;
    }

    public void setName(String name) {
        checkMutable();
        this.name = name;
    }

    // -- ref ------------------------------------------------------------------

    public Optional<RSynonymRef> ref() {
        return Optional.ofNullable(ref);
    }

    public void setRef(RSynonymRef ref) {
        checkMutable();
        this.ref = ref;
    }

    // -- refValue -------------------------------------------------------------

    public Optional<BigInteger> refValue() {
        return Optional.ofNullable(refValue);
    }

    public void setRefValue(BigInteger refValue) {
        checkMutable();
        this.refValue = refValue;
    }

    /** Convenience overload for callers that have a plain {@code int}. */
    public void setRefValue(int refValue) {
        checkMutable();
        this.refValue = BigInteger.valueOf(refValue);
    }

    // -- path -----------------------------------------------------------------

    public Optional<String> path() {
        return Optional.ofNullable(path);
    }

    public void setPath(String path) {
        checkMutable();
        this.path = path;
    }

    // -- maps -----------------------------------------------------------------

    public Optional<BigInteger> maps() {
        return Optional.ofNullable(maps);
    }

    public void setMaps(BigInteger maps) {
        checkMutable();
        this.maps = maps;
    }

    /** Convenience overload for callers that have a plain {@code int}. */
    public void setMaps(int maps) {
        checkMutable();
        this.maps = BigInteger.valueOf(maps);
    }
}
