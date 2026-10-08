package com.regnosys.rosetta.generator.java;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.symbols.diagnostics.DiagnosticCategory;
import com.regnosys.rosetta.symbols.diagnostics.LinkingDiagnostic;
import java.util.function.Predicate;

import com.regnosys.rosetta.ast.RRootElement;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.supporting.REnumValue;
import com.regnosys.rosetta.ast.types.RChoice;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.ast.types.REnumeration;
import com.regnosys.rosetta.ast.types.RTypeAlias;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.symbols.RWorkspace;
import com.regnosys.rosetta.types.RDataTypeRef;
import com.regnosys.rosetta.types.REnumTypeRef;
import com.regnosys.rosetta.types.RChoiceTypeRef;
import com.regnosys.rosetta.types.RMissingType;
import com.regnosys.rosetta.types.RType;
import com.regnosys.rosetta.types.RAliasType;
import com.regnosys.rosetta.types.alias.TypeAliasSolver;
import com.regnosys.rosetta.types.builtin.BuiltinTypeRegistry;
import com.rosetta.model.lib.ModelSymbolId;
import com.rosetta.util.DottedPath;

/**
 * Bridge layer between our M2/M3/M4 AST and the API surface that code
 * generators expect (D3). Provides computed properties that generators
 * need beyond what's directly on AST nodes: symbol IDs, all-attributes
 * (inheritance chain), namespace lookup, version resolution.
 *
 * <p>Replaces upstream's combination of {@code RObjectFactory}, {@code ModelIdProvider},
 * and {@code RDataType/RFunction} R-type wrapper lazy fields. Our AST nodes carry
 * the resolved references directly; this class adds the computed derivations.
 */
public class GeneratorModel {

    private final RWorkspace workspace;
    private final Predicate<RModel> emissionFilter;

    /**
     * facet isoPruneConfig (PR #331): the model project's {@code doNotPrune}
     * generator configuration — upstream
     * {@code RosettaGeneratorsConfiguration.doNotPrune()}, a list of
     * (type, attribute) references consulted by {@code IShouldPrune.Default}:
     * a config-disabled pair renders the builder {@code prune()} KEEP form
     * ({@code if (x!=null) x.prune();}) instead of the nullify form, and the
     * {@code hasData()} presence-only form ({@code if (getX()!=null) return true;})
     * instead of the {@code .hasData()} recursion. Keys are
     * {@code <type canonical FQN>#<attribute rune name>} (upstream compares
     * {@code ref.getType().equals(pojo.getCanonicalName().withDots())} +
     * {@code ref.getAttribute().equals(prop.getRuneName())}). Populated by the
     * caller from the model project's {@code rosetta-config.yml}
     * ({@code generators.doNotPrune}) — the same caller-provided pattern as the
     * emission filter; default empty (all existing callers unchanged). The only
     * corpus carrier is iso20022's 3 {@code ClearingPartyAndTime*Choice__1.dtls}
     * entries.
     */
    private final Set<String> doNotPrune;

    /**
     * Lazy cache for {@link #resolveFromWorkspace(String)}. Populated on first miss
     * during the workspace-search fallback in {@link #resolveTypeCall}. Per R4 F4-3:
     * the fallback walks {@code workspace.files()} + {@code rootElements()} on every
     * unresolved typeCall; for large corpora (CDM, DRR) where a non-trivial fraction
     * of attributes hits the fallback, the cumulative cost is O(attributes × files ×
     * rootElements/file). Cache key is the textual type name; the workspace is
     * immutable within a single generator run (no invalidation needed). A negative
     * cache hit (entry-absent / present-with-Optional.empty()) is recorded after the
     * first miss so the next call for the same missing name short-circuits.
     */
    private final Map<String, Optional<RType>> workspaceTypeCache = new HashMap<>();
    // PR #145 (M7B FUNCTION unpause): parallel cache for the workspace type-ALIAS lookup
    // (resolveAliasNodeFromWorkspace). resolveFromWorkspace deliberately matches only
    // RDataType / REnumeration / RChoice, so function-signature alias typeCalls (whose
    // referencedTypeId is unlinked) need this separate alias resolver to recover the
    // concrete builtin instead of falling through to RMissingType -> Object.
    private final Map<String, Optional<RTypeAlias>> workspaceAliasCache = new HashMap<>();

    // Files that are skipped during code generation (matching upstream)
    private static final Set<String> IGNORED_FILES = Set.of(
            "model-no-code-gen.rosetta",
            "basictypes.rosetta",
            "annotations.rosetta"
    );

    public GeneratorModel(RWorkspace workspace) {
        this(workspace, model -> true);
    }

    public GeneratorModel(RWorkspace workspace, Predicate<RModel> emissionFilter, Set<String> doNotPrune) {
        this.workspace = Objects.requireNonNull(workspace, "workspace");
        this.emissionFilter = Objects.requireNonNull(emissionFilter, "emissionFilter");
        this.doNotPrune = Set.copyOf(Objects.requireNonNull(doNotPrune, "doNotPrune"));
    }

    /**
     * Whether builder pruning is config-disabled for the given (type, attribute)
     * pair — see {@link #doNotPrune}. Key format {@code <typeFqn>#<attrRuneName>}.
     */
    public boolean isPruningDisabled(String typeCanonicalFqn, String attributeRuneName) {
        return !doNotPrune.isEmpty()
                && doNotPrune.contains(typeCanonicalFqn + "#" + attributeRuneName);
    }

