package com.regnosys.rosetta.ast.functions;

import com.regnosys.rosetta.ast.RDefinable;
import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.RRootElement;
import com.regnosys.rosetta.ast.enums.RuleKind;
import com.regnosys.rosetta.ast.regulatory.RDocReference;
import com.regnosys.rosetta.ast.supporting.RTypeCall;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Rule declaration node, corresponding to the {@code rosettaRule} grammar rule.
 *
 * <p>Represents a {@code reporting rule Foo from Bar: <"..."> expression as "alias"}
 * or {@code eligibility rule ...} declaration.
 *
 * <p>Grammar:
 * <pre>
 * rosettaRule:
 *     runeAnnotations?                          // P1.4.2 H11 — hoisted from RRootElement, see U011
 *     (REPORTING | ELIGIBILITY) RULE validID (FROM typeCall)?
 *     definable?
 *     docReference*
 *     expression? (AS STRING)?
 * ;
 * </pre>
 */
public class RRule extends RRootElement implements RDefinable {

    private RuleKind kind;
    private String name;
    private RTypeCall fromType;
    private String definition;
    private RExpression expression;
    private String alias;

    private final List<RDocReference> docReferences = new ArrayList<>();

    // -- kind -----------------------------------------------------------------

    public RuleKind kind() {
        return kind;
    }

    public void setKind(RuleKind kind) {
        checkMutable();
        this.kind = kind;
    }

    // -- name -----------------------------------------------------------------

    public String name() {
        return name;
    }

    public void setName(String name) {
        checkMutable();
        this.name = name;
    }

    // -- fromType -------------------------------------------------------------

    public Optional<RTypeCall> fromType() {
        return Optional.ofNullable(fromType);
    }

    public void setFromType(RTypeCall fromType) {
        checkMutable();
        this.fromType = fromType;
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

    // -- docReferences --------------------------------------------------------

    public List<RDocReference> docReferences() {
        return docReferences;
    }

    // -- expression -----------------------------------------------------------

    public Optional<RExpression> expression() {
        return Optional.ofNullable(expression);
    }

    public void setExpression(RExpression expression) {
        checkMutable();
        this.expression = expression;
    }

    // -- alias ----------------------------------------------------------------

    public Optional<String> alias() {
        return Optional.ofNullable(alias);
    }

    public void setAlias(String alias) {
        checkMutable();
        this.alias = alias;
    }

    // -- children (for traversal) ---------------------------------------------

    @Override
    public List<? extends RNode> children() {
        List<RNode> result = new ArrayList<>();
        result.addAll(super.children());     // hoisted runeAnnotations (P1.4.2 H11)
        if (fromType != null) {
            result.add(fromType);
        }
        result.addAll(docReferences);
        if (expression != null) {
            result.add(expression);
        }
        return List.copyOf(result);
    }
}
