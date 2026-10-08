package com.regnosys.rosetta.ir.core;

/**
 * Closed enum discriminator for {@link IRNode} kinds. Java enums are
 * inherently closed (no {@code sealed} keyword needed) — exhaustive
 * {@code switch} supported out of the box.
 *
 * <p>22 values: 18 top-level kinds (one per RRootElement subclass) + 1
 * model-level container kind ({@link #MODEL}) + 3 sub-element kinds (graph
 * uniformity per spec Section 2 reviewer fix I1).
 *
 * <p><b>Serialization stability:</b> the NAMES of these values are the public API
 * contract - the JSON codec ({@code IRJsonSerializer} / {@code IRJsonDeserializer})
 * and every consumer in-tree read a kind by its {@code name()}, never by its
 * ordinal - and the contract test {@code IRKindCoverageTest} pins the value list
 * ({@code Arrays.toString}) to a committed string, so an insertion is a deliberate,
 * pinned move: v3.3 seat 8 (PR #644) inserted {@link #MODEL} before the three
 * sub-element kinds (the ordinals of {@link #FIELD}, {@link #PARAMETER} and
 * {@link #ENUM_VALUE} moved by one; no ordinal consumer exists). Renaming a value
 * is a breaking change requiring a D-entry (the M9 Python target serialises these
 * names).
 */
public enum IRKind {
    // ----- top-level kinds (18, one per RRootElement subclass) -----
    /** {@code data} declarations — RDataType. */
    STRUCT,
    /** {@code choice} declarations — RChoice. */
    CHOICE,
    /** {@code typeAlias} declarations — RTypeAlias. */
    TYPE_ALIAS,
    /** Built-in basic types — RBasicType. */
    BASIC_TYPE,
    /** Built-in record types — RRecordType. */
    RECORD_TYPE,
    /** Built-in meta types — RMetaType. */
    META_TYPE,
    /** {@code enum} declarations — REnumeration. */
    ENUM,
    /** {@code func} declarations — RFunction. */
    FUNCTION,
    /** Built-in library functions — RLibraryFunction. */
    LIBRARY_FUNCTION,
    /** {@code reportingRule}/{@code eligibilityRule} declarations — RRule. */
    RULE,
    /** {@code report} declarations — RReport (rune-specific). */
    REPORT,
    /** Annotation declarations ({@code annotation}) — RAnnotation (rune-specific). */
    ANNOTATION_DECL,
    /** Legacy synonym source — RSynonymSource (rune-specific). */
    SYNONYM_SOURCE,
    /** Modern external synonym source — RExternalSynonymSource (rune-specific). */
    EXTERNAL_SYNONYM_SOURCE,
    /** External rule source — RExternalRuleSource (rune-specific). */
    EXTERNAL_RULE_SOURCE,
    /** Regulatory body declaration — RBody (rune-specific). */
    REGULATORY_BODY,
    /** Regulatory corpus declaration — RCorpus (rune-specific). */
    REGULATORY_CORPUS,
    /** Regulatory segment definition — RSegmentDef (rune-specific). */
    REGULATORY_SEGMENT_DEF,

    // ----- model-level container kind (1) -----
    /**
     * A {@code namespace} declaration's model-level facts — RModel (v3.3 seat 8, PR #644).
     * The first model-level node.
     */
    MODEL,

    // ----- sub-element kinds (3, graph uniformity) -----
    /** A field of an {@link IRType}. */
    FIELD,
    /** A parameter of an {@link IRFunction}. */
    PARAMETER,
    /** A value of an {@link IREnum}. */
    ENUM_VALUE
}
