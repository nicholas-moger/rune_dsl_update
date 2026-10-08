package com.regnosys.rosetta.generator.java.object;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.RRootElement;
import com.regnosys.rosetta.ast.enums.CardCheckOp;
import com.regnosys.rosetta.ast.enums.QualifiableKind;
import com.regnosys.rosetta.ast.expressions.unary.RCardinalityCheckExpr;
import com.regnosys.rosetta.ast.functions.RCondition;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.model.RQualifiableConfig;
import com.regnosys.rosetta.ast.types.RChoice;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.JavaClassGenerator;
import com.regnosys.rosetta.generator.java.template.ImportCollector;
import com.regnosys.rosetta.generator.java.template.ImportCollisionResolver;
import com.regnosys.rosetta.generator.java.template.TemplateRenderer;
import com.regnosys.rosetta.generator.java.template.model.ConditionRefModel;
import com.regnosys.rosetta.generator.java.template.model.MetaTemplateModel;
import com.regnosys.rosetta.generator.java.template.model.PojoTemplateModel;
import com.regnosys.rosetta.generator.java.template.model.QualifyRefModel;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.RGeneratedJavaClass;
import com.rosetta.model.lib.ModelSymbolId;
import com.rosetta.util.DottedPath;

/**
 * Generates the {@code RosettaMetaData} implementation ({@code <pkg>.meta.<T>Meta})
 * for every {@code Data} AND every {@code choice} type. The 9.83 golden shape is
 * SEVEN methods (drift #2, the #403 catalogue — the vendored generator emits five):
 * {@code dataRules(ValidatorFactory)} / {@code getQualifyFunctions} /
 * {@code validator(factory)} / {@code typeFormatValidator(factory)} /
 * {@code @Deprecated validator()} / {@code @Deprecated typeFormatValidator()} /
 * {@code onlyExistsValidator()}.
 *
 * <p>Byte laws (golden-verified at #403/#407):
 * <ul>
 *   <li><b>dataRules — ordering law 1:</b> conditions of the type and ALL its
 *       supertypes, ROOT-supertype-first, declaration order within each type, each
 *       ref typed to its DECLARING type ({@code factory.<EventInstruction>create(…)}
 *       — golden {@code BusinessEventMeta}); the supertype chain crosses a
 *       data-extends-choice boundary ({@code SpecificAssetMeta} inherits
 *       {@code factory.<Asset>create(AssetChoice.class)}); a {@code choice}
 *       contributes exactly its implicit {@code <Name>Choice} condition.</li>
 *   <li><b>Unnamed-condition naming:</b> {@code <TypeName>{OneOf|Choice|DataRule}<index>}
 *       where the kind comes from the expression ROOT ({@code one-of} /
 *       {@code choice} cardinality-check, else DataRule) and the index counts ALL
 *       NAMED conditions of the type (the count-all-named law — golden
 *       {@code PhysicalSettlementPeriodOneOf2}: 2 named + 1 preceding unnamed
 *       one-of → index 2). ⚠ CORPUS-FITTED like upstream's own rule (the #403
 *       latent limitation): two unnamed conditions on ONE type would mint
 *       duplicate class names — no 9.83 type has ≥2 (Seat-1 #407 re-censused all
 *       cells: max unnamed-per-type = 1), and no golden exists to pin a
 *       different behaviour against; re-check at any corpus/version bump.</li>
 *   <li><b>getQualifyFunctions — ordering law 2:</b> empty
 *       ({@code Collections.emptyList()}) for every type except the qualifiable
 *       roots ({@code isEvent root} / {@code isProduct root} configurations —
 *       4 non-empty files corpus-wide); a root lists every {@code [qualification]}
 *       function in the whole resource set whose FIRST input type is the root, in
 *       the full-path-sorted file-walk × in-file declaration order (golden
 *       {@code BusinessEventMeta}'s 37 entries), each typed to the ROOT.</li>
 *   <li><b>Imports:</b> single alphabetical block; {@code Arrays} always (the empty
 *       dataRules shape is {@code Arrays.asList(\n\t\t);}, never elided);
 *       {@code Collections} only when the qualify list is empty; each condition
 *       ref imports its class AND its declaring type's data class; version javadoc
 *       between two blank lines after the imports.</li>
 *   <li><b>Blank-line bytes:</b> the six inter-method separators are
 *       tab/tab/empty/empty/empty/tab (the ST4 {@code <\t>} escape law — see
 *       {@code java-meta.stg}).</li>
 *   <li><b>The #306 collision law:</b> a java.lang-colliding subject renders FQN at
 *       every TYPE position including {@code @RosettaMeta(model=…)} with no
 *       data-class import (golden iso {@code ErrorMeta}); the same law applies to
 *       each condition ref's declaring type.</li>
 * </ul>
 *
 * <p>Output must match upstream Xtend ModelMetaGenerator at 9.83.0 (D11).
 * Uses ST4 template {@code java-meta.stg} via {@link TemplateRenderer}.
 *
 * <p>Migrated from StringBuilder to ST4 as part of M8 (D15); rebuilt to the 9.83
 * seven-method golden shape + the qualify wing + wired into the D11 gate at
 * PR #407 (coverage burn-down wave B).
 */
