package com.regnosys.rosetta.ir.json;

import com.regnosys.rosetta.ir.adapter.IREnumNode;
import com.regnosys.rosetta.ir.adapter.IREnumValueNode;
import com.regnosys.rosetta.ir.adapter.IRFieldNode;
import com.regnosys.rosetta.ir.adapter.IRMetadata;
import com.regnosys.rosetta.ir.adapter.IRTypeNode;
import com.regnosys.rosetta.ir.core.Cardinality;
import com.regnosys.rosetta.ir.core.IRAliasLink;
import com.regnosys.rosetta.ir.core.IRAnnotationPath;
import com.regnosys.rosetta.ir.core.IRAnnotationUse;
import com.regnosys.rosetta.ir.core.IRBounds;
import com.regnosys.rosetta.ir.core.IRDocReference;
import com.regnosys.rosetta.ir.core.IREffectiveBase;
import com.regnosys.rosetta.ir.core.IREnumSynonym;
import com.regnosys.rosetta.ir.core.IREnumValue;
import com.regnosys.rosetta.ir.core.IRField;
import com.regnosys.rosetta.ir.core.IRKind;
import com.regnosys.rosetta.ir.core.IRLabel;
import com.regnosys.rosetta.ir.core.IRNode;
import com.regnosys.rosetta.ir.core.IRRuleReference;
import com.regnosys.rosetta.ir.core.IRType;
import com.regnosys.rosetta.ir.core.IRTypeArgument;
import com.regnosys.rosetta.ir.core.IRTypeParameter;
import com.regnosys.rosetta.testutil.IRSamples;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * One round-trip per declaration fact of the declaration-IR enrichment (decision D55): a node
 * carrying ONLY the fact under test at a non-default value is serialized, the wire member is
 * asserted present BY NAME in the JSON text, and the document is read back and compared to the
 * original record ({@code equals}, so every other component must come back untouched too).
 *
 * <p>The last test is the backward-compatibility law itself: a node built through a pre-enrichment
 * (old-arity) constructor serializes to EXACTLY the v1 bytes — the literal string is the one the
 * pre-enrichment declaration tests pin — and a v1 document carrying none of the new members reads
 * every new fact back at its empty default.
 */
class IRJsonDeclarationFactsRoundTripTest {

    private static final IRJsonSerializer SER = new IRJsonSerializer();
    private static final IRJsonDeserializer DE = new IRJsonDeserializer();

    // ── the wire members, one test each ────────────────────────────────────────────────────────

    @Test
    void typeNamespace() {
        assertCarries(new Struct().namespace("test.model").build(), "namespace");
    }

    @Test
    void typeResolvedNameOnADeclaration() {
        assertCarries(new Struct().resolvedName("T").build(), "resolvedName");
    }

    @Test
    void typeResolvedNameOnAReferenceThroughAFieldsType() {
        IRField field = new Field()
                .type(IRTypeNode.reference("Direction", IRKind.ENUM,
                        Optional.of("test.model"), Optional.of("DirectionEnum")))
                .build();
        assertCarries(new Struct().fields(field).build(),
                "typeKind", "typeNamespace", "typeResolvedName");
    }

    /**
     * The referent's TRUE kind, for every kind a type reference can resolve to - the schema's OWN list,
     * so the test is driven from it and never from a typed copy.
     */
    static List<IRKind> referenceableKinds() {
        assertEquals(7, IRJsonSchema.TYPE_REFERENCE_KINDS.length, "the schema's referenceable kinds");
        return List.of(IRJsonSchema.TYPE_REFERENCE_KINDS);
    }

    @ParameterizedTest(name = "typeKind {0}")
    @MethodSource("referenceableKinds")
    void referenceKindIsCarriedForEveryReferenceableKind(IRKind referentKind) {
        IRField field = new Field()
                .type(IRTypeNode.reference("Ref", referentKind,
                        Optional.of("test.model"), Optional.of("Referent")))
                .build();
        String json = SER.toJson(field);
        assertTrue(json.contains("\"typeKind\": \"" + referentKind.name() + "\""),
                "typeKind missing or wrong for " + referentKind + ":\n" + json);
        assertEquals(field, DE.fromJsonNode(json), "round-trip drifted for " + referentKind);
    }

    /** The reader admits EXACTLY the schema's seven: every other {@link IRKind} is refused as a reference's kind. */
    @ParameterizedTest(name = "typeKind {0} refused")
    @EnumSource(IRKind.class)
    void aKindOutsideTheSchemasSevenIsRefusedAsAReferenceKind(IRKind kind) {
        if (referenceableKinds().contains(kind)) {
            return;
        }
        String json = SER.toJson(new Field().type(IRTypeNode.reference("Ref", IRKind.CHOICE,
                Optional.of("test.model"), Optional.of("Referent"))).build())
                .replace("\"typeKind\": \"CHOICE\"", "\"typeKind\": \"" + kind.name() + "\"");
        assertTrue(json.contains("\"typeKind\": \"" + kind.name() + "\""), json);
        IRJsonException refused = assertThrows(IRJsonException.class, () -> DE.fromJsonNode(json));
        assertTrue(refused.getMessage().contains("cannot be a type reference's kind"), refused.getMessage());
    }

    @Test
    void aBaseTypeKindOutsideTheSchemasSevenIsRefused() {
        String json = SER.toJson(new Struct().baseType(IRTypeNode.reference("Base", IRKind.CHOICE,
                Optional.of("test.model"), Optional.of("BaseChoice"))).build())
                .replace("\"baseTypeKind\": \"CHOICE\"", "\"baseTypeKind\": \"FIELD\"");
        assertTrue(json.contains("\"baseTypeKind\": \"FIELD\""), json);
        assertThrows(IRJsonException.class, () -> DE.fromJsonNode(json));
    }

    /**
     * The BUILTIN-FALLBACK shape (a workspace without the model library): a resolved name and NO namespace. It
     * round-trips, and {@link IRType#resolvedQualifiedName()} reads empty for it - there is no namespace to qualify by.
     */
    @Test
    void theBuiltinFallbackShapeRoundTrips() {
        IRType builtin = IRTypeNode.reference("string", IRKind.BASIC_TYPE, Optional.empty(), Optional.of("string"));
        IRField field = new Field().type(builtin).build();
        String json = SER.toJson(field);
        assertTrue(json.contains("\"typeResolvedName\": \"string\""), json);
        assertFalse(json.contains("\"typeNamespace\""), json);
        IRField read = (IRField) DE.fromJsonNode(json);
        assertEquals(field, read, "round-trip drifted for the builtin-fallback shape");
        assertEquals(Optional.empty(), read.type().namespace());
        assertEquals(Optional.of("string"), read.type().resolvedName());
        assertEquals(Optional.empty(), read.type().resolvedQualifiedName());
    }

    // ── cq SF-3: ONE law per dependent member, shared by the writer, the reader and the schema ──
    //
    // The writer's law (IRJsonSerializer.addReferenceFacts) is the canonical one: a reference's
    // `<prefix>Kind` is emitted EXACTLY when the reference RESOLVED, i.e. when `<prefix>Namespace`
    // or `<prefix>ResolvedName` is emitted. Either half alone is a document the writer never
    // produces and cannot read back unchanged, so the reader refuses it and the schema states it
    // (IRJsonSchemaValidationTest holds the schema half of each pair below).

