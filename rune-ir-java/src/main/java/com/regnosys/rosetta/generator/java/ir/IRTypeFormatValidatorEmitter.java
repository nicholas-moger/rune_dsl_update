package com.regnosys.rosetta.generator.java.ir;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

import com.regnosys.rosetta.generator.GeneratedIdentifier;
import com.regnosys.rosetta.generator.java.SilentDegradation;
import com.regnosys.rosetta.generator.java.scoping.JavaClassScope;
import com.regnosys.rosetta.generator.java.scoping.JavaFileScope;
import com.regnosys.rosetta.generator.java.scoping.JavaMethodScope;
import com.regnosys.rosetta.generator.java.scoping.JavaStatementScope;
import com.regnosys.rosetta.generator.java.template.ImportCollector;
import com.regnosys.rosetta.generator.java.template.ImportCollisionResolver;
import com.regnosys.rosetta.generator.java.template.JavaStringUtil;
import com.regnosys.rosetta.generator.java.template.TemplateRenderer;
import com.regnosys.rosetta.generator.java.template.model.PojoTemplateModel;
import com.regnosys.rosetta.generator.java.template.model.ValidatorCheckModel;
import com.regnosys.rosetta.generator.java.template.model.ValidatorTemplateModel;
import com.regnosys.rosetta.ir.adapter.IRTypeNode;
import com.regnosys.rosetta.ir.core.IRAliasLink;
import com.regnosys.rosetta.ir.core.IRField;
import com.rosetta.util.DottedPath;
import com.rosetta.util.types.JavaClass;

/**
 * THE TYPE-FORMAT VALIDATOR, FROM THE IR ALONE (v3.3 seat 9, PR #645 commit 12) - the
 * {@link IRTypeUnit.Member#TYPE_FORMAT_VALIDATOR} member of the type unit, and the hardest of the three.
 *
 * <p>The file carries one {@code checkString} / {@code checkNumber} per member whose DECLARED type resolves, after
 * alias stripping, to a CONSTRAINED string or number, and - where a rung of that alias chain carries conditions -
 * THE ALIAS-CONDITION WING: an {@code @Inject} field per distinct condition class and a generator-computed
 * {@code runConditions} method joined to the checks by {@code Streams.concat}.
 *
 * <p><b>EVERY FACT IS AN IR READ</b> ({@link IRValidatorScan}'s class javadoc states the law and the list of shared
 * PURE text machinery). The envelope is {@link IRDerivedFacts#envelope}'s TYPED record, reconciled as
 * {@code typeFormat.<p>.string} / {@code .number} since PR #644; the alias chain is the adapter's, held three ways;
 * the whole-validator refusal is {@link IRDerivedFacts#refusesWholeValidator}; the meta wrap is
 * {@link IRDerivedFacts#metaWrapped}, the OVERRIDE UNION's.
 *
 * <p><b>THE WING IS PORTED VERBATIM</b> ({@code TypeFormatValidatorGenerator:363-491} and {@code :536-620}), with
 * the SCOPE / IDENTIFIER ALLOCATION ORDER preserved statement for statement - it is the ONE place where the old
 * generator's text depends on scope STATE (numbered identifiers {@code i0} / {@code i1}, the escapes {@code _o},
 * {@code _Integer}) rather than on per-attribute facts, and the vendored {@link JavaClassScope} /
 * {@link JavaFileScope} / {@link JavaMethodScope} / {@link JavaStatementScope} are GENERIC TEXT MACHINERY over
 * {@code JavaTypeDeclaration} values, which the IR route builds BY NAME through {@link IRJavaTypes} - the same rule
 * the compat algebra's {@code JavaType} values take. A seam on the old generator was REJECTED: the emitter would
 * then call a class whose other half reads a model.
 *
 * <p><b>THE TWO NAMED REFUSALS</b> ({@link IRValidatorScan.NamedRefusal.Site}) are the old generator's own, and
 * each REFUSES THE TYPE - which, under the unit's all-or-nothing law, leaves all six files to the old generator
 * where the old generator withholds ONE. That asymmetry is the strict path's, disclosed in {@code CONTRACT-C12}
 * § 1.5, and its whole corpus reach is the chaos cell's twelve {@code BOILERPLATE_NAME_COLLISION} files (zero on
 * the 25 vendored cells).
 */
