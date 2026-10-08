package com.regnosys.rosetta.generator.java.object;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import com.regnosys.rosetta.generator.GeneratedIdentifier;
import com.regnosys.rosetta.generator.java.JavaNamingUtil;
import com.regnosys.rosetta.generator.java.scoping.JavaStatementScope;
import com.regnosys.rosetta.generator.java.statement.JavaLambdaBody;
import com.regnosys.rosetta.generator.java.statement.JavaStatement;
import com.regnosys.rosetta.generator.java.statement.builder.JavaConditionalExpression;
import com.regnosys.rosetta.generator.java.statement.builder.JavaExpression;
import com.regnosys.rosetta.generator.java.statement.builder.JavaIfThenElseBuilder;
import com.regnosys.rosetta.generator.java.statement.builder.JavaLiteral;
import com.regnosys.rosetta.generator.java.statement.builder.JavaStatementBuilder;
import com.regnosys.rosetta.generator.java.statement.builder.JavaVariable;
import com.regnosys.rosetta.generator.java.types.JavaPojoProperty;
import com.regnosys.rosetta.generator.java.types.JavaPojoPropertyOperationType;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.generator.java.types.RJavaPojoInterface;
import com.rosetta.util.types.JavaPrimitiveType;
import com.rosetta.util.types.JavaType;

/**
 * The POJO-seat ancestor-shape compatibility matrix — the builder-compat facet burn
 * (PR #412, the PR #410 pinned lead). A structural port of upstream
 * {@code ModelObjectGenerator.derivedIncompatibleGettersForProperty} (impl side),
 * {@code ModelObjectBuilderGenerator.derivedIncompatibleGettersForProperty} +
 * {@code doSetter}'s {@code parentProperty} recursion (builder side), and the
 * {@code TypeCoercionService} item/wrapper conversion table those templates consume —
 * verified against the released-9.83.0 oracle goldens
 * ({@code holdout-goldens/pojo-inheritance}, {@code HoldOutByteCompareTest}).
 *
 * <p>For every SPECIALIZED (override) property, upstream emits ancestor-shape
 * compatibility operations on the Impl and BuilderImpl:
 * <ul>
 *   <li><b>Impl derived getters</b> — {@code @Override @RosettaIgnore @RuneIgnore}
 *       getters in each overridden ancestor's Java shape, the body coercing the
 *       specialized field to the ancestor's type (widening — the DSL restriction law:
 *       an override is always a restriction, so the getter direction never narrows).</li>
 *   <li><b>Builder derived getters</b> — the same walk with builder-flavored bodies
 *       ({@code .toBuilder()} mapping, builder-typed {@code emptyList} witnesses) plus
 *       a {@code getOrCreate} compat delegate per model-typed ancestor.</li>
 *   <li><b>Ancestor setter arms</b> — {@code @RosettaIgnore @RuneIgnore} setters/adders
 *       in each ancestor's shape whose bodies COERCE the argument to the main
 *       property's shape (narrowing — null-safe guarded) and delegate to the main
 *       setter family.</li>
 * </ul>
 *
 * <p><b>The coercion algebra.</b> Bodies are built on the fork's
 * {@link JavaStatementBuilder} algebra — the same classes the proven function pipeline
 * uses — so the structural laws fall out mechanically, byte-identical to upstream:
 * a conversion that stays a plain expression renders as a ternary
 * ({@code numberAttr == null ? null : new BigDecimal(numberAttr)}); a branching
 * conversion upgrades to {@link JavaIfThenElseBuilder} and a setter-seat
 * {@code collapseToSingleExpression} binds the {@code final X ifThenElseResult;
 * if …/else …} local; {@code completeAsReturn} on a branching getter renders the
 * early-return form. Locals bind via {@code declareAsVariable} with upstream's
 * reuse law (a {@link JavaVariable} — a parameter or field — is used in place; a
 * call expression binds a {@code final} local named after its type, escaped against
 * the class scope: {@code final Parent _parent = MapperC.of(parentLists).get();}).
 * All expression composition is LAZY ({@link #compose}) — sub-expressions render at
 * the final {@link #renderBody} pass, after every scope identifier exists (the
 * two-phase build/render contract of {@code GeneratorScope}).
 *
 * <p><b>The conversion table</b> (upstream {@code getItemConversion}, 9.83 line):
 * number ladder (widening plain / narrowing null-safe-guarded), meta unwrap
 * ({@code .getValue()} + recursion), meta wrap
 * ({@code X.builder().setValue(…).build()} with the empty-wrapper null
 * representation), model downcast ({@code x instanceof Y ? Y.class.cast(x) : null})
 * — taken only in the NARROWING (setter) direction; in the widening (getter)
 * direction an unequal model pair is an upcast and needs no conversion (upstream
 * decides via {@code isSubtypeOf}; the DSL restriction law fixes the direction at
 * this seat, so no model-subtype oracle is needed). Cardinality: single→list wraps
 * {@code Collections.singletonList} / {@code Collections.<T>emptyList()}; list→single
 * unwraps via the {@code MapperC.of(x).get()} head; list→list converts items in a
 * multiline {@code .stream().<T>map(…).collect(Collectors.toList())} form.
 *
 * <p>Zero corpus movement by construction: the frozen 9.83.0 corpus has no
 * meta/shape-changing override chains (waves A–D TRUE 100% could not have held
 * otherwise); the corpus-witnessed compat shapes (equal-type delegates, the
 * single-to-single {@code instanceof}/{@code class.cast} arm) reproduce the previous
 * hand-emitted bytes exactly — verified by the full D11 gate.
 *
 * <p><b>The #412 recorded corner — BURNED, WITNESSED AND HEALED at PR #422 (measured,
 * three oracle fixtures):</b> the corner was the bulk {@code ADD_VALUE}/{@code
 * SET_VALUE} arms of a meta-wrapped LIST ancestor with a FINAL value type missing
 * upstream's wildcard→non-wildcard {@code new ArrayList(«it»)} copy
 * (TypeCoercionService L342-344) because the arm's {@code BoundParam} typing masked
 * the pair as identity. The leg-E burn: (a) {@code pojo-bulk-meta-drop} proves a
 * meta-DROPPING override does NOT specialize (the child re-emits the parent's
 * {@code FieldWithMetaString} shape with covariant returns, NO compat arms); (b) a
 * same-V meta-KIND change DOES specialize — {@code pojo-bulk-meta-kind}
 * ({@code scheme} parent, {@code reference} override; {@code FieldWithMetaString} vs
 * {@code ReferenceWithMetaString}, value String FINAL) reaches the copy exactly as
 * the #412 record predicted (golden {@code return
 * addAttrOverriddenAsReferenceWithMetaStringValue(new ArrayList(attrs));}) — HEALED
 * here: the bulk {@code BoundParam} now binds the DECLARED wildcard form
 * ({@code wrapExtends}, matching the rendered signature) and the list→list identity
 * branch emits the raw {@code new ArrayList(«it»)} copy when the source is
 * wildcard-wrapped and the target is not (an initial mid-session
 * "unreachable" verdict was CORRECTED by this fixture — attack unreachability
 * claims with fixtures before closing a corner); (c) the bulk arms' item-conversion
 * stream form got its first witnesses too ({@code pojo-bulk-value-narrow},
 * int-under-number) — byte-identical after the braced-statement-lambda heal
 * ({@code toLambdaBody}'s JavaBlock shape + the tab-tolerant {@code reindent}
 * dedent, this PR).
 */
