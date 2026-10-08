package com.regnosys.rosetta.generator.java.object.deeppath;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.regnosys.rosetta.ast.RRootElement;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.types.RChoice;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.generator.GenerationException;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.JavaClassGenerator;
import com.regnosys.rosetta.generator.java.object.deeppath.DeepPathScan.ScanAttr;
import com.regnosys.rosetta.generator.java.template.ImportCollector;
import com.regnosys.rosetta.generator.java.template.TemplateRenderer;
import com.regnosys.rosetta.generator.java.template.model.DeepPathUtilTemplateModel;
import com.regnosys.rosetta.generator.java.types.JavaPojoProperty;
import com.regnosys.rosetta.generator.java.types.JavaPojoPropertyOperationType;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.generator.java.types.RGeneratedJavaClass;
import com.regnosys.rosetta.generator.java.types.RJavaPojoInterface;
import com.rosetta.model.lib.ModelSymbolId;
import com.rosetta.util.types.JavaClass;
import com.rosetta.util.types.JavaType;
import com.regnosys.rosetta.generator.java.SilentDegradation;

/**
 * Generates {@code <pkg>.util.<T>DeepPathUtil} classes (coverage burn-down wave C) —
 * one per deep-feature-eligible type ({@link DeepPathScan#isEligible}), one
 * {@code choose<Feature>} method per deep feature, in the 9.83 golden shape:
 *
 * <ul>
 *   <li><b>Sequential guard-return chain</b> in attribute declaration order — one
 *       {@code final MapperS<X> attr = MapperS.of(subject).<X>map("getAttr", …)} +
 *       {@code if (exists(attr).getOrDefault(false)) return …;} block per
 *       alternative; terminal {@code return null;} (single) /
 *       {@code return Collections.<X>emptyList();} (multi) /
 *       {@code return FieldWithMetaX.builder().build();} (single meta-wrapped —
 *       golden {@code IndexDeepPathUtil.chooseName}). The vendored source's nested
 *       if-then-else fold LIES about 9.83 — the guard-return chain is the golden
 *       truth (the #397 rule-cascade render lesson).</li>
 *   <li><b>Method order = {@code DeepPathScan.findDeepFeatureMap(type).values()}
 *       iteration order</b> — the upstream HashMap order law (see the scan's javadoc).</li>
 *   <li><b>Constructor-injected sibling utils</b> for recursive alternatives, in
 *       upstream's {@code HashSet<JavaClass<?>>} iteration order: the fork's
 *       {@link RGeneratedJavaClass} inherits {@link JavaClass}'s value-based
 *       {@code Objects.hash(packageName, simpleName)}, so a real {@code HashSet}
 *       reproduces the golden order byte-for-byte — verified against the ONLY
 *       ≥2-dependency golden corpus-wide ({@code ObservableDeepPathUtil}:
 *       {@code indexDeepPathUtil} before {@code assetDeepPathUtil} — buckets 7
 *       and 15 of the default 16-bucket table; neither alphabetical nor
 *       declaration order).</li>
 *   <li><b>Meta-wrapped alternatives</b> insert the {@code "Type coercion"}
 *       null-ternary {@code getValue()} unwrap before descending (golden
 *       {@code IndexDeepPathUtil}'s {@code interestRateIndex} branch); the wrapper
 *       lambda parameter is the wrapper type's lower-first simple name.</li>
 *   <li><b>Lambda naming</b> follows the fork's proven scope rule: desired name =
 *       lower-first of the ITEM type's simple name, underscore-prefixed iff it
 *       collides with the method parameter or any guard variable
 *       ({@code _asset} vs iso's plain {@code fixedRate10__1}).</li>
 *   <li><b>Used-only imports</b>: {@code MapperS} + the wildcard
 *       {@code ExpressionOperatorsNullSafe} static import iff methods exist;
 *       {@code Collections}/{@code List} iff a multi feature exists;
 *       {@code javax.inject.Inject} iff dependencies exist; an eligible type with
 *       zero deep features renders the EMPTY class (golden
 *       {@code UnitTypeDeepPathUtil}). No javadoc/version stamp anywhere.</li>
 *   <li>The #306 java.lang-collision law at every rendered type position.</li>
 * </ul>
 *
 * <p>Two upstream mechanism branches have ZERO corpus witnesses at 9.83 and are
 * implemented for mechanism fidelity only (documented dead): the self-feature
 * branch (upstream {@code deepFeature.match(a)} — the alternative IS the feature;
 * 0 bare {@code return attr.get();} bodies corpus-wide) and the feature-side
 * meta-divergence unwrap (an alternative whose OWN feature is wrapped while the
 * collapsed representative is plain).
 *
 * <p>Output must match upstream Xtend DeepPathUtilGenerator at 9.83.0 (D11).
 * Uses ST4 template {@code java-deeppath-util.stg} via
 * {@link TemplateRenderer#renderNoIndent} (the FUNCTION-template convention for
 * generator-computed multi-line bodies).
 */