    /**
     * THE PRUNING SEAM (v3.3 seat 9, PR #645 commit 9): the {@code doNotPrune} set itself, as an unmodifiable
     * copy. It exists so the IR route's data-type emitter can be CONFIGURED with the very set this generator
     * model answers {@link #isPruningDisabled} from, rather than re-reading {@code rosetta-config.yml} or being
     * handed a guess - {@code IRModelObjectGenerator.shadowPojos} builds
     * {@code IRDataTypeEmitter.Config(version, generatorModel.doNotPrune())} and the emitter applies the SAME law
     * over it ({@code IRDataTypeEmitter.Config.isPruningDisabled}). One declaration of the set, two readers;
     * nothing of the OFF route branches on this accessor and not one byte moves.
     */
    public Set<String> doNotPrune() {
        return Set.copyOf(doNotPrune);
    }

    /**
     * Construct with an explicit emission filter. The filter predicate is
     * consulted by {@link #shouldGenerate(RModel)} and gates whether any
     * generator (ENUM, POJO, METAFIELD, FUNCTION) emits Java for a given
     * model. Models for which the filter returns {@code false} stay in the
     * workspace for symbol/type resolution (cross-corpus references resolve
     * through them) but their root elements + attributes are not walked
     * during emission.
     *
     * <p>This is the namespace-filter knob the L272 TODO flagged — the
     * mechanism by which the test harness restricts emission to a single
     * cell's own corpus while keeping its transitive dependencies' source
     * loaded for resolution. Upstream rune-dsl's equivalent shape is the
     * {@code RosettaGeneratorsConfiguration} project-scoped namespace
     * pattern; our implementation surfaces the same semantic via a
     * Predicate constructor parameter, set by the test harness per cell.
     *
     * <p>Default constructor uses {@code model -> true} (accept all), so
     * existing non-test callers see no behavioural change.
     */
    public GeneratorModel(RWorkspace workspace, Predicate<RModel> emissionFilter) {
        this(workspace, emissionFilter, Set.of());
    }

    // -- Namespace resolution -------------------------------------------------

    /**
     * Get the namespace for a root element by walking up to its parent RModel.
     *
     * @return the namespace as a DottedPath
     * @throws IllegalStateException if the element has no parent RModel
     */
    public DottedPath namespace(RRootElement element) {
        if (element.parent() instanceof RModel model) {
            return DottedPath.splitOnDots(model.namespace());
        }
        throw new IllegalStateException(
                "Root element " + element + " has no parent RModel. "
                + "Was it parsed via RWorkspace.build()?");
    }

    /**
     * Get the namespace for a model.
     */
    public DottedPath namespace(RModel model) {
        return DottedPath.splitOnDots(model.namespace());
    }

    // -- Symbol IDs -----------------------------------------------------------

    /**
     * Compute the {@link ModelSymbolId} for a data type.
     */
    public ModelSymbolId symbolId(RDataType type) {
        return new ModelSymbolId(namespace(type), type.name());
    }

    /**
     * Compute the {@link ModelSymbolId} for an enumeration.
     */
    public ModelSymbolId symbolId(REnumeration enumeration) {
        return new ModelSymbolId(namespace(enumeration), enumeration.name());
    }

    /**
     * Compute the {@link ModelSymbolId} for a function.
     *
     * <p><b>Synthetic-bridge recovery (Phase X T5):</b> when {@code function}
     * is a synthetic produced by {@link RFunction#fromRule(com.regnosys.rosetta.ast.functions.RRule)}
     * or {@link RFunction#fromReport(com.regnosys.rosetta.ast.regulatory.RReport)},
     * it has no {@code RModel} parent attachment (the bridge is in-memory
     * only, post-link). The standard
     * {@link #namespace(com.regnosys.rosetta.ast.RRootElement)} path therefore
     * throws. We recover by reading namespace off the back-pointer
     * ({@link RFunction#originRule()} / {@link RFunction#originReport()}) when
     * present — both are {@code RModel}-attached root elements. Mirrors the
     * pattern at {@code LabelProviderGenerator.createTypeRepresentation} but
     * applied uniformly here so downstream callers like
     * {@code FunctionGenerator.buildStandardModel} work for synthetics too
     * (RuleGenerator + ReportGenerator delegate to that path via
     * {@code buildClassWithBaseInterface}).
     */
    public ModelSymbolId symbolId(RFunction function) {
        // Fast-path: function is RModel-attached.
        if (function.parent() instanceof RModel) {
            return new ModelSymbolId(namespace(function), function.name());
        }
        // Synthetic recovery — read namespace from the origin back-pointer.
        DottedPath ns = function.originRule()
                .map(this::namespace)
                .orElseGet(() -> function.originReport()
                        .map(this::namespace)
                        .orElse(null));
        if (ns != null) {
            return new ModelSymbolId(ns, function.name());
        }
        // Fall through to the standard path for the canonical error message
        // — surfaces the workspace-construction bug at the same line + format
        // as it always has.
        return new ModelSymbolId(namespace(function), function.name());
    }

    // -- Inheritance chain traversal ------------------------------------------

    /**
     * All attributes of a data type, including inherited ones from the
     * super type chain. Own attributes override inherited ones with the
     * same name (last wins in the LinkedHashMap).
     */
    public Collection<RAttribute> allAttributes(RDataType type) {
        Map<String, RAttribute> result = new LinkedHashMap<>();
        // Walk up the inheritance chain, collecting from root down
        List<RDataType> chain = new ArrayList<>();
        RDataType current = type;
        while (current != null) {
            chain.add(current);
            current = current.superType().orElse(null);
        }
        // Reverse: root first, so child overrides parent
        for (int i = chain.size() - 1; i >= 0; i--) {
            for (RAttribute attr : chain.get(i).attributes()) {
                result.put(attr.name(), attr);
            }
        }
        return result.values();
    }

