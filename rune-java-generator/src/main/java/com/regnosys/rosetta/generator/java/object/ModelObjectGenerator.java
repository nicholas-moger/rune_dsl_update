package com.regnosys.rosetta.generator.java.object;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.types.RChoice;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.JavaClassGenerator;
import com.regnosys.rosetta.generator.java.JavaNamingUtil;
import com.regnosys.rosetta.generator.java.template.ImportCollector;
import com.regnosys.rosetta.generator.java.template.ImportCollisionResolver;
import com.regnosys.rosetta.generator.java.template.TemplateRenderer;
import com.regnosys.rosetta.generator.java.template.model.PojoTemplateModel;
import com.regnosys.rosetta.generator.java.types.JavaPojoProperty;
import com.regnosys.rosetta.generator.java.types.JavaPojoPropertyOperationType;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.generator.java.types.RJavaPojoInterface;
import com.regnosys.rosetta.generator.java.util.ModelGeneratorUtil;
import com.rosetta.model.lib.ModelSymbolId;
import com.rosetta.util.DottedPath;
import com.rosetta.util.types.JavaType;
import com.regnosys.rosetta.generator.java.SilentDegradation;

/**
 * Generates the complete Java source file for a data type:
 * interface, Impl class, Builder interface, BuilderImpl class.
 *
 * <p>This is the most complex D11 generator. Output must match
 * upstream Xtend ModelObjectGenerator exactly.
 *
 * <p>Uses ST4 template {@code java-pojo.stg} for file-level assembly
 * (package + imports). The class body is built in Java for precise
 * blank-line whitespace control required by D11 byte-identity.
 *
 * <p>Migrated from monolithic StringBuilder to model/template split
 * as part of M8 (D15).
 */
public class ModelObjectGenerator extends JavaClassGenerator<RDataType, RJavaPojoInterface> {

    private static final String TEMPLATE_GROUP = "templates/java-pojo.stg";

    private final GeneratorModel generatorModel;
    private final JavaTypeTranslator typeTranslator;
    private final JavaTypeUtil typeUtil;
    private final ModelObjectBoilerplate boilerplate;
    private final ModelGeneratorUtil generatorUtil;
    private final TemplateRenderer renderer;

    public ModelObjectGenerator(GeneratorModel generatorModel, JavaTypeTranslator typeTranslator,
                                 JavaTypeUtil typeUtil) {
        this.generatorModel = generatorModel;
        this.typeTranslator = typeTranslator;
        this.typeUtil = typeUtil;
        this.boilerplate = new ModelObjectBoilerplate(typeUtil);
        this.generatorUtil = new ModelGeneratorUtil(generatorModel.workspace());
        this.renderer = new TemplateRenderer();
        this.renderer.loadGroupFromClasspath(TEMPLATE_GROUP);
    }

    @Override
    protected Stream<? extends RDataType> streamObjects(RModel model) {
        return model.rootElements().stream()
                .filter(e -> e instanceof RDataType)
                .map(e -> (RDataType) e);
    }

    @Override
    protected RJavaPojoInterface createTypeRepresentation(RDataType type) {
        return new RJavaPojoInterface(type, generatorModel, typeTranslator, typeUtil);
    }

    @Override
    protected String generate(RDataType type, RJavaPojoInterface pojo, String version) {
        PojoTemplateModel model = buildModel(type, null, pojo, version);
        return renderer.render(TEMPLATE_GROUP, "pojoFile", "m", model);
    }

    /**
     * Build the template model for a choice POJO (P2.1.3c β1 fix).
     *
     * <p>Choices have no inherited attributes, no override-keyword props, and no
     * super-data-type. Override-detection + choiceSuperType paths are forced to
     * empty/false; everything else (imports, body assembly, builder generation)
     * is shared with the RDataType path via the underlying pojo abstraction.
     *
     * <p>P2.1.3c T2 R1 F13/F14: the {@code choiceNode} is threaded into
     * {@link #buildModel} so the metadata-class import path constructs
     * {@link ModelSymbolId} from the original choice's raw namespace
     * (mirroring the RDataType branch's {@code generatorModel.symbolId(type)}),
     * not from {@code pojo.getPackageName()} (which is the already-escaped form
     * and would round-trip through {@link com.regnosys.rosetta.generator.java.scoping.JavaPackageName#escape}
     * a second time downstream).
     */
    PojoTemplateModel buildModelForChoice(RChoice choiceNode,
                                          RJavaPojoInterface pojo, String version) {
        return buildModel(null, choiceNode, pojo, version);
    }

    /**
     * Render the assembled {@link PojoTemplateModel} into the final Java source
     * string. Exposed for {@link ChoiceObjectGenerator} (P2.1.3c β1) which
     * shares the {@code java-pojo.stg} template + render pipeline.
     */
    String renderPojoFile(PojoTemplateModel model) {
        return renderer.render(TEMPLATE_GROUP, "pojoFile", "m", model);
    }

    /**
     * Build the template model: collect imports, build class body.
     *
     * <p>When {@code type == null} (RChoice path), override detection is skipped
     * and {@code extendsChoice} is forced false; metadata-class import, javadoc,
     * and override-only imports are conditionally elided. The {@code choiceNode}
     * supplies the raw rosetta namespace for the {@link ModelSymbolId} fallback
     * (mirrors the {@code symbolId(type)} call on the RDataType branch).
     */
    PojoTemplateModel buildModel(RDataType type, RChoice choiceNode,
                                  RJavaPojoInterface pojo, String version) {
        String typeName = pojo.getSimpleName();
        String packageName = pojo.getPackageName().withDots();
        boolean extended = pojo.getSuperPojo() != null;

        Collection<JavaPojoProperty> allProps = pojo.getAllProperties();
        Collection<JavaPojoProperty> ownProps = pojo.getOwnProperties();

        // facet pojoOverrideNaming (PR #323): the upstream law carries THREE independent
        // extension predicates (interface / Impl / BuilderImpl) — see upstream
        // ModelObjectGenerator.xtend L68-69 + ModelObjectBuilderGenerator.xtend L54-55:
        //  - the interface ALWAYS extends the super pojo (`extended` above);
        //  - the Impl extends the super Impl iff every own property is a COMPATIBLE
        //    (subtype) specialization of its parent (requiredness-only and covariant
        //    type specs keep extension);
        //  - the BuilderImpl extends the super BuilderImpl iff every own property has the
        //    IDENTICAL type as its parent (any type change breaks it — the builder fields
        //    would conflict; requiredness-only renames keep extension).
        // In the 9.83.0 corpus every specialization is subtype-compatible, so implExtended
        // == extended everywhere (the predicate is law-faithful, not corpus-tuned).
        boolean implExtended = extended
                && ownProps.stream().allMatch(JavaPojoProperty::isCompatibleTypeWithParent);
        boolean builderExtended = extended
                && ownProps.stream().allMatch(JavaPojoProperty::isSameTypeAsParent);

        // Imports
        var imports = new ImportCollector(packageName);
        imports.addImport("com.rosetta.model.lib.RosettaModelObject");
        imports.addImport("com.rosetta.model.lib.RosettaModelObjectBuilder");
        imports.addImport("com.rosetta.model.lib.annotations.RosettaDataType");
        imports.addImport("com.rosetta.model.lib.annotations.RuneDataType");
        // 9.83.0 alignment: choice-type POJOs additionally emit @RuneChoiceType.
        // Discriminator: type == null is the RChoice path throughout buildModel + buildBody.
        if (type == null) {
            imports.addImport("com.rosetta.model.lib.annotations.RuneChoiceType");
        }
        imports.addImport("com.rosetta.model.lib.meta.RosettaMetaData");
        imports.addImport("com.rosetta.model.lib.path.RosettaPath");
        imports.addImport("com.rosetta.model.lib.process.BuilderMerger");
        imports.addImport("com.rosetta.model.lib.process.BuilderProcessor");
        imports.addImport("com.rosetta.model.lib.process.Processor");
        imports.addImport("com.rosetta.model.lib.annotations.RuneAttribute");
        // Accessor/AccessorType/RosettaAttribute only needed when the type has any properties
        // (types with zero properties — neither own nor inherited — only use @RuneAttribute)
        if (!allProps.isEmpty()) {
            imports.addImport("com.rosetta.model.lib.annotations.RosettaAttribute");
            imports.addImport("com.rosetta.model.lib.annotations.Accessor");
            imports.addImport("com.rosetta.model.lib.annotations.AccessorType");
        }
        boolean hasMulti = allProps.stream().anyMatch(this::isList);
        boolean hasRequired = allProps.stream().anyMatch(JavaPojoProperty::isRequired);
        if (hasMulti) imports.addImport("com.rosetta.model.lib.annotations.Multi");
        if (hasRequired) imports.addImport("com.rosetta.model.lib.annotations.Required");
        if (hasMulti) imports.addImport("java.util.List");
        // Objects is needed for: non-list properties in equals(), list filter in constructor
        Collection<JavaPojoProperty> implProps = implExtended ? ownProps : allProps;
        boolean needsObjects = implProps.stream().anyMatch(p -> !isList(p))
                || implProps.stream().anyMatch(p -> isList(p) && isModelObj(p));
        if (needsObjects) imports.addImport("java.util.Objects");

        // Interface declarations determine additional imports (GlobalKey, etc.)
        for (com.rosetta.util.types.JavaClass<?> iface : pojo.getInterfaceDeclarations()) {
            String ifaceFqn = iface.getCanonicalName().withDots();
            // facet exceptionNestedFqn (PR #305): getInterfaceDeclarations() includes the
            // supertype. When that supertype is FQN-inlined (cross-package java.lang
            // collision — the *Exception subtypes), golden never imports it (collision-
            // blocked; every reference is FQN-inlined), so skip its import here. This is
            // the second super-import path alongside the gated one below; both must skip
            // for the unused import to drop. A non-colliding/same-package super is unaffected.
            if (extended && superFqnInlined(pojo)
                    && ifaceFqn.equals(pojo.getSuperPojo().getCanonicalName().withDots())) {
                continue;
            }
            if (!ifaceFqn.startsWith("java.lang.")) {
                imports.addImport(ifaceFqn);
            }
        }

        // RuneMetaType for types with synthetic "meta" property (own or inherited)
        boolean hasMetaProperty = allProps.stream().anyMatch(this::isMetaProperty);
        if (hasMetaProperty) {
            imports.addImport("com.rosetta.model.lib.annotations.RuneMetaType");
        }

        // RuneScopedAttributeReference for properties with [metadata address]
        boolean hasScopedRef = allProps.stream()
                .anyMatch(p -> p.getAttributeMetaTypes().contains(
                        com.regnosys.rosetta.generator.java.types.AttributeMetaType.SCOPED_REFERENCE));
        if (hasScopedRef) {
            imports.addImport("com.rosetta.model.lib.annotations.RuneScopedAttributeReference");
        }

        // RuneScopedAttributeKey + Key for properties with [metadata location]
        boolean hasScopedKey = allProps.stream()
                .anyMatch(p -> p.getAttributeMetaTypes().contains(
                        com.regnosys.rosetta.generator.java.types.AttributeMetaType.SCOPED_KEY));
        if (hasScopedKey) {
            imports.addImport("com.rosetta.model.lib.annotations.RuneScopedAttributeKey");
            imports.addImport("com.rosetta.model.lib.meta.Key");
        }

        // AttributeMeta for properties with [metadata id] that use
        // AttributeMeta.GLOBAL_KEY_FIELD in process methods
        boolean hasAttributeMeta = allProps.stream()
                .anyMatch(p -> p.getMeta() != null);
        if (hasAttributeMeta) {
            imports.addImport("com.rosetta.model.lib.process.AttributeMeta");
        }

        for (JavaPojoProperty prop : allProps) {
            addTypeImports(imports, prop);
        }
        // Metadata wrapper class import — symbol comes from the type's namespace + simple name.
        // RDataType: generatorModel.symbolId(type) builds from the raw rosetta namespace.
        // RChoice: mirror the same shape by using the threaded choiceNode's raw namespace
        // (NOT pojo.getPackageName(), which is the already-escaped form and would
        // double-escape downstream when toJavaMetaDataClass re-applies JavaPackageName.escape).
        ModelSymbolId typeId = (type != null)
                ? generatorModel.symbolId(type)
                : new ModelSymbolId(
                        generatorModel.namespace(choiceNode),
                        choiceNode.name());
        String metaClassCanonical = typeTranslator.toJavaMetaDataClass(typeId).getCanonicalName().withDots();
        imports.addImport(metaClassCanonical);
        if (extended && !superFqnInlined(pojo)) {
            // facet exceptionNestedFqn (PR #305): suppress the supertype import for the
            // cross-package java.lang-colliding case (the *Exception subtypes) — golden
            // FQN-inlines every reference (primary extends + nested Builder/Impl refs), so
            // the import is collision-blocked + unused. A same-package colliding super is
            // already not imported (ImportCollector skips same-package), so this is a no-op
            // there; a non-colliding super is imported as before.
            imports.addImport(pojo.getSuperPojo().getCanonicalName().withDots());
        }
        // List imports: ArrayList/Collectors needed for builder setters (all list props)
        // ImmutableList/ListEquals only needed for impl constructor (own list props)
        boolean hasAnyListProps = allProps.stream().anyMatch(this::isList);
        if (hasAnyListProps) {
            imports.addImport("java.util.ArrayList");
            imports.addImport("java.util.stream.Collectors");
        }
        boolean hasImplListProps = implProps.stream().anyMatch(this::isList);
        if (hasImplListProps) {
            imports.addImport("com.google.common.collect.ImmutableList");
            imports.addImport("com.rosetta.util.ListEquals");
        }
        // facet pojoOverrideNaming (PR #323): Consumer is used by the BuilderImpl
        // merge's basic-LIST casts, which iterate builderProps (= ownProps when the
        // builder extends, allProps otherwise) — NOT implProps. The two sets differ
        // exactly when the Impl extends (all specs subtype-compatible) but the Builder
        // does not (a type-changing spec): an INHERITED basic list then needs the
        // import (the #309 ListEquals precedent at the merge seat). builderProps is a
        // superset of implProps in every reachable combination, so this only ADDS the
        // import — a green file that had it keeps it; a file gaining it was an
        // already-waivered used-but-unimported (non-compiling) carrier.
        boolean hasMergeBasicListProps =
                (builderExtended ? ownProps : allProps).stream()
                        .anyMatch(p -> isList(p) && !isModelObj(p));
        if (hasMergeBasicListProps) {
            imports.addImport("java.util.function.Consumer");
        }

        // facet pojoOverrideNaming (PR #323): specialization is PROPERTY-MODEL-driven (the
        // upstream addPropertyIfNecessary law in RJavaPojoInterface.addProperty), replacing
        // the `override`-keyword + item-simple-name heuristic. An annotations-only override
        // (Case 0) creates NO property, so it contributes no members and no imports; a
        // SPECIALIZED property carries a parentProperty chain (one node per specializing
        // ancestor) that drives the @RosettaIgnore delegate setters — the delegates' param
        // types are the ancestors' property types, imported here (idempotent for the
        // equal-type rename chains).
        boolean anySpecialized = allProps.stream()
                .anyMatch(p -> p.getParentProperty() != null);
        // facet builderCompatMatrix (PR #412): a cardinality-changing specialization
        // (list ancestor over a single field, or the reverse) makes the compat members
        // emit the Collections.singletonList/emptyList pair AND the MapperC.of(x).get()
        // multi→single head (holdout witness: Foo2 — golden imports both).
        boolean anyCardinalityChangeInChain = false;
        boolean anyChainList = false;
        for (JavaPojoProperty p : allProps) {
            boolean mainIsList = isList(p);
            for (JavaPojoProperty anc = p.getParentProperty(); anc != null;
                    anc = anc.getParentProperty()) {
                addTypeImports(imports, anc);
                boolean ancIsList = typeUtil.isList(anc.getType());
                anyChainList |= ancIsList;
                if (ancIsList != mainIsList) {
                    anyCardinalityChangeInChain = true;
                }
            }
        }
        if (anySpecialized) {
            imports.addImport("com.rosetta.model.lib.annotations.RosettaIgnore");
            imports.addImport("com.rosetta.model.lib.annotations.RuneIgnore");
        }
        if (anyCardinalityChangeInChain) {
            imports.addImport("java.util.Collections");
            imports.addImport("com.rosetta.model.lib.mapper.MapperC");
        }
        // A list-shaped ancestor's compat arms declare List parameters/returns even
        // when the type has no list-shaped MAIN property (@Multi stays main-gated —
        // compat members carry the ignore pair, never @Multi).
        if (anyChainList) {
            imports.addImport("java.util.List");
        }

        var staticImports = new ImportCollector(packageName);
        // ofNullable used in impl setBuilderFields for all own properties
        // Extended types with no own properties don't need it
        if (!implProps.isEmpty()) {
            staticImports.addStaticImport("java.util.Optional.ofNullable");
        }

        // Build body
        // RChoice path (type == null): RChoice has no choiceSuperType — the AST type
        // hierarchy for choice POJOs is flat (no super, no extends).
        boolean extendsChoice = (type != null) && type.choiceSuperType().isPresent();
        Collection<JavaPojoProperty> builderProps = builderExtended ? ownProps : allProps;
        // facet builderListEquals (PR #309): the BUILDER equals() compares builderProps, which is
        // allProps when the builder is NON-extended (type-changing overrides present), so it can
        // list an INHERITED list prop the impl-only ListEquals gate above (keyed on implProps)
        // misses. Add ListEquals when builderProps carries any list — the boilerplate emits
        // ListEquals.listEquals(...) under the identical typeUtil.isList check, and addImport is
        // idempotent (no-op when implProps already triggered it). Green-safe by construction: a
        // used-but-unimported ListEquals never compiled, so every carrier is already waivered, and
        // the gate fires exactly when the builder equals() emits the call. Carrier: CommonLeg
        // (notionalAmountSchedule/notionalQuantitySchedule inherited from Leg; builder non-extended).
        if (builderProps.stream().anyMatch(this::isList)) {
            imports.addImport("com.rosetta.util.ListEquals");
        }
        fileWrittenSimpleNames = fileWrittenSimpleNames(imports, allProps);
        String body = buildBody(type, pojo, typeName, packageName, extended, implExtended,
                builderExtended, version, allProps, ownProps, builderProps, extendsChoice,
                ImportCollisionResolver.typeRefOrBare(metaClassCanonical));

        // v3.2 seat 11 (D50 - the file-scope first-claim law): every TYPE position of the class text is a
        // sentinel; resolved here in text order from the ONE seed upstream's JavaClassScope registers before
        // writing a byte (the POJO itself); the losers' imports dropped. A POJO with no collision renders
        // the pre-seat bytes. Both the data-type and the choice paths pass through here.
        // v3.3 seat 9 (PR #645 commit 6): the UNRESOLVED list, remembered for the section seam - see lastRawImports()
        lastRawImports.clear();
        lastRawImports.addAll(imports.getImports());
        ImportCollisionResolver.ClassResolution resolved = ImportCollisionResolver.resolveClass(
                body, packageName + "." + typeName, imports.getImports());
        return new PojoTemplateModel(packageName, resolved.imports(),
                staticImports.getStaticImports(), resolved.classText());
    }

