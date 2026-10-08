package com.regnosys.rosetta.generator.java.object.datarule;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.RRootElement;
import com.regnosys.rosetta.ast.enums.CardCheckOp;
import com.regnosys.rosetta.ast.functions.RCondition;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.expressions.unary.RCardinalityCheckExpr;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.supporting.RChoiceOption;
import com.regnosys.rosetta.ast.types.RChoice;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.ast.types.RTypeAlias;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.JavaClassGenerator;
import com.regnosys.rosetta.generator.java.SilentDegradation;
import com.regnosys.rosetta.generator.java.expression.ExpressionCompiler;
import com.regnosys.rosetta.generator.java.expression.handlers.HandlerHelper;
import com.regnosys.rosetta.generator.java.expression.handlers.ReferenceHandler;
import com.regnosys.rosetta.generator.java.function.FunctionDependencyCollector;
import com.regnosys.rosetta.generator.java.function.FunctionExpressionRenderer;
import com.regnosys.rosetta.generator.java.function.FunctionTemplateModel;
import com.regnosys.rosetta.generator.java.function.RenderedStatement;
import com.regnosys.rosetta.generator.java.object.ConditionCases;
import com.regnosys.rosetta.generator.java.object.ConditionCases.ConditionCase;
import com.regnosys.rosetta.generator.java.object.ModelMetaGenerator;
import com.regnosys.rosetta.generator.java.object.ModelObjectGenerator;
import com.regnosys.rosetta.generator.java.template.ImportCollector;
import com.regnosys.rosetta.generator.java.template.ImportCollisionResolver;
import com.regnosys.rosetta.generator.java.template.TemplateRenderer;
import com.regnosys.rosetta.generator.java.template.model.DataRuleTemplateModel;
import com.regnosys.rosetta.generator.java.template.model.PojoTemplateModel;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.generator.java.types.RGeneratedJavaClass;
import com.rosetta.model.lib.ModelSymbolId;
import com.rosetta.util.DottedPath;
import com.rosetta.util.types.JavaClass;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Stream;

/**
 * Data-rule (condition) class generator — coverage burn-down wave D
 * ({@code <pkg>.validation.datarule.<TypeName><ConditionName>}, 3,853 goldens:
 * 448 cdm5 / 555 cdm6 / 2,140 drr / 246 iso20022 / 464 rune-fpml).
 *
 * <p>Upstream mechanism ({@code ConditionGenerator.xtend}, 9.83 golden shape —
 * the vendored parameters/{@code implementsValidatorInterface} wing is
 * post-9.83 and rendered dead by every golden: the interface ALWAYS
 * {@code extends Validator<T>}): one interface per condition of every
 * {@code Data} type plus the SYNTHESIZED {@code <Name>Choice} one-of of every
 * {@code choice} type (upstream {@code ChoiceImpl.getConditions()} — the
 * wave-C {@code DeepPathScan} eligibility precedent), with
 * {@code NAME}/{@code DEFINITION} constants, a {@code Default} implementation
 * carrying {@code @Inject} function dependencies + the fixed
 * {@code getValidationResults} boilerplate (the literal
 * {@code result.getOrDefault(true)} / {@code failureMessage == ""} upstream
 * quirks) + {@code executeDataRule} whose try-body is the compiled condition
 * expression targeting {@link com.rosetta.model.lib.expression.ComparisonResult},
 * and a {@code NoOp}.
 *
 * <p>Byte laws (golden-verified at wave open):
 * <ul>
 *   <li><b>Naming:</b> {@code <TypeName><ConditionName>}; unnamed conditions
 *       take {@code <TypeName>{OneOf|Choice|DataRule}<index>} where the kind
 *       comes from the expression root and the index counts ALL named
 *       conditions of the type (count-all-named — the
 *       {@code PhysicalSettlementPeriodOneOf2} discriminator;
 *       {@link ModelMetaGenerator#unnamedConditionKind} is the single source,
 *       byte-proven corpus-wide through the wave-B XMeta refs).</li>
 *   <li><b>DEFINITION:</b> the expression's source token text (Xtext
 *       {@code getTokenText} join — captured at parse time on
 *       {@link RCondition#expressionText()}) through the
 *       {@link #quote(String)} port of {@code RosettaGrammarUtil.quote}; the
 *       synthesized choice one-of has no source → {@code ""} (golden
 *       {@code AssetChoice}).</li>
 *   <li><b>One-of / elided-argument choice conditions</b> compile through a
 *       generator-synthesized twin ({@link #synthesizeCardinalityTwin}): the
 *       parsed node's argument is elided and its attribute list empty, while
 *       golden lists the type's ALL-attribute names (upstream
 *       {@code caseOneOfOperation} → {@code t.allAttributes}; choice options
 *       project by option TYPE NAME as written — the wave-C law) over the
 *       instance receiver ({@code choice(MapperS.of(asset), Arrays.asList(
 *       "Cash", …), ChoiceRuleValidationMethod.REQUIRED)}).</li>
 *   <li><b>Version javadoc</b> on every file (the D11 loader's per-cell stamp;
 *       iso20022 keeps the literal {@code ${project.version}} — proven at the
 *       wave-B XMeta kind).</li>
 *   <li><b>Imports:</b> used-only (the compiled expression's refs + the fixed
 *       boilerplate set + {@code javax.inject.Inject} iff dependencies);
 *       {@code ExpressionOperatorsNullSafe.*} static wildcard iff operators
 *       used (the per-context import-target law: datarule = NullSafe, 3,550 of
 *       3,853 goldens); regular-import block followed by ONE blank line, then
 *       the static import + ONE blank line — or TWO blank lines when no static
 *       import (the FUNCTION-template gap convention, golden
 *       {@code AmericanExerciseExpirationTimeChoice}).</li>
 *   <li><b>The #306 collision law</b> at the subject type position (FQN-inline
 *       when the simple name collides with {@code java.lang}); the two STRING
 *       positions ({@code ValidationResult.success/failure} type-name argument)
 *       keep the bare Rune name.</li>
 *   <li>Both {@code ValidationType} references render the LONG
 *       {@code ValidationResult.ValidationType.DATA_RULE} form; no
 *       {@code ValidationType} import (golden universal).</li>
 * </ul>
 *
 * <p>Rendered via {@link TemplateRenderer#renderNoIndent} with the class body
 * generator-computed (the wave-C convention — every in-class blank separator
 * line carries its indentation tabs).
 */