    /**
     * All enum values including inherited ones from the super type chain.
     * Inherited values come first, own values appended after (matching upstream).
     */
    public List<REnumValue> allValues(REnumeration enumeration) {
        List<REnumValue> result = new ArrayList<>();
        Optional<REnumeration> parent = enumeration.superType();
        if (parent.isPresent()) {
            result.addAll(allValues(parent.get()));
        }
        result.addAll(enumeration.values());
        return result;
    }

    // -- Attribute type resolution --------------------------------------------

    private static final BuiltinTypeRegistry BUILTINS = BuiltinTypeRegistry.createDefault();

    private static final TypeAliasSolver TYPE_ALIAS_SOLVER = new TypeAliasSolver();

    // Known type aliases from basictypes.rosetta that resolve to builtin types.
    // These are used as a fallback when builtins aren't loaded in the workspace
    // (loading them causes namespace conflicts — see D11 convergence notes).
    private static final Map<String, String> TYPE_ALIAS_TO_BUILTIN = Map.of(
            "calculation", "string",
            "productType", "string",
            "eventType", "string"
    );

    /**
     * Resolve the RType for an attribute's declared type call.
     * Maps the resolved AST node to the M4 type model.
     *
     * @return the RType, or RMissingType if unresolved
     */
    public RType getType(RAttribute attr) {
        return resolveTypeCall(attr.typeCall());
    }

    /**
     * Resolve the RType for a type call (used for both attribute types and choice options).
     *
     * @return the RType, or RMissingType if unresolved
     */
    // Depth bound for the nested-alias recursion in the RTypeAlias branch below.
    // A cyclic typeAlias body (A: B / B: A) would otherwise StackOverflow — the fork
    // has no alias-body cycle validator (only type/enum EXTENSION-cycle validators).
    // Mirrors TypeAliasSolver.MAX_DEPTH; valid corpora nest far shallower than this.
    private static final int MAX_ALIAS_RESOLVE_DEPTH = 100;

    public RType resolveTypeCall(com.regnosys.rosetta.ast.supporting.RTypeCall typeCall) {
        return resolveTypeCall(typeCall, 0);
    }

    /**
     * facet switchChoiceHoist (PR #221): resolve a DECLARED type by its simple name across the
     * loaded workspace (an {@code RDataType} / {@code REnumeration} / {@code RChoice} root
     * element). A choice/type-keyed {@code switch}'s case guard carries only a qualified-name
     * string (the parser defers type-guard resolution for data-type switches — see
     * {@code TypeDirectedResolver.resolveSwitchGuard}), so the renderer resolves the guard's
     * simple type name here to obtain the Java type for the {@code instanceof}/cast/import.
     * Searches ALL loaded files (unlike {@link #resolveFromWorkspace}, which filters by
     * {@link #shouldGenerate}) — a choice-type switch case guard often names a TRANSITIVE-DEP
     * type (e.g. an {@code fpml.consolidated.*} subtype) that is resolved-but-not-generated, so
     * the {@code shouldGenerate} filter would miss it. Matches the FIRST root element with the
     * given simple name; declared type names are effectively unique in the loaded workspace.
     */
    public Optional<RType> resolveTypeByName(String simpleName) {
        var match = workspace.files().stream()
                .flatMap(m -> m.rootElements().stream())
                .filter(e -> e instanceof RDataType
                        || e instanceof REnumeration
                        || e instanceof RChoice)
                .filter(e -> simpleName.equals(rootElementName(e)))
                .findFirst();
        if (match.isEmpty()) {
            return Optional.empty();
        }
        var elt = match.get();
        if (elt instanceof RDataType dt) {
            return Optional.of(new RDataTypeRef(dt));
        }
        if (elt instanceof REnumeration en) {
            return Optional.of(new REnumTypeRef(en));
        }
        if (elt instanceof RChoice ch) {
            return Optional.of(new RChoiceTypeRef(ch.name(), List.of(), ch));
        }
        return Optional.empty();
    }

