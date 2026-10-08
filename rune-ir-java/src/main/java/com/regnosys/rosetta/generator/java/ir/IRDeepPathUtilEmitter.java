package com.regnosys.rosetta.generator.java.ir;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.regnosys.rosetta.generator.java.template.ImportCollector;
import com.regnosys.rosetta.generator.java.template.TemplateRenderer;
import com.regnosys.rosetta.generator.java.template.model.DeepPathUtilTemplateModel;
import com.regnosys.rosetta.ir.adapter.IRTypeNode;
import com.rosetta.util.types.JavaClass;

/**
 * THE DEEP-PATH UTIL, FROM THE IR ALONE (v3.3 seat 9, PR #645 commit 14) - the
 * {@link IRTypeUnit.Member#DEEP_PATH_UTIL} member of the type unit, and THE SIXTH AND LAST.
 *
 * <p>The file is {@code <ns>.util.<Simple>DeepPathUtil}: no javadoc and no version stamp anywhere; the used-only
 * imports plus the {@code ExpressionOperatorsNullSafe} wildcard static import iff the class has methods; the
 * constructor-injected sibling utils for the alternatives that descend; one {@code choose<Feature>} method per deep
 * feature, each a sequential guard-return chain over the alternatives in declaration order. Every rendering law
 * below is {@code DeepPathUtilGenerator}'s, ported method for method and naming the line it copies.
 *
 * <p><b>THE ONE MEMBER THAT MAY ANSWER "NO FILE BY LAW"</b> ({@link IRTypeUnit.Member#mayWriteNoFile}). A type the
 * eligibility fact refuses writes NO util and the corpus holds no golden for one, so {@link #emit} answers
 * {@link Optional#empty()} for it - and that is the ONLY empty answer this emitter may give. An ELIGIBLE type
 * always renders: an eligible type with ZERO deep features renders the EMPTY class (the golden
 * {@code UnitTypeDeepPathUtil} shape), never an absence. A fact the emitter cannot read THROWS by name
 * ({@link IRDataTypeEmitter.MissingIRFact}), which refuses the WHOLE unit.
 *
 * <p><b>EVERY FACT IS AN IR READ.</b> Eligibility, the deep-feature map and ITS ITERATION ORDER, the alternatives
 * and the descend targets come from {@link IRDerivedFacts} (D10 {@code deepPath.eligible}, D13
 * {@code deepPath.order}, {@code deepPath.features} / {@code .featureMeta}, reconciled per element since PR #644);
 * the item types and getter names from {@link IRPropertyModel} through {@link IRDataTypeEmitter}'s own rendering
 * laws (the POJO member's, byte-identical on the whole corpus); the {@code java.lang} collision from
 * {@link IRDerivedFacts#collides}; the class and package names from {@link IRValidatorScan}'s spellings of
 * {@code JavaTypeTranslator}'s laws, which are the SAME laws {@link IRTypeUnit#outputKey} takes. NOTHING here reads
 * an AST node, a {@code GeneratorModel} or a workspace, and nothing calls a legacy generator's model-reading path -
 * in particular neither {@code DeepPathUtilGenerator} nor {@code DeepPathScan}. {@code IRSharingLawTest} is the
 * witness.
 *
 * <p><b>THE DEPENDENCY ORDER IS REPRODUCED BY CONSTRUCTION, NEVER RE-DERIVED.</b> The old generator collects the
 * injected sibling utils in a real {@code HashSet<JavaClass<?>>} of {@code RGeneratedJavaClass} values and renders
 * the fields, the constructor parameters and the assignments in THAT set's iteration order - which is neither
 * alphabetical nor declaration order (the golden {@code ObservableDeepPathUtil}: {@code indexDeepPathUtil} before
 * {@code assetDeepPathUtil}, buckets 7 and 15 of the default 16-bucket table). This emitter builds a real
 * {@code HashSet<JavaClass<?>>} of {@link IRJavaTypes#generated} values, whose {@code hashCode} is
 * {@code JavaClass}'s own value-based {@code Objects.hash(packageName, simpleName)} - the same hash law over the
 * same two strings, inserted in the same sequence - so the table walk is the same walk. The order is RECONCILED
 * besides ({@code deepPath.dependencies}, against the generator's own {@code dependenciesOf} seam).
 *
 * <p>The template is a BYTE-COPY of the old generator's ({@code templates/ir-java-deeppath-util.stg} of
 * {@code templates/java-deeppath-util.stg}, as {@code ir-java-pojo.stg} is of the POJO's), rendered
 * {@code renderNoIndent} - the FUNCTION-template convention for generator-computed multi-line bodies - so a change
 * to the old template cannot silently move the IR route.
 */
