package com.regnosys.rosetta.generator.java.ir;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.regnosys.rosetta.generator.GenerationException;
import com.regnosys.rosetta.ir.adapter.IRTypeNode;
import com.regnosys.rosetta.ir.core.IRAnnotationUse;
import com.regnosys.rosetta.ir.core.IRDocReference;
import com.regnosys.rosetta.ir.core.IREffectiveBase;
import com.regnosys.rosetta.ir.core.IRField;
import com.regnosys.rosetta.ir.core.IRKind;
import com.regnosys.rosetta.ir.core.IRType;

/**
 * THE POJO PROPERTY SURFACE, DERIVED FROM THE IR ALONE (v3.3 seat 8, PR #644 - the property gate): the effective
 * property surface of a STRUCT or CHOICE {@link IRTypeNode}, computed from that node, its ancestors through
 * {@link IRTypeIndex} and NOTHING ELSE. No AST node, no {@code GeneratorModel}, no {@code JavaTypeTranslator} is read
 * here - the one shared producer is {@link IRJavaTypeNames#of} (PR #643), itself a pure function of the IR facts.
 *
 * <p>THE LAW is {@code RJavaPojoInterface}'s, reproduced decision for decision (the sites are the fork's own, at the
 * head PR #644 branches from):
 * <ul>
 *   <li><b>the two maps</b> - a {@code LinkedHashMap} each ({@code :216}, {@code :219}, {@code :221}); {@code all}
 *       SEEDS as a copy of the parent's finalized {@code all}, position for position, and {@code own} starts empty;
 *       the parent is the {@code extends} base - the STRUCT one when the base reference's kind is STRUCT, the CHOICE
 *       one when it is CHOICE ({@code getSuperPojo}, {@code :155-175}).</li>
 *   <li><b>Case 0</b> ({@code :449-452}) - a redeclared attribute whose Java type EQUALS the parent property's AND
 *       whose requiredness equals it produces NO property, in EITHER map: its javadoc, its doc references and its
 *       meta are DROPPED and the parent's property continues to represent it.</li>
 *   <li><b>the re-put order</b> ({@code :428-430}, {@code :464-465}) - a SPECIALIZED property re-{@code put}s on an
 *       existing key and therefore keeps the ANCESTOR'S iteration position in {@code all}; a genuinely new key
 *       appends. {@code own} is plain declaration order with the synthetic {@code meta} LAST.</li>
 *   <li><b>the compatibility names</b> ({@code addProperty} {@code :434-466} over {@code incompatiblePropertyName}
 *       {@code :477-494}) - the GETTER is renamed iff the specialized type is not a POJO subtype of the parent's
 *       ({@code isPojoSubtype}, {@code :511-530}, whose third arm walks the child item's Rosetta supertype chain
 *       through this index); the SETTER is renamed iff the two types' ERASURES are equal (Java cannot overload -
 *       the requiredness-only case), and otherwise OVERLOADS the parent's setter name.</li>
 *   <li><b>the property Java type</b> ({@code declaredPropertyJavaType}, {@code :638-651}) - the bare reference type
 *       from {@link IRJavaTypeNames#of}, meta-wrapped into the GENERATED {@code FieldWithMeta*} /
 *       {@code ReferenceWithMeta*} class when the attribute OR ITS OVERRIDE CHAIN carries {@code [metadata …]}, and
 *       list-wrapped when the field is multi.</li>
 *   <li><b>the per-property JAVADOC</b> ({@code :287-288} for an attribute, {@code :379-380} for a choice option) -
 *       the block {@code ModelObjectGenerator:641-644} writes above the getter, rendered from the declaration's own
 *       definition and its doc-reference UNION by {@link #javadocOf}, which reproduces
 *       {@code ModelGeneratorUtil.javadoc(…, pojoStyle = true)} rather than calling it. It rides the Case 0 /
 *       specialization law with its inputs - Case 0 DROPS the child's block, a specialization REPLACES the parent's
 *       ({@code JavaPojoProperty.specialize}, {@code :104-114}) - and it carries NO {@code @version} line, because
 *       both call sites pass a null version.</li>
 *   <li><b>the synthetic {@code meta}</b> ({@code :316-322} on a type, {@code :257-259} on a choice) - added LAST,
 *       only when the declaration carries {@code [metadata key]}; its type is {@code MetaAndTemplateFields} iff
 *       {@code [metadata template]} is carried too, and a CHOICE only ever takes {@code MetaFields}.</li>
 *   <li><b>the inherited CHOICE options</b> ({@code :343-389}) - a choice's options are ITS properties, named by the
 *       option's type name, never list-wrapped, {@code isRequired = false} by the literal at {@code :386-387}, with
 *       their annotations read DIRECTLY and no override union; a {@code type} extending the choice inherits them
 *       through the ordinary parent seed.</li>
 *   <li><b>the two unions over an override</b> - the doc references parent-chain FIRST then own
 *       ({@code allDocReferences}, {@code :539-556}) and the {@code [metadata …]} annotations likewise
 *       ({@code MetaFieldGenerator.allMetaAnnotationRefs}, {@code :109-118}), both through the same walk
 *       {@code RuleReferenceTraversal.parentAttributeOf} performs ({@code :415-432}): only when the field is declared
 *       {@code override} AND the enclosing declaration is a {@code type}, and only up the STRUCT supertype chain.</li>
 *   <li><b>the two NAMED refusals</b> - a {@code choice} with no option ({@code checkChoice}, {@code :93-98}) and a
 *       top-level {@code choice} carrying {@code [metadata template]} ({@code :246-256}).</li>
 * </ul>
 *
 * <p><b>THE JAVA TYPE RENDERING.</b> A property's Java type is ONE STRING that both halves of the reconcile produce
 * independently, and it is the old generator's OWN spelling - {@code JavaType.toString()}: the CANONICAL dotted name
 * for a class ({@code cdm.base.datetime.AdjustableDate}, {@code java.lang.String}) and
 * {@code SimpleName<argument, …>} for a parameterized type ({@code JavaClass:203}, {@code JavaParameterizedType:224}).
 * So a multi property reads {@code List<cdm.base.datetime.AdjustableDate>}, and a meta-wrapped one reads the WRAPPER's
 * canonical name ({@code cdm.base.datetime.metafields.FieldWithMetaAdjustableDate}), never the value's. The list wrap
 * is INVARIANT and not {@code List<? extends X>}: {@code wrapExtendsIfNotFinal} ({@code JavaTypeUtil:191-198}) takes
 * its {@code wrapExtends} arm only for a {@code JavaPojoInterface} item, and {@code JavaTypeTranslator} renders every
 * model type as a plain {@code RGeneratedJavaClass} ({@code :101-121}, {@code :142-155}), never as a pojo interface.
 */
final class IRPropertyModel {

    /** The two canonical meta-field classes the synthetic {@code meta} property takes ({@code JavaTypeUtil:73-79}). */
    private static final String META_FIELDS = "com.rosetta.model.metafields.MetaFields";
    private static final String META_AND_TEMPLATE_FIELDS = "com.rosetta.model.metafields.MetaAndTemplateFields";

    /** The namespace a wrapper over a BUILTIN value type lives in ({@code resolveWrappedTypeNamespace}, {@code :684-690}). */
    private static final String LIB_METAFIELDS = "com.rosetta.model.metafields";

    /** The alias-chain walk's bound, shared with the parser's own: a cycle refuses rather than hangs. */
    private static final int MAX_ANCESTRY_DEPTH = 100;

    private final List<IRProperty> ownProperties;
    private final List<IRProperty> allProperties;

    private IRPropertyModel(List<IRProperty> ownProperties, List<IRProperty> allProperties) {
        this.ownProperties = List.copyOf(ownProperties);
        this.allProperties = List.copyOf(allProperties);
    }

    /**
     * The property surface of a STRUCT or CHOICE node.
     *
     * @throws GenerationException for a node of any other kind, for a {@code choice} with no option, for a top-level
     *     {@code choice} carrying {@code [metadata template]}, for a supertype chain that cycles, and for every
     *     refusal {@link IRTypeIndex#parent(IRType)} or {@link IRJavaTypeNames#of} raises on the way
     */
    static IRPropertyModel of(IRTypeNode type, IRTypeIndex index) {
        return of(type, index, new LinkedHashSet<>());
    }

    /**
     * TEST SEAM (the lying-IR witnesses of {@code IRPropertyGateTest}, lanes P01-P07): a model stating EXACTLY the two
     * surfaces given, with no law applied - so a witness can hand the reconciler a model with ONE fact altered.
     * Package-private; no production caller.
     */
    static IRPropertyModel stating(List<IRProperty> ownProperties, List<IRProperty> allProperties) {
        return new IRPropertyModel(ownProperties, allProperties);
    }