    /**
     * cq SF-3 (a) REFUSAL: a {@code typeKind} with NEITHER resolved string. Until this law it was
     * ACCEPTED and re-serialized WITHOUT the kind — the stated kind was silently dropped.
     *
     * <p>Able to fail: delete the {@code kindToken.isPresent() && !resolved} arm of
     * {@code IRJsonDeserializer.typeRef} — the document is read again and nothing throws.
     */
    @Test
    void aReferenceKindWithNoResolvedFactIsRefused() {
        String json = fieldDoc("\"typeKind\": \"ENUM\"");
        IRJsonException refused = assertThrows(IRJsonException.class, () -> DE.fromJsonNode(json));
        assertTrue(refused.getMessage().contains("member 'typeKind' without a 'typeNamespace'"),
                refused.getMessage());
    }

    /**
     * cq SF-3 (a) ROUND-TRIP: the shape the writer DOES emit for an unresolved reference — the
     * written name alone, no kind — is read back and re-serialized byte for byte.
     *
     * <p>Able to fail: make {@code addReferenceFacts} emit {@code <prefix>Kind} unconditionally —
     * the re-serialized text gains a member the input never carried.
     */
    @Test
    void anUnresolvedReferenceRoundTripsWithoutAKind() {
        String json = fieldDoc();
        assertEquals(json, SER.toJson(DE.fromJsonNode(json)));
    }

    /**
     * cq SF-3 (b) REFUSAL: a resolved string with no {@code typeKind}. Until this law it was
     * ACCEPTED, the reader substituted the placeholder {@code STRUCT}, and the document
     * re-serialized WITH a {@code "typeKind": "STRUCT"} it never stated.
     *
     * <p>Able to fail: delete the {@code kindToken.isEmpty() && resolved} arm of
     * {@code IRJsonDeserializer.typeRef}.
     */
    @ParameterizedTest(name = "{0} without a typeKind")
    @ValueSource(strings = {"typeNamespace", "typeResolvedName"})
    void aResolvedFactWithNoReferenceKindIsRefused(String member) {
        String json = fieldDoc("\"" + member + "\": \"x\"");
        IRJsonException refused = assertThrows(IRJsonException.class, () -> DE.fromJsonNode(json));
        assertTrue(refused.getMessage().contains("member '" + member + "' without a 'typeKind'"),
                refused.getMessage());
    }

    /**
     * cq SF-3 (b) ROUND-TRIP: the shape the writer DOES emit for a resolved reference — the kind
     * beside its resolved facts — is read back and re-serialized byte for byte.
     *
     * <p>Able to fail: make {@code addReferenceFacts} skip {@code <prefix>Kind}; the re-serialized
     * text then loses the member.
     */
    @Test
    void aResolvedReferenceRoundTripsWithItsKind() {
        String json = fieldDoc("\"typeKind\": \"ENUM\"", "\"typeNamespace\": \"test.model\"",
                "\"typeResolvedName\": \"Direction\"");
        assertEquals(json, SER.toJson(DE.fromJsonNode(json)));
    }

    /**
     * The {@code baseType} prefix obeys the SAME law, and its facts additionally need a
     * {@code baseType} that is a name rather than JSON {@code null}. The two legal shapes — resolved
     * and unresolved — round-trip byte for byte.
     *
     * <p>Able to fail: delete either arm of {@code typeRef} or the {@code baseName.isEmpty()} guard
     * in {@code IRJsonDeserializer.typeNode}.
     */
    @Test
    void theBaseTypeReferenceObeysTheSameLaw() {
        String orphan = structDoc("null", "\"baseTypeKind\": \"STRUCT\"",
                "\"baseTypeNamespace\": \"test.model\"");
        assertTrue(assertThrows(IRJsonException.class, () -> DE.fromJsonNode(orphan)).getMessage()
                .contains("member 'baseTypeKind' without a 'baseType'"), orphan);
        String kindOnly = structDoc("\"Base\"", "\"baseTypeKind\": \"STRUCT\"");
        assertTrue(assertThrows(IRJsonException.class, () -> DE.fromJsonNode(kindOnly)).getMessage()
                .contains("member 'baseTypeKind' without a 'baseTypeNamespace'"), kindOnly);
        String namespaceOnly = structDoc("\"Base\"", "\"baseTypeNamespace\": \"test.model\"");
        assertTrue(assertThrows(IRJsonException.class, () -> DE.fromJsonNode(namespaceOnly)).getMessage()
                .contains("member 'baseTypeNamespace' without a 'baseTypeKind'"), namespaceOnly);
        String resolved = structDoc("\"Base\"", "\"baseTypeKind\": \"STRUCT\"",
                "\"baseTypeNamespace\": \"test.model\"", "\"baseTypeResolvedName\": \"Base\"");
        assertEquals(resolved, SER.toJson(DE.fromJsonNode(resolved)));
        String unresolved = structDoc("\"Base\"");
        assertEquals(unresolved, SER.toJson(DE.fromJsonNode(unresolved)));
    }

    /**
     * cq SF-3 (c) — an enum's {@code parent*} fact with no {@code parent}. The reader already
     * refused it (the schema did not); the message now NAMES the offending member.
     *
     * <p>Able to fail: delete the {@code parentName.isEmpty()} guard in
     * {@code IRJsonDeserializer.enumNode}.
     */
    @ParameterizedTest(name = "{0} without a parent")
    @ValueSource(strings = {"parentNamespace", "parentResolvedName"})
    void anEnumParentFactWithoutAParentIsRefused(String member) {
        String json = "{\n  \"kind\": \"ENUM\",\n  \"name\": \"test.model.E\",\n  \"values\": [],\n"
                + "  \"" + member + "\": \"x\"\n}\n";
        IRJsonException refused = assertThrows(IRJsonException.class, () -> DE.fromJsonNode(json));
        assertTrue(refused.getMessage().contains("member '" + member + "' without a 'parent'"),
                refused.getMessage());
    }

    /**
     * cq SF-3 (c) — {@code negated} beside a {@code nameValue}: the record's law, re-raised by the
     * reader with the document path, and now stated by the schema too.
     *
     * <p>Able to fail: drop the {@code negated && literalValue.isEmpty()} guard from
     * {@code IRTypeArgument} — the document is read and nothing throws.
     */
    @Test
    void aNegatedTypeArgumentWithoutALiteralIsRefused() {
        String json = fieldDoc("\"typeArguments\": [ { \"parameter\": \"digits\","
                + " \"nameValue\": \"n\", \"negated\": true } ]");
        IRJsonException refused = assertThrows(IRJsonException.class, () -> DE.fromJsonNode(json));
        assertTrue(refused.getMessage().contains("negated without a literal value"), refused.getMessage());
        assertTrue(refused.getMessage().endsWith("at $.typeArguments[0]"), refused.getMessage());
    }

    // ── cq NIT-7: the IRBounds invariants ───────────────────────────────────────────────────────

