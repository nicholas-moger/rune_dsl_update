package com.regnosys.rosetta.generator.java.function;

import com.regnosys.rosetta.generator.java.SilentDegradation;
import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.expressions.references.RDeepFeatureCall;
import com.regnosys.rosetta.ast.expressions.references.REnumValueRef;
import com.regnosys.rosetta.ast.expressions.references.RFeatureCall;
import com.regnosys.rosetta.ast.expressions.references.RSymbolReference;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.functions.RRule;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.expression.ExpressionCompiler;
import com.regnosys.rosetta.generator.java.expression.handlers.NavigationHandler;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.rosetta.model.lib.ModelSymbolId;
import com.rosetta.util.types.JavaClass;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Walks all expression ASTs inside an {@link RFunction} to discover the other
 * functions and utilities that the function calls — i.e., the {@code @Inject}
 * dependencies that must be declared as fields in the generated Java class.
 *
 * <p>Strategy: depth-first traversal via {@link RNode#children()}. At each
 * {@link RSymbolReference} node we check whether the resolved symbol is an
 * {@link RFunction}; if so, a {@link FunctionTemplateModel.DependencyModel}
 * is emitted (deduplicated by field name).
 *
 * <p>Deep feature calls ({@code ->>}) are detected by looking for
 * {@link RDeepFeatureCall} nodes. The receiver's type determines which
 * {@code DeepPathUtil} class to inject (e.g., {@code Asset} →
 * {@code AssetDeepPathUtil} in {@code <namespace>.util}).
 */
public class FunctionDependencyCollector {

    private final GeneratorModel generatorModel;
    private final JavaTypeTranslator typeTranslator;
    private final ExpressionCompiler expressionCompiler;

    /**
     * Binary-compatibility constructor (no {@link ExpressionCompiler}). The DeepPathUtil
     * receiver-type resolution then uses the static {@code resolvedFeature()}-only fallback —
     * preserving the pre-PR-#205 behaviour for isolated unit calls.
     */
    public FunctionDependencyCollector(GeneratorModel generatorModel,
                                       JavaTypeTranslator typeTranslator) {
        this(generatorModel, typeTranslator, null);
    }

    /**
     * Full constructor (facet deep_path_util_resolution, PR #205). The {@code expressionCompiler}
     * — built from the SAME {@code (generatorModel, typeTranslator, typeUtil)} as the renderer's —
     * lets {@link #collectDeepPathUtilDependency} resolve the DeepPathUtil receiver type gm-aware
     * through {@link NavigationHandler#resolveDeepReceiverTypeName(RDeepFeatureCall, ExpressionCompiler)},
     * the SAME resolver the renderer uses, so the injected {@code @Inject <Type>DeepPathUtil} field
     * name matches the lambda body's {@code <type>DeepPathUtil} reference exactly.
     */
    public FunctionDependencyCollector(GeneratorModel generatorModel,
                                       JavaTypeTranslator typeTranslator,
                                       ExpressionCompiler expressionCompiler) {
        this.generatorModel = generatorModel;
        this.typeTranslator = typeTranslator;
        this.expressionCompiler = expressionCompiler;
    }

    /**
     * Collect all {@code @Inject} dependencies for a function.
     *
     * <p>Walks every expression reachable from the function's shortcuts,
     * operations, conditions, and post-conditions. Returns a sorted,
     * deduplicated list of {@link FunctionTemplateModel.DependencyModel}.
     *
     * @param func the function to analyse
     * @return sorted (by field name) deduplicated list of dependencies
     */
    public List<FunctionTemplateModel.DependencyModel> collect(RFunction func) {
        return collect(func, null);
    }

    /**
     * Collect all {@code @Inject} dependencies for a function, supplying the
     * <em>generated host class's</em> simple name so that a rule-target
     * dependency whose generated class simple name <b>collides</b> with the
     * host's can be FQN-qualified inline (and its import suppressed) — see the
     * {@link RRule} branch of {@link #walkExpression}.
     *
     * <p>Engine PR #6 facet (b). The host name MUST be the actual generated
     * class simple name (e.g. {@code MaturityDateOfTheUnderlierRule} <b>with</b>
     * the {@code Rule} suffix for rule-origin functions — i.e.
     * {@code clazz.getSimpleName()} threaded from
     * {@code FunctionGenerator.buildClassWithBaseInterface}), NOT the bare
     * function name. Passing the bare name would never detect the collision the
     * golden depends on (golden
     * {@code asic/.../MaturityDateOfTheUnderlierRule} injects the
     * {@code common/.../MaturityDateOfTheUnderlierRule} dependency
     * <em>fully-qualified inline</em> with NO import because the simple names
     * match).
     *
     * @param func the function to analyse
     * @param hostSimpleName the generated host class's simple name — gates the
     *        rule-target {@code RRule} injection branch: it must be NON-NULL to
     *        inject an {@code @Inject <Name>Rule} dependency (and run self-collision
     *        detection). {@code null} on the standard function path + test/non-rule
     *        callers DISABLES the whole {@code RRule} branch (no rule dep injected),
     *        so plain functions are unaffected (engine PR #6 facet (b) gate, made
     *        effective at Copilot R1).
     * @return sorted (by field name) deduplicated list of dependencies
     */
    public List<FunctionTemplateModel.DependencyModel> collect(RFunction func, String hostSimpleName) {
        // BC overload: the host simple name doubles as the RRule injection gate and
        // the rule-collision target. The FUNCTION-dependency host-name collision
        // check (the function-path facet) additionally needs the emitted host
        // PACKAGE — which this overload cannot supply — so it passes a null package,
        // disabling that check and preserving the pre-existing behaviour for every
        // 2-arg caller (the unit tests + any caller that does not thread the
        // emitted package).
        return collect(func, hostSimpleName, hostSimpleName, null);
    }

    /**
     * Collect all {@code @Inject} dependencies, supplying the full generated host
     * class identity so that an injected FUNCTION dependency whose generated class
     * simple name collides with the host's — but lives in a DIFFERENT package
     * (e.g. a {@code version2} CDE function injecting the identically-named
     * {@code version1} function) — is rendered fully-qualified inline with NO
     * import, matching the golden.
     *
     * <p>Without this, the fork imports the colliding FQN and references it by
     * simple name; that simple name resolves to the <em>enclosing class itself</em>
     * (Java single-type-import cannot shadow the type declared in the same
     * compilation unit — it would be a compile error), so the generated source does
     * not compile. The golden never imports such a type; it qualifies the field type
     * inline. This is the FUNCTION-dependency analogue of the rule-target collision
     * handling already in {@link #injectRuleDependency}.
     *
     * <p>The check is regression-free by construction: a currently byte-identical
     * (green) function can never carry such a dependency, because the only
     * alternative rendering — import + simple name — is the non-compiling form the
     * golden cannot emit; and a same-package / self dependency takes the
     * {@code !package.equals} branch and keeps its plain simple name (its import is
     * suppressed downstream by {@code ImportCollector}'s same-package rule).
     *
     * @param func the function to analyse
     * @param ruleGateSimpleName gates the {@link RRule} injection branch (see the
     *        2-arg overload): NON-NULL only on the rule/report emission path.
     * @param hostClassSimpleName the actual generated host class simple name, used
     *        ONLY for FUNCTION-dependency collision detection (it never gates the
     *        RRule branch). On the rule path this equals {@code ruleGateSimpleName};
     *        on the plain function path it is the function's own generated class
     *        simple name (while {@code ruleGateSimpleName} stays null).
     * @param hostPackageName the actual emitted host-class package, paired with
     *        {@code hostClassSimpleName} to distinguish a genuine cross-package
     *        collision (FQN inline) from a same-package / self dependency (plain
     *        simple name). {@code null} disables the function-dependency collision
     *        check entirely.
     * @return sorted (by field name) deduplicated list of dependencies
     */
    public List<FunctionTemplateModel.DependencyModel> collect(RFunction func,
            String ruleGateSimpleName, String hostClassSimpleName, String hostPackageName) {
        HostInfo host = new HostInfo(ruleGateSimpleName, hostClassSimpleName, hostPackageName);
        // Seen set keyed on field name — insertion-ordered for determinism, but
        // we sort at the end anyway.
        Set<String> seen = new LinkedHashSet<>();
        List<FunctionTemplateModel.DependencyModel> deps = new ArrayList<>();
        // facet injectDepCollision (PR #364): the same-simple-name FUNCTION-dependency
        // numbering map (empty on the universal no-collision fast path).
        Map<String, NumberedDependency> depNumbering =
                collidingFunctionDependencyNumbering(func, generatorModel, typeTranslator);

        for (var shortcut : func.shortcuts()) {
            if (shortcut.expression() != null) {
                walkExpression(shortcut.expression(), seen, deps, host, depNumbering);
            }
        }
        for (var op : func.operations()) {
            if (op.expression() != null) {
                walkExpression(op.expression(), seen, deps, host, depNumbering);
            }
        }
        for (var cond : func.conditions()) {
            if (cond.expression() != null) {
                walkExpression(cond.expression(), seen, deps, host, depNumbering);
            }
        }
        for (var pc : func.postConditions()) {
            if (pc.expression() != null) {
                walkExpression(pc.expression(), seen, deps, host, depNumbering);
            }
        }

        // facet reportOperations (PR #322): sort by the TYPE's SIMPLE name
        // (case-sensitive), field name as the tiebreaker — upstream sorts the
        // dependency list by class name, so a lowercase-named rule class
        // (drr `reporting rule barrier` → class `barrierRule`) sorts AFTER
        // every uppercase-initial class ('b' > 'Z'), where the previous
        // field-name sort filed it under 'b' among the lowerCamel fields.
        // Order-neutral for every other dependency: lowerCamelCase only
        // lowers the FIRST character, which is order-preserving across the
        // (uniformly uppercase-initial) remaining population, and same-simple-
        // name collision pairs keep their field-name relative order via the
        // tiebreaker.
        deps.sort(Comparator
                .comparing((FunctionTemplateModel.DependencyModel d) ->
                        simpleTypeName(d.getTypeName()))
                .thenComparing(FunctionTemplateModel.DependencyModel::getFieldName));
        return deps;
    }

    /**
     * The simple name of a dependency's rendered type — a collision-inlined
     * FQN ({@code a.b.C}) reduces to its last segment so the sort key is the
     * class SIMPLE name either way (PR #322).
     */
    private static String simpleTypeName(String typeName) {
        if (typeName == null) return "";
        return typeName.substring(typeName.lastIndexOf('.') + 1);
    }

    /**
     * A same-simple-name FUNCTION-dependency collision-group member (facet
     * injectDepCollision, PR #364): the NUMBERED field name plus whether this member is
     * the group HEAD ({@code importWinner}) — the head keeps the simple-name field type
     * + import; every other member renders its field type fully-qualified inline with
     * the import suppressed, because the head's single-type-import SHADOWS the simple
     * name for the whole compilation unit (a same-package member could otherwise have
     * rendered bare — golden csa {@code CommodityLeg1} is same-package yet FQN).
     */
    public record NumberedDependency(String fieldName, boolean importWinner) {}

    /**
     * Numbered field names for same-simple-name FUNCTION-dependency collision groups
     * (facet injectDepCollision, PR #364): TWO DIFFERENT functions (distinct generated
     * classes) whose {@code lowerCamelCase} field names collide — golden
     * {@code IsCSALeg1Aligned} calls the csa-local {@code CommodityLeg1(product)} AND
     * {@code common.CommodityLeg1(product)}, rendering ONE numbered group:
     * {@code @Inject protected CommodityLeg1 commodityLeg10;} (the common one — group
     * head, imported) + {@code @Inject protected drr.regulation.csa.rewrite.trade
     * .functions.CommodityLeg1 commodityLeg11;} (FQN inline, no import). Upstream:
     * {@code JavaDependencyProvider} collects into a {@code Set<JavaClass<?>>} (FQN
     * identity — both survive) and the class-scope {@code computeActualNames} numbering
     * numbers the same-desired-name group. The fork mirror is this PURE function,
     * consulted at BOTH the collector's field decls and the call-site receiver
     * ({@code ReferenceHandler.disambiguateDependencyReceiver}), keyed by the callee's
     * generated-class FQN; group order = FQN sort ascending (the corpus carriers'
     * golden order — {@code drr.regulation.common…} &lt; {@code drr.regulation.csa…}).
     *
     * <p>Returns an EMPTY map when no bare name is shared by 2+ distinct classes — the
     * universal fast path, leaving {@link #injectFunctionDependency}'s pre-facet body
     * byte-for-byte intact. Green-safe by construction: a collision pair pre-fix
     * rendered ONE bare field with BOTH call sites on it — semantically wrong Java —
     * so no green file can carry the shape (corpus census: exactly the
     * IsCSALeg1/2Aligned pair, verified against the frozen goldens' numbered-FQN
     * {@code @Inject} signature).
     *
     * <p>Cost discipline: pass 1 groups callees by bare name on OBJECT identity (no
     * type resolution); only a group with 2+ distinct callee objects resolves FQNs
     * (dedup — a re-resolved duplicate of the same function collapses back to one).
     * Deliberately DISJOINT from {@link #collidingDependencyAliasNames} (dep-vs-ALIAS
     * numbering): the corpus carries no dep+dep+alias triple; a future one needs the
     * two mechanisms unified into one registration-ordered group.
     */
    /**
     * Per-function memo (Copilot #364 R1): the numbering is a pure function of the
     * {@link RFunction}'s expression roots (an RFunction belongs to ONE workspace/cell,
     * so identity keying is exact), and it is consulted once by {@code collect()} AND
     * once per dependency-receiver render
     * ({@code ReferenceHandler.disambiguateDependencyReceiver}) — without the memo the
     * receiver path re-walked the whole function AST per call site (O(calls × AST)).
     * Weak keys let corpora unload; synchronized for cross-thread safety (surefire
     * forks are separate JVMs, but in-JVM callers stay safe).
     */
    private static final Map<RFunction, Map<String, NumberedDependency>>
            COLLISION_NUMBERING_MEMO =
            Collections.synchronizedMap(new java.util.WeakHashMap<>());

    public static Map<String, NumberedDependency> collidingFunctionDependencyNumbering(
            RFunction func, GeneratorModel generatorModel, JavaTypeTranslator typeTranslator) {
        if (func == null || generatorModel == null || typeTranslator == null) {
            return Map.of();
        }
        Map<String, NumberedDependency> memo = COLLISION_NUMBERING_MEMO.get(func);
        if (memo != null) {
            return memo;
        }
        Map<String, NumberedDependency> computed =
                computeCollidingFunctionDependencyNumbering(func, generatorModel, typeTranslator);
        COLLISION_NUMBERING_MEMO.put(func, computed);
        return computed;
    }

    /** The uncached compute half of {@link #collidingFunctionDependencyNumbering} —
     *  pass 1 groups by OBJECT identity (no type resolution); only a 2+-identity
     *  group resolves FQNs (the cheap-pass discipline: the universal no-collision
     *  function pays one allocation-light walk, ONCE, memoized). */
    private static Map<String, NumberedDependency> computeCollidingFunctionDependencyNumbering(
            RFunction func, GeneratorModel generatorModel, JavaTypeTranslator typeTranslator) {
        Map<String, Set<RFunction>> calleesByBare = new LinkedHashMap<>();
        for (var shortcut : func.shortcuts()) {
            collectFunctionCallees(shortcut.expression(), calleesByBare);
        }
        for (var op : func.operations()) {
            collectFunctionCallees(op.expression(), calleesByBare);
        }
        for (var cond : func.conditions()) {
            collectFunctionCallees(cond.expression(), calleesByBare);
        }
        for (var pc : func.postConditions()) {
            collectFunctionCallees(pc.expression(), calleesByBare);
        }
        Map<String, NumberedDependency> numbering = new LinkedHashMap<>();
        for (var entry : calleesByBare.entrySet()) {
            if (entry.getValue().size() < 2) {
                continue;
            }
            // Resolve + dedup by FQN (identity can over-count re-resolved duplicates
            // of the SAME function; those collapse here and the group evaporates).
            Set<String> fqns = new TreeSet<>();
            for (RFunction callee : entry.getValue()) {
                fqns.add(typeTranslator.toFunctionJavaClass(generatorModel.symbolId(callee))
                        .getCanonicalName().withDots());
            }
            if (fqns.size() < 2) {
                continue;
            }
            int idx = 0;
            for (String fqn : fqns) {
                numbering.put(fqn, new NumberedDependency(entry.getKey() + idx, idx == 0));
                idx++;
            }
        }
        return numbering;
    }

    /**
     * Pass-1 walk of {@link #collidingFunctionDependencyNumbering}: groups every
     * {@link RFunction} callee (the {@link RSymbolReference} arm AND the
     * {@link REnumValueRef}-disguised nav-receiver arm — the same two callee shapes
     * {@link #walkExpression} injects for) by its bare {@code lowerCamelCase} field
     * name, on object identity (no type resolution).
     */
    private static void collectFunctionCallees(RNode node, Map<String, Set<RFunction>> out) {
        if (node == null) {
            return;
        }
        RFunction callee = null;
        if (node instanceof RSymbolReference ref) {
            callee = ref.symbol()
                    .filter(RFunction.class::isInstance)
                    .map(RFunction.class::cast)
                    .orElse(null);
        } else if (node instanceof REnumValueRef evr
                && evrChainHead(evr) instanceof RFunction fn) {
            callee = fn;
        }
        if (callee != null) {
            out.computeIfAbsent(lowerCamelCase(callee.name()),
                            k -> Collections.newSetFromMap(new IdentityHashMap<>()))
                    .add(callee);
        }
        for (RNode child : node.children()) {
            collectFunctionCallees(child, out);
        }
    }

    /**
     * v3.1 flip seat 2 (LADDER RETIREMENT) — the EVR chain-head, read
     * authority-first-with-legacy-fallback: the C1 authority slot
     * ({@code resolvedHead}) supersedes the legacy GAP_A callable rung
     * ({@code resolvedSymbol}) wherever it is bound. A chain headed by a LOCAL
     * ALIAS that shadows a global callable ({@code TradeForEvent -> product}
     * under {@code alias TradeForEvent:} with {@code drr.base.trade.TradeForEvent}
     * in scope) bound the GLOBAL through the legacy rung while the render was
     * already alias-shaped (name-driven) — so the collector injected a FALSE
     * {@code @Inject} dependency the golden does not carry (measured on
     * {@code Create_AnnaDsbUpiRequestFromReportableEventAndUnderlying}: the
     * import + field pair). An empty authority slot falls back to the legacy
     * read unchanged — a genuine bare callable head keeps its dependency (the
     * seat test's positive control).
     *
     * <p>Shared by all three EVR head reads: the callee-numbering pass and the
     * two nav-receiver injection arms.
     */
    private static RNode evrChainHead(REnumValueRef evr) {
        RNode authority = evr.resolvedHead().orElse(null);
        return authority != null ? authority : evr.resolvedSymbol().orElse(null);
    }

    /**
     * Class-member names that COLLIDE between an injected function DEPENDENCY
     * (a {@code @Inject protected X name;} field) and a SHORTCUT/alias
     * (rendered as {@code protected abstract MapperS<…> name(…)}).
     *
     * <p>Upstream rune-dsl's
     * {@code com.regnosys.rosetta.generator.GeneratorScope#computeActualNames}
     * groups every class-scope generated identifier by desired name and numbers any
     * group of size &gt; 1 in registration order (dependencies first, shortcuts
     * second). So a dependency {@code dayOfWeek} and an alias {@code dayOfWeek}
     * render as {@code dayOfWeek0} (the dependency) and {@code dayOfWeek1} (the
     * alias) consistently across the field decl, the abstract method decl, the impl,
     * and EVERY call site. The fork emitted the bare name for both members.
     *
     * <p>Pure function of the {@link RFunction}: reuses {@link #lowerCamelCase} and
     * the SAME function-callee name formula as {@link #collect} (a function
     * dependency's field name is {@code lowerCamelCase(callee.name())}), walking the
     * same expression roots (shortcuts + operations + conditions + post-conditions).
     * Returns the set of BARE names that are simultaneously a function-dependency
     * field name AND a shortcut name; an empty set otherwise (the common no-collision
     * case — keeps the disambiguation a strict no-op on every collision-free function,
     * so it is GREEN-SAFE by construction: a name shared between a dependency and an
     * alias always byte-mismatches the numbered golden, i.e. it was already waivered).
     *
     * <p>Only a FUNCTION dependency can collide with a shortcut — a shortcut name is a
     * lower-camel single identifier, which a rule dependency's {@code <Name>Rule} field
     * or a {@code …DeepPathUtil} field never matches — so the rule / deep-path branches
     * of {@link #collect} are deliberately not mirrored here.
     *
     * <p>Consulted at BOTH the declaration seats (the {@code dependencies} list + the
     * {@code AliasModel} name in {@code FunctionGenerator}) and the call sites
     * (the dependency receiver + the alias invocation in {@code ReferenceHandler}), so
     * the decl and every reference agree on the numbered name.
     */
    public static Set<String> collidingDependencyAliasNames(RFunction func) {
        var shortcuts = func.shortcuts();
        if (shortcuts.isEmpty()) {
            return Set.of();
        }
        Set<String> aliasNames = new LinkedHashSet<>();
        for (var shortcut : shortcuts) {
            if (shortcut.name() != null) {
                aliasNames.add(shortcut.name());
            }
        }
        if (aliasNames.isEmpty()) {
            return Set.of();
        }
        Set<String> depFieldNames = new LinkedHashSet<>();
        for (var shortcut : shortcuts) {
            collectFunctionDependencyNames(shortcut.expression(), depFieldNames);
        }
        for (var op : func.operations()) {
            collectFunctionDependencyNames(op.expression(), depFieldNames);
        }
        for (var cond : func.conditions()) {
            collectFunctionDependencyNames(cond.expression(), depFieldNames);
        }
        for (var pc : func.postConditions()) {
            collectFunctionDependencyNames(pc.expression(), depFieldNames);
        }
        depFieldNames.retainAll(aliasNames);
        return depFieldNames;
    }

    /**
     * Depth-first walk collecting {@code lowerCamelCase(callee.name())} for every
     * {@link RSymbolReference} resolving to an {@link RFunction} — the
     * dependency-field-name half of {@link #collidingDependencyAliasNames}, mirroring
     * the {@link RFunction}-callee branch of {@link #walkExpression} (NAME only; the
     * Java-class / FQN resolution that branch performs is irrelevant to a name
     * collision). Static + model-free so the call sites can consult it directly.
     */
    private static void collectFunctionDependencyNames(RNode node, Set<String> out) {
        if (node == null) {
            return;
        }
        if (node instanceof RSymbolReference ref) {
            ref.symbol()
                    .filter(RFunction.class::isInstance)
                    .map(RFunction.class::cast)
                    .ifPresent(callee -> out.add(lowerCamelCase(callee.name())));
        }
        for (RNode child : node.children()) {
            collectFunctionDependencyNames(child, out);
        }
    }

    /**
     * True iff {@code func} declares a shortcut/alias named {@code name}. A CHEAP (no
     * AST walk) collision check for the DEPENDENCY-RECEIVER call site, where
     * {@code name} is ALREADY a dependency field name ({@code lowerCamelCase} of the
     * called function), so a dependency-versus-alias collision reduces to a
     * shortcut-name match. The call-site sibling of
     * {@link #collidingDependencyAliasNames} (which the once-per-function declaration
     * seats use): a per-receiver {@code collidingDependencyAliasNames} would re-walk
     * the whole function AST on every dependency call, so the hot call sites use this
     * directional check instead (Copilot R1).
     */
    public static boolean hasShortcutNamed(RFunction func, String name) {
        for (var shortcut : func.shortcuts()) {
            if (name.equals(shortcut.name())) {
                return true;
            }
        }
        return false;
    }

    /**
     * True iff {@code func} calls a function dependency whose field name
     * ({@code lowerCamelCase(callee.name())}) equals {@code name}. An EARLY-EXIT AST
     * walk for the ALIAS-INVOCATION call site, where {@code name} is ALREADY a
     * shortcut name, so a dependency-versus-alias collision reduces to a
     * function-dependency-name match — the opposite-direction call-site sibling of
     * {@link #hasShortcutNamed} (early-exit, vs the full-set
     * {@link #collidingDependencyAliasNames}).
     */
    public static boolean hasFunctionDependencyNamed(RFunction func, String name) {
        for (var shortcut : func.shortcuts()) {
            if (callsFunctionDependencyNamed(shortcut.expression(), name)) {
                return true;
            }
        }
        for (var op : func.operations()) {
            if (callsFunctionDependencyNamed(op.expression(), name)) {
                return true;
            }
        }
        for (var cond : func.conditions()) {
            if (callsFunctionDependencyNamed(cond.expression(), name)) {
                return true;
            }
        }
        for (var pc : func.postConditions()) {
            if (callsFunctionDependencyNamed(pc.expression(), name)) {
                return true;
            }
        }
        return false;
    }

    /** Early-exit half of {@link #hasFunctionDependencyNamed}, mirroring the
     *  {@link RFunction}-callee branch of {@link #collectFunctionDependencyNames}. */
    private static boolean callsFunctionDependencyNamed(RNode node, String name) {
        if (node == null) {
            return false;
        }
        if (node instanceof RSymbolReference ref
                && ref.symbol()
                        .filter(RFunction.class::isInstance)
                        .map(RFunction.class::cast)
                        .map(callee -> name.equals(lowerCamelCase(callee.name())))
                        .orElse(false)) {
            return true;
        }
        for (RNode child : node.children()) {
            if (callsFunctionDependencyNamed(child, name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Generated host-class identity threaded through the dependency walk:
     * {@code ruleGateSimpleName} (RRule injection gate, null off the rule path),
     * {@code classSimpleName} + {@code packageName} (FUNCTION-dependency
     * collision target — the actual emitted host class).
     */
    private record HostInfo(String ruleGateSimpleName, String classSimpleName, String packageName) {}

    // -------------------------------------------------------------------------
    // Recursive walker
    // -------------------------------------------------------------------------

    /**
     * Depth-first walk of an expression tree.
     *
     * <p>At each {@link RSymbolReference} that resolves to an {@link RFunction}:
     * <ol>
     *   <li>Computes the field name (lower-camel-case of the function name).</li>
     *   <li>Resolves the Java class via the type translator.</li>
     *   <li>Creates a {@link FunctionTemplateModel.DependencyModel} and adds it
     *       to {@code deps} if not already seen.</li>
     * </ol>
     * The node's {@link RNode#children()} are always walked recursively
     * regardless of node type, so no expression sub-tree is missed.
     *
     * @param node  current AST node
     * @param seen  mutable set of already-collected field names
     * @param deps  mutable accumulator for discovered dependencies
     * @param host  generated host-class identity: {@code ruleGateSimpleName}
     *        gates RRule injection (null off the rule path), {@code classSimpleName}
     *        + {@code packageName} are the FUNCTION-dependency collision target
     */
    private void walkExpression(RNode node, Set<String> seen,
                                List<FunctionTemplateModel.DependencyModel> deps,
                                HostInfo host,
                                Map<String, NumberedDependency> depNumbering) {
        if (node == null) {
            return;
        }

        if (node instanceof RSymbolReference ref) {
            ref.symbol().ifPresent(symbol -> {
                if (symbol instanceof RFunction callee) {
                    injectFunctionDependency(callee, seen, deps, host, depNumbering);
                } else if (symbol instanceof RRule rule) {
                    // Engine PR #6 facet (b): a bare no-arg {@code then <rule>}
                    // reference — OR (facet injectedRuleRef, PR #263) an EXPLICIT-args
                    // cross-namespace rule invocation (`cde.valuation.ValuationMethod(arg)`).
                    // BOTH resolve the symbol to an RRule and BOTH render the call via
                    // the injected `<Name>Rule` field
                    // (FunctionExpressionRenderer.renderBareInvokableThenSet for the
                    // no-arg then-body; ReferenceHandler's explicit-args receiver arm
                    // for the with-args call), so BOTH need the @Inject field. The
                    // `ref.args().isEmpty()` gate was dropped (PR #263) so an
                    // explicit-args rule reference also gains the field; injectRuleDependency
                    // is idempotent (seen.add), so a no-arg + with-args reference to the
                    // same rule injects exactly one field. Green-safe by construction: a
                    // rule reference (either shape) renders to a non-compiling raw name
                    // without the field, so no green file is affected.
                    // Bridge the RRule to a synthetic RFunction
                    // (Origin.RULE) so the SAME toFunctionJavaClass routing the
                    // RuleGenerator uses yields the <namespace>.reports/<Name>Rule
                    // class. The injected field name = lowerCamelCase of the
                    // GENERATED CLASS simple name (i.e. <Name>Rule, WITH the
                    // "Rule" suffix — golden L18/L43:
                    // `maturityDateOfTheUnderlierRule`, NOT
                    // `maturityDateOfTheUnderlier`). This is the SINGLE SOURCE OF
                    // TRUTH shared with the call receiver rendered in facet (c)
                    // (FunctionExpressionRenderer.renderBareInvokableThenSet derives
                    // the same name from the same generated class), guaranteeing
                    // the @Inject field name matches the call receiver exactly.
                    //
                    // facet injectRuleDepInFunctions (PR #332): the rule-family gate
                    // ({@code host.ruleGateSimpleName() != null}) was DROPPED — a plain
                    // {@code <ns>.functions} FUNCTION that references a rule now gains
                    // the @Inject field too, matching upstream JavaDependencyProvider,
                    // which adds EVERY RosettaRule symbol reference as a dependency
                    // unconditionally (no host-kind gate), and upstream
                    // ExpressionGenerator.callableWithArgsCall, which renders the
                    // RosettaRule case identically to the Function case (the injected
                    // dependency-instance receiver). The renderer's matching gate at
                    // ReferenceHandler's explicit-args RRule arm was dropped in the
                    // same facet, so field and receiver stay a consistent pair.
                    // Green-safe by construction: without the field a FUNCTION-body
                    // rule reference rendered the raw (namespace-qualified or bare
                    // UpperCamel) rosetta name — non-compiling Java — so no green
                    // file can carry either shape; injectRuleDependency's
                    // degrade-to-skip catch keeps a resolution failure (e.g. an
                    // iso20022/rune-fpml surface) at the pre-fix bytes instead of
                    // throwing.
                    injectRuleDependency(rule, seen, deps, host);
                }
            });
        }

        // Bare RULE reference used as a navigation receiver (facet F6 residual):
        // `SomeRule -> feature` parses as an REnumValueRef whose resolvedSymbol is an
        // RRule (NOT an RSymbolReference), so the branch above never sees it. Inject the
        // SAME <Name>Rule dependency the receiver rendering calls
        // (ReferenceHandler.synthesizeRuleReceiverNavigation → bare-RRule branch →
        // ruleInvocationReceiver), gated identically on host.ruleGateSimpleName() != null
        // so it fires only on the rule-family emission path.
        if (node instanceof REnumValueRef evr
                && evrChainHead(evr) instanceof RRule rule
                && host.ruleGateSimpleName() != null) {
            injectRuleDependency(rule, seen, deps, host);
        }

        // Bare FUNCTION reference used as a navigation receiver (facet bareSymInvoke,
        // PR #280): `SomeFunction -> feature` parses as an REnumValueRef whose
        // resolvedSymbol is an RFunction (NOT an RSymbolReference), so the
        // RSymbolReference branch above never sees it. Inject the SAME function
        // dependency the receiver rendering calls
        // (ReferenceHandler.synthesizeFunctionReceiverNavigation → bare-FUNCTION branch
        // → renderImplicitFunctionInvocation). The bare-FUNCTION analogue of the
        // REnumValueRef → RRule branch above; NOT gated on ruleGateSimpleName (mirroring
        // the un-gated RSymbolReference → RFunction branch — a function dependency is
        // injected on both the rule and function emission paths, and the @Inject field
        // is needed wherever the receiver renders). Green-safe by construction: without
        // the field the bare type name renders to non-compiling Java, so no green file
        // is affected.
        if (node instanceof REnumValueRef evr
                && evrChainHead(evr) instanceof RFunction func) {
            injectFunctionDependency(func, seen, deps, host, depNumbering);
        }

        // Deep feature calls: detect ->> and add the DeepPathUtil dependency.
        // The receiver's type determines which DeepPathUtil class to inject.
        if (node instanceof RDeepFeatureCall deepCall) {
            collectDeepPathUtilDependency(deepCall, seen, deps);
        }

        // Always recurse into children — covers args of RSymbolReference, both
        // sides of binary expressions, condition branches, etc.
        for (RNode child : node.children()) {
            walkExpression(child, seen, deps, host, depNumbering);
        }
    }

    /**
     * Register the {@code @Inject <Name>Rule} dependency for a bare {@link RRule}
     * reference — bridging the rule to its synthetic {@link RFunction} (Origin.RULE) so
     * the SAME {@code toFunctionJavaClass} routing the {@code RuleGenerator} uses yields
     * the {@code <namespace>.reports/<Name>Rule} class. The injected field name is
     * {@code lowerCamelCase(<generated class simple name>)} — the single source of truth
     * shared with the call receiver ({@code ReferenceHandler.ruleInvocationReceiver}) so
     * the field name and the call receiver always match. FQN-qualifies inline (and emits
     * no import) when the rule's simple name collides with the host class's own simple
     * name; otherwise the simple name + import (golden behaviour, engine PR #6 facet b).
     *
     * <p>Shared by the bare {@code then <rule>} {@link RSymbolReference} path and the
     * {@link REnumValueRef}-disguised nav-receiver path (facet F6 residual).
     * Defense-in-depth: a rule that cannot be bridged to its generated class
     * (unresolvable namespace / type) degrades to skipping the dependency rather than
     * propagating, so the rule-family D11 gate surfaces the missing field as a loud,
     * waiver-classified divergence rather than an opaque generation crash. (Engine PR #6
     * review finding; spec §4.2.)
     */
    private void injectRuleDependency(RRule rule, Set<String> seen,
            List<FunctionTemplateModel.DependencyModel> deps, HostInfo host) {
        try {
            RFunction synthetic = RFunction.fromRule(rule);
            ModelSymbolId symbolId = generatorModel.symbolId(synthetic);
            JavaClass<?> javaClass = typeTranslator.toFunctionJavaClass(synthetic, symbolId);
            String simple = javaClass.getSimpleName();
            String fqn = javaClass.getCanonicalName().withDots();
            String fieldName = ruleDependencyFieldName(simple);
            if (seen.add(fieldName)) {
                // facet noEmitSelfInject (PR #330): the collision check gains the SAME
                // package guard the function-path sibling (injectFunctionDependency)
                // carries — a genuine CROSS-package same-simple-name dependency FQN-inlines
                // (golden csa CustomBasketCodeRule: `@Inject protected drr.standards.iosco
                // .cde.version3.basket.reports.CustomBasketCodeRule …`), while a SAME-
                // package / SELF dependency keeps the plain simple name (golden cdeV2
                // ReportingTimestampRule self-@Inject: `@Inject protected
                // ReportingTimestampRule reportingTimestampRule;` — a class never imports
                // itself). The pre-#330 name-only check was unreachable for the self case
                // (a self-referential rule never EMITTED before the #330 inference fixes).
                boolean collides = simple.equals(host.classSimpleName())
                        && host.packageName() != null
                        && !javaClass.getPackageName().withDots().equals(host.packageName());
                deps.add(new FunctionTemplateModel.DependencyModel(
                        fieldName,
                        collides ? fqn : simple,  // typeName: FQN inline on collision
                        collides ? null : fqn,    // typeFqn: null suppresses the import on collision
                        true));
            }
        } catch (RuntimeException ex) {
            // v3.1 C0: a REFUSAL is never recovery — it must reach the per-element
            // boundary (JavaClassGenerator), which attaches the target path and reports
            // it as a generation error. Locked by scripts/ci/refusal-propagation-lint.py.
            if (ex instanceof SilentDegradation.Refusal __refusal) throw __refusal;
            // see javadoc: degrade to skipping the dependency.
        }
    }

    /**
     * Injects an {@code @Inject} dependency field for a referenced {@link RFunction}.
     *
     * <p>Shared by the plain {@link RSymbolReference}-callee path and the
     * {@link REnumValueRef}-disguised nav-receiver path (facet bareSymInvoke, PR #280):
     * a {@code SomeFunction -> feature} nav resolves the disguised head to an RFunction
     * and renders {@code <fnField>.evaluate(...)} on the injected field, so it needs the
     * same field the callee path injects. Idempotent ({@code seen.add}). FQN-qualifies
     * inline (and suppresses the import) when the dependency's generated class simple
     * name collides with the host class's own simple name in a different package.
     */
    private void injectFunctionDependency(RFunction callee, Set<String> seen,
            List<FunctionTemplateModel.DependencyModel> deps, HostInfo host,
            Map<String, NumberedDependency> depNumbering) {
        String bareName = lowerCamelCase(callee.name());
        // facet injectDepCollision (PR #364): the universal no-collision fast path is
        // the pre-facet body byte-for-byte (bare-name dedup, resolution only on first
        // sight). Only a function that HAS a same-simple-name dependency collision
        // group (a non-empty numbering map — corpus census: the IsCSALeg1/2Aligned
        // pair) takes the numbered path below.
        if (depNumbering.isEmpty()) {
            if (seen.add(bareName)) {
                var symbolId = generatorModel.symbolId(callee);
                var javaClass = typeTranslator.toFunctionJavaClass(symbolId);
                addFunctionDependency(bareName, javaClass, false, host, deps);
            }
            return;
        }
        var symbolId = generatorModel.symbolId(callee);
        var javaClass = typeTranslator.toFunctionJavaClass(symbolId);
        NumberedDependency numbered =
                depNumbering.get(javaClass.getCanonicalName().withDots());
        String fieldName = numbered != null ? numbered.fieldName() : bareName;
        if (seen.add(fieldName)) {
            addFunctionDependency(fieldName, javaClass,
                    numbered != null && !numbered.importWinner(), host, deps);
        }
    }

    /**
     * Emits the {@link FunctionTemplateModel.DependencyModel} for a FUNCTION
     * dependency — the shared tail of {@link #injectFunctionDependency}'s fast
     * (no-collision) and numbered (collision-group) paths.
     *
     * @param groupLoser TRUE for a non-head collision-group member (facet
     *        injectDepCollision, PR #364): the group head's single-type-import shadows
     *        the simple name for the whole compilation unit, so the loser renders its
     *        field type fully-qualified inline with the import suppressed — even a
     *        SAME-PACKAGE loser (golden csa {@code CommodityLeg1} → {@code @Inject
     *        protected drr.regulation.csa.rewrite.trade.functions.CommodityLeg1
     *        commodityLeg11;}).
     */
    private void addFunctionDependency(String fieldName, JavaClass<?> javaClass,
            boolean groupLoser, HostInfo host,
            List<FunctionTemplateModel.DependencyModel> deps) {
        String typeName = javaClass.getSimpleName();
        String typeFqn = javaClass.getCanonicalName().withDots();
        // FUNCTION-dependency name collision (function-path facet): a different-package
        // dependency whose generated class simple name matches the host class cannot be
        // imported under that simple name (it would shadow the enclosing class), so the
        // golden FQN-qualifies it inline and emits no import. typeName=FQN renders the
        // @Inject field type inline; typeFqn=null suppresses the import (collectStandardImports
        // skips null-FQN deps). The package guard keeps a same-package / self dependency on
        // the plain simple-name path (its import is suppressed downstream by ImportCollector's
        // same-package rule).
        boolean collides = host.classSimpleName() != null
                && typeName.equals(host.classSimpleName())
                && host.packageName() != null
                && !javaClass.getPackageName().withDots().equals(host.packageName());
        boolean fqnInline = collides || groupLoser;
        deps.add(new FunctionTemplateModel.DependencyModel(
                fieldName,
                fqnInline ? typeFqn : typeName,
                fqnInline ? null : typeFqn,
                true));
    }

    /**
     * Collects a {@code DeepPathUtil} dependency from a deep feature call.
     *
     * <p>The receiver's type determines the DeepPathUtil class. For example,
     * if the receiver resolves to type {@code Asset}, the dependency is
     * {@code AssetDeepPathUtil} in the {@code <namespace>.util} package.
     *
     * <p>Strategy to determine receiver type:
     * <ol>
     *   <li>If the receiver is an {@link RFeatureCall} with a resolved feature,
     *       use the feature's type call to get the type name and look up the
     *       corresponding data type in the workspace.</li>
     *   <li>Otherwise, the receiver type is unknown and no dependency is emitted
     *       (a warning will be logged by the expression compiler).</li>
     * </ol>
     */
    private void collectDeepPathUtilDependency(RDeepFeatureCall deepCall, Set<String> seen,
                                                List<FunctionTemplateModel.DependencyModel> deps) {
        // facet deep_path_util_resolution (PR #205): resolve the receiver type's ModelSymbolId
        // gm-aware (the SAME rooted resolution the renderer uses) so the @Inject field's NAMESPACE
        // is correct. The symbol id carries the namespace, so it is unambiguous; a name-only lookup
        // (findDataTypeByName) is AMBIGUOUS across cells — e.g. both cdm.product.template.Product
        // and fpml.consolidated.shared.Product exist, and the wrong one yields the wrong import.
        ModelSymbolId typeId = NavigationHandler.resolveDeepReceiverSymbolId(deepCall, expressionCompiler);
        if (typeId == null) {
            // BC fallback (null compiler — isolated unit calls): the pre-PR-#205 name-only path.
            String name = resolveReceiverTypeName(deepCall);
            if (name == null) {
                return;
            }
            RDataType dataType = findDataTypeByName(name);
            if (dataType == null) {
                return; // can't determine namespace
            }
            typeId = generatorModel.symbolId(dataType);
        }

        String fieldName = lowerCamelCase(typeId.getName()) + "DeepPathUtil";
        if (!seen.add(fieldName)) {
            return; // already collected
        }

        var javaClass = typeTranslator.toDeepPathUtilJavaClass(typeId);
        String typeName = javaClass.getSimpleName();
        String typeFqn = javaClass.getCanonicalName().withDots();
        deps.add(new FunctionTemplateModel.DependencyModel(
                fieldName, typeName, typeFqn, false));
    }

    /**
     * Resolves the receiver type name from a deep feature call's receiver expression —
     * gm-aware (facet deep_path_util_resolution, PR #205), delegating to the SAME
     * {@link NavigationHandler#resolveDeepReceiverTypeName(RDeepFeatureCall, ExpressionCompiler)}
     * the renderer uses so the injected {@code @Inject <Type>DeepPathUtil} field name matches the
     * lambda body's {@code <type>DeepPathUtil} reference exactly. The prior
     * {@code resolvedFeature()}-only construct returned {@code null} for a receiver navigating
     * through a one-of/choice (no field injected even though the resolved body references one); a
     * {@code null} {@code expressionCompiler} (the BC constructor) keeps that static fallback.
     */
    private String resolveReceiverTypeName(RDeepFeatureCall deepCall) {
        return NavigationHandler.resolveDeepReceiverTypeName(deepCall, expressionCompiler);
    }

    /**
     * Looks up a data type by simple name in the workspace.
     * Returns the first match, or {@code null} if not found.
     */
    private RDataType findDataTypeByName(String name) {
        for (var model : generatorModel.files()) {
            for (var rootElem : model.rootElements()) {
                if (rootElem instanceof RDataType dt && dt.name().equals(name)) {
                    return dt;
                }
            }
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // Utilities
    // -------------------------------------------------------------------------

    /**
     * Lower-camel-case a function name: first character is lowercased,
     * remainder is preserved.
     *
     * <p>E.g. {@code "CalculatePrice"} → {@code "calculatePrice"},
     * {@code "fx"} → {@code "fx"}.
     *
     * <p><b>Single source of truth for the injected field name.</b> This is the
     * derivation used to register the {@code @Inject} dependency field for a
     * called function (functions: source name == Java simple name, so
     * {@code lowerCamelCase(name)} == upstream {@code simpleName.toFirstLower}).
     * Any code that emits the <em>receiver</em> of a function invocation
     * (e.g. {@code ReferenceHandler}'s implicit-predicate branch) MUST derive
     * the receiver name through this method so the receiver matches the field
     * exactly — drift here produces a reference to an undeclared identifier.
     */
    public static String lowerCamelCase(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    /**
     * The {@code @Inject} field name for a RULE dependency (PR #322):
     * {@code lowerCamelCase(<class simple name>)}, ESCAPED with a leading
     * {@code _} when it would EQUAL the class simple name — a lowercase-named
     * rule (drr {@code reporting rule barrier} → class {@code barrierRule})
     * yields a field that would shadow its own type inside the class body, so
     * golden escapes it ({@code @Inject protected barrierRule _barrierRule;}
     * — upstream's scope-collision escape, the #194 usRename law at the
     * dependency-field seat). Receiver derivations MUST route through this
     * same method (see {@link #lowerCamelCase}'s contract note).
     */
    public static String ruleDependencyFieldName(String classSimpleName) {
        String fieldName = lowerCamelCase(classSimpleName);
        if (fieldName != null && fieldName.equals(classSimpleName)) {
            return "_" + fieldName;
        }
        return fieldName;
    }
}
