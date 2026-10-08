package com.regnosys.rosetta.generator.java.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.regnosys.rosetta.ast.annotations.RAnnotationRef;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.supporting.RChoiceOption;
import com.regnosys.rosetta.ast.types.RChoice;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.object.MetaFieldGenerator;
import com.regnosys.rosetta.generator.java.object.ModelObjectGenerator;
import com.regnosys.rosetta.generator.java.scoping.JavaPackageName;
import com.regnosys.rosetta.generator.java.util.ModelGeneratorUtil;
import com.regnosys.rosetta.types.RType;
import com.rosetta.model.lib.process.AttributeMeta;
import com.rosetta.util.types.JavaClass;
import com.rosetta.util.types.JavaGenericTypeDeclaration;
import com.rosetta.util.types.JavaParameterizedType;
import com.rosetta.util.types.JavaType;

/**
 * Concrete implementation of {@link JavaPojoInterface} backed by our AST
 * {@link RDataType}. Bridges the M2 AST to the POJO type system used by
 * code generators.
 *
 * <p>Ported from upstream's {@code RJavaPojoInterface} which wraps the M4
 * {@code RDataType}. Our version uses {@code GeneratorModel} for type
 * resolution instead of the upstream's Guice-injected services.
 */
public class RJavaPojoInterface extends JavaPojoInterface {

    private final RDataType astNode;      // null when constructed from RChoice
    private final RChoice choiceNode;     // null when constructed from RDataType
    private final GeneratorModel generatorModel;
    private final JavaTypeTranslator typeTranslator;
    private final JavaTypeUtil typeUtil;
    private final ModelGeneratorUtil generatorUtil;

    private RJavaPojoInterface superPojo = null;
    private boolean superPojoInitialized = false;
    private Map<String, JavaPojoProperty> ownProperties = null;
    private Map<String, JavaPojoProperty> allProperties = null;

    /** Constructor for data types. */
    public RJavaPojoInterface(RDataType astNode, GeneratorModel generatorModel,
                               JavaTypeTranslator typeTranslator, JavaTypeUtil typeUtil) {
        super(JavaPackageName.escape(generatorModel.namespace(astNode)),
                astNode.name(), typeUtil);
        this.astNode = astNode;
        this.choiceNode = null;
        this.generatorModel = generatorModel;
        this.typeTranslator = typeTranslator;
        this.typeUtil = typeUtil;
        this.generatorUtil = new ModelGeneratorUtil(generatorModel.workspace());
    }

    /**
     * Constructor for choice types — used to create a super pojo when a data type
     * extends a choice type (upstream: caseChoiceType → caseDataType).
     * Choice options become properties of this pojo.
     *
     * <p>P2.1.3c defensive guard: a choice type must declare at least one option.
     * The grammar requires {@code options+=choiceOption (COMMA options+=choiceOption)*}
     * which mandates ≥1 option, so this check defends against parser-bypass or
     * mutation-bypass construction paths producing a degenerate empty-options
     * RChoice that would yield a zero-property POJO (silent miss-emission).
     */
    public RJavaPojoInterface(RChoice choiceNode, GeneratorModel generatorModel,
                               JavaTypeTranslator typeTranslator, JavaTypeUtil typeUtil) {
        super(JavaPackageName.escape(generatorModel.namespace(checkChoice(choiceNode))),
                choiceNode.name(), typeUtil);
        this.astNode = null;
        this.choiceNode = choiceNode;
        this.generatorModel = generatorModel;
        this.typeTranslator = typeTranslator;
        this.typeUtil = typeUtil;
        this.generatorUtil = new ModelGeneratorUtil(generatorModel.workspace());
    }

    /**
     * Defensive guard for the choice ctor: rejects an empty-options choice.
     * Returns the input unchanged on success (used as a passthrough inside the
     * {@code super(...)} expression so it fires before any instance state is
     * established — Java's first-statement rule for ctor invocations).
     */
    private static RChoice checkChoice(RChoice choiceNode) {
        Preconditions.checkArgument(choiceNode.options().size() >= 1,
                "Choice type '%s' must have at least 1 option, got %s",
                choiceNode.name(), choiceNode.options().size());
        return choiceNode;
    }