final class PojoCompatEmitter {

    private final JavaTypeUtil typeUtil;
    private final ModelObjectGenerator gen;
    /** Class-level scope holding every builder/impl field identifier (the escape context). */
    private final JavaStatementScope classScope;
    private final Map<String, GeneratedIdentifier> fieldIds = new LinkedHashMap<>();

    PojoCompatEmitter(ModelObjectGenerator gen, JavaTypeUtil typeUtil,
                      Collection<JavaPojoProperty> allProps) {
        this.gen = gen;
        this.typeUtil = typeUtil;
        this.classScope = new JavaStatementScope("pojo-compat-class", null);
        for (JavaPojoProperty p : allProps) {
            String fld = ModelObjectGenerator.fieldName(p);
            fieldIds.computeIfAbsent(fld, classScope::createUniqueIdentifier);
        }
    }

    /** Whether any property of the pojo carries a specialization chain. */
    static boolean anySpecialized(Collection<JavaPojoProperty> allProps) {
        return allProps.stream().anyMatch(p -> !ancestorChain(p).isEmpty());
    }

    /**
     * v3.3 seat 9 (PR #645 commit 5) - THE CHAIN WALK, declared ONCE (LAW 69). Every compat walk of this class
     * ({@link #appendImplDerivedGetters}, {@link #appendBuilderDerivedGetters} and the ancestor setter walk) and
     * the chain's import arm ({@code ModelObjectGenerator:288-330}) climb the same
     * {@link JavaPojoProperty#getParentProperty()} link; this is that climb, NEAREST ancestor first, and
     * {@link #anySpecialized} reads it so the declaration is consulted rather than merely stated.
     *
     * <p>It is the SOURCE half of the IR route's {@code property.<name>.parentChain.types} fact, reached from
     * outside this package through {@code ModelObjectGenerator.compatAncestorChain} (this class is package-private,
     * so a public member of it would be reachable by nobody). Behaviour-neutral: no byte of the compat members
     * moves.
     *
     * @return the ancestors of {@code prop}, nearest first; EMPTY when the property is not specialized
     */
    static java.util.List<JavaPojoProperty> ancestorChain(JavaPojoProperty prop) {
        java.util.List<JavaPojoProperty> chain = new java.util.ArrayList<>();
        for (JavaPojoProperty ancestor = prop.getParentProperty(); ancestor != null;
                ancestor = ancestor.getParentProperty()) {
            chain.add(ancestor);
        }
        return chain;
    }

    // =====================================================================
    // Impl-side derived getters (upstream ModelObjectGenerator L276-293)
    // =====================================================================

    /**
     * The impl-side derived-getter walk for one property: one
     * {@code @Override @RosettaIgnore @RuneIgnore} getter per ancestor whose getter
     * the specialized getter does NOT override (the {@code getterOverridesParentGetter}
     * skip law — a covariant link keeps the name and needs no compat member), the body
     * coercing the field to the ancestor's INTERFACE shape.
     */
    void appendImplDerivedGetters(StringBuilder body, JavaPojoProperty prop) {
        JavaPojoProperty cur = prop;
        while (cur.getParentProperty() != null) {
            JavaPojoProperty parent = cur.getParentProperty();
            if (!cur.getterOverridesParentGetter()) {
                JavaStatementScope scope = bodyScope();
                JavaStatementBuilder coerced = coerce(fieldVar(prop), prop, parent, false, scope);
                body.append("\t\t@Override\n");
                body.append("\t\t@").append(ModelObjectGenerator.T_ROSETTA_IGNORE).append("\n");
                body.append("\t\t@").append(ModelObjectGenerator.T_RUNE_IGNORE).append("\n");
                body.append("\t\tpublic ").append(gen.interfaceGetterType(parent)).append(" ")
                    .append(parent.getOperationName(JavaPojoPropertyOperationType.GET))
                    .append("() {\n");
                body.append(renderBody(coerced.completeAsReturn()));
                body.append("\t\t}\n");
                body.append("\t\t\n");
            }
            cur = parent;
        }
    }

    // =====================================================================
    // Builder-side derived getters + getOrCreate compat
    // (upstream ModelObjectBuilderGenerator L198-268)
    // =====================================================================

    /**
     * The builder-side derived-getter walk for one property: per non-overridden
     * ancestor, a builder-flavored compat getter (coercion + {@code .toBuilder()}
     * mapping; a list-shaped ancestor over a single field renders the
     * {@code emptyList}/{@code singletonList} pair with the BUILDER-typed witness),
     * plus — when the ancestor is model-typed — the {@code getOrCreate} compat
     * delegate through the specialized accessor.
     */
    void appendBuilderDerivedGetters(StringBuilder body, RJavaPojoInterface pojo,
                                     JavaPojoProperty prop) {
        JavaPojoProperty cur = prop;
        while (cur.getParentProperty() != null) {
            JavaPojoProperty parent = cur.getParentProperty();
            if (!cur.getterOverridesParentGetter()) {
                appendBuilderDerivedGetter(body, prop, parent);
                if (typeUtil.isRosettaModelObject(parent.getType())) {
                    appendBuilderGetOrCreateCompat(body, pojo, prop, parent);
                }
            }
            cur = parent;
        }
    }