final class IRDeepPathUtilEmitter implements IRTypeUnit.MemberEmitter {

    private static final String TEMPLATE_GROUP = "templates/ir-java-deeppath-util.stg";

    private final IRTypeIndex index;
    private final IRDerivedFacts facts;
    private final TemplateRenderer renderer;
    /** One property model per node, by IDENTITY - the index hands out ONE node per declaration. */
    private final Map<IRTypeNode, IRPropertyModel> properties = new IdentityHashMap<>();

    /**
     * @param index the pass's workspace-wide type index - the descend targets and the targets' property models
     * @param facts the derived facts over that index (eligibility, the deep-feature map and its order, the
     *     {@code java.lang} collision). The deep-path member reads no version stamp: the family writes none
     */
    IRDeepPathUtilEmitter(IRTypeIndex index, IRDerivedFacts facts) {
        this.index = Objects.requireNonNull(index, "index");
        this.facts = Objects.requireNonNull(facts, "facts");
        this.renderer = new TemplateRenderer();
        this.renderer.loadGroupFromClasspath(TEMPLATE_GROUP);
    }

    /**
     * The util's text, or NO FILE BY LAW for a type the eligibility fact refuses
     * ({@code DeepPathUtilGenerator.streamObjects:117}, {@code DeepPathScan.isEligible:121-141}). An eligible type
     * ALWAYS has a text - the zero-feature one is the EMPTY class.
     */
    @Override
    public Optional<String> emit(IRTypeNode node) {
        Objects.requireNonNull(node, "node");
        if (!facts.eligible(node, facts.chain(node))) {
            return Optional.empty();
        }
        return Optional.of(render(node));
    }

    /**
     * The whole file - {@code DeepPathUtilGenerator.generate} ({@code :126-129}): the model, then the template
     * rendered with {@code renderNoIndent}.
     *
     * @throws IRDataTypeEmitter.MissingIRFact when a fact the render needs is not in the IR - by name, never a guess
     */
    String render(IRTypeNode node) {
        return renderer.renderNoIndent(TEMPLATE_GROUP, "deepPathUtilFile", "m", buildModel(node));
    }