    public RDataType getAstNode() {
        return astNode;
    }

    public RChoice getChoiceNode() {
        return choiceNode;
    }

    /** Whether this pojo represents a choice type (not a data type). */
    public boolean isChoiceType() {
        return choiceNode != null;
    }

    @Override
    public String getJavadoc() {
        if (choiceNode != null) {
            return generatorUtil.javadoc(
                    choiceNode.definition().orElse(null),
                    List.of(), getVersion());
        }
        return generatorUtil.javadoc(
                astNode.definition().orElse(null),
                astNode.docReferences(),
                getVersion());
    }

    @Override
    public String getRosettaName() {
        return choiceNode != null ? choiceNode.name() : astNode.name();
    }

    @Override
    public String getVersion() {
        com.regnosys.rosetta.ast.RRootElement node = choiceNode != null ? choiceNode : astNode;
        if (node.parent() instanceof com.regnosys.rosetta.ast.model.RModel model) {
            // Upstream's RosettaModel.version EMF default when the source declares none
            // (Rosetta.xcore: String version = "0.0.0") — see GeneratorModel#version.
            return model.version().orElse(
                    com.regnosys.rosetta.generator.java.GeneratorModel.UNDECLARED_MODEL_VERSION);
        }
        return null;
    }

    @Override
    public Collection<JavaPojoProperty> getOwnProperties() {
        initializeProperties();
        return ownProperties.values();
    }

    @Override
    public Collection<JavaPojoProperty> getAllProperties() {
        initializeProperties();
        return allProperties.values();
    }

    @Override
    public RJavaPojoInterface getSuperPojo() {
        if (!superPojoInitialized) {
            superPojoInitialized = true;
            if (choiceNode != null) {
                // Choice types have no super type
                superPojo = null;
            } else {
                // Check data type super first, then choice super
                astNode.superType().ifPresent(superType ->
                    superPojo = new RJavaPojoInterface(superType, generatorModel,
                            typeTranslator, typeUtil));
                if (superPojo == null) {
                    astNode.choiceSuperType().ifPresent(choiceSuperType ->
                        superPojo = new RJavaPojoInterface(choiceSuperType, generatorModel,
                                typeTranslator, typeUtil));
                }
            }
        }
        return superPojo;
    }

    @Override
    public List<JavaClass<?>> getInterfaceDeclarations() {
        List<JavaClass<?>> interfaces = new ArrayList<>();
        RJavaPojoInterface sp = getSuperPojo();
        if (sp == null) {
            interfaces.add(typeUtil.ROSETTA_MODEL_OBJECT);
        } else {
            interfaces.add(sp);
        }
        if (hasTypeMetaAnnotation("key")) {
            interfaces.add(typeUtil.GLOBAL_KEY);
        }
        if (hasTypeMetaAnnotation("template")) {
            interfaces.add(typeUtil.TEMPLATABLE);
        }
        return interfaces;
    }

    /** Check if the data type itself has a [metadata X] annotation. */
    private boolean hasTypeMetaAnnotation(String qualifierName) {
        List<RAnnotationRef> refs = choiceNode != null
                ? choiceNode.annotationRefs() : astNode.annotationRefs();
        return refs.stream()
                .anyMatch(ref -> "metadata".equals(ref.annotationName())
                        && ref.qualifierName().map(q -> q.equals(qualifierName)).orElse(false));
    }

    @Override
    public List<JavaClass<?>> getInterfaces() {
        return getInterfaceDeclarations();
    }

    // -- Property initialization ------------------------------------------------

