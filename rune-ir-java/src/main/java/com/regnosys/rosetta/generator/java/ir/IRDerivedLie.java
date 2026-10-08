package com.regnosys.rosetta.generator.java.ir;

/**
 * THE LYING-HALF TEST SEAM of the derived-file fact families (v3.3 seat 8, PR #644; RELOCATED to its own top-level
 * declaration at v3.3 seat 9, PR #645 commit 12).
 *
 * <p>Each constant disables ONE named law of ONE half, so a witness can prove that the fact family it names reads RED
 * and that NOTHING else does. Half of them lie to the IR side (a fact the emitter would read wrong) and half to the
 * RECONCILER'S OWN source walk (the independence proof, lane G2: if the two halves were one producer, a lie to one of
 * them would stay green).
 *
 * <p><b>WHY IT IS TOP-LEVEL NOW.</b> It was a nested enum on {@link IRDerivedFactsReconciler} while that class held
 * BOTH halves. Commit 12 relocates the IR half into {@link IRDerivedFacts} - one law, two callers (the reconciler and
 * the emitters) - and a seam that named the reconciler would make the emitters' derivation depend on the class that
 * reconciles it. The constants and their javadoc are VERBATIM the nested enum's; {@code IRDerivedFactsReconciler.lie}
 * remains the test seam and hands the value to {@link IRDerivedFacts}.
 *
 * <p>Package-private and set only by a test - no production caller, and {@link #NONE} is every production field's
 * initial value, so an un-lied derivation is the one every generator holds.
 */
enum IRDerivedLie {
    /** No law disabled - the production value. */
    NONE,
    /** SOURCE walk: the alias chain stops at its first rung (lane D02). */
    ALIAS_CHAIN_NO_RECURSION,
    /** SOURCE walk: the condition chain is collected element-first instead of ROOT-first (lane D05). */
    CONDITION_CHAIN_ELEMENT_FIRST,
    /**
     * SOURCE walk: an UNNAMED condition is indexed by its own ordinal among the type's unnamed conditions
     * instead of by the CORPUS-FITTED {@code namedCount} law ({@code ModelMetaGenerator.java:297-299}) - the
     * lane for risk R7 (lane D05b). The two laws agree for every type with at most ONE unnamed condition, which
     * is every type of the 9.83.0 corpus, and part company on the fixture's {@code TwoUnnamed}: the corpus law
     * gives BOTH unnamed conditions the same class name, the ordinal law gives them two.
     */
    CONDITION_UNNAMED_BY_ORDINAL,
    /** SOURCE walk: the meta-annotation union over an override loses its parent leg (lane G2). */
    META_UNION_NO_PARENT_LEG,
    /** IR half: the {@code isFullyUnbounded} skip is inverted (lane D03). */
    CARDINALITY_SKIP_INVERTED,
    /** IR half: the synthetic-{@code meta} member is NOT skipped (lane D04). */
    ONLY_EXISTS_NO_SYNTHETIC_SKIP,
    /**
     * IR half: the number envelope loses its REGISTRY-BASE fallback (lane D01) - the merge that lets
     * {@code int}'s own {@code fractionalDigits: 0} survive a use site that never restates it, and which is the
     * only reason an {@code int}-typed attribute carries a type-format check at all.
     */
    NUMBER_SLOT_NO_BASE,
    /** IR half: deep-path eligibility drops the "every attribute singular optional" clause (lane D10). */
    DEEP_ELIGIBLE_NO_SINGULAR_CLAUSE,
    /**
     * IR half: the deep-feature map is re-keyed into a {@code LinkedHashMap} in reverse order (lane D13) - the
     * witness for the HashMap ORDER LAW, which no re-derivation from the model can reproduce.
     */
    DEEP_ORDER_NOT_HASH_ORDER,
    /**
     * IR half: the deep-feature METADATA-COLLAPSE value swaps are dropped (lane D14) - the omission that let
     * cdm/5.38.0's {@code SettlementOrigin} keep a {@code chooseSettlementTerms} the golden has not got.
     */
    DEEP_META_COLLAPSE_DROPPED,
    /** IR half: the {@code java.lang} collision predicate is forced false (lane D12). */
    COLLISION_ALWAYS_FALSE,
    /** IR half ({@link IRModelReconciler}): the namespace map loses its {@code LinkedHashSet} dedup (lane D11). */
    PACKAGE_INFO_NO_DEDUP,
    /** IR half ({@link IRModelReconciler}): the qualify wing drops the first-input match (lane D07). */
    QUALIFY_NO_FIRST_INPUT_MATCH,
    /** IR half ({@link IRWrapperReconciler}): the FUNCTION sources are dropped from the spec scan (lane D08). */
    WRAPPER_NO_FUNCTION_SOURCE,
    /** IR half ({@link IRWrapperReconciler}): {@code ENUM} is folded into {@code COMPOSITE} (lane D09). */
    WRAPPER_ENUM_AS_COMPOSITE
}
