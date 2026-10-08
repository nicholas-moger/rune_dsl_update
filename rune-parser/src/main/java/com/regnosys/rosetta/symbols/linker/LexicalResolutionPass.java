package com.regnosys.rosetta.symbols.linker;

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.ast.expressions.references.REnumValueRef;
import com.regnosys.rosetta.ast.expressions.references.RSymbolReference;
import com.regnosys.rosetta.ast.expressions.supporting.RInlineFunction;
import com.regnosys.rosetta.ast.expressions.supporting.ROnlyExistsElement;
import com.regnosys.rosetta.ast.functions.RCondition;
import com.regnosys.rosetta.ast.functions.RDispatch;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.supporting.REnumValue;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.ast.types.REnumeration;
import com.regnosys.rosetta.ast.types.RTypeAlias;
import com.regnosys.rosetta.ast.functions.RRule;
import com.regnosys.rosetta.ast.functions.RShortcut;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.util.AstWalker;
import com.regnosys.rosetta.symbols.RFileScope;
import com.regnosys.rosetta.symbols.RFunctionScope;
import com.regnosys.rosetta.symbols.RInlineFunctionScope;
import com.regnosys.rosetta.symbols.diagnostics.DiagnosticCategory;
import com.regnosys.rosetta.symbols.diagnostics.Diagnostics;
import com.regnosys.rosetta.symbols.index.ReferenceIndex;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Pass 5 — walks function and rule bodies, building lexical scopes
 * per resolution walk and resolving {@link RSymbolReference} and
 * {@link RDispatch} against them.
 *
 * <p>Spec: D7 (pass 5) + D8 in
 * {@code docs/specs/2026-04-07-m3-symbols-resolution-design.md}.
 */
public final class LexicalResolutionPass implements LinkerPass {

    private final Map<RModel, RFileScope> fileScopes;
    private final ReferenceIndex referenceIndex;

    public LexicalResolutionPass(
            Map<RModel, RFileScope> fileScopes,
            ReferenceIndex referenceIndex) {
        this.fileScopes = fileScopes;
        this.referenceIndex = referenceIndex;
    }

    public void run(List<RModel> files, Diagnostics collector) {
        for (RModel file : files) {
            RFileScope fileScope = fileScopes.get(file);
            if (fileScope == null) continue;

            for (RFunction fn : AstWalker.findAll(file, RFunction.class)) {
                RFunctionScope fnScope = buildFunctionScope(fileScope, fn);
                // The output node buildFunctionScope REGISTERED (for a dispatch
                // variant: the BASE's output — mirror its choice exactly); the
                // pre-condition output filter below tests identity against it.
                RAttribute registeredOutput = fn.dispatchBase()
                        .map(base -> base.output().orElse(null))
                        .orElseGet(() -> fn.output().orElse(null));
                resolveSymbolsInScope(fn, fnScope, fileScope, registeredOutput, collector, false);
                resolveEnumValueRefHeads(fn, fnScope, fileScope, registeredOutput);
                resolveDispatch(fn, fnScope, collector);
            }
            for (RRule rule : AstWalker.findAll(file, RRule.class)) {
                RFunctionScope ruleScope = buildRuleScope(fileScope, rule);
                resolveSymbolsInScope(rule, ruleScope, fileScope, null, collector, false);
                resolveEnumValueRefHeads(rule, ruleScope, fileScope, null);
            }
            // Coverage wave D (datarule): DATA-TYPE condition expressions resolve
            // against the declaring type's attribute chain + the file scope —
            // the same scope shape upstream gives a condition's implicit type
            // features. Previously these expressions were never walked (their
            // references stayed symbol-EMPTY); the datarule generator is their
            // first consumer. BEST-EFFORT (silent — the only-exists precedent):
            // an unresolved condition reference stays EMPTY with no
            // SYMBOL_NOT_FOUND, so pre-existing diagnostic baselines are
            // unchanged; the D11 byte gate surfaces any miss as visible drift
            // instead.
            for (RDataType dataType : AstWalker.findAll(file, RDataType.class)) {
                if (dataType.conditions().isEmpty()) {
                    continue;
                }
                RFunctionScope conditionScope = buildTypeConditionScope(fileScope, dataType);
                for (RCondition condition : dataType.conditions()) {
                    resolveSymbolsInScope(condition, conditionScope, fileScope, null, collector, true);
                    resolveEnumValueRefHeads(condition, conditionScope, fileScope, null);
                }
            }
            // v3.2 seat 3 (the chaos census's family F9): TYPE-ALIAS condition expressions
            // resolve too — against the FILE scope alone (an alias declares no attributes; its
            // implicit item is the basic value, and upstream scopes a typeAlias condition exactly
            // as any RosettaTypeWithConditions'). They had stayed unwalked ("the TypeFormat
            // constraint scan consumes them structurally"), so a function CALLED inside an alias
            // condition reached the generator symbol-EMPTY and the data-rule class could not
            // inject it (oracle golden CheckedOk: `@Inject protected IsOk isOk;` +
            // `isOk.evaluate(checked)`). Same best-effort contract as the Data loop above.
            for (RTypeAlias alias : AstWalker.findAll(file, RTypeAlias.class)) {
                if (alias.conditions().isEmpty()) {
                    continue;
                }
                for (RCondition condition : alias.conditions()) {
                    // one scope PER condition (round-1 cq N-5): nothing registers into it today,
                    // and a fresh scope forbids one condition's symbols leaking into the next
                    RFunctionScope aliasConditionScope = new RFunctionScope(fileScope);
                    resolveSymbolsInScope(condition, aliasConditionScope, fileScope, null, collector, true);
                    resolveEnumValueRefHeads(condition, aliasConditionScope, fileScope, null);
                }
            }
        }
    }