    /** {@code DeepPathUtilGenerator.buildModel} ({@code :131-210}) over the IR surface. */
    DeepPathUtilTemplateModel buildModel(IRTypeNode node) {
        String packageName = IRValidatorScan.validatorPackage(node, IRTypeUnit.Member.DEEP_PATH_UTIL);   // :132
        String className = IRValidatorScan.validatorClassName(node, IRTypeUnit.Member.DEEP_PATH_UTIL);   // :133
        var imports = new ImportCollector(packageName);                                                  // :134

        // ONE walker for the whole file, as the old generator runs one DeepPathScan: the ATTRIBUTE views are
        // memoised on it (DeepPathScan:62-70 - stable instances are what the retain check compares) while every
        // feature MAP is still computed fresh, which is the order law (DeepPathScan:36-46).
        IRDerivedFacts.IrDeepPath walk = facts.deepPath();
        List<IRDerivedFacts.Feature> alternatives = walk.attributesOf(node);                             // :136
        // THE ORDER LAW: the fresh map's values() iteration order IS the method order.
        List<IRDerivedFacts.Feature> features =
                new ArrayList<>(facts.featureMap(node, walk).values());                                  // :138

        Recursion recursion = recursionOf(node, alternatives, features, walk);
        Set<JavaClass<?>> dependencies = recursion.dependencies();
        Map<IRDerivedFacts.Feature, Set<String>> deepFeatureNamesByAlt = recursion.deepFeatureNamesByAlt();

        StringBuilder body = new StringBuilder();
        if (!dependencies.isEmpty()) {                                                                   // :159-178
            for (JavaClass<?> dep : dependencies) {
                body.append('\t').append("private final ").append(dep.getSimpleName())
                        .append(' ').append(lowerFirst(dep.getSimpleName())).append(";\n");
                imports.addImport(dep.getCanonicalName().withDots());
            }
            body.append("\t\n");
            body.append("\t@Inject\n");
            body.append("\tpublic ").append(className).append('(')
                    .append(dependencies.stream()
                            .map(d -> d.getSimpleName() + " " + lowerFirst(d.getSimpleName()))
                            .collect(Collectors.joining(", ")))
                    .append(") {\n");
            for (JavaClass<?> dep : dependencies) {
                String inst = lowerFirst(dep.getSimpleName());
                body.append("\t\tthis.").append(inst).append(" = ").append(inst).append(";\n");
            }
            body.append("\t}\n");
            body.append("\t\n");
        }

        if (!features.isEmpty()) {                                                                       // :180-206
            String subjectSimple = IRTypeUnit.simpleName(node);
            String subjectText = typeRef(IRValidatorScan.dataClassFqn(node), imports);
            String subjectParam = escapeReserved(lowerFirst(subjectSimple));
            // The method-scope reserved names: the subject parameter + every guard variable (all methods share
            // the same guard set - the alternatives).
            Set<String> reserved = new LinkedHashSet<>();
            reserved.add(subjectParam);
            for (IRDerivedFacts.Feature alt : alternatives) {
                reserved.add(guardVarName(alt, subjectParam));
            }
            boolean anyMulti = false;
            for (IRDerivedFacts.Feature feature : features) {
                body.append(renderMethod(node, feature, alternatives, deepFeatureNamesByAlt, walk,
                        subjectSimple, subjectText, subjectParam, reserved, imports));
                body.append("\t\n");
                anyMulti |= feature.multi();
            }
            imports.addImport("com.rosetta.model.lib.mapper.MapperS");
            if (anyMulti) {
                imports.addImport("java.util.Collections");
                imports.addImport("java.util.List");
            }
            if (!dependencies.isEmpty()) {
                imports.addImport("javax.inject.Inject");
            }
        }

        return new DeepPathUtilTemplateModel(packageName, className,                                     // :208-209
                imports.getImports(), !features.isEmpty(), body.toString());
    }

    // ------------------------------------------------------------------------------------------ the recursion

    /**
     * ONE COMPUTATION, TWO READERS: the per-alternative deep-feature NAME SET and the DEPENDENCY SET
     * ({@code DeepPathUtilGenerator.recursionOf}, the loop of its {@code buildModel:143-156}). The set is a real
     * {@code HashSet<JavaClass<?>>} deliberately - see the class javadoc's order law.
     */
    private record Recursion(Map<IRDerivedFacts.Feature, Set<String>> deepFeatureNamesByAlt,
                             Set<JavaClass<?>> dependencies) {
    }

    private Recursion recursionOf(IRTypeNode node, List<IRDerivedFacts.Feature> alternatives,
            List<IRDerivedFacts.Feature> features, IRDerivedFacts.IrDeepPath walk) {
        Set<JavaClass<?>> dependencies = new HashSet<>();
        Map<IRDerivedFacts.Feature, Set<String>> deepFeatureNamesByAlt = new IdentityHashMap<>();
        for (IRDerivedFacts.Feature alt : alternatives) {
            IRTypeNode target = walk.descendTarget(alt.field());
            Set<String> deepNames = target == null
                    ? Set.of()
                    : facts.featureMap(target, walk).keySet();
            deepFeatureNamesByAlt.put(alt, deepNames);
            for (IRDerivedFacts.Feature feature : features) {
                if (deepNames.contains(feature.name())) {
                    dependencies.add(IRJavaTypes.generated(utilCanonical(target)));
                }
            }
        }
        return new Recursion(deepFeatureNamesByAlt, dependencies);
    }