    /**
     * WHETHER THE DECLARATION ITSELF CARRIES {@code [metadata <qualifier>]}
     * ({@code RJavaPojoInterface.hasTypeMetaAnnotation}, {@code :196-204}) - read off the node's OWN annotations,
     * never the chain's. The Builder's own use delegates here; the DATA-TYPE EMITTER (v3.3 seat 9, PR #645
     * commit 4) reads it for the interface list, where {@code key} adds {@code GlobalKey} and {@code template} adds
     * {@code Templatable}. Additive VISIBILITY at this commit: the law and every byte it decides are unchanged.
     */
    static boolean hasTypeMeta(IRTypeNode type, String qualifier) {
        return Builder.hasMeta(type.annotations(), qualifier);
    }

    /** The LAW's order: declaration order, a specialized key in place, the synthetic {@code meta} LAST. */
    List<IRProperty> ownProperties() {
        return ownProperties;
    }

    /**
     * The parent's finalized {@code allProperties} FIRST, position for position; a specialized key re-put IN THE
     * ANCESTOR'S POSITION; a new key appended.
     */
    List<IRProperty> allProperties() {
        return allProperties;
    }

    /**
     * ONE property of the effective surface, every fact the emitter writes and the gate asserts.
     *
     * @param name                       the property's own name - the attribute's, the choice option's TYPE name, or
     *                                   the synthetic {@code meta}
     * @param javaType                   the rendered Java type (see the class javadoc's rendering law)
     * @param required                   the declared lower bound is above zero; a choice option is never required
     * @param multi                      the Java type is list-wrapped
     * @param getterCompatibilityName    the name the getter is built from - the parent's when the specialization is a
     *                                   POJO subtype, else the {@code OverriddenAs…} name
     * @param setterCompatibilityName    the name the setter is built from - the {@code OverriddenAs…} name when the
     *                                   erasures are equal, else the parent's (the setter OVERLOADS)
     * @param getterOverridesParentGetter the specialized getter carries the parent's name
     * @param compatibleTypeWithParent   {@code JavaPojoProperty.isCompatibleTypeWithParent} - true when there is no
     *                                   parent property
     * @param sameTypeAsParent           {@code JavaPojoProperty.isSameTypeAsParent} - true when there is no parent
     * @param parentChainDepth           the number of {@code parentProperty} hops - 0 for an unspecialized property
     * @param metaValueType              the BARE value type before the meta wrap; empty when the property is unwrapped
     * @param hasLocation                {@code [metadata location]} is in force (after the override union)
     * @param attributeMetaTypes         {@code SCOPED_REFERENCE} for {@code [metadata address]} then
     *                                   {@code SCOPED_KEY} for {@code [metadata location]}, in that order
     * @param attributeMeta              {@code GLOBAL_KEY_FIELD} for {@code [metadata id]}, else empty - the list form
     *                                   of the old generator's nullable single {@code AttributeMeta}
     * @param docReferences              the doc references in force: the override chain's FIRST, then the field's own
     * @param synthetic                  this is the synthetic {@code meta} property, not a declared member
     * @param inheritedChoiceOption      this property was produced by a CHOICE's option loop
     * @param definition                 the EFFECTIVE {@code <"…">} of the declaration behind the property - the
     *                                   declaring attribute's, or the choice option's - under the Case 0 /
     *                                   specialization law: Case 0 DROPS the child's and the parent's property (with
     *                                   the parent's definition) continues to represent it, while a specialization
     *                                   REPLACES it. Empty when the declaration wrote none; {@code Optional.of("")} is
     *                                   the explicit {@code <"">} the renderer distinguishes from absence
     * @param javadoc                    the RENDERED javadoc block the POJO getter carries
     *                                   ({@code ModelObjectGenerator:641-644} writes {@code prop.getJavadoc()}),
     *                                   reproduced from {@code definition} and {@code docReferences} ALONE by
     *                                   {@link IRPropertyModel#javadocOf}. Empty is the old generator's {@code null}:
     *                                   the synthetic {@code meta}, and every property with neither a definition nor
     *                                   a doc reference. It rides the Case 0 / specialization law with its inputs,
     *                                   because it is computed where the property is built
     * @param itemIsRosettaModelObject   whether this property's ITEM type is a {@code RosettaModelObject} - the fact
     *                                   {@code JavaTypeUtil.isRosettaModelObject(prop.getType())} answers on the old
     *                                   generator's own property ({@code ModelObjectGenerator.isModelObj},
     *                                   {@code :1846-1848}), derived here from {@link IRType#kind()} on the
     *                                   declaration behind the ITEM alone (v3.3 seat 9, PR #645 commit 5 - the gate
     *                                   commit). It decides the {@code List<? extends X>} arm of the getter return
     *                                   type ({@code :1917-1925}), the {@code java.util.Objects} import gate
     *                                   ({@code :180-184}), the {@code java.util.function.Consumer} import gate
     *                                   ({@code :273-286}) and, later, the {@code processRosetta} /
     *                                   {@code processBasic} split ({@code ModelObjectBoilerplate:270-292})
     * @param parentChain                the ANCESTORS of a specialized property, NEAREST FIRST - one
     *                                   {@link IRParentLink} per {@code parentProperty} hop, so its SIZE is
     *                                   {@code parentChainDepth}; EMPTY at depth 0. It is what every compat arm
     *                                   {@code PojoCompatEmitter} writes is shaped from ({@code MOG:621-623}) and
     *                                   what the specialization chain's own imports are decided from
     *                                   ({@code :288-330}) (v3.3 seat 9, PR #645 commit 5)
     * @param itemIsEnum                 whether this property's ITEM type is a plain Java {@code enum} - the fact
     *                                   {@code getItemType(prop.getType()) instanceof RJavaEnum} answers on the old
     *                                   generator's own property ({@code ModelObjectBoilerplate:205-206}, through
     *                                   the seam {@code ModelObjectGenerator.itemIsEnum} that site delegates to),
     *                                   derived here from {@link IRType#kind()} on the declaration behind the ITEM
     *                                   alone. It decides the {@code hashCode} boilerplate's ENUM arm - a
     *                                   class-name hash instead of the value's own (v3.3 seat 9, PR #645 commit 8)
     * @param metaValueIsRosettaModelObject whether the BARE value type behind a meta wrap is a
     *                                   {@code RosettaModelObject}; EMPTY when the property carries no meta value
     *                                   type at all, which is the old generator's {@code getMetaValueType() ==
     *                                   null} and a different statement from {@code false}. It decides the
     *                                   {@code hasData} presence-only arms ({@code ModelObjectGenerator:1325-1327}
     *                                   and {@code :1341-1342}) and the {@code .toBuilder()} suffix of the list
     *                                   meta-value setter ({@code :1585}) (v3.3 seat 9, PR #645 commit 8)
     * @param runeName                   the property's RUNE name - the second of the five name slots
     *                                   {@code RJavaPojoInterface.addProperty} fills ({@code :440-443}), kept
     *                                   through a specialization by {@code JavaPojoProperty.specialize}
     *                                   ({@code :105-113}). It is the ATTRIBUTE half of the pruning-config key
     *                                   {@code GeneratorModel.isPruningDisabled} reads
     *                                   ({@code <typeFqn>#<attrRuneName>}, {@code :116-119}), which section 12's
     *                                   {@code prune()} and {@code hasData()} arms branch on. The old generator
     *                                   fills every slot from the declaration's own name, so on this fork it is
     *                                   always equal to {@link #name} - the emitter does not ASSUME that: the
     *                                   fact is carried and RECONCILED per property against
     *                                   {@code JavaPojoProperty.getRuneName()}, so the day the two part the gate
     *                                   says so rather than a pruning arm silently flipping (v3.3 seat 9,
     *                                   PR #645 commit 9)
     */
    record IRProperty(String name, String javaType, boolean required, boolean multi,
                      String getterCompatibilityName, String setterCompatibilityName, boolean getterOverridesParentGetter,
                      boolean compatibleTypeWithParent, boolean sameTypeAsParent, int parentChainDepth,
                      Optional<String> metaValueType, boolean hasLocation, List<String> attributeMetaTypes,
                      List<String> attributeMeta, List<IRDocReference> docReferences, boolean synthetic,
                      boolean inheritedChoiceOption, Optional<String> definition, Optional<String> javadoc,
                      boolean itemIsRosettaModelObject, List<IRParentLink> parentChain,
                      boolean itemIsEnum, Optional<Boolean> metaValueIsRosettaModelObject,
                      String runeName) {

        IRProperty {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(runeName, "runeName");
            Objects.requireNonNull(javaType, "javaType");
            Objects.requireNonNull(getterCompatibilityName, "getterCompatibilityName");
            Objects.requireNonNull(setterCompatibilityName, "setterCompatibilityName");
            Objects.requireNonNull(metaValueType, "metaValueType");
            Objects.requireNonNull(definition, "definition");
            Objects.requireNonNull(javadoc, "javadoc");
            Objects.requireNonNull(metaValueIsRosettaModelObject, "metaValueIsRosettaModelObject");
            attributeMetaTypes = List.copyOf(attributeMetaTypes);
            attributeMeta = List.copyOf(attributeMeta);
            docReferences = List.copyOf(docReferences);
            // NO size-vs-depth precondition here, deliberately: {@code parentChainDepth} and {@code parentChain} are
            // TWO facts, each reconciled against the old generator's own answer in its own family. A precondition
            // that tied them together would turn a chain the IR got wrong into a THROW before the reconciler could
            // NAME which of the two disagrees - and a lane that drops a link must read RED on the family, not on a
            // constructor (v3.3 seat 9, PR #645 commit 5, lane P10).
            parentChain = List.copyOf(parentChain);
        }

        /** TEST SEAM (lane P02 / P07): this property with its Java type restated. No production caller. */
        IRProperty withJavaType(String restated) {
            return new IRProperty(name, restated, required, multi, getterCompatibilityName, setterCompatibilityName,
                    getterOverridesParentGetter, compatibleTypeWithParent, sameTypeAsParent, parentChainDepth,
                    metaValueType, hasLocation, attributeMetaTypes, attributeMeta, docReferences, synthetic,
                    inheritedChoiceOption, definition, javadoc,
                    itemIsRosettaModelObject, parentChain, itemIsEnum, metaValueIsRosettaModelObject, runeName);
        }

        /** TEST SEAM (lane P03's sibling): this property with its requiredness restated. No production caller. */
        IRProperty withRequired(boolean restated) {
            return new IRProperty(name, javaType, restated, multi, getterCompatibilityName, setterCompatibilityName,
                    getterOverridesParentGetter, compatibleTypeWithParent, sameTypeAsParent, parentChainDepth,
                    metaValueType, hasLocation, attributeMetaTypes, attributeMeta, docReferences, synthetic,
                    inheritedChoiceOption, definition, javadoc,
                    itemIsRosettaModelObject, parentChain, itemIsEnum, metaValueIsRosettaModelObject, runeName);
        }

        /** TEST SEAM (lane P04): this property with its setter compatibility name restated. No production caller. */
        IRProperty withSetterCompatibilityName(String restated) {
            return new IRProperty(name, javaType, required, multi, getterCompatibilityName, restated,
                    getterOverridesParentGetter, compatibleTypeWithParent, sameTypeAsParent, parentChainDepth,
                    metaValueType, hasLocation, attributeMetaTypes, attributeMeta, docReferences, synthetic,
                    inheritedChoiceOption, definition, javadoc,
                    itemIsRosettaModelObject, parentChain, itemIsEnum, metaValueIsRosettaModelObject, runeName);
        }

        /** TEST SEAM (lane P05): this property with its bare meta value type restated. No production caller. */
        IRProperty withMetaValueType(Optional<String> restated) {
            return new IRProperty(name, javaType, required, multi, getterCompatibilityName, setterCompatibilityName,
                    getterOverridesParentGetter, compatibleTypeWithParent, sameTypeAsParent, parentChainDepth,
                    restated, hasLocation, attributeMetaTypes, attributeMeta, docReferences, synthetic,
                    inheritedChoiceOption, definition, javadoc,
                    itemIsRosettaModelObject, parentChain, itemIsEnum, metaValueIsRosettaModelObject, runeName);
        }

        /** TEST SEAM (lane P06): this property with its doc-reference union restated. No production caller. */
        IRProperty withDocReferences(List<IRDocReference> restated) {
            return new IRProperty(name, javaType, required, multi, getterCompatibilityName, setterCompatibilityName,
                    getterOverridesParentGetter, compatibleTypeWithParent, sameTypeAsParent, parentChainDepth,
                    metaValueType, hasLocation, attributeMetaTypes, attributeMeta, restated, synthetic,
                    inheritedChoiceOption, definition, javadoc,
                    itemIsRosettaModelObject, parentChain, itemIsEnum, metaValueIsRosettaModelObject, runeName);
        }

        /** TEST SEAM (lane P08): this property with its EFFECTIVE definition restated. No production caller. */
        IRProperty withDefinition(Optional<String> restated) {
            return new IRProperty(name, javaType, required, multi, getterCompatibilityName, setterCompatibilityName,
                    getterOverridesParentGetter, compatibleTypeWithParent, sameTypeAsParent, parentChainDepth,
                    metaValueType, hasLocation, attributeMetaTypes, attributeMeta, docReferences, synthetic,
                    inheritedChoiceOption, restated, javadoc,
                    itemIsRosettaModelObject, parentChain, itemIsEnum, metaValueIsRosettaModelObject, runeName);
        }

        /**
         * TEST SEAM (v3.3 seat 9, PR #645 commit 5, lane P09): this property with its item-kind verdict restated.
         * No production caller.
         */
        IRProperty withItemIsRosettaModelObject(boolean restated) {
            return new IRProperty(name, javaType, required, multi, getterCompatibilityName, setterCompatibilityName,
                    getterOverridesParentGetter, compatibleTypeWithParent, sameTypeAsParent, parentChainDepth,
                    metaValueType, hasLocation, attributeMetaTypes, attributeMeta, docReferences, synthetic,
                    inheritedChoiceOption, definition, javadoc,
                    restated, parentChain, itemIsEnum, metaValueIsRosettaModelObject, runeName);
        }

        /**
         * TEST SEAM (v3.3 seat 9, PR #645 commit 5, lane P10): this property with its ancestor chain restated - the
         * DEPTH deliberately left alone, so the two facts can go red apart. No production caller.
         */
        IRProperty withParentChain(List<IRParentLink> restated) {
            return new IRProperty(name, javaType, required, multi, getterCompatibilityName, setterCompatibilityName,
                    getterOverridesParentGetter, compatibleTypeWithParent, sameTypeAsParent, parentChainDepth,
                    metaValueType, hasLocation, attributeMetaTypes, attributeMeta, docReferences, synthetic,
                    inheritedChoiceOption, definition, javadoc,
                    itemIsRosettaModelObject, restated, itemIsEnum, metaValueIsRosettaModelObject, runeName);
        }

        /**
         * TEST SEAM (v3.3 seat 9, PR #645 commit 9, lane P13): this property with its RUNE name restated - the
         * {@link #name} deliberately left alone, so the two can go red apart. No production caller.
         */
        IRProperty withRuneName(String restated) {
            return new IRProperty(name, javaType, required, multi, getterCompatibilityName, setterCompatibilityName,
                    getterOverridesParentGetter, compatibleTypeWithParent, sameTypeAsParent, parentChainDepth,
                    metaValueType, hasLocation, attributeMetaTypes, attributeMeta, docReferences, synthetic,
                    inheritedChoiceOption, definition, javadoc,
                    itemIsRosettaModelObject, parentChain, itemIsEnum, metaValueIsRosettaModelObject, restated);
        }

        /** TEST SEAM (lane P09 of PR #644): this property with its RENDERED javadoc restated. No production caller. */
        IRProperty withJavadoc(Optional<String> restated) {
            return new IRProperty(name, javaType, required, multi, getterCompatibilityName, setterCompatibilityName,
                    getterOverridesParentGetter, compatibleTypeWithParent, sameTypeAsParent, parentChainDepth,
                    metaValueType, hasLocation, attributeMetaTypes, attributeMeta, docReferences, synthetic,
                    inheritedChoiceOption, definition, restated,
                    itemIsRosettaModelObject, parentChain, itemIsEnum, metaValueIsRosettaModelObject, runeName);
        }
    }