final class IRTypeFormatValidatorEmitter implements IRTypeUnit.MemberEmitter {

    private static final String TEMPLATE_GROUP = "templates/ir-java-validator-typeformat.stg";

    /** {@code TypeFormatValidatorGenerator.TYPE_FORMAT_TOKENS} ({@code :120-128}) - the template's token census. */
    private static final Map<String, String> TYPE_FORMAT_TOKENS = ImportCollisionResolver.typeRefs(List.of(
            "com.google.common.collect.Lists",
            "com.google.common.collect.Streams",
            "com.rosetta.model.lib.expression.ComparisonResult",
            "com.rosetta.model.lib.path.RosettaPath",
            "com.rosetta.model.lib.validation.ValidationResult",
            "com.rosetta.model.lib.validation.Validator",
            "java.util.List",
            "javax.inject.Inject"));

    /** {@code TypeFormatValidatorGenerator:623-626} - the sentinels the static runConditions renderers write. */
    private static final String T_LIST = ImportCollisionResolver.typeRefOrBare("java.util.List");
    private static final String T_VALIDATION_RESULT =
            ImportCollisionResolver.typeRefOrBare("com.rosetta.model.lib.validation.ValidationResult");
    private static final String T_ROSETTA_PATH =
            ImportCollisionResolver.typeRefOrBare("com.rosetta.model.lib.path.RosettaPath");
    private static final String T_ARRAY_LIST = ImportCollisionResolver.typeRefOrBare("java.util.ArrayList");

    private final IRTypeIndex index;
    private final IRDerivedFacts facts;
    private final TemplateRenderer renderer;

    IRTypeFormatValidatorEmitter(IRTypeIndex index, IRDerivedFacts facts) {
        this.index = Objects.requireNonNull(index, "index");
        this.facts = Objects.requireNonNull(facts, "facts");
        this.renderer = new TemplateRenderer();
        this.renderer.loadGroupFromClasspath(TEMPLATE_GROUP);
    }

    /** Every validated type writes this file, or the type is REFUSED by name - never "no file by law". */
    @Override
    public Optional<String> emit(IRTypeNode node) {
        return Optional.of(render(node));
    }

    /** The whole file - {@code TypeFormatValidatorGenerator.generate} ({@code :168-180}), the D50 two-step. */
    String render(IRTypeNode node) {
        ValidatorTemplateModel model = buildModel(node);
        String classText = renderer.render(TEMPLATE_GROUP, "typeFormatValidatorBody",
                "m", model, "t", TYPE_FORMAT_TOKENS);
        ImportCollisionResolver.ClassResolution resolved = ImportCollisionResolver.resolveClass(
                classText, model.getPackageName() + "." + model.getValidatorClassName(), model.getImports());
        return renderer.render(TEMPLATE_GROUP, "typeFormatValidator", "m", new PojoTemplateModel(
                model.getPackageName(), resolved.imports(), model.getStaticImports(), resolved.classText()));
    }