    /** A node's own deep-path util class, as a canonical name - the SAME law {@link IRTypeUnit#outputKey} takes. */
    private static String utilCanonical(IRTypeNode node) {
        return IRValidatorScan.validatorPackage(node, IRTypeUnit.Member.DEEP_PATH_UTIL) + "."
                + IRValidatorScan.validatorClassName(node, IRTypeUnit.Member.DEEP_PATH_UTIL);
    }

    // --------------------------------------------------------------------------------------------- the methods

    /** {@code DeepPathUtilGenerator.renderMethod} ({@code :212-255}). */
    private String renderMethod(IRTypeNode node, IRDerivedFacts.Feature feature,
            List<IRDerivedFacts.Feature> alternatives,
            Map<IRDerivedFacts.Feature, Set<String>> deepFeatureNamesByAlt, IRDerivedFacts.IrDeepPath walk,
            String subjectSimple, String subjectText, String subjectParam, Set<String> reserved,
            ImportCollector imports) {
        String returnItem = itemOfFeature(feature);                                                      // :215
        String returnItemText = typeRef(returnItem, imports);                                            // :216
        boolean multi = feature.multi();                                                                 // :217
        String methodName = "choose" + upperFirst(feature.name());                                       // :218

        StringBuilder m = new StringBuilder();
        m.append("\tpublic ").append(multi ? "List<" + returnItemText + ">" : returnItemText)            // :221-223
                .append(' ').append(methodName).append('(').append(subjectText).append(' ')
                .append(subjectParam).append(") {\n");

        for (IRDerivedFacts.Feature alt : alternatives) {                                                // :225-242
            IRPropertyModel.IRProperty altProp = propOf(node, alt.name());
            String guardItem = IRDataTypeEmitter.itemTypeOf(altProp);
            String guardItemText = typeRef(guardItem, imports);
            String getter = IRDataTypeEmitter.getterName(altProp);
            String guardVar = guardVarName(alt, subjectParam);
            String outerLambda = lambdaName(subjectSimple, reserved);

            m.append("\t\tfinal MapperS<").append(guardItemText).append("> ").append(guardVar)
                    .append(" = MapperS.of(").append(subjectParam).append(").<").append(guardItemText)
                    .append(">map(\"").append(getter).append("\", ").append(outerLambda)
                    .append(" -> ").append(outerLambda).append('.').append(getter).append("());\n");
            m.append("\t\tif (exists(").append(guardVar).append(").getOrDefault(false)) {\n");
            m.append("\t\t\treturn ").append(renderAlternativeExpr(
                    armDecision(node, feature, alt, deepFeatureNamesByAlt.get(alt), walk),
                    feature, alt, walk, guardVar, guardItem, returnItemText,
                    methodName, multi, reserved, imports)).append(";\n");
            m.append("\t\t}\n");
        }

        if (multi) {                                                                                     // :244-252
            m.append("\t\treturn Collections.<").append(returnItemText).append(">emptyList();\n");
        } else if (feature.hasMeta()) {
            // The null terminal coerced to the wrapper - golden
            // IndexDeepPathUtil.chooseName: `return FieldWithMetaString.builder().build();`.
            m.append("\t\treturn ").append(returnItemText).append(".builder().build();\n");
        } else {
            m.append("\t\treturn null;\n");
        }
        m.append("\t}\n");
        return m.toString();
    }