    // -- Body building (exact whitespace for D11) ---------------------------------

    /**
     * facet exceptionSupertypeFqn (PR #304): the primary {@code extends} supertype is FQN-inlined
     * when its SIMPLE name collides with an implicitly-imported {@code java.lang} type — the only
     * corpus carrier is the fpml {@code fpml.consolidated.msg.Exception} (4 same-package subtypes:
     * {@code EventStatusException}, {@code MessageRejected}, {@code ServiceNotificationException},
     * {@code VerificationStatusException}). This mirrors upstream's {@code ImportingStringConcatenation}:
     * a java.lang-colliding simple name can NEVER be imported, so every reference is FQN-inlined. The
     * fork emitted the bare simple name, which for a SAME-PACKAGE supertype still resolved (same-package
     * resolution wins over {@code java.lang}) but byte-diverged from golden's defensive FQN. Green-safe
     * by construction: across all 5 cells 0 goldens extend a bare java.lang-colliding supertype simple
     * name (verified) — golden ALWAYS FQN-inlines such a supertype; a non-colliding supertype keeps the
     * simple name (the {@code else} below). The cross-package {@code *Exception} subtypes (which keep an
     * {@code import fpml.consolidated.msg.Exception} used by their bare nested {@code Exception.*Builder}
     * refs) move TOWARD golden on the primary-extends line and stay divergent on the other facets.
     * Detection caches {@code Class.forName} (no init) per simple name.
     */
    private static final java.util.Map<String, Boolean> JAVA_LANG_COLLISION =
            new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * v3.2 seat 11 (D50 - the file-scope first-claim law): the library types the POJO's class text writes,
     * each a first-claim sentinel ({@link ImportCollisionResolver#typeRefOrBare}) under its simple name.
     * Every write site of a library type in this generator, {@link ModelObjectBoilerplate} and
     * {@link PojoCompatEmitter} reads its constant here; the model types go through
     * {@link #valueSiteTypeName} / {@link #valueSiteTypeRef} / {@link #metaValueTypeName}; the class text is
     * resolved ONCE in {@link #buildModel} from the own class as the seed (upstream's
     * {@code JavaClassScope.createAndRegisterIdentifier}) and the losers' imports dropped. The java.lang
     * types the POJO writes ({@code Object}, {@code String}, {@code Class}, {@code Override},
     * {@code SuppressWarnings}) stay literal - upstream takes their simple names implicitly.
     */
    static final String T_LIST = ImportCollisionResolver.typeRefOrBare("java.util.List");
    static final String T_ARRAY_LIST = ImportCollisionResolver.typeRefOrBare("java.util.ArrayList");
    static final String T_COLLECTORS = ImportCollisionResolver.typeRefOrBare("java.util.stream.Collectors");
    /** round 1's cq SF-2: the java.math tokens the builder-compat number conversions write ({@link PojoCompatEmitter}). */
    static final String T_BIG_DECIMAL = ImportCollisionResolver.typeRefOrBare("java.math.BigDecimal");
    static final String T_BIG_INTEGER = ImportCollisionResolver.typeRefOrBare("java.math.BigInteger");
    static final String T_COLLECTIONS = ImportCollisionResolver.typeRefOrBare("java.util.Collections");
    static final String T_OBJECTS = ImportCollisionResolver.typeRefOrBare("java.util.Objects");
    static final String T_CONSUMER = ImportCollisionResolver.typeRefOrBare("java.util.function.Consumer");
    static final String T_IMMUTABLE_LIST = ImportCollisionResolver.typeRefOrBare("com.google.common.collect.ImmutableList");
    static final String T_LIST_EQUALS = ImportCollisionResolver.typeRefOrBare("com.rosetta.util.ListEquals");
    static final String T_ROSETTA_MODEL_OBJECT = ImportCollisionResolver.typeRefOrBare("com.rosetta.model.lib.RosettaModelObject");
    static final String T_ROSETTA_MODEL_OBJECT_BUILDER = ImportCollisionResolver.typeRefOrBare("com.rosetta.model.lib.RosettaModelObjectBuilder");
    static final String T_ROSETTA_META_DATA = ImportCollisionResolver.typeRefOrBare("com.rosetta.model.lib.meta.RosettaMetaData");
    static final String T_ROSETTA_PATH = ImportCollisionResolver.typeRefOrBare("com.rosetta.model.lib.path.RosettaPath");
    static final String T_PROCESSOR = ImportCollisionResolver.typeRefOrBare("com.rosetta.model.lib.process.Processor");
    static final String T_BUILDER_PROCESSOR = ImportCollisionResolver.typeRefOrBare("com.rosetta.model.lib.process.BuilderProcessor");
    static final String T_BUILDER_MERGER = ImportCollisionResolver.typeRefOrBare("com.rosetta.model.lib.process.BuilderMerger");
    static final String T_ATTRIBUTE_META = ImportCollisionResolver.typeRefOrBare("com.rosetta.model.lib.process.AttributeMeta");
    static final String T_KEY = ImportCollisionResolver.typeRefOrBare("com.rosetta.model.lib.meta.Key");
    static final String T_MAPPER_C = ImportCollisionResolver.typeRefOrBare("com.rosetta.model.lib.mapper.MapperC");
    static final String T_ROSETTA_DATA_TYPE = ImportCollisionResolver.typeRefOrBare("com.rosetta.model.lib.annotations.RosettaDataType");
    static final String T_RUNE_DATA_TYPE = ImportCollisionResolver.typeRefOrBare("com.rosetta.model.lib.annotations.RuneDataType");
    static final String T_RUNE_CHOICE_TYPE = ImportCollisionResolver.typeRefOrBare("com.rosetta.model.lib.annotations.RuneChoiceType");
    static final String T_RUNE_ATTRIBUTE = ImportCollisionResolver.typeRefOrBare("com.rosetta.model.lib.annotations.RuneAttribute");
    static final String T_ROSETTA_ATTRIBUTE = ImportCollisionResolver.typeRefOrBare("com.rosetta.model.lib.annotations.RosettaAttribute");
    static final String T_ACCESSOR = ImportCollisionResolver.typeRefOrBare("com.rosetta.model.lib.annotations.Accessor");
    static final String T_ACCESSOR_TYPE = ImportCollisionResolver.typeRefOrBare("com.rosetta.model.lib.annotations.AccessorType");
    static final String T_MULTI = ImportCollisionResolver.typeRefOrBare("com.rosetta.model.lib.annotations.Multi");
    static final String T_REQUIRED = ImportCollisionResolver.typeRefOrBare("com.rosetta.model.lib.annotations.Required");
    static final String T_RUNE_META_TYPE = ImportCollisionResolver.typeRefOrBare("com.rosetta.model.lib.annotations.RuneMetaType");
    static final String T_RUNE_SCOPED_REF = ImportCollisionResolver.typeRefOrBare("com.rosetta.model.lib.annotations.RuneScopedAttributeReference");
    static final String T_RUNE_SCOPED_KEY = ImportCollisionResolver.typeRefOrBare("com.rosetta.model.lib.annotations.RuneScopedAttributeKey");
    static final String T_ROSETTA_IGNORE = ImportCollisionResolver.typeRefOrBare("com.rosetta.model.lib.annotations.RosettaIgnore");
    static final String T_RUNE_IGNORE = ImportCollisionResolver.typeRefOrBare("com.rosetta.model.lib.annotations.RuneIgnore");

    // Package-private (was private) so ModelObjectBoilerplate (same package) can reuse the
    // collision check to FQN-inline a java.lang-colliding model attribute type at the
    // interface-process {@code .class} site (facet javaLangAttrFqn, PR #306).
    // PUBLIC since coverage wave D: the datarule generator (object.datarule package)
    // applies the same #306 law at its subject-type positions — visibility only,
    // behavior unchanged.
    public static boolean collidesWithJavaLang(String simpleName) {
        return JAVA_LANG_COLLISION.computeIfAbsent(simpleName, n -> {
            try {
                Class.forName("java.lang." + n, false, ModelObjectGenerator.class.getClassLoader());
                return Boolean.TRUE;
            } catch (ClassNotFoundException | LinkageError | RuntimeException e) {
                if (e instanceof SilentDegradation.Refusal __refusal) throw __refusal;
                // A refusal is not a probe failure: these clauses exist to answer
                // "does this JDK name resolve?" with FALSE, and answering that for a
                // refusal would convert a deliberate decline into a quiet wrong answer.
                // No refusal can arise inside Class.forName today; the guard keeps the
                // C0 contract true by construction if that ever changes (Copilot R2).
                // Defensive (Copilot R1 #304): Class.forName can also throw a RuntimeException
                // (e.g. SecurityException via the loader under a SecurityManager). Treat ANY
                // lookup failure as "no collision" -> bare simple name. This is the green-safe
                // fail-direction: an FQN on a NON-colliding supertype would turn a green file
                // red, whereas bare is the corpus-default and degrades gracefully (at worst the
                // defensive-FQN flips are lost), never crashing generation. (We keep the app
                // loader, NOT the bootstrap null loader Copilot suggested -- a null loader would
                // itself add a getClassLoader permission check under a SecurityManager.)
                return Boolean.FALSE;
            }
        });
    }

    /**
     * facet exceptionNestedFqn (PR #305): true when a supertype's nested
     * {@code Builder}/{@code Impl}/{@code BuilderImpl} references must be FQN-inlined
     * (and its import suppressed) — when the supertype's SIMPLE name collides with an
     * implicitly-imported {@code java.lang} type (see {@link #collidesWithJavaLang}) AND
     * the supertype lives in a DIFFERENT package. The only corpus carrier is the fpml
     * {@code fpml.consolidated.msg.Exception} (golden FQN-inlines the nested
     * {@code Exception.ExceptionBuilder}/{@code .ExceptionImpl}/{@code .ExceptionBuilderImpl}
     * refs and drops the collision-blocked {@code import fpml.consolidated.msg.Exception}
     * for the ~15 cross-package {@code *Exception} subtypes — the #304 compounding follow-on).
     * A SAME-package colliding supertype keeps the BARE nested ref (golden: the 4
     * {@code fpml.consolidated.msg.*Exception} subtypes resolve it same-package — the #304
     * carriers), so the cross-package gate excludes them (green-safe by construction). A
     * non-colliding supertype always keeps its bare simple name.
     */
    private static boolean superFqnInlined(RJavaPojoInterface pojo) {
        return pojo.getSuperPojo() != null
                && collidesWithJavaLang(pojo.getSuperPojo().getSimpleName())
                && !pojo.getSuperPojo().getPackageName().withDots()
                        .equals(pojo.getPackageName().withDots());
    }

    /**
     * facet exceptionNestedFqn (PR #305): the qualifier for a supertype's nested
     * {@code Builder}/{@code Impl}/{@code BuilderImpl} reference — the FQN
     * ({@code fpml.consolidated.msg.Exception}) when {@link #superFqnInlined}, else the
     * bare simple name ({@code Exception}). The nested simple name suffix is appended by
     * the caller (e.g. {@code superNestedQualifier(pojo) + "." + superSimple + "Builder"}).
     */
    private static String superNestedQualifier(RJavaPojoInterface pojo) {
        return superFqnInlined(pojo)
                ? pojo.getSuperPojo().getCanonicalName().withDots()
                : pojo.getSuperPojo().getSimpleName();
    }

    /**
     * v3.3 seat 9 (PR #645 commit 4) - THE POJO SECTION SEAM, read by {@code IRDataTypeEmitterTest} (rune-ir-java)
     * and by nothing in production. {@link #buildBody} records the TEXT OFFSETS of its first five sections as it
     * writes them and fills this map from the finished builder, so the IR emitter's per-section render can be held
     * against the OLD GENERATOR'S OWN bytes rather than against a hand-written expectation. Not one byte of the
     * body moves: the seam only remembers where the sections start and end.
     *
     * <p>The keys are the section names of {@code IRDataTypeEmitter.Section}: {@code TYPE_JAVADOC},
     * {@code ANNOTATIONS}, {@code INTERFACE_DECLARATION}, {@code META_DATA}, {@code GETTERS}, - since v3.3 seat 9,
     * PR #645 commit 6 - {@code BUILD_METHODS} and {@code UTILITY_METHODS}, - since v3.3 seat 9, PR #645
     * commit 7 - {@code PROCESS} and {@code BUILDER_INTERFACE}, - since v3.3 seat 9, PR #645 commit 8 -
     * {@code IMPL}, and - since v3.3 seat 9, PR #645 commit 9 - {@code BUILDER_IMPL}, which ENDS before the
     * class-closing brace {@link #buildBody} appends after it: that brace belongs to the file FRAME, not to
     * section 12, which is why the IR emitter appends it after its last section rather than inside section 12.
     * The map holds the sections of the LAST body this generator built.
     */
    private final java.util.Map<String, String> lastBodySections = new java.util.LinkedHashMap<>();

    /**
     * THE RAW IMPORT SEAM (v3.3 seat 9, PR #645 commit 6), beside {@link #lastBodySections}: the collector's import
     * list of the last {@link #buildModel} call BEFORE the D50 first-claim resolution drops the losers
     * ({@code ImportCollisionResolver.resolveClass}). {@code PojoTemplateModel.getImports()} carries the RESOLVED
     * list, which is the same list for a class text with no simple-name collision and a SHORTER one for a class text
     * with one - so a COLLIDING fixture cannot be held against the IR emitter's own (unresolved) import SET without
     * it. Pure bookkeeping: nothing branches on it and not one byte of the model moves.
     */
    private final java.util.List<String> lastRawImports = new java.util.ArrayList<>();

    /** TEST SEAM (v3.3 seat 9): the sections of the last body {@link #buildBody} wrote. No production caller. */
    java.util.Map<String, String> lastBodySections() {
        return java.util.Map.copyOf(lastBodySections);
    }

    /** TEST SEAM (v3.3 seat 9, PR #645 commit 6): the UNRESOLVED imports of the last {@link #buildModel} call. */
    java.util.List<String> lastRawImports() {
        return java.util.List.copyOf(lastRawImports);
    }

    /**
     * TEST SEAM (v3.3 seat 9, PR #645 commit 8): the {@code equals} / {@code hashCode} / {@code toString} TEXT the
     * LAST {@link ModelObjectBoilerplate#boilerPlate} or {@link ModelObjectBoilerplate#builderBoilerPlate} call
     * produced, read straight off the boilerplate instance this generator writes its own bytes with. It exists so
     * the IR emitter's section-13 text law can be held against the old generator's own bytes ON ITS OWN, apart from
     * the {@code Impl} / {@code BuilderImpl} whole those bytes are embedded (and re-indented) inside.
     *
     * <p>After a whole {@link #buildBody} the LAST such call is {@code generateBuilderImplClass}'s
     * {@code builderBoilerPlate} ({@code :1413}), so what this hands back is the BUILDER variant - the {@code Impl}
     * variant is held by section 11's own whole. No production caller; not one byte moves.
     */
    String lastBoilerPlate() {
        return boilerplate.lastBoilerPlate();
    }

    private String buildBody(RDataType type, RJavaPojoInterface pojo,
                              String typeName, String packageName, boolean extended,
                              boolean implExtended, boolean builderExtended, String version,
                              Collection<JavaPojoProperty> allProps,
                              Collection<JavaPojoProperty> ownProps,
                              Collection<JavaPojoProperty> builderProps,
                              boolean extendsChoice, String metaClassRef) {
        var body = new StringBuilder();

        // Javadoc — RChoice path (type == null) uses pojo.getJavadoc(), which the
        // RJavaPojoInterface choice ctor builds from RChoice.definition() with an
        // empty docReferences() list. The RDataType path keeps the direct
        // generatorUtil.javadoc() call which threads docReferences().
        String javadoc;
        if (type != null) {
            javadoc = generatorUtil.javadoc(
                    type.definition().orElse(null), type.docReferences(), version);
        } else {
            javadoc = pojo.getJavadoc();
        }
        if (javadoc != null) {
            body.append(javadoc).append("\n");
        }
        int seamAfterJavadoc = body.length();   // v3.3 seat 9 section seam - see lastBodySections()

        // Interface declaration
        body.append("@").append(T_ROSETTA_DATA_TYPE).append("(value=\"").append(typeName).append("\", builder=")
            .append(typeName).append(".").append(typeName).append("BuilderImpl.class, version=\"")
            .append(version != null ? version : "").append("\")\n");
        String modelShortName = packageName.contains(".")
                ? packageName.substring(0, packageName.indexOf('.'))
                : packageName;
        body.append("@").append(T_RUNE_DATA_TYPE).append("(value=\"").append(typeName).append("\", model=\"")
            .append(modelShortName).append("\", builder=")
            .append(typeName).append(".").append(typeName).append("BuilderImpl.class, version=\"")
            .append(version != null ? version : "").append("\")\n");
        // 9.83.0 alignment: choice-type POJOs additionally carry @RuneChoiceType.
        // Mirrors upstream ModelObjectGenerator.xtend `«IF javaType.choiceType»@«RuneChoiceType»«ENDIF»`.
        // Discriminator: type == null is the RChoice path (see L316).
        if (type == null) {
            body.append("@").append(T_RUNE_CHOICE_TYPE).append("\n");
        }

        int seamAfterAnnotations = body.length();   // v3.3 seat 9 section seam
        body.append("public interface ").append(typeName);
        if (extended) {
            // facet exceptionSupertypeFqn (PR #304): FQN-inline a supertype whose SIMPLE name
            // collides with an implicitly-imported java.lang type (see collidesWithJavaLang).
            String superSimple = pojo.getSuperPojo().getSimpleName();
            // v3.2 seat 11 (D50): a first-claim sentinel unless the #304 java.lang law writes it canonical
            String superRef = collidesWithJavaLang(superSimple)
                    ? pojo.getSuperPojo().getCanonicalName().withDots()
                    : ImportCollisionResolver.typeRefOrBare(pojo.getSuperPojo().getCanonicalName().withDots());
            body.append(" extends ").append(superRef);
        } else {
            body.append(" extends ").append(T_ROSETTA_MODEL_OBJECT);
        }
        // Additional interfaces: GlobalKey, Templatable
        for (var iface : pojo.getInterfaceDeclarations()) {
            String ifaceName = iface.getSimpleName();
            // Skip the primary extends (RosettaModelObject or super type) — already handled
            if (!"RosettaModelObject".equals(ifaceName) && !ifaceName.equals(
                    extended ? pojo.getSuperPojo().getSimpleName() : "")) {
                body.append(", ").append(ImportCollisionResolver.typeRefOrBare(iface.getCanonicalName().withDots()));
            }
        }
        body.append(" {\n\n");
        int seamAfterInterfaceDeclaration = body.length();   // v3.3 seat 9 section seam

        // metaData static field
        body.append("\t").append(metaClassRef).append(" metaData = new ")
            .append(metaClassRef).append("();\n");
        body.append("\n");
        int seamAfterMetaData = body.length();   // v3.3 seat 9 section seam

        // Section: Getter Methods
        // For extended types: only OWN properties (inherited come from parent interface)
        // For non-extended types: own == all, so same result
        body.append("\t/*********************** Getter Methods  ***********************/\n");
        for (JavaPojoProperty prop : ownProps) {
            generateInterfaceGetter(body, prop);
        }
        body.append("\n");
        // v3.3 seat 9 (PR #645 commit 4) section seam: the five sections the IR emitter renders, recorded from the
        // offsets above. Pure bookkeeping over the SAME builder - no append, no branch on it, no byte moved.
        lastBodySections.clear();
        lastBodySections.put("TYPE_JAVADOC", body.substring(0, seamAfterJavadoc));
        lastBodySections.put("ANNOTATIONS", body.substring(seamAfterJavadoc, seamAfterAnnotations));
        lastBodySections.put("INTERFACE_DECLARATION",
                body.substring(seamAfterAnnotations, seamAfterInterfaceDeclaration));
        lastBodySections.put("META_DATA", body.substring(seamAfterInterfaceDeclaration, seamAfterMetaData));
        lastBodySections.put("GETTERS", body.substring(seamAfterMetaData, body.length()));
        int seamAfterGetters = body.length();   // v3.3 seat 9 (PR #645 commit 6) section seam

        // Section: Build Methods
        body.append("\t/*********************** Build Methods  ***********************/\n");
        body.append("\t").append(typeName).append(" build();\n");
        body.append("\t\n");
        body.append("\t").append(typeName).append(".").append(typeName)
            .append("Builder toBuilder();\n");
        body.append("\t\n");
        body.append("\tstatic ").append(typeName).append(".").append(typeName)
            .append("Builder builder() {\n");
        body.append("\t\treturn new ").append(typeName).append(".")
            .append(typeName).append("BuilderImpl();\n");
        body.append("\t}\n");
        body.append("\n");
        int seamAfterBuildMethods = body.length();   // v3.3 seat 9 (PR #645 commit 6) section seam

        // Section: Utility Methods
        body.append("\t/*********************** Utility Methods  ***********************/\n");
        body.append("\t@Override\n");
        body.append("\tdefault ").append(T_ROSETTA_META_DATA).append("<? extends ").append(typeName)
            .append("> metaData() {\n");
        body.append("\t\treturn metaData;\n");
        body.append("\t}\n");
        body.append("\t\n");
        body.append("\t@Override\n");
        body.append("\t@").append(T_RUNE_ATTRIBUTE).append("(\"@type\")\n");
        body.append("\tdefault Class<? extends ").append(typeName).append("> getType() {\n");
        body.append("\t\treturn ").append(typeName).append(".class;\n");
        body.append("\t}\n");
        body.append("\t\n");
        // v3.3 seat 9 (PR #645 commit 6) section seam: sections 7 and 8, recorded from the offsets above. Pure
        // bookkeeping over the SAME builder - no append, no branch on it, no byte moved.
        lastBodySections.put("BUILD_METHODS", body.substring(seamAfterGetters, seamAfterBuildMethods));
        lastBodySections.put("UTILITY_METHODS", body.substring(seamAfterBuildMethods, body.length()));
        int seamAfterUtilityMethods = body.length();   // v3.3 seat 9 (PR #645 commit 7) section seam

        // Process method
        body.append(boilerplate.processMethod(pojo));
        body.append("\n");
        int seamAfterProcess = body.length();   // v3.3 seat 9 (PR #645 commit 7) section seam

        // Builder interface
        generateBuilderInterface(body, pojo, typeName, extended, allProps, ownProps);
        // v3.3 seat 9 (PR #645 commit 7) section seam: sections 9 and 10, recorded from the offsets above. Pure
        // bookkeeping over the SAME builder - no append, no branch on it, no byte moved.
        lastBodySections.put("PROCESS", body.substring(seamAfterUtilityMethods, seamAfterProcess));
        lastBodySections.put("BUILDER_INTERFACE", body.substring(seamAfterProcess, body.length()));
        int seamAfterBuilderInterface = body.length();   // v3.3 seat 9 (PR #645 commit 8) section seam

        // facet builderCompatMatrix (PR #412): the ancestor-shape compat emitter — one
        // per class, seeded with the field identifiers (the local-escape context).
        // Null when no property carries a specialization chain (the dominant case).
        PojoCompatEmitter compatEmitter = PojoCompatEmitter.anySpecialized(allProps)
                ? new PojoCompatEmitter(this, typeUtil, allProps)
                : null;

        // Impl class (implExtended per the upstream isCompatibleTypeWithParent law)
        generateImplClass(body, pojo, typeName, implExtended, allProps, version, compatEmitter);
        // v3.3 seat 9 (PR #645 commit 8) section seam: section 11, recorded from the offset above. Pure
        // bookkeeping over the SAME builder - no append, no branch on it, no byte moved.
        lastBodySections.put("IMPL", body.substring(seamAfterBuilderInterface, body.length()));
        int seamAfterImpl = body.length();   // v3.3 seat 9 (PR #645 commit 9) section seam

        // BuilderImpl class (builderExtended = false when any own property changes type)
        generateBuilderImplClass(body, pojo, typeName, builderExtended, allProps, builderProps,
                extendsChoice, compatEmitter);
        // v3.3 seat 9 (PR #645 commit 9) section seam: section 12, recorded from the offset above and taken
        // BEFORE the class-closing brace below - the brace is the FRAME's, not the section's, which is why the IR
        // emitter appends it after its last section rather than inside section 12. Pure bookkeeping over the SAME
        // builder - no append, no branch on it, no byte moved.
        lastBodySections.put("BUILDER_IMPL", body.substring(seamAfterImpl, body.length()));

        body.append("}\n");
        return body.toString();
    }

    // -- Interface getters --------------------------------------------------------

    private void generateInterfaceGetter(StringBuilder body, JavaPojoProperty prop) {
        String getterName = prop.getOperationName(JavaPojoPropertyOperationType.GET);
        String returnType = interfaceGetterType(prop);

        if (prop.getJavadoc() != null && !prop.getJavadoc().isEmpty()) {
            String indented = indentJavadoc(prop.getJavadoc(), "\t");
            body.append(indented).append("\n");
        }

        // facet pojoOverrideNaming (PR #323): @Override iff the getter keeps the parent's
        // name (upstream getterOverridesParentGetter — a subtype-compatible specialization);
        // an incompatible specialization takes a RENAMED getter with no @Override
        // (corpus-inert: every 9.83.0 specialization is subtype-compatible).
        if (prop.getterOverridesParentGetter()) {
            body.append("\t@Override\n");
        }
        body.append("\t").append(returnType).append(" ").append(getterName).append("();\n");
    }

    private static String indentJavadoc(String javadoc, String indent) {
        return javadoc.lines()
                .map(line -> indent + line)
                .collect(java.util.stream.Collectors.joining("\n"));
    }

    private static String reindent(String code, String extraIndent) {
        return code.lines()
                .map(line -> line.isEmpty() ? line : extraIndent + line)
                .collect(java.util.stream.Collectors.joining("\n"))
                + (code.endsWith("\n") ? "\n" : "");
    }

    // -- Builder interface --------------------------------------------------------

    private void generateBuilderInterface(StringBuilder body, RJavaPojoInterface pojo,
                                           String typeName, boolean extended,
                                           Collection<JavaPojoProperty> allProps,
                                           Collection<JavaPojoProperty> ownProps) {
        body.append("\t/*********************** Builder Interface  ***********************/\n");
        body.append("\tinterface ").append(typeName).append("Builder extends ")
            .append(typeName);
        if (!extended) {
            body.append(", ").append(T_ROSETTA_MODEL_OBJECT_BUILDER);
        } else {
            // facet exceptionNestedFqn (PR #305): FQN-inline the qualifier for a
            // cross-package java.lang-colliding super (see superNestedQualifier).
            body.append(", ").append(superNestedQualifier(pojo))
                .append(".").append(pojo.getSuperPojo().getSimpleName()).append("Builder");
        }
        // Additional builder interfaces: GlobalKey.GlobalKeyBuilder, Templatable.TemplatableBuilder
        for (var iface : pojo.getInterfaceDeclarations()) {
            String ifaceName = iface.getSimpleName();
            if ("GlobalKey".equals(ifaceName)) {
                body.append(", GlobalKey.GlobalKeyBuilder");
            } else if ("Templatable".equals(ifaceName)) {
                body.append(", Templatable.TemplatableBuilder");
            }
        }
        body.append(" {\n");

        String builderRetType = typeName + "." + typeName + "Builder";

        // getOrCreate + @Override getter: own properties only (inherited come from parent builder)
        for (JavaPojoProperty prop : ownProps) {
            if (isModelObj(prop)) {
                String getOrCreateName = prop.getOperationName(JavaPojoPropertyOperationType.GET_OR_CREATE);
                String getterName = prop.getOperationName(JavaPojoPropertyOperationType.GET);
                if (isList(prop)) {
                    body.append("\t\t").append(builderSingleType(prop))
                        .append(" ").append(getOrCreateName).append("(int index);\n");
                } else {
                    body.append("\t\t").append(builderSingleType(prop))
                        .append(" ").append(getOrCreateName).append("();\n");
                }
                body.append("\t\t@Override\n");
                body.append("\t\t").append(builderGetterType(prop))
                    .append(" ").append(getterName).append("();\n");
            }
        }

        // facet pojoOverrideNaming (PR #323): upstream pojoBuilderInterfaceSetterMethods
        // (ModelObjectGenerator.xtend L147-176) emits the setter declarations GENERATION-
        // MAJOR — recurse into the super chain FIRST (top-most ancestor's own properties
        // first), then each generation's own properties; every non-main generation's
        // declarations carry @Override (covariant-return re-declarations), the main (this)
        // generation's are bare. A specialized property belongs to the generation that
        // SPECIALIZED it, so an override chain contributes one declaration per specializing
        // generation (golden: `@Override setLeg1(LegV1)` in the v1 segment ... `setLeg1(
        // CommonLeg)` in the own segment; a renamed setter re-declares under its renamed
        // name in its declaring generation's segment). For a hierarchy without
        // specializations this reduces to the flattened inherited-then-own order (each
        // generation appends its new properties), byte-identical to the previous
        // two-phase emission.
        emitBuilderInterfaceSetters(body, pojo, pojo, builderRetType);
        body.append("\n");
        body.append("\t\t@Override\n");
        body.append("\t\tdefault void process(").append(T_ROSETTA_PATH).append(" path, ")
            .append(T_BUILDER_PROCESSOR).append(" processor) {\n");
        for (JavaPojoProperty prop : allProps) {
            String getterName = prop.getOperationName(JavaPojoPropertyOperationType.GET);
            // facet javaLangAttrFqn (PR #306): the BUILDER-process MODEL branch visits the
            // builder type (X.XBuilder.class) which golden leaves BARE even for a
            // java.lang-colliding X, so it uses the bare simple name (NOT itemTypeName,
            // which FQNs the VALUE sites). Bare is correct for the 2 SAME-package carriers
            // (same-package precedence resolves X). LATENT GAP (PR #306 Copilot R1, no
            // corpus carrier): a CROSS-package colliding X would bind bare X to java.lang.X
            // here (import suppressed) → uncompilable; it would need the outer FQN-inlined
            // (see dotQualifiedBuilderType — unreachable in the 9.83.0 corpus).
            String processTypeName = itemSimpleName(prop);
            boolean isModel = typeUtil.isRosettaModelObject(prop.getType());
            String metaFlags = prop.getMeta() != null
                    ? ", " + T_ATTRIBUTE_META + "." + prop.getMeta().name() : "";
            body.append("\t\t\t");
            if (isModel) {
                body.append("processRosetta(path.newSubPath(\"").append(prop.getName())
                    .append("\"), processor, ").append(processTypeName)
                    .append(".").append(processTypeName)
                    .append("Builder.class, ").append(getterName).append("()")
                    .append(metaFlags).append(");\n");
            } else {
                // facet javaLangAttrFqn (W42 finding #13, PR #426): the BASIC branch
                // visits the VALUE type (X.class — an enum/basic attribute), which
                // golden FQNs for a java.lang-colliding X exactly like the
                // interface-process (oracle witness func-dispatch-collision MathInput:
                // `processBasic(…, test.dispatchcollision.Math.class, …)` in BOTH
                // process methods; the #306 bare law above is the MODEL branch's
                // X.XBuilder.class only). itemTypeName = the value-site FQN law; the
                // bare form bound same-package Math (compiling but byte-divergent) and
                // would bind a CROSS-package collider to java.lang.X (uncompilable).
                body.append("processor.processBasic(path.newSubPath(\"").append(prop.getName())
                    .append("\"), ").append(itemTypeName(prop)).append(".class, ")
                    .append(getterName).append("(), this")
                    .append(metaFlags).append(");\n");
            }
        }
        body.append("\t\t}\n");
        body.append("\t\t\n");

        body.append("\n");
        body.append("\t\t").append(typeName).append(".").append(typeName).append("Builder prune();\n");
        body.append("\t}\n");
        body.append("\n");
    }

    /**
     * facet pojoOverrideNaming (PR #323): the generation-major builder-interface setter
     * emission — upstream {@code pojoBuilderInterfaceSetterMethods}' super-first recursion.
     * Each generation contributes exactly its OWN properties (a Case-0 override is not a
     * property, so non-specializing intermediate generations contribute nothing); the main
     * (most-derived) generation's declarations are bare, every ancestor generation's carry
     * {@code @Override}.
     */
    private void emitBuilderInterfaceSetters(StringBuilder body, RJavaPojoInterface mainPojo,
                                              RJavaPojoInterface currentPojo,
                                              String builderRetType) {
        RJavaPojoInterface sp = currentPojo.getSuperPojo();
        if (sp != null) {
            emitBuilderInterfaceSetters(body, mainPojo, sp, builderRetType);
        }
        boolean inherited = currentPojo != mainPojo;
        for (JavaPojoProperty prop : currentPojo.getOwnProperties()) {
            emitBuilderInterfaceSetterGroup(body, prop, builderRetType, inherited);
        }
    }

    /**
     * One property's builder-interface setter declaration group (single: SET [+ SET_VALUE
     * when meta-wrapped]; list: ADD/ADD-idx [+ ADD_VALUE ×2]/ADD-list/SET-list [+
     * ADD_VALUE/SET_VALUE list]), each line prefixed with {@code @Override} when the
     * property belongs to an ancestor generation. Names come from the property's
     * law-computed compatibility names ({@code getOperationName}).
     */
    private void emitBuilderInterfaceSetterGroup(StringBuilder body, JavaPojoProperty prop,
                                                  String builderRetType, boolean inherited) {
        String itemType = itemTypeName(prop);
        String setterName = prop.getOperationName(JavaPojoPropertyOperationType.SET);
        // v3.2 seat 3, round 1 (oracle group alias-conditions-reserved): the builder-INTERFACE list
        // declarations keep the attribute's name RAW (an upper-initial name stays as written -
        // golden AnnaDsb) but a JAVA KEYWORD escapes exactly as the single-setter param does
        // (golden `addNew(Integer _new)` / `setNew(List<Integer> _new)`, the same _-prefix law
        // interfaceParamName applies): the seat the PR #231 note anticipated, now carried.
        if (isList(prop)) {
            // round-2 cq N-2: the list arm's only; round 2 (oracle group alias-conditions-filescope): a name
            // the file writes as a type escapes too (`addArrayList(Integer _ArrayList)`, `_Object`, `_List`)
            String listParamName = fileWrittenSimpleNames.contains(prop.getName())
                    ? "_" + prop.getName() : JavaNamingUtil.escapeJavaKeyword(prop.getName());
            String addName = prop.getOperationName(JavaPojoPropertyOperationType.ADD);
            boolean isModel = isModelObj(prop);
            boolean hasMeta = prop.getMetaValueType() != null;
            String listParam = isModel
                    ? T_LIST + "<? extends " + itemType + ">"
                    : T_LIST + "<" + itemType + ">";
            if (inherited) body.append("\t\t@Override\n");
            body.append("\t\t").append(builderRetType).append(" ")
                .append(addName).append("(").append(itemType).append(" ")
                .append(listParamName).append(");\n");
            if (inherited) body.append("\t\t@Override\n");
            body.append("\t\t").append(builderRetType).append(" ")
                .append(addName).append("(").append(itemType).append(" ")
                .append(listParamName).append(", int idx);\n");
            if (hasMeta) {
                String addValueName = prop.getOperationName(JavaPojoPropertyOperationType.ADD_VALUE);
                String valueTypeName = metaValueTypeName(prop);
                if (inherited) body.append("\t\t@Override\n");
                body.append("\t\t").append(builderRetType).append(" ")
                    .append(addValueName).append("(").append(valueTypeName).append(" ")
                    .append(listParamName).append(");\n");
                if (inherited) body.append("\t\t@Override\n");
                body.append("\t\t").append(builderRetType).append(" ")
                    .append(addValueName).append("(").append(valueTypeName).append(" ")
                    .append(listParamName).append(", int idx);\n");
            }
            if (inherited) body.append("\t\t@Override\n");
            body.append("\t\t").append(builderRetType).append(" ")
                .append(addName).append("(").append(listParam)
                .append(" ").append(listParamName).append(");\n");
            if (inherited) body.append("\t\t@Override\n");
            body.append("\t\t").append(builderRetType).append(" ")
                .append(setterName).append("(").append(listParam)
                .append(" ").append(listParamName).append(");\n");
            if (hasMeta) {
                String addValueName = prop.getOperationName(JavaPojoPropertyOperationType.ADD_VALUE);
                String setValueName = prop.getOperationName(JavaPojoPropertyOperationType.SET_VALUE);
                String valueTypeName = metaValueTypeName(prop);
                String valueListParam = T_LIST + "<? extends " + valueTypeName + ">";
                if (inherited) body.append("\t\t@Override\n");
                body.append("\t\t").append(builderRetType).append(" ")
                    .append(addValueName).append("(").append(valueListParam)
                    .append(" ").append(listParamName).append(");\n");
                if (inherited) body.append("\t\t@Override\n");
                body.append("\t\t").append(builderRetType).append(" ")
                    .append(setValueName).append("(").append(valueListParam)
                    .append(" ").append(listParamName).append(");\n");
            }
        } else {
            // P2.1.3c β1: interfaceParamName gives choice options (PascalCase getName())
            // the upstream `_Cash` parameter prefix, keyed on the name-vs-value-type
            // collision; ordinary attribute names stay bare.
            String paramName = interfaceParamName(prop);
            if (inherited) body.append("\t\t@Override\n");
            body.append("\t\t").append(builderRetType).append(" ")
                .append(setterName).append("(").append(itemType).append(" ")
                .append(paramName).append(");\n");
            if (prop.getMetaValueType() != null) {
                String valueTypeName = metaValueTypeName(prop);
                String setValueName = prop.getOperationName(JavaPojoPropertyOperationType.SET_VALUE);
                if (inherited) body.append("\t\t@Override\n");
                body.append("\t\t").append(builderRetType).append(" ")
                    .append(setValueName).append("(").append(valueTypeName).append(" ")
                    .append(paramName).append(");\n");
            }
        }
    }

    // -- Impl class ---------------------------------------------------------------

    private void generateImplClass(StringBuilder body, RJavaPojoInterface pojo,
                                    String typeName, boolean extended,
                                    Collection<JavaPojoProperty> allProps, String version,
                                    PojoCompatEmitter compatEmitter) {
        body.append("\t/*********************** Immutable Implementation of ").append(typeName)
            .append("  ***********************/\n");
        body.append("\tclass ").append(typeName).append("Impl");
        if (extended) {
            // facet exceptionNestedFqn (PR #305): FQN-inline the qualifier for a
            // cross-package java.lang-colliding super (see superNestedQualifier).
            body.append(" extends ").append(superNestedQualifier(pojo))
                .append(".").append(pojo.getSuperPojo().getSimpleName()).append("Impl");
        }
        body.append(" implements ").append(typeName).append(" {\n");

        Collection<JavaPojoProperty> implProps = extended ? pojo.getOwnProperties() : allProps;
        for (JavaPojoProperty prop : implProps) {
            // Field name is lowercased (P2.1.3c β1: choice option props have
            // PascalCase getName(); the Java field uses camelCase). For regular
            // RDataType attrs already-lowercase names, toFirstLower is identity.
            body.append("\t\tprivate final ").append(interfaceGetterType(prop))
                .append(" ").append(fieldName(prop)).append(";\n");
        }
        body.append("\t\t\n");

        body.append("\t\tprotected ").append(typeName).append("Impl(")
            .append(typeName).append(".").append(typeName).append("Builder builder) {\n");
        if (extended) {
            body.append("\t\t\tsuper(builder);\n");
        }
        for (JavaPojoProperty prop : implProps) {
            String getterName = prop.getOperationName(JavaPojoPropertyOperationType.GET);
            String fld = fieldName(prop);
            if (isList(prop) && isModelObj(prop)) {
                body.append("\t\t\tthis.").append(fld)
                    .append(" = ofNullable(builder.").append(getterName)
                    .append("()).filter(_l->!_l.isEmpty()).map(list -> list.stream().filter(").append(T_OBJECTS)
                    .append("::nonNull).map(f->f.build()).filter(").append(T_OBJECTS).append("::nonNull).collect(")
                    .append(T_IMMUTABLE_LIST).append(".toImmutableList())).orElse(null);\n");
            } else if (isList(prop)) {
                body.append("\t\t\tthis.").append(fld)
                    .append(" = ofNullable(builder.").append(getterName)
                    .append("()).filter(_l->!_l.isEmpty()).map(").append(T_IMMUTABLE_LIST).append("::copyOf).orElse(null);\n");
            } else if (isModelObj(prop)) {
                body.append("\t\t\tthis.").append(fld)
                    .append(" = ofNullable(builder.").append(getterName)
                    .append("()).map(f->f.build()).orElse(null);\n");
            } else {
                body.append("\t\t\tthis.").append(fld)
                    .append(" = builder.").append(getterName).append("();\n");
            }
        }
        body.append("\t\t}\n");
        body.append("\t\t\n");

        for (JavaPojoProperty prop : implProps) {
            String getterName = prop.getOperationName(JavaPojoPropertyOperationType.GET);
            body.append("\t\t@Override\n");
            body.append("\t\t@").append(T_ROSETTA_ATTRIBUTE).append("(\"").append(prop.getName()).append("\")\n");
            body.append("\t\t@").append(T_ACCESSOR).append("(").append(T_ACCESSOR_TYPE).append(".GETTER)\n");
            if (prop.isRequired()) body.append("\t\t@").append(T_REQUIRED).append("\n");
            if (isList(prop)) body.append("\t\t@").append(T_MULTI).append("\n");
            body.append("\t\t@").append(T_RUNE_ATTRIBUTE).append("(\"").append(prop.getName()).append("\")\n");
            if (isMetaProperty(prop)) body.append("\t\t@").append(T_RUNE_META_TYPE).append("\n");
            if (hasScopedReference(prop)) body.append("\t\t@").append(T_RUNE_SCOPED_REF).append("\n");
            if (hasScopedKey(prop)) body.append("\t\t@").append(T_RUNE_SCOPED_KEY).append("\n");
            body.append("\t\tpublic ").append(interfaceGetterType(prop))
                .append(" ").append(getterName).append("() {\n");
            body.append("\t\t\treturn ").append(fieldName(prop)).append(";\n");
            body.append("\t\t}\n");
            body.append("\t\t\n");

            // facet builderCompatMatrix (PR #412): impl-side derived compat getters —
            // one @RosettaIgnore/@RuneIgnore getter per non-overridden ancestor shape
            // (upstream ModelObjectGenerator L254, gated on the impl NOT extending).
            if (!extended && compatEmitter != null) {
                compatEmitter.appendImplDerivedGetters(body, prop);
            }
        }

        body.append("\t\t@Override\n");
        body.append("\t\tpublic ").append(typeName).append(" build() {\n");
        body.append("\t\t\treturn this;\n");
        body.append("\t\t}\n");
        body.append("\t\t\n");

        body.append("\t\t@Override\n");
        body.append("\t\tpublic ").append(typeName).append(".").append(typeName)
            .append("Builder toBuilder() {\n");
        body.append("\t\t\t").append(typeName).append(".").append(typeName)
            .append("Builder builder = builder();\n");
        body.append("\t\t\tsetBuilderFields(builder);\n");
        body.append("\t\t\treturn builder;\n");
        body.append("\t\t}\n");
        body.append("\t\t\n");

        body.append("\t\tprotected void setBuilderFields(").append(typeName).append(".")
            .append(typeName).append("Builder builder) {\n");
        if (extended) {
            body.append("\t\t\tsuper.setBuilderFields(builder);\n");
        }
        for (JavaPojoProperty prop : implProps) {
            String getterName = prop.getOperationName(JavaPojoPropertyOperationType.GET);
            // facet pojoOverrideNaming (PR #323): the SETTER compatibility name (renamed
            // for an equal-erasure specialization, plain otherwise) — upstream
            // setBuilderFields uses getOperationName(SET) unconditionally.
            String setterName = prop.getOperationName(JavaPojoPropertyOperationType.SET);
            body.append("\t\t\tofNullable(").append(getterName).append("()).ifPresent(builder::")
                .append(setterName).append(");\n");
        }
        body.append("\t\t}\n");

        body.append("\n");
        body.append(reindent(boilerplate.boilerPlate(pojo, extended, implProps), "\t"));

        body.append("\t}\n");
        body.append("\n");
    }

    // -- BuilderImpl class --------------------------------------------------------

    /**
     * @param builderExtended false when type has type-changing overrides (BuilderImpl doesn't
     *     extend parent — it declares all fields directly since parent field types would conflict)
     */
    private void generateBuilderImplClass(StringBuilder body, RJavaPojoInterface pojo,
                                           String typeName, boolean builderExtended,
                                           Collection<JavaPojoProperty> allProps,
                                           Collection<JavaPojoProperty> builderProps,
                                           boolean extendsChoice,
                                           PojoCompatEmitter compatEmitter) {
        body.append("\t/*********************** Builder Implementation of ").append(typeName)
            .append("  ***********************/\n");
        body.append("\tclass ").append(typeName).append("BuilderImpl");
        if (builderExtended) {
            // facet exceptionNestedFqn (PR #305): FQN-inline the qualifier for a
            // cross-package java.lang-colliding super (see superNestedQualifier).
            body.append(" extends ").append(superNestedQualifier(pojo))
                .append(".").append(pojo.getSuperPojo().getSimpleName()).append("BuilderImpl");
        }
        body.append(" implements ").append(typeName).append(".").append(typeName)
            .append("Builder {\n");
        body.append("\t\n");

        for (JavaPojoProperty prop : builderProps) {
            body.append("\t\tprotected ").append(builderFieldType(prop))
                .append(" ").append(fieldName(prop));
            if (isList(prop)) {
                body.append(" = new ").append(T_ARRAY_LIST).append("<>()");
            }
            body.append(";\n");
        }
        body.append("\t\t\n");

        for (JavaPojoProperty prop : builderProps) {
            String getterName = prop.getOperationName(JavaPojoPropertyOperationType.GET);

            body.append("\t\t@Override\n");
            body.append("\t\t@").append(T_ROSETTA_ATTRIBUTE).append("(\"").append(prop.getName()).append("\")\n");
            body.append("\t\t@").append(T_ACCESSOR).append("(").append(T_ACCESSOR_TYPE).append(".GETTER)\n");
            if (prop.isRequired()) body.append("\t\t@").append(T_REQUIRED).append("\n");
            if (isList(prop)) body.append("\t\t@").append(T_MULTI).append("\n");
            body.append("\t\t@").append(T_RUNE_ATTRIBUTE).append("(\"").append(prop.getName()).append("\")\n");
            if (isMetaProperty(prop)) body.append("\t\t@").append(T_RUNE_META_TYPE).append("\n");
            if (hasScopedReference(prop)) body.append("\t\t@").append(T_RUNE_SCOPED_REF).append("\n");
            if (hasScopedKey(prop)) body.append("\t\t@").append(T_RUNE_SCOPED_KEY).append("\n");
            body.append("\t\tpublic ").append(builderGetterType(prop)).append(" ")
                .append(getterName).append("() {\n");
            body.append("\t\t\treturn ").append(fieldName(prop)).append(";\n");
            body.append("\t\t}\n");
            body.append("\t\t\n");

            if (isModelObj(prop)) {
                String getOrCreateName = prop.getOperationName(JavaPojoPropertyOperationType.GET_OR_CREATE);
                String singleType = builderSingleType(prop);
                String fld = fieldName(prop);
                if (isList(prop)) {
                    // Upstream rule (P2.1.3c T5b): `getOrCreate*(int index)` param uses
                    // `_index` whenever the desired name `index` is already taken in the
                    // type's property scope. Upstream Xtend uses
                    // GeneratorScope.createUniqueIdentifier("index") + escapeName ("_" +
                    // name) — see rune-dsl/rune-lang/.../ModelObjectBuilderGenerator.xtend
                    // L255 + GeneratorScope.java L57/L282-285.
                    //
                    // The conflict surfaces in two empirically observed cases — both
                    // unified under the inherited-property check:
                    //   1. choice parent with `Index` option (CDM 6.15+: BasketConstituent
                    //      extends Observable; option `Index` → property "Index" which
                    //      lowercases to "index").
                    //   2. RDataType parent with `index <Type> (0..1)` field (CDM 5.35:
                    //      BasketConstituent extends Product; Product has `index Index (0..1)`).
                    //
                    // The legacy `extendsChoice` flag caught only sub-case 1 (and only
                    // because the canonical choice with an `index` option happens to be
                    // Observable); sub-case 2 was a Cluster F waiver entry until this fix.
                    String idxParam = hasIndexPropertyInScope(pojo) ? "_index" : "index";
                    body.append("\t\t@Override\n");
                    body.append("\t\tpublic ").append(singleType).append(" ")
                        .append(getOrCreateName).append("(int ").append(idxParam).append(") {\n");
                    body.append("\t\t\tif (").append(fld).append("==null) {\n");
                    body.append("\t\t\t\tthis.").append(fld).append(" = new ").append(T_ARRAY_LIST).append("<>();\n");
                    body.append("\t\t\t}\n");
                    body.append("\t\t\treturn getIndex(").append(fld)
                        .append(", ").append(idxParam).append(", () -> {\n");
                    body.append("\t\t\t\t\t\t").append(singleType).append(" new")
                        .append(JavaNamingUtil.toFirstUpper(fld)).append(" = ")
                        .append(itemTypeName(prop)).append(".builder();\n");
                    if (hasScopedKey(prop)) {
                        body.append("\t\t\t\t\t\tnew").append(JavaNamingUtil.toFirstUpper(fld))
                            .append(".getOrCreateMeta().addKey(").append(T_KEY).append(".builder().setScope(\"DOCUMENT\"));\n");
                    }
                    body.append("\t\t\t\t\t\treturn new")
                        .append(JavaNamingUtil.toFirstUpper(fld)).append(";\n");
                    body.append("\t\t\t\t\t});\n");
                    body.append("\t\t}\n");
                    body.append("\t\t\n");
                } else {
                    // The `result` local escapes to `_result` when a property in
                    // scope claims the name — the same upstream class-scope
                    // deduplication rule as the `_index` param above (the class
                    // scope holds ALL fields, own + inherited; a method-local
                    // whose desired name an ancestor scope took escapes with an
                    // underscore prefix). Hold-out witness: the name-escaping
                    // fixture's attribute literally named `result` (PR #410).
                    String resLocal = hasResultPropertyInScope(pojo) ? "_result" : "result";
                    body.append("\t\t@Override\n");
                    body.append("\t\tpublic ").append(singleType).append(" ")
                        .append(getOrCreateName).append("() {\n");
                    body.append("\t\t\t").append(singleType).append(" ").append(resLocal).append(";\n");
                    body.append("\t\t\tif (").append(fld).append("!=null) {\n");
                    body.append("\t\t\t\t").append(resLocal).append(" = ").append(fld).append(";\n");
                    body.append("\t\t\t}\n");
                    body.append("\t\t\telse {\n");
                    body.append("\t\t\t\t").append(resLocal).append(" = ").append(fld).append(" = ")
                        .append(itemTypeName(prop)).append(".builder();\n");
                    if (hasScopedKey(prop)) {
                        body.append("\t\t\t\t").append(resLocal)
                            .append(".getOrCreateMeta().toBuilder().addKey(").append(T_KEY).append(".builder().setScope(\"DOCUMENT\"));\n");
                    }
                    body.append("\t\t\t}\n");
                    body.append("\t\t\t\n");
                    body.append("\t\t\treturn ").append(resLocal).append(";\n");
                    body.append("\t\t}\n");
                    body.append("\t\t\n");
                }
            }

            // facet builderCompatMatrix (PR #412): builder-side derived compat getters
            // + the getOrCreate compat delegates — one group per non-overridden
            // ancestor shape (upstream ModelObjectBuilderGenerator L193, gated on the
            // builder NOT extending).
            if (!builderExtended && compatEmitter != null) {
                compatEmitter.appendBuilderDerivedGetters(body, pojo, prop);
            }
        }

        // facet fpmlListParamEscape (PR #331): sibling field names of the builder
        // class — the pluralized List-overload param escapes with '_' when it
        // shadows a DIFFERENT property's field (upstream JavaClassScope
        // createUniqueIdentifier semantics; the singular adder param is always
        // '_'-escaped against its OWN field).
        // Seat-1 #331 note (re-audited at PR #386 per its own instruction): the set now
        // holds FIELD identifiers (fieldName = decap + keyword escape) — what the plural
        // param would actually shadow; upstream compares scope identifiers, not raw rune
        // names. Byte-identical for every lowercase non-keyword attribute (the fpml #331
        // carrier is all-lowercase); the #386 upper-initial carriers (AnnaDsb) have no
        // plural-colliding siblings either way.
        Set<String> siblingFieldNames = allProps.stream()
                .map(ModelObjectGenerator::fieldName)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        for (JavaPojoProperty prop : allProps) {
            String builderRetType = typeName + "." + typeName + "Builder";
            if (isList(prop)) {
                generateListSetters(body, prop, typeName, builderRetType, siblingFieldNames);
            } else {
                generateSingleSetter(body, prop, typeName, builderRetType);
                // The meta convenience setter belongs to the MAIN group and precedes
                // the ancestor arms (upstream doSetter emits SET, SET_VALUE, THEN the
                // parentProperty recursion at L574 — golden Foo3: setParentList(RWMGC),
                // setParentListValue(GrandChild), then the RWMC/Foo1-shape arms).
                // Corpus-neutral reorder: no corpus carrier is both meta-wrapped and
                // specialized (waves A–D TRUE 100%).
                if (prop.getMetaValueType() != null) {
                    generateMetaValueSetter(body, prop, typeName);
                }
            }
            // facet builderCompatMatrix (PR #412): the full ancestor-shape setter arms
            // — NEAREST ancestor first, each arm's shape dispatched on the ANCESTOR
            // (upstream doSetter's parentProperty recursion), bodies coercing to the
            // main shape via the POJO-seat coercion table. Replaces the #323
            // single-cardinality instanceof/class.cast delegate (byte-identical for
            // its corpus carriers) and cashes the #323 list-specialization
            // simplification note (list ancestors now emit the ADD/SET family).
            if (compatEmitter != null) {
                compatEmitter.appendAncestorSetterArms(body, prop, builderRetType,
                        siblingFieldNames);
            }
        }

        body.append("\t\t@Override\n");
        body.append("\t\tpublic ").append(typeName).append(" build() {\n");
        body.append("\t\t\treturn new ").append(typeName).append(".")
            .append(typeName).append("Impl(this);\n");
        body.append("\t\t}\n");
        body.append("\t\t\n");

        body.append("\t\t@Override\n");
        body.append("\t\tpublic ").append(typeName).append(".").append(typeName)
            .append("Builder toBuilder() {\n");
        body.append("\t\t\treturn this;\n");
        body.append("\t\t}\n");
        body.append("\t\n");

        body.append("\t\t@SuppressWarnings(\"unchecked\")\n");
        body.append("\t\t@Override\n");
        body.append("\t\tpublic ").append(typeName).append(".").append(typeName)
            .append("Builder prune() {\n");
        if (builderExtended) {
            body.append("\t\t\tsuper.prune();\n");
        }
        for (JavaPojoProperty prop : builderProps) {
            if (isModelObj(prop)) {
                String fld = fieldName(prop);
                // facet isoPruneConfig (PR #331): a (type, attribute) pair listed in the
                // model project's rosetta-config.yml generators.doNotPrune renders the
                // KEEP form — upstream IShouldPrune.Default.shouldBePruned returns false
                // iff isPruningDisabledInConfig, so the ModelObjectBuilderGenerator
                // template's ELSE arm fires: `if (x!=null) x.prune();` (non-list) /
                // the list stream WITHOUT `.filter(b->b.hasData())`.
                boolean pruningDisabled = generatorModel.isPruningDisabled(
                        pojo.getCanonicalName().withDots(), prop.getRuneName());
                if (isList(prop)) {
                    body.append("\t\t\t").append(fld).append(" = ")
                        .append(fld).append(".stream().filter(b->b!=null).<")
                        .append(builderSingleType(prop)).append(">map(b->b.prune())")
                        .append(pruningDisabled ? "" : ".filter(b->b.hasData())")
                        .append(".collect(").append(T_COLLECTORS).append(".toList());\n");
                } else if (pruningDisabled) {
                    body.append("\t\t\tif (").append(fld)
                        .append("!=null) ").append(fld).append(".prune();\n");
                } else {
                    body.append("\t\t\tif (").append(fld)
                        .append("!=null && !").append(fld)
                        .append(".prune().hasData()) ").append(fld)
                        .append(" = null;\n");
                }
            }
        }
        body.append("\t\t\treturn this;\n");
        body.append("\t\t}\n");
        body.append("\t\t\n");

        body.append("\t\t@Override\n");
        body.append("\t\tpublic boolean hasData() {\n");
        if (builderExtended) {
            body.append("\t\t\tif (super.hasData()) return true;\n");
        }
        for (JavaPojoProperty prop : builderProps) {
            if ("meta".equals(prop.getName())) continue;
            String getterName = prop.getOperationName(JavaPojoPropertyOperationType.GET);
            // facet isoPruneConfig (PR #331): a config-disabled pair renders the
            // presence-only hasData arm — upstream IShouldPrune.Default.mayBeEmpty
            // returns false iff isPruningDisabledInConfig, so the hasData template's
            // ELSE arm fires: `if (getX()!=null) return true;` (non-list) /
            // `if (getX()!=null && !getX().isEmpty()) return true;` (list).
            boolean hasDataPruningDisabled = generatorModel.isPruningDisabled(
                    pojo.getCanonicalName().withDots(), prop.getRuneName());
            if (isList(prop)) {
                // v3.3 seat 9 (PR #645 commit 8): ONE declaration of the meta-value-kind law (LAW 69) - the seam
                // this site delegates to is the one IRPropertyReconciler reconciles the IR's fact against.
                boolean isListMetaWrappingBasic = isModelObj(prop)
                        && metaValueIsRosettaModelObject(prop, typeUtil).map(v -> !v).orElse(false);
                if (isModelObj(prop) && !isListMetaWrappingBasic && !hasDataPruningDisabled) {
                    body.append("\t\t\tif (").append(getterName)
                        .append("()!=null && ").append(getterName)
                        .append("().stream().filter(").append(T_OBJECTS).append("::nonNull).anyMatch(a->a.hasData())) return true;\n");
                } else {
                    body.append("\t\t\tif (").append(getterName)
                        .append("()!=null && !").append(getterName)
                        .append("().isEmpty()) return true;\n");
                }
            } else if (isModelObj(prop)) {
                // Meta wrapping a non-model-object value: use !=null only
                // (upstream: IShouldPrune.mayBeEmpty checks if underlying value is model object)
                // Regular model objects and meta wrapping model objects: use .hasData()
                // v3.3 seat 9 (PR #645 commit 8): the same ONE declaration of the meta-value-kind law.
                boolean isMetaWrappingBasic =
                        metaValueIsRosettaModelObject(prop, typeUtil).map(v -> !v).orElse(false);
                if (isMetaWrappingBasic || hasDataPruningDisabled) {
                    body.append("\t\t\tif (").append(getterName)
                        .append("()!=null) return true;\n");
                } else {
                    body.append("\t\t\tif (").append(getterName)
                        .append("()!=null && ").append(getterName)
                        .append("().hasData()) return true;\n");
                }
            } else {
                body.append("\t\t\tif (").append(getterName)
                    .append("()!=null) return true;\n");
            }
        }
        body.append("\t\t\treturn false;\n");
        body.append("\t\t}\n");
        body.append("\t\n");

        body.append("\t\t@SuppressWarnings(\"unchecked\")\n");
        body.append("\t\t@Override\n");
        body.append("\t\tpublic ").append(typeName).append(".").append(typeName)
            .append("Builder merge(").append(T_ROSETTA_MODEL_OBJECT_BUILDER).append(" other, ")
            .append(T_BUILDER_MERGER).append(" merger) {\n");
        if (builderExtended) {
            body.append("\t\t\tsuper.merge(other, merger);\n");
        }
        body.append("\t\t\t").append(typeName).append(".").append(typeName)
            .append("Builder o = (").append(typeName).append(".").append(typeName)
            .append("Builder) other;\n");
        body.append("\t\t\t\n");
        for (JavaPojoProperty prop : builderProps) {
            if (isModelObj(prop)) {
                String getterName = prop.getOperationName(JavaPojoPropertyOperationType.GET);
                if (isList(prop)) {
                    String getOrCreateName = prop.getOperationName(JavaPojoPropertyOperationType.GET_OR_CREATE);
                    body.append("\t\t\tmerger.mergeRosetta(").append(getterName)
                        .append("(), o.").append(getterName).append("(), this::")
                        .append(getOrCreateName).append(");\n");
                } else {
                    // facet pojoOverrideNaming (PR #323): the SETTER compatibility name —
                    // upstream merge uses getOperationName(SET) in BOTH the model and the
                    // basic branch (the basic branch below already did).
                    String setterName = prop.getOperationName(JavaPojoPropertyOperationType.SET);
                    body.append("\t\t\tmerger.mergeRosetta(").append(getterName)
                        .append("(), o.").append(getterName).append("(), this::")
                        .append(setterName).append(");\n");
                }
            }
        }
        body.append("\t\t\t\n");
        for (JavaPojoProperty prop : builderProps) {
            if (!isModelObj(prop)) {
                String getterName = prop.getOperationName(JavaPojoPropertyOperationType.GET);
                if (isList(prop)) {
                    String addName = prop.getOperationName(JavaPojoPropertyOperationType.ADD);
                    body.append("\t\t\tmerger.mergeBasic(").append(getterName)
                        .append("(), o.").append(getterName)
                        .append("(), (").append(T_CONSUMER).append("<").append(itemTypeName(prop))
                        .append(">) this::").append(addName).append(");\n");
                } else {
                    String setterName = prop.getOperationName(JavaPojoPropertyOperationType.SET);
                    body.append("\t\t\tmerger.mergeBasic(").append(getterName)
                        .append("(), o.").append(getterName).append("(), this::")
                        .append(setterName).append(");\n");
                }
            }
        }
        body.append("\t\t\treturn this;\n");
        body.append("\t\t}\n");
        body.append("\t\n");

        body.append(reindent(boilerplate.builderBoilerPlate(pojo, builderExtended, builderProps), "\t"));

        body.append("\t}\n");
    }

    // -- Setter generation helpers ------------------------------------------------

    private void generateSingleSetter(StringBuilder body, JavaPojoProperty prop,
                                       String typeName, String builderRetType) {
        String setterName = prop.getOperationName(JavaPojoPropertyOperationType.SET);
        String itemType = itemTypeName(prop);
        String fld = fieldName(prop);

        body.append("\t\t@").append(T_ROSETTA_ATTRIBUTE).append("(\"").append(prop.getName()).append("\")\n");
        body.append("\t\t@").append(T_ACCESSOR).append("(").append(T_ACCESSOR_TYPE).append(".SETTER)\n");
        if (prop.isRequired()) body.append("\t\t@").append(T_REQUIRED).append("\n");
        body.append("\t\t@").append(T_RUNE_ATTRIBUTE).append("(\"").append(prop.getName()).append("\")\n");
        if (isMetaProperty(prop)) body.append("\t\t@").append(T_RUNE_META_TYPE).append("\n");
        if (hasScopedReference(prop)) body.append("\t\t@").append(T_RUNE_SCOPED_REF).append("\n");
        if (hasScopedKey(prop)) body.append("\t\t@").append(T_RUNE_SCOPED_KEY).append("\n");
        body.append("\t\t@Override\n");
        body.append("\t\tpublic ").append(builderRetType).append(" ")
            .append(setterName).append("(").append(itemType).append(" _")
            .append(fld).append(") {\n");
        if (isModelObj(prop)) {
            body.append("\t\t\tthis.").append(fld).append(" = _")
                .append(fld).append(" == null ? null : _")
                .append(fld).append(".toBuilder();\n");
        } else {
            body.append("\t\t\tthis.").append(fld).append(" = _")
                .append(fld).append(" == null ? null : _")
                .append(fld).append(";\n");
        }
        body.append("\t\t\treturn this;\n");
        body.append("\t\t}\n");
        body.append("\t\t\n");
    }

    private void generateListSetters(StringBuilder body, JavaPojoProperty prop,
                                      String typeName, String builderRetType,
                                      Set<String> siblingFieldNames) {
        String addName = prop.getOperationName(JavaPojoPropertyOperationType.ADD);
        String setterName = prop.getOperationName(JavaPojoPropertyOperationType.SET);
        String itemType = itemTypeName(prop);
        boolean isModel = isModelObj(prop);
        String toBuilderSuffix = isModel ? ".toBuilder()" : "";
        // facet listAddSetImplFieldName (PR #386): the add/set IMPL bodies derive
        // their param names and this.<field> refs from fieldName(prop) — the field
        // identifier SOT (decap + keyword escape) — not the raw attribute name.
        // Byte-identical for every lowercase attribute; the corpus carriers are the
        // drr AnnaDsb upper-initial LIST attributes (UnderlyingRecord /
        // ReturnUnderlierID), where golden writes _underlyingRecord /
        // this.underlyingRecord / underlyingRecords. The builder-INTERFACE add/set
        // declarations keep the RAW name (golden: addUnderlyingRecord(... UnderlyingRecord)).
        String fld = fieldName(prop);
        // facet fpmlListParamEscape (PR #331): the add(List)/set(List) overload param
        // is the pluralized property name; when that string names a SIBLING property
        // (a different builder field it would shadow), golden escapes it with '_'
        // (upstream JavaClassScope.createUniqueIdentifier). Sole corpus carrier:
        // fpml CommodityMarketDisruption — marketDisruptionEvent(list)/-Events(enum)
        // + disruptionFallback(list)/-Fallbacks(enum). The param's OWN field
        // (fld) never equals the plural, so only genuine sibling
        // collisions escape.
        // v3.2 seat 3, round 1 (oracle group alias-conditions-reserved): the plural is the RAW
        // decapitalised name + "s", keyword-escaped as a WHOLE - for a keyword attribute `new` the
        // golden reads `news` (not a keyword), never `_news` (the escaped FIELD + "s"); identical to
        // the former `fld + "s"` for every name whose PLURAL is not itself a Java keyword — for
        // `extend` / `implement` / `clas` / `thi` the new form escapes (`_extends`) where the old
        // emitted the keyword bare (round-2 cq SF-5); no corpus carrier (censused), the
        // alias-conditions-reserved group the only witness of the keyword branch.
        String pluralParam = JavaNamingUtil.escapeJavaKeyword(JavaNamingUtil.toFirstLower(prop.getName()) + "s");
        if (siblingFieldNames.contains(pluralParam)) {
            pluralParam = "_" + pluralParam;
        }

        body.append("\t\t@").append(T_ROSETTA_ATTRIBUTE).append("(\"").append(prop.getName()).append("\")\n");
        body.append("\t\t@").append(T_ACCESSOR).append("(").append(T_ACCESSOR_TYPE).append(".ADDER)\n");
        if (prop.isRequired()) body.append("\t\t@").append(T_REQUIRED).append("\n");
        body.append("\t\t@").append(T_MULTI).append("\n");
        body.append("\t\t@").append(T_RUNE_ATTRIBUTE).append("(\"").append(prop.getName()).append("\")\n");
        if (hasScopedReference(prop)) body.append("\t\t@").append(T_RUNE_SCOPED_REF).append("\n");
        if (hasScopedKey(prop)) body.append("\t\t@").append(T_RUNE_SCOPED_KEY).append("\n");
        body.append("\t\t@Override\n");
        body.append("\t\tpublic ").append(builderRetType).append(" ")
            .append(addName).append("(").append(itemType).append(" _")
            .append(fld).append(") {\n");
        body.append("\t\t\tif (_").append(fld).append(" != null) {\n");
        body.append("\t\t\t\tthis.").append(fld).append(".add(_")
            .append(fld).append(toBuilderSuffix).append(");\n");
        body.append("\t\t\t}\n");
        body.append("\t\t\treturn this;\n");
        body.append("\t\t}\n");
        body.append("\t\t\n");

        body.append("\t\t@Override\n");
        body.append("\t\tpublic ").append(builderRetType).append(" ")
            .append(addName).append("(").append(itemType).append(" _")
            .append(fld).append(", int idx) {\n");
        body.append("\t\t\tgetIndex(this.").append(fld)
            .append(", idx, () -> _").append(fld)
            .append(toBuilderSuffix).append(");\n");
        body.append("\t\t\treturn this;\n");
        body.append("\t\t}\n");
        body.append("\t\t\n");

        // List-meta convenience setters: addValue(value), addValue(value, idx) — BEFORE add(List)
        if (prop.getMetaValueType() != null) {
            generateListMetaValueSettersSingle(body, prop, typeName);
        }

        String listParam = isModel
                ? T_LIST + "<? extends " + itemType + ">"
                : T_LIST + "<" + itemType + ">";
        body.append("\t\t@Override\n");
        body.append("\t\tpublic ").append(builderRetType).append(" ")
            .append(addName).append("(").append(listParam)
            .append(" ").append(pluralParam).append(") {\n");
        body.append("\t\t\tif (").append(pluralParam).append(" != null) {\n");
        body.append("\t\t\t\tfor (final ").append(itemType).append(" toAdd : ")
            .append(pluralParam).append(") {\n");
        body.append("\t\t\t\t\tthis.").append(fld).append(".add(toAdd")
            .append(toBuilderSuffix).append(");\n");
        body.append("\t\t\t\t}\n");
        body.append("\t\t\t}\n");
        body.append("\t\t\treturn this;\n");
        body.append("\t\t}\n");
        body.append("\t\t\n");

        body.append("\t\t@").append(T_ROSETTA_ATTRIBUTE).append("(\"").append(prop.getName()).append("\")\n");
        body.append("\t\t@").append(T_ACCESSOR).append("(").append(T_ACCESSOR_TYPE).append(".SETTER)\n");
        if (prop.isRequired()) body.append("\t\t@").append(T_REQUIRED).append("\n");
        body.append("\t\t@").append(T_MULTI).append("\n");
        body.append("\t\t@").append(T_RUNE_ATTRIBUTE).append("(\"").append(prop.getName()).append("\")\n");
        if (hasScopedReference(prop)) body.append("\t\t@").append(T_RUNE_SCOPED_REF).append("\n");
        if (hasScopedKey(prop)) body.append("\t\t@").append(T_RUNE_SCOPED_KEY).append("\n");
        body.append("\t\t@Override\n");
        body.append("\t\tpublic ").append(builderRetType).append(" ")
            .append(setterName).append("(").append(listParam)
            .append(" ").append(pluralParam).append(") {\n");
        body.append("\t\t\tif (").append(pluralParam).append(" == null) {\n");
        body.append("\t\t\t\tthis.").append(fld).append(" = new ").append(T_ARRAY_LIST).append("<>();\n");
        body.append("\t\t\t} else {\n");
        body.append("\t\t\t\tthis.").append(fld).append(" = ")
            .append(pluralParam).append(".stream()\n");
        if (isModel) {
            body.append("\t\t\t\t\t.map(_a->_a.toBuilder())\n");
        }
        // v3.2 seat 11 (D50): ONE literal stays - `new ArrayList<>()` inside toCollection is LITERAL text in the
        // released plugin's template (never a scoped type write), so a model type named ArrayList that wins
        // the simple name leaves it bare - non-compiling upstream Java (oracle group type-named-util:
        // ArrayList.java writes `new java.util.ArrayList<>()` canonical at its other three sites and
        // `new ArrayList<>()` here; UtilRefs.java the same beside a canonical Collectors)
        body.append("\t\t\t\t\t.collect(").append(T_COLLECTORS).append(".toCollection(()->new ArrayList<>()));\n");
        body.append("\t\t\t}\n");
        body.append("\t\t\treturn this;\n");
        body.append("\t\t}\n");
        body.append("\t\t\n");

        // List-meta bulk convenience setters: addValue(List), setXxxValue(List) — AFTER set(List)
        if (prop.getMetaValueType() != null) {
            generateListMetaValueSettersBulk(body, prop, typeName);
        }
    }

    /** Single-item meta setters: addValue(value), addValue(value, idx) */
    private void generateListMetaValueSettersSingle(StringBuilder body, JavaPojoProperty prop,
                                                     String typeName) {
        String builderRetType = typeName + "." + typeName + "Builder";
        String valueTypeName = metaValueTypeName(prop);
        String addValueName = prop.getOperationName(JavaPojoPropertyOperationType.ADD_VALUE);
        String getOrCreateName = prop.getOperationName(JavaPojoPropertyOperationType.GET_OR_CREATE);
        // v3.3 seat 9 (PR #645 commit 8): the same ONE declaration of the meta-value-kind law. The caller guards on
        // `getMetaValueType() != null` (:1540), so the empty arm is unreachable - it is stated rather than
        // defaulted, because a silent `false` here would write a `.toBuilder()` suffix the golden does not have.
        boolean isModelValue = metaValueIsRosettaModelObject(prop, typeUtil).orElseThrow(
                () -> new IllegalStateException("the list meta-value setter arm was reached for '" + prop.getName()
                        + "', which carries no meta value type"));
        String toBuilderSuffix = isModelValue ? ".toBuilder()" : "";

        // addValue(value)
        body.append("\t\t@Override\n");
        body.append("\t\tpublic ").append(builderRetType).append(" ")
            .append(addValueName).append("(").append(valueTypeName).append(" _")
            .append(prop.getName()).append(") {\n");
        body.append("\t\t\tthis.").append(getOrCreateName)
            .append("(-1).setValue(_").append(prop.getName()).append(toBuilderSuffix).append(");\n");
        body.append("\t\t\treturn this;\n");
        body.append("\t\t}\n");
        body.append("\t\t\n");

        // addValue(value, idx)
        body.append("\t\t@Override\n");
        body.append("\t\tpublic ").append(builderRetType).append(" ")
            .append(addValueName).append("(").append(valueTypeName).append(" _")
            .append(prop.getName()).append(", int idx) {\n");
        body.append("\t\t\tthis.").append(getOrCreateName)
            .append("(idx).setValue(_").append(prop.getName()).append(toBuilderSuffix).append(");\n");
        body.append("\t\t\treturn this;\n");
        body.append("\t\t}\n");
        body.append("\t\t\n");
    }

    /** Bulk meta setters: addValue(List), setXxxValue(List) */
    private void generateListMetaValueSettersBulk(StringBuilder body, JavaPojoProperty prop,
                                                    String typeName) {
        String builderRetType = typeName + "." + typeName + "Builder";
        String valueTypeName = metaValueTypeName(prop);
        String addValueName = prop.getOperationName(JavaPojoPropertyOperationType.ADD_VALUE);
        String setValueName = prop.getOperationName(JavaPojoPropertyOperationType.SET_VALUE);

        // addValue(List)
        body.append("\t\t@Override\n");
        body.append("\t\tpublic ").append(builderRetType).append(" ")
            .append(addValueName).append("(").append(T_LIST).append("<? extends ").append(valueTypeName)
            .append("> ").append(prop.getName()).append("s) {\n");
        body.append("\t\t\tif (").append(prop.getName()).append("s != null) {\n");
        body.append("\t\t\t\tfor (final ").append(valueTypeName).append(" toAdd : ")
            .append(prop.getName()).append("s) {\n");
        body.append("\t\t\t\t\tthis.").append(addValueName).append("(toAdd);\n");
        body.append("\t\t\t\t}\n");
        body.append("\t\t\t}\n");
        body.append("\t\t\treturn this;\n");
        body.append("\t\t}\n");
        body.append("\t\t\n");

        // setXxxValue(List)
        body.append("\t\t@Override\n");
        body.append("\t\tpublic ").append(builderRetType).append(" ")
            .append(setValueName).append("(").append(T_LIST).append("<? extends ").append(valueTypeName)
            .append("> ").append(prop.getName()).append("s) {\n");
        body.append("\t\t\tthis.").append(prop.getName()).append(".clear();\n");
        body.append("\t\t\tif (").append(prop.getName()).append("s != null) {\n");
        body.append("\t\t\t\t").append(prop.getName()).append("s.forEach(this::")
            .append(addValueName).append(");\n");
        body.append("\t\t\t}\n");
        body.append("\t\t\treturn this;\n");
        body.append("\t\t}\n");
        body.append("\t\t\n");
    }

    private void generateMetaValueSetter(StringBuilder body, JavaPojoProperty prop,
                                           String typeName) {
        String builderRetType = typeName + "." + typeName + "Builder";
        String valueTypeName = metaValueTypeName(prop);
        String setValueName = prop.getOperationName(JavaPojoPropertyOperationType.SET_VALUE);
        String getOrCreateName = prop.getOperationName(JavaPojoPropertyOperationType.GET_OR_CREATE);
        // P2.1.3c β1 meta extension: impl-side param uses camelCase fieldName.
        // For RDataType attributes (camelCase prop.getName()) fieldName is identity.
        // For RChoice options (PascalCase prop.getName(), e.g. "InterestRateIndex")
        // fieldName lowercases the first char so the param matches the impl setter
        // (`_interestRateIndex`) per the upstream golden's impl-side naming convention.
        String paramName = "_" + fieldName(prop);
        body.append("\t\t@Override\n");
        body.append("\t\tpublic ").append(builderRetType).append(" ")
            .append(setValueName).append("(").append(valueTypeName).append(" ")
            .append(paramName).append(") {\n");
        // Single meta setter: pass value directly (no .toBuilder())
        // The meta wrapper's setValue() handles conversion internally
        // Note: LIST meta setters DO use .toBuilder() — different pattern
        body.append("\t\t\tthis.").append(getOrCreateName)
            .append("().setValue(").append(paramName).append(");\n");
        body.append("\t\t\treturn this;\n");
        body.append("\t\t}\n");
        body.append("\t\t\n");
    }

    /**
     * Field name for a property. For choice options (name starts uppercase), lowercases first char.
     * For regular properties (name starts lowercase), returns as-is.
     */
    static String fieldName(JavaPojoProperty prop) {
        // PR #231: escape a reserved-word attribute name (e.g. `new`/`short`/`long`/
        // `return`) to `_new` so it is a valid Java field identifier, mirroring the
        // upstream scope escape (SourceVersion.isName → JavaClassScope.escapeName
        // "_"+name). No-op for ordinary names. The single source of truth for the
        // FIELD identifier: every fieldName(prop) consumer (field decl, this.<f>,
        // return <f>, getOrCreate/prune refs) flips together, and the impl setter
        // param `"_" + fieldName(prop)` becomes `__new` (the field-vs-param double
        // escape) for free.
        //
        // SCOPE (PR #231): the keyword carriers in the 9.83.0 corpus are all
        // SINGLE-cardinality attributes (iso TradeReport*Choice `new`, fpml
        // CreditLimitUtilizationPosition `short`/`long`, fpml ReturnLeg `return`).
        // PR #386 (facet listAddSetImplFieldName) cashed the follow-up this note
        // promised: the list add/set IMPL bodies now route their param names and
        // this.<field> refs through fieldName(prop) — the anticipated carriers
        // turned out to be UPPER-INITIAL list attributes (drr AnnaDsb
        // UnderlyingRecord / ReturnUnderlierID), not keywords. Remaining raw-name
        // seats with ZERO corpus carriers (upper-initial or keyword): the list-META
        // value setters (generateListMetaValueSettersSingle/Bulk `_`-params) and
        // the builder-INTERFACE list declarations (which golden itself keeps RAW) —
        // the next anticipated seat if such a model ever appears.
        return JavaNamingUtil.escapeJavaKeyword(JavaNamingUtil.toFirstLower(prop.getName()));
    }

    /**
     * Parameter name for the builder INTERFACE setter declaration.
     *
     * <p>Upstream rune-dsl prefixes the setter parameter with {@code _} to avoid the
     * parameter identifier colliding with a same-named type in scope: the prefix is
     * applied <em>iff the property name equals the simple name of its value type</em>.
     * This covers every observed golden shape:
     * <ul>
     *   <li>{@code choice} options — the option attribute is named after its type
     *       ({@code setCash(Cash _Cash)} on the {@code Asset} choice; name {@code Cash}
     *       == type {@code Cash}).</li>
     *   <li>Choice options inherited by a {@code type} that extends a choice —
     *       {@code setAsset(Asset _Asset)} on {@code BasketConstituent extends Observable}
     *       (name {@code Asset} == type {@code Asset}).</li>
     *   <li>Meta-wrapped options — the collision is against the <em>unwrapped</em> value
     *       type, not the wrapper: {@code setInterestRateIndex(FieldWithMetaInterestRateIndex
     *       _InterestRateIndex)} (name {@code InterestRateIndex} == meta-value type
     *       {@code InterestRateIndex}).</li>
     * </ul>
     *
     * <p>Conversely, a {@code type} attribute whose name does NOT equal its type stays
     * bare — e.g. the ISO-UPI {@code AnnaDsbAGRI} data type's {@code setSEAF(AnnaDsbEmpty
     * SEAF)} (name {@code SEAF} != type {@code AnnaDsbEmpty}). The prior heuristic keyed
     * the prefix on {@code Character.isUpperCase(name)} as a proxy for "is a choice
     * option", which holds for CDM (choice options are PascalCase type references; data
     * attributes are camelCase) but mis-fires on the {@code AnnaDsb*} data types whose
     * attributes legitimately start uppercase yet do not collide with their type. Keying
     * on the actual name-vs-value-type collision matches the golden in all 3947 setter
     * declarations across the corpus.
     */
    private String interfaceParamName(JavaPojoProperty prop) {
        String n = prop.getName();
        // v3.2 seat 3, round 2 (oracle group alias-conditions-filescope, WrittenSingle) + round 3 (cq
        // SF-4): ONE predicate for both interface arms — a name the file writes as ANY type (the
        // item and meta-value types of every property, an import, Object, String) escapes,
        // upstream's file scope registering every type on write. The two own-type branches this
        // arm carried (the property's own item type, its own meta-value type — the 3947-setter
        // match above) are the set's own-property subset and were deleted into it, byte-identical.
        if (fileWrittenSimpleNames.contains(n)) {
            return "_" + n;
        }
        // PR #231: a reserved-word attribute name (e.g. `new`) escapes to `_new` on
        // the builder-INTERFACE setter param too (golden `setNew(X _new);`). The two
        // collision branches above already yield a valid `_`-prefixed identifier, so
        // only the bare-return path needs the keyword escape. The IMPL setter param
        // is `"_" + fieldName(prop)` → `__new`, one underscore deeper, per the
        // field-vs-param scope de-duplication.
        return JavaNamingUtil.escapeJavaKeyword(n);
    }

    /**
     * P2.1.3c T5b: whether the pojo (incl. inherited / choice-option properties)
     * has a property whose lower-cased name is {@code "index"}. This shadows the
     * default {@code int index} parameter on {@code getOrCreate*} list methods, so
     * the upstream scope-deduplication rule re-names the param to {@code _index}.
     *
     * <p>See ModelObjectGenerator L869 + the inline rule narrative for the two
     * empirically observed conflict sub-cases (choice parent with {@code Index}
     * option / RDataType parent with {@code index <T> (0..1)} field).
     */
    static boolean hasIndexPropertyInScope(RJavaPojoInterface pojo) {
        return pojo.getAllProperties().stream()
                .anyMatch(p -> "index".equals(JavaNamingUtil.toFirstLower(p.getName())));
    }

    /**
     * True when the builder class scope claims the identifier {@code result} — a
     * property (own or inherited, matching the upstream class scope that holds ALL
     * fields) whose lower-cased name is {@code "result"}. The single-cardinality
     * {@code getOrCreate*} local then escapes to {@code _result}, the same upstream
     * scope-deduplication rule as {@link #hasIndexPropertyInScope} / {@code _index}.
     * Hold-out witness (PR #410): the name-escaping fixture's {@code result} attribute.
     */
    private static boolean hasResultPropertyInScope(RJavaPojoInterface pojo) {
        return pojo.getAllProperties().stream()
                .anyMatch(p -> "result".equals(JavaNamingUtil.toFirstLower(p.getName())));
    }

    // -- Import + type helpers ----------------------------------------------------

    /**
     * v3.2 seat 3, round 2 (oracle group alias-conditions-filescope): the simple names THIS
     * file writes as types — upstream's file scope registers each on write, so a builder-interface
     * parameter named after one escapes with a {@code _} prefix ({@code addArrayList(Integer
     * _ArrayList)}, {@code _Object}, {@code _String}, {@code _List}, {@code _Multi}, {@code
     * _Processor}, {@code _Consumer}, {@code _Collectors}, {@code _ImmutableList}), while a name
     * the file never writes as a type stays bare ({@code Objects} when no equals() needs it;
     * {@code Override}, which upstream's template writes as text). The set is the import set
     * (complete before the body renders) plus the java.lang types every POJO writes ({@code
     * Object} in equals, {@code String} in toString) and the item / meta-value types of its
     * properties as WRITTEN (round-3 cq SF-4: every property's, same-package ones included — a
     * java.lang-colliding model type is written fully qualified, so its simple name is not the
     * file's). Assigned per POJO in buildModel and read by the two interface-parameter arms;
     * the generator renders one class at a time. KNOWN RESIDUE, one half unmeasured (round-3 cq
     * N-8, round-5 spec N-2 — measured at the corpus, unmeasured only at upstream's file scope):
     * every POJO also writes {@code Class} ({@code Class<? extends X> getType()}) — java.lang,
     * never imported, never a property type — so it is NOT in the set and an attribute named
     * {@code Class} renders {@code setClass(X Class)} on a guess; banked (seat plan § 6). And the
     * SIBLING half of the round-3 widening — an attribute named after a sibling property's
     * same-package type, {@code setBar(String _Bar)} — is reasoned from the same law (the file
     * writes that type in the sibling's field, getter and setter) and carrier-free at the corpus
     * (the 26-cell matrix unmoved); lane AA switches the whole set off, so it cannot separate this
     * half from the own-type half the bar's older groups witness — lane AA red on
     * alias-conditions-filescope (3 mismatching), pojo (1) and func-meta-deep-path-multi (1)
     * (round-4 cq N-4, round-5 cq N-3).
     */
    private Set<String> fileWrittenSimpleNames = Set.of();

    private Set<String> fileWrittenSimpleNames(ImportCollector imports, Collection<JavaPojoProperty> props) {
        Set<String> names = new HashSet<>();
        for (String imported : imports.getImports()) {
            names.add(DottedPath.splitOnDots(imported).last());
        }
        names.add("Object");
        names.add("String");
        for (JavaPojoProperty p : props) {
            // round-3 cq SF-4: the item type as the file WRITES it (a java.lang-colliding model
            // type is written fully qualified — not a simple name the file claims) and the
            // meta-value type, for EVERY property — the one set both interface arms read
            // v3.2 seat 11 (D50): read from the TYPE, never from the rendered text (now a sentinel) - the
            // simple name unless the #306 java.lang law writes the model type canonical
            JavaType itemJavaType = typeUtil.getItemType(p.getType());
            if (!(itemJavaType instanceof com.rosetta.util.types.JavaClass<?> ijc)
                    || ijc.getCanonicalName().withDots().startsWith("java.lang.")
                    || !collidesWithJavaLang(ijc.getSimpleName())) {
                names.add(itemJavaType.getSimpleName());
            }
            JavaType meta = p.getMetaValueType();
            if (meta != null) {
                names.add(meta.getSimpleName());
            }
        }
        return names;
    }

    private void addTypeImports(ImportCollector imports, JavaPojoProperty prop) {
        var type = prop.getType();
        if (type instanceof com.rosetta.util.types.JavaClass<?> jc) {
            String fqn = jc.getCanonicalName().withDots();
            // facet javaLangAttrFqn (PR #306): a model attribute type whose simple name
            // collides with java.lang is FQN-inlined at every value site (see itemTypeName)
            // and its import is collision-blocked — skip it so the unused import drops.
            if (!fqn.startsWith("java.lang.") && !collidesWithJavaLang(jc.getSimpleName())) {
                imports.addImport(fqn);
            }
        }
        if (isList(prop)) {
            JavaType itemType = typeUtil.getItemType(prop.getType());
            if (itemType instanceof com.rosetta.util.types.JavaClass<?> ic) {
                String fqn = ic.getCanonicalName().withDots();
                if (!fqn.startsWith("java.lang.") && !collidesWithJavaLang(ic.getSimpleName())) {
                    imports.addImport(fqn);
                }
            }
        }
        // Import the underlying value type for FieldWithMeta/ReferenceWithMeta wrappers.
        // facet javaLangAttrFqn (PR #306): the collidesWithJavaLang skip is intentionally NOT
        // applied here — a META-wrapped colliding value type (e.g. a hypothetical
        // FieldWithMetaMath) has no carrier in the 9.83.0 corpus, so this path is left as-is to
        // keep the change minimal. If such a carrier ever appears, mirror the gate above.
        if (prop.getMetaValueType() != null) {
            JavaType vt = prop.getMetaValueType();
            if (vt instanceof com.rosetta.util.types.JavaClass<?> vc) {
                String fqn = vc.getCanonicalName().withDots();
                if (!fqn.startsWith("java.lang.")) {
                    imports.addImport(fqn);
                }
            }
        }
    }

    private boolean isList(JavaPojoProperty prop) {
        return typeUtil.isList(prop.getType());
    }

    private boolean isModelObj(JavaPojoProperty prop) {
        return itemIsRosettaModelObject(prop, typeUtil);   // ONE declaration of the law (LAW 69) - the static twin
    }

    /**
     * v3.3 seat 9 (PR #645 commit 5) - THE ITEM-KIND RECONCILE SEAM, and THE declaration of the predicate this
     * generator writes bytes from. {@link #isModelObj} delegates here, so the seam is not a dead twin that could
     * drift from what the POJO is actually emitted with: it IS the site, and the {@code List<? extends X>} getter
     * arm ({@link #interfaceGetterType}, {@code :1917-1925}), the {@code java.util.Objects} import gate
     * ({@code :180-184}) and the {@code java.util.function.Consumer} import gate ({@code :273-286}) all reach it.
     *
     * <p>{@code public static} for VISIBILITY ONLY: the IR route's property reconciler ({@code IRPropertyReconciler}
     * in {@code rune-ir-java}, a different package) asserts its IR-derived
     * {@code property.<name>.itemIsRosettaModelObject} fact against THIS answer, which is what makes the two halves
     * two producers (LAW 69) rather than one mirrored twice. Not one byte of the OFF route moves.
     *
     * @param prop     the property whose ITEM type is asked about ({@code getItemType} of a list, the type itself
     *                 otherwise - {@code JavaTypeUtil.isRosettaModelObject}, {@code :236-238})
     * @param typeUtil the generator's own type table; the predicate is the table's, never this class's
     */
    public static boolean itemIsRosettaModelObject(JavaPojoProperty prop, JavaTypeUtil typeUtil) {
        return typeUtil.isRosettaModelObject(prop.getType());
    }

    /**
     * v3.3 seat 9 (PR #645 commit 5) - THE ANCESTOR-CHAIN RECONCILE SEAM: the rungs of a specialized property, the
     * NEAREST ancestor first, exactly as {@link PojoCompatEmitter} walks them. {@code public static} for VISIBILITY
     * ONLY, over {@code PojoCompatEmitter.ancestorChain} - which is package-private because the emitter itself is,
     * and which {@code PojoCompatEmitter.anySpecialized} now reads, so this seam can never describe a chain the
     * compat members are not written from.
     *
     * <p>The IR route's property reconciler asserts its {@code property.<name>.parentChain.types} fact against
     * these rungs. Behaviour-neutral: the walk is the one the three compat walks and the chain's import arm
     * ({@code :288-330}) already perform over {@code JavaPojoProperty.getParentProperty()}.
     */
    public static java.util.List<JavaPojoProperty> compatAncestorChain(JavaPojoProperty prop) {
        return PojoCompatEmitter.ancestorChain(prop);
    }

    /**
     * v3.3 seat 9 (PR #645 commit 8) - THE ITEM-IS-ENUM RECONCILE SEAM, and THE declaration of the predicate the
     * {@code hashCode} boilerplate writes its enum arm from. {@code ModelObjectBoilerplate.contributeHashCode}
     * ({@code ModelObjectBoilerplate:205-206}) DELEGATES here, so the seam is not a dead twin that could drift from
     * the bytes it describes: it IS the site.
     *
     * <p>{@code public static} for VISIBILITY ONLY: the IR route's property reconciler ({@code IRPropertyReconciler}
     * in {@code rune-ir-java}, a different package) asserts its IR-derived {@code property.<name>.itemIsEnum} fact
     * against THIS answer, which is what makes the two halves two producers (LAW 69). Not one byte of the OFF route
     * moves.
     *
     * @param prop     the property whose ITEM type is asked about ({@code getItemType} of a list, the type itself
     *                 otherwise)
     * @param typeUtil the generator's own type table; the item is the table's, never this class's
     */
    public static boolean itemIsEnum(JavaPojoProperty prop, JavaTypeUtil typeUtil) {
        return typeUtil.getItemType(prop.getType())
                instanceof com.regnosys.rosetta.generator.java.types.RJavaEnum;
    }

    /**
     * v3.3 seat 9 (PR #645 commit 8) - THE META-VALUE-KIND RECONCILE SEAM, and THE declaration of the predicate the
     * {@code hasData} arms ({@code :1325-1327}, {@code :1341-1342}) and the list meta-value setter
     * ({@code :1585}) write their bytes from - all three now DELEGATE here.
     *
     * <p>It is an {@code Optional}, not a {@code boolean}, because the old generator's own fact is a NULLABLE
     * {@code getMetaValueType()}: EMPTY means the property carries no meta value type at all, which is a different
     * statement from "its value is not a model object". Every delegating site above asks exactly the question it
     * asked before - {@code getMetaValueType() != null && !isRosettaModelObject(getMetaValueType())} becomes
     * {@code metaValueIsRosettaModelObject(prop, typeUtil).map(v -> !v).orElse(false)} - so not one byte moves.
     *
     * <p>{@code public static} for VISIBILITY ONLY: {@code IRPropertyReconciler} asserts its IR-derived
     * {@code property.<name>.metaValueIsRosettaModelObject} fact against THIS answer.
     */
    public static java.util.Optional<Boolean> metaValueIsRosettaModelObject(JavaPojoProperty prop,
                                                                           JavaTypeUtil typeUtil) {
        return java.util.Optional.ofNullable(prop.getMetaValueType()).map(typeUtil::isRosettaModelObject);
    }

    private boolean hasScopedReference(JavaPojoProperty prop) {
        return prop.getAttributeMetaTypes().contains(
                com.regnosys.rosetta.generator.java.types.AttributeMetaType.SCOPED_REFERENCE);
    }

    private boolean hasScopedKey(JavaPojoProperty prop) {
        return prop.getAttributeMetaTypes().contains(
                com.regnosys.rosetta.generator.java.types.AttributeMetaType.SCOPED_KEY);
    }

    /**
     * Synthetic "meta" property for types with [metadata key] — narrowed to
     * {@code MetaFields} only.
     *
     * <p>P2.1.3c T3 cont: golden upstream does NOT emit {@code @RuneMetaType}
     * on a "meta" property of type {@code MetaAndTemplateFields} (Templatable
     * types) — only on {@code MetaFields} (GlobalKey-only types). Verified
     * via cdm/5.35.0/ContractualProduct golden (meta : MetaAndTemplateFields,
     * Templatable, no @RuneMetaType + no RuneMetaType import) vs cdm/5.35.0/
     * AdjustableDate golden (meta : MetaFields, GlobalKey, @RuneMetaType
     * emitted + import present).
     */
    private boolean isMetaProperty(JavaPojoProperty prop) {
        return "meta".equals(prop.getName())
                && itemSimpleName(prop).equals("MetaFields");
    }

    /** The attribute item type's bare simple name — used to build the nested builder
     *  type ({@code X.XBuilder}) and the builder-process {@code .class}, both of which
     *  golden leaves BARE even when {@code X} collides with java.lang (facet javaLangAttrFqn). */
    private String itemSimpleName(JavaPojoProperty prop) {
        return typeUtil.getItemType(prop.getType()).getSimpleName();
    }

    /**
     * The attribute item type as a Java type reference used at VALUE sites (getter/setter
     * param/impl field/for-loop element/{@code X.builder()}/interface-process {@code .class}).
     *
     * <p>facet javaLangAttrFqn (PR #306): a model attribute type whose SIMPLE name collides
     * with an implicitly-imported {@code java.lang} type is FQN-inlined here (golden refuses
     * to import it — the upstream {@code ImportingStringConcatenation} first-claim-wins law —
     * so every value reference is fully qualified). Carriers: iso {@code DataResponse.Error}
     * ({@code iso20022.dtcc.rds.harmonized.Error}), fpml {@code Formula.Math}
     * ({@code fpml.consolidated.shared.Math}). Collision-ONLY gate (no package check): golden
     * FQNs even a same-package colliding attribute (both carriers are same-package). The
     * builder-type sites ({@code X.XBuilder}) keep the bare simple name ({@link #itemSimpleName}).
     */
    private String itemTypeName(JavaPojoProperty prop) {
        return valueSiteTypeName(typeUtil.getItemType(prop.getType()));
    }

    /**
     * The value-site render of a bare {@link JavaType} — the {@link #itemTypeName} law
     * factored to a type-level entry so the builder-compat emitter ({@code PojoCompatEmitter},
     * PR #412) applies the identical FQN-collision rule at its coercion positions.
     */
    String valueSiteTypeName(JavaType itemType) {
        return valueSiteTypeRef(itemType);   // round 1's cq SF-1: ONE declaration of the law (LAW 69) - the static twin
    }

    /**
     * v3.2 seat 11 (D50): THE value-site law, one declaration (LAW 69) - {@link #valueSiteTypeName} delegates here
     * (round 1's cq SF-1: the two bodies had been byte-identical twins) and {@link ModelObjectBoilerplate}'s process
     * methods read it directly. FQN ONLY a MODEL type whose simple name collides with java.lang (canonical NOT in
     * java.lang) - a java.lang type itself (Integer/String/Long/Boolean/...) is implicitly imported and stays BARE;
     * every other class is a first-claim sentinel - bare or canonical by the file's text order once buildModel
     * resolves the class text (a parameterised render keeps its spelling; no POJO value site carries one).
     */
    static String valueSiteTypeRef(JavaType itemType) {
        String simple = itemType.getSimpleName();
        if (itemType instanceof com.rosetta.util.types.JavaClass<?> jc) {
            String fqn = jc.getCanonicalName().withDots();
            if (!fqn.startsWith("java.lang.") && collidesWithJavaLang(simple)) {
                return fqn;
            }
            if (ImportCollisionResolver.simpleIsLastSegment(fqn, simple)) {
                return ImportCollisionResolver.typeRefOrBare(fqn);
            }
        }
        return simple;
    }

    /**
     * v3.2 seat 11 (D50): the meta VALUE type at a write - a first-claim sentinel; the #306 java.lang
     * collision law is NOT applied here (the pre-seat path wrote the bare simple name and imported it - the
     * PR #306 note's unmirrored gate, no carrier either way).
     */
    private String metaValueTypeName(JavaPojoProperty prop) {
        JavaType vt = prop.getMetaValueType();
        String simple = vt.getSimpleName();
        if (vt instanceof com.rosetta.util.types.JavaClass<?> vc) {
            String fqn = vc.getCanonicalName().withDots();
            if (ImportCollisionResolver.simpleIsLastSegment(fqn, simple)) {
                return ImportCollisionResolver.typeRefOrBare(fqn);
            }
        }
        return simple;
    }

    String interfaceGetterType(JavaPojoProperty prop) {
        if (isList(prop)) {
            if (isModelObj(prop)) {
                return T_LIST + "<? extends " + itemTypeName(prop) + ">";
            }
            return T_LIST + "<" + itemTypeName(prop) + ">";
        }
        return itemTypeName(prop);
    }

    private String dotQualifiedBuilderType(JavaPojoProperty prop) {
        // facet javaLangAttrFqn (PR #306): the nested builder type (X.XBuilder) keeps the BARE
        // simple name even when X collides with java.lang — golden leaves these bare (only the
        // VALUE sites FQN-inline, see itemTypeName), so use itemSimpleName, not itemTypeName.
        // This is correct for the 2 SAME-package corpus carriers (Error, Math): the collision-
        // blocked import is dropped, but bare X.XBuilder resolves to the same-package model X by
        // Java same-package precedence (which outranks the implicit java.lang import).
        // LATENT GAP (PR #306 Copilot R1, no corpus carrier): a CROSS-package colliding attribute
        // would need the outer FQN-inlined here (other.pkg.X.XBuilder), since cross-package bare X
        // would bind to java.lang.X (no same-package fallback). A corpus-wide scan finds ZERO
        // cross-package java.lang-colliding model attributes, so this path is unreachable in the
        // 9.83.0 corpus. If such a carrier ever appears, gate on collision-AND-cross-package (the
        // enclosing-POJO package, like #305's superFqnInlined) and emit itemTypeName(prop) + "." +
        // itemSimpleName(prop) + "Builder" for the cross-package case — verify against the new
        // golden first; the same-package case must stay bare or it regresses the 2 carriers.
        String item = itemSimpleName(prop);
        return item + "." + item + "Builder";
    }

    String builderGetterType(JavaPojoProperty prop) {
        if (isList(prop)) {
            if (isModelObj(prop)) {
                return T_LIST + "<? extends " + dotQualifiedBuilderType(prop) + ">";
            }
            return T_LIST + "<" + itemTypeName(prop) + ">";
        }
        if (isModelObj(prop)) {
            return dotQualifiedBuilderType(prop);
        }
        return itemTypeName(prop);
    }

    private String builderFieldType(JavaPojoProperty prop) {
        if (isList(prop)) {
            if (isModelObj(prop)) {
                return T_LIST + "<" + dotQualifiedBuilderType(prop) + ">";
            }
            return T_LIST + "<" + itemTypeName(prop) + ">";
        }
        if (isModelObj(prop)) {
            return dotQualifiedBuilderType(prop);
        }
        return itemTypeName(prop);
    }

    String builderSingleType(JavaPojoProperty prop) {
        if (isModelObj(prop)) {
            return dotQualifiedBuilderType(prop);
        }
        return itemTypeName(prop);
    }
}