    /**
     * The record's own law: a bound pair is non-negative and not inverted, and the message names the
     * PAIR. No vendored or chaos cell declares either shape, so the invariant is byte-neutral.
     *
     * <p>Able to fail: drop either guard from the {@code IRBounds} compact constructor — the
     * construction succeeds and nothing throws.
     */
    @Test
    void boundsRefuseANegativeLowerAndAnInvertedPair() {
        IllegalArgumentException negative = assertThrows(IllegalArgumentException.class,
                () -> new IRBounds(BigInteger.valueOf(-1), Optional.empty()));
        assertTrue(negative.getMessage().contains("(-1..*)"), negative.getMessage());
        IllegalArgumentException inverted = assertThrows(IllegalArgumentException.class,
                () -> new IRBounds(BigInteger.TWO, Optional.of(BigInteger.ONE)));
        assertTrue(inverted.getMessage().contains("(2..1)"), inverted.getMessage());
    }

    /** The shapes the model DOES declare stay accepted — the invariant refuses nothing real. */
    @Test
    void theDeclarableBoundShapesAreAccepted() {
        assertEquals("(0..*)", new IRBounds(BigInteger.ZERO, Optional.empty()).display());
        assertEquals("(2..2)", new IRBounds(BigInteger.TWO, Optional.of(BigInteger.TWO)).display());
        assertEquals("(0..1)", new IRBounds(BigInteger.ZERO, Optional.of(BigInteger.ONE)).display());
    }

    /**
     * The reader re-raises both bound rejections as an {@code IRJsonException} naming the document
     * path — it must not leak the record's raw {@code IllegalArgumentException} to a caller that
     * catches only the JSON exception.
     *
     * <p>Able to fail: remove the {@code try}/{@code catch} from {@code IRJsonDeserializer.bounds}
     * — an {@code IllegalArgumentException} escapes and {@code assertThrows} fails.
     */
    @ParameterizedTest(name = "malformed bounds [{index}]")
    @ValueSource(strings = {"{ \"lower\": -1 }", "{ \"lower\": 2, \"upper\": 1 }"})
    void aMalformedBoundsPairIsRefusedByTheReader(String bounds) {
        String json = fieldDoc("\"bounds\": " + bounds);
        IRJsonException refused = assertThrows(IRJsonException.class, () -> DE.fromJsonNode(json));
        assertTrue(refused.getMessage().contains("cardinality bounds"), refused.getMessage());
        assertTrue(refused.getMessage().endsWith("at $.bounds"), refused.getMessage());
    }

    /** A flag is written ONLY when set (the schema pins it {@code const true}): an explicit {@code false} is refused. */
    @Test
    void anExplicitFalseFlagIsRefused() {
        String json = SER.toJson(new Field().override().build());
        assertTrue(json.contains("\"isOverride\": true"), json);
        assertEquals(new Field().override().build(), DE.fromJsonNode(json));
        String explicitFalse = json.replace("\"isOverride\": true", "\"isOverride\": false");
        IRJsonException refused = assertThrows(IRJsonException.class, () -> DE.fromJsonNode(explicitFalse));
        assertTrue(refused.getMessage().contains("isOverride"), refused.getMessage());
    }

    /** EXACTLY ONE of nameValue / literalValue: the record refuses both and neither, the reader re-raises it. */
    @Test
    void aTypeArgumentCarriesExactlyOneValue() {
        assertThrows(IllegalArgumentException.class,
                () -> new IRTypeArgument("digits", Optional.of("n"), Optional.of("5"), false));
        assertThrows(IllegalArgumentException.class,
                () -> new IRTypeArgument("digits", Optional.empty(), Optional.empty(), false));
        assertThrows(IllegalArgumentException.class,
                () -> new IRTypeArgument("digits", Optional.of("n"), Optional.empty(), true));
        String json = SER.toJson(new Field().typeArguments(
                new IRTypeArgument("digits", Optional.of("n"), Optional.empty(), false)).build());
        String both = json.replace("\"nameValue\": \"n\"", "\"nameValue\": \"n\",\n" + indentOf(json, "\"nameValue\"")
                + "\"literalValue\": \"5\"");
        assertTrue(both.contains("\"literalValue\": \"5\""), both);
        IRJsonException refused = assertThrows(IRJsonException.class, () -> DE.fromJsonNode(both));
        assertTrue(refused.getMessage().contains("exactly one is the law"), refused.getMessage());
    }

    private static String indentOf(String json, String member) {
        int at = json.indexOf(member);
        int lineStart = json.lastIndexOf('\n', at) + 1;
        return json.substring(lineStart, at);
    }

    @Test
    void baseTypeResolvedFacts() {
        IRType base = IRTypeNode.reference("Base", IRKind.CHOICE,
                Optional.of("test.model"), Optional.of("BaseChoice"));
        assertCarries(new Struct().baseType(base).build(),
                "baseTypeKind", "baseTypeNamespace", "baseTypeResolvedName");
    }

    @Test
    void typeDefinition() {
        assertCarries(new Struct().definition("what the type is").build(), "definition");
    }

    @Test
    void typeDocReferences() {
        assertCarries(new Struct().docReferences(IRSamples.fullDocReference()).build(),
                "docReferences", "regulatory", "path", "rootItem", "steps", "deep", "body",
                "corpora", "reference", "resolved", "typeKeyword", "displayName", "segments",
                "rationales", "text", "author", "structuredProvision", "provision",
                "reportedField", "namedArgs");
    }

    @Test
    void typeAnnotations() {
        assertCarries(new Struct().annotations(
                        new IRAnnotationUse("metadata", Optional.of("scheme")),
                        new IRAnnotationUse("deprecated", Optional.empty(),
                                List.of(new IRAnnotationUse.Argument("reason", "old", false),
                                        new IRAnnotationUse.Argument("replacement", "other", true))))
                        .build(),
                "annotations", "qualifier", "arguments", "key", "value", "attributeRef");
    }

    @Test
    void typeConditionNames() {
        // a named condition and an unnamed one — the unnamed is JSON null
        IRTypeNode type = new Struct().conditionNames(Optional.of("Named"), Optional.empty()).build();
        String json = SER.toJson(type);
        assertTrue(json.contains("\"conditionNames\""), "conditionNames missing:\n" + json);
        assertTrue(json.contains("\"Named\",\n    null"),
                "the unnamed condition is not the array's own JSON null:\n" + json);
        assertEquals(type, DE.fromJsonNode(json));
    }

    @Test
    void fieldBoundsBounded() {
        assertCarries(new Field()
                        .bounds(new IRBounds(BigInteger.ZERO, Optional.of(BigInteger.TWO))).build(),
                "bounds", "lower", "upper");
    }

    @Test
    void fieldBoundsUnbounded() {
        IRField field = new Field()
                .bounds(new IRBounds(BigInteger.TWO, Optional.empty())).build();
        String json = SER.toJson(field);
        assertTrue(json.contains("\"lower\""), "lower missing:\n" + json);
        assertFalse(json.contains("\"upper\""), "upper must be omitted when unbounded:\n" + json);
        assertEquals(field, DE.fromJsonNode(json));
    }

    @Test
    void fieldIsOverride() {
        assertCarries(new Field().override().build(), "isOverride");
    }

    @Test
    void fieldTypeArguments() {
        assertCarries(new Field().typeArguments(
                        new IRTypeArgument("digits", Optional.of("n"), Optional.empty(), false),
                        new IRTypeArgument("min", Optional.empty(), Optional.of("1.5"), true)).build(),
                "typeArguments", "parameter", "nameValue", "literalValue", "negated");
    }

