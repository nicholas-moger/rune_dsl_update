package com.regnosys.rosetta.ast.functions;

import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.symbols.diagnostics.DiagnosticCategory;
import com.regnosys.rosetta.symbols.linker.CrossRefField;

/**
 * Dispatch info extracted from a function header.
 *
 * <p>When a function declares {@code func Foo(param EnumType -> EnumValue):},
 * this node captures the dispatch parameter name, enum type reference,
 * and enum value name.
 *
 * <p>Grammar (inline in function header):
 * <pre>
 * (LPAREN validID typeCall ARROW validID RPAREN)?
 * </pre>
 */
public class RDispatch extends RNode {

    private String paramName;
    private String enumRef;
    private String valueName;

    // -- paramName ------------------------------------------------------------

    public String paramName() {
        return paramName;
    }

    public void setParamName(String paramName) {
        checkMutable();
        this.paramName = paramName;
    }

    // -- enumRef --------------------------------------------------------------

    public String enumRef() {
        return enumRef;
    }

    public void setEnumRef(String enumRef) {
        checkMutable();
        this.enumRef = enumRef;
    }

    // -- valueName ------------------------------------------------------------

    public String valueName() {
        return valueName;
    }

    public void setValueName(String valueName) {
        checkMutable();
        this.valueName = valueName;
    }

    // === M3 resolved fields (D2) =============================================

    @CrossRefField(category = DiagnosticCategory.DISPATCH_PARAM_NOT_FOUND)
    private com.regnosys.rosetta.ast.supporting.RAttribute resolvedParam;

    @CrossRefField(category = DiagnosticCategory.ENUM_VALUE_NOT_FOUND)
    private com.regnosys.rosetta.ast.supporting.REnumValue resolvedValue;

    public java.util.Optional<com.regnosys.rosetta.ast.supporting.RAttribute> parameter() { return java.util.Optional.ofNullable(resolvedParam); }
    public void setResolvedParam(com.regnosys.rosetta.ast.supporting.RAttribute resolved) { checkMutable(); this.resolvedParam = resolved; }

    public java.util.Optional<com.regnosys.rosetta.ast.supporting.REnumValue> dispatchValue() { return java.util.Optional.ofNullable(resolvedValue); }
    public void setResolvedValue(com.regnosys.rosetta.ast.supporting.REnumValue resolved) { checkMutable(); this.resolvedValue = resolved; }
}
