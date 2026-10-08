package com.regnosys.rosetta.ir.adapter;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.RRootElement;
import com.regnosys.rosetta.ast.annotations.RAnnotationPathExpression;
import com.regnosys.rosetta.ast.annotations.RAnnotationPathSegment;
import com.regnosys.rosetta.ast.annotations.RAnnotationQualifier;
import com.regnosys.rosetta.ast.annotations.RAnnotationRef;
import com.regnosys.rosetta.ast.annotations.RLabelAnnotation;
import com.regnosys.rosetta.ast.annotations.RRuleReferenceAnnotation;
import com.regnosys.rosetta.ast.enums.CardCheckOp;
import com.regnosys.rosetta.ast.expressions.supporting.RWithMetaEntry;
import com.regnosys.rosetta.ast.expressions.unary.RCardinalityCheckExpr;
import com.regnosys.rosetta.ast.expressions.unary.RWithMetaExpr;
import com.regnosys.rosetta.ast.functions.RCondition;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.functions.RRule;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.model.RQualifiableConfig;
import com.regnosys.rosetta.ast.regulatory.RCorpus;
import com.regnosys.rosetta.ast.regulatory.RDocReference;
import com.regnosys.rosetta.ast.regulatory.RDocumentRationale;
import com.regnosys.rosetta.ast.regulatory.RRegulatoryDocumentReference;
import com.regnosys.rosetta.ast.regulatory.RRegulatoryReferenceArg;
import com.regnosys.rosetta.ast.regulatory.RSegmentRef;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.supporting.RCardinality;
import com.regnosys.rosetta.ast.supporting.RChoiceOption;
import com.regnosys.rosetta.ast.supporting.REnumValue;
import com.regnosys.rosetta.ast.supporting.RTypeCall;
import com.regnosys.rosetta.ast.supporting.RTypeCallArgument;
import com.regnosys.rosetta.ast.supporting.RTypeCallArgumentExpression;
import com.regnosys.rosetta.ast.supporting.RTypeParameter;
import com.regnosys.rosetta.ast.synonyms.REnumSynonym;
import com.regnosys.rosetta.ast.types.RBasicType;
import com.regnosys.rosetta.ast.types.RChoice;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.ast.types.REnumeration;
import com.regnosys.rosetta.ast.types.RMetaType;
import com.regnosys.rosetta.ast.types.RRecordType;
import com.regnosys.rosetta.ast.types.RTypeAlias;
import com.regnosys.rosetta.ast.util.AstWalker;
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
import com.regnosys.rosetta.symbols.derived.GeneratedInputRule;
import com.regnosys.rosetta.types.RAliasType;
import com.regnosys.rosetta.types.RChoiceTypeRef;
import com.regnosys.rosetta.types.RDataTypeRef;
import com.regnosys.rosetta.types.REnumTypeRef;
import com.regnosys.rosetta.types.RMetaAnnotatedType;
import com.regnosys.rosetta.types.RNumberType;
import com.regnosys.rosetta.types.RStringType;
import com.regnosys.rosetta.types.RType;
import com.regnosys.rosetta.types.builtin.BuiltinTypeRegistry;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * AST → IR adapter for declaration kinds (Phase 1). Maps each
 * {@link RRootElement} to the corresponding immutable IR node:
 * {@link RDataType} → STRUCT {@link IRTypeNode}, {@link RChoice} → CHOICE
 * {@link IRTypeNode}, {@link REnumeration} → {@link IREnumNode}, and — since v3.3 seat 7
 * (PR #643, the type gate) — {@link RTypeAlias} → TYPE_ALIAS {@link IRTypeNode}. The remaining
 * root-element kinds are recognised but not yet adapted in Phase 1
 * ({@link #adaptRootElement} returns empty) — extension points for later phases;
 * the IR interface surface already covers all 22 {@link IRKind}s. Since v3.3 seat 8
 * (PR #644, the property gate) the MODEL itself adapts too, through {@link #adaptModelNode}
 * ({@link IRKind#MODEL}) — the facts of a {@code namespace} declaration that no type node carries.
 *
 * <p><b>The declaration facts (decision D55, the declaration-IR enrichment).</b> Beside the structure the
 * adapter carries, as PROPER NAMED FIELDS, every fact a backend needs to write a type, a choice or an enum
 * file without a second look at the parsed source: a type reference's TRUE kind and the namespace and own
 * name of the declaration it resolves to; the documentation; {@code override}; a data type's base and an
 * enum's parent as resolved references; the annotations; the doc references (their corpora resolved in the
 * referencing file's scope through the {@link CorpusResolver} the host supplies); the condition names;
 * the EXACT bounds; the type arguments; an enum value's synonyms; a field's labels and rule references.
 * Attribute-, class- and enum-level synonyms are NOT carried: no generated file kind of the Java routes
 * reads them (the ingest kinds do), and a half-modelled mapping body would be worse than its absence.
 *
 * <p>The adapter is pure structure and never feeds the byte-emission path
 * directly (Path-2 reuses Path-1's template models for byte-identity — decision
 * L-001). It exists to (a) prove the declaration structure is recoverable as IR
 * and (b) provide a reconciliation oracle (every fact above, asserted per declaration) for the IR-routed
 * emitter. Stateless and side-effect-free; safe to share.
 *
 * <p>Lab-authored Phase-1 IR adapter (decision L-004).
 */
public final class AstToIRAdapter {

    /**
     * Resolves a doc reference's corpus name to its declaration IN THE REFERENCING FILE'S SCOPE. The scoped
     * lookup is the host generator's (one producer — the legacy javadoc renderer reads the same one), so the
     * adapter takes it as a seam rather than owning a second copy.
     */
    @FunctionalInterface
    public interface CorpusResolver {
        /** The corpus {@code corpusName} names from {@code reference}'s file, or empty when unresolved. */
        Optional<RCorpus> resolve(RDocReference reference, String corpusName);

        /** Resolves nothing: every corpus is carried by its written reference alone. */
        CorpusResolver NONE = (reference, corpusName) -> Optional.empty();
    }

    /**
     * Types an EXPRESSION (v3.3 seat 8, PR #644 - the property gate). A {@code with-meta} expression's wrapper is
     * decided by its ARGUMENT's type, and an expression is typed by the workspace's fixed-point engine
     * ({@code RWorkspace#getInferredType}), not by a declaration walk — so the adapter takes the reading as a seam,
     * exactly as it takes the {@link CorpusResolver}: ONE producer, the host's, never a second copy.
     *
     * <p>THE LAW WHEN THE SEAM IS ABSENT: the adapter does NOT guess. An adapter built without an inferrer, and an
     * inferrer that answers nothing, both make every {@code with-meta} use a {@code missing} refusal
     * ({@link IRWithMetaUse#refusal()}) — a stated ignorance, never an inferred type.
     */
    @FunctionalInterface
    public interface TypeInferrer {
        /** The workspace's inferred type of {@code expression}, or null when it types none. */
        RMetaAnnotatedType inferredTypeOf(RExpression expression);

        /** Infers nothing: every {@code with-meta} use is a {@code missing} refusal. */
        TypeInferrer NONE = expression -> null;
    }

    /** The shared builtin registry - the old generator's own first fallback for a name the workspace cannot resolve. */
    private static final BuiltinTypeRegistry BUILTINS = BuiltinTypeRegistry.createDefault();

    private final CorpusResolver corpusResolver;
    private final TypeInferrer typeInferrer;

    /** An adapter that resolves no corpus and types no expression (a detached model, a unit fixture). */
    public AstToIRAdapter() {
        this(CorpusResolver.NONE, TypeInferrer.NONE);
    }

    /** An adapter whose doc references carry their corpora resolved by {@code corpusResolver}. */
    public AstToIRAdapter(CorpusResolver corpusResolver) {
        this(corpusResolver, TypeInferrer.NONE);
    }

    /**
     * An adapter with both host seams: {@code corpusResolver} for a doc reference's corpus, {@code typeInferrer} for
     * a {@code with-meta} argument's type. The host passes {@code workspace::getInferredType}.
     */
    public AstToIRAdapter(CorpusResolver corpusResolver, TypeInferrer typeInferrer) {
        this.corpusResolver = Objects.requireNonNull(corpusResolver, "corpusResolver");
        this.typeInferrer = Objects.requireNonNull(typeInferrer, "typeInferrer");
    }

    /** Adapt every supported root element of a model to its IR node, in source order. */
    public List<IRNode> adaptModel(RModel model) {
        String namespace = model.namespace();
        List<IRNode> nodes = new ArrayList<>();
        for (RRootElement element : model.rootElements()) {
            adaptRootElement(namespace, element).ifPresent(nodes::add);
        }
        return List.copyOf(nodes);
    }

    /**
     * Adapt a single root element. Returns empty for kinds not yet handled in
     * Phase 1 (functions, rules, reports, annotations, synonym/external/
     * regulatory sources, and the built-in types).
     */
    public Optional<IRNode> adaptRootElement(String namespace, RRootElement element) {
        if (element instanceof RDataType dataType) {
            return Optional.of((IRNode) adaptData(namespace, dataType));
        }
        if (element instanceof RChoice choice) {
            return Optional.of((IRNode) adaptChoice(namespace, choice));
        }
        if (element instanceof REnumeration enumeration) {
            return Optional.of((IRNode) adaptEnum(namespace, enumeration));
        }
        if (element instanceof RTypeAlias alias) {
            return Optional.of((IRNode) adaptTypeAlias(namespace, alias));
        }
        return Optional.empty();
    }

    /** Adapt a {@code data} declaration to a STRUCT {@link IRType}. */
    public IRTypeNode adaptData(String namespace, RDataType dataType) {
        List<IRField> fields = new ArrayList<>();
        for (RAttribute attribute : dataType.attributes()) {
            fields.add(adaptAttribute(attribute));
        }
        Optional<IRType> baseType = dataType.superTypeName()
                .map(superName -> superTypeRef(dataType, superName));
        List<Optional<String>> conditionNames = new ArrayList<>();
        for (RCondition condition : dataType.conditions()) {
            conditionNames.add(condition.name());
        }
        return new IRTypeNode(qualifiedName(namespace, dataType.name()), IRKind.STRUCT,
                fields, baseType, false, Optional.empty(), IRMetadata.EMPTY,
                Optional.ofNullable(namespace), Optional.empty(), dataType.definition(),
                docReferences(dataType.docReferences()), annotations(dataType.annotationRefs()), conditionNames,
                Optional.empty(), List.of(), List.of(), conditionKinds(dataType.conditions()), List.of());
    }

    /**
     * Adapt a {@code choice} declaration to a CHOICE {@link IRType}.
     *
     * <p>A {@code choice} declares NO condition — the grammar gives it options and annotations only
     * ({@link RChoice} carries no {@code conditions()} accessor at all) — so both its condition names and, since
     * v3.3 seat 8 (PR #644), its {@link IRType#conditionKinds()} are EMPTY by the shape of the language, not by an
     * omission of this adapter.
     */
    public IRTypeNode adaptChoice(String namespace, RChoice choice) {
        List<IRField> fields = new ArrayList<>();
        for (RChoiceOption option : choice.options()) {
            fields.add(adaptOption(option));
        }
        return new IRTypeNode(qualifiedName(namespace, choice.name()), IRKind.CHOICE,
                fields, Optional.empty(), false, Optional.empty(), IRMetadata.EMPTY,
                Optional.ofNullable(namespace), Optional.empty(), choice.definition(),
                List.of(), annotations(choice.annotationRefs()), List.of());
    }

    /** Adapt an {@code enum} declaration to an {@link IREnumNode}. */
    public IREnumNode adaptEnum(String namespace, REnumeration enumeration) {
        List<IREnumValue> values = new ArrayList<>();
        for (REnumValue value : enumeration.values()) {
            List<IREnumSynonym> synonyms = new ArrayList<>();
            for (REnumSynonym synonym : value.synonyms()) {
                synonyms.add(new IREnumSynonym(synonym.sources(), synonym.value(), synonym.definitionText(),
                        synonym.patternMatch(), synonym.patternReplace(), synonym.isRemoveHtml()));
            }
            values.add(new IREnumValueNode(value.name(), value.displayName(),
                    Optional.empty(), IRMetadata.EMPTY, value.definition(),
                    docReferences(value.docReferences()), annotations(value.annotationRefs()), synonyms));
        }
        Optional<IRType> parent = enumeration.superTypeName()
                .map(parentName -> resolvedRef(parentName, IRKind.ENUM, safe(enumeration::superType)));
        return new IREnumNode(qualifiedName(namespace, enumeration.name()), values,
                Optional.empty(), IRMetadata.EMPTY, Optional.ofNullable(namespace), parent,
                enumeration.definition(), docReferences(enumeration.docReferences()),
                annotations(enumeration.annotationRefs()));
    }

    /**
     * Adapt a {@code typeAlias} declaration to a TYPE_ALIAS {@link IRType} (v3.3 seat 7, PR #643 — the type gate).
     *
     * <p>An alias declares no field. What it carries is its BODY: {@link IRType#baseType()} is the body's type
     * REFERENCE — and, when the body names another alias, that reference carries the chain collapsed with THIS
     * alias's own parameters unbound (a body argument passing a parameter through by name binds nothing) —
     * {@link IRType#baseTypeArguments()} the arguments the body wrote AS WRITTEN (a literal, or a parameter by
     * name), and {@link IRType#typeParameters()} the parameters the alias declares. The declaration node itself
     * states no effective base: the collapse is a property of a USE SITE, stated on the reference.
     */
    public IRTypeNode adaptTypeAlias(String namespace, RTypeAlias alias) {
        RTypeCall body = alias.typeCall();
        Optional<IRType> baseType = body == null ? Optional.empty() : Optional.of(typeRef(body));
        List<IRTypeParameter> parameters = new ArrayList<>();
        for (RTypeParameter parameter : alias.typeParameters()) {
            // the parameter's OWN type call's arguments ride with it (PR #644, the banked cq SF-1 of #643): a
            // `n number(fractionalDigits: 0)` parameter is integer-grained, and dropping them lost that silently
            parameters.add(new IRTypeParameter(parameter.name(), typeRef(parameter.typeCall()),
                    parameter.definition(), typeArguments(parameter.typeCall())));
        }
        List<Optional<String>> conditionNames = new ArrayList<>();
        for (RCondition condition : alias.conditions()) {
            conditionNames.add(condition.name());
        }
        return new IRTypeNode(qualifiedName(namespace, alias.name()), IRKind.TYPE_ALIAS, List.of(),
                baseType, false, Optional.empty(), IRMetadata.EMPTY,
                Optional.ofNullable(namespace), Optional.empty(), alias.definition(),
                List.of(), List.of(), conditionNames,
                Optional.empty(), parameters, typeArguments(body),
                conditionKinds(alias.conditions()), List.of());
    }

    private IRFieldNode adaptAttribute(RAttribute attribute) {
        return new IRFieldNode(attribute.name(), typeRef(attribute.typeCall()),
                cardinality(attribute.cardinality()), Optional.empty(), IRMetadata.EMPTY,
                attribute.cardinality().map(AstToIRAdapter::bounds), attribute.isOverride(),
                typeArguments(attribute.typeCall()), attribute.definition(),
                docReferences(attribute.docReferences()), annotations(attribute.annotationRefs()),
                labels(attribute.labelAnnotations()), ruleReferences(attribute.ruleReferenceAnnotations()));
    }

    private IRFieldNode adaptOption(RChoiceOption option) {
        // A choice option has no name of its own — its typeCall is its identity.
        RTypeCall typeCall = option.typeCall();
        String name = typeCall == null ? "" : typeCall.typeName();
        return new IRFieldNode(name, typeRef(typeCall), Cardinality.ONE_TO_ONE,
                Optional.empty(), IRMetadata.EMPTY,
                Optional.empty(), false, typeArguments(typeCall), option.definition(),
                docReferences(option.docReferences()), annotations(option.annotationRefs()),
                labels(option.labelAnnotations()), ruleReferences(option.ruleReferenceAnnotations()));
    }

    /**
     * Build a lightweight {@link IRType} reference for a field/option type: the name the model wrote, the
     * TRUE kind of the declaration it resolves to (data / choice / enum / basic type / type alias / record
     * type / meta type) and that declaration's namespace and own simple name. An unresolved or detached
     * reference keeps the STRUCT default and carries NO namespace — the absence is the fact.
     *
     * <p>ONE fallback, the old generator's first ({@code GeneratorModel#resolveTypeCall}): a name that does not
     * resolve — a workspace loaded WITHOUT the model library's {@code basictypes} file — is looked up in the
     * shared {@link BuiltinTypeRegistry}. A builtin is then BASIC_TYPE (or RECORD_TYPE), named, in no namespace.
     * No vendored cell takes it (every reference resolves there — the seat's probe: 0 unresolved over 26
     * cells); the reconcile reads the old generator's own verdict beside it either way.
     *
     * <p>THE EFFECTIVE BASE (v3.3 seat 7, PR #643 — the type gate): a reference that resolves to a
     * {@code typeAlias} declaration ALSO carries its chain collapsed AT THIS USE SITE — the declaration the
     * chain ends on and the arguments in force there after substitution ({@link IREffectiveBase}), the call's
     * own literal arguments being the bindings. The alias name never reaches a generated byte; the base does.
     * A chain the walk cannot end (a cycle, a body no declaration and no registry name answers) carries NO
     * effective base — the absence is the fact.
     *
     * <p>THE WALKED CHAIN (v3.3 seat 8, PR #644 — the property gate): the same reference also carries the chain's
     * RUNGS, outermost-first ({@link IRType#aliasChain()}), because two derived files read them where the collapsed
     * base cannot serve — the type-format validator wires every condition of every alias in the chain and REFUSES
     * the whole validator file when a PARAMETERISED alias in it carries one. The two facts are independent: a chain
     * the walk ended without a leaf carries its links and no base.
     */
    private IRType typeRef(RTypeCall typeCall) {
        if (typeCall == null) {
            return new IRTypeNode("", IRKind.STRUCT, List.of(), Optional.empty(),
                    false, Optional.empty(), IRMetadata.EMPTY);
        }
        Optional<RNode> resolved = safe(typeCall::referencedType);
        if (resolved.isEmpty()) {
            Optional<RType> builtin = BUILTINS.lookup(typeCall.typeName());
            if (builtin.isPresent()) {
                IRKind kind = builtin.get() instanceof com.regnosys.rosetta.types.RRecordType
                        ? IRKind.RECORD_TYPE : IRKind.BASIC_TYPE;
                return IRTypeNode.reference(typeCall.typeName(), kind, Optional.empty(), Optional.of(typeCall.typeName()));
            }
        }
        if (resolved.isPresent() && resolved.get() instanceof RTypeAlias alias) {
            return IRTypeNode.reference(typeCall.typeName(), IRKind.TYPE_ALIAS, namespaceOf(alias),
                    declaredName(alias), collapse(alias, bindingsOf(typeCall), 0), aliasChain(alias));
        }
        return resolvedRef(typeCall.typeName(), IRKind.STRUCT, resolved);
    }

    // === THE EFFECTIVE BASE LAW (v3.3 seat 7, PR #643) ==========================================

    /**
     * A literal bound to a type parameter: the value as the model wrote it, WITHOUT the sign, and the sign
     * ({@link IRTypeArgument}'s own shape).
     */
    private record Literal(String value, boolean negated) {
        IRTypeArgument asArgument(String parameter) {
            return new IRTypeArgument(parameter, Optional.empty(), Optional.of(value), negated);
        }
    }

    /** The alias-chain walk's depth bound — the parser's own ({@code TypeAliasSolver.MAX_DEPTH}). */
    private static final int MAX_ALIAS_DEPTH = 100;

    /**
     * The order a BASIC_TYPE / RECORD_TYPE leaf's known parameters are stated in. Both halves of the D11
     * Java-type reconcile walk the chain independently and must agree on the argument list, so the order is
     * CANONICAL rather than the body's; an argument no builtin knows follows, in body order.
     */
    private static final List<String> CANONICAL_PARAMETERS =
            List.of("digits", "fractionalDigits", "min", "max", "minLength", "maxLength", "pattern");

    /** The literal arguments a USE SITE binds; an argument passing a parameter through by name binds nothing. */
    private static Map<String, Literal> bindingsOf(RTypeCall call) {
        Map<String, Literal> bindings = new LinkedHashMap<>();
        if (call == null) {
            return bindings;
        }
        for (RTypeCallArgument argument : call.arguments()) {
            RTypeCallArgumentExpression value = argument.value();
            if (value != null && value.literalValue().isPresent()) {
                bindings.put(argument.parameterName(), new Literal(value.literalValue().get(), value.isNegated()));
            }
        }
        return bindings;
    }

    /**
     * Collapse {@code alias}'s chain under {@code bindings}: the declaration it ends on and the arguments in
     * force there. The law is the parser's own ({@code TypeAliasSolver.evaluateBodyWithBindings}) — implemented
     * here rather than called, so the reconcile's independent walk has a second producer to agree with (LAW 69).
     * A body argument is a literal (it wins), or one of the alias's parameters passed through by name (it reads
     * the binding, or stays ABSENT when unbound). A body naming another alias recurses with the resolved
     * arguments as the new bindings; a body naming a MODEL-declared basic / record type is {@code nothing} (the
     * released plugin's law); a body no declaration answers is looked up by NAME in the shared registry.
     */
    private static Optional<IREffectiveBase> collapse(RTypeAlias alias, Map<String, Literal> bindings, int depth) {
        if (depth >= MAX_ALIAS_DEPTH) {
            return Optional.empty();
        }
        RTypeCall body = alias.typeCall();
        if (body == null) {
            return Optional.empty();
        }
        Map<String, Literal> resolved = new LinkedHashMap<>();
        for (RTypeCallArgument argument : body.arguments()) {
            RTypeCallArgumentExpression value = argument.value();
            if (value == null) {
                continue;
            }
            if (value.literalValue().isPresent()) {
                resolved.put(argument.parameterName(), new Literal(value.literalValue().get(), value.isNegated()));
            } else if (value.nameValue().isPresent()) {
                Literal bound = bindings.get(value.nameValue().get());
                if (bound != null) {
                    resolved.put(argument.parameterName(), bound);
                }
            }
        }
        Optional<RNode> target = safe(body::referencedType);
        if (target.isPresent()) {
            RNode node = target.get();
            if (node instanceof RTypeAlias inner) {
                return collapse(inner, resolved, depth + 1);
            }
            if (node instanceof RDataType dataType) {
                return Optional.of(new IREffectiveBase(IRKind.STRUCT, dataType.name(), namespaceOf(dataType),
                        declaredArguments(resolved)));
            }
            if (node instanceof REnumeration enumeration) {
                return Optional.of(new IREffectiveBase(IRKind.ENUM, enumeration.name(), namespaceOf(enumeration),
                        declaredArguments(resolved)));
            }
            if (node instanceof RChoice choice) {
                return Optional.of(new IREffectiveBase(IRKind.CHOICE, choice.name(), namespaceOf(choice),
                        declaredArguments(resolved)));
            }
            if (node instanceof RBasicType || node instanceof RRecordType) {
                String declared = declaredName(node).orElse("");
                Optional<RType> registryType = BUILTINS.lookup(declared);
                if (registryType.isPresent()) {
                    return Optional.of(registryBase(declared, registryType.get(), resolved));
                }
                // The released 9.83.0 plugin types a MODEL-declared basic / record type as `nothing`.
                return Optional.of(new IREffectiveBase(IRKind.BASIC_TYPE, "nothing", Optional.empty(), List.of()));
            }
            return Optional.empty();
        }
        String bodyName = body.typeName();
        if (bodyName == null) {
            return Optional.empty();
        }
        return BUILTINS.lookup(bodyName).map(registryType -> registryBase(bodyName, registryType, resolved));
    }

    /** A declared leaf (STRUCT / ENUM / CHOICE) states the resolved arguments in the body's own order. */
    private static List<IRTypeArgument> declaredArguments(Map<String, Literal> resolved) {
        List<IRTypeArgument> arguments = new ArrayList<>();
        for (Map.Entry<String, Literal> entry : resolved.entrySet()) {
            arguments.add(entry.getValue().asArgument(entry.getKey()));
        }
        return arguments;
    }

    /**
     * A builtin leaf: the registry type's OWN constraints seed the arguments ({@code int}'s
     * {@code fractionalDigits: 0} survives a body that never restates it — the parser's own merge), the
     * resolved literals overlay them, and the result is stated in {@link #CANONICAL_PARAMETERS} order.
     */
    private static IREffectiveBase registryBase(String name, RType registryType, Map<String, Literal> resolved) {
        IRKind kind = registryType instanceof com.regnosys.rosetta.types.RRecordType
                ? IRKind.RECORD_TYPE : IRKind.BASIC_TYPE;
        Map<String, Literal> effective = new LinkedHashMap<>(seed(registryType));
        effective.putAll(resolved);
        List<IRTypeArgument> arguments = new ArrayList<>();
        for (String parameter : CANONICAL_PARAMETERS) {
            Literal literal = effective.get(parameter);
            if (literal != null) {
                arguments.add(literal.asArgument(parameter));
            }
        }
        for (Map.Entry<String, Literal> entry : resolved.entrySet()) {
            if (!CANONICAL_PARAMETERS.contains(entry.getKey())) {
                arguments.add(entry.getValue().asArgument(entry.getKey()));
            }
        }
        return new IREffectiveBase(kind, name, Optional.empty(), arguments);
    }

    /** The constraints the registry type carries itself; only a number type has any. */
    private static Map<String, Literal> seed(RType registryType) {
        Map<String, Literal> seeded = new LinkedHashMap<>();
        if (registryType instanceof RNumberType number) {
            number.digits().ifPresent(d -> seeded.put("digits", new Literal(Integer.toString(d), false)));
            number.fractionalDigits()
                    .ifPresent(f -> seeded.put("fractionalDigits", new Literal(Integer.toString(f), false)));
            number.min().ifPresent(m -> seeded.put("min", literalOf(m)));
            number.max().ifPresent(m -> seeded.put("max", literalOf(m)));
        }
        return seeded;
    }

    /** A seeded decimal constraint: the plain string of its magnitude, its sign carried as the negation. */
    private static Literal literalOf(BigDecimal value) {
        return new Literal(value.abs().toPlainString(), value.signum() < 0);
    }

    // === THE PROPERTY GATE (v3.3 seat 8, PR #644) ==============================================

    /**
     * THE CONDITION KINDS, index-parallel to the condition names and stated for EVERY condition.
     *
     * <p>The oracle is the old generator's own {@code ModelMetaGenerator.unnamedConditionKind} (PUBLIC static,
     * {@code rune-java-generator}): the expression ROOT decides the token — a {@code one-of} cardinality check is
     * {@code OneOf}, a {@code choice} one is {@code Choice}, anything else is {@code DataRule}. {@code rune-ir}
     * may NOT depend on {@code rune-java-generator} (its only module dependency is {@code rune-parser}), so the law
     * is REPRODUCED here rather than called — which is also what makes the two halves independent producers at the
     * reconcile (LAW 69). The old generator names only an UNNAMED condition's class by it; the IR states it for the
     * named ones too, because a fact stated only where it is ambiguous cannot be asserted over a whole population,
     * and because a backend reads the same token for a named condition when it decides a {@code one-of}-derived law
     * (the deep-path family's eligibility is exactly an OWN {@code OneOf}).
     */
    private static List<String> conditionKinds(List<RCondition> conditions) {
        List<String> kinds = new ArrayList<>();
        for (RCondition condition : conditions) {
            kinds.add(conditionKind(condition));
        }
        return kinds;
    }

    /** One condition's kind, by its expression root - the oracle's own three-way test. */
    private static String conditionKind(RCondition condition) {
        if (condition.expression() instanceof RCardinalityCheckExpr check) {
            if (check.op() == CardCheckOp.ONE_OF) {
                return "OneOf";
            }
            if (check.op() == CardCheckOp.CHOICE) {
                return "Choice";
            }
        }
        return "DataRule";
    }

    /**
     * THE WALKED ALIAS CHAIN: {@code alias} itself first, then the alias its body names, and so on to the last alias
     * before the leaf. The walk is the old generator's ({@code TypeFormatConstraintScan.aliasHierarchy}): each alias
     * is walked ONCE and the depth is bounded by {@link #MAX_ALIAS_DEPTH}, so a cyclic or unresolvable chain ENDS at
     * the last link walked rather than looping. That scan is package-private in the generator, so the law is walked
     * independently here through this adapter's own resolution — the same two-producer discipline the effective base
     * keeps. What the chain states is WHAT WAS WALKED; whether the chain reached a leaf is the separate fact
     * {@link IRType#effectiveBase()} states.
     */
    private static List<IRAliasLink> aliasChain(RTypeAlias alias) {
        List<IRAliasLink> chain = new ArrayList<>();
        List<RTypeAlias> walked = new ArrayList<>();
        RTypeAlias current = alias;
        int depth = 0;
        while (current != null && depth++ < MAX_ALIAS_DEPTH && !walked.contains(current)) {
            walked.add(current);
            chain.add(aliasLink(current));
            RTypeCall body = current.typeCall();
            RNode next = body == null ? null : safe(body::referencedType).orElse(null);
            current = next instanceof RTypeAlias inner ? inner : null;
        }
        return chain;
    }

    /** One rung: the alias's name, its namespace, its parameters and its conditions with their kinds. */
    private static IRAliasLink aliasLink(RTypeAlias alias) {
        List<String> parameterNames = new ArrayList<>();
        for (RTypeParameter parameter : alias.typeParameters()) {
            parameterNames.add(parameter.name());
        }
        List<Optional<String>> conditionNames = new ArrayList<>();
        for (RCondition condition : alias.conditions()) {
            conditionNames.add(condition.name());
        }
        return new IRAliasLink(alias.name(), namespaceOf(alias), parameterNames, conditionNames,
                conditionKinds(alias.conditions()));
    }

    /**
     * Adapt a {@code namespace} declaration to the MODEL-level {@link IRModelNode} ({@link IRKind#MODEL}) — the
     * facts of a model that no type, choice, enum or alias node carries and that the data-type emitter's DERIVED
     * files read: the namespace's documentation ({@code package-info.java}), the version stamp every POJO and
     * {@code *Meta} header carries, the {@code isEvent root} / {@code isProduct root} configurations and the
     * {@code [qualification]} functions (the {@code *Meta} qualify wing), every function's SIGNATURE and every
     * {@code with-meta} EXPRESSION (the two sources of the {@code FieldWithMeta*} / {@code ReferenceWithMeta*}
     * wrapper set that are not declaration facts).
     *
     * <p>What this node does NOT decide: WHICH configuration wins its kind over the workspace
     * ({@code RQualifiableConfig.firstRoot} — a load-order law, so the INDEX's, computed over the model nodes), and
     * which qualification function belongs to a root (the old generator compares node IDENTITY; the IR compares the
     * resolved qualified name, which only an index that refuses an AMBIGUOUS name keeps sound).
     *
     * <p>{@link IRModelNode#children()} is empty: a model's declarations are the workspace's own nodes, each adapted
     * and reconciled through its own pass.
     */
    public IRModelNode adaptModelNode(RModel model) {
        List<IRQualifiableConfig> configurations = new ArrayList<>();
        for (RQualifiableConfig configuration : model.configurations()) {
            String rootName = configuration.rootTypeName() == null ? "" : configuration.rootTypeName();
            configurations.add(new IRQualifiableConfig(configuration.kind().name(),
                    resolvedRef(rootName, IRKind.STRUCT, safe(configuration::rootType))));
        }
        List<IRFunctionSignature> signatures = new ArrayList<>();
        List<IRQualificationFunction> qualifications = new ArrayList<>();
        for (RRootElement element : model.rootElements()) {
            if (!(element instanceof RFunction function)) {
                continue;
            }
            // THE DECLARED inputs only: a function with no `inputs:` block carries the parser's own
            // `__synthesized_input__` placeholder (GeneratedInputRule - fork-internal scaffolding, "invisible to
            // upstream's declared-signature reads"), which no model wrote and no wrapper collector can claim (it
            // carries no annotation). The IR states the signature the model DECLARED; the placeholder is excluded
            // by the parser's OWN predicate, never by its name here. The reconciler applies the same predicate to
            // the old generator's `inputs()` read, so the two halves compare declared against declared.
            List<RAttribute> declaredInputs = new ArrayList<>();
            for (RAttribute input : function.inputs()) {
                if (!GeneratedInputRule.isSynthesized(input)) {
                    declaredInputs.add(input);
                }
            }
            List<IRField> inputs = new ArrayList<>();
            for (RAttribute input : declaredInputs) {
                inputs.add(adaptAttribute(input));
            }
            Optional<IRField> output = function.output().map(attribute -> (IRField) adaptAttribute(attribute));
            signatures.add(new IRFunctionSignature(function.name(), inputs, output));
            if (isQualification(function) && !declaredInputs.isEmpty()) {
                qualifications.add(new IRQualificationFunction(function.name(),
                        typeRef(declaredInputs.get(0).typeCall())));
            }
        }
        String namespace = model.namespace();
        return new IRModelNode(namespace == null ? "" : namespace, model.definition(), model.version(),
                configurations, qualifications, signatures, withMetaUses(model), Optional.empty(),
                IRMetadata.EMPTY);
    }

    /**
     * The {@code [qualification]} predicate. The AST carries no typed qualification flag — an annotation use states
     * only the NAME the model wrote — so this is the literal comparison the oracle itself makes
     * ({@code ModelMetaGenerator.collectQualifyFunctions}: {@code "qualification".equals(a.annotationName())}).
     * It reads an annotation's own identifier, not the structure of any language content.
     */
    private static boolean isQualification(RFunction function) {
        for (RAnnotationRef ref : function.annotationRefs()) {
            if ("qualification".equals(ref.annotationName())) {
                return true;
            }
        }
        return false;
    }

    /** The AST-walk guard of the wrapper collector's own walk ({@code MetaFieldGenerator.collectFromWithMetaExprs}). */
    private static final int WITH_META_WALK_GUARD = 100_000;

    /**
     * Every {@code with-meta} expression reachable from the model's root elements, in the walk's own order — the
     * SAME subtree walk the wrapper collector makes (a stack seeded with the root element, children pushed in
     * declaration order), so the two halves see the same population in the same order.
     */
    private List<IRWithMetaUse> withMetaUses(RModel model) {
        List<IRWithMetaUse> uses = new ArrayList<>();
        for (RRootElement element : model.rootElements()) {
            collectWithMetaUses(element, uses);
        }
        return uses;
    }

    /** The walk. It fails LOUD on exhaustion: a partial walk would drop a wrapper the bodies reference. */
    private void collectWithMetaUses(RNode root, List<IRWithMetaUse> uses) {
        Deque<RNode> stack = new ArrayDeque<>();
        stack.push(root);
        int guard = 0;
        while (!stack.isEmpty()) {
            if (guard++ >= WITH_META_WALK_GUARD) {
                throw new IllegalStateException("the with-meta walk exceeded " + WITH_META_WALK_GUARD
                        + " nodes under '" + root + "' - a partial walk would silently drop a metafield wrapper"
                        + " the generated bodies reference; failing loud instead");
            }
            RNode current = stack.pop();
            if (current instanceof RWithMetaExpr withMeta) {
                uses.add(withMetaUse(withMeta));
            }
            for (RNode child : current.children()) {
                if (child != null) {
                    stack.push(child);
                }
            }
        }
    }

    /**
     * One {@code with-meta} use: its entry names as written, and its argument's INFERRED type as a reference to the
     * leaf, with the constraints in force stated as literal arguments.
     *
     * <p>THE REFUSALS, both NAMED rather than silent: an argument the seam cannot type — because no inferrer was
     * given, because the inferrer answered nothing, or because the workspace typed it MISSING — is {@code missing};
     * an argument the workspace types {@code nothing} ({@code empty with-meta {…}}) is {@code nothing}, the
     * {@code ReferenceWithMetaVoid} line the old generator skips in silence (the ONE permanent waiver of the byte
     * gate). The {@code nothing} test is made on the RAW inferred type, exactly as the oracle makes it
     * ({@code MetaFieldGenerator.collectFromWithMetaExprs}); an alias whose own leaf is {@code nothing} therefore
     * states a {@code nothing}-named BASIC_TYPE reference rather than a refusal, as it does there.
     */
    private IRWithMetaUse withMetaUse(RWithMetaExpr expression) {
        List<String> entryNames = new ArrayList<>();
        for (RWithMetaEntry entry : expression.entries()) {
            entryNames.add(entry.metaName() == null ? "" : entry.metaName());
        }
        RExpression argument = expression.argument();
        RMetaAnnotatedType inferred = argument == null ? null : inferredTypeOf(argument);
        if (inferred == null || inferred.isMissing()) {
            return new IRWithMetaUse(entryNames, Optional.empty(), List.of(), Optional.of("missing"));
        }
        RType type = inferred.type();
        if (type == com.regnosys.rosetta.types.RBasicType.NOTHING) {
            return new IRWithMetaUse(entryNames, Optional.empty(), List.of(), Optional.of("nothing"));
        }
        RType leaf = aliasLeaf(type);
        return new IRWithMetaUse(entryNames, Optional.of(typeRefOf(leaf)), constraintArguments(leaf),
                Optional.empty());
    }

    /** The seam's read, tolerant of a node the engine never saw: null - and so a refusal - never a throw. */
    private RMetaAnnotatedType inferredTypeOf(RExpression expression) {
        try {
            return typeInferrer.inferredTypeOf(expression);
        } catch (RuntimeException untypeable) {
            return null;
        }
    }

    /** An inferred alias type unwrapped to the type it refers to, bounded by the chain's own depth law. */
    private static RType aliasLeaf(RType type) {
        RType leaf = type;
        int depth = 0;
        while (leaf instanceof RAliasType alias && depth++ < MAX_ALIAS_DEPTH) {
            leaf = alias.refersTo();
        }
        return leaf;
    }

    /**
     * An INFERRED type as a type REFERENCE, by its leaf. A declared type ({@code data} / {@code enum} /
     * {@code choice}) is referenced with its TRUE kind and its declaring namespace, the way every declared reference
     * in this adapter is; a builtin is a BASIC_TYPE / RECORD_TYPE reference named as the registry names it — an
     * inferred number is {@code number} whatever grain it carries, because the grain is stated by
     * {@link #constraintArguments} beside it, and it is the pair that decides the wrapper's Java type.
     *
     * <p>The final throw is unreachable by construction: {@link #withMetaUse} refuses a MISSING type BY NAME before
     * it gets here, and an alias is unwrapped above. It fails loud rather than inventing a reference for a type the
     * IR cannot name.
     */
    private static IRType typeRefOf(RType type) {
        RType leaf = aliasLeaf(type);
        if (leaf instanceof RDataTypeRef data) {
            return resolvedRef(data.name(), IRKind.STRUCT, Optional.of(data.astNode()));
        }
        if (leaf instanceof REnumTypeRef enumeration) {
            return resolvedRef(enumeration.name(), IRKind.ENUM, Optional.of(enumeration.astNode()));
        }
        if (leaf instanceof RChoiceTypeRef choice) {
            return resolvedRef(choice.name(), IRKind.CHOICE, Optional.ofNullable(choice.astNode()));
        }
        if (leaf instanceof RNumberType) {
            return IRTypeNode.reference("number", IRKind.BASIC_TYPE, Optional.empty(), Optional.of("number"));
        }
        if (leaf instanceof RStringType) {
            return IRTypeNode.reference("string", IRKind.BASIC_TYPE, Optional.empty(), Optional.of("string"));
        }
        if (leaf instanceof com.regnosys.rosetta.types.RRecordType recordType) {
            return IRTypeNode.reference(recordType.name(), IRKind.RECORD_TYPE, Optional.empty(),
                    Optional.of(recordType.name()));
        }
        if (leaf instanceof com.regnosys.rosetta.types.RBasicType basic) {
            return IRTypeNode.reference(basic.name(), IRKind.BASIC_TYPE, Optional.empty(),
                    Optional.of(basic.name()));
        }
        throw new IllegalStateException("the workspace typed an expression as '" + leaf
                + "', which the IR can name no reference for - a MISSING type is refused by name before this point"
                + " and an alias is unwrapped to its leaf");
    }

    /**
     * The constraints IN FORCE on an inferred builtin leaf, as LITERAL arguments in {@link #CANONICAL_PARAMETERS}
     * order — the number ladder ({@code digits}, {@code fractionalDigits}, {@code min}, {@code max}) and the string
     * ladder ({@code minLength}, {@code maxLength}, {@code pattern}). Empty for a declared type and for an
     * unconstrained builtin.
     *
     * <p>A NOTE ON THE PATTERN: these are constraints the TYPE ENGINE computed, not text the model wrote, so the
     * pattern is stated as the compiled pattern's own source, WITHOUT the quotation marks a written string literal
     * carries in {@link IRType#baseTypeArguments()}. A reconciler reading both must read this one against
     * {@code RStringType.pattern().pattern()}, not against the source text.
     */
    private static List<IRTypeArgument> constraintArguments(RType leaf) {
        Map<String, Literal> stated = new LinkedHashMap<>(seed(leaf));
        if (leaf instanceof RStringType string) {
            string.minLength().ifPresent(m -> stated.put("minLength", new Literal(Integer.toString(m), false)));
            string.maxLength().ifPresent(m -> stated.put("maxLength", new Literal(Integer.toString(m), false)));
            string.pattern().ifPresent(p -> stated.put("pattern", new Literal(p.pattern(), false)));
        }
        List<IRTypeArgument> arguments = new ArrayList<>();
        for (String parameter : CANONICAL_PARAMETERS) {
            Literal literal = stated.get(parameter);
            if (literal != null) {
                arguments.add(literal.asArgument(parameter));
            }
        }
        return arguments;
    }

    /** A reference named as written, classed and located by the declaration it resolves to. */
    private static IRType resolvedRef(String writtenName, IRKind unresolvedKind, Optional<? extends RNode> resolved) {
        if (resolved.isEmpty()) {
            return IRTypeNode.reference(writtenName, unresolvedKind, Optional.empty(), Optional.empty());
        }
        RNode target = resolved.get();
        return IRTypeNode.reference(writtenName, kindOf(target).orElse(unresolvedKind),
                namespaceOf(target), declaredName(target));
    }

    /** The IR kind of a resolved type declaration; empty for a node that is no type declaration. */
    static Optional<IRKind> kindOf(RNode declaration) {
        if (declaration instanceof REnumeration) {
            return Optional.of(IRKind.ENUM);
        }
        if (declaration instanceof RChoice) {
            return Optional.of(IRKind.CHOICE);
        }
        if (declaration instanceof RDataType) {
            return Optional.of(IRKind.STRUCT);
        }
        if (declaration instanceof RBasicType) {
            return Optional.of(IRKind.BASIC_TYPE);
        }
        if (declaration instanceof RTypeAlias) {
            return Optional.of(IRKind.TYPE_ALIAS);
        }
        if (declaration instanceof RRecordType) {
            return Optional.of(IRKind.RECORD_TYPE);
        }
        if (declaration instanceof RMetaType) {
            return Optional.of(IRKind.META_TYPE);
        }
        return Optional.empty();
    }

    /** The namespace of the model that declares {@code declaration}; empty for a detached node. */
    static Optional<String> namespaceOf(RNode declaration) {
        return AstWalker.findAncestor(declaration, RModel.class).map(RModel::namespace);
    }

    /** A type declaration's own simple name. */
    static Optional<String> declaredName(RNode declaration) {
        if (declaration instanceof RDataType d) {
            return Optional.ofNullable(d.name());
        }
        if (declaration instanceof RChoice c) {
            return Optional.ofNullable(c.name());
        }
        if (declaration instanceof REnumeration e) {
            return Optional.ofNullable(e.name());
        }
        if (declaration instanceof RBasicType b) {
            return Optional.ofNullable(b.name());
        }
        if (declaration instanceof RTypeAlias a) {
            return Optional.ofNullable(a.name());
        }
        if (declaration instanceof RRecordType r) {
            return Optional.ofNullable(r.name());
        }
        if (declaration instanceof RMetaType m) {
            return Optional.ofNullable(m.name());
        }
        if (declaration instanceof RRule rule) {
            return Optional.ofNullable(rule.name());
        }
        return Optional.empty();
    }

    private IRType superTypeRef(RDataType dataType, String superName) {
        Optional<RChoice> choiceSuper = safe(dataType::choiceSuperType);
        if (choiceSuper.isPresent()) {
            return resolvedRef(superName, IRKind.CHOICE, choiceSuper);
        }
        return resolvedRef(superName, IRKind.STRUCT, safe(dataType::superType));
    }

    /** A cross-reference read that tolerates an unresolved symbol / a detached workspace: empty, never a throw. */
    private static <T> Optional<T> safe(java.util.function.Supplier<Optional<T>> read) {
        try {
            Optional<T> value = read.get();
            return value == null ? Optional.empty() : value;
        } catch (RuntimeException unresolved) {
            return Optional.empty();
        }
    }

    private static Cardinality cardinality(Optional<RCardinality> cardinality) {
        if (cardinality.isEmpty()) {
            return Cardinality.ONE_TO_ONE;
        }
        RCardinality c = cardinality.get();
        boolean lowerZero = c.inf() == null || c.inf().signum() == 0;
        boolean many = c.isUnbounded()
                || (c.sup() != null && c.sup().compareTo(BigInteger.ONE) > 0);
        if (lowerZero) {
            return many ? Cardinality.ZERO_TO_MANY : Cardinality.ZERO_TO_ONE;
        }
        return many ? Cardinality.ONE_TO_MANY : Cardinality.ONE_TO_ONE;
    }

    /** The EXACT bounds of a declared cardinality: an absent lower bound reads 0, the {@code *} form no upper. */
    static IRBounds bounds(RCardinality cardinality) {
        BigInteger lower = cardinality.inf() == null ? BigInteger.ZERO : cardinality.inf();
        Optional<BigInteger> upper = cardinality.isUnbounded() ? Optional.empty() : Optional.ofNullable(cardinality.sup());
        return new IRBounds(lower, upper);
    }

    private static List<IRTypeArgument> typeArguments(RTypeCall typeCall) {
        if (typeCall == null) {
            return List.of();
        }
        List<IRTypeArgument> arguments = new ArrayList<>();
        for (RTypeCallArgument argument : typeCall.arguments()) {
            var value = argument.value();
            arguments.add(new IRTypeArgument(argument.parameterName(),
                    value == null ? Optional.empty() : value.nameValue(),
                    value == null ? Optional.empty() : value.literalValue(),
                    value != null && value.isNegated()));
        }
        return arguments;
    }

    private static List<IRAnnotationUse> annotations(List<RAnnotationRef> refs) {
        List<IRAnnotationUse> uses = new ArrayList<>();
        for (RAnnotationRef ref : refs) {
            List<IRAnnotationUse.Argument> arguments = new ArrayList<>();
            for (RAnnotationQualifier qualifier : ref.qualifiers()) {
                arguments.add(new IRAnnotationUse.Argument(qualifier.key(),
                        qualifier.value() == null ? "" : qualifier.value(), qualifier.isAttributeRef()));
            }
            uses.add(new IRAnnotationUse(ref.annotationName(), ref.qualifierName(), arguments));
        }
        return uses;
    }

    private List<IRDocReference> docReferences(List<RDocReference> refs) {
        List<IRDocReference> out = new ArrayList<>();
        for (RDocReference ref : refs) {
            RRegulatoryDocumentReference doc = ref.regulatoryDocRef();
            List<IRDocReference.Corpus> corpora = new ArrayList<>();
            List<IRDocReference.Segment> segments = new ArrayList<>();
            if (doc != null) {
                for (String corpusName : doc.corpusRefs()) {
                    corpora.add(new IRDocReference.Corpus(corpusName,
                            corpusResolver.resolve(ref, corpusName).map(AstToIRAdapter::corpusDeclaration)));
                }
                for (RSegmentRef segment : doc.segmentRefs()) {
                    segments.add(new IRDocReference.Segment(segment.segmentName(), segment.value()));
                }
            }
            List<IRDocReference.Rationale> rationales = new ArrayList<>();
            for (RDocumentRationale rationale : ref.rationales()) {
                rationales.add(new IRDocReference.Rationale(rationale.rationale(), rationale.rationaleAuthor()));
            }
            List<IRDocReference.NamedArg> namedArgs = new ArrayList<>();
            for (RRegulatoryReferenceArg arg : ref.namedArgs()) {
                namedArgs.add(new IRDocReference.NamedArg(arg.name(), arg.value()));
            }
            out.add(new IRDocReference(ref.isRegulatoryReference(), ref.forPath().map(AstToIRAdapter::path),
                    doc == null ? Optional.empty() : Optional.ofNullable(doc.bodyRef()), corpora, segments,
                    rationales, ref.structuredProvision(), ref.provision(), ref.isReportedField(), namedArgs));
        }
        return out;
    }

    private static IRDocReference.Corpus.Declaration corpusDeclaration(RCorpus corpus) {
        return new IRDocReference.Corpus.Declaration(Optional.ofNullable(corpus.corpusTypeKeyword()),
                corpus.name(), corpus.displayName(), corpus.definition());
    }

    private static IRAnnotationPath path(RAnnotationPathExpression expression) {
        List<IRAnnotationPath.Step> steps = new ArrayList<>();
        for (RAnnotationPathSegment segment : expression.segments()) {
            steps.add(new IRAnnotationPath.Step(segment.name(), segment.isDeep()));
        }
        return new IRAnnotationPath(expression.isRootItem(), expression.root(), steps);
    }

    private static List<IRLabel> labels(List<RLabelAnnotation> annotations) {
        List<IRLabel> labels = new ArrayList<>();
        for (RLabelAnnotation annotation : annotations) {
            labels.add(new IRLabel(annotation.label(), annotation.forPath().map(AstToIRAdapter::path),
                    annotation.asPath().map(AstToIRAdapter::path)));
        }
        return labels;
    }

    private static List<IRRuleReference> ruleReferences(List<RRuleReferenceAnnotation> annotations) {
        List<IRRuleReference> references = new ArrayList<>();
        for (RRuleReferenceAnnotation annotation : annotations) {
            Optional<RRule> rule = safe(annotation::rule);
            references.add(new IRRuleReference(annotation.forPath().map(AstToIRAdapter::path),
                    annotation.ruleName(), rule.flatMap(AstToIRAdapter::namespaceOf),
                    rule.flatMap(AstToIRAdapter::declaredName)));
        }
        return references;
    }

    private static String qualifiedName(String namespace, String simpleName) {
        if (namespace == null || namespace.isEmpty()) {
            return simpleName;
        }
        return namespace + "." + simpleName;
    }
}
