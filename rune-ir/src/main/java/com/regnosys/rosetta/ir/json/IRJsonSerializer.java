package com.regnosys.rosetta.ir.json;

import com.regnosys.rosetta.ir.core.IRAliasLink;
import com.regnosys.rosetta.ir.core.IRAnnotationPath;
import com.regnosys.rosetta.ir.core.IRAnnotationUse;
import com.regnosys.rosetta.ir.core.IRBounds;
import com.regnosys.rosetta.ir.core.IRDocReference;
import com.regnosys.rosetta.ir.core.IREffectiveBase;
import com.regnosys.rosetta.ir.core.IREnum;
import com.regnosys.rosetta.ir.core.IREnumSynonym;
import com.regnosys.rosetta.ir.core.IREnumValue;
import com.regnosys.rosetta.ir.core.IRField;
import com.regnosys.rosetta.ir.core.IRFunctionSignature;
import com.regnosys.rosetta.ir.core.IRLabel;
import com.regnosys.rosetta.ir.core.IRModel;
import com.regnosys.rosetta.ir.core.IRNode;
import com.regnosys.rosetta.ir.core.IRQualifiableConfig;
import com.regnosys.rosetta.ir.core.IRQualificationFunction;
import com.regnosys.rosetta.ir.core.IRRuleReference;
import com.regnosys.rosetta.ir.core.IRType;
import com.regnosys.rosetta.ir.core.IRTypeArgument;
import com.regnosys.rosetta.ir.core.IRTypeParameter;
import com.regnosys.rosetta.ir.core.IRWithMetaUse;
import com.regnosys.rosetta.ir.expr.BinaryOp;
import com.regnosys.rosetta.ir.expr.Existence;
import com.regnosys.rosetta.ir.expr.FieldAccess;
import com.regnosys.rosetta.ir.expr.IRApply;
import com.regnosys.rosetta.ir.expr.IRConditional;
import com.regnosys.rosetta.ir.expr.IRConversion;
import com.regnosys.rosetta.ir.expr.IREmptyLiteral;
import com.regnosys.rosetta.ir.expr.IRExpr;
import com.regnosys.rosetta.ir.expr.IRConstruct;
import com.regnosys.rosetta.ir.expr.IRLambdaOp;
import com.regnosys.rosetta.ir.expr.IRListConstruct;
import com.regnosys.rosetta.ir.expr.IRListOp;
import com.regnosys.rosetta.ir.expr.IRMetaAccess;
import com.regnosys.rosetta.ir.expr.IROnlyExists;
import com.regnosys.rosetta.ir.expr.IRPipe;
import com.regnosys.rosetta.ir.expr.IRLiteral;
import com.regnosys.rosetta.ir.expr.IRReference;
import com.regnosys.rosetta.ir.expr.IRPointFreeApply;
import com.regnosys.rosetta.ir.expr.IRAllAnyCompare;
import com.regnosys.rosetta.ir.expr.IRChoiceOptionNav;
import com.regnosys.rosetta.ir.expr.IRDeepFeatureNav;
import com.regnosys.rosetta.ir.expr.IRDispatchInputRef;
import com.regnosys.rosetta.ir.expr.IRRecordFeatureNav;
import com.regnosys.rosetta.ir.expr.IRSynItemNav;
import com.regnosys.rosetta.ir.expr.IRClosureParam;
import com.regnosys.rosetta.ir.expr.IRMetaOutputApply;
import com.regnosys.rosetta.ir.expr.IRSymbolNav;
import com.regnosys.rosetta.ir.expr.IRToString;
import com.regnosys.rosetta.ir.expr.IRVariable;
import com.regnosys.rosetta.ir.expr.Let;
import com.regnosys.rosetta.types.RMetaAnnotatedType;
import com.regnosys.rosetta.types.RNumberType;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Serializes the neutral IR to a deterministic, versioned JSON document — the durable
 * wire form + golden sentinel (step-2). Read-only over the IR.
 *
 * <p>Example (declaration document):
 * <pre>{@code
 * { "irFormatVersion": 1, "nodes": [ { "kind": "ENUM", "name": "MyEnum", "values": [ ... ] } ] }
 * }</pre>
 *
 * <p>Two entry hierarchies (declarations via {@code IRKind}, expressions via
 * {@code IRExprKind}) are dispatched by closed-enum {@code switch} over an open node type
 * (no GoF visitor). Instances are immutable and safe to share.
 *
 * <p><b>Adapter model holes (reflected faithfully, not corrected):</b>
 * <ul>
 *   <li><b>JVM-boxed {@code IRLiteral.value}:</b> typed {@code Object}; the serializer
 *       dispatches on {@code literalKind} and casts — a mismatch would throw at serialize
 *       time, not at compile time. The cast is the correct boundary handler until the
 *       adapter types the field.</li>
 *   <li><b>Optionality / {@code isAbstract=false}:</b> optionality is a genuinely-computed neutral
 *       fact ({@code PRESENT} for most nodes; {@code OPTIONAL} for the empty-literal, optional
 *       navigation chains, and to-string over an optional child). Only {@code isAbstract=false} is a
 *       true hardcoded constant (the adapter's current coverage). Both are serialized verbatim. Emitters
 *       consume {@code OPTIONAL} in fail-closed declines — do not mistake those branches for dead code.</li>
 *   <li><b>{@code IREnum} values are local, the parent is a reference:</b> the adapter does not
 *       walk (nor flatten) parent enumerations — {@code values} holds the locally-declared values
 *       only, while the enumeration a declaration {@code extends} is carried as a REFERENCE
 *       ({@code parent} / {@code parentNamespace} / {@code parentResolvedName}). A consumer that
 *       wants the inherited values walks that reference; the IR states the fact.</li>
 *   <li><b>Type constraints deferred:</b> {@code typeObject} renders only {@code name},
 *       optional {@code fractionalDigits}, and {@code meta} — the same subset as
 *       {@link com.regnosys.rosetta.ir.print.RTypeFormatter}. The full numeric/string/alias
 *       constraint set is deferred to a later step where the typed-expression wire form is
 *       finalized and goldened.</li>
 * </ul>
 */
public final class IRJsonSerializer {

    /** The wire-format version, written on the document root. Consumers may reference this sentinel. */
    public static final int IR_FORMAT_VERSION = 1;

    // ── the D55 declaration facts: OPTIONAL members, appended after the v1 ones ───────────────────
    //
    // The declaration-IR enrichment (decision D55) carries a type's namespace, a reference's
    // resolved facts, the documentation, the annotations, the doc references, the condition names,
    // the exact bounds, the type arguments, the labels, the rule references and an enum value's
    // synonyms. Every one of them is written ONLY when it is non-default — an Optional that is
    // present, a list that is non-empty, a boolean that is true — so a node built through an
    // old-arity constructor serializes to EXACTLY the bytes it did before the enrichment and
    // IR_FORMAT_VERSION stays 1. The member order per node kind (the v1 members first, unchanged,
    // then the new ones in this fixed order) is:
    //
    //   STRUCT / CHOICE  kind, name, isAbstract, baseType, fields,
    //                    baseTypeKind, baseTypeNamespace, baseTypeResolvedName, baseTypeEffectiveBase,
    //                    namespace, resolvedName, definition, docReferences, annotations,
    //                    conditionNames
    //   FIELD            kind, name, type, cardinality,
    //                    typeKind, typeNamespace, typeResolvedName, typeEffectiveBase,
    //                    bounds, isOverride, typeArguments, definition, docReferences,
    //                    annotations, labels, ruleReferences
    //   ENUM             kind, name, values,
    //                    parent, parentNamespace, parentResolvedName,
    //                    namespace, definition, docReferences, annotations
    //   ENUM_VALUE       kind, name, displayName,
    //                    definition, docReferences, annotations, synonyms
    //   TYPE_ALIAS       kind, name, baseType,
    //                    baseTypeKind, baseTypeNamespace, baseTypeResolvedName, baseTypeEffectiveBase,
    //                    baseTypeArguments, typeParameters,
    //                    namespace, definition, docReferences, annotations, conditionNames
    //
    // A type REFERENCE (a field's type, a data type's base, a type alias's body, a type parameter's
    // declared type) keeps its v1 string member — the name the model WROTE — and gains sibling
    // members for the declaration it resolves to: the reference's true kind plus that declaration's
    // namespace and own simple name, written only when the reference is resolved at all. The enum
    // parent carries no kind member: IREnum#parent is contractually an IRKind.ENUM reference, so the
    // kind is not a fact the wire form can vary.
    //
    // ── the type gate (v3.3 seat 7, PR #643): three more OPTIONAL members, same law ───────────────
    //
    // A TYPE_ALIAS reference that the adapter COLLAPSED carries its chain's leaf beside the resolved
    // facts, as `<prefix>EffectiveBase` — an object {kind, name, namespace?, arguments?}. A
    // TYPE_ALIAS DECLARATION is a node of its own (`typeAliasJson`), carrying the arguments its body
    // wrote (`baseTypeArguments`) and its declared parameters (`typeParameters`); each parameter
    // carries its declared type as a reference under the `type` prefix. All three are written only
    // when non-default, so IR_FORMAT_VERSION stays 1 and a v1 document reads unchanged.
    //
    // ── the property gate (v3.3 seat 8, PR #644): two more OPTIONAL members and ONE new node ────
    //
    // A type DECLARATION (data type, choice, alias) gains `conditionKinds`, index-parallel to its
    // `conditionNames`; a type REFERENCE gains `<prefix>AliasChain`, the RUNGS of the chain whose
    // leaf `<prefix>EffectiveBase` already carries, written beside it in the same resolved branch.
    // Both only when non-empty. A new node kind rides too — MODEL (`modelJson`), the facts of one
    // `namespace` declaration:
    //
    //   MODEL            kind, name, namespace,
    //                    definition, version, qualifiableConfigs, qualificationFunctions,
    //                    functionSignatures, withMetaUses
    //
    // A v1 document carries no MODEL node and neither of the two members, so its bytes stand and
    // IR_FORMAT_VERSION stays 1.

    private final boolean debugIds;

    /** Creates a serializer with debug node-ids off (the default for goldens). */
    public IRJsonSerializer() { this(false); }

    /**
     * Creates a serializer with node-id emission controlled by {@code debugIds}.
     *
     * @param debugIds when {@code true}, expression nodes also serialize their {@code nodeId}
     *                 (useful for correlating serialized output with in-memory nodes during
     *                 debugging; keep {@code false} for stable goldens)
     */
    public IRJsonSerializer(boolean debugIds) { this.debugIds = debugIds; }

    /**
     * Serializes a whole model's top-level declaration nodes to the canonical versioned
     * document: {@code {"irFormatVersion":N,"nodes":[...]}}.
     */
    public String toJson(List<? extends IRNode> nodes) {
        Objects.requireNonNull(nodes, "nodes");
        Json doc = new Json.Obj(List.of(
                new Json.Member("irFormatVersion", Json.number(Integer.toString(IR_FORMAT_VERSION))),
                new Json.Member("nodes", arr(nodes, this::nodeJson))));
        return JsonWriter.write(doc);
    }

    /**
     * Serializes a single declaration node as a bare JSON fragment (no version wrapper).
     *
     * <p>Example: {@code toJson(enumNode)} →
     * {@code "{\n  \"kind\": \"ENUM\",\n  \"name\": \"MyEnum\",\n  \"values\": []\n}\n"}.
     * Use {@link #toJson(List)} for the full versioned document.
     *
     * @param node the declaration node to serialize; must not be {@code null}
     * @return a non-null JSON string ending in a single newline
     */
    public String toJson(IRNode node) {
        Objects.requireNonNull(node, "node");
        return JsonWriter.write(nodeJson(node));
    }

    private Json nodeJson(IRNode node) {
        return switch (node.kind()) {
            case STRUCT, CHOICE -> typeJson((IRType) node);
            // IRKind.TYPE_ALIAS is produced both by IRTypeNode alias DECLARATIONS (since the type
            // gate) and by IRNodeImpl for a kind the adapter does not model, so guard the cast the
            // same way the ENUM arm does rather than risk a ClassCastException.
            case TYPE_ALIAS -> node instanceof IRType t ? typeAliasJson(t) : genericJson(node);
            // IRKind.ENUM is produced both by IREnumNode declarations and by IRTypeNode type
            // references (a field/base whose target is an enum); only the former is an IREnum, so
            // guard the cast and fall back to genericJson rather than risk a ClassCastException.
            case ENUM -> node instanceof IREnum en ? enumJson(en) : genericJson(node);
            case FIELD -> fieldJson((IRField) node);
            case ENUM_VALUE -> enumValueJson((IREnumValue) node);
            // IRKind.MODEL is produced by IRModelNode (the property gate, PR #644); guard the cast
            // the same way the ENUM and TYPE_ALIAS arms do rather than risk a ClassCastException on
            // a MODEL-kinded node the adapter did not build.
            case MODEL -> node instanceof IRModel model ? modelJson(model) : genericJson(node);
            default -> genericJson(node);
        };
    }

    private Json typeJson(IRType node) {
        List<Json.Member> m = new ArrayList<>();
        m.add(member("kind", node.kind().name()));
        m.add(member("name", node.name()));
        m.add(new Json.Member("isAbstract", Json.bool(node.isAbstract())));
        m.add(optionalMember("baseType", node.baseType().map(IRType::name)));
        m.add(new Json.Member("fields", arr(node.fields(), this::fieldJson)));
        node.baseType().ifPresent(base -> addReferenceFacts(m, "baseType", base));
        addOptional(m, "namespace", node.namespace());
        addOptional(m, "resolvedName", node.resolvedName());
        addOptional(m, "definition", node.definition());
        addList(m, "docReferences", node.docReferences(), IRJsonSerializer::docReferenceJson);
        addList(m, "annotations", node.annotations(), IRJsonSerializer::annotationJson);
        // an unnamed condition is JSON null — the absence of a name IS the fact
        addList(m, "conditionNames", node.conditionNames(),
                name -> name.<Json>map(Json::str).orElse(Json.NULL));
        // the property gate (PR #644): the root kind of each condition, index-parallel to the names
        addList(m, "conditionKinds", node.conditionKinds(), Json::str);
        return new Json.Obj(m);
    }

    /**
     * A {@code typeAlias} DECLARATION (the type gate, v3.3 seat 7 / PR #643): its BODY as a type
     * reference ({@code baseType} plus that reference's facts, the collapsed
     * {@code baseTypeEffectiveBase} included when the body itself names another alias), the
     * arguments the body wrote AS WRITTEN ({@code baseTypeArguments} — a literal, or one of the
     * alias's own parameters passed through by name), its declared {@code typeParameters}, then the
     * declaration facts {@code typeJson} writes in the same order.
     *
     * <p>It carries neither {@code fields} nor {@code isAbstract} (an alias declares neither) and no
     * {@code resolvedName} (a declaration carries none — its own qualified name IS {@code name}).
     */
    private static Json typeAliasJson(IRType node) {
        List<Json.Member> m = new ArrayList<>();
        m.add(member("kind", node.kind().name()));
        m.add(member("name", node.name()));
        m.add(optionalMember("baseType", node.baseType().map(IRType::name)));
        node.baseType().ifPresent(base -> addReferenceFacts(m, "baseType", base));
        addList(m, "baseTypeArguments", node.baseTypeArguments(), IRJsonSerializer::typeArgumentJson);
        addList(m, "typeParameters", node.typeParameters(), IRJsonSerializer::typeParameterJson);
        addOptional(m, "namespace", node.namespace());
        addOptional(m, "definition", node.definition());
        addList(m, "docReferences", node.docReferences(), IRJsonSerializer::docReferenceJson);
        addList(m, "annotations", node.annotations(), IRJsonSerializer::annotationJson);
        // an unnamed condition is JSON null — the absence of a name IS the fact
        addList(m, "conditionNames", node.conditionNames(),
                name -> name.<Json>map(Json::str).orElse(Json.NULL));
        // the property gate (PR #644): the root kind of each condition, index-parallel to the names
        addList(m, "conditionKinds", node.conditionKinds(), Json::str);
        return new Json.Obj(m);
    }

    private Json fieldJson(IRField node) {
        List<Json.Member> m = new ArrayList<>();
        m.add(member("kind", node.kind().name()));
        m.add(member("name", node.name()));
        m.add(member("type", node.type().name()));
        m.add(member("cardinality", node.cardinality().name()));
        addReferenceFacts(m, "type", node.type());
        node.bounds().ifPresent(bounds -> m.add(new Json.Member("bounds", boundsJson(bounds))));
        addFlag(m, "isOverride", node.isOverride());
        addList(m, "typeArguments", node.typeArguments(), IRJsonSerializer::typeArgumentJson);
        addOptional(m, "definition", node.definition());
        addList(m, "docReferences", node.docReferences(), IRJsonSerializer::docReferenceJson);
        addList(m, "annotations", node.annotations(), IRJsonSerializer::annotationJson);
        addList(m, "labels", node.labels(), IRJsonSerializer::labelJson);
        addList(m, "ruleReferences", node.ruleReferences(), IRJsonSerializer::ruleReferenceJson);
        return new Json.Obj(m);
    }

    private Json enumJson(IREnum node) {
        List<Json.Member> m = new ArrayList<>();
        m.add(member("kind", node.kind().name()));
        m.add(member("name", node.name()));
        m.add(new Json.Member("values", arr(node.values(), this::enumValueJson)));
        node.parent().ifPresent(parent -> {
            m.add(member("parent", parent.name()));
            addOptional(m, "parentNamespace", parent.namespace());
            addOptional(m, "parentResolvedName", parent.resolvedName());
        });
        addOptional(m, "namespace", node.namespace());
        addOptional(m, "definition", node.definition());
        addList(m, "docReferences", node.docReferences(), IRJsonSerializer::docReferenceJson);
        addList(m, "annotations", node.annotations(), IRJsonSerializer::annotationJson);
        return new Json.Obj(m);
    }

    private Json enumValueJson(IREnumValue node) {
        List<Json.Member> m = new ArrayList<>();
        m.add(member("kind", node.kind().name()));
        m.add(member("name", node.name()));
        m.add(optionalMember("displayName", node.displayName()));
        addOptional(m, "definition", node.definition());
        addList(m, "docReferences", node.docReferences(), IRJsonSerializer::docReferenceJson);
        addList(m, "annotations", node.annotations(), IRJsonSerializer::annotationJson);
        addList(m, "synonyms", node.synonyms(), IRJsonSerializer::enumSynonymJson);
        return new Json.Obj(m);
    }

    // -------------------------------------------------------------------------
    // The property gate (v3.3 seat 8, PR #644) — the MODEL-level node
    // -------------------------------------------------------------------------

    /**
     * THE MODEL NODE ({@link com.regnosys.rosetta.ir.core.IRKind#MODEL}, PR #644): the facts of ONE
     * {@code namespace} declaration that no type, enum or alias node carries and that the data-type
     * emitter's DERIVED files read. {@code name} and {@code namespace} are the same string — the
     * record's own law, stated on the wire so a non-JVM consumer never has to know it; every other
     * member is written ONLY when it is non-default, so a model with nothing to say is three members
     * long and no v1 document (which carries no MODEL node at all) moves a byte.
     *
     * <p>A function signature's inputs and its output are FIELD nodes written by {@code fieldJson}
     * itself — the wrapper collector reads their {@code [metadata …]} annotations, so they must
     * carry the same shape a declared attribute does. Every type this node references (a qualifiable
     * root, a qualification function's first input, a with-meta argument) rides the same
     * {@code <prefix>Kind} / {@code <prefix>Namespace} / {@code <prefix>ResolvedName} /
     * {@code <prefix>EffectiveBase} / {@code <prefix>AliasChain} law {@code addReferenceFacts}
     * states for every other reference.
     */
    private Json modelJson(IRModel node) {
        List<Json.Member> m = new ArrayList<>();
        m.add(member("kind", node.kind().name()));
        m.add(member("name", node.name()));
        m.add(member("namespace", node.namespace()));
        addOptional(m, "definition", node.definition());
        addOptional(m, "version", node.version());
        addList(m, "qualifiableConfigs", node.qualifiableConfigs(),
                IRJsonSerializer::qualifiableConfigJson);
        addList(m, "qualificationFunctions", node.qualificationFunctions(),
                IRJsonSerializer::qualificationFunctionJson);
        addList(m, "functionSignatures", node.functionSignatures(), this::functionSignatureJson);
        addList(m, "withMetaUses", node.withMetaUses(), IRJsonSerializer::withMetaUseJson);
        return new Json.Obj(m);
    }

    /**
     * One {@code isEvent root X} / {@code isProduct root X} declaration: the kind token the grammar
     * wrote and the root as a type REFERENCE under the {@code rootType} prefix.
     */
    private static Json qualifiableConfigJson(IRQualifiableConfig config) {
        List<Json.Member> m = new ArrayList<>();
        m.add(member("kind", config.kind()));
        m.add(member("rootType", config.rootType().name()));
        addReferenceFacts(m, "rootType", config.rootType());
        return new Json.Obj(m);
    }

    /**
     * One {@code [qualification]} function with at least one input: its name and its FIRST input's
     * declared type as a REFERENCE under the {@code firstInputType} prefix.
     */
    private static Json qualificationFunctionJson(IRQualificationFunction function) {
        List<Json.Member> m = new ArrayList<>();
        m.add(member("name", function.name()));
        m.add(member("firstInputType", function.firstInputType().name()));
        addReferenceFacts(m, "firstInputType", function.firstInputType());
        return new Json.Obj(m);
    }

    /**
     * One function's SIGNATURE: its declared inputs and its output as FIELD nodes. A function with no
     * inputs writes no {@code inputs} member and one with no output writes no {@code output} member —
     * the same non-default rule every other optional member obeys.
     */
    private Json functionSignatureJson(IRFunctionSignature signature) {
        List<Json.Member> m = new ArrayList<>();
        m.add(member("name", signature.name()));
        addList(m, "inputs", signature.inputs(), this::fieldJson);
        signature.output().ifPresent(output -> m.add(new Json.Member("output", fieldJson(output))));
        return new Json.Obj(m);
    }

    /**
     * One {@code with-meta} expression: the entry names as written, and EXACTLY ONE of the argument's
     * inferred type (as a reference, with the LITERAL constraints in force beside it under
     * {@code typeArguments}) or the {@code refusal} token naming why the workspace carried no type.
     * The record itself refuses both and refuses neither, so the wire form never writes either shape.
     */
    private static Json withMetaUseJson(IRWithMetaUse use) {
        List<Json.Member> m = new ArrayList<>();
        addList(m, "entryNames", use.entryNames(), Json::str);
        use.argumentType().ifPresent(type -> {
            m.add(member("argumentType", type.name()));
            addReferenceFacts(m, "argumentType", type);
        });
        addList(m, "typeArguments", use.typeArguments(), IRJsonSerializer::typeArgumentJson);
        addOptional(m, "refusal", use.refusal());
        return new Json.Obj(m);
    }

    // -------------------------------------------------------------------------
    // The D55 declaration facts — nested shapes
    // -------------------------------------------------------------------------

    /**
     * The resolved facts of a type REFERENCE, under {@code <prefix>Kind} / {@code <prefix>Namespace}
     * / {@code <prefix>ResolvedName}. Written only when the reference resolved at all (it carries a
     * namespace or a resolved name); an unresolved reference is carried by its written name alone —
     * the absence is the fact.
     *
     * <p>This method IS the law: {@code <prefix>Kind} is emitted EXACTLY when one of the two resolved
     * strings is. {@link IRJsonDeserializer} refuses either half alone and {@link IRJsonSchema} states
     * the same dependency, so every document the reader admits re-serializes to itself here.
     *
     * <p>Since the type gate (PR #643) a fourth member rides here: {@code <prefix>EffectiveBase}, the
     * collapsed alias chain of a TYPE_ALIAS reference, written after {@code <prefix>ResolvedName} and
     * only when the reference carries one. It is a fact ABOUT a resolved alias, so it lives INSIDE the
     * resolved branch: collapsing a chain means having resolved the alias declaration, so the adapter
     * never produces an effective base on an unresolved reference, and neither the reader nor the
     * schema admits one ({@code <prefix>EffectiveBase} requires {@code <prefix>Kind}, which in turn
     * requires a resolved string and must read {@code TYPE_ALIAS}).
     */
    private static void addReferenceFacts(List<Json.Member> m, String prefix, IRType reference) {
        if (reference.namespace().isEmpty() && reference.resolvedName().isEmpty()) {
            return;
        }
        m.add(member(prefix + "Kind", reference.kind().name()));
        addOptional(m, prefix + "Namespace", reference.namespace());
        addOptional(m, prefix + "ResolvedName", reference.resolvedName());
        reference.effectiveBase().ifPresent(base ->
                m.add(new Json.Member(prefix + "EffectiveBase", effectiveBaseJson(base))));
        // the property gate (PR #644): the RUNGS of the same chain, outermost-first — a fact about a
        // resolved TYPE_ALIAS reference exactly as the collapsed leaf is, so it rides the same branch
        // and is written only when the adapter walked one.
        addList(m, prefix + "AliasChain", reference.aliasChain(), IRJsonSerializer::aliasLinkJson);
    }

    /**
     * The leaf a TYPE_ALIAS chain collapses to: {@code {"kind": …, "name": …, "namespace": …,
     * "arguments": […]}}. The namespace is written only for a declared leaf (a builtin has none) and
     * the arguments only when the substitution left any; they are LITERAL only, the record's own law.
     */
    private static Json effectiveBaseJson(IREffectiveBase base) {
        List<Json.Member> m = new ArrayList<>();
        m.add(member("kind", base.kind().name()));
        m.add(member("name", base.name()));
        addOptional(m, "namespace", base.namespace());
        addList(m, "arguments", base.arguments(), IRJsonSerializer::typeArgumentJson);
        return new Json.Obj(m);
    }

    /**
     * One RUNG of a {@code typeAlias} CHAIN (the property gate, PR #644): the alias's own simple name
     * and declaring namespace, its declared parameters, its conditions in declaration order (an
     * unnamed one is the array's own JSON {@code null} — the SAME encoding a declaration's
     * {@code conditionNames} uses) and, index-parallel to them, each condition's root kind. Every
     * member but {@code name} is written only when it is non-default.
     */
    private static Json aliasLinkJson(IRAliasLink link) {
        List<Json.Member> m = new ArrayList<>();
        m.add(member("name", link.name()));
        addOptional(m, "namespace", link.namespace());
        addList(m, "parameterNames", link.parameterNames(), Json::str);
        // an unnamed condition is JSON null — the absence of a name IS the fact
        addList(m, "conditionNames", link.conditionNames(),
                name -> name.<Json>map(Json::str).orElse(Json.NULL));
        addList(m, "conditionKinds", link.conditionKinds(), Json::str);
        return new Json.Obj(m);
    }

    /**
     * One declared parameter of a {@code typeAlias}: its name, the written name of its declared type,
     * that type's reference facts under the {@code type} prefix, the arguments its own type call wrote
     * (PR #644 - written only when there are any, so a v1 document is unchanged), and the documentation
     * when the model wrote one.
     */
    private static Json typeParameterJson(IRTypeParameter parameter) {
        List<Json.Member> m = new ArrayList<>();
        m.add(member("name", parameter.name()));
        m.add(member("type", parameter.type().name()));
        addReferenceFacts(m, "type", parameter.type());
        addList(m, "typeArguments", parameter.typeArguments(), IRJsonSerializer::typeArgumentJson);
        addOptional(m, "definition", parameter.definition());
        return new Json.Obj(m);
    }

    /** {@code {"lower": n, "upper": n}}; {@code upper} is omitted for the unbounded {@code (n..*)} form. */
    private static Json boundsJson(IRBounds bounds) {
        List<Json.Member> m = new ArrayList<>();
        m.add(new Json.Member("lower", Json.number(bounds.lower().toString())));
        bounds.upper().ifPresent(upper -> m.add(new Json.Member("upper", Json.number(upper.toString()))));
        return new Json.Obj(m);
    }

    private static Json typeArgumentJson(IRTypeArgument argument) {
        List<Json.Member> m = new ArrayList<>();
        m.add(member("parameter", argument.parameter()));
        addOptional(m, "nameValue", argument.nameValue());
        addOptional(m, "literalValue", argument.literalValue());
        addFlag(m, "negated", argument.negated());
        return new Json.Obj(m);
    }

    /** The attribute path an annotation is scoped to: the {@code item} keyword or a root, plus its steps. */
    private static Json annotationPathJson(IRAnnotationPath path) {
        List<Json.Member> m = new ArrayList<>();
        addFlag(m, "rootItem", path.rootItem());
        if (!path.root().isEmpty()) {
            m.add(member("root", path.root()));
        }
        addList(m, "steps", path.steps(), IRJsonSerializer::pathStepJson);
        return new Json.Obj(m);
    }

    private static Json pathStepJson(IRAnnotationPath.Step step) {
        List<Json.Member> m = new ArrayList<>();
        m.add(member("name", step.name()));
        addFlag(m, "deep", step.deep());
        return new Json.Obj(m);
    }

    private static Json annotationJson(IRAnnotationUse use) {
        List<Json.Member> m = new ArrayList<>();
        m.add(member("name", use.name()));
        addOptional(m, "qualifier", use.qualifier());
        addList(m, "arguments", use.arguments(), IRJsonSerializer::annotationArgumentJson);
        return new Json.Obj(m);
    }

    private static Json annotationArgumentJson(IRAnnotationUse.Argument argument) {
        List<Json.Member> m = new ArrayList<>();
        m.add(member("key", argument.key()));
        m.add(member("value", argument.value()));
        addFlag(m, "attributeRef", argument.attributeRef());
        return new Json.Obj(m);
    }

    private static Json docReferenceJson(IRDocReference reference) {
        List<Json.Member> m = new ArrayList<>();
        addFlag(m, "regulatory", reference.regulatory());
        reference.path().ifPresent(path -> m.add(new Json.Member("path", annotationPathJson(path))));
        addOptional(m, "body", reference.body());
        addList(m, "corpora", reference.corpora(), IRJsonSerializer::corpusJson);
        addList(m, "segments", reference.segments(),
                segment -> new Json.Obj(List.of(member("name", segment.name()),
                        member("value", segment.value()))));
        addList(m, "rationales", reference.rationales(), IRJsonSerializer::rationaleJson);
        addOptional(m, "structuredProvision", reference.structuredProvision());
        addOptional(m, "provision", reference.provision());
        addFlag(m, "reportedField", reference.reportedField());
        addList(m, "namedArgs", reference.namedArgs(),
                arg -> new Json.Obj(List.of(member("name", arg.name()), member("value", arg.value()))));
        return new Json.Obj(m);
    }

    /** A corpus twice over: the text the model wrote, and — when resolved — the declaration's own facts. */
    private static Json corpusJson(IRDocReference.Corpus corpus) {
        List<Json.Member> m = new ArrayList<>();
        m.add(member("reference", corpus.reference()));
        corpus.resolved().ifPresent(declaration ->
                m.add(new Json.Member("resolved", corpusDeclarationJson(declaration))));
        return new Json.Obj(m);
    }

    private static Json corpusDeclarationJson(IRDocReference.Corpus.Declaration declaration) {
        List<Json.Member> m = new ArrayList<>();
        addOptional(m, "typeKeyword", declaration.typeKeyword());
        m.add(member("name", declaration.name()));
        addOptional(m, "displayName", declaration.displayName());
        addOptional(m, "definition", declaration.definition());
        return new Json.Obj(m);
    }

    private static Json rationaleJson(IRDocReference.Rationale rationale) {
        List<Json.Member> m = new ArrayList<>();
        addOptional(m, "text", rationale.text());
        addOptional(m, "author", rationale.author());
        return new Json.Obj(m);
    }

    private static Json labelJson(IRLabel label) {
        List<Json.Member> m = new ArrayList<>();
        m.add(member("label", label.label()));
        label.forPath().ifPresent(path -> m.add(new Json.Member("forPath", annotationPathJson(path))));
        label.asPath().ifPresent(path -> m.add(new Json.Member("asPath", annotationPathJson(path))));
        return new Json.Obj(m);
    }

    /** An absent {@code ruleName} is the {@code empty} keyword (the reference removes an inherited rule). */
    private static Json ruleReferenceJson(IRRuleReference reference) {
        List<Json.Member> m = new ArrayList<>();
        reference.forPath().ifPresent(path -> m.add(new Json.Member("forPath", annotationPathJson(path))));
        addOptional(m, "ruleName", reference.ruleName());
        addOptional(m, "resolvedNamespace", reference.resolvedNamespace());
        addOptional(m, "resolvedName", reference.resolvedName());
        return new Json.Obj(m);
    }

    private static Json enumSynonymJson(IREnumSynonym synonym) {
        List<Json.Member> m = new ArrayList<>();
        addList(m, "sources", synonym.sources(), Json::str);
        m.add(member("value", synonym.value()));
        addOptional(m, "definition", synonym.definition());
        addOptional(m, "patternMatch", synonym.patternMatch());
        addOptional(m, "patternReplace", synonym.patternReplace());
        addFlag(m, "removeHtml", synonym.removeHtml());
        return new Json.Obj(m);
    }

    // Non-declining fallback for kinds the adapter does not model (added in Task 6's default policy review).
    // Intentionally a lossy placeholder, distinct from IRPrinter.printGeneric (which reflectively dumps every
    // non-allowlisted component): this emits only kind + name + the impl class name. Unreached by the real
    // adapter (it produces only the modeled kinds); FUNCTION/RULE etc. get dedicated arms when they land.
    // Asymmetric-defensiveness edge: an ENUM-kinded node that is NOT an IREnum routes here (see nodeJson),
    // producing {kind:"ENUM", name, unmodeled} — which the strict IRJsonDeserializer would REJECT (kind ENUM
    // requires a "values" array and forbids the unknown "unmodeled" member). This is corpus-absent (IREnum has
    // a dedicated arm), so it is documented rather than special-cased.
    private Json genericJson(IRNode node) {
        return new Json.Obj(List.of(
                member("kind", node.kind().name()),
                member("name", node.name()),
                member("unmodeled", node.getClass().getSimpleName())));
    }

    // -------------------------------------------------------------------------
    // Expression path
    // -------------------------------------------------------------------------

    /**
     * Serializes an expression IR tree to a bare JSON fragment (a debug-parity aid; the
     * expression form is not goldened). Recursively serializes named structural children
     * via {@code exprJson} (not via {@link IRExpr#children()} directly — each case names its
     * own fields); assumes bounded tree depth (a very deep tree could overflow — acceptable
     * for a debug aid).
     */
    public String toJson(IRExpr expr) {
        Objects.requireNonNull(expr, "expr");
        return JsonWriter.write(exprJson(expr));
    }

    private Json exprJson(IRExpr e) {
        List<Json.Member> m = new ArrayList<>();
        m.add(member("kind", e.kind().name()));
        switch (e.kind()) {
            case LITERAL -> {
                IRLiteral l = (IRLiteral) e;
                m.add(member("literalKind", l.literalKind().name()));
                m.add(new Json.Member("value", literalValueJson(l)));
            }
            case EMPTY_LITERAL ->
                m.add(member("source", ((IREmptyLiteral) e).source().name()));
            case VARIABLE -> {
                IRVariable v = (IRVariable) e;
                m.add(member("name", v.name()));
                m.add(member("variableKind", v.variableKind().name()));
            }
            case REFERENCE -> {
                IRReference r = (IRReference) e;
                m.add(member("target", r.target()));
                m.add(member("referenceKind", r.referenceKind().name()));
            }
            case APPLY -> {
                IRApply ap = (IRApply) e;
                m.add(new Json.Member("callee", exprJson(ap.callee())));
                m.add(new Json.Member("args", arr(ap.args(), this::exprJson)));
            }
            case BINARY_OP -> {
                BinaryOp b = (BinaryOp) e;
                m.add(member("op", b.op().name()));
                m.add(new Json.Member("left", exprJson(b.left())));
                m.add(new Json.Member("right", exprJson(b.right())));
            }
            case EXISTENCE -> {
                Existence x = (Existence) e;
                m.add(member("op", x.op().name()));
                m.add(new Json.Member("modifier",
                        x.modifier() == null ? Json.NULL : Json.str(x.modifier().name())));
                m.add(new Json.Member("arg", exprJson(x.arg())));
            }
            case FIELD_ACCESS -> {
                FieldAccess fa = (FieldAccess) e;
                m.add(new Json.Member("receiver", exprJson(fa.receiver())));
                m.add(member("feature", fa.feature()));
                m.add(member("featureCardinality", fa.featureCardinality().name()));
            }
            case META_ACCESS -> {
                IRMetaAccess ma = (IRMetaAccess) e;
                m.add(new Json.Member("receiver", exprJson(ma.receiver())));
                m.add(member("feature", ma.feature()));
                m.add(new Json.Member("metaQualifiers", arr(ma.metaQualifiers(), Json.Str::new)));
                m.add(member("featureCardinality", ma.featureCardinality().name()));
            }
            case LIST_OP -> {
                IRListOp lo = (IRListOp) e;
                m.add(member("op", lo.op().name()));
                m.add(new Json.Member("child", exprJson(lo.child())));
            }
            case LIST_CONSTRUCT -> {
                IRListConstruct lc = (IRListConstruct) e;
                m.add(new Json.Member("elements", arr(lc.elements(), this::exprJson)));
            }
            case CONDITIONAL -> {
                IRConditional c = (IRConditional) e;
                m.add(new Json.Member("condition", exprJson(c.condition())));
                m.add(new Json.Member("then", exprJson(c.thenBranch())));
                m.add(new Json.Member("else", c.elseBranch() == null ? Json.NULL : exprJson(c.elseBranch())));
            }
            case LET -> {
                Let lt = (Let) e;
                m.add(member("binder", lt.binder()));
                m.add(new Json.Member("value", exprJson(lt.value())));
                m.add(new Json.Member("in", exprJson(lt.in())));
            }
            case TO_STRING ->
                m.add(new Json.Member("child", exprJson(((IRToString) e).child())));
            case POINT_FREE_APPLY ->
                m.add(member("callee", ((IRPointFreeApply) e).callee()));
            case CONSTRUCT -> {
                IRConstruct k = (IRConstruct) e;
                m.add(member("typeName", k.typeName()));
                m.add(new Json.Member("attributeNames", arr(k.attributeNames(), Json.Str::new)));
                m.add(new Json.Member("spread", Json.bool(k.spread())));
            }
            case LAMBDA_OP -> {
                IRLambdaOp lam = (IRLambdaOp) e;
                m.add(member("op", lam.op().name()));
                m.add(new Json.Member("binderName",
                        lam.binderName() == null ? Json.NULL : new Json.Str(lam.binderName())));
                m.add(new Json.Member("receiver", exprJson(lam.receiver())));
                m.add(new Json.Member("body", exprJson(lam.body())));
            }
            case CONVERSION -> {
                IRConversion conv = (IRConversion) e;
                m.add(member("conversionKind", conv.conversionKind()));
                m.add(new Json.Member("targetTypeName",
                        conv.targetTypeName() == null ? Json.NULL : new Json.Str(conv.targetTypeName())));
                m.add(new Json.Member("child", exprJson(conv.child())));
            }
            case PIPE -> m.add(new Json.Member("spineLength",
                    Json.number(Integer.toString(((IRPipe) e).spineLength()))));
            case ONLY_EXISTS -> m.add(new Json.Member("pathCount",
                    Json.number(Integer.toString(((IROnlyExists) e).pathCount()))));
            case SYMBOL_NAV -> m.add(member("symbolKind", ((IRSymbolNav) e).symbolKind()));
            case CLOSURE_PARAM -> m.add(member("paramName",
                    ((IRClosureParam) e).paramName()));
            case META_OUTPUT_APPLY -> m.add(member("calleeName",
                    ((IRMetaOutputApply) e).calleeName()));
            case ALL_ANY_COMPARE -> {
                IRAllAnyCompare aac = (IRAllAnyCompare) e;
                m.add(member("op", aac.op()));
                m.add(member("modifier", aac.modifier()));
            }
            case SYN_ITEM_NAV -> m.add(member("featureName",
                    ((IRSynItemNav) e).featureName()));
            case META_ITEM_NAV -> m.add(member("featureName",
                    ((com.regnosys.rosetta.ir.expr.IRMetaItemNav) e).featureName()));
            case CHOICE_OPTION_NAV -> {
                IRChoiceOptionNav con = (IRChoiceOptionNav) e;
                m.add(member("headName", con.headName()));
                m.add(member("optionName", con.optionName()));
            }
            case DISPATCH_INPUT_REF -> m.add(member("inputName",
                    ((IRDispatchInputRef) e).inputName()));
            case DEEP_FEATURE_NAV -> {
                IRDeepFeatureNav deep = (IRDeepFeatureNav) e;
                m.add(new Json.Member("receiver", exprJson(deep.receiver())));
                m.add(member("feature", deep.featureName()));
            }
            case RECORD_FEATURE_NAV -> {
                IRRecordFeatureNav rec = (IRRecordFeatureNav) e;
                m.add(member("headName", rec.headName()));
                m.add(member("feature", rec.featureName()));
                m.add(member("recordTypeName", rec.recordTypeName()));
            }
            case RECORD_RECEIVER_NAV -> {
                com.regnosys.rosetta.ir.expr.IRRecordReceiverNav recRecv =
                        (com.regnosys.rosetta.ir.expr.IRRecordReceiverNav) e;
                m.add(new Json.Member("receiver", exprJson(recRecv.receiver())));
                m.add(member("feature", recRecv.featureName()));
                m.add(member("recordTypeName", recRecv.recordTypeName()));
            }
            case QUALIFIER_ITEM_NAV -> m.add(member("qualifierName",
                    ((com.regnosys.rosetta.ir.expr.IRQualifierItemNav) e).qualifierName()));
            case CHOICE_RECEIVER_NAV -> {
                com.regnosys.rosetta.ir.expr.IRChoiceReceiverNav choiceRecv =
                        (com.regnosys.rosetta.ir.expr.IRChoiceReceiverNav) e;
                m.add(new Json.Member("receiver", exprJson(choiceRecv.receiver())));
                m.add(member("optionName", choiceRecv.optionName()));
                m.add(member("choiceName", choiceRecv.choiceName()));
            }
            case QUALIFIER_RECEIVER_NAV -> {
                com.regnosys.rosetta.ir.expr.IRQualifierReceiverNav qualRecv =
                        (com.regnosys.rosetta.ir.expr.IRQualifierReceiverNav) e;
                m.add(new Json.Member("receiver", exprJson(qualRecv.receiver())));
                m.add(member("qualifierName", qualRecv.qualifierName()));
            }
            case SWITCH_OP -> {
                com.regnosys.rosetta.ir.expr.IRSwitchOp sw =
                        (com.regnosys.rosetta.ir.expr.IRSwitchOp) e;
                m.add(new Json.Member("caseCount",
                        Json.number(Integer.toString(sw.caseCount()))));
                m.add(new Json.Member("hasDefault", Json.bool(sw.hasDefault())));
            }
            case DEFAULT_OP -> { /* no kind-specific facts — the shared tail carries the rest */ }
            case MEMBERSHIP_OP -> m.add(member("op",
                    ((com.regnosys.rosetta.ir.expr.IRMembershipOp) e).op().name()));
            case COLLECT_OP -> {
                com.regnosys.rosetta.ir.expr.IRCollectOp col =
                        (com.regnosys.rosetta.ir.expr.IRCollectOp) e;
                m.add(member("op", col.op().name()));
                m.add(new Json.Member("hasBody", Json.bool(col.hasBody())));
            }
            case OUTPUT_REF -> m.add(member("outputName",
                    ((com.regnosys.rosetta.ir.expr.IROutputRef) e).outputName()));
            case META_PARAM_REF -> m.add(member("paramName",
                    ((com.regnosys.rosetta.ir.expr.IRMetaParamRef) e).paramName()));
            case RULE_INPUT_NAV -> m.add(member("featureName",
                    ((com.regnosys.rosetta.ir.expr.IRRuleInputNav) e).featureName()));
            case IMPLICIT_ATTR_NAV -> m.add(member("attributeName",
                    ((com.regnosys.rosetta.ir.expr.IRImplicitAttrNav) e).attributeName()));
            case CONDITION_INSTANCE -> m.add(member("typeName",
                    ((com.regnosys.rosetta.ir.expr.IRConditionInstance) e).typeName()));
            case WITH_META_OP -> m.add(new Json.Member("entryCount", Json.number(
                    Integer.toString(((com.regnosys.rosetta.ir.expr.IRWithMetaOp) e).entryCount()))));
            case JOIN_OP -> m.add(new Json.Member("hasSeparator",
                    Json.bool(((com.regnosys.rosetta.ir.expr.IRJoinOp) e).hasSeparator())));
            case OUTPUT_ALIAS_NAV -> {
                com.regnosys.rosetta.ir.expr.IROutputAliasNav oan =
                        (com.regnosys.rosetta.ir.expr.IROutputAliasNav) e;
                m.add(member("headName", oan.headName()));
                m.add(member("feature", oan.featureName()));
            }
            case LIBRARY_APPLY -> m.add(member("calleeName",
                    ((com.regnosys.rosetta.ir.expr.IRLibraryApply) e).calleeName()));
            default -> m.add(member("unmodeled", e.getClass().getSimpleName()));
        }
        appendExprTail(e, m);
        return new Json.Obj(m);
    }

    /** Appends nodeId (debug only), then the shared type/cardinality/optionality facts, in that order. */
    private void appendExprTail(IRExpr e, List<Json.Member> members) {
        if (debugIds) {
            members.add(member("nodeId", e.nodeId().toString()));
        }
        members.add(new Json.Member("type", typeObject(e.type())));
        members.add(member("cardinality", e.cardinality().name()));
        members.add(member("optionality", e.optionality().name()));
    }

    /** Switch-free: RType.name() is polymorphic; only RNumberType adds fractionalDigits. */
    private Json typeObject(RMetaAnnotatedType type) {
        if (type.isMissing()) {
            return new Json.Obj(List.of(new Json.Member("missing", Json.bool(true))));
        }
        List<Json.Member> m = new ArrayList<>();
        m.add(member("name", type.type().name()));
        if (type.type() instanceof RNumberType n && n.fractionalDigits().isPresent()) {
            m.add(new Json.Member("fractionalDigits",
                    Json.number(Integer.toString(n.fractionalDigits().getAsInt()))));
        }
        if (!type.metaAttributes().isEmpty()) {
            List<Json> meta = type.metaAttributes().stream().sorted().map(Json::str).toList();
            m.add(new Json.Member("meta", new Json.Arr(meta)));
        }
        return new Json.Obj(m);
    }

    private Json literalValueJson(IRLiteral l) {
        Object v = l.value();
        return switch (l.literalKind()) {
            case NUMBER -> Json.str(((BigDecimal) v).toPlainString());
            case INT -> Json.str(((BigInteger) v).toString());   // unbounded BigInteger as a JSON string
            case BOOLEAN -> Json.bool((Boolean) v);
            case STRING -> Json.str((String) v);
        };
    }

    /** A string-valued object member — the dominant shape across every builder. */
    private static Json.Member member(String name, String value) {
        return new Json.Member(name, Json.str(value));
    }

    /** A string-valued member that becomes JSON {@code null} when the value is absent. */
    private static Json.Member optionalMember(String name, Optional<String> value) {
        return new Json.Member(name, value.<Json>map(Json::str).orElse(Json.NULL));
    }

    /** Maps each item in {@code items} through {@code f} and wraps the results in a {@link Json.Arr}. */
    private static <T> Json arr(List<? extends T> items, Function<? super T, Json> f) {
        return new Json.Arr(items.stream().map(f).toList());
    }

    /** Adds a string member only when the optional is PRESENT — the D55 optional-member rule. */
    private static void addOptional(List<Json.Member> m, String name, Optional<String> value) {
        value.ifPresent(v -> m.add(member(name, v)));
    }

    /** Adds an array member only when the list is NON-EMPTY — the D55 optional-member rule. */
    private static <T> void addList(List<Json.Member> m, String name, List<? extends T> items,
                                    Function<? super T, Json> f) {
        if (!items.isEmpty()) {
            m.add(new Json.Member(name, arr(items, f)));
        }
    }

    /** Adds a {@code true} member only when the flag is SET — the D55 optional-member rule. */
    private static void addFlag(List<Json.Member> m, String name, boolean flag) {
        if (flag) {
            m.add(new Json.Member(name, Json.bool(true)));
        }
    }
}
