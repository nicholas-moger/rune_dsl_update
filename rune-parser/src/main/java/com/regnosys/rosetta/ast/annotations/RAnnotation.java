package com.regnosys.rosetta.ast.annotations;

import com.regnosys.rosetta.ast.RDefinable;
import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.RRootElement;
import com.regnosys.rosetta.ast.supporting.RAttribute;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Annotation declaration node, corresponding to the {@code annotationDecl}
 * grammar rule.
 *
 * <p>Represents a top-level annotation declaration such as:
 * <pre>
 * annotation metadata: [prefix myPrefix]
 *     &lt;"Describes metadata annotations."&gt;
 *     key string (0..1)
 * </pre>
 *
 * <p>Grammar:
 * <pre>
 * annotationDecl:
 *     ANNOTATION validID COLON (LBRACK PREFIX validID RBRACK)?
 *     definable?
 *     attribute*
 * ;
 * </pre>
 */
public class RAnnotation extends RRootElement implements RDefinable {

    private String name;
    private String definition;
    private String prefix;
    private final List<RAttribute> attributes = new ArrayList<>();

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

    // -- prefix ---------------------------------------------------------------

    public Optional<String> prefix() {
        return Optional.ofNullable(prefix);
    }

    public void setPrefix(String prefix) {
        checkMutable();
        this.prefix = prefix;
    }

    // -- attributes -----------------------------------------------------------

    public List<RAttribute> attributes() {
        return attributes;
    }

    // -- children (for traversal) ---------------------------------------------

    /**
     * Returns an unmodifiable view of the annotation attributes, per the
     * {@link RNode#children()} contract.
     */
    @Override
    public List<? extends RNode> children() {
        List<RNode> result = new ArrayList<>();
        result.addAll(super.children());     // hoisted runeAnnotations (P1.4.2 H2)
        result.addAll(attributes);
        return List.copyOf(result);
    }
}