    /**
     * ONE ALTERNATIVE'S ARM for one deep feature - the IR half of
     * {@code DeepPathUtilGenerator.ArmDecision} / {@code armDecision}, whose three decisions the
     * {@code deepPath.<feature>.arms} reconcile holds equal per element.
     *
     * @param arm        {@code self} / {@code deeper} / {@code direct}
     * @param unwrap     the {@code "Type coercion"} null-ternary {@code getValue()} step before descending
     * @param featUnwrap the FEATURE-SIDE divergence unwrap of the {@code direct} arm (documented dead at 9.83)
     */
    private record ArmDecision(String arm, boolean unwrap, boolean featUnwrap) {
    }

    private ArmDecision armDecision(IRTypeNode node, IRDerivedFacts.Feature feature, IRDerivedFacts.Feature alt,
            Set<String> altDeepNames, IRDerivedFacts.IrDeepPath walk) {
        String returnItem = itemOfFeature(feature);
        String guardItem = IRDataTypeEmitter.itemTypeOf(propOf(node, alt.name()));
        // Upstream evaluates the self-feature branch FIRST (deepFeature.match(a) - the alternative IS the
        // feature). Documented dead at 9.83: zero bare `return attr.get();` bodies corpus-wide.
        if (walk.match(feature, alt)) {
            return new ArmDecision("self",
                    !guardItem.equals(returnItem) && alt.hasMeta() && !feature.hasMeta(), false);
        }
        if (altDeepNames.contains(feature.name())) {
            return new ArmDecision("deeper", alt.hasMeta(), false);
        }
        IRTypeNode target = walk.descendTarget(alt.field());
        if (target == null) {
            throw new IRDataTypeEmitter.MissingIRFact(
                    "deepPath." + feature.name() + ".directTargetOf." + alt.name(),
                    "the deep feature is read straight off a NON-MODEL alternative, which has no declaration to"
                            + " read it from - the type is refused by name rather than written against a guess");
        }
        String featItem = itemOfProperty(target, feature.name());
        return new ArmDecision("direct", alt.hasMeta(), !featItem.equals(returnItem));
    }

    /** {@code DeepPathUtilGenerator.renderAlternativeExpr} ({@code :257-312}), the decision handed in. */
    private String renderAlternativeExpr(ArmDecision decision, IRDerivedFacts.Feature feature,
            IRDerivedFacts.Feature alt, IRDerivedFacts.IrDeepPath walk, String guardVar, String guardItem,
            String returnItemText, String methodName, boolean multi, Set<String> reserved,
            ImportCollector imports) {
        StringBuilder expr = new StringBuilder(guardVar);
        if ("self".equals(decision.arm())) {
            if (decision.unwrap()) {
                expr.append(unwrapStep(guardItem, returnItemText, reserved));
            }
            return expr.append(multi ? ".getMulti()" : ".get()").toString();
        }

        String receiverItem = guardItem;
        if (decision.unwrap()) {
            // The meta-wrapped alternative unwraps before descending - golden IndexDeepPathUtil
            // `interestRateIndex` branch ("Type coercion" + null-ternary getValue()). The BARE type is the
            // reference's own Java name, which is what JavaTypeTranslator.toJavaReferenceType answers on the
            // other route (the type gate's law, PR #643).
            String bare = IRJavaTypeNames.of(alt.field().type(), alt.field().typeArguments());
            expr.append(unwrapStep(guardItem, typeRef(bare, imports), reserved));
            receiverItem = bare;
        }
        String innerLambda = lambdaName(IRValidatorScan.simpleOf(receiverItem), reserved);
        if ("deeper".equals(decision.arm())) {
            IRTypeNode target = walk.descendTarget(alt.field());
            String depInstance = lowerFirst(IRValidatorScan.simpleOf(utilCanonical(target)));
            expr.append(".<").append(returnItemText).append('>').append(multi ? "mapC" : "map")
                    .append("(\"").append(methodName).append("\", ").append(innerLambda)
                    .append(" -> ").append(depInstance).append('.').append(methodName).append('(')
                    .append(innerLambda).append("))");
        } else {
            IRTypeNode target = walk.descendTarget(alt.field());
            IRPropertyModel.IRProperty featProp = propOf(target, feature.name());
            String featItem = IRDataTypeEmitter.itemTypeOf(featProp);
            String featGetter = IRDataTypeEmitter.getterName(featProp);
            expr.append(".<").append(typeRef(featItem, imports)).append('>')
                    .append(multi ? "mapC" : "map").append("(\"").append(featGetter).append("\", ")
                    .append(innerLambda).append(" -> ").append(innerLambda).append('.')
                    .append(featGetter).append("())");
            if (decision.featUnwrap()) {
                // Feature-side meta divergence (documented dead at 9.83): this alternative's own feature is
                // wrapped while the collapsed representative is plain - unwrap to the representative type.
                expr.append(unwrapStep(featItem, returnItemText, reserved));
            }
        }
        return expr.append(multi ? ".getMulti()" : ".get()").toString();
    }