    /**
     * ONE ANCESTOR of a specialized property, in the shape every compat arm is written from (v3.3 seat 9, PR #645
     * commit 5 - the gate commit). The old generator reaches the same rungs by walking
     * {@code JavaPojoProperty.getParentProperty()} ({@code PojoCompatEmitter.appendImplDerivedGetters},
     * {@code appendBuilderDerivedGetters} and the ancestor setter walk; the import arm at
     * {@code ModelObjectGenerator:288-330} walks the very same link), and reads exactly these facts off each rung:
     * its rendered Java TYPE (the compat member's signature and the coercion's target), whether that type is LIST
     * shaped (the {@code Collections.singletonList} / {@code MapperC.of(x).get()} cardinality arms and the bare
     * {@code java.util.List} import), whether the ancestor was REQUIRED, and the BARE value type behind a meta wrap
     * ({@code addTypeImports}, {@code :1775-1808}, imports the ancestor's meta value type as well as its item).
     *
     * <p>The fourth component is the one this record carries BEYOND the seat contract's three: without it, the
     * chain's own import arm would silently drop a meta-wrapped ancestor's value-type import. It is reconciled in
     * the same family row as the other three, so it can never be wrong quietly.
     *
     * <p><b>THE SIX FACTS PR #645 COMMIT 10 ADDED</b>, each of them the ANCESTOR PROPERTY'S OWN - read off the
     * parent {@link IRProperty} where the rung is built, which is where the old generator's
     * {@code getParentProperty()} link points and therefore the same object {@code PojoCompatEmitter} reads them
     * from. Without them the compat walk could not be written at all, and commit 9 said so by REFUSING every
     * carrier by name:
     * <ul>
     *   <li>{@code itemIsRosettaModelObject} - the rung's own item kind; it decides the rung's
     *       {@code List<? extends X>} getter arm ({@code interfaceGetterType} / {@code builderGetterType}), its
     *       {@code X.XBuilder} single type, the {@code .toBuilder()} suffix of the coerced result
     *       ({@code PojoCompatEmitter:212}, {@code :234}, {@code :454}, {@code :814}) and the model-downcast arm
     *       of the coercion table ({@code :626-627});</li>
     *   <li>{@code itemIsEnum} - the rung's own item kind again, the half {@code itemIsRosettaModelObject}
     *       cannot express: an {@code enum} item is neither a model object nor a library class, and
     *       {@link IRJavaTypes} needs to know which of the two a name is before it can build a {@code JavaType}
     *       for it at all;</li>
     *   <li>{@code metaValueIsRosettaModelObject} - the kind of the rung's BARE value type, empty when the rung
     *       carries none; it is the {@code metaValueType} beside it as a KIND, and the meta wrap / unwrap arms
     *       ({@code :639-657}) are typed from it;</li>
     *   <li>{@code getterOverridesParentGetter} - THE fact that closes commit 8's honest over-refusal. The impl
     *       and builder getter walks skip a rung whose getter keeps the parent's name ({@code :175},
     *       {@code :210}), and the flag they read is the CURSOR's - the main property's for the first hop, then
     *       each rung's own for the hops after it. Without the rung's own verdict a chain of depth two or more
     *       could not be decided and was refused;</li>
     *   <li>{@code getterCompatibilityName} / {@code setterCompatibilityName} - the two names every compat
     *       member of that rung is called by ({@code parent.getOperationName(GET | GET_OR_CREATE | SET |
     *       SET_VALUE | ADD | ADD_VALUE)}, {@code JavaPojoProperty:120-133}). A compat member exists precisely
     *       BECAUSE the specialized property could not keep the ancestor's accessor name, so the ancestor's own
     *       names are not derivable from the specialized property's - they must be carried.</li>
     * </ul>
     *
     * @param javaType      the ancestor property's rendered Java type, in the old generator's own spelling
     * @param multi         the ancestor's type is list-wrapped
     * @param required      the ancestor's declared lower bound is above zero
     * @param metaValueType the BARE value type before the ancestor's meta wrap; empty when it is unwrapped
     * @param itemIsRosettaModelObject      the ancestor's ITEM is a {@code RosettaModelObject}
     * @param itemIsEnum                    the ancestor's ITEM is a plain Java {@code enum}
     * @param metaValueIsRosettaModelObject the kind of the ancestor's bare value type; EMPTY when it has none
     * @param getterOverridesParentGetter   the ancestor's own getter carries ITS parent's name
     * @param getterCompatibilityName       the name the ancestor's getter is built from
     * @param setterCompatibilityName       the name the ancestor's setter is built from
     */
    record IRParentLink(String javaType, boolean multi, boolean required, Optional<String> metaValueType,
                        boolean itemIsRosettaModelObject, boolean itemIsEnum,
                        Optional<Boolean> metaValueIsRosettaModelObject, boolean getterOverridesParentGetter,
                        String getterCompatibilityName, String setterCompatibilityName) {

        IRParentLink {
            Objects.requireNonNull(javaType, "javaType");
            Objects.requireNonNull(metaValueType, "metaValueType");
            Objects.requireNonNull(metaValueIsRosettaModelObject, "metaValueIsRosettaModelObject");
            Objects.requireNonNull(getterCompatibilityName, "getterCompatibilityName");
            Objects.requireNonNull(setterCompatibilityName, "setterCompatibilityName");
        }

        /**
         * The ONE rendering both halves of the reconcile produce independently - the row the fact compares. It
         * GREW by six fields at PR #645 commit 10 and the fact it belongs to did not: the family is still
         * {@code property.<name>.parentChain.types}, ONE assertion per property, so the reconciler's
         * {@code factsAsserted} count does not move.
         */
        String row() {
            return javaType + "|" + multi + "|" + required + "|" + metaValueType.orElse("")
                    + "|" + itemIsRosettaModelObject + "|" + itemIsEnum
                    + "|" + metaValueIsRosettaModelObject.map(String::valueOf).orElse("")
                    + "|" + getterOverridesParentGetter
                    + "|" + getterCompatibilityName + "|" + setterCompatibilityName;
        }
    }