public class ModelMetaGenerator extends JavaClassGenerator<RRootElement, RGeneratedJavaClass<?>> {

    private static final String TEMPLATE_GROUP = "templates/java-meta.stg";

    private final GeneratorModel generatorModel;
    private final JavaTypeTranslator typeTranslator;
    private final TemplateRenderer renderer;
    /** The two first-wins roots, computed ONCE per generator (round 1, cq SF-8): the workspace is immutable within a run. */
    private RDataType productRoot;
    private RDataType eventRoot;
    private boolean rootsComputed;

    public ModelMetaGenerator(GeneratorModel generatorModel, JavaTypeTranslator typeTranslator) {
        this.generatorModel = generatorModel;
        this.typeTranslator = typeTranslator;
        this.renderer = new TemplateRenderer();
        this.renderer.loadGroupFromClasspath(TEMPLATE_GROUP);
    }

    @Override
    protected Stream<? extends RRootElement> streamObjects(RModel model) {
        return model.rootElements().stream()
                .filter(e -> e instanceof RDataType || e instanceof RChoice)
                .map(e -> (RRootElement) e);
    }

    @Override
    protected RGeneratedJavaClass<?> createTypeRepresentation(RRootElement element) {
        ModelSymbolId typeId = new ModelSymbolId(generatorModel.namespace(element),
                elementName(element));
        return typeTranslator.toJavaMetaDataClass(typeId);
    }

    @Override
    protected String generate(RRootElement element, RGeneratedJavaClass<?> metaClass, String version) {
        MetaTemplateModel model = buildModel(element, metaClass, version);
        // v3.2 seat 11 (D50 - the file-scope first-claim law): the class text first, with every TYPE
        // position a sentinel; the sentinels resolved in text order from the ONE seed upstream's
        // JavaClassScope registers before writing a byte (the meta class itself); the losers'
        // imports dropped; then the file wrapper. A meta with no collision renders the pre-seat bytes.
        String classText = renderer.render(TEMPLATE_GROUP, "metaBody", "m", model, "t", META_TOKENS);
        ImportCollisionResolver.ClassResolution resolved = ImportCollisionResolver.resolveClass(
                classText, model.getPackageName() + "." + model.getMetaClassName(), model.getImports());
        return renderer.render(TEMPLATE_GROUP, "metaFile", "m",
                new PojoTemplateModel(model.getPackageName(), resolved.imports(), List.of(), resolved.classText()));
    }

    private static String elementName(RRootElement element) {
        if (element instanceof RDataType dataType) {
            return dataType.name();
        }
        return ((RChoice) element).name();
    }