    /** {@code TypeFormatValidatorGenerator.buildModel} ({@code :187-497}) over the IR surface. */
    ValidatorTemplateModel buildModel(IRTypeNode node) {
        IRValidatorScan.Surface surface = IRValidatorScan.scan(node, index, facts);
        String dataClassName = IRTypeUnit.simpleName(node);                              // :189
        String validatorClassName =
                IRValidatorScan.validatorClassName(node, IRTypeUnit.Member.TYPE_FORMAT_VALIDATOR);    // :190
        String packageName =
                IRValidatorScan.validatorPackage(node, IRTypeUnit.Member.TYPE_FORMAT_VALIDATOR);      // :191
        String dataClassFqn = IRValidatorScan.dataClassFqn(node);                        // :192
        boolean collides = facts.collides(dataClassName);                                // :196
        String dataClassJavaType = IRValidatorScan.dataClassJavaType(dataClassFqn, collides);         // :199

        var imports = new ImportCollector(packageName);                                  // :201-210
        if (!collides) {
            imports.addImport(dataClassFqn);
        }
        imports.addImport("com.google.common.collect.Lists");
        imports.addImport("com.rosetta.model.lib.expression.ComparisonResult");
        imports.addImport("com.rosetta.model.lib.path.RosettaPath");
        imports.addImport("com.rosetta.model.lib.validation.ValidationResult");
        imports.addImport("com.rosetta.model.lib.validation.Validator");
        imports.addImport("java.util.List");

        boolean stringUsed = false;                                                      // :214-219
        boolean numberUsed = false;
        boolean ofUsed = false;
        boolean emptyUsed = false;
        boolean patternUsed = false;
        boolean bigDecimalUsed = false;

        var checkExprs = new ArrayList<String>();                                        // :221
        List<WiringPlan> wiringPlans = new ArrayList<>();                                // :225-226
        Map<String, JavaClass<?>> conditionDeps = new LinkedHashMap<>();
        for (IRValidatorScan.Scanned scanned : surface.scanned()) {                      // :227-329
            IRField declared = scanned.declared();
            if (declared == null) {
                continue;                                                                // :230-232
            }
            // THE WING'S COLLECTION (:233-289): every alias rung of the DECLARED type's chain, outermost first,
            // contributes its condition classes in declaration order; a PARAMETERISED rung carrying a condition
            // refuses the WHOLE validator of the type (the wing is one method over every attribute, so there is
            // no per-attribute file to drop).
            List<JavaClass<?>> wired = new ArrayList<>();
            for (IRAliasLink link : IRDerivedFacts.aliasChain(declared)) {
                // the predicate is IRDerivedFacts's, not this emitter's: the reconcile asserts the very same law
                // as typeFormat.<p>.refusesValidator, and lane V11 inverts it - a second copy here would leave the
                // emitter rendering where the reconciler refuses, which is what that lane found
                if (IRDerivedFacts.refusesAtLink(link)) {                                 // :251-261
                    throw new IRValidatorScan.NamedRefusal(
                            IRValidatorScan.NamedRefusal.Site.TYPE_ALIAS_CONDITION_DROPPED,
                            "attribute " + dataClassName + "." + scanned.name() + " is typed through the"
                                    + " PARAMETERISED typeAlias " + link.name() + "("
                                    + String.join(",", link.parameterNames()) + " ...), whose condition(s) "
                                    + IRDerivedFacts.aliasConditionSimpleNames(link)
                                    + " the data-rule walk refuses (the parameterised condition wing is post-9.83)");
                }
                for (String conditionSimpleName : IRDerivedFacts.aliasConditionSimpleNames(link)) {
                    wired.add(conditionClass(link, conditionSimpleName));                 // :262
                }
            }
            if (!wired.isEmpty()) {                                                       // :265-289
                // the meta wrapper's simple name from the TYPED item type (the same read valueExpression's unwrap
                // takes), never from the rendered local-type text
                boolean metaWrapped = facts.metaWrapped(declared, surface.effective(), surface.chain());
                String item = IRDataTypeEmitter.itemTypeOf(scanned.prop());
                String wrapperSimpleName = metaWrapped ? IRValidatorScan.simpleOf(item) : null;
                // round 2 (oracle group alias-conditions-filescope): the multi local's element type when it is a
                // java.lang class (`List<Integer>`) - upstream's file scope registers a java.lang type the moment
                // the file WRITES it, so an attribute local of that name escapes (`_Integer`). THE TEST IS EXACT
                // ("java.lang".equals(package)), never a startsWith on the canonical name (the review's Q10).
                JavaClass<?> javaLangElement = javaLangElement(item);                      // :274-276
                // v3.2 seat 11 (D50): the wrapper TYPE position is a first-claim sentinel (its canonical the same
                // string the import carries); the hoist NAME keeps the simple name
                String wrapperTypeRef = wrapperSimpleName == null ? null
                        : ImportCollisionResolver.typeRefOrBare(item);                     // :279-280
                wiringPlans.add(new WiringPlan(scanned.name(), scanned.getterCall(), scanned.castType(),
                        scanned.prop().multi(), metaWrapped, wrapperSimpleName, wrapperTypeRef,
                        javaLangElement, wired));                                          // :281-284
                IRValidatorScan.addCastImports(imports, scanned.prop());                   // :285
                for (JavaClass<?> conditionClass : wired) {                                // :286-288
                    conditionDeps.putIfAbsent(conditionClass.getCanonicalName().withDots(), conditionClass);
                }
            }
            Optional<IRDerivedFacts.Envelope> envelope = facts.envelope(declared);         // :290-291
            if (envelope.isEmpty()) {
                continue;                                                                  // :292-294
            }
            String value = valueExpression(scanned, declared, surface, imports);           // :295
            IRDerivedFacts.Envelope constraint = envelope.get();
            if (constraint.isString()) {                                                   // :297-312
                stringUsed = true;
                String maxLen = optionalInt(constraint.maxLength());
                String pattern = constraint.pattern()
                        .map(p -> "of(" + ImportCollisionResolver.typeRefOrBare("java.util.regex.Pattern")
                                + ".compile(\"" + JavaStringUtil.escapeJava(p) + "\"))")
                        .orElse("empty()");
                // v3.1 C2d family 2 (typeformat-optional-presence): the Optional.of / Optional.empty import need
                // is the constraint's PRESENCE, read from the typed envelope - the rendered "of(" / "empty()"
                // prefix was only ever its spelling
                ofUsed |= constraint.maxLength().isPresent() || constraint.pattern().isPresent();
                emptyUsed |= constraint.maxLength().isEmpty() || constraint.pattern().isEmpty();
                patternUsed |= constraint.pattern().isPresent();
                checkExprs.add("checkString(\"" + scanned.name() + "\", " + value + ", "
                        + (constraint.minLength().isPresent() ? constraint.minLength().getAsInt() : 0)
                        + ", " + maxLen + ", " + pattern + ")");
            } else {                                                                       // :313-328
                numberUsed = true;
                String digits = optionalInt(constraint.digits());
                String fractionalDigits = optionalInt(constraint.fractionalDigits());
                String min = optionalBigDecimal(constraint.min());
                String max = optionalBigDecimal(constraint.max());
                ofUsed |= constraint.digits().isPresent() || constraint.fractionalDigits().isPresent()
                        || constraint.min().isPresent() || constraint.max().isPresent();
                emptyUsed |= constraint.digits().isEmpty() || constraint.fractionalDigits().isEmpty()
                        || constraint.min().isEmpty() || constraint.max().isEmpty();
                bigDecimalUsed |= constraint.min().isPresent() || constraint.max().isPresent();
                checkExprs.add("checkNumber(\"" + scanned.name() + "\", " + value + ", "
                        + digits + ", " + fractionalDigits + ", " + min + ", " + max + ")");
            }
        }

        if (patternUsed) {                                                                 // :331-336
            imports.addImport("java.util.regex.Pattern");
        }
        if (bigDecimalUsed) {
            imports.addImport("java.math.BigDecimal");
        }

        var staticImports = new ImportCollector(packageName);                              // :338-356
        staticImports.addStaticImport("com.google.common.base.Strings.isNullOrEmpty");
        if (numberUsed) {
            staticImports.addStaticImport(
                    "com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkNumber");
        }
        if (stringUsed) {
            staticImports.addStaticImport(
                    "com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkString");
        }
        staticImports.addStaticImport("com.rosetta.model.lib.validation.ValidationResult.failure");
        staticImports.addStaticImport("com.rosetta.model.lib.validation.ValidationResult.success");
        if (emptyUsed) {
            staticImports.addStaticImport("java.util.Optional.empty");
        }
        if (ofUsed) {
            staticImports.addStaticImport("java.util.Optional.of");
        }
        staticImports.addStaticImport("java.util.stream.Collectors.toList");

        var checks = new ArrayList<ValidatorCheckModel>();                                 // :358-361
        for (int i = 0; i < checkExprs.size(); i++) {
            checks.add(new ValidatorCheckModel(checkExprs.get(i), i == checkExprs.size() - 1));
        }

        // THE WING'S SCOPE BOOKKEEPING (:363-491), ported statement for statement. PROTOCOL (the vendored scope's
        // contract): every request of a scope precedes its first read - a read CLOSES the scope and its parents -
        // so the requests and the file-scope claims all happen before the first getActualName below; merging the
        // loops would silently change the numbering.
        List<ValidatorTemplateModel.ConditionDependencyModel> depModels = new ArrayList<>();
        String runConditionsBody = "";
        String pathName = "path";
        if (!conditionDeps.isEmpty()) {
            imports.addImport("com.google.common.collect.Streams");                        // :385-387
            imports.addImport("java.util.ArrayList");
            imports.addImport("javax.inject.Inject");
            JavaClass<?> validatorClass = IRJavaTypes.generated(packageName + "." + validatorClassName);
            JavaClassScope classScope = JavaClassScope.createAndRegisterIdentifier(validatorClass);   // :388
            JavaFileScope fileScope = classScope.getFileScope();                           // :389
            GeneratedIdentifier pathId = classScope.createUniqueIdentifier("path");        // :390
            Map<String, GeneratedIdentifier> fieldIds = new LinkedHashMap<>();             // :391-396
            for (JavaClass<?> dep : conditionDeps.values()) {
                fieldIds.put(dep.getCanonicalName().withDots(),
                        classScope.createIdentifier(dep, lowerFirst(dep.getSimpleName())));
            }
            Map<String, SilentDegradation.Claim> claimedSimpleNames = new HashMap<>();     // :412-413
            claimedSimpleNames.put(validatorClassName, new SilentDegradation.Claim(
                    packageName + "." + validatorClassName, SilentDegradation.ClaimOrigin.OWN_CLASS));
            Set<String> headerClaims = new HashSet<>(                                      // :421
                    List.of(validatorClassName, "Validator", dataClassName, "Inject"));
            for (String imported : imports.getImports()) {                                 // :422-424
                claimedSimpleNames.putIfAbsent(DottedPath.splitOnDots(imported).last(),
                        new SilentDegradation.Claim(imported, SilentDegradation.ClaimOrigin.IMPORT));
            }
            for (WiringPlan plan : wiringPlans) {                                          // :430-435
                if (plan.multi() && plan.javaLangElement() != null) {   // the java.lang claim (a18)
                    claimedSimpleNames.putIfAbsent(plan.javaLangElement().getSimpleName(),
                            new SilentDegradation.Claim(
                                    plan.javaLangElement().getCanonicalName().withDots(),
                                    SilentDegradation.ClaimOrigin.JAVA_LANG));
                }
            }
            Map<String, String> depTypeNames = new LinkedHashMap<>();                      // :436-460
            for (JavaClass<?> dep : conditionDeps.values()) {
                String canonical = dep.getCanonicalName().withDots();
                SilentDegradation.Claim claimant = claimedSimpleNames.putIfAbsent(dep.getSimpleName(),
                        new SilentDegradation.Claim(canonical, SilentDegradation.ClaimOrigin.IMPORT));
                String typeName = ImportCollisionResolver.typeRefOrBare(canonical);
                if (claimant == null) {
                    imports.addImport(canonical);
                } else if (!claimant.canonical().equals(canonical)) {
                    if (!headerClaims.contains(dep.getSimpleName())
                            && !conditionDeps.containsKey(claimant.canonical())) {
                        throw new IRValidatorScan.NamedRefusal(
                                IRValidatorScan.NamedRefusal.Site.BOILERPLATE_NAME_COLLISION,
                                "condition class " + canonical + " shares its simple name with "
                                        + claimant.canonical() + ", a type this validator writes after its"
                                        + " @Inject fields; the released 9.83.0 plugin claims the condition class"
                                        + " first and writes " + claimant.canonical() + " fully qualified"
                                        + claimant.origin().witnessClause + " - a text-order import law the"
                                        + " fork's template does not model");
                    }
                    typeName = canonical;
                }
                depTypeNames.put(canonical, typeName);
            }
            for (String imported : imports.getImports()) {                                 // :465-470
                String simple = DottedPath.splitOnDots(imported).last();
                if (!fileScope.isNameTaken(simple)) {
                    fileScope.createUniqueIdentifier(simple);
                }
            }
            for (WiringPlan plan : wiringPlans) {                                          // :471-477
                if (plan.multi() && plan.javaLangElement() != null) {
                    // SIDE EFFECT, the result unused: registers a java.lang type on write, as upstream's file
                    // scope does - load-bearing for the `_Integer` escape
                    fileScope.getIdentifier(plan.javaLangElement());
                }
            }
            Map<String, String> fieldNames = new LinkedHashMap<>();                        // :478-484
            for (JavaClass<?> dep : conditionDeps.values()) {
                String canonical = dep.getCanonicalName().withDots();
                String fieldName = fieldIds.get(canonical).getActualName();
                fieldNames.put(canonical, fieldName);
                depModels.add(new ValidatorTemplateModel.ConditionDependencyModel(
                        depTypeNames.get(canonical), fieldName));
            }
            pathName = pathId.getActualName();                                             // :485
            JavaMethodScope methodScope = classScope.createMethodScope("runConditions");   // :486
            GeneratedIdentifier instanceId = methodScope.createUniqueIdentifier("o");      // :487
            GeneratedIdentifier resultsId = methodScope.createUniqueIdentifier("results"); // :488
            runConditionsBody = renderRunConditions(wiringPlans, fieldNames, dataClassJavaType, pathName,
                    instanceId.getActualName(), resultsId.getActualName(), methodScope.getBodyScope());
        }

        return new ValidatorTemplateModel(packageName, validatorClassName,                 // :493-496
                dataClassName, dataClassJavaType, dataClassFqn,
                imports.getImports(), staticImports.getStaticImports(), checks,
                depModels, runConditionsBody, pathName);
    }

