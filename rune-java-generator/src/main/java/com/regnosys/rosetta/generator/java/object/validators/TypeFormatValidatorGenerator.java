package com.regnosys.rosetta.generator.java.object.validators;

import java.math.BigDecimal;
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
import java.util.regex.Pattern;
import java.util.stream.Stream;


import com.regnosys.rosetta.ast.RRootElement;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.supporting.RChoiceOption;
import com.regnosys.rosetta.ast.supporting.RTypeCall;
import com.regnosys.rosetta.ast.types.RChoice;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.JavaClassGenerator;
import com.regnosys.rosetta.generator.java.SilentDegradation;
import com.regnosys.rosetta.generator.java.object.ConditionCases;
import com.regnosys.rosetta.generator.GeneratedIdentifier;
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
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.generator.java.types.RGeneratedJavaClass;
import com.regnosys.rosetta.generator.java.types.RJavaPojoInterface;
import com.regnosys.rosetta.types.RNumberType;
import com.regnosys.rosetta.types.RStringType;
import com.regnosys.rosetta.types.RType;
import com.rosetta.model.lib.ModelSymbolId;
import com.rosetta.util.DottedPath;
import com.rosetta.util.types.JavaClass;

/**
 * Generates a {@code Validator} implementation that checks type-format
 * constraints — {@code checkString} (length/pattern) and {@code checkNumber}
 * (digits/range) — for each attribute whose declared type resolves (after alias
 * stripping) to a CONSTRAINED string or number ({@link TypeFormatConstraintScan}).
 * One file per {@code Data} AND per {@code choice} type, emitted EVEN WHEN EMPTY
 * (most files carry the empty {@code newArrayList(\n\t\t\t);} body — golden
 * {@code PartyTypeFormatValidator}).
 *
 * <p>THE ALIAS-CONDITION WING (v3.2 seat 3, census family F9): the #403 catalogue's
 * drift #1 recorded the vendored generator's alias-condition wing ({@code runConditions}
 * / {@code @Inject} condition dependencies) as "post-9.83 — zero of the 4,736 goldens
 * carry it" and this class deliberately built the simple wing only. The premise was an
 * absence-of-carrier inference, and the released-9.83.0 chaos golden
 * {@code C8PricedTypeFormatValidator} REFUTES it: the wing IS 9.83.0 behaviour — the
 * vendored corpus simply declares no {@code typeAlias} with a {@code condition}, so no
 * golden ever carried it. The wing is BUILT here since the seat's fix (upstream
 * {@code aliasHierarchyPerAttribute} / {@code checkTypeConditions}): an attribute whose
 * declared type walks through aliases carrying conditions injects every such condition
 * class — outermost alias first — and runs it in {@code runConditions}, joined to the
 * checks by {@code Streams.concat}; every identifier is a request on the vendored
 * {@link JavaClassScope} / {@link JavaMethodScope} / {@link JavaStatementScope} — upstream's
 * scope law itself, one declaration (round-2 spec MF-1). What STAYS refused, at
 * {@link SilentDegradation.Site#TYPE_ALIAS_CONDITION_DROPPED}, is an attribute typed
 * through a PARAMETERISED alias — the post-9.83 sub-wing the released plugin renders as
 * a self-comparison ({@link ConditionCases#parameterisedOwner}).
 *
 * <p>Byte laws (golden-verified at #403/#407): the check value is the POJO getter
 * expression WITHOUT a cast (unlike the cardinality family); an {@code int}
 * attribute renders {@code of(0)} fractional digits (golden
 * {@code FrequencyTypeFormatValidator}); {@code minLength} renders as a RAW int
 * ({@code orElse(0)}); other bounds render {@code of(…)}/{@code empty()};
 * BigDecimal bounds as {@code of(new BigDecimal("…"))}; patterns as
 * {@code of(Pattern.compile("…"))} with {@code escapeJava} literals; entries
 * separate with {@code ", "} (comma + trailing space); static imports follow the
 * used-only law PER SYMBOL ({@code checkString}/{@code checkNumber}/{@code of}/
 * {@code empty} only when the rendered body references them); {@code Pattern}/
 * {@code BigDecimal} imports only when rendered; NO version javadoc; the #306
 * java.lang-collision FQN law on the subject type (golden iso
 * {@code ErrorTypeFormatValidator}).
 *
 * <p>Meta-wrapped attributes: zero CORPUS witnesses exist of a meta-wrapped
 * CONSTRAINED basic (0 {@code FieldWithMeta}/{@code ::getValue} hits across all
 * 4,736 goldens); the upstream unwrap shape ({@code .getValue()} single /
 * stream-map list) is implemented for mechanism fidelity. The LIST branch got its
 * first byte witness from the hold-out {@code pojo-bulk-value-narrow} group
 * (PR #422): the golden spells {@code Collectors.toList()} — the CLASS reference,
 * upstream xtend L247 {@code «Collectors».toList()} — at the unwrap seat (the
 * template's boilerplate keeps the STATIC {@code toList} import), and imports the
 * wrapper type ({@code FieldWithMetaInteger}).
 *
 * <p>Output must match upstream Xtend TypeFormatValidatorGenerator at 9.83.0 (D11).
 * Uses ST4 template {@code java-validator-typeformat.stg} via {@link TemplateRenderer}.
 *
 * <p>Migrated from StringBuilder to ST4 as part of M8 (D15); rebuilt on the POJO
 * property surface ({@link ValidatorScan}) + the family-owned constraint envelope
 * ({@link TypeFormatConstraintScan}) + wired into the D11 gate at PR #407
 * (coverage burn-down wave B).
 */