    private void appendBuilderDerivedGetter(StringBuilder body, JavaPojoProperty prop,
                                            JavaPojoProperty parent) {
        JavaStatementScope scope = bodyScope();
        JavaVariable field = fieldVar(prop);
        boolean parentList = isList(parent);
        boolean propList = isList(prop);
        JavaStatementBuilder result;
        if (parentList && propList) {
            // List over list: coerce items to the ancestor's item type, then re-map each
            // element to its builder (upstream's single-line stream template, L215-225).
            JavaStatementBuilder coerced = coerce(field, prop, parent, false, scope)
                    .collapseToSingleExpression(scope);
            String lambdaParam = scope.disambiguate(
                    JavaNamingUtil.toFirstLower(itemType(parent).getSimpleName()));
            String lambdaBody = typeUtil.isRosettaModelObject(itemType(parent))
                    ? lambdaParam + ".toBuilder()" : lambdaParam;
            result = coerced.mapExpression(it -> compose(parent.getType(),
                    it, ".stream().map(" + lambdaParam + " -> " + lambdaBody
                            + ").collect(" + ModelObjectGenerator.T_COLLECTORS + ".toList())"));
        } else if (parentList) {
            // List-shaped ancestor over a single field: coerce to the ancestor's ITEM
            // type, then null-literal → builder-typed emptyList / value → toBuilder +
            // singletonList (upstream L226-236).
            JavaStatementBuilder itemCoerced = itemToItemFull(field,
                    itemType(prop), prop.getMetaValueType(),
                    itemType(parent), parent.getMetaValueType(), false, scope);
            result = itemCoerced.mapExpression(it -> {
                if (it == JavaLiteral.NULL) {
                    return JavaExpression.from(
                            ModelObjectGenerator.T_COLLECTIONS + ".<" + gen.builderSingleType(parent) + ">emptyList()",
                            parent.getType());
                }
                return compose(parent.getType(),
                        ModelObjectGenerator.T_COLLECTIONS + ".singletonList(", toBuilder(it), ")");
            });
        } else {
            // Single-shaped ancestor: plain coercion + toBuilder on the non-null result.
            result = coerce(field, prop, parent, false, scope)
                    .mapExpressionIfNotNull(this::toBuilder);
        }
        body.append("\t\t@Override\n");
        body.append("\t\t@").append(ModelObjectGenerator.T_ROSETTA_IGNORE).append("\n");
        body.append("\t\t@").append(ModelObjectGenerator.T_RUNE_IGNORE).append("\n");
        body.append("\t\tpublic ").append(gen.builderGetterType(parent)).append(" ")
            .append(parent.getOperationName(JavaPojoPropertyOperationType.GET)).append("() {\n");
        body.append(renderBody(result.completeAsReturn()));
        body.append("\t\t}\n");
        body.append("\t\t\n");
    }

    private void appendBuilderGetOrCreateCompat(StringBuilder body, RJavaPojoInterface pojo,
                                                JavaPojoProperty prop, JavaPojoProperty parent) {
        JavaStatementScope scope = bodyScope();
        String getOrCreateName = parent.getOperationName(JavaPojoPropertyOperationType.GET_OR_CREATE);
        String delegateName = prop.getOperationName(JavaPojoPropertyOperationType.GET_OR_CREATE);
        boolean parentIsList = isList(parent);
        // Upstream L249/L259: the delegate call passes the index through only when the
        // SPECIALIZED property is itself a list (the single-side getOrCreate takes none).
        String idxParam = ModelObjectGenerator.hasIndexPropertyInScope(pojo) ? "_index" : "index";
        String delegateArgs = isList(prop) && parentIsList ? idxParam : "";
        JavaExpression call = JavaExpression.from(
                delegateName + "(" + delegateArgs + ")", itemType(prop));
        JavaStatementBuilder coerced = itemToItemFull(call,
                itemType(prop), prop.getMetaValueType(),
                itemType(parent), parent.getMetaValueType(), false, scope)
                .mapExpressionIfNotNull(this::toBuilder);
        body.append("\t\t@Override\n");
        body.append("\t\tpublic ").append(gen.builderSingleType(parent)).append(" ")
            .append(getOrCreateName).append("(")
            .append(parentIsList ? "int " + idxParam : "").append(") {\n");
        body.append(renderBody(coerced.completeAsReturn()));
        body.append("\t\t}\n");
        body.append("\t\t\n");
    }

    // =====================================================================
    // Ancestor setter arms (upstream ModelObjectBuilderGenerator doSetter,
    // the parentProperty recursion — non-main branches)
    // =====================================================================

    /**
     * All ancestor setter arms for one specialized property, nearest ancestor first.
     * Each arm dispatches on the ANCESTOR's shape (upstream {@code doSetter} recursion
     * dispatches on {@code currentProp}): a single-shaped ancestor contributes SET
     * (+ SET_VALUE when meta-wrapped); a list-shaped ancestor contributes ADD /
     * ADD-idx / (ADD_VALUE pair when its item is meta-wrapped) / ADD-list / SET-list
     * (+ ADD_VALUE/SET_VALUE bulk when meta-wrapped). Bodies coerce to the MAIN
     * property's shape (narrowing, null-safe) and delegate to the main setter family.
     */
    void appendAncestorSetterArms(StringBuilder body, JavaPojoProperty mainProp,
                                  String builderRetType, Set<String> siblingFieldNames) {
        for (JavaPojoProperty anc = mainProp.getParentProperty(); anc != null;
                anc = anc.getParentProperty()) {
            if (isList(anc)) {
                appendListAncestorArms(body, mainProp, anc, builderRetType, siblingFieldNames);
            } else {
                appendSingleAncestorArms(body, mainProp, anc, builderRetType);
            }
        }
    }