    // ------------------------------------------------------------------------------- the wing's inputs, from the IR

    /**
     * ONE condition class of ONE alias rung, as a value the scope machinery can hold -
     * {@code JavaTypeTranslator.toConditionJavaClass}'s own law ({@code :296-302}): the alias's namespace with
     * {@code validation.datarule} below it, ESCAPED as a whole, and {@code <AliasName><ConditionName>} as the
     * simple name. {@link IRDerivedFacts#aliasConditionSimpleNames} supplies the simple name under the very
     * {@code namedCount} law {@code ConditionCases.casesOf} applies.
     */
    private static JavaClass<?> conditionClass(IRAliasLink link, String conditionSimpleName) {
        return IRJavaTypes.generated(
                IRDerivedFacts.aliasConditionPackage(link) + "." + conditionSimpleName);
    }

    /**
     * The multi local's element type when it is a {@code java.lang} class - {@code TypeFormatValidatorGenerator:274-276}.
     * THE TEST IS EXACT: {@code "java.lang".equals(package)}, never {@code startsWith("java.lang.")}, so a class in
     * {@code java.lang.reflect} is NOT one (the review's Q10 correction).
     */
    private static JavaClass<?> javaLangElement(String itemCanonical) {
        if (!IRValidatorScan.isClassName(itemCanonical)) {
            return null;
        }
        int lastDot = itemCanonical.lastIndexOf('.');
        String packageName = itemCanonical.substring(0, lastDot);
        return "java.lang".equals(packageName) ? IRJavaTypes.item(itemCanonical, false, false) : null;
    }