    private RType resolveTypeCall(com.regnosys.rosetta.ast.supporting.RTypeCall typeCall, int aliasDepth) {
        if (typeCall == null) return RMissingType.INSTANCE;
        // Resolve the cross-ref id via the generator's own workspace rather than
        // typeCall.referencedType() (which resolves via the NODE's attached
        // workspace and throws when the node is detached). Synthetic rule/report
        // input typeCalls (RFunction.fromRule/fromReport, back-filled with the
        // origin's referencedTypeId at codegen time) are detached; resolving via
        // the generator's workspace lets them resolve. Equivalent to
        // referencedType() for attached nodes (same single workspace per run);
        // strictly additive for detached id-bearing copies. Phase X1 Gap #1.
        Optional<RNode> resolved = typeCall.referencedTypeId()
                .map(workspace::resolveTypeLike);
        if (resolved.isEmpty()) {
            // Try builtin lookup by name
            String name = typeCall.typeName();
            var builtin = BUILTINS.lookup(name);
            // facet inlineNumberLadder (W42 finding #2, PR #424): apply the call's OWN
            // literal args over the builtin base — an inline `number(digits: N,
            // fractionalDigits: 0)` attribute must ladder Integer/Long/BigInteger
            // exactly like the typeAlias-wrapped form (the alias path evaluates its
            // body args via evaluateAliasBody; the direct path previously dropped
            // them, leaving bare `number` → BigDecimal at the POJO seat). Identity
            // for argless calls and every non-number base.
            if (builtin.isPresent()) {
                return TYPE_ALIAS_SOLVER.applyDirectTypeCallArguments(builtin.get(), typeCall);
            }
            // Try known type aliases (calculation → string, etc.)
            String aliasTarget = TYPE_ALIAS_TO_BUILTIN.get(name);
            if (aliasTarget != null) {
                var aliasType = BUILTINS.lookup(aliasTarget);
                if (aliasType.isPresent()) {
                    return new RAliasType(name, Map.of(), aliasType.get());
                }
            }
            // P2.1.3c T3 Locus 2 fix: workspace-search fallback for the 4th sub-cause
            // (list-of-data-type-to-Object). When the real-parser AST for some
            // list-of-user-data-type attributes does NOT populate
            // RTypeCall.referencedType() (the resolved Optional above stays empty),
            // the standard resolution path falls through to RMissingType.INSTANCE.
            // RMissingType maps to OBJECT in JavaTypeTranslator#caseMissingType,
            // and the list-wrapper at RJavaPojoInterface#initializeProperties
            // produces List<Object> rather than List<? extends UserDataType> (the
            // observed Cluster F 4th residual bug, ground-truth verified at T3.0
            // diagnostic probe 2026-05-16 on cdm/5.35.0 PaymentCalculationPeriod:
            // `List<Object> getCalculationPeriod()` vs golden
            // `List<? extends CalculationPeriod> getCalculationPeriod();`).
            //
            // Per the development audit "cluster-f-final-T0-spike" § 2 + § 5: search the
            // loaded workspace's models for a matching RDataType / REnumeration /
            // RChoice by name; if found, wrap and return the appropriate ref type
            // rather than falling through to RMissingType. Mirrors the P2.1.3b α
            // Case B bypass pattern at the post-resolved RMissingType branch
            // below (see the {@code if (primary == RMissingType.INSTANCE)} block)
            // for the pre-resolved empty-Optional branch.
            //
            // Symmetric handling of RDataType / REnumeration / RChoice ensures any
            // list-of-{datatype,enum,choice} attribute that bypasses referencedType()
            // gets the correct ref type. Latent sub-cause (list-of-type-alias) is
            // covered by the existing alias map fallback above — if T5 confirms
            // shared locus, no additional change here is needed.
            //
            // R4 F4-2: pipeline extracted to {@link #resolveFromWorkspace(String)};
            // R4 F4-3: cached lazily on first miss.
            var fromWorkspace = resolveFromWorkspace(name);
            if (fromWorkspace.isPresent()) {
                return fromWorkspace.get();
            }
            // PR #145 (M7B FUNCTION unpause — return-type-alias-Object facet): a function
            // output/input whose declared type is a workspace type-alias over a builtin
            // (e.g. `uti UTIIdentifier` where `typeAlias UTIIdentifier: string(pattern: ...)`)
            // arrives here with an empty referencedTypeId — the function-signature
            // attribute's typeCall is unlinked, unlike a POJO attribute's (which resolves to
            // the alias node and takes the RTypeAlias branch below). resolveFromWorkspace
            // matches only RDataType / REnumeration / RChoice (it predates this locus), so an
            // alias name falls through to RMissingType -> JavaTypeTranslator#caseMissingType
            // -> OBJECT (the drr FUNCTION `public Object evaluate(...)` divergence vs golden
            // String/Date — 17 flipped here; same-named aliases declared in the iso20022
            // transitive corpus, not loaded in the drr cell, are a separate out-of-scope gap).
            // Resolve via the SAME full alias-body logic as the resolved-node branch
            // (resolveAliasNode) so number-constrained aliases (e.g. an `int(digits: 3)` alias
            // -> Integer) also get the correct concrete type, not the unconstrained string base.
            var aliasNode = resolveAliasNodeFromWorkspace(name);
            if (aliasNode.isPresent()) {
                return resolveAliasNode(aliasNode.get(), typeCall, aliasDepth);
            }
            return RMissingType.INSTANCE;
        }
        var node = resolved.get();
        if (node instanceof RTypeAlias ta) {
            // P2.1.3 / P2.1.3c / PR #124: resolve the alias body via
            // resolveAliasNode (evaluateAliasBody + unconstrained-builtin recovery +
            // depth-bounded nested-alias recursion). Extracted to a shared helper so the
            // empty-referencedTypeId branch above (PR #145 — unlinked function-signature
            // alias typeCalls) can reuse the identical full logic; see resolveAliasNode.
            return resolveAliasNode(ta, typeCall, aliasDepth);
        }
        // P2.1.3b Locus 1 fix: Case B bypass — if referencedType() resolved but not to
        // RTypeAlias (e.g. real-parser returns a non-RTypeAlias node for the 3 string
        // aliases calculation/productType/eventType — synthetic-vs-real AST divergence
        // per cluster-f-residual-T0-spike.md § 1).
        //
        // R4 F1 guard: fire ONLY when astNodeToRType(node) produces RMissingType (the
        // anomalous resolution shape the fix targets). This is tighter than the prior
        // R2 namespace-only guard because any user file may legally declare its
        // namespace as `com.rosetta.model`; a user `type calculation { ... }` there
        // would resolve to RDataType (which astNodeToRType maps to RDataTypeRef, NOT
        // RMissingType) and so will fall through correctly without triggering the
        // alias-to-builtin rewrite. The bypass is constrained to cases where the
        // primary mapping path would have emitted Object — the observed 34-entry
        // Cluster F α residual.
        RType primary = astNodeToRType(node);
        if (primary == RMissingType.INSTANCE) {
            String aliasTarget = TYPE_ALIAS_TO_BUILTIN.get(typeCall.typeName());
            if (aliasTarget != null) {
                var aliasType = BUILTINS.lookup(aliasTarget);
                if (aliasType.isPresent()) {
                    return new RAliasType(typeCall.typeName(), Map.of(), aliasType.get());
                }
            }
            // P2.1.3c T3 Locus 1 fix: extend the Case B bypass with the workspace-search
            // fallback for the 4th sub-cause. When referencedType() resolves to a node
            // that astNodeToRType maps to RMissingType (e.g. a stub/placeholder created
            // for an unresolved forward-reference), the user-defined RDataType /
            // REnumeration / RChoice may still be present in the workspace's loaded
            // root elements — find it by name as a fallback rather than emitting
            // `Object`. Mirrors the symmetric pre-resolved-empty fallback in the
            // {@code if (resolved.isEmpty())} branch above.
            //
            // Ground-truth confirmed at T3.0 diagnostic probe 2026-05-16 on
            // cdm/5.35.0 PaymentCalculationPeriod: standard resolution returned a
            // non-RDataType node (resolved branch — NOT the empty-Optional branch),
            // which astNodeToRType collapsed to RMissingType. Without this fallback,
            // the empty-Optional branch's workspace-search is unreachable
            // for this failure mode.
            //
            // R4 F4-2: pipeline extracted to {@link #resolveFromWorkspace(String)};
            // R4 F4-3: cached lazily on first miss.
            var fromWorkspace = resolveFromWorkspace(typeCall.typeName());
            if (fromWorkspace.isPresent()) {
                return fromWorkspace.get();
            }
        }
        // facet inlineNumberLadder (W42 finding #2, PR #424): the resolved-NODE twin
        // of the builtin-lookup arm above. When the workspace loads the builtin models
        // (basictypes.rosetta), `number` resolves to its `basicType` AST node and
        // astNodeToRType's name-keyed BUILTINS lookup drops the call's args the same
        // way — this seat is the one the holdout/corpus loaders actually ride.
        // Identity for argless calls and every non-RNumberType primary (data/enum/
        // choice refs, records, strings, RMissingType).
        return TYPE_ALIAS_SOLVER.applyDirectTypeCallArguments(primary, typeCall);
    }

