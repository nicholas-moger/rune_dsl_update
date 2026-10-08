package com.regnosys.rosetta.ast;

import java.util.Map;
import java.util.Set;

/**
 * Test-scope mapping from RRootElement subclass FQN to the IRKind name it
 * maps to in the P1.4.3 IR. Mirrors spec Section 4 coverage matrix and is
 * originally rendered into a development audit document; carried here for
 * {@link com.regnosys.rosetta.ir.contract.RRootElementToIRKindMappingTest}.
 *
 * <p>Stored here (rather than reading {@code IRKind} reflectively) so the
 * audit test does not need to import the production IR types — keeping the
 * audit decoupled from the IR scaffolding it documents.
 */
public final class IRKindForClass {

    private static final Map<String, String> MAP = Map.ofEntries(
            Map.entry("com.regnosys.rosetta.ast.types.RDataType",            "STRUCT"),
            Map.entry("com.regnosys.rosetta.ast.types.RChoice",              "CHOICE"),
            Map.entry("com.regnosys.rosetta.ast.types.RTypeAlias",           "TYPE_ALIAS"),
            Map.entry("com.regnosys.rosetta.ast.types.RBasicType",           "BASIC_TYPE"),
            Map.entry("com.regnosys.rosetta.ast.types.RRecordType",          "RECORD_TYPE"),
            Map.entry("com.regnosys.rosetta.ast.types.RMetaType",            "META_TYPE"),
            Map.entry("com.regnosys.rosetta.ast.types.REnumeration",         "ENUM"),
            Map.entry("com.regnosys.rosetta.ast.types.RLibraryFunction",     "LIBRARY_FUNCTION"),
            Map.entry("com.regnosys.rosetta.ast.functions.RFunction",        "FUNCTION"),
            Map.entry("com.regnosys.rosetta.ast.functions.RRule",            "RULE"),
            Map.entry("com.regnosys.rosetta.ast.regulatory.RReport",         "REPORT"),
            Map.entry("com.regnosys.rosetta.ast.annotations.RAnnotation",    "ANNOTATION_DECL"),
            Map.entry("com.regnosys.rosetta.ast.synonyms.RSynonymSource",    "SYNONYM_SOURCE"),
            Map.entry("com.regnosys.rosetta.ast.external.RExternalSynonymSource", "EXTERNAL_SYNONYM_SOURCE"),
            Map.entry("com.regnosys.rosetta.ast.external.RExternalRuleSource",   "EXTERNAL_RULE_SOURCE"),
            Map.entry("com.regnosys.rosetta.ast.regulatory.RBody",           "REGULATORY_BODY"),
            Map.entry("com.regnosys.rosetta.ast.regulatory.RCorpus",         "REGULATORY_CORPUS"),
            Map.entry("com.regnosys.rosetta.ast.regulatory.RSegmentDef",     "REGULATORY_SEGMENT_DEF")
    );

    private IRKindForClass() {
        // utility class — no instances
    }

    /**
     * RRootElement-subclass FQNs known to this map.
     *
     * <p>Returns an immutable copy so callers cannot mutate the underlying
     * map's key set even if {@code MAP} were ever switched from
     * {@link java.util.Map#ofEntries} to a mutable backing.
     */
    public static Set<String> knownFqns() {
        return Set.copyOf(MAP.keySet());
    }

    /**
     * The IRKind NAMES this map reaches — read by
     * {@code RRootElementToIRKindMappingTest#theModelKindMapsFromNoRRootElementSubclass}, which holds the
     * COMPLEMENT: the kinds no {@code RRootElement} subclass produces are the 3 sub-element kinds and, since
     * v3.3 seat 8 (PR #644 — the property gate), {@code MODEL} — a model is the {@code namespace}
     * declaration itself ({@code RModel extends RNode}), which is not an {@code RRootElement}, so it gets no
     * row here and none in {@code CorpusExpected.EXPECTED} either.
     *
     * <p>Returns an immutable copy, as {@link #knownFqns()} does.
     */
    public static Set<String> knownKinds() {
        return Set.copyOf(MAP.values());
    }

    /**
     * Mirrors {@code classifyCorpus} discipline: hard-fail on a path that
     * matched no known bucket. An unmapped FQN means a new RRootElement
     * subclass was added without updating both this map AND
     * {@code CorpusExpected.EXPECTED} — surface that drift loudly rather than
     * letting it slip into the audit doc as a silent {@code "UNMAPPED"}.
     */
    static String lookup(String fqn) {
        String name = MAP.get(fqn);
        if (name == null) {
            throw new IllegalStateException(
                    "RRootElement subclass " + fqn + " has no IRKind mapping in " +
                            "IRKindForClass.MAP — add it (and a matching entry in " +
                            "CorpusExpected.EXPECTED) when introducing a new subclass");
        }
        return name;
    }
}