public class TypeFormatValidatorGenerator extends JavaClassGenerator<RRootElement, RGeneratedJavaClass<?>> {

    private static final String TEMPLATE_GROUP = "templates/java-validator-typeformat.stg";
    /**
     * v3.2 seat 11 (D50): the library types the class-body template writes, each a first-claim sentinel
     * under its simple name ({@code <t.List>} ...). The list IS the template's token census - a token the
     * template writes that this list lacks renders empty, which the D11 and the hold-out bars catch on
     * every type-format validator. Public for the template tests, which render the two steps as generate() does.
     */
    public static final java.util.Map<String, String> TYPE_FORMAT_TOKENS = ImportCollisionResolver.typeRefs(java.util.List.of(
            "com.google.common.collect.Lists",
            "com.google.common.collect.Streams",
            "com.rosetta.model.lib.expression.ComparisonResult",
            "com.rosetta.model.lib.path.RosettaPath",
            "com.rosetta.model.lib.validation.ValidationResult",
            "com.rosetta.model.lib.validation.Validator",
            "java.util.List",
            "javax.inject.Inject"));

    private final GeneratorModel generatorModel;
    private final JavaTypeTranslator typeTranslator;
    private final JavaTypeUtil typeUtil;
    private final TemplateRenderer renderer;

    public TypeFormatValidatorGenerator(GeneratorModel generatorModel,
                                        JavaTypeTranslator typeTranslator, JavaTypeUtil typeUtil) {
        this.generatorModel = generatorModel;
        this.typeTranslator = typeTranslator;
        this.typeUtil = typeUtil;
        this.renderer = new TemplateRenderer();
        this.renderer.loadGroupFromClasspath(TEMPLATE_GROUP);
    }

    /**
     * Convenience overload preserving the pre-#407 two-arg surface
     * ({@link JavaTypeUtil} is stateless — a fresh instance is semantically
     * identical). Keeps the anchor/unit test call sites compilable across the
     * revert-RED verification of the wave-B fix bundle.
     */
    public TypeFormatValidatorGenerator(GeneratorModel generatorModel, JavaTypeTranslator typeTranslator) {
        this(generatorModel, typeTranslator, new JavaTypeUtil());
    }

    @Override
    protected Stream<? extends RRootElement> streamObjects(RModel model) {
        return model.rootElements().stream()
                .filter(ValidatorScan::isValidatedType)
                .map(e -> (RRootElement) e);
    }

    @Override
    protected RGeneratedJavaClass<?> createTypeRepresentation(RRootElement element) {
        ModelSymbolId typeId = new ModelSymbolId(generatorModel.namespace(element),
                ValidatorScan.elementName(element));
        return typeTranslator.toTypeFormatValidatorClass(typeId);
    }

    @Override
    protected String generate(RRootElement element, RGeneratedJavaClass<?> validatorClass, String version) {
        ValidatorTemplateModel model = buildModel(element, validatorClass);
        // v3.2 seat 11 (D50 - the file-scope first-claim law): the class text first, every TYPE position a
        // sentinel; resolved in text order from the ONE seed upstream's JavaClassScope registers before
        // writing a byte (the validator class itself); the losers' imports dropped; then the file wrapper.
        // A validator with no collision renders the pre-seat bytes.
        String classText = renderer.render(TEMPLATE_GROUP, "typeFormatValidatorBody", "m", model, "t", TYPE_FORMAT_TOKENS);
        ImportCollisionResolver.ClassResolution resolved = ImportCollisionResolver.resolveClass(
                classText, model.getPackageName() + "." + model.getValidatorClassName(), model.getImports());
        return renderer.render(TEMPLATE_GROUP, "typeFormatValidator", "m", new PojoTemplateModel(
                model.getPackageName(), resolved.imports(), model.getStaticImports(), resolved.classText()));
    }