    /**
     * Resolve an {@link RTypeAlias} node to its concrete underlying {@link RType},
     * wrapped in an {@link RAliasType} that preserves the alias's user-facing name.
     *
     * <p>Extracted (PR #145) from the resolved-node {@code instanceof RTypeAlias}
     * branch of {@link #resolveTypeCall} so the empty-{@code referencedTypeId} branch
     * — reached by unlinked function-signature alias typeCalls — can reuse the
     * IDENTICAL logic. Using the same path is load-bearing: number-constrained aliases
     * (e.g. {@code Max3Number: int(digits: 3)}) must resolve through
     * {@link TypeAliasSolver#evaluateAliasBody} to the correct parametric
     * {@code RNumberType} ({@code int} → boxed {@code Integer}), not the unconstrained
     * string base a pattern-only recovery would yield.
     *
     * <p>Three layers, in order: (1) {@code evaluateAliasBody} evaluates the body's
     * literal type-call args (e.g. {@code fractionalDigits: 0}); (2) when that yields
     * {@link RMissingType} (string-constrained alias bodies surface the unsupported
     * case per {@code TypeAliasSolver}), recover the UNCONSTRAINED builtin via
     * {@code BUILTINS.lookup(ta.typeCall().typeName())} — matching the golden emit which
     * unwraps the alias to its underlying primitive; (3) when the body's typeName is
     * itself a user alias (alias-of-alias, e.g. {@code FpMLVersion: Token}), recurse via
     * {@link #resolveTypeCall} so the chain collapses to its underlying builtin,
     * depth-bounded by {@link #MAX_ALIAS_RESOLVE_DEPTH} (a cyclic body degrades to
     * RMissingType → Object rather than overflowing the stack — the fork has no
     * alias-body cycle validator). The recovered base is the UNCONSTRAINED primitive
     * (no pattern / minLength / maxLength surface); sufficient for the byte-match
     * codegen contract (upstream also emits the unwrapped primitive), but downstream
     * code needing the constraint envelope must query the original {@code RTypeAlias}.
     *
     * @param useSiteCall the use-site type call (supplies any parametric arguments to
     *     {@code evaluateAliasBody}; for the function-signature path this is the
     *     unlinked attribute typeCall, which carries no args for the affected aliases)
     */
    private RType resolveAliasNode(RTypeAlias ta,
            com.regnosys.rosetta.ast.supporting.RTypeCall useSiteCall, int aliasDepth) {
        RType evaluated = TYPE_ALIAS_SOLVER.evaluateAliasBody(ta, useSiteCall);
        if (evaluated instanceof RMissingType
                && ta.typeCall() != null
                && ta.typeCall().typeName() != null) {
            var baseLookup = BUILTINS.lookup(ta.typeCall().typeName());
            if (baseLookup.isPresent()) {
                evaluated = baseLookup.get();
            } else {
                RType nested = aliasDepth < MAX_ALIAS_RESOLVE_DEPTH
                        ? resolveTypeCall(ta.typeCall(), aliasDepth + 1)
                        : RMissingType.INSTANCE;
                if (!(nested instanceof RMissingType)) {
                    evaluated = nested;
                }
            }
        }
        return new RAliasType(ta.name(), Map.of(), evaluated);
    }