public class DataRuleGenerator extends JavaClassGenerator<ConditionCase, RGeneratedJavaClass<?>> {

    private static final String TEMPLATE_GROUP = "templates/java-datarule.stg";

    // The per-object case type, the case enumeration (casesOf) and the owner-name / parameterised
    // gate live in the neutral ConditionCases (v3.2 seat 3, round-1 cq N-4) — shared with the
    // type-format validator's alias-condition wing without one generator importing the other.

    // protected (not private) so the IR-routed subclass (v3.3 seat 3 - the DATA_RULE seam, D54) can read them
    // from its createExpressionCompiler() override. The constructor invokes that factory (see below); these
    // three are assigned before the invocation, so the override observes them fully set - the FunctionGenerator
    // shape, one kind over.
    protected final GeneratorModel generatorModel;
    protected final JavaTypeTranslator typeTranslator;
    protected final JavaTypeUtil typeUtil;
    private final TemplateRenderer renderer;
    private final FunctionExpressionRenderer expressionRenderer;
    private final FunctionDependencyCollector dependencyCollector;

    public DataRuleGenerator(GeneratorModel generatorModel,
                             JavaTypeTranslator typeTranslator, JavaTypeUtil typeUtil) {
        this.generatorModel = generatorModel;
        this.typeTranslator = typeTranslator;
        this.typeUtil = typeUtil;
        this.renderer = new TemplateRenderer();
        this.renderer.loadGroupFromClasspath(TEMPLATE_GROUP);
        // v3.3 seat 3 (D54): the compiler and the renderer come from the two factory seams FunctionGenerator carries,
        // so the IR-routed subclass substitutes IRExpressionCompiler / IRFunctionExpressionRenderer and this base
        // stays byte-identical by construction (the factories return the standard classes here). The dependency
        // collector takes its own compiler from the same factory, as FunctionGenerator's does; only the renderer's
        // compiler renders the gated Java (renderingExpressionCompiler()).
        this.expressionRenderer = createFunctionExpressionRenderer(createExpressionCompiler());
        this.dependencyCollector = new FunctionDependencyCollector(generatorModel, typeTranslator,
                createExpressionCompiler());
    }