    /**
     * Build the template model: the element's effective POJO property surface
     * (order + getter names + wrapping), joined to each property's DECLARED type
     * call for the constraint envelope.
     */
    ValidatorTemplateModel buildModel(RRootElement element, RGeneratedJavaClass<?> validatorClass) {
        RJavaPojoInterface pojo = ValidatorScan.toPojo(element, generatorModel, typeTranslator, typeUtil);
        String dataClassName = pojo.getSimpleName();
        String validatorClassName = validatorClass.getSimpleName();
        String packageName = validatorClass.getPackageName().withDots();
        String dataClassFqn = pojo.getCanonicalName().withDots();
        // The #306 collision law applied to the subject type itself (golden iso
        // ErrorTypeFormatValidator: no data-class import; every TYPE position FQN;
        // simple name in success/failure string literals).
        boolean collides = ValidatorScan.collidesWithJavaLang(dataClassName);
        // v3.2 seat 11 (D50): the data class is a first-claim sentinel at every TYPE position unless the
        // #306 java.lang law already writes it canonical (the string positions keep dataClassName)
        String dataClassJavaType = collides ? dataClassFqn : ImportCollisionResolver.typeRefOrBare(dataClassFqn);

        var imports = new ImportCollector(packageName);
        if (!collides) {
            imports.addImport(dataClassFqn);
        }
        imports.addImport("com.google.common.collect.Lists");
        imports.addImport("com.rosetta.model.lib.expression.ComparisonResult");
        imports.addImport("com.rosetta.model.lib.path.RosettaPath");
        imports.addImport("com.rosetta.model.lib.validation.ValidationResult");
        imports.addImport("com.rosetta.model.lib.validation.Validator");
        imports.addImport("java.util.List");

        Map<String, DeclaredType> declaredTypes = declaredTypesByPropertyName(element);
        // v3.3 seat 9 (PR #645 commit 12): the wiring walk moved WHOLE into wiredConditionClasses, which this
        // method now calls - the declaredTypesByPropertyName precedent, one declaration read by two consumers
        // (this emission and the IR route's aliasConditionClasses reconcile). The TYPE_ALIAS_CONDITION_DROPPED
        // refusal therefore fires at this line rather than inside the loop below; the whole method throws either
        // way, so not one generated byte moves, and the refusal names the same attribute of the same alias
        // because the seam walks the SAME scan in the SAME POJO order.
        Map<String, List<RGeneratedJavaClass<?>>> wiredByProperty = wiredConditionClasses(element);

        boolean stringUsed = false;
        boolean numberUsed = false;
        boolean ofUsed = false;
        boolean emptyUsed = false;
        boolean patternUsed = false;
        boolean bigDecimalUsed = false;

        var checkExprs = new ArrayList<String>();
        // v3.2 seat 3 (F9): the alias-condition wing's collection — the per-attribute wiring
        // plans in POJO order and the DISTINCT condition classes in first-appearance order
        // (upstream's LinkedHashSet `conditionDependencies`).
        List<WiringPlan> wiringPlans = new ArrayList<>();
        Map<String, RGeneratedJavaClass<?>> conditionDeps = new LinkedHashMap<>();
        for (ValidatorScan.ScannedAttribute scanned
                : ValidatorScan.scan(element, pojo, generatorModel, typeUtil)) {
            DeclaredType declared = declaredTypes.get(scanned.name());
            if (declared == null) {
                continue;
            }
            // v3.2 seat 3 (F9) — the alias-condition wing (the fix; the counter commit refused
            // here): an attribute whose declared type walks through aliases carrying conditions
            // wires every such condition class — upstream TypeFormatValidatorGenerator's
            // aliasHierarchyPerAttribute / checkTypeConditions (released-9.83.0 goldens: the
            // chaos C8PricedTypeFormatValidator; the seat's oracle groups alias-conditions /
            // alias-conditions-meta). Outermost alias first, conditions in declaration order;
            // the class names come from ConditionCases.casesOf — the SAME naming the datarule
            // class is emitted under (LAW 69). A condition the datarule walk REFUSES (the
            // parameterised sub-wing, fail-closed) refuses here too, at the site's counter — and
            // NOTE the blast radius (round-1 cq SF-10): the data-rule seat refuses ONE class, this
            // seat refuses the WHOLE validator of the type walking the alias (the wing is one
            // method over every attribute; there is no per-attribute file to drop), so every
            // other attribute's type-format check of that type is withheld with it. Fail-closed
            // and counted, never silent — the asymmetry is by construction, not oversight.
            List<RGeneratedJavaClass<?>> wired = wiredByProperty.getOrDefault(scanned.name(), List.of());
            if (!wired.isEmpty()) {
                // the meta wrapper's simple name from the TYPED item type (the same read
                // valueExpression's unwrap takes), never from the rendered local-type text
                String wrapperSimpleName = declared.metaWrapped()
                        ? typeUtil.getItemType(scanned.prop().getType()).getSimpleName() : null;
                // round 2 (oracle group alias-conditions-filescope): the multi local's element type
                // when it is a java.lang class (`List<Integer>`) — upstream's file scope registers
                // a java.lang type the moment the file WRITES it, so an attribute local of that
                // name escapes (`_Integer`); read from the TYPED item type, never the local-type text
                JavaClass<?> javaLangElement = typeUtil.getItemType(scanned.prop().getType())
                        instanceof JavaClass<?> javaLang && "java.lang".equals(javaLang.getPackageName().withDots())
                        ? javaLang : null;
                // v3.2 seat 11 (D50): the wrapper TYPE position is a first-claim sentinel (its canonical the
                // same string the import carries); the hoist NAME keeps the simple name
                String wrapperTypeRef = wrapperSimpleName == null ? null : ImportCollisionResolver.typeRefOrBare(
                        ((JavaClass<?>) typeUtil.getItemType(scanned.prop().getType())).getCanonicalName().withDots());
                wiringPlans.add(new WiringPlan(scanned.name(), scanned.getterCall(),
                        ValidatorScan.castType(scanned.prop(), typeUtil),
                        typeUtil.isList(scanned.prop().getType()), declared.metaWrapped(),
                        wrapperSimpleName, wrapperTypeRef, javaLangElement, wired));
                ValidatorScan.addCastImports(imports, scanned.prop(), typeUtil);
                for (RGeneratedJavaClass<?> conditionClass : wired) {
                    conditionDeps.putIfAbsent(conditionClass.getCanonicalName().withDots(), conditionClass);
                }
            }
            Optional<RType> envelope = TypeFormatConstraintScan.constrainedBasic(
                    declared.typeCall(), generatorModel.workspace());
            if (envelope.isEmpty()) {
                continue;
            }
            String value = valueExpression(scanned, declared.metaWrapped(), imports);
            RType constraint = envelope.get();
            if (constraint instanceof RStringType st) {
                stringUsed = true;
                String maxLen = optionalInt(st.maxLength());
                String pattern = st.pattern() // ci-allowlist: regex-on-structured-content (string ASSEMBLY: the next line spells the generated validator's own Pattern.compile call as text; no regex runs here)
                        .map(p -> "of(" + ImportCollisionResolver.typeRefOrBare("java.util.regex.Pattern") + ".compile(\""
                                + JavaStringUtil.escapeJava(p.toString()) + "\"))")
                        .orElse("empty()");
                // v3.1 C2d family 2 (typeformat-optional-presence): the Optional.of / Optional.empty
                // import need is the constraint's PRESENCE, read from the typed envelope - the
                // rendered "of(" / "empty()" prefix was only ever its spelling (optionalInt and the
                // pattern map are total: one form iff present, the other iff absent).
                ofUsed |= st.maxLength().isPresent() || st.pattern().isPresent();
                emptyUsed |= st.maxLength().isEmpty() || st.pattern().isEmpty();
                patternUsed |= st.pattern().isPresent();
                checkExprs.add("checkString(\"" + scanned.name() + "\", " + value + ", "
                        + st.minLength().orElse(0) + ", " + maxLen + ", " + pattern + ")");
            } else if (constraint instanceof RNumberType nt) {
                numberUsed = true;
                String digits = optionalInt(nt.digits());
                String fractionalDigits = optionalInt(nt.fractionalDigits());
                String min = optionalBigDecimal(nt.min());
                String max = optionalBigDecimal(nt.max());
                // v3.1 C2d family 2 (typeformat-optional-presence): presence from the typed envelope,
                // as for the string branch above (optionalInt / optionalBigDecimal are total).
                ofUsed |= nt.digits().isPresent() || nt.fractionalDigits().isPresent()
                        || nt.min().isPresent() || nt.max().isPresent();
                emptyUsed |= nt.digits().isEmpty() || nt.fractionalDigits().isEmpty()
                        || nt.min().isEmpty() || nt.max().isEmpty();
                bigDecimalUsed |= nt.min().isPresent() || nt.max().isPresent();
                checkExprs.add("checkNumber(\"" + scanned.name() + "\", " + value + ", "
                        + digits + ", " + fractionalDigits + ", " + min + ", " + max + ")");
            }
        }

        if (patternUsed) {
            imports.addImport("java.util.regex.Pattern");
        }
        if (bigDecimalUsed) {
            imports.addImport("java.math.BigDecimal");
        }

        var staticImports = new ImportCollector(packageName);
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

        var checks = new ArrayList<ValidatorCheckModel>();
        for (int i = 0; i < checkExprs.size(); i++) {
            checks.add(new ValidatorCheckModel(checkExprs.get(i), i == checkExprs.size() - 1));
        }

        // v3.2 seat 3 (F9): the wing's imports and models — only when a dependency exists
        // (the simple form stays byte-for-byte what it was). Every identifier is a request on the
        // vendored scope classes — upstream's GeneratorScope law itself (D7: byte-identical names;
        // round-2 spec MF-1 retired the round-1 NameScope copy under LAW 69 / Rule 3): the FILE
        // scope holds the class's own name and every simple name the file writes; the CLASS scope
        // `path` first and then one field per dependency in first-appearance order — two
        // dependencies of one simple name are NUMBERED from 0 (oracle golden
        // BothTypeFormatValidator: evenNatNonNeg0 / evenNatNonNeg1); the METHOD scope `o` and
        // `results`; the BODY scope the locals (renderRunConditionStatements). Golden-verified
        // over the seat's oracle groups: `i0..i3` (HolderTypeFormatValidator); `i1` / `i0` (the
        // index created BEFORE the local, LoopTypeFormatValidator); `_results` / `_o`
        // (ClashTypeFormatValidator); `_natNonNeg` (FieldClashTypeFormatValidator); `_new`
        // (the keyword, alias-conditions-reserved); `_Streams` / `_ArrayList` / `_Inject` /
        // `_Integer` (locals named after types the file writes, alias-conditions-filescope).
        // PROTOCOL (the vendored scope's contract): every request of a scope precedes its first
        // read — a read closes the scope and its parents — so the requests and the file-scope
        // claims all happen before the first getActualName below; merging the loops would
        // silently change the numbering (round-2 cq N-10).
        List<ValidatorTemplateModel.ConditionDependencyModel> depModels = new ArrayList<>();
        String runConditionsBody = "";
        String pathName = "path";
        if (!conditionDeps.isEmpty()) {
            imports.addImport("com.google.common.collect.Streams");
            imports.addImport("java.util.ArrayList");
            imports.addImport("javax.inject.Inject");
            JavaClassScope classScope = JavaClassScope.createAndRegisterIdentifier(validatorClass);
            JavaFileScope fileScope = classScope.getFileScope();
            GeneratedIdentifier pathId = classScope.createUniqueIdentifier("path");
            Map<String, GeneratedIdentifier> fieldIds = new LinkedHashMap<>();
            for (RGeneratedJavaClass<?> dep : conditionDeps.values()) {
                // upstream: classScope.createIdentifier(dep.toDependencyInstance, dep.simpleName.toFirstLower)
                fieldIds.put(dep.getCanonicalName().withDots(),
                        classScope.createIdentifier(dep, lowerFirst(dep.getSimpleName())));
            }
            // THE FILE'S SIMPLE-NAME CLAIMS, in upstream's TEXT order (oracle groups
            // alias-conditions-twins / -filescope / -boilerplate): the class header claims the
            // class's own name, `Validator`, the data class and `Inject` BEFORE the first field, so
            // a condition class of one of those names — or of a name an earlier condition class
            // holds — is written fully qualified (golden-verified: OwnTypeFormatValidator,
            // evenNatNonNeg1). Every OTHER type the file writes comes AFTER the fields, where
            // upstream claims the condition class first and writes the boilerplate type fully
            // qualified everywhere (`new java.util.ArrayList()`, the BoilerTypeFormatValidator
            // golden; the java.lang element type of a multi local, a18) — a text-order law this
            // template does not model, its boilerplate names being literal: REFUSED at the
            // register, never emitted with bytes that differ.
            // round 5 (spec SF-2 / cq SF-3): a claim is (canonical name, ORIGIN) — the origin recorded by the
            // producer that registers it, read by the refusal below for its witness clause; round 6 (cq SF-3 /
            // spec N-2): the PAIR is one declaration too, SilentDegradation.Claim, shared with the data-rule seat
            // (LAW 69), its constructor refusing a null half
            Map<String, SilentDegradation.Claim> claimedSimpleNames = new HashMap<>();
            claimedSimpleNames.put(validatorClassName, new SilentDegradation.Claim(packageName + "." + validatorClassName, SilentDegradation.ClaimOrigin.OWN_CLASS));
            // the header's four claims — WITNESSED: the own class name (oracle group
            // alias-conditions-filescope, OwnTypeFormatValidator), the data class and `Inject`
            // (oracle group alias-conditions-header, round 3: `protected
            // test.aliasheader.validation.datarule.PayLoad payLoad` / `...datarule.Inject inject`,
            // both fully qualified); REASONED from the same text order: `Validator` — a condition
            // class of that name is refused at the data-rule seat (its own boilerplate import), so
            // no validator wiring it ever emits beside an existing class (round-3 cq SF-3).
            Set<String> headerClaims = new HashSet<>(List.of(validatorClassName, "Validator", dataClassName, "Inject"));
            for (String imported : imports.getImports()) {
                claimedSimpleNames.putIfAbsent(DottedPath.splitOnDots(imported).last(), new SilentDegradation.Claim(imported, SilentDegradation.ClaimOrigin.IMPORT));
            }
            // round-3 cq MF-1: the java.lang element types the multi locals WRITE (`final
            // List<Integer> _xs`) are never imported (ImportCollector skips java.lang), so they
            // claim here from the SAME wiring plans the file scope reads below — one input for
            // both halves (LAW 69); a condition class of such a name is refused like any
            // boilerplate name (a18, lane AD).
            for (WiringPlan plan : wiringPlans) {
                if (plan.multi() && plan.javaLangElement() != null) { // the java.lang claim (a18)
                    claimedSimpleNames.putIfAbsent(plan.javaLangElement().getSimpleName(),
                            new SilentDegradation.Claim(plan.javaLangElement().getCanonicalName().withDots(), SilentDegradation.ClaimOrigin.JAVA_LANG));
                }
            }
            Map<String, String> depTypeNames = new LinkedHashMap<>();
            for (RGeneratedJavaClass<?> dep : conditionDeps.values()) {
                String canonical = dep.getCanonicalName().withDots();
                SilentDegradation.Claim claimant = claimedSimpleNames.putIfAbsent(dep.getSimpleName(), new SilentDegradation.Claim(canonical, SilentDegradation.ClaimOrigin.IMPORT));
                // v3.2 seat 11 (D50): a first-claim sentinel; the manual header-claim law below keeps writing the
                // canonical where it already did (the resolver never sees a canonical write)
                String typeName = ImportCollisionResolver.typeRefOrBare(canonical);
                if (claimant == null) {
                    imports.addImport(canonical);
                } else if (!claimant.canonical().equals(canonical)) {
                    if (!headerClaims.contains(dep.getSimpleName()) && !conditionDeps.containsKey(claimant.canonical())) {
                        // round-4 spec SF-1 / round-5 spec SF-2, cq SF-3: the message names what WITNESSES the
                        // shape, read from the claim's recorded ORIGIN (one declaration for both seats, LAW 69) -
                        // the banked oracle group for an import claim, the seat fixture alone for a java.lang claim
                        throw SilentDegradation.refuse(SilentDegradation.Site.BOILERPLATE_NAME_COLLISION,
                                "condition class " + canonical + " shares its simple name with " + claimant.canonical()
                                        + ", a type this validator writes after its @Inject fields; the released"
                                        + " 9.83.0 plugin claims the condition class first and writes " + claimant.canonical()
                                        + " fully qualified" + claimant.origin().witnessClause + " - a text-order import law the fork's"
                                        + " template does not model", element);
                    }
                    typeName = canonical;
                }
                depTypeNames.put(canonical, typeName);
            }
            // the file scope: every simple name the file writes — the imports (the boilerplate
            // set, the data class, the cast and wrapper types, the condition classes just imported)
            // and the java.lang element types of the multi locals, which upstream's file scope
            // registers on write — so an attribute-named local of one of those names escapes
            for (String imported : imports.getImports()) {
                String simple = DottedPath.splitOnDots(imported).last();
                if (!fileScope.isNameTaken(simple)) {
                    fileScope.createUniqueIdentifier(simple);
                }
            }
            for (WiringPlan plan : wiringPlans) {
                if (plan.multi() && plan.javaLangElement() != null) {
                    // SIDE EFFECT, the result unused (round-3 cq N-11): registers a java.lang type on
                    // write, as upstream's file scope does — load-bearing for the `_Integer` escape
                    fileScope.getIdentifier(plan.javaLangElement());
                }
            }
            Map<String, String> fieldNames = new LinkedHashMap<>();
            for (RGeneratedJavaClass<?> dep : conditionDeps.values()) {
                String canonical = dep.getCanonicalName().withDots();
                String fieldName = fieldIds.get(canonical).getActualName();
                fieldNames.put(canonical, fieldName);
                depModels.add(new ValidatorTemplateModel.ConditionDependencyModel(depTypeNames.get(canonical), fieldName));
            }
            pathName = pathId.getActualName();
            JavaMethodScope methodScope = classScope.createMethodScope("runConditions");
            GeneratedIdentifier instanceId = methodScope.createUniqueIdentifier("o");
            GeneratedIdentifier resultsId = methodScope.createUniqueIdentifier("results");
            runConditionsBody = renderRunConditions(wiringPlans, fieldNames, dataClassJavaType, pathName,
                    instanceId.getActualName(), resultsId.getActualName(), methodScope.getBodyScope());
        }

        return new ValidatorTemplateModel(packageName, validatorClassName,
                dataClassName, dataClassJavaType, dataClassFqn,
                imports.getImports(), staticImports.getStaticImports(), checks,
                depModels, runConditionsBody, pathName);
    }