    private void appendSingleAncestorArms(StringBuilder body, JavaPojoProperty mainProp,
                                          JavaPojoProperty anc, String builderRetType) {
        String fld = ModelObjectGenerator.fieldName(mainProp);
        // SET in the ancestor's shape → coerce to the main property's WHOLE shape.
        appendArm(body, builderRetType, anc.getOperationName(JavaPojoPropertyOperationType.SET),
                gen.valueSiteTypeName(itemType(anc)), "_" + fld, true,
                new BoundParam(itemType(anc), anc.getMetaValueType()),
                mainProp.getType(), mainProp.getMetaValueType(),
                mainProp.getOperationName(JavaPojoPropertyOperationType.SET), "");
        // SET_VALUE in the ancestor's value shape (meta-wrapped ancestor only). The
        // coercion target and the delegate follow the MAIN property's WHOLE type
        // (upstream doSetter L554/L568: `mainPropType instanceof RJavaWithMetaValue`
        // — a LIST main is never the wrapper itself, so it takes the SET/whole-type
        // arm even when its item is meta-wrapped).
        if (anc.getMetaValueType() != null) {
            boolean mainWholeMeta = !isList(mainProp) && mainProp.getMetaValueType() != null;
            JavaType targetType = mainWholeMeta ? mainProp.getMetaValueType() : mainProp.getType();
            JavaType targetMeta = mainWholeMeta ? null : mainProp.getMetaValueType();
            String mainOp = mainWholeMeta
                    ? mainProp.getOperationName(JavaPojoPropertyOperationType.SET_VALUE)
                    : mainProp.getOperationName(JavaPojoPropertyOperationType.SET);
            appendArm(body, builderRetType,
                    anc.getOperationName(JavaPojoPropertyOperationType.SET_VALUE),
                    gen.valueSiteTypeName(anc.getMetaValueType()), "_" + fld, false,
                    new BoundParam(anc.getMetaValueType(), null),
                    targetType, targetMeta, mainOp, "");
        }
    }

    private void appendListAncestorArms(StringBuilder body, JavaPojoProperty mainProp,
                                        JavaPojoProperty anc, String builderRetType,
                                        Set<String> siblingFieldNames) {
        String fld = ModelObjectGenerator.fieldName(mainProp);
        boolean mainList = isList(mainProp);
        String ancItemRender = gen.valueSiteTypeName(itemType(anc));
        String pluralParam = fld + "s";
        if (siblingFieldNames.contains(pluralParam)) {
            pluralParam = "_" + pluralParam;
        }
        String addName = anc.getOperationName(JavaPojoPropertyOperationType.ADD);
        String setName = anc.getOperationName(JavaPojoPropertyOperationType.SET);
        String mainAdd = mainProp.getOperationName(JavaPojoPropertyOperationType.ADD);
        String mainSet = mainProp.getOperationName(JavaPojoPropertyOperationType.SET);
        // Item-level meta-ness of the MAIN property drives the value-arm delegates
        // (upstream `mainItemType instanceof RJavaWithMetaValue`).
        boolean mainItemMeta = mainProp.getMetaValueType() != null;
        // ADD(item) — annotated with the ignore pair; coerces to the main ITEM type
        // (upstream doSetter L325: addCoercions(mainItemType)).
        appendArm(body, builderRetType, addName, ancItemRender, "_" + fld, true,
                new BoundParam(itemType(anc), anc.getMetaValueType()),
                itemType(mainProp), mainProp.getMetaValueType(),
                mainList ? mainAdd : mainSet, "");
        // ADD(item, idx) — bare; the index rides only onto a list-main ADD.
        appendArm(body, builderRetType, addName,
                ancItemRender, "_" + fld, false,
                new BoundParam(itemType(anc), anc.getMetaValueType()),
                itemType(mainProp), mainProp.getMetaValueType(),
                mainList ? mainAdd : mainSet, mainList ? ", idx" : "",
                ", int idx");
        // ADD_VALUE pair (meta-wrapped ancestor item only): coerce the ancestor's
        // value type to the main's value shape (upstream L361: mainItemType meta ?
        // valueType : mainItemType) and delegate to the main's value-or-plain op.
        if (anc.getMetaValueType() != null) {
            JavaType mainValue = mainItemMeta
                    ? mainProp.getMetaValueType() : itemType(mainProp);
            String mainValueOp = mainItemMeta
                    ? mainProp.getOperationName(mainList
                            ? JavaPojoPropertyOperationType.ADD_VALUE
                            : JavaPojoPropertyOperationType.SET_VALUE)
                    : (mainList ? mainAdd : mainSet);
            String ancAddValue = anc.getOperationName(JavaPojoPropertyOperationType.ADD_VALUE);
            String valueRender = gen.valueSiteTypeName(anc.getMetaValueType());
            appendArm(body, builderRetType, ancAddValue, valueRender, "_" + fld, false,
                    new BoundParam(anc.getMetaValueType(), null),
                    mainValue, null, mainValueOp, "");
            appendArm(body, builderRetType, ancAddValue, valueRender, "_" + fld, false,
                    new BoundParam(anc.getMetaValueType(), null),
                    mainValue, null, mainValueOp, mainList ? ", idx" : "",
                    ", int idx");
        }
        // ADD(List) — bare; coerces the whole list to the main's WHOLE shape
        // (upstream L424: addCoercions(mainPropType)).
        appendArm(body, builderRetType, addName, listParamRender(anc), pluralParam, false,
                new BoundParam(anc.getType(), anc.getMetaValueType()),
                mainProp.getType(), mainProp.getMetaValueType(),
                mainList ? mainAdd : mainSet, "");
        // SET(List) — annotated with the ignore pair.
        appendArm(body, builderRetType, setName, listParamRender(anc), pluralParam, true,
                new BoundParam(anc.getType(), anc.getMetaValueType()),
                mainProp.getType(), mainProp.getMetaValueType(), mainSet, "");
        // Bulk ADD_VALUE/SET_VALUE pair (meta-wrapped ancestor item only); the
        // coercion target is the main's value shape, list-wrapped when main is a
        // list (upstream L491/L515).
        if (anc.getMetaValueType() != null) {
            JavaType mainValue = mainItemMeta
                    ? mainProp.getMetaValueType() : itemType(mainProp);
            JavaType bulkTarget = mainList
                    ? typeUtil.wrapExtendsIfNotFinal(typeUtil.LIST, mainValue)
                    : mainValue;
            String valueRender = gen.valueSiteTypeName(anc.getMetaValueType());
            String bulkParamType = ModelObjectGenerator.T_LIST + "<? extends " + valueRender + ">";
            String mainBulkAdd = mainItemMeta
                    ? mainProp.getOperationName(mainList
                            ? JavaPojoPropertyOperationType.ADD_VALUE
                            : JavaPojoPropertyOperationType.SET_VALUE)
                    : (mainList ? mainAdd : mainSet);
            String mainBulkSet = mainItemMeta
                    ? mainProp.getOperationName(JavaPojoPropertyOperationType.SET_VALUE)
                    : mainSet;
            String ancAddValue = anc.getOperationName(JavaPojoPropertyOperationType.ADD_VALUE);
            String ancSetValue = anc.getOperationName(JavaPojoPropertyOperationType.SET_VALUE);
            // The bulk param is DECLARED `List<? extends V>` unconditionally (the
            // bulkParamType render above — upstream's law), so it must be BOUND that
            // way too: with a FINAL V the old wrapExtendsIfNotFinal binding collapsed
            // the pair to List<V>→List<V> identity and masked upstream's
            // wildcard→non-wildcard `new ArrayList(«it»)` copy (the #412 recorded
            // corner — golden-witnessed by pojo-bulk-meta-kind at PR #422).
            appendArm(body, builderRetType, ancAddValue, bulkParamType, pluralParam, false,
                    new BoundParam(
                            typeUtil.wrapExtends(typeUtil.LIST, anc.getMetaValueType()),
                            null),
                    bulkTarget, null, mainBulkAdd, "");
            appendArm(body, builderRetType, ancSetValue, bulkParamType, pluralParam, false,
                    new BoundParam(
                            typeUtil.wrapExtends(typeUtil.LIST, anc.getMetaValueType()),
                            null),
                    bulkTarget, null, mainBulkSet, "");
        }
    }