    private RFunctionScope buildFunctionScope(RFileScope file, RFunction fn) {
        RFunctionScope scope = new RFunctionScope(file);
        // PR #444 dispatch-base inheritance: a DISPATCH VARIANT ({@code func
        // Foo(param Enum -> VALUE):}) resolves its body against the BASE
        // {@code func Foo:} signature. Upstream builds every Function's symbol
        // scope from the base-aware RosettaFunctionExtensions.getInputs/
        // getOutput (vendored RosettaScopeProvider.getSymbolParentScope:394-400
        // → RosettaFunctionExtensions:59-82), and those REPLACE — whenever a
        // main function exists, the variant's own inputs/output declarations
        // (grammar-legal on both sides) are IGNORED, not merged (the Seat-1
        // #444 MF-3 probe: upstream errors on a variant-own-input reference
        // the union scope would have resolved; zero corpus carriers — no
        // corpus variant declares own inputs/output). The variant's OWN
        // shortcuts always register (upstream adds func.getShortcuts() from
        // the variant itself, never the base's). This also heals
        // resolveDispatch's parameter lookup — the dispatch param IS a base
        // input, found by the existing scope.lookup once registered here.
        Optional<RFunction> base = fn.dispatchBase();
        if (base.isPresent()) {
            for (RAttribute input : base.get().inputs()) {
                scope.register(input.name(), input);
            }
            base.get().output().ifPresent(out -> scope.register(out.name(), out));
        } else {
            for (RAttribute input : fn.inputs()) {
                scope.register(input.name(), input);
            }
            fn.output().ifPresent(out -> scope.register(out.name(), out));
        }
        for (RShortcut sc : fn.shortcuts()) {
            scope.register(sc.name(), sc);
        }
        // Do NOT register dispatch paramName — the parameter is already
        // registered from fn.inputs() (for a dispatch variant: from the BASE's
        // inputs above). Registering it here overwrites the RAttribute binding
        // with an RDispatch, breaking resolveDispatch().
        return scope;
    }

    private RFunctionScope buildRuleScope(RFileScope file, RRule rule) {
        RFunctionScope scope = new RFunctionScope(file);
        // Rules have an optional "input" type via RRule.fromType(). When set,
        // the rule body sees an implicit binding named "input" whose target is
        // the rule itself; M4 reads RRule.fromType().referencedType() for the
        // actual type.
        rule.fromType().ifPresent(typeCall -> scope.register("input", rule));
        return scope;
    }

