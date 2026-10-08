package com.regnosys.rosetta.ast;

import java.util.Set;

/**
 * Test-scope registry of RRootElement subclasses expected to appear at
 * least once in the corpus walk.
 *
 * <p>Two-tier dead-subclass detection (per spec Section 5 reviewer fix I2):
 * <ul>
 *   <li><b>Hard-fail</b> on subclasses listed in {@link #EXPECTED} that are
 *       absent from the corpus.
 *   <li><b>Soft-warn</b> tier — reserved. Originally intended to flag
 *       RRootElement subclasses NOT in this set with zero observations,
 *       but is structurally unreachable today: the audit's
 *       {@code totalByClass} map is built by the corpus walk via
 *       {@code merge(fqn, 1, Integer::sum)}, so every present entry has
 *       a count ≥ 1 by construction — a present key cannot read
 *       {@code value == 0}, and absent keys are never inserted. A
 *       meaningful soft-warn would require pre-seeding {@code totalByClass}
 *       with {@code 0} for every known {@code RRootElement} subclass FQN
 *       before the walk (e.g. via reflective enumeration at test time,
 *       deferred to a follow-up PR).
 * </ul>
 *
 * <p>P1.4.3 marks all 18 subclasses as expected based on the baseline corpus
 * walk over the populated 1,628 fixtures (CATALOGUE 1,626 + 2 rune-runtime
 * builtins; both gitignored / cloned-on-demand).
 *
 * <p>Stored as fully-qualified-name strings rather than {@code Class<?>} so
 * production RRootElement subclasses do not need any test-scope import.
 */
final class CorpusExpected {

    private CorpusExpected() {
        // utility class — no instances
    }

    /**
     * The 18 RRootElement subclasses verified to appear at least once in the
     * P1.4.3 corpus baseline (1,628 fixtures when populated). Hard-failure
     * if any is absent from the corpus walk.
     */
    static final Set<String> EXPECTED = Set.of(
            "com.regnosys.rosetta.ast.types.RDataType",
            "com.regnosys.rosetta.ast.types.RChoice",
            "com.regnosys.rosetta.ast.types.RTypeAlias",
            "com.regnosys.rosetta.ast.types.RBasicType",
            "com.regnosys.rosetta.ast.types.RRecordType",
            "com.regnosys.rosetta.ast.types.RMetaType",
            "com.regnosys.rosetta.ast.types.REnumeration",
            "com.regnosys.rosetta.ast.types.RLibraryFunction",
            "com.regnosys.rosetta.ast.functions.RFunction",
            "com.regnosys.rosetta.ast.functions.RRule",
            "com.regnosys.rosetta.ast.regulatory.RReport",
            "com.regnosys.rosetta.ast.annotations.RAnnotation",
            "com.regnosys.rosetta.ast.synonyms.RSynonymSource",
            "com.regnosys.rosetta.ast.external.RExternalRuleSource",
            "com.regnosys.rosetta.ast.external.RExternalSynonymSource",
            "com.regnosys.rosetta.ast.regulatory.RBody",
            "com.regnosys.rosetta.ast.regulatory.RCorpus",
            "com.regnosys.rosetta.ast.regulatory.RSegmentDef"
    );
}