    /** The ancestor list-param render — the fork's proven model/basic split. */
    private String listParamRender(JavaPojoProperty anc) {
        String item = gen.valueSiteTypeName(itemType(anc));
        return typeUtil.isRosettaModelObject(itemType(anc))
                ? ModelObjectGenerator.T_LIST + "<? extends " + item + ">"
                : ModelObjectGenerator.T_LIST + "<" + item + ">";
    }

    private void appendArm(StringBuilder body, String builderRetType, String methodName,
                           String paramTypeRender, String paramName, boolean ignoreAnnotations,
                           BoundParam boundParam, JavaType targetType, JavaType targetMetaValue,
                           String delegateName, String delegateSuffix) {
        appendArm(body, builderRetType, methodName, paramTypeRender, paramName,
                ignoreAnnotations, boundParam, targetType, targetMetaValue,
                delegateName, delegateSuffix, "");
    }

    private void appendArm(StringBuilder body, String builderRetType, String methodName,
                           String paramTypeRender, String paramName, boolean ignoreAnnotations,
                           BoundParam boundParam, JavaType targetType, JavaType targetMetaValue,
                           String delegateName, String delegateSuffix, String extraParams) {
        JavaStatementScope scope = bodyScope();
        // The parameter registers ONCE in the body scope (its hand-rendered name is
        // already class-scope-escaped by the existing `_`-prefix/plural conventions);
        // coercion locals then resolve against it plus the class fields.
        JavaVariable paramVar = new JavaVariable(
                scope.createUniqueIdentifier(paramName), boundParam.type());
        JavaStatementBuilder coerced = coerceShapes(paramVar,
                boundParam.type(), boundParam.metaValue(),
                targetType, targetMetaValue, true, scope);
        JavaStatementBuilder delegated = coerced.collapseToSingleExpression(scope)
                .mapExpression(it -> compose(null,
                        delegateName + "(", it, delegateSuffix + ")"));
        if (ignoreAnnotations) {
            body.append("\t\t@").append(ModelObjectGenerator.T_ROSETTA_IGNORE).append("\n");
            body.append("\t\t@").append(ModelObjectGenerator.T_RUNE_IGNORE).append("\n");
        }
        body.append("\t\t@Override\n");
        body.append("\t\tpublic ").append(builderRetType).append(" ").append(methodName)
            .append("(").append(paramTypeRender).append(" ").append(paramName)
            .append(extraParams).append(") {\n");
        body.append(renderBody(delegated.completeAsReturn()));
        body.append("\t\t}\n");
        body.append("\t\t\n");
    }

    /** A setter-arm parameter: its Java type plus its meta value type (when wrapped). */
    private record BoundParam(JavaType type, JavaType metaValue) { }

    // =====================================================================
    // The coercion engine (upstream TypeCoercionService, POJO-seat pairs)
    // =====================================================================

    /** Coerce between two PROPERTY shapes (list-ness + meta-ness carried per side). */
    private JavaStatementBuilder coerce(JavaExpression expr, JavaPojoProperty source,
                                        JavaPojoProperty target, boolean narrowing,
                                        JavaStatementScope scope) {
        return coerceShapes(expr, source.getType(), source.getMetaValueType(),
                target.getType(), target.getMetaValueType(), narrowing, scope);
    }