    /**
     * The check VALUE expression - {@code TypeFormatValidatorGenerator.valueExpression} ({@code :726-742}): the
     * plain POJO getter (NO cast, unlike the cardinality family); a META-WRAPPED attribute unwraps
     * ({@code .getValue()} single, stream-map list).
     *
     * <p><b>THE PREDICATE IS {@code metaWrapped}, NOT {@code metaValueType}</b> (the review's Q10, VERIFIED):
     * {@code :726-731} branches on {@code declared.metaWrapped()} - ANY {@code [metadata ...]} under the override
     * union - while {@code IRPropertyModel.metaValueType} is present only for the five wrapping qualifiers
     * ({@code reference} / {@code address} / {@code scheme} / {@code id} / {@code location}). The two laws are not
     * the same predicate, and the source does not establish that they coincide on every annotation. So when the
     * meta-wrap law says WRAPPED and the property surface carries NO bare value type, the two disagree about this
     * node and the member REFUSES BY NAME rather than guessing which is right.
     */
    private String valueExpression(IRValidatorScan.Scanned scanned, IRField declared,
                                   IRValidatorScan.Surface surface, ImportCollector imports) {
        boolean metaWrapped = facts.metaWrapped(declared, surface.effective(), surface.chain());
        if (!metaWrapped) {
            return scanned.getterExpr();
        }
        if (scanned.prop().metaValueType().isEmpty()) {
            throw new IRValidatorScan.NamedRefusal(
                    IRValidatorScan.NamedRefusal.Site.META_VALUE_TYPE_ABSENT,
                    "the property " + scanned.name() + " is META-WRAPPED by the override union's law (any"
                            + " [metadata ...]), but its property surface carries no bare value type (only the"
                            + " reference / address / scheme / id / location qualifiers produce one) - the two laws"
                            + " disagree about this node and the value expression is refused rather than guessed");
        }
        String wrapper = IRDataTypeEmitter.itemTypeOf(scanned.prop());
        if (scanned.prop().multi()) {
            imports.addImport(wrapper);                                                     // :733-734
            imports.addImport("java.util.stream.Collectors");
            return scanned.getterExpr() + ".stream().map("
                    + ImportCollisionResolver.typeRefOrBare(wrapper) + "::getValue).collect("
                    + ImportCollisionResolver.typeRefOrBare("java.util.stream.Collectors") + ".toList())";
        }
        return scanned.getterExpr() + ".getValue()";                                        // :741
    }