    /**
     * Coverage wave D: the scope a DATA-TYPE condition expression resolves in —
     * the declaring type's attributes (data-supertype chain, registered
     * ROOT-FIRST so a subtype override wins via {@code register}'s last-wins
     * put — matching the leaf-first find the generator's
     * {@code findAttributeOnDataType} walk uses) over the file scope (functions
     * and other globals resolve through the parent lookup).
     */
    private RFunctionScope buildTypeConditionScope(RFileScope file, RDataType dataType) {
        RFunctionScope scope = new RFunctionScope(file);
        Deque<RDataType> chain = new ArrayDeque<>();
        Set<RDataType> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (RDataType t = dataType; t != null && seen.add(t); t = t.superType().orElse(null)) {
            chain.push(t);
        }
        for (RDataType t : chain) {
            for (RAttribute attr : t.attributes()) {
                scope.register(attr.name(), attr);
            }
        }
        return scope;
    }

    private void resolveSymbolsInScope(RNode root, RFunctionScope scope, RFileScope fileScope,
            RAttribute registeredOutput, Diagnostics collector,
            boolean silentOnUnresolved) {
        for (RSymbolReference ref : AstWalker.findAll(root, RSymbolReference.class)) {
            // Skip refs already resolved by pass 4 (GlobalResolutionPass).
            // Type names used as expressions resolve globally; re-walking them
            // here would emit a spurious SYMBOL_NOT_FOUND.
            if (ref.symbol().isPresent()) continue;

            // Check if inside an inline function — build nested scope if so.
            // facet nestedClosureParamScope (PR #431): register EVERY enclosing
            // inline function's params, not just the nearest — a closure param is
            // lexically visible to arbitrarily nested inner lambdas (upstream
            // FunctionGeneratorTest nestedInlineFunctionsTest's cross-lambda
            // `item + param1 + param2`, where `param1` is declared two extract
            // levels up). The nearest-only walk left such a reference UNRESOLVED,
            // so the #429 closure-param typing channel read MISSING (the
            // BigDecimal-triple arithmetic fallback) at every cross-lambda seat.
            // Registration runs OUTER-first so an inner re-registration of the
            // same name overwrites (register's last-wins put) — lexical shadowing,
            // matching upstream's innermost-binding resolution.
            Optional<RNode> resolved = lookupInParentChain(ref, ref.name(), scope);

            // facet aliasSelfScopeFilter (PR #453): upstream's getSymbolParentScope
            // wraps each container's contribution as the walk climbs, and TWO
            // containers FILTER the scope above them (vendored
            // RosettaScopeProvider:401-406):
            //  (1) a ShortcutDeclaration (alias) removes its OWN NAME from its
            //      parent scope (name-based — function features AND file-scope
            //      globals alike), so a bare name inside `alias x: ... x ...`
            //      never resolves to the enclosing alias; it falls through to
            //      the implicit ITEM's feature (the engine's Cat 9 arm binds
            //      it — the census's 4 corpus carriers all sit in extract
            //      lambdas whose item type carries the same-named attribute,
            //      exactly like their already-clearing sibling branches).
            //      Lambda params sit BELOW the alias boundary in upstream's
            //      walk (InlineFunction params wrap the filtered scope), so a
            //      param hit (an RInlineFunction binding) is exempt. Shortcuts
            //      cannot nest — the nearest RShortcut ancestor is the only
            //      one. Pass-4 (global) resolutions are skipped above and stay
            //      bound: upstream's name filter would drop those too, but the
            //      corpus carries ZERO such collisions (census: all 4 sites
            //      resolve function-locally) and the #451 resolved-global-type
            //      render class stays deliberately untouched.
            //  (2) a NON-post Condition removes the function OUTPUT declaration
            //      (feature-identity-based — `descr.getEObjectOrProxy()
            //      .eContainingFeature() != FUNCTION__OUTPUT` unless
            //      isPostCondition). The fork splits the classes, so the gate
            //      is an RCondition ancestor (RPostCondition never matches —
            //      the census's 7 live post-condition output refs keep
            //      binding); only the output NODE is removed, so the lookup
            //      falls back to the file scope where a same-named global
            //      would still win (upstream keeps every non-output descr).
            //      Zero corpus carriers (census B-pre/B2 = 0) — the filter is
            //      upstream-fidelity code witnessed by the smoke locks.
            resolved = applyScopeFilters(ref, ref.name(), resolved, fileScope, registeredOutput);

            if (resolved.isPresent()) {
                ref.setResolvedSymbol(resolved.get());
                referenceIndex.registerReference(ref, resolved.get());
            } else if (ROnlyExistsElement.encloses(ref)) {
                // The synthesized only-exists navigation path (AstBuilder.buildOnlyExistsPath; the
                // ONE enclosure read, LAW 69 — v3.2 seat 8). The TEXT is user-written; the NODES
                // are the fork's own synthesis of it (upstream parses the argument as an ordinary
                // feature call), and since seat 8 the leaf is bound by this very resolver like
                // every other feature (the L2 differential and the seat suite's b1/b3/corpus_c3
                // witness it). What stays exempt is the DIAGNOSTIC: only-exists roots carried no
                // reference node before the receiver synthesis, so they were never diagnosed, and
                // a synthesized root must NOT surface a source-level SYMBOL_NOT_FOUND on valid user
                // code. Resolution is still attempted above (a resolvable root — input/output/
                // shortcut — binds and feeds the generator's nav rendering); an unresolvable root
                // (e.g. a bare function/rule name the generator declines on, keeping the legacy
                // placeholder) is left silently unresolved here — best-effort, not source-validated.
                // The ResolutionAudit applies the symmetric exemption (scope unchanged at round 1:
                // narrowing it to the root and the hops would expose the LEAF, unresolvable exactly
                // when its root is — UNMEASURED whether any such root exists in the audited corpus).
                continue;
            } else if (silentOnUnresolved) {
                // Type-condition walk (wave D): best-effort, not source-validated —
                // an unresolved name stays EMPTY without a diagnostic (see run()).
                continue;
            } else {
                SourceRange range = ref.sourceRange();
                collector.error(DiagnosticCategory.SYMBOL_NOT_FOUND, range, ref.name(),
                    "Symbol '" + ref.name() + "' not found", List.of());
            }
        }
    }