    /**
     * v3.2 seat 3 (F9): one attribute's alias-condition wiring — its POJO name, getter call on
     * the instance ({@code getX()}, applied to the method's instance identifier at render) and
     * declared local type ({@link ValidatorScan#castType}: {@code Integer}, {@code List<Integer>},
     * {@code FieldWithMetaInteger}, {@code List<? extends FieldWithMetaInteger>}), whether it is
     * multi and meta-wrapped, and the condition classes to run in order (outermost alias first,
     * declaration order within an alias).
     */
    private record WiringPlan(String attrName, String getterCall, String localType, boolean multi,
                              boolean metaWrapped, String wrapperSimpleName, String wrapperTypeRef,
                              JavaClass<?> javaLangElement, List<RGeneratedJavaClass<?>> conditionClasses) {
    }

    /**
     * v3.2 seat 3 (F9): the {@code runConditions} method, every line carrying its own
     * indentation tabs and newline — upstream {@code checkTypeConditions} +
     * {@code addConditionValidationResultsCode}, golden-verified over the seat's oracle groups:
     * <ul>
     *   <li>SINGLE: {@code results.addAll(<field>.getValidationResults(path.newSubPath("<attr>"),
     *       o.<getter>()));} per condition class — no null guard;</li>
     *   <li>MULTI: {@code final <List type> <local> = o.<getter>();} / {@code if (<local> != null) {} /
     *       {@code for (int <i> = 0; <i> < <local>.size(); <i>++) {} / the calls with
     *       {@code path.newSubPath("<attr>").withIndex(<i>)} and {@code <local>.get(<i>)};</li>
     *   <li>META-WRAPPED: the wrapper is hoisted to a local named after its class
     *       ({@code final FieldWithMetaX <local> = …;}) and passed as
     *       {@code (<local> == null ? null : <local>.getValue())} — upstream's coercion deref;</li>
     *   <li>THE NAMES: every identifier is a request on the vendored {@link JavaStatementScope}
     *       (upstream's {@code GeneratorScope} law itself) in upstream's creation order — the
     *       method scope's {@code o} and {@code results}, then per attribute the loop index
     *       BEFORE the attribute-named local, then the wrapper hoist — read back after the last
     *       request (the scope's close), so a name numbers, escapes or stays bare exactly as
     *       upstream decides. The instance identifier {@code o} of the template's two OTHER
     *       methods is literal, as in upstream's template — a condition class named {@code O}
     *       (a field {@code o} in the class scope) would escape the method's {@code o} to
     *       {@code _o} here and not there; no carrier, banked (round-2 spec N-3).</li>
     * </ul>
     */
    static String renderRunConditions(List<WiringPlan> plans, Map<String, String> fieldNames,
                                      String dataClassJavaType, String pathName, String instanceName,
                                      String resultsName, JavaStatementScope bodyScope) {
        // The whole block is generator-computed (the datarule class-body convention): the
        // tab-only separator line upstream's template leaves before the method, the method
        // itself, and the closing brace — ST4 drops a whitespace-only template line, so the
        // separator cannot live in the template.
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

    /** One attribute's body-scope identifiers, requested in upstream's creation order. */
    private record PlanIds(GeneratedIdentifier index, GeneratedIdentifier local, GeneratedIdentifier hoist) {
    }

    /** The per-attribute statements of {@code runConditions} (see {@link #renderRunConditions}). */
    static String renderRunConditionStatements(List<WiringPlan> plans, Map<String, String> fieldNames,
                                               String pathName, String instanceName, String resultsName,
                                               JavaStatementScope bodyScope) {
        // pass 1: every body-scope identifier, in creation order — upstream creates the loop
        // index (createUniqueIdentifier("i")) BEFORE the attribute local (declareAsVariable),
        // and the wrapper hoist inside the loop after both (oracle golden Loop: `i1` the
        // local, `i0` the index)
        List<PlanIds> ids = new ArrayList<>();
        for (WiringPlan plan : plans) {
            GeneratedIdentifier index = plan.multi() ? bodyScope.createUniqueIdentifier("i") : null;
            GeneratedIdentifier local = plan.multi() ? bodyScope.createUniqueIdentifier(plan.attrName()) : null;
            GeneratedIdentifier hoist = plan.metaWrapped()
                    ? bodyScope.createUniqueIdentifier(lowerFirst(plan.wrapperSimpleName())) : null;
            ids.add(new PlanIds(index, local, hoist));
        }
        // pass 2: render, reading the closed scope's names
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
                for (RGeneratedJavaClass<?> conditionClass : plan.conditionClasses()) {
                    b.append("\t\t").append(resultsName).append(".addAll(").append(fieldOf(conditionClass, fieldNames))
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
            for (RGeneratedJavaClass<?> conditionClass : plan.conditionClasses()) {
                b.append("\t\t\t\t").append(resultsName).append(".addAll(").append(fieldOf(conditionClass, fieldNames))
                 .append(".getValidationResults(").append(subPath).append(".withIndex(").append(index)
                 .append("), ").append(value).append("));\n");
            }
            b.append("\t\t\t}\n");
            b.append("\t\t}\n");
        }
        return b.toString();
    }

    /** v3.2 seat 11 (D50): the first-claim sentinels the static runConditions renderers write. */
    private static final String T_LIST = ImportCollisionResolver.typeRefOrBare("java.util.List");
    private static final String T_VALIDATION_RESULT = ImportCollisionResolver.typeRefOrBare("com.rosetta.model.lib.validation.ValidationResult");
    private static final String T_ROSETTA_PATH = ImportCollisionResolver.typeRefOrBare("com.rosetta.model.lib.path.RosettaPath");
    private static final String T_ARRAY_LIST = ImportCollisionResolver.typeRefOrBare("java.util.ArrayList");

    private static String fieldOf(RGeneratedJavaClass<?> conditionClass, Map<String, String> fieldNames) {
        String canonical = conditionClass.getCanonicalName().withDots();
        // round-1 cq SF-9: a plan whose class was never registered would have rendered the
        // literal `null` — a silent non-compiling emission; fail here instead
        return Objects.requireNonNull(fieldNames.get(canonical), () -> "no injected field for " + canonical);
    }

    private static String lowerFirst(String s) {
        return s == null || s.isEmpty() ? s : Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }

    /**
     * A property's DECLARED type call (the constraint-envelope source) plus its
     * meta-wrapping flag, keyed by the POJO property name the check joins on.
     *
     * <p>RECONCILE SEAM (PR #644, the property gate): {@code public} so the IR route's derived-facts reconciler can
     * read {@link #declaredTypesByPropertyName}'s answer. Visibility only - see that method's seam note.
     */
    public record DeclaredType(RTypeCall typeCall, boolean metaWrapped) {
    }

    /**
     * Collect declared type calls by property name over the element's effective
     * surface: model attributes (the data-supertype chain — the same walk that
     * backs {@link ValidatorScan}) plus choice OPTIONS (the element's own when it
     * is a {@code choice}; the ancestor choice's when a data type extends one —
     * option properties carry the option type's PascalCase simple name).
     *
     * <p><b>RECONCILE SEAM (PR #644, the property gate).</b> {@code public} so the IR route's derived-facts
     * reconciler ({@code rune-ir-java}, {@code com.regnosys.rosetta.generator.java.ir.IRDerivedFactsReconciler}) can
     * join a property to the DECLARED type call this generator itself joins it to - the source half of the
     * {@code typeFormat.envelope} and {@code typeFormat.aliasChain} families. Visibility ONLY: the body, the
     * signature and every call site are unmoved, and no emission path calls the seam.
     */
    public Map<String, DeclaredType> declaredTypesByPropertyName(RRootElement element) {
        Map<String, DeclaredType> result = new LinkedHashMap<>();
        if (element instanceof RChoice choice) {
            for (RChoiceOption option : choice.options()) {
                result.put(optionPropertyName(option),
                        new DeclaredType(option.typeCall(), hasMetadataAnnotation(option.annotationRefs())));
            }
            return result;
        }
        RDataType dataType = (RDataType) element;
        for (RAttribute attr : generatorModel.allAttributes(dataType)) {
            // The override-inheritance union (PR #410): [metadata ...] persists
            // through overrides that do not restate it, so the meta-unwrap wing
            // must see the CHAIN's annotations (hold-out witness: Foo2.stringAttr
            // string(maxLength: 42) keeps Foo1's [metadata scheme] — golden checks
            // `o.getStringAttr().getValue()`). Identity on the corpus (zero such
            // overrides — wave B TRUE 100% could not have held otherwise).
            result.put(attr.name(),
                    new DeclaredType(attr.typeCall(), hasMetadataAnnotation(
                            com.regnosys.rosetta.generator.java.object.MetaFieldGenerator
                                    .allMetaAnnotationRefs(attr))));
        }
        // A data type extending a choice inherits the choice's OPTIONS as
        // properties (fixed 0..1 — see ValidatorScan); walk the data-supertype
        // chain for the choice boundary. Choices have no supertype, so at most
        // one choice contributes.
        RDataType current = dataType;
        while (current != null) {
            Optional<RChoice> choiceSuper = current.choiceSuperType();
            if (choiceSuper.isPresent()) {
                for (RChoiceOption option : choiceSuper.get().options()) {
                    result.putIfAbsent(optionPropertyName(option),
                            new DeclaredType(option.typeCall(), hasMetadataAnnotation(option.annotationRefs())));
                }
                break;
            }
            current = current.superType().orElse(null);
        }
        return result;
    }

    /**
     * THE ALIAS-CONDITION WIRING of every property of a validated element: the condition classes the type-format
     * wing {@code @Inject}s, in wiring order (the alias hierarchy outermost first, the conditions of each alias in
     * declaration order), keyed by the POJO property name the wing joins on. A property whose declared type walks
     * through no condition-carrying alias maps to an EMPTY list; a property with no declared type at all is absent.
     *
     * <p>It THROWS {@link SilentDegradation.Site#TYPE_ALIAS_CONDITION_DROPPED} for an attribute typed through a
     * PARAMETERISED alias carrying a condition - the post-9.83 sub-wing the released plugin renders as a
     * self-comparison - exactly where {@link #buildModel} used to throw it, and for the same attribute of the same
     * alias, because this walks the SAME {@link ValidatorScan#scan} in the SAME POJO order.
     *
     * <p><b>RECONCILE SEAM (v3.3 seat 9, PR #645 commit 12).</b> {@code public} so the IR route's derived-facts
     * reconciler can hold THIS generator's own wiring against the IR's alias-link law - the
     * {@code typeFormat.<p>.aliasConditionClasses} family. It is the generator's OWN decision and not a second
     * opinion about it: {@link #buildModel} calls this very method for the list it renders.
     */
    public Map<String, List<RGeneratedJavaClass<?>>> wiredConditionClasses(RRootElement element) {
        RJavaPojoInterface pojo = ValidatorScan.toPojo(element, generatorModel, typeTranslator, typeUtil);
        String dataClassName = pojo.getSimpleName();
        Map<String, DeclaredType> declaredTypes = declaredTypesByPropertyName(element);
        Map<String, List<RGeneratedJavaClass<?>>> wiredByProperty = new LinkedHashMap<>();
        for (ValidatorScan.ScannedAttribute scanned
                : ValidatorScan.scan(element, pojo, generatorModel, typeUtil)) {
            DeclaredType declared = declaredTypes.get(scanned.name());
            if (declared == null) {
                continue;
            }
            List<RGeneratedJavaClass<?>> wired = new ArrayList<>();
            for (var alias : TypeFormatConstraintScan.aliasHierarchy(
                    declared.typeCall(), generatorModel.workspace())) {
                ModelSymbolId aliasId = new ModelSymbolId(generatorModel.namespace(alias), alias.name());
                String parameter = ConditionCases.parameterisedOwner(alias);
                for (var conditionCase : ConditionCases.casesOf(alias)) {
                    if (parameter != null) {
                        throw SilentDegradation.refuse(SilentDegradation.Site.TYPE_ALIAS_CONDITION_DROPPED,
                                "attribute " + dataClassName + "." + scanned.name() + " is typed through the"
                                        + " PARAMETERISED typeAlias " + alias.name() + "(" + parameter
                                        + " \u2026), whose condition " + conditionCase.conditionName()
                                        + " the data-rule walk refuses (the parameterised condition wing"
                                        + " is post-9.83)",
                                alias);
                    }
                    wired.add(typeTranslator.toConditionJavaClass(aliasId, conditionCase.conditionName()));
                }
            }
            wiredByProperty.put(scanned.name(), wired);
        }
        return wiredByProperty;
    }

    /** The POJO property name of a choice option: the option type's simple name. */
    private static String optionPropertyName(RChoiceOption option) {
        String typeName = option.typeCall() != null ? option.typeCall().typeName() : "";
        int lastDot = typeName.lastIndexOf('.');
        return lastDot >= 0 ? typeName.substring(lastDot + 1) : typeName;
    }

    private static boolean hasMetadataAnnotation(
            List<com.regnosys.rosetta.ast.annotations.RAnnotationRef> annotationRefs) {
        return annotationRefs.stream().anyMatch(a -> "metadata".equals(a.annotationName()));
    }

    /**
     * The check VALUE expression: the plain POJO getter — NO cast (unlike the
     * cardinality family). Meta-wrapped attributes unwrap ({@code .getValue()}
     * single / stream-map list) per the upstream {@code getAttributeValue}
     * mechanism (xtend L247/L249). The LIST branch registers its two type
     * references — the wrapper class and {@code Collectors} (the CLASS spelling
     * {@code Collectors.toList()}, distinct from the template's static
     * {@code toList} import) — hold-out-golden-witnessed at PR #422
     * ({@code pojo-bulk-value-narrow}); zero corpus witnesses at 9.83 (see the
     * class Javadoc).
     */
    private String valueExpression(ValidatorScan.ScannedAttribute scanned, boolean metaWrapped,
            ImportCollector imports) {
        if (!metaWrapped) {
            return scanned.getterExpr();
        }
        if (typeUtil.isList(scanned.prop().getType())) {
            JavaClass<?> wrapper = (JavaClass<?>) typeUtil.getItemType(scanned.prop().getType());
            imports.addImport(wrapper.getPackageName().withDots() + "." + wrapper.getSimpleName());
            imports.addImport("java.util.stream.Collectors");
            // v3.2 seat 11 (D50): the wrapper and Collectors are first-claim sentinels (the same canonical the
            // import above carries, so a loser's import is the one dropped)
            return scanned.getterExpr() + ".stream().map("
                    + ImportCollisionResolver.typeRefOrBare(wrapper.getPackageName().withDots() + "." + wrapper.getSimpleName())
                    + "::getValue).collect(" + ImportCollisionResolver.typeRefOrBare("java.util.stream.Collectors") + ".toList())";
        }
        return scanned.getterExpr() + ".getValue()";
    }

    private static String optionalInt(OptionalInt value) {
        return value.isPresent() ? "of(" + value.getAsInt() + ")" : "empty()";
    }

    private static String optionalBigDecimal(Optional<BigDecimal> value) {
        // v3.2 seat 11 (D50): a first-claim sentinel like every library token the class writes
        return value.map(d -> "of(new " + ImportCollisionResolver.typeRefOrBare("java.math.BigDecimal") + "(\"" + d + "\"))")
                .orElse("empty()");
    }
}