    /** The {@code "Type coercion"} null-ternary {@code getValue()} unwrap step ({@code :314-319}). */
    private static String unwrapStep(String wrapper, String bareText, Set<String> reserved) {
        String w = lambdaName(IRValidatorScan.simpleOf(wrapper), reserved);
        return ".<" + bareText + ">map(\"Type coercion\", " + w + " -> " + w
                + " == null ? null : " + w + ".getValue())";
    }

    // ------------------------------------------------------------------------------------------- the reconcile

    /**
     * RECONCILED FACT {@code deepPath.dependencies} (v3.3 seat 9, PR #645 commit 14): the injected sibling utils'
     * CANONICAL NAMES in this emitter's own {@code HashSet} iteration order - the order the fields, the
     * constructor parameters and the assignments are rendered in. ONE DECLARATION, TWO CALLERS (LAW 69):
     * {@link #buildModel} renders from {@link #recursionOf}, and the derived-facts reconcile holds this against
     * the generator's own {@code dependenciesOf} seam.
     *
     * @return the canonicals in iteration order; EMPTY for a type the eligibility fact refuses and for a
     *     dependency-free one - a witnessed negative, not an absence
     */
    List<String> dependencyCanonicals(IRTypeNode node) {
        if (!facts.eligible(node, facts.chain(node))) {
            return List.of();
        }
        IRDerivedFacts.IrDeepPath walk = facts.deepPath();
        List<IRDerivedFacts.Feature> alternatives = walk.attributesOf(node);
        List<IRDerivedFacts.Feature> features = new ArrayList<>(facts.featureMap(node, walk).values());
        List<String> canonicals = new ArrayList<>();
        for (JavaClass<?> dependency : recursionOf(node, alternatives, features, walk).dependencies()) {
            canonicals.add(dependency.getCanonicalName().withDots());
        }
        return canonicals;
    }

    /**
     * RECONCILED FACT {@code deepPath.<feature>.arms} (v3.3 seat 9, PR #645 commit 14): the ARM this emitter takes
     * for every (deep feature x alternative) pair, in RENDER ORDER, spelled
     * {@code <feature>|<alternative>=<arm>|unwrap=<b>|featUnwrap=<b>} - the generator's own
     * {@code armsOf} spelling. {@link #armDecision} is the one declaration; the renderer and this read it.
     *
     * @return the arms in render order; EMPTY when the type writes no util or has no deep feature
     */
    List<String> armDecisions(IRTypeNode node) {
        if (!facts.eligible(node, facts.chain(node))) {
            return List.of();
        }
        IRDerivedFacts.IrDeepPath walk = facts.deepPath();
        List<IRDerivedFacts.Feature> alternatives = walk.attributesOf(node);
        List<IRDerivedFacts.Feature> features = new ArrayList<>(facts.featureMap(node, walk).values());
        Recursion recursion = recursionOf(node, alternatives, features, walk);
        List<String> arms = new ArrayList<>();
        for (IRDerivedFacts.Feature feature : features) {
            for (IRDerivedFacts.Feature alt : alternatives) {
                ArmDecision decision =
                        armDecision(node, feature, alt, recursion.deepFeatureNamesByAlt().get(alt), walk);
                arms.add(feature.name() + "|" + alt.name() + "=" + decision.arm()
                        + "|unwrap=" + decision.unwrap() + "|featUnwrap=" + decision.featUnwrap());
            }
        }
        return arms;
    }

