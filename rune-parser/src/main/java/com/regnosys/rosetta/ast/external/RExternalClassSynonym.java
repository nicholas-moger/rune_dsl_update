package com.regnosys.rosetta.ast.external;

import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.synonyms.RClassSynonymValue;
import com.regnosys.rosetta.ast.synonyms.RMetaSynonymValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * External class synonym node, corresponding to the
 * {@code rosettaExternalClassSynonym} grammar rule.
 *
 * <p>Represents a class-level synonym within an external class mapping,
 * with an optional value and a required meta synonym value.
 *
 * <p>Grammar:
 * <pre>
 * rosettaExternalClassSynonym:
 *     LBRACK
 *         (VALUE classSynonymValue)?
 *         META rosettaMetaSynonymValue
 *     RBRACK
 * ;
 * </pre>
 */
public class RExternalClassSynonym extends RNode {

    private RClassSynonymValue value;
    private RMetaSynonymValue meta;

    // -- value ----------------------------------------------------------------

    public Optional<RClassSynonymValue> value() {
        return Optional.ofNullable(value);
    }

    public void setValue(RClassSynonymValue value) {
        checkMutable();
        this.value = value;
    }

    // -- meta -----------------------------------------------------------------

    public RMetaSynonymValue meta() {
        return meta;
    }

    public void setMeta(RMetaSynonymValue meta) {
        checkMutable();
        this.meta = meta;
    }

    // -- children (for traversal) ---------------------------------------------

    @Override
    public List<? extends RNode> children() {
        List<RNode> result = new ArrayList<>();
        if (value != null) {
            result.add(value);
        }
        if (meta != null) {
            result.add(meta);
        }
        return List.copyOf(result);
    }
}
