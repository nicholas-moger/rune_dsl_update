package com.regnosys.rosetta.ast.types;

import com.regnosys.rosetta.ast.RDefinable;
import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.RRootElement;
import com.regnosys.rosetta.ast.functions.RCondition;
import com.regnosys.rosetta.ast.supporting.RTypeCall;
import com.regnosys.rosetta.ast.supporting.RTypeParameter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Type alias declaration node, corresponding to the {@code rosettaTypeAlias}
 * grammar rule.
 *
 * <p>Represents a {@code typeAlias Foo(params): <"definition"> typeCall}
 * declaration. The optional definable text follows the colon, before the
 * type call.
 *
 * <p>Grammar:
 * <pre>
 * rosettaTypeAlias:
 *     TYPE_ALIAS validID typeParameters? COLON definable? typeCall condition*
 * ;
 * </pre>
 */
public class RTypeAlias extends RRootElement implements RDefinable {

    private String name;
    private String definition;
    private RTypeCall typeCall;
    private final List<RTypeParameter> typeParameters = new ArrayList<>();

    private final List<RCondition> conditions = new ArrayList<>();

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

    // -- typeCall --------------------------------------------------------------

    public RTypeCall typeCall() {
        return typeCall;
    }

    public void setTypeCall(RTypeCall typeCall) {
        checkMutable();
        this.typeCall = typeCall;
    }

    // -- typeParameters -------------------------------------------------------

    public List<RTypeParameter> typeParameters() {
        return typeParameters;
    }

    // -- conditions -----------------------------------------------------------

    public List<RCondition> conditions() {
        return conditions;
    }

    // -- children (for traversal) ---------------------------------------------

    @Override
    public List<? extends RNode> children() {
        List<RNode> result = new ArrayList<>();
        result.addAll(super.children());     // hoisted runeAnnotations (P1.4.2 H2)
        result.addAll(typeParameters);
        if (typeCall != null) {
            result.add(typeCall);
        }
        result.addAll(conditions);
        return List.copyOf(result);
    }
}
