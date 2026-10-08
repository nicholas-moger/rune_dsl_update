package com.regnosys.rosetta.ast.mapping;

import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.enums.MappingInstanceKind;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Mapping instance node, corresponding to the {@code rosettaMappingInstance}
 * grammar rule.
 *
 * <p>Represents either a {@code set when ...} or {@code default to ...} clause
 * within a mapping block.
 *
 * <p>Grammar:
 * <pre>
 * rosettaMappingInstance:
 *     SET WHEN rosettaMappingPathTests
 *   | DEFAULT TO rosettaMapPrimaryExpression (WHEN rosettaMappingPathTests)?
 * ;
 * </pre>
 */
public class RMappingInstance extends RNode {

    private MappingInstanceKind kind;
    private RMappingPathTests tests;
    private RMapPrimaryExpression defaultValue;

    // -- kind -----------------------------------------------------------------

    public MappingInstanceKind kind() {
        return kind;
    }

    public void setKind(MappingInstanceKind kind) {
        checkMutable();
        this.kind = kind;
    }

    // -- tests (SET_WHEN or optional on DEFAULT_TO) ---------------------------

    public Optional<RMappingPathTests> tests() {
        return Optional.ofNullable(tests);
    }

    public void setTests(RMappingPathTests tests) {
        checkMutable();
        this.tests = tests;
    }

    // -- defaultValue (DEFAULT_TO) --------------------------------------------

    public Optional<RMapPrimaryExpression> defaultValue() {
        return Optional.ofNullable(defaultValue);
    }

    public void setDefaultValue(RMapPrimaryExpression defaultValue) {
        checkMutable();
        this.defaultValue = defaultValue;
    }

    // -- children (for traversal) ---------------------------------------------

    @Override
    public List<? extends RNode> children() {
        List<RNode> result = new ArrayList<>();
        if (tests != null) {
            result.add(tests);
        }
        if (defaultValue != null) {
            result.add(defaultValue);
        }
        return List.copyOf(result);
    }
}
