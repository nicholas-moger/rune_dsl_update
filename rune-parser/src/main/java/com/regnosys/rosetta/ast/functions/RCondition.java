package com.regnosys.rosetta.ast.functions;

import com.regnosys.rosetta.ast.RDefinable;
import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.annotations.RAnnotationRef;
import com.regnosys.rosetta.ast.regulatory.RDocReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Condition block node, corresponding to the {@code condition} grammar rule.
 *
 * <p>Conditions appear in data types, type aliases, and functions. Each condition
 * has an optional name and definition, and a mandatory expression body.
 *
 * <p>Grammar:
 * <pre>
 * condition:
 *     CONDITION validID? definable? COLON
 *     docReference*
 *     annotationRef*
 *     expression
 * ;
 * </pre>
 */
public class RCondition extends RNode implements RDefinable {

    private String name;
    private String definition;
    private RExpression expression;
    private String expressionText;

    private final List<RDocReference> docReferences = new ArrayList<>();

    private final List<RAnnotationRef> annotationRefs = new ArrayList<>();

    // -- name -----------------------------------------------------------------

    public Optional<String> name() {
        return Optional.ofNullable(name);
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

    // -- docReferences --------------------------------------------------------

    public List<RDocReference> docReferences() {
        return docReferences;
    }

    // -- annotationRefs -------------------------------------------------------

    public List<RAnnotationRef> annotationRefs() {
        return annotationRefs;
    }

    // -- expression -----------------------------------------------------------

    public RExpression expression() {
        return expression;
    }

    public void setExpression(RExpression expression) {
        checkMutable();
        this.expression = expression;
    }

    // -- expressionText ---------------------------------------------------------

    /**
     * The expression body's normalized source token text, captured at build time
     * with Xtext {@code getTokenText} semantics (non-hidden tokens joined with a
     * single space at hidden-token gaps — see
     * {@code AstBuilderHelper#tokenText}). Upstream extracts the same string via
     * {@code RosettaGrammarUtil.extractNodeText(condition, CONDITION__EXPRESSION)}
     * for the generated datarule {@code DEFINITION} constant. Empty for a
     * condition without an expression; may be absent (empty Optional) on nodes
     * built by tests that construct the AST directly.
     */
    public Optional<String> expressionText() {
        return Optional.ofNullable(expressionText);
    }

    public void setExpressionText(String expressionText) {
        checkMutable();
        this.expressionText = expressionText;
    }

    // -- children (for traversal) ---------------------------------------------

    @Override
    public List<? extends RNode> children() {
        List<RNode> result = new ArrayList<>();
        result.addAll(docReferences);
        result.addAll(annotationRefs);
        if (expression != null) {
            result.add(expression);
        }
        return List.copyOf(result);
    }
}