    @Test
    void fieldDefinition() {
        assertCarries(new Field().definition("what the field is").build(), "definition");
    }

    @Test
    void fieldDocReferences() {
        assertCarries(new Field().docReferences(IRSamples.fullDocReference()).build(),
                "docReferences", "corpora", "segments");
    }

    @Test
    void fieldAnnotations() {
        assertCarries(new Field().annotations(
                new IRAnnotationUse("metadata", Optional.of("reference"))).build(),
                "annotations", "qualifier");
    }

    @Test
    void fieldLabels() {
        assertCarries(new Field().labels(
                        new IRLabel("for-scoped", Optional.of(path()), Optional.empty()),
                        new IRLabel("as-scoped", Optional.empty(), Optional.of(path()))).build(),
                "labels", "label", "forPath", "asPath");
    }

    @Test
    void fieldRuleReferences() {
        // a resolved reference and the `empty` keyword form (an absent ruleName IS the keyword)
        assertCarries(new Field().ruleReferences(
                        new IRRuleReference(Optional.of(path()), Optional.of("SomeRule"),
                                Optional.of("test.model"), Optional.of("SomeRule")),
                        new IRRuleReference(Optional.empty(), Optional.empty(),
                                Optional.empty(), Optional.empty())).build(),
                "ruleReferences", "ruleName", "resolvedNamespace", "resolvedName");
    }

    @Test
    void enumNamespace() {
        assertCarries(new EnumDecl().namespace("test.model").build(), "namespace");
    }

    @Test
    void enumParent() {
        assertCarries(new EnumDecl().parent(IRTypeNode.reference("BaseEnum", IRKind.ENUM,
                        Optional.of("test.model"), Optional.of("BaseEnum"))).build(),
                "parent", "parentNamespace", "parentResolvedName");
    }

    @Test
    void enumDefinition() {
        assertCarries(new EnumDecl().definition("what the enum is").build(), "definition");
    }

    @Test
    void enumDocReferences() {
        assertCarries(new EnumDecl().docReferences(IRSamples.fullDocReference()).build(),
                "docReferences", "corpora");
    }

    @Test
    void enumAnnotations() {
        assertCarries(new EnumDecl().annotations(
                new IRAnnotationUse("deprecated", Optional.empty())).build(), "annotations");
    }

    @Test
    void enumValueDefinition() {
        assertCarries(new ValueDecl().definition("what the value is").build(), "definition");
    }

    @Test
    void enumValueDocReferences() {
        assertCarries(new ValueDecl().docReferences(IRSamples.fullDocReference()).build(),
                "docReferences", "corpora");
    }

    @Test
    void enumValueAnnotations() {
        assertCarries(new ValueDecl().annotations(
                new IRAnnotationUse("metadata", Optional.of("scheme"))).build(), "annotations");
    }

    @Test
    void enumValueSynonyms() {
        assertCarries(new ValueDecl().synonyms(new IREnumSynonym(List.of("FpML", "ISO"), "EXTERNAL",
                        Optional.of("the synonym definition"), Optional.of("a(.*)"),
                        Optional.of("b$1"), true)).build(),
                "synonyms", "sources", "value", "definition", "patternMatch", "patternReplace",
                "removeHtml");
    }

    // ── the type gate (v3.3 seat 7, PR #643): the collapsed chain and the alias declaration ─────
    //
    // Same law, same three places: the writer emits `<prefix>EffectiveBase` only INSIDE the resolved
    // branch of a reference, the reader refuses it without a `<prefix>Kind` and refuses it beside a
    // kind that is not TYPE_ALIAS, and the schema states both (IRJsonSchemaValidationTest holds that
    // half). `baseTypeArguments` / `typeParameters` are the alias declaration's alone — the
    // IRTypeNode record's own invariant, restated by the reader with the document path.

    /**
     * A whole {@code typeAlias} DECLARATION — a parameterised alias whose body names ANOTHER alias,
     * so the body reference carries a COLLAPSED chain — round-trips to the same record AND to the
     * same bytes.
     *
     * <p>Able to fail: drop any member from {@code typeAliasJson} or from {@code typeAliasNode} in
     * the reader — either the record comparison or the byte comparison fails.
     */
    @Test
    void aTypeAliasDeclarationRoundTrips() {
        IRNode alias = IRSamples.enriched(IRKind.TYPE_ALIAS);
        assertCarries(alias, "baseType", "baseTypeKind", "baseTypeNamespace", "baseTypeResolvedName",
                "baseTypeEffectiveBase", "baseTypeArguments", "typeParameters", "namespace",
                "definition", "docReferences", "annotations", "conditionNames");
        String json = SER.toJson(alias);
        assertEquals(json, SER.toJson(DE.fromJsonNode(json)), "the alias document is not byte-stable");
    }

    /**
     * A field's type reference that resolves to an alias: the leaf and its arguments ride after
     * {@code typeResolvedName}. Both shapes of a leaf are exercised — a DECLARED one (a namespace, no
     * argument) and a BUILTIN one (no namespace, a NEGATED literal argument).
     */
    @Test
    void aFieldTypeReferenceCarriesItsCollapsedChain() {
        IRField declaredLeaf = new Field()
                .type(IRTypeNode.reference("Direction", IRKind.TYPE_ALIAS, Optional.of("test.model"),
                        Optional.of("DirectionAlias"),
                        Optional.of(new IREffectiveBase(IRKind.ENUM, "DirectionEnum",
                                Optional.of("test.model"), List.of()))))
                .build();
        assertCarries(declaredLeaf, "typeKind", "typeNamespace", "typeResolvedName",
                "typeEffectiveBase");
        assertTrue(SER.toJson(declaredLeaf).contains("\"namespace\": \"test.model\""),
                SER.toJson(declaredLeaf));

        IRField builtinLeaf = new Field()
                .type(IRTypeNode.reference("NegMin", IRKind.TYPE_ALIAS, Optional.of("test.model"),
                        Optional.of("NegMin"),
                        Optional.of(new IREffectiveBase(IRKind.BASIC_TYPE, "number", Optional.empty(),
                                List.of(new IRTypeArgument("min", Optional.empty(),
                                        Optional.of("1.5"), true))))))
                .build();
        assertCarries(builtinLeaf, "typeEffectiveBase", "arguments", "parameter", "literalValue",
                "negated");
        String json = SER.toJson(builtinLeaf);
        assertEquals(json, SER.toJson(DE.fromJsonNode(json)), "the collapsed chain is not byte-stable");
    }

    /**
     * REFUSAL (a): an effective base with NO {@code <prefix>Kind}. The kind is what says the reference
     * is an alias at all, so the base cannot stand alone.
     *
     * <p>Able to fail: delete the {@code effectiveBaseJson.isPresent() && kindToken.isEmpty()} arm of
     * {@code IRJsonDeserializer.typeRef}.
     */
    @Test
    void anEffectiveBaseWithoutAReferenceKindIsRefused() {
        String json = fieldDoc("\"typeEffectiveBase\": " + leaf("BASIC_TYPE", "number"));
        IRJsonException refused = assertThrows(IRJsonException.class, () -> DE.fromJsonNode(json));
        assertTrue(refused.getMessage().contains("member 'typeEffectiveBase' without a 'typeKind'"),
                refused.getMessage());
    }

