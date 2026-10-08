package com.regnosys.rosetta.ir.json;

import com.regnosys.rosetta.ir.adapter.IRFieldNode;
import com.regnosys.rosetta.ir.adapter.IRMetadata;
import com.regnosys.rosetta.ir.adapter.IRModelNode;
import com.regnosys.rosetta.ir.adapter.IRTypeNode;
import com.regnosys.rosetta.ir.core.Cardinality;
import com.regnosys.rosetta.ir.core.IRAnnotationUse;
import com.regnosys.rosetta.ir.core.IRFunctionSignature;
import com.regnosys.rosetta.ir.core.IRKind;
import com.regnosys.rosetta.ir.core.IRNode;
import com.regnosys.rosetta.ir.core.IRQualifiableConfig;
import com.regnosys.rosetta.ir.core.IRQualificationFunction;
import com.regnosys.rosetta.ir.core.IRType;
import com.regnosys.rosetta.ir.core.IRTypeArgument;
import com.regnosys.rosetta.ir.core.IRWithMetaUse;
import com.regnosys.rosetta.testutil.IRSamples;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE MODEL NODE on the wire (v3.3 seat 8, PR #644 — the property gate): one round-trip per fact of
 * {@link IRKind#MODEL}, in the same style as {@code IRJsonDeclarationFactsRoundTripTest} — a node
 * carrying ONLY the fact under test is serialized, the wire member is asserted present BY NAME, and
 * the document is read back and compared to the original record ({@code equals}, so every other
 * component must come back untouched too).
 *
 * <p>Beside the round-trips sit the REFUSALS: every law the records state
 * ({@link IRQualifiableConfig#KINDS}, {@link IRWithMetaUse}'s exactly-one and its refusal
 * vocabulary, the model's name-IS-its-namespace) must be re-raised by the reader naming the
 * offending document path, and the "no member when empty" law must hold, so a v1 document — which
 * carries no MODEL node at all — is byte-unchanged.
 */
class IRJsonModelNodeRoundTripTest {

    private static final IRJsonSerializer SER = new IRJsonSerializer();
    private static final IRJsonDeserializer DE = new IRJsonDeserializer();

    private static final String NS = "test.model";

    // ── the round-trips ────────────────────────────────────────────────────────────────────────

    /**
     * The fully-populated sample: EVERY model fact at once — a definition, a version, both
     * qualifiable kinds, a qualification function, a function signature with annotated input and
     * output, and three with-meta uses (a declared type, a builtin with literal constraints, a
     * REFUSED one). It round-trips to the same record AND to the same bytes.
     *
     * <p>Able to fail: drop any member from {@code modelJson} or from {@code modelNode} in the
     * reader — either the record comparison or the byte comparison fails.
     */
    @Test
    void theModelNodeCarriesEveryFactAndRoundTrips() {
        IRNode model = IRSamples.model();
        assertCarries(model, "kind", "name", "namespace", "definition", "version",
                "qualifiableConfigs", "rootType", "qualificationFunctions", "firstInputType",
                "functionSignatures", "inputs", "output", "withMetaUses", "entryNames",
                "argumentType", "typeArguments", "refusal");
        String json = SER.toJson(model);
        assertEquals(json, SER.toJson(DE.fromJsonNode(json)), "the model document is not byte-stable");
        assertEquals(IRKind.MODEL, model.kind());
    }

    /** The whole-document route too: a model node rides {@code nodes[]} like any other declaration. */
    @Test
    void theModelNodeRoundTripsInsideAVersionedDocument() {
        String doc = SER.toJson(List.of(IRSamples.model()));
        assertEquals(doc, SER.toJson(DE.fromJson(doc)), "the model document is not byte-stable");
        assertEquals(List.of(IRSamples.model()), DE.fromJson(doc));
    }

    /** Each optional fact rides ON ITS OWN — no member depends on another being present. */
    @Test
    void eachModelFactRidesOnItsOwn() {
        assertCarries(model().definition("the trade model").build(), "definition");
        assertCarries(model().version("1.2.3").build(), "version");
        assertCarries(model()
                        .qualifiableConfigs(new IRQualifiableConfig("IS_PRODUCT", structRef("Product")))
                        .build(),
                "qualifiableConfigs", "rootType", "rootTypeKind", "rootTypeNamespace",
                "rootTypeResolvedName");
        assertCarries(model()
                        .qualificationFunctions(
                                new IRQualificationFunction("Qualify_Swap", structRef("Product")))
                        .build(),
                "qualificationFunctions", "firstInputType", "firstInputTypeKind",
                "firstInputTypeResolvedName");
        assertCarries(model()
                        .functionSignatures(new IRFunctionSignature("Create_Trade",
                                List.of(annotatedField("in", "Product")),
                                Optional.of(annotatedField("out", "Trade"))))
                        .build(),
                "functionSignatures", "inputs", "output", "annotations");
        assertCarries(model()
                        .withMetaUses(new IRWithMetaUse(List.of("scheme", "reference"),
                                Optional.of(structRef("Product")), List.of(), Optional.empty()))
                        .build(),
                "withMetaUses", "entryNames", "argumentType", "argumentTypeKind");
    }

    /**
     * A function with NO inputs and no output writes neither member, and a signature's inputs are
     * FIELD nodes — the same shape a declared attribute has, because the wrapper collector reads
     * their annotations.
     */
    @Test
    void aFunctionSignaturesInputsAndOutputAreFieldNodes() {
        IRModelNode outputless = model()
                .functionSignatures(new IRFunctionSignature("NoOp", List.of(), Optional.empty()))
                .build();
        String json = SER.toJson(outputless);
        assertFalse(json.contains("\"inputs\""), json);
        assertFalse(json.contains("\"output\""), json);
        assertEquals(outputless, DE.fromJsonNode(json));

        IRModelNode full = model()
                .functionSignatures(new IRFunctionSignature("Create_Trade",
                        List.of(annotatedField("in", "Product")),
                        Optional.of(annotatedField("out", "Trade"))))
                .build();
        String fullJson = SER.toJson(full);
        assertTrue(fullJson.contains("\"kind\": \"FIELD\""),
                "a signature's inputs are FIELD nodes:\n" + fullJson);
        assertEquals(full, DE.fromJsonNode(fullJson));
    }

    /**
     * A builtin argument carries the LITERAL constraints in force beside it; a REFUSED use carries
     * a token instead of a type and no constraints at all. Both round-trip.
     */
    @Test
    void aBuiltinArgumentAndARefusedWithMetaUseBothRoundTrip() {
        IRModelNode builtin = model()
                .withMetaUses(new IRWithMetaUse(List.of("scheme"),
                        Optional.of(IRTypeNode.reference("number", IRKind.BASIC_TYPE,
                                Optional.empty(), Optional.of("number"))),
                        List.of(new IRTypeArgument("digits", Optional.empty(), Optional.of("18"), false),
                                new IRTypeArgument("min", Optional.empty(), Optional.of("1.5"), true)),
                        Optional.empty()))
                .build();
        assertCarries(builtin, "typeArguments", "literalValue", "negated");

        IRModelNode refused = model()
                .withMetaUses(new IRWithMetaUse(List.of("reference"), Optional.empty(), List.of(),
                        Optional.of("nothing")))
                .build();
        String json = SER.toJson(refused);
        assertTrue(json.contains("\"refusal\": \"nothing\""), json);
        assertFalse(json.contains("\"argumentType\""),
                "a refused use carries no argument type:\n" + json);
        assertEquals(refused, DE.fromJsonNode(json));
    }

    /**
     * THE NON-DEFAULT RULE: a model with nothing to say is exactly three members long, so nothing
     * the emitter has not measured leaks into the wire form.
     *
     * <p>Able to fail: write any of the seven optional members unconditionally in
     * {@code modelJson} — the byte comparison fails.
     */
    @Test
    void aModelWithNoFactsWritesOnlyItsThreeRequiredMembers() {
        IRModelNode bare = new IRModelNode(NS);
        assertEquals("""
                {
                  "kind": "MODEL",
                  "name": "test.model",
                  "namespace": "test.model"
                }
                """, SER.toJson(bare));
        assertEquals(bare, DE.fromJsonNode(SER.toJson(bare)));
    }

    // ── the refusals ───────────────────────────────────────────────────────────────────────────

    /** A model node's name IS its namespace: two different strings could not round-trip, so they are refused. */
    @Test
    void aModelNodeWhoseNamespaceDisagreesWithItsNameIsRefused() {
        String json = modelDoc("\"namespace\": \"other.model\"");
        assertTrue(assertThrows(IRJsonException.class, () -> DE.fromJsonNode(json)).getMessage()
                .contains("a model node's name IS its namespace"), json);
    }

    /** Both required members are required. */
    @Test
    void aModelNodeWithoutItsNamespaceIsRefused() {
        String json = "{ \"kind\": \"MODEL\", \"name\": \"test.model\" }";
        assertTrue(assertThrows(IRJsonException.class, () -> DE.fromJsonNode(json)).getMessage()
                .contains("missing member 'namespace'"), json);
    }

    /** An unknown member on the model node is refused, like on every other node. */
    @Test
    void anUnknownMemberOnTheModelNodeIsRefused() {
        String json = modelDoc("\"namespace\": \"test.model\"", "\"bogus\": 1");
        assertTrue(assertThrows(IRJsonException.class, () -> DE.fromJsonNode(json)).getMessage()
                .contains("unknown member 'bogus'"), json);
    }

    /** A qualifiable configuration's kind is one of TWO tokens — anything else is refused AT ITS OWN PATH. */
    @Test
    void aQualifiableConfigKindOutsideTheTwoIsRefusedAtItsPath() {
        String json = modelDoc("\"namespace\": \"test.model\"",
                "\"qualifiableConfigs\": [ { \"kind\": \"IS_SOMETHING\", \"rootType\": \"Product\" } ]");
        String message = assertThrows(IRJsonException.class, () -> DE.fromJsonNode(json)).getMessage();
        assertTrue(message.contains("IS_SOMETHING")
                && message.endsWith("at $.qualifiableConfigs[0].kind"), message);
    }

    /** A with-meta use carrying BOTH an argument type and a refusal is refused at the USE's path. */
    @Test
    void aWithMetaUseWithBothAnArgumentTypeAndARefusalIsRefusedAtItsPath() {
        String json = modelDoc("\"namespace\": \"test.model\"",
                "\"withMetaUses\": [ { \"entryNames\": [ \"scheme\" ],"
                        + " \"argumentType\": \"Product\", \"refusal\": \"nothing\" } ]");
        String message = assertThrows(IRJsonException.class, () -> DE.fromJsonNode(json)).getMessage();
        assertTrue(message.contains("BOTH an argument type and a refusal")
                && message.endsWith("at $.withMetaUses[0]"), message);
    }

    /** ... and one carrying NEITHER is refused at the same path — exactly one is the law. */
    @Test
    void aWithMetaUseWithNeitherIsRefusedAtItsPath() {
        String json = modelDoc("\"namespace\": \"test.model\"",
                "\"withMetaUses\": [ { \"entryNames\": [ \"scheme\" ] } ]");
        String message = assertThrows(IRJsonException.class, () -> DE.fromJsonNode(json)).getMessage();
        assertTrue(message.contains("NEITHER an argument type nor a refusal")
                && message.endsWith("at $.withMetaUses[0]"), message);
    }

    /** A refusal token outside the vocabulary is refused; a refused use carrying constraints is too. */
    @Test
    void aMalformedRefusalIsRefusedAtTheUsesPath() {
        String unknownToken = modelDoc("\"namespace\": \"test.model\"",
                "\"withMetaUses\": [ { \"entryNames\": [ \"scheme\" ], \"refusal\": \"dunno\" } ]");
        assertTrue(assertThrows(IRJsonException.class, () -> DE.fromJsonNode(unknownToken))
                .getMessage().contains("dunno"), unknownToken);

        String withConstraints = modelDoc("\"namespace\": \"test.model\"",
                "\"withMetaUses\": [ { \"entryNames\": [ \"scheme\" ], \"refusal\": \"nothing\","
                        + " \"typeArguments\": [ { \"parameter\": \"digits\","
                        + " \"literalValue\": \"3\" } ] } ]");
        assertTrue(assertThrows(IRJsonException.class, () -> DE.fromJsonNode(withConstraints))
                .getMessage().contains("refused with-meta use carries no type arguments"),
                withConstraints);
    }

    /** An inferred type's constraints are LITERAL: a parameter passed through by name is refused. */
    @Test
    void aWithMetaConstraintPassingAParameterThroughByNameIsRefused() {
        String json = modelDoc("\"namespace\": \"test.model\"",
                "\"withMetaUses\": [ { \"entryNames\": [ \"scheme\" ], \"argumentType\": \"number\","
                        + " \"argumentTypeKind\": \"BASIC_TYPE\", \"argumentTypeResolvedName\": \"number\","
                        + " \"typeArguments\": [ { \"parameter\": \"digits\","
                        + " \"nameValue\": \"d\" } ] } ]");
        assertTrue(assertThrows(IRJsonException.class, () -> DE.fromJsonNode(json)).getMessage()
                .contains("passes a parameter through by name"), json);
    }

    /** An {@code argumentType*} fact without the NAME it is about is refused, as an orphan base fact is. */
    @Test
    void anArgumentTypeFactWithoutItsArgumentTypeIsRefused() {
        String json = modelDoc("\"namespace\": \"test.model\"",
                "\"withMetaUses\": [ { \"entryNames\": [ \"scheme\" ], \"refusal\": \"missing\","
                        + " \"argumentTypeKind\": \"STRUCT\" } ]");
        assertTrue(assertThrows(IRJsonException.class, () -> DE.fromJsonNode(json)).getMessage()
                .contains("member 'argumentTypeKind' without an 'argumentType'"), json);
    }

    /** A function signature's input must BE a field node — any other kind there is refused by name. */
    @Test
    void aFunctionSignatureInputThatIsNotAFieldIsRefused() {
        String json = modelDoc("\"namespace\": \"test.model\"",
                "\"functionSignatures\": [ { \"name\": \"F\", \"inputs\": [ { \"kind\": \"ENUM\","
                        + " \"name\": \"E\", \"values\": [] } ] } ]");
        String message = assertThrows(IRJsonException.class, () -> DE.fromJsonNode(json)).getMessage();
        assertTrue(message.contains("expected a FIELD")
                && message.endsWith("at $.functionSignatures[0].inputs[0]"), message);
    }

    // ── helpers ────────────────────────────────────────────────────────────────────────────────

    /** Serializes, asserts each member appears by name, then asserts the record came back equal. */
    private static void assertCarries(IRNode node, String... members) {
        String json = SER.toJson(node);
        for (String member : members) {
            assertTrue(json.contains("\"" + member + "\""),
                    "member '" + member + "' missing from:\n" + json);
        }
        assertEquals(node, DE.fromJsonNode(json), "round-trip drifted for:\n" + json);
    }

    /** A bare MODEL fragment in the writer's own byte form; {@code members} follow {@code name}. */
    private static String modelDoc(String... members) {
        return "{\n  \"kind\": \"MODEL\",\n  \"name\": \"" + NS + "\",\n  "
                + String.join(",\n  ", members) + "\n}\n";
    }

    private static IRType structRef(String name) {
        return IRTypeNode.reference(name, IRKind.STRUCT, Optional.of(NS), Optional.of(name));
    }

    /** A field carrying a {@code [metadata scheme]} annotation — what the wrapper collector reads. */
    private static IRFieldNode annotatedField(String name, String typeName) {
        return new IRFieldNode(name, structRef(typeName), Cardinality.ONE_TO_ONE, Optional.empty(),
                IRMetadata.EMPTY, Optional.empty(), false, List.of(), Optional.empty(), List.of(),
                List.of(new IRAnnotationUse("metadata", Optional.of("scheme"), List.of())),
                List.of(), List.of());
    }

    private static Model model() {
        return new Model();
    }

    /** Every model fact at its empty default; each test sets exactly one. */
    private static final class Model {
        private Optional<String> definition = Optional.empty();
        private Optional<String> version = Optional.empty();
        private List<IRQualifiableConfig> qualifiableConfigs = List.of();
        private List<IRQualificationFunction> qualificationFunctions = List.of();
        private List<IRFunctionSignature> functionSignatures = List.of();
        private List<IRWithMetaUse> withMetaUses = List.of();

        Model definition(String value) { definition = Optional.of(value); return this; }
        Model version(String value) { version = Optional.of(value); return this; }

        Model qualifiableConfigs(IRQualifiableConfig... values) {
            qualifiableConfigs = List.of(values);
            return this;
        }

        Model qualificationFunctions(IRQualificationFunction... values) {
            qualificationFunctions = List.of(values);
            return this;
        }

        Model functionSignatures(IRFunctionSignature... values) {
            functionSignatures = List.of(values);
            return this;
        }

        Model withMetaUses(IRWithMetaUse... values) {
            withMetaUses = List.of(values);
            return this;
        }

        IRModelNode build() {
            return new IRModelNode(NS, definition, version, qualifiableConfigs, qualificationFunctions,
                    functionSignatures, withMetaUses, Optional.empty(), IRMetadata.EMPTY);
        }
    }
}