    /**
     * Factory for the {@link ExpressionCompiler} that compiles condition bodies to Java - the D43 IR seam
     * (v3.3 seat 3, D54: the DATA_RULE seam, the {@code FunctionGenerator.createExpressionCompiler} shape one
     * kind over), so the IR-routed subclass can substitute an IR-aware compiler with strangler fallback while
     * this base stays byte-identical.
     *
     * <p><b>Construction-time invariant.</b> The constructor calls this once for the dependency collector and
     * once for the expression renderer. An override therefore runs during super-construction - it MUST depend
     * only on {@link #generatorModel}, {@link #typeTranslator} and {@link #typeUtil} (all assigned before the
     * call) and on no subclass state, since subclass fields are not yet initialised.
     *
     * @return a fresh expression compiler wired to this generator's type infrastructure
     */
    protected ExpressionCompiler createExpressionCompiler() {
        return new ExpressionCompiler(generatorModel, typeTranslator, typeUtil);
    }

    /**
     * Factory for the {@link FunctionExpressionRenderer} that renders condition bodies
     * ({@code renderDataRuleCondition} and its ladder) - symmetric to {@link #createExpressionCompiler()} so the
     * IR-routed subclass can substitute its renderer; the base returns the standard renderer, byte-identical.
     * The same construction-time invariant: an override MUST depend only on its {@code compiler} argument and
     * the three protected fields.
     *
     * @param compiler the expression compiler the renderer drives condition bodies through
     * @return a fresh renderer wired to {@code compiler}
     */
    protected FunctionExpressionRenderer createFunctionExpressionRenderer(ExpressionCompiler compiler) {
        return new FunctionExpressionRenderer(compiler);
    }

    /**
     * The expression compiler that renders the condition BODIES - the renderer's, NOT the dependency
     * collector's (the constructor builds two; only the renderer's output is the gated Java). Exposed (the D43
     * seam) so the IR-routed subclass can surface its compiler's counters to the ON-route D11 gate.
     */
    protected ExpressionCompiler renderingExpressionCompiler() {
        return expressionRenderer.expressionCompiler();
    }

    @Override
    protected Stream<? extends ConditionCase> streamObjects(RModel model) {
        // v3.2 seat 3 (census family F9): a typeAlias is a condition OWNER too — upstream's
        // ConditionGenerator streams every RosettaTypeWithConditions (Data, choice AND
        // typeAlias); the walk below used to case Data and choice only, so an alias's
        // conditions never reached this generator and no counter saw them (the chaos
        // C8Even rows: DATA_RULE missingOutput=12, LOUD all zero, both routes).
        return model.rootElements().stream()
                .filter(e -> e instanceof RDataType || e instanceof RChoice || e instanceof RTypeAlias)
                .flatMap(e -> ConditionCases.casesOf((RRootElement) e).stream());
    }

    @Override
    protected RGeneratedJavaClass<?> createTypeRepresentation(ConditionCase conditionCase) {
        return typeTranslator.toConditionJavaClass(
                symbolIdOf(conditionCase.declaringType()), conditionCase.conditionName());
    }

    @Override
    protected String generate(ConditionCase conditionCase, RGeneratedJavaClass<?> ruleClass, String version) {
        RTypeAlias alias = HandlerHelper.aliasOwner(conditionCase.condition());
        if (alias != null) {
            // v3.2 seat 3 (F9): the alias-owned condition class renders through buildModel below
            // (the seat's fix; the counter commit refused every alias case here). What STAYS
            // refused, fail-closed, is the PARAMETERISED sub-wing — a condition whose OWNER
            // declares type parameters: the parameters wing is post-9.83 (the fork's class
            // javadoc) and the released 9.83.0 plugin renders the parameter name as the list op
            // over the item (`item <= max` -> `MapperS.of(bounded) <= MapperC.of(
            // singletonList(bounded)).max()`, oracle group alias-conditions-param) — a
            // self-comparison no fork emission should reproduce. No corpus carrier; the site's
            // counter keeps the shape loud until a ruling says which side to mirror. This seat
            // refuses ONE class — the condition's; the validator seat walking the same alias
            // refuses its WHOLE validator (documented there; round-1 cq SF-10).
            String parameter = ConditionCases.parameterisedOwner(alias);
            if (parameter != null) {
                throw SilentDegradation.refuse(SilentDegradation.Site.TYPE_ALIAS_CONDITION_DROPPED,
                        "condition " + conditionCase.conditionName() + " of the PARAMETERISED typeAlias "
                                + alias.name() + "(" + parameter + " …) (the parameterised condition"
                                + " wing is post-9.83; the released 9.83.0 plugin emits a self-comparison"
                                + " for it, so neither render is justified)",
                        conditionCase.condition());
            }
        }
        DataRuleTemplateModel model = buildModel(conditionCase, ruleClass, version);
        // v3.2 seat 11 (D50 - the file-scope first-claim law): the class text first, every TYPE position a
        // sentinel (the header's annotations and Validator, the subject, the @Inject dependency types and the
        // body's library tokens, plus the expression compiler's own witness sentinels); resolved ONCE in text
        // order from the seed upstream's JavaClassScope registers before writing a byte (the rule class); the
        // losers' imports dropped; then the file wrapper. A rule with no collision renders the pre-seat bytes.
        String classText = renderer.renderNoIndent(TEMPLATE_GROUP, "dataRuleBody", "m", model, "t", DATA_RULE_TOKENS);
        ImportCollisionResolver.ClassResolution resolved = ImportCollisionResolver.resolveClass(
                classText, model.getPackageName() + "." + model.getClassName(), model.getImports());
        return renderer.renderNoIndent(TEMPLATE_GROUP, "dataRuleFile", "m", new PojoTemplateModel(
                model.getPackageName(), resolved.imports(), model.getStaticImports(), resolved.classText()));
    }