    /**
     * Build the template model: the supertype-chain condition refs (law 1), the
     * qualify wing (law 2), the validator-trio wiring, and the used-only imports.
     */
    MetaTemplateModel buildModel(RRootElement element, RGeneratedJavaClass<?> metaClass, String version) {
        String dataClassName = elementName(element);
        String metaClassName = metaClass.getSimpleName();
        String packageName = metaClass.getPackageName().withDots();
        DottedPath namespace = generatorModel.namespace(element);
        // The data-class FQN goes through the keyword-escape law like every
        // translator-built package (a namespace segment that is a Java keyword
        // escapes with an underscore prefix: `foo.package` → `foo._package` —
        // the PR #410 hold-out finding; identity on keyword-free namespaces).
        String dataClassFqn = com.regnosys.rosetta.generator.java.scoping.JavaPackageName
                .escape(namespace).getName().child(dataClassName).withDots();
        boolean collides = ModelObjectGenerator.collidesWithJavaLang(dataClassName);
        // v3.2 seat 11 (D50): the data class is a first-claim sentinel at every TYPE position unless
        // the #306 java.lang law already writes it canonical (the string positions keep the name)
        String dataClassJavaType = collides ? dataClassFqn : ImportCollisionResolver.typeRefOrBare(dataClassFqn);
        ModelSymbolId typeId = new ModelSymbolId(namespace, dataClassName);

        List<ConditionRef> conditionRefs = collectConditionRefs(element);
        List<QualifyRef> qualifyFunctions = collectQualifyFunctions(element);

        var imports = new ImportCollector(packageName);
        if (!collides) {
            imports.addImport(dataClassFqn);
        }
        imports.addImport(typeTranslator.toTypeFormatValidatorClass(typeId).getCanonicalName().withDots());
        imports.addImport(typeTranslator.toValidatorClass(typeId).getCanonicalName().withDots());
        imports.addImport(typeTranslator.toOnlyExistsValidatorClass(typeId).getCanonicalName().withDots());
        for (ConditionRef ref : conditionRefs) {
            imports.addImport(ref.conditionFqn());
            if (ref.declaringImportFqn() != null) {
                imports.addImport(ref.declaringImportFqn());
            }
        }
        for (QualifyRef ref : qualifyFunctions) {
            imports.addImport(ref.fqn());
        }

        imports.addImport("com.rosetta.model.lib.annotations.RosettaMeta");
        imports.addImport("com.rosetta.model.lib.meta.RosettaMetaData");
        imports.addImport("com.rosetta.model.lib.qualify.QualifyFunctionFactory");
        imports.addImport("com.rosetta.model.lib.qualify.QualifyResult");
        imports.addImport("com.rosetta.model.lib.validation.Validator");
        imports.addImport("com.rosetta.model.lib.validation.ValidatorFactory");
        imports.addImport("com.rosetta.model.lib.validation.ValidatorWithArg");
        imports.addImport("java.util.Arrays");
        if (qualifyFunctions.isEmpty()) {
            imports.addImport("java.util.Collections");
        }
        imports.addImport("java.util.List");
        imports.addImport("java.util.Set");
        imports.addImport("java.util.function.Function");

        // v3.2 seat 11 (D50): every TYPE position the class text writes is a first-claim sentinel -
        // the condition class, the qualify function and the validator trio by their canonical names,
        // the data class and each condition's declaring type unless the #306 java.lang law already
        // wrote them canonical (dataClassJavaType / declaringJavaType). The template's own library
        // tokens come through the token map (metaTokens). The resolver in generate() decides bare or
        // canonical in text order; a meta with no collision keeps the pre-seat bytes.
        var conditionModels = new ArrayList<ConditionRefModel>();
        for (int i = 0; i < conditionRefs.size(); i++) {
            ConditionRef ref = conditionRefs.get(i);
            conditionModels.add(new ConditionRefModel(ImportCollisionResolver.typeRefOrBare(ref.conditionFqn()),
                    ref.declaringJavaType(), ref.conditionFqn(), i == conditionRefs.size() - 1));
        }
        var qualifyModels = new ArrayList<QualifyRefModel>();
        for (int i = 0; i < qualifyFunctions.size(); i++) {
            qualifyModels.add(new QualifyRefModel(ImportCollisionResolver.typeRefOrBare(qualifyFunctions.get(i).fqn()),
                    i == qualifyFunctions.size() - 1));
        }

        return new MetaTemplateModel(packageName, metaClassName,
                dataClassName, dataClassJavaType, version,
                imports.getImports(),
                ImportCollisionResolver.typeRefOrBare(typeTranslator.toValidatorClass(typeId).getCanonicalName().withDots()),
                ImportCollisionResolver.typeRefOrBare(typeTranslator.toTypeFormatValidatorClass(typeId).getCanonicalName().withDots()),
                ImportCollisionResolver.typeRefOrBare(typeTranslator.toOnlyExistsValidatorClass(typeId).getCanonicalName().withDots()),
                conditionModels, qualifyModels);
    }

