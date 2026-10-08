package com.regnosys.rosetta.ast.expressions.supporting;

import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.enums.SwitchGuardKind;
import com.regnosys.rosetta.ast.enums.SwitchGuardLiteralKind;

import java.util.Optional;

/**
 * Switch case guard node, corresponding to the {@code switchCaseGuard}
 * grammar rule.
 *
 * <p>Represents the guard condition of a switch case. The guard is either
 * a literal value ({@link SwitchGuardKind#LITERAL}) or a qualified name
 * reference ({@link SwitchGuardKind#NAME}).
 *
 * <p>Grammar:
 * <pre>
 * switchCaseGuard:
 *     literal           // LITERAL kind
 *   | qualifiedName      // NAME kind (type or enum reference)
 * ;
 * </pre>
 */
public class RSwitchCaseGuard extends RNode {

    private SwitchGuardKind kind;
    private String literalValue;
    private SwitchGuardLiteralKind literalKind;
    private String qualifiedName;

    // -- kind -----------------------------------------------------------------

    public SwitchGuardKind kind() {
        return kind;
    }

    public void setKind(SwitchGuardKind kind) {
        checkMutable();
        this.kind = kind;
    }

    // -- literalValue (optional, for LITERAL guards) --------------------------

    /**
     * Returns the literal value as a string for a literal-kind guard.
     *
     * <p><strong>Format:</strong>
     * <ul>
     *   <li>STRING literals — quotes are stripped: {@code "one"} → {@code one}</li>
     *   <li>INT_LITERAL / BIG_DECIMAL — raw numeric text: {@code 42}, {@code 3.14}</li>
     *   <li>TRUE / FALSE — the raw keyword text: {@code True}, {@code False}</li>
     * </ul>
     *
     * <p>The text alone does not say which terminal produced it — a STRING guard
     * {@code "42"} and an INT_LITERAL guard {@code 42} both read {@code 42} here;
     * {@link #literalKind()} is the channel that does.
     *
     * @return the literal text (see format above) or empty for name-kind guards
     */
    public Optional<String> literalValue() {
        return Optional.ofNullable(literalValue);
    }

    public void setLiteralValue(String literalValue) {
        checkMutable();
        this.literalValue = literalValue;
    }

    // -- literalKind (optional, for LITERAL guards) ---------------------------

    /**
     * The lexer-terminal kind of a literal-kind guard — {@code STRING}, {@code INT},
     * {@code DECIMAL} or {@code BOOLEAN} — recorded by the AST builder from the terminal that
     * produced {@link #literalValue()}. Empty for a name-kind guard, and for a node built by
     * hand without one (v3.1 C2d, retirement family 1 {@code numeric-literal-kind}: the typed
     * channel a consumer reads instead of classifying the value's text).
     */
    public Optional<SwitchGuardLiteralKind> literalKind() {
        return Optional.ofNullable(literalKind);
    }

    public void setLiteralKind(SwitchGuardLiteralKind literalKind) {
        checkMutable();
        this.literalKind = literalKind;
    }

    // -- qualifiedName (optional, for NAME guards) ----------------------------

    public Optional<String> qualifiedName() {
        return Optional.ofNullable(qualifiedName);
    }

    public void setQualifiedName(String qualifiedName) {
        checkMutable();
        this.qualifiedName = qualifiedName;
    }

    // === M4 resolved fields (D9) =============================================

    @com.regnosys.rosetta.symbols.linker.CrossRefField(
        category = com.regnosys.rosetta.symbols.diagnostics.DiagnosticCategory.SWITCH_GUARD_TYPE_MISMATCH)
    private com.regnosys.rosetta.ast.RNode resolvedGuard;

    public java.util.Optional<com.regnosys.rosetta.ast.RNode> resolvedGuard() {
        return java.util.Optional.ofNullable(resolvedGuard);
    }
    public void setResolvedGuard(com.regnosys.rosetta.ast.RNode resolved) {
        checkMutable();
        this.resolvedGuard = resolved;
    }
}