    /** The data-rule seat's half of {@code BOILERPLATE_NAME_COLLISION} — see the call site. */
    private static void refuseIfClassNameIsAWrittenType(ImportCollector imports, String className, String packageName,
            String subjectJavaType, String subjectImport, ConditionCase conditionCase, RRootElement declaringType) {
        // round-6 cq SF-3 / spec N-2: the claim is ONE typed pair, SilentDegradation.Claim - the validator seat's shape
        // (LAW 69) - never a name and an origin assigned side by side by convention
        SilentDegradation.Claim clash = null;
        for (String imported : imports.getImports()) {
            if (DottedPath.splitOnDots(imported).last().equals(className)) {
                clash = new SilentDegradation.Claim(imported, SilentDegradation.ClaimOrigin.IMPORT);
                // first claim wins - the validator seat's putIfAbsent order read the other way (round-4 cq N-3). Two
                // same-simple-name imports on one data rule have no corpus, chaos or oracle carrier (the import set is
                // a TreeSet of canonical names), so the alignment is by construction, unwitnessed (round-5 cq N-2).
                break;
            }
        }
        if (clash == null && subjectImport == null && subjectJavaType.equals(className)) { // the java.lang subject (a18, lane AE)
            clash = new SilentDegradation.Claim("java.lang." + subjectJavaType, SilentDegradation.ClaimOrigin.JAVA_LANG);
        }
        if (clash != null) { // a written type's name (a17 / a18, lane Z)
            // round-4 spec SF-1 / round-5 spec SF-2, cq SF-3: the message names what WITNESSES the shape, read from
            // the clash's recorded ORIGIN (one declaration for both seats, LAW 69) - the banked oracle group for an
            // import clash, the seat fixture alone for the java.lang subject
            throw SilentDegradation.refuse(SilentDegradation.Site.BOILERPLATE_NAME_COLLISION,
                    "condition class " + packageName + "." + className + " shares its simple name with " + clash.canonical()
                            + ", a type this data rule writes; the released 9.83.0 plugin claims the class's own name"
                            + " first and writes " + clash.canonical() + " fully qualified" + clash.origin().witnessClause + " - a text-order import law"
                            + " the fork's template does not model",
                    conditionCase.condition() != null ? conditionCase.condition() : declaringType);
        }
    }

