package com.regnosys.rosetta.ir.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.regnosys.rosetta.ir.core.IRAliasLink;
import com.regnosys.rosetta.ir.core.IREffectiveBase;
import com.regnosys.rosetta.ir.core.IRKind;
import com.regnosys.rosetta.ir.core.IRQualifiableConfig;
import com.regnosys.rosetta.ir.core.IRWithMetaUse;
import com.regnosys.rosetta.testutil.IRSamples;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Drift sentinel (no validator): for each declaration kind, the schema's required member set must
 * equal the member-name set IRJsonSerializer actually EMITS — derived by serializing the sample and
 * reading the top-level JSON keys (which include "kind"), NOT from IRSamples.RENDERED_COMPONENTS
 * (a reflected-record-component map that omits "kind" for ENUM/FIELD/ENUM_VALUE).
 */
class IRJsonSchemaFaithfulnessTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final IRJsonSerializer SER = new IRJsonSerializer();

    static Stream<Arguments> kindToDef() {
        return Stream.of(
                Arguments.of(IRKind.STRUCT, "typeNode"),
                Arguments.of(IRKind.CHOICE, "typeNode"),
                Arguments.of(IRKind.ENUM, "enumNode"),
                Arguments.of(IRKind.FIELD, "fieldNode"),
                Arguments.of(IRKind.ENUM_VALUE, "enumValueNode"),
                // the type gate (PR #643): the typeAlias declaration is a $def of its own
                Arguments.of(IRKind.TYPE_ALIAS, "typeAliasNode"),
                // the property gate (PR #644): so is the MODEL node
                Arguments.of(IRKind.MODEL, "modelNode"));
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("kindToDef")
    void schemaRequiredEqualsSerializerEmittedKeys(IRKind kind, String defName) throws Exception {
        // emitted top-level keys (fragment form surfaces FIELD/ENUM_VALUE; includes "kind")
        JsonNode emitted = MAPPER.readTree(SER.toJson(IRSamples.node(kind)));
        Set<String> emittedKeys = new TreeSet<>();
        emitted.fieldNames().forEachRemaining(emittedKeys::add);

        // schema required for the corresponding $def
        JsonNode schema = MAPPER.readTree(IRJsonSchema.schemaJson());
        Set<String> required = new TreeSet<>();
        schema.get("$defs").get(defName).get("required").forEach(n -> required.add(n.asText()));

        assertEquals(required, emittedKeys,
                kind + " emitted keys must equal $defs." + defName + ".required");
    }

    /**
     * The type gate's second sentinel: the schema's ORDERED leaf-kind list — the one the
     * {@code effectiveBaseNode} enum and {@link IRJsonDeserializer} are both built from — must hold
     * EXACTLY the kinds {@link IREffectiveBase#LEAF_KINDS} admits. The record's set is unordered, so
     * the wire contract cannot be generated from it; this pins the copy to the original.
     *
     * <p>Able to fail: add or drop one {@link IRKind} in {@code IRJsonSchema.EFFECTIVE_BASE_KINDS}.
     */
    @Test
    void theSchemasEffectiveBaseKindsAreExactlyTheRecordsLeafKinds() {
        assertEquals(IREffectiveBase.LEAF_KINDS, Set.of(IRJsonSchema.EFFECTIVE_BASE_KINDS),
                "the schema's effective-base kinds must equal IREffectiveBase.LEAF_KINDS");
        assertEquals(List.of("BASIC_TYPE", "RECORD_TYPE", "STRUCT", "ENUM", "CHOICE"),
                List.of(IRJsonSchema.EFFECTIVE_BASE_KINDS).stream().map(IRKind::name).toList(),
                "the wire order of the effective-base kinds is part of the frozen schema");
    }

    /**
     * The property gate's third sentinel (PR #644): each of the three CLOSED vocabularies the new
     * shapes state is read back OUT OF THE RENDERED SCHEMA and compared to the record that owns it -
     * the condition kinds to {@link IRAliasLink#CONDITION_KINDS}, a qualifiable configuration's kind
     * to {@link IRQualifiableConfig#KINDS}, a with-meta use's refusal to
     * {@link IRWithMetaUse#REFUSALS}. The schema is GENERATED from those lists, so this pins that the
     * generation actually reached the document rather than pinning a copy to itself.
     *
     * <p>Able to fail: add or drop one token in any of the three records - the rendered enum moves.
     */
    @Test
    void theSchemasClosedVocabulariesAreExactlyTheirRecordsOwn() throws Exception {
        JsonNode schema = MAPPER.readTree(IRJsonSchema.schemaJson());
        assertEquals(IRAliasLink.CONDITION_KINDS,
                tokens(schema, "aliasLinkNode", "conditionKinds", true),
                "the alias link's condition kinds must equal IRAliasLink.CONDITION_KINDS");
        assertEquals(IRAliasLink.CONDITION_KINDS,
                tokens(schema, "typeNode", "conditionKinds", true),
                "a type declaration's condition kinds must equal IRAliasLink.CONDITION_KINDS");
        assertEquals(IRQualifiableConfig.KINDS,
                tokens(schema, "qualifiableConfigNode", "kind", false),
                "a qualifiable configuration's kinds must equal IRQualifiableConfig.KINDS");
        assertEquals(IRWithMetaUse.REFUSALS,
                tokens(schema, "withMetaUseNode", "refusal", false),
                "a with-meta use's refusals must equal IRWithMetaUse.REFUSALS");
    }

    /** The {@code enum} tokens of one property of one {@code $def}; {@code inArray} unwraps {@code items}. */
    private static List<String> tokens(JsonNode schema, String def, String property, boolean inArray) {
        JsonNode node = schema.get("$defs").get(def).get("properties").get(property);
        if (inArray) {
            node = node.get("items");
        }
        List<String> values = new java.util.ArrayList<>();
        node.get("enum").forEach(n -> values.add(n.asText()));
        return values;
    }
}
