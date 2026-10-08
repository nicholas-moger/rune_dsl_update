package com.regnosys.rosetta.ast.functions;

import com.regnosys.rosetta.ast.RDefinable;
import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.enums.OperationOp;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Operation node in a function body, corresponding to the {@code operation}
 * grammar rule.
 *
 * <p>Represents a {@code set targetPath: expression} or {@code add targetPath: expression}
 * statement with an optional segment path, definition, and as-key flag.
 *
 * <p>Grammar:
 * <pre>
 * operation:
 *     (SET | ADD) validID segment? COLON definable? expressionWithAsKey
 * ;
 *
 * expressionWithAsKey:
 *     expression AS_KEY?
 * ;
 * </pre>
 */
public class ROperation extends RNode implements RDefinable {

    private OperationOp operator;
    private String targetName;
    private RSegment segment;
    private String definition;
    private RExpression expression;
    private boolean asKey;

    // -- operator -------------------------------------------------------------

    public OperationOp operator() {
        return operator;
    }

    public void setOperator(OperationOp operator) {
        checkMutable();
        this.operator = operator;
    }

    // -- targetName -----------------------------------------------------------

    public String targetName() {
        return targetName;
    }

    public void setTargetName(String targetName) {
        checkMutable();
        this.targetName = targetName;
    }

    // -- segment --------------------------------------------------------------

    public Optional<RSegment> segment() {
        return Optional.ofNullable(segment);
    }

    public void setSegment(RSegment segment) {
        checkMutable();
        this.segment = segment;
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

    // -- expression -----------------------------------------------------------

    public RExpression expression() {
        return expression;
    }

    public void setExpression(RExpression expression) {
        checkMutable();
        this.expression = expression;
    }

    // -- asKey ----------------------------------------------------------------

    public boolean isAsKey() {
        return asKey;
    }

    public void setAsKey(boolean asKey) {
        checkMutable();
        this.asKey = asKey;
    }

    // -- children (for traversal) ---------------------------------------------

    @Override
    public List<? extends RNode> children() {
        List<RNode> result = new ArrayList<>();
        if (segment != null) {
            result.add(segment);
        }
        if (expression != null) {
            result.add(expression);
        }
        return List.copyOf(result);
    }
}