    /**
     * REFUSAL (b): an effective base beside a {@code <prefix>Kind} that is NOT {@code TYPE_ALIAS} —
     * only an alias chain collapses, and the record refuses to hold the fact on any other kind, so a
     * document stating it could never be read back.
     *
     * <p>Able to fail: delete the {@code referentKind != IRKind.TYPE_ALIAS} arm of {@code typeRef}.
     */
    @Test
    void anEffectiveBaseBesideANonAliasKindIsRefused() {
        String json = fieldDoc("\"typeKind\": \"STRUCT\"", "\"typeResolvedName\": \"S\"",
                "\"typeEffectiveBase\": " + leaf("BASIC_TYPE", "number"));
        IRJsonException refused = assertThrows(IRJsonException.class, () -> DE.fromJsonNode(json));
        assertTrue(refused.getMessage().contains("member 'typeEffectiveBase' beside a 'typeKind' of STRUCT"),
                refused.getMessage());
    }

    /**
     * REFUSAL (c): the LEAF's own kind. A collapsed chain ends on a basic type, a record type, a data
     * type, an enum or a choice — never on {@code TYPE_ALIAS} (the chain is collapsed) and never on a
     * non-type kind such as {@code FIELD}. The reader refuses every other {@link IRKind} BY NAME, so it
     * admits exactly the five the schema's {@code effectiveBaseNode} enum admits.
     *
     * <p>Able to fail: read the leaf's kind through {@code kind(...)} instead of
     * {@code effectiveBaseKind(...)} — the record's own refusal still fires for TYPE_ALIAS, but the
     * message stops naming the document path.
     */
    @ParameterizedTest(name = "leaf kind {0} refused")
    @ValueSource(strings = {"TYPE_ALIAS", "FIELD", "META_TYPE", "FUNCTION"})
    void anEffectiveBaseKindOutsideTheFiveLeafKindsIsRefused(String kindToken) {
        String json = fieldDoc("\"typeKind\": \"TYPE_ALIAS\"", "\"typeResolvedName\": \"A\"",
                "\"typeEffectiveBase\": " + leaf(kindToken, "x"));
        IRJsonException refused = assertThrows(IRJsonException.class, () -> DE.fromJsonNode(json));
        assertTrue(refused.getMessage().contains("cannot be an effective base's kind"),
                refused.getMessage());
        // the message names the path and then, as referenceKind's does, the kinds the schema admits
        assertTrue(refused.getMessage().contains(" at $.typeEffectiveBase (the schema admits "), refused.getMessage());
    }

    /**
     * REFUSAL (d): an effective ARGUMENT that passes a parameter through by name. A collapsed chain
     * carries literals only — an unbound parameter is simply ABSENT — which is the
     * {@link IREffectiveBase} record's law, re-raised here naming the document path rather than
     * escaping as an {@code IllegalArgumentException}.
     *
     * <p>Able to fail: remove the {@code try}/{@code catch} from
     * {@code IRJsonDeserializer.effectiveBase}.
     */
    @Test
    void anEffectiveArgumentPassingAParameterThroughByNameIsRefused() {
        String json = fieldDoc("\"typeKind\": \"TYPE_ALIAS\"", "\"typeResolvedName\": \"A\"",
                "\"typeEffectiveBase\": { \"kind\": \"BASIC_TYPE\", \"name\": \"number\","
                        + " \"arguments\": [ { \"parameter\": \"digits\", \"nameValue\": \"n\" } ] }");
        IRJsonException refused = assertThrows(IRJsonException.class, () -> DE.fromJsonNode(json));
        assertTrue(refused.getMessage().contains("passes a parameter through by name"),
                refused.getMessage());
        assertTrue(refused.getMessage().endsWith("at $.typeEffectiveBase"), refused.getMessage());
    }

    /**
     * REFUSAL (e): {@code typeParameters} / {@code baseTypeArguments} on a node that is not a
     * {@code TYPE_ALIAS} declaration — the {@code IRTypeNode} record's own invariant, refused by the
     * reader with the document path instead of reaching the record.
     *
     * <p>Able to fail: delete either {@code refuseTypeAliasOnly} call from
     * {@code IRJsonDeserializer.typeNode} — the member is then merely "unknown", which names no law.
     */
    @Test
    void theAliasOnlyMembersAreRefusedOnADataTypeAndOnAChoice() {
        String struct = objectDoc(List.of("\"kind\": \"STRUCT\"", "\"name\": \"test.model.T\"",
                "\"isAbstract\": false", "\"baseType\": null", "\"fields\": []",
                "\"typeParameters\": [ { \"name\": \"digits\", \"type\": \"int\" } ]"));
        assertTrue(assertThrows(IRJsonException.class, () -> DE.fromJsonNode(struct)).getMessage()
                .contains("member 'typeParameters' on a node of kind STRUCT"), struct);

        String choice = objectDoc(List.of("\"kind\": \"CHOICE\"", "\"name\": \"test.model.C\"",
                "\"isAbstract\": false", "\"baseType\": null", "\"fields\": []",
                "\"baseTypeArguments\": [ { \"parameter\": \"digits\", \"literalValue\": \"3\" } ]"));
        assertTrue(assertThrows(IRJsonException.class, () -> DE.fromJsonNode(choice)).getMessage()
                .contains("member 'baseTypeArguments' on a node of kind CHOICE"), choice);
    }

    /** A {@code baseTypeEffectiveBase} needs a {@code baseType} NAME, exactly like the other three facts. */
    @Test
    void aBaseTypeEffectiveBaseWithoutABaseTypeIsRefused() {
        String json = aliasDoc("null", "\"baseTypeEffectiveBase\": " + leaf("BASIC_TYPE", "number"));
        assertTrue(assertThrows(IRJsonException.class, () -> DE.fromJsonNode(json)).getMessage()
                .contains("member 'baseTypeEffectiveBase' without a 'baseType'"), json);
    }

    /** A declared parameter needs both its name and its declared type; either missing is refused. */
    @ParameterizedTest(name = "a typeParameters entry without {0}")
    @ValueSource(strings = {"name", "type"})
    void aTypeParameterWithoutItsNameOrItsTypeIsRefused(String missing) {
        String entry = missing.equals("name") ? "{ \"type\": \"int\" }" : "{ \"name\": \"digits\" }";
        String json = aliasDoc("\"int\"", "\"typeParameters\": [ " + entry + " ]");
        IRJsonException refused = assertThrows(IRJsonException.class, () -> DE.fromJsonNode(json));
        assertTrue(refused.getMessage().contains("missing member '" + missing + "'"),
                refused.getMessage());
        assertTrue(refused.getMessage().endsWith("at $.typeParameters[0]"), refused.getMessage());
    }