    /**
     * Workspace-search for a type-ALIAS root element by simple name — the alias
     * counterpart to {@link #resolveFromWorkspace} (which matches only RDataType /
     * REnumeration / RChoice). PR #145: a function output/input whose declared type is a
     * workspace type-alias-over-builtin (e.g. {@code uti UTIIdentifier}) reaches the
     * empty-{@code referencedTypeId} branch of {@link #resolveTypeCall} with an unlinked
     * typeCall; without this lookup the alias name fell through to RMissingType → OBJECT
     * (the M7B FUNCTION {@code public Object evaluate(...)} divergence vs golden String /
     * Date / Integer). Simple-name match only — a dotted FQN typeName (e.g. the
     * {@code iso20022.auth030.hkma.dtcc.Max3Number} input variants) stays unmatched and
     * conservatively resolves to Object, mirroring {@link #resolveFromWorkspace}'s
     * RDataType behaviour. Cached lazily (including negative hits) in
     * {@link #workspaceAliasCache}; the workspace is immutable within a generator run.
     *
     * <p>Uses the same {@link #shouldGenerate} file filter as {@link #resolveFromWorkspace}
     * (consistency + minimal blast): the 17 flipped drr functions reference aliases
     * declared IN the drr corpus (e.g. {@code UTIIdentifier} / {@code ISODate} in
     * {@code standards-iso-type.rosetta}). Cross-corpus aliases (e.g. the iso20022
     * {@code MICIdentifier} the drr projection functions also reference) are not in the
     * cell's loaded model set at all — a separate TRANSITIVE loader gap, out of scope.
     * Regardless of breadth, this lookup is reached only from the empty-referencedTypeId
     * branch, which a currently byte-matching (green) file never enters (its type already
     * resolved via referencedTypeId), so no green file's output can change.
     */
    private Optional<RTypeAlias> resolveAliasNodeFromWorkspace(String name) {
        return workspaceAliasCache.computeIfAbsent(name, n ->
                workspace.files().stream()
                        .filter(this::shouldGenerate)
                        .flatMap(m -> m.rootElements().stream())
                        .filter(e -> e instanceof RTypeAlias)
                        .map(e -> (RTypeAlias) e)
                        .filter(ta -> n.equals(ta.name()))
                        .findFirst());
    }

    /**
     * Extract the declared name of a root element. Handles the three root-element
     * subclasses involved in the T3 workspace-search fallback: RDataType / REnumeration
     * / RChoice. Used by the workspace-search fallback in {@link #resolveTypeCall}
     * to match a type call's textual {@code typeName()} against the names of
     * loaded root elements.
     *
     * <p>R4 F4-5: throws {@link IllegalStateException} when an unsupported root-element
     * subclass is passed. The two upstream call sites (in {@link #resolveFromWorkspace})
     * pre-filter with {@code instanceof RDataType || REnumeration || RChoice}, so the
     * throw branch is defensive-only — surfacing a contract violation immediately if a
     * new root-element subclass is later added to the upstream filter without updating
     * this helper.
     */
    private static String rootElementName(RRootElement e) {
        if (e instanceof RDataType dt) return dt.name();
        if (e instanceof REnumeration en) return en.name();
        if (e instanceof RChoice ch) return ch.name();
        throw new IllegalStateException(
                "Unsupported root element type: " + e.getClass().getName()
                + " — rootElementName supports RDataType / REnumeration / RChoice only; "
                + "upstream filter in resolveFromWorkspace should have excluded this element.");
    }

    /**
     * Workspace-search fallback for {@link #resolveTypeCall} — looks up an RDataType /
     * REnumeration / RChoice root element by its declared name across the loaded
     * workspace's files (excluding ignored files / filter-rejected models per
     * {@link #shouldGenerate}). Returns an {@link Optional} wrapping the constructed
     * {@code RDataTypeRef} / {@code REnumTypeRef} / {@code RChoiceTypeRef}, or empty
     * if no element of those three kinds matches the name.
     *
     * <p>R4 F4-2: extracted from two sites in {@link #resolveTypeCall} (Locus 2 in the
     * empty-Optional pre-resolution branch, Locus 1 in the post-resolution
     * RMissingType bypass branch) — both had the same stream pipeline; extraction
     * avoids divergence risk if one site is later modified.
     *
     * <p>R4 F4-3: cached lazily on first miss in {@link #workspaceTypeCache}. The
     * workspace is immutable within a single generator run, so the cache lives on the
     * instance and never needs invalidation. Negative cache hits ({@code
     * Optional.empty()}) are also recorded so repeat lookups for missing names
     * short-circuit. {@link Map#computeIfAbsent} guarantees the value is computed
     * at most once per key.
     */
    private Optional<RType> resolveFromWorkspace(String name) {
        return workspaceTypeCache.computeIfAbsent(name, n -> {
            var match = workspace.files().stream()
                    .filter(this::shouldGenerate)
                    .flatMap(m -> m.rootElements().stream())
                    .filter(e -> e instanceof RDataType
                            || e instanceof REnumeration
                            || e instanceof RChoice)
                    .filter(e -> n.equals(rootElementName(e)))
                    .findFirst();
            if (match.isEmpty()) return Optional.empty();
            var elt = match.get();
            if (elt instanceof RDataType dt) return Optional.of(new RDataTypeRef(dt));
            if (elt instanceof REnumeration en) return Optional.of(new REnumTypeRef(en));
            if (elt instanceof RChoice ch) {
                return Optional.of(new RChoiceTypeRef(ch.name(), List.of(), ch));
            }
            return Optional.empty();
        });
    }

