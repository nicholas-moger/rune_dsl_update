package com.regnosys.rosetta.ast.mapping;

import com.regnosys.rosetta.ast.RNode;

import com.regnosys.rosetta.symbols.diagnostics.DiagnosticCategory;
import com.regnosys.rosetta.symbols.linker.CrossRefField;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Map test for function-based conditions, corresponding to
 * {@code FUNC funcName (LPAREN rosettaMapPathValue RPAREN)?}
 * in the grammar.
 */
public class RMapTestFunc extends RMapTest {

    private String funcName;
    private RMapPathValue conditionPath;

    // -- funcName -------------------------------------------------------------

    public String funcName() {
        return funcName;
    }

    public void setFuncName(String funcName) {
        checkMutable();
        this.funcName = funcName;
    }

    // -- conditionPath --------------------------------------------------------

    public Optional<RMapPathValue> conditionPath() {
        return Optional.ofNullable(conditionPath);
    }

    public void setConditionPath(RMapPathValue conditionPath) {
        checkMutable();
        this.conditionPath = conditionPath;
    }

    // -- children (for traversal) ---------------------------------------------

    @Override
    public List<? extends RNode> children() {
        if (conditionPath != null) {
            return Collections.singletonList(conditionPath);
        }
        return Collections.emptyList();
    }

    // === M3 resolved fields (D2) =============================================

    @CrossRefField(category = DiagnosticCategory.FUNCTION_NOT_FOUND)
    private com.regnosys.rosetta.ast.functions.RFunction resolvedFunction;

    public java.util.Optional<com.regnosys.rosetta.ast.functions.RFunction> referencedFunction() { return java.util.Optional.ofNullable(resolvedFunction); }
    public void setResolvedFunction(com.regnosys.rosetta.ast.functions.RFunction resolved) { checkMutable(); this.resolvedFunction = resolved; }
}