    DataRuleTemplateModel buildModel(ConditionCase conditionCase, RGeneratedJavaClass<?> ruleClass,
                                     String version) {
        String packageName = ruleClass.getPackageName().withDots();
        String className = ruleClass.getSimpleName();
        RRootElement declaringType = conditionCase.declaringType();
        String subjectRuneName = elementName(declaringType);
        DottedPath namespace = generatorModel.namespace(declaringType);
        // Keyword-escape law on the hand-built subject FQN (`foo.package` →
        // `foo._package`) — the PR #410 hold-out class-sweep sibling of the
        // ModelMetaGenerator data-class import; identity on keyword-free
        // namespaces, so the byte-proven corpus population is untouched.
        String subjectFqn = com.regnosys.rosetta.generator.java.scoping.JavaPackageName
                .escape(namespace).getName().child(subjectRuneName).withDots();
        boolean collides = ModelObjectGenerator.collidesWithJavaLang(subjectRuneName);
        // v3.2 seat 11 (D50): a first-claim sentinel unless the #306 java.lang law writes it canonical
        String subjectJavaType = collides ? subjectFqn : ImportCollisionResolver.typeRefOrBare(subjectFqn);
        // The subject import: the model type's FQN for a Data/choice owner (unless the #306
        // collision law FQN-inlines it), else null.
        String subjectImport = collides ? null : subjectFqn;
        // round-2 cq N-4: the case's declaring type IS the owner (casesOf builds the pair); the
        // predicate is consulted once more only to ASSERT the two halves agree (LAW 69)
        RTypeAlias ownerAlias = declaringType instanceof RTypeAlias a ? a : null;
        if (conditionCase.condition() != null
                && HandlerHelper.aliasOwner(conditionCase.condition()) != ownerAlias) {
            throw new IllegalStateException("condition " + conditionCase.conditionName() + " of "
                    + subjectRuneName + ": the case's declaring type and the condition's owner disagree");
        }
        if (ownerAlias != null) {
            // v3.2 seat 3 (F9): an alias-owned condition validates the alias's BASE Java type —
            // upstream JavaConditionInterface.instanceClass = toJavaReferenceType(the alias type),
            // i.e. the stripped base (oracle goldens: `Validator<Integer>` / `<BigDecimal>` /
            // `<String>` / `<Boolean>`); imported unless java.lang. The instance name and the two
            // STRING positions keep the alias's Rune name (`evenNat`, "EvenNat").
            JavaClass<?> base = typeTranslator.toJavaReferenceType(
                    generatorModel.resolveTypeCall(ownerAlias.typeCall()));
            String baseCanonical = base.getCanonicalName().withDots();
            // v3.2 seat 11 (D50): the base is a first-claim sentinel; a java.lang base stays bare
            subjectJavaType = ImportCollisionResolver.typeRefOrBare(baseCanonical);
            subjectImport = baseCanonical.startsWith("java.lang.") ? null : baseCanonical;
        }
        String instanceName = HandlerHelper.conditionInstanceName(subjectRuneName);

        var imports = new ImportCollector(packageName);
        if (subjectImport != null) {
            imports.addImport(subjectImport);
        }
        imports.addImport("com.google.inject.ImplementedBy");
        imports.addImport("com.rosetta.model.lib.annotations.RosettaDataRule");
        imports.addImport("com.rosetta.model.lib.expression.ComparisonResult");
        imports.addImport("com.rosetta.model.lib.path.RosettaPath");
        imports.addImport("com.rosetta.model.lib.validation.ValidationResult");
        imports.addImport("com.rosetta.model.lib.validation.Validator");
        imports.addImport("java.util.Arrays");
        imports.addImport("java.util.Collections");
        imports.addImport("java.util.List");
        // @Inject function dependencies — the FUNCTION kind's collector over a
        // synthetic single-condition wrapper (upstream:
        // dependencies.javaDependencies(condition.expression); the fork walk +
        // type-simple-name sort are byte-proven over the FUNCTION population).
        List<FunctionTemplateModel.DependencyModel> deps = List.of();
        RCondition condition = conditionCase.condition();
        if (condition != null && condition.expression() != null) {
            RFunction wrapper = new RFunction();
            wrapper.conditions().add(condition);
            deps = dependencyCollector.collect(wrapper, null, className, packageName);
        }

        List<String> seeds = new ArrayList<>();
        seeds.add(instanceName);
        for (var dep : deps) {
            seeds.add(dep.getFieldName());
        }

        RExpression expression = compileExpressionOf(conditionCase);
        RenderedStatement body = expressionRenderer.renderDataRuleCondition(expression, 4, seeds);

        // Witness sentinels (the #227 first-claim-wins house style) resolve here in
        // FILE ORDER — the subject type + the @Inject dependency types are emitted
        // textually ahead of the body, so they seed the claim map (the
        // FunctionGenerator.buildStandardModel convention); FQN-ed losers get their
        // imports suppressed.
        // v3.2 seat 11 (D50): the body's witness sentinels are no longer resolved apart. Their seeds (the
        // datarule class's OWN simple name - a same-simple-name model type in the body can never be imported,
        // golden TradeIdentifierChoice / LoanCovenantObligationChoice / SensitivityDefinitionChoice; the
        // subject; the @Inject dependency types) are exactly the class header's own writes, so the ONE
        // whole-class resolve in generate() (seeded with the class, the header's sentinels claiming in text
        // order ahead of the body) decides every sentinel and drops every loser's import - the header's
        // library tokens (List, ValidationResult, RosettaPath, ComparisonResult, Arrays, ...) now claim
        // ahead of a same-named body witness as upstream's file scope does.
        String bodySource = body.source();
        for (JavaClass<?> ref : body.refs()) {
            String canonical = ref.getCanonicalName().withDots();
            // A ref whose simple name equals the DECLARED class is never
            // importable (a same-name import in the declaring compilation unit
            // is a compile error) — the body references it FQN-inline.
            if (ref.getSimpleName().equals(className)) {
                continue;
            }
            imports.addImport(canonical);
        }
        List<String> staticImports = new ArrayList<>(new TreeSet<>(
                body.staticWildcardImports().stream()
                        .map(w -> w.getCanonicalName().withDots() + ".*")
                        .toList()));
        if (!deps.isEmpty()) {
            imports.addImport("javax.inject.Inject");
            for (var dep : deps) {
                // An FQN-inlined (collision) dependency type suppresses its
                // import — the FUNCTION-kind convention.
                if (!dep.getTypeName().contains(".")) {
                    imports.addImport(dep.getTypeFqn());
                }
            }
        }
        // v3.2 seat 3, round 2 (oracle group alias-conditions-boilerplate, banked) + round 3 (cq
        // MF-1 / N-10): ONE read over the COMPLETE import set — the subject, the boilerplate names,
        // `Inject` and the dependency types are all in it here — plus the java.lang subject type no
        // import carries. A condition class whose simple name is a type this file writes
        // (`ValidationResult`, `Validator`, `RosettaPath`, `ComparisonResult`, `List`, `Inject`, …
        // or its own subject `Integer` / `String` / `Boolean`): the released plugin claims the
        // class's OWN name first and writes the other type fully qualified
        // (`List<com.rosetta.model.lib.validation.ValidationResult<?>>` inside
        // datarule/ValidationResult.java; `java.lang.Integer` the subject's form); this template
        // writes those names literally, so the shape is REFUSED at the register rather than emitted
        // as the clash javac rejects, which is what it silently was (SilentPairSeatTest a17 / a18).
        // No corpus or chaos carrier. THE TRADE of reading here (round-4 cq N-2): the body has already
        // been compiled and rendered above, so a condition class that both collides AND carries a body
        // the compiler refuses is reported at the body's site and this site under-counts it - loud
        // either way, the file withheld either way; accounting, not silence.
        refuseIfClassNameIsAWrittenType(imports, className, packageName, subjectJavaType, subjectImport,
                conditionCase, declaringType);

        String definition = condition == null
                ? quote("")
                : quote(condition.expressionText().orElse(""));

        String classBody = renderClassBody(className, subjectRuneName, subjectJavaType,
                instanceName, definition, deps, bodySource);

        return new DataRuleTemplateModel(packageName, className, subjectJavaType, version,
                imports.getImports(), staticImports, classBody);
    }