    // ------------------------------------------------------------------------------------------------- the law

    private static IRPropertyModel of(IRTypeNode type, IRTypeIndex index, Set<String> ancestry) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(index, "index");
        if (!ancestry.add(type.name())) {
            throw new GenerationException("IR property model: the supertype chain of '" + type.name() + "' CYCLES"
                    + " through " + ancestry + " - the surface is refused rather than guessed", null, null);
        }
        if (ancestry.size() > MAX_ANCESTRY_DEPTH) {
            throw new GenerationException("IR property model: the supertype chain of '" + type.name() + "' is deeper"
                    + " than " + MAX_ANCESTRY_DEPTH + " - the surface is refused", null, null);
        }
        if (type.kind() == IRKind.CHOICE) {
            return new Builder(index, ancestry).buildChoice(type);
        }
        if (type.kind() != IRKind.STRUCT) {
            throw new GenerationException("IR property model: a node of kind " + type.kind() + " has no POJO property"
                    + " surface ('" + type.name() + "')", null, null);
        }
        return new Builder(index, ancestry).buildStruct(type);
    }

    /** The mutable half of the law: the two {@code LinkedHashMap}s and the one {@code addProperty} every path goes through. */
    private static final class Builder {

        private final IRTypeIndex index;
        private final Set<String> ancestry;
        private final Map<String, IRProperty> own = new LinkedHashMap<>();
        private final Map<String, IRProperty> all = new LinkedHashMap<>();

        Builder(IRTypeIndex index, Set<String> ancestry) {
            this.index = index;
            this.ancestry = ancestry;
        }

        IRPropertyModel buildStruct(IRTypeNode type) {
            Optional<IRType> base = type.baseType();
            if (base.isPresent()) {
                // the seed: the parent's FINALIZED allProperties, in the parent's order (RJavaPojoInterface:214-221)
                for (IRProperty inherited : of(index.parent(base.get()), index, ancestry).allProperties()) {
                    all.put(inherited.name(), inherited);
                }
            }
            for (IRField field : type.fields()) {
                addAttribute(type, field);
            }
            if (hasTypeMeta(type, "key")) {
                addSyntheticMeta(hasTypeMeta(type, "template") ? META_AND_TEMPLATE_FIELDS : META_FIELDS);
            }
            return new IRPropertyModel(new ArrayList<>(own.values()), new ArrayList<>(all.values()));
        }

        IRPropertyModel buildChoice(IRTypeNode type) {
            // the ctor's own precondition, before any state is established (RJavaPojoInterface:93-98)
            if (type.fields().isEmpty()) {
                throw new GenerationException("IR property model: the choice '" + type.name() + "' declares no option"
                        + " - a choice type must have at least 1 option", null, null);
            }
            for (IRField option : type.fields()) {
                addOption(option);
            }
            // the guard fires AFTER the options and BEFORE the synthetic meta, exactly as the old generator's does
            if (hasTypeMeta(type, "template")) {
                throw new GenerationException("IR property model: the top-level choice '" + type.name() + "' carries"
                        + " [metadata template] - the choice path's template scaffolding was never mirrored and the"
                        + " old generator refuses it rather than silently degrading", null, null);
            }
            if (hasTypeMeta(type, "key")) {
                addSyntheticMeta(META_FIELDS);   // a choice only ever takes MetaFields (RJavaPojoInterface:257-259)
            }
            return new IRPropertyModel(new ArrayList<>(own.values()), new ArrayList<>(all.values()));
        }

        private void addAttribute(IRTypeNode enclosing, IRField field) {
            List<IRAnnotationUse> annotations = unionedAnnotations(enclosing, field, 0);
            MetaKind metaKind = metaKindOf(annotations);
            String bare = IRJavaTypeNames.of(field.type(), field.typeArguments());
            String item = metaKind == MetaKind.NONE ? bare : wrapper(metaKind, field.type(), bare);
            boolean multi = isMulti(field);
            List<String> attributeMetaTypes = new ArrayList<>();
            if (hasMeta(annotations, "address")) {
                attributeMetaTypes.add("SCOPED_REFERENCE");
            }
            boolean hasLocation = hasMeta(annotations, "location");
            if (hasLocation) {
                attributeMetaTypes.add("SCOPED_KEY");
            }
            addProperty(field.name(), multi ? list(item) : item,
                    metaKind == MetaKind.NONE ? Optional.empty() : Optional.of(bare),
                    hasLocation, attributeMetaTypes,
                    hasMeta(annotations, "id") ? List.of("GLOBAL_KEY_FIELD") : List.<String>of(),
                    unionedDocReferences(enclosing, field, 0), isRequired(field), multi,
                    // the bare item reference feeds the model-level subtype check - only meaningful UNWRAPPED, since a
                    // wrapped property's Java item is the FieldWithMeta/ReferenceWithMeta class (RJavaPojoInterface:309-311)
                    metaKind == MetaKind.NONE ? field.type() : null, false, false,
                    // the attribute's OWN definition - never the override chain's: the javadoc's definition line is
                    // `attr.definition()` at RJavaPojoInterface:287-288, and only the DOC REFERENCES take the union
                    field.definition(),
                    // the ITEM's kind, read off the field's own type reference - the fact gap 1 of commit 4 named
                    itemIsRosettaModelObject(metaKind, field.type()),
                    // the two facts sections 11-13 read (v3.3 seat 9, PR #645 commit 8), off the SAME reference
                    itemIsEnum(metaKind, field.type()), metaValueIsRosettaModelObject(metaKind, field.type()));
        }

        private void addOption(IRField option) {
            // a choice option's annotations are read DIRECTLY: an option is no RAttribute and has no override chain
            List<IRAnnotationUse> annotations = option.annotations();
            MetaKind metaKind = metaKindOf(annotations);
            String bare = IRJavaTypeNames.of(option.type(), option.typeArguments());
            List<String> attributeMetaTypes = new ArrayList<>();
            if (hasMeta(annotations, "address")) {
                attributeMetaTypes.add("SCOPED_REFERENCE");
            }
            boolean hasLocation = hasMeta(annotations, "location");
            if (hasLocation) {
                attributeMetaTypes.add("SCOPED_KEY");
            }
            addProperty(option.name(), metaKind == MetaKind.NONE ? bare : wrapper(metaKind, option.type(), bare),
                    metaKind == MetaKind.NONE ? Optional.empty() : Optional.of(bare),
                    hasLocation, attributeMetaTypes, List.of(), List.of(),
                    false,   // the literal isRequired = false of RJavaPojoInterface:386-387 - every option is 0..1
                    false,   // and never list-wrapped
                    metaKind == MetaKind.NONE ? option.type() : null, false, true,
                    // an option's javadoc is `option.definition()` with NO doc references at all
                    // (RJavaPojoInterface:379-380 passes List.of()), which the empty list above already states
                    option.definition(),
                    itemIsRosettaModelObject(metaKind, option.type()),
                    itemIsEnum(metaKind, option.type()), metaValueIsRosettaModelObject(metaKind, option.type()));
        }

        private void addSyntheticMeta(String metaFieldsType) {
            // the synthetic meta is added with a NULL javadoc (RJavaPojoInterface:258, :321) - no definition, no
            // doc reference, and so no block at all. Its item is MetaFields or MetaAndTemplateFields - both
            // GENERATED classes of com.rosetta.model.metafields that implement RosettaModelObject, so the item-kind
            // fact is TRUE by the class the synthetic property is given, not by any declaration's kind.
            addProperty("meta", metaFieldsType, Optional.empty(), false, List.of(), List.of(), List.of(),
                    false, false, null, true, false, Optional.empty(), true,
                    // MetaFields / MetaAndTemplateFields are generated CLASSES, not enums, and the synthetic
                    // property is unwrapped - so it is no enum and it states no meta-value verdict at all
                    false, Optional.<Boolean>empty());
        }

        /**
         * {@code RJavaPojoInterface.addProperty} ({@code :434-466}), fact for fact: a new key builds a property whose
         * five name slots are all the attribute's own; an existing key is Case 0 or a SPECIALIZATION, and the
         * specialization re-{@code put}s so the ancestor's position is kept.
         */
        private void addProperty(String name, String javaType, Optional<String> metaValueType, boolean hasLocation,
                                 List<String> attributeMetaTypes, List<String> attributeMeta,
                                 List<IRDocReference> docReferences, boolean required, boolean multi,
                                 IRType itemReferenceForSubtype, boolean synthetic, boolean inheritedChoiceOption,
                                 Optional<String> definition, boolean itemIsRosettaModelObject,
                                 boolean itemIsEnum, Optional<Boolean> metaValueIsRosettaModelObject) {
            // the per-property javadoc the POJO getter carries: rendered HERE, from this property's own definition and
            // doc references, so that it can only ever ride the Case 0 / specialization law its inputs ride
            Optional<String> javadoc = javadocOf(definition, docReferences);
            IRProperty parent = all.get(name);
            if (parent == null) {
                IRProperty property = new IRProperty(name, javaType, required, multi, name, name,
                        false, true, true, 0, metaValueType, hasLocation, attributeMetaTypes, attributeMeta,
                        docReferences, synthetic, inheritedChoiceOption, definition, javadoc,
                        itemIsRosettaModelObject, List.<IRParentLink>of(), itemIsEnum,
                        // THE RUNE NAME (v3.3 seat 9, PR #645 commit 9): a FRESH property's five name slots are
                        // all the declaration's own (RJavaPojoInterface:440-443 passes `name` five times), and the
                        // rune name is one of them. The emitter never ASSUMES the identity - the fact is carried
                        // here and reconciled per property against getRuneName() - but this is where the identity
                        // is STATED, once, at the one site the old generator states it
                        metaValueIsRosettaModelObject, name);
                own.put(name, property);
                all.put(name, property);
                return;
            }
            if (javaType.equals(parent.javaType()) && required == parent.required()) {
                return;   // Case 0 - no property in EITHER map; the child's javadoc, doc references and meta are dropped
            }
            boolean subtypeOfParent = isPojoSubtype(javaType, parent.javaType(), itemReferenceForSubtype);
            String getterCompatibilityName = subtypeOfParent
                    ? parent.getterCompatibilityName()
                    : incompatiblePropertyName(name, parent.javaType(), javaType);
            String setterCompatibilityName = erasure(javaType).equals(erasure(parent.javaType()))
                    ? incompatiblePropertyName(name, parent.javaType(), javaType)
                    : parent.setterCompatibilityName();
            // THE ANCESTOR CHAIN (v3.3 seat 9, PR #645 commit 5): the parent property's OWN link first, then the
            // parent's own chain - which is the ancestor order `for (anc = p.getParentProperty(); anc != null; anc =
            // anc.getParentProperty())` visits at every one of PojoCompatEmitter's walks and at the chain's import
            // arm (ModelObjectGenerator:288-330). Built where the specialization is built, so it can only ever ride
            // the same law the depth beside it rides.
            // THE SIX RUNG FACTS (v3.3 seat 9, PR #645 commit 10) are the PARENT PROPERTY'S OWN, read off the
            // very object the old generator's getParentProperty() link points at - so a rung can never describe
            // a different derivation from the one the ancestor's own members are written from (LAW 69).
            List<IRParentLink> parentChain = new ArrayList<>(parent.parentChain().size() + 1);
            parentChain.add(new IRParentLink(parent.javaType(), parent.multi(), parent.required(),
                    parent.metaValueType(), parent.itemIsRosettaModelObject(), parent.itemIsEnum(),
                    parent.metaValueIsRosettaModelObject(), parent.getterOverridesParentGetter(),
                    parent.getterCompatibilityName(), parent.setterCompatibilityName()));
            parentChain.addAll(parent.parentChain());
            IRProperty specialized = new IRProperty(name, javaType, required, multi,
                    getterCompatibilityName, setterCompatibilityName,
                    getterCompatibilityName.equals(parent.getterCompatibilityName()),
                    subtypeOfParent, javaType.equals(parent.javaType()), parent.parentChainDepth() + 1,
                    metaValueType, hasLocation, attributeMetaTypes, attributeMeta, docReferences,
                    synthetic, inheritedChoiceOption,
                    // a specialization REPLACES the parent's definition and javadoc, exactly as
                    // JavaPojoProperty.specialize (:104-114) takes the newJavadoc rather than keeping its own
                    definition, javadoc,
                    itemIsRosettaModelObject, parentChain, itemIsEnum, metaValueIsRosettaModelObject,
                    // THE RUNE NAME (v3.3 seat 9, PR #645 commit 9): a specialization KEEPS the parent property's,
                    // exactly as JavaPojoProperty.specialize (:105-113) passes its own `runeName` through rather
                    // than re-deriving it from the specializing declaration's name
                    parent.runeName());
            own.put(name, specialized);
            all.put(name, specialized);   // the re-put keeps the ANCESTOR's iteration position
        }

        // ------------------------------------------------------------------------------------ the subtype walk

        /**
         * {@code RJavaPojoInterface.isPojoSubtype} ({@code :511-530}) over the rendered types. Its fourth arm - "the
         * parent item is not a {@code JavaClass}" - can never fire here: every rendered item IS a class name, because
         * {@code getItemType} of a {@code List<X>} is {@code X} and a non-list property type is a class already.
         */
        private boolean isPojoSubtype(String childType, String parentType, IRType childItemReference) {
            if (childType.equals(parentType)) {
                return true;
            }
            boolean childList = isList(childType);
            if (childList != isList(parentType)) {
                return false;
            }
            String childItem = childList ? itemOf(childType) : childType;
            String parentItem = childList ? itemOf(parentType) : parentType;
            if (childItem.equals(parentItem)) {
                return true;
            }
            return modelTypeStrictlyExtends(childItemReference, parentItem);
        }

        /**
         * {@code RJavaPojoInterface.modelTypeStrictlyExtends} ({@code :565-597}): the child item's Rosetta supertype
         * chain, walked STRICTLY (the child itself never counts) through the index. A wrapped item passes {@code null}
         * and only equality can succeed; a CHOICE item unwraps to no data node and so extends nothing; a CHOICE
         * supertype terminates the chain.
         *
         * <p><b>WHERE THIS DELIBERATELY DIVERGES FROM THE ORACLE (the property gate's design, PLAN § C.1).</b> The
         * oracle reaches an ancestor through the linker's own {@code superType()} / {@code choiceSuperType()} link
         * ({@code RJavaPojoInterface.modelTypeStrictlyExtends}, {@code :565-597}): a link the linker never resolved is
         * simply ABSENT there, the walk returns {@code false}, and the POJO is emitted - flat, with no ancestry and no
         * word said about it. This walk reaches the ancestor through {@link IRTypeIndex#parent(IRType)} instead, which
         * REFUSES ({@code GenerationException}) when the reference is UNRESOLVED, when the qualified name is declared
         * NOWHERE (ABSENT) or when it is declared more than once (AMBIGUOUS). That is not an oversight to be relaxed
         * back to the oracle's silence: a type whose parent cannot be found has an UNKNOWN property surface, and the
         * IR route states that it cannot write the file rather than writing a file that silently lost its inherited
         * members. The divergence is INERT on the frozen corpus - every extending type's parent resolves - and the
         * gate is what proves it: an element the IR route refuses is booked by {@code threw()} as a mismatch and can
         * never leave the population quietly.
         */
        private boolean modelTypeStrictlyExtends(IRType childItemReference, String parentItemCanonical) {
            if (childItemReference == null || childItemReference.kind() != IRKind.STRUCT) {
                return false;
            }
            IRTypeNode node = index.parent(childItemReference);
            Set<String> seen = new LinkedHashSet<>();
            while (node != null && seen.add(node.name()) && seen.size() <= MAX_ANCESTRY_DEPTH) {
                Optional<IRType> base = node.baseType();
                if (base.isEmpty()) {
                    return false;
                }
                IRType ancestor = base.get();
                boolean named = parentItemCanonical.equals(canonicalOrNull(ancestor));
                if (ancestor.kind() == IRKind.STRUCT) {
                    if (named) {
                        return true;
                    }
                    node = index.parent(ancestor);
                    continue;
                }
                return ancestor.kind() == IRKind.CHOICE && named;
            }
            return false;
        }

        // -------------------------------------------------------------------------------- the two override unions

        /**
         * {@code MetaFieldGenerator.allMetaAnnotationRefs} ({@code :109-118}): the override chain's annotations FIRST,
         * then the field's own.
         */
        private List<IRAnnotationUse> unionedAnnotations(IRTypeNode enclosing, IRField field, int depth) {
            Inherited parent = parentAttributeOf(enclosing, field, depth);
            if (parent == null) {
                return field.annotations();
            }
            List<IRAnnotationUse> all = new ArrayList<>(unionedAnnotations(parent.owner(), parent.field(), depth + 1));
            all.addAll(field.annotations());
            return all;
        }

        /** {@code RJavaPojoInterface.allDocReferences} ({@code :539-556}): the parent chain's FIRST, then the own. */
        private List<IRDocReference> unionedDocReferences(IRTypeNode enclosing, IRField field, int depth) {
            List<IRDocReference> own = field.docReferences();
            Inherited parent = parentAttributeOf(enclosing, field, depth);
            if (parent == null) {
                return own;
            }
            List<IRDocReference> parentAll = unionedDocReferences(parent.owner(), parent.field(), depth + 1);
            if (parentAll.isEmpty()) {
                return own;
            }
            List<IRDocReference> all = new ArrayList<>(parentAll.size() + own.size());
            all.addAll(parentAll);
            all.addAll(own);
            return all;
        }

        /**
         * {@code RuleReferenceTraversal.parentAttributeOf} ({@code :415-432}): the first same-named field found
         * walking the enclosing type's STRUCT supertype chain - and only when the field is declared {@code override}
         * and the enclosing declaration is a {@code type} (a choice option has no override chain).
         */
        private Inherited parentAttributeOf(IRTypeNode enclosing, IRField field, int depth) {
            if (!field.isOverride() || enclosing.kind() != IRKind.STRUCT || depth > MAX_ANCESTRY_DEPTH) {
                return null;
            }
            IRTypeNode current = structSuperOf(enclosing);
            Set<String> seen = new LinkedHashSet<>();
            while (current != null && seen.add(current.name()) && seen.size() <= MAX_ANCESTRY_DEPTH) {
                for (IRField candidate : current.fields()) {
                    if (field.name().equals(candidate.name())) {
                        return new Inherited(current, candidate);
                    }
                }
                current = structSuperOf(current);
            }
            return null;
        }

        /**
         * The STRUCT supertype the override chain climbs, or {@code null} when the node declares none (or extends a
         * CHOICE, which has no attributes to override).
         *
         * <p><b>WHERE THIS DELIBERATELY DIVERGES FROM THE ORACLE (the property gate's design, PLAN § C.1).</b> The
         * oracle's chain walk is {@code RuleReferenceTraversal.parentAttributeOf} ({@code :415-431}), which climbs
         * {@code RDataType.superType()} - a link the linker never resolved is ABSENT, the walk stops and returns
         * {@code null}, the override's inherited doc references and {@code [metadata …]} are silently lost, and the
         * POJO is emitted anyway. This walk climbs through {@link IRTypeIndex#parent(IRType)}, which REFUSES an
         * UNRESOLVED, an ABSENT and an AMBIGUOUS reference by name. Same law, deliberately louder end: an override
         * whose parent cannot be found has an UNKNOWN annotation and doc-reference union, and the IR route refuses the
         * file rather than emitting one that quietly dropped them. Inert on the frozen corpus; the refusal is booked
         * as a mismatch by the gate, never dropped.
         */
        private IRTypeNode structSuperOf(IRTypeNode node) {
            Optional<IRType> base = node.baseType();
            if (base.isEmpty() || base.get().kind() != IRKind.STRUCT) {
                return null;
            }
            return index.parent(base.get());
        }

        /**
         * The meta wrapper's canonical class name ({@code wrapFieldWithMeta} / {@code wrapReferenceWithMeta},
         * {@code :658-677}): the wrapper's package is derived from the WRAPPED type's namespace, not the containing
         * type's - a generated value type takes its own package plus {@code metafields}, and a builtin takes
         * {@code com.rosetta.model.metafields}.
         */
        private static String wrapper(MetaKind metaKind, IRType reference, String bare) {
            String prefix = metaKind == MetaKind.REFERENCE_WITH_META ? "ReferenceWithMeta" : "FieldWithMeta";
            return wrapperPackage(reference, bare) + "." + prefix + simpleNameOf(bare);
        }

        private static String wrapperPackage(IRType reference, String bare) {
            if (!isGenerated(reference)) {
                return LIB_METAFIELDS;
            }
            int lastDot = bare.lastIndexOf('.');
            // the value type's own (already escaped) package, plus the `metafields` segment - `metafields` is a valid
            // Java identifier, so the old generator's second escape of the joined path is the identity here
            return lastDot < 0 ? "metafields" : bare.substring(0, lastDot) + ".metafields";
        }

        /**
         * THE ITEM-KIND FACT (v3.3 seat 9, PR #645 commit 5 - the gate commit): whether the property's ITEM type is
         * a {@code RosettaModelObject}, derived from the IR ALONE. The oracle is
         * {@code JavaTypeUtil.isRosettaModelObject} ({@code :236-238}) - {@code getItemType(type).isSubtypeOf(
         * ROSETTA_MODEL_OBJECT)} - read on the old generator's own property through the commit's seam
         * ({@code ModelObjectGenerator.itemIsRosettaModelObject}); this is the SECOND producer (LAW 69) and it reads
         * no Java type lattice at all:
         * <ul>
         *   <li>a META-WRAPPED item is the GENERATED {@code FieldWithMeta*} / {@code ReferenceWithMeta*} class, and
         *       every one of those implements {@code RosettaModelObject} - TRUE whatever the value type is;</li>
         *   <li>otherwise the item IS the declaration the field's type reference resolves to: a {@code type}
         *       (STRUCT) and a {@code choice} (CHOICE) are emitted as {@code RosettaModelObject} interfaces - TRUE;
         *       an {@code enum} (ENUM) is a plain Java enum, and a BASIC_TYPE / RECORD_TYPE / META_TYPE is a value
         *       class the model library declares ({@code String}, {@code BigDecimal},
         *       {@code com.rosetta.model.lib.records.Date}) - FALSE;</li>
         *   <li>a {@code typeAlias} reference answers on the LEAF its chain COLLAPSES to ({@link IRType#effectiveBase()},
         *       PR #643) - the alias name never reaches a generated byte, the base does.</li>
         * </ul>
         *
         * @throws GenerationException for an alias reference whose chain did not collapse - the same absence
         *     {@link IRJavaTypeNames} refuses a Java type for; the fact is REFUSED rather than defaulted, because a
         *     silent {@code false} would write {@code List<X>} where the golden writes {@code List<? extends X>}
         */
        private static boolean itemIsRosettaModelObject(MetaKind metaKind, IRType reference) {
            if (metaKind != MetaKind.NONE) {
                return true;
            }
            return isModelObjectReference(reference);
        }

        /**
         * THE ITEM-IS-ENUM FACT (v3.3 seat 9, PR #645 commit 8): whether the property's ITEM type is a plain Java
         * {@code enum}, which is the ONE question the {@code hashCode} boilerplate's enum arm asks
         * ({@code ModelObjectBoilerplate:205-206}: an enum-item field hashes its CLASS NAME, every other field its
         * own {@code hashCode}). The oracle is {@code getItemType(prop.getType()) instanceof RJavaEnum}, read on the
         * old generator's own property through this commit's seam {@code ModelObjectGenerator.itemIsEnum} - the
         * very expression the byte-writing site now delegates to, so the two can never drift.
         *
         * <p>The IR half reads no Java type lattice at all: a META-WRAPPED item is the GENERATED
         * {@code FieldWithMeta*} / {@code ReferenceWithMeta*} CLASS and never an enum, whatever the value type is -
         * FALSE; otherwise the item IS the declaration the reference resolves to, and only {@link IRKind#ENUM} is
         * emitted as a Java {@code enum} (a STRUCT and a CHOICE are interfaces, a BASIC_TYPE / RECORD_TYPE /
         * META_TYPE is a value class the model library declares); an alias answers on the LEAF its chain collapses
         * to (PR #643).
         *
         * @throws GenerationException for an alias reference whose chain did not collapse - refused, never
         *     defaulted, exactly as the item-kind fact beside it is
         */
        private static boolean itemIsEnum(MetaKind metaKind, IRType reference) {
            if (metaKind != MetaKind.NONE) {
                return false;
            }
            return collapsedKind(reference, "item-is-enum") == IRKind.ENUM;
        }

        /**
         * THE META-VALUE-KIND FACT (v3.3 seat 9, PR #645 commit 8): whether the BARE value type behind a meta wrap
         * is a {@code RosettaModelObject} - EMPTY when the property carries no meta value type at all, because
         * "there is no value type" and "the value type is not a model object" are two different statements and the
         * old generator's own fact is a NULLABLE {@code getMetaValueType()}. The oracle is
         * {@code ModelObjectGenerator.metaValueIsRosettaModelObject}, which the three byte-writing sites of the
         * fact ({@code :1325-1327} and {@code :1341-1342}, the two {@code hasData} presence-only arms, and
         * {@code :1585}, the list meta-value setter's {@code .toBuilder()} suffix) now delegate to.
         *
         * <p>The IR half asks {@link #isModelObjectReference} of the SAME reference the {@code metaValueType}
         * rendering is taken from - the bare value BEFORE the wrap - so the two facts of one reference can never
         * disagree about which declaration they describe.
         */
        private static Optional<Boolean> metaValueIsRosettaModelObject(MetaKind metaKind, IRType reference) {
            if (metaKind == MetaKind.NONE) {
                return Optional.empty();
            }
            return Optional.of(isModelObjectReference(reference));
        }

        /**
         * The ONE reference-kind law of this class (v3.3 seat 9, PR #645 commit 8): whether the declaration a
         * reference resolves to - through its collapsed alias chain - is emitted as a {@code RosettaModelObject}
         * interface. The item-kind fact and the meta-value-kind fact are this same question asked of a DIFFERENT
         * reference, which is why it is declared once.
         */
        private static boolean isModelObjectReference(IRType reference) {
            return isModelObjectKind(collapsedKind(reference, "item-kind"));
        }

        /**
         * The reference's own kind, or - for a {@code typeAlias} - the kind of the LEAF its chain COLLAPSES to
         * ({@link IRType#effectiveBase()}, PR #643). An alias whose chain did not collapse is REFUSED rather than
         * defaulted: it is the same absence {@link IRJavaTypeNames} refuses a Java type for, and a silent default
         * would write a byte no declaration asked for.
         */
        private static IRKind collapsedKind(IRType reference, String factName) {
            IRKind kind = reference.kind();
            if (kind == IRKind.TYPE_ALIAS) {
                IREffectiveBase base = reference.effectiveBase().orElseThrow(() -> new GenerationException(
                        "IR property model: the alias reference '" + reference.name() + "' carries no collapsed"
                                + " effective base, so the " + factName + " fact cannot be derived - it is"
                                + " refused, never defaulted", null, null));
                return base.kind();
            }
            return kind;
        }

        /** The two kinds the old generator emits as {@code RosettaModelObject} interfaces. */
        private static boolean isModelObjectKind(IRKind kind) {
            return kind == IRKind.STRUCT || kind == IRKind.CHOICE;
        }

        /**
         * True when the old generator's {@code toJavaReferenceType} renders this reference as an
         * {@code RGeneratedJavaClass} - a declared {@code type}, {@code choice} or {@code enum}, or an alias chain
         * that ends on one. Every other reference is a builtin the model library declares.
         */
        private static boolean isGenerated(IRType reference) {
            IRKind kind = reference.kind();
            if (kind == IRKind.TYPE_ALIAS) {
                return reference.effectiveBase().map(base -> isDeclared(base.kind())).orElse(false);
            }
            return isDeclared(kind);
        }

        private static boolean isDeclared(IRKind kind) {
            return kind == IRKind.STRUCT || kind == IRKind.CHOICE || kind == IRKind.ENUM;
        }

        /** The escaped canonical name of a resolved declaration REFERENCE, or {@code null} when it resolves nothing. */
        private static String canonicalOrNull(IRType reference) {
            try {
                return IRJavaTypeNames.of(reference, List.of());
            } catch (GenerationException refused) {
                return null;
            }
        }

        private static boolean hasTypeMeta(IRTypeNode type, String qualifier) {
            return IRPropertyModel.hasTypeMeta(type, qualifier);
        }

        private static boolean hasMeta(List<IRAnnotationUse> annotations, String qualifier) {
            for (IRAnnotationUse annotation : annotations) {
                if ("metadata".equals(annotation.name())
                        && annotation.qualifier().map(qualifier::equals).orElse(false)) {
                    return true;
                }
            }
            return false;
        }

        /** {@code MetaFieldGenerator.detectMetaKind} ({@code :128-146}): {@code reference}/{@code address} WINS. */
        private static MetaKind metaKindOf(List<IRAnnotationUse> annotations) {
            boolean hasReference = false;
            boolean hasFieldMeta = false;
            for (IRAnnotationUse annotation : annotations) {
                if (!"metadata".equals(annotation.name())) {
                    continue;
                }
                String qualifier = annotation.qualifier().orElse("");
                if ("reference".equals(qualifier) || "address".equals(qualifier)) {
                    hasReference = true;
                }
                if ("scheme".equals(qualifier) || "id".equals(qualifier) || "location".equals(qualifier)) {
                    hasFieldMeta = true;
                }
            }
            if (hasReference) {
                return MetaKind.REFERENCE_WITH_META;
            }
            return hasFieldMeta ? MetaKind.FIELD_WITH_META : MetaKind.NONE;
        }

        /** {@code GeneratorModel.isMulti} ({@code :673-677}): a field with no declared cardinality is never multi. */
        private static boolean isMulti(IRField field) {
            return field.bounds()
                    .map(b -> b.isUnbounded() || b.upper().get().compareTo(BigInteger.ONE) > 0)
                    .orElse(false);
        }

        /** {@code RJavaPojoInterface:302-304}: a field with no declared cardinality is never required. */
        private static boolean isRequired(IRField field) {
            return field.bounds().map(b -> b.lower().signum() > 0).orElse(false);
        }
    }

    /** The owning node and the field an override chain's next rung resolves to. */
    private record Inherited(IRTypeNode owner, IRField field) {
    }

    /** The three meta kinds, reproduced rather than read: the IR half never imports the old generator's enum. */
    private enum MetaKind {
        NONE, FIELD_WITH_META, REFERENCE_WITH_META
    }

    // ------------------------------------------------------------------------------- the rendered-type string laws

    /**
     * {@code RJavaPojoInterface.incompatiblePropertyName} ({@code :477-494}) over the rendered types: list to list
     * recurses on the item types; list to single appends {@code OverriddenAsSingle}, plus the specialized type's
     * SIMPLE name when the parent's item differs from it; anything else appends {@code OverriddenAs} plus that simple
     * name.
     */
    static String incompatiblePropertyName(String propertyName, String parentType, String specializedType) {
        if (isList(parentType) && isList(specializedType)) {
            return incompatiblePropertyName(propertyName, itemOf(parentType), itemOf(specializedType));
        }
        if (isList(parentType)) {
            if (itemOf(parentType).equals(specializedType)) {
                return propertyName + "OverriddenAsSingle";
            }
            return propertyName + "OverriddenAsSingle" + simpleNameOf(specializedType);
        }
        return propertyName + "OverriddenAs" + simpleNameOf(specializedType);
    }

    /**
     * {@code JavaType.getTypeErasure()} over the rendered types ({@code JavaType:74-76},
     * {@code JavaParameterizedType:249-251}): a list erases to {@code java.util.List} whatever its item, and every
     * other type erases to itself.
     */
    static String erasure(String rendered) {
        return isList(rendered) ? "java.util.List" : rendered;
    }

    /** The rendering of a list property: {@code JavaParameterizedType.toString()} spells the wrapper SIMPLE. */
    static String list(String itemType) {
        return "List<" + itemType + ">";
    }

    static boolean isList(String rendered) {
        return rendered.startsWith("List<") && rendered.endsWith(">");
    }

    static String itemOf(String rendered) {
        return rendered.substring("List<".length(), rendered.length() - 1);
    }

    /** {@code JavaTypeDeclaration.getSimpleName()}: the last segment of the canonical name - {@code List} for a list. */
    static String simpleNameOf(String rendered) {
        if (isList(rendered)) {
            return "List";
        }
        int lastDot = rendered.lastIndexOf('.');
        return lastDot < 0 ? rendered : rendered.substring(lastDot + 1);
    }

    // ------------------------------------------------------------------------------- the rendered javadoc's own law

    /**
     * THE PER-PROPERTY JAVADOC, RENDERED FROM THE IR ALONE - the block {@code ModelObjectGenerator} indents and writes
     * above the interface getter ({@code :641-644}, {@code prop.getJavadoc()}). The old generator builds it at
     * {@code RJavaPojoInterface:287-288} for an attribute and {@code :379-380} for a choice option, both through
     * {@code ModelGeneratorUtil.javadoc(definition, docRefs, version, pojoStyle = true)}; this is that method
     * REPRODUCED, never called, because the two halves of the reconcile must be two producers (LAW 69) - and because
     * the emitter that follows may hold no {@code ModelGeneratorUtil} at all.
     *
     * <p>THE LAW, decision for decision ({@code ModelGeneratorUtil:77-102}):
     * <ul>
     *   <li><b>the version is NOT part of it.</b> Both property call sites pass {@code null} for the version - the
     *       {@code @version} line belongs to the TYPE's own javadoc ({@code RJavaPojoInterface.getJavadoc},
     *       {@code :113-124}), never to a property's. So this renderer takes no version and needs none, and
     *       {@link #of(IRTypeNode, IRTypeIndex)} keeps its two arguments.</li>
     *   <li><b>three definition states, not two</b> - ABSENT emits no block at all (when there is no doc reference
     *       either); the explicit empty {@code <"">} emits the block with NO body line; a non-empty definition emits
     *       {@code  * <text>}.</li>
     *   <li><b>the separator</b> - one {@code  *} line before EACH doc reference, and the block closes with
     *       {@code  *&#47;} (the comment terminator, spelt with an entity so this javadoc survives it); there is no trailing newline.</li>
     *   <li><b>HTML escaping</b> ({@code escapeHtml}, {@code :141-158}) on the definition, the corpus display name,
     *       the corpus definition and each segment value - and, pointedly, NOT on the body, the corpus name, the
     *       segment name or the provision, which the old generator appends raw.</li>
     *   <li><b>the Xtend continuation prefix</b> ({@code xtendIndent}, {@code :123-136}) on every interpolated text:
     *       an embedded newline takes one following space, because the upstream template's interpolation sits on a
     *       line whose leading whitespace is one space.</li>
     * </ul>
     *
     * @return the rendered block, or empty when the declaration has neither a definition nor a doc reference - which
     *     is the old generator's {@code null} and the value {@code ModelObjectGenerator:641} skips on
     */
    static Optional<String> javadocOf(Optional<String> definition, List<IRDocReference> docReferences) {
        return javadocOf(definition, docReferences, null);
    }

    /**
     * THE SAME LAW WITH ITS {@code @version} LINE (v3.3 seat 9, PR #645 commit 4 - the data-type emitter's section
     * 2): a TYPE's javadoc is the identical block plus {@code  * @version <v>} written after the definition body
     * and before the doc references, and a non-empty version ALONE is enough to emit the block - exactly
     * {@code ModelGeneratorUtil.javadoc(definition, docRefs, version, pojoStyle = true)} ({@code :74-102}), whose
     * two PROPERTY call sites pass {@code null} for the version and so reach the two-argument form above with their
     * bytes unmoved. Additive at this commit: no existing caller changes.
     *
     * @param version the host's version stamp; {@code null} or empty emits no {@code @version} line
     */
    static Optional<String> javadocOf(Optional<String> definition, List<IRDocReference> docReferences,
                                      String version) {
        boolean hasDefinition = definition.isPresent();
        boolean hasDefinitionBody = hasDefinition && !definition.get().isEmpty();
        boolean hasDocReferences = !docReferences.isEmpty();
        boolean hasVersion = version != null && !version.isEmpty();
        if (!hasDefinition && !hasDocReferences && !hasVersion) {
            return Optional.empty();
        }
        StringBuilder sb = new StringBuilder("/**\n");
        if (hasDefinitionBody) {
            sb.append(" * ").append(xtendIndent(escapeHtml(definition.get()))).append("\n");
        }
        if (hasVersion) {
            sb.append(" * @version ").append(version).append("\n");
        }
        for (IRDocReference reference : docReferences) {
            sb.append(" *\n");
            appendDocReference(sb, reference);
        }
        return Optional.of(sb.append(" */").toString());
    }

    /**
     * ONE doc reference's lines ({@code ModelGeneratorUtil.appendDocReference}, {@code :168-232}): the {@code for}
     * path (when the reference is pathed), the Body line, ALL corpora on ONE line, all segments on ONE line, then the
     * Provision block between two bare {@code  *} lines.
     *
     * <p>The old generator guards the Body / Corpus / Segment lines on {@code regulatoryDocRef() != null}; the IR
     * carries that same absence as an empty {@link IRDocReference#body()} with no corpora and no segments (the
     * adapter's own {@code doc == null} arm), so the three loops below produce the identical nothing.
     *
     * <p>A corpus the referencing file's scope could NOT resolve renders its raw reference text followed by THREE
     * spaces - the old generator's own else-arm, kept rather than tidied, because it is byte-bearing.
     */
    private static void appendDocReference(StringBuilder sb, IRDocReference reference) {
        reference.path().ifPresent(path -> sb.append(" * ").append(path.display()).append("\n"));
        reference.body().ifPresent(body -> sb.append(" * Body ").append(body).append("\n"));
        for (IRDocReference.Corpus corpus : reference.corpora()) {
            sb.append(" * Corpus ");
            Optional<IRDocReference.Corpus.Declaration> resolved = corpus.resolved();
            if (resolved.isPresent()) {
                IRDocReference.Corpus.Declaration declaration = resolved.get();
                declaration.typeKeyword().ifPresent(keyword -> sb.append(keyword).append(" "));
                sb.append(declaration.name()).append(" ");
                declaration.displayName().ifPresent(name -> sb.append(xtendIndent(escapeHtml(name))));
                sb.append(" ");
                declaration.definition().ifPresent(text ->
                        sb.append("\"").append(xtendIndent(escapeHtml(text))).append("\""));
                sb.append(" ");
            } else {
                sb.append(corpus.reference()).append("   ");
            }
        }
        if (!reference.corpora().isEmpty()) {
            sb.append("\n");
        }
        if (!reference.segments().isEmpty()) {
            sb.append(" * ");
            for (int i = 0; i < reference.segments().size(); i++) {
                if (i > 0) {
                    sb.append(" * ");
                }
                IRDocReference.Segment segment = reference.segments().get(i);
                sb.append(segment.name()).append(" \"")
                        .append(xtendIndent(escapeHtml(segment.value()))).append("\"");
            }
            sb.append("\n");
        }
        sb.append(" *\n");
        String provision = reference.provision().orElse("");
        if (provision.isEmpty()) {
            sb.append(" * Provision \n");   // the trailing space is the old generator's, and it is byte-bearing
        } else {
            sb.append(" * Provision ").append(xtendIndent(provision)).append("\n");   // NOT html-escaped, as there
        }
        sb.append(" *\n");
    }

    /** {@code ModelGeneratorUtil.xtendIndent} ({@code :123-136}): one space after every embedded newline. */
    private static String xtendIndent(String text) {
        return text.indexOf('\n') < 0 ? text : text.replace("\n", "\n ");
    }

    /** {@code ModelGeneratorUtil.escapeHtml} ({@code :141-158}): the five entities, in its own order. */
    private static String escapeHtml(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '&' -> sb.append("&amp;");
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '"' -> sb.append("&quot;");
                case '\'' -> sb.append("&#39;");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }
}
