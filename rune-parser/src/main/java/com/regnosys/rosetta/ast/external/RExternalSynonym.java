package com.regnosys.rosetta.ast.external;

import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.synonyms.RSynonymBody;

import java.util.List;

/**
 * External synonym node, corresponding to the
 * {@code rosettaExternalSynonym} grammar rule.
 *
 * <p>Wraps a {@link RSynonymBody} for use within an external attribute mapping.
 *
 * <p>Grammar:
 * <pre>
 * rosettaExternalSynonym:
 *     LBRACK rosettaSynonymBody RBRACK
 * ;
 * </pre>
 */
public class RExternalSynonym extends RNode {

    private RSynonymBody body;

    // -- body -----------------------------------------------------------------

    public RSynonymBody body() {
        return body;
    }

    public void setBody(RSynonymBody body) {
        checkMutable();
        this.body = body;
    }

    // -- children (for traversal) ---------------------------------------------

    /**
     * Returns an unmodifiable singleton list containing the body when present,
     * or an empty list otherwise. Avoids allocating a mutable {@code ArrayList}
     * for a single child, per the {@link RNode#children()} contract.
     */
    @Override
    public List<? extends RNode> children() {
        return body != null ? List.of(body) : List.of();
    }
}