    /**
     * The expression handed to the proven compiler. A parsed cardinality check
     * ({@code one-of} / elided-argument {@code choice}) and the synthesized
     * {@code choice}-type condition both compile through a generator-built twin
     * ({@link #synthesizeCardinalityTwin}); every other parsed expression
     * compiles as-is (its bare attribute references bind through the
     * condition-instance branch in {@code ReferenceHandler}).
     */
    private RExpression compileExpressionOf(ConditionCase conditionCase) {
        RRootElement declaringType = conditionCase.declaringType();
        RCondition condition = conditionCase.condition();
        if (condition == null) {
            // The synthesized <Name>Choice one-of of a choice type.
            return synthesizeCardinalityTwin(declaringType, null, CardCheckOp.ONE_OF, null);
        }
        RExpression expr = condition.expression();
        if (expr instanceof RCardinalityCheckExpr card) {
            return synthesizeCardinalityTwin(declaringType, condition, card.op(),
                    card);
        }
        return expr;
    }

    /**
     * Build the compile twin of a cardinality-check condition: op + necessity
     * copied, the attribute list resolved (one-of → the type's ALL attributes /
     * choice options by TYPE NAME as written — upstream {@code caseOneOfOperation}
     * {@code t.allAttributes}; explicit {@code choice} → the parsed names), the
     * argument = the synthetic instance receiver (renders
     * {@code MapperS.of(<instance>)} through the variable path).
     */
    private RExpression synthesizeCardinalityTwin(RRootElement declaringType, RCondition condition,
                                                  CardCheckOp op, RCardinalityCheckExpr parsed) {
        RCardinalityCheckExpr twin = new RCardinalityCheckExpr();
        twin.setOp(op);
        if (parsed != null) {
            parsed.necessity().ifPresent(twin::setNecessity);
        }
        if (parsed != null && !parsed.attributes().isEmpty()) {
            twin.attributes().addAll(parsed.attributes());
        } else if (declaringType instanceof RChoice choice) {
            for (RChoiceOption option : choice.options()) {
                twin.attributes().add(option.typeCall() == null ? "?" : option.typeCall().typeName());
            }
        } else {
            for (RAttribute attr : generatorModel.allAttributes((RDataType) declaringType)) {
                twin.attributes().add(attr.name());
            }
        }
        twin.setParent(condition != null ? condition : declaringType);
        RExpression receiver = ReferenceHandler.syntheticConditionInstanceRef(
                twin, elementName(declaringType));
        twin.setArgument(receiver);
        return twin;
    }

