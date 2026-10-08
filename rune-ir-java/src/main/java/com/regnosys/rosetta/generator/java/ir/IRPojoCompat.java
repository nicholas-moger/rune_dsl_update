package com.regnosys.rosetta.generator.java.ir;

import java.util.LinkedHashMap;
import java.util.List;
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
import com.rosetta.util.types.JavaPrimitiveType;
import com.rosetta.util.types.JavaType;

/**
 * THE COMPAT MEMBERS OF A SPECIALIZED CHAIN, WRITTEN FROM THE IR ALONE (v3.3 seat 9, PR #645 commit 10) - the PORT
 * of {@code rune-java-generator/.../java/object/PojoCompatEmitter.java} ({@code CE}, 881 lines at {@code ef66b7b9c}),
 * method for method, under the same names, at the same three call sites. {@code CE}'s own class javadoc
 * ({@code :28-112}) is the algebra's specification - the three member families, the coercion table, the
 * cardinality arms and the {@code #412} recorded corner healed at PR #422 - and it is not restated here; what IS
 * stated here is every place this port DIFFERS, because a port whose differences are not written down is a
 * rewrite.
 *
 * <p><b>THE DIFFERENCES, exhaustively</b> (the seat contract's own table):
 * <ol>
 *   <li><b>{@code JavaPojoProperty} becomes {@link Shape}</b> - the ONE view an
 *       {@link IRPropertyModel.IRProperty} and an {@link IRPropertyModel.IRParentLink} both present to the
 *       algebra: a rendered Java type, its cardinality, its item's kind, its bare meta value type and that
 *       value's kind, its two compatibility names and its getter-override verdict. Every one of those is a
 *       RECONCILED fact - the property's since PR #645 commits 5, 8 and 9, the RUNG's since commit 10 - so no
 *       decision below reads the old generator's object or the AST.</li>
 *   <li><b>the chain walk</b> - {@code CE} climbs {@code JavaPojoProperty.getParentProperty()}; this port walks
 *       {@code prop.parentChain()}, which is the SAME climb, nearest first, carried as data. {@code CE} reads
 *       {@code cur.getterOverridesParentGetter()} where {@code cur} walks {@code prop}, its parent, its
 *       grandparent … ({@code :172-188}, {@code :207-216}); this port reproduces THAT sequence exactly - the
 *       MAIN property's own flag for the first hop, then each RUNG's own for the hops after it. That is what
 *       closes commit 8's honest over-refusal at depth two and deeper: the rung now carries its own verdict.</li>
 *   <li><b>the {@code JavaType} values</b> - built by {@link IRJavaTypes} from the rendered strings and the
 *       reconciled facts, never by {@code JavaTypeTranslator}; the type table is reached only through
 *       {@link IRTypeAlgebra}. The statement algebra itself ({@code JavaStatementBuilder} and friends,
 *       {@code JavaTypeJoiner}, the scopes) is GENERIC TEXT MACHINERY parameterised by those values and is
 *       REUSED AS-IS, exactly as {@code TemplateRenderer} and {@code ImportCollisionResolver} are.</li>
 *   <li><b>the renders</b> - {@code gen.interfaceGetterType} / {@code builderGetterType} /
 *       {@code builderSingleType} / {@code valueSiteTypeName} / {@code ModelObjectGenerator.fieldName} /
 *       {@code hasIndexPropertyInScope} become {@link IRDataTypeEmitter}'s own laws over the rendered type and
 *       the facts, each of them the SAME law the main property's own members are written from (LAW 69: the
 *       three type renders gained an overload over {@code (rendered, multi, itemIsModelObject)} that BOTH the
 *       property and the rung call).</li>
 *   <li><b>the library tokens</b> - {@code ModelObjectGenerator}'s {@code T_*} constants become this emitter's
 *       own copies ({@code IRDataTypeEmitter.T_*}), as every other section's do.</li>
 * </ol>
 *
 * <p>Everything else - the scopes, {@code compose}, {@code renderBody}, {@code reindent}, {@code guarded},
 * {@code convertNullSafe}, {@code emptyForItem}, {@code toBuilder}, {@code isNumber}, {@code numberConversion},
 * {@code itemConversion}, {@code metaToItem}, {@code itemToMeta}, {@code downcast}, {@code itemToList},
 * {@code itemToItemFull}, {@code coerceShapes}, {@code coerce}, {@code BoundParam}, both {@code appendArm}
 * overloads and {@code listParamRender} - is VERBATIM, with the same names and in the same order, so a reader can
 * diff the two files method by method.
 */
final class IRPojoCompat {

    /** The operation names {@code JavaPojoProperty.getOperationName} builds ({@code JavaPojoPropertyOperationType}). */
    private enum Op {
        GET("get", "", true),
        GET_OR_CREATE("getOrCreate", "", true),
        SET("set", "", false),
        SET_VALUE("set", "Value", false),
        ADD("add", "", false),
        ADD_VALUE("add", "Value", false);

        private final String prefix;
        private final String postfix;
        private final boolean getterSide;

        Op(String prefix, String postfix, boolean getterSide) {
            this.prefix = prefix;
            this.postfix = postfix;
            this.getterSide = getterSide;
        }
    }

    private final IRDataTypeEmitter gen;
    private final IRTypeAlgebra algebra;
    /** The whole property surface - the class scope's population and the {@code index} escape's ({@code CE:278}). */
    private final List<IRPropertyModel.IRProperty> allProps;
    /** Class-level scope holding every builder/impl field identifier (the escape context). */
    private final JavaStatementScope classScope;
    private final Map<String, GeneratedIdentifier> fieldIds = new LinkedHashMap<>();

