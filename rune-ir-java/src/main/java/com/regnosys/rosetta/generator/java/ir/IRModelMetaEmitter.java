package com.regnosys.rosetta.generator.java.ir;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.regnosys.rosetta.generator.java.scoping.JavaPackageName;
import com.regnosys.rosetta.generator.java.template.ImportCollector;
import com.regnosys.rosetta.generator.java.template.ImportCollisionResolver;
import com.regnosys.rosetta.generator.java.template.TemplateRenderer;
import com.regnosys.rosetta.generator.java.template.model.ConditionRefModel;
import com.regnosys.rosetta.generator.java.template.model.MetaTemplateModel;
import com.regnosys.rosetta.generator.java.template.model.PojoTemplateModel;
import com.regnosys.rosetta.generator.java.template.model.QualifyRefModel;
import com.regnosys.rosetta.ir.adapter.IRModelNode;
import com.regnosys.rosetta.ir.adapter.IRTypeNode;
import com.regnosys.rosetta.ir.core.IRKind;
import com.regnosys.rosetta.ir.core.IRQualifiableConfig;
import com.regnosys.rosetta.ir.core.IRQualificationFunction;
import com.regnosys.rosetta.ir.core.IRType;
import com.rosetta.util.DottedPath;

/**
 * THE {@code *Meta} REGISTRY, FROM THE IR ALONE (v3.3 seat 9, PR #645 commit 13) - the
 * {@link IRTypeUnit.Member#META} member of the type unit.
 *
 * <p>The file is {@code <ns>.meta.<Simple>Meta implements RosettaMetaData<Simple>}: the version javadoc, the
 * {@code @RosettaMeta(model=…)} annotation and SEVEN methods - {@code dataRules(ValidatorFactory)} over the
 * CONDITION REFS of the supertype chain, {@code getQualifyFunctions(QualifyFunctionFactory)} over the workspace's
 * {@code [qualification]} functions when this type is the first-wins qualifiable ROOT of a kind, and the validator
 * trio's five accessors. Every rendering law below is ported from {@code ModelMetaGenerator} method for method and
 * names the line it copies.
 *
 * <p><b>EVERY FACT IS AN IR READ.</b> The chain and its condition refs come from {@link IRDerivedFacts}
 * ({@code meta.conditionRefs} / {@code meta.conditionKinds} / {@code meta.unnamedKind}, reconciled since PR #644);
 * the {@code java.lang} collision from {@link IRDerivedFacts#collides}; the class and package names from
 * {@link IRValidatorScan}'s own spellings of {@code JavaTypeTranslator}'s laws, which are the SAME laws
 * {@link IRTypeUnit#outputKey} takes, so a class and its file can never be spelled apart; the qualify wing from
 * {@link IRModelIndex} - the workspace-wide MODEL nodes in LOAD ORDER, whose {@code firstRoot(kind)} is the IR's own
 * reading of {@code RQualifiableConfig.firstRoot} and whose {@link IRModelNode#qualificationFunctions()} carry the
 * D07 facts. NOTHING here reads an AST node, a {@code GeneratorModel} or a workspace, and nothing calls a legacy
 * generator's model-reading path: the shared code is PURE TEXT machinery ({@link ImportCollector},
 * {@link ImportCollisionResolver}, {@link TemplateRenderer}, {@link JavaPackageName#escape}, {@link DottedPath} and
 * the three template-model carriers), which reads no model. {@code IRSharingLawTest} is the witness.
 *
 * <p><b>THE META NEVER ANSWERS "NO FILE BY LAW."</b> Every validated data type has a {@code *Meta}, so an empty
 * answer from this member is a REFUSAL BY NAME ({@link IRTypeUnit.Member#mayWriteNoFile}); a section this emitter
 * cannot render THROWS rather than guessing.
 *
 * <p>The template is a BYTE-COPY of the old generator's ({@code templates/ir-java-meta.stg} of
 * {@code templates/java-meta.stg}, as {@code ir-java-pojo.stg} is of the POJO's), so a change to the old template
 * cannot silently move the IR route; the token map below is a COPY of the old generator's own census for the same
 * reason (the sharing law forbids reading it off the generator class).
 */
