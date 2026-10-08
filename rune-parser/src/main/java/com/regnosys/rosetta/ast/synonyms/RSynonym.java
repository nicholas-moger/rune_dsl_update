package com.regnosys.rosetta.ast.synonyms;

import com.regnosys.rosetta.ast.RNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Attribute-level synonym declaration node, corresponding to the
 * {@code rosettaSynonymDecl} grammar rule.
 *
 * <p>Represents a synonym mapping such as
 * {@code [synonym FpML, DTCC value "trade" path "tradeHeader"]}.
 *
 * <p>Grammar:
 * <pre>
 * synonymDecl:
 *     LBRACKET SYNONYM sources+=qualifiedName (COMMA sources+=qualifiedName)*
 *     rosettaSynonymBody
 *     RBRACKET
 * ;
 * </pre>
 */
public class RSynonym extends RNode {

    private final List<String> sources = new ArrayList<>();
    private RSynonymBody body;

    // -- sources --------------------------------------------------------------

    public List<String> sources() {
        return sources;
    }

    // -- body -----------------------------------------------------------------

    public RSynonymBody body() {
        return body;
    }

    public void setBody(RSynonymBody body) {
        checkMutable();
        this.body = body;
    }

    // -- children (for traversal) ---------------------------------------------

    @Override
    public List<? extends RNode> children() {
        if (body != null) {
            return Collections.singletonList(body);
        }
        return Collections.emptyList();
    }
}