    /**
     * Map an AST node (resolved from a type call) to an RType.
     * Uses fully-qualified AST type names where they clash with M4 types.
     */
    private RType astNodeToRType(RNode node) {
        if (node instanceof RDataType dt) return new RDataTypeRef(dt);
        if (node instanceof REnumeration en) return new REnumTypeRef(en);
        // Choice types generate as data type interfaces (upstream: caseChoiceType → caseDataType)
        if (node instanceof com.regnosys.rosetta.ast.types.RChoice ch) {
            return new RChoiceTypeRef(ch.name(), List.of(), ch);
        }
        // AST basic types (ast.types.RBasicType) and record types (ast.types.RRecordType)
        // are different classes from M4 types (types.RBasicType, types.RRecordType).
        // v3.2 seat 9 (D47, F8): a resolved basic / record type DECLARATION node — the builtins
        // model's (its M4 twin by name, as before) or the MODEL's own (`nothing` → Void, the
        // released plugin's mapping, where the seat found RMissingType → Object: the chaos s15
        // family, 36 declared rows) — ONE law, BuiltinTypeRegistry.basicOrRecordNodeType (LAW 69
        // with the parser twin ExpressionTypeComputer.astNodeToRType and the alias body).
        var declared = BUILTINS.basicOrRecordNodeType(node);
        if (declared.isPresent()) return declared.get();
        // P2.1.3: RTypeAlias case is handled upstream in resolveTypeCall(), not here.
        // The alias body's typeCall arguments must be evaluated via
        // TypeAliasSolver.evaluateAliasBody to preserve fractionalDigits: 0 etc.
        return RMissingType.INSTANCE;
    }

    /**
     * Whether an attribute is multi-valued (cardinality upper bound > 1 or unbounded).
     */
    public boolean isMulti(RAttribute attr) {
        return attr.cardinality()
                .map(c -> c.isUnbounded() || c.sup().intValue() > 1)
                .orElse(false);
    }

    // -- Version resolution ---------------------------------------------------

    /**
     * Resolve the version string for a model: the declared version, or upstream's
     * default when the source declares none.
     *
     * <p><b>The default is upstream's, not ours (v3.1 phase C, C0 item 4 / N4).</b>
     * Upstream's EMF model declares it literally — {@code rune-dsl/rune-lang/model/
     * Rosetta.xcore}: {@code class RosettaModel … String version = "0.0.0"} — so an
     * absent {@code ('version' version=STRING)?} clause still reaches upstream's
     * emitters as {@code "0.0.0"}, never as null. Our AST keeps the parse-level truth
     * (absent means {@link RModel#version()} is empty), so the default is applied
     * here, at the generator boundary, where output parity is defined.
     *
     * <p>Witness: drr 5.61.0's two {@code techsprint.g20.mas} models declare no
     * version and their four goldens carry {@code @version 0.0.0}. Band-wide these are
     * the only two such models — every other one of the 4,060 model files declares a
     * version — so this default cannot move any other cell, gated or otherwise.
     * Locked by {@link com.regnosys.rosetta.generator.java.object.ModelVersionDefaultTest}.
     */
    public String version(RModel model) {
        return model.version().orElse(UNDECLARED_MODEL_VERSION);
    }

    /**
     * Upstream's {@code RosettaModel.version} EMF default (see {@link #version(RModel)}).
     */
    public static final String UNDECLARED_MODEL_VERSION = "0.0.0";

    // -- Unresolved-reference lookup (v3.1 C0 item 1) -------------------------

    /** Lazily built key set of every reference the linker reported unresolved: {@code file|offset|name}. */
    private volatile Set<String> unresolvedReferenceKeys;
    /** v3.2 seat 9 round 1: the same keys suffixed by the diagnostic's category (see {@link #isReportedUnresolved(com.regnosys.rosetta.ast.RNode, String, com.regnosys.rosetta.symbols.diagnostics.DiagnosticCategory)}). */
    private volatile Set<String> unresolvedReferenceCategoryKeys;

    /**
     * Whether the linker REPORTED this expression's name as unresolved — the resolver's own
     * verdict, not a guess about it.
     *
     * <p>v3.1 phase C, C0 item 1. Distinguishing "the AST node carries no symbol" from "this
     * name resolves to nothing" matters: the first is routine (a lambda-bound name, a
     * closure parameter, an attribute read the binder never attaches a symbol to — all of
     * which render correctly and byte-match the goldens), while only the second is the
     * E1/E2 echo that emits an unbound Java identifier. Measured on the 25-cell band, the
     * loose test fired 117 times on cells that are byte-perfect; this one keys on the
     * diagnostic the linker already emitted, so it fires exactly where resolution FAILED
     * (M3's D2 contract: the linker never silently nulls a field — it always emits a
     * {@code LinkingDiagnostic} with a precise range).
     *
     * <p>Keyed by source range + name so two references to the same name in one file are
     * judged separately. Diagnostics without offsets fall back to name+file, which can
     * only ever over-approximate within a file that already has a failure.
     *
     * <p>The consumer below deliberately still consults the wildcard key even when the
     * EXPRESSION has offsets: the fallback exists for offset-less DIAGNOSTICS, and an
     * offset-bearing expression must still be able to match one. It is the producer that
     * must not mint a wildcard for a diagnostic that knows its own offset.
     */
    public boolean isReportedUnresolved(RExpression expr, String name) {
        if (expr == null || name == null || workspace == null) {
            return false;
        }
        Set<String> keys = unresolvedKeySet(false);
        SourceRange exprRange = expr.sourceRange();
        if (exprRange == null) {
            return false;
        }
        return keys.contains(unresolvedKey(exprRange.file(), exprRange.startOffset(), name))
                || keys.contains(unresolvedKey(exprRange.file(), SourceRange.OFFSETS_UNKNOWN, name));
    }