    IRPojoCompat(IRDataTypeEmitter gen, IRTypeAlgebra algebra,
                 List<IRPropertyModel.IRProperty> allProps) {
        this.gen = gen;
        this.algebra = algebra;
        this.allProps = List.copyOf(allProps);
        this.classScope = new JavaStatementScope("pojo-compat-class", null);
        for (IRPropertyModel.IRProperty p : this.allProps) {
            String fld = IRDataTypeEmitter.fieldName(p);
            fieldIds.computeIfAbsent(fld, classScope::createUniqueIdentifier);
        }
    }

    /**
     * Whether any property of the pojo carries a specialization chain ({@code CE:133-135}). It is the gate
     * {@code ModelObjectGenerator:684-687} and {@code :707-710} build the compat emitter at all under, so
     * sections 11 and 12 both read THIS declaration rather than restating the predicate (LAW 69).
     */
    static boolean anySpecialized(List<IRPropertyModel.IRProperty> allProps) {
        return allProps.stream().anyMatch(p -> !p.parentChain().isEmpty());
    }

    // =====================================================================
    // Impl-side derived getters (CE:164-190)
    // =====================================================================

    /**
     * The impl-side derived-getter walk for one property ({@code CE:171-190}): one
     * {@code @Override @RosettaIgnore @RuneIgnore} getter per ancestor whose getter the specialized getter does
     * NOT override (the {@code getterOverridesParentGetter} skip law - a covariant link keeps the name and needs
     * no compat member), the body coercing the field to the ancestor's INTERFACE shape.
     *
     * <p>{@code CE:172-188} holds a cursor {@code cur} that starts at {@code prop} and climbs; the flag it tests
     * at {@code :175} is the CURSOR's, not the parent's. This loop reproduces that sequence exactly: the main
     * property's own flag decides the first rung, and each rung's own flag - the fact PR #645 commit 10 added to
     * {@link IRPropertyModel.IRParentLink} - decides the rung after it.
     */
    void appendImplDerivedGetters(StringBuilder body, IRPropertyModel.IRProperty prop) {
        if (prop.parentChain().isEmpty()) {
            return;   // the walk's own `while (cur.getParentProperty() != null)` never turns over
        }
        Shape main = shape(prop);
        boolean curOverrides = prop.getterOverridesParentGetter();   // CE:172 - the cursor starts at the property
        for (IRPropertyModel.IRParentLink rung : prop.parentChain()) {
            Shape parent = shape(rung);
            if (!curOverrides) {   // CE:175 - a covariant link keeps the name and needs no compat member
                JavaStatementScope scope = bodyScope();
                JavaStatementBuilder coerced = coerce(fieldVar(main), main, parent, false, scope);
                body.append("\t\t@Override\n");
                body.append("\t\t@").append(IRDataTypeEmitter.T_ROSETTA_IGNORE).append("\n");
                body.append("\t\t@").append(IRDataTypeEmitter.T_RUNE_IGNORE).append("\n");
                body.append("\t\tpublic ").append(interfaceGetterType(parent)).append(" ")
                    .append(operationName(parent, Op.GET))
                    .append("() {\n");
                body.append(renderBody(coerced.completeAsReturn()));
                body.append("\t\t}\n");
                body.append("\t\t\n");
            }
            curOverrides = rung.getterOverridesParentGetter();
        }
    }

    // =====================================================================
    // Builder-side derived getters + getOrCreate compat (CE:192-293)
    // =====================================================================

    /**
     * The builder-side derived-getter walk for one property ({@code CE:205-218}): per non-overridden ancestor, a
     * builder-flavored compat getter (coercion + {@code .toBuilder()} mapping; a list-shaped ancestor over a
     * single field renders the {@code emptyList}/{@code singletonList} pair with the BUILDER-typed witness), plus
     * - when the ancestor is model-typed - the {@code getOrCreate} compat delegate through the specialized
     * accessor. The cursor law is {@link #appendImplDerivedGetters}'s exactly.
     */
    void appendBuilderDerivedGetters(StringBuilder body, IRPropertyModel.IRProperty prop) {
        if (prop.parentChain().isEmpty()) {
            return;   // the walk's own `while (cur.getParentProperty() != null)` never turns over
        }
        Shape main = shape(prop);
        boolean curOverrides = prop.getterOverridesParentGetter();   // CE:207 - the same cursor, builder side
        for (IRPropertyModel.IRParentLink rung : prop.parentChain()) {
            Shape parent = shape(rung);
            if (!curOverrides) {   // CE:210 - the same skip law on the builder side
                appendBuilderDerivedGetter(body, main, parent);
                if (algebra.isRosettaModelObject(parent.type)) {
                    appendBuilderGetOrCreateCompat(body, main, parent);
                }
            }
            curOverrides = rung.getterOverridesParentGetter();
        }
    }

