package com.regnosys.rosetta.ir.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import com.regnosys.rosetta.ir.core.IRKind;
import com.regnosys.rosetta.testutil.CorpusWalker;
import com.regnosys.rosetta.testutil.IRSamples;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IRJsonSchemaValidationTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final JsonSchema SCHEMA = loadSchema();
    private static final Path GOLDEN_DIR = CorpusWalker.moduleRoot()
            .resolve("src/test/resources/ir-json-golden");

    private static JsonSchema loadSchema() {
        try {
            JsonNode schemaNode = MAPPER.readTree(IRJsonSchema.schemaJson());
            return JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012).getSchema(schemaNode);
        } catch (Exception e) {
            throw new RuntimeException("failed to load the generated schema as a Draft-2020-12 schema", e);
        }
    }

    private static Set<ValidationMessage> validate(String json) throws Exception {
        return SCHEMA.validate(MAPPER.readTree(json));
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"simple-type", "simple-choice", "simple-enum",
            "type-with-extends", "enum-with-extends", "abstract-type"})
    void goldenDocumentValidates(String name) throws Exception {
        String doc = Files.readString(GOLDEN_DIR.resolve(name + ".ir.json"));
        Set<ValidationMessage> messages = validate(doc);
        assertTrue(messages.isEmpty(), name + " failed schema validation: " + messages);
    }

    @Test
    void emptyDocumentValidates() throws Exception {
        assertTrue(validate("{ \"irFormatVersion\": 1, \"nodes\": [] }").isEmpty());
    }

    @Test
    void malformedDocumentsAreRejected() throws Exception {
        // non-object root + primitive-in-array (these guard the explicit type keywords)
        assertRejected("[1, 2, 3]");
        assertRejected("{ \"irFormatVersion\": 1, \"nodes\": [ { \"kind\": \"ENUM\", \"name\": \"E\", \"values\": [42] } ] }");
        // wrong version
        assertRejected("{ \"irFormatVersion\": 2, \"nodes\": [] }");
        // unknown member on a node
        assertRejected("{ \"irFormatVersion\": 1, \"nodes\": [ { \"kind\": \"ENUM\", \"name\": \"E\", \"values\": [], \"x\": 1 } ] }");
        // missing required member (values)
        assertRejected("{ \"irFormatVersion\": 1, \"nodes\": [ { \"kind\": \"ENUM\", \"name\": \"E\" } ] }");
        // unknown kind
        assertRejected("{ \"irFormatVersion\": 1, \"nodes\": [ { \"kind\": \"BOGUS\", \"name\": \"E\" } ] }");
        // wrong-typed member (isAbstract as a string)
        assertRejected("{ \"irFormatVersion\": 1, \"nodes\": [ { \"kind\": \"STRUCT\", \"name\": \"S\", \"isAbstract\": \"no\", \"baseType\": null, \"fields\": [] } ] }");
        // bad cardinality enum value
        assertRejected("{ \"irFormatVersion\": 1, \"nodes\": [ { \"kind\": \"STRUCT\", \"name\": \"S\", \"isAbstract\": false, \"baseType\": null, \"fields\": [ { \"kind\": \"FIELD\", \"name\": \"f\", \"type\": \"int\", \"cardinality\": \"SOMETIMES\" } ] } ] }");
    }

    /**
     * THE D55 HALF OF THE SCHEMA, TIED TO THE SERIALIZER (PR #641 round 1, cq SF-2): the goldens carry a handful of the new
     * members and the faithfulness sentinel reads REQUIRED sets only, so a mistyped optional property in one of the new
     * {@code $defs} failed nothing. The document of every fully-enriched sample - every declaration fact at once, the nested
     * shapes included - must VALIDATE; and, the control, the same document with ONE member of each nested shape misspelt must
     * be REJECTED ({@code additionalProperties: false}), so this test cannot pass by the schema admitting anything.
     */
    @ParameterizedTest(name = "enriched {0}")
    @EnumSource(value = IRKind.class, names = {"STRUCT", "CHOICE", "TYPE_ALIAS", "ENUM"})
    void everyEnrichedSampleValidatesAndAMisspeltMemberIsRejected(IRKind kind) throws Exception {
        String doc = new IRJsonSerializer().toJson(List.of(IRSamples.enriched(kind)));
        Set<ValidationMessage> messages = validate(doc);
        assertTrue(messages.isEmpty(), "the enriched " + kind + " failed schema validation: " + messages + "\n" + doc);
        List<String> members = switch (kind) {
            case ENUM -> List.of("namespace", "parentNamespace", "definition", "docReferences", "annotations", "synonyms",
                    "patternMatch", "removeHtml", "corpora", "segments", "rationales", "structuredProvision", "namedArgs",
                    "arguments");
            // the type gate (PR #643): the alias declaration's own members, the collapsed chain and
            // the declared parameters' reference facts
            // the property gate (PR #644): the alias chain's rungs and the condition kinds
            case TYPE_ALIAS -> List.of("namespace", "baseTypeKind", "baseTypeNamespace", "baseTypeResolvedName",
                    "baseTypeEffectiveBase", "baseTypeAliasChain", "parameterNames", "conditionKinds",
                    "baseTypeArguments", "typeParameters", "typeKind", "typeResolvedName",
                    "definition", "docReferences", "annotations", "conditionNames", "arguments", "nameValue",
                    "literalValue", "negated", "corpora", "segments", "rationales", "namedArgs");
            default -> List.of("namespace", "baseTypeKind", "definition", "docReferences", "annotations", "conditionNames",
                    "conditionKinds",
                    "typeKind", "typeNamespace", "typeResolvedName", "bounds", "isOverride", "typeArguments", "literalValue",
                    "labels", "forPath", "ruleReferences", "resolvedNamespace", "corpora", "segments", "rationales",
                    "namedArgs");
        };
        for (String member : members) {
            String needle = "\"" + member + "\":";
            assertTrue(doc.contains(needle), "the enriched " + kind + " does not carry '" + member + "' - the control would be vacuous:\n" + doc);
            assertRejected(doc.replace(needle, "\"" + member + "X\":"));
        }
    }

    /** A type argument carries EXACTLY ONE of nameValue / literalValue - the schema's {@code oneOf}, the record's law. */
    @Test
    void aTypeArgumentWithBothValuesOrNeitherIsRejected() throws Exception {
        String head = "{ \"irFormatVersion\": 1, \"nodes\": [ { \"kind\": \"STRUCT\", \"name\": \"S\", \"isAbstract\": false, \"baseType\": null,"
                + " \"fields\": [ { \"kind\": \"FIELD\", \"name\": \"f\", \"type\": \"number\", \"cardinality\": \"ONE_TO_ONE\","
                + " \"typeArguments\": [ { \"parameter\": \"digits\"";
        String tail = " } ] } ] } ] }";
        assertTrue(validate(head + ", \"literalValue\": \"18\"" + tail).isEmpty(), "exactly one value must validate");
        assertTrue(validate(head + ", \"nameValue\": \"n\"" + tail).isEmpty(), "exactly one value must validate");
        assertRejected(head + ", \"nameValue\": \"n\", \"literalValue\": \"18\"" + tail);
        assertRejected(head + tail);
    }

    /**
     * THE DEPENDENT-MEMBER LAW (cq SF-3): a member whose legality turns on another is STATED by the
     * schema, so the schema refuses exactly what {@link IRJsonDeserializer} refuses — a type
     * reference's {@code <prefix>Kind} and its resolved strings stand or fall together, a
     * {@code baseType*} fact needs a non-null {@code baseType}, a {@code parent*} fact needs a
     * {@code parent}, {@code negated} needs a {@code literalValue}. Each pair is asserted BOTH ways
     * (the shape the serializer emits VALIDATES, the half-stated one is REJECTED), so the test
     * cannot pass by the schema admitting or refusing everything.
     *
     * <p>Able to fail: delete the {@code dependentRequired} or {@code dependentSchemas} member from
     * any of the four {@code $defs} in {@link IRJsonSchema} — the matching rejected document below
     * starts validating.
     */
    @Test
    void aHalfStatedDependentMemberIsRejected() throws Exception {
        // a field's type reference: the writer's two legal shapes
        assertAccepted(fieldDoc(""));
        assertAccepted(fieldDoc(", \"typeKind\": \"ENUM\", \"typeNamespace\": \"ns\""));
        assertAccepted(fieldDoc(", \"typeKind\": \"BASIC_TYPE\", \"typeResolvedName\": \"string\""));
        assertRejected(fieldDoc(", \"typeKind\": \"ENUM\""));
        assertRejected(fieldDoc(", \"typeNamespace\": \"ns\""));
        assertRejected(fieldDoc(", \"typeResolvedName\": \"T\""));

        // a data type's base reference: the same law, plus a baseType that is a name, not null
        assertAccepted(structDoc("null", ""));
        assertAccepted(structDoc("\"Base\"", ""));
        assertAccepted(structDoc("\"Base\"", ", \"baseTypeKind\": \"STRUCT\", \"baseTypeResolvedName\": \"Base\""));
        assertRejected(structDoc("null", ", \"baseTypeKind\": \"STRUCT\", \"baseTypeNamespace\": \"ns\""));
        assertRejected(structDoc("\"Base\"", ", \"baseTypeKind\": \"STRUCT\""));
        assertRejected(structDoc("\"Base\"", ", \"baseTypeNamespace\": \"ns\""));

        // an enum parent: no kind member, so the facts depend on the parent alone
        assertAccepted(enumDoc(", \"parent\": \"P\", \"parentNamespace\": \"ns\", \"parentResolvedName\": \"P\""));
        assertRejected(enumDoc(", \"parentNamespace\": \"ns\""));
        assertRejected(enumDoc(", \"parentResolvedName\": \"P\""));

        // a type argument's sign is a fact ABOUT a literal
        assertAccepted(fieldDoc(", \"typeArguments\": [ { \"parameter\": \"min\","
                + " \"literalValue\": \"1.5\", \"negated\": true } ]"));
        assertRejected(fieldDoc(", \"typeArguments\": [ { \"parameter\": \"digits\","
                + " \"nameValue\": \"n\", \"negated\": true } ]"));
    }

    /**
     * A cardinality bound is never negative — {@code minimum: 0} on both members of
     * {@code boundsNode}, the schema's half of the {@code IRBounds} invariant (the record enforces
     * {@code lower <= upper} too; Draft 2020-12 has no spelling for that one).
     *
     * <p>Able to fail: put {@code typed("integer")} back in place of {@code nonNegativeInteger()} in
     * {@code IRJsonSchema} — the negative documents start validating.
     */
    @Test
    void aNegativeCardinalityBoundIsRejected() throws Exception {
        assertAccepted(fieldDoc(", \"bounds\": { \"lower\": 0, \"upper\": 1 }"));
        assertAccepted(fieldDoc(", \"bounds\": { \"lower\": 2 }"));
        assertRejected(fieldDoc(", \"bounds\": { \"lower\": -1 }"));
        assertRejected(fieldDoc(", \"bounds\": { \"lower\": 0, \"upper\": -2 }"));
    }

    /**
     * THE TYPE GATE'S DEPENDENT LAWS (PR #643), the schema half of what {@link IRJsonDeserializer}
     * refuses: a {@code <prefix>EffectiveBase} needs a {@code <prefix>Kind}
     * ({@code dependentRequired}) and that kind must read {@code TYPE_ALIAS}
     * ({@code dependentSchemas}); a collapsed chain's leaf kind is one of the five LEAF kinds and its
     * arguments are LITERAL only; and {@code typeParameters} / {@code baseTypeArguments} belong to the
     * alias declaration alone. Each law is asserted BOTH ways, so the test cannot pass by the schema
     * admitting or refusing everything.
     *
     * <p>Able to fail: drop the {@code dependsOn(prefix + "EffectiveBase", …)} entry or the
     * {@code prefix + "EffectiveBase"} {@code dependentSchemas} entry from
     * {@code IRJsonSchema.referenceFacts}, or the {@code without("nameValue")} narrowing from
     * {@code effectiveBaseNode} — the matching rejected document starts validating.
     */
    @Test
    void theTypeGatesDependentMembersAreStated() throws Exception {
        String leaf = "{ \"kind\": \"BASIC_TYPE\", \"name\": \"number\" }";
        // the writer's own shape on a field whose declared type is an alias
        assertAccepted(fieldDoc(", \"typeKind\": \"TYPE_ALIAS\", \"typeResolvedName\": \"A\","
                + " \"typeEffectiveBase\": " + leaf));
        assertRejected(fieldDoc(", \"typeEffectiveBase\": " + leaf));
        assertRejected(fieldDoc(", \"typeKind\": \"STRUCT\", \"typeResolvedName\": \"S\","
                + " \"typeEffectiveBase\": " + leaf));
        // the leaf's own kind: never TYPE_ALIAS (the chain is collapsed), never a non-type kind
        assertRejected(fieldDoc(", \"typeKind\": \"TYPE_ALIAS\", \"typeResolvedName\": \"A\","
                + " \"typeEffectiveBase\": { \"kind\": \"TYPE_ALIAS\", \"name\": \"x\" }"));
        assertRejected(fieldDoc(", \"typeKind\": \"TYPE_ALIAS\", \"typeResolvedName\": \"A\","
                + " \"typeEffectiveBase\": { \"kind\": \"FIELD\", \"name\": \"x\" }"));
        // an effective argument is LITERAL: never a parameter passed through by name
        assertAccepted(fieldDoc(", \"typeKind\": \"TYPE_ALIAS\", \"typeResolvedName\": \"A\","
                + " \"typeEffectiveBase\": { \"kind\": \"BASIC_TYPE\", \"name\": \"number\","
                + " \"arguments\": [ { \"parameter\": \"min\", \"literalValue\": \"1.5\","
                + " \"negated\": true } ] }"));
        assertRejected(fieldDoc(", \"typeKind\": \"TYPE_ALIAS\", \"typeResolvedName\": \"A\","
                + " \"typeEffectiveBase\": { \"kind\": \"BASIC_TYPE\", \"name\": \"number\","
                + " \"arguments\": [ { \"parameter\": \"digits\", \"nameValue\": \"n\" } ] }"));
        // the alias-only members belong to the alias declaration alone
        assertRejected(structDoc("null", ", \"typeParameters\": [ { \"name\": \"digits\","
                + " \"type\": \"int\" } ]"));
        assertRejected("{ \"irFormatVersion\": 1, \"nodes\": [ { \"kind\": \"CHOICE\", \"name\": \"C\","
                + " \"isAbstract\": false, \"baseType\": null, \"fields\": [], \"baseTypeArguments\":"
                + " [ { \"parameter\": \"digits\", \"literalValue\": \"3\" } ] } ] }");
    }

    /**
     * The alias DECLARATION node: its own three required members, the same {@code baseType} prefix law
     * the data type obeys, and its two alias-only lists.
     */
    @Test
    void theTypeAliasDeclarationNodeValidates() throws Exception {
        assertAccepted(aliasDoc("\"number\"", ""));
        assertAccepted(aliasDoc("null", ""));
        assertAccepted(aliasDoc("\"int\"", ", \"baseTypeKind\": \"TYPE_ALIAS\","
                + " \"baseTypeNamespace\": \"test.model\", \"baseTypeResolvedName\": \"int\","
                + " \"baseTypeEffectiveBase\": { \"kind\": \"BASIC_TYPE\", \"name\": \"number\","
                + " \"arguments\": [ { \"parameter\": \"digits\", \"literalValue\": \"3\" } ] },"
                + " \"baseTypeArguments\": [ { \"parameter\": \"digits\", \"nameValue\": \"digits\" } ],"
                + " \"typeParameters\": [ { \"name\": \"digits\", \"type\": \"int\","
                + " \"typeKind\": \"BASIC_TYPE\", \"typeResolvedName\": \"int\" } ]"));
        // the missing required member, the unknown member, and the half-stated base reference
        assertRejected("{ \"irFormatVersion\": 1, \"nodes\": [ { \"kind\": \"TYPE_ALIAS\","
                + " \"name\": \"A\" } ] }");
        assertRejected(aliasDoc("\"number\"", ", \"fields\": []"));
        assertRejected(aliasDoc("\"number\"", ", \"baseTypeKind\": \"BASIC_TYPE\""));
        assertRejected(aliasDoc("null", ", \"baseTypeKind\": \"BASIC_TYPE\","
                + " \"baseTypeResolvedName\": \"number\""));
        // a declared parameter needs both its name and its declared type
        assertRejected(aliasDoc("\"number\"", ", \"typeParameters\": [ { \"name\": \"digits\" } ]"));
        assertRejected(aliasDoc("\"number\"", ", \"typeParameters\": [ { \"type\": \"int\" } ]"));
    }


    /**
     * THE PROPERTY GATE'S OWN SHAPES (PR #644): the MODEL node's three required members and its
     * seven optional ones, and the two record laws the schema CAN state on a with-meta use — EXACTLY
     * ONE of {@code argumentType} / {@code refusal} ({@code oneOf}), and a REFUSED use carrying no
     * {@code typeArguments}. Each is asserted BOTH ways, so the test cannot pass by the schema
     * admitting or refusing everything.
     *
     * <p>Able to fail: drop {@code modelNode} from the {@code declarationNode} {@code oneOf}, or the
     * {@code oneOf} from {@code exactlyOneOfArgumentTypeOrRefusal} — the matching document flips.
     */
    @Test
    void theModelNodeAndItsSubShapesValidate() throws Exception {
        assertAccepted(modelDoc(""));
        assertAccepted(modelDoc(", \"definition\": \"d\", \"version\": \"1.2.3\""));
        assertAccepted(modelDoc(", \"qualifiableConfigs\": [ { \"kind\": \"IS_EVENT\","
                + " \"rootType\": \"E\", \"rootTypeKind\": \"STRUCT\","
                + " \"rootTypeResolvedName\": \"E\" } ]"));
        assertAccepted(modelDoc(", \"qualificationFunctions\": [ { \"name\": \"Q\","
                + " \"firstInputType\": \"P\" } ]"));
        assertAccepted(modelDoc(", \"functionSignatures\": [ { \"name\": \"F\", \"inputs\":"
                + " [ { \"kind\": \"FIELD\", \"name\": \"a\", \"type\": \"T\","
                + " \"cardinality\": \"ONE_TO_ONE\" } ] } ]"));
        assertAccepted(modelDoc(", \"withMetaUses\": [ { \"entryNames\": [ \"scheme\" ],"
                + " \"refusal\": \"nothing\" } ]"));
        assertAccepted(modelDoc(", \"withMetaUses\": [ { \"entryNames\": [ \"scheme\" ],"
                + " \"argumentType\": \"number\", \"typeArguments\": [ { \"parameter\":"
                + " \"digits\", \"literalValue\": \"18\" } ] } ]"));

        // the missing required member, the unknown member, and the two half-stated with-meta shapes
        assertRejected("{ \"irFormatVersion\": 1, \"nodes\": [ { \"kind\": \"MODEL\","
                + " \"name\": \"test.model\" } ] }");
        assertRejected(modelDoc(", \"bogus\": 1"));
        assertRejected(modelDoc(", \"withMetaUses\": [ { \"entryNames\": [ \"scheme\" ],"
                + " \"argumentType\": \"P\", \"refusal\": \"nothing\" } ]"));
        assertRejected(modelDoc(", \"withMetaUses\": [ { \"entryNames\": [ \"scheme\" ] } ]"));
        // a refused use carries no constraints, and a refusal token is one of the two
        assertRejected(modelDoc(", \"withMetaUses\": [ { \"refusal\": \"nothing\","
                + " \"typeArguments\": [ { \"parameter\": \"d\", \"literalValue\": \"1\" } ] } ]"));
        assertRejected(modelDoc(", \"withMetaUses\": [ { \"refusal\": \"dunno\" } ]"));
        // an inferred type's constraints are LITERAL, never a parameter passed through by name
        assertRejected(modelDoc(", \"withMetaUses\": [ { \"argumentType\": \"number\","
                + " \"typeArguments\": [ { \"parameter\": \"d\", \"nameValue\": \"n\" } ] } ]"));
        // a qualifiable configuration's kind is one of TWO tokens
        assertRejected(modelDoc(", \"qualifiableConfigs\": [ { \"kind\": \"IS_SOMETHING\","
                + " \"rootType\": \"E\" } ]"));
        // ... and its root is required
        assertRejected(modelDoc(", \"qualifiableConfigs\": [ { \"kind\": \"IS_EVENT\" } ]"));
    }

    /**
     * THE PROPERTY GATE'S DEPENDENT LAWS (PR #644): a {@code <prefix>AliasChain} rides the
     * {@code <prefix>EffectiveBase} law verbatim — it needs a {@code <prefix>Kind}
     * ({@code dependentRequired}) and that kind must read {@code TYPE_ALIAS}
     * ({@code dependentSchemas}); a link's {@code conditionKinds} tokens are the three the record
     * admits; and an {@code argumentType*} fact needs the {@code argumentType} NAME it is about.
     *
     * <p>Able to fail: drop the {@code dependsOn(prefix + "AliasChain", …)} entry or the
     * {@code prefix + "AliasChain"} {@code dependentSchemas} entry from
     * {@code IRJsonSchema.referenceFacts} — the matching rejected document starts validating.
     */
    @Test
    void theAliasChainAndTheConditionKindsAreStated() throws Exception {
        String chain = "[ { \"name\": \"Outer\", \"namespace\": \"ns\","
                + " \"parameterNames\": [ \"digits\" ],"
                + " \"conditionNames\": [ \"Positive\", null ],"
                + " \"conditionKinds\": [ \"DataRule\", \"OneOf\" ] }, { \"name\": \"Inner\" } ]";
        assertAccepted(fieldDoc(", \"typeKind\": \"TYPE_ALIAS\", \"typeResolvedName\": \"A\","
                + " \"typeAliasChain\": " + chain));
        assertRejected(fieldDoc(", \"typeAliasChain\": " + chain));
        assertRejected(fieldDoc(", \"typeKind\": \"STRUCT\", \"typeResolvedName\": \"S\","
                + " \"typeAliasChain\": " + chain));
        // a link needs its name, admits no unknown member, and its kinds are the three tokens
        assertRejected(fieldDoc(", \"typeKind\": \"TYPE_ALIAS\", \"typeResolvedName\": \"A\","
                + " \"typeAliasChain\": [ { \"namespace\": \"ns\" } ]"));
        assertRejected(fieldDoc(", \"typeKind\": \"TYPE_ALIAS\", \"typeResolvedName\": \"A\","
                + " \"typeAliasChain\": [ { \"name\": \"Outer\", \"bogus\": 1 } ]"));
        assertRejected(fieldDoc(", \"typeKind\": \"TYPE_ALIAS\", \"typeResolvedName\": \"A\","
                + " \"typeAliasChain\": [ { \"name\": \"Outer\","
                + " \"conditionKinds\": [ \"Bogus\" ] } ]"));
        // the base prefix obeys the same law, and a chain still needs a baseType NAME
        assertAccepted(aliasDoc("\"int\"", ", \"baseTypeKind\": \"TYPE_ALIAS\","
                + " \"baseTypeResolvedName\": \"int\", \"baseTypeAliasChain\": " + chain));
        assertRejected(aliasDoc("null", ", \"baseTypeKind\": \"TYPE_ALIAS\","
                + " \"baseTypeResolvedName\": \"int\", \"baseTypeAliasChain\": " + chain));
        // a type declaration's own condition kinds are the same three tokens
        assertAccepted(structDoc("null", ", \"conditionNames\": [ \"A\" ],"
                + " \"conditionKinds\": [ \"OneOf\" ]"));
        assertRejected(structDoc("null", ", \"conditionKinds\": [ \"Bogus\" ]"));
        // an argumentType* fact needs the argumentType NAME it is about
        assertRejected(modelDoc(", \"withMetaUses\": [ { \"refusal\": \"missing\","
                + " \"argumentTypeKind\": \"STRUCT\", \"argumentTypeResolvedName\": \"S\" } ]"));
    }

    /** A document whose single MODEL node carries the given {@code extra} members. */
    private static String modelDoc(String extra) {
        return "{ \"irFormatVersion\": 1, \"nodes\": [ { \"kind\": \"MODEL\","
                + " \"name\": \"test.model\", \"namespace\": \"test.model\"" + extra + " } ] }";
    }

    /** A document whose single TYPE_ALIAS node has the given raw {@code baseType} plus {@code extra} members. */
    private static String aliasDoc(String baseType, String extra) {
        return "{ \"irFormatVersion\": 1, \"nodes\": [ { \"kind\": \"TYPE_ALIAS\", \"name\": \"A\","
                + " \"baseType\": " + baseType + extra + " } ] }";
    }

    /** A document whose single STRUCT node carries one field, plus {@code extra} members on that field. */
    private static String fieldDoc(String extra) {
        return "{ \"irFormatVersion\": 1, \"nodes\": [ { \"kind\": \"STRUCT\", \"name\": \"S\","
                + " \"isAbstract\": false, \"baseType\": null, \"fields\": [ { \"kind\": \"FIELD\","
                + " \"name\": \"f\", \"type\": \"T\", \"cardinality\": \"ONE_TO_ONE\"" + extra
                + " } ] } ] }";
    }

    /** A document whose single STRUCT node has no fields; {@code baseType} is the raw JSON value. */
    private static String structDoc(String baseType, String extra) {
        return "{ \"irFormatVersion\": 1, \"nodes\": [ { \"kind\": \"STRUCT\", \"name\": \"S\","
                + " \"isAbstract\": false, \"baseType\": " + baseType + ", \"fields\": []" + extra
                + " } ] }";
    }

    /** A document whose single ENUM node has no values, plus {@code extra} members on it. */
    private static String enumDoc(String extra) {
        return "{ \"irFormatVersion\": 1, \"nodes\": [ { \"kind\": \"ENUM\", \"name\": \"E\","
                + " \"values\": []" + extra + " } ] }";
    }

    private void assertAccepted(String json) throws Exception {
        Set<ValidationMessage> messages = validate(json);
        assertTrue(messages.isEmpty(), "expected the schema to ACCEPT: " + json + "\n" + messages);
    }

    private void assertRejected(String json) throws Exception {
        Set<ValidationMessage> messages = validate(json);
        assertFalse(messages.isEmpty(), "expected the schema to REJECT: " + json);
    }
}
