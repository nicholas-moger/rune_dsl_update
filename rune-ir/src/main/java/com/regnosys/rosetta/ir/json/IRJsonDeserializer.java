package com.regnosys.rosetta.ir.json;

import com.regnosys.rosetta.ir.adapter.IREnumNode;
import com.regnosys.rosetta.ir.adapter.IREnumValueNode;
import com.regnosys.rosetta.ir.adapter.IRFieldNode;
import com.regnosys.rosetta.ir.adapter.IRMetadata;
import com.regnosys.rosetta.ir.adapter.IRModelNode;
import com.regnosys.rosetta.ir.adapter.IRNodeImpl;
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
import com.regnosys.rosetta.ir.core.IRFunctionSignature;
import com.regnosys.rosetta.ir.core.IRKind;
import com.regnosys.rosetta.ir.core.IRLabel;
import com.regnosys.rosetta.ir.core.IRNode;
import com.regnosys.rosetta.ir.core.IRQualifiableConfig;
import com.regnosys.rosetta.ir.core.IRQualificationFunction;
import com.regnosys.rosetta.ir.core.IRRuleReference;
import com.regnosys.rosetta.ir.core.IRType;
import com.regnosys.rosetta.ir.core.IRTypeArgument;
import com.regnosys.rosetta.ir.core.IRTypeParameter;
import com.regnosys.rosetta.ir.core.IRWithMetaUse;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * Reads the v1 JSON declaration wire form back into IR — the inverse of {@link IRJsonSerializer}
 * for the six declaration kinds (STRUCT/CHOICE/TYPE_ALIAS/ENUM/FIELD/ENUM_VALUE) plus the generic
 * ({@code IRNodeImpl}) fallback. Expressions are out of scope (their wire form is not yet frozen).
 *
 * <p><b>Strict validator.</b> Rejects an unknown {@code kind}, a missing/{@code != 1}
 * {@code irFormatVersion}, a missing required member, an unknown member, and a duplicate key —
 * each naming the offending document path. The nested D55 shapes (a doc reference, an annotation
 * use, a bounds pair, a label, a rule reference, an enum synonym, an annotation path) are read by
 * the same strict {@code Members} reader, so an unknown member inside one of them is rejected too.
 * A DEPENDENT member present without the member it depends on is rejected as well — a type
 * reference's {@code <prefix>Kind} and its {@code <prefix>Namespace} / {@code <prefix>ResolvedName}
 * stand or fall together, a {@code baseType*} fact needs a non-null {@code baseType}, a
 * {@code parent*} fact needs a {@code parent}, and {@code negated} needs a {@code literalValue} —
 * each message naming the offending member. The schema states the same four dependencies
 * ({@code dependentRequired} / {@code dependentSchemas}), so the reader refuses exactly what the
 * schema refuses.
 *
 * <p><b>The type gate (PR #643) adds four refusals, stated by the schema too.</b> A
 * {@code <prefix>EffectiveBase} without a {@code <prefix>Kind} is refused, and so is one whose
 * {@code <prefix>Kind} is not {@code TYPE_ALIAS} (only a collapsed alias chain has a base). An
 * effective base's own {@code kind} must be one of the five LEAF kinds
 * ({@link IREffectiveBase#LEAF_KINDS}) — every other {@link IRKind} is refused BY NAME — and each of
 * its {@code arguments} must be literal (a {@code nameValue} there is the record's own refusal,
 * re-raised naming the document path). {@code baseTypeArguments} and {@code typeParameters} are
 * refused on a node whose kind is not {@code TYPE_ALIAS}, which is the record's invariant too.
 *
 * <p><b>The property gate (PR #644) adds five more refusals, stated by the schema too.</b> A
 * {@code <prefix>AliasChain} obeys the {@code <prefix>EffectiveBase} law exactly - refused without a
 * {@code <prefix>Kind}, refused beside a kind that is not {@code TYPE_ALIAS}, and (under the
 * {@code baseType} prefix) refused without a {@code baseType} NAME. A {@code conditionKinds} list
 * that is not index-parallel to its {@code conditionNames}, or that carries a token outside
 * {@link IRAliasLink#CONDITION_KINDS}, is refused AT THAT MEMBER'S PATH - on a type node and on an
 * alias link alike. A MODEL node whose {@code namespace} disagrees with its {@code name} is refused
 * (the record makes them one string). A {@code with-meta} use carrying BOTH an argument type and a
 * refusal, NEITHER of them, a refusal token outside the vocabulary, constraints beside a refusal, or
 * a constraint passing a parameter through by name is the record's own refusal, re-raised at the
 * use's path; and an {@code argumentType*} fact without the {@code argumentType} name it is about is
 * refused exactly as an orphan {@code baseType*} fact is.
 *
 * <p><b>Reconstruction.</b> Omitted infra fields are rebuilt as {@code Optional.empty()} sourceRange
 * and {@link IRMetadata#EMPTY}; the D55 facts are read through the canonical (full-arity) node
 * constructors, and a member the serializer omitted (it was at its empty default) reads back at that
 * default — so a v1 document written before the enrichment deserializes exactly as it did then. A
 * type reference (a field's type, a data type's base) is an {@link IRTypeNode} carrying the written
 * name plus, when the document states them, the referent's kind and the resolved declaration's
 * namespace and own name; an unresolved reference keeps the placeholder {@code STRUCT} kind (the
 * residual kind-drop hole — the wire form does not state the kind of a reference it could not
 * resolve). The enum parent is reconstructed as an {@link IRKind#ENUM} reference, which is what
 * {@link com.regnosys.rosetta.ir.core.IREnum#parent()} contractually is. The generic node's
 * {@code unmodeled} member is a write-only class-derived field, recognized then discarded.
 *
 * <p><b>Round-trip contract.</b> The JSON-first round-trip
 * {@code new IRJsonSerializer().toJson(fromJson(json)).equals(json)} holds for the five modelled declaration
 * kinds and for a generic document ONLY when {@code unmodeled == "IRNodeImpl"} (the value regenerated on
 * re-serialization). The {@code unmodeled} field is write-only and non-preserved: it is recognized during
 * deserialization but discarded; on re-serialization it is regenerated from the reconstructed class
 * ({@link IRNodeImpl}) — always {@code "IRNodeImpl"}. A generic document whose original {@code unmodeled}
 * differed from {@code "IRNodeImpl"} would diverge on the JSON-first round-trip. The IR-first inverse
 * ({@code fromJson(serializer.toJson(node)).equals(node)}) holds for the five modelled declaration kinds,
 * but for a generic node ONLY when the source node's class is exactly {@link IRNodeImpl}: the serializer's
 * lossy generic fallback keeps just {@code kind}/{@code name}, so a generic node of any <em>other</em>
 * concrete {@code IRNode} class deserializes back to an {@code IRNodeImpl} — not {@code .equals} to the
 * original. Example:
 * <pre>{@code
 * List<IRNode> nodes = new IRJsonDeserializer().fromJson(jsonText);
 * }</pre>
 */
public final class IRJsonDeserializer {

    /** Reads a versioned document {@code {"irFormatVersion":1,"nodes":[...]}} into its declaration nodes. */
    public List<IRNode> fromJson(String text) {
        Members doc = new Members(JsonReader.parse(text), "$");
        long version = doc.requireInt("irFormatVersion");
        if (version != IRJsonSerializer.IR_FORMAT_VERSION) {
            throw new IRJsonException("unsupported irFormatVersion " + version + " (this reader supports "
                    + IRJsonSerializer.IR_FORMAT_VERSION + ") at $");
        }
        List<Json> nodesJson = doc.requireArray("nodes");
        doc.requireNoUnknown();
        List<IRNode> nodes = new ArrayList<>();
        for (int i = 0; i < nodesJson.size(); i++) {
            nodes.add(node(nodesJson.get(i), "$.nodes[" + i + "]"));
        }
        return nodes;
    }

    /** Reads a single bare node fragment (no version wrapper; mirrors the serializer's unversioned fragment). */
    public IRNode fromJsonNode(String text) {
        return node(JsonReader.parse(text), "$");
    }

    private IRNode node(Json json, String path) {
        Members m = new Members(json, path);
        IRKind kind = kind(m.requireString("kind"), path);
        IRNode result = switch (kind) {
            case STRUCT, CHOICE -> typeNode(m, kind, path);
            case TYPE_ALIAS -> typeAliasNode(m, path);
            case ENUM -> enumNode(m, path);
            case FIELD -> fieldNode(m, path);
            case ENUM_VALUE -> enumValueNode(m, path);
            // the property gate (PR #644): MODEL is a MODELLED kind now, so it leaves the generic
            // fallback exactly as TYPE_ALIAS did at the type gate.
            case MODEL -> modelNode(m, path);
            default -> genericNode(m, kind);
        };
        m.requireNoUnknown();
        return result;
    }

    private IRType typeNode(Members m, IRKind kind, String path) {
        String name = m.requireString("name");
        boolean isAbstract = m.requireBoolean("isAbstract");
        Optional<IRType> baseType = baseTypeRef(m, path);
        // the record's own invariant: only a TYPE_ALIAS node carries these two
        refuseTypeAliasOnly(m, "baseTypeArguments", kind, path);
        refuseTypeAliasOnly(m, "typeParameters", kind, path);
        List<IRField> fields = new ArrayList<>();
        List<Json> fieldsJson = m.requireArray("fields");
        for (int i = 0; i < fieldsJson.size(); i++) {
            String childPath = path + ".fields[" + i + "]";
            IRNode child = node(fieldsJson.get(i), childPath);
            if (!(child instanceof IRField field)) {
                throw new IRJsonException("expected a FIELD at " + childPath);
            }
            fields.add(field);
        }
        List<Optional<String>> conditionNames =
                conditionNames(m.arrayIfPresent("conditionNames"), path + ".conditionNames");
        return new IRTypeNode(name, kind, fields, baseType, isAbstract, Optional.empty(), IRMetadata.EMPTY,
                m.stringIfPresent("namespace"), m.stringIfPresent("resolvedName"),
                m.stringIfPresent("definition"), docReferences(m, path), annotations(m, path),
                conditionNames, Optional.empty(), List.of(), List.of(),
                conditionKinds(m, conditionNames, "type " + name, path), List.of());
    }

    /**
     * A {@code typeAlias} DECLARATION (the type gate, PR #643): the body as a {@code baseType}
     * reference, the arguments the body wrote, the declared parameters, then the declaration facts.
     * It states neither {@code fields} nor {@code isAbstract} (an alias declares neither) and no
     * {@code resolvedName} (a declaration carries none), so each is rebuilt at its empty default.
     */
    private IRType typeAliasNode(Members m, String path) {
        String name = m.requireString("name");
        Optional<IRType> baseType = baseTypeRef(m, path);
        List<IRTypeArgument> baseTypeArguments = each(m.arrayIfPresent("baseTypeArguments"),
                path + ".baseTypeArguments", IRJsonDeserializer::typeArgument);
        List<IRTypeParameter> typeParameters = each(m.arrayIfPresent("typeParameters"),
                path + ".typeParameters", IRJsonDeserializer::typeParameter);
        List<Optional<String>> conditionNames =
                conditionNames(m.arrayIfPresent("conditionNames"), path + ".conditionNames");
        return new IRTypeNode(name, IRKind.TYPE_ALIAS, List.of(), baseType, false, Optional.empty(),
                IRMetadata.EMPTY, m.stringIfPresent("namespace"), Optional.empty(),
                m.stringIfPresent("definition"), docReferences(m, path), annotations(m, path),
                conditionNames, Optional.empty(), typeParameters, baseTypeArguments,
                conditionKinds(m, conditionNames, "type alias " + name, path), List.of());
    }

    /**
     * The {@code baseType} member and its reference facts, shared by the data-type and the type-alias
     * readers: the written name (JSON {@code null} for a type with no base), and — only for a base
     * that is stated at all — the reference's kind, the resolved declaration's namespace and own name
     * and, for a collapsed alias body, its effective base. Every {@code baseType*} fact without a
     * {@code baseType} NAME is refused, the message naming the offending member.
     */
    private static Optional<IRType> baseTypeRef(Members m, String path) {
        Optional<String> baseName = m.optionalString("baseType");
        Optional<String> baseKind = m.stringIfPresent("baseTypeKind");
        Optional<String> baseNamespace = m.stringIfPresent("baseTypeNamespace");
        Optional<String> baseResolvedName = m.stringIfPresent("baseTypeResolvedName");
        Optional<Json> baseEffectiveBase = m.memberIfPresent("baseTypeEffectiveBase");
        List<Json> baseAliasChain = m.arrayIfPresent("baseTypeAliasChain");
        if (baseName.isEmpty()) {
            String orphan = baseKind.isPresent() ? "baseTypeKind"
                    : baseNamespace.isPresent() ? "baseTypeNamespace"
                    : baseResolvedName.isPresent() ? "baseTypeResolvedName"
                    : baseEffectiveBase.isPresent() ? "baseTypeEffectiveBase"
                    : !baseAliasChain.isEmpty() ? "baseTypeAliasChain" : null;
            if (orphan != null) {
                throw new IRJsonException("member '" + orphan + "' without a 'baseType' at " + path);
            }
        }
        return baseName.map(written -> typeRef(written, "baseType", baseKind, baseNamespace,
                baseResolvedName, baseEffectiveBase, baseAliasChain, path));
    }

    /**
     * Refuses a member only a TYPE_ALIAS DECLARATION may carry ({@code baseTypeArguments},
     * {@code typeParameters}) on a node of any other kind — the {@code IRTypeNode} invariant, stated
     * here with the document path instead of reaching the record as an {@code IllegalArgumentException}.
     */
    private static void refuseTypeAliasOnly(Members m, String member, IRKind kind, String path) {
        if (m.memberIfPresent(member).isPresent()) {
            throw new IRJsonException("member '" + member + "' on a node of kind " + kind + " at " + path
                    + " (only a TYPE_ALIAS declaration carries it)");
        }
    }

    private IRField fieldNode(Members m, String path) {
        String name = m.requireString("name");
        IRType type = typeRef(m.requireString("type"), "type", m.stringIfPresent("typeKind"),
                m.stringIfPresent("typeNamespace"), m.stringIfPresent("typeResolvedName"),
                m.memberIfPresent("typeEffectiveBase"), m.arrayIfPresent("typeAliasChain"), path);
        Cardinality cardinality = cardinality(m.requireString("cardinality"), path);
        Optional<IRBounds> bounds = m.memberIfPresent("bounds")
                .map(json -> bounds(json, path + ".bounds"));
        return new IRFieldNode(name, type, cardinality, Optional.empty(), IRMetadata.EMPTY,
                bounds, m.flagIfPresent("isOverride"),
                each(m.arrayIfPresent("typeArguments"), path + ".typeArguments",
                        IRJsonDeserializer::typeArgument),
                m.stringIfPresent("definition"), docReferences(m, path), annotations(m, path),
                each(m.arrayIfPresent("labels"), path + ".labels", IRJsonDeserializer::label),
                each(m.arrayIfPresent("ruleReferences"), path + ".ruleReferences",
                        IRJsonDeserializer::ruleReference));
    }

    private IRNode enumNode(Members m, String path) {
        String name = m.requireString("name");
        List<IREnumValue> values = new ArrayList<>();
        List<Json> valuesJson = m.requireArray("values");
        for (int i = 0; i < valuesJson.size(); i++) {
            String childPath = path + ".values[" + i + "]";
            IRNode child = node(valuesJson.get(i), childPath);
            if (!(child instanceof IREnumValue value)) {
                throw new IRJsonException("expected an ENUM_VALUE at " + childPath);
            }
            values.add(value);
        }
        Optional<String> parentName = m.stringIfPresent("parent");
        Optional<String> parentNamespace = m.stringIfPresent("parentNamespace");
        Optional<String> parentResolvedName = m.stringIfPresent("parentResolvedName");
        if (parentName.isEmpty() && (parentNamespace.isPresent() || parentResolvedName.isPresent())) {
            throw new IRJsonException("member '"
                    + (parentNamespace.isPresent() ? "parentNamespace" : "parentResolvedName")
                    + "' without a 'parent' at " + path);
        }
        // IREnum#parent is contractually an ENUM reference, so the wire form states no parent kind.
        Optional<IRType> parent = parentName.map(written ->
                IRTypeNode.reference(written, IRKind.ENUM, parentNamespace, parentResolvedName));
        return new IREnumNode(name, values, Optional.empty(), IRMetadata.EMPTY,
                m.stringIfPresent("namespace"), parent, m.stringIfPresent("definition"),
                docReferences(m, path), annotations(m, path));
    }

    private IRNode enumValueNode(Members m, String path) {
        String name = m.requireString("name");
        Optional<String> displayName = m.optionalString("displayName");
        return new IREnumValueNode(name, displayName, Optional.empty(), IRMetadata.EMPTY,
                m.stringIfPresent("definition"), docReferences(m, path), annotations(m, path),
                each(m.arrayIfPresent("synonyms"), path + ".synonyms", IRJsonDeserializer::enumSynonym));
    }

    // -------------------------------------------------------------------------
    // The property gate (v3.3 seat 8, PR #644) — the MODEL-level node
    // -------------------------------------------------------------------------

    /**
     * THE MODEL NODE (PR #644): the facts of one {@code namespace} declaration. {@code name} and
     * {@code namespace} are the SAME string — {@link IRModelNode#namespace()} returns the name, so a
     * document stating two different strings could not be read back unchanged and is REFUSED rather
     * than silently rewritten. Every other member is optional and an absent one reads as its empty
     * default; the infra fields ({@code sourceRange}, {@code metadata}) are rebuilt as every other
     * node's are.
     */
    private IRNode modelNode(Members m, String path) {
        String name = m.requireString("name");
        String namespace = m.requireString("namespace");
        if (!namespace.equals(name)) {
            throw new IRJsonException("member 'namespace' states '" + namespace
                    + "' while the node's name is '" + name + "' at " + path
                    + " (a model node's name IS its namespace)");
        }
        List<IRQualifiableConfig> qualifiableConfigs = each(m.arrayIfPresent("qualifiableConfigs"),
                path + ".qualifiableConfigs", IRJsonDeserializer::qualifiableConfig);
        List<IRQualificationFunction> qualificationFunctions =
                each(m.arrayIfPresent("qualificationFunctions"), path + ".qualificationFunctions",
                        IRJsonDeserializer::qualificationFunction);
        List<IRFunctionSignature> functionSignatures = each(m.arrayIfPresent("functionSignatures"),
                path + ".functionSignatures", this::functionSignature);
        List<IRWithMetaUse> withMetaUses = each(m.arrayIfPresent("withMetaUses"),
                path + ".withMetaUses", IRJsonDeserializer::withMetaUse);
        try {
            return new IRModelNode(name, m.stringIfPresent("definition"), m.stringIfPresent("version"),
                    qualifiableConfigs, qualificationFunctions, functionSignatures, withMetaUses,
                    Optional.empty(), IRMetadata.EMPTY);
        } catch (IllegalArgumentException e) {
            throw new IRJsonException(e.getMessage() + " at " + path);
        }
    }

    /**
     * One {@code isEvent root} / {@code isProduct root} declaration. A {@code kind} outside the two
     * tokens the grammar admits is the record's own refusal, re-raised AT THE MEMBER'S OWN PATH.
     */
    private static IRQualifiableConfig qualifiableConfig(Json json, String path) {
        Members m = new Members(json, path);
        String kind = m.requireString("kind");
        IRType rootType = typeRef(m.requireString("rootType"), "rootType",
                m.stringIfPresent("rootTypeKind"), m.stringIfPresent("rootTypeNamespace"),
                m.stringIfPresent("rootTypeResolvedName"), m.memberIfPresent("rootTypeEffectiveBase"),
                m.arrayIfPresent("rootTypeAliasChain"), path);
        IRQualifiableConfig config;
        try {
            config = new IRQualifiableConfig(kind, rootType);
        } catch (IllegalArgumentException e) {
            throw new IRJsonException(e.getMessage() + " at " + path + ".kind");
        }
        m.requireNoUnknown();
        return config;
    }

    /** One {@code [qualification]} function: its name and its FIRST input's type as a reference. */
    private static IRQualificationFunction qualificationFunction(Json json, String path) {
        Members m = new Members(json, path);
        String name = m.requireString("name");
        IRType firstInputType = typeRef(m.requireString("firstInputType"), "firstInputType",
                m.stringIfPresent("firstInputTypeKind"), m.stringIfPresent("firstInputTypeNamespace"),
                m.stringIfPresent("firstInputTypeResolvedName"),
                m.memberIfPresent("firstInputTypeEffectiveBase"),
                m.arrayIfPresent("firstInputTypeAliasChain"), path);
        IRQualificationFunction function;
        try {
            function = new IRQualificationFunction(name, firstInputType);
        } catch (IllegalArgumentException e) {
            throw new IRJsonException(e.getMessage() + " at " + path);
        }
        m.requireNoUnknown();
        return function;
    }

    /** One function's SIGNATURE: its inputs and its output read by the FIELD node reader itself. */
    private IRFunctionSignature functionSignature(Json json, String path) {
        Members m = new Members(json, path);
        String name = m.requireString("name");
        List<IRField> inputs = each(m.arrayIfPresent("inputs"), path + ".inputs", this::field);
        Optional<IRField> output = m.memberIfPresent("output")
                .map(value -> field(value, path + ".output"));
        IRFunctionSignature signature;
        try {
            signature = new IRFunctionSignature(name, inputs, output);
        } catch (IllegalArgumentException e) {
            throw new IRJsonException(e.getMessage() + " at " + path);
        }
        m.requireNoUnknown();
        return signature;
    }

    /** A nested FIELD node, read by the strict node reader and refused when its kind is not FIELD. */
    private IRField field(Json json, String path) {
        IRNode child = node(json, path);
        if (!(child instanceof IRField declared)) {
            throw new IRJsonException("expected a FIELD at " + path);
        }
        return declared;
    }

    /**
     * One {@code with-meta} expression. Its argument type rides the {@code argumentType} prefix under
     * the same reference law every other reference obeys, so a fact about it without the NAME it is
     * about is refused here exactly as a {@code baseType*} fact is. EXACTLY ONE of the argument type
     * and the refusal, a refusal token inside the vocabulary, and no constraints on a refused use are
     * the {@link IRWithMetaUse} record's own three laws, re-raised with the document path.
     */
    private static IRWithMetaUse withMetaUse(Json json, String path) {
        Members m = new Members(json, path);
        List<String> entryNames = strings(m.arrayIfPresent("entryNames"), path + ".entryNames");
        Optional<String> argumentName = m.stringIfPresent("argumentType");
        Optional<String> argumentKind = m.stringIfPresent("argumentTypeKind");
        Optional<String> argumentNamespace = m.stringIfPresent("argumentTypeNamespace");
        Optional<String> argumentResolvedName = m.stringIfPresent("argumentTypeResolvedName");
        Optional<Json> argumentEffectiveBase = m.memberIfPresent("argumentTypeEffectiveBase");
        List<Json> argumentAliasChain = m.arrayIfPresent("argumentTypeAliasChain");
        if (argumentName.isEmpty()) {
            String orphan = argumentKind.isPresent() ? "argumentTypeKind"
                    : argumentNamespace.isPresent() ? "argumentTypeNamespace"
                    : argumentResolvedName.isPresent() ? "argumentTypeResolvedName"
                    : argumentEffectiveBase.isPresent() ? "argumentTypeEffectiveBase"
                    : !argumentAliasChain.isEmpty() ? "argumentTypeAliasChain" : null;
            if (orphan != null) {
                throw new IRJsonException("member '" + orphan + "' without an 'argumentType' at " + path);
            }
        }
        Optional<IRType> argumentType = argumentName.map(written ->
                typeRef(written, "argumentType", argumentKind, argumentNamespace, argumentResolvedName,
                        argumentEffectiveBase, argumentAliasChain, path));
        List<IRTypeArgument> typeArguments = each(m.arrayIfPresent("typeArguments"),
                path + ".typeArguments", IRJsonDeserializer::typeArgument);
        IRWithMetaUse use;
        try {
            use = new IRWithMetaUse(entryNames, argumentType, typeArguments,
                    m.stringIfPresent("refusal"));
        } catch (IllegalArgumentException e) {
            throw new IRJsonException(e.getMessage() + " at " + path);
        }
        m.requireNoUnknown();
        return use;
    }

    /**
     * One RUNG of a {@code typeAlias} chain. The condition-kinds law is checked FIRST, so a kinds list
     * that is not index-parallel to the names (or that carries a token outside the vocabulary) is
     * refused at {@code <chain>[i].conditionKinds} rather than reaching the record unlocated.
     */
    private static IRAliasLink aliasLink(Json json, String path) {
        Members m = new Members(json, path);
        String name = m.requireString("name");
        Optional<String> namespace = m.stringIfPresent("namespace");
        List<String> parameterNames = strings(m.arrayIfPresent("parameterNames"),
                path + ".parameterNames");
        List<Optional<String>> names =
                conditionNames(m.arrayIfPresent("conditionNames"), path + ".conditionNames");
        List<String> kinds = conditionKinds(m, names, "alias link '" + name + "'", path);
        IRAliasLink link;
        try {
            link = new IRAliasLink(name, namespace, parameterNames, names, kinds);
        } catch (IllegalArgumentException e) {
            throw new IRJsonException(e.getMessage() + " at " + path);
        }
        m.requireNoUnknown();
        return link;
    }

    /**
     * The shared {@code conditionKinds} member: read as strings, then checked against the names by the
     * ONE law ({@link IRAliasLink#checkConditionKinds}) both a type node and an alias link obey —
     * refused AT THE MEMBER'S OWN PATH, because a fact half-stated is no fact.
     */
    private static List<String> conditionKinds(Members m, List<Optional<String>> names, String owner,
                                               String path) {
        List<String> kinds = strings(m.arrayIfPresent("conditionKinds"), path + ".conditionKinds");
        try {
            IRAliasLink.checkConditionKinds(owner, names, kinds);
        } catch (IllegalArgumentException e) {
            throw new IRJsonException(e.getMessage() + " at " + path + ".conditionKinds");
        }
        return kinds;
    }

    private IRNode genericNode(Members m, IRKind kind) {
        String name = m.requireString("name");
        m.consume("unmodeled"); // required-and-discarded: the serializer always emits this write-only class-derived field, so consume() (which delegates to require()) never throws here
        return new IRNodeImpl(name, kind, List.of(), Optional.empty(), IRMetadata.EMPTY);
    }

    /**
     * A type reference: the name the model wrote, plus the referent's kind and the resolved
     * declaration's namespace and own name when the document states them (it states them only for a
     * reference that resolved). An unresolved reference keeps the placeholder {@code STRUCT} kind —
     * the residual kind-drop hole — which is exactly what the adapter itself produces for one.
     *
     * <p>ONE law, the writer's ({@code IRJsonSerializer.addReferenceFacts}), enforced here and stated
     * by the schema's {@code dependentRequired} / {@code dependentSchemas}: {@code <prefix>Kind} is
     * present EXACTLY when the reference resolved, i.e. when {@code <prefix>Namespace} or
     * {@code <prefix>ResolvedName} is present. Either half alone is a document the writer never
     * emits and could not read back unchanged — a kind with no resolved fact re-serializes without
     * the kind, a resolved fact with no kind re-serializes with the placeholder {@code STRUCT} — so
     * both are refused rather than silently rewritten.
     *
     * <p>The type gate (PR #643) adds the collapsed chain, {@code <prefix>EffectiveBase}: a fact ABOUT
     * a resolved TYPE_ALIAS reference, so it is refused without a {@code <prefix>Kind} and refused
     * beside a {@code <prefix>Kind} that is not {@code TYPE_ALIAS}. The schema states both.
     */
    private static IRType typeRef(String name, String prefix, Optional<String> kindToken,
                                  Optional<String> namespace, Optional<String> resolvedName,
                                  Optional<Json> effectiveBaseJson, List<Json> aliasChainJson,
                                  String path) {
        boolean resolved = namespace.isPresent() || resolvedName.isPresent();
        if (kindToken.isPresent() && !resolved) {
            throw new IRJsonException("member '" + prefix + "Kind' without a '" + prefix + "Namespace'"
                    + " or '" + prefix + "ResolvedName' at " + path
                    + " (the kind is written only for a reference that resolved)");
        }
        if (kindToken.isEmpty() && resolved) {
            throw new IRJsonException("member '"
                    + (namespace.isPresent() ? prefix + "Namespace" : prefix + "ResolvedName")
                    + "' without a '" + prefix + "Kind' at " + path);
        }
        if (effectiveBaseJson.isPresent() && kindToken.isEmpty()) {
            throw new IRJsonException("member '" + prefix + "EffectiveBase' without a '" + prefix
                    + "Kind' at " + path);
        }
        if (!aliasChainJson.isEmpty() && kindToken.isEmpty()) {
            throw new IRJsonException("member '" + prefix + "AliasChain' without a '" + prefix
                    + "Kind' at " + path);
        }
        IRKind referentKind = kindToken.isPresent() ? referenceKind(kindToken.get(), path) : IRKind.STRUCT;
        if (effectiveBaseJson.isPresent() && referentKind != IRKind.TYPE_ALIAS) {
            throw new IRJsonException("member '" + prefix + "EffectiveBase' beside a '" + prefix
                    + "Kind' of " + referentKind + " at " + path
                    + " (only a TYPE_ALIAS reference collapses to an effective base)");
        }
        if (!aliasChainJson.isEmpty() && referentKind != IRKind.TYPE_ALIAS) {
            throw new IRJsonException("member '" + prefix + "AliasChain' beside a '" + prefix
                    + "Kind' of " + referentKind + " at " + path
                    + " (only a TYPE_ALIAS reference walks an alias chain)");
        }
        return IRTypeNode.reference(name, referentKind, namespace, resolvedName,
                effectiveBaseJson.map(json -> effectiveBase(json, path + "." + prefix + "EffectiveBase")),
                each(aliasChainJson, path + "." + prefix + "AliasChain", IRJsonDeserializer::aliasLink));
    }

    /**
     * The leaf a TYPE_ALIAS chain collapses to. Its {@code kind} must be one of the five LEAF kinds
     * ({@link IREffectiveBase#LEAF_KINDS}; the schema's own list, so the reader admits exactly what the
     * schema admits) and each of its {@code arguments} must be LITERAL — an effective base never
     * carries a parameter passed through by name, the record's law, re-raised here with the path.
     */
    private static IREffectiveBase effectiveBase(Json json, String path) {
        Members m = new Members(json, path);
        IREffectiveBase base;
        try {
            base = new IREffectiveBase(effectiveBaseKind(m.requireString("kind"), path),
                    m.requireString("name"), m.stringIfPresent("namespace"),
                    each(m.arrayIfPresent("arguments"), path + ".arguments",
                            IRJsonDeserializer::typeArgument));
        } catch (IllegalArgumentException e) {
            throw new IRJsonException(e.getMessage() + " at " + path);
        }
        m.requireNoUnknown();
        return base;
    }

    /** An effective base's kind: one of the schema's five leaf kinds — any other {@link IRKind} is refused. */
    private static IRKind effectiveBaseKind(String token, String path) {
        IRKind kind = kind(token, path);
        for (IRKind admitted : IRJsonSchema.EFFECTIVE_BASE_KINDS) {
            if (admitted == kind) {
                return kind;
            }
        }
        throw new IRJsonException("kind '" + token + "' cannot be an effective base's kind at " + path
                + " (the schema admits " + java.util.Arrays.toString(IRJsonSchema.EFFECTIVE_BASE_KINDS) + ")");
    }

    /**
     * One declared parameter of a {@code typeAlias}: its name, its declared type as a REFERENCE under
     * the {@code type} prefix (the same law as a field's), the arguments its own type call wrote
     * ({@code typeArguments}, OPTIONAL - PR #644), and the documentation when stated.
     */
    private static IRTypeParameter typeParameter(Json json, String path) {
        Members m = new Members(json, path);
        String name = m.requireString("name");
        IRType type = typeRef(m.requireString("type"), "type", m.stringIfPresent("typeKind"),
                m.stringIfPresent("typeNamespace"), m.stringIfPresent("typeResolvedName"),
                m.memberIfPresent("typeEffectiveBase"), m.arrayIfPresent("typeAliasChain"), path);
        List<IRTypeArgument> typeArguments = each(m.arrayIfPresent("typeArguments"),
                path + ".typeArguments", IRJsonDeserializer::typeArgument);
        IRTypeParameter parameter;
        try {
            parameter = new IRTypeParameter(name, type, m.stringIfPresent("definition"), typeArguments);
        } catch (IllegalArgumentException e) {
            throw new IRJsonException(e.getMessage() + " at " + path);
        }
        m.requireNoUnknown();
        return parameter;
    }

    /** A type REFERENCE's kind: one of the schema's seven referenceable kinds - any other {@link IRKind} is refused. */
    private static IRKind referenceKind(String token, String path) {
        IRKind kind = kind(token, path);
        for (IRKind admitted : IRJsonSchema.TYPE_REFERENCE_KINDS) {
            if (admitted == kind) {
                return kind;
            }
        }
        throw new IRJsonException("kind '" + token + "' cannot be a type reference's kind at " + path
                + " (the schema admits " + java.util.Arrays.toString(IRJsonSchema.TYPE_REFERENCE_KINDS) + ")");
    }

    // -------------------------------------------------------------------------
    // The D55 declaration facts — nested shapes
    // -------------------------------------------------------------------------

    private List<IRDocReference> docReferences(Members m, String path) {
        return each(m.arrayIfPresent("docReferences"), path + ".docReferences",
                IRJsonDeserializer::docReference);
    }

    private List<IRAnnotationUse> annotations(Members m, String path) {
        return each(m.arrayIfPresent("annotations"), path + ".annotations",
                IRJsonDeserializer::annotationUse);
    }

    /** Reads each element of an array member, naming its index in the path of any rejection. */
    private static <T> List<T> each(List<Json> elements, String path, BiFunction<Json, String, T> read) {
        List<T> out = new ArrayList<>();
        for (int i = 0; i < elements.size(); i++) {
            out.add(read.apply(elements.get(i), path + "[" + i + "]"));
        }
        return out;
    }

    /** A condition name is a string, or JSON {@code null} for an unnamed condition. */
    private static List<Optional<String>> conditionNames(List<Json> elements, String path) {
        List<Optional<String>> names = new ArrayList<>();
        for (int i = 0; i < elements.size(); i++) {
            Json element = elements.get(i);
            if (element instanceof Json.Str str) {
                names.add(Optional.of(str.value()));
            } else if (element instanceof Json.Raw raw && raw.token().equals("null")) {
                names.add(Optional.empty());
            } else {
                throw new IRJsonException("expected a string or null at " + path + "[" + i + "]");
            }
        }
        return names;
    }

    private static List<String> strings(List<Json> elements, String path) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < elements.size(); i++) {
            if (!(elements.get(i) instanceof Json.Str str)) {
                throw new IRJsonException("expected a string at " + path + "[" + i + "]");
            }
            out.add(str.value());
        }
        return out;
    }

    private static IRBounds bounds(Json json, String path) {
        Members m = new Members(json, path);
        IRBounds bounds;
        try {
            bounds = new IRBounds(m.requireInteger("lower"), m.integerIfPresent("upper"));
        } catch (IllegalArgumentException e) {
            throw new IRJsonException(e.getMessage() + " at " + path);
        }
        m.requireNoUnknown();
        return bounds;
    }

    private static IRTypeArgument typeArgument(Json json, String path) {
        Members m = new Members(json, path);
        IRTypeArgument argument;
        try {
            argument = new IRTypeArgument(m.requireString("parameter"),
                    m.stringIfPresent("nameValue"), m.stringIfPresent("literalValue"),
                    m.flagIfPresent("negated"));
        } catch (IllegalArgumentException e) {
            throw new IRJsonException(e.getMessage() + " at " + path);
        }
        m.requireNoUnknown();
        return argument;
    }

    private static IRAnnotationPath annotationPath(Json json, String path) {
        Members m = new Members(json, path);
        IRAnnotationPath result = new IRAnnotationPath(m.flagIfPresent("rootItem"),
                m.stringIfPresent("root").orElse(""),
                each(m.arrayIfPresent("steps"), path + ".steps", IRJsonDeserializer::pathStep));
        m.requireNoUnknown();
        return result;
    }

    private static IRAnnotationPath.Step pathStep(Json json, String path) {
        Members m = new Members(json, path);
        IRAnnotationPath.Step step =
                new IRAnnotationPath.Step(m.requireString("name"), m.flagIfPresent("deep"));
        m.requireNoUnknown();
        return step;
    }

    private static IRAnnotationUse annotationUse(Json json, String path) {
        Members m = new Members(json, path);
        IRAnnotationUse use = new IRAnnotationUse(m.requireString("name"), m.stringIfPresent("qualifier"),
                each(m.arrayIfPresent("arguments"), path + ".arguments",
                        IRJsonDeserializer::annotationArgument));
        m.requireNoUnknown();
        return use;
    }

    private static IRAnnotationUse.Argument annotationArgument(Json json, String path) {
        Members m = new Members(json, path);
        IRAnnotationUse.Argument argument = new IRAnnotationUse.Argument(m.requireString("key"),
                m.requireString("value"), m.flagIfPresent("attributeRef"));
        m.requireNoUnknown();
        return argument;
    }

    private static IRDocReference docReference(Json json, String path) {
        Members m = new Members(json, path);
        IRDocReference reference = new IRDocReference(m.flagIfPresent("regulatory"),
                m.memberIfPresent("path").map(p -> annotationPath(p, path + ".path")),
                m.stringIfPresent("body"),
                each(m.arrayIfPresent("corpora"), path + ".corpora", IRJsonDeserializer::corpus),
                each(m.arrayIfPresent("segments"), path + ".segments", IRJsonDeserializer::segment),
                each(m.arrayIfPresent("rationales"), path + ".rationales", IRJsonDeserializer::rationale),
                m.stringIfPresent("structuredProvision"), m.stringIfPresent("provision"),
                m.flagIfPresent("reportedField"),
                each(m.arrayIfPresent("namedArgs"), path + ".namedArgs", IRJsonDeserializer::namedArg));
        m.requireNoUnknown();
        return reference;
    }

    private static IRDocReference.Corpus corpus(Json json, String path) {
        Members m = new Members(json, path);
        IRDocReference.Corpus corpus = new IRDocReference.Corpus(m.requireString("reference"),
                m.memberIfPresent("resolved").map(r -> corpusDeclaration(r, path + ".resolved")));
        m.requireNoUnknown();
        return corpus;
    }

    private static IRDocReference.Corpus.Declaration corpusDeclaration(Json json, String path) {
        Members m = new Members(json, path);
        IRDocReference.Corpus.Declaration declaration = new IRDocReference.Corpus.Declaration(
                m.stringIfPresent("typeKeyword"), m.requireString("name"),
                m.stringIfPresent("displayName"), m.stringIfPresent("definition"));
        m.requireNoUnknown();
        return declaration;
    }

    private static IRDocReference.Segment segment(Json json, String path) {
        Members m = new Members(json, path);
        IRDocReference.Segment segment =
                new IRDocReference.Segment(m.requireString("name"), m.requireString("value"));
        m.requireNoUnknown();
        return segment;
    }

    private static IRDocReference.Rationale rationale(Json json, String path) {
        Members m = new Members(json, path);
        IRDocReference.Rationale rationale =
                new IRDocReference.Rationale(m.stringIfPresent("text"), m.stringIfPresent("author"));
        m.requireNoUnknown();
        return rationale;
    }

    private static IRDocReference.NamedArg namedArg(Json json, String path) {
        Members m = new Members(json, path);
        IRDocReference.NamedArg arg =
                new IRDocReference.NamedArg(m.requireString("name"), m.requireString("value"));
        m.requireNoUnknown();
        return arg;
    }

    private static IRLabel label(Json json, String path) {
        Members m = new Members(json, path);
        IRLabel label = new IRLabel(m.requireString("label"),
                m.memberIfPresent("forPath").map(p -> annotationPath(p, path + ".forPath")),
                m.memberIfPresent("asPath").map(p -> annotationPath(p, path + ".asPath")));
        m.requireNoUnknown();
        return label;
    }

    private static IRRuleReference ruleReference(Json json, String path) {
        Members m = new Members(json, path);
        IRRuleReference reference = new IRRuleReference(
                m.memberIfPresent("forPath").map(p -> annotationPath(p, path + ".forPath")),
                m.stringIfPresent("ruleName"), m.stringIfPresent("resolvedNamespace"),
                m.stringIfPresent("resolvedName"));
        m.requireNoUnknown();
        return reference;
    }

    private static IREnumSynonym enumSynonym(Json json, String path) {
        Members m = new Members(json, path);
        IREnumSynonym synonym = new IREnumSynonym(
                strings(m.arrayIfPresent("sources"), path + ".sources"), m.requireString("value"),
                m.stringIfPresent("definition"), m.stringIfPresent("patternMatch"),
                m.stringIfPresent("patternReplace"), m.flagIfPresent("removeHtml"));
        m.requireNoUnknown();
        return synonym;
    }

    private static IRKind kind(String token, String path) {
        try {
            return IRKind.valueOf(token);
        } catch (IllegalArgumentException e) {
            throw new IRJsonException("unknown kind '" + token + "' at " + path);
        }
    }

    private static Cardinality cardinality(String token, String path) {
        try {
            return Cardinality.valueOf(token);
        } catch (IllegalArgumentException e) {
            throw new IRJsonException("unknown cardinality '" + token + "' at " + path);
        }
    }

    /**
     * A strict reader over a {@link Json.Obj}: builds a name→value map (rejecting duplicate keys),
     * serves typed member reads that mark each key consumed, and rejects any unconsumed member.
     */
    private static final class Members {
        private final String path;
        private final Map<String, Json> map = new LinkedHashMap<>();
        private final Set<String> consumed = new HashSet<>();

        Members(Json json, String path) {
            this.path = path;
            if (!(json instanceof Json.Obj obj)) {
                throw new IRJsonException("expected an object at " + path);
            }
            for (Json.Member member : obj.members()) {
                if (map.put(member.name(), member.value()) != null) {
                    throw new IRJsonException("duplicate member '" + member.name() + "' at " + path);
                }
            }
        }

        String requireString(String key) {
            Json v = require(key);
            if (v instanceof Json.Str str) {
                return str.value();
            }
            throw typeError(key, "a string");
        }

        /** The key must be PRESENT; returns {@code Optional.empty()} iff the value is JSON {@code null}
         *  (an absent key throws "missing member"). This is sound because the serializer always emits the
         *  key — as {@code null} when the optional is empty. */
        Optional<String> optionalString(String key) {
            Json v = require(key);
            if (v instanceof Json.Str str) {
                return Optional.of(str.value());
            }
            if (isNull(v)) {
                return Optional.empty();
            }
            throw typeError(key, "a string or null");
        }

        boolean requireBoolean(String key) {
            String token = requireRaw(key);
            if (token.equals("true")) {
                return true;
            }
            if (token.equals("false")) {
                return false;
            }
            throw typeError(key, "a boolean");
        }

        long requireInt(String key) {
            String token = requireRaw(key);
            try {
                return Long.parseLong(token);
            } catch (NumberFormatException e) {
                throw typeError(key, "an integer");
            }
        }

        List<Json> requireArray(String key) {
            Json v = require(key);
            if (v instanceof Json.Arr arr) {
                return arr.elements();
            }
            throw typeError(key, "an array");
        }

        void consume(String key) {
            require(key);
        }

        // ── the D55 optional members: ABSENT reads as the fact's empty default ─────────────────

        /** The raw value of an optional member, marked consumed; {@code null} when the key is absent. */
        private Json optionalValue(String key) {
            Json v = map.get(key);
            if (v == null) {
                return null;
            }
            consumed.add(key);
            return v;
        }

        /** An optional member's value, for the nested object shapes; empty when the key is absent. */
        Optional<Json> memberIfPresent(String key) {
            return Optional.ofNullable(optionalValue(key));
        }

        /** An optional string member; empty when the key is absent (never written as {@code null}). */
        Optional<String> stringIfPresent(String key) {
            Json v = optionalValue(key);
            if (v == null) {
                return Optional.empty();
            }
            if (v instanceof Json.Str str) {
                return Optional.of(str.value());
            }
            throw typeError(key, "a string");
        }

        /**
         * An optional flag member; {@code false} when the key is absent. A flag is written ONLY when it is set (the
         * schema pins it {@code {"const": true}}), so an explicit {@code false} is REFUSED: it would read back as the
         * absent member and re-serialize without it - a document that does not round-trip is not admitted.
         */
        boolean flagIfPresent(String key) {
            Json v = optionalValue(key);
            if (v == null) {
                return false;
            }
            if (v instanceof Json.Raw raw && raw.token().equals("true")) {
                return true;
            }
            throw typeError(key, "the constant true (a flag is written only when it is set)");
        }

        /** An optional array member; the empty list when the key is absent. */
        List<Json> arrayIfPresent(String key) {
            Json v = optionalValue(key);
            if (v == null) {
                return List.of();
            }
            if (v instanceof Json.Arr arr) {
                return arr.elements();
            }
            throw typeError(key, "an array");
        }

        /** A required unbounded integer member (a cardinality bound is not capped at {@code long}). */
        BigInteger requireInteger(String key) {
            String token = requireRaw(key);
            try {
                return new BigInteger(token);
            } catch (NumberFormatException e) {
                throw typeError(key, "an integer");
            }
        }

        /** An optional unbounded integer member; empty when the key is absent. */
        Optional<BigInteger> integerIfPresent(String key) {
            Json v = optionalValue(key);
            if (v == null) {
                return Optional.empty();
            }
            if (v instanceof Json.Raw raw) {
                try {
                    return Optional.of(new BigInteger(raw.token()));
                } catch (NumberFormatException e) {
                    throw typeError(key, "an integer");
                }
            }
            throw typeError(key, "an integer");
        }

        void requireNoUnknown() {
            for (String key : map.keySet()) {
                if (!consumed.contains(key)) {
                    throw new IRJsonException("unknown member '" + key + "' at " + path);
                }
            }
        }

        private Json require(String key) {
            Json v = map.get(key);
            if (v == null) {
                throw new IRJsonException("missing member '" + key + "' at " + path);
            }
            consumed.add(key);
            return v;
        }

        private String requireRaw(String key) {
            Json v = require(key);
            if (v instanceof Json.Raw raw) {
                return raw.token();
            }
            throw typeError(key, "a literal");
        }

        private static boolean isNull(Json v) {
            return v instanceof Json.Raw raw && raw.token().equals("null");
        }

        private IRJsonException typeError(String key, String expected) {
            return new IRJsonException("member '" + key + "' is not " + expected + " at " + path);
        }
    }
}