    /** A declared parameter's own type reference obeys the SAME prefix law a field's does. */
    @Test
    void aTypeParameterReferenceObeysTheReferenceLaw() {
        IRTypeNode alias = new IRTypeNode("test.model.A", IRKind.TYPE_ALIAS, List.of(),
                Optional.of(oldArityRef("number")), false, Optional.empty(), IRMetadata.EMPTY,
                Optional.empty(), Optional.empty(), Optional.empty(), List.of(), List.of(), List.of(),
                Optional.empty(),
                List.of(new IRTypeParameter("digits",
                        IRTypeNode.reference("int", IRKind.BASIC_TYPE, Optional.empty(),
                                Optional.of("int")),
                        Optional.of("the digit count"), List.of())),
                List.of());
        assertCarries(alias, "typeParameters", "typeKind", "typeResolvedName", "definition");
        assertFalse(SER.toJson(alias).contains("\"typeArguments\""),
                "a parameter with no arguments of its own writes no typeArguments member - a v1 document is unchanged");

        String orphan = aliasDoc("\"number\"",
                "\"typeParameters\": [ { \"name\": \"digits\", \"type\": \"int\","
                        + " \"typeNamespace\": \"test.model\" } ]");
        assertTrue(assertThrows(IRJsonException.class, () -> DE.fromJsonNode(orphan)).getMessage()
                .contains("member 'typeNamespace' without a 'typeKind'"), orphan);
    }

    /**
     * A declared parameter's OWN type call's arguments (PR #644, the banked cq SF-1 of #643): {@code n number(fractionalDigits: 0)}
     * round-trips its {@code typeArguments} as written, and a malformed one (both a name and a literal value) is refused
     * at the parameter's own path.
     */
    @Test
    void aTypeParametersOwnArgumentsRoundTripAndAMalformedOneIsRefusedAtItsPath() {
        IRTypeNode alias = new IRTypeNode("test.model.A", IRKind.TYPE_ALIAS, List.of(),
                Optional.of(oldArityRef("number")), false, Optional.empty(), IRMetadata.EMPTY,
                Optional.empty(), Optional.empty(), Optional.empty(), List.of(), List.of(), List.of(),
                Optional.empty(),
                List.of(new IRTypeParameter("n",
                        IRTypeNode.reference("number", IRKind.BASIC_TYPE, Optional.empty(), Optional.of("number")),
                        Optional.empty(),
                        List.of(new IRTypeArgument("fractionalDigits", Optional.empty(), Optional.of("0"), false),
                                new IRTypeArgument("min", Optional.empty(), Optional.of("1"), true)))),
                List.of());
        assertCarries(alias, "typeParameters", "typeArguments", "literalValue", "negated");

        String malformed = aliasDoc("\"number\"",
                "\"typeParameters\": [ { \"name\": \"n\", \"type\": \"number\","
                        + " \"typeArguments\": [ { \"parameter\": \"fractionalDigits\", \"nameValue\": \"d\", \"literalValue\": \"0\" } ] } ]");
        String message = assertThrows(IRJsonException.class, () -> DE.fromJsonNode(malformed)).getMessage();
        assertTrue(message.contains("BOTH a name value and a literal value") && message.contains("typeParameters[0].typeArguments[0]"),
                message);
    }

    // ── the property gate (v3.3 seat 8, PR #644): the condition kinds and the alias chain ────
    //
    // A type DECLARATION states, index-parallel to its `conditionNames`, each condition's ROOT kind
    // (`conditionKinds`) - the token the old generator names an unnamed condition's class by. A type
    // REFERENCE that resolved to an alias states the RUNGS of the chain whose leaf
    // `<prefix>EffectiveBase` already carries (`<prefix>AliasChain`), outermost-first. Both are
    // OPTIONAL and written only when non-empty, so a v1 document is byte-unchanged; both obey the
    // laws their records state, re-raised by the reader AT THE OFFENDING MEMBER'S PATH.

    /**
     * The condition kinds round-trip beside the names, and a type that states none writes no member.
     *
     * <p>Able to fail: write {@code conditionKinds} unconditionally in {@code typeJson} - the
     * absence assertion fires; drop it from the reader - the record comparison fails.
     */
    @Test
    void typeConditionKinds() {
        IRTypeNode type = new Struct()
                .conditionNames(Optional.of("Named"), Optional.empty())
                .conditionKinds("DataRule", "OneOf")
                .build();
        String json = SER.toJson(type);
        assertTrue(json.contains("\"conditionKinds\""), "conditionKinds missing:\n" + json);
        assertEquals(type, DE.fromJsonNode(json));

        String unstated = SER.toJson(new Struct().conditionNames(Optional.of("Named")).build());
        assertFalse(unstated.contains("conditionKinds"),
                "a type that states no kinds writes no member - a v1 document is unchanged:\n" + unstated);
    }

    /** A kinds list that is not index-parallel to the names is refused AT THE MEMBER'S OWN PATH. */
    @Test
    void conditionKindsThatAreNotIndexParallelAreRefusedAtTheirPath() {
        String json = structDoc("null", "\"conditionNames\": [ \"A\", null ]",
                "\"conditionKinds\": [ \"OneOf\" ]");
        String message = assertThrows(IRJsonException.class, () -> DE.fromJsonNode(json)).getMessage();
        assertTrue(message.contains("index-parallel") && message.endsWith("at $.conditionKinds"),
                message);
    }

    /** A token outside the three-word vocabulary is refused at the same path. */
    @Test
    void aConditionKindOutsideTheVocabularyIsRefused() {
        String json = structDoc("null", "\"conditionNames\": [ \"A\" ]",
                "\"conditionKinds\": [ \"Bogus\" ]");
        String message = assertThrows(IRJsonException.class, () -> DE.fromJsonNode(json)).getMessage();
        assertTrue(message.contains("Bogus") && message.endsWith("at $.conditionKinds"), message);
    }

    /**
     * A TYPE_ALIAS reference carries the RUNGS its use site walks: every component of a two-link
     * chain (a namespace, declared parameters, a named AND an unnamed condition, their kinds) round-
     * trips to the same record AND the same bytes, and the plain second link writes name alone.
     *
     * <p>Able to fail: drop a member from {@code aliasLinkJson} or from {@code aliasLink} in the
     * reader - either the record comparison or the byte comparison fails.
     */
    @Test
    void aTypeAliasReferenceCarriesItsAliasChain() {
        IRField field = new Field()
                .type(IRTypeNode.reference("Outer", IRKind.TYPE_ALIAS, Optional.of("test.model"),
                        Optional.of("Outer"),
                        Optional.of(new IREffectiveBase(IRKind.BASIC_TYPE, "number", Optional.empty(),
                                List.of())),
                        List.of(new IRAliasLink("Outer", Optional.of("test.model"), List.of("digits"),
                                        List.of(Optional.of("Positive"), Optional.empty()),
                                        List.of("DataRule", "OneOf")),
                                new IRAliasLink("Inner", Optional.empty(), List.of(), List.of(),
                                        List.of()))))
                .build();
        assertCarries(field, "typeEffectiveBase", "typeAliasChain", "parameterNames",
                "conditionNames", "conditionKinds");
        String json = SER.toJson(field);
        // a bare FIELD fragment writes no other JSON null, so this IS the link's unnamed condition
        assertTrue(json.contains("null"),
                "the unnamed condition of a link is not the array's own JSON null:\n" + json);
        assertEquals(json, SER.toJson(DE.fromJsonNode(json)), "the alias chain is not byte-stable");
    }