    private JavaStatementBuilder coerceShapes(JavaExpression expr,
                                              JavaType sourceType, JavaType sourceMetaValue,
                                              JavaType targetType, JavaType targetMetaValue,
                                              boolean narrowing, JavaStatementScope scope) {
        boolean srcList = typeUtil.isList(sourceType);
        boolean tgtList = typeUtil.isList(targetType);
        JavaType srcItem = srcList ? typeUtil.getItemType(sourceType) : sourceType;
        JavaType tgtItem = tgtList ? typeUtil.getItemType(targetType) : targetType;
        if (!srcList && !tgtList) {
            return itemToItemFull(expr, srcItem, sourceMetaValue, tgtItem, targetMetaValue,
                    narrowing, scope);
        }
        if (!srcList) {
            return itemToList(expr, srcItem, sourceMetaValue, targetType, tgtItem,
                    targetMetaValue, narrowing, scope);
        }
        if (!tgtList) {
            // List source → single target: the MapperC.of(x).get() unwrap head, then
            // the item conversion (upstream wrapperToItem + getListToItemConversionExpression).
            JavaExpression unwrapped = compose(srcItem, ModelObjectGenerator.T_MAPPER_C + ".of(", expr, ").get()");
            return itemToItemFull(unwrapped, srcItem, sourceMetaValue, tgtItem,
                    targetMetaValue, narrowing, scope);
        }
        // List → list: item conversion inside the multiline stream form (upstream
        // wrapperToWrapper + getListItemConversionExpression); identity when the items
        // need no conversion (the wildcard wrapper pair is assignment-compatible).
        Optional<Function<JavaExpression, JavaStatementBuilder>> conv =
                itemConversion(srcItem, sourceMetaValue, tgtItem, targetMetaValue,
                        narrowing, scope);
        if (conv.isEmpty()) {
            // Items agree — but an immutable `List<? extends T>` source must still
            // COPY into the mutable `List<T>` a final-T target demands (upstream's
            // wrapper conversion, TypeCoercionService L342-344: "Case immutable
            // List<? extends T> to mutable List<T>" → `new ArrayList(«it»)`, RAW —
            // the pojo-bulk-meta-kind golden's bulk arms, PR #422). Identity only
            // when the wildcard-ness agrees too (ArrayList is import-gated by the
            // list-prop rule in ModelObjectGenerator — every carrier has list props).
            if (expr.getExpressionType() != null
                    && typeUtil.hasWildcardArgument(expr.getExpressionType())
                    && !typeUtil.hasWildcardArgument(targetType)) {
                return compose(targetType, "new " + ModelObjectGenerator.T_ARRAY_LIST + "(", expr, ")");
            }
            return expr;
        }
        String lambdaParam = scope.disambiguate(
                JavaNamingUtil.toFirstLower(srcItem.getSimpleName()));
        JavaStatementBuilder resultItem = conv.get()
                .apply(JavaExpression.from(lambdaParam, srcItem));
        JavaLambdaBody lambdaBody = resultItem.toLambdaBody();
        return compose(targetType,
                expr, ".stream()\n\t.<" + gen.valueSiteTypeName(tgtItem) + ">map("
                        + lambdaParam + " -> ",
                lambdaBody,
                ")\n\t.collect(" + ModelObjectGenerator.T_COLLECTORS + ".toList())\n");
    }

    /** Single item → list target (upstream itemToWrapper, List branch). */
    private JavaStatementBuilder itemToList(JavaExpression expr,
                                            JavaType srcItem, JavaType sourceMetaValue,
                                            JavaType targetListType, JavaType tgtItem,
                                            JavaType targetMetaValue, boolean narrowing,
                                            JavaStatementScope scope) {
        // Upstream's isMetaToItemConversion: a meta-wrapped source unwrapping into a
        // plain-item list fuses the value null-check into the condition.
        boolean fused = sourceMetaValue != null && targetMetaValue == null;
        Optional<Function<JavaExpression, JavaStatementBuilder>> inner =
                itemConversion(srcItem, sourceMetaValue, tgtItem, targetMetaValue,
                        narrowing, scope);
        Function<JavaExpression, JavaStatementBuilder> wrap = x -> compose(targetListType,
                ModelObjectGenerator.T_COLLECTIONS + ".singletonList(", x, ")");
        Function<JavaExpression, JavaStatementBuilder> total = inner
                .<Function<JavaExpression, JavaStatementBuilder>>map(conv ->
                        e -> conv.apply(e).mapExpression(wrap::apply))
                .orElse(wrap);
        JavaExpression empty = JavaExpression.from(
                ModelObjectGenerator.T_COLLECTIONS + ".<" + gen.valueSiteTypeName(tgtItem) + ">emptyList()",
                targetListType);
        return convertNullSafe(expr, total, empty, fused, scope);
    }

    /** Upstream itemToItem: conversion (if any) wrapped null-safe; identity otherwise. */
    private JavaStatementBuilder itemToItemFull(JavaExpression expr,
                                                JavaType srcItem, JavaType sourceMetaValue,
                                                JavaType tgtItem, JavaType targetMetaValue,
                                                boolean narrowing, JavaStatementScope scope) {
        return itemConversion(srcItem, sourceMetaValue, tgtItem, targetMetaValue,
                narrowing, scope)
                .map(conv -> convertNullSafe(expr, conv,
                        emptyForItem(tgtItem, targetMetaValue), false, scope))
                .orElse(expr);
    }

    /**
     * The item-conversion table (upstream {@code getItemConversion}, POJO-seat pairs).
     * Empty means "no conversion needed" (identity or assignment-compatible upcast).
     */
    private Optional<Function<JavaExpression, JavaStatementBuilder>> itemConversion(
            JavaType srcItem, JavaType sourceMetaValue,
            JavaType tgtItem, JavaType targetMetaValue,
            boolean narrowing, JavaStatementScope scope) {
        if (srcItem.equals(tgtItem)) {
            return Optional.empty();
        }
        if (isNumber(srcItem) && isNumber(tgtItem)) {
            return Optional.of(e -> numberConversion(e, srcItem, tgtItem));
        }
        if (sourceMetaValue != null) {
            return Optional.of(e -> metaToItem(e, sourceMetaValue, tgtItem,
                    targetMetaValue, narrowing, scope));
        }
        if (targetMetaValue != null) {
            return Optional.of(e -> itemToMeta(e, srcItem, tgtItem, targetMetaValue,
                    narrowing, scope));
        }
        if (narrowing && typeUtil.isRosettaModelObject(srcItem)
                && typeUtil.isRosettaModelObject(tgtItem)) {
            // The setter direction: the ancestor's item strictly widens the main's (the
            // DSL restriction law), so an unequal model pair narrows via the
            // instanceof/class.cast guard. In the widening (getter) direction the same
            // pair is an upcast — assignment-compatible, no conversion (upstream decides
            // via isSubtypeOf; the direction fixes the verdict here).
            return Optional.of(e -> downcast(e, tgtItem));
        }
        return Optional.empty();
    }