    /**
     * v3.2 seat 11 (D50): the library types the class text writes, each a first-claim sentinel - the template's
     * (the annotations and the Validator interface, by simple name) and the class body's (the constants).
     */
    static final java.util.Map<String, String> DATA_RULE_TOKENS = ImportCollisionResolver.typeRefs(List.of(
            "com.google.inject.ImplementedBy",
            "com.rosetta.model.lib.annotations.RosettaDataRule",
            "com.rosetta.model.lib.validation.Validator"));
    private static final String T_INJECT = ImportCollisionResolver.typeRefOrBare("javax.inject.Inject");
    private static final String T_LIST = ImportCollisionResolver.typeRefOrBare("java.util.List");
    private static final String T_ARRAYS = ImportCollisionResolver.typeRefOrBare("java.util.Arrays");
    private static final String T_COLLECTIONS = ImportCollisionResolver.typeRefOrBare("java.util.Collections");
    private static final String T_VALIDATION_RESULT = ImportCollisionResolver.typeRefOrBare("com.rosetta.model.lib.validation.ValidationResult");
    private static final String T_ROSETTA_PATH = ImportCollisionResolver.typeRefOrBare("com.rosetta.model.lib.path.RosettaPath");
    private static final String T_COMPARISON_RESULT = ImportCollisionResolver.typeRefOrBare("com.rosetta.model.lib.expression.ComparisonResult");