    private void initializeProperties() {
        if (ownProperties != null) return;

        RJavaPojoInterface sp = getSuperPojo();
        if (sp == null) {
            allProperties = new LinkedHashMap<>();
        } else {
            sp.initializeProperties();
            allProperties = new LinkedHashMap<>(sp.allProperties);
        }
        ownProperties = new LinkedHashMap<>();

        // Choice types: options become properties (each is a single-valued model-object)
        if (choiceNode != null) {
            initializeChoiceProperties();
            // Synthetic "meta" property for choice types with type-level [metadata key]
            // (e.g. `choice Payout: [metadata key] ...`) — mirrors the RDataType branch
            // below. Must run AFTER initializeChoiceProperties so it appears last in
            // the property order, matching the upstream golden's getter ordering.
            //
            // R4 F4-6 defensive guard: the RDataType branch below performs additional
            // template-specific scaffolding when [metadata template] is present
            // (handled by the surrounding generator pipeline, not visible at this
            // synthesis point). For a top-level `choice` carrying [metadata template]
            // (whether alone or together with [metadata key]), this branch currently
            // only adds the synthetic `meta` property and returns — the template
            // scaffolding is NOT applied. No current corpus has a top-level choice
            // with [metadata template] (verified at R4: grep across cdm-5.32 through
            // cdm-6.18 + drr corpora found no `choice X: [metadata template]`
            // combinations — the only [metadata template] usages are on RDataType
            // declarations such as cdm `type ContractualProduct`). The guard below
            // fails fast on the first occurrence so we can extend the choice path
            // intentionally rather than silently degrade. P2.1.3c U015 doesn't
            // support [metadata template] on choice types — please file an issue
            // if encountered.
            if (hasTypeMetaAnnotation("template")) {
                throw new IllegalStateException(
                        "Top-level `choice` with [metadata template] is not currently "
                        + "supported (choice: " + choiceNode.name() + "). The RDataType "
                        + "branch's template scaffolding has not been mirrored on the "
                        + "choice path — P2.1.3c U015 deliberately scoped to [metadata "
                        + "key] on choice (per the Payout test fixture). File an issue "
                        + "with the choice declaration so the choice-path template "
                        + "scaffolding can be designed intentionally rather than "
                        + "silently degraded.");
            }
            if (hasTypeMetaAnnotation("key")) {
                addProperty("meta", typeUtil.META_FIELDS, null, null, null, false, List.of(), false, null);
            }
            return;
        }

        for (RAttribute attr : astNode.attributes()) {
            String name = attr.name();
            RType rtype = generatorModel.getType(attr);
            // Meta attribute wrapping (MetaFieldGenerator.detectMetaKind): [metadata scheme/id/location]
            // → FieldWithMeta, [metadata reference/address] → ReferenceWithMeta (reference wins when both
            // are present); the original (unwrapped) type is kept for import generation. The property's Java type is THE ONE computation
            // declaredPropertyJavaType (seat-21 review SF-3 / LAW 69) — the same body the
            // override-chain selector builds its desired type with.
            MetaFieldGenerator.MetaKind metaKind = detectMetaKind(attr);
            JavaType metaValueType = metaKind == MetaFieldGenerator.MetaKind.FIELD_WITH_META
                    || metaKind == MetaFieldGenerator.MetaKind.REFERENCE_WITH_META
                    ? typeTranslator.toJavaReferenceType(rtype) : null;
            JavaType javaType = declaredPropertyJavaType(attr, rtype, generatorModel, typeTranslator, typeUtil);

            // facet docrefInheritance (PR #330): the getter javadoc carries ALL doc
            // references — the override-parent chain's FIRST, then the attribute's own
            // (upstream RJavaPojoInterface uses attr.getAllDocReferences() =
            // inheritAnnotationsFromParent, the SAME recursion the #321 label seat
            // ports as allLabelAnnotations). The fork read own docrefs only, dropping
            // e.g. the Case-0 annotations-only parent CFTCTransactionReport.leg1/leg2
            // regulatoryReference blocks from the specialized CFTCPart45 override
            // getters (1,039 golden javadoc lines). Green-safe by construction: golden
            // always includes inherited docrefs, so no green file carries a
            // specialized override whose parent chain has docrefs.
            String javadoc = generatorUtil.javadoc(
                    attr.definition().orElse(null), allDocReferences(attr), null);

            // Meta annotations for AttributeMeta and meta type flags
            AttributeMeta meta = hasMetaAnnotation(attr, "id")
                    ? AttributeMeta.GLOBAL_KEY_FIELD : null;
            boolean hasLocation = hasMetaAnnotation(attr, "location");
            List<AttributeMetaType> metaTypes = new ArrayList<>();
            if (hasMetaAnnotation(attr, "address")) {
                metaTypes.add(AttributeMetaType.SCOPED_REFERENCE);
            }
            if (hasMetaAnnotation(attr, "location")) {
                metaTypes.add(AttributeMetaType.SCOPED_KEY);
            }

            boolean isRequired = attr.cardinality()
                    .map(c -> c.inf().intValue() > 0)
                    .orElse(false);

            // facet pojoOverrideNaming (PR #323): the bare item RType feeds the model-level
            // subtype check for the getter-compatibility law. Only meaningful when the item
            // is NOT meta-wrapped (a wrapped property's Java item is the FieldWithMeta/
            // ReferenceWithMeta class, whose ancestry the bare value RType does not describe).
            RType itemRTypeForSubtype =
                    (metaKind == MetaFieldGenerator.MetaKind.NONE) ? rtype : null;
            addProperty(name, javaType, metaValueType, javadoc, meta, hasLocation, metaTypes,
                    isRequired, itemRTypeForSubtype);
        }

        // Synthetic "meta" property for types with [metadata key]
        if (hasTypeMetaAnnotation("key")) {
            JavaType metaFieldsType = hasTypeMetaAnnotation("template")
                    ? typeUtil.META_AND_TEMPLATE_FIELDS
                    : typeUtil.META_FIELDS;
            addProperty("meta", metaFieldsType, null, null, null, false, List.of(), false, null);
        }
    }