    /** Meta unwrap: {@code e.getValue()} + the further item conversion, null-safe. */
    private JavaStatementBuilder metaToItem(JavaExpression e, JavaType sourceMetaValue,
                                            JavaType tgtItem, JavaType targetMetaValue,
                                            boolean narrowing, JavaStatementScope scope) {
        JavaExpression unwrapped = compose(sourceMetaValue, e, ".getValue()");
        return itemToItemFull(unwrapped, sourceMetaValue, null, tgtItem, targetMetaValue,
                narrowing, scope);
    }

    /** Meta wrap: convert to the value shape (if needed), then {@code builder().setValue(…).build()}. */
    private JavaStatementBuilder itemToMeta(JavaExpression e, JavaType srcItem,
                                            JavaType tgtWrapper, JavaType targetMetaValue,
                                            boolean narrowing, JavaStatementScope scope) {
        String wrapperName = gen.valueSiteTypeName(tgtWrapper);
        Function<JavaExpression, JavaExpression> wrap = x -> compose(tgtWrapper,
                wrapperName + ".builder().setValue(", x, ").build()");
        return itemConversion(srcItem, null, targetMetaValue, null, narrowing, scope)
                .map(conv -> conv.apply(e).mapExpression(wrap::apply))
                .orElseGet(() -> wrap.apply(e));
    }

    /** Model downcast: {@code x instanceof Y ? Y.class.cast(x) : null} (null-safe form). */
    private JavaStatementBuilder downcast(JavaExpression e, JavaType tgtItem) {
        String typeName = gen.valueSiteTypeName(tgtItem);
        return new JavaConditionalExpression(
                compose(JavaPrimitiveType.BOOLEAN, e, " instanceof " + typeName),
                compose(tgtItem, typeName + ".class.cast(", e, ")"),
                JavaLiteral.NULL,
                typeUtil);
    }

    /**
     * The number-conversion ladder (upstream {@code getNumberConversionExpression},
     * POJO-seat = boxed types, {@code throwOnFail=false}). Widening is plain; narrowing
     * guards with the round-trip check and falls back to {@code null}. The received
     * expression is always a parameter or a {@code convertNullSafe}-bound local at this
     * seat, so upstream's {@code declareAsVariable(true, "i"/"d")} reuse resolves to
     * using it in place.
     */
    private JavaStatementBuilder numberConversion(JavaExpression e, JavaType src, JavaType tgt) {
        // v3.2 seat 11 round 1 (cq SF-2): the java.math tokens are first-claim sentinels like every other library
        // type the POJO writes (D50 point 1) - resolved with the class text by buildModel; no vendored or oracle
        // carrier collides here, so the swap is byte-inert on every golden - WITNESSED at the hold-out byte bar by the three
        // goldens that carry these arms (round 2's cq SF-2 had read the witness as vacuous - the vendored corpus has no
        // carrier, the hold-out battery has three; round 3's spec NIT-1 / rule6 NIT-3 split the label per golden): SIX of
        // the ten arms are byte-compared there - `BigInteger.valueOf(integer)`, `BigDecimal.valueOf(integer)` and the two
        // `.intValue()` round-trip guards in pojo-inheritance/Foo3.java, `BigDecimal.valueOf(integer)` and its guard in
        // pojo-bulk-value-narrow/Child.java, the BigInteger-to-BigDecimal constructor arm and its round-trip guard in
        // pojo-inheritance/Foo2.java; the two long-source guards (`longValue()).equals(` / `longValue()).compareTo(`)
        // occur in no golden, and the two long-source widenings render the integer arms' text, so no golden can tell
        // them apart - and by the rings at every chain
        if (typeUtil.isInteger(src)) {
            if (typeUtil.isLong(tgt)) {
                return compose(tgt, e, ".longValue()");
            }
            if (typeUtil.isBigInteger(tgt)) {
                return compose(tgt, ModelObjectGenerator.T_BIG_INTEGER + ".valueOf(", e, ")");
            }
            return compose(tgt, ModelObjectGenerator.T_BIG_DECIMAL + ".valueOf(", e, ")");
        }
        if (typeUtil.isLong(src)) {
            if (typeUtil.isInteger(tgt)) {
                return guarded(compose(JavaPrimitiveType.BOOLEAN,
                                e, " <= Integer.MAX_VALUE && ", e, " >= Integer.MIN_VALUE"),
                        compose(tgt, "(int) ", e));
            }
            if (typeUtil.isBigInteger(tgt)) {
                return compose(tgt, ModelObjectGenerator.T_BIG_INTEGER + ".valueOf(", e, ")");
            }
            return compose(tgt, ModelObjectGenerator.T_BIG_DECIMAL + ".valueOf(", e, ")");
        }
        if (typeUtil.isBigInteger(src)) {
            if (typeUtil.isInteger(tgt)) {
                return guarded(compose(JavaPrimitiveType.BOOLEAN,
                                ModelObjectGenerator.T_BIG_INTEGER + ".valueOf(", e, ".intValue()).equals(", e, ")"),
                        compose(tgt, e, ".intValue()"));
            }
            if (typeUtil.isLong(tgt)) {
                return guarded(compose(JavaPrimitiveType.BOOLEAN,
                                ModelObjectGenerator.T_BIG_INTEGER + ".valueOf(", e, ".longValue()).equals(", e, ")"),
                        compose(tgt, e, ".longValue()"));
            }
            return compose(tgt, "new " + ModelObjectGenerator.T_BIG_DECIMAL + "(", e, ")");
        }
        // BigDecimal source
        if (typeUtil.isInteger(tgt)) {
            return guarded(compose(JavaPrimitiveType.BOOLEAN,
                            ModelObjectGenerator.T_BIG_DECIMAL + ".valueOf(", e, ".intValue()).compareTo(", e, ") == 0"),
                    compose(tgt, e, ".intValue()"));
        }
        if (typeUtil.isLong(tgt)) {
            return guarded(compose(JavaPrimitiveType.BOOLEAN,
                            ModelObjectGenerator.T_BIG_DECIMAL + ".valueOf(", e, ".longValue()).compareTo(", e, ") == 0"),
                    compose(tgt, e, ".longValue()"));
        }
        return guarded(compose(JavaPrimitiveType.BOOLEAN,
                        "new " + ModelObjectGenerator.T_BIG_DECIMAL + "(", e, ".toBigInteger()).compareTo(", e, ") == 0"),
                compose(tgt, e, ".toBigInteger()"));
    }