    /** A link whose kinds are not index-parallel to its names is refused at THAT LINK'S member path. */
    @Test
    void aMalformedAliasChainLinkIsRefusedAtItsPath() {
        String json = fieldDoc("\"typeKind\": \"TYPE_ALIAS\"", "\"typeResolvedName\": \"A\"",
                "\"typeAliasChain\": [ { \"name\": \"Outer\" },"
                        + " { \"name\": \"Inner\", \"conditionNames\": [ \"A\", \"B\" ],"
                        + " \"conditionKinds\": [ \"OneOf\" ] } ]");
        String message = assertThrows(IRJsonException.class, () -> DE.fromJsonNode(json)).getMessage();
        assertTrue(message.contains("index-parallel")
                && message.endsWith("at $.typeAliasChain[1].conditionKinds"), message);
    }

    /** An unknown member inside a link is refused too - {@code additionalProperties: false} discipline. */
    @Test
    void anAliasChainLinkWithAnUnknownMemberIsRefusedAtItsPath() {
        String json = fieldDoc("\"typeKind\": \"TYPE_ALIAS\"", "\"typeResolvedName\": \"A\"",
                "\"typeAliasChain\": [ { \"name\": \"Outer\", \"bogus\": \"x\" } ]");
        String message = assertThrows(IRJsonException.class, () -> DE.fromJsonNode(json)).getMessage();
        assertTrue(message.contains("unknown member 'bogus'")
                && message.endsWith("at $.typeAliasChain[0]"), message);
    }

    /** Only a TYPE_ALIAS reference has rungs: a chain beside any other referent kind is refused. */
    @Test
    void anAliasChainBesideANonAliasKindIsRefused() {
        String json = fieldDoc("\"typeKind\": \"STRUCT\"", "\"typeResolvedName\": \"S\"",
                "\"typeAliasChain\": [ { \"name\": \"Outer\" } ]");
        assertTrue(assertThrows(IRJsonException.class, () -> DE.fromJsonNode(json)).getMessage()
                .contains("member 'typeAliasChain' beside a 'typeKind' of STRUCT"), json);
    }

    /** ... and one with no reference kind at all: the chain is a fact ABOUT a resolved alias. */
    @Test
    void anAliasChainWithoutAReferenceKindIsRefused() {
        String json = fieldDoc("\"typeAliasChain\": [ { \"name\": \"Outer\" } ]");
        assertTrue(assertThrows(IRJsonException.class, () -> DE.fromJsonNode(json)).getMessage()
                .contains("member 'typeAliasChain' without a 'typeKind'"), json);
    }

    /** A {@code baseTypeAliasChain} needs a {@code baseType} NAME, exactly like the other four facts. */
    @Test
    void aBaseTypeAliasChainWithoutABaseTypeIsRefused() {
        String json = aliasDoc("null", "\"baseTypeAliasChain\": [ { \"name\": \"Outer\" } ]");
        assertTrue(assertThrows(IRJsonException.class, () -> DE.fromJsonNode(json)).getMessage()
                .contains("member 'baseTypeAliasChain' without a 'baseType'"), json);
    }

    /**
     * THE NON-DEFAULT RULE for the property gate's two reference-side members: a node with no stated
     * condition kinds and no alias anywhere writes NEITHER member, so every v1 document's bytes stand.
     *
     * <p>Able to fail: write either member unconditionally - the matching assertion fires.
     */
    @Test
    void aNodeWithNeitherKindsNorAChainWritesNeitherMember() {
        String json = SER.toJson(new Struct()
                .conditionNames(Optional.of("Named"))
                .fields(new Field().type(IRTypeNode.reference("Base", IRKind.STRUCT,
                        Optional.of("test.model"), Optional.of("Base"))).build())
                .build());
        assertFalse(json.contains("conditionKinds"), json);
        assertFalse(json.contains("AliasChain"), json);
    }

    /** A compact effective-base object for the refusal documents (whitespace is irrelevant to the reader). */
    private static String leaf(String kind, String name) {
        return "{ \"kind\": \"" + kind + "\", \"name\": \"" + name + "\" }";
    }

    /** A bare TYPE_ALIAS fragment; {@code baseType} is the raw JSON value. */
    private static String aliasDoc(String baseType, String... members) {
        return objectDoc(concat(List.of("\"kind\": \"TYPE_ALIAS\"", "\"name\": \"test.model.A\"",
                "\"baseType\": " + baseType), members));
    }

    /** The fully-enriched samples — every fact of a kind carried at once, not one at a time. */
    @ParameterizedTest(name = "enriched {0}")
    @EnumSource(value = IRKind.class,
            names = {"STRUCT", "CHOICE", "TYPE_ALIAS", "ENUM", "ENUM_VALUE", "FIELD"})
    void everyEnrichedSampleRoundTrips(IRKind kind) {
        IRNode node = IRSamples.enriched(kind);
        assertEquals(node, DE.fromJsonNode(SER.toJson(node)), "enriched round-trip drifted for " + kind);
    }

    // ── the backward-compatibility law ─────────────────────────────────────────────────────────