    /**
     * THE ONE BUILDER of the two unresolved-key sets (round 2, the code-quality review's SF-5 - LAW 69: the
     * category-blind and the category-filtered forms had carried the same walk twice). One pass over the linker's
     * diagnostics fills both: the {@code file|offset|name} key and, beside it, the same key suffixed by the
     * diagnostic's category. The file+name WILDCARD is for diagnostics that have no offsets, and ONLY those: adding it
     * unconditionally made the lookup collapse to file+name for every diagnostic, so one genuine failure on a name
     * condemned EVERY other reference to that name in the same file - the opposite of the "judged separately"
     * contract, and a false refusal rather than a false pass (Copilot R13, PR #566). Both sets are published
     * IMMUTABLE: the fields are volatile and built under a benign race (two callers may both build; one wins), so an
     * effectively-immutable value is what makes that race safe - Set.copyOf removes the "effectively" (Copilot R18,
     * PR #566). The category-blind set is exactly the projection of the category-filtered one (every key in it
     * has at least one suffixed twin), so the two forms cannot disagree.
     */
    // round 3 (the code-quality review's NIT-3): both memo fields are written by this method's ONE walk, as two separate
    // volatile writes - so under the benign race above a reader may see one non-null and the other still null. What makes
    // the early return safe is narrower (round 4, cq SF-5): each call reads ONLY the field it returns, published complete
    // (Set.copyOf) from that walk, so a non-null read is a complete value for ITS form whatever the other field holds.
    // The cached copies are returned as locals rather than re-reading the volatile a second time.
    private Set<String> unresolvedKeySet(boolean byCategory) {
        Set<String> have = byCategory ? unresolvedReferenceCategoryKeys : unresolvedReferenceKeys;
        if (have != null) {
            return have;
        }
        Set<String> blind = new HashSet<>();
        Set<String> filtered = new HashSet<>();
        for (LinkingDiagnostic diagnostic : workspace.linkingDiagnostics()) {
            SourceRange range = diagnostic.range();
            String suffix = "|" + diagnostic.category();
            String key = unresolvedKey(range.file(), range.startOffset(), diagnostic.unresolvedName());
            blind.add(key);
            filtered.add(key + suffix);
            if (range.startOffset() == SourceRange.OFFSETS_UNKNOWN) {
                String wild = unresolvedKey(range.file(), SourceRange.OFFSETS_UNKNOWN, diagnostic.unresolvedName());
                blind.add(wild);
                filtered.add(wild + suffix);
            }
        }
        Set<String> blindKeys = Set.copyOf(blind);
        Set<String> filteredKeys = Set.copyOf(filtered);
        unresolvedReferenceKeys = blindKeys;
        unresolvedReferenceCategoryKeys = filteredKeys;
        return byCategory ? filteredKeys : blindKeys;
    }

    /**
     * v3.2 seat 9 round 1 (PR #630, the code-quality review's SF-9): the same verdict FILTERED by the diagnostic's
     * category, for a refusal whose message names the category — the F13 gate names {@code ENUM_VALUE_NOT_FOUND}, so
     * it reads this form (the two-argument form is category-blind). Node-typed: an {@code RDispatch} is not an
     * expression, and the linker anchors its diagnostic at the node's own range — the same {@code file|offset|name}
     * key, suffixed by the category. Both forms consult {@link #unresolvedKeySet} (round 2, cq SF-5).
     */
    public boolean isReportedUnresolved(RNode node, String name, DiagnosticCategory category) {
        if (node == null || name == null || category == null || workspace == null) {
            return false;
        }
        Set<String> keys = unresolvedKeySet(true);
        SourceRange nodeRange = node.sourceRange();
        if (nodeRange == null) {
            return false;
        }
        String suffix = "|" + category;
        return keys.contains(unresolvedKey(nodeRange.file(), nodeRange.startOffset(), name) + suffix)
                || keys.contains(unresolvedKey(nodeRange.file(), SourceRange.OFFSETS_UNKNOWN, name) + suffix);
    }

    private static String unresolvedKey(String file, int offset, String name) {
        return file + "|" + offset + "|" + name;
    }

    // -- File filtering -------------------------------------------------------

    /**
     * Whether a model should be skipped during generation.
     * Checks the source file name against the ignored files set.
     */
    public boolean isIgnoredFile(RModel model) {
        String filePath = model.sourceRange().file();
        if (filePath == null || filePath.equals("<unknown>")) {
            return false;
        }
        // Extract just the filename from the path
        int lastSlash = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
        String fileName = lastSlash >= 0 ? filePath.substring(lastSlash + 1) : filePath;
        return IGNORED_FILES.contains(fileName);
    }

    /**
     * Whether a model should have code generated.
     * Checks both the ignored file list and the namespace filter.
     */
    public boolean shouldGenerate(RModel model) {
        return !isIgnoredFile(model) && emissionFilter.test(model);
    }

    // -- Workspace access -----------------------------------------------------

    public RWorkspace workspace() {
        return workspace;
    }

    public List<RModel> files() {
        return workspace.files();
    }
}