public class DeepPathUtilGenerator extends JavaClassGenerator<RRootElement, RGeneratedJavaClass<?>> {

    private static final String TEMPLATE_GROUP = "templates/java-deeppath-util.stg";

    private final GeneratorModel generatorModel;
    private final JavaTypeTranslator typeTranslator;
    private final JavaTypeUtil typeUtil;
    private final TemplateRenderer renderer;
    private final DeepPathScan scan;
    private final Map<Object, RJavaPojoInterface> pojoCache = new IdentityHashMap<>();

    public DeepPathUtilGenerator(GeneratorModel generatorModel,
                                 JavaTypeTranslator typeTranslator, JavaTypeUtil typeUtil) {
        this.generatorModel = generatorModel;
        this.typeTranslator = typeTranslator;
        this.typeUtil = typeUtil;
        this.renderer = new TemplateRenderer();
        this.renderer.loadGroupFromClasspath(TEMPLATE_GROUP);
        this.scan = new DeepPathScan(generatorModel);
    }

    @Override
    protected Stream<? extends RRootElement> streamObjects(RModel model) {
        return model.rootElements().stream()
                .filter(e -> e instanceof RDataType || e instanceof RChoice)
                .map(e -> (RRootElement) e)
                .filter(scan::isEligible);
    }

    @Override
    protected RGeneratedJavaClass<?> createTypeRepresentation(RRootElement element) {
        return typeTranslator.toDeepPathUtilJavaClass(symbolIdOf(element));
    }

    @Override
    protected String generate(RRootElement element, RGeneratedJavaClass<?> utilClass, String version) {
        DeepPathUtilTemplateModel model = buildModel(element, utilClass);
        return renderer.renderNoIndent(TEMPLATE_GROUP, "deepPathUtilFile", "m", model);
    }