    private static String optionalInt(OptionalInt value) {                                  // :744-746
        return value.isPresent() ? "of(" + value.getAsInt() + ")" : "empty()";
    }

    /**
     * {@code optionalBigDecimal} ({@code :748-752}) over the envelope's DECIMAL SPELLING - the
     * {@code stripTrailingZeros().toString()} text {@link IRDerivedFacts.Envelope} carries, which IS the literal
     * the old generator writes (its {@code BigDecimal} was stripped at parse time,
     * {@code TypeFormatConstraintScan.parse:201}).
     */
    private static String optionalBigDecimal(Optional<String> value) {
        return value.map(d -> "of(new " + ImportCollisionResolver.typeRefOrBare("java.math.BigDecimal")
                        + "(\"" + d + "\"))")
                .orElse("empty()");
    }

    // ------------------------------------------------------------------------------------ the wing's own renderers

    /**
     * v3.2 seat 3 (F9): one attribute's alias-condition wiring - {@code TypeFormatValidatorGenerator.WiringPlan}
     * ({@code :507-510}), its condition classes held as the IR route's own {@code JavaClass} values.
     */
    private record WiringPlan(String attrName, String getterCall, String localType, boolean multi,
                              boolean metaWrapped, String wrapperSimpleName, String wrapperTypeRef,
                              JavaClass<?> javaLangElement, List<JavaClass<?>> conditionClasses) {
    }