final class IRModelMetaEmitter implements IRTypeUnit.MemberEmitter {

    private static final String TEMPLATE_GROUP = "templates/ir-java-meta.stg";

    /**
     * A COPY of {@code ModelMetaGenerator.META_TOKENS} ({@code :232-244}) - the library types the class-body
     * template writes, each a first-claim sentinel under its simple name ({@code <t.List>} …). The list IS the
     * template's token census: a token the template writes that this list lacks renders EMPTY, which the D11 META
     * shadow and the hold-out bar catch on every meta file. It is COPIED rather than referenced because the sharing
     * law forbids an emitter any reference to the legacy generator class - the twelve canonicals are data, and the
     * {@code IRModelMetaEmitterTest} byte comparison against the old generator's own output is what holds the copy
     * to the original.
     */
    private static final Map<String, String> META_TOKENS = ImportCollisionResolver.typeRefs(List.of(
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

    private final IRDerivedFacts facts;
    private final IRModelIndex modelIndex;
    private final String version;
    private final TemplateRenderer renderer;

    /**
     * @param facts      the derived facts over the pass's workspace-wide type index (the chain, the condition
     *     refs and the {@code java.lang} collision all come through it - this member needs no other index read)
     * @param modelIndex the pass's workspace-wide MODEL index - the ONE member whose facts are not a function of the
     *     node's own chain needs it (the qualify wing's first-wins root is a LOAD-ORDER fact over every model of the
     *     workspace, dependency models included). Required non-null: an absent index would silently turn every type
     *     into a non-root and write {@code Collections.emptyList()} where the corpus has 37 entries
     * @param version    the host's version stamp for this model, empty when it passes none
     */
    IRModelMetaEmitter(IRDerivedFacts facts, IRModelIndex modelIndex, String version) {
        this.facts = Objects.requireNonNull(facts, "facts");
        this.modelIndex = Objects.requireNonNull(modelIndex, "modelIndex");
        this.version = Objects.requireNonNull(version, "version");
        this.renderer = new TemplateRenderer();
        this.renderer.loadGroupFromClasspath(TEMPLATE_GROUP);
    }

    /** Every validated type writes this file, or the type is REFUSED by name - never "no file by law". */
    @Override
    public Optional<String> emit(IRTypeNode node) {
        return Optional.of(render(node));
    }

    /**
     * The whole file - {@code ModelMetaGenerator.generate} ({@code :123-134}), the D50 two-step: the class text
     * first with every TYPE position a first-claim sentinel, the sentinels resolved in text order from the ONE seed
     * the meta class itself is, the losers' imports dropped, then the file wrapper.
     */
    String render(IRTypeNode node) {
        MetaTemplateModel model = buildModel(node);
        String classText = renderer.render(TEMPLATE_GROUP, "metaBody", "m", model, "t", META_TOKENS);
        ImportCollisionResolver.ClassResolution resolved = ImportCollisionResolver.resolveClass(
                classText, model.getPackageName() + "." + model.getMetaClassName(), model.getImports());
        return renderer.render(TEMPLATE_GROUP, "metaFile", "m",
                new PojoTemplateModel(model.getPackageName(), resolved.imports(), List.of(), resolved.classText()));
    }

    /** {@code ModelMetaGenerator.buildModel} ({@code :147-224}) over the IR surface. */
    MetaTemplateModel buildModel(IRTypeNode node) {
        String dataClassName = IRTypeUnit.simpleName(node);                                            // :148
        String metaClassName = IRValidatorScan.validatorClassName(node, IRTypeUnit.Member.META);       // :149
        String packageName = IRValidatorScan.validatorPackage(node, IRTypeUnit.Member.META);           // :150
        // :156-157 - the data-class FQN goes through the keyword-escape law like every translator-built package
        String dataClassFqn = IRValidatorScan.dataClassFqn(node);                                      // :156-157
        boolean collides = facts.collides(dataClassName);                                              // :158
        String dataClassJavaType = IRValidatorScan.dataClassJavaType(dataClassFqn, collides);          // :161

        List<IRTypeNode> chain = facts.chain(node);
        List<ConditionRef> conditionRefs = collectConditionRefs(chain);                                // :164
        List<String> qualifyFunctions = qualifyFunctionClasses(node, modelIndex);                      // :165

        String typeFormatValidator = validatorCanonical(node, IRTypeUnit.Member.TYPE_FORMAT_VALIDATOR);
        String cardinalityValidator = validatorCanonical(node, IRTypeUnit.Member.CARDINALITY_VALIDATOR);
        String onlyExistsValidator = validatorCanonical(node, IRTypeUnit.Member.ONLY_EXISTS_VALIDATOR);

        var imports = new ImportCollector(packageName);                                                // :167-197
        if (!collides) {
            imports.addImport(dataClassFqn);
        }
        imports.addImport(typeFormatValidator);
        imports.addImport(cardinalityValidator);
        imports.addImport(onlyExistsValidator);
        for (ConditionRef ref : conditionRefs) {
            imports.addImport(ref.conditionFqn());
            if (ref.declaringImportFqn() != null) {
                imports.addImport(ref.declaringImportFqn());
            }
        }
        for (String qualifyClass : qualifyFunctions) {
            imports.addImport(qualifyClass);
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
            // :192-194 - Collections only when the qualify list is empty (the used-only import law)
            imports.addImport("java.util.Collections");
        }
        imports.addImport("java.util.List");
        imports.addImport("java.util.Set");
        imports.addImport("java.util.function.Function");

        var conditionModels = new ArrayList<ConditionRefModel>();                                      // :205-210
        for (int i = 0; i < conditionRefs.size(); i++) {
            ConditionRef ref = conditionRefs.get(i);
            conditionModels.add(new ConditionRefModel(ImportCollisionResolver.typeRefOrBare(ref.conditionFqn()),
                    ref.declaringJavaType(), ref.conditionFqn(), i == conditionRefs.size() - 1));
        }
        var qualifyModels = new ArrayList<QualifyRefModel>();                                          // :211-215
        for (int i = 0; i < qualifyFunctions.size(); i++) {
            qualifyModels.add(new QualifyRefModel(
                    ImportCollisionResolver.typeRefOrBare(qualifyFunctions.get(i)),
                    i == qualifyFunctions.size() - 1));
        }

        return new MetaTemplateModel(packageName, metaClassName,                                       // :217-223
                dataClassName, dataClassJavaType, version,
                imports.getImports(),
                ImportCollisionResolver.typeRefOrBare(cardinalityValidator),
                ImportCollisionResolver.typeRefOrBare(typeFormatValidator),
                ImportCollisionResolver.typeRefOrBare(onlyExistsValidator),
                conditionModels, qualifyModels);
    }

    /**
     * ONE member's class as a CANONICAL NAME - {@code JavaTypeTranslator.toValidatorClass} /
     * {@code toTypeFormatValidatorClass} / {@code toOnlyExistsValidatorClass} ({@code :255-289}), whose package is
     * the declaring namespace with the member's sub-package below it, ESCAPED as a whole, and whose simple name is
     * the type's own with the member's suffix. The SAME two spellings {@link IRTypeUnit#outputKey} composes.
     */
    private static String validatorCanonical(IRTypeNode node, IRTypeUnit.Member member) {
        return IRValidatorScan.validatorPackage(node, member) + "."
                + IRValidatorScan.validatorClassName(node, member);
    }

    // ------------------------------------------------------------------------------------- the condition refs

    /**
     * ONE condition reference as the template needs it - {@code ModelMetaGenerator.ConditionRef} ({@code :254-256}),
     * rebuilt here from {@link IRDerivedFacts.ConditionRefLink}.
     *
     * @param conditionFqn       the condition class's fully-qualified name
     * @param declaringJavaType  the DECLARING type as written at the {@code factory.<…>create} TYPE position
     * @param declaringImportFqn the declaring type's import, or {@code null} under the #306 collision law
     */
    private record ConditionRef(String conditionFqn, String declaringJavaType, String declaringImportFqn) {
    }

    /**
     * Ordering law 1 - {@code ModelMetaGenerator.collectConditionRefs} ({@code :273-321}) over the IR chain. The
     * walk itself (root-first, the choice's implicit {@code <Name>Choice}, the unnamed condition's count-all-named
     * suffix) is {@link IRDerivedFacts#conditionRefLinks}'s, the ONE declaration both halves of the
     * {@code meta.conditionRefs} reconcile already read; what this method adds is the three rendered spellings the
     * old generator computes per ref from its DECLARING link.
     *
     * <p><b>THE UNESCAPED SPELLING IS THE OLD GENERATOR'S, COPIED AS WRITTEN.</b> {@code :295} builds the declaring
     * type's FQN as {@code declaringNs.child(declaringName).withDots()} and {@code :300} the data-rule package as
     * {@code declaringNs.child("validation").child("datarule")} - NEITHER goes through
     * {@code JavaPackageName.escape}, where the SUBJECT type's own FQN at {@code :156-157} does. That asymmetry is
     * upstream's and it is reproduced here byte for byte rather than "fixed": the {@code *Meta}'s import block must
     * spell what the file spells, and the hold-out group {@code name-escaping} is the witness that holds both
     * spellings against the old generator's own output.
     */
    private List<ConditionRef> collectConditionRefs(List<IRTypeNode> chain) {
        List<ConditionRef> refs = new ArrayList<>();
        for (IRDerivedFacts.ConditionRefLink ref : IRDerivedFacts.conditionRefLinks(chain)) {
            IRTypeNode link = ref.link();
            String declaringName = IRTypeUnit.simpleName(link);                                        // :293
            DottedPath declaringNs = DottedPath.splitOnDots(IRTypeUnit.namespaceOf(link));             // :294
            String declaringFqn = declaringNs.child(declaringName).withDots();                         // :295
            boolean declaringCollides = facts.collides(declaringName);                                 // :296
            // :298 - a first-claim sentinel unless the #306 java.lang law writes it canonical
            String declaringJavaType = declaringCollides
                    ? declaringFqn : ImportCollisionResolver.typeRefOrBare(declaringFqn);
            String declaringImportFqn = declaringCollides ? null : declaringFqn;                       // :299
            DottedPath dataRulePkg = declaringNs.child("validation").child("datarule");                // :300
            refs.add(new ConditionRef(dataRulePkg.child(ref.simpleName()).withDots(),                  // :304 / :316
                    declaringJavaType, declaringImportFqn));
        }
        return refs;
    }

    // ----------------------------------------------------------------------------------------- the qualify wing

    /**
     * Ordering law 2 - {@code ModelMetaGenerator.collectQualifyFunctions} ({@code :360-402}) and
     * {@code isQualifiableRoot} ({@code :427-435}) over the IR alone. ONE DECLARATION, TWO CALLERS (LAW 69): this
     * emitter renders its {@code getQualifyFunctions} body from it, and the derived-facts reconcile holds it against
     * the generator's OWN {@code collectQualifyFunctions} through the seam opened at this commit
     * ({@code meta.qualifyFunctions}).
     *
     * <p>THE THREE LAWS, each the old generator's:
     * <ul>
     *   <li><b>only a data type, and only a ROOT</b> ({@code :373}): a choice is never a qualifiable root, and a
     *       data type is one only when it IS the first-wins root of a kind. The old generator compares NODE
     *       IDENTITY against {@code RQualifiableConfig.firstRoot}; the IR compares the RESOLVED QUALIFIED NAME
     *       against {@link IRModelIndex#firstRoot(String)}, the same first-wins-in-load-order law computed over the
     *       MODEL NODES - the substitution {@code model.qualify.root.<kind>} and {@code model.qualify.matched} hold
     *       under assertion on every cell (risk R4, PR #644);</li>
     *   <li><b>the whole workspace, in load order x in-file order</b> ({@code :377-378}): the old generator walks
     *       {@code generatorModel.files()} and each model's root elements; {@link IRModelIndex#nodes()} is the same
     *       population in the same order - dependency models included, which is the live hazard the first-wins law
     *       creates and which the root fact measures;</li>
     *   <li><b>the first DECLARED input's type resolves to the root</b> ({@code :389-394}): the adapter carries only
     *       the {@code [qualification]} functions with a declared input, each with its first input's type as a
     *       reference ({@code AstToIRAdapter.adaptModelNode}, the D07 facts).</li>
     * </ul>
     *
     * <p>The class canonical is {@code JavaTypeTranslator.toJavaFunctionClass}'s own ({@code :385-390}):
     * {@code JavaPackageName.escape(<ns>.functions)} with the function's own name below it - the declaring MODEL
     * node's namespace IS the function's ({@code IRQualificationFunction}'s javadoc), and the BC entry point the old
     * generator calls takes the FUNCTION-origin router unconditionally, so a report- or rule-origin function
     * reached through this wing would spell the same package there and here.
     *
     * @return the qualify functions' class canonicals in order, or an EMPTY list for every type that is not a root
     */
    static List<String> qualifyFunctionClasses(IRTypeNode node, IRModelIndex modelIndex) {
        Objects.requireNonNull(node, "node");
        Objects.requireNonNull(modelIndex, "modelIndex");
        if (node.kind() != IRKind.STRUCT || !isQualifiableRoot(node, modelIndex)) {                    // :373
            return List.of();
        }
        String rootName = node.name();
        List<String> refs = new ArrayList<>();
        for (IRModelNode model : modelIndex.nodes()) {                                                 // :377
            for (IRQualificationFunction function : model.qualificationFunctions()) {                  // :378-388
                Optional<String> firstInput = function.firstInputType().resolvedQualifiedName();       // :389-393
                if (firstInput.isPresent() && firstInput.get().equals(rootName)) {                     // :394
                    refs.add(functionClassCanonical(model.namespace(), function.name()));              // :395-397
                }
            }
        }
        return refs;
    }

    /**
     * {@code ModelMetaGenerator.isQualifiableRoot} ({@code :427-435}) from the IR: this node IS the first-wins root
     * of a kind. The comparison is by RESOLVED QUALIFIED NAME because the IR has no node identity to compare across
     * the adapter's two products; a declaration's own qualified name is {@link IRTypeNode#name()}
     * ({@code IRType.resolvedQualifiedName}'s javadoc), which is the spelling
     * {@link IRModelIndex#firstRoot(String)} answers in.
     */
    private static boolean isQualifiableRoot(IRTypeNode node, IRModelIndex modelIndex) {
        for (String kind : IRQualifiableConfig.KINDS) {
            Optional<String> root = modelIndex.firstRoot(kind).flatMap(IRType::resolvedQualifiedName);
            if (root.isPresent() && root.get().equals(node.name())) {
                return true;
            }
        }
        return false;
    }

    /**
     * A qualification function's class canonical - {@code JavaTypeTranslator.toJavaFunctionClass}
     * ({@code :385-390}): the declaring namespace with {@code functions} below it, ESCAPED as a whole, then the
     * function's own name. Unlike the condition refs above this one IS escaped, because the old generator builds it
     * through the translator rather than by hand.
     */
    private static String functionClassCanonical(String namespace, String functionName) {
        DottedPath functionPackage = (namespace == null || namespace.isEmpty()
                ? DottedPath.of() : DottedPath.splitOnDots(namespace)).child("functions");
        return JavaPackageName.escape(functionPackage).getName().child(functionName).withDots();
    }
}