    /**
     * Initialize properties from choice options. Each option becomes a single-valued
     * model-object property. The property name is the type name (e.g., "Cash").
     *
     * <p>P2.1.3c β1: javadoc on each option is propagated to the per-property
     * javadoc string so the interface getter emits the option's documentation
     * comment (mirrors the upstream golden where each variant getter has its
     * own javadoc block).
     *
     * <p>P2.1.3c β1 meta extension: per-option {@code [metadata location/address/
     * scheme/id/reference]} annotations are propagated identically to the
     * RDataType {@link RAttribute} path. {@code [metadata location/scheme/id]}
     * wrap in {@code FieldWithMeta*}; {@code [metadata address/reference]} wrap
     * in {@code ReferenceWithMeta*}; {@code hasLocation} + the SCOPED_KEY /
     * SCOPED_REFERENCE {@link AttributeMetaType} flags drive
     * {@code @RuneScopedAttributeKey} / {@code @RuneScopedAttributeReference}
     * annotation emission in {@link ModelObjectGenerator}.
     */
    private void initializeChoiceProperties() {
        for (RChoiceOption option : choiceNode.options()) {
            String optionTypeName = option.typeCall().typeName();
            RType rtype = generatorModel.resolveTypeCall(option.typeCall());
            JavaType javaType = typeTranslator.toJavaReferenceType(rtype);

            // P2.1.3c T2 R1 F15: cache the annotation list + per-qualifier
            // booleans once per option — prior version walked option.annotationRefs()
            // three times (detectMetaKind + two hasOptionMetaAnnotation calls).
            List<RAnnotationRef> optionRefs = option.annotationRefs();
            boolean hasLocation = hasOptionMetaAnnotation(optionRefs, "location");
            boolean hasAddress = hasOptionMetaAnnotation(optionRefs, "address");

            // Meta wrapping: [metadata scheme/id/location] → FieldWithMeta,
            // [metadata reference/address] → ReferenceWithMeta. Mirrors the
            // RDataType-attribute branch above (initializeProperties).
            JavaType metaValueType = null;
            MetaFieldGenerator.MetaKind metaKind =
                    MetaFieldGenerator.detectMetaKind(optionRefs);
            if (metaKind == MetaFieldGenerator.MetaKind.FIELD_WITH_META) {
                metaValueType = javaType;
                javaType = wrapFieldWithMeta(javaType);
            } else if (metaKind == MetaFieldGenerator.MetaKind.REFERENCE_WITH_META) {
                metaValueType = javaType;
                javaType = wrapReferenceWithMeta(javaType);
            }

            // Per-option attribute-meta flags (mirrors the RDataType path).
            List<AttributeMetaType> metaTypes = new ArrayList<>();
            if (hasAddress) {
                metaTypes.add(AttributeMetaType.SCOPED_REFERENCE);
            }
            if (hasLocation) {
                metaTypes.add(AttributeMetaType.SCOPED_KEY);
            }

            String javadoc = generatorUtil.javadoc(
                    option.definition().orElse(null), List.of(), null);
            // Choice options are single-valued, non-required, model-object properties.
            // facet pojoOverrideNaming (PR #323): the option's bare RType feeds the subtype
            // check exactly like the RDataType-attribute path (meta-wrapped options pass null).
            RType itemRTypeForSubtype =
                    (metaKind == MetaFieldGenerator.MetaKind.NONE) ? rtype : null;
            addProperty(optionTypeName, javaType, metaValueType, javadoc, null,
                    hasLocation, metaTypes, false, itemRTypeForSubtype);
        }
    }