    /**
     * R4 of the resolution spec — the lexical parent chain, innermost first:
     * every enclosing lambda's parameters (outer-first registration so an inner
     * re-registration of the same name wins), then the enclosing function's
     * inputs, output and shortcuts, then the file scope.
     *
     * <p>facet nestedClosureParamScope (PR #431): EVERY enclosing inline
     * function's params are registered, not just the nearest — a closure param
     * is lexically visible to arbitrarily nested inner lambdas (upstream
     * FunctionGeneratorTest nestedInlineFunctionsTest's cross-lambda
     * {@code item + param1 + param2}, where {@code param1} is declared two
     * extract levels up). The nearest-only walk left such a reference
     * UNRESOLVED, so the #429 closure-param typing channel read MISSING (the
     * BigDecimal-triple arithmetic fallback) at every cross-lambda seat.
     */
    private Optional<RNode> lookupInParentChain(RNode ref, String name, RFunctionScope scope) {
        RInlineFunction enclosingInline =
                AstWalker.findAncestor(ref, RInlineFunction.class).orElse(null);
        if (enclosingInline == null) {
            return scope.lookup(name);
        }
        RInlineFunctionScope inlineScope = new RInlineFunctionScope(scope);
        Deque<RInlineFunction> enclosingLambdas = new ArrayDeque<>();
        for (RInlineFunction fn = enclosingInline; fn != null;
                fn = AstWalker.findAncestor(fn, RInlineFunction.class).orElse(null)) {
            enclosingLambdas.push(fn);
        }
        for (RInlineFunction fn : enclosingLambdas) {
            // v3.2 seat 8 (F14): a DECLARED parameter binds to its own node —
            // upstream's ClosureParameter (RInlineFunction.parameters()); a
            // name-only parameter — the synthetic `item` an implicit lambda gains,
            // or a lambda a test hand-built through paramNames() — keeps binding
            // to the declaring lambda, the pre-node shape every consumer still
            // reads through RInlineFunction.declaringLambdaOf.
            for (String paramName : fn.paramNames()) {
                inlineScope.register(paramName,
                        fn.parameter(paramName).<RNode>map(p -> p).orElse(fn));
            }
        }
        return inlineScope.lookup(name);
    }

