package com.regnosys.rosetta.ast.synonyms;

import com.regnosys.rosetta.ast.RNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Class-level synonym declaration node, corresponding to the
 * {@code classSynonym} grammar rule.
 *
 * <p>Represents a class-level synonym such as
 * {@code [synonym FpML value "Trade" meta "tradeHeader"]}.
 *
 * <p>Grammar:
 * <pre>
 * classSynonym:
 *     LBRACKET SYNONYM sources+=qualifiedName (COMMA sources+=qualifiedName)*
 *     (VALUE classSynonymValue)?
 *     (META rosettaMetaSynonymValue)?
 *     RBRACKET
 * ;
 * </pre>
 */
public class RClassSynonym extends RNode {

    private final List<String> sources = new ArrayList<>();
    private RClassSynonymValue value;
    private RMetaSynonymValue meta;

    // -- sources --------------------------------------------------------------

    public List<String> sources() {
        return sources;
    }

    // -- value ----------------------------------------------------------------

    public Optional<RClassSynonymValue> value() {
        return Optional.ofNullable(value);
    }

    public void setValue(RClassSynonymValue value) {
        checkMutable();
        this.value = value;
    }

    // -- meta -----------------------------------------------------------------

    public Optional<RMetaSynonymValue> meta() {
        return Optional.ofNullable(meta);
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