    /**
     * Per-option meta annotation check (RChoiceOption analog of
     * {@link #hasMetaAnnotation(RAttribute, String)}). RChoiceOption is not an
     * RAttribute, so the existing helper cannot be reused directly.
     *
     * <p>P2.1.3c T2 R1 F15: operates on the pre-fetched annotation-ref list
     * (callers cache {@code option.annotationRefs()} once per option to avoid
     * repeated traversal for each qualifier check).
     */
    private boolean hasOptionMetaAnnotation(List<RAnnotationRef> annotationRefs,
                                             String qualifierName) {
        return annotationRefs.stream()
                .anyMatch(ref -> "metadata".equals(ref.annotationName())
                        && ref.qualifierName().map(q -> q.equals(qualifierName)).orElse(false));
    }

    /**
     * facet pojoOverrideNaming (PR #323): a faithful port of upstream
     * {@code RJavaPojoInterface.addPropertyIfNecessary} (verified against the 9.83.0 goldens) — the POJO
     * property-specialization law. Per declared attribute whose name already exists on the
     * super chain:
     * <ul>
     *   <li><b>Case 0 (no specialization)</b> — identical property Java type AND identical
     *       requiredness (an annotations-only {@code override}): NO new property is created;
     *       the attribute contributes zero members anywhere (the parent property continues
     *       to represent it, and the overriding declaration's javadoc is dropped).</li>
     *   <li><b>Specialized</b> — type or requiredness differs: the parent property is
     *       specialized with law-computed compatibility names. The GETTER keeps the parent's
     *       name when the new type is a (generic/model) subtype of the parent's (emitted
     *       with {@code @Override}); otherwise it is renamed via
     *       {@link #incompatiblePropertyName}. The SETTER is renamed via
     *       {@link #incompatiblePropertyName} when the two property types' ERASURES are
     *       equal (Java cannot overload — e.g. a requiredness-only change:
     *       {@code setTechnicalRecordIdOverriddenAsString}); a different erasure OVERLOADS
     *       the parent's setter name (e.g. {@code setLeg1(CommonLeg)} beside
     *       {@code setLeg1(Leg)}).</li>
     * </ul>
     * The specialized property re-{@code put}s into the {@code allProperties} LinkedHashMap,
     * which keeps the ANCESTOR's iteration position (upstream relies on the same re-put
     * semantics), and chains {@code parentProperty} one node per SPECIALIZING type — the
     * chain drives the per-ancestor {@code @RosettaIgnore} delegate setters and the
     * builder-interface generation segments in {@link ModelObjectGenerator}.
     */
    private void addProperty(String name, JavaType type, JavaType metaValueType,
                              String javadoc, AttributeMeta meta, boolean hasLocation,
                              List<AttributeMetaType> attributeMetaTypes,
                              boolean isRequired, RType itemRTypeForSubtype) {
        JavaPojoProperty parentProperty = allProperties.get(name);
        if (parentProperty == null) {
            JavaPojoProperty prop = new JavaPojoProperty(
                    this, name, name, name, name, name,
                    type, metaValueType, javadoc, meta, hasLocation,
                    attributeMetaTypes, isRequired);
            ownProperties.put(name, prop);
            allProperties.put(name, prop);
            return;
        }
        JavaType parentType = parentProperty.getType();
        if (type.equals(parentType) && isRequired == parentProperty.isRequired()) {
            // Case 0 — no new property (upstream: the else-branch simply does nothing).
            return;
        }
        boolean subtypeOfParent = isPojoSubtype(type, parentType, itemRTypeForSubtype);
        String getterCompatibilityName = subtypeOfParent
                ? parentProperty.getGetterCompatibilityName()
                : incompatiblePropertyName(name, parentType, type, typeUtil);
        String setterCompatibilityName =
                type.getTypeErasure().equals(parentType.getTypeErasure())
                        ? incompatiblePropertyName(name, parentType, type, typeUtil)
                        : parentProperty.getSetterCompatibilityName();
        JavaPojoProperty specialized = parentProperty.specialize(
                this, getterCompatibilityName, setterCompatibilityName, type, metaValueType,
                javadoc, meta, hasLocation, attributeMetaTypes, isRequired, subtypeOfParent);
        ownProperties.put(name, specialized);
        allProperties.put(name, specialized);
    }