    /**
     * v3.2 seat 11 (D50): the library types {@code java-meta.stg}'s class text writes, each a
     * first-claim sentinel under its simple name ({@code <m.t.List>} …). The list IS the template's
     * token census — a token the template writes that this list lacks renders empty, which the D11
     * and the hold-out bars catch on every meta.
     */
    public static final java.util.Map<String, String> META_TOKENS = ImportCollisionResolver.typeRefs(java.util.List.of(
            "com.rosetta.model.lib.annotations.RosettaMeta",
            "com.rosetta.model.lib.meta.RosettaMetaData",
            "com.rosetta.model.lib.qualify.QualifyFunctionFactory",
            "com.rosetta.model.lib.qualify.QualifyResult",
            "com.rosetta.model.lib.validation.Validator",
            "com.rosetta.model.lib.validation.ValidatorFactory",
            "com.rosetta.model.lib.validation.ValidatorWithArg",
            "java.util.Arrays",
            "java.util.Collections",
            "java.util.List",
            "java.util.Set",
            "java.util.function.Function"));

    /**
     * One condition reference: the condition class simple name + FQN, and the
     * DECLARING type's collision-aware Java type text + import (null when the
     * declaring type FQN-inlines under the #306 collision law).
     *
     * <p>RECONCILE SEAM (PR #644, the property gate): {@code public} so the IR route's derived-facts reconciler can
     * read {@link #collectConditionRefs}'s answer. Visibility only — see that method's seam note.
     */
    public record ConditionRef(String simpleName, String conditionFqn,
                               String declaringJavaType, String declaringImportFqn) {
    }

    /**
     * Ordering law 1: walk the supertype chain (data supertypes AND a
     * data-extends-choice boundary) to the root, then collect ROOT-FIRST; each
     * type contributes its conditions in declaration order; a {@code choice}
     * contributes its implicit {@code <Name>Choice} condition.
     *
     * <p><b>RECONCILE SEAM (PR #644, the property gate).</b> {@code public} so the IR route's derived-facts
     * reconciler ({@code rune-ir-java}, {@code com.regnosys.rosetta.generator.java.ir.IRDerivedFactsReconciler}) can
     * read THIS generator's own answer beside its own mirror walk of the same law — the third read behind
     * {@code meta.conditionRefs.scanAgrees}, which is what stops the two IR-route halves being a mirror with nothing
     * to answer to. Visibility ONLY: the body, the signature and every call site are unmoved, and no emission path
     * runs through the seam (the emitting caller is {@code generateClass}'s own, unchanged). The read is
     * side-effect free — {@code generatorModel.namespace} and {@code ImportCollisionResolver.typeRefOrBare} are pure
     * over an immutable workspace — so a reconciler calling it perturbs no generator state.
     */
    public List<ConditionRef> collectConditionRefs(RRootElement element) {
        List<RRootElement> chain = new ArrayList<>();
        RRootElement current = element;
        while (current != null) {
            chain.add(current);
            if (current instanceof RDataType dataType) {
                Optional<RDataType> dataSuper = dataType.superType();
                if (dataSuper.isPresent()) {
                    current = dataSuper.get();
                    continue;
                }
                current = dataType.choiceSuperType().orElse(null);
                continue;
            }
            current = null; // choices have no supertype
        }

        List<ConditionRef> refs = new ArrayList<>();
        for (int i = chain.size() - 1; i >= 0; i--) {
            RRootElement link = chain.get(i);
            String declaringName = elementName(link);
            DottedPath declaringNs = generatorModel.namespace(link);
            String declaringFqn = declaringNs.child(declaringName).withDots();
            boolean declaringCollides = ModelObjectGenerator.collidesWithJavaLang(declaringName);
            // v3.2 seat 11 (D50): a first-claim sentinel unless the #306 java.lang law writes it canonical
            String declaringJavaType = declaringCollides ? declaringFqn : ImportCollisionResolver.typeRefOrBare(declaringFqn);
            String declaringImportFqn = declaringCollides ? null : declaringFqn;
            DottedPath dataRulePkg = declaringNs.child("validation").child("datarule");

            if (link instanceof RChoice choice) {
                String simpleName = choice.name() + "Choice";
                refs.add(new ConditionRef(simpleName, dataRulePkg.child(simpleName).withDots(),
                        declaringJavaType, declaringImportFqn));
                continue;
            }
            RDataType dataType = (RDataType) link;
            long namedCount = dataType.conditions().stream()
                    .filter(c -> c.name().isPresent())
                    .count();
            for (RCondition condition : dataType.conditions()) {
                String conditionName = condition.name()
                        .orElseGet(() -> unnamedConditionKind(condition) + namedCount);
                String simpleName = declaringName + conditionName;
                refs.add(new ConditionRef(simpleName, dataRulePkg.child(simpleName).withDots(),
                        declaringJavaType, declaringImportFqn));
            }
        }
        return refs;
    }

