package com.regnosys.rosetta.ir.json;

import com.regnosys.rosetta.ir.core.Cardinality;
import com.regnosys.rosetta.ir.core.IRAliasLink;
import com.regnosys.rosetta.ir.core.IREffectiveBase;
import com.regnosys.rosetta.ir.core.IRKind;
import com.regnosys.rosetta.ir.core.IRQualifiableConfig;
import com.regnosys.rosetta.ir.core.IRWithMetaUse;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates the Draft 2020-12 JSON Schema for the v1 IR JSON <b>declaration</b> wire form — the
 * machine-readable contract an external (non-JVM) consumer validates IR JSON against.
 *
 * <p>The schema is built as a {@link Json} tree and rendered by {@link JsonWriter}, so it reuses the
 * step-2 serializer infrastructure and adds <b>zero</b> third-party production dependency. It covers
 * the document envelope plus the six declaration kinds (STRUCT/CHOICE/TYPE_ALIAS/ENUM top-level,
 * FIELD/ENUM_VALUE nested) — exactly what {@code IRJsonSerializer.toJson(List)} emits for a real
 * model. Expressions and the generic fallback are out of scope (not part of the frozen document form).
 *
 * <p>Every object schema declares {@code "type":"object"} and every array {@code "type":"array"}:
 * under Draft 2020-12 the object/array applicators are vacuously satisfied by a wrong-typed instance,
 * so without the explicit {@code type} the schema would wrongly accept a non-object root or a
 * primitive-filled array. The {@code $defs} entries carry a {@code Node} suffix so the definition names
 * do not coincide with the JSON Schema keyword names {@code type}/{@code enum}. (A {@code $def} literally
 * named {@code type} or {@code enum} would in fact be valid — {@code $defs} members are namespaced and
 * referenced as {@code #/$defs/<name>}, so they cannot shadow a keyword — but a name mirroring a keyword is
 * confusing to read and fragile under naive tooling; the uniform {@code Node} suffix avoids it.)
 *
 * <p>The member/required sets are hand-coded here to mirror {@link IRJsonSerializer}'s emission; the
 * {@code IRJsonSchemaFaithfulnessTest} drift sentinel pins them to the serializer so they cannot
 * silently diverge.
 *
 * <p>The declaration facts of decision D55 (a namespace, a reference's resolved facts, the
 * documentation, the annotations, the doc references, the condition names, the exact bounds, the
 * type arguments, the labels, the rule references, an enum value's synonyms) are declared as
 * <b>optional</b> properties — the serializer writes each one ONLY when it is non-default, so a v1
 * document written before the enrichment still validates unchanged. A flag the serializer emits
 * only when true ({@code isOverride}, {@code regulatory}, {@code reportedField}, {@code negated},
 * {@code rootItem}, {@code deep}, {@code attributeRef}, {@code removeHtml}) is pinned as
 * {@code {"const": true}}, which is the emission rule stated in the contract rather than merely a
 * boolean type.
 *
 * <p>A member whose legality turns on ANOTHER member is stated as such, so the schema refuses exactly
 * what {@link IRJsonDeserializer} refuses: a type reference's {@code <prefix>Kind} and its
 * {@code <prefix>Namespace} / {@code <prefix>ResolvedName} stand or fall together and a
 * {@code baseType*} fact additionally needs a non-null {@code baseType}
 * ({@code dependentRequired} + {@code dependentSchemas}, see {@code referenceFacts}); an enum's
 * {@code parentNamespace} / {@code parentResolvedName} need a {@code parent}; a type argument's
 * {@code negated} needs a {@code literalValue}. Each of those shapes is one the serializer never
 * emits and the reader could not read back unchanged.
 *
 * <p>The type gate (v3.3 seat 7, PR #643) adds three more of the same shape, all OPTIONAL so a v1
 * document still validates unchanged: a reference's {@code <prefix>EffectiveBase} needs a
 * {@code <prefix>Kind} ({@code dependentRequired}) and that kind must be {@code TYPE_ALIAS}
 * ({@code dependentSchemas}); an effective base's own {@code arguments} are LITERAL only
 * ({@code not required nameValue}, the {@link IREffectiveBase} record's law); and
 * {@code baseTypeArguments} / {@code typeParameters} live on {@code typeAliasNode} alone, which
 * {@code additionalProperties: false} states on every other {@code $def}. PR #644 adds a parameter's own
 * {@code typeArguments} (the arguments its type call wrote), OPTIONAL on {@code typeParameterNode}.
 *
 * <p>The property gate (v3.3 seat 8, PR #644) adds one NEW node shape and two OPTIONAL members, so a
 * v1 document still validates unchanged: {@code modelNode} (the facts of one {@code namespace}
 * declaration, three required members and seven optional ones, with {@code qualifiableConfigNode} /
 * {@code qualificationFunctionNode} / {@code functionSignatureNode} / {@code withMetaUseNode} beneath
 * it); a type declaration's {@code conditionKinds}, its tokens pinned to
 * {@link IRAliasLink#CONDITION_KINDS}; and a reference's {@code <prefix>AliasChain}
 * ({@code aliasLinkNode}), which rides the {@code <prefix>EffectiveBase} dependency law verbatim -
 * it needs a {@code <prefix>Kind} and that kind must read {@code TYPE_ALIAS}. A with-meta use states
 * its record's own two laws too: EXACTLY ONE of {@code argumentType} / {@code refusal}
 * ({@code oneOf}), and a REFUSED use carries no {@code typeArguments}.
 *
 * <p>Example: {@code IRJsonSchema.schemaJson()} yields the committed {@code v1.schema.json}.
 */
public final class IRJsonSchema {

    private static final String DIALECT = "https://json-schema.org/draft/2020-12/schema";
    private static final String SCHEMA_ID = "https://finos.org/rune-dsl/schemas/ir-declaration-v1.json";

    private IRJsonSchema() {
    }

    /** The v1 declaration schema as a {@link Json} tree. */
    public static Json schema() {
        return obj(
                m("$schema", str(DIALECT)),
                m("$id", str(SCHEMA_ID)),
                m("title", str("Rune DSL IR — v1 declaration wire form")),
                m("type", str("object")),
                m("properties", obj(
                        m("irFormatVersion", obj(m("const",
                                Json.number(Integer.toString(IRJsonSerializer.IR_FORMAT_VERSION))))),
                        m("nodes", arrayOf(ref("declarationNode"))))),
                m("required", arr(str("irFormatVersion"), str("nodes"))),
                m("additionalProperties", Json.bool(false)),
                m("$defs", obj(
                        m("declarationNode", obj(m("oneOf",
                                arr(ref("typeNode"), ref("enumNode"), ref("typeAliasNode"),
                                        ref("modelNode"))))),
                        m("typeNode", referenceFacts(objectSchema(
                                List.of(
                                        m("kind", enumOf("STRUCT", "CHOICE")),
                                        m("name", string()),
                                        m("isAbstract", typed("boolean")),
                                        m("baseType", nullableString()),
                                        m("fields", arrayOf(ref("fieldNode"))),
                                        // the D55 facts — optional, in the serializer's emission order
                                        m("baseTypeKind", enumOf(typeReferenceKindNames())),
                                        m("baseTypeNamespace", string()),
                                        m("baseTypeResolvedName", string()),
                                        m("baseTypeEffectiveBase", ref("effectiveBaseNode")),
                                        m("baseTypeAliasChain", arrayOf(ref("aliasLinkNode"))),
                                        m("namespace", string()),
                                        m("resolvedName", string()),
                                        m("definition", string()),
                                        m("docReferences", arrayOf(ref("docReferenceNode"))),
                                        m("annotations", arrayOf(ref("annotationUseNode"))),
                                        m("conditionNames", arrayOf(nullableString())),
                                        // the property gate (PR #644) - index-parallel to the names,
                                        // which only the reader can state
                                        m("conditionKinds", arrayOf(enumOf(conditionKindNames())))),
                                List.of("kind", "name", "isAbstract", "baseType", "fields")),
                                // a baseType* fact also needs a baseType that is a NAME, not JSON null
                                "baseType", m("properties", obj(m("baseType", string()))))),
                        m("fieldNode", referenceFacts(objectSchema(
                                List.of(
                                        m("kind", constStr("FIELD")),
                                        m("name", string()),
                                        m("type", string()),
                                        m("cardinality", enumOf(cardinalityNames())),
                                        // the D55 facts — optional, in the serializer's emission order
                                        m("typeKind", enumOf(typeReferenceKindNames())),
                                        m("typeNamespace", string()),
                                        m("typeResolvedName", string()),
                                        m("typeEffectiveBase", ref("effectiveBaseNode")),
                                        m("typeAliasChain", arrayOf(ref("aliasLinkNode"))),
                                        m("bounds", ref("boundsNode")),
                                        m("isOverride", constTrue()),
                                        m("typeArguments", arrayOf(ref("typeArgumentNode"))),
                                        m("definition", string()),
                                        m("docReferences", arrayOf(ref("docReferenceNode"))),
                                        m("annotations", arrayOf(ref("annotationUseNode"))),
                                        m("labels", arrayOf(ref("labelNode"))),
                                        m("ruleReferences", arrayOf(ref("ruleReferenceNode")))),
                                List.of("kind", "name", "type", "cardinality")), "type")),
                        m("enumNode", dependentRequired(objectSchema(
                                List.of(
                                        m("kind", constStr("ENUM")),
                                        m("name", string()),
                                        m("values", arrayOf(ref("enumValueNode"))),
                                        // the D55 facts — optional, in the serializer's emission order
                                        m("parent", string()),
                                        m("parentNamespace", string()),
                                        m("parentResolvedName", string()),
                                        m("namespace", string()),
                                        m("definition", string()),
                                        m("docReferences", arrayOf(ref("docReferenceNode"))),
                                        m("annotations", arrayOf(ref("annotationUseNode")))),
                                List.of("kind", "name", "values")),
                                // the enum parent carries no kind member, so the facts depend on the parent alone
                                dependsOn("parentNamespace", "parent"),
                                dependsOn("parentResolvedName", "parent"))),
                        m("enumValueNode", objectSchema(
                                List.of(
                                        m("kind", constStr("ENUM_VALUE")),
                                        m("name", string()),
                                        m("displayName", nullableString()),
                                        // the D55 facts — optional, in the serializer's emission order
                                        m("definition", string()),
                                        m("docReferences", arrayOf(ref("docReferenceNode"))),
                                        m("annotations", arrayOf(ref("annotationUseNode"))),
                                        m("synonyms", arrayOf(ref("enumSynonymNode")))),
                                List.of("kind", "name", "displayName"))),
                        // ── the type gate (PR #643): the typeAlias DECLARATION ─────────────────
                        // No `fields` and no `isAbstract` (an alias declares neither) and no
                        // `resolvedName` (a declaration carries none); the body is a reference under
                        // the same `baseType` prefix law the data type uses.
                        m("typeAliasNode", referenceFacts(objectSchema(
                                List.of(
                                        m("kind", constStr("TYPE_ALIAS")),
                                        m("name", string()),
                                        m("baseType", nullableString()),
                                        m("baseTypeKind", enumOf(typeReferenceKindNames())),
                                        m("baseTypeNamespace", string()),
                                        m("baseTypeResolvedName", string()),
                                        m("baseTypeEffectiveBase", ref("effectiveBaseNode")),
                                        m("baseTypeAliasChain", arrayOf(ref("aliasLinkNode"))),
                                        m("baseTypeArguments", arrayOf(ref("typeArgumentNode"))),
                                        m("typeParameters", arrayOf(ref("typeParameterNode"))),
                                        m("namespace", string()),
                                        m("definition", string()),
                                        m("docReferences", arrayOf(ref("docReferenceNode"))),
                                        m("annotations", arrayOf(ref("annotationUseNode"))),
                                        m("conditionNames", arrayOf(nullableString())),
                                        m("conditionKinds", arrayOf(enumOf(conditionKindNames())))),
                                List.of("kind", "name", "baseType")),
                                // a baseType* fact also needs a baseType that is a NAME, not JSON null
                                "baseType", m("properties", obj(m("baseType", string()))))),
                        // ── the property gate (PR #644): the MODEL-level node ────────────────
                        // The facts of ONE `namespace` declaration the emitter's derived files read.
                        // `name` and `namespace` are the same string (the record's law, which only
                        // the reader can state); every other member is OPTIONAL, so a model with
                        // nothing to say is three members long and a v1 document - which carries no
                        // MODEL node at all - validates unchanged.
                        m("modelNode", objectSchema(
                                List.of(
                                        m("kind", constStr("MODEL")),
                                        m("name", string()),
                                        m("namespace", string()),
                                        m("definition", string()),
                                        m("version", string()),
                                        m("qualifiableConfigs", arrayOf(ref("qualifiableConfigNode"))),
                                        m("qualificationFunctions",
                                                arrayOf(ref("qualificationFunctionNode"))),
                                        m("functionSignatures", arrayOf(ref("functionSignatureNode"))),
                                        m("withMetaUses", arrayOf(ref("withMetaUseNode")))),
                                List.of("kind", "name", "namespace"))),
                        m("qualifiableConfigNode", referenceFacts(objectSchema(
                                List.of(
                                        m("kind", enumOf(qualifiableConfigKindNames())),
                                        m("rootType", string()),
                                        m("rootTypeKind", enumOf(typeReferenceKindNames())),
                                        m("rootTypeNamespace", string()),
                                        m("rootTypeResolvedName", string()),
                                        m("rootTypeEffectiveBase", ref("effectiveBaseNode")),
                                        m("rootTypeAliasChain", arrayOf(ref("aliasLinkNode")))),
                                List.of("kind", "rootType")), "rootType")),
                        m("qualificationFunctionNode", referenceFacts(objectSchema(
                                List.of(
                                        m("name", string()),
                                        m("firstInputType", string()),
                                        m("firstInputTypeKind", enumOf(typeReferenceKindNames())),
                                        m("firstInputTypeNamespace", string()),
                                        m("firstInputTypeResolvedName", string()),
                                        m("firstInputTypeEffectiveBase", ref("effectiveBaseNode")),
                                        m("firstInputTypeAliasChain", arrayOf(ref("aliasLinkNode")))),
                                List.of("name", "firstInputType")), "firstInputType")),
                        // a function's inputs and output are FIELD nodes: the wrapper collector reads
                        // their `[metadata ...]` annotations, so they carry an attribute's own shape
                        m("functionSignatureNode", objectSchema(
                                List.of(
                                        m("name", string()),
                                        m("inputs", arrayOf(ref("fieldNode"))),
                                        m("output", ref("fieldNode"))),
                                List.of("name"))),
                        // EXACTLY ONE of argumentType / refusal, a REFUSED use carries no constraints,
                        // and an inferred type's constraints are LITERAL - the IRWithMetaUse record's
                        // own three laws. The argument type rides the standard reference prefix and
                        // additionally needs the `argumentType` NAME it is a fact about.
                        m("withMetaUseNode", exactlyOneOfArgumentTypeOrRefusal(referenceFacts(
                                objectSchema(
                                        List.of(
                                                m("entryNames", arrayOf(string())),
                                                m("argumentType", string()),
                                                m("argumentTypeKind", enumOf(typeReferenceKindNames())),
                                                m("argumentTypeNamespace", string()),
                                                m("argumentTypeResolvedName", string()),
                                                m("argumentTypeEffectiveBase", ref("effectiveBaseNode")),
                                                m("argumentTypeAliasChain",
                                                        arrayOf(ref("aliasLinkNode"))),
                                                m("typeArguments", arrayOf(allOf(
                                                        ref("typeArgumentNode"), without("nameValue")))),
                                                m("refusal", enumOf(withMetaRefusalNames()))),
                                        List.of()),
                                "argumentType", m("required", arr(str("argumentType")))))),
                        // ── the D55 nested shapes ──────────────────────────────────────────────
                        // `lower <= upper` is the record's own invariant (IRBounds) — it has no
                        // Draft 2020-12 spelling; `minimum: 0` is the half the schema CAN state.
                        m("boundsNode", objectSchema(
                                List.of(
                                        m("lower", nonNegativeInteger()),
                                        m("upper", nonNegativeInteger())),
                                List.of("lower"))),
                        m("typeArgumentNode", exactlyOneOf(dependentRequired(objectSchema(
                                List.of(
                                        m("parameter", string()),
                                        m("nameValue", string()),
                                        m("literalValue", string()),
                                        m("negated", constTrue())),
                                List.of("parameter")),
                                // a sign is a fact ABOUT a literal: never beside a passed-through name
                                dependsOn("negated", "literalValue")), "nameValue", "literalValue")),
                        // ── the type gate (PR #643): the two nested shapes ─────────────────────
                        // An effective argument is LITERAL: a collapsed chain never passes a
                        // parameter through by name (an unbound one is simply absent), which the
                        // IREffectiveBase record enforces and `not: {required: [nameValue]}` states.
                        m("effectiveBaseNode", objectSchema(
                                List.of(
                                        m("kind", enumOf(effectiveBaseKindNames())),
                                        m("name", string()),
                                        m("namespace", string()),
                                        m("arguments", arrayOf(
                                                allOf(ref("typeArgumentNode"), without("nameValue"))))),
                                List.of("kind", "name"))),
                        // ── the property gate (PR #644): one RUNG of an alias chain ─────────
                        // The kinds are index-parallel to the names, which Draft 2020-12 has no
                        // spelling for; the enum is the half the schema CAN state (the reader states
                        // the other half, IRAliasLink.checkConditionKinds).
                        m("aliasLinkNode", objectSchema(
                                List.of(
                                        m("name", string()),
                                        m("namespace", string()),
                                        m("parameterNames", arrayOf(string())),
                                        m("conditionNames", arrayOf(nullableString())),
                                        m("conditionKinds", arrayOf(enumOf(conditionKindNames())))),
                                List.of("name"))),
                        m("typeParameterNode", referenceFacts(objectSchema(
                                List.of(
                                        m("name", string()),
                                        m("type", string()),
                                        m("typeKind", enumOf(typeReferenceKindNames())),
                                        m("typeNamespace", string()),
                                        m("typeResolvedName", string()),
                                        m("typeEffectiveBase", ref("effectiveBaseNode")),
                                        m("typeAliasChain", arrayOf(ref("aliasLinkNode"))),
                                        // the parameter's OWN type call's arguments (PR #644): as written, optional
                                        m("typeArguments", arrayOf(ref("typeArgumentNode"))),
                                        m("definition", string())),
                                List.of("name", "type")), "type")),
                        m("annotationPathNode", objectSchema(
                                List.of(
                                        m("rootItem", constTrue()),
                                        m("root", string()),
                                        m("steps", arrayOf(ref("pathStepNode")))),
                                List.of())),
                        m("pathStepNode", objectSchema(
                                List.of(
                                        m("name", string()),
                                        m("deep", constTrue())),
                                List.of("name"))),
                        m("annotationUseNode", objectSchema(
                                List.of(
                                        m("name", string()),
                                        m("qualifier", string()),
                                        m("arguments", arrayOf(ref("annotationArgumentNode")))),
                                List.of("name"))),
                        m("annotationArgumentNode", objectSchema(
                                List.of(
                                        m("key", string()),
                                        m("value", string()),
                                        m("attributeRef", constTrue())),
                                List.of("key", "value"))),
                        m("docReferenceNode", objectSchema(
                                List.of(
                                        m("regulatory", constTrue()),
                                        m("path", ref("annotationPathNode")),
                                        m("body", string()),
                                        m("corpora", arrayOf(ref("corpusNode"))),
                                        m("segments", arrayOf(ref("segmentNode"))),
                                        m("rationales", arrayOf(ref("rationaleNode"))),
                                        m("structuredProvision", string()),
                                        m("provision", string()),
                                        m("reportedField", constTrue()),
                                        m("namedArgs", arrayOf(ref("namedArgNode")))),
                                List.of())),
                        m("corpusNode", objectSchema(
                                List.of(
                                        m("reference", string()),
                                        m("resolved", ref("corpusDeclarationNode"))),
                                List.of("reference"))),
                        m("corpusDeclarationNode", objectSchema(
                                List.of(
                                        m("typeKeyword", string()),
                                        m("name", string()),
                                        m("displayName", string()),
                                        m("definition", string())),
                                List.of("name"))),
                        m("segmentNode", objectSchema(
                                List.of(
                                        m("name", string()),
                                        m("value", string())),
                                List.of("name", "value"))),
                        m("rationaleNode", objectSchema(
                                List.of(
                                        m("text", string()),
                                        m("author", string())),
                                List.of())),
                        m("namedArgNode", objectSchema(
                                List.of(
                                        m("name", string()),
                                        m("value", string())),
                                List.of("name", "value"))),
                        m("labelNode", objectSchema(
                                List.of(
                                        m("label", string()),
                                        m("forPath", ref("annotationPathNode")),
                                        m("asPath", ref("annotationPathNode"))),
                                List.of("label"))),
                        m("ruleReferenceNode", objectSchema(
                                List.of(
                                        m("forPath", ref("annotationPathNode")),
                                        m("ruleName", string()),
                                        m("resolvedNamespace", string()),
                                        m("resolvedName", string())),
                                List.of())),
                        m("enumSynonymNode", objectSchema(
                                List.of(
                                        m("sources", arrayOf(string())),
                                        m("value", string()),
                                        m("definition", string()),
                                        m("patternMatch", string()),
                                        m("patternReplace", string()),
                                        m("removeHtml", constTrue())),
                                List.of("value"))))));
    }

    /** The v1 declaration schema rendered to deterministic JSON text (the committed-golden form). */
    public static String schemaJson() {
        return JsonWriter.write(schema());
    }

    // ── builders (all return a Json schema fragment) ───────────────────────────────────────────

    private static Json.Obj obj(Json.Member... members) {
        return new Json.Obj(List.of(members));
    }

    private static Json.Member m(String name, Json value) {
        return new Json.Member(name, value);
    }

    private static Json str(String s) {
        return Json.str(s);
    }

    private static Json.Arr arr(Json... elements) {
        return new Json.Arr(List.of(elements));
    }

    /** A `{"$ref":"#/$defs/<def>"}` reference. */
    private static Json ref(String def) {
        return obj(m("$ref", str("#/$defs/" + def)));
    }

    /** `{"type":"string"}`. */
    private static Json string() {
        return obj(m("type", str("string")));
    }

    /** `{"type":"<type>"}` for a single primitive type. */
    private static Json typed(String type) {
        return obj(m("type", str(type)));
    }

    /** `{"type":"integer","minimum":0}` — a cardinality bound is never negative ({@link com.regnosys.rosetta.ir.core.IRBounds}). */
    private static Json nonNegativeInteger() {
        return obj(m("type", str("integer")), m("minimum", Json.number("0")));
    }

    /** `{"type":["string","null"]}` — a JSON string or null. */
    private static Json nullableString() {
        return obj(m("type", arr(str("string"), str("null"))));
    }

    /** `{"const":"<value>"}` for a fixed string. */
    private static Json constStr(String value) {
        return obj(m("const", str(value)));
    }

    /**
     * `{"const":true}` — a flag the serializer writes ONLY when it is set, so the contract states
     * the emission rule rather than merely `{"type":"boolean"}`.
     */
    private static Json constTrue() {
        return obj(m("const", Json.bool(true)));
    }

    /** `{"type":"array","items":<items>}`. */
    private static Json arrayOf(Json items) {
        return obj(m("type", str("array")), m("items", items));
    }

    /** `{"allOf":[<a>,<b>]}` — a `$ref`ed shape NARROWED by a second rule. */
    private static Json allOf(Json a, Json b) {
        return obj(m("allOf", arr(a, b)));
    }

    /** `{"not":{"required":["<member>"]}}` — the member must be ABSENT. */
    private static Json without(String member) {
        return obj(m("not", obj(m("required", arr(str(member))))));
    }

    /** `{"enum":[...]}`. */
    private static Json enumOf(String... values) {
        List<Json> elements = new ArrayList<>();
        for (String v : values) {
            elements.add(str(v));
        }
        return obj(m("enum", new Json.Arr(elements)));
    }

    /** The {@link Cardinality} constant names in declaration order (DRY — kept in sync with the enum). */
    private static String[] cardinalityNames() {
        Cardinality[] values = Cardinality.values();
        String[] names = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            names[i] = values[i].name();
        }
        return names;
    }

    /**
     * The kinds a type REFERENCE can carry: the six {@link com.regnosys.rosetta.ir.core.IRType}
     * kinds plus {@link IRKind#ENUM} (a field whose declared type is an enumeration). Written as
     * {@link IRKind} constants, not string literals, so a rename breaks the compile. ONE list for both
     * halves of the contract: {@link IRJsonDeserializer} refuses a {@code typeKind} / {@code baseTypeKind}
     * token outside it, so the reader accepts exactly what the schema admits.
     */
    static final IRKind[] TYPE_REFERENCE_KINDS = {
            IRKind.STRUCT, IRKind.CHOICE, IRKind.TYPE_ALIAS, IRKind.BASIC_TYPE,
            IRKind.RECORD_TYPE, IRKind.META_TYPE, IRKind.ENUM};

    private static String[] typeReferenceKindNames() {
        String[] names = new String[TYPE_REFERENCE_KINDS.length];
        for (int i = 0; i < TYPE_REFERENCE_KINDS.length; i++) {
            names[i] = TYPE_REFERENCE_KINDS[i].name();
        }
        return names;
    }

    /**
     * The kinds a COLLAPSED alias chain can end on, in a deterministic order (the record's own
     * {@link IREffectiveBase#LEAF_KINDS} is an unordered {@code Set}, which a wire contract cannot be
     * built from). Written as {@link IRKind} constants so a rename breaks the compile, and pinned
     * SET-EQUAL to {@code LEAF_KINDS} by {@code IRJsonSchemaFaithfulnessTest}. ONE list for both
     * halves of the contract: {@link IRJsonDeserializer} refuses an effective base's {@code kind}
     * outside it, so the reader accepts exactly what the schema admits.
     */
    static final IRKind[] EFFECTIVE_BASE_KINDS = {
            IRKind.BASIC_TYPE, IRKind.RECORD_TYPE, IRKind.STRUCT, IRKind.ENUM, IRKind.CHOICE};

    private static String[] effectiveBaseKindNames() {
        String[] names = new String[EFFECTIVE_BASE_KINDS.length];
        for (int i = 0; i < EFFECTIVE_BASE_KINDS.length; i++) {
            names[i] = EFFECTIVE_BASE_KINDS[i].name();
        }
        return names;
    }

    /**
     * The condition-kind vocabulary, taken from {@link IRAliasLink#CONDITION_KINDS} itself - ONE list
     * for both halves of the contract, so the schema admits exactly the tokens the record admits and
     * a change to the record's list moves the golden schema.
     */
    private static String[] conditionKindNames() {
        return IRAliasLink.CONDITION_KINDS.toArray(new String[0]);
    }

    /** The two tokens a qualifiable configuration's kind may read, from {@link IRQualifiableConfig#KINDS}. */
    private static String[] qualifiableConfigKindNames() {
        return IRQualifiableConfig.KINDS.toArray(new String[0]);
    }

    /** The two tokens a with-meta use's refusal may read, from {@link IRWithMetaUse#REFUSALS}. */
    private static String[] withMetaRefusalNames() {
        return IRWithMetaUse.REFUSALS.toArray(new String[0]);
    }

    /**
     * The with-meta use's own {@code oneOf}: EXACTLY ONE of {@code argumentType} / {@code refusal},
     * and the {@code refusal} branch additionally forbids {@code typeArguments} - a refused use
     * carries no constraints, which is the {@link IRWithMetaUse} record's second law. Written here
     * rather than through {@code exactlyOneOf} because the two branches are not symmetric.
     */
    private static Json exactlyOneOfArgumentTypeOrRefusal(Json objectSchema) {
        return append(objectSchema, m("oneOf", arr(
                obj(m("required", arr(str("argumentType")))),
                obj(m("required", arr(str("refusal"))),
                        m("not", obj(m("required", arr(str("typeArguments")))))))));
    }

    /** The object schema plus `"oneOf":[{"required":["a"]},{"required":["b"]}]` - EXACTLY one of the two members is present. */
    private static Json exactlyOneOf(Json objectSchema, String a, String b) {
        return append(objectSchema, m("oneOf",
                arr(obj(m("required", arr(str(a)))), obj(m("required", arr(str(b)))))));
    }

    /** The object schema plus `"dependentRequired":{...}` — each member may not appear without the ones it names. */
    private static Json dependentRequired(Json objectSchema, Json.Member... dependencies) {
        return append(objectSchema, m("dependentRequired", new Json.Obj(List.of(dependencies))));
    }

    /** One `dependentRequired` entry: `"<member>": ["<required>", ...]`. */
    private static Json.Member dependsOn(String member, String... required) {
        List<Json> names = new ArrayList<>();
        for (String r : required) {
            names.add(str(r));
        }
        return m(member, new Json.Arr(names));
    }

    /**
     * The dependent-member law of a type REFERENCE under one prefix ({@code type} on a field,
     * {@code baseType} on a data type): {@code <prefix>Kind} is present EXACTLY when the reference
     * RESOLVED. The two resolved strings require the kind ({@code dependentRequired}); the kind
     * requires at least one of them ({@code dependentSchemas} + {@code anyOf}), plus whatever else
     * that prefix demands — the {@code baseType} prefix also demands that {@code baseType} be a name
     * rather than JSON {@code null}.
     *
     * <p>ONE law with the writer ({@code IRJsonSerializer.addReferenceFacts}, which emits the triple
     * only for a resolved reference) and the reader ({@code IRJsonDeserializer.typeRef}, which refuses
     * either half alone): a half-stated reference does not round-trip, so no half of the contract
     * admits it.
     *
     * <p>The type gate (PR #643) rides the same prefix: {@code <prefix>EffectiveBase} requires the
     * kind ({@code dependentRequired}) and pins it to {@code TYPE_ALIAS} ({@code dependentSchemas}) —
     * only a collapsed alias chain has a base. The property gate (PR #644) rides it verbatim once
     * more with {@code <prefix>AliasChain}, the RUNGS of that same chain.
     */
    private static Json referenceFacts(Json objectSchema, String prefix, Json.Member... extraKindRules) {
        List<Json.Member> kindRule = new ArrayList<>(List.of(extraKindRules));
        kindRule.add(m("anyOf", arr(obj(m("required", arr(str(prefix + "Namespace")))),
                obj(m("required", arr(str(prefix + "ResolvedName")))))));
        Json withRequired = dependentRequired(objectSchema,
                dependsOn(prefix + "Namespace", prefix + "Kind"),
                dependsOn(prefix + "ResolvedName", prefix + "Kind"),
                dependsOn(prefix + "EffectiveBase", prefix + "Kind"),
                dependsOn(prefix + "AliasChain", prefix + "Kind"));
        return append(withRequired, m("dependentSchemas",
                obj(m(prefix + "Kind", new Json.Obj(kindRule)),
                        m(prefix + "EffectiveBase", obj(m("properties",
                                obj(m(prefix + "Kind", constStr("TYPE_ALIAS")))))),
                        m(prefix + "AliasChain", obj(m("properties",
                                obj(m(prefix + "Kind", constStr("TYPE_ALIAS")))))))));
    }

    /** Appends one member to an already-built object schema, keeping the existing member order. */
    private static Json append(Json objectSchema, Json.Member member) {
        List<Json.Member> members = new ArrayList<>(((Json.Obj) objectSchema).members());
        members.add(member);
        return new Json.Obj(members);
    }

    /** An object schema: `{"type":"object","properties":{...},"required":[...],"additionalProperties":false}`. */
    private static Json objectSchema(List<Json.Member> properties, List<String> required) {
        List<Json> requiredArr = new ArrayList<>();
        for (String r : required) {
            requiredArr.add(str(r));
        }
        return obj(
                m("type", str("object")),
                m("properties", new Json.Obj(properties)),
                m("required", new Json.Arr(requiredArr)),
                m("additionalProperties", Json.bool(false)));
    }
}