    /**
     * R5 of the resolution spec — the two filters upstream's
     * {@code getSymbolParentScope} applies as the walk climbs (vendored
     * RosettaScopeProvider:401-406), facet aliasSelfScopeFilter (PR #453):
     *
     * <ol>
     *   <li>a ShortcutDeclaration (alias) removes its OWN NAME from its parent
     *       scope — name-based, function features AND file-scope globals alike
     *       — so a bare name inside {@code alias x: ... x ...} never resolves to
     *       the enclosing alias; it falls through to the implicit ITEM's
     *       feature. Lambda params sit BELOW the alias boundary in upstream's
     *       walk (InlineFunction params wrap the filtered scope), so a param hit
     *       (an RInlineFunction binding) is exempt. Shortcuts cannot nest, so
     *       the nearest RShortcut ancestor is the only one.</li>
     *   <li>a NON-post Condition removes the function OUTPUT declaration
     *       (feature-identity-based upstream; the fork splits the classes, so
     *       the gate is an RCondition ancestor and RPostCondition never
     *       matches). Only the output NODE is removed, so the lookup falls back
     *       to the file scope where a same-named global would still win.</li>
     * </ol>
     */
    private Optional<RNode> applyScopeFilters(RNode ref, String name, Optional<RNode> resolved,
            RFileScope fileScope, RAttribute registeredOutput) {
        if (resolved.isEmpty() || RInlineFunction.isClosureParameterBinding(resolved.get())) {
            return resolved; // a param hit (an RClosureParameter, or its lambda) is exempt
        }
        RShortcut enclosingAlias = AstWalker.findAncestor(ref, RShortcut.class).orElse(null);
        if (enclosingAlias != null && name.equals(enclosingAlias.name())) {
            return Optional.empty();
        }
        if (registeredOutput != null && resolved.get() == registeredOutput
                && AstWalker.findAncestor(ref, RCondition.class).isPresent()) {
            return fileScope.lookup(name).map(r -> (RNode) r);
        }
        return resolved;
    }

    /**
     * v3.1 C1 — THE HEAD OF EVERY {@code a -> b} GETS A BINDING.
     *
     * <p>The fork's grammar turns {@code a -> b} into an REnumValueRef whose
     * left name had no binding at all in the common case: GlobalResolutionPass
     * looks the head up GLOBALLY, and when that finds no enumeration the name is
     * simply never resolved — the engine reaches the feature through a
     * typing-only channel instead. Upstream parses the same text as a feature
     * call over an ordinary symbol reference, so its head binds by the normal
     * symbol rule. Measured against upstream, that gap was 23 of 51 conformance
     * differences, the largest single class.
     *
     * <p>This walk gives the head the SAME parent chain and the SAME two filters
     * a bare name gets (R4, R5), and stores the answer in
     * {@code REnumValueRef.resolvedHead}.
     *
     * <p><b>Strictly additive, and deliberately so.</b> A head that pass 4
     * already bound to an enumeration is left alone: upstream's parent chain
     * would put the file scope LAST, not first, so a lexical binding should in
     * principle shadow a global enumeration of the same name — but flipping that
     * order is a behaviour change on already-green cells, and this fork gates on
     * an EXACT byte ring. The shadowing case is counted rather than flipped (see
     * {@code headShadowingConflicts}); C1 reports the count and only then
     * decides. No diagnostic is emitted here either — pass 4 already owns
     * ENUM_NOT_FOUND for this node, and a second one would double-count the
     * budgets.
     */
    private void resolveEnumValueRefHeads(RNode root, RFunctionScope scope, RFileScope fileScope,
            RAttribute registeredOutput) {
        for (REnumValueRef ref : AstWalker.findAll(root, REnumValueRef.class)) {
            String head = ref.enumName();
            if (head == null || head.isEmpty() || ref.resolvedHead().isPresent()) {
                continue;
            }
            Optional<RNode> resolved = applyScopeFilters(
                    ref, head, lookupInParentChain(ref, head, scope), fileScope, registeredOutput);
            if (resolved.isEmpty()
                    || !com.regnosys.rosetta.symbols.UpstreamSymbolKinds.isUpstreamSymbol(resolved.get())) {
                continue;
            }
            if (ref.enumeration().isPresent()) {
                // Pass 4 bound the head globally to an enumeration AND the
                // lexical chain has a binding for the same name. Upstream would
                // prefer the lexical one. Counted, not flipped.
                if (resolved.get() != ref.enumeration().get()) {
                    headShadowingConflicts++;
                }
                continue;
            }
            ref.setResolvedHead(resolved.get());
            referenceIndex.registerReference(ref, resolved.get());
        }
    }

