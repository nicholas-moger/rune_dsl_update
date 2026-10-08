package com.regnosys.rosetta.ast.types;

import com.regnosys.rosetta.ast.RDefinable;
import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.RRootElement;
import com.regnosys.rosetta.ast.supporting.RRecordFeature;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Built-in record type declaration node, corresponding to the
 * {@code rosettaRecordType} grammar rule.
 *
 * <p>Represents a {@code recordType Foo:} declaration with record features.
 *
 * <p>Grammar:
 * <pre>
 * rosettaRecordType:
 *     RECORD_TYPE qualifiedName LBRACE definable?
 *     features+=recordFeature*
 *     RBRACE
 * ;
 * </pre>
 */
public class RRecordType extends RRootElement implements RDefinable {

    private String name;
    private String definition;
    private final List<RRecordFeature> features = new ArrayList<>();

    // -- name -----------------------------------------------------------------

    public String name() {
        return name;
    }

    public void setName(String name) {
        checkMutable();
        this.name = name;
    }

    // -- definition (RDefinable) ----------------------------------------------

    @Override
    public Optional<String> definition() {
        return Optional.ofNullable(definition);
    }

    public void setDefinition(String definition) {
        checkMutable();
        this.definition = definition;
    }

    // -- features -------------------------------------------------------------

    public List<RRecordFeature> features() {
        return features;
    }

    // -- children (for traversal) ---------------------------------------------

    @Override
    public List<? extends RNode> children() {
        List<RNode> result = new ArrayList<>();
        result.addAll(super.children());     // hoisted runeAnnotations (P1.4.2 H2)
        result.addAll(features);
        return List.copyOf(result);
    }
}
