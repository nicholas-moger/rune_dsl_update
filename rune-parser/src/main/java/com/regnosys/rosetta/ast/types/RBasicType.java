package com.regnosys.rosetta.ast.types;

import com.regnosys.rosetta.ast.RDefinable;
import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.RRootElement;
import com.regnosys.rosetta.ast.supporting.RTypeParameter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Built-in basic type declaration node, corresponding to the
 * {@code rosettaBasicType} grammar rule.
 *
 * <p>Represents a {@code basicType Foo(params):} declaration for primitive
 * types like {@code string}, {@code number}, {@code int}, {@code boolean}.
 *
 * <p>Grammar:
 * <pre>
 * rosettaBasicType:
 *     BASIC_TYPE qualifiedName
 *     (LPAREN typeParameters+=typeParameter (COMMA typeParameters+=typeParameter)* RPAREN)?
 *     definable?
 * ;
 * </pre>
 */
public class RBasicType extends RRootElement implements RDefinable {

    private String name;
    private String definition;
    private final List<RTypeParameter> typeParameters = new ArrayList<>();

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

    // -- typeParameters -------------------------------------------------------

    public List<RTypeParameter> typeParameters() {
        return typeParameters;
    }

    // -- children (for traversal) ---------------------------------------------

    @Override
    public List<? extends RNode> children() {
        List<RNode> result = new ArrayList<>();
        result.addAll(super.children());     // hoisted runeAnnotations (P1.4.2 H2)
        result.addAll(typeParameters);
        return List.copyOf(result);
    }
}
