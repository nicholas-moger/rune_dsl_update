package com.regnosys.rosetta.ir.core;

import java.util.Objects;
import java.util.Optional;

/**
 * One {@code [ruleReference …]} annotation on a field or a choice option. The report and label-provider
 * kinds read these.
 *
 * @param forPath           the {@code for <path>} scope, when written
 * @param ruleName          the rule's name AS WRITTEN; empty for the {@code empty} keyword (the reference
 *                          removes an inherited rule)
 * @param resolvedNamespace the namespace of the rule the name RESOLVES to; empty when unresolved or {@code empty}
 * @param resolvedName      that rule's own simple name
 */
public record IRRuleReference(Optional<IRAnnotationPath> forPath, Optional<String> ruleName,
                              Optional<String> resolvedNamespace, Optional<String> resolvedName) {
    public IRRuleReference {
        Objects.requireNonNull(forPath, "forPath");
        Objects.requireNonNull(ruleName, "ruleName");
        Objects.requireNonNull(resolvedNamespace, "resolvedNamespace");
        Objects.requireNonNull(resolvedName, "resolvedName");
    }

    /** True for the {@code empty} keyword. */
    public boolean isEmptyKeyword() {
        return ruleName.isEmpty();
    }
}