    /**
     * facet pojoOverrideNaming (PR #323): a port of upstream
     * {@code RJavaPojoInterface.getIncompatiblePropertyName} (verified against the 9.83.0 goldens) — the
     * {@code OverriddenAs} name-construction law. The suffix is the SPECIALIZED ITEM Java
     * type's simple name (the meta wrapper's when the item is wrapped), never the attribute
     * name. Public static + shared: {@code FunctionExpressionRenderer}'s report-seat and
     * function-path override-setter derivations delegate here so the POJO declaration and
     * every expression-side call site can never drift.
     */
    public static String incompatiblePropertyName(String propertyName,
            JavaType parentType, JavaType specializedType, JavaTypeUtil typeUtil) {
        if (typeUtil.isList(parentType) && typeUtil.isList(specializedType)) {
            // List to list
            return incompatiblePropertyName(propertyName,
                    typeUtil.getItemType(parentType), typeUtil.getItemType(specializedType),
                    typeUtil);
        } else if (typeUtil.isList(parentType)) {
            // List to single
            JavaType parentItemType = typeUtil.getItemType(parentType);
            if (parentItemType.equals(specializedType)) {
                return propertyName + "OverriddenAsSingle";
            }
            return propertyName + "OverriddenAsSingle" + specializedType.getSimpleName();
        }
        // Type to other type
        return propertyName + "OverriddenAs" + specializedType.getSimpleName();
    }

    /**
     * facet pojoOverrideNaming (PR #323): the generic-subtype check the getter-compatibility
     * law needs (upstream {@code type.isSubtypeOf(parentType)}). The fork's generated model
     * classes are flat {@code RGeneratedJavaClass} instances whose only supertype is
     * {@code RosettaModelObject}, so the runtime {@code isSubtypeOf} cannot see MODEL-level
     * covariance ({@code CommonLeg} extends {@code Leg}); this helper adds it by walking the
     * Rosetta supertype chain of the CHILD item's {@code RType}. Semantics mirrored from
     * upstream generics: equal types are subtypes; {@code List<? extends X>} is a subtype of
     * {@code List<? extends Y>} iff X is a subtype of Y (both sides here are built by this
     * class's own wrap, so like-for-like); meta-wrapper items are invariant (a wrapped item
     * passes {@code null} and only equality can succeed); basic items are invariant beyond
     * equality (the walk only understands data/choice ancestry — a hypothetical
     * number-hierarchy specialization would read as incompatible and surface as a
     * divergence, never a green regression).
     */
    private boolean isPojoSubtype(JavaType childType, JavaType parentType,
                                   RType childItemRType) {
        if (childType.equals(parentType)) {
            return true;
        }
        boolean childList = typeUtil.isList(childType);
        boolean parentList = typeUtil.isList(parentType);
        if (childList != parentList) {
            return false;
        }
        JavaType childItem = childList ? typeUtil.getItemType(childType) : childType;
        JavaType parentItem = parentList ? typeUtil.getItemType(parentType) : parentType;
        if (childItem.equals(parentItem)) {
            return true;
        }
        if (!(parentItem instanceof JavaClass<?> parentItemClass)) {
            return false;
        }
        return modelTypeStrictlyExtends(childItemRType, parentItemClass);
    }

