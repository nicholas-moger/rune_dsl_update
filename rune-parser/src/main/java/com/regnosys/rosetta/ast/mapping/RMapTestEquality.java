package com.regnosys.rosetta.ast.mapping;

import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.enums.EqOp;

import java.util.ArrayList;
import java.util.List;

/**
 * Map test for path equality or inequality, corresponding to
 * {@code rosettaMapPathValue (EQ | NEQ) rosettaMapPrimaryExpression}
 * in the grammar.
 */
public class RMapTestEquality extends RMapTest {

    private RMapPathValue pathValue;
    private EqOp op;
    private RMapPrimaryExpression value;

    // -- pathValue ------------------------------------------------------------

    public RMapPathValue pathValue() {
        return pathValue;
    }

    public void setPathValue(RMapPathValue pathValue) {
        checkMutable();
        this.pathValue = pathValue;
    }

    // -- op -------------------------------------------------------------------

    public EqOp op() {
        return op;
    }

    public void setOp(EqOp op) {
        checkMutable();
        this.op = op;
    }

    // -- value ----------------------------------------------------------------

    public RMapPrimaryExpression value() {
        return value;
    }

    public void setValue(RMapPrimaryExpression value) {
        checkMutable();
        this.value = value;
    }

    // -- children (for traversal) ---------------------------------------------

    @Override
    public List<? extends RNode> children() {
        List<RNode> result = new ArrayList<>();
        if (pathValue != null) {
            result.add(pathValue);
        }
        if (value != null) {
            result.add(value);
        }
        return List.copyOf(result);
    }
}