    /**
     * {@code renderRunConditions} ({@code :536-554}) - the {@code runConditions} method, every line carrying its
     * own indentation tabs and newline. The whole block is generator-computed (the datarule class-body
     * convention): the tab-only separator line upstream's template leaves before the method, the method itself,
     * and the closing brace - ST4 drops a whitespace-only template line, so the separator cannot live in the
     * template.
     */
    static String renderRunConditions(List<WiringPlan> plans, Map<String, String> fieldNames,
                                      String dataClassJavaType, String pathName, String instanceName,
                                      String resultsName, JavaStatementScope bodyScope) {
        StringBuilder b = new StringBuilder();
        b.append("\t\n");
        b.append("\tprivate ").append(T_LIST).append("<").append(T_VALIDATION_RESULT).append("<?>> runConditions(")
         .append(T_ROSETTA_PATH).append(' ').append(pathName)
         .append(", ").append(dataClassJavaType).append(' ').append(instanceName).append(") {\n");
        b.append("\t\t").append(T_LIST).append("<").append(T_VALIDATION_RESULT).append("<?>> ").append(resultsName)
         .append(" = new ").append(T_ARRAY_LIST).append("();\n");
        b.append(renderRunConditionStatements(plans, fieldNames, pathName, instanceName, resultsName, bodyScope));
        b.append("\t\treturn ").append(resultsName).append(";\n");
        b.append("\t}\n");
        return b.toString();
    }

    /** One attribute's body-scope identifiers, requested in upstream's creation order ({@code :557-558}). */
    private record PlanIds(GeneratedIdentifier index, GeneratedIdentifier local, GeneratedIdentifier hoist) {
    }