    /** {@code CE:220-268}, verbatim over the two shapes. */
    private void appendBuilderDerivedGetter(StringBuilder body, Shape prop, Shape parent) {
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
            String lambdaBody = algebra.isRosettaModelObject(itemType(parent))
                    ? lambdaParam + ".toBuilder()" : lambdaParam;
            result = coerced.mapExpression(it -> compose(parent.type,
                    it, ".stream().map(" + lambdaParam + " -> " + lambdaBody
                            + ").collect(" + IRDataTypeEmitter.T_COLLECTORS + ".toList())"));
        } else if (parentList) {
            // List-shaped ancestor over a single field: coerce to the ancestor's ITEM
            // type, then null-literal → builder-typed emptyList / value → toBuilder +
            // singletonList (upstream L226-236).
            JavaStatementBuilder itemCoerced = itemToItemFull(field,
                    itemType(prop), prop.metaValueType,
                    itemType(parent), parent.metaValueType, false, scope);
            result = itemCoerced.mapExpression(it -> {
                if (it == JavaLiteral.NULL) {
                    return JavaExpression.from(
                            IRDataTypeEmitter.T_COLLECTIONS + ".<" + builderSingleType(parent) + ">emptyList()",
                            parent.type);
                }
                return compose(parent.type,
                        IRDataTypeEmitter.T_COLLECTIONS + ".singletonList(", toBuilder(it), ")");
            });
        } else {
            // Single-shaped ancestor: plain coercion + toBuilder on the non-null result.
            result = coerce(field, prop, parent, false, scope)
                    .mapExpressionIfNotNull(this::toBuilder);
        }
        body.append("\t\t@Override\n");
        body.append("\t\t@").append(IRDataTypeEmitter.T_ROSETTA_IGNORE).append("\n");
        body.append("\t\t@").append(IRDataTypeEmitter.T_RUNE_IGNORE).append("\n");
        body.append("\t\tpublic ").append(builderGetterType(parent)).append(" ")
            .append(operationName(parent, Op.GET)).append("() {\n");
        body.append(renderBody(result.completeAsReturn()));
        body.append("\t\t}\n");
        body.append("\t\t\n");
    }

    /** {@code CE:270-293}, verbatim; {@code hasIndexPropertyInScope(pojo)} becomes the emitter's own scope law. */
    private void appendBuilderGetOrCreateCompat(StringBuilder body, Shape prop, Shape parent) {
        JavaStatementScope scope = bodyScope();
        String getOrCreateName = operationName(parent, Op.GET_OR_CREATE);
        String delegateName = operationName(prop, Op.GET_OR_CREATE);
        boolean parentIsList = isList(parent);
        // Upstream L249/L259: the delegate call passes the index through only when the
        // SPECIALIZED property is itself a list (the single-side getOrCreate takes none).
        String idxParam = IRDataTypeEmitter.hasPropertyInScope(allProps, "index") ? "_index" : "index";
        String delegateArgs = isList(prop) && parentIsList ? idxParam : "";
        JavaExpression call = JavaExpression.from(
                delegateName + "(" + delegateArgs + ")", itemType(prop));
        JavaStatementBuilder coerced = itemToItemFull(call,
                itemType(prop), prop.metaValueType,
                itemType(parent), parent.metaValueType, false, scope)
                .mapExpressionIfNotNull(this::toBuilder);
        body.append("\t\t@Override\n");
        body.append("\t\tpublic ").append(builderSingleType(parent)).append(" ")
            .append(getOrCreateName).append("(")
            .append(parentIsList ? "int " + idxParam : "").append(") {\n");
        body.append(renderBody(coerced.completeAsReturn()));
        body.append("\t\t}\n");
        body.append("\t\t\n");
    }

    // =====================================================================
    // Ancestor setter arms (CE:295-449)
    // =====================================================================

    /**
     * All ancestor setter arms for one specialized property, nearest ancestor first ({@code CE:309-319}). Each arm
     * dispatches on the ANCESTOR's shape: a single-shaped ancestor contributes SET (+ SET_VALUE when
     * meta-wrapped); a list-shaped ancestor contributes ADD / ADD-idx / (ADD_VALUE pair when its item is
     * meta-wrapped) / ADD-list / SET-list (+ ADD_VALUE/SET_VALUE bulk when meta-wrapped). Bodies coerce to the
     * MAIN property's shape (narrowing, null-safe) and delegate to the main setter family.
     *
     * <p>This walk has NO getter-override test at all ({@code CE:311-318}) and is not gated on the builder's
     * extension ({@code ModelObjectGenerator:1266-1270}) - which is why it was a Section of its own while it
     * refused, and why EVERY rung of every non-empty chain reaches it now.
     */
    void appendAncestorSetterArms(StringBuilder body, IRPropertyModel.IRProperty mainProp,
                                  String builderRetType, Set<String> siblingFieldNames) {
        if (mainProp.parentChain().isEmpty()) {
            return;   // CE:311 - the ancestor loop never turns over for an unspecialized property
        }
        Shape main = shape(mainProp);
        for (IRPropertyModel.IRParentLink rung : mainProp.parentChain()) {
            Shape anc = shape(rung);
            if (isList(anc)) {
                appendListAncestorArms(body, main, anc, builderRetType, siblingFieldNames);
            } else {
                appendSingleAncestorArms(body, main, anc, builderRetType);
            }
        }
    }

    /** {@code CE:321-348}, verbatim. */
    private void appendSingleAncestorArms(StringBuilder body, Shape mainProp, Shape anc, String builderRetType) {
        String fld = mainProp.fieldName;
        // SET in the ancestor's shape → coerce to the main property's WHOLE shape.
        appendArm(body, builderRetType, operationName(anc, Op.SET),
                IRDataTypeEmitter.valueSiteTypeRef(itemRender(anc)), "_" + fld, true,
                new BoundParam(itemType(anc), anc.metaValueType),
                mainProp.type, mainProp.metaValueType,
                operationName(mainProp, Op.SET), "");
        // SET_VALUE in the ancestor's value shape (meta-wrapped ancestor only). The
        // coercion target and the delegate follow the MAIN property's WHOLE type
        // (upstream doSetter L554/L568: `mainPropType instanceof RJavaWithMetaValue`
        // — a LIST main is never the wrapper itself, so it takes the SET/whole-type
        // arm even when its item is meta-wrapped).
        if (anc.metaValueType != null) {
            boolean mainWholeMeta = !isList(mainProp) && mainProp.metaValueType != null;
            JavaType targetType = mainWholeMeta ? mainProp.metaValueType : mainProp.type;
            JavaType targetMeta = mainWholeMeta ? null : mainProp.metaValueType;
            String mainOp = mainWholeMeta
                    ? operationName(mainProp, Op.SET_VALUE)
                    : operationName(mainProp, Op.SET);
            appendArm(body, builderRetType,
                    operationName(anc, Op.SET_VALUE),
                    IRDataTypeEmitter.valueSiteTypeRef(anc.metaValueRendered), "_" + fld, false,
                    new BoundParam(anc.metaValueType, null),
                    targetType, targetMeta, mainOp, "");
        }
    }

    /** {@code CE:350-449}, verbatim. */
    private void appendListAncestorArms(StringBuilder body, Shape mainProp, Shape anc,
                                        String builderRetType, Set<String> siblingFieldNames) {
        String fld = mainProp.fieldName;
        boolean mainList = isList(mainProp);
        String ancItemRender = IRDataTypeEmitter.valueSiteTypeRef(itemRender(anc));
        String pluralParam = fld + "s";
        if (siblingFieldNames.contains(pluralParam)) {
            pluralParam = "_" + pluralParam;
        }
        String addName = operationName(anc, Op.ADD);
        String setName = operationName(anc, Op.SET);
        String mainAdd = operationName(mainProp, Op.ADD);
        String mainSet = operationName(mainProp, Op.SET);
        // Item-level meta-ness of the MAIN property drives the value-arm delegates
        // (upstream `mainItemType instanceof RJavaWithMetaValue`).
        boolean mainItemMeta = mainProp.metaValueType != null;
        // ADD(item) — annotated with the ignore pair; coerces to the main ITEM type
        // (upstream doSetter L325: addCoercions(mainItemType)).
        appendArm(body, builderRetType, addName, ancItemRender, "_" + fld, true,
                new BoundParam(itemType(anc), anc.metaValueType),
                itemType(mainProp), mainProp.metaValueType,
                mainList ? mainAdd : mainSet, "");
        // ADD(item, idx) — bare; the index rides only onto a list-main ADD.
        appendArm(body, builderRetType, addName,
                ancItemRender, "_" + fld, false,
                new BoundParam(itemType(anc), anc.metaValueType),
                itemType(mainProp), mainProp.metaValueType,
                mainList ? mainAdd : mainSet, mainList ? ", idx" : "",
                ", int idx");
        // ADD_VALUE pair (meta-wrapped ancestor item only): coerce the ancestor's
        // value type to the main's value shape (upstream L361: mainItemType meta ?
        // valueType : mainItemType) and delegate to the main's value-or-plain op.
        if (anc.metaValueType != null) {
            JavaType mainValue = mainItemMeta
                    ? mainProp.metaValueType : itemType(mainProp);
            String mainValueOp = mainItemMeta
                    ? operationName(mainProp, mainList ? Op.ADD_VALUE : Op.SET_VALUE)
                    : (mainList ? mainAdd : mainSet);
            String ancAddValue = operationName(anc, Op.ADD_VALUE);
            String valueRender = IRDataTypeEmitter.valueSiteTypeRef(anc.metaValueRendered);
            appendArm(body, builderRetType, ancAddValue, valueRender, "_" + fld, false,
                    new BoundParam(anc.metaValueType, null),
                    mainValue, null, mainValueOp, "");
            appendArm(body, builderRetType, ancAddValue, valueRender, "_" + fld, false,
                    new BoundParam(anc.metaValueType, null),
                    mainValue, null, mainValueOp, mainList ? ", idx" : "",
                    ", int idx");
        }
        // ADD(List) — bare; coerces the whole list to the main's WHOLE shape
        // (upstream L424: addCoercions(mainPropType)).
        appendArm(body, builderRetType, addName, listParamRender(anc), pluralParam, false,
                new BoundParam(anc.type, anc.metaValueType),
                mainProp.type, mainProp.metaValueType,
                mainList ? mainAdd : mainSet, "");
        // SET(List) — annotated with the ignore pair.
        appendArm(body, builderRetType, setName, listParamRender(anc), pluralParam, true,
                new BoundParam(anc.type, anc.metaValueType),
                mainProp.type, mainProp.metaValueType, mainSet, "");
        // Bulk ADD_VALUE/SET_VALUE pair (meta-wrapped ancestor item only); the
        // coercion target is the main's value shape, list-wrapped when main is a
        // list (upstream L491/L515).
        if (anc.metaValueType != null) {
            JavaType mainValue = mainItemMeta
                    ? mainProp.metaValueType : itemType(mainProp);
            JavaType bulkTarget = mainList
                    ? algebra.wrapExtendsIfNotFinal(mainValue)
                    : mainValue;
            String valueRender = IRDataTypeEmitter.valueSiteTypeRef(anc.metaValueRendered);
            String bulkParamType = IRDataTypeEmitter.T_LIST + "<? extends " + valueRender + ">";
            String mainBulkAdd = mainItemMeta
                    ? operationName(mainProp, mainList ? Op.ADD_VALUE : Op.SET_VALUE)
                    : (mainList ? mainAdd : mainSet);
            String mainBulkSet = mainItemMeta
                    ? operationName(mainProp, Op.SET_VALUE)
                    : mainSet;
            String ancAddValue = operationName(anc, Op.ADD_VALUE);
            String ancSetValue = operationName(anc, Op.SET_VALUE);
            // The bulk param is DECLARED `List<? extends V>` unconditionally (the
            // bulkParamType render above — upstream's law), so it must be BOUND that
            // way too: with a FINAL V the old wrapExtendsIfNotFinal binding collapsed
            // the pair to List<V>→List<V> identity and masked upstream's
            // wildcard→non-wildcard `new ArrayList(«it»)` copy (the #412 recorded
            // corner — golden-witnessed by pojo-bulk-meta-kind at PR #422).
            appendArm(body, builderRetType, ancAddValue, bulkParamType, pluralParam, false,
                    new BoundParam(algebra.wrapExtends(anc.metaValueType), null),
                    bulkTarget, null, mainBulkAdd, "");
            appendArm(body, builderRetType, ancSetValue, bulkParamType, pluralParam, false,
                    new BoundParam(algebra.wrapExtends(anc.metaValueType), null),
                    bulkTarget, null, mainBulkSet, "");
        }
    }

    /** The ancestor list-param render — the fork's proven model/basic split ({@code CE:451-457}). */
    private String listParamRender(Shape anc) {
        String item = IRDataTypeEmitter.valueSiteTypeRef(itemRender(anc));
        return algebra.isRosettaModelObject(itemType(anc))
                ? IRDataTypeEmitter.T_LIST + "<? extends " + item + ">"
                : IRDataTypeEmitter.T_LIST + "<" + item + ">";
    }

    /** {@code CE:459-466}. */
    private void appendArm(StringBuilder body, String builderRetType, String methodName,
                           String paramTypeRender, String paramName, boolean ignoreAnnotations,
                           BoundParam boundParam, JavaType targetType, JavaType targetMetaValue,
                           String delegateName, String delegateSuffix) {
        appendArm(body, builderRetType, methodName, paramTypeRender, paramName,
                ignoreAnnotations, boundParam, targetType, targetMetaValue,
                delegateName, delegateSuffix, "");
    }

    /** {@code CE:468-495}. */
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
            body.append("\t\t@").append(IRDataTypeEmitter.T_ROSETTA_IGNORE).append("\n");
            body.append("\t\t@").append(IRDataTypeEmitter.T_RUNE_IGNORE).append("\n");
        }
        body.append("\t\t@Override\n");
        body.append("\t\tpublic ").append(builderRetType).append(" ").append(methodName)
            .append("(").append(paramTypeRender).append(" ").append(paramName)
            .append(extraParams).append(") {\n");
        body.append(renderBody(delegated.completeAsReturn()));
        body.append("\t\t}\n");
        body.append("\t\t\n");
    }

    /** A setter-arm parameter: its Java type plus its meta value type (when wrapped) ({@code CE:498}). */
    private record BoundParam(JavaType type, JavaType metaValue) { }

    // =====================================================================
    // The coercion engine (CE:500-781)
    // =====================================================================

    /** Coerce between two PROPERTY shapes (list-ness + meta-ness carried per side) ({@code CE:505-510}). */
    private JavaStatementBuilder coerce(JavaExpression expr, Shape source, Shape target,
                                        boolean narrowing, JavaStatementScope scope) {
        return coerceShapes(expr, source.type, source.metaValueType,
                target.type, target.metaValueType, narrowing, scope);
    }

    /** {@code CE:512-566}. */
    private JavaStatementBuilder coerceShapes(JavaExpression expr,
                                              JavaType sourceType, JavaType sourceMetaValue,
                                              JavaType targetType, JavaType targetMetaValue,
                                              boolean narrowing, JavaStatementScope scope) {
        boolean srcList = algebra.isList(sourceType);
        boolean tgtList = algebra.isList(targetType);
        JavaType srcItem = srcList ? algebra.getItemType(sourceType) : sourceType;
        JavaType tgtItem = tgtList ? algebra.getItemType(targetType) : targetType;
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
            JavaExpression unwrapped = compose(srcItem, IRDataTypeEmitter.T_MAPPER_C + ".of(", expr, ").get()");
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
                    && algebra.hasWildcardArgument(expr.getExpressionType())
                    && !algebra.hasWildcardArgument(targetType)) {
                return compose(targetType, "new " + IRDataTypeEmitter.T_ARRAY_LIST + "(", expr, ")");
            }
            return expr;
        }
        String lambdaParam = scope.disambiguate(
                JavaNamingUtil.toFirstLower(srcItem.getSimpleName()));
        JavaStatementBuilder resultItem = conv.get()
                .apply(JavaExpression.from(lambdaParam, srcItem));
        JavaLambdaBody lambdaBody = resultItem.toLambdaBody();
        return compose(targetType,
                expr, ".stream()\n\t.<" + valueSiteTypeName(tgtItem) + ">map("
                        + lambdaParam + " -> ",
                lambdaBody,
                ")\n\t.collect(" + IRDataTypeEmitter.T_COLLECTORS + ".toList())\n");
    }

    /** Single item → list target (upstream itemToWrapper, List branch) ({@code CE:569-590}). */
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
                IRDataTypeEmitter.T_COLLECTIONS + ".singletonList(", x, ")");
        Function<JavaExpression, JavaStatementBuilder> total = inner
                .<Function<JavaExpression, JavaStatementBuilder>>map(conv ->
                        e -> conv.apply(e).mapExpression(wrap::apply))
                .orElse(wrap);
        JavaExpression empty = JavaExpression.from(
                IRDataTypeEmitter.T_COLLECTIONS + ".<" + valueSiteTypeName(tgtItem) + ">emptyList()",
                targetListType);
        return convertNullSafe(expr, total, empty, fused, scope);
    }

    /** Upstream itemToItem: conversion (if any) wrapped null-safe; identity otherwise ({@code CE:593-602}). */
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
     * The item-conversion table (upstream {@code getItemConversion}, POJO-seat pairs) ({@code CE:608-636}).
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
        if (narrowing && algebra.isRosettaModelObject(srcItem)
                && algebra.isRosettaModelObject(tgtItem)) {
            // The setter direction: the ancestor's item strictly widens the main's (the
            // DSL restriction law), so an unequal model pair narrows via the
            // instanceof/class.cast guard. In the widening (getter) direction the same
            // pair is an upcast — assignment-compatible, no conversion (upstream decides
            // via isSubtypeOf; the direction fixes the verdict here).
            return Optional.of(e -> downcast(e, tgtItem));
        }
        return Optional.empty();
    }

    /** Meta unwrap: {@code e.getValue()} + the further item conversion, null-safe ({@code CE:639-645}). */
    private JavaStatementBuilder metaToItem(JavaExpression e, JavaType sourceMetaValue,
                                            JavaType tgtItem, JavaType targetMetaValue,
                                            boolean narrowing, JavaStatementScope scope) {
        JavaExpression unwrapped = compose(sourceMetaValue, e, ".getValue()");
        return itemToItemFull(unwrapped, sourceMetaValue, null, tgtItem, targetMetaValue,
                narrowing, scope);
    }

    /** Meta wrap: convert to the value shape (if needed), then {@code builder().setValue(…).build()} ({@code CE:648-657}). */
    private JavaStatementBuilder itemToMeta(JavaExpression e, JavaType srcItem,
                                            JavaType tgtWrapper, JavaType targetMetaValue,
                                            boolean narrowing, JavaStatementScope scope) {
        String wrapperName = valueSiteTypeName(tgtWrapper);
        Function<JavaExpression, JavaExpression> wrap = x -> compose(tgtWrapper,
                wrapperName + ".builder().setValue(", x, ").build()");
        return itemConversion(srcItem, null, targetMetaValue, null, narrowing, scope)
                .map(conv -> conv.apply(e).mapExpression(wrap::apply))
                .orElseGet(() -> wrap.apply(e));
    }

    /** Model downcast: {@code x instanceof Y ? Y.class.cast(x) : null} (null-safe form) ({@code CE:660-667}). */
    private JavaStatementBuilder downcast(JavaExpression e, JavaType tgtItem) {
        String typeName = valueSiteTypeName(tgtItem);
        return new JavaConditionalExpression(
                compose(JavaPrimitiveType.BOOLEAN, e, " instanceof " + typeName),
                compose(tgtItem, typeName + ".class.cast(", e, ")"),
                JavaLiteral.NULL,
                algebra.statementAlgebraTypeUtil());
    }

    /**
     * The number-conversion ladder (upstream {@code getNumberConversionExpression}, POJO-seat = boxed types,
     * {@code throwOnFail=false}) ({@code CE:677-736}). Widening is plain; narrowing guards with the round-trip
     * check and falls back to {@code null}. The received expression is always a parameter or a
     * {@code convertNullSafe}-bound local at this seat, so upstream's {@code declareAsVariable(true, "i"/"d")}
     * reuse resolves to using it in place.
     *
     * <p>The two {@code java.math} tokens are first-claim sentinels like every other library type this emitter
     * writes (D50 point 1), and their imports ride the rung's own type import ({@code addTypeImports} over
     * {@code prop.parentChain()}) - a number ladder's rungs ARE the {@code Integer} / {@code Long} /
     * {@code BigInteger} / {@code BigDecimal} classes the arms name.
     */
    private JavaStatementBuilder numberConversion(JavaExpression e, JavaType src, JavaType tgt) {
        if (algebra.isInteger(src)) {
            if (algebra.isLong(tgt)) {
                return compose(tgt, e, ".longValue()");
            }
            if (algebra.isBigInteger(tgt)) {
                return compose(tgt, IRDataTypeEmitter.T_BIG_INTEGER + ".valueOf(", e, ")");
            }
            return compose(tgt, IRDataTypeEmitter.T_BIG_DECIMAL + ".valueOf(", e, ")");
        }
        if (algebra.isLong(src)) {
            if (algebra.isInteger(tgt)) {
                return guarded(compose(JavaPrimitiveType.BOOLEAN,
                                e, " <= Integer.MAX_VALUE && ", e, " >= Integer.MIN_VALUE"),
                        compose(tgt, "(int) ", e));
            }
            if (algebra.isBigInteger(tgt)) {
                return compose(tgt, IRDataTypeEmitter.T_BIG_INTEGER + ".valueOf(", e, ")");
            }
            return compose(tgt, IRDataTypeEmitter.T_BIG_DECIMAL + ".valueOf(", e, ")");
        }
        if (algebra.isBigInteger(src)) {
            if (algebra.isInteger(tgt)) {
                return guarded(compose(JavaPrimitiveType.BOOLEAN,
                                IRDataTypeEmitter.T_BIG_INTEGER + ".valueOf(", e, ".intValue()).equals(", e, ")"),
                        compose(tgt, e, ".intValue()"));
            }
            if (algebra.isLong(tgt)) {
                return guarded(compose(JavaPrimitiveType.BOOLEAN,
                                IRDataTypeEmitter.T_BIG_INTEGER + ".valueOf(", e, ".longValue()).equals(", e, ")"),
                        compose(tgt, e, ".longValue()"));
            }
            return compose(tgt, "new " + IRDataTypeEmitter.T_BIG_DECIMAL + "(", e, ")");
        }
        // BigDecimal source
        if (algebra.isInteger(tgt)) {
            return guarded(compose(JavaPrimitiveType.BOOLEAN,
                            IRDataTypeEmitter.T_BIG_DECIMAL + ".valueOf(", e, ".intValue()).compareTo(", e, ") == 0"),
                    compose(tgt, e, ".intValue()"));
        }
        if (algebra.isLong(tgt)) {
            return guarded(compose(JavaPrimitiveType.BOOLEAN,
                            IRDataTypeEmitter.T_BIG_DECIMAL + ".valueOf(", e, ".longValue()).compareTo(", e, ") == 0"),
                    compose(tgt, e, ".longValue()"));
        }
        return guarded(compose(JavaPrimitiveType.BOOLEAN,
                        "new " + IRDataTypeEmitter.T_BIG_DECIMAL + "(", e, ".toBigInteger()).compareTo(", e, ") == 0"),
                compose(tgt, e, ".toBigInteger()"));
    }

    /** A null-safe narrowing guard: {@code condition ? conversion : null} ({@code CE:739-742}). */
    private JavaConditionalExpression guarded(JavaExpression condition,
                                              JavaExpression conversion) {
        return new JavaConditionalExpression(condition, conversion, JavaLiteral.NULL,
                algebra.statementAlgebraTypeUtil());
    }

    /**
     * Upstream {@code convertNullSafe} ({@code CE:753-772}): bind the expression (a {@link JavaVariable} —
     * parameter or field — is reused in place; anything else binds a {@code final} local named after its type,
     * class-scope-escaped), then guard: null (optionally fused with the meta value null-check) → the target's
     * empty representation, else the conversion. A plain-expression conversion stays a ternary
     * ({@link JavaConditionalExpression}); a branching one upgrades to {@link JavaIfThenElseBuilder}.
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
                return new JavaConditionalExpression(condition, empty, convertedExpr,
                        algebra.statementAlgebraTypeUtil());
            }
            return new JavaIfThenElseBuilder(condition, empty, converted,
                    algebra.statementAlgebraTypeUtil());
        });
    }

    /** The empty representation of a single-item target (upstream {@code empty}) ({@code CE:775-781}). */
    private JavaExpression emptyForItem(JavaType tgtItem, JavaType targetMetaValue) {
        if (targetMetaValue != null) {
            return JavaExpression.from(
                    valueSiteTypeName(tgtItem) + ".builder().build()", tgtItem);
        }
        return JavaLiteral.NULL;
    }

    // =====================================================================
    // Rendering helpers (CE:783-881)
    // =====================================================================

    /**
     * A LAZY expression composition ({@code CE:794-809}): string parts append verbatim; {@link JavaExpression} /
     * {@link JavaLambdaBody} parts render at final render time — after every scope identifier exists, honoring
     * {@code GeneratorScope}'s two-phase build/render contract (an eager {@code renderToString} mid-build would
     * close the scope and poison later {@code createIdentifier} calls).
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

    /** {@code .toBuilder()} on a model-typed expression; identity otherwise ({@code CE:812-818}). */
    private JavaExpression toBuilder(JavaExpression e) {
        if (e.getExpressionType() != null
                && algebra.isRosettaModelObject(e.getExpressionType())) {
            return compose(e.getExpressionType(), e, ".toBuilder()");
        }
        return e;
    }

    /** {@code CE:820-823}. */
    private JavaVariable fieldVar(Shape prop) {
        GeneratedIdentifier id = fieldIds.get(prop.fieldName);
        return new JavaVariable(id, prop.type);
    }

    /** A method-body scope whose parent carries the class's field identifiers ({@code CE:826-828}). */
    private JavaStatementScope bodyScope() {
        return new JavaStatementScope("pojo-compat-body", classScope);
    }

    /** Render a completed statement at method-body depth (3 tabs), brace-aware ({@code CE:831-835}). */
    private static String renderBody(JavaStatement statement) {
        var flat = new StringBuilder();
        statement.render(flat);
        return reindent(flat.toString(), 3);
    }

    /**
     * Re-indent a flat statement render ({@code CE:847-867}): every line gets {@code base + braceDepth} tabs; a
     * leading {@code }} dedents its own line — TAB-TOLERANTLY, so a braced lambda's {@code \t}} close (the
     * {@code toLambdaBody} shape, whose embedded tab is the stream-continuation level) dedents exactly like a
     * bare {@code }} and lands AT the continuation level (the golden {@code })}, PR #422); tabs already embedded
     * in a line (the multiline stream continuations) are preserved after the computed prefix. The rendered
     * grammar is closed (no string literals containing braces).
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

    /** {@code CE:869-871}. */
    private boolean isList(Shape prop) {
        return algebra.isList(prop.type);
    }

    /** {@code CE:873-875}. */
    private JavaType itemType(Shape prop) {
        return isList(prop) ? algebra.getItemType(prop.type) : prop.type;
    }

    /** {@code CE:877-880}. */
    private boolean isNumber(JavaType t) {
        return algebra.isInteger(t) || algebra.isLong(t)
                || algebra.isBigInteger(t) || algebra.isBigDecimal(t);
    }

    // =====================================================================
    // The IR side: the shape, the renders and the operation names
    // =====================================================================

    /**
     * {@code gen.valueSiteTypeName(t)} ({@code CE:233}, {@code :249}, {@code :344}, {@code :355}, {@code :392},
     * {@code :420}, {@code :453}, {@code :562}, {@code :587}, {@code :651}, {@code :661}, {@code :778}) over a
     * {@link JavaType} the algebra produced rather than over a rendered string: the canonical name is the type's
     * own {@code toString()}, which is the very spelling {@link IRPropertyModel}'s rendering law states and the
     * property surface carries. Two entries, ONE law ({@code IRDataTypeEmitter.valueSiteTypeRef}).
     */
    private static String valueSiteTypeName(JavaType type) {
        return IRDataTypeEmitter.valueSiteTypeRef(type.toString());
    }

    /** {@code gen.interfaceGetterType(parent)} ({@code CE:181}) over the rung's own rendered type and facts. */
    private String interfaceGetterType(Shape shape) {
        return gen.interfaceGetterType(shape.rendered, shape.multi, shape.itemIsModelObject);
    }

    /** {@code gen.builderGetterType(parent)} ({@code CE:263}). */
    private String builderGetterType(Shape shape) {
        return gen.builderGetterType(shape.rendered, shape.multi, shape.itemIsModelObject);
    }

    /** {@code gen.builderSingleType(parent)} ({@code CE:249}, {@code :287}). */
    private String builderSingleType(Shape shape) {
        return gen.builderSingleType(shape.rendered, shape.itemIsModelObject);
    }

    /** The rendered ITEM of a shape - the string half of {@link #itemType}. */
    private static String itemRender(Shape shape) {
        return IRDataTypeEmitter.itemTypeOf(shape.rendered);
    }

    /**
     * {@code JavaPojoProperty.getOperationName(op)} ({@code :120-133}): the GETTER compatibility name for
     * {@code get} / {@code getOrCreate}, the SETTER one for {@code set} / {@code add}, then the
     * {@code Object} / {@code RosettaModelObject} escape - all three already declared once by
     * {@code IRDataTypeEmitter.operationName}.
     */
    private static String operationName(Shape shape, Op op) {
        return IRDataTypeEmitter.operationName(op.prefix,
                op.getterSide ? shape.getterCompatibilityName : shape.setterCompatibilityName, op.postfix);
    }

    /** The MAIN property as a shape - the side every walk coerces FROM. */
    private Shape shape(IRPropertyModel.IRProperty prop) {
        return new Shape(prop.javaType(), prop.multi(), prop.itemIsRosettaModelObject(), prop.itemIsEnum(),
                prop.metaValueType(), prop.metaValueIsRosettaModelObject(),
                prop.getterCompatibilityName(), prop.setterCompatibilityName(),
                IRDataTypeEmitter.fieldName(prop));
    }

    /** One ANCESTOR rung as a shape - the side every walk writes its member IN. */
    private Shape shape(IRPropertyModel.IRParentLink rung) {
        return new Shape(rung.javaType(), rung.multi(), rung.itemIsRosettaModelObject(), rung.itemIsEnum(),
                rung.metaValueType(), rung.metaValueIsRosettaModelObject(),
                rung.getterCompatibilityName(), rung.setterCompatibilityName(),
                null);
    }

    /**
     * THE ONE VIEW an {@link IRPropertyModel.IRProperty} and an {@link IRPropertyModel.IRParentLink} both present
     * to the algebra - the substitution for {@code CE}'s {@code JavaPojoProperty} parameter. Its {@link JavaType}
     * values are built ONCE, here, from the rendered strings and the reconciled facts ({@link IRJavaTypes}), so
     * one walk never builds two unequal instances of one type and the joins the ternaries perform stay the
     * identity joins {@code CE}'s do.
     *
     * <p>{@code fieldName} is the MAIN property's Java field identifier and is {@code null} on a rung: {@code CE}
     * asks {@code ModelObjectGenerator.fieldName} of the main property only ({@code :177}, {@code :323},
     * {@code :353}, {@code :821}) - a rung has no field of its own in this class.
     */
    private final class Shape {
        private final String rendered;
        private final boolean multi;
        private final boolean itemIsModelObject;
        private final String getterCompatibilityName;
        private final String setterCompatibilityName;
        private final String fieldName;
        private final JavaType type;
        /** The bare value type behind a meta wrap - {@code null} is {@code getMetaValueType() == null}. */
        private final JavaType metaValueType;
        private final String metaValueRendered;

        private Shape(String rendered, boolean multi, boolean itemIsModelObject, boolean itemIsEnum,
                      Optional<String> metaValue, Optional<Boolean> metaValueIsModelObject,
                      String getterCompatibilityName, String setterCompatibilityName, String fieldName) {
            this.rendered = rendered;
            this.multi = multi;
            this.itemIsModelObject = itemIsModelObject;
            this.getterCompatibilityName = getterCompatibilityName;
            this.setterCompatibilityName = setterCompatibilityName;
            this.fieldName = fieldName;
            this.type = IRJavaTypes.of(rendered, itemIsModelObject, itemIsEnum, algebra);
            this.metaValueRendered = metaValue.orElse(null);
            if (metaValueRendered == null) {
                this.metaValueType = null;
            } else {
                boolean valueIsModelObject = metaValueIsModelObject.orElseThrow(
                        () -> new IRDataTypeEmitter.MissingIRFact(
                                "type." + metaValue.orElseThrow() + ".metaValueIsRosettaModelObject",
                                "the shape carries a bare meta value type but states no kind for it - the compat"
                                        + " member is refused by name rather than written against a guessed one"));
                this.metaValueType = IRJavaTypes.metaValue(metaValueRendered, valueIsModelObject);
            }
        }
    }
}