    /**
     * All doc references of an attribute — the override-parent chain's (transitively,
     * parent's FIRST) plus its own. Verbatim port of upstream
     * {@code RAttribute.getAllDocReferences} / {@code inheritAnnotationsFromParent},
     * mirroring the #321 {@code LabelProviderGenerator.allLabelAnnotations} recursion
     * over the shared {@code RuleReferenceTraversal.parentAttributeOf} walk (PR #330).
     */
    private static List<com.regnosys.rosetta.ast.regulatory.RDocReference> allDocReferences(
            RAttribute attr) {
        List<com.regnosys.rosetta.ast.regulatory.RDocReference> own = attr.docReferences();
        RAttribute parent = com.regnosys.rosetta.generator.java.function.RuleReferenceTraversal
                .parentAttributeOf(attr);
        if (parent == null) {
            return own;
        }
        List<com.regnosys.rosetta.ast.regulatory.RDocReference> parentAll = allDocReferences(parent);
        if (parentAll.isEmpty()) {
            return own;
        }
        List<com.regnosys.rosetta.ast.regulatory.RDocReference> all =
                new ArrayList<>(parentAll.size() + own.size());
        all.addAll(parentAll);
        all.addAll(own);
        return all;
    }

    /**
     * True iff the (data/choice) model type behind {@code childItemRType} STRICTLY extends
     * the model type named by {@code parentItemClass} — the Rosetta {@code superType()} /
     * {@code choiceSuperType()} chain walk. Ancestor canonical names are built exactly as
     * {@code JavaTypeTranslator.caseDataType} builds them (escaped namespace + simple name)
     * so the comparison can never drift from the translated property types.
     */
    private boolean modelTypeStrictlyExtends(RType childItemRType,
                                              JavaClass<?> parentItemClass) {
        if (childItemRType == null) {
            return false;
        }
        RDataType node = null;
        if (childItemRType instanceof com.regnosys.rosetta.types.RDataTypeRef dref) {
            node = dref.astNode();
        }
        // A choice item never strictly extends anything (choices have no supertype).
        String parentCanonical = parentItemClass.getCanonicalName().withDots();
        java.util.Set<RDataType> seen = new java.util.HashSet<>();
        while (node != null && seen.add(node)) {
            RDataType dataSuper = node.superType().orElse(null);
            if (dataSuper != null) {
                if (parentCanonical.equals(modelTypeCanonicalName(
                        dataSuper.name(), dataSuper.parent()))) {
                    return true;
                }
                node = dataSuper;
                continue;
            }
            RChoice choiceSuper = node.choiceSuperType().orElse(null);
            if (choiceSuper != null
                    && parentCanonical.equals(modelTypeCanonicalName(
                            choiceSuper.name(), choiceSuper.parent()))) {
                return true;
            }
            // A choice supertype terminates the chain (choices have no super).
            return false;
        }
        return false;
    }

    /** The escaped-package canonical name a model type node translates to, or null. */
    private static String modelTypeCanonicalName(String simpleName, Object parentNode) {
        if (parentNode instanceof com.regnosys.rosetta.ast.model.RModel model) {
            return JavaPackageName.escape(
                    com.rosetta.util.DottedPath.splitOnDots(model.namespace()))
                    .getName().withDots() + "." + simpleName;
        }
        return null;
    }

    // -- Meta annotation helpers ------------------------------------------------

    // Delegate to MetaFieldGenerator's shared static utility (D32 refactor)
    private MetaFieldGenerator.MetaKind detectMetaKind(RAttribute attr) {
        return MetaFieldGenerator.detectMetaKind(attr);
    }