    /**
     * {@code renderRunConditionStatements} ({@code :561-620}) - the per-attribute statements. THE ALLOCATION ORDER
     * IS THE TEXT: pass 1 requests every body-scope identifier in upstream's creation order (the loop index BEFORE
     * the attribute local, the wrapper hoist after both); pass 2 renders, reading the CLOSED scope's names - a
     * read closes the scope, so a merged single pass would silently change the numbering (oracle golden
     * {@code LoopTypeFormatValidator}: {@code i1} the local, {@code i0} the index).
     */
    static String renderRunConditionStatements(List<WiringPlan> plans, Map<String, String> fieldNames,
                                               String pathName, String instanceName, String resultsName,
                                               JavaStatementScope bodyScope) {
        List<PlanIds> ids = new ArrayList<>();
        for (WiringPlan plan : plans) {
            GeneratedIdentifier index = plan.multi() ? bodyScope.createUniqueIdentifier("i") : null;
            GeneratedIdentifier local = plan.multi() ? bodyScope.createUniqueIdentifier(plan.attrName()) : null;
            GeneratedIdentifier hoist = plan.metaWrapped()
                    ? bodyScope.createUniqueIdentifier(lowerFirst(plan.wrapperSimpleName())) : null;
            ids.add(new PlanIds(index, local, hoist));
        }
        StringBuilder b = new StringBuilder();
        for (int p = 0; p < plans.size(); p++) {
            WiringPlan plan = plans.get(p);
            PlanIds id = ids.get(p);
            String subPath = pathName + ".newSubPath(\"" + plan.attrName() + "\")";
            String getter = instanceName + "." + plan.getterCall();
            if (!plan.multi()) {
                String value = getter;
                if (plan.metaWrapped()) {
                    String hoist = id.hoist().getActualName();
                    b.append("\t\tfinal ").append(plan.localType()).append(' ').append(hoist)
                     .append(" = ").append(getter).append(";\n");
                    value = "(" + hoist + " == null ? null : " + hoist + ".getValue())";
                }
                for (JavaClass<?> conditionClass : plan.conditionClasses()) {
                    b.append("\t\t").append(resultsName).append(".addAll(")
                     .append(fieldOf(conditionClass, fieldNames))
                     .append(".getValidationResults(").append(subPath).append(", ").append(value).append("));\n");
                }
                continue;
            }
            String index = id.index().getActualName();
            String local = id.local().getActualName();
            b.append("\t\tfinal ").append(plan.localType()).append(' ').append(local)
             .append(" = ").append(getter).append(";\n");
            b.append("\t\tif (").append(local).append(" != null) {\n");
            b.append("\t\t\tfor (int ").append(index).append(" = 0; ").append(index).append(" < ")
             .append(local).append(".size(); ").append(index).append("++) {\n");
            String value = local + ".get(" + index + ")";
            if (plan.metaWrapped()) {
                String hoist = id.hoist().getActualName();
                b.append("\t\t\t\tfinal ").append(plan.wrapperTypeRef()).append(' ').append(hoist)
                 .append(" = ").append(value).append(";\n");
                value = "(" + hoist + " == null ? null : " + hoist + ".getValue())";
            }
            for (JavaClass<?> conditionClass : plan.conditionClasses()) {
                b.append("\t\t\t\t").append(resultsName).append(".addAll(")
                 .append(fieldOf(conditionClass, fieldNames))
                 .append(".getValidationResults(").append(subPath).append(".withIndex(").append(index)
                 .append("), ").append(value).append("));\n");
            }
            b.append("\t\t\t}\n");
            b.append("\t\t}\n");
        }
        return b.toString();
    }

    /** {@code fieldOf} ({@code :628-633}): a plan whose class was never registered would render the literal null. */
    private static String fieldOf(JavaClass<?> conditionClass, Map<String, String> fieldNames) {
        String canonical = conditionClass.getCanonicalName().withDots();
        return Objects.requireNonNull(fieldNames.get(canonical), () -> "no injected field for " + canonical);
    }

    private static String lowerFirst(String s) {                                            // :635-637
        return s == null || s.isEmpty() ? s : Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }
}
