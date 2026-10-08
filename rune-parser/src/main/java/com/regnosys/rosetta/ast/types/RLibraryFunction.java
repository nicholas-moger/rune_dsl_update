package com.regnosys.rosetta.ast.types;

import com.regnosys.rosetta.ast.RDefinable;
import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.RRootElement;
import com.regnosys.rosetta.ast.supporting.RParameter;
import com.regnosys.rosetta.ast.supporting.RTypeCall;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Library function declaration node, corresponding to the
 * {@code rosettaLibraryFunction} grammar rule.
 *
 * <p>Represents a {@code library function Foo(params) returnType:} declaration
 * for built-in library functions.
 *
 * <p>Grammar:
 * <pre>
 * rosettaLibraryFunction:
 *     LIBRARY FUNCTION qualifiedName
 *     LPAREN parameters+=parameter (COMMA parameters+=parameter)* RPAREN
 *     typeCall definable?
 * ;
 * </pre>
 */
public class RLibraryFunction extends RRootElement implements RDefinable {

    private String name;
    private String definition;
    private RTypeCall returnType;
    private final List<RParameter> parameters = new ArrayList<>();

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

    // -- returnType -----------------------------------------------------------

    public RTypeCall returnType() {
        return returnType;
    }

    public void setReturnType(RTypeCall returnType) {
        checkMutable();
        this.returnType = returnType;
    }

    // -- parameters -----------------------------------------------------------

    public List<RParameter> parameters() {
        return parameters;
    }

    // -- children (for traversal) ---------------------------------------------

    @Override
    public List<? extends RNode> children() {
        List<RNode> result = new ArrayList<>();
        result.addAll(super.children());     // hoisted runeAnnotations (P1.4.2 H2)
        result.addAll(parameters);
        if (returnType != null) {
            result.add(returnType);
        }
        return List.copyOf(result);
    }
}