    // R6 lives in ONE place: com.regnosys.rosetta.symbols.UpstreamSymbolKinds.
    //
    // The first cut here was a private BLACKLIST of the five type-like classes,
    // which silently admitted every root kind nobody had thought of — measured
    // against the root-element census, that was EIGHT wrongly-admitted kinds
    // (RMetaType, RReport, RAnnotation, RBody, RCorpus, RExternalRuleSource,
    // RSegmentDef, RSynonymSource), none of which upstream's RosettaSymbol
    // scope can contain, in a corpus whose DRR cells declare bodies, corpora
    // and annotations by the hundred. Upstream's rule is an instanceof against
    // an interface — an allow-list by construction — and the shared whitelist
    // mirrors it. The Fable review of commits 13-17/n found the two
    // implementations answering R6 differently and consolidated them; the
    // 25-cell seed and the 5-cell ring measured the switch inert on the corpus.

    /**
     * How many {@code a -> b} heads pass 4 bound to a global enumeration while
     * the lexical chain offered a different binding — the case where upstream's
     * innermost-first order and the fork's globals-first phase order disagree.
     * Expected to be ZERO across the 25-cell band; surfaced so that "expected"
     * is a measurement.
     */
    private int headShadowingConflicts;

    public int headShadowingConflicts() {
        return headShadowingConflicts;
    }

    private void resolveDispatch(RFunction fn, RFunctionScope scope, Diagnostics collector) {
        Optional<RDispatch> dispatchOpt = fn.dispatch();
        if (dispatchOpt.isEmpty()) return;
        RDispatch dispatch = dispatchOpt.get();

        // Resolve the dispatched-on parameter
        if (dispatch.paramName() != null) {
            Optional<RNode> param = scope.lookup(dispatch.paramName());
            if (param.isPresent() && param.get() instanceof RAttribute attr) {
                dispatch.setResolvedParam(attr);
                referenceIndex.registerReference(dispatch, attr);

                // Resolve dispatch value: the param's type should be an enum;
                // look up valueName in that enum's values list
                if (dispatch.valueName() != null) {
                    if (attr.typeCall() != null
                            && attr.typeCall().referencedType().isPresent()
                            && attr.typeCall().referencedType().get() instanceof REnumeration enumType) {
                        // Own-values only. Upstream's dispatch-value syntax is a
                        // RosettaEnumValueReference, scoped against
                        // getAllEnumValues() — own + INHERITED (vendored
                        // RosettaScopeProvider:233-236) — so a dispatch value
                        // declared on a super-enum is upstream-legal; the corpus
                        // carries ZERO such dispatches (every dispatch enum is
                        // supertype-free), and the pass-safe cross-file hop walk
                        // (REnumeration.superType() is unusable before workspace
                        // attachment; see GlobalResolutionPass.superEnumOf)
                        // needs this pass to grow the qualified-name machinery —
                        // deferred until a carrier exists (PR #444 census).
                        boolean found = false;
                        for (REnumValue val : enumType.values()) {
                            if (dispatch.valueName().equals(val.name())) {
                                dispatch.setResolvedValue(val);
                                referenceIndex.registerReference(dispatch, val);
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            collector.error(DiagnosticCategory.ENUM_VALUE_NOT_FOUND,
                                dispatch.sourceRange(), dispatch.valueName(),
                                "Dispatch value '" + dispatch.valueName() + "' not found in " + enumType.name(),
                                List.of());
                        }
                    } else {
                        // Param type is missing, unresolved, or not an enum —
                        // dispatch value can't resolve. Emit diagnostic.
                        collector.error(DiagnosticCategory.ENUM_VALUE_NOT_FOUND,
                            dispatch.sourceRange(), dispatch.valueName(),
                            "Dispatch value '" + dispatch.valueName() + "' cannot resolve — parameter type is not an enum",
                            List.of());
                    }
                }
            } else {
                collector.error(DiagnosticCategory.DISPATCH_PARAM_NOT_FOUND,
                    dispatch.sourceRange(), dispatch.paramName(),
                    "Dispatch parameter '" + dispatch.paramName() + "' not found", List.of());
            }
        }
    }
}
