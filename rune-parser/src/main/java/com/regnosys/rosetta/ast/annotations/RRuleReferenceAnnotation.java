package com.regnosys.rosetta.ast.annotations;

import com.regnosys.rosetta.ast.RNode;

import com.regnosys.rosetta.symbols.diagnostics.DiagnosticCategory;
import com.regnosys.rosetta.symbols.linker.CrossRefField;

import java.util.List;
import java.util.Optional;

/**
 * Rule reference annotation node, corresponding to the
 * {@code ruleReferenceAnnotation} grammar rule.
 *
 * <p>Represents a rule reference annotation such as
 * {@code [ruleReference for trade -> partyRole TradePartyRole]}
 * or {@code [ruleReference empty]}.
 *
 * <p>Grammar:
 * <pre>
 * ruleReferenceAnnotation:
 *     LBRACK RULE_REFERENCE (FOR annotationPathExpression)?
 *     (qualifiedName | EMPTY) RBRACK
 * ;
 * </pre>
 */
public class RRuleReferenceAnnotation extends RNode {

    private RAnnotationPathExpression forPath;
    private String ruleName;

    // -- forPath --------------------------------------------------------------

    public Optional<RAnnotationPathExpression> forPath() {
        return Optional.ofNullable(forPath);
    }

    public void setForPath(RAnnotationPathExpression forPath) {
        checkMutable();
        this.forPath = forPath;
    }

    // -- ruleName -------------------------------------------------------------

    /**
     * Returns the referenced rule's qualified name, or {@link Optional#empty()}
     * if the {@code EMPTY} keyword was used.
     */
    public Optional<String> ruleName() {
        return Optional.ofNullable(ruleName);
    }

    public void setRuleName(String ruleName) {
        checkMutable();
        this.ruleName = ruleName;
    }

    // -- children (for traversal) ---------------------------------------------

    /**
     * Returns an unmodifiable singleton list containing {@code forPath} when
     * present, or an empty list otherwise. Avoids allocating a mutable
     * {@code ArrayList} for a single child, per the {@link RNode#children()}
     * contract.
     */
    @Override
    public List<? extends RNode> children() {
        return forPath != null ? List.of(forPath) : List.of();
    }

    // === M3 resolved fields (D2) =============================================

    @CrossRefField(category = DiagnosticCategory.RULE_NOT_FOUND)
    private com.regnosys.rosetta.ast.functions.RRule resolvedRule;

    public java.util.Optional<com.regnosys.rosetta.ast.functions.RRule> rule() { return java.util.Optional.ofNullable(resolvedRule); }
    public void setResolvedRule(com.regnosys.rosetta.ast.functions.RRule resolved) { checkMutable(); this.resolvedRule = resolved; }
}