    private boolean hasMetaAnnotation(RAttribute attr, String qualifierName) {
        // The override-inheritance union (PR #410) — one law with detectMetaKind:
        // an override inherits the parent chain's [metadata ...] annotations
        // (upstream getRMetaAttributesOfSymbol recursion); identity for the
        // corpus (zero overrides there inherit meta — waves A–D TRUE 100%).
        return MetaFieldGenerator.allMetaAnnotationRefs(attr).stream()
                .anyMatch(ref -> "metadata".equals(ref.annotationName())
                        && ref.qualifierName().map(q -> q.equals(qualifierName)).orElse(false));
    }

    /**
     * THE ONE computation of a declared attribute's POJO property Java type (seat-21 review SF-3,
     * LAW 69): the bare reference type, meta-wrapped into the GENERATED
     * {@code FieldWithMeta<X>} / {@code ReferenceWithMeta<X>} class when the attribute (or its
     * override chain, PR #410) carries {@code [metadata …]}, {@code List<? extends …>}-wrapped when
     * multi. Read by {@code initializeProperties} (the declaration half — every property this
     * interface exposes) AND by {@code NavigationHandler.overrideChainBaseForDeclaredType} (the
     * desired type a callee's declared input demands — the nav-witness half), so the selector
     * compares like with like: the generic runtime {@code FieldWithMeta<T>} that
     * {@code JavaTypeTranslator.toMetaJavaType} returns is never a subtype of the generated class and
     * would silently degrade every meta-annotated input to the leaf.
     */
    public static JavaType declaredPropertyJavaType(RAttribute attr, RType rtype, GeneratorModel gm,
            JavaTypeTranslator tt, JavaTypeUtil tu) {
        JavaType javaType = tt.toJavaReferenceType(rtype);
        MetaFieldGenerator.MetaKind metaKind = MetaFieldGenerator.detectMetaKind(attr);
        if (metaKind == MetaFieldGenerator.MetaKind.FIELD_WITH_META) {
            javaType = wrapFieldWithMeta(javaType);
        } else if (metaKind == MetaFieldGenerator.MetaKind.REFERENCE_WITH_META) {
            javaType = wrapReferenceWithMeta(javaType);
        }
        if (gm.isMulti(attr)) {
            javaType = tu.wrapExtendsIfNotFinal(tu.LIST, javaType);
        }
        return javaType;
    }

    /**
     * Wrap a Java type in FieldWithMeta. The wrapper package is derived from
     * the wrapped type's namespace, NOT the containing type's namespace.
     * Builtins (string, date, etc.) use com.rosetta.model.metafields.
     */
    private static JavaType wrapFieldWithMeta(JavaType valueType) {
        String valueName = valueType.getSimpleName();
        com.rosetta.util.DottedPath ns = resolveWrappedTypeNamespace(valueType);
        return RGeneratedJavaClass.create(
                JavaPackageName.escape(ns.child("metafields")),
                "FieldWithMeta" + valueName,
                com.rosetta.model.lib.RosettaModelObject.class);
    }

    /**
     * Wrap a Java type in ReferenceWithMeta.
     */
    private static JavaType wrapReferenceWithMeta(JavaType valueType) {
        String valueName = valueType.getSimpleName();
        com.rosetta.util.DottedPath ns = resolveWrappedTypeNamespace(valueType);
        return RGeneratedJavaClass.create(
                JavaPackageName.escape(ns.child("metafields")),
                "ReferenceWithMeta" + valueName,
                com.rosetta.model.lib.RosettaModelObject.class);
    }

    /**
     * Resolve the namespace for a wrapped type. For generated types (data/enum),
     * uses the type's package. For builtins (String, Date, etc.), uses
     * com.rosetta.model (matching upstream LIB_NAMESPACE).
     */
    private static com.rosetta.util.DottedPath resolveWrappedTypeNamespace(JavaType valueType) {
        if (valueType instanceof RGeneratedJavaClass<?> gen) {
            return com.rosetta.util.DottedPath.splitOnDots(gen.getPackageName().toString());
        }
        // Builtins: com.rosetta.model
        return com.rosetta.util.DottedPath.splitOnDots("com.rosetta.model");
    }
}