    /**
     * The unnamed-condition class-name kind from the expression ROOT
     * ({@code JavaConditionInterface.computeConditionClassName} upstream):
     * a {@code one-of} cardinality check → {@code OneOf}; a {@code choice}
     * cardinality check → {@code Choice}; anything else → {@code DataRule}.
     *
     * <p>PUBLIC since coverage wave D: the datarule generator's class names and
     * this class's XMeta {@code dataRules} refs MUST agree byte-for-byte (the
     * refs import those classes), so the kind rule is single-sourced here —
     * {@code ConditionCases.casesOf} consumes it.
     */
    public static String unnamedConditionKind(RCondition condition) {
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
     * One qualification-function reference: the function class simple name + FQN.
     *
     * <p>RECONCILE SEAM (PR #645 commit 13, the banked cq SF-3 of PR #644): {@code public} so the IR route's
     * derived-facts reconciler can read {@link #collectQualifyFunctions}'s answer. Visibility only - see that
     * method's seam note.
     */
    public record QualifyRef(String simpleName, String fqn) {
    }

    /**
     * Ordering law 2: when this element is a qualifiable ROOT (the FIRST
     * {@code isEvent root} / {@code isProduct root} configuration of its kind in the
     * workspace's load order names it — {@link #isQualifiableRoot}, since v3.2 seat 4 the
     * one rule the validator's warning shares), collect every {@code [qualification]}-annotated
     * function in the WHOLE resource set whose first input type is this root — in
     * the loader's full-path-sorted model order × in-file declaration order —
     * typed to the root. Every non-root type gets the empty list
     * ({@code Collections.emptyList()}).
     *
     * <p><b>RECONCILE SEAM (PR #645 commit 13, the banked cq SF-3 of PR #644).</b> {@code public} so the IR route's
     * derived-facts reconciler ({@code rune-ir-java},
     * {@code com.regnosys.rosetta.generator.java.ir.IRDerivedFactsReconciler}) can read THIS generator's own answer
     * beside the IR emitter's own law over {@code IRModelIndex} - the fact {@code meta.qualifyFunctions}, which is
     * what stops the emitter's qualify wing being a mirror with nothing to answer to (before it, the IR half of the
     * wing was reconciled only at {@code model.qualify.matched}, per MODEL and by function NAME, never as the
     * ORDERED CLASS LIST the {@code *Meta} file actually writes). Visibility ONLY: the body, the signature and
     * every call site are unmoved, and no emission path runs through the seam (the emitting caller is
     * {@link #buildModel}'s own, unchanged). The read is side-effect free over an immutable workspace - the memo
     * {@link #isQualifiableRoot} fills is the two first-wins roots, which a reconciling caller computes to the same
     * two values - so a reconciler calling it perturbs no generator state.
     */
    public List<QualifyRef> collectQualifyFunctions(RRootElement element) {
        // Root matching is NODE-IDENTITY (==) against the first-wins root of each kind.
        // generatorModel.files() spans resolution-only dependency models too (the drr
        // cells' workspaces hold cdm models with their isEvent/isProduct configs), and
        // under first-wins (v3.2 seat 4) THAT is the live hazard, not the retired one: a
        // DEPENDENCY's configuration that sorts first WINS its kind, and the cell's own
        // root then gets an EMPTY meta (its qualifiers warned about). No vendored cell
        // carries the shape (the drr cells' cdm roots are cdm's own types, never an
        // emitted drr element) - the XMETA rows measure it at every chain; whether the
        // released plugin's index admits a dependency's configuration is yes BY SOURCE and
        // by the released 9.83.0 jar's bytecode alike (javap, round 2) under a file: URI -
        // what stays UNMEASURED is the plugin's RUNTIME URI scheme, not the code (the
        // javadoc of isQualifiableRoot below; RQualifiableConfig.firstRoot).
        if (!(element instanceof RDataType) || !isQualifiableRoot(element)) {
            return List.of();
        }
        List<QualifyRef> refs = new ArrayList<>();
        for (RModel model : generatorModel.files()) {
            for (Object root : model.rootElements()) {
                if (!(root instanceof RFunction function)) {
                    continue;
                }
                if (function.annotationRefs().stream()
                        .noneMatch(a -> "qualification".equals(a.annotationName()))) {
                    continue;
                }
                if (function.inputs().isEmpty()) {
                    continue;
                }
                RNode firstInputType = function.inputs().get(0).typeCall() == null
                        ? null
                        : function.inputs().get(0).typeCall().referencedTypeId()
                                .map(id -> generatorModel.workspace().resolveTypeLike(id))
                                .orElse(null);
                if (firstInputType == element) {
                    String fqn = typeTranslator.toFunctionJavaClass(generatorModel.symbolId(function))
                            .getCanonicalName().withDots();
                    refs.add(new QualifyRef(function.name(), fqn));
                }
            }
        }
        return refs;
    }

    /**
     * v3.2 seat 4 (PR #625, F10): the qualifiable root of a kind is the FIRST resolved configuration of
     * that kind over the workspace's models in load order ({@link RQualifiableConfig#firstRoot} — the ONE
     * declaration, read over the SAME population the validator's warning reads, LAW 69): upstream's
     * {@code RosettaConfigExtension.findRosettaQualifiableConfiguration} takes {@code getFirst} over the
     * index's configurations of the kind, and a later configuration of the same kind LOSES with its meta
     * empty and its qualifiers warned about. Before this seat ANY model's configuration made a root, so
     * each of the chaos cell's s22 namespaces registered its own qualifiers (ten D11 rows). Upstream's
     * index order is a function of the resource PATH (the seat's three order probes: the same three
     * files, byte-identical, won differently under two directory names — not a sorted order; a hash over
     * resource URIs is consistent with it and INFERRED, not measured), so where a golden fixes a winner
     * the LOADER replays it by pin (the #413 law — the D11 cell walk's order pins, the hold-out bar's
     * per-group first-file pins) and this seat reads the order it is given, over the WORKSPACE's models
     * (dependency models included). Whether the released plugin's index admits a DEPENDENCY's
     * configuration is, by the post-9.83 source and by the released 9.83.0 jar's bytecode alike
     * ({@code isProjectLocal} returns {@code true} at once for a non-platform URI — {@code javap}-verified at
     * round 2), yes under {@code file:} URIs — which is what this read does; what stays UNMEASURED is the
     * released Maven plugin's RUNTIME URI scheme, not the code: no vendored cell carries a dependency-declared
     * root that any emitted type is (the drr cells' CDM roots are CDM's own types), no oracle group can
     * load a dependency, and the emission-filtered "project-local" read the fix first carried was an
     * inference with no witness, withdrawn under the #614 law; banked as a gen-2 oracle question. Round 1
     * (cq SF-8): the two roots are computed ONCE per generator, not once per element.
     */
    private boolean isQualifiableRoot(RRootElement element) {
        if (!rootsComputed) {
            List<RModel> models = generatorModel.files();
            productRoot = RQualifiableConfig.firstRoot(models, QualifiableKind.IS_PRODUCT).orElse(null);
            eventRoot = RQualifiableConfig.firstRoot(models, QualifiableKind.IS_EVENT).orElse(null);
            rootsComputed = true;
        }
        return element == productRoot || element == eventRoot;
    }
}