    // ------------------------------------------------------------------------------------------ the type plumbing

    /**
     * A FEATURE's item type, resolved against the element that DECLARES it -
     * {@code DeepPathUtilGenerator.renderMethod:215} ({@code propOf(feature.owner(), feature.name())}). A deep
     * feature reached through an alternative is the TARGET's attribute, never the subject's.
     */
    private String itemOfFeature(IRDerivedFacts.Feature feature) {
        return itemOfProperty(feature.owner(), feature.name());
    }

    private String itemOfProperty(IRTypeNode owner, String name) {
        return IRDataTypeEmitter.itemTypeOf(propOf(owner, name));
    }

    /**
     * {@code DeepPathUtilGenerator.propOf} ({@code :373-381}) over the IR property surface: the named property of
     * the element's own {@code allProperties}, or a NAMED refusal - an emitter never writes a getter it cannot
     * resolve.
     */
    private IRPropertyModel.IRProperty propOf(IRTypeNode node, String name) {
        IRPropertyModel model = properties.computeIfAbsent(node, n -> IRPropertyModel.of(n, index));
        for (IRPropertyModel.IRProperty prop : model.allProperties()) {
            if (prop.name().equals(name)) {
                return prop;
            }
        }
        throw new IRDataTypeEmitter.MissingIRFact("deepPath." + node.name() + ".property." + name,
                "the deep-path family needs this property's getter and item type and the property surface does"
                        + " not carry it - the type is refused by name");
    }

    /**
     * Render a type reference and collect its import - {@code DeepPathUtilGenerator.typeRef} ({@code :387-402}):
     * the simple name normally; the FQN (never imported) when the simple name collides with an implicitly imported
     * {@code java.lang} type - the #306 collision law.
     */
    private String typeRef(String canonical, ImportCollector imports) {
        String simple = IRValidatorScan.simpleOf(canonical);
        if (IRValidatorScan.isClassName(canonical)) {
            if (!canonical.startsWith("java.lang.") && facts.collides(simple)) {
                return canonical;
            }
            imports.addImport(canonical);
        }
        return simple;
    }

    // ------------------------------------------------------------------------------------------------ the naming

    /** {@code DeepPathUtilGenerator.guardVarName} ({@code :323-328}). */
    private static String guardVarName(IRDerivedFacts.Feature alt, String subjectParam) {
        String name = escapeReserved(lowerFirst(alt.name()));
        // The subject parameter claims its name first (upstream scope order); zero corpus witnesses.
        return name.equals(subjectParam) ? "_" + name : name;
    }

    /**
     * {@code DeepPathUtilGenerator.lambdaName} ({@code :335-338}): desired = lower-first of the item type's simple
     * name; underscore-prefixed iff reserved by the method scope (the subject parameter + the guard variables).
     */
    private static String lambdaName(String itemSimpleName, Set<String> reserved) {
        String desired = escapeReserved(lowerFirst(itemSimpleName));
        return reserved.contains(desired) ? "_" + desired : desired;
    }

    /**
     * {@code DeepPathUtilGenerator.escapeReserved} ({@code :346-348}) - an attribute named {@code new} declares its
     * guard variable {@code _new}. {@code javax.lang.model.SourceVersion.isKeyword} is a JDK pure function, so it
     * is SHARED rather than re-implemented.
     */
    private static String escapeReserved(String name) {
        return javax.lang.model.SourceVersion.isKeyword(name) ? "_" + name : name;
    }

    private static String lowerFirst(String s) {
        return s.isEmpty() ? s : Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }

    private static String upperFirst(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