    DeepPathUtilTemplateModel buildModel(RRootElement element, RGeneratedJavaClass<?> utilClass) {
        String packageName = utilClass.getPackageName().withDots();
        String className = utilClass.getSimpleName();
        var imports = new ImportCollector(packageName);

        List<ScanAttr> alternatives = scan.attributesOf(element);
        // THE ORDER LAW: the fresh map's values() iteration order IS the method order.
        List<ScanAttr> features = new ArrayList<>(scan.findDeepFeatureMap(element).values());

        Recursion recursion = recursionOf(alternatives, features);
        Set<JavaClass<?>> dependencies = recursion.dependencies();
        Map<ScanAttr, Set<String>> deepFeatureNamesByAlt = recursion.deepFeatureNamesByAlt();

        StringBuilder body = new StringBuilder();
        if (!dependencies.isEmpty()) {
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

        if (!features.isEmpty()) {
            RJavaPojoInterface subjectPojo = pojoOf(element);
            String subjectText = typeRef(subjectPojo, imports);
            String subjectParam = escapeReserved(lowerFirst(subjectPojo.getSimpleName()));
            // The method-scope reserved names: the subject parameter + every guard
            // variable (all methods share the same guard set — the alternatives).
            Set<String> reserved = new LinkedHashSet<>();
            reserved.add(subjectParam);
            for (ScanAttr alt : alternatives) {
                reserved.add(guardVarName(alt, subjectParam));
            }
            boolean anyMulti = false;
            for (ScanAttr feature : features) {
                body.append(renderMethod(element, feature, alternatives, deepFeatureNamesByAlt,
                        subjectText, subjectParam, reserved, imports));
                body.append("\t\n");
                anyMulti |= feature.isMulti();
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

        return new DeepPathUtilTemplateModel(packageName, className,
                imports.getImports(), !features.isEmpty(), body.toString());
    }

    /**
     * ONE COMPUTATION, TWO READERS (LAW 69): the per-alternative deep-feature NAME SET and the DEPENDENCY SET
     * {@link #buildModel} renders the constructor from, and {@link #dependenciesOf} projects for the reconcile.
     * The set is a real {@code HashSet<JavaClass<?>>} deliberately - the ctor-order law in the class javadoc -
     * and the per-alternative deep map is content-stable, so one computation per alternative suffices.
     */
    record Recursion(Map<ScanAttr, Set<String>> deepFeatureNamesByAlt, Set<JavaClass<?>> dependencies) {
    }

    private Recursion recursionOf(List<ScanAttr> alternatives, List<ScanAttr> features) {
        Set<JavaClass<?>> dependencies = new HashSet<>();
        Map<ScanAttr, Set<String>> deepFeatureNamesByAlt = new IdentityHashMap<>();
        for (ScanAttr alt : alternatives) {
            RRootElement target = scan.descendTarget(alt);
            Set<String> deepNames = target == null
                    ? Set.of()
                    : scan.findDeepFeatureMap(target).keySet();
            deepFeatureNamesByAlt.put(alt, deepNames);
            for (ScanAttr feature : features) {
                if (deepNames.contains(feature.name())) {
                    dependencies.add(typeTranslator.toDeepPathUtilJavaClass(symbolIdOf(target)));
                }
            }
        }
        return new Recursion(deepFeatureNamesByAlt, dependencies);
    }

    /**
     * ONE ALTERNATIVE'S ARM for one deep feature - the three decisions {@link #renderAlternativeExpr} takes,
     * factored into a PURE method the renderer calls, so there is exactly ONE declaration of each and the
     * reconcile seam {@link #armsOf} cannot drift from the bytes.
     *
     * @param arm        {@code self} (the alternative IS the feature - {@link DeepPathScan#match}),
     *                   {@code deeper} (the target's own deep-feature map carries the feature) or
     *                   {@code direct} (the feature is read straight off the target)
     * @param unwrap     the {@code "Type coercion"} null-ternary {@code getValue()} step before descending -
     *                   the meta-wrapped alternative's, and on the {@code self} arm the guard-vs-return
     *                   divergence's
     * @param featUnwrap the FEATURE-SIDE divergence unwrap of the {@code direct} arm (documented dead at 9.83)
     */
    record ArmDecision(String arm, boolean unwrap, boolean featUnwrap) {
    }

    /**
     * THE ARM DECISION, from the element, the feature and the alternative alone - {@code DeepPathUtilGenerator}'s
     * own {@code renderAlternativeExpr} laws ({@code :262-310}), unmoved, in one place.
     */
    ArmDecision armDecision(RRootElement element, ScanAttr feature, ScanAttr alt, Set<String> altDeepNames) {
        JavaType returnItem = itemTypeOf(propOf(feature.owner(), feature.name()));
        JavaType guardItem = itemTypeOf(propOf(element, alt.name()));
        // Upstream evaluates the self-feature branch FIRST (deepFeature.match(a)). Documented dead at 9.83.
        if (scan.match(feature, alt)) {
            return new ArmDecision("self",
                    !guardItem.equals(returnItem) && alt.hasMeta() && !feature.hasMeta(), false);
        }
        if (altDeepNames.contains(feature.name())) {
            return new ArmDecision("deeper", alt.hasMeta(), false);
        }
        RRootElement target = scan.descendTarget(alt);
        if (target == null) {
            throw new GenerationException("deep-path direct feature '" + feature.name()
                    + "' on a non-model alternative '" + alt.name() + "'", null, null);
        }
        JavaType featItem = itemTypeOf(propOf(target, feature.name()));
        return new ArmDecision("direct", alt.hasMeta(), !featItem.equals(returnItem));
    }

    /**
     * RECONCILE SEAM (v3.3 seat 9, PR #645 commit 14). VISIBILITY AND A PROJECTION ONLY - the
     * {@code declaredTypesByPropertyName} precedent: the injected sibling utils' CANONICAL NAMES in the
     * generator's OWN {@code HashSet} iteration order, which is the order the constructor and its fields are
     * rendered in. No law is duplicated here; {@link #recursionOf} is the one declaration, and the IR half
     * ({@code IRDeepPathUtilEmitter}) builds its own set from {@code IRJavaTypes}-built values whose hash law is
     * this set's, so the reconcile compares two independently-built orders and not one order with itself.
     *
     * <p>The list is EMPTY for an element the eligibility fact refuses and for a dependency-free one - a
     * witnessed negative on every cell, the {@code ObservableDeepPathUtil} shape (two dependencies) being the
     * only corpus-wide witness of the {@code >= 2} order law.
     *
     * @return the canonical names, in iteration order; EMPTY when the element writes no util or injects nothing
     */
    public List<String> dependenciesOf(RRootElement element) {
        if (!scan.isEligible(element)) {
            return List.of();
        }
        List<ScanAttr> alternatives = scan.attributesOf(element);
        List<ScanAttr> features = new ArrayList<>(scan.findDeepFeatureMap(element).values());
        List<String> canonicals = new ArrayList<>();
        for (JavaClass<?> dependency : recursionOf(alternatives, features).dependencies()) {
            canonicals.add(dependency.getCanonicalName().withDots());
        }
        return canonicals;
    }

    /**
     * RECONCILE SEAM (v3.3 seat 9, PR #645 commit 14). VISIBILITY AND A PROJECTION ONLY: the ARM this generator
     * takes for every (deep feature x alternative) pair, in RENDER ORDER (the feature map's iteration order, then
     * the alternatives' declaration order), each spelled
     * {@code <feature>|<alternative>=<arm>|unwrap=<b>|featUnwrap=<b>}. {@link #armDecision} is the one
     * declaration; the renderer and this seam read it.
     *
     * @return the arms in render order; EMPTY when the element writes no util or has no deep feature
     */
    public List<String> armsOf(RRootElement element) {
        if (!scan.isEligible(element)) {
            return List.of();
        }
        List<ScanAttr> alternatives = scan.attributesOf(element);
        List<ScanAttr> features = new ArrayList<>(scan.findDeepFeatureMap(element).values());
        Recursion recursion = recursionOf(alternatives, features);
        List<String> arms = new ArrayList<>();
        for (ScanAttr feature : features) {
            for (ScanAttr alt : alternatives) {
                ArmDecision decision =
                        armDecision(element, feature, alt, recursion.deepFeatureNamesByAlt().get(alt));
                arms.add(feature.name() + "|" + alt.name() + "=" + decision.arm()
                        + "|unwrap=" + decision.unwrap() + "|featUnwrap=" + decision.featUnwrap());
            }
        }
        return arms;
    }

    private String renderMethod(RRootElement element, ScanAttr feature, List<ScanAttr> alternatives,
                                Map<ScanAttr, Set<String>> deepFeatureNamesByAlt, String subjectText,
                                String subjectParam, Set<String> reserved, ImportCollector imports) {
        JavaType returnItem = itemTypeOf(propOf(feature.owner(), feature.name()));
        String returnItemText = typeRef(returnItem, imports);
        boolean multi = feature.isMulti();
        String methodName = "choose" + upperFirst(feature.name());

        StringBuilder m = new StringBuilder();
        m.append("\tpublic ").append(multi ? "List<" + returnItemText + ">" : returnItemText)
                .append(' ').append(methodName).append('(').append(subjectText).append(' ')
                .append(subjectParam).append(") {\n");

        for (ScanAttr alt : alternatives) {
            JavaPojoProperty altProp = propOf(element, alt.name());
            JavaType guardItem = itemTypeOf(altProp);
            String guardItemText = typeRef(guardItem, imports);
            String getter = altProp.getOperationName(JavaPojoPropertyOperationType.GET);
            String guardVar = guardVarName(alt, subjectParam);
            String outerLambda = lambdaName(subjectPojoSimpleName(element), reserved);

            m.append("\t\tfinal MapperS<").append(guardItemText).append("> ").append(guardVar)
                    .append(" = MapperS.of(").append(subjectParam).append(").<").append(guardItemText)
                    .append(">map(\"").append(getter).append("\", ").append(outerLambda)
                    .append(" -> ").append(outerLambda).append('.').append(getter).append("());\n");
            m.append("\t\tif (exists(").append(guardVar).append(").getOrDefault(false)) {\n");
            m.append("\t\t\treturn ").append(renderAlternativeExpr(
                    armDecision(element, feature, alt, deepFeatureNamesByAlt.get(alt)),
                    feature, alt, guardVar, guardItem, returnItemText,
                    methodName, multi, reserved, imports)).append(";\n");
            m.append("\t\t}\n");
        }

        if (multi) {
            m.append("\t\treturn Collections.<").append(returnItemText).append(">emptyList();\n");
        } else if (feature.hasMeta()) {
            // The null terminal coerced to the wrapper — golden
            // IndexDeepPathUtil.chooseName: `return FieldWithMetaString.builder().build();`.
            m.append("\t\treturn ").append(returnItemText).append(".builder().build();\n");
        } else {
            m.append("\t\treturn null;\n");
        }
        m.append("\t}\n");
        return m.toString();
    }

    private String renderAlternativeExpr(ArmDecision decision, ScanAttr feature, ScanAttr alt,
                                         String guardVar, JavaType guardItem,
                                         String returnItemText, String methodName, boolean multi,
                                         Set<String> reserved, ImportCollector imports) {
        StringBuilder expr = new StringBuilder(guardVar);
        // Upstream evaluates the self-feature branch FIRST (deepFeature.match(a) —
        // the alternative IS the feature). Documented dead at 9.83: zero bare
        // `return attr.get();` bodies corpus-wide.
        if ("self".equals(decision.arm())) {
            if (decision.unwrap()) {
                expr.append(unwrapStep(guardItem, returnItemText, reserved));
            }
            return expr.append(multi ? ".getMulti()" : ".get()").toString();
        }

        JavaType receiverItem = guardItem;
        if (decision.unwrap()) {
            // The meta-wrapped alternative unwraps before descending — golden
            // IndexDeepPathUtil `interestRateIndex` branch ("Type coercion" +
            // null-ternary getValue()).
            JavaClass<?> bare = typeTranslator.toJavaReferenceType(alt.resolvedType());
            expr.append(unwrapStep(guardItem, typeRef(bare, imports), reserved));
            receiverItem = bare;
        }
        String innerLambda = lambdaName(receiverItem.getSimpleName(), reserved);
        boolean goDeeper = "deeper".equals(decision.arm());
        if (goDeeper) {
            RRootElement target = scan.descendTarget(alt);
            JavaClass<?> depClass = typeTranslator.toDeepPathUtilJavaClass(symbolIdOf(target));
            String depInstance = lowerFirst(depClass.getSimpleName());
            expr.append(".<").append(returnItemText).append('>').append(multi ? "mapC" : "map")
                    .append("(\"").append(methodName).append("\", ").append(innerLambda)
                    .append(" -> ").append(depInstance).append('.').append(methodName).append('(')
                    .append(innerLambda).append("))");
        } else {
            RRootElement target = scan.descendTarget(alt);
            if (target == null) {
                throw new GenerationException("deep-path direct feature '" + feature.name()
                        + "' on a non-model alternative '" + alt.name() + "'", null, null);
            }
            JavaPojoProperty featProp = propOf(target, feature.name());
            JavaType featItem = itemTypeOf(featProp);
            String featGetter = featProp.getOperationName(JavaPojoPropertyOperationType.GET);
            expr.append(".<").append(typeRef(featItem, imports)).append('>')
                    .append(multi ? "mapC" : "map").append("(\"").append(featGetter).append("\", ")
                    .append(innerLambda).append(" -> ").append(innerLambda).append('.')
                    .append(featGetter).append("())");
            if (decision.featUnwrap()) {
                // Feature-side meta divergence (documented dead at 9.83): this
                // alternative's own feature is wrapped while the collapsed
                // representative is plain — unwrap to the representative type.
                expr.append(unwrapStep(featItem, returnItemText, reserved));
            }
        }
        return expr.append(multi ? ".getMulti()" : ".get()").toString();
    }

    /** The {@code "Type coercion"} null-ternary {@code getValue()} unwrap step. */
    private String unwrapStep(JavaType wrapper, String bareText, Set<String> reserved) {
        String w = lambdaName(wrapper.getSimpleName(), reserved);
        return ".<" + bareText + ">map(\"Type coercion\", " + w + " -> " + w
                + " == null ? null : " + w + ".getValue())";
    }

    // -- naming ------------------------------------------------------------------

    private String guardVarName(ScanAttr alt, String subjectParam) {
        String name = escapeReserved(lowerFirst(alt.name()));
        // The subject parameter claims its name first (upstream scope order);
        // zero corpus witnesses of the collision.
        return name.equals(subjectParam) ? "_" + name : name;
    }

    /**
     * The fork's proven lambda-naming rule: desired = lower-first of the item
     * type's simple name; underscore-prefixed iff reserved by the method scope
     * (the subject parameter + the guard variables).
     */
    private static String lambdaName(String itemSimpleName, Set<String> reserved) {
        String desired = escapeReserved(lowerFirst(itemSimpleName));
        return reserved.contains(desired) ? "_" + desired : desired;
    }

    /**
     * Java-keyword escape — an attribute named {@code new} declares its guard
     * variable {@code _new} (golden iso {@code TradeReport33Choice__1DeepPathUtil};
     * the audit-1 drift class, 6 iso witnesses). Upstream's scope machinery
     * escapes identically.
     */
    private static String escapeReserved(String name) {
        return javax.lang.model.SourceVersion.isKeyword(name) ? "_" + name : name;
    }

    private String subjectPojoSimpleName(RRootElement element) {
        return pojoOf(element).getSimpleName();
    }

    // -- type plumbing -------------------------------------------------------------

    private ModelSymbolId symbolIdOf(RRootElement element) {
        return new ModelSymbolId(generatorModel.namespace(element), elementName(element));
    }

    private static String elementName(RRootElement element) {
        if (element instanceof RDataType dataType) {
            return dataType.name();
        }
        return ((RChoice) element).name();
    }

    private RJavaPojoInterface pojoOf(RRootElement element) {
        return pojoCache.computeIfAbsent(element, e -> element instanceof RDataType dataType
                ? new RJavaPojoInterface(dataType, generatorModel, typeTranslator, typeUtil)
                : new RJavaPojoInterface((RChoice) element, generatorModel, typeTranslator, typeUtil));
    }

    private JavaPojoProperty propOf(RRootElement element, String name) {
        for (JavaPojoProperty prop : pojoOf(element).getAllProperties()) {
            if (prop.getName().equals(name)) {
                return prop;
            }
        }
        throw new GenerationException("deep-path property '" + name + "' not found on "
                + elementName(element), null, null);
    }

    private JavaType itemTypeOf(JavaPojoProperty prop) {
        return typeUtil.getItemType(prop.getType());
    }

    /**
     * Render a type reference and collect its import: the simple name normally;
     * the FQN (never imported) when the simple name collides with an implicitly
     * imported {@code java.lang} type — the #306 collision law.
     */
    private String typeRef(JavaType type, ImportCollector imports) {
        String simple = type.getSimpleName();
        if (type instanceof JavaClass<?> javaClass) {
            String fqn = javaClass.getCanonicalName().withDots();
            if (!fqn.startsWith("java.lang.") && collidesWithJavaLang(simple)) {
                return fqn;
            }
            imports.addImport(fqn);
        }
        return simple;
    }

    private static String lowerFirst(String s) {
        return s.isEmpty() ? s : Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }

    private static String upperFirst(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    // Mirrors ValidatorScan.collidesWithJavaLang (package-private there — the
    // established per-family replication). Cached Class.forName lookup, no init;
    // ANY failure = "no collision" (the green-safe fail-direction).
    private static final Map<String, Boolean> JAVA_LANG_COLLISION = new ConcurrentHashMap<>();

    private static boolean collidesWithJavaLang(String simpleName) {
        return JAVA_LANG_COLLISION.computeIfAbsent(simpleName, n -> {
            try {
                Class.forName("java.lang." + n, false, DeepPathUtilGenerator.class.getClassLoader());
                return Boolean.TRUE;
            } catch (ClassNotFoundException | LinkageError | RuntimeException e) {
                if (e instanceof SilentDegradation.Refusal __refusal) throw __refusal;
                // A refusal is not a probe failure: these clauses exist to answer
                // "does this JDK name resolve?" with FALSE, and answering that for a
                // refusal would convert a deliberate decline into a quiet wrong answer.
                // No refusal can arise inside Class.forName today; the guard keeps the
                // C0 contract true by construction if that ever changes (Copilot R2).
                return Boolean.FALSE;
            }
        });
    }
}