    private String renderClassBody(String className, String subjectRuneName, String subjectJavaType,
                                   String instanceName, String definition,
                                   List<FunctionTemplateModel.DependencyModel> deps,
                                   String compiledBody) {
        // PR #415 — the reserved-instance-name escape family, BYTE-PINNED against the
        // released-9.83.0 oracle (holdout group `reserved-names`; leg-C finding #1,
        // pinned at PR #414). Upstream's scope machinery resolves a subject type named
        // Path / Result / FailureMessage into THREE distinct observed conventions:
        //   * `type Path` — the fixed RosettaPath parameter escapes NUMERICALLY to
        //     `path0` in Default (the instance keeps `path`); in NoOp BOTH escape
        //     (`path0`, `path1`).
        //   * `type Result` — the ComparisonResult local escapes to `_result`
        //     (declaration + `.getError()` read), BUT upstream's `getOrDefault` guard
        //     still reads the LITERAL `result` — binding the model instance, which has
        //     no getOrDefault: UPSTREAM EMITS NON-COMPILING JAVA for this shape. The
        //     fork reproduces the bytes (the bug-compat law; the CompileGate carries a
        //     documented EXPECTED_NON_COMPILING pin + the FINOS-issue rider in the
        //     waiver-file header).
        //   * `type FailureMessage` — the String local escapes to `_failureMessage`
        //     CONSISTENTLY (declaration, null-guard, reassignment, failure(...) arg).
        // Deeper permutations (e.g. a type named `Path0`) are oracle-unwitnessed and
        // deliberately not guessed at.
        String pathParam = "path".equals(instanceName) ? "path0" : "path";
        String noOpInstance = "path".equals(instanceName) ? "path1" : instanceName;
        String resultLocal = "result".equals(instanceName) ? "_result" : "result";
        String failureLocal = "failureMessage".equals(instanceName) ? "_failureMessage" : "failureMessage";

        StringBuilder b = new StringBuilder();
        b.append("\t\n");
        b.append("\tString NAME = \"").append(className).append("\";\n");
        b.append("\tString DEFINITION = ").append(definition).append(";\n");
        b.append("\t\n");
        b.append("\tclass Default implements ").append(className).append(" {\n");
        b.append("\t\n");
        for (var dep : deps) {
            // v3.2 seat 11 (D50): the dependency type a first-claim sentinel (a collision-canonical name stays)
            b.append("\t\t@").append(T_INJECT).append(" protected ")
             .append(dep.getTypeName().contains(".") ? dep.getTypeName()
                     : ImportCollisionResolver.typeRefOrBare(dep.getTypeFqn())).append(' ')
             .append(dep.getFieldName()).append(";\n");
            b.append("\t\t\n");
        }
        b.append("\t\t@Override\n");
        b.append("\t\tpublic ").append(T_LIST).append('<').append(T_VALIDATION_RESULT).append("<?>> getValidationResults(")
         .append(T_ROSETTA_PATH).append(' ').append(pathParam)
         .append(", ").append(subjectJavaType).append(' ').append(instanceName).append(") {\n");
        b.append("\t\t\t").append(T_COMPARISON_RESULT).append(' ').append(resultLocal).append(" = executeDataRule(")
         .append(instanceName).append(");\n");
        // The guard reads the LITERAL `result` even when the local escaped — the
        // oracle-pinned upstream bug (see the method comment).
        b.append("\t\t\tif (result.getOrDefault(true)) {\n");
        b.append("\t\t\t\treturn ").append(T_ARRAYS).append(".asList(").append(T_VALIDATION_RESULT)
         .append(".success(NAME, ").append(T_VALIDATION_RESULT).append(".ValidationType.DATA_RULE, \"")
         .append(subjectRuneName).append("\", ").append(pathParam).append(", DEFINITION));\n");
        b.append("\t\t\t}\n");
        b.append("\t\t\t\n");
        b.append("\t\t\tString ").append(failureLocal).append(" = ").append(resultLocal).append(".getError();\n");
        b.append("\t\t\tif (").append(failureLocal).append(" == null || ").append(failureLocal)
         .append(".contains(\"Null\") || ").append(failureLocal).append(" == \"\") {\n");
        b.append("\t\t\t\t").append(failureLocal).append(" = \"Condition has failed.\";\n");
        b.append("\t\t\t}\n");
        b.append("\t\t\treturn ").append(T_ARRAYS).append(".asList(").append(T_VALIDATION_RESULT)
         .append(".failure(NAME, ").append(T_VALIDATION_RESULT).append(".ValidationType.DATA_RULE, \"")
         .append(subjectRuneName).append("\", ").append(pathParam).append(", DEFINITION, ")
         .append(failureLocal).append("));\n");
        b.append("\t\t}\n");
        b.append("\t\t\n");
        b.append("\t\tprivate ").append(T_COMPARISON_RESULT).append(" executeDataRule(").append(subjectJavaType).append(' ')
         .append(instanceName).append(") {\n");
        b.append("\t\t\ttry {\n");
        b.append("\t\t\t\t").append(compiledBody).append('\n');
        b.append("\t\t\t}\n");
        b.append("\t\t\tcatch (Exception ex) {\n");
        b.append("\t\t\t\treturn ").append(T_COMPARISON_RESULT).append(".failure(ex.getMessage());\n");
        b.append("\t\t\t}\n");
        b.append("\t\t}\n");
        b.append("\t}\n");
        b.append("\t\n");
        b.append("\t@SuppressWarnings(\"unused\")\n");
        b.append("\tclass NoOp implements ").append(className).append(" {\n");
        b.append("\t\n");
        b.append("\t\t@Override\n");
        b.append("\t\tpublic ").append(T_LIST).append('<').append(T_VALIDATION_RESULT).append("<?>> getValidationResults(")
         .append(T_ROSETTA_PATH).append(' ').append(pathParam)
         .append(", ").append(subjectJavaType).append(' ').append(noOpInstance).append(") {\n");
        b.append("\t\t\treturn ").append(T_COLLECTIONS).append(".emptyList();\n");
        b.append("\t\t}\n");
        b.append("\t}\n");
        return b.toString();
    }

    /**
     * The {@code RosettaGrammarUtil.quote} port, operation-faithful: trim →
     * escape {@code "} → CRLF→LF → ONE {@code \n\n}→{@code \n} collapse pass →
     * newlines become Java string continuations. The newline arms are DEAD at
     * 9.83 by construction — {@code getTokenText} collapses every hidden run
     * (including newlines) to a single space, so no captured expression text
     * contains {@code \n}; zero of the 3,853 goldens carry a continuation.
     * Ported whole for mechanism fidelity (the wave-C documented-dead-branch
     * convention).
     */
    static String quote(String text) {
        return '"' + text.trim()
                .replace("\"", "\\\"")
                .replace("\r\n", "\n")
                .replace("\n\n", "\n")
                .replace("\n", "\\n\" + \n\t\"")
                + '"';
    }

    private ModelSymbolId symbolIdOf(RRootElement element) {
        return new ModelSymbolId(generatorModel.namespace(element), elementName(element));
    }

    /** The owner's Rune name — the class-name prefix and the datarule package key ({@link ConditionCases#ownerName}). */
    private static String elementName(RRootElement element) {
        return ConditionCases.ownerName(element);
    }
}
