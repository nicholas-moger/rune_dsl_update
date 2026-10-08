package com.regnosys.rosetta.harness.matrix;

/**
 * One axis of the scope matrix: the kind of top-level element the test exercises.
 *
 * <p>The project scope matrix defines 18+ element kinds per
 * {@code docs/ROADMAP.md}. This enum enumerates them; every matrix test coordinate
 * carries one {@code ElementKind} so coverage reports can group by kind and so
 * the scope-honesty rule holds (no silent element-kind defaults).
 *
 * <p>The enum is open to growth: new kinds added here do not break existing
 * tests (JUnit parameterized tests iterate over {@link #values()} and the
 * matrix-runner framework treats unrecognised kinds as a skip diagnostic rather
 * than a failure).
 *
 * <p>Part of P1.2 test-harness infrastructure (audit hook H13).
 */
public enum ElementKind {

    /** A {@code type} declaration. */
    TYPE,
    /** A {@code data} declaration (alias for {@code type} in newer grammar). */
    DATA_TYPE,
    /** An {@code enum} declaration. */
    ENUM,
    /** A {@code choice} declaration (discriminated enumeration). */
    CHOICE,
    /** A {@code typeAlias} declaration. */
    TYPE_ALIAS,
    /** A {@code recordType} declaration. */
    RECORD_TYPE,
    /** A {@code basicType} declaration. */
    BASIC_TYPE,
    /** A {@code metaType} declaration. */
    META_TYPE,
    /** A {@code func} declaration. */
    FUNCTION,
    /** A {@code libraryFunction} declaration. */
    LIBRARY_FUNCTION,
    /** A {@code rule} declaration. */
    RULE,
    /** A {@code report} declaration. */
    REPORT,
    /** A {@code synonym} declaration. */
    SYNONYM,
    /** A {@code synonymSource} declaration. */
    SYNONYM_SOURCE,
    /** An {@code externalSynonymSource} declaration. */
    EXTERNAL_SYNONYM_SOURCE,
    /** An {@code externalRuleSource} declaration. */
    EXTERNAL_RULE_SOURCE,
    /** An {@code annotation} declaration. */
    ANNOTATION,
    /** An {@code ingest} declaration. */
    INGEST,
    /** An {@code apiIntegrationShell} declaration. */
    API_INTEGRATION_SHELL,
    /** A {@code corpus} declaration. */
    CORPUS_DECL,
    /** A {@code segment} declaration. */
    SEGMENT,
    /** A {@code body} declaration. */
    BODY_DECL,

    /**
     * An element kind the harness did not recognise at discovery time (e.g. a
     * new element kind added to the grammar that the enum does not yet cover).
     * A cell tagged {@code UNKNOWN} is recorded as a skip with diagnostic —
     * never silently passed.
     */
    UNKNOWN;
}