    /**
     * A node built through a pre-enrichment constructor must serialize to the v1 bytes EXACTLY (the
     * literal below is the string {@code IRJsonSerializerDeclarationTest} pinned before the
     * enrichment), and a v1 document carrying none of the new members must read every new fact back
     * at its empty default.
     */
    @Test
    void anOldArityNodeIsByteIdenticalAndAV1DocumentReadsTheEmptyDefaults() {
        IRFieldNode bar = new IRFieldNode("bar", oldArityRef("string"), Cardinality.ONE_TO_ONE,
                Optional.empty(), IRMetadata.EMPTY);
        IRFieldNode baz = new IRFieldNode("baz", oldArityRef("number"), Cardinality.ZERO_TO_MANY,
                Optional.empty(), IRMetadata.EMPTY);
        IRTypeNode foo = new IRTypeNode("test.model.Foo", IRKind.STRUCT, List.of(bar, baz),
                Optional.empty(), false, Optional.empty(), IRMetadata.EMPTY);

        String v1 = """
                {
                  "irFormatVersion": 1,
                  "nodes": [
                    {
                      "kind": "STRUCT",
                      "name": "test.model.Foo",
                      "isAbstract": false,
                      "baseType": null,
                      "fields": [
                        {
                          "kind": "FIELD",
                          "name": "bar",
                          "type": "string",
                          "cardinality": "ONE_TO_ONE"
                        },
                        {
                          "kind": "FIELD",
                          "name": "baz",
                          "type": "number",
                          "cardinality": "ZERO_TO_MANY"
                        }
                      ]
                    }
                  ]
                }
                """;
        assertEquals(v1, SER.toJson(List.of(foo)),
                "an old-arity node must serialize to the v1 bytes unchanged");

        IRType read = (IRType) DE.fromJson(v1).get(0);
        assertEquals(Optional.empty(), read.namespace());
        assertEquals(Optional.empty(), read.resolvedName());
        assertEquals(Optional.empty(), read.resolvedQualifiedName());
        assertEquals(Optional.empty(), read.definition());
        assertEquals(List.of(), read.docReferences());
        assertEquals(List.of(), read.annotations());
        assertEquals(List.of(), read.conditionNames());
        IRField field = read.fields().get(0);
        assertEquals(Optional.empty(), field.bounds());
        assertFalse(field.isOverride());
        assertEquals(List.of(), field.typeArguments());
        assertEquals(Optional.empty(), field.definition());
        assertEquals(List.of(), field.docReferences());
        assertEquals(List.of(), field.annotations());
        assertEquals(List.of(), field.labels());
        assertEquals(List.of(), field.ruleReferences());
        assertEquals(Optional.empty(), field.type().namespace());
        assertEquals(Optional.empty(), field.type().resolvedName());
        // and the whole document is byte-stable through the round-trip
        assertEquals(v1, SER.toJson(DE.fromJson(v1)));
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

    /**
     * A bare FIELD fragment in the writer's OWN byte form (its member order, its 2-space indent, its
     * trailing newline) carrying the given members verbatim after {@code cardinality} — so a document
     * the writer would itself emit compares byte for byte against {@code SER.toJson(...)}.
     */
    private static String fieldDoc(String... members) {
        return objectDoc(concat(List.of("\"kind\": \"FIELD\"", "\"name\": \"f\"", "\"type\": \"T\"",
                "\"cardinality\": \"ONE_TO_ONE\""), members));
    }

    /** The same for a STRUCT fragment with no fields; {@code baseType} is the raw JSON value. */
    private static String structDoc(String baseType, String... members) {
        return objectDoc(concat(List.of("\"kind\": \"STRUCT\"", "\"name\": \"test.model.T\"",
                "\"isAbstract\": false", "\"baseType\": " + baseType, "\"fields\": []"), members));
    }

    private static List<String> concat(List<String> head, String... tail) {
        List<String> all = new ArrayList<>(head);
        all.addAll(List.of(tail));
        return all;
    }

    /** The members in the writer's object form: one per line, 2-space indent, a single trailing newline. */
    private static String objectDoc(List<String> members) {
        return "{\n  " + String.join(",\n  ", members) + "\n}\n";
    }

    /** {@code item -> leg ->> payout} — the {@code item} keyword root with a deep step. */
    private static IRAnnotationPath path() {
        return new IRAnnotationPath(true, "", List.of(
                new IRAnnotationPath.Step("leg", false),
                new IRAnnotationPath.Step("payout", true)));
    }

    /** The pre-enrichment name-only type reference (what a v1 document reconstructs). */
    private static IRType oldArityRef(String name) {
        return new IRTypeNode(name, IRKind.STRUCT, List.of(), Optional.empty(), false,
                Optional.empty(), IRMetadata.EMPTY);
    }

    // ── builders: every D55 fact at its empty default, each test sets exactly one ──────────────

    private static final class Struct {
        private List<IRField> fields = List.of();
        private Optional<IRType> baseType = Optional.empty();
        private Optional<String> namespace = Optional.empty();
        private Optional<String> resolvedName = Optional.empty();
        private Optional<String> definition = Optional.empty();
        private List<IRDocReference> docReferences = List.of();
        private List<IRAnnotationUse> annotations = List.of();
        private List<Optional<String>> conditionNames = List.of();
        private List<String> conditionKinds = List.of();

        Struct fields(IRField... values) { fields = List.of(values); return this; }
        Struct baseType(IRType value) { baseType = Optional.of(value); return this; }
        Struct namespace(String value) { namespace = Optional.of(value); return this; }
        Struct resolvedName(String value) { resolvedName = Optional.of(value); return this; }
        Struct definition(String value) { definition = Optional.of(value); return this; }
        Struct docReferences(IRDocReference... values) { docReferences = List.of(values); return this; }
        Struct annotations(IRAnnotationUse... values) { annotations = List.of(values); return this; }

        @SafeVarargs
        @SuppressWarnings("varargs")
        final Struct conditionNames(Optional<String>... values) {
            conditionNames = List.of(values);
            return this;
        }

        Struct conditionKinds(String... values) { conditionKinds = List.of(values); return this; }

        IRTypeNode build() {
            return new IRTypeNode("test.model.T", IRKind.STRUCT, fields, baseType, false, Optional.empty(),
                    IRMetadata.EMPTY, namespace, resolvedName, definition, docReferences,
                    annotations, conditionNames, Optional.empty(), List.of(), List.of(),
                    conditionKinds, List.of());
        }
    }

    private static final class Field {
        private IRType type = oldArityRef("string");
        private Optional<IRBounds> bounds = Optional.empty();
        private boolean isOverride;
        private List<IRTypeArgument> typeArguments = List.of();
        private Optional<String> definition = Optional.empty();
        private List<IRDocReference> docReferences = List.of();
        private List<IRAnnotationUse> annotations = List.of();
        private List<IRLabel> labels = List.of();
        private List<IRRuleReference> ruleReferences = List.of();

        Field type(IRType value) { type = value; return this; }
        Field bounds(IRBounds value) { bounds = Optional.of(value); return this; }
        Field override() { isOverride = true; return this; }
        Field typeArguments(IRTypeArgument... values) { typeArguments = List.of(values); return this; }
        Field definition(String value) { definition = Optional.of(value); return this; }
        Field docReferences(IRDocReference... values) { docReferences = List.of(values); return this; }
        Field annotations(IRAnnotationUse... values) { annotations = List.of(values); return this; }
        Field labels(IRLabel... values) { labels = List.of(values); return this; }
        Field ruleReferences(IRRuleReference... values) { ruleReferences = List.of(values); return this; }

        IRFieldNode build() {
            return new IRFieldNode("f", type, Cardinality.ONE_TO_ONE, Optional.empty(),
                    IRMetadata.EMPTY, bounds, isOverride, typeArguments, definition, docReferences,
                    annotations, labels, ruleReferences);
        }
    }

    private static final class EnumDecl {
        private Optional<String> namespace = Optional.empty();
        private Optional<IRType> parent = Optional.empty();
        private Optional<String> definition = Optional.empty();
        private List<IRDocReference> docReferences = List.of();
        private List<IRAnnotationUse> annotations = List.of();

        EnumDecl namespace(String value) { namespace = Optional.of(value); return this; }
        EnumDecl parent(IRType value) { parent = Optional.of(value); return this; }
        EnumDecl definition(String value) { definition = Optional.of(value); return this; }
        EnumDecl docReferences(IRDocReference... values) { docReferences = List.of(values); return this; }
        EnumDecl annotations(IRAnnotationUse... values) { annotations = List.of(values); return this; }

        IREnumNode build() {
            return new IREnumNode("test.model.E", List.<IREnumValue>of(), Optional.empty(),
                    IRMetadata.EMPTY, namespace, parent, definition, docReferences, annotations);
        }
    }

    private static final class ValueDecl {
        private Optional<String> definition = Optional.empty();
        private List<IRDocReference> docReferences = List.of();
        private List<IRAnnotationUse> annotations = List.of();
        private List<IREnumSynonym> synonyms = List.of();

        ValueDecl definition(String value) { definition = Optional.of(value); return this; }
        ValueDecl docReferences(IRDocReference... values) { docReferences = List.of(values); return this; }
        ValueDecl annotations(IRAnnotationUse... values) { annotations = List.of(values); return this; }
        ValueDecl synonyms(IREnumSynonym... values) { synonyms = List.of(values); return this; }

        IREnumValueNode build() {
            return new IREnumValueNode("V", Optional.empty(), Optional.empty(), IRMetadata.EMPTY,
                    definition, docReferences, annotations, synonyms);
        }
    }
}