    /** A null-safe narrowing guard: {@code condition ? conversion : null}. */
    private JavaConditionalExpression guarded(JavaExpression condition,
                                              JavaExpression conversion) {
        return new JavaConditionalExpression(condition, conversion, JavaLiteral.NULL, typeUtil);
    }

    /**
     * Upstream {@code convertNullSafe}: bind the expression (a {@link JavaVariable} —
     * parameter or field — is reused in place; anything else binds a {@code final}
     * local named after its type, class-scope-escaped), then guard: null (optionally
     * fused with the meta value null-check) → the target's empty representation, else
     * the conversion. A plain-expression conversion stays a ternary
     * ({@link JavaConditionalExpression}); a branching one upgrades to
     * {@link JavaIfThenElseBuilder}.
     */
    private JavaStatementBuilder convertNullSafe(JavaExpression expr,
                                                 Function<JavaExpression, JavaStatementBuilder> conversion,
                                                 JavaExpression empty, boolean fusedMetaValueCheck,
                                                 JavaStatementScope scope) {
        JavaStatementBuilder bound = expr instanceof JavaVariable ? expr
                : expr.declareAsVariable(true,
                        JavaNamingUtil.toFirstLower(expr.getExpressionType().getSimpleName()),
                        scope);
        return bound.mapExpression(varExpr -> {
            JavaExpression condition = fusedMetaValueCheck
                    ? compose(JavaPrimitiveType.BOOLEAN,
                            varExpr, " == null || ", varExpr, ".getValue() == null")
                    : compose(JavaPrimitiveType.BOOLEAN, varExpr, " == null");
            JavaStatementBuilder converted = conversion.apply(varExpr);
            if (converted instanceof JavaExpression convertedExpr) {
                return new JavaConditionalExpression(condition, empty, convertedExpr, typeUtil);
            }
            return new JavaIfThenElseBuilder(condition, empty, converted, typeUtil);
        });
    }

    /** The empty representation of a single-item target (upstream {@code empty}). */
    private JavaExpression emptyForItem(JavaType tgtItem, JavaType targetMetaValue) {
        if (targetMetaValue != null) {
            return JavaExpression.from(
                    gen.valueSiteTypeName(tgtItem) + ".builder().build()", tgtItem);
        }
        return JavaLiteral.NULL;
    }

    // =====================================================================
    // Rendering helpers
    // =====================================================================

    /**
     * A LAZY expression composition: string parts append verbatim; {@link JavaExpression}
     * / {@link JavaLambdaBody} parts render at final render time — after every scope
     * identifier exists, honoring {@code GeneratorScope}'s two-phase build/render
     * contract (an eager {@code renderToString} mid-build would close the scope and
     * poison later {@code createIdentifier} calls).
     */
    private static JavaExpression compose(JavaType type, Object... parts) {
        return new JavaExpression(type) {
            @Override
            public void render(StringBuilder sb) {
                for (Object part : parts) {
                    if (part instanceof JavaExpression expr) {
                        expr.render(sb);
                    } else if (part instanceof JavaLambdaBody lambda) {
                        lambda.render(sb);
                    } else {
                        sb.append(part);
                    }
                }
            }
        };
    }

    /** {@code .toBuilder()} on a model-typed expression; identity otherwise. */
    private JavaExpression toBuilder(JavaExpression e) {
        if (e.getExpressionType() != null
                && typeUtil.isRosettaModelObject(e.getExpressionType())) {
            return compose(e.getExpressionType(), e, ".toBuilder()");
        }
        return e;
    }

    private JavaVariable fieldVar(JavaPojoProperty prop) {
        GeneratedIdentifier id = fieldIds.get(ModelObjectGenerator.fieldName(prop));
        return new JavaVariable(id, prop.getType());
    }

    /** A method-body scope whose parent carries the class's field identifiers. */
    private JavaStatementScope bodyScope() {
        return new JavaStatementScope("pojo-compat-body", classScope);
    }

    /** Render a completed statement at method-body depth (3 tabs), brace-aware. */
    private static String renderBody(JavaStatement statement) {
        var flat = new StringBuilder();
        statement.render(flat);
        return reindent(flat.toString(), 3);
    }

    /**
     * Re-indent a flat statement render: every line gets {@code base + braceDepth}
     * tabs; a leading {@code }} dedents its own line — TAB-TOLERANTLY, so a braced
     * lambda's {@code \t}} close (the {@code toLambdaBody} shape, whose embedded tab
     * is the stream-continuation level) dedents exactly like a bare {@code }} and
     * lands AT the continuation level (the golden {@code })}, PR #422); tabs already
     * embedded in a line (the multiline stream continuations) are preserved after
     * the computed prefix. The rendered grammar is closed (no string literals
     * containing braces).
     */
    private static String reindent(String flat, int baseTabs) {
        var out = new StringBuilder();
        int depth = 0;
        for (String line : flat.split("\n", -1)) {
            if (line.isEmpty()) {
                continue;
            }
            int lineDepth = line.stripLeading().startsWith("}") ? depth - 1 : depth;
            out.append("\t".repeat(baseTabs + Math.max(lineDepth, 0)))
               .append(line).append('\n');
            for (int i = 0; i < line.length(); i++) {
                char ch = line.charAt(i);
                if (ch == '{') {
                    depth++;
                } else if (ch == '}') {
                    depth--;
                }
            }
        }
        return out.toString();
    }

    private boolean isList(JavaPojoProperty prop) {
        return typeUtil.isList(prop.getType());
    }

    private JavaType itemType(JavaPojoProperty prop) {
        return isList(prop) ? typeUtil.getItemType(prop.getType()) : prop.getType();
    }

    private boolean isNumber(JavaType t) {
        return typeUtil.isInteger(t) || typeUtil.isLong(t)
                || typeUtil.isBigInteger(t) || typeUtil.isBigDecimal(t);
    }
}
