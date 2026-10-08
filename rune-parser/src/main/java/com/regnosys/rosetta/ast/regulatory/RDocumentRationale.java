package com.regnosys.rosetta.ast.regulatory;

import com.regnosys.rosetta.ast.RNode;

import java.util.Optional;

/**
 * Document rationale node, corresponding to the {@code documentRationale}
 * grammar rule.
 *
 * <p>Represents rationale and/or rationale-author text within a doc reference,
 * such as {@code rationale "Required for reporting"} or
 * {@code rationale_author "ISDA"}.
 *
 * <p>Grammar:
 * <pre>
 * documentRationale:
 *     (RATIONALE rationale=STRING)?
 *     (RATIONALE_AUTHOR rationaleAuthor=STRING)?
 * ;
 * </pre>
 */
public class RDocumentRationale extends RNode {

    private String rationale;
    private String rationaleAuthor;

    // -- rationale ------------------------------------------------------------

    /**
     * Returns the optional rationale text.
     */
    public Optional<String> rationale() {
        return Optional.ofNullable(rationale);
    }

    public void setRationale(String rationale) {
        checkMutable();
        this.rationale = rationale;
    }

    // -- rationaleAuthor ------------------------------------------------------

    /**
     * Returns the optional rationale author text.
     */
    public Optional<String> rationaleAuthor() {
        return Optional.ofNullable(rationaleAuthor);
    }

    public void setRationaleAuthor(String rationaleAuthor) {
        checkMutable();
        this.rationaleAuthor = rationaleAuthor;
    }
}
